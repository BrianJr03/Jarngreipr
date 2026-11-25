#!/usr/bin/env bash
# Jarn label handoff check for #434 on the AYN Thor (debug build).
#
#   scripts/jarnfe-verify/verify-434-jarn-label.sh [options]
#
#   --apk PATH              adb install -r this APK first (never uninstalls).
#   --out DIR               Where dumps and screenshots go (default: build/verify-434/<timestamp>).
#   --keep-cue              Leave "Jarn label" selected afterwards instead of restoring the old cue.
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when control moves between screens. The script never writes it.
#
# Selects the "Jarn label" switch cue by editing the app's own prefs (run-as, so debug builds
# only), restarts Jarn FE, then:
#   1. finds the label on Stage and its slot on the Deck in uiautomator dumps,
#   2. presses X and checks the label left Stage and arrived on the Deck,
#   3. presses X again and checks it came back,
#   4. presses X twice in one burst (an interrupted handoff) and checks it settles on Stage,
#   5. opens Settings with Start and checks no screen shows the label while the Deck has hidden
#      its tabs, then leaves with B and checks the label is back on Stage,
#   6. checks logcat for crashes or ANRs, the process stayed alive, and screen_focus_lock.
# Both screens are captured mid-animation and after it settles, for a human look at the motion.
#
# Safety: never uninstalls or clears data. Keys are X, Start (only on Library) and B, and only
# while Jarn FE holds focus.
# On exit the old switch cue is written back and Jarn FE is restarted, unless --keep-cue.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

SUITE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SUITE_DIR/lib"
REPO_ROOT="$(cd "$SUITE_DIR/../.." && pwd)"
HELPER="$LIB_DIR/labelquery.py"
# shellcheck source=lib/parse.sh
source "$LIB_DIR/parse.sh"
# shellcheck source=lib/common.sh
source "$LIB_DIR/common.sh"
# shellcheck source=lib/checks.sh
source "$LIB_DIR/checks.sh"

PKG="${JARNFE_PKG:-jr.brian.home.debug}"
STAGE_ACTIVITY="jr.brian.jarnfe.shell.JarnStageActivity"
LOOK_PREFS=jarnfe_look_prefs
CUE_KEY=switch_cue
CUE_ID=jarn_label
WORDMARK=JARN
TOP_DISPLAY="${JARNFE_TOP_DISPLAY:-0}"
BOTTOM_DISPLAY="${JARNFE_BOTTOM_DISPLAY:-4}"
TOP_PHYSICAL="${JARNFE_TOP_PHYSICAL:-4630946441858561667}"
BOTTOM_PHYSICAL="${JARNFE_BOTTOM_PHYSICAL:-4630946482288158084}"
# Share of near-white pixels the label adds to its box. Text covers far more than this.
INK_MARGIN="${JARNFE_INK_MARGIN:-0.02}"

APK="" OUT_DIR="" KEEP_CUE=0 ALLOW_SYNC=0 HANDOFF_OK=0 HANDOFF_REASON=""
LOCK_MODE="" START_LOCK="" START_PID="" LOG_START=""
ORIG_CUE="" CUE_CHANGED=0
STAGE_DISPLAY=$TOP_DISPLAY DECK_DISPLAY=$BOTTOM_DISPLAY
STAGE_PHYSICAL=$TOP_PHYSICAL DECK_PHYSICAL=$BOTTOM_PHYSICAL
STAGE_BOX="" DECK_BOX=""

usage() { sed -n '2,28p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --apk) APK="${2:?--apk needs a path}"; shift ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      --keep-cue) KEEP_CUE=1 ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      -h|--help) usage; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

select_device() {
  command -v adb >/dev/null || { echo "adb not found" >&2; exit 2; }
  python3 -I -c 'import PIL' 2>/dev/null || { echo "python3 with Pillow is needed" >&2; exit 2; }
  local serials count
  serials="$(adb devices | tr -d '\r' | awk 'NR > 1 && $2 == "device" { print $1 }')"
  count="$(printf '%s' "$serials" | grep -c .)"
  if [ -n "${ANDROID_SERIAL:-}" ]; then
    text_has_line "$serials" "$ANDROID_SERIAL" || { echo "ANDROID_SERIAL=$ANDROID_SERIAL is not attached" >&2; exit 2; }
  elif [ "$count" = 0 ]; then
    echo "no adb device attached" >&2; exit 2
  elif [ "$count" -gt 1 ]; then
    echo "more than one device attached, set ANDROID_SERIAL" >&2; exit 2
  else
    export ANDROID_SERIAL="$serials"
  fi
}

read_roles() {
  local prefs
  prefs="$(read_prefs screen_assignment_prefs)" || return 0
  if [ "$(printf '%s' "$prefs" | parse_pref assignment)" = "FRONTEND_ON_BOTTOM" ]; then
    STAGE_DISPLAY=$BOTTOM_DISPLAY DECK_DISPLAY=$TOP_DISPLAY
    STAGE_PHYSICAL=$BOTTOM_PHYSICAL DECK_PHYSICAL=$TOP_PHYSICAL
  fi
}

read_focus_lock_gate() {
  local prefs
  START_LOCK="$(focus_lock_value)"
  if prefs="$(read_prefs focus_lock_prefs)"; then
    LOCK_MODE="$(printf '%s' "$prefs" | parse_pref focus_lock_mode)"
    LOCK_MODE="${LOCK_MODE:-Follow}"
  fi
  if [ "$ALLOW_SYNC" = 1 ] || { [ -n "$LOCK_MODE" ] && focus_sync_is_inert "$LOCK_MODE" "$START_LOCK"; }; then
    HANDOFF_OK=1
  else
    HANDOFF_REASON="Focus Lock mode ${LOCK_MODE:-unreadable} may rewrite screen_focus_lock (--allow-app-focus-sync to run)"
  fi
}

# Writes $1 as the switch cue, or removes it when empty, with the app stopped so it can't overwrite it.
write_cue() {
  local value="$1" script
  sh_dev am force-stop "$PKG" >/dev/null
  script="$(python3 -I -c '
import sys
import xml.etree.ElementTree as ET
raw, key, value = sys.stdin.read(), sys.argv[1], sys.argv[2]
try:
    root = ET.fromstring(raw) if raw.strip() else ET.Element("map")
except ET.ParseError:
    root = ET.Element("map")
for item in list(root):
    if item.get("name") == key:
        root.remove(item)
if value:
    node = ET.SubElement(root, "string", name=key)
    node.text = value
print("<?xml version=\x271.0\x27 encoding=\x27utf-8\x27 standalone=\x27yes\x27 ?>")
print(ET.tostring(root, encoding="unicode"))
' "$CUE_KEY" "$value" <<<"$(read_prefs "$LOOK_PREFS")")" || return 1
  printf '%s\n' "$script" | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/$LOOK_PREFS.xml'" || return 1
  [ "$(read_prefs "$LOOK_PREFS" | parse_pref "$CUE_KEY")" = "$value" ]
}

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
}

restore() {
  [ "$CUE_CHANGED" = 1 ] && [ "$KEEP_CUE" = 0 ] || return 0
  section "Restore"
  if write_cue "$ORIG_CUE"; then
    echo "  info  switch cue back to ${ORIG_CUE:-default}"
    sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  else
    echo "  WARN  could not restore the switch cue (was ${ORIG_CUE:-default})"
  fi
}

preflight() {
  section "Preflight"
  if [ -n "$APK" ]; then
    adb install -r "$APK" >/dev/null || { echo "install of $APK failed (nothing uninstalled)" >&2; return 1; }
    echo "  info  installed $APK"
  fi
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_prefs "$LOOK_PREFS" >/dev/null || {
    echo "run-as is unavailable for $PKG. On a release build pick Settings > Display > Screen switch cue > Jarn label by hand, then rerun with JARNFE_PKG set." >&2
    return 1
  }
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  ORIG_CUE="$(read_prefs "$LOOK_PREFS" | parse_pref "$CUE_KEY")"
  write_cue "$CUE_ID" || { echo "could not select the Jarn label cue" >&2; return 1; }
  CUE_CHANGED=1
  start_stage || { echo "could not bring Stage to the front" >&2; return 1; }
  LOG_START="$(sh_dev date +%s).000"
  START_PID="$(sh_dev pidof "$PKG")"
  echo "  info  $PKG, cue was ${ORIG_CUE:-default}, Stage on display $STAGE_DISPLAY, Deck on $DECK_DISPLAY"
  echo "  info  output in $OUT_DIR"
}

find_boxes() {
  section "Label slots"
  if ui_wait_role stage; then
    cp "$UI_XML" "$OUT_DIR/stage.xml"
    STAGE_BOX="$(python3 -I "$HELPER" bounds "$UI_XML" "$WORDMARK")"
  fi
  [ -n "$STAGE_BOX" ] && pass "Stage label at [$STAGE_BOX]" || fail "no $WORDMARK node on Stage"
  key "$KEY_X" || return 1
  if ui_wait_role deck; then
    cp "$UI_XML" "$OUT_DIR/deck.xml"
    DECK_BOX="$(python3 -I "$HELPER" bounds "$UI_XML" "$WORDMARK")"
  fi
  [ -n "$DECK_BOX" ] && pass "Deck label slot left of the tabs at [$DECK_BOX]" || fail "no $WORDMARK slot on the Deck"
  key "$KEY_X" || return 1
  wait_top_display "$STAGE_DISPLAY"
  pause 1
  [ -n "$STAGE_BOX" ] && [ -n "$DECK_BOX" ]
}

# Captures both screens as $1-stage.png and $1-deck.png at the same moment.
shots() {
  shot "$STAGE_PHYSICAL" "$OUT_DIR/$1-stage.png" &
  shot "$DECK_PHYSICAL" "$OUT_DIR/$1-deck.png" &
  wait
}

ink() {
  local box
  if [ "$2" = stage ]; then box="$STAGE_BOX"; else box="$DECK_BOX"; fi
  # shellcheck disable=SC2086
  python3 -I "$HELPER" ink "$OUT_DIR/$1-$2.png" $box
}

more_than() { python3 -I -c 'import sys; a, b, m = map(float, sys.argv[1:]); sys.exit(0 if a > b + m else 1)' "$1" "$2" "$INK_MARGIN"; }

# $1 the frame, $2 the screen that should show the label. Compares against the rest frame,
# where the label is on Stage.
expect_label_on() {
  local frame="$1" holder="$2" stage deck rest_stage rest_deck
  stage="$(ink "$frame" stage)" deck="$(ink "$frame" deck)"
  rest_stage="$(ink rest stage)" rest_deck="$(ink rest deck)"
  echo "  info  $frame ink: stage $stage (rest $rest_stage), deck $deck (rest $rest_deck)"
  if [ "$holder" = deck ]; then
    if more_than "$deck" "$rest_deck"; then pass "$frame: label arrived on the Deck"; else fail "$frame: no label on the Deck"; fi
    if more_than "$rest_stage" "$stage"; then pass "$frame: label left Stage"; else fail "$frame: label still on Stage"; fi
  else
    if more_than "$deck" "$rest_deck"; then fail "$frame: label still on the Deck"; else pass "$frame: Deck has no label"; fi
    if more_than "$rest_stage" "$stage"; then fail "$frame: label missing on Stage"; else pass "$frame: label back on Stage"; fi
  fi
}

# One X, with mid-animation frames right away and the settled frames after.
switch_and_capture() {
  local name="$1"
  key_now "$KEY_X" || return 1
  shots "$name-mid"
  pause 1.2
  shots "$name"
}

# Like key(), but sends $2 presses in one burst with no settle between them.
key_now() {
  local code="$1" count="${2:-1}" display app codes="" i
  [ "$ABORTED" = 0 ] || return 1
  display="$(top_display)"
  app="$(focused_app "${display:-0}")"
  case "$app" in
    "$PKG"/*) ;;
    *) fail "Jarn FE lost focus (display ${display:-?}: ${app:-none}), stopping all input"; ABORTED=1; return 1 ;;
  esac
  for ((i = 0; i < count; i++)); do codes="$codes $code"; done
  # shellcheck disable=SC2086
  sh_dev input -d 0 keyevent $codes >/dev/null
  KEYS_SENT=1
}

check_handoff() {
  section "X moves the label"
  shots rest
  switch_and_capture to-deck || return 1
  expect_label_on to-deck deck
  switch_and_capture to-stage || return 1
  expect_label_on to-stage stage
  section "Interrupted handoff settles"
  key_now "$KEY_X" 2 || return 1
  shots burst-mid
  pause 1.5
  shots burst
  expect_label_on burst stage
}

check_hidden_tabs() {
  section "Settings on the Deck hides the label"
  settings_start_is_safe || { skip "Stage is not on Library, so Start is not pressed"; return 0; }
  key "$KEY_START" || return 1
  if ! ui_wait_role deck || [ -z "$(ui_selected "${SETTINGS_TABS[@]}")" ]; then
    skip "Settings did not open on the Deck"
    key "$KEY_B"
    return 0
  fi
  cp "$UI_XML" "$OUT_DIR/settings-deck.xml"
  if ui_has "$WORDMARK"; then fail "the Deck shows a label slot in Settings"; else pass "no label slot on the Deck in Settings"; fi
  pause 0.6
  shots settings
  local stage
  stage="$(ink settings stage)"
  echo "  info  settings ink: stage $stage (rest $(ink rest stage))"
  if more_than "$(ink rest stage)" "$stage"; then pass "settings: label left Stage"; else fail "settings: label still on Stage"; fi
  key "$KEY_B" || return 1
  wait_top_display "$STAGE_DISPLAY"
  pause 1.2
  shots after-settings
  expect_label_on after-settings stage
}

main() {
  parse_args "$@"
  select_device
  OUT_DIR="${OUT_DIR:-$REPO_ROOT/build/verify-434/$(date +%Y%m%d-%H%M%S)}"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  trap restore EXIT
  preflight || exit 2
  find_boxes && check_handoff && check_hidden_tabs
  check_no_crashes
  check_lock_unchanged
  section "For a human"
  echo "  look  $OUT_DIR/*-mid-*.png: label part way out on one screen, part way in on the other"
  echo "  look  swap screens (L3) and rerun: the label should still travel toward the screen it goes to"
  printf '\n%s passed, %s failed, %s skipped\n' "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT"
  [ "$FAIL_COUNT" = 0 ]
}

main "$@"

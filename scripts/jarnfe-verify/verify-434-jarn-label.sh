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
#   2. presses X and checks the label left Stage and arrived on the Deck, and that each screen's
#      first tab slid left into the label's place where the label is away, and back where it is,
#   3. presses X again and checks it came back, with the tabs back in place,
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
# Mean pixel difference in the label box. Below it the label shows as in its reference frame.
SAME_MAX="${JARNFE_SAME_MAX:-10}"
# A tab back in place may land this many px off after rounding.
SHIFT_TOL="${JARNFE_SHIFT_TOL:-6}"

APK="" OUT_DIR="" KEEP_CUE=0 ALLOW_SYNC=0 HANDOFF_OK=0 HANDOFF_REASON=""
LOCK_MODE="" START_LOCK="" START_PID="" LOG_START=""
ORIG_CUE="" CUE_CHANGED=0
STAGE_DISPLAY=$TOP_DISPLAY DECK_DISPLAY=$BOTTOM_DISPLAY
STAGE_PHYSICAL=$TOP_PHYSICAL DECK_PHYSICAL=$BOTTOM_PHYSICAL
STAGE_BOX="" DECK_BOX="" STAGE_TAB="" DECK_TAB="" STAGE_SLOT=0 DECK_SLOT=0

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
    STAGE_TAB="$(python3 -I "$HELPER" bounds "$UI_XML" "${LIBRARY_TABS[0]}")"
  fi
  [ -n "$STAGE_BOX" ] && pass "Stage label at [$STAGE_BOX]" || fail "no $WORDMARK node on Stage"
  key "$KEY_X" || return 1
  if ui_wait_role deck; then
    cp "$UI_XML" "$OUT_DIR/deck.xml"
    DECK_BOX="$(python3 -I "$HELPER" bounds "$UI_XML" "$WORDMARK")"
    DECK_TAB="$(python3 -I "$HELPER" bounds "$UI_XML" "${DECK_TABS[0]}")"
  fi
  [ -n "$DECK_BOX" ] && pass "Deck label slot left of the tabs at [$DECK_BOX]" || fail "no $WORDMARK slot on the Deck"
  key "$KEY_X" || return 1
  wait_top_display "$STAGE_DISPLAY"
  pause 1
  [ -n "$STAGE_TAB" ] && [ -n "$DECK_TAB" ] || { fail "first tab not found on both screens"; return 1; }
  STAGE_SLOT="$(slot_width "$STAGE_BOX" "$STAGE_TAB")"
  DECK_SLOT="$(slot_width "$DECK_BOX" "$DECK_TAB")"
  echo "  info  slot widths: stage ${STAGE_SLOT}px, deck ${DECK_SLOT}px"
  [ -n "$STAGE_BOX" ] && [ -n "$DECK_BOX" ]
}

# Label left edge to first tab left edge: how far the tab moves when the slot closes.
slot_width() {
  local label tab
  read -r -a label <<<"$1"
  read -r -a tab <<<"$2"
  echo $((tab[0] - label[0]))
}

# Captures both screens as $1-stage.png and $1-deck.png at the same moment.
shots() {
  shot "$STAGE_PHYSICAL" "$OUT_DIR/$1-stage.png" &
  shot "$DECK_PHYSICAL" "$OUT_DIR/$1-deck.png" &
  wait
}

box_of() { if [ "$1" = stage ]; then echo "$STAGE_BOX"; else echo "$DECK_BOX"; fi; }
tab_of() { if [ "$1" = stage ]; then echo "$STAGE_TAB"; else echo "$DECK_TAB"; fi; }
slot_of() { if [ "$1" = stage ]; then echo "$STAGE_SLOT"; else echo "$DECK_SLOT"; fi; }

# Frame names where each screen holds the label: Stage at rest, the Deck after the first X.
home_frame() { if [ "$1" = stage ]; then echo rest; else echo to-deck; fi; }

# $1 reference frame, $2 frame, $3 screen.
label_diff() {
  # shellcheck disable=SC2046
  python3 -I "$HELPER" diff "$OUT_DIR/$1-$3.png" "$OUT_DIR/$2-$3.png" $(box_of "$3")
}

tab_shift() {
  # shellcheck disable=SC2046
  python3 -I "$HELPER" shift "$OUT_DIR/$1-$3.png" "$OUT_DIR/$2-$3.png" $(tab_of "$3") "$(( $(slot_of "$3") + 40 ))"
}

below() { python3 -I -c 'import sys; sys.exit(0 if float(sys.argv[1]) <= float(sys.argv[2]) else 1)' "$1" "$2"; }

# The first tab of $1 in frame $2 sits where it does with the label home.
expect_tab_home() {
  local dx
  dx="$(tab_shift "$(home_frame "$1")" "$2" "$1")"
  if [ "${dx#-}" -le "$SHIFT_TOL" ]; then pass "$2: $1 tabs in place beside the label (moved ${dx}px)"
  else fail "$2: $1 tabs moved ${dx}px with the label home"; fi
}

# The first tab of $1 in frame $2 slid left by most of the slot.
expect_tab_away() {
  local dx want
  dx="$(tab_shift "$(home_frame "$1")" "$2" "$1")"
  want=$(( $(slot_of "$1") * 7 / 10 ))
  if [ "$dx" -le "-$want" ]; then pass "$2: $1 tabs slid ${dx}px into the label's place"
  else fail "$2: $1 tabs moved ${dx}px, wanted about -$(slot_of "$1")px"; fi
}

expect_label_home() {
  local diff
  diff="$(label_diff "$(home_frame "$1")" "$2" "$1")"
  if below "$diff" "$SAME_MAX"; then pass "$2: label shows on $1 (diff $diff)"
  else fail "$2: label missing on $1 (diff $diff)"; fi
}

expect_label_away() {
  local diff
  diff="$(label_diff "$(home_frame "$1")" "$2" "$1")"
  if below "$diff" "$SAME_MAX"; then fail "$2: label still on $1 (diff $diff)"
  else pass "$2: label gone from $1 (diff $diff)"; fi
}

# $1 the frame, $2 the screen that should show the label.
expect_label_on() {
  local frame="$1" holder="$2" away
  if [ "$holder" = stage ]; then away=deck; else away=stage; fi
  if [ "$frame" != "$(home_frame "$holder")" ]; then
    expect_label_home "$holder" "$frame"
    expect_tab_home "$holder" "$frame"
  fi
  expect_label_away "$away" "$frame"
  expect_tab_away "$away" "$frame"
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
  expect_tab_away deck rest
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
  expect_label_away stage settings
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
  echo "  look  the tabs slide with the label and land with it, never jumping at the end"
  echo "  look  swap screens (L3) and rerun: the label should still travel toward the screen it goes to"
  printf '\n%s passed, %s failed, %s skipped\n' "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT"
  [ "$FAIL_COUNT" = 0 ]
}

main "$@"

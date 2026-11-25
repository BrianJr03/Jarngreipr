#!/usr/bin/env bash
# #450: the one-time "Use <look> fonts?" prompt on the Deck.
#
#   scripts/jarnfe-verify/verify-450-font-prompt.sh [options]
#
#   --install APK           adb install -r this APK first (never uninstalls).
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when the prompt moves control to the Deck. The script never writes it.
#
# Debug builds only (run-as). Needs Command Panel on a look with its own fonts, and the fonts
# reader (#449); without them no prompt shows and the check skips. With Jarn FE stopped it clears
# fonts_prompt_shown, turns Match Command Panel fonts off and marks the tour done, then:
#   1. starts Jarn FE and waits for the prompt on the Deck, with focus on "Use fonts",
#   2. presses B, and checks the prompt closed, fonts_prompt_shown is true and fonts stayed off,
#   3. restarts Jarn FE and checks the prompt does not come back,
#   4. no crash or ANR, and screen_focus_lock is unchanged.
# The Deck is captured with the prompt up and after the relaunch.
#
# Safety: never uninstalls, clears data or presses A. Keys are B only, and only while Jarn FE
# holds focus. On exit the old prompt, fonts and tour values are written back and Jarn FE restarted.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

LOOK_PREFS=jarnfe_look_prefs
PROMPT_KEY=fonts_prompt_shown
FONTS_KEY=match_command_panel_fonts
TOUR_KEY=first_run_done
USE_LABEL="Use fonts"
NOT_NOW_LABEL="Not now"
PROMPT_WAIT_TRIES=8
INSTALL_APK="" ORIG_PROMPT="" ORIG_FONTS="" ORIG_TOUR="" PREF_CHANGED=0

usage_450() { sed -n '2,23p' "$VERIFY_DIR/verify-450-font-prompt.sh" | sed 's/^# \{0,1\}//'; }

parse_450_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_450; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

install_if_asked() {
  [ -n "$INSTALL_APK" ] || return 0
  [ -f "$INSTALL_APK" ] || { echo "no such APK: $INSTALL_APK" >&2; exit 2; }
  echo "Installing $INSTALL_APK"
  adb install -r "$INSTALL_APK" || { echo "adb install failed (signature mismatch? not uninstalling)" >&2; exit 2; }
}

look_pref() { read_prefs "$LOOK_PREFS" | parse_pref "$1"; }

# Writes boolean pref $1 as $2 (true or false), or removes it when $2 is empty, with the app stopped.
write_bool() {
  local name="$1" value="$2" script
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
    ET.SubElement(root, "boolean", name=key, value=value)
print("<?xml version=\x271.0\x27 encoding=\x27utf-8\x27 standalone=\x27yes\x27 ?>")
print(ET.tostring(root, encoding="unicode"))
' "$name" "$value" <<<"$(read_prefs "$LOOK_PREFS")")" || return 1
  printf '%s\n' "$script" | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/$LOOK_PREFS.xml'" || return 1
  [ "$(look_pref "$name")" = "$value" ]
}

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
}

restore_450() {
  [ "$PREF_CHANGED" = 1 ] || return 0
  PREF_CHANGED=0
  section "Restore"
  local ok=1
  write_bool "$PROMPT_KEY" "$ORIG_PROMPT" || ok=0
  write_bool "$FONTS_KEY" "$ORIG_FONTS" || ok=0
  write_bool "$TOUR_KEY" "$ORIG_TOUR" || ok=0
  if [ "$ok" = 1 ]; then
    echo "  info  prompt ${ORIG_PROMPT:-default}, fonts ${ORIG_FONTS:-default}, tour ${ORIG_TOUR:-default} written back"
  else
    echo "  WARN  could not restore every value (prompt ${ORIG_PROMPT:-default}, fonts ${ORIG_FONTS:-default}, tour ${ORIG_TOUR:-default})"
  fi
  sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
}

preflight_450() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_prefs "$LOOK_PREFS" >/dev/null || { echo "run-as is unavailable for $PKG (debug builds only)" >&2; return 1; }
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  ORIG_PROMPT="$(look_pref "$PROMPT_KEY")"
  ORIG_FONTS="$(look_pref "$FONTS_KEY")"
  ORIG_TOUR="$(look_pref "$TOUR_KEY")"
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  $PKG, prompt ${ORIG_PROMPT:-default}, fonts ${ORIG_FONTS:-default}, tour ${ORIG_TOUR:-default}"
  echo "  info  Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY, output in $OUT_DIR"
}

focused_label() { ui_focused | awk -F' [|] ' '{ print $1 }'; }

prompt_is_up() { ui_dump && ui_has "$USE_LABEL" && ui_has "$NOT_NOW_LABEL"; }

wait_for_prompt() {
  local i
  for ((i = 0; i < PROMPT_WAIT_TRIES; i++)); do
    wait_top_display "$DECK_DISPLAY" && prompt_is_up && return 0
    pause 1
  done
  return 1
}

arm_prompt() {
  write_bool "$PROMPT_KEY" "" && write_bool "$FONTS_KEY" false && write_bool "$TOUR_KEY" true
}

run_450() {
  section "Fonts prompt shows once"
  PREF_CHANGED=1
  arm_prompt || { fail "could not clear the prompt flag"; return 0; }
  start_stage
  START_PID="$(sh_dev pidof "$PKG")"
  if ! wait_for_prompt; then
    skip "no prompt: Command Panel is not on a font look, or the fonts reader (#449) is not in this build"
    return 0
  fi
  pass "prompt shows on the Deck"
  shot "$DECK_PHYSICAL" "$OUT_DIR/450-prompt.png" || skip "Deck screenshot failed"
  if [ "$(focused_label)" = "$USE_LABEL" ]; then pass "focus starts on \"$USE_LABEL\""
  else fail "focus starts on \"$(focused_label)\", not \"$USE_LABEL\""; fi

  key "$KEY_B" || return 0
  pause 1.5
  if prompt_is_up; then fail "B did not close the prompt"; else pass "B closes the prompt"; fi
  [ "$(look_pref "$PROMPT_KEY")" = true ] && pass "fonts_prompt_shown is true" || fail "fonts_prompt_shown was not set"
  [ "$(look_pref "$FONTS_KEY")" = false ] && pass "B left fonts off" || fail "B changed Match Command Panel fonts"

  sh_dev am force-stop "$PKG" >/dev/null
  start_stage
  START_PID="$(sh_dev pidof "$PKG")"
  pause 3
  shot "$DECK_PHYSICAL" "$OUT_DIR/450-relaunch.png" || skip "Deck screenshot failed"
  if prompt_is_up; then fail "the prompt came back after a relaunch"; else pass "no prompt after a relaunch"; fi
}

main_450() {
  parse_450_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-450)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_if_asked
  trap restore_450 EXIT
  trap 'restore_450; exit 130' INT TERM
  preflight_450 || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_450
  check_no_crashes
  check_lock_unchanged
  restore_450
  section "For a human"
  echo "  look  $OUT_DIR/450-prompt.png: title, both samples side by side, Use fonts focused"
  echo "  look  $OUT_DIR/450-relaunch.png: no prompt"
  summary
}

main_450 "$@"

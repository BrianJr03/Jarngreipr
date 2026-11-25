#!/usr/bin/env bash
# #446: the "Match Command Panel fonts" row in Jarn FE Settings > Look > Theme.
#
#   scripts/jarnfe-verify/verify-446-cp-fonts.sh [options]
#
#   --install APK           adb install -r this APK first (never uninstalls).
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when control moves to the Deck. The script never writes it.
#
# Debug builds only (run-as). Writes the fonts switch off with Jarn FE stopped (and marks the tour
# done if it isn't, so it doesn't cover Library), restarts Jarn FE on
# Stage, opens Settings with Start, steps the Deck to Look and presses Down until the fonts row has
# focus. Then:
#   1. the row right before it in D-pad order is "Match Command Panel colors" (skipped when
#      Command Panel is not installed, since both rows are then hidden),
#   2. presses A once on the fonts row, and reads match_command_panel_fonts back with run-as:
#      true when the row offered to turn on, still false when its supporting text says why not,
#   3. the colors switch (match_command_panel) did not change, so the two stay independent,
#   4. B still leaves Settings, and there was no crash or ANR.
# The Deck is captured before and after A, for a human look at the row and its supporting text.
#
# Safety: never uninstalls or clears data. A is pressed once, only after a fresh dump shows the
# fonts row focused. Other keys are Start, D-pad, shoulders and B. On exit the old fonts and tour
# values are written back and Jarn FE restarted.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

KEY_A=96
LOOK_PREFS=jarnfe_look_prefs
FONTS_KEY=match_command_panel_fonts
COLORS_KEY=match_command_panel
TOUR_KEY=first_run_done
FONTS_LABEL="Match Command Panel fonts"
COLORS_LABEL="Match Command Panel colors"
UNAVAILABLE_PREFIX="Unavailable."
MAX_ROWS=30
INSTALL_APK="" ORIG_FONTS="" ORIG_COLORS="" ORIG_TOUR="" PREF_CHANGED=0 BEFORE_FOCUS="" FONTS_FOCUS=""

usage_446() { sed -n '2,26p' "$VERIFY_DIR/verify-446-cp-fonts.sh" | sed 's/^# \{0,1\}//'; }

parse_446_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_446; exit 0 ;;
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

write_fonts() { write_bool "$FONTS_KEY" "$1"; }

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
}

restore_446() {
  [ "$PREF_CHANGED" = 1 ] || return 0
  PREF_CHANGED=0
  section "Restore"
  if [ "$ORIG_TOUR" != true ]; then
    if write_bool "$TOUR_KEY" "$ORIG_TOUR"; then echo "  info  tour state back to ${ORIG_TOUR:-default (not done)}"
    else echo "  WARN  could not restore the tour state (was ${ORIG_TOUR:-default})"; fi
  fi
  if write_fonts "$ORIG_FONTS"; then
    echo "  info  Match Command Panel fonts back to ${ORIG_FONTS:-default (off)}"
    sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  else
    echo "  WARN  could not restore Match Command Panel fonts (was ${ORIG_FONTS:-default})"
  fi
}

preflight_446() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_prefs "$LOOK_PREFS" >/dev/null || {
    echo "run-as is unavailable for $PKG. On a release build toggle Settings > Look > Match Command Panel fonts by hand." >&2
    return 1
  }
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  ORIG_FONTS="$(look_pref "$FONTS_KEY")"
  ORIG_COLORS="$(look_pref "$COLORS_KEY")"
  ORIG_TOUR="$(look_pref "$TOUR_KEY")"
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  $PKG, fonts was ${ORIG_FONTS:-default (off)}, colors ${ORIG_COLORS:-default (off)}"
  echo "  info  Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY"
  echo "  info  output in $OUT_DIR"
}

focused_label() { ui_focused | awk -F' [|] ' '{ print $1 }'; }

open_look_settings() {
  ui_wait_role stage || return 1
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY" || return 1
  settings_start_is_safe || { fail "Stage is not on Library, so Start is not pressed"; return 1; }
  key "$KEY_START" || return 1
  IN_SETTINGS=1
  ui_wait_role deck || { fail "Settings did not show on the Deck"; return 1; }
  tab_keys_for "$DECK_DISPLAY"
  press_times "$TAB_PREV" "$((${#SETTINGS_TABS[@]} - 1))" &&
    press_times "$TAB_NEXT" "$(index_of Look "${SETTINGS_TABS[@]}")" || return 1
  ui_dump
  [ "$(ui_selected "${SETTINGS_TABS[@]}")" = Look ] || { fail "could not step the Deck to Look"; return 1; }
}

# Presses Down until the fonts row has focus. Sets BEFORE_FOCUS and FONTS_FOCUS.
find_fonts_row() {
  local i last=""
  for ((i = 0; i < MAX_ROWS; i++)); do
    key "$KEY_DOWN" || return 1
    ui_wait_role deck || continue
    if [ "$(focused_label)" = "$FONTS_LABEL" ]; then
      BEFORE_FOCUS="$last"
      FONTS_FOCUS="$(ui_focused)"
      return 0
    fi
    last="$(focused_label)"
  done
  return 1
}

press_a_on_fonts() {
  ui_wait_role deck || return 1
  [ "$(focused_label)" = "$FONTS_LABEL" ] || return 1
  jarn_is_focused || { ABORTED=1; return 1; }
  sh_dev input -d 0 keyevent "$KEY_A" >/dev/null
  KEYS_SENT=1
  pause 1.5
}

check_order() {
  if [ "$BEFORE_FOCUS" = "$COLORS_LABEL" ]; then
    pass "D-pad order: colors row, then fonts row"
  else
    fail "the row before the fonts row was \"${BEFORE_FOCUS:-none}\", not \"$COLORS_LABEL\""
  fi
}

check_toggle() {
  local fonts colors
  fonts="$(look_pref "$FONTS_KEY")"
  colors="$(look_pref "$COLORS_KEY")"
  if text_has "$FONTS_FOCUS" "$UNAVAILABLE_PREFIX"; then
    if [ "${fonts:-false}" = false ]; then pass "disabled row stayed off and says why: $FONTS_FOCUS"
    else fail "A turned on a row that said it was unavailable"; fi
  elif [ "$fonts" = true ]; then
    pass "A turned Match Command Panel fonts on"
  else
    fail "A did not turn the fonts switch on (read ${fonts:-nothing}, row: $FONTS_FOCUS)"
  fi
  if [ "$colors" = "$ORIG_COLORS" ]; then pass "colors switch untouched (${colors:-default})"
  else fail "colors switch changed from ${ORIG_COLORS:-default} to ${colors:-default}"; fi
}

leave_settings() {
  key "$KEY_B" || return
  IN_SETTINGS=0
  if wait_top_display "$STAGE_DISPLAY"; then pass "B leaves Settings"
  else fail "B did not return control to Stage"; fi
}

run_446() {
  section "Match Command Panel fonts"
  write_fonts false || { fail "could not set the fonts switch off"; return 0; }
  PREF_CHANGED=1
  if [ "$ORIG_TOUR" != true ]; then
    write_bool "$TOUR_KEY" true || { fail "could not mark the tour done"; return 0; }
  fi
  start_stage || { fail "could not bring Stage to the front"; return 0; }
  START_PID="$(sh_dev pidof "$PKG")"
  open_look_settings || return 0
  if ! find_fonts_row; then
    skip "no fonts row on Look (Command Panel not installed, so the Theme rows are hidden)"
    leave_settings
    return 0
  fi
  check_order
  shot "$DECK_PHYSICAL" "$OUT_DIR/446-before.png" || skip "Deck screenshot failed"
  press_a_on_fonts || { fail "refused to press A, fonts row not focused"; return 0; }
  ui_wait_role deck && echo "  info  after A: $(ui_focused)"
  shot "$DECK_PHYSICAL" "$OUT_DIR/446-after.png" || skip "Deck screenshot failed"
  check_toggle
  leave_settings
}

main_446() {
  parse_446_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-446)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_if_asked
  trap restore_446 EXIT
  trap 'restore_446; exit 130' INT TERM
  preflight_446 || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_446
  check_no_crashes
  check_lock_unchanged
  restore_446
  section "For a human"
  echo "  look  $OUT_DIR/446-before.png: fonts row sits right under the colors row, focused"
  echo "  look  $OUT_DIR/446-after.png: switch on, or still off with the reason under it"
  summary
}

main_446 "$@"

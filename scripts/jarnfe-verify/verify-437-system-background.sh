#!/usr/bin/env bash
# #437: the "System background" switch on Now hides a focused system's art and cover wall.
#
#   scripts/jarnfe-verify/verify-437-system-background.sh [options]
#
#   --install APK           adb install -r this APK first (never uninstalls).
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when Jarn FE restarts. The script never writes it.
#
# Debug builds only: the switch is flipped by editing the app's own prefs with run-as, with Jarn FE
# stopped so it can't overwrite them. For each value (on, then off) it restarts Jarn FE on Stage,
# steps Stage to the Systems tab and the Deck to Now (L1/L2, then R1/R2), nudges focus onto a
# system with Right and Left, and captures the Deck display. Then:
#   1. off: most of the band between the tabs and the game row is one flat color (the theme
#      background), so neither system art nor the cover wall is drawn,
#   2. on: that band differs from the off frame, so the art came back (skipped when the system has
#      no art at all, since both frames then look the same),
#   3. no crash or ANR, and screen_focus_lock is unchanged.
# A live wallpaper shows through when the switch is off, so the flat check only reports then.
#
# Safety: never uninstalls, clears data or presses A. Keys are D-pad, L1/R1 and L2/R2, and only
# while Jarn FE holds focus. On exit the old switch value is written back and Jarn FE restarted.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

HELPER="$LIB_DIR/backgroundquery.py"
LOOK_PREFS=jarnfe_look_prefs
BACKGROUND_KEY=now_system_background_visible
LIVE_WALLPAPER_KEY=live_wallpaper_id
# Share of the band that must be plain theme background with the switch off.
MIN_FLAT="${JARNFE_MIN_FLAT:-0.6}"
# Share of the band that must change when the art comes back.
MIN_CHANGED="${JARNFE_MIN_CHANGED:-0.2}"
INSTALL_APK="" ORIG_VALUE="" PREF_CHANGED=0 LIVE_WALLPAPER=""

usage_437() { sed -n '2,24p' "$VERIFY_DIR/verify-437-system-background.sh" | sed 's/^# \{0,1\}//'; }

parse_437_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_437; exit 0 ;;
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

has_pillow() { python3 -I -c 'import PIL' 2>/dev/null; }

# Writes $1 (true or false) for the switch, or removes it when empty, with the app stopped.
write_background() {
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
    ET.SubElement(root, "boolean", name=key, value=value)
print("<?xml version=\x271.0\x27 encoding=\x27utf-8\x27 standalone=\x27yes\x27 ?>")
print(ET.tostring(root, encoding="unicode"))
' "$BACKGROUND_KEY" "$value" <<<"$(read_prefs "$LOOK_PREFS")")" || return 1
  printf '%s\n' "$script" | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/$LOOK_PREFS.xml'" || return 1
  [ "$(read_prefs "$LOOK_PREFS" | parse_pref "$BACKGROUND_KEY")" = "$value" ]
}

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
}

restore_437() {
  [ "$PREF_CHANGED" = 1 ] || return 0
  PREF_CHANGED=0
  section "Restore"
  if write_background "$ORIG_VALUE"; then
    echo "  info  System background back to ${ORIG_VALUE:-default (on)}"
    sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  else
    echo "  WARN  could not restore System background (was ${ORIG_VALUE:-default})"
  fi
}

preflight_437() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  has_pillow || { echo "python3 with Pillow is needed" >&2; return 1; }
  read_prefs "$LOOK_PREFS" >/dev/null || {
    echo "run-as is unavailable for $PKG. On a release build flip Settings > Display > Now page > System background by hand." >&2
    return 1
  }
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  ORIG_VALUE="$(read_prefs "$LOOK_PREFS" | parse_pref "$BACKGROUND_KEY")"
  LIVE_WALLPAPER="$(read_prefs "$LOOK_PREFS" | parse_pref "$LIVE_WALLPAPER_KEY")"
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  $PKG, System background was ${ORIG_VALUE:-default (on)}, live wallpaper ${LIVE_WALLPAPER:-none}"
  echo "  info  Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY"
  echo "  info  output in $OUT_DIR"
}

focus_a_system() {
  ui_wait_role stage || return 1
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY" || return 1
  step_to_tab 0 "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return 1
  key "$KEY_RIGHT" && key "$KEY_LEFT" || return 1
  ui_dump && echo "  info  Stage focus: $(ui_focused)"
  pause 1.5
}

# Restarts Jarn FE with the switch at $1 and captures the Deck as $OUT_DIR/437-$1.png.
capture_with() {
  local value="$1"
  write_background "$value" || { fail "could not set System background to $value"; return 1; }
  PREF_CHANGED=1
  start_stage || { fail "could not bring Stage to the front"; return 1; }
  START_PID="$(sh_dev pidof "$PKG")"
  focus_a_system || { fail "could not focus a system on Stage"; return 1; }
  shot "$DECK_PHYSICAL" "$OUT_DIR/437-$value.png" || { fail "could not capture the Deck display"; return 1; }
  echo "  info  $value: $(python3 -I "$HELPER" flat "$OUT_DIR/437-$value.png")"
}

at_least() { python3 -I -c 'import sys; sys.exit(0 if float(sys.argv[1]) >= float(sys.argv[2]) else 1)' "$1" "$2"; }

check_off() {
  local flat
  flat="$(python3 -I "$HELPER" flat "$OUT_DIR/437-false.png" | tr ' ' '\n' | grep '^flat=' | cut -d= -f2)"
  if [ -n "$LIVE_WALLPAPER" ]; then
    echo "  info  live wallpaper $LIVE_WALLPAPER shows through, flat share $flat not judged"
  elif at_least "$flat" "$MIN_FLAT"; then
    pass "off: ${flat} of the band is plain theme background"
  else
    fail "off: only ${flat} of the band is plain background (art or cover wall still drawn?)"
  fi
}

check_on() {
  local changed on_flat
  changed="$(python3 -I "$HELPER" changed "$OUT_DIR/437-true.png" "$OUT_DIR/437-false.png" | cut -d= -f2)"
  on_flat="$(python3 -I "$HELPER" flat "$OUT_DIR/437-true.png" | tr ' ' '\n' | grep '^flat=' | cut -d= -f2)"
  if at_least "$changed" "$MIN_CHANGED"; then
    pass "on: ${changed} of the band differs from off, so the art is back"
  elif at_least "$on_flat" "$MIN_FLAT"; then
    skip "on: the focused system has no art or covers, so on and off look the same"
  else
    fail "on: only ${changed} of the band differs from off"
  fi
}

run_437() {
  section "System background on Now"
  capture_with true || return 0
  capture_with false || return 0
  check_off
  check_on
}

main_437() {
  parse_437_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-437)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_if_asked
  trap restore_437 EXIT
  trap 'restore_437; exit 130' INT TERM
  preflight_437 || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_437
  check_no_crashes
  check_lock_unchanged
  restore_437
  section "For a human"
  echo "  look  $OUT_DIR/437-false.png: the theme background, no system art or cover wall"
  echo "  look  open a game with the switch off: its own art still fills the background"
  summary
}

main_437 "$@"

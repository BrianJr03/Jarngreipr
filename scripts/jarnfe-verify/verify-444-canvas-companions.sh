#!/usr/bin/env bash
# #444: launching a game from a Canvas ROM tile with two companions shows "Open a companion" on
# the Deck, and B closes it.
#
#   scripts/jarnfe-verify/verify-444-canvas-companions.sh --launch-game [options]
#
#   --launch-game           Required. The check starts a real game (one press of A), see Safety.
#   --companions A,B        The two companion packages to give the game. Default: two installed
#                           apps with a launcher entry (Settings first). They are never opened.
#   --install APK           adb install -r this APK first (never uninstalls).
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when control moves between screens. The script never writes it.
#
# Debug builds only, and the Canvas needs at least one ROM tile on its first page. It restarts
# Jarn FE on Stage, steps the Deck to Canvas, moves control to the Deck and walks the tiles with
# the D-pad until a pinned ROM (read from pinned_roms_prefs) is focused. With Jarn FE stopped, that
# ROM gets the two companions in Classic's esde_prefs (game_companion_map, keyed by the tile's
# system and path, as the launch engine reads it). Then it restarts, walks to the same tile and
# presses A once. Once the game is in front:
#   1. the Deck display is captured (444-offer.png, and 444-tap-hint.png for the "Tap to choose"
#      hint, which uiautomator can't read while the game holds focus),
#   2. one tap on the sheet's bottom edge gives the Deck the controller, then a uiautomator dump
#      must show the picker sheet with two rows, focus inside it and no tap hint,
#   3. B closes it: the next dump has no picker,
#   4. no crash or ANR, and screen_focus_lock is unchanged.
#
# Safety: never uninstalls, clears data, or opens a companion. A is pressed exactly once, on the
# tile it read, which launches that game in its emulator; the game is left running. Other keys
# are D-pad, B, X, L1/R1 and L2/R2, only while Jarn FE holds focus. On exit the original
# game_companion_map is written back and Jarn FE restarted on Stage.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"
# shellcheck source=lib/companions.sh
source "$LIB_DIR/companions.sh"

TICKET=444
LAUNCH_FROM="a Canvas ROM tile"
PINNED_PREFS=pinned_roms_prefs
CANVAS_TAB=2
WALK_COLUMNS="${JARNFE_WALK_COLUMNS:-6}"
WALK_ROWS="${JARNFE_WALK_ROWS:-4}"
PINNED_ROMS=""

usage_444() { sed -n '2,32p' "$VERIFY_DIR/verify-444-canvas-companions.sh" | sed 's/^# \{0,1\}//'; }

parse_444_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --launch-game) CONSENT=1 ;;
      --companions) COMPANIONS="${2:?--companions needs two packages}"; shift ;;
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_444; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

# Prints "name<TAB>system<TAB>path" for every pinned ROM on any Canvas page.
read_pinned_roms() {
  read_prefs "$PINNED_PREFS" | python3 -I -c '
import json, sys
import xml.etree.ElementTree as ET
raw = sys.stdin.read()
root = ET.fromstring(raw) if raw.strip() else ET.Element("map")
for item in root:
    if item.tag != "string" or not (item.get("name") or "").startswith("pinned_roms_"):
        continue
    try:
        roms = json.loads(item.text or "[]")
    except ValueError:
        continue
    for rom in roms:
        if rom.get("name") and rom.get("systemName") and rom.get("path"):
            print("\t".join((rom["name"], rom["systemName"], rom["path"])))
'
}

# Prints "system<TAB>path<TAB>" for the pinned ROM whose name is the focused label $1.
rom_row_for_label() {
  local name system path
  while IFS=$'\t' read -r name system path; do
    [ -n "$name" ] || continue
    if [ "$1" = "$name" ] || text_has "$1" "$name"; then
      printf '%s\t%s\t\n' "$system" "$path"
      return 0
    fi
  done <<<"$PINNED_ROMS"
  return 1
}

# Game art tiles carry no label, so with one pinned ROM an unlabelled focused tile is taken as it.
focused_rom() {
  local focused row name system path
  ui_dump || return 1
  focused="$(ui_focused)"
  if [ -z "$focused" ] && [ "$(grep -c . <<<"$PINNED_ROMS")" = 1 ] && text_has "$(cat "$UI_XML")" 'focused="true"'; then
    IFS=$'\t' read -r name system path <<<"$PINNED_ROMS"
    FOCUSED_COVER="$name" FOCUSED_ROW="$(printf '%s\t%s\t' "$system" "$path")"
    return 0
  fi
  row="$(rom_row_for_label "$focused")" || return 1
  FOCUSED_COVER="$focused" FOCUSED_ROW="$row"
}

# Rows left to right, then down. The same walk after a restart lands on the same tile.
walk_to_rom_tile() {
  # Not a UI dump: animated system art keeps Stage from going idle, so uiautomator times out there.
  wait_top_display "$STAGE_DISPLAY" || return 1
  step_to_tab "$CANVAS_TAB" "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return 1
  activate_display "$DECK_DISPLAY" || return 1
  ui_wait_role deck || return 1
  local row column
  for ((row = 0; row < WALK_ROWS; row++)); do
    for ((column = 0; column < WALK_COLUMNS; column++)); do
      focused_rom && return 0
      key "$KEY_RIGHT" || return 1
    done
    press_times "$KEY_LEFT" "$WALK_COLUMNS" && key "$KEY_DOWN" || return 1
  done
  return 1
}

restart_on_rom_tile() {
  start_stage || { fail "could not bring Stage to the front"; return 1; }
  FOCUSED_COVER="" FOCUSED_ROW=""
  walk_to_rom_tile || { fail "could not focus a ROM tile on the Canvas"; return 1; }
}

main_444() {
  parse_444_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-444)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_if_asked
  trap restore_companions EXIT
  trap 'restore_companions; exit 130' INT TERM
  preflight_companions || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  PINNED_ROMS="$(read_pinned_roms)"
  [ -n "$PINNED_ROMS" ] || { echo "No ROM tile is pinned to the Canvas. Add one first." >&2; exit 2; }
  run_companion_launch restart_on_rom_tile
  check_no_crashes
  check_lock_unchanged
  restore_companions
  section "For a human"
  echo "  look  $OUT_DIR/444-tap-hint.png: the sheet shows \"Tap to choose\" as plain text, no key cap"
  echo "  look  $OUT_DIR/444-offer.png: \"$PICKER_TITLE\" sits over the Canvas while the game runs"
  echo "  look  pick a companion by touch: it opens and the sheet goes away"
  echo "  look  a ROM tile with one companion: that companion opens by itself, no sheet"
  echo "  look  on one screen: launch from the Canvas in the Deck panel, the panel shows the picker"
  summary
}

main_444 "$@"

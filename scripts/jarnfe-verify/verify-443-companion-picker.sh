#!/usr/bin/env bash
# #443: launching a game with two companions shows "Open a companion" on the Deck, from the system
# view of Now too, and B closes it.
#
#   scripts/jarnfe-verify/verify-443-companion-picker.sh --launch-game [options]
#
#   --launch-game           Required. The check starts a real game (one press of A), see Safety.
#   --system-row R          Rows down from the first system (default 0).
#   --system-steps N        Systems right within that row (default 0). Together they pick a system
#                           whose emulator is set and installed.
#   --companions A,B        The two companion packages to give the game. Default: two installed
#                           apps with a launcher entry (Settings first). They are never opened.
#   --install APK           adb install -r this APK first (never uninstalls).
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when control moves between screens. The script never writes it.
#
# Debug builds only. It restarts Jarn FE on Stage, steps Stage to Systems and the Deck to Now,
# focuses a system, moves control to the Deck and reads the focused cover on the system view's
# game row. With Jarn FE stopped, that game gets the two companions in Classic's esde_prefs
# (game_companion_map, the same map the game's Companion row writes, under both of its path
# keys). Then it restarts, walks to the same cover and presses A once. Once the game is in front:
#   1. the Deck display is captured (443-offer.png, and 443-tap-hint.png for the "Tap to choose"
#      hint, which uiautomator can't read while the game holds focus),
#   2. one tap on the sheet's bottom edge gives the Deck the controller, then a uiautomator dump
#      must show the picker sheet with two rows, focus inside it and no tap hint,
#   3. B closes it: the next dump has no picker,
#   4. no crash or ANR, and screen_focus_lock is unchanged.
#
# Safety: never uninstalls, clears data, or opens a companion. A is pressed exactly once, on the
# cover it read, which launches that game in its emulator; the game is left running. Other keys
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

TICKET=443
LAUNCH_FROM="the system view"
DB_NAME=jarngreipr_library
SYSTEM_STEPS=0
SYSTEM_ROWS=0

usage_443() { sed -n '2,34p' "$VERIFY_DIR/verify-443-companion-picker.sh" | sed 's/^# \{0,1\}//'; }

parse_443_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --launch-game) CONSENT=1 ;;
      --system-row) SYSTEM_ROWS="${2:?--system-row needs a number}"; shift ;;
      --system-steps) SYSTEM_STEPS="${2:?--system-steps needs a number}"; shift ;;
      --companions) COMPANIONS="${2:?--companions needs two packages}"; shift ;;
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_443; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

# Prints "system<TAB>relative<TAB>absolute" for the game whose title is $1, from a host copy of the
# library database.
game_row_for_title() {
  local dir="$OUT_DIR/db" part
  mkdir -p "$dir"
  for part in "" -wal -shm; do
    adb exec-out run-as "$PKG" cat "databases/$DB_NAME$part" >"$dir/$DB_NAME$part" 2>/dev/null || rm -f "$dir/$DB_NAME$part"
  done
  python3 -I -c '
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
title = sys.argv[2].strip()
rows = db.execute("select systemId, relativePath, coalesce(absolutePath, \"\"), displayName from games").fetchall()
for exact in (True, False):
    for system, relative, absolute, name in rows:
        if name and (name == title if exact else name in title):
            print(f"{system}\t{relative}\t{absolute}")
            sys.exit(0)
' "$dir/$DB_NAME" "$1" 2>/dev/null
}

# Systems on Stage, Now on the Deck, a system focused, then control on the Deck's game row.
walk_to_cover() {
  # Not a UI dump: animated system art keeps Stage from going idle, so uiautomator times out there.
  wait_top_display "$STAGE_DISPLAY" || return 1
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY" || return 1
  step_to_tab 0 "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return 1
  # A cold start focuses the first system, so rows down and steps right always reach the same one.
  press_times "$KEY_DOWN" "$SYSTEM_ROWS" || return 1
  if [ "$SYSTEM_STEPS" -gt 0 ]; then
    press_times "$KEY_RIGHT" "$SYSTEM_STEPS" || return 1
  else
    key "$KEY_RIGHT" && key "$KEY_LEFT" || return 1
  fi
  pause 2
  activate_display "$DECK_DISPLAY" || return 1
  ui_wait_role deck || return 1
  local attempt focused row
  for attempt in 1 2 3; do
    ui_dump && focused="$(ui_focused)"
    row="$(game_row_for_title "$focused")"
    if [ -n "$row" ]; then
      FOCUSED_COVER="$focused" FOCUSED_ROW="$row"
      return 0
    fi
    key "$KEY_DOWN" || return 1
    pause 1
  done
  return 1
}

restart_on_cover() {
  sh_dev am force-stop "$PKG"
  start_stage || { fail "could not bring Stage to the front"; return 1; }
  FOCUSED_COVER="" FOCUSED_ROW=""
  walk_to_cover || { fail "could not focus a game on the system view's row"; return 1; }
}

main_443() {
  parse_443_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-443)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_if_asked
  trap restore_companions EXIT
  trap 'restore_companions; exit 130' INT TERM
  preflight_companions || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_companion_launch restart_on_cover
  check_no_crashes
  check_lock_unchanged
  restore_companions
  section "For a human"
  echo "  look  $OUT_DIR/443-offer.png: \"$PICKER_TITLE\" sits over the Deck while the game runs"
  echo "  look  $OUT_DIR/443-tap-hint.png: the sheet shows \"Tap to choose\" as plain text, no key cap"
  echo "  look  pick a companion by touch: it opens and the sheet goes away"
  echo "  look  launch from Continue playing, the Resume poster and a game page: the sheet shows once"
  echo "  look  on one screen (no Deck display): the Deck panel shows the picker over the game's return"
  summary
}

main_443 "$@"

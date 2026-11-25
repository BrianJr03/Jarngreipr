#!/usr/bin/env bash
# #443: launching a game with two companions shows "Open a companion" on the Deck, from the system
# view of Now too, and B closes it.
#
#   scripts/jarnfe-verify/verify-443-companion-picker.sh --launch-game [options]
#
#   --launch-game           Required. The check starts a real game (one press of A), see Safety.
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
#   1. the Deck display is captured (443-offer.png),
#   2. one tap on the sheet's bottom edge gives the Deck the controller, then a uiautomator dump
#      must show the picker sheet with two rows and focus inside it,
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

HELPER="$LIB_DIR/companionquery.py"
ESDE_PREFS=esde_prefs
COMPANION_KEY=game_companion_map
DB_NAME=jarngreipr_library
PICKER_TITLE="Open a companion"
KEY_A=96
GAME_WAIT_S="${JARNFE_GAME_WAIT_S:-20}"
INSTALL_APK="" CONSENT=0 COMPANIONS="" ORIG_JSON="" ORIG_PRESENT=0 PREF_CHANGED=0
COVER_TITLE="" GAME_ROW=""

usage_443() { sed -n '2,31p' "$VERIFY_DIR/verify-443-companion-picker.sh" | sed 's/^# \{0,1\}//'; }

parse_443_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --launch-game) CONSENT=1 ;;
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

install_if_asked() {
  [ -n "$INSTALL_APK" ] || return 0
  [ -f "$INSTALL_APK" ] || { echo "no such APK: $INSTALL_APK" >&2; exit 2; }
  echo "Installing $INSTALL_APK"
  adb install -r "$INSTALL_APK" || { echo "adb install failed (signature mismatch? not uninstalling)" >&2; exit 2; }
}

has_launcher() { text_has "$(sh_dev cmd package resolve-activity --brief "$1")" "$1/"; }

# Two launchable apps that are neither Jarngreipr nor already the game's emulator.
pick_companions() {
  if [ -n "$COMPANIONS" ]; then
    local one="${COMPANIONS%%,*}" two="${COMPANIONS#*,}"
    has_launcher "$one" && has_launcher "$two" && [ "$one" != "$two" ] && return 0
    echo "--companions needs two different installed apps with a launcher entry" >&2
    return 1
  fi
  local found="" candidate
  for candidate in com.android.settings $(sh_dev cmd package query-activities --brief \
      -a android.intent.action.MAIN -c android.intent.category.LAUNCHER | grep / | cut -d/ -f1 | sort -u); do
    case "$candidate" in jr.brian.home*) continue ;; esac
    has_launcher "$candidate" || continue
    text_has_line "${found//,/$'\n'}" "$candidate" && continue
    found="${found:+$found,}$candidate"
    [ "${found//[^,]/}" = "," ] && break
  done
  COMPANIONS="$found"
  [ "${COMPANIONS//[^,]/}" = "," ]
}

# Prints the raw JSON under game_companion_map, or nothing. Exit 3 when the key is absent.
read_companion_map() {
  read_prefs "$ESDE_PREFS" | python3 -I -c '
import sys
import xml.etree.ElementTree as ET
raw = sys.stdin.read()
root = ET.fromstring(raw) if raw.strip() else ET.Element("map")
for item in root:
    if item.tag == "string" and item.get("name") == sys.argv[1]:
        print(item.text or "")
        sys.exit(0)
sys.exit(3)
' "$COMPANION_KEY"
}

# Writes $1 as the whole game_companion_map JSON with Jarn FE stopped, or removes it for "remove".
write_companion_map() {
  local json="$1" mode="${2:-set}" script
  sh_dev am force-stop "$PKG" >/dev/null
  script="$(python3 -I -c '
import sys
import xml.etree.ElementTree as ET
raw, key, value, mode = sys.stdin.read(), sys.argv[1], sys.argv[2], sys.argv[3]
try:
    root = ET.fromstring(raw) if raw.strip() else ET.Element("map")
except ET.ParseError:
    root = ET.Element("map")
for item in list(root):
    if item.get("name") == key:
        root.remove(item)
if mode != "remove":
    ET.SubElement(root, "string", name=key).text = value
print("<?xml version=\x271.0\x27 encoding=\x27utf-8\x27 standalone=\x27yes\x27 ?>")
print(ET.tostring(root, encoding="unicode"))
' "$COMPANION_KEY" "$json" "$mode" <<<"$(read_prefs "$ESDE_PREFS")")" || return 1
  printf '%s\n' "$script" | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/$ESDE_PREFS.xml'"
}

# The original map plus both path keys of GAME_ROW (system, relative, absolute) set to the pair.
with_companions() {
  python3 -I -c '
import json, sys
data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
system, relative, absolute = sys.argv[2].split("\t")
for path in {relative, absolute} - {""}:
    data[f"{system}/{path}"] = sys.argv[3].split(",")
print(json.dumps(data))
' "$ORIG_JSON" "$GAME_ROW" "$COMPANIONS"
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

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
}

# Systems on Stage, Now on the Deck, a system focused, then control on the Deck's game row.
walk_to_cover() {
  ui_wait_role stage || return 1
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY" || return 1
  step_to_tab 0 "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return 1
  key "$KEY_RIGHT" && key "$KEY_LEFT" || return 1
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
  start_stage || { fail "could not bring Stage to the front"; return 1; }
  FOCUSED_COVER="" FOCUSED_ROW=""
  walk_to_cover || { fail "could not focus a game on the system view's row"; return 1; }
}

jarn_owns_top() { case "$(focused_app "$TOP_DISPLAY")" in "$PKG"/*|"") return 0 ;; *) return 1 ;; esac; }

wait_for_game() {
  local deadline=$((SECONDS + GAME_WAIT_S))
  while [ "$SECONDS" -lt "$deadline" ]; do
    jarn_owns_top || return 0
    sleep 1
  done
  return 1
}

# The sheet sits at the bottom, so its bottom padding takes a touch without pressing a row.
tap_sheet_edge() {
  local size width height density
  size="$(sh_dev wm size -d "$DECK_DISPLAY" | awk -F': ' '/size/ { value = $2 } END { print value }')"
  density="$(sh_dev wm density -d "$DECK_DISPLAY" | awk -F': ' '/density/ { value = $2 } END { print value }')"
  width="${size%x*}" height="${size#*x}"
  [ -n "$width" ] && [ -n "$height" ] || return 1
  sh_dev input -d "$DECK_DISPLAY" tap "$((width / 2))" "$((height - ${density:-320} * 6 / 160))" >/dev/null
  pause 1.5
}

picker_field() { python3 -I "$HELPER" "$UI_XML" picker "$PICKER_TITLE" | tr ' ' '\n' | grep "^$1=" | cut -d= -f2; }

check_offer() {
  shot "$DECK_PHYSICAL" "$OUT_DIR/443-offer.png" && echo "  info  Deck while the game runs: $OUT_DIR/443-offer.png"
  tap_sheet_edge || { fail "could not read the Deck display size for the tap"; return 1; }
  if ! ui_wait_role deck; then
    fail "the Deck did not take the controller after the tap"
    return 1
  fi
  cp "$UI_XML" "$OUT_DIR/443-offer.xml"
  if [ "$(picker_field shown)" != 1 ]; then
    fail "no companion picker on the Deck after launching from the system view"
    return 1
  fi
  pass "the companion picker is on the Deck"
  local rows
  rows="$(picker_field rows)"
  [ "${rows:-0}" -ge 2 ] && pass "it lists $rows companions" || fail "it lists ${rows:-no} rows, wanted 2"
  [ "$(picker_field focused)" = 1 ] && pass "focus is inside the picker" \
    || fail "focus is not inside the picker ($(ui_focused))"
}

check_back_closes() {
  key "$KEY_B" || return 1
  pause 1
  ui_wait_role deck || { fail "could not read the Deck after B"; return 1; }
  cp "$UI_XML" "$OUT_DIR/443-after-back.xml"
  [ "$(picker_field shown)" = 0 ] && pass "B closed the picker" || fail "the picker is still open after B"
  shot "$DECK_PHYSICAL" "$OUT_DIR/443-after-back.png"
}

restore_443() {
  [ "$PREF_CHANGED" = 1 ] || return 0
  PREF_CHANGED=0
  section "Restore"
  if [ "$ORIG_PRESENT" = 1 ]; then write_companion_map "$ORIG_JSON"; else write_companion_map "" remove; fi \
    && echo "  info  game_companion_map restored" \
    || echo "  WARN  could not restore game_companion_map, saved copy in $OUT_DIR/443-original.json"
  sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
}

preflight_443() {
  section "Preflight"
  [ "$CONSENT" = 1 ] || { echo "This check launches a game. Pass --launch-game to allow it." >&2; return 1; }
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_prefs "$ESDE_PREFS" >/dev/null || {
    echo "run-as is unavailable for $PKG. On a release build add two companions to a game by hand." >&2
    return 1
  }
  pick_companions || { echo "could not find two launchable apps to use as companions" >&2; return 1; }
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  ORIG_JSON="$(read_companion_map)" && ORIG_PRESENT=1
  printf '%s\n' "$ORIG_JSON" >"$OUT_DIR/443-original.json"
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  $PKG, Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY"
  echo "  info  companions: $COMPANIONS"
  echo "  info  output in $OUT_DIR"
}

run_443() {
  section "Pick a game on the system view"
  restart_on_cover || return 0
  COVER_TITLE="$FOCUSED_COVER" GAME_ROW="$FOCUSED_ROW"
  echo "  info  game: $COVER_TITLE (${GAME_ROW//$'\t'/ | })"
  write_companion_map "$(with_companions)" || { fail "could not write the companions"; return 0; }
  PREF_CHANGED=1
  section "Launch from the system view"
  restart_on_cover || return 0
  START_PID="$(sh_dev pidof "$PKG")"
  [ "$FOCUSED_COVER" = "$COVER_TITLE" ] || { fail "focus landed on $FOCUSED_COVER, not $COVER_TITLE"; return 0; }
  EXTRA_KEYS="$KEY_A"
  key "$KEY_A" || return 0
  EXTRA_KEYS=""
  if ! wait_for_game; then
    ui_dump && cp "$UI_XML" "$OUT_DIR/443-no-game.xml"
    skip "the game did not start within ${GAME_WAIT_S}s (no emulator set?), see $OUT_DIR/443-no-game.xml"
    return 0
  fi
  echo "  info  in front on top: $(focused_app "$TOP_DISPLAY")"
  section "Companion picker"
  check_offer && check_back_closes
}

main_443() {
  parse_443_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-443)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  FOCUSED_COVER="" FOCUSED_ROW=""
  select_device
  install_if_asked
  trap restore_443 EXIT
  trap 'restore_443; exit 130' INT TERM
  preflight_443 || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_443
  check_no_crashes
  check_lock_unchanged
  restore_443
  section "For a human"
  echo "  look  $OUT_DIR/443-offer.png: \"$PICKER_TITLE\" sits over the Deck while the game runs"
  echo "  look  pick a companion by touch: it opens and the sheet goes away"
  echo "  look  launch from Continue playing, the Resume poster and a game page: the sheet shows once"
  echo "  look  on one screen (no Deck display): the Deck panel shows the picker over the game's return"
  summary
}

main_443 "$@"

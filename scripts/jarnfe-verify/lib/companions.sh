#!/usr/bin/env bash
# Shared by the companion picker checks (#443, #444). Source after run.sh, then set TICKET (the
# file prefix) and LAUNCH_FROM (where the launch starts, for messages). The caller's walk sets
# FOCUSED_COVER and FOCUSED_ROW ("system<TAB>path<TAB>other path or empty") on the game to launch.

HELPER="$LIB_DIR/companionquery.py"
ESDE_PREFS=esde_prefs
COMPANION_KEY=game_companion_map
PICKER_TITLE="Open a companion"
TAP_HINT_TAG=jarn_companion_tap_hint
KEY_A=96
GAME_WAIT_S="${JARNFE_GAME_WAIT_S:-20}"
INSTALL_APK="" CONSENT=0 COMPANIONS="" ORIG_JSON="" ORIG_PRESENT=0 PREF_CHANGED=0
COVER_TITLE="" GAME_ROW="" FOCUSED_COVER="" FOCUSED_ROW=""

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

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
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

tap_hint_shown() { python3 -I "$HELPER" "$UI_XML" tag "$TAP_HINT_TAG"; }

# Before the tap the game holds focus, so uiautomator can't read the Deck: the hint is only
# captured here, for a human. After the tap the dump must show it gone.
check_offer() {
  shot "$DECK_PHYSICAL" "$OUT_DIR/$TICKET-offer.png" && echo "  info  Deck while the game runs: $OUT_DIR/$TICKET-offer.png"
  shot "$DECK_PHYSICAL" "$OUT_DIR/$TICKET-tap-hint.png" \
    && echo "  info  "Tap to choose" hint before the Deck has control: $OUT_DIR/$TICKET-tap-hint.png"
  tap_sheet_edge || { fail "could not read the Deck display size for the tap"; return 1; }
  if ! ui_wait_role deck; then
    fail "the Deck did not take the controller after the tap"
    return 1
  fi
  cp "$UI_XML" "$OUT_DIR/$TICKET-offer.xml"
  if [ "$(picker_field shown)" != 1 ]; then
    fail "no companion picker on the Deck after launching from $LAUNCH_FROM"
    return 1
  fi
  pass "the companion picker is on the Deck"
  local rows
  rows="$(picker_field rows)"
  [ "${rows:-0}" -ge 2 ] && pass "it lists $rows companions" || fail "it lists ${rows:-no} rows, wanted 2"
  [ "$(picker_field focused)" = 1 ] && pass "focus is inside the picker" \
    || fail "focus is not inside the picker ($(ui_focused))"
  [ "$(tap_hint_shown)" = 0 ] && pass "the tap hint is gone once the Deck has control" \
    || fail "the tap hint still shows with control on the Deck"
}

check_back_closes() {
  key "$KEY_B" || return 1
  pause 1
  ui_wait_role deck || { fail "could not read the Deck after B"; return 1; }
  cp "$UI_XML" "$OUT_DIR/$TICKET-after-back.xml"
  [ "$(picker_field shown)" = 0 ] && pass "B closed the picker" || fail "the picker is still open after B"
  shot "$DECK_PHYSICAL" "$OUT_DIR/$TICKET-after-back.png"
}

restore_companions() {
  [ "$PREF_CHANGED" = 1 ] || return 0
  PREF_CHANGED=0
  section "Restore"
  if [ "$ORIG_PRESENT" = 1 ]; then write_companion_map "$ORIG_JSON"; else write_companion_map "" remove; fi \
    && echo "  info  game_companion_map restored" \
    || echo "  WARN  could not restore game_companion_map, saved copy in $OUT_DIR/$TICKET-original.json"
  sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
}

preflight_companions() {
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
  printf '%s\n' "$ORIG_JSON" >"$OUT_DIR/$TICKET-original.json"
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  $PKG, Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY"
  echo "  info  companions: $COMPANIONS"
  echo "  info  output in $OUT_DIR"
}

# Gives the game that $1 (a restart and a walk to it) focuses two companions, walks there again
# and presses A once, then checks the picker on the Deck and that B closes it.
run_companion_launch() {
  local walk="$1"
  section "Pick a game ($LAUNCH_FROM)"
  "$walk" || return 0
  COVER_TITLE="$FOCUSED_COVER" GAME_ROW="$FOCUSED_ROW"
  echo "  info  game: $COVER_TITLE (${GAME_ROW//$'\t'/ | })"
  write_companion_map "$(with_companions)" || { fail "could not write the companions"; return 0; }
  PREF_CHANGED=1
  section "Launch from $LAUNCH_FROM"
  "$walk" || return 0
  START_PID="$(sh_dev pidof "$PKG")"
  [ "$FOCUSED_COVER" = "$COVER_TITLE" ] || { fail "focus landed on $FOCUSED_COVER, not $COVER_TITLE"; return 0; }
  EXTRA_KEYS="$KEY_A"
  key "$KEY_A" || return 0
  EXTRA_KEYS=""
  if ! wait_for_game; then
    ui_dump && cp "$UI_XML" "$OUT_DIR/$TICKET-no-game.xml"
    skip "the game did not start within ${GAME_WAIT_S}s (no emulator set?), see $OUT_DIR/$TICKET-no-game.xml"
    return 0
  fi
  echo "  info  in front on top: $(focused_app "$TOP_DISPLAY")"
  section "Companion picker"
  check_offer && check_back_closes
}

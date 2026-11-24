#!/usr/bin/env bash
# Tests the suite's parsing and safety logic against the fake adb. Never touches a real device.
set -uo pipefail

TEST_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SUITE="$TEST_DIR/../run.sh"
WORK="$(mktemp -d -t jarnfe-verify-test)"
trap 'rm -rf "$WORK"' EXIT
chmod +x "$TEST_DIR/adb"
export PATH="$TEST_DIR:$PATH" JARNFE_SETTLE=0
unset ANDROID_SERIAL
# shellcheck source=../lib/parse.sh
source "$TEST_DIR/../lib/parse.sh"

passed=0 failed=0
ok() { echo "ok    $*"; passed=$((passed + 1)); }
bad() { echo "FAIL  $*"; failed=$((failed + 1)); }
expect() { if [ "$2" = "$3" ]; then ok "$1"; else bad "$1 (wanted '$2', got '$3')"; fi; }

# Runs the suite with FAKE_* assignments before --, suite arguments after. Sets OUTPUT and CODE.
scenario() {
  local name="$1" dir="$WORK/$1"
  shift
  mkdir -p "$dir"
  local vars=()
  while [ $# -gt 0 ] && [ "$1" != "--" ]; do vars+=("$1"); shift; done
  [ $# -gt 0 ] && shift
  LOG="$dir/adb.log"
  : >"$LOG"
  OUTPUT="$(env FAKE_STATE="$dir/state.json" FAKE_LOG="$LOG" ${vars[@]+"${vars[@]}"} \
    "$BASH" "${SCRIPT:-$SUITE}" --out "$dir/out" "$@" 2>&1)"
  CODE=$?
  STATE="$dir/state.json"
  printf '%s\n' "$OUTPUT" >"$dir/output.txt"
}

state_field() { python3 -I -c 'import json,sys; print(json.load(open(sys.argv[1]))[sys.argv[2]])' "$STATE" "$1"; }
has() { if grep -qF -- "$2" <<<"$OUTPUT"; then ok "$1"; else bad "$1 (missing: $2)"; fi; }
has_text() { if grep -qF -- "$2" <<<"$3"; then ok "$1"; else bad "$1 (missing: $2)"; fi; }
lacks() { if grep -qF -- "$2" <<<"$OUTPUT"; then bad "$1 (found: $2)"; else ok "$1"; fi; }

# No destructive adb command and no key outside the allow list, in any scenario.
assert_safe() {
  if grep -qE 'uninstall|pm clear|settings put|settings delete|monkey|bmgr|install ' "$LOG"; then
    bad "$1: forbidden command sent"
  else
    ok "$1: no forbidden command"
  fi
  local keys
  keys="$(grep -oE 'keyevent [0-9]+' "$LOG" | awk '{print $2}')"
  if [ -n "$keys" ] && grep -qvE '^(19|20|21|22|97|99|102|103|104|105|108)$' <<<"$keys"; then
    bad "$1: key outside the allow list"
  else
    ok "$1: only allowed keys"
  fi
}

test_parsers() {
  local window='Display: mDisplayId=0 stacks=1
  mFocusedApp=ActivityRecord{a1 u0 pkg.one/.Stage t1}
Display: mDisplayId=4 stacks=1
  mFocusedApp=ActivityRecord{b2 u0 pkg.two/.Deck t2}'
  expect "focused app on 0" "pkg.one/.Stage" "$(printf '%s\n' "$window" | parse_focused_app 0)"
  expect "focused app on 4" "pkg.two/.Deck" "$(printf '%s\n' "$window" | parse_focused_app 4)"
  expect "null focused app" "" "$(printf 'Display: mDisplayId=4 x\n  mFocusedApp=null\n' | parse_focused_app 4)"
  expect "top display" "4" "$(printf 'x\n mTopFocusedDisplayId=4\n mTopFocusedDisplayId=0\n' | parse_top_display)"
  local activities='Display #0 (activities from top to bottom):
  topResumedActivity=ActivityRecord{a u0 pkg/.Stage t1}
Display #4 (activities from top to bottom):
  ResumedActivity: ActivityRecord{b u0 pkg/.Deck t2}'
  expect "resumed on 0" "pkg/.Stage" "$(printf '%s\n' "$activities" | parse_resumed 0)"
  expect "resumed on 4" "pkg/.Deck" "$(printf '%s\n' "$activities" | parse_resumed 4)"
  expect "string pref" "Top" "$(echo '<map><string name="m">Top</string></map>' | parse_pref m)"
  expect "boolean pref" "false" "$(echo '<map><boolean name="h" value="false" /></map>' | parse_pref h)"
  expect "missing pref" "" "$(echo '<map></map>' | parse_pref h)"
  local row
  for row in "Off:1:0" "Off:2:0" "Top:1:0" "Top:2:1" "Bottom:2:0" "Bottom:1:1" "Follow:1:1" ":1:1"; do
    IFS=: read -r mode value want <<<"$row"
    focus_sync_is_inert "$mode" "$value"
    expect "focus sync inert for '$mode' at $value" "$want" "$?"
  done
}

test_clean_run() {
  scenario clean FAKE_MODE=Top --
  expect "clean run exits 0" 0 "$CODE"
  has "clean run summary" "FAIL 0"
  has "switching checked" "X moved focus to display 4"
  has "dpad deck checked" "Deck active: focus ring moved on the active screen only"
  has "dpad stage checked" "Stage active: focus ring moved on the active screen only"
  has "tab focus checked" "focusing a tab selects it: Favorites"
  has "settings checked" "R2 opens section: Display"
  has "settings exit checked" "B leaves Settings"
  assert_safe "clean"
  expect "restored active screen" 0 "$(state_field active)"
  expect "restored library tab" 1 "$(state_field library)"
  expect "left Settings" library "$(state_field route)"
  expect "lock untouched" 1 "$(state_field lock)"
}

test_restores_start() {
  scenario restore-tab FAKE_DECK_TAB=2 --
  expect "restore run passes" 0 "$CODE"
  expect "restored Deck tab" 2 "$(state_field deck)"
  expect "restored library tab" 1 "$(state_field library)"
  expect "restored Stage as active" 0 "$(state_field active)"
  scenario restore-deck FAKE_START_DISPLAY=4 FAKE_DECK_TAB=2 --
  expect "Deck-start run passes" 0 "$CODE"
  expect "restored Deck as active" 4 "$(state_field active)"
  expect "restored Deck tab from Deck start" 2 "$(state_field deck)"
  expect "restored library tab from Deck start" 1 "$(state_field library)"
}

test_focus_lock_gate() {
  scenario follow FAKE_MODE=Follow FAKE_FOLLOW_WRITES=1 --
  expect "Follow mode has no failures" 0 "$CODE"
  has "Follow holds back switching" "may rewrite screen_focus_lock"
  if grep -qE 'keyevent (108|99)' "$LOG"; then bad "Follow mode sent X or Start"; else ok "Follow mode sent no X or Start"; fi
  expect "Follow leaves lock alone" 1 "$(state_field lock)"
  scenario default-mode FAKE_MODE=MISSING FAKE_FOLLOW_WRITES=1 --
  has "missing pref counts as Follow" "Focus Lock mode Follow"
  if grep -qE 'keyevent (108|99)' "$LOG"; then bad "default mode sent X or Start"; else ok "default mode sent no X or Start"; fi
  scenario runas FAKE_RUNAS_FAIL=1 FAKE_FOLLOW_WRITES=1 --
  has "unreadable mode holds back" "Focus Lock mode is unreadable"
  if grep -qE 'keyevent (108|99)' "$LOG"; then bad "unreadable mode sent X or Start"; else ok "unreadable mode sent no X or Start"; fi
  scenario top-mismatch FAKE_MODE=Top FAKE_LOCK=2 --
  has "Top mode at 2 holds back" "Focus Lock mode Top may rewrite"
  scenario opt-in FAKE_MODE=Follow FAKE_FOLLOW_WRITES=1 -- --allow-app-focus-sync
  has "opt-in runs switching" "X moved focus to display 4"
  has "opt-in lock returns" "screen_focus_lock still 1"
  assert_safe "opt-in"
}

test_failures_are_caught() {
  scenario leak FAKE_DPAD_LEAK=1 --
  expect "D-pad leak fails" 1 "$CODE"
  has "D-pad leak reported" "focus reached the inactive screen"
  scenario crash FAKE_CRASH=1 --
  expect "crash fails" 1 "$CODE"
  has "crash reported" "crash logged"
  scenario nodeck FAKE_NO_DECK=1 --
  expect "missing Deck fails" 1 "$CODE"
  has "missing Deck reported" "Deck activity not resumed"
  scenario release FAKE_RELEASE_LEAK=1 --
  expect "installed release leak fails" 1 "$CODE"
  has "release leak reported" "declares Jarn FE components"
}

test_input_stops_when_focus_is_lost() {
  scenario foreign FAKE_FOREIGN_AFTER=99 --
  expect "focus loss fails" 1 "$CODE"
  has "focus loss reported" "Jarn FE lost focus"
  has "focus loss summary" "Input was stopped early"
  local after
  after="$(awk '/keyevent 99/ { seen = 1; next } seen && /keyevent/' "$LOG" | wc -l | tr -d ' ')"
  expect "no key sent after focus loss" 0 "$after"
}

test_inconclusive_is_skip() {
  scenario noisy FAKE_NOISY=1 -- --only dpad
  expect "animated inactive screen passes" 0 "$CODE"
  lacks "animated screen is not a leak" "focus reached the inactive screen"
  scenario nomove FAKE_NO_FOCUS_MOVE=1 --
  has "no focus move is a skip" "no visible focus move"
  scenario noui FAKE_UI_FAIL=1 --
  expect "unreadable UI does not fail" 0 "$CODE"
  has "unreadable UI is a skip" "could not be read"
  scenario deckui FAKE_DECK_UNREADABLE=1 --
  expect "unreadable Deck does not fail" 0 "$CODE"
  has "unreadable Deck uses pixels for Settings" "by pixels"
  scenario hints-off FAKE_HINTS=false --
  has "hints off is a skip" "hint bar is turned off"
}

test_long_dumps_do_not_break_pipes() {
  scenario huge FAKE_HUGE_DUMPS=1 -- --only displays,release
  expect "long dumps pass" 0 "$CODE"
  has "top screen found in a long dump" "top screen present"
  has "bottom screen found in a long dump" "bottom screen present"
  lacks "no broken pipe" "Broken pipe"
  scenario huge-crash FAKE_HUGE_DUMPS=1 FAKE_CRASH=1 -- --only crashes
  expect "crash in a long log fails" 1 "$CODE"
  has "crash in a long log reported" "crash logged"
  scenario huge-anr FAKE_HUGE_DUMPS=1 FAKE_ANR=1 -- --only crashes
  expect "ANR in a long log fails" 1 "$CODE"
  has "ANR in a long log reported" "ANR logged"
  scenario huge-release FAKE_HUGE_DUMPS=1 FAKE_RELEASE_LEAK=1 -- --only release
  expect "release leak in a long dump fails" 1 "$CODE"
  has "release leak in a long dump reported" "declares Jarn FE components"
}

test_deck_follows_stage() {
  scenario follows FAKE_DECK_FOLLOWS=1 -- --only dpad
  expect "Deck content following Stage passes" 0 "$CODE"
  has "Stage direction passes" "Stage active: focus ring moved on the active screen only"
  scenario follows-leak FAKE_DECK_FOLLOWS=1 FAKE_DPAD_LEAK=1 -- --only dpad
  expect "a ring on the Deck still fails" 1 "$CODE"
  has "ring leak reported" "focus reached the inactive screen"
}

test_stale_ui_window() {
  scenario ui-lag FAKE_UI_LAG=3 -- --only hints,tabs,settings
  expect "lagging dump passes" 0 "$CODE"
  has "lagging dump reads Deck hints" "Deck shows the Back hint"
  has "lagging dump reads Deck tabs" "R2 moves one Deck tab: Canvas"
  has "lagging dump reads Settings" "R2 opens section: Display"
  lacks "lagging dump has no Library skip" "not on the Library screen"
  scenario ui-stuck FAKE_UI_LAG=999 -- --only hints,tabs,settings
  expect "stuck dump falls back without failing" 0 "$CODE"
  has "stuck dump: Deck tabs by pixels" "Deck tabs: R2 changed the content"
  has "stuck dump: Settings by pixels" "Settings: R2 changed the content"
  has "stuck dump: Deck hints skip" "Deck could not be read"
  expect "stuck dump leaves Settings" library "$(state_field route)"
  expect "stuck dump restores Stage" 0 "$(state_field active)"
}

test_swapped_screens() {
  scenario swapped FAKE_ASSIGNMENT=FRONTEND_ON_BOTTOM -- --only displays,focus-lock
  has "swap read" "Stage on display 4, Deck on display 0"
  expect "swapped read-only run" 0 "$CODE"
}

# #377: the shoulder script on the fake, with the real map and with the old one.
test_shoulders_377() {
  local script="$TEST_DIR/../verify-377-shoulders.sh"
  SCRIPT="$script" scenario shoulders FAKE_MODE=Top FAKE_DECK_TAB=2 --
  expect "shoulder run passes" 0 "$CODE"
  has "shoulders summary" "FAIL 0"
  has "top pair reaches the Deck after a swap" "swapped, controller top, L1/R1 -> top (Deck)"
  has "Deck read from either screen" "moved the Deck tabs (Canvas to Apps)"
  has "Stage read by pixels from the Deck" "changed Stage's content (by pixels)"
  has "controller stays put" "controller stayed on the bottom screen"
  has "swap restored" "L3 swapped the roles (not swapped)"
  expect "swap restored in state" False "$(state_field swapped)"
  expect "active screen restored" 0 "$(state_field active)"
  expect "Deck tab restored" 2 "$(state_field deck)"
  expect "library tab restored" 1 "$(state_field library)"
  expect "lock untouched by shoulders" 1 "$(state_field lock)"
  if grep -qE 'uninstall|settings put|settings delete|pm clear' "$LOG"; then bad "shoulders: forbidden command sent"
  else ok "shoulders: no forbidden command"; fi
  SCRIPT="$script" scenario shoulders-old FAKE_MODE=Top FAKE_OLD_SHOULDERS=1 --
  expect "old shoulder map fails" 1 "$CODE"
  has "old map reported" "did not move the Deck tabs"
  SCRIPT="$script" scenario shoulders-follow FAKE_MODE=Follow FAKE_FOLLOW_WRITES=1 --
  expect "Follow mode still passes" 0 "$CODE"
  if grep -qE 'keyevent (106|108|99)' "$LOG"; then bad "Follow mode sent X or Swap"; else ok "Follow mode sent no X or Swap"; fi
}

test_device_selection() {
  scenario two FAKE_DEVICES=A,B --
  expect "two devices refused" 2 "$CODE"
  has "two devices message" "set ANDROID_SERIAL"
  if grep -q keyevent "$LOG"; then bad "two devices sent keys"; else ok "two devices sent no keys"; fi
  scenario none FAKE_DEVICES= --
  expect "no device refused" 2 "$CODE"
  ANDROID_SERIAL=GONE scenario gone --
  expect "wrong serial refused" 2 "$CODE"
  ANDROID_SERIAL=B scenario chosen FAKE_DEVICES=A,B -- --only displays
  expect "explicit serial runs" 0 "$CODE"
}

test_static_and_args() {
  scenario static -- --static
  expect "static run passes on this tree" 0 "$CODE"
  has "static checks sources" "no Jarn FE entries in non-debug source manifests"
  expect "static touches no device" 0 "$(wc -l <"$LOG" | tr -d ' ')"
  scenario bad-arg -- --nope
  expect "unknown argument" 2 "$CODE"
  scenario bad-check -- --only nope
  expect "unknown check" 2 "$CODE"
}

# The #371 heading check: learns a first row walking down, then flags its heading going missing.
test_settings_query() {
  local dir="$WORK/settings-query" out query="$TEST_DIR/../lib/settingsquery.py"
  mkdir -p "$dir"
  query_row() { printf '<node text="" focusable="true" focused="%s" bounds="[0,%d][100,%d]"><node text="%s" bounds="[0,%d][100,%d]" /></node>' "$1" "$2" "$(($2 + 50))" "$3" "$2" "$(($2 + 50))"; }
  query_heading() { printf '<node text="%s" focusable="false" bounds="[0,%d][100,%d]" />' "$1" "$2" "$(($2 + $3))"; }
  query_page() { printf '<?xml version="1.0"?><hierarchy><node scrollable="true" bounds="[0,0][100,300]">%s</node></hierarchy>' "$1" >"$dir/ui.xml"; }
  query_page "$(query_heading Tiles 10 20)$(query_row true 40 'System tile size')$(query_row false 100 'Game tile size')"
  out="$(python3 -I "$query" "$dir/state.json" "$dir/ui.xml" Tiles Feedback)"
  has_text "query learns the first row" $'first\tTiles\tok' "$out"
  query_page "$(query_heading Tiles 0 8)$(query_row true 10 'System tile size')"
  out="$(python3 -I "$query" "$dir/state.json" "$dir/ui.xml" Tiles Feedback)"
  has_text "query flags a clipped heading" $'first\tTiles\thidden' "$out"
  query_page "$(query_row true 10 'System tile size')$(query_row false 70 'Game tile size')"
  out="$(python3 -I "$query" "$dir/state.json" "$dir/ui.xml" Tiles Feedback)"
  has_text "query flags a missing heading" $'first\tTiles\thidden' "$out"
  query_page "$(query_heading Tiles 10 20)$(query_row false 40 'System tile size')$(query_row true 100 'Game tile size')"
  out="$(python3 -I "$query" "$dir/state.json" "$dir/ui.xml" Tiles Feedback)"
  if grep -q '^first' <<<"$out"; then bad "query ignores a later row"; else ok "query ignores a later row"; fi
}

test_parsers
test_settings_query
test_clean_run
test_restores_start
test_focus_lock_gate
test_failures_are_caught
test_input_stops_when_focus_is_lost
test_inconclusive_is_skip
test_long_dumps_do_not_break_pipes
test_deck_follows_stage
test_stale_ui_window
test_swapped_screens
test_shoulders_377
test_device_selection
test_static_and_args

printf '\n%d passed, %d failed (bash %s)\n' "$passed" "$failed" "$BASH_VERSION"
[ "$failed" = 0 ]

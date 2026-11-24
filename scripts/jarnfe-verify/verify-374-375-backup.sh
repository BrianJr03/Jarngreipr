#!/usr/bin/env bash
# Backup and restore checks for #374 and #375 on a debug build.
#
#   scripts/jarnfe-verify/verify-374-375-backup.sh [--out DIR] [--keep-data]
#
#   --out DIR     Where the data copy, exports and fixtures go (default: a new timestamped folder
#                 under ~/jarnfe-verify-backups, outside the repo since it holds personal data).
#   --keep-data   Leave the device as the test left it instead of putting the original data back.
#
# What it does, in order:
#   1. Stops the app and copies its prefs, databases and files to the Mac with run-as.
#   2. Seeds a known look, RSS feed and jingle repo, then exports through the debug-only
#      BackupVerifyReceiver (src/debug, never in release builds).
#   3. Changes theme, OLED, icon shape, tab animation, and removes the feed and repo.
#   4. Restores the export and checks prefs and the database got the backed-up values.
#   5. Restores a v51 copy of the export with every new field removed and checks nothing changed.
#   6. Restores a canvas with a fake widget id and a missing photo, and checks the widget id
#      became 0 and the photo was cleared.
#   7. Stops the app and puts the original data back.
#
# Safety: never uninstalls, never writes screen_focus_lock (it only reads it, before and after),
# and only touches the debug package. Stopping the app restarts the launcher if it is the home app.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

SUITE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SUITE_DIR/lib"
# shellcheck source=lib/parse.sh
source "$LIB_DIR/parse.sh"
# shellcheck source=lib/common.sh
source "$LIB_DIR/common.sh"

PKG="${JARNFE_PKG:-jr.brian.home.debug}"
RECEIVER="$PKG/jr.brian.home.debug.BackupVerifyReceiver"
ACTION_PREFIX="jr.brian.home.debug.backup."
WORK="files/backup-verify"
INCLUDE_STOPPED=0x00000020
VERIFY_FEED="https://verify.example/feed"
VERIFY_REPO="verify/jingles"
FAKE_WIDGET_ID=987654
DB_NAME="jarngreipr_database"

OUT_DIR="" KEEP_DATA=0 SAVED=0 RESTORED=0 START_LOCK=""

usage() { sed -n '2,24p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      --keep-data) KEEP_DATA=1 ;;
      -h|--help) usage; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
  [ -n "$OUT_DIR" ] || OUT_DIR="$HOME/jarnfe-verify-backups/backup-374-375-$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$OUT_DIR" || { echo "cannot create $OUT_DIR" >&2; exit 2; }
}

select_device() {
  command -v adb >/dev/null || { echo "adb not found" >&2; exit 2; }
  local serials count
  serials="$(adb devices | tr -d '\r' | awk 'NR > 1 && $2 == "device" { print $1 }')"
  count="$(printf '%s' "$serials" | grep -c .)"
  if [ -n "${ANDROID_SERIAL:-}" ]; then
    text_has_line "$serials" "$ANDROID_SERIAL" || { echo "ANDROID_SERIAL=$ANDROID_SERIAL is not attached" >&2; exit 2; }
  elif [ "$count" = 0 ]; then
    echo "no adb device attached" >&2; exit 2
  elif [ "$count" -gt 1 ]; then
    echo "more than one device attached, set ANDROID_SERIAL:" >&2
    printf '%s\n' "$serials" >&2
    exit 2
  else
    export ANDROID_SERIAL="$serials"
  fi
}

require_debug_build() {
  text_has_line "$(sh_dev pm list packages "$PKG")" "package:$PKG" || { echo "$PKG is not installed" >&2; exit 2; }
  case "$(sh_dev run-as "$PKG" id)" in
    *uid=*) ;;
    *) echo "run-as $PKG is not allowed, so this is not a debuggable build" >&2; exit 2 ;;
  esac
  text_has "$(sh_dev dumpsys package "$PKG")" "BackupVerifyReceiver" || {
    echo "this build has no BackupVerifyReceiver, install a debug build with it" >&2
    exit 2
  }
}

stop_app() { sh_dev am force-stop "$PKG" >/dev/null; pause 1; }

save_data() {
  section "Saving the app's data"
  stop_app
  adb exec-out run-as "$PKG" tar -cf - shared_prefs databases files 2>/dev/null >"$OUT_DIR/original-data.tar"
  if tar -tf "$OUT_DIR/original-data.tar" 2>/dev/null | grep -q '^shared_prefs/'; then
    SAVED=1
    pass "copied prefs, databases and files to $OUT_DIR/original-data.tar"
  else
    fail "could not copy the app's data, stopping before anything changes"
    exit 1
  fi
}

put_data_back() {
  [ "$SAVED" = 1 ] && [ "$RESTORED" = 0 ] || return 0
  RESTORED=1
  if [ "$KEEP_DATA" = 1 ]; then
    skip "left the test data in place (--keep-data), original copy is $OUT_DIR/original-data.tar"
    return 0
  fi
  section "Putting the original data back"
  stop_app
  sh_dev run-as "$PKG" rm -rf shared_prefs databases files >/dev/null
  adb exec-in run-as "$PKG" tar -xf - <"$OUT_DIR/original-data.tar" 2>/dev/null
  stop_app
  if text_has "$(sh_dev run-as "$PKG" ls shared_prefs)" ".xml"; then
    pass "original data is back"
  else
    fail "original data may not be back, restore it by hand from $OUT_DIR/original-data.tar"
  fi
}

# Sends $1 to the debug receiver and waits for its done file. Extra args go to am broadcast.
hook() {
  local action="$1" attempt result
  shift
  sh_dev run-as "$PKG" rm -f "$WORK/$action.done" >/dev/null
  sh_dev am broadcast -f "$INCLUDE_STOPPED" -a "$ACTION_PREFIX$action" -n "$RECEIVER" "$@" >/dev/null
  for attempt in $(seq 1 40); do
    result="$(sh_dev run-as "$PKG" cat "$WORK/$action.done")"
    case "$result" in
      ok) return 0 ;;
      "") pause 0.5 ;;
      *) fail "$action: $result"; return 1 ;;
    esac
  done
  fail "$action: no answer from the debug receiver"
  return 1
}

pull_work_file() { sh_dev run-as "$PKG" cat "$WORK/$1" >"$OUT_DIR/$2"; [ -s "$OUT_DIR/$2" ]; }

push_work_file() {
  adb exec-in run-as "$PKG" sh -c "cat > $WORK/$2" <"$OUT_DIR/$1" 2>/dev/null
}

# JSON file $1, python expression $2 over `c` (the config) -> prints the result.
json_eval() { python3 -I -c 'import json,sys; c=json.load(open(sys.argv[1])); print(eval(sys.argv[2]))' "$1" "$2"; }

# Prefs file $1, set key $2 -> its strings, one per line.
pref_set() {
  read_prefs "$1" | python3 -I -c '
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.fromstring(sys.stdin.read())
except ET.ParseError:
    sys.exit(0)
for item in root:
    if item.tag == "set" and item.get("name") == sys.argv[1]:
        for value in item:
            print(value.text or "")
' "$2"
}

pref() { read_prefs "$1" | parse_pref "$2"; }

# Prints "yes" or "no" for the verify feed, or nothing when neither sqlite3 nor an export can tell.
feed_present() {
  local rows
  rows="$(sh_dev run-as "$PKG" sqlite3 "databases/$DB_NAME" "select url from rss_feeds;")"
  if text_has "$rows" "://"; then
    text_has_line "$rows" "$VERIFY_FEED" && echo yes || echo no
    return
  fi
  hook export --es file state.json && pull_work_file state.json state.json || return
  [ "$(json_eval "$OUT_DIR/state.json" "any(f['url'] == '$VERIFY_FEED' for f in c['feature'].get('rssFeeds') or [])")" = True ] \
    && echo yes || echo no
}

expect() {
  local label="$1" want="$2" got="$3"
  if [ "$got" = "$want" ]; then pass "$label is $want"; else fail "$label is ${got:-unset}, wanted $want"; fi
}

expect_look() {
  local theme="$1" oled="$2" shape="$3" tab="$4" repo="$5" feed="$6"
  expect "theme" "$theme" "$(pref gaming_launcher_prefs selected_theme)"
  expect "OLED mode" "$oled" "$(pref gaming_launcher_prefs oled_mode_enabled)"
  expect "keyboard OLED exemption" "$oled" "$(pref gaming_launcher_prefs keyboard_oled_exempt)"
  expect "icon shape" "$shape" "$(pref icon_shape_prefs icon_shape)"
  expect "tab animation" "$tab" "$(pref gaming_launcher_prefs tab_animation_enabled)"
  if text_has_line "$(pref_set jingles_prefs jingle_repos)" "$VERIFY_REPO"; then
    expect "jingle repo $VERIFY_REPO present" "$repo" yes
  else
    expect "jingle repo $VERIFY_REPO present" "$repo" no
  fi
  expect "RSS feed $VERIFY_FEED present" "$feed" "$(feed_present)"
}

check_round_trip() {
  section "#375 export and restore"
  hook seed || return
  hook export || return
  pull_work_file export.json export.json || { fail "could not read the export"; return; }
  expect "export version" 52 "$(json_eval "$OUT_DIR/export.json" "c['version']")"
  expect "exported theme" green_cyan "$(json_eval "$OUT_DIR/export.json" "c['ui']['appearance']['themeId']")"
  expect "export keeps scraper password blank" "" \
    "$(json_eval "$OUT_DIR/export.json" "c['feature']['romSearch']['scraper']['screenScraperUserPassword']")"
  hook mutate || return
  expect_look red_blue false ROUNDED false no no
  hook restore --es file export.json || return
  expect_look green_cyan true SQUARE true yes yes
}

check_old_backup() {
  section "#375 a v51 backup changes none of the new settings"
  [ -s "$OUT_DIR/export.json" ] || { skip "no export to build the v51 fixture from"; return; }
  python3 -I -c '
import json, sys
c = json.load(open(sys.argv[1]))
c["version"] = 51
c["ui"].pop("appearance", None)
c["page"].pop("widgetProviders", None)
c["feature"].pop("rssFeeds", None)
c["feature"]["jingles"].pop("enabled", None)
c["feature"]["jingles"].pop("repos", None)
json.dump(c, open(sys.argv[2], "w"), indent=2)
' "$OUT_DIR/export.json" "$OUT_DIR/v51.json" || { fail "could not build the v51 fixture"; return; }
  push_work_file v51.json v51.json
  hook mutate || return
  hook restore --es file v51.json || return
  expect_look red_blue false ROUNDED false no no
}

check_canvas_repair() {
  section "#374 restored canvas drops a dead widget id and a missing photo"
  [ -s "$OUT_DIR/export.json" ] || { skip "no export to build the canvas fixture from"; return; }
  python3 -I -c '
import json, sys
c = json.load(open(sys.argv[1]))
layouts = c["page"].setdefault("canvasLayouts", {})
layout = layouts.setdefault("0", {"items": []})
layout.setdefault("items", []).extend([
    {"type": "widget", "id": "verify-widget", "widgetId": int(sys.argv[3])},
    {"type": "photo", "id": "verify-photo", "imageUri": "file:///data/local/tmp/verify-missing.png"},
])
providers = c["page"].get("widgetProviders") or {}
providers[sys.argv[3]] = "verify.fake/.Widget"
c["page"]["widgetProviders"] = providers
json.dump(c, open(sys.argv[2], "w"), indent=2)
' "$OUT_DIR/export.json" "$OUT_DIR/canvas.json" "$FAKE_WIDGET_ID" || { fail "could not build the canvas fixture"; return; }
  push_work_file canvas.json canvas.json
  hook restore --es file canvas.json || return
  hook export --es file after-canvas.json || return
  pull_work_file after-canvas.json after-canvas.json || { fail "could not read the export after restore"; return; }
  local items="c['page']['canvasLayouts']['0']['items']"
  expect "fake widget tile id" 0 \
    "$(json_eval "$OUT_DIR/after-canvas.json" "[i['widgetId'] for i in $items if i['id'] == 'verify-widget'][0]")"
  expect "missing photo tile picture" None \
    "$(json_eval "$OUT_DIR/after-canvas.json" "[i.get('imageUri') for i in $items if i['id'] == 'verify-photo'][0]")"
  local stored
  stored="$(pref canvas_layout_prefs layout_0)"
  if text_has "$stored" "$FAKE_WIDGET_ID"; then
    fail "saved canvas layout still holds widget id $FAKE_WIDGET_ID"
  else
    pass "saved canvas layout no longer holds widget id $FAKE_WIDGET_ID"
  fi
}

check_focus_lock_untouched() {
  local now
  now="$(focus_lock_value)"
  if [ "$now" = "$START_LOCK" ]; then
    pass "screen_focus_lock unchanged (${now:-unset})"
  else
    fail "screen_focus_lock changed from ${START_LOCK:-unset} to ${now:-unset}, the app's own sync wrote it"
  fi
}

main() {
  parse_args "$@"
  select_device
  require_debug_build
  START_LOCK="$(focus_lock_value)"
  trap put_data_back EXIT
  save_data
  check_round_trip
  check_old_backup
  check_canvas_repair
  put_data_back
  section "Focus lock"
  check_focus_lock_untouched
  printf '\n%d passed, %d failed, %d skipped. Files: %s\n' "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT" "$OUT_DIR"
  [ "$FAIL_COUNT" = 0 ]
}

main "$@"

#!/usr/bin/env bash
# #439: on a cold start the Library focuses its first system, not the tab row.
#
#   scripts/jarnfe-verify/verify-439-launch-focus.sh [options]
#
#   --apk PATH              adb install -r this APK first (never uninstalls).
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --runs N                How many cold starts to make (default 3).
#   --wait S                Seconds to wait for the Library after launch (default 20).
#   --allow-app-focus-sync  Launch even if the app's Focus Lock mode would rewrite
#                           screen_focus_lock. The script still never writes it.
#
# Each run force-stops the app, starts JarnStageActivity, waits out the splash and the Library
# load without pressing anything, then reads Stage with uiautomator: the focused node must be a
# shelf or grid item, never a Library tab. A cold start always opens on Systems, so Favorites
# and Recent are then reached with the Stage's own shoulder and must also land in the content.
#
# Safety: force-stops and relaunches the app (that is the test), but never uninstalls, clears
# data, writes settings or presses A. Keys are the Stage's shoulder pair only, and input stops as
# soon as Jarn FE loses focus.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

RUNS=3 WAIT_S=20 FOCUSED_NOW=""

usage_439() { sed -n '2,22p' "$VERIFY_DIR/verify-439-launch-focus.sh" | sed 's/^# \{0,1\}//'; }

parse_439_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --apk) APK="${2:?--apk needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --runs) RUNS="${2:?--runs needs a number}"; shift ;;
      --wait) WAIT_S="${2:?--wait needs seconds}"; shift ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_439; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

install_apk() {
  [ -n "$APK" ] || return 0
  section "Install"
  [ -f "$APK" ] || { echo "APK not found: $APK" >&2; exit 2; }
  if adb install -r "$APK" 2>&1 | tr -d '\r' | tee "$OUT_DIR/install.log" | grep -q '^Success'; then
    pass "installed $(basename "$APK")"
  else
    echo "install failed, see $OUT_DIR/install.log (never uninstalls on a signature mismatch)" >&2
    exit 2
  fi
}

is_tab_label() {
  local tab labels
  labels="${1// | /$'\n'}"
  for tab in "${LIBRARY_TABS[@]}"; do text_has_line "$labels" "$tab" && return 0; done
  return 1
}

# Polls until Stage has a focused node that is not a tab, or the wait runs out.
wait_content_focus() {
  local deadline=$((SECONDS + WAIT_S))
  FOCUSED_NOW=""
  while [ "$SECONDS" -lt "$deadline" ]; do
    if [ "$(ui_role)" = stage ]; then
      FOCUSED_NOW="$(ui_focused)"
      [ -n "$FOCUSED_NOW" ] && ! is_tab_label "$FOCUSED_NOW" && return 0
    fi
    sleep 1
  done
  return 1
}

cold_start() {
  sh_dev am force-stop "$PKG" >/dev/null
  sleep 1
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  sleep 4
  START_PID="$(sh_dev pidof "$PKG")"
  START_DISPLAY="$(top_display)"
  jarn_is_focused
}

expect_content_focus() {
  local what="$1" file="$2"
  if wait_content_focus; then
    pass "$what: content is focused ($FOCUSED_NOW)"
  elif is_tab_label "$FOCUSED_NOW"; then
    fail "$what: the tab row holds focus ($FOCUSED_NOW)"
  else
    fail "$what: nothing focused on Stage after ${WAIT_S}s"
  fi
  cp "$UI_XML" "$OUT_DIR/$file.xml" 2>/dev/null
  shot "$STAGE_PHYSICAL" "$OUT_DIR/$file.png"
}

check_selected_tab() {
  local want="$1" what="$2" now
  now="$(ui_selected "${LIBRARY_TABS[@]}")"
  if [ "$now" = "$want" ]; then pass "$what: $want is the open tab"
  else skip "$what: open tab reads as ${now:-unknown}, wanted $want"; fi
}

run_cold_starts() {
  local run
  for ((run = 1; run <= RUNS; run++)); do
    section "Cold start $run"
    cold_start || { fail "run $run: Jarn FE is not on screen after launch"; return 1; }
    expect_content_focus "run $run, Systems" "439-run$run-systems"
    check_selected_tab Systems "run $run"
  done
}

run_other_tabs() {
  section "Favorites and Recent after a cold start"
  [ "$(top_display)" = "$STAGE_DISPLAY" ] || { skip "Stage does not have the controller"; return 0; }
  tab_keys_for "$STAGE_DISPLAY"
  local tab
  for tab in Favorites Recent; do
    key "$TAB_NEXT" || return 0
    pause 1
    expect_content_focus "$tab" "439-$(printf '%s' "$tab" | tr '[:upper:]' '[:lower:]')"
    check_selected_tab "$tab" "$tab"
  done
}

run_439() {
  if [ "$HANDOFF_OK" != 1 ]; then
    skip "cold start: $HANDOFF_REASON"
    return 0
  fi
  run_cold_starts || return 0
  run_other_tabs
}

main_439() {
  parse_439_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-439)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_apk
  LAUNCH=1
  preflight || { echo "Preflight failed, nothing was pressed." >&2; exit 2; }
  run_439
  check_no_crashes
  check_lock_unchanged
  summary
}

main_439 "$@"

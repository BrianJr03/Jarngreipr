#!/usr/bin/env bash
# Jarn FE dual-screen verification for the AYN Thor. Entry point for the whole suite.
#
#   scripts/jarnfe-verify/run.sh [options]
#
#   --static                Release manifest checks only. No device needed.
#   --only a,b              Run only these checks (see --list).
#   --list                  List the checks and exit.
#   --launch                Open Jarn FE first if it is not already on screen.
#   --apk PATH              Also check this release APK's manifest.
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run screen switching even if the app's Focus Lock mode would rewrite
#                           screen_focus_lock. The suite still never writes it. See the note below.
#
# Safety: never uninstalls, clears data, changes settings, restores, launches a game, or presses A.
# Keys are limited to D-pad, B, L1, R1, L2, R2, Start and Select, and stop as soon as Jarn FE loses
# focus. L1/R1 step the top screen's tabs and L2/R2 the bottom screen's.
#
# Focus Lock note: on a Thor, moving control between screens calls the app's own focus lock sync.
# In Follow mode (the default) with Shizuku granted, that writes screen_focus_lock. So unless the
# mode is proven inert, checks that switch screens are skipped rather than risk changing it.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

SUITE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$SUITE_DIR/lib"
REPO_ROOT="$(cd "$SUITE_DIR/../.." && pwd)"
# shellcheck source=lib/parse.sh
source "$LIB_DIR/parse.sh"
# shellcheck source=lib/common.sh
source "$LIB_DIR/common.sh"
# shellcheck source=lib/checks.sh
source "$LIB_DIR/checks.sh"

PKG="${JARNFE_PKG:-jr.brian.home.debug}"
RELEASE_PKG="${JARNFE_RELEASE_PKG:-jr.brian.home}"
STAGE_ACTIVITY="jr.brian.jarnfe.shell.JarnStageActivity"
DECK_ACTIVITY="jr.brian.jarnfe.shell.JarnDeckActivity"
TOP_DISPLAY=0
BOTTOM_DISPLAY=4
TOP_PHYSICAL=4630946441858561667
BOTTOM_PHYSICAL=4630946482288158084
ALL_CHECKS="displays activities focus-lock switching dpad hints tabs settings crashes release"

STATIC=0 LAUNCH=0 ALLOW_SYNC=0 HAS_DEVICE=0 HANDOFF_OK=0 IN_SETTINGS=0 RESTORED=0
HANDOFF_REASON="" APK="" ONLY="" OUT_DIR="" LOCK_MODE="" HINTS_PREF=""
START_LOCK="" START_PID="" START_DISPLAY="" START_DECK_TAB="" START_LIBRARY_TAB="" LOG_START=""
DECK_TAB_MOVED=0
STAGE_DISPLAY=$TOP_DISPLAY DECK_DISPLAY=$BOTTOM_DISPLAY
STAGE_PHYSICAL=$TOP_PHYSICAL DECK_PHYSICAL=$BOTTOM_PHYSICAL

usage() { sed -n '2,23p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --static) STATIC=1 ;;
      --launch) LAUNCH=1 ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --only) ONLY="${2:?--only needs a value}"; shift ;;
      --apk) APK="${2:?--apk needs a path}"; shift ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      --list) printf '%s\n' $ALL_CHECKS; exit 0 ;;
      -h|--help) usage; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
  local name
  for name in ${ONLY//,/ }; do
    case " $ALL_CHECKS " in
      *" $name "*) ;;
      *) echo "unknown check: $name (see --list)" >&2; exit 2 ;;
    esac
  done
}

wants() { [ -z "$ONLY" ] || case ",$ONLY," in *",$1,"*) return 0 ;; *) return 1 ;; esac; }

select_device() {
  command -v adb >/dev/null || { echo "adb not found" >&2; exit 2; }
  local serials count
  serials="$(adb devices | tr -d '\r' | awk 'NR > 1 && $2 == "device" { print $1 }')"
  count="$(printf '%s' "$serials" | grep -c .)"
  if [ -n "${ANDROID_SERIAL:-}" ]; then
    text_has_line "$serials" "$ANDROID_SERIAL" || {
      echo "ANDROID_SERIAL=$ANDROID_SERIAL is not attached" >&2
      exit 2
    }
  elif [ "$count" = 0 ]; then
    echo "no adb device attached" >&2
    exit 2
  elif [ "$count" -gt 1 ]; then
    echo "more than one device attached, set ANDROID_SERIAL:" >&2
    printf '%s\n' "$serials" >&2
    exit 2
  else
    export ANDROID_SERIAL="$serials"
  fi
  HAS_DEVICE=1
}

read_roles() {
  local prefs
  prefs="$(read_prefs screen_assignment_prefs)" || return
  if [ "$(printf '%s' "$prefs" | parse_pref assignment)" = "FRONTEND_ON_BOTTOM" ]; then
    STAGE_DISPLAY=$BOTTOM_DISPLAY DECK_DISPLAY=$TOP_DISPLAY
    STAGE_PHYSICAL=$BOTTOM_PHYSICAL DECK_PHYSICAL=$TOP_PHYSICAL
  fi
}

# Follow is the app default, so a missing prefs file or key still means Follow.
read_focus_lock_gate() {
  local prefs
  START_LOCK="$(focus_lock_value)"
  if prefs="$(read_prefs focus_lock_prefs)"; then
    LOCK_MODE="$(printf '%s' "$prefs" | parse_pref focus_lock_mode)"
    LOCK_MODE="${LOCK_MODE:-Follow}"
  else
    LOCK_MODE=""
  fi
  if [ "$ALLOW_SYNC" = 1 ]; then
    HANDOFF_OK=1
  elif [ -z "$LOCK_MODE" ]; then
    HANDOFF_REASON="Focus Lock mode is unreadable, so a switch might change screen_focus_lock (--allow-app-focus-sync to run)"
  elif focus_sync_is_inert "$LOCK_MODE" "$START_LOCK"; then
    HANDOFF_OK=1
  else
    HANDOFF_REASON="Focus Lock mode $LOCK_MODE may rewrite screen_focus_lock on a switch (--allow-app-focus-sync to run)"
  fi
}

jarn_is_focused() {
  local display
  display="$(top_display)"
  case "$(focused_app "${display:-0}")" in "$PKG"/*) return 0 ;; *) return 1 ;; esac
}

launch_if_asked() {
  jarn_is_focused && return 0
  if [ "$LAUNCH" = 0 ]; then
    echo "Jarn FE is not on screen. Open it, or pass --launch." >&2
    return 1
  fi
  if [ "$HANDOFF_OK" != 1 ]; then
    echo "Not launching: $HANDOFF_REASON" >&2
    return 1
  fi
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 6
  jarn_is_focused
}

preflight() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_roles
  read_focus_lock_gate
  launch_if_asked || return 1
  LOG_START="$(sh_dev date +%s).000"
  START_PID="$(sh_dev pidof "$PKG")"
  START_DISPLAY="$(top_display)"
  HINTS_PREF="$(read_prefs jarnfe_look_prefs | parse_pref hints_visible)"
  echo "  info  package $PKG, pid ${START_PID:-none}, active display ${START_DISPLAY:-?}"
  echo "  info  Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY"
  echo "  info  output in $OUT_DIR"
  if [ "$START_DISPLAY" = "$STAGE_DISPLAY" ]; then remember_library_tab || true; fi
  return 0
}

# Leaves Settings if a check was interrupted there, then puts control back where it started.
restore_start() {
  [ "$RESTORED" = 0 ] || return 0
  RESTORED=1
  [ "$KEYS_SENT" = 1 ] && [ "$ABORTED" = 0 ] || return 0
  if [ "$IN_SETTINGS" = 1 ]; then
    key "$KEY_B" && IN_SETTINGS=0
  fi
  if [ -n "$START_DECK_TAB" ] && [ "$HANDOFF_OK" = 1 ] && activate_display "$DECK_DISPLAY"; then
    step_to_tab "$(index_of "$START_DECK_TAB" "${DECK_TABS[@]}")" "${#DECK_TABS[@]}" "$DECK_DISPLAY"
  elif [ "$DECK_TAB_MOVED" = 1 ]; then
    echo "  info  starting Deck tab could not be read, so it was not restored"
  fi
  if [ -n "$START_LIBRARY_TAB" ] && activate_display "$STAGE_DISPLAY"; then
    step_to_tab "$(index_of "$START_LIBRARY_TAB" "${LIBRARY_TABS[@]}")" "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY"
  fi
  if [ -n "$START_DISPLAY" ] && [ "$(top_display)" != "$START_DISPLAY" ]; then
    key "$KEY_SELECT" && wait_top_display "$START_DISPLAY"
  fi
  if [ -n "$START_DISPLAY" ] && [ "$(top_display)" != "$START_DISPLAY" ]; then
    fail "could not restore the starting screen (display $START_DISPLAY)"
  fi
}

summary() {
  printf '\n== Summary\n  PASS %d   FAIL %d   SKIP %d\n' "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT"
  [ "$ABORTED" = 0 ] || echo "  Input was stopped early because Jarn FE lost focus."
  echo "  Output: $OUT_DIR"
  [ "$FAIL_COUNT" = 0 ]
}

run_device_checks() {
  wants displays && check_displays
  wants activities && check_activities
  wants focus-lock && check_focus_lock
  wants switching && check_switching
  wants dpad && check_dpad
  wants hints && check_hints
  wants tabs && check_tabs
  wants settings && check_settings
  restore_start
  wants crashes && check_no_crashes
  check_lock_unchanged
}

main() {
  parse_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify)"
  mkdir -p "$OUT_DIR"
  # Set here, not in ui_dump, because ui_role runs in a subshell and callers read the file after.
  UI_XML="$OUT_DIR/ui.xml"
  if [ "$STATIC" = 0 ]; then
    select_device
    trap restore_start EXIT
    trap 'restore_start; exit 130' INT TERM
    preflight || { RESTORED=1; echo "Preflight failed, nothing was pressed." >&2; exit 2; }
    run_device_checks
  fi
  wants release && check_release
  summary
}

# Other scripts source this file for its helpers, so main only runs when it is executed.
if [ "${BASH_SOURCE[0]}" = "$0" ]; then main "$@"; fi

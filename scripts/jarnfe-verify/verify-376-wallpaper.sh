#!/usr/bin/env bash
# #376: Jarn FE wallpapers move. Checks a test GIF and each live wallpaper on both screens.
#
#   scripts/jarnfe-verify/verify-376-wallpaper.sh [options]
#
#   --animations-off  Also run every check with Android animations off (animator_duration_scale 0),
#                     the case #376 is about. Restores the scale at the end.
#   --live IDS        Live wallpapers to check, comma separated (default: starstave,ironweave,hearthrise).
#   --out DIR         Where screenshots go (default: a new temp folder).
#
# Needs a debug build from this branch: it uses the debug-only JarnWallpaperDebugReceiver to set
# the wallpaper and Reduce motion, and puts back what it found at the end.
#
# Safety: never uninstalls, clears data or presses keys, and never writes screen_focus_lock.
# Battery Saver is only read. It opens JarnStageActivity directly.
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
STAGE_ACTIVITY="jr.brian.jarnfe.shell.JarnStageActivity"
DECK_ACTIVITY="jr.brian.jarnfe.shell.JarnDeckActivity"
RECEIVER="jr.brian.home.debug.JarnWallpaperDebugReceiver"
HOOK_ACTION="jr.brian.home.debug.JARN_WALLPAPER"
TOP_DISPLAY=0
BOTTOM_DISPLAY=4
TOP_PHYSICAL=4630946441858561667
BOTTOM_PHYSICAL=4630946482288158084
REMOTE_TMP_GIF=/data/local/tmp/jarnfe-verify-376.gif
APP_GIF_DIR=files/jarnfe_verify
APP_GIF="$APP_GIF_DIR/wallpaper-376.gif"
MOVING_MIN="${JARNFE_MOVING_MIN:-6}"
STILL_MAX="${JARNFE_STILL_MAX:-2}"
SHOT_GAP="${JARNFE_SHOT_GAP:-0.7}"
SAMPLES="${JARNFE_SAMPLES:-3}"
LIVE_WAIT="${JARNFE_LIVE_WAIT:-65}"
LIVE_IDS="starstave,ironweave,hearthrise"

ANIMATIONS_OFF=0 OUT_DIR="" RESTORED=1
START_STATE="" START_SCALE="" START_LOCK="" GIF_URI=""

usage() { sed -n '2,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --animations-off) ANIMATIONS_OFF=1 ;;
      --live) LIVE_IDS="${2:?--live needs a value}"; shift ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

select_device() {
  command -v adb >/dev/null || { echo "adb not found" >&2; exit 2; }
  local serials count
  serials="$(adb devices | tr -d '\r' | awk 'NR > 1 && $2 == "device" { print $1 }')"
  count="$(printf '%s' "$serials" | grep -c .)"
  if [ -n "${ANDROID_SERIAL:-}" ]; then
    text_has_line "$serials" "$ANDROID_SERIAL" || { echo "ANDROID_SERIAL=$ANDROID_SERIAL is not attached" >&2; exit 2; }
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
}

# Sends the debug hook. Arguments are extra am flags, values already single-quoted. Prints the state.
hook() {
  local out
  out="$(sh_dev "am broadcast -a $HOOK_ACTION -n $PKG/$RECEIVER $*")"
  text_has "$out" 'data="' || return 1
  printf '%s\n' "$out" | sed -n 's/.*data="\(.*\)".*/\1/p'
}

state_field() { diff_field "$1" "$2"; }

preflight() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  START_STATE="$(hook)" || {
    echo "The debug wallpaper hook did not answer. Install a debug build from this branch." >&2
    return 1
  }
  START_SCALE="$(sh_dev settings get global animator_duration_scale)"
  START_LOCK="$(focus_lock_value)"
  local low_power look_reduced
  low_power="$(sh_dev settings get global low_power)"
  look_reduced="$(read_prefs jarnfe_look_prefs | parse_pref reduced_motion)"
  echo "  info  animator_duration_scale ${START_SCALE:-unset}"
  echo "  info  low_power (Battery Saver) ${low_power:-unset}"
  echo "  info  Jarn Reduce motion pref ${look_reduced:-unset (off)}"
  echo "  info  wallpaper at start: $START_STATE"
  echo "  info  output in $OUT_DIR"
  if [ "$low_power" = 1 ]; then
    echo "Battery Saver is on, so wallpapers are meant to hold still. Turn it off and run again." >&2
    return 1
  fi
  RESTORED=0
  return 0
}

restore() {
  [ "$RESTORED" = 0 ] || return 0
  RESTORED=1
  section "Restore"
  local type uri live reduced now
  type="$(state_field "$START_STATE" type)"
  uri="$(state_field "$START_STATE" uri)"
  live="$(state_field "$START_STATE" live)"
  reduced="$(state_field "$START_STATE" reduced)"
  now="$(hook "--es type '$type' --es uri '$uri' --es live '$live' --ez reduced '$reduced'")"
  if [ "$now" = "$START_STATE" ]; then pass "wallpaper and Reduce motion restored ($now)"
  else fail "wallpaper restore: wanted '$START_STATE', got '${now:-no answer}'"; fi
  sh_dev run-as "$PKG" rm -f "$APP_GIF" >/dev/null
  sh_dev rm -f "$REMOTE_TMP_GIF" >/dev/null
  if [ -n "$START_SCALE" ] && [ "$(sh_dev settings get global animator_duration_scale)" != "$START_SCALE" ]; then
    if [ "$START_SCALE" = null ]; then sh_dev settings delete global animator_duration_scale >/dev/null
    else sh_dev settings put global animator_duration_scale "$START_SCALE" >/dev/null; fi
    echo "  info  animator_duration_scale put back to $START_SCALE"
  fi
  local lock
  lock="$(focus_lock_value)"
  if [ "$lock" = "$START_LOCK" ]; then pass "screen_focus_lock still ${lock:-unset}"
  else fail "screen_focus_lock changed from $START_LOCK to ${lock:-empty} (left as is, not repaired)"; fi
}

push_test_gif() {
  local local_gif="$OUT_DIR/wallpaper-376.gif" data_dir
  python3 -I "$LIB_DIR/motion.py" gif "$local_gif" || return 1
  adb push "$local_gif" "$REMOTE_TMP_GIF" >/dev/null 2>&1 || return 1
  sh_dev chmod 644 "$REMOTE_TMP_GIF" >/dev/null
  sh_dev run-as "$PKG" mkdir -p "$APP_GIF_DIR" >/dev/null
  sh_dev run-as "$PKG" cp "$REMOTE_TMP_GIF" "$APP_GIF" >/dev/null
  data_dir="$(sh_dev run-as "$PKG" pwd)"
  [ -n "$data_dir" ] || return 1
  text_has "$(sh_dev run-as "$PKG" ls "$APP_GIF_DIR")" "wallpaper-376.gif" || return 1
  GIF_URI="file://$data_dir/$APP_GIF"
}

open_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  # Past the splash, which holds every wallpaper still until it ends.
  pause 6
}

# $SAMPLES shots $SHOT_GAP apart on one display, each compared with the first. A faint wallpaper
# can look still between two close frames, so the largest change counts. Prints that count, or
# "noshot".
motion_on() {
  local physical="$1" name="$2" first="$OUT_DIR/$2-1.png" i shot_file result changed best=0
  shot "$physical" "$first" || { echo "noshot"; return; }
  for ((i = 2; i <= SAMPLES; i++)); do
    sleep "$SHOT_GAP"
    shot_file="$OUT_DIR/$name-$i.png"
    shot "$physical" "$shot_file" || { echo "noshot"; return; }
    result="$(python3 -I "$LIB_DIR/motion.py" diff "$first" "$shot_file")"
    changed="$(diff_field "$result" changed)"
    [ "${changed:-0}" -gt "$best" ] && best="$changed"
  done
  echo "$best"
}

# Screenshots only prove something while this build's own windows are the resumed ones.
jarn_windows_up() {
  local dump
  dump="$(sh_dev dumpsys activity activities)"
  text_has "$(printf '%s\n' "$dump" | parse_resumed "$TOP_DISPLAY")" "$PKG/$STAGE_ACTIVITY" &&
    text_has "$(printf '%s\n' "$dump" | parse_resumed "$BOTTOM_DISPLAY")" "$PKG/$DECK_ACTIVITY"
}

# Live scenes rest by design (the Ironweave sheen is off screen for up to ~25s of its 60s loop),
# so a quiet sample is retried until $3 seconds pass. A frozen clock never moves and still fails.
expect_moving() {
  local label="$1" wait="${3:-0}" physical name changed started tries
  for physical in "$TOP_PHYSICAL" "$BOTTOM_PHYSICAL"; do
    name="$2-$( [ "$physical" = "$TOP_PHYSICAL" ] && echo top || echo bottom )"
    started="$(date +%s)" tries=1
    changed="$(motion_on "$physical" "$name")"
    while [ "$changed" != noshot ] && [ "${changed:-0}" -lt "$MOVING_MIN" ] &&
      [ $(($(date +%s) - started)) -lt "$wait" ]; do
      tries=$((tries + 1))
      changed="$(motion_on "$physical" "$name")"
    done
    case "$changed" in
      noshot) skip "$label ($name): no screenshot from display $physical" ;;
      *) if [ "${changed:-0}" -ge "$MOVING_MIN" ]; then
           pass "$label moves on $name ($changed cells changed, sample set $tries)"
         else
           fail "$label holds still on $name ($changed cells changed over ${wait}s, wanted $MOVING_MIN or more)"
         fi ;;
    esac
  done
}

expect_still() {
  local label="$1" physical name changed
  for physical in "$TOP_PHYSICAL" "$BOTTOM_PHYSICAL"; do
    name="$2-$( [ "$physical" = "$TOP_PHYSICAL" ] && echo top || echo bottom )"
    changed="$(motion_on "$physical" "$name")"
    case "$changed" in
      noshot) skip "$label ($name): no screenshot from display $physical" ;;
      *) if [ "${changed:-0}" -le "$STILL_MAX" ]; then pass "$label holds still on $name ($changed cells changed)"
         else fail "$label still moves on $name ($changed cells changed, wanted $STILL_MAX or fewer)"; fi ;;
    esac
  done
}

# Runs the GIF and live checks, with $1 naming the pass ("anim-on" or "anim-off").
check_round() {
  local round="$1" id
  section "GIF wallpaper ($round)"
  if [ -z "$GIF_URI" ]; then
    skip "test GIF could not be copied into the app"
  else
    hook "--es type 'GIF' --es uri '$GIF_URI' --es live 'off' --ez reduced 'false'" >/dev/null
    pause 2
    jarn_windows_up || { fail "$PKG Stage and Deck are not both resumed, so nothing can be checked"; return; }
    expect_moving "GIF" "gif-$round"
    hook "--ez reduced 'true'" >/dev/null
    pause 1.5
    expect_still "GIF with Reduce motion" "gif-reduced-$round"
    hook "--ez reduced 'false'" >/dev/null
  fi
  for id in ${LIVE_IDS//,/ }; do
    section "Live wallpaper $id ($round)"
    hook "--es live '$id' --ez reduced 'false'" >/dev/null
    pause 2
    jarn_windows_up || { fail "$PKG Stage and Deck are not both resumed, so nothing can be checked"; return; }
    expect_moving "$id" "$id-$round" "$LIVE_WAIT"
  done
}

main() {
  parse_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-376)"
  mkdir -p "$OUT_DIR"
  select_device
  trap restore EXIT
  trap 'restore; exit 130' INT TERM
  preflight || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  push_test_gif || echo "  info  could not push the test GIF, GIF checks will be skipped"
  open_stage
  check_round anim-on
  if [ "$ANIMATIONS_OFF" = 1 ]; then
    sh_dev settings put global animator_duration_scale 0 >/dev/null
    open_stage
    check_round anim-off
  fi
  restore
  printf '\n== Summary\n  PASS %d   FAIL %d   SKIP %d\n' "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT"
  echo "  Output: $OUT_DIR"
  [ "$FAIL_COUNT" = 0 ]
}

main "$@"

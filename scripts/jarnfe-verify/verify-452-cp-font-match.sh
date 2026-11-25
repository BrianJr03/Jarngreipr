#!/usr/bin/env bash
# #452: end-to-end device checks for "Match Command Panel fonts" (debug Jarn and debug Command Panel).
#
#   scripts/jarnfe-verify/verify-452-cp-font-match.sh [options]
#
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --warm-runs N           Warm starts per timing pass (default 3, median reported).
#   --no-inject             Never add a saved look to Command Panel's prefs for the imported font check.
#   --skip-fallback         Leave Command Panel's theme provider alone (step 6 reports SKIP).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when an activity takes a screen. The script never writes it.
#
# Steps:
#   1. records the Command Panel look, Jarn's fonts, colors and tour prefs, and screen_focus_lock,
#   2. turns the fonts toggle on (colors off, tour done) with Jarn stopped,
#   3. for SEGMENT_LCD, E_INK, TERMINAL and AURORA_GLASS: applies the look through Command Panel's
#      debug broadcast, checks the /fonts row with content query, resumes Jarn, and captures both
#      screens. With the reader (#449) installed, Stage must redraw between looks whose fonts differ,
#   4. picks an imported font (ShareTechMono first), applies a saved look that uses it (adding one
#      when none does, unless --no-inject), checks user:<id> and its sha256 on the row, resumes Jarn
#      and checks no_backup/cp_fonts/<sha>.ttf exists with that sha256,
#   5. times cold and warm Stage starts (am start -W and the logcat "Displayed" line) with the
#      toggle on and off, and fails when the warm start with cached fonts is over 100 ms slower,
#   6. stops Command Panel and disables its theme provider, then checks Jarn still starts cleanly,
#   7. checks for crashes, ANRs and a changed screen_focus_lock, restores everything, sums up.
# Steps 3 (the Jarn side) and 4 (the cache) SKIP when the installed Jarn has no reader. Detected from
# the APK; JARNFE_CP_READER=1 or 0 overrides.
#
# Safety: debug packages only. Never uninstalls, clears data or sends keys. Command Panel's prefs are
# only written with it stopped, after a backup that is written back on exit. The provider is enabled
# again and every pref put back by an exit trap, then Jarn FE is restarted on Stage.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"
# shellcheck source=lib/prefs.sh
source "$VERIFY_DIR/lib/prefs.sh"
# shellcheck source=lib/cpfonts.sh
source "$VERIFY_DIR/lib/cpfonts.sh"

CP_PKG="${JARNFE_CP_PKG:-jr.brian.commandpannel.debug}"
LOOK_PREFS=jarnfe_look_prefs
FONTS_KEY=match_command_panel_fonts
COLORS_KEY=match_command_panel
TOUR_KEY=first_run_done
LOOKS=(SEGMENT_LCD E_INK TERMINAL AURORA_GLASS)
INJECT_LOOK_ID=jarn-verify-452
SLOW_MS=100
WARM_RUNS=3 NO_INJECT=0 SKIP_FALLBACK=0
ORIG_FONTS="" ORIG_COLORS="" ORIG_TOUR="" CP_ORIG_LOOK="" CP_WAS_RUNNING="" CP_BACKUP=""
CP_OK=0 READER=0 PROVIDER_START_ENABLED=0 TIMING_LOOK=SEGMENT_LCD
JARN_CHANGED=0 LOOK_CHANGED=0 CP_PREFS_WRITTEN=0 PROVIDER_DISABLED=0 DONE_RESTORE=0
FONT_LOOK="" TIMED_COLD="" TIMED_WARM=""
TIMING_LINES=()

usage_452() { sed -n '2,33p' "$VERIFY_DIR/verify-452-cp-font-match.sh" | sed 's/^# \{0,1\}//'; }

parse_452_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      --warm-runs) WARM_RUNS="${2:?--warm-runs needs a number}"; shift ;;
      --no-inject) NO_INJECT=1 ;;
      --skip-fallback) SKIP_FALLBACK=1 ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      -h|--help) usage_452; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
  [[ "$WARM_RUNS" =~ ^[1-9][0-9]*$ ]] || { echo "--warm-runs needs a positive number" >&2; exit 2; }
}

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 5
  START_PID="$(pidof_pkg "$PKG")"
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
}

resume_jarn() {
  send_jarn_back
  start_stage
  pause 2
}

pidof_pkg() { sh_dev pidof "$1"; }

restore_cp() {
  if [ "$PROVIDER_DISABLED" = 1 ]; then
    if text_has "$(sh_dev pm enable "$(cp_provider_component)")" enabled; then
      PROVIDER_DISABLED=0
      echo "  info  Command Panel theme provider enabled again"
    else
      echo "  WARN  could not enable $(cp_provider_component); run: adb shell pm enable $(cp_provider_component)"
    fi
  fi
  if [ "$CP_PREFS_WRITTEN" = 1 ]; then
    sh_dev am force-stop "$CP_PKG" >/dev/null
    if cp_write_prefs <"$CP_BACKUP" && [ "$(cp_current_look)" = "$CP_ORIG_LOOK" ]; then
      CP_PREFS_WRITTEN=0 LOOK_CHANGED=0
      echo "  info  Command Panel prefs written back from $CP_BACKUP"
    else
      echo "  WARN  could not write Command Panel prefs back, the backup is $CP_BACKUP"
    fi
  elif [ "$LOOK_CHANGED" = 1 ]; then
    cp_ensure_service
    sh_dev am broadcast -a "$CP_DEBUG_ACTION" -p "$CP_PKG" --es look "$CP_ORIG_LOOK" >/dev/null
    pause 1.5
    if [ "$(cp_current_look)" = "$CP_ORIG_LOOK" ]; then echo "  info  Command Panel look back to ${CP_ORIG_LOOK:-default}"
    else echo "  WARN  Command Panel look is $(cp_current_look), was ${CP_ORIG_LOOK:-default}"; fi
  fi
  if [ -n "$CP_WAS_RUNNING" ] && [ -z "$(pidof_pkg "$CP_PKG")" ]; then
    cp_ensure_service && echo "  info  Command Panel started again"
  fi
}

restore_jarn() {
  [ "$JARN_CHANGED" = 1 ] || return 0
  local key value ok=1
  for key in "$TOUR_KEY" "$COLORS_KEY" "$FONTS_KEY"; do
    case "$key" in
      "$TOUR_KEY") value="$ORIG_TOUR" ;;
      "$COLORS_KEY") value="$ORIG_COLORS" ;;
      *) value="$ORIG_FONTS" ;;
    esac
    write_bool "$key" "$value" || { ok=0; echo "  WARN  could not restore $key (was ${value:-default})"; }
  done
  [ "$ok" = 1 ] && echo "  info  Jarn fonts ${ORIG_FONTS:-default}, colors ${ORIG_COLORS:-default}, tour ${ORIG_TOUR:-default}"
  sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
}

restore_452() {
  [ "$DONE_RESTORE" = 0 ] || return 0
  DONE_RESTORE=1
  section "Restore"
  restore_cp
  restore_jarn
}

preflight_452() {
  section "Preflight"
  case "$PKG:$CP_PKG" in
    *.debug:*.debug) ;;
    *) echo "debug packages only (got $PKG and $CP_PKG)" >&2; return 1 ;;
  esac
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_prefs "$LOOK_PREFS" >/dev/null || { echo "run-as is unavailable for $PKG" >&2; return 1; }
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  ORIG_FONTS="$(look_pref "$FONTS_KEY")"
  ORIG_COLORS="$(look_pref "$COLORS_KEY")"
  ORIG_TOUR="$(look_pref "$TOUR_KEY")"
  if cp_installed && [ -n "$(cp_prefs)" ]; then
    CP_OK=1
    CP_ORIG_LOOK="$(cp_current_look)"
    CP_WAS_RUNNING="$(pidof_pkg "$CP_PKG")"
    CP_BACKUP="$OUT_DIR/command_panel-backup.xml"
    cp_prefs >"$CP_BACKUP"
    cp_provider_enabled && PROVIDER_START_ENABLED=1
    echo "  info  $CP_PKG look ${CP_ORIG_LOOK:-default}, pid ${CP_WAS_RUNNING:-none (not running)}"
  else
    echo "  info  $CP_PKG missing or its prefs unreadable, Command Panel steps will SKIP"
  fi
  if jarn_reader_installed; then READER=1; echo "  info  Jarn has the Command Panel font reader"
  else echo "  info  Jarn has no Command Panel font reader (#449), Jarn-side checks will SKIP"; fi
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  Jarn fonts ${ORIG_FONTS:-default (off)}, colors ${ORIG_COLORS:-default (off)}, tour ${ORIG_TOUR:-default}"
  echo "  info  screen_focus_lock $START_LOCK, Focus Lock mode ${LOCK_MODE:-unreadable}"
  echo "  info  Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY, output in $OUT_DIR"
}

step_toggle_on() {
  section "Fonts toggle on"
  JARN_CHANGED=1
  [ "$ORIG_TOUR" = true ] || write_bool "$TOUR_KEY" true || fail "could not mark the tour done"
  write_bool "$COLORS_KEY" false || fail "could not turn the colors switch off"
  if write_bool "$FONTS_KEY" true; then pass "match_command_panel_fonts is true"
  else fail "could not turn the fonts switch on"; return 1; fi
  [ "$CP_OK" = 0 ] || cp_ensure_service || fail "Command Panel's service did not start"
  start_stage || fail "could not bring Stage to the front"
}

capture_both() {
  shot_idle "$STAGE_PHYSICAL" "$OUT_DIR/452-$1-stage" || skip "$1: Stage screenshot failed"
  shot "$DECK_PHYSICAL" "$OUT_DIR/452-$1-deck.png" || skip "$1: Deck screenshot failed"
}

check_row_for_look() {
  local look="$1" row want display label
  row="$(cp_query_fonts)"
  printf '%s %s\n' "$look" "$row" >>"$OUT_DIR/452-fonts-rows.txt"
  want="$(cp_expected_fonts "$look")"
  display="$(cp_col "$row" display_font)"
  label="$(cp_col "$row" label_font)"
  if [ "$(cp_col "$row" version)" != 2 ]; then fail "$look: /fonts version is $(cp_col "$row" version), not 2"
  elif [ "$display $label" = "$want" ]; then pass "$look: /fonts gives $display, $label"
  else fail "$look: /fonts gives ${display:-?}, ${label:-?}, wanted ${want/ /, }"; fi
}

# Stage must redraw between two looks whose fonts differ: idle frames of $1 against a frame of $2.
compare_stage() {
  local from="$1" to="$2" prefix="$OUT_DIR/452-$1-stage" diff
  [ -f "$prefix-idle3.png" ] && [ -f "$OUT_DIR/452-$to-stage-idle1.png" ] || { skip "$from vs $to: no frames"; return; }
  cp "$OUT_DIR/452-$to-stage-idle1.png" "$prefix-after.png"
  diff="$(frame_changes "$prefix")" || { skip "$from vs $to: could not compare frames"; return; }
  if [ "$(diff_field "$diff" changed)" -gt "$CONTENT_CHANGE_CELLS" ]; then pass "Stage redrew from $from to $to ($diff)"
  else fail "Stage looks the same in $from and $to ($diff), fonts may not apply on resume"; fi
}

step_looks() {
  section "Built-in looks"
  [ "$CP_OK" = 1 ] || { skip "built-in looks: Command Panel not available"; return; }
  local look
  for look in "${LOOKS[@]}"; do
    LOOK_CHANGED=1
    cp_set_look "$look" || { fail "$look: Command Panel did not apply it"; continue; }
    check_row_for_look "$look"
    resume_jarn || fail "$look: Stage did not come back to the front"
    capture_both "$look"
  done
  if [ "$READER" = 0 ]; then skip "Jarn side of the looks: reader (#449) not installed"; return; fi
  if ! python3 -I -c 'import PIL' 2>/dev/null; then skip "Jarn side of the looks: Pillow is not installed"; return; fi
  compare_stage E_INK AURORA_GLASS
  compare_stage SEGMENT_LCD TERMINAL
}

# Sets FONT_LOOK to the saved look that uses font $1, adding one when allowed. Empty when none.
# Not run in a subshell, so CP_PREFS_WRITTEN reaches the exit trap.
look_with_font() {
  local id="$1" name="$2" file="$3" xml
  FONT_LOOK="$(cp_prefs_tool look-for "$id")"
  [ -z "$FONT_LOOK" ] && [ "$NO_INJECT" = 0 ] || return 0
  sh_dev am force-stop "$CP_PKG" >/dev/null
  xml="$(cp_prefs_tool inject "$INJECT_LOOK_ID" "$id" "$name" "$file")" || return 1
  CP_PREFS_WRITTEN=1
  printf '%s\n' "$xml" | cp_write_prefs || return 1
  [ "$(cp_prefs_tool look-for "$id")" = "$INJECT_LOOK_ID" ] || return 1
  cp_ensure_service || return 1
  FONT_LOOK="$INJECT_LOOK_ID"
  echo "  info  added saved look $INJECT_LOOK_ID (removed on exit)"
}

step_imported() {
  section "Imported font"
  [ "$CP_OK" = 1 ] || { skip "imported font: Command Panel not available"; return; }
  local font id name file look row value sha cached
  font="$(cp_prefs_tool font)"
  [ -n "$font" ] || { skip "imported font: none in Command Panel (import ShareTechMono in Theme Studio)"; return; }
  IFS=$'\t' read -r id name file <<<"$font"
  echo "  info  using imported font $name ($id)"
  look_with_font "$id" "$name" "$file" || fail "could not add a saved look for $name"
  look="$FONT_LOOK"
  [ -n "$look" ] || { skip "imported font: no saved look uses $name (--no-inject, or the prefs write failed)"; return; }
  LOOK_CHANGED=1
  cp_set_look "custom:$look" || { fail "Command Panel did not apply custom:$look"; return; }
  TIMING_LOOK="custom:$look"
  row="$(cp_query_fonts)"
  printf 'custom:%s %s\n' "$look" "$row" >>"$OUT_DIR/452-fonts-rows.txt"
  value="$(cp_col "$row" label_font)"
  sha="$(cp_col "$row" label_font_sha256)"
  if [ "$value" = "user:$id" ]; then pass "/fonts label_font is user:$id"
  else fail "/fonts label_font is ${value:-empty}, wanted user:$id"; fi
  if [[ "$sha" =~ ^[0-9a-f]{64}$ ]]; then pass "/fonts carries the font's sha256"
  else fail "/fonts label_font_sha256 is '${sha}'"; return; fi
  if [ "$READER" = 0 ]; then skip "cp_fonts cache: reader (#449) not installed"; return; fi
  resume_jarn || fail "Stage did not come back to the front"
  capture_both imported
  cached="$(jarn_cached_sha "$sha")"
  if [ "$cached" = "$sha" ]; then pass "Jarn cached no_backup/cp_fonts/$sha.ttf with a matching sha256"
  elif [ -n "$cached" ]; then fail "cp_fonts/$sha.ttf hashes to $cached"
  else fail "no cp_fonts/$sha.ttf in Jarn (found: $(sh_dev run-as "$PKG" ls no_backup/cp_fonts | tr '\n' ' '))"; fi
}

# Sets TIMED_COLD and TIMED_WARM (median TotalTime) for the current toggle state, labeled $1.
time_starts() {
  local label="$1" result cold warm_list="" i
  sh_dev am force-stop "$PKG" >/dev/null
  pause 1
  result="$(timed_jarn_start)"
  cold="$(cut -d' ' -f2 <<<"$result")"
  TIMING_LINES+=("$label cold: $result")
  pause 4
  for ((i = 0; i < WARM_RUNS; i++)); do
    send_jarn_back
    result="$(timed_jarn_start)"
    TIMING_LINES+=("$label warm $((i + 1)): $result")
    warm_list+="$(cut -d' ' -f2 <<<"$result")"$'\n'
    pause 2
  done
  TIMED_COLD="$cold"
  TIMED_WARM="$(median <<<"$warm_list")"
  echo "  info  $label: cold ${TIMED_COLD:-?} ms, warm median ${TIMED_WARM:-?} ms over $WARM_RUNS"
}

step_timing() {
  section "Start times"
  local on_cold on_warm off_cold off_warm line
  [ "$CP_OK" = 0 ] || [ "$LOOK_CHANGED" = 0 ] || cp_set_look "$TIMING_LOOK" ||
    echo "  info  could not re-apply $TIMING_LOOK for timing"
  echo "  info  look while timing: $TIMING_LOOK (state total_ms displayed_ms per start)"
  time_starts "fonts on"
  on_cold="$TIMED_COLD" on_warm="$TIMED_WARM"
  write_bool "$FONTS_KEY" false || { fail "could not turn the fonts switch off for timing"; return; }
  time_starts "fonts off"
  off_cold="$TIMED_COLD" off_warm="$TIMED_WARM"
  write_bool "$FONTS_KEY" true || fail "could not turn the fonts switch back on"
  start_stage
  for line in "${TIMING_LINES[@]}"; do echo "        $line"; done
  printf '%s\n' "${TIMING_LINES[@]}" >"$OUT_DIR/452-timing.txt"
  if [ -n "$on_warm" ] && [ -n "$off_warm" ]; then
    if [ $((on_warm - off_warm)) -gt "$SLOW_MS" ]; then
      fail "warm start with fonts on is $((on_warm - off_warm)) ms slower than off ($on_warm vs $off_warm)"
    else
      pass "warm start with fonts on within $SLOW_MS ms of off ($on_warm vs $off_warm ms)"
    fi
  else
    skip "warm start comparison: am start -W gave no TotalTime"
  fi
  [ "$READER" = 1 ] || echo "  info  without the reader both passes run the same code, so the gap is noise"
  echo "  info  cold: fonts on ${on_cold:-?} ms, fonts off ${off_cold:-?} ms"
}

disable_provider() {
  [ "$CP_OK" = 1 ] || return 1
  [ "$PROVIDER_START_ENABLED" = 1 ] || { echo "  info  provider was already disabled"; return 0; }
  PROVIDER_DISABLED=1
  sh_dev pm disable "$(cp_provider_component)" >/dev/null
  ! cp_provider_enabled
}

step_fallback() {
  section "Fallback without Command Panel"
  if [ "$SKIP_FALLBACK" = 1 ]; then skip "fallback: --skip-fallback"; return; fi
  if [ "$CP_OK" = 1 ]; then
    sh_dev am force-stop "$CP_PKG" >/dev/null
    if disable_provider && cp_fonts_unreadable; then pass "Command Panel stopped and /fonts unreadable"
    else skip "fallback: could not make /fonts unreadable, checked with Command Panel stopped only"; fi
  else
    echo "  info  Command Panel is not available, which is the fallback already"
  fi
  sh_dev am force-stop "$PKG" >/dev/null
  start_stage
  pause 2
  if jarn_is_focused; then pass "Jarn starts and holds focus with no Command Panel fonts"
  else fail "Jarn is not in front after starting without Command Panel"; fi
  capture_both fallback
}

main_452() {
  parse_452_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-452)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  trap restore_452 EXIT
  trap 'restore_452; exit 130' INT TERM
  preflight_452 || { DONE_RESTORE=1; echo "Preflight failed, nothing was changed." >&2; exit 2; }
  step_toggle_on && {
    step_looks
    step_imported
    step_timing
    step_fallback
  }
  check_no_crashes
  check_lock_unchanged
  restore_452
  section "For a human"
  echo "  look  $OUT_DIR/452-<look>-stage-idle1.png and -deck.png: Jarn text in that look's fonts"
  echo "  look  $OUT_DIR/452-imported-*.png: the imported font on both screens"
  echo "  look  $OUT_DIR/452-fallback-*.png: Jarn's own fonts with Command Panel gone"
  summary
}

main_452 "$@"

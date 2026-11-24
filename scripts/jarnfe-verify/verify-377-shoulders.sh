#!/usr/bin/env bash
# #377: L1/R1 always step the top screen's tabs and L2/R2 the bottom screen's, whichever role is
# on which screen and whichever screen has the controller.
#
#   scripts/jarnfe-verify/verify-377-shoulders.sh [options]
#
#   --launch                Open Jarn FE (JarnStageActivity) first if it is not already on screen.
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run the Select and Swap phases even if the app's Focus Lock mode would
#                           rewrite screen_focus_lock. The script still never writes it.
#   --no-swap               Skip the swapped phases.
#
# Four placements: not swapped and swapped, each with the controller on the top and then the
# bottom screen. In each one every shoulder pair is pressed once, the script checks that only the
# expected screen's tabs moved and that the controller stayed put, then presses the opposite
# shoulder to put the tab back. The Deck tab is read from the app's saved session (debug build),
# so it works from either screen. The Stage Library tab is read with uiautomator while Stage has
# the controller, and from screenshots otherwise. The swap and the active screen are restored.
#
# Safety: never uninstalls, clears data, writes settings or presses A. Keys are L1, R1, L2, R2,
# Select and L3 (Swap) only, and input stops as soon as Jarn FE loses focus.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

EXTRA_KEYS="$KEY_L3"
NO_SWAP=0 START_SWAPPED="" SWAP_RESTORED=0

usage_377() { sed -n '2,23p' "$VERIFY_DIR/verify-377-shoulders.sh" | sed 's/^# \{0,1\}//'; }

parse_377_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --launch) LAUNCH=1 ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --no-swap) NO_SWAP=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_377; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

reload_roles() {
  STAGE_DISPLAY=$TOP_DISPLAY DECK_DISPLAY=$BOTTOM_DISPLAY
  STAGE_PHYSICAL=$TOP_PHYSICAL DECK_PHYSICAL=$BOTTOM_PHYSICAL
  read_roles
}

is_swapped() { [ "$STAGE_DISPLAY" = "$BOTTOM_DISPLAY" ]; }

placement() { if is_swapped; then echo "swapped"; else echo "not swapped"; fi; }

display_name() { if [ "$1" = "$TOP_DISPLAY" ]; then echo "top"; else echo "bottom"; fi; }

physical_of() { if [ "$1" = "$TOP_DISPLAY" ]; then echo "$TOP_PHYSICAL"; else echo "$BOTTOM_PHYSICAL"; fi; }

deck_tab() { read_prefs jarnfe_session_prefs | parse_pref deck_tab; }

wait_deck_tab() {
  local want="$1" attempt
  for attempt in 1 2 3 4 5 6; do
    [ "$(deck_tab)" = "$want" ] && return 0
    pause 0.5
  done
  return 1
}

# The Library tab, or nothing when Stage has no controller or is off the Library screen.
stage_tab_now() {
  [ "$(top_display)" = "$STAGE_DISPLAY" ] || return 0
  ui_wait_role stage || return 0
  ui_selected "${LIBRARY_TABS[@]}"
}

has_pillow() { python3 -I -c 'import PIL' 2>/dev/null; }

# Content changed on physical display $1 between the idle frames at $2 and the after frame. The
# top strip counts, because the Library tab row sits there.
frames_changed() {
  local physical="$1" prefix="$2" diff
  shot "$physical" "$prefix-after.png" || return 2
  diff="$(python3 -I "$LIB_DIR/framediff.py" --keep-top \
    "$prefix-idle1.png" "$prefix-idle2.png" "$prefix-idle3.png" "$prefix-after.png")" || return 2
  echo "$(basename "$prefix"): $diff" >>"$OUT_DIR/frames.log"
  [ "$(diff_field "$diff" changed)" -gt "$CONTENT_CHANGE_CELLS" ]
}

# Picks the direction that can move from tab $1 in a strip of $2..., so a clamp never hides a step.
# Never steps onto $AVOID_TAB when it can go the other way. Sets MOVE_KEY, MOVE_NAME, UNDO_KEY
# and WANT_TAB.
pick_direction() {
  local current="$1" index
  shift
  index="$(index_of "$current" "$@")" || return 1
  local tabs=("$@")
  if [ "$index" -lt $((${#tabs[@]} - 1)) ] &&
    { [ "$index" = 0 ] || [ "${tabs[$((index + 1))]}" != "${AVOID_TAB:-}" ]; }; then
    MOVE_KEY=$TAB_NEXT MOVE_NAME=$NEXT_NAME UNDO_KEY=$TAB_PREV WANT_TAB="${tabs[$((index + 1))]}"
  else
    MOVE_KEY=$TAB_PREV MOVE_NAME=$PREV_NAME UNDO_KEY=$TAB_NEXT WANT_TAB="${tabs[$((index - 1))]}"
  fi
}

expect_controller_on() {
  local want="$1" what="$2" now
  now="$(top_display)"
  if [ "$now" = "$want" ]; then pass "$what: controller stayed on the $(display_name "$want") screen"
  else fail "$what: controller moved from display $want to ${now:-?}"; fi
}

# Stage must not change while the Deck tabs move. Baseline taken by stage_baseline.
expect_stage_still() {
  local what="$1" prefix="$2" now
  if [ -n "$STAGE_BEFORE" ]; then
    now="$(stage_tab_now)"
    if [ "$now" = "$STAGE_BEFORE" ]; then pass "$what: Stage tabs stayed on $STAGE_BEFORE"
    else fail "$what: Stage tabs moved from $STAGE_BEFORE to ${now:-unknown}"; fi
  elif [ "$STAGE_SHOT" = 1 ]; then
    frames_changed "$STAGE_PHYSICAL" "$prefix"
    case $? in
      0) fail "$what: Stage content changed too (frames in $OUT_DIR)" ;;
      1) pass "$what: Stage content stayed the same (by pixels)" ;;
      *) skip "$what: could not compare Stage frames" ;;
    esac
  else
    skip "$what: Stage could not be read"
  fi
}

stage_baseline() {
  local prefix="$1"
  STAGE_BEFORE="$(stage_tab_now)" STAGE_SHOT=0
  [ -n "$STAGE_BEFORE" ] && return 0
  has_pillow && shot_idle "$STAGE_PHYSICAL" "$prefix" && STAGE_SHOT=1
  return 0
}

probe_deck() {
  local what="$1" prefix="$2" active="$3" before
  before="$(deck_tab)"
  [ -n "$before" ] || { skip "$what: Deck tab unreadable (needs the debug build)"; return 0; }
  # The first visit to Canvas opens its tour, which would take over Stage and the shoulders.
  AVOID_TAB=Canvas pick_direction "$before" "${DECK_TABS[@]}" || { skip "$what: unknown Deck tab $before"; return 0; }
  stage_baseline "$prefix-stage"
  key "$MOVE_KEY" || return 1
  if wait_deck_tab "$WANT_TAB"; then pass "$what: $MOVE_NAME moved the Deck tabs ($before to $WANT_TAB)"
  else fail "$what: $MOVE_NAME did not move the Deck tabs (still $(deck_tab), wanted $WANT_TAB)"; fi
  expect_stage_still "$what" "$prefix-stage"
  expect_controller_on "$active" "$what"
  key "$UNDO_KEY" || return 1
  wait_deck_tab "$before" || fail "$what: could not put the Deck back on $before"
}

# With a readable Stage the step is exact. Otherwise next is tried, then previous for a last tab.
probe_stage() {
  local what="$1" prefix="$2" active="$3" deck_before before
  deck_before="$(deck_tab)"
  before="$(stage_tab_now)"
  if [ -n "$before" ]; then
    pick_direction "$before" "${LIBRARY_TABS[@]}" || { skip "$what: unknown Library tab $before"; return 0; }
    key "$MOVE_KEY" || return 1
    TAB_ROLE=stage
    expect_selected "$WANT_TAB" "$what: $MOVE_NAME moved the Stage tabs" "${LIBRARY_TABS[@]}"
  else
    probe_stage_by_frames "$what" "$prefix" || return 1
  fi
  if [ -z "$deck_before" ]; then skip "$what: Deck tab unreadable (needs the debug build)"
  elif [ "$(deck_tab)" = "$deck_before" ]; then pass "$what: Deck tabs stayed on $deck_before"
  else fail "$what: Deck tabs moved from $deck_before to $(deck_tab)"; fi
  expect_controller_on "$active" "$what"
  [ -n "${UNDO_KEY:-}" ] && { key "$UNDO_KEY" || return 1; }
  if [ -n "$before" ] && [ "$(stage_tab_now)" != "$before" ]; then
    fail "$what: could not put Stage back on $before"
  fi
  return 0
}

probe_stage_by_frames() {
  local what="$1" prefix="$2" code result
  UNDO_KEY=""
  has_pillow || { skip "$what: Stage unreadable and Pillow missing"; return 0; }
  for code in "$TAB_NEXT" "$TAB_PREV"; do
    shot_idle "$STAGE_PHYSICAL" "$prefix-$code" || { skip "$what: could not capture Stage frames"; return 0; }
    key "$code" || return 1
    pause 0.4
    frames_changed "$STAGE_PHYSICAL" "$prefix-$code"
    result=$?
    if [ "$result" = 0 ]; then
      if [ "$code" = "$TAB_NEXT" ]; then UNDO_KEY=$TAB_PREV; else UNDO_KEY=$TAB_NEXT; fi
      pass "$what: the shoulder changed Stage's content (by pixels)"
      return 0
    fi
    [ "$result" = 1 ] || { skip "$what: could not compare Stage frames"; return 0; }
  done
  fail "$what: neither shoulder changed Stage's content (frames in $OUT_DIR)"
}

# The active screen's hint bar names its own pair. Only a wrong glyph fails, since the bar
# might be hidden or drawn without text.
check_active_hint() {
  local active="$1" what="$2" role want wrong
  [ "$HINTS_PREF" != "false" ] || return 0
  if [ "$active" = "$STAGE_DISPLAY" ]; then role=stage; else role=deck; fi
  ui_wait_role "$role" || { skip "$what: hint bar unreadable"; return 0; }
  if [ "$active" = "$TOP_DISPLAY" ]; then want="L1/R1" wrong="L2/R2"; else want="L2/R2" wrong="L1/R1"; fi
  if ui_has "$want"; then pass "$what: hint bar shows $want for its tabs"
  elif ui_has "$wrong"; then fail "$what: hint bar shows $wrong, wanted $want"
  else skip "$what: no shoulder glyph found in the hint bar"; fi
}

probe_pair() {
  local display="$1" active="$2" where target what prefix
  where="$(display_name "$display")"
  tab_keys_for "$display"
  if [ "$display" = "$STAGE_DISPLAY" ]; then target=Stage; else target=Deck; fi
  what="$(placement), controller $(display_name "$active"), $PREV_NAME/$NEXT_NAME -> $where ($target)"
  prefix="$OUT_DIR/377-$(placement | tr ' ' '-')-$active-$PREV_NAME"
  [ "$ABORTED" = 0 ] || return 1
  if [ "$target" = Deck ]; then probe_deck "$what" "$prefix" "$active"; else probe_stage "$what" "$prefix" "$active"; fi
}

run_placement() {
  local active="$1"
  section "$(placement), controller on the $(display_name "$active") screen"
  if [ "$(top_display)" != "$active" ]; then
    need_handoff "moving the controller" || return 0
    activate_display "$active" || { fail "could not move the controller to display $active"; return 0; }
  fi
  echo "  info  Stage on the $(display_name "$STAGE_DISPLAY") screen, Deck on the $(display_name "$DECK_DISPLAY")"
  check_active_hint "$active" "$(placement), controller $(display_name "$active")"
  probe_pair "$TOP_DISPLAY" "$active" || return 1
  probe_pair "$BOTTOM_DISPLAY" "$active"
}

wait_swap() {
  local want="$1" attempt
  for attempt in 1 2 3 4 5 6 7 8; do
    reload_roles
    [ "$(placement)" = "$want" ] && return 0
    pause 0.5
  done
  return 1
}

toggle_swap() {
  local want
  if is_swapped; then want="not swapped"; else want="swapped"; fi
  key "$KEY_L3" || return 1
  if wait_swap "$want"; then pass "L3 swapped the roles ($want)"; return 0; fi
  fail "L3 did not swap the roles (still $(placement))"
  return 1
}

run_both_placements() {
  local other
  if [ "$1" = "$TOP_DISPLAY" ]; then other=$BOTTOM_DISPLAY; else other=$TOP_DISPLAY; fi
  run_placement "$1" || return 1
  run_placement "$other"
}

restore_377() {
  [ "$SWAP_RESTORED" = 0 ] || return 0
  SWAP_RESTORED=1
  if [ -n "$START_SWAPPED" ] && [ "$KEYS_SENT" = 1 ] && [ "$ABORTED" = 0 ]; then
    reload_roles
    [ "$(placement)" = "$START_SWAPPED" ] || toggle_swap || fail "could not restore the swap ($START_SWAPPED)"
  fi
  restore_start
}

run_377() {
  START_SWAPPED="$(placement)"
  run_both_placements "$START_DISPLAY" || return
  if [ "$NO_SWAP" = 1 ]; then skip "swapped placements: --no-swap"; return; fi
  need_handoff "Swap" || return
  section "Swap"
  toggle_swap || return
  run_both_placements "$(top_display)"
}

main_377() {
  parse_377_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-377)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  trap restore_377 EXIT
  trap 'restore_377; exit 130' INT TERM
  preflight || { RESTORED=1 SWAP_RESTORED=1; echo "Preflight failed, nothing was pressed." >&2; exit 2; }
  run_377
  restore_377
  check_no_crashes
  check_lock_unchanged
  summary
}

main_377 "$@"

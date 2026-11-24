#!/usr/bin/env bash
# One function per check. Checks that move control between screens call need_handoff first.

LIBRARY_TABS=(Systems Favorites Recent)
DECK_TABS=(Now Apps Canvas)
SETTINGS_TABS=(Library Art Look Display Apps General Data)
# Wallpaper showing through the gaps between tiles moved up to 5 cells on a real Thor idle.
CONTENT_CHANGE_CELLS=12
MIN_USABLE_CELLS=100
TAB_ROLE=stage

need_handoff() {
  [ "$ABORTED" = 0 ] || { skip "$1: input stopped earlier"; return 1; }
  [ "$HANDOFF_OK" = 1 ] && return 0
  skip "$1: $HANDOFF_REASON"
  return 1
}

index_of() {
  local want="$1" i=0 item
  shift
  for item in "$@"; do
    [ "$item" = "$want" ] && { echo "$i"; return 0; }
    i=$((i + 1))
  done
  return 1
}

wait_top_display() {
  local want="$1" i
  for i in 1 2 3 4 5 6; do
    [ "$(top_display)" = "$want" ] && return 0
    pause 0.5
  done
  return 1
}

activate_display() {
  [ "$(top_display)" = "$1" ] && return 0
  key "$KEY_SELECT" || return 1
  wait_top_display "$1"
}

# L1/R1 step the top screen's tabs and L2/R2 the bottom screen's, whichever screen is active.
# Sets TAB_PREV, TAB_NEXT and their names for the tabs on display $1.
tab_keys_for() {
  if [ "$1" = "$TOP_DISPLAY" ]; then
    TAB_PREV=$KEY_L1 TAB_NEXT=$KEY_R1 PREV_NAME=L1 NEXT_NAME=R1
  else
    TAB_PREV=$KEY_L2 TAB_NEXT=$KEY_R2 PREV_NAME=L2 NEXT_NAME=R2
  fi
}

# Clamped tabs make this deterministic: previous to the first tab, then next to the target.
# $3 is the display the tabs are on.
step_to_tab() {
  tab_keys_for "$3"
  press_times "$TAB_PREV" "$(($2 - 1))" && press_times "$TAB_NEXT" "$1"
}

# Call while the matching screen is active, before anything moves its tabs.
remember_deck_tab() {
  [ -z "$START_DECK_TAB" ] || return 0
  ui_wait_role deck && START_DECK_TAB="$(ui_selected "${DECK_TABS[@]}")"
}

remember_library_tab() {
  [ -z "$START_LIBRARY_TAB" ] || return 0
  ui_wait_role stage && START_LIBRARY_TAB="$(ui_selected "${LIBRARY_TABS[@]}")"
}

check_displays() {
  section "Displays"
  local dump
  dump="$(sh_dev dumpsys display)"
  if text_has "$dump" "local:$TOP_PHYSICAL"; then pass "top screen present ($TOP_PHYSICAL)"
  else fail "top screen $TOP_PHYSICAL not found"; fi
  if text_has "$dump" "local:$BOTTOM_PHYSICAL"; then pass "bottom screen present ($BOTTOM_PHYSICAL)"
  else fail "bottom screen $BOTTOM_PHYSICAL not found"; fi
}

check_activities() {
  section "Both activities resumed"
  local dump resumed
  dump="$(sh_dev dumpsys activity activities)"
  resumed="$(printf '%s\n' "$dump" | parse_resumed "$TOP_DISPLAY")"
  if text_has "$resumed" "$PKG/$STAGE_ACTIVITY"; then
    pass "Stage activity resumed on display $TOP_DISPLAY"
  else
    fail "Stage activity not resumed on display $TOP_DISPLAY"
  fi
  resumed="$(printf '%s\n' "$dump" | parse_resumed "$BOTTOM_DISPLAY")"
  if text_has "$resumed" "$PKG/$DECK_ACTIVITY"; then
    pass "Deck activity resumed on display $BOTTOM_DISPLAY"
  else
    fail "Deck activity not resumed on display $BOTTOM_DISPLAY"
  fi
}

check_focus_lock() {
  section "Focus lock (read only)"
  case "$START_LOCK" in
    0|1|2) pass "screen_focus_lock is $START_LOCK" ;;
    *) fail "unexpected screen_focus_lock: ${START_LOCK:-empty}" ;;
  esac
  echo "  info  app focus lock mode: ${LOCK_MODE:-unreadable}"
  if [ "$HANDOFF_OK" = 1 ]; then pass "screen switching checks are allowed to run"
  else skip "screen switching checks held back: $HANDOFF_REASON"; fi
}

check_switching() {
  section "Select switches the active screen"
  need_handoff "screen switching" || return
  local from to
  from="$(top_display)"
  if [ "$from" = "$TOP_DISPLAY" ]; then to="$BOTTOM_DISPLAY"; else to="$TOP_DISPLAY"; fi
  key "$KEY_SELECT" || return
  if wait_top_display "$to"; then pass "Select moved focus to display $to"
  else fail "Select did not move focus to display $to (now $(top_display))"; fi
  key "$KEY_SELECT" || return
  if wait_top_display "$from"; then pass "Select moved focus back to display $from"
  else fail "Select did not return focus to display $from (now $(top_display))"; fi
}

# One D-pad move on the active screen. Sets the frame diff results for both screens.
measure_dpad() {
  local active="$1" inactive="$2" code="$3" name="$4"
  shot_idle "$active" "$OUT_DIR/$name-active" || return 1
  shot_idle "$inactive" "$OUT_DIR/$name-inactive" || return 1
  key "$code" || return 1
  pause 0.4
  shot "$active" "$OUT_DIR/$name-active-after.png" || return 1
  shot "$inactive" "$OUT_DIR/$name-inactive-after.png" || return 1
  ACTIVE_DIFF="$(frame_changes "$OUT_DIR/$name-active")" || return 1
  INACTIVE_DIFF="$(frame_changes "$OUT_DIR/$name-inactive")" || return 1
}

# A focus ring on the inactive screen is always a leak. Content only counts when $1 is "strict":
# the Deck is meant to follow the Stage selection, but nothing on the Stage follows the Deck.
inactive_leaked() {
  [ "$(diff_field "$INACTIVE_DIFF" ring)" = 1 ] && return 0
  [ "$1" = strict ] || return 1
  [ "$(diff_field "$INACTIVE_DIFF" usable)" -ge "$MIN_USABLE_CELLS" ] || return 1
  [ "$(diff_field "$INACTIVE_DIFF" changed)" -gt "$CONTENT_CHANGE_CELLS" ]
}

check_dpad_one() {
  local label="$1" active="$2" inactive="$3" name="$4" rule="$5" code moved=0
  for code in "$KEY_DOWN" "$KEY_UP" "$KEY_RIGHT" "$KEY_LEFT"; do
    measure_dpad "$active" "$inactive" "$code" "$name" || { skip "$label: could not capture frames"; return; }
    if [ "$(diff_field "$ACTIVE_DIFF" ring)" = 1 ]; then moved=1; break; fi
  done
  if [ "$moved" = 0 ]; then
    skip "$label: no visible focus move on the active screen ($ACTIVE_DIFF)"
    return
  fi
  if inactive_leaked "$rule"; then
    measure_dpad "$active" "$inactive" "$code" "$name-retry" || { skip "$label: could not capture frames"; return; }
    if inactive_leaked "$rule"; then
      fail "$label: focus reached the inactive screen ($INACTIVE_DIFF), frames in $OUT_DIR"
      return
    fi
  fi
  pass "$label: focus ring moved on the active screen only"
}

check_dpad() {
  section "D-pad reaches only the active screen"
  need_handoff "D-pad isolation" || return
  if ! python3 -I -c 'import PIL' 2>/dev/null; then skip "D-pad isolation: Pillow is not installed"; return; fi
  activate_display "$DECK_DISPLAY" || { fail "could not activate the Deck"; return; }
  remember_deck_tab
  step_to_tab 1 "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return
  DECK_TAB_MOVED=1
  check_dpad_one "Deck active" "$DECK_PHYSICAL" "$STAGE_PHYSICAL" dpad-deck strict
  [ "$ABORTED" = 0 ] || return
  activate_display "$STAGE_DISPLAY" || { fail "could not activate Stage"; return; }
  remember_library_tab
  check_dpad_one "Stage active" "$STAGE_PHYSICAL" "$DECK_PHYSICAL" dpad-stage follows
}

check_hints() {
  section "Hints"
  if [ "$HINTS_PREF" = "false" ]; then skip "hint bar is turned off in Settings"; return; fi
  if [ "$(top_display)" != "$STAGE_DISPLAY" ]; then skip "Stage is not active"; return; fi
  if ! ui_wait_role stage; then skip "Stage could not be read with uiautomator"; return; fi
  if ui_has "Switch screen"; then pass "Stage shows the Switch screen hint"
  else fail "Stage is missing the Switch screen hint"; fi
  if ui_has "Back"; then fail "Stage root shows a Back hint with nothing to go back to"
  else pass "Stage root has no Back hint"; fi
  need_handoff "Deck hints" || return
  activate_display "$DECK_DISPLAY" || { fail "could not activate the Deck"; return; }
  if ui_wait_role deck; then
    if ui_has "Back"; then pass "Deck shows the Back hint"; else fail "Deck is missing the Back hint"; fi
  else
    skip "Deck could not be read with uiautomator"
  fi
  activate_display "$STAGE_DISPLAY" || fail "could not return to Stage"
}

# Polls because the dump can lag a moment behind a tab change.
expect_selected() {
  local want="$1" what="$2" got="" attempt
  shift 2
  for attempt in 1 2 3 4; do
    ui_wait_role "$TAB_ROLE" || { skip "$what: no UI dump"; return 1; }
    got="$(ui_selected "$@")"
    [ "$got" = "$want" ] && { pass "$what: $want"; return 0; }
    pause 0.5
  done
  fail "$what: expected $want, got ${got:-nothing}"
}

# Fallback when the screen can't be read: each next press must visibly replace the content.
check_tabs_by_frames() {
  local physical="$1" label="$2" count="$3" display="$4" i prefix diff
  tab_keys_for "$display"
  press_times "$TAB_PREV" "$((count - 1))" || return 1
  for ((i = 1; i < count; i++)); do
    prefix="$OUT_DIR/tabs-$label-$i"
    shot_idle "$physical" "$prefix" || { skip "$label: could not capture frames"; return 0; }
    key "$TAB_NEXT" || return 1
    pause 0.4
    shot "$physical" "$prefix-after.png" || { skip "$label: could not capture frames"; return 0; }
    diff="$(frame_changes "$prefix")" || { skip "$label: could not compare frames"; return 0; }
    if [ "$(diff_field "$diff" changed)" -gt "$CONTENT_CHANGE_CELLS" ]; then
      pass "$label: $NEXT_NAME changed the content (step $i, by pixels)"
    else
      fail "$label: $NEXT_NAME did not change the content (step $i, $diff)"
    fi
  done
}

check_stage_tabs() {
  TAB_ROLE=stage
  if ! ui_wait_role stage; then
    check_tabs_by_frames "$STAGE_PHYSICAL" "Stage tabs" "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY"
    return
  fi
  local i focused=""
  remember_library_tab
  [ -n "$(ui_selected "${LIBRARY_TABS[@]}")" ] || { skip "Stage tabs: not on the Library screen"; return; }
  tab_keys_for "$STAGE_DISPLAY"
  press_times "$TAB_PREV" 2 || return
  expect_selected Systems "$PREV_NAME clamps to the first tab" "${LIBRARY_TABS[@]}"
  key "$TAB_NEXT" || return
  expect_selected Favorites "$NEXT_NAME moves one tab" "${LIBRARY_TABS[@]}"
  key "$TAB_NEXT" || return
  expect_selected Recent "$NEXT_NAME moves one tab" "${LIBRARY_TABS[@]}"
  key "$TAB_NEXT" || return
  expect_selected Recent "$NEXT_NAME clamps at the last tab" "${LIBRARY_TABS[@]}"
  press_times "$TAB_PREV" 2 || return
  for i in 1 2 3 4 5; do
    ui_wait_role stage || break
    focused="$(ui_focused)"
    [ "$focused" = "Systems" ] && break
    key "$KEY_UP" || return
    focused=""
  done
  if [ "$focused" = "Systems" ]; then
    key "$KEY_RIGHT" || return
    expect_selected Favorites "focusing a tab selects it" "${LIBRARY_TABS[@]}"
    key "$KEY_LEFT" || return
    expect_selected Systems "focusing a tab selects it" "${LIBRARY_TABS[@]}"
    key "$KEY_DOWN" || return
  else
    skip "tab focus: D-pad Up did not reach the tab row"
  fi
}

check_deck_tabs() {
  need_handoff "Deck tabs" || return
  activate_display "$DECK_DISPLAY" || { fail "could not activate the Deck"; return; }
  TAB_ROLE=deck
  DECK_TAB_MOVED=1
  if ui_wait_role deck; then
    remember_deck_tab
    tab_keys_for "$DECK_DISPLAY"
    press_times "$TAB_PREV" 2 || return
    expect_selected Now "$PREV_NAME clamps to the first Deck tab" "${DECK_TABS[@]}"
    key "$TAB_NEXT" || return
    expect_selected Apps "$NEXT_NAME moves one Deck tab" "${DECK_TABS[@]}"
    key "$TAB_NEXT" || return
    expect_selected Canvas "$NEXT_NAME moves one Deck tab" "${DECK_TABS[@]}"
    key "$TAB_NEXT" || return
    expect_selected Canvas "$NEXT_NAME clamps at the last Deck tab" "${DECK_TABS[@]}"
  else
    check_tabs_by_frames "$DECK_PHYSICAL" "Deck tabs" "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return
  fi
  activate_display "$STAGE_DISPLAY" || fail "could not return to Stage"
}

check_tabs() {
  section "Tabs"
  if [ "$(top_display)" = "$STAGE_DISPLAY" ]; then check_stage_tabs; else skip "Stage tabs: Stage is not active"; fi
  [ "$ABORTED" = 0 ] || return
  check_deck_tabs
}

check_settings_by_ui() {
  local original name
  TAB_ROLE=deck
  original="$(ui_selected "${SETTINGS_TABS[@]}")"
  tab_keys_for "$DECK_DISPLAY"
  press_times "$TAB_PREV" "$((${#SETTINGS_TABS[@]} - 1))" || return
  expect_selected Library "$PREV_NAME clamps to the first section" "${SETTINGS_TABS[@]}"
  for name in Art Look Display Apps General Data; do
    key "$TAB_NEXT" || return
    expect_selected "$name" "$NEXT_NAME opens section" "${SETTINGS_TABS[@]}"
  done
  key "$TAB_NEXT" || return
  expect_selected Data "$NEXT_NAME clamps at the last section" "${SETTINGS_TABS[@]}"
  [ -n "$original" ] && step_to_tab "$(index_of "$original" "${SETTINGS_TABS[@]}")" "${#SETTINGS_TABS[@]}" "$DECK_DISPLAY"
}

# The starting section can't be read here, so it ends on the first one, the session default.
check_settings_by_frames() {
  check_tabs_by_frames "$DECK_PHYSICAL" "Settings" "${#SETTINGS_TABS[@]}" "$DECK_DISPLAY" || return
  press_times "$TAB_PREV" "$((${#SETTINGS_TABS[@]} - 1))" || return
  echo "  info  Settings section could not be read, left on the first section"
}

# Refuses only when Stage is readably off Library. If it can't be read, Start and B are still
# safe from any route, since Start opens Settings and B pops back to where it was.
settings_start_is_safe() {
  ui_wait_role stage || return 0
  [ -n "$(ui_selected "${LIBRARY_TABS[@]}")" ]
}

# Uses shoulder buttons only. D-pad could adjust a focused slider, and A could toggle a row.
check_settings() {
  section "Settings sections"
  need_handoff "Settings" || return
  activate_display "$STAGE_DISPLAY" || { fail "could not activate Stage"; return; }
  if ! settings_start_is_safe; then
    skip "Settings: Stage is not on the Library screen"
    return
  fi
  key "$KEY_START" || return
  IN_SETTINGS=1
  if wait_top_display "$DECK_DISPLAY"; then pass "Start opens Settings with control on the Deck"
  else fail "Start did not move control to the Deck"; fi
  if ui_wait_role deck && [ -n "$(ui_selected "${SETTINGS_TABS[@]}")" ]; then
    check_settings_by_ui
  elif python3 -I -c 'import PIL' 2>/dev/null; then
    check_settings_by_frames
  else
    skip "Settings sections: Deck unreadable and Pillow missing"
  fi
  [ "$ABORTED" = 0 ] || return
  key "$KEY_B" || return
  IN_SETTINGS=0
  if wait_top_display "$STAGE_DISPLAY"; then pass "B leaves Settings and returns control to Stage"
  else fail "B did not return control to Stage"; fi
}

check_no_crashes() {
  section "No crashes"
  local crash system pid escaped
  crash="$(sh_dev logcat -d -b crash -T "$LOG_START")"
  if text_has "$crash" "Process: $PKG,"; then
    fail "crash logged for $PKG"
    printf '%s\n' "$crash" >"$OUT_DIR/crash.log"
    echo "        details: $OUT_DIR/crash.log"
  else
    pass "no fatal exception"
  fi
  system="$(sh_dev logcat -d -b system -T "$LOG_START")"
  escaped="$(sed 's/\./\\./g' <<<"$PKG")"
  if text_matches "$system" "ANR in $escaped( |$)"; then fail "ANR logged for $PKG"
  else pass "no ANR"; fi
  pid="$(sh_dev pidof "$PKG")"
  if [ -n "$pid" ] && [ "$pid" = "$START_PID" ]; then pass "process stayed alive (pid $pid)"
  else fail "process changed: started ${START_PID:-none}, now ${pid:-none}"; fi
}

check_lock_unchanged() {
  section "Focus lock unchanged"
  local now
  now="$(focus_lock_value)"
  if [ "$now" = "$START_LOCK" ]; then pass "screen_focus_lock still $now"
  else fail "screen_focus_lock changed from $START_LOCK to ${now:-empty} (left as is, not repaired)"; fi
}

manifest_has_jarn() { grep -qiE 'jarnfe|JarnStageActivity|JarnDeckActivity' "$1"; }

check_release_sources() {
  local file found=0
  for file in "$REPO_ROOT"/app/src/*/AndroidManifest.xml "$REPO_ROOT"/jarnfe/src/*/AndroidManifest.xml; do
    [ -f "$file" ] || continue
    case "$file" in */src/debug/*|*Debug/*) continue ;; esac
    if manifest_has_jarn "$file" || { case "$file" in */jarnfe/*) grep -q '<activity' "$file" ;; *) false ;; esac; }; then
      fail "Jarn FE entry in ${file#"$REPO_ROOT"/}"
      found=1
    fi
  done
  [ "$found" = 0 ] && pass "no Jarn FE entries in non-debug source manifests"
}

check_release_merged() {
  local file count=0 found=0
  while IFS= read -r file; do
    count=$((count + 1))
    if manifest_has_jarn "$file"; then fail "Jarn FE entry in ${file#"$REPO_ROOT"/}"; found=1; fi
  done < <(find "$REPO_ROOT/app/build/intermediates" -type f -name AndroidManifest.xml \
    -path '*merged_manifest*' -path '*[Rr]elease*' 2>/dev/null)
  if [ "$count" = 0 ]; then skip "no merged release manifest built yet"
  elif [ "$found" = 0 ]; then pass "merged release manifests are clean ($count)"; fi
}

newest_aapt2() {
  local sdk="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
  find "$sdk/build-tools" -type f -name aapt2 2>/dev/null | sort -V | tail -1
}

check_release_apk() {
  [ -n "$APK" ] || { skip "no APK given (--apk PATH)"; return; }
  [ -f "$APK" ] || { fail "APK not found: $APK"; return; }
  local aapt
  aapt="$(newest_aapt2)"
  [ -n "$aapt" ] || { skip "aapt2 not found in the Android SDK"; return; }
  local tree="$OUT_DIR/apk-manifest.txt"
  "$aapt" dump xmltree --file AndroidManifest.xml "$APK" >"$tree" 2>/dev/null || { fail "could not read $APK"; return; }
  if manifest_has_jarn "$tree"; then fail "Jarn FE entry in the APK manifest"; else pass "APK manifest is clean"; fi
}

check_release_installed() {
  [ "$HAS_DEVICE" = 1 ] || { skip "installed release: no device"; return; }
  local paths dump
  paths="$(sh_dev pm path "$RELEASE_PKG")"
  if ! text_matches "$paths" '^package:'; then
    skip "$RELEASE_PKG is not installed"
    return
  fi
  dump="$(sh_dev dumpsys package "$RELEASE_PKG")"
  if text_matches "$dump" 'jr\.brian\.jarnfe|JarnStageActivity|JarnDeckActivity'; then
    fail "installed $RELEASE_PKG declares Jarn FE components"
  else
    pass "installed $RELEASE_PKG declares no Jarn FE components"
  fi
}

check_release() {
  section "Release manifest has no Jarn FE entries"
  check_release_sources
  check_release_merged
  check_release_apk
  check_release_installed
}

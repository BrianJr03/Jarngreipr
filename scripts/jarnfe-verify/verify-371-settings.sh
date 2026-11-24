#!/usr/bin/env bash
# Jarn FE Settings layout check for #371 on the AYN Thor.
#
#   scripts/jarnfe-verify/verify-371-settings.sh [options]
#
#   --out DIR               Where dumps and screenshots go (default: build/verify-371/<timestamp>).
#   --no-dialog             Skip the RSS Clear dialog check, so A is never pressed.
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when control moves between screens. The script never writes it.
#
# Opens JarnStageActivity if it is not on screen, opens Settings with Start, then for every
# section walks down to the last row and back up to the tabs with the D-pad. Each step dumps the
# UI and checks that every group heading shows, and that a group's first row never has its
# heading hidden above the screen. Screenshots of both screens are taken at the top and bottom
# of each section. Finally it opens the RSS Clear dialog, cancels it with B, and checks focus is
# back on the Clear row.
#
# Safety: never uninstalls, clears data, changes or restores settings, or writes
# screen_focus_lock. Keys are D-pad Up and Down, B, the Deck's tab shoulders (L2/R2 on the bottom
# screen, L1/R1 on the top), Start and X. A is pressed once,
# only while the dump shows the Clear row focused, and the dialog it opens is cancelled with B.
# Input stops as soon as Jarn FE loses focus. On exit it returns to the starting section, leaves
# Settings, and gives control back to the starting screen.
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
STAGE_ACTIVITY="jr.brian.jarnfe.shell.JarnStageActivity"
TOP_DISPLAY=0
BOTTOM_DISPLAY=4
TOP_PHYSICAL=4630946441858561667
BOTTOM_PHYSICAL=4630946482288158084
KEY_A=96
MAX_ROWS=60
CLEAR_LABEL="Clear saved play positions"
CLEAR_DIALOG_TITLE="Clear play positions?"

# Headings per section, in order. A trailing ? marks one that only shows with certain data.
HEADINGS_Library=("Game folders" "Systems?")
HEADINGS_Art=("Game art" "Scraping" "System art")
HEADINGS_Look=("Theme" "Wallpaper")
HEADINGS_Display=("Tiles" "Feedback and motion" "Two screens")
HEADINGS_Apps=("App icons and names" "Dock")
HEADINGS_General=("Experience?" "Sound and tools" "RSS feeds?" "Tours" "About and support")
HEADINGS_Data=("Backup" "Updates")

DIALOG=1 ALLOW_SYNC=0 HANDOFF_OK=0 IN_SETTINGS=0 RESTORED=0
HANDOFF_REASON="" OUT_DIR="" LOCK_MODE="" START_LOCK="" START_DISPLAY="" START_SECTION=""
STAGE_DISPLAY=$TOP_DISPLAY DECK_DISPLAY=$BOTTOM_DISPLAY
STAGE_PHYSICAL=$TOP_PHYSICAL DECK_PHYSICAL=$BOTTOM_PHYSICAL
LIBRARY_TAB_AT_START=""
FOCUS="" FIRST_RESULTS=""

usage() { sed -n '2,27p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

parse_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --no-dialog) DIALOG=0 ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
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
    echo "more than one device attached, set ANDROID_SERIAL" >&2
    exit 2
  else
    export ANDROID_SERIAL="$serials"
  fi
}

read_roles() {
  local prefs
  prefs="$(read_prefs screen_assignment_prefs)" || return 0
  if [ "$(printf '%s' "$prefs" | parse_pref assignment)" = "FRONTEND_ON_BOTTOM" ]; then
    STAGE_DISPLAY=$BOTTOM_DISPLAY DECK_DISPLAY=$TOP_DISPLAY
    STAGE_PHYSICAL=$BOTTOM_PHYSICAL DECK_PHYSICAL=$TOP_PHYSICAL
  fi
}

# Same gate as run.sh: moving control between screens can make the app sync screen_focus_lock.
read_focus_lock_gate() {
  local prefs
  START_LOCK="$(focus_lock_value)"
  if prefs="$(read_prefs focus_lock_prefs)"; then
    LOCK_MODE="$(printf '%s' "$prefs" | parse_pref focus_lock_mode)"
    LOCK_MODE="${LOCK_MODE:-Follow}"
  fi
  if [ "$ALLOW_SYNC" = 1 ] || { [ -n "$LOCK_MODE" ] && focus_sync_is_inert "$LOCK_MODE" "$START_LOCK"; }; then
    HANDOFF_OK=1
  else
    HANDOFF_REASON="Focus Lock mode ${LOCK_MODE:-unreadable} may rewrite screen_focus_lock (--allow-app-focus-sync to run)"
  fi
}

jarn_is_focused() {
  local display
  display="$(top_display)"
  case "$(focused_app "${display:-0}")" in "$PKG"/*) return 0 ;; *) return 1 ;; esac
}

open_stage() {
  jarn_is_focused && return 0
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 6
  jarn_is_focused
}

preflight() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  open_stage || { echo "Could not bring $STAGE_ACTIVITY to the front" >&2; return 1; }
  activate_display "$STAGE_DISPLAY" || { echo "Could not activate Stage" >&2; return 1; }
  START_DISPLAY="$(top_display)"
  settings_start_is_safe || { echo "Stage is not on the Library screen, so Start is not pressed" >&2; return 1; }
  ui_wait_role stage && LIBRARY_TAB_AT_START="$(ui_selected "${LIBRARY_TABS[@]}")"
  echo "  info  package $PKG, Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY"
  echo "  info  output in $OUT_DIR"
}

open_settings() {
  section "Open Settings"
  key "$KEY_START" || return 1
  IN_SETTINGS=1
  if ! ui_wait_role deck; then fail "Settings did not show on the Deck"; return 1; fi
  START_SECTION="$(ui_selected "${SETTINGS_TABS[@]}")"
  pass "Start opened Settings (section ${START_SECTION:-unknown})"
  settings_tab_keys
  press_times "$TAB_PREV" "$((${#SETTINGS_TABS[@]} - 1))" || return 1
  ui_dump
  [ "$(ui_selected "${SETTINGS_TABS[@]}")" = Library ] || { fail "$PREV_NAME did not reach Library"; return 1; }
}

# Settings sections sit on the Deck, so they step with that screen's shoulder pair.
settings_tab_keys() {
  if declare -F tab_keys_for >/dev/null; then
    tab_keys_for "$DECK_DISPLAY"
  else
    TAB_PREV=$KEY_L1 TAB_NEXT=$KEY_R1 PREV_NAME=L1 NEXT_NAME=R1
  fi
}

step_to_settings_tab() {
  settings_tab_keys
  press_times "$TAB_PREV" "$((${#SETTINGS_TABS[@]} - 1))" &&
    press_times "$TAB_NEXT" "$(index_of "$1" "${SETTINGS_TABS[@]}")"
}

# Dumps the Deck, saves the dump, and runs the heading check. Sets FOCUS and FIRST_RESULTS.
inspect() {
  local name="$1" step="$2" out
  shift 2
  ui_wait_role deck || { FOCUS="" FIRST_RESULTS=""; return 1; }
  cp "$UI_XML" "$OUT_DIR/$name/dump-$step.xml"
  out="$(python3 -I "$LIB_DIR/settingsquery.py" "$OUT_DIR/$name/state.json" "$UI_XML" "$@")" || return 1
  FOCUS="$(awk -F'\t' '$1 == "focus" { print $2 }' <<<"$out")"
  FIRST_RESULTS="$(awk -F'\t' '$1 == "first" { print $2 "\t" $3 }' <<<"$out")"
  awk -F'\t' '$1 == "seen" { print $2 }' <<<"$out" >>"$OUT_DIR/$name/seen.txt"
}

record_first() {
  local name="$1" heading result
  [ -n "$FIRST_RESULTS" ] || return 0
  while IFS=$'\t' read -r heading result; do
    [ -n "$heading" ] || continue
    if [ "$result" = ok ]; then
      echo "$heading" >>"$OUT_DIR/$name/first-ok.txt"
    else
      fail "$name: \"$heading\" hidden while its first row \"$FOCUS\" is focused (see $OUT_DIR/$name)"
      shot "$DECK_PHYSICAL" "$OUT_DIR/$name/hidden-$(tr -c 'A-Za-z0-9' '_' <<<"$heading").png"
    fi
  done <<<"$FIRST_RESULTS"
}

shoot_both() {
  local name="$1" when="$2"
  shot "$TOP_PHYSICAL" "$OUT_DIR/$name-$when-top.png" || skip "$name: top screenshot failed"
  shot "$BOTTOM_PHYSICAL" "$OUT_DIR/$name-$when-bottom.png" || skip "$name: bottom screenshot failed"
}

# Walks with $2 until focus stops moving or reaches the tabs. Steps continue from $3.
walk() {
  local name="$1" code="$2" step="$3" last="" same=0 i
  shift 3
  for ((i = 0; i < MAX_ROWS; i++)); do
    key "$code" || return 1
    step=$((step + 1))
    inspect "$name" "$step" "$@" || { skip "$name: Deck dump unreadable at step $step"; continue; }
    record_first "$name"
    if [ "$code" = "$KEY_UP" ] && text_has_line "$(printf '%s\n' "${SETTINGS_TABS[@]}")" "$FOCUS"; then break; fi
    if [ "$FOCUS" = "$last" ]; then
      same=$((same + 1))
      [ "$same" -ge 2 ] && break
    else
      same=0
    fi
    last="$FOCUS"
  done
  WALK_STEP=$step
}

expect_headings() {
  local name="$1" heading plain
  shift
  for heading in "$@"; do
    plain="${heading%\?}"
    if grep -qxF -- "$plain" "$OUT_DIR/$name/seen.txt" 2>/dev/null; then
      if grep -qxF -- "$plain" "$OUT_DIR/$name/first-ok.txt" 2>/dev/null; then
        pass "$name: \"$plain\" shows, and shows with its first row"
      else
        skip "$name: \"$plain\" shows, but its first row was never matched to it"
      fi
    elif [ "$plain" != "$heading" ]; then
      echo "  info  $name: \"$plain\" is not shown here (depends on device data)"
    else
      fail "$name: heading \"$plain\" never appeared"
    fi
  done
}

check_section() {
  local name="$1" headings=()
  eval "headings=(\"\${HEADINGS_$name[@]}\")"
  local plain=("${headings[@]%\?}")
  section "Settings: $name"
  mkdir -p "$OUT_DIR/$name"
  : >"$OUT_DIR/$name/seen.txt"
  : >"$OUT_DIR/$name/first-ok.txt"
  ui_dump
  [ "$(ui_selected "${SETTINGS_TABS[@]}")" = "$name" ] || { fail "$name: section is not selected"; return 1; }
  inspect "$name" 0 "${plain[@]}" && record_first "$name"
  shoot_both "$name" top
  walk "$name" "$KEY_DOWN" 0 "${plain[@]}" || return 1
  shoot_both "$name" bottom
  walk "$name" "$KEY_UP" "$WALK_STEP" "${plain[@]}" || return 1
  expect_headings "$name" "${headings[@]}"
}

# A is sent only after a fresh dump shows the Clear row focused, and only on the Jarn FE screen.
press_a_on_clear() {
  ui_wait_role deck || return 1
  [ "$(ui_focused | awk -F' [|] ' '{ print $1 }')" = "$CLEAR_LABEL" ] || return 1
  jarn_is_focused || { ABORTED=1; return 1; }
  sh_dev input -d 0 keyevent "$KEY_A" >/dev/null
  KEYS_SENT=1
  pause
}

check_clear_dialog() {
  section "RSS Clear dialog returns focus"
  [ "$DIALOG" = 1 ] || { skip "RSS Clear dialog: --no-dialog"; return; }
  [ "$ABORTED" = 0 ] || { skip "RSS Clear dialog: input stopped earlier"; return; }
  step_to_settings_tab General || return
  local i found=0
  for ((i = 0; i < MAX_ROWS; i++)); do
    key "$KEY_DOWN" || return
    ui_wait_role deck || continue
    if [ "$(ui_focused | awk -F' [|] ' '{ print $1 }')" = "$CLEAR_LABEL" ]; then found=1; break; fi
  done
  [ "$found" = 1 ] || { skip "RSS Clear dialog: no Clear row (no RSS feeds service?)"; return; }
  press_a_on_clear || { fail "RSS Clear dialog: refused to press A, Clear row not focused"; return; }
  ui_dump
  if ! ui_has "$CLEAR_DIALOG_TITLE"; then
    fail "RSS Clear dialog: A did not open the dialog, B not pressed"
    return
  fi
  shoot_both General clear-dialog
  key "$KEY_B" || return
  ui_wait_role deck
  if ui_has "$CLEAR_DIALOG_TITLE"; then fail "RSS Clear dialog: B did not close the dialog"; return; fi
  local focus
  focus="$(ui_focused | awk -F' [|] ' '{ print $1 }')"
  if [ "$focus" = "$CLEAR_LABEL" ]; then pass "B closes the dialog and focus returns to the Clear row"
  else fail "RSS Clear dialog: focus returned to \"${focus:-nothing}\", not the Clear row"; fi
}

restore_start() {
  [ "$RESTORED" = 0 ] || return 0
  RESTORED=1
  [ "$KEYS_SENT" = 1 ] && [ "$ABORTED" = 0 ] || return 0
  if [ "$IN_SETTINGS" = 1 ]; then
    if [ -n "$START_SECTION" ]; then
      step_to_settings_tab "$START_SECTION"
    fi
    key "$KEY_B" && IN_SETTINGS=0
  fi
  if [ -n "$START_DISPLAY" ] && [ "$(top_display)" != "$START_DISPLAY" ]; then
    key "$KEY_X" && wait_top_display "$START_DISPLAY"
  fi
  if [ -n "$LIBRARY_TAB_AT_START" ] && ui_wait_role stage; then
    [ "$(ui_selected "${LIBRARY_TABS[@]}")" = "$LIBRARY_TAB_AT_START" ] || fail "Library tab changed from $LIBRARY_TAB_AT_START"
  fi
}

summary() {
  printf '\n== Summary\n  PASS %d   FAIL %d   SKIP %d\n' "$PASS_COUNT" "$FAIL_COUNT" "$SKIP_COUNT"
  [ "$ABORTED" = 0 ] || echo "  Input was stopped early because Jarn FE lost focus."
  echo "  Screenshots and dumps: $OUT_DIR"
  [ "$FAIL_COUNT" = 0 ]
}

main() {
  parse_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$REPO_ROOT/build/verify-371/$(date +%Y%m%d-%H%M%S)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  trap restore_start EXIT
  trap 'restore_start; exit 130' INT TERM
  preflight || { RESTORED=1; echo "Preflight failed, nothing was pressed." >&2; exit 2; }
  local name
  if open_settings; then
    for name in "${SETTINGS_TABS[@]}"; do
      [ "$ABORTED" = 0 ] || break
      [ "$name" = Library ] || key "$TAB_NEXT" || break
      check_section "$name"
    done
    check_clear_dialog
  fi
  restore_start
  check_lock_unchanged
  summary
}

main "$@"

#!/usr/bin/env bash
# Reaching and flipping the "Match Command Panel fonts" row in Settings > Look with the controller,
# for builds without run-as. Callers source run.sh first.

KEY_A=96
FONTS_LABEL="Match Command Panel fonts"
UNAVAILABLE_PREFIX="Unavailable."
MAX_ROWS=30
BEFORE_FOCUS="" FONTS_FOCUS="" UI_FONTS="" UI_FONTS_BEFORE=""

focused_label() { ui_focused | awk -F' [|] ' '{ print $1 }'; }
ui_checked() { python3 -I "$LIB_DIR/uiquery.py" "$UI_XML" checked; }

open_look_settings() {
  ui_wait_role stage || return 1
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY" || return 1
  settings_start_is_safe || { fail "Stage is not on Library, so Start is not pressed"; return 1; }
  key "$KEY_START" || return 1
  IN_SETTINGS=1
  ui_wait_role deck || { fail "Settings did not show on the Deck"; return 1; }
  tab_keys_for "$DECK_DISPLAY"
  press_times "$TAB_PREV" "$((${#SETTINGS_TABS[@]} - 1))" &&
    press_times "$TAB_NEXT" "$(index_of Look "${SETTINGS_TABS[@]}")" || return 1
  ui_dump
  [ "$(ui_selected "${SETTINGS_TABS[@]}")" = Look ] || { fail "could not step the Deck to Look"; return 1; }
}

# Presses Down until the fonts row has focus. Sets BEFORE_FOCUS and FONTS_FOCUS.
find_fonts_row() {
  local i last=""
  for ((i = 0; i < MAX_ROWS; i++)); do
    key "$KEY_DOWN" || return 1
    ui_wait_role deck || continue
    if [ "$(focused_label)" = "$FONTS_LABEL" ]; then
      BEFORE_FOCUS="$last"
      FONTS_FOCUS="$(ui_focused)"
      return 0
    fi
    last="$(focused_label)"
  done
  return 1
}

# A only ever goes to the fonts row, after a fresh dump shows it focused.
press_a_on_fonts() {
  ui_wait_role deck || return 1
  [ "$(focused_label)" = "$FONTS_LABEL" ] || return 1
  jarn_is_focused || { ABORTED=1; return 1; }
  sh_dev input -d 0 keyevent "$KEY_A" >/dev/null
  KEYS_SENT=1
  pause 1.5
}

leave_settings_quiet() {
  [ "$IN_SETTINGS" = 1 ] || return 0
  key "$KEY_B" || return 1
  IN_SETTINGS=0
  wait_top_display "$STAGE_DISPLAY"
}

# Opens Settings > Look on a running Stage and reads the fonts switch into UI_FONTS ("true",
# "false", or "" when the row is missing or unreadable), leaving Settings open on the row.
read_fonts_ui() {
  UI_FONTS=""
  open_look_settings || return 1
  find_fonts_row || return 1
  ui_wait_role deck || return 1
  UI_FONTS="$(ui_checked)"
  [ -n "$UI_FONTS" ]
}

# Turns the fonts switch to $1 (true or false) with the controller and leaves Settings.
# UI_FONTS_BEFORE keeps what the switch read before, "" when it couldn't be read.
set_fonts_ui() {
  local want="$1" ok=1
  UI_FONTS_BEFORE=""
  if read_fonts_ui; then
    UI_FONTS_BEFORE="$UI_FONTS"
    if [ "$UI_FONTS" != "$want" ]; then
      if text_has "$FONTS_FOCUS" "$UNAVAILABLE_PREFIX"; then ok=0
      elif press_a_on_fonts && ui_wait_role deck; then UI_FONTS="$(ui_checked)"
      else ok=0; fi
    fi
  else
    ok=0
  fi
  leave_settings_quiet || ok=0
  [ "$ok" = 1 ] && [ "$UI_FONTS" = "$want" ]
}

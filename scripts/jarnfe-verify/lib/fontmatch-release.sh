#!/usr/bin/env bash
# --release steps of verify-452-cp-font-match.sh. Neither app is debuggable, so nothing here uses
# run-as or writes prefs: the fonts toggle goes through Settings with the controller, and Command
# Panel's look is only read (/fonts answers any caller; only the font files are gated).

preflight_release() {
  read_roles || true
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  if cp_installed && text_matches "$(cp_query_fonts)" '^Row: '; then
    CP_OK=1
    CP_WAS_RUNNING="$(pidof_pkg "$CP_PKG")"
    echo "  info  $CP_PKG answers /fonts"
  else
    echo "  info  $CP_PKG missing or /fonts unanswered, Command Panel steps will SKIP"
  fi
  detect_reader
  echo "  info  release mode: no run-as, no prefs writes, no pm disable; the toggle is flipped in Settings"
  echo "  info  screen roles can't be read here, so Stage is taken to be on display $STAGE_DISPLAY"
  preflight_footer
}

toggle_on_release() {
  start_stage || { fail "could not bring Stage to the front"; return 1; }
  local ok=0
  set_fonts_ui true && ok=1
  ORIG_FONTS_UI="$UI_FONTS_BEFORE"
  [ "$ORIG_FONTS_UI" = true ] || [ "$UI_FONTS" != true ] || UI_CHANGED=1
  if [ "$ok" = 1 ]; then
    pass "fonts switch on through Settings (it was ${ORIG_FONTS_UI:-unreadable})"
    return 0
  fi
  fail "could not turn the fonts switch on (read ${UI_FONTS:-nothing}, row: ${FONTS_FOCUS:-not found}; tour still showing?)"
  return 1
}

# Asks for a look with an imported font when one isn't applied and a person is at the terminal.
ask_for_imported_look() {
  local answer
  [ -t 0 ] && [ -t 1 ] || return 0
  echo "  ask   In Command Panel, apply a saved look that uses an imported font, then press Enter (s skips)"
  read -r answer || return 0
  [ "$answer" != s ]
}

# Stage with the toggle off (Jarn's own fonts) against Stage with it on, under an imported-font look.
imported_release() {
  local row
  row="$(cp_query_fonts)"
  if ! cp_row_has_user_font "$row"; then
    ask_for_imported_look || { skip "imported font: skipped at the prompt"; return; }
    row="$(cp_query_fonts)"
  fi
  printf 'imported %s\n' "$row" >>"$OUT_DIR/452-fonts-rows.txt"
  cp_row_has_user_font "$row" || {
    skip "imported font: look $(cp_col "$row" look_id) uses no imported font, apply one in Command Panel first"
    return
  }
  pass "/fonts reports an imported font (label $(cp_col "$row" label_font), display $(cp_col "$row" display_font))"
  skip "cp_fonts cache and sha256: no run-as on a release build"
  [ "$READER" = 1 ] || { skip "imported font on screen: reader (#449) not installed"; return; }
  python3 -I -c 'import PIL' 2>/dev/null || { skip "imported font on screen: Pillow is not installed"; return; }
  start_stage
  set_fonts_ui false || { fail "could not turn the fonts switch off for the baseline"; return; }
  resume_jarn
  capture_both default
  set_fonts_ui true || { fail "could not turn the fonts switch back on"; return; }
  resume_jarn
  capture_both imported
  compare_stage default imported
}

# Turns the switch back off with the controller when it started off.
restore_jarn_release() {
  [ "$JARN_CHANGED" = 1 ] && [ "$ORIG_FONTS_UI" = false ] || {
    [ "$JARN_CHANGED" = 1 ] && [ -z "$ORIG_FONTS_UI" ] && echo "  WARN  the fonts switch's starting state was unreadable, so it was left as is"
    return 0
  }
  [ "$ABORTED" = 0 ] || { echo "  WARN  input was stopped, turn Match Command Panel fonts off by hand"; return 0; }
  leave_settings_quiet
  jarn_is_focused || start_stage
  if set_fonts_ui false; then echo "  info  Match Command Panel fonts turned back off through Settings"
  else echo "  WARN  could not turn Match Command Panel fonts back off, do it in Settings > Look"; fi
}

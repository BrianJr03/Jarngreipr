#!/usr/bin/env bash
# #436: coming back to the RSS Canvas page shows the list right away, at the same article.
#
#   scripts/jarnfe-verify/verify-436-rss-return.sh [options]
#
#   --apk PATH              adb install -r this APK first (never uninstalls).
#   --launch                Open Jarn FE (JarnStageActivity) first if it is not already on screen.
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --steps N               D-pad Down presses into the article list before leaving (default 4).
#   --rounds N              How many leave and return trips to make (default 3).
#   --allow-app-focus-sync  Move the controller to the Deck even if the app's Focus Lock mode
#                           would rewrite screen_focus_lock. The script still never writes it.
#
# Setup: the Deck must be on the Canvas tab showing an RSS page with at least one feed loaded.
# The script focuses an article a few rows down, then steps the Deck tabs to Apps and back with
# its shoulder pair. Right on return it screenshots the Deck and reads it with uiautomator: the
# "Loading feeds" text must be gone and the same article must be focused (or, when focus sits
# elsewhere, still on screen). A debug build is needed to read the Deck tab.
#
# Safety: never uninstalls, clears data, writes settings or presses A. Keys are D-pad, the Deck's
# shoulder pair and X only, and input stops as soon as Jarn FE loses focus.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

LOADING_TEXT="Loading feeds"
RAIL_ALL="All"
STEPS=4 ROUNDS=3 TARGET="" FOCUSED_NOW=""

usage_436() { sed -n '2,23p' "$VERIFY_DIR/verify-436-rss-return.sh" | sed 's/^# \{0,1\}//'; }

parse_436_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --apk) APK="${2:?--apk needs a path}"; shift ;;
      --launch) LAUNCH=1 ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --steps) STEPS="${2:?--steps needs a number}"; shift ;;
      --rounds) ROUNDS="${2:?--rounds needs a number}"; shift ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_436; exit 0 ;;
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

deck_tab() { read_prefs jarnfe_session_prefs | parse_pref deck_tab; }

wait_deck_tab() {
  local want="$1" attempt
  for attempt in 1 2 3 4 5 6; do
    [ "$(deck_tab)" = "$want" ] && return 0
    pause 0.5
  done
  return 1
}

on_rss_page() { ui_wait_role deck && ui_has "$RAIL_ALL"; }

# Polls briefly, since the page restores focus a frame after it shows.
wait_focused() {
  local want="$1" attempt
  for attempt in 1 2 3 4; do
    ui_dump || return 1
    FOCUSED_NOW="$(ui_focused)"
    [ "$FOCUSED_NOW" = "$want" ] && return 0
    pause 0.4
  done
  return 1
}

check_setup() {
  section "Setup"
  local tab
  tab="$(deck_tab)"
  [ -n "$tab" ] || { skip "Deck tab unreadable (needs the debug build)"; return 1; }
  [ "$tab" = Canvas ] || { skip "Deck is on $tab, open the RSS Canvas page first"; return 1; }
  if [ "$(top_display)" != "$DECK_DISPLAY" ]; then
    need_handoff "moving the controller to the Deck" || return 1
    activate_display "$DECK_DISPLAY" || { fail "could not move the controller to the Deck"; return 1; }
  fi
  on_rss_page || { skip "the Deck Canvas page is not an RSS page with feeds"; return 1; }
  pass "Deck shows the RSS page"
}

focus_article() {
  key "$KEY_RIGHT" || return 1
  press_times "$KEY_DOWN" "$STEPS" || return 1
  pause 0.6
  ui_dump || return 1
  TARGET="$(ui_focused)"
  [ -n "$TARGET" ] || { skip "no focused article to remember"; return 1; }
  shot "$DECK_PHYSICAL" "$OUT_DIR/436-before.png"
  echo "  info  focused before leaving: $TARGET"
}

leave_and_return() {
  local round="$1"
  tab_keys_for "$DECK_DISPLAY"
  key "$TAB_PREV" || return 1
  wait_deck_tab Apps || { fail "round $round: $PREV_NAME did not leave Canvas (Deck on $(deck_tab))"; return 1; }
  SETTLE=0 key "$TAB_NEXT" || return 1
  shot "$DECK_PHYSICAL" "$OUT_DIR/436-return-$round.png"
  ui_dump || { skip "round $round: Deck unreadable on return"; return 0; }
  cp "$UI_XML" "$OUT_DIR/436-return-$round.xml"
  if ui_has "$LOADING_TEXT"; then fail "round $round: \"$LOADING_TEXT\" showed on return"
  else pass "round $round: no loading state on return"; fi
  if wait_focused "$TARGET"; then
    pass "round $round: the same article is focused"
  elif ui_has "$(printf '%s' "$TARGET" | cut -d'|' -f1 | sed 's/ $//')"; then
    pass "round $round: the article is still on screen (focus is on ${FOCUSED_NOW:-nothing})"
  else
    fail "round $round: the list moved, \"$TARGET\" is gone (dump in $OUT_DIR)"
  fi
}

run_436() {
  check_setup || return 0
  START_DECK_TAB=Canvas
  focus_article || return 0
  local round
  for ((round = 1; round <= ROUNDS; round++)); do
    [ "$ABORTED" = 0 ] || return 0
    section "Leave and return, round $round"
    leave_and_return "$round" || return 0
  done
}

main_436() {
  parse_436_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-436)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_apk
  trap restore_start EXIT
  trap 'restore_start; exit 130' INT TERM
  preflight || { RESTORED=1; echo "Preflight failed, nothing was pressed." >&2; exit 2; }
  run_436
  restore_start
  check_no_crashes
  check_lock_unchanged
  summary
}

main_436 "$@"

#!/usr/bin/env bash
# Reporting, read-only device access, and the one guarded path for sending keys.

KEY_UP=19 KEY_DOWN=20 KEY_LEFT=21 KEY_RIGHT=22
KEY_B=97 KEY_L1=102 KEY_R1=103 KEY_L2=104 KEY_R2=105 KEY_L3=106 KEY_START=108 KEY_SELECT=109
# Keys a script may add to the allow list on top of the navigation keys, such as L3 for Swap.
EXTRA_KEYS=""

PASS_COUNT=0 FAIL_COUNT=0 SKIP_COUNT=0
ABORTED=0 KEYS_SENT=0
SETTLE="${JARNFE_SETTLE:-0.9}"
REMOTE_UI=/data/local/tmp/jarnfe-verify-ui.xml

pass() { printf '  PASS  %s\n' "$*"; PASS_COUNT=$((PASS_COUNT + 1)); }
fail() { printf '  FAIL  %s\n' "$*"; FAIL_COUNT=$((FAIL_COUNT + 1)); }
skip() { printf '  SKIP  %s\n' "$*"; SKIP_COUNT=$((SKIP_COUNT + 1)); }
section() { printf '\n== %s\n' "$*"; }
pause() { [ "$SETTLE" = 0 ] || sleep "${1:-$SETTLE}"; }

sh_dev() { adb shell "$@" 2>/dev/null | tr -d '\r'; }

# Never pipe into grep -q here: it exits at the first match, and under pipefail the writer's
# SIGPIPE turns that match into a failure. Here-strings have no pipe and work on bash 3.2.
text_has() { grep -qF -- "$2" <<<"$1"; }
text_matches() { grep -qE -- "$2" <<<"$1"; }
text_has_line() { grep -qxF -- "$2" <<<"$1"; }

top_display() { sh_dev dumpsys window | parse_top_display; }
focused_app() { sh_dev dumpsys window displays | parse_focused_app "$1"; }
focus_lock_value() { sh_dev settings get system screen_focus_lock; }

# Prints a prefs file. Returns 1 only when run-as itself is unavailable; a missing file prints nothing.
read_prefs() {
  local listing
  listing="$(adb shell run-as "$PKG" ls shared_prefs 2>/dev/null)" || return 1
  listing="$(printf '%s' "$listing" | tr -d '\r')"
  case "$listing" in *run-as:*) return 1 ;; esac
  text_has_line "$listing" "$1.xml" || return 0
  sh_dev run-as "$PKG" cat "shared_prefs/$1.xml"
}

key_allowed() {
  case "$1" in
    "$KEY_UP"|"$KEY_DOWN"|"$KEY_LEFT"|"$KEY_RIGHT"|"$KEY_B"|"$KEY_L1"|"$KEY_R1"|"$KEY_L2"|"$KEY_R2") return 0 ;;
    "$KEY_START"|"$KEY_SELECT") return 0 ;;
  esac
  case " $EXTRA_KEYS " in *" $1 "*) return 0 ;; esac
  return 1
}

# Only navigation keys are allowed, and only while Jarn FE holds top focus.
key() {
  local code="$1" display app
  key_allowed "$code" || { fail "refused key $code (not on the allow list)"; ABORTED=1; return 1; }
  [ "$ABORTED" = 0 ] || return 1
  display="$(top_display)"
  app="$(focused_app "${display:-0}")"
  case "$app" in
    "$PKG"/*) ;;
    *)
      fail "Jarn FE lost focus (display ${display:-?}: ${app:-none}), stopping all input"
      ABORTED=1
      return 1
      ;;
  esac
  sh_dev input -d 0 keyevent "$code" >/dev/null
  KEYS_SENT=1
  pause
}

press_times() {
  local code="$1" count="$2" i
  for ((i = 0; i < count; i++)); do key "$code" || return 1; done
}

ui_dump() {
  local attempt
  for attempt in 1 2 3; do
    adb exec-out "uiautomator dump $REMOTE_UI >/dev/null 2>&1; cat $REMOTE_UI 2>/dev/null; rm -f $REMOTE_UI" \
      >"$UI_XML" 2>/dev/null
    if grep -q '<hierarchy' "$UI_XML" && python3 -I "$LIB_DIR/uiquery.py" "$UI_XML" role >/dev/null; then
      return 0
    fi
    pause 0.5
  done
  return 1
}

ui_role() { ui_dump && python3 -I "$LIB_DIR/uiquery.py" "$UI_XML" role; }

# uiautomator reads one window, and on the Thor it can still be the previously active screen
# right after a switch. Polls until the dump shows the wanted screen ("stage" or "deck").
ui_wait_role() {
  local attempt
  for attempt in 1 2 3 4 5 6; do
    [ "$(ui_role)" = "$1" ] && return 0
    pause 0.7
  done
  return 1
}

ui_has() { python3 -I "$LIB_DIR/uiquery.py" "$UI_XML" has "$1"; }
ui_selected() { python3 -I "$LIB_DIR/uiquery.py" "$UI_XML" selected "$@"; }
ui_focused() { python3 -I "$LIB_DIR/uiquery.py" "$UI_XML" focused; }

shot() {
  local physical="$1" file="$2"
  adb exec-out screencap -p -d "$physical" >"$file" 2>/dev/null
  [ "$(head -c 4 "$file" 2>/dev/null | tail -c 3)" = "PNG" ]
}

# Three idle frames of display $1 into prefix $2, to learn what moves on its own.
shot_idle() {
  local physical="$1" prefix="$2" i
  for i in 1 2 3; do
    shot "$physical" "$prefix-idle$i.png" || return 1
    pause 0.4
  done
}

frame_changes() {
  local prefix="$1"
  python3 -I "$LIB_DIR/framediff.py" \
    "$prefix-idle1.png" "$prefix-idle2.png" "$prefix-idle3.png" "$prefix-after.png"
}

diff_field() { printf '%s\n' "$1" | tr ' ' '\n' | grep "^$2=" | cut -d= -f2; }

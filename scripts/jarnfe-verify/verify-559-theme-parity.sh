#!/usr/bin/env bash
# #559: browse looks the same after the theme engine rebuild. Masked screenshots against a baseline.
#
#   scripts/jarnfe-verify/verify-559-theme-parity.sh --baseline|--compare [options]
#
#   --baseline              Capture today's browse into the baseline folder (overwrites it).
#   --compare               Capture again and diff every frame against the baseline.
#   --dir DIR               Baseline folder (default: build/jarnfe-theme-parity in the repo).
#   --install APK           adb install -r this APK first (never uninstalls).
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when Jarn FE restarts. The script never writes it.
#
# Debug builds only (run-as). Typical use: --baseline on a build from before the rebuild, install
# the rebuilt build, then --compare. With Jarn FE stopped, reduced motion is turned on and video
# previews off (both put back on exit). Jarn FE restarts on Stage, the Library steps to Systems
# and the Deck to Now, then three idle frames of each screen are taken for systems browse. A opens
# the focused system, only after a fresh dump shows a system from the library database focused,
# and the same frames are taken for its game list. B goes back.
#
# Two screens give top and bottom frames. With no bottom screen attached the single screen is
# captured, into its own baseline. Grid or Carousel is whatever Settings has for systems and games;
# the baseline remembers it and --compare refuses a different choice, a different library or a
# different focused item, since any of those would change the frames for a reason other than the
# rebuild. Compare runs framediff.py both ways (each run's idle frames mask its own motion), and a
# frame passes when no more than CONTENT_CHANGE_CELLS cells differ.
#
# Safety: never uninstalls, clears data or launches a game. A is pressed on a system only.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"
# shellcheck source=lib/prefs.sh
source "$LIB_DIR/prefs.sh"

KEY_A=96
LOOK_PREFS=jarnfe_look_prefs
REDUCED_KEY=reduced_motion
VIDEO_KEY=video_previews
DB_NAME=jarngreipr_library
MAX_SEEK=40
MODE="" BASE_DIR="" INSTALL_APK="" LAYOUT="" PREF_CHANGED=0 OPENED_SYSTEM=0
ORIG_REDUCED="" ORIG_VIDEO="" FOCUSED_NOW=""

usage_559() { sed -n '2,29p' "$VERIFY_DIR/verify-559-theme-parity.sh" | sed 's/^# \{0,1\}//'; }

parse_559_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --baseline) MODE=baseline ;;
      --compare) MODE=compare ;;
      --dir) BASE_DIR="${2:?--dir needs a path}"; shift ;;
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_559; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
  [ -n "$MODE" ] || { echo "pass --baseline or --compare" >&2; exit 2; }
  [ -n "$BASE_DIR" ] || BASE_DIR="$REPO_ROOT/build/jarnfe-theme-parity"
}

install_if_asked() {
  [ -n "$INSTALL_APK" ] || return 0
  [ -f "$INSTALL_APK" ] || { echo "no such APK: $INSTALL_APK" >&2; exit 2; }
  echo "Installing $INSTALL_APK"
  adb install -r "$INSTALL_APK" || { echo "adb install failed (signature mismatch? not uninstalling)" >&2; exit 2; }
}

has_pillow() { python3 -I -c 'import PIL' 2>/dev/null; }

detect_layout() {
  if text_has "$(sh_dev dumpsys display)" "local:$BOTTOM_PHYSICAL"; then LAYOUT=dual; else LAYOUT=single; fi
}

# Screens to capture as name:physical. A single screen is captured without a display id.
screens() {
  if [ "$LAYOUT" = dual ]; then echo "top:$TOP_PHYSICAL bottom:$BOTTOM_PHYSICAL"; else echo "single:"; fi
}

shot_screen() {
  local physical="$1" file="$2"
  if [ -n "$physical" ]; then shot "$physical" "$file"; return; fi
  adb exec-out screencap -p >"$file" 2>/dev/null
  [ "$(head -c 4 "$file" 2>/dev/null | tail -c 3)" = "PNG" ]
}

# Three idle frames of every screen into $1 as <page>-<screen>-idleN.png.
capture_page() {
  local dir="$1" page="$2" entry name physical i
  pause 2
  for entry in $(screens); do
    name="${entry%%:*}" physical="${entry#*:}"
    for i in 1 2 3; do
      shot_screen "$physical" "$dir/$page-$name-idle$i.png" || { fail "could not capture $page on $name"; return 1; }
      pause 0.4
    done
  done
}

# Systems and games, one line each, from a host copy of the library database.
library_fingerprint() {
  local dir="$OUT_DIR/db" part
  mkdir -p "$dir"
  for part in "" -wal -shm; do
    adb exec-out run-as "$PKG" cat "databases/$DB_NAME$part" >"$dir/$DB_NAME$part" 2>/dev/null || rm -f "$dir/$DB_NAME$part"
  done
  python3 -I -c '
import hashlib, sqlite3, sys
db = sqlite3.connect(sys.argv[1])
systems = sorted(db.execute("select id, displayName from systems"))
games = sorted(db.execute("select systemId, displayName from games"))
digest = hashlib.sha256(repr((systems, games)).encode()).hexdigest()[:16]
print(f"library={len(systems)} systems, {len(games)} games, {digest}")
' "$dir/$DB_NAME" 2>/dev/null
}

system_titles() {
  python3 -I -c '
import sqlite3, sys
for (name,) in sqlite3.connect(sys.argv[1]).execute("select displayName from systems"):
    print(name)
' "$OUT_DIR/db/$DB_NAME" 2>/dev/null
}

# True when the focused label starts with a system title, so A opens a list and never launches.
focus_is_system() {
  local title
  while IFS= read -r title; do
    [ -n "$title" ] || continue
    case "$FOCUSED_NOW" in "$title"|"$title |"*) return 0 ;; esac
  done <<<"$(system_titles)"
  return 1
}

read_focus() { ui_wait_role stage && FOCUSED_NOW="$(ui_focused)"; }

# Steps right until the focused label is $1. Grid and carousel both reach every item this way.
seek_focus() {
  local want="$1" i
  read_focus || return 1
  for ((i = 0; i < MAX_SEEK; i++)); do
    [ "$FOCUSED_NOW" = "$want" ] && return 0
    key "$KEY_RIGHT" || return 1
    read_focus || return 1
  done
  [ "$FOCUSED_NOW" = "$want" ]
}

meta_value() { grep "^$2=" "$1" 2>/dev/null | head -n 1 | cut -d= -f2-; }

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
}

# Turns motion and video off for steady frames. write_bool stops the app first.
quiet_motion() {
  ORIG_REDUCED="$(look_pref "$REDUCED_KEY")" ORIG_VIDEO="$(look_pref "$VIDEO_KEY")"
  PREF_CHANGED=1
  write_bool "$REDUCED_KEY" true && write_bool "$VIDEO_KEY" false
}

restore_559() {
  if [ "$OPENED_SYSTEM" = 1 ] && [ "$ABORTED" = 0 ]; then
    OPENED_SYSTEM=0
    key "$KEY_B" || true
  fi
  [ "$PREF_CHANGED" = 1 ] || return 0
  PREF_CHANGED=0
  section "Restore"
  write_bool "$REDUCED_KEY" "$ORIG_REDUCED" && write_bool "$VIDEO_KEY" "$ORIG_VIDEO" \
    && echo "  info  reduced motion and video previews restored" \
    || echo "  WARN  could not restore: reduced_motion was '${ORIG_REDUCED:-unset}', video_previews '${ORIG_VIDEO:-unset}'"
  sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
}

preflight_559() {
  section "Preflight"
  [ "$PKG" != "$RELEASE_PKG" ] || { echo "debug builds only, $PKG is the release package" >&2; return 1; }
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  has_pillow || { echo "python3 with Pillow is needed" >&2; return 1; }
  read_prefs "$LOOK_PREFS" >/dev/null || { echo "run-as is unavailable for $PKG, debug builds only" >&2; return 1; }
  detect_layout
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  if [ "$MODE" = compare ] && [ ! -f "$BASE_DIR/$LAYOUT/meta.txt" ]; then
    echo "no $LAYOUT baseline in $BASE_DIR, run --baseline first" >&2
    return 1
  fi
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  $PKG, $LAYOUT screen, Stage on display $STAGE_DISPLAY"
  echo "  info  baseline $BASE_DIR/$LAYOUT, output in $OUT_DIR"
}

# Writes what the frames depend on. In compare mode each line must match the baseline.
write_meta() {
  local meta="$1"
  {
    echo "systems_view=$(look_pref system_view)"
    echo "games_view=$(look_pref game_view)"
    library_fingerprint
  } >"$meta"
}

same_setup() {
  local base="$BASE_DIR/$LAYOUT/meta.txt" now="$1" key ok=0
  for key in systems_view games_view library; do
    if [ "$(meta_value "$base" "$key")" = "$(meta_value "$now" "$key")" ]; then continue; fi
    fail "$key differs: baseline '$(meta_value "$base" "$key")', now '$(meta_value "$now" "$key")'"
    ok=1
  done
  return "$ok"
}

# Brings focus to the baseline's item in compare mode, or records it in baseline mode.
settle_focus() {
  local dir="$1" page="$2" want
  if [ "$MODE" = baseline ]; then
    read_focus || return 1
    echo "${page}_focus=$FOCUSED_NOW" >>"$dir/meta.txt"
    return 0
  fi
  want="$(meta_value "$BASE_DIR/$LAYOUT/meta.txt" "${page}_focus")"
  seek_focus "$want" || { fail "$page: could not focus '$want' (now '$FOCUSED_NOW')"; return 1; }
}

capture_all() {
  local dir="$1"
  section "Systems browse"
  start_stage || { fail "could not bring Stage to the front"; return 1; }
  ui_wait_role stage || { fail "Stage did not show"; return 1; }
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY" || return 1
  if [ "$LAYOUT" = dual ]; then step_to_tab 0 "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return 1; fi
  activate_display "$STAGE_DISPLAY" || return 1
  settle_focus "$dir" systems || return 1
  capture_page "$dir" systems || return 1
  pass "systems captured on focus '$FOCUSED_NOW'"
  section "Games browse"
  focus_is_system || { fail "focus is not on a system ('$FOCUSED_NOW'), A is not pressed"; return 1; }
  EXTRA_KEYS="$KEY_A"
  key "$KEY_A" || return 1
  EXTRA_KEYS=""
  OPENED_SYSTEM=1
  pause 2
  settle_focus "$dir" games || return 1
  capture_page "$dir" games || return 1
  pass "games captured on focus '$FOCUSED_NOW'"
}

# Diffs one frame both ways: each side's idle frames mask what moves on its own there.
compare_frame() {
  local base="$1" now="$2" label="$3" forward backward a b
  forward="$(python3 -I "$LIB_DIR/framediff.py" "$base-idle1.png" "$base-idle2.png" "$base-idle3.png" "$now-idle3.png")"
  backward="$(python3 -I "$LIB_DIR/framediff.py" "$now-idle1.png" "$now-idle2.png" "$now-idle3.png" "$base-idle3.png")"
  a="$(diff_field "$forward" changed)" b="$(diff_field "$backward" changed)"
  if [ -z "$a" ] || [ -z "$b" ]; then fail "$label: frames could not be compared (size changed?)"; return; fi
  if [ "$(diff_field "$forward" usable)" -lt "$MIN_USABLE_CELLS" ]; then
    skip "$label: too much moves on its own to compare ($forward)"
  elif [ "$a" -le "$CONTENT_CHANGE_CELLS" ] && [ "$b" -le "$CONTENT_CHANGE_CELLS" ]; then
    pass "$label matches the baseline ($a and $b cells differ)"
  else
    fail "$label differs from the baseline ($a and $b cells), see $now-idle3.png"
  fi
}

compare_all() {
  local dir="$1" page entry name
  section "Compare with $BASE_DIR/$LAYOUT"
  for page in systems games; do
    for entry in $(screens); do
      name="${entry%%:*}"
      compare_frame "$BASE_DIR/$LAYOUT/$page-$name" "$dir/$page-$name" "$page on $name"
    done
  done
}

run_559() {
  local dir
  if [ "$MODE" = baseline ]; then dir="$BASE_DIR/$LAYOUT"; rm -rf "$dir"; else dir="$OUT_DIR/current"; fi
  mkdir -p "$dir"
  quiet_motion || { fail "could not turn reduced motion on and video previews off"; return 0; }
  write_meta "$dir/meta.txt"
  if [ "$MODE" = compare ]; then same_setup "$dir/meta.txt" || return 0; fi
  START_PID=""
  capture_all "$dir" || return 0
  START_PID="$(sh_dev pidof "$PKG")"
  if [ "$MODE" = compare ]; then compare_all "$dir"; else echo "  info  baseline saved in $dir"; fi
}

main_559() {
  parse_559_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-559)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_if_asked
  trap restore_559 EXIT
  trap 'restore_559; exit 130' INT TERM
  preflight_559 || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_559
  check_no_crashes
  check_lock_unchanged
  restore_559
  section "For a human"
  echo "  look  $OUT_DIR/current: same tiles, spacing and focus ring as the baseline"
  echo "  look  flip Grid and Carousel in Settings > Display and run both modes again for each"
  summary
}

main_559 "$@"

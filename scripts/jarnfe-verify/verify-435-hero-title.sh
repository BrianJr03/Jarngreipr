#!/usr/bin/env bash
# #435: the Continue playing title on Stage runs up to the cover art instead of stopping mid card.
#
#   scripts/jarnfe-verify/verify-435-hero-title.sh [options]
#
#   --install APK           adb install -r this APK first (never uninstalls).
#   --launch                Open Jarn FE (JarnStageActivity) first if it is not already on screen.
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Move the controller to Stage even if the app's Focus Lock mode would
#                           rewrite screen_focus_lock. The script still never writes it.
#
# Needs a played game, so the card exists. Steps Stage to the Systems tab, presses Up until the
# Continue playing card has focus, takes a screenshot of the Stage display and measures how far the
# caption's ink runs across the card. Before the fix it stopped by 55% of the card minus the 24dp
# margin. Compose reports the full title to accessibility even when it is drawn with an ellipsis,
# so the ellipsis itself is checked by eye on the saved screenshot.
#
# Safety: never uninstalls, clears data, writes settings or presses A. Keys are D-pad, L1/R1 and X,
# and input stops as soon as Jarn FE loses focus. The Library tab and the active screen are restored.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

EYEBROW="Continue playing"
HERO_HEIGHT_DP=160
OLD_TEXT_FRACTION=0.55
STAGE_MARGIN_DP=24
MAX_UP_PRESSES=6
INSTALL_APK=""

usage_435() { sed -n '2,21p' "$VERIFY_DIR/verify-435-hero-title.sh" | sed 's/^# \{0,1\}//'; }

parse_435_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --launch) LAUNCH=1 ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_435; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

install_if_asked() {
  [ -n "$INSTALL_APK" ] || return 0
  [ -f "$INSTALL_APK" ] || { echo "no such APK: $INSTALL_APK" >&2; exit 2; }
  echo "Installing $INSTALL_APK"
  adb install -r "$INSTALL_APK" || { echo "adb install failed (signature mismatch? not uninstalling)" >&2; exit 2; }
}

hero_card() { python3 -I "$LIB_DIR/heroquery.py" card "$UI_XML" "$EYEBROW"; }

ui_stage_dump() { ui_wait_role stage && ui_dump; }

go_to_systems_tab() {
  local now
  now="$(ui_selected "${LIBRARY_TABS[@]}")"
  [ "$now" = "Systems" ] && return 0
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY"
}

focus_hero() {
  local i
  for ((i = 0; i < MAX_UP_PRESSES; i++)); do
    ui_stage_dump || return 1
    case "$(hero_card)" in *" focused") return 0 ;; esac
    key "$KEY_UP" || return 1
  done
  ui_stage_dump && case "$(hero_card)" in *" focused") return 0 ;; esac
  return 1
}

# Prints "end width gap density" for the caption ink, or nothing.
measure_caption() {
  local shot_file="$1" left="$2" top="$3" right="$4" bottom="$5" out
  out="$(python3 -I "$LIB_DIR/heroquery.py" ink "$shot_file" "$left" "$top" "$right" "$bottom" "$HERO_HEIGHT_DP")" || return 1
  printf '%s\n' "$out" | tr ' ' '\n' | cut -d= -f2 | tr '\n' ' '
}

check_caption_width() {
  local shot_file="$1" left="$2" top="$3" right="$4" bottom="$5" end width gap density old_end card_end
  read -r end width gap density <<<"$(measure_caption "$@")"
  [ -n "${end:-}" ] || { skip "no caption ink found on the card (see $shot_file)"; return 0; }
  old_end="$(python3 -I -c "import sys; w,d=float(sys.argv[1]),float(sys.argv[2]); print(int(w*$OLD_TEXT_FRACTION - $STAGE_MARGIN_DP*d))" "$width" "$density")"
  card_end="$(python3 -I -c "import sys; w,d=float(sys.argv[1]),float(sys.argv[2]); print(int(w - $STAGE_MARGIN_DP*d))" "$width" "$density")"
  echo "  info  card ${width}px wide, caption ink ends at ${end}px, old cap at ${old_end}px, gap to art ${gap}px"
  if [ "$end" -gt "$card_end" ]; then
    fail "caption runs into the card's end margin (${end}px > ${card_end}px)"
  elif [ "$end" -gt "$old_end" ]; then
    pass "caption runs past the old 55% cap ($((end * 100 / width))% of the card)"
  else
    skip "caption ends inside the old cap ($((end * 100 / width))%): the title is short, try a game with a long title"
  fi
}

run_435() {
  section "Continue playing title"
  if [ "$(top_display)" != "$STAGE_DISPLAY" ]; then
    need_handoff "moving the controller to Stage" || return 0
    activate_display "$STAGE_DISPLAY" || { fail "could not move the controller to Stage"; return 0; }
  fi
  ui_stage_dump || { skip "Stage UI could not be read"; return 0; }
  remember_library_tab || true
  go_to_systems_tab || return 0
  ui_stage_dump || { skip "Stage UI could not be read"; return 0; }
  hero_card >/dev/null || { skip "no Continue playing card (play a game first)"; return 0; }
  focus_hero || { fail "Up never focused the Continue playing card"; return 0; }
  pass "Continue playing card has focus"
  local left top right bottom state shot_file="$OUT_DIR/435-stage-hero.png"
  read -r left top right bottom state <<<"$(hero_card)"
  pause 0.6
  shot "$STAGE_PHYSICAL" "$shot_file" || { skip "could not capture the Stage display"; return 0; }
  echo "  info  screenshot: $shot_file (check the title has no ellipsis, or only near the art)"
  has_pillow || { skip "Pillow missing, caption not measured"; return 0; }
  check_caption_width "$shot_file" "$left" "$top" "$right" "$bottom"
}

has_pillow() { python3 -I -c 'import PIL' 2>/dev/null; }

main_435() {
  parse_435_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-435)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  select_device
  install_if_asked
  trap restore_start EXIT
  trap 'restore_start; exit 130' INT TERM
  preflight || { RESTORED=1; echo "Preflight failed, nothing was pressed." >&2; exit 2; }
  run_435
  restore_start
  check_no_crashes
  check_lock_unchanged
  summary
}

main_435 "$@"

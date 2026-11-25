#!/usr/bin/env bash
# #438: a system's picked "Now background" shows behind its Now page, ahead of the system art.
#
#   scripts/jarnfe-verify/verify-438-now-background.sh [options]
#
#   --install APK           adb install -r this APK first (never uninstalls).
#   --system ID             The system to give the test image. Default: the system Stage focuses
#                           first, looked up by its title in a copy of the library database.
#   --out DIR               Where screenshots and logs go (default: a new temp folder).
#   --allow-app-focus-sync  Run even if the app's Focus Lock mode could rewrite screen_focus_lock
#                           when Jarn FE restarts. The script never writes it.
#
# Debug builds only. A solid magenta test image is made on the host, pushed, and copied into the
# app's own files folder (no file picker, so no content URI permission is involved). With Jarn FE
# stopped, the system's focusBackgroundUri in Classic's esde_prefs is pointed at that file, the
# same field the Systems editor's "Now background" row writes. For each state it restarts Jarn FE
# on Stage, steps to the Systems tab and the Deck to Now, focuses the system and captures the Deck:
#   1. baseline (no pick): little of the art band is magenta,
#   2. picked: most of the art band is magenta, and it differs from the baseline,
#   3. removed again: the band matches the baseline (system art is back),
#   4. no crash or ANR, and screen_focus_lock is unchanged.
# Needs the "System background" switch (#437) on; the script only reads it.
#
# Safety: never uninstalls, clears data or presses A. On exit the original system_customizations
# value is written back, the test image deleted and Jarn FE restarted.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

HELPER="$LIB_DIR/backgroundquery.py"
ESDE_PREFS=esde_prefs
CUSTOM_KEY=system_customizations
LOOK_PREFS=jarnfe_look_prefs
BACKGROUND_KEY=now_system_background_visible
DB_NAME=jarngreipr_library
IMAGE_NAME=verify-438-now-background.png
# Share of the upper art band that must be the test color once picked, and at most before.
MIN_PICKED="${JARNFE_MIN_PICKED:-0.5}"
MAX_BASELINE="${JARNFE_MAX_BASELINE:-0.2}"
# Share of the band allowed to differ between the baseline and the removed frame.
MAX_RESTORED_CHANGE="${JARNFE_MAX_RESTORED_CHANGE:-0.25}"
INSTALL_APK="" SYSTEM_ID="" ORIG_JSON="" ORIG_PRESENT=0 PREF_CHANGED=0 IMAGE_PATH=""

usage_438() { sed -n '2,27p' "$VERIFY_DIR/verify-438-now-background.sh" | sed 's/^# \{0,1\}//'; }

parse_438_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --system) SYSTEM_ID="${2:?--system needs an id}"; shift ;;
      --allow-app-focus-sync) ALLOW_SYNC=1 ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_438; exit 0 ;;
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

has_pillow() { python3 -I -c 'import PIL' 2>/dev/null; }

# Prints the raw JSON stored under system_customizations, or nothing. Sets ORIG_PRESENT.
read_customizations() {
  read_prefs "$ESDE_PREFS" | python3 -I -c '
import sys
import xml.etree.ElementTree as ET
raw = sys.stdin.read()
root = ET.fromstring(raw) if raw.strip() else ET.Element("map")
for item in root:
    if item.tag == "string" and item.get("name") == sys.argv[1]:
        print(item.text or "")
        sys.exit(0)
sys.exit(3)
' "$CUSTOM_KEY"
}

# Writes $1 as the whole system_customizations JSON, or removes the key when $2 is "remove".
write_customizations() {
  local json="$1" mode="${2:-set}" script
  sh_dev am force-stop "$PKG" >/dev/null
  script="$(python3 -I -c '
import sys
import xml.etree.ElementTree as ET
raw, key, value, mode = sys.stdin.read(), sys.argv[1], sys.argv[2], sys.argv[3]
try:
    root = ET.fromstring(raw) if raw.strip() else ET.Element("map")
except ET.ParseError:
    root = ET.Element("map")
for item in list(root):
    if item.get("name") == key:
        root.remove(item)
if mode != "remove":
    ET.SubElement(root, "string", name=key).text = value
print("<?xml version=\x271.0\x27 encoding=\x27utf-8\x27 standalone=\x27yes\x27 ?>")
print(ET.tostring(root, encoding="unicode"))
' "$CUSTOM_KEY" "$json" "$mode" <<<"$(read_prefs "$ESDE_PREFS")")" || return 1
  printf '%s\n' "$script" | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/$ESDE_PREFS.xml'"
}

# Prints the customizations JSON with $1's focusBackgroundUri set to $2 (empty clears it).
with_pick() {
  python3 -I -c '
import json, sys
data = json.loads(sys.argv[1]) if sys.argv[1].strip() else {}
entry = data.get(sys.argv[2]) or {"backgroundUri": None, "showName": True, "solidColorArgb": None}
entry["focusBackgroundUri"] = sys.argv[3] or None
data[sys.argv[2]] = entry
print(json.dumps(data))
' "$ORIG_JSON" "$1" "$2"
}

make_test_image() {
  local local_png="$OUT_DIR/$IMAGE_NAME"
  python3 -I -c 'import sys; from PIL import Image; Image.new("RGB", (1920, 1080), (255, 0, 255)).save(sys.argv[1])' "$local_png" || return 1
  adb push "$local_png" "/data/local/tmp/$IMAGE_NAME" >/dev/null || return 1
  adb shell "run-as $PKG sh -c 'mkdir -p files && cp /data/local/tmp/$IMAGE_NAME files/$IMAGE_NAME'" || return 1
  IMAGE_PATH="$(sh_dev "run-as $PKG sh -c 'cd files && pwd'")/$IMAGE_NAME"
  text_matches "$(sh_dev run-as "$PKG" ls "files/$IMAGE_NAME")" "$IMAGE_NAME"
}

# Looks up the id of the system whose title is $1 in a host copy of the library database.
system_id_for_title() {
  local dir="$OUT_DIR/db" part
  mkdir -p "$dir"
  for part in "" -wal -shm; do
    adb exec-out run-as "$PKG" cat "databases/$DB_NAME$part" >"$dir/$DB_NAME$part" 2>/dev/null || rm -f "$dir/$DB_NAME$part"
  done
  python3 -I -c '
import sqlite3, sys
db = sqlite3.connect(sys.argv[1])
title = sys.argv[2].strip()
for system_id, name in db.execute("select id, displayName from systems"):
    if name and (name == title or name in title):
        print(system_id)
        break
' "$dir/$DB_NAME" "$1" 2>/dev/null
}

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 7
  wait_top_display "$STAGE_DISPLAY" || activate_display "$STAGE_DISPLAY"
}

focus_a_system() {
  ui_wait_role stage || return 1
  step_to_tab 0 "${#LIBRARY_TABS[@]}" "$STAGE_DISPLAY" || return 1
  step_to_tab 0 "${#DECK_TABS[@]}" "$DECK_DISPLAY" || return 1
  key "$KEY_RIGHT" && key "$KEY_LEFT" || return 1
  ui_dump && FOCUSED_LABEL="$(ui_focused)" && echo "  info  Stage focus: $FOCUSED_LABEL"
  pause 2
}

# Prints the share of the upper art band (above the bottom shade) that is the magenta test color.
picked_share() {
  python3 -I -c '
import sys
from PIL import Image, ImageStat
image = Image.open(sys.argv[1]).convert("RGB")
w, h = image.size
top, bottom, cols, rows = int(h * 0.12), int(h * 0.38), 32, 10
hits = total = 0
for r in range(rows):
    for c in range(cols):
        box = (c * w // cols, top + r * (bottom - top) // rows, (c + 1) * w // cols, top + (r + 1) * (bottom - top) // rows)
        red, green, blue = ImageStat.Stat(image.crop(box)).mean
        total += 1
        hits += red > green + 60 and blue > green + 60 and abs(red - blue) < 60
print(round(hits / total, 3))
' "$1"
}

# Restarts Jarn FE with $1 as the system_customizations JSON and captures the Deck as 438-$2.png.
capture_with() {
  local json="$1" name="$2"
  local mode=set
  [ -n "$json" ] || mode=remove
  write_customizations "$json" "$mode" || { fail "could not write the $name customizations"; return 1; }
  PREF_CHANGED=1
  start_stage || { fail "could not bring Stage to the front"; return 1; }
  focus_a_system || { fail "could not focus a system on Stage"; return 1; }
  shot "$DECK_PHYSICAL" "$OUT_DIR/438-$name.png" || { fail "could not capture the Deck display"; return 1; }
  echo "  info  $name: magenta share $(picked_share "$OUT_DIR/438-$name.png")"
}

at_least() { python3 -I -c 'import sys; sys.exit(0 if float(sys.argv[1]) >= float(sys.argv[2]) else 1)' "$1" "$2"; }

restore_438() {
  [ "$PREF_CHANGED" = 1 ] || [ -n "$IMAGE_PATH" ] || return 0
  section "Restore"
  if [ "$PREF_CHANGED" = 1 ]; then
    PREF_CHANGED=0
    if [ "$ORIG_PRESENT" = 1 ]; then write_customizations "$ORIG_JSON"; else write_customizations "" remove; fi \
      && echo "  info  system_customizations restored" \
      || echo "  WARN  could not restore system_customizations, saved copy in $OUT_DIR/438-original.json"
  fi
  if [ -n "$IMAGE_PATH" ]; then
    sh_dev run-as "$PKG" rm -f "files/$IMAGE_NAME" >/dev/null
    sh_dev rm -f "/data/local/tmp/$IMAGE_NAME" >/dev/null
    IMAGE_PATH=""
  fi
  sh_dev am start --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
}

preflight_438() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  has_pillow || { echo "python3 with Pillow is needed" >&2; return 1; }
  read_prefs "$ESDE_PREFS" >/dev/null || {
    echo "run-as is unavailable for $PKG. On a release build pick the image by hand: Systems, Select on a system, Appearance, Now background." >&2
    return 1
  }
  if [ "$(read_prefs "$LOOK_PREFS" | parse_pref "$BACKGROUND_KEY")" = "false" ]; then
    echo "System background (#437) is off. Turn it on in Settings > Display > Now page first." >&2
    return 1
  fi
  read_roles
  read_focus_lock_gate
  [ "$HANDOFF_OK" = 1 ] || { echo "Not running: $HANDOFF_REASON" >&2; return 1; }
  ORIG_JSON="$(read_customizations)" && ORIG_PRESENT=1
  printf '%s\n' "$ORIG_JSON" >"$OUT_DIR/438-original.json"
  LOG_START="$(sh_dev date +%s).000"
  echo "  info  $PKG, Stage on display $STAGE_DISPLAY, Deck on display $DECK_DISPLAY"
  echo "  info  output in $OUT_DIR"
}

run_438() {
  local baseline picked removed changed restored
  section "Now background"
  capture_with "$ORIG_JSON" baseline || return 0
  if [ -z "$SYSTEM_ID" ]; then
    SYSTEM_ID="$(system_id_for_title "$FOCUSED_LABEL")"
    [ -n "$SYSTEM_ID" ] || { fail "could not tell which system is focused, pass --system ID"; return 0; }
  fi
  echo "  info  test system: $SYSTEM_ID"
  make_test_image || { fail "could not copy the test image into the app"; return 0; }
  capture_with "$(with_pick "$SYSTEM_ID" "$IMAGE_PATH")" picked || return 0
  capture_with "$(with_pick "$SYSTEM_ID" "")" removed || return 0
  baseline="$(picked_share "$OUT_DIR/438-baseline.png")"
  picked="$(picked_share "$OUT_DIR/438-picked.png")"
  removed="$(picked_share "$OUT_DIR/438-removed.png")"
  changed="$(python3 -I "$HELPER" changed "$OUT_DIR/438-picked.png" "$OUT_DIR/438-baseline.png" | cut -d= -f2)"
  restored="$(python3 -I "$HELPER" changed "$OUT_DIR/438-removed.png" "$OUT_DIR/438-baseline.png" | cut -d= -f2)"
  at_least "$MAX_BASELINE" "$baseline" && pass "baseline: ${baseline} magenta, system art as before" \
    || skip "baseline already ${baseline} magenta (system art is pink?), picked check is weaker"
  at_least "$picked" "$MIN_PICKED" && pass "picked: ${picked} of the band is the test image (${changed} changed)" \
    || fail "picked: only ${picked} of the band is the test image"
  at_least "$MAX_RESTORED_CHANGE" "$restored" && at_least "$MAX_BASELINE" "$removed" \
    && pass "removed: back to the system art (${restored} differs from baseline)" \
    || fail "removed: ${restored} of the band still differs from baseline, magenta ${removed}"
}

main_438() {
  parse_438_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-438)"
  mkdir -p "$OUT_DIR"
  UI_XML="$OUT_DIR/ui.xml"
  FOCUSED_LABEL=""
  select_device
  install_if_asked
  trap restore_438 EXIT
  trap 'restore_438; exit 130' INT TERM
  preflight_438 || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_438
  check_no_crashes
  check_lock_unchanged
  restore_438
  section "For a human"
  echo "  look  $OUT_DIR/438-picked.png: the test image fills the Now background for $SYSTEM_ID"
  echo "  look  pick a real image in Systems > Appearance > Now background: Now updates without a restart"
  echo "  look  turn System background off: the editor row shows the hint, Now shows the theme"
  summary
}

main_438 "$@"

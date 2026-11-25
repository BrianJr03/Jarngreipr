#!/usr/bin/env bash
# #449: Jarn FE follows Command Panel's fonts, including imported ones.
#
#   scripts/jarnfe-verify/verify-449-cp-font-reader.sh [options]
#
#   --install APK   adb install -r this Jarn FE APK first (never uninstalls).
#   --look KEY      Command Panel look to apply first (custom:<id> or a built-in name). Needs a debug
#                   Command Panel (its DEBUG_BACKGROUND broadcast); restored on exit.
#   --cp-pkg PKG    Command Panel package that owns the look (default jr.brian.commandpannel.debug).
#   --out DIR       Where screenshots go (default: a new temp folder).
#
# Debug Jarn FE only (run-as). Steps:
#   1. reads Command Panel's /fonts row from the authority Jarn reads (jr.brian.commandpannel.theme),
#      after applying --look if given. With no user: font in the row the imported-font checks skip.
#   2. writes match_command_panel_fonts=true with Jarn stopped, starts Stage and waits.
#   3. run-as: no_backup/cp_fonts/<sha256>.ttf exists for each user: font and hashes to its name,
#      and no_backup/command_panel_fonts holds the row's look_id.
#   4. screenshots both screens with fonts on, then with fonts off, for a human to compare.
#   5. restore: the fonts switch goes back, Jarn restarts, and with it off the copies are gone.
#
# Trust note: Jarn reads only the release Command Panel authority, and a release Command Panel only
# serves font files to release-signed Jarngreipr. A debug Jarn FE therefore sees the row but is refused
# the file, so step 3 fails by design until one side trusts the other's debug build. A failure there
# prints which case it is.
#
# Exit codes: 0 no failures, 1 at least one failure, 2 could not run.
set -uo pipefail

VERIFY_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=run.sh
source "$VERIFY_DIR/run.sh"

LOOK_PREFS=jarnfe_look_prefs
FONTS_KEY=match_command_panel_fonts
CP_AUTHORITY=jr.brian.commandpannel.theme
CP_PKG=jr.brian.commandpannel.debug
CP_PREFS=/data/user_de/0/$CP_PKG/shared_prefs/command_panel.xml
CP_ACTION=jr.brian.commandpannel.action.DEBUG_BACKGROUND
INSTALL_APK="" LOOK="" ORIG_FONTS="" ORIG_CP_LOOK="" PREF_CHANGED=0 LOOK_CHANGED=0 ROW="" SHAS=""

usage_449() { sed -n '2,26p' "$VERIFY_DIR/verify-449-cp-font-reader.sh" | sed 's/^# \{0,1\}//'; }

parse_449_args() {
  while [ $# -gt 0 ]; do
    case "$1" in
      --install) INSTALL_APK="${2:?--install needs a path}"; shift ;;
      --look) LOOK="${2:?--look needs a key}"; shift ;;
      --cp-pkg) CP_PKG="${2:?--cp-pkg needs a package}"; CP_PREFS=/data/user_de/0/$CP_PKG/shared_prefs/command_panel.xml; shift ;;
      --out) OUT_DIR="${2:?--out needs a path}"; shift ;;
      -h|--help) usage_449; exit 0 ;;
      *) echo "unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
  done
}

look_pref() { read_prefs "$LOOK_PREFS" | parse_pref "$1"; }

# Writes boolean pref $1 as $2, or removes it when $2 is empty, with Jarn stopped.
write_bool() {
  local name="$1" value="$2" script
  sh_dev am force-stop "$PKG" >/dev/null
  script="$(python3 -I -c '
import sys
import xml.etree.ElementTree as ET
raw, key, value = sys.stdin.read(), sys.argv[1], sys.argv[2]
try:
    root = ET.fromstring(raw) if raw.strip() else ET.Element("map")
except ET.ParseError:
    root = ET.Element("map")
for item in list(root):
    if item.get("name") == key:
        root.remove(item)
if value:
    ET.SubElement(root, "boolean", name=key, value=value)
print("<?xml version=\x271.0\x27 encoding=\x27utf-8\x27 standalone=\x27yes\x27 ?>")
print(ET.tostring(root, encoding="unicode"))
' "$name" "$value" <<<"$(read_prefs "$LOOK_PREFS")")" || return 1
  printf '%s\n' "$script" | adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/$LOOK_PREFS.xml'" || return 1
  [ "$(look_pref "$name")" = "$value" ]
}

# The value of column $2 in a `content query` row $1.
row_col() { grep -oE "(^| )$2=[^,]*" <<<"$1" | head -1 | sed -E "s/^ ?$2=//"; }

cp_look() { sh_dev run-as "$CP_PKG" cat "$CP_PREFS" | grep 'name="overlay_look"' | sed -E 's/.*>([^<]*)<.*/\1/'; }
set_cp_look() { sh_dev am broadcast -a "$CP_ACTION" -p "$CP_PKG" --es look "$1" >/dev/null; pause 2; }

start_stage() {
  sh_dev am start -W --display "$TOP_DISPLAY" -n "$PKG/$STAGE_ACTIVITY" >/dev/null
  pause 8
}

copies() { sh_dev run-as "$PKG" ls no_backup/cp_fonts 2>/dev/null | grep -v '^ls:'; }

restore_449() {
  if [ "$PREF_CHANGED" = 1 ]; then
    PREF_CHANGED=0
    section "Restore"
    if write_bool "$FONTS_KEY" "$ORIG_FONTS"; then echo "  info  Match Command Panel fonts back to ${ORIG_FONTS:-default (off)}"
    else echo "  WARN  could not restore Match Command Panel fonts (was ${ORIG_FONTS:-default})"; fi
    start_stage
  fi
  if [ "$LOOK_CHANGED" = 1 ]; then
    LOOK_CHANGED=0
    set_cp_look "$ORIG_CP_LOOK"
    if [ "$(cp_look)" = "$ORIG_CP_LOOK" ]; then echo "  info  Command Panel look back to ${ORIG_CP_LOOK:-default}"
    else echo "  WARN  Command Panel look is $(cp_look), was ${ORIG_CP_LOOK:-default}"; fi
  fi
}

preflight_449() {
  section "Preflight"
  text_matches "$(sh_dev pm path "$PKG")" '^package:' || { echo "$PKG is not installed" >&2; return 1; }
  read_prefs "$LOOK_PREFS" >/dev/null || { echo "run-as is unavailable for $PKG (debug Jarn FE only)" >&2; return 1; }
  read_roles
  ORIG_FONTS="$(look_pref "$FONTS_KEY")"
  echo "  info  $PKG, fonts was ${ORIG_FONTS:-default (off)}, output in $OUT_DIR"
  if [ -n "$LOOK" ]; then
    text_matches "$(sh_dev pm path "$CP_PKG")" '^package:' || { echo "$CP_PKG is not installed" >&2; return 1; }
    ORIG_CP_LOOK="$(cp_look)"
    echo "  info  Command Panel look was ${ORIG_CP_LOOK:-default}"
  fi
}

read_row() {
  section "Command Panel fonts row"
  if [ -n "$LOOK" ]; then
    LOOK_CHANGED=1
    set_cp_look "$LOOK"
    [ "$(cp_look)" = "$LOOK" ] && pass "applied Command Panel look $LOOK" || fail "Command Panel look is $(cp_look), not $LOOK"
  fi
  ROW="$(sh_dev content query --uri "content://$CP_AUTHORITY/fonts")"
  echo "  info  $ROW"
  [ "$(row_col "$ROW" version)" = 2 ] || { fail "no version 2 fonts row on $CP_AUTHORITY"; return 1; }
  pass "fonts row version 2, look_id=$(row_col "$ROW" look_id)"
  SHAS="$(for role in display label; do
    [[ "$(row_col "$ROW" "${role}_font")" == user:* ]] && row_col "$ROW" "${role}_font_sha256"
  done | sort -u)"
  [ -n "$SHAS" ] || skip "the row has no user: font, so the imported-font checks skip (pass --look custom:<id>)"
}

check_copies() {
  section "Cached copies"
  local sha listing actual
  listing="$(copies)"
  for sha in $SHAS; do
    if text_has_line "$listing" "$sha.ttf"; then
      actual="$(sh_dev run-as "$PKG" sha256sum "no_backup/cp_fonts/$sha.ttf" | awk '{ print $1 }')"
      [ "$actual" = "$sha" ] && pass "copy $sha.ttf matches its digest" || fail "copy $sha.ttf hashes to $actual"
    else
      fail "no copy for $sha (debug Jarn and release Command Panel don't trust each other's debug builds, see the trust note)"
    fi
  done
  local cached
  cached="$(sh_dev run-as "$PKG" cat no_backup/command_panel_fonts 2>/dev/null)"
  if text_has "$cached" "$(row_col "$ROW" look_id)"; then pass "cached row holds look_id $(row_col "$ROW" look_id)"
  else fail "cached row missing or stale: ${cached:-nothing}"; fi
}

shots() {
  shot "$STAGE_PHYSICAL" "$OUT_DIR/449-stage-$1.png" || skip "Stage screenshot failed"
  shot "$DECK_PHYSICAL" "$OUT_DIR/449-deck-$1.png" || skip "Deck screenshot failed"
}

run_449() {
  read_row || return 0
  section "Fonts on"
  write_bool "$FONTS_KEY" true || { fail "could not turn the fonts switch on"; return 0; }
  PREF_CHANGED=1
  start_stage
  check_copies
  shots on
  section "Fonts off"
  write_bool "$FONTS_KEY" false || { fail "could not turn the fonts switch off"; return 0; }
  start_stage
  [ -z "$(copies)" ] && pass "copies deleted once matching is off" || fail "copies left behind: $(copies | tr '\n' ' ')"
  shots off
}

main_449() {
  parse_449_args "$@"
  [ -n "$OUT_DIR" ] || OUT_DIR="$(mktemp -d -t jarnfe-verify-449)"
  mkdir -p "$OUT_DIR"
  select_device
  if [ -n "$INSTALL_APK" ]; then
    adb install -r "$INSTALL_APK" || { echo "adb install failed (signature mismatch? not uninstalling)" >&2; exit 2; }
  fi
  trap restore_449 EXIT
  trap 'restore_449; exit 130' INT TERM
  preflight_449 || { echo "Preflight failed, nothing was changed." >&2; exit 2; }
  run_449
  restore_449
  section "For a human"
  echo "  look  $OUT_DIR/449-*-on.png vs 449-*-off.png: headings and labels in Command Panel's fonts when on"
  summary
}

main_449 "$@"

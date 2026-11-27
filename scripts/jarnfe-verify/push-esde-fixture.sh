#!/usr/bin/env bash
# #575: copies a synthetic ES-DE fixture theme into ES-DE's themes folder on an EMULATOR, for
# trying the ES-DE theme import by hand or from a later script.
#
#   scripts/jarnfe-verify/push-esde-fixture.sh <fixture|--list> [--remove]
#
#   <fixture>   A folder name under frontend/src/test/resources/esde-fixtures (see --list).
#   --list      Lists the fixtures and exits.
#   --remove    Deletes the pushed copy instead.
#
# The theme lands in /sdcard/ES-DE/themes/jarn-fixture-<fixture>, so it never replaces a real
# theme. The hostile fixture also gets its oversize include (written here, not checked in) and a
# file just outside its folder, as the unit test does. Only emulator serials are accepted
# (ANDROID_SERIAL, default emulator-5554); a real device is refused.
#
# Exit codes: 0 done, 1 failed, 2 could not run.
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
FIXTURES="$REPO_ROOT/frontend/src/test/resources/esde-fixtures"
THEMES_DIR=/sdcard/ES-DE/themes
PREFIX=jarn-fixture-
SERIAL="${ANDROID_SERIAL:-emulator-5554}"
OVERSIZE_BYTES=$((512 * 1024 + 4 * 1024))

usage() { sed -n '2,17p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'; }

list_fixtures() {
  find "$FIXTURES" -mindepth 1 -maxdepth 1 -type d -exec basename {} \; | sort
}

require_emulator() {
  case "$SERIAL" in
    emulator-*) ;;
    *) echo "refusing $SERIAL: this script only writes to emulators" >&2; exit 2 ;;
  esac
  [ "$(adb -s "$SERIAL" get-state 2>/dev/null)" = device ] || { echo "$SERIAL is not running" >&2; exit 2; }
}

write_oversize() {
  local file="$1"
  python3 -I -c '
import sys
size = int(sys.argv[2])
body = "padding " * (size // 8 + 1)
open(sys.argv[1], "w").write("<theme><!--" + body + "--></theme>")
' "$file" "$OVERSIZE_BYTES"
}

push_fixture() {
  local name="$1" staging target="$THEMES_DIR/$PREFIX$1"
  [ -d "$FIXTURES/$name" ] || { echo "no fixture named $name (try --list)" >&2; exit 2; }
  staging="$(mktemp -d -t jarn-esde-fixture)"
  cp -R "$FIXTURES/$name" "$staging/$PREFIX$name"
  if [ "$name" = hostile ]; then
    write_oversize "$staging/$PREFIX$name/_inc/huge.xml"
    printf '%s\n' '<theme><view name="system"><text name="leak"/></view></theme>' >"$staging/outside.xml"
  fi
  adb -s "$SERIAL" shell mkdir -p "$THEMES_DIR" || exit 1
  adb -s "$SERIAL" shell rm -rf "$target" || exit 1
  adb -s "$SERIAL" push "$staging/$PREFIX$name" "$THEMES_DIR/" >/dev/null || exit 1
  if [ -f "$staging/outside.xml" ]; then
    adb -s "$SERIAL" push "$staging/outside.xml" "$THEMES_DIR/outside.xml" >/dev/null || exit 1
  fi
  rm -rf "$staging"
  echo "pushed $name to $target"
  adb -s "$SERIAL" shell ls "$target"
}

remove_fixture() {
  local name="$1"
  adb -s "$SERIAL" shell rm -rf "$THEMES_DIR/$PREFIX$name" || exit 1
  [ "$name" != hostile ] || adb -s "$SERIAL" shell rm -f "$THEMES_DIR/outside.xml"
  echo "removed $THEMES_DIR/$PREFIX$name"
}

main() {
  local name="" remove=0
  while [ $# -gt 0 ]; do
    case "$1" in
      --list) list_fixtures; exit 0 ;;
      --remove) remove=1 ;;
      -h|--help) usage; exit 0 ;;
      -*) echo "unknown option: $1" >&2; exit 2 ;;
      *) name="$1" ;;
    esac
    shift
  done
  [ -n "$name" ] || { usage; exit 2; }
  require_emulator
  if [ "$remove" = 1 ]; then remove_fixture "$name"; else push_fixture "$name"; fi
}

main "$@"

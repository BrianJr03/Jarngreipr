#!/usr/bin/env bash
# Jarn FE look prefs through run-as (debug builds only). Callers set PKG and LOOK_PREFS.

look_pref() { read_prefs "$LOOK_PREFS" | parse_pref "$1"; }

# Writes boolean pref $1 as $2 (true or false), or removes it when $2 is empty, with the app stopped.
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

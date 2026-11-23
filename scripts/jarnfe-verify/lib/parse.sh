#!/usr/bin/env bash
# Pure text parsers for dumpsys output. They read stdin and never touch the device.

# `dumpsys window` -> the display id holding top focus.
parse_top_display() {
  grep -oE 'mTopFocusedDisplayId=-?[0-9]+' | sed -n '1s/.*=//p'
}

# `dumpsys window displays` -> the focused pkg/activity on display $1.
parse_focused_app() {
  awk -v want="mDisplayId=$1" '
    /Display: mDisplayId=/ {
      current = ""
      for (i = 1; i <= NF; i++) if ($i ~ /^mDisplayId=/) current = $i
    }
    !found && current == want && /mFocusedApp=/ {
      found = 1
      for (i = 1; i <= NF; i++) if ($i ~ /\//) { gsub(/[{}]/, "", $i); print $i; break }
    }'
}

# `dumpsys activity activities` -> every resumed pkg/activity on display $1, one per line.
parse_resumed() {
  awk -v want="#$1" '
    /^ *Display #/ {
      current = ""
      for (i = 1; i <= NF; i++) if ($i ~ /^#[0-9]+$/) current = $i
    }
    current == want && /[Rr]esumedActivity[:=]/ {
      for (i = 1; i <= NF; i++) if ($i ~ /\//) { gsub(/[{}]/, "", $i); print $i; break }
    }' | sort -u
}

# Shared prefs XML -> the value of key $1, for string, boolean and int entries.
parse_pref() {
  python3 -I -c '
import sys
import xml.etree.ElementTree as ET
try:
    root = ET.fromstring(sys.stdin.read())
except ET.ParseError:
    sys.exit(0)
for item in root:
    if item.get("name") == sys.argv[1]:
        print(item.get("value") if item.get("value") is not None else (item.text or ""))
        break
' "$1"
}

# Mode, current setting -> 0 when the app's own focus lock sync would not change the setting.
focus_sync_is_inert() {
  case "$1:$2" in
    Off:*|Top:1|Bottom:2) return 0 ;;
    *) return 1 ;;
  esac
}

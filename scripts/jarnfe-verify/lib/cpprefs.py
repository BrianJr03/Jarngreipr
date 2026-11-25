#!/usr/bin/env python3
"""Reads and edits Command Panel's command_panel.xml prefs (from stdin). Never touches a device.

Usage:
  cpprefs.py font                     -> "id<TAB>name<TAB>file" of the imported font to use, or nothing.
                                         A font named like ShareTechMono wins, else the first one.
  cpprefs.py look-for FONT_ID         -> id of a saved look whose label or display font is FONT_ID.
  cpprefs.py inject LOOK_ID FONT_ID NAME FILE
                                      -> the prefs XML with a saved look added (Aurora Glass base,
                                         both fonts set to the imported font).
"""
import json
import sys
import xml.etree.ElementTree as ET

LOOK_FORMAT = "command-panel-look"


def load(raw):
    try:
        return ET.fromstring(raw) if raw.strip() else ET.Element("map")
    except ET.ParseError:
        return ET.Element("map")


def string_pref(root, key):
    for item in root:
        if item.tag == "string" and item.get("name") == key:
            return item, item.text or ""
    return None, ""


def json_list(root, key):
    _, text = string_pref(root, key)
    try:
        value = json.loads(text) if text else []
    except ValueError:
        return []
    return value if isinstance(value, list) else []


def squash(text):
    return "".join(ch for ch in text.lower() if ch.isalnum())


def pick_font(root):
    fonts = [f for f in json_list(root, "user_fonts") if isinstance(f, dict) and f.get("id")]
    for font in fonts:
        if "sharetechmono" in squash(font.get("name", "") + font.get("file", "")):
            return font
    return fonts[0] if fonts else None


def font_ids(look):
    for key in ("labelFont", "displayFont"):
        ref = look.get(key)
        if isinstance(ref, dict) and ref.get("id"):
            yield ref["id"]


def look_for(root, font_id):
    for look in json_list(root, "custom_looks"):
        if isinstance(look, dict) and look.get("id") and font_id in font_ids(look):
            return look["id"]
    return None


def inject(root, look_id, font_id, name, file_name):
    looks = [l for l in json_list(root, "custom_looks") if not (isinstance(l, dict) and l.get("id") == look_id)]
    ref = {"file": file_name, "user": name, "id": font_id}
    looks.append({
        "format": LOOK_FORMAT, "version": 1, "id": look_id, "name": "Jarn verify 452",
        "baseLook": "AURORA_GLASS", "labelFont": ref, "displayFont": dict(ref),
    })
    item, _ = string_pref(root, "custom_looks")
    if item is None:
        item = ET.SubElement(root, "string", name="custom_looks")
    item.text = json.dumps(looks, separators=(",", ":"))
    print("<?xml version='1.0' encoding='utf-8' standalone='yes' ?>")
    print(ET.tostring(root, encoding="unicode"))


def main(argv):
    root = load(sys.stdin.read())
    command = argv[1] if len(argv) > 1 else ""
    if command == "font":
        font = pick_font(root)
        if font:
            name = font.get("name", "")
            print("\t".join([font["id"], name, font.get("file") or name + ".ttf"]))
    elif command == "look-for" and len(argv) == 3:
        found = look_for(root, argv[2])
        if found:
            print(found)
    elif command == "inject" and len(argv) == 6:
        inject(root, *argv[2:6])
    else:
        print(__doc__, file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

#!/usr/bin/env python3
"""Canvas layout, folder store and tile removal journal helpers for verify-372-373-378-removal.sh.

Reads and writes local copies only; the shell script moves files to and from the device.

  removal.py dangling LAYOUT_XML FOLDERS_PB            tiles on a missing folder, "page tile folder" per line;
                                                       exit 3 when a page's folders can't be read
  removal.py canvas-page PAGE_TYPES_XML               lowest Canvas page index (exit 1 when none)
  removal.py add-canvas-page TYPES_XML COUNT_XML OUT_TYPES OUT_COUNT   appends a Canvas page, prints its index
  removal.py ensure-layout LAYOUT_XML OUT PAGE         adds an empty saved layout for PAGE when it has none
  removal.py first-page LAYOUT_XML                     lowest page index that has a saved layout
  removal.py has-tile LAYOUT_XML PAGE TILE             exit 0 on the page, 1 not on it, 2 unreadable
  removal.py has-folder FOLDERS_PB PAGE FOLDER         exit 0 stored on the page, 1 not, 2 unreadable
  removal.py add-folder FOLDERS_PB OUT PAGE FOLDER NAME PKG
  removal.py add-tile LAYOUT_XML OUT PAGE TILE FOLDER
  removal.py drop-tile LAYOUT_XML OUT PAGE TILE
  removal.py journal OUT PAGE TILE FOLDER [PAGE TILE FOLDER ...]
  removal.py journal-pending JOURNAL_XML               number of pending removals (0 when absent)
  removal.py center UI_XML LABEL                       "x y" of the first node labelled LABEL
  removal.py has-text UI_XML TEXT                      exit 0 when any node label contains TEXT
"""
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

LAYOUT_KEY = re.compile(r"^layout_(\d+)$")
FOLDER_KEY = "folders_canvas_page_{}"
JOURNAL_KEY = "pending"


def read_varint(data, pos):
    shift = result = 0
    while True:
        byte = data[pos]
        pos += 1
        result |= (byte & 0x7F) << shift
        if not byte & 0x80:
            return result, pos
        shift += 7


def write_varint(value):
    out = bytearray()
    while True:
        bits = value & 0x7F
        value >>= 7
        if value:
            out.append(bits | 0x80)
        else:
            out.append(bits)
            return bytes(out)


def fields(data):
    pos = 0
    while pos < len(data):
        tag, pos = read_varint(data, pos)
        number, wire = tag >> 3, tag & 7
        if wire == 0:
            value, pos = read_varint(data, pos)
        elif wire == 1:
            value, pos = data[pos:pos + 8], pos + 8
        elif wire == 2:
            size, pos = read_varint(data, pos)
            value, pos = data[pos:pos + size], pos + size
        elif wire == 5:
            value, pos = data[pos:pos + 4], pos + 4
        else:
            raise ValueError("unsupported wire type %d" % wire)
        yield number, wire, value


def length_field(number, payload):
    return write_varint(number << 3 | 2) + write_varint(len(payload)) + payload


def read_pb(path):
    """DataStore preferences -> ordered list of (key, raw Value bytes)."""
    if not os.path.exists(path):
        return []
    with open(path, "rb") as handle:
        data = handle.read()
    entries = []
    for number, wire, entry in fields(data):
        if number != 1 or wire != 2:
            continue
        key, value = None, b""
        for inner, inner_wire, payload in fields(entry):
            if inner == 1 and inner_wire == 2:
                key = payload.decode("utf-8")
            elif inner == 2 and inner_wire == 2:
                value = payload
        if key is not None:
            entries.append((key, value))
    return entries


def pb_string(value):
    for number, wire, payload in fields(value):
        if number == 5 and wire == 2:
            return payload.decode("utf-8")
    return None


def write_pb(path, entries):
    out = b"".join(
        length_field(1, length_field(1, key.encode("utf-8")) + length_field(2, value))
        for key, value in entries
    )
    with open(path, "wb") as handle:
        handle.write(out)


def stored_folders(entries, page):
    """Folder ids stored for a page, or None when that page's folders can't be read."""
    raw = dict(entries).get(FOLDER_KEY.format(page))
    if raw is None:
        return set()
    text = pb_string(raw)
    if text is None:
        return None
    try:
        return {folder["id"] for folder in json.loads(text)}
    except (ValueError, KeyError, TypeError):
        return None


def read_layouts(path):
    tree = ET.parse(path)
    layouts = {}
    for item in tree.getroot():
        match = LAYOUT_KEY.match(item.get("name") or "")
        if item.tag == "string" and match:
            layouts[int(match.group(1))] = (item, json.loads(item.text or "{}"))
    return tree, layouts


def write_xml(tree, path):
    with open(path, "wb") as handle:
        handle.write(b"<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n")
        tree.write(handle, encoding="utf-8", xml_declaration=False)


def folder_tiles(layout):
    return [item for item in layout.get("items", []) if item.get("type") == "folder"]


def next_rect(arrangement, horizontal):
    if horizontal:
        col = max((r["col"] + r["colSpan"] for r in arrangement.values()), default=0)
        return {"col": col, "row": 0, "colSpan": 1, "rowSpan": 1}
    row = max((r["row"] + r["rowSpan"] for r in arrangement.values()), default=0)
    return {"col": 0, "row": row, "colSpan": 1, "rowSpan": 1}


def node_label(node):
    return node.get("text") or node.get("content-desc") or ""


def ui_nodes(path):
    return ET.parse(path).getroot().iter("node")


def cmd_dangling(layout_xml, folders_pb):
    entries = read_pb(folders_pb)
    _, layouts = read_layouts(layout_xml)
    unreadable = False
    for page, (_, layout) in sorted(layouts.items()):
        stored = stored_folders(entries, page)
        if stored is None:
            print("folders on page %d can't be read" % page, file=sys.stderr)
            unreadable = True
            continue
        for tile in folder_tiles(layout):
            if tile.get("folderId") not in stored:
                print("%d %s %s" % (page, tile.get("id"), tile.get("folderId")))
    return 3 if unreadable else 0


def prefs_values(path):
    return {item.get("name"): item for item in ET.parse(path).getroot()}


def cmd_canvas_page(types_xml):
    if not os.path.exists(types_xml):
        return 1
    values = prefs_values(types_xml)
    count_node = values.get("page_count")
    count = int(count_node.get("value")) if count_node is not None else 0
    for index in range(count):
        node = values.get("page_type_%d" % index)
        if node is not None and (node.text or "") == "UNIFIED_CANVAS":
            print(index)
            return 0
    return 1


def cmd_add_canvas_page(types_xml, count_xml, out_types, out_count):
    types_tree = ET.parse(types_xml)
    count_tree = ET.parse(count_xml)
    types_count = types_tree.getroot().find("int[@name='page_count']")
    pages_count = count_tree.getroot().find("int[@name='page_count']")
    if types_count is None or pages_count is None:
        return 1
    index = int(types_count.get("value"))
    ET.SubElement(types_tree.getroot(), "string", name="page_type_%d" % index).text = "UNIFIED_CANVAS"
    types_count.set("value", str(index + 1))
    pages_count.set("value", str(int(pages_count.get("value")) + 1))
    write_xml(types_tree, out_types)
    write_xml(count_tree, out_count)
    print(index)
    return 0


def cmd_ensure_layout(layout_xml, out, page):
    if os.path.exists(layout_xml) and os.path.getsize(layout_xml) > 0:
        tree = ET.parse(layout_xml)
    else:
        tree = ET.ElementTree(ET.Element("map"))
    key = "layout_%d" % int(page)
    if not any(item.get("name") == key for item in tree.getroot()):
        empty = {"items": [], "verticalArrangement": {}, "horizontalArrangement": {}, "version": 2}
        ET.SubElement(tree.getroot(), "string", name=key).text = json.dumps(empty, separators=(",", ":"))
    write_xml(tree, out)
    return 0


def cmd_first_page(layout_xml):
    _, layouts = read_layouts(layout_xml)
    if not layouts:
        return 1
    print(min(layouts))
    return 0


def cmd_has_tile(layout_xml, page, tile):
    _, layouts = read_layouts(layout_xml)
    layout = layouts.get(int(page), (None, {}))[1]
    return 0 if any(item.get("id") == tile for item in layout.get("items", [])) else 1


def cmd_has_folder(folders_pb, page, folder):
    if not os.path.exists(folders_pb):
        print("removal.py has-folder: %s is missing" % folders_pb, file=sys.stderr)
        return 2
    stored = stored_folders(read_pb(folders_pb), int(page))
    if stored is None:
        return 2
    return 0 if folder in stored else 1


def cmd_add_folder(folders_pb, out, page, folder, name, pkg):
    entries = read_pb(folders_pb)
    key = FOLDER_KEY.format(page)
    current = dict(entries).get(key)
    text = pb_string(current) if current is not None else "[]"
    if text is None:
        return 1
    folders = json.loads(text)
    folders.append({"id": folder, "name": name, "appPackageNames": [pkg], "x": 0.0, "y": 0.0, "iconSize": 64.0})
    value = length_field(5, json.dumps(folders, separators=(",", ":")).encode("utf-8"))
    entries = [(k, v) for k, v in entries if k != key] + [(key, value)]
    write_pb(out, entries)
    return 0


def cmd_add_tile(layout_xml, out, page, tile, folder):
    tree, layouts = read_layouts(layout_xml)
    if int(page) not in layouts:
        return 1
    node, layout = layouts[int(page)]
    layout.setdefault("items", []).append({"type": "folder", "id": tile, "folderId": folder})
    for key, horizontal in (("horizontalArrangement", True), ("verticalArrangement", False)):
        arrangement = layout.setdefault(key, {})
        arrangement[tile] = next_rect(arrangement, horizontal)
    node.text = json.dumps(layout, separators=(",", ":"))
    write_xml(tree, out)
    return 0


def cmd_drop_tile(layout_xml, out, page, tile):
    tree, layouts = read_layouts(layout_xml)
    if int(page) not in layouts:
        return 1
    node, layout = layouts[int(page)]
    layout["items"] = [item for item in layout.get("items", []) if item.get("id") != tile]
    for key in ("horizontalArrangement", "verticalArrangement"):
        layout.get(key, {}).pop(tile, None)
    node.text = json.dumps(layout, separators=(",", ":"))
    write_xml(tree, out)
    return 0


def cmd_journal(out, *triples):
    pending = [
        {"pageIndex": int(page), "item": {"type": "folder", "id": tile, "folderId": folder}}
        for page, tile, folder in zip(triples[0::3], triples[1::3], triples[2::3])
    ]
    root = ET.Element("map")
    ET.SubElement(root, "string", name=JOURNAL_KEY).text = json.dumps(pending, separators=(",", ":"))
    write_xml(ET.ElementTree(root), out)
    return 0


def cmd_journal_pending(journal_xml):
    if not os.path.exists(journal_xml) or os.path.getsize(journal_xml) == 0:
        print(0)
        return 0
    for item in ET.parse(journal_xml).getroot():
        if item.get("name") == JOURNAL_KEY:
            print(len(json.loads(item.text or "[]")))
            return 0
    print(0)
    return 0


def cmd_center(ui_xml, label):
    for node in ui_nodes(ui_xml):
        if node_label(node) == label:
            bounds = re.findall(r"\d+", node.get("bounds") or "")
            if len(bounds) == 4:
                left, top, right, bottom = map(int, bounds)
                print("%d %d" % ((left + right) // 2, (top + bottom) // 2))
                return 0
    return 1


def cmd_has_text(ui_xml, text):
    return 0 if any(text in node_label(node) for node in ui_nodes(ui_xml)) else 1


COMMANDS = {
    "dangling": cmd_dangling,
    "first-page": cmd_first_page,
    "canvas-page": cmd_canvas_page,
    "add-canvas-page": cmd_add_canvas_page,
    "ensure-layout": cmd_ensure_layout,
    "has-tile": cmd_has_tile,
    "has-folder": cmd_has_folder,
    "add-folder": cmd_add_folder,
    "add-tile": cmd_add_tile,
    "drop-tile": cmd_drop_tile,
    "journal": cmd_journal,
    "journal-pending": cmd_journal_pending,
    "center": cmd_center,
    "has-text": cmd_has_text,
}


def main(argv):
    if len(argv) < 2 or argv[1] not in COMMANDS:
        print(__doc__, file=sys.stderr)
        return 2
    try:
        return COMMANDS[argv[1]](*argv[2:])
    except (OSError, ET.ParseError, ValueError, TypeError, IndexError) as error:
        print("removal.py %s: %s" % (argv[1], error), file=sys.stderr)
        return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))

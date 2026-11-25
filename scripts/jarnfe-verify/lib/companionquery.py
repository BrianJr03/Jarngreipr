#!/usr/bin/env python3
"""Reads a uiautomator XML dump for the companion picker (#443).

Usage: companionquery.py FILE picker TITLE
       companionquery.py FILE tag TAG
picker prints "shown=0|1 rows=N focused=0|1": whether the picker sheet (by its tag, or else its
title) is on screen, how many clickable rows it holds, and whether focus sits inside it.
tag prints 1 when a node carries the test tag TAG, else 0.
"""
import sys
import xml.etree.ElementTree as ET

PICKER_TAG = "jarn_companion_picker"


def label(node):
    return node.get("text") or node.get("content-desc") or ""


def tagged(root, tag=PICKER_TAG):
    for node in root.iter("node"):
        if (node.get("resource-id") or "").rsplit("/", 1)[-1] == tag:
            return node
    return None


def titled(root, title):
    return any(label(node) == title for node in root.iter("node"))


def picker(root, title):
    sheet = tagged(root)
    if sheet is None:
        return f"shown={int(titled(root, title))} rows=0 focused=0"
    nodes = list(sheet.iter("node"))
    rows = sum(1 for node in nodes if node is not sheet and node.get("clickable") == "true")
    focused = any(node.get("focused") == "true" for node in nodes)
    return f"shown=1 rows={rows} focused={int(focused)}"


def main(argv):
    if len(argv) < 4 or argv[2] not in ("picker", "tag"):
        return 2
    try:
        root = ET.parse(argv[1]).getroot()
    except (ET.ParseError, OSError):
        return 2
    if argv[2] == "tag":
        print(int(tagged(root, argv[3]) is not None))
    else:
        print(picker(root, argv[3]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

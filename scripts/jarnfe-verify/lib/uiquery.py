#!/usr/bin/env python3
"""Reads a uiautomator XML dump. Usage: uiquery.py FILE role|has TEXT|selected LABEL...|focused"""
import sys
import xml.etree.ElementTree as ET

WORDMARK = "JARN"
# Each window tags its root with its role, so the role reads the same whichever cue is chosen.
ROLE_TAGS = {"jarn_stage": "stage", "jarn_deck": "deck"}
DECK_TABS = {"Now", "Apps", "Canvas"}
SETTINGS_TABS = {"Library", "Art", "Look", "Display", "Apps", "General", "Data"}


def label(node):
    return node.get("text") or node.get("content-desc") or ""


def labels_in(node):
    return [text for text in (label(item) for item in node.iter("node")) if text]


def tagged_role(root):
    for node in root.iter("node"):
        resource = (node.get("resource-id") or "").rsplit("/", 1)[-1]
        if resource in ROLE_TAGS:
            return ROLE_TAGS[resource]
    return ""


def role(root):
    tagged = tagged_role(root)
    if tagged:
        return tagged
    # Builds before the role tags only ever showed the wordmark on Stage.
    found = set(labels_in(root))
    if WORDMARK in found:
        return "stage"
    if DECK_TABS <= found or SETTINGS_TABS <= found:
        return "deck"
    return "unknown"


def selected(root, wanted):
    for node in root.iter("node"):
        if node.get("selected") != "true":
            continue
        for text in labels_in(node):
            if text in wanted:
                return text
    return ""


def focused(root):
    for node in root.iter("node"):
        if node.get("focused") == "true":
            return " | ".join(labels_in(node))
    return ""


def main(argv):
    if len(argv) < 3:
        return 2
    try:
        root = ET.parse(argv[1]).getroot()
    except (ET.ParseError, OSError):
        return 2
    command, args = argv[2], argv[3:]
    if command == "role":
        print(role(root))
    elif command == "has":
        return 0 if args and args[0] in labels_in(root) else 1
    elif command == "selected":
        print(selected(root, set(args)))
    elif command == "focused":
        print(focused(root))
    else:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

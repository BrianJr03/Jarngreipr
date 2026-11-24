#!/usr/bin/env python3
"""Checks Settings group headings in a uiautomator dump.

Usage: settingsquery.py STATE_JSON UI_XML HEADING...

Prints tab-separated lines:
  focus  LABEL                    the focused row's main label, or empty
  seen   HEADING                  each listed heading that is in the dump
  first  HEADING  ok|hidden       when the focused row is that heading's first row

A first row is learned while walking down, where the heading sits right above it on screen with
no other focusable row between them. Walking back up, the same row must show that heading again, fully.
uiautomator leaves out nodes that are off screen and clips the bounds of partly visible ones, so
a missing heading, or one shorter than its full height, counts as hidden.
"""
import json
import os
import re
import sys
import xml.etree.ElementTree as ET

BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")
CLIP_SLOP = 2
# A focused row grows a little past its slot, so it can overlap what sits just above it.
ABOVE_SLOP = 8


def bounds(node):
    match = BOUNDS.fullmatch(node.get("bounds") or "")
    return tuple(int(value) for value in match.groups()) if match else None


def label(node):
    return node.get("text") or node.get("content-desc") or ""


def labels_in(node):
    return [text for text in (label(item) for item in node.iter("node")) if text]


def load_state(path):
    if os.path.exists(path):
        with open(path) as handle:
            return json.load(handle)
    return {"first": {}, "height": {}}


def save_state(path, state):
    with open(path, "w") as handle:
        json.dump(state, handle)


def parents_of(root):
    return {child: parent for parent in root.iter() for child in parent}


def viewport(node, parents, root):
    current = node
    while current in parents:
        current = parents[current]
        if current.get("scrollable") == "true" and bounds(current):
            return bounds(current)
    first = next(iter(root.iter("node")), None)
    return bounds(first) if first is not None else None


def fully_visible(heading, view, full_height):
    box = bounds(heading)
    if box is None or view is None:
        return False
    inside = box[1] >= view[1] and box[3] <= view[3] and box[3] > box[1]
    return inside and (box[3] - box[1]) >= full_height - CLIP_SLOP


def in_row(node, parents):
    """True for text inside a focusable row, like a row's value that matches a heading's text."""
    current = node
    while current in parents:
        current = parents[current]
        if current.get("focusable") == "true":
            return True
    return False


def heading_above(nodes, focused, headings, skip):
    """The heading directly above the focused row, with no other row in between, or None.

    Uses bounds, not dump order: a focused Compose row can be listed after rows below it.
    """
    target = bounds(focused)
    if target is None:
        return None
    closest, closest_bottom = None, None
    for node in nodes:
        if node in skip:
            continue
        is_heading = node in headings
        if not is_heading and node.get("focusable") != "true":
            continue
        box = bounds(node)
        if box is None or box[3] > target[1] + ABOVE_SLOP:
            continue
        if not is_heading and (box[2] <= target[0] or box[0] >= target[2]):
            continue
        if closest_bottom is None or box[3] > closest_bottom:
            closest, closest_bottom = node, box[3]
    return closest if closest is not None and closest in headings else None


def check(state, root, headings):
    nodes = list(root.iter("node"))
    parents = parents_of(root)
    lines = []
    present = {}
    for node in nodes:
        text = label(node)
        if text in headings and text not in present and not in_row(node, parents):
            present[text] = node
            box = bounds(node)
            if box:
                state["height"][text] = max(state["height"].get(text, 0), box[3] - box[1])
            lines.append(("seen", text))
    focused = next((node for node in nodes if node.get("focused") == "true"), None)
    if focused is None:
        return [("focus", "")] + lines
    names = labels_in(focused)
    key = names[0] if names else ""
    lines.insert(0, ("focus", key))
    view = viewport(focused, parents, root)
    skip = set(focused.iter("node"))
    current = focused
    while current in parents:
        current = parents[current]
        skip.add(current)
    above = heading_above(nodes, focused, set(present.values()), skip)
    if above is not None:
        state["first"][key] = label(above)
    heading = state["first"].get(key)
    if heading:
        node = present.get(heading)
        full = state["height"].get(heading, 0)
        ok = node is not None and fully_visible(node, view, full)
        lines.append(("first", heading, "ok" if ok else "hidden"))
    return lines


def main(argv):
    if len(argv) < 4:
        return 2
    state = load_state(argv[1])
    try:
        root = ET.parse(argv[2]).getroot()
    except (ET.ParseError, OSError):
        return 2
    for line in check(state, root, set(argv[3:])):
        print("\t".join(line))
    save_state(argv[1], state)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

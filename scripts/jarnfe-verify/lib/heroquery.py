#!/usr/bin/env python3
"""Finds the Continue playing card and measures how far its caption runs (#435).

Usage:
  heroquery.py card UI_XML EYEBROW   Prints "left top right bottom focused" for the card.
  heroquery.py ink PNG L T R B DP_H   Prints "end=PX width=PX gap=PX density=D" for the caption.

Compose reports a card's full title to accessibility even when it is drawn with an ellipsis, so
the dump only finds the card. "ink" reads the screenshot instead: per column it compares each
pixel with that column's median, so the art scrim's gradient is not ink. The caption is the run of
inked columns from the left edge, and it ends at the first gap wider than GAP_DP, which is the
empty strip before the cover art. DP_H is the card's height in dp, used to get the density.
"""
import re
import sys
import xml.etree.ElementTree as ET

INK_DELTA = 48
INK_MIN_PIXELS = 2
GAP_DP = 20
# Keeps the focus border, which crosses every column, out of the measurement.
EDGE_INSET_DP = 12
BOUNDS = re.compile(r"\[(-?\d+),(-?\d+)\]\[(-?\d+),(-?\d+)\]")


def label(node):
    return node.get("text") or node.get("content-desc") or ""


def labels_in(node):
    return [text for text in (label(item) for item in node.iter("node")) if text]


def card(root, eyebrow):
    best = None
    for node in root.iter("node"):
        if node.get("clickable") != "true" and node.get("focusable") != "true":
            continue
        if not any(eyebrow in text for text in labels_in(node)):
            continue
        match = BOUNDS.match(node.get("bounds", ""))
        if not match:
            continue
        left, top, right, bottom = map(int, match.groups())
        area = (right - left) * (bottom - top)
        if best is None or area < best[0]:
            best = (area, left, top, right, bottom, node.get("focused") == "true")
    if best is None:
        return 1
    _, left, top, right, bottom, focused = best
    print(left, top, right, bottom, "focused" if focused else "unfocused")
    return 0


def inked_columns(image, left, top, right, bottom):
    gray = image.convert("L").crop((left, top, right, bottom))
    width, height = gray.size
    pixels = gray.load()
    inked = []
    for x in range(width):
        column = sorted(pixels[x, y] for y in range(height))
        median = column[height // 2]
        hits = sum(1 for value in column if abs(value - median) > INK_DELTA)
        inked.append(hits >= INK_MIN_PIXELS)
    return inked


def caption_end(inked, gap):
    start = next((x for x, ink in enumerate(inked) if ink), None)
    if start is None:
        return None
    end, empty = start, 0
    for x in range(start, len(inked)):
        if inked[x]:
            end, empty = x, 0
        else:
            empty += 1
            if empty > gap:
                break
    return end


def ink(path, left, top, right, bottom, height_dp):
    from PIL import Image

    density = (bottom - top) / height_dp
    gap = int(GAP_DP * density)
    inset = int(EDGE_INSET_DP * density)
    inked = inked_columns(Image.open(path), left + inset, top + inset, right - inset, bottom - inset)
    end = caption_end(inked, gap)
    if end is None:
        return 1
    art = next((x for x in range(end + 1, len(inked)) if inked[x]), len(inked))
    print(f"end={end + inset} width={right - left} gap={art - end} density={density:.3f}")
    return 0


def main(argv):
    if len(argv) < 3:
        return 2
    command = argv[1]
    try:
        if command == "card" and len(argv) == 4:
            return card(ET.parse(argv[2]).getroot(), argv[3])
        if command == "ink" and len(argv) == 8:
            left, top, right, bottom = map(int, argv[3:7])
            return ink(argv[2], left, top, right, bottom, float(argv[7]))
    except (ET.ParseError, OSError, ValueError):
        return 2
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))

#!/usr/bin/env python3
"""Helpers for verify 434-jarn-label-handoff.sh.

  434_label.py bounds DUMP.xml TEXT       -> "x1 y1 x2 y2" of the first node showing TEXT
  434_label.py ink SHOT.png x1 y1 x2 y2   -> share of near-white pixels in the box (0..1)
  434_label.py diff A.png B.png x1 y1 x2 y2 -> mean per-pixel difference in the box (0..255)

The box is padded by 32px on each side, so a label sitting 24dp off its rest spot still counts.
"""
import re
import sys
import xml.etree.ElementTree as ET

from PIL import Image, ImageChops, ImageStat

PAD = 32
BRIGHT = 200


def bounds(path, text):
    root = ET.parse(path).getroot()
    for node in root.iter("node"):
        if (node.get("text") or node.get("content-desc")) == text:
            found = re.findall(r"\d+", node.get("bounds") or "")
            if len(found) == 4:
                return " ".join(found)
    return ""


def box(image, coords):
    x1, y1, x2, y2 = (int(value) for value in coords)
    width, height = image.size
    return image.crop((max(0, x1 - PAD), max(0, y1 - PAD), min(width, x2 + PAD), min(height, y2 + PAD)))


def ink(path, coords):
    crop = box(Image.open(path).convert("L"), coords)
    pixels = list(crop.getdata())
    return sum(1 for value in pixels if value >= BRIGHT) / max(1, len(pixels))


def diff(path_a, path_b, coords):
    a = box(Image.open(path_a).convert("L"), coords)
    b = box(Image.open(path_b).convert("L"), coords)
    return ImageStat.Stat(ImageChops.difference(a, b)).mean[0]


def main(argv):
    if len(argv) < 2:
        return 2
    command, args = argv[1], argv[2:]
    if command == "bounds" and len(args) == 2:
        print(bounds(args[0], args[1]))
    elif command == "ink" and len(args) == 5:
        print(f"{ink(args[0], args[1:]):.4f}")
    elif command == "diff" and len(args) == 6:
        print(f"{diff(args[0], args[1], args[2:]):.2f}")
    else:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

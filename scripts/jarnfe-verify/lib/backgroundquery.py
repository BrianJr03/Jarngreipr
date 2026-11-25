#!/usr/bin/env python3
"""Measures how much of a screenshot is plain background. Used by the #437 system background check.

Usage:
  backgroundquery.py flat SHOT           Prints: flat=SHARE color=#rrggbb
  backgroundquery.py changed SHOT_A SHOT_B   Prints: changed=SHARE

The band between the tab row and the game row is cut into a grid. "flat" is the share of cells
whose pixels barely vary and sit close to the frame's most common flat color, which is the theme
background when no art is drawn. Text and the logo only touch a few cells. "changed" is the share
of cells whose average color differs between two frames.
"""
import sys

from PIL import Image, ImageStat

COLUMNS = 32
ROWS = 18
BAND_TOP = 0.12
BAND_BOTTOM = 0.62
FLAT_STDDEV = 6.0
COLOR_DELTA = 14.0
CHANGE_DELTA = 10.0


def band_cells(image):
    width, height = image.size
    top, bottom = int(height * BAND_TOP), int(height * BAND_BOTTOM)
    for row in range(ROWS):
        for column in range(COLUMNS):
            box = (
                column * width // COLUMNS,
                top + row * (bottom - top) // ROWS,
                (column + 1) * width // COLUMNS,
                top + (row + 1) * (bottom - top) // ROWS,
            )
            yield ImageStat.Stat(image.crop(box))


def distance(a, b):
    return max(abs(x - y) for x, y in zip(a, b))


def flat(path):
    stats = list(band_cells(Image.open(path).convert("RGB")))
    smooth = [s.mean for s in stats if max(s.stddev) <= FLAT_STDDEV]
    if not smooth:
        return 0.0, (0, 0, 0)
    buckets = {}
    for mean in smooth:
        key = tuple(int(c) // 8 for c in mean)
        buckets.setdefault(key, []).append(mean)
    common = max(buckets.values(), key=len)
    color = tuple(sum(c[i] for c in common) / len(common) for i in range(3))
    near = sum(1 for mean in smooth if distance(mean, color) <= COLOR_DELTA)
    return near / len(stats), color


def changed(path_a, path_b):
    a = list(band_cells(Image.open(path_a).convert("RGB")))
    b = list(band_cells(Image.open(path_b).convert("RGB")))
    moved = sum(1 for x, y in zip(a, b) if distance(x.mean, y.mean) > CHANGE_DELTA)
    return moved / len(a)


def main(argv):
    if len(argv) == 3 and argv[1] == "flat":
        share, color = flat(argv[2])
        print("flat=%.3f color=#%02x%02x%02x" % ((share,) + tuple(int(c) for c in color)))
    elif len(argv) == 4 and argv[1] == "changed":
        print("changed=%.3f" % changed(argv[2], argv[3]))
    else:
        print(__doc__, file=sys.stderr)
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

#!/usr/bin/env python3
"""Wallpaper motion helpers for verify-376-wallpaper.sh.

Usage:
  motion.py gif OUT         Writes a small looping GIF whose frames differ across the whole image.
  motion.py diff A B        Prints: changed=N total=N for two screenshots of the same display.

"diff" cuts the frame into a grid and counts the cells where enough pixels changed. Wallpaper
motion is faint (a twinkling star is a few pixels), so a cell counts once a handful of its pixels moved.
The top strip (status and clock) is ignored.
"""
import sys

from PIL import Image, ImageChops

COLUMNS = 32
ROWS = 18
TOP_STRIP_ROWS = 2
PIXEL_DELTA = 6
CELL_MIN_PIXELS = 8

GIF_SIZE = (320, 180)
# A 4 second loop, so two shots taken anywhere from 0.1 to 3.9 seconds apart never land on the
# same frame.
GIF_FRAMES = 40
GIF_FRAME_MS = 100


def make_gif(path):
    frames = []
    for index in range(GIF_FRAMES):
        hue = index * 255 // GIF_FRAMES
        frame = Image.new("HSV", GIF_SIZE, (hue, 255, 255)).convert("RGB")
        band = Image.new("RGB", (GIF_SIZE[0] // GIF_FRAMES, GIF_SIZE[1]), (255, 255, 255))
        frame.paste(band, (index * GIF_SIZE[0] // GIF_FRAMES, 0))
        frames.append(frame)
    frames[0].save(path, save_all=True, append_images=frames[1:], duration=GIF_FRAME_MS, loop=0)


def changed_cells(first, second):
    a = Image.open(first).convert("L")
    b = Image.open(second).convert("L")
    if a.size != b.size:
        return None
    moved = ImageChops.difference(a, b).point(lambda value: 255 if value > PIXEL_DELTA else 0)
    width, height = moved.size
    changed = 0
    total = 0
    for row in range(TOP_STRIP_ROWS, ROWS):
        for column in range(COLUMNS):
            box = (
                column * width // COLUMNS,
                row * height // ROWS,
                (column + 1) * width // COLUMNS,
                (row + 1) * height // ROWS,
            )
            cell = moved.crop(box)
            moved_pixels = cell.histogram()[255]
            total += 1
            if moved_pixels >= CELL_MIN_PIXELS:
                changed += 1
    return changed, total


def main(argv):
    if len(argv) == 3 and argv[1] == "gif":
        make_gif(argv[2])
        return 0
    if len(argv) == 4 and argv[1] == "diff":
        result = changed_cells(argv[2], argv[3])
        if result is None:
            print("changed=0 total=0")
            return 1
        print("changed=%d total=%d" % result)
        return 0
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv))

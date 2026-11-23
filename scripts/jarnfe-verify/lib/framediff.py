#!/usr/bin/env python3
"""Compares screenshots while ignoring whatever moves on its own (wallpaper, clock).

Usage: framediff.py IDLE1 IDLE2 IDLE3 AFTER
Prints: changed=N usable=N total=N ring=0|1

The frame is cut into a grid. A cell that differs between any idle frames is masked, along with
its neighbors, and the top strip (status and clock) is always masked. "changed" counts the
remaining cells that differ between the last idle frame and AFTER.

"ring" is 1 when the change from the last idle frame to AFTER contains a focus ring: long, thin,
high-contrast strokes in both directions. Wallpaper motion is too faint to qualify, and content
swapping under a ring (new art or text) is filled rather than thin.
"""
import sys

from PIL import Image, ImageChops, ImageStat

COLUMNS = 48
ROWS = 27
TOP_STRIP_ROWS = 3
CELL_DELTA = 4.0
RING_DELTA = 60
RING_MIN_RUN = 40
RING_MAX_THICKNESS = 8
RING_THIN_SHARE = 0.8


def cells(image):
    width, height = image.size
    grid = []
    for row in range(ROWS):
        for column in range(COLUMNS):
            box = (
                column * width // COLUMNS,
                row * height // ROWS,
                (column + 1) * width // COLUMNS,
                (row + 1) * height // ROWS,
            )
            grid.append(ImageStat.Stat(image.crop(box)).mean)
    return grid


def differs(first, second):
    return max(abs(a - b) for a, b in zip(first, second)) > CELL_DELTA


def neighbors(index):
    row, column = divmod(index, COLUMNS)
    for next_row in range(max(row - 1, 0), min(row + 2, ROWS)):
        for next_column in range(max(column - 1, 0), min(column + 2, COLUMNS)):
            yield next_row * COLUMNS + next_column


def noisy_cells(idle_frames):
    noisy = set(range(TOP_STRIP_ROWS * COLUMNS))
    for earlier, later in zip(idle_frames, idle_frames[1:]):
        for index, (first, second) in enumerate(zip(earlier, later)):
            if differs(first, second):
                noisy.update(neighbors(index))
    return noisy


def change_mask(before, after):
    size = (before.width // 2, before.height // 2)
    delta = ImageChops.difference(
        before.resize(size, Image.NEAREST), after.resize(size, Image.NEAREST)
    )
    red, green, blue = delta.split()
    strongest = ImageChops.lighter(ImageChops.lighter(red, green), blue)
    return strongest.point(lambda value: 255 if value > RING_DELTA else 0)


def is_thin_run(pixels, start, end, y, height):
    above = y - RING_MAX_THICKNESS
    below = y + RING_MAX_THICKNESS
    if above < 0 or below >= height:
        return False
    thin = sum(1 for x in range(start, end) if not pixels[x, above] and not pixels[x, below])
    return thin >= RING_THIN_SHARE * (end - start)


def has_stroke(mask):
    width, height = mask.size
    pixels = mask.load()
    for y in range(height):
        x = 0
        while x < width:
            if not pixels[x, y]:
                x += 1
                continue
            start = x
            while x < width and pixels[x, y]:
                x += 1
            if x - start >= RING_MIN_RUN and is_thin_run(pixels, start, x, y, height):
                return True
    return False


def has_ring(before, after):
    if before.size != after.size:
        return False
    mask = change_mask(before, after)
    return has_stroke(mask) and has_stroke(mask.transpose(Image.Transpose.TRANSPOSE))


def load(path):
    with Image.open(path) as image:
        return image.convert("RGB")


def main(argv):
    if len(argv) != 5:
        print("usage: framediff.py IDLE1 IDLE2 IDLE3 AFTER", file=sys.stderr)
        return 2
    try:
        images = [load(path) for path in argv[1:]]
    except OSError as error:
        print(f"unreadable frame: {error}", file=sys.stderr)
        return 2
    if any(image.size != images[0].size for image in images):
        return 2
    frames = [cells(image) for image in images]
    idle, after = frames[:3], frames[3]
    noisy = noisy_cells(idle)
    total = COLUMNS * ROWS
    changed = sum(
        1
        for index in range(total)
        if index not in noisy and differs(idle[-1][index], after[index])
    )
    ring = int(has_ring(images[2], images[3]))
    print(f"changed={changed} usable={total - len(noisy)} total={total} ring={ring}")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))

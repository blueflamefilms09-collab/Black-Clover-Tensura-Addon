#!/usr/bin/env python3
"""
Pictures of the plaza template for `gen_mega_hub.py --preview` (needs Pillow): a top-down plan and an isometric view, coloured by block id.

Why our own and not `mega_preview.py`: that tool shades by the Mega-Dungeon's floor row and has no colours for flowers, wool, water or signs, so
a paved square with a fountain and four stalls comes out as one grey blur. The point of a picture is to judge shape, pattern, colour balance and
clutter before anything is in the game, so every block the plaza uses has a colour here (the table is checked: an unknown block is a loud grey).
"""
import math
import os
import re
import sys

from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)

EXACT = {
    "stone_bricks": (124, 124, 124), "cracked_stone_bricks": (112, 112, 112), "mossy_stone_bricks": (108, 128, 98),
    "chiseled_stone_bricks": (118, 118, 124), "polished_andesite": (156, 156, 154), "andesite": (136, 137, 135), "smooth_stone": (168, 168, 168),
    "stone_brick_stairs": (128, 128, 128), "stone_brick_slab": (130, 130, 130), "stone_brick_wall": (126, 126, 126), "dirt": (134, 96, 67),
    "grass_block": (92, 160, 58), "short_grass": (80, 150, 50), "water": (46, 92, 210), "sea_lantern": (196, 232, 224),
    "light_blue_stained_glass": (132, 176, 236), "lantern": (250, 196, 84), "barrel": (122, 92, 52), "flower_pot": (160, 90, 70),
    "labyrinth_gate": (70, 120, 255), "poppy": (206, 40, 40), "dandelion": (244, 222, 58), "cornflower": (80, 104, 224),
    "azure_bluet": (232, 232, 238), "oxeye_daisy": (240, 240, 235), "allium": (176, 100, 204), "red_tulip": (210, 50, 40), "pink_tulip": (240, 150, 190),
    "white_tulip": (238, 238, 240), "orange_tulip": (238, 140, 40), "lily_of_the_valley": (240, 245, 240), "rose_bush": (190, 40, 50),
    "peony": (236, 160, 200), "lilac": (190, 150, 220), "fern": (70, 130, 50), "tall_grass": (80, 150, 50), "large_fern": (70, 130, 50),
}
SUFFIX = [
    (r".*_wool|.*_carpet|.*_wall_banner|.*_banner", None),
    (r"spruce_.*|stripped_spruce_.*", (104, 76, 46)),
    (r".*_sign", (160, 128, 84)),
    (r"potted_.*", (190, 80, 70)),
    (r".*_leaves", (60, 130, 50)),
]
WOOL = {"red": (180, 44, 40), "blue": (50, 70, 170), "yellow": (232, 200, 50), "green": (70, 150, 50), "white": (238, 238, 238), "black": (28, 28, 32)}


def color_of(block):
    path = block.split(":", 1)[1] if ":" in block else block
    if path in EXACT:
        return EXACT[path] + (255,) if len(EXACT[path]) == 3 else EXACT[path]
    for pat, col in SUFFIX:
        if re.fullmatch(pat, path):
            if col is None:
                key = path.split("_", 1)[0]
                return WOOL.get(key, (200, 200, 200)) + (255,)
            return col + (255,)
    return (255, 0, 255, 255)           # unknown: loud


def shade(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)), c[3] if len(c) > 3 else 255)


def top_view(t, out, scale=16):
    sx, sy, sz = t.size
    img = Image.new("RGBA", (sx * scale, sz * scale), (40, 80, 40, 255))
    d = ImageDraw.Draw(img)
    best = {}
    for (x, y, z), c in t.cells.items():
        if c.block == "minecraft:air":
            continue
        if (x, z) not in best or y > best[(x, z)][0]:
            best[(x, z)] = (y, c)
    for (x, z), (y, c) in best.items():
        col = shade(color_of(c.block), 0.62 + 0.38 * min(1.0, y / 9.0))
        d.rectangle([x * scale, z * scale, (x + 1) * scale - 1, (z + 1) * scale - 1], fill=col)
    img.save(out)


def _rot(x, z, size, k):
    """Turn a cell a quarter turn k times about the middle of the footprint (so the plaza can be looked at from all four sides)."""
    for _ in range(k % 4):
        x, z = size - 1 - z, x
    return x, z


def iso_view(t, out, scale=9, rot=0, crop=None):
    sx_, _, sz_ = t.size
    cells = {}
    for (x, y, z), c in t.cells.items():
        if c.block == "minecraft:air":
            continue
        nx, nz = _rot(x, z, sx_, rot)
        cells[(nx, y, nz)] = c
    if crop:
        x1, x2, z1, z2 = crop
        cells = {p: c for p, c in cells.items() if x1 <= p[0] <= x2 and z1 <= p[2] <= z2}
    tw, th, vh = 2 * scale, scale, scale
    xs = [(x - z) * tw // 2 for (x, y, z) in cells] or [0]
    ys = [(x + z) * th // 2 - y * vh for (x, y, z) in cells] or [0]
    ox, oy = -min(xs) + tw, -min(ys) + vh * 2
    img = Image.new("RGBA", (max(xs) - min(xs) + tw * 2 + 2, max(ys) - min(ys) + vh * 3 + th + 4), (150, 190, 240, 255))
    d = ImageDraw.Draw(img, "RGBA")
    for (x, y, z) in sorted(cells, key=lambda p: (p[0] + p[1] + p[2], p[1])):
        if all((x + dx, y + dy, z + dz) in cells for dx, dy, dz in ((1, 0, 0), (0, 0, 1), (0, 1, 0))):
            continue
        c = color_of(cells[(x, y, z)].block)
        sx = (x - z) * tw // 2 + ox
        sy = (x + z) * th // 2 - y * vh + oy
        top_pts = [(sx, sy - th // 2), (sx + tw // 2, sy), (sx, sy + th // 2), (sx - tw // 2, sy)]
        left_pts = [(sx - tw // 2, sy), (sx, sy + th // 2), (sx, sy + th // 2 + vh), (sx - tw // 2, sy + vh)]
        right_pts = [(sx + tw // 2, sy), (sx, sy + th // 2), (sx, sy + th // 2 + vh), (sx + tw // 2, sy + vh)]
        d.polygon(left_pts, fill=shade(c, 0.74))
        d.polygon(right_pts, fill=shade(c, 0.56))
        d.polygon(top_pts, fill=shade(c, 1.0))
    img.save(out)


def render(t, prefix):
    top_view(t, prefix + "_top.png")
    for k in range(4):
        iso_view(t, "%s_iso%d.png" % (prefix, k), scale=8, rot=k)
    print("wrote %s_top.png and %s_iso0..3.png" % (prefix, prefix))


def elevation(t, out, x1, x2, z1, z2, scale=28, from_south=True, y1=0):
    """A flat front view of a part of the template: looking north (from the south) or south, each column shows the nearest block."""
    sx, sy, sz = t.size
    w, h = (x2 - x1 + 1) * scale, (sy - y1) * scale
    img = Image.new("RGBA", (w, h), (150, 190, 240, 255))
    d = ImageDraw.Draw(img)
    for x in range(x1, x2 + 1):
        for y in range(y1, sy):
            zs = range(z2, z1 - 1, -1) if from_south else range(z1, z2 + 1)
            for z in zs:
                c = t.cells.get((x, y, z))
                if c is not None and c.block != "minecraft:air":
                    col = shade(color_of(c.block), 1.0 - 0.015 * abs((z2 if from_south else z1) - z))
                    px = (x - x1) * scale
                    py = (sy - 1 - y) * scale
                    d.rectangle([px, py, px + scale - 1, py + scale - 1], fill=col, outline=(0, 0, 0, 60))
                    break
    img.save(out)

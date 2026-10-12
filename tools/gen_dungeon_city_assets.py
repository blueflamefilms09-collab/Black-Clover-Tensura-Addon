#!/usr/bin/env python3
"""
Assets of the Golden City (docs/living_dungeon.md section 7): the Aurelian Altar (block textures, blockstate, block and item models) and the
Aurelian Core it gives (item sprite, facet texture, a 3D item model of a gold cage round a crystal). Idempotent: running it again rewrites
the same files.

    python3 tools/gen_dungeon_city_assets.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")

GOLD = (217, 172, 46)
GOLD_HI = (247, 218, 110)
GOLD_LO = (140, 104, 22)
VIOLET_DARK = (36, 29, 46)
VIOLET = (179, 108, 255)
VIOLET_HI = (240, 216, 255)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def noise(n, seed, blur=1):
    rng = np.random.default_rng(seed)
    a = rng.random((n, n))
    for _ in range(blur):
        a = (a + np.roll(a, 1, 0) + np.roll(a, -1, 0) + np.roll(a, 1, 1) + np.roll(a, -1, 1)) / 5.0
    return (a - a.min()) / (a.max() - a.min() + 1e-9)


def shade(color, f):
    return tuple(int(max(0, min(255, c * f))) for c in color)


def altar_side():
    """Black stone with gold bands above and below and an amethyst eye between them."""
    n = 16
    nz = noise(n, 11, 1)
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            px[x, y] = shade(VIOLET_DARK, 0.8 + 0.5 * nz[y, x]) + (255,)
    for x in range(n):
        px[x, 0], px[x, 15] = GOLD_LO + (255,), GOLD_LO + (255,)
        px[x, 1], px[x, 14] = GOLD_HI + (255,), GOLD + (255,)
        px[x, 2], px[x, 13] = GOLD + (255,), GOLD_HI + (255,)
        px[x, 3], px[x, 12] = GOLD_LO + (255,), GOLD_LO + (255,)
    for y in range(n):                                                  # the edges of the block are a little darker
        for x in (0, 15):
            px[x, y] = shade(px[x, y][:3], 0.7) + (255,)
    # the eye: a diamond of amethyst with a bright pupil
    for y in range(5, 11):
        for x in range(4, 12):
            d = abs(x - 7.5) / 3.6 + abs(y - 7.5) / 2.6
            if d <= 1.0:
                px[x, y] = shade(VIOLET, 0.55 + 0.45 * (1 - d)) + (255,)
    for (x, y) in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = VIOLET_HI + (255,)
    for (x, y) in ((6, 7), (9, 8)):
        px[x, y] = (214, 170, 255, 255)
    return img


def altar_top():
    """A gold plate with an inlaid magic circle: a dark ring, an amethyst ring and a bright heart."""
    n = 16
    nz = noise(n, 12, 1)
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            r = math.hypot(x - 7.5, y - 7.5)
            c = shade(GOLD, 0.85 + 0.3 * nz[y, x])
            if x in (0, 15) or y in (0, 15):
                c = GOLD_LO
            elif x in (1, 14) or y in (1, 14):
                c = GOLD_HI
            elif 5.4 <= r < 6.6:
                c = shade(VIOLET_DARK, 1.0)
            elif 3.4 <= r < 4.6:
                c = shade(VIOLET, 0.8 + 0.3 * nz[y, x])
            elif r < 2.0:
                c = VIOLET_HI if r < 1.2 else shade(VIOLET, 1.1)
            px[x, y] = tuple(int(v) for v in c) + (255,)
    for (x, y) in ((7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8)):     # a gem at each point of the compass
        px[x, y] = VIOLET + (255,)
    return img


def core_facet():
    """The faces of the crystal inside the cage: a faceted amethyst, bright from the upper left."""
    n = 16
    nz = noise(n, 13, 1)
    img = Image.new("RGBA", (n, n))
    px = img.load()
    for y in range(n):
        for x in range(n):
            light = 1.15 - 0.55 * ((x + y) / 30.0)
            facet = 0.9 + 0.2 * (((x // 4) + (y // 4)) % 2)
            c = shade(VIOLET, light * facet * (0.9 + 0.2 * nz[y, x]))
            px[x, y] = c + (255,)
    for i in range(16):                                                 # a bright diagonal glint and a gold edge
        px[i, 15 - i] = shade(VIOLET_HI, 0.95) + (255,)
    for i in range(16):
        px[i, 0] = shade(GOLD_HI, 0.9) + (255,)
        px[0, i] = shade(GOLD_HI, 0.9) + (255,)
    return img


def core_sprite():
    """The Aurelian Core as a flat icon: a faceted crystal in a ring of gold, with sparks."""
    n = 16
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    nz = noise(n, 14, 1)
    for y in range(n):
        for x in range(n):
            d = abs(x - 7.5) / 4.4 + abs(y - 7.5) / 6.4                  # a tall diamond
            if d <= 1.0:
                light = 1.2 - 0.6 * ((x + y) / 30.0) - 0.25 * d
                half = 0.85 if x < 8 else 1.0                              # the right half is a facet in shadow
                px[x, y] = shade(VIOLET, light * half * (0.92 + 0.16 * nz[y, x])) + (255,)
    for y in range(4, 8):                                               # the bright upper-left facet
        for x in range(5, 8):
            if px[x, y][3] and abs(x - 7.5) / 4.4 + abs(y - 7.5) / 6.4 < 0.85:
                px[x, y] = shade(VIOLET_HI, 0.95 - 0.04 * (x - 5)) + (255,)
    px[6, 5] = (255, 255, 255, 255)
    # a ring of gold round the middle, in front of the crystal at the bottom and behind it at the top
    for i in range(24):
        a = i / 24.0 * 2 * math.pi
        x, y = int(round(7.5 + 6.2 * math.cos(a))), int(round(8.5 + 2.2 * math.sin(a)))
        if 0 <= x < n and 0 <= y < n and (math.sin(a) > 0 or px[x, y][3] == 0):
            px[x, y] = (GOLD_HI if math.cos(a) < 0 else GOLD) + (255,)
    for (x, y) in ((1, 4), (14, 3), (2, 12), (13, 13)):                   # sparks of mana
        px[x, y] = (255, 240, 170, 255)
    for (x, y) in ((1, 3), (1, 5), (14, 2), (14, 4)):
        px[x, y] = (255, 240, 170, 140)
    # a dark outline
    out = img.copy()
    op = out.load()
    for y in range(n):
        for x in range(n):
            if px[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < n and 0 <= yy < n and px[xx, yy][3] == 255:
                        op[x, y] = (24, 14, 34, 255)
                        break
    return out


def altar_model():
    """A plinth, a column, a broad table of gold, and a crystal turning over it (two crossed cubes)."""
    faces = lambda tex, up="#top", down=None: {f: {"texture": tex} for f in ("north", "south", "east", "west")} | {"up": {"texture": up}, "down": {"texture": down or tex}}
    gem = {f: {"texture": "#gem"} for f in ("north", "south", "east", "west", "up", "down")}
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "nusmp:block/aurelian_altar_side", "side": "nusmp:block/aurelian_altar_side", "top": "nusmp:block/aurelian_altar_top",
                     "base": "minecraft:block/polished_blackstone", "gem": "nusmp:item/aurelian_core_facet"},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 3, 15], "faces": faces("#side", "#top", "#base")},
            {"from": [4, 3, 4], "to": [12, 9, 12], "faces": faces("#side", "#side", "#side")},
            {"from": [1, 9, 1], "to": [15, 12, 15], "faces": faces("#side", "#top", "#base")},
            {"from": [6, 12.2, 6], "to": [10, 15.8, 10], "rotation": {"angle": 45, "axis": "y", "origin": [8, 14, 8]}, "faces": gem},
            {"from": [6, 12.2, 6], "to": [10, 15.8, 10], "rotation": {"angle": 45, "axis": "x", "origin": [8, 14, 8]}, "faces": gem},
        ],
    }


def bar(x1, y1, z1, x2, y2, z2):
    return {"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": {f: {"texture": "#gold"} for f in ("north", "south", "east", "west", "up", "down")}}


def core_model():
    """A cage of gold bars with a faceted crystal (two crossed cubes) floating inside, in a block-style model so it is a real 3D object in hand."""
    gem = {f: {"texture": "#gem"} for f in ("north", "south", "east", "west", "up", "down")}
    elements = [
        bar(2, 2, 2, 14, 3, 3), bar(2, 13, 2, 14, 14, 3), bar(2, 2, 13, 14, 3, 14), bar(2, 13, 13, 14, 14, 14),          # four bars along x
        bar(2, 3, 2, 3, 13, 3), bar(13, 3, 2, 14, 13, 3), bar(2, 3, 13, 3, 13, 14), bar(13, 3, 13, 14, 13, 14),           # four posts
        bar(2, 2, 3, 3, 3, 13), bar(13, 2, 3, 14, 3, 13), bar(2, 13, 3, 3, 14, 13), bar(13, 13, 3, 14, 14, 13),           # four bars along z
        {"from": [5, 5, 5], "to": [11, 11, 11], "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]}, "faces": gem},
        {"from": [5, 5, 5], "to": [11, 11, 11], "rotation": {"angle": 45, "axis": "x", "origin": [8, 8, 8]}, "faces": gem},
    ]
    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": "nusmp:item/aurelian_core", "gold": "minecraft:block/gold_block", "gem": "nusmp:item/aurelian_core_facet"},
        "elements": elements,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.9, 0.9, 0.9]},
            "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.5, 0.5, 0.5]},
            "firstperson_righthand": {"rotation": [0, 30, 0], "translation": [1.2, 3, 1.5], "scale": [0.6, 0.6, 0.6]},
            "firstperson_lefthand": {"rotation": [0, 30, 0], "translation": [1.2, 3, 1.5], "scale": [0.6, 0.6, 0.6]},
            "head": {"rotation": [0, 0, 0], "translation": [0, 13, 0], "scale": [0.8, 0.8, 0.8]},
        },
    }


def main():
    block_tex = os.path.join(ASSETS, "textures", "block")
    item_tex = os.path.join(ASSETS, "textures", "item")
    os.makedirs(block_tex, exist_ok=True)
    os.makedirs(item_tex, exist_ok=True)
    altar_side().save(os.path.join(block_tex, "aurelian_altar_side.png"))
    altar_top().save(os.path.join(block_tex, "aurelian_altar_top.png"))
    core_sprite().save(os.path.join(item_tex, "aurelian_core.png"))
    core_facet().save(os.path.join(item_tex, "aurelian_core_facet.png"))
    write_json(os.path.join(ASSETS, "blockstates", "aurelian_altar.json"), {"variants": {"": {"model": "nusmp:block/aurelian_altar"}}})
    write_json(os.path.join(ASSETS, "models", "block", "aurelian_altar.json"), altar_model())
    write_json(os.path.join(ASSETS, "models", "item", "aurelian_altar.json"), {"parent": "nusmp:block/aurelian_altar"})
    write_json(os.path.join(ASSETS, "models", "item", "aurelian_core.json"), core_model())
    print("done")


if __name__ == "__main__":
    main()

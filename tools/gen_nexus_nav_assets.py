#!/usr/bin/env python3
"""
2D assets of the Dungeon Delving Gear pass (docs/living_dungeon.md sections 3 and 6): the Venom Gland, the first organ a Butcher Cleaver
cuts out of a Basilisk or a spider. A 16x16 sprite in the style of tools/gen_dungeon_assets.py and its item model. (The Ki-Resonance
Lantern and the Anti-Bird Compass are drawn by tools/gen_nexus_relic_icons.py and gen_weapon_models.py.) Idempotent.

    python3 tools/gen_nexus_nav_assets.py
"""
import json
import math
import os
import random

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def clamp(c):
    return tuple(int(max(0, min(255, round(v)))) for v in c)


def lerp(a, b, t):
    return tuple(x + (y - x) * t for x, y in zip(a, b))


def venom_gland():
    """A swollen gland: a glossy green sac with darker veins, a fleshy duct at the top and a bright drop of venom at its tip."""
    n = 16
    rng = random.Random(7)
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    outline = (26, 48, 26)
    deep, mid, light, shine = (52, 104, 38), (104, 170, 50), (166, 214, 78), (234, 250, 168)
    flesh, flesh_dark = (170, 78, 92), (104, 40, 58)
    cx, cy, rx, ry = 7.6, 9.3, 5.2, 5.6                     # the sac
    sac = set()
    for y in range(n):
        for x in range(n):
            dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            # a pear: narrower toward the top where the duct leaves it
            narrow = 1.0 + 0.32 * max(0.0, -dy)
            d = math.hypot(dx * narrow, dy)
            if d <= 1.0:
                sac.add((x, y))
    for (x, y) in sac:                                       # shade: lit from the upper left, rounder toward the middle
        dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
        d = min(1.0, math.hypot(dx, dy))
        light_amount = 0.5 - 0.55 * dx - 0.45 * dy
        t = max(0.0, min(1.0, 0.45 + 0.55 * light_amount - 0.35 * d * d))
        col = lerp(deep, mid, min(1.0, t * 1.6)) if t < 0.62 else lerp(mid, light, (t - 0.62) / 0.38)
        col = tuple(v * (0.93 + 0.14 * rng.random()) for v in col)
        px[x, y] = clamp(col) + (255,)
    for (x, y) in sac:                                       # an outline where the sac meets the air
        if any((x + a, y + b) not in sac for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            px[x, y] = outline + (255,)
    for (x, y) in ((4, 8), (5, 7), (5, 8)):                  # the gloss
        if px[x, y][3]:
            px[x, y] = shine + (255,)
    if px[6, 7][3]:
        px[6, 7] = clamp(lerp(light, shine, 0.5)) + (255,)
    for (x, y) in ((9, 10), (10, 11), (10, 12), (9, 13), (8, 12), (7, 11)):   # veins, darker where the venom is thickest
        if (x, y) in sac and px[x, y][:3] != outline:
            px[x, y] = clamp(lerp(deep, outline, 0.25)) + (255,)
    # the duct: a short fleshy tube from the top of the sac, bending to the right
    duct = [(7, 4), (7, 3), (8, 3), (8, 2), (9, 2), (9, 1)]
    for i, (x, y) in enumerate(duct):
        px[x, y] = clamp(lerp(flesh, flesh_dark, 0.15 + 0.1 * (i % 2))) + (255,)
    for (x, y) in duct:
        for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + a, y + b)
            if 0 <= q[0] < n and 0 <= q[1] < n and px[q][3] == 0:
                px[q] = outline + (255,)
    px[10, 1] = clamp(flesh) + (255,)
    px[10, 2] = outline + (255,)
    # a drop of venom at the bottom, brighter than the gland and glowing a little
    for (x, y, c) in ((11, 13, (206, 240, 96)), (11, 14, (150, 214, 64)), (10, 14, (150, 214, 64)), (12, 14, (150, 214, 64)), (11, 15, (96, 160, 44))):
        px[x, y] = clamp(c) + (255,)
    px[11, 13] = clamp(shine) + (255,)
    return img


def main():
    out = os.path.join(ASSETS, "textures", "item", "venom_gland.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    venom_gland().save(out)
    write_json(os.path.join(ASSETS, "models", "item", "venom_gland.json"),
               {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/venom_gland"}})
    print("wrote", out)


if __name__ == "__main__":
    main()

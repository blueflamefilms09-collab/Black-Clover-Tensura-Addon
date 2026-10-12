#!/usr/bin/env python3
"""
Assets of the Living Dungeon's rewards (docs/living_dungeon.md): the Maw Fruit that cut Maw roots give up (item texture and item model).
16x16 pixel art in the style of tools/gen_dungeon_assets.py. Idempotent: running it again rewrites the same files.

    python3 tools/gen_dungeon_reward_assets.py
"""
import json
import math
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")

OUTLINE = (30, 16, 40, 255)
ROOT_DARK, ROOT_LIGHT = (66, 46, 32), (116, 86, 58)          # the root it hangs from (the Maw Root's browns)
MANA = (95, 227, 192)                                          # the glowing veins of mana in the Maw Root
SHADOW, LIGHT = (44, 18, 88), (156, 92, 220)                   # the fruit's flesh: deep indigo to bright violet


def noise(n, seed, blur=1):
    rng = np.random.default_rng(seed)
    a = rng.random((n, n))
    for _ in range(blur):
        a = (a + np.roll(a, 1, 0) + np.roll(a, -1, 0) + np.roll(a, 1, 1) + np.roll(a, -1, 1)) / 5.0
    return (a - a.min()) / (a.max() - a.min() + 1e-9)


def mix(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def maw_fruit_texture():
    """A round fruit of dense mana hanging from a short length of root: violet flesh lit from the top left, glowing veins, a bright seed core."""
    n = 16
    nz = noise(n, 41, 1)
    img = Image.new("RGBA", (n, n), (0, 0, 0, 0))
    px = img.load()
    cx, cy, rx, ry = 7.6, 9.4, 5.3, 5.0
    inside = {}
    for y in range(n):
        for x in range(n):
            dx, dy = (x - cx) / rx, (y - cy) / ry
            if dx * dx + dy * dy <= 1.0 + 0.16 * (nz[y, x] - 0.5):
                inside[(x, y)] = (dx, dy)
    for (x, y), (dx, dy) in inside.items():
        lit = max(0.0, min(1.0, 0.55 - 0.55 * dx - 0.62 * dy))          # light comes from the top left
        r = math.hypot(dx, dy)
        lit = lit * (1.15 - 0.45 * r) + 0.18 * (1 - r)                   # the rim falls into shadow, the middle glows a little
        lit = max(0.0, min(1.0, lit + 0.16 * (nz[y, x] - 0.5)))
        col = mix(SHADOW, LIGHT, lit)
        px[x, y] = tuple(int(v) for v in col) + (255,)
    for (x, y) in ((4, 11), (5, 12), (6, 13), (7, 13), (11, 12), (12, 11)):   # a warm rim light low on the shadow side
        if (x, y) in inside:
            px[x, y] = (196, 84, 168, 255)
    veins = [((8, 5), (8, 7), (7, 9), (7, 11), (8, 13)),                  # glowing mana veins running down from the root
             ((8, 5), (9, 7), (10, 9), (10, 11)),
             ((8, 5), (7, 6), (5, 8), (4, 10))]
    for path in veins:
        for (x0, y0), (x1, y1) in zip(path, path[1:]):
            steps = max(abs(x1 - x0), abs(y1 - y0))
            for s in range(steps + 1):
                x, y = x0 + round((x1 - x0) * s / max(1, steps)), y0 + round((y1 - y0) * s / max(1, steps))
                if (x, y) in inside:
                    px[x, y] = tuple(int(v) for v in mix((40, 120, 112), MANA, 0.75 if y < 10 else 0.45)) + (255,)
    for (x, y) in ((9, 9), (9, 10)):                                      # the seed core, a bright bead between the veins
        if (x, y) in inside:
            px[x, y] = (226, 255, 248, 255) if (x, y) == (9, 9) else (150, 240, 214, 255)
    for (x, y) in ((4, 7), (5, 6), (4, 8)):                               # a soft highlight
        if (x, y) in inside:
            px[x, y] = (232, 206, 255, 255)
    stem = [(8, 4, ROOT_LIGHT), (8, 3, ROOT_DARK), (9, 2, ROOT_LIGHT), (9, 1, ROOT_DARK), (10, 1, ROOT_LIGHT), (7, 4, ROOT_DARK), (9, 4, ROOT_DARK)]
    for (x, y, c) in stem:                                                 # a stub of the Maw Root it grew on
        px[x, y] = c + (255,)
    px[10, 2] = MANA + (255,)                                              # mana still beads at the cut
    out = img.copy()
    op = out.load()
    for y in range(n):                                                     # a dark outline, as on the other dungeon foods
        for x in range(n):
            if px[x, y][3] == 0:
                for ddx, ddy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + ddx, y + ddy
                    if 0 <= xx < n and 0 <= yy < n and px[xx, yy][3] > 0:
                        op[x, y] = OUTLINE
    return out


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


def main():
    tex = os.path.join(ASSETS, "textures", "item")
    os.makedirs(tex, exist_ok=True)
    maw_fruit_texture().save(os.path.join(tex, "maw_fruit.png"))
    write_json(os.path.join(ASSETS, "models", "item", "maw_fruit.json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/maw_fruit"}})
    print("done")


if __name__ == "__main__":
    main()

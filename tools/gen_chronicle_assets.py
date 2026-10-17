"""1.61.0: the 16x16 art of the Chronicle Sigil (nusmp:chronicle_sigil). Deterministic.

    python tools/gen_chronicle_assets.py

Writes
  assets/nusmp/textures/item/chronicle_sigil.png      the inventory icon: a gold-rimmed seal of lapis with an open book and a spark above it, hung from a bail
                                                      (the in-hand 3D model is built from it by tools/gen_weapon_models.py, which places the mesh on this sprite's box)
  assets/nusmp/models/item/chronicle_sigil.json       the in-hand model: builtin/entity (the mesh is drawn by client/WeaponRenderer) with the hand-item display transforms
  assets/nusmp/models/item/chronicle_sigil_icon.json  the flat model the inventory, the tab and a frame use (the sprite above)
Run this BEFORE `python tools/gen_weapon_models.py chronicle_sigil`.
"""
import json
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

GOLD = (222, 170, 52)
GOLD_HI = (255, 226, 130)
GOLD_LO = (120, 78, 22)
LAPIS = (34, 54, 138)
LAPIS_HI = (60, 90, 190)
PAGE = (238, 228, 198)
PAGE_LO = (196, 184, 150)
SPINE = (92, 62, 26)
INK = (118, 96, 60)


def save(path, arr):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(arr.astype(np.uint8), "RGBA").save(path, optimize=True)


def sigil():
    img = np.zeros((16, 16, 4), np.float32)

    def px(x, y, c, a=255):
        if 0 <= x < 16 and 0 <= y < 16:
            img[y, x] = (*c, a)

    cx, cy, r_out, r_in = 7.5, 8.5, 6.9, 5.4
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - (cx + 0.5), y + 0.5 - (cy + 0.5))
            if d <= r_out:
                if d > r_in:                                                   # the gold rim, lit from the upper left
                    lit = (x + y) < 15
                    px(x, y, GOLD_HI if (lit and (x + y) % 3 == 0) else GOLD if lit else GOLD_LO)
                else:                                                          # the lapis field with a little grain
                    grain = (x * 7 + y * 13) % 5 == 0
                    px(x, y, LAPIS_HI if grain else LAPIS)
    # the bail: a small gold ring above the seal
    for (x, y) in [(7, 0), (8, 0), (6, 1), (9, 1)]:
        px(x, y, GOLD)
    px(7, 1, GOLD_LO)
    px(8, 1, GOLD_LO)
    # the spark above the book
    for (x, y) in [(7, 4), (8, 4)]:
        px(x, y, GOLD_HI)
    px(7, 3, GOLD, 220)
    px(8, 3, GOLD, 220)
    # the open book: two pages and a dark spine, three lines of ink
    rows = [".####.####.", "#####.#####", "#####.#####", "#####.#####", "#####.#####", ".####.####.", "..###.###.."]
    x0, y0 = 3, 6
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch != "#":
                continue
            if i == 5:
                px(x0 + i, y0 + j, SPINE)
            elif j in (2, 4) and i % 2 == 0:
                px(x0 + i, y0 + j, INK)
            else:
                px(x0 + i, y0 + j, PAGE if i < 5 else PAGE_LO)
    save(os.path.join(ASSETS, "textures", "item", "chronicle_sigil.png"), img)


def models():
    display = {
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
        "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    }
    hand = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/chronicle_sigil"}, "display": display}
    icon = {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/chronicle_sigil"}}
    for name, body in (("chronicle_sigil", hand), ("chronicle_sigil_icon", icon)):
        path = os.path.join(ASSETS, "models", "item", name + ".json")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", newline="\n") as f:
            json.dump(body, f, indent=2)


if __name__ == "__main__":
    sigil()
    models()
    print("wrote chronicle_sigil.png, chronicle_sigil.json, chronicle_sigil_icon.json")

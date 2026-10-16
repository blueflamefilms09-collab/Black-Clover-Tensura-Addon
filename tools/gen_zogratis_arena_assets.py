"""1.46.0: the 16x16 art of the Zogratis arena. Deterministic.

    python tools/gen_zogratis_arena_assets.py

Writes
  assets/nusmp/textures/item/zogratis_hourglass.png          the Hourglass of the Final Hour (the inventory icon; the in-hand 3D model is
                                                              built from it by tools/gen_weapon_models.py, which places the mesh on this sprite's box)
  assets/nusmp/textures/block/arena_return_sigil_top.png     the Return Sigil, top face: a gold ring and a rune on black stone
  assets/nusmp/textures/block/arena_return_sigil_side.png    its sides: black bricks with a gold seam
Run this BEFORE `python tools/gen_weapon_models.py zogratis_hourglass`.
"""
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures")

GOLD = (222, 170, 52)
GOLD_HI = (255, 226, 130)
GOLD_LO = (120, 78, 22)
GLASS = (176, 208, 238)
GLASS_EDGE = (110, 140, 190)
SAND = (238, 196, 84)
SAND_HI = (255, 230, 150)
VIOLET = (150, 90, 220)


def rng(seed):
    return np.random.RandomState(seed)


def save(path, arr):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(arr.astype(np.uint8), "RGBA").save(path, optimize=True)


def hourglass():
    img = np.zeros((16, 16, 4), np.float32)

    def px(x, y, c, a=255):
        if 0 <= x < 16 and 0 <= y < 16:
            img[y, x] = (*c, a)

    # frame plates, top and bottom
    for x in range(2, 14):
        px(x, 1, GOLD_HI if x % 3 == 0 else GOLD)
        px(x, 2, GOLD)
        px(x, 13, GOLD)
        px(x, 14, GOLD_LO)
    for x in range(2, 14):
        px(x, 0, GOLD_LO, 0)
    # posts
    for y in range(3, 13):
        px(2, y, GOLD_LO)
        px(13, y, GOLD_LO)
        px(3, y, GOLD, 255 if y % 2 == 0 else 220)
        px(12, y, GOLD, 255 if y % 2 == 0 else 220)
    # glass bulbs: half widths from top to bottom (the neck is at y = 7 and 8)
    half = {3: 4, 4: 4, 5: 3, 6: 2, 7: 1, 8: 1, 9: 2, 10: 3, 11: 4, 12: 4}
    for y, h in half.items():
        for x in range(8 - h, 8 + h):
            edge = x in (8 - h, 8 + h - 1)
            px(x, y, GLASS_EDGE if edge else GLASS, 255 if edge else 190)
    # sand: a little in the upper bulb, a heap in the lower one, a thread through the neck
    for (x, y) in [(6, 5), (7, 5), (8, 5), (9, 5), (7, 6), (8, 6)]:
        px(x, y, SAND if (x + y) % 2 else SAND_HI)
    px(7, 7, SAND)
    px(8, 8, SAND_HI)
    for (x, y) in [(7, 9), (8, 9), (6, 10), (7, 10), (8, 10), (9, 10), (5, 11), (6, 11), (7, 11), (8, 11), (9, 11), (10, 11),
                   (5, 12), (6, 12), (7, 12), (8, 12), (9, 12), (10, 12)]:
        px(x, y, SAND if (x + y) % 3 else SAND_HI)
    # a violet gem on the top plate, the Zogratis mark
    px(7, 1, VIOLET)
    px(8, 1, VIOLET)
    px(7, 0, VIOLET, 200)
    px(8, 0, VIOLET, 200)
    save(os.path.join(ASSETS, "item", "zogratis_hourglass.png"), img)


def block_textures():
    r = rng(46)
    top = np.zeros((16, 16, 4), np.float32)
    side = np.zeros((16, 16, 4), np.float32)
    for y in range(16):
        for x in range(16):
            n = r.randint(-6, 7)
            base = np.array((26, 24, 34), np.float32) + n
            top[y, x] = (*base, 255)
            sb = np.array((30, 28, 38), np.float32) + n
            side[y, x] = (*sb, 255)
    # side: brick seams and one gold line
    for x in range(16):
        side[7, x] = (*GOLD_LO, 255)
        side[15, x] = (*GOLD_LO, 255)
        side[8, x] = (*GOLD, 255) if x % 4 else (*GOLD_HI, 255)
    for y in range(0, 7):
        side[y, 0] = (14, 12, 20, 255)
        side[y, 8] = (14, 12, 20, 255)
    for y in range(8, 15):
        side[y, 4] = (14, 12, 20, 255)
        side[y, 12] = (14, 12, 20, 255)
    # top: a gold ring and a four-point rune
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if abs(d - 6.0) < 0.75:
                top[y, x] = (*(GOLD_HI if (x + y) % 4 == 0 else GOLD), 255)
            elif d < 2.6 and (abs(x - 7.5) < 0.8 or abs(y - 7.5) < 0.8 or d < 1.6):
                top[y, x] = (*VIOLET, 255)
    for k in range(4):
        a = k * math.pi / 2
        for t in np.linspace(2.0, 5.0, 8):
            x, y = int(round(7.5 + math.cos(a) * t)), int(round(7.5 + math.sin(a) * t))
            top[y, x] = (*GOLD_LO, 255) if t < 3.4 else (*GOLD, 255)
    save(os.path.join(ASSETS, "block", "arena_return_sigil_top.png"), top)
    save(os.path.join(ASSETS, "block", "arena_return_sigil_side.png"), side)


if __name__ == "__main__":
    hourglass()
    block_textures()
    print("wrote zogratis_hourglass.png, arena_return_sigil_top.png, arena_return_sigil_side.png")

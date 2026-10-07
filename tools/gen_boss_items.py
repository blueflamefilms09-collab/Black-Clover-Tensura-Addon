"""0.52: inventory sprites for Zagred's drops (the 3D in-hand models come from tools/gen_weapon_models.py).

    python tools/gen_boss_items.py

  textures/item/last_word.png                    32x32  the quill-blade, on the same diagonal as the other swords
  textures/item/shroud_of_margins.png            16x16
  textures/item/circlet_of_quickened_thought.png 16x16
  textures/item/heart_of_words.png               16x16
"""
import math
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item")


def save(arr, name):
    Image.fromarray(arr.astype(np.uint8), "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name)


def last_word():
    S = 32
    a = np.zeros((S, S, 4), np.float32)
    x0, y0 = 4.0, 28.0
    rng = np.random.default_rng(520)
    for y in range(S):
        for x in range(S):
            px, py = x + 0.5 - x0, y + 0.5 - y0
            u = (px - py) / math.sqrt(2)              # along the diagonal (up and right)
            v = (px + py) / math.sqrt(2)              # across (positive = lower right, the edge side)
            c = None
            if 0 <= u < 2.4 and abs(v) < 1.7:
                c = (200, 40, 60) if abs(v) < 0.7 and 0.6 < u < 1.8 else (190, 150, 60)          # pommel with a gem
            elif 2.4 <= u < 9.5 and abs(v) < 1.15:
                c = (30, 24, 34) if int(u * 1.4) % 2 else (58, 40, 48)                           # wrapped grip
            elif 9.5 <= u < 11.2 and abs(v) < 4.4:
                c = (214, 176, 70) if abs(v) > 1.4 else (200, 30, 54)                            # a swept guard, crimson at the heart
            elif 11.2 <= u < 33.5:
                hw = 1.9 * (1 - (u - 11.2) / 22.3 * 0.8) + 0.25
                if abs(v) <= hw:
                    c = (34, 28, 42)
                    if abs(v) < 0.55 and u < 31:
                        c = (214, 34, 56)                                                       # the red text in the fuller
                    elif abs(abs(v) - hw) < 0.55:
                        c = (96, 90, 112)                                                       # the edge catching light
                elif v > 0 and hw < v < hw + 1.15 and int(u) % 3 == 0 and u < 28:
                    c = (24, 20, 32)                                                            # a feather barb
            if c is not None:
                n = rng.integers(-6, 7)
                a[y, x] = (*np.clip(np.array(c) + n, 0, 255), 255)
    # a white spark at the tip
    a[5, 27] = (240, 236, 255, 255)
    save(a, "last_word")


def shroud():
    S = 16
    a = np.zeros((S, S, 4), np.float32)
    rng = np.random.default_rng(521)
    for y in range(2, 15):
        half = 3 + (y - 2) * 0.35
        for x in range(S):
            d = abs(x + 0.5 - 8)
            if d < half:
                c = np.array((38, 26, 62), np.float32)
                if d > half - 1.0:
                    c = np.array((206, 196, 232), np.float32)                                    # the white margin
                elif y > 5 and 3 < (x + y) % 6 and y % 2 == 0 and d < half - 2:
                    c = np.array((150, 120, 220), np.float32)                                    # lines of writing
                if y == 14 and (x % 2 == 0):
                    continue                                                                     # a ragged hem
                a[y, x] = (*np.clip(c + rng.integers(-8, 9), 0, 255), 255)
    for x in range(5, 11):                                                                       # the hood
        a[1, x] = (24, 16, 44, 255)
    for x in (7, 8):
        a[4, x] = (230, 220, 255, 255)                                                           # the clasp
    save(a, "shroud_of_margins")


def circlet():
    S = 16
    a = np.zeros((S, S, 4), np.float32)
    for y in range(S):
        for x in range(S):
            dx, dy = (x + 0.5 - 8) / 6.5, (y + 0.5 - 9) / 3.6
            d = math.hypot(dx, dy)
            if 0.72 < d < 1.0 and y >= 6 - (1 if abs(dx) < 0.2 else 0):
                a[y, x] = (214, 176, 70, 255) if d > 0.86 else (150, 118, 40, 255)
    for (x, y) in ((8, 5), (7, 6), (8, 6), (9, 6), (8, 7)):
        a[y, x] = (110, 230, 250, 255)                                                           # the cyan stone
    a[5, 8] = (230, 255, 255, 255)
    for (x, y) in ((2, 6), (1, 5), (14, 6), (15, 5), (3, 7), (13, 7)):
        a[y, x] = (240, 232, 200, 255)                                                           # small swept wings
    save(a, "circlet_of_quickened_thought")


def heart():
    S = 16
    a = np.zeros((S, S, 4), np.float32)
    for y in range(S):
        for x in range(S):
            nx, ny = (x + 0.5 - 8) / 6.2, -(y + 0.5 - 8.5) / 6.2 + 0.1
            if nx * nx + (ny - math.sqrt(abs(nx)) * 0.75) ** 2 * 1.6 < 0.62:
                c = np.array((120, 20, 60), np.float32)
                if nx * nx + ny * ny < 0.25:
                    c = np.array((190, 40, 90), np.float32)
                a[y, x] = (*c, 255)
    for (x, y) in ((6, 6), (7, 6), (9, 6), (8, 7), (7, 8), (8, 9), (9, 9)):
        a[y, x] = (255, 200, 255, 255)                                                           # glyph strokes
    save(a, "heart_of_words")


if __name__ == "__main__":
    last_word()
    shroud()
    circlet()
    heart()

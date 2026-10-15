"""Generates the four 16x16 block textures of the Relic Forge (relic_forge_side/plate/top/bottom.png).

Deterministic, Pillow only. Output: src/main/resources/assets/nusmp/textures/block/

    python3 -B tools/gen_forge_textures.py

Look: dark gunmetal body with riveted gold trim (side / plate), and a top face of the same metal with a glowing violet rune circle in the
centre (the rune is what the player reads as "this table does magic"). Why: the block is a smithing-table-like body with an anvil-like top plate,
so it needs a body face, a trim face for the plate's edge, a top and a bottom.
"""
import os
import random
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "block")
METAL = [(34, 36, 46), (40, 42, 54), (46, 48, 62), (28, 30, 38)]
GOLD = [(232, 196, 104), (200, 160, 70), (150, 114, 46)]
VIOLET = [(154, 124, 255), (190, 160, 255), (235, 225, 255)]


def base(seed):
    rnd = random.Random(seed)
    im = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), METAL[rnd.randrange(4)])
    return im


def frame(im, col, dark):
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            im.putpixel(p, col)
        for p in ((i, 1), (i, 14), (1, i), (14, i)):
            if 1 < i < 14:
                im.putpixel(p, dark)


def rivets(im, col):
    for p in ((2, 2), (13, 2), (2, 13), (13, 13)):
        im.putpixel(p, col)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"))
    print("wrote", name)


def side():
    im = base(1)
    frame(im, GOLD[1], METAL[3])
    rivets(im, GOLD[0])
    for x in range(4, 12):                       # a little gold inlay bar: the smith's mark
        im.putpixel((x, 8), GOLD[2])
    im.putpixel((8, 7), GOLD[0])
    im.putpixel((8, 9), GOLD[0])
    return im


def plate():
    im = base(2)
    for x in range(16):
        im.putpixel((x, 0), GOLD[0])
        im.putpixel((x, 1), GOLD[1])
        im.putpixel((x, 14), GOLD[2])
        im.putpixel((x, 15), GOLD[2])
    return im


def top():
    im = base(3)
    frame(im, GOLD[1], METAL[3])
    rivets(im, GOLD[0])
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if 4.6 <= d <= 5.5:                  # outer rune ring
                im.putpixel((x, y), VIOLET[0])
            elif d < 1.6:                        # bright core
                im.putpixel((x, y), VIOLET[2])
    for k in range(-3, 4):                       # the cross strokes of the rune
        for p in ((8 + k, 8), (7 - k, 7)):
            if 2 <= p[0] <= 13:
                im.putpixel(p, VIOLET[1])
        for p in ((8, 8 + k), (7, 7 - k)):
            if 2 <= p[1] <= 13:
                im.putpixel(p, VIOLET[1])
    im.putpixel((8, 8), VIOLET[2])
    im.putpixel((7, 7), VIOLET[2])
    return im


def bottom():
    im = base(4)
    frame(im, METAL[3], METAL[0])
    return im


if __name__ == "__main__":
    save(side(), "relic_forge_side")
    save(plate(), "relic_forge_plate")
    save(top(), "relic_forge_top")
    save(bottom(), "relic_forge_bottom")

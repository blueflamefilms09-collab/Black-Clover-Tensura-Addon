"""Elsdocia's 16x16 inventory sprite.

1.29.0 look (default): the owner's reference render, a broad blade of faceted blue crystal with white-hot cells, a short bronze ricasso with a dark
club / clover cut-out under the guard, a plain dark-gold cross guard with small flared ends, a short slim dark grip and a round gold pommel with a
blue gem, on the same diagonal as every other weapon icon (hilt bottom left, tip top right: the mesh is placed on this diagonal).
1.2.0 look (kept, rule 2):   python3 -B tools/gen_elsdocia_icon.py --legacy   writes textures/item/elsdocia_legacy.png
The 0.99 sprite stays as textures/item/elsdocia_v099.png.   Run: python3 -B tools/gen_elsdocia_icon.py
"""
import os
import sys
from PIL import Image

ITEM_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item")
NAVY, DEEP, MID, LIGHT, ICE, WHITE = (12, 26, 82), (28, 70, 190), (52, 128, 238), (110, 204, 255), (190, 236, 255), (246, 252, 255)
GOLD, GOLD_HI, GOLD_LO, GRIP = (226, 178, 62), (255, 226, 120), (150, 104, 30), (30, 34, 52)
BRONZE, BRONZE_HI, BRONZE_LO, BRONZE_DK = (176, 128, 58), (222, 176, 96), (112, 76, 34), (70, 48, 26)
GRIP_BROWN, GRIP_HI = (58, 44, 34), (96, 76, 54)
CLOVER = (22, 16, 40)


def new_image():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), c + (255,))


def hsh(*a):
    h = 2166136261
    for v in a:
        h = ((h ^ (v & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
    return h


def new_look():
    """t runs along the sword (0 pommel ... 30 tip), p across it (0 = the centre line of the diagonal)."""
    img = new_image()
    for y in range(16):
        for x in range(16):
            t, p = x + (15 - y), x - (15 - y)
            ap = abs(p)
            if t in (0, 1) and ap <= 1:                                  # round pommel, blue gem in the middle
                put(img, x, y, LIGHT if (t == 1 and p == 0) else (GOLD_LO if t == 0 else GOLD))
            elif t == 2 and ap <= 1:
                put(img, x, y, GOLD_HI if p == 0 else GOLD_LO)
            elif 3 <= t <= 6 and p == 0:                                 # short slim grip
                put(img, x, y, GRIP_HI if t == 4 else GRIP_BROWN)
            elif (t == 8 and ap <= 6) or (t == 7 and ap <= 1) or (t == 9 and ap in (5, 6)):   # plain dark-gold cross guard, flared ends
                c = BRONZE_HI if (t == 8 and ap <= 2) else BRONZE
                if ap >= 5:
                    c = GOLD
                put(img, x, y, c)
            elif t >= 9 and ap <= (4 if t < 26 else 2 if t < 29 else 0):
                edge = ap == (4 if t < 26 else 2 if t < 29 else 0)
                if t < 15:                                               # bronze ricasso with the dark club cut-out
                    c = BRONZE_LO if edge else (BRONZE_HI if ap == 3 and t == 14 else BRONZE)
                    if (t, ap) in ((12, 0), (11, 1), (13, 1)):
                        c = CLOVER
                    put(img, x, y, c)
                elif edge:
                    put(img, x, y, NAVY)
                else:                                                    # fractured crystal: a tone per facet cell, light seams, white-hot cells
                    cell = hsh(t // 3, (p + 8) // 3, 5) % 5
                    c = (MID, LIGHT, DEEP, MID, ICE)[cell]
                    if abs(p) <= 1 and (t + p) % 4 == 0:
                        c = ICE
                    if (t % 3 == 0 and ap < 3) and cell in (0, 2):
                        c = DEEP if c != ICE else c
                    if hsh(x, y, 9) % 11 in (0, 5) and t > 17:
                        c = WHITE
                    put(img, x, y, c)
    return img


def legacy_look():
    """The 1.2.0 sprite (bronze-gold top section with a clover cut-out, shattered crystal below, thin gold guard)."""
    img = new_image()
    for y in range(16):
        for x in range(16):
            t, p = x + (15 - y), x - (15 - y)
            if t in (0, 1) and abs(p) <= 1:
                put(img, x, y, GOLD_LO if t == 0 else GOLD)
            elif 2 <= t <= 6 and p == 0:
                put(img, x, y, GRIP)
            elif t in (7, 8) and abs(p) <= 6 and (p % 2 == 0 or abs(p) <= 2):
                put(img, x, y, GOLD_HI if t == 8 and abs(p) <= 2 else GOLD)
            elif t >= 9 and abs(p) <= (4 if t < 26 else 2 if t < 29 else 0):
                edge = abs(p) == (4 if t < 26 else 2 if t < 29 else 0)
                if t < 14:
                    c = (GOLD_LO if edge else GOLD)
                    if t in (11, 12) and abs(p) <= 1:
                        c = NAVY
                    put(img, x, y, c)
                elif edge:
                    put(img, x, y, NAVY)
                else:
                    h = (x * 7 + y * 13 + t * 3) % 11
                    c = MID if abs(p) >= 2 else LIGHT
                    if h in (0, 5):
                        c = WHITE
                    elif h == 3:
                        c = ICE
                    elif h in (1, 8) and abs(p) >= 2:
                        c = DEEP
                    put(img, x, y, c)
    return img


if __name__ == "__main__":
    if "--legacy" in sys.argv:
        out = os.path.join(ITEM_DIR, "elsdocia_legacy.png")
        legacy_look().save(out)
    else:
        out = os.path.join(ITEM_DIR, "elsdocia.png")
        new_look().save(out)
    print("wrote", out)

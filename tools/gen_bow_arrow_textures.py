#!/usr/bin/env python3
"""Paints the textures of the Legendary Bow's real arrows (docs/heroweapon/bow_arrows.md).

    python3 -B tools/gen_bow_arrow_textures.py

Writes src/main/resources/assets/anime_multiverse/textures/entity/bow_arrow/<form>.png (32x32, the layout of the vanilla arrow texture: the 16x5 shaft at
(0,0) with the fletching on the left and the point on the right, the 5x5 back plate at (0,5)) for every form of the table below, and glow.png (16x16, the
soft ribbon of the trail: white, alpha shaped across the width, no colour under a transparent pixel). The renderer tints the glow with the form colour.
Keep this table in step with BowArrowForms.java; BowArrowFormsTest fails when a form of legendary_bow.json has no file here.
"""
import math
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "entity", "bow_arrow")

# form -> (fletching colour, point colour)  (same order and colours as BowArrowForms.java)
FORMS = {
    "base": (0xFFE8A0, 0xFFF4C8),
    "hunters_bow": (0x7DAA45, 0xC8D8A0),
    "scouts_bow": (0x6FD6E0, 0xCFF6FA),
    "bind_arrow": (0xE6E6E6, 0xFFFFFF),
    "bird_hunting": (0x7FB0FF, 0xCFE2FF),
    "arrow_shower": (0xFFC040, 0xFFE59A),
    "meteor_bow": (0xFF6A20, 0xFFC080),
    "frozen_rain": (0x9FE8FF, 0xE6FAFF),
    "eagle_piercer": (0xD8C060, 0xFFF0A0),
    "sagittarius": (0xB8A0FF, 0xEDE4FF),
    "bow_of_pride_1": (0x7A3FB5, 0xD9B8FF),
    "bow_of_pride_2": (0x7A3FB5, 0xD9B8FF),
    "bow_of_pride_3": (0x7A3FB5, 0xD9B8FF),
}

WOOD = [(0x8A6238), (0x6E4A28), (0x57391E)]


def rgba(c, a=255, shade=1.0):
    return (min(255, int(((c >> 16) & 255) * shade)), min(255, int(((c >> 8) & 255) * shade)), min(255, int((c & 255) * shade)), a)


def arrow(flet, tip):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = im.load()
    # shaft: 16 columns (tail on the left) by 5 rows
    for x in range(16):
        for y in range(5):
            if x <= 4:                                  # fletching: a fan, widest at the tail, a bright rim and a dark quill
                reach = 2 - (x // 2) if x < 4 else 0     # half-height of the vane at this column
                if abs(y - 2) <= reach + (1 if x < 2 else 0):
                    shade = 1.15 if (y + x) % 3 == 0 else 0.85
                    px[x, y] = rgba(flet, 255, shade)
                if y == 2 and x >= 1:
                    px[x, y] = rgba(flet, 255, 0.55)
            elif x <= 12:                                # wooden shaft, three rows, lit from the top
                if 1 <= y <= 3:
                    px[x, y] = rgba(WOOD[y - 1], 255, 1.0 + 0.08 * ((x % 3) - 1))
            else:                                        # the point: a small blade of the form's pale colour
                half = 2 if x <= 14 else 1
                if abs(y - 2) <= half - (1 if x == 15 and half == 1 else 0) or (x == 13 and abs(y - 2) <= 1):
                    px[x, y] = rgba(tip, 255, 1.0 - 0.12 * abs(y - 2))
    # back plate 5x5 at (0,5): the nock seen from behind
    for x in range(5):
        for y in range(5):
            d = math.hypot(x - 2, y - 2)
            if d <= 2.3:
                px[x, 5 + y] = rgba(flet, 255, 1.1 - 0.18 * d) if d > 0.6 else rgba(0x3A2814)
    return im


def glow():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for x in range(16):
        a = math.exp(-((x - 7.5) / 3.2) ** 2)
        for y in range(16):
            al = int(255 * a)
            px[x, y] = (255, 255, 255, al) if al >= 8 else (0, 0, 0, 0)
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    for form, (flet, tip) in FORMS.items():
        arrow(flet, tip).save(os.path.join(OUT, form + ".png"))
    glow().save(os.path.join(OUT, "glow.png"))
    print(len(FORMS), "arrow textures and glow.png written to", OUT)


if __name__ == "__main__":
    main()

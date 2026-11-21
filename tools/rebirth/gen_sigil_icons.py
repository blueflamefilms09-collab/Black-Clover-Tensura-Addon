#!/usr/bin/env python3
"""Draws the two 16x16 inventory icons of the rebirth rites (PIL, transparent background):

  sigil_of_rebirth.png          a gold ring around a four-leaf clover (gold / green)
  sigil_of_the_second_book.png  a white ring around two leaves growing side by side (white / gold)

usage: python3 -B tools/rebirth/gen_sigil_icons.py   (writes into src/main/resources/assets/anime_multiverse/textures/item/)
"""
import math
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/anime_multiverse/textures/item"
C = 7.5  # centre of a 16 px icon


def put(img, x, y, rgb):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), rgb + (255,))


def disc(img, d_in, d_out, rgb):
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - C, y - C)
            if d_in <= d < d_out:
                put(img, x, y, rgb)


def ellipse(img, cx, cy, a, b, ang, rgb, hi, edge, edge_from=0.62):
    """A leaf: ellipse with semi-axes a (along ang) and b; lit on its upper-left, edged on the rim."""
    ca, sa = math.cos(ang), math.sin(ang)
    for y in range(16):
        for x in range(16):
            u = ((x - cx) * ca + (y - cy) * sa) / a
            v = (-(x - cx) * sa + (y - cy) * ca) / b
            r = u * u + v * v
            if r <= 1.0:
                col = edge if r > edge_from else rgb
                if r <= edge_from and (x - cx) + (y - cy) < -0.6:
                    col = hi
                put(img, x, y, col)


def rebirth():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    disc(img, 0, 8.2, (74, 48, 8))        # dark outline
    disc(img, 0, 7.3, (242, 194, 48))     # gold ring
    disc(img, 5.7, 6.3, (255, 229, 138))  # ring highlight line
    disc(img, 0, 5.7, (16, 58, 28))       # deep green field
    for dx, dy in ((-2, -2), (2, -2), (-2, 2), (2, 2)):  # four leaves
        ellipse(img, C + dx * 1.15, C + dy * 1.15, 2.7, 2.1, math.atan2(dy, dx), (52, 178, 70), (142, 240, 138), (24, 100, 42))
    for k in range(4):                    # stem to the lower right
        put(img, 8 + k // 2 + 1, 8 + k + 1, (255, 229, 138))
    put(img, 7, 7, (255, 229, 138))
    put(img, 8, 7, (255, 229, 138))
    # glints on the ring
    put(img, 4, 2, (255, 255, 220))
    put(img, 12, 13, (255, 245, 180))
    return img


def second_book():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    disc(img, 0, 8.2, (96, 72, 16))        # dark gold outline
    disc(img, 0, 7.3, (250, 250, 250))     # white ring
    disc(img, 5.7, 6.3, (214, 224, 238))   # ring shade
    disc(img, 0, 5.7, (236, 200, 84))      # gold field
    disc(img, 0, 3.0, (250, 224, 130))     # soft light in the middle
    base_y, a = 11.4, 3.3
    for base_x, deg in ((6.6, -112), (9.4, -68)):   # two leaves side by side, fanned apart
        ang = math.radians(deg)
        ellipse(img, base_x + math.cos(ang) * a, base_y + math.sin(ang) * a, a, 1.75, ang, (255, 255, 255), (255, 255, 255), (170, 124, 24), 0.8)
    for k in range(2):                     # a short stem
        put(img, 7, 11 + k, (170, 124, 24))
        put(img, 8, 11 + k, (170, 124, 24))
    put(img, 4, 2, (255, 255, 255))
    put(img, 11, 13, (255, 255, 255))
    return img


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    rebirth().save(OUT / "sigil_of_rebirth.png")
    second_book().save(OUT / "sigil_of_the_second_book.png")
    print("wrote 2 icons to", OUT)

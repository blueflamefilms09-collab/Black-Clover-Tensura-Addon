"""Textures for the 0.46 Four Kingdoms menu (the button in Tensura's magic screen and the custom menu).

    python tools/gen_menu_textures.py  ->  src/main/resources/assets/nusmp/textures/gui/{four_kingdoms_logo,grimoire_page}.png

four_kingdoms_logo.png is a stand-in emblem drawn after the owner's "Four Kingdoms Tensura" logo: a stone ring, a four-leaf clover
of heart-shaped leaves, one per kingdom (Clover green, Diamond blue, Heart green/violet, Spade red), a blue slime at the foot.
The menu draws the title text itself, so dropping the real logo image in at the same path (square works best) replaces it.
grimoire_page.png is a parchment page for the grimoire spread.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "gui")
SS = 4


def noise(w, h, cell, seed):
    rng = np.random.default_rng(seed)
    g = rng.random((h // cell + 2, w // cell + 2)).astype(np.float32)
    return np.asarray(Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)).astype(np.float32) / 255


def heart(d, cx, cy, size, angle, fill, outline=None, width=0):
    """A heart (clover leaf) whose point is at (cx, cy) and whose lobes point along 'angle'."""
    pts = []
    for k in range(80):
        t = k / 80 * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        x, y = x / 17 * size, -(y + 17) / 17 * size         # point of the heart (y = -17) at the origin, lobes up
        ca, sa = math.cos(angle), math.sin(angle)
        pts.append((cx + x * ca - y * sa, cy + x * sa + y * ca))
    d.polygon(pts, fill=fill, outline=outline, width=width)


def logo(size=256):
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2
    # the stone ring with an inner bevel and engraved rune ticks
    d.ellipse((S * 0.03, S * 0.03, S * 0.97, S * 0.97), fill=(52, 52, 58, 255))
    d.ellipse((S * 0.07, S * 0.07, S * 0.93, S * 0.93), fill=(92, 92, 98, 255))
    d.ellipse((S * 0.13, S * 0.13, S * 0.87, S * 0.87), fill=(40, 40, 46, 255))
    for k in range(48):
        a = k * math.pi / 24
        r0, r1 = S * 0.385, S * 0.42
        d.line([(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a) * r1, c + math.sin(a) * r1)], fill=(150, 150, 160, 255), width=SS * 2)
    # four heart-shaped leaves, points meeting in the middle
    leaves = [(-math.pi / 2, (60, 150, 70)), (0, (60, 130, 220)), (math.pi / 2, (70, 160, 90)), (math.pi, (190, 40, 40))]
    for ang, col in leaves:
        heart(d, c, c, S * 0.215, ang + math.pi / 2, (20, 20, 24, 255))
        heart(d, c, c, S * 0.19, ang + math.pi / 2, col + (255,))
    # kingdom emblems in each leaf (clover, diamond, heart, spade)
    def at(ang, r):
        return c + math.cos(ang) * r, c + math.sin(ang) * r
    x, y = at(-math.pi / 2, S * 0.2)
    for k in range(3):
        a = -math.pi / 2 + k * 2 * math.pi / 3
        d.ellipse((x + math.cos(a) * S * 0.035 - S * 0.03, y + math.sin(a) * S * 0.035 - S * 0.03,
                   x + math.cos(a) * S * 0.035 + S * 0.03, y + math.sin(a) * S * 0.035 + S * 0.03), fill=(230, 200, 90, 255))
    x, y = at(0, S * 0.2)
    d.polygon([(x, y - S * 0.06), (x + S * 0.04, y), (x, y + S * 0.06), (x - S * 0.04, y)], fill=(180, 230, 255, 255))
    x, y = at(math.pi / 2, S * 0.2)
    heart(d, x, y + S * 0.045, S * 0.05, 0, (90, 220, 120, 255))
    x, y = at(math.pi, S * 0.2)
    heart(d, x, y - S * 0.04, S * 0.05, math.pi, (110, 10, 20, 255))
    d.polygon([(x, y), (x - S * 0.015, y + S * 0.05), (x + S * 0.015, y + S * 0.05)], fill=(110, 10, 20, 255))
    # the slime at the foot
    d.ellipse((c - S * 0.08, S * 0.8, c + S * 0.08, S * 0.92), fill=(40, 90, 220, 255))
    d.ellipse((c - S * 0.04, S * 0.82, c + S * 0.0, S * 0.85), fill=(190, 220, 255, 255))
    im = im.resize((size, size), Image.LANCZOS)
    glow = im.filter(ImageFilter.GaussianBlur(3))
    out = Image.alpha_composite(glow, im)
    out.save(os.path.join(OUT, "four_kingdoms_logo.png"), optimize=True)


def page(w=128, h=160):
    n = noise(w, h, 8, 7) * 0.6 + noise(w, h, 2, 8) * 0.4
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    edge = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
    vign = np.clip(edge / 12, 0, 1)
    base = np.array([238, 226, 196], np.float32)
    k = (0.86 + 0.1 * n) * (0.82 + 0.18 * vign)
    rgb = np.clip(base[None, None, :] * k[..., None], 0, 255).astype(np.uint8)
    Image.fromarray(np.dstack([rgb, np.full((h, w), 255, np.uint8)]), "RGBA").save(os.path.join(OUT, "grimoire_page.png"), optimize=True)


def main():
    os.makedirs(OUT, exist_ok=True)
    logo()
    page()
    print("wrote menu textures")


if __name__ == "__main__":
    main()

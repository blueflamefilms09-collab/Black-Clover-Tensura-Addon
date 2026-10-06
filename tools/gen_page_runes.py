"""Glowing spell runes for the open grimoire's pages (0.36): drawn only while the book is held open, full-bright and tinted with
the book's trim metal, so the open pages read as "casting" (after the owner's floating-grimoire references).

    python tools/gen_page_runes.py  ->  textures/item/grimoire_book/page_runes.png (64x64, white glyphs on transparent)
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_book", "page_runes.png")
N, SS = 64, 4


def main():
    rng = np.random.default_rng(36)
    im = Image.new("L", (N * SS, N * SS), 0)
    d = ImageDraw.Draw(im)
    # a small magic circle at the top of the page
    c, r = (N * SS / 2, 15 * SS), 10 * SS
    d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], outline=255, width=int(1.2 * SS))
    tri = [(c[0] + math.cos(-math.pi / 2 + k * 2 * math.pi / 3) * r * 0.8, c[1] + math.sin(-math.pi / 2 + k * 2 * math.pi / 3) * r * 0.8) for k in range(3)]
    d.polygon(tri, outline=230, width=int(1 * SS))
    # lines of angular rune script below it
    for row in range(5):
        y = (30 + row * 6.5) * SS
        x = 8 * SS
        while x < (N - 10) * SS:
            gw = rng.uniform(2.5, 4.5) * SS
            pts = [(x + rng.random() * gw, y + rng.uniform(-2, 2) * SS) for _ in range(3)]
            d.line(pts, fill=255, width=int(0.9 * SS), joint="curve")
            x += gw + rng.uniform(1, 2.5) * SS
    a = im.resize((N, N), Image.LANCZOS)
    glow = a.filter(ImageFilter.GaussianBlur(1.2))
    alpha = np.clip(np.asarray(a).astype(np.float32) + np.asarray(glow).astype(np.float32) * 0.6, 0, 255).astype(np.uint8)
    out = np.dstack([np.full((N, N), 255, np.uint8)] * 3 + [alpha])
    Image.fromarray(out, "RGBA").save(OUT)
    print("wrote page_runes")


if __name__ == "__main__":
    main()

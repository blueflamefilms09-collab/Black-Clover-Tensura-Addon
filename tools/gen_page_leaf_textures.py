"""Textures for the page-flip animation (0.22): a single loose grimoire page, drawn while pages turn on a spell switch.

    python tools/gen_page_leaf_textures.py  ->  textures/item/grimoire_book/page_leaf.png, page_leaf_tattered.png

page_leaf          classic parchment: warm cream, darker aged rim, faint lines of script
page_leaf_tattered anti-magic: torn, ragged edges (transparent bites), dark soot stains and a few cracks
Deterministic, Pillow + numpy.
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_book")
W, H = 32, 32


def smooth_noise(cell, seed):
    rng = np.random.default_rng(seed)
    g = rng.random((H // cell + 2, W // cell + 2))
    yy, xx = np.mgrid[0:H, 0:W] / cell
    x0, y0 = xx.astype(int), yy.astype(int)
    tx, ty = xx - x0, yy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def parchment(seed):
    n = 0.6 * smooth_noise(8, seed) + 0.4 * smooth_noise(3, seed + 1)
    yy, xx = np.mgrid[0:H, 0:W]
    edge = np.minimum.reduce([xx, yy, W - 1 - xx, H - 1 - yy]).astype(np.float32)
    rim = np.clip(1 - edge / 4.0, 0, 1)
    shade = 0.92 + 0.12 * (n - 0.5) - 0.28 * rim
    rgb = np.dstack([243 * shade, 230 * shade, 196 * shade])
    # faint script: short dashes on every 3rd row inside a margin
    rng = np.random.default_rng(seed + 7)
    for y in range(6, H - 5, 3):
        x = 5
        while x < W - 6:
            L = int(rng.integers(2, 6))
            rgb[y, x:min(x + L, W - 5)] *= 0.72
            x += L + int(rng.integers(1, 3))
    return rgb, n


def main():
    os.makedirs(OUT, exist_ok=True)
    rgb, _ = parchment(3)
    a = np.full((H, W), 255.0)
    Image.fromarray(np.dstack([rgb, a]).clip(0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "page_leaf.png"), optimize=True)

    rgb, n = parchment(11)
    rgb *= 0.4                                                      # soot-darkened
    soot = smooth_noise(5, 23)
    rgb *= (0.55 + 0.6 * soot)[..., None]
    yy, xx = np.mgrid[0:H, 0:W]
    edge = np.minimum.reduce([xx, yy, W - 1 - xx, H - 1 - yy]).astype(np.float32)
    ragged = smooth_noise(2, 31) * 3.2 + smooth_noise(6, 37) * 2.0
    a = np.where(edge + 0.5 < ragged, 0.0, 255.0)                   # torn bites out of every edge
    rng = np.random.default_rng(41)
    for _ in range(3):                                              # cracks with a faint red glow
        x, y = int(rng.integers(6, W - 6)), int(rng.integers(6, H - 6))
        for _ in range(int(rng.integers(5, 9))):
            if 0 <= x < W and 0 <= y < H:
                rgb[y, x] = (150, 24, 30)
            x += int(rng.integers(-1, 2)); y += int(rng.integers(-1, 2))
    Image.fromarray(np.dstack([rgb, a]).clip(0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "page_leaf_tattered.png"), optimize=True)
    print("wrote page_leaf.png, page_leaf_tattered.png")


if __name__ == "__main__":
    main()

"""The Time grimoire's page drum (0.37): the ribbed outer band of page edges, after the owner's screenshot of the coverless
cylinder (cream pages packed edge to edge, thin shadow lines between them, slightly uneven tops and bottoms).

    python tools/gen_time_drum_texture.py  ->  textures/item/grimoire_book/drum_edge.png (64x32, U round the drum, V top -> bottom)
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_book", "drum_edge.png")


def main():
    w, h = 64, 32
    rng = np.random.default_rng(37)
    xx = np.arange(w)[None, :] * np.ones((h, 1))
    yy = np.arange(h)[:, None] * np.ones((1, w))
    base = np.array([240, 232, 208], np.float32)
    rib = np.where(xx % 2 == 0, 1.0, 0.86)                     # every other page edge a little darker: the ribbing
    jitter = rng.uniform(0.94, 1.02, (1, w))
    shade = np.clip(1.02 - 0.12 * np.abs(yy - h / 2) / (h / 2), 0, 1)
    col = base[None, None, :] * (rib * jitter * shade)[..., None]
    alpha = np.full((h, w), 255, np.uint8)
    tops = rng.integers(0, 2, w)                                # uneven top / bottom edges
    for x in range(w):
        alpha[: tops[x], x] = 0
        alpha[h - rng.integers(0, 2):, x] = 0
    Image.fromarray(np.dstack([np.clip(col, 0, 255).astype(np.uint8), alpha]), "RGBA").save(OUT)
    print("wrote drum_edge")


if __name__ == "__main__":
    main()

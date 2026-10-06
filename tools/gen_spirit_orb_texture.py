"""Generates the Spirit Lord orb texture (white, tinted by the spirit's colour in SpiritOrbRenderer).

    python tools/gen_spirit_orb_texture.py   ->  src/main/resources/assets/nusmp/textures/entity/spirit_orb.png
"""
import math
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity", "spirit_orb.png")


def main(size=64):
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    ang = np.arctan2(ny, nx)
    core = np.exp(-(r / 0.28) ** 2)                                   # bright heart
    body = np.clip(1 - r / 0.62, 0, 1) ** 1.2                          # the orb
    swirl = np.clip(np.sin(ang * 2 + r * 10) * 0.5 + 0.5, 0, 1) ** 4 * np.clip(1 - r / 0.62, 0, 1)
    halo = np.exp(-((r - 0.62) / 0.12) ** 2) * 0.55                    # soft rim
    a = np.clip(body * 0.85 + core + halo + swirl * 0.3, 0, 1) * np.clip((1 - r) / 0.08, 0, 1)
    white = np.clip(0.75 + core * 0.4 + swirl * 0.2, 0, 1)
    img = np.dstack([white * 255, white * 255, white * 255, a * 255]).astype(np.uint8)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    Image.fromarray(img, "RGBA").save(OUT, optimize=True)
    print("wrote", OUT)


if __name__ == "__main__":
    main()

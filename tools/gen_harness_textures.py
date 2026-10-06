"""Grimoire holster harness (0.35): leather strap and brass fittings, after the owner's belt-holster references.

    python tools/gen_harness_textures.py  ->  textures/entity/grimoire_harness_leather.png (64x16, U along the strap)
                                              textures/entity/grimoire_harness_brass.png   (16x16, buckle / strap tip)
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity")


def noise(w, h, seed):
    rng = np.random.default_rng(seed)
    return rng.random((h, w)).astype(np.float32)


def leather(w=64, h=16):
    base = np.array([168, 84, 40], np.float32)                      # saddle tan, like the reference holster
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    grain = 0.9 + 0.1 * noise(w, h, 3) + 0.05 * np.sin(xx * 0.7 + noise(w, h, 4) * 3)
    edge = np.clip(np.minimum(yy, h - 1 - yy) / 2.5, 0, 1)            # darker burnished edges
    col = base[None, None, :] * (grain * (0.6 + 0.4 * edge))[..., None]
    stitch = ((yy == 2) | (yy == h - 3)) & ((xx.astype(int) % 4) < 2)
    col[stitch] = [232, 214, 170]
    col[(yy == 0) | (yy == h - 1)] *= 0.55
    a = np.full((h, w), 255, np.uint8)
    return Image.fromarray(np.dstack([np.clip(col, 0, 255).astype(np.uint8), a]), "RGBA")


def brass(s=16):
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    shine = 0.75 + 0.35 * (1 - (xx + yy) / (2 * s)) + 0.08 * noise(s, s, 9)
    col = np.array([196, 150, 64], np.float32)[None, None, :] * shine[..., None]
    a = np.full((s, s), 255, np.uint8)
    hole = (np.abs(xx - s / 2 + 0.5) < 3) & (np.abs(yy - s / 2 + 0.5) < 3)    # the buckle's frame opening
    col[hole] = [70, 40, 20]
    rim = (xx < 1) | (yy < 1) | (xx > s - 2) | (yy > s - 2)
    col[rim] *= 0.6
    return Image.fromarray(np.dstack([np.clip(col, 0, 255).astype(np.uint8), a]), "RGBA")


def main():
    os.makedirs(OUT, exist_ok=True)
    leather().save(os.path.join(OUT, "grimoire_harness_leather.png"))
    brass().save(os.path.join(OUT, "grimoire_harness_brass.png"))
    print("wrote harness textures")


if __name__ == "__main__":
    main()

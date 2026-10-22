#!/usr/bin/env python3
"""Textures of the Light and Bone Magic remake (docs/magic/light_bone_ja.md): three small opaque textures that the solid parts of the props are drawn with.

    python3 -B tools/gen_light_bone_ja_textures.py

assets/nusmp/textures/prop/bone_ja_bone.png     ivory bone with grain, pits and a marrow tint (the fangs, the bullets, the grip of the sword)
assets/nusmp/textures/prop/bone_ja_space.png    near-black violet with thin glowing veins (the blade of the Demon Sword Dainsleif: bone coated in cut space)
assets/nusmp/textures/prop/light_ja_plate.png   pale gold plate with panel lines (the swords of light, the golden giant)

They are plain opaque textures for the cutout render type (no glow card, nothing additive), so the safe-additive scan does not apply to them. Deterministic (fixed seed): running it again
writes the same files. Needs: numpy, pillow.
"""
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.abspath(os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "prop"))


def save(arr, name):
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, name))


def noise(rng, n, scale):
    """Value noise n x n with the given cell size, tiled (wraps) so the cylinder seam does not show."""
    cells = max(2, n // scale)
    g = rng.random((cells, cells))
    img = np.zeros((n, n))
    for y in range(n):
        for x in range(n):
            fx, fy = x / scale, y / scale
            x0, y0 = int(fx) % cells, int(fy) % cells
            x1, y1 = (x0 + 1) % cells, (y0 + 1) % cells
            tx, ty = fx - int(fx), fy - int(fy)
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            img[y, x] = (g[y0, x0] * (1 - tx) + g[y0, x1] * tx) * (1 - ty) + (g[y1, x0] * (1 - tx) + g[y1, x1] * tx) * ty
    return img


def bone():
    n = 32
    rng = np.random.default_rng(1101)
    grain = noise(rng, n, 4)
    streak = np.tile(noise(rng, n, 8)[:1, :], (n, 1))          # streaks that run along v (the length of a cylinder)
    base = np.zeros((n, n, 4))
    base[..., 3] = 255
    shade = 0.80 + 0.14 * grain + 0.10 * streak
    for c, v in enumerate((236, 227, 204)):
        base[..., c] = v * shade
    pits = rng.random((n, n)) > 0.965
    base[pits, :3] *= 0.62
    marrow = noise(rng, n, 8) > 0.78
    base[marrow, 0] *= 0.98
    base[marrow, 1] *= 0.90
    base[marrow, 2] *= 0.84
    save(base, "bone_ja_bone.png")


def space():
    n = 32
    rng = np.random.default_rng(1102)
    base = np.zeros((n, n, 4))
    base[..., 3] = 255
    g = noise(rng, n, 4)
    base[..., 0] = 20 + 16 * g
    base[..., 1] = 10 + 8 * g
    base[..., 2] = 38 + 26 * g
    veins = np.abs(noise(rng, n, 8) - 0.5) < 0.035
    base[veins, 0] = 168
    base[veins, 1] = 120
    base[veins, 2] = 255
    specks = rng.random((n, n)) > 0.985
    base[specks, :3] = (210, 190, 255)
    save(base, "bone_ja_space.png")


def plate():
    n = 32
    rng = np.random.default_rng(1103)
    base = np.zeros((n, n, 4))
    base[..., 3] = 255
    g = noise(rng, n, 8)
    shade = 0.93 + 0.07 * g
    for c, v in enumerate((255, 251, 238)):
        base[..., c] = v * shade
    for k in range(0, n, 8):                                    # panel lines every 8 px, gold
        base[k, :, :3] = (238, 204, 122)
        base[:, k, :3] = (238, 204, 122)
        base[(k + 1) % n, :, :3] = (255, 240, 190)
        base[:, (k + 1) % n, :3] = (255, 240, 190)
    rivets = [(4, 4), (12, 4), (20, 4), (28, 4), (4, 20), (12, 20), (20, 20), (28, 20)]
    for (x, y) in rivets:
        base[y, x, :3] = (214, 170, 80)
    save(base, "light_ja_plate.png")


if __name__ == "__main__":
    bone()
    space()
    plate()
    print("wrote bone_ja_bone.png bone_ja_space.png light_ja_plate.png to", OUT)

"""The soft glow behind an open revamped grimoire (client/grimoire/GrimoireRevampFx): a white radial falloff with a faint ring, alpha only.
Drawn translucent and tinted with the magic's glow colour, never additive (so it can not blow out on a bright sky).
    python3 -B tools/grimoire_revamp/gen_halo.py"""
import os
import numpy as np
from PIL import Image

N = 128
y, x = np.mgrid[0:N, 0:N]
r = np.hypot((x + 0.5) / N * 2 - 1, (y + 0.5) / N * 2 - 1)
core = np.clip(1 - r, 0, 1) ** 2.4
ring = np.exp(-((r - 0.78) / 0.05) ** 2) * 0.35
a = np.clip(core + ring, 0, 1) * (r < 1)
px = np.zeros((N, N, 4), np.uint8)
px[..., :3] = 255
px[..., 3] = (a * 255).astype(np.uint8)
out = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "entity", "grimoire_revamp_halo.png")
Image.fromarray(px, "RGBA").save(os.path.abspath(out), optimize=True)
print("wrote", os.path.abspath(out))

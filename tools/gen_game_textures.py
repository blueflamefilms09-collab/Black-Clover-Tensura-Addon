"""0.49 Game Magic VFX texture: textures/particle/game_board.png - a round game board of squares (white + alpha, tinted per quad).

    python tools/gen_game_textures.py
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
S, N = 256, 12
yy, xx = np.mgrid[0:S, 0:S].astype(np.float32) + 0.5
u, v = xx / S, yy / S
r = np.hypot(u - 0.5, v - 0.5) * 2
cell = ((np.floor(u * N) + np.floor(v * N)) % 2).astype(np.float32)
fu, fv = (u * N) % 1, (v * N) % 1
line = ((np.minimum(fu, 1 - fu) < 0.06) | (np.minimum(fv, 1 - fv) < 0.06)).astype(np.float32)
ring = np.clip(1 - np.abs(r - 0.94) * 40, 0, 1)
inside = (r < 0.92).astype(np.float32)
a = inside * (0.18 + 0.22 * cell + 0.6 * line) * np.clip((0.92 - r) * 12, 0, 1) + ring
g = np.clip(0.7 + 0.3 * line + ring, 0, 1)
img = np.dstack([g * 255, g * 255, g * 255, np.clip(a, 0, 1) * 255]).astype(np.uint8)
Image.fromarray(img, "RGBA").save(os.path.join(OUT, "game_board.png"), optimize=True)
print("wrote particle/game_board")

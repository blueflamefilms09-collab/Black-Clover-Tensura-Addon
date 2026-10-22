"""Generates the material sheet of the solid spell props (textures/flatprops/solid.png).

Deterministic (fixed seeds), Pillow + numpy only.

    python3 -B tools/gen_flatprops_textures.py

Why (docs/visual/flat_props.md): the flat pale cut-outs of swords, axes, shields and doubles are gone; the same spells now draw small solid models built of shaded parts
(client/vfx SolidParts). The parts need a material with grain, so each cell of the sheet is a fully OPAQUE 32 x 32 tile of one material:
    row 0:  0 steel   1 gold trim   2 bone   3 glass (pale, with a sheen)
    row 1:  4 oak     5 dark steel  6 leather / cloth   7 mirror silver
The sheet is 128 x 64 (4 x 2 cells). Faces sample the middle of a cell only (SolidParts insets the UV by one texel), so linear filtering never bleeds a neighbour.
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "flatprops")
CELL = 32


def tile(base, grain, streak, seed, vertical=False, speck=0.05):
    """One opaque tile: base colour, soft low-frequency mottling, fine streaks along one axis, a few specks, a lit top edge."""
    rng = np.random.default_rng(seed)
    low = rng.random((4, 4)).astype(np.float32)
    low = np.asarray(Image.fromarray((low * 255).astype(np.uint8)).resize((CELL, CELL), Image.BICUBIC), np.float32) / 255
    fine = rng.random((CELL, CELL)).astype(np.float32)
    s = rng.random((1, CELL)).astype(np.float32) if not vertical else rng.random((CELL, 1)).astype(np.float32)
    s = np.broadcast_to(s, (CELL, CELL))
    k = 1.0 + (low - 0.5) * grain + (s - 0.5) * streak + (fine - 0.5) * grain * 0.35
    spots = (rng.random((CELL, CELL)) < speck).astype(np.float32) * (rng.random((CELL, CELL)).astype(np.float32) - 0.5) * 0.35
    k = k + spots
    yy = np.linspace(0.07, -0.07, CELL, dtype=np.float32)[:, None]                  # a gentle vertical light ramp
    k = k + yy
    rgb = np.clip(np.array(base, np.float32)[None, None, :] * k[..., None], 0, 255)
    return rgb


def build():
    cells = [
        tile((172, 180, 196), 0.18, 0.20, 1),                    # 0 steel
        tile((222, 184, 92), 0.16, 0.10, 2, speck=0.08),         # 1 gold trim
        tile((236, 228, 206), 0.14, 0.05, 3, vertical=True),     # 2 bone
        tile((196, 220, 236), 0.06, 0.04, 4, speck=0.02),        # 3 glass
        tile((148, 100, 58), 0.22, 0.32, 5, vertical=True),      # 4 oak
        tile((88, 94, 112), 0.20, 0.18, 6),                      # 5 dark steel
        tile((104, 60, 52), 0.20, 0.10, 7),                      # 6 leather / cloth
        tile((206, 208, 224), 0.08, 0.05, 8, speck=0.03),        # 7 mirror silver
    ]
    sheet = np.zeros((CELL * 2, CELL * 4, 4), np.uint8)
    sheet[..., 3] = 255
    for i, c in enumerate(cells):
        cy, cx = divmod(i, 4)
        sheet[cy * CELL:(cy + 1) * CELL, cx * CELL:(cx + 1) * CELL, :3] = c.astype(np.uint8)
    # the glass cell gets a diagonal sheen stripe, so a glass figure shows a highlight and not a flat tint
    for y in range(CELL):
        for x in range(CELL):
            d = abs((x + y) - CELL * 0.9)
            if d < 3:
                px = sheet[y, 96 + x, :3].astype(np.float32)
                sheet[y, 96 + x, :3] = np.clip(px + (3 - d) * 18, 0, 255).astype(np.uint8)
    return sheet


def main():
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(build(), "RGBA").save(os.path.join(OUT, "solid.png"))
    print("wrote", os.path.join(OUT, "solid.png"))


if __name__ == "__main__":
    main()

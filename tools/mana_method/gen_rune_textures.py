"""
Generates the textures of the written Mana Method rune (assets/nusmp/textures/particle/):

  rune_stroke_<tint>.png   32 x 48: ONE stroke segment, across the stroke (x) and along it (y: the start of the stroke at the bottom, as the vertex buffer maps a quad's texture). A bright core
                           and an orange-red edge in the colours of the tint (gold = True Magic and the reference look, flame, water, wind, earth, lightning, plant), soft round ends. Straight colour
                           with the alpha shaping it (the vertex buffer's additive blend is SRC_ALPHA, ONE).
  rune_line_ring.png       128 x 128: a thin glowing circle at 92 % of the half width (RuneGlyphLayout.LINE_RADIUS_IN_TEXTURE), white (the layer tints it), transparent corners.
  rune_arc_strip.png       8 x 16: a lightning strip, a bright core with a soft edge across, white (tinted by the layer).

    python tools/mana_method/gen_rune_textures.py
"""
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")

# tint -> (core colour, edge colour); the same table as gen_rune_glyphs.py
TINTS = {
    "gold": ((255, 238, 110), (232, 84, 22)),
    "flame": ((255, 214, 96), (222, 52, 12)),
    "water": ((190, 240, 255), (28, 108, 226)),
    "wind": ((228, 255, 238), (52, 176, 134)),
    "earth": ((255, 232, 170), (150, 98, 44)),
    "lightning": ((255, 255, 190), (240, 168, 20)),
    "plant": ((220, 255, 170), (66, 170, 52)),
}


def save(arr, name):
    im = Image.fromarray((np.clip(arr, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name))


def stroke(tint):
    core, edge = (np.array(c, np.float32) / 255.0 for c in TINTS[tint])
    across, along = 32, 48
    t = (np.arange(across) + 0.5) / across * 2 - 1                      # -1 .. 1 across
    s = (np.arange(along) + 0.5) / along                                  # 0 .. 1 along
    T, S = np.meshgrid(t, s)
    edge_a = np.clip((1.0 - np.abs(T)) / 0.38, 0, 1) ** 1.15
    end = np.clip(np.minimum(S, 1 - S) / 0.13, 0, 1) ** 0.8
    alpha = edge_a * end
    k = np.exp(-(T / 0.36) ** 2)
    rgb = edge[None, None, :] * (1 - k[..., None]) + core[None, None, :] * k[..., None]
    # transparent texels are black (no colour under alpha: the sprite is safe with every blend)
    rgb = rgb * (alpha[..., None] > 0.02)
    return np.dstack([rgb, alpha])


def line_ring():
    n = 128
    y, x = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.sqrt((x + 0.5 - n / 2) ** 2 + (y + 0.5 - n / 2) ** 2)
    r0 = 0.92 * n / 2
    a = np.exp(-((r - r0) / 1.4) ** 2) + 0.10 * np.exp(-((r - r0) / 4.5) ** 2)
    a = np.clip(a, 0, 1) * (r < n / 2 - 1)
    return np.dstack([np.ones((n, n, 3), np.float32) * (a[..., None] > 0.01), a])


def arc_strip():
    across, along = 8, 16
    t = (np.arange(across) + 0.5) / across * 2 - 1
    T = np.tile(t[None, :], (along, 1))
    a = np.clip(np.exp(-(T / 0.55) ** 2) * 1.1 - 0.05, 0, 1)
    return np.dstack([np.ones((along, across, 3), np.float32) * (a[..., None] > 0.02), a])


def main():
    for tint in TINTS:
        save(stroke(tint), "rune_stroke_%s.png" % tint)
    save(line_ring(), "rune_line_ring.png")
    save(arc_strip(), "rune_arc_strip.png")
    print("wrote", OUT)


if __name__ == "__main__":
    sys.exit(main())

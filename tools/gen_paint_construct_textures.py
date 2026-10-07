"""Skins for Painting Magic's living illustrations (0.44): bodies of wet paint in each paint colour.

    python tools/gen_paint_construct_textures.py  ->  src/main/resources/assets/nusmp/textures/entity/painted/{beast,humanoid}_<paint>.png

Every texel is painted with overlapping brushstrokes (lighter and darker tones of the paint, a glossy streak here and there),
so any UV layout reads as a body of paint. The face gets two bright eyes; the humanoid gets an ink-dark visor band.
"""
import os
import random

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity", "painted")
PAINTS = {  # name -> rgb (PaintStudio.Paint order)
    "ink": (0x3A, 0x7B, 0xFF), "fire": (0xFF, 0x5A, 0x3A), "water": (0x4A, 0xA8, 0xFF), "ice": (0x8A, 0xE6, 0xFF),
    "wind": (0x7C, 0xF0, 0xB0), "earth": (0xB0, 0x86, 0x4A), "lightning": (0xFF, 0xE6, 0x5A),
}


def tone(rgb, k):
    return tuple(int(max(0, min(255, c * k + (255 - c) * max(0, k - 1)))) for c in rgb)


def skin(rgb, seed, eyes, visor=None):
    rnd = random.Random(seed)
    s = 64
    im = Image.new("RGBA", (s, s), tone(rgb, 0.8) + (255,))
    d = ImageDraw.Draw(im)
    for _ in range(260):                                   # overlapping brushstrokes
        x, y = rnd.uniform(-4, s), rnd.uniform(-2, s)
        ln, w = rnd.uniform(4, 14), rnd.uniform(1.5, 3.5)
        k = rnd.choice([0.65, 0.75, 0.85, 0.95, 1.05, 1.15])
        d.line([(x, y), (x + ln, y + rnd.uniform(-1.5, 1.5))], fill=tone(rgb, k) + (255,), width=int(round(w)))
    for _ in range(40):                                    # wet glossy streaks
        x, y = rnd.uniform(0, s), rnd.uniform(0, s)
        d.line([(x, y), (x + rnd.uniform(2, 6), y)], fill=tone(rgb, 1.45) + (255,), width=1)
    if visor:
        d.rectangle(visor, fill=tone(rgb, 0.35) + (255,))
    for (ex, ey) in eyes:
        d.rectangle((ex, ey, ex + 1, ey), fill=(255, 255, 245, 255))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, (name, rgb) in enumerate(PAINTS.items()):
        # beast: head box at texOffs(0,0), 6x6x6 -> front face u 6..12, v 6..12
        skin(rgb, 100 + i, eyes=[(7, 8), (10, 8)]).save(os.path.join(OUT, f"beast_{name}.png"), optimize=True)
        # humanoid (zombie/player layout): head front u 8..16, v 8..16; a visor band across the eyes
        skin(rgb, 200 + i, eyes=[(9, 11), (13, 11)], visor=(8, 10, 15, 12)).save(os.path.join(OUT, f"humanoid_{name}.png"), optimize=True)
        print("wrote", name)


if __name__ == "__main__":
    main()

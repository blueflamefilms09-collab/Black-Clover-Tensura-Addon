#!/usr/bin/env python3
"""
STAR V2 (docs/vfx_remake/star_ingame.md): the game textures of the Star Magic look, A "anime cel" and B "volumetric glow".

    python tools/gen_star_v2_textures.py            # Pillow + numpy only, deterministic (fixed seeds)

The pictures are the ones of the preview round (tools/gen_star_new_textures.py, docs/previews/vfx_remake/star/tex/); this script runs those generators unchanged and writes the 24 that the game draws into
src/main/resources/assets/nusmp/textures/particle/stv_<name>.png, with one extra step: the colour of every visible texel is bled 4 texels into the clear ones, so linear filtering never draws a dark
edge (the same rule as the Fire V2 textures). The palette is a PLACEHOLDER (gold-white and starlight blue): no canon still of Star Magic was available.
The list below must equal vfx/StarV2.TEXTURE_NAMES (a test pins it).
"""
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_star_new_textures as g  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")
PREFIX = "stv_"

# preview name -> game name
KEEP = {
    "A_star4": "a_star4", "A_spark": "a_spark", "A_sigil": "a_sigil", "A_line": "a_line", "A_pane": "a_pane", "A_ring": "a_ring", "A_shard": "a_shard", "A_burst": "a_burst",
    "A_whip": "a_whip", "A_beam": "a_beam", "A_crack": "a_crack", "A_rays": "a_rays",
    "B_glow": "b_glow", "B_glow_blue": "b_glow_blue", "B_flare": "b_flare", "B_star": "b_star", "B_beam": "b_beam", "B_ring": "b_ring", "B_rays": "b_rays",
    "B_dust_a": "b_dust_a", "B_dust_b": "b_dust_b", "B_dust_c": "b_dust_c", "B_trail": "b_trail", "B_sigil": "b_sigil",
}
_made = {}


def bleed(im, iterations=4):
    """copies the colour of visible texels into the clear texels next to them (alpha stays 0 there)"""
    a = np.asarray(im.convert("RGBA"), np.float32).copy()
    rgb, al = a[..., :3], a[..., 3]
    has = al > 0
    for _ in range(iterations):
        new = ~has
        acc = np.zeros_like(rgb)
        cnt = np.zeros(al.shape, np.float32)
        for dy in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx == 0 and dy == 0:
                    continue
                sh = np.roll(np.roll(rgb, dy, 0), dx, 1)
                shh = np.roll(np.roll(has, dy, 0), dx, 1)
                acc += sh * shh[..., None]
                cnt += shh
        fill = new & (cnt > 0)
        rgb[fill] = acc[fill] / cnt[fill][..., None]
        has = has | fill
    a[..., :3] = rgb
    return Image.fromarray(np.clip(a + 0.5, 0, 255).astype(np.uint8), "RGBA")


def capture(name, im):
    _made[name] = im


def main():
    g.save = capture
    g.a_star4(); g.a_star4_small(); g.a_burst(); g.a_ring(); g.a_sigil(); g.a_line(); g.a_beam(); g.a_whip(); g.a_pane(); g.a_crack(); g.a_rays(); g.a_shard()
    g.b_glow(); g.b_glow_blue(); g.b_flare(); g.b_star()
    for i in range(3):
        g.b_dust(i)
    g.b_beam(); g.b_trail(); g.b_ring(); g.b_rays(); g.b_sigil()
    os.makedirs(OUT, exist_ok=True)
    n = 0
    for src, dst in KEEP.items():
        im = _made[src]
        bleed(im).save(os.path.join(OUT, PREFIX + dst + ".png"))
        n += 1
    print(n, "textures ->", OUT)


if __name__ == "__main__":
    main()

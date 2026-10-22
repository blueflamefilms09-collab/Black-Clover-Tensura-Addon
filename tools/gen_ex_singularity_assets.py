#!/usr/bin/env python3
"""Generates the textures of the EX-Singularity sword effect: the prismatic shard (a four-pointed star glint) and the tint-neutral copy of Shishou's glow skin.

Output (deterministic):
  src/main/resources/assets/nusmp/textures/entity/weapon/ex_singularity_star.png    (64x64 star glint)
  src/main/resources/assets/nusmp/textures/entity/weapon/shishou_prism_glow.png     (the sword's glow skin made tint-neutral)

The sword's orbiting shards (client/ExSingularityFx) draw this one star in the additive pass and tint it per vertex with the spectrum, so the texture itself is neutral: white, with the
colour PREMULTIPLIED (RGB = alpha, transparent texels are 0,0,0,0). That makes it safe in both blends of client/SafeAdditive (alpha-weighted and plain additive): nothing under alpha, no
hard edge, no box (tools/safe_additive/scan_textures.py scans it: the name contains "star" under entity/weapon).
Run:  python3 -B tools/gen_ex_singularity_assets.py
"""
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
WEAPON = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity", "weapon")
OUT = os.path.join(WEAPON, "ex_singularity_star.png")
GLOW_IN = os.path.join(WEAPON, "shishou_glow.png")
GLOW_OUT = os.path.join(WEAPON, "shishou_prism_glow.png")
N = 64


def star():
    c = (N - 1) / 2.0
    ys, xs = np.mgrid[0:N, 0:N].astype(np.float64)
    x = (xs - c) / c            # -1..1
    y = (ys - c) / c
    def spike(ax, ay, wx, wy):
        return np.exp(-((ax / wx) ** 2 + (ay / wy) ** 2))
    core = np.exp(-((x ** 2 + y ** 2) / 0.012))
    vert = spike(x, y, 0.045, 0.72)                 # the long vertical spike
    horz = spike(x, y, 0.72, 0.045)                 # the long horizontal spike (a little shorter below)
    horz *= 0.85
    r = (x + y) / np.sqrt(2.0)
    s = (x - y) / np.sqrt(2.0)
    d1 = spike(r, s, 0.40, 0.030) * 0.45            # short diagonal flares
    d2 = spike(s, r, 0.40, 0.030) * 0.45
    halo = np.exp(-((x ** 2 + y ** 2) / 0.10)) * 0.22
    a = np.clip(core * 1.25 + vert + horz + d1 + d2 + halo, 0.0, 1.0)
    edge = np.clip((1.0 - np.maximum(np.abs(x), np.abs(y))) / 0.12, 0.0, 1.0)      # a smooth window: nothing reaches the border
    a = a * edge * edge
    a[a < 0.012] = 0.0
    return a


def prism_glow():
    """The sword's own glow skin (shishou_glow.png) is cyan and yellow: tinted with a hue it would only ever make a dim cyan-ish red or green. The prismatic passes multiply their vertex
    colour with the texture, so they need a NEUTRAL map: the same shape and alpha, every lit texel white scaled by the brightest channel and PREMULTIPLIED by the alpha (colour never sits under alpha 0)."""
    im = np.asarray(Image.open(GLOW_IN).convert("RGBA"), np.float64)
    a = im[..., 3]
    lum = im[..., :3].max(axis=2)
    out = np.zeros(im.shape, np.uint8)
    v = np.round(lum * a / 255.0).astype(np.uint8)                      # premultiplied: nothing under alpha 0, and plain and alpha-weighted additive blends agree
    out[..., 0] = v
    out[..., 1] = v
    out[..., 2] = v
    out[..., 3] = a.astype(np.uint8)
    Image.fromarray(out, "RGBA").save(GLOW_OUT)
    print("wrote", os.path.relpath(GLOW_OUT, os.path.join(HERE, "..")), "lit texels", int((a > 0).sum()))


def main():
    prism_glow()
    a = star()
    v = np.round(a * 255.0).astype(np.uint8)
    img = np.zeros((N, N, 4), dtype=np.uint8)
    img[..., 0] = v
    img[..., 1] = v
    img[..., 2] = v
    img[..., 3] = v
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    Image.fromarray(img, "RGBA").save(OUT)
    print("wrote", os.path.relpath(OUT, os.path.join(HERE, "..")), "nonzero", int((v > 0).sum()))


if __name__ == "__main__":
    main()

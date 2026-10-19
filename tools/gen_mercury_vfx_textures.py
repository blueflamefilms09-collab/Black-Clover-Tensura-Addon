"""Particle textures of the Mercury Magic effects (vfx/client/layer/MercuryLayer.java): textures/particle/mercury_*.png.

    python3 -B tools/gen_mercury_vfx_textures.py

ripple     concentric ripples with an interference ring, the surface of a pool (256)
droplet    a teardrop with a lit side (64)
streak     a ribbon of liquid with beads on it, 1:4 (64 x 256)
crown      the crown of a splash: tapering spikes round a ring (128)
spearhead  a spear tip in profile with a lit edge (64 x 128)
grid       a liquid lattice: bars and cross-links bending a little, for the cage and the dome (256)
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from transmutation_vfx_kit import Img, save, glow, radial  # noqa: E402


def ripple():
    yy, xx = np.mgrid[0:256, 0:256]
    r = np.sqrt((xx - 127.5) ** 2 + (yy - 127.5) ** 2)
    a = np.zeros((256, 256))
    for k, (r0, w, s) in enumerate(((118, 2.4, 1.0), (96, 1.8, 0.8), (74, 2.4, 0.9), (52, 1.8, 0.7), (30, 2.0, 0.8), (10, 3.0, 1.0))):
        a = np.maximum(a, np.exp(-((r - r0) / w) ** 2) * s)
    a += 0.25 * np.exp(-(((r - 60) % 22) / 3.0) ** 2) * (r < 120)
    a *= np.clip((125 - r) / 10.0, 0, 1)
    out = np.zeros((256, 256, 4))
    out[..., :3] = 255
    out[..., 3] = np.clip(a, 0, 1) * 255
    save("mercury_ripple", out)


def droplet():
    im = Img(64, 64)
    pts = [(32, 3)]
    for t in np.linspace(-1.0, 1.0, 21):
        ang = math.pi * 0.5 + t * math.pi * 0.92
        pts.append((32 + math.cos(ang) * 15 * (0.55 + 0.45 * abs(t)), 44 + math.sin(ang) * 15))
    im.poly(pts, col=235)
    im.disc(26, 40, 4.0, col=255)
    save("mercury_droplet", glow(im.out(), 2.0, 0.5))


def streak():
    rng = np.random.default_rng(17)
    yy, xx = np.mgrid[0:256, 0:64]
    w = 14 * (1 - np.abs(yy - 128) / 150.0) ** 0.6 + 2.0
    a = np.clip((w - np.abs(xx - 31.5)) / 1.5, 0, 1)
    a *= 0.75 + 0.25 * np.sin(yy * 0.33)
    for k in range(6):                                            # beads
        cy = 20 + k * 40 + float(rng.random()) * 8
        a = np.maximum(a, np.clip(1 - np.sqrt(((xx - 31.5) / 8.0) ** 2 + ((yy - cy) / 6.0) ** 2), 0, 1))
    out = np.zeros((256, 64, 4))
    out[..., :3] = 255
    out[..., 3] = np.clip(a, 0, 1) * 255
    save("mercury_streak", out)


def crown():
    im = Img(128, 128)
    c = 64
    for k in range(16):
        a = 2 * math.pi * k / 16
        h = 52 if k % 2 == 0 else 38
        base_l, base_r = im.polar(c, c, 22, a - 0.12), im.polar(c, c, 22, a + 0.12)
        im.poly([base_l, im.polar(c, c, h, a), base_r], col=240)
        im.disc(*im.polar(c, c, h + 4, a), 2.4 if k % 2 == 0 else 1.6)
    im.ring(c, c, 22, 3.0)
    im.ring(c, c, 14, 1.4, a=190)
    save("mercury_crown", glow(im.out(), 2.5, 0.5))


def spearhead():
    im = Img(64, 128)
    im.poly([(32, 2), (46, 70), (32, 62), (18, 70)], col=235)
    im.poly([(32, 2), (46, 70), (32, 62)], col=255)
    im.poly([(32, 62), (36, 126), (28, 126)], col=215)
    im.line(32, 6, 32, 60, 1.0, 255, 190)
    im.rect(22, 68, 42, 72, col=255)
    save("mercury_spearhead", glow(im.out(), 3.0, 0.6))


def grid():
    rng = np.random.default_rng(23)
    im = Img(256, 256)
    for k in range(9):
        x = 14 + k * 28
        pts = [(x + math.sin(y * 0.04 + k) * 3.0, y) for y in range(0, 257, 8)]
        for i in range(len(pts) - 1):
            im.line(*pts[i], *pts[i + 1], 2.4)
    for j in range(5):
        y = 20 + j * 54
        pts = [(x, y + math.sin(x * 0.05 + j) * 3.0) for x in range(0, 257, 8)]
        for i in range(len(pts) - 1):
            im.line(*pts[i], *pts[i + 1], 1.4, a=190)
    for k in range(18):
        im.disc(14 + 28 * int(rng.integers(0, 9)) + math.sin(0.5), 20 + 54 * int(rng.integers(0, 5)), 3.0)
    save("mercury_grid", glow(im.out(), 2.0, 0.4))


if __name__ == "__main__":
    ripple(); droplet(); streak(); crown(); spearhead(); grid()

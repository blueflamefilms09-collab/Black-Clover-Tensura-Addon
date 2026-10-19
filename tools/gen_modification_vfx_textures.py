"""Particle textures of the Modification Magic effects (vfx/client/layer/ModificationLayer.java): textures/particle/modification_*.png.

    python3 -B tools/gen_modification_vfx_textures.py

ring      a rune ring with a diagonal lattice inside and bracket marks, the edit circle (256)
needle    a long dissection needle with a ring head (32 x 256)
tendril   a clawed tendril: banded segments tapering to a three-hook claw (64 x 256)
lattice   a hexagonal lattice that fades toward its rim, the structure laid bare (256)
slice     a clean cut: a hairline with a flare and two edge flecks, 8:1 (256 x 32)
shard     a fragment of broken structure (64)
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from transmutation_vfx_kit import Img, save, glow, radial  # noqa: E402


def ring():
    rng = np.random.default_rng(21)
    im = Img(256, 256)
    c = 128
    im.ring(c, c, 124, 3.0)
    im.ring(c, c, 112, 1.2)
    im.ring(c, c, 70, 2.0)
    im.ticks(c, c, 112, 124, 36, 1.2)
    for k in range(12):
        a = 2 * math.pi * k / 12
        x, y = im.polar(c, c, 91, a)
        im.glyph(x, y, 3.0, rng, 1.3)
    for k in range(-6, 7):                                        # the lattice inside the inner ring: two families of diagonals clipped to the ring by drawing then masking
        pass
    arr = im.out()
    yy, xx = np.mgrid[0:256, 0:256]
    r = np.sqrt((xx - c) ** 2 + (yy - c) ** 2)
    lat = np.zeros((256, 256))
    for k in range(-12, 13):
        d1 = np.abs(((xx - yy) - k * 14.0 + 7) % 14.0 - 7)
        d2 = np.abs(((xx + yy) - k * 14.0 + 7) % 14.0 - 7)
        lat = np.maximum(lat, np.maximum(1 - d1 / 0.9, 0))
        lat = np.maximum(lat, np.maximum(1 - d2 / 0.9, 0))
    lat = np.clip(lat, 0, 1) * (r < 66) * 150
    arr[..., 3] = np.maximum(arr[..., 3], lat)
    arr[..., :3] = 255
    for k in range(4):                                            # bracket marks outside
        a = math.pi / 4 + k * math.pi / 2
        x0, y0 = im.polar(c, c, 127, a)
    save("modification_ring", glow(arr, 2.4, 0.6))


def needle():
    im = Img(32, 256)
    im.ring(16, 12, 9, 2.4)
    im.ring(16, 12, 4, 1.2)
    im.rect(14, 22, 18, 40, col=235)
    im.poly([(15, 40), (17, 40), (16.4, 246), (15.6, 246)], col=255)
    im.line(16, 40, 16, 246, 0.9, 255, 255)
    im.rect(12, 38, 20, 41, col=200)
    save("modification_needle", glow(im.out(), 2.0, 0.6))


def tendril():
    im = Img(64, 256)
    y = 250
    widths = [20, 18, 16, 14, 12, 10, 8, 6]
    for i, w in enumerate(widths):
        h = 26
        im.rect(32 - w / 2, y - h, 32 + w / 2, y, col=240 if i % 2 == 0 else 190)
        im.rect(32 - w / 2, y - 3, 32 + w / 2, y, col=255)
        y -= h + 1
    for k, ang in enumerate((-0.5, 0.0, 0.5)):                    # the claw
        x0, y0 = 32, y + 2
        im.poly([(x0 - 2, y0), (x0 + 2, y0), (x0 + math.sin(ang) * 34 + 0.8, y0 - math.cos(ang) * 34), (x0 + math.sin(ang) * 34 - 0.8, y0 - math.cos(ang) * 34)], col=255)
    save("modification_tendril", glow(im.out(), 2.0, 0.5))


def lattice():
    im = Img(256, 256)
    s = 22.0
    for row in range(-8, 9):
        for col in range(-8, 9):
            cx = 128 + col * s * 1.5
            cy = 128 + row * s * math.sqrt(3) + (s * math.sqrt(3) / 2 if col % 2 else 0)
            pts = [im.polar(cx, cy, s * 0.92, math.pi / 3 * k) for k in range(6)]
            for k in range(6):
                im.line(*pts[k], *pts[(k + 1) % 6], 1.4)
    arr = im.out()
    arr[..., 3] *= radial(256, 256, 0.7)
    arr[..., :3] = 255
    save("modification_lattice", arr)


def slice_():
    im = Img(256, 32)
    im.poly([(0, 16), (128, 12), (256, 16), (128, 20)], col=255)
    im.line(0, 16, 256, 16, 1.2, 255, 255)
    arr = im.out()
    yy, xx = np.mgrid[0:32, 0:256]
    arr[..., 3] = np.maximum(arr[..., 3], np.exp(-((yy - 16) / 5.0) ** 2) * (1 - np.abs(xx - 128) / 128.0) * 140)
    arr[..., :3] = 255
    save("modification_slice", glow(arr, 2.0, 0.4))


def shard():
    rng = np.random.default_rng(31)
    im = Img(64, 64)
    pts = [(32 + math.cos(a) * r, 32 + math.sin(a) * r) for a, r in ((0.2, 26), (1.3, 14), (2.3, 24), (3.5, 12), (4.4, 28), (5.5, 16))]
    im.poly(pts, col=225, outline=255, ow=1.6)
    im.line(*pts[0], *pts[3], 1.0, 150, 220)
    im.line(*pts[2], *pts[5], 1.0, 170, 200)
    save("modification_shard", glow(im.out(), 2.0, 0.4))


if __name__ == "__main__":
    ring(); needle(); tendril(); lattice(); slice_(); shard()

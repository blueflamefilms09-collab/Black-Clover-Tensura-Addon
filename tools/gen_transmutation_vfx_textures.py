"""Particle textures of the Transmutation Magic effects (vfx/client/layer/TransmutationLayer.java): textures/particle/transmutation_*.png.

    python3 -B tools/gen_transmutation_vfx_textures.py

circle      the alchemist's circle: a double outer ring with 48 ticks and glyph boxes, a hexagram, an inner ring with three nodes (256)
band        a ribbon of glyph boxes between two rules, 16:1 (512 x 32), for hoops that climb round a zone
spike       a tapering iron spike with a lit edge (64 x 128)
star        a four-pointed sparkle with a halo (64)
petal       a five-petal flower, what Flowering turns a spell into (64)
ring        a flat shock ring with a thick glow and a thin rune line (256)
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from transmutation_vfx_kit import Img, save, glow, radial  # noqa: E402


def circle():
    rng = np.random.default_rng(7)
    im = Img(256, 256)
    c = 128
    im.ring(c, c, 124, 3.0)
    im.ring(c, c, 118, 1.2)
    im.ring(c, c, 96, 2.0)
    im.ring(c, c, 90, 1.0)
    im.ticks(c, c, 118, 124, 48, 1.0)
    for k in range(16):                                           # glyph boxes between the two outer rings
        a = 2 * math.pi * k / 16
        x, y = im.polar(c, c, 107, a)
        im.rect(x - 5, y - 5, x + 5, y + 5, col=None, outline=255, ow=1.0)
        im.glyph(x, y, 2.4, rng, 1.1)
    im.star(c, c, 88, 6, w=2.0, skip=2)
    im.ring(c, c, 52, 2.0)
    im.ring(c, c, 44, 1.0)
    for k in range(3):
        x, y = im.polar(c, c, 88, -math.pi / 2 + 2 * math.pi * k / 3)
        im.ring(x, y, 9, 2.0)
        im.disc(x, y, 3.5)
    im.disc(c, c, 6)
    for k in range(6):
        x0, y0 = im.polar(c, c, 52, 2 * math.pi * k / 6)
        x1, y1 = im.polar(c, c, 88 * 0.5, 2 * math.pi * k / 6)
        im.line(x0, y0, x1, y1, 1.0)
    save("transmutation_circle", glow(im.out(), 2.5, 0.6))


def band():
    rng = np.random.default_rng(11)
    im = Img(512, 32)
    im.line(0, 3, 512, 3, 2.0)
    im.line(0, 29, 512, 29, 2.0)
    im.line(0, 6, 512, 6, 0.8)
    im.line(0, 26, 512, 26, 0.8)
    for k in range(16):
        x = 16 + 32 * k
        im.rect(x - 11, 8, x + 11, 24, col=None, outline=255, ow=1.2)
        im.glyph(x, 16, 5.0, rng, 1.6)
    save("transmutation_rune_band", im.out())


def spike():
    im = Img(64, 128)
    im.poly([(32, 2), (50, 118), (14, 118)], col=210, a=255)
    im.poly([(32, 2), (50, 118), (36, 118)], col=255, a=255)            # the lit edge
    im.poly([(32, 2), (14, 118), (24, 118)], col=130, a=255)
    im.line(32, 8, 32, 116, 1.4, 255, 190)
    save("transmutation_spike", glow(im.out(), 3.0, 0.5))


def star():
    im = Img(64, 64)
    for ang in (0, math.pi / 2):
        x0, y0 = im.polar(32, 32, 30, ang)
        x1, y1 = im.polar(32, 32, 30, ang + math.pi)
        im.poly([(x0, y0), im.polar(32, 32, 3.2, ang + math.pi / 2), (x1, y1), im.polar(32, 32, 3.2, ang - math.pi / 2)], col=255)
    for ang in (math.pi / 4, 3 * math.pi / 4):
        x0, y0 = im.polar(32, 32, 14, ang)
        x1, y1 = im.polar(32, 32, 14, ang + math.pi)
        im.poly([(x0, y0), im.polar(32, 32, 2.0, ang + math.pi / 2), (x1, y1), im.polar(32, 32, 2.0, ang - math.pi / 2)], col=255, a=190)
    arr = glow(im.out(), 4.0, 0.8)
    arr[..., 3] = np.maximum(arr[..., 3], radial(64, 64, 2.4) * 200)
    save("transmutation_star", arr)


def petal():
    im = Img(64, 64)
    for k in range(5):
        a = -math.pi / 2 + 2 * math.pi * k / 5
        pts = [im.polar(32, 32, 3, a)]
        for t in np.linspace(-0.55, 0.55, 9):
            pts.append(im.polar(32, 32, 26 * (1 - 0.35 * abs(t)), a + t * 0.9))
        im.poly(pts, col=235, a=255)
        im.line(*im.polar(32, 32, 4, a), *im.polar(32, 32, 22, a), 0.8, 150, 200)
    im.disc(32, 32, 5, col=255)
    save("transmutation_petal", glow(im.out(), 2.0, 0.5))


def ring():
    im = Img(256, 256)
    im.ring(128, 128, 120, 14.0, a=140)
    im.ring(128, 128, 120, 4.0)
    im.ring(128, 128, 108, 1.0)
    im.ticks(128, 128, 100, 108, 64, 1.0, a=200)
    save("transmutation_ring", glow(im.out(), 5.0, 0.7))


if __name__ == "__main__":
    circle(); band(); spike(); star(); petal(); ring()

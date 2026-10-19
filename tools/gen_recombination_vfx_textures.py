"""Particle textures of the Recombination Magic effects (vfx/client/layer/RecombinationLayer.java): textures/particle/recombination_*.png.

    python3 -B tools/gen_recombination_vfx_textures.py

plank       a board with grain lines and two nails (128 x 32), grey-scale so the wood colour is the vertex colour
blueprint   a floor plan: rooms, door arcs, dimension marks and a dashed circle (256)
smoke       a soft cluster of puffs (128)
post        a timber post with a bracket (32 x 128)
spark       a nail-spark: a small cross with a halo (32)
ring        an octagonal ring of bricks / joints, the house's footprint (256)
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from transmutation_vfx_kit import Img, save, glow, radial  # noqa: E402


def plank():
    rng = np.random.default_rng(3)
    im = Img(128, 32)
    im.rect(1, 3, 126, 29, col=215)
    im.rect(1, 3, 126, 5, col=255)
    im.rect(1, 27, 126, 29, col=150)
    for k in range(7):                                            # grain
        y = 7 + k * 3 + float(rng.random())
        x0 = float(rng.random()) * 30
        im.line(x0, y, x0 + 50 + float(rng.random()) * 60, y + float(rng.random()) - 0.5, 0.8, 160, 200)
    im.disc(8, 16, 2.0, col=90)
    im.disc(120, 16, 2.0, col=90)
    im.rect(1, 3, 126, 29, col=None, outline=110, ow=1.2)
    save("recombination_plank", im.out())


def blueprint():
    rng = np.random.default_rng(5)
    im = Img(256, 256)
    for k in range(40):                                           # a dashed circle
        a0 = 2 * math.pi * k / 40
        a1 = a0 + 2 * math.pi / 80
        im.line(*im.polar(128, 128, 122, a0), *im.polar(128, 128, 122, a1), 1.6)
    im.rect(56, 56, 200, 200, col=None, outline=255, ow=2.4)
    im.rect(56, 56, 128, 112, col=None, outline=255, ow=1.6)
    im.rect(128, 56, 200, 128, col=None, outline=255, ow=1.6)
    im.rect(56, 112, 112, 200, col=None, outline=255, ow=1.6)
    im.rect(112, 128, 200, 200, col=None, outline=255, ow=1.6)
    for (x, y, r, a0) in ((92, 112, 16, 0), (128, 90, 14, 90), (112, 160, 14, 180), (170, 128, 14, 270)):         # door arcs
        for k in range(10):
            t0 = math.radians(a0 + 9 * k)
            t1 = math.radians(a0 + 9 * (k + 1))
            im.line(*im.polar(x, y, r, t0), *im.polar(x, y, r, t1), 1.0, a=210)
        im.line(x, y, *im.polar(x, y, r, math.radians(a0)), 1.0, a=210)
    for k in range(4):                                            # corner marks
        sx = -1 if k % 2 == 0 else 1
        sy = -1 if k < 2 else 1
        cx, cy = 128 + sx * 86, 128 + sy * 86
        im.line(cx, cy, cx - sx * 14, cy, 2.0)
        im.line(cx, cy, cx, cy - sy * 14, 2.0)
    for y in (40, 216):                                           # dimension lines
        im.line(56, y, 200, y, 1.0, a=200)
        im.line(56, y - 5, 56, y + 5, 1.0)
        im.line(200, y - 5, 200, y + 5, 1.0)
    for k in range(5):
        x = 70 + float(rng.random()) * 120
        y = 70 + float(rng.random()) * 120
        im.disc(x, y, 2.0, a=180)
    save("recombination_blueprint", glow(im.out(), 2.0, 0.4))


def smoke():
    rng = np.random.default_rng(9)
    yy, xx = np.mgrid[0:128, 0:128]
    out = np.zeros((128, 128, 4))
    for k in range(9):
        cx, cy = 64 + float(rng.normal(0, 14)), 64 + float(rng.normal(0, 14))
        r = 18 + float(rng.random()) * 14
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / r
        out[..., 3] = np.maximum(out[..., 3], np.clip(1 - d, 0, 1) ** 1.4 * 150)
    out[..., :3] = 235
    save("recombination_smoke", out)


def post():
    im = Img(32, 128)
    im.rect(10, 4, 22, 124, col=205)
    im.rect(10, 4, 13, 124, col=255)
    im.rect(19, 4, 22, 124, col=140)
    im.rect(5, 18, 27, 24, col=120)
    im.rect(5, 100, 27, 106, col=120)
    for y in (21, 103):
        im.disc(8, y, 1.4, col=230)
        im.disc(24, y, 1.4, col=230)
    im.rect(10, 4, 22, 124, col=None, outline=90, ow=1.0)
    save("recombination_post", im.out())


def spark():
    im = Img(32, 32)
    im.line(16, 3, 16, 29, 2.4)
    im.line(3, 16, 29, 16, 2.4)
    im.line(8, 8, 24, 24, 1.2, a=200)
    im.line(24, 8, 8, 24, 1.2, a=200)
    im.disc(16, 16, 3.0)
    arr = glow(im.out(), 3.0, 0.9)
    arr[..., 3] = np.maximum(arr[..., 3], radial(32, 32, 2.0) * 190)
    save("recombination_spark", arr)


def ring():
    rng = np.random.default_rng(13)
    im = Img(256, 256)
    pts = [im.polar(128, 128, 122, math.pi / 8 + 2 * math.pi * k / 8) for k in range(8)]
    inner = [im.polar(128, 128, 98, math.pi / 8 + 2 * math.pi * k / 8) for k in range(8)]
    for pp, w in ((pts, 3.2), (inner, 2.4)):
        for k in range(8):
            im.line(*pp[k], *pp[(k + 1) % 8], w)
    for k in range(8):                                            # joints: short radial bars like the ends of beams, and a brick pattern between
        im.line(*pts[k], *inner[k], 2.0)
        for j in range(1, 5):
            t = j / 5
            x0, y0 = pts[k][0] * (1 - t) + inner[k][0] * t, pts[k][1] * (1 - t) + inner[k][1] * t
            x1, y1 = pts[(k + 1) % 8][0] * (1 - t) + inner[(k + 1) % 8][0] * t, pts[(k + 1) % 8][1] * (1 - t) + inner[(k + 1) % 8][1] * t
            im.line(x0, y0, x1, y1, 0.8, a=170)
    im.ring(128, 128, 64, 2.0)
    im.ticks(128, 128, 56, 64, 24, 1.2)
    save("recombination_ring", glow(im.out(), 2.5, 0.5))


if __name__ == "__main__":
    plank(); blueprint(); smoke(); post(); spark(); ring()

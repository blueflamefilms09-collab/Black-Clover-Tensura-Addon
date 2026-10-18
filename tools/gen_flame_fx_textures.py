"""Flame Magic (rebuild 1.93.0): the sprite textures of the Flame VFX layer (vfx/client/layer/FlameFxLayer.java) and of the armour's extras (client/aura/FlameAura.java).
White sprites whose alpha is the shape (the vertex colour tints them); written to textures/particle/flamefx_*.png:

    flamefx_flame    the tongue lying down, tip to the right (ribbons run tail to tip along u)
    flamefx_tongue   a single flame tongue, S-curved, teardrop, hot core (rises from the bottom edge)
    flamefx_ring     a ring of fire with flame teeth on its outer rim (ground rings, halo)
    flamefx_ember    a spark with a four-point star
    flamefx_lion     the sun lion sigil: ring, sixteen rays, the mane and the lion's face cut out of the disc
    flamefx_burst    a starburst of flame spikes (impact flash)
    flamefx_lick     a flame streak running to the right, tip at the right edge (trails, beams)
    flamefx_smoke    a soft rising puff
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_fx_lib import Canvas, rot  # noqa: E402


def tongue():
    c = Canvas(96, 192, seed=11)
    n = c.noise(3, 3, 5)
    pts = []
    L = []
    R = []
    for i in range(0, 21):
        t = i / 20.0
        y = 188 - t * 176
        sway = math.sin(t * 5.0) * 9 * t
        w = (1 - t) ** 0.8 * 40 * (0.35 + 0.65 * math.sin(min(1.0, t * 5.5 + 0.2) * math.pi / 2))
        L.append((48 + sway - w, y))
        R.append((48 + sway + w, y))
    c.polygon(L + R[::-1], soft=2.0, val=1.0, hot=0.0)
    inner = []
    inner2 = []
    for i in range(0, 15):
        t = i / 14.0 * 0.7
        y = 184 - t * 176
        sway = math.sin(t * 5.0) * 9 * t
        w = (1 - t / 0.75) ** 0.9 * 17
        inner.append((48 + sway - w, y))
        inner2.append((48 + sway + w, y))
    cc = Canvas(96, 192, seed=12)
    cc.polygon(inner + inner2[::-1], soft=3.0, val=1.0)
    c.core = np.maximum(c.core, cc.m)
    c.blur(0.6)
    c.fade_edges(0.03)
    from PIL import Image
    c.image().transpose(Image.ROTATE_270).save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle", "flamefx_flame.png"))   # the same flame lying down, tip to the right: what a ribbon (tail -> tip) wants
    return c.save("flamefx_tongue")


def ring():
    c = Canvas(256, seed=13)
    c.ring(128, 128, 98, 7, soft=1.8, val=1.0, hot=0.9)
    c.ring(128, 128, 82, 3, soft=1.4, val=0.7, hot=0.4)
    rng = np.random.default_rng(3)
    for i in range(36):
        a = i / 36 * math.tau + rng.uniform(-0.04, 0.04)
        h = rng.uniform(12, 26)
        base = rot((128 + 98, 128), (128, 128), a)
        tip = rot((128 + 98 + h, 128 + rng.uniform(-3, 3)), (128, 128), a)
        l = rot((128 + 98, 128 - 6), (128, 128), a)
        r = rot((128 + 98, 128 + 6), (128, 128), a)
        c.polygon([l, tip, r], soft=1.2, val=0.9, hot=0.5)
    for i in range(8):                                                   # inner glyph ticks
        a = i / 8 * math.tau
        c.stroke([rot((128 + 66, 128), (128, 128), a), rot((128 + 78, 128), (128, 128), a)], 4, 2, soft=1.2, val=0.8, hot=0.4)
    c.glow(128, 128, 120, power=3.0, val=0.12, hot=0.0)
    c.fade_edges(0.02)
    return c.save("flamefx_ring")


def ember():
    c = Canvas(64, seed=14)
    c.glow(32, 32, 30, power=2.4, val=0.9, hot=0.9)
    for a in (0.0, math.pi / 2):
        p0 = rot((2, 32), (32, 32), a)
        p1 = rot((62, 32), (32, 32), a)
        c.stroke([p0, (32, 32), p1], 2.2, 2.2, soft=1.2, val=0.85, hot=0.9, fade=(0.1, 0.1))
        c.stroke([p0, (32, 32)], 3.0, 0.4, soft=1.0, val=0.9, hot=0.9)
        c.stroke([(32, 32), p1], 3.0, 0.4, soft=1.0, val=0.9, hot=0.9)
    c.disc(32, 32, 4.5, soft=2.0, val=1.0, hot=1.0)
    c.fade_edges(0.05)
    return c.save("flamefx_ember")


def lion():
    c = Canvas(256, seed=15)
    c.ring(128, 128, 118, 6, soft=1.6, val=1.0, hot=0.6)
    c.ring(128, 128, 104, 3, soft=1.4, val=0.8, hot=0.3)
    for i in range(16):                                                  # the sixteen rays: long and short
        a = i / 16 * math.tau
        L = 20 if i % 2 == 0 else 12
        c.polygon([rot((128 + 120, 128 - 5), (128, 128), a), rot((128 + 120 + L, 128), (128, 128), a), rot((128 + 120, 128 + 5), (128, 128), a)], soft=1.0, val=0.95, hot=0.5)
    # the mane: a ring of curled tufts around the face
    for i in range(14):
        a = i / 14 * math.tau
        p0 = rot((128 + 56, 128), (128, 128), a)
        p1 = rot((128 + 86, 128 + 14), (128, 128), a)
        p2 = rot((128 + 100, 128 - 2), (128, 128), a)
        c.polygon([rot((128 + 52, 128 - 12), (128, 128), a), p1, p2, rot((128 + 52, 128 + 12), (128, 128), a)], soft=1.0, val=0.85, hot=0.3)
    c.disc(128, 128, 54, soft=1.4, val=1.0, hot=0.5)
    carve = Canvas(256, seed=16)
    for sx in (-1, 1):                                                   # eyes: slanted slits
        carve.polygon([(128 + sx * 12, 112), (128 + sx * 34, 104), (128 + sx * 30, 117), (128 + sx * 14, 120)], soft=0.8, val=1.0)
    carve.polygon([(118, 126), (138, 126), (128, 144)], soft=0.8, val=1.0)                     # the nose
    carve.stroke([(128, 144), (128, 152), (112, 162)], 3, 2, soft=0.8, val=1.0)                # the mouth
    carve.stroke([(128, 152), (144, 162)], 3, 2, soft=0.8, val=1.0)
    carve.polygon([(112, 160), (118, 160), (115, 170)], soft=0.6, val=1.0)                     # fangs
    carve.polygon([(138, 160), (144, 160), (141, 170)], soft=0.6, val=1.0)
    c.m = np.clip(c.m - carve.m * 1.2, 0, 1)
    c.core = np.clip(c.core - carve.m, 0, 1)
    c.fade_edges(0.01)
    return c.save("flamefx_lion")


def burst():
    c = Canvas(256, seed=17)
    rng = np.random.default_rng(5)
    for i in range(22):
        a = i / 22 * math.tau + rng.uniform(-0.08, 0.08)
        L = rng.uniform(70, 124) if i % 2 == 0 else rng.uniform(40, 80)
        w = rng.uniform(7, 13)
        c.polygon([rot((128 + 10, 128 - w), (128, 128), a), rot((128 + L, 128), (128, 128), a), rot((128 + 10, 128 + w), (128, 128), a)], soft=1.4, val=0.95, hot=0.55)
    c.glow(128, 128, 70, power=1.8, val=0.9, hot=1.0)
    c.disc(128, 128, 20, soft=3, val=1.0, hot=1.0)
    c.fade_edges(0.02)
    return c.save("flamefx_burst")


def lick():
    c = Canvas(256, 64, seed=18)
    rng = np.random.default_rng(7)
    pts = [(4 + i * 12.0, 32 + math.sin(i * 0.7) * 6 * (1 - i / 22.0)) for i in range(22)]
    c.stroke(pts, 30, 1.5, soft=2.0, val=1.0, hot=0.0, fade=(0.5, 1.0))
    c.stroke(pts[1:], 14, 1.0, soft=2.0, val=1.0, hot=1.0, fade=(0.6, 1.0))
    for k in range(6):                                                   # side licks
        i = rng.integers(3, 17)
        x, y = pts[i]
        c.stroke([(x, y), (x + 18, y + rng.uniform(-14, 14)), (x + 34, y + rng.uniform(-18, 18))], 8, 1, soft=1.4, val=0.8, hot=0.5)
    c.fade_edges(0.04)
    return c.save("flamefx_lick")


def smoke():
    c = Canvas(128, seed=19)
    n = c.noise(4, 3, 9)
    d = np.hypot(c.xx - 128, c.yy - 128) / 128.0
    c.m = np.clip((1 - d) * 1.6, 0, 1) ** 1.5 * np.clip(n * 1.5 - 0.1, 0, 1)
    c.core = c.m * 0.3
    c.fade_edges(0.1)
    return c.save("flamefx_smoke")


def main():
    for f in (tongue, ring, ember, lion, burst, lick, smoke):
        print("wrote", f())


if __name__ == "__main__":
    main()

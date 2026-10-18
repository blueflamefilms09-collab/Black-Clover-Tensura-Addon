"""Dark Magic (rebuild 1.93.0): the sprite textures of the Dark VFX layer (vfx/client/layer/DarkFxLayer.java) and of the armour's extras (client/aura/DarkAura.java).
White sprites whose alpha is the shape (the vertex colour tints them; most are drawn alpha-blended in black or additive in violet); written to textures/particle/darkrb_*.png:

    darkrb_slash     a thin crescent cut streak, tapered at both ends, lying along u (slashes, katana cuts)
    darkrb_moon      the black moon: a disc with a corona of rays and a bright thin rim
    darkrb_hole      the accretion disc: a bright inner ring, spiral arms, a dark centre hole
    darkrb_tendril   a tapering tendril of darkness, wavy, head at the right
    darkrb_rune      the dark sigil: nested rings, a six-point star and eight blade ticks
    darkrb_wisp      a soft rising puff of shadow
    darkrb_crack     a jagged crack of the void with branches (dimension cuts)
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_fx_lib import Canvas, rot  # noqa: E402


def slash():
    c = Canvas(256, 64, seed=31)
    pts = [(6 + i * 11.2, 32 + 16 * math.sin(i / 22.0 * math.pi) ** 1.0 * -0.0 - 11 * math.sin((i / 22.0) * math.pi)) for i in range(23)]
    c.stroke(pts, 30, 1.0, soft=2.5, val=1.0, hot=0.0, fade=(0.2, 1.0))
    top = [(x, y - 0.3) for x, y in pts]
    c.stroke(pts, 8, 1.0, soft=1.6, val=1.0, hot=1.0, fade=(0.3, 1.0))
    # a bright inner edge on the lower side
    c.stroke([(x, y + 5) for x, y in pts[2:-2]], 2.0, 0.6, soft=1.2, val=0.6, hot=0.6)
    c.fade_edges(0.03)
    return c.save("darkrb_slash")


def moon():
    c = Canvas(256, seed=32)
    n = c.noise(3, 3, 3)
    for i in range(36):                                                  # the corona
        a = i / 36 * math.tau
        L = 14 + 18 * ((i * 7) % 5) / 4.0
        c.polygon([rot((128 + 78, 128 - 4), (128, 128), a), rot((128 + 78 + L, 128), (128, 128), a), rot((128 + 78, 128 + 4), (128, 128), a)], soft=1.2, val=0.7, hot=0.2)
    c.disc(128, 128, 80, soft=2.0, val=1.0, hot=0.1)
    c.ring(128, 128, 80, 5, soft=1.6, val=1.0, hot=1.0)
    c.glow(128, 128, 126, power=2.6, val=0.35, hot=0.2)
    # craters: darker pits (carve a little so the disc has a face)
    carve = Canvas(256, seed=33)
    for cx, cy, r in ((98, 100, 14), (150, 90, 10), (140, 150, 18), (96, 152, 8), (170, 128, 7)):
        carve.disc(cx, cy, r, soft=3.0, val=0.35)
    c.m = np.clip(c.m - carve.m, 0, 1)
    c.fade_edges(0.01)
    return c.save("darkrb_moon")


def hole():
    c = Canvas(256, seed=34)
    for k in range(3):                                                   # spiral arms of the accretion disc
        pts = []
        for i in range(40):
            t = i / 39.0
            a = k * math.tau / 3 + t * 5.2
            r = 40 + t * 80
            pts.append((128 + math.cos(a) * r, 128 + math.sin(a) * r))
        c.stroke(pts, 14, 3, soft=2.0, val=0.8, hot=0.4, fade=(1.0, 0.1))
    c.ring(128, 128, 46, 12, soft=2.0, val=1.0, hot=1.0)
    c.ring(128, 128, 62, 5, soft=1.6, val=0.7, hot=0.5)
    c.ring(128, 128, 88, 3, soft=1.4, val=0.45, hot=0.2)
    carve = Canvas(256, seed=35)
    carve.disc(128, 128, 38, soft=3.0, val=1.0)
    c.m = np.clip(c.m - carve.m, 0, 1)
    c.core = np.clip(c.core - carve.m, 0, 1)
    c.fade_edges(0.02)
    return c.save("darkrb_hole")


def tendril():
    c = Canvas(256, 64, seed=36)
    pts = [(6 + i * 10.8, 32 + 10 * math.sin(i * 0.5) * (1 - i / 24.0) + math.sin(i * 1.3) * 2) for i in range(24)]
    c.stroke(pts, 20, 2, soft=2.0, val=1.0, hot=0.0, fade=(0.4, 1.0))
    c.stroke(pts[1:], 6, 1, soft=1.4, val=1.0, hot=1.0, fade=(0.5, 1.0))
    rng = np.random.default_rng(8)
    for k in range(5):
        i = int(rng.integers(3, 18))
        x, y = pts[i]
        c.stroke([(x, y), (x + 14, y + rng.uniform(-12, 12)), (x + 26, y + rng.uniform(-16, 16))], 6, 1, soft=1.2, val=0.8, hot=0.4)
    c.fade_edges(0.04)
    return c.save("darkrb_tendril")


def rune():
    c = Canvas(256, seed=37)
    c.ring(128, 128, 118, 6, soft=1.6, val=1.0, hot=0.8)
    c.ring(128, 128, 98, 3, soft=1.4, val=0.7, hot=0.3)
    c.ring(128, 128, 54, 3, soft=1.4, val=0.7, hot=0.3)
    for k in range(2):                                                   # the six-point star: two triangles
        pts = [rot((128 + 94, 128), (128, 128), k * math.pi / 3 + i * math.tau / 3) for i in range(3)]
        for i in range(3):
            c.stroke([pts[i], pts[(i + 1) % 3]], 4, 4, soft=1.2, val=0.9, hot=0.5)
    for i in range(8):                                                   # blade ticks between the rings
        a = i / 8 * math.tau + math.pi / 8
        c.polygon([rot((128 + 98, 128 - 4), (128, 128), a), rot((128 + 116, 128), (128, 128), a), rot((128 + 98, 128 + 4), (128, 128), a)], soft=1.0, val=0.9, hot=0.4)
    c.disc(128, 128, 10, soft=2.0, val=1.0, hot=1.0)
    c.fade_edges(0.01)
    return c.save("darkrb_rune")


def wisp():
    c = Canvas(128, seed=38)
    n = c.noise(4, 3, 12)
    d = np.hypot(c.xx - 128, c.yy - 128) / 128.0
    c.m = np.clip((1 - d) * 1.5, 0, 1) ** 1.4 * np.clip(n * 1.6 - 0.15, 0, 1)
    c.core = c.m * 0.2
    c.fade_edges(0.1)
    return c.save("darkrb_wisp")


def crack():
    c = Canvas(256, 128, seed=39)
    rng = np.random.default_rng(9)

    def branch(x, y, ang, L, w, depth):
        pts = [(x, y)]
        for i in range(8):
            ang += rng.uniform(-0.45, 0.45)
            x += math.cos(ang) * L / 8
            y += math.sin(ang) * L / 8
            pts.append((x, y))
        c.stroke(pts, w, w * 0.2, soft=1.2, val=1.0, hot=1.0, fade=(1.0, 0.6))
        if depth > 0:
            for i in (3, 6):
                branch(pts[i][0], pts[i][1], ang + rng.choice([-1, 1]) * rng.uniform(0.6, 1.0), L * 0.5, w * 0.6, depth - 1)
    branch(8, 64, 0.0, 240, 8, 2)
    c.glow(128, 64, 100, power=3.0, val=0.12, hot=0.0)
    c.fade_edges(0.03)
    return c.save("darkrb_crack")


def main():
    for f in (slash, moon, hole, tendril, rune, wisp, crack):
        print("wrote", f())


if __name__ == "__main__":
    main()

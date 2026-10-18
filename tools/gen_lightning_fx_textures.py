"""Lightning Magic (rebuild 1.93.0): the sprite textures of the Lightning VFX layer (vfx/client/layer/LightningFxLayer.java) and of the armour's extras (client/aura/LightningAura.java).
White sprites whose alpha is the shape (the vertex colour tints them; all additive); written to textures/particle/lightningfx_*.png:

    lightningfx_bolt     a forked bolt standing up the sprite (a pillar strike, an arc seen from the side), glowing core, thin forks
    lightningfx_arc      a jagged arc lying along u (arcs between points, trails, ribbons), a thin hot core with a soft sheath
    lightningfx_ring     an electric ring with sparks leaping off it (ground rings, shock rings)
    lightningfx_flash    a four-point flash star with long diagonal rays (impact flash)
    lightningfx_feather  one feather of lightning: a rib with barbs of tiny bolts (wing sprites)
    lightningfx_sigil    the thunder-drum sigil: a ring of seven drums, spokes and a bolt in the middle
    lightningfx_spark    a tiny crackling spark with a star
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_fx_lib import Canvas, rot  # noqa: E402


def jag(rng, p0, p1, n, amp, taper=True):
    pts = [p0]
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    L = math.hypot(dx, dy)
    nx, ny = -dy / L, dx / L
    for i in range(1, n):
        t = i / n
        k = amp * (math.sin(t * math.pi) if taper else 1.0) * rng.uniform(-1, 1)
        pts.append((p0[0] + dx * t + nx * k, p0[1] + dy * t + ny * k))
    pts.append(p1)
    return pts


def bolt():
    c = Canvas(128, 256, seed=51)
    rng = np.random.default_rng(1)
    main = jag(rng, (64, 4), (64, 252), 14, 22)
    c.stroke(main, 14, 7, soft=2.5, val=0.55, hot=0.0)
    c.stroke(main, 6, 3.5, soft=1.6, val=1.0, hot=1.0)
    for i in (3, 6, 9, 11):
        x, y = main[i]
        d = rng.choice([-1, 1])
        br = jag(rng, (x, y), (x + d * rng.uniform(26, 48), y + rng.uniform(26, 56)), 5, 8)
        c.stroke(br, 5, 1.5, soft=1.2, val=0.85, hot=0.8)
    c.glow(64, 128, 70, power=2.4, val=0.22, hot=0.0)
    c.fade_edges(0.03)
    return c.save("lightningfx_bolt")


def arc():
    c = Canvas(256, 64, seed=52)
    rng = np.random.default_rng(2)
    main = jag(rng, (4, 32), (252, 32), 16, 14)
    c.stroke(main, 12, 4, soft=2.5, val=0.5)
    c.stroke(main, 4.5, 2, soft=1.4, val=1.0, hot=1.0)
    for i in (4, 8, 12):
        x, y = main[i]
        br = jag(rng, (x, y), (x + rng.uniform(20, 36), y + rng.choice([-1, 1]) * rng.uniform(14, 26)), 4, 5)
        c.stroke(br, 3.5, 1, soft=1.1, val=0.8, hot=0.7)
    c.fade_edges(0.03)
    return c.save("lightningfx_arc")


def ring():
    c = Canvas(256, seed=53)
    rng = np.random.default_rng(3)
    pts = []
    for i in range(72):
        a = i / 72 * math.tau
        r = 104 + rng.uniform(-5, 5)
        pts.append((128 + math.cos(a) * r, 128 + math.sin(a) * r))
    pts.append(pts[0])
    c.stroke(pts, 8, 8, soft=2.0, val=0.6)
    c.stroke(pts, 3.2, 3.2, soft=1.2, val=1.0, hot=1.0)
    for i in range(18):                                                  # sparks leaping off the ring
        a = i / 18 * math.tau + rng.uniform(-0.1, 0.1)
        p0 = rot((128 + 104, 128), (128, 128), a)
        p1 = rot((128 + 104 + rng.uniform(12, 26), 128 + rng.uniform(-10, 10)), (128, 128), a)
        c.stroke(jag(rng, p0, p1, 3, 4), 3, 1, soft=1.0, val=0.9, hot=0.8)
    c.ring(128, 128, 72, 2.4, soft=1.2, val=0.45, hot=0.2)
    c.glow(128, 128, 120, power=3.0, val=0.12, hot=0.0)
    c.fade_edges(0.02)
    return c.save("lightningfx_ring")


def flash():
    c = Canvas(256, seed=54)
    for k, (L, w) in enumerate(((124, 10), (124, 10), (88, 6), (88, 6))):
        a = k * math.pi / 2 if k < 2 else math.pi / 4 + (k - 2) * math.pi / 2
        for s in (-1, 1):
            c.polygon([rot((128 + 8, 128 - w), (128, 128), a + (0 if s > 0 else math.pi)), rot((128 + L, 128), (128, 128), a + (0 if s > 0 else math.pi)),
                       rot((128 + 8, 128 + w), (128, 128), a + (0 if s > 0 else math.pi))], soft=1.4, val=0.95, hot=0.7)
    c.glow(128, 128, 64, power=1.8, val=0.9, hot=1.0)
    c.disc(128, 128, 14, soft=3, val=1.0, hot=1.0)
    c.fade_edges(0.02)
    return c.save("lightningfx_flash")


def feather():
    c = Canvas(128, 256, seed=55)
    rng = np.random.default_rng(5)
    rib = [(64, 250), (66, 190), (62, 130), (66, 70), (64, 8)]
    c.stroke(rib, 7, 2, soft=1.6, val=1.0, hot=1.0)
    for i in range(10):                                                  # barbs of tiny bolts
        t = i / 10.0
        x, y = 64, 235 - t * 220
        for s in (-1, 1):
            L = (1 - t * 0.7) * 56
            c.stroke(jag(rng, (x, y), (x + s * L, y - 24 - 10 * t), 4, 4), 4.2 * (1 - 0.5 * t), 1, soft=1.1, val=0.9, hot=0.6)
    c.glow(64, 128, 80, power=2.5, val=0.18, hot=0.0)
    c.fade_edges(0.03)
    return c.save("lightningfx_feather")


def sigil():
    c = Canvas(256, seed=56)
    c.ring(128, 128, 118, 5, soft=1.6, val=1.0, hot=0.8)
    c.ring(128, 128, 62, 3, soft=1.4, val=0.7, hot=0.3)
    for i in range(7):                                                   # the seven drums
        a = i / 7 * math.tau - math.pi / 2
        cx, cy = 128 + math.cos(a) * 92, 128 + math.sin(a) * 92
        c.disc(cx, cy, 22, soft=1.4, val=0.9, hot=0.4)
        c.ring(cx, cy, 14, 3, soft=1.2, val=1.0, hot=0.8)
        c.stroke([(128 + math.cos(a) * 64, 128 + math.sin(a) * 64), (cx - math.cos(a) * 22, cy - math.sin(a) * 22)], 3, 3, soft=1.0, val=0.7, hot=0.3)
    zz = [(128, 86), (112, 116), (134, 118), (116, 170)]
    c.stroke(zz, 9, 3, soft=1.4, val=1.0, hot=1.0)
    c.fade_edges(0.01)
    return c.save("lightningfx_sigil")


def spark():
    c = Canvas(64, seed=57)
    c.glow(32, 32, 28, power=2.6, val=0.9, hot=0.9)
    for a in (0.0, math.pi / 4, math.pi / 2, 3 * math.pi / 4):
        c.stroke([rot((4, 32), (32, 32), a), (32, 32), rot((60, 32), (32, 32), a)], 2.0, 2.0, soft=1.1, val=0.85, hot=0.9, fade=(0.15, 0.15))
    c.disc(32, 32, 4.5, soft=2.0, val=1.0, hot=1.0)
    c.fade_edges(0.05)
    return c.save("lightningfx_spark")


def main():
    for f in (bolt, arc, ring, flash, feather, sigil, spark):
        print("wrote", f())


if __name__ == "__main__":
    main()

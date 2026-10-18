"""Water Magic (rebuild 1.93.0): the sprite textures of the Water VFX layer (vfx/client/layer/WaterFxLayer.java) and of the armour's extras (client/aura/WaterAura.java).
White sprites whose alpha is the shape (the vertex colour tints them); written to textures/particle/waterfx_*.png:

    waterfx_stream   a flowing stream lying down, head at the right: wavy edges, a bright thread, droplets shed along it (ribbons, beams, the dragon's body)
    waterfx_ripple   concentric ripples with crest ticks (ground rings, shock rings)
    waterfx_drop     a teardrop with a highlight
    waterfx_sigil    the sea sigil: wave ring, twelve crests, a trident in the middle
    waterfx_wave     a breaking wave crest curling over (crash flashes, walls of water)
    waterfx_spray    a burst of droplets and mist
    waterfx_head     a sea dragon's head in profile facing right (the roar's head sprite)
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_fx_lib import Canvas, rot  # noqa: E402


def stream():
    c = Canvas(256, 64, seed=21)
    rng = np.random.default_rng(2)
    top = [(6 + i * 11.0, 32 - (9 + 4 * math.sin(i * 0.55)) * (0.35 + 0.65 * min(1, i / 5.0)) * (1 - 0.7 * (i / 22.0) ** 2) + math.sin(i * 0.9) * 2) for i in range(23)]
    bot = [(x, 64 - y + math.sin(x * 0.07) * 2) for x, y in top]
    c.polygon(top + bot[::-1], soft=2.0, val=0.82)
    c.stroke([(x, 32 + math.sin(x * 0.06) * 3) for x, _ in top], 6, 2, soft=2.0, val=1.0, hot=1.0, fade=(0.5, 1.0))
    for i in range(10):                                                  # droplets shed along the stream
        x = rng.uniform(30, 240)
        y = 32 + rng.choice([-1, 1]) * rng.uniform(13, 22)
        c.disc(x, y, rng.uniform(1.6, 3.0), soft=1.4, val=0.9, hot=0.6)
    c.fade_edges(0.03)
    return c.save("waterfx_stream")


def ripple():
    c = Canvas(256, seed=22)
    for k, (r, w, v) in enumerate(((108, 5, 1.0), (86, 3, 0.7), (64, 2.5, 0.5), (44, 2, 0.35))):
        c.ring(128, 128, r, w, soft=1.6, val=v, hot=0.8 if k == 0 else 0.3)
    for i in range(24):                                                  # crest ticks on the outer ring
        a = i / 24 * math.tau
        c.stroke([rot((128 + 108, 128), (128, 128), a), rot((128 + 120, 128), (128, 128), a + 0.05)], 3, 0.8, soft=1.0, val=0.8, hot=0.4)
    c.glow(128, 128, 118, power=3.0, val=0.10, hot=0.0)
    c.fade_edges(0.02)
    return c.save("waterfx_ripple")


def drop():
    c = Canvas(64, seed=23)
    pts = []
    for i in range(0, 25):
        t = i / 24.0 * math.pi
        pts.append((32 + math.sin(t) * 15 * (0.4 + 0.6 * t / math.pi), 52 - (1 - math.cos(t)) * 20))
    c.polygon([(32, 6)] + [(32 + math.sin(t / 12.0 * math.pi) * 17, 40 + math.cos(t / 12.0 * math.pi) * -1 * 0 + 6 - math.cos(t / 12.0 * math.pi) * 14) for t in range(0, 13)] + [(32 - math.sin(t / 12.0 * math.pi) * 17, 46 - math.cos(t / 12.0 * math.pi) * 14 + 0) for t in range(12, -1, -1)], soft=1.6, val=0.9)
    c.disc(32, 40, 15, soft=1.8, val=0.9, hot=0.2)
    c.disc(26, 36, 4.5, soft=2.0, val=1.0, hot=1.0)
    c.fade_edges(0.05)
    return c.save("waterfx_drop")


def sigil():
    c = Canvas(256, seed=24)
    c.ring(128, 128, 118, 5, soft=1.6, val=1.0, hot=0.7)
    c.ring(128, 128, 100, 3, soft=1.4, val=0.7, hot=0.3)
    for i in range(12):                                                  # twelve wave crests round the ring (curled arcs)
        a = i / 12 * math.tau
        pts = []
        for k in range(10):
            t = k / 9.0
            ang = a - 0.15 + 0.3 * t
            r = 82 + 10 * math.sin(t * math.pi)
            pts.append((128 + math.cos(ang) * r, 128 + math.sin(ang) * r))
        c.stroke(pts, 5, 1.5, soft=1.2, val=0.9, hot=0.4)
    c.ring(128, 128, 62, 2.5, soft=1.2, val=0.6)
    for dx, L in ((-22, 54), (0, 70), (22, 54)):                         # the trident
        c.stroke([(128 + dx, 128 + 56), (128 + dx, 128 + 56 - L)], 6, 4, soft=1.2, val=1.0, hot=0.7)
        c.polygon([(128 + dx - 7, 128 + 56 - L + 2), (128 + dx, 128 + 56 - L - 16), (128 + dx + 7, 128 + 56 - L + 2)], soft=1.0, val=1.0, hot=0.7)
    c.stroke([(128 - 22, 128 + 30), (128 + 22, 128 + 30)], 6, 6, soft=1.2, val=1.0, hot=0.5)
    c.fade_edges(0.01)
    return c.save("waterfx_sigil")


def wave():
    c = Canvas(256, 128, seed=25)
    # the crest: a rising face on the left, the lip curling over to the right and down
    face = []
    for i in range(0, 31):
        t = i / 30.0
        x = 10 + t * 130
        y = 120 - (t ** 1.4) * 96
        face.append((x, y))
    lip = []
    for i in range(1, 22):
        t = i / 21.0
        a = -math.pi * 0.5 + t * math.pi * 1.25
        lip.append((150 + math.cos(a) * 52 * (1 - 0.25 * t), 40 + math.sin(a) * 32 * (1 + 0.1 * t) + 6 * t))
    c.polygon([(10, 124)] + face + lip + [(236, 124)], soft=2.0, val=0.8)
    c.stroke(face + lip, 7, 2, soft=1.6, val=1.0, hot=1.0)
    rng = np.random.default_rng(4)
    for i in range(14):
        a = rng.uniform(-0.6, 1.3) * math.pi
        r = rng.uniform(50, 78)
        c.disc(150 + math.cos(a) * r * 0.9, 40 + math.sin(a) * r * 0.55, rng.uniform(1.4, 3.2), soft=1.2, val=0.9, hot=0.6)
    c.fade_edges(0.03)
    return c.save("waterfx_wave")


def spray():
    c = Canvas(128, seed=26)
    rng = np.random.default_rng(6)
    n = c.noise(4, 3, 3)
    d = np.hypot(c.xx - 128, c.yy - 128) / 128.0
    c.m = np.clip((1 - d) * 1.5, 0, 1) ** 1.6 * np.clip(n * 1.4 - 0.1, 0, 1) * 0.55
    for i in range(34):
        a = rng.uniform(0, math.tau)
        r = rng.uniform(12, 56)
        c.disc(64 + math.cos(a) * r, 64 + math.sin(a) * r, rng.uniform(1.2, 3.4), soft=1.2, val=0.95, hot=0.7)
    c.glow(64, 64, 24, power=2.0, val=0.5, hot=0.8)
    c.fade_edges(0.06)
    return c.save("waterfx_spray")


def head():
    c = Canvas(256, 128, seed=27)
    # a sea dragon's head in profile, facing right: skull, long jaw, a frill of fins behind, an eye
    c.polygon([(60, 70), (110, 44), (176, 46), (232, 62), (238, 74), (190, 80), (176, 96), (120, 100), (74, 92)], soft=1.6, val=0.88)       # skull and snout
    c.polygon([(186, 80), (236, 84), (200, 98), (150, 104)], soft=1.4, val=0.8)                                                          # lower jaw
    for i in range(5):                                                   # the frill: five swept fins
        a = -0.5 + i * 0.28
        base = (96 + i * 6, 52 + i * 4)
        c.polygon([base, (base[0] - 70 * math.cos(a), base[1] - 48 * math.sin(a + 1.0) - 6), (base[0] - 40, base[1] + 6)], soft=1.2, val=0.85, hot=0.3)
    c.polygon([(126, 44), (150, 12), (160, 46)], soft=1.0, val=0.9, hot=0.3)                                                             # the horn
    for i in range(6):                                                   # teeth
        c.polygon([(196 + i * 6, 79), (198 + i * 6, 88), (200 + i * 6, 79)], soft=0.6, val=1.0, hot=0.8)
    c.disc(150, 62, 7, soft=1.6, val=1.0, hot=1.0)
    c.stroke([(70, 74), (110, 70), (160, 74), (220, 70)], 2.2, 1.2, soft=1.0, val=0.5, hot=0.5)
    c.fade_edges(0.03)
    return c.save("waterfx_head")


def main():
    for f in (stream, ripple, drop, sigil, wave, spray, head):
        print("wrote", f())


if __name__ == "__main__":
    main()

"""Spatial Magic textures (1.93.0 rebuild): the particle sprites of the SPATIAL_FX1..3 layer, the Spatial armour's ground rings and glints, and the props.

    python3 -B tools/gen_spatial_textures.py     # -> src/main/resources/assets/nusmp/textures/particle/spatial_*.png

White (or a light tint) with the shape in the alpha, so the effect colour (the grimoire's 0xB088FF violet) tints them:
  spatial_ring    a gate ring: two rims, 48 ticks, a band of runes, a soft inner glow (ground ring of the armour, the FX2 field wall)
  spatial_sigil   a ground sigil: outer rim, a square and a triangle turned inside it, rune marks (signature cast, FX2 floor)
  spatial_star    a four-pointed sparkle with a long vertical ray
  spatial_mote    a soft dot with a hot core
  spatial_rift    a vertical slit of folded space with a white seam and jagged lips (128 x 256; the Spatial Cut, FX1 / FX3)
  spatial_shard   a glass-like fragment (flying debris)
  spatial_fold    the inside of a gate: spiral arms of folded space round a bright eye
  spatial_crack   fracture lines radiating from a point (space cracks where a cut lands)
  spatial_lens    a thin double ring with a faint halo (the lensing rim of a collapse)
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_tex_kit import *   # noqa: E402,F401,F403


def ring():
    m = Mask(256)
    m.ring(0.96, 0.012)
    m.ring(0.90, 0.006, 0.8)
    m.ring(0.58, 0.010, 0.9)
    m.ring(0.52, 0.005, 0.7)
    m.ticks(0.90, 0.955, 48, 0.006, 0.9)
    m.ticks(0.58, 0.64, 24, 0.008, 0.9, phase=0.07)
    for i in range(16):                                          # the band of runes between the rims
        a = 2 * math.pi * i / 16
        m.rune_glyph(math.cos(a) * 0.74, math.sin(a) * 0.74, 0.045, 100 + i, 0.95, 0.010)
    m.add(glow(256, 3.0) * 0.18)
    m.add(blur(m.a, 3.0), 0.35)
    save("spatial_ring", m.rgba(edge=(190, 160, 255)))


def sigil():
    m = Mask(256)
    m.ring(0.97, 0.014)
    m.ring(0.86, 0.007, 0.8)
    for k in range(3):                                            # an inscribed square and two triangles
        a = k * math.pi / 6
        m.line([(0.82 * math.cos(a + i * math.pi / 2 + math.pi / 4), 0.82 * math.sin(a + i * math.pi / 2 + math.pi / 4)) for i in range(5)], 0.009, 0.85 - 0.2 * k)
    for k in range(2):
        a = k * math.pi
        m.line([(0.6 * math.cos(a + i * 2 * math.pi / 3 - math.pi / 2), 0.6 * math.sin(a + i * 2 * math.pi / 3 - math.pi / 2)) for i in range(4)], 0.009, 0.9)
    m.ring(0.34, 0.008, 0.9)
    m.ticks(0.86, 0.95, 36, 0.006, 0.9)
    for i in range(12):
        a = 2 * math.pi * i / 12 + 0.26
        m.rune_glyph(math.cos(a) * 0.76, math.sin(a) * 0.76, 0.04, 300 + i, 0.9, 0.009)
    m.disc(0.08, 1.0)
    m.add(glow(256, 2.6) * 0.14)
    save("spatial_sigil", m.rgba(edge=(190, 160, 255)))


def star():
    m = Mask(64)
    m.star(4, 0.98, 0.07, 1.0, blur_px=0.4)
    m.star(4, 0.45, 0.16, 0.9, phase=math.pi / 4, blur_px=0.4)
    m.add(glow(64, 2.4) * 0.8)
    save("spatial_star", m.rgba())


def mote():
    m = Mask(32)
    m.add(glow(32, 1.8))
    m.disc(0.3, 1.0, 0.4)
    save("spatial_mote", m.rgba())


def rift():
    w, h = 128, 256
    m = Mask(w, h)
    rng = random.Random(5)
    # the body of the slit: a lens, widest in the middle, with jagged lips
    top, bot = [], []
    n = 40
    for i in range(n + 1):
        t = i / n
        y = -0.97 + 1.94 * t
        half = 0.50 * math.sin(math.pi * t) ** 0.8
        j = rng.uniform(-0.05, 0.05) * (1 - abs(2 * t - 1))
        top.append((-half - j - (0.03 if i % 2 else 0.0), y))
        bot.append((half + j + (0.03 if i % 3 == 0 else 0.0), y))
    m.poly(top + bot[::-1], 0.62)
    # the hot seam
    seam = [(rng.uniform(-0.03, 0.03) * math.sin(math.pi * i / 24), -0.95 + 1.9 * i / 24) for i in range(25)]
    m.line(seam, 0.035, 1.0, 0.6)
    m.line(seam, 0.012, 1.0)
    # small cracks leaving the lips
    for i in range(7):
        y = rng.uniform(-0.7, 0.7)
        s = rng.choice((-1, 1))
        half = 0.50 * math.sin(math.pi * (y + 0.97) / 1.94) ** 0.8
        m.line([(s * half, y), (s * (half + rng.uniform(0.1, 0.3)), y + rng.uniform(-0.12, 0.12))], 0.012, 0.8)
    m.add(blur(m.a, 4.0), 0.5)
    save("spatial_rift", m.rgba(edge=(190, 160, 255), core=(255, 250, 255)))


def shard():
    m = Mask(64)
    m.poly([(-0.2, -0.95), (0.35, -0.2), (0.15, 0.95), (-0.35, 0.1)], 0.85)
    m.poly([(-0.2, -0.95), (0.05, -0.1), (-0.35, 0.1)], 1.0)
    m.line([(-0.2, -0.95), (0.15, 0.95)], 0.03, 0.9)
    m.add(blur(m.a, 1.2), 0.4)
    save("spatial_shard", m.rgba(edge=(200, 180, 255)))


def fold():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x, y = (xx - 127.5) / 127.5, (yy - 127.5) / 127.5
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    arms = 0.5 + 0.5 * np.sin(5 * th + r * 11.0)
    body = smooth(1.0, 0.94, r) * (0.25 + 0.55 * arms * (1.0 - 0.4 * r))
    eye = np.exp(-(r / 0.16) ** 2) * 1.0
    rim = np.exp(-((r - 0.96) / 0.025) ** 2) * 0.9
    a = np.clip(body + eye + rim, 0, 1)
    save("spatial_fold", compose_rgba(np.ones((n, n, 3)) * 255, a))


def crack():
    m = Mask(128)
    rng = random.Random(9)
    for i in range(9):
        a = 2 * math.pi * i / 9 + rng.uniform(-0.2, 0.2)
        pts = [(0, 0)]
        r = 0.0
        while r < 0.95:
            r += rng.uniform(0.1, 0.22)
            a += rng.uniform(-0.25, 0.25)
            pts.append((math.cos(a) * r, math.sin(a) * r))
        m.line(pts, 0.016 * (1.0 if i % 2 else 0.7), 0.95)
        if len(pts) > 3:
            b = a + rng.choice((-1, 1)) * 0.8
            q = pts[2]
            m.line([q, (q[0] + math.cos(b) * 0.3, q[1] + math.sin(b) * 0.3)], 0.01, 0.8)
    m.add(glow(128, 3.0) * 0.4)
    save("spatial_crack", m.rgba(edge=(190, 160, 255)))


def lens():
    m = Mask(256)
    m.ring(0.92, 0.010)
    m.ring(0.86, 0.004, 0.7)
    m.add(blur(m.a, 3.0), 0.8)
    m.add(glow(256, 5.0) * 0.12)
    save("spatial_lens", m.rgba(edge=(190, 160, 255)))


if __name__ == "__main__":
    for f in (ring, sigil, star, mote, rift, shard, fold, crack, lens):
        f()

"""Slash Magic textures (1.93.0 rebuild): the sprites of the SLASH_FX1..3 layer, the armour's ground ring and glints, and the props.

    python3 -B tools/gen_slash_textures.py       # -> src/main/resources/assets/nusmp/textures/particle/slash_rip_*.png

White (tinted by the effect colour, green 0x48FF7A); the existing slash_arc / slash_blade / slash_crescent textures stay where they are.
  slash_rip_crescent   a wide jagged crescent, a bright thin edge on the outside, torn teeth on the inside (256 x 128)
  slash_rip_cross      two crossing cut marks, each a long tapered wedge with a white core (the crossed impact of Lunatic Slash)
  slash_rip_streak     a tall thin tapered streak with a hot head (a flying cut seen edge-on, speed lines)
  slash_rip_shard      a long blade shard
  slash_rip_ring       a serrated ring: a rim with outward teeth (the ground ring of the armour, the scythe's sweep)
  slash_rip_glint      a four-pointed glint with a long horizontal blade of light
  slash_rip_mark       one diagonal cut mark with a faint tear around it (a wound in the air: the adapt marks, the cut of the dash)
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_tex_kit import *   # noqa: E402,F401,F403


def crescent():
    w, h = 256, 128
    m = Mask(w, h)
    rng = random.Random(3)
    # outer edge: a big arc; inner edge: the same arc pulled in by a swelling profile
    n = 60
    outer, inner = [], []
    for i in range(n + 1):
        t = i / n
        a = math.pi * (0.12 + 0.76 * t)
        x, y = -math.cos(a) * 0.96, 0.92 - math.sin(a) * 1.7 + 0.0
        outer.append((x, y))
    for i in range(n, -1, -1):
        t = i / n
        a = math.pi * (0.12 + 0.76 * t)
        depth = 0.62 * math.sin(math.pi * t) ** 0.85
        jag = rng.uniform(-0.035, 0.035) * math.sin(math.pi * t)
        r = 1.7 - depth + jag
        x, y = -math.cos(a) * (0.96 - depth * 0.1), 0.92 - math.sin(a) * r
        inner.append((x, y))
    m.poly(outer + inner, 0.7)
    m.line(outer, 0.03, 1.0, 0.5)
    m.line(outer, 0.012, 1.0)
    for i in range(10):                                                  # torn teeth on the inside
        t = rng.uniform(0.15, 0.85)
        a = math.pi * (0.12 + 0.76 * t)
        depth = 0.62 * math.sin(math.pi * t) ** 0.85
        r0 = 1.7 - depth
        p0 = (-math.cos(a) * 0.96, 0.92 - math.sin(a) * r0)
        p1 = (-math.cos(a) * 0.96, 0.92 - math.sin(a) * (r0 - rng.uniform(0.08, 0.2)))
        m.line([p0, p1], 0.014, 0.85)
    m.add(blur(m.a, 3.0), 0.45)
    save("slash_rip_crescent", m.rgba(edge=(110, 255, 150), core=(255, 255, 255)))


def cross():
    m = Mask(256)
    for ang, off in ((0.62, 0.0), (-0.62, 0.05)):
        c, s = math.cos(ang), math.sin(ang)
        pts = [(-c * 0.95 + s * 0.0, -s * 0.95), (-s * 0.07 + c * 0.0 + 0.0, c * 0.07), (c * 0.95, s * 0.95), (s * 0.07, -c * 0.07)]
        m.poly(pts, 0.8)
        m.line([(-c * 0.95, -s * 0.95), (c * 0.95, s * 0.95)], 0.012, 1.0)
    m.disc(0.07, 1.0, 1.0)
    m.add(blur(m.a, 3.0), 0.5)
    m.add(glow(256, 3.0) * 0.18)
    save("slash_rip_cross", m.rgba(edge=(110, 255, 150), core=(255, 255, 255)))


def streak():
    n_w, n_h = 64, 256
    yy, xx = np.mgrid[0:n_h, 0:n_w].astype(np.float32)
    x = (xx - 31.5) / 32.0
    y = yy / (n_h - 1.0)
    width = 0.05 + 0.4 * (1.0 - y) ** 1.6
    core = np.exp(-(x / width) ** 2 * 2.5)
    a = core * (0.3 + 0.7 * (1.0 - y) ** 0.7)
    a += np.exp(-((x / 0.05) ** 2)) * (1 - y) * 0.6
    save("slash_rip_streak", compose_rgba(np.ones((n_h, n_w, 3)) * 255, np.clip(a, 0, 1)))


def shard():
    m = Mask(64, 128)
    m.poly([(0.0, -0.98), (0.30, 0.1), (0.12, 0.98), (-0.2, 0.2)], 0.85)
    m.line([(0.0, -0.98), (0.12, 0.98)], 0.04, 1.0)
    m.poly([(0.0, -0.98), (0.1, -0.3), (-0.2, 0.2)], 1.0)
    m.add(blur(m.a, 1.2), 0.4)
    save("slash_rip_shard", m.rgba(edge=(110, 255, 150), core=(255, 255, 255)))


def ring():
    m = Mask(256)
    m.ring(0.80, 0.018)
    m.ring(0.70, 0.006, 0.8)
    n = 28
    for i in range(n):                                                   # teeth leaning forward
        a = 2 * math.pi * i / n
        c, s = math.cos(a), math.sin(a)
        r0, r1 = 0.80, 0.97
        m.poly([(c * r0 - s * 0.045, s * r0 + c * 0.045), (c * r1 + s * 0.02, s * r1 - c * 0.02), (c * r0 + s * 0.045, s * r0 - c * 0.045)], 0.95)
    m.ticks(0.56, 0.66, 20, 0.007, 0.8, phase=0.1)
    m.add(glow(256, 3.0) * 0.14)
    save("slash_rip_ring", m.rgba(edge=(110, 255, 150)))


def glint():
    m = Mask(128)
    m.poly([(-0.98, 0), (0, 0.045), (0.98, 0), (0, -0.045)], 1.0, 0.4)
    m.poly([(0, -0.55), (0.06, 0), (0, 0.55), (-0.06, 0)], 1.0, 0.4)
    m.star(4, 0.4, 0.1, 0.9, phase=math.pi / 4)
    m.add(glow(128, 2.4) * 0.8)
    save("slash_rip_glint", m.rgba())


def mark():
    m = Mask(128)
    m.poly([(-0.9, 0.7), (-0.05, 0.02), (0.9, -0.7), (0.05, -0.02)], 0.85)
    m.line([(-0.9, 0.7), (0.9, -0.7)], 0.025, 1.0)
    m.line([(-0.5, 0.45), (-0.62, 0.7)], 0.012, 0.8)
    m.line([(0.4, -0.3), (0.55, -0.12)], 0.012, 0.8)
    m.add(blur(m.a, 2.5), 0.5)
    save("slash_rip_mark", m.rgba(edge=(110, 255, 150), core=(255, 255, 255)))


if __name__ == "__main__":
    for f in (crescent, cross, streak, shard, ring, glint, mark):
        f()

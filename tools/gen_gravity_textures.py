"""Gravity Magic textures (1.93.0 rebuild): the particle sprites of the GRAVITY_FX1..3 layer, the armour's ground sigil and glints, and the props.

    python3 -B tools/gen_gravity_textures.py     # -> src/main/resources/assets/nusmp/textures/particle/gravity_*.png

White or light (tinted by the effect colour, violet 0xB45AFF) unless said:
  gravity_ring     a pressure ring: a double rim with short ticks pointing inwards (the rings of a well that crowd towards the centre)
  gravity_disc     (coloured) a black sphere with a bright thin rim and a faint lensing halo: the singularity seen head-on (alpha blended, black)
  gravity_chevron  a stack of downward chevrons fading upwards: compressing lanes, falling pressure
  gravity_crack    ground fractures radiating from a point, thick near the middle, with small branches
  gravity_mote     a soft dot that is dark in the middle (a falling particle)
  gravity_sigil    a ground sigil: a rim, a ring of down arrows and a square knot (the Demon King's presence)
  gravity_shard    a rock chunk with a lit top edge (flying debris)
  gravity_streak   a vertical pressure streak (a tall tapered line, brighter at the foot)
  gravity_lens     a thin double ring with a faint halo (the lensing rim of a collapse)
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_tex_kit import *   # noqa: E402,F401,F403


def ring():
    m = Mask(256)
    m.ring(0.96, 0.014)
    m.ring(0.88, 0.006, 0.8)
    for i in range(36):                                              # ticks pointing inwards
        a = 2 * math.pi * i / 36
        c, s = math.cos(a), math.sin(a)
        r0, r1 = 0.78, 0.88 if i % 3 else 0.84
        m.line([(c * r0, s * r0), (c * r1, s * r1)], 0.008, 0.9)
    m.ring(0.62, 0.008, 0.7)
    m.add(glow(256, 3.0) * 0.16)
    save("gravity_ring", m.rgba(edge=(190, 130, 255)))


def disc():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x, y = (xx - 127.5) / 127.5, (yy - 127.5) / 127.5
    r = np.sqrt(x * x + y * y)
    body = smooth(0.63, 0.58, r)
    rim = np.exp(-((r - 0.64) / 0.022) ** 2)
    halo = np.exp(-((r - 0.66) / 0.14) ** 2) * 0.55
    halo2 = np.exp(-((r - 0.9) / 0.05) ** 2) * 0.25 * (r < 0.98)
    a = np.clip(body + rim * 0.95 + halo * (r > 0.6) + halo2, 0, 1)
    col = np.zeros((n, n, 3), np.float32)
    inside = (r < 0.6)[..., None]
    col[...] = np.array([230, 190, 255], np.float32)
    col = np.where(inside, np.array([6, 2, 14], np.float32), col)
    col = np.where(((r >= 0.6) & (r < 0.65))[..., None], np.array([255, 230, 255], np.float32) * rim[..., None] + np.array([90, 40, 160], np.float32) * (1 - rim[..., None]), col)
    save("gravity_disc", compose_rgba(col, a))


def chevron():
    m = Mask(128, 256)
    for k in range(5):
        y = -0.8 + k * 0.38
        w = 0.78 - k * 0.04
        m.line([(-w, y), (0, y + 0.30), (w, y)], 0.06 + 0.012 * k, 0.35 + 0.15 * k, 0.5)
    m.mul(np.linspace(0.45, 1.0, 256, dtype=np.float32)[:, None] ** 0.8)
    m.add(blur(m.a, 2.0), 0.4)
    save("gravity_chevron", m.rgba(edge=(190, 130, 255)))


def crack():
    m = Mask(256)
    rng = random.Random(21)
    for i in range(11):
        a = 2 * math.pi * i / 11 + rng.uniform(-0.15, 0.15)
        pts = [(0, 0)]
        r = 0.0
        w0 = 0.03
        while r < 0.97:
            r += rng.uniform(0.08, 0.2)
            a += rng.uniform(-0.22, 0.22)
            pts.append((math.cos(a) * r, math.sin(a) * r))
        for j in range(len(pts) - 1):
            wd = w0 * (1.0 - j / max(1, len(pts) - 1) * 0.8)
            m.line([pts[j], pts[j + 1]], wd, 0.95)
        for j in (2, 4):
            if j < len(pts):
                b = a + rng.choice((-1, 1)) * rng.uniform(0.6, 1.0)
                q = pts[j]
                m.line([q, (q[0] + math.cos(b) * 0.22, q[1] + math.sin(b) * 0.22)], 0.012, 0.8)
    m.disc(0.1, 1.0, 1.0)
    m.add(glow(256, 2.6) * 0.3)
    save("gravity_crack", m.rgba(edge=(200, 140, 255)))


def mote():
    n = 32
    r = np.clip(radial(n), 0, 1)
    a = (1 - r) ** 1.6
    col = np.zeros((n, n, 3), np.float32)
    col[...] = np.array([220, 170, 255], np.float32) * (r[..., None] ** 0.7) + np.array([20, 8, 40], np.float32) * (1 - r[..., None] ** 0.7)
    save("gravity_mote", compose_rgba(col, a))


def sigil():
    m = Mask(256)
    m.ring(0.97, 0.014)
    m.ring(0.84, 0.007, 0.8)
    m.ring(0.30, 0.008, 0.9)
    for i in range(12):                                              # a ring of down arrows
        a = 2 * math.pi * i / 12
        c, s = math.cos(a), math.sin(a)
        px, py = c * 0.66, s * 0.66
        tx, ty = -s, c
        m.line([(px - c * 0.10, py - s * 0.10), (px + c * 0.10, py + s * 0.10)], 0.010, 0.95)
        m.poly([(px - c * 0.10 + tx * 0.06, py - s * 0.10 + ty * 0.06), (px - c * 0.10 - tx * 0.06, py - s * 0.10 - ty * 0.06), (px - c * 0.16, py - s * 0.16)], 0.95)
    for k in range(2):                                               # a square knot
        a = k * math.pi / 4
        m.line([(0.48 * math.cos(a + i * math.pi / 2 + math.pi / 4), 0.48 * math.sin(a + i * math.pi / 2 + math.pi / 4)) for i in range(5)], 0.01, 0.9)
    m.ticks(0.84, 0.95, 48, 0.006, 0.9)
    m.disc(0.07, 1.0)
    m.add(glow(256, 2.8) * 0.14)
    save("gravity_sigil", m.rgba(edge=(190, 130, 255)))


def shard():
    m = Mask(64)
    m.poly([(-0.55, -0.1), (-0.2, -0.8), (0.45, -0.6), (0.7, 0.05), (0.3, 0.7), (-0.4, 0.6)], 0.8)
    m.poly([(-0.55, -0.1), (-0.2, -0.8), (0.45, -0.6), (0.2, -0.3)], 1.0)
    m.line([(-0.2, -0.8), (0.45, -0.6)], 0.04, 1.0)
    m.add(blur(m.a, 1.0), 0.3)
    save("gravity_shard", m.rgba(edge=(170, 120, 230)))


def streak():
    n_w, n_h = 64, 256
    yy, xx = np.mgrid[0:n_h, 0:n_w].astype(np.float32)
    x = (xx - 31.5) / 32.0
    y = yy / (n_h - 1.0)
    width = 0.15 + 0.5 * y
    core = np.exp(-(x / width) ** 2 * 3.0)
    a = core * (0.15 + 0.85 * y ** 1.4)
    a *= smooth(0.0, 0.1, y)
    save("gravity_streak", compose_rgba(np.ones((n_h, n_w, 3)) * 255, a))


def lens():
    m = Mask(256)
    m.ring(0.92, 0.010)
    m.ring(0.85, 0.004, 0.7)
    m.add(blur(m.a, 3.0), 0.8)
    m.add(glow(256, 5.0) * 0.1)
    save("gravity_lens", m.rgba(edge=(190, 130, 255)))


if __name__ == "__main__":
    for f in (ring, disc, chevron, crack, mote, sigil, shard, streak, lens):
        f()

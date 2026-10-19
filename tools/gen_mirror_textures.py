"""Mirror Magic textures (1.93.0 rebuild): the sprites of the MIRROR_FX1..3 layer, the Glass Garb's ground ring and glints, and the props. The older
mirror_frame_* / mirror_glass_* / mirror_corona / mirror_prism / mirror_ripple textures (tools/gen_mirror_vfx_textures.py) stay where they are.

    python3 -B tools/gen_mirror_textures.py      # -> src/main/resources/assets/nusmp/textures/particle/mirror_reflex_*.png

White or light (tinted by the effect colour, violet-silver 0xB89CFF) unless said:
  mirror_reflex_pane     a tall rectangular pane of glass: a bright rim, two diagonal reflection streaks, a soft gradient (128 x 256)
  mirror_reflex_ring     a halo ring with fine spokes and a double rim (the ground ring of the armour, the field floor)
  mirror_reflex_beam     a tall beam: a hot core, soft side bands (the ray, seen edge-on)
  mirror_reflex_shard    a faceted glass shard with a lit edge
  mirror_reflex_glint    a four-pointed glint with long diagonal rays
  mirror_reflex_sigil    a ground sigil of mirrored triangles and an eight-point star
  mirror_reflex_prism    (coloured) a rainbow streak: the light split by glass (alpha blended over additive layers)
  mirror_reflex_ripple   concentric ripples with a bright leading ring (the surface of a mirror world)
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_tex_kit import *   # noqa: E402,F401,F403


def pane():
    w, h = 128, 256
    m = Mask(w, h)
    m.poly([(-0.86, -0.97), (0.86, -0.97), (0.86, 0.97), (-0.86, 0.97)], 0.30)
    m.line([(-0.86, -0.97), (0.86, -0.97), (0.86, 0.97), (-0.86, 0.97), (-0.86, -0.97)], 0.05, 1.0)
    m.line([(-0.62, -0.90), (-0.62, 0.90)], 0.012, 0.5)
    for off, wd, k in ((-0.25, 0.10, 0.55), (0.18, 0.045, 0.75)):                                  # two diagonal reflection streaks
        m.poly([(off - 0.55 - wd, 0.9), (off - 0.55 + wd, 0.9), (off + 0.55 + wd, -0.9), (off + 0.55 - wd, -0.9)], k)
    m.add(blur(m.a, 2.0), 0.35)
    save("mirror_reflex_pane", m.rgba(edge=(200, 214, 255), core=(255, 255, 255)))


def ring():
    m = Mask(256)
    m.ring(0.96, 0.012)
    m.ring(0.90, 0.005, 0.8)
    for i in range(48):                                              # fine spokes between the rims
        a = 2 * math.pi * i / 48
        c, s = math.cos(a), math.sin(a)
        m.line([(c * 0.66, s * 0.66), (c * 0.89, s * 0.89)], 0.004, 0.6 if i % 2 else 0.9)
    m.ring(0.62, 0.008, 0.9)
    for i in range(8):                                               # eight lozenges
        a = 2 * math.pi * i / 8
        c, s = math.cos(a), math.sin(a)
        m.poly([(c * 0.80 - s * 0.03, s * 0.80 + c * 0.03), (c * 0.86, s * 0.86), (c * 0.80 + s * 0.03, s * 0.80 - c * 0.03), (c * 0.74, s * 0.74)], 0.95)
    m.add(glow(256, 3.0) * 0.14)
    save("mirror_reflex_ring", m.rgba(edge=(200, 214, 255)))


def beam():
    n_w, n_h = 64, 256
    yy, xx = np.mgrid[0:n_h, 0:n_w].astype(np.float32)
    x = (xx - 31.5) / 32.0
    y = yy / (n_h - 1.0)
    core = np.exp(-(x / 0.12) ** 2)
    side = 0.4 * np.exp(-((np.abs(x) - 0.55) / 0.12) ** 2)
    halo = 0.35 * np.exp(-(x / 0.5) ** 2)
    a = np.clip(core + side + halo, 0, 1)
    a *= smooth(0.0, 0.08, y) * smooth(1.0, 0.92, y)
    save("mirror_reflex_beam", compose_rgba(np.ones((n_h, n_w, 3)) * 255, np.clip(a, 0, 1)))


def shard():
    m = Mask(64, 128)
    m.poly([(-0.1, -0.98), (0.55, -0.1), (0.2, 0.98), (-0.55, 0.15)], 0.85)
    m.poly([(-0.1, -0.98), (0.1, -0.05), (-0.55, 0.15)], 1.0)
    m.line([(-0.1, -0.98), (0.2, 0.98)], 0.04, 1.0)
    m.line([(0.1, -0.05), (0.2, 0.98)], 0.02, 0.8)
    m.add(blur(m.a, 1.2), 0.4)
    save("mirror_reflex_shard", m.rgba(edge=(200, 214, 255), core=(255, 255, 255)))


def glint():
    m = Mask(128)
    m.poly([(-0.98, 0), (0, 0.04), (0.98, 0), (0, -0.04)], 1.0, 0.4)
    m.poly([(0, -0.98), (0.04, 0), (0, 0.98), (-0.04, 0)], 1.0, 0.4)
    for sgn in (-1, 1):
        m.poly([(-0.55 * sgn, -0.55), (0.025 * sgn, 0.0), (0.55 * sgn, 0.55), (-0.025 * sgn, 0.0)], 0.8, 0.4)
    m.star(4, 0.34, 0.1, 0.9, phase=math.pi / 4)
    m.add(glow(128, 2.2) * 0.85)
    save("mirror_reflex_glint", m.rgba())


def sigil():
    m = Mask(256)
    m.ring(0.97, 0.013)
    m.ring(0.88, 0.006, 0.8)
    for k in range(2):                                               # two mirrored triangle pairs
        a = k * math.pi / 3
        m.line([(0.8 * math.cos(a + i * 2 * math.pi / 3 + math.pi / 2), 0.8 * math.sin(a + i * 2 * math.pi / 3 + math.pi / 2)) for i in range(4)], 0.009, 0.9)
        m.line([(0.8 * math.cos(a + math.pi + i * 2 * math.pi / 3 + math.pi / 2), 0.8 * math.sin(a + math.pi + i * 2 * math.pi / 3 + math.pi / 2)) for i in range(4)], 0.009, 0.9)
    m.star(8, 0.52, 0.045, 0.95, inner=0.1)
    m.ring(0.30, 0.007, 0.9)
    m.ticks(0.88, 0.96, 64, 0.004, 0.8)
    m.disc(0.05, 1.0)
    m.add(glow(256, 2.8) * 0.12)
    save("mirror_reflex_sigil", m.rgba(edge=(200, 214, 255)))


def prism():
    n_w, n_h = 64, 256
    yy, xx = np.mgrid[0:n_h, 0:n_w].astype(np.float32)
    x = (xx - 31.5) / 32.0
    y = yy / (n_h - 1.0)
    t = np.clip((x + 1.0) * 0.5, 0, 1)
    r = np.clip(1.5 - np.abs(t * 6 - 1.0), 0, 1) + np.clip(1.5 - np.abs(t * 6 - 5.0), 0, 1)
    g = np.clip(1.5 - np.abs(t * 6 - 3.0), 0, 1)
    b = np.clip(1.5 - np.abs(t * 6 - 4.5), 0, 1) + np.clip(1.5 - np.abs(t * 6 - 0.0), 0, 1) * 0.0
    a = (0.85 * (1 - np.abs(x) ** 2)) * smooth(0.0, 0.1, y) * smooth(1.0, 0.7, y)
    col = np.dstack([np.clip(r, 0, 1) * 255, np.clip(g, 0, 1) * 255, np.clip(b, 0, 1) * 255])
    save("mirror_reflex_prism", compose_rgba(col, np.clip(a, 0, 1)))


def ripple():
    n = 256
    r = radial(n)
    a = np.zeros((n, n), np.float32)
    for k, (rad, wd, v) in enumerate(((0.92, 0.016, 1.0), (0.78, 0.012, 0.7), (0.62, 0.010, 0.55), (0.46, 0.008, 0.4), (0.30, 0.006, 0.3))):
        a += np.exp(-((r - rad) / wd) ** 2) * v
    a *= smooth(1.0, 0.95, r)
    a += glow(n, 4.0) * 0.1
    save("mirror_reflex_ripple", compose_rgba(np.ones((n, n, 3)) * 255, np.clip(a, 0, 1)))


if __name__ == "__main__":
    for f in (pane, ring, beam, shard, glint, sigil, prism, ripple):
        f()

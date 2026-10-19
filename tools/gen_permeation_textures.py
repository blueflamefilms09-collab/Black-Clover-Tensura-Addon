"""Permeation Magic textures (1.93.0 rebuild): the sprites of the PERMEATION_FX1..3 layer, the Phase Shroud's ground ring and glints, and the props. The wiki
magic layers' own wiki_permeation / wiki_* textures stay where they are.

    python3 -B tools/gen_permeation_textures.py  # -> src/main/resources/assets/nusmp/textures/particle/permeation_*.png

White (tinted by the effect colour, pale cyan 0x65D7E8):
  permeation_ripple   concentric rings made of dashes that shift against each other (a surface that things pass through)
  permeation_ring     a double ring of dashes and little squares (the ground ring of the armour, the tunnel rings of a cast)
  permeation_door     a tall doorway: a thin frame, rune ticks up the jambs and wave lines across the opening (128 x 256)
  permeation_ghost    a ghost: a hollow outline of a standing figure with a wavy hem and soft inside (128 x 256)
  permeation_lattice  a fine grid fading at the edges (the see-through lattice of the field)
  permeation_streak   a tall soft streak with faint wave lines (64 x 256)
  permeation_mote     a small hollow ring (a bubble of nothing)
  permeation_wave     a sine ribbon with a bright crest, tileable along its length (256 x 64)
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_tex_kit import *   # noqa: E402,F401,F403


def ripple():
    n = 256
    r = radial(n)
    th = np.arctan2(*(np.mgrid[0:n, 0:n].astype(np.float32) - (n - 1) / 2.0))
    a = np.zeros((n, n), np.float32)
    for k, (rad, wd, dash, ph) in enumerate(((0.94, 0.014, 40, 0.0), (0.80, 0.010, 32, 0.5), (0.66, 0.009, 26, 1.0), (0.52, 0.008, 20, 1.5), (0.38, 0.007, 14, 2.0), (0.24, 0.006, 8, 0.2))):
        mask = (np.sin(th * dash + ph * 3) > -0.25).astype(np.float32)
        a += np.exp(-((r - rad) / wd) ** 2) * (0.35 + 0.65 * mask) * (1.0 - 0.1 * k)
    a *= smooth(1.0, 0.96, r)
    a += glow(n, 4.0) * 0.1
    save("permeation_ripple", compose_rgba(np.ones((n, n, 3)) * 255, np.clip(a, 0, 1)))


def ring():
    n = 256
    m = Mask(n)
    r = radial(n)
    th = np.arctan2(*(np.mgrid[0:n, 0:n].astype(np.float32) - (n - 1) / 2.0))
    for rad, wd, dash in ((0.95, 0.014, 36), (0.82, 0.008, 24)):
        mask = (np.sin(th * dash) > -0.1).astype(np.float32)
        m.add(np.exp(-((r - rad) / wd) ** 2) * (0.3 + 0.7 * mask))
    for i in range(12):
        a = 2 * math.pi * i / 12
        c, s = math.cos(a), math.sin(a)
        m.poly([(c * 0.70 - 0.025, s * 0.70 - 0.025), (c * 0.70 + 0.025, s * 0.70 - 0.025), (c * 0.70 + 0.025, s * 0.70 + 0.025), (c * 0.70 - 0.025, s * 0.70 + 0.025)], 0.9)
    m.ring(0.58, 0.006, 0.7)
    m.add(glow(n, 3.0) * 0.12)
    save("permeation_ring", m.rgba(edge=(150, 240, 255)))


def door():
    w, h = 128, 256
    m = Mask(w, h)
    m.line([(-0.82, 0.97), (-0.82, -0.97), (0.82, -0.97), (0.82, 0.97)], 0.03, 1.0)
    m.line([(-0.82, 0.97), (0.82, 0.97)], 0.03, 1.0)
    for i in range(10):
        y = -0.85 + i * 0.19
        m.line([(-0.82, y), (-0.68, y)], 0.014, 0.9)
        m.line([(0.68, y), (0.82, y)], 0.014, 0.9)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    x, y = (xx - w / 2.0) / (w / 2.0), (yy - h / 2.0) / (h / 2.0)
    wave = 0.5 + 0.5 * np.sin(y * 26.0 + np.sin(x * 4.0) * 1.6)
    inside = ((np.abs(x) < 0.78) & (np.abs(y) < 0.94)).astype(np.float32)
    m.add(inside * (0.10 + 0.22 * wave))
    m.add(blur(m.a, 2.0), 0.3)
    save("permeation_door", m.rgba(edge=(150, 240, 255)))


def ghost():
    w, h = 128, 256
    m = Mask(w, h)
    pts = []
    pts += [(-0.28, -0.96), (0.28, -0.96), (0.36, -0.7), (0.28, -0.48)]                       # head
    pts += [(0.72, -0.40), (0.82, 0.30), (0.62, 0.34), (0.5, 0.0)]                             # right shoulder and arm
    pts += [(0.52, 0.55)]
    for i in range(7):                                                                         # the wavy hem
        pts.append((0.52 - i * 0.1733, 0.88 + 0.07 * (1 if i % 2 else -1)))
    pts += [(-0.52, 0.55), (-0.5, 0.0), (-0.62, 0.34), (-0.82, 0.30), (-0.72, -0.40), (-0.28, -0.48), (-0.36, -0.7)]
    m.poly(pts, 0.28)
    m.line(pts + [pts[0]], 0.022, 1.0)
    m.disc(0.07, 0.9, 0.6, 0.0, -0.64)
    m.add(blur(m.a, 3.0), 0.4)
    save("permeation_ghost", m.rgba(edge=(150, 240, 255)))


def lattice():
    n = 128
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x, y = (xx - 63.5) / 64.0, (yy - 63.5) / 64.0
    g = ((np.abs(np.sin(x * 8 * math.pi)) < 0.28) | (np.abs(np.sin(y * 8 * math.pi)) < 0.28)).astype(np.float32)
    a = g * smooth(1.0, 0.7, np.sqrt(x * x + y * y)) * 0.9
    save("permeation_lattice", compose_rgba(np.ones((n, n, 3)) * 255, a))


def streak():
    n_w, n_h = 64, 256
    yy, xx = np.mgrid[0:n_h, 0:n_w].astype(np.float32)
    x = (xx - 31.5) / 32.0
    y = yy / (n_h - 1.0)
    core = np.exp(-(x / 0.18) ** 2)
    lines = 0.35 * np.exp(-((np.abs(x) - 0.5 + 0.1 * np.sin(y * 18)) / 0.07) ** 2)
    a = np.clip(core * 0.8 + lines, 0, 1) * smooth(0.0, 0.1, y) * smooth(1.0, 0.8, y)
    save("permeation_streak", compose_rgba(np.ones((n_h, n_w, 3)) * 255, a))


def mote():
    n = 32
    r = np.clip(radial(n), 0, 1)
    a = np.exp(-((r - 0.55) / 0.16) ** 2) * 0.9 + glow(n, 2.0) * 0.15
    save("permeation_mote", compose_rgba(np.ones((n, n, 3)) * 255, np.clip(a, 0, 1)))


def wave():
    w, h = 256, 64
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    x, y = xx / w, (yy - h / 2.0) / (h / 2.0)
    crest = np.sin(x * 2 * math.pi * 4) * 0.45
    a = np.exp(-((y - crest) / 0.16) ** 2) + 0.4 * np.exp(-((y + crest * 0.6) / 0.10) ** 2)
    a *= 0.6 + 0.4 * np.sin(x * 2 * math.pi * 8) ** 2
    save("permeation_wave", compose_rgba(np.ones((h, w, 3)) * 255, np.clip(a, 0, 1)))


if __name__ == "__main__":
    for f in (ripple, ring, door, ghost, lattice, streak, mote, wave):
        f()

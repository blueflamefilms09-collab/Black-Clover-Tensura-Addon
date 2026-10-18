"""Vine Magic VFX sprites -> textures/particle/vine_*.png (used by vfx.client.layer.VineLayer and client.aura.VineAura).

Look: twisted green vines with dark grooves and ivory thorns, curling tendrils, lime glow. Sprites are coloured (the layer tints them lightly).
Writes: vine_mote, vine_leaf, vine_sigil (spiral-tendril mandala), vine_strap (twisted vine with thorns, tiles along V), vine_thorn, vine_curl (a tendril spiral),
vine_bud, vine_burst (thorn star).   Run: python3 -B tools/gen_vine_rebuild_textures.py
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_fxlib import *   # noqa: E402,F401,F403

OL = (8, 30, 10)
IVORY = (236, 226, 178, 255)


def mote():
    c = Canvas(64, 64)
    c.glow(32, 32, 30, (150, 255, 70), 0.9, 2.2)
    c.glow(32, 32, 12, (240, 255, 190), 1.0, 1.6)
    c.ring(32, 32, 20, 2.0, (200, 255, 120, 200))
    c.save("vine_mote")


def leaf():
    c = Canvas(64, 64)
    pts = [(32, 2), (42, 14), (50, 28), (46, 44), (32, 62), (18, 44), (14, 28), (22, 14)]
    c.fill(pts, lambda X, Y: grad(Y, 4, 60, (150, 220, 70, 255), (30, 110, 36, 255)))
    c.tint_noise(0.1, 3.0, 8)
    c.line([(32, 60), (32, 8)], 1.4, (216, 255, 160, 255))
    for k in range(4):
        y = 18 + k * 10
        c.line([(32, y + 5), (22, y - 2)], 1.0, (200, 250, 140, 200))
        c.line([(32, y + 5), (42, y - 2)], 1.0, (200, 250, 140, 200))
    c.outline(OL, 0.9, 0.95)
    c.save("vine_leaf")


def sigil():
    c = Canvas(256, 256)
    for k, r in enumerate((120, 104)):
        c.ring(128, 128, r, 3.2 if k == 0 else 1.8, (170, 255, 90, 230 if k == 0 else 150))
    for arm in range(3):                                                  # three spiral tendrils
        pts = []
        for i in range(60):
            t = i / 59.0
            pts.append(polar(128, 128, 10 + 90 * t, arm * 2.094 + t * 5.2))
        c.line(pts, 4.0, (120, 230, 60, 230))
        c.line(pts, 1.6, (230, 255, 170, 200))
        tip = pts[-1]
        c.ellipse(tip[0], tip[1], 4.5, 4.5, (220, 255, 140, 240))
    for k in range(18):                                                   # thorn ticks
        a = k * math.pi / 9
        c.poly([polar(128, 128, 120, a + 0.06), polar(128, 128, 134, a), polar(128, 128, 120, a - 0.06)], (210, 255, 130, 210))
    c.glow(128, 128, 26, (200, 255, 120), 0.9, 1.5)
    c.save("vine_sigil")


def strap():
    c = Canvas(64, 256)
    for s in range(2):
        path = [(32 + 13 * math.sin(y / 256.0 * 4 * math.pi + s * math.pi), y) for y in range(-8, 266, 4)]
        c.line(path, 16, (10, 40, 14, 255))
        m = c.mask_line(path, 12.5)
        sh = 0.5 + 0.5 * np.sin(c.Y / 256.0 * 4 * math.pi + s * math.pi + 1.0)
        base = np.stack([20 + 70 * sh, 80 + 110 * sh, 24 + 50 * sh, np.full_like(sh, 255)], axis=-1)
        c.paint(m, base)
        c.line([(x - 3, y) for x, y in path], 2.0, (210, 255, 150, 150))
    for y in range(20, 250, 30):                                           # thorns on the edges
        x = 32 + 13 * math.sin(y / 256.0 * 4 * math.pi)
        side = 1 if (y // 30) % 2 == 0 else -1
        c.poly([(x + side * 5, y - 4), (x + side * 18, y - 8), (x + side * 5, y + 3)], IVORY)
    c.tint_noise(0.12, 2.2, 9)
    c.save("vine_strap")


def thorn():
    c = Canvas(64, 64)
    c.fill([(26, 62), (34, 62), (42, 36), (36, 12), (30, 2), (28, 14), (24, 34)], lambda X, Y: grad(Y, 2, 62, (252, 246, 214, 255), (170, 150, 100, 255)))
    c.line([(30, 58), (31, 14)], 1.2, (130, 110, 70, 150))
    c.outline((60, 50, 30), 0.9, 1.0)
    c.save("vine_thorn")


def curl():
    c = Canvas(128, 128)
    pts = []
    for i in range(70):
        t = i / 69.0
        a = t * 11.0
        pts.append(polar(64, 60, 4 + 52 * (1 - t) ** 1.2, a))
    for i in range(len(pts) - 1):
        w = 2.0 + 6.0 * (1 - i / 69.0)
        c.line([pts[i], pts[i + 1]], w + 2.2, (10, 44, 14, 255))
        c.line([pts[i], pts[i + 1]], w, (70 + int(80 * (i % 8) / 8.0), 170, 60, 255))
    c.line([(64, 122), (68, 100), (60, 80), pts[0]], 8, (50, 140, 46, 255))
    c.glow(pts[-1][0], pts[-1][1], 8, (220, 255, 140), 0.8, 1.5)
    c.outline(OL, 0.8, 0.8)
    c.save("vine_curl")


def bud():
    c = Canvas(64, 64)
    for k in range(5):
        a = -math.pi / 2 + (k - 2) * 0.55
        c.fill([polar(32, 50, 3, a + 1.57), polar(32, 50, 34, a - 0.14), polar(32, 50, 38, a), polar(32, 50, 34, a + 0.14), polar(32, 50, 3, a - 1.57)],
               lambda X, Y: grad(Y, 12, 50, (210, 255, 130, 255), (50, 150, 50, 255)))
    c.glow(32, 30, 12, (240, 255, 190), 0.9, 1.5)
    c.outline(OL, 0.8, 0.95)
    c.save("vine_bud")


def burst():
    c = Canvas(128, 128)
    rng = random.Random(11)
    for k in range(12):
        a = k * math.pi / 6 + rng.uniform(-0.08, 0.08)
        r = rng.uniform(38, 62)
        c.poly([polar(64, 64, 8, a + 0.22), polar(64, 64, r, a), polar(64, 64, 8, a - 0.22)], (236 + rng.randint(-14, 14), 230, 186, 255))
        c.line([polar(64, 64, 10, a), polar(64, 64, r * 0.9, a)], 1.0, (120, 110, 70, 190))
    c.glow(64, 64, 24, (190, 255, 100), 0.9, 1.5)
    c.outline((50, 60, 30), 0.8, 0.8)
    c.save("vine_burst")


if __name__ == "__main__":
    for f in (mote, leaf, sigil, strap, thorn, curl, bud, burst):
        f()
    contact_sheet(["vine_mote", "vine_leaf", "vine_sigil", "vine_strap", "vine_thorn", "vine_curl", "vine_bud", "vine_burst"],
                  os.path.join(os.environ.get("SHEET_DIR", "/tmp"), "vine_sheet.png"), 8, 128)

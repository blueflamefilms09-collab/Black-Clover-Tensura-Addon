"""Mud Magic VFX sprites -> textures/particle/mud_*.png (used by vfx.client.layer.MudLayer and client.aura.MudAura).

Look: wet brown mud with bright highlights, clods, splashes and drips, cracked ground with amber fissures. Sprites are coloured (the layer tints them lightly).
Writes: mud_glob (a thrown blob with a trail of drops), mud_splat (a splash crown), mud_drip, mud_sigil (a cracked ring with amber fissures), mud_chunk (a clod),
mud_mist (dust), mud_burst (a radial splash), mud_ripple (concentric ripples).   Run: python3 -B tools/gen_mud_rebuild_textures.py
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_fxlib import *   # noqa: E402,F401,F403

OL = (30, 18, 10)
MUD0, MUD1 = (82, 52, 30, 255), (150, 100, 62, 255)


def mudfill(x0, y0, x1, y1):
    return lambda X, Y: grad(Y, y0, y1, MUD1, MUD0)


def glob():
    c = Canvas(128, 128)
    pts = jitter_circle(52, 64, 34, 28, 0.14, 4)
    c.fill(pts, mudfill(0, 30, 0, 98))
    c.tint_noise(0.14, 3.0, 4)
    c.ellipse(40, 50, 8, 5, (214, 168, 120, 230), 0.8)
    for k, (x, y, r) in enumerate(((92, 60, 8), (108, 70, 5), (100, 80, 3.5), (116, 62, 3))):
        c.fill_ellipse(x, y, r, r * 0.85, mudfill(0, y - r, 0, y + r))
    c.poly([(80, 52), (96, 58), (80, 76)], MUD0)
    c.outline(OL, 1.0, 1.0)
    c.save("mud_glob")


def splat():
    c = Canvas(128, 128)
    rng = random.Random(9)
    pts = []
    for k in range(36):
        a = k * 2 * math.pi / 36
        r = 34 + (18 if k % 3 == 0 else 6) * rng.uniform(0.6, 1.2)
        pts.append(polar(64, 64, r, a))
    c.fill(pts, lambda X, Y: grad(np.sqrt((X - 64) ** 2 + (Y - 64) ** 2), 0, 58, MUD0, MUD1))
    for k in range(12):
        a = rng.uniform(0, 6.28)
        r = rng.uniform(46, 60)
        s = rng.uniform(2.5, 5)
        c.fill_ellipse(*polar(64, 64, r, a), s, s, mudfill(0, 40, 0, 90))
    c.ellipse(52, 52, 12, 6, (214, 168, 120, 190), 1.0)
    c.tint_noise(0.14, 3.0, 5)
    c.outline(OL, 0.9, 1.0)
    c.save("mud_splat")


def drip():
    c = Canvas(64, 64)
    pts = [(32, 4), (40, 24), (46, 40), (42, 54), (32, 60), (22, 54), (18, 40), (24, 24)]
    c.fill(pts, mudfill(0, 4, 0, 60))
    c.ellipse(26, 42, 3, 6, (214, 168, 120, 210), 0.6)
    c.outline(OL, 0.9, 1.0)
    c.save("mud_drip")


def sigil():
    c = Canvas(256, 256)
    rng = random.Random(13)
    for r, w in ((120, 6), (104, 3)):
        pts = jitter_circle(128, 128, r, 90, 0.02, int(r))
        pts.append(pts[0])
        c.line(pts, w, (120, 80, 48, 255))
        c.line(pts, w * 0.35, (255, 160, 70, 230))
    for k in range(14):                                                   # fissures
        a = k * 2 * math.pi / 14 + rng.uniform(-0.08, 0.08)
        pts = [polar(128, 128, 14, a)]
        r = 14
        aa = a
        while r < 118:
            r += rng.uniform(10, 18)
            aa += rng.uniform(-0.09, 0.09)
            pts.append(polar(128, 128, min(r, 118), aa))
        c.line(pts, 4.5, (46, 28, 16, 255))
        c.line(pts, 1.8, (255, 170, 80, 240))
    for k in range(10):                                                   # clods on the rim
        a = k * 2 * math.pi / 10
        x, y = polar(128, 128, 112, a)
        c.fill_ellipse(x, y, 7, 6, mudfill(0, y - 6, 0, y + 6))
    c.glow(128, 128, 30, (255, 170, 80), 0.9, 1.5)
    c.save("mud_sigil")


def chunk():
    c = Canvas(64, 64)
    pts = [(10, 40), (14, 18), (30, 8), (50, 16), (56, 36), (44, 56), (22, 54)]
    c.fill(pts, lambda X, Y: grad(Y, 8, 56, (170, 130, 90, 255), (70, 44, 26, 255)))
    c.tint_noise(0.2, 2.5, 7)
    c.line([(20, 22), (30, 30), (28, 46)], 1.2, (36, 20, 10, 200))
    c.line([(40, 24), (36, 36)], 1.0, (36, 20, 10, 200))
    c.outline(OL, 1.0, 1.0)
    c.save("mud_chunk")


def mist():
    c = Canvas(128, 128)
    n = vnoise(c.W, c.H, 20 * c.ss, 6) * 0.6 + vnoise(c.W, c.H, 8 * c.ss, 7) * 0.4
    d = np.sqrt((c.X - 64) ** 2 + (c.Y - 64) ** 2) / 62.0
    a = clamp01(1.0 - d) ** 1.3 * clamp01(n * 1.4) * 0.9
    col = np.stack([150 + 50 * n, 112 + 40 * n, 76 + 30 * n, np.full_like(n, 255)], axis=-1)
    c.paint(a, col)
    c.save("mud_mist")


def burst():
    c = Canvas(128, 128)
    rng = random.Random(15)
    for k in range(13):
        a = k * 2 * math.pi / 13 + rng.uniform(-0.08, 0.08)
        r = rng.uniform(40, 62)
        w = rng.uniform(0.1, 0.18)
        c.fill([polar(64, 64, 8, a + w * 2), polar(64, 64, r * 0.8, a + w), polar(64, 64, r, a), polar(64, 64, r * 0.8, a - w), polar(64, 64, 8, a - w * 2)], mudfill(0, 4, 0, 124))
        c.ellipse(*polar(64, 64, r, a), 3.2, 3.2, (130, 86, 52, 255))
    c.fill_ellipse(64, 64, 16, 14, mudfill(0, 50, 0, 78))
    c.glow(64, 64, 18, (255, 190, 110), 0.5, 1.6)
    c.outline(OL, 0.8, 0.9)
    c.save("mud_burst")


def ripple():
    c = Canvas(256, 256)
    for k, r in enumerate((116, 92, 68, 44, 22)):
        pts = jitter_circle(128, 128, r, 80, 0.015, 50 + k)
        pts.append(pts[0])
        c.line(pts, 5 - k * 0.6, (96, 62, 36, 220 - k * 24))
        c.line(pts, 1.6, (214, 170, 120, 180 - k * 20))
    c.save("mud_ripple")


if __name__ == "__main__":
    for f in (glob, splat, drip, sigil, chunk, mist, burst, ripple):
        f()
    contact_sheet(["mud_glob", "mud_splat", "mud_drip", "mud_sigil", "mud_chunk", "mud_mist", "mud_burst", "mud_ripple"],
                  os.path.join(os.environ.get("SHEET_DIR", "/tmp"), "mud_sheet.png"), 8, 128)

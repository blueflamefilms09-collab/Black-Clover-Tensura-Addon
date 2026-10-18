"""Poison Plant Magic VFX sprites -> textures/particle/poison_plant_*.png (used by vfx.client.layer.PoisonPlantLayer and client.aura.PoisonPlantAura).

Look: mottled violet and olive flesh, ivory fangs and thorns, acid-green venom and gas. Sprites are coloured (the layer tints them lightly).
Writes: poison_plant_spore (a glowing spore), poison_plant_mist (a toxic cloud puff), poison_plant_thorn (a hooked fang), poison_plant_sigil (a toxic flower sigil with fangs),
poison_plant_drop (a venom drop), poison_plant_vine (a thorny vine, tiles along V), poison_plant_burst (a spiked star), poison_plant_maw (an open trap in profile).
Run: python3 -B tools/gen_poison_plant_rebuild_textures.py
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_fxlib import *   # noqa: E402,F401,F403

OL = (14, 6, 24)
FANG = (240, 234, 200, 255)


def spore():
    c = Canvas(64, 64)
    c.glow(32, 32, 30, (150, 255, 30), 0.9, 2.2)
    c.fill_ellipse(32, 32, 12, 12, lambda X, Y: grad(np.sqrt((X - 32) ** 2 + (Y - 32) ** 2), 0, 12, (240, 255, 150, 255), (90, 200, 20, 255)))
    c.ring(32, 32, 17, 1.6, (190, 255, 80, 200))
    for k in range(6):
        a = k * math.pi / 3
        c.ellipse(*polar(32, 32, 23, a), 2.2, 2.2, (210, 255, 110, 220))
    c.save("poison_plant_spore")


def mist():
    c = Canvas(128, 128)
    n = vnoise(c.W, c.H, 18 * c.ss, 3) * 0.6 + vnoise(c.W, c.H, 8 * c.ss, 4) * 0.4
    d = np.sqrt((c.X - 64) ** 2 + (c.Y - 64) ** 2) / 62.0
    a = clamp01(1.0 - d) ** 1.2 * clamp01(n * 1.5)
    col = np.stack([150 + 80 * n, 220 + 30 * n, 60 + 60 * n, np.full_like(n, 255)], axis=-1)
    c.paint(a, col)
    c.save("poison_plant_mist")


def thorn():
    c = Canvas(64, 64)
    c.fill([(22, 62), (36, 62), (46, 40), (40, 18), (26, 4), (30, 20), (22, 40)], lambda X, Y: grad(Y, 4, 62, (252, 246, 220, 255), (150, 130, 96, 255)))
    c.poly([(28, 12), (30, 44)], (0, 0, 0, 0))
    c.glow(28, 8, 8, (190, 255, 60), 0.9, 1.4)
    c.outline((50, 40, 30), 0.9, 1.0)
    c.save("poison_plant_thorn")


def sigil():
    c = Canvas(256, 256)
    c.ring(128, 128, 122, 3.4, (180, 255, 50, 235))
    c.ring(128, 128, 104, 2.0, (140, 90, 220, 190))
    for k in range(10):                                                   # ten fangs pointing in
        a = k * math.pi / 5
        c.poly([polar(128, 128, 120, a + 0.1), polar(128, 128, 82, a), polar(128, 128, 120, a - 0.1)], (230, 255, 150, 200))
    for k in range(5):                                                    # a five-lobed venom flower
        a = k * 2 * math.pi / 5 - math.pi / 2
        pts = [polar(128, 128, 8, a + 1.2), polar(128, 128, 66, a + 0.34), polar(128, 128, 78, a), polar(128, 128, 66, a - 0.34), polar(128, 128, 8, a - 1.2)]
        c.fill(pts, lambda X, Y: grad(np.sqrt((X - 128) ** 2 + (Y - 128) ** 2), 8, 78, (120, 60, 190, 90), (190, 255, 60, 220)))
        c.line(pts + [pts[0]], 1.8, (210, 255, 110, 230))
    c.glow(128, 128, 22, (200, 255, 80), 1.0, 1.4)
    c.ring(128, 128, 14, 2.4, (240, 255, 170, 240))
    c.save("poison_plant_sigil")


def drop():
    c = Canvas(64, 64)
    pts = [(32, 4), (40, 22), (46, 38), (42, 52), (32, 60), (22, 52), (18, 38), (24, 22)]
    c.fill(pts, lambda X, Y: grad(Y, 8, 58, (230, 255, 130, 255), (80, 190, 20, 255)))
    c.ellipse(26, 42, 3, 5, (250, 255, 220, 230))
    c.glow(32, 36, 28, (160, 255, 40), 0.35, 2.0)
    c.outline((20, 70, 10), 0.8, 0.9)
    c.save("poison_plant_drop")


def vine():
    c = Canvas(64, 256)
    for s in range(2):
        path = [(32 + 12 * math.sin(y / 256.0 * 5 * math.pi + s * math.pi), y) for y in range(-8, 266, 4)]
        c.line(path, 15, (12, 6, 26, 255))
        m = c.mask_line(path, 11.5)
        sh = 0.5 + 0.5 * np.sin(c.Y / 256.0 * 5 * math.pi + s * math.pi + 1.0)
        base = np.stack([60 + 100 * sh, 24 + 60 * sh, 100 + 110 * sh, np.full_like(sh, 255)], axis=-1) if s == 0 else np.stack([60 + 80 * sh, 90 + 90 * sh, 20 + 30 * sh, np.full_like(sh, 255)], axis=-1)
        c.paint(m, base)
    for y in range(14, 250, 26):
        x = 32 + 12 * math.sin(y / 256.0 * 5 * math.pi)
        side = 1 if (y // 26) % 2 == 0 else -1
        c.poly([(x + side * 4, y - 3), (x + side * 18, y - 9), (x + side * 4, y + 4)], FANG)
    c.line([(32, 0), (32, 256)], 1.2, (190, 255, 60, 70))
    c.tint_noise(0.12, 2.2, 15)
    c.save("poison_plant_vine")


def burst():
    c = Canvas(128, 128)
    rng = random.Random(31)
    for k in range(14):
        a = k * math.pi / 7 + rng.uniform(-0.06, 0.06)
        r = rng.uniform(36, 62)
        col = (236, 250, 160, 255) if k % 2 == 0 else (150, 90, 210, 255)
        c.poly([polar(64, 64, 8, a + 0.2), polar(64, 64, r, a), polar(64, 64, 8, a - 0.2)], col)
    c.glow(64, 64, 24, (190, 255, 60), 0.95, 1.4)
    c.outline(OL, 0.8, 0.8)
    c.save("poison_plant_burst")


def maw():
    c = Canvas(128, 128)
    up = [(10, 62), (30, 20), (64, 6), (100, 20), (118, 62), (64, 50)]
    lo = [(10, 70), (64, 82), (118, 70), (100, 112), (64, 122), (30, 112)]
    c.fill(up, lambda X, Y: grad(Y, 6, 62, (170, 110, 230, 255), (80, 30, 130, 255)))
    c.fill(lo, lambda X, Y: grad(Y, 70, 122, (140, 50, 100, 255), (200, 80, 130, 255)))
    c.fill([(20, 64), (64, 54), (108, 64), (64, 78)], lambda X, Y: grad(Y, 54, 78, (200, 255, 80, 255), (80, 190, 20, 255)))
    for k in range(7):
        x = 22 + k * 14
        c.poly([(x - 4, 60), (x, 76), (x + 4, 60)], FANG)
        c.poly([(x - 4, 74), (x, 58), (x + 4, 74)], FANG)
    c.glow(64, 66, 22, (190, 255, 60), 0.7, 1.6)
    c.outline(OL, 0.9, 1.0)
    c.save("poison_plant_maw")


if __name__ == "__main__":
    for f in (spore, mist, thorn, sigil, drop, vine, burst, maw):
        f()
    contact_sheet(["poison_plant_spore", "poison_plant_mist", "poison_plant_thorn", "poison_plant_sigil", "poison_plant_drop", "poison_plant_vine", "poison_plant_burst", "poison_plant_maw"],
                  os.path.join(os.environ.get("SHEET_DIR", "/tmp"), "poison_sheet.png"), 8, 128)

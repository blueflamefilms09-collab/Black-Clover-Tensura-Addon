"""Plant Magic VFX sprites -> textures/particle/plant_*.png (used by vfx.client.layer.PlantLayer and client.aura.PlantAura).

Look: soft pink and white petals with veins, a golden heart, green leaves, drifting pollen. Sprites are coloured (the layer tints them lightly).
Writes: plant_petal, plant_flower (a six-petal bloom), plant_mote (pollen), plant_sigil (flower mandala), plant_leaf, plant_stem (stem with leaves, tiles along V),
plant_burst (a ring of petals), plant_heart (golden glow with a cross sparkle).   Run: python3 -B tools/gen_plant_rebuild_textures.py
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_fxlib import *   # noqa: E402,F401,F403

OL = (96, 24, 60)


def petal_shape(cx, cy, length, width, ang):
    pts = []
    for i in range(0, 21):
        t = i / 20.0
        r = length * t
        w = width * math.sin(t * math.pi) ** 0.8 * (1 - 0.25 * t)
        pts.append((r, w))
    out = [polar(cx, cy, r, ang) if False else (cx + r * math.cos(ang) - w * math.sin(ang), cy + r * math.sin(ang) + w * math.cos(ang)) for r, w in pts]
    out += [(cx + r * math.cos(ang) + w * math.sin(ang), cy + r * math.sin(ang) - w * math.cos(ang)) for r, w in reversed(pts)]
    return out


def petal():
    c = Canvas(64, 64)
    pts = petal_shape(32, 58, 54, 17, -math.pi / 2)
    c.fill(pts, lambda X, Y: grad(Y, 4, 58, (255, 232, 242, 255), (238, 120, 168, 255)))
    c.tint_noise(0.05, 4.0, 2)
    for k in range(-2, 3):
        c.line([(32, 56), (32 + k * 6, 30), (32 + k * 8, 10)], 0.9, (214, 90, 140, 130))
    c.outline(OL, 0.8, 0.7)
    c.save("plant_petal")


def flower():
    c = Canvas(128, 128)
    for ring_i, (n, ln, wd, col0, col1, rot) in enumerate(((6, 56, 22, (255, 236, 246, 255), (240, 130, 176, 255), 0.0), (6, 40, 17, (255, 246, 214, 255), (250, 190, 120, 255), 0.52))):
        for k in range(n):
            a = rot + k * 2 * math.pi / n
            c.fill(petal_shape(64, 64, ln, wd, a), lambda X, Y, c0=col0, c1=col1: grad(np.sqrt((X - 64) ** 2 + (Y - 64) ** 2), 4, 58, c1, c0))
            c.line([polar(64, 64, 6, a), polar(64, 64, ln * 0.8, a)], 0.9, (220, 100, 150, 120))
    c.ellipse(64, 64, 12, 12, (255, 200, 70, 255))
    c.glow(64, 64, 14, (255, 240, 160), 0.9, 1.4)
    c.outline(OL, 0.8, 0.6)
    c.save("plant_flower")


def mote():
    c = Canvas(64, 64)
    c.glow(32, 32, 30, (255, 220, 90), 0.9, 2.2)
    c.glow(32, 32, 11, (255, 250, 200), 1.0, 1.6)
    for k in range(4):
        a = k * math.pi / 2 + 0.78
        c.poly([polar(32, 32, 2.5, a + 1.57), polar(32, 32, 20, a), polar(32, 32, 2.5, a - 1.57)], (255, 240, 170, 130))
    c.save("plant_mote")


def sigil():
    c = Canvas(256, 256)
    c.ring(128, 128, 120, 3.0, (255, 190, 220, 230))
    c.ring(128, 128, 108, 1.6, (255, 220, 160, 170))
    for k in range(8):
        a = k * math.pi / 4
        c.fill(petal_shape(128, 128, 100, 26, a), lambda X, Y: grad(np.sqrt((X - 128) ** 2 + (Y - 128) ** 2), 10, 100, (255, 230, 160, 40), (255, 150, 200, 210)))
        c.line(list(petal_shape(128, 128, 100, 26, a))[:21], 1.8, (255, 220, 235, 220))
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        c.fill(petal_shape(128, 128, 62, 16, a), lambda X, Y: grad(np.sqrt((X - 128) ** 2 + (Y - 128) ** 2), 10, 62, (255, 240, 170, 60), (255, 200, 120, 200)))
    for k in range(16):
        a = k * math.pi / 8
        c.ellipse(*polar(128, 128, 114, a), 3, 3, (255, 244, 190, 230))
    c.glow(128, 128, 28, (255, 230, 150), 0.95, 1.4)
    c.save("plant_sigil")


def leaf():
    c = Canvas(64, 64)
    pts = [(32, 2), (44, 14), (52, 30), (46, 46), (32, 62), (18, 46), (12, 30), (20, 14)]
    c.fill(pts, lambda X, Y: grad(Y, 4, 60, (170, 236, 110, 255), (40, 130, 56, 255)))
    c.tint_noise(0.08, 3.0, 12)
    c.line([(32, 60), (32, 8)], 1.4, (224, 255, 180, 255))
    for k in range(4):
        y = 18 + k * 10
        c.line([(32, y + 5), (22, y - 2)], 1.0, (210, 250, 160, 190))
        c.line([(32, y + 5), (42, y - 2)], 1.0, (210, 250, 160, 190))
    c.outline((14, 56, 24), 0.9, 0.9)
    c.save("plant_leaf")


def stem():
    c = Canvas(64, 256)
    path = [(32 + 6 * math.sin(y / 256.0 * 3 * math.pi), y) for y in range(-8, 266, 4)]
    c.line(path, 9, (20, 70, 28, 255))
    c.line(path, 6, (90, 190, 80, 255))
    c.line([(x - 1.5, y) for x, y in path], 1.6, (200, 255, 170, 170))
    for y in (50, 120, 190):
        s = 1 if (y // 70) % 2 == 0 else -1
        x = 32 + 6 * math.sin(y / 256.0 * 3 * math.pi)
        c.fill([(x, y), (x + s * 14, y - 12), (x + s * 26, y - 6), (x + s * 14, y + 6)], lambda X, Y: grad(X, x, x + s * 26, (60, 160, 60, 255), (150, 225, 100, 255)) if s > 0 else grad(X, x, x - 26, (60, 160, 60, 255), (150, 225, 100, 255)))
    c.outline((14, 56, 24), 0.8, 0.8)
    c.save("plant_stem")


def burst():
    c = Canvas(128, 128)
    rng = random.Random(21)
    for k in range(12):
        a = k * math.pi / 6 + rng.uniform(-0.05, 0.05)
        r = rng.uniform(44, 62)
        c.fill(petal_shape(64, 64, r, 9, a), lambda X, Y: grad(np.sqrt((X - 64) ** 2 + (Y - 64) ** 2), 6, 62, (255, 236, 244, 255), (240, 130, 176, 255)))
    c.glow(64, 64, 22, (255, 240, 170), 0.9, 1.4)
    c.outline(OL, 0.7, 0.6)
    c.save("plant_burst")


def heart():
    c = Canvas(64, 64)
    c.glow(32, 32, 31, (255, 210, 90), 0.8, 2.0)
    c.glow(32, 32, 14, (255, 248, 200), 1.0, 1.3)
    c.poly([(32, 0), (35, 29), (64, 32), (35, 35), (32, 64), (29, 35), (0, 32), (29, 29)], (255, 244, 190, 160))
    c.save("plant_heart")


if __name__ == "__main__":
    for f in (petal, flower, mote, sigil, leaf, stem, burst, heart):
        f()
    contact_sheet(["plant_petal", "plant_flower", "plant_mote", "plant_sigil", "plant_leaf", "plant_stem", "plant_burst", "plant_heart"],
                  os.path.join(os.environ.get("SHEET_DIR", "/tmp"), "plant_sheet.png"), 8, 128)

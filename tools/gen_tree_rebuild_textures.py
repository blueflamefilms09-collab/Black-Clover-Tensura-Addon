"""Tree Magic VFX sprites -> textures/particle/tree_fx_*.png (the older tree_leaf, tree_root, tree_canopy belong to the World Tree layers and stay untouched) (used by vfx.client.layer.TreeLayer and client.aura.TreeAura).

Look: heartwood and bark with amber sap light, green leaves, growth rings. Sprites are coloured (the layer tints them lightly with the caster's colour).
Writes: tree_fx_mote (sap spark), tree_fx_leaf, tree_fx_ring (growth-ring sigil), tree_fx_bark (bark shard), tree_fx_root (root strap, tiles along V), tree_fx_splinter (splinter burst),
tree_fx_seed (acorn), tree_fx_branch (twig fork).   Run: python3 -B tools/gen_tree_rebuild_textures.py
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_fxlib import *   # noqa: E402,F401,F403

BARK = [(46, 28, 14, 255), (92, 60, 32, 255), (140, 98, 56, 255), (190, 146, 92, 255)]
OL = (22, 12, 6)


def mote():
    c = Canvas(64, 64)
    c.glow(32, 32, 30, (255, 190, 80), 0.9, 2.2)
    c.glow(32, 32, 13, (255, 240, 170), 1.0, 1.6)
    for k in range(4):
        a = k * math.pi / 2
        c.poly([polar(32, 32, 3, a + 1.57), polar(32, 32, 30, a), polar(32, 32, 3, a - 1.57)], (255, 236, 160, 150))
    c.save("tree_fx_mote")


def leaf():
    c = Canvas(64, 64)
    pts = [(32, 3), (47, 15), (54, 32), (48, 48), (32, 61), (16, 48), (10, 32), (17, 15)]
    c.fill(pts, lambda X, Y: grad(X, 12, 52, (40, 110, 34, 255), (130, 210, 80, 255)))
    c.tint_noise(0.1, 3.5, 3)
    c.line([(32, 60), (32, 6)], 1.6, (200, 255, 150, 255))
    for k in range(5):
        y = 14 + k * 9
        c.line([(32, y + 4), (21, y - 3)], 1.0, (190, 245, 140, 220))
        c.line([(32, y + 4), (43, y - 3)], 1.0, (190, 245, 140, 220))
    c.outline(OL, 0.9, 0.95)
    c.save("tree_fx_leaf")


def ring():
    c = Canvas(256, 256)
    rng = random.Random(5)
    for k, r in enumerate((118, 98, 80, 63, 47, 33, 20)):
        pts = jitter_circle(128, 128, r, 64, 0.025, 10 + k)
        pts.append(pts[0])
        c.line(pts, 3.2 if k % 2 == 0 else 1.8, (255, 214, 130, 235 if k % 2 == 0 else 160))
    for k in range(10):                                                  # radial growth cracks
        a = rng.uniform(0, 6.28)
        r0, r1 = rng.uniform(10, 30), rng.uniform(70, 118)
        c.line([polar(128, 128, r0, a), polar(128, 128, (r0 + r1) / 2, a + 0.05), polar(128, 128, r1, a - 0.03)], 1.4, (255, 190, 90, 150))
    for k in range(24):                                                  # ticks round the rim
        a = k * math.pi / 12
        c.line([polar(128, 128, 122, a), polar(128, 128, 127, a)], 2.4, (255, 236, 170, 220))
    c.glow(128, 128, 30, (255, 220, 130), 0.9, 1.5)
    c.save("tree_fx_ring")


def bark():
    c = Canvas(64, 64)
    pts = [(10, 52), (6, 22), (22, 6), (46, 10), (58, 34), (50, 58)]
    c.fill(pts, lambda X, Y: grad(Y, 6, 58, (150, 106, 62, 255), (62, 38, 20, 255)))
    for k in range(6):
        x = 14 + k * 8
        c.line([(x, 56), (x + 2, 30), (x - 1, 8)], 1.2, (36, 20, 10, 190))
    c.tint_noise(0.18, 2.5, 4)
    c.outline(OL, 1.0, 1.0)
    c.save("tree_fx_bark")


def root():
    c = Canvas(64, 256)
    for s in range(2):
        pts = [(32 + 14 * math.sin(y / 256.0 * 4 * math.pi + s * math.pi), y) for y in range(-8, 266, 4)]
        c.line(pts, 15, (30, 16, 8, 255))
        c.fill([(0, 0), (64, 0), (64, 256), (0, 256)], lambda X, Y: np.broadcast_to(np.array((110, 74, 40, 0), dtype=np.float64), X.shape + (4,)), 0)
        m = c.mask_line(pts, 12)
        shade = 0.55 + 0.45 * np.sin((c.Y / 256.0 * 4 * math.pi) + s * math.pi + 1.2) * 0.5 + 0.22 * (1 if s == 0 else 0)
        base = np.stack([60 + 90 * shade, 38 + 62 * shade, 20 + 36 * shade, np.full_like(shade, 255)], axis=-1)
        c.paint(m, base)
        c.line([(32 + 14 * math.sin(y / 256.0 * 4 * math.pi + s * math.pi) - 3, y) for y in range(-8, 266, 4)], 2.0, (200, 160, 100, 140))
    c.tint_noise(0.16, 2.2, 5)
    c.save("tree_fx_root")


def splinter():
    c = Canvas(128, 128)
    rng = random.Random(7)
    for k in range(14):
        a = k * math.pi * 2 / 14 + rng.uniform(-0.1, 0.1)
        r = rng.uniform(40, 62)
        w = rng.uniform(0.07, 0.12)
        c.poly([polar(64, 64, 6, a + w * 2), polar(64, 64, r, a), polar(64, 64, 6, a - w * 2)], (200 + rng.randint(-20, 20), 150, 90, 255))
        c.line([polar(64, 64, 8, a), polar(64, 64, r * 0.9, a)], 1.0, (70, 40, 20, 200))
    c.glow(64, 64, 22, (255, 220, 140), 0.9, 1.6)
    c.outline(OL, 0.8, 0.8)
    c.save("tree_fx_splinter")


def seed():
    c = Canvas(64, 64)
    c.fill_ellipse(32, 40, 15, 19, lambda X, Y: grad(Y, 22, 58, (230, 170, 80, 255), (140, 84, 34, 255)))
    c.fill([(14, 28), (50, 28), (46, 14), (32, 10), (18, 14)], lambda X, Y: grad(Y, 10, 28, (110, 76, 44, 255), (60, 38, 20, 255)))
    c.line([(32, 12), (32, 4)], 3, (70, 44, 24, 255))
    for k in range(5):
        c.line([(18 + k * 7, 26), (20 + k * 7, 15)], 1.0, (38, 22, 10, 170))
    c.glow(32, 42, 18, (255, 220, 120), 0.4, 2.0)
    c.outline(OL, 0.9, 1.0)
    c.save("tree_fx_seed")


def branch():
    c = Canvas(128, 128)

    def limb(p0, p1, w0, w1):
        n = 10
        for i in range(n):
            t0, t1 = i / float(n), (i + 1) / float(n)
            a = (p0[0] + (p1[0] - p0[0]) * t0, p0[1] + (p1[1] - p0[1]) * t0)
            b = (p0[0] + (p1[0] - p0[0]) * t1, p0[1] + (p1[1] - p0[1]) * t1)
            w = w0 + (w1 - w0) * (t0 + t1) / 2.0
            c.line([a, b], w, (96, 64, 34, 255))
            c.line([(a[0] - w * 0.2, a[1]), (b[0] - w * 0.2, b[1])], w * 0.35, (170, 124, 76, 220))
    limb((64, 124), (62, 70), 11, 7)
    limb((62, 70), (36, 36), 6, 3)
    limb((62, 70), (90, 30), 6, 3)
    limb((62, 70), (64, 10), 6, 2.5)
    limb((48, 52), (22, 58), 3.5, 1.8)
    limb((78, 50), (104, 56), 3.5, 1.8)
    c.tint_noise(0.15, 2.5, 6)
    c.outline(OL, 1.0, 1.0)
    c.save("tree_fx_branch")


if __name__ == "__main__":
    for f in (mote, leaf, ring, bark, root, splinter, seed, branch):
        f()
    contact_sheet(["tree_fx_mote", "tree_fx_leaf", "tree_fx_ring", "tree_fx_bark", "tree_fx_root", "tree_fx_splinter", "tree_fx_seed", "tree_fx_branch"],
                  os.path.join(os.environ.get("SHEET_DIR", "/tmp"), "tree_sheet.png"), 8, 128)

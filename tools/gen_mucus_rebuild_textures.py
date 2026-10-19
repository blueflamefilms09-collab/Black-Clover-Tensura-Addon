"""Mucus Magic VFX sprites -> textures/particle/mucus_*.png (used by vfx.client.layer.MucusLayer and client.aura.MucusAura).

Look: translucent teal gel with bright rims and clear centres, bubbles, long sticky strands, a glowing nucleus. Sprites are coloured with soft alpha (the layer tints them lightly).
Writes: mucus_bubble, mucus_blob (a gel blob with a nucleus), mucus_strand (a sticky strand, tiles along V), mucus_sigil (a membrane mandala of bubbles), mucus_splat,
mucus_drip, mucus_burst (a popping bubble with droplets), mucus_ring (a ripple ring).   Run: python3 -B tools/gen_mucus_rebuild_textures.py
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_fxlib import *   # noqa: E402,F401,F403

TEAL = (110, 224, 190)
PALE = (220, 255, 246)


def gelfill(cx, cy, r, a0=70, a1=170):
    """Gel disc colours: clearer in the middle, brighter and more opaque towards the rim."""
    def fn(X, Y):
        d = clamp01(np.sqrt((X - cx) ** 2 + (Y - cy) ** 2) / float(r))
        col = np.zeros(X.shape + (4,))
        col[..., 0] = 90 + 130 * d ** 2
        col[..., 1] = 210 + 40 * d ** 2
        col[..., 2] = 180 + 66 * d ** 2
        col[..., 3] = a0 + (a1 - a0) * d ** 1.5
        return col
    return fn


def bubble():
    c = Canvas(64, 64)
    c.fill_ellipse(32, 32, 28, 28, gelfill(32, 32, 28, 40, 150))
    c.ring(32, 32, 27, 2.0, (230, 255, 248, 240))
    c.ellipse(22, 21, 8, 4.5, (255, 255, 255, 230), 1.2)
    c.ellipse(41, 43, 3, 2, (255, 255, 255, 150), 0.8)
    c.glow(32, 32, 30, TEAL, 0.25, 2.0)
    c.save("mucus_bubble")


def blob():
    c = Canvas(128, 128)
    pts = jitter_circle(64, 66, 46, 30, 0.1, 21)
    c.fill(pts, gelfill(64, 66, 48, 90, 190))
    c.tint_noise(0.08, 5.0, 3)
    c.fill_ellipse(64, 70, 17, 17, lambda X, Y: grad(np.sqrt((X - 64) ** 2 + (Y - 70) ** 2), 0, 17, (230, 255, 252, 250), (60, 220, 190, 220)))
    c.glow(64, 70, 26, (120, 255, 225), 0.7, 1.6)
    c.ellipse(46, 40, 11, 5, (255, 255, 255, 220), 1.2)
    for x, y, r in ((92, 52, 5), (40, 84, 4), (86, 88, 3)):
        c.ring(x, y, r, 1.4, (240, 255, 250, 230))
    c.save("mucus_blob")


def strand():
    c = Canvas(64, 256)
    for s, (w, a) in enumerate(((12, 150), (5, 230))):
        path = [(32 + 11 * math.sin(y / 256.0 * 3 * math.pi + 0.6 * s), y) for y in range(-8, 266, 4)]
        c.line(path, w, (110 + 80 * s, 224 + 20 * s, 190 + 40 * s, a))
    for y in range(18, 256, 38):                                           # beads of gel on the strand
        x = 32 + 11 * math.sin(y / 256.0 * 3 * math.pi)
        c.fill_ellipse(x, y, 6.5, 6.5, gelfill(x, y, 6.5, 120, 220))
        c.ellipse(x - 2, y - 2, 2, 1.4, (255, 255, 255, 230))
    c.save("mucus_strand")


def sigil():
    c = Canvas(256, 256)
    c.ring(128, 128, 120, 3.0, (150, 255, 226, 235))
    c.ring(128, 128, 104, 1.6, (200, 255, 240, 170))
    for k in range(12):
        a = k * math.pi / 6
        x, y = polar(128, 128, 112, a)
        c.fill_ellipse(x, y, 9, 9, gelfill(x, y, 9, 50, 170))
        c.ring(x, y, 8.5, 1.2, (240, 255, 250, 220))
    for k in range(6):
        a = k * math.pi / 3 + math.pi / 6
        x, y = polar(128, 128, 66, a)
        c.fill_ellipse(x, y, 22, 22, gelfill(x, y, 22, 40, 160))
        c.ring(x, y, 21, 1.8, (230, 255, 248, 220))
        c.ellipse(x - 7, y - 7, 5, 2.4, (255, 255, 255, 220), 0.8)
    c.fill_ellipse(128, 128, 26, 26, gelfill(128, 128, 26, 60, 190))
    c.glow(128, 128, 28, (130, 255, 225), 0.95, 1.4)
    c.ring(128, 128, 14, 2.4, (240, 255, 250, 240))
    c.save("mucus_sigil")


def splat():
    c = Canvas(128, 128)
    rng = random.Random(17)
    pts = []
    for k in range(32):
        a = k * 2 * math.pi / 32
        r = 36 + (16 if k % 4 == 0 else 5) * rng.uniform(0.6, 1.2)
        pts.append(polar(64, 64, r, a))
    c.fill(pts, gelfill(64, 64, 56, 70, 190))
    for k in range(10):
        a = rng.uniform(0, 6.28)
        r = rng.uniform(46, 60)
        s = rng.uniform(2.5, 5)
        x, y = polar(64, 64, r, a)
        c.fill_ellipse(x, y, s, s, gelfill(x, y, s, 90, 200))
    c.ellipse(50, 52, 11, 5, (255, 255, 255, 200), 1.0)
    c.save("mucus_splat")


def drip():
    c = Canvas(64, 64)
    pts = [(32, 4), (40, 24), (46, 40), (42, 54), (32, 60), (22, 54), (18, 40), (24, 24)]
    c.fill(pts, gelfill(32, 40, 26, 110, 210))
    c.ellipse(26, 40, 3, 7, (255, 255, 255, 220), 0.6)
    c.ring(32, 38, 17, 1.2, (230, 255, 248, 160))
    c.save("mucus_drip")


def burst():
    c = Canvas(128, 128)
    rng = random.Random(19)
    c.ring(64, 64, 34, 3.0, (200, 255, 240, 220))
    for k in range(12):
        a = k * math.pi / 6 + rng.uniform(-0.1, 0.1)
        r = rng.uniform(38, 60)
        x, y = polar(64, 64, r, a)
        s = rng.uniform(3, 7)
        c.line([polar(64, 64, 36, a), (x, y)], 1.4, (190, 250, 232, 180))
        c.fill_ellipse(x, y, s, s, gelfill(x, y, s, 70, 210))
    c.glow(64, 64, 24, (130, 255, 225), 0.8, 1.5)
    c.save("mucus_burst")


def ring():
    c = Canvas(256, 256)
    for k, r in enumerate((116, 92, 68, 44)):
        c.ring(128, 128, r, 5 - k * 0.8, (150, 255, 226, 220 - k * 30))
        c.ring(128, 128, r - 2, 1.4, (240, 255, 250, 180 - k * 24))
    c.save("mucus_ring")


if __name__ == "__main__":
    for f in (bubble, blob, strand, sigil, splat, drip, burst, ring):
        f()
    contact_sheet(["mucus_bubble", "mucus_blob", "mucus_strand", "mucus_sigil", "mucus_splat", "mucus_drip", "mucus_burst", "mucus_ring"],
                  os.path.join(os.environ.get("SHEET_DIR", "/tmp"), "mucus_sheet.png"), 8, 128)

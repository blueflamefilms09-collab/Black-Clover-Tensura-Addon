#!/usr/bin/env python3
"""Why: the Beast Will animal spirits (3.57.0 look pass). Every will has its own spirit: a body silhouette (phase 1, "_a") and an accent layer (phase 2, "_b": wings, mane, flames, antlers, lightning),
drawn behind the player as additive billboards by client/aura/BeastWillAura.java, plus a few shared specks (leaf, shard, bolt, ember, dust) and a wind ribbon. All art is procedural (Pillow + numpy, fixed
seeds, so the output is byte-for-byte the same every run). The textures are WHITE glow maps: the painter tints them with the will's colour. Rule for additive textures: no colour under a fully
transparent pixel (SafeAdditiveTextureScanTest), so rgb is always 0 where alpha is 0.
Writes src/main/resources/assets/nusmp/textures/particle/bwspirit_*.png and, with --preview, the contact sheet. Run from the repo root: python3 tools/beastwill/gen_spirit_textures.py [--preview PATH]"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
OUT_DIR = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "particle")
S = 1024          # drawing size
OUT = 256         # texture size of the big sprites
PI = math.pi


def cr(pts, n=20):
    """Catmull-Rom samples through pts (open curve)."""
    p = [pts[0]] + list(pts) + [pts[-1]]
    out = []
    for i in range(1, len(p) - 2):
        p0, p1, p2, p3 = p[i - 1], p[i], p[i + 1], p[i + 2]
        for k in range(n):
            t = k / n
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2 + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in range(2)))
    out.append(pts[-1])
    return out


class Art:
    """A mask drawn with ordered fill (255) and erase (0) operations in unit coordinates (0..1, y down), plus a layer of bright spots (eyes)."""

    def __init__(self):
        self.m = Image.new("L", (S, S), 0)
        self.d = ImageDraw.Draw(self.m)
        self.e = Image.new("L", (S, S), 0)
        self.ed = ImageDraw.Draw(self.e)

    @staticmethod
    def px(p):
        return (p[0] * S, p[1] * S)

    def poly(self, pts, v=255, smooth=False):
        if smooth:
            pts = cr(pts + [pts[0]], 10)
        self.d.polygon([self.px(p) for p in pts], fill=v)

    def hole(self, pts, smooth=False):
        self.poly(pts, 0, smooth)

    def ell(self, cx, cy, rx, ry, v=255):
        self.d.ellipse([(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S], fill=v)

    def stroke(self, pts, w0, w1=None, v=255, bulge=1.0):
        """A tapering stroke along a smooth curve through pts; widths are full widths in unit coordinates."""
        if w1 is None:
            w1 = w0
        if len(pts) > 2:
            path = cr(pts, 24)
        else:
            path = [(pts[0][0] + (pts[1][0] - pts[0][0]) * i / 60, pts[0][1] + (pts[1][1] - pts[0][1]) * i / 60) for i in range(61)]
        n = len(path)
        for i, q in enumerate(path):
            t = i / max(1, n - 1)
            r = (w0 + (w1 - w0) * (t ** bulge if bulge != 1.0 else t)) * 0.5 * S
            x, y = q[0] * S, q[1] * S
            self.d.ellipse([x - r, y - r, x + r, y + r], fill=v)

    def gap(self, pts, w0, w1=None):
        self.stroke(pts, w0, w1, 0)

    def spot(self, cx, cy, r):
        self.ed.ellipse([(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S], fill=255)

    def flame(self, cx, cy, w, h, lean=0.0, v=255):
        self.stroke([(cx, cy), (cx + lean * h * 0.4, cy - h * 0.5), (cx + lean * h, cy - h)], w, 0.0, v, 0.8)

    def ray(self, cx, cy, ang, r0, r1, w, v=255, bend=0.0):
        a0 = (cx + math.cos(ang) * r0, cy + math.sin(ang) * r0)
        am = (cx + math.cos(ang + bend) * (r0 + r1) / 2, cy + math.sin(ang + bend) * (r0 + r1) / 2)
        a1 = (cx + math.cos(ang + bend * 1.6) * r1, cy + math.sin(ang + bend * 1.6) * r1)
        self.stroke([a0, am, a1], w, 0.0, v, 0.85)

    def mirror_x(self):
        """Adds the left-right mirror image of what is drawn so far (mask and spots)."""
        mm = self.m.transpose(Image.FLIP_LEFT_RIGHT)
        self.m = Image.fromarray(np.maximum(np.asarray(self.m), np.asarray(mm)))
        ee = self.e.transpose(Image.FLIP_LEFT_RIGHT)
        self.e = Image.fromarray(np.maximum(np.asarray(self.e), np.asarray(ee)))
        self.d = ImageDraw.Draw(self.m)
        self.ed = ImageDraw.Draw(self.e)


def noise(seed, size, sigma):
    rng = np.random.RandomState(seed)
    a = Image.fromarray((rng.rand(size, size) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(sigma))
    v = np.asarray(a).astype(np.float32)
    v = (v - v.min()) / max(1e-3, v.max() - v.min())
    return v


def edge_fade():
    """0 on the border, 1 from 7 percent inwards (smooth), so a spirit never ends in a hard cut and the corners stay transparent."""
    t = np.clip(np.minimum(np.arange(S), np.arange(S)[::-1]) / (0.07 * S), 0, 1)
    t = t * t * (3 - 2 * t)
    return (t[:, None] * t[None, :]).astype(np.float32)


def blur(arr, r):
    im = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im).astype(np.float32) / 255.0


def finish(art, name, seed, soft=0.0, rim=0.95, fill=0.30, halo=0.55, size=OUT, grad=True):
    """Turns a mask into a white additive glow map and writes it."""
    b = np.asarray(art.m).astype(np.float32) / 255.0
    if soft > 0:
        b = blur(b, soft)
    b = blur(b, 2.5)
    eyes = blur(np.asarray(art.e).astype(np.float32) / 255.0, 6)
    inner = blur(b, 16)
    edge = np.clip(b - inner, 0, 1) * 2.4
    glow = blur(b, 42) * halo
    tex = 0.55 + 0.45 * noise(seed, S, 14) + 0.25 * noise(seed + 1, S, 5)
    yy = np.linspace(0, 1, S, dtype=np.float32)[:, None]
    g = (1.0 - 0.45 * yy) if grad else 1.0
    v = fill * g * tex * b + edge * rim * b + glow * (1.0 - b * 0.6) * 0.8 + eyes * 1.3
    v = np.clip(v, 0, 1)
    # keep a clean transparent border so no sprite can draw as a square
    v *= edge_fade()
    im = Image.fromarray((v * 255).astype(np.uint8)).resize((size, size), Image.LANCZOS)
    a = np.asarray(im).astype(np.float32) / 255.0
    alpha = np.clip(a * 1.25, 0, 1)
    rgb = np.clip(a * 1.6, 0, 1)
    out = np.zeros((size, size, 4), np.uint8)
    out[..., 0] = out[..., 1] = out[..., 2] = (rgb * 255 + 0.5).astype(np.uint8)
    out[..., 3] = (alpha * 255 + 0.5).astype(np.uint8)
    out[out[..., 3] == 0, :3] = 0
    path = os.path.join(OUT_DIR, "bwspirit_%s.png" % name)
    Image.fromarray(out, "RGBA").save(path, optimize=True)
    return path


# ---------------------------------------------------------------------------------------------- the spirits
def dragon_a():
    a = Art()
    a.stroke([(0.10, 0.99), (0.20, 0.80), (0.30, 0.62), (0.42, 0.48)], 0.22, 0.12)                     # neck
    a.poly([(0.36, 0.40), (0.44, 0.30), (0.58, 0.25), (0.70, 0.29), (0.84, 0.35), (0.97, 0.39), (0.99, 0.43), (0.86, 0.47), (0.72, 0.48), (0.56, 0.50), (0.42, 0.52)])   # skull and snout
    a.poly([(0.44, 0.50), (0.58, 0.58), (0.74, 0.66), (0.92, 0.67), (0.80, 0.58), (0.68, 0.51)])      # open lower jaw
    for i in range(6):                                                                                   # fangs
        x = 0.62 + i * 0.065
        a.poly([(x, 0.47), (x + 0.02, 0.56), (x + 0.04, 0.47)])
    a.stroke([(0.46, 0.30), (0.38, 0.18), (0.26, 0.10), (0.12, 0.07)], 0.07, 0.006, bulge=0.8)        # horns
    a.stroke([(0.55, 0.27), (0.52, 0.14), (0.42, 0.03)], 0.05, 0.005)
    for k in range(5):                                                                                   # neck and back spikes
        t = k / 4
        x, y = 0.40 - 0.28 * t, 0.46 + 0.40 * t
        a.poly([(x - 0.02, y - 0.03), (x - 0.09, y - 0.12 - 0.02 * k), (x + 0.03, y - 0.06)])
    a.gap([(0.58, 0.36), (0.66, 0.34)], 0.022, 0.012)                                                    # brow gap
    a.spot(0.64, 0.375, 0.022)                                                                           # eye
    a.flame(0.82, 0.70, 0.06, 0.12, 0.5)
    a.flame(0.90, 0.72, 0.05, 0.10, 0.6)
    return finish(a, "dragon_a", 11)


def dragon_b():
    a = Art()
    # the left wing: arm, finger bones, scalloped membrane
    tips = [(0.04, 0.16), (0.02, 0.34), (0.08, 0.52), (0.20, 0.62)]
    a.poly([(0.46, 0.48), (0.26, 0.26), (0.04, 0.15), (0.015, 0.34), (0.045, 0.40), (0.04, 0.52), (0.08, 0.56), (0.12, 0.64), (0.20, 0.70), (0.30, 0.60), (0.40, 0.62), (0.46, 0.60)])
    for t in tips:
        a.gap([(0.46, 0.50), ((0.46 + t[0]) / 2, (0.50 + t[1]) / 2 - 0.03), t], 0.012, 0.004)
    a.stroke([(0.46, 0.48), (0.27, 0.27), (0.04, 0.15)], 0.045, 0.015)
    a.flame(0.40, 0.78, 0.07, 0.16, -0.3)
    a.flame(0.30, 0.82, 0.05, 0.11, -0.5)
    a.mirror_x()
    a.ell(0.5, 0.52, 0.085, 0.16)                                                                          # chest
    a.stroke([(0.5, 0.60), (0.5, 0.80), (0.46, 0.95)], 0.07, 0.01)                                      # tail
    return finish(a, "dragon_b", 12)


def panther_a():
    a = Art()
    a.stroke([(0.22, 0.44), (0.40, 0.40), (0.58, 0.42), (0.70, 0.47)], 0.20, 0.22)                    # barrel
    a.ell(0.34, 0.45, 0.16, 0.12)                                                                        # haunch
    a.stroke([(0.64, 0.46), (0.76, 0.43), (0.84, 0.46)], 0.17, 0.12)                                   # neck
    a.ell(0.86, 0.47, 0.095, 0.075)                                                                      # head
    a.poly([(0.94, 0.45), (1.0 - 0.01, 0.50), (0.93, 0.53)])                                             # muzzle
    a.poly([(0.80, 0.41), (0.79, 0.32), (0.86, 0.39)])                                                   # ears
    a.poly([(0.87, 0.39), (0.89, 0.31), (0.93, 0.40)])
    for x, y0, y1, lean in [(0.26, 0.52, 0.84, -0.04), (0.38, 0.54, 0.80, 0.05), (0.62, 0.54, 0.82, -0.05), (0.74, 0.52, 0.86, 0.06)]:     # legs mid-stride
        a.stroke([(x, y0), (x + lean * 0.5, (y0 + y1) / 2), (x + lean, y1)], 0.07, 0.04)
        a.ell(x + lean + 0.02, y1 + 0.005, 0.045, 0.02)
    a.stroke([(0.14, 0.44), (0.05, 0.40), (0.03, 0.26), (0.10, 0.16), (0.18, 0.18)], 0.06, 0.02)         # tail curling up
    a.spot(0.885, 0.455, 0.020)
    a.spot(0.840, 0.455, 0.016)
    return finish(a, "panther_a", 21)


def panther_b():
    a = Art()
    rng = np.random.RandomState(22)
    for i in range(9):                                                                                   # smoke tendrils
        x0 = 0.12 + 0.76 * (i / 8.0)
        pts = [(x0, 0.95)]
        for k in range(1, 5):
            pts.append((x0 + 0.07 * math.sin(i * 1.7 + k * 1.3) + 0.03 * (rng.rand() - 0.5), 0.95 - k * 0.21))
        a.stroke(pts, 0.11 + 0.03 * (i % 3), 0.01, bulge=0.9)
    return finish(a, "panther_b", 23, soft=22, rim=0.35, fill=0.34, halo=0.5)


def elder_a():
    a = Art()
    a.stroke([(0.50, 0.99), (0.50, 0.80), (0.50, 0.66)], 0.34, 0.24)                                    # trunk shoulders
    a.stroke([(0.20, 0.99), (0.30, 0.86), (0.40, 0.76)], 0.10, 0.05)
    a.stroke([(0.80, 0.99), (0.70, 0.86), (0.60, 0.76)], 0.10, 0.05)
    a.poly([(0.36, 0.40), (0.40, 0.30), (0.50, 0.26), (0.60, 0.30), (0.64, 0.40), (0.60, 0.58), (0.54, 0.70), (0.50, 0.72), (0.46, 0.70), (0.40, 0.58)], smooth=True)    # bark face
    a.hole([(0.42, 0.38), (0.47, 0.41), (0.46, 0.45), (0.41, 0.42)])
    a.hole([(0.58, 0.38), (0.53, 0.41), (0.54, 0.45), (0.59, 0.42)])
    a.gap([(0.50, 0.48), (0.50, 0.58)], 0.035, 0.02)
    a.spot(0.44, 0.415, 0.020)
    a.spot(0.56, 0.415, 0.020)
    for side in (-1, 1):                                                                                 # antlers: a main beam with branches
        def p(x, y): return (0.5 + side * x, y)
        a.stroke([p(0.09, 0.30), p(0.18, 0.22), p(0.24, 0.12), p(0.30, 0.03)], 0.05, 0.012)
        a.stroke([p(0.16, 0.24), p(0.24, 0.24), p(0.34, 0.18)], 0.035, 0.008)
        a.stroke([p(0.21, 0.17), p(0.30, 0.12), p(0.40, 0.09)], 0.03, 0.007)
        a.stroke([p(0.24, 0.11), p(0.20, 0.03)], 0.025, 0.006)
        a.stroke([p(0.12, 0.28), p(0.20, 0.34), p(0.30, 0.34)], 0.03, 0.007)
    return finish(a, "elder_a", 31)


def elder_b():
    a = Art()
    for side in (-1, 1):
        def p(x, y): return (0.5 + side * x, y)
        a.stroke([p(0.05, 0.92), p(0.18, 0.70), p(0.34, 0.50), p(0.42, 0.22)], 0.07, 0.01)
        a.stroke([p(0.14, 0.76), p(0.30, 0.74), p(0.44, 0.64)], 0.04, 0.008)
        a.stroke([p(0.28, 0.56), p(0.40, 0.52), p(0.48, 0.40)], 0.035, 0.008)
        a.stroke([p(0.36, 0.40), p(0.30, 0.28), p(0.34, 0.12)], 0.03, 0.007)
        a.stroke([p(0.10, 0.88), p(0.30, 0.94), p(0.46, 0.90)], 0.05, 0.01)                              # root web at the foot
        for k, (x, y) in enumerate([(0.44, 0.24), (0.34, 0.12), (0.46, 0.64), (0.40, 0.52), (0.24, 0.74), (0.48, 0.40), (0.30, 0.29)]):
            a.ell(0.5 + side * x, y, 0.028, 0.016)                                                       # leaves
    return finish(a, "elder_b", 32, rim=0.8)


def lion_a():
    a = Art()
    for i in range(22):                                                                                  # mane locks
        ang = i * 2 * PI / 22
        a.ray(0.5, 0.5, ang, 0.20, 0.47 + 0.02 * (i % 2), 0.075, bend=0.12)
    a.ell(0.5, 0.52, 0.27, 0.30)                                                                         # face
    a.hole([(0.30, 0.40), (0.42, 0.44), (0.40, 0.48), (0.30, 0.45)])                                    # eyes
    a.hole([(0.70, 0.40), (0.58, 0.44), (0.60, 0.48), (0.70, 0.45)])
    a.poly([(0.44, 0.52), (0.56, 0.52), (0.52, 0.60), (0.48, 0.60)], v=0)                               # nose
    a.poly([(0.36, 0.64), (0.64, 0.64), (0.58, 0.82), (0.42, 0.82)], v=0)                               # roaring mouth
    for x in (0.40, 0.58):                                                                               # fangs
        a.poly([(x, 0.64), (x + 0.025, 0.74), (x + 0.05, 0.64)])
    a.poly([(0.45, 0.82), (0.47, 0.76), (0.50, 0.82)])
    a.poly([(0.50, 0.82), (0.53, 0.76), (0.55, 0.82)])
    a.spot(0.355, 0.435, 0.020)
    a.spot(0.645, 0.435, 0.020)
    a.gap([(0.30, 0.34), (0.42, 0.38)], 0.02, 0.01)
    a.gap([(0.70, 0.34), (0.58, 0.38)], 0.02, 0.01)
    return finish(a, "lion_a", 41, fill=0.26)


def lion_b():
    a = Art()
    for i in range(34):                                                                                  # a sunburst of long flame-locks
        ang = i * 2 * PI / 34 + 0.04
        a.ray(0.5, 0.5, ang, 0.24, 0.49 if i % 2 else 0.42, 0.06 if i % 2 else 0.05, bend=0.16)
    for i in range(17):
        a.ray(0.5, 0.5, i * 2 * PI / 17 + 0.1, 0.20, 0.31, 0.05, bend=-0.1)
    a.ell(0.5, 0.5, 0.215, 0.215, v=0)                                                                    # room for the head
    return finish(a, "lion_b", 42, rim=1.1, fill=0.36)


def wolf_a():
    a = Art()
    a.stroke([(0.16, 0.60), (0.36, 0.57), (0.54, 0.60)], 0.22, 0.20)                                    # body
    a.ell(0.25, 0.63, 0.13, 0.14)                                                                        # haunch
    a.stroke([(0.24, 0.70), (0.19, 0.84), (0.24, 0.97)], 0.10, 0.05)                                    # hind leg
    a.stroke([(0.54, 0.68), (0.55, 0.82), (0.58, 0.97)], 0.09, 0.05)                                    # front legs
    a.stroke([(0.44, 0.68), (0.43, 0.84), (0.40, 0.97)], 0.08, 0.045)
    a.stroke([(0.13, 0.58), (0.05, 0.68), (0.07, 0.86)], 0.10, 0.012)                                   # brush tail
    a.stroke([(0.50, 0.56), (0.60, 0.44), (0.67, 0.32)], 0.18, 0.12)                                    # neck thrown back
    a.poly([(0.60, 0.32), (0.64, 0.20), (0.73, 0.16), (0.80, 0.24), (0.76, 0.34), (0.68, 0.40)], smooth=True)   # skull
    a.poly([(0.72, 0.19), (0.80, 0.09), (0.90, 0.02), (0.97, 0.03), (0.95, 0.10), (0.87, 0.16), (0.80, 0.23)])    # upper jaw, pointing at the moon
    a.poly([(0.79, 0.27), (0.87, 0.21), (0.97, 0.15), (0.95, 0.22), (0.86, 0.30), (0.78, 0.34)])                 # lower jaw
    a.poly([(0.65, 0.22), (0.61, 0.07), (0.72, 0.16)])                                                   # ear
    for k in range(7):                                                                                   # ruff and back fur
        x, y = 0.30 + k * 0.045, 0.49 - 0.012 * abs(k - 3)
        a.poly([(x, y + 0.02), (x + 0.03, y - 0.06), (x + 0.07, y + 0.02)])
    for k in range(5):
        x, y = 0.52 + k * 0.012, 0.52 - k * 0.06
        a.poly([(x, y), (x + 0.10, y + 0.05), (x + 0.01, y + 0.09)])
    a.spot(0.745, 0.245, 0.015)
    return finish(a, "wolf_a", 51)


def wolf_b():
    a = Art()
    a.ell(0.62, 0.30, 0.20, 0.20)                                                                        # a crescent moon
    a.ell(0.70, 0.26, 0.17, 0.17, v=0)
    rng = np.random.RandomState(52)
    for i in range(9):                                                                                   # frost crystals
        cx, cy, r = 0.12 + 0.76 * rng.rand(), 0.12 + 0.76 * rng.rand(), 0.025 + 0.035 * rng.rand()
        for k in range(3):
            ang = k * PI / 3
            a.stroke([(cx - math.cos(ang) * r * 1.8, cy - math.sin(ang) * r * 1.8), (cx + math.cos(ang) * r * 1.8, cy + math.sin(ang) * r * 1.8)], 0.012, 0.012)
    for i in range(3):                                                                                   # curling mist
        y = 0.62 + i * 0.12
        a.stroke([(0.04, y), (0.25, y - 0.05), (0.50, y + 0.04), (0.78, y - 0.03), (0.97, y + 0.02)], 0.06, 0.012, bulge=1.4)
    return finish(a, "wolf_b", 53, soft=3, rim=0.9)


def boar_a():
    a = Art()
    a.ell(0.45, 0.50, 0.30, 0.20)                                                                        # barrel
    a.ell(0.32, 0.38, 0.22, 0.18)                                                                        # shoulder hump
    a.poly([(0.52, 0.38), (0.70, 0.40), (0.86, 0.50), (0.96, 0.56), (0.95, 0.62), (0.82, 0.64), (0.66, 0.66), (0.54, 0.62)])   # head and snout
    a.poly([(0.60, 0.40), (0.60, 0.28), (0.68, 0.37)])                                                   # ear
    a.stroke([(0.80, 0.62), (0.88, 0.76), (0.98, 0.70), (0.99, 0.60)], 0.05, 0.008)                    # tusk
    a.stroke([(0.72, 0.64), (0.78, 0.78), (0.88, 0.80)], 0.035, 0.006)
    for k in range(8):                                                                                   # bristle crest
        x = 0.14 + k * 0.065
        a.poly([(x, 0.30 + 0.05 * abs(k - 3) / 3 * 0.0), (x + 0.02, 0.15 + 0.02 * (k % 2) + 0.01 * k * 0), (x + 0.06, 0.33)])
    for x, ph in [(0.30, -1), (0.40, 1), (0.62, -1), (0.72, 1)]:                                         # galloping legs
        a.stroke([(x, 0.62), (x + 0.06 * ph, 0.78), (x + 0.12 * ph, 0.90)], 0.08, 0.045)
        a.ell(x + 0.12 * ph + 0.01, 0.91, 0.05, 0.022)
    a.stroke([(0.17, 0.46), (0.08, 0.42), (0.05, 0.50)], 0.04, 0.01)
    a.spot(0.725, 0.50, 0.020)
    a.gap([(0.66, 0.46), (0.76, 0.48)], 0.02, 0.01)
    return finish(a, "boar_a", 61)


def boar_b():
    a = Art()
    rng = np.random.RandomState(62)
    for i in range(10):                                                                                  # speed lines
        y = 0.20 + 0.60 * rng.rand()
        x1 = 0.55 * rng.rand() + 0.2
        a.stroke([(0.02, y), (x1, y + 0.01)], 0.014, 0.004)
    for i in range(14):                                                                                  # a rolling dust bank
        a.ell(0.10 + 0.8 * rng.rand(), 0.80 + 0.12 * rng.rand(), 0.07 + 0.07 * rng.rand(), 0.05 + 0.04 * rng.rand())
    for i in range(6):                                                                                   # thrown stones
        x, y = 0.15 + 0.5 * rng.rand(), 0.55 + 0.2 * rng.rand()
        a.poly([(x, y), (x + 0.04, y - 0.015), (x + 0.05, y + 0.02), (x + 0.01, y + 0.035)])
    return finish(a, "boar_b", 63, soft=2.5, rim=0.7, fill=0.34)


def raptor_a():
    a = Art()
    a.stroke([(0.40, 0.99), (0.46, 0.80), (0.54, 0.58), (0.58, 0.40)], 0.40, 0.14)                    # broad chest and neck
    for i in range(6):                                                                                   # folded wing: feathers swept back and up
        t = i / 5.0
        a.stroke([(0.44, 0.62 - 0.05 * t), (0.30 - 0.06 * t, 0.50 - 0.10 * t), (0.12 + 0.02 * t, 0.36 - 0.12 * t)], 0.07, 0.008, bulge=0.8)
    a.ell(0.60, 0.30, 0.13, 0.11)                                                                        # head
    a.poly([(0.70, 0.22), (0.84, 0.26), (0.95, 0.36), (0.92, 0.46), (0.88, 0.40), (0.80, 0.40), (0.72, 0.36)])   # hooked beak
    for i in range(3):                                                                                   # crest
        a.stroke([(0.54 - 0.02 * i, 0.22), (0.46 - 0.04 * i, 0.12), (0.38 - 0.04 * i, 0.10 + 0.03 * i)], 0.04, 0.006)
    a.gap([(0.62, 0.25), (0.72, 0.29)], 0.025, 0.012)                                                    # brow
    a.spot(0.675, 0.30, 0.020)
    a.stroke([(0.52, 0.80), (0.56, 0.92)], 0.03, 0.02)
    return finish(a, "raptor_a", 71)


def raptor_b():
    a = Art()
    for i in range(9):                                                                                   # long feather-blades fanning up and out from the shoulder
        t = i / 8.0
        ang = -2.0 - 1.5 * t
        L = 0.50 - 0.10 * abs(t - 0.4)
        x0, y0 = 0.48, 0.55
        mid = (x0 + math.cos(ang) * L * 0.5, y0 + math.sin(ang) * L * 0.5 - 0.04)
        end = (x0 + math.cos(ang - 0.30) * L, y0 + math.sin(ang - 0.30) * L)
        a.stroke([(x0, y0), mid, end], 0.07, 0.006, bulge=0.7)
    a.mirror_x()
    a.ell(0.5, 0.58, 0.05, 0.12)
    return finish(a, "raptor_b", 72, rim=1.0)


def serpent_a():
    a = Art()
    pts = []
    for k in range(0, 41):                                                                               # an S-coil rising, tapering to the head
        t = k / 40.0
        pts.append((0.5 + 0.30 * math.sin(t * 3.2 * PI) * (1.0 - 0.35 * t), 0.97 - 0.62 * t - 0.03 * math.sin(t * 6)))
    a.stroke(pts[::4], 0.13, 0.07)
    head = pts[-1]
    a.ell(head[0], head[1] - 0.04, 0.085, 0.05)
    a.poly([(head[0] - 0.12, head[1] - 0.04), (head[0] - 0.05, head[1] - 0.12), (head[0] + 0.05, head[1] - 0.12), (head[0] + 0.12, head[1] - 0.04), (head[0] + 0.03, head[1] + 0.03), (head[0] - 0.03, head[1] + 0.03)])   # hood
    a.stroke([(head[0], head[1] + 0.02), (head[0] + 0.01, head[1] + 0.09), (head[0] - 0.03, head[1] + 0.13)], 0.012, 0.004)                                     # tongue
    a.stroke([(head[0] + 0.01, head[1] + 0.10), (head[0] + 0.05, head[1] + 0.13)], 0.012, 0.004)
    a.spot(head[0] - 0.03, head[1] - 0.05, 0.016)
    a.spot(head[0] + 0.03, head[1] - 0.05, 0.016)
    for k in range(7):                                                                                   # belly scale gaps
        q = pts[4 + k * 5]
        a.gap([(q[0] - 0.05, q[1]), (q[0] + 0.05, q[1] + 0.012)], 0.008)
    return finish(a, "serpent_a", 81)


def serpent_b():
    a = Art()
    rng = np.random.RandomState(82)
    for i in range(16):                                                                                  # flames licking round the coil
        ang = i * 2 * PI / 16
        cx, cy = 0.5 + 0.34 * math.cos(ang), 0.55 + 0.38 * math.sin(ang)
        a.flame(cx, cy + 0.06, 0.07, 0.15 + 0.06 * rng.rand(), 0.25 * math.cos(ang))
    for i in range(10):
        a.flame(0.15 + 0.7 * rng.rand(), 0.95, 0.05, 0.1 + 0.18 * rng.rand(), 0.3 * (rng.rand() - 0.5))
    return finish(a, "serpent_b", 83, rim=0.8)


def chimera_a():
    a = Art()
    a.stroke([(0.50, 0.99), (0.50, 0.85), (0.50, 0.72)], 0.46, 0.30)                                    # shoulders
    a.stroke([(0.50, 0.74), (0.50, 0.60), (0.50, 0.48)], 0.24, 0.16)                                    # lion neck
    for i in range(14):                                                                                  # lion mane
        a.ray(0.5, 0.36, i * 2 * PI / 14, 0.10, 0.25, 0.06, bend=0.14)
    a.ell(0.5, 0.37, 0.14, 0.16)
    a.hole([(0.42, 0.34), (0.48, 0.36), (0.46, 0.38), (0.42, 0.37)])
    a.hole([(0.58, 0.34), (0.52, 0.36), (0.54, 0.38), (0.58, 0.37)])
    a.poly([(0.44, 0.44), (0.56, 0.44), (0.54, 0.52), (0.46, 0.52)], v=0)
    a.poly([(0.46, 0.44), (0.47, 0.49), (0.49, 0.44)])
    a.poly([(0.51, 0.44), (0.53, 0.49), (0.54, 0.44)])
    a.spot(0.45, 0.355, 0.014); a.spot(0.55, 0.355, 0.014)
    # the goat head on the left, in profile, with curved horns
    a.stroke([(0.34, 0.78), (0.24, 0.62), (0.18, 0.50)], 0.10, 0.07)
    a.poly([(0.12, 0.44), (0.20, 0.38), (0.28, 0.42), (0.26, 0.54), (0.18, 0.58), (0.08, 0.55), (0.05, 0.49)], smooth=True)
    a.stroke([(0.20, 0.38), (0.22, 0.24), (0.14, 0.14), (0.05, 0.16)], 0.045, 0.008)
    a.stroke([(0.07, 0.56), (0.04, 0.64)], 0.03, 0.008)
    a.spot(0.15, 0.46, 0.014)
    # the serpent head on the right: a hood on a long neck
    a.stroke([(0.66, 0.80), (0.78, 0.66), (0.80, 0.50), (0.76, 0.38)], 0.09, 0.05)
    a.poly([(0.66, 0.30), (0.72, 0.22), (0.82, 0.22), (0.88, 0.30), (0.82, 0.42), (0.72, 0.42)], smooth=True)
    a.stroke([(0.77, 0.42), (0.78, 0.48), (0.74, 0.52)], 0.012, 0.004)
    a.spot(0.74, 0.31, 0.013); a.spot(0.81, 0.31, 0.013)
    return finish(a, "chimera_a", 91)


def bolt(a, p0, p1, rng, depth=1, w=0.018):
    """A zigzag bolt of straight segments, with one short fork per bolt."""
    n = 6
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    length = math.hypot(dx, dy)
    nx, ny = -dy / max(length, 1e-3), dx / max(length, 1e-3)
    pts = [p0]
    for k in range(1, n):
        o = (rng.rand() - 0.5) * 0.09
        pts.append((p0[0] + dx * k / n + nx * o, p0[1] + dy * k / n + ny * o))
    pts.append(p1)
    for k in range(n):
        a.stroke([pts[k], pts[k + 1]], w, w * 0.7)
    if depth > 0:
        m = pts[3]
        a.stroke([m, (m[0] + dx * 0.25 + nx * 0.09, m[1] + dy * 0.25 + ny * 0.09)], w * 0.7, w * 0.3)


def chimera_b():
    a = Art()
    rng = np.random.RandomState(92)
    for i in range(10):                                                                                  # a crown of lightning
        ang = i * 2 * PI / 10 + 0.15
        r0, r1 = 0.18, 0.47
        bolt(a, (0.5 + math.cos(ang) * r0, 0.45 + math.sin(ang) * r0), (0.5 + math.cos(ang) * r1, 0.45 + math.sin(ang) * r1), rng, 1, 0.024)
    return finish(a, "chimera_b", 93, rim=1.0, fill=0.2, halo=0.7)


# ---------------------------------------------------------------------------------------------- the specks
def small(name, size, draw, seed, soft=0.0):
    a = Art()
    draw(a)
    b = np.asarray(a.m).astype(np.float32) / 255.0
    b = blur(b, 2.5 + soft)
    inner = blur(b, 14)
    v = np.clip(0.55 * b + np.clip(b - inner, 0, 1) * 2.0 + blur(b, 30) * 0.5 + blur(np.asarray(a.e).astype(np.float32) / 255.0, 8), 0, 1)
    k = 8 * S // 256
    v[:k, :] = 0; v[-k:, :] = 0; v[:, :k] = 0; v[:, -k:] = 0
    im = Image.fromarray((v * 255).astype(np.uint8)).resize((size, size), Image.LANCZOS)
    f = np.asarray(im).astype(np.float32) / 255.0
    out = np.zeros((size, size, 4), np.uint8)
    out[..., :3] = (np.clip(f * 1.6, 0, 1)[..., None] * 255 + 0.5).astype(np.uint8)
    out[..., 3] = (np.clip(f * 1.25, 0, 1) * 255 + 0.5).astype(np.uint8)
    out[out[..., 3] == 0, :3] = 0
    path = os.path.join(OUT_DIR, "bwspirit_%s.png" % name)
    Image.fromarray(out, "RGBA").save(path, optimize=True)
    return path


def leaf(a):
    a.poly([(0.5, 0.08), (0.72, 0.30), (0.70, 0.62), (0.5, 0.92), (0.30, 0.62), (0.28, 0.30)], smooth=True)
    a.gap([(0.5, 0.18), (0.5, 0.82)], 0.02, 0.012)


def shard(a):
    a.poly([(0.5, 0.05), (0.58, 0.42), (0.95, 0.5), (0.58, 0.58), (0.5, 0.95), (0.42, 0.58), (0.05, 0.5), (0.42, 0.42)])


def bolt_small(a):
    a.poly([(0.60, 0.04), (0.28, 0.52), (0.46, 0.52), (0.36, 0.96), (0.74, 0.40), (0.54, 0.40)])


def ember(a):
    a.stroke([(0.5, 0.88), (0.46, 0.55), (0.52, 0.08)], 0.42, 0.0, bulge=0.8)


def dust(a):
    rng = np.random.RandomState(7)
    for i in range(9):
        a.ell(0.5 + (rng.rand() - 0.5) * 0.34, 0.5 + (rng.rand() - 0.5) * 0.28, 0.14 + 0.08 * rng.rand(), 0.11 + 0.06 * rng.rand())


def ribbon():
    """A wind ribbon: bright thread in the middle fading to both ends, 256 x 64."""
    w, h = 256, 64
    x = np.linspace(0, 1, w)[None, :]
    y = np.linspace(-1, 1, h)[:, None]
    prof = np.exp(-(y / (0.18 + 0.30 * np.sin(PI * x))) ** 2 * 1.6) * np.sin(PI * x) ** 0.8
    prof = np.clip(prof, 0, 1)
    out = np.zeros((h, w, 4), np.uint8)
    out[..., :3] = (np.clip(prof * 1.5, 0, 1)[..., None] * 255 + 0.5).astype(np.uint8)
    out[..., 3] = (np.clip(prof * 1.2, 0, 1) * 255 + 0.5).astype(np.uint8)
    out[out[..., 3] == 0, :3] = 0
    out[:2, :] = 0; out[-2:, :] = 0
    path = os.path.join(OUT_DIR, "bwspirit_ribbon.png")
    Image.fromarray(out, "RGBA").save(path, optimize=True)
    return path


BIG = [("dragon", dragon_a, dragon_b), ("panther", panther_a, panther_b), ("elder", elder_a, elder_b), ("lion", lion_a, lion_b), ("wolf", wolf_a, wolf_b),
       ("boar", boar_a, boar_b), ("raptor", raptor_a, raptor_b), ("serpent", serpent_a, serpent_b), ("chimera", chimera_a, chimera_b)]
LABEL = {"dragon": "Dragon Will", "panther": "Shadow Panther Will", "elder": "Elderwood Guardian Will", "lion": "Lion Will", "wolf": "Ridge-Wolf Will", "boar": "Crested Boar Will",
         "raptor": "Wind-Skimmer Raptor Will", "serpent": "Flame-Serpent Will", "chimera": "Thunder-Fang Chimera Will"}
TINT = {"dragon": 0x7FD6FF, "panther": 0x4B4B8C, "elder": 0x2FA371, "lion": 0xE0A93A, "wolf": 0x9AC9D0, "boar": 0x8B5A2B, "raptor": 0xA0DEC8, "serpent": 0xE04A1A, "chimera": 0x5AB4FF}
ACCENT = {"dragon": 0xFF8A3A, "panther": 0x9A6CFF, "elder": 0x8FE06A, "lion": 0xFFE6A0, "wolf": 0xE8F6FF, "boar": 0xC89B5A, "raptor": 0xFFFFFF, "serpent": 0xFFC24A, "chimera": 0xDDEEFF}
SMALL = [("leaf", leaf), ("shard", shard), ("bolt", bolt_small), ("ember", ember), ("dust", dust)]


def preview(path):
    """A contact sheet: every will's two layers, tinted and added over a dark background, labelled."""
    cell, pad = 300, 8
    font = ImageFont.load_default()
    try:
        font = ImageFont.truetype("DejaVuSans-Bold.ttf", 17)
    except OSError:
        pass
    cols = 3
    rows = 3
    sheet = Image.new("RGB", (cols * (cell * 2 + pad * 2), rows * (cell + 44)), (8, 8, 14))
    d = ImageDraw.Draw(sheet)
    for n, (k, _a, _b) in enumerate(BIG):
        cx, cy = (n % cols) * (cell * 2 + pad * 2), (n // cols) * (cell + 44)
        bg = np.zeros((cell, cell * 2, 3), np.float32) + np.array([14, 14, 24], np.float32)
        for li, suffix in enumerate(("a", "b")):
            im = Image.open(os.path.join(OUT_DIR, "bwspirit_%s_%s.png" % (k, suffix))).convert("RGBA").resize((cell, cell), Image.LANCZOS)
            arr = np.asarray(im).astype(np.float32) / 255.0
            tint = TINT[k] if suffix == "a" else ACCENT[k]
            t = np.array([(tint >> 16) & 255, (tint >> 8) & 255, tint & 255], np.float32)
            add = arr[..., :3] * arr[..., 3:4] * t * 1.5
            bg[:, li * cell:(li + 1) * cell] += add
        # the layers together on the right of the sheet row: b behind a
        both = Image.fromarray(np.clip(bg, 0, 255).astype(np.uint8))
        sheet.paste(both, (cx + pad, cy + 34))
        d.text((cx + pad + 4, cy + 8), "%s   (phase 1 body | phase 2 accent)" % LABEL[k], fill=(235, 235, 245), font=font)
    sheet.save(path)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for k, fa, fb in BIG:
        fa()
        fb()
    for name, fn in SMALL:
        small(name, 64, fn, 1)
    ribbon()
    if "--preview" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview") + 1])


if __name__ == "__main__":
    main()

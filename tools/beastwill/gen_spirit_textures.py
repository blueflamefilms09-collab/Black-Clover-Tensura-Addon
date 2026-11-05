#!/usr/bin/env python3
"""Why: the Beast Will animal spirits (3.57.0 look pass). Every will has its own spirit: a body silhouette (phase 1, "_a") and an accent layer (phase 2, "_b": wings, mane, flames, antlers, lightning),
drawn behind the player as additive billboards by client/aura/BeastWillAura.java, plus a few shared specks (leaf, shard, bolt, ember, dust) and a wind ribbon. All art is procedural (Pillow + numpy, fixed
seeds, so the output is byte-for-byte the same every run). The textures are WHITE glow maps: the painter tints them with the will's colour. Rule for additive textures: no colour under a fully
transparent pixel (SafeAdditiveTextureScanTest), so rgb is always 0 where alpha is 0.
Writes src/main/resources/assets/nusmp/textures/particle/bwspirit_*.png and, with --preview, the contact sheet. Run from the repo root: python3 tools/beastwill/gen_spirit_textures.py [--new-only] [--preview PATH] [--preview-new PATH]"""
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


# ---------------------------------------------------------------------------------------------- the 18 later spirits (docs/beastwill_new.md)
# Fenrir, Jormungandr, Cerberus, Minotaur, Hydra, Cu Sith, Nidhoggr, Hafgufa, Kraken, Typhon, Scylla and Charybdis, Sphinx, Pegasus, Yamata no Orochi, Kitsune, Nue, Ammit, Apophis. Same rules as above (white
# glow maps, fixed seeds, transparent border). For the five wills whose heads multiply (hydra, typhon, scylla, orochi; cerberus has all three painted in) the "_b" layer is ONE head on its neck, which the
# painter draws several times, swaying apart, in the full phase-2 detail (BeastWillLook.HEADS).
RUNES = {"fehu": [((0, 0), (0, 1)), ((0, 0.35), (1, 0)), ((0, 0.65), (1, 0.3))], "uruz": [((0, 1), (0, 0)), ((0, 0), (1, 0.25)), ((1, 0.25), (1, 1))],
         "thurs": [((0, 0), (0, 1)), ((0, 0.25), (1, 0.5)), ((1, 0.5), (0, 0.75))], "ansuz": [((0, 0), (0, 1)), ((0, 0), (1, 0.25)), ((0, 0.4), (1, 0.65))],
         "raido": [((0, 0), (0, 1)), ((0, 0), (1, 0.25)), ((1, 0.25), (0, 0.5)), ((0, 0.5), (1, 1))], "kenaz": [((1, 0.15), (0, 0.5)), ((0, 0.5), (1, 0.85))],
         "gebo": [((0, 0), (1, 1)), ((1, 0), (0, 1))], "tiwaz": [((0.5, 0), (0.5, 1)), ((0.5, 0), (0, 0.35)), ((0.5, 0), (1, 0.35))],
         "sowilo": [((0.8, 0), (0.2, 0.4)), ((0.2, 0.4), (0.8, 0.6)), ((0.8, 0.6), (0.2, 1))], "isa": [((0.5, 0), (0.5, 1))],
         "ingwaz": [((0.5, 0), (1, 0.5)), ((1, 0.5), (0.5, 1)), ((0.5, 1), (0, 0.5)), ((0, 0.5), (0.5, 0))],
         "dagaz": [((0, 0), (1, 1)), ((1, 0), (0, 1)), ((0, 0), (0, 1)), ((1, 0), (1, 1))]}
RUNE_NAMES = list(RUNES)


def rune(a, cx, cy, h, name, v=255, w=0.012):
    """One Elder Futhark rune of height h centred on (cx, cy); v = 0 cuts it out of the body, 255 draws it as a glowing mark."""
    for p0, p1 in RUNES[name]:
        a.stroke([(cx + (p0[0] - 0.5) * h * 0.55, cy + (p0[1] - 0.5) * h), (cx + (p1[0] - 0.5) * h * 0.55, cy + (p1[1] - 0.5) * h)], w, w, v)


def ring(a, cx, cy, rx, ry, w, v=255, a0=0.0, a1=2 * PI, n=40):
    pts = [(cx + math.cos(a0 + (a1 - a0) * i / n) * rx, cy + math.sin(a0 + (a1 - a0) * i / n) * ry) for i in range(n + 1)]
    a.stroke(pts[::2] + [pts[-1]], w, w, v)


def rot(p, ang, o=(0.0, 0.0)):
    c, s = math.cos(ang), math.sin(ang)
    return (o[0] + p[0] * c - p[1] * s, o[1] + p[0] * s + p[1] * c)


def shead(a, x, y, ang, s, eyes=True, tongue=True, hood=False, v=255):
    """A snake head at (x, y) pointing along ang (0 = up, y is down), size s."""
    pts = [(-0.030, 0.02), (-0.048, -0.05), (-0.030, -0.12), (0.0, -0.15), (0.030, -0.12), (0.048, -0.05), (0.030, 0.02)]
    a.poly([(x + rot((p[0] * s, p[1] * s), ang)[0], y + rot((p[0] * s, p[1] * s), ang)[1]) for p in pts], v, smooth=True)
    if hood:
        a.poly([(x + rot((p[0] * s, p[1] * s), ang)[0], y + rot((p[0] * s, p[1] * s), ang)[1]) for p in [(-0.09, 0.05), (-0.07, -0.04), (0.0, -0.05), (0.07, -0.04), (0.09, 0.05), (0, 0.08)]], v, smooth=True)
    if tongue:
        q0, q1, q2 = rot((0, -0.15 * s), ang, (x, y)), rot((0.004 * s, -0.20 * s), ang, (x, y)), rot((-0.012 * s, -0.235 * s), ang, (x, y))
        a.stroke([q0, q1, q2], 0.011 * s * 6, 0.004, v)
    if eyes:
        for sx in (-1, 1):
            q = rot((sx * 0.02 * s, -0.085 * s), ang, (x, y))
            a.spot(q[0], q[1], 0.011 * s * 1.6)


def dhead(a, x, y, ang, s, open_=True, v=255):
    """A dog / wolf head in profile at (x, y), muzzle pointing along ang (0 = right, y down), size s; open_ = jaws apart with fangs."""
    def T(p): q = rot((p[0] * s, p[1] * s), ang, (x, y)); return q
    a.poly([T(p) for p in [(-0.07, -0.05), (0.0, -0.075), (0.09, -0.04), (0.17, -0.02), (0.17, 0.015), (0.09, 0.03), (-0.02, 0.06), (-0.07, 0.03)]], v)
    a.poly([T(p) for p in [(-0.05, -0.06), (-0.04, -0.15), (0.02, -0.07)]], v)
    if open_:
        a.poly([T(p) for p in [(0.0, 0.03), (0.15, 0.045), (0.17, 0.075), (0.06, 0.09), (-0.03, 0.06)]], v)
        for k in range(3):
            q = T((0.07 + k * 0.035, 0.02))
            a.poly([q, T((0.075 + k * 0.035, 0.055)), T((0.09 + k * 0.035, 0.02))], v)
    q = T((0.03, -0.035))
    a.spot(q[0], q[1], 0.012 * s * 1.5)


def mound(a, cx, cy, rx, ry, seed, n=9, h=0.05):
    rng = np.random.RandomState(seed)
    for i in range(n):
        x = cx - rx + 2 * rx * (i + 0.5) / n
        a.poly([(x - 0.025, cy), (x + (rng.rand() - 0.5) * 0.03, cy - h - 0.05 * rng.rand()), (x + 0.025, cy)])


def waves(a, y0, rows=3, amp=0.025, w=0.022, seed=1, x0=0.04, x1=0.96):
    rng = np.random.RandomState(seed)
    for r in range(rows):
        ph = rng.rand() * 6
        pts = [(x0 + (x1 - x0) * i / 10, y0 + r * 0.075 + amp * math.sin(i * 1.3 + ph)) for i in range(11)]
        a.stroke(pts, w, w * 0.4, bulge=1.2)


def drips(a, rng, n, x0=0.1, x1=0.9, y0=0.6, y1=0.95, r=0.012):
    for i in range(n):
        x, y = x0 + (x1 - x0) * rng.rand(), y0 + (y1 - y0) * rng.rand()
        a.stroke([(x, y - 0.03), (x, y + 0.02)], r * 2, r * 2)
        a.ell(x, y + 0.02, r, r)


def fenrir_a():
    a = Art()
    a.stroke([(0.18, 0.56), (0.40, 0.52), (0.62, 0.56)], 0.27, 0.25)                                     # a giant wolf, standing, head low
    a.ell(0.26, 0.60, 0.14, 0.15)
    a.ell(0.62, 0.58, 0.12, 0.14)
    for x, y0, y1, w in ((0.64, 0.68, 0.97, 0.09), (0.53, 0.70, 0.97, 0.08), (0.20, 0.72, 0.97, 0.09), (0.34, 0.72, 0.97, 0.08)):
        a.stroke([(x, y0), (x - 0.01, (y0 + y1) / 2), (x + 0.005, y1)], w, w * 0.55)
    a.stroke([(0.16, 0.50), (0.07, 0.56), (0.04, 0.72), (0.08, 0.86)], 0.13, 0.02)                      # brush tail
    a.stroke([(0.60, 0.50), (0.70, 0.43), (0.78, 0.42)], 0.20, 0.15)
    a.poly([(0.74, 0.34), (0.82, 0.30), (0.90, 0.35), (0.98, 0.43), (0.98, 0.48), (0.90, 0.47), (0.84, 0.52), (0.76, 0.51)])
    a.poly([(0.84, 0.50), (0.93, 0.50), (0.96, 0.56), (0.86, 0.57)])
    a.poly([(0.76, 0.35), (0.78, 0.19), (0.84, 0.32)])
    for k in range(9):                                                                                   # the ruff along the back
        x, y = 0.22 + k * 0.05, 0.40 + 0.012 * abs(k - 4)
        a.poly([(x, y + 0.03), (x + 0.03, y - 0.07), (x + 0.08, y + 0.03)])
    for name, cx, cy in (("gebo", 0.30, 0.56), ("ansuz", 0.40, 0.55), ("fehu", 0.50, 0.57), ("tiwaz", 0.60, 0.60)):   # runes cut into the flank
        rune(a, cx, cy, 0.10, name, 0, 0.011)
    for x0, y0 in ((0.34, 0.40), (0.50, 0.37), (0.12, 0.42)):                                            # soul wisps leaving the back
        a.stroke([(x0, y0), (x0 - 0.04, y0 - 0.14), (x0 + 0.02, y0 - 0.27), (x0 - 0.02, y0 - 0.36)], 0.035, 0.0, bulge=0.7)
    a.spot(0.865, 0.385, 0.016)
    return finish(a, "fenrir_a", 101)


def fenrir_b():
    a = Art()
    ring(a, 0.5, 0.5, 0.40, 0.40, 0.012)                                                                 # a seal of runes round the spirit
    ring(a, 0.5, 0.5, 0.31, 0.31, 0.008)
    for i in range(12):
        ang = i * 2 * PI / 12 + 0.1
        rune(a, 0.5 + math.cos(ang) * 0.355, 0.5 + math.sin(ang) * 0.355, 0.075, RUNE_NAMES[i % len(RUNE_NAMES)], 255, 0.010)
    rng = np.random.RandomState(102)
    for i in range(9):                                                                                   # soul wisps
        x, y, s = 0.15 + 0.7 * rng.rand(), 0.2 + 0.65 * rng.rand(), 0.05 + 0.04 * rng.rand()
        a.ell(x, y, s * 0.45, s * 0.45)
        a.stroke([(x, y), (x + 0.03, y + s * 1.6), (x - 0.02, y + s * 3.0)], s * 0.8, 0.0, bulge=0.9)
    for i in range(4):                                                                                   # frost
        cx, cy = 0.2 + 0.6 * rng.rand(), 0.2 + 0.6 * rng.rand()
        for k in range(3):
            ang = k * PI / 3
            a.stroke([(cx - math.cos(ang) * 0.04, cy - math.sin(ang) * 0.04), (cx + math.cos(ang) * 0.04, cy + math.sin(ang) * 0.04)], 0.01, 0.01)
    return finish(a, "fenrir_b", 103, soft=2, rim=0.9)


def jormungandr_a():
    a = Art()
    n = 60
    pts = []
    for i in range(n + 1):                                                                               # the serpent that rings the world, biting its own tail
        t = i / n
        ang = -PI * 0.42 + t * 2 * PI * 0.90
        pts.append((0.5 + math.cos(ang) * 0.34, 0.52 + math.sin(ang) * 0.34))
    a.stroke(pts[::3] + [pts[-1]], 0.07, 0.15, bulge=1.3)
    for i in range(1, n, 3):                                                                             # fin spines on the outer edge
        ang = -PI * 0.42 + i / n * 2 * PI * 0.90
        x, y = 0.5 + math.cos(ang) * 0.42, 0.52 + math.sin(ang) * 0.42
        a.poly([(0.5 + math.cos(ang - 0.05) * 0.38, 0.52 + math.sin(ang - 0.05) * 0.38), (x + math.cos(ang) * 0.03, y + math.sin(ang) * 0.03), (0.5 + math.cos(ang + 0.05) * 0.38, 0.52 + math.sin(ang + 0.05) * 0.38)])
    hx, hy = pts[-1]
    a.poly([(hx - 0.05, hy - 0.07), (hx + 0.09, hy - 0.10), (hx + 0.20, hy - 0.05), (hx + 0.09, hy - 0.01), (hx - 0.03, hy + 0.04)], smooth=True)       # head, jaws on the tail
    a.stroke([(hx + 0.10, hy - 0.04), (hx + 0.19, hy + 0.03)], 0.022, 0.006)
    a.spot(hx + 0.03, hy - 0.065, 0.014)
    for k in range(7):                                                                                   # belly plates
        q = pts[8 + k * 6]
        a.gap([(q[0] - 0.03, q[1] - 0.02), (q[0] + 0.03, q[1] + 0.02)], 0.008)
    waves(a, 0.80, rows=3, amp=0.03, w=0.03, seed=111, x0=0.18, x1=0.82)                                # the sea in the middle of the ring
    return finish(a, "jormungandr_a", 111)


def jormungandr_b():
    a = Art()
    for r, (rx, ry) in enumerate(((0.44, 0.17), (0.34, 0.13), (0.24, 0.09), (0.14, 0.05))):              # a whirlpool of rings on the water
        ring(a, 0.5, 0.78, rx, ry, 0.018, a0=0.2 + r, a1=0.2 + r + 5.2)
    rng = np.random.RandomState(112)
    for i in range(6):                                                                                   # curling waves
        x, y = 0.12 + 0.76 * rng.rand(), 0.2 + 0.45 * rng.rand()
        a.stroke([(x - 0.07, y + 0.04), (x - 0.02, y - 0.03), (x + 0.04, y - 0.02), (x + 0.06, y + 0.03)], 0.03, 0.006, bulge=0.8)
    drips(a, rng, 9, 0.2, 0.8, 0.12, 0.6)
    return finish(a, "jormungandr_b", 113, soft=2)


def cerberus_a():
    a = Art()
    a.ell(0.5, 0.64, 0.25, 0.17)                                                                         # body, seen from the front
    a.ell(0.5, 0.53, 0.20, 0.13)
    for x, w in ((0.30, 0.11), (0.43, 0.09), (0.57, 0.09), (0.70, 0.11)):
        a.stroke([(x, 0.70), (x + (x - 0.5) * 0.08, 0.84), (x + (x - 0.5) * 0.14, 0.98)], w, w * 0.55)
    a.stroke([(0.74, 0.70), (0.90, 0.72), (0.94, 0.58), (0.86, 0.50)], 0.05, 0.02)                      # the snake tail
    shead(a, 0.86, 0.50, -0.8, 0.9, hood=False)
    for cx, cy, s, tilt in ((0.5, 0.20, 1.0, 0.0), (0.24, 0.30, 0.85, -0.35), (0.76, 0.30, 0.85, 0.35)):  # three heads on three necks
        a.stroke([(0.5 + (cx - 0.5) * 0.35, 0.56), (0.5 + (cx - 0.5) * 0.7, 0.42), (cx, cy + 0.08)], 0.16 * s, 0.11 * s)
        a.ell(cx, cy, 0.09 * s, 0.10 * s)
        a.ell(cx, cy + 0.075 * s, 0.055 * s, 0.06 * s)
        for sx in (-1, 1):
            a.poly([(cx + sx * 0.05 * s, cy - 0.06 * s), (cx + sx * (0.10 + 0.02 * abs(tilt)) * s, cy - 0.17 * s), (cx + sx * 0.10 * s, cy - 0.04 * s)])
            a.spot(cx + sx * 0.04 * s, cy - 0.01 * s, 0.013 * s)
        a.poly([(cx - 0.04 * s, cy + 0.09 * s), (cx + 0.04 * s, cy + 0.09 * s), (cx + 0.025 * s, cy + 0.15 * s), (cx - 0.025 * s, cy + 0.15 * s)], v=0)
        for sx in (-1, 1):
            a.poly([(cx + sx * 0.035 * s, cy + 0.085 * s), (cx + sx * 0.02 * s, cy + 0.125 * s), (cx + sx * 0.012 * s, cy + 0.085 * s)])
    return finish(a, "cerberus_a", 121)


def cerberus_b():
    a = Art()
    rng = np.random.RandomState(122)
    for cx in (0.5, 0.24, 0.76):                                                                         # hellfire rising behind each head
        for k in range(5):
            a.flame(cx + (k - 2) * 0.045, 0.34 + 0.02 * abs(k - 2), 0.07, 0.20 - 0.025 * abs(k - 2) + 0.05 * rng.rand(), (k - 2) * 0.12)
    for i in range(14):
        a.flame(0.1 + 0.8 * rng.rand(), 0.97, 0.05, 0.08 + 0.15 * rng.rand(), 0.3 * (rng.rand() - 0.5))
    for i in range(3):                                                                                   # broken chain links
        cx, cy = 0.2 + i * 0.3, 0.55 + 0.08 * (i % 2)
        a.ell(cx, cy, 0.075, 0.04)
        a.ell(cx, cy, 0.05, 0.022, 0)
    return finish(a, "cerberus_b", 123, rim=0.85)


def minotaur_a():
    a = Art()
    a.poly([(0.28, 0.42), (0.72, 0.42), (0.78, 0.50), (0.66, 0.80), (0.34, 0.80), (0.22, 0.50)], smooth=True)   # a bull-headed giant, seen from the front
    a.ell(0.27, 0.46, 0.08, 0.07)
    a.ell(0.73, 0.46, 0.08, 0.07)
    a.stroke([(0.20, 0.50), (0.15, 0.66), (0.17, 0.80)], 0.10, 0.07)
    a.stroke([(0.80, 0.50), (0.84, 0.66), (0.83, 0.80)], 0.10, 0.07)
    a.stroke([(0.40, 0.78), (0.38, 0.90), (0.36, 0.98)], 0.12, 0.08)
    a.stroke([(0.60, 0.78), (0.62, 0.90), (0.64, 0.98)], 0.12, 0.08)
    a.stroke([(0.90, 0.30), (0.90, 0.97)], 0.02, 0.02)                                                   # the double axe
    a.poly([(0.90, 0.30), (0.99, 0.24), (0.97, 0.42), (0.90, 0.40)])
    a.poly([(0.90, 0.30), (0.81, 0.24), (0.83, 0.42), (0.90, 0.40)])
    a.ell(0.5, 0.28, 0.10, 0.12)
    a.ell(0.5, 0.38, 0.065, 0.06)
    a.poly([(0.41, 0.20), (0.38, 0.10), (0.46, 0.17)])
    a.poly([(0.59, 0.20), (0.62, 0.10), (0.54, 0.17)])
    a.stroke([(0.42, 0.24), (0.30, 0.22), (0.23, 0.12), (0.25, 0.03)], 0.055, 0.008)                    # horns
    a.stroke([(0.58, 0.24), (0.70, 0.22), (0.77, 0.12), (0.75, 0.03)], 0.055, 0.008)
    a.ell(0.5, 0.435, 0.03, 0.03)                                                                        # the nose ring
    a.ell(0.5, 0.435, 0.018, 0.018, 0)
    a.spot(0.46, 0.27, 0.014)
    a.spot(0.54, 0.27, 0.014)
    for k in range(3):
        a.gap([(0.38 + k * 0.012, 0.52 + k * 0.07), (0.62 - k * 0.012, 0.52 + k * 0.07)], 0.008)
    return finish(a, "minotaur_a", 131)


def minotaur_b():
    a = Art()
    x0, y0, x1, y1 = 0.10, 0.10, 0.90, 0.90                                                              # a labyrinth: a square spiral of walls
    p = (x0, y0)
    for k in range(8):
        d = k % 4
        q = (x1, y0) if d == 0 else (x1, y1) if d == 1 else (x0, y1) if d == 2 else (x0, y0)
        a.stroke([p, q], 0.026, 0.026)
        p = q
        if d == 0: y0 += 0.1
        elif d == 1: x1 -= 0.1
        elif d == 2: y1 -= 0.1
        else: x0 += 0.1
    rng = np.random.RandomState(132)
    for i in range(5):                                                                                   # crescent horns floating
        cx, cy = 0.15 + 0.7 * rng.rand(), 0.15 + 0.7 * rng.rand()
        ring(a, cx, cy, 0.05, 0.05, 0.016, a0=PI * 0.1, a1=PI * 0.9, n=12)
    return finish(a, "minotaur_b", 133, soft=1)


def hydra_a():
    a = Art()
    a.ell(0.5, 0.80, 0.34, 0.12)                                                                         # a swamp-bound body
    a.stroke([(0.18, 0.84), (0.08, 0.92), (0.02, 0.88)], 0.10, 0.015)
    for hx, hy, mid in ((0.33, 0.22, 0.20), (0.67, 0.28, 0.80)):                                         # two heads on S-necks
        a.stroke([(0.5 + (hx - 0.5) * 0.3, 0.78), (mid, 0.62), (hx + (hx - 0.5) * 0.1, 0.42), (hx, hy + 0.06)], 0.13, 0.065)
        shead(a, hx, hy + 0.04, (hx - 0.5) * 0.5, 1.3)
    for k in range(3):                                                                                   # stumps with buds that will grow back
        x = 0.42 + k * 0.08
        a.stroke([(x, 0.76), (x + 0.01, 0.66)], 0.05, 0.035)
        a.ell(x + 0.01, 0.65, 0.022, 0.022)
    waves(a, 0.90, rows=2, amp=0.02, w=0.02, seed=141)
    return finish(a, "hydra_a", 141)


def snake_neck(a, curve, head_ang, hs, fangs=True, hood=False):
    a.stroke(curve, 0.13, 0.07)
    x, y = curve[-1]
    shead(a, x, y + 0.03, head_ang, hs, hood=hood)


def hydra_b():
    a = Art()
    snake_neck(a, [(0.5, 0.98), (0.40, 0.80), (0.60, 0.58), (0.46, 0.38), (0.5, 0.22)], 0.1, 1.5)
    rng = np.random.RandomState(142)
    drips(a, rng, 5, 0.38, 0.62, 0.12, 0.35, 0.011)
    a.ell(0.5, 0.97, 0.06, 0.03)
    return finish(a, "hydra_b", 143, soft=1)


def cu_sith_a():
    a = Art()
    a.stroke([(0.20, 0.56), (0.42, 0.50), (0.62, 0.56)], 0.22, 0.20)                                     # a shaggy hound at a run
    a.ell(0.27, 0.58, 0.12, 0.13)
    for pts in (((0.64, 0.62), (0.78, 0.74), (0.92, 0.70)), ((0.56, 0.64), (0.68, 0.80), (0.82, 0.86)), ((0.26, 0.66), (0.12, 0.80), (0.04, 0.76)), ((0.32, 0.68), (0.22, 0.84), (0.14, 0.88))):
        a.stroke(list(pts), 0.085, 0.045)
    a.stroke([(0.60, 0.50), (0.70, 0.44)], 0.17, 0.13)
    a.poly([(0.66, 0.38), (0.74, 0.33), (0.82, 0.37), (0.94, 0.42), (0.95, 0.46), (0.84, 0.47), (0.76, 0.51), (0.66, 0.49)])
    a.poly([(0.68, 0.38), (0.62, 0.24), (0.74, 0.34)])
    a.stroke([(0.20, 0.50), (0.12, 0.38), (0.20, 0.28), (0.32, 0.34), (0.30, 0.44)], 0.10, 0.03)       # the tail curled over the back
    for k in range(10):                                                                                  # shaggy coat
        x, y = 0.18 + k * 0.045, 0.62 + 0.04 * (k % 2)
        a.poly([(x, y), (x + 0.02, y + 0.10), (x + 0.05, y)])
    for k in range(8):
        x, y = 0.24 + k * 0.05, 0.43 + 0.01 * abs(k - 4)
        a.poly([(x, y + 0.02), (x + 0.03, y - 0.05), (x + 0.07, y + 0.03)])
    a.spot(0.775, 0.395, 0.017)
    return finish(a, "cu_sith_a", 151, soft=1)


def cu_sith_b():
    a = Art()
    rng = np.random.RandomState(152)
    for i in range(7):                                                                                   # corpse-candle wisps
        x, y = 0.12 + 0.76 * rng.rand(), 0.3 + 0.5 * rng.rand()
        a.ell(x, y, 0.028, 0.028)
        a.flame(x, y - 0.02, 0.05, 0.10 + 0.06 * rng.rand(), 0.4 * (rng.rand() - 0.5))
    for i in range(3):                                                                                   # moor mist
        y = 0.70 + i * 0.1
        a.stroke([(0.03, y), (0.25, y - 0.04), (0.5, y + 0.04), (0.78, y - 0.03), (0.97, y + 0.02)], 0.06, 0.01, bulge=1.4)
    for i in range(5):                                                                                   # paw prints
        x, y = 0.15 + 0.14 * i, 0.9 - 0.1 * (i % 2)
        a.ell(x, y, 0.025, 0.02)
        for k in range(3):
            a.ell(x + (k - 1) * 0.022, y - 0.032, 0.009, 0.011)
    return finish(a, "cu_sith_b", 153, rim=0.85)


def nidhoggr_a():
    a = Art()
    a.stroke([(0.10, 0.99), (0.14, 0.82), (0.28, 0.66), (0.40, 0.52)], 0.22, 0.13)                     # the dragon's neck rising from the dark
    a.poly([(0.34, 0.44), (0.42, 0.33), (0.56, 0.27), (0.70, 0.30), (0.84, 0.37), (0.96, 0.42), (0.97, 0.47), (0.84, 0.50), (0.70, 0.52), (0.56, 0.54), (0.42, 0.55)])
    a.poly([(0.44, 0.54), (0.58, 0.62), (0.76, 0.70), (0.93, 0.71), (0.80, 0.61), (0.68, 0.54)])
    for i in range(6):
        x = 0.60 + i * 0.062
        a.poly([(x, 0.50), (x + 0.02, 0.59), (x + 0.04, 0.50)])
    a.stroke([(0.46, 0.34), (0.36, 0.20), (0.22, 0.12), (0.08, 0.10)], 0.07, 0.006, bulge=0.8)
    a.stroke([(0.57, 0.30), (0.54, 0.16), (0.44, 0.05)], 0.05, 0.005)
    a.stroke([(0.62, 0.52), (0.66, 0.70), (0.58, 0.84), (0.68, 0.97)], 0.07, 0.025)                      # the root it gnaws
    for i in range(5):
        y = 0.64 + i * 0.07
        a.stroke([(0.64 + 0.01 * i, y), (0.52 - 0.02 * i, y + 0.04)], 0.016, 0.004)
    for k in range(5):
        t = k / 4
        x, y = 0.30 - 0.18 * t, 0.58 + 0.30 * t
        a.poly([(x - 0.02, y - 0.03), (x - 0.09, y - 0.11), (x + 0.03, y - 0.06)])
    a.gap([(0.58, 0.40), (0.66, 0.38)], 0.02, 0.012)
    a.spot(0.64, 0.415, 0.02)
    for k in range(6):                                                                                   # scale gaps on the neck
        a.gap([(0.10 + k * 0.045, 0.86 - k * 0.06), (0.16 + k * 0.045, 0.82 - k * 0.06)], 0.01)
    return finish(a, "nidhoggr_a", 161)


def root_branch(a, rng, x, y, ang, length, w, depth):
    ex, ey = x + math.cos(ang) * length, y + math.sin(ang) * length
    mx, my = (x + ex) / 2 + (rng.rand() - 0.5) * 0.05, (y + ey) / 2 + (rng.rand() - 0.5) * 0.05
    a.stroke([(x, y), (mx, my), (ex, ey)], w, w * 0.45)
    if depth > 0:
        root_branch(a, rng, ex, ey, ang + 0.5 + rng.rand() * 0.3, length * 0.7, w * 0.6, depth - 1)
        root_branch(a, rng, ex, ey, ang - 0.5 - rng.rand() * 0.3, length * 0.7, w * 0.6, depth - 1)


def nidhoggr_b():
    a = Art()
    rng = np.random.RandomState(162)
    for k in range(5):                                                                                   # the roots of the world-tree, spreading and rotting
        root_branch(a, rng, 0.5, 0.10, PI * (0.18 + k * 0.16), 0.24, 0.06, 2)
    for i in range(12):
        x, y = 0.1 + 0.8 * rng.rand(), 0.25 + 0.65 * rng.rand()
        a.ell(x, y, 0.02 + 0.015 * rng.rand(), 0.02 + 0.015 * rng.rand(), 0)                              # decay holes
    return finish(a, "nidhoggr_b", 163, rim=0.9)


def hafgufa_a():
    a = Art()
    a.poly([(0.04, 0.78), (0.10, 0.60), (0.24, 0.45), (0.42, 0.38), (0.60, 0.38), (0.78, 0.46), (0.92, 0.60), (0.97, 0.78)], smooth=True)   # a "island" that is a back
    mound(a, 0.5, 0.42, 0.34, 0.1, 171, 9, 0.06)                                                         # trees and rocks on it
    a.poly([(0.28, 0.76), (0.72, 0.76), (0.80, 0.92), (0.20, 0.92)], smooth=True)
    a.poly([(0.30, 0.74), (0.5, 0.64), (0.70, 0.74), (0.5, 0.96)], 0)                                    # the maw opens under the waterline
    for k in range(7):
        x = 0.33 + k * 0.057
        a.poly([(x, 0.745), (x + 0.025, 0.82), (x + 0.05, 0.745)])
        a.poly([(x, 0.93), (x + 0.025, 0.86), (x + 0.05, 0.93)])
    a.ell(0.30, 0.56, 0.035, 0.03, 0)
    a.ell(0.70, 0.56, 0.035, 0.03, 0)
    a.spot(0.30, 0.56, 0.02)
    a.spot(0.70, 0.56, 0.02)
    waves(a, 0.93, rows=1, amp=0.015, w=0.025, seed=172)
    return finish(a, "hafgufa_a", 171)


def spiral(a, cx, cy, r0, r1, turns, ph, w0, w1, sy=1.0):
    n = 50
    pts = [(cx + math.cos(ph + t * turns * 2 * PI) * (r0 + (r1 - r0) * t), cy + math.sin(ph + t * turns * 2 * PI) * (r0 + (r1 - r0) * t) * sy) for t in [i / n for i in range(n + 1)]]
    a.stroke(pts[::3] + [pts[-1]], w0, w1)


def hafgufa_b():
    a = Art()
    for k in range(3):                                                                                   # the whirl it makes as it swallows
        spiral(a, 0.5, 0.58, 0.05, 0.44, 0.9, k * 2 * PI / 3, 0.02, 0.05, 0.55)
    rng = np.random.RandomState(172)
    for i in range(9):
        x, y, r = 0.15 + 0.7 * rng.rand(), 0.15 + 0.6 * rng.rand(), 0.02 + 0.02 * rng.rand()
        a.ell(x, y, r, r)
        a.ell(x, y, r * 0.55, r * 0.55, 0)
    for x in (0.2, 0.8):
        a.stroke([(x, 0.9), (x, 0.55), (x + 0.03, 0.30), (x - 0.03, 0.18)], 0.05, 0.015)                  # spouts
    return finish(a, "hafgufa_b", 173, soft=1.5)


def kraken_a():
    a = Art()
    a.poly([(0.30, 0.46), (0.34, 0.26), (0.44, 0.10), (0.5, 0.04), (0.56, 0.10), (0.66, 0.26), (0.70, 0.46)], smooth=True)   # the mantle
    a.poly([(0.30, 0.46), (0.70, 0.46), (0.72, 0.56), (0.5, 0.62), (0.28, 0.56)], smooth=True)
    a.poly([(0.34, 0.24), (0.18, 0.12), (0.22, 0.30)])                                                   # fins
    a.poly([(0.66, 0.24), (0.82, 0.12), (0.78, 0.30)])
    a.ell(0.5, 0.50, 0.06, 0.06, 0)
    a.ell(0.5, 0.50, 0.03, 0.03)
    a.spot(0.5, 0.50, 0.024)
    tent = [[(0.34, 0.58), (0.18, 0.66), (0.10, 0.82), (0.20, 0.94)], [(0.40, 0.60), (0.32, 0.76), (0.40, 0.90), (0.30, 0.98)], [(0.46, 0.62), (0.44, 0.78), (0.52, 0.92)],
            [(0.54, 0.62), (0.58, 0.78), (0.50, 0.92)], [(0.60, 0.60), (0.68, 0.76), (0.60, 0.90), (0.70, 0.98)], [(0.66, 0.58), (0.82, 0.66), (0.90, 0.82), (0.80, 0.94)]]
    for t in tent:
        a.stroke(t, 0.10, 0.015, bulge=0.9)
        for j in range(1, 5):                                                                            # suckers
            k = int(len(t) * j / 5)
            q = t[min(k, len(t) - 1)]
            a.ell(q[0], q[1], 0.011, 0.011, 0)
    a.poly([(0.74, 0.86), (0.98, 0.86), (0.92, 0.95), (0.80, 0.95)])                                    # a ship in its grip
    a.stroke([(0.86, 0.86), (0.86, 0.66)], 0.015, 0.015)
    a.poly([(0.86, 0.68), (0.96, 0.78), (0.86, 0.80)])
    return finish(a, "kraken_a", 181)


def kraken_b():
    a = Art()
    for r, (rx, ry) in enumerate(((0.44, 0.20), (0.34, 0.15), (0.24, 0.10), (0.13, 0.055))):
        ring(a, 0.5, 0.72, rx, ry, 0.02, a0=r * 1.3, a1=r * 1.3 + 5.4)
    rng = np.random.RandomState(182)
    for i in range(4):                                                                                   # tentacle tips curling up out of the whirl
        x = 0.2 + 0.2 * i
        a.stroke([(x, 0.78), (x + 0.06, 0.6), (x - 0.02, 0.42), (x + 0.05, 0.30), (x + 0.0, 0.24)], 0.06, 0.008, bulge=0.8)
    for i in range(10):
        x, y, r = 0.1 + 0.8 * rng.rand(), 0.1 + 0.5 * rng.rand(), 0.015 + 0.02 * rng.rand()
        a.ell(x, y, r, r)
        a.ell(x, y, r * 0.55, r * 0.55, 0)
    for i in range(4):                                                                                   # broken planks
        x, y, ang = 0.15 + 0.7 * rng.rand(), 0.15 + 0.3 * rng.rand(), rng.rand() * PI
        a.stroke([(x, y), (x + math.cos(ang) * 0.1, y + math.sin(ang) * 0.04)], 0.018, 0.018)
    return finish(a, "kraken_b", 183, soft=1.5)


def typhon_a():
    a = Art()
    a.poly([(0.38, 0.30), (0.62, 0.30), (0.67, 0.42), (0.58, 0.66), (0.42, 0.66), (0.33, 0.42)], smooth=True)   # a colossal torso
    a.ell(0.5, 0.20, 0.07, 0.085)
    a.stroke([(0.44, 0.14), (0.40, 0.04)], 0.035, 0.008)
    a.stroke([(0.56, 0.14), (0.60, 0.04)], 0.035, 0.008)
    a.poly([(0.36, 0.36), (0.14, 0.14), (0.05, 0.36), (0.14, 0.50), (0.26, 0.56), (0.34, 0.50)], smooth=True)    # great wings
    a.poly([(0.64, 0.36), (0.86, 0.14), (0.95, 0.36), (0.86, 0.50), (0.74, 0.56), (0.66, 0.50)], smooth=True)
    a.stroke([(0.40, 0.64), (0.22, 0.74), (0.20, 0.90), (0.34, 0.97)], 0.14, 0.06)                      # two viper coils for legs
    a.stroke([(0.60, 0.64), (0.78, 0.74), (0.80, 0.90), (0.66, 0.97)], 0.14, 0.06)
    shead(a, 0.28, 0.40, -0.9, 1.0)                                                                      # snake heads on the shoulders
    shead(a, 0.72, 0.40, 0.9, 1.0)
    rng = np.random.RandomState(192)
    for i in range(18):                                                                                  # the hundred small heads along the edge
        ang = PI * (1.05 + 0.9 * i / 17)
        x, y = 0.5 + math.cos(ang) * 0.46, 0.55 + math.sin(ang) * 0.46
        a.stroke([(0.5 + math.cos(ang) * 0.34, 0.55 + math.sin(ang) * 0.30), (x, y)], 0.03, 0.012)
        shead(a, x, y, ang + PI / 2, 0.35, eyes=False, tongue=False)
    a.spot(0.47, 0.20, 0.014)
    a.spot(0.53, 0.20, 0.014)
    return finish(a, "typhon_a", 191)


def typhon_b():
    a = Art()
    snake_neck(a, [(0.5, 0.98), (0.60, 0.80), (0.40, 0.58), (0.54, 0.38), (0.5, 0.26)], 0.0, 1.5)
    for k in range(3):                                                                                   # storm fire out of its mouth
        a.flame(0.5 + (k - 1) * 0.05, 0.12, 0.06, 0.14, (k - 1) * 0.4)
    return finish(a, "typhon_b", 193, soft=1, rim=0.9)


def scylla_charybdis_a():
    a = Art()
    a.poly([(0.02, 0.98), (0.04, 0.60), (0.12, 0.45), (0.20, 0.52), (0.30, 0.40), (0.38, 0.56), (0.44, 0.98)])       # the cliff of Scylla
    a.stroke([(0.22, 0.54), (0.22, 0.38)], 0.16, 0.12)                                                   # her torso
    a.ell(0.22, 0.28, 0.05, 0.06)
    for k, (hx, hy, ang) in enumerate(((0.07, 0.22, -2.4), (0.38, 0.18, -0.4), (0.12, 0.08, -1.7))):  # dog heads on long necks round her waist
        a.stroke([(0.22, 0.46), ((0.22 + hx) / 2, (0.46 + hy) / 2 + 0.06), (hx, hy)], 0.05, 0.035)
        dhead(a, hx, hy, ang, 1.5)
    for r, (rx, ry) in enumerate(((0.28, 0.12), (0.22, 0.095), (0.16, 0.07), (0.10, 0.045), (0.05, 0.02))):   # Charybdis, the whirlpool
        ring(a, 0.72, 0.78, rx, ry, 0.026, a0=r, a1=r + 5.4)
    a.ell(0.72, 0.80, 0.02, 0.012)
    for k in range(6):
        x = 0.46 + k * 0.1
        a.stroke([(x, 0.96), (x + 0.03, 0.92)], 0.02, 0.01)
    return finish(a, "scylla_charybdis_a", 201)


def scylla_charybdis_b():
    a = Art()
    a.stroke([(0.5, 0.98), (0.44, 0.78), (0.58, 0.58), (0.44, 0.40), (0.52, 0.30)], 0.12, 0.07)
    dhead(a, 0.55, 0.22, -1.2, 2.0)
    return finish(a, "scylla_charybdis_b", 203, soft=1)


def sphinx_a():
    a = Art()
    a.stroke([(0.10, 0.76), (0.40, 0.72), (0.66, 0.74)], 0.28, 0.24)                                     # a lion's body at rest
    a.ell(0.20, 0.72, 0.14, 0.17)
    a.stroke([(0.62, 0.80), (0.82, 0.86), (0.98, 0.85)], 0.10, 0.09)
    a.stroke([(0.58, 0.88), (0.80, 0.93), (0.97, 0.93)], 0.08, 0.07)
    a.stroke([(0.12, 0.84), (0.04, 0.78), (0.04, 0.66), (0.10, 0.60)], 0.05, 0.02)                      # tail
    a.stroke([(0.66, 0.70), (0.72, 0.58), (0.74, 0.48)], 0.20, 0.15)
    a.poly([(0.62, 0.32), (0.74, 0.22), (0.88, 0.32), (0.91, 0.62), (0.66, 0.62)], smooth=True)          # the nemes headcloth
    a.ell(0.755, 0.40, 0.065, 0.085, 0)                                                                  # face (cut out) ...
    a.ell(0.755, 0.40, 0.055, 0.075)                                                                     # ... and drawn again lighter
    a.poly([(0.72, 0.51), (0.79, 0.51), (0.77, 0.58), (0.74, 0.58)])
    a.poly([(0.75, 0.30), (0.75, 0.16), (0.78, 0.30)])                                                   # uraeus
    a.poly([(0.48, 0.66), (0.30, 0.40), (0.14, 0.16), (0.08, 0.30), (0.14, 0.50), (0.28, 0.64)])        # one folded wing
    for k in range(5):
        a.gap([(0.40 - k * 0.06, 0.60 - k * 0.07), (0.26 - k * 0.04, 0.34 - k * 0.04)], 0.01)
    a.spot(0.735, 0.395, 0.012)
    a.spot(0.78, 0.395, 0.012)
    return finish(a, "sphinx_a", 211)


def sphinx_b():
    a = Art()
    a.stroke([(0.36, 0.30), (0.38, 0.14), (0.52, 0.09), (0.62, 0.18), (0.58, 0.32), (0.50, 0.42), (0.50, 0.54)], 0.065, 0.05)   # the riddle: a question mark
    a.ell(0.5, 0.68, 0.04, 0.04)
    ring(a, 0.5, 0.40, 0.30, 0.30, 0.01, a0=0.4, a1=5.9)
    a.ell(0.5, 0.88, 0.1, 0.045)                                                                         # the eye of judgement
    a.ell(0.5, 0.88, 0.05, 0.022, 0)
    a.spot(0.5, 0.88, 0.016)
    rng = np.random.RandomState(212)
    for i in range(10):
        a.ell(0.1 + 0.8 * rng.rand(), 0.2 + 0.7 * rng.rand(), 0.01, 0.01)
    return finish(a, "sphinx_b", 213, soft=1)


def pegasus_a():
    a = Art()
    a.stroke([(0.28, 0.68), (0.45, 0.60), (0.60, 0.52)], 0.23, 0.20)                                     # a winged stallion rearing
    a.ell(0.30, 0.70, 0.11, 0.13)
    for pts in (((0.30, 0.76), (0.25, 0.88), (0.30, 0.98)), ((0.38, 0.76), (0.40, 0.88), (0.45, 0.98))):
        a.stroke(list(pts), 0.07, 0.04)
    for pts in (((0.62, 0.52), (0.78, 0.46), (0.82, 0.60)), ((0.58, 0.56), (0.72, 0.58), (0.72, 0.70))):
        a.stroke(list(pts), 0.07, 0.04)
    a.stroke([(0.58, 0.48), (0.62, 0.34), (0.68, 0.24)], 0.17, 0.11)
    a.poly([(0.64, 0.20), (0.72, 0.13), (0.82, 0.16), (0.89, 0.25), (0.86, 0.29), (0.78, 0.27), (0.70, 0.30)])
    a.poly([(0.66, 0.18), (0.66, 0.06), (0.71, 0.14)])
    a.stroke([(0.56, 0.38), (0.46, 0.34), (0.40, 0.42)], 0.05, 0.01)                                    # mane
    a.stroke([(0.58, 0.30), (0.46, 0.22), (0.38, 0.28)], 0.045, 0.01)
    a.stroke([(0.26, 0.62), (0.12, 0.70), (0.08, 0.88)], 0.10, 0.0)                                      # tail
    for i in range(8):                                                                                   # the wing: eight primaries
        ang = math.radians(-175 + i * 14)
        a.stroke([(0.52, 0.48), (0.52 + math.cos(ang) * 0.24, 0.48 + math.sin(ang) * 0.26), (0.52 + math.cos(ang + 0.1) * (0.46 - 0.02 * i), 0.48 + math.sin(ang + 0.1) * (0.50 - 0.03 * i))], 0.075, 0.012)
    a.spot(0.80, 0.21, 0.013)
    return finish(a, "pegasus_a", 221, soft=1)


def star4(a, cx, cy, r, w=0.012):
    a.stroke([(cx - r, cy), (cx + r, cy)], w * 2, 0.0)
    a.stroke([(cx, cy - r), (cx, cy + r)], w * 2, 0.0)
    a.ell(cx, cy, r * 0.25, r * 0.25)


def pegasus_b():
    a = Art()
    ring(a, 0.5, 0.42, 0.26, 0.26, 0.016)                                                                # a halo
    rng = np.random.RandomState(222)
    pts = []
    for i in range(7):                                                                                   # a constellation
        x, y = 0.12 + 0.76 * rng.rand(), 0.1 + 0.7 * rng.rand()
        pts.append((x, y))
        star4(a, x, y, 0.045 + 0.03 * rng.rand())
    pts.sort()
    for p, q in zip(pts, pts[1:]):
        a.stroke([p, q], 0.006, 0.006)
    for i in range(5):                                                                                   # drifting feathers
        x, y, ang = 0.15 + 0.7 * rng.rand(), 0.5 + 0.4 * rng.rand(), rng.rand() * 1.2 - 0.6
        a.stroke([(x, y), (x + math.sin(ang) * 0.1, y + 0.1)], 0.03, 0.002)
    return finish(a, "pegasus_b", 223, soft=1)


def yamata_no_orochi_a():
    a = Art()
    a.ell(0.5, 0.78, 0.34, 0.13)                                                                         # one great body over eight valleys
    mound(a, 0.5, 0.70, 0.30, 0.1, 231, 8, 0.05)
    for i in range(8):                                                                                   # eight tails
        x = 0.14 + i * 0.1
        ang = (i - 3.5) * 0.19
        a.stroke([(0.5 + (x - 0.5) * 0.6, 0.86), (x, 0.93), (x + math.sin(ang) * 0.05, 0.99)], 0.055, 0.004)
    for hx, hy, mid, ang in ((0.5, 0.12, 0.5, 0.0), (0.26, 0.24, 0.30, -0.55), (0.74, 0.24, 0.70, 0.55)):  # three of the eight heads on long necks
        a.stroke([(0.5 + (hx - 0.5) * 0.5, 0.70), (mid, 0.52), (hx + (hx - 0.5) * 0.05, hy + 0.14), (hx, hy + 0.06)], 0.12, 0.06)
        shead(a, hx, hy + 0.04, ang, 1.4)
        for sx in (-1, 1):
            q = rot((sx * 0.03 * 1.4, -0.06 * 1.4), ang, (hx, hy + 0.04))
            a.stroke([q, rot((sx * 0.07 * 1.4, -0.12 * 1.4), ang, (hx, hy + 0.04))], 0.014, 0.004)
    return finish(a, "yamata_no_orochi_a", 231)


def yamata_no_orochi_b():
    a = Art()
    snake_neck(a, [(0.5, 0.98), (0.42, 0.80), (0.58, 0.58), (0.46, 0.38), (0.5, 0.22)], 0.05, 1.5)
    for sx in (-1, 1):
        a.stroke([(0.5 + sx * 0.04, 0.11), (0.5 + sx * 0.10, 0.04)], 0.016, 0.004)
        a.spot(0.5 + sx * 0.03, 0.15, 0.022)
    return finish(a, "yamata_no_orochi_b", 233, soft=1)


def kitsune_a():
    a = Art()
    a.ell(0.50, 0.72, 0.16, 0.20)                                                                        # a fox seated, nine tails fanned behind it
    a.stroke([(0.50, 0.58), (0.50, 0.46)], 0.14, 0.10)
    a.ell(0.50, 0.38, 0.09, 0.075)
    a.poly([(0.43, 0.38), (0.5, 0.52), (0.57, 0.38)])
    a.poly([(0.43, 0.34), (0.38, 0.14), (0.48, 0.30)])
    a.poly([(0.57, 0.34), (0.62, 0.14), (0.52, 0.30)])
    a.stroke([(0.42, 0.82), (0.40, 0.92), (0.38, 0.98)], 0.07, 0.04)
    a.stroke([(0.58, 0.82), (0.60, 0.92), (0.62, 0.98)], 0.07, 0.04)
    for i in range(9):
        ang = math.radians(200 + i * 17.5)                                                               # from left, over the top, to the right
        ex, ey = 0.50 + math.cos(ang) * 0.46, 0.76 + math.sin(ang) * 0.60
        mx, my = 0.50 + math.cos(ang + 0.12) * 0.26, 0.76 + math.sin(ang + 0.12) * 0.34
        a.stroke([(0.50, 0.80), (mx, my), (ex, ey)], 0.07, 0.015, bulge=0.9)
        a.gap([(ex - math.cos(ang) * 0.06, ey - math.sin(ang) * 0.07), (ex, ey)], 0.03, 0.01)           # the pale tail tips
    a.spot(0.465, 0.375, 0.012)
    a.spot(0.535, 0.375, 0.012)
    return finish(a, "kitsune_a", 241, soft=1)


def kitsune_b():
    a = Art()
    for x in (0.18, 0.82):                                                                               # a torii gate behind the fox
        a.stroke([(x, 0.30), (x, 0.96)], 0.05, 0.05)
    a.stroke([(0.08, 0.28), (0.5, 0.20), (0.92, 0.28)], 0.06, 0.06)
    a.stroke([(0.14, 0.42), (0.86, 0.42)], 0.035, 0.035)
    for i in range(9):                                                                                   # nine foxfire lights
        t = i / 8
        x, y = 0.12 + 0.76 * t, 0.62 - 0.18 * math.sin(t * PI)
        a.ell(x, y, 0.028, 0.028)
        a.flame(x, y - 0.02, 0.05, 0.09 + 0.03 * (i % 3), 0.3 * (t - 0.5))
    return finish(a, "kitsune_b", 243, soft=1, rim=0.9)


def nue_a():
    a = Art()
    a.ell(0.42, 0.52, 0.22, 0.15)                                                                        # a tanuki's round body
    for x, y0, y1 in ((0.64, 0.60, 0.97), (0.54, 0.64, 0.97), (0.26, 0.62, 0.97), (0.36, 0.66, 0.97)):   # a tiger's striped legs
        a.stroke([(x, y0), (x + 0.01, y1)], 0.10, 0.06)
        for j in range(3):
            a.gap([(x - 0.05, y0 + 0.08 + j * 0.08), (x + 0.05, y0 + 0.11 + j * 0.08)], 0.012)
    a.stroke([(0.58, 0.46), (0.68, 0.36)], 0.14, 0.11)                                                   # a monkey's face
    a.ell(0.72, 0.28, 0.09, 0.10)
    a.ell(0.72, 0.34, 0.06, 0.05, 0)
    a.ell(0.72, 0.34, 0.045, 0.035)
    a.ell(0.64, 0.26, 0.03, 0.04)
    a.ell(0.80, 0.26, 0.03, 0.04)
    a.poly([(0.64, 0.18), (0.70, 0.12), (0.76, 0.12), (0.82, 0.18)], smooth=True)
    a.stroke([(0.22, 0.46), (0.10, 0.40), (0.06, 0.26), (0.14, 0.14), (0.26, 0.10)], 0.07, 0.03)        # the snake tail, rearing
    shead(a, 0.27, 0.10, 1.2, 0.9)
    a.spot(0.69, 0.27, 0.014)
    a.spot(0.75, 0.27, 0.014)
    for k in range(5):
        a.gap([(0.24 + k * 0.08, 0.60), (0.28 + k * 0.08, 0.66)], 0.01)
    return finish(a, "nue_a", 251)


def nue_b():
    a = Art()
    rng = np.random.RandomState(252)
    for cx, cy, r in ((0.3, 0.22, 0.14), (0.5, 0.16, 0.16), (0.7, 0.22, 0.14), (0.2, 0.32, 0.10), (0.8, 0.32, 0.10), (0.5, 0.30, 0.13)):    # a black cloud
        a.ell(cx, cy, r, r * 0.75)
    for i in range(3):
        x = 0.3 + 0.2 * i
        bolt(a, (x, 0.34), (x + (rng.rand() - 0.5) * 0.2, 0.88), rng, 1, 0.026)
    for r in (0.12, 0.2, 0.28):                                                                          # the eerie cry: waves
        ring(a, 0.5, 0.74, r, r * 0.6, 0.012, a0=3.4, a1=6.0, n=14)
    return finish(a, "nue_b", 253, rim=1.0, fill=0.25)


def ammit_a():
    a = Art()
    a.ell(0.27, 0.58, 0.20, 0.21)                                                                        # a hippopotamus' hindquarters
    a.stroke([(0.34, 0.54), (0.50, 0.50), (0.64, 0.52)], 0.24, 0.22)                                     # a lion's body
    for x, y0 in ((0.18, 0.70), (0.34, 0.72), (0.58, 0.68), (0.68, 0.66)):
        a.stroke([(x, y0), (x + 0.01, 0.97)], 0.10, 0.06)
    for k in range(10):                                                                                  # the lion's mane
        ang = math.radians(150 + k * 28)
        a.ray(0.62, 0.42, ang, 0.06, 0.20, 0.06, bend=0.1)
    a.stroke([(0.62, 0.46), (0.74, 0.46)], 0.15, 0.1)
    a.poly([(0.70, 0.38), (0.80, 0.34), (0.98, 0.37), (0.99, 0.42), (0.82, 0.45), (0.72, 0.49)])         # a crocodile's jaws
    a.poly([(0.74, 0.52), (0.96, 0.51), (0.98, 0.57), (0.82, 0.62), (0.72, 0.58)])
    for k in range(6):
        x = 0.79 + k * 0.033
        a.poly([(x, 0.43), (x + 0.012, 0.51), (x + 0.026, 0.43)])
        a.poly([(x + 0.01, 0.53), (x + 0.02, 0.47), (x + 0.03, 0.53)])
    a.stroke([(0.10, 0.50), (0.04, 0.60), (0.06, 0.74)], 0.06, 0.02)
    a.ell(0.78, 0.34, 0.025, 0.02)
    a.spot(0.78, 0.34, 0.016)
    return finish(a, "ammit_a", 261)


def ammit_b():
    a = Art()
    a.stroke([(0.5, 0.18), (0.5, 0.30)], 0.03, 0.03)                                                     # the scales of Ma'at
    a.stroke([(0.14, 0.30), (0.86, 0.30)], 0.026, 0.026)
    for x in (0.14, 0.86):
        a.stroke([(x, 0.30), (x - 0.07, 0.52)], 0.01, 0.01)
        a.stroke([(x, 0.30), (x + 0.07, 0.52)], 0.01, 0.01)
        a.stroke([(x - 0.08, 0.52), (x, 0.58), (x + 0.08, 0.52)], 0.03, 0.03)
    a.poly([(0.5, 0.82), (0.36, 0.66), (0.40, 0.54), (0.5, 0.56), (0.60, 0.54), (0.64, 0.66)], smooth=True)   # a heart
    a.stroke([(0.5, 0.30), (0.5, 0.56)], 0.02, 0.02)
    a.stroke([(0.86, 0.60), (0.86, 0.44), (0.80, 0.40)], 0.02, 0.004)                                    # the feather
    a.stroke([(0.5, 0.30), (0.5, 0.96)], 0.012, 0.012)
    rng = np.random.RandomState(262)
    for i in range(8):
        a.ell(0.1 + 0.8 * rng.rand(), 0.62 + 0.3 * rng.rand(), 0.012, 0.012)
    return finish(a, "ammit_b", 263, soft=1)


def apophis_a():
    a = Art()
    pts = [(0.30, 0.99), (0.64, 0.88), (0.32, 0.72), (0.66, 0.56), (0.38, 0.42), (0.58, 0.30), (0.56, 0.20)]     # a serpent that rises in great coils
    a.stroke(pts, 0.16, 0.10)
    hx, hy = 0.56, 0.17
    a.poly([(hx - 0.07, hy + 0.05), (hx - 0.05, hy - 0.05), (hx + 0.03, hy - 0.09), (hx + 0.18, hy - 0.08), (hx + 0.27, hy - 0.04), (hx + 0.18, hy + 0.0), (hx + 0.05, hy + 0.04)], smooth=True)
    a.poly([(hx + 0.04, hy + 0.06), (hx + 0.17, hy + 0.06), (hx + 0.26, hy + 0.12), (hx + 0.12, hy + 0.14), (hx - 0.02, hy + 0.10)])
    for k in range(3):
        a.poly([(hx + 0.12 + k * 0.05, hy), (hx + 0.135 + k * 0.05, hy + 0.06), (hx + 0.15 + k * 0.05, hy)])
    ring(a, 0.28, 0.14, 0.12, 0.12, 0.04)                                                                # the sun-disc it is swallowing
    a.spot(hx + 0.02, hy - 0.04, 0.016)
    for k in range(7):
        q = pts[1] if k < 0 else (0.32 + 0.30 * math.sin(k * 0.9), 0.95 - k * 0.12)
        a.gap([(q[0] - 0.05, q[1]), (q[0] + 0.05, q[1] + 0.02)], 0.01)
    return finish(a, "apophis_a", 271)


def apophis_b():
    a = Art()
    ring(a, 0.5, 0.5, 0.26, 0.26, 0.05)                                                                  # an eclipse with a corona of dark rays
    for i in range(16):
        a.ray(0.5, 0.5, i * 2 * PI / 16, 0.31, 0.46, 0.025 if i % 2 else 0.04, bend=0.25)
    rng = np.random.RandomState(272)
    for i in range(9):                                                                                   # shards of broken order
        cx, cy = 0.1 + 0.8 * rng.rand(), 0.1 + 0.8 * rng.rand()
        if abs(cx - 0.5) < 0.3 and abs(cy - 0.5) < 0.3:
            continue
        s = 0.025 + 0.02 * rng.rand()
        a.poly([(cx, cy - s * 2), (cx + s, cy), (cx, cy + s * 1.5), (cx - s, cy)])
    return finish(a, "apophis_b", 273, rim=1.0)


NEW_BIG = [("fenrir", fenrir_a, fenrir_b), ("jormungandr", jormungandr_a, jormungandr_b), ("cerberus", cerberus_a, cerberus_b), ("minotaur", minotaur_a, minotaur_b), ("hydra", hydra_a, hydra_b),
           ("cu_sith", cu_sith_a, cu_sith_b), ("nidhoggr", nidhoggr_a, nidhoggr_b), ("hafgufa", hafgufa_a, hafgufa_b), ("kraken", kraken_a, kraken_b), ("typhon", typhon_a, typhon_b),
           ("scylla_charybdis", scylla_charybdis_a, scylla_charybdis_b), ("sphinx", sphinx_a, sphinx_b), ("pegasus", pegasus_a, pegasus_b), ("yamata_no_orochi", yamata_no_orochi_a, yamata_no_orochi_b),
           ("kitsune", kitsune_a, kitsune_b), ("nue", nue_a, nue_b), ("ammit", ammit_a, ammit_b), ("apophis", apophis_a, apophis_b)]
NEW_LABEL = {"fenrir": "Fenrir Will", "jormungandr": "Jormungandr Will", "cerberus": "Cerberus Will", "minotaur": "Minotaur Will", "hydra": "Hydra Will", "cu_sith": "Cu Sith Will",
             "nidhoggr": "Nidhoggr Will", "hafgufa": "Hafgufa Will", "kraken": "Kraken Will", "typhon": "Typhon Will (legendary)", "scylla_charybdis": "Scylla and Charybdis Will", "sphinx": "Sphinx Will",
             "pegasus": "Pegasus Will", "yamata_no_orochi": "Yamata no Orochi Will (legendary)", "kitsune": "Kitsune Will", "nue": "Nue Will", "ammit": "Ammit Will", "apophis": "Apophis Will (legendary)"}
NEW_TINT = {"fenrir": 0xD9D4FF, "jormungandr": 0x1E9AA8, "cerberus": 0x9A3FD0, "minotaur": 0xB5482B, "hydra": 0x8FB82A, "cu_sith": 0x7CFF4B, "nidhoggr": 0xB8A82A, "hafgufa": 0x2E62D0,
            "kraken": 0x0E7A66, "typhon": 0xE03CA0, "scylla_charybdis": 0x7C93B0, "sphinx": 0xE6D08A, "pegasus": 0xFFB8D8, "yamata_no_orochi": 0xD81E3C, "kitsune": 0xFF9A6A, "nue": 0x56606E, "ammit": 0xB0708A,
            "apophis": 0x6E2CF0}
NEW_ACCENT = {"fenrir": 0xFFFFFF, "jormungandr": 0xB8FFF0, "cerberus": 0xFF9A3A, "minotaur": 0xE8C890, "hydra": 0xE0FF80, "cu_sith": 0xE8FFE0, "nidhoggr": 0xC0FF60, "hafgufa": 0xA0E8FF,
              "kraken": 0xA0FFE8, "typhon": 0xFFE060, "scylla_charybdis": 0xD8F0FF, "sphinx": 0xFFF0B0, "pegasus": 0xFFFFE0, "yamata_no_orochi": 0xFF6060, "kitsune": 0xFFF0A0, "nue": 0xE0E8FF, "ammit": 0xFFD890,
              "apophis": 0xE0A0FF}


BIG = [("dragon", dragon_a, dragon_b), ("panther", panther_a, panther_b), ("elder", elder_a, elder_b), ("lion", lion_a, lion_b), ("wolf", wolf_a, wolf_b),
       ("boar", boar_a, boar_b), ("raptor", raptor_a, raptor_b), ("serpent", serpent_a, serpent_b), ("chimera", chimera_a, chimera_b)]
LABEL = {"dragon": "Dragon Will", "panther": "Shadow Panther Will", "elder": "Elderwood Guardian Will", "lion": "Lion Will", "wolf": "Ridge-Wolf Will", "boar": "Crested Boar Will",
         "raptor": "Wind-Skimmer Raptor Will", "serpent": "Flame-Serpent Will", "chimera": "Thunder-Fang Chimera Will"}
TINT = {"dragon": 0x7FD6FF, "panther": 0x4B4B8C, "elder": 0x2FA371, "lion": 0xE0A93A, "wolf": 0x9AC9D0, "boar": 0x8B5A2B, "raptor": 0xA0DEC8, "serpent": 0xE04A1A, "chimera": 0x5AB4FF}
ACCENT = {"dragon": 0xFF8A3A, "panther": 0x9A6CFF, "elder": 0x8FE06A, "lion": 0xFFE6A0, "wolf": 0xE8F6FF, "boar": 0xC89B5A, "raptor": 0xFFFFFF, "serpent": 0xFFC24A, "chimera": 0xDDEEFF}
SMALL = [("leaf", leaf), ("shard", shard), ("bolt", bolt_small), ("ember", ember), ("dust", dust)]


def preview(path, items=None):
    """A contact sheet: every will's two layers, tinted and added over a dark background, labelled. items = [(key, label, tint, accent)]; default the nine first wills."""
    if items is None:
        items = [(k, LABEL[k], TINT[k], ACCENT[k]) for k, _a, _b in BIG]
    cell, pad = 300, 8
    font = ImageFont.load_default()
    try:
        font = ImageFont.truetype("DejaVuSans-Bold.ttf", 17)
    except OSError:
        pass
    cols = 3
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (cell * 2 + pad * 2), rows * (cell + 44)), (8, 8, 14))
    d = ImageDraw.Draw(sheet)
    for n, (k, label, tint_a, tint_b) in enumerate(items):
        cx, cy = (n % cols) * (cell * 2 + pad * 2), (n // cols) * (cell + 44)
        bg = np.zeros((cell, cell * 2, 3), np.float32) + np.array([14, 14, 24], np.float32)
        for li, suffix in enumerate(("a", "b")):
            im = Image.open(os.path.join(OUT_DIR, "bwspirit_%s_%s.png" % (k, suffix))).convert("RGBA").resize((cell, cell), Image.LANCZOS)
            arr = np.asarray(im).astype(np.float32) / 255.0
            tint = tint_a if suffix == "a" else tint_b
            t = np.array([(tint >> 16) & 255, (tint >> 8) & 255, tint & 255], np.float32)
            add = arr[..., :3] * arr[..., 3:4] * t * 1.5
            bg[:, li * cell:(li + 1) * cell] += add
        both = Image.fromarray(np.clip(bg, 0, 255).astype(np.uint8))
        sheet.paste(both, (cx + pad, cy + 34))
        d.text((cx + pad + 4, cy + 8), "%s   (phase 1 body | phase 2 accent)" % label, fill=(235, 235, 245), font=font)
    sheet.save(path)


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    if "--new-only" not in sys.argv:                                                                     # --new-only: only the 18 later spirits (the old files stay byte-for-byte untouched)
        for k, fa, fb in BIG:
            fa()
            fb()
        for name, fn in SMALL:
            small(name, 64, fn, 1)
        ribbon()
    for k, fa, fb in NEW_BIG:
        fa()
        fb()
    if "--preview" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview") + 1])
    if "--preview-new" in sys.argv:
        preview(sys.argv[sys.argv.index("--preview-new") + 1], [(k, NEW_LABEL[k], NEW_TINT[k], NEW_ACCENT[k]) for k, _a, _b in NEW_BIG])


if __name__ == "__main__":
    main()

"""Generates the Sealing Magic VFX textures (deterministic, Pillow + numpy, no external images).

    python3 -B tools/gen_seal_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/seal_*.png

Look reference: docs/attributes/art_reference/seal_circles_pack.jpg (arcane circle designs: concentric rings, rune bands, hexagrams, stars,
tick rings, nodes on the vertices) and seal_inverse_release.webp (a hard white core with sharp rays). All textures are white / grey with
alpha so the vertex colour tints them; the brighter the pixel, the hotter the line.
  seal_sigil   256  hexagram sigil: double rim, rune band, tick ring, hexagram with vertex nodes, inner hexagon and eight-point star
  seal_rings   256  concentric seal rings: dashed rim, notched ring, rune band, broken thick arcs, inner diamond ring
  seal_star    256  octagram {8/3} with an {8/2} double square, tip triangles and an inner octagon
  seal_band    256x64  tileable rune band (runes, rails, separator dots)
  seal_lock    128  a padlock inside a ring: the sealed mark
  seal_glint   64   a four-point glint
  seal_crystal 128x256  a faceted seal crystal
  seal_burst   256  hard white core with sharp rays and thin shock rings
  seal_streak  256x32  a tapered streak, bright head on the right
  seal_ray     64x256  a light pillar
"""
import math
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")


class Cv:
    """A supersampled canvas. Units: x right, y down, half the HEIGHT = 1.0 (a 256x64 band spans x -4..4, y -1..1).
    A holds the coverage (alpha), H the heat (how close to white the line is)."""

    def __init__(self, w, h, ss=3):
        self.w, self.h, self.ss = w, h, ss
        self.W, self.HH = w * ss, h * ss
        self.A = np.zeros((self.HH, self.W), np.float32)
        self.H = np.zeros((self.HH, self.W), np.float32)
        self.u = 2.0 / (h * ss)
        self.cx, self.cy = self.W / 2.0, self.HH / 2.0

    def crop(self, x0, y0, x1, y1, pad):
        c0 = int(max(0, math.floor((x0 - pad) / self.u + self.cx)))
        c1 = int(min(self.W, math.ceil((x1 + pad) / self.u + self.cx)))
        r0 = int(max(0, math.floor((y0 - pad) / self.u + self.cy)))
        r1 = int(min(self.HH, math.ceil((y1 + pad) / self.u + self.cy)))
        if c1 <= c0 or r1 <= r0:
            return None
        xs = (np.arange(c0, c1) + 0.5 - self.cx) * self.u
        ys = (np.arange(r0, r1) + 0.5 - self.cy) * self.u
        X, Y = np.meshgrid(xs, ys)
        return (slice(r0, r1), slice(c0, c1)), X, Y

    def full(self):
        return self.crop(-1e3, -1e3, 1e3, 1e3, 0)

    def put(self, sl, cov, heat=1.0, gain=1.0):
        cov = np.clip(cov * gain, 0, 1).astype(np.float32)
        a = self.A[sl]
        self.A[sl] = 1 - (1 - a) * (1 - cov)
        self.H[sl] = np.maximum(self.H[sl], np.clip(cov * heat, 0, 1))

    def aa(self, d, w):
        return np.clip(0.5 - (d - w / 2) / self.u, 0, 1)

    # ---- primitives
    def seg(self, p0, p1, w, heat=1.0, glow=0.0, ga=0.35, flat=False, gain=1.0):
        (x0, y0), (x1, y1) = p0, p1
        r = self.crop(min(x0, x1), min(y0, y1), max(x0, x1), max(y0, y1), w + glow * 3 + 3 * self.u)
        if r is None:
            return
        sl, X, Y = r
        vx, vy = x1 - x0, y1 - y0
        ln = math.hypot(vx, vy) + 1e-9
        ex, ey = vx / ln, vy / ln
        t = (X - x0) * ex + (Y - y0) * ey
        p = np.abs(-(X - x0) * ey + (Y - y0) * ex)
        if flat:
            d = np.maximum(p, np.maximum(-t, t - ln))
        else:
            tc = np.clip(t, 0, ln)
            d = np.hypot(X - (x0 + ex * tc), Y - (y0 + ey * tc))
        cov = self.aa(d, w)
        if glow > 0:
            cov = np.maximum(cov, ga * np.exp(-((d / glow) ** 2)))
        self.put(sl, cov, heat * (self.aa(d, w) > 0.5), gain)

    def ring(self, r, w, heat=1.0, glow=0.0, ga=0.3, a0=None, a1=None, cx=0.0, cy=0.0, dashes=0, duty=0.6, phase=0.0, gain=1.0):
        pad = w + glow * 3 + 3 * self.u
        res = self.crop(cx - r, cy - r, cx + r, cy + r, pad)
        if res is None:
            return
        sl, X, Y = res
        dx, dy = X - cx, Y - cy
        rho = np.hypot(dx, dy)
        d = np.abs(rho - r)
        cov = self.aa(d, w)
        if glow > 0:
            cov = np.maximum(cov, ga * np.exp(-((d / glow) ** 2)))
        ang = np.arctan2(dy, dx)
        m = np.ones_like(cov)
        if a0 is not None:
            span = (a1 - a0) % (2 * math.pi)
            rel = (ang - a0) % (2 * math.pi)
            m = m * (rel <= span)
        if dashes:
            ph = ((ang + phase) / (2 * math.pi) * dashes) % 1.0
            m = m * (ph < duty)
        self.put(sl, cov * m, heat * (self.aa(d, w) > 0.5), gain)

    def disc(self, r, soft=0.0, heat=1.0, cx=0.0, cy=0.0, gain=1.0):
        res = self.crop(cx - r, cy - r, cx + r, cy + r, soft + 3 * self.u)
        if res is None:
            return
        sl, X, Y = res
        rho = np.hypot(X - cx, Y - cy)
        cov = self.aa(rho, 2 * r) if soft <= 0 else np.clip((r + soft - rho) / (soft + 1e-6), 0, 1) ** 2
        self.put(sl, cov, heat, gain)

    def glow(self, sigma, cx=0.0, cy=0.0, amp=1.0, heat=0.0, sx=None, sy=None):
        res = self.crop(cx - sigma * 3, cy - sigma * 3, cx + sigma * 3, cy + sigma * 3, 0) if sx is None else self.full()
        if res is None:
            return
        sl, X, Y = res
        gx = sigma if sx is None else sx
        gy = sigma if sy is None else sy
        cov = amp * np.exp(-(((X - cx) / gx) ** 2 + ((Y - cy) / gy) ** 2))
        self.put(sl, cov, heat)

    def poly(self, pts, w, closed=True, heat=1.0, glow=0.0, ga=0.3, flat=False):
        n = len(pts)
        for i in range(n if closed else n - 1):
            self.seg(pts[i], pts[(i + 1) % n], w, heat, glow, ga, flat)

    def fill(self, pts, heat=0.5, alpha=1.0):
        """Fill a convex polygon (counter-clockwise or clockwise)."""
        xs = [p[0] for p in pts]
        ys = [p[1] for p in pts]
        res = self.crop(min(xs), min(ys), max(xs), max(ys), 2 * self.u)
        if res is None:
            return
        sl, X, Y = res
        sgn = None
        inside = np.ones(X.shape, bool)
        n = len(pts)
        area = sum(pts[i][0] * pts[(i + 1) % n][1] - pts[(i + 1) % n][0] * pts[i][1] for i in range(n))
        for i in range(n):
            (ax, ay), (bx, by) = pts[i], pts[(i + 1) % n]
            e = (bx - ax) * (Y - ay) - (by - ay) * (X - ax)
            dist = e / (math.hypot(bx - ax, by - ay) + 1e-9) * (1 if area > 0 else -1)
            inside &= dist >= 0
        cov = inside.astype(np.float32) * alpha
        self.put(sl, cov, heat)

    def rune(self, cx, cy, ang, size, w, seed, heat=1.0):
        """A small made-up rune glyph: a few strokes on a 3x4 point grid, turned by ang (0 = upright, glyph's up = -y rotated)."""
        rng = np.random.default_rng(seed)
        gx, gy = [-0.5, 0.0, 0.5], [-1.0, -0.33, 0.33, 1.0]
        pts = [(int(rng.integers(0, 3)), int(rng.integers(0, 4)))]
        stroke = [pts[0]]
        for _ in range(int(rng.integers(3, 6))):
            for _try in range(6):
                nx = (stroke[-1][0] + int(rng.integers(-2, 3))) % 3
                ny = int(np.clip(stroke[-1][1] + int(rng.integers(-2, 3)), 0, 3))
                if (nx, ny) != stroke[-1]:
                    stroke.append((nx, ny))
                    break
        c, s = math.cos(ang), math.sin(ang)
        pp = []
        for (ix, iy) in stroke:
            lx, ly = gx[ix] * size * 0.55, gy[iy] * size * 0.5
            pp.append((cx + lx * c - ly * s, cy + lx * s + ly * c))
        for i in range(len(pp) - 1):
            self.seg(pp[i], pp[i + 1], w, heat, flat=False)
        if rng.random() < 0.5:
            lx, ly = gx[int(rng.integers(0, 3))] * size * 0.55, -1.3 * size * 0.5
            self.seg((cx + lx * c - (-1.0 * size * 0.5) * s, cy + lx * s + (-1.0 * size * 0.5) * c),
                     (cx + lx * c - ly * s, cy + lx * s + ly * c), w, heat)

    def rune_ring(self, r, size, n, w, seed, heat=0.9):
        for i in range(n):
            a = 2 * math.pi * i / n
            self.rune(r * math.cos(a), r * math.sin(a), a + math.pi / 2, size, w, seed + i * 7, heat)

    def ticks(self, r0, r1, n, w, heat=0.9, every=0, r2=None):
        for i in range(n):
            a = 2 * math.pi * i / n
            rr1 = r2 if (every and i % every == 0 and r2) else r1
            self.seg((r0 * math.cos(a), r0 * math.sin(a)), (rr1 * math.cos(a), rr1 * math.sin(a)), w, heat)

    def star(self, r, n, step, w, rot=-math.pi / 2, heat=1.0, glow=0.0, ga=0.3):
        v = [(r * math.cos(rot + 2 * math.pi * i / n), r * math.sin(rot + 2 * math.pi * i / n)) for i in range(n)]
        for i in range(n):
            self.seg(v[i], v[(i + step) % n], w, heat, glow, ga)
        return v

    def polar(self, fn, heat=0.0, gain=1.0):
        res = self.full()
        sl, X, Y = res
        rho = np.hypot(X, Y)
        ang = np.arctan2(Y, X)
        cov = fn(rho, ang, X, Y)
        self.put(sl, cov, heat, gain)

    def save(self, name):
        a = self.A.reshape(self.h, self.ss, self.w, self.ss).mean((1, 3))
        hh = self.H.reshape(self.h, self.ss, self.w, self.ss).mean((1, 3))
        hh = np.where(a > 1e-4, hh / np.maximum(a, 1e-4), 0)
        rgb = np.clip(0.62 + 0.38 * hh, 0, 1)
        rgba = np.dstack([rgb, rgb, rgb, a])
        im = Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")
        os.makedirs(OUT, exist_ok=True)
        im.save(os.path.join(OUT, name + ".png"), optimize=True)
        print("wrote", name, im.size)


def hexagram(cv, r, w, rot=-math.pi / 2, heat=1.0, glow=0.0, ga=0.3):
    return cv.star(r, 6, 2, w, rot, heat, glow, ga)


# ------------------------------------------------------------------ textures
def tex_sigil():
    cv = Cv(256, 256)
    cv.disc(0.94, soft=0.04, heat=0.0, gain=0.06)
    cv.ring(0.975, 0.014, 1.0, glow=0.03, ga=0.35)
    cv.ring(0.93, 0.024, 0.9)
    cv.ring(0.855, 0.010, 0.9)
    cv.rune_ring(0.893, 0.062, 30, 0.0075, 11)
    cv.ticks(0.80, 0.835, 90, 0.005, 0.8, every=5, r2=0.85)
    cv.ring(0.795, 0.006, 0.8)
    v = hexagram(cv, 0.78, 0.013, glow=0.022, ga=0.3)
    for (x, y) in v:
        cv.ring(0.052, 0.009, 1.0, cx=x, cy=y)
        cv.disc(0.020, heat=1.0, cx=x, cy=y)
    # inner hexagon + rotated small hexagram + rings
    cv.ring(0.405, 0.011, 1.0, glow=0.015, ga=0.3)
    cv.ring(0.365, 0.006, 0.8)
    hexagram(cv, 0.40, 0.008, rot=0.0, heat=0.9)
    cv.poly([(0.26 * math.cos(2 * math.pi * i / 6), 0.26 * math.sin(2 * math.pi * i / 6)) for i in range(6)], 0.007, heat=0.9)
    cv.ticks(0.0, 0.20, 8, 0.008, 1.0)
    cv.ring(0.135, 0.008, 1.0)
    cv.disc(0.05, heat=1.0)
    cv.glow(0.10, amp=0.5)
    cv.save("seal_sigil")


def tex_rings():
    cv = Cv(256, 256)
    cv.disc(0.9, soft=0.06, heat=0.0, gain=0.05)
    cv.ring(0.965, 0.020, 1.0, glow=0.03, ga=0.3)
    cv.ring(0.905, 0.012, 0.9, dashes=36, duty=0.62)
    cv.ring(0.845, 0.034, 1.0)
    for i in range(48):
        a = 2 * math.pi * i / 48
        cv.seg((0.808 * math.cos(a), 0.808 * math.sin(a)), (0.83 * math.cos(a), 0.83 * math.sin(a)), 0.006, 0.9)
    cv.ring(0.77, 0.007, 0.8)
    cv.rune_ring(0.705, 0.075, 22, 0.0085, 29)
    cv.ring(0.64, 0.007, 0.8)
    cv.ring(0.625, 0.012, 1.0, dashes=14, duty=0.7)
    for k in range(3):
        a0 = k * 2 * math.pi / 3 + 0.2
        cv.ring(0.52, 0.042, 1.0, a0=a0, a1=a0 + 1.55, glow=0.02, ga=0.3)
        cv.ring(0.445, 0.008, 0.9, a0=a0 + 0.6, a1=a0 + 2.0)
    cv.ring(0.38, 0.010, 1.0)
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        x, y = 0.30 * math.cos(a), 0.30 * math.sin(a)
        cv.poly([(x + 0.06 * math.cos(a + k * math.pi / 2), y + 0.06 * math.sin(a + k * math.pi / 2)) for k in range(4)], 0.009, heat=1.0)
    cv.ring(0.17, 0.010, 1.0, glow=0.02, ga=0.35)
    cv.disc(0.06, heat=1.0)
    cv.glow(0.09, amp=0.5)
    cv.save("seal_rings")


def tex_star():
    cv = Cv(256, 256)
    cv.disc(0.9, soft=0.06, heat=0.0, gain=0.05)
    cv.ring(0.965, 0.016, 1.0, glow=0.03, ga=0.3)
    cv.ring(0.90, 0.007, 0.8)
    cv.ticks(0.905, 0.94, 64, 0.006, 0.8, every=4, r2=0.955)
    v = cv.star(0.86, 8, 3, 0.014, heat=1.0, glow=0.02, ga=0.3)
    cv.star(0.62, 8, 2, 0.008, heat=0.85)
    cv.star(0.62, 8, 2, 0.008, rot=-math.pi / 2 + math.pi / 8, heat=0.7)
    cv.poly(v, 0.008, heat=0.8)
    for i, (x, y) in enumerate(v):
        a = math.atan2(y, x)
        tip = (1.02 * math.cos(a), 1.02 * math.sin(a))
        b1 = (0.93 * math.cos(a + 0.07), 0.93 * math.sin(a + 0.07))
        b2 = (0.93 * math.cos(a - 0.07), 0.93 * math.sin(a - 0.07))
        cv.fill([tip, b1, b2], 1.0, 1.0)
        cv.disc(0.022, heat=1.0, cx=x, cy=y)
    cv.poly([(0.36 * math.cos(2 * math.pi * i / 8 + math.pi / 8), 0.36 * math.sin(2 * math.pi * i / 8 + math.pi / 8)) for i in range(8)], 0.009, heat=0.9)
    cv.ring(0.2, 0.010, 1.0)
    cv.rune_ring(0.5, 0.05, 16, 0.006, 53, 0.7)
    cv.disc(0.07, heat=1.0)
    cv.glow(0.1, amp=0.5)
    cv.save("seal_star")


def tex_band():
    cv = Cv(256, 64)
    cv.seg((-4.2, -0.86), (4.2, -0.86), 0.05, 1.0)
    cv.seg((-4.2, 0.86), (4.2, 0.86), 0.05, 1.0)
    cv.seg((-4.2, -0.68), (4.2, -0.68), 0.016, 0.8)
    cv.seg((-4.2, 0.68), (4.2, 0.68), 0.016, 0.8)
    n = 12
    for i in range(n):
        cx = -4 + (i + 0.5) * 8 / n
        cv.rune(cx, 0.0, 0.0, 0.8, 0.06, 101 + i * 5, 0.95)
        sx = -4 + i * 8 / n
        cv.disc(0.07, heat=1.0, cx=sx, cy=0.0)
        cv.poly([(sx - 0.14, 0), (sx, -0.14), (sx + 0.14, 0), (sx, 0.14)], 0.02, heat=0.9)
    cv.save("seal_band")


def tex_lock():
    cv = Cv(128, 128)
    cv.ring(0.95, 0.04, 1.0, glow=0.07, ga=0.35)
    cv.ring(0.84, 0.014, 0.8)
    cv.ring(0.30, 0.095, 1.0, a0=math.pi, a1=2 * math.pi, cy=-0.12, glow=0.03, ga=0.3)
    cv.seg((-0.30, -0.12), (-0.30, 0.04), 0.095, 1.0)
    cv.seg((0.30, -0.12), (0.30, 0.04), 0.095, 1.0)
    body = [(-0.47, 0.0), (0.47, 0.0), (0.47, 0.64), (-0.47, 0.64)]
    cv.fill(body, 0.55, 0.55)
    cv.poly(body, 0.06, heat=1.0, glow=0.04, ga=0.3, flat=True)
    cv.disc(0.105, heat=1.0, cx=0, cy=0.27)
    cv.fill([(-0.04, 0.3), (0.04, 0.3), (0.075, 0.5), (-0.075, 0.5)], 1.0, 1.0)
    cv.glow(0.14, cy=0.27, amp=0.5)
    cv.save("seal_lock")


def tex_glint():
    cv = Cv(64, 64)
    res = cv.full()
    sl, X, Y = res
    ax, ay = np.abs(X), np.abs(Y)
    cross = np.exp(-(ay / 0.05)) * np.clip(1 - ax, 0, 1) ** 1.8 + np.exp(-(ax / 0.05)) * np.clip(1 - ay, 0, 1) ** 1.8
    dx, dy = np.abs(X + Y) * 0.7071, np.abs(X - Y) * 0.7071
    dia = (np.exp(-(dy / 0.05)) * np.clip(1 - np.hypot(X, Y) / 0.5, 0, 1) ** 2 + np.exp(-(dx / 0.05)) * np.clip(1 - np.hypot(X, Y) / 0.5, 0, 1) ** 2) * 0.5
    core = np.exp(-((X * X + Y * Y) / 0.012))
    cv.put(sl, np.clip(cross + dia + 0.8 * core, 0, 1), np.clip(cross * 0.8 + core, 0, 1))
    cv.save("seal_glint")


def tex_crystal():
    cv = Cv(128, 256)
    # (the canvas is 128 wide = x -0.5..0.5 in units of half-height 1; keep the crystal inside)
    top, bot = (0, -0.94), (0, 0.94)
    sl_, sr_ = (-0.40, -0.42), (0.40, -0.42)
    bl_, br_ = (-0.40, 0.42), (0.40, 0.42)
    mid = [(-0.15, -0.42), (0.15, -0.42)]
    midb = [(-0.15, 0.42), (0.15, 0.42)]
    cv.glow(0.5, amp=0.18, sx=0.34, sy=0.8)
    facets = [
        ([top, sl_, mid[0]], 0.30), ([top, mid[0], mid[1]], 0.95), ([top, mid[1], sr_], 0.55),
        ([sl_, mid[0], midb[0], bl_], 0.25), ([mid[0], mid[1], midb[1], midb[0]], 0.75), ([mid[1], sr_, br_, midb[1]], 0.45),
        ([bl_, midb[0], bot], 0.20), ([midb[0], midb[1], bot], 0.60), ([midb[1], br_, bot], 0.35),
    ]
    for pts, h in facets:
        cv.fill(pts, h, 0.62)
    outline = [top, sr_, br_, bot, bl_, sl_]
    cv.poly(outline, 0.022, heat=1.0, glow=0.04, ga=0.3)
    for p, q in [(mid[0], midb[0]), (mid[1], midb[1]), (top, mid[0]), (top, mid[1]), (bot, midb[0]), (bot, midb[1]), (sl_, sr_), (bl_, br_)]:
        if p != q:
            cv.seg(p, q, 0.011, 0.85)
    cv.seg((-0.27, -0.7), (-0.1, -0.2), 0.03, 1.0)
    cv.glow(0.05, cx=-0.16, cy=-0.45, amp=0.8, heat=1.0)
    cv.save("seal_crystal")


def tex_burst():
    cv = Cv(256, 256)

    def fn(rho, ang, X, Y):
        out = np.exp(-(rho / 0.14) ** 2) * 1.2 + np.exp(-(rho / 0.42) ** 2) * 0.28
        rng = np.random.default_rng(7)
        for i in range(18):
            a = 2 * math.pi * i / 18 + (rng.random() - 0.5) * 0.1
            ln = 0.55 + 0.45 * rng.random() if i % 2 == 0 else 0.30 + 0.25 * rng.random()
            dang = np.abs((ang - a + math.pi) % (2 * math.pi) - math.pi)
            wd = 0.012 + 0.07 * np.clip(1 - rho / ln, 0, 1) ** 2
            out = np.maximum(out, np.exp(-((dang * np.maximum(rho, 0.02)) / (0.01 + 0.03 * np.clip(1 - rho / ln, 0, 1))) ** 2) * np.clip(1 - rho / ln, 0, 1) ** 1.3 * 0.95)
        return out

    cv.polar(fn, heat=1.0)
    cv.ring(0.58, 0.010, 1.0, glow=0.02, ga=0.3)
    cv.ring(0.90, 0.006, 0.7, dashes=40, duty=0.5)
    cv.ring(0.34, 0.006, 0.8)
    cv.ring(0.86, 0.004, 0.6)
    cv.save("seal_burst")


def tex_streak():
    cv = Cv(256, 32)
    res = cv.full()
    sl, X, Y = res
    u = (X + 8.0) / 16.0
    v = Y
    wd = 0.10 + 0.55 * u
    body = np.exp(-((v / wd) ** 2) * 2.0) * np.clip(u, 0, 1) ** 2.0
    core = np.exp(-((v / (wd * 0.3)) ** 2) * 2.0) * np.clip(u, 0, 1) ** 2.6
    head = np.exp(-(((1 - u) / 0.05) ** 2)) * np.exp(-((v / 0.5) ** 2))
    cv.put(sl, np.clip(body * 0.8 + core + head * 0.6, 0, 1) * np.clip(1 - np.maximum(u - 0.985, 0) * 60, 0, 1), np.clip(core + head, 0, 1))
    cv.save("seal_streak")


def tex_ray():
    cv = Cv(64, 256)
    res = cv.full()
    sl, X, Y = res
    x = X / 0.25
    vy = (Y + 1) / 2
    prof = np.exp(-(x * x) * 2.2)
    core = np.exp(-(X / 0.045) ** 2)
    fade = (np.clip(vy, 0, 1) ** 1.3) * np.clip((1 - vy) * 12, 0, 1)
    cv.put(sl, np.clip((prof * 0.55 + core) * fade * 1.15, 0, 1), np.clip(core * fade, 0, 1))
    chev = np.clip(np.sin((Y * 14 - np.abs(X) * 22)) , 0, 1) ** 8 * prof * fade * 0.45
    cv.put(sl, chev, 0.5)
    cv.save("seal_ray")


def main():
    for f in (tex_sigil, tex_rings, tex_star, tex_band, tex_lock, tex_glint, tex_crystal, tex_burst, tex_streak, tex_ray):
        f()


if __name__ == "__main__":
    main()

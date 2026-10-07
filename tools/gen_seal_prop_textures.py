"""Generates the Sealing Magic prop textures: the four basic-seal circle designs (fantasy, sci-fi, gothic, anime), the three seal shapes
(hexagram, concentric rings, star) and the Eternal Prison cube parts (pane, edge, flare, speed line, rune cross, cracks).

Deterministic and font-free (runes are drawn from strokes). White with alpha so the vertex colour tints them, except the cube pane.
Output: src/main/resources/assets/nusmp/textures/prop/seal_*.png

    python tools/gen_seal_prop_textures.py

Look reference: docs/attributes/art_reference/seal_circles_pack.jpg (the four circle families) and seal_eternal_prison.webp (pane, bright rim, speed lines).
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "prop")
SS = 3

GLYPHS = [
    [(0, -1, 0, 1)],
    [(0, -1, 0, 1), (-0.5, -0.6, 0.5, 0.6)],
    [(0, -1, 0, 1), (-0.5, 0, 0.5, 0)],
    [(0, -1, 0, 1), (0, -0.3, 0.5, -1), (0, 0.2, 0.5, 1)],
    [(0, -1, 0, 1), (-0.5, -1, 0, -0.3), (0, 0.2, -0.5, 1)],
    [(0, -1, 0.5, -0.4), (0.5, -0.4, 0, 0.2), (0, 0.2, 0, 1), (-0.5, 0.5, 0.5, 0.5)],
    [(-0.5, -1, 0.5, 1), (0.5, -1, -0.5, 1)],
    [(-0.5, -1, 0.5, -1), (0, -1, 0, 1), (-0.5, 1, 0.5, 1)],
    [(-0.5, -1, 0, 0), (0, 0, 0.5, -1), (0, 0, 0, 1)],
    [(0, -1, -0.5, -0.3), (-0.5, -0.3, 0.5, 0.3), (0.5, 0.3, 0, 1)],
]


class Cv:
    """A supersampled mask canvas in unit coordinates: centre (0,0), radius 1 reaches the tile edge."""

    def __init__(self, w, h=None):
        self.w, self.h = w, h or w
        self.im = Image.new("L", (self.w * SS, self.h * SS), 0)
        self.d = ImageDraw.Draw(self.im)
        self.r = min(self.w, self.h) * SS / 2 - 2 * SS

    def xy(self, x, y):
        return (self.w * SS / 2 + x * self.r, self.h * SS / 2 + y * self.r)

    def px(self, wpx):
        return max(1, int(round(wpx * SS)))

    def line(self, pts, wpx=1.5, v=255):
        self.d.line([self.xy(*p) for p in pts], fill=v, width=self.px(wpx), joint="curve")

    def seg(self, x0, y0, x1, y1, wpx=1.5, v=255):
        self.line([(x0, y0), (x1, y1)], wpx, v)

    def ring(self, r, wpx=1.5, v=255, cx=0.0, cy=0.0):
        x, y = self.xy(cx, cy)
        rr = r * self.r
        self.d.ellipse([x - rr, y - rr, x + rr, y + rr], outline=v, width=self.px(wpx))

    def disc(self, r, v=255, cx=0.0, cy=0.0):
        x, y = self.xy(cx, cy)
        rr = r * self.r
        self.d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=v)

    def arc(self, r, a0, a1, wpx=1.5, v=255, cx=0.0, cy=0.0):
        n = max(6, int(abs(a1 - a0) / 0.05))
        self.line([(cx + r * math.cos(a0 + (a1 - a0) * i / n), cy + r * math.sin(a0 + (a1 - a0) * i / n)) for i in range(n + 1)], wpx, v)

    def poly(self, pts, wpx=1.5, v=255, close=True):
        p = list(pts) + ([pts[0]] if close else [])
        self.line(p, wpx, v)

    def ngon(self, n, r, rot=0.0, wpx=1.5, cx=0.0, cy=0.0):
        self.poly([(cx + r * math.cos(rot + math.tau * i / n), cy + r * math.sin(rot + math.tau * i / n)) for i in range(n)], wpx)

    def star(self, n, k, r, rot=0.0, wpx=1.5):
        pts = [(r * math.cos(rot + math.tau * i / n), r * math.sin(rot + math.tau * i / n)) for i in range(n)]
        for i in range(n):
            a, b = pts[i], pts[(i + k) % n]
            self.seg(a[0], a[1], b[0], b[1], wpx)

    def glyph(self, r, ang, size, rng, wpx=1.2, v=255):
        """A rune whose up points away from the centre, at radius r, angle ang, height size (unit coordinates)."""
        g = GLYPHS[rng.randrange(len(GLYPHS))]
        ca, sa = math.cos(ang), math.sin(ang)
        for (x0, y0, x1, y1) in g:
            pts = []
            for (x, y) in ((x0, y0), (x1, y1)):
                lx, ly = x * size * 0.42, -y * size * 0.5          # local: ly along the radius (up = outward)
                pts.append((ca * (r + ly) - sa * lx, sa * (r + ly) + ca * lx))
            self.seg(pts[0][0], pts[0][1], pts[1][0], pts[1][1], wpx, v)

    def ticks(self, r0, r1, n, wpx=1.0, off=0.0):
        for i in range(n):
            a = off + math.tau * i / n
            self.seg(r0 * math.cos(a), r0 * math.sin(a), r1 * math.cos(a), r1 * math.sin(a), wpx)

    def dots(self, r, n, rad, off=0.0):
        for i in range(n):
            a = off + math.tau * i / n
            self.disc(rad, cx=r * math.cos(a), cy=r * math.sin(a))

    def mask(self):
        return np.asarray(self.im.resize((self.w, self.h), Image.LANCZOS), dtype=np.float32) / 255.0


def finish(m, name, glow=(2.0, 5.0), gain=(0.7, 0.45), tint=None):
    """mask (0..1) -> RGBA: core white, soft halo around it; the alpha carries everything."""
    h, w = m.shape
    img = Image.fromarray((m * 255).astype(np.uint8))
    g1 = np.asarray(img.filter(ImageFilter.GaussianBlur(glow[0])), dtype=np.float32) / 255.0
    g2 = np.asarray(img.filter(ImageFilter.GaussianBlur(glow[1])), dtype=np.float32) / 255.0
    a = np.clip(m + g1 * gain[0] + g2 * gain[1], 0, 1)
    rgb = np.ones((h, w, 3), dtype=np.float32)
    if tint is not None:
        rgb = np.clip(np.array(tint, dtype=np.float32) / 255.0 + (1 - np.array(tint, dtype=np.float32) / 255.0) * np.clip(m, 0, 1)[:, :, None] ** 2, 0, 1)
    out = np.dstack([rgb, a])
    Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, name))


def radial(n, f):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.sqrt(((xx - n / 2 + 0.5) / (n / 2)) ** 2 + ((yy - n / 2 + 0.5) / (n / 2)) ** 2)
    return f(r)


# ============================================================================================================ outer rings
def outer_fantasy(rng):
    c = Cv(512)
    c.ring(0.995, 2.6); c.ring(0.955, 1.2); c.ring(0.745, 2.2); c.ring(0.775, 1.0)
    for i in range(40):
        c.glyph(0.875, math.tau * i / 40, 0.11, rng, 1.6)
    for i in range(8):
        a = math.tau * i / 8
        x, y = 0.865 * math.cos(a + math.tau / 80), 0.865 * math.sin(a + math.tau / 80)
        c.poly([(x * 1.0 + 0.045 * math.cos(a), y + 0.045 * math.sin(a)), (x + 0.045 * math.cos(a + 1.57), y + 0.045 * math.sin(a + 1.57)),
                (x - 0.045 * math.cos(a), y - 0.045 * math.sin(a)), (x + 0.045 * math.cos(a - 1.57), y + 0.045 * math.sin(a - 1.57))], 1.4)
    c.ticks(0.955, 0.985, 80, 1.0)
    c.dots(0.775, 24, 0.008)
    return c.mask()


def outer_scifi(rng):
    c = Cv(512)
    c.ring(0.995, 1.6); c.ring(0.76, 1.4); c.ring(0.735, 0.8)
    for i in range(180):
        a = math.tau * i / 180
        big = i % 15 == 0
        mid = i % 5 == 0
        r0 = 0.905 if big else (0.93 if mid else 0.95)
        c.seg(r0 * math.cos(a), r0 * math.sin(a), 0.985 * math.cos(a), 0.985 * math.sin(a), 2.0 if big else 1.0)
    for k in range(7):
        a0 = rng.uniform(0, math.tau)
        c.arc(0.855, a0, a0 + rng.uniform(0.35, 1.1), rng.uniform(3, 7))
    for k in range(5):
        a = rng.uniform(0, math.tau)
        x, y = 0.80 * math.cos(a), 0.80 * math.sin(a)
        c.disc(0.018, cx=x, cy=y); c.ring(0.035, 1.0, cx=x, cy=y)
    c.arc(0.79, 0.3, 2.2, 1.0); c.arc(0.79, 3.4, 5.0, 1.0)
    for i in range(24):
        c.glyph(0.842, math.tau * (i + 0.5) / 24 + 0.05, 0.035, rng, 1.0)
    return c.mask()


def outer_gothic(rng):
    c = Cv(512)
    c.ring(0.995, 4.0); c.ring(0.945, 1.6); c.ring(0.755, 4.0); c.ring(0.725, 1.2)
    n = 12
    for i in range(n):
        a = math.tau * i / n
        ca, sa = math.cos(a), math.sin(a)
        # a pointed lancet arch standing on the inner ring
        def P(r, t):
            return (ca * r - sa * t, sa * r + ca * t)
        pts = [P(0.77, -0.085), P(0.83, -0.085), P(0.89, -0.04), P(0.935, 0.0), P(0.89, 0.04), P(0.83, 0.085), P(0.77, 0.085)]
        c.poly(pts, 2.0, close=False)
        c.line([P(0.80, 0.0), P(0.86, 0.0)], 1.6)
        c.ring(0.018, 1.4, cx=ca * 0.845, cy=sa * 0.845)
        # spikes between the arches on the rim
        b = a + math.tau / (2 * n)
        cb, sb = math.cos(b), math.sin(b)
        c.poly([(0.95 * cb - 0.025 * sb, 0.95 * sb + 0.025 * cb), (0.995 * cb, 0.995 * sb), (0.95 * cb + 0.025 * sb, 0.95 * sb - 0.025 * cb)], 1.8, close=False)
        c.disc(0.016, cx=0.745 * cb, cy=0.745 * sb)
    return c.mask()


def outer_anime(rng):
    c = Cv(512)
    c.ring(0.995, 3.0); c.ring(0.955, 1.3); c.ring(0.765, 3.0); c.ring(0.735, 1.3)
    n = 8
    for i in range(n):
        a = math.tau * i / n
        x, y = 0.865 * math.cos(a), 0.865 * math.sin(a)
        c.ring(0.082, 3.0, cx=x, cy=y)
        c.ring(0.06, 1.2, cx=x, cy=y)
        k = i % 4
        if k == 0:   # star
            for j in range(5):
                b = a + math.pi + math.tau * j / 5
                b2 = a + math.pi + math.tau * ((j + 2) % 5) / 5
                c.seg(x + 0.045 * math.cos(b), y + 0.045 * math.sin(b), x + 0.045 * math.cos(b2), y + 0.045 * math.sin(b2), 1.8)
        elif k == 1:   # crescent
            c.arc(0.036, 0.7, 5.6, 2.4, cx=x, cy=y)
            c.arc(0.022, 1.0, 5.3, 1.6, cx=x + 0.01, cy=y)
        elif k == 2:   # triangle sigil
            c.ngon(3, 0.045, a, 2.0, cx=x, cy=y); c.disc(0.01, cx=x, cy=y)
        else:          # spiral
            c.line([(x + (0.005 + 0.007 * t) * math.cos(t * 1.1), y + (0.005 + 0.007 * t) * math.sin(t * 1.1)) for t in range(0, 7)], 2.0)
            c.arc(0.045, 0.2, 5.9, 1.4, cx=x, cy=y)
        # script between the medallions: dashes and dots on the ring
        for j in range(1, 6):
            b = a + math.tau / n * j / 6
            if j % 2 == 1:
                c.arc(0.865, b - 0.02, b + 0.02, 2.4)
            else:
                c.disc(0.008, cx=0.865 * math.cos(b), cy=0.865 * math.sin(b))
    for i in range(48):
        c.glyph(0.945, math.tau * i / 48, 0.022, rng, 1.0)
    return c.mask()


# ============================================================================================================ shapes (r <= 1 tile)
def shape_hex(rng):
    c = Cv(512)
    c.ring(0.99, 2.4); c.ring(0.95, 1.0)
    c.ngon(3, 0.9, -math.pi / 2, 3.4); c.ngon(3, 0.9, math.pi / 2, 3.4)
    c.ngon(3, 0.9 * 0.5, -math.pi / 2, 1.2); c.ngon(3, 0.9 * 0.5, math.pi / 2, 1.2)
    c.ngon(6, 0.45, 0.0, 1.8)
    c.ring(0.43, 1.2); c.ring(0.25, 2.2); c.disc(0.035)
    for i in range(6):
        a = -math.pi / 2 + math.tau * i / 6
        c.ring(0.052, 2.4, cx=0.9 * math.cos(a), cy=0.9 * math.sin(a)); c.disc(0.018, cx=0.9 * math.cos(a), cy=0.9 * math.sin(a))
        c.glyph(0.69, a + math.tau / 12, 0.1, rng, 1.6)
    return c.mask()


def shape_rings(rng):
    c = Cv(512)
    for r, w in ((0.99, 2.6), (0.89, 1.2), (0.72, 3.2), (0.56, 1.4), (0.40, 2.4), (0.24, 1.2), (0.1, 2.0)):
        c.ring(r, w)
    for i in range(16):
        a = math.tau * i / 16
        c.seg(0.89 * math.cos(a), 0.89 * math.sin(a), 0.99 * math.cos(a), 0.99 * math.sin(a), 1.2)
    for i in range(10):
        a0 = math.tau * i / 10
        c.arc(0.81, a0, a0 + 0.42, 4.0)
    for i in range(6):
        a0 = math.tau * i / 6 + 0.2
        c.arc(0.48, a0, a0 + 0.62, 2.8)
    c.dots(0.64, 12, 0.012)
    for i in range(8):
        c.glyph(0.32, math.tau * i / 8, 0.09, rng, 1.4)
    c.seg(-0.1, 0, 0.1, 0, 1.2); c.seg(0, -0.1, 0, 0.1, 1.2)
    return c.mask()


def shape_star(rng):
    c = Cv(512)
    c.ring(0.99, 2.4); c.ring(0.86, 1.2)
    c.star(5, 2, 0.86, -math.pi / 2, 3.6)
    c.ngon(5, 0.86 * 0.382, math.pi / 2 - 0.0, 1.4)
    c.star(5, 2, 0.31, math.pi / 2, 2.0)
    c.ring(0.2, 1.6); c.disc(0.035)
    for i in range(5):
        a = -math.pi / 2 + math.tau * i / 5
        c.ring(0.055, 2.4, cx=0.86 * math.cos(a), cy=0.86 * math.sin(a)); c.disc(0.017, cx=0.86 * math.cos(a), cy=0.86 * math.sin(a))
        b = a + math.tau / 10
        c.glyph(0.93, b, 0.07, rng, 1.2)
        c.glyph(0.55, b, 0.08, rng, 1.4)
    return c.mask()


# ============================================================================================================ inner ornaments
def inner_fantasy(rng):
    c = Cv(256)
    c.ring(0.98, 1.6); c.ring(0.62, 1.2)
    c.ngon(4, 0.9, 0.0, 2.0); c.ngon(4, 0.9, math.pi / 4, 2.0)
    c.ring(0.3, 1.8); c.disc(0.08)
    for i in range(8):
        a = math.tau * i / 8
        c.seg(0.3 * math.cos(a), 0.3 * math.sin(a), 0.62 * math.cos(a), 0.62 * math.sin(a), 1.0)
        c.disc(0.035, cx=0.98 * math.cos(a + math.tau / 16) * 0.0 + 0.62 * math.cos(a + math.tau / 16), cy=0.62 * math.sin(a + math.tau / 16))
    return c.mask()


def inner_scifi(rng):
    c = Cv(256)
    c.ring(0.98, 1.2)
    for k in range(3):
        rot = math.pi * k / 3
        pts = [(0.92 * math.cos(t) * math.cos(rot) - 0.38 * math.sin(t) * math.sin(rot),
                0.92 * math.cos(t) * math.sin(rot) + 0.38 * math.sin(t) * math.cos(rot)) for t in [math.tau * i / 64 for i in range(65)]]
        c.line(pts, 1.4)
    c.ring(0.35, 1.0); c.disc(0.1)
    for k, a in enumerate((0.5, 2.6, 4.4)):
        rot = math.pi * k / 3
        x, y = 0.92 * math.cos(a), 0.38 * math.sin(a)
        c.disc(0.05, cx=x * math.cos(rot) - y * math.sin(rot), cy=x * math.sin(rot) + y * math.cos(rot))
    return c.mask()


def inner_gothic(rng):
    c = Cv(256)
    c.ring(0.98, 2.4); c.ring(0.44, 1.8); c.disc(0.06)
    n = 8
    for i in range(n):
        a = math.tau * i / n
        ca, sa = math.cos(a), math.sin(a)
        def P(r, t):
            return (ca * r - sa * t, sa * r + ca * t)
        c.poly([P(0.44, -0.2), P(0.7, -0.16), P(0.97, 0.0), P(0.7, 0.16), P(0.44, 0.2)], 1.8, close=False)
        c.seg(*P(0.5, 0), *P(0.9, 0), 1.0)
    return c.mask()


def inner_anime(rng):
    c = Cv(256)
    c.ring(0.98, 1.6)
    for i in range(6):
        a = math.tau * i / 6
        c.ring(0.36, 1.5, cx=0.5 * math.cos(a), cy=0.5 * math.sin(a))
    c.ring(0.36, 1.5)
    c.star(8, 3, 0.3, 0.0, 1.4)
    c.disc(0.06)
    return c.mask()


# ============================================================================================================ soft parts
def disc_soft():
    m = radial(256, lambda r: np.clip(1.0 - r, 0, 1) ** 1.3 * 0.55 + np.clip(1 - np.abs(r - 0.96) * 18, 0, 1) * 0.5)
    m[radial(256, lambda r: r) > 1.0] = 0
    return m


def orb():
    def f(r):
        core = np.exp(-(r / 0.13) ** 2)
        halo = np.exp(-(r / 0.4) ** 2) * 0.55
        return np.clip(core + halo, 0, 1) * np.clip((1 - r) * 4, 0, 1)
    return radial(128, f)


def trail():
    n_u, n_v = 128, 32
    uu = np.linspace(0, 1, n_u)[None, :]
    vv = np.linspace(-1, 1, n_v)[:, None]
    a = (uu ** 1.6) * np.exp(-(vv / (0.12 + 0.55 * uu)) ** 2 * 1.0)
    return np.clip(a, 0, 1)


def flare():
    def f(r):
        return np.exp(-(r / 0.09) ** 2) + 0.35 * np.exp(-(r / 0.3) ** 2)
    n = 128
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x = (xx - n / 2 + 0.5) / (n / 2)
    y = (yy - n / 2 + 0.5) / (n / 2)
    r = np.sqrt(x * x + y * y)
    spike = np.exp(-(x / 0.03) ** 2) * np.clip(1 - np.abs(y), 0, 1) ** 1.5 + np.exp(-(y / 0.03) ** 2) * np.clip(1 - np.abs(x), 0, 1) ** 1.5
    d1 = (x + y) / 1.4142
    d2 = (x - y) / 1.4142
    diag = (np.exp(-(d1 / 0.02) ** 2) * np.clip(1 - np.abs(d2) * 1.6, 0, 1) ** 2 + np.exp(-(d2 / 0.02) ** 2) * np.clip(1 - np.abs(d1) * 1.6, 0, 1) ** 2) * 0.5
    return np.clip(f(r) + spike + diag, 0, 1) * np.clip((1 - r) * 3, 0, 1)


# ============================================================================================================ cube parts
def cube_pane():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x = (xx + 0.5) / n * 2 - 1
    y = (yy + 0.5) / n * 2 - 1
    edge = np.maximum(np.abs(x), np.abs(y))
    rim = np.clip((edge - 0.78) / 0.22, 0, 1) ** 2.2                     # bright frosted rim
    sheen = np.clip(1 - np.abs((x + y) * 0.5 - 0.15) * 3.2, 0, 1) ** 2 * 0.28 + np.clip(1 - np.abs((x + y) * 0.5 + 0.55) * 9, 0, 1) ** 2 * 0.22
    gl = np.zeros_like(x)
    for k in (-0.5, 0.0, 0.5):
        gl = np.maximum(gl, np.clip(1 - np.abs(x - k) * 70, 0, 1) * 0.16)
        gl = np.maximum(gl, np.clip(1 - np.abs(y - k) * 70, 0, 1) * 0.16)
    a = np.clip(0.32 + rim * 0.68 + sheen + gl, 0, 1)
    a[edge > 1.0] = 0
    r = np.clip(0.55 + 0.45 * rim + sheen * 0.4, 0, 1)
    g = np.clip(0.88 + 0.12 * rim, 0, 1)
    b = np.ones_like(x)
    out = np.dstack([r, g, b, a])
    Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, "seal_cube_pane.png"))


def cube_edge():
    n = 64
    v = np.linspace(-1, 1, n)[:, None] * np.ones((1, n))
    a = np.exp(-(v / 0.07) ** 2) + 0.5 * np.exp(-(v / 0.3) ** 2) + 0.15 * np.exp(-(v / 0.8) ** 2)
    a = np.clip(a, 0, 1) * np.clip((1 - np.abs(v)) * 4, 0, 1)
    return a


def cube_line():
    n_u, n_v = 128, 16
    uu = np.linspace(0, 1, n_u)[None, :]
    vv = np.linspace(-1, 1, n_v)[:, None]
    taper = np.clip(uu * 6, 0, 1) * np.clip((1 - uu) * 2.5, 0, 1)
    head = np.clip(uu, 0, 1) ** 0.6
    a = np.exp(-(vv / (0.28 * (0.2 + 0.8 * taper))) ** 2) * head * (0.3 + 0.7 * taper)
    return np.clip(a, 0, 1)


def cube_cross(rng):
    c = Cv(256)
    c.ring(0.98, 1.8); c.ring(0.9, 0.8); c.ring(0.42, 1.2)
    c.seg(-0.78, 0, 0.78, 0, 2.6); c.seg(0, -0.78, 0, 0.78, 2.6)
    for i in range(4):
        a = math.tau * i / 4
        x, y = 0.8 * math.cos(a), 0.8 * math.sin(a)
        c.poly([(x + 0.1 * math.cos(a), y + 0.1 * math.sin(a)), (x + 0.07 * math.cos(a + 1.57), y + 0.07 * math.sin(a + 1.57)),
                (x - 0.07 * math.cos(a), y - 0.07 * math.sin(a)), (x + 0.07 * math.cos(a - 1.57), y + 0.07 * math.sin(a - 1.57))], 1.6)
    for i in range(12):
        a = math.tau * i / 12
        if i % 3:
            c.glyph(0.67, a, 0.1, rng, 1.4)
    c.ngon(4, 0.3, math.pi / 4, 1.4)
    c.disc(0.05)
    return c.mask()


def cracks(stage, rng_seed):
    """The same crack web, grown in three steps (each stage contains the one before): cracks of seed rng_seed, branching from the centre."""
    n = 256
    c = Cv(n)
    rng = random.Random(rng_seed)
    # grow once with the full length, keep the segments in order of growth, draw the first part for each stage
    segs = []
    arms = 7
    for k in range(arms):
        a = math.tau * k / arms + rng.uniform(-0.25, 0.25)
        x, y, ang = rng.uniform(-0.05, 0.05), rng.uniform(-0.05, 0.05), a
        for step in range(11):
            ang += rng.uniform(-0.55, 0.55)
            ln = rng.uniform(0.08, 0.14)
            nx, ny = x + ln * math.cos(ang), y + ln * math.sin(ang)
            segs.append((step, x, y, nx, ny, 3.0 * (1 - step / 13)))
            if step in (3, 6, 8) and rng.random() < 0.9:
                b = ang + rng.choice((-1, 1)) * rng.uniform(0.6, 1.2)
                bx, by = nx, ny
                for s2 in range(4):
                    b += rng.uniform(-0.45, 0.45)
                    l2 = rng.uniform(0.05, 0.09)
                    segs.append((step + s2 + 1, bx, by, bx + l2 * math.cos(b), by + l2 * math.sin(b), 1.8))
                    bx, by = bx + l2 * math.cos(b), by + l2 * math.sin(b)
            x, y = nx, ny
    limit = (4, 8, 99)[stage - 1]
    for (st, x0, y0, x1, y1, w) in segs:
        if st < limit:
            c.seg(x0, y0, x1, y1, max(1.0, w * 0.8))
    m = c.mask()
    # keep the edges of the tile clean
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    edge = np.maximum(np.abs(xx / n * 2 - 1), np.abs(yy / n * 2 - 1))
    m *= np.clip((1 - edge) * 6, 0, 1)
    return m


# ============================================================================================================ main
def save_mask(m, name, **kw):
    finish(m, name, **kw)


def main():
    os.makedirs(OUT, exist_ok=True)
    rng = random.Random(5521)
    designs = [("fantasy", outer_fantasy, inner_fantasy), ("scifi", outer_scifi, inner_scifi),
               ("gothic", outer_gothic, inner_gothic), ("anime", outer_anime, inner_anime)]
    for name, fo, fi in designs:
        save_mask(fo(random.Random(hash_name(name))), "seal_outer_%s.png" % name, glow=(2.0, 6.0))
        save_mask(fi(random.Random(hash_name(name) + 1)), "seal_inner_%s.png" % name, glow=(1.5, 4.5))
    save_mask(shape_hex(rng), "seal_shape_hexagram.png", glow=(2.0, 6.0))
    save_mask(shape_rings(rng), "seal_shape_rings.png", glow=(2.0, 6.0))
    save_mask(shape_star(rng), "seal_shape_star.png", glow=(2.0, 6.0))
    a = disc_soft()
    Image.fromarray((np.dstack([np.ones((256, 256, 3)), a]) * 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "seal_disc.png"))
    for nm, arr in (("seal_orb.png", orb()), ("seal_trail.png", trail()), ("seal_flare.png", flare()), ("seal_edge.png", cube_edge()), ("seal_line.png", cube_line())):
        h, w = arr.shape
        Image.fromarray((np.dstack([np.ones((h, w, 3)), arr]) * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, nm))
    cube_pane()
    save_mask(cube_cross(random.Random(77)), "seal_cube_cross.png", glow=(1.6, 5.0))
    for st in (1, 2, 3):
        save_mask(cracks(st, 31337), "seal_cube_crack_%d.png" % st, glow=(1.2, 3.5), gain=(0.8, 0.5))
    print("wrote", len([f for f in os.listdir(OUT) if f.startswith("seal_")]), "textures to", os.path.normpath(OUT))


def hash_name(s):
    h = 17
    for ch in s:
        h = (h * 31 + ord(ch)) & 0xFFFFFF
    return h


if __name__ == "__main__":
    main()

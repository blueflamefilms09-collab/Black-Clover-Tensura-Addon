"""Textures for the 30 wiki magics (WikiMagicLayer, WikiAura; docs/wiki_magic_vfx.md). Deterministic, no fonts, no external images.

    python tools/gen_wiki_magic_textures.py                 -> src/main/resources/assets/nusmp/textures/particle/wiki_*.png
    python tools/gen_wiki_magic_textures.py --preview OUT   -> also writes a contact sheet (PNG) to OUT

Three families, all greyscale + alpha like the rest of textures/particle (one vertex colour tints a quad):
  wiki_grp_<group>   the bulk sprite of a visual group (7): motes, trails, circles, shards, fluid, spiral, infernal
  wiki_<magic>       the accent sprite of one magic (30), drawn in its group's shape language with its own motif
  wiki_shell_<group> a 64x64 tileable pattern per group (7) that the armour / aura shell (WikiAura) wraps round the player model
The group of each magic is the table in vfx/WikiStyles.java (kept in step by WikiStylesTest). Never touches time_*.png.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm, grey, noise  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4
N = 128
TAU = math.tau


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


# ------------------------------------------------------------------------------------------------ drawing kit (unit coordinates, y down)
class Pen:
    def __init__(self, n=N):
        self.n = n
        self.s = n * SS
        self.im = Image.new("L", (self.s, self.s), 0)
        self.d = ImageDraw.Draw(self.im)

    def p(self, x, y):
        return (x * self.s, y * self.s)

    def w(self, t):
        return max(1, int(round(t * self.s)))

    def line(self, pts, width=0.02, fill=255):
        self.d.line([self.p(*q) for q in pts], fill=fill, width=self.w(width), joint="curve")

    def poly(self, pts, fill=255):
        self.d.polygon([self.p(*q) for q in pts], fill=fill)

    def ellipse(self, cx, cy, rx, ry=None, fill=255, outline=None, width=0.02):
        ry = rx if ry is None else ry
        box = [self.p(cx - rx, cy - ry), self.p(cx + rx, cy + ry)]
        if outline is None:
            self.d.ellipse(box, fill=fill)
        else:
            self.d.ellipse(box, outline=outline, width=self.w(width))

    def arc(self, cx, cy, r, a0, a1, width=0.02, fill=255):
        self.d.arc([self.p(cx - r, cy - r), self.p(cx + r, cy + r)], math.degrees(a0), math.degrees(a1), fill=fill, width=self.w(width))

    def ring(self, cx, cy, r, width=0.02, fill=255):
        self.d.ellipse([self.p(cx - r, cy - r), self.p(cx + r, cy + r)], outline=fill, width=self.w(width))

    def ticks(self, cx, cy, r0, r1, count, width=0.012, every=0, r2=None, phase=0.0):
        for k in range(count):
            a = phase + TAU * k / count
            rr = r2 if (every and r2 and k % every == 0) else r1
            self.line([(cx + math.cos(a) * r0, cy + math.sin(a) * r0), (cx + math.cos(a) * rr, cy + math.sin(a) * rr)], width)

    def array(self):
        return np.asarray(self.im.resize((self.n, self.n), Image.LANCZOS)).astype(np.float32) / 255


def blur(a, radius):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(radius))
    return np.asarray(im).astype(np.float32) / 255


def radial(n=N):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    return np.hypot((xx + 0.5) / n * 2 - 1, (yy + 0.5) / n * 2 - 1)


def finish(a, glow=1.5, window=True, soft=0.55, base=0.62):
    """Mask (0..1) -> RGBA: a soft halo around the strokes, a brighter core, faded to nothing at the border."""
    n = a.shape[0]
    if glow:
        a = np.clip(a + blur(a, glow * n / 128) * soft, 0, 1)
    if window:
        a = a * np.clip((1.0 - radial(n)) * 7.0, 0, 1)
    return grey(a, np.clip(base + (1 - base) * a, 0, 1))


def bez(p0, p1, p2, steps=24):
    out = []
    for i in range(steps + 1):
        t = i / steps
        out.append(((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]))
    return out


def taper(pen, pts, w0, w1, fill=255):
    """A stroke whose width goes from w0 to w1 along the points (drawn as stacked discs and quads)."""
    k = len(pts) - 1
    for i in range(k):
        t0, t1 = i / k, (i + 1) / k
        wa, wb = w0 + (w1 - w0) * t0, w0 + (w1 - w0) * t1
        ax, ay = pts[i]
        bx, by = pts[i + 1]
        dx, dy = bx - ax, by - ay
        l = math.hypot(dx, dy) or 1e-6
        nx, ny = -dy / l, dx / l
        pen.poly([(ax + nx * wa / 2, ay + ny * wa / 2), (bx + nx * wb / 2, by + ny * wb / 2),
                  (bx - nx * wb / 2, by - ny * wb / 2), (ax - nx * wa / 2, ay - ny * wa / 2)], fill)
        pen.ellipse(bx, by, wb / 2, fill=fill)


def rot(pts, a, cx=0.5, cy=0.5):
    c, s = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for x, y in pts]


def prism(pen, cx, base, w, h, tip, a=0.0, fill=255, facet=True):
    """A hexagonal crystal standing on (cx, base): body w x h plus a pointed tip, tilted by a."""
    body = [(cx - w / 2, base), (cx - w / 2, base - h), (cx, base - h - tip), (cx + w / 2, base - h), (cx + w / 2, base)]
    body = rot(body, a, cx, base)
    pen.poly(body, fill)
    if facet:
        pen.line(rot([(cx, base), (cx, base - h - tip)], a, cx, base), 0.008, 120)
        pen.line(rot([(cx - w / 2, base - h), (cx, base - h - tip * 0.1), (cx + w / 2, base - h)], a, cx, base), 0.008, 120)


# ================================================================================================ GROUP BULK SPRITES
def grp_motes():
    """A drifting mote: a soft round core with a curling wisp trailing off one side."""
    p = Pen()
    taper(p, bez((0.5, 0.5), (0.2, 0.52), (0.18, 0.8), 28), 0.16, 0.012)
    p.ellipse(0.5, 0.5, 0.17)
    a = p.array()
    r = radial()
    core = np.exp(-(r / 0.36) ** 2)
    return finish(np.clip(a * 0.8 + core * 0.6, 0, 1), glow=3.0, soft=0.9, base=0.7)


def grp_trails():
    """A slashing trail: a tapered lens-shaped streak running along U (hot at the head on the right, a sheared tail on the left) with a bright core line."""
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    u, v = (xx + 0.5) / N, (yy + 0.5) / N
    across = np.clip(1 - np.abs(v - 0.5) * 2.0 / (0.12 + 0.88 * np.sin(np.clip(u, 0, 1) * math.pi * 0.92) ** 0.8 * 0.5 + 1e-3), 0, 1)
    along = np.clip(u * 1.1, 0, 1) ** 1.1 * np.clip((1.0 - u) * 14, 0, 1)
    core = np.exp(-((v - 0.5) / 0.035) ** 2) * np.clip(u * 1.25, 0, 1)
    speed = (np.exp(-((v - 0.36) / 0.012) ** 2) + np.exp(-((v - 0.64) / 0.012) ** 2)) * np.clip(u * 1.5, 0, 1) * np.clip((0.8 - u) * 6, 0, 1) * 0.5
    a = np.clip(across ** 1.4 * along * 0.9 + core * 0.9 + speed, 0, 1)
    return finish(a, glow=1.2, window=False, base=0.6)


def grp_circles():
    """A rune ring: outer ring, 60 ticks (every fifth long), a glyph band between two rings, an inner ring and a spoked hub."""
    p = Pen(256)
    c = 0.5
    p.ring(c, c, 0.47, 0.012)
    p.ring(c, c, 0.455, 0.004)
    p.ticks(c, c, 0.47, 0.485, 60, 0.005, every=5, r2=0.495)
    p.ring(c, c, 0.37, 0.008)
    p.ring(c, c, 0.30, 0.012)
    for k in range(24):                                       # glyph blocks in the band
        a = TAU * k / 24
        r0, r1 = 0.315, 0.355
        q = [(c + math.cos(a) * r0, c + math.sin(a) * r0), (c + math.cos(a + 0.09) * r1, c + math.sin(a + 0.09) * r1),
             (c + math.cos(a + 0.18) * r0, c + math.sin(a + 0.18) * r0)] if k % 2 else \
            [(c + math.cos(a) * r1, c + math.sin(a) * r1), (c + math.cos(a + 0.09) * r0, c + math.sin(a + 0.09) * r0),
             (c + math.cos(a + 0.18) * r1, c + math.sin(a + 0.18) * r1)]
        p.line(q + [q[0]], 0.006)
    p.ring(c, c, 0.18, 0.008)
    for k in range(6):                                        # hexagram hub
        a = TAU * k / 6
        p.line([(c + math.cos(a) * 0.18, c + math.sin(a) * 0.18), (c + math.cos(a + TAU / 3) * 0.18, c + math.sin(a + TAU / 3) * 0.18)], 0.006)
    p.ellipse(c, c, 0.025)
    return finish(p.array(), glow=1.0, soft=0.7)


def grp_shards():
    """A crystalline starburst: eight radial spikes of different length with a bright facet line and a small crystal core."""
    p = Pen()
    lengths = [0.47, 0.30, 0.42, 0.26, 0.47, 0.30, 0.40, 0.26]
    for k, l in enumerate(lengths):
        a = TAU * k / 8 + 0.08 * (k % 2)
        w = 0.055 if l > 0.35 else 0.04
        tip = (0.5 + math.cos(a) * l, 0.5 + math.sin(a) * l)
        l1 = (0.5 + math.cos(a - w / l * 1.0) * 0.07, 0.5 + math.sin(a - w / l * 1.0) * 0.07)
        r1 = (0.5 + math.cos(a + w / l * 1.0) * 0.07, 0.5 + math.sin(a + w / l * 1.0) * 0.07)
        p.poly([l1, tip, r1])
        p.line([(0.5, 0.5), tip], 0.007, 120)
    p.poly([(0.5, 0.42), (0.57, 0.5), (0.5, 0.58), (0.43, 0.5)])
    return finish(p.array(), glow=1.2, soft=0.6)


def grp_fluid():
    """A droplet with a splash crown: a teardrop above, a ring of splash arcs and small droplets below, a highlight."""
    p = Pen()
    # teardrop: tip at the top, a round belly
    p.d.polygon([p.p(0.5, 0.06), p.p(0.37, 0.28), p.p(0.34, 0.38), p.p(0.40, 0.48), p.p(0.50, 0.52), p.p(0.60, 0.48), p.p(0.66, 0.38), p.p(0.63, 0.28)], fill=255)
    p.arc(0.5, 0.74, 0.30, math.pi * 1.1, math.pi * 1.9, 0.045)
    p.arc(0.5, 0.74, 0.20, math.pi * 1.15, math.pi * 1.85, 0.03)
    for dx, dy, r in [(-0.34, 0.60, 0.035), (0.35, 0.58, 0.04), (-0.22, 0.52, 0.025), (0.22, 0.50, 0.028), (0.0, 0.62, 0.03), (-0.40, 0.72, 0.022), (0.41, 0.70, 0.022)]:
        p.ellipse(0.5 + dx, dy, r)
    a = p.array()
    hi = Pen()
    hi.ellipse(0.44, 0.34, 0.035, 0.07)
    return finish(np.clip(a * 0.85 + hi.array() * 0.5, 0, 1), glow=1.6, base=0.55)


def grp_spiral():
    """A two-arm spiral: logarithmic arms that thin and fade outward, with a bright eye."""
    p = Pen()
    for arm in range(2):
        pts = []
        for t in np.linspace(0.0, 1.0, 60):
            r = 0.04 + 0.44 * t
            a = arm * math.pi + 4.2 * t * math.pi * 0.62
            pts.append((0.5 + math.cos(a) * r, 0.5 + math.sin(a) * r))
        taper(p, pts, 0.07, 0.008)
    p.ellipse(0.5, 0.5, 0.06)
    return finish(p.array(), glow=1.8, soft=0.7)


def grp_infernal():
    """A dark seal: a heavy disc with a {7/2} star cut into it, a toothed outer ring and ember specks (alpha blended dark, embers added)."""
    p = Pen(256)
    c = 0.5
    p.ellipse(c, c, 0.30)
    p.ring(c, c, 0.46, 0.02)
    p.ring(c, c, 0.40, 0.01)
    for k in range(28):                                        # outer teeth like licking flames
        a = TAU * k / 28
        l = 0.052 if k % 2 == 0 else 0.03
        p.poly([(c + math.cos(a - 0.05) * 0.465, c + math.sin(a - 0.05) * 0.465), (c + math.cos(a) * (0.465 + l), c + math.sin(a) * (0.465 + l)),
                (c + math.cos(a + 0.05) * 0.465, c + math.sin(a + 0.05) * 0.465)])
    a = p.array()
    cut = Pen(256)
    star = [(c + math.cos(TAU * ((k * 2) % 7) / 7 - math.pi / 2) * 0.27, c + math.sin(TAU * ((k * 2) % 7) / 7 - math.pi / 2) * 0.27) for k in range(8)]
    cut.line(star, 0.012)
    cut.ring(c, c, 0.27, 0.008)
    cut.ellipse(c, c, 0.05)
    # the cut-out: the star lines are holes in the dark disc
    hole = cut.array()
    rng = np.random.default_rng(666)
    specks = np.zeros_like(a)
    for _ in range(40):
        ang, rad = rng.uniform(0, TAU), rng.uniform(0.30, 0.44)
        x, y = int((c + math.cos(ang) * rad) * 256), int((c + math.sin(ang) * rad) * 256)
        specks[max(0, y - 1):y + 2, max(0, x - 1):x + 2] = 1
    out = np.clip(a * (1 - hole * 0.9) + specks * 0.4, 0, 1)
    img = grey(out * np.clip((1 - radial(256)) * 7, 0, 1), np.clip(0.35 + 0.65 * (1 - np.clip(a, 0, 1) * 0.0), 0, 1))
    return img


# ================================================================================================ PER-MAGIC ACCENT SPRITES
# ---- MOTES
def m_air():
    p = Pen()
    for k, (cx, cy, r, a0) in enumerate([(0.5, 0.5, 0.40, 0.4), (0.5, 0.5, 0.28, 2.6), (0.5, 0.5, 0.16, 4.6)]):
        p.arc(cx, cy, r, a0, a0 + 4.1, 0.035 - 0.006 * k)
        e = a0 + 4.1
        p.ellipse(cx + math.cos(e) * r, cy + math.sin(e) * r, 0.03 - 0.006 * k)
    taper(p, bez((0.5, 0.5), (0.58, 0.58), (0.66, 0.5)), 0.05, 0.01)
    return finish(p.array(), glow=2.2)


def m_snow():
    p = Pen()
    for k in range(6):
        a = TAU * k / 6
        dx, dy = math.cos(a), math.sin(a)
        p.line([(0.5, 0.5), (0.5 + dx * 0.42, 0.5 + dy * 0.42)], 0.03)
        for t, l in [(0.55, 0.14), (0.78, 0.09)]:
            bx, by = 0.5 + dx * 0.42 * t, 0.5 + dy * 0.42 * t
            for sgn in (-1, 1):
                aa = a + sgn * 0.9
                p.line([(bx, by), (bx + math.cos(aa) * l, by + math.sin(aa) * l)], 0.02)
    p.ellipse(0.5, 0.5, 0.045)
    return finish(p.array(), glow=1.6)


def m_smoke():
    n = fbm(N, N, 24, 160)
    n2 = fbm(N, N, 12, 161)
    r = radial()
    blob = np.clip(1.15 - r * 1.2, 0, 1) ** 1.2
    a = np.clip(blob * (0.35 + 0.9 * n) * (0.7 + 0.5 * n2), 0, 1)
    p = Pen()
    taper(p, bez((0.5, 0.62), (0.75, 0.45), (0.55, 0.2)), 0.14, 0.01)
    a = np.clip(a * 0.9 + blur(p.array(), 2.5) * 0.7, 0, 1)
    return finish(a, glow=2.5, soft=0.6, base=0.5)


# ---- TRAILS
def m_hair():
    p = Pen()
    for k in range(7):
        o = (k - 3) * 0.045
        taper(p, bez((0.04, 0.5 + o), (0.45, 0.5 + o * 2.2 - 0.08 * math.sin(k)), (0.96, 0.5 + o * 0.6 + 0.14 * math.cos(k * 1.4))), 0.026, 0.004)
    return finish(p.array(), glow=1.3, window=False)


def m_poison_plant():
    p = Pen()
    stem = bez((0.04, 0.62), (0.45, 0.2), (0.96, 0.52), 30)
    taper(p, stem, 0.04, 0.012)
    for t, side, l in [(0.2, -1, 0.11), (0.38, 1, 0.12), (0.56, -1, 0.1), (0.74, 1, 0.09)]:   # thorns
        i = int(t * (len(stem) - 1))
        x, y = stem[i]
        nx, ny = (stem[i + 1][0] - x), (stem[i + 1][1] - y)
        l2 = math.hypot(nx, ny) or 1e-6
        nx, ny = -ny / l2, nx / l2
        p.poly([(x - nx * side * 0.0 - ny * 0.0 + (stem[i + 1][0] - x) * 0.5, y + (stem[i + 1][1] - y) * 0.5),
                (x + nx * side * l, y + ny * side * l), (x - (stem[i + 1][0] - x) * 0.5, y - (stem[i + 1][1] - y) * 0.5)])
    p.ellipse(0.9, 0.5, 0.035)                                                         # a poison bead at the head
    return finish(p.array(), glow=1.4, window=False)


def m_vine():
    p = Pen()
    pts = []
    for t in np.linspace(0, 1, 70):
        pts.append((0.06 + 0.88 * t, 0.5 + 0.2 * math.sin(t * TAU * 1.5) * (1 - t * 0.3)))
    taper(p, pts, 0.034, 0.01)
    curl = [(0.94 + math.cos(a) * 0.04 * (1 - a / 14), 0.5 + math.sin(a) * 0.04 * (1 - a / 14)) for a in np.linspace(0, 12, 40)]
    taper(p, curl, 0.012, 0.006)
    for i, t in enumerate([0.2, 0.42, 0.64, 0.82]):                                      # leaves
        x, y = 0.06 + 0.88 * t, 0.5 + 0.2 * math.sin(t * TAU * 1.5) * (1 - t * 0.3)
        sgn = 1 if i % 2 else -1
        p.poly([(x, y), (x + 0.04, y + sgn * 0.05), (x + 0.08, y + sgn * 0.13), (x + 0.0, y + sgn * 0.09)])
    return finish(p.array(), glow=1.3, window=False)


def m_wing():
    p = Pen()
    for k in range(6):
        t = k / 5
        tip = (0.96 - 0.12 * t, 0.12 + 0.62 * t)
        taper(p, bez((0.06, 0.55), (0.4, 0.2 + 0.5 * t - 0.12), tip), 0.07 - 0.012 * t, 0.006)
        p.line(bez((0.06, 0.55), (0.4, 0.2 + 0.5 * t - 0.12), tip), 0.006, 110)
    p.ellipse(0.06, 0.55, 0.045)
    return finish(p.array(), glow=1.4, window=False)


# ---- CIRCLES (per-magic ring: the group ring with its own glyph, 128 px)
def _ring_base(p, ticks=36, every=6):
    p.ring(0.5, 0.5, 0.46, 0.016)
    p.ring(0.5, 0.5, 0.36, 0.008)
    p.ticks(0.5, 0.5, 0.46, 0.485, ticks, 0.008, every=every, r2=0.5)


def m_memory():
    p = Pen()
    _ring_base(p, 36, 6)
    p.poly([(0.2, 0.5)] + [(0.2 + 0.6 * t, 0.5 - 0.17 * math.sin(t * math.pi)) for t in np.linspace(0, 1, 16)] + [(0.8, 0.5)] +
           [(0.8 - 0.6 * t, 0.5 + 0.17 * math.sin(t * math.pi)) for t in np.linspace(0, 1, 16)])
    iris = Pen()
    iris.ellipse(0.5, 0.5, 0.1)
    cut = iris.array()
    a = np.clip(p.array() - cut * 0.85, 0, 1)
    q = Pen()
    q.ellipse(0.5, 0.5, 0.045)
    for k in range(10):
        q.ellipse(0.5 + math.cos(TAU * k / 10) * 0.30, 0.5 + math.sin(TAU * k / 10) * 0.30, 0.012)
    return finish(np.clip(a + q.array(), 0, 1), glow=1.0)


def m_modification():
    p = Pen()
    p.ring(0.5, 0.5, 0.3, 0.02)
    for k in range(12):
        a = TAU * k / 12
        p.poly(rot([(0.5 - 0.045, 0.5 - 0.30), (0.5 - 0.03, 0.5 - 0.40), (0.5 + 0.03, 0.5 - 0.40), (0.5 + 0.045, 0.5 - 0.30)], a))
    p.ring(0.5, 0.5, 0.44, 0.008)
    hexa = [(0.5 + math.cos(TAU * k / 6 + 0.5) * 0.17, 0.5 + math.sin(TAU * k / 6 + 0.5) * 0.17) for k in range(7)]
    p.line(hexa, 0.014)
    for k in range(3):                                                                    # arrows changing the form
        a = TAU * k / 3 + 0.5
        x, y = 0.5 + math.cos(a) * 0.11, 0.5 + math.sin(a) * 0.11
        p.poly(rot([(x, y - 0.05), (x + 0.035, y + 0.02), (x - 0.035, y + 0.02)], a + math.pi / 2, x, y))
    return finish(p.array(), glow=1.0)


def m_permeation():
    p = Pen()
    p.ring(0.5, 0.5, 0.46, 0.014)
    for k, r in enumerate([0.38, 0.30, 0.22]):                                            # a dashed ring slipping through solid rings
        for j in range(12 if k == 1 else 1):
            if k == 1:
                p.arc(0.5, 0.5, r, TAU * j / 12, TAU * j / 12 + 0.32, 0.02)
            else:
                p.ring(0.5, 0.5, r, 0.014)
    p.ring(0.34, 0.5, 0.16, 0.012)
    p.ring(0.66, 0.5, 0.16, 0.012)
    p.ellipse(0.5, 0.5, 0.03)
    return finish(p.array(), glow=1.0)


def m_soul():
    p = Pen()
    _ring_base(p, 24, 6)
    taper(p, bez((0.5, 0.74), (0.34, 0.5), (0.5, 0.2)), 0.2, 0.01)
    taper(p, bez((0.5, 0.74), (0.66, 0.5), (0.5, 0.2)), 0.2, 0.01)
    a = p.array()
    eye = Pen()
    eye.ellipse(0.46, 0.56, 0.025)
    eye.ellipse(0.54, 0.56, 0.025)
    return finish(np.clip(a - eye.array() * 0.9, 0, 1), glow=1.2)


def m_sound():
    p = Pen()
    _ring_base(p, 48, 4)
    p.ellipse(0.5, 0.5, 0.035)
    for r in (0.09, 0.15, 0.21, 0.27):
        p.arc(0.5, 0.5, r, -0.9, 0.9, 0.018)
        p.arc(0.5, 0.5, r, math.pi - 0.9, math.pi + 0.9, 0.018)
    return finish(p.array(), glow=1.0)


def m_switching():
    p = Pen()
    _ring_base(p, 30, 5)
    for k in range(2):                                                                    # two arrows chasing round the ring
        a0 = k * math.pi
        p.arc(0.5, 0.5, 0.25, a0 + 0.3, a0 + 2.5, 0.03)
        e = a0 + 2.5
        ex, ey = 0.5 + math.cos(e) * 0.25, 0.5 + math.sin(e) * 0.25
        p.poly(rot([(ex, ey - 0.06), (ex + 0.05, ey + 0.03), (ex - 0.05, ey + 0.03)], e + math.pi, ex, ey))
    p.line([(0.42, 0.42), (0.58, 0.58)], 0.014)
    p.line([(0.58, 0.42), (0.42, 0.58)], 0.014)
    return finish(p.array(), glow=1.0)


def m_tree():
    p = Pen()
    _ring_base(p, 28, 7)
    rng = np.random.default_rng(25)
    for r in (0.05, 0.1, 0.15, 0.2, 0.25, 0.3):                                           # growth rings, slightly wobbling
        pts = [(0.5 + math.cos(a) * (r + 0.008 * math.sin(a * 3 + r * 40)), 0.5 + math.sin(a) * (r + 0.008 * math.sin(a * 3 + r * 40))) for a in np.linspace(0, TAU, 40)]
        p.line(pts, 0.011)
    for k in range(8):                                                                    # leaves outside the inner ring
        a = TAU * k / 8 + 0.2
        x, y = 0.5 + math.cos(a) * 0.4, 0.5 + math.sin(a) * 0.4
        p.poly(rot([(x, y - 0.045), (x + 0.025, y), (x, y + 0.045), (x - 0.025, y)], a + math.pi / 2, x, y))
    return finish(p.array(), glow=1.0)


# ---- SHARDS
def m_mineral():
    p = Pen()
    for cx, base, w, h, tip, a in [(0.5, 0.9, 0.16, 0.42, 0.16, 0.0), (0.33, 0.92, 0.12, 0.28, 0.12, -0.35), (0.67, 0.92, 0.12, 0.3, 0.12, 0.3),
                                   (0.2, 0.94, 0.09, 0.15, 0.09, -0.7), (0.8, 0.94, 0.09, 0.17, 0.09, 0.65)]:
        prism(p, cx, base, w, h, tip, a)
    return finish(p.array(), glow=1.0)


def m_nail():
    p = Pen()
    for dx, dy, l, a in [(0.0, 0.0, 0.78, -0.62), (-0.2, 0.18, 0.46, -0.45)]:
        cx, cy = 0.5 + dx, 0.5 + dy
        pts = rot([(cx - 0.012, cy - l / 2 + 0.05), (cx + 0.012, cy - l / 2 + 0.05), (cx + 0.02, cy + l / 2 - 0.12), (cx, cy + l / 2), (cx - 0.02, cy + l / 2 - 0.12)], a, cx, cy)
        p.poly(pts)
        head = rot([(cx - 0.065, cy - l / 2 - 0.0), (cx + 0.065, cy - l / 2), (cx + 0.05, cy - l / 2 + 0.04), (cx - 0.05, cy - l / 2 + 0.04)], a, cx, cy)
        p.poly(head)
    return finish(p.array(), glow=1.0)


def m_red_ochre():
    rng = np.random.default_rng(10)
    p = Pen()
    for _ in range(11):
        cx, cy = rng.uniform(0.18, 0.82), rng.uniform(0.18, 0.82)
        r = rng.uniform(0.05, 0.13)
        k = rng.integers(4, 7)
        ang = np.sort(rng.uniform(0, TAU, k))
        p.poly([(cx + math.cos(a) * r * rng.uniform(0.5, 1.2), cy + math.sin(a) * r * rng.uniform(0.5, 1.2)) for a in ang])
    dust = (rng.random((N, N)) > 0.93).astype(np.float32) * np.clip(1.1 - radial(), 0, 1)
    return finish(np.clip(p.array() + blur(dust, 0.7) * 0.8, 0, 1), glow=0.8)


def m_rock():
    p = Pen()
    pts = [(0.5, 0.1), (0.76, 0.2), (0.9, 0.5), (0.78, 0.82), (0.5, 0.92), (0.2, 0.8), (0.1, 0.46), (0.26, 0.2)]
    p.poly(pts)
    for a, b in [((0.5, 0.1), (0.46, 0.5)), ((0.46, 0.5), (0.9, 0.5)), ((0.46, 0.5), (0.2, 0.8)), ((0.46, 0.5), (0.5, 0.92)), ((0.1, 0.46), (0.46, 0.5)), ((0.26, 0.2), (0.46, 0.5))]:
        p.line([a, b], 0.012, 0)
    a = p.array()
    facets = Pen()
    facets.poly([(0.5, 0.1), (0.76, 0.2), (0.9, 0.5), (0.46, 0.5)], 255)
    return finish(np.clip(a * 0.7 + blur(facets.array(), 1.0) * a * 0.5, 0, 1), glow=0.9, base=0.5)


def m_sandstone():
    p = Pen()
    pts = [(0.14, 0.34), (0.5, 0.12), (0.88, 0.3), (0.9, 0.7), (0.5, 0.9), (0.12, 0.7)]
    p.poly(pts)
    a = p.array()
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    strata = 0.55 + 0.45 * np.sign(np.sin(yy / N * TAU * 3.0 + 0.4 * np.sin(xx / N * TAU * 2)))
    return finish(a * (0.55 + 0.45 * strata), glow=0.8, base=0.5)


def m_scale():
    p = Pen()
    rows = 5
    for r in range(rows):
        cols = 5 if r % 2 == 0 else 4
        for c in range(cols):
            cx = 0.5 + (c - (cols - 1) / 2) * 0.19
            cy = 0.2 + r * 0.15
            p.d.pieslice([p.p(cx - 0.1, cy - 0.1), p.p(cx + 0.1, cy + 0.1)], 0, 180, fill=255)
            p.d.arc([p.p(cx - 0.1, cy - 0.1), p.p(cx + 0.1, cy + 0.1)], 0, 180, fill=0, width=p.w(0.012))
    return finish(p.array(), glow=0.8)


def m_shakudo():
    p = Pen()
    p.poly([(0.16, 0.7), (0.3, 0.34), (0.74, 0.34), (0.86, 0.7)])
    p.poly([(0.22, 0.78), (0.78, 0.78), (0.86, 0.7), (0.16, 0.7)])
    for x in (0.34, 0.5, 0.66):
        p.line([(x, 0.4), (x - 0.06, 0.66)], 0.01, 0)
    star = Pen()
    star.line([(0.5, 0.04), (0.5, 0.3)], 0.018)
    star.line([(0.36, 0.17), (0.64, 0.17)], 0.018)
    return finish(np.clip(p.array() * 0.85 + star.array(), 0, 1), glow=1.0, base=0.5)


def m_spike():
    p = Pen()
    for k in range(9):
        a = -math.pi / 2 + (k - 4) * 0.27
        l = 0.46 - abs(k - 4) * 0.04
        base = (0.5, 0.92)
        tip = (0.5 + math.cos(a) * l * 1.0, 0.92 + math.sin(a) * l * 1.9)
        w = 0.05 - abs(k - 4) * 0.004
        p.poly([(base[0] - w, base[1]), tip, (base[0] + w, base[1])])
        p.line([base, tip], 0.006, 120)
    return finish(p.array(), glow=0.9)


def m_stone():
    p = Pen()
    p.poly([(0.12, 0.28), (0.82, 0.14), (0.9, 0.7), (0.2, 0.86)])
    p.line([(0.5, 0.2), (0.44, 0.44), (0.56, 0.58), (0.5, 0.82)], 0.016, 0)
    p.line([(0.44, 0.44), (0.28, 0.5)], 0.012, 0)
    p.line([(0.56, 0.58), (0.74, 0.5)], 0.012, 0)
    return finish(p.array(), glow=0.9, base=0.5)


# ---- FLUID
def m_mucus():
    p = Pen()
    p.ellipse(0.5, 0.36, 0.26, 0.2)
    for x, l in [(0.34, 0.34), (0.5, 0.5), (0.64, 0.28)]:                                 # dangling strands with a bead
        taper(p, [(x, 0.46), (x + 0.015, 0.46 + l * 0.6), (x, 0.46 + l)], 0.04, 0.012)
        p.ellipse(x, 0.46 + l, 0.028)
    for dx, dy, r in [(-0.12, -0.04, 0.035), (0.08, -0.1, 0.025), (0.16, 0.0, 0.03)]:
        p.ellipse(0.5 + dx, 0.36 + dy, r, fill=0)
    return finish(p.array(), glow=1.4, base=0.55)


def m_mud():
    rng = np.random.default_rng(6)
    p = Pen()
    p.ellipse(0.5, 0.5, 0.2)
    for k in range(12):
        a = TAU * k / 12 + rng.uniform(-0.1, 0.1)
        l = rng.uniform(0.22, 0.4)
        taper(p, [(0.5 + math.cos(a) * 0.15, 0.5 + math.sin(a) * 0.15), (0.5 + math.cos(a) * l, 0.5 + math.sin(a) * l)], 0.07, 0.015)
        p.ellipse(0.5 + math.cos(a) * (l + 0.05), 0.5 + math.sin(a) * (l + 0.05), rng.uniform(0.018, 0.04))
    return finish(p.array(), glow=1.4, base=0.45)


def m_tongue():
    p = Pen()
    pts = bez((0.08, 0.82), (0.5, 0.98), (0.8, 0.42), 40)
    taper(p, pts, 0.17, 0.2)
    tip = pts[-1]
    p.ellipse(tip[0], tip[1], 0.1)
    a = p.array()
    g = Pen()
    g.line(bez((0.14, 0.8), (0.5, 0.93), (0.78, 0.44), 36), 0.012)
    for dx, dy, r in [(0.9, 0.55, 0.03), (0.86, 0.68, 0.022), (0.2, 0.52, 0.025)]:
        g.ellipse(dx, dy, r)
    drops = g.array()
    return finish(np.clip(a - drops * 0.0 - blur(g.array(), 0.5) * a * 0.7 * (drops > 0.5), 0, 1), glow=1.3, base=0.55)


# ---- SPIRAL
def m_skin():
    p = Pen()
    for arm in range(2):
        pts = []
        for t in np.linspace(0.0, 1.0, 60):
            r = 0.06 + 0.4 * t
            a = arm * math.pi + 3.0 * t * math.pi
            pts.append((0.5 + math.cos(a) * r, 0.5 + math.sin(a) * r))
        taper(p, pts, 0.09, 0.045)
        for i in range(6, 60, 6):                                                         # stitches across the band
            x, y = pts[i]
            nx, ny = pts[i + 1][0] - x, pts[i + 1][1] - y
            l = math.hypot(nx, ny) or 1e-6
            p.line([(x - ny / l * 0.03, y + nx / l * 0.03), (x + ny / l * 0.03, y - nx / l * 0.03)], 0.01, 0)
    return finish(p.array(), glow=1.3, base=0.55)


def m_song():
    p = Pen()
    for k in range(3):                                                                    # a curved staff
        pts = [(0.08 + 0.84 * t, 0.62 - 0.0 + k * 0.045 - 0.22 * math.sin(t * math.pi * 0.9)) for t in np.linspace(0, 1, 30)]
        p.line(pts, 0.008)
    for x, y, flip in [(0.32, 0.5, 0), (0.62, 0.38, 1)]:                                  # two eighth notes
        p.ellipse(x, y + 0.1, 0.05, 0.036)
        p.line([(x + 0.045, y + 0.1), (x + 0.045, y - 0.12)], 0.014)
        p.line([(x + 0.045, y - 0.12), (x + 0.12, y - 0.04)], 0.016)
    return finish(p.array(), glow=1.2, window=False)


def m_vortex():
    p = Pen()
    for arm in range(4):
        pts = []
        for t in np.linspace(0.0, 1.0, 50):
            r = 0.03 + 0.44 * t
            a = arm * math.pi / 2 + 2.6 * t * math.pi * 0.8
            pts.append((0.5 + math.cos(a) * r, 0.5 + math.sin(a) * r))
        taper(p, pts, 0.07, 0.006)
    p.ellipse(0.5, 0.5, 0.04)
    return finish(p.array(), glow=1.6)


# ---- INFERNAL
def m_soul_corpse():
    p = Pen()
    p.ring(0.5, 0.5, 0.46, 0.02)
    for k in range(20):
        a = TAU * k / 20
        taper(p, [(0.5 + math.cos(a) * 0.46, 0.5 + math.sin(a) * 0.46), (0.5 + math.cos(a + 0.05) * 0.49, 0.5 + math.sin(a + 0.05) * 0.49)], 0.02, 0.004)
    p.ellipse(0.5, 0.42, 0.22, 0.2)                                                       # cranium
    p.poly([(0.36, 0.55), (0.64, 0.55), (0.6, 0.76), (0.4, 0.76)])                        # jaw
    holes = Pen()
    holes.ellipse(0.42, 0.44, 0.055, 0.065)
    holes.ellipse(0.58, 0.44, 0.055, 0.065)
    holes.poly([(0.5, 0.5), (0.47, 0.57), (0.53, 0.57)])
    for x in (0.44, 0.5, 0.56):
        holes.line([(x, 0.65), (x, 0.76)], 0.01)
    a = np.clip(p.array() - holes.array(), 0, 1)
    return finish(a, glow=1.1, base=0.45)


# ================================================================================================ ARMOUR SHELLS (64x64, tileable)
def _tile_voronoi(n, cells, seed):
    rng = np.random.default_rng(seed)
    pts = rng.random((cells, 2)) * n
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    best = np.full((n, n), 1e9, np.float32)
    second = np.full((n, n), 1e9, np.float32)
    for (px, py) in pts:
        dx = np.abs(xx - px)
        dy = np.abs(yy - py)
        dx = np.minimum(dx, n - dx)
        dy = np.minimum(dy, n - dy)
        d = np.hypot(dx, dy)
        second = np.where(d < best, best, np.minimum(second, d))
        best = np.minimum(best, d)
    return best, second


def sh_motes():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    flow = np.sin((yy / n + 0.15 * np.sin(xx / n * TAU * 2)) * TAU * 3) * 0.5 + 0.5
    a = np.clip((flow - 0.62) * 3.0, 0, 1) * (0.5 + 0.5 * noise(n, n, 8, 71))
    return grey(a * 0.8, 0.7 + 0.3 * a)


def sh_trails():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    d = ((xx + yy * 0.5) / n * 3) % 1
    streak = np.clip(1 - np.abs(d - 0.5) * 16, 0, 1) * (0.4 + 0.6 * noise(n, n, 16, 72))
    return grey(np.clip(streak, 0, 1) * 0.9, 0.75 + 0.25 * streak)


def sh_circles():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    cx = (xx % 32) - 16
    cy = (yy % 32) - 16
    r = np.hypot(cx, cy)
    ring = np.clip(1 - np.abs(r - 12) * 0.9, 0, 1) + np.clip(1 - np.abs(r - 7) * 1.2, 0, 1) * 0.7
    ang = np.arctan2(cy, cx)
    tick = (np.clip(1 - np.abs(r - 14.5) * 1.1, 0, 1)) * (np.sin(ang * 12) > 0.6)
    a = np.clip(ring + tick, 0, 1)
    return grey(a * 0.9, 0.7 + 0.3 * a)


def sh_shards():
    n = 64
    best, second = _tile_voronoi(n, 14, 73)
    edge = np.clip(1 - (second - best) * 0.7, 0, 1)
    facet = 0.35 + 0.65 * noise(n, n, 16, 74)
    a = np.clip(0.55 * facet + edge * 0.55, 0, 1)
    return grey(a, np.clip(0.45 + 0.55 * edge + 0.2 * facet, 0, 1))


def sh_fluid():
    n = 64
    best, _ = _tile_voronoi(n, 9, 75)
    bubble = np.clip((9 - best) / 9, 0, 1)
    rim = np.clip(1 - np.abs(best - 7.5) * 0.7, 0, 1)
    hi = np.clip(1 - np.hypot(best - 3.0, 0) * 0.5, 0, 1) * (bubble > 0.1)
    a = np.clip(bubble * 0.55 + rim * 0.6, 0, 1)
    return grey(a, np.clip(0.5 + 0.5 * rim + 0.3 * hi, 0, 1))


def sh_spiral():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    ph = (yy / n * 2 + np.sin(xx / n * TAU) * 0.18) % 1
    band = np.clip(1 - np.abs(ph - 0.5) * 7, 0, 1)
    ph2 = (yy / n * 2 + 0.5 + np.sin(xx / n * TAU + 1.6) * 0.18) % 1
    band2 = np.clip(1 - np.abs(ph2 - 0.5) * 11, 0, 1) * 0.7
    a = np.clip(band + band2, 0, 1)
    return grey(a * 0.9, 0.7 + 0.3 * a)


def sh_infernal():
    n = 64
    best, second = _tile_voronoi(n, 11, 76)
    crack = np.clip(1 - (second - best) * 0.9, 0, 1)
    dark = 0.55 + 0.35 * noise(n, n, 8, 77)
    a = np.clip(dark, 0, 1)
    return grey(a, np.clip(0.25 + 0.75 * crack, 0, 1))


# ================================================================================================ MAIN
GROUPS = {"motes": grp_motes, "trails": grp_trails, "circles": grp_circles, "shards": grp_shards, "fluid": grp_fluid, "spiral": grp_spiral,
          "infernal": grp_infernal}
SHELLS = {"motes": sh_motes, "trails": sh_trails, "circles": sh_circles, "shards": sh_shards, "fluid": sh_fluid, "spiral": sh_spiral,
          "infernal": sh_infernal}
MAGICS = [("air", m_air), ("hair", m_hair), ("memory", m_memory), ("mineral", m_mineral), ("modification", m_modification), ("mucus", m_mucus),
          ("mud", m_mud), ("nail", m_nail), ("permeation", m_permeation), ("poison_plant", m_poison_plant), ("red_ochre", m_red_ochre),
          ("rock", m_rock), ("sandstone", m_sandstone), ("scale", m_scale), ("shakudo", m_shakudo), ("skin", m_skin), ("smoke", m_smoke),
          ("snow", m_snow), ("song", m_song), ("soul_corpse", m_soul_corpse), ("soul", m_soul), ("sound", m_sound), ("spike", m_spike),
          ("switching", m_switching), ("tongue", m_tongue), ("tree", m_tree), ("stone", m_stone), ("vine", m_vine), ("vortex", m_vortex),
          ("wing", m_wing)]


def preview(images, path):
    """A contact sheet on a dark checker: the bulk sprites, the 30 accents, the shells (tiled 2x2)."""
    cell = 128
    cols = 8
    items = images
    rows = (len(items) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * (cell + 14)), (14, 16, 22, 255))
    d = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(items):
        x, y = (i % cols) * cell, (i // cols) * (cell + 14)
        bg = Image.new("RGBA", (cell, cell), (26, 30, 40, 255))
        if im.size[0] == 64:
            t = Image.new("RGBA", (128, 128))
            for a in range(2):
                for b in range(2):
                    t.paste(im, (a * 64, b * 64))
            im = t
        elif im.size[0] != cell:
            im = im.resize((cell, cell), Image.LANCZOS)
        tint = Image.new("RGBA", im.size, (120, 220, 200, 255))
        rgba = Image.composite(tint, Image.new("RGBA", im.size, (0, 0, 0, 0)), im.split()[3])
        grey_l = im.split()[0]
        rgba = Image.merge("RGBA", (grey_l.point(lambda v: int(v * 0.5 + 40)), grey_l.point(lambda v: int(v * 0.95)), grey_l.point(lambda v: int(v * 0.85)), im.split()[3]))
        bg.alpha_composite(rgba)
        sheet.paste(bg, (x, y))
        d.text((x + 3, y + cell + 1), name, fill=(200, 210, 220, 255))
    sheet.convert("RGB").save(path)
    print("preview", path, sheet.size)


def main():
    args = sys.argv[1:]
    prev = args[args.index("--preview") + 1] if "--preview" in args else None
    done = []
    for key, fn in GROUPS.items():
        im = fn()
        save(im, "wiki_grp_" + key)
        done.append(("grp_" + key, im))
    for name, fn in MAGICS:
        im = fn()
        save(im, "wiki_" + name)
        done.append((name, im))
    for key, fn in SHELLS.items():
        im = fn()
        save(im, "wiki_shell_" + key)
        done.append(("shell_" + key, im))
    if prev:
        preview(done, prev)


if __name__ == "__main__":
    main()

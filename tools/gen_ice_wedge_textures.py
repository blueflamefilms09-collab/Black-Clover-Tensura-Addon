"""Generates the Ice Wedge Magic VFX textures.

Output: src/main/resources/assets/nusmp/textures/particle/ice_wedge_*.png   (python3 -B tools/gen_ice_wedge_textures.py)

Look reference (owner art, docs/attributes/art_reference/pack_ice_wedge.webp): ice drawn as chiselled WEDGES. A tall flat fan / leaf of
blunt-tipped wedges radiating from a bright spine, a giant faceted hand of crystal fingers, a crown of long spikes. Bright cyan-white
facets, patchy deeper-blue shading inside, a dark blue outline on every edge, a white-hot rim glow, a few cold sparkles around.

Everything is grey-scale + alpha so the vertex colour tints it (dark outline = tint * 0.25, lit facet = tint). Deterministic seeds.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


# ---------------------------------------------------------------------------------------------------- helpers
def noise(w, h, cells, seed, octaves=4):
    """Fractal value noise 0..1 (w x h)."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = max(2, int(cells * (2 ** o)))
        g = rng.random((c, c)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(im, np.float32) / 255.0
        tot += amp
        amp *= 0.5
    return out / tot


class Canvas:
    """Draw in final-pixel units; supersampled; luma + alpha stored separately, premultiplied on down-scale."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.L = Image.new("L", (w * SS, h * SS), 0)
        self.A = Image.new("L", (w * SS, h * SS), 0)
        self.dL, self.dA = ImageDraw.Draw(self.L), ImageDraw.Draw(self.A)

    def _p(self, pts):
        return [(x * SS, y * SS) for x, y in pts]

    def poly(self, pts, fill, alpha=255):
        p = self._p(pts)
        self.dL.polygon(p, fill=int(fill))
        self.dA.polygon(p, fill=int(alpha))

    def line(self, pts, fill, width, alpha=255):
        p = self._p(pts)
        w = max(1, int(width * SS))
        self.dL.line(p, fill=int(fill), width=w, joint="curve")
        self.dA.line(p, fill=int(alpha), width=w, joint="curve")

    def outline(self, pts, fill, width):
        self.line(list(pts) + [pts[0]], fill, width)

    def ellipse(self, box, fill, alpha=255):
        b = [v * SS for v in box]
        self.dL.ellipse(b, fill=int(fill))
        self.dA.ellipse(b, fill=int(alpha))

    def arrays(self):
        L = np.asarray(self.L.resize((self.w, self.h), Image.BOX), np.float32) / 255.0
        # premultiplied down-scale
        Lp = np.asarray(Image.fromarray((np.asarray(self.L, np.float32) * np.asarray(self.A, np.float32) / 255).astype(np.uint8))
                        .resize((self.w, self.h), Image.BOX), np.float32) / 255.0
        A = np.asarray(self.A.resize((self.w, self.h), Image.BOX), np.float32) / 255.0
        L = np.where(A > 1e-3, Lp / np.maximum(A, 1e-3), 0.0)
        return np.clip(L, 0, 1), np.clip(A, 0, 1)


def save(name, L, A):
    rgba = np.zeros(L.shape + (4,), np.uint8)
    v = (np.clip(L, 0, 1) * 255).astype(np.uint8)
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = v
    rgba[..., 3] = (np.clip(A, 0, 1) * 255).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, name + ".png"))
    print("wrote", name, L.shape[::-1])


def halo(A, sigma, k):
    im = Image.fromarray((np.clip(A, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(sigma))
    return np.asarray(im, np.float32) / 255.0 * k


def blotch(L, A, seed, strength=0.28, cells=5):
    """The patchy deeper-blue shading inside the ice of the stills."""
    n = noise(L.shape[1], L.shape[0], cells, seed)
    n = np.clip((n - 0.35) * 2.2, 0, 1)
    return np.clip(L * (1 - strength * n), 0, 1)


DARK, LIT, SHADE = 55, 242, 168


def wedge(c, base, ang, length, bw, tipw, light=1.0, pointy=0.0, edge=1.6):
    """A chiselled ice wedge: base centre, angle (deg, 0 = right, 90 = up), blunt chisel end of width tipw (pointy 0..1 sharpens it)."""
    a = math.radians(ang)
    d = (math.cos(a), -math.sin(a))
    n = (d[1], -d[0])
    bx, by = base

    def P(t, s):
        return (bx + d[0] * length * t + n[0] * s, by + d[1] * length * t + n[1] * s)

    tt = 0.9 + 0.1 * pointy
    hw = tipw * 0.5 * (1 - pointy)
    BL, BR = P(0, -bw / 2), P(0, bw / 2)
    TL, TR = P(tt, -hw), P(tt, hw)
    ap = P(1.0, 0)
    B0 = P(0.0, 0)
    ML = P(0.45, -bw * 0.5 * (1 - 0.45 * (1 - tipw / bw)))
    MR = P(0.45, bw * 0.5 * (1 - 0.45 * (1 - tipw / bw)))
    left = [BL, B0, ap, TL] if hw > 0.01 else [BL, B0, ap]
    right = [B0, BR, TR, ap] if hw > 0.01 else [B0, BR, ap]
    c.poly(left, min(255, LIT * light))
    c.poly(right, SHADE * light)
    # a lit tip facet and a darker foot
    c.poly([P(0.62, -bw * 0.18), P(0.62, bw * 0.18), ap], min(255, 252 * light))
    outl = [BL, BR, TR, ap, TL] if hw > 0.01 else [BL, BR, ap]
    c.outline(outl, DARK, edge)
    c.line([B0, P(0.78, 0)], 250, edge * 0.7)


# ---------------------------------------------------------------------------------------------------- fan (the leaf of wedges)
def gen_fan():
    N = 256
    c = Canvas(N, N)
    cx, cy = N * 0.5, N * 0.54
    a, b = N * 0.36, N * 0.46
    # soft body under the wedges
    rng = np.random.default_rng(11)
    for row, (steps, lscale, light) in enumerate([(8, 0.9, 0.7), (7, 1.0, 1.0)]):
        for k in range(steps):
            th = -84 + 168 * (k + (0.5 if row == 0 else 0.0)) / (steps if row == 0 else steps - 1)
            for side in (-1, 1):
                t = math.radians(th)
                r = 1 / math.sqrt((math.cos(t) / a) ** 2 + (math.sin(t) / b) ** 2) * lscale
                ang = th if side == 1 else 180 - th
                ln = r * (0.93 + 0.1 * rng.random())
                bw = max(9.0, ln * 0.52)
                wedge(c, (cx, cy), ang, ln, bw, bw * 0.55, light, 0.0, 1.5)
    # radial white streaks and the bright spine
    for k in range(18):
        t = math.radians(-90 + 180 * k / 17)
        for side in (-1, 1):
            ex = cx + side * math.cos(t) * a * 0.98
            ey = cy - math.sin(t) * b * 0.98
            c.line([(cx, cy), (ex, ey)], 255, 0.8, 140)
    c.line([(cx, cy - b * 0.97), (cx, cy + b * 0.95)], 255, 3.2)
    c.line([(cx, cy - b * 0.97), (cx, cy + b * 0.95)], 255, 1.2)
    L, A = c.arrays()
    L = blotch(L, A, 5, 0.3, 6)
    # hot core
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    core = np.exp(-(((xx - cx) / (N * 0.07)) ** 2 + ((yy - cy) / (N * 0.14)) ** 2))
    L = np.clip(L + core * 0.5, 0, 1)
    A0 = A
    A = np.maximum(A0, halo(A0, 7, 0.55))
    L = np.where(A0 > 0.5, L, 0.9)
    save("ice_wedge_fan", L, A)


# ---------------------------------------------------------------------------------------------------- single spike + cluster + shard
def gen_spike():
    N = 128
    c = Canvas(N, N)
    wedge(c, (N * 0.5, N * 0.99), 90, N * 0.97, N * 0.46, 0, 1.0, 1.0, 1.4)
    # internal facet cuts
    c.line([(N * 0.5, N * 0.99), (N * 0.46, N * 0.45)], DARK, 0.8, 150)
    c.line([(N * 0.43, N * 0.78), (N * 0.55, N * 0.62)], 255, 0.8, 200)
    L, A = c.arrays()
    L = blotch(L, A, 7, 0.3, 4)
    save("ice_wedge_spike", L, A)


def gen_cluster():
    N = 256
    c = Canvas(N, N)
    rng = np.random.default_rng(23)
    spec = [(-62, 0.62, 0.7), (62, 0.62, 0.7), (-44, 0.78, 0.8), (44, 0.78, 0.8), (-26, 0.9, 0.9), (26, 0.9, 0.9), (-9, 0.99, 1.0), (9, 0.95, 1.0), (0, 1.0, 1.0)]
    for dang, ln, light in spec:
        base = (N * (0.5 + dang / 260.0 * 0.5), N * 0.985)
        wedge(c, base, 90 - dang * 0.72, N * 0.97 * ln, N * 0.17 * (0.7 + 0.3 * ln), 0, light, 1.0, 1.5)
    for k in range(7):
        x = N * (0.12 + 0.76 * rng.random())
        c.poly([(x - 4, N * 0.99), (x + 4, N * 0.99), (x + rng.normal(0, 4), N * (0.7 + 0.2 * rng.random()))], 140, 230)
    L, A = c.arrays()
    L = blotch(L, A, 9, 0.3, 6)
    A = np.maximum(A, halo(A, 6, 0.4))
    save("ice_wedge_cluster", L, A)


def gen_shard():
    N = 64
    c = Canvas(N, N)
    pts = [(32, 3), (45, 24), (40, 46), (30, 61), (21, 40), (19, 20)]
    c.poly(pts, LIT)
    c.poly([(32, 3), (30, 61), (40, 46), (45, 24)], SHADE)
    c.poly([(32, 3), (19, 20), (21, 40), (30, 61)], 250)
    c.poly([(32, 3), (38, 26), (30, 36)], 255)
    c.poly([(30, 61), (30, 36), (40, 46)], 120)
    c.outline(pts, DARK, 1.4)
    c.line([(32, 3), (30, 61)], 255, 0.8, 200)
    L, A = c.arrays()
    L = blotch(L, A, 3, 0.25, 3)
    save("ice_wedge_shard", L, A)


# ---------------------------------------------------------------------------------------------------- ground sigil / crack / ring
def gen_sigil():
    N = 256
    c = Canvas(N, N)
    m = N / 2
    P = lambda r, a: (m + math.cos(a) * r * N, m + math.sin(a) * r * N)
    for r, w in ((0.485, 1.6), (0.465, 0.9), (0.395, 1.6), (0.19, 1.5)):
        c.outline([P(r, 2 * math.pi * i / 128) for i in range(128)], 255, w)
    # inward wedge teeth between the rings
    n = 24
    for i in range(n):
        a = 2 * math.pi * i / n
        da = math.pi / n * 0.8
        c.poly([P(0.462, a - da), P(0.462, a + da), P(0.405, a)], 225)
        c.outline([P(0.462, a - da), P(0.462, a + da), P(0.405, a)], 70, 1.0)
        # outward diamonds on the outer rim
        a2 = a + math.pi / n
        c.poly([P(0.49, a2), P(0.5, a2 - 0.03), P(0.52, a2), P(0.5, a2 + 0.03)], 255)
    # hexagram of wedges
    for tri in (0, 1):
        pts = [P(0.385, math.radians(90 + 120 * k + 60 * tri)) for k in range(3)]
        c.outline(pts, 255, 2.0)
    for k in range(6):
        a = math.radians(60 * k)
        wedge(c, P(0.19, a), -math.degrees(a), 0.175 * N, 0.07 * N, 0.04 * N, 1.0, 0.7, 1.3)
    # snowflake in the middle
    for k in range(6):
        a = math.radians(60 * k + 30)
        c.line([(m, m), P(0.14, a)], 255, 1.6)
        for t in (0.07, 0.1):
            for s in (-1, 1):
                c.line([P(t, a), P(t + 0.035, a + s * 0.6)], 255, 1.0)
    L, A = c.arrays()
    n = noise(N, N, 6, 4)
    L = np.clip(L * (0.82 + 0.3 * n), 0, 1)
    A = np.clip(A * (0.88 + 0.12 * n), 0, 1)
    save("ice_wedge_sigil", L, np.maximum(A, halo(A, 3, 0.45)))
    return


def gen_crack():
    N = 256
    c = Canvas(N, N)
    rng = np.random.default_rng(31)
    m = N / 2

    def branch(x, y, ang, ln, w, depth):
        pts = [(x, y)]
        for _ in range(int(ln / 7) + 1):
            ang += rng.normal(0, 0.28)
            x += math.cos(ang) * 7
            y += math.sin(ang) * 7
            pts.append((x, y))
            if depth < 3 and rng.random() < 0.28:
                branch(x, y, ang + rng.choice([-1, 1]) * (0.5 + rng.random() * 0.5), ln * 0.4, w * 0.6, depth + 1)
        for i in range(len(pts) - 1):
            ww = max(0.7, w * (1 - i / len(pts)))
            c.line([pts[i], pts[i + 1]], 255, ww)
            c.line([pts[i], pts[i + 1]], 255, ww * 3.2, 70)
    for k in range(11):
        branch(m, m, 2 * math.pi * (k + rng.random() * 0.5) / 11, N * (0.2 + 0.13 * rng.random()), 3.4, 0)
    # a few frost wedges at the centre
    L, A = c.arrays()
    save("ice_wedge_crack", L, np.maximum(A, halo(A, 2.4, 0.7)))


def gen_ring():
    N = 256
    c = Canvas(N, N)
    m = N / 2
    P = lambda r, a: (m + math.cos(a) * r * N, m + math.sin(a) * r * N)
    rng = np.random.default_rng(41)
    n = 40
    for i in range(n):
        a = 2 * math.pi * i / n + rng.normal(0, 0.03)
        ln = 0.06 + 0.07 * rng.random() * (1 + (i % 3 == 0))
        da = 0.045
        c.poly([P(0.37, a - da), P(0.37, a + da), P(0.37 + ln, a)], 255)
        c.poly([P(0.37, a - da), P(0.37, a + da), P(0.37 - ln * 0.35, a)], 200)
    c.outline([P(0.37, 2 * math.pi * i / 160) for i in range(160)], 255, 2.6)
    L, A = c.arrays()
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    r = np.hypot(xx - m, yy - m) / N
    haze = np.exp(-((r - 0.37) / 0.05) ** 2)
    A = np.clip(np.maximum(A, haze * 0.65), 0, 1)
    L = np.maximum(L, haze)
    edge = np.clip((0.5 - r) / 0.06, 0, 1)
    save("ice_wedge_ring", L, A * edge)


# ---------------------------------------------------------------------------------------------------- snow, glint, streak
def gen_snow():
    N = 64
    c = Canvas(N, N)
    m = N / 2
    for k in range(6):
        a = math.radians(60 * k + 90)
        d = (math.cos(a), -math.sin(a))
        c.line([(m, m), (m + d[0] * 29, m + d[1] * 29)], 255, 2.0)
        for t, ln in ((13, 9), (20, 7)):
            for s in (-1, 1):
                b = a + s * 0.95
                x0, y0 = m + d[0] * t, m + d[1] * t
                c.line([(x0, y0), (x0 + math.cos(b) * ln, y0 - math.sin(b) * ln)], 255, 1.5)
        c.poly([(m + d[0] * 29 - 2, m + d[1] * 29), (m + d[0] * 29, m + d[1] * 29 - 2), (m + d[0] * 29 + 2, m + d[1] * 29), (m + d[0] * 29, m + d[1] * 29 + 2)], 255)
    c.ellipse((m - 4, m - 4, m + 4, m + 4), 255)
    L, A = c.arrays()
    save("ice_wedge_snow", L, np.maximum(A, halo(A, 1.6, 0.8)))


def gen_glint():
    N = 128
    yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
    x, y = (xx - N / 2 + 0.5) / (N / 2), (yy - N / 2 + 0.5) / (N / 2)
    r = np.hypot(x, y)
    cross = np.exp(-np.abs(y) * 38) * np.exp(-np.abs(x) * 3.2) + np.exp(-np.abs(x) * 38) * np.exp(-np.abs(y) * 3.2)
    xr, yr = (x + y) * 0.7071, (y - x) * 0.7071
    diag = 0.45 * (np.exp(-np.abs(yr) * 45) * np.exp(-np.abs(xr) * 5.5) + np.exp(-np.abs(xr) * 45) * np.exp(-np.abs(yr) * 5.5))
    core = np.exp(-(r / 0.12) ** 2) * 1.2 + np.exp(-(r / 0.35) ** 2) * 0.35
    v = np.clip(cross + diag + core, 0, 1) * np.clip(1 - r, 0, 1) ** 0.6
    save("ice_wedge_glint", np.ones_like(v), v)


def gen_streak():
    W, H = 64, 256
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u, v = xx / (W - 1), yy / (H - 1)
    d = np.abs(u - 0.5)
    n = noise(W, H, 3, 17, 3)
    # long striations along the flight
    rng = np.random.default_rng(5)
    st = np.zeros((H, W), np.float32)
    for _ in range(22):
        ux = 0.5 + rng.normal(0, 0.16)
        v0, ln = rng.random(), 0.12 + rng.random() * 0.4
        prof = np.exp(-((u - ux) / 0.012) ** 2) * np.clip(1 - np.abs(v - v0) / ln, 0, 1)
        st += prof * (0.5 + 0.5 * rng.random())
    core = np.exp(-(d / 0.045) ** 2)
    halo_ = np.exp(-(d / 0.2) ** 2) * 0.55
    env = np.clip(1 - np.abs(u - 0.5) * 2, 0, 1) ** 0.5
    a = np.clip((core + halo_ * (0.6 + 0.4 * n) + st) * env, 0, 1) * np.clip(v * 8, 0, 1) ** 0.5 * np.clip((1 - v) * 4 + 0.35, 0, 1)
    save("ice_wedge_streak", np.ones_like(a), np.clip(a, 0, 1))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    gen_fan()
    gen_spike()
    gen_cluster()
    gen_shard()
    gen_sigil()
    gen_crack()
    gen_ring()
    gen_snow()
    gen_glint()
    gen_streak()

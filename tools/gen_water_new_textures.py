#!/usr/bin/env python3
"""
WATER remake, PHASE A = PREVIEW ONLY (nothing is registered in the game, nothing goes to assets/).

Textures of the two sprite directions (the third direction, C "real 3D toon meshes", needs no texture: see tools/gen_water_new_meshes.py):

  A  anime cel      silhouettes filled with three hard bands (shade / body / light), a white highlight shape, a cyan outline (the stills of the pack: Roar, Waterball, Nest)
  B  volumetric     the same silhouettes as translucent glass: depth gradient, fresnel rim, baked tileable caustic network, white speculars (the OneHoox armour look)

Both stylers read the SAME silhouette masks, so the two directions are compared on equal shapes. Output (preview folder only):
  docs/previews/vfx_remake/water_new/tex/<a|b>_<name>.png  +  water_new_textures.png (contact sheet)
Run:  python tools/gen_water_new_textures.py        (Pillow + numpy only, deterministic)
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "water_new", "tex")

# --------------------------------------------------------------------------------------------------------------- palettes (own art, not measured from the anime)
CEL = {"outline": (31, 168, 232), "light": (112, 214, 255), "body": (46, 143, 224), "shade": (30, 95, 168), "hi": (255, 255, 255), "foam": (234, 248, 255)}
GLASS = {"deep": (20, 80, 127), "body": (46, 154, 224), "mid": (111, 208, 255), "rim": (216, 244, 255), "teal": (14, 124, 147)}


# --------------------------------------------------------------------------------------------------------------- helpers
def blur(a, s):
    im = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(s))
    return np.asarray(im, np.float32) / 255.0


def shift(a, dx, dy):
    out = np.zeros_like(a)
    h, w = a.shape
    xs, xe = max(0, dx), min(w, w + dx)
    ys, ye = max(0, dy), min(h, h + dy)
    out[ys:ye, xs:xe] = a[ys - dy:ye - dy, xs - dx:xe - dx]
    return out


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def spline(pts, n=10, closed=True):
    """Catmull-Rom through the points."""
    P = [np.array(p, float) for p in pts]
    out = []
    m = len(P)
    rng = range(m) if closed else range(m - 1)
    for i in rng:
        p0, p1, p2, p3 = P[(i - 1) % m], P[i], P[(i + 1) % m], P[(i + 2) % m]
        if not closed:
            p0 = P[max(i - 1, 0)]
            p3 = P[min(i + 2, m - 1)]
        for k in range(n):
            t = k / n
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    if not closed:
        out.append(P[-1])
    return [tuple(p) for p in out]


class Mk:
    """Draws a silhouette at 4x the working size and hands it back as a float mask."""

    def __init__(self, w, h, work=2):
        self.w, self.h, self.k = w * work, h * work, work
        self.ss = 2
        self.im = Image.new("L", (self.w * self.ss, self.h * self.ss), 0)
        self.d = ImageDraw.Draw(self.im)

    def S(self, p):
        return (p[0] * self.k * self.ss, p[1] * self.k * self.ss)

    def poly(self, pts, v=255):
        self.d.polygon([self.S(p) for p in pts], fill=v)

    def ell(self, cx, cy, rx, ry, v=255):
        a, b = self.S((cx - rx, cy - ry)), self.S((cx + rx, cy + ry))
        self.d.ellipse([a, b], fill=v)

    def line(self, pts, w, v=255):
        self.d.line([self.S(p) for p in pts], fill=v, width=int(w * self.k * self.ss), joint="curve")

    def mask(self):
        im = self.im.resize((self.w, self.h), Image.LANCZOS)
        return np.asarray(im, np.float32) / 255.0


def teeth(p0, p1, n, h, side, wbase=0.5):
    """zig-zag from p0 to p1 with n teeth pointing to `side` (+1/-1 of the left normal)"""
    p0, p1 = np.array(p0, float), np.array(p1, float)
    d = p1 - p0
    L = np.linalg.norm(d)
    d /= L
    nrm = np.array([-d[1], d[0]]) * side
    out = []
    for i in range(n):
        a = p0 + d * L * (i + (1 - wbase) / 2) / n
        b = p0 + d * L * (i + 0.5) / n + nrm * h * (0.75 + 0.25 * ((i * 7) % 3) / 2)
        c = p0 + d * L * (i + (1 + wbase) / 2) / n
        out += [tuple(a), tuple(b + d * h * 0.12), tuple(c)]
    return out


# --------------------------------------------------------------------------------------------------------------- the silhouettes (float masks, working size = 2x the texture)
def head_mask():
    """dragon head facing +x, mouth open, 256 x 256 (neck at x = 0)"""
    m = Mk(256, 256)
    top = [(0, 80), (12, 62), (20, 26), (31, 50), (44, 48), (56, 14), (70, 44), (84, 42), (95, 20), (104, 46), (122, 52), (150, 58), (178, 66), (206, 82), (232, 94), (251, 104), (252, 114)]
    up_teeth = teeth((248, 118), (122, 142), 5, 26, -1)           # points along the upper lip, fangs down
    corner = [(104, 150)]
    low_teeth = teeth((112, 160), (226, 226), 5, 26, 1)           # lower jaw top edge, fangs up
    chin = [(246, 232), (238, 244), (200, 247), (150, 238), (100, 228), (60, 212), (0, 196)]
    pts = top[:-1] + [top[-1]] + up_teeth + corner + low_teeth + chin
    m.poly(pts)
    # horns / fins sweeping back
    m.poly([(70, 60), (30, 72), (6, 100), (46, 90)])
    return m.mask()


def body_mask():
    """tileable water body strip 256 x 64 (u along the flight)"""
    m = Mk(256, 64)
    u = np.arange(0, 257, 4)
    top = [(x, 9 + 3.0 * math.sin(2 * math.pi * x / 128.0)) for x in u]
    bot = [(x, 55 - 3.0 * math.sin(2 * math.pi * x / 128.0 + 1.0)) for x in u][::-1]
    m.poly(top + bot)
    return m.mask()


def blob_mask():
    m = Mk(128, 128)
    m.ell(64, 66, 52, 50)
    m.ell(98, 38, 14, 13)
    return m.mask()


def drop_mask():
    m = Mk(64, 64)
    pts = spline([(32, 4), (46, 34), (50, 46), (42, 58), (32, 61), (22, 58), (14, 46), (18, 34)], 8)
    m.poly(pts)
    return m.mask()


def crown_mask():
    """splash crown 256 x 128: a flat foam base with curling horns, each tipped with a separate droplet"""
    m = Mk(256, 128)
    m.ell(128, 108, 122, 16)
    rng = np.random.default_rng(7)
    xs = [22, 56, 92, 128, 164, 200, 234]
    hs = [58, 88, 70, 104, 72, 86, 56]
    for x, h in zip(xs, hs):
        lean = (x - 128) / 128.0 * 14
        top = (x + lean, 108 - h)
        pts = spline([(x - 15, 112), (x - 11, 108 - h * 0.45), (top[0] - 7, top[1] + 9), top, (top[0] + 8, top[1] + 12), (x + 11, 108 - h * 0.4), (x + 15, 112)], 6)
        m.poly(pts)
        m.ell(top[0] + lean * 0.25, top[1] - 11, 5.5, 6.5)
    for i in range(12):                                                   # small foam beads on the base
        m.ell(rng.uniform(20, 236), rng.uniform(100, 122), rng.uniform(4, 8), rng.uniform(3, 6))
    return m.mask()


def ring_mask():
    """ground ripple 256 x 256: three wobbling water rings (a wave in each radius), one small break each, foam beads on the outer ring"""
    m = Mk(256, 256)
    rng = np.random.default_rng(5)
    for (r0, wd, ph, br) in ((116, 15, 0.3, 40), (84, 10, 1.7, 200), (56, 6, 2.9, 300)):
        outer, inner = [], []
        for i in range(0, 361, 3):
            a = math.radians(i)
            r = r0 + 4 * math.sin(5 * a + ph) + 3 * math.sin(9 * a + 2 * ph)
            w_ = wd * (0.75 + 0.25 * math.sin(3 * a + ph * 2))
            if br - 5 < i < br + 5:
                w_ *= 0.0
            outer.append((128 + (r + w_) * math.cos(a), 128 + (r + w_) * math.sin(a)))
            inner.append((128 + (r - w_) * math.cos(a), 128 + (r - w_) * math.sin(a)))
        m.poly(outer + inner[::-1])
    for i in range(16):
        a = rng.uniform(0, math.tau)
        r = 122 + rng.uniform(-3, 7)
        m.ell(128 + r * math.cos(a), 128 + r * math.sin(a), rng.uniform(4, 8), rng.uniform(4, 8))
    return m.mask()


def sphere_mask():
    m = Mk(256, 256)
    m.ell(128, 128, 120, 120)
    return m.mask()


def swirl_mask():
    """a spiral band, 1.7 turns, tapering (256 x 256)"""
    m = Mk(256, 256)
    pts_o, pts_i = [], []
    N = 80
    for i in range(N + 1):
        t = i / N
        ang = t * 1.7 * 2 * math.pi
        r = 12 + 108 * t
        wd = 22 * (1 - 0.8 * t) + 2
        pts_o.append((128 + (r + wd) * math.cos(ang), 128 + (r + wd) * math.sin(ang)))
        pts_i.append((128 + (r - wd) * math.cos(ang), 128 + (r - wd) * math.sin(ang)))
    m.poly(pts_o + pts_i[::-1])
    return m.mask()


def dome_mask():
    """half dome 256 x 140 (flat bottom), with three spiral eddies cut out as lighter bands by the stylers (the mask is only the silhouette)"""
    m = Mk(256, 140)
    pts = [(6 + 244 * (0.5 - 0.5 * math.cos(math.pi * i / 40)), 130 - 124 * math.sin(math.pi * i / 40) ** 0.9) for i in range(41)]
    m.poly(pts + [(250, 130), (6, 130)])
    return m.mask()


def lance_mask():
    """lance 256 x 64, tip at +x: a leaf head, a slim shaft with two back-swept barbs"""
    m = Mk(256, 64)
    m.poly([(250, 32), (200, 22), (150, 17), (110, 25), (70, 28), (0, 29), (0, 35), (70, 36), (110, 39), (150, 47), (200, 42)])
    m.poly([(150, 18), (120, 4), (106, 25)])
    m.poly([(150, 46), (120, 60), (106, 39)])
    return m.mask()


def wing_mask():
    """a wing of water feathers, 256 x 256, root at the lower left, a swept leading edge and long overlapping feathers fanning to the right"""
    m = Mk(256, 256)
    root = np.array([22, 232])
    m.poly(spline([(18, 236), (30, 190), (70, 120), (140, 62), (214, 34), (250, 30), (200, 80), (150, 140), (100, 200), (60, 238)], 8))
    for i, (ang, ln, wd) in enumerate([(-6, 232, 26), (-20, 236, 25), (-34, 226, 24), (-48, 208, 22), (-62, 184, 21), (-76, 152, 19), (-90, 118, 17)]):
        a = math.radians(ang)
        d = np.array([math.cos(a), math.sin(a)])
        n = np.array([-d[1], d[0]])
        base = root + d * 14
        tip = root + d * ln
        pts = spline([tuple(base - n * wd * 0.5), tuple(root + d * ln * 0.55 - n * wd), tuple(tip), tuple(root + d * ln * 0.55 + n * wd), tuple(base + n * wd * 0.5)], 8, closed=True)
        m.poly(pts)
    return m.mask()


def flash_mask():
    m = Mk(128, 128)
    pts = []
    for i in range(24):
        a = i * math.pi / 12
        r = 60 if i % 2 == 0 else 22
        pts.append((64 + r * math.cos(a), 64 + r * math.sin(a)))
    m.poly(pts)
    return m.mask()


# --------------------------------------------------------------------------------------------------------------- stylers
def to_rgba(rgb, a, w=None):
    out = np.zeros(rgb.shape[:2] + (4,), np.float32)
    out[..., :3] = rgb
    out[..., 3] = a
    return out


def finish(rgba, size):
    im = Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA")
    # premultiply-safe downscale
    pm = np.asarray(im, np.float32) / 255.0
    pm[..., :3] *= pm[..., 3:4]
    im2 = Image.fromarray((pm * 255).astype(np.uint8), "RGBA").resize(size, Image.LANCZOS)
    a = np.asarray(im2, np.float32) / 255.0
    rgb = np.where(a[..., 3:4] > 1e-3, a[..., :3] / np.maximum(a[..., 3:4], 1e-3), 0)
    a[..., :3] = np.clip(rgb, 0, 1)
    return Image.fromarray((a * 255 + 0.5).astype(np.uint8), "RGBA")


def col(c):
    return np.array(c, np.float32) / 255.0


def cel(mask, light=(-1.0, -1.0), strokes=None, outline=7, shade_t=0.05, light_t=0.04, blobs=None, hi=True, stripes=None, scale=1.0):
    """3 hard bands from a pillow height field + white highlight + outline. mask: float array (working size)."""
    h, w = mask.shape
    m = mask > 0.5
    P = CEL
    H = blur(mask, 0.075 * min(h, w) * scale)
    gy, gx = np.gradient(H)
    lum = -(gx * light[0] + gy * light[1])                         # >0 where the surface faces the light
    lum = blur(lum / (np.abs(lum).max() + 1e-6), 1.2)
    img = np.zeros((h, w, 3), np.float32)
    img[:] = col(P["body"])
    band_l = lum > light_t * 3.0
    band_s = lum < -shade_t * 3.0
    img[band_l] = col(P["light"])
    img[band_s] = col(P["shade"])
    if stripes is not None:                                        # extra hard-edged lighter streaks (the banded body of the Roar still)
        s = stripes(w, h) & m & ~band_s
        img[s] = col(P["light"]) * 0.8 + col(P["foam"]) * 0.2
    if hi:                                                         # one crisp white highlight: the most lit part, shrunk
        inner = blur(mask, 0.05 * min(h, w)) > 0.82
        hl = (lum > 0.62) & inner
        hl = blur(hl.astype(np.float32), 1.5) > 0.5
        img[hl] = col(P["hi"])
    if strokes is not None:
        s = strokes(w, h) & m
        img[s] = col(P["hi"])
    if blobs is not None:
        b = blobs(w, h) & m
        img[b] = col(P["light"])
    a = mask.copy()
    out = to_rgba(img, a)
    # outline
    k = outline * (h / 512.0) * 2.0
    grown = blur(mask, k * 0.45) > 0.12
    ol = grown & ~m
    olf = np.clip(blur(ol.astype(np.float32), 0.8), 0, 1)
    out[..., :3] = np.where(ol[..., None], col(P["outline"]), out[..., :3])
    out[..., 3] = np.where(ol, 1.0, np.where(m, 1.0, 0.0)) * np.maximum(1.0, 1.0)
    # soft edge: re-apply the original anti-aliased coverage inside, outline coverage outside
    edge_in = np.clip(mask * 1.0, 0, 1)
    out[..., 3] = np.where(m, np.maximum(edge_in, 0.0) * 0 + 1.0, out[..., 3])
    return out


def worley_edges(n, pts, seed, scale=1.0):
    """tileable cell-edge map F2 - F1 on an n x n grid (periodic)"""
    rng = np.random.default_rng(seed)
    P = rng.uniform(0, n, (pts, 2))
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    d1 = np.full((n, n), 1e9, np.float32)
    d2 = np.full((n, n), 1e9, np.float32)
    for px, py in P:
        dx = np.abs(xx - px)
        dx = np.minimum(dx, n - dx)
        dy = np.abs(yy - py)
        dy = np.minimum(dy, n - dy)
        d = np.sqrt(dx * dx + dy * dy)
        d2 = np.minimum(d2, np.maximum(d1, d))
        d1 = np.minimum(d1, d)
    e = d2 - d1
    return np.clip(1.0 - e / (n * 0.045 * scale), 0, 1) ** 1.5


def caustic(w, h, seed=3, pts=26):
    n = 256
    e = worley_edges(n, pts, seed)
    e = blur(e, 0.8)
    im = Image.fromarray((e * 255).astype(np.uint8), "L").resize((w, h), Image.BICUBIC)
    return np.asarray(im, np.float32) / 255.0


def glass(mask, cause=0.5, seed=3, spec=None, y_dark=True, a_max=0.86, a_min=0.4, rim=1.0):
    """translucent volumetric look: depth gradient, fresnel rim, baked caustics, white speculars"""
    h, w = mask.shape
    P = GLASS
    depth = np.clip(blur(mask, 0.06 * min(h, w)) * 1.0, 0, 1)
    d = smoothstep(0.35, 0.98, depth)                              # 0 at the rim, 1 deep inside
    yy = (np.mgrid[0:h, 0:w][0] / h).astype(np.float32)
    c = np.zeros((h, w, 3), np.float32)
    top = col(P["mid"]) * (1 - yy[..., None]) + col(P["body"]) * yy[..., None]
    deep = col(P["deep"]) * (0.3 + 0.7 * yy[..., None]) if y_dark else col(P["deep"])
    inner = top * (1 - 0.3 * d[..., None] * yy[..., None]) + deep * (0.28 * yy[..., None])
    c = col(P["rim"]) * (1 - d[..., None]) + inner * d[..., None]
    cau = (caustic(w, h, seed, 26) * 0.8 + caustic(w, h, seed + 40, 70) * 0.45) * cause
    c = c + cau[..., None] * (0.7 + 0.3 * d[..., None])
    a = mask * (a_min + (a_max - a_min) * (1 - d) ** 0.7 * rim)
    a = np.clip(a + cau * 0.3 * mask, 0, 1)
    if spec is not None:
        s = spec(w, h)
        c = np.where(s[..., None] > 0.5, 1.0, c)
        a = np.where(s > 0.5, 1.0 * mask, a)
    return to_rgba(np.clip(c, 0, 1), a)


# --------------------------------------------------------------------------------------------------------------- stroke helpers (white highlights drawn on top)
def stroke_fn(paths, width, size):
    def fn(w, h):
        m = Mk(size[0], size[1])
        for pts, wd in paths:
            m.line(pts, wd)
        mm = m.mask()
        if mm.shape != (h, w):
            mm = np.asarray(Image.fromarray((mm * 255).astype(np.uint8)).resize((w, h)), np.float32) / 255.0
        return mm > 0.5
    return fn


def save(img, name, size):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + ".png"))


def build():
    out = {}
    # ----------------------------------------------------------------------------------------------- head
    hm = head_mask()
    h_str = stroke_fn([([(100, 60), (150, 56), (190, 66), (230, 88)], 4), ([(120, 200), (170, 206), (214, 210)], 3.5), ([(36, 100), (70, 94), (96, 100)], 3)], 4, (256, 256))
    # banded stripes down the neck / throat as in the Roar still
    def head_stripes(w, h):
        yy, xx = np.mgrid[0:h, 0:w]
        s = ((xx + yy * 0.6) % (w / 5.0)) < (w / 24.0)
        s &= xx < w * 0.62
        return s
    img = cel(hm, strokes=h_str, stripes=head_stripes)
    # eye
    d = ImageDraw.Draw(Image.new("L", (1, 1)))
    out["a_head"] = finish(img, (256, 256))
    # paint the eye on the finished texture
    eye = out["a_head"].copy()
    ed = ImageDraw.Draw(eye)
    ed.ellipse([166, 82, 188, 100], fill=(255, 255, 255, 255), outline=(31, 168, 232, 255), width=2)
    ed.ellipse([172, 87, 183, 96], fill=(60, 220, 255, 255))
    out["a_head"] = eye
    g = glass(hm, cause=0.55, seed=11, spec=stroke_fn([([(100, 60), (150, 54), (192, 64), (232, 88)], 3.2)], 3, (256, 256)))
    gi = finish(g, (256, 256))
    gd = ImageDraw.Draw(gi)
    gd.ellipse([168, 83, 186, 99], fill=(255, 255, 255, 255))
    gd.ellipse([173, 88, 182, 96], fill=(40, 190, 240, 255))
    out["b_head"] = gi
    # ----------------------------------------------------------------------------------------------- body strip (tileable)
    bm = body_mask()
    def body_strokes(w, h):
        m = Mk(256, 64)
        pts = [(x, 22 + 4 * math.sin(2 * math.pi * x / 128.0 + 0.5)) for x in range(0, 257, 8)]
        m.line(pts, 3.2)
        return m.mask() > 0.5
    def body_stripes(w, h):
        yy, xx = np.mgrid[0:h, 0:w]
        return ((xx * 1.0 + yy * 0.8) % (w / 4.0)) < (w / 28.0)
    bimg = cel(bm, strokes=body_strokes, stripes=body_stripes, hi=False, outline=4, scale=0.6)
    out["a_body"] = finish(bimg, (256, 64))
    gb = glass(bm, cause=0.8, seed=5, a_max=0.8, a_min=0.45)
    out["b_body"] = finish(gb, (256, 64))
    # flow streaks for glass (tileable): domain-warped soft lines
    rng = np.random.default_rng(21)
    flow = np.zeros((128, 512), np.float32)
    for i in range(14):
        y0 = rng.uniform(8, 120)
        ph = rng.uniform(0, 6.28)
        amp = rng.uniform(2, 6)
        ln = rng.uniform(60, 200)
        x0 = rng.uniform(0, 512)
        for x in range(int(ln)):
            xx = int(x0 + x) % 512
            yy = int(y0 + amp * math.sin(x / 30.0 * 2 + ph))
            if 0 <= yy < 128:
                flow[yy, xx] = max(flow[yy, xx], 1.0 - x / ln)
    flow = blur(flow, 1.2)
    flow = np.clip(flow * 2.2, 0, 1)
    fl = np.zeros((128, 512, 4), np.float32)
    fl[..., :3] = 1.0
    fl[..., 3] = flow * 0.8
    out["b_flow"] = finish(fl, (256, 64))
    # ----------------------------------------------------------------------------------------------- blob / bubble / drop
    bl = blob_mask()
    s_blob = stroke_fn([([(34, 40), (42, 30), (56, 24)], 5)], 5, (128, 128))
    out["a_blob"] = finish(cel(bl, strokes=s_blob, hi=False, outline=5, scale=0.9), (128, 128))
    # bubble: a ring with a bright rim, a glint and a faint caustic (glass)
    bm2 = Mk(64, 64)
    bm2.ell(32, 32, 28, 28)
    m_b = bm2.mask()
    ring_a = np.clip(1 - blur(m_b, 0.12 * m_b.shape[0]) * 1.0, 0, 1) * m_b
    ring_a = np.clip(m_b * (0.2 + 1.6 * np.clip(blur(m_b, 4) - 0.5, 0, 1) * 0 + ring_a * 2.2), 0, 1)
    gb2 = np.zeros(m_b.shape + (4,), np.float32)
    gb2[..., :3] = col(GLASS["rim"])
    gb2[..., 3] = np.clip(ring_a * 0.9 + m_b * 0.12, 0, 1)
    gim = finish(gb2, (64, 64))
    gd2 = ImageDraw.Draw(gim)
    gd2.ellipse([15, 13, 25, 21], fill=(255, 255, 255, 255))
    gd2.ellipse([40, 44, 45, 48], fill=(255, 255, 255, 190))
    out["b_bubble"] = gim
    dm = drop_mask()
    out["a_drop"] = finish(cel(dm, strokes=stroke_fn([([(26, 30), (24, 40)], 3)], 3, (64, 64)), hi=False, outline=3, scale=0.7), (64, 64))
    out["b_drop"] = finish(glass(dm, cause=0.2, seed=2, a_max=0.9, a_min=0.55), (64, 64))
    # ----------------------------------------------------------------------------------------------- crown
    cm = crown_mask()
    out["a_crown"] = finish(cel(cm, strokes=None, hi=True, outline=6, scale=0.55), (256, 128))
    out["b_crown"] = finish(glass(cm, cause=0.7, seed=8, a_max=0.8, a_min=0.4), (256, 128))
    # ----------------------------------------------------------------------------------------------- ring
    rm = ring_mask()
    out["a_ring"] = finish(cel(rm, hi=False, outline=5, scale=0.3), (256, 256))
    rg = glass(rm, cause=0.35, seed=4, a_max=0.7, a_min=0.35)
    out["b_ripple"] = finish(rg, (256, 256))
    # ----------------------------------------------------------------------------------------------- sphere
    sm = sphere_mask()
    def sph_blobs(w, h):
        m = Mk(256, 256)
        for (x, y, rx, ry) in ((104, 96, 38, 24), (160, 150, 30, 40), (92, 168, 22, 16), (170, 76, 16, 12)):
            m.ell(x, y, rx, ry)
        return m.mask() > 0.5
    sph_hi = stroke_fn([([(52, 112), (58, 82), (84, 56), (112, 44)], 7)], 7, (256, 256))
    simg = cel(sm, strokes=sph_hi, blobs=sph_blobs, hi=False, outline=7, scale=1.4)
    sim = finish(simg, (256, 256))
    sd = ImageDraw.Draw(sim)
    sd.ellipse([150, 52, 172, 66], fill=(255, 255, 255, 255))
    out["a_sphere"] = sim
    sg = glass(sm, cause=0.5, seed=6, a_max=0.7, a_min=0.2, spec=stroke_fn([([(50, 112), (56, 82), (82, 54), (112, 42)], 6)], 6, (256, 256)))
    sgi = finish(sg, (256, 256))
    sgd = ImageDraw.Draw(sgi)
    sgd.ellipse([168, 150, 188, 164], fill=(255, 255, 255, 220))
    out["b_sphere"] = sgi
    # ----------------------------------------------------------------------------------------------- swirl
    wm = swirl_mask()
    out["a_swirl"] = finish(cel(wm, hi=True, outline=5, scale=0.4), (256, 256))
    out["b_swirl"] = finish(glass(wm, cause=0.7, seed=9, a_max=0.78, a_min=0.4), (256, 256))
    # ----------------------------------------------------------------------------------------------- dome (nest): body + three eddies
    dmk = dome_mask()
    h, w = dmk.shape
    dome_a = np.zeros((h, w, 4), np.float32)
    dome_a[..., :3] = col((38, 106, 190))
    gy, gx = np.gradient(blur(dmk, 0.12 * h))
    lum = blur(-(gx * -0.3 + gy * -1.0), 1.5)
    lum /= (np.abs(lum).max() + 1e-6)
    dome_a[..., :3][lum > 0.25] = col((54, 132, 214))
    dome_a[..., :3][lum < -0.25] = col(CEL["shade"])
    dome_a[..., 3] = 0.96 * (dmk > 0.5)
    # three eddies: spiral glyphs in the lighter blue with a white edge
    sw = np.asarray(Image.fromarray((wm * 255).astype(np.uint8)).resize((h * 2 // 2, h * 2 // 2)), np.float32) / 255.0
    for (cx, cy, sc) in ((0.2, 0.6, 0.62), (0.52, 0.42, 0.74), (0.82, 0.62, 0.6)):
        sz = int(h * sc)
        sp = np.asarray(Image.fromarray((wm * 255).astype(np.uint8)).resize((sz, sz), Image.LANCZOS), np.float32) / 255.0
        x0, y0 = int(cx * w - sz / 2), int(cy * h - sz / 2)
        for yy in range(sz):
            for xx in range(sz):
                X, Y = x0 + xx, y0 + yy
                if 0 <= X < w and 0 <= Y < h and dmk[Y, X] > 0.5 and sp[yy, xx] > 0.5:
                    edge = sp[yy, xx] < 0.9
                    dome_a[Y, X, :3] = col(CEL["foam"]) if edge else col((120, 205, 250))
    dome_a[int(h * 0.93):, :, :3] = np.where((dmk[int(h * 0.93):, :, None] > 0.5), col(CEL["foam"]), dome_a[int(h * 0.93):, :, :3])
    ol = (blur(dmk, 2.6) > 0.1) & ~(dmk > 0.5)
    dome_a[ol, :3] = col(CEL["outline"])
    dome_a[ol, 3] = 1.0
    out["a_dome"] = finish(dome_a, (256, 140))
    # glass dome sheet: caustic-filled, no eddies
    gdm = glass(dmk, cause=0.9, seed=12, a_max=0.55, a_min=0.22)
    out["b_dome"] = finish(gdm, (256, 140))
    # ----------------------------------------------------------------------------------------------- lance
    lm = lance_mask()
    out["a_lance"] = finish(cel(lm, strokes=stroke_fn([([(20, 31), (100, 30), (190, 30)], 2.6)], 3, (256, 64)), hi=False, outline=4, scale=0.5), (256, 64))
    out["b_lance"] = finish(glass(lm, cause=0.8, seed=14, a_max=0.85, a_min=0.5), (256, 64))
    # ----------------------------------------------------------------------------------------------- wing
    wgm = wing_mask()
    out["a_wing"] = finish(cel(wgm, strokes=None, hi=True, outline=6, scale=0.55), (256, 256))
    out["b_wing"] = finish(glass(wgm, cause=0.8, seed=15, a_max=0.8, a_min=0.4), (256, 256))
    # ----------------------------------------------------------------------------------------------- flash and glow (additive)
    fm = flash_mask()
    fa = np.zeros(fm.shape + (4,), np.float32)
    fa[..., :3] = 1.0
    fa[..., 3] = fm
    out["a_flash"] = finish(fa, (128, 128))
    n = 128
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.sqrt((xx - n / 2) ** 2 + (yy - n / 2) ** 2) / (n / 2)
    gl = np.zeros((n, n, 4), np.float32)
    gl[..., :3] = col((190, 236, 255))
    gl[..., 3] = np.clip(1 - r, 0, 1) ** 2.2
    out["glow"] = Image.fromarray((gl * 255).astype(np.uint8), "RGBA")
    # glint (b): small four-point star
    gl2 = np.zeros((n, n, 4), np.float32)
    gl2[..., :3] = 1.0
    gl2[..., 3] = np.clip(np.exp(-(np.abs(xx - n / 2) * 0.12 + np.abs(yy - n / 2) * 0.012)) + np.exp(-(np.abs(yy - n / 2) * 0.12 + np.abs(xx - n / 2) * 0.012)), 0, 1) * np.clip(1.3 - r, 0, 1)
    out["b_glint"] = Image.fromarray((gl2 * 255).astype(np.uint8), "RGBA")
    return out


def main():
    os.makedirs(OUT, exist_ok=True)
    tex = build()
    for k, im in tex.items():
        im.save(os.path.join(OUT, k + ".png"))
    # contact sheet on a mid-dark blue-grey background (so both pale and dark pixels show)
    names = sorted(tex)
    cols = 6
    cell = 200
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * cell, rows * cell), (48, 62, 78))
    d = ImageDraw.Draw(sheet)
    for i, k in enumerate(names):
        im = tex[k].copy()
        s = min((cell - 16) / im.width, (cell - 30) / im.height)
        im = im.resize((max(1, int(im.width * s)), max(1, int(im.height * s))), Image.LANCZOS)
        x, y = (i % cols) * cell, (i // cols) * cell
        sheet.paste(im, (x + 8, y + 6), im)
        d.text((x + 8, y + cell - 18), k, fill=(255, 255, 255))
    sheet.save(os.path.join(os.path.dirname(OUT), "water_new_textures.png"))
    print(len(tex), "textures ->", OUT)


if __name__ == "__main__":
    main()

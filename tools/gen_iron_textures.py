"""Generates the Iron Magic VFX textures (deterministic, no fonts, no external images).

    python3 -B tools/gen_iron_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/iron_*.png

Look reference (the owner's manga still "Iron God of War"): a stern iron giant drawn in stippled grey screentone with heavy black ink
linework, studded shoulders and chest (rivets in rows), a crown with a third eye, a chain at the hip, a huge gauntlet fist, bold ink
impact strokes and long speed lines, and pale glassy shards shattering around it. Spells: Iron Bullet (an iron casing launched from the
arm), Immovable Guardian Deity (a large metal bell with a Buddhist figure and rows of studs).
Texture convention: white / greyscale with alpha so the vertex colour tints them (steel, ink, hot white); solid pieces carry their own
dark ink outline in the greyscale.

Sprites: bullet (riveted casing, tip at the top), streak (brushed speed lines), bell (studded bell with a carved figure), ring (belt of
bolted plates, tiles in u), sigil (ground wheel with chain band), shard (atlas of 4 glassy shards, 64 px columns), chain (vertical chain
tile), burst (white impact star), ink (bold black-ink strokes, tinted dark), fist (studded gauntlet fist), halo (crown of spikes),
halftone (screentone cloud), flecks (glints).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


# ------------------------------------------------------------------------------------------------ helpers
def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, np.float32) / 255.0


def layer(w, h, fn):
    """Draw with fn(draw, s) at SS x resolution (s = SS), return a float 0..1 array at (h, w)."""
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    return np.asarray(im.resize((w, h), Image.LANCZOS), np.float32) / 255.0


def value_noise(w, h, cx, cy, seed):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, int(w // cx)), max(1, int(h // cy))
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    g[-1, :] = g[0, :]
    g[:, -1] = g[:, 0]
    xx, yy = grid(w, h)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, cell, seed, octaves=4, ax=1.0):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        c = max(2, cell >> o)
        n = n + value_noise(w, h, max(1, c * ax), c, seed + o * 7) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def grey(lum, alpha):
    l = np.clip(lum, 0, 255)
    a = np.clip(alpha, 0, 1) * 255
    return Image.fromarray(np.dstack([l, l, l, a]).astype(np.uint8), "RGBA")


def shade(m, seed, bevel=3.0, cyl=None, ink=1.0, base=150):
    """Steel look for a silhouette mask: bevel light from the upper left, brushed grain, stipple speckle, black ink outline."""
    h, w = m.shape
    hh = blur(m, bevel)
    gx, gy = np.gradient(hh, axis=1), np.gradient(hh, axis=0)
    sc = max(1e-6, float(np.abs(gx).max()), float(np.abs(gy).max()))
    light = np.clip((-gx * 0.75 - gy * 0.65) / sc, -1, 1)
    lum = base + 85 * light
    if cyl is not None:
        lum = lum + 50 * cyl
    brushed = fbm(w, h, 32, seed, 4, ax=6.0)
    lum = lum + (brushed - 0.5) * 60
    rng = np.random.default_rng(seed + 99)
    lum = lum + (rng.random((h, w)) - 0.5) * 26
    inner = smooth(0.55, 0.92, blur(m, max(1.2, w / 110)))
    edge = m * (1 - inner)
    lum = lum * (1 - 0.9 * edge * ink)
    return lum


def dark(lum, mask, k):
    return lum * (1 - np.clip(mask, 0, 1) * k)


def bright(lum, mask, v):
    mk = np.clip(mask, 0, 1)
    return lum * (1 - mk) + v * mk


def rivets(lum, w, h, pts, r):
    big = layer(w, h, lambda d, s: [d.ellipse([(x - r * 1.45) * s, (y - r * 1.45) * s, (x + r * 1.45) * s, (y + r * 1.45) * s], fill=255) for x, y in pts])
    dot = layer(w, h, lambda d, s: [d.ellipse([(x - r) * s, (y - r) * s, (x + r) * s, (y + r) * s], fill=255) for x, y in pts])
    hi = layer(w, h, lambda d, s: [d.ellipse([(x - r * 0.6) * s, (y - r * 0.7) * s, (x + r * 0.05) * s, (y - r * 0.05) * s], fill=255) for x, y in pts])
    lum = dark(lum, big, 0.75)
    lum = bright(lum, dot, 175)
    return bright(lum, hi, 250)


def cylinder(w, x0, x1):
    xx = (np.arange(w, dtype=np.float32) - (x0 + x1) / 2) / ((x1 - x0) / 2)
    xx = np.clip(xx, -1, 1)
    return (-xx * 0.8 + (1 - xx * xx) * 0.5)[None, :]


def rows_poly(hwfun, h, cx, y0, y1):
    """Polygon points of a symmetric silhouette given half-width per row."""
    ys = np.arange(y0, y1)
    left = [(cx - hwfun(y), y) for y in ys]
    right = [(cx + hwfun(y), y) for y in ys][::-1]
    return left + right


# ------------------------------------------------------------------------------------------------ sprites
def bullet():
    w, h = 128, 256
    c = 64

    def hw(y):
        if y < 96:
            return 40 * math.sqrt(max(0.0, 1 - ((96 - y) / 92.0) ** 2))
        if y < 206:
            return 40.0
        return 40 + (y - 206) * 0.28

    m = layer(w, h, lambda d, s: d.polygon([(x * s, y * s) for x, y in rows_poly(hw, h, c, 4, 252)], fill=255))
    # tail fins notch
    notch = layer(w, h, lambda d, s: [d.polygon([((c + sg * 26) * s, 232 * s), ((c + sg * 38) * s, 256 * s), ((c + sg * 14) * s, 256 * s)], fill=255) for sg in (-1, 1)])
    m = np.clip(m - notch, 0, 1)
    xx, _ = grid(w, h)
    cy = np.clip((xx - c) / 44.0, -1, 1)
    lum = shade(m, 11, 3.0, cyl=(-cy * 0.8 + (1 - cy * cy) * 0.5))
    bands = layer(w, h, lambda d, s: [d.rectangle([14 * s, y * s, 114 * s, (y + 5) * s], fill=255) for y in (112, 192)])
    lum = dark(lum, bands * m, 0.7)
    seam = layer(w, h, lambda d, s: d.line([(c * s, 120 * s), (c * s, 188 * s)], fill=255, width=3 * s))
    lum = dark(lum, seam, 0.5)
    pts = [(x, y) for y in (122, 182) for x in (30, 50, 78, 98)] + [(c, 60), (c, 80)]
    lum = rivets(lum, w, h, pts, 3.4)
    # nose highlight stripe
    st = layer(w, h, lambda d, s: d.line([(48 * s, 50 * s), (44 * s, 100 * s)], fill=255, width=4 * s))
    lum = bright(lum, blur(st, 1.2) * m, 245)
    return grey(lum, m)


def streak():
    w, h = 64, 256
    rng = np.random.default_rng(7)
    xx, yy = grid(w, h)
    a = np.zeros((h, w), np.float32)
    for i in range(18):
        x = rng.random() * w
        ln = 90 + rng.random() * 160
        wd = 0.7 + rng.random() * 1.8
        top = h - ln * (0.5 + 0.5 * rng.random())
        prof = np.exp(-((xx - x) / wd) ** 2) * smooth(top, top + ln * 0.6, yy) * (1 - 0.0 * yy)
        a = np.maximum(a, prof * (0.45 + 0.55 * rng.random()))
    core = np.exp(-((xx - 32) / 7.0) ** 2) * smooth(60, 250, yy)
    a = np.clip(np.maximum(a, core * 0.8), 0, 1) * smooth(0, 0.18 * 256, 256 - yy) * (1 - 0.0)
    a = a * (1 - np.exp(-((xx - 32) / 30.0) ** 6) * 0)  # keep
    a = a * smooth(0, 6, np.minimum(xx, w - 1 - xx) + 2)
    return grey(250 - 25 * fbm(w, h, 16, 3), a)


def bell():
    w, h = 256, 256
    c = 128
    ctrl = [(14, 0), (18, 9), (30, 14), (44, 16), (58, 40), (80, 66), (120, 84), (170, 98), (210, 108), (228, 114), (240, 124), (254, 126)]
    ys, hs = zip(*ctrl)

    def hw(y):
        return float(np.interp(y, ys, hs))

    m = layer(w, h, lambda d, s: d.polygon([(x * s, y * s) for x, y in rows_poly(hw, h, c, 14, 254)], fill=255))
    knob = layer(w, h, lambda d, s: d.ellipse([(c - 13) * s, 2 * s, (c + 13) * s, 34 * s], fill=255))
    m = np.clip(m + knob, 0, 1)
    xx, yy = grid(w, h)
    widthrow = np.interp(yy, ys, hs) + 1
    cy = np.clip((xx - c) / widthrow, -1, 1)
    lum = shade(m, 21, 4.0, cyl=(-cy * 0.8 + (1 - cy * cy) * 0.5), base=140)
    # shoulder rim, lip bands
    bands = layer(w, h, lambda d, s: [d.rectangle([0, y * s, w * s, (y + t) * s], fill=255) for y, t in ((60, 4), (74, 3), (206, 5), (224, 4), (244, 6))])
    lum = dark(lum, bands * m, 0.75)
    hi = layer(w, h, lambda d, s: [d.rectangle([0, (y + t) * s, w * s, (y + t + 2) * s], fill=255) for y, t in ((60, 4), (206, 5), (224, 4))])
    lum = bright(lum, hi * m, 235)
    # vertical panel seams
    seams = layer(w, h, lambda d, s: [d.line([((c + k) * s, 80 * s), ((c + k * 1.12) * s, 204 * s)], fill=255, width=3 * s) for k in (-40, 40)])
    lum = dark(lum, seams * m, 0.7)
    # studs (nyu): 3 x 3 in each side panel
    pts = []
    for sg in (-1, 1):
        for j in range(3):
            for i in range(3):
                y = 100 + j * 34
                pts.append((c + sg * (62 + i * 20) * (0.8 + 0.2 * (y - 90) / 100.0), y))
    lum = rivets(lum, w, h, pts, 5.0)
    # carved seated figure in the centre panel: halo ring, head, shoulders, crossed legs
    def fig(d, s):
        d.ellipse([(c - 30) * s, 92 * s, (c + 30) * s, 152 * s], outline=255, width=3 * s)
        d.ellipse([(c - 9) * s, 106 * s, (c + 9) * s, 124 * s], fill=255)
        d.polygon([((c - 22) * s, 150 * s), ((c - 12) * s, 126 * s), ((c + 12) * s, 126 * s), ((c + 22) * s, 150 * s)], fill=255)
        d.ellipse([(c - 32) * s, 148 * s, (c + 32) * s, 168 * s], fill=255)
        d.ellipse([(c - 5) * s, 108 * s, (c + 5) * s, 112 * s], fill=0)
    fg = layer(w, h, fig)
    lum = dark(lum, fg * m, 0.62)
    edge = np.clip(fg - np.roll(fg, 2, axis=0), 0, 1)
    lum = bright(lum, edge * m, 240)
    return grey(lum, m)


def ring():
    w, h = 512, 64
    m = layer(w, h, lambda d, s: [d.rounded_rectangle([(i * 64 + 3) * s, 6 * s, (i * 64 + 61) * s, 58 * s], radius=5 * s, fill=255) for i in range(8)])
    lum = shade(m, 31, 2.6)
    pts = [(i * 64 + x, y) for i in range(8) for x in (13, 51) for y in (16, 48)]
    lum = rivets(lum, w, h, pts, 3.2)
    mid = layer(w, h, lambda d, s: [d.line([((i * 64 + 18) * s, 32 * s), ((i * 64 + 46) * s, 32 * s)], fill=255, width=2 * s) for i in range(8)])
    lum = dark(lum, mid * m, 0.6)
    return grey(lum, m)


def sigil():
    n = 256
    c = n / 2

    def draw(d, s):
        def circ(r, wd):
            d.ellipse([(c - r) * s, (c - r) * s, (c + r) * s, (c + r) * s], outline=255, width=int(wd * s))
        circ(124, 4)
        circ(115, 2)
        circ(78, 3)
        circ(54, 2)
        for i in range(36):
            a = math.tau * i / 36
            d.line([((c + math.cos(a) * 117) * s, (c + math.sin(a) * 117) * s), ((c + math.cos(a) * 123) * s, (c + math.sin(a) * 123) * s)], fill=255, width=2 * s)
        # chain band: interlocked links
        for i in range(24):
            a = math.tau * i / 24
            px, py = c + math.cos(a) * 96, c + math.sin(a) * 96
            ca, sa = math.cos(a + math.pi / 2), math.sin(a + math.pi / 2)
            pts = []
            for k in range(0, 24):
                t = math.tau * k / 24
                ex, ey = math.cos(t) * 11.5, math.sin(t) * 6
                pts.append(((px + ca * ex - sa * ey) * s, (py + sa * ex + ca * ey) * s))
            d.line(pts + [pts[0]], fill=255, width=int(2.2 * s))
        # two interlaced squares (guard seal)
        for rot in (0, math.pi / 4):
            q = [((c + math.cos(rot + math.pi / 2 * k + math.pi / 4) * 76) * s, (c + math.sin(rot + math.pi / 2 * k + math.pi / 4) * 76) * s) for k in range(4)]
            d.line(q + [q[0]], fill=255, width=2 * s)
        # dharma wheel: 8 spokes with trident tips
        for i in range(8):
            a = math.tau * i / 8
            ca, sa = math.cos(a), math.sin(a)
            d.line([((c + ca * 14) * s, (c + sa * 14) * s), ((c + ca * 50) * s, (c + sa * 50) * s)], fill=255, width=3 * s)
            tip = [((c + ca * 62) * s, (c + sa * 62) * s), ((c + ca * 46 - sa * 7) * s, (c + sa * 46 + ca * 7) * s), ((c + ca * 46 + sa * 7) * s, (c + sa * 46 - ca * 7) * s)]
            d.polygon(tip, fill=255)
        d.ellipse([(c - 14) * s, (c - 14) * s, (c + 14) * s, (c + 14) * s], outline=255, width=3 * s)
        d.ellipse([(c - 6) * s, (c - 6) * s, (c + 6) * s, (c + 6) * s], fill=255)

    a = layer(n, n, draw)
    gl = blur(a, 3.0)
    alpha = np.clip(a + gl * 0.55, 0, 1)
    xx, yy = grid(n, n)
    r = np.hypot(xx - c, yy - c) / c
    alpha *= 1 - smooth(0.96, 1.0, r)
    return grey(215 + 40 * a + 20 * fbm(n, n, 24, 41), alpha)


def shard():
    w, h = 256, 128
    rng = np.random.default_rng(55)
    shapes = []
    for i in range(4):
        x0 = i * 64
        cxs = x0 + 32
        top = 6 + rng.random() * 8
        bot = 118 - rng.random() * 6
        wl, wr = 12 + rng.random() * 12, 10 + rng.random() * 12
        k = 0.45 + rng.random() * 0.25
        pts = [(cxs + (rng.random() - 0.5) * 6, top), (cxs + wr, top + (bot - top) * k * 0.7), (cxs + wr * 0.6, top + (bot - top) * 0.82),
               (cxs - 2 + (rng.random() - 0.5) * 4, bot), (cxs - wl * 0.7, top + (bot - top) * 0.75), (cxs - wl, top + (bot - top) * k)]
        shapes.append(pts)

    def draw(d, s):
        for pts in shapes:
            d.polygon([(x * s, y * s) for x, y in pts], fill=255)
    m = layer(w, h, draw)
    ed = m - blur(m, 1.4)
    edge = np.clip(ed * 3.0 + 0.0, 0, 1) * m
    xx, yy = grid(w, h)
    facet = np.zeros_like(m)

    def facets(d, s):
        for pts in shapes:
            tip = pts[0]
            for p in (pts[2], pts[4], pts[1]):
                d.line([(tip[0] * s, tip[1] * s), (p[0] * s, p[1] * s)], fill=255, width=2 * s)
    fc = layer(w, h, facets)
    alpha = np.clip(m * 0.55 + edge * 0.5 + fc * 0.45 * m, 0, 1)
    lum = 205 + 50 * fbm(w, h, 16, 5) + 40 * edge
    return grey(lum, alpha)


def chain():
    w, h = 64, 128

    def draw(d, s):
        for dy in (-128, 0, 128):
            cy = 32 + dy
            d.ellipse([(32 - 22) * s, (cy - 34) * s, (32 + 22) * s, (cy + 34) * s], outline=255, width=9 * s)
            cy2 = 96 + dy
            d.rounded_rectangle([(32 - 7) * s, (cy2 - 36) * s, (32 + 7) * s, (cy2 + 36) * s], radius=7 * s, fill=255)
    m = layer(w, h, draw)
    # the face link passes over the edge link: knock a gap into the edge link where they cross
    lum = shade(m, 61, 2.0, ink=1.0, base=160)
    return grey(lum, m)


def burst():
    n = 256
    c = n / 2
    rng = np.random.default_rng(81)

    def draw(d, s):
        k = 22
        pts = []
        for i in range(k * 2):
            a = math.tau * i / (k * 2) + (rng.random() - 0.5) * 0.04
            r = (c * (0.55 + 0.45 * rng.random()) if i % 2 == 0 else c * (0.16 + 0.1 * rng.random()))
            pts.append(((c + math.cos(a) * r) * s, (c + math.sin(a) * r) * s))
        d.polygon(pts, fill=255)
        for i in range(40):
            a = rng.random() * math.tau
            r0, r1 = c * (0.35 + 0.2 * rng.random()), c * (0.8 + 0.2 * rng.random())
            d.line([((c + math.cos(a) * r0) * s, (c + math.sin(a) * r0) * s), ((c + math.cos(a) * r1) * s, (c + math.sin(a) * r1) * s)], fill=255, width=int(1 * s))
    a = layer(n, n, draw)
    xx, yy = grid(n, n)
    r = np.hypot(xx - c, yy - c) / c
    core = np.exp(-(r / 0.28) ** 2)
    alpha = np.clip(a * (0.65 + 0.35 * core) + core * 0.6, 0, 1) * (1 - smooth(0.9, 1.0, r))
    return grey(235 + 20 * core, alpha)


def ink():
    n = 256
    c = n / 2
    rng = np.random.default_rng(97)

    def draw(d, s):
        # bold tapered wedges radiating from the centre, with jagged bolts between them
        for i in range(14):
            a = math.tau * i / 14 + (rng.random() - 0.5) * 0.18
            r0, r1 = 24 + rng.random() * 26, c * (0.7 + 0.3 * rng.random())
            wd = 0.05 + rng.random() * 0.06
            d.polygon([((c + math.cos(a - wd) * r0) * s, (c + math.sin(a - wd) * r0) * s), ((c + math.cos(a) * r1) * s, (c + math.sin(a) * r1) * s),
                       ((c + math.cos(a + wd) * r0) * s, (c + math.sin(a + wd) * r0) * s)], fill=255)
        for b in range(5):
            a = math.tau * (b + rng.random() * 0.5) / 5
            x, y = c + math.cos(a) * 30, c + math.sin(a) * 30
            pts = [(x * s, y * s)]
            for k in range(1, 6):
                r = 30 + k * 18
                jit = (k % 2 * 2 - 1) * (7 + rng.random() * 5)
                x, y = c + math.cos(a) * r - math.sin(a) * jit, c + math.sin(a) * r + math.cos(a) * jit
                pts.append((x * s, y * s))
            d.line(pts, fill=255, width=int(4 * s))
    a = layer(n, n, draw)
    xx, yy = grid(n, n)
    r = np.hypot(xx - c, yy - c) / c
    a = a * (1 - smooth(0.85, 1.0, r)) * smooth(0.06, 0.2, r)
    return grey(255 - 18 * fbm(n, n, 20, 9), a)


def fist():
    w, h = 256, 256

    def draw(d, s):
        d.rounded_rectangle([60 * s, 84 * s, 206 * s, 206 * s], radius=22 * s, fill=255)
        for k in range(4):
            x = 74 + k * 36
            d.ellipse([(x - 22) * s, 56 * s, (x + 22) * s, 108 * s], fill=255)
        d.ellipse([28 * s, 118 * s, 96 * s, 204 * s], fill=255)               # thumb
        d.rounded_rectangle([86 * s, 196 * s, 180 * s, 256 * s], radius=8 * s, fill=255)   # wrist
    m = layer(w, h, draw)
    xx, yy = grid(w, h)
    cyl = np.clip(-((xx - 133) / 90.0) * 0.6 + 0.2, -1, 1)
    lum = shade(m, 71, 4.5, cyl=cyl, base=150)
    creases = layer(w, h, lambda d, s: [(d.line([((74 + k * 36 - 17) * s, 128 * s), ((74 + k * 36 + 17) * s, 128 * s)], fill=255, width=4 * s),
                                          d.line([((74 + k * 36 - 15) * s, 158 * s), ((74 + k * 36 + 15) * s, 158 * s)], fill=255, width=4 * s)) for k in range(4)])
    lum = dark(lum, creases * m, 0.75)
    cuff = layer(w, h, lambda d, s: [d.rectangle([86 * s, 206 * s, 180 * s, 212 * s], fill=255), d.rectangle([86 * s, 238 * s, 180 * s, 243 * s], fill=255)])
    lum = dark(lum, cuff * m, 0.7)
    sp = layer(w, h, lambda d, s: [d.polygon([((74 + k * 36 - 7) * s, 80 * s), ((74 + k * 36) * s, 56 * s), ((74 + k * 36 + 7) * s, 80 * s)], fill=255) for k in range(4)])
    lum = bright(lum, sp * 0.0 + blur(sp, 0.8), 215)
    pts = [(74 + k * 36, 82) for k in range(4)] + [(100 + 20 * i, 224) for i in range(4)] + [(62, 150), (70, 176)]
    lum = rivets(lum, w, h, pts, 4.2)
    return grey(lum, np.clip(m + blur(sp, 0.8) * 0.0, 0, 1))


def halo():
    n = 256
    c = n / 2

    def draw(d, s):
        for r, wd in ((86, 5), (64, 2), (100, 2)):
            d.ellipse([(c - r) * s, (c - r) * s, (c + r) * s, (c + r) * s], outline=255, width=int(wd * s))
        for i in range(24):
            a = math.tau * i / 24
            ln = 124 if i % 2 == 0 else 108
            wd = 0.065 if i % 2 == 0 else 0.05
            d.polygon([((c + math.cos(a - wd) * 90) * s, (c + math.sin(a - wd) * 90) * s), ((c + math.cos(a) * ln) * s, (c + math.sin(a) * ln) * s),
                       ((c + math.cos(a + wd) * 90) * s, (c + math.sin(a + wd) * 90) * s)], fill=255)
        for i in range(48):
            a = math.tau * (i + 0.5) / 48
            d.line([((c + math.cos(a) * 66) * s, (c + math.sin(a) * 66) * s), ((c + math.cos(a) * 80) * s, (c + math.sin(a) * 80) * s)], fill=255, width=2 * s)
        d.ellipse([(c - 7) * s, (c - 7) * s, (c + 7) * s, (c + 7) * s], fill=255)
    a = layer(n, n, draw)
    xx, yy = grid(n, n)
    r = np.hypot(xx - c, yy - c) / c
    alpha = np.clip(a + blur(a, 3.5) * 0.7 + np.exp(-(r / 0.5) ** 2) * 0.12, 0, 1) * (1 - smooth(0.93, 1.0, r))
    return grey(235 + 20 * a, alpha)


def halftone():
    n = 128
    xx, yy = grid(n, n)
    c = (n - 1) / 2
    r = np.hypot(xx - c, yy - c) / c
    u = (xx + yy) / math.sqrt(2)
    v = (xx - yy) / math.sqrt(2)
    sp = 7.0
    du = (u / sp) % 1 - 0.5
    dv = (v / sp) % 1 - 0.5
    d = np.hypot(du, dv)
    f = np.clip(1 - r, 0, 1) ** 0.8 * (0.8 + 0.5 * fbm(n, n, 16, 13))
    rad = 0.52 * np.clip(f, 0, 1)
    a = smooth(rad + 0.06, rad - 0.06, d) * (f > 0.04)
    a = a * (1 - smooth(0.85, 1.0, r))
    return grey(255 - 25 * fbm(n, n, 8, 5), a)


def flecks():
    n = 128
    rng = np.random.default_rng(171)
    xx, yy = grid(n, n)
    a = np.zeros((n, n), np.float32)
    for i in range(34):
        x, y = rng.random() * n, rng.random() * n
        sz = 1.2 + rng.random() * 3.6
        if i % 3 == 0:  # 4-point glint
            g = np.exp(-(((xx - x) / (sz * 0.35)) ** 2 + ((yy - y) / (sz * 3.2)) ** 2)) + np.exp(-(((xx - x) / (sz * 3.2)) ** 2 + ((yy - y) / (sz * 0.35)) ** 2))
            g = g + np.exp(-(((xx - x) ** 2 + (yy - y) ** 2) / (sz * 0.6) ** 2)) * 1.2
        else:
            g = np.exp(-(((xx - x) ** 2 + (yy - y) ** 2) / (sz * 0.7) ** 2))
        a = np.maximum(a, g)
    return grey(250 + 0 * a, np.clip(a, 0, 1))


if __name__ == "__main__":
    save(bullet(), "iron_bullet")
    save(streak(), "iron_streak")
    save(bell(), "iron_bell")
    save(ring(), "iron_ring")
    save(sigil(), "iron_sigil")
    save(shard(), "iron_shard")
    save(chain(), "iron_chain")
    save(burst(), "iron_burst")
    save(ink(), "iron_ink")
    save(fist(), "iron_fist")
    save(halo(), "iron_halo")
    save(halftone(), "iron_halftone")
    save(flecks(), "iron_flecks")

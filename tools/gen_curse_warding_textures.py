"""Generates the Curse-Warding Magic VFX textures (pale-violet warding runes, silver seals, broken curse rings).

Deterministic (fixed seeds, no fonts). Output: src/main/resources/assets/nusmp/textures/particle/curse_warding_*.png
White / grey with alpha so the vertex colour tints them.

    python3 -B tools/gen_curse_warding_textures.py

Sprites:
  sigil        256  ground ward seal: rings, tick marks, rune ring, hexagram, central lens
  rune_band    512x32  a ribbon of angular warding runes between two rules (wraps, 16 runes)
  strand       64x256  braided rune thread (trail / curtain)
  lance        64x256  ward lance body, tip at the TOP of the image
  star         128  eight-point ward star with a ring (charge flash, projectile head)
  broken_ring  256  a seal ring snapped into three arcs with thorn tips (shockwave, gather ring)
  shard        128  faceted fragment of a broken ward
  hex          256  hexagonal ward lattice disc with lit cells (field ceiling / floor)
  burst        256  irregular radial rays with a hot core (impact flash)
  rune_a / rune_b  128  one big glowing rune each (rising / flying runes)
  mote         64   four-point sparkle
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


# ------------------------------------------------------------------------------------------------ helpers
def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, dtype=np.float32) / 255.0


def canvas(w, h):
    im = Image.new("L", (w * SS, h * SS), 0)
    return im, ImageDraw.Draw(im)


def down(im, w, h):
    return np.asarray(im.resize((w, h), Image.LANCZOS), dtype=np.float32) / 255.0


def save(mask, name, lum=None):
    mask = np.clip(mask, 0, 1)
    h, w = mask.shape
    if lum is None:
        lum = np.ones_like(mask)
    rgb = (np.clip(lum, 0, 1) * 255).astype(np.uint8)
    a = (mask * 255).astype(np.uint8)
    img = np.dstack([rgb, rgb, rgb, a])
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT, "curse_warding_" + name + ".png"), optimize=True)
    print("wrote curse_warding_" + name, (w, h))


def glow_add(mask, radius, k):
    """Crisp mask plus a soft halo around it."""
    return np.clip(mask + (1 - mask) * blur(mask, radius) * k, 0, 1)


def line(d, p0, p1, w, fill=255):
    d.line([p0, p1], fill=fill, width=max(1, int(w)))
    r = w / 2
    for p in (p0, p1):
        d.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=fill)


def circle(d, c, r, w, fill=255):
    d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], outline=fill, width=max(1, int(w)))


def disc(d, c, r, fill=255):
    d.ellipse([c[0] - r, c[1] - r, c[0] + r, c[1] + r], fill=fill)


def polar(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    x = (x + 0.5) / w * 2 - 1
    y = (y + 0.5) / h * 2 - 1
    return np.hypot(x, y), np.arctan2(y, x), x, y


# ------------------------------------------------------------------------------------------------ runes
GRID = [(0, 0), (2, 0), (0, 2), (2, 2), (0, 4), (2, 4), (1, 1), (1, 3), (1, 0), (1, 4), (1, 2)]


def glyph(seed):
    """An angular rune: a stem (sometimes broken) plus 2 to 4 diagonal / branch strokes. Coordinates x -0.5..0.5, y -1..1."""
    rng = np.random.default_rng(seed)
    segs = []
    if rng.random() < 0.8:
        segs.append(((1, 0), (1, 4)))
    else:
        segs += [((1, 0), (1, 1.6)), ((1, 2.4), (1, 4))]
    for _ in range(int(rng.integers(4, 8))):
        if len(segs) >= 5:
            break
        a, b = rng.choice(len(GRID), 2, replace=False)
        p, q = GRID[a], GRID[b]
        if p == q or abs(p[0] - q[0]) + abs(p[1] - q[1]) < 1.5:
            continue
        segs.append((p, q))
    dot = (1, 2) if rng.random() < 0.3 else None
    return segs, dot


def put_glyph(d, g, cx, cy, half_h, ang, w, fill=255):
    segs, dot = g
    ca, sa = math.cos(ang), math.sin(ang)

    def tf(p):
        x, y = (p[0] - 1) * 0.5 * half_h, (p[1] - 2) * 0.5 * half_h
        return (cx + x * ca - y * sa, cy + x * sa + y * ca)

    for p, q in segs:
        line(d, tf(p), tf(q), w, fill)
    if dot:
        c = tf((0.2, 2))
        disc(d, c, w * 1.1, fill)


def ring_poly(d, c, r_in, r_out, a0, a1, fill=255, steps=48):
    pts = []
    for i in range(steps + 1):
        a = a0 + (a1 - a0) * i / steps
        pts.append((c[0] + math.cos(a) * r_out, c[1] + math.sin(a) * r_out))
    for i in range(steps, -1, -1):
        a = a0 + (a1 - a0) * i / steps
        pts.append((c[0] + math.cos(a) * r_in, c[1] + math.sin(a) * r_in))
    d.polygon(pts, fill=fill)


# ------------------------------------------------------------------------------------------------ textures
def gen_sigil():
    N = 256
    im, d = canvas(N, N)
    S = N * SS
    c = (S / 2, S / 2)
    R = S * 0.49
    circle(d, c, R * 0.99, S * 0.014)
    circle(d, c, R * 0.93, S * 0.005)
    for i in range(120):                                     # dotted ring
        a = i / 120 * math.tau
        disc(d, (c[0] + math.cos(a) * R * 0.96, c[1] + math.sin(a) * R * 0.96), S * 0.0045)
    for i in range(72):                                      # tick marks
        a = i / 72 * math.tau
        long = i % 6 == 0
        r0, r1 = R * (0.80 if long else 0.84), R * 0.89
        line(d, (c[0] + math.cos(a) * r0, c[1] + math.sin(a) * r0), (c[0] + math.cos(a) * r1, c[1] + math.sin(a) * r1),
             S * (0.006 if long else 0.003))
    circle(d, c, R * 0.78, S * 0.008)
    circle(d, c, R * 0.60, S * 0.008)
    for i in range(16):                                      # rune ring
        a = i / 16 * math.tau
        put_glyph(d, glyph(100 + i), c[0] + math.cos(a) * R * 0.69, c[1] + math.sin(a) * R * 0.69, S * 0.036, a + math.pi / 2, S * 0.0075)
    for k in range(2):                                       # hexagram
        pts = [(c[0] + math.cos(math.pi / 2 + k * math.pi / 3 + j * math.tau / 3) * R * 0.57,
                c[1] + math.sin(math.pi / 2 + k * math.pi / 3 + j * math.tau / 3) * R * 0.57) for j in range(3)]
        for j in range(3):
            line(d, pts[j], pts[(j + 1) % 3], S * 0.007)
    for j in range(6):
        a = math.pi / 2 + j * math.tau / 6
        disc(d, (c[0] + math.cos(a) * R * 0.57, c[1] + math.sin(a) * R * 0.57), S * 0.012)
        circle(d, (c[0] + math.cos(a) * R * 0.57, c[1] + math.sin(a) * R * 0.57), S * 0.024, S * 0.004)
    circle(d, c, R * 0.30, S * 0.007)
    circle(d, c, R * 0.27, S * 0.003)
    for j in range(6):                                       # inner spokes
        a = math.pi / 2 + j * math.tau / 6 + math.pi / 6
        line(d, (c[0] + math.cos(a) * R * 0.12, c[1] + math.sin(a) * R * 0.12), (c[0] + math.cos(a) * R * 0.27, c[1] + math.sin(a) * R * 0.27), S * 0.005)
    # the central lens (a ward eye): two arcs meeting at points
    top = [(c[0] + x * R * 0.20, c[1] - (1 - x * x) * R * 0.09) for x in np.linspace(-1, 1, 30)]
    bot = [(c[0] + x * R * 0.20, c[1] + (1 - x * x) * R * 0.09) for x in np.linspace(-1, 1, 30)]
    for pts in (top, bot):
        for i in range(len(pts) - 1):
            line(d, pts[i], pts[i + 1], S * 0.006)
    disc(d, c, S * 0.022)
    m = down(im, N, N)
    save(glow_add(m, 1.4, 0.55), "sigil", 0.78 + 0.22 * blur(m, 3))


def gen_rune_band():
    W, H = 512, 32
    im, d = canvas(W, H)
    for i in range(16):
        put_glyph(d, glyph(300 + i), (i + 0.5) * 32 * SS, 16 * SS, 9.5 * SS, 0, 1.7 * SS)
    d.rectangle([0, 3 * SS, W * SS, 3.9 * SS], fill=255)
    d.rectangle([0, 28.1 * SS, W * SS, 29 * SS], fill=255)
    for i in range(32):
        disc(d, ((i + 0.5) * 16 * SS, 1.2 * SS), 0.55 * SS)
        disc(d, ((i + 0.5) * 16 * SS, 30.8 * SS), 0.55 * SS)
    m = down(im, W, H)
    save(glow_add(m, 0.8, 0.6), "rune_band", 0.8 + 0.2 * blur(m, 1.2))


def gen_strand():
    W, H = 64, 256
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    x = (x + 0.5) / W * 2 - 1
    t = y / H * math.tau * 4
    m = np.zeros((H, W), np.float32)
    lum = np.zeros((H, W), np.float32)
    for ph, base in ((0.0, 0.85), (math.pi, 0.7)):
        cx = 0.42 * np.sin(t + ph)
        depth = np.cos(t + ph)
        wid = 0.15 + 0.04 * depth
        th = np.exp(-((x - cx) / wid) ** 4)
        m = np.maximum(m, th * (0.75 + 0.25 * (depth > 0)))
        lum = np.maximum(lum, th * (base + 0.15 * depth))
    core = np.exp(-(x / 0.18) ** 2) * 0.25
    # little rune ticks along the strand
    ticks = np.zeros((H, W), np.float32)
    for k in range(8):
        yy = (k + 0.5) / 8 * H
        ticks += np.exp(-((y - yy) / 1.6) ** 2) * np.exp(-((np.abs(x) - 0.62) / 0.12) ** 2) * 0.8
    m = np.clip(m + core + ticks, 0, 1)
    m = m * np.exp(-(np.abs(x) / 0.98) ** 6)
    save(glow_add(m, 1.3, 0.5), "strand", np.clip(0.55 + lum * 0.5 + ticks * 0.3, 0, 1))


def gen_lance():
    W, H = 64, 256
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    x = (x + 0.5) / W * 2 - 1
    v = y / H                                            # 0 tip .. 1 tail
    # spindle: sharp tip, fat shoulder at v=0.3, tapering into a soft tail
    shoulder = np.where(v < 0.3, np.sin(np.clip(v / 0.3, 0, 1) * math.pi / 2) ** 1.4, np.exp(-((v - 0.3) / 0.55) ** 2) ** 0.8)
    wid = 0.62 * shoulder + 0.02
    body = np.clip(1 - (np.abs(x) / wid) ** 2.2, 0, 1) * np.clip(v / 0.01, 0, 1)
    spine = np.exp(-(x / 0.07) ** 2) * (1 - v * 0.9)
    edge = np.exp(-((np.abs(x) / wid - 0.92) / 0.08) ** 2) * (v < 0.7) * 0.6
    barb = np.zeros_like(x)
    for k in range(5):                                   # rune barbs / fletching
        vv = 0.34 + k * 0.1
        barb += np.exp(-((v - vv - np.abs(x) * 0.35) / 0.012) ** 2) * (np.abs(x) < wid * 1.05) * (1 - k * 0.12)
    tail = np.clip((1 - v) ** 1.5, 0, 1)
    m = np.clip(body * 0.55 + spine + edge * body + barb * 0.5, 0, 1) * tail
    save(glow_add(m, 1.4, 0.5), "lance", np.clip(0.6 + spine * 0.4 + edge * 0.3, 0, 1))


def gen_star():
    N = 128
    r, a, _, _ = polar(N, N)
    rr = np.maximum(r, 1e-3)
    long = np.abs(np.cos(2 * a)) ** 36 * np.exp(-rr * 2.6)
    short = np.abs(np.sin(2 * a)) ** 36 * np.exp(-rr * 5.5) * 0.7
    diag = (np.abs(np.cos(4 * a)) ** 80) * np.exp(-rr * 7) * 0.35
    core = np.exp(-(r / 0.085) ** 2)
    halo = np.exp(-(r / 0.28) ** 2) * 0.35
    ringr = np.exp(-((r - 0.40) / 0.014) ** 2) * 0.75 + np.exp(-((r - 0.46) / 0.008) ** 2) * 0.4
    ticks = ringr * (np.abs(np.cos(8 * a)) ** 6 * 0.5 + 0.5)
    m = np.clip(long + short + diag + core + halo + ticks, 0, 1) * np.clip((1 - r) * 5, 0, 1)
    save(m, "star", np.clip(0.7 + core * 0.3, 0, 1))


def gen_broken_ring():
    N = 256
    im, d = canvas(N, N)
    S = N * SS
    c = (S / 2, S / 2)
    rng = np.random.default_rng(77)
    arcs = [(0.15, 2.0), (2.35, 4.05), (4.45, 6.0)]
    for a0, a1 in arcs:
        steps = 60
        outer, inner = [], []
        for i in range(steps + 1):
            u = i / steps
            a = a0 + (a1 - a0) * u
            taper = math.sin(math.pi * u) ** 0.6
            n = 1 + 0.012 * math.sin(a * 9) + rng.uniform(-0.004, 0.004)
            ro = S * 0.45 * n + S * 0.02 * taper
            ri = S * 0.45 * n - S * 0.035 * taper
            outer.append((c[0] + math.cos(a) * ro, c[1] + math.sin(a) * ro))
            inner.append((c[0] + math.cos(a) * ri, c[1] + math.sin(a) * ri))
        d.polygon(outer + inner[::-1], fill=255)
        for a in (a0, a1):                                   # thorn tips at the broken ends
            tip = (c[0] + math.cos(a + (0.1 if a == a1 else -0.1)) * S * 0.495, c[1] + math.sin(a + (0.1 if a == a1 else -0.1)) * S * 0.495)
            b1 = (c[0] + math.cos(a) * S * 0.46, c[1] + math.sin(a) * S * 0.46)
            b2 = (c[0] + math.cos(a) * S * 0.41, c[1] + math.sin(a) * S * 0.41)
            d.polygon([tip, b1, b2], fill=255)
    circle(d, c, S * 0.40, S * 0.004, 170)
    for i in range(24):                                      # dim inner ticks
        a = i / 24 * math.tau + 0.1
        line(d, (c[0] + math.cos(a) * S * 0.36, c[1] + math.sin(a) * S * 0.36), (c[0] + math.cos(a) * S * 0.385, c[1] + math.sin(a) * S * 0.385), S * 0.003, 150)
    for i in range(3):
        put_glyph(d, glyph(500 + i), c[0] + math.cos(1.1 + i * 2.1) * S * 0.45, c[1] + math.sin(1.1 + i * 2.1) * S * 0.45, S * 0.022, 1.1 + i * 2.1 + math.pi / 2, S * 0.004, 40)
    m = down(im, N, N)
    save(glow_add(m, 1.6, 0.6), "broken_ring", 0.75 + 0.25 * blur(m, 2.5))


def gen_shard():
    N = 128
    im, d = canvas(N, N)
    S = N * SS
    pts = [(0.50 * S, 0.04 * S), (0.76 * S, 0.62 * S), (0.60 * S, 0.96 * S), (0.30 * S, 0.80 * S), (0.24 * S, 0.40 * S)]
    mid = (0.50 * S, 0.52 * S)
    shades = [235, 150, 205, 120, 255]
    lum_im = Image.new("L", (S, S), 0)
    ld = ImageDraw.Draw(lum_im)
    for i in range(5):
        tri = [mid, pts[i], pts[(i + 1) % 5]]
        d.polygon(tri, fill=255)
        ld.polygon(tri, fill=shades[i])
    for i in range(5):
        line(ld, pts[i], pts[(i + 1) % 5], S * 0.02, 255)
        line(ld, mid, pts[i], S * 0.008, 255)
    line(ld, pts[0], (0.46 * S, 0.7 * S), S * 0.006, 255)       # a crack
    m = down(im, N, N)
    lum = down(lum_im, N, N)
    save(np.clip(m, 0, 1) * 0.92 + glow_add(m, 1.2, 0.4) * 0.08, "shard", np.clip(lum * 1.05, 0.35, 1))


def gen_hex():
    N = 256
    r, a, x, y = polar(N, N)
    s = 0.105
    q = (math.sqrt(3) / 3 * x - y / 3) / s
    rr = (2 / 3 * y) / s
    cx_, cy_, cz_ = q, -q - rr, rr
    rx_, ry_, rz_ = np.round(cx_), np.round(cy_), np.round(cz_)
    dx, dy, dz = np.abs(rx_ - cx_), np.abs(ry_ - cy_), np.abs(rz_ - cz_)
    rx_ = np.where((dx > dy) & (dx > dz), -ry_ - rz_, rx_)
    rz_ = np.where(~((dx > dy) & (dx > dz)) & ~(dy > dz), -rx_ - ry_, rz_)
    qx, ryy = rx_, rz_
    cxp = s * (math.sqrt(3) * qx + math.sqrt(3) / 2 * ryy)
    cyp = s * 1.5 * ryy
    ox, oy = x - cxp, y - cyp
    ap = s * math.sqrt(3) / 2
    hd = np.maximum.reduce([np.abs(ox), np.abs(ox * 0.5 + oy * math.sqrt(3) / 2), np.abs(ox * 0.5 - oy * math.sqrt(3) / 2)])
    edge = np.clip(1 - (ap - hd) / 0.006, 0, 1)
    rng = np.random.default_rng(9)
    cell = (np.sin(qx * 12.9898 + ryy * 78.233) * 43758.5453) % 1.0
    lit = np.where(cell > 0.72, 0.30 + 0.25 * cell, 0.0) * np.clip((ap - hd) / 0.02, 0, 1)
    env = np.clip((0.93 - r) / 0.06, 0, 1)
    rim = np.exp(-((r - 0.955) / 0.012) ** 2) + np.exp(-((r - 0.915) / 0.005) ** 2) * 0.6
    m = np.clip((edge * 0.75 + lit) * env * (0.45 + 0.55 * (1 - r)) + rim, 0, 1)
    save(glow_add(m, 1.0, 0.4), "hex", 0.7 + 0.3 * edge)


def gen_burst():
    N = 256
    r, a, _, _ = polar(N, N)
    rng = np.random.default_rng(31)
    m = np.zeros_like(r)
    for i in range(46):
        ang = rng.uniform(-math.pi, math.pi)
        wid = rng.uniform(0.012, 0.05)
        ln = rng.uniform(0.35, 0.98)
        da = np.abs(np.angle(np.exp(1j * (a - ang))))
        ray = np.exp(-(da / (wid * (1.4 - np.clip(r / ln, 0, 1)))) ** 2) * np.clip(1 - r / ln, 0, 1) ** 1.3 * rng.uniform(0.5, 1.0)
        m = np.maximum(m, ray)
    core = np.exp(-(r / 0.10) ** 2)
    halo = np.exp(-(r / 0.30) ** 2) * 0.45
    ringr = np.exp(-((r - 0.52) / 0.012) ** 2) * 0.5 * (0.5 + 0.5 * np.cos(a * 7))
    out = np.clip(m + core + halo + ringr, 0, 1) * np.clip((1 - r) * 6, 0, 1)
    save(out, "burst", np.clip(0.65 + core * 0.35 + m * 0.2, 0, 1))


def gen_rune(name, seed):
    N = 128
    im, d = canvas(N, N)
    S = N * SS
    c = (S / 2, S / 2)
    g = glyph(seed)
    put_glyph(d, g, c[0], c[1], S * 0.34, 0, S * 0.055)
    im2, d2 = canvas(N, N)
    put_glyph(d2, g, c[0], c[1], S * 0.34, 0, S * 0.022)
    circle(d, c, S * 0.46, S * 0.012, 200)
    for i in range(4):                                       # bracket ticks
        a = i * math.pi / 2 + math.pi / 4
        line(d, (c[0] + math.cos(a) * S * 0.40, c[1] + math.sin(a) * S * 0.40), (c[0] + math.cos(a) * S * 0.46, c[1] + math.sin(a) * S * 0.46), S * 0.012)
    m = down(im, N, N)
    hot = down(im2, N, N)
    save(glow_add(m, 3.0, 0.9), name, np.clip(0.62 + hot * 0.38, 0, 1))


def gen_mote():
    N = 64
    r, a, x, y = polar(N, N)
    cross = np.exp(-(np.abs(x) / 0.05) ** 2) * np.exp(-np.abs(y) * 2.6) + np.exp(-(np.abs(y) / 0.05) ** 2) * np.exp(-np.abs(x) * 2.6)
    diamond = np.clip(1 - (np.abs(x) + np.abs(y)) / 0.34, 0, 1) ** 1.5
    core = np.exp(-(r / 0.09) ** 2)
    m = np.clip(cross * 0.9 + diamond * 0.7 + core, 0, 1) * np.clip((1 - r) * 5, 0, 1)
    save(m, "mote", np.clip(0.75 + core * 0.25, 0, 1))


if __name__ == "__main__":
    gen_sigil()
    gen_rune_band()
    gen_strand()
    gen_lance()
    gen_star()
    gen_broken_ring()
    gen_shard()
    gen_hex()
    gen_burst()
    gen_rune("rune_a", 710)
    gen_rune("rune_b", 733)
    gen_mote()

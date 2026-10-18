#!/usr/bin/env python3
"""Generate one skill-tree background per line: assets/nusmp/textures/gui/skill_trees/<id>.png (256 x 256, palette PNG).

usage: python3 tools/lines/gen_backgrounds.py [--check] [--only id,id] [--rows-dir DIR] [--routes-dir DIR] [--out DIR] [--demo DIR]
  --check      regenerate in memory and fail (exit 1) when a file is missing or its pixels differ (mean error > 1.5 / 255, so another zlib or Pillow version does not flake)
  --only       only these line ids
  --rows-dir   read the rows from another directory (tests use a temporary rows folder that is never committed)
  --out        write somewhere else (previews)
  --selftest   check the promises of the generator (deterministic, seeded, distinct themes, dark, tinted, small)
  --demo DIR   write one sample of each of the 15 themes (and a contact sheet) to DIR, to look at them

WHY: a skill tree is a big empty panel with nodes on it, and 283 trees on the same flat colour read as one tree. Every line therefore gets its own picture, chosen by the line's
background_theme (15 families, catalog.BACKGROUNDS: what the place IS), coloured by the line's colour (what the line FEELS like) and seeded from the line id (so two lines of the same
theme are still two different pictures). The pictures are DARK and LOW CONTRAST on purpose: the nodes, their icons and the lines between them are drawn over this, and the first
rule of a background is to lose to them. `normalize()` enforces it for every theme: the 99th percentile of the brightness is pulled to 0.27 and the mean stays under 0.15.
Source of the lines: route_sources.collect (the route json files plus the rows), so it works while rows/ is empty or half filled; hand-built routes get their theme from a table.
Deterministic: everything is seeded from sha256(id), no clock, no global random state.
"""
import hashlib
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import route_sources as RS  # noqa: E402
import catalog as C  # noqa: E402

OUT_DIR = os.path.join(RS.ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "skill_trees")
N = 256
# a line without a theme (a future franchise file that forgot it) falls back to its franchise's usual place
FRANCHISE_THEME = {"exiled_heavy_knight": "ruin", "tensura": "ocean", "black_clover": "forest", "magi": "sand", "solo_leveling": "void", "danmachi": "tower", "fairy_tail": "stellar",
                   "tbate": "holy", "tower_of_god": "tower", "god_of_high_school": "city", "omniscient_readers_viewpoint": "dream"}
# base (the dark ground), mid (the structure), accent (the glow): linear-ish 0..1 RGB. The line colour is mixed into mid and accent.
PALETTE = {
    "magma": ((.07, .03, .03), (.30, .12, .08), (1.00, .42, .10)),
    "void": ((.03, .02, .06), (.20, .10, .34), (.55, .30, .95)),
    "stellar": ((.02, .03, .08), (.14, .18, .40), (.80, .85, 1.00)),
    "forest": ((.02, .06, .03), (.08, .26, .12), (.70, .90, .40)),
    "ocean": ((.01, .04, .09), (.04, .20, .34), (.40, .85, .95)),
    "storm": ((.04, .045, .06), (.20, .22, .30), (.75, .85, 1.00)),
    "ruin": ((.06, .055, .05), (.30, .28, .24), (.45, .55, .30)),
    "blood": ((.07, .0, .01), (.28, .02, .04), (.90, .10, .12)),
    "holy": ((.05, .05, .06), (.30, .27, .20), (1.00, .92, .62)),
    "frost": ((.03, .06, .09), (.16, .30, .40), (.80, .95, 1.00)),
    "tower": ((.045, .045, .06), (.20, .20, .28), (.80, .75, .55)),
    "city": ((.02, .025, .045), (.10, .12, .22), (.20, .90, .95)),
    "dream": ((.07, .05, .10), (.30, .22, .40), (1.00, .70, .90)),
    "sand": ((.08, .06, .03), (.34, .24, .12), (.95, .75, .40)),
    "iron": ((.045, .045, .05), (.26, .27, .30), (.60, .45, .35)),
}


# ---------------------------------------------------------------------------------------------------------- noise toolbox
def seed_of(line_id, salt=""):
    return int.from_bytes(hashlib.sha256((line_id + salt).encode()).digest()[:8], "big")


YY, XX = np.mgrid[0:N, 0:N].astype(np.float32)
U, V = XX / (N - 1), YY / (N - 1)


def smooth(rng, cells):
    """Smooth value noise in 0..1: a random lattice of cells x cells, bicubic upsampled."""
    a = rng.random((cells + 1, cells + 1)).astype(np.float32)
    im = Image.fromarray((a * 255).astype(np.uint8)).resize((N, N), Image.BICUBIC)
    return np.asarray(im, np.float32) / 255.0


def fbm(rng, base=4, octaves=5, gain=0.5):
    out = np.zeros((N, N), np.float32)
    amp, tot, cells = 1.0, 0.0, base
    for _ in range(octaves):
        out += amp * smooth(rng, cells)
        tot += amp
        amp *= gain
        cells *= 2
    out /= tot
    lo, hi = np.percentile(out, 1), np.percentile(out, 99)
    return np.clip((out - lo) / max(hi - lo, 1e-6), 0, 1)


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def worley(rng, count):
    """(F1, F2, id) of a Worley field with `count` random points, distances in 0..1 units of the image."""
    pts = rng.random((count, 2)).astype(np.float32)
    d = np.sqrt((U[..., None] - pts[:, 0]) ** 2 + (V[..., None] - pts[:, 1]) ** 2)
    idx = np.argsort(d, axis=2)
    f1 = np.take_along_axis(d, idx[..., :1], 2)[..., 0]
    f2 = np.take_along_axis(d, idx[..., 1:2], 2)[..., 0]
    return f1, f2, idx[..., 0]


def draw_mask(fn, blur=0.0, ss=1):
    """A float mask from PIL drawing code: fn(ImageDraw, scale) draws in a (N*ss)^2 L image."""
    im = Image.new("L", (N * ss, N * ss), 0)
    fn(ImageDraw.Draw(im), ss)
    if ss != 1:
        im = im.resize((N, N), Image.LANCZOS)
    if blur > 0:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im, np.float32) / 255.0


def blur(a, r):
    return np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), np.float32) / 255.0


def stars(rng, count, power=3.0, size=1):
    m = np.zeros((N, N), np.float32)
    xs, ys = rng.integers(0, N, count), rng.integers(0, N, count)
    br = rng.random(count) ** power
    for x, y, b in zip(xs, ys, br):
        m[y, x] = max(m[y, x], 0.25 + 0.75 * b)
    return m


def glint(mask_points, arm):
    """A plus-shaped glint at each (x, y, strength)."""
    m = np.zeros((N, N), np.float32)
    for x, y, s in mask_points:
        for k in range(-arm, arm + 1):
            f = (1 - abs(k) / (arm + 1)) * s
            if 0 <= x + k < N:
                m[y, x + k] = max(m[y, x + k], f)
            if 0 <= y + k < N:
                m[y + k, x] = max(m[y + k, x], f)
    return m


# ---------------------------------------------------------------------------------------------------------- the 15 themes: each returns [(mask 0..1, 'mid'|'accent'|(r,g,b)), ...]
def t_magma(rng):
    f1, f2, cid = worley(rng, 46)
    edge = f2 - f1
    veins = 1 - smoothstep(0.0, 0.022, edge)
    plates = (0.35 + 0.65 * rng.random(46)[cid]) * (0.5 + 0.5 * fbm(rng, 6, 4)) * (1 - veins)
    heat = fbm(rng, 3, 4)
    glow = np.clip(veins * (0.45 + 0.8 * heat), 0, 1)
    return [(plates * 0.8, "mid"), (blur(glow, 3) * 0.9, "accent"), (glow, "accent")]


def t_void(rng):
    n1, n2 = fbm(rng, 3, 6), fbm(rng, 5, 5)
    neb = np.clip(n1 - 0.35, 0, 1) ** 1.6 * 1.8
    swirl = np.clip(n2 - 0.5, 0, 1) * 2
    cx, cy = 0.3 + 0.4 * rng.random(), 0.3 + 0.4 * rng.random()
    r = np.sqrt((U - cx) ** 2 + (V - cy) ** 2)
    ring = np.exp(-((r - 0.22) / 0.012) ** 2) * 0.5
    return [(neb, "mid"), (swirl * neb, "accent"), (ring, "accent"), (stars(rng, 150), (0.8, 0.75, 1.0))]


def t_stellar(rng):
    band = np.exp(-(((U * 0.8 + V * 0.6) - (0.55 + 0.1 * rng.random())) / 0.12) ** 2) * fbm(rng, 6, 5)
    big = [(int(rng.integers(8, N - 8)), int(rng.integers(8, N - 8)), 0.7 + 0.3 * rng.random()) for _ in range(14)]
    def lines(d, ss):
        pts = [(p[0] * ss, p[1] * ss) for p in sorted(big[:9])]          # sorted by x so the constellation is a chain, not a scribble
        for a, b in zip(pts, pts[1:]):
            d.line([a, b], fill=70, width=ss)
    return [(band * 0.8, "mid"), (stars(rng, 520, 2.5), (0.9, 0.9, 1.0)), (glint(big, 5), "accent"), (draw_mask(lines, 0.6, 2), "accent")]


def t_forest(rng):
    def leaves(d, ss):
        for depth in range(3):
            for _ in range(70):
                cx, cy = rng.integers(-10, N + 10) * ss, rng.integers(-10, N + 10) * ss
                rx, ry = int(rng.integers(10, 26) * ss), int(rng.integers(4, 11) * ss)
                ang = rng.random() * math.pi
                pts = []
                for k in range(16):
                    t = k / 16 * 2 * math.pi
                    x, y = rx * math.cos(t), ry * math.sin(t) * (1 + 0.3 * math.cos(t))
                    pts.append((cx + x * math.cos(ang) - y * math.sin(ang), cy + x * math.sin(ang) + y * math.cos(ang)))
                d.polygon(pts, fill=int(55 + 60 * depth + rng.integers(0, 30)))
    can = draw_mask(leaves, 0.7, 2)
    shafts = np.clip(np.sin((U * 9 - V * 5) * math.pi + 4 * fbm(rng, 3, 3)) * 0.5 + 0.4, 0, 1) * smoothstep(1.0, 0.1, V) * (1 - can * 0.8) * fbm(rng, 4, 4)
    return [(can * 0.9, "mid"), (shafts * 0.55, "accent")]


def t_ocean(rng):
    w1, w2 = fbm(rng, 3, 4), fbm(rng, 4, 4)
    caustic = (1 - np.abs(np.sin(U * 24 + 7 * w1) * np.sin(V * 24 + 7 * w2))) ** 9
    depth = (1 - V) ** 1.6
    def bubbles(d, ss):
        for _ in range(46):
            x, y, r = rng.integers(0, N) * ss, rng.integers(0, N) * ss, int(rng.integers(2, 9) * ss)
            d.ellipse((x - r, y - r, x + r, y + r), outline=150, width=max(1, ss // 2))
    return [(depth * 0.7, "mid"), (caustic * (0.3 + 0.7 * depth) * 0.8, "accent"), (draw_mask(bubbles, 0.4, 2) * 0.7, "mid")]


def t_storm(rng):
    clouds = fbm(rng, 3, 6)
    dark = np.clip(clouds, 0, 1) ** 1.4
    def rain(d, ss):
        for _ in range(260):
            x, y = rng.integers(-20, N) * ss, rng.integers(-20, N) * ss
            L = int(rng.integers(10, 26) * ss)
            d.line([(x, y), (x + L * 0.28, y + L)], fill=int(60 + rng.integers(0, 60)), width=ss)
    def bolt(d, ss):
        x, y = int((0.25 + 0.5 * rng.random()) * N * ss), 0
        branches = [(x, y, 0)]
        for _ in range(60):
            if not branches:
                break
            bx, by, depth = branches.pop(0)
            for _s in range(int(rng.integers(10, 22))):
                nx, ny = bx + int(rng.integers(-9, 10) * ss), by + int(rng.integers(5, 12) * ss)
                d.line([(bx, by), (nx, ny)], fill=255 if depth == 0 else 160, width=max(1, ss * (2 if depth == 0 else 1)))
                bx, by = nx, ny
                if depth < 2 and rng.random() < 0.12:
                    branches.append((bx, by, depth + 1))
                if by > N * ss:
                    break
    b = draw_mask(bolt, 0.5, 2)
    return [(dark * 0.9, "mid"), (draw_mask(rain, 0.3, 2) * 0.5, "accent"), (blur(b, 4) * 0.7, "accent"), (b * 0.9, (1.0, 1.0, 1.0))]


def t_ruin(rng):
    def blocks(d, ss):
        y = 0
        while y < N:
            h = int(rng.integers(24, 38))
            x = -int(rng.integers(0, 50))
            while x < N:
                w = int(rng.integers(34, 74))
                v = int(110 + rng.integers(0, 110))
                d.rectangle(((x + 2) * ss, (y + 2) * ss, (x + w - 2) * ss, (y + h - 2) * ss), fill=v)
                x += w
            y += h
    stone = draw_mask(blocks, 0.4, 2) * (0.45 + 0.55 * fbm(rng, 8, 4))
    def cracks(d, ss):
        for _ in range(9):
            x, y = int(rng.integers(0, N)), int(rng.integers(0, N))
            for _s in range(int(rng.integers(8, 20))):
                nx, ny = x + int(rng.integers(-8, 9)), y + int(rng.integers(2, 10))
                d.line([(x * ss, y * ss), (nx * ss, ny * ss)], fill=255, width=ss)
                x, y = nx, ny
    cr = draw_mask(cracks, 0.3, 2)
    moss = np.clip(fbm(rng, 5, 5) - 0.55, 0, 1) * 2.2 * smoothstep(0.3, 1.0, V)
    return [(stone * (1 - cr * 0.9), "mid"), (moss * 0.5, "accent")]


def t_blood(rng):
    mott = fbm(rng, 4, 5)
    def veins(d, ss):
        def grow(x, y, ang, width, depth):
            for _ in range(int(rng.integers(14, 26))):
                ang += (rng.random() - 0.5) * 0.7
                nx, ny = x + math.cos(ang) * 8, y + math.sin(ang) * 8
                d.line([(x * ss, y * ss), (nx * ss, ny * ss)], fill=255, width=max(1, int(width * ss)))
                x, y = nx, ny
                if depth < 4 and rng.random() < 0.22:
                    grow(x, y, ang + (rng.random() - 0.5) * 2.0, max(1, width * 0.6), depth + 1)
        for _ in range(4):
            grow(rng.random() * N, rng.random() * N, rng.random() * 6.28, 3.2, 0)
    v = draw_mask(veins, 0.6, 2)
    def drops(d, ss):
        for _ in range(26):
            x, y, r = rng.integers(0, N) * ss, rng.integers(0, N) * ss, int(rng.integers(2, 9) * ss)
            d.ellipse((x - r, y - r, x + r, y + r), fill=255)
            for _s in range(int(rng.integers(2, 6))):
                dx, dy, rr = rng.integers(-20, 21) * ss, rng.integers(-20, 21) * ss, int(rng.integers(1, 3) * ss)
                d.ellipse((x + dx - rr, y + dy - rr, x + dx + rr, y + dy + rr), fill=255)
    cx, cy = 0.5, 0.5
    pulse = (0.5 + 0.5 * np.sin(np.sqrt((U - cx) ** 2 + (V - cy) ** 2) * 60)) * 0.35
    return [((mott * 0.8 + pulse * 0.4), "mid"), (blur(v, 2) * 0.6, "accent"), (v, "accent"), (draw_mask(drops, 0.5, 2) * 0.8, "accent")]


def t_holy(rng):
    cx = 0.5
    ang = np.arctan2(V - (-0.05), U - cx)
    r = np.sqrt((U - cx) ** 2 + (V + 0.05) ** 2)
    rays = (0.5 + 0.5 * np.sin(ang * 16 + 3 * fbm(rng, 2, 2))) ** 3 * np.exp(-r * 1.4)
    halo = np.exp(-((r - 0.46) / 0.012) ** 2) * 0.8 + np.exp(-((r - 0.52) / 0.006) ** 2) * 0.5
    glow = np.exp(-(r / 0.35) ** 2)
    pts = [(int(rng.integers(6, N - 6)), int(rng.integers(6, N - 6)), 0.5 + 0.5 * rng.random()) for _ in range(34)]
    return [(glow * 0.9, "mid"), (rays * 1.2, "accent"), (halo, "accent"), (glint(pts, 4), (1.0, 0.97, 0.85))]


def t_frost(rng):
    f1, f2, cid = worley(rng, 34)
    facets = (0.3 + 0.7 * rng.random(34)[cid]) * (1 - smoothstep(0.0, 0.5, f1)) * 0.9 + 0.1
    edge = 1 - smoothstep(0.0, 0.012, f2 - f1)
    def flakes(d, ss):
        for _ in range(4):
            cx, cy, R = rng.integers(30, N - 30) * ss, rng.integers(30, N - 30) * ss, int(rng.integers(22, 48) * ss)
            for k in range(6):
                a = k * math.pi / 3 + 0.3
                d.line([(cx, cy), (cx + R * math.cos(a), cy + R * math.sin(a))], fill=255, width=ss)
                for frac in (0.4, 0.65, 0.85):
                    bx, by = cx + R * frac * math.cos(a), cy + R * frac * math.sin(a)
                    L = R * (0.3 - 0.18 * frac)
                    for side in (-1, 1):
                        d.line([(bx, by), (bx + L * math.cos(a + side * 0.9), by + L * math.sin(a + side * 0.9))], fill=230, width=max(1, ss // 2 + 1))
    fl = draw_mask(flakes, 0.4, 2)
    speck = (rng.random((N, N)) > 0.992).astype(np.float32)
    return [(facets, "mid"), (edge * 0.8, "accent"), (fl * 0.9, "accent"), (speck, (1.0, 1.0, 1.0))]


def t_tower(rng):
    def floors(d, ss):
        y, gap = 8.0, 14.0
        while y < N:
            d.rectangle((0, int(y) * ss, N * ss, int(y + 3) * ss), fill=255)
            d.rectangle((0, int(y + 3) * ss, N * ss, int(y + 5) * ss), fill=60)
            wx = 0
            while wx < N:
                if rng.random() < 0.55:
                    d.rectangle(((wx + 4) * ss, int(y + gap * 0.45) * ss, (wx + 12) * ss, int(y + gap * 0.85) * ss), fill=255)
                wx += 22
            y += gap
            gap = min(gap * 1.07, 40.0)
    fl = draw_mask(floors, 0.4, 2)
    pillars = (np.abs(((XX % 64) - 32)) < 2).astype(np.float32) * 0.35
    ang_r = np.sqrt((U - 0.5) ** 2 + (V - 0.5) ** 2)
    rings = np.exp(-((ang_r - 0.2) / 0.008) ** 2) + np.exp(-((ang_r - 0.3) / 0.006) ** 2)
    lit = (rng.random((N, N)) > 0.5).astype(np.float32)
    return [(np.clip(fl * 0.5 + pillars, 0, 1), "mid"), (np.clip(rings, 0, 1) * 0.5, "accent"), (blur(fl * lit * (fl > 0.9), 1.2) * 0.8, "accent")]


def t_city(rng):
    def blocks(d, ss):
        x = 0
        while x < N:
            w = int(rng.integers(14, 36))
            h = int(rng.integers(30, 170))
            d.rectangle((x * ss, (N - h) * ss, (x + w - 3) * ss, N * ss), fill=255)
            x += w
        for _ in range(0):
            pass
    sk = draw_mask(blocks, 0.0, 1)
    win = (rng.random((N, N)) > 0.86).astype(np.float32) * ((XX % 4 < 2) & (YY % 5 < 3)).astype(np.float32) * sk
    gridx = ((XX % 32) < 1).astype(np.float32) * 0.4
    gridy = ((YY % 32) < 1).astype(np.float32) * 0.4
    neon = np.zeros((N, N), np.float32)
    for _ in range(5):
        if rng.random() < 0.5:
            y = int(rng.integers(0, N)); neon[y:y + 2, :] = 1
        else:
            x = int(rng.integers(0, N)); neon[:, x:x + 2] = 1
    return [(sk * 0.5, "mid"), ((gridx + gridy) * (1 - sk * 0.7), "mid"), (win * 0.8, "accent"), (blur(neon, 3) * 0.7, "accent"), (neon * 0.5, "accent")]


def t_dream(rng):
    a, b = fbm(rng, 3, 4), fbm(rng, 3, 4)
    grad = np.clip(0.5 + (U * 0.7 + V * 0.4 - 0.55) * 1.2 + (a - 0.5) * 0.8, 0, 1)
    def bokeh(d, ss):
        for _ in range(34):
            x, y, r = rng.integers(0, N) * ss, rng.integers(0, N) * ss, int(rng.integers(8, 34) * ss)
            d.ellipse((x - r, y - r, x + r, y + r), fill=int(60 + rng.integers(0, 100)))
    bk = draw_mask(bokeh, 5, 1)
    pts = [(int(rng.integers(5, N - 5)), int(rng.integers(5, N - 5)), 0.5 + 0.5 * rng.random()) for _ in range(30)]
    return [(grad * 0.8, "mid"), (bk * (0.4 + 0.6 * b), "accent"), (glint(pts, 3), (1.0, 0.95, 1.0))]


def t_sand(rng):
    warp = fbm(rng, 3, 4)
    th = rng.random() * 0.5 + 0.2
    phase = (U * math.cos(th) + V * math.sin(th)) * 10 + 2.6 * warp
    saw = (phase % 1.0)
    lit = np.where(saw < 0.7, saw / 0.7, (1 - saw) / 0.3) ** 1.3
    ridge = np.exp(-((saw - 0.7) / 0.04) ** 2)
    ripple = 0.5 + 0.5 * np.sin((U * math.cos(th + 1.4) + V * math.sin(th + 1.4)) * 160 + 12 * fbm(rng, 5, 3))
    grain = rng.random((N, N)).astype(np.float32)
    return [(lit * 0.7 + ripple * 0.15 * lit, "mid"), (ridge * 0.35, "accent"), ((grain > 0.985).astype(np.float32) * 0.5, "accent")]


def t_iron(rng):
    P = 64
    plate_id = ((YY // P) * 7 + (XX // P) * 13).astype(np.int64)
    val = (0.55 + 0.4 * ((plate_id * 2654435761 % 997) / 997.0)).astype(np.float32)
    brushed = np.asarray(Image.fromarray((rng.random((N, 8)) * 255).astype(np.uint8)).resize((N, N), Image.BICUBIC), np.float32) / 255.0
    brushed = 0.7 + 0.3 * brushed
    lx, ly = XX % P, YY % P
    seam = ((lx < 2) | (ly < 2)).astype(np.float32)
    hi = ((lx == 2) | (ly == 2)).astype(np.float32) * 0.6
    rivet = np.zeros((N, N), np.float32)
    for oy in range(0, N, P):
        for ox in range(0, N, P):
            for (rx, ry) in ((7, 7), (P - 8, 7), (7, P - 8), (P - 8, P - 8)):
                r = np.sqrt((XX - (ox + rx)) ** 2 + (YY - (oy + ry)) ** 2)
                rivet += np.clip(1.6 - r / 2.5, 0, 1)
    def scratches(d, ss):
        for _ in range(40):
            x, y = rng.integers(0, N) * ss, rng.integers(0, N) * ss
            d.line([(x, y), (x + int(rng.integers(-30, 31)) * ss, y + int(rng.integers(-6, 7)) * ss)], fill=int(80 + rng.integers(0, 120)), width=ss)
    rust = np.clip(fbm(rng, 4, 5) - 0.55, 0, 1) * 2.0
    return [(val * brushed * (1 - seam * 0.85) + hi * 0.4, "mid"), (np.clip(rivet, 0, 1) * 0.7, "mid"), (draw_mask(scratches, 0.3, 2) * 0.4, "mid"), (rust * 0.35, "accent")]


THEMES = {"magma": t_magma, "void": t_void, "stellar": t_stellar, "forest": t_forest, "ocean": t_ocean, "storm": t_storm, "ruin": t_ruin, "blood": t_blood, "holy": t_holy,
          "frost": t_frost, "tower": t_tower, "city": t_city, "dream": t_dream, "sand": t_sand, "iron": t_iron}


# ---------------------------------------------------------------------------------------------------------- compose
def mix(a, b, t):
    return tuple(x * (1 - t) + y * t for x, y in zip(a, b))


def normalize(img):
    """Dark and low contrast for every theme: the 99th percentile of the brightness goes to 0.27, the mean may not exceed 0.15; a soft vignette."""
    lum = img @ np.array([0.3, 0.59, 0.11], np.float32)
    p99 = float(np.percentile(lum, 99))
    scale = min(3.0, max(0.1, 0.27 / max(p99, 1e-3)))
    img = img * scale
    r = np.sqrt((U - 0.5) ** 2 + (V - 0.5) ** 2)
    img = img * (0.78 + 0.22 * (1 - np.clip(r / 0.72, 0, 1) ** 2))[..., None]
    lum = img @ np.array([0.3, 0.59, 0.11], np.float32)
    mean = float(lum.mean())
    if mean > 0.15:
        img = img * (0.15 / mean)
    return np.clip(img, 0, 1)


def render(line_id, theme, color_hex):
    """One background as an RGB float array (N, N, 3) in 0..1. Pure function of (id, theme, colour)."""
    if theme not in THEMES:
        raise ValueError("unknown background theme %r for %s" % (theme, line_id))
    rng = np.random.default_rng(seed_of(line_id, theme))
    base, mid, accent = PALETTE[theme]
    tint = tuple(int(color_hex[i:i + 2], 16) / 255.0 for i in (1, 3, 5))
    lum_t = 0.3 * tint[0] + 0.59 * tint[1] + 0.11 * tint[2]
    tint = tuple(c / max(lum_t, 0.35) * 0.55 if lum_t < 0.35 else c for c in tint)             # a very dark line colour would paint nothing: lift it
    tint = tuple(min(1.0, c) for c in tint)
    mid_c = mix(mid, tuple(c * 0.55 for c in tint), 0.42)
    acc_c = mix(accent, tint, 0.55)
    base_c = mix(base, tuple(c * 0.14 for c in tint), 0.5)
    img = np.zeros((N, N, 3), np.float32) + np.array(base_c, np.float32)
    for mask, role in THEMES[theme](rng):
        col = mid_c if role == "mid" else acc_c if role == "accent" else role
        img += np.clip(mask, 0, 1)[..., None] * np.array(col, np.float32)
    return normalize(img)


def to_png(rgb):
    im = Image.fromarray((rgb * 255 + 0.5).astype(np.uint8), "RGB")
    return im.quantize(colors=32, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)


def write(path, rgb, check, problems):
    pal = to_png(rgb)
    if check:
        if not os.path.exists(path):
            problems.append("missing " + path)
            return
        old = np.asarray(Image.open(path).convert("RGB"), np.float32)
        new = np.asarray(pal.convert("RGB"), np.float32)
        if old.shape != new.shape or float(np.abs(old - new).mean()) > 1.5:
            problems.append("differs " + path)
        return
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        old = np.asarray(Image.open(path).convert("RGB"), np.float32)
        new = np.asarray(pal.convert("RGB"), np.float32)
        if old.shape == new.shape and float(np.abs(old - new).mean()) < 1e-6:
            return
    pal.save(path, optimize=True)


def theme_of(line):
    t = line["theme"]
    if t in THEMES:
        return t
    t = FRANCHISE_THEME.get(line["franchise"])
    if t:
        return t
    return C.BACKGROUNDS[seed_of(line["id"]) % len(C.BACKGROUNDS)]


def main():
    args = sys.argv[1:]
    get = lambda k: args[args.index(k) + 1] if k in args else None
    check = "--check" in args
    out = get("--out") or OUT_DIR
    if "--selftest" in args:
        selftest()
        return
    if get("--demo"):
        demo(get("--demo"))
        return
    lines = RS.collect(get("--rows-dir"), get("--routes-dir"))
    only = set(get("--only").split(",")) if get("--only") else None
    problems, n = [], 0
    for lid, line in lines.items():
        if only and lid not in only:
            continue
        write(os.path.join(out, lid + ".png"), render(lid, theme_of(line), line["color"]), check, problems)
        n += 1
    if check and problems:
        print("STALE (%d):\n  %s\nrun python3 tools/lines/gen_backgrounds.py" % (len(problems), "\n  ".join(problems[:40])))
        sys.exit(1)
    print("ok: %d backgrounds %s" % (n, "checked" if check else "written to " + out))


def selftest():
    """Properties the generator promises: deterministic, seeded by id, themes distinct, dark, tinted, small."""
    import io
    fails = []
    for t in C.BACKGROUNDS:
        a = render("probe_a", t, "#40A0E0")
        if not np.array_equal(a, render("probe_a", t, "#40A0E0")):
            fails.append("%s is not deterministic" % t)
        b = render("probe_b", t, "#40A0E0")
        if float(np.abs(a - b).mean()) < 0.004:
            fails.append("%s: two ids give (almost) the same picture" % t)
        lum = a @ np.array([0.3, 0.59, 0.11])
        if lum.mean() > 0.15 or np.percentile(lum, 99) > 0.34:
            fails.append("%s is too bright: mean %.3f p99 %.3f" % (t, lum.mean(), np.percentile(lum, 99)))
        warm, cool = render("probe_a", t, "#E04020"), render("probe_a", t, "#2040E0")
        if not (warm[..., 0].mean() - warm[..., 2].mean() > cool[..., 0].mean() - cool[..., 2].mean()):
            fails.append("%s ignores the line colour" % t)
        buf = io.BytesIO()
        to_png(a).save(buf, "PNG", optimize=True)
        if buf.tell() > 45000:
            fails.append("%s is %d bytes" % (t, buf.tell()))
    themes = list(C.BACKGROUNDS)
    for i in range(len(themes)):
        for j in range(i + 1, len(themes)):
            a, b = render("probe_a", themes[i], "#808080"), render("probe_a", themes[j], "#808080")
            if float(np.abs(a - b).mean()) < 0.01:
                fails.append("themes %s and %s look alike" % (themes[i], themes[j]))
    if fails:
        print("SELFTEST FAILED:\n  " + "\n  ".join(fails))
        sys.exit(1)
    print("selftest ok: %d themes deterministic, seeded, distinct, dark, tinted, small" % len(themes))


def demo(d):
    os.makedirs(d, exist_ok=True)
    cols = ["#E8541C", "#8E5BD6", "#6EC3F7", "#3FAA55", "#2D8BC9", "#F5D63B", "#B0B8C8", "#C0182A", "#FFE9A0", "#BDEFFF", "#C9A86A", "#20E0E8", "#F0A0D8", "#E0B070", "#9AA4B0"]
    tiles = []
    for i, t in enumerate(C.BACKGROUNDS):
        rgb = render("demo_" + t, t, cols[i])
        write(os.path.join(d, t + ".png"), rgb, False, [])
        tiles.append(Image.fromarray((rgb * 255).astype(np.uint8)))
    sheet = Image.new("RGB", (5 * N, 3 * N))
    for i, t in enumerate(tiles):
        sheet.paste(t, ((i % 5) * N, (i // 5) * N))
    sheet.save(os.path.join(d, "sheet.png"))
    print("demo written to", d)


if __name__ == "__main__":
    main()

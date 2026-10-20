"""The skins of the God worn relics: one 512 x 512 albedo PNG and one 512 x 512 emissive glow map per relic (textures/relic_worn/god/<id>.png and <id>_glow.png).

Layout: 8 x 8 cells of 64 x 64 texels. Cells 0..47 are the shared MATERIAL library (hammered metal, engraved gilt, plate with bevel and rivets, woven cloth, damask, silk, chain
mail, dragon scale, hoarfrost, mirror glass, ... and the glow-only sprites), cells 48..55 are the relic's own DECALS (crest, panel, border, trim, two rune bands, filigree, surface
weave) drawn from its own motif, cells 56..63 are spare. Like the old atlas the albedo is grey and is tinted by the relic's palette slot at draw time (main, trim, glow, dark,
accent), so the hammered, engraved, worn look is in the grey and the colour comes from the palette; the glow map is white-on-black and is drawn additive in the glow slot colour
(`LIT` quads and `EMIT` quads sample it).

Tileable materials are painted on a 48 texel period and wrapped into an 8 texel gutter (the same trick as gen_worn_lib's atlas); "fit" cells are stretched over a part. Every
material is a height field lit from the upper left (bevels, AO from the height, wear scuffs on edges), so the detail reads as relief and not as a flat picture.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "..", "relics"))
import gen_worn_lib as L  # noqa: E402

C = 64                  # cell size
PERIOD = 48             # tileable period inside a cell
GUT = (C - PERIOD) // 2
COLS = ROWS = 8
SKIN = C * COLS

# ----------------------------------------------------------------------------- the cell table
TILE_NAMES = ["metal", "hammered", "gilt", "cloth", "damask", "silk", "velvet", "leather", "chain", "scale", "fur", "bone", "horn", "frost", "glass", "crystal", "vein", "shadow", "slime",
              "marble", "rope", "rust", "flame", "stars", "lace", "feathery", "brushed", "weave2"]
FIT_NAMES = ["plate", "feather", "gem", "gem2", "rune_band", "trim", "hem", "iris", "sun", "ring_glow", "soft", "spark", "ember", "pearl", "white", "black", "gold", "banner", "pauldron", "link"]
LIBRARY = TILE_NAMES + FIT_NAMES                                 # cells 0..47 (padded)
DECALS = ["crest", "panel", "border", "trim2", "rune_a", "rune_b", "filigree", "surface"]    # cells 48..55
CELL_INDEX = {n: i for i, n in enumerate(LIBRARY)}
for _k, _n in enumerate(DECALS):
    CELL_INDEX[_n] = 48 + _k
TILEABLE = set(TILE_NAMES) | {"surface"}


def is_tileable(name):
    return name in TILEABLE


# ----------------------------------------------------------------------------- field helpers (all tileable unless said)
def rng(seed):
    return np.random.default_rng(seed)


def fit01(a):
    a = a - a.min()
    m = a.max()
    return a / m if m > 1e-9 else a


def fbm(n, seed, octaves=4, base=2):
    return fit01(L.pnoise(n, n, seed, octaves=octaves, base=base))


def grid(n):
    y, x = np.mgrid[0:n, 0:n].astype(np.float64)
    return x + 0.5, y + 0.5


def voronoi(n, k, seed, sx=1.0, sy=1.0):
    """Tileable voronoi: distance to the nearest (d1) and second nearest (d2) point and the id of the nearest."""
    r = rng(seed)
    pts = r.uniform(0, n, (k, 2))
    x, y = grid(n)
    d1 = np.full((n, n), 1e9)
    d2 = np.full((n, n), 1e9)
    ids = np.zeros((n, n), int)
    for i, (px, py) in enumerate(pts):
        dx = np.abs(x - px)
        dx = np.minimum(dx, n - dx) * sx
        dy = np.abs(y - py)
        dy = np.minimum(dy, n - dy) * sy
        d = np.hypot(dx, dy)
        closer = d < d1
        d2 = np.where(closer, d1, np.minimum(d2, d))
        ids = np.where(closer, i, ids)
        d1 = np.where(closer, d, d1)
    return d1, d2, ids


def lit(h, strength=1.0, light=(-0.65, -0.75), base=0.80):
    """Lambert-ish shading of a height field (tileable: np.roll). Relief runs 0..1 over a few texels."""
    gx = (np.roll(h, -1, 1) - np.roll(h, 1, 1)) * 0.5 * 8.0
    gy = (np.roll(h, -1, 0) - np.roll(h, 1, 0)) * 0.5 * 8.0
    return base + strength * 0.25 * (-(gx * light[0] + gy * light[1]))


def lit_flat(h, strength=1.0, light=(-0.65, -0.75), base=0.80):
    gy, gx = np.gradient(h)
    return base + strength * 0.25 * (-(gx * 8.0 * light[0] + gy * 8.0 * light[1]))


def ao(h, k=2):
    """Cheap ambient occlusion of a height field: darker where the neighbourhood is higher."""
    b = h.copy()
    acc = np.zeros_like(h)
    for dx in range(-k, k + 1):
        for dy in range(-k, k + 1):
            acc += np.roll(np.roll(h, dx, 1), dy, 0)
    acc /= (2 * k + 1) ** 2
    return np.clip(1.0 - 1.6 * np.clip(acc - b, 0, 1), 0.55, 1.0)


def smooth(x, a, b):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def rgb(v):
    v = np.clip(v, 0, 1)
    return np.repeat(v[..., None], 3, axis=2)


def wear(n, seed, amount=0.5):
    """Scuffs: thin bright scratches and a few dark nicks."""
    r = rng(seed)
    w = np.zeros((n, n))
    for _ in range(int(10 * amount) + 3):
        x0, y0 = r.uniform(0, n, 2)
        ang = r.uniform(0, math.pi)
        ln = r.uniform(4, 14)
        for t in np.linspace(0, ln, int(ln * 2)):
            xx, yy = int(x0 + t * math.cos(ang)) % n, int(y0 + t * math.sin(ang)) % n
            w[yy, xx] = r.choice([0.14, -0.16])
    return w


# ----------------------------------------------------------------------------- tileable materials: each returns (albedo grey n x n, glow n x n)
def mat_metal(n=PERIOD):
    h = fbm(n, 11, 3, 3) * 0.3
    streak = np.tile(rng(12).normal(0, 1, (n, 1)), (1, n))
    h = h + 0.08 * streak
    v = lit(h, 0.9) * 0.9 + wear(n, 13, 0.8) + 0.05 * fbm(n, 14, 2, 6)
    return np.clip(v, 0.45, 1.0), np.zeros((n, n))


def mat_brushed(n=PERIOD):
    r = rng(15)
    line = np.tile(r.normal(0, 1, (n, 1)), (1, n))
    line = (line + np.roll(line, 1, 0)) * 0.5
    v = 0.84 + 0.06 * line + 0.05 * fbm(n, 16, 3, 2)
    v += 0.5 * (lit(fbm(n, 17, 2, 2) * 0.2, 1.0) - 0.8)
    return np.clip(v, 0.5, 1.0), np.zeros((n, n))


def mat_hammered(n=PERIOD):
    d1, d2, ids = voronoi(n, 26, 21)
    cell = np.sqrt(np.clip(d1 / 6.0, 0, 1))
    h = -(1.0 - smooth(cell, 0.0, 1.0)) * 0.9 + 0.12 * fbm(n, 22, 3, 3)
    edge = smooth(d2 - d1, 0.0, 1.4)
    tone = (rng(23).uniform(0.9, 1.05, 26))[ids]
    v = (lit(h, 1.6) * tone) * (0.8 + 0.2 * edge) + wear(n, 24, 0.6) * 0.8
    return np.clip(v, 0.42, 1.0), np.zeros((n, n))


def mat_gilt(n=PERIOD):
    x, y = grid(n)
    t = 2 * math.pi / 24.0
    a = np.abs(np.sin((x + y) * t * 0.5)) ** 0.25
    b = np.abs(np.sin((x - y) * t * 0.5)) ** 0.25
    lines = np.minimum(a, b)                                    # 0 on the diamond lattice
    groove = 1.0 - smooth(lines, 0.0, 0.35)
    cx = np.minimum(np.abs((x % 24) - 12), 12)
    cy = np.minimum(np.abs((y % 24) - 12), 12)
    bead = smooth(5.0 - np.hypot(((x % 24) - 12), ((y % 24) - 12)) * 0.9, 0.0, 3.0)
    h = -0.5 * groove + 0.45 * bead + 0.05 * fbm(n, 31, 3, 2)
    v = lit(h, 1.4) + 0.05 * wear(n, 32, 0.4) + 0.1
    return np.clip(v, 0.5, 1.0), 0.12 * groove


def mat_cloth(n=PERIOD):
    x, y = grid(n)
    weave = ((np.floor(x / 1.0) + np.floor(y / 1.0)) % 2) * 0.5 + ((np.floor(x / 2.0) % 2) * 0.25 - (np.floor(y / 2.0) % 2) * 0.25)
    fold = np.sin(2 * math.pi * x / n * 2 + 0.7 * np.sin(2 * math.pi * y / n)) * 0.5 + 0.5
    thread = fbm(n, 41, 2, 7)
    h = 0.14 * weave + 0.5 * fold * 0.4 + 0.06 * thread
    v = lit(h, 1.2) * 0.96 + 0.04 * thread
    return np.clip(v, 0.45, 1.0), np.zeros((n, n))


def mat_damask(n=PERIOD):
    x, y = grid(n)
    u = (x % 24) / 24.0 - 0.5
    w = (y % 24) / 24.0 - 0.5
    d = np.abs(u) + np.abs(w)                                    # lozenge distance
    motif = smooth(0.42 - d, 0.0, 0.08) * (1 - smooth(0.30 - d, 0.0, 0.05) * 0.6)
    petal = smooth(0.12 - np.hypot(u, w * 0.55), 0.0, 0.05)
    offset = ((x + 12) % 24) / 24.0 - 0.5
    off2 = ((y + 12) % 24) / 24.0 - 0.5
    d_b = np.hypot(offset, off2)
    dots = smooth(0.1 - d_b, 0.0, 0.04)
    pat = np.clip(motif * 0.7 + petal * 0.6 + dots * 0.7, 0, 1)
    weave = (((x.astype(int) + y.astype(int)) % 2) * 0.04)
    h = 0.35 * pat + weave + 0.04 * fbm(n, 51, 2, 3)
    v = lit(h, 1.0) * 0.9 + 0.1 + 0.08 * pat
    return np.clip(v, 0.5, 1.0), 0.0 * pat


def mat_silk(n=PERIOD):
    x, y = grid(n)
    ph = 2 * math.pi * (y / n * 1.0 + 0.25 * np.sin(2 * math.pi * x / n))
    sheen = 0.5 + 0.5 * np.sin(ph * 2)
    thread = np.tile(rng(61).normal(0, 1, (1, n)), (n, 1)) * 0.015
    v = 0.78 + 0.18 * sheen ** 1.6 + thread + 0.04 * fbm(n, 62, 3, 2)
    return np.clip(v, 0.5, 1.0), np.zeros((n, n))


def mat_velvet(n=PERIOD):
    soft = fbm(n, 71, 4, 2)
    fibre = rng(72).normal(0, 1, (n, n))
    v = 0.55 + 0.25 * soft + 0.04 * fibre + 0.15 * lit(soft * 0.3, 0.8) - 0.12
    return np.clip(v, 0.3, 1.0), np.zeros((n, n))


def mat_leather(n=PERIOD):
    d1, d2, ids = voronoi(n, 54, 81)
    h = smooth(d2 - d1, 0.0, 1.3) * 0.5 + 0.03 * fbm(n, 82, 3, 5)
    v = lit(h, 1.3) * 0.95 + 0.06
    x, y = grid(n)
    seam = (np.abs((y % 24) - 3) < 0.7)
    stitch = seam & (((x.astype(int)) % 4) < 2)
    v = np.where(stitch, 1.0, v)
    v = np.where(np.abs((y % 24) - 5) < 0.6, v * 0.7, v)
    return np.clip(v, 0.4, 1.0), np.zeros((n, n))


def mat_chain(n=PERIOD):
    x, y = grid(n)
    h = np.zeros((n, n))
    alb = np.full((n, n), 0.25)
    for cx, cy, horiz in ((6, 6, True), (30, 6, False), (6, 30, False), (30, 30, True)):
        dx = ((x - cx + n / 2) % n) - n / 2
        dy = ((y - cy + n / 2) % n) - n / 2
        a, b = (9.0, 6.0) if horiz else (6.0, 9.0)
        e = np.sqrt((dx / a) ** 2 + (dy / b) ** 2)
        ring = smooth(0.34 - np.abs(e - 0.72), 0.0, 0.22)
        h = np.maximum(h, ring)
    h = h + 0.04 * fbm(n, 91, 3, 4)
    v = lit(h, 2.2, base=0.42) + 0.6 * h
    return np.clip(v, 0.15, 1.0), np.zeros((n, n))


def mat_scale(n=PERIOD):
    x, y = grid(n)
    h = np.zeros((n, n))
    tone = np.zeros((n, n))
    r = rng(101)
    rows, per = 8, 6
    sh = n / rows
    sw = n / per
    for row in range(rows):
        for k in range(per):
            cx = (k + (0.5 if row % 2 else 0.0)) * sw + sw / 2
            cy = row * sh + sh * 0.35
            dx = ((x - cx + n / 2) % n) - n / 2
            dy = ((y - cy + n / 2) % n) - n / 2
            e = np.sqrt((dx / (sw * 0.55)) ** 2 + (np.where(dy > 0, dy / (sh * 1.05), dy / (sh * 0.5))) ** 2)
            bump = np.clip(1.0 - e, 0, 1) ** 0.7
            ridge = np.clip(1.0 - np.abs(dx) / 1.2, 0, 1) * np.clip(1.0 - e, 0, 1) * 0.3
            top = (bump + ridge) * (row + 1) / (rows + 1) * 0 + (bump + ridge)
            upd = top > h
            h = np.where(upd, top + row * 0.0, h)
            tone = np.where(upd, r.uniform(0.85, 1.05), tone)
    v = lit(h, 1.8) * tone * 0.95 + 0.05 + wear(n, 102, 0.5) * 0.5
    return np.clip(v, 0.3, 1.0), np.zeros((n, n))


def mat_fur(n=PERIOD):
    r = rng(111)
    fib = r.normal(0, 1, (n, n))
    fib = (fib + np.roll(fib, 1, 0) + np.roll(fib, 2, 0)) / 3.0
    clump = fbm(n, 112, 3, 4)
    v = 0.78 + 0.12 * clump + 0.16 * fib
    return np.clip(v, 0.4, 1.0), np.zeros((n, n))


def mat_bone(n=PERIOD):
    soft = fbm(n, 121, 4, 2)
    pores = (rng(122).random((n, n)) > 0.985).astype(float)
    v = 0.9 + 0.07 * soft - 0.25 * pores
    r = rng(123)
    for _ in range(3):
        x0, y0 = int(r.integers(0, n)), int(r.integers(0, n))
        for k in range(12):
            v[(y0 + k) % n, (x0 + int(2.5 * math.sin(k * 0.8))) % n] -= 0.16
    v += 0.4 * (lit(soft * 0.25, 1.0) - 0.8)
    return np.clip(v, 0.5, 1.0), np.zeros((n, n))


def mat_horn(n=PERIOD):
    x, y = grid(n)
    ridge = 0.5 + 0.5 * np.sin(2 * math.pi * y / n * 4)
    grain = fbm(n, 131, 3, 2)
    h = 0.2 * ridge + 0.08 * grain
    v = lit(h, 1.6) * 0.95 + 0.03 * grain - 0.05
    return np.clip(v + 0.1, 0.4, 1.0), np.zeros((n, n))


def mat_frost(n=PERIOD):
    a = 1.0 - np.abs(L.pnoise(n, n, 141, octaves=4, base=3))
    b = 1.0 - np.abs(L.pnoise(n, n, 142, octaves=3, base=5))
    br = np.clip(a ** 5 + 0.5 * b ** 7, 0, 1)
    h = br * 0.6 + 0.05 * fbm(n, 143, 3, 2)
    v = 0.82 + 0.15 * br + 0.5 * (lit(h, 1.0) - 0.8)
    sparkle = (rng(144).random((n, n)) > 0.992).astype(float)
    return np.clip(v + sparkle * 0.4, 0.5, 1.0), 0.22 * br ** 4 + sparkle


def mat_glass(n=PERIOD):
    d1, d2, ids = voronoi(n, 18, 151)
    tilt = rng(152).uniform(-1, 1, (18, 2))
    gx, gy = tilt[ids, 0], tilt[ids, 1]
    x, y = grid(n)
    face = 0.78 + 0.18 * (gx * -0.65 + gy * -0.75) + 0.06 * fbm(n, 153, 2, 2)
    edge = 1.0 - smooth(d2 - d1, 0.0, 0.9)
    v = face + 0.4 * edge
    streak = np.clip(1.0 - np.abs(((x + y * 0.6) % 24) - 12) / 1.5, 0, 1) * 0.15
    return np.clip(v + streak, 0.5, 1.0), 0.5 * edge


def mat_crystal(n=PERIOD):
    d1, d2, ids = voronoi(n, 12, 161, 1.0, 0.45)
    tilt = rng(162).uniform(-1, 1, 12)
    face = 0.72 + 0.2 * tilt[ids] + 0.05 * fbm(n, 163, 2, 2)
    edge = 1.0 - smooth(d2 - d1, 0.0, 1.1)
    return np.clip(face + 0.35 * edge, 0.4, 1.0), 0.3 * edge


def mat_vein(n=PERIOD):
    n1 = L.pnoise(n, n, 171, octaves=4, base=2)
    n2 = L.pnoise(n, n, 172, octaves=3, base=4)
    n1 = n1 / max(1e-6, np.abs(n1).max())
    n2 = n2 / max(1e-6, np.abs(n2).max())
    v1 = np.clip(np.exp(-(n1 / 0.035) ** 2) + 0.6 * np.exp(-(n2 / 0.03) ** 2), 0, 1)             # thin lines where the noise crosses zero: branching veins, about a tenth of the cell
    h = 0.5 * v1 + 0.1 * fbm(n, 173, 3, 3)
    v = 0.45 + 0.25 * fbm(n, 174, 3, 2) + 0.5 * (lit(h, 1.0) - 0.8)
    return np.clip(v, 0.15, 1.0), np.clip(v1 * 0.95, 0, 1)


def mat_shadow(n=PERIOD):
    a = fbm(n, 181, 5, 2)
    b = fbm(n, 182, 4, 3)
    wisp = np.clip(1.0 - np.abs(a - 0.5) * 6.0, 0, 1) * b
    v = 0.30 + 0.35 * a + 0.2 * wisp
    return np.clip(v, 0.1, 1.0), 0.45 * wisp ** 2


def mat_slime(n=PERIOD):
    d1, d2, ids = voronoi(n, 14, 191)
    blob = smooth(1.0 - d1 / 10.0, 0.0, 1.0)
    h = blob + 0.1 * fbm(n, 192, 3, 2)
    lv = lit(h, 2.4)
    spec = np.clip((lv - 1.0) * 3.0, 0, 1)
    v = 0.72 + 0.14 * blob + 0.3 * spec
    return np.clip(v, 0.45, 1.0), 0.06 * blob + 0.5 * spec


def mat_marble(n=PERIOD):
    x, y = grid(n)
    vein = np.abs(np.sin(2 * math.pi * (x / n * 2 + y / n + 0.6 * L.pnoise(n, n, 201, 3, 2)))) ** 0.15
    v = 0.92 - 0.14 * (1 - vein) + 0.04 * fbm(n, 202, 3, 3)
    return np.clip(v, 0.55, 1.0), np.zeros((n, n))


def mat_rope(n=PERIOD):
    x, y = grid(n)
    tw = 0.5 + 0.5 * np.sin(2 * math.pi * (x / 12.0 + y / 6.0))
    strand = 0.5 + 0.5 * np.sin(2 * math.pi * (x / 12.0 - y / 6.0 * 0.0))
    h = 0.5 * tw * (0.6 + 0.4 * strand) + 0.04 * fbm(n, 211, 3, 4)
    v = lit(h, 2.0) * 0.95 + 0.05
    return np.clip(v, 0.3, 1.0), np.zeros((n, n))


def mat_rust(n=PERIOD):
    pits = fbm(n, 221, 4, 3)
    heat = fbm(n, 222, 3, 2)
    h = 0.25 * pits + 0.1 * fbm(n, 223, 4, 6)
    v = lit(h, 1.6) * (0.7 + 0.25 * pits) + 0.06 * heat + wear(n, 224, 1.0) * 0.6
    return np.clip(v, 0.2, 1.0), 0.25 * np.clip(heat - 0.6, 0, 1) * 2.5


def mat_flame(n=PERIOD):
    x, y = grid(n)
    v = np.zeros((n, n))
    for xx in range(n):
        h = 0.5 + 0.5 * math.sin(xx / n * 2 * math.pi * 3 + 1.1) * math.sin(xx / n * 2 * math.pi * 5)
        for yy in range(n):
            v[yy, xx] = max(0.0, 1.0 - (yy / n) * (1.2 + 0.5 * h)) * (0.7 + 0.3 * h)
    v = np.clip(v + 0.1 * (fbm(n, 231, 3, 3) - 0.5), 0, 1)
    return 0.4 + 0.6 * v, v


def mat_stars(n=PERIOD):
    r = rng(241)
    g = np.zeros((n, n))
    for _ in range(46):
        x0, y0 = int(r.integers(0, n)), int(r.integers(0, n))
        b = r.uniform(0.4, 1.0)
        g[y0, x0] = max(g[y0, x0], b)
        if b > 0.8:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                g[(y0 + dy) % n, (x0 + dx) % n] = max(g[(y0 + dy) % n, (x0 + dx) % n], b * 0.5)
    nebula = fbm(n, 242, 4, 2)
    return np.clip(0.2 + 0.2 * nebula + g * 0.6, 0, 1), np.clip(g + 0.25 * nebula ** 3, 0, 1)


def mat_lace(n=PERIOD):
    x, y = grid(n)
    u = ((x % 12) - 6) / 6.0
    w = ((y % 12) - 6) / 6.0
    ring = smooth(0.2 - np.abs(np.hypot(u, w) - 0.7), 0.0, 0.1)
    cross = smooth(0.12 - np.minimum(np.abs(u), np.abs(w)), 0.0, 0.06) * (np.hypot(u, w) < 0.8)
    pat = np.clip(ring + cross, 0, 1)
    h = 0.4 * pat
    v = lit(h, 1.2) * 0.95 + 0.1 - 0.1 * (1 - pat)
    return np.clip(v, 0.4, 1.0), 0.0 * pat


def mat_feathery(n=PERIOD):
    x, y = grid(n)
    barb = 0.5 + 0.5 * np.sin(2 * math.pi * (y / n * 6 + np.abs(((x % 24) - 12)) / 12.0 * 0.9))
    rach = (np.abs((x % 24) - 12) < 0.8).astype(float)
    v = 0.66 + 0.26 * barb + 0.1 * rach + 0.04 * fbm(n, 251, 3, 3)
    return np.clip(v, 0.4, 1.0), np.zeros((n, n))


def mat_weave2(n=PERIOD):
    x, y = grid(n)
    over = (((x / 4).astype(int) + (y / 4).astype(int)) % 2).astype(float)
    xi = (x % 4) / 4.0
    yi = (y % 4) / 4.0
    h = np.where(over > 0.5, 0.5 + 0.5 * np.sin(math.pi * yi), 0.5 + 0.5 * np.sin(math.pi * xi)) * 0.4
    v = lit(h, 1.6) * 0.95 + 0.05
    return np.clip(v, 0.3, 1.0), np.zeros((n, n))


TILE_FNS = {"metal": mat_metal, "hammered": mat_hammered, "gilt": mat_gilt, "cloth": mat_cloth, "damask": mat_damask, "silk": mat_silk, "velvet": mat_velvet, "leather": mat_leather,
            "chain": mat_chain, "scale": mat_scale, "fur": mat_fur, "bone": mat_bone, "horn": mat_horn, "frost": mat_frost, "glass": mat_glass, "crystal": mat_crystal, "vein": mat_vein,
            "shadow": mat_shadow, "slime": mat_slime, "marble": mat_marble, "rope": mat_rope, "rust": mat_rust, "flame": mat_flame, "stars": mat_stars, "lace": mat_lace,
            "feathery": mat_feathery, "brushed": mat_brushed, "weave2": mat_weave2}


# ----------------------------------------------------------------------------- fitted materials (64 x 64)
def fit_plate():
    n = C
    x, y = grid(n)
    b = np.minimum(np.minimum(x, n - x), np.minimum(y, n - y))
    h = np.zeros((n, n))
    h += smooth(b, 0.0, 3.0) * 0.5                     # outer chamfer
    h -= smooth(b, 3.0, 5.0) * 0.18                    # recessed field
    h += smooth(b, 4.5, 6.5) * 0.18 * (b < 8)
    ctr = np.hypot(x - n / 2, y - n / 2)
    h += 0.18 * smooth(24.0 - ctr, 0.0, 6.0) * (1 - smooth(18 - ctr, 0.0, 6.0))      # a raised ring boss
    h += 0.15 * smooth(8.0 - ctr, 0.0, 4.0)
    for rx, ry in ((9, 9), (n - 9, 9), (9, n - 9), (n - 9, n - 9)):
        h += 0.4 * smooth(2.6 - np.hypot(x - rx, y - ry), 0.0, 1.5)
    chev = np.zeros((n, n))
    for k in range(3):
        yy = 14 + k * 3
        chev = np.maximum(chev, smooth(0.9 - np.abs((y - yy) - np.abs(x - n / 2) * 0.35), 0.0, 0.6) * (np.abs(x - n / 2) < 18) * (y < 40) * 0)
    h += 0.05 * fbm(n, 261, 3, 4)
    v = lit_flat(h, 2.2) * (0.92 + 0.08 * ao(h, 2)) * 0.98
    v = np.where(b < 1.5, v * 0.7, v)
    v += wear(n, 262, 0.6) * 0.5 * (b < 10)
    return np.clip(v, 0.25, 1.0), 0.1 * smooth(ctr, 14, 20) * (1 - smooth(ctr, 21, 24)) * 1.0


def fit_feather():
    n = C
    v = np.zeros((n, n))
    for j in range(n):
        for i in range(n):
            dx = (i - n / 2 + 0.5) / (n / 2)
            dy = j / (n - 1)
            barb = 0.5 + 0.5 * math.sin((abs(dx) * 16 + dy * 22))
            v[j, i] = 0.6 + 0.3 * barb
            if abs(dx) < 0.05:
                v[j, i] = 1.0
            edge = 1 - abs(dx)
            v[j, i] *= 0.75 + 0.25 * min(1.0, edge * 3)
    return np.clip(v, 0.3, 1.0), np.zeros((n, n))


def _gem(n, sides, seed):
    x, y = grid(n)
    cx = cy = n / 2
    ang = np.arctan2(y - cy, x - cx)
    rad = np.hypot(x - cx, y - cy) / (n / 2)
    facet = np.floor((ang + math.pi) / (2 * math.pi) * sides)
    tone = rng(seed).uniform(0.55, 1.0, sides)[facet.astype(int) % sides]
    table = rad < 0.38
    v = np.where(table, 0.95, tone * (0.7 + 0.3 * (1 - rad)))
    edge = np.abs(rad - 0.38) < 0.03
    spokes = np.abs(((ang + math.pi) / (2 * math.pi) * sides % 1.0) - 0.0) < 0.03
    v = np.where(edge | (spokes & (rad < 0.95)), 1.0, v)
    v = np.where(rad > 0.97, 0.3, v)
    v[(np.abs(x - 22) < 3) & (np.abs(y - 20) < 3)] = 1.0
    return np.clip(v, 0.25, 1.0), np.clip(v * 1.05, 0, 1) * (rad < 0.98)


def fit_gem():
    return _gem(C, 6, 271)


def fit_gem2():
    return _gem(C, 8, 272)


def fit_rune_band():
    n = C
    x, y = grid(n)
    h = np.zeros((n, n))
    g = np.zeros((n, n))
    h += 0.4 * smooth(np.minimum(y, n - y), 0.0, 3.0)
    r = rng(281)
    for row in (20, 44):
        h -= 0.25 * ((np.abs(y - row) < 8) * 1.0)
        for gx in range(2, n - 8, 9):
            for _ in range(3):
                x0, y0 = gx + int(r.integers(0, 6)), row - 5 + int(r.integers(0, 10))
                dx, dy = [(1, 0), (0, 1), (1, 1), (1, -1)][int(r.integers(0, 4))]
                for k in range(int(r.integers(3, 6))):
                    xx, yy = x0 + dx * k, y0 + dy * k
                    if 0 <= xx < n and 0 <= yy < n:
                        g[yy, xx] = 1.0
    g = np.array(Image.fromarray((g * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.6))) / 255.0 * 1.8
    v = lit_flat(h, 1.6) * 0.9 + 0.05
    return np.clip(v, 0.2, 1.0), np.clip(g, 0, 1)


def fit_trim():
    n = C
    x, y = grid(n)
    h = np.zeros((n, n))
    key = ((np.floor(x / 8.0) + np.floor(y / 8.0)) % 2 == 0) & (np.minimum(y, n - y) > 10)
    h += 0.35 * key + 0.5 * smooth(np.minimum(y, n - y), 0.0, 6.0)
    pearl = smooth(2.0 - np.hypot((x % 8) - 4, (np.minimum(y, n - y) - 4)), 0.0, 1.2) * (np.minimum(y, n - y) < 8)
    h += 0.45 * pearl
    v = lit_flat(h, 1.8) * 0.95 + 0.05
    return np.clip(v, 0.3, 1.0), 0.15 * pearl


def fit_hem():
    n = C
    x, y = grid(n)
    h = np.zeros((n, n))
    scal = np.abs(((x % 16) - 8) / 8.0)
    edge = (y - 40 - 10 * (1 - np.sqrt(np.clip(1 - scal ** 2, 0, 1))))
    h += 0.4 * smooth(-edge, -1.0, 1.0)
    h += 0.4 * (np.abs(edge - 4) < 1.0) * (edge < 6)
    flower = smooth(2.5 - np.hypot((x % 16) - 8, y - 24), 0.0, 1.5)
    h += 0.35 * flower
    v = lit_flat(h, 1.8) * 0.95 + 0.05
    return np.clip(v, 0.3, 1.0), 0.5 * flower


def fit_iris():
    n = C
    x, y = grid(n)
    cx = cy = n / 2
    ang = np.arctan2(y - cy, x - cx)
    rad = np.hypot(x - cx, y - cy) / (n / 2)
    fibre = 0.5 + 0.5 * np.sin(ang * 22 + 3 * np.sin(ang * 5))
    v = np.where(rad < 0.28, 0.08, 0.55 + 0.35 * fibre * (1 - rad * 0.5))
    v = np.where((rad > 0.82) & (rad < 0.95), 0.25, v)
    v = np.where(rad >= 0.95, 0.9, v)
    g = np.where(rad < 0.28, 0.0, 0.5 + 0.5 * fibre) * (rad < 0.95) * (1 - 0.4 * smooth(rad, 0.7, 0.95))
    g[(np.abs(x - 24) < 4) & (np.abs(y - 22) < 4)] = 1.0
    return np.clip(v, 0.05, 1.0), np.clip(g, 0, 1)


def fit_sun():
    n = C
    x, y = grid(n)
    cx = cy = n / 2
    ang = np.arctan2(y - cy, x - cx)
    rad = np.hypot(x - cx, y - cy) / (n / 2)
    rays = np.clip(np.cos(ang * 12) * 0.5 + 0.5, 0, 1) ** 3
    disc = smooth(0.42 - rad, 0.0, 0.1)
    g = np.clip(disc + rays * (rad < 0.96) * (1 - rad) * 1.4 + 0.0, 0, 1)
    v = 0.6 + 0.4 * g
    return np.clip(v, 0.3, 1.0), g


def fit_ring_glow():
    n = C
    x, y = grid(n)
    rad = np.hypot(x - n / 2, y - n / 2) / (n / 2)
    g = np.exp(-((rad - 0.6) / 0.16) ** 2)
    return np.zeros((n, n)) + 0.05, np.clip(g, 0, 1)


def fit_soft():
    n = C
    x, y = grid(n)
    rad = np.hypot(x - n / 2, y - n / 2) / (n / 2)
    g = np.clip(1.0 - rad, 0, 1) ** 1.6
    return 0.2 + 0.8 * g, g


def fit_spark():
    n = C
    x, y = grid(n)
    cx = cy = n / 2
    rad = np.hypot(x - cx, y - cy) / (n / 2)
    sp = np.clip(1.0 - np.minimum(np.abs(x - cx), np.abs(y - cy)) / 3.0, 0, 1) * np.clip(1.0 - rad * 1.05, 0, 1) ** 0.5
    sp = np.maximum(sp, np.clip(1.0 - rad * 1.2, 0, 1) ** 2.2)
    return 0.2 + 0.8 * sp, np.clip(sp, 0, 1)


def fit_ember():
    n = C
    x, y = grid(n)
    em = np.clip(1.0 - y / n, 0, 1) ** 0.8 * (1 - 0.4 * np.abs(x - n / 2) / (n / 2))
    return 0.2 + 0.8 * em, np.clip(em, 0, 1)


def fit_pearl():
    n = C
    x, y = grid(n)
    rad = np.hypot(x - n / 2, y - n / 2) / (n / 2)
    pe = np.clip(1.0 - 0.55 * rad ** 2 + 0.3 * np.exp(-(((x - 22) ** 2 + (y - 20) ** 2) / 36.0)), 0.3, 1.0)
    return pe, 0.25 * pe


def fit_solid(val):
    def f():
        n = C
        x, y = grid(n)
        rad = np.hypot(x - n / 2, y - n / 2) / (n / 2)
        return np.clip(val * (0.88 + 0.12 * (1 - rad)), 0, 1), np.zeros((n, n))
    return f


def fit_banner():
    n = C
    x, y = grid(n)
    h = np.zeros((n, n))
    h += 0.5 * smooth(np.minimum(x, n - x), 0.0, 3.0)
    band = (np.abs(y - n / 2) < 3) * 0.4
    zig = smooth(1.2 - np.abs(((x % 12) - 6) - (y - 18) * 0.0 - np.where(y < 32, 0, 0)), 0.0, 0.8) * 0
    h += band
    v = lit_flat(h, 1.4) * 0.92 + 0.08
    return np.clip(v, 0.3, 1.0), 0.3 * band


def fit_pauldron():
    n = C
    x, y = grid(n)
    h = np.zeros((n, n))
    for k in range(5):                                          # five overlapping lames, each a lit arc
        yy = 8 + k * 11
        arc = yy + 3 * np.sin((x / n) * math.pi)
        lam = smooth(y - arc, 0.0, 1.0) * (1 - smooth(y - arc, 8.0, 10.0))
        h = np.maximum(h, lam * (0.45 + 0.1 * k) - 0.05 * (y - arc) * lam)
        h -= 0.3 * (np.abs(y - arc) < 1.0)
    rivets = np.zeros((n, n))
    for k in range(5):
        yy = 8 + k * 11 + 6
        for rx in (14, 32, 50):
            rivets += smooth(1.8 - np.hypot(x - rx, y - yy), 0.0, 1.0)
    h += 0.4 * rivets
    v = lit_flat(h, 2.2) * (0.9 + 0.1 * ao(h, 2)) + wear(n, 291, 0.5) * 0.4
    return np.clip(v, 0.25, 1.0), np.zeros((n, n))


def fit_link():
    n = C
    x, y = grid(n)
    rad = np.hypot(x - n / 2, (y - n / 2) * 1.0)
    ring = smooth(0.30 - np.abs(rad / (n / 2) - 0.68), 0.0, 0.2)
    h = ring
    v = lit_flat(h, 2.4) * 0.95 + 0.2 * ring
    return np.clip(v, 0.15, 1.0), np.zeros((n, n))


FIT_FNS = {"plate": fit_plate, "feather": fit_feather, "gem": fit_gem, "gem2": fit_gem2, "rune_band": fit_rune_band, "trim": fit_trim, "hem": fit_hem, "iris": fit_iris, "sun": fit_sun,
           "ring_glow": fit_ring_glow, "soft": fit_soft, "spark": fit_spark, "ember": fit_ember, "pearl": fit_pearl, "white": fit_solid(0.97), "black": fit_solid(0.09),
           "gold": fit_solid(0.9), "banner": fit_banner, "pauldron": fit_pauldron, "link": fit_link}


# ----------------------------------------------------------------------------- decals: drawn big, embossed, downsampled
S = 4


class Canv:
    """A supersampled pair of images: the relief (what is raised) and the glow (what shines)."""

    def __init__(self, n=C):
        self.n = n
        self.sz = n * S
        self.hi = Image.new("L", (self.sz, self.sz), 0)
        self.gi = Image.new("L", (self.sz, self.sz), 0)
        self.h = ImageDraw.Draw(self.hi)
        self.g = ImageDraw.Draw(self.gi)

    def p(self, pts):
        return [(x * self.sz, y * self.sz) for x, y in pts]

    def poly(self, pts, hv=255, gv=0):
        if hv:
            self.h.polygon(self.p(pts), fill=hv)
        if gv:
            self.g.polygon(self.p(pts), fill=gv)

    def line(self, pts, w=0.03, hv=255, gv=0):
        wi = max(1, int(w * self.sz))
        if hv:
            self.h.line(self.p(pts), fill=hv, width=wi, joint="curve")
        if gv:
            self.g.line(self.p(pts), fill=gv, width=wi, joint="curve")

    def ell(self, cx, cy, rx, ry, hv=255, gv=0, outline=False, w=0.03):
        box = [(cx - rx) * self.sz, (cy - ry) * self.sz, (cx + rx) * self.sz, (cy + ry) * self.sz]
        wi = max(1, int(w * self.sz))
        for d, v in ((self.h, hv), (self.g, gv)):
            if v:
                if outline:
                    d.ellipse(box, outline=v, width=wi)
                else:
                    d.ellipse(box, fill=v)

    def arc(self, cx, cy, r, a0, a1, w=0.03, hv=255, gv=0):
        box = [(cx - r) * self.sz, (cy - r) * self.sz, (cx + r) * self.sz, (cy + r) * self.sz]
        wi = max(1, int(w * self.sz))
        for d, v in ((self.h, hv), (self.g, gv)):
            if v:
                d.arc(box, a0, a1, fill=v, width=wi)

    def arrays(self):
        hi = self.hi.filter(ImageFilter.GaussianBlur(S * 0.5)).resize((self.n, self.n), Image.LANCZOS)
        gi = self.gi.filter(ImageFilter.GaussianBlur(S * 0.4)).resize((self.n, self.n), Image.LANCZOS)
        return np.asarray(hi, np.float64) / 255.0, np.asarray(gi, np.float64) / 255.0


def polar(cx, cy, r, a):
    return (cx + r * math.cos(a), cy + r * math.sin(a))


def m_star_burst(c, r):
    for k in range(16):
        a = k * math.pi / 8
        rr = 0.44 if k % 2 == 0 else 0.22
        a1, a2 = a - math.pi / 16, a + math.pi / 16
        c.poly([polar(.5, .5, 0.08, a1), polar(.5, .5, rr, a), polar(.5, .5, 0.08, a2)], 255, 200)
    c.ell(.5, .5, .1, .1, 255, 255)
    c.ell(.5, .5, .3, .3, 0, 120, outline=True, w=0.02)


def m_skull_crown(c, r):
    c.ell(.5, .56, .26, .24, 255, 150)
    c.poly([(.38, .78), (.62, .78), (.58, .9), (.42, .9)], 255, 0)
    c.ell(.4, .54, .06, .08, 0, 255)
    c.ell(.6, .54, .06, .08, 0, 255)
    c.poly([(.5, .6), (.47, .68), (.53, .68)], 0, 255)
    pts = [(.24, .36), (.28, .16), (.38, .3), (.5, .08), (.62, .3), (.72, .16), (.76, .36)]
    c.poly(pts, 255, 180)


def m_broken_seal(c, r):
    c.ell(.5, .5, .38, .38, 0, 200, outline=True, w=0.04)
    c.ell(.5, .5, .26, .26, 255, 140, outline=True, w=0.03)
    for k in range(12):
        a = k * math.pi / 6
        c.line([polar(.5, .5, .3, a), polar(.5, .5, .37, a)], 0.02, 255, 200)
    c.line([(.2, .8), (.42, .55), (.5, .6), (.58, .42), (.8, .2)], 0.05, 0, 255)
    c.poly([(.46, .46), (.54, .5), (.46, .56)], 255, 255)


def m_shadow_crown(c, r):
    pts = [(.15, .7), (.15, .3), (.3, .5), (.4, .12), (.5, .45), (.6, .12), (.7, .5), (.85, .3), (.85, .7)]
    c.poly(pts, 255, 150)
    c.poly([(.15, .74), (.85, .74), (.8, .86), (.2, .86)], 255, 0)
    for k in range(5):
        c.line([(.3 + .1 * k, .74), (.3 + .1 * k + 0.02 * ((k % 2) - 0.5), .55)], 0.02, 0, 255)
    c.ell(.5, .62, .06, .06, 0, 255)


def m_slime_face(c, r):
    c.poly([(.12, .75), (.2, .45), (.34, .3), (.5, .22), (.66, .3), (.8, .45), (.88, .75), (.78, .86), (.22, .86)], 255, 100)
    c.ell(.38, .52, .07, .1, 0, 255)
    c.ell(.62, .52, .07, .1, 0, 255)
    c.arc(.5, .6, .16, 20, 160, 0.03, 0, 255)
    c.ell(.3, .38, .04, .04, 0, 255)


def m_snowflake(c, r):
    for k in range(6):
        a = k * math.pi / 3
        c.line([polar(.5, .5, 0.0, a), polar(.5, .5, .42, a)], 0.04, 255, 255)
        for t, ln in ((.18, .1), (.3, .14)):
            bx, by = polar(.5, .5, t + 0.05, a)
            c.line([(bx, by), polar(bx, by, ln, a + 0.7)], 0.025, 255, 220)
            c.line([(bx, by), polar(bx, by, ln, a - 0.7)], 0.025, 255, 220)
    c.ell(.5, .5, .06, .06, 255, 255)


def m_dragon_face(c, r):
    c.poly([(.5, .1), (.64, .32), (.82, .22), (.7, .46), (.74, .66), (.6, .86), (.5, .9), (.4, .86), (.26, .66), (.3, .46), (.18, .22), (.36, .32)], 255, 80)
    c.poly([(.32, .5), (.44, .46), (.42, .52)], 0, 255)
    c.poly([(.68, .5), (.56, .46), (.58, .52)], 0, 255)
    c.line([(.5, .5), (.5, .74)], 0.03, 0, 255)
    c.ell(.44, .78, .02, .02, 0, 255)
    c.ell(.56, .78, .02, .02, 0, 255)


def m_tower_chain(c, r):
    c.poly([(.36, .88), (.36, .3), (.32, .3), (.32, .16), (.4, .16), (.4, .22), (.46, .22), (.46, .16), (.54, .16), (.54, .22), (.6, .22), (.6, .16), (.68, .16), (.68, .3), (.64, .3), (.64, .88)], 255, 120)
    c.poly([(.46, .88), (.46, .66), (.54, .66), (.54, .88)], 0, 255)
    c.poly([(.44, .42), (.48, .42), (.48, .5), (.44, .5)], 0, 255)
    for k in range(10):
        a = k * math.pi / 5
        c.ell(.5 + .42 * math.cos(a), .5 + .42 * math.sin(a), .045, .03, 255, 160, outline=True, w=0.02)


def m_celtic_knot(c, r):
    for k in range(3):
        a = k * math.pi / 3
        pts = [(.5 + .4 * math.cos(t) * math.cos(a) - .16 * math.sin(t) * math.sin(a), .5 + .4 * math.cos(t) * math.sin(a) + .16 * math.sin(t) * math.cos(a)) for t in np.linspace(0, 2 * math.pi, 40)]
        c.line(pts, 0.05, 255, 200)
    c.ell(.5, .5, .38, .38, 255, 0, outline=True, w=0.025)
    c.ell(.5, .5, .06, .06, 255, 255)


def m_mirror_diamond(c, r):
    for k, s in enumerate((.44, .34, .24, .14)):
        c.poly([(.5, .5 - s), (.5 + s * .8, .5), (.5, .5 + s), (.5 - s * .8, .5)], 255 if k % 2 == 0 else 120, 180 if k % 2 == 0 else 0)
    c.line([(.5, .06), (.5, .94)], 0.015, 0, 255)
    c.line([(.1, .5), (.9, .5)], 0.015, 0, 255)


def m_claw_x(c, r):
    for dx in (-0.16, 0.0, 0.16):
        c.poly([(.18 + dx, .1), (.26 + dx, .1), (.84 + dx, .9), (.76 + dx, .9)], 255, 200)
        c.poly([(.82 + dx, .1), (.74 + dx, .1), (.16 + dx, .9), (.24 + dx, .9)], 255, 100)


def m_dragon_crest(c, r):
    c.poly([(.5, .18), (.62, .5), (.5, .86), (.38, .5)], 255, 220)
    c.poly([(.62, .4), (.96, .22), (.84, .52), (.9, .7), (.66, .6)], 255, 150)
    c.poly([(.38, .4), (.04, .22), (.16, .52), (.1, .7), (.34, .6)], 255, 150)
    c.ell(.5, .5, .05, .05, 0, 255)


def m_wind_spiral(c, r):
    for arm in range(3):
        pts = []
        for t in np.linspace(0.1, 1.0, 30):
            a = t * 5.0 + arm * 2 * math.pi / 3
            pts.append(polar(.5, .5, .42 * t, a))
        c.line(pts, 0.05, 255, 220)
    c.ell(.5, .5, .05, .05, 255, 255)


def m_waves(c, r):
    for k in range(3):
        pts = [(x, .35 + .18 * k + .06 * math.sin(x * 12 + k)) for x in np.linspace(.08, .92, 30)]
        c.line(pts, 0.05, 255, 230)
    c.ell(.5, .22, .08, .08, 255, 255)


def m_hand_eye(c, r):
    c.poly([(.3, .9), (.28, .5), (.34, .3), (.4, .5), (.42, .2), (.48, .5), (.52, .14), (.58, .5), (.62, .2), (.66, .5), (.72, .32), (.72, .56), (.8, .52), (.74, .9)], 255, 60)
    c.poly([(.4, .62), (.5, .54), (.6, .62), (.5, .7)], 0, 255)
    c.ell(.5, .62, .03, .03, 255, 255)


def m_wing(c, r):
    for k in range(7):
        t = k / 6.0
        c.poly([(.1, .6 + 0.0), (.9 - 0.4 * t, .12 + .74 * t), (.9 - 0.4 * t - 0.06, .12 + .74 * t + .04), (.1, .64)], 255, 180 if k % 2 == 0 else 80)
    c.poly([(.06, .55), (.2, .5), (.2, .68), (.06, .68)], 255, 200)


def m_blood_drops(c, r):
    for (x, y, s) in ((.3, .35, .13), (.62, .28, .1), (.5, .66, .15), (.76, .62, .09)):
        c.poly([(x, y - s * 1.4), (x + s * .8, y + s * .2), (x, y + s), (x - s * .8, y + s * .2)], 255, 220)
        c.ell(x, y + s * .2, s * .85, s * .85, 255, 220)


def m_gear_vent(c, r):
    for k in range(10):
        a = k * math.pi / 5
        c.poly([polar(.5, .5, .36, a - 0.12), polar(.5, .5, .46, a - 0.07), polar(.5, .5, .46, a + 0.07), polar(.5, .5, .36, a + 0.12)], 255, 100)
    c.ell(.5, .5, .38, .38, 255, 0)
    c.ell(.5, .5, .22, .22, 0, 255, outline=True, w=0.04)
    for k in range(5):
        c.line([(.34, .36 + .07 * k), (.66, .36 + .07 * k)], 0.03, 0, 230)


def m_eye(c, r):
    c.poly([(.06, .5), (.28, .28), (.5, .22), (.72, .28), (.94, .5), (.72, .72), (.5, .78), (.28, .72)], 255, 80)
    c.ell(.5, .5, .17, .17, 0, 255)
    c.ell(.5, .5, .07, .07, 0, 0)
    for k in range(8):
        a = k * math.pi / 4
        c.line([polar(.5, .5, .24, a), polar(.5, .5, .3, a)], 0.015, 0, 200)


def m_cross_rays(c, r):
    c.ell(.5, .5, .24, .24, 255, 255, outline=True, w=0.05)
    for k in range(16):
        a = k * math.pi / 8
        ln = .46 if k % 2 == 0 else .36
        c.line([polar(.5, .5, .3, a), polar(.5, .5, ln, a)], 0.025, 255, 220)
    c.poly([(.46, .3), (.54, .3), (.54, .7), (.46, .7)], 0, 200)
    c.poly([(.3, .46), (.7, .46), (.7, .54), (.3, .54)], 0, 200)


def m_crown_glyph(c, r):
    c.poly([(.12, .72), (.16, .32), (.32, .5), (.5, .2), (.68, .5), (.84, .32), (.88, .72)], 255, 160)
    c.poly([(.12, .76), (.88, .76), (.84, .88), (.16, .88)], 255, 0)
    for x in (.25, .5, .75):
        c.ell(x, .6, .05, .05, 0, 255)


def m_six_eyes(c, r):
    for k in range(6):
        a = k * math.pi / 3 + math.pi / 6
        cx, cy = polar(.5, .5, .27, a)
        c.poly([(cx - .13, cy), (cx, cy - .07), (cx + .13, cy), (cx, cy + .07)], 255, 90)
        c.ell(cx, cy, .035, .035, 0, 255)
    c.ell(.5, .5, .05, .05, 255, 200)


def m_raven(c, r):
    c.poly([(.1, .5), (.3, .36), (.46, .34), (.6, .2), (.7, .3), (.9, .4), (.74, .46), (.86, .66), (.6, .6), (.48, .72), (.36, .62), (.2, .6)], 255, 70)
    c.ell(.68, .32, .025, .025, 0, 255)
    c.arc(.5, .5, .42, 200, 340, 0.03, 255, 230)


def m_sun_ankh(c, r):
    c.ell(.5, .3, .15, .15, 255, 255)
    for k in range(12):
        a = k * math.pi / 6
        c.line([polar(.5, .3, .2, a), polar(.5, .3, .28, a)], 0.025, 255, 230)
    c.ell(.5, .56, .09, .09, 255, 200, outline=True, w=0.04)
    c.poly([(.47, .62), (.53, .62), (.53, .9), (.47, .9)], 255, 200)
    c.poly([(.36, .72), (.64, .72), (.64, .78), (.36, .78)], 255, 200)


def m_edge_blades(c, r):
    for k in range(5):
        x = .14 + .18 * k
        c.poly([(x, .9), (x + .05, .16), (x + .1, .9)], 255, 200)
    c.line([(.08, .9), (.92, .9)], 0.04, 255, 150)


def m_hex_glass(c, r):
    for k in range(7):
        cx, cy = (.5, .5) if k == 0 else polar(.5, .5, .28, (k - 1) * math.pi / 3)
        c.poly([polar(cx, cy, .13, a * math.pi / 3 + math.pi / 6) for a in range(6)], 255 if k % 2 == 0 else 150, 180)


def m_pact_chain(c, r):
    for k in range(5):
        c.ell(.2 + .15 * k, .5 + .07 * (k % 2), .09, .06, 255, 150, outline=True, w=0.035)
    c.line([(.5, .2), (.46, .45), (.54, .6), (.5, .85)], 0.03, 0, 255)


def m_star_staff(c, r):
    c.line([(.5, .12), (.5, .9)], 0.04, 255, 220)
    c.poly([(.5, .06), (.56, .2), (.7, .2), (.58, .3), (.63, .44), (.5, .35), (.37, .44), (.42, .3), (.3, .2), (.44, .2)], 255, 255)
    c.ell(.5, .8, .1, .1, 255, 180, outline=True, w=0.03)


MOTIFS = {"star_burst": m_star_burst, "skull_crown": m_skull_crown, "broken_seal": m_broken_seal, "shadow_crown": m_shadow_crown, "slime_face": m_slime_face, "snowflake": m_snowflake,
          "dragon_face": m_dragon_face, "tower_chain": m_tower_chain, "celtic_knot": m_celtic_knot, "mirror_diamond": m_mirror_diamond, "claw_x": m_claw_x, "dragon_crest": m_dragon_crest,
          "wind_spiral": m_wind_spiral, "waves": m_waves, "hand_eye": m_hand_eye, "wing": m_wing, "blood_drops": m_blood_drops, "gear_vent": m_gear_vent, "eye": m_eye,
          "cross_rays": m_cross_rays, "crown_glyph": m_crown_glyph, "six_eyes": m_six_eyes, "raven": m_raven, "sun_ankh": m_sun_ankh, "edge_blades": m_edge_blades,
          "hex_glass": m_hex_glass, "pact_chain": m_pact_chain, "star_staff": m_star_staff}


def emboss(hh, gg, base=0.78, strength=2.6, glow_boost=1.0):
    v = lit_flat(hh, strength, base=base) * (0.7 + 0.3 * (1.0 - 0.5 * np.clip(ao_flat(hh), 0, 1))) * 1.12
    v = np.where(hh < 0.02, base * 0.86, v)
    return np.clip(v, 0.2, 1.0), np.clip(gg * glow_boost, 0, 1)


def ao_flat(h):
    b = np.asarray(Image.fromarray((np.clip(h, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.5)), np.float64) / 255.0
    return np.clip(b - h, 0, 1) * 1.5


def decal_crest(motif):
    c = Canv()
    MOTIFS[motif](c, rng(1))
    hh, gg = c.arrays()
    # a plate behind the emblem so it reads as a medallion
    x, y = grid(C)
    rad = np.hypot(x - C / 2, y - C / 2) / (C / 2)
    base = smooth(1.0 - rad, 0.0, 0.08) * 0.35
    hh = np.clip(hh * 0.8 + base, 0, 1)
    a, g = emboss(hh, gg)
    a = np.where(rad > 1.0, 0.6, a)
    return a, g


def decal_panel(motif, seed):
    """A plate whose face carries the motif small, 3 times, with bevel and rivets."""
    n = C
    c = Canv()
    MOTIFS[motif](c, rng(seed))
    hh, gg = c.arrays()
    small = np.asarray(Image.fromarray((hh * 255).astype(np.uint8)).resize((22, 22), Image.LANCZOS), np.float64) / 255.0
    gs = np.asarray(Image.fromarray((gg * 255).astype(np.uint8)).resize((22, 22), Image.LANCZOS), np.float64) / 255.0
    H = np.zeros((n, n))
    G = np.zeros((n, n))
    for (ox, oy) in ((2, 2), (40, 2), (21, 22), (2, 40), (40, 40)):
        if ox + 22 <= n and oy + 22 <= n:
            H[oy:oy + 22, ox:ox + 22] = np.maximum(H[oy:oy + 22, ox:ox + 22], small * 0.8)
            G[oy:oy + 22, ox:ox + 22] = np.maximum(G[oy:oy + 22, ox:ox + 22], gs)
    x, y = grid(n)
    b = np.minimum(np.minimum(x, n - x), np.minimum(y, n - y))
    H += smooth(b, 0.0, 2.5) * 0.35
    H = np.clip(H, 0, 1)
    a, g = emboss(H, G * 0.9)
    return a, g


def decal_border(kind, seed):
    n = C
    x, y = grid(n)
    h = np.zeros((n, n))
    g = np.zeros((n, n))
    t = np.minimum(y, n - y)
    h += smooth(t, 0.0, 2.5) * 0.4
    k = kind % 6
    if k == 0:                                                         # greek key
        key = ((np.floor(x / 8.0) % 2 == 0) & (np.abs(t - 14) < 6)) | ((np.floor(x / 8.0) % 2 == 1) & (np.abs(t - 14) < 2))
        h += 0.5 * key
        g += 0.5 * (np.abs(t - 14) < 1.1)
    elif k == 1:                                                       # thorns
        th = (t - 10) < 14 * np.abs(((x % 12) - 6) / 6.0) * -1 + 14
        h += 0.5 * ((t > 8) & (t < 8 + 16 * (1 - np.abs(((x % 12) - 6) / 6.0))))
        g += 0.5 * ((t > 8) & (t < 10))
    elif k == 2:                                                       # scallops
        sc = np.hypot((x % 12) - 6, t - 8) < 5
        h += 0.5 * sc
        g += 0.6 * (np.abs(np.hypot((x % 12) - 6, t - 8) - 5) < 0.9)
    elif k == 3:                                                       # chain links
        lk = np.abs(np.hypot(((x % 14) - 7) * 0.8, t - 14) - 5) < 1.7
        h += 0.5 * lk
        g += 0.5 * lk * (((x // 14) % 2) == 0)
    elif k == 4:                                                       # waves
        w = np.abs(t - 14 - 5 * np.sin(x / 64.0 * 2 * math.pi * 4)) < 2.4
        h += 0.5 * w
        g += 0.55 * w
    else:                                                              # diamonds
        d = (np.abs((x % 12) - 6) + np.abs(t - 14)) < 5
        h += 0.5 * d
        g += 0.5 * (np.abs((np.abs((x % 12) - 6) + np.abs(t - 14)) - 5) < 1.0)
    a, gg = emboss(np.clip(h, 0, 1), np.clip(g, 0, 1), base=0.8, strength=2.0)
    return a, gg


def decal_runes(seed, rows=3):
    n = C
    r = rng(seed)
    c = Canv()
    for row in range(rows):
        y0 = (row + 0.5) / rows
        for gx in range(7):
            x0 = 0.07 + gx * 0.13
            for _ in range(3):
                a = r.uniform(0, math.pi)
                ln = r.uniform(0.04, 0.09)
                cx, cy = x0 + r.uniform(0, 0.05), y0 + r.uniform(-0.06, 0.06)
                c.line([(cx, cy), (cx + ln * math.cos(a), cy + ln * math.sin(a))], 0.018, 255, 255)
    hh, gg = c.arrays()
    x, y = grid(n)
    band = 0.25 * smooth(np.minimum(y, n - y), 0.0, 2.0)
    a, g = emboss(np.clip(hh * 0.6 + band, 0, 1), gg * 1.1, base=0.55)
    return a, g


def decal_filigree(seed):
    r = rng(seed)
    c = Canv()
    for k in range(4):
        cx, cy = r.uniform(.2, .8), r.uniform(.2, .8)
        pts = []
        for t in np.linspace(0, 1, 40):
            a = t * 9.0 + k
            rr = 0.02 + 0.2 * t
            pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
        c.line(pts, 0.025, 255, 255)
    for _ in range(6):
        c.ell(r.uniform(.1, .9), r.uniform(.1, .9), .02, .02, 255, 255)
    hh, gg = c.arrays()
    a, g = emboss(hh, gg, base=0.8, strength=2.8)
    return a, g


def decal_surface(motif, seed):
    """A tileable weave with the motif small, repeating: the 'embroidered' surface of the garment."""
    n = PERIOD
    c = Canv(24)
    MOTIFS[motif](c, rng(seed))
    hh, gg = c.arrays()
    H = np.tile(hh, (2, 2))
    G = np.tile(gg, (2, 2))
    weave = mat_weave2(n)[0]
    base = fbm(n, seed + 5, 3, 3)
    h2 = np.clip(H * 0.7 + 0.15 * base, 0, 1)
    v = lit(h2, 2.2) * (0.78 + 0.18 * weave) + 0.04
    return np.clip(v, 0.25, 1.0), np.clip(G * 0.85, 0, 1)


# ----------------------------------------------------------------------------- composition
_LIB = {}


def library():
    if _LIB:
        return _LIB
    for name in TILE_NAMES:
        a, g = TILE_FNS[name]()
        _LIB[name] = (a, g, True)
    for name in FIT_NAMES:
        a, g = FIT_FNS[name]()
        _LIB[name] = (a, g, False)
    return _LIB


def _place(alb, glo, idx, a, g, tile):
    cx, cy = (idx % COLS) * C, (idx // COLS) * C
    if tile:
        n = PERIOD
        jj, kk = np.mgrid[0:C, 0:C]
        sa = a[(jj - GUT) % n, (kk - GUT) % n]
        sg = g[(jj - GUT) % n, (kk - GUT) % n]
    else:
        sa, sg = a, g
    alb[cy:cy + C, cx:cx + C, :3] = np.clip(sa, 0, 1)[..., None]
    alb[cy:cy + C, cx:cx + C, 3] = 1.0
    glo[cy:cy + C, cx:cx + C, :3] = np.clip(sg, 0, 1)[..., None]


def compose(spec):
    """spec: dict with 'crest' (motif name), 'panel' (motif), 'border' (int kind), 'trim2' (int kind), 'runes' (seed), 'filigree' (seed), 'surface' (motif), 'seed' (int).
    Returns (albedo RGBA float 512 x 512, glow RGB float 512 x 512)."""
    lib = library()
    alb = np.zeros((SKIN, SKIN, 4))
    glo = np.zeros((SKIN, SKIN, 3))
    for name in LIBRARY:
        a, g, tile = lib[name]
        _place(alb, glo, CELL_INDEX[name], a, g, tile)
    seed = int(spec.get("seed", 1))
    d = {}
    d["crest"] = decal_crest(spec["crest"])
    d["panel"] = decal_panel(spec["panel"], seed + 1)
    d["border"] = decal_border(spec["border"], seed + 2)
    d["trim2"] = decal_border(spec["trim2"], seed + 3)
    d["rune_a"] = decal_runes(seed + 4, 3)
    d["rune_b"] = decal_runes(seed + 5, 2)
    d["filigree"] = decal_filigree(seed + 6)
    sa, sg = decal_surface(spec["surface"], seed + 7)
    d["surface"] = (sa, sg)
    for name in DECALS:
        a, g = d[name]
        _place(alb, glo, CELL_INDEX[name], a, g, name == "surface")
    # spare cells: a copy of the plate and gem so a bleed to the neighbours never shows an unpainted cell
    for idx in range(56, 64):
        alb[(idx // COLS) * C:(idx // COLS + 1) * C, (idx % COLS) * C:(idx % COLS + 1) * C, 3] = 0.0
    return alb, glo


def write_skin(rid, spec, outdir):
    alb, glo = compose(spec)
    os.makedirs(outdir, exist_ok=True)
    Image.fromarray((np.clip(alb, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(outdir, rid + ".png"), optimize=True)
    Image.fromarray((np.clip(glo, 0, 1) * 255 + 0.5).astype(np.uint8), "RGB").save(os.path.join(outdir, rid + "_glow.png"), optimize=True)
    return alb, glo


if __name__ == "__main__":
    a, g = write_skin("_test", {"crest": "star_burst", "panel": "claw_x", "border": 0, "trim2": 3, "surface": "star_staff", "seed": 7}, os.path.join(HERE, "_out"))
    print("ok", a.shape)

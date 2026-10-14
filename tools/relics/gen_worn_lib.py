"""gen_worn_lib: the shared kit of the worn-relic model library (client/relics/worn/lib).

Every worn relic model is built here with the owner's method (tools/relics/mesh_lib.py: named parts, lofts, lathes, smoothed normals) and written as a
small text file the game parses once and caches: assets/nusmp/relic_worn/<model>.txt. The colours are NOT painted into the models: a quad names a colour
SLOT (main, trim, glow, dark, accent, none) and a material TILE of one shared, tileable, grey-ish atlas (assets/nusmp/textures/relic_worn/atlas.png), and the
game tints the tile with the relic's palette. One model therefore serves every relic of its look, and the signature relics only change the numbers.

Authoring frame (Y UP, front is -Z, 1 unit = 1 model pixel = 1/16 block), relative to the pivot of the body part the model is attached to:
    head   origin at the neck, the head box is x -4..4, y 0..8, z -4..4
    body   origin at the neck line, the torso is x -4..4, y -12..0, z -2..2
    rarm   origin at the right shoulder pivot, the arm is x -3..1, y -10..2, z -2..2 (the left arm is the same mirrored by the catalog)
    rleg   origin at the hip pivot, the leg is x -2..2, y -12..0, z -2..2
The writer flips y into the game's layer space (y down) and keeps the smoothed normals outward.

File format (one record per line, everything else is a comment):
    model <name>                        anchor <head|body|rarm|larm|rleg|lleg>
    grp <idx> <name> <kind> px py pz a b c      animation group idx >= 1 (kind: bob spin swing sway pulse wave orbit), pivot in layer space
    q <grp> <slot> <flags> x y z u v nx ny nz   (x4)     slot 0 main 1 trim 2 glow 3 dark 4 accent 5 none; flags 1 emissive 2 shell(rim/shimmer) 4 hot (pulses)
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml  # noqa: E402
from mesh_lib import Part, loft, lathe, sweep, smooth_normals, circle, octagon, rect_ring, ellipse, bevelled, lozenge  # noqa: E402

ROOT = ml.ROOT
ASSETS = ml.ASSETS
WORN_DIR = os.path.join(ASSETS, "relic_worn")
TEX_DIR = os.path.join(ASSETS, "textures", "relic_worn")

# ---------------------------------------------------------------- slots, flags, tiles
MAIN, TRIM, GLOW, DARK, ACCENT, NONE = range(6)
EMIT, SHELL, HOT = 1, 2, 4

CELL, COLS, ROWS = 32, 4, 8
ATLAS_W, ATLAS_H = CELL * COLS, CELL * ROWS
PERIOD = 24                       # a tileable tile is PERIOD texels wide inside its cell, with a (CELL - PERIOD) / 2 gutter of wrapped content all round
GUT = (CELL - PERIOD) // 2

TILES = ["metal", "gilt", "cloth", "leather", "gem", "feather", "fur", "horn", "chain", "plate", "rune", "flame", "white", "duck", "orange", "black",
         "scale", "bone", "crystal", "soft", "spark", "ember", "honey", "snow", "wood", "stripe", "silk", "moth", "pearl", "vein", "paper", "gold_fixed"]
TILE = {n: i for i, n in enumerate(TILES)}
TILEABLE = {"metal", "gilt", "cloth", "leather", "fur", "horn", "chain", "rune", "flame", "scale", "bone", "honey", "snow", "wood", "stripe", "silk", "vein", "paper"}

# ================================================================ the atlas
def _rng(seed):
    return np.random.default_rng(seed)


def pnoise(n, period, seed, octaves=3, base=2):
    """Tileable noise: a sum of sinusoids with integer wave numbers over `period` texels, sampled on an n x n grid; roughly -1..1."""
    r = _rng(seed)
    y, x = np.mgrid[0:n, 0:n].astype(np.float64)
    out = np.zeros((n, n))
    amp = 1.0
    tot = 0.0
    for o in range(octaves):
        for _ in range(4):
            kx, ky = int(r.integers(-base * (o + 1), base * (o + 1) + 1)), int(r.integers(-base * (o + 1), base * (o + 1) + 1))
            if kx == 0 and ky == 0:
                kx = 1
            out += amp * np.sin(2 * math.pi * (kx * x + ky * y) / period + r.uniform(0, 2 * math.pi))
            tot += amp
        amp *= 0.55
    return out / tot


def _fit01(a, lo=0.0, hi=1.0):
    a = a - a.min()
    m = a.max()
    return lo + (hi - lo) * (a / m if m > 1e-9 else a)


def _tile_arrays():
    """name -> (rgb float 0..1 array [h, w, 3], alpha) painted on the full CELL x CELL cell (tileable ones are painted on PERIOD and wrapped)."""
    P = PERIOD
    tiles = {}

    def rgb(v):
        return np.repeat(v[..., None], 3, axis=2)

    # metal: brushed streaks + a few scratches
    n = pnoise(P, P, 11)
    streak = np.tile(_rng(12).normal(0, 0.06, (P, 1)), (1, P))
    v = 0.84 + 0.10 * n + streak
    r = _rng(13)
    for _ in range(5):
        x0, y0 = int(r.integers(0, P)), int(r.integers(0, P))
        for k in range(int(r.integers(5, 10))):
            v[(y0 + k // 2) % P, (x0 + k) % P] += 0.12 if r.random() < 0.5 else -0.14
    tiles["metal"] = rgb(np.clip(v, 0.45, 1.0))

    # gilt: bright engraved lattice (diamond lines + dots)
    y, x = np.mgrid[0:P, 0:P]
    d1 = ((x + y) % 8 == 0) | ((x - y) % 8 == 0)
    dots = ((x % 8 == 4) & (y % 8 == 0)) | ((x % 8 == 0) & (y % 8 == 4))
    v = 0.95 + 0.04 * pnoise(P, P, 21)
    v[d1] -= 0.17
    v[dots] += 0.06
    tiles["gilt"] = rgb(np.clip(v, 0.4, 1.0))

    # cloth: weave and soft vertical folds
    weave = 0.07 * (((x % 2) ^ (y % 2)).astype(float) - 0.5)
    fold = 0.12 * np.sin(2 * math.pi * x / P * 2 + 0.7 * np.sin(2 * math.pi * y / P))
    v = 0.84 + weave + fold + 0.05 * pnoise(P, P, 31)
    tiles["cloth"] = rgb(np.clip(v, 0.45, 1.0))

    # leather: pebbled grain + a stitched seam along the top row
    g = pnoise(P, P, 41, octaves=3, base=4)
    v = 0.78 + 0.16 * g
    for xx in range(0, P, 4):
        v[2, xx:xx + 2] = 0.98
        v[3, xx:xx + 2] = 0.55
    tiles["leather"] = rgb(np.clip(v, 0.4, 1.0))

    # fur / wool: dense soft fibres
    v = 0.80 + 0.2 * pnoise(P, P, 51, octaves=3, base=6) + 0.08 * _rng(52).normal(0, 1, (P, P))
    tiles["fur"] = rgb(np.clip(v, 0.4, 1.0))

    # horn: growth ridges across the length (v) with a long grain
    v = 0.78 + 0.16 * np.sin(2 * math.pi * y / P * 3) + 0.06 * pnoise(P, P, 61) + 0.05 * np.sin(2 * math.pi * x / P * 2)
    tiles["horn"] = rgb(np.clip(v, 0.4, 1.0))

    # chain: interlocked rings
    v = np.full((P, P), 0.45)
    for cx, cy, horiz in ((6, 6, True), (18, 6, False), (6, 18, False), (18, 18, True)):
        for yy in range(P):
            for xx in range(P):
                dx, dy = ((xx - cx + P / 2) % P) - P / 2, ((yy - cy + P / 2) % P) - P / 2
                e = (dx / 6.0) ** 2 + (dy / (4.0 if horiz else 6.0)) ** 2 if horiz else (dx / 4.0) ** 2 + (dy / 6.0) ** 2
                if 0.45 < e < 1.0:
                    v[yy, xx] = 0.98 - 0.35 * abs(e - 0.72) * 3
    tiles["chain"] = rgb(np.clip(v + 0.03 * pnoise(P, P, 71), 0.3, 1.0))

    # scale: overlapping rounded scales
    v = np.zeros((P, P))
    for yy in range(P):
        for xx in range(P):
            row = yy // 6
            ox = 4 if row % 2 else 0
            dx = (xx + ox) % 8 - 4
            dy = yy % 6
            e = (dx / 4.0) ** 2 + (dy / 6.0) ** 2
            v[yy, xx] = 0.98 - 0.45 * min(1.0, e) ** 2 - (0.25 if dy == 5 else 0.0)
    tiles["scale"] = rgb(np.clip(v + 0.03 * pnoise(P, P, 81), 0.3, 1.0))

    # bone / ivory: smooth, with fine cracks
    v = 0.90 + 0.06 * pnoise(P, P, 91)
    r = _rng(92)
    for _ in range(3):
        x0, y0 = int(r.integers(0, P)), int(r.integers(0, P))
        for k in range(8):
            v[(y0 + k) % P, (x0 + int(2 * math.sin(k))) % P] -= 0.15
    tiles["bone"] = rgb(np.clip(v, 0.5, 1.0))

    # honeycomb
    v = np.full((P, P), 0.95)
    for yy in range(P):
        for xx in range(P):
            ox = 6 if (yy // 6) % 2 else 0
            dx, dy = (xx + ox) % 12 - 6, yy % 6 - 3
            if abs(dx) + abs(dy) * 1.7 > 5.2:
                v[yy, xx] = 0.45
    tiles["honey"] = rgb(np.clip(v + 0.04 * pnoise(P, P, 101), 0.3, 1.0))

    # snow / frost: soft with sparkling flecks
    v = 0.90 + 0.06 * pnoise(P, P, 111)
    r = _rng(112)
    for _ in range(14):
        v[int(r.integers(0, P)), int(r.integers(0, P))] = 1.0
    tiles["snow"] = rgb(np.clip(v, 0.5, 1.0))

    # wood grain
    v = 0.74 + 0.16 * np.sin(2 * math.pi * (x / P * 2 + 0.6 * pnoise(P, P, 121))) + 0.04 * pnoise(P, P, 122)
    tiles["wood"] = rgb(np.clip(v, 0.4, 1.0))

    # stripe (towel / banner): bold bands in two tones across
    v = np.where((y // 6) % 2 == 0, 0.98, 0.62).astype(float) + 0.03 * pnoise(P, P, 131)
    tiles["stripe"] = rgb(np.clip(v, 0.4, 1.0))

    # silk: long sheen
    v = 0.86 + 0.12 * np.sin(2 * math.pi * (y / P * 1.0 + 0.3 * np.sin(2 * math.pi * x / P))) + 0.03 * pnoise(P, P, 141)
    tiles["silk"] = rgb(np.clip(v, 0.5, 1.0))

    # vein (organic / wing membrane): branching lines on a light base
    v = 0.92 + 0.04 * pnoise(P, P, 151)
    for xx in range(P):
        v[(int(5 * math.sin(xx / P * 2 * math.pi * 2)) + P // 2) % P, xx] -= 0.30
        v[(int(5 * math.cos(xx / P * 2 * math.pi * 3)) + 4) % P, xx] -= 0.22
    tiles["vein"] = rgb(np.clip(v, 0.4, 1.0))

    # paper / parchment
    v = 0.92 + 0.05 * pnoise(P, P, 161, octaves=3, base=4)
    tiles["paper"] = rgb(np.clip(v, 0.55, 1.0))

    # rune (glow): tileable glyph strokes on black
    v = np.zeros((P, P))
    r = _rng(171)
    for gy in range(0, P, 8):
        for gx in range(0, P, 8):
            for _ in range(3):
                x0, y0 = gx + int(r.integers(1, 7)), gy + int(r.integers(1, 7))
                dx, dy = [(1, 0), (0, 1), (1, 1), (1, -1)][int(r.integers(0, 4))]
                for k in range(int(r.integers(2, 5))):
                    v[(y0 + dy * k) % P, (x0 + dx * k) % P] = 1.0
    tiles["rune"] = rgb(v * 0.95)

    # flame (glow): vertical licks, bright at the bottom of v
    v = np.zeros((P, P))
    for xx in range(P):
        h = 0.5 + 0.5 * math.sin(xx / P * 2 * math.pi * 3 + 1.1) * math.sin(xx / P * 2 * math.pi * 5)
        for yy in range(P):
            v[yy, xx] = max(0.0, 1.0 - (yy / P) * (1.2 + 0.5 * h)) * (0.7 + 0.3 * h)
    tiles["flame"] = rgb(np.clip(v, 0, 1))

    # ---- the fitted tiles are painted on the whole cell
    C = CELL
    yy, xx = np.mgrid[0:C, 0:C].astype(float)
    cx = cy = (C - 1) / 2
    ang = np.arctan2(yy - cy, xx - cx)
    rad = np.hypot(xx - cx, yy - cy) / (C / 2)

    # gem: faceted star facets, bright rim, white glint
    facet = 0.55 + 0.35 * np.cos(ang * 4 + 0.4) * 0.5 + 0.5 * (1 - np.clip(rad, 0, 1)) * 0.5
    facet = 0.5 + 0.45 * (0.5 + 0.5 * np.cos(ang * 6)) * (0.6 + 0.4 * (1 - rad))
    g = np.clip(facet + 0.25 * (1 - np.clip(rad * 1.4, 0, 1)), 0.25, 1.0)
    g[(abs(xx - 11) < 2) & (abs(yy - 10) < 2)] = 1.0
    tiles["gem"] = rgb(g)

    # feather: rachis down the middle and barbs sweeping up and out
    v = np.zeros((C, C))
    for j in range(C):
        for i in range(C):
            dx = (i - cx) / (C / 2)
            dy = j / (C - 1)
            barb = 0.5 + 0.5 * math.sin((abs(dx) * 14 + dy * 18))
            v[j, i] = 0.62 + 0.3 * barb
            if abs(dx) < 0.06:
                v[j, i] = 1.0
    tiles["feather"] = rgb(v)

    # plate: bevelled panel with a dark border and rivets
    v = np.full((C, C), 0.86)
    b = np.minimum(np.minimum(xx, C - 1 - xx), np.minimum(yy, C - 1 - yy))
    v[b < 2] = 0.50
    v[(b >= 2) & (b < 4)] = 0.98
    v += 0.05 * np.tile(pnoise(C, C, 181), (1, 1))
    for (rx, ry) in ((6, 6), (25, 6), (6, 25), (25, 25)):
        v[(abs(xx - rx) < 1.6) & (abs(yy - ry) < 1.6)] = 1.0
        v[(abs(xx - rx - 1) < 1) & (abs(yy - ry - 1) < 1)] = 0.55
    tiles["plate"] = rgb(np.clip(v, 0.3, 1.0))

    # crystal: long clear facets
    f = 0.62 + 0.30 * np.sin(ang * 3 + rad * 3) * 0.5 + 0.4 * (1 - rad)
    tiles["crystal"] = rgb(np.clip(f, 0.35, 1.0))

    # pearl: soft sphere shading
    pe = np.clip(1.0 - 0.55 * rad ** 2 + 0.25 * np.exp(-(((xx - 11) ** 2 + (yy - 10) ** 2) / 18.0)), 0.3, 1.0)
    tiles["pearl"] = rgb(pe)

    # soft: round glow falloff
    tiles["soft"] = rgb(np.clip(1.0 - rad, 0, 1) ** 1.6)

    # spark: four-point star
    sp = np.clip(1.0 - np.minimum(abs(xx - cx), abs(yy - cy)) / 2.2, 0, 1) * np.clip(1.0 - rad * 1.1, 0, 1) ** 0.5
    sp = np.maximum(sp, np.clip(1.0 - rad * 1.2, 0, 1) ** 2.2)
    tiles["spark"] = rgb(np.clip(sp, 0, 1))

    # ember: gradient from hot yellow-white at the base to dim at the tip
    em = np.clip(1.0 - yy / C, 0, 1) ** 0.8
    tiles["ember"] = rgb(em)

    # fixed colours
    one = np.ones((C, C))
    sh = 0.93 + 0.07 * np.cos(ang) * (rad > 0)
    tiles["white"] = rgb(np.clip(one * 0.97, 0, 1))
    duck = np.zeros((C, C, 3))
    duck[...] = (1.0, 0.83, 0.26)
    duck *= (0.9 + 0.1 * (1 - yy / C))[..., None]
    tiles["duck"] = duck
    orange = np.zeros((C, C, 3))
    orange[...] = (1.0, 0.55, 0.12)
    tiles["orange"] = orange * (0.88 + 0.12 * (1 - yy / C))[..., None]
    tiles["black"] = np.zeros((C, C, 3)) + np.array([0.07, 0.07, 0.10])
    gold = np.zeros((C, C, 3))
    gold[...] = (1.0, 0.82, 0.38)
    tiles["gold_fixed"] = gold * (0.82 + 0.18 * (1 - rad)[..., None]) if False else gold * (0.80 + 0.2 * np.clip(1 - rad, 0, 1))[..., None]
    # the moth wing tile: dusty bands of a light tone with eye-spots
    mo = 0.86 + 0.08 * np.sin(yy / 3.0)
    mo[((xx - 16) ** 2 + (yy - 16) ** 2) < 30] = 0.35
    mo[((xx - 16) ** 2 + (yy - 16) ** 2) < 10] = 1.0
    tiles["moth"] = rgb(np.clip(mo, 0.3, 1.0))
    return tiles


def build_atlas():
    """The body atlas (RGBA, ATLAS_W x ATLAS_H): tileable tiles wrapped into a gutter, fitted tiles over the whole cell."""
    tiles = _tile_arrays()
    img = np.zeros((ATLAS_H, ATLAS_W, 4), np.float64)
    for name, i in TILE.items():
        cx0, cy0 = (i % COLS) * CELL, (i // COLS) * CELL
        t = tiles[name]
        if name in TILEABLE:
            for j in range(CELL):
                for k in range(CELL):
                    img[cy0 + j, cx0 + k, :3] = t[(j - GUT) % PERIOD, (k - GUT) % PERIOD]
        else:
            img[cy0:cy0 + CELL, cx0:cx0 + CELL, :3] = t
        img[cy0:cy0 + CELL, cx0:cx0 + CELL, 3] = 1.0
    return img


def build_shimmer():
    """A 64 x 64 tileable shimmer texture (soft diagonal bands and glints, additive-friendly): the shell pass scrolls it over the worn model."""
    n = 64
    y, x = np.mgrid[0:n, 0:n].astype(float)
    band = 0.5 + 0.5 * np.sin(2 * math.pi * (x + y) / n * 2)
    band2 = 0.5 + 0.5 * np.sin(2 * math.pi * (x * 3 - y * 2) / n)
    nz = pnoise(n, n, 201, octaves=3, base=3)
    v = np.clip(0.30 * band ** 3 + 0.30 * band2 ** 4 + 0.35 * (0.5 + 0.5 * nz) ** 3, 0, 1)
    r = _rng(202)
    for _ in range(26):
        v[int(r.integers(0, n)), int(r.integers(0, n))] = 1.0
    rgba = np.zeros((n, n, 4))
    rgba[..., :3] = v[..., None]
    rgba[..., 3] = 1.0
    return rgba


def write_textures():
    os.makedirs(TEX_DIR, exist_ok=True)
    a = build_atlas()
    Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "RGBA").save(os.path.join(TEX_DIR, "atlas.png"))
    s = build_shimmer()
    Image.fromarray((np.clip(s, 0, 1) * 255).astype(np.uint8), "RGBA").save(os.path.join(TEX_DIR, "shimmer.png"))
    return a, s


# ================================================================ geometry helpers (authoring frame, y up)
RN = 12                          # ring segments of a band: the vertex budget of a worn model is about 1000, so rings stay coarse and the smoothed normals do the rest

def sph(part, c, r, lat=5, lon=8, squash=1.0):
    """A sphere / spheroid as a lathe round y (poles closed)."""
    prof = [(-r * squash * math.cos(math.pi * i / lat), r * math.sin(math.pi * i / lat)) for i in range(lat + 1)]
    prof = [(s, max(rr, 0.0)) for s, rr in prof]
    return lathe(part, "y", c, prof, sides=lon)


def dia(part, c, rx, ry, rz, up=None):
    """An octahedron (a faceted bead / shard) of half sizes rx, ry, rz at c: 8 triangles, the cheapest gem there is."""
    c = np.array(c, float)
    t, b = part.add_vertex(c + (0, ry, 0)), part.add_vertex(c + (0, -ry, 0))
    eq = [part.add_vertex(c + d) for d in ((rx, 0, 0), (0, 0, rz), (-rx, 0, 0), (0, 0, -rz))]
    for i in range(4):
        j = (i + 1) % 4
        part.add_face(eq[i], eq[j], t, t)
        part.add_face(eq[j], eq[i], b, b)
    part.ensure_outward()
    return part


def ring_pts(a, b, n, y=0.0, phase=0.0):
    return [(a * math.cos(phase + 2 * math.pi * i / n), y, b * math.sin(phase + 2 * math.pi * i / n)) for i in range(n)]


def _sq(th, a, b, p):
    """A point of the superellipse of half sizes (a, b) at angle th (p = 2 an ellipse, 4 a rounded square): the head is a box, so crowns follow it."""
    c, s = math.cos(th), math.sin(th)
    e = 2.0 / p
    return a * math.copysign(abs(c) ** e, c), b * math.copysign(abs(s) ** e, s)


def band(part, y0, y1, a, b, t, n=24, taper_top=0.0, flare=0.0, p=3.0, flare_b=None):
    """A closed band round the y axis: a superellipse ring (half sizes a, b; exponent p) from y0 to y1, `t` thick; taper_top narrows the top edge
    (a crown rim), flare moves the top outward. Built as rings of 4 points bridged all round."""
    rings = []
    fb = flare if flare_b is None else flare_b
    for i in range(n):
        th = 2 * math.pi * i / n
        pts = []
        for (u, v, fl) in [(0.0, y0, 0.0), (t, y0, 0.0), (t - taper_top, y1, flare), (0.0, y1, flare)]:
            x, z = _sq(th, a + u + fl, b + u + (fl if flare_b is None else fb), p)
            pts.append(part.add_vertex((x, v, z)))
        rings.append(ml.Ring(pts))
    for i in range(n):
        j = (i + 1) % n
        for k in range(4):
            l = (k + 1) % 4
            part.add_face(rings[i][k], rings[j][k], rings[j][l], rings[i][l])
    part.ensure_outward()
    return rings


def torus(part, c, R, r, n=12, m=4, a_scale=1.0, b_scale=1.0, plane="xz"):
    """A ring (torus) with centre c, major radius R, minor r; plane 'xz' (a halo / crown rim, axis y), 'xy' (a medallion ring, axis z) or 'yz'."""
    rings = []
    for i in range(n):
        th = 2 * math.pi * i / n
        ca, sa = math.cos(th), math.sin(th)
        pts = []
        for k in range(m):
            ph = 2 * math.pi * k / m
            rr = R + r * math.cos(ph)
            u, w = rr * ca * a_scale, r * math.sin(ph)
            v = rr * sa * b_scale
            if plane == "xz":
                p = (c[0] + u, c[1] + w, c[2] + v)
            elif plane == "xy":
                p = (c[0] + u, c[1] + v, c[2] + w)
            else:
                p = (c[0] + w, c[1] + u, c[2] + v)
            pts.append(part.add_vertex(p))
        rings.append(ml.Ring(pts))
    for i in range(n):
        j = (i + 1) % n
        for k in range(m):
            l = (k + 1) % m
            part.add_face(rings[i][k], rings[j][k], rings[j][l], rings[i][l])
    part.ensure_outward()
    return rings


def cone(part, base, tip, r, sides=5, mid=0.6, mid_r=0.55, cap=False, squash=1.0):
    """A spike / horn / spire from a base centre to a tip: a loft along the base->tip vector with a bulge profile."""
    b, t = np.array(base, float), np.array(tip, float)
    L = float(np.linalg.norm(t - b))
    if L < 1e-6:
        return None
    st = [(0.0, 1.0, 1.0 * squash), (L * mid, mid_r, mid_r * squash), (L, 0.0, 0.0)]
    return loft(part, t - b, b, st, circle(r, sides), caps=(cap, True))


def horn(part, pts, r0, r1, sides=6, up=None):
    """A curved horn / tusk / claw along a polyline (a Bezier sample of a few points), tapering from r0 to r1 and closing at the tip."""
    n = len(pts)
    scales = []
    for i in range(n):
        t = i / (n - 1)
        k = (1 - t) * 1.0 + t * (r1 / r0 if r0 > 0 else 0)
        scales.append((max(k, 0.0) if i < n - 1 else 0.0,) * 2)
    return sweep(part, pts, circle(r0, sides), scales=scales, caps=(True, True), up=up)


def bez(p0, p1, p2, n=6):
    out = []
    for i in range(n + 1):
        t = i / n
        out.append(tuple((1 - t) ** 2 * a + 2 * (1 - t) * t * b + t * t * c for a, b, c in zip(p0, p1, p2)))
    return out


def bez3(p0, p1, p2, p3, n=7):
    out = []
    for i in range(n + 1):
        t = i / n
        out.append(tuple((1 - t) ** 3 * a + 3 * (1 - t) ** 2 * t * b + 3 * (1 - t) * t * t * c + t ** 3 * d for a, b, c, d in zip(p0, p1, p2, p3)))
    return out


def tube(part, pts, r, sides=6, scales=None):
    """A round bar along a polyline (a chain run, a wire, a cord) with optional per-point scales."""
    sc = scales if scales is not None else [(1.0, 1.0)] * len(pts)
    return sweep(part, pts, circle(r, sides), scales=sc, caps=(True, True))


def beads(part, pts, r, sides=6, every=1, bead=1.0, thin=0.45):
    """A beaded chain: a tube whose radius swells at every `every`-th point."""
    sc = [((bead if i % every == 0 else thin),) * 2 for i in range(len(pts))]
    return sweep(part, pts, circle(r, sides), scales=sc, caps=(True, True))


def sheet(part, fn, nu, nv, thick=0.5, close_edges=False):
    """A thick cloth sheet from a surface function fn(u, v) -> (x, y, z), u and v in 0..1, nu x nv quads. The back surface is `thick` behind the front along the
    surface normal (a negative thickness puts it on the other side). Each face is wound to look away from the middle of the sheet, so the lighting is right on both sides;
    with `close_edges` the four rims are closed too (a solid), without them a sheet costs 2 quads a cell."""
    def P(i, j):
        return np.array(fn(i / nu, j / nv), float)

    front = [[part.add_vertex(P(i, j)) for i in range(nu + 1)] for j in range(nv + 1)]
    back = []
    for j in range(nv + 1):
        row = []
        for i in range(nu + 1):
            a = P(min(i + 1, nu), j) - P(max(i - 1, 0), j)
            b = P(i, min(j + 1, nv)) - P(i, max(j - 1, 0))
            n = np.cross(a, b)
            ln = np.linalg.norm(n)
            n = n / ln if ln > 1e-9 else np.array([0, 0, 1.0])
            row.append(part.add_vertex(P(i, j) + n * thick))
        back.append(row)

    def quad(face, back_side, i, j):
        ids = (face[j][i], face[j][i + 1], face[j + 1][i + 1], face[j + 1][i])
        nrm = np.zeros(3)
        for k in range(4):
            nrm += np.cross(part.V[ids[k]], part.V[ids[(k + 1) % 4]])
        off = (np.array(part.V[back[j][i]]) - np.array(part.V[front[j][i]]))
        sign = float(np.dot(nrm, off))
        wrong = sign > 0 if not back_side else sign < 0
        if wrong:
            ids = (ids[0], ids[3], ids[2], ids[1])
        part.add_face(*ids)

    for j in range(nv):
        for i in range(nu):
            quad(front, False, i, j)
            quad(back, True, i, j)
    if close_edges:
        for j in range(nv):
            part.add_face(front[j][0], front[j + 1][0], back[j + 1][0], back[j][0])
            part.add_face(front[j][nu], back[j][nu], back[j + 1][nu], front[j + 1][nu])
        for i in range(nu):
            part.add_face(front[0][i], back[0][i], back[0][i + 1], front[0][i + 1])
            part.add_face(front[nv][i], front[nv][i + 1], back[nv][i + 1], back[nv][i])
        part.ensure_outward()
    return part


def quad_strip(part, pts_left, pts_right, thick=0.4, edges=False):
    """A flat strip (a feather vane, a wing finger, a ribbon) between two polylines, `thick` thick along z: a front face (looking to -z) and a back face
    (looking to +z), and with `edges` the thin rims too. Two faces are enough for a vane, so a feather costs 2 quads a segment."""
    n = len(pts_left)
    L = [np.array(p, float) for p in pts_left]
    R = [np.array(p, float) for p in pts_right]
    fr = [(part.add_vertex(L[i]), part.add_vertex(R[i])) for i in range(n)]
    bk = [(part.add_vertex(L[i] + (0, 0, thick)), part.add_vertex(R[i] + (0, 0, thick))) for i in range(n)]
    nrm = np.cross(R[0] - L[0], L[1] - L[0]) if n > 1 else np.zeros(3)
    flip = nrm[2] > 0                         # make the front face look to -z whatever order the polylines came in
    for i in range(n - 1):
        f = (fr[i][0], fr[i][1], fr[i + 1][1], fr[i + 1][0])
        b = (bk[i][0], bk[i + 1][0], bk[i + 1][1], bk[i][1])
        if flip:
            f, b = (f[0], f[3], f[2], f[1]), (b[0], b[3], b[2], b[1])
        part.add_face(*f)
        part.add_face(*b)
        if edges:
            part.add_face(fr[i][0], fr[i + 1][0], bk[i + 1][0], bk[i][0])
            part.add_face(fr[i][1], bk[i][1], bk[i + 1][1], fr[i + 1][1])
    return part


def feather(part, base, tip, w, thick=0.35, n=5, curl=0.0, dz=0.0):
    """One feather: a leaf-shaped vane from base to tip (a lens outline of 2n+1 points), a spine bulge down the middle."""
    b, t = np.array(base, float), np.array(tip, float)
    d = t - b
    L = float(np.linalg.norm(d))
    if L < 1e-6:
        return part
    ax = d / L
    side = np.cross(ax, np.array([0, 0, 1.0]))
    if np.linalg.norm(side) < 1e-6:
        side = np.array([1.0, 0, 0])
    side /= np.linalg.norm(side)
    left, right = [], []
    for i in range(n + 1):
        s = i / n
        wd = w * math.sin(math.pi * min(1.0, s * 0.92 + 0.04)) ** 0.8
        c = b + ax * L * s + side * curl * s * s * L
        c = c + np.array([0, 0, dz * s])
        left.append(tuple(c - side * wd))
        right.append(tuple(c + side * wd))
    return quad_strip(part, left, right, thick)


# ================================================================ the model container and the writer
class WModel:
    """A worn model: parts (each with a colour slot, a tile, flags, a group) and the animation groups."""

    def __init__(self, name, anchor):
        self.name, self.anchor = name, anchor
        self.items = []                 # (part, slot, tile, flags, group, uv_mode, density)
        self.groups = [None]            # index 0 is the static base; others (name, kind, pivot (x,y,z) in authoring frame, a, b, c)

    def group(self, name, kind, pivot=(0, 0, 0), a=0.0, b=0.0, c=0.0):
        self.groups.append((name, kind, tuple(pivot), a, b, c))
        return len(self.groups) - 1

    def add(self, part, slot=MAIN, tile="metal", flags=0, group=0, uv="tile", density=4.0, smooth=40.0):
        if part is None:
            return None
        if not part.F:
            return None
        part.smooth = smooth
        self.items.append((part, slot, tile, flags, group, uv, density))
        return part

    def quad_count(self):
        return sum(len(p.F) for (p, *_r) in self.items)

    def vertex_count(self):
        return 4 * self.quad_count()


def _dom(n):
    ax = int(np.argmax(np.abs(n)))
    return ax, (1 if n[ax] >= 0 else -1)


def _ab(ax, p):
    x, y, z = p
    return (z, y) if ax == 0 else ((x, z) if ax == 1 else (x, y))


def _quad_uv(part, f, tile, uv_mode, density, fit_box):
    """UVs (atlas 0..1) for the 4 corners of face f of a part: tileable tiles repeat by density (texels per model pixel) with the quad centred in its
    period; fitted tiles stretch over the part's extent on that face's dominant axis."""
    idx = part.F[f]
    P = [part.V[i] for i in idx]
    n = part.face_normal(f)
    ax, sg = _dom(n)
    ab = np.array([_ab(ax, p) for p in P])
    ti = TILE[tile]
    cx0, cy0 = (ti % COLS) * CELL, (ti // COLS) * CELL
    if tile in TILEABLE and uv_mode == "tile":
        c = (ab.max(axis=0) + ab.min(axis=0)) / 2.0                 # the middle of the quad's extent (a triangle's corners are not centred on their mean)
        span = (ab.max(axis=0) - ab.min(axis=0)) * density
        k = np.ones(2)
        for d in range(2):
            if span[d] > PERIOD:
                k[d] = PERIOD / span[d]
        fr = (c * density) % PERIOD
        half = span * k / 2.0                              # keep the whole quad inside the cell: the period plus its wrapped gutter
        fr = np.clip(fr, half - GUT + 0.01, PERIOD + GUT - half - 0.01)
        out = []
        for q in ab:
            off = (q - c) * density * k
            u, v = fr + off
            out.append((cx0 + GUT + u, cy0 + GUT + v))
    else:
        lo, hi = fit_box[ax]
        ext = np.maximum(hi - lo, 1e-6)
        out = []
        for q in ab:
            t = (q - lo) / ext
            if ax == 1:
                pass
            out.append((cx0 + 2 + t[0] * (CELL - 4), cy0 + 2 + (1.0 - t[1]) * (CELL - 4)))
    return [(u / ATLAS_W, v / ATLAS_H) for (u, v) in out]


def write_model(model, path=None):
    """Writes the model file; returns its vertex count."""
    path = path or os.path.join(WORN_DIR, model.name + ".txt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    lines = [f"# worn relic model {model.name}: {model.quad_count()} quads, {model.vertex_count()} vertices; generated by tools/relics/gen_worn_*.py, do not edit",
             f"model {model.name}", f"anchor {model.anchor}"]
    for gi in range(1, len(model.groups)):
        name, kind, pv, a, b, c = model.groups[gi]
        lines.append("grp %d %s %s %.3f %.3f %.3f %.4f %.4f %.4f" % (gi, name, kind, pv[0], -pv[1], pv[2], a, b, c))
    quads = []
    for (part, slot, tile, flags, group, uvm, dens) in model.items:
        part.ensure_outward()
        smooth_normals(part, part.smooth)
        P = np.array(part.V)
        fit_box = {}
        for f in range(len(part.F)):
            pass
        # per dominant axis extent over the part's faces (for the fitted tiles)
        ext = {0: [None, None], 1: [None, None], 2: [None, None]}
        for f, idx in enumerate(part.F):
            ax, _sg = _dom(part.face_normal(f))
            ab = np.array([_ab(ax, part.V[i]) for i in idx])
            lo, hi = ab.min(axis=0), ab.max(axis=0)
            e = ext[ax]
            e[0] = lo if e[0] is None else np.minimum(e[0], lo)
            e[1] = hi if e[1] is None else np.maximum(e[1], hi)
        fit_box = {ax: (np.array(e[0]), np.array(e[1])) for ax, e in ext.items() if e[0] is not None}
        for f, idx in enumerate(part.F):
            uv = _quad_uv(part, f, tile, uvm, dens, fit_box)
            rec = []
            for k in range(4):
                p = part.V[idx[k]]
                nrm = part.N[f][k]
                rec.append((p[0], -p[1], p[2], uv[k][0], uv[k][1], nrm[0], -nrm[1], nrm[2]))
            rec = [rec[0], rec[3], rec[2], rec[1]]          # the y flip into layer space reverses the winding: the game wants counter-clockwise from outside
            quads.append((group, slot, flags, rec))
    for (g, slot, flags, rec) in quads:
        lines.append("q %d %d %d %s" % (g, slot, flags, " ".join("%.3f %.3f %.3f %.4f %.4f %.3f %.3f %.3f" % r for r in rec)))
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return model.vertex_count()


# ================================================================ the catalog (look / relic -> pieces; relic -> fx theme)
class Catalog:
    def __init__(self):
        self.looks, self.relics, self.fx = [], [], []
        self.items, self.ipal, self.itier, self.islot = [], [], [], []          # armour items (and modes) by id: pieces, palette (main trim glow), grade tier

    def look(self, slot, look, *pieces):
        self.looks.append((slot, look, pieces))

    def relic(self, rid, *pieces):
        self.relics.append((rid, pieces))

    def theme(self, rid, theme):
        self.fx.append((rid, theme))

    def item(self, key, pieces, palette=None, tier=None):
        """An armour item (by registry path) or a mode (mode_<name>): the models it wears, its palette (main, trim, glow as 0xRRGGBB) and its grade tier (1..7)."""
        self.items.append((key, tuple(pieces)))
        if palette is not None:
            self.ipal.append((key, tuple(palette)))
        if tier is not None:
            self.itier.append((key, int(tier)))

    def slot_of(self, key, slot):
        """A Curios-worn item that is not a relic: the slot it is drawn in (CHARM, BACK, BELT, HANDS, NECK)."""
        self.islot.append((key, slot))

    def write(self, path=None):
        path = path or os.path.join(WORN_DIR, "catalog.txt")
        out = ["# worn relic catalog: which models a relic wears. generated by tools/relics/gen_worn_all.py, do not edit.",
               "# look <SLOT> <LOOK> <piece...>   the models of every relic of that slot and look (piece = model[@anchor][~ mirrored])",
               "# relic <id> <piece...>           a signature relic's own models (replace the look's)",
               "# fx <id> <theme>                 ambient particle theme (ember snow spark leaf wisp star bee petal)",
               "# item <path> <piece...>          an armour item of the mod (or mode_<name>): the models it wears on the player",
               "# ipal <path> <main> <trim> <glow>  its palette (RRGGBB hex); itier <path> <n>  its grade tier 1..7 (rim, motes, particles)",
               "# islot <path> <SLOT>             a Curios-worn item that is not a relic: the slot it is drawn in"]
        for s, l, ps in self.looks:
            out.append("look %s %s %s" % (s, l, " ".join(ps)))
        for r, ps in self.relics:
            out.append("relic %s %s" % (r, " ".join(ps)))
        for r, t in self.fx:
            out.append("fx %s %s" % (r, t))
        for k, ps in self.items:
            out.append("item %s %s" % (k, " ".join(ps)))
        for k, (a, b, c) in self.ipal:
            out.append("ipal %s %06X %06X %06X" % (k, a, b, c))
        for k, t in self.itier:
            out.append("itier %s %d" % (k, t))
        for k, sl in self.islot:
            out.append("islot %s %s" % (k, sl))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", newline="\n") as fh:
            fh.write("\n".join(out) + "\n")

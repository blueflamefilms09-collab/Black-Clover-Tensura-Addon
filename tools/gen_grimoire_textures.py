#!/usr/bin/env python3
"""
Generates the greyscale texture set for the procedural 3D grimoire system (0.20).

Output: src/main/resources/assets/nusmp/textures/item/grimoire3d/*.png  (32 files)

Every cover / trim / insignia / aura texture is GREYSCALE: the colour comes from the item's
GrimoireAppearance through tintindex 0..3, so ~32 files on disk yield 100k+ visual identities.
Only pages.png is coloured (cream paper, never tinted).

Run from the project root:   python tools/gen_grimoire_textures.py
Needs: Pillow, numpy. Deterministic (fixed seeds) - re-running rewrites identical files.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..",
                   "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire3d")
os.makedirs(OUT, exist_ok=True)

COVER_W, COVER_H = 24, 34      # cover art, same 10:14 ratio as the 3D board
INS = 24                       # insignia canvas
written = []


def save_grey(name, grey, alpha=None):
    """grey: float array 0..1 (h,w); alpha: float array 0..1 or None (opaque)."""
    g = np.clip(grey, 0, 1)
    h, w = g.shape
    px = np.zeros((h, w, 4), dtype=np.uint8)
    v = (g * 255 + 0.5).astype(np.uint8)
    px[..., 0] = v
    px[..., 1] = v
    px[..., 2] = v
    px[..., 3] = 255 if alpha is None else (np.clip(alpha, 0, 1) * 255 + 0.5).astype(np.uint8)
    Image.fromarray(px, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)
    written.append(name)


def save_rgb(name, rgb, alpha=None):
    h, w, _ = rgb.shape
    px = np.zeros((h, w, 4), dtype=np.uint8)
    px[..., :3] = (np.clip(rgb, 0, 1) * 255 + 0.5).astype(np.uint8)
    px[..., 3] = 255 if alpha is None else (np.clip(alpha, 0, 1) * 255 + 0.5).astype(np.uint8)
    Image.fromarray(px, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)
    written.append(name)


def noise(rng, h, w, cell):
    """Smooth-ish value noise in 0..1 (bilinear upsample of a coarse random grid)."""
    gh, gw = h // cell + 2, w // cell + 2
    g = rng.random((gh, gw))
    ys = np.linspace(0, gh - 2, h)
    xs = np.linspace(0, gw - 2, w)
    y0, x0 = ys.astype(int), xs.astype(int)
    fy, fx = (ys - y0)[:, None], (xs - x0)[None, :]
    a = g[y0][:, x0]
    b = g[y0][:, x0 + 1]
    c = g[y0 + 1][:, x0]
    d = g[y0 + 1][:, x0 + 1]
    return a * (1 - fx) * (1 - fy) + b * fx * (1 - fy) + c * (1 - fx) * fy + d * fx * fy


def border_dist(h, w):
    ys, xs = np.mgrid[0:h, 0:w]
    return np.minimum.reduce([xs, ys, w - 1 - xs, h - 1 - ys])


def bevel(grey, mask, light=0.14, dark=0.16):
    """Lit top/left edge, shaded bottom/right edge of a mask region."""
    up = np.roll(mask, 1, axis=0)
    up[0, :] = False
    left = np.roll(mask, 1, axis=1)
    left[:, 0] = False
    down = np.roll(mask, -1, axis=0)
    down[-1, :] = False
    right = np.roll(mask, -1, axis=1)
    right[:, -1] = False
    lit = mask & (~up | ~left)
    shd = mask & (~down | ~right)
    out = grey.copy()
    out[lit] += light
    out[shd] -= dark
    return out


def rect_mask(h, w, x0, y0, x1, y1):
    m = np.zeros((h, w), dtype=bool)
    m[max(y0, 0):min(y1, h), max(x0, 0):min(x1, w)] = True
    return m


# --------------------------------------------------------------------------- covers (layer 0)
def cover_base(rng, base, grain=0.045, blotch=0.05):
    h, w = COVER_H, COVER_W
    g = np.full((h, w), base)
    g += (rng.random((h, w)) - 0.5) * 2 * grain
    g += (noise(rng, h, w, 6) - 0.5) * 2 * blotch
    return g


def frame_edges(g):
    h, w = g.shape
    d = border_dist(h, w)
    g = g.copy()
    g[d == 0] = 0.50
    g[d == 1] = 0.64
    return g


def cover_plain():
    rng = np.random.default_rng(101)
    g = cover_base(rng, 0.80)
    h, w = g.shape
    frame = rect_mask(h, w, 4, 4, w - 4, h - 4) & ~rect_mask(h, w, 5, 5, w - 5, h - 5)
    g = bevel(g, frame, 0.10, 0.18)
    g = frame_edges(g)
    save_grey("cover_plain", g)


def cover_stitched():
    rng = np.random.default_rng(102)
    g = cover_base(rng, 0.76, grain=0.06)
    h, w = g.shape
    # leather streaks
    g += (np.tile(rng.random((1, w)), (h, 1)) - 0.5) * 0.05
    for (x0, y0, x1, y1) in [(3, 3, w - 3, h - 3)]:
        for x in range(x0, x1):
            on = (x // 2) % 2 == 0
            for y in (y0, y1 - 1):
                if on:
                    g[y, x] = 0.97
                    if y + 1 < h:
                        g[y + 1, x] = min(g[y + 1, x], 0.52)
        for y in range(y0, y1):
            on = (y // 2) % 2 == 0
            for x in (x0, x1 - 1):
                if on:
                    g[y, x] = 0.97
                    if x + 1 < w:
                        g[y, x + 1] = min(g[y, x + 1], 0.52)
    save_grey("cover_stitched", frame_edges(g))


def cover_dragonhide():
    rng = np.random.default_rng(103)
    h, w = COVER_H, COVER_W
    g = np.zeros((h, w))
    cw, ch = 5, 4
    for y in range(h):
        for x in range(w):
            row = y // ch
            off = (cw // 2) if row % 2 else 0
            lx = (x + off) % cw
            ly = y % ch
            dx = (lx - (cw - 1) / 2) / (cw / 2)
            dy = (ly - 0.0) / ch
            dist = math.sqrt(dx * dx * 0.85 + dy * dy * 1.1)
            v = 0.95 - 0.40 * min(dist, 1.0)
            if dist > 0.93:
                v = 0.38
            g[y, x] = v
    g += (rng.random((h, w)) - 0.5) * 0.07
    g += (noise(rng, h, w, 8) - 0.5) * 0.08
    d = border_dist(h, w)
    g[d == 0] = 0.34
    g[d == 1] = 0.5
    save_grey("cover_dragonhide", g)


def cover_tattered():
    rng = np.random.default_rng(104)
    g = cover_base(rng, 0.70, grain=0.08, blotch=0.12)
    h, w = g.shape
    im = Image.fromarray((np.clip(g, 0, 1) * 255).astype(np.uint8), "L")
    dr = ImageDraw.Draw(im)
    for _ in range(16):                      # scratches
        x, y = int(rng.integers(0, w)), int(rng.integers(0, h))
        ln = int(rng.integers(4, 12))
        ang = rng.random() * math.pi
        dr.line([x, y, x + int(math.cos(ang) * ln), y + int(math.sin(ang) * ln)],
                fill=int(255 * (0.38 if rng.random() < 0.6 else 0.93)))
    for (cx, cy) in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:   # worn corners
        for _ in range(26):
            dr.point([cx + int(rng.normal(0, 3)), cy + int(rng.normal(0, 3))], fill=int(255 * (0.9 if rng.random() < 0.5 else 0.45)))
    for _ in range(5):                       # stains
        x, y = int(rng.integers(3, w - 3)), int(rng.integers(3, h - 3))
        r = int(rng.integers(2, 5))
        dr.ellipse([x - r, y - r, x + r, y + r], outline=int(255 * 0.52))
    g = np.array(im).astype(float) / 255
    d = border_dist(h, w)
    frayed = (d <= 1) & (rng.random((h, w)) < 0.45)
    g[d == 0] = 0.46
    g[frayed] = 0.30
    save_grey("cover_tattered", g)


def cover_metallic():
    """The 'ornate' cover (rare luck covers): fine leather with a quiet embossed panel. The gilded border ornaments are a
    separate overlay (trim_border) drawn in the trim metal colour, so the gold stays gold on any cover colour."""
    rng = np.random.default_rng(105)
    g = cover_base(rng, 0.78, grain=0.04)
    h, w = g.shape
    panel = rect_mask(h, w, 6, 6, w - 6, h - 6)
    inner = rect_mask(h, w, 7, 7, w - 7, h - 7)
    g = bevel(g, panel & ~inner, 0.10, 0.20)
    save_grey("cover_metallic", frame_edges(g))


def trim_border():
    """Intricate ornaments around the cover border (alpha overlay, tinted with the trim metal): double rule, dotted band between,
    plus-shaped flourishes in the corners and a small diamond in the middle of each side."""
    h, w = COVER_H, COVER_W
    m = np.zeros((h, w), dtype=bool)

    def outline(x0, y0, x1, y1):
        m[y0, x0:x1] = True
        m[y1 - 1, x0:x1] = True
        m[y0:y1, x0] = True
        m[y0:y1, x1 - 1] = True

    outline(1, 1, w - 1, h - 1)
    outline(4, 4, w - 4, h - 4)
    for x in range(6, w - 6, 3):
        m[2, x] = m[h - 3, x] = True
    for y in range(6, h - 6, 3):
        m[y, 2] = m[y, w - 3] = True

    def plus(cx, cy, r):
        for d in range(-r, r + 1):
            if 0 <= cx + d < w:
                m[cy, cx + d] = True
            if 0 <= cy + d < h:
                m[cy + d, cx] = True

    for (cx, cy) in [(3, 3), (w - 4, 3), (3, h - 4), (w - 4, h - 4)]:
        plus(cx, cy, 2)
        m[cy - 1:cy + 2, cx - 1:cx + 2] = True
    for (cx, cy) in [(w // 2, 2), (w // 2, h - 3), (2, h // 2), (w - 3, h // 2)]:
        plus(cx, cy, 1)
    g = bevel(np.full((h, w), 0.90), m, 0.10, 0.38)
    save_grey("trim_border", g, m.astype(float))


# --------------------------------------------------------------------------- trim metals (layer 1)
def trim_plain():
    rng = np.random.default_rng(201)
    h = w = 16
    g = 0.86 + (np.tile(rng.random((h, 1)), (1, w)) - 0.5) * 0.10
    edge = rect_mask(h, w, 0, 0, w, h)
    g = bevel(g, edge, 0.14, 0.30)
    g[7:9, 7:9] = 0.45
    g[7, 7] = 1.0
    save_grey("trim_plain", g)


def trim_heavy():
    rng = np.random.default_rng(202)
    h = w = 16
    ys, xs = np.mgrid[0:h, 0:w]
    g = 0.68 + (((xs + ys) % 4 == 0) * 0.10) + (((xs - ys) % 4 == 0) * 0.07)
    g += (rng.random((h, w)) - 0.5) * 0.08
    g = bevel(g, rect_mask(h, w, 0, 0, w, h), 0.18, 0.30)
    for (x, y) in [(2, 2), (12, 2), (2, 12), (12, 12)]:
        g[y:y + 2, x:x + 2] = 0.38
        g[y, x] = 0.98
    pits = rng.random((h, w)) < 0.05
    g[pits] = 0.35
    save_grey("trim_heavy", g)


def trim_runed():
    rng = np.random.default_rng(203)
    h = w = 16
    g = 0.80 + (rng.random((h, w)) - 0.5) * 0.08
    g = bevel(g, rect_mask(h, w, 0, 0, w, h), 0.14, 0.26)
    im = Image.fromarray((np.clip(g, 0, 1) * 255).astype(np.uint8), "L")
    dr = ImageDraw.Draw(im)
    rune = int(255 * 0.30)
    glow = int(255 * 1.0)
    # two engraved glyph rows
    strokes = [
        [(3, 3, 3, 7), (3, 3, 6, 5), (6, 5, 3, 7)],
        [(8, 3, 8, 7), (8, 5, 11, 3), (8, 5, 11, 7)],
        [(13, 3, 13, 7), (11, 5, 13, 5)],
        [(3, 9, 6, 13), (6, 9, 3, 13)],
        [(8, 9, 11, 9), (9, 9, 9, 13), (11, 9, 11, 13)],
        [(13, 9, 13, 13), (12, 11, 14, 11)],
    ]
    for glyph in strokes:
        for (x0, y0, x1, y1) in glyph:
            dr.line([x0, y0, x1, y1], fill=rune)
            dr.point([x0 + 1, y0 + 1] if x0 == x1 else [x0, y0 + 1], fill=glow)
    save_grey("trim_runed", np.array(im).astype(float) / 255)


# --------------------------------------------------------------------------- clasps
def clasp_strap():
    rng = np.random.default_rng(301)
    h = w = 16
    g = 0.50 + (np.tile(rng.random((h, 1)), (1, w)) - 0.5) * 0.10 + (rng.random((h, w)) - 0.5) * 0.06
    g[0, :] = 0.28
    g[h - 1, :] = 0.28
    for x in range(0, w, 3):
        g[2, x:x + 2] = 0.78
        g[h - 3, x:x + 2] = 0.78
        g[3, x:x + 2] = 0.34
        g[h - 2, x:x + 2] = 0.34
    save_grey("clasp_strap", g)


def clasp_buckle():
    h = w = 16
    g = np.full((h, w), 0.20)
    ring = rect_mask(h, w, 1, 1, w - 1, h - 1) & ~rect_mask(h, w, 4, 4, w - 4, h - 4)
    g[ring] = 0.92
    g = bevel(g, ring, 0.08, 0.34)
    g[7:9, 3:13] = 0.88              # prong bar
    g[7, 3:13] = 1.0
    g[9, 3:13] = 0.5
    save_grey("clasp_buckle", g)


def clasp_chain():
    h = w = 16
    g = np.full((h, w), 0.12)
    im = Image.fromarray((g * 255).astype(np.uint8), "L")
    dr = ImageDraw.Draw(im)
    for i, x in enumerate(range(-2, w, 6)):          # alternating flat / edge-on links
        if i % 2 == 0:
            dr.ellipse([x, 3, x + 8, 12], outline=int(255 * 0.92), width=2)
        else:
            dr.rectangle([x + 1, 6, x + 7, 9], fill=int(255 * 0.80))
    a = np.array(im).astype(float) / 255
    a = bevel(a, a > 0.5, 0.06, 0.25)
    save_grey("clasp_chain", a)


def clasp_lock():
    h = w = 16
    g = np.linspace(0.96, 0.66, h)[:, None] * np.ones((1, w))
    g = bevel(g, rect_mask(h, w, 0, 0, w, h), 0.06, 0.28)
    im = Image.fromarray((np.clip(g, 0, 1) * 255).astype(np.uint8), "L")
    dr = ImageDraw.Draw(im)
    dr.ellipse([6, 4, 9, 7], fill=int(255 * 0.12))      # keyhole
    dr.polygon([(7, 7), (8, 7), (9, 12), (6, 12)], fill=int(255 * 0.12))
    for (x, y) in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        dr.point([x, y], fill=int(255 * 0.35))
    save_grey("clasp_lock", np.array(im).astype(float) / 255)


# --------------------------------------------------------------------------- pages (coloured)
def pages():
    rng = np.random.default_rng(401)
    h = w = 16
    base = np.array([0.93, 0.89, 0.78])
    rgb = np.ones((h, w, 3)) * base
    lines = (np.arange(h) % 2 == 0)[:, None] * np.ones((1, w))
    rgb *= (1 - 0.07 * lines[..., None])
    rgb *= (1 + (rng.random((h, w, 1)) - 0.5) * 0.06)
    rgb[:, 0] *= 0.82
    rgb[:, w - 1] *= 0.86
    save_rgb("pages", rgb)


# --------------------------------------------------------------------------- insignia (layer 2)
S = 8   # supersample


def new_canvas():
    return Image.new("L", (INS * S, INS * S), 0)


def poly(draw, pts, fill=255):
    draw.polygon([(x * S, y * S) for x, y in pts], fill=fill)


def heart_pts(cx, cy, size, up=True, rot=0.0, n=64):
    """Heart outline. size = full height. up=True: lobes at top, tip at bottom. rot in degrees (about cx,cy)."""
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((x, y))
    ys = [p[1] for p in pts]
    ymin, ymax = min(ys), max(ys)
    k = size / (ymax - ymin)
    out = []
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    for x, y in pts:
        px = x * k
        py = -(y - (ymin + ymax) / 2) * k          # +y down, lobes up
        if not up:
            py = -py
        out.append((cx + px * ca - py * sa, cy + px * sa + py * ca))
    return out


def lobe_pts(cx, cy, length, angle_deg, slim=0.80, lift=1.1):
    """Heart-shaped clover lobe whose tip sits `lift` px out from (cx,cy) and points outward along angle_deg (0=right, 90=up)."""
    pts = heart_pts(0, 0, length, up=False)       # tip at top (y negative), lobes at bottom
    # tip is at min y; translate so tip is origin, lobes extend +y; then rotate so +y -> direction angle
    ymin = min(p[1] for p in pts)
    pts = [(x * slim, y - ymin + lift) for x, y in pts]
    ang = math.radians(angle_deg)
    dx, dy = math.cos(ang), -math.sin(ang)        # screen direction (y down)
    # local +y maps to (dx,dy); local +x maps to perpendicular
    px_, py_ = -dy, dx
    return [(cx + x * px_ + y * dx, cy + x * py_ + y * dy) for x, y in pts]


def spade_pts(cx, cy, size):
    return heart_pts(cx, cy - size * 0.12, size * 0.78, up=False)


def diamond_pts(cx, cy, w, h):
    return [(cx, cy - h / 2), (cx + w / 2, cy), (cx, cy + h / 2), (cx - w / 2, cy)]


def ngon_pts(cx, cy, r, n, rot=-90):
    return [(cx + r * math.cos(math.radians(rot + 360 * i / n)), cy + r * math.sin(math.radians(rot + 360 * i / n))) for i in range(n)]


def mask_of(img):
    small = img.resize((INS, INS), Image.BOX)
    return np.array(small).astype(float) / 255 >= 0.5


def shade_layer(out_g, out_a, mask, base=0.90, outline=0.10):
    """Composite one shape: dark outline ring, then bevelled body."""
    pad = np.pad(mask, 1)
    ring = np.zeros_like(mask)
    for dy, dx in [(-1, 0), (1, 0), (0, -1), (0, 1), (-1, -1), (1, 1), (-1, 1), (1, -1)]:
        ring |= np.roll(np.roll(mask, dy, 0), dx, 1)
    ring &= ~mask
    out_g[ring] = outline
    out_a[ring] = 1.0
    body = np.full(mask.shape, base)
    ys = np.arange(INS)[:, None]
    body = body - 0.10 * (ys / INS)
    body = bevel(body, mask, 0.12, 0.28)
    out_g[mask] = body[mask]
    out_a[mask] = 1.0


def carve(mask, other):
    return mask & ~other


def draw_clover(n, devil=False):
    cx, cy = 12, 11.0
    angles = {3: [90, 210, 330], 4: [45, 135, 225, 315], 5: [90, 162, 234, 306, 18]}[n]
    length = {3: 9.5, 4: 8.6, 5: 8.0}[n]
    layers = []
    stem = new_canvas()
    ds = ImageDraw.Draw(stem)
    ds.line([(cx * S, cy * S), (cx * S + 1.5 * S, 21.5 * S)], fill=255, width=int(1.7 * S))
    layers.append(draw_mask_shape(mask_of(stem)))
    lobes = []
    for a in angles:
        img = new_canvas()
        poly(ImageDraw.Draw(img), lobe_pts(cx, cy, length, a))
        m = mask_of(img)
        lobes.append(m)
        layers.append(draw_mask_shape(m))
    g, a = merge(layers)
    if devil:                                   # crossed slits in the lobes (the devil within)
        for m in lobes:
            ys, xs = np.nonzero(m)
            mx_, my_ = int(xs.mean()), int(ys.mean())
            for k in (-1, 0, 1):
                for (yy, xx) in ((my_ + k, mx_ + k), (my_ + k, mx_ - k)):
                    if 0 <= yy < INS and 0 <= xx < INS and m[yy, xx]:
                        g[yy, xx] = 0.12
    return g, a


def draw_heart_shape(cx, cy, size, rot=0.0):
    img = new_canvas()
    poly(ImageDraw.Draw(img), heart_pts(cx, cy, size, up=True, rot=rot))
    return mask_of(img)


def crack_apart(mask, cx, ys=None):
    """Split a mask down a jagged crack and push the halves apart by one pixel."""
    out = np.zeros_like(mask)
    h, w = mask.shape
    for y in range(h):
        cxy = cx + (1 if (y // 2) % 2 == 0 else -1) * (1 if y % 3 else 0)
        for x in range(w):
            if not mask[y, x]:
                continue
            if x < cxy - 0:
                nx = x - 1
            elif x > cxy:
                nx = x + 1
            else:
                continue                              # the crack itself
            if 0 <= nx < w:
                out[y, nx] = True
    return out


def draw_mask_shape(m, base=0.90):
    g = np.zeros((INS, INS))
    a = np.zeros((INS, INS))
    shade_layer(g, a, m, base)
    return g, a


def merge(layers):
    g = np.zeros((INS, INS))
    a = np.zeros((INS, INS))
    for lg, la in layers:
        g = np.where(la > 0, lg, g)
        a = np.maximum(a, la)
    return g, a


def draw_spade_mask(cx, cy, size):
    img = new_canvas()
    d = ImageDraw.Draw(img)
    poly(d, spade_pts(cx, cy, size))
    base_w = size * 0.28
    stem_top = cy + size * 0.20
    poly(d, [(cx - 0.9, stem_top), (cx + 0.9, stem_top), (cx + base_w, cy + size * 0.52), (cx - base_w, cy + size * 0.52)])
    return mask_of(img)


def draw_diamond_mask(cx, cy, w, h):
    img = new_canvas()
    poly(ImageDraw.Draw(img), diamond_pts(cx, cy, w, h))
    return mask_of(img)


def insignia_masks():
    out = {}
    out["three_leaf"] = draw_clover(3)
    out["four_leaf"] = draw_clover(4)
    out["five_leaf"] = draw_clover(5, devil=True)

    out["heart"] = draw_mask_shape(draw_heart_shape(12, 12, 17))
    two = [draw_mask_shape(draw_heart_shape(9.5, 10.5, 12)), draw_mask_shape(draw_heart_shape(14.5, 14, 12))]
    out["two_heart"] = merge(two)
    tiers = [draw_mask_shape(draw_heart_shape(12, 16.5, 12)), draw_mask_shape(draw_heart_shape(12, 10.5, 9)), draw_mask_shape(draw_heart_shape(12, 5.8, 6))]
    out["tiered_heart"] = merge(tiers)
    out["cracked_heart"] = draw_mask_shape(crack_apart(draw_heart_shape(12, 12, 17), 12), 0.78)

    out["spade"] = draw_mask_shape(draw_spade_mask(12, 11.5, 19))
    out["double_spade"] = merge([draw_mask_shape(draw_spade_mask(8.5, 11.5, 14)), draw_mask_shape(draw_spade_mask(15.5, 12.5, 14))])
    out["triple_spade"] = merge([draw_mask_shape(draw_spade_mask(6, 13.5, 11)), draw_mask_shape(draw_spade_mask(18, 13.5, 11)),
                                 draw_mask_shape(draw_spade_mask(12, 10.5, 15))])

    out["diamond"] = draw_mask_shape(draw_diamond_mask(12, 12, 15, 21))
    out["dual_diamond"] = merge([draw_mask_shape(draw_diamond_mask(9.5, 12, 11, 17)), draw_mask_shape(draw_diamond_mask(14.5, 12, 11, 17))])
    img = new_canvas()
    poly(ImageDraw.Draw(img), ngon_pts(12, 12.5, 10.5, 5))
    out["five_sided"] = draw_mask_shape(mask_of(img))
    out["cracked_diamond"] = draw_mask_shape(crack_apart(draw_diamond_mask(12, 12, 15, 21), 12), 0.78)

    # boundless / coverless: a clock face (time magic) - ring, 12 ticks, two hands
    img = new_canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([(12 - 10.5) * S, (12 - 10.5) * S, (12 + 10.5) * S, (12 + 10.5) * S], fill=255)
    d.ellipse([(12 - 8.2) * S, (12 - 8.2) * S, (12 + 8.2) * S, (12 + 8.2) * S], fill=0)
    for i in range(12):
        a = math.radians(30 * i)
        r0, r1 = (6.3, 8.2) if i % 3 == 0 else (7.2, 8.2)
        d.line([(12 + r0 * math.sin(a)) * S, (12 - r0 * math.cos(a)) * S, (12 + r1 * math.sin(a)) * S, (12 - r1 * math.cos(a)) * S], fill=255, width=S)
    d.line([12 * S, 12 * S, 12 * S, 6.2 * S], fill=255, width=int(1.3 * S))
    d.line([12 * S, 12 * S, 16.5 * S, 14.5 * S], fill=255, width=int(1.3 * S))
    d.ellipse([(12 - 1.4) * S, (12 - 1.4) * S, (12 + 1.4) * S, (12 + 1.4) * S], fill=255)
    out["boundless"] = draw_mask_shape(mask_of(img))

    # forbidden runes: circle, pentagram, outer rune ticks
    img = new_canvas()
    d = ImageDraw.Draw(img)
    d.ellipse([(12 - 10.5) * S, (12 - 10.5) * S, (12 + 10.5) * S, (12 + 10.5) * S], outline=255, width=int(1.3 * S))
    star = ngon_pts(12, 12, 9.2, 5)
    for i in range(5):
        a, b = star[i], star[(i + 2) % 5]
        d.line([a[0] * S, a[1] * S, b[0] * S, b[1] * S], fill=255, width=int(1.1 * S))
    for i in range(10):
        a = math.radians(36 * i + 18)
        d.line([(12 + 8.6 * math.sin(a)) * S, (12 - 8.6 * math.cos(a)) * S, (12 + 10.5 * math.sin(a)) * S, (12 - 10.5 * math.cos(a)) * S], fill=255, width=int(0.9 * S))
    out["forbidden_runes"] = draw_mask_shape(mask_of(img), 0.86)
    return out


def insignia():
    for name, (g, a) in insignia_masks().items():
        save_grey("insignia_" + name, g, a)


# --------------------------------------------------------------------------- aura masks (layer 3, emissive)
def aura_halo():
    rng = np.random.default_rng(501)
    h = w = 32
    d = border_dist(h, w).astype(float)
    # a tight rim light that hugs the cover edge (the quad is the cover's own size, so it never reads as a floating pane)
    a = np.exp(-d / 2.6) * 0.42            # canon: only a slight glow
    a *= 0.78 + 0.22 * noise(rng, h, w, 4)
    for (cx, cy) in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:           # corner flares
        ys, xs = np.mgrid[0:h, 0:w]
        a = np.maximum(a, np.exp(-(np.hypot(xs - cx, ys - cy)) / 4.0) * 0.5)
    a[d > 8] *= 0.0
    save_grey("aura_halo", np.ones((h, w)), np.clip(a, 0, 1))


def aura_sigil():
    h = w = 32
    im = Image.new("L", (h * S, w * S), 0)
    d = ImageDraw.Draw(im)
    c = 16 * S
    d.ellipse([c - 14.5 * S, c - 14.5 * S, c + 14.5 * S, c + 14.5 * S], outline=255, width=int(1.1 * S))
    d.ellipse([c - 11.2 * S, c - 11.2 * S, c + 11.2 * S, c + 11.2 * S], outline=190, width=int(0.8 * S))
    for i in range(24):
        a = math.radians(15 * i)
        r0, r1 = (11.4, 14.2) if i % 2 == 0 else (12.6, 14.2)
        d.line([c + r0 * S * math.sin(a), c - r0 * S * math.cos(a), c + r1 * S * math.sin(a), c - r1 * S * math.cos(a)], fill=255 if i % 2 == 0 else 150, width=int(0.8 * S))
    for i in range(6):
        a = math.radians(60 * i)
        d.polygon([(c + 15.4 * S * math.sin(a), c - 15.4 * S * math.cos(a)),
                   (c + 13.2 * S * math.sin(a + 0.12), c - 13.2 * S * math.cos(a + 0.12)),
                   (c + 13.2 * S * math.sin(a - 0.12), c - 13.2 * S * math.cos(a - 0.12))], fill=255)
    a = np.array(im.resize((w, h), Image.BOX)).astype(float) / 255
    save_grey("aura_sigil", np.ones((h, w)), np.clip(a * 1.15, 0, 1))


def aura_sparks():
    rng = np.random.default_rng(503)
    h = w = 32
    a = np.zeros((h, w))
    pts = []
    while len(pts) < 16:
        x, y = int(rng.integers(1, w - 1)), int(rng.integers(1, h - 1))
        if math.hypot(x - 15.5, y - 15.5) < 12.5:
            continue
        pts.append((x, y))
    for i, (x, y) in enumerate(pts):
        a[y, x] = 1.0
        if i % 2 == 0:
            for dx, dy in [(1, 0), (-1, 0), (0, 1), (0, -1)]:
                if 0 <= x + dx < w and 0 <= y + dy < h:
                    a[y + dy, x + dx] = max(a[y + dy, x + dx], 0.55)
            if i % 4 == 0:
                for dx, dy in [(2, 0), (-2, 0), (0, 2), (0, -2)]:
                    if 0 <= x + dx < w and 0 <= y + dy < h:
                        a[y + dy, x + dx] = max(a[y + dy, x + dx], 0.30)
    save_grey("aura_sparks", np.ones((h, w)), a)


def main():
    cover_plain()
    cover_stitched()
    cover_dragonhide()
    cover_tattered()
    cover_metallic()
    trim_border()
    trim_plain()
    trim_heavy()
    trim_runed()
    clasp_strap()
    clasp_buckle()
    clasp_chain()
    clasp_lock()
    pages()
    insignia()
    aura_halo()
    aura_sigil()
    aura_sparks()
    print(f"wrote {len(written)} textures to {os.path.normpath(OUT)}")
    for n in written:
        print("  ", n)
    return 0


if __name__ == "__main__":
    sys.exit(main())

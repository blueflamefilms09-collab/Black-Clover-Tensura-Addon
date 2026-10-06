"""Generates the grimoire item textures (the 0.21 look, built after the Black Clover wiki grimoire art).

    python tools/gen_grimoire_book_textures.py   ->  src/main/resources/assets/nusmp/textures/item/grimoire_book/*.png

Deterministic, no fonts or images. Everything the item colour handler tints is greyscale:
  tint 0 (cover colour): cover_leather, cover_tattered, spine
  tint 1 (trim metal)  : trim_metal, medallion, ornament_<motif>
  tint 2 (emblem)      : emblem_<cover>
  untinted             : pages
The look (from the reference art): near-square book with thick boards, a raised double border frame, corner ornaments, a central
medallion carrying the kingdom emblem (clover / spade / heart / diamond), a banded spine and bright cream pages.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "item", "grimoire_book")
SS = 4


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def noise(w, h, cell, seed, tile=True):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    if tile:
        g[-1, :] = g[0, :]
        g[:, -1] = g[:, 0]
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, cell, seed, octaves=4):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + noise(w, h, max(2, cell >> o), seed + o) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def grey(v, a=None):
    v = np.clip(v, 0, 255)
    a = np.full_like(v, 255) if a is None else np.clip(a, 0, 255)
    return Image.fromarray(np.dstack([v, v, v, a]).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------ leather, spine, metal, pages
def leather(size=32):
    n = fbm(size, size, 16, 11)
    pores = noise(size, size, 2, 12)
    v = 205 + (n - 0.5) * 50 + (pores - 0.5) * 18
    save(grey(v), "cover_leather")


def tattered(size=32):
    n = fbm(size, size, 16, 21)
    grime = fbm(size, size, 8, 22, 3)
    v = 150 + (n - 0.5) * 60
    v = np.where(grime > 0.55, v * 0.55, v)
    rng = np.random.default_rng(23)
    im = grey(v)
    d = ImageDraw.Draw(im)
    for _ in range(9):                                         # scratches and tears
        x, y = rng.integers(0, size, 2)
        l = rng.integers(3, 9)
        a = rng.uniform(0, math.pi)
        d.line([(x, y), (x + math.cos(a) * l, y + math.sin(a) * l)], fill=(70, 70, 70, 255), width=1)
    save(im, "cover_tattered")


def spine(size=32):
    n = fbm(size, size, 16, 31)
    yy = np.mgrid[0:size, 0:size][0]
    ridges = np.where((yy % 8) < 1, -35, 0)
    v = 195 + (n - 0.5) * 40 + ridges
    save(grey(v), "spine")


def trim_metal(size=16):
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    v = 200 + 45 * np.sin((xx + yy) / size * math.pi) + (noise(size, size, 2, 41) - 0.5) * 20
    save(grey(v), "trim_metal")


def pages(size=32):
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    lines = np.where((yy % 2) < 1, 0.9, 1.0) * np.where((xx % 2) < 1, 0.95, 1.0)
    n = noise(size, size, 8, 51)
    r = (250 * lines - n * 10)
    g = (240 * lines - n * 10)
    b = (205 * lines - n * 12)
    save(Image.fromarray(np.dstack([r, g, b, np.full_like(r, 255)]).clip(0, 255).astype(np.uint8), "RGBA"), "pages")


def medallion(size=32):
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    c = (size - 1) / 2
    r = np.sqrt((xx - c) ** 2 + (yy - c) ** 2) / c
    ring = np.exp(-((r - 0.86) / 0.07) ** 2)
    inner = np.exp(-((r - 0.70) / 0.03) ** 2) * 0.6
    field = np.where(r < 0.8, 0.55, 0)
    v = np.clip(field * 255 + ring * 255 + inner * 255, 0, 255)
    a = np.where(r < 0.98, 255, 0)
    save(grey(v, a), "medallion")


# ------------------------------------------------------------------------------------------------ emblems (white shapes)
def heart_pts(cx, cy, s, rot=0.0, n=60, flip=False):
    pts = []
    for k in range(n):
        t = 2 * math.pi * k / n
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        if flip:
            y = -y
        x, y = x / 34 * s, y / 34 * s
        c, si = math.cos(rot), math.sin(rot)
        pts.append((cx + x * c - y * si, cy + x * si + y * c))
    return pts


def leaf(cx, cy, s, ang):
    """A clover leaf: a heart whose tip sits at the centre, pointing outward at angle ang (0 = up)."""
    out = []
    for x, y in heart_pts(0, 0, s):
        y = y - s * 0.5                                           # tip at (0,0), lobes outward (up before rotating)
        c, si = math.cos(ang), math.sin(ang)
        out.append((cx + x * c - y * si, cy + x * si + y * c))
    return out


def emblem(name, draw_fn, size=32):
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    draw_fn(d, S)
    im = im.resize((size, size), Image.LANCZOS)
    save(im, "emblem_" + name)


W = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)


def clover(n):
    def f(d, S):
        c = S / 2
        angles = [0, math.radians(120), math.radians(-120)] if n == 3 else [2 * math.pi * k / n for k in range(n)]
        for a in angles:
            d.polygon(leaf(c, c, S * (0.40 if n == 3 else 0.36), a), fill=W)
        if n == 3:
            d.line([(c, c), (c + S * 0.08, c + S * 0.40)], fill=W, width=int(S * 0.05))
    return f


def spade_at(d, cx, cy, s):
    d.polygon(heart_pts(cx, cy - s * 0.05, s, flip=True), fill=W)
    d.polygon([(cx, cy + s * 0.1), (cx - s * 0.18, cy + s * 0.52), (cx + s * 0.18, cy + s * 0.52)], fill=W)


def spades(n):
    def f(d, S):
        c = S / 2
        if n == 1:
            spade_at(d, c, c - S * 0.05, S * 0.75)
        else:
            offs = [-0.2, 0.2] if n == 2 else [-0.26, 0, 0.26]
            for i, o in enumerate(offs):
                spade_at(d, c + o * S, c - S * 0.05 + (0.06 * S if n == 3 and i != 1 else 0), S * (0.5 if n == 2 else 0.42))
    return f


def hearts(kind):
    def f(d, S):
        c = S / 2
        if kind == "one":
            d.polygon(heart_pts(c, c + S * 0.05, S * 0.8), fill=W)
        elif kind == "two":
            d.polygon(heart_pts(c - S * 0.14, c + S * 0.05, S * 0.56), fill=W)
            d.polygon(heart_pts(c + S * 0.14, c + S * 0.05, S * 0.56), fill=W)
        else:                                                      # cracked
            d.polygon(heart_pts(c, c + S * 0.05, S * 0.8), fill=W)
            d.line([(c, c - S * 0.25), (c - S * 0.06, c - S * 0.05), (c + S * 0.06, c + S * 0.1), (c - S * 0.02, c + S * 0.35)],
                   fill=CLEAR, width=int(S * 0.05))
    return f


def diamonds(kind):
    def f(d, S):
        c = S / 2
        if kind == "five":
            pts = [(c + math.sin(2 * math.pi * k / 5) * S * 0.4, c - math.cos(2 * math.pi * k / 5) * S * 0.4) for k in range(5)]
            d.polygon(pts, fill=W)
        else:
            d.polygon([(c, c - S * 0.42), (c + S * 0.3, c), (c, c + S * 0.42), (c - S * 0.3, c)], fill=W)
            if kind == "cracked":
                d.line([(c - S * 0.05, c - S * 0.3), (c + S * 0.05, c - S * 0.05), (c - S * 0.04, c + S * 0.12), (c + S * 0.03, c + S * 0.3)],
                       fill=CLEAR, width=int(S * 0.05))
    return f


# ------------------------------------------------------------------------------------------------ cover ornaments (per motif)
def ornament(name, motif, size=64):
    """Front-cover overlay: inner frame line, corner pieces and the motif pattern; the centre stays clear for the medallion."""
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    lw = int(S * 0.012)
    inset = S * 0.13
    if motif != "tattered":
        d.rectangle([inset, inset, S - inset, S - inset], outline=W, width=lw)                # inner frame line
    c = S / 2
    rng = np.random.default_rng(abs(hash(name)) % (2 ** 31))

    def corner_curls(scale=1.0):
        for sx, sy in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
            ox, oy = (inset if sx > 0 else S - inset), (inset if sy > 0 else S - inset)
            for k in range(2):
                pts = []
                for i in range(40):
                    t = i / 39
                    ang = t * 4.4
                    r = S * 0.085 * scale * (1 - t * 0.8)
                    u, v = r * math.cos(ang), r * math.sin(ang)
                    if k:
                        u, v = v, u
                    pts.append((ox + sx * (S * 0.085 * scale - u + S * 0.02), oy + sy * (S * 0.085 * scale - v + S * 0.02)))
                d.line(pts, fill=W, width=lw)

    if motif in ("filigree", "ornate"):
        corner_curls(1.0 if motif == "filigree" else 1.25)
        # fleur-de-lis tips at the edge midpoints
        for (x, y, ax) in ((c, inset, 0), (c, S - inset, 0), (inset, c, 1), (S - inset, c, 1)):
            r = S * 0.04
            if ax == 0:
                d.polygon([(x - r, y), (x, y + (r * 1.6 if y < c else -r * 1.6)), (x + r, y)], fill=W)
            else:
                d.polygon([(x, y - r), (x + (r * 1.6 if x < c else -r * 1.6), y), (x, y + r)], fill=W)
        if motif == "ornate":                                      # dense gilding: a second frame and swirls round the medallion
            i2 = inset + S * 0.05
            d.rectangle([i2, i2, S - i2, S - i2], outline=W, width=max(1, lw // 2))
            for k in range(8):
                a = 2 * math.pi * k / 8
                x, y = c + math.cos(a) * S * 0.25, c + math.sin(a) * S * 0.25
                d.arc([x - S * 0.05, y - S * 0.05, x + S * 0.05, y + S * 0.05], 0, 270, fill=W, width=lw)
    elif motif == "wheels":                                         # Zenon's spade books: radial wheels in the corners
        for sx, sy in ((0.27, 0.27), (0.73, 0.27), (0.27, 0.73), (0.73, 0.73)):
            x, y, r = S * sx, S * sy, S * 0.11
            d.ellipse([x - r, y - r, x + r, y + r], outline=W, width=lw)
            for k in range(8):
                a = 2 * math.pi * k / 8
                d.line([(x, y), (x + math.cos(a) * r, y + math.sin(a) * r)], fill=W, width=max(1, lw // 2))
    elif motif == "stars":
        for k in range(10):
            x, y = rng.uniform(inset * 1.4, S - inset * 1.4, 2)
            if abs(x - c) < S * 0.2 and abs(y - c) < S * 0.2:
                continue
            r = S * rng.uniform(0.03, 0.06)
            pts = []
            for j in range(8):
                rr = r if j % 2 == 0 else r * 0.35
                a = math.pi / 4 * j
                pts.append((x + math.cos(a) * rr, y + math.sin(a) * rr))
            d.polygon(pts, fill=W)
    elif motif == "lattice":
        step = S * 0.09
        for k in range(-12, 13):
            d.line([(inset + k * step, inset), (inset + k * step + (S - 2 * inset), S - inset)], fill=W, width=max(1, lw // 2))
            d.line([(S - inset - k * step, inset), (S - inset - k * step - (S - 2 * inset), S - inset)], fill=W, width=max(1, lw // 2))
        # keep the lattice inside the frame and clear the centre
        mask = Image.new("L", (S, S), 0)
        ImageDraw.Draw(mask).rectangle([inset, inset, S - inset, S - inset], fill=255)
        ImageDraw.Draw(mask).ellipse([c - S * 0.2, c - S * 0.2, c + S * 0.2, c + S * 0.2], fill=0)
        a = np.array(im)
        a[..., 3] = np.minimum(a[..., 3], np.array(mask))
        im = Image.fromarray(a, "RGBA")
        d = ImageDraw.Draw(im)
        d.rectangle([inset, inset, S - inset, S - inset], outline=W, width=lw)
    elif motif == "floral":
        for (x, y) in ((0.27, 0.27), (0.73, 0.27), (0.27, 0.73), (0.73, 0.73), (0.5, 0.2), (0.5, 0.8), (0.2, 0.5), (0.8, 0.5)):
            x, y = S * x, S * y
            for k in range(5):
                a = 2 * math.pi * k / 5
                px, py = x + math.cos(a) * S * 0.035, y + math.sin(a) * S * 0.035
                d.ellipse([px - S * 0.028, py - S * 0.028, px + S * 0.028, py + S * 0.028], fill=W)
            d.ellipse([x - S * 0.015, y - S * 0.015, x + S * 0.015, y + S * 0.015], fill=CLEAR)
    elif motif == "straps":                                         # Karna: straps with buckles across the cover
        for x in (0.3, 0.7):
            d.rectangle([S * x - S * 0.06, 0, S * x + S * 0.06, S], fill=W)
            d.rectangle([S * x - S * 0.08, S * 0.62, S * x + S * 0.08, S * 0.72], outline=(140, 140, 140, 255), width=lw * 2)
    elif motif == "tattered":                                       # Asta: a frame half worn away, scratches
        pts = [(inset, inset), (S - inset, inset), (S - inset, S - inset), (inset, S - inset), (inset, inset)]
        for (a, b) in zip(pts, pts[1:]):
            for t0 in np.linspace(0, 1, 9)[:-1]:
                if rng.random() < 0.45:
                    continue
                t1 = t0 + 1 / 8 * rng.uniform(0.4, 1.0)
                d.line([(a[0] + (b[0] - a[0]) * t0, a[1] + (b[1] - a[1]) * t0), (a[0] + (b[0] - a[0]) * t1, a[1] + (b[1] - a[1]) * t1)],
                       fill=(150, 150, 150, 255), width=lw)
        for _ in range(14):
            x, y = rng.uniform(0, S, 2)
            a = rng.uniform(0, math.pi)
            l = S * rng.uniform(0.04, 0.12)
            d.line([(x, y), (x + math.cos(a) * l, y + math.sin(a) * l)], fill=(110, 110, 110, 200), width=max(1, lw // 2))
    # "plain" keeps only the frame line
    im = im.resize((size, size), Image.LANCZOS)
    save(im, "ornament_" + name)


if __name__ == "__main__":
    leather()
    tattered()
    spine()
    trim_metal()
    pages()
    medallion()
    emblem("three_leaf", clover(3))
    emblem("four_leaf", clover(4))
    emblem("five_leaf", clover(5))
    emblem("spade", spades(1))
    emblem("double_spade", spades(2))
    emblem("triple_spade", spades(3))
    emblem("heart", hearts("one"))
    emblem("two_heart", hearts("two"))
    emblem("cracked_heart", hearts("cracked"))
    emblem("diamond", diamonds("one"))
    emblem("five_sided", diamonds("five"))
    emblem("cracked_diamond", diamonds("cracked"))
    # 0.24: the cover designs come from the art pack (tools/gen_cover_art_textures.py); only the canon specials stay procedural
    for m in ("straps", "tattered"):
        ornament(m, m)

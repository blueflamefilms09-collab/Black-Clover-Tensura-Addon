"""Icon forge v3, the pixel painter (Pillow + numpy only, deterministic).

Why a new painter: the v2 / v3 pipelines paint a smooth, soft-shaded picture at 128 / 256 px and box-filter it down to 32 px. The result has 600 to 800
distinct colours per icon (Tensura's own icons: about 80, Tensura No Index: about 200), mostly one-pixel gradients and grain, so edges are mushy and the symbol
reads as a blur. This painter keeps the same scenes (the `Ctx` item lists of tools/skills/icon3_*.py, or a glyph mask) but renders them as pixel art on the
native 32 px grid:

  * each shape is cut to a hard mask (coverage >= 0.45), no anti-aliasing;
  * each shape is shaded with 4 flat tones (shadow / base / light / highlight) from one light direction (top left), a 3x3 mode filter removes lone pixels;
  * a 1 px selective outline (the outline colour is a dark version of the colour next to it, not black) and 1 px dark separation lines between parts;
  * a 1 px rim light on the lower right edge in the glow colour, a 2-step aura halo, a banded (not gradient) background with a lit centre, star sparkles;
  * a thin class frame (1 px bright ring + 1 px dark outer + 1 px inner shadow) and rank pips in the colours of the mod's own relic Grades (relics/Grade.java).

Animation (Ultimates and root Uniques) follows what Tensura No Index does for its animated item icons (12 to 16 frames, 3 to 4 ticks, no interpolation): a local
diagonal glint that crosses only the symbol, twinkling sparkles, a pulsing aura / core and a few embers rising. Every effect is periodic.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "skills"))
import icon3_lib as L                      # noqa: E402

OUT = 32
S = L.S
K = S // OUT
THRESH = 0.45

# the mod's own relic Grade colours (relics/Grade.java), used for the rank pips
GRADE = {"RARE": (0x55, 0xFF, 0xFF), "UNIQUE": (0xFF, 0x77, 0xFF), "LEGEND": (0xFF, 0xAA, 0x00), "GOD": (0xFF, 0x55, 0x55),
         "GENESIS": (0xFF, 0xFF, 0xFF), "AKASHIC": (0xFF, 0x8C, 0xFF)}
RANK = {"unique": ("RARE", 3), "extra": ("LEGEND", 4), "ultimate": ("GENESIS", 5)}

FR = L.FRAMES


def rgb(c):
    return L.rgb(c)


def clamp(c):
    return np.clip(c, 0, 255)


def box(a):
    """S x S (x C) -> OUT x OUT (x C), mean of each K x K block."""
    sh = (OUT, K, OUT, K) + a.shape[2:]
    return a.reshape(sh).mean(axis=(1, 3))


def boxmax(a):
    sh = (OUT, K, OUT, K) + a.shape[2:]
    return a.reshape(sh).max(axis=(1, 3))


# ------------------------------------------------------------------------------------------------ small pixel helpers
_DISC = {}


def disc(r):
    if r not in _DISC:
        _DISC[r] = [(dx, dy) for dy in range(-r, r + 1) for dx in range(-r, r + 1) if dx * dx + dy * dy <= r * r + r * 0.5]
    return _DISC[r]


def shifted(b, dx, dy):
    out = np.zeros_like(b)
    h, w = b.shape[:2]
    ys, yd = (slice(max(0, -dy), h - max(0, dy)), slice(max(0, dy), h - max(0, -dy)))
    xs, xd = (slice(max(0, -dx), w - max(0, dx)), slice(max(0, dx), w - max(0, -dx)))
    out[yd, xd] = b[ys, xs]
    return out


def dilate(b, r, square=False):
    out = np.zeros_like(b, dtype=bool)
    offs = [(dx, dy) for dy in range(-r, r + 1) for dx in range(-r, r + 1)] if square else disc(r)
    for dx, dy in offs:
        out |= shifted(b, dx, dy)
    return out


def dilate4(b):
    out = b.copy()
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        out |= shifted(b, dx, dy)
    return out


def mode_filter(idx, mask, n_tones=4):
    """Replaces a pixel whose tone appears nowhere among its 4 neighbours by the majority tone of its neighbourhood (keeps pixel art free of lone pixels)."""
    out = idx.copy()
    counts = np.zeros((n_tones,) + idx.shape, dtype=np.int32)
    for k in range(n_tones):
        m = (idx == k) & mask
        c = np.zeros(idx.shape, dtype=np.int32)
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if dx or dy:
                    c += shifted(m, dx, dy).astype(np.int32)
        counts[k] = c
    maj = counts.argmax(axis=0)
    own = np.take_along_axis(counts, idx[None].clip(0, n_tones - 1), axis=0)[0]
    nb4 = np.zeros(idx.shape, dtype=bool)
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nb4 |= shifted((idx == idx) & mask, dx, dy) & (shifted(idx, dx, dy) == idx)
    lone = mask & ~nb4 & (counts.max(axis=0) >= 3)
    out[lone] = maj[lone]
    return out


# ------------------------------------------------------------------------------------------------ colour ramps
def ramp(base):
    """shadow, base, light, highlight of one colour (cool shadows, warm lights)."""
    b = rgb(base)
    sh = clamp(L.darken(b, 0.50) * np.array([0.92, 0.92, 1.10], np.float32))
    mid = b
    li = clamp(L.lighten(b, 0.36))
    hi = clamp(L.lighten(b, 0.74))
    return np.stack([sh, mid, li, hi]).astype(np.float32)


def outline_col(base, bg_dark):
    return clamp(L.mix(L.darken(base, 0.80), bg_dark, 0.45))


def light_maps(mask, bevel, k):
    """Lit-bevel maths like icon3_lib.shade_rgb, but returning the light value t and the specular lam instead of a colour. The surface is inflated more than
    in the smooth painter (two blur radii), so a large shape (a face, a shield) still gets a lit upper left and a shaded lower right in 4 flat tones."""
    r = bevel * (S / 128.0)
    h = 0.5 * L.blur(mask, r * 1.5) + 0.5 * L.blur(mask, r * 3.2)
    gy, gx = np.gradient(h)
    kk = bevel * 4.6 * k
    nx, ny = -gx * kk, -gy * kk
    ln = np.sqrt(nx * nx + ny * ny + 1.0)
    lam = np.clip((nx * L._L[0] + ny * L._L[1] + L._L[2]) / ln, 0, 1)
    t = np.clip(0.20 + 0.80 * lam * 1.12, 0, 1)
    yy, _ = L.grid()
    t = np.clip(t + 0.10 * (0.5 - yy / S), 0, 1)
    return t, lam


TONE_CUTS = (0.40, 0.60, 0.82)


# ------------------------------------------------------------------------------------------------ scene
class Scene:
    """The still layers of one icon: everything an animation frame needs without painting the symbol again."""
    pass


def build(items, pal, seed, rays=True, bg=None):
    """Paints the symbol of a Ctx item list on a banded background; returns a Scene."""
    glow = rgb(pal["glow"])
    bg_dark = rgb(pal["bg1"])
    bgimg = make_bg(pal, seed, rays) if bg is None else bg
    img = bgimg.copy()
    ids = -np.ones((OUT, OUT), dtype=np.int32)
    union = np.zeros((OUT, OUT), dtype=bool)
    olunion = np.zeros((OUT, OUT), dtype=bool)
    ol_cols = {}
    part_tone = np.full((OUT, OUT), -1, dtype=np.int32)
    part_rgb = np.zeros((OUT, OUT, 3), dtype=np.float32)
    addl = np.zeros((OUT, OUT, 3), dtype=np.float32)
    n = 0
    yy32, _ = np.mgrid[0:OUT, 0:OUT]
    for it in items:
        kind = it[0]
        if kind == "part":
            _, m, col, bevel, spec, grad, outl, k = it
            cov = box(m)
            b = cov >= THRESH
            if b.sum() < 2:
                b = cov >= 0.25
            if not b.any():
                continue
            t, lam = light_maps(m, bevel, k)
            w = np.maximum(m, 1e-4)
            t32 = box(t * w) / np.maximum(box(w), 1e-4)
            lam32 = boxmax(lam * (m > 0.5))
            tone = np.digitize(t32, TONE_CUTS).astype(np.int32)
            tone[(lam32 > 0.93) & (spec > 0.2)] = 3
            tone = mode_filter(tone, b)
            if grad is not None:
                c0, c1 = rgb(grad[0]), rgb(grad[1])
                v = np.clip((np.arange(OUT) * K + K / 2 - S * 0.15) / (S * 0.7), 0, 1)
                band = np.minimum((v * 3).astype(int), 2)                   # 3 vertical bands of the gradient
                rows = [ramp(c0 * (1 - f) + c1 * f) for f in (0.0, 0.5, 1.0)]
                cols = np.empty((OUT, OUT, 3), dtype=np.float32)
                for y in range(OUT):
                    cols[y] = rows[band[y]][tone[y]]
                base_rgb = cols
                base_col = c0 * 0.5 + c1 * 0.5
            else:
                rp = ramp(col)
                base_rgb = rp[tone]
                base_col = rgb(col)
            # a shade-dependent pixel is only painted where the mask is; cast a 1 px shadow of earlier parts is not needed (separation lines do it)
            nb = np.zeros((OUT, OUT), dtype=bool)
            if union.any():
                other = (ids >= 0) & ~b
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nb |= shifted(other, dx, dy)
            img[b] = base_rgb[b]
            if outl and union.any():
                sep = b & nb
                sepc = ramp(base_col)[0] * 0.55
                img[sep] = sepc
            ids[b] = n
            part_tone[b] = tone[b]
            part_rgb[b] = img[b]
            union |= b
            if outl:
                olunion |= b
                ol_cols[n] = outline_col(base_col, bg_dark)
            n += 1
        elif kind == "flat":
            _, m, col, alpha, outl = it
            cov = box(m)
            b = cov >= 0.40
            if not b.any():
                continue
            a = 1.0 if alpha >= 0.7 else alpha
            c = rgb(col)
            img[b] = img[b] * (1 - a) + c * a
            if outl:
                olunion |= b
                union |= b
                ids[b] = n
                ol_cols[n] = outline_col(c, bg_dark)
                n += 1
            part_rgb[b] = img[b]
        elif kind == "add":
            _, m, col, alpha = it
            cov = box(m) * alpha
            q = np.round(np.clip(cov, 0, 1) * 3) / 3.0
            addl += q[..., None] * rgb(col)
        elif kind == "cut":
            _, m, col, alpha = it
            b = (box(m) >= 0.40) & union
            a = 1.0 if alpha >= 0.7 else alpha
            img[b] = img[b] * (1 - a) + rgb(col) * a
    sc = Scene()
    sc.pal, sc.seed, sc.glow, sc.bg_dark = pal, seed, glow, bg_dark
    sc.bg, sc.img, sc.ids, sc.union, sc.olunion, sc.ol_cols, sc.addl = bgimg, img, ids, union, olunion, ol_cols, addl
    return sc


def make_bg(pal, seed, rays=True):
    """Banded background: 6 flat bands from the dark edge to the lit centre, plus a few faint ray wedges (one extra band step)."""
    yy, xx = np.mgrid[0:OUT, 0:OUT].astype(np.float32)
    cx, cy = OUT * 0.5 - 0.5, OUT * 0.46 - 0.5
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / (OUT * 0.62)
    t = np.clip(1 - d, 0, 1) ** 1.3
    ang = np.arctan2(yy - cy, xx - cx)
    if rays:
        wedge = (np.cos(ang * 7 + (seed % 628) / 100.0) > 0.55) & (t > 0.12)
        t = t + wedge * 0.10
    band = np.minimum(np.floor(np.clip(t, 0, 1) * 6), 5) / 5.0
    c0, c1 = rgb(pal["bg0"]), rgb(pal["bg1"])
    return (c1 + (c0 - c1) * band[..., None]).astype(np.float32)


def aura(sc, strength=1.0):
    """Returns an image: the background with a 2-step halo of the glow colour round the symbol."""
    img = sc.bg.copy()
    glow = sc.glow
    ring2 = dilate(sc.union, 4) & ~sc.union
    ring1 = dilate(sc.union, 2) & ~sc.union
    a2, a1 = 0.14 * strength, 0.28 * strength
    img[ring2] = img[ring2] * (1 - a2) + glow * a2
    img[ring1] = img[ring1] * (1 - a1) + glow * a1
    return img


def sparkle_spots(sc, count=4):
    """Fixed sparkle positions in the free background (deterministic from the seed)."""
    rng = np.random.RandomState(sc.seed & 0x7FFFFFFF)
    free = ~dilate(sc.olunion, 3)
    spots = []
    for _ in range(200):
        x, y = rng.randint(4, OUT - 4), rng.randint(4, OUT - 4)
        if not free[y, x]:
            continue
        if all(abs(x - sx) + abs(y - sy) >= 9 for sx, sy in spots):
            spots.append((x, y))
        if len(spots) >= count:
            break
    return spots


def put_star(img, x, y, level, color):
    """level 1: one pixel, 2: a plus."""
    col = np.asarray(color, np.float32)
    if level >= 1 and 0 <= x < OUT and 0 <= y < OUT:
        img[y, x] = col
    if level >= 2:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            xx, yy = x + dx, y + dy
            if 0 <= xx < OUT and 0 <= yy < OUT:
                img[yy, xx] = img[yy, xx] * 0.45 + col * 0.55


# ------------------------------------------------------------------------------------------------ frame
METALS = {
    "gold": dict(outer=(40, 18, 4), light=(255, 226, 120), mid=(236, 160, 40), dark=(160, 88, 16), inner=(52, 28, 6), corner=(255, 246, 200)),
    "silver": dict(outer=(14, 18, 28), light=(236, 242, 250), mid=(160, 176, 198), dark=(78, 90, 112), inner=(22, 32, 50), corner=(255, 255, 255)),
}
METAL_RANK = {"gold": ("LEGEND", 3), "silver": ("RARE", 3), "iron": ("GOD", 5)}


def iron_frame(glow):
    g = rgb(glow)
    return dict(outer=(6, 4, 10), light=L.lighten(g, 0.35), mid=g, dark=g * 0.45, inner=(20, 8, 30), corner=(255, 255, 255))


def draw_medallion(img, spec, shimmer=None):
    """Round plate (the five Veldanava Ultimates): a 1.4 px bright gold ring lit from the top left, a dark outer line, an inner shadow line, transparent outside the circle."""
    f = spec
    out = np.zeros((OUT, OUT, 4), dtype=np.float32)
    out[..., :3] = img
    out[..., 3] = 255
    yy, xx = np.mgrid[0:OUT, 0:OUT]
    d = np.hypot(xx - 15.5, yy - 15.5)
    lit = (xx + yy) < (OUT - 1)
    out[(d > 12.4) & (d <= 13.5), :3] = out[(d > 12.4) & (d <= 13.5), :3] * 0.55 + rgb(f["inner"]) * 0.45
    ring = (d > 13.5) & (d <= 15.0)
    out[ring & lit, :3] = rgb(f["light"])
    out[ring & ~lit, :3] = rgb(f["mid"])
    rim = (d > 15.0) & (d <= 16.1)
    out[rim & lit, :3] = rgb(f["outer"])
    out[rim & ~lit, :3] = rgb(f["dark"]) * 0.55
    gem = (np.abs(xx - 15.5) < 1.0) & (yy <= 1)
    out[gem, :3] = rgb(f["corner"])
    if shimmer is not None:
        ang = (np.arctan2(yy - 15.5, xx - 15.5) / (2 * math.pi)) % 1.0
        dd = np.abs(ang - shimmer)
        dd = np.minimum(dd, 1 - dd)
        boost = np.clip(1 - dd / 0.06, 0, 1) ** 1.2
        out[ring, :3] = out[ring, :3] + (255 - out[ring, :3]) * (boost[ring][:, None] * 0.7)
    out[d > 16.1, 3] = 0
    return np.clip(out, 0, 255).astype(np.uint8)


def draw_frame(img, cls, accent, pips=True, shimmer=None, twinkle=0, spec=None, rank=None):
    """Thin class frame (pixel exact) -> RGBA uint8. x = 0 dark outer line, 1 bright ring (lit top left), 2 inner shadow line.
    cls = unique / extra / ultimate, or any name when spec (a dict like FRAMES[...]) and rank (Grade name, count) are given."""
    f = spec if spec is not None else FR[cls]
    out = np.zeros((OUT, OUT, 4), dtype=np.float32)
    out[..., :3] = img
    out[..., 3] = 255
    yy, xx = np.mgrid[0:OUT, 0:OUT]
    e = np.minimum(np.minimum(xx, OUT - 1 - xx), np.minimum(yy, OUT - 1 - yy))
    lit = (xx + yy) < (OUT - 1)
    out[e == 2, :3] = out[e == 2, :3] * 0.55 + rgb(f["inner"]) * 0.45
    out[e == 0, :3] = rgb(f["outer"])
    ring = e == 1
    out[ring & lit, :3] = rgb(f["light"])
    out[ring & ~lit, :3] = rgb(f["mid"])
    # a bevel: the outer line is lighter on the dark side so the frame reads as a raised rim
    out[(e == 0) & ~lit, :3] = rgb(f["dark"]) * 0.55
    if cls == "ultimate":
        acc = rgb(accent)
        for (cx, cy) in ((1, 1), (OUT - 2, 1), (1, OUT - 2), (OUT - 2, OUT - 2)):
            out[cy, cx, :3] = L.lighten(acc, 0.55)
            sx = 1 if cx < 16 else -1
            sy = 1 if cy < 16 else -1
            out[cy, cx + sx, :3] = acc
            out[cy + sy, cx, :3] = acc
        out[ring & lit, :3] = rgb(f["light"])
        # violet inner line of the Ultimate plate
        out[(e == 2), :3] = out[(e == 2), :3] * 0.5 + rgb((110, 50, 170)) * 0.5
    else:
        for (cx, cy) in ((1, 1), (OUT - 2, 1), (1, OUT - 2), (OUT - 2, OUT - 2)):
            out[cy, cx, :3] = rgb(f["corner"])
    if pips:
        name, n = rank if rank is not None else RANK[cls]
        col = rgb(GRADE[name])
        x0 = OUT // 2 - n + 1                      # n pips, one px apart, centred on the top inner line (y = 2)
        for i in range(n):
            out[2, x0 + 2 * i, :3] = col
        if cls == "ultimate":                      # the fifth Grade star flashes violet like EX-Singularity
            out[2, x0 + 2 * (n // 2), :3] = rgb(GRADE["AKASHIC"]) if twinkle % 2 else col
    if shimmer is not None:
        ang = (np.arctan2(yy - (OUT - 1) / 2, xx - (OUT - 1) / 2) / (2 * math.pi)) % 1.0
        dd = np.abs(ang - shimmer)
        dd = np.minimum(dd, 1 - dd)
        boost = np.clip(1 - dd / 0.07, 0, 1) ** 1.2
        sel = ring
        out[sel, :3] = out[sel, :3] + (255 - out[sel, :3]) * (boost[sel][:, None] * 0.7)
    corner = ((xx == 0) | (xx == OUT - 1)) & ((yy == 0) | (yy == OUT - 1))
    out[corner, 3] = 0
    return np.clip(out, 0, 255).astype(np.uint8)


# ------------------------------------------------------------------------------------------------ compose
def compose(sc, cls, t=None, accent=None, frame=True, pips=True, spec=None, rank=None, rim_all=False, medallion=None):
    """One 32 px RGBA frame of a Scene. t = loop phase in [0, 1) (an animation frame) or None (the still)."""
    pulse = 1.0 if t is None else 1.0 + 0.45 * math.sin(2 * math.pi * t)
    base = aura(sc, pulse)
    img = base.copy()
    union, ids = sc.union, sc.ids
    # painted symbol over the aura
    img[union] = sc.img[union]
    # parts that paint outside the union only through flats are in sc.img already; copy flats painted on background
    diff = np.any(sc.img != sc.bg, axis=2) & ~union
    img[diff] = sc.img[diff]
    # additive lights (cores, eyes), pulsing
    add = sc.addl * (1.0 if t is None else 0.8 + 0.35 * (0.5 + 0.5 * math.sin(2 * math.pi * t)))
    img = clamp(img + add * 255 / 255.0)
    # selective outline
    ol = dilate4(sc.olunion) & ~sc.olunion
    ol &= ~(union & ~sc.olunion)
    if ol.any():
        for y, x in zip(*np.nonzero(ol)):
            best = -1
            for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                xx, yy = x + dx, y + dy
                if 0 <= xx < OUT and 0 <= yy < OUT and sc.ids[yy, xx] > best:
                    best = sc.ids[yy, xx]
            img[y, x] = sc.ol_cols.get(best, outline_col((60, 60, 80), sc.bg_dark))
    # rim light: lower right inner edge of the symbol picks up the glow colour
    rim = sc.olunion & ~shifted_neighbour_mask(sc.olunion)
    if rim_all:
        rim = sc.olunion & ~(shifted(sc.olunion, 0, -1) & shifted(sc.olunion, 0, 1) & shifted(sc.olunion, 1, 0) & shifted(sc.olunion, -1, 0))
        img[rim] = img[rim] * 0.35 + sc.glow * 0.65
    else:
        img[rim] = img[rim] * 0.62 + sc.glow * 0.38
    if t is not None:
        # glint: a diagonal band crosses the symbol during the first half of the loop
        if t < 0.5:
            pos = -6 + (t / 0.5) * (OUT * 2 + 10)
            yy, xx = np.mgrid[0:OUT, 0:OUT]
            u = (xx + yy).astype(np.float32)
            dd = np.abs(u - pos)
            core = (dd <= 1.0) & union
            edge = (dd <= 2.4) & ~core & union
            img[core] = img[core] * 0.30 + 255 * 0.70
            img[edge] = img[edge] * 0.72 + 255 * 0.28
        # embers rising from the symbol
        rng = np.random.RandomState((sc.seed * 7 + 3) & 0x7FFFFFFF)
        ys, xs = np.nonzero(union)
        if len(xs):
            for i in range(3):
                j = rng.randint(0, len(xs))
                x0, ytop = int(xs[j]), int(ys[xs == xs[j]].min()) if (xs == xs[j]).any() else int(ys[j])
                ph = (t + i / 3.0) % 1.0
                y = ytop - 1 - int(ph * 9)
                if 3 <= y < OUT - 3 and not union[y, x0] and ph < 0.85:
                    a = 0.9 * (1 - ph)
                    img[y, x0] = img[y, x0] * (1 - a) + L.lighten(sc.glow, 0.45) * a
    # sparkles in the background
    spots = sparkle_spots(sc, 4)
    for i, (x, y) in enumerate(spots):
        if t is None:
            lvl = 2 if i % 2 == 0 else 1
        else:
            tw = (t * 2 + i * 0.27) % 1.0
            lvl = 2 if 0.40 < tw < 0.60 else (1 if 0.25 < tw < 0.75 else 0)
        if lvl:
            put_star(img, x, y, lvl, L.lighten(sc.glow, 0.8))
    if not frame:
        return np.clip(img, 0, 255).astype(np.uint8)
    if medallion is not None:
        return draw_medallion(img, medallion, shimmer=None if t is None else t)
    shimmer = None if t is None else t
    tw = 0 if t is None else int(t * 4)
    return draw_frame(img, cls, accent if accent is not None else sc.pal["glow"], pips, shimmer=None, twinkle=tw, spec=spec, rank=rank)


def shifted_neighbour_mask(b):
    """True where both the pixel below and the pixel to the right are inside b (so ~ this inside b = the lower right edge)."""
    return shifted(b, 0, -1) & shifted(b, -1, 0)


def to_image(arr):
    return Image.fromarray(arr, "RGBA")

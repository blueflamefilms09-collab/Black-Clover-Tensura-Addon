"""The skin painter of the genesis_weapons upgrade: hand-authored-looking surfaces from maths (numpy + PIL), painted in model space.

A painter gets a `Ctx` for every UV group of a part: the skin / glow / vein views (float32 RGBA 0..255) and, for every texel, the model-space coordinates
(X, Y, Z on a dominant group, arc / length on a loft group), the position T / S along and across the form, and helpers to draw engraved text, inlay
lines, rune rows and edge wear. Everything is deterministic (seeded by the part name).

Materials (`surface`): brushed / hammered metal with dents lit by a fake bump light, black glass with violet caustics, enamel, leather, silk, bone, crystal,
basalt, star-metal. They are the base; part painters (engraving, veins, enamel inlays, runes, wear) are drawn over them.
"""
import math
import os
import zlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

FONT_DIRS = ["/usr/share/fonts/truetype/dejavu", "/usr/share/fonts/truetype/freefont", "/usr/share/fonts/truetype/noto", "/usr/share/fonts/truetype/liberation"]
_FONTS = {}


def font(name, px):
    key = (name, int(px))
    f = _FONTS.get(key)
    if f is None:
        for d in FONT_DIRS:
            p = os.path.join(d, name)
            if os.path.exists(p):
                f = ImageFont.truetype(p, max(4, int(px)))
                break
        if f is None:
            f = ImageFont.load_default()
        _FONTS[key] = f
    return f


# ================================================================ noise
def _grid(rng, gh, gw):
    return rng.random((gh, gw), dtype=np.float32)


def vnoise(shape, cell, seed, cell_b=None):
    """Smooth value noise in 0..1 with features of about `cell` px (cell_b across, if different)."""
    h, w = int(shape[0]), int(shape[1])
    cb = cell if cell_b is None else cell_b
    rng = np.random.default_rng(seed & 0xFFFFFFFF)
    g = _grid(rng, max(2, int(math.ceil(h / cell)) + 2), max(2, int(math.ceil(w / cb)) + 2))
    return np.clip(np.asarray(Image.fromarray(g).resize((w, h), Image.BICUBIC)), 0, 1)


def fbm(shape, cell, seed, octaves=4, gain=0.55, cell_b=None):
    """Fractal noise in 0..1."""
    out = np.zeros(shape, np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = max(1.0, cell / (2 ** o))
        cb = None if cell_b is None else max(1.0, cell_b / (2 ** o))
        out += amp * vnoise(shape, c, seed * 31 + o * 977, cb)
        tot += amp
        amp *= gain
    return out / tot


def voronoi(shape, cell, seed, jitter=0.95):
    """(F1, F2, id) of a jittered grid of feature points, distances in px. Hammer dents, facets, scales, caustic webs."""
    h, w = int(shape[0]), int(shape[1])
    rng = np.random.default_rng(seed & 0xFFFFFFFF)
    gh, gw = int(math.ceil(h / cell)) + 3, int(math.ceil(w / cell)) + 3
    px = (np.arange(gw)[None, :] + 0.5 + (rng.random((gh, gw)) - 0.5) * jitter) * cell
    py = (np.arange(gh)[:, None] + 0.5 + (rng.random((gh, gw)) - 0.5) * jitter) * cell
    ident = rng.random((gh, gw)).astype(np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    cy = (yy // cell).astype(np.int32) + 1
    cx = (xx // cell).astype(np.int32) + 1
    f1 = np.full((h, w), 1e9, np.float32)
    f2 = np.full((h, w), 1e9, np.float32)
    ids = np.zeros((h, w), np.float32)
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            qy, qx = np.clip(cy + dy, 0, gh - 1), np.clip(cx + dx, 0, gw - 1)
            d = np.hypot(px[qy, qx] - xx - cell, py[qy, qx] - yy - cell)           # the +cell offset: the grid starts one cell before the patch
            better = d < f1
            f2 = np.where(better, f1, np.minimum(f2, d))
            ids = np.where(better, ident[qy, qx], ids)
            f1 = np.where(better, d, f1)
    return f1, f2, ids


def smoothstep(x, a, b):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def soft(d, w):
    """1 at d = 0 falling to 0 at |d| = w."""
    return np.clip(1.0 - np.abs(d) / max(w, 1e-6), 0.0, 1.0)


def _box(a, k, axis):
    """Running mean of width 2k+1 along an axis (edge values repeated)."""
    if k <= 0:
        return a
    n = a.shape[axis]
    pad = [(0, 0)] * a.ndim
    pad[axis] = (k + 1, k)
    p = np.pad(a, pad, mode="edge")
    c = np.cumsum(p, axis=axis, dtype=np.float64)
    hi = np.take(c, np.arange(2 * k + 1, 2 * k + 1 + n), axis=axis)
    lo = np.take(c, np.arange(0, n), axis=axis)
    return ((hi - lo) / (2 * k + 1)).astype(np.float32)


def blur(a, r):
    """Gaussian-like blur (three box passes) of a float array, sigma `r` px."""
    if r <= 0.05:
        return a
    k = max(1, int(round(math.sqrt(3.0 * r * r / 3.0 + 0.25))))
    out = np.asarray(a, np.float32)
    for _ in range(3):
        out = _box(_box(out, k, 0), k, 1)
    return out


def bump(height, k=1.0, light=(-0.6, 0.8)):
    """Lambert-ish shading of a height field (px units): ~1 flat, brighter facing the light, darker facing away."""
    gy, gx = np.gradient(height)
    return 1.0 + k * (-gx * light[0] - gy * light[1])


def lerp3(a, b, t):
    a = np.asarray(a, np.float32)
    b = np.asarray(b, np.float32)
    return a[None, None, :] * (1 - t[..., None]) + b[None, None, :] * t[..., None]


# ================================================================ materials
MATS = {
    # kind: base, highlight, shadow, brushed streak, grain, dent amplitude, dent cell (px at 10 ppu), patina amount, spec power
    "steel":     dict(base=(150, 158, 172), hi=(246, 250, 255), lo=(52, 58, 72), streak=.10, grain=.05, dent=.35, cell=7, patina=.10, spec=3.0),
    "silver":    dict(base=(196, 202, 214), hi=(255, 255, 255), lo=(88, 94, 112), streak=.07, grain=.04, dent=.22, cell=8, patina=.06, spec=3.0),
    "gold":      dict(base=(214, 168, 64), hi=(255, 240, 160), lo=(96, 62, 18), streak=.08, grain=.05, dent=.30, cell=7, patina=.10, spec=3.2),
    "bronze":    dict(base=(160, 106, 58), hi=(240, 190, 120), lo=(58, 34, 18), streak=.10, grain=.07, dent=.45, cell=6, patina=.22, spec=2.6),
    "iron":      dict(base=(76, 80, 94), hi=(176, 184, 202), lo=(20, 22, 30), streak=.12, grain=.09, dent=.55, cell=6, patina=.22, spec=2.4),
    "obsidian":  dict(base=(22, 18, 32), hi=(150, 120, 220), lo=(4, 2, 8), streak=.04, grain=.03, dent=.10, cell=12, patina=.05, spec=4.5),
    "starmetal": dict(base=(26, 32, 58), hi=(150, 190, 255), lo=(4, 6, 16), streak=.06, grain=.05, dent=.20, cell=9, patina=.08, spec=3.6),
    "basalt":    dict(base=(70, 66, 74), hi=(168, 160, 178), lo=(18, 16, 22), streak=.05, grain=.16, dent=.50, cell=8, patina=.30, spec=1.8),
    "bone":      dict(base=(226, 214, 180), hi=(252, 246, 226), lo=(120, 104, 74), streak=.05, grain=.08, dent=.18, cell=9, patina=.20, spec=1.6),
    "leather":   dict(base=(96, 60, 38), hi=(160, 110, 70), lo=(30, 16, 10), streak=.04, grain=.16, dent=.30, cell=3, patina=.18, spec=1.4),
    "cloth":     dict(base=(150, 36, 48), hi=(230, 110, 120), lo=(46, 8, 14), streak=.05, grain=.09, dent=.25, cell=4, patina=.10, spec=1.8),
    "wood":      dict(base=(104, 70, 40), hi=(168, 124, 76), lo=(34, 20, 10), streak=.16, grain=.10, dent=.12, cell=10, patina=.12, spec=1.6),
    "crystal":   dict(base=(96, 190, 228), hi=(236, 252, 255), lo=(20, 60, 96), streak=.04, grain=.03, dent=.10, cell=10, patina=.04, spec=4.0),
    "enamel":    dict(base=(40, 90, 200), hi=(190, 220, 255), lo=(8, 20, 70), streak=.02, grain=.02, dent=.08, cell=12, patina=.02, spec=5.0),
}


class Ctx:
    """One UV group's patch (with a 1 px bleed round it) and the model-space coordinates of every texel."""

    def __init__(self, atlas, part, group, bleed=1):
        self.atlas, self.part, self.group = atlas, part, group
        x, y, w, h = group.rect
        b = bleed
        self.x0, self.y0, self.w, self.h, self.b = x - b, y - b, w + 2 * b, h + 2 * b, b
        sl = (slice(self.y0, self.y0 + self.h), slice(self.x0, self.x0 + self.w))
        self.img, self.glow, self.vein = atlas.img[sl], atlas.glow[sl], atlas.veins[sl]
        self.ppu = group.ppu
        self.kind = group.kind
        self.axis, self.sign = (group.key[0], group.key[1]) if group.kind == "dom" else (-1, 0)
        yy, xx = np.mgrid[0:self.h, 0:self.w].astype(np.float32)
        xx -= b
        yy -= b
        if group.kind == "loft":
            ext = group.ext
            self.A = (xx + 0.5) / self.ppu
            self.B = (1.0 - (yy + 0.5) / max(group.rect[3], 1)) * ext[1]
            self.T = np.clip(self.B / max(ext[1], 1e-6), 0, 1)
            self.S = (xx + 0.5) / max(group.rect[2], 1)
            self.X = self.A
            self.Y = self.B
            self.Z = np.zeros_like(self.A)
            self.length, self.perim = ext[1], ext[0]
        else:
            self.A = group.origin[0] + (xx + 0.5) / self.ppu
            self.B = group.origin[1] - (yy + 0.5) / self.ppu
            sg = self.sign if self.sign else 1
            if self.axis == 2:
                self.X, self.Y, self.Z = self.A * sg, self.B, np.zeros_like(self.A)
            elif self.axis == 0:
                self.X, self.Y, self.Z = np.zeros_like(self.A), self.B, -self.A * sg
            else:
                self.X, self.Y, self.Z = self.A, np.zeros_like(self.A), -self.B * sg
            ys = [v[1] for v in part.V]
            lo, hi = min(ys), max(ys)
            self.T = np.clip((self.Y - lo) / max(hi - lo, 1e-6), 0, 1) if self.axis != 1 else np.zeros_like(self.A)
            ext = group.ext
            self.S = np.clip((xx + 0.5) / max(group.rect[2], 1), 0, 1)
            self.length, self.perim = ext[1], ext[0]
        self.shape = (self.h, self.w)
        self.seed = zlib.crc32(part.name.encode()) % 100003 + (group.key[0] * 7 + group.key[1] * 3 if group.kind == "dom" else 0)
        self._cover = None

    # ---- coordinates -> texels
    def to_px(self, a, b):
        """Texel (x, y) in the patch of the plane coordinates (a right, b up) of a dominant group, or (arc, length) of a loft group."""
        g = self.group
        if g.kind == "loft":
            return a * self.ppu + self.b, (g.ext[1] - b) * self.ppu + self.b
        return (a - g.origin[0]) * self.ppu + self.b, (g.origin[1] - b) * self.ppu + self.b

    def plane(self, x=0.0, y=0.0, z=0.0):
        """Model point -> plane coordinates (a, b) of this dominant group."""
        sg = self.sign if self.sign else 1
        if self.axis == 2:
            return x * sg, y
        if self.axis == 0:
            return -z * sg, y
        return x, -z * sg

    def cover(self):
        """Float 0..1: the texels some quad of the group covers (the rest of the rect is unused skin)."""
        if self._cover is None:
            im = Image.new("L", (self.w, self.h), 0)
            d = ImageDraw.Draw(im)
            k = self.atlas.k
            for f in self.group.faces:
                uv = self.part.UV[f]
                d.polygon([(u * k - self.x0, v * k - self.y0) for (u, v) in uv], fill=255)
            self._cover = np.asarray(im, np.float32) / 255.0
        return self._cover

    def edge(self, r=2.0):
        """1 on the rim of the covered area falling to 0 `r` px inside."""
        c = self.cover()
        return np.clip(1.0 - (blur(c, r) - 0.5) * 2.0, 0, 1) * c

    def cavity(self, r=3.0):
        """Darkening where the surface dips (blurred minus sharp height): a cheap ambient occlusion from the cover mask."""
        c = self.cover()
        return np.clip(c - blur(c, r), 0, 1)


def surface(ctx, kind, base=None, along="v", wear=0.5, seed_off=0, dent=None, streak=None, patina=None):
    """The base colour of a patch (float RGB 0..255) and its height field. `along` = the brushing direction ('v' texel rows run along the form, 'u' columns)."""
    M = dict(MATS[kind])
    if base is not None:
        M["base"] = tuple(base)
        M["hi"] = tuple(min(255, c * 0.55 + 140) for c in base) if "hi_auto" not in M else M["hi"]
        M["lo"] = tuple(c * 0.30 for c in base)
    if dent is not None:
        M["dent"] = dent
    if streak is not None:
        M["streak"] = streak
    if patina is not None:
        M["patina"] = patina
    sh = ctx.shape
    seed = ctx.seed + seed_off * 7919 + zlib.crc32(kind.encode()) % 997
    ppu = max(ctx.ppu, 1.0)
    cell = max(2.0, M["cell"] * ppu / 10.0)
    # height field: dents (voronoi cups) + low waves + grain
    f1, f2, ids = voronoi(sh, cell, seed + 1)
    cup = np.clip(f1 / (cell * 0.62), 0, 1)
    ridge = np.clip((f2 - f1) / (cell * 0.28), 0, 1)
    height = (-(1.0 - cup ** 2) * 0.9 + (ridge - 1.0) * 0.18) * M["dent"] * cell * 0.5
    height += (fbm(sh, cell * 6, seed + 2, 3) - 0.5) * cell * 0.9 * M["dent"]
    gcell = (1.2, 1.2) if along == "v" else (1.2, 1.2)
    grain = (vnoise(sh, 1.4, seed + 3) - 0.5) * 2.0
    height += grain * M["grain"] * 1.2
    if M["streak"] > 0:
        st = vnoise(sh, 40.0, seed + 4, cell_b=1.3) if along == "v" else vnoise(sh, 1.3, seed + 4, cell_b=40.0)
        height += (st - 0.5) * M["streak"] * 6.0
    lit = bump(height, 0.9)
    lit = np.clip(lit, 0.0, 2.0)
    # colour: shadow..base..highlight by the lit term, patina blotches, a broad sheen band across the form
    t = np.clip((lit - 0.55) / 0.9, 0, 1)
    lo, ba, hi = np.array(M["lo"], np.float32), np.array(M["base"], np.float32), np.array(M["hi"], np.float32)
    col = np.where((t < 0.5)[..., None], lo[None, None, :] + (ba - lo)[None, None, :] * (t * 2)[..., None], ba[None, None, :] + (hi - ba)[None, None, :] * ((t - 0.5) * 2 ** 1.0)[..., None] * 0.9)
    spec = np.clip((lit - 1.0) * 1.6, 0, 1) ** M["spec"]
    col = col + (hi - col) * (spec * 0.55)[..., None]
    pat = fbm(sh, cell * 9, seed + 5, 4)
    tint_dark = np.clip((pat - 0.55) * 2.0, 0, 1) * M["patina"]
    col = col * (1.0 - tint_dark[..., None] * 0.8)
    band = np.exp(-((ctx.S - 0.32) / 0.22) ** 2) * 0.16 if ctx.kind == "loft" else 0.0
    col = col * (1.0 + np.asarray(band)[..., None] if np.ndim(band) else 1.0)
    # wear: bright worn rim, dark cavities
    if wear > 0:
        e = ctx.edge(max(1.0, 0.9 * ppu / 6.0))
        wn = fbm(sh, cell * 2.0, seed + 6, 3)
        worn = np.clip(e * (wn * 1.6 - 0.2), 0, 1) * wear
        col = col + (hi - col) * (worn * 0.55)[..., None]
        cav = ctx.cavity(max(1.5, 0.9 * ppu / 4.0))
        col = col * (1.0 - cav[..., None] * 0.35 * min(1.0, wear * 1.6))
    return np.clip(col, 0, 255), height


def base_paint(ctx):
    """Default painter: the part's material over the whole patch (colour + alpha 255)."""
    p = ctx.part
    kw = getattr(p, "mat", {})
    col, height = surface(ctx, p.material, base=p.base, along=p.along or ("v" if ctx.kind == "loft" or ctx.length >= ctx.perim else "u"), wear=p.wear, **kw)
    ctx.img[..., :3] = col
    ctx.img[..., 3] = 255
    return height


# ================================================================ drawing helpers (on a Ctx)
def _mask(ctx, fn):
    im = Image.new("L", (ctx.w, ctx.h), 0)
    fn(ImageDraw.Draw(im))
    return np.asarray(im, np.float32) / 255.0


def mask_polyline(ctx, pts, width, soft_px=0.7):
    """Mask of a polyline in plane coordinates [(a, b)], `width` in model units."""
    px = [ctx.to_px(a, b) for (a, b) in pts]
    wpx = max(1.0, width * ctx.ppu)
    m = _mask(ctx, lambda d: d.line(px, fill=255, width=int(round(wpx)), joint="curve"))
    return blur(m, soft_px) if soft_px > 0 else m


def mask_polygon(ctx, pts, soft_px=0.7):
    px = [ctx.to_px(a, b) for (a, b) in pts]
    m = _mask(ctx, lambda d: d.polygon(px, fill=255))
    return blur(m, soft_px) if soft_px > 0 else m


def mask_ellipse(ctx, cx, cy, rx, ry, soft_px=0.7):
    x0, y0 = ctx.to_px(cx - rx, cy + ry)
    x1, y1 = ctx.to_px(cx + rx, cy - ry)
    m = _mask(ctx, lambda d: d.ellipse([x0, y0, x1, y1], fill=255))
    return blur(m, soft_px) if soft_px > 0 else m


def mask_ring(ctx, cx, cy, r, width, soft_px=0.7):
    return np.clip(mask_ellipse(ctx, cx, cy, r + width / 2, r + width / 2, soft_px) - mask_ellipse(ctx, cx, cy, r - width / 2, r - width / 2, soft_px), 0, 1)


def mask_text(ctx, text, a, b, size, rot=0.0, fname="DejaVuSerif-Bold.ttf", spacing=1.0, anchor="mm"):
    """Mask of `text` centred at plane point (a, b), cap height about `size` model units, turned by rot degrees."""
    px = max(5.0, size * ctx.ppu * 1.35)
    f = font(fname, px)
    x, y = ctx.to_px(a, b)
    layer = Image.new("L", (ctx.w, ctx.h), 0)
    if abs(rot) < 0.01 and spacing == 1.0:
        ImageDraw.Draw(layer).text((x, y), text, fill=255, font=f, anchor=anchor)
    else:
        tw = int(len(text) * px * 1.1 * spacing) + 8
        tmp = Image.new("L", (tw, int(px * 2.2)), 0)
        d = ImageDraw.Draw(tmp)
        cx = 4.0
        for chh in text:
            d.text((cx, px * 1.1), chh, fill=255, font=f, anchor="lm")
            cx += f.getlength(chh) * spacing
        tmp = tmp.crop((0, 0, int(cx + 4), tmp.height))
        tmp = tmp.rotate(rot, expand=True, resample=Image.BICUBIC)
        layer.paste(tmp, (int(x - tmp.width / 2), int(y - tmp.height / 2)), tmp)
    return np.asarray(layer, np.float32) / 255.0


def engrave(ctx, m, depth=0.55, rim=0.18, color=None):
    """Cuts a mask into the skin: darkens (or paints `color`) and lights the lower-left rim a little, as a chisel groove would."""
    if m.max() <= 0:
        return
    if color is None:
        ctx.img[..., :3] *= (1.0 - depth * m)[..., None]
    else:
        ctx.img[..., :3] = ctx.img[..., :3] * (1 - m[..., None]) + np.array(color, np.float32)[None, None, :] * m[..., None]
    sh = np.roll(np.roll(m, 1, axis=0), 1, axis=1)
    lip = np.clip(sh - m, 0, 1)
    ctx.img[..., :3] += (255.0 - ctx.img[..., :3]) * (rim * lip)[..., None]


def inlay(ctx, m, rgb, gloss=0.35, edge=0.5):
    """An enamel / metal inlay: flat colour with a dark rim and a soft top light."""
    if m.max() <= 0:
        return
    c = np.array(rgb, np.float32)
    inner = np.clip(blur(m, 1.6) * 1.6 - 0.6, 0, 1) * m
    rimm = np.clip(m - inner, 0, 1)
    y = np.linspace(1.0, 0.0, ctx.h, dtype=np.float32)[:, None]
    col = c[None, None, :] * (0.8 + 0.4 * y * gloss * 2)[..., None]
    ctx.img[..., :3] = ctx.img[..., :3] * (1 - m[..., None]) + col * m[..., None]
    ctx.img[..., :3] *= (1.0 - edge * rimm)[..., None]
    ctx.img[..., :3] += (255 - ctx.img[..., :3]) * (gloss * 0.25 * np.clip(blur(m, 3.0) - 0.3, 0, 1) * m)[..., None]


def put(buf, m, rgb, strength=1.0, core=0.35, halo=0.0):
    """Writes a mask into a glow / vein buffer: a hot core (`core` of the way to white) and an optional soft halo of `halo` px."""
    if m.max() <= 0:
        return
    mm = np.clip(m, 0, 1)
    if halo > 0:
        mm = np.maximum(mm, blur(mm, halo) * 0.75)
    c = np.array(rgb, np.float32)
    hot = c + (255 - c) * core
    col = c[None, None, :] + (hot - c)[None, None, :] * np.clip(m, 0, 1)[..., None]
    a = mm * 255 * strength
    new = np.concatenate([col, a[..., None]], axis=2)
    use = a > buf[..., 3]
    buf[use] = new[use]


def rune_glyphs(n, seed):
    """n short runic strings (Elder Futhark letters) for engraving rows."""
    letters = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟ"
    rng = np.random.default_rng(seed)
    return ["".join(letters[int(rng.integers(0, len(letters)))] for _ in range(int(rng.integers(3, 6)))) for _ in range(n)]


def stars(ctx, density, seed, rgb=(255, 255, 255), size=1.0, bright=1.0, buf="img", strength=1.0):
    """Scatters star points (and a few 4-point flares) over a patch; into the skin (`img`) or a glow buffer."""
    rng = np.random.default_rng(seed + ctx.seed)
    area = ctx.w * ctx.h
    n = int(area * density)
    if n <= 0:
        return
    xs, ys = rng.integers(1, max(2, ctx.w - 1), n), rng.integers(1, max(2, ctx.h - 1), n)
    mag = rng.random(n) ** 3
    m = np.zeros(ctx.shape, np.float32)
    for x, y, g in zip(xs, ys, mag):
        m[y, x] = max(m[y, x], 0.35 + 0.65 * g)
        if g > 0.8 and size > 0:
            m[y, x - 1:x + 2] = np.maximum(m[y, x - 1:x + 2], 0.45)
            m[y - 1:y + 2, x] = np.maximum(m[y - 1:y + 2, x], 0.45)
    m *= bright
    if buf == "img":
        ctx.img[..., :3] = ctx.img[..., :3] * (1 - m[..., None]) + np.array(rgb, np.float32)[None, None, :] * m[..., None]
    else:
        put(getattr(ctx, buf), m, rgb, strength, 0.5)


def caustics(ctx, scale, seed, power=2.0):
    """A bright network of thin lines (the caustic web of light inside glass / water): 0..1."""
    f1, f2, _ = voronoi(ctx.shape, max(3.0, scale * ctx.ppu / 10.0), ctx.seed + seed)
    d = np.clip((f2 - f1) / max(2.0, scale * ctx.ppu / 10.0 * 0.22), 0, 1)
    return (1.0 - d) ** power


def tint(ctx, rgb, k=1.0, mask=None):
    """Colours the skin toward `rgb` keeping its luminance."""
    lum = ctx.img[..., :3].mean(axis=2, keepdims=True) / 128.0
    tgt = np.clip(lum * np.array(rgb, np.float32)[None, None, :], 0, 255)
    kk = np.asarray(k, np.float32) if mask is None else np.clip(mask, 0, 1) * np.asarray(k, np.float32)
    if kk.ndim == 2:
        kk = kk[..., None]
    ctx.img[..., :3] = ctx.img[..., :3] * (1 - kk) + tgt * kk


def crack_star(ctx, center, radius, n=9, seed=0, width=0.35, branch=2, jag=0.28):
    """A mask of cracks radiating from `center` (plane coordinates (a, b)) to `radius`: n main fissures as jagged random walks, each with side branches (a star-split face)."""
    rng = np.random.default_rng(seed + ctx.seed)
    m = np.zeros(ctx.shape, np.float32)

    def walk(p0, ang, length, w, depth):
        nonlocal m
        pts = [p0]
        steps = max(4, int(length / (radius * 0.07)))
        a = ang
        p = np.array(p0, float)
        for _ in range(steps):
            a += rng.uniform(-jag, jag)
            p = p + np.array([math.cos(a), math.sin(a)]) * (length / steps)
            pts.append((float(p[0]), float(p[1])))
        m = np.maximum(m, mask_polyline(ctx, pts, w, 0.8))
        if depth > 0:
            for k in range(branch):
                i = int(rng.integers(steps // 3, steps))
                sa = ang + rng.choice([-1, 1]) * rng.uniform(0.4, 0.9)
                walk(pts[i], sa, length * rng.uniform(0.25, 0.5), w * 0.6, depth - 1)

    for k in range(n):
        ang = 2 * math.pi * k / n + rng.uniform(-0.2, 0.2)
        walk(tuple(center), ang, radius * rng.uniform(0.7, 1.0), width, 2)
    return m

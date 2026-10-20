"""Paint module of the Erza armour remake (1.99.0): procedural, hand-authored surfaces for the GeoLite armour pieces, at the Djinn resolution or higher.

Every face of every box is painted separately (the per-face uv of the Bedrock format lets a 3 px plate carry 6 to 8 texels per model pixel), from a HEIGHT FIELD that is then lit:

    pattern -> height, tone, accent mask, glow mask, alpha        (one pattern per material kind)
    height  -> normal -> a fixed light from the top left  (bevel highlights on the lit edges, shadow on the far ones)
    edge distance -> ambient occlusion in the corners and along the seams
    wear: worn-bright edges, scratches, patina blotches        (Mat.wear)
    decals: inlays, engravings, gems, emblems (heart, cross, sun, bolt, wave, feather, rune, wheel, flame, star ...), drawn at 4x and reduced
    ramp: tone -> colour through the material's dark / base / light colours, the accent colour where the accent mask is set
    glow: a separate emissive map (the engravings, the inlays, the gems, the veins), white hot in the core and in the armour's glow colour at the fringe

Face orientation of the GeoLite box net (see client/geo/GeoDraw): on the four side faces v runs DOWN the face; u runs toward +x on the north (front) face, toward -x on the south (back) face,
toward the front (-z) on the west face and toward the back (+z) on the east face. On the top face v runs from the back to the front, on the bottom face the net is stored flipped.

Deterministic: every random number comes from a generator seeded by the material seed and the face tag. Run python as `python3 -B`.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import water_aura_lib as WL  # noqa: E402

fbm = WL.fbm
smooth = WL.smooth


def rgb(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)


def lerp(a, b, t):
    return a + (b - a) * t


class Mat:
    """A material. kind: plate steel gold scales feather cloth leather gem | flame bolt wisp crystal water mane (the energy kinds are the 1.93.0 painters).

    base / dark / light : the ramp (dark = recess and shadow, base = the body colour, light = highlights and worn-bright metal)
    accent              : the second colour of the surface (gold trim on a painted plate, the hem of a cloth); None = the light colour
    glow / glow_amount  : the emissive colour and how much of the surface carries it (0 = no glow pass at all for this material)
    wear 0..1           : edge wear, scratches, blotches        shine 0..1 : how hard the lit side of the bevels is (metal 0.8, cloth 0.15)
    kw                  : trim=1 an accent border · inlay=1 a glowing inlay line · grooves=1 panel lines · rivets=0/1 · ribbed=1 horizontal lames · filigree=1 · cells=n (scales / crystals)
    """

    def __init__(self, kind, base, dark=None, light=None, accent=None, glow=None, glow_amount=0.5, alpha=255, seed=0, wear=0.35, shine=0.6, **kw):
        self.kind, self.alpha, self.seed, self.wear, self.shine = kind, alpha, seed, wear, shine
        self.base = rgb(base)
        self.dark = rgb(dark) if dark else self.base * 0.5
        self.light = rgb(light) if light else np.minimum(255.0, self.base * 1.4 + 24)
        self.accent = rgb(accent) if accent else None
        self.glow = rgb(glow) if glow else None
        self.glow_amount = glow_amount
        self.kw = kw

    def with_(self, **ch):
        m = Mat(self.kind, "000000")
        m.__dict__.update(self.__dict__)
        m.kw = dict(self.kw)
        for k, v in ch.items():
            if k in ("base", "dark", "light", "accent", "glow") and isinstance(v, str):
                v = rgb(v)
            if k in m.__dict__:
                m.__dict__[k] = v
            else:
                m.kw[k] = v
        return m

    @property
    def has_glow(self):
        return self.glow is not None and self.glow_amount > 0


# ====================================================================================================================== small shapes for decals
def _heart(n=48):
    t = np.linspace(0, 2 * math.pi, n, endpoint=False)
    x = 16 * np.sin(t) ** 3
    y = 13 * np.cos(t) - 5 * np.cos(2 * t) - 2 * np.cos(3 * t) - np.cos(4 * t)
    return [(float(a) / 17.0, float(-b) / 17.0 + 0.04) for a, b in zip(x, y)]


def _star(n, r0, r1, rot=-math.pi / 2):
    out = []
    for i in range(2 * n):
        r = r1 if i % 2 == 0 else r0
        a = rot + i * math.pi / n
        out.append((r * math.cos(a), r * math.sin(a)))
    return out


SHAPES = {
    "heart": [_heart()],
    "cross": [[(-0.16, -1), (0.16, -1), (0.16, -0.16), (1, -0.16), (1, 0.16), (0.16, 0.16), (0.16, 1), (-0.16, 1), (-0.16, 0.16), (-1, 0.16), (-1, -0.16), (-0.16, -0.16)]],
    "kreuz": [[(-0.2, -1), (0.2, -1), (0.12, -0.2), (1, -0.2), (1, 0.2), (0.12, 0.2), (0.2, 1), (-0.2, 1), (-0.12, 0.2), (-1, 0.2), (-1, -0.2), (-0.12, -0.2)]],
    "sun": [_star(8, 0.45, 1.0), [(0.4 * math.cos(a / 24 * 2 * math.pi), 0.4 * math.sin(a / 24 * 2 * math.pi)) for a in range(24)]],
    "star": [_star(5, 0.42, 1.0)],
    "star4": [_star(4, 0.28, 1.0)],
    "bolt": [[(0.15, -1), (-0.55, 0.12), (-0.05, 0.12), (-0.3, 1), (0.6, -0.2), (0.05, -0.2), (0.45, -1)]],
    "diamond": [[(0, -1), (0.62, 0), (0, 1), (-0.62, 0)]],
    "drop": [[(0, -1), (0.55, 0.15), (0.45, 0.6), (0, 1), (-0.45, 0.6), (-0.55, 0.15)]],
    "flame": [[(0, -1), (0.28, -0.45), (0.62, -0.1), (0.55, 0.55), (0.0, 1), (-0.55, 0.55), (-0.62, -0.1), (-0.28, -0.45)]],
    "feather": [[(0, -1), (0.42, -0.5), (0.5, 0.1), (0.3, 0.7), (0.04, 1), (-0.04, 1), (-0.3, 0.7), (-0.5, 0.1), (-0.42, -0.5)]],
    "wing": [[(-1, 0.5), (-0.7, -0.3), (-0.2, -0.8), (0.5, -1), (0.2, -0.5), (0.8, -0.55), (0.3, -0.1), (0.9, 0.0), (0.2, 0.3), (0.5, 0.8), (-0.3, 0.5)]],
    "crescent": [[(0.2, -1), (-0.55, -0.55), (-0.85, 0.1), (-0.55, 0.7), (0.2, 1), (-0.25, 0.55), (-0.45, 0.1), (-0.25, -0.5)]],
    "blade": [[(0, -1), (0.2, -0.7), (0.2, 0.55), (0.5, 0.65), (0.5, 0.8), (0.12, 0.8), (0.12, 1), (-0.12, 1), (-0.12, 0.8), (-0.5, 0.8), (-0.5, 0.65), (-0.2, 0.55), (-0.2, -0.7)]],
    "fang": [[(-0.45, -1), (0.45, -1), (0.0, 1)]],
    "shield": [[(-0.8, -0.8), (0.8, -0.8), (0.8, 0.1), (0, 1), (-0.8, 0.1)]],
}
# runes: strokes (x0, y0, x1, y1) in -1..1
RUNES = [
    [(-0.3, -1, -0.3, 1), (-0.3, -0.2, 0.5, -0.9), (-0.3, 0.3, 0.5, -0.4)],
    [(0, -1, 0, 1), (-0.6, -0.4, 0, 0.1), (0.6, -0.4, 0, 0.1), (-0.6, 0.6, 0.6, 0.6)],
    [(-0.5, -1, -0.5, 1), (0.5, -1, 0.5, 1), (-0.5, -0.5, 0.5, 0.5)],
    [(0, -1, 0.6, -0.3), (0.6, -0.3, -0.6, 0.3), (-0.6, 0.3, 0, 1), (0, -1, 0, 1)],
    [(-0.6, -1, 0.6, -1), (-0.6, -1, -0.6, 0.2), (0.6, -1, 0.6, 0.2), (-0.6, 0.2, 0.6, 1)],
    [(-0.5, 1, 0, -1), (0, -1, 0.5, 1), (-0.3, 0.2, 0.3, 0.2)],
    [(-0.55, -0.8, 0.55, 0.8), (0.55, -0.8, -0.55, 0.8), (0, -1, 0, 1)],
    [(0, -1, 0, 1), (0, -0.6, 0.6, -0.1), (0, 0.1, 0.6, 0.6)],
]
SS = 4                                                   # supersampling of the decals


def shape_mask(name, w, h, size, cx=0.5, cy=0.5, rot=0.0, flip=False, seed=0, stroke=0.12):
    """The mask (h x w, 0..1) of a shape. `size` is the shape's radius as a share of the face's smaller side; the centre (cx, cy) is in 0..1 face coordinates."""
    W, H = w * SS, h * SS
    im = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(im)
    r = size * min(W, H) * 0.5
    ca, sa = math.cos(math.radians(rot)), math.sin(math.radians(rot))

    def tr(x, y):
        if flip:
            x = -x
        return (cx * W + r * (x * ca - y * sa), cy * H + r * (x * sa + y * ca))

    if name == "rune":
        g = RUNES[seed % len(RUNES)]
        for (x0, y0, x1, y1) in g:
            d.line([tr(x0, y0), tr(x1, y1)], fill=255, width=max(1, int(r * stroke * 2)))
    elif name == "ring":
        x0, y0 = tr(0, 0)
        d.ellipse([x0 - r, y0 - r, x0 + r, y0 + r], outline=255, width=max(1, int(r * stroke * 2)))
    elif name == "disc":
        x0, y0 = tr(0, 0)
        d.ellipse([x0 - r, y0 - r, x0 + r, y0 + r], fill=255)
    elif name == "wheel":
        x0, y0 = tr(0, 0)
        d.ellipse([x0 - r, y0 - r, x0 + r, y0 + r], outline=255, width=max(1, int(r * stroke * 2)))
        for k in range(8):
            a = k * math.pi / 4
            d.line([tr(0, 0), tr(math.cos(a), math.sin(a))], fill=255, width=max(1, int(r * stroke * 1.4)))
        d.ellipse([x0 - r * 0.22, y0 - r * 0.22, x0 + r * 0.22, y0 + r * 0.22], fill=255)
    elif name == "wave":
        for k in range(3):
            pts = [tr(-1 + 2 * i / 24.0, -0.55 + k * 0.55 + 0.22 * math.sin(i / 24.0 * 2 * math.pi * 1.5 + k)) for i in range(25)]
            d.line(pts, fill=255, width=max(1, int(r * stroke * 1.6)))
    elif name == "line":
        d.line([tr(-1, 0), tr(1, 0)], fill=255, width=max(1, int(r * stroke * 2)))
    elif name == "vline":
        d.line([tr(0, -1), tr(0, 1)], fill=255, width=max(1, int(r * stroke * 2)))
    elif name in SHAPES:
        for poly in SHAPES[name]:
            d.polygon([tr(x, y) for x, y in poly], fill=255)
    a = np.asarray(im.resize((w, h), Image.BOX), dtype=np.float32) / 255.0
    return a


class Decal:
    """A mark on one face. shape: see SHAPES (+ rune ring disc wheel wave line vline); size: radius as a share of the smaller side of the face; at: centre in 0..1 face coordinates.
    mode: 'inlay' (the accent colour, raised) · 'glow' (engraved, emissive) · 'gem' (a faceted jewel, emissive) · 'dark' / 'light' (painted) · 'cut' (engraved, no glow)."""

    def __init__(self, shape, size=0.8, at=(0.5, 0.5), mode="inlay", rot=0.0, flip=False, seed=0, stroke=0.12, strength=1.0):
        self.shape, self.size, self.at, self.mode, self.rot, self.flip, self.seed, self.stroke, self.strength = shape, size, at, mode, rot, flip, seed, stroke, strength


# ====================================================================================================================== patterns
def _edges(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    ex = np.minimum(xx + 0.5, w - xx - 0.5)
    ey = np.minimum(yy + 0.5, h - yy - 0.5)
    return xx, yy, ex, ey


def _line(coord, centre, half):
    """1 inside a band of half-width `half` (texels) round `centre`, antialiased."""
    return np.clip(half + 0.5 - np.abs(coord - centre), 0.0, 1.0)


def _rivet(xx, yy, cx, cy, r):
    d = np.hypot(xx + 0.5 - cx, yy + 0.5 - cy)
    dome = np.clip(1.0 - d / r, 0.0, 1.0)
    ring = np.clip(1.0 - np.abs(d - r * 1.25) / 0.6, 0.0, 1.0)
    return dome, ring


def _scratches(rng, w, h, n, length):
    im = Image.new("L", (w * 2, h * 2), 0)
    d = ImageDraw.Draw(im)
    for _ in range(n):
        x, y = rng.uniform(0, w * 2), rng.uniform(0, h * 2)
        a = rng.choice([rng.uniform(-0.4, 0.4), rng.uniform(math.pi / 2 - 0.4, math.pi / 2 + 0.4)])
        L = rng.uniform(0.3, 1.0) * length * 2
        d.line([(x, y), (x + math.cos(a) * L, y + math.sin(a) * L)], fill=int(rng.uniform(120, 255)), width=1)
    return np.asarray(im.resize((w, h), Image.BOX), dtype=np.float32) / 255.0


def _filigree(u, v, w, h, D, band_v, period=1.0):
    """A scroll pattern along u (a running vine: a sine with a dot in each trough), centred on v = band_v, 1 where the metal is engraved."""
    pitch = max(5.0, 3.2 * D * period)
    ph = (u * w / pitch) * 2 * math.pi
    y = band_v * h + np.sin(ph) * 0.9 * D * 0.5
    line = _line(v * h, y, max(0.45, 0.16 * D))
    dots = np.clip(1.0 - np.hypot((np.mod(u * w, pitch) - pitch * 0.5), (v * h - (band_v * h + 1.1 * D * 0.5 * np.sign(np.sin(ph + 1.5))))) / max(0.9, 0.3 * D), 0.0, 1.0)
    return np.maximum(line, dots * 0.9)


def pattern(mat, w, h, face, tag, dims, rng):
    """(tone 0..1, height 0..1, accent 0..1, glow 0..1, alpha 0..1) of a face of w x h texels whose size in model pixels is `dims`."""
    xx, yy, ex, ey = _edges(w, h)
    u, v = (xx + 0.5) / w, (yy + 0.5) / h
    edge = np.minimum(ex, ey)
    D = max(2.0, min(w / max(dims[0], 0.01), h / max(dims[1], 0.01)))             # texels per model pixel
    kind = mat.kind
    kw = mat.kw
    big = min(w, h) >= 3 * D * 0.9
    alpha = np.ones((h, w), np.float32)
    acc = np.zeros((h, w), np.float32)
    gl = np.zeros((h, w), np.float32)
    noise = fbm(rng, w, h, 3, 3)
    tone = np.full((h, w), 0.5, np.float32) + (noise - 0.5) * 0.10
    mn = float(min(w, h))
    bw = min(max(1.2, 0.5 * D), max(1.0, 0.2 * mn))                                 # the width of the rim in texels: never more than a fifth of the thinner side
    hgt = np.full((h, w), 0.5, np.float32)

    if kind in ("plate", "steel", "gold"):
        lip = smooth(edge, 0.0, bw * 0.55) * (1.0 - smooth(edge, bw * 0.55, bw * 1.05))
        hgt = 0.42 + 0.20 * smooth(edge, 0.0, bw * 0.6) + 0.18 * lip
        hgt += (noise - 0.5) * 0.04
        if kind == "steel":
            streak = fbm(rng, w, 1, 3, 6)[0][None, :] if face in ("up", "down") else fbm(rng, 1, h, 3, 6)[:, 0][:, None]
            tone = tone + (np.broadcast_to(streak, (h, w)) - 0.5) * 0.22
        elif kind == "gold":
            streak = fbm(rng, 1, h, 3, 5)[:, 0][:, None]
            tone = tone + (np.broadcast_to(streak, (h, w)) - 0.5) * 0.16 + 0.04 * np.sin(u * w * 0.9)
        else:
            tone = tone + 0.18 * (1.0 - v) - 0.06 * np.abs(u - 0.5)                  # enamel: the upper half of the plate catches more light
        if kw.get("grooves", 0) and big:
            for pos in (1.0 / 3.0, 2.0 / 3.0):
                gcoord = v if w >= h else u
                span = h if w >= h else w
                g = _line(gcoord * span, pos * span, 0.5)
                hgt -= 0.16 * g
                tone -= 0.12 * g
        if kw.get("lame", 0):                                # one lame of a shingled stack: dark where the plate above overlaps it, bright along its own lower edge
            low = _line(v * h, h - bw * 0.6, bw * 0.55)
            shadow = 1.0 - smooth(v, 0.0, 0.35)
            tone = tone * 0.55 + 0.22 + 0.34 * v ** 1.4 - 0.30 * shadow + 0.30 * low
            hgt = hgt + 0.25 * v - 0.25 * shadow + 0.30 * low
            if mat.accent is not None:
                acc = np.maximum(acc, _line(v * h, h - bw * 0.6, bw * 0.4))
        if kw.get("ribbed", 0):
            n = max(2, int(round(h / (1.8 * D))))
            ph = (v * n) % 1.0
            hgt += 0.22 * (1.0 - smooth(ph, 0.0, 0.18)) * smooth(ph, 0.0, 0.05) - 0.12 * smooth(ph, 0.82, 1.0)
            tone -= 0.10 * smooth(ph, 0.86, 1.0)
        if kw.get("trim", 0) and (mat.accent is not None) and mn >= 2.2 * D:
            acc = np.maximum(acc, _line(edge, bw * 0.55, bw * 0.40)) * 1.0
            hgt += 0.10 * acc
            if kw.get("filigree", 0) and big and min(w, h) >= 4.0 * D:
                f = _filigree(u, v, w, h, D, 0.5 if h <= w else 0.5)
                innr = smooth(edge, bw * 1.4, bw * 1.8)
                acc = np.maximum(acc, f * innr * 0.8)
        if kind == "gold" and kw.get("filigree", 1) and mn >= 1.6 * D:
            f = _filigree(u, v, w, h, D, 0.5)
            acc = np.maximum(acc, f * smooth(edge, 0.8, 1.4) * 0.7)
            hgt += 0.08 * f
        if kw.get("inlay", 0) and mat.has_glow and mn >= 2.5 * D:
            ring = _line(edge, bw * 1.65, max(0.5, 0.18 * D))
            ring *= smooth(edge, bw * 1.0, bw * 1.3)
            gl = np.maximum(gl, ring * 0.95)
            hgt -= 0.10 * ring
            if big and kw.get("inlay", 0) >= 2:
                cross = np.maximum(_line(u * w, w * 0.5, max(0.5, 0.16 * D)), _line(v * h, h * 0.5, max(0.5, 0.16 * D)))
                cross *= smooth(edge, bw * 1.7, bw * 2.2)
                gl = np.maximum(gl, cross * 0.8)
                hgt -= 0.08 * cross
        if kw.get("rivets", 1) and min(w, h) >= 4 * 0.9 * D:
            r = max(0.8, 0.30 * D)
            off = bw * 1.2 + r
            pts = [(off, off), (w - off, off), (off, h - off), (w - off, h - off)]
            if w > 5 * D:
                pts += [(w * 0.5, off), (w * 0.5, h - off)]
            if h > 5 * D:
                pts += [(off, h * 0.5), (w - off, h * 0.5)]
            for cx, cy in pts:
                dome, ring = _rivet(xx, yy, cx, cy, r)
                hgt += 0.5 * dome
                tone -= 0.10 * ring
                tone += 0.16 * dome
        if mat.has_glow and not kw.get("inlay", 0):
            gl = np.maximum(gl, (fbm(rng, w, h, 3, 4) > 0.86) * 0.45 * mat.glow_amount)
    elif kind == "scales":
        n = max(2.5, kw.get("cells", 4) * 0.5 * D / 3.0)
        cw = max(3.0, w / max(1.0, round(w / (n * D * 0.55))))
        ch = max(2.5, cw * 0.78)
        row = np.floor(v * h / ch)
        px = np.mod(u * w + np.mod(row, 2) * cw * 0.5, cw)
        py = np.mod(v * h, ch)
        d = np.hypot((px - cw * 0.5) / (cw * 0.52), (py - 0.0) / (ch * 1.02))
        inside = d < 1.0
        hgt = np.where(inside, 0.35 + 0.5 * np.clip(1.0 - d, 0, 1) ** 0.6, 0.15)
        tone = np.where(inside, 0.38 + 0.5 * np.clip(1.0 - d, 0, 1) ** 0.8 + 0.12 * (1.0 - py / ch), 0.14) + (noise - 0.5) * 0.12
        seam = np.abs(d - 1.0) < (0.55 / cw + 0.05)
        tone = np.where(seam, 0.10, tone)
        acc = np.where(inside & (d < 0.30), 0.55, 0.0).astype(np.float32) if mat.accent is not None else acc
        if mat.has_glow:
            gl = np.where(inside & (d < 0.22), mat.glow_amount * 0.9 * (1.0 - d / 0.22), 0.0)
        hgt = hgt * (0.7 + 0.3 * smooth(edge, 0.0, bw))
    elif kind == "feather":
        rib = _line(u * w, w * 0.5, max(0.6, 0.12 * D))
        barb = 0.5 + 0.5 * np.sin((v * h * 1.0 + np.abs(u - 0.5) * w * 1.3) * (1.5 / max(1.0, D / 4.0)))
        tone = 0.30 + 0.42 * barb + 0.18 * (1.0 - v) + (noise - 0.5) * 0.10
        tone = np.where(rib > 0.4, 0.92, tone)
        hgt = 0.45 + 0.18 * barb + 0.3 * rib
        taper = 1.0 - (v ** kw.get("taper_pow", 2.2)) * kw.get("taper", 0.0)
        if kw.get("taper", 0.0) > 0:
            alpha = (np.abs(u - 0.5) * 2 < taper).astype(np.float32)
        if mat.accent is not None:
            acc = np.maximum(smooth(v, 0.78, 0.95) * 0.7, rib * 0.5)
        if mat.has_glow:
            gl = np.maximum(rib * 0.9, (barb > 0.93) * 0.4) * mat.glow_amount * alpha
    elif kind == "cloth":
        fold = 0.5 + 0.5 * np.sin(u * w * (0.55 / max(1.0, D / 4.0)) + fbm(rng, w, h, 2, 3) * 3.0)
        weave = ((np.floor(u * w / max(1.0, D / 5.0)) + np.floor(v * h / max(1.0, D / 5.0))) % 2) * 0.05
        tone = 0.30 + 0.36 * fold + weave - 0.10 * v + (noise - 0.5) * 0.06
        hgt = 0.5 + 0.22 * fold + weave
        if kw.get("trim", 0) and mat.accent is not None:
            hem = _line(v * h, h - bw * 1.6, bw * 0.55)
            acc = np.maximum(acc, hem)
            side = _line(edge, bw * 0.9, bw * 0.4)
            acc = np.maximum(acc, side * 0.8)
            if mat.has_glow:
                gl = np.maximum(gl, hem * 0.8 * mat.glow_amount)
        if kw.get("ragged", 0):
            nn = fbm(rng, w, h, 3, 4)
            alpha = smooth(1.25 - v - nn * 0.55 + 0.35, 0.28, 0.42)
        if kw.get("pleats", 0):
            ph = np.mod(u * w / max(2.0, 1.4 * D), 1.0)
            hgt += 0.25 * np.sin(ph * math.pi)
            tone -= 0.14 * (1.0 - np.sin(ph * math.pi))
        if kw.get("stitch", 0):
            st = (np.mod(u * w / max(1.0, D * 0.6), 1.0) < 0.55) & (_line(edge, bw * 1.5, 0.45) > 0.5)
            tone = np.where(st, tone + 0.28, tone)
    elif kind == "leather":
        grain = fbm(rng, w, h, 4, 8)
        tone = 0.38 + 0.30 * grain + (noise - 0.5) * 0.08
        hgt = 0.4 + 0.3 * grain
        st = (np.mod(u * w / max(1.0, D * 0.55), 1.0) < 0.55) & (_line(edge, bw * 1.1, 0.45) > 0.5)
        tone = np.where(st, 0.8, tone)
        hgt = np.where(st, hgt + 0.2, hgt)
        hgt = hgt * (0.8 + 0.2 * smooth(edge, 0.0, bw))
    elif kind == "gem":
        cx, cy = w * 0.5, h * 0.5
        d = np.hypot((xx + 0.5 - cx) / (w * 0.5), (yy + 0.5 - cy) / (h * 0.5))
        facet = (np.floor(np.arctan2(yy + 0.5 - cy, xx + 0.5 - cx) / (math.pi / 3.0)) % 2)
        tone = np.clip(0.9 - d * 0.55 + (facet - 0.5) * 0.28, 0, 1)
        hgt = np.clip(1.0 - d, 0, 1)
        gl = np.clip(1.15 - d * 0.9, 0, 1) * mat.glow_amount if mat.has_glow else gl
    else:
        # the energy kinds (flame, bolt, wisp, crystal, water, mane): the 1.93.0 painters, returned as a finished image
        return None
    return tone, hgt, acc, gl, alpha, D, noise, edge, xx, yy, u, v


LIGHT = np.array([-0.52, -0.66, 0.54], np.float32)
LIGHT /= np.linalg.norm(LIGHT)


def paint(mat, w, h, face, tag, dims, decals=()):
    """One face: (rgba uint8 h x w x 4, glow rgba uint8 h x w x 4)."""
    w, h = max(2, int(w)), max(2, int(h))
    if mat.kind == "uvtest":                                              # a debug material: red grows to the right, green downward, a white grid every 10 texels
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        out = np.zeros((h, w, 4), np.uint8)
        out[..., 0] = (255 * (xx + 0.5) / w).astype(np.uint8)
        out[..., 1] = (255 * (yy + 0.5) / h).astype(np.uint8)
        out[..., 2] = 80
        out[..., 3] = 255
        grid = (np.mod(xx, 10) < 1) | (np.mod(yy, 10) < 1)
        out[grid, :3] = 255
        return out, np.zeros((h, w, 4), np.uint8)
    rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) * 31 + mat.seed * 7919) % (2 ** 32))
    pat = pattern(mat, w, h, face, tag, dims, rng)
    if pat is None:                                                       # an energy material: paint with the old kit, no relief
        a, g = WL.Mat(mat.kind, "%02X%02X%02X" % tuple(int(x) for x in mat.base), dark="%02X%02X%02X" % tuple(int(x) for x in mat.dark),
                      light="%02X%02X%02X" % tuple(int(x) for x in mat.light),
                      glow=("%02X%02X%02X" % tuple(int(x) for x in mat.glow)) if mat.glow is not None else None, glow_amount=mat.glow_amount, alpha=mat.alpha, seed=mat.seed,
                      **{k: v for k, v in mat.kw.items() if k in ("cells", "ragged", "grain", "inlay", "seam", "trim", "tip")}).paint(face, w, h, tag)
        a, g = _decals_energy(a, g, mat, w, h, decals, dims, face, tag)
        return np.clip(a, 0, 255).astype(np.uint8), np.clip(g, 0, 255).astype(np.uint8)
    tone, hgt, acc, gl, alpha, D, noise, edge, xx, yy, u, v = pat
    bw = min(max(1.2, 0.5 * D), max(1.0, 0.2 * min(w, h)))
    # ---------------------------------------------------------------- decals
    gem_tone = None
    for dc in decals:
        m = shape_mask(dc.shape, w, h, dc.size, dc.at[0], dc.at[1], dc.rot, dc.flip, dc.seed, dc.stroke) * dc.strength
        if m.max() <= 0:
            continue
        if dc.mode == "inlay":
            acc = np.maximum(acc, m)
            hgt = hgt + 0.18 * m
        elif dc.mode == "glow":
            hgt = hgt - 0.14 * m
            tone = tone - 0.20 * m
            gl = np.maximum(gl, m)
        elif dc.mode == "gem":
            dist = np.hypot((xx + 0.5 - w * dc.at[0]) / max(1.0, w * dc.size * 0.5), (yy + 0.5 - h * dc.at[1]) / max(1.0, h * dc.size * 0.5))
            fac = (np.floor(np.arctan2(yy + 0.5 - h * dc.at[1], xx + 0.5 - w * dc.at[0]) / (math.pi / 3.0)) % 2)
            gt = np.clip(0.95 - dist * 0.5 + (fac - 0.5) * 0.3, 0, 1)
            tone = np.where(m > 0.5, gt, tone)
            hgt = hgt + 0.25 * m
            gl = np.maximum(gl, m * np.clip(1.2 - dist * 0.8, 0.2, 1.0))
            gem_tone = m
        elif dc.mode == "dark":
            tone = tone - 0.32 * m
        elif dc.mode == "light":
            tone = tone + 0.30 * m
        elif dc.mode == "cut":
            hgt = hgt - 0.16 * m
            tone = tone - 0.22 * m
    # ---------------------------------------------------------------- light: a height field lit from the top left
    gx = np.gradient(hgt, axis=1)
    gy = np.gradient(hgt, axis=0)
    k = 1.7 * mat.shine * (D / 4.0) ** 0.4 * 3.0
    nx, ny, nz = -gx * k, -gy * k, np.ones_like(hgt)
    inv = 1.0 / np.sqrt(nx * nx + ny * ny + nz * nz)
    shade = (nx * LIGHT[0] + ny * LIGHT[1] + nz * LIGHT[2]) * inv
    shade0 = LIGHT[2]
    t = tone + (shade - shade0) * (0.85 + 0.9 * mat.shine)
    # ambient occlusion in the corners, a faint light on the rim
    ao = 1.0 - 0.20 * (1.0 - smooth(edge, 0.0, bw * 1.3))
    t = t * ao + 0.05 * (1.0 - smooth(edge, 0.0, 1.2)) * mat.shine
    if face == "up":
        t = t + 0.06
    elif face == "down":
        t = t - 0.08
    # ---------------------------------------------------------------- wear
    if mat.wear > 0 and mat.kind in ("plate", "steel", "gold") and min(w, h) >= 2.5 * D:
        wn = fbm(rng, w, h, 4, 5)
        worn = (wn > 0.70 - 0.20 * mat.wear) & (edge < bw * 1.6)
        t = np.where(worn, np.minimum(1.0, t + 0.30), t)
        sc = _scratches(rng, w, h, int(mat.wear * 5 * (w * h) ** 0.5 / 10) + 1, max(3.0, min(w, h) * 0.8))
        t = t + sc * 0.22 * mat.wear
        blot = fbm(rng, w, h, 2, 2)
        t = t - 0.10 * mat.wear * smooth(blot, 0.55, 0.9)
    t = np.clip(t, 0.0, 1.0)
    ramp = _ramp(t, mat.dark, mat.base, mat.light)
    if mat.accent is not None and acc.max() > 0:
        a_ramp = _ramp(t, mat.accent * 0.42, mat.accent, np.minimum(255.0, mat.accent * 1.28 + 30))
        ramp = ramp * (1.0 - acc[..., None]) + a_ramp * acc[..., None]
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = ramp
    out[..., 3] = np.clip(alpha, 0, 1) * mat.alpha
    g = np.zeros((h, w, 4), np.float32)
    if mat.has_glow:
        gl = np.clip(gl, 0.0, 1.0)
        core = np.clip((gl - 0.55) / 0.45, 0, 1) ** 1.5
        col = mat.glow[None, None, :] * (1.0 - 0.0 * core[..., None]) + (255.0 - mat.glow[None, None, :]) * (0.55 * core[..., None])
        g[..., :3] = col * gl[..., None]
        g[..., 3] = np.where(gl > 0.04, 255.0, 0.0)
    return np.clip(out, 0, 255).astype(np.uint8), np.clip(g, 0, 255).astype(np.uint8)


def _ramp(t, dark, base, light):
    lo = (t < 0.5)[..., None]
    a = dark + (base - dark) * (t * 2)[..., None]
    b = base + (light - base) * ((t - 0.5) * 2)[..., None]
    return np.where(lo, a, b)


def _decals_energy(a, g, mat, w, h, decals, dims, face, tag):
    """Decals on an energy material: bright marks only (the glow of the shape added to both maps)."""
    for dc in decals:
        m = shape_mask(dc.shape, w, h, dc.size, dc.at[0], dc.at[1], dc.rot, dc.flip, dc.seed, dc.stroke) * dc.strength
        if m.max() <= 0:
            continue
        col = mat.light if mat.glow is None else mat.glow
        a[..., :3] = a[..., :3] * (1 - m[..., None]) + col[None, None, :] * m[..., None]
        a[..., 3] = np.maximum(a[..., 3], m * mat.alpha)
        if mat.glow is not None:
            g[..., :3] = np.maximum(g[..., :3], mat.glow[None, None, :] * m[..., None])
            g[..., 3] = np.maximum(g[..., 3], np.where(m > 0.1, 255.0, 0.0))
    return a, g

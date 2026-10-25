"""Painters of the anime-cel Goliath (tools/gen_anime_goliath.py): a fixed 23-colour palette, faces drawn at SS x and reduced by the most common colour (never a blend, so no new colours
appear), cel bands (one light band, one shadow band), a one-texel painted outline in the hue-shifted dark of the local colour, moss that drips from the edges, hairline cracks, glowing rune
cracks and the cards (moss tufts, vines, stone spikes). Everything is deterministic: a face is drawn from (material, face name, size, seed).

    idx, glow = stone_face(Mat(fam="slate", moss=0.5), "top", 40, 24, seed)       # uint8 arrays (h, w) of palette indices (0 = clear)
    rgba = LUT[idx]                                                                 # (h, w, 4) uint8

Faces follow tools/zogratis_lucius_kit.Pack: i = 0 is the x-min edge of front / top / bottom, the x-max edge of back, the BACK edge of the right face and the FRONT edge of the left face;
j = 0 is the top edge of the side faces and the BACK edge of top / bottom.
"""
import math
import random
import sys
import zlib

import numpy as np
from PIL import Image, ImageDraw

sys.dont_write_bytecode = True

SS = 4

# ------------------------------------------------------------------------------------------------------------------ palette: 5 hue families x 3 tones + an outline tone each, void, amber
_DEF = [
    ("clear", (0, 0, 0, 0)),
    ("S0", (46, 38, 62)), ("S1", (108, 98, 126)), ("S2", (164, 156, 168)), ("S3", (222, 214, 206)),          # slate: shadow shifted to violet, light to cool white
    ("W0", (58, 42, 52)), ("W1", (108, 90, 94)), ("W2", (152, 134, 124)), ("W3", (204, 188, 156)),          # sandstone: warm, light shifted to yellow
    ("M0", (18, 50, 46)), ("M1", (44, 100, 68)), ("M2", (90, 154, 72)), ("M3", (160, 208, 96)),              # moss
    ("V0", (40, 26, 30)), ("V1", (88, 60, 52)), ("V2", (134, 96, 62)), ("V3", (182, 138, 88)),               # vine / root brown
    ("A0", (178, 58, 26)), ("A1", (252, 128, 38)), ("A2", (255, 198, 86)), ("A3", (255, 246, 200)),          # amber glow
    ("B0", (22, 36, 52)), ("B1", (44, 76, 92)), ("B2", (76, 120, 130)), ("B3", (140, 184, 178)),            # teal slate: the cool armour plates
    ("D0", (14, 10, 26)), ("D1", (32, 26, 52)), ("D2", (56, 48, 82)),                                       # void (the eye slot, the joints, the rune flank)
]
NAMES = [n for n, _ in _DEF]
I = {n: k for k, n in enumerate(NAMES)}
LUT = np.zeros((len(_DEF), 4), np.uint8)
for _k, (_n, _c) in enumerate(_DEF):
    LUT[_k] = (_c[0], _c[1], _c[2], _c[3] if len(_c) == 4 else 255)

# family = (outline, shadow, base, light)
FAM = {"slate": (I["S0"], I["S1"], I["S2"], I["S3"]), "sand": (I["W0"], I["W1"], I["W2"], I["W3"]), "basalt": (I["B0"], I["B1"], I["B2"], I["B3"]), "moss": (I["M0"], I["M1"], I["M2"], I["M3"]),
       "vine": (I["V0"], I["V1"], I["V2"], I["V3"]), "amber": (I["A0"], I["A0"], I["A1"], I["A2"]), "dark": (I["D0"], I["D0"], I["D1"], I["D2"])}
_N = len(_DEF)
OUT = np.arange(_N, dtype=np.uint8)           # any colour -> the outline tone of its family
DARKER = np.arange(_N, dtype=np.uint8)
LIGHTER = np.arange(_N, dtype=np.uint8)
for _f in ("slate", "sand", "basalt", "moss", "vine", "dark"):
    _o, _s, _b, _l = FAM[_f]
    ramp = [_o, _s, _b, _l] if _f != "dark" else [I["D0"], I["D1"], I["D2"]]
    for _t, _c in enumerate(ramp):
        OUT[_c] = ramp[0]
        DARKER[_c] = ramp[max(0, _t - 1)]
        LIGHTER[_c] = ramp[min(len(ramp) - 1, _t + 1)]
for _t, _c in enumerate((I["A0"], I["A1"], I["A2"], I["A3"])):
    OUT[_c] = I["D0"]
    DARKER[_c] = (I["A0"], I["A0"], I["A1"], I["A2"])[_t]
    LIGHTER[_c] = (I["A1"], I["A2"], I["A3"], I["A3"])[_t]
OUT[0] = DARKER[0] = LIGHTER[0] = 0
MOSS_SET = np.zeros(_N, bool)
MOSS_SET[[I["M0"], I["M1"], I["M2"], I["M3"]]] = True
AMBER_SET = np.zeros(_N, bool)
AMBER_SET[[I["A0"], I["A1"], I["A2"], I["A3"]]] = True

SIDEF = ("front", "back", "left", "right")


def clamp(v, lo, hi):
    return lo if v < lo else hi if v > hi else v


def seed_of(*parts):
    return zlib.crc32("|".join(str(p) for p in parts).encode()) & 0x7FFFFFFF


class Mat:
    """What a stone slab is made of and what is on it. All fields optional."""
    def __init__(self, fam="slate", **kw):
        self.fam = fam
        self.moss = 0.0            # share of the top face under moss (0..1)
        self.drip = 0.0            # texels of moss hanging over the upper edge of the side faces
        self.crack = 0.0           # 0..1: the chance of a hairline crack on a face
        self.seam = None           # "brick" | "slab" | None
        self.chip = 0.2           # chance of a chiselled corner facet
        self.ow = None             # outline width in texels (None: 2 on big faces, 1 on small)
        self.decals = []           # [{"face": "front", "kind": "sigil" | "runes", ...}]
        self.flat = False          # no light / shadow bands (a dark filler)
        self.moss_sides = ()       # side faces that get the drip (default: all sides when drip > 0)
        self.bandk = 1.0           # scale of both cel bands
        self.hot = 0.0             # chance of a glowing rune crack on a face
        self.panel = False         # an inset bevelled panel with four rivets on faces of 14 texels or more
        self.__dict__.update(kw)


# ------------------------------------------------------------------------------------------------------------------ drawing on a supersampled index canvas
class Sub:
    """A fw x fh texel picture of palette indices drawn at SS x with PIL (flat fills only), reduced by the most common colour."""
    def __init__(self, fw, fh, fill=0):
        self.fw, self.fh = fw, fh
        self.im = Image.new("P", (fw * SS, fh * SS), int(fill))
        self.d = ImageDraw.Draw(self.im)

    def _p(self, pts):
        return [(x * SS, y * SS) for x, y in pts]

    def rect(self, x0, y0, x1, y1, c):
        self.d.rectangle([x0 * SS, y0 * SS, x1 * SS - 1, y1 * SS - 1], fill=int(c))

    def poly(self, pts, c):
        self.d.polygon(self._p(pts), fill=int(c))

    def ell(self, cx, cy, rx, ry, c):
        self.d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS - 1, (cy + ry) * SS - 1], fill=int(c))

    def line(self, pts, c, w=1.0):
        self.d.line(self._p(pts), fill=int(c), width=max(1, int(round(w * SS))), joint="curve")

    def array(self):
        return np.array(self.im, dtype=np.uint8)

    def set_array(self, a):
        self.im = Image.fromarray(a, "P")
        self.d = ImageDraw.Draw(self.im)


def reduce_mode(a):
    """(h*SS, w*SS) -> (h, w): the most common index of every SS x SS block; a tie goes to the lower non-zero index, clear loses ties."""
    H, W = a.shape[0] // SS, a.shape[1] // SS
    b = a.reshape(H, SS, W, SS).transpose(0, 2, 1, 3).reshape(H, W, SS * SS)
    vals = np.unique(b)
    best = np.zeros((H, W), np.uint8)
    bc = np.full((H, W), -1, np.int16)
    order = [int(v) for v in vals if v != 0] + ([0] if 0 in vals else [])
    for k in order:
        c = (b == k).sum(axis=2).astype(np.int16)
        m = c > bc
        best[m] = k
        bc[m] = c[m]
    return best


def ragged(n, length, base, amp, rng):
    """n+1 points (x, y) along 0..length at depth base +- amp: the chiselled edge of a cel band."""
    return [(length * k / n, base + rng.uniform(-amp, amp)) for k in range(n + 1)]


def shift(m, dy, dx=0):
    """m moved by (dy, dx) with False filling in."""
    out = np.zeros_like(m)
    h, w = m.shape
    ys, yd = (slice(0, h - dy), slice(dy, h)) if dy >= 0 else (slice(-dy, h), slice(0, h + dy))
    xs, xd = (slice(0, w - dx), slice(dx, w)) if dx >= 0 else (slice(-dx, w), slice(0, w + dx))
    out[yd, xd] = m[ys, xs]
    return out


# ------------------------------------------------------------------------------------------------------------------ final-resolution helpers
def bres(x0, y0, x1, y1):
    x0, y0, x1, y1 = int(round(x0)), int(round(y0)), int(round(x1)), int(round(y1))
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    out = []
    while True:
        out.append((x0, y0))
        if x0 == x1 and y0 == y1:
            return out
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def path_pts(pts):
    out = []
    for k in range(len(pts) - 1):
        out += bres(pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1])
    return out


def inside(a, x, y):
    return 0 <= x < a.shape[1] and 0 <= y < a.shape[0]


def crack_path(rng, fw, fh, length):
    """A hairline crack: a walk from the top / side edge using only vertical, 45-degree and short horizontal steps (pixel-art friendly), one branch."""
    pts = []
    x, y = rng.randint(2, max(2, fw - 3)), 0
    side = rng.random() < 0.25
    if side:
        x, y = (0 if rng.random() < 0.5 else fw - 1), rng.randint(1, max(1, fh // 2))
    d = 1 if x < fw / 2 else -1
    pts.append((x, y))
    n = 0
    while n < length and y < fh - 1:
        r = rng.random()
        if r < 0.45:
            step = (0, rng.randint(1, 3))
        elif r < 0.8:
            step = (d * rng.randint(1, 2), rng.randint(1, 2))
        else:
            d = -d
            step = (d, 1)
        x += step[0]
        y += step[1]
        x = clamp(x, 0, fw - 1)
        pts.append((x, y))
        n += abs(step[0]) + abs(step[1])
    return pts


# ------------------------------------------------------------------------------------------------------------------ the stone face
def _moss_mask(face, fw, fh, mat, rng):
    """Boolean mask at SS x of where the moss lies: blobs on top faces, a scalloped edge with a few long drips hanging from the upper edge of the side faces."""
    im = Image.new("L", (fw * SS, fh * SS), 0)
    d = ImageDraw.Draw(im)

    def ell(cx, cy, rx, ry):
        d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=255)

    if face == "top" and mat.moss > 0:
        tries = 0
        while tries < 16:
            tries += 1
            r = rng.uniform(0.16, 0.34) * max(fw, fh) * (0.7 + mat.moss * 0.5)
            ell(rng.uniform(0, fw), rng.uniform(0, fh), r * 1.15, r * 0.8)
            if (np.array(im)[::SS * 2, ::SS * 2] > 0).mean() >= mat.moss * 0.92:
                break
    elif face in SIDEF and mat.drip > 0 and (not mat.moss_sides or face in mat.moss_sides) and fw >= 6 and fh >= 8:
        n = max(2, fw // 5)
        for k in range(n):
            cx = (k + rng.uniform(0.2, 0.8)) * fw / n
            depth = mat.drip * rng.uniform(0.45, 1.0)
            ell(cx, 0, rng.uniform(2.2, 4.2), depth)
        for _ in range(max(1, fw // 12)):                             # a long drip: a narrow tongue with a round end
            cx = rng.uniform(2, fw - 2)
            w = rng.uniform(1.0, 1.6)
            ln = mat.drip * rng.uniform(1.3, 2.0)
            d.rectangle([(cx - w) * SS, 0, (cx + w) * SS, (ln - w) * SS], fill=255)
            ell(cx, ln - w, w, w)
    return np.array(im) > 0


def stone_face(mat, face, fw, fh, seed):
    rng = random.Random(seed)
    o, sh, ba, lt = FAM[mat.fam]
    big = min(fw, fh) >= 12
    ow = mat.ow if mat.ow is not None else (2 if big else 1)
    sub = Sub(fw, fh, ba)
    lit = face in ("front", "left", "back")
    k = mat.bandk
    if mat.flat:
        pass
    elif face == "top":
        sub.rect(0, 0, fw, fh, lt)
        t = clamp(fw * 0.2, 1, 3.5) * k
        sub.poly([(fw, 0), (fw - t, 0)] + [(fw - t + rng.uniform(-0.6, 0.6), fh * q / 4) for q in range(1, 4)] + [(fw - t, fh), (fw, fh)], ba)
    elif face == "bottom":
        sub.rect(0, 0, fw, fh, sh)
    else:
        if not lit:
            sub.rect(0, 0, fw, fh, sh)
        tl = (clamp(0.2 * fh, 1.5, 5.0) if lit else clamp(0.14 * fh, 1.0, 3.0)) * k
        sub.poly([(0, 0), (fw, 0)] + list(reversed(ragged(max(2, fw // 4), fw, tl, 0.7, rng))), lt if lit else ba)
        tb = (clamp(0.3 * fh, 1.5, 8.0) if lit else clamp(0.34 * fh, 1.5, 9.0)) * k
        sub.poly([(0, fh), (fw, fh)] + list(reversed([(x, fh - y) for x, y in ragged(max(2, fw // 4), fw, tb, 0.8, rng)])), sh if lit else o)
        if lit and fw > 8:                                                    # a vertical shade band on the far edge keeps big faces from reading as flat
            t = clamp(fw * 0.12, 1, 3.5) * k
            sub.poly([(fw, 0), (fw - t, 0)] + [(fw - t + rng.uniform(-0.6, 0.6), fh * q / 4) for q in range(1, 4)] + [(fw - t, fh), (fw, fh)], sh)
    if not mat.flat and fw >= 7 and fh >= 7 and rng.random() < mat.chip:                 # a chiselled corner facet
        a, b = rng.uniform(2.0, min(5.0, fw / 3.0)), rng.uniform(2.0, min(5.0, fh / 3.0))
        top = rng.random() < 0.55
        left = rng.random() < 0.5
        x0, x1 = (0, a) if left else (fw, fw - a)
        y0, y1 = (0, b) if top else (fh, fh - b)
        tone = lt if top else sh
        if face in ("right", "back") and top:
            tone = ba
        sub.poly([(x0, y0), (x1, y0), (x0, y1)], tone)
    arr = sub.array()
    mmask = None
    if mat.moss > 0 or mat.drip > 0:
        m = _moss_mask(face, fw, fh, mat, rng)
        if m.any():
            kk = max(1, int(1.2 * SS))
            light = m & ~shift(m, kk)
            shade = m & ~shift(m, -kk)
            mo, ms, mb, ml = FAM["moss"]
            arr[m] = mb
            arr[light] = ml
            arr[shade & ~light] = ms
            mmask = m
    a = reduce_mode(arr)
    g = np.zeros((fh, fw), np.uint8)
    mossy = MOSS_SET[a]
    if mmask is not None:                                                      # outline the moss where it meets stone
        nb = np.zeros_like(mossy)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nb |= ~shift(mossy, dy, dx) & mossy
        edge = nb.copy()
        edge[0, :] = False if face in SIDEF else edge[0, :]
        a[edge] = FAM["moss"][0]
    # masonry seams and hairline cracks (final resolution, never over moss or glow)
    def put(x, y, fn):
        if inside(a, x, y) and not MOSS_SET[a[y, x]] and not AMBER_SET[a[y, x]]:
            a[y, x] = fn[a[y, x]]
    if mat.seam and face in SIDEF and fw >= 10 and fh >= 8:
        rh = int(clamp(round(fh / max(2, round(fh / 7.0))), 4, 9))
        bw = int(clamp(fw // 3, 7, 14))
        row = 0
        y = rh
        while y < fh - 2:
            for x in range(1, fw - 1):
                put(x, y, DARKER)
            off = (bw // 2) if (row & 1) else 0
            xx = off + bw
            while xx < fw - 1:
                for yy in range(y - rh + 1, y):
                    put(xx, yy, DARKER)
                xx += bw
            y += rh
            row += 1
    if face in SIDEF + ("top",) and fw >= 8 and fh >= 8 and rng.random() < mat.crack:
        for x, y in path_pts(crack_path(rng, fw, fh, rng.randint(max(3, fh // 2), max(4, int(fh * 0.9))))):
            put(x, y, OUT)
    if mat.panel and face in SIDEF and fw >= 14 and fh >= 14:
        m = 3 if min(fw, fh) < 24 else 4
        for x in range(m, fw - m):
            a[m, x] = LIGHTER[a[m, x]] if not MOSS_SET[a[m, x]] else a[m, x]
            a[fh - 1 - m, x] = DARKER[a[fh - 1 - m, x]] if not MOSS_SET[a[fh - 1 - m, x]] else a[fh - 1 - m, x]
        for y in range(m, fh - m):
            a[y, m] = LIGHTER[a[y, m]] if not MOSS_SET[a[y, m]] else a[y, m]
            a[y, fw - 1 - m] = DARKER[a[y, fw - 1 - m]] if not MOSS_SET[a[y, fw - 1 - m]] else a[y, fw - 1 - m]
        for (rx_, ry_) in ((m + 2, m + 2), (fw - m - 4, m + 2), (m + 2, fh - m - 4), (fw - m - 4, fh - m - 4)):
            if not MOSS_SET[a[ry_, rx_]]:
                a[ry_:ry_ + 2, rx_:rx_ + 2] = FAM[mat.fam][3]
                a[ry_ + 1, rx_:rx_ + 2] = FAM[mat.fam][1]
    # glowing rune cracks, shared by the decals (see _decal)
    for dc in mat.decals:
        if dc.get("face", face) == face or dc.get("faces") and face in dc["faces"]:
            _decal(a, g, dc, face, fw, fh, rng, mat)
    if mat.hot and face in SIDEF and fw >= 10 and fh >= 8 and rng.random() < mat.hot:
        pts = rune_path(rng, fw, fh)
        glow_line(a, g, pts)
    # the painted outline: the dark of the local colour on the face's border
    ring = np.zeros_like(mossy)
    ring[:ow, :] = True
    ring[-ow:, :] = True
    ring[:, :ow] = True
    ring[:, -ow:] = True
    sel = ring & (a != 0)
    keep = AMBER_SET[a]
    a[sel & ~keep] = OUT[a[sel & ~keep]]
    return a, g


def rune_path(rng, fw, fh, vertical=True, length=None):
    """A zig-zag rune crack of horizontal / vertical / 45-degree runs inside the face, as a list of texel points."""
    x, y = rng.randint(3, max(3, fw - 4)), rng.randint(2, max(2, fh // 3))
    pts = [(x, y)]
    n = rng.randint(3, 5) if length is None else length
    d = rng.choice((-1, 1))
    for _ in range(n):
        r = rng.random()
        if r < 0.5:
            y += rng.randint(2, max(2, fh // 5))
        elif r < 0.85:
            ln = rng.randint(2, 4)
            x += d * ln
            y += ln
        else:
            d = -d
            x += d * rng.randint(2, 4)
        x = clamp(x, 2, fw - 3)
        y = clamp(y, 1, fh - 2)
        pts.append((x, y))
    return pts


def glow_line(a, g, pts, core=I["A2"], mid=I["A1"], flank=I["D0"], halo=True):
    """A glowing crack: dark flank, amber core, the glow layer gets the core and a dim halo."""
    cp = set(path_pts(pts))
    fl = set()
    for (x, y) in cp:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            q = (x + dx, y + dy)
            if q not in cp:
                fl.add(q)
    for (x, y) in fl:
        if inside(a, x, y):
            a[y, x] = flank
            if halo:
                g[y, x] = I["A0"]
    for n, (x, y) in enumerate(sorted(cp, key=lambda p: (p[1], p[0]))):
        if inside(a, x, y):
            a[y, x] = core if n % 5 else I["A3"]
            g[y, x] = core if n % 5 else I["A3"]
            if n % 3 == 1:
                a[y, x] = mid
                g[y, x] = mid


def _decal(a, g, dc, face, fw, fh, rng, mat):
    kind = dc["kind"]
    if kind == "runes":                                                       # explicit polylines in 0..1 face coordinates
        for line in dc["lines"]:
            glow_line(a, g, [(u * (fw - 1), v * (fh - 1)) for u, v in line])
    elif kind == "sigil":
        sigil(a, g, dc.get("cx", 0.5) * fw, dc.get("cy", 0.5) * fh, dc["r"], mat)
    elif kind == "mouth":                                                     # a dark grille mouth: a slot with a dim amber line and tooth seams
        x0, x1 = int(dc.get("x0", 0.18) * fw), int(dc.get("x1", 0.82) * fw)
        y0, y1 = int(dc.get("y0", 0.3) * fh), int(dc.get("y1", 0.78) * fh)
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if inside(a, x, y):
                    a[y, x] = I["D0"]
        ym = (y0 + y1) // 2
        for x in range(x0 + 1, x1):
            if inside(a, x, ym):
                a[ym, x] = I["A1"] if (x - x0) % 6 else I["D0"]
                g[ym, x] = I["A0"] if (x - x0) % 6 else 0
        for x in range(x0 + 3, x1 - 2, 6):
            for y in range(y0, y1 + 1):
                if inside(a, x, y) and y != ym:
                    a[y, x] = I["D1"]
    elif kind == "bar":                                                       # a lit slot (a light seam in an armour joint)
        x0, y0, x1, y1 = dc["box"]
        for y in range(int(y0 * fh), int(y1 * fh) + 1):
            for x in range(int(x0 * fw), int(x1 * fw) + 1):
                if inside(a, x, y):
                    a[y, x] = I["A2"]
                    g[y, x] = I["A2"]


def sigil(a, g, cx, cy, R, mat):
    """The chest sigil: an amber ring round a dark void with a four-point star, eight rune ticks round the ring. Drawn at SS x on the colour and the glow layers."""
    h, w = a.shape
    sc, sg = Sub(w, h, 0), Sub(w, h, 0)
    cur = a.copy()
    base = np.array(Image.fromarray(cur, "P").resize((w * SS, h * SS), Image.NEAREST), dtype=np.uint8)
    sc.set_array(base)
    D0, D1, A1, A2, A3 = I["D0"], I["D1"], I["A1"], I["A2"], I["A3"]
    sc.ell(cx, cy, R + 1.6, R + 1.6, D0)
    sc.ell(cx, cy, R + 0.6, R + 0.6, A2)
    sc.ell(cx, cy, R - 1.0, R - 1.0, D1)
    sg.ell(cx, cy, R + 0.6, R + 0.6, A2)
    sg.ell(cx, cy, R - 1.0, R - 1.0, 0)
    for k in range(8):                                                        # ticks
        th = math.pi / 4 * k + math.pi / 8
        r0, r1 = R + 2.4, R + 4.4
        pa = (cx + r0 * math.cos(th), cy + r0 * math.sin(th))
        pb = (cx + r1 * math.cos(th), cy + r1 * math.sin(th))
        sc.line([pa, pb], D0, 2.6)
        sc.line([pa, pb], A1, 1.0)
        sg.line([pa, pb], A1, 1.2)
    r = R - 2.2                                                               # four-point star
    star = []
    for k in range(8):
        rr = r if k % 2 == 0 else r * 0.34
        th = -math.pi / 2 + math.pi / 4 * k
        star.append((cx + rr * math.cos(th), cy + rr * math.sin(th)))
    sc.poly(star, A2)
    sg.poly(star, A2)
    sc.ell(cx, cy, 1.2, 1.2, A3)
    sg.ell(cx, cy, 1.2, 1.2, A3)
    a[:] = np.where(reduce_mode(sc.array()) != 0, reduce_mode(sc.array()), a)
    gg = reduce_mode(sg.array())
    g[:] = np.where(gg != 0, gg, g)


# ------------------------------------------------------------------------------------------------------------------ the face of the eye slot
def eye_slot_face(fw, fh, seed):
    """Front of the dark slot under the brow: void with a lit slit made of two tilted wedges meeting low in the middle (a stern V), three amber tones, a pale core line; glow = the slit."""
    sub = Sub(fw, fh, I["D0"])
    sg = Sub(fw, fh, 0)
    A1, A2, A3 = I["A1"], I["A2"], I["A3"]
    cy = fh * 0.56
    for sgn in (-1, 1):
        x_in, x_out = fw / 2 + sgn * fw * 0.07, fw / 2 + sgn * fw * 0.43
        yi, yo = cy + fh * 0.12, cy - fh * 0.16
        th = fh * 0.30
        poly = [(x_in, yi - th * 0.8), (x_out, yo - th), (x_out + sgn * 0.5, yo + th * 0.25), (x_in, yi + th * 0.5)]
        sub.poly(poly, A1)
        sg.poly(poly, A1)
        core = [(x_in, yi - th * 0.35), (x_out - sgn * fw * 0.04, yo - th * 0.45), (x_out - sgn * fw * 0.04, yo + th * 0.0), (x_in, yi + th * 0.15)]
        sub.poly(core, A2)
        sg.poly(core, A2)
        mid = [(x_in + sgn * fw * 0.04, yi - th * 0.1), (x_out - sgn * fw * 0.1, yo - th * 0.25), (x_out - sgn * fw * 0.1, yo - th * 0.05), (x_in + sgn * fw * 0.04, yi + th * 0.05)]
        sub.poly(mid, A3)
        sg.poly(mid, A3)
    a = reduce_mode(sub.array())
    g = reduce_mode(sg.array())
    a[0, :] = I["D1"]                                                           # the lip of the brow casts a lighter bevel row below it
    ring = np.zeros(a.shape, bool)
    ring[-1, :] = True
    ring[:, 0] = True
    ring[:, -1] = True
    a[ring & ~AMBER_SET[a]] = I["D0"]
    return a, g


# ------------------------------------------------------------------------------------------------------------------ cards (alpha cut flat quads)
def _finish_card(a, g=None, ow=1):
    """Outline every opaque texel that touches a clear one (or the card border) in the dark of its colour."""
    opaque = a != 0
    edge = np.zeros_like(opaque)
    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        edge |= opaque & ~shift(opaque, dy, dx)
    h, w = a.shape
    edge[0, :] |= opaque[0, :]
    edge[-1, :] |= opaque[-1, :]
    edge[:, 0] |= opaque[:, 0]
    edge[:, -1] |= opaque[:, -1]
    a[edge & ~AMBER_SET[a]] = OUT[a[edge & ~AMBER_SET[a]]]
    if g is None:
        g = np.zeros_like(a)
    return a, g


def tuft_card(fw, fh, seed, fam="moss", leaves=None):
    """A moss tuft hanging from its top edge: a scalloped band that ends in 3 to 5 pointed leaves, a light band along the top, a shadow band on the lower half of every leaf."""
    rng = random.Random(seed)
    o, sh, ba, lt = FAM[fam]
    n = leaves or clamp(fw // 5, 2, 6)
    sub = Sub(fw, fh, 0)
    top = fh * 0.30
    pts = [(0, 0), (fw, 0), (fw, top * rng.uniform(0.8, 1.2))]
    for k in range(n - 1, -1, -1):
        cx = (k + 0.5) * fw / n
        ln = fh * rng.uniform(0.62, 1.0)
        pts += [(cx + fw / n * 0.5, top * rng.uniform(0.9, 1.1)), (cx, ln)]
    pts += [(0, top)]
    # valleys sit between the leaf tips: build explicitly for a clean zig-zag
    pts = [(0, 0), (fw, 0)]
    for k in range(n - 1, -1, -1):
        cx = (k + 0.5) * fw / n
        ln = fh * rng.uniform(0.6, 1.0)
        pts += [((k + 1) * fw / n, top * rng.uniform(0.85, 1.1)), (cx + fw / n * 0.18, ln * 0.72), (cx, ln)]
    pts += [(0, top * 0.9)]
    sub.poly(pts, ba)
    mask = sub.array() != 0
    arr = sub.array()
    kk = max(1, int(1.1 * SS))
    light = mask & ~shift(mask, kk)
    arr[light] = lt
    # shadow: a lower diagonal band over the leaves (right half of each leaf)
    for k in range(n):
        cx = (k + 0.5) * fw / n
        sub.set_array(arr)
        sub.poly([(cx, top * 0.7), (cx + fw / n * 0.6, top * 0.7), (cx + fw / n * 0.25, fh), (cx, fh)], sh)
        sub.set_array(np.where(mask, sub.array(), 0).astype(np.uint8))
        arr = sub.array()
    arr[light] = lt
    a = reduce_mode(arr)
    return _finish_card(a)


def vine_card(fw, fh, seed):
    """A hanging vine: a wavy brown stem with paired moss leaves, long enough to swing."""
    rng = random.Random(seed)
    sub = Sub(fw, fh, 0)
    sx = lambda y: fw / 2 + math.sin(y * 0.55 + seed) * fw * 0.12
    stem = [(sx(y), y) for y in np.linspace(0, fh, max(8, int(fh)))]
    sub.line(stem, I["V1"], 1.6)
    sub.line([(x - 0.5, y) for x, y in stem], I["V2"], 0.9)
    y = fh * 0.12
    side = 1
    while y < fh - 3:
        x = sx(y)
        ln = rng.uniform(0.28, 0.42) * fw * 1.3
        w = ln * 0.5
        tip = (x + side * ln, y + ln * 0.5)
        leaf = [(x, y), (x + side * ln * 0.5, y - w * 0.55), tip, (x + side * ln * 0.45, y + w * 0.7)]
        sub.poly(leaf, I["M2"])
        sub.poly([(x, y), tip, (x + side * ln * 0.45, y + w * 0.7)], I["M1"])
        sub.line([(x, y), (x + side * ln * 0.7, y + ln * 0.2)], I["M3"], 0.5)
        side = -side
        y += rng.uniform(3.0, 5.0)
    a = reduce_mode(sub.array())
    return _finish_card(a)


def spike_card(fw, fh, seed, fam="slate", curve=0.0):
    """A stone spike (a crest horn, a pauldron thorn): a tall triangle with an optional lean, lit on its left half, shadowed on its right, a light edge, outlined."""
    o, sh, ba, lt = FAM[fam]
    sub = Sub(fw, fh, 0)
    tip = fw / 2 + curve * fw
    tri = [(0, fh), (fw, fh), (tip + fw * 0.05, 0)]
    sub.poly(tri, ba)
    sub.poly([((fw / 2 + tip) / 2 + 0.2, fh * 0.5), (fw, fh), (fw / 2 + 0.5, fh)], sh)
    sub.poly([(tip + fw * 0.05, 0), ((fw / 2 + tip) / 2 + 0.2, fh * 0.5), (fw / 2 + 0.5, fh), (tip, fh * 0.3)], sh)
    sub.poly([(0, fh), (fw * 0.18, fh), (tip - fw * 0.06, fh * 0.12)], lt)
    a = reduce_mode(sub.array())
    mask = np.array(Image.fromarray(a, "P")) != 0
    a[~mask] = 0
    return _finish_card(a)


def leaf_hang(fw, fh, seed):
    """A short drooping ruffle of leaves (a collar of moss) : two rows of overlapping rounded leaves."""
    rng = random.Random(seed)
    sub = Sub(fw, fh, 0)
    n = max(3, fw // 4)
    for row, (cy, col, shd) in enumerate(((fh * 0.38, I["M1"], I["M0"]), (fh * 0.2, I["M2"], I["M1"]))):
        for k in range(n):
            cx = (k + 0.5 + (0.25 if row else -0.25)) * fw / n
            ry = fh * rng.uniform(0.28, 0.42)
            sub.ell(cx, cy + ry * 0.5, fw / n * 0.62, ry, col)
            sub.ell(cx + 0.4, cy + ry * 0.7, fw / n * 0.4, ry * 0.7, shd) if False else None
    arr = sub.array()
    mask = arr != 0
    kk = max(1, int(1.0 * SS))
    light = mask & ~shift(mask, kk) & (arr == I["M2"])
    arr[light] = I["M3"]
    a = reduce_mode(arr)
    return _finish_card(a)


# ------------------------------------------------------------------------------------------------------------------ painters handed to Pack.box
class Painter:
    """paint(face, i, j, fw, fh) for tools/zogratis_lucius_kit.Pack: the face is drawn once (cached) by `fn(face, fw, fh, seed) -> (index array, glow array)`."""
    def __init__(self, fn, seed):
        self.fn, self.seed = fn, seed
        self.cache = {}

    def arrays(self, face, fw, fh):
        k = (face, fw, fh)
        if k not in self.cache:
            self.cache[k] = self.fn(face, fw, fh, seed_of(self.seed, face, fw, fh))
        return self.cache[k]

    def __call__(self, face, i, j, fw, fh):
        a, g = self.arrays(face, fw, fh)
        c = a[j, i]
        gv = g[j, i]
        return (tuple(int(v) for v in LUT[c]) if c else None, tuple(int(v) for v in LUT[gv]) if gv else None)


def stone(mat, seed):
    return Painter(lambda face, fw, fh, sd: stone_face(mat, face, fw, fh, sd), seed)


def card(fn, seed, **kw):
    """A card painter: the same picture on the front and the back face (one of them is usually asked for)."""
    def f(face, fw, fh, sd):
        a, g = fn(fw, fh, sd, **kw)
        return a, g
    return Painter(f, seed)


def eye_painter(seed):
    def f(face, fw, fh, sd):
        if face == "front":
            return eye_slot_face(fw, fh, sd)
        return stone_face(Mat("dark", flat=True, chip=0), face, fw, fh, sd)
    return Painter(f, seed)

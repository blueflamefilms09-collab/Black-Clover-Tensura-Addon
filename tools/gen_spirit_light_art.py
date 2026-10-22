"""The hand-tuned, cel-shaded skins of the Spirit of Light (docs/magic/spirit_light.md): the golden lion cub and the seraph, and the soft textures of the beam, the ring, the glow and the spark.
Every painter is deterministic (seeded from its own name and options) and draws with thin dark linework: golds and whites, three or four hard tone steps (shade, base, light, highlight) instead of
smooth gradients, short fur strokes, feather barbs, pleats of light. Glow is ONLY ever painted into the separate additive sheets (kinds that start with `fx_`): soft radial or ring falloff on a
fully transparent ground, nothing under a transparent texel and no flat white cut-outs (a peak is a tinted pale gold, never a solid white shape). Used by tools/gen_spirit_light_geo.py;
`python3 -B tools/gen_spirit_light_art.py` writes the four particle textures (textures/particle/spiritlight_{beam,ring,glow,spark}.png).
"""
import hashlib
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spirit_light_lib as L  # noqa: E402

SS = 3                                                    # supersampling of the drawn shapes


def C(h, a=255):
    c = L.rgb(h)
    return int(c[0]), int(c[1]), int(c[2]), int(a)


def mixc(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(4))


# ================================================================================================================== palettes
CUB = dict(base="#EDB64C", hi="#FFDF8C", hi2="#FFF4C8", lo="#CB8A2E", deep="#9A5C1C", line="#4A260B")
CREAM = dict(base="#FFEFC4", hi="#FFFAE2", hi2="#FFFFFF", lo="#F0CD8C", deep="#D4A25A", line="#5A3412")
MANE = dict(base="#E2862A", hi="#FFB445", hi2="#FFD982", lo="#B35F1B", deep="#7C3C10", line="#4A260B")
TUFT = dict(base="#B8661C", hi="#E48A2C", hi2="#FFB445", lo="#843F11", deep="#4E230A", line="#33160A")
PAW = dict(base="#F6CC70", hi="#FFE9A8", hi2="#FFF8DC", lo="#D79A3A", deep="#A5621E", line="#4A260B")
ROBE = dict(base="#FFF5D8", hi="#FFFDF0", hi2="#FFFFFF", lo="#EDD497", deep="#C9A05A", line="#6A4814")
GOLDW = dict(base="#F4C95E", hi="#FFE699", hi2="#FFF6CF", lo="#CC9238", deep="#8E5C1E", line="#5A3A10")
FEATHER = dict(base="#FFF3D0", hi="#FFFCEC", hi2="#FFFFFF", lo="#EFD38A", deep="#D4A94E", line="#6E4A14")
CRYSTAL = dict(base="#FFE9A6", hi="#FFFBE2", hi2="#FFFFFF", lo="#EBBA55", deep="#B8802A", line="#6A4814")
PALS = {"cub": CUB, "cream": CREAM, "mane": MANE, "tuft": TUFT, "paw": PAW, "robe": ROBE, "gold": GOLDW, "feather": FEATHER, "crystal": CRYSTAL}


def pal(name):
    p = PALS[name]
    return {k: C(v) for k, v in p.items()}


# ================================================================================================================== canvas
class Cv:
    """A supersampled RGBA canvas in FINAL pixel coordinates (floats welcome): the shapes are drawn at SS times the size and averaged down. A colour with alpha under 255 is composited over what is
    there (the strokes of fur are not holes)."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.img = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
        self.dr = ImageDraw.Draw(self.img)

    def _put(self, fn, col):
        if col[3] >= 255:
            fn(self.dr, col)
            return
        layer = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
        fn(ImageDraw.Draw(layer), col)
        self.img = Image.alpha_composite(self.img, layer)
        self.dr = ImageDraw.Draw(self.img)

    def rect(self, x0, y0, x1, y1, col):
        self._put(lambda d, c: d.rectangle([x0 * SS, y0 * SS, x1 * SS - 1, y1 * SS - 1], fill=c), col)

    def poly(self, pts, col):
        self._put(lambda d, c: d.polygon([(x * SS, y * SS) for x, y in pts], fill=c), col)

    def ell(self, cx, cy, rx, ry, col):
        self._put(lambda d, c: d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=c), col)

    def line(self, pts, col, width=1.0):
        wd = max(1, int(round(width * SS)))
        self._put(lambda d, c: d.line([(x * SS, y * SS) for x, y in pts], fill=c, width=wd, joint="curve"), col)

    def arc(self, cx, cy, rx, ry, a0, a1, col, width=1.0):
        wd = max(1, int(round(width * SS)))
        self._put(lambda d, c: d.arc([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], a0, a1, fill=c, width=wd), col)

    def done(self):
        return np.asarray(self.img.resize((self.w, self.h), Image.LANCZOS), dtype=np.float32)


def rng_for(kind, face, opts):
    keyed = sorted((k, repr(v)) for k, v in opts.items() if not k.startswith("_"))
    h = hashlib.sha1((kind + "|" + face + "|" + repr(keyed) + "|" + repr(opts.get("_size"))).encode()).digest()
    return np.random.default_rng(int.from_bytes(h[:8], "little"))


def lw(opts, k=0.28, lo=1.0):
    """The width in final pixels of a thin dark line: about a quarter of a model unit, never under one pixel."""
    return max(lo, k * opts.get("_d", 4.0))


def outline(cv, p, opts, edges="tblr", alpha=0.85, k=0.28):
    w, h = cv.w, cv.h
    t = lw(opts, k)
    col = (p["line"][0], p["line"][1], p["line"][2], int(255 * alpha))
    if "t" in edges:
        cv.rect(0, 0, w, t, col)
    if "b" in edges:
        cv.rect(0, h - t, w, h, col)
    if "l" in edges:
        cv.rect(0, 0, t, h, col)
    if "r" in edges:
        cv.rect(w - t, 0, w, h, col)


def wavy(rng, n, amp):
    """n+1 random offsets in -amp..amp, smoothed once: the edge of a tone band that is not a ruler line."""
    v = rng.uniform(-amp, amp, n + 1)
    return (v + np.roll(v, 1)) * 0.5


# ================================================================================================================== fur and cloth
def fur_canvas(w, h, face, opts):
    """A face of fur in four hard tones: base, a light band on the top (the whole face for an upward face), a shade band at the bottom (the whole face for a downward face), short strokes of fur in the
    shade and in the light, an optional tufted lower edge (a row of points; the texel under a point is transparent, so the silhouette is furry), and thin dark edges. Returns the canvas."""
    p = pal(opts.get("pal", "cub"))
    rng = rng_for("fur", face, opts)
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p["base"])
    n = 14
    xs = np.linspace(0, w, n + 1)
    if face == "up":
        cv.ell(w * 0.48, h * 0.46, w * 0.40, h * 0.38, p["hi"])
        cv.ell(w * 0.40, h * 0.36, w * 0.18, h * 0.16, p["hi2"])
    elif face == "down":
        cv.rect(0, 0, w, h, p["lo"])
        cv.ell(w * 0.5, h * 0.5, w * 0.30, h * 0.3, p["deep"])
    else:
        wv = wavy(rng, n, h * 0.05)
        cv.poly([(xs[i], h * 0.19 + wv[i]) for i in range(n + 1)] + [(w, 0), (0, 0)], p["hi"])
        cv.poly([(xs[i], h * 0.10 + wv[i] * 0.6) for i in range(n + 1)] + [(w, 0), (0, 0)], p["hi2"])
        wv2 = wavy(rng, n, h * 0.05)
        cv.poly([(xs[i], h * 0.80 + wv2[i]) for i in range(n + 1)] + [(w, h), (0, h)], p["lo"])
        cv.poly([(xs[i], h * 0.92 + wv2[i] * 0.5) for i in range(n + 1)] + [(w, h), (0, h)], p["deep"])
        if face in ("west", "east"):                                                    # the sides are a little darker toward the rear
            cv.rect(w * (0.78 if face == "east" else 0.0), 0, w * (1.0 if face == "east" else 0.22), h, (p["lo"][0], p["lo"][1], p["lo"][2], 70))
    # fur: rows of small chevrons (a short "v" of two strokes), staggered, in the shade tone, with a light stroke on the lit side of some: reads as short fur without long wooden grain
    d = opts.get("_d", 4.0)
    cell = max(3.0, d * 1.25)
    dens = float(opts.get("strands", 1.0))
    rows = int(h / cell) + 1
    for r in range(rows):
        y = (r + 0.5) * cell
        off = (r % 2) * cell * 0.5 + rng.uniform(-0.2, 0.2) * cell
        cols = int(w / cell) + 2
        for c in range(cols):
            if rng.random() > dens * 0.75:
                continue
            x = c * cell + off + rng.uniform(-0.25, 0.25) * cell
            if x < 1 or x > w - 1:
                continue
            jy = y + rng.uniform(-0.2, 0.2) * cell
            col = p["lo"] if rng.random() < 0.7 else p["hi"]
            arm = cell * 0.42
            cv.line([(x - arm, jy - arm * 0.9), (x, jy + arm * 0.4), (x + arm, jy - arm * 0.9)], (col[0], col[1], col[2], 170), lw(opts, 0.20, 1.0))
    tuft = opts.get("tuft")
    edges = opts.get("edges", "tblr")
    if tuft:
        top_ = opts.get("tuft_edge", "bottom") == "top"
        outline(cv, p, opts, edges.replace("t" if top_ else "b", ""), opts.get("line", 0.75))                 # the straight edges first: the mask below cuts the furry edge out of them
        edges = ""                                                                           # a row of points along the bottom (or the top) edge: the cut-out silhouette of fur
        top = opts.get("tuft_edge", "bottom") == "top"
        k = max(2, int(opts.get("points", max(3, round(w / (d * 1.7))))))
        depth = h * float(tuft)
        zig = [(w, h - depth)]
        for i in reversed(range(k)):
            x0, x1 = w * i / k, w * (i + 1) / k
            tipx = (x0 + x1) * 0.5 + rng.uniform(-0.18, 0.18) * (x1 - x0)
            tipy = h - depth * rng.uniform(0.0, 0.45)
            zig += [(x1, h - depth), (tipx, tipy), (x0, h - depth)]
        if top:
            zig = [(x, h - y) for x, y in zig]
        mask = Image.new("L", cv.img.size, 0)
        base_poly = [(0, h), (w, h)] if top else [(0, 0), (w, 0)]
        ImageDraw.Draw(mask).polygon([(x * SS, y * SS) for x, y in base_poly] + [(x * SS, y * SS) for x, y in zig], fill=255)
        a = np.minimum(np.asarray(cv.img.getchannel("A")), np.asarray(mask))
        cv.img.putalpha(Image.fromarray(a.astype(np.uint8)))
        cv.dr = ImageDraw.Draw(cv.img)
        cv.line(zig[1:], (p["line"][0], p["line"][1], p["line"][2], int(255 * opts.get("line", 0.75))), lw(opts, 0.26))
    outline(cv, p, opts, edges, opts.get("line", 0.75))
    return cv


def fur(w, h, face, opts):
    return fur_canvas(w, h, face, opts).done()


def cloth(w, h, face, opts):
    """Robe and sash: white-gold cloth in vertical pleats (three tone steps with soft wavy edges), a gold hem line, thin dark gold edges."""
    p = pal(opts.get("pal", "robe"))
    g = pal("gold")
    rng = rng_for("cloth", face, opts)
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p["base"])
    if face == "up" or face == "down":
        cv.ell(w / 2, h / 2, w * 0.38, h * 0.38, p["hi"])
        outline(cv, g, opts, "tblr", 0.7)
        return cv.done()
    pleats = int(opts.get("pleats", max(2, round(w / (opts.get("_d", 3.0) * 2.4)))))
    pw = w / pleats
    for i in range(pleats):
        x0, x1 = i * pw, (i + 1) * pw
        # each pleat: a lit side (hi), the fold's shade (lo) and a thin deep crease between pleats
        cv.rect(x0 + pw * 0.10, 0, x0 + pw * 0.45, h, p["hi"])
        cv.rect(x0 + pw * 0.16, 0, x0 + pw * 0.30, h, p["hi2"])
        cv.rect(x0 + pw * 0.62, 0, x1, h, p["lo"])
        cv.rect(x1 - pw * 0.12, 0, x1, h, p["deep"])
    if face in ("west", "east"):
        cv.rect(0, 0, w, h, (p["lo"][0], p["lo"][1], p["lo"][2], 60))
    trim = float(opts.get("trim", 0.0))
    if trim > 0:
        t = max(2.0, h * trim)
        cv.rect(0, h - t, w, h, g["base"])
        cv.rect(0, h - t, w, h - t * 0.62, g["hi"])
        cv.rect(0, h - t * 0.22, w, h, g["lo"])
        # a chain of small diamonds on the trim
        n = max(2, int(w / (t * 1.3)))
        for i in range(n):
            cx = (i + 0.5) * w / n
            s = t * 0.28
            cv.poly([(cx, h - t * 0.5 - s), (cx + s, h - t * 0.5), (cx, h - t * 0.5 + s), (cx - s, h - t * 0.5)], g["deep"])
        cv.rect(0, h - t - lw(opts, 0.2), w, h - t + lw(opts, 0.2), (g["line"][0], g["line"][1], g["line"][2], 200))
    toptrim = float(opts.get("toptrim", 0.0))
    if toptrim > 0:
        t = max(2.0, h * toptrim)
        cv.rect(0, 0, w, t, g["base"])
        cv.rect(0, 0, w, t * 0.45, g["hi"])
    if opts.get("glyphs"):                                                              # a column of small gold diamonds down the middle: light stitched into the cloth
        n = int(opts["glyphs"])
        for i in range(n):
            cy = (i + 0.5) * h / n
            s = min(w * 0.22, h / n * 0.28)
            cv.poly([(w / 2, cy - s * 1.3), (w / 2 + s, cy), (w / 2, cy + s * 1.3), (w / 2 - s, cy)], g["base"])
            cv.poly([(w / 2, cy - s * 0.7), (w / 2 + s * 0.5, cy), (w / 2, cy + s * 0.7), (w / 2 - s * 0.5, cy)], g["hi2"])
    outline(cv, g, opts, opts.get("edges", "tblr"), 0.8)
    return cv.done()


def plain(w, h, face, opts):
    """A flat tone with thin edges: the backs and undersides nobody reads."""
    p = pal(opts.get("pal", "cub"))
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p[opts.get("tone", "base")])
    outline(cv, p, opts, opts.get("edges", "tblr"), 0.7)
    return cv.done()


# ================================================================================================================== the cub's face
def almond(cv, cx, cy, rx, ry, col):
    pts = []
    for i in range(0, 41):
        t = i / 40.0
        x = cx + (t * 2 - 1) * rx
        y = cy - math.sin(t * math.pi) * ry
        pts.append((x, y))
    for i in range(40, -1, -1):
        t = i / 40.0
        x = cx + (t * 2 - 1) * rx
        y = cy + math.sin(t * math.pi) * ry * 0.85
        pts.append((x, y))
    cv.poly(pts, col)


EYE_UNITS = (4.8, 5.8, 2.7, 2.9)        # head front face of 22 x 19 units: x of the left eye, y, half width, half height (the right eye mirrors it)


def cub_eye(cv, mood, ex, ey, rx, ry, side, opts):
    """One eye of the cub in a mood: happy (round, bright, two catch lights), sleepy (closed, a heavy curve with lashes), fierce (narrowed, slanted, a thin slit pupil), surprised (huge, a tiny pupil, a white ring)."""
    ln = C("#4A260B")
    if mood == "sleepy":
        cv.ell(ex, ey + ry * 0.05, rx * 1.05, ry * 0.60, C("#F3C260"))                              # the lid, a little lighter than the fur
        cv.arc(ex, ey - ry * 0.40, rx * 1.08, ry * 1.0, 20, 160, ln, lw(opts, 0.62))               # the lash line: a curve that bows downward
        for k in (-1, 0, 1):
            x0 = ex + k * rx * 0.62
            cv.line([(x0, ey + ry * 0.52), (x0 + side * 0.25 * rx, ey + ry * 0.92)], ln, lw(opts, 0.26))
        return
    if mood == "fierce":
        sl = 1.0 if side < 0 else -1.0                                                                 # the inner corner sits low and toward the nose
        top = [(ex - rx * sl * 1.05, ey - ry * 0.80), (ex, ey - ry * 0.52), (ex + rx * sl * 1.05, ey + ry * 0.05)]
        bot = [(ex + rx * sl * 1.05, ey + ry * 0.05), (ex + rx * sl * 0.2, ey + ry * 0.70), (ex - rx * sl * 0.9, ey + ry * 0.45), (ex - rx * sl * 1.05, ey - ry * 0.80)]
        cv.poly(top + bot, ln)
        s2 = 0.86
        top2 = [(ex - rx * sl * 1.0 * s2, ey - ry * 0.62), (ex, ey - ry * 0.40), (ex + rx * sl * 1.0 * s2, ey + ry * 0.0)]
        bot2 = [(ex + rx * sl * 1.0 * s2, ey + ry * 0.0), (ex + rx * sl * 0.2, ey + ry * 0.56), (ex - rx * sl * 0.85 * s2, ey + ry * 0.38), (ex - rx * sl * 1.0 * s2, ey - ry * 0.62)]
        cv.poly(top2 + bot2, C("#FF9F1C"))
        cv.poly([(x, y - ry * 0.10) for x, y in top2] + [(ex + rx * sl * 0.5, ey + ry * 0.10), (ex - rx * sl * 0.5, ey + ry * 0.10)], C("#FFD24A"))
        cv.ell(ex, ey + ry * 0.0, rx * 0.15, ry * 0.52, C("#2A1004"))                              # a thin slit pupil
        cv.ell(ex - rx * sl * 0.30, ey - ry * 0.24, rx * 0.12, ry * 0.12, C("#FFFFFF"))
        cv.line([(ex - rx * sl * 1.15, ey - ry * 1.35), (ex, ey - ry * 0.90), (ex + rx * sl * 1.15, ey - ry * 0.45)], ln, lw(opts, 0.62))     # the slanted brow
        return
    k = 1.2 if mood == "surprised" else 1.0
    rx, ry = rx * k, ry * k
    cv.ell(ex, ey, rx * 1.12, ry * 1.12, ln)
    cv.ell(ex, ey, rx * 1.0, ry * 1.0, C("#FFF6DA"))
    if mood == "surprised":
        cv.ell(ex, ey, rx * 0.72, ry * 0.76, C("#E08F1C"))
        cv.ell(ex, ey - ry * 0.04, rx * 0.64, ry * 0.66, C("#FFC83A"))
        cv.ell(ex, ey - ry * 0.18, rx * 0.46, ry * 0.36, C("#FFE98A"))
        cv.ell(ex, ey, rx * 0.20, ry * 0.26, C("#3A1A08"))
        cv.ell(ex - rx * 0.28, ey - ry * 0.34, rx * 0.20, ry * 0.18, C("#FFFFFF"))
        cv.line([(ex - rx * 1.0, ey - ry * 1.45), (ex, ey - ry * 1.85), (ex + rx * 1.0, ey - ry * 1.45)], ln, lw(opts, 0.5))       # raised brows
        return
    cv.ell(ex, ey + ry * 0.04, rx * 0.84, ry * 0.88, C("#E08F1C"))
    cv.ell(ex, ey - ry * 0.02, rx * 0.74, ry * 0.76, C("#FFC83A"))
    cv.ell(ex, ey - ry * 0.18, rx * 0.56, ry * 0.46, C("#FFE98A"))
    cv.ell(ex, ey + ry * 0.04, rx * 0.30, ry * 0.58, C("#3A1A08"))
    cv.ell(ex - rx * 0.30, ey - ry * 0.34, rx * 0.22, ry * 0.2, C("#FFFFFF"))
    cv.ell(ex + rx * 0.28, ey + ry * 0.30, rx * 0.10, ry * 0.09, C("#FFFFFF", 230))
    cv.arc(ex, ey, rx * 1.05, ry * 1.05, 195, 345, ln, lw(opts, 0.7))                                    # the heavier upper lid
    cv.line([(ex - rx * 1.0, ey - ry * 1.18), (ex - rx * 0.1, ey - ry * 1.5), (ex + rx * 0.95, ey - ry * 1.22)], ln, lw(opts, 0.42))   # the brow stroke


def cub_head_front(w, h, face, opts):
    """The face of the cub in a mood (opts mood: happy | sleepy | fierce | surprised): golden fur with a light brow band and cheeks, the eyes of the mood, a forehead mark. The overlay faces use the
    same fur so they sit on the base face without a seam."""
    mood = opts.get("mood", "happy")
    p = pal("cub")
    cr = pal("cream")
    fw, fh = opts["_size"]
    sx, sy = w / fw, h / fh
    cv = fur_canvas(w, h, "north", {"_d": opts["_d"], "_size": opts["_size"], "strands": 0.8, "edges": "", "seed": 1})        # the same fur under every mood (no mood in the seed)
    for side in (-1, 1):                                                                              # cheeks: lighter fur under the eyes, toward the sides, and a blush
        ex = (EYE_UNITS[0] if side < 0 else fw - EYE_UNITS[0]) * sx
        cv.ell(ex, h * 0.72, w * 0.17, h * 0.17, cr["base"])
        cv.ell(ex, h * 0.70, w * 0.11, h * 0.10, cr["hi"])
        cv.ell(ex + side * w * 0.01, h * 0.64, w * 0.075, h * 0.05, (255, 150, 120, 120 if mood != "sleepy" else 190))
    cv.rect(w * 0.34, h * 0.60, w * 0.66, h, cr["base"])                                                # the muzzle bridge continues into the cream muzzle
    cv.rect(w * 0.38, h * 0.62, w * 0.62, h, cr["hi"])
    cx = w / 2
    if mood == "fierce":                                                                               # a frown: two short furrows
        cv.line([(cx - w * 0.04, h * 0.05), (cx - w * 0.01, h * 0.26)], (p["deep"][0], p["deep"][1], p["deep"][2], 255), lw(opts, 0.42))
        cv.line([(cx + w * 0.04, h * 0.05), (cx + w * 0.01, h * 0.26)], (p["deep"][0], p["deep"][1], p["deep"][2], 255), lw(opts, 0.42))
    else:                                                                                              # the forehead mark: a short V of darker fur and a tuft line
        cv.line([(cx - w * 0.07, h * 0.06), (cx, h * 0.24), (cx + w * 0.07, h * 0.06)], (p["lo"][0], p["lo"][1], p["lo"][2], 255), lw(opts, 0.42))
        cv.line([(cx, h * 0.05), (cx, h * 0.18)], (p["deep"][0], p["deep"][1], p["deep"][2], 255), lw(opts, 0.3))
    for side in (-1, 1):
        ex = (EYE_UNITS[0] if side < 0 else fw - EYE_UNITS[0]) * sx
        ey = EYE_UNITS[1] * sy
        cub_eye(cv, mood, ex, ey + (sy * 0.3 if mood == "sleepy" else 0.0), EYE_UNITS[2] * sx, EYE_UNITS[3] * sy, side, opts)
    outline(cv, p, opts, "tblr", 0.85)
    return cv.done()


def cub_muzzle_front(w, h, face, opts):
    """The cream muzzle seen from the front: whisker dots, the line from the nose down and the little 'w' of the mouth along the lower edge."""
    p = pal("cream")
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p["base"])
    cv.rect(0, 0, w, h * 0.45, p["hi"])
    cv.rect(0, h * 0.82, w, h, p["lo"])
    cx = w / 2
    ln = p["line"]
    cv.line([(cx, h * 0.12), (cx, h * 0.50)], (ln[0], ln[1], ln[2], 255), lw(opts, 0.32))
    cv.line([(cx, h * 0.50), (cx - w * 0.20, h * 0.66), (cx - w * 0.34, h * 0.60)], (ln[0], ln[1], ln[2], 255), lw(opts, 0.32))
    cv.line([(cx, h * 0.50), (cx + w * 0.20, h * 0.66), (cx + w * 0.34, h * 0.60)], (ln[0], ln[1], ln[2], 255), lw(opts, 0.32))
    for side in (-1, 1):
        for j, (dx, dy) in enumerate(((0.26, 0.28), (0.34, 0.40), (0.22, 0.42))):
            cv.ell(cx + side * w * dx, h * dy, max(0.8, w * 0.025), max(0.8, w * 0.025), (ln[0], ln[1], ln[2], 210))
    outline(cv, p, opts, "tlr", 0.8)
    return cv.done()


def cub_nose(w, h, face, opts):
    """The nose: a dark rose triangle with a pale highlight."""
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, C("#7B3A2E"))
    cv.poly([(w * 0.08, h * 0.12), (w * 0.92, h * 0.12), (w * 0.5, h * 0.95)], C("#9C5444"))
    cv.poly([(w * 0.2, h * 0.18), (w * 0.62, h * 0.18), (w * 0.4, h * 0.46)], C("#C98C7C"))
    cv.ell(w * 0.34, h * 0.26, max(0.8, w * 0.07), max(0.6, h * 0.08), C("#FFE9DC"))
    outline(cv, pal("cub"), opts, "tblr", 0.8)
    return cv.done()


def mouth_in(w, h, face, opts):
    """The inside of the mouth (the top of the lower jaw, the underside of the upper one): rose with a pale tongue stripe and two small fangs."""
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, C("#8E3B3B"))
    cv.rect(w * 0.2, h * 0.1, w * 0.8, h * 0.9, C("#C5535C"))
    cv.rect(w * 0.36, h * 0.1, w * 0.64, h * 0.9, C("#E8808A"))
    cv.poly([(w * 0.06, 0), (w * 0.16, 0), (w * 0.11, h * 0.2)], C("#FFF6DA"))
    cv.poly([(w * 0.84, 0), (w * 0.94, 0), (w * 0.89, h * 0.2)], C("#FFF6DA"))
    outline(cv, pal("cub"), opts, "tblr", 0.8)
    return cv.done()


def ear_front(w, h, face, opts):
    """The front of an ear: a dark golden rim round a warm, light inside."""
    p = pal("cub")
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p["lo"])
    cv.rect(w * 0.16, h * 0.16, w * 0.84, h, C("#F2A98B"))
    cv.rect(w * 0.26, h * 0.30, w * 0.74, h, C("#FFD0B4"))
    cv.rect(w * 0.30, h * 0.38, w * 0.52, h, C("#FFE6D2"))
    outline(cv, p, opts, "tblr", 0.9)
    return cv.done()


def paw(w, h, face, opts):
    """A paw seen from the front or the top: light fur with three toe lines."""
    p = pal("paw")
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p["base"])
    cv.rect(0, 0, w, h * 0.35, p["hi"])
    cv.rect(0, h * 0.78, w, h, p["lo"])
    ln = p["line"]
    for i in (1, 2):
        cv.line([(w * i / 3, h * 0.30), (w * i / 3, h * 0.95)], (ln[0], ln[1], ln[2], 230), lw(opts, 0.30))
    outline(cv, p, opts, "tblr", 0.85)
    return cv.done()


# ================================================================================================================== rings, prism, feathers, core
def ring_art(w, h, face, opts):
    """A ring on a square sheet: transparent inside and outside, a bright gold band with a pale edge and a dark thin line on each rim (a cut-out, drawn solid), with small eyes or ticks along it.
    opts: inner (inner radius share, 0.70), eyes (how many), dark (a darker gold for the lower rings)."""
    p = pal("gold")
    cv = Cv(w, h)
    cx, cy = w / 2, h / 2
    R = w * 0.495
    inner = float(opts.get("inner", 0.72))
    r0 = R * inner
    cv.ell(cx, cy, R, R, p["line"])
    cv.ell(cx, cy, R - lw(opts, 0.3), R - lw(opts, 0.3), p["lo"] if opts.get("dark") else p["base"])
    band = R - r0
    cv.ell(cx, cy, R - band * 0.22, R - band * 0.22, p["base"])
    cv.ell(cx, cy, R - band * 0.40, R - band * 0.40, p["hi"])
    cv.ell(cx, cy, R - band * 0.58, R - band * 0.58, p["hi2"])
    cv.ell(cx, cy, R - band * 0.80, R - band * 0.80, p["lo"])
    cv.ell(cx, cy, r0 + lw(opts, 0.3), r0 + lw(opts, 0.3), p["line"])
    # punch the inside out
    a = np.asarray(cv.img.getchannel("A")).copy()
    hole = Image.new("L", cv.img.size, 255)
    ImageDraw.Draw(hole).ellipse([(cx - r0) * SS, (cy - r0) * SS, (cx + r0) * SS, (cy + r0) * SS], fill=0)
    cv.img.putalpha(Image.fromarray(np.minimum(a, np.asarray(hole)).astype(np.uint8)))
    cv.dr = ImageDraw.Draw(cv.img)
    n = int(opts.get("eyes", 0))
    rm = (R + r0) / 2
    for i in range(n):
        ang = (i + 0.5) / n * math.tau
        ex, ey = cx + math.cos(ang) * rm, cy + math.sin(ang) * rm
        s = band * 0.30
        cv.ell(ex, ey, s * 1.35, s * 0.8, p["line"])
        cv.ell(ex, ey, s * 1.12, s * 0.62, C("#FFFDF0"))
        cv.ell(ex, ey, s * 0.52, s * 0.52, C("#F0A828"))
        cv.ell(ex, ey, s * 0.24, s * 0.30, C("#3A1A08"))
    nt = int(opts.get("ticks", 0))
    for i in range(nt):
        ang = (i + 0.5) / nt * math.tau + math.pi / nt
        x0, y0 = cx + math.cos(ang) * (r0 + band * 0.1), cy + math.sin(ang) * (r0 + band * 0.1)
        x1, y1 = cx + math.cos(ang) * (R - band * 0.1), cy + math.sin(ang) * (R - band * 0.1)
        cv.line([(x0, y0), (x1, y1)], (p["line"][0], p["line"][1], p["line"][2], 200), lw(opts, 0.22))
    return cv.done()


def prism(w, h, face, opts):
    """A cut gem face: triangular facets in four tones round a bright table, thin dark gold facet lines."""
    p = pal("crystal")
    cv = Cv(w, h)
    cx, cy = w / 2, h / 2
    cv.rect(0, 0, w, h, p["base"])
    tones = [p["hi"], p["lo"], p["base"], p["deep"], p["hi2"], p["lo"], p["hi"], p["base"]]
    n = 8
    for i in range(n):
        a0, a1 = i / n * math.tau, (i + 1) / n * math.tau
        pts = [(cx, cy), (cx + math.cos(a0) * w, cy + math.sin(a0) * h), (cx + math.cos(a1) * w, cy + math.sin(a1) * h)]
        cv.poly(pts, tones[(i + int(opts.get("turn", 0))) % n])
    t = 0.30
    cv.poly([(cx - w * t, cy - h * t), (cx + w * t, cy - h * t), (cx + w * t, cy + h * t), (cx - w * t, cy + h * t)], p["hi"])
    cv.poly([(cx - w * t * 0.5, cy - h * t * 0.5), (cx + w * t * 0.3, cy - h * t * 0.5), (cx + w * t * 0.3, cy + h * t * 0.1), (cx - w * t * 0.5, cy + h * t * 0.1)], p["hi2"])
    for i in range(n):
        a0 = i / n * math.tau
        cv.line([(cx, cy), (cx + math.cos(a0) * w, cy + math.sin(a0) * h)], (p["line"][0], p["line"][1], p["line"][2], 190), lw(opts, 0.20))
    outline(cv, p, opts, "tblr", 0.9)
    return cv.done()


def core(w, h, face, opts):
    """The seraph's face-less core: a pale golden lens of three hard tone steps, a soft white heart and a thin ring; no features."""
    p = pal("crystal")
    cv = Cv(w, h)
    cx, cy = w / 2, h / 2
    cv.rect(0, 0, w, h, p["lo"])
    cv.ell(cx, cy, w * 0.50, h * 0.50, p["base"])
    cv.ell(cx, cy, w * 0.38, h * 0.38, p["hi"])
    cv.ell(cx, cy, w * 0.24, h * 0.24, p["hi2"])
    cv.arc(cx, cy, w * 0.43, h * 0.43, 0, 360, (p["deep"][0], p["deep"][1], p["deep"][2], 200), lw(opts, 0.22))
    outline(cv, p, opts, "tblr", 0.9)
    return cv.done()


def eye_mark(cv, cx, cy, rx, ry, opts, tilt=0.0, big=False):
    """A small almond eye: dark outline, white, a golden iris in two tones, a pupil, a catch light; the upper lid is heavier."""
    ln = C("#4A2A08")
    almond(cv, cx, cy, rx * 1.12, ry * 1.18, ln)
    almond(cv, cx, cy, rx, ry, C("#FFFEF6"))
    r = min(rx * 0.55, ry * 1.05)
    cv.ell(cx, cy, r, r, C("#E69A1C"))
    cv.ell(cx, cy - r * 0.12, r * 0.82, r * 0.82, C("#FFC83A"))
    cv.ell(cx, cy - r * 0.35, r * 0.5, r * 0.4, C("#FFE98A"))
    cv.ell(cx, cy, r * 0.36, r * 0.55, C("#3A1A08"))
    cv.ell(cx - r * 0.3, cy - r * 0.42, max(0.6, r * 0.18), max(0.6, r * 0.18), C("#FFFFFF"))
    cv.line([(cx - rx * 1.0, cy), (cx - rx * 0.4, cy - ry * 0.95), (cx + rx * 0.4, cy - ry * 0.95), (cx + rx * 1.0, cy)], (ln[0], ln[1], ln[2], 255), lw(opts, 0.28))


def seraph_face(w, h, face, opts):
    """The face of the seraph's mask of light, in a mood: serene (closed, gentle eyes, a faint smile), stern (straight slanted brows, narrow open slits, a level mouth), compassionate (heavy lids, raised
    inner brows, a tear of light, a soft smile), radiant (wide open eyes with a bright iris, high brows, an open smile and rays). Pale gold with thin deep-gold lines."""
    mood = opts.get("mood", "serene")
    p = pal("crystal")
    g = pal("gold")
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p["base"])
    cv.ell(w / 2, h * 0.52, w * 0.56, h * 0.58, p["hi"])
    cv.ell(w / 2, h * 0.46, w * 0.36, h * 0.38, p["hi2"])
    cv.rect(0, h * 0.88, w, h, p["lo"])
    ln = g["deep"]
    lnk = (g["line"][0], g["line"][1], g["line"][2], 255)
    t = lw(opts, 0.30)
    exs = (w * 0.30, w * 0.70)
    ey = h * 0.46
    rx = w * 0.15
    if mood == "serene":
        for ex in exs:
            cv.arc(ex, ey - h * 0.02, rx, h * 0.08, 10, 170, lnk, t * 1.4)
            cv.line([(ex - rx * 0.9, ey + h * 0.01), (ex - rx * 1.1, ey + h * 0.04)], lnk, t * 0.8)
            cv.arc(ex, ey - h * 0.15, rx * 1.1, h * 0.05, 200, 340, ln, t)
        cv.line([(w * 0.5, h * 0.52), (w * 0.5, h * 0.60)], (ln[0], ln[1], ln[2], 140), t * 0.8)
        cv.arc(w * 0.5, h * 0.66, w * 0.10, h * 0.05, 20, 160, lnk, t)
    elif mood == "stern":
        for k, ex in enumerate(exs):
            sl = 1 if k == 0 else -1
            cv.line([(ex - sl * rx * 1.1, ey - h * 0.17), (ex + sl * rx * 1.1, ey - h * 0.08)], lnk, t * 1.7)
            cv.poly([(ex - rx, ey), (ex, ey - h * 0.04), (ex + rx, ey), (ex, ey + h * 0.035)], C("#FFF8D8"))
            cv.ell(ex, ey, rx * 0.30, h * 0.03, C("#F0A828"))
            cv.arc(ex, ey, rx, h * 0.04, 190, 350, lnk, t * 1.3)
        cv.line([(w * 0.5, h * 0.52), (w * 0.5, h * 0.60)], (ln[0], ln[1], ln[2], 140), t * 0.8)
        cv.line([(w * 0.40, h * 0.69), (w * 0.60, h * 0.69)], lnk, t * 1.2)
    elif mood == "compassionate":
        for k, ex in enumerate(exs):
            sl = 1 if k == 0 else -1
            cv.ell(ex, ey, rx * 1.0, h * 0.06, p["hi"])
            cv.arc(ex, ey - h * 0.015, rx, h * 0.075, 10, 170, lnk, t * 1.4)
            cv.line([(ex - sl * rx * 1.0, ey - h * 0.10), (ex + sl * rx * 1.05, ey - h * 0.19)], ln, t * 1.1)
        for ex in exs:
            cv.ell(ex + (rx * 0.9 if ex > w / 2 else -rx * 0.9), ey + h * 0.13, max(0.8, w * 0.02), max(1.0, h * 0.035), C("#FFFFFF"))
        cv.line([(w * 0.5, h * 0.52), (w * 0.5, h * 0.60)], (ln[0], ln[1], ln[2], 140), t * 0.8)
        cv.arc(w * 0.5, h * 0.665, w * 0.09, h * 0.045, 20, 160, lnk, t)
    else:
        for ex in exs:
            cv.poly([(ex - rx * 1.1, ey), (ex, ey - h * 0.10), (ex + rx * 1.1, ey), (ex, ey + h * 0.08)], lnk)
            cv.poly([(ex - rx, ey), (ex, ey - h * 0.085), (ex + rx, ey), (ex, ey + h * 0.065)], C("#FFFFF0"))
            cv.ell(ex, ey, rx * 0.46, rx * 0.46, C("#F0A828"))
            cv.ell(ex, ey - h * 0.01, rx * 0.34, rx * 0.34, C("#FFD34D"))
            cv.ell(ex, ey, rx * 0.15, rx * 0.17, C("#4A2606"))
            cv.ell(ex - rx * 0.16, ey - rx * 0.18, max(0.8, rx * 0.10), max(0.8, rx * 0.10), C("#FFFFFF"))
            cv.arc(ex, ey - h * 0.20, rx * 1.2, h * 0.07, 200, 340, ln, t)
        cv.poly([(w * 0.40, h * 0.64), (w * 0.60, h * 0.64), (w * 0.55, h * 0.74), (w * 0.45, h * 0.74)], C("#C97A2C"))
        cv.arc(w * 0.5, h * 0.64, w * 0.10, h * 0.05, 0, 180, lnk, t)
        for i in range(10):
            a = (i + 0.5) / 10 * math.tau
            x0, y0 = w * 0.5 + math.cos(a) * w * 0.40, h * 0.5 + math.sin(a) * h * 0.40
            x1, y1 = w * 0.5 + math.cos(a) * w * 0.70, h * 0.5 + math.sin(a) * h * 0.70
            cv.line([(x0, y0), (x1, y1)], (g["hi2"][0], g["hi2"][1], g["hi2"][2], 150), t * 0.7)
    outline(cv, g, opts, "tblr", 0.9)
    return cv.done()


def feather_shape(w, h, widen=0.34, tip=0.5, base=0.22):
    """The outline of a blade on a w x h sheet (long axis down the sheet, the tip at the top): a rounded tip, the widest place at `widen` of the length, a narrow quill at the bottom."""
    n = 22
    right = []
    for i in range(n + 1):
        t = i / n
        if t < 0.15:
            half = 0.10 + 0.90 * math.sqrt(max(0.0, 1.0 - ((0.15 - t) / 0.15) ** 2))
        elif t < widen:
            half = 1.0
        else:
            k = (t - widen) / max(1e-6, 1 - widen)
            half = 1.0 - (1.0 - base) * (k ** 1.5)
        right.append((0.5 + half * 0.5 * 0.96, t))
    left = [(1.0 - x, y) for x, y in right]
    return [(x * w, y * h) for x, y in right] + [(x * w, y * h) for x, y in reversed(left)]


def feather(w, h, face, opts):
    """A blade of a wing: a cut-out silhouette (alpha), a pale cream body in three tone steps with a gold edge, a central rib, barbs sweeping toward the tip, a thin dark gold outline, and eyes.
    opts: eyes = [(y share, size share), ...] (default one), layer ('primary' | 'covert': the covert is a little warmer), seed."""
    p = pal("feather")
    g = pal("gold")
    if opts.get("tint"):                                                                  # the coverts are a little warmer
        p = {k: mixc(v, g["hi"], 0.22) for k, v in p.items()}
    rng = rng_for("feather", face, opts)
    cv = Cv(w, h)
    shape = feather_shape(w, h, float(opts.get("widen", 0.34)), float(opts.get("tip", 0.5)), float(opts.get("base", 0.22)))
    cv.poly(shape, g["base"])
    # inner body: the shape shrunk toward the rib in two steps
    cx = w / 2
    for shrink, colr in ((0.90, p["lo"]), (0.80, p["base"]), (0.60, p["hi"]), (0.36, p["hi2"])):
        cv.poly([(cx + (x - cx) * shrink, y) for x, y in shape], colr)
    # barbs: fine strokes from the rib toward the edge and the tip
    nb = int(h / max(2.5, opts.get("_d", 3.0) * 1.1))
    for i in range(nb):
        y0 = h * (0.12 + 0.86 * i / max(1, nb))
        for side in (-1, 1):
            x1 = cx + side * w * 0.44
            cv.line([(cx, y0 + h * 0.012), (x1, y0 - h * 0.05)], (g["lo"][0], g["lo"][1], g["lo"][2], 120), lw(opts, 0.14, 1.0))
    cv.line([(cx, h * 0.06), (cx, h * 0.98)], g["deep"], lw(opts, 0.20))
    cv.line([(cx, h * 0.06), (cx, h * 0.98)], (g["hi2"][0], g["hi2"][1], g["hi2"][2], 150), lw(opts, 0.08, 0.7))
    # eyes
    for (ey, es) in opts.get("eyes", [(0.30, 1.0)]):
        eyc = cx
        rx = w * 0.34 * es
        ry = max(1.6, w * 0.17 * es)
        eye_mark(cv, eyc, h * ey, rx, ry, opts)
    cv.line(shape + [shape[0]], (g["line"][0], g["line"][1], g["line"][2], 235), lw(opts, 0.30))
    return cv.done()


def hand(w, h, face, opts):
    """A hand of light: a pale gold glove with fine finger lines."""
    p = pal("robe")
    g = pal("gold")
    cv = Cv(w, h)
    cv.rect(0, 0, w, h, p["base"])
    cv.rect(0, 0, w, h * 0.3, p["hi"])
    cv.rect(0, h * 0.72, w, h, p["lo"])
    for i in (1, 2, 3):
        cv.line([(w * i / 4, h * 0.35), (w * i / 4, h * 0.95)], (g["line"][0], g["line"][1], g["line"][2], 200), lw(opts, 0.22))
    outline(cv, g, opts, "tblr", 0.8)
    return cv.done()


# ================================================================================================================== additive sheets (kinds that start with fx_)
def gauss_disc(w, h, sigma=0.32, col="#FFD86A", peak=1.0, power=1.0):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dx, dy = (xx - (w - 1) / 2) / (w / 2), (yy - (h - 1) / 2) / (h / 2)
    r = np.sqrt(dx * dx + dy * dy)
    a = np.exp(-(r / sigma) ** 2 * 1.2) * np.clip(1 - r, 0, 1) ** power * peak
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = L.rgb(col)
    out[..., 3] = np.clip(a, 0, 1) * 255
    return out


def fx_glow(w, h, face, opts):
    """A soft round glow (additive): tinted pale gold, bright in the middle, falling to nothing before the edge."""
    out = gauss_disc(w, h, opts.get("sigma", 0.36), opts.get("col", "#FFD86A"), opts.get("peak", 0.95))
    return out


def fx_eye(w, h, face, opts):
    """The glow of the cub's eyes in a mood (additive): two soft golden discs at the eye places of the face sheet (one slab for both); wider and brighter when surprised, a narrower amber glow when fierce."""
    mood = opts.get("mood", "happy")
    out = np.zeros((h, w, 4), np.float32)
    fw, fh = opts["_size"]
    sx, sy = w / fw, h / fh
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    acc = np.zeros((h, w), np.float32)
    kx, ky, peak, col = {"happy": (1.5, 1.5, 150, "#FFC944"), "surprised": (1.9, 1.9, 170, "#FFD866"), "fierce": (1.5, 1.0, 190, "#FF9A2A"), "sleepy": (1.2, 0.5, 70, "#FFD070")}[mood]
    for side in (-1, 1):
        ex = (EYE_UNITS[0] if side < 0 else fw - EYE_UNITS[0]) * sx
        ey = EYE_UNITS[1] * sy
        rx, ry = EYE_UNITS[2] * sx * kx, EYE_UNITS[3] * sy * ky
        r = np.sqrt(((xx - ex) / rx) ** 2 + ((yy - ey) / ry) ** 2)
        acc += np.exp(-(r / 0.62) ** 2) * np.clip(1.3 - r, 0, 1)
    out[..., :3] = L.rgb(col)
    out[..., 3] = np.clip(acc, 0, 1) * peak
    return out


def fx_ring(w, h, face, opts):
    """A soft ring of light on a square sheet: a gaussian band round the ring radius (peak pale gold, edges gold), tiny bright beads along it, nothing at the centre and nothing at the rim."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dx, dy = (xx - (w - 1) / 2) / (w / 2), (yy - (h - 1) / 2) / (h / 2)
    r = np.sqrt(dx * dx + dy * dy)
    r0 = float(opts.get("r", 0.80))
    sig = float(opts.get("width", 0.075))
    a = np.exp(-((r - r0) / sig) ** 2) * np.clip((1.0 - r) * 12, 0, 1)
    ang = np.arctan2(dy, dx)
    nb = int(opts.get("beads", 0))
    if nb:
        bead = np.maximum(0, np.cos(ang * nb)) ** 14
        a = a * (0.75 + 0.45 * bead)
    out = np.zeros((h, w, 4), np.float32)
    col = L.rgb(opts.get("col", "#FFCF5A"))
    hot = L.rgb("#FFF0B8")
    k = np.clip(a * 1.1, 0, 1)[..., None]
    out[..., :3] = col * (1 - k * 0.55) + hot * (k * 0.55)
    out[..., 3] = np.clip(a * float(opts.get("peak", 0.9)), 0, 1) * 255
    return out


def fx_spark(w, h, face, opts):
    """A four-point star: two thin crossed spikes in a soft halo."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dx, dy = (xx - (w - 1) / 2) / (w / 2), (yy - (h - 1) / 2) / (h / 2)
    r = np.sqrt(dx * dx + dy * dy)
    spike = np.exp(-(np.abs(dx) / 0.05) ** 1.0) * np.clip(1 - np.abs(dy), 0, 1) ** 2 + np.exp(-(np.abs(dy) / 0.05) ** 1.0) * np.clip(1 - np.abs(dx), 0, 1) ** 2
    halo = np.exp(-(r / 0.22) ** 2)
    a = np.clip(spike * 0.9 + halo * 0.6, 0, 1) * np.clip((1.0 - r) * 3, 0, 1)
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = L.rgb(opts.get("col", "#FFE9A0"))
    out[..., 3] = a * 255
    return out


def fx_core(w, h, face, opts):
    """The glow of a core: a wide soft disc with a brighter heart (additive)."""
    a = gauss_disc(w, h, 0.5, "#FFE39A", 0.9)
    b = gauss_disc(w, h, 0.22, "#FFF7D8", 0.8)
    out = a.copy()
    out[..., :3] = np.where(b[..., 3:4] > a[..., 3:4], b[..., :3], a[..., :3])
    out[..., 3] = np.clip(a[..., 3] + b[..., 3] * 0.6, 0, 255)
    return out


def fx_feather_eye(w, h, face, opts):
    """The glow of the eyes on a blade: soft golden discs at the same places as the eyes of the colour sheet (same options)."""
    out = np.zeros((h, w, 4), np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    acc = np.zeros((h, w), np.float32)
    for (ey, es) in opts.get("eyes", [(0.30, 1.0)]):
        cx, cy = w / 2, h * ey
        rx, ry = w * 0.46 * es, max(2.0, w * 0.30 * es)
        r = np.sqrt(((xx - cx) / rx) ** 2 + ((yy - cy) / ry) ** 2)
        acc += np.exp(-(r / 0.6) ** 2) * np.clip(1.25 - r, 0, 1)
    out[..., :3] = L.rgb("#FFC63E")
    out[..., 3] = np.clip(acc, 0, 1) * 170
    return out


def fx_face(w, h, face, opts):
    """The glow of the seraph's face in a mood (additive): a faint soft light at the closed eyes (serene), narrow bright slits (stern), a gentle glow and a glint (compassionate), a big bright bloom (radiant)."""
    mood = opts.get("mood", "serene")
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    acc = np.zeros((h, w), np.float32)
    spec = {"serene": (0.16, 0.07, 70, 0.46), "stern": (0.19, 0.045, 170, 0.46), "compassionate": (0.17, 0.09, 110, 0.46), "radiant": (0.20, 0.15, 190, 0.46)}[mood]
    rxf, ryf, peak, eyf = spec
    for ex in (w * 0.30, w * 0.70):
        r = np.sqrt(((xx - ex) / (w * rxf * 1.6)) ** 2 + ((yy - h * eyf) / (h * ryf * 1.6)) ** 2)
        acc += np.exp(-(r / 0.7) ** 2) * np.clip(1.25 - r, 0, 1)
    if mood == "radiant":
        r = np.sqrt(((xx - w / 2) / (w * 0.55)) ** 2 + ((yy - h * 0.5) / (h * 0.55)) ** 2)
        acc += 0.55 * np.exp(-(r / 0.6) ** 2) * np.clip(1.2 - r, 0, 1)
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = L.rgb("#FFE08A")
    out[..., 3] = np.clip(acc, 0, 1) * peak
    return out


def fx_none(w, h, face, opts):
    return np.zeros((h, w, 4), np.float32)


PAINTERS = {
    "fur": fur, "cloth": cloth, "plain": plain, "cub_head_front": cub_head_front, "cub_muzzle_front": cub_muzzle_front, "cub_nose": cub_nose, "mouth_in": mouth_in, "ear_front": ear_front,
    "paw": paw, "ring": ring_art, "prism": prism, "core": core, "feather": feather, "hand": hand,
    "seraph_face": seraph_face, "fx_face": fx_face, "fx_glow": fx_glow, "fx_eye": fx_eye, "fx_ring": fx_ring, "fx_spark": fx_spark, "fx_core": fx_core, "fx_feather_eye": fx_feather_eye, "fx_none": fx_none,
}


def paint(kind, w, h, face, opts):
    """The painter entry the model kit calls: the region of a face of the given size; returns float RGBA (h, w, 4)."""
    return PAINTERS[kind](w, h, face, opts)


# ================================================================================================================== particle textures
def particle_dir():
    return os.path.join(L.ROOT, "textures", "particle")


def save_particle(name, arr):
    os.makedirs(particle_dir(), exist_ok=True)
    a = np.clip(arr + 0.5, 0, 255).astype(np.uint8)
    a[a[..., 3] == 0] = 0                                                    # nothing under a fully transparent texel (the safe additive rule)
    Image.fromarray(a, "RGBA").save(os.path.join(particle_dir(), name + ".png"), optimize=True)


def make_particles():
    """The four soft textures of the beam, the pulse ring, the glow and the spark. Beam: a stripe across (u along the beam, v across it: bright centre, soft falloff, a gentle pulse along it). Ring: a
    soft double ring with beads. Glow and spark: round soft falloffs."""
    # beam: 64 x 64; v across (the ribbon's width), u along; tinted pale gold, bright ridge in the middle
    w, h = 128, 64
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    v = (yy - (h - 1) / 2) / (h / 2)
    u = xx / (w - 1)
    across = np.exp(-(np.abs(v) / 0.34) ** 1.6) * np.clip(1 - np.abs(v), 0, 1)
    along = 0.86 + 0.14 * np.cos(u * math.tau * 6)
    a = np.clip(across * along, 0, 1)
    out = np.zeros((h, w, 4), np.float32)
    k = np.clip(np.exp(-(np.abs(v) / 0.16) ** 2), 0, 1)[..., None]
    out[..., :3] = L.rgb("#FFCF5A") * (1 - k * 0.7) + L.rgb("#FFF4CC") * (k * 0.7)
    out[..., 3] = a * 255
    save_particle("spiritlight_beam", out)
    # ring: 128 x 128
    r1 = fx_ring(128, 128, "up", dict(r=0.84, width=0.06, beads=24, peak=0.95))
    r2 = fx_ring(128, 128, "up", dict(r=0.62, width=0.04, beads=0, peak=0.55))
    out = r1.copy()
    take = r2[..., 3] > r1[..., 3]
    out[take] = r2[take]
    out[..., 3] = np.clip(r1[..., 3] + r2[..., 3] * 0.8, 0, 255)
    save_particle("spiritlight_ring", out)
    save_particle("spiritlight_glow", gauss_disc(64, 64, 0.40, "#FFD86A", 1.0))
    save_particle("spiritlight_spark", fx_spark(64, 64, "up", {}))


if __name__ == "__main__":
    make_particles()
    print("particle textures written to", particle_dir())

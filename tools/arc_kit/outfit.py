"""Why: the body and everything worn on it (torso, arms, legs, boots, gloves, the skirts of a coat or robe, collars, belts, neck pieces, bandages, a cape, armour) is
built here from the `outfit` / `cape` / `armor` sections of a spec, so a character author names materials and colours instead of drawing limbs. Every part gets one
painter composed of layers (the base material, then regions on top of it: sleeve, cuff, glove, bandage, trim, emblem), and the extra geometry only where a shape
needs it (the skirt panels of a coat, a collar, a cape).

The kinds of clothing live in the dictionaries at the top; docs/arc/art_kit.md lists them with the defaults.
"""
import math

from . import color as C
from .core import (Canvas, SIDES, fabric, leather, plate, chain_paint, fur, bone_paint, bandage_paint, scale_paint, metal, skin_paint, solid, px, mix, hsh)

TOP_KINDS = ("bare", "tank", "shirt", "tunic", "vest", "coat", "robe", "uniform", "dress", "kimono", "tabard", "jumpsuit")
LENGTHS = {"none": 0.0, "hip": 0.14, "thigh": 0.42, "knee": 0.62, "calf": 0.80, "ankle": 0.95, "floor": 1.0}
LEG_KINDS = ("trousers", "shorts", "skirt", "bare", "hakama", "baggy", "leggings", "chaps")
BOOT_KINDS = ("boots", "shoes", "sandals", "bare", "geta", "greaves", "sabatons")
GLOVE_KINDS = ("none", "gloves", "fingerless", "wraps", "gauntlets")
SLEEVE_KINDS = ("auto", "none", "short", "long", "wide", "bell", "torn")
NECK_KINDS = ("none", "scarf", "belts", "strap", "cravat", "choker", "necklace", "fur")
COLLARS = ("none", "stand", "high", "fur", "wide")
INNER_KINDS = ("none", "bare", "tank", "shirt")
CAPE_KINDS = ("cape", "cloak", "mantle", "tattered", "hooded_cloak")
ARMOR_PIECES = ("pauldrons", "breastplate", "bracers", "gauntlets", "greaves", "sabatons", "tassets", "helm", "gorget", "spikes")
EMBLEMS = ("clover", "bull", "star", "cross", "skull", "sun", "moon", "flame", "wings", "diamond", "heart", "spade", "rune", "crest", "eye", "tree", "none")
PATTERNS = ("plain", "stripes", "patches", "runes", "checks", "dots")
BODY_PARTS = ("chest", "torso", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r", "shin_l", "shin_r", "neck", "hand_l", "hand_r")

DEFAULT_LENGTH = {"bare": "none", "tank": "none", "shirt": "none", "tunic": "thigh", "vest": "none", "coat": "calf", "robe": "floor", "uniform": "hip",
                  "dress": "knee", "kimono": "ankle", "tabard": "knee", "jumpsuit": "none"}
DEFAULT_OPEN = {"coat": True, "vest": True}


def pal5(spec, contrast=1.0):
    return C.cloth_palette(spec, 5, contrast)


def mat(kind, color, salt=0, **kw):
    """A material painter from a kind and a colour: cloth, heavy, leather, plate, chain, fur, bone, scale, skin, solid, bandage."""
    if kind == "skin":
        return skin_paint(C.skin_palette(color), salt)
    if kind in ("plate", "metal"):
        cols = C.cloth_palette(color, 5, 1.25)
        return plate(cols, salt, glow=kw.get("glow"), trim=kw.get("trim")) if kind == "plate" else metal(cols, salt, glow=kw.get("glow"))
    cols = C.cloth_palette(color, 5, kw.get("contrast", 0.9))
    lum = sum(cols[2]) / 765.0
    bias = 0.14 if lum > 0.62 else (0.06 if lum > 0.45 else 0.0)            # a light cloth sits on the upper tones, so a shaded pleat is still light
    if kind == "cloth":
        return fabric(cols, salt, period=kw.get("period", 8.0), amp=kw.get("amp", 0.15), patch=kw.get("patch"), patch_thr=0.25, hem=kw.get("hem", 0),
                      hem_cols=kw.get("hem_cols"), cut=kw.get("cut", ""), cut_depth=kw.get("cut_depth", 4), seam=kw.get("seam", True), noise=0.07, weave=0.0, dither=0.02, bias=bias,
                      cut_width=kw.get("cut_width", 2.0), cut_taper=kw.get("cut_taper", 1.5))
    if kind == "heavy":
        dark = C.cloth_palette(color, 5, 1.3)
        return fabric(cols, salt, period=kw.get("period", 7.0), amp=kw.get("amp", 0.22), patch=dark, patch_thr=0.2, hem=kw.get("hem", 0), hem_cols=kw.get("hem_cols"), cut=kw.get("cut", ""),
                      cut_depth=kw.get("cut_depth", 5), seam=kw.get("seam", True), noise=0.08, weave=0.0, dither=0.02, bias=bias,
                      cut_width=kw.get("cut_width", 2.0), cut_taper=kw.get("cut_taper", 1.5))
    if kind == "leather":
        return leather(cols, salt, seam=kw.get("seam", True), noise=0.09, bias=bias)
    if kind == "chain":
        return chain_paint(cols, salt)
    if kind == "fur":
        return fur(cols, salt, flow="y", root=0.5, cut=kw.get("cut"), cut_depth=kw.get("cut_depth", 4))
    if kind == "bone":
        return bone_paint(cols, salt)
    if kind == "scale":
        return scale_paint(cols, salt)
    if kind == "bandage":
        return bandage_paint(cols, salt, slope=kw.get("slope", 1.0))
    if kind == "solid":
        return solid(C.rgb(color), noise=2.0, salt=salt)
    raise ValueError("unknown material %r" % kind)


# ------------------------------------------------------------------------------------------------------------------------ layered painters
class Surf:
    """A painter built from layers: the base material, then regions on top (each a mask function and a painter). A transparent texel of a layer shows what is
    below it. `decal(face, canvas)` puts a pre-drawn canvas on one face."""

    def __init__(self, base):
        self.layers = [(None, base)]

    def region(self, mask, painter, faces=None):
        self.layers.append(((mask, faces), painter))
        return self

    def decal(self, face, canvas):
        def m(f, i, j, fw, fh):
            return f == face
        def p(f, i, j, fw, fh):
            c, g = canvas.at(i, j)
            return (c, g) if (c is not None or g is not None) else (None, None)
        self.layers.append(((m, None), p))
        return self

    def decals(self, d):
        for f, cv in d.items():
            self.decal(f, cv)
        return self

    def __call__(self, face, i, j, fw, fh):
        for k in range(len(self.layers) - 1, 0, -1):
            (mask, faces), painter = self.layers[k]
            if faces is not None and face not in faces:
                continue
            if mask is not None and not mask(face, i, j, fw, fh):
                continue
            c, g = painter(face, i, j, fw, fh)
            if c is not None:
                return c, g
            if g is not None:
                return self.layers[0][1](face, i, j, fw, fh)[0], g
        return self.layers[0][1](face, i, j, fw, fh)


def rows_top(n):
    return lambda f, i, j, fw, fh: j < n


def rows_bottom(n):
    return lambda f, i, j, fw, fh: j >= fh - n


def rows_frac(a, b):
    return lambda f, i, j, fw, fh: a * fh <= j < b * fh


def rows_range(a, b):
    return lambda f, i, j, fw, fh: a <= j < b


def everywhere(f, i, j, fw, fh):
    return True


# ------------------------------------------------------------------------------------------------------------------------ emblems
def emblem(kind, w, h, col, glow=None, dark=None):
    """A symbol drawn on a w x h texel canvas (polygons in unit coordinates): the emblem of a squad, a crest on a tabard, a mark on a cape."""
    cv = Canvas(w, h)
    c0 = tuple(col)
    c1 = dark or mix(c0, (0, 0, 0), 0.45)
    g = None if glow is None else tuple(glow)

    def P(pts, cc=c0):
        cv.poly([(x * w, y * h) for x, y in pts], cc, 255, g, 210)

    def E(cx, cy, rx, ry, cc=c0):
        cv.ellipse(cx * w, cy * h, rx * w, ry * h, cc, 255, g, 210)

    if kind == "clover":
        for (cx, cy) in ((0.30, 0.30), (0.70, 0.30), (0.30, 0.66), (0.70, 0.66)):
            E(cx, cy, 0.22, 0.22)
        P([(0.42, 0.42), (0.58, 0.42), (0.58, 0.58), (0.42, 0.58)])
        P([(0.46, 0.6), (0.54, 0.6), (0.52, 1.0), (0.48, 1.0)], c1)
    elif kind == "bull":
        P([(0.28, 0.28), (0.72, 0.28), (0.78, 0.58), (0.66, 0.90), (0.34, 0.90), (0.22, 0.58)])
        P([(0.08, 0.14), (0.26, 0.30), (0.24, 0.42), (0.04, 0.30)])
        P([(0.92, 0.14), (0.74, 0.30), (0.76, 0.42), (0.96, 0.30)])
        P([(0.04, 0.30), (0.04, 0.0), (0.16, 0.18)])
        P([(0.96, 0.30), (0.96, 0.0), (0.84, 0.18)])
        E(0.38, 0.52, 0.06, 0.05, c1)
        E(0.62, 0.52, 0.06, 0.05, c1)
        E(0.42, 0.80, 0.04, 0.04, c1)
        E(0.58, 0.80, 0.04, 0.04, c1)
    elif kind == "star":
        pts = []
        for k in range(10):
            a = -math.pi / 2 + k * math.pi / 5
            r = 0.5 if k % 2 == 0 else 0.22
            pts.append((0.5 + r * math.cos(a), 0.5 + r * math.sin(a) * (h / w if False else 1.0)))
        P(pts)
    elif kind == "cross":
        P([(0.40, 0.0), (0.60, 0.0), (0.60, 0.34), (1.0, 0.34), (1.0, 0.54), (0.60, 0.54), (0.60, 1.0), (0.40, 1.0), (0.40, 0.54), (0.0, 0.54), (0.0, 0.34), (0.40, 0.34)])
    elif kind == "skull":
        E(0.5, 0.40, 0.40, 0.38)
        P([(0.28, 0.62), (0.72, 0.62), (0.68, 0.96), (0.32, 0.96)])
        E(0.34, 0.42, 0.10, 0.11, c1)
        E(0.66, 0.42, 0.10, 0.11, c1)
        P([(0.46, 0.56), (0.54, 0.56), (0.50, 0.66)], c1)
        for x in (0.40, 0.50, 0.60):
            P([(x - 0.02, 0.78), (x + 0.02, 0.78), (x + 0.02, 0.96), (x - 0.02, 0.96)], c1)
    elif kind == "sun":
        E(0.5, 0.5, 0.26, 0.26)
        for k in range(8):
            a = k * math.pi / 4
            P([(0.5 + 0.34 * math.cos(a - 0.14), 0.5 + 0.34 * math.sin(a - 0.14)), (0.5 + 0.5 * math.cos(a), 0.5 + 0.5 * math.sin(a)),
               (0.5 + 0.34 * math.cos(a + 0.14), 0.5 + 0.34 * math.sin(a + 0.14))])
    elif kind == "moon":
        E(0.5, 0.5, 0.46, 0.46)
        E(0.66, 0.44, 0.38, 0.40, c1)
    elif kind == "flame":
        P([(0.5, 0.0), (0.74, 0.34), (0.90, 0.62), (0.76, 0.94), (0.5, 1.0), (0.24, 0.94), (0.10, 0.62), (0.28, 0.40), (0.38, 0.52)])
        P([(0.5, 0.48), (0.62, 0.70), (0.54, 0.92), (0.40, 0.84), (0.38, 0.66)], c1)
    elif kind == "wings":
        P([(0.5, 0.35), (0.0, 0.0), (0.12, 0.4), (0.02, 0.62), (0.22, 0.58), (0.20, 0.82), (0.5, 0.72)])
        P([(0.5, 0.35), (1.0, 0.0), (0.88, 0.4), (0.98, 0.62), (0.78, 0.58), (0.80, 0.82), (0.5, 0.72)])
    elif kind == "diamond":
        P([(0.5, 0.0), (1.0, 0.5), (0.5, 1.0), (0.0, 0.5)])
        P([(0.5, 0.22), (0.78, 0.5), (0.5, 0.78), (0.22, 0.5)], c1)
    elif kind == "heart":
        E(0.30, 0.32, 0.26, 0.26)
        E(0.70, 0.32, 0.26, 0.26)
        P([(0.04, 0.42), (0.96, 0.42), (0.5, 1.0)])
    elif kind == "spade":
        P([(0.5, 0.0), (0.96, 0.52), (0.5, 0.62), (0.04, 0.52)])
        E(0.30, 0.56, 0.22, 0.20)
        E(0.70, 0.56, 0.22, 0.20)
        P([(0.44, 0.60), (0.56, 0.60), (0.62, 1.0), (0.38, 1.0)], c0)
    elif kind == "rune":
        cv.ellipse(w / 2, h / 2, w * 0.48, h * 0.48, c0, 255, g, 210)
        cv.ellipse(w / 2, h / 2, w * 0.38, h * 0.38, c1, 255, None)
        P([(0.5, 0.14), (0.60, 0.5), (0.5, 0.86), (0.40, 0.5)])
        P([(0.14, 0.5), (0.5, 0.40), (0.86, 0.5), (0.5, 0.60)])
    elif kind == "crest":
        P([(0.08, 0.0), (0.92, 0.0), (0.92, 0.55), (0.5, 1.0), (0.08, 0.55)])
        P([(0.18, 0.1), (0.82, 0.1), (0.82, 0.52), (0.5, 0.86), (0.18, 0.52)], c1)
        P([(0.42, 0.2), (0.58, 0.2), (0.58, 0.6), (0.42, 0.6)])
        P([(0.28, 0.32), (0.72, 0.32), (0.72, 0.44), (0.28, 0.44)])
    elif kind == "eye":
        P([(0.0, 0.5), (0.25, 0.2), (0.5, 0.1), (0.75, 0.2), (1.0, 0.5), (0.75, 0.8), (0.5, 0.9), (0.25, 0.8)])
        E(0.5, 0.5, 0.2, 0.3, c1)
        E(0.5, 0.5, 0.08, 0.14, c0)
    elif kind == "tree":
        P([(0.5, 0.0), (0.9, 0.4), (0.7, 0.4), (0.96, 0.7), (0.56, 0.7), (0.56, 1.0), (0.44, 1.0), (0.44, 0.7), (0.04, 0.7), (0.30, 0.4), (0.10, 0.4)])
    return cv


# ------------------------------------------------------------------------------------------------------------------------ the torso
def _opening_poly(fw, fh, top_w, bot_w, y0=0.0, y1=None):
    y1 = fh if y1 is None else y1
    cx = fw / 2.0
    return [(cx - top_w / 2.0, y0), (cx + top_w / 2.0, y0), (cx + bot_w / 2.0, y1), (cx - bot_w / 2.0, y1)]


def torso_surface(R, T, inner_p, top_p, trim_pal, belt, emblems, bandage, bandage_p, salt):
    """The painter of the torso box. T = the resolved `top` spec. Draws the opening or neckline, the trim along it, buttons, the belt band and the emblems."""
    d = R.d
    fw, fh = int(round(d.tw * 2)), int(round(d.torso_h * 2))
    sw = int(round(d.td * 2))
    kind = T["kind"]
    s = Surf(top_p if kind not in ("bare",) else inner_p)
    trim = trim_pal
    front = Canvas(fw, fh)
    back = Canvas(fw, fh)
    belt_rows = 0
    if belt:
        belt_rows = 5 if belt.get("kind", "plain") != "obi" else 8
        by0 = fh - belt_rows - 1
    is_open = T.get("open", DEFAULT_OPEN.get(kind, False))
    if kind in ("tank", "shirt", "tunic", "jumpsuit", "uniform", "dress", "tabard", "robe", "kimono"):
        neck_w = {"tank": 8, "shirt": 5, "tunic": 6, "jumpsuit": 5, "uniform": 4, "dress": 6, "tabard": 6, "robe": 6, "kimono": 7}[kind]
        depth = {"tank": 5, "shirt": 3, "tunic": 4, "jumpsuit": 2, "uniform": 3, "dress": 4, "tabard": 4, "robe": 5, "kimono": 9}[kind]
        if kind == "kimono":
            # the wrap: one panel crosses over the other, a diagonal edge of trim from the neck to the waist
            for y in range(0, fh - belt_rows - 1):
                x = int(fw / 2 - 3 + y * (fw * 0.28) / max(1, fh - belt_rows))
                front.put(x, y, trim[3])
                front.put(x - 1, y, trim[1])
                if y < 7:
                    for xx in range(int(fw / 2 - 4), x - 1):
                        front.put(xx, y, inner_p("front", xx, y, fw, fh)[0])
        else:
            # a neckline: a rounded / V cut at the top showing the inner layer
            for y in range(depth):
                half = neck_w / 2.0 * (1.0 - y / float(depth)) + 0.5
                for x in range(int(fw / 2 - half), int(fw / 2 + half)):
                    c = inner_p("front", x, y, fw, fh)[0]
                    front.put(x, y, c)
                front.put(int(fw / 2 - half) - 1, y, trim[1]) if kind in ("uniform", "tunic", "dress", "robe", "tabard") else None
                front.put(int(fw / 2 + half), y, trim[1]) if kind in ("uniform", "tunic", "dress", "robe", "tabard") else None
    if is_open:
        top_w = T.get("open_top", 7)
        bot_w = T.get("open_bottom", 3)
        poly = _opening_poly(fw, fh, top_w, bot_w, 0, fh)
        # the inner layer through the gap, with trim along both edges
        for y in range(fh):
            half = (top_w + (bot_w - top_w) * y / float(fh)) / 2.0
            xa, xb = int(round(fw / 2.0 - half)), int(round(fw / 2.0 + half))
            for x in range(xa, xb):
                front.put(x, y, inner_p("front", x, y, fw, fh)[0])
            for k in range(2):
                front.put(xa - 1 - k, y, trim[3 - k] if k == 0 else trim[1])
                front.put(xb + k, y, trim[3 - k] if k == 0 else trim[1])
    if T.get("buttons") and not is_open:
        for y in range(5, fh - belt_rows - 2, 5):
            front.rect(fw // 2 - 1, y, fw // 2, y + 1, trim[3])
            front.put(fw // 2 - 1, y, trim[4])
    if T.get("pattern", "plain") != "plain":
        _pattern(front, T["pattern"], T.get("pattern_color", trim[2]), fw, fh)
        _pattern(back, T["pattern"], T.get("pattern_color", trim[2]), fw, fh)
    if T.get("hem_trim", False):
        for x in range(fw):
            front.put(x, fh - 1, trim[2])
            back.put(x, fh - 1, trim[2])
    for em in emblems:
        ew = int(round(em.get("size", 6) * 2))
        eh = int(round(em.get("size", 6) * 2 * em.get("aspect", 1.0)))
        cv = emblem(em["kind"], ew, eh, em["color"], em.get("glow"), em.get("dark"))
        where = em.get("where", "back")
        cvs = []
        if where in ("back", "both"):
            cvs.append(back)
        if where in ("chest", "both"):
            cvs.append(front)
        for target in cvs:
            ox = (fw - ew) // 2 + int(em.get("dx", 0) * 2)
            oy = int(em.get("y", 3) * 2)
            for yy in range(eh):
                for xx in range(ew):
                    c, g = cv.at(xx, yy)
                    if c is not None or g is not None:
                        target.put(ox + xx, oy + yy, c, 255 if c else 0, g[:3] if g else None, g[3] if g else 255)
    s.decal("front", front)
    s.decal("back", back)
    if belt:
        bp = _belt_painter(belt, salt)
        s.region(rows_range(by0, by0 + belt_rows), bp, faces=SIDES)
        if belt.get("kind") == "double":                         # a second, thinner strap above the belt
            s.region(rows_range(by0 - 4, by0 - 2), bp, faces=SIDES)
        bw = Canvas(fw, fh)
        _buckle(bw, belt, fw, by0, belt_rows)
        s.decal("front", bw)
    if bandage and (bandage.get("torso") or bandage.get("chest")):
        s.region(rows_range(int(fh * 0.05), int(fh * 0.62)), bandage_p, faces=SIDES)
    return s


def _pattern(cv, kind, col, fw, fh):
    if kind == "stripes":
        for x in range(2, fw, 6):
            for y in range(fh):
                cv.put(x, y, col)
    elif kind == "checks":
        for y in range(0, fh, 4):
            for x in range(0, fw, 4):
                if ((x // 4) + (y // 4)) % 2 == 0:
                    cv.rect(x, y, x + 1, y + 1, col, 160)
    elif kind == "dots":
        for y in range(2, fh, 5):
            for x in range(2 + (y // 5 % 2) * 2, fw, 5):
                cv.put(x, y, col)
    elif kind == "runes":
        for y in range(3, fh - 3, 6):
            for x in range(3, fw - 3, 6):
                if hsh(x, y, 4) > 0.45:
                    cv.line(x, y, x, y + 2, col)
                    cv.put(x + 1, y + 1, col)
    elif kind == "patches":
        for (x, y, w, h) in ((3, 4, 4, 3), (fw - 8, fh // 2, 5, 4), (fw // 2 - 2, fh - 9, 4, 3)):
            cv.rect(x, y, x + w, y + h, col, 200)
            for xx in range(x, x + w + 1, 2):
                cv.put(xx, y, mix(col, (0, 0, 0), 0.4))


def _belt_painter(belt, salt):
    kind = belt.get("kind", "plain")
    col = belt.get("color", "#3c281c")
    return mat("leather" if kind not in ("obi", "sash") else "cloth", col, salt + 3, seam=True)


def _buckle(cv, belt, fw, by0, rows):
    if belt.get("kind") in ("obi", "sash"):
        return
    bc = C.rgb(belt.get("buckle") or "#c8a040")
    hi = mix(bc, (255, 255, 255), 0.4)
    x0 = fw // 2 - 2
    cv.rect(x0, by0, x0 + 3, by0 + rows - 1, bc)
    cv.rect(x0 + 1, by0 + 1, x0 + 2, by0 + rows - 2, mix(bc, (0, 0, 0), 0.5))
    cv.put(x0, by0, hi)


# ------------------------------------------------------------------------------------------------------------------------ build the whole body
def _hand_painter(gl, skin_p, side):
    return skin_p


class Body:
    """Everything the body builder decided that other parts (clips, props, accessories) need to know."""

    def __init__(self):
        self.skirt = False
        self.skirt_len = 0.0
        self.long_robe = False
        self.cape = False
        self.sleeve_wide = False
        self.notes = []


def build_body(R, S, Lk):
    """Torso, head attach points, arms, hands, legs, boots, the coat or robe skirts and the details. Lk = the resolved palettes (see build.Look)."""
    d = R.d
    O = S["outfit"]
    body = Body()
    top = O["top"]
    kind = top["kind"]
    inner = O["inner"]
    sleeves = O["sleeves"]
    legs = O["legs"]
    boots = O["boots"]
    gloves = O["gloves"]
    belt = O.get("belt")
    neck = O.get("neck", {"kind": "none"})
    band = {p: True for p in (O.get("bandages") or {}).get("parts", [])}
    band_col = (O.get("bandages") or {}).get("color", "#d8d0b4")
    band_p = mat("bandage", band_col, 71)
    skin = Lk.skin
    skin_p = skin_paint(skin, 3)
    top_col = top.get("color", "#3c3c48")
    trim_col = top.get("trim") or mix(C.rgb(top_col), (255, 255, 255), 0.5)
    top_kind_mat = top.get("material") or ("heavy" if top.get("fold") else "cloth")
    ragged = top.get("ragged", False)
    top_p = mat(top_kind_mat, top_col, 11, hem=0)
    trim_pal = C.shades(trim_col, 5, 1.0)
    inner_kind = inner.get("kind", "bare" if kind in ("bare", "vest") else "tank")
    if inner_kind == "bare" or inner_kind == "none":
        inner_p = _bare_torso(skin, d)
    else:
        inner_p = mat("cloth", inner.get("color", "#ecebea"), 17, period=9.0)
    emblems = []
    if top.get("emblem") and top["emblem"].get("kind", "none") != "none":
        em = dict(top["emblem"])
        em["color"] = C.rgb(em.get("color", trim_col))
        if em.get("glow") is not None:
            em["glow"] = C.rgb(em["glow"])
        emblems.append(em)

    # ---- torso
    tw, th, td = d.tw, d.torso_h, d.td
    torso_p = torso_surface(R, dict(top, kind=kind), inner_p, top_p, trim_pal, belt, emblems, band, band_p, 5)
    R.box("body", (-tw / 2, d.hip_y, -td / 2), (tw, th, td), torso_p)

    # ---- the arms
    long_sl = {"auto": kind not in ("tank", "vest", "bare", "tabard", "dress"), "none": False, "short": False, "long": True, "wide": True, "bell": True,
               "torn": True}[sleeves.get("kind", "auto")]
    sk_kind = sleeves.get("kind", "auto")
    sl_col = sleeves.get("color") or (top_col if kind not in ("tank",) else inner.get("color", "#ecebea"))
    sl_mat = mat(top_kind_mat if sk_kind != "torn" else "heavy", sl_col, 23)
    cuff_col = sleeves.get("cuff") or trim_col
    cuff_p = mat("cloth", cuff_col, 24, seam=False)
    glove_kind = gloves.get("kind", "none")
    glove_p = None
    if glove_kind != "none":
        glove_p = mat("leather" if glove_kind in ("gloves", "fingerless") else ("plate" if glove_kind == "gauntlets" else "bandage"), gloves.get("color", "#2a2a30"), 29)
    for side in ("l", "r"):
        pass
    # upper arm, forearm, hand: one painter per segment (the pair is symmetric unless a part is bandaged on one side only)
    asym = any(band.get(k) for k in ("arm_l", "forearm_l", "hand_l")) != any(band.get(k) for k in ("arm_r", "forearm_r", "hand_r")) or gloves.get("side", "both") != "both"

    def arm_painters(sg):
        sfx = "_" + sg
        up = Surf(skin_p)
        fo = Surf(skin_p)
        ha = Surf(skin_p)
        if long_sl:
            up.layers[0] = (None, sl_mat)
            fo.layers[0] = (None, sl_mat)
            if sk_kind in ("wide", "bell"):
                pass
            fo.region(rows_bottom(3), cuff_p)
        elif sk_kind == "short":
            up.region(rows_top(int(d.upper * 0.9)), sl_mat)
        if band.get("arm" + sfx):
            up.region(everywhere, band_p)
        if band.get("forearm" + sfx):
            fo.region(everywhere, band_p)
        if band.get("hand" + sfx):
            ha.region(everywhere, band_p)
        gl_on = glove_p is not None and gloves.get("side", "both") in ("both", "left" if sg == "l" else "right")
        if gl_on:
            if glove_kind in ("gauntlets", "wraps"):
                fo.region(lambda f, i, j, fw, fh: j >= fh * 0.35, glove_p)
            elif glove_kind in ("gloves",):
                fo.region(rows_bottom(3), glove_p)
            ha.layers[0] = (None, glove_p) if glove_kind != "fingerless" else (None, skin_p)
            if glove_kind == "fingerless":
                ha.region(rows_top(3), glove_p)
        return up, fo, ha

    fw_ = max(2.0, d.aw - 0.5)
    fd_ = max(2.0, d.ad - 0.5)
    hw_ = max(2.0, fw_ - 0.5)
    hd_ = max(2.0, fd_ - 0.5)
    pl = arm_painters("l")
    pr = arm_painters("r") if asym else None
    ax = d.arm_x
    up_h = d.sh_y - d.elbow_y
    for nm, org, size, idx in (("arm", (ax - d.aw / 2, d.elbow_y, -d.ad / 2), (d.aw, up_h, d.ad), 0),
                               ("forearm", (ax - fw_ / 2, d.wrist_y, -fd_ / 2), (fw_, d.fore, fd_), 1),
                               ("hand", (ax - hw_ / 2, d.wrist_y - d.hand, -hd_ / 2), (hw_, d.hand, hd_), 2)):
        if pr is None:
            R.pair(nm + "_l", nm + "_r", org, size, pl[idx])
        else:
            R.pair2(nm + "_l", nm + "_r", org, size, pl[idx], pr[idx])

    if sk_kind in ("wide", "bell"):                              # a flared sleeve hanging from the forearm (kimono, robe): a bigger box on the forearm bone, hemmed
        wide = sk_kind == "wide"
        sw_, sd_ = fw_ + (3.4 if wide else 2.0), fd_ + (3.0 if wide else 1.8)
        sln = d.fore + 1.0 + (d.hand * 0.5 if wide else 0.0)
        R.pair("forearm_l", "forearm_r", (ax - sw_ / 2, d.wrist_y - (d.hand * 0.5 if wide else 0.0), -sd_ / 2), (sw_, sln, sd_),
               mat(top_kind_mat, sl_col, 27, hem=2, hem_cols=C.shades(cuff_col, 5, 1.0), seam=False), faces=SIDES + ("bottom",))

    # ---- the legs and boots
    thigh_len = d.hip_y + 1.0 - d.knee_y
    leg_kind = legs.get("kind", "trousers")
    leg_col = legs.get("color", "#2c2c38")
    cuts_skin = leg_kind in ("bare", "skirt", "shorts")
    leg_mat = skin_p if leg_kind == "bare" else (mat("heavy", leg_col, 31, period=5.0, amp=0.3) if leg_kind == "hakama" else mat("cloth", leg_col, 31, period=9.0))
    leg_infl = {"hakama": 0.9, "baggy": 0.5}.get(leg_kind, 0.0)
    boot_kind = boots.get("kind", "boots")
    boot_col = boots.get("color", "#2a2220")
    boot_h = {"ankle": 3.0, "calf": 6.0, "knee": 9.0}.get(boots.get("height", "calf"), 6.0)
    boot_p = mat("leather", boot_col, 37) if boot_kind in ("boots", "shoes", "geta", "sandals") else mat("plate", boots.get("color", "#8c98a8"), 37)
    cuff_boot = mat("cloth", boots.get("cuff") or trim_col, 38, seam=False)
    if leg_kind == "hakama" or leg_kind == "baggy":
        pass
    thigh = Surf(leg_mat)
    shin = Surf(leg_mat)
    if leg_kind == "shorts":
        thigh.layers[0] = (None, skin_p)
        thigh.region(rows_top(int(thigh_len * 0.9)), mat("cloth", leg_col, 31))
        shin.layers[0] = (None, skin_p)
    elif leg_kind == "skirt":
        thigh.layers[0] = (None, skin_p)
        shin.layers[0] = (None, skin_p)
    elif leg_kind == "chaps":
        # leather chaps over the trousers: the whole outer face and the outer half of the front, the trousers show on the inner half
        cp = mat("leather", legs.get("chaps", "#c8b48c"), 33)
        ch = lambda f, i, j, fw, fh: (f == "left") or (f == "front" and i >= fw * 0.42) or (f == "back" and i < fw * 0.2)
        thigh.region(ch, cp)
        shin.region(ch, cp)
    elif leg_kind == "leggings":
        pass
    if legs.get("stripe"):
        sp = mat("cloth", legs["stripe"], 34, seam=False)
        st = lambda f, i, j, fw, fh: (f == "left" and 1 <= i < fw - 1) or (f == "front" and i >= fw - 2)
        thigh.region(st, sp)
        shin.region(st, sp)
    if band.get("leg_l") or band.get("leg_r"):
        thigh.region(everywhere, band_p)
    if band.get("shin_l") or band.get("shin_r"):
        shin.region(everywhere, band_p)
    shin_h = d.knee_y - 2.0
    if boot_kind in ("boots", "greaves", "sabatons"):
        n = int(boot_h * 2)
        shin.region(rows_bottom(n), boot_p)
        if boots.get("cuff") is not None or boot_h >= 6:
            shin.region(rows_range(int((shin_h - boot_h) * 2), int((shin_h - boot_h) * 2) + 2), cuff_boot) if boots.get("cuff") is not None else None
    elif boot_kind in ("shoes", "geta", "sandals"):
        shin.region(rows_bottom(2), boot_p)
    foot_p = boot_p if boot_kind not in ("bare",) else skin_p
    if boot_kind == "geta":
        foot_p = mat("leather", boots.get("color", "#5a3c28"), 39)
    if boot_kind == "sandals":
        foot_p = Surf(skin_p).region(lambda f, i, j, fw, fh: (j >= fh - 2 or (i % 4 == 1 and f in ("front", "left", "right"))), boot_p)
    R.pair("leg_l", "leg_r", (d.leg_x - d.lw / 2, d.knee_y, -d.ld / 2), (d.lw, thigh_len, d.ld), thigh, inflate=leg_infl)
    R.pair("shin_l", "shin_r", (d.leg_x - d.lw / 2 + 0.25, 2.0, -d.ld / 2 + 0.25), (d.lw - 0.5, d.knee_y - 2.0, d.ld - 0.5), shin, inflate=leg_infl)
    R.pair("shin_l", "shin_r", (d.leg_x - d.lw / 2, 0.0, -d.ld / 2 - 1.5), (d.lw, 2.0, d.ld + 1.5), foot_p)

    # ---- coat / robe / dress skirts
    ln_frac = LENGTHS[top.get("length", DEFAULT_LENGTH.get(kind, "none"))]
    skirt_kinds = ("coat", "robe", "dress", "kimono", "tabard", "tunic", "uniform")
    if kind in skirt_kinds and ln_frac > 0:
        body.skirt = True
        body.skirt_len = ln_frac * (d.hip_y - 0.5)
        body.long_robe = ln_frac >= 0.8
        _skirt(R, S, top, kind, top_kind_mat, top_col, trim_col, ln_frac, is_open=top.get("open", DEFAULT_OPEN.get(kind, False)), ragged=ragged, belt=belt)

    # ---- collar, neck
    _collar(R, top, top_col, trim_col, kind)
    _neck(R, neck, skin, top_col)
    if band.get("neck"):
        nw = max(2.5, min(4.0, d.tw * 0.42))
        R.box("body", (-(nw + 1.0) / 2, d.sh_y - 0.3, -(nw + 0.8) / 2), (nw + 1.0, d.neck + 1.4, nw + 0.8), band_p, inflate=0.1, faces=SIDES)
    if belt:
        _belt_extras(R, belt)
    return body


def _bare_torso(skin, d):
    """A bare chest: skin with pectoral and abdominal shading (the lines go with the build)."""
    sk = skin_paint(skin, 8)

    def p(face, i, j, fw, fh):
        c, g = sk(face, i, j, fw, fh)
        if face == "front" and fw >= 12:
            cx = fw / 2.0
            u = (i - cx) / (fw / 2.0)
            v = j / float(fh)
            dk = skin[0]
            if abs(i - cx + 0.5) < 0.6 and 0.12 < v < 0.9:                  # the centre line
                return px(dk), None
            if 0.30 < v < 0.36 and 0.12 < abs(u) < 0.85:                    # under the pectorals
                return px(dk), None
            if 0.12 < v < 0.64 and abs(abs(u) - 0.72) < 0.06:               # the outer edge of the pectorals
                return px(dk), None
            if 0.46 < v < 0.9 and abs(u) < 0.5 and (j % 5 == 2) and abs(i - cx + 0.5) > 0.6:     # the abdominal rows
                return px(dk), None
        return c, g
    return p


def _skirt(R, S, top, kind, mkind, color, trim, ln_frac, is_open, ragged, belt):
    """The panels that hang from the hips (bones skirt_l / skirt_r swing with the legs): tiers that widen toward the hem; an open coat leaves the front centre
    open, a robe or dress is closed. The hem is a trim band, or ragged (saw-tooth) for a tattered coat."""
    d = R.d
    full = d.hip_y - 0.3
    ln = max(2.0, ln_frac * full)
    y_bot = d.hip_y - ln
    tiers = 3 if ln > 6 else 2
    cuts = "b" if ragged else ""
    hw = d.tw / 2 + 0.25
    hemp = trim if top.get("hem_trim") or kind in ("robe", "kimono", "dress", "tabard") else None
    for k in range(tiers):
        y1 = d.hip_y + 0.4 - ln * k / tiers
        y0 = d.hip_y + 0.4 - ln * (k + 1) / tiers
        flare = 0.5 * (k + 1) * (1.0 + (0.5 if kind in ("robe", "dress", "kimono") else 0.0))
        last = k == tiers - 1
        p = mat(mkind, color, 40 + k, hem=2 if (last and hemp) else 0, hem_cols=C.shades(hemp, 5, 1.0) if hemp else None, cut=cuts if last else "", cut_depth=5, seam=False)
        w = hw + flare
        dd = d.td + 1.0 + flare * 1.2
        if kind in ("robe", "dress", "kimono", "tabard") and not is_open:
            R.pair("skirt_l", "skirt_r", (0, y0, -dd / 2), (w, y1 - y0, dd), p, faces=SIDES + ("bottom",))
        else:
            th_ = 1.0
            # side, back and the outer half of the front: the centre front stays open
            R.pair("skirt_l", "skirt_r", (w - th_, y0, -dd / 2), (th_, y1 - y0, dd), p, faces=("left", "right", "front", "back", "bottom"))
            R.pair("skirt_l", "skirt_r", (0, y0, dd / 2 - th_), (w, y1 - y0, th_), p, faces=("front", "back", "left", "right", "bottom"))
            R.pair("skirt_l", "skirt_r", (w * 0.45, y0, -dd / 2), (w * 0.55, y1 - y0, th_), p, faces=("front", "back", "left", "right", "bottom"))
    if kind in ("robe", "kimono", "dress", "tabard") or is_open:
        pass


def _collar(R, top, color, trim, kind):
    d = R.d
    c = top.get("collar", "none")
    if c == "none":
        return
    col = top.get("collar_color") or color
    if c == "stand":
        R.box("body", (-3.5, d.sh_y - 0.4, -2.9), (7.0, 2.4, 5.8), mat("cloth", col, 51, seam=False), inflate=0.25, faces=SIDES + ("top",))
    elif c == "high":
        p = mat("cloth", col, 52, seam=False)
        R.box("body", (-4.0, d.sh_y - 0.4, 2.0), (8.0, 4.8, 1.6), p, faces=SIDES + ("top",))
        for sg in (1, -1):
            R.box("body", (sg * 3.2 - 0.8 if sg > 0 else -4.0, d.sh_y - 0.4, -2.2), (1.2, 4.2, 5.0), p, faces=SIDES + ("top",))
    elif c == "fur":
        R.box("body", (-d.tw / 2 - 1.5, d.sh_y - 1.0, -d.td / 2 - 1.5), (d.tw + 3.0, 3.2, d.td + 3.0), mat("fur", col, 53, cut="b", cut_depth=3), inflate=0.1,
              faces=SIDES + ("top",))
    elif c == "wide":
        p = mat("cloth", col, 54, seam=False)
        R.box("body", (-d.tw / 2 - 0.5, d.sh_y - 0.8, -d.td / 2 - 0.5), (d.tw + 1.0, 1.6, d.td + 1.0), p)


def _neck(R, neck, skin, top_col):
    """Things worn at the neck, sized to the neck of the build: scarf, belts (two straps round it), strap, cravat, choker, necklace, fur ring."""
    d = R.d
    k = neck.get("kind", "none")
    if k == "none":
        return
    col = neck.get("color", "#7a1e24")
    nw = max(2.5, min(4.0, d.tw * 0.42))                       # the width of the neck box of build_head
    y0 = d.sh_y
    gold = metal(C.shades("#c8a040", 5, 1.2), 5)
    if k == "scarf":
        R.box("body", (-(nw + 1.8) / 2, y0 - 0.6, -(nw + 1.4) / 2), (nw + 1.8, 2.0 + d.neck * 0.5, nw + 1.4), mat("cloth", col, 61), inflate=0.2)
    elif k == "belts":
        p = mat("leather", col, 62)
        R.box("body", (-(nw + 1.4) / 2, y0 - 0.1, -(nw + 1.2) / 2), (nw + 1.4, 0.9, nw + 1.2), p, inflate=0.15, rotation=(0, 0, 4), pivot=(0, y0 + 0.3, 0))
        R.box("body", (-(nw + 1.4) / 2, y0 + 0.9 + d.neck * 0.3, -(nw + 1.2) / 2), (nw + 1.4, 0.9, nw + 1.2), p, inflate=0.15, rotation=(0, 0, -3), pivot=(0, y0 + 1.3, 0))
        R.box("body", (-0.6, y0 - 0.1, -(nw + 1.2) / 2 - 0.45), (1.2, 1.2, 0.4), gold, faces=("front",))
    elif k == "strap":
        R.box("body", (-(nw + 1.2) / 2, y0 - 0.4, -(nw + 1.0) / 2), (nw + 1.2, 1.2, nw + 1.0), mat("leather", col, 63), inflate=0.2)
        R.box("body", (-0.8, y0 - 0.5, -(nw + 1.0) / 2 - 0.5), (1.6, 1.6, 0.4), gold, faces=("front",))
    elif k == "choker":
        R.box("body", (-(nw + 0.8) / 2, y0 + 0.2 + d.neck * 0.3, -(nw + 0.6) / 2), (nw + 0.8, 0.8, nw + 0.6), mat("leather", col, 64), inflate=0.15)
    elif k == "cravat":
        R.box("body", (-1.5, y0 - 3.0, -d.td / 2 - 0.4), (3.0, 3.6, 0.8), mat("cloth", col, 65), faces=("front", "left", "right"))
    elif k == "necklace":
        R.box("body", (-(nw + 0.6) / 2, y0 + 0.2, -(nw + 0.4) / 2), (nw + 0.6, 0.5, nw + 0.4), gold, inflate=0.15)
        R.box("body", (-1.0, y0 - 2.4, -d.td / 2 - 0.4), (2.0, 2.6, 0.7), metal(C.shades(neck.get("gem", "#c83030"), 5, 1.1), 8, glow=(255, 80, 80, 140) if neck.get("glow") else None),
              faces=("front", "left", "right"))
    elif k == "fur":
        R.box("body", (-d.tw / 2 - 1.2, y0 - 0.8, -d.td / 2 - 1.2), (d.tw + 2.4, 3.0, d.td + 2.4), mat("fur", col, 66, cut="b", cut_depth=3))


def _belt_extras(R, belt):
    d = R.d
    if belt.get("pouches"):
        pp = mat("leather", belt.get("color", "#3c281c"), 80)
        R.pair("body", "body", (d.tw / 2 - 0.2, d.hip_y + 1.0, -1.5), (2.0, 2.5, 3.0), pp)
        R.box("body", (-d.tw / 2 - 1.2, d.hip_y + 0.4, 1.0), (2.0, 3.0, 2.4), pp)
    if belt.get("sash"):
        sc = belt["sash"]
        R.bone("sash", "body", (d.tw / 2 - 1.5, d.hip_y + 2.0, -d.td / 2 - 0.3))
        R.box("sash", (d.tw / 2 - 3.0, d.hip_y - 6.5, -d.td / 2 - 0.9), (3.0, 8.5, 0.8), mat("cloth", sc, 81, hem=2, hem_cols=C.shades(sc, 5, 1.0)), faces=("front", "back", "left", "right", "bottom"))
    if belt.get("books"):
        bp = mat("leather", "#5a2c2c", 82)
        for k, sx in enumerate((-d.tw / 2 + 0.5, d.tw / 2 - 2.7)):
            R.box("body", (sx, d.hip_y + 0.2, -d.td / 2 - 1.2), (2.2, 3.0, 1.2), bp, faces=SIDES + ("top",))

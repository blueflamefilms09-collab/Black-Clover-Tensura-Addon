"""16 x 16 icons of the functional anime loot (procedural pixel art, one consistent palette method), the armour layer textures and the effect icon. Deterministic.

    python3 -B tools/gen_loot_icons.py            writes assets/nusmp/textures/item/<id>.png for the 30 foods, 26 materials and 8 armour pieces,
                                                 assets/nusmp/textures/models/armor/loot_<set>_layer_1/2.png, assets/nusmp/textures/mob_effect/mana_surge.png
    python3 -B tools/gen_loot_icons.py sheet X   also writes a contact sheet (8x) of every icon to X (for a look; not part of the build)

The weapons and trinkets have no icon here: their sprite is rendered from the finished 3D mesh by tools/gen_loot_models.py (the inventory keeps a sprite, the hand shows the model).

How the shading works (so every icon looks like the same hand drew it): a shape is a set of pixels; each pixel gets one of five tones of the shape's base colour from a light
that comes from the upper left (dark, shadow, mid, light, highlight), edge pixels are a tone darker, one pixel gets the highlight; a one-pixel outline of a dark shade of its
neighbours' colour is added round the whole icon at the end. Palettes are ramps of one base colour (never free-hand colours), so a family of items shares a hue.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "loot"))
import roster as R  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
S = 16


# ================================================================ colour
def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def shade(c, k):
    return clamp([v * k for v in c])


def mix(a, b, t):
    return clamp([x + (y - x) * t for x, y in zip(a, b)])


def ramp(base):
    """dark, shadow, mid, light, highlight of one base colour (cooler in the shadow, warmer in the light)."""
    base = hexrgb(base) if isinstance(base, str) else base
    return [mix(shade(base, 0.42), (30, 20, 70), 0.18), mix(shade(base, 0.70), (40, 30, 80), 0.08), base, mix(base, (255, 244, 220), 0.32), mix(base, (255, 255, 255), 0.70)]


# ================================================================ canvas and shapes
class Canvas:
    def __init__(self):
        self.px = {}

    def put(self, x, y, c):
        if 0 <= x < S and 0 <= y < S:
            self.px[(x, y)] = tuple(c)

    def get(self, x, y):
        return self.px.get((x, y))

    def blob(self, mask, base, light=(-0.6, -0.8), flat=False):
        """Fills a set of pixels with the five tones of `base`, lit from the upper left."""
        if not mask:
            return
        R5 = ramp(base)
        xs = [p[0] for p in mask]
        ys = [p[1] for p in mask]
        cx, cy = (min(xs) + max(xs)) / 2.0, (min(ys) + max(ys)) / 2.0
        r = max(max(xs) - min(xs), max(ys) - min(ys)) / 2.0 + 0.8
        best, bestv = None, -9
        tones = {}
        for (x, y) in mask:
            interior = all((x + dx, y + dy) in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            v = -(((x - cx) * light[0] + (y - cy) * light[1]) / r)
            if flat:
                idx = 2
            elif not interior:
                idx = 1 if v < 0.15 else 2
            else:
                idx = 3 if v > 0.30 else 2 if v > -0.30 else 1
            tones[(x, y)] = idx
            if interior and v > bestv:
                best, bestv = (x, y), v
        if best is not None and not flat and len(mask) > 14:
            tones[best] = 4
        for p, idx in tones.items():
            self.px[p] = R5[idx]

    def mark(self, pts, c):
        for (x, y) in pts:
            if (x, y) in self.px:
                self.px[(x, y)] = tuple(c)

    def outline(self):
        out = {}
        for (x, y), c in self.px.items():
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (x + dx, y + dy)
                if q not in self.px and 0 <= q[0] < S and 0 <= q[1] < S:
                    out.setdefault(q, []).append(c)
        for q, cs in out.items():
            avg = tuple(sum(c[i] for c in cs) / len(cs) for i in range(3))
            self.px[q] = mix(shade(avg, 0.22), (22, 14, 40), 0.45)

    def image(self):
        a = np.zeros((S, S, 4), np.uint8)
        for (x, y), c in self.px.items():
            a[y, x] = (c[0], c[1], c[2], 255)
        return Image.fromarray(a, "RGBA")


def _mask(draw_fn):
    im = Image.new("L", (S, S), 0)
    draw_fn(ImageDraw.Draw(im))
    a = np.array(im)
    return {(x, y) for y in range(S) for x in range(S) if a[y, x] > 127}


def ell(x0, y0, x1, y1):
    return _mask(lambda d: d.ellipse([x0, y0, x1, y1], fill=255))


def rect(x0, y0, x1, y1):
    return {(x, y) for y in range(y0, y1 + 1) for x in range(x0, x1 + 1)}


def poly(*pts):
    return _mask(lambda d: d.polygon(list(pts), fill=255))


def line(x0, y0, x1, y1):
    return _mask(lambda d: d.line([x0, y0, x1, y1], fill=255))


def sub(a, b):
    return a - b


def tint(cv, mask, base, t=0.5):
    """Mixes the pixels already drawn inside the mask towards `base`."""
    b = hexrgb(base) if isinstance(base, str) else base
    for p in mask:
        if p in cv.px:
            cv.px[p] = mix(cv.px[p], b, t)


# ================================================================ foods
def steam(cv, x, y, n=2):
    for i in range(n):
        cv.put(x + (i % 2), y - i * 2, (236, 240, 246))
        cv.put(x + (i % 2), y - i * 2 - 1, (200, 208, 222))


def bowl(content, extra=None, vessel="#8a5a3a", dots=(), glow=False):
    cv = Canvas()
    cv.blob(ell(1, 6, 14, 12) | rect(2, 8, 13, 10), vessel)
    cv.blob(poly((3, 12), (12, 12), (11, 14), (4, 14)), vessel)
    top = ell(2, 5, 13, 10)
    cv.blob(top, content)
    if extra:
        cv.mark(dots or [(5, 7), (8, 6), (10, 8), (6, 8)], hexrgb(extra))
    if glow:
        cv.mark([(4, 6), (11, 7), (8, 8)], (200, 255, 240))
    steam(cv, 6, 4, 2)
    steam(cv, 10, 3, 2)
    return cv


def strip(meat, fleck):
    cv = Canvas()
    cv.blob(poly((2, 11), (4, 8), (12, 3), (14, 5), (6, 12), (3, 13)), meat)
    cv.mark([(5, 9), (8, 7), (10, 5), (7, 10)], hexrgb(fleck))
    cv.mark([(12, 4), (13, 5)], mix(hexrgb(meat), (255, 255, 255), 0.4))
    return cv


def hardtack(c):
    cv = Canvas()
    cv.blob(rect(3, 3, 12, 12), c)
    for p in [(5, 5), (7, 5), (9, 5), (5, 7), (7, 7), (9, 7), (5, 9), (7, 9), (9, 9)]:
        cv.px[p] = shade(hexrgb(c), 0.55)
    return cv


def pudding(body, top):
    cv = Canvas()
    cv.blob(poly((3, 13), (13, 13), (12, 6), (4, 6)), body)
    cv.blob(ell(3, 3, 12, 8), top)
    cv.mark([(10, 8), (12, 9), (11, 11)], mix(hexrgb(body), (255, 255, 255), 0.6))
    cv.put(8, 2, mix(hexrgb(top), (255, 255, 255), 0.4))
    return cv


def skewer(items):
    cv = Canvas()
    cv.blob(line(1, 14, 14, 1), "#b88a52", flat=True)
    for i, (c, cap) in enumerate(items):
        x, y = 4 + i * 3, 11 - i * 3
        cv.blob(ell(x - 1, y - 2, x + 3, y + 2) if cap else rect(x - 1, y - 1, x + 2, y + 2), c)
    return cv


def roast(meat, bone="#ece4d0"):
    cv = Canvas()
    cv.blob(ell(3, 2, 13, 11), meat)
    cv.blob(rect(10, 9, 13, 13), bone)
    cv.blob(ell(11, 12, 15, 15), bone)
    cv.mark([(6, 5), (8, 4), (9, 7), (6, 8)], mix(hexrgb(meat), (255, 220, 160), 0.5))
    return cv


def steak(meat, fat):
    cv = Canvas()
    cv.blob(poly((2, 7), (4, 3), (10, 2), (14, 5), (13, 11), (8, 14), (3, 12)), meat)
    cv.mark([(5, 6), (7, 5), (9, 7), (11, 6), (6, 9), (9, 10)], hexrgb(fat))
    return cv


def ball(c, swirl):
    cv = Canvas()
    cv.blob(ell(2, 2, 13, 13), c)
    cv.mark([(5, 5), (6, 7), (8, 8), (9, 10), (7, 9), (10, 6)], hexrgb(swirl))
    return cv


def cake(base, icing, drip):
    cv = Canvas()
    cv.blob(rect(2, 7, 13, 13), base)
    cv.blob(rect(2, 4, 13, 7), icing)
    cv.mark([(3, 8), (4, 9), (7, 8), (8, 10), (11, 8), (12, 9)], hexrgb(drip))
    cv.put(8, 3, hexrgb(drip))
    return cv


def cup(vessel, content, steam_on=True, glow=None):
    cv = Canvas()
    cv.blob(poly((3, 5), (12, 5), (11, 13), (4, 13)), vessel)
    cv.blob(rect(4, 5, 11, 6), content)
    cv.blob(ell(11, 6, 14, 10), vessel)
    cv.px.pop((12, 8), None)
    cv.px.pop((13, 8), None)
    if glow:
        cv.mark([(6, 9), (8, 10), (7, 8)], hexrgb(glow))
    if steam_on:
        steam(cv, 6, 3, 2)
    return cv


def sorbet(c1, c2):
    cv = Canvas()
    cv.blob(poly((4, 9), (11, 9), (8, 15)), "#e8d0a0")
    cv.blob(ell(3, 5, 8, 10), c1)
    cv.blob(ell(7, 5, 12, 10), c2)
    cv.blob(ell(5, 2, 10, 7), mix(hexrgb(c1), (255, 255, 255), 0.4))
    return cv


def cutlet(crumb, garnish):
    cv = Canvas()
    cv.blob(poly((2, 8), (4, 4), (10, 3), (14, 6), (12, 12), (6, 13)), crumb)
    cv.mark([(5, 6), (8, 5), (10, 8), (7, 9), (11, 10), (4, 9)], mix(hexrgb(crumb), (255, 232, 160), 0.6))
    cv.blob(ell(9, 9, 14, 14), garnish)
    return cv


def loaf(c):
    cv = Canvas()
    cv.blob(ell(1, 5, 14, 13), c)
    cv.mark([(5, 7), (7, 6), (9, 7), (11, 8)], mix(hexrgb(c), (255, 235, 190), 0.6))
    cv.mark([(4, 9), (7, 10), (10, 10)], shade(hexrgb(c), 0.6))
    return cv


def pouch(c, bits):
    cv = Canvas()
    cv.blob(ell(2, 5, 13, 14), c)
    cv.blob(rect(5, 3, 10, 6), shade(hexrgb(c), 0.8))
    cv.mark([(4, 3), (11, 3)], mix(hexrgb(c), (255, 255, 255), 0.2))
    for i, p in enumerate([(5, 9), (8, 8), (10, 10), (6, 12), (9, 12)]):
        cv.put(p[0], p[1], hexrgb(bits[i % len(bits)]))
    return cv


def biscuit(c, crack):
    cv = Canvas()
    cv.blob(ell(2, 3, 13, 13), c)
    cv.mark([(5, 6), (6, 7), (7, 8), (9, 9), (10, 10)], hexrgb(crack))
    return cv


def mushroom_soup():
    return bowl("#3a9a9a", "#aaffee", glow=True, dots=[(5, 7), (9, 6)])


def bottle(kind, liquid, glow=None, cork="#a8794a", star=False):
    cv = Canvas()
    gl = "#cfe6f0"
    if kind == "round":
        cv.blob(ell(2, 6, 13, 15), gl)
        cv.blob(ell(3, 8, 12, 14), liquid)
        cv.blob(rect(6, 3, 9, 7), gl)
    elif kind == "tall":
        cv.blob(rect(5, 4, 10, 14), gl)
        cv.blob(rect(6, 7, 9, 13), liquid)
        cv.blob(rect(6, 3, 9, 4), gl)
    elif kind == "flat":
        cv.blob(poly((3, 14), (12, 14), (13, 8), (10, 6), (5, 6), (2, 8)), gl)
        cv.blob(poly((4, 13), (11, 13), (12, 9), (4, 9)), liquid)
        cv.blob(rect(6, 3, 9, 6), gl)
    else:                                           # fluted
        cv.blob(poly((5, 14), (10, 14), (12, 9), (9, 7), (6, 7), (3, 9)), gl)
        cv.blob(poly((5, 13), (10, 13), (11, 10), (4, 10)), liquid)
        cv.blob(rect(7, 3, 8, 7), gl)
    cv.blob(rect(6, 1, 9, 3), cork, flat=True)
    cv.mark([(4, 9), (5, 8)] if kind == "round" else [(6, 8)], (255, 255, 255))
    if glow:
        cv.mark([(7, 11), (8, 12), (6, 12)], hexrgb(glow))
    if star:
        cv.mark([(5, 11), (9, 9), (10, 12)], (255, 248, 200))
    return cv


# ================================================================ materials
def ear(c):
    cv = Canvas()
    cv.blob(poly((2, 12), (3, 6), (7, 2), (12, 4), (13, 9), (9, 13), (5, 14)), c)
    cv.mark([(6, 6), (8, 6), (7, 8), (9, 9)], shade(hexrgb(c), 0.55))
    cv.mark([(4, 10), (5, 11)], mix(hexrgb(c), (255, 200, 200), 0.5))
    return cv


def tooth(c, long_=False):
    cv = Canvas()
    if long_:
        cv.blob(poly((10, 1), (14, 3), (11, 9), (7, 15), (5, 12), (7, 6)), c)
    else:
        cv.blob(poly((4, 3), (11, 3), (12, 8), (9, 14), (7, 14), (4, 8)), c)
        cv.mark([(6, 4), (7, 5)], mix(hexrgb(c), (255, 255, 255), 0.6))
    return cv


def tusk(c):
    cv = Canvas()
    cv.blob(poly((3, 14), (2, 9), (5, 4), (11, 2), (14, 3), (11, 5), (8, 8), (6, 14)), c)
    cv.mark([(4, 11), (5, 8), (7, 5)], shade(hexrgb(c), 0.7))
    return cv


def hide(c, stitch="#3a2a1a"):
    cv = Canvas()
    cv.blob(poly((2, 3), (13, 2), (14, 9), (12, 14), (4, 13), (2, 8)), c)
    cv.mark([(4, 5), (6, 6), (9, 4), (11, 7), (6, 10), (9, 11), (5, 8)], shade(hexrgb(c), 0.72))
    cv.mark([(3, 3), (13, 3)], hexrgb(stitch))
    return cv


def shards(c):
    cv = Canvas()
    cv.blob(poly((2, 14), (4, 6), (7, 3), (8, 12)), c)
    cv.blob(poly((7, 14), (9, 5), (13, 2), (14, 11), (11, 14)), c)
    cv.blob(poly((5, 14), (8, 8), (9, 14)), shade(hexrgb(c), 0.85))
    return cv


def claw(c):
    cv = Canvas()
    cv.blob(poly((3, 2), (6, 2), (12, 8), (14, 14), (9, 11), (4, 6)), c)
    cv.mark([(5, 3), (7, 5), (9, 7)], mix(hexrgb(c), (255, 255, 255), 0.5))
    return cv


def dust_pouch(c, dust):
    cv = Canvas()
    cv.blob(ell(3, 6, 12, 14), c)
    cv.blob(rect(6, 3, 9, 6), shade(hexrgb(c), 0.75))
    cv.mark([(5, 2), (10, 2)], shade(hexrgb(c), 0.6))
    cv.blob(ell(4, 8, 11, 12), dust)
    for p in [(6, 7), (9, 7), (7, 2), (8, 1), (5, 5), (11, 5)]:
        cv.put(p[0], p[1], mix(hexrgb(dust), (255, 255, 255), 0.5))
    return cv


def wisp(c, trail=True):
    cv = Canvas()
    cv.blob(ell(4, 3, 11, 10), c)
    if trail:
        cv.blob(poly((6, 9), (9, 9), (10, 13), (8, 15), (6, 13)), shade(hexrgb(c), 0.9))
    cv.mark([(6, 5), (7, 4)], (255, 255, 255))
    cv.mark([(8, 7), (9, 6)], mix(hexrgb(c), (255, 255, 255), 0.75))
    return cv


def essence(c):
    cv = Canvas()
    cv.blob(poly((6, 2), (10, 2), (11, 6), (9, 8), (12, 12), (9, 15), (5, 12), (7, 8), (4, 6)), c)
    cv.mark([(7, 4), (8, 5)], (235, 245, 255))
    cv.mark([(7, 11), (8, 10)], mix(hexrgb(c), (255, 255, 255), 0.7))
    return cv


def gel(c):
    cv = Canvas()
    cv.blob(ell(2, 5, 13, 14), c)
    cv.blob(ell(5, 2, 10, 7), c)
    cv.mark([(5, 8), (6, 7)], (255, 255, 255))
    cv.mark([(9, 11), (10, 10)], mix(hexrgb(c), (255, 255, 255), 0.55))
    return cv


def core(c, frame="#6a6a74", glow=None):
    cv = Canvas()
    cv.blob(poly((8, 2), (13, 7), (8, 14), (3, 7)), c)
    cv.blob(poly((8, 2), (13, 7), (8, 8), (3, 7)), mix(hexrgb(c), (255, 255, 255), 0.25))
    cv.mark([(7, 4), (8, 5)], (255, 255, 255))
    if frame:
        for p in [(2, 8), (2, 9), (13, 8), (13, 9), (5, 13), (11, 13)]:
            cv.put(p[0], p[1], hexrgb(frame))
    return cv


def cog(c):
    cv = Canvas()
    cv.blob(ell(3, 3, 12, 12), c)
    for p in [(7, 1), (8, 1), (7, 14), (8, 14), (1, 7), (1, 8), (14, 7), (14, 8), (3, 3), (12, 3), (3, 12), (12, 12)]:
        cv.put(p[0], p[1], hexrgb(c))
    for p in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        cv.px.pop(p, None)
    cv.mark([(5, 5), (6, 4)], mix(hexrgb(c), (255, 255, 255), 0.4))
    return cv


def cloth(c, thread="#2a1a3a"):
    cv = Canvas()
    cv.blob(poly((2, 3), (14, 2), (13, 8), (14, 14), (3, 13), (4, 8)), c)
    cv.mark([(5, 5), (8, 5), (11, 5), (6, 9), (9, 9), (7, 12)], hexrgb(thread))
    return cv


def sigil(c, rune):
    cv = Canvas()
    cv.blob(ell(2, 2, 13, 13), c)
    cv.blob(ell(4, 4, 11, 11), shade(hexrgb(c), 0.7))
    cv.mark([(8, 4), (8, 5), (8, 6), (6, 7), (7, 8), (9, 8), (10, 7), (8, 10), (7, 11), (9, 11)], hexrgb(rune))
    return cv


def horn(c):
    cv = Canvas()
    cv.blob(poly((4, 14), (3, 9), (6, 4), (12, 2), (10, 6), (9, 10), (9, 14)), c)
    cv.mark([(6, 8), (7, 5), (9, 4)], mix(hexrgb(c), (255, 255, 255), 0.45))
    return cv


def ember(c):
    cv = Canvas()
    cv.blob(ell(3, 4, 12, 13), "#3a2a2a")
    cv.blob(ell(5, 6, 10, 11), c)
    cv.mark([(7, 8), (8, 8)], (255, 244, 180))
    for p in [(7, 2), (10, 3), (4, 3), (13, 6)]:
        cv.put(p[0], p[1], mix(hexrgb(c), (255, 220, 120), 0.5))
    return cv


def scales(c):
    cv = Canvas()
    for (x, y) in [(2, 3), (7, 3), (4, 7), (9, 7), (6, 11), (11, 11), (1, 11)]:
        cv.blob(poly((x, y), (x + 5, y), (x + 5, y + 2), (x + 2, y + 5), (x, y + 2)), c)
    return cv


def pearl(c):
    cv = Canvas()
    cv.blob(ell(3, 3, 12, 12), c)
    cv.mark([(5, 5), (6, 5), (5, 6)], (255, 255, 255))
    cv.mark([(9, 10), (10, 9)], mix(hexrgb(c), (120, 180, 220), 0.5))
    return cv


def gland(c):
    cv = Canvas()
    cv.blob(ell(3, 5, 12, 14), c)
    cv.blob(rect(6, 2, 9, 6), shade(hexrgb(c), 0.8))
    cv.mark([(5, 8), (6, 8)], mix(hexrgb(c), (180, 190, 255), 0.6))
    cv.mark([(9, 11), (10, 11), (9, 12)], shade(hexrgb(c), 0.55))
    return cv


def chitin(c):
    cv = Canvas()
    cv.blob(poly((3, 5), (8, 2), (13, 5), (13, 11), (8, 14), (3, 11)), c)
    cv.mark([(8, 3), (8, 5), (8, 7), (8, 9), (8, 11), (8, 13)], shade(hexrgb(c), 0.62))
    cv.mark([(5, 6), (6, 5)], mix(hexrgb(c), (255, 255, 255), 0.5))
    return cv


def sac(c):
    cv = Canvas()
    cv.blob(ell(3, 5, 12, 14), c)
    cv.blob(poly((6, 2), (9, 2), (10, 6), (5, 6)), shade(hexrgb(c), 0.85))
    cv.mark([(5, 8), (6, 7)], mix(hexrgb(c), (255, 255, 255), 0.6))
    cv.mark([(8, 11), (9, 10), (7, 12)], (60, 90, 30))
    return cv


def ingot(c, glow="#8a4aff"):
    cv = Canvas()
    cv.blob(poly((2, 11), (4, 6), (13, 6), (14, 11)), c)
    cv.blob(poly((4, 6), (6, 3), (14, 3), (13, 6)), mix(hexrgb(c), (255, 255, 255), 0.18))
    cv.mark([(5, 8), (7, 8), (9, 8), (11, 8)], hexrgb(glow))
    cv.mark([(7, 4), (9, 4)], (210, 190, 255))
    return cv


def bone_arrow():
    cv = Canvas()
    cv.blob(line(2, 14, 12, 4), "#a47a4a", flat=True)
    cv.blob(poly((11, 1), (14, 1), (14, 4), (12, 5), (10, 3)), "#e8dcc0")
    cv.mark([(2, 12), (3, 11), (2, 14), (3, 13)], (240, 240, 235))
    cv.mark([(1, 14), (4, 12)], (210, 210, 205))
    return cv


# ================================================================ armour
def helm(c, trim, visor=None, crest=None):
    cv = Canvas()
    cv.blob(poly((3, 14), (3, 6), (5, 2), (11, 2), (13, 6), (13, 14), (10, 14), (10, 10), (6, 10), (6, 14)), c)
    cv.mark([(5, 11), (5, 12), (11, 11), (11, 12)], hexrgb(trim))
    if visor:
        cv.mark([(5, 8), (6, 8), (7, 8), (8, 8), (9, 8), (10, 8), (11, 8)], hexrgb(visor))
    if crest:
        cv.mark([(8, 1), (8, 2), (7, 3), (9, 3)], hexrgb(crest))
    cv.mark([(5, 4), (6, 3)], mix(hexrgb(c), (255, 255, 255), 0.4))
    return cv


def chest(c, trim, motif):
    cv = Canvas()
    cv.blob(poly((1, 4), (5, 2), (11, 2), (15, 4), (14, 8), (12, 8), (12, 14), (4, 14), (4, 8), (2, 8)), c)
    cv.mark([(7, 2), (8, 2), (7, 3), (8, 3)], shade(hexrgb(c), 0.4))
    for p in motif:
        cv.put(p[0], p[1], hexrgb(trim))
    return cv


def legs(c, trim):
    cv = Canvas()
    cv.blob(poly((3, 2), (13, 2), (13, 8), (11, 14), (9, 14), (8, 7), (7, 7), (6, 14), (4, 14), (3, 8)), c)
    cv.mark([(3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (11, 3), (12, 3)], hexrgb(trim))
    cv.mark([(5, 8), (10, 8)], hexrgb(trim))
    return cv


def boots(c, trim):
    cv = Canvas()
    cv.blob(poly((2, 12), (2, 7), (4, 3), (8, 3), (8, 8), (13, 9), (14, 12), (14, 14), (2, 14)), c)
    cv.mark([(2, 13), (3, 13), (4, 13), (5, 13), (6, 13), (7, 13), (8, 13), (9, 13), (10, 13), (11, 13), (12, 13), (13, 13)], hexrgb(trim))
    cv.mark([(4, 4), (5, 4), (6, 4), (7, 4)], hexrgb(trim))
    return cv


# ================================================================ the table
DRAKE = ("#3f8a56", "#e0b84a")
SHADOW = ("#2f2a44", "#8a5aff")

ICONS = {
    # foods
    "dungeon_ration_stew": lambda: bowl("#8a5030", "#c8a060", dots=[(4, 7), (7, 6), (10, 8), (8, 8)]),
    "goblin_ear_jerky": lambda: strip("#7a2f28", "#e6d0a0"),
    "adventurers_hardtack": lambda: hardtack("#c8a070"),
    "slime_jelly_pudding": lambda: pudding("#58c8d8", "#a8f0ff"),
    "mandrake_stew": lambda: bowl("#6a8a3a", "#c8a0d0", dots=[(5, 7), (8, 6), (10, 8)]),
    "walking_mushroom_skewer": lambda: skewer([("#c84a3a", True), ("#e8c890", True), ("#c84a3a", True)]),
    "orc_haunch_roast": lambda: roast("#9a4a2a"),
    "wyvern_steak": lambda: steak("#b04a3a", "#f0d8c0"),
    "bone_broth": lambda: bowl("#d8c8a0", "#fff4dc", dots=[(5, 7), (9, 6)]),
    "kraken_fritter": lambda: ball("#d8a040", "#2a3a6a"),
    "dryad_honey_cake": lambda: cake("#e8c070", "#f0e0a0", "#e8a020"),
    "frostbite_sorbet": lambda: sorbet("#a8e0f0", "#e0f4ff"),
    "ember_chili": lambda: bowl("#b83a20", "#ffb030", dots=[(5, 7), (8, 6), (10, 8), (7, 8)]),
    "cave_mushroom_soup": mushroom_soup,
    "falna_cutlet": lambda: cutlet("#d8a050", "#a8c850"),
    "hearth_bread": lambda: loaf("#c88a48"),
    "delvers_trail_mix": lambda: pouch("#8a6a40", ["#c85a3a", "#e8d070", "#6a8a3a"]),
    "golem_stone_biscuit": lambda: biscuit("#8a8a92", "#4a4a52"),
    "spirit_tea": lambda: cup("#e8e0d0", "#58d8a0", True, "#c8ffe8"),
    "mana_elixir": lambda: bottle("round", "#3a7aff", "#a8d0ff"),
    "greater_mana_elixir": lambda: bottle("fluted", "#2a50e8", "#90b8ff"),
    "aura_tonic": lambda: bottle("tall", "#e8b030", "#fff0a0"),
    "wisp_draught": lambda: bottle("round", "#38d8d0", "#e0fffc"),
    "starlit_nectar": lambda: bottle("fluted", "#7a40d8", "#d8b8ff", star=True),
    "sages_elixir": lambda: bottle("flat", "#f0e8b0", "#ffffff", cork="#d8b050", star=True),
    "healers_vial": lambda: bottle("tall", "#e03a50", "#ffc0c8"),
    "fortitude_flask": lambda: bottle("flat", "#e8c848", "#fff4a0"),
    "swiftfoot_tonic": lambda: bottle("round", "#58d058", "#d0ffc8"),
    "berserkers_draught": lambda: bottle("fluted", "#901820", "#ff8060"),
    "bulwark_tonic": lambda: bottle("flat", "#6a8aa8", "#d0e4f0"),
    # materials
    "goblin_ear": lambda: ear("#6aa04a"),
    "goblin_tooth": lambda: tooth("#e8dcb0"),
    "orc_tusk": lambda: tusk("#e8e0c8"),
    "troll_hide": lambda: hide("#7a8a6a"),
    "bone_shard": lambda: shards("#e8e0cc"),
    "ghoul_claw": lambda: claw("#5a4a62"),
    "lich_dust": lambda: dust_pouch("#5a4a7a", "#b8a8e8"),
    "wraith_essence": lambda: essence("#78a8d8"),
    "slime_gel": lambda: gel("#58d8c0"),
    "gel_core": lambda: pearl("#3ab8a8"),
    "rusted_cog": lambda: cog("#a0603a"),
    "golem_core": lambda: core("#d8803a", "#6a6a74"),
    "cursed_cloth": lambda: cloth("#4a3a62"),
    "cultist_sigil": lambda: sigil("#8a2a3a", "#ffc860"),
    "imp_horn": lambda: horn("#c83a3a"),
    "shadow_core_shard": lambda: core("#4a2a7a", None),
    "spirit_wisp": lambda: wisp("#78e0a0"),
    "ember_heart": lambda: ember("#ff7a20"),
    "beast_fang": lambda: tooth("#f0e8d0", True),
    "drake_scale": lambda: scales("#3f8a56"),
    "tide_pearl": lambda: pearl("#a8d8e8"),
    "kraken_ink": lambda: gland("#2a3a7a"),
    "chitin_plate": lambda: chitin("#6a4a30"),
    "venom_sac": lambda: sac("#8ac040"),
    "shadowsteel_ingot": lambda: ingot("#3a3652"),
    "bone_arrow": bone_arrow,
    # armour
    "drakescale_helm": lambda: helm(DRAKE[0], DRAKE[1], crest="#e0b84a"),
    "drakescale_vest": lambda: chest(DRAKE[0], DRAKE[1], [(4, 6), (6, 6), (8, 6), (10, 6), (5, 8), (7, 8), (9, 8), (11, 8), (6, 10), (8, 10), (10, 10)]),
    "drakescale_greaves": lambda: legs(DRAKE[0], DRAKE[1]),
    "drakescale_boots": lambda: boots(DRAKE[0], DRAKE[1]),
    "shadowsteel_helm": lambda: helm(SHADOW[0], SHADOW[1], visor=SHADOW[1]),
    "shadowsteel_cuirass": lambda: chest(SHADOW[0], SHADOW[1], [(8, 5), (8, 6), (7, 7), (9, 7), (8, 8), (8, 10), (8, 11)]),
    "shadowsteel_greaves": lambda: legs(SHADOW[0], SHADOW[1]),
    "shadowsteel_sabatons": lambda: boots(SHADOW[0], SHADOW[1]),
}


def make(item_id):
    cv = ICONS[item_id]()
    cv.outline()
    return cv.image()


# ================================================================ armour layers and the effect icon
def armour_layer(set_id, layer):
    """A 64 x 32 vanilla armour layer texture for the places the worn-model library does not draw (a mob, an armour stand): the set's pattern over the whole sheet."""
    base, trim = (DRAKE if set_id == "drakescale" else SHADOW)
    a = np.zeros((32, 64, 4), np.uint8)
    b, t = hexrgb(base), hexrgb(trim)
    rng = np.random.default_rng(7 if set_id == "drakescale" else 11)
    for y in range(32):
        for x in range(64):
            if set_id == "drakescale":
                row = y // 3
                off = 2 if row % 2 else 0
                cx, cy = (x + off) % 4, y % 3
                c = b
                if cx == 0 or cy == 0:
                    c = shade(b, 0.62)
                elif cx == 1 and cy == 1:
                    c = mix(b, (255, 255, 255), 0.25)
                if (y % 8 == 0) and layer == 1:
                    c = t
            else:
                c = b
                if (x + y) % 8 == 0:
                    c = mix(b, t, 0.55)
                if y % 8 == 7:
                    c = shade(b, 0.6)
                if x % 16 == 0:
                    c = mix(b, t, 0.25)
            n = int(rng.integers(-5, 6))
            a[y, x] = (max(0, min(255, c[0] + n)), max(0, min(255, c[1] + n)), max(0, min(255, c[2] + n)), 255)
    return Image.fromarray(a, "RGBA")


def effect_icon():
    """The 18 x 18 icon of the mana_surge effect: a blue-white drop with a rising spark."""
    cv = Canvas()
    img = np.zeros((18, 18, 4), np.uint8)
    base = ramp("#3a7aff")
    d = {(x, y) for y in range(18) for x in range(18) if ((x - 9) ** 2 / 30.0 + (y - 10) ** 2 / 42.0) <= 1.0}
    for (x, y) in d:
        v = -(((x - 9) * -0.6 + (y - 10) * -0.8) / 6.0)
        idx = 3 if v > 0.3 else 2 if v > -0.3 else 1
        img[y, x] = (*base[idx], 255)
    for (x, y) in [(7, 7), (7, 8), (8, 7)]:
        img[y, x] = (255, 255, 255, 255)
    for (x, y) in [(12, 3), (12, 4), (11, 4), (13, 4), (12, 5)]:
        img[y, x] = (200, 235, 255, 255)
    out = img.copy()
    for y in range(18):
        for x in range(18):
            if img[y, x, 3] == 0 and any(0 <= x + dx < 18 and 0 <= y + dy < 18 and img[y + dy, x + dx, 3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out[y, x] = (14, 24, 70, 255)
    return Image.fromarray(out, "RGBA")


def main(argv):
    items = os.path.join(ASSETS, "textures", "item")
    os.makedirs(items, exist_ok=True)
    for iid in ICONS:
        make(iid).save(os.path.join(items, iid + ".png"))
    arm = os.path.join(ASSETS, "textures", "models", "armor")
    os.makedirs(arm, exist_ok=True)
    for s in ("drakescale", "shadowsteel"):
        for layer in (1, 2):
            armour_layer(s, layer).save(os.path.join(arm, "loot_%s_layer_%d.png" % (s, layer)))
    fx = os.path.join(ASSETS, "textures", "mob_effect")
    os.makedirs(fx, exist_ok=True)
    effect_icon().save(os.path.join(fx, "mana_surge.png"))
    print("%d icons, 4 armour layers, 1 effect icon" % len(ICONS))
    if len(argv) >= 2 and argv[0] == "sheet":
        ids = list(ICONS)
        cols = 10
        rows = (len(ids) + cols - 1) // cols
        sheet = Image.new("RGBA", (cols * (S * 6 + 6), rows * (S * 6 + 6)), (58, 60, 72, 255))
        for i, iid in enumerate(ids):
            im = make(iid).resize((S * 6, S * 6), Image.NEAREST)
            sheet.alpha_composite(im, ((i % cols) * (S * 6 + 6) + 3, (i // cols) * (S * 6 + 6) + 3))
        sheet.save(argv[1])
        print("sheet", argv[1])


if __name__ == "__main__":
    main(sys.argv[1:])

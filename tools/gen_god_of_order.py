"""The God of Order (relics/order/GodOfOrder), the arbiter that comes for a second copy of a relic, in the repo's GeoLite pipeline (tools/geo_builder.py ->
client.geo.GeoDraw, drawn by client/relics/order/GodOfOrderRenderer): tall (3.4 blocks), slender, in a white and gold robe with a stole of ledger glyphs, a
shoulder mantle and a long cape, a hood over a porcelain mask (a balance on the brow, two slits of light for eyes, no mouth), a gold crown of prongs, a halo of
ledger glyphs (tally marks, balances, equals bars, ledger lines, numerals, seals) turning behind the shoulders, and a long measuring staff, graduated like a
rule, that ends in a ring with a plumb bob. Original pixel art, nothing is traced.

    python3 -B tools/gen_god_of_order.py                    # everything (idempotent)
    python3 -B tools/gen_god_of_order.py base               # only the named models (base, gates)

Models (GeoSpec.of("god_of_order", "<name>")):
    base     the God while it keeps the ledger
    gates    the same body with the ledger open (100% authority): a second, larger counter-turning ring and six floating pages that orbit it, more gold

Writes (through tools/geo_builder.py):
    assets/nusmp/geo/entity/god_of_order_<name>.geo.json
    assets/nusmp/animations/entity/god_of_order_<name>.animation.json
    assets/nusmp/textures/entity/god_of_order_<name>.png          512 wide at 2 texels per model pixel (the mask, the halo, the staff ring and the pages at 4)
    assets/nusmp/textures/entity/god_of_order_<name>_glow.png     eye slits, glyphs, the brow balance, trims (additive, full bright)
and tools/render_preview/scenes.d/god_of_order.json (python3 -B tools/render_preview/preview.py god_base_front god_gates_read ...; the scenes are named
god_<model>_<front|back|face|idle|walk|run|attack|cast|cast_big|hurt|transform|death|read|beam|bow>).

Clips (the same names on both models): idle walk run attack cast cast_big hurt transform death, the ones client/zogratis/ZogratisPose plays, and read beam bow, the
ones the renderer picks from the synced phase of the visit. Rotation signs of this frame (see tools/geo_builder.py): positive x swings a hanging limb FORWARD,
negative x folds a knee / leans the body forward; positive z swings a hanging limb towards +x (the model's left); positive y turns the head towards the model's
right. Positions are pixels (y up), scales multiply.
"""
import json
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim, ROOT  # noqa: E402
from zogratis_lucius_kit import Pack, Canvas, pal, mix, px, clamp, r5, SIN, COS, TAU, SIDES, FACES, FACE_T, fabric, keys, cyc  # noqa: E402
from spirit_geo import nz, fz, hsh  # noqa: E402
from gen_devil_avatar import pose_clip, loop_clip, spin_track  # noqa: E402

KEY = "god_of_order"
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "god_of_order.json")
MODELS = ("base", "gates")

# ------------------------------------------------------------------------------------------------------------------ palette (dark -> light)
CREAM = ((176, 170, 152), (206, 200, 182), (230, 225, 208), (246, 242, 230), (255, 252, 244))
CREAM_SHADE = ((146, 140, 126), (178, 172, 156), (208, 202, 186), (232, 227, 212))
GOLD = ((70, 50, 10), (122, 88, 16), (180, 136, 28), (226, 184, 56), (250, 224, 112), (255, 243, 188))
SHADOW = ((26, 24, 22), (42, 38, 34), (60, 54, 46), (82, 74, 62))
PORCELAIN = ((196, 192, 182), (224, 220, 210), (242, 238, 228), (255, 253, 247))
WOOD = ((44, 34, 22), (70, 54, 34), (98, 76, 48), (132, 104, 66))
GLOW = (255, 214, 96)
HOT = (255, 246, 210)
PALE = (255, 236, 160)


def glow_px(a, c=GLOW):
    return None if a <= 0 else px(c, int(clamp(a, 0, 255)))


# ------------------------------------------------------------------------------------------------------------------ painters
def lit_fabric(cols, salt):
    """White cloth: vertical pleats quantised into a light palette (the baseline sits high so the robe reads white, not grey), a darker lower edge, a faint weave."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        f = SIN((i + 2.5 * SIN(j * 0.12 + salt)) * TAU / 7.0 + salt * 1.3)
        t = 0.74 + 0.20 * f * (0.7 + 0.3 * SIN(j * 0.07 + salt * 2)) - 0.16 * v + nz(i * 0.35, j * 0.35, salt) * 0.12 + FACE_T[face] * 0.45
        t += 0.02 if (i + j) % 2 == 0 else -0.02
        if j == 0 and face in SIDES:
            return px(cols[0]), None
        return px(pal(cols, t, i, j, 0.04)), None
    return p


def cloth(salt=1, cols=CREAM, hem=0, trim="", shade=False, glyphs=0.0):
    """White robe cloth: pleats, a gold hem of `hem` texels, gold edge lines (trim = 'l', 'r', 't'), an optional faint row of glyph dots (glyphs = density)."""
    base = lit_fabric(CREAM_SHADE if shade else cols, salt)

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face not in SIDES:
            return c, g
        if hem and j >= fh - hem:
            top = j == fh - hem
            col = pal(GOLD, 0.82 if top else 0.50 + 0.18 * hsh(i // 2, salt, 4), i, j, 0.0)
            return px(col), (glow_px(120) if top else None)
        edge = ("l" in trim and i == 1) or ("r" in trim and i == fw - 2) or ("t" in trim and j == 1)
        if edge and fw > 4 and fh > 4:
            return px(pal(GOLD, 0.62, i, j, 0.0)), glow_px(60)
        if glyphs > 0 and j > 3 and j < fh - 3 - hem and 3 < i < fw - 4 and (i % 5 == 2) and (j % 6 == 3) and hsh(i, j, salt + 7) < glyphs:
            return px(pal(GOLD, 0.7, i, j, 0.0)), glow_px(90)
        return c, g
    return p


def gold(salt=2, glow=40, band=True):
    """Worked gold: lit top rows, dark lower rows, a bright rim, fine engraved dashes."""
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.80 - 0.50 * v + FACE_T[face] * 0.5 + 0.07 * fz(i, j, salt)
        if face == "top":
            t = 0.95
        if (i == 0 or j == 0) and fw > 3 and fh > 3:
            t += 0.14
        if (i == fw - 1 or j == fh - 1) and fw > 3 and fh > 3:
            t -= 0.20
        if band and fw >= 6 and fh >= 5 and face in SIDES and j == fh // 2 and i % 4 != 3:
            t -= 0.16
        return px(pal(GOLD, t, i, j, 0.04)), glow_px(glow * (0.5 + 0.5 * (1 - v)))
    return p


def dark(salt=3):
    def p(face, i, j, fw, fh):
        t = 0.45 + 0.20 * nz(i * 0.3, j * 0.3, salt) - 0.2 * j / max(1, fh - 1) + FACE_T[face] * 0.5
        return px(pal(SHADOW, t, i, j, 0.05)), None
    return p


def glove(salt=4):
    def p(face, i, j, fw, fh):
        t = 0.62 + 0.10 * nz(i * 0.4, j * 0.4, salt) - 0.18 * j / max(1, fh - 1) + FACE_T[face] * 0.5
        return px(pal(CREAM, t, i, j, 0.05)), None
    return p


def staff_pole(salt=5):
    """The measuring staff's pole: dark wood with a gold-lit rule down the front, a tick every 2 texels and a long tick every 10, numerals as dots."""
    def p(face, i, j, fw, fh):
        t = 0.50 + 0.14 * nz(i * 0.5, j * 0.12, salt) + FACE_T[face] * 0.6
        c = pal(WOOD, t, i, j, 0.05)
        g = None
        if face in ("front", "left", "right", "back"):
            if j % 8 == 0 and i < fw - 1:
                c = pal(GOLD, 0.7, i, j, 0.0)
                g = glow_px(110)
            if j % 40 == 0:
                c = pal(GOLD, 0.9, i, j, 0.0)
                g = glow_px(200)
                if i < fw:
                    pass
        return px(c), g
    return p


# ------------------------------------------------------------------------------------------------------------------ glyphs of the Order
def ledger_glyph(cv, cx, cy, kind, r, col=GLOW, hot=HOT):
    """One glyph of the Order's script about (cx, cy) in a cell of radius r: 0 tally strokes struck through, 1 a balance, 2 an equals bar over a dot, 3 ledger
    lines behind a dot, 4 numerals (I II V), 5 a seal (ring with a cross); drawn in the glow colour on both layers."""
    def dot(x, y, c=col):
        cv.put(round(x), round(y), c, 255, c, 255)

    def ln(x0, y0, x1, y1, c=col):
        cv.line(x0, y0, x1, y1, c, 255, c, 255)
    if kind == 0:
        for k in range(4):
            x = cx - r * 0.6 + k * r * 0.4
            ln(x, cy - r * 0.7, x, cy + r * 0.7)
        ln(cx - r * 0.8, cy + r * 0.5, cx + r * 0.8, cy - r * 0.5, hot)
    elif kind == 1:
        ln(cx, cy - r * 0.8, cx, cy + r * 0.8)
        ln(cx - r * 0.9, cy - r * 0.4, cx + r * 0.9, cy - r * 0.4)
        for sx in (-1, 1):
            ln(cx + sx * r * 0.9, cy - r * 0.4, cx + sx * r * 0.9, cy + r * 0.1)
            for a in range(0, 181, 30):
                dot(cx + sx * r * 0.9 + math.cos(math.radians(a)) * r * 0.45, cy + r * 0.1 + math.sin(math.radians(a)) * r * 0.35)
        ln(cx - r * 0.5, cy + r * 0.8, cx + r * 0.5, cy + r * 0.8)
    elif kind == 2:
        ln(cx - r * 0.8, cy - r * 0.2, cx + r * 0.8, cy - r * 0.2)
        ln(cx - r * 0.8, cy + r * 0.3, cx + r * 0.8, cy + r * 0.3)
        dot(cx, cy - r * 0.75, hot)
        dot(cx + 1, cy - r * 0.75, hot)
    elif kind == 3:
        dot(cx - r * 0.8, cy - r * 0.6, hot)
        for k in range(3):
            y = cy - r * 0.6 + k * r * 0.6
            ln(cx - r * 0.4, y, cx + r * (0.9 - 0.2 * k), y)
    elif kind == 4:
        ln(cx - r * 0.7, cy - r * 0.7, cx - r * 0.7, cy + r * 0.7)
        ln(cx - r * 0.2, cy - r * 0.7, cx - r * 0.2, cy + r * 0.7)
        ln(cx + r * 0.2, cy - r * 0.7, cx + r * 0.6, cy + r * 0.7)
        ln(cx + r * 1.0, cy - r * 0.7, cx + r * 0.6, cy + r * 0.7)
    else:
        for a in range(0, 360, 24):
            dot(cx + math.cos(math.radians(a)) * r * 0.85, cy + math.sin(math.radians(a)) * r * 0.85)
        ln(cx - r * 0.5, cy, cx + r * 0.5, cy)
        ln(cx, cy - r * 0.5, cx, cy + r * 0.5)
        dot(cx, cy, hot)


def halo_canvas(size, gates, outer=False):
    """A ring of ledger glyphs (size x size texels): a thin outer rim and an inner rim of gold, between them twelve glyphs that burn through the glow layer, tick
    marks on the inner edge, a hollow middle (transparent: the head shows through). The glyphs repeat every four cells, so the ring can turn a third in a loop
    and not show it. outer=True: a bigger, plainer ring of the open ledger (eight larger glyphs)."""
    cv = Canvas(size, size)
    c = (size - 1) / 2.0
    ro, ri, rin = size * 0.495, size * 0.425, size * 0.325
    rk = size * 0.285
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            col = None
            if ri <= d <= ro:
                k = 0.42 + 0.14 * nz(x * 0.4, y * 0.4, 12)
                if d > ro - 1.6 or d < ri + 1.0:
                    k = 0.9
                col = pal(GOLD, k, x, y, 0.05)
            elif rin <= d < ri:
                col = pal(GOLD, 0.34 + 0.12 * nz(x * 0.4, y * 0.4, 14), x, y, 0.05)
            elif rk <= d < rin - 0.5:
                ang = math.degrees(math.atan2(y - c, x - c)) % 360.0
                if abs((ang % (360.0 / (24 if not outer else 12)))) < 1.4 or d > rin - 2.5:
                    col = pal(GOLD, 0.5, x, y, 0.0)
            if col is not None:
                cv.put(x, y, col)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if abs(d - (ro - 0.6)) < 0.6 or abs(d - (ri + 0.5)) < 0.55 or abs(d - rin) < 0.55:
                cv.put(x, y, GLOW, 255, GLOW, 200)
    n = 12 if not outer else 8
    pattern = (1, 0, 3, 5) if not outer else (1, 5)
    for k in range(n):
        a = math.radians(360.0 * k / n - 90.0)
        gx, gy = c + math.cos(a) * (ro + ri) / 2.0, c + math.sin(a) * (ro + ri) / 2.0
        ledger_glyph(cv, gx, gy, pattern[k % len(pattern)], (ro - ri) * 0.36)
    ticks = 60 if not outer else 36
    for k in range(ticks):
        a = math.radians(360.0 * k / ticks)
        r0, r1 = rin + 1.0, rin + (3.0 if k % 5 == 0 else 1.6)
        cv.line(c + math.cos(a) * r0, c + math.sin(a) * r0, c + math.cos(a) * r1, c + math.sin(a) * r1, GLOW, 255, GLOW, 255)
    return cv


def page_canvas():
    """A floating ledger page (5 x 7 px at 4 texels = 20 x 28): cream paper, a gold border, six lines of script that burn."""
    cv = Canvas(20, 28)
    for y in range(28):
        for x in range(20):
            edge = x in (0, 19) or y in (0, 27)
            cv.put(x, y, pal(GOLD, 0.8 if edge else 0.2, x, y, 0.0) if edge else pal(CREAM, 0.72 + 0.1 * nz(x * 0.5, y * 0.5, 9), x, y, 0.05),
                   255, GLOW if edge else None, 150)
    for k in range(6):
        y = 5 + k * 3
        n = 11 if k else 7
        for x in range(3, 3 + n):
            if hsh(x, k, 21) > 0.25:
                cv.put(x, y, GLOW, 255, GLOW, 230)
    return cv


def mask_canvas():
    """The mask's front (6 x 8 px at 4 texels = 24 x 32): smooth porcelain, a seam down the middle, a balance on the brow in gold, two slits of light for eyes
    under a straight brow line, a single gold line for the nose that ends in a small seal, and nothing where a mouth would be."""
    cv = Canvas(24, 32)
    for y in range(32):
        for x in range(24):
            t = 0.72 + 0.08 * nz(x * 0.3, y * 0.3, 14) - 0.14 * (y / 31.0) - 0.10 * (abs(x - 11.5) / 11.5) ** 2
            cv.put(x, y, pal(PORCELAIN, t, x, y, 0.05))
    for y in range(32):                                               # the seam
        cv.put(11, y, PORCELAIN[1])
        cv.put(12, y, PORCELAIN[3] if y % 2 else PORCELAIN[2])
    ledger_glyph(cv, 11.5, 6.0, 1, 3.4, GLOW, HOT)                    # the balance on the brow
    cv.line(3, 12, 20, 12, GOLD[3], 255, GLOW, 120)                   # the brow line
    for (x0, x1) in ((3, 9), (14, 20)):                               # the eyes: slits of light in dark sockets
        for y in range(14, 19):
            for x in range(x0, x1 + 1):
                cv.put(x, y, SHADOW[1] if 15 <= y <= 17 else SHADOW[2])
        for x in range(x0 + 1, x1):
            cv.put(x, 15, HOT, 255, HOT, 255)
            cv.put(x, 16, GLOW, 255, GLOW, 255)
        cv.put(x0, 16, GLOW, 255, GLOW, 200)
        cv.put(x1, 16, GLOW, 255, GLOW, 200)
    cv.line(11.5, 18, 11.5, 25, GOLD[3], 255, GLOW, 110)              # the nose line
    cv.line(12.5, 18, 12.5, 25, GOLD[2], 255)
    ledger_glyph(cv, 11.5, 28.0, 5, 1.8, GLOW, HOT)                   # the seal where the mouth would be
    return cv


def buckle_canvas():
    """The belt buckle (3.2 x 3.2 px at 4 texels = 13 x 13): a gold plate with a small balance."""
    cv = Canvas(13, 13)
    for y in range(13):
        for x in range(13):
            edge = x in (0, 12) or y in (0, 12)
            cv.put(x, y, pal(GOLD, 0.9 if edge else 0.55 + 0.1 * nz(x * 0.6, y * 0.6, 3), x, y, 0.0))
    ledger_glyph(cv, 6.0, 6.0, 1, 3.6, HOT, HOT)
    return cv


def stole_canvas(w, h):
    """The tabard's front (4.5 x 38 px at 4 texels = 18 x 152): cream with gold edges and a column of ledger glyphs, one every 15 texels, between gold bars."""
    cv = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            edge = x in (0, w - 1) or (x in (1, w - 2) and y % 2 == 0)
            cv.put(x, y, pal(GOLD, 0.82 if x in (0, w - 1) else 0.55, x, y, 0.0) if edge else pal(CREAM, 0.80 + 0.08 * nz(x * 0.5, y * 0.3, 7), x, y, 0.05),
                   255, GLOW if edge else None, 70)
    cell = 15
    for k in range(h // cell):
        y0 = k * cell
        for x in range(3, w - 3):
            cv.put(x, y0, GOLD[3], 255, GLOW, 90)
        ledger_glyph(cv, (w - 1) / 2.0, y0 + cell / 2.0 + 0.5, (k * 2 + 1) % 5, 4.0, GLOW, HOT)
    for y in range(h - 5, h):
        for x in range(w):
            cv.put(x, y, pal(GOLD, 0.7 if y == h - 5 else 0.5, x, y, 0.0), 255, GLOW if y == h - 5 else None, 140)
    return cv


def cape_canvas(w, h):
    """The cape's back (11 x 24 px at 4 texels = 44 x 96): cream with a gold hem and edges, and a tall ledger down the middle: a page of columns of script
    under a great balance."""
    cv = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            f = SIN((x + 2.0 * SIN(y * 0.1)) * TAU / 11.0)
            t = 0.60 + 0.20 * f - 0.14 * y / h + 0.12 * nz(x * 0.3, y * 0.3, 5)
            cv.put(x, y, pal(CREAM, t, x, y, 0.045))
    for y in range(h):
        for x in (0, 1, w - 2, w - 1):
            cv.put(x, y, pal(GOLD, 0.65 if x in (0, w - 1) else 0.8, x, y, 0.0), 255, GLOW, 90)
    for y in range(h - 6, h):
        for x in range(w):
            cv.put(x, y, pal(GOLD, 0.8 if y == h - 6 else 0.5 + 0.1 * (x % 2), x, y, 0.0), 255, GLOW if y == h - 6 else None, 150)
    cx = (w - 1) / 2.0
    ledger_glyph(cv, cx, 14, 1, 9.0, GLOW, HOT)                       # the great balance
    for y in range(27, h - 12, 2):                                    # columns of script: short bold dashes
        for col in (-9, -3, 3, 9):
            n = 2 + int(hsh(y, col, 31) * 3)
            for d in range(n):
                cv.put(cx + col + d - 1, y, GLOW, 255, GLOW, 235)
                cv.put(cx + col + d - 1, y + 1, GOLD[3], 255, GLOW, 120)
    cv.line(cx - 12, 24, cx + 12, 24, GOLD[3], 255, GLOW, 170)
    cv.line(cx - 12, h - 14, cx + 12, h - 14, GOLD[3], 255, GLOW, 170)
    return cv


def ring_canvas(size):
    """The ring at the head of the staff (9 x 9 px at 4 texels = 36 x 36): a gold ring with a hollow middle, glyph nicks round it and a glow along the inner edge."""
    cv = Canvas(size, size)
    c = (size - 1) / 2.0
    ro, ri = size * 0.5, size * 0.33
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if ri <= d <= ro:
                k = 0.45 + 0.1 * nz(x * 0.5, y * 0.5, 8)
                if d > ro - 1.4 or d < ri + 1.0:
                    k = 0.92
                cv.put(x, y, pal(GOLD, k, x, y, 0.04))
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if abs(d - (ri + 0.4)) < 0.7:
                cv.put(x, y, GLOW, 255, GLOW, 220)
    for k in range(8):
        a = math.radians(45 * k)
        r = (ro + ri) / 2.0
        cv.put(c + math.cos(a) * r, c + math.sin(a) * r, HOT, 255, HOT, 255)
    return cv


def canvas_on(canvas_fn, face_name, fallback):
    """A painter that reads a canvas on one face and falls back on the others; texels the canvas leaves clear fall back too."""
    cache = {}

    def p(face, i, j, fw, fh):
        if "c" not in cache:
            cache["c"] = canvas_fn()
        if face == face_name:
            c, g = cache["c"].at(i, j)
            if c is not None or g is not None:
                if c is None:
                    return fallback(face, i, j, fw, fh)[0], g
                return c, g
        return fallback(face, i, j, fw, fh)
    return p


def plate_of(canvas_fn):
    """A painter for a thin plate: the canvas on the front face and, mirrored, on the back face; every other face stays clear."""
    cache = {}

    def p(face, i, j, fw, fh):
        if "c" not in cache:
            cache["c"] = canvas_fn()
        cv = cache["c"]
        if face == "front":
            return cv.at(i, j)
        if face == "back":
            return cv.at(fw - 1 - i, j)
        return None, None
    return p


# ------------------------------------------------------------------------------------------------------------------ the build helper
class Build:
    """A model under construction: bones, boxes (with mirrored pairs), animation."""

    def __init__(self, name, tw=512, th=1024, dens=2):
        self.m = Model(KEY, name, tw, th)
        self.pk = Pack(tw, th, dens)
        self.A = Anim()
        self.name = name

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        self.m.bone(name, parent, pivot, rotation)

    def box(self, bone, origin, size, paint, **kw):
        return self.pk.box(self.m, bone, origin, size, paint, **kw)

    def pair(self, bone_l, bone_r, origin, size, paint, **kw):
        """origin is the +x cube; its mirrored twin (x -> -x) goes on bone_r."""
        c = self.pk.box(self.m, bone_l, origin, size, paint, **kw)
        self.pk.twin(self.m, bone_r, c)
        return c

    def finish(self):
        self.pk.finish()
        return self.pk.images()

    def save_geo(self):
        used = self.pk.height_used()
        hh = 64
        while hh < used:
            hh += 64
        self.m.tex_h = hh
        self.m.save(self.A)
        return hh

    def save_textures(self, img, glow, hh):
        img, glow = img.crop((0, 0, self.pk.W, hh)), glow.crop((0, 0, self.pk.W, hh))
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        img.save(os.path.join(ROOT, "textures", "entity", self.m.id + ".png"))
        glow.save(os.path.join(ROOT, "textures", "entity", self.m.id + "_glow.png"))


# ------------------------------------------------------------------------------------------------------------------ the body
def build_body(b, gates):
    """Bones and boxes of the God. Heights in pixels, feet at 0, the front at -z, +x the model's left; 16 pixels are a block."""
    robe = cloth(1, hem=3, trim="")
    robe_b = cloth(2, hem=3, shade=True)
    robe_plain = cloth(3, hem=0)
    inner = dark(4)
    bones = [("root", None, (0, 0, 0)), ("hips", "root", (0, 26, 0)), ("body", "hips", (0, 28, 0)), ("head", "body", (0, 42.5, 0)),
             ("halo", "body", (0, 47, 8.0)),
             ("arm_l", "body", (5.4, 40, 0)), ("forearm_l", "arm_l", (5.4, 31, 0)), ("hand_l", "forearm_l", (5.4, 22, 0)),
             ("arm_r", "body", (-5.4, 40, 0)), ("forearm_r", "arm_r", (-5.4, 31, 0)), ("hand_r", "forearm_r", (-5.4, 22, 0)),
             ("leg_l", "root", (2.0, 25, 0)), ("leg_r", "root", (-2.0, 25, 0)),
             ("skirt_f", "hips", (0, 15.5, -3.2)), ("skirt_b", "hips", (0, 15.5, 3.2)), ("skirt_l", "hips", (4.4, 15.5, 0)), ("skirt_r", "hips", (-4.4, 15.5, 0)),
             ("stole", "body", (0, 41, -2.8)), ("mantle", "body", (0, 41, 0)), ("cape", "mantle", (0, 37, 3.4)),
             ("staff", "hand_r", (-5.4, 22, 0))]
    if gates:
        bones += [("halo_b", "body", (0, 47, 9.6)), ("pages", "body", (0, 47, 10.6))]
    rest = {"skirt_f": (11, 0, 0), "skirt_b": (-11, 0, 0), "skirt_l": (0, 0, 11), "skirt_r": (0, 0, -11), "stole": (9, 0, 0)}
    for n, par, pv in bones:
        b.bone(n, par, pv, rest.get(n))
    # ---- legs and feet, hidden by the robe
    b.pair("leg_l", "leg_r", (0.8, 1.2, -1.2), (2.4, 24.0, 2.4), inner)
    b.pair("leg_l", "leg_r", (0.5, 0.0, -3.2), (3.0, 1.6, 4.6), gold(5, 20, band=False))
    # ---- the robe: a solid core, the hips, and four panels that flare to the hem
    b.box("hips", (-5.2, 2.4, -3.4), (10.4, 12.4, 6.8), inner)
    b.box("hips", (-4.6, 14.0, -3.2), (9.2, 12.5, 6.4), robe_plain)
    b.box("skirt_f", (-5.6, 1.0, -4.0), (11.2, 14.8, 0.9), robe, faces=("front", "back", "left", "right", "bottom"))
    b.box("skirt_b", (-5.6, 1.0, 3.1), (11.2, 14.8, 0.9), robe_b, faces=("front", "back", "left", "right", "bottom"))
    b.box("skirt_l", (4.1, 1.0, -3.4), (0.9, 14.8, 6.8), robe, faces=("front", "back", "left", "right", "bottom"))
    b.box("skirt_r", (-5.0, 1.0, -3.4), (0.9, 14.8, 6.8), robe, faces=("front", "back", "left", "right", "bottom"))
    # ---- belt and buckle
    b.box("hips", (-4.8, 25.0, -2.9), (9.6, 2.4, 5.8), gold(6, 50))
    b.box("hips", (-1.7, 24.7, -3.4), (3.4, 3.2, 0.6), canvas_on(buckle_canvas, "front", gold(7, 60)), dens={"front": 4}, faces=("front", "top", "bottom", "left", "right"))
    # ---- torso: the bodice, the collar, the stole, the mantle and the cape
    b.box("body", (-4.2, 27.4, -2.5), (8.4, 13.2, 5.0), robe_plain)
    b.box("body", (-3.7, 40.2, -2.9), (7.4, 1.5, 5.8), gold(8, 60))
    b.box("body", (-1.3, 41.5, -1.3), (2.6, 1.4, 2.6), glove(9))
    st = lambda: stole_canvas(18, 152)
    b.box("stole", (-2.25, 2.5, -4.9), (4.5, 38.0, 0.7), canvas_on(st, "front", robe_plain), dens={"front": 4}, faces=("front", "back", "left", "right", "bottom"))
    b.box("mantle", (-7.2, 36.4, -3.5), (14.4, 5.2, 7.0), cloth(10, hem=0, trim="t"), faces=FACES)
    b.box("mantle", (-7.4, 36.0, -3.7), (14.8, 1.0, 7.4), gold(11, 40))
    for sx in (-1, 1):                                                   # shoulder plates
        b.box("mantle", (4.0 if sx > 0 else -8.4, 40.6, -3.0), (4.4, 1.6, 6.0), gold(12, 50))
    ca = lambda: cape_canvas(44, 96)
    b.box("cape", (-5.5, 13.5, 3.0), (11.0, 24.0, 0.9), canvas_on(ca, "back", cloth(13, hem=0)), dens={"back": 4}, faces=("front", "back", "left", "right", "bottom"))
    # ---- arms: upper arm, the wide sleeve, the gold cuff, the glove
    b.pair("arm_l", "arm_r", (3.4, 31.0, -1.8), (4.0, 9.8, 3.6), robe_plain)
    b.pair("forearm_l", "forearm_r", (3.0, 21.8, -2.2), (4.8, 9.6, 4.4), cloth(14, hem=0, trim="l"))
    b.pair("forearm_l", "forearm_r", (2.8, 21.0, -2.4), (5.2, 1.6, 4.8), gold(15, 60))
    b.pair("hand_l", "hand_r", (4.2, 17.6, -1.4), (2.4, 3.8, 2.8), glove(16))
    b.pair("hand_l", "hand_r", (4.3, 17.2, -1.3), (2.2, 0.8, 2.6), gold(17, 80, band=False))
    # ---- the head: the dark inside of the hood, the mask, the hood, its gold rim, the crown
    b.box("head", (-3.4, 42.6, -3.4), (6.8, 8.8, 6.8), inner)
    b.box("head", (-3.0, 43.0, -4.1), (6.0, 8.0, 1.0), canvas_on(mask_canvas, "front", glove(18)), dens={"front": 4})
    b.box("head", (-4.1, 42.2, -3.8), (8.2, 9.6, 8.0), cloth(19, hem=0, trim=""), faces=("top", "left", "right", "back", "bottom"))
    b.box("head", (-4.3, 42.0, -4.3), (1.0, 9.6, 1.2), gold(21, 40, band=False))
    b.box("head", (3.3, 42.0, -4.3), (1.0, 9.6, 1.2), gold(22, 40, band=False))
    b.box("head", (-4.3, 51.2, -4.3), (8.6, 1.0, 8.6), gold(23, 40), faces=("top", "front", "left", "right"))
    for k, (x, ln) in enumerate(((-3.2, 3.0), (-1.6, 4.6), (0.0, 6.4), (1.6, 4.6), (3.2, 3.0))):          # the crown: five prongs, the middle the tallest
        lean = (x / 3.2) * 12.0
        b.box("head", (x - 0.35, 52.2, -0.4), (0.7, ln, 0.7), gold(24 + k, 90, band=False), rotation=(0, 0, -lean), pivot=(x, 52.2, 0), key="prong%d" % k)
        b.box("head", (x - 0.6, 52.2 + ln, -0.6), (1.2, 1.0, 1.2), gold(34, 140, band=False), rotation=(0, 0, -lean), pivot=(x, 52.2, 0), key="bead%d" % k)
    # ---- the staff: the pole, its collars, the ring and the plumb
    b.box("staff", (-5.9, 0.0, -0.5), (1.0, 56.0, 1.0), staff_pole(30), dens={"front": 4, "back": 4, "left": 4, "right": 4})
    for y in (4.0, 17.0, 26.0, 36.0, 48.0):
        b.box("staff", (-6.4, y, -1.0), (2.0, 1.2, 2.0), gold(31, 90))
    b.box("staff", (-9.9, 56.0, -0.3), (9.0, 9.0, 0.6), plate_of(lambda: ring_canvas(36)), dens={"front": 4, "back": 4}, faces=("front", "back"))
    b.box("staff", (-6.2, 58.2, -0.6), (1.6, 3.2, 1.2), gold(33, 180, band=False))
    b.box("staff", (-5.8, 56.6, -0.4), (0.8, 1.8, 0.8), gold(34, 200, band=False))
    # ---- the halo: the ring of ledger glyphs and a crown of blades round it
    b.box("halo", (-14.0, 33.0, 7.5), (28.0, 28.0, 0.9), plate_of(lambda: halo_canvas(112, gates)), dens={"front": 4, "back": 4}, faces=("front", "back"))
    for k in range(12):
        th = 360.0 * k / 12.0
        bx, by = 13.6 * math.sin(math.radians(th)), 47.0 + 13.6 * math.cos(math.radians(th))
        b.box("halo", (bx - 0.5, by, 7.6), (1.0, 2.6 + (1.4 if k % 3 == 0 else 0), 0.7), gold(40 + k % 3, 120, band=False), rotation=(0, 0, -th), pivot=(bx, by, 7.95),
              key="blade%d" % (k % 3))
    if gates:
        b.box("halo_b", (-19.0, 28.0, 9.4), (38.0, 38.0, 0.8), plate_of(lambda: halo_canvas(152, gates, outer=True)), dens={"front": 4, "back": 4}, faces=("front", "back"))
        for k in range(6):
            th = 360.0 * k / 6.0 + 30.0
            px_, py_ = 22.0 * math.sin(math.radians(th)), 47.0 + 22.0 * math.cos(math.radians(th))
            b.box("pages", (px_ - 2.5, py_ - 3.5, 10.4), (5.0, 7.0, 0.5), plate_of(page_canvas), dens={"front": 4, "back": 4}, faces=("front", "back"),
                  rotation=(0, 0, -th), pivot=(px_, py_, 10.65), key="page")
    return b


# ------------------------------------------------------------------------------------------------------------------ animation
def add_clips(b, gates):
    """Every clip of the God on this model. The clip names are the ones client/zogratis/ZogratisPose plays, plus read, beam and bow."""
    A = b.A
    PANELS = ("skirt_f", "skirt_b", "skirt_l", "skirt_r")

    def spin(clip, L, deg):
        spin_track(A, clip, "halo", L, deg)
        if gates:
            spin_track(A, clip, "halo_b", L, -deg * 1.5)
            spin_track(A, clip, "pages", L, deg * 2.0)

    # ------------------------------------------------------------------ idle: 6 s, three slow breaths, the halo a third of a turn, the cape and the panels drifting
    L = 6.0
    t = {}
    t["hips:p"] = lambda th: (0.0, 0.0, 0.0)
    t["body:p"] = lambda th: (0.0, 0.45 * SIN(th), 0.0)
    t["body:r"] = lambda th: (1.0 * SIN(th + 0.6), 2.0 * SIN(th * 0.34), 0.7 * SIN(th * 0.5))
    t["head:r"] = lambda th: (-1.5 * SIN(th) + 1.5, 7.0 * SIN(th * 0.34 + 0.4), 0.0)
    t["arm_l:r"] = lambda th: (4.0 * SIN(th + 0.3) + 3.0, 0.0, 8.0 + 1.5 * SIN(th))
    t["forearm_l:r"] = lambda th: (8.0 + 3.0 * SIN(th + 0.8), 0.0, 3.0)
    t["hand_l:r"] = lambda th: (-6.0, 0.0, 6.0 * SIN(th))
    t["arm_r:r"] = lambda th: (6.0 + 1.5 * SIN(th + 0.3), 0.0, -4.0)
    t["forearm_r:r"] = lambda th: (8.0, 0.0, 0.0)
    t["staff:r"] = lambda th: (-(14.0 + 1.5 * SIN(th + 0.3)), 0.0, 4.0)                  # the staff stays upright in the hand while the arm sways
    t["cape:r"] = lambda th: (3.0 * SIN(th + 0.5) + 2.0, 0.0, 0.0)
    t["mantle:r"] = lambda th: (1.0 * SIN(th + 0.2), 0.0, 0.0)
    t["stole:r"] = lambda th: (1.2 * SIN(th + 0.9), 0.0, 0.0)
    t["skirt_f:r"] = lambda th: (2.0 * SIN(th + 0.4), 0.0, 0.0)
    t["skirt_b:r"] = lambda th: (-2.0 * SIN(th + 0.8), 0.0, 0.0)
    t["skirt_l:r"] = lambda th: (0.0, 0.0, 1.6 * SIN(th + 1.2))
    t["skirt_r:r"] = lambda th: (0.0, 0.0, -1.6 * SIN(th + 1.6))
    t["halo:p"] = lambda th: (0.0, 0.6 * SIN(th + 1.0), 0.0)
    if gates:
        t["halo_b:p"] = lambda th: (0.0, -0.6 * SIN(th + 0.4), 0.0)
        t["pages:p"] = lambda th: (0.0, 0.5 * SIN(th * 2 + 0.4), 0.0)
    loop_clip(A, "idle", L, 3, 48, {k: v for k, v in t.items() if k.split(":")[0] in b.m.bones})
    spin("idle", L, 120.0)

    # ------------------------------------------------------------------ walk: 1.2 s, a long unhurried stride, the staff planted every step
    def stride(L_, leg_amp, arm_amp, lean, bob, stream, tail):
        d = {}
        for sd, nm in ((1, "l"), (-1, "r")):
            ph = 0.0 if sd > 0 else math.pi
            d["leg_%s:r" % nm] = (lambda ph: lambda th: (leg_amp * SIN(th + ph), 0.0, 0.0))(ph)
        d["hips:p"] = lambda th: (0.0, -bob * COS(2 * th), 0.0)
        d["hips:r"] = lambda th: (0.0, -3.0 * SIN(th), 0.0)
        d["body:r"] = lambda th: (-lean, 3.0 * SIN(th), 1.0 * SIN(th))
        d["head:r"] = lambda th: (lean * 0.8 + 1.0, -2.0 * SIN(th), 0.0)
        d["arm_l:r"] = lambda th: (-arm_amp * SIN(th) + 3.0, 0.0, 8.0)
        d["forearm_l:r"] = lambda th: (10.0 + 0.4 * arm_amp * (1 + SIN(th + 1.0)), 0.0, 2.0)
        d["arm_r:r"] = lambda th: (arm_amp * 0.35 * SIN(th) + 8.0, 0.0, -4.0)
        d["forearm_r:r"] = lambda th: (8.0, 0.0, 0.0)
        d["staff:r"] = lambda th: (-(arm_amp * 0.35 * SIN(th) + 16.0), 0.0, 4.0)
        d["cape:r"] = lambda th: (stream + 4.0 * SIN(th - 0.8), 0.0, 0.0)
        d["mantle:r"] = lambda th: (stream * 0.4 + 1.5 * SIN(th - 0.4), 0.0, 0.0)
        d["stole:r"] = lambda th: (stream * 0.5 + 2.0 * SIN(th - 0.4), 0.0, 0.0)
        d["skirt_f:r"] = lambda th: (tail * SIN(th) + 2.0, 0.0, 0.0)
        d["skirt_b:r"] = lambda th: (-tail * SIN(th) - stream * 0.5, 0.0, 0.0)
        d["skirt_l:r"] = lambda th: (0.0, 0.0, 2.0 * SIN(th + 1.0))
        d["skirt_r:r"] = lambda th: (0.0, 0.0, -2.0 * SIN(th + 1.0))
        return {k: v for k, v in d.items() if k.split(":")[0] in b.m.bones}
    loop_clip(A, "walk", 1.2, 1, 24, stride(1.2, 26.0, 14.0, 3.0, 0.7, 6.0, 7.0))
    spin("walk", 1.2, 120.0)
    loop_clip(A, "run", 0.7, 1, 24, stride(0.7, 38.0, 24.0, 12.0, 1.3, 20.0, 12.0))
    spin("run", 0.7, 120.0)

    # ------------------------------------------------------------------ attack: a sweep of the staff (the clip starts 0.2 s in)
    pose_clip(A, "attack", 0.6, False, [
        (0.00, {"body:r": (-3, 22, 0), "arm_r:r": (-60, 0, -20), "forearm_r:r": (-20, 0, 0), "hips:r": (0, -8, 0), "head:r": (0, -12, 0), "arm_l:r": (10, 0, 14)}),
        (0.12, {"body:r": (-8, -8, 0), "arm_r:r": (-26, 0, -32), "forearm_r:r": (-14, 0, 0), "hips:r": (0, 4, 0), "head:r": (0, -2, 0), "arm_l:r": (12, 0, 14)}),
        (0.26, {"body:r": (-12, -30, 0), "arm_r:r": (70, 0, 24), "forearm_r:r": (12, 0, 0), "hips:r": (0, 14, 0), "head:r": (-4, 10, 0), "arm_l:r": (16, 0, 16)}),
        (0.40, {"body:r": (-8, -20, 0), "arm_r:r": (56, 0, 18), "forearm_r:r": (14, 0, 0), "hips:r": (0, 10, 0), "head:r": (-2, 6, 0), "arm_l:r": (12, 0, 14)}),
        (0.60, {"body:r": (-1, -3, 0), "arm_r:r": (10, 0, -4), "forearm_r:r": (8, 0, 0), "hips:r": (0, 2, 0), "arm_l:r": (6, 0, 10)})])

    # ------------------------------------------------------------------ cast: the wind-up of a move: the staff lifts, the free hand opens, the head rises, the halo spins up
    pose_clip(A, "cast", 1.4, False, [
        (0.0, {}),
        (0.4, {"body:r": (5, 0, 0), "head:r": (10, 0, 0), "arm_l:r": (60, 0, 30), "forearm_l:r": (24, 0, 0), "hand_l:r": (-10, 0, 14), "arm_r:r": (30, 0, -10),
               "forearm_r:r": (14, 0, 0), "cape:r": (-10, 0, 0), "skirt_b:r": (4, 0, 0)}),
        (0.9, {"body:r": (7, 0, 0), "head:r": (14, 0, 0), "arm_l:r": (80, 0, 38), "forearm_l:r": (26, 0, 0), "hand_l:r": (-14, 0, 18), "arm_r:r": (44, 0, -12),
               "forearm_r:r": (16, 0, 0), "cape:r": (-14, 0, 0), "skirt_b:r": (6, 0, 0)}),
        (1.4, {"body:r": (8, 0, 0), "head:r": (16, 0, 0), "arm_l:r": (86, 0, 42), "forearm_l:r": (28, 0, 0), "hand_l:r": (-14, 0, 20), "arm_r:r": (50, 0, -14),
               "forearm_r:r": (18, 0, 0), "cape:r": (-16, 0, 0), "skirt_b:r": (7, 0, 0)})])
    spin("cast", 1.4, 240.0)

    # ------------------------------------------------------------------ cast_big: Erasure: the staff raised in both hands, a long gathering, the halo swelling; at 0.8 of the clip the staff comes down
    pose_clip(A, "cast_big", 2.4, False, [
        (0.0, {}),
        (0.6, {"body:r": (4, 0, 0), "head:r": (8, 0, 0), "arm_r:r": (110, 0, -14), "forearm_r:r": (14, 0, 0), "arm_l:r": (100, 0, 18), "forearm_l:r": (14, 0, 0),
               "cape:r": (-14, 0, 0)}),
        (1.5, {"body:r": (8, 0, 0), "head:r": (18, 0, 0), "arm_r:r": (160, 0, -10), "forearm_r:r": (8, 0, 0), "arm_l:r": (150, 0, 22), "forearm_l:r": (8, 0, 0),
               "cape:r": (-22, 0, 0), "skirt_b:r": (8, 0, 0), "hips:p": (0, 0.6, 0)}),
        (1.92, {"body:r": (12, 0, 0), "head:r": (22, 0, 0), "arm_r:r": (176, 0, -8), "forearm_r:r": (4, 0, 0), "arm_l:r": (166, 0, 24), "forearm_l:r": (4, 0, 0),
                "cape:r": (-26, 0, 0), "skirt_b:r": (10, 0, 0), "hips:p": (0, 0.9, 0)}),
        (2.06, {"body:r": (-22, 0, 0), "head:r": (-10, 0, 0), "arm_r:r": (28, 0, -6), "forearm_r:r": (22, 0, 0), "arm_l:r": (30, 0, 10), "forearm_l:r": (20, 0, 0),
                "cape:r": (22, 0, 0), "hips:p": (0, -2.4, 0), "leg_l:r": (-20, 0, 0), "leg_r:r": (10, 0, 0)}),
        (2.4, {"body:r": (-3, 0, 0), "arm_r:r": (10, 0, -4), "forearm_r:r": (8, 0, 0), "arm_l:r": (8, 0, 10), "cape:r": (5, 0, 0)})])
    A.rot("cast_big", "halo", {0.0: (0, 0, 0), 1.92: (0, 0, 960), 2.4: (0, 0, 1020)})
    A.scale("cast_big", "halo", {0.0: (1, 1, 1), 1.7: (1, 1, 1), 1.92: (1.16, 1.16, 1.0), 2.15: (1.28, 1.28, 1.0), 2.4: (1, 1, 1)})
    if gates:
        A.rot("cast_big", "halo_b", {0.0: (0, 0, 0), 1.92: (0, 0, -720), 2.4: (0, 0, -810)})
        A.rot("cast_big", "pages", {0.0: (0, 0, 0), 1.92: (0, 0, 1080), 2.4: (0, 0, 1180)})
        A.scale("cast_big", "pages", {0.0: (1, 1, 1), 1.92: (1.2, 1.2, 1.0), 2.4: (1, 1, 1)})

    # ------------------------------------------------------------------ hurt: a flinch
    pose_clip(A, "hurt", 0.3, False, [
        (0.0, {}),
        (0.07, {"body:r": (8, 8, 3), "head:r": (12, -10, 0), "arm_l:r": (10, 0, 14), "arm_r:r": (14, 0, -10), "cape:r": (-14, 0, 0), "hips:p": (0, -0.5, 0)}),
        (0.18, {"body:r": (3, 3, 1), "head:r": (4, -3, 0), "cape:r": (4, 0, 0)}),
        (0.30, {})])
    A.rot("hurt", "halo", {0.0: (0, 0, 0), 0.07: (0, 0, 14), 0.18: (0, 0, -6), 0.3: (0, 0, 0)})

    # ------------------------------------------------------------------ transform: the open ledger: the God rises, the arms spread, the halo spins; the gates model grows its second ring and its pages
    spread = {"body:r": (10, 0, 0), "head:r": (22, 0, 0), "arm_l:r": (20, 0, 64), "arm_r:r": (20, 0, -64), "forearm_l:r": (14, 0, 0), "forearm_r:r": (14, 0, 0),
              "hand_l:r": (14, 0, 24), "hand_r:r": (14, 0, -24), "cape:r": (-26, 0, 0), "skirt_b:r": (8, 0, 0), "hips:p": (0, 1.2, 0)}
    if not gates:
        pose_clip(A, "transform", 1.5, False, [
            (0.0, {}),
            (0.45, {"body:r": (-8, 0, 0), "hips:p": (0, -1.6, 0), "head:r": (-6, 0, 0), "arm_l:r": (14, 0, 12), "arm_r:r": (14, 0, -12), "cape:r": (-6, 0, 0)}),
            (1.0, dict(spread, **{"body:r": (4, 0, 0), "arm_l:r": (12, 0, 34), "arm_r:r": (12, 0, -34)})),
            (1.5, spread)])
        A.rot("transform", "halo", {0.0: (0, 0, 0), 1.5: (0, 0, 720)})
    else:
        pose_clip(A, "transform", 1.5, False, [
            (0.0, spread),
            (0.45, dict(spread, **{"head:r": (26, 0, 0), "body:r": (14, 0, 0)})),
            (1.0, {"body:r": (3, 0, 0), "head:r": (6, 0, 0), "arm_l:r": (24, 0, 24), "arm_r:r": (24, 0, -24), "forearm_l:r": (12, 0, 0), "forearm_r:r": (12, 0, 0), "cape:r": (-8, 0, 0)}),
            (1.5, {"body:r": (-1, 0, 0), "arm_l:r": (8, 0, 10), "arm_r:r": (10, 0, -4), "forearm_l:r": (8, 0, 0), "forearm_r:r": (8, 0, 0), "cape:r": (3, 0, 0)})])
        A.rot("transform", "halo", {0.0: (0, 0, 0), 1.5: (0, 0, 360)})
        A.rot("transform", "halo_b", {0.0: (0, 0, 0), 1.5: (0, 0, -540)})
        A.rot("transform", "pages", {0.0: (0, 0, 0), 1.5: (0, 0, 720)})
        grow = {0.0: (0.01, 0.01, 0.01), 0.25: (0.01, 0.01, 0.01), 0.9: (1.0, 1.0, 1.0), 1.5: (1.0, 1.0, 1.0)}
        for n_ in ("halo_b", "pages"):
            A.scale("transform", n_, grow)

    # ------------------------------------------------------------------ death: the 20 ticks of the dying body: it lowers itself to its knees, the staff drops, the cape settles, the halo goes out
    pose_clip(A, "death", 1.2, False, [
        (0.0, {}),
        (0.25, {"body:r": (8, 0, 0), "head:r": (16, 0, 0), "arm_l:r": (20, 0, 30), "arm_r:r": (20, 0, -30), "forearm_l:r": (14, 0, 0), "forearm_r:r": (14, 0, 0),
                "hips:p": (0, -1.0, 0), "cape:r": (-18, 0, 0)}),
        (0.6, {"body:r": (-6, 0, 0), "head:r": (-16, 0, 0), "hips:p": (0, -12.0, 2.0), "leg_l:r": (-70, 0, 0), "leg_r:r": (-62, 0, 0),
               "arm_l:r": (24, 0, 16), "arm_r:r": (30, 0, -16), "forearm_l:r": (20, 0, 0), "forearm_r:r": (24, 0, 0), "cape:r": (10, 0, 0)}),
        (0.95, {"body:r": (-30, 0, 0), "head:r": (-26, 0, 0), "hips:p": (0, -14.0, 2.0), "leg_l:r": (-84, 0, 0), "leg_r:r": (-78, 0, 0),
                "arm_l:r": (50, 0, 12), "arm_r:r": (56, 0, -12), "forearm_l:r": (10, 0, 0), "forearm_r:r": (10, 0, 0), "cape:r": (22, 0, 0)}),
        (1.2, {"body:r": (-38, 0, 0), "head:r": (-24, 0, 0), "hips:p": (0, -15.0, 2.0), "leg_l:r": (-88, 0, 0), "leg_r:r": (-84, 0, 0),
               "arm_l:r": (60, 0, 10), "arm_r:r": (64, 0, -10), "cape:r": (28, 0, 0)})])
    A.rot("death", "halo", {0.0: (0, 0, 0), 1.2: (0, 0, 270)})
    A.scale("death", "halo", {0.0: (1, 1, 1), 0.6: (1.0, 1.0, 1.0), 1.2: (0.01, 0.01, 0.01)})
    if gates:
        A.rot("death", "halo_b", {0.0: (0, 0, 0), 1.2: (0, 0, -360)})
        A.scale("death", "halo_b", {0.0: (1, 1, 1), 0.5: (1.0, 1.0, 1.0), 1.0: (0.01, 0.01, 0.01), 1.2: (0.01, 0.01, 0.01)})
        A.scale("death", "pages", {0.0: (1, 1, 1), 0.5: (1.0, 1.0, 1.0), 1.0: (0.01, 0.01, 0.01), 1.2: (0.01, 0.01, 0.01)})

    # ------------------------------------------------------------------ read: 4 s, looping: the verdict is read aloud: the head bows to the page, both hands open, the halo turns fast
    L = 4.0
    t = {}
    t["body:p"] = lambda th: (0.0, 0.35 * SIN(th * 2), 0.0)
    t["body:r"] = lambda th: (-4.0 + 1.0 * SIN(th * 2), 1.5 * SIN(th), 0.0)
    t["head:r"] = lambda th: (-14.0 + 2.0 * SIN(th * 2 + 0.5), 5.0 * SIN(th), 0.0)
    t["arm_l:r"] = lambda th: (34.0 + 3.0 * SIN(th * 2), 0.0, 26.0)
    t["forearm_l:r"] = lambda th: (40.0, 0.0, 6.0)
    t["hand_l:r"] = lambda th: (-12.0, 0.0, 10.0 + 3.0 * SIN(th * 2))
    t["arm_r:r"] = lambda th: (22.0 + 2.0 * SIN(th * 2 + 1.0), 0.0, -16.0)
    t["forearm_r:r"] = lambda th: (22.0, 0.0, 0.0)
    t["staff:r"] = lambda th: (-(44.0 + 2.0 * SIN(th * 2 + 1.0)), 0.0, 16.0)
    t["cape:r"] = lambda th: (6.0 + 2.0 * SIN(th * 2), 0.0, 0.0)
    t["stole:r"] = lambda th: (1.5 * SIN(th * 2 + 1.0), 0.0, 0.0)
    t["skirt_f:r"] = lambda th: (2.0 * SIN(th * 2), 0.0, 0.0)
    t["skirt_b:r"] = lambda th: (-2.0 * SIN(th * 2 + 1.0), 0.0, 0.0)
    t["halo:p"] = lambda th: (0.0, 0.7 * SIN(th * 2 + 1.0), 0.0)
    loop_clip(A, "read", L, 2, 48, {k: v for k, v in t.items() if k.split(":")[0] in b.m.bones})
    spin("read", L, 240.0)
    A.scale("read", "halo", {0.0: (1.0, 1.0, 1.0), 2.0: (1.08, 1.08, 1.0), 4.0: (1.0, 1.0, 1.0)})

    # ------------------------------------------------------------------ beam: 1.6 s, looping: the staff levelled at the holder, the free hand held back, the halo blazing
    L = 1.6
    t = {}
    t["body:r"] = lambda th: (-2.0, 0.0, 0.0)
    t["body:p"] = lambda th: (0.0, 0.25 * SIN(th * 2), 0.0)
    t["head:r"] = lambda th: (2.0, 0.0, 0.0)
    t["arm_r:r"] = lambda th: (84.0 + 1.5 * SIN(th * 4), 0.0, -6.0)
    t["forearm_r:r"] = lambda th: (6.0, 0.0, 0.0)
    t["hand_r:r"] = lambda th: (0.0, 0.0, 0.0)
    t["staff:r"] = lambda th: (-176.0, 0.0, 0.0)
    t["arm_l:r"] = lambda th: (-14.0, 0.0, 30.0)
    t["forearm_l:r"] = lambda th: (14.0, 0.0, 0.0)
    t["cape:r"] = lambda th: (-10.0 + 2.0 * SIN(th * 4), 0.0, 0.0)
    t["skirt_b:r"] = lambda th: (4.0, 0.0, 0.0)
    loop_clip(A, "beam", L, 1, 32, {k: v for k, v in t.items() if k.split(":")[0] in b.m.bones})
    spin("beam", L, 240.0)
    A.scale("beam", "halo", {0.0: (1.12, 1.12, 1.0), 0.8: (1.2, 1.2, 1.0), 1.6: (1.12, 1.12, 1.0)})

    # ------------------------------------------------------------------ bow: 2 s, once: the staff upright, the hands folded, a slow bow from the waist, and up again
    pose_clip(A, "bow", 2.0, False, [
        (0.0, {}),
        (0.7, {"body:r": (-30, 0, 0), "head:r": (-14, 0, 0), "hips:p": (0, -0.5, 0), "arm_l:r": (46, 0, 16), "forearm_l:r": (30, 0, 0), "arm_r:r": (22, 0, -6),
               "forearm_r:r": (16, 0, 0), "cape:r": (24, 0, 0), "stole:r": (-10, 0, 0), "skirt_b:r": (-6, 0, 0)}),
        (1.2, {"body:r": (-30, 0, 0), "head:r": (-14, 0, 0), "hips:p": (0, -0.5, 0), "arm_l:r": (46, 0, 16), "forearm_l:r": (30, 0, 0), "arm_r:r": (22, 0, -6),
               "forearm_r:r": (16, 0, 0), "cape:r": (24, 0, 0), "stole:r": (-10, 0, 0), "skirt_b:r": (-6, 0, 0)}),
        (2.0, {})])
    spin("bow", 2.0, 120.0)


def build_all(only=None):
    out = {}
    for name in MODELS:
        if only and name not in only:
            continue
        gates = name == "gates"
        b = Build(name)
        build_body(b, gates)
        add_clips(b, gates)
        img, glow = b.finish()
        hh = b.save_geo()
        b.save_textures(img, glow, hh)
        print(b.m.id, "cubes", b.m.cube_count(), "texture", b.pk.W, "x", hh, "(rows used", b.pk.height_used(), ")")
        out[name] = b
    return out


# ------------------------------------------------------------------------------------------------------------------ preview scenes
def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(model, anim, ages, views, tile=(420, 640)):
    return {"kind": "geo", "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages), "tile": list(tile), "floor": True,
            "figure": False, "expect": [], "anim": anim, "model": "%s_%s" % (KEY, model), "views": views}


def scenes_for(models=MODELS):
    out = {}
    for name in models:
        pre = "god_%s_" % name
        h = 1.7
        out[pre + "front"] = scene(name, "idle", [0], [view("front", 0, 4, (0, h, 0), 7.6), view("front 3/4", 35, 8, (0, h, 0), 7.6)])
        out[pre + "back"] = scene(name, "idle", [0], [view("back", 180, 5, (0, h, 0), 7.6), view("side", 90, 4, (0, h, 0), 7.6)])
        out[pre + "face"] = scene(name, "idle", [0], [view("face", 0, 2, (0, 2.95, 0), 2.6), view("face 3/4", 32, 6, (0, 2.95, 0), 2.6)], tile=(420, 420))
        for clip, ages in (("walk", [0, 5, 10, 15]), ("run", [0, 3, 6, 9]), ("attack", [0, 4, 8, 12]), ("cast", [0, 6, 12, 18, 24]), ("cast_big", [0, 14, 28, 42, 52, 60]),
                           ("hurt", [0, 2, 4, 6]), ("transform", [0, 12, 24, 36, 48, 60]), ("death", [0, 5, 10, 15, 20]), ("read", [0, 20, 40, 60]),
                           ("beam", [0, 8, 16, 24]), ("bow", [0, 14, 28, 42])):
            out[pre + clip] = scene(name, clip, ages, [view("3/4", 30, 6, (0, h, 0), 8.0)], tile=(340, 520))
        out[pre + "idle"] = scene(name, "idle", [0, 20, 40, 60, 80], [view("3/4", 30, 6, (0, h, 0), 8.0)], tile=(340, 520))
        out[pre + "beam_side"] = scene(name, "beam", [8], [view("side", 90, 4, (0, h, 0), 8.0), view("front", 0, 4, (0, h, 0), 8.0), view("top", 60, 50, (0, h, 0), 8.0)],
                                       tile=(420, 520))
    return out


def write_scenes(models=MODELS, path=SCENES_OUT):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(scenes_for(models), f, indent=1)


def main(argv):
    only = [a for a in argv if not a.startswith("-")]
    build_all(only or None)
    write_scenes()


if __name__ == "__main__":
    main(sys.argv[1:])

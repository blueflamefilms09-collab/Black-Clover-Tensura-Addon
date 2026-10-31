"""Malty S. Melromarc (The Rising of the Shield Hero, princess of Melromarc, alias Myne), HD 256x256 (3.38.x, group b of docs/bosses/hd_outfits_b.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/malty_melromarc.py) through the HD bridge; every part that shows is then drawn again at 4x detail:
the sharp red-eyed face with long lashes, thin arched brows, rose eyeshadow and a red-lipped smirk; the gold tiara (a peak, a ruby, filigree studs) on the hat layer; the crimson royal dress with
a skin V-neck, a gold gorget collar with a ruby, a cream brocade tabard with gold piping, a gold belt with a ruby buckle and a pleated short skirt with a scalloped gold hem; the crimson velvet
mantle with an ermine collar, a gold crest and a gold scalloped hem on the back; puffed crimson shoulders over cream sleeves with gold buttons, a lace ruffle and gold cuffs; wine thigh-high
stockings with gold garters and maroon boots with gold folded cuffs and gold lacing.
Hair: `anime_long_open` (long crimson hair, the tiara shows through the parted fringe). Original art from memory of the canon look (docs/bosses/hd_outfits_b.md).
Glow: the rubies only (tiara, collar brooch, belt buckle, crest).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

CRIM, CRIM_L, CRIM_D = "#bc1f2e", "#e65a62", "#6e0f1a"
MANTLE = "#951528"
GOLD, GOLD_D, GOLD_L = "#e0b030", "#8a5a14", "#ffe58a"
CREAM, CREAM_D = "#f6ead8", "#d8c4a8"
WINE, MAROON = "#7a2440", "#55181e"
RUBY = "#e0183c"
SKIN = ("#e8b8a0", "#f7dcc8", "#fff0e4")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    """The base island and the second-layer island of a face: crisp things go on BOTH (the second layer is dropped beyond 24 blocks)."""
    return (cv.face(part, name, False), cv.face(part, name, True))


def lattice(f, mask, color, period=1.7, width=0.07, a=0.45, dots=True):
    """A diamond brocade: thin diagonal lines on a grid with a dot at every crossing (period 1.7 model pixels, never 1)."""
    u = (f.X + f.Y) / period
    v = (f.X - f.Y) / period
    du = np.abs(u - np.round(u)) * period / 1.4142
    dv = np.abs(v - np.round(v)) * period / 1.4142
    line = np.clip(0.5 - (np.minimum(du, dv) - width / 2) * f.f, 0, 1)
    f.fill(mask * line, color, a)
    if dots:
        dd = np.hypot(du, dv)
        f.fill(mask * np.clip(0.5 - (dd - 0.15) * f.f, 0, 1), color, min(1.0, a * 1.4))


def damask(f, mask, color, period=1.9, a=0.3):
    """A tone-on-tone brocade for the crimson cloth: a lattice of small diamonds with a dot in the middle of every cell."""
    u = (f.X + f.Y) / period
    v = (f.X - f.Y) / period
    du = np.abs(u - np.round(u - 0.5) - 0.5) * period / 1.4142
    dv = np.abs(v - np.round(v - 0.5) - 0.5) * period / 1.4142
    cell = np.clip(0.5 - (np.hypot(du, dv) - 0.2) * f.f, 0, 1)
    f.fill(mask * cell, color, a)


def velvet(f, mask, color, y0, y1, folds_x, depth=0.14, top=1.12, bottom=0.78):
    H.cloth(f, mask, color, top=top, bottom=bottom, round_=0.08, fold_xs=[], weave_k=0.0)
    H.folds(f, mask, folds_x, depth=depth, width=0.28, y0=y0, y1=y1, sway=0.2)


# ================================================================================================ head
def head(cv, hair):
    f = cv.face("head", "front", False)
    H.skin_face(f, SKIN, hair=H.tone(hair, 0.9), hairline=1.55, wave=0.5, shadow=0.2)
    for cx, rot in ((2.3, -13), (5.7, 13)):                                                       # rose eyeshadow rising to the outer corner
        m = f.m_ellipse(cx, 4.2, 1.7, 0.95, rot=rot, soft=7.0)
        f.fill(m, H.lin((0, 3.3), (0, 5.2), [(0, "#c8405c"), (0.6, "#e8808c"), (1, "#f6b8b0")]), 0.38)
    iris = ("#3c0610", "#c42a32", "#ff9a86")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.95, 2.7, 2.15, iris, lash="#2a0810", tilt=0.30, flick=0.55, lash_w=0.44, lid_shadow=0.30)
    H.brow(f, (3.35, 3.55), (0.9, 2.85), "#5a1018", 0.20, 0.07, arch=0.20)
    H.brow(f, (4.65, 3.55), (7.1, 2.85), "#5a1018", 0.20, 0.07, arch=0.20)
    H.blush(f, 1.2, 6.25, 0.95, 0.5, "#f08088", 0.3)
    H.blush(f, 6.8, 6.25, 0.95, 0.5, "#f08088", 0.3)
    H.nose(f, 4.0, 6.3, "#d89a88", 0.2)
    f.fill(f.m_poly([(3.2, 7.02), (3.7, 6.9), (4.15, 6.95), (4.6, 6.9), (5.1, 7.02), (4.15, 7.18)], soft=1.5), "#a8102c", 0.95)     # the red lipstick: a bowed upper lip ...
    f.fill(f.m_ellipse(4.15, 7.33, 0.92, 0.26, soft=1.5), "#c81e3e", 0.95)                                                     # ... and a fuller lower lip with a gloss
    f.fill(f.m_polyline([(3.2, 7.02), (4.15, 7.13), (5.1, 7.02)], 0.09), "#46061a", 0.9)
    f.ellipse(3.9, 7.3, 0.3, 0.07, "#ffc0cc", a=0.7, soft=2.5)
    f.line((5.08, 7.02), (5.5, 6.72), "#7a0c20", 0.15, 0.07, a=0.95)                                                          # the smirk corner
    # the tiara: a gold band round the hat layer, a peak and a ruby on the front, filigree studs
    for nm in SIDES:
        g = cv.face("head", nm, True)
        g.clear()
        H.trim_band(g, 1.75, 2.85, GOLD, outline=0.55)
        for x in ((1.2, 3.0, 5.0, 6.8) if nm in ("front", "back") else (0.8, 2.0, 3.2)):
            H.stud(g, x, 2.3, 0.15, GOLD_L)
    g = cv.face("head", "front", True)
    peak = g.m_poly([(3.0, 2.05), (3.5, 0.95), (4.0, 0.12), (4.5, 0.95), (5.0, 2.05)])
    g.fill(g.m_poly([(2.9, 2.1), (3.5, 0.85), (4.0, -0.05), (4.5, 0.85), (5.1, 2.1)]), H.tone(GOLD, 0.4), 0.6)
    g.fill(peak, H.metal_paint(0.1, 2.1, GOLD))
    H.bevel(g, peak, depth=0.3, gain=1.1, spec=0.5)
    for sg in (-1, 1):                                                                         # two small side points
        m = g.m_poly([(4.0 + sg * 1.35, 2.05), (4.0 + sg * 1.75, 1.2), (4.0 + sg * 2.15, 2.05)])
        g.fill(m, H.metal_paint(1.2, 2.1, GOLD))
        H.bevel(g, m, depth=0.2, gain=1.0, spec=0.4)
    H.gem(g, 4.0, 2.3, 0.5, RUBY, setting=GOLD, glow=True)


# ================================================================================================ body
def collar_front(f):
    """The gold gorget along the V neckline with a ruby brooch."""
    pts = [(0.25, 0.45), (2.0, 0.45), (4.0, 2.1), (6.0, 0.45), (7.75, 0.45)]
    H.trim_line(f, pts, GOLD, width=0.78, outline=0.6, spec=0.6)
    H.trim_line(f, [(p[0], p[1] + 0.03) for p in pts], GOLD_L, width=0.12, outline=0.0, spec=0.0, a=0.6)
    for x in (0.9, 7.1):
        H.stud(f, x, 0.45, 0.14, GOLD_L)
    H.gem(f, 4.0, 2.28, 0.52, RUBY, setting=GOLD, glow=True)


def tabard_poly(top=2.55, bot=12.2):
    return [(2.95, top), (5.05, top), (5.5, bot), (2.5, bot)]


def tabard(f, top=2.55, bot=12.2):
    m = f.m_poly(tabard_poly(top, bot))
    f.fill(m, H.lin((0, top), (0, bot), [(0, H.tone(CREAM, 1.04)), (0.6, CREAM), (1, H.tone(CREAM, 0.86))]))
    lattice(f, m, "#c8962c", period=1.7, width=0.07, a=0.42)
    H.folds(f, m, [3.6, 4.4], depth=0.05, width=0.2, y0=top, y1=bot)
    for sg in (0, 1):                                                                           # gold piping down both edges
        pts = [(2.95, top), (2.5, bot)] if sg == 0 else [(5.05, top), (5.5, bot)]
        H.trim_line(f, pts, GOLD, width=0.34, outline=0.55, spec=0.5)
        H.stitch(f, [(p[0] + (0.34 if sg == 0 else -0.34), p[1]) for p in pts], "#fff0b8", width=0.06, dash=(0.3, 0.2), shadow=0.18)
    return m


def belt(f, w=8.0, front=True):
    H.trim_band(f, 7.7, 9.05, GOLD, x0=0.0, x1=w, outline=0.6, spec=0.6)
    for x in ([0.7, 1.6, 6.4, 7.3] if front else [0.8, 2.2, 3.6, 5.0, 6.4, 7.2]):
        if x < w:
            H.stud(f, x, 8.38, 0.17, GOLD_L)
    if front and w >= 7.5:
        plate = f.m_rect(2.75, 7.35, 5.25, 9.4, 0.5)
        f.fill(f.m_rect(2.68, 7.28, 5.32, 9.47, 0.55), H.tone(GOLD, 0.4), 0.7)
        f.fill(plate, H.rad((3.4, 7.9), 3.2, [(0, GOLD_L), (0.5, GOLD), (1, GOLD_D)]))
        H.bevel(f, plate, depth=0.32, gain=1.1, spec=0.6)
        H.gem(f, 4.0, 8.38, 0.52, RUBY, setting=GOLD_D, glow=True)
        f.polyline([(4.0, 9.45), (4.0, 10.5)], GOLD_D, 0.2, a=0.9)                       # a gold cord and tassel hang from the buckle
        f.polyline([(4.0, 9.45), (4.0, 10.5)], GOLD, 0.1, a=1.0)
        f.ellipse(4.0, 10.6, 0.22, 0.22, rad_gold(4.0, 10.6), a=1.0)
        for dx in (-0.26, -0.09, 0.09, 0.26):
            f.line((4.0 + dx * 0.4, 10.78), (4.0 + dx, 11.5), GOLD, 0.09, 0.05, a=0.95)


def rad_gold(cx, cy):
    return H.rad((cx - 0.08, cy - 0.1), 0.34, [(0, GOLD_L), (1, GOLD_D)])


def body_front(cv):
    base, ov = lay(cv, "body", "front")
    ov.clear()
    # ---- base: crimson bodice and skirt, the skin V, the tabard
    f = base
    dress = f.m_band(0.0, 12.0)
    f.fill(dress, H.lin((0, 0), (0, 12), [(0, H.tone(CRIM, 1.12)), (0.6, CRIM), (1, H.tone(CRIM, 0.78))]))
    H.sheen(f, f.m_band(0.5, 7.7), [1.2, 6.8], width=0.3, strength=0.13, tilt=0.1, y0=0.8, y1=7.4, color="#ff9aa2")
    for sg in (0, 1):                                                                           # princess seams
        xs = [(2.05, 1.4), (1.7, 4.4), (2.0, 7.7)] if sg == 0 else [(5.95, 1.4), (6.3, 4.4), (6.0, 7.7)]
        f.polyline(xs, H.tone(CRIM, 0.62), 0.10, a=0.6)
        f.polyline([(p[0] + (0.12 if sg == 0 else -0.12), p[1]) for p in xs], H.tone(CRIM, 1.25), 0.06, a=0.35)
    v = f.m_poly([(1.9, -0.3), (6.1, -0.3), (4.0, 2.0)])
    f.fill(v, H.lin((0, 0), (0, 2.2), [(0, SKIN[1]), (1, SKIN[0])]))
    f.shade(f.m_poly([(1.9, -0.3), (6.1, -0.3), (4.0, 2.0)], soft=3.0) * H.smoothstep(0.0, 1.6, f.Y) * 0.5, 0.9)
    for f in (base,):
        tabard(f)
    # the skirt part below the belt: pleats
    sk = base.m_band(9.05, 12.0)
    damask(base, H.minus(sk, base.m_poly(tabard_poly())), H.tone(CRIM, 1.3), a=0.3)
    H.folds(base, H.minus(sk, base.m_poly(tabard_poly())), [0.7, 1.7, 6.3, 7.3], depth=0.18, width=0.22, y0=9.05, y1=12.0, sway=0.1)
    H.shadow_under(base, 9.05, 0.7, 0.28)
    belt(base)
    collar_front(base)
    # ---- overlay: the cape strips, the tabard, the belt, the skirt panels, the collar
    f = ov
    for x0, x1, sg in ((0.0, 1.25, 1), (6.75, 8.0, -1)):
        m = f.m_poly([(x0, 0.5), (x1, 1.0), (x1 - sg * 0.1, 7.7), (x0, 7.7)] if sg == 1 else [(x0, 1.0), (x1, 0.5), (x1, 7.7), (x0 + 0.1, 7.7)])
        velvet(f, m, MANTLE, 0.5, 7.7, [x0 + (x1 - x0) * 0.4], depth=0.16, top=1.1, bottom=0.8)
        damask(f, m, H.tone(MANTLE, 1.35), a=0.28)
        H.bevel(f, m, depth=0.25, gain=0.8)
        H.trim_line(f, [(x1 if sg == 1 else x0, 1.0), (x1 if sg == 1 else x0, 7.7)], GOLD, width=0.3, outline=0.5, spec=0.5)
    tabard(f, top=2.55, bot=12.2)
    sk_m = H.minus(f.m_band(9.05, 12.0), f.m_poly(tabard_poly()))
    f.fill(sk_m, H.lin((0, 9.05), (0, 12.0), [(0, H.tone(CRIM, 1.05)), (1, H.tone(CRIM, 0.82))]))
    damask(f, sk_m, H.tone(CRIM, 1.3), a=0.3)
    H.folds(f, sk_m, [0.7, 1.7, 6.3, 7.3], depth=0.2, width=0.22, y0=9.05, y1=12.0, sway=0.1)
    H.sheen(f, sk_m, [1.2, 6.8], width=0.2, strength=0.2, y0=9.2, y1=11.8, color="#ff9aa2")
    H.shadow_under(f, 9.05, 0.7, 0.28)
    belt(f)
    collar_front(f)
    for g in (base, ov):
        H.trim_line(g, [(0.0, 11.85), (8.0, 11.85)], GOLD_D, width=0.2, outline=0.0, spec=0.0, a=0.7)


def mantle_back(f, w=8.0, crest=True):
    m = f.m_band(1.4, 12.0)
    f.fill(m, H.lin((0, 1.4), (0, 12), [(0, H.tone(MANTLE, 1.05)), (0.55, MANTLE), (1, H.tone(MANTLE, 0.72))]))
    damask(f, m, H.tone(MANTLE, 1.35), a=0.26)
    H.folds(f, m, [0.9, 2.0, 3.2, 4.8, 6.0, 7.1] if w > 5 else [0.9, 2.2, 3.4], depth=0.2, width=0.3, y0=1.6, y1=12.0, sway=0.3)
    H.sheen(f, m, [1.5, 4.0, 6.5] if w > 5 else [1.4, 3.0], width=0.24, strength=0.16, tilt=0.06, y0=2.0, y1=10.0, color="#ff9aa2")
    H.shadow_under(f, 1.9, 0.9, 0.3)
    for x in (0.18, w - 0.18):
        H.trim_line(f, [(x, 1.6), (x, 11.0)], GOLD, width=0.34, outline=0.55, spec=0.5)
    if crest:
        cx, cy = w / 2.0, 5.1
        dia = f.m_poly([(cx, cy - 2.3), (cx + 1.55, cy), (cx, cy + 2.3), (cx - 1.55, cy)])
        f.fill(f.m_poly([(cx, cy - 2.55), (cx + 1.75, cy), (cx, cy + 2.55), (cx - 1.75, cy)]), H.tone(GOLD_D, 0.6), 0.85)
        f.fill(dia, H.metal_paint(cy - 2.3, cy + 2.3, GOLD))
        H.bevel(f, dia, depth=0.34, gain=1.1, spec=0.6)
        inner = f.m_poly([(cx, cy - 1.45), (cx + 0.95, cy), (cx, cy + 1.45), (cx - 0.95, cy)])
        f.fill(inner, H.tone(CRIM_D, 0.9))
        H.bevel(f, inner, depth=0.2, gain=-0.7)
        for sg in (-1, 1):
            f.polyline([(cx, cy - 0.8), (cx + sg * 0.55, cy - 0.15), (cx + sg * 0.3, cy + 0.65)], GOLD, 0.1, a=0.85)
        H.gem(f, cx, cy, 0.5, RUBY, setting=GOLD_L, glow=True)
        for dx, dy in ((0, -2.7), (0, 2.7)):
            H.stud(f, cx + dx, cy + dy, 0.17, GOLD_L)
    # the belt that cinches the mantle
    H.trim_band(f, 7.9, 8.85, GOLD, x0=0.0, x1=w, outline=0.55, spec=0.5)
    H.shadow_under(f, 8.85, 0.7, 0.25)


def ermine(f, y0=0.0, depth=1.9, w=8.0):
    """A cream fur collar: two rows of soft tabs, ermine tails (little dark teardrops) along it."""
    H.scallops(f, f.m_band(y0, y0 + depth + 0.62), y0, rows=2, cell=1.34, depth=depth, base="#f6ead8", tip="#ffffff", edge="#d8c4a8", edge_a=0.4, shadow=0.18)
    for x in ((1.5, 4.0, 6.5) if w > 5 else (1.2, 2.8)):
        f.poly([(x - 0.12, y0 + 0.75), (x + 0.12, y0 + 0.75), (x, y0 + 1.55)], "#2a1a1a", a=0.9)


def hem_scallops(f, y0, cell=1.34, depth=1.3, rows=1):
    H.scallops(f, f.m_band(y0, y0 + depth + 0.62 * (rows - 1)), y0, rows=rows, cell=cell, depth=depth, base=GOLD, tip=GOLD_L, edge=GOLD_D, edge_a=0.8, shadow=0.22)


def body_back(cv):
    base, ov = lay(cv, "body", "back")
    ov.clear()
    f = base
    f.fill(f.m_band(0, 12), H.lin((0, 0), (0, 12), [(0, H.tone(CRIM, 1.0)), (0.6, H.tone(CRIM, 0.9)), (1, H.tone(CRIM, 0.72))]))
    H.trim_band(f, 7.9, 8.85, GOLD)
    H.trim_line(f, [(0.0, 11.85), (8.0, 11.85)], GOLD_D, width=0.2, outline=0.0, spec=0.0, a=0.7)
    ermine(f)
    f = ov
    mantle_back(f)
    f.clear(f.m_band(10.7, 12.2))                                                              # the mantle ends in a gold scalloped hem
    mantle_hem(f)
    ermine(f)


def mantle_hem(f):
    hem = f.m_band(10.6, 12.0)
    f.fill(hem, H.lin((0, 10.6), (0, 12), [(0, H.tone(MANTLE, 0.85)), (1, H.tone(MANTLE, 0.7))]))
    hem_scallops(f, 10.55, cell=1.34, depth=1.45, rows=1)


def body_side(cv, name):
    base, ov = lay(cv, "body", name)
    ov.clear()
    f = base
    f.fill(f.m_band(0, 12), H.lin((0, 0), (0, 12), [(0, H.tone(CRIM, 1.0)), (0.6, H.tone(CRIM, 0.88)), (1, H.tone(CRIM, 0.7))]))
    H.trim_band(f, 7.9, 8.85, GOLD)
    ermine(f, w=4.0)
    f = ov
    mantle_back(f, w=4.0, crest=False)
    H.trim_line(f, [(0.18 if name == "west" else 3.82, 1.6), (0.18 if name == "west" else 3.82, 10.6)], GOLD, width=0.34, outline=0.55, spec=0.5)
    f.clear(f.m_band(10.7, 12.2))
    mantle_hem(f)
    ermine(f, w=4.0)


def body(cv):
    body_front(cv)
    body_back(cv)
    body_side(cv, "west")
    body_side(cv, "east")
    for f in lay(cv, "body", "top"):
        f.fill(f.m_all(), H.lin((0, 0), (f.w, 0), [(0, "#fff8ec"), (0.5, "#f2e2c6"), (1, "#fff8ec")]))
    ov = cv.face("body", "top", True)
    ov.clear()
    H.trim_band(ov, 0.0, ov.h, GOLD, outline=0.0)
    ov.clear()
    for f in lay(cv, "body", "bottom"):
        f.fill(f.m_all(), H.tone(CRIM_D, 0.8))
    cv.face("body", "bottom", True).clear()


# ================================================================================================ arms (slim)
def puff(f, part, name):
    top = f.m_poly([(0.0, 0.0), (f.w, 0.0), (f.w + 0.15, 3.0), (f.w - 0.2, 3.65), (0.2, 3.65), (-0.15, 3.0)])
    f.fill(top, H.lin((0, 0), (0, 3.6), [(0, H.tone(CRIM, 1.08)), (0.5, CRIM), (1, H.tone(CRIM, 0.66))]))
    f.fill(top, H.rad((f.w * 0.35, 0.9), f.w * 0.9, [(0, "#f06a74"), (1, CRIM)]), 0.3)
    damask(f, top, H.tone(CRIM, 1.35), period=1.7, a=0.25)
    H.folds(f, top, [f.w * 0.3, f.w * 0.65], depth=0.2, width=0.2, y0=1.2, y1=3.6, sway=0.1)
    H.bevel(f, top, depth=0.4, gain=0.9, spec=0.35)
    H.trim_band(f, 3.5, 4.3, GOLD, outline=0.6)
    H.stud(f, f.w / 2.0, 0.9, 0.17, GOLD_L) if name == "front" else None


def arm(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        ov.clear()
        for f in (base, ov):
            sleeve = f.m_band(3.9, 9.6)
            H.cloth(f, sleeve, CREAM, top=1.04, bottom=0.88, round_=0.1, fold_xs=[f.w * 0.3, f.w * 0.72], fold_depth=0.07, fold_y0=4.2, fold_y1=9.2, weave_k=0.0)
            H.sheen(f, sleeve, [f.w * 0.5], width=0.2, strength=0.18, y0=4.4, y1=9.0)
            puff(f, part, nm)
            if nm == "front":
                for y in (5.3, 6.9, 8.4 - 0.0):
                    H.stud(f, f.w / 2.0, y, 0.19, GOLD)
            # the lace ruffle and the gold cuff
            H.scallops(f, f.m_band(8.55, 9.75), 8.5, rows=1, cell=1.0 if f.w < 4 else 1.33, depth=1.0, base="#fffaf0", tip="#ffffff", edge=CREAM_D, edge_a=0.5, shadow=0.2)
            H.trim_band(f, 9.35, 10.6, GOLD, outline=0.6, spec=0.6)
            if nm == "front":
                H.gem(f, f.w / 2.0, 9.98, 0.34, RUBY, setting=GOLD_D, glow=True)
        ov.fill(ov.m_band(10.62, 12.0, soft=1), "#000000", mode="erase")
        hand = base.m_band(10.55, 12.0)
        base.fill(hand, H.lin((0, 10.55), (0, 12), [(0, SKIN[0]), (0.4, SKIN[1]), (1, H.tone(SKIN[1], 0.94))]))
        base.shade(base.m_band(10.55, 10.95), 0.86)                                       # the shadow of the cuff
        if nm in ("front", "back"):
            for x in (f.w / 3.0, 2 * f.w / 3.0):
                base.line((x, 11.35), (x, 12.0), H.tone(SKIN[0], 0.82), 0.07, 0.05, a=0.7)
            if nm == "front":
                base.rect(0.18, 11.62, base.w - 0.18, 11.95, "#c83048", r=0.1, a=0.6)             # nails
    for f in lay(cv, part, "top"):
        f.fill(f.m_all(), H.rad((f.w * 0.4, f.h * 0.4), f.w, [(0, "#f06a72"), (1, CRIM_D)]))
        H.stud(f, f.w / 2.0, f.h / 2.0, 0.34, GOLD, glow=None)
    cv.face(part, "top", True).clear()
    for f in lay(cv, part, "bottom"):
        f.fill(f.m_all(), SKIN[0])
    cv.face(part, "bottom", True).clear()


# ================================================================================================ legs
def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        ov.clear()
        for f in (base, ov):
            # stockings (the thighs show between the skirt and the boot top)
            st = f.m_band(0.0, 7.2)
            H.cloth(f, st, WINE, top=1.14, bottom=0.82, round_=0.12, fold_xs=[], weave_k=0.0)
            H.sheen(f, st, [f.w * 0.3], width=0.22, strength=0.3, y0=0.4, y1=6.8, color="#ff9ab8")
            f.shade(st * np.clip(1.0 - np.abs(f.Y - 3.4) * 2.5, 0, 1), 0.88)
            # garter
            H.trim_band(f, 2.95, 3.6, GOLD, outline=0.55)
            if nm == "front":
                H.gem(f, f.w * 0.5, 3.28, 0.26, RUBY, setting=GOLD_D, glow=None)
            # boot: maroon leather, gold lacing on the front, gold cuff
            boot = f.m_band(7.4, 12.0)
            f.fill(boot, H.lin((0, 7.4), (0, 12), [(0, H.tone(MAROON, 1.25)), (0.5, MAROON), (1, H.tone(MAROON, 0.6))]))
            H.sheen(f, boot, [f.w * 0.28], width=0.2, strength=0.28, y0=7.5, y1=11.3, color="#e87a80")
            H.shadow_under(f, 7.6, 0.7, 0.3)
            if nm == "front":
                for y in (8.5, 9.4, 10.3):
                    H.trim_line(f, [(0.6, y), (f.w - 0.6, y + 0.5)], GOLD, width=0.16, outline=0.3, spec=0.3)
                    H.trim_line(f, [(f.w - 0.6, y), (0.6, y + 0.5)], GOLD, width=0.16, outline=0.3, spec=0.3)
            cuff = f.m_band(6.4, 7.9)
            f.fill(cuff, H.metal_paint(6.4, 7.9, GOLD))
            H.bevel(f, cuff, depth=0.4, gain=1.2, spec=0.6)
            H.scallops(f, f.m_band(7.6, 8.6), 7.5, rows=1, cell=1.0 if f.w < 3 else 1.33, depth=0.9, base=GOLD, tip=GOLD_L, edge=GOLD_D, edge_a=0.8, shadow=0.3)
            sole = f.m_band(11.3, 12.0)
            f.fill(sole, H.lin((0, 11.3), (0, 12), [(0, "#2a1014"), (1, "#14080a")]))
            H.trim_line(f, [(0.0, 11.3), (f.w, 11.3)], GOLD_D, width=0.12, outline=0.0, spec=0.0, a=0.7)
        ov.fill(ov.m_band(2.3, 6.3), "#000000", mode="erase")                              # only the garter and the cuff stay raised
        H.trim_band(ov, 2.95, 3.6, GOLD, outline=0.55)
        # the skirt flare on the second layer (y 0..2.3) with its gold scalloped hem
        sk = ov.m_band(-0.2, 1.5)
        ov.fill(sk, H.lin((0, 0), (0, 1.6), [(0, H.tone(CRIM, 1.05)), (1, H.tone(CRIM, 0.75))]))
        damask(ov, sk, H.tone(CRIM, 1.3), a=0.28)
        H.folds(ov, sk, [ov.w * 0.25, ov.w * 0.6, ov.w * 0.95], depth=0.2, width=0.2, y0=0.0, y1=1.6, sway=0.1)
        hem_scallops(ov, 1.2, cell=1.0 if ov.w < 3 else 1.33, depth=1.2)
        bsk = base.m_band(-0.2, 1.5)                                                                   # the skirt and its gold hem on the base layer too (kept when the second layer is dropped)
        base.fill(bsk, H.lin((0, 0), (0, 1.6), [(0, H.tone(CRIM, 1.05)), (1, H.tone(CRIM, 0.75))]))
        hem_scallops(base, 1.2, cell=1.0 if base.w < 3 else 1.33, depth=1.2)
    # the cream tabard continues down the inner column of the two front faces and ends in a gold tassel edge
    for part_ in ("leg_r", "leg_l"):
        f = cv.face(part_, "front", True)
        x0, x1 = (3.0, 4.0) if part_ == "leg_r" else (0.0, 1.0)
        edge = x1 if part_ == "leg_r" else x0
        mask = f.m_rect(x0, -0.2, x1, 2.9)
        f.fill(mask, H.lin((0, 0), (0, 2.9), [(0, CREAM), (1, H.tone(CREAM, 0.88))]))
        lattice(f, mask, "#c8962c", period=1.7, a=0.4)
        H.trim_line(f, [(edge, 0.0), (edge, 2.5)], GOLD, width=0.3, outline=0.5, spec=0.5)
        hem_scallops(f, 2.15, cell=1.0, depth=0.9)
    for part_ in ("leg_r", "leg_l"):
        for f in lay(cv, part_, "top"):
            f.fill(f.m_all(), H.tone(CRIM, 0.8))
        cv.face(part_, "top", True).clear()
        for f in lay(cv, part_, "bottom"):
            f.fill(f.m_all(), "#14080a")
        cv.face(part_, "bottom", True).clear()


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.family(GOLD, 20, 0.5)], "gem": [H.near("#ff6a78", 0.2), H.near(RUBY, 0.2)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

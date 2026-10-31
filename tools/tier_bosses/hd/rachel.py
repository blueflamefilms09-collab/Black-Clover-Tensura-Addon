"""Rachel (Tower of God), HD 256x256 (3.38.x, group b of docs/bosses/hd_outfits_b.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/rachel.py) through the HD bridge; every part that shows is then drawn again at 4x detail:
the cold round amber-brown eyes with long lashes and flat brows, a thin straight mouth and no blush; a white satin hair ribbon with a side bow on the hat layer; the pale cyan fitted dress with
princess seams, a white high collar with a cascading cyan bow, a white satin tabard with bright-cyan piping and a stitched chain of diamonds, a white pointed lace-edged capelet with pearls,
a charcoal leather belt with a silver clasp and a cyan stone, a flared pleated skirt with a white scalloped petticoat hem; cyan sleeves with big white double-ruffle cuffs; charcoal leggings
with a bright cyan side line; slate boots with straps, silver buckles and white folded cuffs.
Hair: `anime_long_open` (long chestnut hair, parted fringe). Original art from memory of the character's look (docs/bosses/hd_outfits_b.md); Rachel is the least certain canon-wise of the group.
Glow: none (nothing on her outfit glows).
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

CYAN, CYAN_L, CYAN_D, CYAN_B = "#79d6e4", "#c4f4f8", "#2c8fa6", "#2cc6dc"
WHITE, WHITE_D = "#f7fcfd", "#cde0e8"
CHAR, CHAR_D = "#3a4a54", "#1f2b32"
SLATE, SLATE_D = "#4a6674", "#243238"
SILVER = "#d6e0e8"
SKIN = ("#e8bfa6", "#f7e0cd", "#fff2e6")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def scallop_edge(f, pts, r, color, edge, edge_a=0.6, step=None, inside=None, pearl=None):
    """A lace/frill border along a path: round tabs of radius r every `step` along the polyline `pts`, a thin edge arc on the outer side of each, an optional pearl in each tab."""
    step = step or r * 1.7
    samples = []
    for i in range(len(pts) - 1):
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        n = max(1, int(round(math.hypot(x1 - x0, y1 - y0) / step)))
        for k in range(n):
            t = (k + 0.5) / n
            samples.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * t))
    for cx, cy in samples:
        f.ellipse(cx, cy, r, r, color)
    if edge is not None:
        for cx, cy in samples:
            ring = f.m_ring(cx, cy, r, 0.09)
            f.fill(ring * (1.0 - (inside if inside is not None else 0.0)), edge, edge_a)
    if pearl:
        for cx, cy in samples:
            H.stud(f, cx, cy, r * 0.28, pearl, spec=0.6)
    return samples


def damask(f, mask, color, period=1.9, a=0.22):
    """A tone-on-tone brocade for the cyan cloth: a lattice of small diamonds."""
    u = (f.X + f.Y) / period
    v = (f.X - f.Y) / period
    du = np.abs(u - np.round(u - 0.5) - 0.5) * period / 1.4142
    dv = np.abs(v - np.round(v - 0.5) - 0.5) * period / 1.4142
    cell = np.clip(0.5 - (np.hypot(du, dv) - 0.2) * f.f, 0, 1)
    f.fill(mask * cell, color, a)


def satin(f, mask, color, y0, y1, top=1.1, bottom=0.82, streaks=None, strength=0.2, tint="#ffffff"):
    f.fill(mask, H.lin((0, y0), (0, y1), [(0, H.tone(color, top)), (0.55, color), (1, H.tone(color, bottom))]))
    if streaks:
        H.sheen(f, mask, streaks, width=0.26, strength=strength, tilt=0.08, y0=y0 + 0.2, y1=y1 - 0.2, color=tint)


# ================================================================================================ head
def head(cv, hair):
    f = cv.face("head", "front", False)
    H.skin_face(f, SKIN, hair=H.tone(hair, 0.9), hairline=1.6, wave=0.45, shadow=0.2)
    for cx in (2.3, 5.7):                                                                          # a faint cool shadow under the lids: the cold look
        f.fill(f.m_ellipse(cx, 5.95, 1.3, 0.35, soft=5.0), "#8a8cb0", 0.16)
    iris = ("#3a1a0c", "#a8681e", "#f0c070")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.9, 2.7, 2.4, iris, lash="#2a140c", tilt=0.05, flick=0.34, lash_w=0.38, lid_shadow=0.42, iris_r=1.05)
        f.fill(f.m_band(3.55, 3.75, x0=cx - 1.2, x1=cx + 1.2, soft=2.0), "#3a2418", 0.28)       # the lid crease
    H.brow(f, (3.3, 3.2), (0.9, 3.05), "#4a2a16", 0.24, 0.10, arch=0.04)                           # flat, cold brows
    H.brow(f, (4.7, 3.2), (7.1, 3.05), "#4a2a16", 0.24, 0.10, arch=0.04)
    H.nose(f, 4.0, 6.3, "#d49c88", 0.18)
    H.mouth_smile(f, 4.0, 7.15, 1.7, color="#9a5056", lip="#d08486", depth=0.02, thick=0.16)
    f.fill(f.m_band(7.3, 7.5, x0=3.3, x1=4.7, soft=2.0), "#e8a8a4", 0.45)
    # the hair ribbon: a white satin band round the hat layer, a bow on the right side of the head
    for nm in SIDES:
        g = cv.face("head", nm, True)
        g.clear()
        band = g.m_band(1.75, 2.95)
        g.fill(band, H.lin((0, 1.75), (0, 2.95), [(0, "#ffffff"), (1, H.tone(WHITE, 0.85))]))
        H.sheen(g, band, [g.w * 0.3, g.w * 0.75], width=0.3, strength=0.2, y0=1.8, y1=2.9)
        H.bevel(g, band, depth=0.25, gain=0.7, spec=0.3)
        g.fill(g.m_band(1.75, 1.95), H.tone(WHITE_D, 0.9), 0.5)
    w = cv.face("head", "west", True)
    cx, cy = 6.2, 2.35
    for sg, rot in ((-1, 25), (1, -25)):                                                           # two loops and two tails
        tail = w.m_poly([(cx + sg * 0.1, cy + 0.3), (cx + sg * 0.95, cy + 2.35), (cx + sg * 0.45, cy + 2.6), (cx - sg * 0.1, cy + 0.9)])
        w.fill(tail, H.lin((0, cy), (0, cy + 2.6), [(0, "#ffffff"), (1, WHITE_D)]))
        H.bevel(w, tail, depth=0.15, gain=0.7)
    for sg in (-1, 1):
        loop = w.m_ellipse(cx + sg * 1.05, cy - 0.1, 1.15, 0.78, rot=sg * 20)
        w.fill(w.m_ellipse(cx + sg * 1.05, cy, 1.25, 0.86, rot=sg * 20), H.tone(WHITE_D, 0.65), 0.7)
        w.fill(loop, H.lin((0, cy - 0.9), (0, cy + 0.8), [(0, "#ffffff"), (1, WHITE_D)]))
        w.fill(w.m_ellipse(cx + sg * 1.1, cy - 0.05, 0.6, 0.3, rot=sg * 20), H.tone(WHITE_D, 0.8), 0.7)
        H.bevel(w, loop, depth=0.25, gain=0.8, spec=0.4)
    knot = w.m_ellipse(cx, cy + 0.05, 0.42, 0.55)
    w.fill(knot, H.rad((cx - 0.1, cy - 0.2), 0.7, [(0, "#ffffff"), (1, WHITE_D)]))
    H.bevel(w, knot, depth=0.2, gain=0.8, spec=0.5)
    f = cv.face("head", "front", True)
    for x in (0.9, 7.1):
        H.stud(f, x, 2.35, 0.14, SILVER)


# ================================================================================================ body
def bow(f, cx, cy, w=1.0):
    for sg in (-1, 1):                                                                                # tails first
        tail = f.m_poly([(cx + sg * 0.15, cy + 0.2), (cx + sg * 1.05 * w, cy + 3.2), (cx + sg * 0.45 * w, cy + 3.4), (cx - sg * 0.05, cy + 0.9)])
        f.fill(f.m_poly([(cx + sg * 0.15, cy + 0.25), (cx + sg * 1.15 * w, cy + 3.3), (cx + sg * 0.4 * w, cy + 3.55), (cx - sg * 0.1, cy + 0.95)]), H.tone(CYAN_D, 0.6), 0.6)
        f.fill(tail, H.lin((0, cy), (0, cy + 3.4), [(0, CYAN_L), (1, CYAN_B)]))
        H.bevel(f, tail, depth=0.18, gain=0.8, spec=0.3)
    for sg in (-1, 1):
        loop = f.m_ellipse(cx + sg * 1.25 * w, cy - 0.05, 1.3 * w, 0.82, rot=sg * 18)
        f.fill(f.m_ellipse(cx + sg * 1.25 * w, cy + 0.05, 1.4 * w, 0.9, rot=sg * 18), H.tone(CYAN_D, 0.6), 0.7)
        f.fill(loop, H.lin((0, cy - 0.9), (0, cy + 0.8), [(0, CYAN_L), (0.6, CYAN), (1, CYAN_D)]))
        f.fill(f.m_ellipse(cx + sg * 1.3 * w, cy, 0.7 * w, 0.32, rot=sg * 18), H.tone(CYAN_D, 0.85), 0.6)
        H.bevel(f, loop, depth=0.28, gain=0.9, spec=0.5)
    knot = f.m_ellipse(cx, cy, 0.5, 0.6)
    f.fill(f.m_ellipse(cx, cy + 0.07, 0.58, 0.68), H.tone(CYAN_D, 0.55), 0.8)
    f.fill(knot, H.rad((cx - 0.12, cy - 0.2), 0.8, [(0, CYAN_L), (1, CYAN_B)]))
    H.bevel(f, knot, depth=0.22, gain=0.9, spec=0.6)


def tabard_poly(top, bot, w0=1.35, w1=1.55):
    return [(4.0 - w0, top), (4.0 + w0, top), (4.0 + w1, bot), (4.0 - w1, bot)]


def tabard(f, top, bot, diamonds=True):
    m = f.m_poly(tabard_poly(top, bot))
    satin(f, m, WHITE, top, bot, top=1.04, bottom=0.88, streaks=[3.7, 4.4], strength=0.14)
    for sg in (-1, 1):
        edge = [(4.0 + sg * 1.35, top), (4.0 + sg * 1.55, bot)]
        H.trim_line(f, edge, CYAN_B, width=0.3, outline=0.45, spec=0.4, depth=0.14)
        H.stitch(f, [(p[0] - sg * 0.3, p[1]) for p in edge], "#7adbe8", width=0.05, dash=(0.26, 0.18), shadow=0.12)
    if diamonds:
        y = top + 0.9
        while y < bot - 0.6:
            f.poly([(4.0, y - 0.38), (4.34, y), (4.0, y + 0.38), (3.66, y)], CYAN_B, a=0.9)
            f.ellipse(4.0, y, 0.09, 0.09, "#ffffff", a=0.8)
            y += 1.45
    return m


def collar(f, y1=1.6, notch=False):
    """The white high collar (a stand-up band, a scalloped lower rim)."""
    m = f.m_band(0.0, y1)
    satin(f, m, WHITE, 0, y1, top=1.04, bottom=0.86, streaks=[f.w * 0.3, f.w * 0.72], strength=0.12)
    H.folds(f, m, [f.w * 0.5], depth=0.05, width=0.2, y0=0.0, y1=y1)
    H.trim_line(f, [(0.0, 0.1), (f.w, 0.1)], CYAN_B, width=0.16, outline=0.3, spec=0.3, depth=0.1)
    H.shadow_under(f, y1, 0.5, 0.2)


def capelet_front(f):
    """The white pointed lace capelet over the shoulders: scalloped border, pearls, cyan piping inside."""
    outline = [(0.0, 1.2), (0.25, 2.4), (1.6, 3.5), (3.0, 4.35), (4.0, 4.75), (5.0, 4.35), (6.4, 3.5), (7.75, 2.4), (8.0, 1.2)]
    m = f.m_poly(outline + [(8.2, 1.0), (8.2, -0.2), (-0.2, -0.2), (-0.2, 1.0)])
    neck = f.m_poly([(2.4, -0.3), (5.6, -0.3), (5.3, 1.7), (4.0, 2.2), (2.7, 1.7)])
    shape = H.minus(m, neck)
    inner = [(0.15, 2.2), (1.7, 3.2), (3.0, 4.0), (4.0, 4.4), (5.0, 4.0), (6.3, 3.2), (7.85, 2.2)]
    scallop_edge(f, outline[1:-1], 0.5, "#ffffff", WHITE_D, edge_a=0.8, step=0.85, inside=shape, pearl=None)
    f.fill(shape, H.lin((0, 0), (0, 4.5), [(0, "#ffffff"), (0.6, WHITE), (1, H.tone(WHITE, 0.9))]))
    H.folds(f, shape, [1.1, 2.4, 5.6, 6.9], depth=0.07, width=0.2, y0=0.0, y1=4.5)
    f.polyline(inner, CYAN_B, 0.13, a=0.9)
    for x, y in ((1.7, 3.2), (3.0, 4.0), (4.0, 4.4), (5.0, 4.0), (6.3, 3.2)):
        H.stud(f, x, y + 0.2, 0.12, "#fff6fa", spec=0.6)
    H.shadow_under(f, 4.7, 0.5, 0.0)


def belt(f, w=8.0, front=True):
    b = f.m_band(7.8, 9.0, x0=0.0, x1=w)
    f.fill(b, H.lin((0, 7.8), (0, 9.0), [(0, H.tone(CHAR, 1.3)), (0.5, CHAR), (1, CHAR_D)]))
    H.bevel(f, b, depth=0.2, gain=0.8, spec=0.35)
    H.stitch(f, [(0.0, 8.02), (w, 8.02)], "#7a8c96", width=0.05, dash=(0.3, 0.2), shadow=0.0, a=0.7)
    H.stitch(f, [(0.0, 8.78), (w, 8.78)], "#7a8c96", width=0.05, dash=(0.3, 0.2), shadow=0.0, a=0.7)
    H.shadow_under(f, 9.0, 0.7, 0.25)
    if front and w >= 7.5:
        plate = f.m_ellipse(4.0, 8.4, 1.3, 0.92)
        f.fill(f.m_ellipse(4.0, 8.46, 1.4, 1.0), H.tone(CHAR_D, 0.8), 0.7)
        f.fill(plate, H.rad((3.4, 7.9), 2.0, [(0, "#ffffff"), (0.5, SILVER), (1, H.tone(SILVER, 0.62))]))
        H.bevel(f, plate, depth=0.3, gain=1.0, spec=0.6)
        H.gem(f, 4.0, 8.4, 0.46, CYAN_B, setting=SILVER, glow=False)


def body_front(cv):
    base, ov = lay(cv, "body", "front")
    ov.clear()
    f = base
    dress = f.m_band(0, 12)
    satin(f, dress, CYAN, 0, 12, top=1.12, bottom=0.8, streaks=[1.1, 6.9], strength=0.16, tint="#e8ffff")
    damask(f, dress, H.tone(CYAN, 1.3))
    for sg in (0, 1):                                                                                  # princess seams
        xs = [(1.9, 2.0), (1.55, 5.0), (1.85, 7.8)] if sg == 0 else [(6.1, 2.0), (6.45, 5.0), (6.15, 7.8)]
        f.polyline(xs, H.tone(CYAN, 0.62), 0.10, a=0.6)
        f.polyline([(p[0] + (0.12 if sg == 0 else -0.12), p[1]) for p in xs], H.tone(CYAN, 1.25), 0.06, a=0.4)
    collar(f)
    tabard(f, 1.5, 12.2)
    sk = H.minus(f.m_band(9.0, 12.0), f.m_poly(tabard_poly(1.5, 12.2)))
    H.folds(f, sk, [0.6, 1.5, 2.3, 5.7, 6.5, 7.4], depth=0.16, width=0.2, y0=9.0, y1=12.0, sway=0.1)
    belt(f)
    bow(f, 4.0, 1.9)
    f = ov
    for x0, x1 in ((0.0, 1.3), (6.7, 8.0)):                                                           # the dress edges stay raised under the capelet
        m = f.m_rect(x0, 0.5, x1, 7.9)
        f.fill(m, H.lin((0, 0), (0, 8), [(0, H.tone(CYAN, 1.1)), (1, H.tone(CYAN, 0.85))]))
        H.bevel(f, m, depth=0.3, gain=0.7)
    tabard(f, 1.5, 12.2)
    sk = H.minus(f.m_band(9.0, 12.0), f.m_poly(tabard_poly(1.5, 12.2)))
    f.fill(sk, H.lin((0, 9), (0, 12), [(0, H.tone(CYAN, 1.05)), (1, H.tone(CYAN, 0.8))]))
    damask(f, sk, H.tone(CYAN, 1.3))
    H.folds(f, sk, [0.6, 1.5, 2.3, 5.7, 6.5, 7.4], depth=0.18, width=0.2, y0=9.0, y1=12.0, sway=0.1)
    H.shadow_under(f, 9.0, 0.6, 0.25)
    belt(f)
    capelet_front(f)
    collar(f)
    bow(f, 4.0, 1.9)


def body_back(cv):
    base, ov = lay(cv, "body", "back")
    ov.clear()
    f = base
    satin(f, f.m_band(0, 12), CYAN, 0, 12, top=1.02, bottom=0.78, streaks=[2.0, 6.0], strength=0.12, tint="#e8ffff")
    H.folds(f, f.m_band(9.0, 12.0), [0.7, 1.6, 2.6, 3.7, 4.4, 5.4, 6.4, 7.3], depth=0.16, width=0.2, y0=9.0, y1=12.0, sway=0.1)
    belt(f, front=False)
    collar(f)
    f = ov
    satin(f, f.m_band(0.5, 12.0), CYAN, 0.5, 12, top=1.08, bottom=0.8, streaks=[1.4, 3.2, 4.8, 6.6], strength=0.1, tint="#e8ffff")
    H.folds(f, f.m_band(0.5, 7.8), [2.6, 5.4], depth=0.08, width=0.2, y0=0.5, y1=7.8)
    H.folds(f, f.m_band(9.0, 12.0), [0.7, 1.6, 2.6, 3.7, 4.4, 5.4, 6.4, 7.3], depth=0.18, width=0.2, y0=9.0, y1=12.0, sway=0.1)
    # the capelet's round back with its scalloped edge and a big bow at the waist
    outline = [(0.0, 2.2), (0.6, 3.4), (2.0, 4.2), (4.0, 4.6), (6.0, 4.2), (7.4, 3.4), (8.0, 2.2)]
    m = f.m_poly(outline + [(8.2, 2.0), (8.2, -0.2), (-0.2, -0.2), (-0.2, 2.0)])
    scallop_edge(f, outline[1:-1], 0.5, "#ffffff", WHITE_D, edge_a=0.8, step=0.85, inside=m)
    f.fill(m, H.lin((0, 0), (0, 4.5), [(0, "#ffffff"), (0.6, WHITE), (1, H.tone(WHITE, 0.9))]))
    H.folds(f, m, [1.2, 2.6, 4.0, 5.4, 6.8], depth=0.07, width=0.2, y0=0.0, y1=4.5)
    f.polyline([(0.15, 1.8), (0.7, 3.0), (2.0, 3.8), (4.0, 4.2), (6.0, 3.8), (7.3, 3.0), (7.85, 1.8)], CYAN_B, 0.13, a=0.9)
    for x, y in ((0.7, 3.0), (2.0, 3.8), (4.0, 4.2), (6.0, 3.8), (7.3, 3.0)):
        H.stud(f, x, y + 0.2, 0.12, "#fff6fa", spec=0.6)
    collar(f)
    belt(f, front=False)
    bow(f, 4.0, 8.4, w=1.15)
    H.shadow_under(f, 4.65, 0.5, 0.18)


def body_side(cv, name):
    base, ov = lay(cv, "body", name)
    ov.clear()
    f = base
    satin(f, f.m_band(0, 12), CYAN, 0, 12, top=1.02, bottom=0.78, streaks=[1.6], strength=0.1, tint="#e8ffff")
    H.folds(f, f.m_band(9.0, 12.0), [0.6, 1.6, 2.6, 3.4], depth=0.16, width=0.2, y0=9.0, y1=12.0, sway=0.1)
    belt(f, w=4.0, front=False)
    collar(f)
    f = ov
    satin(f, f.m_band(0.5, 12), CYAN, 0.5, 12, top=1.08, bottom=0.8, streaks=[1.6], strength=0.1, tint="#e8ffff")
    H.folds(f, f.m_band(9.0, 12.0), [0.6, 1.6, 2.6, 3.4], depth=0.18, width=0.2, y0=9.0, y1=12.0, sway=0.1)
    belt(f, w=4.0, front=False)
    H.scallops(f, f.m_band(0.0, 3.4), 0.0, rows=2, cell=1.33, depth=1.9, base="#ffffff", tip="#ffffff", edge=WHITE_D, edge_a=0.6, shadow=0.2)
    collar(f)


def body(cv):
    body_front(cv)
    body_back(cv)
    body_side(cv, "west")
    body_side(cv, "east")
    for f in lay(cv, "body", "top"):
        f.fill(f.m_all(), H.lin((0, 0), (f.w, 0), [(0, "#ffffff"), (0.5, WHITE_D), (1, "#ffffff")]))
    cv.face("body", "top", True).clear()
    for f in lay(cv, "body", "bottom"):
        f.fill(f.m_all(), H.tone(CYAN_D, 0.8))
    cv.face("body", "bottom", True).clear()


# ================================================================================================ arms (slim)
def arm(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        ov.clear()
        for f in (base, ov):
            sleeve = f.m_band(0.0, 9.2)
            satin(f, sleeve, CYAN, 0, 9.2, top=1.1, bottom=0.82, streaks=[f.w * 0.3], strength=0.14, tint="#e8ffff")
            H.folds(f, sleeve, [f.w * 0.5, f.w * 0.85], depth=0.07, width=0.22, y0=1.5, y1=8.5)
            # the capelet's shoulder tabs
            H.scallops(f, f.m_band(0.0, 3.0), 0.0, rows=2, cell=1.0 if f.w < 4 else 1.33, depth=1.4, base="#ffffff", tip="#ffffff", edge=WHITE_D, edge_a=0.7, shadow=0.24)
            # the big double white ruffle cuff
            H.scallops(f, f.m_band(8.1, 10.9), 8.0, rows=2, cell=1.0 if f.w < 4 else 1.33, depth=1.5, base="#fbfeff", tip="#ffffff", edge=WHITE_D, edge_a=0.65, shadow=0.26)
            H.trim_band(f, 7.8, 8.25, CYAN_B, outline=0.4, spec=0.3)
        ov.fill(ov.m_band(10.7, 12.0, soft=1), "#000000", mode="erase")
        hand = base.m_band(10.5, 12.0)
        base.fill(hand, H.lin((0, 10.5), (0, 12), [(0, SKIN[0]), (0.4, SKIN[1]), (1, H.tone(SKIN[1], 0.95))]))
        base.shade(base.m_band(10.5, 11.0), 0.85)
        if nm in ("front", "back"):
            for x in (base.w / 3.0, 2 * base.w / 3.0):
                base.line((x, 11.35), (x, 12.0), H.tone(SKIN[0], 0.82), 0.07, 0.05, a=0.7)
    for f in lay(cv, part, "top"):
        f.fill(f.m_all(), H.rad((f.w * 0.4, f.h * 0.4), f.w, [(0, "#ffffff"), (1, WHITE_D)]))
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
            tights = f.m_band(0.0, 7.0)
            satin(f, tights, CHAR, 0, 7, top=1.2, bottom=0.82, streaks=[f.w * 0.3], strength=0.2, tint="#9ab4c4")
            # the bright cyan side line (on the outer side of each leg, and a thin one on the front's outer edge)
            if nm == "west" or nm == "east":
                x = f.w * (0.5)
                H.trim_line(f, [(x, 2.6), (x, 6.6)], CYAN_B, width=0.34, outline=0.35, spec=0.4, depth=0.12)
            if nm == "front":
                x = 0.12 if part == "leg_r" else f.w - 0.12
                H.trim_line(f, [(x, 2.8), (x, 6.6)], CYAN_B, width=0.2, outline=0.3, spec=0.3, depth=0.1)
            boot = f.m_band(7.2, 12.0)
            f.fill(boot, H.lin((0, 7.2), (0, 12), [(0, H.tone(SLATE, 1.25)), (0.5, SLATE), (1, SLATE_D)]))
            H.sheen(f, boot, [f.w * 0.28], width=0.2, strength=0.26, y0=8.0, y1=11.3, color="#c8e0ee")
            H.shadow_under(f, 7.3, 0.7, 0.3)
            if nm == "front":
                for y in (8.9, 10.3):                                                                 # two straps with silver buckles
                    H.trim_band(f, y, y + 0.55, CHAR_D, outline=0.0, spec=0.2)
                    H.stud(f, f.w * 0.5, y + 0.27, 0.27, SILVER)
            # the white folded cuff
            cuff = f.m_band(6.2, 8.0)
            satin(f, cuff, WHITE, 6.2, 8.0, top=1.05, bottom=0.86, streaks=[f.w * 0.3], strength=0.14)
            H.scallops(f, f.m_band(7.0, 8.4), 6.9, rows=1, cell=1.0 if f.w < 3 else 1.33, depth=1.2, base="#ffffff", tip="#ffffff", edge=WHITE_D, edge_a=0.7, shadow=0.3)
            sole = f.m_band(11.3, 12.0)
            f.fill(sole, H.lin((0, 11.3), (0, 12), [(0, "#141c20"), (1, "#0a1014")]))
            H.trim_line(f, [(0.0, 11.3), (f.w, 11.3)], SILVER, width=0.09, outline=0.0, spec=0.0, a=0.5)
        # the skirt on the second layer, ending in a white scalloped petticoat
        ov.fill(ov.m_band(0.0, 6.0), "#000000", mode="erase")
        bsk = base.m_band(-0.2, 1.8)                                                                   # the same skirt and petticoat hem on the base layer (kept when the second layer is dropped)
        base.fill(bsk, H.lin((0, 0), (0, 1.8), [(0, H.tone(CYAN, 1.05)), (1, H.tone(CYAN, 0.78))]))
        H.scallops(base, base.m_band(1.4, 3.4), 1.4, rows=1, cell=1.0 if base.w < 3 else 1.33, depth=1.5, base="#ffffff", tip="#ffffff", edge=WHITE_D, edge_a=0.6, shadow=0.22)
        sk = ov.m_band(-0.2, 1.8)
        ov.fill(sk, H.lin((0, 0), (0, 1.8), [(0, H.tone(CYAN, 1.05)), (1, H.tone(CYAN, 0.78))]))
        H.folds(ov, sk, [ov.w * 0.25, ov.w * 0.6, ov.w * 0.95], depth=0.2, width=0.2, y0=0.0, y1=1.8, sway=0.1)
        H.scallops(ov, ov.m_band(1.4, 4.0), 1.4, rows=2, cell=1.0 if ov.w < 3 else 1.33, depth=1.5, base="#ffffff", tip="#ffffff", edge=WHITE_D, edge_a=0.6, shadow=0.22)
    for part_ in ("leg_r", "leg_l"):
        for f in lay(cv, part_, "top"):
            f.fill(f.m_all(), H.tone(CYAN, 0.8))
        cv.face(part_, "top", True).clear()
        for f in lay(cv, part_, "bottom"):
            f.fill(f.m_all(), "#0a1014")
        cv.face(part_, "bottom", True).clear()


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.near("#d6e0e8", 0.1)], "gem": [H.near(CYAN_B, 0.1)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

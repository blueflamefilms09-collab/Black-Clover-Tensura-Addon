"""Jugram Haschwalth (Bleach, the Wandenreich's Grandmaster, Yhwach's right hand), HD 256x256 (3.38.x, group b of docs/bosses/hd_outfits_b.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/jugram_haschwalth.py) through the HD bridge; every part that shows is then drawn again at 4x detail:
the calm sharp pale-blue eyes with thin lashes and pale brows, a cold thin mouth; the white Wandenreich coat worn open (blue piping and topstitching, folds, silver buttons, an upright collar,
long tails with a blue hem) over a black vest with a glowing blue Quincy cross; a steel belt with a blue gem; layered steel pauldrons with rivets and a blue edge; white sleeves, a blue cuff
and bulky riveted steel gauntlets; charcoal trousers; flared steel boots with a blue band, plates and rivets.
Hair: `anime_bangs_long` (long pale ash-blond hair with long parted bangs, forehead and eyes visible). Original art from memory of the canon look (docs/bosses/hd_outfits_b.md).
Glow: the Quincy cross and the belt gem (Reishi blue).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

WHITE, WHITE_D = "#f2f5fb", "#c6cfdf"
BLACK = "#14182c"
BLUE, BLUE_L, BLUE_D = "#3b82dc", "#9ad6ff", "#173e80"
STEEL, STEEL_L, STEEL_D = "#8da0c0", "#d6e2f4", "#3a4a6c"
TROUS = "#2a3148"
SKIN = ("#e3c4b6", "#f4dcd0", "#fff1ea")
SIDES = ("front", "back", "west", "east")
HAIR_ID = "anime_long_open"
HAIR_RGB = (250, 240, 196)


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def steel(f, mask, y0, y1, k=1.0, streak=True):
    f.fill(mask, H.metal_paint(y0, y1, STEEL, contrast=k))
    if streak:
        H.sheen(f, mask, [f.w * 0.3, f.w * 0.78], width=0.16, strength=0.22, tilt=0.0, y0=y0, y1=y1, color=STEEL_L)


def rivet(f, x, y, r=0.17):
    H.stud(f, x, y, r, STEEL_L, rim_color=STEEL_D, spec=0.7)


def qcross(f, cx, cy, s=1.0, glow=True):
    """The Quincy cross: a long upright bar, a short cross bar, flared pointed tips and a ring at the crossing, in glowing blue."""
    bars = [f.m_rect(cx - 0.26 * s, cy - 2.0 * s, cx + 0.26 * s, cy + 2.7 * s), f.m_rect(cx - 1.55 * s, cy - 0.55 * s, cx + 1.55 * s, cy + 0.0 * s)]
    tips = [f.m_poly([(cx, cy - 2.75 * s), (cx + 0.55 * s, cy - 1.9 * s), (cx - 0.55 * s, cy - 1.9 * s)]),
            f.m_poly([(cx, cy + 3.35 * s), (cx + 0.55 * s, cy + 2.55 * s), (cx - 0.55 * s, cy + 2.55 * s)]),
            f.m_poly([(cx - 2.2 * s, cy - 0.27 * s), (cx - 1.4 * s, cy - 0.85 * s), (cx - 1.4 * s, cy + 0.3 * s)]),
            f.m_poly([(cx + 2.2 * s, cy - 0.27 * s), (cx + 1.4 * s, cy - 0.85 * s), (cx + 1.4 * s, cy + 0.3 * s)])]
    m = H.union(*(bars + tips))
    f.fill(f.m_ellipse(cx, cy, 2.6 * s, 2.9 * s, soft=7.0), BLUE, 0.12)
    f.fill(m, H.lin((0, cy - 2.7 * s), (0, cy + 3.3 * s), [(0, BLUE_L), (0.5, BLUE), (1, BLUE_D)]), glow=glow)
    H.bevel(f, m, depth=0.2 * s, gain=0.8, spec=0.5)
    ring = f.m_ring(cx, cy - 0.27 * s, 0.62 * s, 0.14 * s)
    f.fill(ring, BLUE_L, 1.0, glow=glow)
    f.fill(f.m_ellipse(cx, cy - 0.27 * s, 0.28 * s, 0.28 * s), "#ffffff", 0.95, glow=glow)


# ================================================================================================ head
def head(cv, hair):
    f = cv.face("head", "front", False)
    H.skin_face(f, SKIN, hair=H.tone(hair, 0.9), hairline=1.55, wave=0.5, shadow=0.18)
    iris = ("#27468a", "#6a9ee0", "#d2e8ff")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.95, 2.7, 1.95, iris, lash="#3a3850", tilt=0.22, flick=0.38, lash_w=0.30, lid_shadow=0.34)
        f.fill(f.m_ellipse(cx, 4.1, 1.4, 0.35, soft=5.0), "#9aa0c8", 0.18)                       # a cool shade over the lid
    H.brow(f, (3.3, 3.5), (0.9, 2.95), "#c8bc92", 0.19, 0.07, arch=0.14)
    H.brow(f, (4.7, 3.5), (7.1, 2.95), "#c8bc92", 0.19, 0.07, arch=0.14)
    H.nose(f, 4.0, 6.3, "#d2a898", 0.18)
    H.mouth_smile(f, 4.0, 7.15, 1.9, color="#9a6468", lip="#cc9490", depth=0.04, thick=0.15)
    f.fill(f.m_band(6.0, 6.25, x0=3.7, x1=4.3, soft=2.0), "#ffffff", 0.1)
    # the Wandenreich collar band round the neck on the hat layer is not used; the hat layer stays clear for the hair
    for nm in SIDES + ("top", "bottom"):
        cv.face("head", nm, True).clear()


# ================================================================================================ body
def collar(f, w=8.0, front=False):
    """The upright white collar: a blue-piped stand with a steel clasp."""
    if front:
        for x0, x1, sg in ((0.0, 2.7, 1), (5.3, 8.0, -1)):
            m = f.m_poly([(x0, -0.1), (x1, -0.1), (x1 - sg * 0.1, 1.6), (x0, 1.45)] if sg == 1 else [(x0, -0.1), (x1, -0.1), (x1, 1.45), (x0 - sg * 0.1, 1.6)])
            f.fill(m, H.lin((0, 0), (0, 1.5), [(0, "#ffffff"), (1, H.tone(WHITE, 0.86))]))
            H.bevel(f, m, depth=0.25, gain=0.8, spec=0.3)
            H.trim_line(f, [(x0 if sg == -1 else x0 + 0.02, 1.45), (x1, 1.55)] if sg == 1 else [(x0, 1.55), (x1, 1.45)], BLUE, width=0.2, outline=0.4, spec=0.4, depth=0.1)
            H.trim_line(f, [(x1 - sg * 0.12, 0.1), (x1 - sg * 0.12, 1.5)], BLUE, width=0.2, outline=0.4, spec=0.4, depth=0.1)
    else:
        m = f.m_band(0.0, 1.5)
        f.fill(m, H.lin((0, 0), (0, 1.5), [(0, "#ffffff"), (1, H.tone(WHITE, 0.86))]))
        H.bevel(f, m, depth=0.25, gain=0.7, spec=0.3)
        H.trim_line(f, [(0.0, 1.5), (w, 1.5)], BLUE, width=0.22, outline=0.4, spec=0.4, depth=0.1)
        H.trim_line(f, [(0.0, 0.1), (w, 0.1)], BLUE, width=0.18, outline=0.3, spec=0.3, depth=0.1)


def belt(f, w=8.0, front=True):
    b = f.m_band(7.9, 9.15, x0=0.0, x1=w)
    steel(f, b, 7.9, 9.15, k=1.1)
    H.bevel(f, b, depth=0.3, gain=1.0, spec=0.5)
    for x in ([0.7, 1.5, 6.5, 7.3] if front else [0.8, 2.4, 4.0, 5.6, 7.2]):
        if x < w:
            rivet(f, x, 8.52, 0.15)
    H.shadow_under(f, 9.15, 0.8, 0.28)
    if front and w >= 7.5:
        plate = f.m_rect(2.7, 7.5, 5.3, 9.5, 0.4)
        f.fill(f.m_rect(2.62, 7.42, 5.38, 9.58, 0.45), H.tone(STEEL_D, 0.7), 0.8)
        steel(f, plate, 7.5, 9.5, k=1.2, streak=False)
        H.bevel(f, plate, depth=0.3, gain=1.2, spec=0.6)
        H.gem(f, 4.0, 8.52, 0.5, BLUE, setting=STEEL_L, glow=True)


def coat_panel(f, x0, x1, y0, y1, edge_x, sg, top_round=False):
    """One white coat panel (x0..x1) whose open edge (the one at edge_x) carries a blue piping with topstitching."""
    m = f.m_rect(x0, y0, x1, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(WHITE, 1.05)), (0.55, WHITE), (1, H.tone(WHITE, 0.82))]))
    n = max(2, int((x1 - x0) / 0.85))
    H.folds(f, m, [x0 + (x1 - x0) * (i + 0.5) / n for i in range(n)], depth=0.1, width=0.2, y0=y0, y1=y1, sway=0.14)
    H.sheen(f, m, [(x0 + x1) / 2.0], width=0.2, strength=0.12, y0=y0 + 0.5, y1=y1 - 0.5)
    H.trim_line(f, [(edge_x, y0), (edge_x, y1)], BLUE, width=0.3, outline=0.5, spec=0.5, depth=0.13)
    H.stitch(f, [(edge_x - sg * 0.32, y0 + 0.1), (edge_x - sg * 0.32, y1 - 0.1)], "#9ab4e0", width=0.05, dash=(0.28, 0.2), shadow=0.1)
    y = y0 + 1.1
    while y < y1 - 0.9:                                                                                # a chain of small blue diamonds (embroidery)
        cx = edge_x - sg * 0.72
        f.poly([(cx, y - 0.3), (cx + 0.2, y), (cx, y + 0.3), (cx - 0.2, y)], BLUE, a=0.85)
        y += 1.3
    return m


def body_front(cv):
    base, ov = lay(cv, "body", "front")
    ov.clear()
    f = base
    vest = f.m_band(0, 9.0)
    f.fill(vest, H.lin((0, 0), (0, 9), [(0, H.tone(BLACK, 1.12)), (1, H.tone(BLACK, 0.8))]))
    H.folds(f, vest, [2.9, 5.1], depth=0.1, width=0.25, y0=0.5, y1=8.0)
    for x in (2.15, 5.85):                                                                              # the vest's own blue edge
        H.trim_line(f, [(x, 0.4), (x, 7.8)], BLUE_D, width=0.14, outline=0.0, spec=0.0, a=0.9)
    qcross(f, 4.0, 2.5, 1.0)
    tr = f.m_band(9.0, 12.0)
    f.fill(tr, H.lin((0, 9), (0, 12), [(0, H.tone(TROUS, 1.1)), (1, H.tone(TROUS, 0.8))]))
    H.folds(f, tr, [3.4, 4.6], depth=0.1, width=0.2, y0=9.0, y1=12.0)
    coat_panel(f, 0.0, 2.2, 0.7, 12.2, 2.2, 1)                                                          # the coat edges are on the base layer too (the second layer is dropped far away)
    coat_panel(f, 5.8, 8.0, 0.7, 12.2, 5.8, -1)
    belt(f)
    collar(f, front=True)
    f = ov
    coat_panel(f, 0.0, 2.2, 0.7, 12.2, 2.2, 1)
    coat_panel(f, 5.8, 8.0, 0.7, 12.2, 5.8, -1)
    for x in (1.0, 7.0):                                                                                # silver buttons on the coat panels
        for y in (3.1, 5.0, 6.9):
            H.stud(f, x, y, 0.2, STEEL_L, rim_color=STEEL_D)
    belt(f)
    collar(f, front=True)
    for g in (base, ov):
        H.trim_line(g, [(0.0, 11.85), (8.0, 11.85)], BLUE, width=0.2, outline=0.3, spec=0.3, depth=0.1)


def body_back(cv):
    base, ov = lay(cv, "body", "back")
    ov.clear()
    base.fill(base.m_band(0, 12), H.lin((0, 0), (0, 12), [(0, H.tone(BLACK, 1.1)), (1, H.tone(TROUS, 0.9))]))
    for f in (base, ov):                                                                               # the coat back is on both layers (the second layer is dropped far away)
        m = f.m_band(0.5, 12.2)
        f.fill(m, H.lin((0, 0.5), (0, 12.2), [(0, H.tone(WHITE, 1.05)), (0.55, WHITE), (1, H.tone(WHITE, 0.8))]))
        H.folds(f, m, [1.0, 2.0, 3.0, 5.0, 6.0, 7.0], depth=0.1, width=0.22, y0=0.5, y1=12.2, sway=0.16)
        H.sheen(f, m, [2.0, 6.0], width=0.24, strength=0.1, y0=1.0, y1=11.0)
        # the back vent (a dark seam with a blue piping) and a faint embroidered cross
        H.trim_line(f, [(4.0, 8.9), (4.0, 12.2)], BLUE, width=0.16, outline=0.4, spec=0.3, depth=0.1)
        f.fill(f.m_rect(3.88, 9.0, 4.12, 12.2), H.tone(WHITE_D, 0.55), 0.8)
        qcross(f, 4.0, 3.5, 0.8, glow=None)
        belt(f, front=False)
        collar(f, front=False)
        H.trim_line(f, [(0.0, 11.85), (8.0, 11.85)], BLUE, width=0.2, outline=0.3, spec=0.3, depth=0.1)


def body_side(cv, name):
    base, ov = lay(cv, "body", name)
    ov.clear()
    base.fill(base.m_band(0, 12), H.lin((0, 0), (0, 12), [(0, H.tone(BLACK, 1.1)), (1, H.tone(TROUS, 0.9))]))
    for f in (base, ov):
        m = f.m_band(0.5, 12.2)
        f.fill(m, H.lin((0, 0.5), (0, 12.2), [(0, H.tone(WHITE, 1.0)), (0.55, H.tone(WHITE, 0.94)), (1, H.tone(WHITE, 0.78))]))
        H.folds(f, m, [0.8, 1.8, 2.8], depth=0.1, width=0.2, y0=0.5, y1=12.2, sway=0.14)
        fx = 3.75 if name == "west" else 0.25                                                          # the front edge of the coat carries the blue piping
        H.trim_line(f, [(fx, 0.5), (fx, 12.2)], BLUE, width=0.3, outline=0.5, spec=0.5, depth=0.13)
        belt(f, w=4.0, front=False)
        collar(f, w=4.0)
        H.trim_line(f, [(0.0, 11.85), (4.0, 11.85)], BLUE, width=0.2, outline=0.3, spec=0.3, depth=0.1)


def body(cv):
    body_front(cv)
    body_back(cv)
    body_side(cv, "west")
    body_side(cv, "east")
    for f in lay(cv, "body", "top"):
        f.fill(f.m_all(), H.lin((0, 0), (f.w, 0), [(0, "#ffffff"), (0.5, WHITE_D), (1, "#ffffff")]))
    cv.face("body", "top", True).clear()
    for f in lay(cv, "body", "bottom"):
        f.fill(f.m_all(), "#10142a")
    cv.face("body", "bottom", True).clear()


# ================================================================================================ arms (wide)
def pauldron(f, nm):
    """Three overlapping steel lames (round-bottomed, each throwing a shadow on the one below) with a raised centre rib, a blue enamel inlay and rivets; a dark mail joint under them."""
    w = f.w
    f.fill(f.m_band(3.3, 4.2), H.lin((0, 3.3), (0, 4.2), [(0, "#252c44"), (1, "#141a2c")]))
    for xx in np.arange(0.2, w, 0.55):                                                                 # the mail rings of the joint
        f.fill(f.m_ellipse(xx + 0.27, 3.75, 0.2, 0.13), "#3a4666", 0.55)
    for i, (y0, y1) in enumerate(((0.0, 1.5), (1.15, 2.7), (2.35, 3.75))):
        m = H.union(f.m_rect(-0.1, y0, w + 0.1, y1 - 0.35, 0.1), f.m_ellipse(w / 2.0, y1 - 0.5, w * 0.62, 0.55))
        shadow = H.union(f.m_rect(-0.1, y0 + 0.12, w + 0.1, y1 - 0.2, 0.1), f.m_ellipse(w / 2.0, y1 - 0.38, w * 0.62, 0.62))
        f.fill(shadow, H.tone(STEEL_D, 0.45), 0.65)
        steel(f, m, y0, y1, k=1.2, streak=False)
        H.bevel(f, m, depth=0.3, gain=1.25, spec=0.6)
        f.fill(f.m_rect(w / 2.0 - 0.14, y0 + 0.1, w / 2.0 + 0.14, y1 - 0.15) * m, STEEL_L, 0.5)           # the raised rib
        if i == 1:
            f.poly([(w / 2.0, y0 + 0.35), (w / 2.0 + 0.5, (y0 + y1) / 2.0 - 0.05), (w / 2.0, y1 - 0.55), (w / 2.0 - 0.5, (y0 + y1) / 2.0 - 0.05)], BLUE, a=0.95)
            f.ellipse(w / 2.0, (y0 + y1) / 2.0 - 0.08, 0.1, 0.1, "#ffffff", a=0.9)
        elif nm in ("front", "back") and w > 3:
            rivet(f, w * 0.2, (y0 + y1) / 2.0 - 0.1, 0.12)
            rivet(f, w * 0.8, (y0 + y1) / 2.0 - 0.1, 0.12)
    H.trim_line(f, [(0.2, 3.62), (w - 0.2, 3.62)], BLUE, width=0.18, outline=0.3, spec=0.5, depth=0.1)


def gauntlet(f, nm, y0=8.0, y1=11.4):
    m = f.m_band(y0, y1)
    steel(f, m, y0, y1, k=1.2)
    H.bevel(f, m, depth=0.3, gain=0.9, spec=0.4)
    for y in (y0 + 1.25, y0 + 2.2):                                                                    # plate seams
        H.trim_line(f, [(0.0, y), (f.w, y)], STEEL_D, width=0.11, outline=0.0, spec=0.0, a=0.85)
    for x in ([f.w * 0.22, f.w * 0.78] if f.w > 3 else [f.w * 0.5]):
        rivet(f, x, y0 + 0.62, 0.15)
    if nm == "front":                                                                                  # knuckle plate
        k = f.m_rect(0.4, y1 - 1.1, f.w - 0.4, y1, 0.2)
        f.fill(f.m_rect(0.35, y1 - 1.15, f.w - 0.35, y1 + 0.05, 0.22), H.tone(STEEL_D, 0.5), 0.7)
        steel(f, k, y1 - 1.1, y1, k=1.2, streak=False)
        H.bevel(f, k, depth=0.2, gain=1.1, spec=0.5)
        for x in (f.w * 0.3, f.w * 0.5, f.w * 0.7):
            f.line((x, y1 - 1.0), (x, y1 - 0.15), H.tone(STEEL_D, 0.7), 0.07, 0.07, a=0.7)


def arm(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        ov.clear()
        for f in (base, ov):
            sl = f.m_band(3.5, 7.6)
            f.fill(sl, H.lin((0, 3.5), (0, 7.6), [(0, H.tone(WHITE, 1.0)), (1, H.tone(WHITE, 0.82))]))
            H.folds(f, sl, [f.w * 0.3, f.w * 0.68], depth=0.1, width=0.22, y0=3.5, y1=7.6, sway=0.14)
            pauldron(f, nm)
            H.trim_band(f, 7.0, 8.0, BLUE, outline=0.5, spec=0.5)
            f.fill(f.m_band(7.15, 7.3), BLUE_L, 0.55)
            gauntlet(f, nm)
        ov.fill(ov.m_band(11.3, 12.0), "#000000", mode="erase")
        base.fill(base.m_band(11.3, 12.0), H.lin((0, 11.3), (0, 12), [(0, SKIN[0]), (1, SKIN[1])]))
        base.shade(base.m_band(11.3, 11.7), 0.85)
        if nm in ("front", "back"):
            for x in (base.w * 0.25, base.w * 0.5, base.w * 0.75):
                base.line((x, 11.7), (x, 12.0), H.tone(SKIN[0], 0.82), 0.07, 0.05, a=0.7)
    for f in lay(cv, part, "top"):
        m = f.m_all()
        steel(f, m, 0, f.h, k=1.0, streak=False)
        H.bevel(f, f.m_rect(0.0, 0.0, f.w, f.h, 0.4), depth=0.4, gain=0.9, spec=0.4)
        rivet(f, f.w / 2.0, f.h / 2.0, 0.3)
        H.trim_line(f, [(0.0, f.h - 0.1), (f.w, f.h - 0.1)], BLUE, width=0.16, outline=0.0, spec=0.0)
    cv.face(part, "top", True).clear()
    for f in lay(cv, part, "bottom"):
        f.fill(f.m_all(), SKIN[0])
    cv.face(part, "bottom", True).clear()


# ================================================================================================ legs
def boot(f, nm):
    """The flared steel boot: a wide cuff lip, plates, rivets, a blue band, a dark sole."""
    m = f.m_band(7.0, 12.0)
    f.fill(m, H.lin((0, 7.0), (0, 12), [(0, H.tone(STEEL, 1.15)), (0.5, STEEL), (1, H.tone(STEEL, 0.62))]))
    H.sheen(f, m, [f.w * 0.28, f.w * 0.75], width=0.16, strength=0.22, y0=7.4, y1=11.4, color=STEEL_L)
    lip = f.m_band(6.5, 8.0)
    f.fill(lip, H.metal_paint(6.5, 8.0, STEEL, contrast=1.2))
    H.bevel(f, lip, depth=0.35, gain=1.3, spec=0.7)
    H.trim_line(f, [(0.0, 6.62), (f.w, 6.62)], BLUE, width=0.2, outline=0.4, spec=0.5, depth=0.1)
    H.shadow_under(f, 8.0, 0.7, 0.3)
    for y in (9.4, 10.7):
        H.trim_line(f, [(0.0, y), (f.w, y)], STEEL_D, width=0.11, outline=0.0, spec=0.0, a=0.85)
    for x in ((f.w * 0.25, f.w * 0.75)):
        rivet(f, x, 8.75, 0.14)
        rivet(f, x, 10.05, 0.14)
    if nm == "front":
        tc = f.m_rect(0.3, 10.65, f.w - 0.3, 11.35, 0.3)
        steel(f, tc, 10.65, 11.35, k=1.2, streak=False)
        H.bevel(f, tc, depth=0.2, gain=1.1, spec=0.5)
    sole = f.m_band(11.35, 12.0)
    f.fill(sole, H.lin((0, 11.35), (0, 12), [(0, "#1a2034"), (1, "#0a0e1a")]))


def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        ov.clear()
        f = base
        tr = f.m_band(0.0, 7.4)
        f.fill(tr, H.lin((0, 0), (0, 7.4), [(0, H.tone(TROUS, 1.15)), (1, H.tone(TROUS, 0.78))]))
        H.folds(f, tr, [f.w * 0.35, f.w * 0.75], depth=0.08, width=0.2, y0=0.0, y1=7.4)
        H.sheen(f, tr, [f.w * 0.3], width=0.2, strength=0.12, y0=0.5, y1=6.5, color="#8aa0d0")
        boot(base, nm)
        boot(ov, nm)
        ov.fill(ov.m_band(0.0, 6.5), "#000000", mode="erase")
        # the coat tails (white, blue hem; the inner column of each front face is open so the trousers show), on both layers
        inner_open = (nm == "front")
        for g in (base, ov):
            x0, x1 = 0.0, g.w
            if inner_open:
                if part == "leg_r":
                    x1 = g.w - 0.9
                else:
                    x0 = 0.9
            tail = g.m_rect(x0, -0.1, x1, 5.9)
            g.fill(tail, H.lin((0, 0), (0, 5.9), [(0, H.tone(WHITE, 1.02)), (0.6, H.tone(WHITE, 0.92)), (1, H.tone(WHITE, 0.78))]))
            H.folds(g, tail, [x0 + (x1 - x0) * 0.3, x0 + (x1 - x0) * 0.7], depth=0.12, width=0.22, y0=0.0, y1=5.9, sway=0.14)
            hm = g.m_rect(x0, 5.1, x1, 6.0)
            g.fill(hm, H.lin((0, 5.1), (0, 6.0), [(0, BLUE_L), (0.35, BLUE), (1, BLUE_D)]))
            H.bevel(g, hm, depth=0.25, gain=1.0, spec=0.5)
            if inner_open:
                edge = x1 if part == "leg_r" else x0
                H.trim_line(g, [(edge, 0.0), (edge, 5.1)], BLUE, width=0.26, outline=0.5, spec=0.5, depth=0.12)
            H.shadow_under(g, 6.0, 0.6, 0.22)
    for part_ in ("leg_r", "leg_l"):
        for f in lay(cv, part_, "top"):
            f.fill(f.m_all(), "#10142a")
        cv.face(part_, "top", True).clear()
        for f in lay(cv, part_, "bottom"):
            f.fill(f.m_all(), "#0a0e1a")
        cv.face(part_, "bottom", True).clear()


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    if HAIR_ID in masks:
        hair = HAIR_ID
    hrgb = HAIR_RGB
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.near("#8da0c0", 0.14), H.near("#d6e2f4", 0.12)], "gem": [H.near(BLUE, 0.12)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

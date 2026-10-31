"""Julius Novachrono, the Wizard King, HD 256x256 (docs/arc/hd_arc_bc4.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_julius_novachrono.py): a floor-length white robe with gold trim and a wide collar, wide sleeves with gold cuffs, a gold sash with a
pale buckle, pale trousers and ankle boots. The white mantle with the gold clover, the gold tassel that hangs from the sash and the drum of loose pages (his coverless grimoire, turning at his
right hip) are the `pmx_julius_novachrono` extra (the mantle and its lower half cover the whole back down to the hem; the tassel hangs in front of the sash at the left hip, so the sash is
painted with a knot there and nothing else; the drum floats to the right of the body). No glow on the skin (the glow is the gold bands of the drum). The sheet's second layer covers the whole
robe and both sleeves down to the hem.
Drawn at 4x detail on both layers: the warm amber eyes with a gentle lid line, the raised light brows, the calm smile with a light blush, the pale curtains of the fringe that frame a bare
forehead; the robe with soft folds, gold-embroidered borders and a clover-vine pattern, the wide gold-edged collar with a V of skin and a gold clover brooch, the gold sash with its pale buckle and
the knot for the tassel, the wide sleeves with deep folds and big gold cuffs, the pleated skirt with its gold hem.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus docs/arc/npcb.md: a tall, easy-going
man with pale hair and a warm smile, the white-and-gold robe of the Wizard King and the Clover Kingdom's clover. The colours of the robe are the sheet's (a choice, no source gives them); the
embroidery, the brooch and the knot are added in the HD pass.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc4 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_long_open"                                # medium-long pale hair that frames the face with no centre fringe
HAIR = "#fff6d2"                                              # pale cream, lifted: the hair sheet is grey and the tint multiplies (the sheet's #e0cf9a went khaki-grey)
HAIR_RGB = (255, 246, 210)
SKIN = ("#e4b8a0", "#f4d0b8", "#fce4d0")
ROBE = "#f4efe2"
ROBE_D = "#d3cab4"
LINING = "#e3d6b4"
GOLD = "#dcb450"
GOLD_D = "#8a6a20"
SASH = "#d8ac44"
TROUS = "#e8e0c8"
BOOT = "#5a4a30"
IRIS = ("#6a4a0c", "#d6a640", "#fff1b8")
EMB = "#e0bc5c"                                               # the gold embroidery on the white


# ================================================================================================ small helpers
def clover(f, cx, cy, r, color=GOLD, dark=GOLD_D):
    """A four-leaf clover (the Clover Kingdom's): four round leaves and a short stem."""
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        f.ellipse(cx + dx * r * 0.55, cy + dy * r * 0.55, r * 0.58, r * 0.58, dark, a=0.9)
    for dx, dy in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        f.ellipse(cx + dx * r * 0.55, cy + dy * r * 0.55, r * 0.48, r * 0.48, color)
    f.line((cx, cy + r * 0.4), (cx + r * 0.5, cy + r * 1.5), dark, 0.2, 0.14)
    f.ellipse(cx - r * 0.7, cy - r * 0.75, r * 0.2, r * 0.14, "#fff3c0", a=0.8)


def vine(f, x, y0, y1, side=1.0, a=0.8, step=1.15):
    """A thin gold vine running down a border with small leaves (the embroidery on the white of the robe)."""
    pts = [(x + side * 0.06 * ((i % 2) * 2 - 1), y0 + (y1 - y0) * i / 12) for i in range(13)]
    K.line(f, pts, EMB, 0.2, a)
    y = y0 + 0.5
    k = 0
    while y < y1 - 0.3:
        s = side if k % 2 == 0 else -side
        f.fill(f.m_ellipse(x + s * 0.28, y, 0.26, 0.14, rot=0.6 * s), EMB, a)
        y += step
        k += 1


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.08, chin=0.06)
    pal = K.hair_pal(HAIR, light_k=1.04, dark_k=0.62)
    H.blush(f, 1.25, 6.3, 0.95, 0.5, "#f09a92", 0.34)
    H.blush(f, 6.75, 6.3, 0.95, 0.5, "#f09a92", 0.34)
    # raised, light brows
    brow = "#bba878"
    H.brow(f, (3.5, 3.95), (1.3, 3.5), brow, 0.3, 0.14, arch=0.14)
    H.brow(f, (4.5, 3.95), (6.7, 3.5), brow, 0.3, 0.14, arch=0.14)
    # warm amber eyes, the outer corners a little low: a gentle, smiling look; a soft lower lid line lifts with the smile
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.15, 2.4, 1.7, IRIS, lash="#4a3a24", tilt=-0.12, lash_w=0.36, flick=0.24, lid_shadow=0.28, hi=0.9)
        f.fill(f.m_polyline([(cx - 1.0, 6.25), (cx, 6.45), (cx + 1.0, 6.25)], 0.12), H.tone(SKIN[0], 0.75), 0.4)
    H.nose(f, 4.0, 6.3, "#cf9c86", 0.2)
    H.mouth_smile(f, 4.0, 6.95, 2.6, color="#a45a56", lip="#e29a92", depth=0.34, thick=0.18)
    # the hair: two pale curtains parted in the middle, a bare forehead; long locks fall beside the face
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.6), (7.3, 2.1), (6.4, 1.2), (5.3, 0.8), (4.0, 0.55), (2.7, 0.8), (1.6, 1.2), (0.7, 2.1), (-0.3, 2.6)], y_bot=2.4)
    K.strands(f, pal, (0.8, 1.9, 3.2, 4.8, 6.1, 7.2), 0.0, 1.5, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.9, (1.5, 1.9, 1.1, 3.0, 0.45)),                  # the left curtain sweeps from the parting to the temple
        (0.9, (6.5, 1.9, 1.1, 3.0, -0.45)),
        (0.95, (2.6, 1.3, 0.8, 1.6, 0.5)),
        (0.95, (5.4, 1.3, 0.8, 1.6, -0.5)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.45, 1.2, 0.8, 5.6, 0.3, 1.0)
    K.lock(f, pal, 7.55, 1.2, 0.8, 5.6, -0.3, 1.0)


# ================================================================================================ the robe
def collar(f, name):
    """The wide collar: a white band with a gold edge and a V of skin at the throat; a gold clover brooch on the front."""
    m = f.m_band(0.0, 1.45)
    K.cloth(f, m, ROBE, top=1.1, bottom=0.88, round_=0.08)
    f.fill(f.m_rect(0.0, 1.32, f.w, 1.5), GOLD, 1.0)
    f.fill(f.m_rect(0.0, 1.32, f.w, 1.38), K.lit(GOLD, 1.5), 0.9)
    f.fill(f.m_rect(0.0, 1.5, f.w, 1.6), GOLD_D, 0.6)
    H.shadow_under(f, 1.52, 0.5, 0.22)
    K.seam(f, [(0.15, 1.0), (f.w - 0.15, 1.0)], EMB, 0.05, 0.6)
    if name == "front":
        v = f.m_poly([(3.0, 0.0), (5.0, 0.0), (4.0, 2.5)])
        f.fill(v, H.lin((0, 0), (0, 2.5), [(0, SKIN[2]), (1, SKIN[1])]))
        f.fill(f.m_polyline([(3.0, 0.0), (4.0, 2.5), (5.0, 0.0)], 0.22), GOLD, 1.0)
        f.fill(f.m_ellipse(4.0, 0.3, 0.9, 0.6, soft=2.0) * v, H.tone(SKIN[0], 0.8), 0.4)
        clover(f, 4.0, 3.1, 0.38)


def robe_front(f, ov):
    K.cloth(f, f.m_all(), ROBE, top=1.1, bottom=0.82, round_=0.12, fold_xs=[1.2, 2.3, 5.7, 6.8], fold_depth=0.12, fold_y0=2.0, fold_y1=8.6)
    K.sheen(f, f.m_rect(0, 1.6, 8, 8.8), [1.0, 7.0], 2.0, 8.2, 0.2, "#ffffff")
    # the opening of the robe: an inner tunic between two gold borders
    op = f.m_poly([(3.4, 2.3), (4.6, 2.3), (4.75, 12.0), (3.25, 12.0)])
    K.cloth(f, op, LINING, top=1.05, bottom=0.88, round_=0.04)
    for x, s in ((3.35, -1.0), (4.65, 1.0)):
        H.trim_line(f, [(x - s * 0.0, 2.2), (x + s * 0.12, 12.0)], GOLD, width=0.34, spec=0.5, outline=0.55)
        vine(f, x + s * 0.55, 3.0, 8.4, s, 0.75)
    # a gold border runs down the outer edges as well
    for x in (0.18, 7.82):
        K.line(f, [(x, 1.7), (x, 8.8)], EMB, 0.2, 0.55)
    collar(f, "front")
    # the sash with its pale buckle and the knot where the tassel hangs (the tassel is the extra)
    f.fill(f.m_rect(0.0, 8.8, f.w, 10.45), H.lin((0, 8.8), (0, 10.45), [(0, K.lit(SASH, 1.25)), (0.4, SASH), (1, H.tone(SASH, 0.7))]))
    f.fill(f.m_rect(0.0, 8.8, f.w, 8.95), K.lit(SASH, 1.8), 0.6)
    f.fill(f.m_rect(0.0, 10.3, f.w, 10.45), "#4a3410", 0.6)
    K.seam(f, [(0.0, 9.15), (f.w, 9.15)], "#f4d680", 0.05, 0.5, (0.3, 0.18), 0.2)
    K.seam(f, [(0.0, 10.1), (f.w, 10.1)], "#f4d680", 0.05, 0.5, (0.3, 0.18), 0.2)
    H.shadow_under(f, 10.45, 0.6, 0.3)
    K.buckle(f, 4.0, 9.6, 1.9, 1.5, "#fff2b8", hole="#6a4a10", r=0.5)
    f.ellipse(6.9, 9.6, 0.55, 0.62, H.tone(SASH, 0.85))                                                  # the knot
    f.ellipse(6.9, 9.6, 0.42, 0.5, K.lit(SASH, 1.2))
    f.line((6.55, 9.35), (7.3, 9.85), H.tone(SASH, 0.6), 0.14, 0.12)
    f.ellipse(6.78, 9.4, 0.14, 0.1, "#fff3c0", a=0.8)
    skirt(f, 10.45, 12.0, "front")


def skirt(f, y0, y1, name):
    """The robe below the sash: soft pleats, a slightly darker cloth towards the hem."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(ROBE, 0.92)), (1, H.tone(ROBE, 0.8))]))
    H.folds(f, m, [f.w * 0.2, f.w * 0.4, f.w * 0.6, f.w * 0.8], depth=0.13, width=0.26, y0=y0, y1=y1)


def robe_back(f, ov):
    K.cloth(f, f.m_all(), ROBE, top=1.08, bottom=0.82, round_=0.12, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.10, fold_y0=2.0, fold_y1=8.6)
    collar(f, "back")
    f.fill(f.m_line((4.0, 1.9), (4.0, 8.7), 0.2, 0.2), K.deep(ROBE, 0.7), 0.5)
    K.seam(f, [(3.62, 2.0), (3.62, 8.6)], EMB, 0.05, 0.4)
    f.fill(f.m_rect(0.0, 8.8, f.w, 10.45), H.lin((0, 8.8), (0, 10.45), [(0, K.lit(SASH, 1.25)), (0.4, SASH), (1, H.tone(SASH, 0.7))]))
    f.fill(f.m_rect(0.0, 8.8, f.w, 8.95), K.lit(SASH, 1.8), 0.6)
    f.fill(f.m_rect(0.0, 10.3, f.w, 10.45), "#4a3410", 0.6)
    skirt(f, 10.45, 12.0, "back")


def robe_side(f, ov, name):
    K.cloth(f, f.m_all(), ROBE, top=1.08, bottom=0.82, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.10, fold_y0=2.0, fold_y1=8.6)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.line(f, [(ed, 2.0), (ed, 8.6)], EMB, 0.2, 0.5)
    f.fill(f.m_rect(0.0, 8.8, f.w, 10.45), H.lin((0, 8.8), (0, 10.45), [(0, K.lit(SASH, 1.25)), (0.4, SASH), (1, H.tone(SASH, 0.7))]))
    f.fill(f.m_rect(0.0, 8.8, f.w, 8.95), K.lit(SASH, 1.8), 0.6)
    f.fill(f.m_rect(0.0, 10.3, f.w, 10.45), "#4a3410", 0.6)
    skirt(f, 10.45, 12.0, name)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                robe_front(f, isov)
            elif nm == "back":
                robe_back(f, isov)
            else:
                robe_side(f, isov, nm)


# ================================================================================================ the arms and legs
def sleeve(f, name):
    """A wide sleeve to the wrist: deep soft folds, a gold line along the shoulder seam, a broad gold cuff, the lining showing under it."""
    m = f.m_band(0.0, 12.0)
    K.cloth(f, m, ROBE, top=1.1, bottom=0.8, round_=0.16, fold_xs=[0.8, 1.7, 2.7], fold_depth=0.13, fold_y0=1.2, fold_y1=10.0)
    K.sheen(f, m, [f.w * 0.3], 1.8, 9.2, 0.2, "#ffffff")
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.32), K.lit(ROBE, 1.15), 0.5)
    K.seam(f, [(0.0, 0.7), (f.w, 0.7)], EMB, 0.05, 0.55)                                                # the shoulder seam
    for y, k in ((5.2, 0.55), (5.8, 0.35)):                                                              # the elbow creases
        f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.16) * m, K.deep(ROBE, 0.62), k)
    if name in ("front", "back"):
        vine(f, f.w * 0.5, 1.6, 7.0, 1.0, 0.55, 1.3)
    H.shadow_under(f, 8.7, 0.6, 0.34)
    # the cuff: a broad gold band that flares, with a darker lining below it
    H.trim_band(f, 8.7, 10.5, GOLD, spec=0.45, outline=0.6)
    K.seam(f, [(0.12, 9.6), (f.w - 0.12, 9.6)], "#fff0b0", 0.05, 0.7, (0.3, 0.18), 0.2)
    for x in (0.6, f.w - 0.6):
        H.stud(f, x, 9.6, 0.15, "#fff4c0", spec=0.7)
    H.shadow_under(f, 10.5, 0.5, 0.3)
    low = f.m_band(10.5, 12.0)
    K.cloth(f, low, LINING, top=1.05, bottom=0.78, round_=0.12, fold_xs=[1.0, 2.2], fold_depth=0.12, fold_y0=10.6, fold_y1=12.0)
    f.fill(f.m_band(11.75, 12.0), "#7a6a44", 0.5)


def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve(f, nm)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", ROBE, edge=EMB, k_top=1.1, overlay=ov)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                m = f.m_band(0.0, 12.0)
                K.cloth(f, m, ROBE, top=1.0, bottom=0.78, round_=0.14, fold_xs=[1.0, 2.0, 3.0], fold_depth=0.14, fold_y0=0.0, fold_y1=10.8)
                K.sheen(f, m, [f.w * 0.3], 0.6, 10.0, 0.14, "#ffffff")
                if nm == "front":
                    vine(f, f.w * 0.5, 1.0, 9.6, 1.0, 0.5, 1.4)
                # the hem: a gold band, a shadow above it, the toe of the boot peeking below
                H.shadow_under(f, 10.2, 0.7, 0.3)
                H.trim_band(f, 10.2, 11.35, GOLD, spec=0.4, outline=0.6)
                K.seam(f, [(0.12, 10.78), (f.w - 0.12, 10.78)], "#fff0b0", 0.05, 0.65, (0.3, 0.18), 0.2)
                H.shadow_under(f, 11.35, 0.45, 0.34)
                f.fill(f.m_band(11.45, 12.0), H.lin((0, 11.45), (0, 12.0), [(0, "#6a5638"), (1, "#2a2014")]))
                f.fill(f.m_band(11.4, 11.5), "#1c140a", 0.6)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    arm_tops(cv)
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)

"""Charmy Pappitson, the cotton and food mage of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc2.md).

What she wears is the 64x64 sheet's outfit (tools/arc/char_charmy_pappitson.py): a cream tunic with tan trim and a dark belt with a gold buckle, short sleeves with tan cuffs, brown shorts, bare
arms and legs, small brown shoes. The Black Bull cloak that is too big for her (its bull glows biscuit-coloured) is the `pmx_charmy_pappitson` extra and hangs from the shoulders at the back, so
the back of the tunic is kept plain. HD additions that the 64x64 sheet did not have: the strap and the flap of the food satchel (the spec lists a satchel; the sheet did not paint it) across the
chest and at the left hip. No glow on the skin. Drawn at 4x detail on both layers: the big round green eyes with their highlights, the rosy blush, the happy open mouth with a small fang,
the black bob-like fringe, the tunic with its tan trim, lacing and stitching, the satchel strap and its brass buckle, the belt and buckle, the stitched sleeve cuffs, the bare skin of the arms and
legs with dimples at the knees, the brown shorts with turned hems and the shoes with straps and soles.

Source (rule 10): no wiki page could be fetched in this environment; the look is from memory of the series plus the repo sheet: a tiny, round-cheeked girl with big green eyes and short black hair,
always hungry. The repo sheet says black hair in a bun; the hair here is the kit's `anime_short`, which has no bun (see the doc: a decision for the owner).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc2 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_short"
HAIR = "#2a2634"                                              # the sheet's black (#201e26), a little lifted so the highlights of the hair sheet do not die after the tint
HAIR_RGB = (42, 38, 52)
SKIN = ("#eec1a8", "#f9dcc8", "#fff0e4")
TUNIC = "#f2e6c8"
TRIM = "#c89a58"
TRIM_D = "#8a6430"
SHORTS = "#6d4c32"
SHOE = "#5e3e2a"
BELT = "#4a3020"
STRAP = "#a8743c"
GOLD = "#d4aa44"
IRIS = ("#1c6a38", "#4cc472", "#c8f6cc")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.08, chin=0.05)
    pal = K.hair_pal(HAIR, light_k=1.5, dark_k=0.5)
    f.fill(f.m_ellipse(1.15, 6.4, 1.05, 0.62, soft=2.0), "#f58c8c", 0.5)               # the big round blush of a child
    f.fill(f.m_ellipse(6.85, 6.4, 1.05, 0.62, soft=2.0), "#f58c8c", 0.5)
    for cx in (2.3, 5.7):
        H.eye(f, cx, 5.0, 2.6, 2.5, IRIS, lash="#241810", tilt=0.04, lash_w=0.3, flick=0.2, lid_shadow=0.2, hi=1.0, iris_r=1.1)
    H.brow(f, (3.4, 3.2), (1.3, 3.0), "#3a2c28", 0.2, 0.12, arch=0.1)
    H.brow(f, (4.6, 3.2), (6.7, 3.0), "#3a2c28", 0.2, 0.12, arch=0.1)
    H.nose(f, 4.0, 6.1, "#dca088", 0.16)
    H.mouth_smile(f, 4.0, 6.65, 2.0, color="#7a2a30", lip="#e48088", depth=0.28, thick=0.18, teeth="#fffdf6", open_h=0.7)
    f.poly([(3.3, 6.95), (3.7, 6.95), (3.5, 7.45)], "#ffffff", 0.95)                    # a small fang
    # the fringe: blunt black bangs with a few longer locks, a sheen on top
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.5), (7.4, 2.2), (6.4, 2.6), (5.2, 2.1), (4.0, 2.6), (2.8, 2.1), (1.6, 2.6), (0.7, 2.2), (-0.3, 2.6)], y_bot=2.7)
    K.strands(f, pal, (0.9, 2.1, 3.4, 4.7, 6.0, 7.1), 0.0, 2.2, mass, a=0.5)
    for sh, (cx, w, top, ln, lean) in ((0.9, (6.9, 1.6, 1.4, 2.1, -0.3)), (0.9, (1.1, 1.6, 1.4, 2.1, 0.3)), (1.0, (5.2, 1.4, 1.6, 1.3, 0.2)), (1.0, (2.8, 1.4, 1.6, 1.3, -0.2)), (1.0, (4.0, 1.2, 1.7, 1.0, 0.0))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.4, 1.0, 0.8, 3.3, 0.25, 1.0)
    K.lock(f, pal, 7.6, 1.0, 0.8, 3.3, -0.25, 1.0)


# ================================================================================================ the tunic
def satchel_strap(f, x0, y0, x1, y1, w=0.95):
    """A leather strap across a face from (x0, y0) to (x1, y1): a lit top edge, a dark lower edge, stitching along its middle."""
    m = f.m_line((x0, y0), (x1, y1), w, w)
    f.fill(f.m_line((x0, y0 + 0.14), (x1, y1 + 0.14), w + 0.18, w + 0.18), "#1e120a", 0.45)
    f.fill(m, H.lin((x0, y0), (x1, y1), [(0, K.lit(STRAP, 1.15)), (1, H.tone(STRAP, 0.85))]))
    f.fill(f.m_line((x0, y0 - w * 0.34), (x1, y1 - w * 0.34), 0.22, 0.22) * m, K.lit(STRAP, 1.8), 0.6)
    f.fill(f.m_line((x0, y0 + w * 0.36), (x1, y1 + w * 0.36), 0.22, 0.22) * m, K.deep(STRAP, 0.5), 0.6)
    K.seam(f, [(x0, y0), (x1, y1)], "#e0b878", 0.055, 0.6, mask=m)


def body_front(f, ov):
    m = f.m_all()
    K.cloth(f, m, TUNIC, top=1.08, bottom=0.86, round_=0.08, fold_xs=[1.2, 2.5, 5.5, 6.8], fold_depth=0.08, fold_y0=2.0, fold_y1=11.0)
    K.sheen(f, m, [1.3, 6.8], 2.0, 8.2, 0.16)
    # the round neck with a tan band, a short lace in a V
    H.trim_band(f, 0.0, 0.95, TRIM, spec=0.25, outline=0.55)
    f.fill(f.m_poly([(3.1, 0.0), (4.9, 0.0), (4.0, 1.7)]), H.tone(SKIN[1], 0.98))
    f.fill(f.m_polyline([(3.1, 0.0), (4.0, 1.75), (4.9, 0.0)], 0.2), TRIM_D, 0.95)
    for y in (0.55, 1.05):
        K.line(f, [(3.55 + (y - 0.55) * 0.3, y), (4.45 - (y - 0.55) * 0.3, y + 0.28)], TRIM_D, 0.2, 0.9)
        K.line(f, [(4.45 - (y - 0.55) * 0.3, y), (3.55 + (y - 0.55) * 0.3, y + 0.28)], TRIM_D, 0.2, 0.9)
    # tan trim panels at both front edges
    for x0, x1, s in ((0.0, 0.9, 1.0), (8.0, 7.1, -1.0)):
        bm = f.m_rect(min(x0, x1), 1.0, max(x0, x1), 11.0)
        f.fill(bm, H.lin((0, 1.0), (0, 11.0), [(0, K.lit(TRIM, 1.1)), (1, H.tone(TRIM, 0.8))]))
        f.fill(f.m_line((x1, 1.0), (x1, 11.0), 0.2, 0.2), TRIM_D, 0.85)
        K.seam(f, [(x1 + s * 0.4, 1.4), (x1 + s * 0.4, 8.4)], "#f6e6b8", 0.055, 0.55)
    # the satchel: a strap from the right shoulder (left of the picture) across the chest to the left hip, and the flap of the bag at the hip
    satchel_strap(f, 0.5, 1.2, 7.2, 9.8)
    # the belt and its gold buckle (under the bag at the hip)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    K.buckle(f, 3.6, 9.5, 1.9, 1.3, GOLD, hole="#2a1c0c")
    # the flap of the bag hangs over the belt at the left hip
    f.rect(5.6, 8.5, 7.9, 11.2, H.lin((0, 8.5), (0, 11.2), [(0, K.lit(STRAP, 1.1)), (1, H.tone(STRAP, 0.78))]), r=0.2)
    f.rect(5.6, 8.5, 7.9, 9.7, H.lin((0, 8.5), (0, 9.7), [(0, K.lit(STRAP, 1.3)), (1, STRAP)]), r=0.2)
    f.fill(f.m_rect(5.6, 9.62, 7.9, 9.78), "#1e120a", 0.7)
    K.seam(f, [(5.8, 10.9), (7.7, 10.9)], "#e0b878", 0.055, 0.6)
    H.stud(f, 6.75, 9.7, 0.3, GOLD, spec=0.9)
    H.shadow_under(f, 11.2, 0.4, 0.25, x0=5.6, x1=7.9)
    f.fill(f.m_band(11.3, 12.0), TRIM, 0.9)
    f.fill(f.m_rect(0.0, 11.3, f.w, 11.45), K.lit(TRIM, 1.6), 0.6)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, TUNIC, top=1.06, bottom=0.84, round_=0.08, fold_xs=[1.5, 3.0, 5.0, 6.5], fold_depth=0.08, fold_y0=2.0, fold_y1=11.0)
    H.trim_band(f, 0.0, 0.95, TRIM, spec=0.25, outline=0.55)
    f.fill(f.m_line((4.0, 1.4), (4.0, 8.6), 0.2, 0.2), H.tone(TUNIC, 0.62), 0.5)
    satchel_strap(f, 7.6, 1.2, 0.8, 9.2)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    f.fill(f.m_band(11.3, 12.0), TRIM, 0.9)
    f.fill(f.m_rect(0.0, 11.3, f.w, 11.45), K.lit(TRIM, 1.6), 0.6)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), TUNIC, top=1.06, bottom=0.84, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.08, fold_y0=2.0, fold_y1=11.0)
    H.trim_band(f, 0.0, 0.95, TRIM, spec=0.25, outline=0.55)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    f.fill(f.m_band(11.3, 12.0), TRIM, 0.9)
    f.fill(f.m_rect(0.0, 11.3, f.w, 11.45), K.lit(TRIM, 1.6), 0.6)
    if name == "east":                                                                   # the side of the satchel at the left hip
        f.rect(0.0, 8.5, 1.5, 11.2, H.lin((0, 8.5), (0, 11.2), [(0, K.lit(STRAP, 1.1)), (1, H.tone(STRAP, 0.78))]), r=0.2)
        f.fill(f.m_rect(0.0, 9.62, 1.5, 9.78), "#1e120a", 0.7)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ arms and legs
def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            K.limb_skin(f, SKIN, 3.6, 12.0, tendon=nm == "front")
            K.fingers(f, SKIN, y0=10.9, y1=11.95, n=2)
            if nm == "front" or nm == "back":                                          # the elbow dimple
                f.fill(f.m_ellipse(f.w * 0.5, 7.4, 0.5, 0.3, soft=2.0), H.tone(SKIN[0], 0.82), 0.4)
            sl = f.m_band(0.0, 3.1)
            K.cloth(f, sl, TUNIC, top=1.1, bottom=0.88, round_=0.14, fold_xs=[0.9, 2.1], fold_depth=0.08, fold_y0=0.9, fold_y1=2.8)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.32), K.deep(TUNIC, 0.76), 0.6)
            K.seam(f, [(0.0, 0.7), (f.w, 0.7)], "#a8905c", 0.055, 0.55, (0.3, 0.2), 0.15)
            H.shadow_under(f, 3.1, 0.55, 0.3)
            K.cuff(f, 2.9, 3.9, TRIM, pipe="#f6e6b8", stud_color=GOLD)
            H.shadow_under(f, 3.9, 0.7, 0.22, mask=f.m_band(3.9, 12.0))


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.limb_skin(f, SKIN, 4.0, 9.8, tendon=False)
                if nm == "front":
                    f.fill(f.m_ellipse(f.w * 0.5, 6.7, 0.7, 0.35, soft=2.0), H.tone(SKIN[0], 0.82), 0.4)       # the knee
                    f.fill(f.m_ellipse(f.w * 0.4, 7.4, 0.9, 0.5, soft=2.5), K.lit(SKIN[1], 1.1), 0.25)
                sh = f.m_band(0.0, 4.2)
                K.cloth(f, sh, SHORTS, top=1.26, bottom=0.8, round_=0.12, fold_xs=[1.2, 2.8], fold_depth=0.1, fold_y0=0.2, fold_y1=3.6)
                H.trim_band(f, 3.5, 4.3, K.deep(SHORTS, 0.78), spec=0.15, outline=0.55)                 # the turned hem
                K.seam(f, [(0.12, 3.9), (f.w - 0.12, 3.9)], "#b89870", 0.055, 0.7, (0.3, 0.18), 0.2)
                H.shadow_under(f, 4.3, 0.7, 0.3, mask=f.m_band(4.3, 9.8))
                # the little shoes: a sock cuff, brown leather, a strap and a stud
                f.fill(f.m_band(9.55, 10.2), H.lin((0, 9.55), (0, 10.2), [(0, "#f4ecd6"), (1, "#c8bea0")]))
                K.boot(f, SHOE, 10.0, nm, toe=True, welt="#7a5a40", sole="#0e0a08", shaft_end=11.3)
                if nm == "front":
                    f.rect(0.0, 10.3, f.w, 10.8, H.tone(SHOE, 0.8), r=0.0)
                    H.stud(f, f.w * 0.5, 10.55, 0.22, GOLD, spec=0.8)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN, ears=True)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)

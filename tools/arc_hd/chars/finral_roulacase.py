"""Finral Roulacase, the spatial mage of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc2.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_finral_roulacase.py): a light cream shirt with a wide open collar, long sleeves with buttoned cuffs, a dark belt with a gold buckle,
brown trousers turned up over brown calf boots, and green earrings (his portals' colour). The Black Bull cloak (its bull glows green) is the `pmx_finral_roulacase` extra and hangs from the
shoulders at the back, so the back of the shirt is kept plain. HD additions that the 64x64 sheet did not have: the earrings are a gold hook with a green gem on both layers and on both sides of the
head, and the gems light the glow map (a green portal glow, the sheet's own glow colour #58e898), so this look now has a small glow map (it had none). Drawn at 4x detail on both layers: the big
droopy green eyes with their shy lashes, the sad slanted brows, the small nervous smile, the messy light-brown fringe, the open collar and the shirt with its placket, buttons and wrinkles,
the buttoned cuffs and bare hands, the belt, the creased trousers with their turned cuffs and the boots with straps.

Source (rule 10): no wiki page could be fetched in this environment; the look is from memory of the series plus the repo sheet: a slim, shy, flirty young man with unruly light brown hair, big
droopy eyes and green earrings.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc2 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_short"
HAIR = "#b98c52"                                              # honey brown (the sheet's #9a7650 reads dull after the tint)
HAIR_RGB = (185, 140, 82)
SKIN = ("#e9b595", "#f6d2b6", "#ffe8d4")
SHIRT = "#ece6d2"
SHIRT_D = "#b9ad88"
TROUS = "#5f4a33"
CUFF_T = "#80603f"
BOOT = "#4c3626"
BELT = "#3a281b"
GOLD = "#d0a844"
GREEN = "#58c878"
GLOW = "#58e898"
IRIS = ("#1e5a34", "#5cbc72", "#c4f4c8")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.09, chin=0.07)
    pal = K.hair_pal(HAIR)
    f.fill(f.m_ellipse(1.4, 6.4, 0.95, 0.5, soft=2.0), "#f29a8e", 0.3)
    f.fill(f.m_ellipse(6.6, 6.4, 0.95, 0.5, soft=2.0), "#f29a8e", 0.3)
    for cx in (2.3, 5.7):                                                             # big droopy eyes: the outer corners hang
        H.eye(f, cx, 5.05, 2.5, 2.2, IRIS, lash="#2a1c10", tilt=-0.12, lash_w=0.34, flick=0.18, lid_shadow=0.28, hi=1.0, iris_r=1.05)
    H.brow(f, (3.5, 3.55), (1.1, 4.0), "#8a5e32", 0.3, 0.13, arch=0.05)          # sad brows: the inner ends are higher
    H.brow(f, (4.5, 3.55), (6.9, 4.0), "#8a5e32", 0.3, 0.13, arch=0.05)
    H.nose(f, 4.0, 6.1, "#d29a82", 0.2)
    H.mouth_smile(f, 4.0, 7.0, 2.0, color="#8a4a44", lip="#dc8f86", depth=0.16, thick=0.17)
    # the fringe: untidy light-brown locks with a long side bang
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.3, 1.8), (6.3, 2.5), (5.1, 1.7), (3.9, 2.4), (2.7, 1.6), (1.4, 2.3), (-0.3, 2.0)], y_bot=2.4)
    K.strands(f, pal, (0.9, 2.1, 3.4, 4.7, 6.0, 7.1), 0.0, 1.9, mass)
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.9, 1.6, 1.0, 2.7, -0.4)), (0.85, (1.1, 1.6, 1.0, 2.5, 0.5)), (0.9, (5.0, 1.5, 1.2, 1.9, 0.5)), (0.9, (3.1, 1.5, 1.2, 1.7, -0.3)),
                                      (1.0, (6.0, 1.3, 1.2, 1.4, -0.3)), (1.0, (2.0, 1.3, 1.2, 1.6, 0.3)), (1.0, (4.0, 1.2, 1.3, 1.3, -0.2))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.4, 1.0, 0.8, 3.2, 0.2, 1.0)
    K.lock(f, pal, 7.6, 1.0, 0.8, 3.2, -0.2, 1.0)


def earrings(cv):
    """The green earrings on both sides of the head, on both layers (the second layer only where the 64x64 sheet has them: a one-pixel column, rows 5 and 6, just in front of the ear). The
    gems light the glow map."""
    for nm in ("west", "east"):
        cx = 3.5 if nm == "west" else 4.5
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            if ov:
                f.clear()
            H.stud(f, cx, 5.4, 0.2, GOLD, spec=0.8)
            H.gem(f, cx, 6.35, 0.3, GREEN, setting=GOLD, facets=4, spec=0.9, glow=GLOW)


# ================================================================================================ the shirt
def body_front(f, ov):
    m = f.m_all()
    K.cloth(f, m, SHIRT, top=1.06, bottom=0.84, round_=0.09, fold_xs=[1.0, 2.3, 5.7, 7.0], fold_depth=0.1, fold_y0=2.0, fold_y1=11.0)
    K.sheen(f, m, [1.3, 6.8], 2.0, 8.4, 0.16)
    # the open collar: a V down to the chest showing skin, the two wide collar flaps, a cord of shadow under them
    v = f.m_poly([(2.5, 0.0), (5.5, 0.0), (4.0, 3.5)])
    f.fill(v, H.lin((0, 0), (0, 3.5), [(0, SKIN[1]), (1, SKIN[0])]))
    H.shadow_under(f, 0.0, 1.2, 0.18, mask=v)
    for sgn in (-1.0, 1.0):
        fl = f.m_poly([(4.0 + sgn * 0.05, 3.5), (4.0 + sgn * 1.6, 0.0), (4.0 + sgn * 2.9, 0.0), (4.0 + sgn * 2.4, 1.3), (4.0 + sgn * 0.9, 3.55)])
        f.fill(fl, H.lin((0, 0), (0, 3.5), [(0, K.lit(SHIRT, 1.08)), (1, SHIRT)]))
        f.fill(f.m_polyline([(4.0 + sgn * 0.05, 3.5), (4.0 + sgn * 1.6, 0.0)], 0.2), K.deep(SHIRT, 0.55), 0.8)
        f.fill(f.m_polyline([(4.0 + sgn * 0.9, 3.55), (4.0 + sgn * 2.4, 1.3), (4.0 + sgn * 2.9, 0.0)], 0.22), K.deep(SHIRT, 0.55), 0.85)
        K.seam(f, [(4.0 + sgn * 0.95, 3.15), (4.0 + sgn * 2.3, 1.1)], SHIRT_D, 0.055, 0.55)
    # the placket: a darker line, buttons, a stitched edge
    f.fill(f.m_line((4.0, 3.6), (4.0, 12.0), 0.2, 0.2), K.deep(SHIRT, 0.6), 0.6)
    K.seam(f, [(4.5, 3.8), (4.5, 8.5)], SHIRT_D, 0.055, 0.5)
    K.seam(f, [(3.5, 3.8), (3.5, 8.5)], SHIRT_D, 0.055, 0.5)
    for y in (4.4, 6.0, 7.6):
        H.stud(f, 4.0, y, 0.2, "#d8cc9c", spec=0.8)
    # wrinkles pulling toward the belt
    for x in (1.6, 6.4):
        f.fill(f.m_polyline([(x - 0.5, 7.2), (x, 8.3), (x + 0.4, 8.7)], 0.18), K.deep(SHIRT, 0.6), 0.5)
    # the belt, the gold buckle
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    K.buckle(f, 4.0, 9.5, 2.0, 1.3, GOLD, hole="#2a1c0c")
    # the hem of the shirt, a little untucked
    f.fill(f.m_band(11.3, 12.0), SHIRT_D, 0.55)
    H.shadow_under(f, 10.1, 0.6, 0.2)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, SHIRT, top=1.04, bottom=0.82, round_=0.09, fold_xs=[1.5, 3.0, 5.0, 6.5], fold_depth=0.1, fold_y0=2.0, fold_y1=11.0)
    f.fill(f.m_band(0.0, 1.0), K.deep(SHIRT, 0.82), 0.7)
    f.fill(f.m_polyline([(0.4, 1.9), (4.0, 2.4), (7.6, 1.9)], 0.2), K.deep(SHIRT, 0.6), 0.5)
    f.fill(f.m_line((4.0, 2.4), (4.0, 8.5), 0.18, 0.18), K.deep(SHIRT, 0.6), 0.45)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    f.fill(f.m_band(11.3, 12.0), SHIRT_D, 0.55)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), SHIRT, top=1.04, bottom=0.82, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.1, fold_y0=2.0, fold_y1=11.0)
    f.fill(f.m_band(0.0, 1.0), K.deep(SHIRT, 0.82), 0.7)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    f.fill(f.m_band(11.3, 12.0), SHIRT_D, 0.55)


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
            sl = f.m_band(0.0, 9.4)
            K.cloth(f, sl, SHIRT, top=1.06, bottom=0.84, round_=0.15, fold_xs=[0.9, 2.1], fold_depth=0.1, fold_y0=1.0, fold_y1=9.0)
            K.sheen(f, sl, [f.w * 0.35], 1.4, 8.4, 0.16)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.32), K.deep(SHIRT, 0.7), 0.6)
            K.seam(f, [(0.0, 0.7), (f.w, 0.7)], SHIRT_D, 0.055, 0.55, (0.3, 0.2), 0.15)
            for y in (4.8, 5.3, 5.8):                                              # the elbow wrinkles
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.16) * sl, K.deep(SHIRT, 0.62), 0.45)
            f.fill(f.m_polyline([(0.0, 4.55), (f.w * 0.5, 4.8), (f.w, 4.6)], 0.2) * sl, K.lit(SHIRT, 1.2), 0.4)
            H.shadow_under(f, 9.4, 0.4, 0.25)
            # a buttoned cuff
            H.trim_band(f, 8.7, 9.7, K.deep(SHIRT, 0.9), spec=0.2, outline=0.5)
            K.seam(f, [(0.12, 9.2), (f.w - 0.12, 9.2)], SHIRT_D, 0.055, 0.7, (0.3, 0.18), 0.2)
            H.stud(f, f.w - 0.55, 9.2, 0.16, "#d8cc9c", spec=0.8)
            K.limb_skin(f, SKIN, 9.7, 12.0, tendon=True)
            K.fingers(f, SKIN, y0=10.9, y1=11.95, n=2 if f.w == 3 else 3)
            H.shadow_under(f, 9.7, 0.5, 0.3, mask=f.m_band(9.7, 12.0))


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 6.0, nm, knee=True, fold_xs=(1.1, 2.9), seam_color="#8a7050", top=1.3, bottom=0.8)
                H.trim_band(f, 5.9, 6.95, CUFF_T, spec=0.2, outline=0.55)                  # the turned-up cuff
                K.seam(f, [(0.12, 6.45), (f.w - 0.12, 6.45)], "#c8a878", 0.055, 0.7, (0.3, 0.18), 0.2)
                H.shadow_under(f, 6.95, 0.7, 0.35)
                K.boot(f, BOOT, 6.95, nm, strap="#6a4a30", strap_color="#6a4a30", cuff_color=None, welt="#7a5a40", sole="#0c0a08", gold=GOLD)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN, ears=True)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    ec = {("head", nm): K.cover_of(cv, "head", nm) for nm in ("west", "east")}
    earrings(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim, extra_covers=ec)

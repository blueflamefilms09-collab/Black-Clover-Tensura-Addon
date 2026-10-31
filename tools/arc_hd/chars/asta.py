"""Asta, the boy with no magic, HD 256x256 (docs/arc/hd_arc_bc1.md): the hand-painted look of the Black Bulls' loudest member, the second friend a player meets in the Sanctuary Hub.

What he wears is the 64x64 sheet's outfit (tools/arc/char_asta.py): a black sleeveless top with bare arms, dark trousers in calf boots, a brown belt with a gold buckle, the dark Black Bull headband (its two
tails are the `ribbon` extra of the look). The black Black Bull cloak and the huge black Demon-Slayer sword on his back are 3D extras (`pmx_asta`: `cape`, `cape_low`, `prop_back`), so the skin only carries
what is under them (the back of the top) and leaves them alone. No glow (anti-magic is black).

Drawn at 4x detail on both layers: the wide emerald anime eyes, thick slanted ash brows, a big open grin with the upper teeth, a fringe of thick clumps over the black headband, the sleeveless top (neck rib,
armholes, chest folds, hem), the bare muscled arms (deltoid, biceps, forearm, veins, knuckles), the belt with its buckle and the trouser waist, the trousers with creases and the boots with cuff and straps.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus the earlier research in
docs/arc/npca.md (which cites the wikis): short upswept ash-white hair, big green eyes, a wide grin, thick brows, a black headband, a black sleeveless top. The kit hair is `anime_spike_tall` (thick clumps).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_spike_tall"                                                     # thick, layered, back-swept clumps: the owner's "no fingers" rule
HAIR = "#d6dae4"                                                                    # ash white with a cool cast (the sheet's #cfd2d8 reads dull grey after the hair tint pass)
HAIR_RGB = (214, 218, 228)
SKIN = ("#e6b18f", "#f6cfb2", "#ffe5d2")
TOP, TOP_L = "#22222b", "#3a3a47"                                                   # the black top and its lit folds
RIB = "#32323e"                                                                     # the ribbed neck, armhole and hem edges
TROUS = "#343848"
BOOT = "#4a3626"
BELT, BUCKLE = "#5a3d28", "#d8b048"
BAND = "#18181e"                                                                    # the headband
IRIS = ("#0f5a2a", "#3aae52", "#b6f2b4")
BROW = "#7d8499"


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.09, chin=0.07)
    pal = K.hair_palette(HAIR)
    # the hair of the fringe: a thick mass on the crown with clumps that hang over the band (the 3D hair is drawn on top of it; this is what shows between its locks)
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 1.9), (7.5, 1.5), (6.9, 2.1), (6.0, 1.6), (5.2, 2.0), (4.0, 1.4), (3.0, 2.0), (2.0, 1.5), (1.1, 2.1), (-0.3, 1.7)]
    K.hair_mass(f, pal, outline, 0.0, 2.2, streaks=(0.8, 2.0, 3.4, 4.8, 6.2, 7.2))
    for sh, (cx, w, top, ln, lean) in ((0.8, (7.1, 1.6, 1.0, 2.2, -0.35)), (0.9, (5.45, 1.5, 1.1, 1.5, 0.25)), (0.9, (2.6, 1.5, 1.1, 1.5, -0.25)), (1.0, (0.9, 1.6, 1.0, 2.4, 0.35))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    # the headband: a black cloth band across the forehead just above the brows, with a lit top edge, a fold and a stitch
    band = f.m_rect(-0.1, 2.5, 8.1, 3.45)
    f.fill(band, H.lin((0, 2.5), (0, 3.45), [(0, K.lit(BAND, 2.0)), (0.35, BAND), (1, H.tone(BAND, 0.7))]))
    f.fill(f.m_rect(-0.1, 2.5, 8.1, 2.62), "#4a4a58", 0.6)
    K.seam(f, [(0.2, 3.1), (7.8, 3.1)], "#5a5a68", 0.05, 0.5, (0.3, 0.2), 0.0)
    f.fill(f.m_polyline([(2.5, 2.55), (2.8, 3.0), (2.55, 3.4)], 0.09) * band, "#0a0a0e", 0.6)
    H.shadow_under(f, 3.45, 0.55, 0.3)
    # the hair that falls over the band: two short clumps
    for sh, (cx, w, top, ln, lean) in ((0.9, (2.4, 1.3, 2.2, 1.0, 0.1)), (0.95, (5.6, 1.3, 2.2, 0.95, -0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    # thick, low, slanted brows; wide eyes with emerald irises; a tiny nose; the big open grin
    H.brow(f, (3.45, 4.05), (1.0, 3.5), BROW, 0.46, 0.18, arch=0.06)
    H.brow(f, (4.55, 4.05), (7.0, 3.5), BROW, 0.46, 0.18, arch=0.06)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.1, 2.5, 2.1, IRIS, lash="#1a1218", tilt=0.2, lash_w=0.42, flick=0.34, lid_shadow=0.32, iris_r=1.06, hi=0.95)
    H.blush(f, 1.1, 6.35, 0.9, 0.45, "#ee9088", 0.35)
    H.blush(f, 6.9, 6.35, 0.9, 0.45, "#ee9088", 0.35)
    H.nose(f, 4.0, 6.15, "#d39a80", 0.24)
    H.mouth_smile(f, 4.0, 6.65, 4.0, color="#6a2428", lip="#d98a80", depth=0.28, thick=0.2, teeth="#fffdf6", open_h=0.8)
    f.fill(f.m_ellipse(4.0, 7.35, 0.9, 0.22) * f.m_rect(3.0, 6.9, 5.0, 7.6), "#c8504e", 0.55)                    # the tongue in the open mouth


def head_band_sides(cv):
    """The headband wraps the head: a black band on row 2.5..3.45 of the back and both sides (base layer and the 64x64 sheet's second-layer row 1), where the ribbon extra ties on."""
    for nm in ("back", "west", "east"):
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            if ov and f.m_alpha().max() < 0.5:
                continue
            y0, y1 = (1.0, 2.0) if ov else (2.5, 3.45)
            band = f.m_rect(-0.1, y0, f.w + 0.1, y1)
            f.fill(band, H.lin((0, y0), (0, y1), [(0, K.lit(BAND, 2.0)), (0.35, BAND), (1, H.tone(BAND, 0.7))]))
            f.fill(f.m_rect(-0.1, y0, f.w + 0.1, y0 + 0.12), "#4a4a58", 0.6)
            if nm == "back":                                                                   # the knot where the two tails are tied
                f.ellipse(f.w * 0.5, (y0 + y1) / 2.0, 0.9, 0.62, H.tone(BAND, 1.3))
                f.fill(f.m_polyline([(f.w * 0.5 - 0.8, (y0 + y1) / 2.0), (f.w * 0.5 + 0.8, (y0 + y1) / 2.0)], 0.1), "#0a0a0e", 0.7)


# ================================================================================================ the body
def top_front(f, ov):
    m = f.m_all()
    K.cloth(f, m, TOP, top=1.3, bottom=0.82, round_=0.08)
    # the cloth is stretched over a strong chest: one broad lit plane across the pecs, a shallow shadow arc under each, the sternum, the abs as soft rows, a tension fold from each armhole
    f.shade(m, 1.0 + 0.22 * K.blob(f, 4.0, 2.7, 3.7, 1.4, 0.9), 0.9)
    for sgn in (-1.0, 1.0):
        cx = 4.0 + sgn * 1.9
        f.fill(f.m_polyline([(cx - 1.85, 4.35), (cx - 0.9, 4.7), (cx + 0.1, 4.75), (cx + 1.0, 4.55), (cx + 1.8, 4.25)] if sgn < 0 else
                            [(cx - 1.8, 4.25), (cx - 1.0, 4.55), (cx - 0.1, 4.75), (cx + 0.9, 4.7), (cx + 1.85, 4.35)], 0.34, 2.0), H.tone(TOP, 0.4), 0.55)
    f.fill(f.m_line((4.0, 2.1), (4.0, 4.7), 0.2, 0.14), H.tone(TOP, 0.45), 0.5)
    f.fill(f.m_line((4.0, 4.9), (4.0, 8.2), 0.14, 0.1), H.tone(TOP, 0.5), 0.4)
    for y in (5.9, 7.1):
        f.shade(m, 1.0 - 0.16 * K.blob(f, 4.0, y, 2.0, 0.3, 1.0), 0.9)
    K.crease(f, [(0.7, 5.3), (1.7, 6.2), (2.2, 7.6)], TOP, 0.1, 0.4)
    K.crease(f, [(7.3, 5.3), (6.3, 6.2), (5.8, 7.6)], TOP, 0.1, 0.4)
    # the round neck: a ribbed collar band (a lit edge, a dark inner rim), the skin of the neck above it
    neck = f.m_poly([(1.9, 0.0), (6.1, 0.0), (5.7, 0.55), (4.0, 1.2), (2.3, 0.55)])
    f.fill(neck, H.lin((0, 0), (0, 1.2), [(0, H.tone(SKIN[1], 0.88)), (1, H.tone(SKIN[0], 0.92))]))
    f.fill(f.m_polyline([(1.9, 0.0), (2.3, 0.55), (4.0, 1.2), (5.7, 0.55), (6.1, 0.0)], 0.3), K.lit(RIB, 1.0), 0.95)
    f.fill(f.m_polyline([(2.0, 0.1), (2.4, 0.62), (4.0, 1.28), (5.6, 0.62), (6.0, 0.1)], 0.1), "#0c0c10", 0.75)
    f.fill(f.m_polyline([(2.4, 0.5), (4.0, 1.1), (5.6, 0.5)], 0.06), H.tone(SKIN[0], 0.7), 0.5)   # the collar bones
    # the armholes: a strip of skin with a ribbed edge; the hem above the belt
    for x0, x1 in ((0.0, 0.7), (7.3, 8.0)):
        f.fill(f.m_rect(x0, 0.9, x1, 3.4), H.lin((0, 0.9), (0, 3.4), [(0, H.tone(SKIN[1], 0.9)), (1, SKIN[0])]))
        f.fill(f.m_line((x1 if x0 < 4 else x0, 0.9), (x1 if x0 < 4 else x0, 3.4), 0.22, 0.22), RIB, 0.95)
    f.fill(f.m_rect(0.0, 8.3, 8.0, 8.85), H.lin((0, 8.3), (0, 8.85), [(0, RIB), (1, H.tone(RIB, 0.6))]), 0.9)
    K.seam(f, [(0.1, 8.55), (7.9, 8.55)], "#6a6a80", 0.05, 0.5, (0.3, 0.2), 0.2)


def belt(f, front):
    K.belt_band(f, 9.0, 10.05, BELT)
    K.belt_band(f, 10.05, 10.5, H.tone(BELT, 0.62), stitch_a=0.0)
    H.shadow_under(f, 10.5, 0.45, 0.3)
    if front:
        for x in (1.0, 7.0):
            H.stud(f, x, 9.52, 0.18, BUCKLE)
        K.buckle(f, 4.0, 9.55, 2.5, 1.5, BUCKLE)
        f.poly([(5.4, 9.2), (5.9, 9.2), (5.9, 11.6), (5.55, 11.9), (5.4, 11.6)], H.tone(BELT, 0.8))               # the loose end of the belt
        f.fill(f.m_polyline([(5.4, 9.2), (5.4, 11.6), (5.55, 11.9), (5.9, 11.6), (5.9, 9.2)], 0.07), "#0e0a08", 0.7)


def waist(f, front):
    """Rows 10.5..12 under the belt: the waist of the dark trousers, a little lighter than the top."""
    f.fill(f.m_rect(0.0, 10.5, f.w, 12.0), H.lin((0, 10.5), (0, 12.0), [(0, H.tone(TROUS, 0.78)), (1, H.tone(TROUS, 0.55))]))
    if front:
        f.fill(f.m_line((4.0, 10.7), (4.0, 12.0), 0.1, 0.1), H.tone(TROUS, 0.4), 0.8)
    H.shadow_under(f, 10.5, 0.4, 0.28)


def body_front(f, ov):
    if not ov:
        top_front(f, ov)
        waist(f, True)
    belt(f, True)


def body_back(f, ov):
    if not ov:
        m = f.m_all()
        K.cloth(f, m, TOP, top=1.25, bottom=0.8, round_=0.08, fold_xs=[1.7, 6.3], fold_depth=0.08, fold_y0=1.5, fold_y1=8.8)
        f.shade(m, 1.0 + 0.22 * K.blob(f, 2.2, 3.1, 1.7, 1.9) + 0.22 * K.blob(f, 5.8, 3.1, 1.7, 1.9), 0.9)       # the shoulder blades under the cloth
        f.fill(f.m_line((4.0, 1.0), (4.0, 8.6), 0.17, 0.12), H.tone(TOP, 0.45), 0.55)
        K.seam(f, [(4.0, 1.2), (4.0, 8.5)], "#55556a", 0.05, 0.5)
        f.fill(f.m_polyline([(1.9, 0.0), (2.3, 0.6), (4.0, 1.0), (5.7, 0.6), (6.1, 0.0)], 0.3), RIB, 0.95)
        for x0, x1 in ((0.0, 0.7), (7.3, 8.0)):
            f.fill(f.m_rect(x0, 0.9, x1, 3.4), H.lin((0, 0.9), (0, 3.4), [(0, H.tone(SKIN[1], 0.9)), (1, SKIN[0])]))
            f.fill(f.m_line((x1 if x0 < 4 else x0, 0.9), (x1 if x0 < 4 else x0, 3.4), 0.22, 0.22), RIB, 0.95)
        f.fill(f.m_rect(0.0, 8.3, 8.0, 8.85), H.lin((0, 8.3), (0, 8.85), [(0, RIB), (1, H.tone(RIB, 0.6))]), 0.9)
        waist(f, False)
    belt(f, False)


def body_side(f, ov, name):
    if not ov:
        K.cloth(f, f.m_all(), TOP, top=1.25, bottom=0.8, round_=0.1, fold_xs=[1.2, 2.6], fold_depth=0.08, fold_y0=1.5, fold_y1=8.8)
        # the armhole on the side next to the arm: skin, then the dark seam of the cloth
        x0, x1 = (0.0, 1.3) if name == "east" else (f.w - 1.3, f.w)
        f.fill(f.m_rect(x0, 0.0, x1, 3.6), H.lin((0, 0.0), (0, 3.6), [(0, H.tone(SKIN[1], 0.9)), (1, SKIN[0])]))
        f.fill(f.m_line((x1 if name == "east" else x0, 0.0), (x1 if name == "east" else x0, 3.6), 0.22, 0.22), RIB, 0.95)
        f.fill(f.m_rect(0.0, 8.3, f.w, 8.85), H.lin((0, 8.3), (0, 8.85), [(0, RIB), (1, H.tone(RIB, 0.6))]), 0.9)
        waist(f, False)
    belt(f, False)


def body(cv):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, ov)
            elif nm == "back":
                body_back(f, ov)
            else:
                body_side(f, ov, nm)


# ================================================================================================ the arms (bare)
def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            K.bare_arm(f, SKIN, nm, 0.0, 12.0, power=1.0)
            f.fill(f.m_band(0.0, 0.9), H.tone(SKIN[0], 0.88), 0.4)                    # the shoulder is shaded by the armhole


# ================================================================================================ the legs
def leg(f, ov, name):
    m = f.m_band(0.0, 6.4)
    K.cloth(f, m, TROUS, top=1.35, bottom=0.78, round_=0.12, fold_xs=[f.w * 0.5], fold_depth=0.06, fold_y0=0.6, fold_y1=6.0)
    # the thigh is lit on the front, the knee has two soft creases, the side seam is a stitched line, a lit edge runs down the front of the leg
    f.shade(m, 1.0 + 0.16 * K.blob(f, f.w * 0.45, 1.8, f.w * 0.7, 2.0), 0.9)
    K.crease(f, [(0.2, 3.5), (f.w * 0.5, 3.8), (f.w - 0.2, 3.55)], TROUS, 0.1, 0.5, mask=m)
    K.crease(f, [(0.4, 4.2), (f.w * 0.5, 4.45), (f.w - 0.4, 4.25)], TROUS, 0.08, 0.35, mask=m)
    if name in ("west", "east"):
        K.seam(f, [(f.w * 0.5, 0.3), (f.w * 0.5, 6.0)], "#6a6a80", 0.06, 0.5)
    if name == "front":
        K.seam(f, [(f.w * 0.5, 0.4), (f.w * 0.5, 3.0)], "#5a5a70", 0.05, 0.35)          # the front crease of the trousers
    H.shadow_under(f, 6.1, 0.9, 0.35)
    # the folded boot cuff: a lighter leather band with a stitch and a shadow under it
    cuff = f.m_rect(0.0, 6.3, f.w, 7.35)
    f.fill(cuff, H.lin((0, 6.3), (0, 7.35), [(0, K.lit(BOOT, 1.9)), (0.5, K.lit(BOOT, 1.35)), (1, H.tone(BOOT, 0.8))]))
    f.fill(f.m_rect(0.0, 6.3, f.w, 6.42), "#0c0806", 0.55)
    f.fill(f.m_rect(0.0, 6.42, f.w, 6.56), K.lit(BOOT, 2.3), 0.5)
    K.seam(f, [(0.12, 6.9), (f.w - 0.12, 6.9)], "#c0a07c", 0.06, 0.7, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 7.35, 11.25, name, strap="#241a14", gold="#c8a040")


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f, ov in zip(A.lay(cv, part, nm), (False, True)):
                leg(f, ov, nm)


# ================================================================================================ paint
def paint(spec, cid, masks):
    spec["hair"]["color"] = HAIR
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=HAIR_STYLE)
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, HAIR_RGB, skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    covers = K.all_covers(cv)
    head_front(cv)
    head_band_sides(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim

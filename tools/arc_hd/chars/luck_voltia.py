"""Luck Voltia, the lightning mage of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc2.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_luck_voltia.py): a thigh-length aqua tunic with a darker teal hem, a dark belt with a gold buckle, long aqua sleeves ending in gold
fingerless gloves, teal trousers and small dark shoes. The Black Bull cloak (with its lightning-yellow bull) is the `pmx_luck_voltia` extra: it hangs from the shoulders at the back and covers the
back, so the back of the tunic is kept plain. No glow on the skin (the bull glows on the extra). Drawn at 4x detail on both layers: the wide pale-blue eyes with their lightning-bright highlights,
the pointed raised brows, the huge grin with the upper teeth and two fangs, the messy fringe of pointed blond locks, the laced V-neck, the tunic with gold piping, stitched hem and belt, the gold
fingerless gloves with a bolt mark and bare fingertips, the trousers with their knee creases and rolled hems, and the little shoes with a gold clasp.

Source (rule 10): no wiki page could be fetched in this environment; the look is from memory of the series plus the repo sheet: short, messy blond hair, light blue eyes, a wide happy grin, a small
and wiry boy who loves a fight. The aqua and teal are the sheet's; the gold piping that echoes the gloves and the lightning is an addition of the HD pass.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc2 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_short"
HAIR = "#f1cd3a"                                              # a saturated golden blond: pale yellows read khaki after the hair tint
HAIR_RGB = (241, 205, 58)
SKIN = ("#ecbc9c", "#f8d7bc", "#ffeadc")
TUNIC = "#4ab8c8"
TRIM = "#2a8a96"
TRIM_D = "#1a5a66"
TROUS = "#25808a"
GOLD = "#e8c040"
CREAM = "#f0ead8"
SHOE = "#2a2a34"
BELT = "#3e2a20"
IRIS = ("#1a64a8", "#5cc4f2", "#d0f4ff")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.09, chin=0.07)
    pal = K.hair_pal(HAIR)
    # the wide grin and the big eager eyes; the cheeks are pushed up a little
    f.fill(f.m_ellipse(1.3, 6.2, 0.9, 0.5, soft=2.0), "#f29a90", 0.32)
    f.fill(f.m_ellipse(6.7, 6.2, 0.9, 0.5, soft=2.0), "#f29a90", 0.32)
    for cx in (2.3, 5.7):
        H.eye(f, cx, 5.0, 2.5, 2.1, IRIS, lash="#2a2210", tilt=0.1, lash_w=0.4, flick=0.34, lid_shadow=0.3, hi=1.0, iris_r=1.05)
    H.brow(f, (3.5, 3.45), (1.0, 2.95), "#b88a14", 0.30, 0.13, arch=0.1)
    H.brow(f, (4.5, 3.45), (7.0, 2.95), "#b88a14", 0.30, 0.13, arch=0.1)
    H.nose(f, 4.0, 6.0, "#d49a82", 0.22)
    H.mouth_smile(f, 4.0, 6.55, 4.0, color="#6a2a2a", lip="#d97c78", depth=0.3, thick=0.2, teeth="#fffdf6", open_h=0.75)
    for x in (2.9, 5.1):                                                                  # two fangs
        f.poly([(x - 0.2, 6.85), (x + 0.2, 6.85), (x, 7.4)], "#ffffff", 0.95)
    # the fringe: a messy mass with pointed locks hanging over the brow
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.2), (7.3, 1.7), (6.5, 2.3), (5.4, 1.6), (4.3, 2.2), (3.1, 1.6), (2.0, 2.2), (0.9, 1.7), (-0.3, 2.3)], y_bot=2.4)
    K.strands(f, pal, (0.9, 2.1, 3.4, 4.7, 6.0, 7.1), 0.0, 1.9, mass)
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.9, 1.5, 1.0, 2.3, -0.5)), (0.85, (1.1, 1.5, 1.0, 2.2, 0.5)), (0.9, (4.9, 1.4, 1.2, 1.7, 0.3)), (0.9, (3.1, 1.4, 1.2, 1.6, -0.3)),
                                      (1.0, (5.9, 1.3, 1.2, 1.2, -0.2)), (1.0, (2.0, 1.3, 1.2, 1.3, 0.2)), (1.0, (4.0, 1.0, 1.3, 0.9, 0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.4, 1.0, 0.8, 3.0, 0.2, 1.0)
    K.lock(f, pal, 7.6, 1.0, 0.8, 3.0, -0.2, 1.0)


# ================================================================================================ the tunic
def body_front(f, ov):
    m = f.m_all()
    K.cloth(f, m, TUNIC, top=1.18, bottom=0.84, round_=0.09, fold_xs=[1.2, 2.7, 5.3, 6.8], fold_depth=0.08, fold_y0=2.0, fold_y1=10.0)
    K.sheen(f, m, [1.0, 7.0], 2.0, 8.2, 0.2, "#e8ffff")
    for x0, x1 in ((0.0, 0.5), (7.5, 8.0)):
        f.fill(f.m_rect(x0, 1.0, x1, 11.0), K.deep(TUNIC, 0.62), 0.45)                                # the darker sides of the tunic turn away
    for xs in (2.8, 5.2):                                                                   # two gold-piped seams down the front panels
        K.seam(f, [(xs, 2.6), (xs, 8.4)], GOLD, 0.06, 0.55, (0.3, 0.2), 0.2)
    # the laced V-neck: a cream under-shirt in the V, gold piping along its edge, crossed laces
    v = f.m_poly([(2.7, 0.0), (5.3, 0.0), (4.0, 2.7)])
    f.fill(v, H.lin((0, 0), (0, 2.7), [(0, CREAM), (1, H.tone(CREAM, 0.78))]))
    f.fill(f.m_polyline([(2.7, 0.0), (4.0, 2.7), (5.3, 0.0)], 0.3), K.deep(TUNIC, 0.5), 0.95)
    f.fill(f.m_polyline([(2.7, 0.0), (4.0, 2.7), (5.3, 0.0)], 0.2), GOLD, 0.95)
    for y in (0.7, 1.4, 2.0):
        K.line(f, [(3.4 + (y - 0.7) * 0.25, y), (4.6 - (y - 0.7) * 0.25, y + 0.35)], H.tone(GOLD, 0.8), 0.2, 0.9)
        K.line(f, [(4.6 - (y - 0.7) * 0.25, y), (3.4 + (y - 0.7) * 0.25, y + 0.35)], H.tone(GOLD, 0.8), 0.2, 0.9)
    H.shadow_under(f, 0.1, 0.3, 0.0)
    # the belt and its gold buckle with a bolt cut in it
    K.belt_band(f, 8.8, 10.0, BELT, stitch="#a8805a")
    K.buckle(f, 4.0, 9.4, 2.2, 1.5, GOLD, hole="#3a2a10")
    f.fill(f.m_polyline([(4.2, 8.85), (3.7, 9.4), (4.1, 9.4), (3.8, 9.95)], 0.2), "#fff2a0", 0.9)       # the bolt
    for x in (1.0, 7.0):
        H.stud(f, x, 9.4, 0.17, GOLD, spec=0.7)
    # the hem: a darker teal band with a gold stitch, and the shadow of the belt
    H.trim_band(f, 11.0, 12.0, TRIM, spec=0.3, outline=0.5)
    K.seam(f, [(0.12, 11.5), (f.w - 0.12, 11.5)], GOLD, 0.055, 0.75, (0.3, 0.18), 0.2)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, TUNIC, top=1.12, bottom=0.82, round_=0.09, fold_xs=[1.5, 3.0, 5.0, 6.5], fold_depth=0.08, fold_y0=2.0, fold_y1=10.0)
    f.fill(f.m_line((4.0, 1.0), (4.0, 8.6), 0.22, 0.22), H.tone(TUNIC, 0.55), 0.55)
    H.trim_band(f, 0.0, 0.9, TRIM, spec=0.3, outline=0.5)                                  # the neckband
    K.belt_band(f, 8.8, 10.0, BELT, stitch="#a8805a")
    H.stud(f, 4.0, 9.4, 0.2, GOLD, spec=0.7)
    H.trim_band(f, 11.0, 12.0, TRIM, spec=0.3, outline=0.5)
    K.seam(f, [(0.12, 11.5), (f.w - 0.12, 11.5)], GOLD, 0.055, 0.75, (0.3, 0.18), 0.2)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), TUNIC, top=1.12, bottom=0.82, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.08, fold_y0=2.0, fold_y1=10.0)
    H.trim_band(f, 0.0, 0.9, TRIM, spec=0.3, outline=0.5)
    K.belt_band(f, 8.8, 10.0, BELT, stitch="#a8805a")
    H.trim_band(f, 11.0, 12.0, TRIM, spec=0.3, outline=0.5)
    K.seam(f, [(0.12, 11.5), (f.w - 0.12, 11.5)], GOLD, 0.055, 0.75, (0.3, 0.18), 0.2)
    ed = f.w - 0.8 if name == "west" else 0.8
    K.seam(f, [(ed, 1.6), (ed, 8.4)], GOLD, 0.055, 0.45)


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
            sl = f.m_band(0.0, 8.7)
            K.cloth(f, sl, TUNIC, top=1.16, bottom=0.85, round_=0.15, fold_xs=[0.9, 2.1], fold_depth=0.09, fold_y0=1.0, fold_y1=8.2)
            K.sheen(f, sl, [f.w * 0.35], 1.4, 7.8, 0.2, "#e8ffff")
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.32), K.deep(TUNIC, 0.7), 0.6)                    # the shoulder seam
            K.seam(f, [(0.0, 0.7), (f.w, 0.7)], GOLD, 0.055, 0.6, (0.3, 0.2), 0.15)
            for y in (4.5, 5.0):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.28), (f.w, y + 0.04)], 0.16) * sl, K.deep(TUNIC, 0.6), 0.45)
            H.shadow_under(f, 8.7, 0.5, 0.25)
            K.cuff(f, 8.2, 9.0, TRIM, pipe=GOLD, stud_color=GOLD)
            # the fingerless gold glove: from the wrist to the knuckles, a ribbed band, the bolt of the lightning cut in the back of the hand; bare fingertips under it
            gl = f.m_band(9.0, 11.2)
            K.cloth(f, gl, GOLD, top=1.22, bottom=0.8, round_=0.12)
            f.fill(f.m_band(9.0, 9.3) * gl, H.tone(GOLD, 0.7), 0.6)
            for y in (9.7, 10.2, 10.7):
                f.fill(f.m_line((0.1, y), (f.w - 0.1, y + 0.05), 0.12, 0.12) * gl, K.deep(GOLD, 0.62), 0.3)
            f.fill(f.m_band(11.0, 11.3), K.deep(GOLD, 0.55), 0.8)
            if nm in ("front", "back"):
                f.fill(f.m_polyline([(f.w * 0.62, 9.35), (f.w * 0.36, 10.05), (f.w * 0.6, 10.1), (f.w * 0.34, 10.9)], 0.2) * gl, "#7a5a08", 0.75)
            K.limb_skin(f, (SKIN[0], SKIN[1], SKIN[2]), 11.2, 12.0, tendon=False)
            if nm in ("front", "back"):
                for x in (f.w * 0.33, f.w * 0.66):
                    f.fill(f.m_line((x, 11.35), (x, 12.0), 0.22, 0.2), H.tone(SKIN[0], 0.7), 0.7)
            H.shadow_under(f, 11.2, 0.4, 0.25, mask=f.m_band(11.2, 12.0))


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                # the trousers under the tunic skirt
                K.trousers(f, TROUS, 3.0, 9.6, nm, knee=False, fold_xs=(1.1, 2.9), seam_color="#6fc0c8", top=1.28, bottom=0.78)
                for y, k in ((6.3, 0.6), (6.8, 0.4)):
                    f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.3), (f.w - 0.2, y + 0.05)], 0.14), K.deep(TROUS, 0.55), k)
                f.fill(f.m_polyline([(0.2, 6.0), (f.w * 0.5, 6.25), (f.w - 0.2, 6.0)], 0.22), K.lit(TROUS, 1.5), 0.3)
                # the tunic skirt over the thigh: aqua with the trim hem and a gold stitch
                sk = f.m_band(0.0, 3.4)
                K.cloth(f, sk, TUNIC, top=1.14, bottom=0.84, round_=0.12, fold_xs=[1.2, 2.8], fold_depth=0.08, fold_y0=0.2, fold_y1=3.0)
                H.trim_band(f, 3.1, 3.95, TRIM, spec=0.3, outline=0.55)
                K.seam(f, [(0.12, 3.55), (f.w - 0.12, 3.55)], GOLD, 0.055, 0.75, (0.3, 0.18), 0.2)
                H.shadow_under(f, 3.95, 0.8, 0.34)
                # the rolled hem of the trousers, the small shoes with a gold clasp
                H.trim_band(f, 8.9, 9.7, H.tone(TROUS, 1.25), spec=0.25, outline=0.55)
                K.boot(f, SHOE, 9.7, nm, toe=True, welt="#4a4a58", sole="#0a0a0e", shaft_end=11.3)
                if nm == "front":
                    f.ellipse(f.w * 0.5, 10.55, 0.62, 0.3, GOLD, a=0.95)
                    f.ellipse(f.w * 0.5 - 0.15, 10.45, 0.2, 0.1, "#fffbe0", a=0.8)


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

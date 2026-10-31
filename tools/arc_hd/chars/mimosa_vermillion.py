"""Mimosa Vermillion, the Golden Dawn's gentle healer, HD 256x256 (docs/arc/hd_arc_bc1.md): the hand-painted look of the shy, blushing girl with long wavy golden hair and green eyes.

What she wears is the 64x64 sheet's outfit (tools/arc/char_mimosa_vermillion.py): a cream tunic with a tan stand collar and tan cuffs, a brown belt with a gold buckle over a brown corset, short puffy white
shorts and brown knee boots. The golden Golden Dawn mantle (`cape`, `cape_low`) and the floating plant-green grimoire (`prop_float`, the only glow of the look) are 3D extras (`pmx_mimosa_vermillion`), so the
skin carries nothing of them. The blue-feathered shoulder collar of the Golden Dawn mantle (the sheet's `shoulder_fur` accessory, #5f7fb0, which the 64x64 sheet and the extras do not show) is painted on
the shoulders. The pink hair bow of the spec cannot be drawn: the 3D hair cap covers every place on the head where it could sit and the look has no ribbon extra, so it is left out.

Drawn at 4x detail on both layers: the big shaded green eyes with lashes and two highlights, worried golden brows, a strong blush and a small shy smile, a parted golden fringe with long locks along the
cheeks, the cream tunic with soft blouse folds and a stand collar with stitching, the blue feather collar on the shoulders, the corset with its lacing and gold eyelets, the belt with its buckle, a scalloped
tunic hem, the puffy sleeves with tan cuffs over bare hands, the white puffy shorts with a gathered hem, the bare thighs and the knee boots with a folded cuff and lacing.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus docs/arc/npcb.md
section 10 (which cites search results that disagreed on hair and eyes): long wavy golden hair with a pink bow, green eyes, a cream blouse, short puffy white trousers, a brown corset, blue-furred shoulders.
Not verified against a wiki: the bare thighs between the shorts and the boots (the sheet drew the boots up to the shorts) and the exact corset cut.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_long"
HAIR = "#f2d878"
HAIR_RGB = (242, 216, 120)
SKIN = ("#efb89e", "#fbd9c6", "#fff0e6")
TUNIC, TUNIC_D = "#f3e9cd", "#c9b88c"
TRIM, TRIM_D = "#cdb487", "#8e7650"
CORSET, CORSET_L = "#7a4c33", "#a97650"
LACE = "#f1e7c9"
BELT, BUCKLE = "#684628", "#d8b252"
SHORTS, SHORTS_D = "#f7f2ea", "#b9b6d2"
BOOT, CUFF = "#68422c", "#dac79d"
FUR, FUR_L, FUR_D = "#5f87c0", "#e2effc", "#35588c"
IRIS = ("#1d6a34", "#58c46a", "#d2fcc4")
BROW = "#b98f3c"


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.07, chin=0.06)
    pal = K.hair_palette(HAIR)
    # the golden fringe parted in the middle: a mass over the brow, a lock on each side of the parting, long locks along the cheeks (the 3D hair is drawn on top of it)
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.7), (7.5, 2.3), (6.6, 2.9), (5.6, 2.1), (4.7, 1.8), (4.0, 1.5), (3.3, 1.8), (2.4, 2.1), (1.4, 2.9), (0.6, 2.3), (-0.3, 2.7)]
    K.hair_mass(f, pal, outline, 0.0, 2.9, shadow_k=0.88, shadow_depth=1.8, streaks=(0.9, 2.0, 3.2, 4.8, 6.0, 7.1))
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.9, 1.6, 1.8, 1.9, -0.45)), (0.9, (5.3, 1.3, 1.7, 1.3, 0.25)), (0.9, (2.7, 1.3, 1.7, 1.3, -0.25)), (0.85, (1.1, 1.6, 1.8, 1.9, 0.45))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    for x, lean in ((0.4, 0.15), (7.6, -0.15)):
        K.lock(f, pal, x, 1.0, 1.4, 6.0, lean, 1.0)
    # worried brows (the inner ends high), big shaded eyes with long lashes and big highlights, a strong blush, a tiny nose and a small shy smile
    H.brow(f, (3.45, 3.55), (1.2, 3.95), BROW, 0.24, 0.09, arch=0.15)
    H.brow(f, (4.55, 3.55), (6.8, 3.95), BROW, 0.24, 0.09, arch=0.15)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.05, 2.7, 2.4, IRIS, lash="#4a3418", tilt=-0.06, lash_w=0.5, flick=0.4, lid_shadow=0.3, iris_r=1.1, hi=1.0)
    H.blush(f, 1.1, 6.35, 1.15, 0.6, "#f67e8c", 0.62)
    H.blush(f, 6.9, 6.35, 1.15, 0.6, "#f67e8c", 0.62)
    f.fill(f.m_line((0.8, 6.15), (1.2, 6.35), 0.07, 0.04), "#ff9aa6", 0.55)                    # blush strokes
    f.fill(f.m_line((1.2, 6.05), (1.6, 6.25), 0.07, 0.04), "#ff9aa6", 0.55)
    f.fill(f.m_line((6.8, 6.35), (7.2, 6.15), 0.07, 0.04), "#ff9aa6", 0.55)
    f.fill(f.m_line((6.4, 6.25), (6.8, 6.05), 0.07, 0.04), "#ff9aa6", 0.55)
    H.nose(f, 4.0, 6.2, "#dba08c", 0.18)
    H.mouth_smile(f, 4.0, 7.0, 2.0, color="#a8505a", lip="#ec9aa0", depth=0.14, thick=0.15)


# ================================================================================================ the body
def collar(f, front):
    """The tan stand collar with a lit top edge, stitching and a small gold button; the blue feathered collar of the mantle on the two shoulders."""
    x0, x1 = 1.5, f.w - 1.5
    if f.w < 6:
        K.feather_pad(f, -0.1, f.w + 0.1, 0.0, 2.4, FUR, FUR_L, FUR_D, seed=5)
        return
    band = f.m_rect(x0, 0.0, x1, 1.25)
    f.fill(band, H.lin((0, 0), (0, 1.25), [(0, K.lit(TRIM, 1.2)), (0.5, TRIM), (1, H.tone(TRIM, 0.75))]))
    f.fill(f.m_rect(x0, 1.18, x1, 1.32), TRIM_D, 0.9)
    K.seam(f, [(x0 + 0.15, 0.95), (x1 - 0.15, 0.95)], "#f4e6c4", 0.05, 0.6, (0.3, 0.2), 0.15)
    H.shadow_under(f, 1.3, 0.5, 0.28, x0=x0, x1=x1)
    if front:
        f.fill(f.m_line((4.0, 0.0), (4.0, 1.25), 0.09, 0.09), TRIM_D, 0.8)
        H.stud(f, 4.0, 0.7, 0.16, "#e8c870")
    K.feather_pad(f, -0.1, x0 + 0.05, 0.0, 2.5, FUR, FUR_L, FUR_D, seed=3)
    K.feather_pad(f, x1 - 0.05, f.w + 0.1, 0.0, 2.5, FUR, FUR_L, FUR_D, seed=4)


def tunic_front(f):
    m = f.m_all()
    K.cloth(f, m, TUNIC, top=1.04, bottom=0.82, round_=0.08, fold_xs=[1.3, 6.7], fold_depth=0.06, fold_y0=2.0, fold_y1=5.4)
    H.sheen(f, m, [2.0, 6.0], width=0.22, strength=0.12, tilt=0.02, y0=1.8, y1=5.2, color="#fffaf0")
    # the soft gathered blouse over the bust: two shallow shadow arcs, a lit centre, a few drape folds
    f.shade(m, 1.0 + 0.08 * K.blob(f, 4.0, 3.2, 3.0, 1.5, 0.9), 0.9)
    for sgn in (-1.0, 1.0):
        cx = 4.0 + sgn * 1.5
        f.fill(f.m_polyline([(cx - 1.2, 3.9), (cx - 0.4, 4.25), (cx + 0.4, 4.25), (cx + 1.2, 3.9)], 0.24, 2.0), TUNIC_D, 0.45)
    K.crease(f, [(1.1, 2.3), (1.5, 3.2), (1.3, 4.2)], TUNIC, 0.08, 0.35, light=1.05)
    K.crease(f, [(6.9, 2.3), (6.5, 3.2), (6.7, 4.2)], TUNIC, 0.08, 0.35, light=1.05)
    collar(f, True)


def corset(f, front):
    """The brown corset (rows 5.2..9.0): a leather gradient, lit seams at the boning, lacing in the middle of the front with gold eyelets, a stitched top edge."""
    m = f.m_rect(0.0, 5.2, f.w, 9.0)
    f.fill(m, H.lin((0, 5.2), (0, 9.0), [(0, K.lit(CORSET, 1.25)), (0.5, CORSET), (1, H.tone(CORSET, 0.75))]))
    away = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 1.4, away)), 0.9)
    H.shadow_under(f, 5.2, 0.4, 0.3)
    f.fill(f.m_rect(0.0, 5.2, f.w, 5.34), K.lit(CORSET_L, 1.4), 0.65)
    K.seam(f, [(0.0, 5.55), (f.w, 5.55)], "#d8b896", 0.05, 0.6, (0.3, 0.2), 0.15)
    for x in (1.6, 2.7, 5.3, 6.4):                                                              # the boning: a dark line with a light line beside it
        f.fill(f.m_line((x, 5.7), (x, 8.8), 0.1, 0.1), H.tone(CORSET, 0.5), 0.6)
        f.fill(f.m_line((x + 0.14, 5.7), (x + 0.14, 8.8), 0.06, 0.06), CORSET_L, 0.5)
    if front:
        f.fill(f.m_rect(3.4, 5.5, 4.6, 8.9), H.tone(CORSET, 0.55), 0.7)
        for i, y in enumerate(np.arange(5.8, 8.8, 0.75)):                                       # the lacing: crossed cream cords through gold eyelets
            H.stud(f, 3.45, y, 0.13, "#e6c670", spec=0.5)
            H.stud(f, 4.55, y, 0.13, "#e6c670", spec=0.5)
            a, b = ((3.45, y), (4.55, y + 0.75)) if i % 2 == 0 else ((4.55, y), (3.45, y + 0.75))
            f.fill(f.m_line(a, b, 0.13, 0.13), LACE, 0.95)
    else:
        f.fill(f.m_line((4.0, 5.5), (4.0, 8.9), 0.12, 0.12), H.tone(CORSET, 0.5), 0.5)


def belt(f, front):
    K.belt_band(f, 9.0, 10.05, BELT, stitch="#a98a60")
    K.belt_band(f, 10.05, 10.4, H.tone(BELT, 0.62), stitch_a=0.0)
    H.shadow_under(f, 10.4, 0.4, 0.28)
    if front:
        K.buckle(f, 4.0, 9.55, 2.1, 1.4, BUCKLE)


def hem(f, front):
    """Rows 10.4..12: the hip-length tunic flares over the shorts; a tan trim with a scalloped edge."""
    m = f.m_rect(0.0, 10.4, f.w, 12.0)
    f.fill(m, H.lin((0, 10.4), (0, 12.0), [(0, H.tone(TUNIC, 0.9)), (1, H.tone(TUNIC, 0.7))]))
    for x in np.arange(0.9, f.w, 1.8):                                                            # the folds of the flare
        f.fill(f.m_line((x, 10.5), (x + 0.15, 11.5), 0.12, 0.12), TUNIC_D, 0.5)
    f.fill(f.m_rect(0.0, 11.5, f.w, 12.0), H.lin((0, 11.5), (0, 12.0), [(0, TRIM), (1, H.tone(TRIM, 0.8))]))
    f.fill(f.m_rect(0.0, 11.46, f.w, 11.56), TRIM_D, 0.7)
    K.seam(f, [(0.1, 11.78), (f.w - 0.1, 11.78)], "#f4e6c4", 0.05, 0.6, (0.3, 0.2), 0.15)
    H.shadow_under(f, 10.4, 0.4, 0.28)


def body_front(f, ov):
    tunic_front(f)
    corset(f, True)
    hem(f, True)
    belt(f, True)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, TUNIC, top=1.04, bottom=0.82, round_=0.08, fold_xs=[1.7, 6.3], fold_depth=0.06, fold_y0=1.5, fold_y1=5.4)
    f.fill(f.m_line((4.0, 1.4), (4.0, 5.2), 0.12, 0.1), TUNIC_D, 0.4)
    collar(f, False)
    corset(f, False)
    hem(f, False)
    belt(f, False)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), TUNIC, top=1.04, bottom=0.82, round_=0.1, fold_xs=[1.2, 2.6], fold_depth=0.06, fold_y0=1.5, fold_y1=5.4)
    collar(f, False)
    corset(f, False)
    hem(f, False)
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


# ================================================================================================ the arms (cream sleeves, tan cuffs, bare hands)
def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            sleeve = f.m_band(0.0, 9.9)
            K.cloth(f, sleeve, TUNIC, top=1.05, bottom=0.82, round_=0.14, fold_xs=[f.w * 0.35, f.w * 0.78], fold_depth=0.07, fold_y0=1.5, fold_y1=9.4)
            f.shade(sleeve, 1.0 + 0.07 * K.blob(f, f.w * 0.4, 4.0, f.w * 0.7, 2.0), 0.9)
            for y, k in ((4.6, 0.4), (5.2, 0.3)):                                                  # the sleeve gathers at the elbow
                K.crease(f, [(0.0, y), (f.w * 0.4, y + 0.28), (f.w, y + 0.05)], TUNIC, 0.09, k, light=1.1, mask=sleeve)
            cuff = f.m_rect(0.0, 9.6, f.w, 10.8)
            f.fill(cuff, H.lin((0, 9.6), (0, 10.8), [(0, K.lit(TRIM, 1.2)), (1, H.tone(TRIM, 0.78))]))
            f.fill(f.m_rect(0.0, 9.6, f.w, 9.74), TRIM_D, 0.7)
            K.seam(f, [(0.1, 10.2), (f.w - 0.1, 10.2)], "#f4e6c4", 0.05, 0.6, (0.3, 0.2), 0.15)
            H.shadow_under(f, 10.8, 0.4, 0.28)
            if nm == "front":
                H.stud(f, f.w * 0.5, 10.2, 0.14, "#e8c870", spec=0.6)
            hand = f.m_band(10.8, 12.0)
            f.fill(hand, H.lin((0, 10.8), (0, 12.0), [(0, H.tone(SKIN[1], 0.92)), (1, SKIN[0])]))
            K.fingers(f, SKIN, 11.2, 12.0, n=2)
            K.feather_pad(f, -0.1, f.w + 0.1, 0.0, 2.8, FUR, FUR_L, FUR_D, seed=11 + len(nm) + (3 if part == "arm_l" else 0))         # the feathered collar rests on the shoulder


# ================================================================================================ the legs
def laces(f):
    """The lacing up the front of the boot: gold eyelets on both sides of the front and crossed brown cords."""
    for i, y in enumerate(np.arange(6.9, 10.2, 0.8)):
        H.stud(f, 1.4, y, 0.1, "#e6c670", spec=0.5)
        H.stud(f, 2.6, y, 0.1, "#e6c670", spec=0.5)
        a, b = ((1.4, y), (2.6, y + 0.8)) if i % 2 == 0 else ((2.6, y), (1.4, y + 0.8))
        f.fill(f.m_line(a, b, 0.12, 0.12), "#e0cda0", 0.9)


def leg(f, ov, name):
    # the bare thigh from the shorts' hem (row 3.4) to the boot (row 5.3)
    thigh = f.m_band(0.0, 5.6)
    f.fill(thigh, H.lin((0, 0), (0, 5.6), [(0, H.tone(SKIN[1], 0.9)), (0.6, SKIN[1]), (1, H.tone(SKIN[0], 0.95))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(thigh, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
    f.shade(thigh, 1.0 + 0.1 * K.blob(f, f.w * 0.4, 4.2, f.w * 0.6, 1.1), 0.9)
    # the puffy white shorts (rows 0..3.6): soft cool shadows, vertical gathers, a gathered hem band
    sh = f.m_band(0.0, 3.7)
    K.cloth(f, sh, SHORTS, top=1.02, bottom=0.86, round_=0.16, fold_xs=[f.w * 0.3, f.w * 0.7], fold_depth=0.1, fold_y0=0.3, fold_y1=3.2)
    f.shade(sh, 1.0 - 0.1 * K.blob(f, f.w * 0.5, 3.3, f.w * 0.6, 0.7, 1.0), 0.9)
    f.fill(f.m_rect(0.0, 3.1, f.w, 3.7), H.lin((0, 3.1), (0, 3.7), [(0, K.lit(SHORTS_D, 1.1)), (1, H.tone(SHORTS_D, 0.8))]), 0.9)         # the gathered hem band
    for x in np.arange(0.35, f.w, 0.5):
        f.fill(f.m_line((x, 3.12), (x + 0.04, 3.66), 0.06, 0.05), H.tone(SHORTS_D, 0.6), 0.55)
    f.fill(f.m_rect(0.0, 3.06, f.w, 3.14), H.tone(SHORTS_D, 0.6), 0.6)
    H.shadow_under(f, 3.7, 0.8, 0.34, mask=thigh)
    # the knee boots: a folded cream-tan cuff with a stitch, then the brown shaft with its lacing in front
    cuff = f.m_rect(0.0, 5.3, f.w, 6.4)
    f.fill(cuff, H.lin((0, 5.3), (0, 6.4), [(0, K.lit(CUFF, 1.12)), (1, H.tone(CUFF, 0.78))]))
    f.fill(f.m_rect(0.0, 5.3, f.w, 5.44), "#4a3020", 0.55)
    K.seam(f, [(0.12, 5.9), (f.w - 0.12, 5.9)], "#8e7650", 0.05, 0.7, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 6.4, 11.25, name, strap=None)
    if name == "front":
        laces(f)


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
    K.head_rear(cv, K.hair_palette(HAIR), back_y=7.0, nape_y=8.0, side_top=3.0, ear_x=4.3, side_low=6.4)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim

"""Lucius Zogratis, the Wizard King's other soul and the mod's main Zogratis antagonist, HD 256x256 (docs/arc/hd_arc_bc5.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_lucius_zogratis.py): a huge white fur mantle over a tall dark collar and a choker, a floor-length brown-maroon robe crossed by dark straps with
wide sleeves and dark cuffs, a dark sash with a gold buckle, dark gloves and ankle boots. The long black cloak with its dark fur lining, the floating grimoire and, in the divine form, the horns
(antlers), the three pairs of wings and the halo of gears are the `pmx_lucius_zogratis*` extras (the cloak hangs over the back of the body and the shoulders from behind): nothing of them is painted on
the skin, the back of the robe is kept plain under the cloak and only the clasp of the cloak is left to the extra.
Drawn at 4x detail on both layers: a pale, sharp face with narrow red eyes (glowing), thin arched brows, a faint smirk and a small sigil on the forehead; the fur mantle (fluffy white fur in three tones
with a tufted lower edge) round the shoulders and over the arms; the robe in lit panels with two crossed dark leather straps and a studded clasp at the crossing, the dark stand collar and choker
under the fur, the sash with a gold buckle, wide sleeves with dark cuffs, gloves; the robe's lower skirt with folds and a dark hem, ankle boots.
Form 2 (`lucius_zogratis_form2.py`, the divine form) runs this same code with FORM2 = True: a pale-blue and white layered robe with gold borders, a golden sun medallion that glows, a gold glowing
necklace, white gloves, wide golden glowing eyes, white spikes, golden light lines on the face (the glow map).

Source (rule 10): no wiki page could be fetched in this environment (English and Japanese fandom are blocked); the look is from memory of the series (a pale man with black spiky hair, red eyes, a
white fur mantle and a dark cloak) plus docs/arc/remodel.md and the sheet. The cut, the straps and the colours are the sheet's choice.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

FORM2 = False
HAIR_STYLE = "anime_spike_tall"
HAIR = "#1a1618"
HAIR_RGB = (26, 22, 24)
ROBE = "#7a4444"
STRAP = "#2a1a20"
FUR = "#f2f0f6"
FUR_L = "#ffffff"
FUR_D = "#bcb8cc"
GLOVE = "#322a34"
SASH = "#2c1c22"
GOLD = "#d4ac48"
GOLD_D = "#7a5a1c"
SUN = "#ffd870"
RED = "#d02a34"
RED_G = "#ff3a44"


def skin_tones():
    return K.skin3("pale")


def robe_color():
    return "#cde0f8" if FORM2 else ROBE


def gold():
    return "#e8b830" if FORM2 else GOLD


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    skin, crease = skin_tones()
    K.face_base(f, skin, side=0.09, chin=0.08)
    f.fill(f.m_ellipse(1.7, 6.3, 0.8, 0.9, soft=3.0), H.tone(skin[0], 0.92), 0.28)                       # a narrow face: the cheeks hollow
    f.fill(f.m_ellipse(6.3, 6.3, 0.8, 0.9, soft=3.0), H.tone(skin[0], 0.92), 0.28)
    if FORM2:
        brow = "#c8c0d0"
        H.brow(f, (3.55, 3.9), (0.95, 3.55), brow, 0.24, 0.12, arch=0.1)
        H.brow(f, (4.45, 3.9), (7.05, 3.55), brow, 0.24, 0.12, arch=0.1)
        for cx in (2.4, 5.6):                                                                          # wide golden eyes that glow
            H.eye(f, cx, 5.1, 2.5, 2.0, ("#8a5a10", "#f0c040", "#fff6c0"), lash="#8a6a20", tilt=0.0, lash_w=0.36, flick=0.3, lid_shadow=0.2,
                  glow="#ffd870", hi=0.95, pupil="#fff2b0")
        for sg in (-1.0, 1.0):                                                                         # the light lines: three thin golden lines on each cheek
            x0 = 4.0 + sg * 3.2
            for i, y in enumerate((6.0, 6.55, 7.1)):
                pts = [(x0 + sg * 0.6, y - 0.1), (x0, y), (x0 - sg * (1.3 - i * 0.15), y + 0.28)]
                f.fill(f.m_polyline(pts, 0.15), "#f0c040", 0.95, glow="#ffd870")
                f.fill(f.m_polyline(pts, 0.06), "#fffbe0", 0.8)
        H.mouth_smile(f, 4.0, 7.0, 2.0, color="#b87a7a", lip="#e0a8a0", depth=0.0, thick=0.15)
    else:
        brow = "#1a1620"
        H.brow(f, (3.55, 4.0), (0.9, 3.35), brow, 0.3, 0.12, arch=0.1)
        H.brow(f, (4.45, 4.0), (7.1, 3.35), brow, 0.3, 0.12, arch=0.1)
        for cx in (2.4, 5.6):                                                                          # narrow sharp red eyes: lids half down, the outer corner raised
            H.eye(f, cx, 5.2, 2.6, 1.55, ("#6a0a14", RED, "#ff9a98"), lash="#14101a", tilt=0.22, lash_w=0.46, flick=0.46, lid_shadow=0.46,
                  glow=RED_G, hi=0.85, pupil="#14060a")
            f.fill(f.m_polyline([(cx - 1.2, 6.0), (cx, 6.18), (cx + 1.2, 6.0)], 0.12), "#b68a96", 0.4)
        H.mouth_smile(f, 4.0, 7.0, 2.0, color="#a05a68", lip="#d09aa0", depth=0.16, thick=0.17)           # a faint smirk
        f.fill(f.m_line((5.0, 6.95), (5.45, 6.75), 0.12, 0.12), "#a05a68", 0.7)
    H.nose(f, 4.0, 6.2, crease, 0.2)
    # the sigil on the left of the forehead: a small diamond with a stroke across it, red (gold in the second form), lit on the glow map
    sx, sy = 3.1, 2.75
    col, gcol = ("#f0c040", "#ffd870") if FORM2 else ("#e0303c", RED_G)
    d = [(sx, sy - 0.55), (sx + 0.38, sy), (sx, sy + 0.62), (sx - 0.38, sy)]
    f.fill(f.m_polyline(d + [d[0]], 0.13), col, 0.95, glow=gcol)
    f.fill(f.m_line((sx - 0.7, sy + 0.05), (sx + 0.7, sy + 0.05), 0.11, 0.11), col, 0.9, glow=gcol)
    f.fill(f.m_line((sx, sy + 0.4), (sx, sy + 1.0), 0.11, 0.08), col, 0.85, glow=gcol)


def head_rest(cv, img64, spec):
    skin, _ = skin_tones()
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, skin)


# ================================================================================================ the mantle and the robe
def mantle(f, y0, y1, name, seed):
    """The white fur mantle: a thick band of fur y0..y1 with a tufted lower edge, light and dark strokes."""
    K.fur_band(f, y0, y1, FUR, FUR_L, FUR_D, tuft=0.55, seed=seed)
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.15), "#ffffff", 0.5)


def collar_dark(f, name):
    """The tall dark collar and the choker between the mantle's two sides: seen at the neck, in the fur's opening."""
    if name in ("front", "back"):
        c = "#241a22" if not FORM2 else gold()
        f.fill(f.m_poly([(2.4, 0.0), (5.6, 0.0), (5.2, 1.4), (4.0, 1.9), (2.8, 1.4)]), H.lin((0, 0), (0, 1.9), [(0, K.lit(c, 1.3)), (1, H.tone(c, 0.7))]), 1.0)
        f.fill(f.m_polyline([(2.4, 0.2), (2.8, 1.4), (4.0, 1.9), (5.2, 1.4), (5.6, 0.2)], 0.12), "#0a060a", 0.8)
        if name == "front":
            H.stud(f, 4.0, 1.5, 0.2, gold() if not FORM2 else "#fff0b0", spec=0.8, glow=("#ffd870" if FORM2 else None))


def straps(f):
    """Two crossed dark leather straps over the chest with a studded ring where they cross (form 1); in the divine form the gold borders and the sun medallion replace them."""
    for a, b in (((0.6, 2.2), (7.3, 9.0)), ((7.4, 2.2), (0.7, 9.0))):
        m = f.m_line(a, b, 0.7, 0.7)
        f.fill(f.m_line(a, b, 0.95, 0.95), "#0a060a", 0.7)
        f.fill(m, H.lin((0, 2.0), (0, 9.0), [(0, K.lit(STRAP, 1.6)), (1, H.tone(STRAP, 0.8))]))
        f.fill(f.m_line((a[0] + 0.05, a[1] - 0.1), (b[0] + 0.05, b[1] - 0.1), 0.12, 0.12) * m, K.lit(STRAP, 3.2), 0.5)
        K.seam(f, [(a[0] + 0.12, a[1] + 0.3), (b[0] + 0.12, b[1] - 0.3)], "#8a6a58", 0.04, 0.5, (0.3, 0.2), 0.0)
    cx, cy = 4.0, 5.6
    f.ellipse(cx, cy, 0.92, 0.92, "#0a060a")
    f.fill(f.m_ring(cx, cy, 0.78, 0.3), H.lin((0, cy - 0.8), (0, cy + 0.8), [(0, K.lit(GOLD, 1.5)), (0.5, GOLD), (1, H.tone(GOLD, 0.55))]), 1.0)
    H.stud(f, cx, cy, 0.34, GOLD, spec=0.8)


def sun(f, cx, cy, r):
    """The golden sun medallion of the divine form: a disc with twelve rays, a ring and a lit core; the whole glows."""
    c = gold()
    for i in range(12):
        a = np.pi * 2 * i / 12
        ca, sa = np.cos(a), np.sin(a)
        f.poly([(cx + ca * r * 0.62 - sa * r * 0.13, cy + sa * r * 0.62 + ca * r * 0.13), (cx + ca * r * 1.28, cy + sa * r * 1.28), (cx + ca * r * 0.62 + sa * r * 0.13, cy + sa * r * 0.62 - ca * r * 0.13)],
               H.tone(c, 1.1) if i % 2 == 0 else H.tone(c, 0.8), 0.98)
    f.ellipse(cx, cy, r * 0.74, r * 0.74, "#5a3a08")
    f.ellipse(cx, cy, r * 0.66, r * 0.66, H.rad((cx - r * 0.2, cy - r * 0.25), r * 0.9, [(0, "#fff6c0"), (0.5, SUN), (1, H.tone(c, 0.8))]))
    f.fill(f.m_ring(cx, cy, r * 0.42, 0.1), H.tone(c, 0.7), 0.8)
    f.glow_fill(f.m_ellipse(cx, cy, r * 0.95, r * 0.95, soft=3.0), SUN, 0.85)


def body_front(f, ov):
    rc = robe_color()
    K.cloth(f, f.m_all(), rc, top=1.35, bottom=0.78, round_=0.14, fold_xs=[1.5, 6.5], fold_depth=0.07, fold_y0=2.5, fold_y1=8.8)
    if FORM2:
        # a pale-blue robe with a white tabard over it, gold borders down both sides and a gold-edged hem
        tab = f.m_poly([(1.3, 2.0), (6.7, 2.0), (6.9, 9.0), (1.1, 9.0)])
        K.cloth(f, tab, "#eef4fc", top=1.12, bottom=0.82, round_=0.1, fold_xs=[2.6, 5.4], fold_depth=0.06, fold_y0=2.5, fold_y1=8.8)
        f.fill(f.m_polyline([(1.3, 2.0), (1.1, 9.0)], 0.2), "#7a9ac8", 0.9)
        f.fill(f.m_polyline([(6.7, 2.0), (6.9, 9.0)], 0.2), "#7a9ac8", 0.9)
        for x0, x1 in ((0.0, 0.95), (7.05, 8.0)):
            f.fill(f.m_rect(x0, 0.0, x1, 12.0), H.lin((0, 0), (0, 12), [(0, K.lit(gold(), 1.25)), (1, H.tone(gold(), 0.7))]))
            f.fill(f.m_rect(x0, 0.0, x1, 12.0) * f.m_rect(x0 + 0.1, 0, x0 + 0.3, 12), "#fff0b0", 0.45)
        K.sheen(f, tab, [3.0], 2.4, 8.6, 0.2, "#ffffff", width=0.35)
        sun(f, 4.0, 4.6, 1.5)
        for y in (6.9, 7.7):
            K.seam(f, [(1.5, y), (6.5, y)], "#8aa8d0", 0.05, 0.6)
    else:
        for x0, x1 in ((0.0, 3.2), (4.8, 8.0)):
            K.sheen(f, f.m_rect(x0, 2.4, x1, 8.8), [x0 + 0.8 if x0 < 4 else x1 - 0.8], 2.6, 8.6, 0.26, "#e0a0a0", width=0.3)
        f.fill(f.m_rect(0.0, 2.4, 0.3, 8.9), K.lit(rc, 1.6), 0.3)
        f.fill(f.m_rect(7.7, 2.4, 8.0, 8.9), "#000000", 0.3)
        for y in (3.3, 6.5):                                                                           # the sheet's two darker bands across the robe
            f.fill(f.m_rect(0.0, y, 8.0, y + 0.7), H.tone(rc, 0.62), 0.55)
        straps(f)
    belt(f, True)
    mantle(f, 0.0, 2.3, "front", 1)
    collar_dark(f, "front")
    hem(f, 10.9)


def belt(f, front=False):
    c = gold()
    sash = "#d8b050" if FORM2 else SASH
    K.belt_band(f, 8.9, 10.1, "#f0e4b0" if FORM2 else SASH, stitch="#8a6a58" if not FORM2 else "#d8b868")
    if front:
        K.buckle(f, 4.0, 9.5, 2.2, 1.5, c, hole="#1a1014" if not FORM2 else "#7a5a1c")
        if FORM2:
            H.stud(f, 4.0, 9.5, 0.3, "#fff0b0", spec=0.9, glow="#ffd870")
    else:
        H.stud(f, 4.0, 9.5, 0.18, c, spec=0.5)


def hem(f, y0):
    c = gold()
    if FORM2:
        f.fill(f.m_band(y0, 12.0), H.lin((0, y0), (0, 12.0), [(0, K.lit(c, 1.3)), (0.5, c), (1, H.tone(c, 0.66))]))
        f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.18), "#5a3a08", 0.7)
        for x in np.arange(0.0, f.w + 0.5, 1.0):
            f.fill(f.m_poly([(x - 0.4, y0 + 0.5), (x + 0.4, y0 + 0.5), (x, y0 + 1.0)]), H.tone(c, 0.55), 0.6)
    else:
        f.fill(f.m_band(y0, 12.0), H.lin((0, y0), (0, 12.0), [(0, "#34222a"), (1, "#1a1016")]))
        f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.14), "#0a060a", 0.7)
        f.fill(f.m_rect(0.0, y0 + 0.14, f.w, y0 + 0.26), K.lit("#34222a", 2.2), 0.5)
    H.shadow_under(f, y0, 0.5, 0.26)


def body_back(f, ov):
    rc = robe_color()
    K.cloth(f, f.m_all(), rc, top=1.3, bottom=0.78, round_=0.14, fold_xs=[1.5, 3.0, 5.0, 6.5], fold_depth=0.07, fold_y0=2.5, fold_y1=8.8)
    K.sheen(f, f.m_rect(0, 2.4, 8, 8.8), [1.8, 6.2], 2.6, 8.4, 0.2, "#e0a0a0" if not FORM2 else "#ffffff", width=0.3)
    f.fill(f.m_line((4.0, 2.4), (4.0, 8.8), 0.18, 0.18), H.tone(rc, 0.5), 0.7)
    if FORM2:
        for x0, x1 in ((0.0, 0.95), (7.05, 8.0)):
            f.fill(f.m_rect(x0, 0.0, x1, 12.0), H.lin((0, 0), (0, 12), [(0, K.lit(gold(), 1.25)), (1, H.tone(gold(), 0.7))]))
        sun(f, 4.0, 5.0, 1.2)
    else:
        for y in (3.3, 6.5):
            f.fill(f.m_rect(0.0, y, 8.0, y + 0.7), H.tone(rc, 0.62), 0.55)
    belt(f)
    mantle(f, 0.0, 2.3, "back", 2)
    hem(f, 10.9)


def body_side(f, ov, name):
    rc = robe_color()
    K.cloth(f, f.m_all(), rc, top=1.3, bottom=0.78, round_=0.14, fold_xs=[1.5], fold_depth=0.07, fold_y0=2.5, fold_y1=8.8)
    if FORM2:
        edge = f.m_rect(f.w - 1.0, 0, f.w, 12) if name == "west" else f.m_rect(0, 0, 1.0, 12)
        f.fill(edge, H.lin((0, 0), (0, 12), [(0, K.lit(gold(), 1.25)), (1, H.tone(gold(), 0.7))]))
    else:
        for y in (3.3, 6.5):
            f.fill(f.m_rect(0.0, y, f.w, y + 0.7), H.tone(rc, 0.62), 0.55)
    belt(f)
    mantle(f, 0.0, 2.3, name, 3)
    hem(f, 10.9)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ the arms and legs
def arm(cv, part):
    rc = robe_color()
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sl = f.m_band(0.0, 8.0)
            K.cloth(f, sl, rc, top=1.35, bottom=0.8, round_=0.18, fold_xs=[1.0, 2.5], fold_depth=0.07, fold_y0=2.0, fold_y1=7.6)
            K.sheen(f, sl, [f.w * 0.3], 2.4, 7.6, 0.24, "#e0a0a0" if not FORM2 else "#ffffff", width=0.3)
            if FORM2:
                K.band(f, 5.6, 6.1, gold(), spec=0.1)
            else:
                f.fill(f.m_polyline([(0.0, 4.4), (f.w * 0.5, 4.65), (f.w, 4.45)], 0.18) * sl, H.tone(rc, 0.55), 0.55)
            # the wide cuff: dark in the first form (the sheet's dark trim), gold-edged white in the second
            if FORM2:
                K.cloth(f, f.m_band(6.1, 8.0), "#eef4fc", top=1.1, bottom=0.82, round_=0.1)
                K.band(f, 7.6, 8.1, gold(), spec=0.15)
            else:
                K.cloth(f, f.m_band(6.9, 8.1), "#30202a", top=1.5, bottom=0.8, round_=0.12)
                K.seam(f, [(0.1, 7.35), (f.w - 0.1, 7.35)], "#8a6a58", 0.05, 0.6)
                f.fill(f.m_rect(0.0, 6.88, f.w, 7.0), K.lit("#30202a", 2.5), 0.5)
            H.shadow_under(f, 8.1, 0.6, 0.32)
            K.glove(f, "#f4f2f8" if FORM2 else GLOVE, 8.1, 12.0, nm, finger_n=3, knuckle_k=1.1 if FORM2 else 1.5)
            mantle(f, 0.0, 2.35, nm, 5 + len(nm))                                                         # the fur over the shoulder


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", FUR, edge=FUR_D, k_top=1.05, overlay=ov)


def legs(cv):
    rc = robe_color()
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.cloth(f, f.m_band(0.0, 11.0), rc, top=1.25, bottom=0.75, round_=0.14, fold_xs=[1.3, 2.7], fold_depth=0.1, fold_y0=0.4, fold_y1=10.4)
                K.sheen(f, f.m_band(0.0, 11.0), [f.w * 0.3], 0.6, 10.2, 0.2, "#e0a0a0" if not FORM2 else "#ffffff", width=0.3)
                if FORM2:
                    # the white under-skirt over the blue one, its gold-fringed edge, and the bare toe under the hem
                    K.cloth(f, f.m_band(0.0, 3.4), "#eef4fc", top=1.12, bottom=0.82, round_=0.1, fold_xs=[2.0], fold_depth=0.06, fold_y0=0.2, fold_y1=3.2)
                    K.band(f, 3.4, 4.0, gold(), spec=0.15)
                    H.shadow_under(f, 4.0, 0.8, 0.34)
                    for y in (7.2, 9.0):
                        f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.25), (f.w, y + 0.05)], 0.12), "#6293d3", 0.35)
                else:
                    for y in (2.9, 3.9):
                        f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.25), (f.w, y + 0.05)], 0.14), H.tone(rc, 0.55), 0.5)
                hem(f, 11.0)
                if nm == "front" and not FORM2:
                    f.fill(f.m_ellipse(f.w * 0.5, 11.45, 1.3, 0.35, soft=2.0), K.lit("#34222a", 1.8), 0.4)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    head_rest(cv, img64, spec)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    arm_tops(cv)
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)

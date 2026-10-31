"""Nozel Silva, Captain of the Silver Eagle, HD 256x256 (docs/arc/hd_arc_bc2.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_nozel_silva.py): a knee-length silver-white coat with a high steel-blue stand collar, an open front that shows a dark navy shirt with
a row of buttons, a dark belt with a silver buckle, long sleeves with steel-blue cuffs over silver-white gloves, a coat skirt that ends in a steel-blue hem, and knee-high dark boots with silver
cuffs. The cape with the wings of the Silver Eagle and the floating grimoire are the `pmx_nozel_silva` extra (the cape hangs from the shoulders at the back and covers the whole back, so the back of
the coat is kept plain; the grimoire floats at the left of the body). No glow on the skin (the glow of this character is the grimoire's mercury light, on the extra).
Drawn at 4x detail on both layers: the cold half-lidded eyes with the long lashes, the flat pale brows, the long side fringe over one eye, the coat panels with their lit edges, piping and
stitching, the collar, the navy placket with silver buttons, the belt and buckle, the stitched cuffs, the gloves with finger gaps, the hem trim, the boots with their silver cuffs.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus docs/arc/npcb.md: a tall slim noble,
silver hair with a long side fringe, a cold blue-violet gaze, a pale face with no smile, the silver-white captain's coat. Colours are the sheet's, pushed toward blue-violet shadows.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc2 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_swept"                                    # the owner's silver reference 155: a big swept silver mass with a long fringe over one eye
HAIR = "#e6eaf8"                                              # a cool silver; the hair sheet is grey and the tint multiplies, so it must start very light
HAIR_RGB = (230, 234, 248)
SKIN = ("#e8c3b3", "#f6dccf", "#fdeee4")
SKIN_D = "#c49a8c"
COAT = "#d7dbe9"                                              # silver-white coat (the sheet's #d4d8e4)
TRIM = "#6f79a6"                                              # steel blue (the sheet's #8890a8, a little deeper and bluer so it reads as a colour block)
NAVY = "#262b4a"                                              # the dark inner shirt (the sheet's #2a2e3a)
NAVY_L = "#3b4274"
SILVER = "#c9cddb"
BOOT = "#1f2230"
BELT = "#2c3044"
GLOVE = "#dde0ec"
PIPE = "#eceef8"                                              # light stitching on the steel-blue trim
IRIS = ("#1e2a78", "#5a76d2", "#b4c8ff")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, (SKIN[0], SKIN[1], SKIN[2]), side=0.10, chin=0.08)
    pal = K.hair_pal(HAIR, light_k=1.04, dark_k=0.66)
    # a cool shade under the lid and a faint violet shadow at the temples: a pale noble face
    f.fill(f.m_ellipse(2.35, 5.45, 1.5, 0.5, soft=2.5), "#c9aebf", 0.28)
    f.fill(f.m_ellipse(5.65, 5.45, 1.5, 0.5, soft=2.5), "#c9aebf", 0.28)
    # the cold eyes: narrow, half-lidded, the outer corner raised; the iris is blue-violet
    for cx in (2.35, 5.65):
        H.eye(f, cx, 5.0, 2.5, 1.55, IRIS, lash="#161a30", tilt=0.2, lash_w=0.44, flick=0.38, lid_shadow=0.5, hi=0.8)
    # thin flat pale brows
    H.brow(f, (3.45, 3.9), (1.2, 3.6), "#aeb4cc", 0.30, 0.13, arch=0.04)
    H.brow(f, (4.55, 3.9), (6.8, 3.6), "#aeb4cc", 0.30, 0.13, arch=0.04)
    H.nose(f, 4.0, 6.15, "#cfa699", 0.22)
    H.mouth_smile(f, 4.0, 7.1, 1.6, color="#a46d6c", lip="#d99d98", depth=-0.05, thick=0.17)
    # the hair of the fringe: a swept silver mass, one long side-swept bang over the left eye of the picture (the character's right), shorter locks on the other side
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.2, 1.7), (6.1, 2.5), (5.0, 1.2), (3.8, 1.9), (2.2, 1.7), (0.9, 2.1), (-0.3, 2.3)], y_bot=2.4)
    K.strands(f, pal, (0.8, 2.0, 3.3, 4.6, 5.9, 7.1), 0.0, 1.9, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.85, (4.6, 2.2, 0.8, 4.3, -2.7)),                 # the long bang: from the parting down across the left eye
        (0.9, (3.3, 2.0, 1.0, 3.7, -2.0)),
        (0.95, (2.0, 1.9, 1.0, 3.2, -1.2)),
        (0.9, (6.3, 1.8, 1.1, 2.0, 0.6)),                   # the short locks of the other side
        (0.95, (7.3, 1.6, 1.0, 2.9, 0.3)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.4, 1.0, 0.8, 4.6, 0.25, 1.0)           # the sideburn locks that frame the face
    K.lock(f, pal, 7.6, 1.0, 0.8, 4.8, -0.25, 1.0)


# ================================================================================================ the coat
def coat_panel(f, x0, x1, side):
    """One front panel of the coat (side -1 = the left of the picture): silver cloth, a lit outer edge, soft folds, a steel-blue piped edge along the opening."""
    m = f.m_rect(x0, 0.0, x1, 12.0)
    K.cloth(f, m, COAT, top=1.10, bottom=0.84, round_=0.10, fold_xs=[x0 + (x1 - x0) * 0.42], fold_depth=0.09, fold_y0=1.8, fold_y1=11.0)
    K.sheen(f, m, [x0 + (x1 - x0) * 0.3], 2.0, 8.4, 0.2, "#ffffff")
    outer = x0 if side < 0 else x1
    f.fill(m * f.m_rect(min(outer, outer + side * -0.3), 0, max(outer, outer + side * -0.3), 12), K.lit(COAT, 1.25), 0.4)


def body_front(f, ov):
    coat_panel(f, 0.0, 3.1, -1)
    coat_panel(f, 4.9, 8.0, 1)
    # the navy shirt in the opening, with its buttons
    pl = f.m_rect(3.1, 0.0, 4.9, 12.0)
    K.cloth(f, pl, NAVY, top=1.25, bottom=0.8, round_=0.05)
    f.fill(f.m_line((4.0, 1.4), (4.0, 12.0), 0.2, 0.2) * pl, H.tone(NAVY, 0.55), 0.7)
    for y in (2.5, 4.3, 6.1, 7.7):
        H.stud(f, 4.0, y, 0.24, SILVER, spec=0.9)
    # the piped edge of the coat along the opening: steel blue, a light stitch next to it
    for xe, s in ((3.1, -1.0), (4.9, 1.0)):
        H.trim_line(f, [(xe - s * 0.05, 1.3), (xe - s * 0.05, 12.0)], TRIM, width=0.34, spec=0.35, outline=0.6)
        K.seam(f, [(xe + s * -0.42, 1.8), (xe + s * -0.42, 8.4)], PIPE, 0.06, 0.65)
    # the shoulders: the coat yoke is a little darker and a seam runs from the collar to the arm
    for x0, x1 in ((0.0, 1.3), (6.7, 8.0)):
        f.fill(f.m_rect(x0, 1.3, x1, 1.55), K.deep(COAT, 0.62), 0.55)
    # the silver eagle brooch of the Captain, on the left breast of the coat (the right of the picture is the wearer's left)
    f.ellipse(1.4, 3.6, 0.5, 0.5, H.tone(SILVER, 0.5), a=0.85)
    f.ellipse(1.4, 3.6, 0.42, 0.42, H.metal_paint(3.2, 4.0, SILVER, 1.0))
    f.poly([(1.4, 3.3), (1.0, 3.75), (1.4, 3.65), (1.8, 3.75)], "#5568c8", 0.9)
    f.ellipse(1.28, 3.45, 0.1, 0.07, "#ffffff", a=0.9)
    # the high collar: steel blue, bevelled, a V of navy at the throat, a silver clasp
    H.trim_band(f, 0.0, 1.55, TRIM, spec=0.35, outline=0.6)
    f.fill(f.m_poly([(3.2, 0.0), (4.8, 0.0), (4.0, 1.4)]), NAVY, 1.0)
    f.fill(f.m_polyline([(3.2, 0.0), (4.0, 1.42), (4.8, 0.0)], 0.2), H.tone(TRIM, 0.45), 0.9)
    K.seam(f, [(0.2, 1.2), (2.8, 1.2)], PIPE, 0.06, 0.7)
    K.seam(f, [(8.0 - 0.2, 1.2), (5.2, 1.2)], PIPE, 0.06, 0.7)
    H.shadow_under(f, 1.55, 0.55, 0.28)
    # the belt with the silver buckle
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#6c7498")
    K.buckle(f, 4.0, 9.5, 2.3, 1.5, SILVER, hole="#12142a")
    for x in (1.0, 7.0):
        H.stud(f, x, 9.5, 0.17, SILVER, spec=0.7)
    # the coat skirt below the belt and its hem
    f.fill(f.m_band(11.3, 12.0), H.tone(COAT, 0.82), 0.6)
    H.shadow_under(f, 10.1, 0.6, 0.22, x0=0.0, x1=3.1)
    H.shadow_under(f, 10.1, 0.6, 0.22, x0=4.9, x1=8.0)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.08, bottom=0.82, round_=0.10, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.08, fold_y0=2.0, fold_y1=11.0)
    K.sheen(f, m, [1.4, 6.3], 2.2, 8.4, 0.16)
    H.trim_band(f, 0.0, 1.55, TRIM, spec=0.35, outline=0.6)
    K.seam(f, [(0.2, 1.2), (7.8, 1.2)], PIPE, 0.06, 0.7)
    H.shadow_under(f, 1.55, 0.55, 0.26)
    f.fill(f.m_line((4.0, 1.9), (4.0, 12.0), 0.22, 0.22), H.tone(COAT, 0.5), 0.6)                         # the centre seam of the coat back
    K.seam(f, [(3.7, 2.0), (3.7, 8.3)], PIPE, 0.06, 0.45)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#6c7498")
    H.stud(f, 4.0, 9.5, 0.2, SILVER, spec=0.7)
    f.fill(f.m_band(11.3, 12.0), H.tone(COAT, 0.82), 0.6)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), COAT, top=1.08, bottom=0.82, round_=0.10, fold_xs=[1.4, 2.6], fold_depth=0.08, fold_y0=2.0, fold_y1=11.0)
    H.trim_band(f, 0.0, 1.55, TRIM, spec=0.35, outline=0.6)
    K.seam(f, [(0.15, 1.2), (f.w - 0.15, 1.2)], PIPE, 0.06, 0.7)
    H.shadow_under(f, 1.55, 0.55, 0.26)
    ed = f.w - 0.7 if name == "west" else 0.7                                                              # the front edge of the coat: the piped opening turns round the side
    K.seam(f, [(ed, 2.0), (ed, 8.4)], PIPE, 0.06, 0.5)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#6c7498")
    f.fill(f.m_band(11.3, 12.0), H.tone(COAT, 0.82), 0.6)


def body(cv):
    for nm in SIDES:
        base, ov = K.pair(cv, "body", nm)
        for f, isov in ((base, False), (ov, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ the arms and legs
def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.4)
            K.cloth(f, sleeve, COAT, top=1.12, bottom=0.85, round_=0.14, fold_xs=[0.9, 2.1], fold_depth=0.09, fold_y0=1.0, fold_y1=8.0)
            K.sheen(f, sleeve, [f.w * 0.35], 1.8, 7.6, 0.18)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.35), K.lit(COAT, 1.2), 0.55)                                   # the shoulder cap catches the light
            f.fill(f.m_rect(0.0, 1.4, f.w, 1.62), K.deep(COAT, 0.6), 0.55)                                  # the shoulder seam of the coat
            for y, k in ((5.2, 0.55), (5.75, 0.38)):                                                          # the elbow creases
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.16) * sleeve, K.deep(COAT, 0.6), k)
            f.fill(f.m_polyline([(0.0, 4.95), (f.w * 0.5, 5.2), (f.w, 5.0)], 0.2) * sleeve, K.lit(COAT, 1.3), 0.35)
            H.shadow_under(f, 8.4, 0.55, 0.3)
            K.cuff(f, 8.4, 9.7, TRIM, pipe=PIPE, stud_color=SILVER)
            K.glove(f, GLOVE, 9.7, 12.0, nm, finger_n=2)
            H.shadow_under(f, 9.7, 0.5, 0.2)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                skirt = f.m_band(0.0, 6.0)
                K.cloth(f, skirt, COAT, top=1.08, bottom=0.8, round_=0.12, fold_xs=[1.1, 2.9], fold_depth=0.1, fold_y0=0.3, fold_y1=5.7)
                K.sheen(f, skirt, [f.w * 0.3], 0.8, 5.2, 0.14)
                H.trim_band(f, 5.9, 6.85, TRIM, spec=0.3, outline=0.6)
                K.seam(f, [(0.12, 6.4), (f.w - 0.12, 6.4)], PIPE, 0.055, 0.8, (0.3, 0.18), 0.2)
                H.shadow_under(f, 6.85, 0.8, 0.4)
                K.boot(f, BOOT, 6.85, nm, strap=None, cuff_color=SILVER, cuff_h=0.95, welt="#4c5470", sole="#0a0b12")
                if nm == "front":
                    H.stud(f, f.w * 0.5, 8.7, 0.3, SILVER, spec=0.7)
                    f.fill(f.m_rect(0.0, 8.35, f.w, 9.05), H.tone(BELT, 1.1), 0.9)
                    f.fill(f.m_rect(0.0, 8.35, f.w, 8.5), K.lit(BELT, 1.7), 0.7)
                    H.stud(f, f.w * 0.5, 8.7, 0.3, SILVER, spec=0.7)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN, ears=False)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)

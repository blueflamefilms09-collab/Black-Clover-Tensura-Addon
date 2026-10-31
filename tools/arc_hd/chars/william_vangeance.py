"""William Vangeance, Captain of the Golden Dawn, HD 256x256 (docs/arc/hd_arc_bc4.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_william_vangeance.py): a knee-length gold-and-cream captain's coat with a high collar, gold trim and buttons over a cream shirt, a double
belt with a gold buckle, long sleeves with gold cuffs over pale gloves, dark trousers and knee-high brown boots, and the gold eye-band visor that is his mask (a purple light behind the slits).
The gold cape with the World Tree (its lining is the Golden Dawn's blue) and the floating grimoire are the `pmx_william_vangeance` extra (the cape hangs from the shoulders and covers the whole
back and the back of the legs down to the shin, so the back of the coat is kept plain; nothing is painted where the grimoire floats). The blue fur of the Golden Dawn's shoulders is the sheet's
`shoulder_fur` accessory: the 64x64 skin did not paint it (only the cape lining showed it), the HD pass paints it on the shoulders of the arms. Slim (3 px) arms. GLOW: the purple light of the
visor slits (the glow map exists for this look and is redrawn here, the slits only).
Drawn at 4x detail on both layers: the visor (a gold mask with pointed temples, a bridge notch, an engraved line, dark slits that glow violet), the swept white fringe over one eye; the coat with
lit panels, a gold placket with a row of buttons, the high gold collar, a chain clasp at the chest, the double belt with the buckle, the gold hem; the blue fur shoulders with tufted edges, long
sleeves with stitched cuffs and cream gloves, the dark trousers and the knee boots with gold cuffs and straps.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus docs/arc/npcb.md: a lean, tall man
with silver-white hair, a violet gaze behind a gold mask, the Golden Dawn's gold-and-cream coat and blue-furred shoulders. The chain clasp and the engraved line of the visor are added in the HD pass.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc4 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_swept"                                    # long white hair swept over one eye
HAIR = "#f4f5fb"                                              # white with a cool tint: the hair sheet is grey and the tint multiplies, so it must start very light
HAIR_RGB = (244, 245, 251)
SKIN = ("#e4b8a0", "#f4d0b8", "#fce4d0")
COAT = "#ebe2c6"                                              # cream (the sheet's #e4dcc0)
COAT_D = "#c9bc98"
GOLD = "#d4a846"
GOLD_L = "#f0cc6a"
GOLD_D = "#7a5a1a"
SHIRT = "#f6f2e6"
GLOVE = "#ece4cc"
TROUS = "#2e2e3c"
BOOT = "#4a362a"
BELT = "#3e2a22"
FUR, FUR_L, FUR_D = "#6a90c8", "#bcd2f0", "#38558a"           # the Golden Dawn's blue fur
VIOLET = "#b58cff"
SEAM = "#c9a050"
IRIS = ("#3a1a68", "#9a6ae0", "#e8d8ff")


# ================================================================================================ the head
def clear_glow(f):
    f.glow[...] = 0.0


def visor(f, glow_it=True):
    """The gold eye-band: a mask across the eyes with pointed temples and a notch for the nose, a dark rim, an engraved brow line, two slanted dark slits with a violet light behind them."""
    top = 4.1
    outer = [(0.0, 4.45), (1.0, top), (3.3, top + 0.12), (4.0, top + 0.5), (4.7, top + 0.12), (7.0, top), (8.0, 4.45), (8.0, 5.65), (7.1, 6.35), (5.5, 6.5), (4.55, 6.0), (3.45, 6.0), (2.5, 6.5), (0.9, 6.35),
             (0.0, 5.65)]
    m = f.m_poly(outer)
    f.fill(m, H.metal_paint(top, 6.5, GOLD, 1.0))
    H.bevel(f, m, depth=0.32, gain=1.25, spec=0.7)
    f.fill(f.m_polyline(outer + [outer[0]], 0.2) * m, GOLD_D, 0.85)                                       # the dark rim
    f.fill(f.m_polyline([(1.1, top + 0.5), (3.2, top + 0.62), (4.0, top + 0.95), (4.8, top + 0.62), (6.9, top + 0.5)], 0.14) * m, GOLD_D, 0.7)     # the engraved brow line
    for sgn in (1.0, -1.0):
        cx = 4.0 - sgn * 1.75                                                                              # the slit of the left of the picture first
        slit = [(cx + sgn * 1.45, 5.18), (cx + sgn * 0.5, 4.78), (cx - sgn * 0.9, 4.86), (cx - sgn * 1.45, 5.05), (cx - sgn * 0.5, 5.5), (cx + sgn * 0.7, 5.52)]
        sm = f.m_poly(slit)
        f.fill(sm, "#140c20")
        light = [(cx + sgn * 1.2, 5.16), (cx + sgn * 0.45, 4.9), (cx - sgn * 0.8, 4.97), (cx - sgn * 1.2, 5.07), (cx - sgn * 0.45, 5.35), (cx + sgn * 0.6, 5.36)]
        f.fill(f.m_poly(light), H.lin((0, 4.85), (0, 5.4), [(0, "#7a44d8"), (0.5, VIOLET), (1, "#e6d4ff")]), 0.95, glow=VIOLET if glow_it else None)
        f.fill(f.m_line((cx - sgn * 0.7, 5.12), (cx + sgn * 0.8, 5.2), 0.2, 0.16) * sm, "#fbf6ff", 0.95, glow="#f0e6ff" if glow_it else None)
        f.fill(f.m_polyline(slit + [slit[0]], 0.14), "#0a0612", 0.9)
    for x in (0.55, 7.45):                                                                                 # rivets at the temples
        H.stud(f, x, 5.3, 0.17, "#fff0b0", spec=0.8)
    H.stud(f, 4.0, 5.0, 0.2, "#fff0b0", spec=0.8)                                                          # a stud on the bridge


def head_front(cv):
    f = cv.face("head", "front")
    clear_glow(f)
    K.face_base(f, SKIN, side=0.08, chin=0.06)
    pal = K.hair_pal(HAIR, light_k=1.02, dark_k=0.7)
    # cool shade under the mask, a thin calm mouth, the lower face of a pale noble
    f.fill(f.m_ellipse(4.0, 6.7, 3.2, 0.5, soft=2.5), "#c9b0be", 0.2)
    H.nose(f, 4.0, 6.65, "#cfa699", 0.2)
    H.mouth_smile(f, 4.0, 7.2, 1.8, color="#a46d6c", lip="#d99d98", depth=-0.03, thick=0.16)
    visor(f)
    # the hair: a swept white fringe, one long bang over the left of the picture, framing locks
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.2, 1.7), (6.1, 2.5), (5.0, 1.2), (3.8, 1.9), (2.2, 1.7), (0.9, 2.1), (-0.3, 2.3)], y_bot=2.4)
    K.strands(f, pal, (0.8, 2.0, 3.3, 4.6, 5.9, 7.1), 0.0, 1.9, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.85, (4.6, 2.1, 0.8, 3.4, -2.2)),
        (0.9, (3.3, 1.9, 1.0, 2.9, -1.6)),
        (0.95, (2.0, 1.8, 1.0, 2.5, -1.0)),
        (0.9, (6.3, 1.7, 1.1, 1.8, 0.5)),
        (0.95, (7.3, 1.5, 1.0, 2.5, 0.3)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.4, 1.0, 0.8, 4.4, 0.25, 1.0)
    K.lock(f, pal, 7.6, 1.0, 0.8, 4.4, -0.25, 1.0)
    visor_overlay(cv)


def visor_overlay(cv):
    """The sheet put the visor on the second layer of the head (it sits a little off the face): redraw it there, the rest of that island stays as it is."""
    o = cv.face("head", "front", True)
    clear_glow(o)
    o.fill(o.m_poly([(-0.3, 2.7), (8.3, 2.7), (8.3, 6.9), (-0.3, 6.9)]), "#000000", mode="erase")        # the 64x64 frame (rows 3 to 6) is redrawn from nothing
    visor(o)
    # keep only the mask shape on the second layer (the 64x64 sheet had it as a 3-row band): everything else of the island is outside the band and stays untouched


# ================================================================================================ the coat
def collar(f, name):
    m = f.m_band(0.0, 1.7)
    K.cloth(f, m, GOLD, top=1.18, bottom=0.78, round_=0.1)
    H.bevel(f, m, depth=0.3, gain=1.15, spec=0.4)
    f.fill(f.m_rect(0.0, 1.55, f.w, 1.72), H.tone(GOLD, 0.55), 0.85)
    f.fill(f.m_rect(0.0, 1.55, f.w, 1.6), K.lit(GOLD, 1.6), 0.7)
    H.shadow_under(f, 1.72, 0.5, 0.26)
    K.seam(f, [(0.15, 1.1), (f.w - 0.15, 1.1)], "#f6dc90", 0.05, 0.6)
    if name == "front":
        f.fill(f.m_poly([(3.4, 0.0), (4.6, 0.0), (4.0, 1.35)]), H.lin((0, 0), (0, 1.35), [(0, SHIRT), (1, H.tone(SHIRT, 0.75))]))
        f.fill(f.m_polyline([(3.4, 0.0), (4.0, 1.35), (4.6, 0.0)], 0.14), "#6a4a14", 0.7)


def skirt(f, y0, y1=12.0, hem=None):
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.deep(COAT, 0.97)), (1, K.deep(COAT, 0.8))]))
    H.folds(f, m, [f.w * 0.25, f.w * 0.5, f.w * 0.75], depth=0.12, width=0.25, y0=y0, y1=y1)
    K.sheen(f, m, [f.w * 0.3], y0 + 0.2, y1 - 0.2, 0.14, "#fff8e0")
    if hem is not None:
        f.fill(f.m_band(hem, y1), H.lin((0, hem), (0, y1), [(0, K.lit(GOLD, 1.2)), (1, H.tone(GOLD, 0.7))]))
        f.fill(f.m_band(hem - 0.06, hem + 0.06), "#4a3410", 0.7)
        f.fill(f.m_band(hem + 0.04, hem + 0.16), K.lit(GOLD, 1.7), 0.6)


def clasp(f):
    """The fastening of the cape: two gold discs on the chest joined by a chain that sags between them."""
    pts = [(1.4, 2.7), (2.4, 3.15), (4.0, 3.35), (5.6, 3.15), (6.6, 2.7)]
    K.line(f, pts, H.tone(GOLD, 0.5), 0.26, 0.9)
    K.line(f, pts, GOLD_L, 0.14, 0.95)
    for x in (1.2, 6.8):
        f.ellipse(x, 2.65, 0.52, 0.52, H.tone(GOLD, 0.4), a=0.95)
        f.ellipse(x, 2.65, 0.44, 0.44, H.metal_paint(2.2, 3.1, GOLD, 1.0))
        H.gem(f, x, 2.65, 0.2, "#a67cf2", setting=GOLD_L, facets=4, spec=0.9)


def body_front(f, ov):
    K.cloth(f, f.m_all(), COAT, top=1.12, bottom=0.8, round_=0.12, fold_xs=[1.3, 6.7], fold_depth=0.10, fold_y0=2.0, fold_y1=8.8)
    for x0, x1 in ((0.0, 3.0), (5.0, 8.0)):
        K.sheen(f, f.m_rect(x0, 1.7, x1, 8.8), [x0 + 0.8 if x0 < 4 else x1 - 0.8], 2.0, 8.4, 0.2, "#ffffff")
    f.fill(f.m_rect(0.0, 1.7, 0.26, 8.8), K.lit(COAT, 1.2), 0.35)
    f.fill(f.m_rect(7.74, 1.7, 8.0, 8.8), K.deep(COAT, 0.7), 0.4)
    # the placket: cream shirt-coloured strip between two gold edges, a row of buttons
    pl = f.m_rect(3.3, 1.6, 4.7, 8.9)
    K.cloth(f, pl, SHIRT, top=1.05, bottom=0.82, round_=0.04)
    for x in (3.3, 4.7):
        H.trim_line(f, [(x, 1.7), (x, 8.9)], GOLD, width=0.34, spec=0.5, outline=0.6)
        K.seam(f, [(x + (-0.46 if x < 4 else 0.46), 1.9), (x + (-0.46 if x < 4 else 0.46), 8.7)], SEAM, 0.05, 0.5)
    for y in (2.7, 4.3, 5.9, 7.5):
        H.stud(f, 4.0, y, 0.24, GOLD_L, spec=0.9)
    collar(f, "front")
    clasp(f)
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.7), H.tone(BELT, 0.8), 1.0)
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.3), K.lit(BELT, 1.6), 0.6)
    K.buckle(f, 4.0, 9.55, 2.1, 1.55, GOLD, hole="#241a0a")
    for x in (1.0, 7.0):
        H.stud(f, x, 9.5, 0.2, GOLD, spec=0.8)
    skirt(f, 10.7)


def body_back(f, ov):
    K.cloth(f, f.m_all(), COAT, top=1.1, bottom=0.8, round_=0.12, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.09, fold_y0=2.0, fold_y1=9.0)
    collar(f, "back")
    f.fill(f.m_line((4.0, 1.9), (4.0, 10.7), 0.2, 0.2), K.deep(COAT, 0.55), 0.6)
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.7), H.tone(BELT, 0.8), 1.0)
    H.stud(f, 4.0, 9.5, 0.2, GOLD, spec=0.7)
    skirt(f, 10.7)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), COAT, top=1.1, bottom=0.8, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.09, fold_y0=2.0, fold_y1=9.0)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.6)], SEAM, 0.05, 0.5)
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.7), H.tone(BELT, 0.8), 1.0)
    skirt(f, 10.7)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ the arms: blue fur shoulders, cuffs, gloves
def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.4)
            K.cloth(f, sleeve, COAT, top=1.12, bottom=0.82, round_=0.16, fold_xs=[0.8, 1.9], fold_depth=0.10, fold_y0=3.0, fold_y1=8.0)
            K.sheen(f, sleeve, [f.w * 0.33], 2.6, 7.6, 0.18, "#ffffff")
            for y, k in ((5.2, 0.55), (5.75, 0.36)):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.16) * sleeve, K.deep(COAT, 0.6), k)
            K.seam(f, [(0.0, 3.4), (f.w, 3.5)], SEAM, 0.05, 0.4)
            H.shadow_under(f, 8.4, 0.5, 0.3)
            K.cuff(f, 8.4, 9.65, GOLD, pipe="#6a4a14", stud_color="#fff0b0", spec=0.4)
            K.glove(f, GLOVE, 9.65, 12.0, nm, finger_n=2, knuckle_k=1.2)
            H.shadow_under(f, 9.65, 0.5, 0.22)
            # the Golden Dawn's blue fur on the shoulder: a thick tufted band over the top of the sleeve
            H.shadow_under(f, 2.9, 0.7, 0.38)
            K.fur_band(f, 0.0, 2.35, FUR, FUR_L, FUR_D, tuft=0.65, seed=3 if part == "arm_r" else 5)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            f = cv.face(part, "top", ov)
            f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, FUR_L), (0.5, FUR), (1, FUR_D)]))
            for i in range(14):
                x = f.w * H.hsh(7, i, 1)
                y = f.h * H.hsh(7, i, 2)
                f.fill(f.m_line((x, y), (x + 0.15, y + 0.4), 0.2, 0.1), FUR_L if i % 2 else FUR_D, 0.7)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 8.0, nm, knee=False, top=1.3, bottom=0.85, seam_color="#5a5a6a")
                skirt(f, 0.0, 7.4, hem=6.55)
                H.shadow_under(f, 7.4, 0.8, 0.34)
                K.boot(f, BOOT, 7.3, nm, strap=True, strap_color="#8a6a44", gold=GOLD, cuff_color=GOLD, cuff_h=0.85, welt="#8a6a44", sole="#0d0a0a")


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

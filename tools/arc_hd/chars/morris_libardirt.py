"""Morris Libardirt, the blind Magic Scholar of the Spade Kingdom, HD 256x256 (docs/arc/hd_arc_bc6.md). One painter for both forms: `morris_libardirt_form2.py` is the same code with the host palette.

What he wears is the 64x64 sheet's outfit (tools/arc/char_morris_libardirt.py): a calf-long cream scholar's coat with an orange stand collar, orange piping and cuffs, an orange cravat, a dark shirt
in the opening, a brown belt with an orange buckle and a row of notebooks, dark trousers, low black shoes; the orange rune of Modification Magic on the back of the coat. Slim, tall. The orb in the right
hand is the `pmx_morris_libardirt*` extra, and in form 2 so are Lucifero's horns and bat wings (the horns grow from the top of the head at both sides of the centre, the wings from the upper back:
the top of the head and the upper back are kept plain). The round black glasses of the sheet are painted on both layers (the geo extra carries no lens), the temple arms run along the sides of the head.
Form 2 (Lucifero surfaces): no glasses, violet slit eyes that glow, the dark sigil on the forehead and thin veins on the cheeks that glow violet, black sleeves with violet cuffs and black gloves
whose veins glow; the coat itself stays cream.
Glow: the sheet's glow map (form 2: the eyes, the sigil, the veins); form 1 adds only the rune on the back (the sheet's emblem glow) as a faint orange line work.
Drawn at 4x detail on both layers: round tinted lenses with a glint and a black frame (the eyes behind are closed), thin blond brows, a smirk, a sharp nose, a fringe of thick blond clumps with a
dark root edge (the cut is the 3D hair `anime_short`); the coat with lit panels, orange piping on the opening, the collar and the hem, board shoulders, a belt with three notebooks and an orange
buckle, the rune with its ring and three glyph strokes; bare slim hands (form 1), gloves and veins (form 2); trousers, shoes with a lit toe cap.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); from memory of the series plus docs/arc/morris_libardirt.md: a tall slim pale
man, short spiky blond hair, round black tinted glasses over his blind eyes, a long light coat; as the host a dark mark on the forehead, slit eyes, black arms, horns and wings. The orange trim, the rune
and the notebooks are the sheet's own additions for the game.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc6 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_short_open"                               # thick layered blond clumps swept up and aside, the forehead open for the sigil (the pipeline's `anime_spiky` is thin fingers)
HAIR = "#ffe680"
HAIR_RGB = (255, 230, 128)
TINT = (255, 206, 30)                                             # the hair tint of the kit: a more saturated gold (the tint pipeline renders pale yellow as khaki)
SKIN = ("#e8c8c0", "#f6e0d6", "#fff2ea")
COAT, COAT_L, COAT_D = "#dad2c0", "#f4efe2", "#9c927c"
ORANGE, ORANGE_L, ORANGE_D = "#f28e44", "#ffc084", "#a85a22"
SHIRT = "#33303e"
BELT = "#54402e"
TROUS = "#34323c"
SHOE = "#211f25"
LENS = ("#2a2632", "#0e0c14")
FRAME = "#0b0a0f"
VIOLET, VIOLET_L, VIOLET_D = "#8a4ee0", "#c09cff", "#4a2088"
BLACK_SLEEVE = "#1a1722"


# ================================================================================================ the head
def glasses(f, overlay):
    """Round black tinted glasses: two lenses (a dark gradient with a glint), a thick black frame, a bridge. The lens hides the closed eyes."""
    for cx in (2.3, 5.7):
        f.fill(f.m_ellipse(cx, 4.95, 1.62, 1.62), FRAME, 1.0)
        f.fill(f.m_ellipse(cx, 4.95, 1.28, 1.28), H.lin((cx - 1.2, 3.7), (cx + 1.2, 6.2), [(0, LENS[0]), (1, LENS[1])]), 1.0)
        f.fill(f.m_polyline([(cx - 0.85, 4.25), (cx - 0.4, 3.85), (cx + 0.25, 3.72)], 0.2), "#aeb4d8", 0.8)             # the glint on the glass
        f.fill(f.m_ellipse(cx + 0.45, 5.6, 0.28, 0.16), "#6a6486", 0.45)
        f.fill(f.m_ring(cx, 4.95, 1.4, 0.16), "#3a3644", 0.8)                                                      # the lit edge of the frame
    f.fill(f.m_rect(3.55, 4.55, 4.45, 4.95), FRAME, 1.0)
    f.fill(f.m_line((0.0, 4.9), (0.7, 4.85), 0.35, 0.35), FRAME, 1.0)
    f.fill(f.m_line((8.0, 4.9), (7.3, 4.85), 0.35, 0.35), FRAME, 1.0)


def head_front(cv, form2):
    f = cv.face("head", "front")
    fo = cv.face("head", "front", True)
    fo.clear()
    K.face_base(f, SKIN, side=0.08, chin=0.07)
    pal = K.hair_pal(HAIR, light_k=1.2, dark_k=0.6)
    # the closed eyes (form 1) or the host's slit eyes (form 2)
    brow = "#b69a58"
    if not form2:
        H.brow(f, (3.5, 3.3), (1.0, 3.0), brow, 0.26, 0.12, arch=0.08)
        H.brow(f, (4.5, 3.3), (7.0, 3.0), brow, 0.26, 0.12, arch=0.08)
        for cx in (2.3, 5.7):
            f.fill(f.m_polyline([(cx - 1.1, 5.0), (cx, 5.3), (cx + 1.1, 5.0)], 0.2), "#7a6a60", 0.7)                    # a closed lid line under the glass
        K.eye_bag(f, 2.3, 6.45, 2.2, "#a88a96", 0.4)
        K.eye_bag(f, 5.7, 6.45, 2.2, "#a88a96", 0.4)
    else:
        H.brow(f, (3.45, 3.55), (1.0, 3.05), "#8a7a4a", 0.32, 0.14, arch=0.02)
        H.brow(f, (4.55, 3.55), (7.0, 3.05), "#8a7a4a", 0.32, 0.14, arch=0.02)
        iris = (VIOLET_D, VIOLET, VIOLET_L)
        for cx in (2.3, 5.7):
            al = H.eye(f, cx, 5.0, 2.5, 1.7, iris, lash="#1a1024", sclera="#e4dcf4", tilt=0.28, lash_w=0.5, flick=0.44, lid_shadow=0.42, hi=0.5)
            f.fill(f.m_line((cx, 4.2), (cx, 5.85), 0.2, 0.2) * al, "#0c0614", 0.95)                                   # the vertical slit
            K.eye_glow(f, al, cx, 5.0, 2.5, 1.7, VIOLET_L, 0.9)
        K.eye_bag(f, 2.3, 6.2, 2.2, "#7a5a9a", 0.45)
        K.eye_bag(f, 5.7, 6.2, 2.2, "#7a5a9a", 0.45)
    H.nose(f, 4.0, 6.2, "#d0a4a0", 0.24)
    H.mouth_smile(f, 4.2, 7.2, 2.0, color="#a8686c", lip="#d49a9a", depth=0.1 if not form2 else -0.04, thick=0.17)
    f.fill(f.m_line((5.3, 7.2), (5.55, 6.95), 0.14, 0.1), "#b27a7a", 0.6)                                           # the smirk's corner
    if form2:
        sigil(f)
        for p in ([(0.55, 5.5), (1.0, 6.5), (1.7, 7.4)], [(7.45, 5.5), (7.0, 6.5), (6.3, 7.4)], [(3.2, 6.6), (3.0, 7.4)], [(4.8, 6.6), (5.0, 7.4)]):
            K.glow_line(f, p, VIOLET_L, 0.15, 0.85, core="#f0e4ff", core_a=0.5)
    else:
        for g in (f, fo):
            glasses(g, g is fo)
    # the hair: thick blond clumps over the forehead, a dark root edge, two sideburns
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.2), (7.2, 1.6), (6.1, 2.4), (5.0, 1.5), (3.8, 2.3), (2.7, 1.5), (1.5, 2.2), (0.6, 1.7), (-0.3, 2.3)], y_bot=2.4)
    K.strands(f, pal, (0.9, 2.1, 3.4, 4.7, 6.0, 7.1), 0.0, 1.8, mass)
    for sh, (cx, w, top, ln, lean) in ((0.9, (6.6, 1.7, 1.0, 1.9, -0.3)), (0.9, (1.5, 1.6, 1.0, 1.8, 0.3)), (1.0, (4.8, 1.4, 0.9, 1.5, 0.1)), (0.95, (3.0, 1.4, 0.9, 1.4, -0.1))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.45, 1.0, 0.7, 3.8, 0.2, 1.0)
    K.lock(f, pal, 7.55, 1.0, 0.7, 3.8, -0.2, 1.0)


def sigil(f):
    """Lucifero's mark on the forehead: a dark pointed sigil with a violet edge that glows."""
    d = [(4.0, 1.9), (4.62, 2.65), (4.0, 3.7), (3.38, 2.65)]
    f.fill(f.m_poly(d), "#10081c", 0.95)
    f.fill(f.m_polyline(d + [d[0]], 0.13), VIOLET_L, 0.95, glow=VIOLET_L)
    f.fill(f.m_line((3.1, 2.65), (4.9, 2.65), 0.14, 0.14), VIOLET_L, 0.9, glow=VIOLET_L)


def head_sides(cv, form2):
    pal = K.hair_pal(HAIR, light_k=1.2, dark_k=0.6)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.6, 1.0, 3.0, lean, 0.95)
        if not form2:                                                                  # the glasses' temple arm along the side of the head
            for ov in (False, True):
                g = cv.face("head", nm, ov)
                x0, x1 = (3.6, 8.0) if nm == "west" else (0.0, 4.4)
                g.fill(g.m_rect(x0, 4.45, x1, 4.9), FRAME, 1.0)


# ================================================================================================ the coat
def collar(f, name):
    m = f.m_band(0.0, 1.9)
    K.cloth(f, m, ORANGE, top=1.3, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.25, gain=1.15, spec=0.3)
    f.fill(f.m_rect(0.0, 1.78, f.w, 1.9), ORANGE_D, 1.0)
    f.fill(f.m_rect(0.0, 1.0, f.w, 1.08), ORANGE_D, 0.45)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.12), ORANGE_L, 0.5)
    H.shadow_under(f, 1.96, 0.5, 0.3)


def body_front(f, form2):
    K.cloth(f, f.m_all(), COAT, top=1.18, bottom=0.82, round_=0.12, fold_xs=[0.9, 1.9, 6.1, 7.1], fold_depth=0.1, fold_y0=2.0, fold_y1=11.6)
    for x0, x1 in ((0.0, 3.0), (5.0, 8.0)):
        K.sheen(f, f.m_rect(x0, 2.0, x1, 11.8), [x0 + 0.9 if x0 < 4 else x1 - 0.9], 2.2, 11.2, 0.3, "#ffffff", width=0.28)
    f.fill(f.m_rect(7.72, 2.0, 8.0, 12.0), COAT_D, 0.5)
    # the opening: the dark shirt, orange piping on both coat edges, orange buttons
    op = f.m_rect(3.0, 1.9, 5.0, 12.2)
    f.fill(op, H.lin((0, 1.9), (0, 12.0), [(0, K.lit(SHIRT, 1.3)), (1, H.tone(SHIRT, 0.7))]))
    f.fill(f.m_rect(3.0, 1.9, 3.35, 12.0), "#0a0910", 0.5)
    for x in (3.0, 5.0):
        H.trim_line(f, [(x, 1.9), (x, 12.0)], ORANGE, width=0.26, spec=0.45, outline=0.55)
    for y in (3.1, 4.5, 5.9, 7.3):
        H.stud(f, 4.0, y, 0.2, ORANGE_L, spec=0.9)
    # the orange cravat
    f.fill(f.m_poly([(3.1, 0.2), (4.9, 0.2), (4.5, 1.7), (4.0, 3.0), (3.5, 1.7)]), H.lin((0, 0.2), (0, 3.0), [(0, ORANGE_L), (1, ORANGE_D)]))
    H.gem(f, 4.0, 1.7, 0.28, "#ffe0b0", setting=ORANGE_D, facets=4, spec=0.8, glow=None)
    collar(f, "front")
    f.fill(f.m_poly([(2.6, 0.0), (3.4, 0.0), (3.4, 2.05), (3.0, 2.05)]), ORANGE_L, 1.0)
    f.fill(f.m_poly([(5.4, 0.0), (4.6, 0.0), (4.6, 2.05), (5.0, 2.05)]), H.tone(ORANGE, 0.82), 1.0)
    # the belt with three notebooks hanging from it and an orange buckle
    K.belt_band(f, 8.7, 9.7, BELT, stitch=ORANGE_D)
    K.buckle(f, 4.0, 9.2, 1.8, 1.3, ORANGE, hole="#1c130c")
    for bx, col in ((0.9, "#6a4a38"), (6.1, "#4a3a56")):
        f.fill(f.m_rect(bx, 9.75, bx + 1.1, 10.95), "#100c0a", 1.0)
        f.fill(f.m_rect(bx + 0.06, 9.8, bx + 1.04, 10.9), H.lin((bx, 9.8), (bx + 1.1, 10.9), [(0, K.lit(col, 1.4)), (1, H.tone(col, 0.7))]))
        f.fill(f.m_rect(bx + 0.18, 9.8, bx + 0.3, 10.9), ORANGE, 0.9)
        f.fill(f.m_rect(bx + 0.3, 10.3, bx + 0.95, 10.45), ORANGE_L, 0.7)
    K.seam(f, [(0.2, 2.6), (2.8, 2.3)], COAT_D, 0.05, 0.6)
    K.seam(f, [(7.8, 2.6), (5.2, 2.3)], COAT_D, 0.05, 0.6)


def rune(f, cx, cy, r):
    """The orange rune of Modification Magic on the back: a ring with a gap, a cross of three glyph strokes and a dot."""
    f.fill(f.m_ring(cx, cy, r, 0.5), ORANGE_D, 0.9)
    f.fill(f.m_ring(cx, cy, r, 0.34), ORANGE, 1.0, glow=ORANGE)
    f.fill(f.m_ring(cx, cy, r - 0.2, 0.1), ORANGE_L, 0.85)
    f.fill(f.m_rect(cx - 0.35, cy - r - 0.3, cx + 0.35, cy - r + 0.45), COAT, 1.0)
    for p0, p1 in (((cx, cy - r * 0.7), (cx, cy + r * 0.72)), ((cx - r * 0.62, cy - r * 0.2), (cx + r * 0.62, cy - r * 0.2)), ((cx - r * 0.5, cy + r * 0.55), (cx + r * 0.5, cy - r * 0.45))):
        f.fill(f.m_line(p0, p1, 0.34, 0.34), ORANGE_D, 0.95)
        f.fill(f.m_line(p0, p1, 0.2, 0.2), ORANGE_L, 1.0, glow=ORANGE)
    f.fill(f.m_ellipse(cx, cy + r * 0.05, 0.3, 0.3), "#fff0d0", 1.0, glow=ORANGE_L)


def body_back(f, form2):
    K.cloth(f, f.m_all(), COAT, top=1.15, bottom=0.8, round_=0.12, fold_xs=[1.4, 2.6, 5.4, 6.6], fold_depth=0.1, fold_y0=2.0, fold_y1=11.6)
    collar(f, "back")
    K.sheen(f, f.m_rect(0.0, 2.0, 8.0, 11.8), [1.5, 6.5], 2.2, 11.2, 0.22, "#ffffff", width=0.28)
    rune(f, 4.0, 5.3, 2.3)
    f.fill(f.m_line((4.0, 9.8), (4.0, 12.0), 0.2, 0.2), COAT_D, 0.7)
    K.seam(f, [(3.7, 9.8), (3.7, 12.0)], COAT_D, 0.05, 0.5)
    K.seam(f, [(4.3, 9.8), (4.3, 12.0)], COAT_D, 0.05, 0.5)
    K.belt_band(f, 8.7, 9.7, BELT, stitch=ORANGE_D)
    H.stud(f, 4.0, 9.2, 0.2, ORANGE, spec=0.7)


def body_side(f, name):
    K.cloth(f, f.m_all(), COAT, top=1.15, bottom=0.8, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.1, fold_y0=2.0, fold_y1=11.6)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.1), (ed, 8.4)], COAT_D, 0.05, 0.55)
    K.belt_band(f, 8.7, 9.7, BELT, stitch=ORANGE_D)


def body(cv, form2):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, form2)
            elif nm == "back":
                body_back(f, form2)
            else:
                body_side(f, nm)


# ================================================================================================ arms and legs
def arm(cv, part, form2):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.4)
            col = BLACK_SLEEVE if form2 else COAT
            K.cloth(f, sleeve, col, top=1.7 if form2 else 1.18, bottom=0.8 if form2 else 0.84, round_=0.16, fold_xs=[0.7, 1.8], fold_depth=0.1, fold_y0=1.8, fold_y1=7.8)
            K.sheen(f, sleeve, [f.w * 0.3], 1.8, 7.6, 0.28 if form2 else 0.3, "#b8a0f0" if form2 else "#ffffff", width=0.26)
            sb = f.m_band(0.0, 1.45)                                                                       # the board shoulder with an orange edge
            f.fill(sb, H.lin((0, 0), (0, 1.45), [(0, K.lit(col, 1.2)), (1, col)]))
            H.bevel(f, sb, depth=0.2, gain=1.15, spec=0.3)
            f.fill(f.m_rect(0.0, 1.38, f.w, 1.5), ORANGE if not form2 else VIOLET, 1.0)
            f.fill(f.m_rect(0.0, 1.38, f.w, 1.44), ORANGE_L if not form2 else VIOLET_L, 0.7)
            f.fill(f.m_rect(0.0, 1.5, f.w, 1.6), "#0c0a12", 0.4)
            for y, k in ((4.8, 0.5), (5.35, 0.3)):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.28), (f.w, y + 0.05)], 0.15) * sleeve, COAT_D if not form2 else "#06050a", k)
            if nm in ("west", "east"):
                f.fill(f.m_line((f.w * 0.5, 1.7), (f.w * 0.5, 7.2), 0.2, 0.2) * sleeve, COAT_D if not form2 else "#2a2036", 0.6)
            K.cuff(f, 7.25, 8.5, ORANGE if not form2 else "#5b2a8c", pipe=ORANGE_L if not form2 else VIOLET_L, stud_color=ORANGE_L if not form2 else VIOLET_L, spec=0.4)
            if form2:                                                                                      # black gloves, glowing veins
                K.glove(f, "#17131f", 8.5, 12.0, nm, finger_n=3, cuff_color=None, knuckle_k=1.8)
                for p in ([(f.w * 0.3, 2.4), (f.w * 0.5, 4.2), (f.w * 0.4, 6.4)], [(f.w * 0.55, 9.2), (f.w * 0.5, 10.4)]):
                    if nm in ("front", "west", "east"):
                        K.glow_line(f, p, VIOLET, 0.14, 0.75, core="#e4d4ff", core_a=0.45)
            else:
                hand = f.m_band(8.5, 12.0)
                f.fill(hand, H.lin((0, 8.5), (0, 12.0), [(0, H.tone(SKIN[1], 0.92)), (0.5, SKIN[1]), (1, H.tone(SKIN[0], 0.95))]))
                side = np.minimum(f.X, f.w - f.X)
                f.shade(hand, 1.0 - 0.1 * (1.0 - H.smoothstep(0.0, 0.8, side)), 0.9)
                H.shadow_under(f, 8.55, 0.7, 0.3, mask=hand)
                if nm in ("front", "back"):
                    for i in (1, 2):
                        x = f.w * i / 3
                        f.fill(f.m_line((x, 10.9), (x, 11.95), 0.2, 0.16), H.tone(SKIN[0], 0.72), 0.65)
                f.fill(f.m_band(10.75, 10.9), H.tone(SKIN[0], 0.74), 0.5)


def arm_tops(cv, form2):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", BLACK_SLEEVE if form2 else COAT_L, edge=ORANGE_D if not form2 else VIOLET_D, k_top=1.2, overlay=ov)


def shoe(f, nm, y0=10.2):
    m = f.m_band(y0, 12.0)
    f.fill(m, H.lin((0, y0), (0, 12.0), [(0, K.lit(SHOE, 1.7)), (0.6, SHOE), (1, "#0a090c")]))
    if nm == "front":
        f.ellipse(f.w * 0.5, y0 + 0.95, 1.3, 0.55, K.lit(SHOE, 2.4), a=0.45, soft=2.0)
    f.fill(f.m_band(y0, y0 + 0.14), "#6a6672", 0.55)
    f.fill(f.m_band(11.55, 12.0), "#d8d2c4", 0.55)
    f.fill(f.m_band(11.7, 12.0), "#2a2630", 0.9)
    H.shadow_under(f, y0, 0.5, 0.3)


def legs(cv, form2):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 10.6, nm, knee=True, top=1.5, bottom=0.8, seam_color="#6a6678")
                sk = f.m_band(0.0, 6.3)
                f.fill(sk, H.lin((0, 0), (0, 6.3), [(0, K.lit(COAT, 1.05)), (1, H.tone(COAT, 0.8))]))
                H.folds(f, sk, [f.w * 0.25, f.w * 0.5, f.w * 0.75], depth=0.1, width=0.25, y0=0.0, y1=6.0)
                K.sheen(f, sk, [f.w * 0.3], 0.3, 5.8, 0.28, "#ffffff")
                f.fill(f.m_rect(0.0, 5.65, f.w, 6.3), H.lin((0, 5.65), (0, 6.3), [(0, ORANGE_L), (1, ORANGE_D)]))
                f.fill(f.m_rect(0.0, 5.6, f.w, 5.7), ORANGE_D, 0.6)
                H.shadow_under(f, 6.3, 0.8, 0.35)
                shoe(f, nm)


# ================================================================================================ paint
def paint(spec, cid, masks):
    form2 = K.is_form2(cid)
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN)
    head_sides(cv, form2)
    head_front(cv, form2)
    body(cv, form2)
    arm(cv, "arm_r", form2)
    arm(cv, "arm_l", form2)
    arm_tops(cv, form2)
    legs(cv, form2)
    return K.finish(cv, covers, hair, TINT, slim)

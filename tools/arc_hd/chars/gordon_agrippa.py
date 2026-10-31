"""Gordon Agrippa, the poison and curse mage of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc2.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_gordon_agrippa.py): a dark violet-grey knee-length coat with a high lighter stand collar and a row of bone buttons down the
front, a dark belt with a muted poison-green buckle, long sleeves over pale bare hands, a coat skirt over dark trousers and dark calf boots. The hooded Black Bull cloak (the bull on it
glows poison green) is the `pmx_gordon_agrippa` extra: it hangs from the shoulders at the back, so the back of the coat is kept plain. WIDE (4 px) arms. No glow on the skin.
Drawn at 4x detail on both layers: the tired half-lidded eyes with their dark bags, the missing brows, the big nose, the flat frown, the long black side-swept fringe, the coat panels with
piping and stitching, the high collar, the bone buttons, the belt and buckle, the stitched grey-violet cuffs, the pale long-fingered hands with their veins, the coat hem and the boots.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus the repo sheet and the spec
(docs/arc/npcb.md): tall and thin, deathly pale, a long face with a big nose and dark rings under tired eyes, no eyebrows, long black hair falling to one side, a downcast mumble.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc2 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_swept"                                    # long black hair that falls to one side over an eye (the spec's long hair falling over one eye)
HAIR = "#1b1b26"                                              # near black, but a little blue so the tint on the grey hair sheet still shows the folds
HAIR_RGB = (27, 27, 38)
SKIN = ("#d7bab6", "#ead3cc", "#f6e8e2")                      # deathly pale, a cold grey-pink
COAT = "#3a3950"                                              # the sheet's #2c2b38, a touch lighter and bluer so the cloth folds read
COLLAR = "#6e6a8c"                                            # the high stand collar, lighter grey-violet
TRIM = "#7a7698"                                              # cuffs and hem
PIPE = "#9a96b4"                                              # light stitching on the trim
PIPE_G = "#b6ca96"                                            # the collar piping, a pale poison green (the colour of the cloak bull's glow)
BONE = "#ddd5bc"                                              # buttons
BELT = "#2c2430"
GREEN = "#98aa78"                                             # the buckle's muted poison green
TROUS = "#1f1e29"
BOOT = "#18181f"
IRIS = ("#26262f", "#4c4a66", "#8e8cb0")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.11, chin=0.09)
    pal = K.hair_pal(HAIR, light_k=1.9, dark_k=0.5)
    # a long gaunt face: hollow cheeks and cold shadows at the temples
    for x in (1.1, 6.9):
        f.fill(f.m_ellipse(x, 6.6, 0.9, 1.1, soft=2.4), "#a98b98", 0.2)
    # the tired eyes: half-lidded, the outer corner drooping, a dull violet-grey iris; the dark bags under them
    for cx, s in ((2.3, 1.0), (5.7, -1.0)):
        H.eye(f, cx, 4.95, 2.3, 1.45, IRIS, lash="#17161f", tilt=-0.12, lash_w=0.42, flick=0.1, lid_shadow=0.8, hi=0.5, iris_r=0.9)
        f.fill(f.m_ellipse(cx, 5.95, 1.15, 0.26, soft=1.8), "#8d7490", 0.5)               # the bag
        f.fill(f.m_polyline([(cx - 1.1, 5.72), (cx, 6.02), (cx + 1.1, 5.72)], 0.12), "#6f5a74", 0.5)       # the crease under the eye
        f.fill(f.m_line((cx - 0.9 * s, 3.95), (cx + 0.9 * s, 3.85), 0.16, 0.1), "#a98fa0", 0.25)       # a trace of the lid, there are no brows
    # the big nose: a long lit bridge, a shaded side, wide nostrils
    f.fill(f.m_line((4.0, 4.9), (4.0, 6.2), 0.4, 0.3), "#fbeee8", 0.35)
    H.nose(f, 4.0, 6.3, "#b48b8a", 0.34)
    f.fill(f.m_ellipse(3.45, 6.55, 0.26, 0.14), "#8a5f66", 0.7)
    f.fill(f.m_ellipse(4.55, 6.55, 0.26, 0.14), "#8a5f66", 0.7)
    f.fill(f.m_line((4.45, 5.3), (4.58, 6.3), 0.26, 0.2), "#c6a2a0", 0.4)
    # the flat frown, a small crease of worry on each side
    H.mouth_smile(f, 4.0, 7.3, 2.0, color="#5a3a40", lip="#b07c7c", depth=-0.3, thick=0.2)
    for x, s in ((2.9, -1.0), (5.1, 1.0)):
        f.fill(f.m_polyline([(x, 6.85), (x + 0.1 * s, 7.3), (x + 0.3 * s, 7.65)], 0.14), "#a78392", 0.45)
    # the hair of the fringe: long black locks that sweep from a side parting over the left eye of the picture
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.6), (7.3, 2.0), (6.2, 2.9), (5.0, 1.4), (3.9, 2.3), (2.4, 2.0), (1.0, 2.5), (-0.3, 2.8)], y_bot=2.7)
    K.strands(f, pal, (0.8, 2.0, 3.3, 4.6, 5.9, 7.1), 0.0, 2.1, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.85, (4.4, 2.2, 0.8, 4.9, -3.0)),                 # the long bang from the parting across the left eye
        (0.9, (3.1, 2.0, 1.0, 4.2, -2.2)),
        (0.95, (1.9, 1.9, 1.0, 3.5, -1.2)),
        (0.9, (6.2, 1.8, 1.1, 2.3, 0.6)),                   # the shorter locks of the other side
        (0.95, (7.3, 1.6, 1.0, 3.3, 0.3)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.4, 1.0, 0.8, 5.2, 0.25, 1.0)           # the long side locks frame the thin face
    K.lock(f, pal, 7.6, 1.0, 0.8, 5.4, -0.25, 1.0)


# ================================================================================================ the coat
def coat_panel(f, x0, x1, side):
    """One front panel of the coat: dark violet-grey cloth, a lit outer edge, soft folds."""
    m = f.m_rect(x0, 0.0, x1, 12.0)
    K.cloth(f, m, COAT, top=1.18, bottom=0.8, round_=0.10, fold_xs=[x0 + (x1 - x0) * 0.45], fold_depth=0.1, fold_y0=1.8, fold_y1=11.0)
    K.sheen(f, m, [x0 + (x1 - x0) * 0.3], 2.0, 8.4, 0.2, "#b8b4d8", width=0.24)
    outer = x0 if side < 0 else x1
    f.fill(m * f.m_rect(min(outer, outer + side * -0.3), 0, max(outer, outer + side * -0.3), 12), K.lit(COAT, 1.3), 0.4)


def body_front(f, ov):
    coat_panel(f, 0.0, 4.0, -1)
    coat_panel(f, 4.0, 8.0, 1)
    # the closed front: a lapped placket with a piped edge, the row of bone buttons
    pl = f.m_rect(3.55, 1.4, 4.45, 12.0)
    f.fill(pl, H.lin((0, 1.4), (0, 12.0), [(0, K.lit(COAT, 1.25)), (1, K.lit(COAT, 1.05))]))
    for xe in (3.55, 4.45):
        f.fill(f.m_line((xe, 1.4), (xe, 12.0), 0.18, 0.18), K.deep(COAT, 0.45), 0.7)
    f.fill(f.m_line((4.0, 1.4), (4.0, 12.0), 0.14, 0.14), K.deep(COAT, 0.5), 0.45)
    for y in (2.6, 4.4, 6.2, 7.8):
        f.ellipse(4.0, y, 0.34, 0.34, K.deep(BONE, 0.4), a=0.85)
        f.ellipse(4.0, y, 0.29, 0.29, H.metal_paint(y - 0.3, y + 0.3, BONE, 1.0))
        f.ellipse(3.9, y - 0.1, 0.07, 0.06, "#ffffff", a=0.85)
        f.line((3.88, y), (4.12, y), K.deep(BONE, 0.45), 0.07, 0.04)
    # two stitched lines run beside the front to the hem
    for xe in (3.0, 5.0):
        K.seam(f, [(xe, 2.0), (xe, 8.4)], PIPE, 0.055, 0.38)
    # the shoulder yokes of the coat: a darker seam from the collar to the arm
    for x0, x1 in ((0.0, 1.5), (6.5, 8.0)):
        f.fill(f.m_rect(x0, 1.7, x1, 1.92), K.deep(COAT, 0.55), 0.55)
    # the high stand collar: lighter grey-violet, bevelled, a dark V at the throat, light piping
    H.trim_band(f, 0.0, 1.7, COLLAR, spec=0.3, outline=0.6)
    f.fill(f.m_poly([(3.2, 0.0), (4.8, 0.0), (4.0, 1.5)]), K.deep(COAT, 0.7), 1.0)
    f.fill(f.m_polyline([(3.2, 0.0), (4.0, 1.52), (4.8, 0.0)], 0.2), K.deep(COLLAR, 0.5), 0.9)
    K.seam(f, [(0.2, 1.35), (2.9, 1.35)], PIPE_G, 0.06, 0.7)
    K.seam(f, [(8.0 - 0.2, 1.35), (5.1, 1.35)], PIPE_G, 0.06, 0.7)
    H.shadow_under(f, 1.7, 0.55, 0.3)
    # breast pocket slit on the lit side
    f.fill(f.m_line((5.6, 3.2), (7.0, 3.0), 0.2, 0.2), K.deep(COAT, 0.45), 0.7)
    f.fill(f.m_line((5.6, 3.34), (7.0, 3.14), 0.14, 0.14), K.lit(COAT, 1.8), 0.4)
    # the belt, a muted poison-green buckle
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#6c5a78")
    K.buckle(f, 4.0, 9.5, 2.1, 1.5, GREEN, hole="#141a10")
    for x in (1.2, 6.8):
        H.stud(f, x, 9.5, 0.17, GREEN, spec=0.5)
    # the coat skirt below the belt
    f.fill(f.m_band(11.2, 12.0), K.deep(COAT, 0.65), 0.55)
    H.shadow_under(f, 10.1, 0.6, 0.24, x0=0.0, x1=3.55)
    H.shadow_under(f, 10.1, 0.6, 0.24, x0=4.45, x1=8.0)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.14, bottom=0.8, round_=0.10, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.09, fold_y0=2.0, fold_y1=11.0)
    K.sheen(f, m, [1.4, 6.3], 2.2, 8.4, 0.16, "#b8b4d8", width=0.24)
    H.trim_band(f, 0.0, 1.7, COLLAR, spec=0.3, outline=0.6)
    K.seam(f, [(0.2, 1.35), (7.8, 1.35)], PIPE_G, 0.06, 0.7)
    H.shadow_under(f, 1.7, 0.55, 0.28)
    f.fill(f.m_line((4.0, 1.9), (4.0, 12.0), 0.22, 0.22), K.deep(COAT, 0.45), 0.6)                         # the centre seam of the coat back
    K.seam(f, [(3.7, 2.0), (3.7, 8.3)], PIPE, 0.055, 0.4)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#6c5a78")
    H.stud(f, 4.0, 9.5, 0.2, GREEN, spec=0.5)
    f.fill(f.m_band(11.2, 12.0), K.deep(COAT, 0.65), 0.55)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), COAT, top=1.14, bottom=0.8, round_=0.12, fold_xs=[1.4, 2.6], fold_depth=0.09, fold_y0=2.0, fold_y1=11.0)
    H.trim_band(f, 0.0, 1.7, COLLAR, spec=0.3, outline=0.6)
    K.seam(f, [(0.15, 1.35), (f.w - 0.15, 1.35)], PIPE_G, 0.06, 0.7)
    H.shadow_under(f, 1.7, 0.55, 0.28)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.4)], PIPE, 0.055, 0.45)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#6c5a78")
    f.fill(f.m_band(11.2, 12.0), K.deep(COAT, 0.65), 0.55)


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
    """A WIDE (4 px) arm: a long coat sleeve with the shoulder seam, elbow creases, a bulky grey-violet cuff with bone buttons, then the pale long-fingered hand."""
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.6)
            K.cloth(f, sleeve, COAT, top=1.16, bottom=0.82, round_=0.14, fold_xs=[1.0, 2.6], fold_depth=0.1, fold_y0=1.0, fold_y1=8.2)
            K.sheen(f, sleeve, [f.w * 0.3], 1.8, 7.8, 0.18, "#b8b4d8", width=0.24)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.35), K.lit(COAT, 1.25), 0.55)                                   # the shoulder cap catches the light
            f.fill(f.m_rect(0.0, 1.4, f.w, 1.62), K.deep(COAT, 0.55), 0.55)                                  # the shoulder seam of the coat
            for y, k in ((5.3, 0.55), (5.85, 0.4)):                                                          # the elbow creases
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.16) * sleeve, K.deep(COAT, 0.5), k)
            f.fill(f.m_polyline([(0.0, 5.0), (f.w * 0.5, 5.25), (f.w, 5.05)], 0.2) * sleeve, K.lit(COAT, 1.4), 0.32)
            H.shadow_under(f, 8.6, 0.55, 0.3)
            K.cuff(f, 8.6, 10.15, TRIM, pipe=PIPE, stud_color=BONE)                                          # the bulky turned cuff
            f.fill(f.m_band(8.6, 8.78), K.lit(TRIM, 1.5), 0.5)
            K.limb_skin(f, SKIN, 10.15, 12.0, tendon=True)
            K.fingers(f, SKIN, 10.95, 11.95, 3)
            if nm in ("front", "back"):
                for x in (f.w * 0.3, f.w * 0.62):                                                           # the thin bluish veins on the back of the hand
                    f.fill(f.m_polyline([(x, 10.3), (x + 0.1, 10.7), (x - 0.05, 11.0)], 0.12), "#a99ab8", 0.45)
            H.shadow_under(f, 10.15, 0.5, 0.22)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                skirt = f.m_band(0.0, 6.2)
                K.cloth(f, skirt, COAT, top=1.14, bottom=0.8, round_=0.12, fold_xs=[1.1, 2.9], fold_depth=0.11, fold_y0=0.3, fold_y1=5.8)
                K.sheen(f, skirt, [f.w * 0.3], 0.8, 5.3, 0.14, "#b8b4d8", width=0.24)
                if nm == "front":                                                                            # the pocket slit and a button
                    f.fill(f.m_line((f.w * 0.62, 1.2), (f.w * 0.8, 3.0), 0.2, 0.2), K.deep(COAT, 0.45), 0.7)
                    f.fill(f.m_line((f.w * 0.62 + 0.14, 1.2), (f.w * 0.8 + 0.14, 3.0), 0.14, 0.14), K.lit(COAT, 1.8), 0.35)
                H.trim_band(f, 5.55, 6.3, TRIM, spec=0.28, outline=0.6)                                      # the hem of the coat
                K.seam(f, [(0.12, 5.93), (f.w - 0.12, 5.93)], PIPE, 0.055, 0.75, (0.3, 0.18), 0.2)
                H.shadow_under(f, 6.3, 0.8, 0.42)
                tr = f.m_band(6.3, 7.0)                                                                      # a little of the dark trousers
                K.cloth(f, tr, TROUS, top=1.3, bottom=0.7, round_=0.1)
                H.shadow_under(f, 7.0, 0.5, 0.3)
                K.boot(f, BOOT, 7.0, nm, strap=None, cuff_color=COAT, cuff_h=0.9, welt="#4a4a5c", sole="#08080c")
                if nm == "front":                                                                            # two thin buckled straps on the shin
                    for y in (8.5, 9.35):
                        f.fill(f.m_rect(0.0, y, f.w, y + 0.4), "#26252f", 0.95)
                        f.fill(f.m_rect(0.0, y, f.w, y + 0.12), K.lit("#26252f", 2.0), 0.6)
                        H.stud(f, f.w * 0.5, y + 0.2, 0.17, GREEN, spec=0.6)


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

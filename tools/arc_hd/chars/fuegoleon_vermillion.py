"""Fuegoleon Vermillion, Captain of the Crimson Lion Kings, HD 256x256 (docs/arc/hd_arc_bc4.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_fuegoleon_vermillion.py): a high-collared dark-red uniform with a thigh-long skirt and gold trim, a cream shirt at the throat, a
double belt with a gold buckle, long sleeves with gold cuffs over black gloves, black trousers and knee-high dark boots. The crimson mantle with the gold flame and the floating grimoire are
the `pmx_fuegoleon_vermillion` extra (the mantle hangs from the shoulders and covers the whole back and the back of the legs down to the knee, so the back of the uniform is kept plain; the
grimoire floats at the left of the body, nothing is painted where they attach). No glow on the skin (the flame glow is the grimoire's).
Drawn at 4x detail on both layers: the calm sharp amber eyes with strong lashes, the thick straight brows, the swept-back fringe with two long locks that frame the face, the closed mouth; the
uniform with lit panels, a dark button placket flanked by gold piping, the high collar with its gold edge and the cream cravat, a gold lion-flame crest on the left breast, the double belt with
the gold buckle, the gold hem; shoulder boards on the arms, stitched gold cuffs, black gloves with finger gaps, the creased trousers and the boots with a gold cuff and a strap.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus docs/arc/npcb.md: a tall, upright,
calm captain with swept-back deep-red hair and two framing locks, an amber gaze, the crimson captain's mantle. The crest and the shoulder boards are added in the HD pass.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc4 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_swept"                                    # red hair swept back with a fringe (the 3D hair is thick layered clumps)
HAIR = "#c2321c"                                              # a deep red; the hair sheet is grey and the tint multiplies
HAIR_RGB = (194, 50, 28)
SKIN = ("#deaa8a", "#f0c6a4", "#fadcbc")
SKIN_D = "#b8846c"
COAT = "#7d1c1f"                                              # the dark red uniform (the sheet's #4a1a18, lifted so it reads as a colour block)
COAT_D = "#521214"
COAT_L = "#a52a2a"
GOLD = "#e6b44a"
CREAM = "#efe9dc"
GLOVE = "#2a2a34"
TROUS = "#262630"
BOOT = "#4a3430"
BELT = "#3e2a22"
IRIS = ("#7a3408", "#e2902a", "#ffd982")
SEAM = "#d9b45a"


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.10, chin=0.08)
    pal = K.hair_pal(HAIR, light_k=1.2, dark_k=0.52)
    f.fill(f.m_ellipse(2.35, 5.85, 1.5, 0.5, soft=2.5), "#c98f7a", 0.22)
    f.fill(f.m_ellipse(5.65, 5.85, 1.5, 0.5, soft=2.5), "#c98f7a", 0.22)
    # thick straight brows, set low: a stern, calm look
    brow = H.mix(HAIR, "#2a0c08", 0.55)
    H.brow(f, (3.55, 4.4), (1.0, 3.85), brow, 0.44, 0.2, arch=0.04)
    H.brow(f, (4.45, 4.4), (7.0, 3.85), brow, 0.44, 0.2, arch=0.04)
    # sharp amber eyes: wide, a little narrowed, long lashes
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.35, 2.7, 1.9, IRIS, lash="#2a100a", tilt=0.22, lash_w=0.5, flick=0.42, lid_shadow=0.42, hi=0.9)
    H.nose(f, 4.0, 6.5, "#c58f78", 0.22)
    H.mouth_smile(f, 4.0, 7.2, 2.0, color="#9a5650", lip="#d58f84", depth=-0.04, thick=0.18)
    # the fringe: swept back from the left of the picture, a long lock over the brow's inner end, two long locks that frame the face
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.0), (7.3, 1.5), (6.4, 2.1), (5.4, 1.3), (4.4, 1.9), (3.2, 1.4), (2.0, 2.0), (0.9, 1.6), (-0.3, 2.1)], y_bot=2.2)
    K.strands(f, pal, (0.8, 2.0, 3.3, 4.6, 5.9, 7.1), 0.0, 1.8, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.85, (3.4, 1.7, 0.9, 2.6, 0.8)),                  # the long fringe lock over the inner brow
        (0.95, (5.6, 1.5, 1.0, 1.7, -0.6)),
        (0.9, (1.7, 1.5, 1.0, 1.6, 0.5)),
        (0.9, (6.9, 1.4, 1.0, 1.4, -0.4)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.45, 1.1, 0.7, 5.2, 0.35, 1.0)          # the two long locks that frame the face
    K.lock(f, pal, 7.55, 1.1, 0.7, 5.2, -0.35, 1.0)


# ================================================================================================ the uniform
def crest(f, cx, cy, s=1.0):
    """The Crimson Lion's badge on the left breast: a round gold plate with a dark-red flame in it."""
    f.ellipse(cx, cy, 0.62 * s, 0.62 * s, H.tone(GOLD, 0.4), a=0.9)
    f.ellipse(cx, cy, 0.54 * s, 0.54 * s, H.metal_paint(cy - 0.54 * s, cy + 0.54 * s, GOLD, 1.0))
    H.bevel(f, f.m_ellipse(cx, cy, 0.54 * s, 0.54 * s), depth=0.16, gain=1.2, spec=0.6)
    flame = [(cx, cy - 0.42 * s), (cx + 0.2 * s, cy - 0.1 * s), (cx + 0.32 * s, cy + 0.12 * s), (cx + 0.15 * s, cy + 0.4 * s), (cx - 0.15 * s, cy + 0.4 * s), (cx - 0.32 * s, cy + 0.12 * s),
             (cx - 0.12 * s, cy - 0.05 * s)]
    f.poly(flame, COAT_D, 0.95)
    f.ellipse(cx - 0.22 * s, cy - 0.3 * s, 0.12 * s, 0.07 * s, "#fff3c8", a=0.8)


def yoke(f, front=False):
    """The shoulder yoke: a darker band across the upper chest with a gold line under it, so the collar, the yoke and the lit panels are three blocks of colour."""
    m = f.m_band(1.6, 2.65)
    K.cloth(f, m, COAT_D, top=1.35, bottom=0.8, round_=0.08)
    H.trim_line(f, [(0.0, 2.6), (f.w, 2.6)], GOLD, width=0.26, spec=0.45, outline=0.55)
    H.shadow_under(f, 2.72, 0.5, 0.26)
    K.seam(f, [(0.15, 2.05), (f.w - 0.15, 2.05)], SEAM, 0.05, 0.5)


def collar(f, name):
    """The high stand collar: dark red, bevelled, a gold edge along its lower border; the cream cravat in a V at the throat of the front."""
    m = f.m_band(0.0, 1.6)
    K.cloth(f, m, COAT_D, top=1.35, bottom=0.85, round_=0.1)
    f.fill(f.m_rect(0.0, 1.46, f.w, 1.62), GOLD, 1.0)
    f.fill(f.m_rect(0.0, 1.46, f.w, 1.52), K.lit(GOLD, 1.5), 0.9)
    f.fill(f.m_rect(0.0, 1.6, f.w, 1.7), "#2a0a0a", 0.6)
    H.shadow_under(f, 1.62, 0.5, 0.26)
    K.seam(f, [(0.15, 1.12), (f.w - 0.15, 1.12)], SEAM, 0.05, 0.55)
    if name == "front":
        f.fill(f.m_poly([(3.05, 0.0), (4.95, 0.0), (4.0, 1.85)]), H.lin((0, 0), (0, 1.85), [(0, H.tone(CREAM, 1.0)), (1, H.tone(CREAM, 0.74))]))
        f.fill(f.m_polyline([(3.05, 0.0), (4.0, 1.85), (4.95, 0.0)], 0.16), "#6a3a2a", 0.7)
        f.fill(f.m_line((4.0, 0.3), (4.0, 1.6), 0.14, 0.12), H.tone(CREAM, 0.6), 0.5)


def body_front(f, ov):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.3, bottom=0.78, round_=0.13, fold_xs=[1.3, 6.7], fold_depth=0.10, fold_y0=2.0, fold_y1=9.0)
    for x0, x1 in ((0.0, 3.0), (5.0, 8.0)):                                                      # the two lit chest panels
        K.sheen(f, f.m_rect(x0, 1.7, x1, 8.8), [x0 + 0.7 if x0 < 4 else x1 - 0.7], 2.0, 8.4, 0.2, "#e7a090")
    f.fill(f.m_rect(0.0, 1.7, 0.28, 8.8), K.lit(COAT, 1.5), 0.35)
    f.fill(f.m_rect(7.72, 1.7, 8.0, 8.8), K.deep(COAT, 0.6), 0.4)
    # the placket: darker, flanked by two gold pipings, four gold buttons
    pl = f.m_rect(3.0, 1.6, 5.0, 8.9)
    K.cloth(f, pl, COAT_D, top=1.12, bottom=0.82, round_=0.05)
    for x in (3.0, 5.0):
        H.trim_line(f, [(x, 1.7), (x, 8.9)], GOLD, width=0.36, spec=0.5, outline=0.6)
        K.seam(f, [(x + (0.45 if x < 4 else -0.45), 1.9), (x + (0.45 if x < 4 else -0.45), 8.7)], SEAM, 0.05, 0.5)
    for y in (2.7, 4.5, 6.3, 8.0):
        H.stud(f, 4.0, y, 0.27, GOLD, spec=0.9)
    yoke(f, True)
    crest(f, 6.4, 3.6)
    collar(f, "front")
    # the shoulders: a seam where the sleeve joins
    for x0, x1 in ((0.0, 1.0), (7.0, 8.0)):
        f.fill(f.m_rect(x0, 1.7, x1, 1.9), K.deep(COAT, 0.55), 0.55)
    # the double belt with the gold buckle
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.75), H.tone(BELT, 0.8), 1.0)
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.3), K.lit(BELT, 1.6), 0.6)
    K.buckle(f, 4.0, 9.55, 2.2, 1.55, GOLD, hole="#1c0c0a")
    for x in (1.0, 7.0):
        H.stud(f, x, 9.5, 0.2, GOLD, spec=0.8)
    skirt(f, 10.75)


def skirt(f, y0, y1=12.0, hem=None):
    """The coat skirt (the uniform's cloth in soft vertical pleats) from y0 to y1; `hem` = the top of a gold hem band that ends at y1."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(COAT, 0.86)), (1, H.tone(COAT, 0.74))]))
    H.folds(f, m, [f.w * 0.25, f.w * 0.5, f.w * 0.75], depth=0.12, width=0.25, y0=y0, y1=y1)
    if hem is not None:
        f.fill(f.m_band(hem, y1), H.lin((0, hem), (0, y1), [(0, K.lit(GOLD, 1.15)), (1, H.tone(GOLD, 0.7))]))
        f.fill(f.m_band(hem - 0.06, hem + 0.06), "#2a0a0a", 0.7)
        f.fill(f.m_band(hem + 0.04, hem + 0.16), K.lit(GOLD, 1.7), 0.6)


def body_back(f, ov):
    K.cloth(f, f.m_all(), COAT, top=1.22, bottom=0.78, round_=0.13, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.09, fold_y0=2.0, fold_y1=9.0)
    yoke(f)
    collar(f, "back")
    f.fill(f.m_line((4.0, 1.9), (4.0, 10.7), 0.2, 0.2), K.deep(COAT, 0.45), 0.6)                    # the centre seam
    K.seam(f, [(3.62, 2.0), (3.62, 8.7)], SEAM, 0.05, 0.4)
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.75), H.tone(BELT, 0.8), 1.0)
    H.stud(f, 4.0, 9.5, 0.2, GOLD, spec=0.7)
    skirt(f, 10.75)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), COAT, top=1.22, bottom=0.78, round_=0.13, fold_xs=[1.3, 2.7], fold_depth=0.09, fold_y0=2.0, fold_y1=9.0)
    yoke(f)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7                                                         # the gold piping of the opening turns round the side
    K.seam(f, [(ed, 2.0), (ed, 8.6)], SEAM, 0.05, 0.5)
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.75), H.tone(BELT, 0.8), 1.0)
    skirt(f, 10.75)


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
def shoulder_board(f, name):
    """A stiff shoulder board on the top of the sleeve: a darker crimson plate, a gold border along its lower edge, two gold studs."""
    plate = f.m_band(0.0, 2.5)
    K.cloth(f, plate, COAT_D, top=1.4, bottom=0.86, round_=0.1)
    H.bevel(f, plate, depth=0.3, gain=1.1, spec=0.25)
    f.fill(f.m_rect(0.0, 2.1, f.w, 2.5), GOLD, 1.0)
    f.fill(f.m_rect(0.0, 2.1, f.w, 2.2), K.lit(GOLD, 1.5), 0.9)
    f.fill(f.m_rect(0.0, 2.48, f.w, 2.62), "#2a0a0a", 0.65)
    K.seam(f, [(0.15, 0.55), (f.w - 0.15, 0.55)], SEAM, 0.05, 0.5)
    H.shadow_under(f, 2.62, 0.55, 0.3)
    if name in ("front", "back"):
        for x in (0.75, f.w - 0.75):
            H.stud(f, x, 1.3, 0.22, GOLD, spec=0.85)


def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.4)
            K.cloth(f, sleeve, COAT, top=1.25, bottom=0.8, round_=0.16, fold_xs=[0.9, 2.1], fold_depth=0.10, fold_y0=2.8, fold_y1=8.0)
            K.sheen(f, sleeve, [f.w * 0.33], 3.0, 7.6, 0.2, "#e7a090")
            for y, k in ((5.2, 0.6), (5.75, 0.4)):                                                       # the elbow creases
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.16) * sleeve, K.deep(COAT, 0.55), k)
            f.fill(f.m_polyline([(0.0, 4.95), (f.w * 0.5, 5.2), (f.w, 5.0)], 0.2) * sleeve, K.lit(COAT, 1.4), 0.3)
            shoulder_board(f, nm)
            H.shadow_under(f, 8.4, 0.5, 0.3)
            K.cuff(f, 8.4, 9.65, GOLD, pipe="#6a4a14", stud_color="#fff0b0", spec=0.4)
            K.glove(f, GLOVE, 9.65, 12.0, nm, finger_n=3 if f.w >= 4 else 2, knuckle_k=1.5)
            H.shadow_under(f, 9.65, 0.5, 0.22)


def caps(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", COAT_D, edge=GOLD, k_top=1.4, overlay=ov)
            K.cap(cv, part, "bottom", GLOVE, edge=None, k_top=1.2, overlay=ov)
    for part in ("leg_r", "leg_l"):
        K.cap(cv, part, "bottom", "#0d0a0a", edge=None, k_top=1.0)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 7.0, nm, knee=False, top=1.35, bottom=0.85, seam_color="#5a5a6a")
                skirt(f, 0.0, 5.2, hem=4.4)                                                                  # the coat skirt hangs to the middle of the thigh, a gold hem
                H.shadow_under(f, 5.2, 0.8, 0.3)
                H.shadow_under(f, 7.0, 0.8, 0.4)
                K.boot(f, BOOT, 7.0, nm, strap=True, strap_color="#7a5a40", gold=GOLD, cuff_color=GOLD, cuff_h=1.0, welt="#7a5a40", sole="#0d0a0a")


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    caps(cv)
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)

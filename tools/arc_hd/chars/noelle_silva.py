"""Noelle Silva, the proud water mage of House Silva, HD 256x256 (docs/arc/hd_arc_bc1.md): the hand-painted look of the Black Bulls' tsundere princess.

What she wears is the 64x64 sheet's outfit (tools/arc/char_noelle_silva.py): a navy dress with a white stand collar and a dark-blue ribbon cravat at the neck, a brown belt with a gold buckle, long white
sleeves with lilac cuffs, knee boots. The short black Black Bull cloak with the water-blue bull (`cape`, `cape_low`, the only glow) is a 3D extra (`pmx_noelle_silva`), so the skin carries nothing of it.
The sheet's legs were navy down to the boots; the HD pass shows the bare thigh between the skirt hem and the knee boots (the sheet's spec says `legs: bare`).

Drawn at 4x detail on both layers: the big, shaded blue-violet anime eyes with lashes and two highlights, thin arched silver brows, a strong blush and a small pout, silver bangs framing the face, the white
collar with its stitching, the ribbon cravat with its knot and tails, the navy dress with princess seams, white piping and gold buttons, the belt with its gold buckle, a pleated skirt hem with white trim,
the white sleeves with folds and lilac cuffs over bare hands, the bare thighs and the knee boots with a folded cuff, a strap and a buckle.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus docs/arc/npca.md (which
cites the wikis): long silver-white hair in two low twin tails with navy ribbons, deep blue eyes, a fair blushing face, a pout. The kit hair stays the classic `twin_tails` (no anime cut has twin tails).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "twin_tails"
HAIR = "#e2e7f2"
HAIR_RGB = (226, 231, 242)
SKIN = ("#efbba0", "#fbd8c4", "#fff0e6")
DRESS, DRESS_D, DRESS_L = "#2c3e80", "#16204a", "#5070b8"
WHITE, WHITE_D = "#f1f1f7", "#c9cde0"
RIBBON, RIBBON_L = "#2a3f98", "#5b78d8"
LILAC = "#b9bfe0"
BELT, BUCKLE = "#3e2c22", "#d8b048"
BOOT = "#46342c"
IRIS = ("#17246e", "#5a74e0", "#c4d6ff")
BROW = "#9aa2bc"
SEAMS = "#8a9ae0"


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.08, chin=0.06)
    pal = K.hair_palette(HAIR)
    # silver bangs: a mass over the brow with a short fringe of soft locks, two long locks that frame the face (the twin tails hang behind)
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.5), (7.4, 2.1), (6.5, 2.6), (5.5, 1.9), (4.5, 2.4), (3.6, 1.8), (2.6, 2.5), (1.5, 2.0), (0.7, 2.6), (-0.3, 2.3)]
    K.hair_mass(f, pal, outline, 0.0, 2.6, shadow_k=0.87, shadow_depth=2.0, streaks=(0.9, 2.1, 3.3, 4.7, 6.0, 7.2))
    for sh, (cx, w, top, ln, lean) in ((0.85, (6.5, 1.5, 1.6, 1.7, -0.3)), (0.9, (4.6, 1.2, 1.7, 1.0, 0.15)), (0.9, (3.4, 1.2, 1.7, 1.1, -0.15)), (0.85, (1.5, 1.5, 1.6, 1.8, 0.3))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    for x, lean in ((0.4, 0.2), (7.6, -0.2)):                                           # the long locks along the cheeks
        K.lock(f, pal, x, 1.0, 1.4, 5.6, lean, 1.0)
    # thin arched silver brows (the inner ends a little low: she is cross), big shaded eyes with long lashes, a strong blush, a nose dot and a small pout
    H.brow(f, (3.4, 3.95), (1.15, 3.4), BROW, 0.26, 0.1, arch=0.2)
    H.brow(f, (4.6, 3.95), (6.85, 3.4), BROW, 0.26, 0.1, arch=0.2)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.05, 2.6, 2.3, IRIS, lash="#1a1630", tilt=0.1, lash_w=0.5, flick=0.4, lid_shadow=0.34, iris_r=1.08, hi=0.97)
    H.blush(f, 1.05, 6.3, 1.05, 0.55, "#f2848c", 0.55)
    H.blush(f, 6.95, 6.3, 1.05, 0.55, "#f2848c", 0.55)
    H.nose(f, 4.0, 6.2, "#d89a86", 0.2)
    f.fill(f.m_polyline([(3.4, 7.2), (3.75, 7.0), (4.25, 7.0), (4.6, 7.2)], 0.2), "#9c4c58", 0.9)       # a small tight pout: the mouth line bends down at both ends
    f.ellipse(4.0, 7.5, 0.62, 0.24, "#ea9aa2", a=0.85, soft=1.6)                         # the pushed-out lower lip
    f.ellipse(3.88, 7.43, 0.22, 0.08, "#ffd6d8", a=0.7, soft=1.5)
    f.fill(f.m_line((3.5, 7.78), (4.5, 7.78), 0.14, 0.1), H.tone(SKIN[0], 0.8), 0.45)                      # the dimple under the lip


def head_rear(cv):
    """The back and the sides of the head (base layer): the sheet's twin-tail cut leaves the nape bare; her hair is gathered from the whole back of the head into the two tails, so the scalp is
    painted as silver hair down to a pointed hairline at the nape and behind the ears (the ear and the temple stay skin). The 3D cap sits over it."""
    pal = K.hair_palette(HAIR)
    f = cv.face("head", "back")
    poly = [(-0.3, -0.3), (8.3, -0.3), (8.3, 5.2), (7.3, 6.1), (6.2, 6.0), (5.2, 6.9), (4.0, 7.4), (2.8, 6.9), (1.8, 6.0), (0.7, 6.1), (-0.3, 5.2)]
    mass = K.hair_mass(f, pal, poly, 0.0, 6.5, shadow_k=0.9, shadow_depth=1.2, streaks=(0.8, 1.9, 3.0, 4.2, 5.3, 6.4, 7.3), streak_len=3.2)
    f.fill(f.m_line((4.0, 0.2), (4.0, 7.0), 0.1, 0.08) * mass, pal["d"], 0.5)                       # the parting of the two tails
    for nm in ("west", "east"):
        f = cv.face("head", nm)
        flip = (nm == "east")
        px = (lambda x: f.w - x) if flip else (lambda x: x)                                    # model x from the BACK edge of the side face (west: u=0 is the back)
        outline = [(px(-0.3), -0.3), (px(8.3), -0.3), (px(8.3), 2.9), (px(5.6), 3.1), (px(4.4), 3.5), (px(4.1), 5.2), (px(4.4), 6.0), (px(3.2), 6.5), (px(1.6), 7.1), (px(-0.3), 6.6)]
        K.hair_mass(f, pal, outline, 0.0, 6.5, shadow_k=0.88, shadow_depth=1.0, streaks=tuple(px(x) for x in (0.7, 1.8, 2.9, 4.1, 5.2, 6.4, 7.5)), streak_len=2.6)


# ================================================================================================ the body
def collar(f, front):
    """The white stand collar with a stitched edge; on the front the ribbon cravat: a knot and two tails."""
    band = f.m_rect(-0.1, 0.0, f.w + 0.1, 1.5)
    f.fill(band, H.lin((0, 0), (0, 1.5), [(0, K.lit(WHITE, 1.05)), (0.55, WHITE), (1, H.tone(WHITE, 0.82))]))
    f.fill(f.m_rect(-0.1, 1.42, f.w + 0.1, 1.58), WHITE_D, 0.9)
    K.seam(f, [(0.12, 1.15), (f.w - 0.12, 1.15)], "#aeb4d0", 0.05, 0.6, (0.3, 0.2), 0.15)
    H.shadow_under(f, 1.55, 0.55, 0.3)
    if front:
        for sgn in (-1.0, 1.0):                                                          # the ribbon tails hang from the knot down the chest
            tail = f.m_poly([(4.0 + sgn * 0.2, 1.3), (4.0 + sgn * 1.15, 1.9), (4.0 + sgn * 1.55, 3.9), (4.0 + sgn * 1.0, 3.55), (4.0 + sgn * 0.55, 4.0), (4.0 + sgn * 0.3, 2.2)])
            f.fill(tail, H.lin((4, 1.3), (4, 4.0), [(0, RIBBON_L), (0.5, RIBBON), (1, H.tone(RIBBON, 0.65))]))
            f.fill(f.m_polyline([(4.0 + sgn * 0.2, 1.4), (4.0 + sgn * 1.1, 1.95), (4.0 + sgn * 1.5, 3.8)], 0.07) * tail, K.lit(RIBBON_L, 1.3), 0.7)
        f.ellipse(4.0, 1.25, 0.75, 0.55, RIBBON)                                         # the knot
        f.ellipse(3.85, 1.12, 0.35, 0.22, K.lit(RIBBON_L, 1.2), a=0.7)
        f.fill(f.m_polyline([(3.4, 1.35), (4.0, 1.55), (4.6, 1.35)], 0.09), H.tone(RIBBON, 0.45), 0.8)


def dress_front(f):
    m = f.m_all()
    K.cloth(f, m, DRESS, top=1.35, bottom=0.8, round_=0.08, fold_xs=[1.2, 6.8], fold_depth=0.07, fold_y0=3.0, fold_y1=9.0)
    H.sheen(f, m, [1.5, 6.5], width=0.22, strength=0.15, tilt=0.02, y0=2.0, y1=8.6, color="#8a9ae0")
    # princess seams from the shoulders to the belt, white piping down both front edges, two gold buttons
    for x0, x1 in ((1.4, 2.0), (6.6, 6.0)):
        K.seam(f, [(x0, 1.9), (x1, 8.7)], SEAMS, 0.06, 0.55)
    for x in (3.55, 4.45):
        f.fill(f.m_line((x, 3.9), (x, 8.9), 0.12, 0.12), K.lit(WHITE, 1.0), 0.85)
    f.fill(f.m_rect(3.62, 3.9, 4.38, 8.9), H.tone(DRESS, 0.7), 0.5)
    for y in (5.0, 7.0):
        H.stud(f, 4.0, y, 0.22, "#e6c878")
    K.crease(f, [(0.5, 6.2), (1.6, 6.9), (2.3, 8.3)], DRESS, 0.1, 0.4)
    K.crease(f, [(7.5, 6.2), (6.4, 6.9), (5.7, 8.3)], DRESS, 0.1, 0.4)
    collar(f, True)


def belt(f, front):
    K.belt_band(f, 9.0, 10.05, BELT, stitch="#a07a58")
    K.belt_band(f, 10.05, 10.45, H.tone(BELT, 0.62), stitch_a=0.0)
    H.shadow_under(f, 10.45, 0.4, 0.3)
    if front:
        K.buckle(f, 4.0, 9.55, 2.2, 1.45, BUCKLE)


def skirt(f, front):
    """Rows 10.45..12: the pleated hem of the dress with a white trim at the bottom."""
    m = f.m_rect(0.0, 10.45, f.w, 12.0)
    f.fill(m, H.lin((0, 10.45), (0, 12.0), [(0, H.tone(DRESS, 0.95)), (1, H.tone(DRESS, 0.7))]))
    for x in np.arange(0.5, f.w, 1.0):                                                   # pleats: a dark fold and a light flank
        f.fill(f.m_line((x, 10.5), (x - 0.04, 11.8), 0.12, 0.1), H.tone(DRESS, 0.55), 0.55)
        f.fill(f.m_line((x + 0.2, 10.5), (x + 0.16, 11.8), 0.09, 0.07), K.lit(DRESS, 1.45), 0.4)
    f.fill(f.m_rect(0.0, 11.62, f.w, 12.0), H.lin((0, 11.62), (0, 12.0), [(0, WHITE), (1, WHITE_D)]))
    f.fill(f.m_rect(0.0, 11.56, f.w, 11.66), H.tone(DRESS, 0.5), 0.7)
    H.shadow_under(f, 10.45, 0.4, 0.28)


def body_front(f, ov):
    dress_front(f)
    skirt(f, True)
    belt(f, True)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, DRESS, top=1.3, bottom=0.8, round_=0.08, fold_xs=[1.7, 6.3], fold_depth=0.07, fold_y0=2.5, fold_y1=9.0)
    f.fill(f.m_line((4.0, 1.6), (4.0, 9.0), 0.16, 0.12), H.tone(DRESS, 0.5), 0.5)
    for y in (2.6, 4.0, 5.4, 6.8, 8.2):                                                  # the lacing of the back: small gold eyelets
        for x in (3.62, 4.38):
            f.ellipse(x, y, 0.1, 0.1, "#d8b048", a=0.85)
    collar(f, False)
    skirt(f, False)
    belt(f, False)
    bow = [(4.0, 8.9), (2.6, 8.3), (2.3, 9.6), (3.9, 9.3)]                               # the belt's bow knot sits at the back
    f.fill(f.m_poly(bow), H.tone(RIBBON, 1.0), 0.0)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), DRESS, top=1.3, bottom=0.8, round_=0.1, fold_xs=[1.2, 2.6], fold_depth=0.07, fold_y0=2.5, fold_y1=9.0)
    x = 0.9 if name == "east" else f.w - 0.9
    K.seam(f, [(x, 2.0), (x, 8.8)], SEAMS, 0.05, 0.5)
    collar(f, False)
    skirt(f, False)
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


# ================================================================================================ the arms (white sleeves, lilac cuffs, bare hands)
def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            sleeve = f.m_band(0.0, 10.0)
            K.cloth(f, sleeve, WHITE, top=1.06, bottom=0.82, round_=0.14, fold_xs=[f.w * 0.35, f.w * 0.75], fold_depth=0.08, fold_y0=1.0, fold_y1=9.6)
            f.shade(sleeve, 1.0 + 0.06 * K.blob(f, f.w * 0.4, 1.4, f.w * 0.7, 1.6), 0.9)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), WHITE_D, 0.7)                             # the shoulder seam
            K.seam(f, [(0.0, 0.75), (f.w, 0.75)], "#b4bad0", 0.05, 0.5, (0.3, 0.2), 0.15)
            for y, k in ((4.8, 0.45), (5.4, 0.3)):
                K.crease(f, [(0.0, y), (f.w * 0.4, y + 0.3), (f.w, y + 0.05)], WHITE, 0.1, k, light=1.1, mask=sleeve)
            # the lilac cuff with a stitch and a small button, the bare hand under it
            cuff = f.m_rect(0.0, 9.6, f.w, 10.8)
            f.fill(cuff, H.lin((0, 9.6), (0, 10.8), [(0, K.lit(LILAC, 1.1)), (1, H.tone(LILAC, 0.8))]))
            f.fill(f.m_rect(0.0, 9.6, f.w, 9.74), H.tone(LILAC, 0.6), 0.7)
            K.seam(f, [(0.1, 10.2), (f.w - 0.1, 10.2)], "#e6e8f6", 0.05, 0.6, (0.3, 0.2), 0.15)
            H.shadow_under(f, 10.8, 0.4, 0.28)
            if nm == "front":
                H.stud(f, f.w * 0.5, 10.2, 0.15, "#d8b048", spec=0.6)
            hand = f.m_band(10.8, 12.0)
            f.fill(hand, H.lin((0, 10.8), (0, 12.0), [(0, H.tone(SKIN[1], 0.9)), (1, SKIN[0])]))
            K.fingers(f, SKIN, 11.2, 12.0, n=2)


# ================================================================================================ the legs
def leg(f, ov, name):
    # the bare thigh: skin from the skirt hem (row 3.6) to the knee boot (row 5.6)
    thigh = f.m_band(0.0, 5.8)
    f.fill(thigh, H.lin((0, 0), (0, 5.8), [(0, H.tone(SKIN[1], 0.9)), (0.6, SKIN[1]), (1, H.tone(SKIN[0], 0.95))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(thigh, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
    f.shade(thigh, 1.0 + 0.1 * K.blob(f, f.w * 0.4, 3.9, f.w * 0.6, 1.4), 0.9)
    # the navy skirt (the sheet's second layer covers rows 0..3.6) with a white trim at its hem
    sk = f.m_band(0.0, 3.6)
    K.cloth(f, sk, DRESS, top=1.2, bottom=0.8, round_=0.1, fold_xs=[f.w * 0.5], fold_depth=0.07, fold_y0=0.2, fold_y1=3.2)
    f.fill(f.m_rect(0.0, 3.1, f.w, 3.6), H.lin((0, 3.1), (0, 3.6), [(0, WHITE), (1, WHITE_D)]))
    f.fill(f.m_rect(0.0, 3.06, f.w, 3.16), H.tone(DRESS, 0.5), 0.7)
    H.shadow_under(f, 3.6, 0.8, 0.36, mask=thigh)
    # the knee boots: a folded cuff with a stitch, then the shaft with a strap
    cuff = f.m_rect(0.0, 5.5, f.w, 6.6)
    f.fill(cuff, H.lin((0, 5.5), (0, 6.6), [(0, K.lit(BOOT, 1.9)), (0.5, K.lit(BOOT, 1.35)), (1, H.tone(BOOT, 0.8))]))
    f.fill(f.m_rect(0.0, 5.5, f.w, 5.64), "#0c0806", 0.55)
    K.seam(f, [(0.12, 6.1), (f.w - 0.12, 6.1)], "#c8a884", 0.06, 0.7, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 6.6, 11.25, name, strap="#2a1e18", gold=BUCKLE)


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
    head_rear(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim

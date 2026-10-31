"""Yuno, the Golden Dawn's wind prodigy, HD 256x256 (docs/arc/hd_arc_bc1.md): the hand-painted look of the quiet rival of the Black Bulls' Asta.

What he wears is the 64x64 sheet's outfit (tools/arc/char_yuno.py): a black shirt with a gold stand collar and gold cuffs under the Golden Dawn mantle, long sleeves, a dark brown belt with a gold buckle,
dark trousers in calf boots. The tan-gold mantle with the sun emblem and its blue feathered collar (`cape`, `cape_low`) and the floating four-leaf grimoire (`prop_float`, wind-green glow) are 3D extras
(`pmx_yuno`), so the skin only carries what is under them. No glow on the skin (the glow is on the grimoire).

Drawn at 4x detail on both layers: the calm, sharp, teal-green anime eyes with lashes and highlights, thin straight brows, the fair face, the parted black fringe, the gold stand collar with stitching and a
button, the black shirt with a sheen, a placket with three gold buttons and gold piping, the long sleeves with elbow creases and the beveled gold cuffs over bare hands, the belt with its gold buckle, the
trousers and the boots with cuff, strap and welt.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus docs/arc/npcb.md (which
cites the wikis): short messy black hair with long bangs, a calm composed face, green eyes, the Golden Dawn's gold-white robe over a dark shirt. The kit hair is `anime_bangs_long`: the parted bangs
that sweep to the temples and leave the eyes free.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc1_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_bangs_long"
HAIR = "#1a1a24"                                                                    # black with a blue cast (the sheet's #16161c turns to a flat black after the tint pass)
HAIR_RGB = (26, 26, 36)
SKIN = ("#e9b898", "#f8d4ba", "#ffeadb")
SHIRT, SHIRT_L = "#202034", "#3c3c5c"
GOLD, GOLD_D = "#d4aa54", "#8c6a24"
TROUS = "#272733"
BOOT = "#43322a"
BELT, BUCKLE = "#3e2c22", "#d4aa54"
FUR, FUR_L, FUR_D = "#6285b8", "#dcecfb", "#34507c"                                 # the blue feathered shoulder collar of the Golden Dawn mantle (the sheet's `shoulder_fur` #5f7fb0)
IRIS = ("#103c34", "#2f8a72", "#a6ecd4")
BROW = "#1a1a22"


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.10, chin=0.08)
    pal = K.dark_hair_palette(HAIR)
    # the parted fringe: a black mass at the crown; from the part at x 4 two blades sweep out to the temples and leave the middle of the forehead and the eyes free
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.6, 2.0), (6.6, 1.5), (5.3, 1.2), (4.0, 1.0), (2.7, 1.2), (1.4, 1.5), (0.4, 2.0), (-0.3, 2.3)]
    K.hair_mass(f, pal, outline, 0.0, 2.2, shadow_k=0.88, shadow_depth=1.6, streaks=(0.9, 2.2, 3.4, 4.6, 5.8, 7.0))
    for sgn in (-1.0, 1.0):
        x0 = 4.0 + sgn * 0.15
        blade = [(x0, 0.7), (x0 + sgn * 1.6, 1.0), (x0 + sgn * 3.0, 1.9), (x0 + sgn * 3.7, 3.2), (x0 + sgn * 3.85, 4.6), (x0 + sgn * 3.4, 3.7), (x0 + sgn * 2.3, 2.8), (x0 + sgn * 0.9, 2.1), (x0, 1.9)]
        m = f.m_poly(blade)
        f.fill(m, H.lin((x0, 0.7), (x0 + sgn * 3.8, 4.6), [(0, pal["m"]), (0.6, pal["l"]), (1, pal["m"])]))
        f.fill(f.m_polyline(blade[1:5], 0.07) * m, pal["h"], 0.55)
        f.fill(f.m_polyline(blade[5:9], 0.07) * m, pal["d"], 0.8)
    for x in (0.45, 7.55):                                                              # the side locks at the temples
        K.lock(f, pal, x, 0.9, 1.0, 4.2, 0.15 if x < 4 else -0.15, 1.0)
    # thin straight brows, calm sharp eyes: narrow almonds with a heavy lash line, teal irises, a small neutral mouth
    H.brow(f, (3.4, 3.7), (1.1, 3.35), BROW, 0.22, 0.09, arch=0.03)
    H.brow(f, (4.6, 3.7), (6.9, 3.35), BROW, 0.22, 0.09, arch=0.03)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.0, 2.5, 1.7, IRIS, lash="#0c0c14", tilt=0.26, lash_w=0.46, flick=0.38, lid_shadow=0.42, iris_r=1.02, hi=0.9, pupil="#06100e")
    H.nose(f, 4.0, 6.1, "#cf9a86", 0.2)
    H.mouth_smile(f, 4.0, 7.0, 1.7, color="#9a5a56", lip="#e0a09a", depth=0.0, thick=0.15)
    f.shade(f.m_ellipse(1.3, 6.2, 0.9, 1.2), 0.95, 0.5)                                    # the cheeks are a little hollow: a calm, lean face
    f.shade(f.m_ellipse(6.7, 6.2, 0.9, 1.2), 0.95, 0.5)


# ================================================================================================ the body
def collar(f, front, back=False):
    """The gold stand collar round the neck (x 1.5..6.5) with a dark lower edge, stitching and, on the front, a button; the blue feathered collar of the mantle sits on the shoulders at both ends."""
    x0, x1 = 1.4, f.w - 1.4
    if f.w < 6:
        x0, x1 = 0.0, 0.0
    if x1 > x0:
        band = f.m_rect(x0, 0.0, x1, 1.45)
        f.fill(band, H.lin((0, 0), (0, 1.45), [(0, H.tone(GOLD, 1.35)), (0.45, GOLD), (1, H.tone(GOLD, 0.62))]))
        H.bevel(f, band, depth=0.22, gain=1.0, spec=0.35)
        f.fill(f.m_rect(x0, 1.38, x1, 1.55), GOLD_D, 0.9)
        K.seam(f, [(x0 + 0.15, 1.12), (x1 - 0.15, 1.12)], "#f6e2a0", 0.05, 0.6, (0.3, 0.2), 0.15)
        H.shadow_under(f, 1.5, 0.55, 0.32, x0=x0, x1=x1)
        if front:
            f.fill(f.m_line((4.0, 0.0), (4.0, 1.45), 0.1, 0.1), GOLD_D, 0.8)
            H.stud(f, 3.55, 0.75, 0.17, "#f0d890")
        K.feather_pad(f, -0.1, x0 + 0.05, 0.0, 1.9, FUR, FUR_L, FUR_D, seed=3)
        K.feather_pad(f, x1 - 0.05, f.w + 0.1, 0.0, 1.9, FUR, FUR_L, FUR_D, seed=4)
    else:
        K.feather_pad(f, -0.1, f.w + 0.1, 0.0, 1.9, FUR, FUR_L, FUR_D, seed=5)


def shirt_front(f):
    m = f.m_all()
    K.cloth(f, m, SHIRT, top=1.4, bottom=0.8, round_=0.08, fold_xs=[1.3, 6.7], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    H.sheen(f, m, [1.5, 6.4], width=0.26, strength=0.26, tilt=0.03, y0=2.0, y1=8.6, color="#9a9ac0")
    # the placket down the middle: a lit fold, a gold piping on each side and three small gold buttons
    f.fill(f.m_rect(3.7, 1.5, 4.3, 9.0), H.tone(SHIRT, 0.62), 0.6)
    for x in (3.62, 4.38):
        f.fill(f.m_line((x, 1.5), (x, 9.0), 0.14, 0.14), GOLD, 0.85)
    for y in (3.0, 5.2, 7.4):
        H.stud(f, 4.0, y, 0.2, "#e6c876")
    K.crease(f, [(0.5, 6.0), (1.6, 6.7), (2.4, 8.2)], SHIRT, 0.1, 0.4)
    K.crease(f, [(7.5, 6.0), (6.4, 6.7), (5.6, 8.2)], SHIRT, 0.1, 0.4)
    collar(f, True)


def belt(f, front):
    K.belt_band(f, 9.0, 10.05, BELT, stitch="#a07a58")
    K.belt_band(f, 10.05, 10.5, H.tone(BELT, 0.62), stitch_a=0.0)
    H.shadow_under(f, 10.5, 0.45, 0.3)
    if front:
        K.buckle(f, 4.0, 9.55, 2.2, 1.45, BUCKLE)


def skirt(f, front):
    """Rows 10.5..12: the shirt tail hangs over the trousers with a gold hem line."""
    m = f.m_rect(0.0, 10.5, f.w, 12.0)
    f.fill(m, H.lin((0, 10.5), (0, 12.0), [(0, H.tone(SHIRT, 0.8)), (1, H.tone(SHIRT, 0.55))]))
    f.fill(f.m_rect(0.0, 11.7, f.w, 11.9), GOLD, 0.8)
    if front:
        f.fill(f.m_line((4.0, 10.6), (4.0, 12.0), 0.1, 0.1), H.tone(SHIRT, 0.4), 0.8)
    H.shadow_under(f, 10.5, 0.4, 0.28)


def body_front(f, ov):
    if not ov:
        shirt_front(f)
        skirt(f, True)
    else:
        shirt_front(f)
        skirt(f, True)
    belt(f, True)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, SHIRT, top=1.3, bottom=0.8, round_=0.08, fold_xs=[1.7, 6.3], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    f.shade(m, 1.0 + 0.16 * K.blob(f, 2.2, 3.2, 1.7, 1.9) + 0.16 * K.blob(f, 5.8, 3.2, 1.7, 1.9), 0.9)
    f.fill(f.m_line((4.0, 1.6), (4.0, 9.0), 0.17, 0.12), H.tone(SHIRT, 0.45), 0.55)
    K.seam(f, [(4.0, 1.8), (4.0, 8.9)], "#5a5a78", 0.05, 0.5)
    collar(f, False, True)
    skirt(f, False)
    belt(f, False)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), SHIRT, top=1.3, bottom=0.8, round_=0.1, fold_xs=[1.2, 2.6], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    seam_x = 0.9 if name == "east" else f.w - 0.9
    K.seam(f, [(seam_x, 2.0), (seam_x, 8.8)], "#5a5a78", 0.05, 0.5)
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


# ================================================================================================ the arms (long sleeves, gold cuffs, bare hands)
def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            sleeve = f.m_band(0.0, 10.2)
            K.cloth(f, sleeve, SHIRT, top=1.35, bottom=0.82, round_=0.14, fold_xs=[f.w * 0.5], fold_depth=0.07, fold_y0=1.5, fold_y1=9.5)
            f.shade(sleeve, 1.0 + 0.12 * K.blob(f, f.w * 0.4, 1.2, f.w * 0.7, 1.5), 0.9)           # the shoulder catches the light
            H.sheen(f, sleeve, [f.w * 0.3], width=0.24, strength=0.24, tilt=0.0, y0=1.5, y1=9.6, color="#9a9ac0")
            K.feather_pad(f, -0.1, f.w + 0.1, 0.0, 2.3, FUR, FUR_L, FUR_D, seed=11 + len(nm))        # the feathered collar of the mantle rests on the shoulder
            for y, k in ((4.8, 0.5), (5.4, 0.35)):                                                   # the creases at the elbow
                K.crease(f, [(0.0, y), (f.w * 0.4, y + 0.3), (f.w, y + 0.05)], SHIRT, 0.1, k, mask=sleeve)
            # the beveled gold cuff and the bare hand under it
            H.trim_band(f, 9.9, 10.9, GOLD, spec=0.4, outline=0.6)
            K.seam(f, [(0.1, 10.4), (f.w - 0.1, 10.4)], "#f6e2a0", 0.05, 0.6, (0.3, 0.2), 0.15)
            H.shadow_under(f, 10.9, 0.4, 0.3)
            hand = f.m_band(10.9, 12.0)
            f.fill(hand, H.lin((0, 10.9), (0, 12.0), [(0, H.tone(SKIN[1], 0.88)), (1, SKIN[0])]))
            K.fingers(f, SKIN, 11.2, 12.0, n=2)


# ================================================================================================ the legs
def leg(f, ov, name):
    K.trousers(f, TROUS, 0.0, 6.4, name, knee=True, fold_xs=(f.w * 0.5,), top=1.35, bottom=0.78, seam_color="#5a5a78", knee_y=(3.5, 4.2))
    f.shade(f.m_band(0.0, 6.4), 1.0 + 0.14 * K.blob(f, f.w * 0.45, 1.8, f.w * 0.7, 2.0), 0.9)
    H.shadow_under(f, 6.1, 0.9, 0.35)
    cuff = f.m_rect(0.0, 6.3, f.w, 7.35)                                                  # the folded boot cuff
    f.fill(cuff, H.lin((0, 6.3), (0, 7.35), [(0, K.lit(BOOT, 1.9)), (0.5, K.lit(BOOT, 1.35)), (1, H.tone(BOOT, 0.8))]))
    f.fill(f.m_rect(0.0, 6.3, f.w, 6.42), "#0c0806", 0.55)
    f.fill(f.m_rect(0.0, 6.42, f.w, 6.56), K.lit(BOOT, 2.3), 0.5)
    K.seam(f, [(0.12, 6.9), (f.w - 0.12, 6.9)], "#c0a07c", 0.06, 0.7, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 7.35, 11.25, name, strap="#2a1e18", gold="#d4aa54")


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
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim

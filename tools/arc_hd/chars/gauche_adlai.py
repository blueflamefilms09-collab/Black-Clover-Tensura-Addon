"""Gauche Adlai, the cold and clever mirror mage of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc3.md): the hand-painted look of the brother who loves his little sister.

What he wears is the 64x64 sheet's outfit (tools/arc/char_gauche_adlai.py): a near-black high-collared uniform with lavender trim, long sleeves, black gloves, a dark belt with a pale buckle, dark trousers
in calf boots, and a silver monocle over the left eye that stands for the mirror that replaces it. The Black Bull cloak (`cape`, `cape_low`) and the orbiting glass orb (`prop_float`) are 3D extras
(`pmx_gauche_adlai`), so the back of the coat is kept plain. The 3D hair is `anime_short_open` (short unkempt black hair that leaves the painted fringe visible; tinted a blue-black so the clumps read).

Drawn at 4x detail on both layers: the tired dark violet right eye with its bags, the left eye as a polished silver mirror with a violet rim light (glow map) behind a raised silver monocle ring (second
layer) with a short chain, flat thick brows, a thin cold mouth, a messy black fringe of tapered clumps, the high collar with lavender piping and a clasp, the double lavender stripes down the front with small
silver buttons, lavender shoulder lines, the belt with its pale buckle, the sleeves with lavender cuff bands over black gloves with knuckle seams, the trousers with a lavender side stripe and the calf boots
with buckled straps, welts and soles.

Glow map (new in the HD pass; the 64x64 skin had none): the mirror eye and the rim of the monocle, in the violet-white of his Mirror Magic (the sheet says the monocle shines violet).

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus the header of
tools/arc/char_gauche_adlai.py (which cites the wikis): short messy black hair, pale skin, tired dark eyes, a cold mouth, a dark high-collared uniform under the Black Bull cloak, and a mirror where his
left eye should be (here the monocle).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc3_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_short_open"
HAIR = "#2a2a40"
HAIR_RGB = (42, 42, 64)
SKIN = ("#e2bcb6", "#f3dbd4", "#ffefe8")
COAT, COAT_L = "#2a2933", "#4a4860"
LAV, LAV_L, LAV_D = "#9a94c8", "#d6d0f4", "#5a5486"
TROUS = "#22212b"
BOOT = "#2c2a36"
BELT = "#2e2632"
GLOVE = "#1d1c24"
VIOLET, VIOLET_L = "#a89cff", "#e4deff"
MIRROR = ("#6a6a8a", "#c8c8e4", "#ffffff")
IRIS = ("#14121c", "#3a3550", "#7a7298")


# ================================================================================================ the head
def fringe(f, pal):
    """A messy black fringe: a mass at the crown and a few thick clumps over the brow, one long one over the right temple; the two side locks frame the face."""
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.5), (7.4, 2.1), (6.4, 2.6), (5.2, 1.9), (4.2, 2.4), (3.0, 1.9), (1.8, 2.5), (0.7, 2.0), (-0.3, 2.6)]
    K.hair_mass(f, pal, outline, 0.0, 2.5, shadow_k=0.88, shadow_depth=1.5, streaks=(0.9, 2.1, 3.3, 4.6, 5.8, 7.0))
    for sh, (cx, w, top, ln, lean) in ((0.9, (6.6, 1.5, 1.3, 2.3, -0.4)), (1.0, (4.9, 1.4, 1.4, 1.9, 0.4)), (0.9, (3.3, 1.4, 1.3, 2.4, -0.4)), (1.0, (1.7, 1.5, 1.2, 2.1, 0.3))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.5, 1.0, 1.0, 4.4, 0.15, 1.0)
    K.lock(f, pal, 7.5, 1.0, 1.0, 4.6, -0.15, 1.0)


def mirror_eye(f, cx, cy, w, h):
    """The left eye as a mirror: a polished silver almond with a bright sky-to-ground reflection, a dark horizon, a violet rim and a glint; it glows on the glow map."""
    almond = H.eye(f, cx, cy, w, h, ("#7a7a9c", "#d4d4ec", "#ffffff"), lash="#14121c", tilt=0.1, lash_w=0.4, flick=0.3, lid_shadow=0.3, iris_r=1.25, hi=0.0, pupil="#e8e8f8", sclera="#c8c8de")
    ys = np.clip((f.Y - (cy - h * 0.5)) / h, 0, 1)
    refl = f.m_ellipse(cx, cy + 0.05, w * 0.44, h * 0.46) * almond
    f.fill(refl, H.lin((cx, cy - h * 0.5), (cx, cy + h * 0.5), [(0, "#f4f4ff"), (0.48, "#c6c6e6"), (0.52, "#4a4670"), (1, "#8a84c0")]))
    f.fill(refl * np.clip(1.0 - np.abs(ys - 0.5) / 0.05, 0, 1), "#2a2648", 0.8)                  # the horizon line of the reflection
    f.fill(f.m_ring(cx, cy + 0.05, w * 0.44, 0.16) * almond, VIOLET, 0.8)
    f.ellipse(cx - w * 0.14, cy - h * 0.16, w * 0.1, h * 0.1, "#ffffff", a=0.95, soft=1.5)
    f.glow_fill(refl, VIOLET_L, 0.75)
    f.glow_fill(f.m_ring(cx, cy + 0.05, w * 0.44, 0.22) * almond, VIOLET, 0.95)
    return almond


def monocle(f, ov, cx=5.65, cy=5.05, r=1.45):
    """The silver monocle ring round the left eye (the viewer's right): a bevelled ring with a lit upper left, a clasp at its lower right and a short chain; on the second layer only ring and chain are cut."""
    ring = f.m_ring(cx, cy, r, 0.3)
    f.fill(f.m_ring(cx, cy, r, 0.44), H.tone("#8a8aa6", 0.4), 0.8)
    f.fill(ring, H.lin((cx - r, cy - r), (cx + r, cy + r), [(0, "#ffffff"), (0.35, "#d6d6ea"), (0.7, "#8a8aa8"), (1, "#5a5a78")]))
    f.fill(f.m_ring(cx, cy, r, 0.1) * f.m_rect(cx - r - 1, cy - r - 1, cx, cy), "#ffffff", 0.7)
    f.glow_fill(ring, VIOLET, 0.7)
    if not ov:
        f.fill(f.m_ellipse(cx, cy, r - 0.1, r - 0.1), "#c8c0ff", 0.1)
    chain = [(cx + r * 0.7, cy + r * 0.72), (cx + r * 0.85, cy + r * 1.45), (cx + r * 0.4, cy + r * 2.05), (cx + r * 0.8, cy + r * 2.55)]
    f.fill(f.m_polyline(chain, 0.2), "#b8b8d0", 0.95)
    for i in range(len(chain)):
        H.stud(f, chain[i][0], chain[i][1], 0.1, "#e0e0f0", spec=0.7)
    return ring


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.10, chin=0.08, jaw=0.12)
    pal = K.dark_hair_palette(HAIR)
    fringe(f, pal)
    for cx in (2.4, 5.6):
        K.eye_bags(f, cx, 5.8, 2.6, "#8a6a8c", 0.55, 0.5)
    H.brow(f, (3.5, 3.8), (0.9, 3.5), "#1a1a24", 0.34, 0.14, arch=0.0)                      # flat, heavy brows
    H.brow(f, (4.5, 3.8), (7.1, 3.5), "#1a1a24", 0.34, 0.14, arch=0.0)
    H.eye(f, 2.4, 5.1, 2.5, 1.5, IRIS, lash="#0a0a10", tilt=0.1, lash_w=0.46, flick=0.32, lid_shadow=0.5, iris_r=1.05, hi=0.8, pupil="#06060a")
    mirror_eye(f, 5.6, 5.1, 2.5, 1.7)
    H.nose(f, 4.0, 6.3, "#cf9e9a", 0.2)
    H.mouth_smile(f, 4.0, 7.2, 1.9, color="#9a5a62", lip="#d8a0a0", depth=-0.06, thick=0.15)    # a flat, slightly downturned mouth
    f.shade(f.m_ellipse(1.2, 6.5, 0.8, 1.1), 0.93, 0.55)
    f.shade(f.m_ellipse(6.8, 6.5, 0.8, 1.1), 0.93, 0.55)
    for ov in (False, True):
        g = cv.face("head", "front", ov)
        monocle(g, ov)
    ov = cv.face("head", "front", True)
    ov.fill(1.0 - H.union(ov.m_ring(5.65, 5.05, 1.45, 0.44), ov.m_polyline([(6.68, 6.1), (6.9, 6.5), (6.2, 7.1), (6.8, 7.6)], 0.28)), "#000000", mode="erase")


def head_rest(cv):
    pal = K.dark_hair_palette(HAIR)
    K.head_rear(cv, pal, back_y=5.8, nape_y=6.8, side_top=3.1, ear_x=4.1, side_low=5.6)
    for nm in ("west", "east"):
        f = cv.face("head", nm, False)
        K.ear_face(f, SKIN, nm, 4.6, 1.7)
    f = cv.face("head", "east", False)                                                     # the monocle's chain end is clipped to his left cheek; the side face shows a hint of the ring's back
    f.fill(f.m_line((0.1, 4.9), (0.9, 4.9), 0.2, 0.2), "#8a8aa8", 0.9)


# ================================================================================================ the body: a black uniform with lavender trim
def collar(f, front):
    """The high collar: a band with a lavender top edge and piping, a clasp on the front."""
    if f.w < 6:
        band = f.m_rect(0.0, 0.0, f.w, 1.5)
        f.fill(band, H.lin((0, 0), (0, 1.5), [(0, K.lit(COAT, 1.5)), (1, H.tone(COAT, 0.7))]))
        f.fill(f.m_rect(0.0, 1.38, f.w, 1.6), LAV, 0.95)
        f.fill(f.m_rect(0.0, 1.38, f.w, 1.46), LAV_L, 0.8)
        return
    x0, x1 = 1.2, f.w - 1.2
    band = f.m_rect(x0, 0.0, x1, 1.55)
    f.fill(band, H.lin((0, 0), (0, 1.55), [(0, K.lit(COAT, 1.6)), (0.6, COAT), (1, H.tone(COAT, 0.7))]))
    f.fill(f.m_rect(x0, 0.0, x0 + 0.2, 1.55), LAV, 0.8)
    f.fill(f.m_rect(x1 - 0.2, 0.0, x1, 1.55), LAV, 0.8)
    f.fill(f.m_rect(x0, 1.36, x1, 1.62), LAV)
    f.fill(f.m_rect(x0, 1.36, x1, 1.44), LAV_L, 0.8)
    H.shadow_under(f, 1.6, 0.5, 0.3, x0=x0, x1=x1)
    if front:
        f.fill(f.m_line((4.0, 0.0), (4.0, 1.5), 0.12, 0.12), H.tone(COAT, 0.4), 0.9)
        f.fill(f.m_rect(3.15, 0.55, 4.85, 1.1, 0.12), H.tone(LAV, 0.5))
        f.fill(f.m_rect(3.25, 0.62, 4.75, 1.02, 0.1), H.metal_paint(0.62, 1.02, LAV, 1.0))
        H.stud(f, 4.0, 0.82, 0.17, LAV_L, spec=0.9, glow=VIOLET)


def front_coat(f):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.5, bottom=0.8, round_=0.08, fold_xs=[2.2, 5.8], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    H.sheen(f, m, [1.6, 6.4], width=0.3, strength=0.2, tilt=0.03, y0=2.0, y1=8.6, color="#8a88b8")
    # the front opening: a centre seam, lavender piping on both sides of it and a row of small buttons
    f.fill(f.m_line((4.0, 1.7), (4.0, 9.0), 0.2, 0.2), H.tone(COAT, 0.4), 0.9)
    for x in (3.55, 4.45):
        f.fill(f.m_line((x, 1.7), (x, 9.0), 0.16, 0.16), LAV, 0.95)
        f.fill(f.m_line((x - 0.04, 1.7), (x - 0.04, 9.0), 0.05, 0.05), LAV_L, 0.7)
    for y in (2.6, 3.9, 5.2, 6.5, 7.8):
        H.stud(f, 5.0, y, 0.2, LAV_L, spec=0.9)
    # the two lavender stripes at the edges of the chest and the shoulder lines
    K.vband(f, 0.0, 0.55, 1.5, 9.1, LAV, 0.12, 0.4)
    K.vband(f, 7.45, 8.0, 1.5, 9.1, LAV, 0.12, 0.4)
    K.seam(f, [(0.95, 2.0), (0.95, 8.9)], LAV_L, 0.05, 0.55)
    K.seam(f, [(7.05, 2.0), (7.05, 8.9)], LAV_L, 0.05, 0.55)
    collar(f, True)


def tails(f, front):
    m = f.m_rect(0.0, 10.5, f.w, 12.0)
    f.fill(m, H.lin((0, 10.5), (0, 12.0), [(0, H.tone(COAT, 0.75)), (1, H.tone(COAT, 0.5))]))
    f.fill(f.m_rect(0.0, 11.7, f.w, 11.95), LAV, 0.95)
    f.fill(f.m_rect(0.0, 11.7, f.w, 11.78), LAV_L, 0.8)
    if f.w >= 6:
        f.fill(f.m_line((4.0, 10.6), (4.0, 11.7), 0.14, 0.14), H.tone(COAT, 0.35), 0.9)


def belt(f, front):
    K.belt_band(f, 9.15, 10.15, BELT, stitch="#7a6a8a")
    K.belt_band(f, 10.15, 10.55, H.tone(BELT, 0.65), stitch_a=0.0)
    H.shadow_under(f, 10.55, 0.45, 0.3)
    if front and f.w >= 6:
        K.buckle(f, 4.0, 9.65, 2.0, 1.4, LAV, opening="#14111a")


def body_back(f):
    K.cloth(f, f.m_all(), COAT, top=1.4, bottom=0.8, round_=0.08, fold_xs=[1.8, 6.2], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    f.fill(f.m_line((4.0, 1.8), (4.0, 9.0), 0.2, 0.2), H.tone(COAT, 0.4), 0.8)
    K.seam(f, [(4.0, 1.9), (4.0, 9.0)], LAV, 0.05, 0.5)
    K.vband(f, 0.0, 0.45, 1.5, 9.1, LAV, 0.1, 0.3)
    K.vband(f, 7.55, 8.0, 1.5, 9.1, LAV, 0.1, 0.3)
    collar(f, False)


def body_side(f, name):
    K.cloth(f, f.m_all(), COAT, top=1.4, bottom=0.8, round_=0.1, fold_xs=[1.4, 2.7], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    front_x0 = 3.45 if name == "west" else 0.0
    K.vband(f, front_x0, front_x0 + 0.55, 1.5, 9.1, LAV, 0.12, 0.4)
    K.seam(f, [(1.6 if name == "east" else 2.4, 2.0), (1.6 if name == "east" else 2.4, 8.9)], "#5a587a", 0.05, 0.45)
    collar(f, False)


def body(cv):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, "body", nm), (False, True)):
            if nm == "front":
                front_coat(f)
            elif nm == "back":
                body_back(f)
            else:
                body_side(f, nm)
            tails(f, nm == "front")
            belt(f, nm == "front")


# ================================================================================================ the arms: black sleeves, lavender cuffs, black gloves
def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            K.sleeve(f, COAT, 0.0, 9.6, sheen="#8a88b8", elbow=4.2)
            f.fill(f.m_rect(0.0, 0.3, f.w, 0.62), LAV, 0.9)                                      # the shoulder line
            f.fill(f.m_rect(0.0, 0.3, f.w, 0.4), LAV_L, 0.8)
            H.shadow_under(f, 0.62, 0.4, 0.25)
            K.cuff(f, 9.2, 10.4, LAV_D, LAV, "#d6d0f4")
            H.shadow_under(f, 10.4, 0.4, 0.3)
            K.glove(f, GLOVE, 10.4, 12.0, nm, shade=0.55)


# ================================================================================================ the legs
def leg(f, ov, name):
    K.trousers(f, TROUS, 0.0, 6.8, name, knee=True, fold_xs=(f.w * 0.5,), top=1.45, bottom=0.8, seam_color="#4a4868", knee_y=(3.4, 4.1))
    f.shade(f.m_band(0.0, 6.8), 1.0 + 0.12 * K.blob(f, f.w * 0.45, 2.0, f.w * 0.7, 2.2), 0.9)
    if name in ("west", "east"):
        K.vband(f, f.w * 0.5 - 0.2, f.w * 0.5 + 0.2, 0.0, 6.6, LAV, 0.08, 0.3, 0.3)             # the lavender side stripe
    H.shadow_under(f, 6.7, 0.7, 0.38)
    cuff = f.m_rect(0.0, 6.6, f.w, 7.6)
    f.fill(cuff, H.lin((0, 6.6), (0, 7.6), [(0, K.lit(BOOT, 2.2)), (1, H.tone(BOOT, 0.7))]))
    f.fill(f.m_rect(0.0, 6.6, f.w, 6.72), "#06060a", 0.6)
    f.fill(f.m_rect(0.0, 6.72, f.w, 6.86), LAV, 0.8)
    K.boot(f, BOOT, 7.6, 11.25, name, strap="#2e2632", gold=LAV)


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
    K.clear_head_overlay(cv)
    covers = K.all_covers(cv, ("body", "arm_r", "arm_l", "leg_r", "leg_l"))
    head_front(cv)
    head_rest(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    K.hold_cover(cv, covers)
    skin, glow = cv.finish()
    return skin, glow, hair, HAIR_RGB, slim

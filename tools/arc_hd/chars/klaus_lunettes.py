"""Klaus Lunettes, the Golden Dawn's proud strategist, HD 256x256 (docs/arc/hd_arc_bc3.md): the hand-painted look of the careful junior with the glasses.

What he wears is the 64x64 sheet's outfit (tools/arc/char_klaus_lunettes.py): a navy double-breasted uniform with a high collar, gold trim down both edges of the front and gold buttons over a white shirt,
long sleeves with white gloves under steel bracers, a brown belt with a gold buckle, dark trousers in knee-high boots, the blue feathered shoulder collar of the Golden Dawn mantle, gold hoop earrings and
dark rectangular glasses. The gold mantle with the sun (`cape`, `cape_low`) and the floating grimoire (`prop_float`, steel-blue glow) are 3D extras (`pmx_klaus_lunettes`), so the skin carries only what is
under them (the back is plain). No glow on the skin (the glow is on the grimoire).

Drawn at 4x detail on both layers: the sharp fuchsia anime eyes behind rectangular glasses (frames on the second layer, so they stand off the face, with glare), thin pale-blue brows, the angular side-swept
fringe, gold hoops on the ears, the high gold collar with a white jabot and a pin, the gold-bordered double-breasted front with two columns of buttons and stitching, the belt with its gold buckle, the long
navy sleeves with elbow creases, the beveled steel bracers (rim, ridge, rivets, gold bands) over white gloves, the trousers with creases and the knee boots with gold-trimmed turned cuffs, straps and soles.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus docs/arc/npcb.md section 10
(which cites the wikis): a tall slender nobleman, light-blue hair swept to the sides and back with an angular fringe, dark rectangular glasses, gold hoop earrings, fuchsia eyes, a proud stern face. The kit
hair is `anime_bangs_long` (long parted bangs that sweep out over the forehead and leave the glasses clear).
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc3_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_bangs_long"
HAIR = "#6fa4ee"
HAIR_RGB = (111, 164, 238)
SKIN = ("#e6b898", "#f6d4bc", "#ffeadc")
COAT = "#2b3659"
COAT_D = "#161c33"
GOLD, GOLD_L = "#d9b24e", "#f4de8c"
SHIRT = "#ece8e2"
STEEL = "#6f89a8"
BELT, BUCKLE = "#40291f", "#dcb650"
TROUS = "#1d2133"
BOOT = "#4a3528"
FUR, FUR_L, FUR_D = "#5f86bc", "#d6e8fa", "#2f4d7c"
IRIS = ("#62144a", "#cc4496", "#ffb4de")
FRAME = "#1a1d2c"


# ================================================================================================ the head
def fringe(f, pal):
    """The hair of the forehead: a mass at the crown with a side parting at x 5.2 and an angular swept fringe: a long blade over the left brow, a shorter one on the right."""
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.5), (7.6, 2.2), (6.8, 1.7), (5.6, 1.1), (4.4, 1.5), (3.2, 1.9), (1.8, 2.1), (0.6, 2.4), (-0.3, 2.8)]
    K.hair_mass(f, pal, outline, 0.0, 2.6, shadow_k=0.88, shadow_depth=1.6, streaks=(0.9, 2.0, 3.1, 4.3, 5.5, 6.7))
    blades = [
        # the long fringe: from the parting, swept to the left over the brow
        ([(5.2, 0.5), (4.0, 1.0), (2.6, 1.9), (1.4, 2.9), (0.9, 3.9), (1.9, 3.2), (3.0, 2.7), (4.3, 2.3), (5.5, 1.8)], 0),
        # a second blade under it, shorter
        ([(4.4, 1.1), (3.5, 1.8), (2.8, 2.9), (2.6, 3.4), (3.7, 2.9), (4.7, 2.2)], 1),
        # the right side: the hair falls from the parting to the temple
        ([(5.4, 0.7), (6.4, 1.0), (7.4, 1.8), (7.8, 3.0), (7.6, 4.3), (7.2, 3.2), (6.6, 2.5), (5.8, 2.0)], 0),
    ]
    for pts, tone_k in blades:
        m = f.m_poly(pts)
        c0 = pal["l"] if tone_k == 0 else pal["m"]
        f.fill(m, H.lin((pts[0][0], 0.5), (pts[4][0], pts[4][1]), [(0, c0), (0.55, pal["m"]), (1, H.mix(pal["m"], pal["d"], 0.35))]))
        f.fill(f.m_polyline(pts[1:5], 0.14) * m, pal["h"], 0.55)
        f.fill(f.m_polyline(pts[5:] + [pts[0]], 0.14) * m, pal["d"], 0.8)
    for x, ln, lean in ((0.45, 4.3, 0.1), (7.55, 4.1, -0.1)):                      # the sideburns
        K.lock(f, pal, x, 0.95, 1.0, ln, lean, 1.0)


def glasses(f, ov):
    """Rectangular dark glasses: a rounded frame with a heavier brow line, the bridge, a faint blue lens tint and two glare streaks. On the second layer only the frame is cut out."""
    for cx in (2.35, 5.65):
        x0, x1, y0, y1 = cx - 1.5, cx + 1.5, 3.75, 6.3
        ring, inside = K.ring_mask(f, x0, y0, x1, y1, 0.2, 0.35)
        brow_line = f.m_rect(x0, y0, x1, y0 + 0.34, 0.15)
        frame = H.union(ring, brow_line)
        if not ov:
            f.fill(inside, "#c8e0f6", 0.16)
            f.fill(f.m_poly([(x0 + 0.5, y1 - 0.4), (x0 + 1.25, y0 + 0.65), (x0 + 1.55, y0 + 0.65), (x0 + 0.8, y1 - 0.4)]) * inside, "#ffffff", 0.42)
            f.fill(f.m_poly([(x0 + 1.7, y1 - 0.4), (x0 + 2.2, y0 + 0.65), (x0 + 2.4, y0 + 0.65), (x0 + 1.9, y1 - 0.4)]) * inside, "#ffffff", 0.22)
        f.fill(frame, FRAME)
        f.fill(f.m_rect(x0 + 0.1, y0, x1 - 0.1, y0 + 0.12), "#4a4e66", 0.7)                     # the lit top edge of the frame
    br = f.m_rect(3.85, 4.3, 4.15, 4.6)
    f.fill(br, FRAME)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.10, chin=0.08, jaw=0.10)
    pal = K.hair_palette(HAIR)
    fringe(f, pal)
    H.brow(f, (3.4, 3.3), (0.9, 3.0), "#4e6a94", 0.26, 0.10, arch=0.04)
    H.brow(f, (4.6, 3.3), (7.1, 3.0), "#4e6a94", 0.26, 0.10, arch=0.04)
    K.eye_pair(f, IRIS, cx=(2.35, 5.65), cy=5.05, w=2.55, h=1.95, lash="#14101a", tilt=0.28, lash_w=0.44, flick=0.4, lid_shadow=0.42, iris_r=1.0, hi=0.9, pupil="#240a1c")
    H.nose(f, 4.0, 6.35, "#cf9a86", 0.2)
    H.mouth_smile(f, 4.0, 7.2, 1.8, color="#9a5a56", lip="#d89a92", depth=0.0, thick=0.16)       # a stern, level mouth
    f.shade(f.m_ellipse(1.1, 6.4, 0.8, 1.1), 0.94, 0.5)
    f.shade(f.m_ellipse(6.9, 6.4, 0.8, 1.1), 0.94, 0.5)
    for ov in (False, True):
        glasses(cv.face("head", "front", ov), ov)


def hoop(f, cx, y, r=0.8, w=0.2):
    """A gold hoop earring on the lobe of an ear: a ring with a lit upper edge."""
    f.fill(f.m_ring(cx, y + r, r, w), GOLD)
    f.fill(f.m_ring(cx, y + r, r, w) * f.m_rect(cx - r - 1, y, cx - 0.1, y + r), GOLD_L, 0.9)


def head_rest(cv):
    pal = K.hair_palette(HAIR)
    K.head_rear(cv, pal, back_y=6.1, nape_y=7.0, side_top=3.1, ear_x=4.0, side_low=6.0)
    for nm in ("west", "east"):
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            if not ov:
                K.ear_face(f, SKIN, nm, 4.5, 1.8)
            cx = 3.9 if nm == "west" else 4.1
            hoop(f, cx, 6.2)
        # the temple of the glasses runs from the front edge back to the top of the ear
        for ov in (False, True):
            f = cv.face("head", nm, ov)
            front_x = 7.9 if nm == "west" else 0.1
            end_x = 4.5 if nm == "west" else 3.5
            f.fill(f.m_line((front_x, 4.35), (end_x, 4.2), 0.2, 0.18), FRAME)


# ================================================================================================ the body
def collar(f, front, name=None):
    """The high navy collar with a gold border and, on the front, the white jabot under it with a gold pin; the blue feather pad of the mantle rests on the shoulders."""
    if f.w >= 6:
        x0, x1 = 1.3, f.w - 1.3
        band = f.m_rect(x0, 0.0, x1, 1.55)
        f.fill(band, H.lin((0, 0), (0, 1.55), [(0, K.lit(COAT, 1.6)), (0.6, COAT), (1, H.tone(COAT, 0.7))]))
        K.vband(f, x0, x0 + 0.34, 0.0, 1.55, GOLD, 0.1, 0.3, 0.4)
        K.vband(f, x1 - 0.34, x1, 0.0, 1.55, GOLD, 0.1, 0.3, 0.4)
        f.fill(f.m_rect(x0, 1.38, x1, 1.72), GOLD, 0.95)
        f.fill(f.m_rect(x0, 1.38, x1, 1.48), GOLD_L, 0.8)
        f.fill(f.m_rect(x0, 1.7, x1, 1.78), H.tone(GOLD, 0.4), 0.9)
        H.shadow_under(f, 1.76, 0.5, 0.32, x0=x0, x1=x1)
        if front:
            tri = f.m_poly([(3.0, 1.4), (5.0, 1.4), (4.0, 3.6)])
            f.fill(tri, H.lin((0, 1.4), (0, 3.6), [(0, "#f6f2ec"), (1, H.tone(SHIRT, 0.8))]))
            f.fill(f.m_polyline([(3.0, 1.45), (4.0, 3.5), (5.0, 1.45)], 0.12), H.tone(SHIRT, 0.55), 0.7)
            f.fill(f.m_line((4.0, 1.9), (4.0, 3.3), 0.12, 0.1), H.tone(SHIRT, 0.7), 0.5)
            H.gem(f, 4.0, 3.1, 0.3, "#c43a78", setting=GOLD, facets=4, spec=0.8, glow=False)
        pad = (-0.1, x0 + 0.1), (x1 - 0.1, f.w + 0.1)
    else:
        pad = ((-0.1, f.w + 0.1),)
    for i, (a, b) in enumerate(pad):
        K.feather_pad(f, a, b, 0.0, 1.9, FUR, FUR_L, FUR_D, seed=3 + i + (name == "west"))


def front_coat(f):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.45, bottom=0.78, round_=0.08, fold_xs=[2.2, 5.8], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    H.sheen(f, m, [1.6, 6.4], width=0.3, strength=0.22, tilt=0.03, y0=2.0, y1=8.6, color="#9aa6d0")
    # the double breast: the right flap (to the viewer's right) laps over the left one along x 4.0, edged with a stitch
    flap = f.m_poly([(4.0, 1.7), (6.9, 1.7), (6.9, 9.1), (4.0, 9.1)])
    f.fill(flap, K.lit(COAT, 1.12), 0.55)
    f.fill(f.m_line((4.0, 1.9), (4.0, 9.0), 0.2, 0.2), COAT_D, 0.85)
    f.fill(f.m_line((4.22, 1.9), (4.22, 9.0), 0.12, 0.12), K.lit(COAT, 1.9), 0.4)
    K.button_column(f, 2.45, (2.9, 4.7, 6.5, 8.3), GOLD, 0.27)
    K.button_column(f, 5.55, (2.9, 4.7, 6.5, 8.3), GOLD, 0.27)
    # the gold borders of the front: a bevelled band at each edge, a stitch inside it
    K.vband(f, 0.0, 1.0, 1.4, 9.15, GOLD, 0.22, 0.45)
    K.vband(f, 7.0, 8.0, 1.4, 9.15, GOLD, 0.22, 0.45)
    K.seam(f, [(1.3, 2.0), (1.3, 8.9)], GOLD_L, 0.06, 0.7)
    K.seam(f, [(6.7, 2.0), (6.7, 8.9)], GOLD_L, 0.06, 0.7)
    collar(f, True)


def tails(f, front):
    """The coat tails below the belt (y 10.5..12): dark navy with a gold hem line and a centre slit."""
    m = f.m_rect(0.0, 10.5, f.w, 12.0)
    f.fill(m, H.lin((0, 10.5), (0, 12.0), [(0, H.tone(COAT, 0.72)), (1, H.tone(COAT, 0.5))]))
    f.fill(f.m_rect(0.0, 11.65, f.w, 11.95), GOLD, 0.95)
    f.fill(f.m_rect(0.0, 11.65, f.w, 11.75), GOLD_L, 0.8)
    if f.w >= 6:
        f.fill(f.m_line((4.0, 10.6), (4.0, 11.65), 0.14, 0.14), COAT_D, 0.9)


def belt(f, front):
    K.belt_band(f, 9.15, 10.15, BELT, stitch="#a6804e")
    K.belt_band(f, 10.15, 10.55, H.tone(BELT, 0.6), stitch_a=0.0)
    H.shadow_under(f, 10.55, 0.45, 0.3)
    if front and f.w >= 6:
        K.buckle(f, 4.0, 9.65, 2.0, 1.4, BUCKLE)


def body_front(f, ov):
    front_coat(f)
    tails(f, True)
    belt(f, True)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.35, bottom=0.78, round_=0.08, fold_xs=[1.8, 6.2], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    f.fill(f.m_line((4.0, 1.8), (4.0, 9.0), 0.2, 0.2), COAT_D, 0.7)
    K.seam(f, [(4.0, 1.9), (4.0, 9.0)], GOLD_L, 0.05, 0.5)
    K.vband(f, 0.0, 0.6, 1.4, 9.15, GOLD, 0.15, 0.3)
    K.vband(f, 7.4, 8.0, 1.4, 9.15, GOLD, 0.15, 0.3)
    collar(f, False)
    tails(f, False)
    belt(f, False)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), COAT, top=1.35, bottom=0.78, round_=0.1, fold_xs=[1.4, 2.7], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    front_x0 = 3.0 if name == "west" else 0.0                                          # the gold border at the front end of the side
    K.vband(f, front_x0, front_x0 + 1.0, 1.4, 9.15, GOLD, 0.2, 0.4)
    K.seam(f, [(1.5 if name == "east" else 2.5, 2.0), (1.5 if name == "east" else 2.5, 8.9)], H.tone(COAT, 1.6), 0.05, 0.4)
    collar(f, False, name)
    tails(f, False)
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


# ================================================================================================ the arms: navy sleeves, steel bracers, white gloves
def bracer(f, y0, y1):
    """A steel bracer from y0 to y1: a bevelled plate with a dark outline, a centre ridge, gold bands at both ends and rivets."""
    m = f.m_rect(0.0, y0, f.w, y1, 0.12)
    f.fill(f.m_rect(-0.1, y0 - 0.1, f.w + 0.1, y1 + 0.1, 0.14), H.tone(STEEL, 0.32), 0.85)
    f.fill(m, H.lin((0, 0), (f.w, 0), [(0, K.lit(STEEL, 1.3)), (0.45, STEEL), (1, H.tone(STEEL, 0.62))]))
    f.fill(m, H.metal_paint(y0, y1, STEEL, 0.55), 0.55)
    H.bevel(f, m, depth=0.3, gain=1.15, spec=0.6, spec_pow=14)
    ridge = f.m_line((f.w * 0.5, y0 + 0.6), (f.w * 0.5, y1 - 0.6), 0.34, 0.34)
    f.fill(ridge, H.tone(STEEL, 0.72), 0.6)
    f.fill(f.m_line((f.w * 0.5 - 0.1, y0 + 0.7), (f.w * 0.5 - 0.1, y1 - 0.7), 0.1, 0.1), "#f2f8ff", 0.5)
    for yy in (y0, y1 - 0.5):
        H.trim_band(f, yy, yy + 0.5, GOLD, spec=0.5, outline=0.5)
    for yy in (y0 + 1.1, y1 - 1.1):
        H.stud(f, f.w * 0.5, yy, 0.17, "#e9eef4", spec=0.8)
    return m


def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            sl = f.m_band(0.0, 6.0)
            K.cloth(f, sl, COAT, top=1.4, bottom=0.8, round_=0.14, fold_xs=[f.w * 0.5], fold_depth=0.08, fold_y0=1.5, fold_y1=5.6)
            f.shade(sl, 1.0 + 0.12 * K.blob(f, f.w * 0.4, 1.2, f.w * 0.7, 1.4), 0.9)
            H.sheen(f, sl, [f.w * 0.3], width=0.26, strength=0.2, tilt=0.0, y0=1.5, y1=5.6, color="#9aa6d0")
            for y, k in ((3.9, 0.5), (4.5, 0.35)):                                             # the creases at the elbow
                K.crease(f, [(0.0, y), (f.w * 0.4, y + 0.3), (f.w, y + 0.05)], COAT, 0.1, k, mask=sl)
            K.feather_pad(f, -0.1, f.w + 0.1, 0.0, 2.2, FUR, FUR_L, FUR_D, seed=11 + len(nm) + (part == "arm_l"))
            f.fill(f.m_rect(0.0, 5.2, f.w, 6.0), H.tone(COAT, 0.7), 0.9)                       # the dark gap under the bracer
            bracer(f, 5.7, 10.4)
            H.shadow_under(f, 10.4, 0.45, 0.32)
            K.glove(f, SHIRT, 10.4, 12.0, nm)


# ================================================================================================ the legs: dark trousers, knee boots with gold cuffs
def leg(f, ov, name):
    K.trousers(f, TROUS, 0.0, 4.4, name, knee=False, fold_xs=(f.w * 0.5,), top=1.4, bottom=0.8, seam_color="#4a527a")
    f.shade(f.m_band(0.0, 4.4), 1.0 + 0.14 * K.blob(f, f.w * 0.45, 1.8, f.w * 0.7, 2.0), 0.9)
    H.shadow_under(f, 4.2, 0.8, 0.35)
    cuff = f.m_rect(0.0, 4.3, f.w, 5.55)                                                    # the turned boot cuff, gold-trimmed
    f.fill(cuff, H.lin((0, 4.3), (0, 5.55), [(0, K.lit(BOOT, 1.9)), (0.5, K.lit(BOOT, 1.35)), (1, H.tone(BOOT, 0.8))]))
    f.fill(f.m_rect(0.0, 4.3, f.w, 4.42), "#0c0806", 0.55)
    H.trim_band(f, 4.42, 4.82, GOLD, spec=0.5, outline=0.45)
    K.seam(f, [(0.12, 5.2), (f.w - 0.12, 5.2)], "#c0a07c", 0.06, 0.7, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 5.55, 11.25, name, strap="#2a1c16", gold=GOLD)


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

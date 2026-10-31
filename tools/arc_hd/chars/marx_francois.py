"""Marx Francois, the Wizard King's aide, HD 256x256 (docs/arc/hd_arc_bc3.md): the hand-painted look of the exact, long-suffering keeper of the Sanctuary's records.

What he wears is the 64x64 sheet's outfit (tools/arc/char_marx_francois.py): a deep-blue aide's coat with silver trim and a high collar over a white shirt, long sleeves with silver cuffs and white gloves, a
dark belt with a silver buckle, dark trousers in calf boots, and the short blue cape with the silver clover. The cape (`cape`, `cape_low`) and the ledger in his left hand (`prop_float`) are 3D extras
(`pmx_marx_francois`), so the back of the coat is kept plain. He has wide (4 px) arms, as the 64x64 skin had. The 3D hair is `bob` (the neat bowl cut of the sheet).

Drawn at 4x detail on both layers: sharp blue-violet anime eyes under flat thin brows, a level thin mouth and a faint stern line at the nose, a blunt bowl-cut fringe, the high collar with silver
piping over a white cravat with a silver pin, the coat opening in a V over a slate waistcoat with silver buttons, wide lapels piped in silver, a silver aiguillette cord on the right chest and two silver
clasps where the cape is pinned, the belt with its silver buckle, coat tails with a silver hem, long sleeves with silver-and-navy shoulder boards, turned silver cuffs with two buttons over white gloves with
knuckle seams, creased trousers and calf boots with silver-trimmed turned cuffs, straps and welts.

No glow map: the sheet has no glowing part on the skin (its glow colour is only the faint trim of the ledger).

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus docs/arc/npcb.md
section 10 and the header of tools/arc/char_marx_francois.py (which cite the wikis): a smaller man with a neat bowl-cut of dark hair, a serious face, and formal blue aide's robes with silver trim and
the clover on the short cape. The colours are the sheet's choice and are kept.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc3_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "bob"
HAIR = "#5a4232"
HAIR_RGB = (90, 66, 50)
SKIN = ("#e8c0a4", "#f6dcc8", "#ffeee2")
COAT = "#2a3a6a"
COAT_D = "#141c38"
VEST, VEST_L = "#1f2a52", "#3a4a82"
SILVER, SILVER_L, SILVER_D = "#c8d4e8", "#f4f8ff", "#7a88a8"
SHIRT = "#eef0f6"
BELT = "#1e2438"
TROUS = "#242c48"
BOOT = "#2c3350"
GLOVE = "#eef0f6"
IRIS = ("#27306a", "#5a6aa8", "#a4b6ea")


# ================================================================================================ the head
def fringe(f, pal):
    """A neat bowl-cut fringe: a blunt line across the brow with small even steps, two sideburns that frame the face."""
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.9), (7.4, 2.7), (6.5, 3.0), (5.5, 2.7), (4.5, 3.0), (3.5, 2.7), (2.5, 3.0), (1.5, 2.7), (0.6, 3.0), (-0.3, 2.8)]
    K.hair_mass(f, pal, outline, 0.0, 2.9, shadow_k=0.88, shadow_depth=1.5, streaks=(0.8, 1.9, 3.0, 4.1, 5.2, 6.3, 7.3), streak_len=2.4)
    f.fill(f.m_rect(0.0, 2.55, f.w, 2.75), H.tone(pal["d"], 0.7), 0.5)
    for x, ln in ((0.45, 4.0), (7.55, 4.0)):
        K.lock(f, pal, x, 0.95, 1.0, ln, 0.0, 1.0)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.10, chin=0.08, jaw=0.11)
    pal = K.hair_palette(HAIR)
    fringe(f, pal)
    for cx in (2.4, 5.6):                                                                    # the faint bags of a man who keeps everyone's records
        K.eye_bags(f, cx, 5.7, 2.2, "#a07a7a", 0.3, 0.38)
    H.brow(f, (3.5, 3.9), (0.9, 3.6), "#3a2a22", 0.30, 0.12, arch=0.0)                      # flat, thin, stern brows
    H.brow(f, (4.5, 3.9), (7.1, 3.6), "#3a2a22", 0.30, 0.12, arch=0.0)
    K.eye_pair(f, IRIS, cx=(2.4, 5.6), cy=5.1, w=2.4, h=1.6, lash="#1a1420", tilt=0.2, lash_w=0.46, flick=0.36, lid_shadow=0.46, iris_r=1.02, hi=0.85, pupil="#0e1030")
    H.nose(f, 4.0, 6.3, "#cf9e88", 0.2)
    f.fill(f.m_polyline([(3.3, 4.3), (3.45, 5.3)], 0.12, soft=2.0), "#b8826e", 0.35)         # the stern line at the nose bridge
    f.fill(f.m_polyline([(4.7, 4.3), (4.55, 5.3)], 0.12, soft=2.0), "#b8826e", 0.35)
    H.mouth_smile(f, 4.0, 7.2, 1.8, color="#a05a58", lip="#d8a094", depth=0.0, thick=0.15)       # a level, thin mouth
    f.shade(f.m_ellipse(1.1, 6.4, 0.8, 1.1), 0.94, 0.5)
    f.shade(f.m_ellipse(6.9, 6.4, 0.8, 1.1), 0.94, 0.5)


def head_rest(cv):
    pal = K.hair_palette(HAIR)
    K.head_rear(cv, pal, back_y=6.2, nape_y=7.1, side_top=3.2, ear_x=4.1, side_low=6.1)
    for nm in ("west", "east"):
        K.ear_face(cv.face("head", nm, False), SKIN, nm, 4.6, 1.7)


# ================================================================================================ the body: a deep-blue coat open over a slate waistcoat
def collar(f, front):
    """The high navy collar with silver piping; on the front a white cravat knot with a silver pin sits in the opening."""
    if f.w < 6:
        band = f.m_rect(0.0, 0.0, f.w, 1.5)
        f.fill(band, H.lin((0, 0), (0, 1.5), [(0, K.lit(COAT, 1.5)), (1, H.tone(COAT, 0.7))]))
        f.fill(f.m_rect(0.0, 1.36, f.w, 1.58), SILVER, 0.95)
        f.fill(f.m_rect(0.0, 1.36, f.w, 1.44), SILVER_L, 0.8)
        return
    x0, x1 = 1.3, f.w - 1.3
    band = f.m_rect(x0, 0.0, x1, 1.55)
    f.fill(band, H.lin((0, 0), (0, 1.55), [(0, K.lit(COAT, 1.6)), (0.6, COAT), (1, H.tone(COAT, 0.7))]))
    f.fill(f.m_rect(x0, 1.36, x1, 1.62), SILVER)
    f.fill(f.m_rect(x0, 1.36, x1, 1.45), SILVER_L, 0.85)
    f.fill(f.m_rect(x0, 0.0, x0 + 0.18, 1.55), SILVER, 0.8)
    f.fill(f.m_rect(x1 - 0.18, 0.0, x1, 1.55), SILVER, 0.8)
    H.shadow_under(f, 1.62, 0.5, 0.3, x0=x0, x1=x1)
    if front:
        tri = f.m_poly([(2.9, 1.4), (5.1, 1.4), (4.0, 3.3)])
        f.fill(tri, H.lin((0, 1.4), (0, 3.3), [(0, "#f8f8fc"), (1, H.tone(SHIRT, 0.78))]))
        f.fill(f.m_polyline([(2.9, 1.45), (4.0, 3.2), (5.1, 1.45)], 0.12), H.tone(SHIRT, 0.55), 0.7)
        f.fill(f.m_line((4.0, 1.8), (4.0, 3.0), 0.12, 0.1), H.tone(SHIRT, 0.7), 0.5)
        H.gem(f, 4.0, 2.75, 0.3, "#8aa4e6", setting=SILVER, facets=4, spec=0.8, glow=False)


def front_coat(f):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.45, bottom=0.78, round_=0.08, fold_xs=[1.5, 6.5], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    H.sheen(f, m, [1.4, 6.6], width=0.3, strength=0.22, tilt=0.03, y0=2.0, y1=8.6, color="#8a9ccc")
    # the V opening: the slate waistcoat shows between the two lapels, wide at the collar and narrow at the belt
    vee = f.m_poly([(2.15, 1.5), (5.85, 1.5), (4.55, 9.15), (3.45, 9.15)])
    f.fill(vee, H.lin((0, 1.5), (0, 9.15), [(0, VEST_L), (0.45, VEST), (1, H.tone(VEST, 0.6))]))
    f.fill(f.m_line((4.0, 3.4), (4.0, 9.1), 0.14, 0.14) * vee, H.tone(VEST, 0.5), 0.7)
    for y in (4.1, 5.4, 6.7, 8.0):
        H.stud(f, 4.0, y, 0.24, SILVER_L, spec=0.9)
    f.fill(f.m_polyline([(2.15, 1.6), (3.45, 9.1)], 0.3) * f.m_all(), SILVER, 0.95)           # the silver piping of the lapel edges
    f.fill(f.m_polyline([(5.85, 1.6), (4.55, 9.1)], 0.3) * f.m_all(), SILVER, 0.95)
    f.fill(f.m_polyline([(2.2, 1.6), (3.5, 9.1)], 0.08) * f.m_all(), SILVER_L, 0.7)
    # the lapel facing: a lighter navy strip next to the piping
    f.fill(f.m_poly([(1.5, 1.6), (2.1, 1.6), (3.35, 9.1), (2.7, 9.1)]), K.lit(COAT, 1.22), 0.5)
    f.fill(f.m_poly([(6.5, 1.6), (5.9, 1.6), (4.65, 9.1), (5.3, 9.1)]), K.lit(COAT, 1.22), 0.5)
    # the cape clasps on both shoulders: a silver disc with a small clover dent
    for cx in (0.85, 7.15):
        f.fill(f.m_ellipse(cx, 2.1, 0.55, 0.55), H.tone(SILVER, 0.35), 0.9)
        H.stud(f, cx, 2.1, 0.46, SILVER, spec=0.95)
        H.stud(f, cx, 2.1, 0.16, SILVER_D, spec=0.2)
    # the aiguillette: two silver cords looped from the right shoulder (the viewer's left) with tips
    for off, a in ((0.0, 0.95), (0.4, 0.8)):
        pts = [(0.85, 2.7), (1.1 + off, 3.9), (2.0 + off, 4.8), (2.9 - off * 0.3, 4.4), (2.75 - off * 0.3, 3.6)]
        f.fill(f.m_polyline(pts, 0.17 + 0.0 * off) * f.m_all(), H.tone(SILVER, 0.5), 0.6)
        f.fill(f.m_polyline(pts, 0.13) * f.m_all(), SILVER, a)
    for x, y in ((2.75, 3.6), (2.55, 3.85)):
        H.stud(f, x, y, 0.15, SILVER_L, spec=0.8)
    collar(f, True)


def tails(f, front):
    """The coat tails below the belt (y 10.5..12): dark blue with a silver hem line and a centre slit."""
    m = f.m_rect(0.0, 10.5, f.w, 12.0)
    f.fill(m, H.lin((0, 10.5), (0, 12.0), [(0, H.tone(COAT, 0.75)), (1, H.tone(COAT, 0.5))]))
    f.fill(f.m_rect(0.0, 11.7, f.w, 11.95), SILVER, 0.95)
    f.fill(f.m_rect(0.0, 11.7, f.w, 11.8), SILVER_L, 0.8)
    if f.w >= 6:
        f.fill(f.m_line((4.0, 10.6), (4.0, 11.7), 0.14, 0.14), COAT_D, 0.9)


def belt(f, front):
    K.belt_band(f, 9.15, 10.15, BELT, stitch="#7a88a8")
    K.belt_band(f, 10.15, 10.55, H.tone(BELT, 0.65), stitch_a=0.0)
    H.shadow_under(f, 10.55, 0.45, 0.3)
    if front and f.w >= 6:
        K.buckle(f, 4.0, 9.65, 2.0, 1.4, SILVER, opening="#0e1220")


def body_back(f):
    K.cloth(f, f.m_all(), COAT, top=1.4, bottom=0.78, round_=0.08, fold_xs=[1.8, 6.2], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    f.fill(f.m_line((4.0, 1.8), (4.0, 9.0), 0.2, 0.2), COAT_D, 0.75)
    K.seam(f, [(4.0, 1.9), (4.0, 9.0)], SILVER, 0.05, 0.5)
    K.vband(f, 0.0, 0.5, 1.5, 9.1, SILVER, 0.1, 0.3)
    K.vband(f, 7.5, 8.0, 1.5, 9.1, SILVER, 0.1, 0.3)
    collar(f, False)


def body_side(f, name):
    K.cloth(f, f.m_all(), COAT, top=1.4, bottom=0.78, round_=0.1, fold_xs=[1.4, 2.7], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    front_x0 = 3.5 if name == "west" else 0.0
    K.vband(f, front_x0, front_x0 + 0.5, 1.5, 9.1, SILVER, 0.12, 0.35)
    K.seam(f, [(1.5 if name == "east" else 2.5, 2.0), (1.5 if name == "east" else 2.5, 8.9)], "#5a6a9a", 0.05, 0.45)
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


# ================================================================================================ the arms: blue sleeves, silver boards and cuffs, white gloves
def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            K.sleeve(f, COAT, 0.0, 9.9, sheen="#8a9ccc", elbow=4.3)
            K.epaulette(f, 0.0, f.w, 0.0, 1.3, "#33437a", "#5a6aa8", SILVER_L)                      # the shoulder board: navy with silver edges
            for i in range(int(f.w * 2)):                                                         # its short fringe
                x = 0.12 + i * 0.5
                if x < f.w - 0.05:
                    f.fill(f.m_line((x, 1.3), (x, 1.75), 0.1, 0.1), SILVER, 0.9)
            H.shadow_under(f, 1.78, 0.4, 0.25)
            K.cuff(f, 8.4, 10.4, SILVER_D, SILVER, "#f4f8ff")                                     # the turned silver cuff with two buttons
            f.fill(f.m_rect(0.0, 8.95, f.w, 9.85), H.lin((0, 8.95), (0, 9.85), [(0, K.lit(COAT, 1.2)), (1, H.tone(COAT, 0.8))]), 0.95)
            if nm in ("front", "back"):
                for x in (f.w * 0.3, f.w * 0.7):
                    H.stud(f, x, 9.4, 0.2, SILVER_L, spec=0.9)
            H.shadow_under(f, 10.4, 0.4, 0.3)
            K.glove(f, GLOVE, 10.4, 12.0, nm, shade=0.62)


# ================================================================================================ the legs
def leg(f, ov, name):
    K.trousers(f, TROUS, 0.0, 6.8, name, knee=True, fold_xs=(f.w * 0.5,), top=1.45, bottom=0.8, seam_color="#4a5a8a", knee_y=(3.3, 4.0))
    f.shade(f.m_band(0.0, 6.8), 1.0 + 0.12 * K.blob(f, f.w * 0.45, 2.0, f.w * 0.7, 2.2), 0.9)
    if name in ("front", "back"):                                                             # the pressed crease
        f.fill(f.m_line((f.w * 0.5, 0.4), (f.w * 0.5, 6.5), 0.1, 0.1), K.lit(TROUS, 1.9), 0.4)
    H.shadow_under(f, 6.7, 0.7, 0.38)
    cuff = f.m_rect(0.0, 6.6, f.w, 7.6)
    f.fill(cuff, H.lin((0, 6.6), (0, 7.6), [(0, K.lit(BOOT, 2.2)), (1, H.tone(BOOT, 0.7))]))
    f.fill(f.m_rect(0.0, 6.6, f.w, 6.72), "#06060a", 0.6)
    H.trim_band(f, 6.72, 7.1, SILVER, spec=0.5, outline=0.45)
    K.boot(f, BOOT, 7.6, 11.25, name, strap="#2c3042", gold=SILVER)


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

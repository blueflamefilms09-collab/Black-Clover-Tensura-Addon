"""Solid Silva, the sneering knight of the Silver Eagle, HD 256x256 (docs/arc/hd_arc_bc3.md): the hand-painted look of the second son of the Silva family.

What he wears is the 64x64 sheet's outfit (tools/arc/char_solid_silva.py): a dark-blue noble's uniform with silver trim and a high collar over a white shirt, long sleeves with silver cuffs and white gloves,
a grey belt with a silver buckle, dark trousers in knee-high boots, and the Silver Eagle's silver mantle with its wings. The mantle (`cape`, `cape_low`) and the water-blue floating grimoire
(`prop_float`) are 3D extras (`pmx_solid_silva`), so the back of the coat is kept plain. He has slim (3 px) arms, as the 64x64 skin had. The 3D hair is `anime_swept` (a neat side-swept cut).

Drawn at 4x detail on both layers: sharp half-lidded blue anime eyes with a cold highlight, thin brows with the right one arched, a crooked smirk, a swept fringe of angular silver-blue blades, the high
collar with silver piping over a white cravat with a sapphire pin, the closed front with wide silver plackets, a column of silver buttons and chevron stitching, the Silver Eagle crest (spread wings with a gem)
on the chest, the belt with its silver buckle, coat tails with a silver hem, long sleeves under layered silver feather pauldrons, silver cuffs over white gloves with knuckle seams, creased trousers and
knee boots with silver turned cuffs, a knee plate, straps and welts.

No glow map: the sheet's glow is only the water-blue light of the floating grimoire, which is a separate model.

Source (rule 10): no wiki page could be fetched in this environment (blackclover.fandom.com and ja.wikipedia.org refused the connection); the look is from memory of the series plus docs/arc/npcb.md
section 10 and the header of tools/arc/char_solid_silva.py (which cite the wikis): a neat, refined young man with pale silver-blue hair and blue eyes in a dark-blue noble's uniform with silver trim
and the Silver Eagle's silver mantle with its wings; the sheet says no source described his hair or uniform, so the colours are the sheet's choice and are kept.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc3_kit as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_swept"
HAIR = "#8fb4f4"
HAIR_RGB = (143, 180, 244)
SKIN = ("#f0cdb8", "#fbe4d6", "#fff3ea")
COAT = "#3a4668"
COAT_D = "#1a2038"
SILVER, SILVER_L, SILVER_D = "#c8d4ec", "#f6f9ff", "#6c7aa0"
SHIRT = "#eef0f6"
BELT = "#3a3e4a"
TROUS = "#2a3048"
BOOT = "#303a58"
GLOVE = "#eef0f6"
IRIS = ("#1c3a7a", "#4a7ac8", "#a8d0ff")


# ================================================================================================ the head
def fringe(f, pal):
    """A swept silver-blue fringe: a mass at the crown, a parting at x 3.2, a long angular blade swept to the right over the brow and a shorter one to the left, sideburns."""
    outline = [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.7), (7.5, 2.3), (6.6, 1.8), (5.4, 1.3), (4.2, 1.4), (3.2, 1.0), (2.2, 1.7), (1.2, 2.1), (0.5, 2.4), (-0.3, 2.9)]
    K.hair_mass(f, pal, outline, 0.0, 2.6, shadow_k=0.9, shadow_depth=1.6, streaks=(0.9, 2.0, 3.2, 4.4, 5.6, 6.8), streak_len=2.2)
    blades = [
        ([(3.2, 0.6), (4.4, 1.0), (5.8, 1.8), (7.0, 2.9), (7.6, 4.0), (6.4, 3.3), (5.2, 2.8), (4.0, 2.3), (3.4, 1.6)], 0),
        ([(4.2, 1.2), (5.0, 1.9), (5.6, 3.0), (5.7, 3.5), (4.7, 3.0), (4.1, 2.2)], 1),
        ([(3.0, 0.7), (2.0, 1.1), (0.9, 1.9), (0.5, 3.0), (0.6, 4.2), (1.3, 3.0), (2.0, 2.4), (2.8, 1.8)], 0),
    ]
    for pts, tone_k in blades:
        m = f.m_poly(pts)
        c0 = pal["l"] if tone_k == 0 else pal["m"]
        f.fill(m, H.lin((pts[0][0], 0.5), (pts[4][0], pts[4][1]), [(0, c0), (0.55, pal["m"]), (1, H.mix(pal["m"], pal["d"], 0.35))]))
        f.fill(f.m_polyline(pts[1:5], 0.14) * m, pal["h"], 0.55)
        f.fill(f.m_polyline(pts[5:] + [pts[0]], 0.14) * m, pal["d"], 0.8)
    for x, ln in ((0.45, 4.1), (7.55, 4.0)):
        K.lock(f, pal, x, 0.95, 1.0, ln, 0.0, 1.0)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    K.face_base(f, SKIN, side=0.09, chin=0.07, jaw=0.10)
    pal = K.hair_palette(HAIR)
    fringe(f, pal)
    H.brow(f, (3.5, 3.55), (0.9, 3.25), "#7a8aa8", 0.26, 0.10, arch=0.0)                    # the left brow is flat and thin
    H.brow(f, (4.5, 3.6), (7.1, 3.0), "#7a8aa8", 0.26, 0.10, arch=0.18)                    # the right one is arched: the sneer
    K.eye_pair(f, IRIS, cx=(2.4, 5.6), cy=5.1, w=2.4, h=1.55, lash="#1a1c34", tilt=0.3, lash_w=0.44, flick=0.4, lid_shadow=0.6, iris_r=1.02, hi=0.85, pupil="#0c1a44")
    H.nose(f, 4.0, 6.3, "#d8a894", 0.18)
    # the smirk: a thin line that rises on the viewer's right with a small dimple and a lip under it
    f.fill(f.m_polyline([(3.15, 7.35), (4.1, 7.4), (4.9, 7.2), (5.5, 6.85)], 0.15), "#a0605e", 0.95)
    f.fill(f.m_polyline([(3.5, 7.62), (4.5, 7.6)], 0.16, soft=2.0), "#e6a8a0", 0.6)
    f.fill(f.m_line((5.6, 6.7), (5.75, 7.05), 0.1, 0.1), "#c88a80", 0.55)
    f.shade(f.m_ellipse(1.1, 6.4, 0.8, 1.1), 0.95, 0.5)
    f.shade(f.m_ellipse(6.9, 6.4, 0.8, 1.1), 0.95, 0.5)


def head_rest(cv):
    pal = K.hair_palette(HAIR)
    K.head_rear(cv, pal, back_y=6.0, nape_y=7.0, side_top=3.1, ear_x=4.1, side_low=5.9)
    for nm in ("west", "east"):
        K.ear_face(cv.face("head", nm, False), SKIN, nm, 4.6, 1.7)


# ================================================================================================ the body: a dark-blue uniform with silver plackets and the eagle crest
def collar(f, front):
    """The high collar with silver piping; on the front a white cravat knot with a sapphire pin."""
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
        tri = f.m_poly([(3.0, 1.4), (5.0, 1.4), (4.0, 3.2)])
        f.fill(tri, H.lin((0, 1.4), (0, 3.2), [(0, "#fafaff"), (1, H.tone(SHIRT, 0.78))]))
        f.fill(f.m_polyline([(3.0, 1.45), (4.0, 3.1), (5.0, 1.45)], 0.12), H.tone(SHIRT, 0.55), 0.7)
        H.gem(f, 4.0, 2.65, 0.3, "#4a8ae8", setting=SILVER, facets=4, spec=0.85, glow=False)


def crest(f, cx, cy, k=1.0):
    """The Silver Eagle crest: two spread wings of three thick tapered feathers each (fanned up and out) round a sapphire, in silver with a dark outline."""
    for s in (-1.0, 1.0):
        for i in range(3):
            x0, y0 = cx + s * 0.3 * k, cy + 0.1 + 0.42 * i
            x1, y1 = cx + s * (2.0 - 0.3 * i) * k, cy - 1.0 + 0.62 * i
            m = f.m_line((x0, y0), (x1, y1), 0.42, 0.14)
            f.fill(f.m_line((x0, y0 + 0.04), (x1, y1 + 0.04), 0.54, 0.22), H.tone(SILVER_D, 0.4), 0.9)
            f.fill(m, H.lin((cx - 1.0, cy - 1.0), (cx + 1.0, cy + 1.0), [(0, SILVER_L), (0.5, SILVER), (1, SILVER_D)]))
            f.fill(f.m_line((x0, y0 - 0.1), (x1, y1 - 0.1), 0.12, 0.05) * m, "#ffffff", 0.7)
    H.gem(f, cx, cy + 0.15, 0.36, "#4a8ae8", setting=SILVER, facets=4, spec=0.9, glow=False)


def front_coat(f):
    m = f.m_all()
    K.cloth(f, m, COAT, top=1.45, bottom=0.78, round_=0.08, fold_xs=[1.7, 6.3], fold_depth=0.08, fold_y0=2.0, fold_y1=8.8)
    H.sheen(f, m, [1.5, 6.5], width=0.3, strength=0.24, tilt=0.03, y0=2.0, y1=8.6, color="#9aacdc")
    # the closed front: a centre seam, the silver plackets on both sides, one column of buttons
    f.fill(f.m_line((4.0, 1.9), (4.0, 9.05), 0.2, 0.2), COAT_D, 0.85)
    K.vband(f, 3.3, 3.7, 1.7, 9.1, SILVER, 0.12, 0.45)
    K.vband(f, 4.3, 4.7, 1.7, 9.1, SILVER, 0.12, 0.45)
    for y in (2.5, 3.9, 5.3, 6.7, 8.1):
        H.stud(f, 4.0, y, 0.21, SILVER_L, spec=0.95)
    # the shoulder lines and the side borders of the uniform
    K.vband(f, 0.0, 0.45, 1.5, 9.1, SILVER, 0.12, 0.3)
    K.vband(f, 7.55, 8.0, 1.5, 9.1, SILVER, 0.12, 0.3)
    # chevron stitching under the plackets
    for y in (3.2, 4.6, 6.0, 7.4):
        f.fill(f.m_polyline([(2.2, y - 0.25), (2.7, y + 0.2), (3.0, y - 0.25)], 0.07), SILVER_D, 0.7)
        f.fill(f.m_polyline([(5.0, y - 0.25), (5.3, y + 0.2), (5.8, y - 0.25)], 0.07), SILVER_D, 0.7)
    crest(f, 6.1, 4.1, 0.85)                                                                      # the Silver Eagle crest on the left chest (the viewer's right)
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
    K.belt_band(f, 9.15, 10.15, BELT, stitch="#8a98b8")
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
    K.seam(f, [(1.5 if name == "east" else 2.5, 2.0), (1.5 if name == "east" else 2.5, 8.9)], "#6a7aaa", 0.05, 0.45)
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


# ================================================================================================ the arms: blue sleeves under silver feather pauldrons, silver cuffs, white gloves
def arm(cv, part):
    for nm in SIDES:
        for f, ov in zip(A.lay(cv, part, nm), (False, True)):
            K.sleeve(f, COAT, 0.0, 9.9, sheen="#9aacdc", elbow=4.3)
            K.feather_pad(f, -0.1, f.w + 0.1, 0.0, 2.5, "#aebcd8", "#f4f8ff", "#6c7aa0", seed=21 + len(nm) + (part == "arm_l"))   # the layered silver pauldron
            f.fill(f.m_rect(0.0, 2.3, f.w, 2.5), SILVER_D, 0.55)
            H.shadow_under(f, 2.55, 0.45, 0.28)
            if nm in ("front", "back"):                                                      # a silver stripe down the sleeve
                K.vband(f, f.w * 0.5 - 0.15, f.w * 0.5 + 0.15, 2.6, 8.2, SILVER, 0.08, 0.3, 0.3)
            K.cuff(f, 8.4, 10.4, SILVER_D, SILVER, "#f4f8ff")                               # the turned silver cuff with two buttons
            f.fill(f.m_rect(0.0, 8.95, f.w, 9.85), H.lin((0, 8.95), (0, 9.85), [(0, K.lit(COAT, 1.2)), (1, H.tone(COAT, 0.8))]), 0.95)
            if nm in ("front", "back"):
                H.stud(f, f.w * 0.5, 9.4, 0.2, SILVER_L, spec=0.9)
            H.shadow_under(f, 10.4, 0.4, 0.3)
            K.glove(f, GLOVE, 10.4, 12.0, nm, shade=0.62)


# ================================================================================================ the legs: trousers and knee boots with silver cuffs
def leg(f, ov, name):
    K.trousers(f, TROUS, 0.0, 4.4, name, knee=False, fold_xs=(f.w * 0.5,), top=1.4, bottom=0.8, seam_color="#4a5a8a")
    f.shade(f.m_band(0.0, 4.4), 1.0 + 0.14 * K.blob(f, f.w * 0.45, 1.8, f.w * 0.7, 2.0), 0.9)
    if name in ("front", "back"):
        f.fill(f.m_line((f.w * 0.5, 0.4), (f.w * 0.5, 4.1), 0.1, 0.1), K.lit(TROUS, 1.9), 0.4)
    H.shadow_under(f, 4.2, 0.8, 0.35)
    cuff = f.m_rect(0.0, 4.3, f.w, 5.55)                                                     # the turned boot cuff, silver-trimmed
    f.fill(cuff, H.lin((0, 4.3), (0, 5.55), [(0, K.lit(BOOT, 2.2)), (0.5, K.lit(BOOT, 1.4)), (1, H.tone(BOOT, 0.8))]))
    f.fill(f.m_rect(0.0, 4.3, f.w, 4.42), "#06060a", 0.55)
    H.trim_band(f, 4.42, 4.82, SILVER, spec=0.5, outline=0.45)
    K.seam(f, [(0.12, 5.2), (f.w - 0.12, 5.2)], SILVER_D, 0.06, 0.7, (0.28, 0.18), 0.2)
    K.boot(f, BOOT, 5.55, 11.25, name, strap="#2c3248", gold=SILVER)
    if name == "front":                                                                      # the knee plate
        mk = f.m_rect(f.w * 0.5 - 1.0, 3.5, f.w * 0.5 + 1.0, 4.35, 0.3)
        K.plate(f, mk, SILVER, light=1.15, spec=0.6, bevel=0.22)
        K.edge_line(f, mk, H.tone(SILVER_D, 0.5), 0.8, 1)


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

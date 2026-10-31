"""Mereoleona Vermillion, the strongest Vermillion, HD 256x256 (docs/arc/hd_arc_bc4.md).

What she wears is the 64x64 sheet's outfit (tools/arc/char_mereoleona_vermillion.py): a sleeveless dark-red top with gold trim that shows her arms, wrapped fists, a double belt with a gold
buckle, black trousers and knee-high dark boots. The long red cape with the gold flame and the floating grimoire are the `pmx_mereoleona_vermillion` extra (the cape hangs from the shoulders and
covers the back and the back of the legs down to the shin, so the back of the top is kept plain; nothing is painted where the grimoire floats). No glow on the skin (the flame glow is the
grimoire's). The sheet's second layer covers only the belt rows, the wraps and the legs, so the top and the bare arms live on the base layer.
Drawn at 4x detail on both layers: the fierce blue eyes with heavy slanted brows, the open grin with one big canine, the wild locks of the fringe; the tank top with a scoop neckline, gold
trim and a laced front, the belts with the gold buckle; the muscular tan arms (shoulder cap, biceps, triceps and forearm shading), the wraps with their diagonal turns, the knuckle roll and the
loose end; the trousers with creases, the boots with their turned cuffs, straps and gold buckles.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus docs/arc/npcb.md: a tall, broad,
tanned and muscular woman with a long wild vermilion mane, blue eyes, a sharp canine tooth and a long red cape. The laced front and the muscle shading are added in the HD pass.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc4 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_mane"                                      # the wild red lion's mane (kept from the baseline: big, wild, spiky)
HAIR = "#d4421c"
HAIR_RGB = (212, 66, 28)
SKIN = ("#be845c", "#d8a072", "#ecbc8a")                      # tan
TANK = "#7a1d17"
TANK_D = "#4c100e"
GOLD = "#e6b44a"
WRAP = "#e9dcbc"
TROUS = "#2c2424"
BOOT = "#5a3026"
BELT = "#3e2a22"
IRIS = ("#1c3a8c", "#4a88e4", "#c2e0ff")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.10, chin=0.07)
    pal = K.hair_pal(HAIR, light_k=1.18, dark_k=0.5)
    f.fill(f.m_ellipse(1.5, 6.2, 1.0, 0.6, soft=2.5), "#e0908a", 0.22)                                       # a warm flush of the cheeks
    f.fill(f.m_ellipse(6.5, 6.2, 1.0, 0.6, soft=2.5), "#e0908a", 0.22)
    # thick, steeply slanted brows: a fighter's scowl above a grin
    brow = H.mix(HAIR, "#240a06", 0.5)
    H.brow(f, (3.6, 4.55), (0.9, 3.7), brow, 0.5, 0.2, arch=0.0)
    H.brow(f, (4.4, 4.55), (7.1, 3.7), brow, 0.5, 0.2, arch=0.0)
    # sharp blue eyes: wide, the outer corners pulled up
    for cx in (2.35, 5.65):
        H.eye(f, cx, 5.3, 2.7, 1.85, IRIS, lash="#1c0c08", tilt=0.34, lash_w=0.52, flick=0.46, lid_shadow=0.4, hi=0.9)
    H.nose(f, 4.0, 6.35, "#b97c58", 0.24)
    # the open grin with the big canine
    H.mouth_smile(f, 4.0, 6.85, 4.0, color="#6a2420", lip="#c4705c", depth=0.3, thick=0.2, teeth="#fffdf6", open_h=0.66)
    f.poly([(2.75, 7.12), (3.2, 7.12), (2.98, 7.8)], "#ffffff", 0.95)                                        # the sharp canine tooth
    # the fringe: wild locks over the brows, long locks at the temples
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.5, 1.6), (6.7, 2.7), (5.7, 1.5), (4.5, 2.5), (3.4, 1.4), (2.2, 2.6), (1.1, 1.7), (-0.3, 2.4)], y_bot=2.5)
    K.strands(f, pal, (0.7, 1.9, 3.1, 4.3, 5.5, 6.7), 0.0, 2.0, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.85, (4.5, 1.7, 1.2, 2.3, -0.5)),
        (0.9, (6.7, 1.6, 1.2, 2.0, -0.7)),
        (0.9, (2.2, 1.6, 1.2, 2.1, 0.6)),
        (0.95, (3.4, 1.3, 1.0, 1.5, 0.3)),
        (0.95, (5.7, 1.3, 1.0, 1.4, -0.3)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.5, 1.3, 0.7, 5.0, 0.4, 1.0)
    K.lock(f, pal, 7.5, 1.3, 0.7, 5.0, -0.4, 1.0)


# ================================================================================================ the top
def laces(f, y0, y1, cx=4.0):
    """The lacing of the front of the top: a gold cord criss-crossing between small eyelets."""
    n = 4
    for i in range(n):
        ya, yb = y0 + (y1 - y0) * i / n, y0 + (y1 - y0) * (i + 1) / n
        K.line(f, [(cx - 0.75, ya), (cx + 0.75, yb)], GOLD, 0.2, 0.95)
        K.line(f, [(cx + 0.75, ya), (cx - 0.75, yb)], GOLD, 0.2, 0.95)
    for i in range(n + 1):
        y = y0 + (y1 - y0) * i / n
        for x in (cx - 0.8, cx + 0.8):
            H.stud(f, x, y, 0.15, GOLD, spec=0.8)


def body_front(f, ov):
    m = f.m_all()
    K.cloth(f, m, TANK, top=1.28, bottom=0.78, round_=0.13, fold_xs=[1.4, 6.6], fold_depth=0.10, fold_y0=2.5, fold_y1=8.8)
    K.sheen(f, f.m_rect(0, 2.0, 8, 8.8), [1.7, 6.3], 2.4, 8.4, 0.2, "#e49a84")
    # the scoop neckline: bare chest above it, a gold trim along its edge
    scoop = [(2.0, 0.0), (6.0, 0.0), (5.6, 1.1), (4.0, 2.3), (2.4, 1.1)]
    sm = f.m_poly(scoop)
    f.fill(sm, H.lin((0, 0), (0, 2.3), [(0, SKIN[2]), (0.6, SKIN[1]), (1, SKIN[0])]))
    f.fill(f.m_ellipse(4.0, 0.4, 2.1, 0.9, soft=2.5) * sm, H.tone(SKIN[0], 0.82), 0.45)                       # the hollow of the throat
    f.fill(f.m_line((4.0, 0.9), (4.0, 2.2), 0.2, 0.1) * sm, H.tone(SKIN[0], 0.75), 0.45)                       # the breastbone line
    H.trim_line(f, [(2.0, 0.0), (2.4, 1.1), (4.0, 2.3), (5.6, 1.1), (6.0, 0.0)], GOLD, width=0.3, spec=0.5, outline=0.6)
    laces(f, 2.5, 5.9)
    # the hem of the top above the belt
    f.fill(f.m_rect(0.0, 8.35, 8.0, 8.9), K.deep(TANK, 0.8), 0.7)
    K.seam(f, [(0.2, 8.15), (7.8, 8.15)], "#c99a50", 0.05, 0.6)
    # the double belt
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.75), H.tone(BELT, 0.8), 1.0)
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.3), K.lit(BELT, 1.6), 0.6)
    K.buckle(f, 4.0, 9.55, 2.4, 1.55, GOLD, hole="#1c0c0a")
    for x in (1.0, 7.0):
        H.stud(f, x, 9.5, 0.2, GOLD, spec=0.8)
    f.fill(f.m_rect(0.0, 10.75, f.w, 12.0), H.lin((0, 10.75), (0, 12.0), [(0, H.tone(TROUS, 1.1)), (1, H.tone(TROUS, 0.8))]))        # the trousers' waist under the belt


def body_back(f, ov):
    K.cloth(f, f.m_all(), TANK, top=1.22, bottom=0.78, round_=0.13, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.09, fold_y0=1.0, fold_y1=8.6)
    f.fill(f.m_line((4.0, 0.3), (4.0, 8.4), 0.2, 0.2), K.deep(TANK, 0.45), 0.6)
    H.trim_line(f, [(0.2, 0.6), (4.0, 1.9), (7.8, 0.6)], GOLD, width=0.28, spec=0.4, outline=0.55)             # the cut of the back
    f.fill(f.m_rect(0.0, 8.35, 8.0, 8.9), K.deep(TANK, 0.8), 0.7)
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.75), H.tone(BELT, 0.8), 1.0)
    H.stud(f, 4.0, 9.5, 0.2, GOLD, spec=0.7)
    f.fill(f.m_rect(0.0, 10.75, f.w, 12.0), H.lin((0, 10.75), (0, 12.0), [(0, H.tone(TROUS, 1.1)), (1, H.tone(TROUS, 0.8))]))


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), TANK, top=1.22, bottom=0.78, round_=0.13, fold_xs=[1.3, 2.7], fold_depth=0.09, fold_y0=1.0, fold_y1=8.6)
    f.fill(f.m_rect(0.0, 8.35, f.w, 8.9), K.deep(TANK, 0.8), 0.7)
    K.belt_band(f, 8.9, 10.15, BELT, stitch="#a8794a")
    f.fill(f.m_rect(0.0, 10.2, f.w, 10.75), H.tone(BELT, 0.8), 1.0)
    f.fill(f.m_rect(0.0, 10.75, f.w, 12.0), H.lin((0, 10.75), (0, 12.0), [(0, H.tone(TROUS, 1.1)), (1, H.tone(TROUS, 0.8))]))


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ the arms: bare, muscular, wrapped fists
def wrap_section(f, y0, y1, name):
    """Cream hand wraps from y0 to y1: diagonal turns with a dark gap between them, a lit edge on each turn, a roll at the top; the fist (the last two rows) is a thicker, tighter bundle."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(WRAP, 1.08)), (0.5, WRAP), (1, H.tone(WRAP, 0.82))]))
    per = 0.95
    ph = ((f.Y - 0.5 * (f.X - f.w / 2.0) - y0) % per) / per
    f.shade(m * np.clip((0.3 - ph * per) / 0.12, 0, 1), 0.62, 0.95)
    f.fill(m * np.clip(1.0 - np.abs(ph - 0.5) / 0.2, 0, 1), "#ffffff", 0.22)
    f.fill(f.m_rect(0.0, y0 - 0.02, f.w, y0 + 0.14), H.tone(WRAP, 0.42), 0.85)
    roll = f.m_band(y0 + 0.14, y0 + 0.66)
    f.fill(roll, H.lin((0, y0), (0, y0 + 0.66), [(0, H.tone(WRAP, 1.14)), (1, H.tone(WRAP, 0.78))]))
    f.fill(f.m_rect(0.0, y0 + 0.6, f.w, y0 + 0.72), H.tone(WRAP, 0.6), 0.75)
    H.shadow_under(f, y0 + 0.05, 0.6, 0.3)
    f.fill(f.m_band(y1 - 0.2, y1), H.tone(WRAP, 0.6), 0.6)
    fist = f.m_band(10.35, 11.55)
    f.fill(fist, H.lin((0, 10.35), (0, 11.55), [(0, H.tone(WRAP, 1.15)), (1, H.tone(WRAP, 0.85))]), 0.9)
    f.fill(f.m_rect(0.0, 10.3, f.w, 10.42), H.tone(WRAP, 0.5), 0.85)
    f.fill(f.m_rect(0.0, 11.5, f.w, 11.62), H.tone(WRAP, 0.5), 0.85)
    if name in ("front", "back"):                                                                  # the knuckles
        for x in (f.w * 0.2, f.w * 0.4, f.w * 0.6, f.w * 0.8):
            f.fill(f.m_ellipse(x, 10.6, 0.2, 0.15), "#fffaf0", 0.45)
    if name == "front":                                                                            # the loose end of the wrap hangs from the roll
        tail = f.m_poly([(0.7, y0 + 0.6), (1.7, y0 + 0.6), (1.6, y0 + 2.0), (1.2, y0 + 1.7), (0.85, y0 + 2.1)])
        f.fill(tail, H.lin((0, y0 + 0.6), (0, y0 + 2.1), [(0, H.tone(WRAP, 1.0)), (1, H.tone(WRAP, 0.78))]))
        f.fill(f.m_polyline([(0.7, y0 + 0.6), (0.85, y0 + 2.1), (1.2, y0 + 1.7), (1.6, y0 + 2.0), (1.7, y0 + 0.6)], 0.14), H.tone(WRAP, 0.45), 0.8)


def bare_arm(f, name, y1):
    """The bare arm from the shoulder to y1: skin with the roundness of a heavy arm, the shoulder cap, the biceps and triceps, a vein on the forearm."""
    m = f.m_band(0.0, y1)
    f.fill(m, H.lin((0, 0), (0, y1), [(0, SKIN[2]), (0.25, SKIN[1]), (0.75, H.tone(SKIN[1], 0.97)), (1, SKIN[0])]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.16 * (1.0 - H.smoothstep(0.0, 1.0, side)), 0.9)
    K.sheen(f, m, [f.w * 0.4], 0.5, y1 - 0.5, 0.14, "#fff0dc", width=0.5)
    # the deltoid cap and the bulge of the biceps (front) or triceps (back)
    f.fill(f.m_ellipse(f.w * 0.5, 0.9, f.w * 0.55, 1.3, soft=2.5) * m, K.lit(SKIN[2], 1.05), 0.35)
    if name in ("front", "back"):
        bump = f.m_ellipse(f.w * 0.46, 3.3, f.w * 0.3, 1.55, soft=2.5)
        f.fill(bump * m, K.lit(SKIN[2], 1.08), 0.38)
        K.muscle(f, m, SKIN, [(f.w * 0.78, 1.9), (f.w * 0.84, 3.2), (f.w * 0.76, 4.6)], [(f.w * 0.62, 1.9), (f.w * 0.66, 3.0)], 0.7, 0.45)
        f.fill(f.m_ellipse(f.w * 0.5, 5.2, f.w * 0.42, 0.45, soft=2.5) * m, H.tone(SKIN[0], 0.8), 0.45)        # the inner elbow
    else:
        K.muscle(f, m, SKIN, [(f.w * 0.3, 1.8), (f.w * 0.36, 3.0), (f.w * 0.3, 4.5)], None, 0.72, 0.4)
    f.fill(f.m_ellipse(f.w * 0.5, y1 + 0.2, f.w * 0.5, 0.6, soft=2.5) * m, H.tone(SKIN[0], 0.8), 0.35)
    if name in ("front", "back"):
        f.fill(f.m_line((f.w * 0.6, 5.6), (f.w * 0.56, 6.8), 0.14, 0.12) * m, H.tone(SKIN[0], 0.78), 0.4)       # a forearm vein


def arm(cv, part):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, part, nm), (False, True)):
            bare_arm(f, nm, 7.2)
            wrap_section(f, 6.9, 12.0, nm)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            f = cv.face(part, "top", ov)
            f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, SKIN[2]), (1, SKIN[1])]))


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 6.0, nm, knee=False, top=1.3, bottom=0.85, seam_color="#5a4a4a")
                H.shadow_under(f, 6.0, 0.8, 0.4)
                K.boot(f, BOOT, 5.6, nm, strap=True, strap_color="#8a5a40", gold=GOLD, cuff_color="#7a4030", cuff_h=1.2, welt="#8a5a40", sole="#0d0a0a")
                if nm == "front":
                    f.fill(f.m_rect(0.0, 8.2, f.w, 8.9), H.tone("#7a4a34", 1.0), 0.95)                      # a second strap lower down
                    f.fill(f.m_rect(0.0, 8.2, f.w, 8.32), K.lit("#7a4a34", 1.6), 0.8)
                    H.stud(f, f.w * 0.5, 8.55, 0.3, GOLD)


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

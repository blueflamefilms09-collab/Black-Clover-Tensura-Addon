"""Lotus Whomalt, the Smoke Magic general of the Diamond Kingdom, HD 256x256 (docs/arc/hd_arc_bc6.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_lotus_whomalt.py): a calf-long smoky violet-grey general's coat with a high collar and light lilac trim, a pale shirt and a violet
cravat in the opening, long sleeves with trimmed cuffs, a plain belt with a lilac buckle, dark trousers and calf boots; the Diamond Kingdom's diamond on the back of the coat. Slim, tall.
The smoke plumes on his shoulders, the wisp from the pipe and the low wisp behind the hem are the `pmx_lotus_whomalt` extra, and so is the open grimoire in the left hand: the shoulders are plain
boards under the plumes, the back hem is kept free, the left hand is a bare hand.
Glow: the sheet has none on the skin (the glow lives on the extra); the only warm light on him is the pipe's ember, which is added here as a few texels (the pipe itself is painted on the face, since
the geo extra only carries its smoke).
Drawn at 4x detail on both layers: half-lidded grey-violet eyes under heavy lids with dark rings, flat dark brows, stubble round the mouth and jaw, a pipe at the corner of the mouth with a
glowing ember, a fringe of unkempt dark locks with one grey streak (the cut is the 3D hair `anime_short`); the coat with lit panels, lilac piping along the opening, the hem and the cuffs, flared
collar flaps over a violet cravat, board shoulders with studs, side seams, a back vent and the lilac diamond; the shirt strip with its placket and buttons; the belt with a buckle; a bare slim hand;
trousers and boots with a lilac cuff.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); from memory of the series plus docs/arc/lotus_whomalt.md (which says the
sources give no clothes or colours): tall, slouching, tired, scarred chest (hidden by the coat), smoke. Every colour is the sheet's own, lifted.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc6 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "anime_short"
HAIR = "#4c4540"
HAIR_RGB = (76, 69, 64)
STREAK = "#9a949a"
SKIN = ("#e0b8a0", "#f0cfb8", "#fde6d2")
COAT, COAT_L, COAT_D = "#443d58", "#6a6086", "#26212f"
TRIM, TRIM_D, TRIM_L = "#aaa6bc", "#6a6682", "#e4e0f4"
SHIRT, SHIRT_D = "#cbc5d8", "#8c869e"
CRAVAT, CRAVAT_D = "#8e82ae", "#5a5078"
BELT = "#4c3c52"
TROUS = "#2e2a3a"
BOOT = "#322b32"
DIAMOND = "#cdc4e4"
PIPE, PIPE_D = "#6a4630", "#3a2418"
EMBER, EMBER_G = "#ff9a40", "#ff8a30"
IRIS = ("#403a56", "#8d86ac", "#d8d4ea")


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.08, chin=0.08)
    pal = K.hair_pal(HAIR, light_k=1.45, dark_k=0.58)
    # stubble round the mouth and the jaw (dots of the shadow tone; a hash picks them)
    for i in range(46):
        x = 1.1 + 5.8 * H.hsh(7, i, 1)
        y = 6.0 + 1.9 * H.hsh(7, i, 2)
        if (x - 4.0) ** 2 / 9.0 + (y - 7.4) ** 2 / 2.5 < 1.0 and abs(x - 4.0) > 0.0:
            f.fill(f.m_ellipse(x, y, 0.15, 0.13), "#6a5048", 0.34)
    f.fill(f.m_ellipse(4.0, 7.5, 2.7, 0.9, soft=3.0), "#8a6a5a", 0.18)
    # heavy flat brows
    brow = "#3a3534"
    H.brow(f, (3.5, 3.9), (1.0, 3.7), brow, 0.42, 0.18, arch=0.0)
    H.brow(f, (4.5, 3.9), (7.0, 3.7), brow, 0.42, 0.18, arch=0.0)
    # tired eyes: half lids, the iris low, dark bags under them
    K.eye_bag(f, 2.4, 6.0, 2.3, "#8a6a7a", 0.5)
    K.eye_bag(f, 5.6, 6.0, 2.3, "#8a6a7a", 0.5)
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.1, 2.5, 1.35, IRIS, lash="#1c1820", tilt=-0.08, lash_w=0.5, flick=0.26, lid_shadow=0.45, hi=0.7, look=(0.0, 0.1))
        f.fill(f.m_polyline([(cx - 1.15, 4.55), (cx, 4.4), (cx + 1.15, 4.6)], 0.14), "#6a5a60", 0.55)         # the crease of the heavy lid
    H.nose(f, 4.0, 6.2, "#c49480", 0.22)
    H.mouth_smile(f, 3.55, 7.3, 1.5, color="#8a5a5a", lip="#c48a86", depth=-0.03, thick=0.16)
    pipe(f)
    # the hair: an unkempt dark fringe falling over the brow, one grey streak, ragged sideburns
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.3), (7.3, 1.8), (6.3, 2.6), (5.2, 1.7), (4.1, 2.5), (3.0, 1.6), (1.9, 2.4), (0.9, 1.8), (-0.3, 2.3)], y_bot=2.4)
    K.strands(f, pal, (0.9, 2.1, 3.4, 4.7, 6.0, 7.1), 0.0, 1.9, mass)
    for sh, (cx, w, top, ln, lean) in ((0.9, (6.4, 1.5, 1.2, 2.2, -0.4)), (0.9, (1.6, 1.4, 1.2, 2.0, 0.4)), (1.0, (4.2, 1.2, 1.0, 1.7, 0.2)), (0.95, (2.9, 1.2, 1.0, 1.5, -0.2))):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    sp = {"d": H.col("#6a646a"), "m": H.col(STREAK), "l": H.col("#c4c0c8"), "h": H.col("#eceaf0")}
    K.lock(f, sp, 3.7, 1.0, 0.7, 2.7, 0.3, 1.0)                                                             # the grey streak
    K.lock(f, pal, 0.45, 1.1, 0.7, 4.6, 0.3, 1.0)
    K.lock(f, pal, 7.55, 1.1, 0.7, 4.6, -0.3, 1.0)


def pipe(f):
    """A small pipe at the right corner of the mouth (the viewer's right): a stem, a brown bowl with a rim and an ember that glows (the extra only carries its smoke, which rises from here)."""
    f.fill(f.m_line((4.5, 7.5), (5.5, 7.35), 0.44, 0.4), PIPE_D, 1.0)
    f.fill(f.m_line((4.5, 7.46), (5.5, 7.31), 0.26, 0.22), K.lit(PIPE, 1.3), 0.9)
    f.fill(f.m_rect(5.2, 6.55, 6.45, 7.95, r=0.4), PIPE_D, 1.0)
    bowl = f.m_rect(5.3, 6.65, 6.35, 7.85, r=0.35)
    f.fill(bowl, H.lin((5.3, 6.65), (6.35, 7.85), [(0, K.lit(PIPE, 1.55)), (0.5, PIPE), (1, H.tone(PIPE, 0.62))]))
    H.bevel(f, bowl, depth=0.3, gain=1.1, spec=0.3)
    f.fill(f.m_ellipse(5.82, 6.7, 0.5, 0.17), "#2a1810", 1.0)
    f.fill(f.m_ellipse(5.82, 6.68, 0.38, 0.12), EMBER, 1.0, glow=EMBER_G)
    f.fill(f.m_ellipse(5.82, 6.66, 0.2, 0.07), "#fff0b0", 0.9)


def head_sides(cv):
    pal = K.hair_pal(HAIR, light_k=1.4, dark_k=0.58)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 1.8, 1.0, 3.6, lean, 0.95)


# ================================================================================================ the coat
def collar_flaps(f, name):
    """The high collar: a stand collar round the neck with a lilac edge; on the front two flared flaps over the cravat."""
    m = f.m_band(0.0, 1.9)
    K.cloth(f, m, COAT, top=1.55, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.25, gain=1.15, spec=0.2)
    f.fill(f.m_rect(0.0, 1.78, f.w, 1.9), TRIM, 1.0)
    f.fill(f.m_rect(0.0, 1.78, f.w, 1.84), TRIM_L, 0.7)
    f.fill(f.m_rect(0.0, 1.9, f.w, 2.02), "#0c0a12", 0.55)
    H.shadow_under(f, 2.0, 0.5, 0.3)
    K.seam(f, [(0.15, 1.1), (f.w - 0.15, 1.1)], TRIM_D, 0.05, 0.5)


def body_front(f):
    # the coat everywhere, the opening cut out afterwards
    K.cloth(f, f.m_all(), COAT, top=1.7, bottom=0.78, round_=0.15, fold_xs=[0.9, 1.8, 6.2, 7.1], fold_depth=0.12, fold_y0=2.0, fold_y1=11.6)
    for x0, x1 in ((0.0, 3.0), (5.0, 8.0)):
        K.sheen(f, f.m_rect(x0, 2.0, x1, 11.8), [x0 + 0.9 if x0 < 4 else x1 - 0.9], 2.2, 11.2, 0.3, "#c8c0f0", width=0.28)
    f.fill(f.m_rect(0.0, 2.0, 0.28, 12.0), K.lit(COAT, 1.9), 0.4)
    f.fill(f.m_rect(7.72, 2.0, 8.0, 12.0), "#07060c", 0.4)
    # the opening: a shirt strip between the coat's front edges, lilac piping on both edges
    op = f.m_poly([(3.0, 1.9), (5.0, 1.9), (5.0, 12.2), (3.0, 12.2)])
    f.fill(op, H.lin((0, 1.9), (0, 12.0), [(0, K.lit(SHIRT, 1.1)), (0.7, SHIRT), (1, H.tone(SHIRT_D, 0.7))]))
    f.fill(f.m_rect(3.0, 1.9, 3.4, 12.0) * op, SHIRT_D, 0.35)                                                  # the coat's shadow on the shirt
    f.fill(f.m_rect(4.6, 1.9, 5.0, 12.0) * op, SHIRT_D, 0.25)
    for x in (3.0, 5.0):
        H.trim_line(f, [(x, 1.9), (x, 12.0)], TRIM, width=0.24, spec=0.4, outline=0.55)
    f.fill(f.m_line((4.0, 2.0), (4.0, 8.6), 0.14, 0.14), SHIRT_D, 0.5)                                         # the placket line
    for y in (3.3, 4.7, 6.1, 7.5):
        H.stud(f, 4.0, y, 0.2, "#f2eef8", spec=0.8)
    # the violet cravat
    f.fill(f.m_poly([(3.1, 0.2), (4.9, 0.2), (4.55, 1.6), (4.0, 2.7), (3.45, 1.6)]), H.lin((0, 0.2), (0, 2.7), [(0, K.lit(CRAVAT, 1.3)), (1, H.tone(CRAVAT, 0.7))]))
    f.fill(f.m_polyline([(3.1, 0.2), (3.45, 1.6), (4.0, 2.7), (4.55, 1.6), (4.9, 0.2)], 0.14), CRAVAT_D, 0.8)
    H.gem(f, 4.0, 1.5, 0.3, "#cdb4ff", setting=TRIM, facets=4, spec=0.8, glow=None)
    # the collar flaps
    collar_flaps(f, "front")
    f.fill(f.m_poly([(2.6, 0.0), (3.4, 0.0), (3.4, 2.05), (3.0, 2.05)]), K.lit(COAT, 1.25), 1.0)
    f.fill(f.m_poly([(5.4, 0.0), (4.6, 0.0), (4.6, 2.05), (5.0, 2.05)]), H.tone(COAT, 0.85), 1.0)
    for pts in ([(2.6, 0.0), (3.0, 2.05)], [(5.4, 0.0), (5.0, 2.05)]):
        H.trim_line(f, pts, TRIM, width=0.22, spec=0.4, outline=0.5)
    # the belt over the opening and a pair of coat belts at the sides
    K.belt_band(f, 8.7, 9.8, BELT, stitch=TRIM_D)
    K.buckle(f, 4.0, 9.25, 1.9, 1.3, TRIM, hole="#17131f")
    # the pockets of the coat and the shoulder seams
    K.seam(f, [(0.4, 10.4), (2.4, 10.7)], TRIM_D, 0.05, 0.6)
    K.seam(f, [(7.6, 10.4), (5.6, 10.7)], TRIM_D, 0.05, 0.6)
    K.seam(f, [(0.2, 2.6), (2.8, 2.3)], TRIM_D, 0.05, 0.5)
    K.seam(f, [(7.8, 2.6), (5.2, 2.3)], TRIM_D, 0.05, 0.5)
    hem(f, 10.3)


def hem(f, y0, y1=12.0):
    """The coat's trimmed hem band (only on the body when the coat stops here; on the legs the hem is lower)."""
    pass


def body_back(f):
    K.cloth(f, f.m_all(), COAT, top=1.6, bottom=0.78, round_=0.15, fold_xs=[1.4, 2.6, 5.4, 6.6], fold_depth=0.12, fold_y0=2.0, fold_y1=11.6)
    collar_flaps(f, "back")
    K.sheen(f, f.m_rect(0.0, 2.0, 8.0, 11.8), [1.5, 6.5], 2.2, 11.2, 0.22, "#c8c0f0", width=0.28)
    # the Diamond Kingdom's diamond (a lilac rhombus with an inner line)
    d = [(4.0, 2.55), (6.15, 5.35), (4.0, 8.15), (1.85, 5.35)]
    f.fill(f.m_poly(d), H.lin((0, 2.55), (0, 8.15), [(0, K.lit(DIAMOND, 1.12)), (1, H.tone(DIAMOND, 0.78))]))
    f.fill(f.m_polyline(d + [d[0]], 0.2), H.tone(DIAMOND, 0.55), 0.9)
    di = [(4.0, 3.5), (5.15, 5.35), (4.0, 7.2), (2.85, 5.35)]
    f.fill(f.m_polyline(di + [di[0]], 0.18), "#ffffff", 0.7)
    f.fill(f.m_poly([(4.0, 3.5), (5.15, 5.35), (4.0, 5.35)]), "#ffffff", 0.22)
    f.fill(f.m_line((4.0, 9.8), (4.0, 12.0), 0.2, 0.2), "#07060c", 0.65)                                       # the vent
    K.seam(f, [(3.7, 9.8), (3.7, 12.0)], TRIM_D, 0.05, 0.5)
    K.seam(f, [(4.3, 9.8), (4.3, 12.0)], TRIM_D, 0.05, 0.5)
    K.belt_band(f, 8.7, 9.8, BELT, stitch=TRIM_D)
    H.stud(f, 4.0, 9.25, 0.2, TRIM, spec=0.7)


def body_side(f, name):
    K.cloth(f, f.m_all(), COAT, top=1.6, bottom=0.78, round_=0.15, fold_xs=[1.3, 2.7], fold_depth=0.12, fold_y0=2.0, fold_y1=11.6)
    collar_flaps(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.1), (ed, 8.4)], TRIM_D, 0.05, 0.5)
    K.belt_band(f, 8.7, 9.8, BELT, stitch=TRIM_D)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f)
            elif nm == "back":
                body_back(f)
            else:
                body_side(f, nm)


# ================================================================================================ arms and legs
def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.4)
            K.cloth(f, sleeve, COAT, top=1.7, bottom=0.8, round_=0.18, fold_xs=[0.7, 1.8], fold_depth=0.12, fold_y0=1.8, fold_y1=7.8)
            K.sheen(f, sleeve, [f.w * 0.3], 1.8, 7.6, 0.3, "#c8c0f0", width=0.26)
            # the board shoulder: a lilac trimmed pad, two studs
            sb = f.m_band(0.0, 1.45)
            f.fill(sb, H.lin((0, 0), (0, 1.45), [(0, K.lit(COAT_L, 1.2)), (1, COAT)]))
            H.bevel(f, sb, depth=0.2, gain=1.15, spec=0.3)
            f.fill(f.m_rect(0.0, 1.38, f.w, 1.5), TRIM, 1.0)
            f.fill(f.m_rect(0.0, 1.38, f.w, 1.44), TRIM_L, 0.7)
            f.fill(f.m_rect(0.0, 1.5, f.w, 1.6), "#0c0a12", 0.5)
            for x in (0.6, f.w - 0.6):
                H.stud(f, x, 0.7, 0.15, TRIM_L, spec=0.9)
            for y, k in ((4.8, 0.55), (5.35, 0.35)):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.28), (f.w, y + 0.05)], 0.15) * sleeve, COAT_D, k)
            f.fill(f.m_polyline([(0.0, 4.6), (f.w * 0.5, 4.85), (f.w, 4.65)], 0.2) * sleeve, K.lit(COAT, 2.0), 0.3)
            if nm in ("west", "east"):
                f.fill(f.m_line((f.w * 0.5, 1.7), (f.w * 0.5, 7.2), 0.2, 0.2) * sleeve, TRIM_D, 0.5)
            K.cuff(f, 7.25, 8.5, TRIM, pipe=TRIM_L, stud_color=TRIM_L, spec=0.35)
            # the bare slim hand
            hand = f.m_band(8.5, 12.0)
            f.fill(hand, H.lin((0, 8.5), (0, 12.0), [(0, H.tone(SKIN[1], 0.9)), (0.5, SKIN[1]), (1, H.tone(SKIN[0], 0.95))]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(hand, 1.0 - 0.1 * (1.0 - H.smoothstep(0.0, 0.8, side)), 0.9)
            H.shadow_under(f, 8.55, 0.7, 0.3, mask=hand)
            if nm in ("front", "back"):
                for i in (1, 2):
                    x = f.w * i / 3
                    f.fill(f.m_line((x, 10.9), (x, 11.95), 0.2, 0.16), H.tone(SKIN[0], 0.72), 0.65)
                f.fill(f.m_line((f.w * 0.62, 9.0), (f.w * 0.6, 10.2), 0.14, 0.1), "#b88c9c", 0.35)
            f.fill(f.m_band(10.75, 10.9), H.tone(SKIN[0], 0.74), 0.5)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", COAT_L, edge=TRIM_D, k_top=1.3, overlay=ov)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 7.4, nm, knee=True, top=1.6, bottom=0.8, seam_color=TRIM_D)
                # the long coat skirt over the thigh, lilac hem
                sk = f.m_band(0.0, 6.0)
                f.fill(sk, H.lin((0, 0), (0, 6.0), [(0, K.lit(COAT, 1.35)), (1, H.tone(COAT, 0.7))]))
                H.folds(f, sk, [f.w * 0.25, f.w * 0.5, f.w * 0.75], depth=0.14, width=0.25, y0=0.0, y1=5.8)
                K.sheen(f, sk, [f.w * 0.3], 0.3, 5.5, 0.22, "#c8c0f0")
                if nm == "back":
                    f.fill(f.m_line((f.w * (0.0 if part == "leg_l" else 1.0), 0.0), (f.w * (0.0 if part == "leg_l" else 1.0), 6.0), 0.5, 0.5), "#07060c", 0.5)
                f.fill(f.m_rect(0.0, 5.45, f.w, 6.0), H.lin((0, 5.45), (0, 6.0), [(0, K.lit(TRIM, 1.15)), (1, H.tone(TRIM, 0.7))]))
                f.fill(f.m_rect(0.0, 5.4, f.w, 5.5), "#0c0a12", 0.55)
                f.fill(f.m_rect(0.0, 5.5, f.w, 5.58), TRIM_L, 0.6)
                H.shadow_under(f, 6.0, 0.7, 0.4)
                K.boot(f, BOOT, 7.3, nm, strap=True, strap_color=TRIM_D, gold=TRIM_L, cuff_color=TRIM_D, cuff_h=1.0, welt=TRIM_D, sole="#08060a")


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN)
    head_sides(cv)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    arm_tops(cv)
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)

"""Nacht Faust, the devil host of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc4.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_nacht_faust.py): a dark high-collared coat with lilac-grey trim down to the thigh, long sleeves, a plain belt with a lilac buckle, a dark
choker, dark trousers and calf boots. The long Black Bull cloak (its white bull glows violet) and the four violet relic gems on a cord at his chest are the `pmx_nacht_faust` extra (the cloak is
wide and ragged and covers the whole back and the arms from behind, the gems hang in front of the coat, so the front of the chest is kept free of any gem and the back of the coat is kept plain).
No glow on the skin (the glow is the gems and the bull, on the extra). Slim (3 px) arms. The sheet's second layer covers the whole coat, the sleeves and the legs.
Drawn at 4x detail on both layers: the cold pale-blue eyes with heavy lids and dark rings under them, thin straight brows, a flat mouth, a fringe that parts over the brow with two long side
locks (the high ponytail is the 3D hair); the coat with lit panels in a dark blue-black, a lilac front edge, a row of small buttons, a high stand collar with a trim line, the choker; the belt
with its lilac buckle, the hem line of the coat, bare pale hands, the trousers and the boots with their lilac cuffs and buckles. Dark cloth gets a lilac sheen and lit edges so it does not read
as one black block.

Source (rule 10): no wiki page could be fetched in this environment (English fandom and ja.wikipedia are blocked); the look is from memory of the series plus docs/arc/npcb.md: a slim, pale man
with a cold calm face, pale blue eyes, long black hair in a high ponytail and a dark high-collared coat under the Black Bull cloak. The cut of the coat and the lilac trim are the sheet's choice.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc4 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "high_ponytail"                                  # KEPT classic (hair_map): the high ponytail; no anime cut is a ponytail
HAIR = "#1c1c28"                                              # black lifted a little: the hair sheet is grey, the tint multiplies, pure black reads dead
HAIR_RGB = (28, 28, 40)
SKIN = ("#e2beba", "#f7dcd4", "#ffeee6")                      # pale
COAT = "#30303f"                                              # the sheet's #24242e, lifted so the folds read
COAT_L = "#4c4c64"
COAT_D = "#1c1c28"
TRIM = "#9a9ac2"                                              # lilac-grey (the sheet's #8e8eb0)
TRIM_D = "#5a5a80"
TROUS = "#262634"
BOOT = "#20202c"
BELT = "#2a2630"
CHOKER = "#2a2630"
IRIS = ("#3a5a86", "#92c0e6", "#e6f6ff")
SEAM = "#7a7aa4"
GEMS_ON_SKIN = True                                           # the four relic gems are the spec's extra, which the geo does not export: they are painted on the coat (and glow); False = plain coat
GEM = "#8a4ee0"
GEM_G = "#c8a0ff"
CORD = "#16121c"


# ================================================================================================ the head
def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.08, chin=0.07)
    pal = K.hair_pal(HAIR, light_k=1.5, dark_k=0.55)
    f.fill(f.m_ellipse(2.35, 5.6, 1.5, 0.55, soft=2.5), "#b6a0bc", 0.34)                              # dark rings under the eyes: a tired, cold face
    f.fill(f.m_ellipse(5.65, 5.6, 1.5, 0.55, soft=2.5), "#b6a0bc", 0.34)
    # thin straight brows, a little slanted: no warmth
    brow = "#2c2c3c"
    H.brow(f, (3.5, 4.35), (1.1, 4.05), brow, 0.3, 0.14, arch=0.02)
    H.brow(f, (4.5, 4.35), (6.9, 4.05), brow, 0.3, 0.14, arch=0.02)
    # pale blue eyes, heavy lids: half closed, the outer corner low
    for cx in (2.4, 5.6):
        H.eye(f, cx, 5.2, 2.5, 1.5, IRIS, lash="#12121c", tilt=0.08, lash_w=0.5, flick=0.34, lid_shadow=0.52, hi=0.8)
        f.fill(f.m_polyline([(cx - 1.1, 5.95), (cx, 6.1), (cx + 1.1, 5.95)], 0.12), "#a48ca8", 0.45)
    H.nose(f, 4.0, 6.3, "#d0aaa6", 0.2)
    H.mouth_smile(f, 4.0, 7.2, 1.7, color="#a0686e", lip="#d498a0", depth=-0.05, thick=0.16)
    # the hair: black, parted at the left of the centre, a fringe that falls over the brow, two long side locks that frame the face (the ponytail is the 3D hair)
    mass = K.fringe_mass(f, pal, [(-0.3, -0.3), (8.3, -0.3), (8.3, 2.5), (7.2, 2.0), (6.0, 2.8), (4.9, 1.6), (3.6, 2.6), (2.5, 1.7), (1.4, 2.4), (0.6, 1.9), (-0.3, 2.4)], y_bot=2.6)
    K.strands(f, pal, (0.8, 2.0, 3.3, 4.6, 5.9, 7.1), 0.0, 2.0, mass)
    for sh, (cx, w, top, ln, lean) in (
        (0.9, (3.6, 1.7, 1.2, 2.3, 0.5)),
        (0.9, (6.0, 1.7, 1.2, 2.2, -0.5)),
        (0.95, (4.9, 1.2, 1.0, 1.3, 0.0)),
        (0.95, (2.2, 1.4, 1.0, 1.5, 0.3)),
    ):
        K.lock(f, pal, cx, w, top, ln, lean, sh)
    K.lock(f, pal, 0.45, 1.1, 0.7, 5.4, 0.3, 1.0)
    K.lock(f, pal, 7.55, 1.1, 0.7, 5.4, -0.3, 1.0)


def head_sides(cv):
    """The long side locks of his hair continue on the sides of the head: a lock in front of the ear down to the jaw (the ears stay free), so the temple is not bare skin under the ponytail."""
    pal = K.hair_pal(HAIR, light_k=1.5, dark_k=0.55)
    # the back of the head and the back half of the sides: the ponytail's hair covers the head down to the nape (the sheet leaves it skin under a hat of hair)
    back = {"back": [(-0.3, -0.3), (8.3, -0.3), (8.3, 6.0), (6.4, 6.8), (4.0, 6.3), (1.6, 6.8), (-0.3, 6.0)],
            "west": [(-0.3, -0.3), (2.7, -0.3), (2.7, 5.0), (1.6, 5.9), (-0.3, 6.2)],
            "east": [(8.3, -0.3), (5.3, -0.3), (5.3, 5.0), (6.4, 5.9), (8.3, 6.2)]}
    for nm, poly in back.items():
        f = cv.face("head", nm, False)
        m = f.m_poly(poly)
        f.fill(m, H.lin((0, 0), (0, 7.0), [(0, H.mix(pal["m"], pal["d"], 0.2)), (0.5, pal["m"]), (1, H.tone(pal["m"], 0.72))]))
        for x in (0.9, 2.2, 3.6, 5.0, 6.4, 7.5):
            f.fill(f.m_line((x, 0.4), (x + 0.15, 6.4), 0.2, 0.12) * m, pal["d"], 0.4)
        f.fill(f.m_line((1.4, 0.8), (1.55, 5.6), 0.18, 0.1) * m, pal["h"], 0.3)
        f.fill(f.m_line((5.6, 0.8), (5.7, 5.6), 0.18, 0.1) * m, pal["h"], 0.3)
    for nm, cx, lean in (("west", 7.0, 0.3), ("east", 1.0, -0.3)):
        f = cv.face("head", nm, False)
        K.lock(f, pal, cx, 2.0, 1.2, 5.2, lean, 0.92)
        K.lock(f, pal, cx + (0.9 if nm == "east" else -0.9), 1.0, 1.4, 4.2, lean, 1.0)


# ================================================================================================ the coat
def lilac_edge(f, x, y0, y1, w=0.3):
    H.trim_line(f, [(x, y0), (x, y1)], TRIM, width=w, spec=0.45, outline=0.55)


def collar(f, name):
    m = f.m_band(0.0, 1.75)
    K.cloth(f, m, COAT, top=1.55, bottom=0.8, round_=0.1)
    H.bevel(f, m, depth=0.25, gain=1.2, spec=0.3)
    f.fill(f.m_rect(0.0, 1.58, f.w, 1.76), TRIM, 1.0)
    f.fill(f.m_rect(0.0, 1.58, f.w, 1.64), K.lit(TRIM, 1.45), 0.8)
    f.fill(f.m_rect(0.0, 1.76, f.w, 1.88), "#0a0a12", 0.6)
    H.shadow_under(f, 1.78, 0.5, 0.3)
    # the choker: a dark band round the neck at the top of the collar
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.5), H.tone(CHOKER, 0.9), 1.0)
    f.fill(f.m_rect(0.0, 0.48, f.w, 0.58), "#08080e", 0.7)
    f.fill(f.m_rect(0.0, 0.0, f.w, 0.1), K.lit(CHOKER, 1.8), 0.5)
    K.seam(f, [(0.15, 1.15), (f.w - 0.15, 1.15)], SEAM, 0.05, 0.55)
    if name == "front":
        f.fill(f.m_polyline([(3.55, 0.6), (4.0, 1.5), (4.45, 0.6)], 0.14), "#08080e", 0.7)


def relic_gems(f):
    """The cord with the four violet relic gems on the chest: a necklace that hangs from the collar, the two inner stones larger, each stone faceted with a bezel and a light on the glow map."""
    cord = [(0.0, 2.0), (0.6, 2.6), (1.2, 3.05), (2.85, 3.85), (4.0, 4.2), (5.15, 3.85), (6.8, 3.05), (7.4, 2.6), (8.0, 2.0)]
    f.fill(f.m_polyline(cord, 0.2), CORD, 0.95)
    f.fill(f.m_polyline([(x, y - 0.06) for x, y in cord], 0.07), K.lit(TRIM, 1.0), 0.45)
    for cx, cy, r in ((1.2, 3.5, 0.4), (2.85, 4.25, 0.5), (5.15, 4.25, 0.5), (6.8, 3.5, 0.4)):
        f.fill(f.m_line((cx, cy - r * 1.3), (cx, cy - r * 0.6), 0.1, 0.1), CORD, 0.9)                         # the loop the stone hangs from
        H.gem(f, cx, cy, r, GEM, setting="#5a5a80", facets=5, spec=0.9, glow=GEM_G, ry=r * 1.15)


def skirt(f, y0, y1=12.0, hem=None):
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, K.lit(COAT, 1.05)), (1, H.tone(COAT, 0.72))]))
    H.folds(f, m, [f.w * 0.25, f.w * 0.5, f.w * 0.75], depth=0.16, width=0.25, y0=y0, y1=y1)
    K.sheen(f, m, [f.w * 0.3], y0 + 0.2, y1 - 0.2, 0.2, "#aab0e0")
    if hem is not None:
        f.fill(f.m_band(hem, y1), H.lin((0, hem), (0, y1), [(0, K.lit(TRIM, 1.2)), (1, H.tone(TRIM, 0.7))]))
        f.fill(f.m_band(hem - 0.06, hem + 0.06), "#08080e", 0.7)
        f.fill(f.m_band(hem + 0.04, hem + 0.16), K.lit(TRIM, 1.6), 0.6)


def body_front(f, ov):
    K.cloth(f, f.m_all(), COAT, top=1.6, bottom=0.78, round_=0.14, fold_xs=[1.3, 6.7], fold_depth=0.12, fold_y0=2.0, fold_y1=8.8)
    for x0, x1 in ((0.0, 3.3), (4.7, 8.0)):                                                        # the two lit chest panels
        K.sheen(f, f.m_rect(x0, 1.8, x1, 8.8), [x0 + 0.8 if x0 < 4 else x1 - 0.8], 2.0, 8.4, 0.3, "#b4b8f0", width=0.28)
    f.fill(f.m_rect(0.0, 1.8, 0.28, 8.8), K.lit(COAT, 1.8), 0.4)
    f.fill(f.m_rect(7.72, 1.8, 8.0, 8.8), "#0a0a12", 0.4)
    # the front: a lilac edge on the left of the placket and a row of small buttons; the other side is plain: an asymmetric military closure
    lilac_edge(f, 3.35, 1.8, 8.9, 0.3)
    lilac_edge(f, 4.75, 1.8, 8.9, 0.22)
    f.fill(f.m_rect(3.45, 1.8, 4.65, 8.9), H.tone(COAT, 0.88), 0.55)
    for y in (2.5, 5.7, 7.1, 8.4):
        H.stud(f, 4.05, y, 0.22, TRIM, spec=0.9)
    # a shoulder line on each side
    K.seam(f, [(0.2, 2.6), (3.0, 2.9)], SEAM, 0.05, 0.5)
    K.seam(f, [(7.8, 2.6), (5.1, 2.9)], SEAM, 0.05, 0.5)
    collar(f, "front")
    if GEMS_ON_SKIN:
        relic_gems(f)
    K.belt_band(f, 8.9, 10.2, BELT, stitch="#6a6a8a")
    K.buckle(f, 4.0, 9.55, 1.9, 1.5, TRIM, hole="#0e0e18")
    skirt(f, 10.2)


def body_back(f, ov):
    K.cloth(f, f.m_all(), COAT, top=1.5, bottom=0.78, round_=0.14, fold_xs=[1.5, 2.9, 5.1, 6.5], fold_depth=0.11, fold_y0=2.0, fold_y1=9.0)
    collar(f, "back")
    f.fill(f.m_line((4.0, 1.9), (4.0, 8.8), 0.2, 0.2), "#0a0a12", 0.6)
    K.seam(f, [(3.62, 2.0), (3.62, 8.6)], SEAM, 0.05, 0.4)
    K.belt_band(f, 8.9, 10.2, BELT, stitch="#6a6a8a")
    H.stud(f, 4.0, 9.55, 0.2, TRIM, spec=0.7)
    skirt(f, 10.2)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), COAT, top=1.5, bottom=0.78, round_=0.14, fold_xs=[1.3, 2.7], fold_depth=0.11, fold_y0=2.0, fold_y1=9.0)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.6)], SEAM, 0.05, 0.5)
    K.belt_band(f, 8.9, 10.2, BELT, stitch="#6a6a8a")
    skirt(f, 10.2)


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
def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 9.4)
            K.cloth(f, sleeve, COAT, top=1.55, bottom=0.8, round_=0.18, fold_xs=[0.8, 1.9], fold_depth=0.12, fold_y0=1.6, fold_y1=9.0)
            K.sheen(f, sleeve, [f.w * 0.3], 1.8, 8.4, 0.28, "#b4b8f0", width=0.26)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.3), K.lit(COAT, 1.8), 0.5)
            K.seam(f, [(0.0, 0.65), (f.w, 0.65)], SEAM, 0.05, 0.5)
            for y, k in ((5.3, 0.6), (5.85, 0.4)):
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.16) * sleeve, "#08080e", k)
            f.fill(f.m_polyline([(0.0, 5.0), (f.w * 0.5, 5.25), (f.w, 5.05)], 0.2) * sleeve, K.lit(COAT, 2.0), 0.3)
            # a lilac stripe along the outer seam of the sleeve
            if nm in ("west", "east"):
                f.fill(f.m_line((f.w * 0.5, 0.8), (f.w * 0.5, 8.2), 0.2, 0.2) * sleeve, TRIM_D, 0.6)
            H.shadow_under(f, 9.4, 0.5, 0.3)
            K.cuff(f, 8.2, 9.4, TRIM, pipe="#e4e4fa", stud_color="#f0f0ff", spec=0.4)
            # the bare pale hand below the cuff
            hand = f.m_band(9.4, 12.0)
            f.fill(hand, H.lin((0, 9.4), (0, 12.0), [(0, H.tone(SKIN[1], 0.9)), (0.5, SKIN[1]), (1, H.tone(SKIN[0], 0.95))]))
            side = np.minimum(f.X, f.w - f.X)
            f.shade(hand, 1.0 - 0.1 * (1.0 - H.smoothstep(0.0, 0.8, side)), 0.9)
            H.shadow_under(f, 9.45, 0.7, 0.3, mask=hand)
            if nm in ("front", "back"):
                for i in (1, 2):
                    x = f.w * i / 3
                    f.fill(f.m_line((x, 10.9), (x, 11.95), 0.2, 0.16), H.tone(SKIN[0], 0.72), 0.65)
                f.fill(f.m_line((f.w * 0.62, 9.9), (f.w * 0.6, 10.7), 0.14, 0.1), "#b88c9c", 0.35)                  # a vein on the back of the hand
            f.fill(f.m_band(10.75, 10.9), H.tone(SKIN[0], 0.74), 0.5)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", COAT, edge=TRIM_D, k_top=1.5, overlay=ov)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 7.2, nm, knee=True, top=1.7, bottom=0.8, seam_color="#5a5a7a")
                skirt(f, 0.0, 4.1, hem=3.35)
                H.shadow_under(f, 4.1, 0.8, 0.4)
                H.shadow_under(f, 7.2, 0.8, 0.4)
                K.boot(f, BOOT, 7.1, nm, strap=True, strap_color="#4a4a68", gold=TRIM, cuff_color=TRIM_D, cuff_h=0.9, welt="#4a4a68", sole="#050508")


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

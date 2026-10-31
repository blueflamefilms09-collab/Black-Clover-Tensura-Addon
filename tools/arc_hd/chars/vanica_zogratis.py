"""Vanica Zogratis, the middle of the Dark Triad, the crowned blood mage with the eyepatch, HD 256x256 (docs/arc/hd_arc_bc5.md).

What she wears is the 64x64 sheet's outfit (tools/arc/char_vanica_zogratis.py): a belted wine-red short dress with a gold stand collar, a gold spade on the chest and a gold hem, long black sleeves
and gloves, long black boots with gold cuffs, a gold crown round the brow and a black eyepatch with a gold emblem over one eye, the other eye red. Slim (3 px) arms. The horns, the thorn halo and the
floating blood orb of the second form and the long red claws are the `pmx_vanica_zogratis_form2` extras: none of them is painted on the skin.
Drawn at 4x detail on both layers: a pale heart-shaped face with a long-lashed red eye, an arched brow, red lips in an open grin with a fang, a blush; the eyepatch (black, gold rim, a gold spade)
with its two straps and the crown (a gold band with five points and a ruby); the dress with lit panels, a gold-edged placket and spade, a ring buckle on a studded belt, a scalloped gold hem; the
black sleeves with a sheen and gold cuffs, the gloves with finger lines; the long boots with a gold cuff, buckles and a laced front.
Form 2 (`vanica_zogratis_form2.py`, the hybrid of the Gates, 100% Megicula) runs this same code with FORM2 = True: red skin, a darker dress with coral trim, a slit glowing eye, fangs, a glowing sigil
on the brow, veins and cracks, a glowing spade (the glow map exists only there).

Source (rule 10): no wiki page could be fetched in this environment (English and Japanese fandom are blocked); the look is from memory of the series plus docs/arc/remodel.md and the sheet. Her
canon look (a young woman with short black hair, a crown and an eyepatch in the Spade Kingdom's colours) is the sheet's reading; the cut and the colours are the sheet's choice.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc5 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

FORM2 = False
HAIR_STYLE = "bob"                                            # KEPT classic (hair_map): the black bob frames the face and the eyepatch; an anime cut would cover the crown
HAIR = "#221a20"
HAIR_RGB = (34, 26, 32)
DRESS = "#8a2236"
DRESS_L = "#b2364c"
GOLD = "#e2b84c"
GOLD_D = "#8a6a20"
BLACK = "#2a2230"
BELT = "#2c1c20"
BOOT = "#2a222e"
EYE = ("#7a0e1a", "#d8303c", "#ffb0a8")
CORAL = "#ff6a5a"
GLOW = "#ff4a4a"


def skin_tones():
    if FORM2:
        return ("#b04a50", "#d06a68", "#e89890"), "#8a3a40"
    return K.skin3("fair")


def trim():
    return CORAL if FORM2 else GOLD


def dress():
    return "#5e1022" if FORM2 else DRESS


# ================================================================================================ the head
def crown(f, name, overlay):
    """A gold crown band round the head (y 0.95..2.05) with points (five on the front and the back) and a ruby on the front."""
    c = GOLD
    f.fill(f.m_rect(-0.5, 0.95, f.w + 0.5, 2.05), H.lin((0, 0.95), (0, 2.05), [(0, K.lit(c, 1.35)), (0.45, c), (1, H.tone(c, 0.62))]))
    f.fill(f.m_rect(-0.5, 0.95, f.w + 0.5, 1.2), K.lit(c, 1.9), 0.7)
    f.fill(f.m_rect(-0.5, 1.9, f.w + 0.5, 2.05), "#3a2410", 0.85)
    pts = [(-0.3, 0.95)]
    n = 5 if name in ("front", "back") else 3
    for i in range(n):
        cx = f.w * (i + 0.5) / n
        h = 0.0 if i == n // 2 else 0.35
        pts += [(cx - 0.55, 0.95), (cx, h), (cx + 0.55, 0.95)]
    pts += [(f.w + 0.3, 0.95)]
    pm = f.m_poly(pts)
    f.fill(pm, H.lin((0, 0.0), (0, 1.0), [(0, K.lit(c, 1.7)), (1, c)]))
    f.fill(f.m_polyline(pts, 0.18) * pm, "#3a2410", 0.6)
    if name == "front":
        H.gem(f, 4.0, 1.5, 0.4, "#d8283c", setting=GOLD_D, facets=5, spec=0.9, glow=GLOW if FORM2 else None, ry=0.44)
    else:
        for x in np.arange(0.9, f.w, 1.7):
            H.stud(f, float(x), 1.5, 0.14, GOLD_D, spec=0.4)


def eyepatch(f):
    """The black eyepatch over the eye on the left of the picture: a rounded patch with a gold rim and a gold spade, a strap up to the crown and one round to the ear."""
    cx, cy = 2.35, 5.0
    f.fill(f.m_polyline([(cx - 0.2, cy - 1.0), (3.4, 3.4), (5.3, 2.25)], 0.34), "#14101a", 1.0)
    f.fill(f.m_polyline([(cx - 0.2, cy - 1.0), (3.4, 3.4), (5.3, 2.25)], 0.1) * f.m_rect(0, 0, 8, 8), "#46404e", 0.45)
    f.fill(f.m_polyline([(cx - 1.2, cy - 0.2), (0.4, 4.1), (-0.3, 3.8)], 0.34), "#14101a", 1.0)
    m = f.m_ellipse(cx, cy, 1.4, 1.35)
    f.fill(f.m_ellipse(cx, cy, 1.52, 1.47), GOLD_D, 1.0)
    f.fill(m, H.lin((0, cy - 1.35), (0, cy + 1.35), [(0, "#34303c"), (0.5, "#18141e"), (1, "#0a080e")]))
    f.fill(f.m_ring(cx, cy, 1.5, 0.22), H.lin((0, cy - 1.5), (0, cy + 1.5), [(0, K.lit(GOLD, 1.5)), (0.5, GOLD), (1, H.tone(GOLD, 0.6))]), 1.0)
    K.spade(f, cx, cy - 0.05, 1.25, GOLD, spec=0.4)
    f.fill(f.m_ellipse(cx - 0.55, cy - 0.7, 0.35, 0.2), "#ffffff", 0.2)


def head_front(cv):
    f = cv.face("head", "front")
    K.clear_glow(f)
    skin, crease = skin_tones()
    K.face_base(f, skin, side=0.08, chin=0.08)
    f.fill(f.m_ellipse(1.1, 6.1, 0.9, 0.55, soft=2.5), "#f08890" if not FORM2 else "#ff9a8a", 0.34)           # the blush
    f.fill(f.m_ellipse(6.9, 6.1, 0.9, 0.55, soft=2.5), "#f08890" if not FORM2 else "#ff9a8a", 0.34)
    # the arched brow over the red eye, the other brow is under the patch's strap
    H.brow(f, (4.7, 3.9), (7.0, 3.35), "#1a1418", 0.3, 0.12, arch=0.28)
    H.brow(f, (3.3, 3.6), (0.9, 3.3), "#1a1418", 0.28, 0.12, arch=0.2)
    # the red eye: big, long lashes; in the second form a slit that glows
    if FORM2:
        H.eye(f, 5.6, 5.1, 2.5, 1.9, ("#7a0a14", "#ff3a3a", "#ffc0b0"), lash="#14060a", tilt=0.3, lash_w=0.46, flick=0.5, glow=GLOW, hi=0.8, pupil="#14060a", lid_shadow=0.36)
        f.fill(f.m_ellipse(5.6, 5.12, 0.1, 0.7), "#14060a", 0.95)
    else:
        H.eye(f, 5.6, 5.05, 2.6, 2.1, EYE, lash="#14101a", tilt=0.2, lash_w=0.5, flick=0.55, lid_shadow=0.34, hi=0.95)
    f.fill(f.m_polyline([(4.5, 6.15), (5.6, 6.35), (6.9, 6.1)], 0.12), crease, 0.4)
    H.nose(f, 4.0, 6.15, crease, 0.2)
    # red lips in an open grin: a fang on each side in the second form, one in the first
    H.mouth_smile(f, 4.0, 6.8, 3.0, color="#a01828", lip="#e0505a", depth=0.3, thick=0.26, teeth="#fffaf6", open_h=0.5)
    if FORM2:
        for x in (3.05, 4.95):
            f.poly([(x - 0.16, 6.95), (x + 0.16, 6.95), (x, 7.5)], "#ffffff", 0.97)
    else:
        f.poly([(4.75, 6.98), (5.1, 6.98), (4.92, 7.45)], "#ffffff", 0.95)
    if FORM2:
        K.jag(f, [(7.2, 3.8), (6.9, 4.5), (7.25, 5.2), (6.95, 6.0)], CORAL, 0.14, 0.95, glow=GLOW, core="#fff0ea", dark="#400a14")
        K.jag(f, [(0.7, 6.0), (1.1, 6.6), (0.85, 7.2)], CORAL, 0.12, 0.9, glow=GLOW, core="#fff0ea", dark="#400a14")
        sx, sy = 4.0, 2.75                                                                                  # the sigil on the brow: a diamond with a hooked cross-line
        d = [(sx, sy - 0.55), (sx + 0.4, sy), (sx, sy + 0.55), (sx - 0.4, sy)]
        f.fill(f.m_polyline(d + [d[0]], 0.14), CORAL, 0.95, glow=GLOW)
        f.fill(f.m_line((sx - 0.7, sy), (sx + 0.7, sy), 0.12, 0.12), CORAL, 0.9, glow=GLOW)
    eyepatch(f)
    crown(f, "front", False)
    f2 = cv.face("head", "front", True)
    f2.clear()
    crown(f2, "front", True)
    # the patch and its strap are also on the second layer (it stands out from the face)
    m_ov = f2
    m_ov.fill(m_ov.m_ellipse(2.35, 5.0, 1.5, 1.45), "#18141e", 1.0)
    m_ov.fill(m_ov.m_ring(2.35, 5.0, 1.5, 0.22), GOLD, 1.0)
    K.spade(m_ov, 2.35, 4.95, 1.25, GOLD, spec=0.4)
    f2.fill(f2.m_polyline([(2.15, 4.0), (3.4, 3.4), (5.3, 2.25)], 0.34) * f2.m_rect(0, 2.05, 8, 8), "#14101a", 1.0)


def head_rest(cv, img64, spec):
    skin, _ = skin_tones()
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, skin)
    for nm in ("west", "east", "back"):
        crown(cv.face("head", nm, False), nm, False)
        ov = cv.face("head", nm, True)
        ov.clear()
        crown(ov, nm, True)


# ================================================================================================ the dress
def collar(f, name):
    c = trim()
    m = f.m_band(0.0, 1.8)
    f.fill(m, H.lin((0, 0), (0, 1.8), [(0, K.lit(c, 1.25)), (0.5, c), (1, H.tone(c, 0.66))]))
    H.bevel(f, m, depth=0.22, gain=1.1, spec=0.3)
    f.fill(f.m_rect(0.0, 1.68, f.w, 1.82), "#2a1608" if not FORM2 else "#400a14", 0.8)
    H.shadow_under(f, 1.82, 0.5, 0.32)
    if name == "front":
        f.fill(f.m_polyline([(3.55, 0.0), (4.0, 1.6), (4.45, 0.0)], 0.2), "#2a1608" if not FORM2 else "#400a14", 0.8)


def scallop_hem(f, y0, y1):
    """The gold hem of the dress: a band with a row of rounded scallops hanging from it."""
    c = trim()
    f.fill(f.m_rect(0.0, y0, f.w, y1 - 0.45), H.lin((0, y0), (0, y1), [(0, K.lit(c, 1.3)), (0.5, c), (1, H.tone(c, 0.66))]))
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.18), "#2a1608" if not FORM2 else "#400a14", 0.8)
    f.fill(f.m_rect(0.0, y0 + 0.18, f.w, y0 + 0.3), K.lit(c, 1.8), 0.55)
    for x in np.arange(0.5, f.w + 0.4, 1.0):
        sc = f.m_ellipse(float(x), y1 - 0.55, 0.52, 0.55)
        f.fill(sc, H.lin((0, y1 - 1.1), (0, y1), [(0, c), (1, H.tone(c, 0.66))]))
        f.fill(f.m_ring(float(x), y1 - 0.55, 0.52, 0.12) * sc, "#2a1608" if not FORM2 else "#400a14", 0.7)
    f.fill(f.m_rect(0.0, y1 - 0.5, f.w, y1), "#000000", 0.0)


def body_front(f, ov):
    c = dress()
    K.cloth(f, f.m_all(), c, top=1.4, bottom=0.78, round_=0.16, fold_xs=[1.5, 6.5], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    for x0, x1 in ((0.0, 3.0), (5.0, 8.0)):
        K.sheen(f, f.m_rect(x0, 2.0, x1, 8.8), [x0 + 0.7 if x0 < 4 else x1 - 0.7], 2.2, 8.6, 0.26, "#ffb4b8", width=0.3)
    f.fill(f.m_rect(0.0, 2.0, 0.3, 8.9), K.lit(c, 1.6), 0.35)
    f.fill(f.m_rect(7.7, 2.0, 8.0, 8.9), "#000000", 0.3)
    # the princess seams from the shoulder to the belt, the gold placket in the middle with small buttons
    for x0, x1 in ((2.2, 1.7), (5.8, 6.3)):
        K.seam(f, [(x0, 2.2), (x1, 8.6)], trim() if not FORM2 else "#ff9a8a", 0.06, 0.55)
    # the lower bodice: a darker panel under a gold sweetheart seam with a stitched diamond quilting
    low = f.m_poly([(-0.2, 6.1), (2.0, 6.5), (4.0, 6.9), (6.0, 6.5), (8.2, 6.1), (8.2, 8.9), (-0.2, 8.9)])
    f.fill(low, H.lin((0, 6.0), (0, 8.9), [(0, H.tone(c, 0.78)), (1, H.tone(c, 0.6))]), 0.95)
    for x in (1.0, 3.0, 5.0, 7.0):
        K.seam(f, [(x - 1.0, 8.8), (x + 0.4, 7.0)], trim() if not FORM2 else "#ff9a8a", 0.05, 0.35)
        K.seam(f, [(x + 1.0, 8.8), (x - 0.4, 7.0)], trim() if not FORM2 else "#ff9a8a", 0.05, 0.35)
    H.trim_line(f, [(0.0, 6.0), (2.0, 6.4), (4.0, 6.8), (6.0, 6.4), (8.0, 6.0)], trim(), width=0.3, spec=0.3, outline=0.6)
    # the gold spade on the chest
    K.spade(f, 4.0, 3.95, 2.9, trim(), spec=0.55, glow=GLOW if FORM2 else None)
    collar(f, "front")
    belt(f, True)
    scallop_hem(f, 10.9, 12.0)


def belt(f, front=False):
    K.belt_band(f, 8.8, 10.0, BELT, stitch="#a08060" if not FORM2 else "#d08090")
    if front:
        cx, cy = 4.0, 9.4
        f.fill(f.m_ring(cx, cy, 1.0, 0.42), H.lin((0, cy - 1.0), (0, cy + 1.0), [(0, K.lit(trim(), 1.5)), (0.5, trim()), (1, H.tone(trim(), 0.55))]), 1.0)
        f.fill(f.m_ring(cx, cy, 1.24, 0.1), "#2a1608" if not FORM2 else "#400a14", 0.8)
        f.fill(f.m_ring(cx, cy, 0.76, 0.1), "#2a1608" if not FORM2 else "#400a14", 0.7)
        f.ellipse(cx - 0.35, cy - 0.55, 0.28, 0.14, "#fff6c8", a=0.8)
        for x in (1.0, 7.0):
            H.stud(f, x, 9.4, 0.18, GOLD_D if not FORM2 else "#ffd0c8", spec=0.6)
    else:
        H.stud(f, 4.0, 9.4, 0.18, GOLD_D if not FORM2 else "#ffd0c8", spec=0.5)


def body_back(f, ov):
    c = dress()
    K.cloth(f, f.m_all(), c, top=1.35, bottom=0.78, round_=0.16, fold_xs=[1.6, 6.4], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    K.sheen(f, f.m_rect(0, 2, 8, 8.8), [1.7, 6.2], 2.2, 8.4, 0.2, "#ffb4b8", width=0.3)
    collar(f, "back")
    # the back: a gold lacing up the middle and a bow of black ribbon at the belt
    for y in np.arange(2.5, 8.3, 0.95):
        K.line(f, [(3.3, y), (4.7, y + 0.6)], trim(), 0.16, 0.9)
        K.line(f, [(4.7, y), (3.3, y + 0.6)], trim(), 0.16, 0.9)
    belt(f)
    scallop_hem(f, 10.9, 12.0)


def body_side(f, ov, name):
    c = dress()
    K.cloth(f, f.m_all(), c, top=1.35, bottom=0.78, round_=0.16, fold_xs=[1.4], fold_depth=0.07, fold_y0=2.0, fold_y1=8.8)
    collar(f, name)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.1), (ed, 8.7)], trim() if not FORM2 else "#ff9a8a", 0.06, 0.6)
    belt(f)
    scallop_hem(f, 10.9, 12.0)


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
    skin, crease = skin_tones()
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sleeve = f.m_band(0.0, 8.4)
            K.cloth(f, sleeve, BLACK, top=1.9, bottom=0.78, round_=0.2, fold_xs=[f.w * 0.5], fold_depth=0.08, fold_y0=1.0, fold_y1=8.0)
            K.sheen(f, sleeve, [f.w * 0.3], 1.4, 7.8, 0.34, "#a898c0", width=0.3)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.35), K.lit(BLACK, 2.4), 0.55)
            K.band(f, 0.8, 1.5, trim())                                                                    # the gold edge of the shoulder
            H.shadow_under(f, 1.5, 0.5, 0.28)
            f.fill(f.m_polyline([(0.0, 4.6), (f.w * 0.5, 4.85), (f.w, 4.65)], 0.16) * sleeve, "#08060c", 0.55)   # elbow creases
            f.fill(f.m_polyline([(0.0, 4.9), (f.w * 0.5, 5.15), (f.w, 4.95)], 0.2) * sleeve, K.lit(BLACK, 2.6), 0.3)
            K.band(f, 8.3, 9.3, trim())                                                                    # the gold cuff between the sleeve and the glove
            for x in np.arange(0.5, f.w, 0.9):
                f.fill(f.m_poly([(x - 0.3, 8.5), (x + 0.3, 8.5), (x, 9.0)]), H.tone(trim(), 0.55), 0.7)
            H.shadow_under(f, 9.3, 0.5, 0.3)
            K.glove(f, "#34293a", 9.3, 12.0, nm, finger_n=2, knuckle_k=1.6)
            f.fill(f.m_band(9.3, 12.0) * f.m_rect(0, 9.3, f.w, 9.7), "#000000", 0.0)


def arm_tops(cv):
    for part in ("arm_r", "arm_l"):
        for ov in (False, True):
            K.cap(cv, part, "top", BLACK, edge=H.tone(BLACK, 0.5), k_top=2.0, overlay=ov)


def legs(cv):
    c = dress()
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.cloth(f, f.m_band(0.0, 3.0), c, top=1.35, bottom=0.82, round_=0.14, fold_xs=[2.0], fold_depth=0.07, fold_y0=0.2, fold_y1=2.9)
                scallop_hem(f, 3.0, 4.2)
                H.shadow_under(f, 4.2, 0.8, 0.4)
                # the long boots: black, a gold top cuff with studs, a laced front with buckles, a toe cap, a gold welt
                K.boot(f, BOOT, 4.2, nm, strap=False, gold=GOLD_D, cuff_color=trim(), cuff_h=0.9, welt="#6a5028" if not FORM2 else "#8a3a3a", sole="#08060a", toe=True)
                if nm == "front":
                    for y in (6.2, 8.0):
                        f.fill(f.m_rect(0.0, y, f.w, y + 0.62), H.lin((0, y), (0, y + 0.62), [(0, "#3a3040"), (1, "#16101c")]))
                        f.fill(f.m_rect(0.0, y, f.w, y + 0.12), K.lit("#3a3040", 2.0), 0.5)
                        H.stud(f, f.w * 0.5, y + 0.31, 0.26, trim(), spec=0.8)
                    K.seam(f, [(f.w * 0.5, 5.3), (f.w * 0.5, 6.1)], "#6a5a78", 0.05, 0.6)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    head_rest(cv, img64, spec)
    head_front(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    arm_tops(cv)
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim)

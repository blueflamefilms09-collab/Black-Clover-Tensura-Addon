"""Magna Swing, the fire mage of the Black Bulls, HD 256x256 (docs/arc/hd_arc_bc2.md).

What he wears is the 64x64 sheet's outfit (tools/arc/char_magna_swing.py): a short navy leather jacket with a stand collar over a white shirt, a dark belt with a gold buckle and two pouches,
black fingerless gloves, black trousers and black calf boots, a black strap round the head and tinted goggles with blue lenses. The broom Crazy Cyclone (stowed across the back, its straw glows) and
the Black Bull cloak are the `pmx_magna_swing` extra, so the back of the jacket is kept plain. The mohawk is the kit's classic `mohawk` hair (no anime cut is a mohawk, owner decision KEPT).
No glow on the skin. Drawn at 4x detail on both layers: the angry brows, the goggles (a black frame, blue lenses with a glare), the grin with two fangs, the strap, the jacket with its
lit leather edges, piping and stitching, the white shirt with buttons and collar flaps, the belt with pouches, the gloves with studs, the creased trousers and the strapped boots.

Source (rule 10): no wiki page could be fetched in this environment; the look is from memory of the series plus the repo sheet: a lean ex-delinquent with a black mohawk, tinted goggles, a short
navy jacket over a white shirt, fingerless gloves, a hot-headed grin.
"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import _bc2 as K  # noqa: E402

A, H = K.A, K.H
SIDES = K.SIDES

HAIR_STYLE = "mohawk"
HAIR = "#1a1a22"
HAIR_RGB = (26, 26, 34)
SKIN = ("#e8b496", "#f5d1b4", "#ffe6d2")
JACKET = "#2d3a62"                                            # navy leather (the sheet's #2a3555, a little brighter so it reads as leather)
COLLAR = "#4a5a90"
SHIRT = "#f0efec"
TROUS = "#25252e"
BOOT = "#1b1b20"
GLOVE = "#202026"
BELT = "#3e2c22"
GOLD = "#c8a040"
FRAME = "#1c1c24"
LENS = "#3a78d8"
IRIS = ("#22222c", "#3e3e50", "#7a7a90")


# ================================================================================================ the head
def goggles(f):
    """Tinted goggles over the eyes: a black frame with a bevel, blue lenses with a glare, a bridge, and the strap that runs into the sides."""
    for cx in (2.3, 5.7):
        f.ellipse(cx, 5.05, 1.5, 1.28, H.tone(FRAME, 0.7))                       # the dark rim
        f.ellipse(cx, 5.05, 1.38, 1.16, FRAME)
        lens = f.m_ellipse(cx, 5.05, 1.12, 0.9)
        f.fill(lens, H.lin((0, 4.1), (0, 6.0), [(0, "#2a58b0"), (0.5, LENS), (1, "#6aa4f0")]))
        H.eye(f, cx, 5.08, 1.9, 1.35, IRIS, lash="#14141a", tilt=0.0, lash_w=0.3, flick=0.0, lid_shadow=0.25, hi=0.5, iris_r=0.95)
        f.fill(lens, "#3a78d8", 0.5)                                              # the tint over the eye
        f.fill(f.m_line((cx - 0.75, 5.65), (cx - 0.1, 4.45), 0.24, 0.12) * lens, "#ffffff", 0.7)       # the glare
        f.fill(f.m_line((cx + 0.15, 5.7), (cx + 0.55, 5.05), 0.2, 0.1) * lens, "#ffffff", 0.45)
        f.fill(f.m_ring(cx, 5.05, 1.25, 0.2) * f.m_rect(0, 3.8, 8, 4.5), K.lit(FRAME, 3.4), 0.6)      # the light on the top edge of the rim
    f.fill(f.m_rect(3.7, 4.7, 4.3, 5.3), FRAME)                                    # the bridge
    f.fill(f.m_rect(3.7, 4.7, 4.3, 4.9), K.lit(FRAME, 3.0), 0.5)
    f.fill(f.m_rect(0.0, 4.5, 0.95, 5.5), FRAME)                                   # the strap leaves the frame on both sides
    f.fill(f.m_rect(7.05, 4.5, 8.0, 5.5), FRAME)


def head_front(cv):
    f = cv.face("head", "front")
    K.face_base(f, SKIN, side=0.09, chin=0.07)
    pal = K.hair_pal("#2a2a34", light_k=1.3)
    # a few lighter stubbly roots of the shaved sides show at the temples
    for x0 in (0.0, 6.6):
        f.fill(f.m_rect(x0, 0.0, x0 + 1.4, 1.0, soft=2.0), pal["m"], 0.55)
    goggles(f)
    # angry thick black brows, low and slanted, above the frame
    H.brow(f, (3.35, 3.5), (0.9, 2.7), "#14141a", 0.5, 0.2, arch=0.0)
    H.brow(f, (4.65, 3.5), (7.1, 2.7), "#14141a", 0.5, 0.2, arch=0.0)
    H.nose(f, 4.0, 6.3, "#cf977e", 0.22)
    f.fill(f.m_ellipse(1.1, 6.6, 0.85, 0.45, soft=2.0), "#f0988e", 0.3)
    f.fill(f.m_ellipse(6.9, 6.6, 0.85, 0.45, soft=2.0), "#f0988e", 0.3)
    H.mouth_smile(f, 4.0, 6.95, 3.0, color="#5a2424", lip="#d17a72", depth=0.26, thick=0.2, teeth="#fffdf6", open_h=0.55)
    for x in (2.9, 5.1):
        f.poly([(x - 0.2, 7.1), (x + 0.2, 7.1), (x, 7.6)], "#ffffff", 0.95)
    # the headband over the forehead (row 1 and 2 of the head)
    band(f, 1.0, 2.0)


def band(f, y0, y1):
    """The black strap that runs round the head at the forehead: a gradient, a lit top edge, a stitch and a metal slide at the centre of the front."""
    f.fill(f.m_rect(0.0, y0, f.w, y1), H.lin((0, y0), (0, y1), [(0, K.lit("#26262e", 1.9)), (0.4, "#1c1c24"), (1, "#0e0e14")]))
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.16), K.lit("#26262e", 3.2), 0.5)
    f.fill(f.m_rect(0.0, y1 - 0.12, f.w, y1), "#050508", 0.7)
    K.seam(f, [(0.0, (y0 + y1) / 2.0), (f.w, (y0 + y1) / 2.0)], "#6a6a7c", 0.055, 0.55, (0.3, 0.2), 0.0)
    if f.name == "front":
        f.rect(3.4, y0 - 0.12, 4.6, y1 + 0.12, H.metal_paint(y0, y1, "#9aa0ae", 1.1), r=0.1)
        f.rect(3.7, y0 + 0.2, 4.3, y1 - 0.2, "#14141c", r=0.05)


def head_overlay(cv):
    """The strap on the second layer, all four sides (row 1 of the 64x64 sheet)."""
    for nm in SIDES:
        f = cv.face("head", nm, True)
        f.clear()
        band(f, 1.0, 2.0)
    return {("head", nm): np.asarray((cv.face("head", nm, True).m_alpha() > 0.5), np.float32) for nm in SIDES}


# ================================================================================================ the jacket
def jacket_panel(f, x0, x1):
    m = f.m_rect(x0, 0.0, x1, 12.0)
    K.cloth(f, m, JACKET, top=1.22, bottom=0.8, round_=0.1, fold_xs=[x0 + (x1 - x0) * 0.5], fold_depth=0.1, fold_y0=2.0, fold_y1=10.0)
    K.sheen(f, m, [x0 + (x1 - x0) * 0.28], 2.2, 8.6, 0.3, "#b8c8f0", width=0.26)


def body_front(f, ov):
    # the shirt in the open front, then the two jacket panels over it
    sh = f.m_rect(2.9, 0.0, 5.1, 12.0)
    K.cloth(f, sh, SHIRT, top=1.06, bottom=0.82, round_=0.05)
    f.fill(f.m_line((4.0, 1.4), (4.0, 12.0), 0.2, 0.2) * sh, H.tone(SHIRT, 0.6), 0.55)
    for y in (2.3, 4.0, 5.7, 7.3):
        H.stud(f, 4.0, y, 0.2, "#c8c8d0", spec=0.7)
    jacket_panel(f, 0.0, 2.9)
    jacket_panel(f, 5.1, 8.0)
    for xe, s in ((2.9, -1.0), (5.1, 1.0)):                                         # the lit leather edge of the opening, a stitch beside it
        f.fill(f.m_line((xe, 1.4), (xe, 12.0), 0.22, 0.22), K.lit(JACKET, 1.9), 0.75)
        f.fill(f.m_line((xe + s * 0.22, 1.4), (xe + s * 0.22, 12.0), 0.22, 0.22), K.deep(JACKET, 0.4), 0.6)
        K.seam(f, [(xe + s * -0.45, 2.0), (xe + s * -0.45, 11.0)], "#8aa0d8", 0.055, 0.55)
    # the collar: a stand collar of lighter navy, the shirt's collar flaps in the V
    H.trim_band(f, 0.0, 1.5, COLLAR, spec=0.4, outline=0.55)
    for sgn in (-1.0, 1.0):
        fl = f.m_poly([(4.0 + sgn * 0.12, 0.0), (4.0 + sgn * 1.2, 0.0), (4.0 + sgn * 0.95, 1.2), (4.0 + sgn * 0.1, 2.1)])
        f.fill(fl, H.tone(SHIRT, 1.04))
        f.fill(f.m_polyline([(4.0 + sgn * 0.1, 2.1), (4.0 + sgn * 0.95, 1.2), (4.0 + sgn * 1.2, 0.0)], 0.2), H.tone(SHIRT, 0.55), 0.8)
    H.shadow_under(f, 1.5, 0.5, 0.26)
    # a chest pocket seam on the lit panel and a zip pull
    K.seam(f, [(0.7, 3.4), (2.2, 3.4), (2.2, 5.2)], "#8aa0d8", 0.055, 0.5)
    # the belt, pouches and the gold buckle
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    for x0, x1 in ((0.45, 1.95), (6.05, 7.55)):
        pm = f.m_rect(x0, 9.0, x1, 10.6, r=0.12)
        f.fill(pm, H.lin((0, 9.0), (0, 10.6), [(0, K.lit("#4a382c", 1.3)), (1, "#2a1c14")]))
        f.fill(f.m_rect(x0, 9.0, x1, 9.55), K.lit("#4a382c", 1.5), 0.6)
        H.stud(f, (x0 + x1) / 2.0, 9.55, 0.14, GOLD, spec=0.6)
    K.buckle(f, 4.0, 9.5, 2.1, 1.4, GOLD, hole="#241808")
    # the jacket hem
    f.fill(f.m_band(11.3, 12.0), K.deep(JACKET, 0.62), 0.6)


def body_back(f, ov):
    m = f.m_all()
    K.cloth(f, m, JACKET, top=1.18, bottom=0.8, round_=0.1, fold_xs=[1.5, 3.0, 5.0, 6.5], fold_depth=0.1, fold_y0=2.0, fold_y1=10.0)
    K.sheen(f, m, [1.5, 6.4], 2.2, 8.6, 0.24, "#b8c8f0", width=0.26)
    H.trim_band(f, 0.0, 1.5, COLLAR, spec=0.4, outline=0.55)
    H.shadow_under(f, 1.5, 0.5, 0.26)
    f.fill(f.m_line((4.0, 1.7), (4.0, 12.0), 0.22, 0.22), K.deep(JACKET, 0.45), 0.6)
    K.seam(f, [(3.6, 1.9), (3.6, 8.6)], "#8aa0d8", 0.055, 0.45)
    f.fill(f.m_polyline([(0.4, 3.0), (4.0, 3.5), (7.6, 3.0)], 0.22), K.deep(JACKET, 0.55), 0.55)           # the yoke seam
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    f.fill(f.m_band(11.3, 12.0), K.deep(JACKET, 0.62), 0.6)


def body_side(f, ov, name):
    K.cloth(f, f.m_all(), JACKET, top=1.16, bottom=0.8, round_=0.12, fold_xs=[1.3, 2.7], fold_depth=0.1, fold_y0=2.0, fold_y1=10.0)
    K.sheen(f, f.m_all(), [f.w * 0.3], 2.2, 8.6, 0.22, "#b8c8f0", width=0.26)
    H.trim_band(f, 0.0, 1.5, COLLAR, spec=0.4, outline=0.55)
    H.shadow_under(f, 1.5, 0.5, 0.26)
    ed = f.w - 0.7 if name == "west" else 0.7
    K.seam(f, [(ed, 2.0), (ed, 8.4)], "#8aa0d8", 0.055, 0.5)
    K.belt_band(f, 8.9, 10.1, BELT, stitch="#a07a5a")
    f.fill(f.m_band(11.3, 12.0), K.deep(JACKET, 0.62), 0.6)


def body(cv):
    for nm in SIDES:
        for f, isov in zip(K.pair(cv, "body", nm), (False, True)):
            if nm == "front":
                body_front(f, isov)
            elif nm == "back":
                body_back(f, isov)
            else:
                body_side(f, isov, nm)


# ================================================================================================ arms and legs
def arm(cv, part):
    for nm in SIDES:
        for f in K.pair(cv, part, nm):
            sl = f.m_band(0.0, 8.2)
            K.cloth(f, sl, JACKET, top=1.2, bottom=0.82, round_=0.14, fold_xs=[1.2, 2.8], fold_depth=0.1, fold_y0=1.0, fold_y1=7.8)
            K.sheen(f, sl, [f.w * 0.3], 1.4, 7.6, 0.28, "#b8c8f0", width=0.26)
            f.fill(f.m_rect(0.0, 0.0, f.w, 0.32), K.deep(JACKET, 0.62), 0.6)
            K.seam(f, [(0.0, 0.75), (f.w, 0.75)], "#8aa0d8", 0.055, 0.55, (0.3, 0.2), 0.15)
            for y in (4.6, 5.1):                                                   # the elbow creases of the leather
                f.fill(f.m_polyline([(0.0, y), (f.w * 0.5, y + 0.3), (f.w, y + 0.05)], 0.18) * sl, K.deep(JACKET, 0.5), 0.55)
            f.fill(f.m_polyline([(0.0, 4.35), (f.w * 0.5, 4.6), (f.w, 4.4)], 0.22) * sl, K.lit(JACKET, 1.7), 0.35)
            H.shadow_under(f, 8.2, 0.5, 0.3)
            K.cuff(f, 7.7, 8.9, H.tone(JACKET, 0.8), pipe="#8aa0d8", stud_color="#b8c0d0")      # a strapped cuff
            g = f.m_band(8.9, 10.9)
            K.cloth(f, g, GLOVE, top=1.5, bottom=0.8, round_=0.1)
            K.sheen(f, g, [f.w * 0.3], 9.0, 10.8, 0.2, "#c8c8e0")
            f.fill(f.m_band(8.9, 9.2), "#0a0a0e", 0.6)
            for x in (f.w * 0.25, f.w * 0.5, f.w * 0.75):                          # the studs on the knuckles
                H.stud(f, x, 9.95, 0.15, "#b8bcc8", spec=0.8)
            K.limb_skin(f, SKIN, 10.9, 12.0, tendon=False)
            if nm in ("front", "back"):
                for x in (f.w * 0.25, f.w * 0.5, f.w * 0.75):
                    f.fill(f.m_line((x, 11.1), (x, 12.0), 0.22, 0.2), H.tone(SKIN[0], 0.7), 0.7)
            f.fill(f.m_band(10.8, 11.05), "#08080c", 0.7)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in K.pair(cv, part, nm):
                K.trousers(f, TROUS, 0.0, 6.2, nm, knee=True, fold_xs=(1.1, 2.9), seam_color="#5a5a72", top=1.5, bottom=0.8)
                H.shadow_under(f, 6.2, 0.7, 0.35)
                K.boot(f, BOOT, 6.2, nm, strap="#4a3a30", cuff_color="#34343c", cuff_h=0.8, welt="#46464e", sole="#08080c", gold=GOLD)
                if nm == "front":
                    f.fill(f.m_rect(0.0, 8.6, f.w, 9.3), "#3a2c24", 0.95)
                    f.fill(f.m_rect(0.0, 8.6, f.w, 8.75), K.lit("#3a2c24", 1.7), 0.7)
                    H.stud(f, f.w * 0.5, 8.95, 0.28, GOLD, spec=0.7)


# ================================================================================================ paint
def paint(spec, cid, masks):
    cv, covers, hair, slim, img64 = K.start(cid, spec, masks, HAIR_STYLE, HAIR, HAIR_RGB)
    K.tidy_head(cv, img64, spec, HAIR, HAIR_RGB, SKIN, ears=True)
    head_front(cv)
    hc = head_overlay(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    legs(cv)
    return K.finish(cv, covers, hair, HAIR_RGB, slim, extra_covers=hc)

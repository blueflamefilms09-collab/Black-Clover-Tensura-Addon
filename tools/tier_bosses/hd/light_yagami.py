"""Light Yagami (Death Note), HD 256x256 (3.38.0, group d of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_d.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/light_yagami.py): a slim, poised young man with side-swept auburn-brown hair, calm sharp amber eyes (they flare warm and glow), a faint
knowing smile, a dark navy school blazer with silver buttons and notched lapels over a white shirt and a charcoal tie, dark trousers with a crease, polished black shoes.  Kept from the 3.36.0 build
(the owner's motif and LightYagamiDataTest), drawn again at HD by this module and no longer by gen_tier_looks.light_post: the small teal notebook in the breast pocket (white page edge on top, a gold
bookmark ribbon, a spine, a corner highlight; on both layers) and the thin teal page-edge line round the left cuff.  The suit stays dark and has no red.
HD adds: a really drawn sharp face (narrow eyes with a flare, thin brows, a smirk, the glow on both irises), a swept fringe on the base head, a blazer with stitched notch lapels, a two-button front,
flap pockets, a welt breast pocket, sleeve buttons, white shirt cuffs, a woven charcoal tie with a knot, a creased trouser leg, polished shoes with laces and a specular streak.
Hair: `anime_short_open` (the swept fringe stops above the brows: forehead and eyes stay visible; `anime_swept` hid his eyes), auburn.  Original art from memory of the canon look (rule 10: no wiki could be fetched).  Glow: the irises.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402
import _dlib as D  # noqa: E402

SKIN = ("#d6a888", "#f4d4b8", "#fde8d4")
HAIR = (118, 82, 58)
HAIRC = "#7a5438"
NAVY = "#1a2848"
NAVY_D = "#0a1224"
NAVY_L = "#3a4c7c"
SHIRT = "#f6f6fa"
SHIRT_D = "#a8aec8"
TIE = "#2c303e"
TROUS = "#1a1f30"
SHOE = "#0c0c14"
SIL, SIL_D = "#cfd4de", "#6a7284"
TEAL, TEAL_D, TEAL_L = "#1f7a74", "#165c58", "#3ab4a8"
PAGES = "#ecebe0"
RIBBON = "#d6b040"
CUFF = "#35d0b5"                       # the page-edge line of the left cuff (the 3.36.0 colour)
AMBER = ("#4a2208", "#b4601c", "#f2a448")
SIDES = D.SIDES


# ================================================================================================ the head
def head(cv):
    D.head_lod(cv, SKIN, HAIRC, sb_x=7.1, sb_y=3.6, fringe=1.5, ear_x=4.3, nape=6.2, back_nape=6.6)
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(HAIRC, 0.95), hairline=2.0, wave=0.6, shadow=0.22, side=0.07)
    # the side-swept fringe on the base head: two strands slanting from the parting on the viewer's left across the forehead
    for pts in (
        [(0.6, 1.6), (3.2, 1.9), (5.4, 2.4), (6.6, 3.1), (5.7, 2.7), (3.9, 2.4), (1.8, 2.3)],
        [(2.4, 1.7), (4.6, 2.05), (6.3, 2.7), (7.4, 3.5), (6.5, 2.6), (4.2, 2.2)],
    ):
        m = f.m_poly(pts)
        f.fill(m, H.lin((0, 1.4), (0, 3.2), [(0, H.tone(HAIRC, 1.05)), (1, H.tone(HAIRC, 0.8))]), 0.9)
        f.fill(f.m_polyline(pts[:4], 0.07), H.tone(HAIRC, 1.5), 0.4)
    # thin, slightly severe brows: the inner end lower
    H.brow(f, (3.5, 3.55), (0.9, 3.15), "#4a2a18", 0.2, 0.12, arch=0.06)
    H.brow(f, (4.5, 3.55), (7.1, 3.15), "#4a2a18", 0.2, 0.12, arch=0.06)
    # calm sharp amber eyes: narrow, the outer corner raised; both irises glow
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.95, 2.4, 1.4, AMBER, lash="#26120a", sclera="#f4efe4", tilt=0.2, pupil="#180a04", hi=0.95, lid_shadow=0.42, lash_w=0.42, flick=0.34, glow="#a8641a", outer=(-1 if cx < 4 else 1))
        f.polyline([(cx - 1.15, 4.35), (cx, 4.1), (cx + 1.15, 4.35)], H.tone(SKIN[0], 0.75), 0.06, a=0.35)         # the lid crease
    H.nose(f, 4.0, 6.2, "#d0968a", 0.26)
    f.line((4.0, 4.8), (3.95, 6.0), H.tone(SKIN[0], 0.82), 0.08, 0.14, a=0.32)
    # a faint knowing smile, one corner up, a sharp jaw
    H.mouth_smile(f, 4.0, 7.2, 1.9, color="#a8625a", lip="#d49286", depth=0.13, thick=0.13)
    f.line((5.0, 7.33), (5.4, 7.0), "#a8625a", 0.1, 0.07, a=0.9)
    f.fill(f.m_poly([(0.0, 6.0), (0.9, 7.2), (1.6, 8.0), (0.0, 8.0)]), SKIN[0], 0.3)
    f.fill(f.m_poly([(8.0, 6.0), (7.1, 7.2), (6.4, 8.0), (8.0, 8.0)]), SKIN[0], 0.3)


# ================================================================================================ pieces
def blazer(f, mask, folds=(), y0=0.0, y1=12.0, k=1.0):
    H.cloth(f, mask, H.tone(NAVY, k), top=1.2, bottom=0.78, round_=0.12, fold_xs=None, weave_k=0.04)
    H.folds(f, mask, list(folds), depth=0.11, width=0.3, sway=0.12, y0=y0, y1=y1)
    H.sheen(f, mask, [x + 0.4 for x in folds[:1]], width=0.15, strength=0.1, y0=y0, y1=y1, color="#8aa0d8")


def button(f, x, y, r=0.24):
    H.stud(f, x, y, r, SIL, rim_color=SIL_D, spec=0.85)
    f.fill(f.m_ring(x, y, r * 0.5, 0.05), SIL_D, 0.5)


def notebook(f):
    """The teal notebook in the breast pocket (viewer's right): the white pages on top, the teal cover with a darker spine, a corner highlight, a gold bookmark ribbon hanging from the spine edge."""
    x0, x1, y0, y1 = 5.15, 6.85, 2.3, 5.4
    f.fill(f.m_rect(x0 + 0.06, y0 + 0.07, x1 + 0.08, y1, 0.05), "#050a14", 0.5)                    # the shadow on the blazer
    body = f.m_rect(x0, y0, x1, y1, 0.05)
    f.fill(body, TEAL_D)
    cover = f.m_rect(x0 + 0.05, y0 + 0.52, x1 - 0.05, y1, 0.04)
    f.fill(cover, H.lin((0, y0), (0, y1), [(0, TEAL_L), (0.35, TEAL), (1, TEAL_D)]))
    f.fill(f.m_rect(x0, y0, x0 + 0.34, y1, 0.0) * body, TEAL_D, 0.85)                              # the spine
    H.bevel(f, cover, depth=0.18, gain=0.55, spec=0.2)
    pages = f.m_rect(x0 + 0.04, y0, x1 - 0.04, y0 + 0.55, 0.04)
    f.fill(pages, H.lin((0, y0), (0, y0 + 0.55), [(0, "#ffffff"), (1, PAGES)]))
    for i in range(1, 4):
        f.line((x0 + 0.1, y0 + i * 0.13), (x1 - 0.1, y0 + i * 0.13), "#c4c2b2", 0.025, 0.025, a=0.7)    # the page edges
    f.fill(f.m_ring(x0 + 0.85, y0 + 1.55, 0.23, 0.05), TEAL_L, 0.5)                                # a small round label mark on the cover (original)
    f.line((x1 - 0.2, y0 + 0.55), (x1 - 0.2, y1 + 0.5), RIBBON, 0.17, 0.12, a=1.0)                  # the bookmark ribbon: it hangs over the pocket edge
    f.poly([(x1 - 0.28, y1 + 0.5), (x1 - 0.12, y1 + 0.5), (x1 - 0.2, y1 + 0.22)], RIBBON, a=1.0)
    f.line((x0 + 0.5, y0 + 0.62), (x1 - 0.4, y0 + 0.62), TEAL_L, 0.05, 0.05, a=0.7)                 # the lit top edge of the cover


def pocket_welt(f):
    f.fill(f.m_rect(4.95, 5.2, 7.1, 5.62, 0.04), "#060c1a", 0.9)
    f.fill(f.m_line((5.0, 5.18), (7.05, 5.18), 0.07, 0.07), NAVY_L, 0.9)
    f.fill(f.m_line((5.0, 5.64), (7.05, 5.64), 0.05, 0.05), "#050a16", 0.8)


# ================================================================================================ the body
def body_front(f):
    # the blazer first, the shirt shows in the V between the lapels
    blazer(f, f.m_all(), folds=[0.8, 1.7, 6.3, 7.2], y0=0.5, y1=12.0)
    v = f.m_poly([(2.55, -0.1), (5.45, -0.1), (5.0, 0.9), (4.45, 6.0), (4.0, 6.7), (3.55, 6.0), (3.0, 0.9)])
    f.fill(f.m_poly([(2.65, 0.0), (5.55, 0.0), (5.1, 1.0), (4.55, 6.1), (4.05, 6.85), (3.45, 6.1), (2.95, 1.0)]), "#04060e", 0.45)
    H.cloth(f, v, SHIRT, top=1.04, bottom=0.84, round_=0.05, fold_xs=[], weave_k=0.015)
    # the lapels: a lighter facing with a notch, bevelled, with stitching along the edge
    for sg in (0, 1):
        lap = [(2.55, -0.1), (3.0, 0.9), (3.55, 4.2), (3.15, 5.0), (2.45, 3.9), (1.3, 3.1), (1.75, 2.0), (2.0, 0.7)]
        if sg:
            lap = D.mirror(lap, 8.0)
        m = f.m_poly(lap)
        f.fill(m, H.lin((0, 0), (0, 5.0), [(0, NAVY_L), (1, H.tone(NAVY_L, 0.75))]))
        H.bevel(f, m, depth=0.18, gain=0.7, spec=0.12)
        f.fill(f.m_polyline(lap[1:5], 0.08), "#0a1226", 0.7)
        D.stitch_seam(f, [(x + (-0.2 if sg == 0 else 0.2), y) for x, y in lap[1:4]], "#8aa0d0", 0.03, dash=(0.16, 0.12))
    # the shirt collar points lie on the blazer, the tie knot and tie
    for sg in (-1, 1):
        pts = [(4.0 + sg * 0.2, -0.1), (4.0 + sg * 1.45, -0.1), (4.0 + sg * 1.5, 0.95), (4.0 + sg * 0.25, 1.15)]
        H.cloth(f, f.m_poly(pts), SHIRT, top=1.03, bottom=0.86, round_=0.0, fold_xs=[], weave_k=0.0)
        f.fill(f.m_polyline(pts[1:4], 0.06), SHIRT_D, 0.6)
    knot = f.m_poly([(3.45, 1.1), (4.55, 1.1), (4.4, 1.95), (3.6, 1.95)])
    H.cloth(f, knot, H.tone(TIE, 1.25), top=1.15, bottom=0.8, round_=0.0, fold_xs=[], weave_k=0.0)
    tie = f.m_poly([(3.58, 1.95), (4.42, 1.95), (4.72, 7.3), (4.0, 7.95), (3.28, 7.3)])
    f.fill(f.m_poly([(3.68, 2.0), (4.52, 2.0), (4.82, 7.4), (4.1, 8.05), (3.38, 7.4)]), "#05060c", 0.35)
    H.cloth(f, tie, TIE, top=1.2, bottom=0.8, round_=0.0, fold_xs=[], weave_k=0.0)
    for k in range(6):                                                                              # the diagonal weave of the tie
        f.line((3.4, 2.3 + k * 0.95), (4.7, 1.8 + k * 0.95), H.tone(TIE, 1.5), 0.05, 0.05, a=0.28)
    H.shadow_under(f, 1.95, 0.35, 0.3, mask=f.m_band(1.95, 3.0, x0=3.4, x1=4.6))
    # two silver buttons, the lower closing edge, the breast pocket with the teal notebook, flap pockets
    f.fill(f.m_line((4.1, 6.5), (4.15, 12.0), 0.07, 0.07), "#06091a", 0.8)
    f.fill(f.m_line((4.17, 6.5), (4.22, 12.0), 0.04, 0.04), NAVY_L, 0.4)
    button(f, 3.65, 8.0)
    button(f, 3.65, 10.1)
    for x0, x1 in ((0.55, 2.35), (5.65, 7.45)):
        f.fill(f.m_rect(x0, 9.0, x1, 9.35, 0.03), "#050a16", 0.85)                                  # the flap pockets
        fl = f.m_rect(x0, 8.55, x1, 9.15, 0.04)
        blazer(f, fl, folds=[], y0=8.5, y1=9.3, k=1.12)
        f.fill(f.m_line((x0, 9.15), (x1, 9.15), 0.05, 0.05), "#050a16", 0.7)
    notebook(f)
    pocket_welt(f)
    # the front edge stitching and the hem
    D.stitch_seam(f, [(0.25, 0.4), (0.18, 11.6)], "#8aa0d0", 0.03, dash=(0.16, 0.12))
    D.stitch_seam(f, [(7.75, 0.4), (7.82, 11.6)], "#8aa0d0", 0.03, dash=(0.16, 0.12))
    H.trim_line(f, [(-0.1, 11.7), (8.1, 11.7)], "#070d1e", width=0.14, outline=0.0, spec=0.0)
    f.fill(f.m_band(11.75, 12.0), "#000000", a=0.3)


def body_back(f):
    blazer(f, f.m_all(), folds=[1.4, 2.9, 5.1, 6.6], y0=0.0, y1=12.0)
    H.cloth(f, f.m_band(0.0, 0.7), SHIRT, top=1.03, bottom=0.86, round_=0.0, fold_xs=[], weave_k=0.0)
    f.fill(f.m_band(0.7, 1.4), NAVY_L, 0.6)
    H.shadow_under(f, 0.75, 0.5, 0.3)
    f.polyline([(0.0, 3.2), (2.2, 3.55), (4.0, 3.4), (5.8, 3.55), (8.0, 3.2)], "#070d1e", 0.09, a=0.5)
    f.line((4.0, 0.9), (4.0, 11.9), "#070d1e", 0.09, 0.09, a=0.7)                                    # the centre seam
    D.stitch_seam(f, [(4.2, 1.2), (4.2, 11.7)], "#8aa0d0", 0.03, dash=(0.16, 0.12))
    for sg in (-1, 1):                                                                              # the side darts
        f.polyline([(4.0 + sg * 2.2, 3.6), (4.0 + sg * 1.9, 6.5), (4.0 + sg * 2.15, 9.0)], "#070d1e", 0.07, a=0.45)
    f.poly([(3.8, 9.4), (4.2, 9.4), (4.45, 12.1), (3.55, 12.1)], "#05091a", a=0.7)                  # the vent
    H.trim_line(f, [(-0.1, 11.7), (8.1, 11.7)], "#070d1e", width=0.14, outline=0.0, spec=0.0)


def body_side(f, nm):
    blazer(f, f.m_all(), folds=[1.2, 2.8], y0=0.0, y1=12.0)
    f.line((2.0, 1.0), (2.0, 11.7), "#070d1e", 0.08, 0.08, a=0.6)                                    # the side seam
    D.stitch_seam(f, [(2.2, 1.3), (2.2, 11.6)], "#8aa0d0", 0.03, dash=(0.16, 0.12))
    H.trim_line(f, [(-0.1, 11.7), (4.1, 11.7)], "#070d1e", width=0.14, outline=0.0, spec=0.0)


def body(cv):
    for f in D.lay(cv, "body", "front"):
        body_front(f)
    for f in D.lay(cv, "body", "back"):
        body_back(f)
    for nm in ("west", "east"):
        for f in D.lay(cv, "body", nm):
            body_side(f, nm)
    t = cv.face("body", "top")
    t.fill(t.m_all(), NAVY_L)
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), TROUS)
    cv.face("body", "top", True).fill(cv.face("body", "top", True).m_all(), NAVY_L)


# ================================================================================================ the arms
def arm(cv, part):
    left = part == "arm_l"
    for nm in SIDES:
        for f in D.lay(cv, part, nm):
            blazer(f, f.m_band(0.0, 10.4), folds=[0.8, 2.0] if f.w <= 3 else [1.0, 2.6], y0=0.0, y1=10.4)
            f.polyline([(0.1, 6.6), (1.2, 6.35), (2.3, 6.65), (f.w, 6.5)], "#070d1e", 0.08, a=0.45)      # the elbow crease
            H.trim_line(f, [(0.0, 0.5), (f.w, 0.5)], "#070d1e", width=0.07, outline=0.0, spec=0.0)       # the shoulder seam
            # the blazer cuff with three small buttons, the white shirt cuff and the hand
            H.trim_line(f, [(0.0, 9.3), (f.w, 9.3)], "#070d1e", width=0.1, outline=0.0, spec=0.0)
            H.shadow_under(f, 10.4, 0.5, 0.4)
            cf = f.m_band(10.4, 11.3)
            H.cloth(f, cf, SHIRT, top=1.04, bottom=0.82, round_=0.05, fold_xs=[], weave_k=0.0)
            f.line((0.0, 10.95), (f.w, 10.95), SHIRT_D, 0.04, 0.04, a=0.5)
            hand = f.m_band(11.3, 12.0)
            f.fill(hand, H.lin((0, 11.3), (0, 12.0), [(0, SKIN[1]), (1, SKIN[0])]))
            if nm in ("front", "back"):
                for k in range(1, 3):
                    f.line((k * f.w / 3.0, 11.6), (k * f.w / 3.0, 11.97), H.tone(SKIN[0], 0.7), 0.04, 0.03, a=0.5)
            if nm in ("front", "back", "east" if not left else "west"):
                for k in range(3):
                    button(f, (0.7 + 0.8 * k) if f.w > 3 else 0.55 + 0.95 * k, 9.85, 0.14)
            if left:
                # the thin teal page-edge line round the cuff (the 3.36.0 motif): a bevelled teal band with a lighter upper edge, on both layers and all four faces
                band = f.m_band(8.85, 9.35)
                f.fill(band, H.lin((0, 8.85), (0, 9.35), [(0, "#7af0d8"), (0.4, CUFF), (1, TEAL)]))
                f.fill(f.m_band(8.78, 8.86), "#05100e", 0.5)
                f.fill(f.m_band(9.34, 9.44), "#05100e", 0.5)
    for ov in (False, True):
        t = cv.face(part, "top", ov)
        t.fill(t.m_all(), NAVY_L)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), SKIN[0])


# ================================================================================================ the legs
def leg(cv, part):
    right = part == "leg_r"
    for nm in SIDES:
        for f in D.lay(cv, part, nm):
            H.cloth(f, f.m_band(0.0, 9.3), TROUS, top=1.1, bottom=0.82, round_=0.12, fold_xs=None, weave_k=0.04)
            H.folds(f, f.m_band(0.0, 9.3), [0.9, 3.0], depth=0.2, width=0.25, sway=0.08, y0=0.3, y1=9.2)
            if nm == "front":                                                                      # the sharp crease
                f.line((2.0, 0.0), (2.0, 9.2), "#04060e", 0.09, 0.09, a=0.6)
                f.line((2.13, 0.0), (2.13, 9.2), "#4a5478", 0.06, 0.06, a=0.5)
            if nm == "front":
                D.slash_pocket(f, right, TROUS, dark=0.5, light=1.7)
            for k in range(2):
                f.line((0.2, 5.0 + k * 0.45), (f.w - 0.2, 5.12 + k * 0.45), "#04060e", 0.07, 0.05, a=0.4)
            # the trouser hem breaks over the polished shoe
            H.shadow_under(f, 9.1, 0.45, 0.4)
            shoe = f.m_band(9.45, 12.0)
            f.fill(shoe, H.lin((0, 9.45), (0, 12.0), [(0, "#26283a"), (0.4, SHOE), (1, "#04040a")]))
            H.sheen(f, shoe, [0.9, 2.9], width=0.16, strength=0.5, tilt=0.03, y0=9.8, y1=11.6, color="#9aa4d0")
            if nm == "front":
                for k in range(3):                                                                 # the laces
                    f.line((1.2, 9.9 + k * 0.38), (2.8, 10.02 + k * 0.38), "#58607e", 0.05, 0.05, a=0.8)
                f.polyline([(0.3, 11.1), (1.3, 10.78), (2.7, 10.78), (3.7, 11.1)], "#2c3048", 0.08, a=0.8)
                f.fill(f.m_ellipse(1.4, 11.0, 0.9, 0.25, soft=3.0), "#aab4e0", 0.28)
            f.fill(f.m_band(11.7, 12.0), "#000000", a=0.5)
    t = cv.face(part, "top")
    t.fill(t.m_all(), TROUS)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), "#04040a")
    D.clear_top_bottom_overlay(cv, part)


def paint(spec, cid, masks):
    cv = H.Canvas(256, True)
    D.base_fill(cv, NAVY)
    head(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    cv.face("head", "top", True).clear()
    skin, glow = cv.finish()
    return skin, glow, "anime_short_open", HAIR, True

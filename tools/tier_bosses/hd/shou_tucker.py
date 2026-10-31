"""Shou Tucker (Fullmetal Alchemist, the Sewing-Life Alchemist), HD 256x256 (3.38.0, group d of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_d.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/shou_tucker.py): a thin, stooped man with shaggy brown hair, round wire glasses over tired eyes with heavy bags, stubble and a mild, too
gentle smile; a warm-white laboratory coat (a green-brown alchemical stain, a breast pocket full of pens) open over a red-brown waistcoat, a cream shirt and a loosened olive tie, the sleeves rolled
to the elbow (bare forearms, a silver watch), dark-brown trousers and worn brown shoes.  HD adds: a really drawn tired face (half-shut amber eyes, bags, stubble, a small smile), real thin round
glasses (a one-texel wire ring on the second layer, a soft shadow ring on the base, the temple arms on the sides), notch lapels and stitched seams, pens in the pocket (blue, red, black), buttons, a
stitched patch (he is the sewing-life alchemist) and a faint chalk transmutation circle on the back, rolled cuffs with a fold and a stitch line, forearms with tendons, a watch, creased trousers, worn
shoes with laces.
Hair: `anime_short_open` (shaggy, the forehead and the glasses stay visible; `anime_swept` hid the eyes), brown.  Original art from memory of the canon look (rule 10: no wiki could be fetched).  No glow.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402
import _dlib as D  # noqa: E402

SKIN = ("#c89c7c", "#eec7a6", "#fbe0c6")
HAIR = (86, 62, 46)
HAIRC = "#5a4030"
COAT = "#f1ebdc"
COAT_D = "#b9ae98"
VEST, VEST_D, VEST_L = "#8a4a2e", "#4e2616", "#b46e48"
SHIRT = "#f7f1de"
TIE = "#62802c"
TIE_D = "#3a521a"
TROUS = "#6a4a36"
SHOE = "#4c3022"
SIL = "#cfd5df"
SIDES = D.SIDES


# ================================================================================================ the head
def glasses(cv):
    """Thin round wire glasses: a one-texel ring on the second layer (the game draws it as a cutout), a soft shadow ring on the base layer, a bridge and the temple arms on the sides."""
    for ov in (False, True):
        f = cv.face("head", "front", ov)
        rim_c = "#9aa4b6" if ov else "#6a4c3a"
        for cx in (2.3, 5.7):
            m = f.m_ring(cx, 4.95, 1.38, 0.2 if ov else 0.28, soft=0.7 if ov else 1.4)
            if ov:
                f.fill(m, rim_c, 1.0)
                f.fill(f.m_ring(cx, 4.95, 1.38, 0.08, soft=0.6) * f.m_poly([(cx - 1.4, 3.5), (cx - 0.2, 3.5), (cx - 1.4, 4.5)]), "#ffffff", 1.0)
                f.fill(f.m_ring(cx, 4.95, 1.38, 0.2, soft=0.7) * f.m_poly([(cx - 1.5, 5.5), (cx + 1.5, 5.5), (cx + 1.5, 6.5), (cx - 1.5, 6.5)]), "#8a94a6", 1.0)    # the lower rim in shade
                f.fill(f.m_line((cx - 0.75, 4.3), (cx - 0.15, 4.0), 0.13, 0.1), "#ffffff", 1.0)        # a glint on the lens
            else:
                f.fill(m, rim_c, 0.55)
        br = f.m_polyline([(3.62, 4.6), (4.0, 4.38), (4.38, 4.6)], 0.2 if ov else 0.26, soft=0.7)
        f.fill(br, rim_c, 1.0 if ov else 0.55)
    for ov in (False, True):
        for nm in ("west", "east"):
            f = cv.face("head", nm, ov)
            front_x = 8.0 if nm == "west" else 0.0
            sgn = -1.0 if nm == "west" else 1.0
            pts = [(front_x, 4.7), (front_x + sgn * 1.6, 4.62), (front_x + sgn * 3.2, 4.64), (front_x + sgn * 3.6, 5.3)]
            f.fill(f.m_polyline(pts, 0.2 if ov else 0.26, soft=0.7), "#9aa4b6" if ov else "#6a4c3a", 1.0 if ov else 0.5)


def head(cv):
    D.head_lod(cv, SKIN, HAIRC, sb_x=7.2, sb_y=3.8, fringe=1.6, ear_x=4.4, nape=5.9, back_nape=6.2)
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(HAIRC, 0.95), hairline=1.7, wave=0.9, shadow=0.24, side=0.07)
    # a long thin face: cheek hollows under the cheekbones, a narrow jaw
    for sg in (-1, 1):
        f.fill(f.m_ellipse(4.0 + sg * 3.3, 6.5, 0.8, 1.6, soft=3.0), H.tone(SKIN[0], 0.8), a=0.34)
    # worried thin brows, the inner end raised
    for sg in (-1, 1):
        H.brow(f, (4.0 + sg * 0.9, 3.15), (4.0 + sg * 3.2, 3.7), "#4a3424", 0.26, 0.14, arch=0.1)
    # half-shut tired amber eyes: a heavy lid over the iris, deep bags under
    iris = ("#3c2a16", "#80602c", "#cca060")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 5.0, 2.3, 1.3, iris, lash="#2a1a10", sclera="#efe8d6", tilt=-0.1, pupil="#140c06", hi=0.8, lid_shadow=0.5, lash_w=0.34, flick=0.08, outer=(-1 if cx < 4 else 1))
        f.fill(f.m_band(4.25, 4.75, x0=cx - 1.2, x1=cx + 1.2), SKIN[0], 0.2)                       # the heavy lid
        f.fill(f.m_ellipse(cx, 6.0, 1.25, 0.4, soft=3.0), "#7a5a82", a=0.36)                       # the bags
        f.polyline([(cx - 1.1, 6.15), (cx, 6.4), (cx + 1.1, 6.15)], H.tone(SKIN[0], 0.72), 0.07, a=0.5)
    H.nose(f, 4.0, 6.2, "#c4887a", 0.3)
    f.line((3.72, 4.9), (3.62, 6.0), H.tone(SKIN[0], 0.78), 0.1, 0.16, a=0.4)
    f.line((4.28, 4.9), (4.38, 6.0), H.tone(SKIN[0], 0.78), 0.1, 0.16, a=0.4)
    # a small, mild, tired smile and a thin stubble beard
    H.mouth_smile(f, 4.0, 7.2, 2.0, color="#9a5a50", lip="#cf8e7c", depth=0.15, thick=0.14)
    f.line((5.0, 7.34), (5.3, 7.12), "#9a5a50", 0.1, 0.07, a=0.8)
    for sg in (-1, 1):
        f.polyline([(4.0 + sg * 1.45, 6.45), (4.0 + sg * 1.75, 7.0), (4.0 + sg * 1.55, 7.4)], H.tone(SKIN[0], 0.7), 0.07, a=0.4)
    beard = f.m_poly([(0.5, 6.4), (7.5, 6.4), (7.3, 8.0), (0.7, 8.0)])
    D.noise_speckle(f, beard, "#5a4234", n=60, seed=5, a=0.4, r=0.032, area=(0.5, 6.4, 7.5, 8.0))
    f.fill(f.m_ellipse(4.0, 7.65, 2.6, 0.85, soft=3.0), "#6a5242", a=0.2)
    glasses(cv)


# ================================================================================================ pieces
def coat(f, mask, folds=(), y0=0.0, y1=12.0):
    H.cloth(f, mask, COAT, top=1.05, bottom=0.8, round_=0.08, fold_xs=None, weave_k=0.025)
    H.folds(f, mask, list(folds), depth=0.11, width=0.3, sway=0.1, y0=y0, y1=y1)


def button(f, x, y, r=0.2, c="#d8c8a0"):
    H.stud(f, x, y, r, c, rim_color=H.tone(c, 0.5), spec=0.7)
    f.fill(f.m_ellipse(x, y, r * 0.45, r * 0.45), H.tone(c, 0.6), 0.5)


def pens(f, x, y):
    """Three pens in a breast pocket: blue, red and black, sticking up at slightly different heights and angles."""
    for dx, ln, c, ang in ((0.0, 0.95, "#3a64c8", -0.12), (0.46, 1.15, "#c43a3a", 0.0), (0.92, 0.85, "#2a2a32", 0.1)):
        x0, y0 = x + dx, y
        x1, y1 = x0 + ang * ln, y0 - ln
        f.fill(f.m_line((x0 + 0.05, y0 + 0.05), (x1 + 0.05, y1 + 0.05), 0.24, 0.24), "#2a1a10", 0.45)
        f.fill(f.m_line((x0, y0), (x1, y1), 0.22, 0.22), H.lin((x0 - 0.15, 0), (x0 + 0.15, 0), [(0, H.tone(c, 1.3)), (1, H.tone(c, 0.65))]))
        f.fill(f.m_line((x1 - ang * 0.12, y1 + 0.12), (x1, y1), 0.24, 0.24), SIL, 0.95)
        f.line((x0 + 0.1, y0), (x0 + 0.1 + ang * 0.5, y0 - 0.5), "#e8ecf2", 0.05, 0.05, a=0.8)


def stain(f, cx, cy, s=1.0):
    """The alchemical smear on the coat: an irregular green-brown blot with a darker rim and a few drips."""
    pts = []
    for i in range(14):
        a = math.tau * i / 14
        r = (0.95 + 0.4 * math.sin(i * 2.3 + 1.1) + 0.25 * math.sin(i * 4.1)) * s
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r * 0.8))
    m = f.m_poly(pts)
    f.fill(f.m_poly([(x + 0.05, y + 0.06) for x, y in pts]), "#2a2a10", 0.3)
    f.fill(m, H.rad((cx - 0.2, cy - 0.2), 1.4 * s, [(0, "#8aa040"), (0.55, "#667a30"), (1, "#3c4a1c")]), 0.85)
    f.fill(f.m_ring(cx, cy, 0.9 * s, 0.12) * m, "#2a3412", 0.45)
    for dx, ln in ((-0.4, 0.8), (0.3, 1.1)):
        f.fill(f.m_line((cx + dx * s, cy + 0.6 * s), (cx + dx * s, cy + (0.6 + ln) * s), 0.14, 0.09), "#667a30", 0.8)


# ================================================================================================ the body
def body_front(f):
    # the shirt and the waistcoat under the open lab coat
    H.cloth(f, f.m_all(), SHIRT, top=1.04, bottom=0.82, round_=0.06, fold_xs=[], weave_k=0.02)
    vest = f.m_poly([(1.9, 1.0), (6.1, 1.0), (6.3, 8.4), (4.0, 9.5), (1.7, 8.4)])
    H.cloth(f, vest, VEST, top=1.14, bottom=0.78, round_=0.06, fold_xs=None, weave_k=0.04)
    H.folds(f, vest, [3.1, 5.0], depth=0.1, width=0.25, sway=0.1, y0=1.5, y1=8.6)
    H.sheen(f, vest, [2.6], width=0.2, strength=0.14, tilt=0.03, y0=2.0, y1=8.0, color="#f0a888")
    H.trim_line(f, [(1.9, 1.0), (1.7, 8.4), (4.0, 9.5), (6.3, 8.4), (6.1, 1.0)], VEST_D, width=0.12, outline=0.0, spec=0.0)
    D.stitch_seam(f, [(2.2, 1.6), (2.05, 8.15), (4.0, 9.1), (5.95, 8.15), (5.8, 1.6)], "#d4a07a", 0.035, dash=(0.18, 0.14))
    for y in (3.9, 5.2, 6.5, 7.8):
        button(f, 4.0, y, 0.22, "#d8c08c")
    # the shirt collar and the loosened green tie
    H.cloth(f, f.m_poly([(2.9, -0.1), (5.1, -0.1), (4.0, 1.9)]), SHIRT, top=1.02, bottom=0.86, round_=0.0, fold_xs=[], weave_k=0.0)
    for sg in (-1, 1):
        f.fill(f.m_poly([(4.0 + sg * 0.1, 0.05), (4.0 + sg * 1.35, 0.05), (4.0 + sg * 1.0, 1.15), (4.0 + sg * 0.1, 1.15)]), SHIRT, 0.0)
    knot = f.m_poly([(3.55, 1.25), (4.45, 1.25), (4.3, 2.0), (3.7, 2.0)])
    H.cloth(f, knot, TIE, top=1.18, bottom=0.8, round_=0.0, fold_xs=[], weave_k=0.0)
    tie = f.m_poly([(3.7, 2.0), (4.3, 2.0), (4.65, 6.3), (4.0, 6.8), (3.35, 6.3)])
    H.cloth(f, tie, TIE, top=1.16, bottom=0.76, round_=0.0, fold_xs=[], weave_k=0.0)
    f.polyline([(3.5, 2.3), (3.7, 5.2), (3.55, 6.3)], TIE_D, 0.06, a=0.6)
    f.polyline([(4.5, 2.3), (4.3, 5.2), (4.45, 6.3)], H.tone(TIE, 1.35), 0.05, a=0.5)
    # the belt buckle under the waistcoat
    D.leather(f, f.m_band(9.5, 10.3), "#3a2418", 9.5, 10.3)
    D.buckle(f, 4.0, 9.9, 1.1, 0.7, color="#c8ced8", hole="#3a2418")
    # trousers
    H.cloth(f, f.m_band(10.3, 12.0), TROUS, top=1.1, bottom=0.8, round_=0.08, fold_xs=None, weave_k=0.03)
    H.shadow_under(f, 10.3, 0.5, 0.3)
    # the two panels of the open lab coat: wide notch lapels, a pen-filled breast pocket, a hip pocket, the stain
    for sg in (0, 1):
        pts = [(-0.1, -0.1), (2.2, -0.1), (2.0, 1.2), (1.5, 2.9), (2.0, 4.0), (1.75, 9.0), (1.9, 12.1), (-0.1, 12.1)]
        if sg:
            pts = D.mirror(pts, 8.0)
        m = f.m_poly(pts)
        f.fill(f.m_poly([(x + (0.1 if not sg else -0.1), y + 0.04) for x, y in pts]), "#241a10", 0.34)
        coat(f, m, folds=[0.5 if not sg else 7.5, 1.1 if not sg else 6.9], y0=0.5, y1=12.0)
        # the lapel: a lighter facing along the inner edge from the collar to the first notch
        lap = f.m_poly([pts[1], pts[2], pts[3], pts[4], (pts[4][0] + (-0.5 if not sg else 0.5), 4.2), (pts[3][0] + (-0.5 if not sg else 0.5), 3.0), (pts[1][0] + (-0.9 if not sg else 0.9), 0.0)])
        H.bevel(f, lap, depth=0.2, gain=0.5)
        f.fill(f.m_polyline(pts[1:6], 0.1), COAT_D, 0.6)
        D.stitch_seam(f, [(x + (-0.28 if not sg else 0.28), y) for x, y in pts[2:6]], "#c8bca0", 0.035, dash=(0.2, 0.15))
    # the breast pocket on the viewer's right panel with its pens, the hip pocket flap on the left
    pk = f.m_rect(6.3, 3.5, 7.7, 5.1, 0.08)
    f.fill(f.m_rect(6.34, 3.56, 7.76, 5.16, 0.08), "#2a1e12", 0.3)
    H.cloth(f, pk, COAT, top=1.06, bottom=0.88, round_=0.0, fold_xs=[], weave_k=0.02)
    f.fill(f.m_band(3.5, 3.72, x0=6.3, x1=7.7), COAT_D, 0.8)
    D.stitch_seam(f, [(6.4, 4.95), (7.6, 4.95)], "#c8bca0", 0.035)
    pens(f, 6.55, 3.65)
    fl = f.m_rect(0.3, 9.4, 1.7, 10.1, 0.06)
    H.cloth(f, fl, COAT, top=1.08, bottom=0.85, round_=0.0, fold_xs=[], weave_k=0.02)
    f.fill(f.m_band(9.95, 10.12, x0=0.3, x1=1.7), COAT_D, 0.7)
    stain(f, 1.0, 6.4, 0.75)
    # the hem of the coat
    H.trim_line(f, [(-0.1, 11.85), (2.0, 11.85)], COAT_D, width=0.12, outline=0.0, spec=0.0)
    H.trim_line(f, [(6.0, 11.85), (8.1, 11.85)], COAT_D, width=0.12, outline=0.0, spec=0.0)


def chalk_circle(f, cx, cy, r=2.5):
    """A faint chalk transmutation circle (original): two rings, a hexagram of lines, a few tick marks; the lines are broken and smudged like chalk dust."""
    c = "#9aa6bc"
    f.fill(f.m_ring(cx, cy, r, 0.12), c, 0.34)
    f.fill(f.m_ring(cx, cy, r * 0.82, 0.07), c, 0.3)
    for k in range(2):
        pts = [(cx + math.cos(math.radians(a + 60 * k + (0 if k == 0 else 0))) * r * 0.82, cy + math.sin(math.radians(a + 60 * k)) * r * 0.82) for a in (-90, 30, 150, -90)]
        f.fill(f.m_polyline(pts, 0.08), c, 0.3)
    for i in range(12):
        a = math.tau * i / 12
        f.fill(f.m_line((cx + math.cos(a) * r * 0.9, cy + math.sin(a) * r * 0.9), (cx + math.cos(a) * r * 1.12, cy + math.sin(a) * r * 1.12), 0.08, 0.07), c, 0.3)
    f.fill(f.m_ellipse(cx, cy, r * 1.15, r * 1.15, soft=4.0) * (np.sin(f.X * 7.0) * np.sin(f.Y * 5.0) > 0.2), "#ffffff", 0.18)


def body_back(f):
    coat(f, f.m_all(), folds=[1.3, 2.7, 5.3, 6.7], y0=0.0, y1=12.0)
    H.cloth(f, f.m_band(0.0, 1.0), H.tone(COAT, 1.02), top=1.04, bottom=0.88, round_=0.0, fold_xs=[], weave_k=0.0)
    H.trim_line(f, [(0.0, 1.05), (8.0, 1.05)], COAT_D, width=0.1, outline=0.0, spec=0.0)
    f.polyline([(0.0, 3.0), (2.0, 3.35), (4.0, 3.2), (6.0, 3.35), (8.0, 3.0)], COAT_D, 0.08, a=0.6)
    D.stitch_seam(f, [(4.0, 1.4), (4.0, 12.0)], "#c8bca0", 0.04, dash=(0.22, 0.16))
    f.fill(f.m_poly([(3.85, 9.0), (4.15, 9.0), (4.35, 12.0), (3.65, 12.0)]), COAT_D, 0.55)           # the centre vent
    chalk_circle(f, 4.0, 6.0, 2.4)
    # the stitched patch (the sewing-life alchemist): a square of other cloth sewn on with thick dark stitches
    patch = f.m_rect(0.8, 8.3, 3.0, 10.4, 0.06)
    H.cloth(f, patch, "#d9d1b6", top=1.05, bottom=0.88, round_=0.0, fold_xs=[], weave_k=0.04)
    for (p0, p1) in (((0.8, 8.3), (3.0, 8.3)), ((3.0, 8.3), (3.0, 10.4)), ((3.0, 10.4), (0.8, 10.4)), ((0.8, 10.4), (0.8, 8.3))):
        H.stitch(f, [p0, p1], "#5a4630", width=0.07, dash=(0.26, 0.16), shadow=0.2)
    f.line((1.0, 8.5), (2.8, 10.2), "#5a4630", 0.05, 0.05, a=0.5)
    f.line((2.8, 8.5), (1.0, 10.2), "#5a4630", 0.05, 0.05, a=0.5)
    H.trim_line(f, [(-0.1, 11.85), (8.1, 11.85)], COAT_D, width=0.12, outline=0.0, spec=0.0)
    # frayed hem
    H.scallops(f, f.m_band(11.2, 12.2), 11.2, rows=1, cell=0.6, depth=0.8, base=H.tone(COAT, 0.9), tip=COAT, edge=COAT_D, edge_a=0.4, shadow=0.0)


def body_side(f, nm):
    coat(f, f.m_all(), folds=[1.2, 2.8])
    H.cloth(f, f.m_band(0.0, 1.0), H.tone(COAT, 1.02), top=1.04, bottom=0.88, round_=0.0, fold_xs=[], weave_k=0.0)
    f.line((2.0, 1.2), (2.0, 12.0), COAT_D, 0.07, 0.07, a=0.5)                                       # the side seam
    D.stitch_seam(f, [(2.25, 1.4), (2.25, 12.0)], "#c8bca0", 0.035)
    f.fill(f.m_rect(1.2, 8.2, 2.0, 9.9, 0.06), "#24180c", 0.35)                                       # the slit of a side pocket
    H.trim_line(f, [(-0.1, 11.85), (4.1, 11.85)], COAT_D, width=0.12, outline=0.0, spec=0.0)


def body(cv):
    for f in D.lay(cv, "body", "front"):
        body_front(f)
    for f in D.lay(cv, "body", "back"):
        body_back(f)
    for nm in ("west", "east"):
        for f in D.lay(cv, "body", nm):
            body_side(f, nm)
    t = cv.face("body", "top")
    t.fill(t.m_all(), COAT)
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), TROUS)
    cv.face("body", "top", True).fill(cv.face("body", "top", True).m_all(), COAT)


# ================================================================================================ the arms
def arm(cv, part):
    left = part == "arm_l"
    for nm in SIDES:
        for f in D.lay(cv, part, nm):
            # the lab coat sleeve down to the rolled cuff
            coat(f, f.m_band(0.0, 5.7), folds=[0.9, 2.1] if f.w > 3 else [0.8, 2.2], y0=0.0, y1=5.5)
            f.polyline([(0.2, 3.9), (1.4, 3.6), (2.4, 3.95)], COAT_D, 0.08, a=0.45)
            # the cuff: a thick roll of cloth with a light top edge, a shadow underneath and a stitch line
            roll = f.m_band(5.0, 6.5)
            f.fill(roll, H.lin((0, 5.0), (0, 6.5), [(0, H.tone(COAT, 1.04)), (0.45, COAT), (1, H.tone(COAT, 0.8))]))
            H.bevel(f, roll, depth=0.3, gain=0.8, spec=0.1)
            f.polyline([(0.0, 5.85), (f.w, 5.85)], COAT_D, 0.07, a=0.55)
            D.stitch_seam(f, [(0.0, 5.35), (f.w, 5.35)], "#c8bca0", 0.035)
            H.shadow_under(f, 6.5, 0.6, 0.34)
            # the bare forearm: skin with a tendon line and the watch
            fa = f.m_band(6.5, 12.0)
            f.fill(fa, H.lin((0, 6.5), (0, 12.0), [(0, SKIN[1]), (0.6, H.tone(SKIN[1], 0.97)), (1, SKIN[2])]))
            f.shade(fa, 1.0 - 0.1 * H.smoothstep(0.55, 1.0, (f.X if f.light[0] > 0 else f.w - f.X) / f.w))
            if nm in ("front", "back"):
                f.line((f.w * 0.42, 7.2), (f.w * 0.5, 10.0), H.tone(SKIN[0], 0.8), 0.07, 0.04, a=0.4)
                f.line((f.w * 0.62, 7.4), (f.w * 0.58, 9.6), H.tone(SKIN[0], 0.85), 0.05, 0.03, a=0.3)
            # the hand: long fingers
            hand = f.m_band(10.4, 12.0)
            f.fill(hand, H.lin((0, 10.4), (0, 12.0), [(0, SKIN[1]), (1, H.tone(SKIN[1], 0.93))]))
            if nm in ("front", "back"):
                for k in range(1, 4 if f.w > 3 else 3):
                    f.line((k * f.w / (4.0 if f.w > 3 else 3.0), 11.1), (k * f.w / (4.0 if f.w > 3 else 3.0), 11.95), H.tone(SKIN[0], 0.68), 0.05, 0.04, a=0.55)
            if left:
                # the silver watch: a band with a small bevelled face
                band = f.m_band(9.1, 10.0)
                f.fill(band, H.metal_paint(9.1, 10.0, "#aab3c2", contrast=0.6))
                H.bevel(f, band, depth=0.2, gain=0.7, spec=0.3)
                if nm == "front":
                    H.stud(f, f.w / 2.0, 9.55, 0.34, "#e8edf4", rim_color="#5a6272")
                    f.fill(f.m_ellipse(f.w / 2.0, 9.55, 0.2, 0.2), "#1c2438", 0.9)
                    f.line((f.w / 2.0, 9.55), (f.w / 2.0 + 0.1, 9.4), "#e8edf4", 0.04, 0.03, a=0.9)
        if nm == "front":
            for f in D.lay(cv, part, nm):
                if not left:
                    stain(f, 1.5, 3.0, 0.45)
    for ov in (False, True):
        t = cv.face(part, "top", ov)
        t.fill(t.m_all(), COAT)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), SKIN[0])


# ================================================================================================ the legs
def leg(cv, part):
    right = part == "leg_r"
    for nm in SIDES:
        for f in D.lay(cv, part, nm):
            H.cloth(f, f.m_band(0.0, 9.0), TROUS, top=1.12, bottom=0.78, round_=0.12, fold_xs=None, weave_k=0.04)
            H.folds(f, f.m_band(0.0, 9.0), [1.0, 2.9], depth=0.12, width=0.22, sway=0.08, y0=0.3, y1=9.0)
            if nm == "front":
                f.line((2.0, 0.0), (2.0, 8.8), H.tone(TROUS, 0.62), 0.1, 0.1, a=0.5)
                f.line((2.12, 0.0), (2.12, 8.8), H.tone(TROUS, 1.4), 0.07, 0.07, a=0.4)
            if nm == "front":
                D.slash_pocket(f, right, TROUS)
            for k in range(3):                                                                    # the knee bags
                f.line((0.2, 5.0 + k * 0.4), (f.w - 0.2, 5.15 + k * 0.4), H.tone(TROUS, 0.68), 0.07, 0.05, a=0.38)
            # the cuff turn-up and the worn shoe
            H.trim_line(f, [(0.0, 8.7), (f.w, 8.7)], H.tone(TROUS, 0.6), width=0.1, outline=0.0, spec=0.0)
            H.shadow_under(f, 8.8, 0.4, 0.3)
            shoe = f.m_band(9.1, 12.0)
            f.fill(shoe, H.lin((0, 9.1), (0, 12.0), [(0, H.tone(SHOE, 1.25)), (0.5, SHOE), (1, H.tone(SHOE, 0.55))]))
            H.sheen(f, shoe, [1.0, 2.9], width=0.2, strength=0.22, y0=9.6, y1=11.5)
            if nm == "front":
                for k in range(3):                                                                # the laces
                    f.line((1.1, 9.6 + k * 0.42), (2.9, 9.76 + k * 0.42), "#e0d4b8", 0.06, 0.06, a=0.8)
                f.polyline([(0.3, 11.2), (1.3, 10.85), (2.7, 10.85), (3.7, 11.2)], H.tone(SHOE, 0.5), 0.1, a=0.7)
                f.fill(f.m_ellipse(1.5, 11.3, 0.9, 0.3, soft=3.0), "#a07858", 0.22)               # scuffed toe
            f.fill(f.m_band(11.7, 12.0), "#000000", a=0.55)
    t = cv.face(part, "top")
    t.fill(t.m_all(), TROUS)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), "#1a100a")
    D.clear_top_bottom_overlay(cv, part)


def paint(spec, cid, masks):
    cv = H.Canvas(256, True)
    D.base_fill(cv, COAT)
    head(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    cv.face("head", "top", True).clear()
    skin, glow = cv.finish()
    return skin, glow, "anime_short_open", HAIR, True

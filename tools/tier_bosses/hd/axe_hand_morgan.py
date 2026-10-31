"""Axe-Hand Morgan (One Piece, the Marine captain of Shells Town), HD 256x256 (3.38.0, group d of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_d.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/axe_hand_morgan.py): a huge man with a flat-topped crop and a slab of a jaw, heavy angry brows, small hard steel-blue eyes, stubble;
the deep-navy Marine captain coat with cream facings (collar, lapels, cuffs), a double row of gold buttons and gold piping, enormous steel epaulettes with gold bullion fringe, a black double belt
with a big gold buckle, light grey-blue trousers, black boots with steel cuffs, and his right hand replaced by a great steel axe.  HD adds: a really drawn face (square jaw shading, stubble, scowl),
a flat-top hairline with sideburns on the base head, wide peak lapels with gold piping, a white cravat, a shirt, a crest of gold wings and an anchor on the back, riveted steel epaulettes with ribs and
fringe, cream cuffs with buttons, a riveted steel axe head with a bright cutting edge, creased trousers, boots with riveted steel greaves.
Hair: `anime_flattop` (the flat-top block of the hair rework), brown.  Original art from memory of the canon look (rule 10: no wiki could be fetched).  No glow (nothing in the outfit shines by itself).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402
import _dlib as D  # noqa: E402

SKIN = ("#c58f68", "#e8b48c", "#f8d6b4")
HAIR = (96, 66, 38)
HAIRC = "#604226"
NAVY = "#243e78"
NAVY_D = "#101c3c"
CREAM = "#f1ead6"
CREAM_D = "#b3a88c"
GOLD, GOLD_D, GOLD_L = "#e0aa24", "#8a5e10", "#ffe27a"
STEEL, STEEL_D, STEEL_L = "#667592", "#262e42", "#dfe8f6"
TROUS = "#7784a6"
K0, K1, K2 = "#08080c", "#17161e", "#34323f"
SIDES = D.SIDES


# ================================================================================================ the head
def head(cv):
    D.head_lod(cv, SKIN, HAIRC, sb_x=7.1, sb_y=4.4, fringe=1.4, ear_x=4.3, nape=4.3, back_nape=4.2)
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=None)
    # the square slab of a jaw: the cheek planes step darker toward the corners of the chin, the chin is a flat block
    f.fill(f.m_poly([(0, 5.5), (1.0, 6.6), (1.2, 8.0), (0, 8.0)]), SKIN[0], 0.55)
    f.fill(f.m_poly([(8, 5.5), (7.0, 6.6), (6.8, 8.0), (8, 8.0)]), SKIN[0], 0.55)
    f.shade(f.m_band(7.15, 8.0), 0.9)
    f.line((1.15, 6.7), (1.15, 7.95), H.tone(SKIN[0], 0.65), 0.1, 0.1, a=0.45)                  # the jaw corners
    f.line((6.85, 6.7), (6.85, 7.95), H.tone(SKIN[0], 0.65), 0.1, 0.1, a=0.45)
    f.line((3.55, 7.65), (4.45, 7.65), H.tone(SKIN[0], 0.8), 0.1, 0.1, a=0.5)                   # the cleft of the chin
    # the flat-top hairline and the long sideburns
    hm = f.m_poly([(-0.2, -0.2), (8.2, -0.2), (8.2, 5.0), (7.4, 5.0), (7.05, 1.6), (0.95, 1.6), (0.6, 5.0), (-0.2, 5.0)])
    D.paint_hair(f, hm, HAIRC, strands=3.2, seed=2, top=1.04, bottom=0.86, flow=0.1)
    f.fill(f.m_polyline([(0.95, 1.6), (7.05, 1.6)], 0.12) * hm, H.tone(HAIRC, 0.45), 0.5)
    f.shade(f.m_band(1.6, 2.5) * (1.0 - hm), 0.86)
    # heavy brows slanting down to the nose: a scowl
    for sg in (-1, 1):
        cx = 4.0
        H.brow(f, (cx + sg * 0.35, 3.95), (cx + sg * 3.35, 2.95), "#2a1c12", 0.56, 0.3, arch=-0.04)
    for x in (3.62, 4.38):
        f.line((x, 3.3), (x, 4.15), H.tone(SKIN[0], 0.72), 0.07, 0.07, a=0.55)                 # the furrow between the brows
    iris = ("#10182c", "#37567f", "#9db4d4")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.85, 2.15, 1.1, iris, lash="#140e0c", sclera="#ece8de", tilt=-0.2, pupil="#080a12", hi=0.55, lid_shadow=0.55, lash_w=0.4, flick=0.04, outer=(-1 if cx < 4 else 1))
        f.fill(f.m_ellipse(cx, 5.65, 1.1, 0.28, soft=3.0), "#7a5a58", a=0.32)                  # eye bags
    # a broad nose: shadow planes on both sides of the bridge, a light bridge, nostrils
    for sg in (-1, 1):
        f.line((4.0 + sg * 0.34, 4.5), (4.0 + sg * 0.62, 6.15), H.tone(SKIN[0], 0.7), 0.2, 0.34, a=0.5)
        f.ellipse(4.0 + sg * 0.62, 6.3, 0.36, 0.2, H.tone(SKIN[0], 0.5), a=0.85)
    f.line((4.0, 4.3), (4.0, 6.0), SKIN[2], 0.16, 0.22, a=0.4)
    f.ellipse(4.0, 6.28, 0.5, 0.3, SKIN[2], a=0.3, soft=2.5)
    for sg in (-1, 1):                                                                            # nasolabial lines
        f.polyline([(4.0 + sg * 1.55, 6.3), (4.0 + sg * 1.95, 7.0), (4.0 + sg * 1.8, 7.5)], H.tone(SKIN[0], 0.68), 0.1, a=0.5)
    # the mouth: wide, hard, turned down at the corners, a heavy lower lip
    H.mouth_smile(f, 4.0, 7.12, 3.3, color="#4a2220", lip="#9c5e52", depth=-0.16, thick=0.24)
    f.fill(f.m_ellipse(4.0, 7.55, 1.2, 0.25, soft=3.0), H.tone(SKIN[0], 0.62), a=0.3)
    # stubble over the jaw, the upper lip and the cheeks
    mask = f.m_poly([(0.3, 5.9), (7.7, 5.9), (7.7, 8.0), (0.3, 8.0)])
    D.noise_speckle(f, mask, "#5a4030", n=46, seed=3, a=0.3, r=0.03, area=(0.6, 6.6, 7.4, 8.0))
    f.shade(f.m_ellipse(4.0, 7.15, 2.9, 1.15, soft=3.0), 0.93, a=0.5)
    # the neck ring and a shadow under the chin on the hat layer is not needed: the 3D hair is the hat layer


# ================================================================================================ pieces
def navy(f, mask, folds=(), y0=0.0, y1=12.0, k=1.0):
    H.cloth(f, mask, H.tone(NAVY, k), top=1.17, bottom=0.76, round_=0.12, fold_xs=list(folds), fold_depth=0.17, fold_y0=y0, fold_y1=y1, weave_k=0.035)


def gold_band(f, y0, y1, **kw):
    return H.trim_band(f, y0, y1, GOLD, **kw)


def gold_line(f, pts, w=0.17):
    return H.trim_line(f, pts, GOLD, width=w, outline=0.5, spec=0.35)


def button(f, x, y, r=0.3):
    H.stud(f, x, y, r, GOLD, spec=0.9)
    f.fill(f.m_ring(x, y, r * 0.55, 0.05), GOLD_D, 0.5)


def anchor(f, cx, cy, s=1.0, color=GOLD_L, shade=GOLD_D):
    """A small ship's anchor: a ring, a shank, a stock, curved flukes."""
    def draw(dx, dy, c, a):
        f.polyline([(cx + dx, cy - 1.0 * s + dy), (cx + dx, cy + 1.05 * s + dy)], c, 0.22 * s, a=a)
        f.polyline([(cx - 0.62 * s + dx, cy - 0.45 * s + dy), (cx + 0.62 * s + dx, cy - 0.45 * s + dy)], c, 0.18 * s, a=a)
        arc = [(cx + math.cos(t) * 0.95 * s + dx, cy + 0.2 * s + math.sin(t) * 0.88 * s + dy) for t in np.linspace(0.18 * math.pi, 0.82 * math.pi, 12)]
        f.polyline(arc, c, 0.2 * s, a=a)
        f.poly([(cx - 0.95 * s * math.cos(0.18 * math.pi) + dx, cy + 0.2 * s + 0.88 * s * math.sin(0.18 * math.pi) + dy - 0.35 * s), (cx - 0.95 * s * math.cos(0.18 * math.pi) + dx - 0.3 * s, cy + 0.2 * s + 0.6 * s + dy), (cx - 0.95 * s * math.cos(0.18 * math.pi) + dx + 0.2 * s, cy + 0.2 * s + 0.62 * s + dy)], c, a=a)
        f.poly([(cx + 0.95 * s * math.cos(0.18 * math.pi) + dx, cy + 0.2 * s + 0.88 * s * math.sin(0.18 * math.pi) + dy - 0.35 * s), (cx + 0.95 * s * math.cos(0.18 * math.pi) + dx + 0.3 * s, cy + 0.2 * s + 0.6 * s + dy), (cx + 0.95 * s * math.cos(0.18 * math.pi) + dx - 0.2 * s, cy + 0.2 * s + 0.62 * s + dy)], c, a=a)
        f.ellipse(cx + dx, cy - 1.1 * s + dy, 0.26 * s, 0.26 * s, c, a=a)
    draw(0.06, 0.07, "#10182c", 0.6)
    draw(0.0, 0.0, color, 1.0)


def wing(f, cx, cy, sg, w=3.0, h=1.9):
    """One feathered wing of the Marine crest, spread out from (cx, cy) toward the side sg (-1 left, +1 right): five tapering feathers, rising outward."""
    for i in range(5):
        t = i / 4.0
        ang = math.radians(-18 + 52 * t)
        ln = w * (1.0 - 0.34 * t)
        x1, y1 = cx + sg * math.cos(ang) * ln, cy + math.sin(ang) * ln * 0.62 - h * 0.1
        pts = [(cx + sg * 0.15, cy - 0.35 + 0.34 * t), (x1, y1 - 0.3), (x1 + sg * 0.15, y1 + 0.02), (cx + sg * 0.3, cy + 0.2 + 0.3 * t)]
        f.poly([(x + 0.04, y + 0.05) for x, y in pts], "#10182c", a=0.55)
        f.poly(pts, H.lin((cx, cy - 1), (cx, cy + 1.5), [(0, GOLD_L), (1, GOLD)]))
        f.line((cx + sg * 0.3, cy - 0.05 + 0.3 * t), (x1, y1 - 0.12), GOLD_D, 0.05, 0.03, a=0.7)


# ================================================================================================ the body
def lapels(f):
    left = [(1.45, 0.0), (3.1, 0.0), (3.9, 5.3), (3.35, 5.55), (2.15, 3.75), (1.45, 3.0)]
    right = D.mirror(left, 8.0)
    # the shirt and the cravat between the lapels
    shirt = f.m_poly([(3.0, -0.1), (5.0, -0.1), (4.05, 5.5), (3.95, 5.5)])
    H.cloth(f, shirt, "#f6f4ee", top=1.04, bottom=0.82, round_=0.05, fold_xs=[], weave_k=0.0)
    for pts in (left, right):
        m = f.m_poly(pts)
        f.fill(f.m_poly([(x + (0.07 if pts is left else -0.07), y + 0.09) for x, y in pts]), NAVY_D, 0.55)
        H.cloth(f, m, CREAM, top=1.05, bottom=0.88, round_=0.08, fold_xs=[], weave_k=0.025)
        H.bevel(f, m, depth=0.22, gain=0.6)
        H.trim_line(f, pts[1:5] if pts is left else pts[1:5], GOLD, width=0.13, outline=0.45, spec=0.3)
        H.stitch(f, [(x + (0.28 if pts is left else -0.28), y) for x, y in pts[1:4]], CREAM_D, width=0.04, dash=(0.2, 0.16), shadow=0.0)
    # the cravat: a soft white scarf with a knot
    H.cloth(f, f.m_poly([(2.95, -0.1), (5.05, -0.1), (4.55, 1.5), (4.0, 2.1), (3.45, 1.5)]), "#fbfaf4", top=1.02, bottom=0.78, round_=0.04, fold_xs=[3.55, 4.45], fold_depth=0.2, weave_k=0.0)
    f.polyline([(3.1, 0.5), (3.7, 1.35), (4.0, 2.0)], H.tone("#fbfaf4", 0.72), 0.07, a=0.7)
    f.polyline([(4.9, 0.5), (4.3, 1.35), (4.0, 2.0)], H.tone("#fbfaf4", 0.72), 0.07, a=0.7)
    H.stud(f, 4.0, 2.55, 0.17, GOLD_L)                                                           # the tie pin


def body_front(f):
    navy(f, f.m_all(), folds=[1.0, 7.0])
    lapels(f)
    # the closing edge of the double-breasted coat and the two columns of gold buttons
    gold_line(f, [(4.0, 5.55), (4.0, 8.15)], 0.12)
    for x in (2.35, 5.65):
        for y in (6.15, 7.35):
            button(f, x, y, 0.3)
    # a small gold captain's badge over the heart (viewer's right)
    f.rect(5.1, 2.9, 7.0, 3.5, "#0e1630", r=0.1, a=0.55)
    H.stud(f, 6.05, 3.2, 0.33, GOLD_L, rim_color=GOLD_D)
    f.fill(f.m_ring(6.05, 3.2, 0.2, 0.06), GOLD_D, 0.6)
    # the double belt: a thin strap over the main belt, a big gold buckle
    D.leather(f, f.m_band(8.15, 8.7), "#2a2630", 8.15, 8.7, sheen_x=[1.2, 6.4])
    D.leather(f, f.m_band(8.82, 10.0), K1, 8.82, 10.0, sheen_x=[0.9, 1.9, 6.3, 7.1], sheen_strength=0.3)
    D.stitch_seam(f, [(0.0, 9.0), (8.0, 9.0)], "#6a6070", 0.04)
    D.stitch_seam(f, [(0.0, 9.82), (8.0, 9.82)], "#6a6070", 0.04)
    H.shadow_under(f, 10.0, 0.6, 0.34)
    H.shadow_under(f, 8.7, 0.18, 0.22)
    H.trim_band(f, 8.45, 10.2, GOLD, x0=2.55, x1=5.45, r=0.3, outline=0.7)
    f.rect(3.1, 8.95, 4.9, 9.7, H.lin((0, 8.95), (0, 9.7), [(0, "#0a1230"), (1, "#1c2c58")]), r=0.15)
    anchor(f, 4.0, 9.32, 0.36, GOLD_L, GOLD_D)
    for x in (2.85, 5.15):
        H.stud(f, x, 9.32, 0.1, GOLD_L)
    # the coat skirt: pocket flaps with a gold button, a deep fold at the vent, the gold hem
    navy(f, f.m_band(10.0, 12.0), folds=[2.0, 6.0], y0=10.0, y1=12.0, k=1.0)
    for x0, x1 in ((0.5, 2.7), (5.3, 7.5)):
        flap = f.m_rect(x0, 10.35, x1, 11.15, 0.1)
        f.fill(f.m_rect(x0, 10.42, x1 + 0.05, 11.25, 0.1), NAVY_D, 0.5)
        H.cloth(f, flap, NAVY, top=1.22, bottom=0.9, round_=0.0, fold_xs=[], weave_k=0.02)
        H.trim_line(f, [(x0, 11.12), (x1, 11.12)], GOLD, width=0.1, outline=0.4, spec=0.2)
        button(f, (x0 + x1) / 2.0, 10.75, 0.17)
    f.line((4.0, 10.1), (4.0, 12.0), "#070d1e", 0.12, 0.2, a=0.7)
    H.trim_band(f, 11.5, 12.0, GOLD, outline=0.6)


def body_back(f):
    navy(f, f.m_all(), folds=[1.6, 6.4])
    # the cream collar band, the yoke seam, the centre seam with stitching
    H.cloth(f, f.m_band(0.0, 1.3), CREAM, top=1.04, bottom=0.84, round_=0.06, fold_xs=[], weave_k=0.03)
    H.trim_line(f, [(0.0, 1.35), (8.0, 1.35)], GOLD, width=0.13, outline=0.45, spec=0.3)
    H.shadow_under(f, 1.4, 0.5, 0.3)
    f.polyline([(0.0, 3.2), (2.0, 3.6), (4.0, 3.45), (6.0, 3.6), (8.0, 3.2)], NAVY_D, 0.1, a=0.55)
    D.stitch_seam(f, [(4.0, 1.6), (4.0, 8.0)], "#8aa0cc", 0.045, dash=(0.24, 0.18))
    # the crest of the Marines, an original one: gold wings spread over a gold anchor
    wing(f, 3.55, 4.55, -1, w=3.1)
    wing(f, 4.45, 4.55, 1, w=3.1)
    anchor(f, 4.0, 5.55, 0.95)
    # belts: the strap, the main belt, a gold tab with two loops
    D.leather(f, f.m_band(8.15, 8.7), "#2a2630", 8.15, 8.7, sheen_x=[1.2, 6.4])
    D.leather(f, f.m_band(8.82, 10.0), K1, 8.82, 10.0, sheen_x=[0.9, 6.3])
    D.stitch_seam(f, [(0.0, 9.0), (8.0, 9.0)], "#6a6070", 0.04)
    D.stitch_seam(f, [(0.0, 9.82), (8.0, 9.82)], "#6a6070", 0.04)
    H.shadow_under(f, 10.0, 0.6, 0.34)
    H.trim_band(f, 8.6, 10.1, GOLD, x0=3.3, x1=4.7, r=0.2, outline=0.6)
    for x in (2.2, 5.8):
        f.rect(x - 0.2, 8.1, x + 0.2, 10.1, H.tone(K2, 1.1), r=0.1, a=0.9)
    # the skirt with its open vent and the gold hem
    navy(f, f.m_band(10.0, 12.0), folds=[1.4, 6.6], y0=10.0, y1=12.0)
    f.poly([(3.45, 10.05), (4.55, 10.05), (4.85, 12.0), (3.15, 12.0)], "#050a16", a=0.85)
    f.line((3.45, 10.05), (3.15, 12.0), NAVY, 0.1, 0.1, a=0.7)
    f.line((4.55, 10.05), (4.85, 12.0), NAVY, 0.1, 0.1, a=0.7)
    H.trim_band(f, 11.5, 12.0, GOLD, outline=0.6)


def body_side(f, nm):
    navy(f, f.m_all(), folds=[1.4, 2.8])
    H.cloth(f, f.m_band(0.0, 1.3), CREAM, top=1.04, bottom=0.84, round_=0.06, fold_xs=[], weave_k=0.03)
    H.trim_line(f, [(0.0, 1.35), (4.0, 1.35)], GOLD, width=0.13, outline=0.45, spec=0.3)
    H.shadow_under(f, 1.4, 0.5, 0.3)
    front_x = 3.5 if nm == "west" else 0.5                                                       # the side seam of the coat, a gold piping line
    gold_line(f, [(2.0, 1.6), (2.0, 8.1)], 0.1)
    D.leather(f, f.m_band(8.15, 8.7), "#2a2630", 8.15, 8.7)
    D.leather(f, f.m_band(8.82, 10.0), K1, 8.82, 10.0, sheen_x=[1.0, 3.0])
    H.shadow_under(f, 10.0, 0.6, 0.34)
    navy(f, f.m_band(10.0, 12.0), folds=[1.2, 2.8], y0=10.0, y1=12.0)
    H.trim_band(f, 11.5, 12.0, GOLD, outline=0.6)


def body(cv):
    for f in D.lay(cv, "body", "front"):
        body_front(f)
    for f in D.lay(cv, "body", "back"):
        body_back(f)
    for nm in ("west", "east"):
        for f in D.lay(cv, "body", nm):
            body_side(f, nm)
    t = cv.face("body", "top")
    t.fill(t.m_all(), CREAM)
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), K1)
    cv.face("body", "top", True).fill(cv.face("body", "top", True).m_all(), CREAM)


# ================================================================================================ the arms
def epaulette(f, y1=3.2):
    """A huge steel epaulette board: ribbed blue steel with a gold rim, a stud and a hanging bullion fringe."""
    plate = f.m_rect(-0.15, -0.15, f.w + 0.15, y1, r=0.25)
    H.shadow_under(f, y1 + 0.02, 0.8, 0.4)
    f.fill(plate, H.lin((0, 0), (0, y1), [(0, "#7384a8"), (0.35, "#56668c"), (0.75, "#3a476a"), (1, "#2a3452")]))
    for x in np.arange(0.5, f.w, 0.5):
        f.line((float(x), 0.1), (float(x), y1 - 0.55), "#1c2640", 0.06, 0.06, a=0.5)
        f.line((float(x) + 0.1, 0.1), (float(x) + 0.1, y1 - 0.55), "#8fa0c6", 0.05, 0.05, a=0.45)
    for x in (0.0, f.w / 2.0, f.w):                                                              # three gold ribs along the board
        H.trim_line(f, [(float(x) if 0 < x < f.w else (0.12 if x == 0 else f.w - 0.12), 0.05), (float(x) if 0 < x < f.w else (0.12 if x == 0 else f.w - 0.12), y1 - 0.5)], GOLD, width=0.13, outline=0.35, spec=0.3)
    H.bevel(f, plate, depth=0.3, gain=0.55, spec=0.25, spec_pow=14)
    H.trim_band(f, y1 - 0.55, y1, GOLD, x0=-0.1, x1=f.w + 0.1, outline=0.6)
    H.trim_line(f, [(0.0, 0.05), (f.w, 0.05)], GOLD, width=0.15, outline=0.4, spec=0.3)
    H.fringe(f, 0.1, f.w - 0.1, y1 - 0.05, 1.25, n=8, color=GOLD, dark=GOLD_D, width=0.11, var=0.28, seed=3)


def sleeve_navy(f):
    navy(f, f.m_all(), folds=[1.1, 2.9], y0=3.0, y1=10.0)
    f.polyline([(0.2, 6.3), (1.5, 6.0), (2.6, 6.3), (3.8, 6.1)], "#0a1228", 0.1, a=0.45)          # the elbow crease
    f.polyline([(0.3, 6.8), (1.6, 6.55), (2.7, 6.85)], NAVY, 0.07, a=0.45)


def cuff(f, y0=6.7, y1=8.5):
    m = f.m_band(y0, y1)
    H.shadow_under(f, y1, 0.6, 0.36)
    H.cloth(f, m, CREAM, top=1.05, bottom=0.84, round_=0.1, fold_xs=[], weave_k=0.03)
    H.trim_band(f, y0 - 0.02, y0 + 0.28, GOLD, outline=0.55)
    H.trim_band(f, y1 - 0.3, y1 + 0.02, GOLD, outline=0.55)


def arm_l(cv):
    for nm in SIDES:
        for f in D.lay(cv, "arm_l", nm):
            sleeve_navy(f)
            cuff(f)
            epaulette(f)
            # the bare fist below the cuff
            hand = f.m_band(8.5, 12.0)
            f.fill(hand, H.lin((0, 8.5), (0, 12.0), [(0, SKIN[2]), (0.5, SKIN[1]), (1, SKIN[0])]))
            H.shadow_under(f, 8.5, 0.5, 0.3)
            if nm == "front":                                                                    # knuckles and the four fingers of a clenched fist
                for k in range(4):
                    x = 0.5 + k * 1.0
                    f.line((x, 10.0), (x, 11.8), H.tone(SKIN[0], 0.65), 0.07, 0.05, a=0.55)
                    f.ellipse(x - 0.02, 9.9, 0.4, 0.26, SKIN[2], a=0.5, soft=2.0)
            if nm == "back":
                for k in range(4):
                    f.ellipse(0.55 + k * 0.95, 11.1, 0.36, 0.3, SKIN[2], a=0.4, soft=2.0)
                f.polyline([(0.3, 9.4), (1.0, 10.0), (2.0, 9.8), (3.0, 10.1), (3.7, 9.5)], H.tone(SKIN[0], 0.7), 0.06, a=0.4)
        if nm == "front":
            for f in D.lay(cv, "arm_l", nm):
                button(f, 1.0, 7.6, 0.24)
                button(f, 3.0, 7.6, 0.24)
    for ov in (False, True):
        t = cv.face("arm_l", "top", ov)
        t.fill(t.m_all(), H.metal_paint(0, 4, STEEL, contrast=0.6))
        t.rect(0.0, 0.0, 4.0, 4.0, GOLD_D, r=0.0, a=0.0)
        H.trim_line(t, [(0.1, 0.1), (3.9, 0.1), (3.9, 3.9), (0.1, 3.9), (0.1, 0.1)], GOLD, width=0.2, outline=0.4, spec=0.3)
        H.stud(t, 2.0, 2.0, 0.55, GOLD)
    cv.face("arm_l", "bottom").fill(cv.face("arm_l", "bottom").m_all(), SKIN[0])


def axe_head(f, nm):
    """The right hand is a great steel axe: a riveted socket ring and a heavy blade; the bright cutting edge runs along the outer edge and the bottom."""
    y0 = 8.6
    H.shadow_under(f, y0, 0.5, 0.36)
    blade = f.m_rect(-0.1, y0, f.w + 0.1, 12.05)
    f.fill(blade, H.lin((0, y0), (f.w, 12.0), [(0, "#7e8da8"), (0.4, "#566480"), (0.75, "#3a4560"), (1, "#262e44")]))
    # the socket ring at the wrist, with rivets
    H.trim_band(f, y0, y0 + 0.95, "#56627a", outline=0.7)
    for x in np.arange(0.5, f.w, 1.0):
        H.stud(f, float(x), y0 + 0.48, 0.14, "#cfd8e6")
    if nm in ("front", "back"):
        # the blade seen flat: a quarter-moon whose cutting edge is the long convex arc on the outer side and the bottom, the back of the head straight at the wrist; the corner outside of the arc is a dark
        # shadow (the second layer is cut away there, so the blade stands out of the arm)
        mir = nm == "back"
        arc = [(0.0, 9.55), (0.0, 10.25), (0.2, 11.0), (0.8, 11.6), (1.9, 11.97), (4.05, 12.05)]
        if mir:
            arc = [(f.w - x, y) for x, y in arc]
        poly = [(-0.2 if not mir else f.w + 0.2, 9.5), (f.w + 0.2 if not mir else -0.2, 9.5), (f.w + 0.2 if not mir else -0.2, 12.2)] + arc[::-1]
        bl = f.m_poly(poly)
        outside = H.minus(f.m_band(y0 + 0.9, 12.1), bl)
        if f.overlay:
            f.fill(outside, "#000000", mode="erase")
        else:
            f.fill(outside, H.lin((0, 9), (0, 12), [(0, "#1a2034"), (1, "#0a0e1a")]))
        f.fill(bl, H.lin((0, 9.4), (f.w, 12.0), [(0, "#8ea0c0"), (0.45, "#5c6c8c"), (1, "#2e3854")]))
        f.fill(f.m_polyline(arc, 0.95) * bl, H.lin((0, 9.4), (f.w, 12.0), [(0, "#dfe9fa"), (1, "#a9bad8")]), 0.9)               # the ground bevel of the edge
        f.fill(f.m_polyline(arc, 0.34) * bl, "#ffffff", 0.95)                                                                    # the bright cutting edge
        f.fill(f.m_polyline(arc, 0.15), "#0a0e1a", 0.0)
        # an engraved line following the edge, a rivet at the top corner and a nick on the edge
        f.fill(f.m_polyline([(x + (0.75 if not mir else -0.75), y - (0.5 if y > 11 else 0.0)) for x, y in arc[1:5]], 0.07) * bl, "#1e2640", 0.55)
        H.stud(f, (f.w - 0.55) if not mir else 0.55, 9.95, 0.15, "#d4dcea")
        H.stud(f, (f.w - 0.55) if not mir else 0.55, 11.4, 0.15, "#d4dcea")
        f.poly([(1.0 if not mir else f.w - 1.0, 11.62), (1.28 if not mir else f.w - 1.28, 11.93), (1.55 if not mir else f.w - 1.55, 11.78)], "#0a0e1a", a=0.45)
        H.bevel(f, bl, depth=0.28, gain=0.3, spec=0.15, spec_pow=10)
    else:
        # the profile: a thick spine at the back, a wedge to the edge at the front
        spine_x = 0.0 if nm == "west" else f.w
        edge_x = f.w if nm == "west" else 0.0
        f.poly([(spine_x, y0 + 1.0), (edge_x, y0 + 1.0), (edge_x, 12.05), (spine_x, 12.05)], H.lin((spine_x, 0), (edge_x, 0), [(0, "#262e44"), (0.65, "#566480"), (1, "#b8c8e4")]))
        f.line((edge_x + (-0.1 if nm == "west" else 0.1), y0 + 1.0), (edge_x + (-0.1 if nm == "west" else 0.1), 12.0), "#ffffff", 0.09, 0.09, a=0.95)
        f.polyline([(f.w * 0.5, y0 + 1.0), (f.w * 0.5, 12.0)], STEEL_D, 0.06, a=0.4)
    if nm in ("west", "east"):
        f.fill(f.m_band(11.55, 12.05), H.lin((0, 11.55), (0, 12.0), [(0, "#b8c6e0"), (0.6, "#f4f8ff"), (1, "#a0b0d0")]), 0.9)
        H.bevel(f, f.m_rect(0, y0 + 0.95, f.w, 12.0), depth=0.3, gain=0.3, spec=0.15, spec_pow=10)


def arm_r(cv):
    for nm in SIDES:
        for f in D.lay(cv, "arm_r", nm):
            sleeve_navy(f)
            cuff(f, 6.2, 7.9)
            epaulette(f)
            axe_head(f, nm)
        if nm == "front":
            for f in D.lay(cv, "arm_r", nm):
                button(f, 1.0, 7.0, 0.24)
                button(f, 3.0, 7.0, 0.24)
    for ov in (False, True):
        t = cv.face("arm_r", "top", ov)
        t.fill(t.m_all(), H.metal_paint(0, 4, STEEL, contrast=0.6))
        H.trim_line(t, [(0.1, 0.1), (3.9, 0.1), (3.9, 3.9), (0.1, 3.9), (0.1, 0.1)], GOLD, width=0.2, outline=0.4, spec=0.3)
        H.stud(t, 2.0, 2.0, 0.55, GOLD)
    b = cv.face("arm_r", "bottom")
    b.fill(b.m_all(), H.lin((0, 0), (0, 4), [(0, "#f2f7ff"), (1, "#9fb0cc")]))
    H.bevel(b, b.m_rect(0, 0, 4, 4), depth=0.5, gain=0.8, spec=0.4)


# ================================================================================================ the legs
def leg(cv, part):
    for nm in SIDES:
        for f in D.lay(cv, part, nm):
            H.cloth(f, f.m_band(0.0, 8.2), TROUS, top=1.15, bottom=0.78, round_=0.12, fold_xs=None, weave_k=0.03)
            H.folds(f, f.m_band(0.0, 8.2), [1.0, 2.9] if nm != "front" else [0.8, 3.2], depth=0.12, width=0.2, sway=0.05, y0=0.5, y1=8.0)
            if nm == "front":                                                                     # the sharp crease
                f.line((2.0, 0.0), (2.0, 8.0), H.tone(TROUS, 0.62), 0.1, 0.1, a=0.55)
                f.line((2.12, 0.0), (2.12, 8.0), H.tone(TROUS, 1.4), 0.07, 0.07, a=0.45)
            if nm == "front":
                D.slash_pocket(f, part == "leg_r", TROUS)
            for k in range(3):                                                                    # the knee wrinkles
                y = 4.6 + k * 0.38
                f.line((0.2, y), (f.w - 0.2, y + 0.18 * (1 if k % 2 else -1)), H.tone(TROUS, 0.7), 0.07, 0.05, a=0.38)
            # the trouser hem falls over the boot, the steel greave cuff with rivets, the black boot
            H.shadow_under(f, 7.55, 0.5, 0.38)
            boot = f.m_band(8.0, 12.0)
            f.fill(boot, H.lin((0, 8.0), (0, 12.0), [(0, K2), (0.4, K1), (1, K0)]))
            H.sheen(f, boot, [0.85, 2.9], width=0.2, strength=0.28, tilt=0.03, y0=9.4, y1=11.6)
            gr = f.m_band(7.7, 9.3)
            f.fill(gr, H.metal_paint(7.7, 9.3, STEEL, contrast=0.7))
            H.bevel(f, gr, depth=0.3, gain=0.9, spec=0.45, spec_pow=12)
            H.trim_line(f, [(0.0, 7.78), (f.w, 7.78)], GOLD, width=0.12, outline=0.4, spec=0.3)
            H.trim_line(f, [(0.0, 9.22), (f.w, 9.22)], H.tone(STEEL, 0.55), width=0.14, outline=0.3, spec=0.2)
            for x in np.arange(0.5, f.w, 1.0):
                H.stud(f, float(x), 8.5, 0.15, "#dfe7f4")
            if nm == "front":
                f.polyline([(0.4, 11.0), (1.4, 10.65), (2.6, 10.65), (3.6, 11.0)], "#4a4858", 0.1, a=0.7)           # the toe cap seam
                D.leather(f, f.m_band(9.85, 10.35), "#2a2630", 9.85, 10.35)
                D.buckle(f, 2.0, 10.1, 0.6, 0.62, color="#cfd5df", hole="#17161e")
            f.fill(f.m_band(11.7, 12.0), "#000000", a=0.6)
    t = cv.face(part, "top")
    t.fill(t.m_all(), TROUS)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), K0)
    D.clear_top_bottom_overlay(cv, part)


def paint(spec, cid, masks):
    cv = H.Canvas(256, False)
    D.base_fill(cv, NAVY)
    head(cv)
    body(cv)
    arm_l(cv)
    arm_r(cv)
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    cv.face("head", "top", True).clear()
    skin, glow = cv.finish()
    return skin, glow, "anime_flattop", HAIR, False

"""Sogou (Sugou) Nobuyuki as Oberon, the fairy king of Alfheim (Sword Art Online), HD 256x256 (3.38.0, group a of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_a.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/sogou_nobuyuki.py): long golden hair under a gold crown with a red jewel, a smirking face with glowing violet eyes, a deep-violet long coat
with gold piping and a tall gold collar, a white jabot with a gem, gold pauldrons and gauntlets over white gloves, a black belt with a gold buckle, black trousers with a gold side stripe, tall black
boots with gold cuffs, and the translucent pale-blue dragonfly wings on the back (glowing). HD adds: gold arabesque embroidery on the coat, real pauldrons with an embossed rim, segmented gauntlets,
two pairs of veined wings with a glowing net, a crown with a jewel, a sharp violet eye, scalloped boot cuffs.
Hair: `anime_short_open` (golden, open fringe so the crown shows, the nape short so the wings on the back stay in view; `anime_long_open` shows the crown and the long hair but hides the wings behind it), gold.
Original art from memory of the canon look. Glow: the eyes, the crown jewel, the pauldron gems and the wings.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402

SKIN = ("#e4b69e", "#f6d8c2", "#fdeadb")
HAIR = (255, 208, 0)
HAIRC = "#ffd000"
V0, V1, V2, V3 = "#1e0c40", "#3a1a74", "#5a2cae", "#8a56d6"                 # the violet coat: shadow, base, light, highlight
G0, G1, G2, G3, G4 = "#6a4a08", "#b88a1c", "#e0aa24", "#f6cc48", "#ffee9a"   # gold
GT = "#c4961c"                                                                # the base of the trim bands
K0, K1, K2 = "#07040e", "#14102a", "#2c2250"                                  # the black-violet trousers, boots and belt
WH0, WH1, WH2 = "#bdb7d6", "#eeebf8", "#ffffff"
JEW, JEW_L = "#d41040", "#ff8aa8"
WING_V, WING_M, WING_L = "#2a7eb0", "#9ad8f4", "#eefcff"
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def gold(f, mask, y0, y1, k=1.0):
    f.fill(mask, H.metal_paint(y0, y1, H.tone(G2, k), contrast=0.62))
    H.bevel(f, mask, depth=0.3, gain=0.85, spec=0.35, spec_pow=12)


def coat(f, mask, folds, y0=0.0, y1=None):
    H.cloth(f, mask, V1, top=1.14, bottom=0.8, round_=0.14, fold_xs=folds, fold_depth=0.2, fold_y0=y0, fold_y1=y1, weave_k=0.04)
    H.sheen(f, mask, [x + 0.3 for x in folds], width=0.14, strength=0.18, color="#b08af0", y0=y0, y1=y1)


def vine(f, x0, y0, y1, amp=0.42, period=2.2, sg=1, a=0.95, w=0.1):
    """A gold arabesque stem with leaves: a sine wave down a coat panel."""
    pts = [(x0 + amp * math.sin((y - y0) / period * math.tau) * sg, y) for y in np.linspace(y0, y1, 24)]
    f.polyline([(x + 0.04, y + 0.05) for x, y in pts], "#10042a", w + 0.06, a=0.5)
    f.polyline(pts, G3, w, a=a)
    for i in range(1, 5):
        t = i / 5.0
        y = y0 + (y1 - y0) * t
        x = x0 + amp * math.sin((y - y0) / period * math.tau) * sg
        d = 1.0 if i % 2 else -1.0
        f.poly([(x, y), (x + d * 0.55, y - 0.28), (x + d * 0.8, y + 0.05), (x + d * 0.45, y + 0.2)], G3, a=a)
        f.line((x, y), (x + d * 0.7, y - 0.04), G1, 0.04, 0.03, a=0.7)


# ================================================================================================ the head
def head(cv):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(HAIRC, 0.95), hairline=2.0, wave=0.6, shadow=0.2)
    iris = ("#2a0c58", "#7a3ad0", "#e0b8ff")
    for cx in (2.25, 5.75):
        H.eye(f, cx, 5.0, 2.45, 1.85, iris, lash="#3a2414", sclera="#fffaf6", tilt=0.24, pupil="#0c0420", glow="#6a2ab0", lid_shadow=0.34, lash_w=0.4, flick=0.32, hi=0.95)
    H.brow(f, (3.45, 3.75), (0.95, 3.15), "#a8841c", 0.2, 0.1, arch=0.04)                   # thin angled brows
    H.brow(f, (4.55, 3.75), (7.05, 3.15), "#a8841c", 0.2, 0.1, arch=0.04)
    H.blush(f, 1.2, 6.2, 0.8, 0.5, "#ee98a0", 0.4)
    H.blush(f, 6.8, 6.2, 0.8, 0.5, "#ee98a0", 0.4)
    H.nose(f, 4.0, 6.2, "#d09a8c", 0.22)
    H.mouth_smile(f, 4.0, 7.15, 2.0, color="#a8404a", lip="#d88088", depth=0.14, thick=0.16)       # a thin cruel smile, one corner higher
    f.line((5.0, 7.22), (5.42, 6.92), "#a8404a", 0.14, 0.08, a=0.9)
    f.line((3.0, 7.22), (2.75, 7.32), "#a8404a", 0.1, 0.06, a=0.8)
    f.ellipse(4.0, 7.45, 0.55, 0.12, "#ffffff", a=0.25, soft=3.0)
    # the crown: a gold band over the brow with a jewel, points above it (the hair's open fringe leaves the band in view)
    for ov in (False, True):
        for nm in SIDES:
            g = cv.face("head", nm, ov)
            crown(g, nm == "front")
    t = cv.face("head", "top")
    t.fill(t.m_all(), H.tone(HAIRC, 1.0))
    cv.face("head", "bottom").fill(cv.face("head", "bottom").m_all(), SKIN[0])
    for nm in ("west", "east", "back"):                                                                  # the hair painted on the base head for far views
        g = cv.face("head", nm)
        g.fill(g.m_all(), H.lin((0, 0), (0, 8), [(0, H.tone(HAIRC, 1.25)), (1, H.tone(HAIRC, 0.78))]))
        for i in range(20):
            x = (i + 0.5) * 0.4 + 0.15 * math.sin(i * 2.1)
            g.line((x, 0.0), (x + 0.2 * math.sin(i), 8.0), H.tone(HAIRC, 0.7 if i % 2 else 1.4), 0.08, 0.05, a=0.5)


def crown(f, front):
    band = f.m_band(1.75, 2.75)
    f.fill(band, H.metal_paint(1.75, 2.75, G2, contrast=0.7))
    H.bevel(f, band, depth=0.3, gain=1.1, spec=0.5, spec_pow=12)
    f.fill(f.m_band(1.72, 1.8), G0, a=0.8)
    f.fill(f.m_band(2.7, 2.78), G0, a=0.8)
    n = 5 if front else 4
    for i in range(n):                                                                                 # the points of the crown
        x = (i + 0.5) * f.w / n
        tall = 1.0 if (front and i == n // 2) else 0.65
        pt = f.m_poly([(x - 0.55, 1.78), (x + 0.55, 1.78), (x, 1.78 - 1.5 * tall)])
        f.fill(pt, H.lin((0, 0.2), (0, 1.8), [(0, G4), (1, G2)]))
        f.line((x, 1.78 - 1.4 * tall), (x, 1.8), G1, 0.04, 0.04, a=0.6)
        f.ellipse(x, 1.78 - 1.5 * tall, 0.12, 0.12, JEW_L, a=0.9)
    if front:
        H.gem(f, 4.0, 2.25, 0.44, JEW, setting=G3, glow=True, facets=5)
        for x in (1.4, 6.6):
            H.stud(f, x, 2.25, 0.17, G4)
    else:
        for x in np.arange(0.9, f.w, 1.5):
            H.stud(f, float(x), 2.25, 0.15, G4)


# ================================================================================================ the body
def body(cv):
    for nm in SIDES:
        base, ov = lay(cv, "body", nm)
        for f in (base, ov):
            coat(f, f.m_all(), (1.0, 2.4, 5.6, 7.0) if f.w > 6 else (1.0, 3.0))
    for f in lay(cv, "body", "front"):
        coat_front(f)
    for f in lay(cv, "body", "back"):
        coat_back(f)
    for nm in ("west", "east"):
        for f in lay(cv, "body", nm):
            collar(f, 2.2)
            skirt_edge(f)
            belt(f)
            vine(f, 2.0, 3.0, 8.0, amp=0.35, sg=1)
    for ovl in (False, True):
        t = cv.face("body", "top", ovl)
        t.fill(t.m_all(), H.metal_paint(0, 4, G2))
        if ovl:
            t.fill(t.m_rect(1.2, 0.9, 6.8, 3.1, soft=1.0), "#000000", mode="erase")
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), K1)


def collar(f, h, x0=None, x1=None):
    x0 = 0.0 if x0 is None else x0
    x1 = f.w if x1 is None else x1
    m = f.m_rect(x0, -0.3, x1, h, 0.3)
    f.fill(m, H.metal_paint(0.0, h, G2, contrast=0.65))
    H.bevel(f, m, depth=0.3, gain=0.9, spec=0.4)
    for x in np.arange(x0 + 0.5, x1, 1.0):
        f.line((float(x), 0.1), (float(x), h - 0.2), G1, 0.05, 0.03, a=0.45)
    H.shadow_under(f, h, 0.4, 0.3, x0=x0, x1=x1)


def belt(f):
    m = f.m_band(8.1, 9.5)
    f.fill(m, H.lin((0, 8.1), (0, 9.5), [(0, K2), (1, K0)]))
    H.sheen(f, m, [1.5, 4.5, 6.8] if f.w > 6 else [1.4], width=0.2, strength=0.2, color="#8a7ad0", y0=8.1, y1=9.5)
    H.trim_line(f, [(0.0, 8.1), (f.w, 8.1)], G2, width=0.13, outline=0.3, spec=0.3)
    H.trim_line(f, [(0.0, 9.5), (f.w, 9.5)], G2, width=0.13, outline=0.3, spec=0.3)
    H.shadow_under(f, 9.6, 0.5, 0.3)


def skirt_edge(f):
    """The coat skirt below the belt: violet to the hem, gold trim along the hem."""
    H.trim_band(f, 11.2, 12.0, GT, outline=0.4)
    for x in np.arange(0.5, f.w, 1.0):
        f.line((float(x), 11.25), (float(x), 11.95), G1, 0.05, 0.03, a=0.45)


def coat_front(f):
    collar(f, 2.25, 1.4, 6.6)
    # the open lapels: a white jabot between two violet panels, gold piping down the edges
    jab = f.m_poly([(2.2, 2.0), (5.8, 2.0), (5.3, 8.1), (2.7, 8.1)])
    f.fill(jab, H.lin((0, 2.0), (0, 8.1), [(0, WH2), (0.6, WH1), (1, WH0)]))
    H.folds(f, jab, [3.1, 4.0, 4.9], depth=0.1, width=0.2, y0=2.5, y1=8.0)
    for dy in (0.0, 0.7, 1.4):                                                                          # the ruffles of the jabot
        H.scallops(f, f.m_poly([(2.3, 2.2 + dy), (5.7, 2.2 + dy), (5.5, 3.1 + dy), (2.5, 3.1 + dy)]), 2.2 + dy, rows=1, cell=0.8, depth=0.7, base="#fbfaff", tip="#ffffff", edge="#c4bedb", edge_a=0.5, shadow=0.2)
    for pts in ([(2.2, 2.0), (2.7, 8.1)], [(5.8, 2.0), (5.3, 8.1)]):
        f.polyline([(x + (0.06 if pts[0][0] < 4 else -0.06), y) for x, y in pts], "#10042a", 0.34, a=0.55)
        H.trim_line(f, pts, G2, width=0.26, outline=0.4, spec=0.5)
    H.gem(f, 4.0, 3.5, 0.5, JEW, setting=G3, glow=True, facets=6)                                      # the brooch of the jabot
    for y in (5.0, 6.1, 7.2):
        H.stud(f, 4.0, y, 0.15, G3)
    vine(f, 1.0, 2.8, 7.9, sg=1)
    vine(f, 7.0, 2.8, 7.9, sg=-1)
    belt(f)
    bk = f.m_rect(3.1, 8.0, 4.9, 9.6, 0.25)                                                            # the buckle
    f.fill(bk, H.metal_paint(8.0, 9.6, G2))
    H.bevel(f, bk, depth=0.25, gain=1.2, spec=0.6)
    f.fill(f.m_rect(3.4, 8.3, 4.6, 9.3, 0.15), K1)
    f.ellipse(4.0, 8.8, 0.22, 0.22, G3)
    # the skirt of the coat opens over the black trousers
    f.fill(f.m_poly([(3.3, 9.6), (4.7, 9.6), (5.2, 12.1), (2.8, 12.1)]), H.lin((0, 9.6), (0, 12.0), [(0, K1), (1, K0)]))
    H.trim_line(f, [(3.3, 9.6), (2.8, 12.0)], G2, width=0.2, outline=0.4, spec=0.4)
    H.trim_line(f, [(4.7, 9.6), (5.2, 12.0)], G2, width=0.2, outline=0.4, spec=0.4)
    H.trim_band(f, 11.3, 12.0, GT, x0=0.0, x1=2.85, outline=0.4)
    H.trim_band(f, 11.3, 12.0, GT, x0=5.15, x1=8.0, outline=0.4)
    vine(f, 1.4, 9.8, 11.2, amp=0.25, period=1.4)
    vine(f, 6.6, 9.8, 11.2, amp=0.25, period=1.4, sg=-1)


def wing(f, sgn):
    """One pair of dragonfly wings (sgn -1 = the viewer's left one): a long fore wing and a shorter hind wing, pale blue, veined, glowing."""
    def X(x):
        return x if sgn < 0 else 8.0 - x
    fore = [(3.6, 2.1), (2.8, 1.0), (1.4, 0.7), (0.3, 1.5), (0.0, 3.4), (0.2, 5.6), (1.0, 7.3), (2.2, 6.4), (3.2, 4.4)]
    hind = [(3.6, 3.7), (2.4, 5.4), (0.9, 7.6), (0.5, 9.8), (1.6, 11.0), (2.8, 9.4), (3.5, 6.6)]
    for pts, root in ((hind, (3.7, 3.7)), (fore, (3.7, 2.2))):
        poly = [(X(x), y) for x, y in pts]
        m = f.m_poly(poly)
        f.fill(m, H.lin((X(3.6), 0), (X(0.0), 8), [(0, WING_L), (0.5, WING_M), (1, "#6cb8e0")]), glow="#5aa8d8")
        # the net of veins: spokes from the root to the rim, rings across them
        ox, oy = X(root[0]), root[1]
        for (x, y) in pts[1:-1]:
            f.line((ox, oy), (X(x), y), WING_V, 0.11, 0.05, a=0.9)
        for t in (0.35, 0.62, 0.84):
            ring = [(ox + (X(x) - ox) * t, oy + (y - oy) * t) for x, y in pts[1:-1]]
            f.polyline(ring, WING_V, 0.07, a=0.75)
        f.polyline([(X(x), y) for x, y in pts] + [(X(pts[0][0]), pts[0][1])], "#17608e", 0.16, a=0.95)
        H.bevel(f, m, depth=0.2, gain=0.5)


def coat_back(f):
    collar(f, 2.0, 1.0, 7.0)
    belt(f)
    skirt_edge(f)
    vine(f, 4.0, 9.8, 11.2, amp=0.3, period=1.4)
    f.fill(f.m_rect(3.3, 1.9, 4.7, 8.6, 0.6), H.lin((0, 2), (0, 8.6), [(0, V2), (1, V0)]), a=0.9)         # the spine of the coat between the wings
    H.trim_line(f, [(4.0, 2.0), (4.0, 8.0)], G2, width=0.16, outline=0.4, spec=0.4)
    wing(f, -1)                                                                                          # the wings lie over the belt
    wing(f, 1)


# ================================================================================================ the arms
def arm(cv, part):
    right = part == "arm_r"
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        for f in (base, ov):
            coat(f, f.m_all(), (1.0, 2.7) if f.w > 3 else (1.2,), y0=0.0, y1=11.0)
            vine(f, f.w / 2.0, 3.6, 7.6, amp=0.4, sg=1 if right else -1)
            # the gold pauldron: a domed plate over the shoulder with a scalloped lower edge
            plate = H.union(f.m_band(0.0, 2.8), f.m_ellipse(f.w / 2.0, 2.8, f.w / 2.0 + 0.05, 1.0))
            gold(f, plate, 0.0, 3.8)
            for x in np.arange(0.5, f.w, 1.0):
                f.line((float(x), 0.1), (float(x), 3.0), G1, 0.05, 0.03, a=0.5)
            H.trim_line(f, [(0.0, 2.9), (f.w, 2.9)], G1, width=0.14, outline=0.4, spec=0.3)
            H.scallops(f, f.m_band(3.0, 4.0) * 0.0, 3.0, rows=1)
            H.stud(f, f.w / 2.0, 3.3, 0.14, G4)
            # the gold gauntlet over the forearm, segmented, then the white glove
            ga = f.m_band(7.4, 10.6)
            gold(f, ga, 7.4, 10.6)
            for y in (8.3, 9.2):
                H.trim_line(f, [(0.0, y), (f.w, y)], G1, width=0.1, outline=0.5, spec=0.25)
            H.trim_band(f, 7.3, 7.8, GT, outline=0.5, spec=0.4)
            H.trim_band(f, 10.3, 10.75, GT, outline=0.5, spec=0.4)
            H.shadow_under(f, 10.7, 0.4, 0.3)
            glove = f.m_band(10.7, 12.0)
            H.cloth(f, glove, WH1, top=1.04, bottom=0.88, fold_xs=[], weave_k=0.0)
        if nm == "front":
            for f in (base, ov):
                H.gem(f, f.w / 2.0, 1.4, 0.5, JEW, setting=G3, glow=True, facets=6)
            base.line((1.0, 11.2), (1.0, 11.9), WH0, 0.05, 0.05, a=0.7)
            base.line((2.0, 11.2), (2.0, 11.9), WH0, 0.05, 0.05, a=0.7)
            base.line((3.0, 11.2), (3.0, 11.9), WH0, 0.05, 0.05, a=0.7)
    for ovl in (False, True):
        t = cv.face(part, "top", ovl)
        t.fill(t.m_all(), H.rad((2, 2), 3, [(0, G4), (0.5, G2), (1, G1)]))
        t.ellipse(2.0, 2.0, 0.9, 0.9, H.rad((1.8, 1.8), 1.2, [(0, JEW_L), (1, JEW)]), glow=JEW)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), WH1)


# ================================================================================================ the legs
def leg(cv, part):
    outer = "west" if part == "leg_r" else "east"
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        for f in (base, ov):
            H.cloth(f, f.m_all(), "#1a1236", top=1.12, bottom=0.8, round_=0.14, fold_xs=[1.0, 2.9], fold_depth=0.16, weave_k=0.03)
            if nm == outer:                                                                              # the gold side stripe
                H.trim_line(f, [(2.0, 0.0), (2.0, 6.2)], G2, width=0.55, outline=0.5, spec=0.5)
                H.stitch(f, [(1.55, 0.2), (1.55, 6.0)], G4, width=0.05, dash=(0.28, 0.2), shadow=0.0)
            # the tall boot: a gold cuff with a scalloped top, black leather, a strap and a buckle
            boot = f.m_band(6.6, 12.0)
            f.fill(boot, H.lin((0, 6.6), (0, 12.0), [(0, K2), (0.5, K1), (1, K0)]))
            H.sheen(f, boot, [1.0, 2.9], width=0.17, strength=0.22, color="#9a8ad8", y0=7.6, y1=11.4)
            cuff = f.m_band(6.1, 8.0)
            gold(f, cuff, 6.1, 8.0)
            H.scallops(f, f.m_band(6.0, 6.7), 5.95, rows=1, cell=0.8, depth=0.6, base=G3, tip=G4, edge=G1, edge_a=0.7, shadow=0.0)
            H.trim_line(f, [(0.0, 7.95), (f.w, 7.95)], G1, width=0.11, outline=0.5, spec=0.2)
            if nm == "front":
                H.trim_line(f, [(0.2, 9.4), (f.w - 0.2, 9.4)], G2, width=0.2, outline=0.5, spec=0.4)
                f.rect(1.5, 9.1, 2.5, 9.7, G3, r=0.1)
                H.stud(f, 2.0, 9.4, 0.14, K1)
            f.fill(f.m_band(11.5, 12.0), "#000000", a=0.5)
        ov.fill(ov.m_band(11.6, 12.0), "#000000", mode="erase")
    t = cv.face(part, "top")
    t.fill(t.m_all(), K1)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), K0)
    cv.face(part, "top", True).clear()
    cv.face(part, "bottom", True).clear()


def paint(spec, cid, masks):
    cv = H.Canvas(256, False)
    for f in cv.faces(overlay=(False,)):
        f.fill(f.m_all(), V1)
    head(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    cv.face("head", "top", True).clear()
    skin, glow = cv.finish()
    return skin, glow, "anime_short_open", HAIR, False

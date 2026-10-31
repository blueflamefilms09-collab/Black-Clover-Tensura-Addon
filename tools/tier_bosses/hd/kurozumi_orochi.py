"""Kurozumi Orochi (One Piece, the Shogun of Wano), HD 256x256 (3.38.0, group a of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_a.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/kurozumi_orochi.py): a pale heavy-browed face with slit eyes, a thin drooping moustache and a goatee, a black topknot, a deep-purple kimono
with lavender snake scales (uroko), a crossed white collar, winged indigo shoulders (kataginu) with a lavender edge, a wide lavender obi with a gold cord and a big knot on the back, wide hanging sleeves
with lilac linings, the gold-and-white eight-headed-snake crest on the back, indigo pleated hakama, white tabi and wooden geta. HD adds: a real uroko lattice that tiles smoothly, a brocade obi with
a flat gold knot and tassels, a drawn eight-necked snake crest, a collar that overlaps, stiff shoulder wings with a piped edge, pleats, the geta with their two teeth, a face with eye bags.
Hair: `anime_topknot` (black, the knot sits on the head; the older `anime_short` gave no knot at all), blue-black.
Original art from memory of the canon look (no wiki could be fetched). Glow: the obi trim, the gold cord and the crest, softly.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402

SKIN = ("#dcb4a2", "#f2d4be", "#fde6d6")
HAIR = (38, 38, 60)
HAIRC = "#26263c"
P0, P1, P2, P3 = "#1c0a36", "#3a1668", "#4e1f8c", "#7a3cc8"                 # the kimono: shadow, base, light, highlight
I0, I1, I2, I3 = "#0a0d2e", "#171d52", "#26327e", "#4054b0"                 # indigo (shoulders, hakama)
LV0, LV1, LV2, LV3 = "#8a64b8", "#b494dc", "#d8c0f4", "#f2e6ff"           # lavender: obi, linings
W0, W1, W2 = "#c4c0d8", "#eeeaf8", "#ffffff"
G0, G1, G2, G3 = "#8a5a0c", "#c89a20", "#f0c43c", "#fff0a0"
WD = ("#2e1c10", "#5a3820", "#8a5a34")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def uroko(f, mask, size=1.7, h=0.95, y0=0.0, hi=P3, lo=P0, a=0.5, outline=0.5):
    """The snake-scale lattice: staggered rows of upward triangles, each a little lighter than the cloth, with a thin dark outline."""
    t = (f.Y - y0) / h
    r = np.floor(t)
    xo = (r % 2.0) * size / 2.0
    u = (((f.X - xo) / size) % 1.0 - 0.5) * 2.0                          # -1..1 across a triangle
    v = t - r                                                            # 0 at the apex row, 1 at the base
    d = (np.abs(u) - v) * (size / 2.0) * 0.8                               # signed distance to the slanted edges (positive outside), about model pixels
    ins = np.clip(0.5 - d * f.f * 0.9, 0.0, 1.0)
    base_line = np.clip(1.0 - np.abs(v - 1.0) * h * f.f / 1.5, 0.0, 1.0) * (np.abs(u) < 1.0)
    edge = np.clip(1.0 - np.abs(d) * f.f / 1.5, 0.0, 1.0) * (v >= 0.0)
    f.fill(mask * ins, hi, a=a)
    f.fill(mask * np.maximum(edge * (v < 1.0), base_line * 0.8), lo, a=outline)


def shade_cloth(f, mask, color, folds, y0=0.0, y1=None, top=1.1, bottom=0.82, depth=0.16):
    H.cloth(f, mask, color, top=top, bottom=bottom, round_=0.14, fold_xs=folds, fold_depth=depth, fold_y0=y0, fold_y1=y1, weave_k=0.03)


# ================================================================================================ the head
def head(cv):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(HAIRC, 1.0), hairline=2.0, wave=0.5, shadow=0.24)
    iris = ("#240a10", "#7a2a30", "#c87068")
    for cx in (2.2, 5.8):
        H.eye(f, cx, 5.0, 2.3, 1.0, iris, lash="#10080c", sclera="#f6eee8", tilt=0.14, pupil="#0c0406", lid_shadow=0.5, lash_w=0.4, flick=0.12, hi=0.6)
        f.fill(f.m_ellipse(cx, 5.75, 1.15, 0.28, soft=3.0), "#6a4a6a", a=0.3)                       # the dark bags under the eyes
        f.fill(f.m_ellipse(cx, 4.4, 1.2, 0.32, soft=3.0), "#4a2a3a", a=0.35)                        # the heavy lids
    H.brow(f, (3.7, 3.85), (0.7, 3.2), "#16101c", 0.44, 0.22, arch=0.12)                        # thick, low, drawn-down brows
    H.brow(f, (4.3, 3.85), (7.3, 3.2), "#16101c", 0.44, 0.22, arch=0.12)
    f.line((3.7, 4.4), (3.78, 6.0), H.tone(SKIN[0], 0.8), 0.14, 0.2, a=0.5)                        # a long thin nose
    f.ellipse(3.55, 6.3, 0.22, 0.14, "#b8808a", a=0.8)
    f.ellipse(4.45, 6.3, 0.22, 0.14, "#b8808a", a=0.8)
    # the moustache: two thin long hairs drooping from the nose to below the chin line, and the goatee
    for sg in (-1, 1):
        mp = [(4.0 + sg * 0.3, 6.5), (4.0 + sg * 0.95, 6.85), (4.0 + sg * 1.5, 7.4), (4.0 + sg * 1.6, 8.1)]
        f.polyline([(x + 0.04, y + 0.05) for x, y in mp], "#000000", 0.17, a=0.25)
        f.polyline(mp, "#16101c", 0.15, a=1.0)
    H.mouth_smile(f, 4.0, 7.3, 1.5, color="#6a2a3a", lip="#a8606a", depth=-0.06, thick=0.15)
    gt = f.m_poly([(3.4, 7.55), (4.6, 7.55), (4.35, 8.2), (3.65, 8.2)])
    f.fill(gt, "#16101c", a=0.95)
    for i in range(6):
        f.line((3.55 + i * 0.18, 7.6), (3.62 + i * 0.15, 8.05), "#3a3248", 0.04, 0.03, a=0.6)
    f.shade(f.m_all(), 1.0 - 0.1 * (1.0 - H.smoothstep(0.0, 1.0, np.minimum(f.X, 8.0 - f.X))))
    for nm in ("west", "east", "back", "top"):
        g = cv.face("head", nm)
        g.fill(g.m_all(), H.lin((0, 0), (0, 8), [(0, H.tone(HAIRC, 1.3)), (1, H.tone(HAIRC, 0.75))]))
        for i in range(20):
            x = (i + 0.5) * 0.4 + 0.15 * math.sin(i * 2.1)
            g.line((x, 0.0), (x + 0.2 * math.sin(i), 8.0), H.tone(HAIRC, 0.6 if i % 2 else 1.6), 0.07, 0.04, a=0.5)
    cv.face("head", "bottom").fill(cv.face("head", "bottom").m_all(), SKIN[0])
    # the hair roots on the base head's front (a rounded hairline), the rest is the 3D hair
    top = cv.face("head", "front")
    top.fill(top.m_band(0.0, 1.3), H.lin((0, 0), (0, 1.3), [(0, H.tone(HAIRC, 1.2)), (1, H.tone(HAIRC, 0.9))]), a=0.9)


# ================================================================================================ the kimono
def kimono_front(f):
    shade_cloth(f, f.m_all(), P1, [1.0, 2.4, 5.6, 7.0], y1=12.0)
    uroko(f, f.m_all(), y0=0.0)
    # the neckline: the throat and a pale under-kimono (juban) in the V, then the two collar bands. The wearer's right lapel (the viewer's left) runs from the left shoulder across to the
    # right hip and lies over the other one, which runs from the right shoulder under it.
    v = f.m_poly([(2.9, -0.1), (5.1, -0.1), (4.55, 5.0), (4.2, 5.0)])
    f.fill(v, H.lin((0, 0), (0, 5), [(0, SKIN[2]), (0.35, "#e8ded8"), (1, "#c8bedc")]))
    f.fill(f.m_poly([(2.9, -0.1), (5.1, -0.1), (4.0, 2.3)]), H.lin((0, 0), (0, 2.3), [(0, SKIN[1]), (1, SKIN[0])]))
    right = [(5.55, -0.2), (4.5, 5.45)]
    left = [(2.5, -0.2), (5.5, 8.05)]
    for pts, w, over in ((right, 1.0, False), (left, 1.05, True)):
        f.polyline([(x + 0.06, y + 0.07) for x, y in pts], "#06030c", w + 0.14, a=0.5)
        band = f.m_polyline(pts, w)
        f.fill(band, H.lin((0, 0), (0, 8), [(0, W2), (1, W1)]))
        H.bevel(f, band, depth=0.2, gain=0.6)
        for sgn in (-1, 1):
            f.polyline([(x + sgn * w * 0.42, y) for x, y in pts], LV2, 0.08, a=0.95)                    # the lavender piping along both edges of each collar
    f.fill(f.m_band(0.0, 0.25), W0, a=0.7)


def obi(f, front):
    m = f.m_band(8.0, 10.3)
    f.fill(m, H.lin((0, 8.0), (0, 10.3), [(0, LV3), (0.5, LV2), (1, LV1)]))
    # a brocade of small diamonds
    X, Y = f.X, f.Y
    d = np.minimum(np.abs(((X + (Y - 8.0)) % 1.2) - 0.6), 0.6)
    d2 = np.minimum(np.abs(((X - (Y - 8.0)) % 1.2) - 0.6), 0.6)
    f.fill(m * np.clip(1.0 - np.minimum(d, d2) * f.f / 0.7, 0, 1) * 0.45, LV0, a=0.55)
    H.folds(f, m, [1.0, 2.6, 5.4, 7.0] if f.w > 6 else [1.0, 3.0], depth=0.12, width=0.2, y0=8.0, y1=10.3)
    H.trim_band(f, 7.9, 8.2, G1, outline=0.5, spec=0.5)
    H.trim_band(f, 10.1, 10.4, G1, outline=0.5, spec=0.5)
    H.shadow_under(f, 10.4, 0.5, 0.32)
    f.glow_fill(f.m_band(7.9, 8.2), G2, 0.6)
    f.glow_fill(f.m_band(10.1, 10.4), G2, 0.6)
    if front:
        knot = f.m_rect(3.1, 8.2, 4.9, 10.1, 0.4)                                                  # the flat gold knot of the cord with two tassels
        f.fill(knot, H.metal_paint(8.2, 10.1, G2, contrast=0.7))
        H.bevel(f, knot, depth=0.3, gain=1.0, spec=0.5)
        f.fill(f.m_ring(4.0, 9.15, 0.5, 0.12), G0, a=0.8)
        for x in (3.6, 4.4):
            H.fringe(f, x - 0.18, x + 0.18, 10.4, 1.4, n=3, color=G2, dark=G0, width=0.1, seed=int(x * 10))
        H.stud(f, 4.0, 9.15, 0.2, G3, glow=G2)
        f.glow_fill(f.m_rect(3.1, 8.2, 4.9, 10.1, 0.4), G2, 0.45)


def below_obi(f):
    shade_cloth(f, f.m_band(10.3, 12.0), P1, [1.0, 2.4, 5.6, 7.0] if f.w > 6 else [1.0, 3.0], y0=10.3, y1=12.0)
    uroko(f, f.m_band(10.3, 12.0), y0=0.2)
    H.trim_band(f, 11.5, 12.0, LV2, outline=0.4, spec=0.3)


def crest(f):
    """The eight-necked snake: a white disc in a gold ring, eight gold necks curling out to eight small heads."""
    cx, cy = 4.0, 4.3
    f.fill(f.m_ellipse(cx, cy, 3.15, 3.15), "#0a0418", a=0.5)
    f.fill(f.m_ellipse(cx, cy, 3.0, 3.0), H.rad((cx - 0.8, cy - 0.9), 4.0, [(0, G3), (0.55, G2), (1, G0)]))
    f.fill(f.m_ellipse(cx, cy, 2.45, 2.45), H.tone(P1, 0.8))
    f.fill(f.m_ring(cx, cy, 2.7, 0.1), G0, a=0.8)
    for i in range(8):
        a0 = math.tau * i / 8.0 + 0.2
        neck = [(cx + math.cos(a0 + 0.55 * t) * (0.7 + 1.55 * t), cy + math.sin(a0 + 0.55 * t) * (0.7 + 1.55 * t)) for t in np.linspace(0.0, 1.0, 9)]
        f.polyline(neck, "#10061e", 0.42, a=0.9)
        f.polyline(neck, G2, 0.3)
        hx, hy = neck[-1]
        ang = a0 + 0.55 + 1.2
        f.ellipse(hx, hy, 0.36, 0.26, G3, rot=math.degrees(ang))
        f.ellipse(hx - 0.04, hy - 0.04, 0.12, 0.1, "#ffffff", a=0.8)
    f.ellipse(cx, cy, 0.95, 0.95, H.rad((cx - 0.3, cy - 0.3), 1.2, [(0, W2), (1, W0)]))
    f.fill(f.m_ring(cx, cy, 0.95, 0.1), G1, a=0.9)
    f.glow_fill(f.m_ellipse(cx, cy, 2.9, 2.9, soft=3.0), G2, 0.35)


def body(cv):
    f, ov = lay(cv, "body", "front")
    for g in (f, ov):
        kimono_front(g)
        below_obi(g)
        obi(g, True)
    for g in lay(cv, "body", "back"):
        shade_cloth(g, g.m_all(), P1, [1.0, 2.4, 5.6, 7.0], y1=12.0)
        uroko(g, g.m_all(), y0=0.0)
        g.fill(g.m_band(0.0, 0.8), H.lin((0, 0), (0, 0.8), [(0, W1), (1, W0)]))                 # the back of the collar
        H.trim_line(g, [(0.0, 0.85), (8.0, 0.85)], LV2, width=0.1, outline=0.3, spec=0.2)
        crest(g)
        below_obi(g)
        obi(g, False)
        musubi(g)
    for nm in ("west", "east"):
        for g in lay(cv, "body", nm):
            shade_cloth(g, g.m_all(), P1, [1.0, 3.0], y1=12.0)
            uroko(g, g.m_all(), y0=0.0)
            g.fill(g.m_band(0.0, 0.8), W1)
            below_obi(g)
            obi(g, False)
    # the kataginu: stiff indigo vest-wings over the shoulders and the upper chest, the lavender edge piped
    for g in (cv.face("body", "front", False), cv.face("body", "front", True)):
        for sg in (0, 1):
            x0, x1 = (0.0, 2.1) if sg == 0 else (5.9, 8.0)
            m = g.m_poly([(x0, 0.0), (x1, 0.0), (x1 + (-0.6 if sg == 0 else 0.6), 4.6), (x0, 3.8)])
            shade_cloth(g, m, I2, [x0 + 0.6, x0 + 1.4], y0=0.0, y1=4.6, top=1.15, bottom=0.8)
            edge = [(x1, 0.0), (x1 + (-0.6 if sg == 0 else 0.6), 4.6), (x0, 3.8)]
            g.polyline([(x + 0.05, y + 0.06) for x, y in edge], "#05030a", 0.3, a=0.5)
            H.trim_line(g, edge, LV2, width=0.2, outline=0.4, spec=0.4)
    for ovl in (False, True):
        t = cv.face("body", "top", ovl)
        t.fill(t.m_all(), H.lin((0, 0), (0, 4), [(0, W2), (1, W1)]))
        if ovl:
            t.fill(t.m_rect(1.2, 0.9, 6.8, 3.1, soft=1.0), "#000000", mode="erase")
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), P0)


def musubi(f):
    """The big obi knot at the small of the back: two loops and two hanging tails in lavender with a gold cord."""
    for sg in (-1, 1):
        loop = f.m_ellipse(4.0 + sg * 1.55, 9.0, 1.5, 1.0, rot=sg * 14)
        f.fill(f.m_ellipse(4.0 + sg * 1.55, 9.12, 1.6, 1.08, rot=sg * 14), "#10061e", a=0.55)
        f.fill(loop, H.lin((0, 8.0), (0, 10.0), [(0, LV3), (1, LV1)]))
        H.bevel(f, loop, depth=0.3, gain=0.8, spec=0.35)
        f.fill(f.m_ellipse(4.0 + sg * 1.6, 9.0, 0.8, 0.4, rot=sg * 14), LV0, a=0.75)
        tail = f.m_poly([(4.0 + sg * 0.3, 9.6), (4.0 + sg * 1.4, 9.6), (4.0 + sg * 1.9, 12.0), (4.0 + sg * 0.7, 12.0)])
        f.fill(tail, H.lin((0, 9.6), (0, 12.0), [(0, LV2), (1, LV1)]))
        H.folds(f, tail, [4.0 + sg * 1.1], depth=0.18, width=0.14, y0=9.6, y1=12.0)
        H.trim_line(f, [(4.0 + sg * 0.3, 9.6), (4.0 + sg * 0.7, 12.0)], G1, width=0.09, outline=0.4, spec=0.3)
    knot = f.m_rect(3.2, 8.3, 4.8, 9.8, 0.4)
    f.fill(knot, H.metal_paint(8.3, 9.8, G2, contrast=0.7))
    H.bevel(f, knot, depth=0.3, gain=1.0, spec=0.5)
    f.glow_fill(f.m_rect(3.2, 8.3, 4.8, 9.8, 0.4), G2, 0.45)
    for x in (3.5, 4.5):
        f.line((x, 8.5), (x, 9.6), G0, 0.05, 0.05, a=0.7)


# ================================================================================================ the sleeves
def arm(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        for f in (base, ov):
            shade_cloth(f, f.m_all(), P1, [1.0, 2.7] if f.w > 3 else [1.2], y1=10.9)
            uroko(f, f.m_band(3.5, 8.0), y0=0.4)
            # the winged shoulder (kataginu): stiff indigo with a lavender piped edge and a pointed lower rim
            wing = f.m_poly([(-0.1, -0.1), (f.w + 0.1, -0.1), (f.w + 0.1, 3.3), (f.w * 0.5, 4.3), (-0.1, 3.3)])
            shade_cloth(f, wing, I2, [f.w * 0.3, f.w * 0.7], y0=0.0, y1=4.0, top=1.2, bottom=0.8)
            edge = [(-0.1, 3.3), (f.w * 0.5, 4.3), (f.w + 0.1, 3.3)]
            f.polyline([(x + 0.04, y + 0.07) for x, y in edge], "#05030a", 0.3, a=0.5)
            H.trim_line(f, edge, LV2, width=0.2, outline=0.4, spec=0.4)
            H.shadow_under(f, 4.4, 0.5, 0.25, mask=f.m_band(4.3, 5.0))
            # the wide sleeve opening with its lilac lining
            f.fill(f.m_band(8.3, 10.7), H.lin((0, 8.3), (0, 10.7), [(0, LV2), (0.6, LV1), (1, LV0)]))
            H.folds(f, f.m_band(8.3, 10.7), [0.8, 2.0, 3.1], depth=0.16, width=0.18, y0=8.3, y1=10.7)
            f.fill(f.m_band(8.15, 8.45), H.lin((0, 8.15), (0, 8.45), [(0, P0), (1, P1)]))
            H.trim_line(f, [(0.0, 8.35), (f.w, 8.35)], LV3, width=0.1, outline=0.3, spec=0.2)
            f.fill(f.m_band(10.4, 10.75), LV0, a=0.9)
        ov.fill(ov.m_band(10.8, 12.0), "#000000", mode="erase")
        # the pale hand under the sleeve
        base.fill(base.m_band(10.75, 12.0), H.lin((0, 10.75), (0, 12.0), [(0, SKIN[1]), (1, SKIN[0])]))
        if nm == "front":
            for x in (1.0, 2.0, 3.0):
                base.line((x, 11.25), (x, 11.9), H.tone(SKIN[0], 0.8), 0.05, 0.05, a=0.6)
    for ovl in (False, True):
        t = cv.face(part, "top", ovl)
        t.fill(t.m_all(), H.rad((2, 2), 3, [(0, I3), (1, I1)]))
        t.fill(t.m_ring(2, 2, 1.7, 0.12), LV2, a=0.9)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), SKIN[1])
    cv.face(part, "bottom", True).clear()


# ================================================================================================ the legs
def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        for f in (base, ov):
            shade_cloth(f, f.m_all(), I1, [0.7, 1.7, 2.8, 3.6] if nm in ("front", "back") else [0.9, 2.2, 3.2], y0=0.2, y1=10.0, top=1.2, bottom=0.8, depth=0.3)
            H.sheen(f, f.m_band(0.0, 10.0), [1.2, 3.0], width=0.15, strength=0.14, color="#7a6ad0", y0=0.5, y1=9.6)
            # the hem: a lavender line and an indigo band
            H.trim_band(f, 9.1, 9.7, LV2, outline=0.4, spec=0.3)
            f.fill(f.m_band(9.7, 10.5), H.lin((0, 9.7), (0, 10.5), [(0, I2), (1, I1)]))
            H.shadow_under(f, 10.5, 0.4, 0.3)
            # the white tabi and the wooden geta
            f.fill(f.m_band(10.5, 11.4), H.lin((0, 10.5), (0, 11.4), [(0, W2), (1, W0)]))
            f.fill(f.m_band(11.3, 12.0), H.lin((0, 11.3), (0, 12.0), [(0, WD[2]), (1, WD[0])]))
            f.line((0.0, 11.3), (f.w, 11.3), "#0a0612", 0.07, 0.07, a=0.7)
            if nm == "front":
                f.line((0.4, 10.7), (2.0, 11.2), "#c8283a", 0.15, 0.15, a=0.95)                   # the red thongs of the geta
                f.line((3.6, 10.7), (2.0, 11.2), "#c8283a", 0.15, 0.15, a=0.95)
            if nm in ("west", "east"):
                for x0 in (0.3, 2.7):
                    f.rect(x0, 11.3, x0 + 1.0, 12.0, WD[1], r=0.1)
        ov.fill(ov.m_band(10.5, 12.0), "#000000", mode="erase")
    t = cv.face(part, "top")
    t.fill(t.m_all(), I1)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), WD[0])
    cv.face(part, "top", True).clear()
    cv.face(part, "bottom", True).clear()


def paint(spec, cid, masks):
    cv = H.Canvas(256, False)
    for f in cv.faces(overlay=(False,)):
        f.fill(f.m_all(), P1)
    head(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    cv.face("head", "top", True).clear()
    skin, glow = cv.finish()
    return skin, glow, "anime_topknot", HAIR, False

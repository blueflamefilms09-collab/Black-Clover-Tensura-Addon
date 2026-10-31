"""Yammy Llargo (Bleach, the Espada 10 / Cero Espada), HD 256x256 (3.38.0, group a of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_a.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/yammy_llargo.py): a huge tanned brute with spiked black hair and a bone mask fragment at the right temple, small fierce eyes with an orange
glowing iris, a heavy frown, a crack scar; the white Espada jacket worn open over a bare muscular chest with the numeral 10 on the belly (orange, glowing), broad white shoulders, a high collar, black
wrist guards with white tape, a black sash over wide pleated white hakama, black tabi and wooden geta. HD adds: a really drawn chest (pecs, abs, ribs, collarbones, forearm tendons), a face with a
scar and stubble, a bone fragment with cracks, jacket with piping and stitching, a knotted sash, pleats that fold, geta with their two teeth.
Hair: `anime_spike_tall` (tall thick back-swept spikes), blue-black. Original art from memory of the canon look. Glow: the iris and the numeral 10.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402

SK = ("#955a34", "#d09058", "#eab27a", "#f8d2a0")          # tan skin: shade, base, light, highlight
HAIR = (42, 42, 62)
HAIRC = "#2a2a3e"
W0, W1, W2 = "#bdbccb", "#e6e6ef", "#fbfbff"                # white jacket / hakama: shade, base, light
K0, K1, K2 = "#0c0c14", "#1c1c28", "#34344a"               # black
ORG, ORG_L = "#ff7a2c", "#ffc27a"
BONE = ("#7a7360", "#bdb69e", "#e8e3cc", "#f8f5e6")
WOOD = ("#4a2c18", "#7a4a2a", "#a06a3c")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def skin_fill(f, mask=None, k=1.0):
    mask = f.m_all() if mask is None else mask
    f.fill(mask, H.lin((0, 0), (0, f.h), [(0, H.tone(SK[2], k)), (0.5, H.tone(SK[1], k)), (1, H.tone(SK[0], k * 1.1))]))


def hair_base(f, c=HAIRC):
    """Hair painted on the base head (what shows when the 3D hair is dropped beyond 24 blocks)."""
    f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, H.tone(c, 1.35)), (1, H.tone(c, 0.7))]))
    for i in range(int(f.w * 2.5)):
        x = (i + 0.5) / 2.5 + 0.2 * math.sin(i * 1.7)
        f.line((x, 0.0), (x + 0.25 * math.sin(i), f.h), H.tone(c, 0.6 if i % 2 else 1.6), 0.07, 0.04, a=0.55)


# ================================================================================================ the head
def head(cv):
    f = cv.face("head", "front")
    H.skin_face(f, (SK[0], SK[1], SK[2]), hair=H.tone(HAIRC, 0.95), hairline=2.0, wave=0.7, shadow=0.26)
    H.brow(f, (3.6, 3.95), (0.8, 3.0), "#140e12", 0.42, 0.22, arch=-0.05)                  # thick brows slanting down to the nose: an angry look
    H.brow(f, (4.4, 3.95), (7.2, 3.0), "#140e12", 0.42, 0.22, arch=-0.05)
    iris = ("#5a1c06", "#ff7a2c", "#ffd49a")
    for cx in (2.2, 5.8):
        H.eye(f, cx, 4.95, 2.1, 1.25, iris, lash="#0c0608", sclera="#f4f0e6", tilt=0.22, pupil="#120606", glow="#a04810", lid_shadow=0.5, lash_w=0.36, flick=0.1, hi=0.5)
    f.fill(f.m_ellipse(2.2, 4.5, 1.2, 0.38, soft=3.0), "#3a2010", a=0.3)                      # a heavy lid over each eye
    f.fill(f.m_ellipse(5.8, 4.5, 1.2, 0.38, soft=3.0), "#3a2010", a=0.3)
    # a broad nose, a heavy jaw, a deep frown
    f.line((3.7, 4.6), (3.55, 6.2), H.tone(SK[0], 0.8), 0.22, 0.34, a=0.5)
    f.line((4.3, 4.6), (4.45, 6.2), H.tone(SK[0], 0.8), 0.22, 0.34, a=0.5)
    f.ellipse(3.4, 6.35, 0.38, 0.22, H.tone(SK[0], 0.55), a=0.85)
    f.ellipse(4.6, 6.35, 0.38, 0.22, H.tone(SK[0], 0.55), a=0.85)
    f.ellipse(4.0, 6.3, 0.55, 0.3, SK[3], a=0.35, soft=2.5)
    H.mouth_smile(f, 4.0, 7.35, 3.5, color="#2a0c0c", lip="#8a4a3a", depth=-0.2, thick=0.24, teeth="#f2eee0", open_h=0.28)
    f.fill(f.m_ellipse(4.0, 7.85, 1.3, 0.3, soft=3.0), H.tone(SK[0], 0.6), a=0.35)
    for sg in (-1, 1):
        f.polyline([(4.0 + sg * 1.55, 6.55), (4.0 + sg * 1.95, 7.1), (4.0 + sg * 1.8, 7.7)], H.tone(SK[0], 0.65), 0.1, a=0.5)             # the heavy lower lip's shadow
    for k in range(22):                                                                         # stubble round the jaw
        x, y = 0.5 + 7.0 * H.hsh(k, 11), 6.6 + 1.3 * H.hsh(k, 12)
        if abs(x - 4.0) > 0.9 or y > 7.7:
            f.ellipse(x, y, 0.035, 0.035, "#3a2418", a=0.4)
    # the crack scar across the right cheek (viewer's right): a pale seam with a dark edge and three stitch marks
    scar = [(6.5, 3.6), (6.1, 4.3), (6.3, 5.0), (5.7, 6.0)]
    f.polyline([(x + 0.07, y + 0.05) for x, y in scar], "#3a1a10", 0.17, a=0.7)
    f.polyline(scar, "#e0a8a0", 0.1, a=0.9)
    for (x, y) in ((6.35, 4.0), (6.2, 4.7), (6.05, 5.45)):
        f.line((x - 0.22, y), (x + 0.22, y + 0.06), "#e0a8a0", 0.06, 0.06, a=0.8)
    # the face roundness
    f.shade(f.m_all(), 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 1.0, np.minimum(f.X, 8.0 - f.X))))
    # the bone mask fragment: a curved plate over the right (viewer's) temple, seen from the front on the hat layer
    for ov in (False, True):
        g = cv.face("head", "front", ov)
        bone_front(g)
        for nm in ("west", "east", "back"):
            h = cv.face("head", nm, ov)
            if not ov:
                hair_base(h)
            if nm == "east":
                bone_side(h, ov)
    t = cv.face("head", "top")
    hair_base(t)
    cv.face("head", "bottom").fill(cv.face("head", "bottom").m_all(), SK[0])
    top = cv.face("head", "front", False)
    # the hair roots hide the top rows of the base head
    top.fill(top.m_band(0.0, 1.6), H.lin((0, 0), (0, 1.6), [(0, H.tone(HAIRC, 1.2)), (1, H.tone(HAIRC, 0.85))]), a=0.95)


def bone_front(f):
    """The hollow's mask fragment: an ivory plate on the viewer's right temple with tooth-like ridges, cracks and a dark socket."""
    plate = f.m_poly([(6.0, 0.7), (8.2, 0.7), (8.2, 4.7), (7.5, 4.2), (7.15, 3.5), (6.6, 3.3), (6.25, 2.3)])
    f.fill(f.m_poly([(5.9, 0.9), (8.3, 0.9), (8.3, 4.9), (7.5, 4.45), (7.0, 3.7), (6.45, 3.45), (6.1, 2.4)]), "#241410", a=0.55)
    f.fill(plate, H.lin((6.0, 0.8), (8.0, 4.6), [(0, BONE[3]), (0.5, BONE[2]), (1, BONE[1])]))
    H.bevel(f, plate, depth=0.26, gain=1.0, spec=0.3)
    for (x0, y0, x1, y1) in ((6.5, 2.7, 6.9, 3.2), (7.0, 2.9, 7.4, 3.6)):
        f.line((x0, y0), (x1, y1), BONE[0], 0.1, 0.05, a=0.8)
    f.polyline([(7.2, 1.8), (7.0, 2.4), (7.3, 2.8)], BONE[0], 0.06, a=0.8)
    f.ellipse(7.7, 3.7, 0.22, 0.22, "#241410", a=0.9)


def bone_side(f, ov):
    """The same fragment on the head's left side (east face, its front at the left end)."""
    plate = f.m_poly([(0.0, 0.8), (3.9, 0.6), (3.2, 3.3), (0.0, 4.9)])
    f.fill(plate, H.lin((0, 0.8), (3.5, 3.3), [(0, BONE[3]), (0.5, BONE[2]), (1, BONE[1])]))
    H.bevel(f, plate, depth=0.25, gain=0.9)
    f.polyline([(1.2, 1.5), (1.7, 2.3), (1.4, 3.1)], BONE[0], 0.07, a=0.85)
    f.polyline([(2.4, 1.2), (2.6, 2.0)], BONE[0], 0.06, a=0.85)


# ================================================================================================ the chest and the jacket
def chest(f):
    """The bare, muscular chest on the base layer of the body front."""
    skin_fill(f)
    for cx in (2.45, 5.55):                                                               # the pecs
        pec = f.m_ellipse(cx, 2.75, 2.05, 1.55, rot=-8 if cx < 4 else 8)
        f.fill(pec, H.rad((cx - 0.3, 2.2), 2.2, [(0, SK[3]), (0.55, SK[2]), (1, SK[1])]), a=0.9)
        f.fill(f.m_ellipse(cx, 4.15, 2.0, 0.5, soft=3.0), H.tone(SK[0], 0.7), a=0.65)         # the shadow under the pec
        f.fill(f.m_ellipse(cx + (0.5 if cx < 4 else -0.5), 2.2, 0.9, 0.35, rot=-10, soft=3.0), SK[3], a=0.4)
    f.line((4.0, 1.4), (4.0, 4.3), H.tone(SK[0], 0.65), 0.2, 0.36, a=0.7)                  # the sternum groove
    f.line((0.4, 1.0), (3.4, 0.6), H.tone(SK[0], 0.7), 0.1, 0.2, a=0.45)                  # the collarbones
    f.line((7.6, 1.0), (4.6, 0.6), H.tone(SK[0], 0.7), 0.1, 0.2, a=0.45)
    for r, y in enumerate((4.65, 5.85, 7.05)):                                              # the abs: two columns of three rounded blocks
        for cx in (2.95, 5.05):
            blk = f.m_rect(cx - 0.78, y, cx + 0.78, y + 1.0, 0.35)
            f.fill(blk, H.lin((0, y), (0, y + 1.0), [(0, SK[2]), (1, SK[1])]))
            H.bevel(f, blk, depth=0.3, gain=0.5, spec=0.0)
            f.fill(f.m_rect(cx - 0.78, y + 0.95, cx + 0.78, y + 1.12, soft=3.0), H.tone(SK[0], 0.6), a=0.32)
    f.line((4.0, 4.5), (4.0, 8.2), H.tone(SK[0], 0.65), 0.14, 0.2, a=0.7)                  # the linea alba
    for sg in (-1, 1):                                                                       # the ribs and obliques at both sides
        for k in range(3):
            xa = 0.35 if sg < 0 else 7.65
            f.line((xa, 3.8 + k * 0.9), (xa + sg * -1.0, 4.35 + k * 0.9), H.tone(SK[0], 0.7), 0.07, 0.1, a=0.45)


def numeral(f):
    """The numeral 10 on the belly: a dark ink tattoo with a thin orange glowing core."""
    cx, cy = 4.95, 6.5
    one = [(3.0, 5.75), (3.4, 5.45), (3.4, 7.5)]

    def shapes(k):
        zero = H.minus(f.m_ellipse(cx, cy, 0.78 - k, 1.05 - k), f.m_ellipse(cx, cy, 0.44 + k, 0.7 + k))
        return H.union(zero, f.m_polyline(one, 0.46 - 2 * k), f.m_line((2.8, 7.5), (4.0, 7.5), 0.4 - 2 * k))
    f.fill(shapes(0.0), "#1c0804", a=0.9)
    f.fill(shapes(0.08), ORG, glow=ORG)
    f.fill(shapes(0.15), ORG_L, a=0.9, glow=ORG_L)


def jacket(f, left):
    """One front panel of the open jacket: a lapel strip, widest at the shoulder, narrowing toward the sash, edged in black with stitching."""
    x0, xt, xb = (0.0, 1.85, 1.25) if left else (8.0, 6.15, 6.75)
    m = f.m_poly([(x0, 0.0), (xt, 0.0), (xb, 8.35), (x0, 8.35)])
    cx = 0.75 if left else 7.25
    H.cloth(f, m, W1, top=1.06, bottom=0.86, round_=0.1, fold_xs=[cx - 0.2, cx + 0.5] if left else [cx - 0.5, cx + 0.2], fold_depth=0.12, fold_y0=1.0, fold_y1=8.3)
    edge = [(xt, 0.0), (xb, 8.35)]
    f.polyline([(x + (0.07 if left else -0.07), y) for x, y in edge], "#05050a", 0.34, a=0.55)
    H.trim_line(f, edge, K1, width=0.3, outline=0.0, spec=0.4)
    H.stitch(f, [(x + (-0.3 if left else 0.3), y) for x, y in edge], "#c4c3d2", width=0.05, dash=(0.28, 0.2), shadow=0.0)
    return m


def body(cv):
    # the base layer: bare chest, the black sash and the hakama's top
    f = cv.face("body", "front", False)
    chest(f)
    numeral(f)
    for nm in ("back", "west", "east"):
        g = cv.face("body", nm, False)
        H.cloth(g, g.m_all(), W1, top=1.06, bottom=0.86, fold_xs=[1.5, 3.0, 4.6, 6.2] if g.w > 6 else [1.3, 2.7], fold_depth=0.12)
    for nm in SIDES:
        for g in lay(cv, "body", nm):
            sash(g, nm)
    # the second layer: the open jacket
    ov = cv.face("body", "front", True)
    jacket(ov, True)
    jacket(ov, False)
    collar(ov, front=True)
    for nm in ("back", "west", "east"):
        g = cv.face("body", nm, True)
        H.cloth(g, g.m_band(0.0, 8.3), W1, top=1.08, bottom=0.88, round_=0.1, fold_xs=[1.3, 2.8, 5.2, 6.7] if g.w > 6 else [1.3, 2.7], fold_depth=0.13, fold_y0=1.0, fold_y1=8.3)
        collar(g, front=False)
    bk = cv.face("body", "back", True)
    H.trim_line(bk, [(4.0, 1.8), (4.0, 8.3)], W0, width=0.14, outline=0.0, spec=0.0)
    H.stitch(bk, [(4.2, 1.8), (4.2, 8.3)], "#c4c3d2", width=0.05, dash=(0.28, 0.2), shadow=0.0)
    for ovl in (False, True):
        t = cv.face("body", "top", ovl)
        t.fill(t.m_all(), W2)
        if ovl:
            t.fill(t.m_rect(1.2, 0.9, 6.8, 3.1, soft=1.0), "#000000", mode="erase")
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), W0)


def collar(f, front):
    """The tall white Espada collar with a black rim, standing up round the neck."""
    h = 1.9
    m = f.m_band(0.0, h)
    if front:
        m = H.minus(m, f.m_poly([(2.15, -0.1), (5.85, -0.1), (5.85, 1.0), (4.0, 2.1), (2.15, 1.0)]))
    f.fill(m, H.lin((0, 0), (0, h), [(0, W2), (1, W1)]))
    H.folds(f, m, [1.0, 2.2, 3.5, 4.8, 6.0, 7.0], depth=0.1, width=0.2, y0=0.2, y1=h)
    f.fill(m * f.m_band(0.0, 0.4), K1)
    f.fill(m * f.m_band(0.38, 0.48), "#d8d8e6", a=0.8)
    H.shadow_under(f, h, 0.45, 0.28, mask=m)


def sash(f, nm):
    """The black sash round the waist: folds, a sheen, a stitched edge; the hakama's white top and a koshita plate below."""
    m = f.m_band(8.3, 10.2)
    f.fill(m, H.lin((0, 8.3), (0, 10.2), [(0, H.tone(K2, 1.0)), (0.45, K1), (1, K0)]))
    H.folds(f, m, [0.9, 2.2, 3.7, 5.0, 6.4, 7.5] if f.w > 6 else [1.0, 2.5], depth=0.2, width=0.2, y0=8.3, y1=10.2)
    H.sheen(f, m, [1.6, 4.4, 6.8] if f.w > 6 else [1.5], width=0.22, strength=0.18, color="#9a9ac0", y0=8.4, y1=10.1)
    f.fill(f.m_band(8.3, 8.45), "#05050a", a=0.6)
    H.shadow_under(f, 10.2, 0.5, 0.25)
    hak = f.m_band(10.2, 12.0)
    H.cloth(f, hak, W1, top=1.04, bottom=0.84, round_=0.1, fold_xs=[1.0, 2.3, 3.7, 5.0, 6.4, 7.5] if f.w > 6 else [1.0, 2.6], fold_depth=0.16, fold_y0=10.3, fold_y1=12.0)
    if nm == "front":
        f.polyline([(4.0, 8.4), (4.0, 10.2)], "#05050a", 0.1, a=0.5)
        knot = f.m_rect(3.0, 8.5, 5.0, 10.0, 0.4)                                            # the knot of the sash
        f.fill(knot, H.lin((0, 8.5), (0, 10.0), [(0, K2), (1, K0)]))
        H.bevel(f, knot, depth=0.3, gain=0.9, spec=0.35)
    if nm == "back":
        pl = f.m_rect(1.6, 10.3, 6.4, 11.9, 0.3)                                              # the stiff back plate of the hakama
        f.fill(pl, H.lin((0, 10.3), (0, 11.9), [(0, W2), (1, W1)]))
        H.bevel(f, pl, depth=0.2, gain=0.7)
    if nm == "back":
        for sg, x in ((-1, 3.2), (1, 4.8)):                                                    # the two hanging tails of the sash
            tail = f.m_poly([(x - 0.7, 9.3), (x + 0.7, 9.3), (x + sg * 0.9 + 0.6, 11.9), (x + sg * 0.9 - 0.6, 11.9)])
            f.fill(tail, H.lin((0, 9.3), (0, 11.9), [(0, K2), (1, K0)]))
            H.folds(f, tail, [x + sg * 0.4], depth=0.2, width=0.15, y0=9.3, y1=11.9)
            f.polyline([(x - 0.6, 9.4), (x - 0.4 + sg * 0.9, 11.8)], "#6a6a90", 0.05, a=0.6)
        knot = f.m_rect(3.1, 8.6, 4.9, 9.9, 0.35)
        f.fill(knot, H.lin((0, 8.6), (0, 9.9), [(0, K2), (1, K0)]))
        H.bevel(f, knot, depth=0.3, gain=0.9, spec=0.35)


# ================================================================================================ the arms
def arm(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        # the bare arm: the forearm bulges, a tendon line, knuckles on the fist
        skin_fill(base)
        base.fill(base.m_ellipse(2.0, 6.4, 1.1, 1.6, soft=3.0), SK[3], a=0.35)
        base.line((1.0, 4.8), (1.2, 7.8), H.tone(SK[0], 0.7), 0.1, 0.06, a=0.4)
        base.line((2.9, 4.8), (2.8, 7.8), H.tone(SK[0], 0.7), 0.1, 0.06, a=0.4)
        base.fill(base.m_band(10.3, 12.0), H.lin((0, 10.3), (0, 12.0), [(0, SK[1]), (1, SK[0])]))
        if nm == "front":
            for x in (0.7, 1.55, 2.4, 3.25):
                base.line((x, 11.2), (x, 11.9), H.tone(SK[0], 0.6), 0.06, 0.06, a=0.7)
        # the broad white sleeve of the jacket, with a black rim
        for f in (base, ov):
            H.cloth(f, f.m_band(0.0, 4.6), W1, top=1.1, bottom=0.88, round_=0.12, fold_xs=[1.0, 2.7] if f.w > 3 else [1.2], fold_depth=0.14, fold_y0=0.4, fold_y1=4.5)
            f.fill(f.m_band(4.55, 5.1), H.lin((0, 4.55), (0, 5.1), [(0, K2), (1, K0)]))
            H.stitch(f, [(0.2, 4.35), (f.w - 0.2, 4.35)], "#c4c3d2", width=0.05, dash=(0.28, 0.2), shadow=0.0)
            H.shadow_under(f, 5.1, 0.5, 0.3)
            # the wrist guard: black leather with a white tape band
            g = f.m_band(8.0, 10.4)
            f.fill(g, H.lin((0, 8.0), (0, 10.4), [(0, K2), (0.5, K1), (1, K0)]))
            H.sheen(f, g, [1.2, 2.8], width=0.16, strength=0.2, color="#9a9ac0", y0=8.0, y1=10.4)
            H.trim_band(f, 8.0, 8.8, "#e8e6f0", outline=0.4, spec=0.2)
            f.fill(f.m_band(10.3, 10.45), "#05050a", a=0.6)
        ov.fill(ov.m_band(10.4, 12.0), "#000000", mode="erase")
        ov.fill(ov.m_band(5.1, 8.0), "#000000", mode="erase")
    for ovl in (False, True):
        t = cv.face(part, "top", ovl)
        t.fill(t.m_all(), H.rad((2, 2), 3, [(0, W2), (1, W1)]))
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), SK[0])


# ================================================================================================ the legs
def hakama(f, nm):
    f.fill(f.m_all(), "#ffffff")
    H.cloth(f, f.m_band(0.0, 10.3), W1, top=1.1, bottom=0.84, round_=0.14, fold_xs=[0.7, 1.7, 2.9, 3.6] if nm in ("front", "back") else [0.9, 2.2, 3.2], fold_depth=0.22, fold_y0=0.3, fold_y1=10.2)
    H.sheen(f, f.m_band(0.0, 10.3), [1.2, 3.0], width=0.16, strength=0.12, color="#ffffff", y0=0.5, y1=9.8)
    f.fill(f.m_band(9.55, 10.3), H.lin((0, 9.55), (0, 10.3), [(0, W1), (1, W0)]))
    H.trim_band(f, 9.7, 10.3, K1, outline=0.0, spec=0.3)                                   # the black hem band
    f.fill(f.m_band(9.7, 9.8), "#e8e6f0", a=0.7)


def tabi_and_geta(f, nm):
    f.fill(f.m_band(10.3, 12.0), H.lin((0, 10.3), (0, 12.0), [(0, "#14141c"), (1, "#05050a")]))
    if nm == "front":
        f.line((0.4, 10.6), (2.0, 11.4), WOOD[2], 0.16, 0.16, a=0.9)                       # the geta thong
        f.line((3.6, 10.6), (2.0, 11.4), WOOD[2], 0.16, 0.16, a=0.9)
    f.fill(f.m_band(11.2, 12.0), H.lin((0, 11.2), (0, 12.0), [(0, WOOD[2]), (1, WOOD[0])]))   # the wooden geta
    f.line((0.0, 11.2), (f.w, 11.2), "#05050a", 0.08, 0.08, a=0.7)
    if nm in ("west", "east"):
        for x0 in (0.3, 2.7):
            f.rect(x0, 11.2, x0 + 1.0, 12.0, WOOD[1], r=0.1)


def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        hakama(ov, nm)
        hakama(base, nm)
        tabi_and_geta(base, nm)
        tabi_and_geta(ov, nm)
        ov.fill(ov.m_band(10.3, 12.0), "#000000", mode="erase")
    for ovl in (False, True):
        t = cv.face(part, "top", ovl)
        t.fill(t.m_all(), W1)
        b = cv.face(part, "bottom", ovl)
        b.fill(b.m_all(), WOOD[0])
    cv.face(part, "top", True).clear()
    cv.face(part, "bottom", True).clear()


def paint(spec, cid, masks):
    cv = H.Canvas(256, False)
    for f in cv.faces(overlay=(False,)):
        f.fill(f.m_all(), SK[1])
    head(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    skin, glow = cv.finish()
    return skin, glow, "anime_spike_tall", HAIR, False

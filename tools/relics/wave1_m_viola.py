"""Viola of Moonlit Spirits (card: Mr James Gifford style sheet, "Viola of Moonlit Spirits", rare): a dark ornate viola with a flat wide paddle-shaped
body, silver filigree swirls, glowing pale-blue crescent moon sound holes, bone pegs and a crescent-hooked scroll, ghostly blue wisps rising; its bow
lies beside it. The 3D model, built with the owner's method (stood upright, neck up, like the other tools).

Reference: the card, 720 x 672; the instrument lies on its side with its tail at card x 140 and the scroll's tip at x 608 (468 px).
Model: y up the instrument from the tail (0) to the scroll; x across (card y down = +x); the face is +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 0.67                                       # a viola with its scroll
X0, X1 = 140.0, 608.0                                # card x of the tail and the scroll tip
K = w.px_to_cm(TOTAL_M * 100.0, X1 - X0)             # cm per card px (0.143)
YC = 235.5                                           # card y of the instrument's axis (the body spans y 163..308)
BODY_W = w.lerp_table([(150, 60), (158, 118), (170, 142), (380, 142), (405, 118), (420, 62), (432, 30), (437, 24)])   # full body width in px along the card x
BODY_X = (150.0, 437.0)                              # the body from the tail to where the neck begins
BODY_T = 6.0                                         # the body's depth in cm (not on the card: a flat paddle)
NECK_X = (430.0, 522.0)                              # the neck / fingerboard
NECK_W = (24.0, 20.0)                                # its width in px at both ends
PEGBOX_X = (520.0, 553.0)                            # the pegbox with the bone pegs
PEGBOX_W = 34.0
STRING_X = (166.0, 540.0)                            # the strings run from the tailpiece to the nut
BRIDGE_X = 212.0
HOLE_CX = 288.0                                      # the two crescent sound holes at card x about 300 (px)
HOLE_UP, HOLE_LO = 200.0, 272.0                      # their card y centres
FACE = BODY_T / 2


def M(xpx, ypx, z=0.0):
    """Card (x, y) to model (x, y, z)."""
    return ((ypx - YC) * K, (xpx - X0) * K, z)


def body_half(xpx):
    return BODY_W(xpx) / 2 * K


def build():
    m = ml.Mesh("viola_of_moonlit_spirits")
    xs = np.arange(BODY_X[0], BODY_X[1] + 0.1, 6.0)
    # ---- 1 the body: a flat paddle lofted along y, section a rounded rectangle
    body = w.add(m, "body", "wood", base=(46, 44, 52), smooth=30, wear=0.7)
    st = [((x - X0) * K, BODY_W(x) * K / (142 * K), 1.0) for x in xs]
    ml.loft(body, "y", (0, 0, 0), st, ml.rounded_rect(142 * K, BODY_T, 1.0, 3))
    # ---- 2 the raised silver filigree border, swept round the face a little inside the edge
    bd = w.add(m, "face_border", "silver", base=(156, 160, 174), smooth=45, wear=0.8)
    outline = [M(x, YC - BODY_W(x) / 2 + 9, FACE + 0.02) for x in xs] + [M(x, YC + BODY_W(x) / 2 - 9, FACE + 0.02) for x in xs[::-1]]
    outline.append(outline[0])
    ml.sweep(bd, outline, ml.circle(0.2, 6), caps=(False, False))
    # ---- 3-6 filigree swirls in the four corners: small spirals
    sw = w.add(m, "corner_swirls", "silver", base=(146, 150, 166), smooth=45)
    for cx, cy, sx, sy in ((178, 176, 1, 1), (178, 295, 1, -1), (392, 176, -1, 1), (392, 295, -1, -1)):
        pts = []
        for i in range(0, 22):
            a = i * 0.42
            r = 14.0 * (1.0 - i / 24.0)
            pts.append(M(cx + sx * (r * math.cos(a)), cy + sy * (r * math.sin(a)), FACE + 0.05))
        ml.sweep(sw, pts, ml.circle(0.17, 6), scales=lambda t: (1.0 - 0.5 * t, 1.0 - 0.5 * t))
    # ---- 7/8 the crescent sound holes: two crescents opening toward the middle, glowing
    for nm, cy, sg in (("hole_upper", HOLE_UP, 1), ("hole_lower", HOLE_LO, -1)):
        h = w.add(m, nm, "crystal", base=(150, 214, 240), glow=True, smooth=40)
        R = 24.0
        pts = []
        for i in range(0, 17):
            a = math.radians(-70 + 140 * i / 16)
            pts.append(M(HOLE_CX + R * math.sin(a) * 1.0, cy + sg * (-R * math.cos(a) * 0.9 + R * 0.45), FACE + 0.06))
        ml.sweep(h, pts, ml.lozenge(1.0, 1.0, 0.0), scales=lambda t: (12.0 * K * math.sin(math.pi * t) + (0.0 if 0 < t < 1 else 0.0), 0.2 if 0 < t < 1 else 0.0), up=(0, 0, 1))
    # ---- 9 tailpiece, 10 bridge, 11 endpin
    tp = w.add(m, "tailpiece", "wood", base=(40, 36, 36), smooth=40)
    ml.loft(tp, "y", (0, 0, FACE), [(M(172, YC)[1]), (M(236, YC)[1])], ml.rounded_rect(22 * K, 0.9, 0.3, 2))
    br = w.add(m, "bridge", "bone", base=(200, 190, 160), wear=0.4)
    bx = M(BRIDGE_X, YC)[1]
    ml.loft(br, "y", (0, bx, FACE + 0.6), [(-0.35), (0.35)], ml.rounded_rect(30 * K, 1.0, 0.2, 2))
    ep = w.add(m, "endpin", "wood", base=(34, 28, 28), smooth=60)
    ml.lathe(ep, "y", (0, 0, 0), [(-1.2, 0.0), (-0.9, 0.9), (0.2, 1.1), (1.0, 0.6)], sides=10)
    # ---- 12 the neck and 13 the fingerboard laid on it
    ny0, ny1 = M(NECK_X[0], YC)[1], M(NECK_X[1], YC)[1]
    nk = w.add(m, "neck", "wood", base=(52, 40, 36), uv_mode="loft", smooth=35)
    ml.loft(nk, "y", (0, 0, 0), [(ny0, 1.0, 1.0), (ny1, NECK_W[1] / NECK_W[0], 0.9)], ml.rounded_rect(NECK_W[0] * K, 2.4, 0.5, 2))
    fb = w.add(m, "fingerboard", "obsidian", base=(40, 36, 44), wear=0.3)
    ml.loft(fb, "y", (0, 0, 1.15), [(ny0 + 1.0, 0.92, 1.0), (ny1, 0.82, 1.0)], ml.rounded_rect(NECK_W[0] * K, 0.5, 0.15, 2))
    # ---- 14 four strings, 15 the nut
    sg = w.add(m, "strings", "silver", base=(206, 210, 222), wear=0.1, smooth=60)
    sy0, sy1 = M(STRING_X[0], YC)[1], M(STRING_X[1], YC)[1]
    for i in range(4):
        off = (i - 1.5) * 0.52
        a = (off * (1.0 if i else 1.0), sy0, FACE + 0.3)
        p0 = (off * 0.8, sy0, FACE + 0.35)
        pbr = (off * 0.9, bx, FACE + 1.15)
        p1 = (off * 0.55, sy1, 1.55)
        ml.bar(sg, p0, pbr, 0.10, 0.05, up=(0, 0, 1), section="rect")
        ml.bar(sg, pbr, p1, 0.10, 0.05, up=(0, 0, 1), section="rect")
    nut = w.add(m, "nut", "bone", base=(210, 200, 170))
    ml.loft(nut, "y", (0, ny1 - 0.2, 1.4), [0.0, 0.5], ml.rounded_rect(NECK_W[1] * K * 0.95, 0.6, 0.2, 2))
    # ---- 16 the pegbox and 17-20 four bone pegs
    pb0, pb1 = M(PEGBOX_X[0], YC)[1], M(PEGBOX_X[1], YC)[1]
    pgb = w.add(m, "pegbox", "wood", base=(50, 40, 36), smooth=35)
    ml.loft(pgb, "y", (0, 0, 0), [(pb0, 0.9, 0.9), ((pb0 + pb1) / 2, 1.0, 1.0), (pb1, 0.85, 0.85)], ml.rounded_rect(PEGBOX_W * K, 3.0, 0.7, 2))
    for i, (yy, sgn) in enumerate(((pb0 + 1.6, 1), (pb0 + 1.6, -1), (pb1 - 1.7, 1), (pb1 - 1.7, -1))):
        pg = w.add(m, f"peg_{i}", "bone", base=(236, 228, 204), smooth=60)
        ml.lathe(pg, "x", (0, yy, 0.2), [(sgn * 1.0, 0.45), (sgn * 2.4, 0.4), (sgn * 2.9, 0.75), (sgn * 3.5, 0.7), (sgn * 3.9, 0.0)], sides=8)
        ml.lathe(pg, "x", (0, yy, 0.2), [(sgn * 3.3, 0.0), (sgn * 3.7, 0.9), (sgn * 4.4, 0.6), (sgn * 4.9, 0.0)], sides=8)
    # ---- 21 the scroll: a crescent hook swept off the pegbox end
    sc = w.add(m, "scroll", "silver", base=(176, 182, 198), smooth=45)
    pts = [M(553, YC), M(566, YC - 8), M(580, YC - 20), M(597, YC - 26), M(609, YC - 18), M(612, YC - 6), M(604, YC + 3)]
    ml.sweep(sc, pts, ml.circle(1.0, 8), scales=lambda t: (1.15 - 0.9 * t, 1.0 - 0.8 * t))
    # ---- 22-24 the ghostly wisps rising off the sound holes (glow only look)
    for i, (hx, hy) in enumerate(((300, HOLE_UP), (330, HOLE_LO), (260, YC))):
        ws = w.add(m, f"wisp_{i}", "crystal", base=(170, 214, 244), glow=True, smooth=60, uv_mode="loft")
        pts = [M(hx + 36 * t, hy + 16 * math.sin(t * 1.8 + i) * (1 if i != 1 else -1), FACE + 0.5 + 1.2 * t) for t in np.linspace(0, 3.0, 14)]
        ml.sweep(ws, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.3 * math.sin(math.pi * t) + 0.05, 0.1))
    # ---- 25-28 the bow beside it: a curved stick, its hair, the frog and the tip
    BX0, BY0, BX1, BY1 = 232.0, 358.0, 575.0, 284.0
    stick = [M(BX0 + (BX1 - BX0) * t, BY0 + (BY1 - BY0) * t + 14.0 * math.sin(math.pi * t) * 0.9 + 6 * t, -1.0) for t in np.linspace(0, 1, 18)]
    bw = w.add(m, "bow_stick", "wood", base=(60, 46, 40), smooth=45)
    ml.sweep(bw, stick, ml.circle(0.32, 8), scales=lambda t: (1.0 - 0.25 * t, 1.0 - 0.25 * t))
    hair = [M(BX0 + 20 + (BX1 - BX0 - 38) * t, BY0 - 6 + (BY1 - BY0) * t + 4.0 * math.sin(math.pi * t) * 0.3, -1.0) for t in np.linspace(0, 1, 12)]
    bh = w.add(m, "bow_hair", "bone", base=(232, 226, 206), smooth=40, wear=0.2)
    ml.sweep(bh, hair, ml.rect_ring(1.0, 1.0), scales=lambda t: (0.28, 0.1))
    fr = w.add(m, "bow_frog", "obsidian", base=(36, 32, 36), smooth=45)
    fx, fy, _ = stick[0]
    ml.loft(fr, "y", (fx, fy - 0.5, -1.0), [0.0, 4.2], ml.rounded_rect(1.5, 1.5, 0.4, 2))
    ft = w.add(m, "bow_tip", "bone", base=(226, 220, 200), smooth=50)
    tx, ty, _ = stick[-1]
    ml.lathe(ft, "y", (tx, ty, -1.0), [(-1.0, 0.3), (0.0, 0.5), (0.9, 0.0)], sides=8)

    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    glowc = lambda rgb, s, core=0.3: (lambda c: w.set_glow(c, rgb, s, core=core))
    for nm in ("hole_upper", "hole_lower"):
        w.paint_part(atlas, m.part(nm), glowc((140, 220, 255), 1.0, 0.5))
    for i in range(3):
        w.paint_part(atlas, m.part(f"wisp_{i}"), glowc((150, 205, 255), 0.55, 0.2))
    w.paint_part(atlas, m.part("face_border"), glowc((150, 200, 255), 0.12, 0.0))
    w.paint_part(atlas, m.part("corner_swirls"), glowc((150, 200, 255), 0.10, 0.0))
    ref = w.reference("23cab649685c43cc51bb623d36fd07aa.jpg", (130, 100, 640, 380), "viola_of_moonlit_spirits")
    w.finish("viola_of_moonlit_spirits", m, atlas, real_m=TOTAL_M, fit=True, glow=("FFFFFF", 0.14, 0.55, 1.0), ref=ref)

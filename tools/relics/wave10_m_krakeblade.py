"""Krakeblade (card: "Krakeblade", weapon (greatsword), very rare, requires attunement): a massive cleaver-like slab of barnacle- and coral-crusted
stone, a brown octopus tentacle (with suckers) coiling round the blade's root and lying along its face, a long thin wrapped grip that ends in a
tentacle tip. The 3D model, built with the owner's method.

Reference: the card, 391 x 456: the sword runs from the pommel (377, 122) to the point (8, 440), 487 px. s = card px from the pommel along it.
Axis: y up from the pommel to the point; the broad face looks toward +z; the edge is on +x, the straight spine on -x.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card

# ---------------------------------------------------------------- 1 measure
TOTAL_CM = 160.0                                     # a greatsword: 1.6 m (the card calls it massive)
S_TIP = 487.0                                        # card px from the pommel to the point
K = TOTAL_CM / S_TIP                                 # cm per card px (0.3285)
S_GRIP_END = 138.0                                   # the grip meets the tentacle coil and the blade at card (270, 215): 138 px from the pommel
Y_GRIP0, Y_GRIP1 = 2.5, S_GRIP_END * K               # the grip: 2.5 cm above the pommel to 45.3 cm
R_GRIP = 1.45                                        # the grip is long and thin: about 9 px wide on the card
Y_BLADE0 = Y_GRIP1                                   # the slab starts at the guard
# blade half width (cm) along y: narrow at the root, broad slab, a cut tip (read off the card: 55 px at the widest = 18 cm, drawn at 85 percent)
HALF_W = w.hermite([(Y_BLADE0, 3.6), (Y_BLADE0 + 6, 6.6), (Y_BLADE0 + 26, 8.8), (Y_BLADE0 + 56, 9.6), (Y_BLADE0 + 84, 9.0), (Y_BLADE0 + 100, 7.4), (Y_BLADE0 + 109, 4.2), (TOTAL_CM - 0.5, 0.4)])
HALF_T = 1.25                                        # half thickness of the slab (2.5 cm: a thick cleaver)
TIP_SHIFT = 3.6                                      # the point sits towards the edge side (the card's tip is cut on the diagonal)
TENT_R0, TENT_R1 = 1.7, 0.45                         # the tentacle's radius at the root and at its tip
N_BARNACLES = 26                                     # the card shows a crust of small round growths
SPIKES = [(Y_BLADE0 + 22, 1.9), (Y_BLADE0 + 40, 2.3), (Y_BLADE0 + 58, 2.0), (Y_BLADE0 + 76, 2.4), (Y_BLADE0 + 92, 1.7)]   # coral spikes on the spine: (y, length)


def wedge():
    """A cleaver section: one ground edge on +u, a thick spine on -u (unit half width and half thickness; 7 points, counter-clockwise)."""
    return [(1.0, 0.0), (0.55, 0.5), (-0.88, 0.5), (-1.0, 0.3), (-1.0, -0.3), (-0.88, -0.5), (0.55, -0.5)]


def build(charged=False):
    m = ml.Mesh("krakeblade")
    rng = np.random.default_rng(10)
    yb0 = Y_BLADE0
    # ---- 1 the slab: a loft of the wedge section; the width profile is the card's silhouette, the point shifts toward the edge
    bl = w.add(m, "blade", "stone", base=(96, 126, 96), smooth=38, uv_mode="loft")
    ys = [yb0 - 1.0, yb0 + 4, yb0 + 12, yb0 + 28, yb0 + 56, yb0 + 84, yb0 + 100, yb0 + 108, yb0 + 112, TOTAL_CM - 3.0, TOTAL_CM]
    st = []
    for yy in ys:
        hw = HALF_W(min(yy, TOTAL_CM - 0.5))
        tt = HALF_T * (1.0 if yy < yb0 + 100 else max(0.35, 1.0 - (yy - yb0 - 100) / 40.0))
        shift = TIP_SHIFT * max(0.0, (yy - (yb0 + 96)) / (TOTAL_CM - yb0 - 96)) ** 1.5
        if yy >= TOTAL_CM - 0.01:
            st.append((yy, 0.0, 0.0, shift, 0.0))
        else:
            st.append((yy, hw, tt, shift, 0.0))
    ml.loft(bl, "y", (0, 0, 0), st, wedge())
    # ---- 2 the raised spine ridge: a bead along the straight back of the slab
    sp = w.add(m, "spine_ridge", "stone", base=(70, 86, 74), wear=0.8, smooth=50)
    path = [(-HALF_W(yy) + 0.5, yy, 0.0) for yy in np.linspace(yb0 + 5, yb0 + 108, 9)]
    ml.sweep(sp, path, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (0.7 * math.sin(math.pi * min(1.0, t * 1.0)) ** 0.5 + 0.1, 1.5 * (0.6 + 0.4 * math.sin(math.pi * t))), up=(0, 0, 1))
    # ---- 3 the barnacle crust: small domed growths on both faces
    bc = w.add(m, "barnacles", "bone", base=(196, 198, 176), wear=0.9, smooth=60)
    for i in range(N_BARNACLES):
        yy = yb0 + 6 + rng.random() * 100
        xx = (rng.random() * 2 - 1) * HALF_W(yy) * 0.8
        face = 1 if i % 2 == 0 else -1
        r = 0.38 + rng.random() * 0.42
        ml.stud(bc, (xx, yy, face * HALF_T * 0.5 * (1.0 if abs(xx) < HALF_W(yy) * 0.5 else 0.96)), r, r * 0.8, (0, 0, face), 5, 0.5)
    # ---- 4 the coral spikes along the spine
    cs = w.add(m, "coral_spikes", "stone", base=(190, 120, 110), wear=0.7, smooth=55)
    for (yy, ln) in SPIKES:
        ml.lathe(cs, (-0.55, 0.85, 0.15), (-HALF_W(yy) + 0.2, yy, 0.0), [(-0.3, 0.55), (0.0, 0.7), (ln * 0.55, 0.38), (ln, 0.0)], sides=6)
    # ---- 5 the tentacle coil round the root of the blade (2.2 turns), 6 its suckers
    tc = w.add(m, "tentacle_coil", "leather", base=(128, 82, 58), smooth=70, uv_mode="loft")
    suck = w.add(m, "suckers", "bone", base=(236, 214, 196), wear=0.2, smooth=40)
    cpts, spots = [], []
    n = 30
    y_a, y_b = yb0 - 5.0, yb0 + 34.0
    for i in range(n + 1):
        t = i / n
        a = 2 * math.pi * 2.2 * t + 0.6
        yy = y_a + (y_b - y_a) * t
        rx = HALF_W(yy) + 1.5 + 1.2 * (1 - t)
        rz = HALF_T + 1.5 + 0.8 * (1 - t)
        cpts.append((rx * math.cos(a), yy, rz * math.sin(a)))
        if i % 4 == 1 and i > 2:
            spots.append((cpts[-1], (math.cos(a), 0.0, math.sin(a) * 0.9)))
    rad = lambda t: TENT_R0 * (1.0 - 0.45 * t) if t < 0.97 else 0.0
    ml.sweep(tc, cpts, ml.circle(1.0, 6), scales=lambda t: (rad(t), rad(t)))
    for (p, nrm) in spots:
        ml.stud(suck, p, 0.62, 0.38, nrm, 5, 0.5)
    # ---- 7 the loose end of the tentacle lying down the face
    tt_ = w.add(m, "tentacle_tail", "leather", base=(138, 90, 62), smooth=70, uv_mode="loft")
    tpts = []
    for i in range(0, 21):
        t = i / 20
        yy = y_b + t * 52
        tpts.append((-HALF_W(yy) * 0.45 + 2.4 * math.sin(t * 5.2), yy, HALF_T + 0.8 + 0.5 * math.sin(t * 3.0)))
    ml.sweep(tt_, tpts, ml.circle(1.0, 6), scales=lambda t: (1.35 * (1 - t) ** 0.8 + 0.12 if t < 0.97 else 0.0,) * 2)
    for i in range(4, 20, 5):
        t = i / 20
        r = 1.35 * (1 - t) ** 0.8 + 0.12
        p = tpts[i]
        ml.stud(suck, (p[0], p[1], p[2] + r * 0.6), r * 0.42, r * 0.3, (0, 0, 1), 5, 0.5)
    # ---- 8 the guard's collar, 9 the short bar of two small tentacle hooks
    gc = w.add(m, "guard_collar", "bronze", base=(104, 76, 52), smooth=50)
    ml.lathe(gc, "y", (0, 0, 0), [(Y_GRIP1 - 1.8, R_GRIP * 1.1), (Y_GRIP1 - 1.1, R_GRIP * 1.9), (Y_GRIP1 + 0.2, R_GRIP * 2.0), (Y_GRIP1 + 1.4, R_GRIP * 1.2)], sides=12, squash=0.8)
    gb = w.add(m, "guard_bar", "bone", base=(214, 196, 168), smooth=55)
    for sgn in (-1, 1):
        ml.sweep(gb, [(sgn * 1.0, Y_GRIP1 - 0.4, 0.0), (sgn * 3.4, Y_GRIP1 - 0.6, 0.2), (sgn * 5.6, Y_GRIP1 + 0.6, 0.3), (sgn * 6.8, Y_GRIP1 + 2.6, 0.2)], ml.circle(1.0, 8),
                 scales=lambda t: (0.85 * (1 - t) ** 0.6 + 0.1 if t < 0.97 else 0.0,) * 2)
    # ---- 10 the long thin grip, 11 its wrap rings
    gp = w.add(m, "grip", "leather", base=(104, 66, 46), uv_mode="loft", smooth=50)
    ml.loft(gp, "y", (0, 0, 0), [(Y_GRIP0, 0.94, 0.94), (Y_GRIP0 + 10, 1.0, 1.0), ((Y_GRIP0 + Y_GRIP1) / 2, 1.04, 1.04), (Y_GRIP1 - 6, 1.0, 1.0), (Y_GRIP1 - 1.5, 1.08, 1.08)], ml.circle(R_GRIP, 12))
    gw = w.add(m, "grip_wraps", "leather", base=(70, 44, 30), wear=0.3, smooth=60)
    for i in range(9):
        ml.collar(gw, Y_GRIP0 + 3.0 + (Y_GRIP1 - Y_GRIP0 - 7.0) * i / 8, R_GRIP * 1.07, 0.7, lip=0.2, sides=10, caps=(False, False))
    # ---- 12 the pommel: the end of the grip becomes a curled tentacle tip, 13 a small cap under it
    pc = w.add(m, "pommel_cap", "bronze", base=(104, 76, 52), smooth=55)
    ml.lathe(pc, "y", (0, 0, 0), [(Y_GRIP0 - 0.2, R_GRIP * 1.15), (Y_GRIP0 - 0.9, R_GRIP * 1.7), (Y_GRIP0 - 1.9, R_GRIP * 1.5), (Y_GRIP0 - 2.6, R_GRIP * 0.8)], sides=12)
    pt = w.add(m, "pommel_tentacle", "leather", base=(134, 88, 60), smooth=70, uv_mode="loft")
    ppts = [(0.0, Y_GRIP0 - 2.2, 0.0), (0.3, Y_GRIP0 - 4.4, 0.4), (1.5, Y_GRIP0 - 6.4, 0.9), (3.2, Y_GRIP0 - 7.2, 0.6), (4.4, Y_GRIP0 - 6.0, 0.0), (4.2, Y_GRIP0 - 4.4, -0.5)]
    ml.sweep(pt, ppts, ml.circle(1.0, 8), scales=lambda t: (1.25 * (1 - 0.8 * t) if t < 0.97 else 0.0,) * 2)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p3/Relics of the world part 3/7484b79295a6807822dbeb19643dd313.jpg", (0, 100, 391, 456), "krakeblade")
    w.finish("krakeblade", m, atlas, real_m=TOTAL_CM / 100.0, glow=("FFFFFF", 0.12, 0.35, 1.0), ref=ref)


def _paint(m, atlas):
    def crust(c):
        sh = c.img.shape[:2]
        n1, n2, n3 = w.noise2(sh, 22, 1), w.noise2(sh, 7, 2), w.noise2(sh, 2.5, 3)
        moss, grey, teal = np.array([84, 126, 80], np.float32), np.array([128, 134, 128], np.float32), np.array([64, 124, 118], np.float32)
        t1 = np.clip((n1 - 0.4) * 2.2, 0, 1)[..., None]
        col = grey * (1 - t1) + moss * t1
        t2 = np.clip((n2 - 0.6) * 2.0, 0, 1)[..., None] * 0.55
        col = col * (1 - t2) + teal * t2
        col = col * (0.86 + 0.28 * n3[..., None])
        holes = (w.noise2(sh, 4, 7) > 0.9)[..., None]
        col = np.where(holes, np.array([30, 42, 38], np.float32), col)
        c.img[..., :3] = np.clip(col, 0, 255)
        glowmask = (n2 > 0.7) & (n1 > 0.5)
        w.set_glow(c, (60, 230, 205), 0.30, mask=glowmask.astype(np.float32), core=0.2)
    w.paint_part(atlas, m.part("blade"), crust)
    for nm in ("tentacle_coil", "tentacle_tail", "pommel_tentacle"):
        def skin(c):
            sh = c.img.shape[:2]
            n = w.noise2(sh, 5, 11)
            c.img[..., :3] = np.clip(c.img[..., :3] * (0.8 + 0.5 * n[..., None]) + np.array([14, 4, 0]), 0, 255)
            ring = (np.sin(c.S * 3.14159 * 6) > 0.92)
            w.set_glow(c, (80, 230, 210), 0.12, mask=ring.astype(np.float32) * 0.5, core=0.0)
        w.paint_part(atlas, m.part(nm), skin)
    w.paint_part(atlas, m.part("suckers"), lambda c: w.set_glow(c, (110, 255, 230), 0.55, core=0.3))

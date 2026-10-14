"""Jade Xiphos (card: "Jade Xiphos", uncommon shortsword, martial, finesse, light): a short leaf-shaped blade of translucent bright green jade-glass with a
central ridge, a gold crossguard with small curled ends, a gold wrapped grip and a round gold pommel with a green stone. The 3D model, built with the
owner's method.

Reference: the card, 320 x 320; the sword runs left to right from the pommel (20, 117) to the point (302, 126): 282 px; a xiphos with this hilt is
66 cm, so K = 0.234 cm per px. s = px from the pommel. Axis: y up from the pommel; the broad face looks toward +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w
from wave10_models import card, tube, glow_all, wrapped_grip

# ---------------------------------------------------------------- 1 measure
TOTAL = 66.0
S_TOT = 282.0
K = TOTAL / S_TOT


def Y(s):
    return s * K


Y_POMMEL = Y(30)                                       # the round pommel, card s 0..30 (a ball about 28 px across)
Y_GRIP0, Y_GRIP1 = Y(28), Y(66)                        # the gold-wrapped grip, card s 28..66
Y_GUARD = Y(72)                                        # the crossguard, card s 72 (50 px across: 11.7 cm)
GUARD_HALF = 5.9
Y_BLADE0 = Y(80)                                       # the leaf blade starts at the guard
BLADE_W, BLADE_T = 7.4, 0.9                            # the blade is 32 px wide at its widest (7.4 cm), thin
R_GRIP = 1.15
WIDEST = 0.34                                          # the blade is widest a third of the way along
LEAF = w.hermite([(0.0, 0.92), (0.1, 0.99), (WIDEST, 1.0), (0.7, 1.0), (0.86, 0.97), (0.93, 0.62), (0.985, 0.2), (1.0, 0.0)])   # the card's blade: parallel sided, then a short pointed end


def build():
    m = ml.Mesh("jade_xiphos")
    # ---- 1 the jade blade: a lozenge section whose width follows a leaf outline
    bl = w.add(m, "blade", "crystal", base=(60, 214, 110), smooth=30, uv_mode="loft", glow=True)
    L = TOTAL - Y_BLADE0
    st = []
    for i in range(0, 15):
        t = i / 14
        s = LEAF(t)
        st.append((Y_BLADE0 + L * t - (0.6 if i == 0 else 0.0), s if t < 1.0 else 0.0, (0.55 + 0.45 * s) if t < 1.0 else 0.0))
    ml.loft(bl, "y", (0, 0, 0), st, ml.lozenge(BLADE_W, BLADE_T, 0.2))
    # ---- 2 the central ridge on each face (two raised ridges framing the groove)
    mr = w.add(m, "midrib", "crystal", base=(150, 255, 190), smooth=30, glow=True)
    for face in (1, -1):
        ml.fuller(mr, (0.0, Y_BLADE0 + 1.0, face * BLADE_T * 0.44), (0.0, TOTAL - 5.0, face * BLADE_T * 0.2), (0, 0, face), 1.2, 0.3, 0.12, taper=5.0)
    # ---- 3 the gold crossguard (a flat bar with a swell in the middle), 4 its small curled ends
    gd = w.add(m, "guard", "gold", base=(228, 184, 76), smooth=45)
    ml.loft(gd, "x", (0, Y_GUARD, 0), [(-GUARD_HALF, 1.4, 0.9), (-GUARD_HALF + 1.2, 2.0, 1.1), (-2.5, 1.2, 1.0), (0.0, 1.8, 1.4), (2.5, 1.2, 1.0), (GUARD_HALF - 1.2, 2.0, 1.1), (GUARD_HALF, 1.4, 0.9)], ml.octagon(1.0, 1.0, 0.3))
    gc = w.add(m, "guard_curls", "gold", base=(240, 200, 96), smooth=60)
    for sgn in (-1, 1):                                                   # a small curl: out from the bar's end, up and back in
        pts = [(sgn * (GUARD_HALF - 0.3 + 1.3 * math.sin(0.5 * i)), Y_GUARD + 1.1 * (1 - math.cos(0.6 * i)), 0.0) for i in range(0, 9)]
        ml.sweep(gc, pts, ml.circle(1.0, 6), scales=lambda t: (0.5 * (1 - 0.45 * t),) * 2)
    # ---- 5 a collar between guard and blade, 6 the gold wrapped grip with its wire (7), 8 the round pommel, 9 the green stone in it
    co = w.add(m, "collar", "gold", base=(214, 170, 64), smooth=45)
    ml.lathe(co, "y", (0, 0, 0), [(Y_GUARD + 0.8, R_GRIP * 1.1), (Y_GUARD + 1.3, R_GRIP * 1.8), (Y_BLADE0 + 0.5, R_GRIP * 1.5), (Y_BLADE0 + 1.1, R_GRIP * 0.6)], sides=10, squash=0.5)
    wrapped_grip(m, Y_GRIP0, Y_GRIP1, R_GRIP, (200, 156, 60), (244, 206, 100), 8, material="gold", wrap_material="gold", name="grip", swell=1.07)
    pm = w.add(m, "pommel", "gold", base=(232, 188, 80), smooth=55)
    w.ellipsoid(pm, (0.0, Y(11), 0.0), 2.2, 2.3, 2.2, sides=12, stacks=8)
    ps = w.add(m, "pommel_stone", "crystal", base=(80, 230, 130), glow=True, smooth=0)
    ml.gem(ps, (0.0, Y(11), 2.0), 0.9, long=1.2, sides=6, axis="z")

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    ref = card("p2/Relics of the world part 2/0dfd3687f89e9c91ad54e2879b6a5d31.jpg", (0, 70, 320, 180), "jade_xiphos")
    w.finish("jade_xiphos", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.16, 0.3, 0.9), ref=ref)


def _paint(m, atlas):
    def jade(c):
        sh = c.img.shape[:2]
        n = w.noise2(sh, 14, 3)
        edge = np.clip((np.abs(c.S - 0.5) - 0.3) * 4, 0, 1)
        col = np.array([54, 206, 104], np.float32) * (0.85 + 0.35 * n[..., None])
        col = col * (1 - 0.3 * edge[..., None]) + np.array([210, 255, 224], np.float32) * (0.12 * edge[..., None])
        c.img[..., :3] = np.clip(col, 0, 255)
        w.set_glow(c, (90, 255, 150), 0.18, mask=(0.4 + 0.6 * n), core=0.2)
    w.paint_part(atlas, m.part("blade"), jade)
    glow_all(atlas, m.part("midrib"), (150, 255, 190), 0.5, 0.3)
    glow_all(atlas, m.part("pommel_stone"), (100, 255, 150), 0.9, 0.4)

"""Contingency Plan (card by milonaschronicles, "Weapon (dagger), very rare (requires attunement)"): an ornate dagger with a long narrow copper-red blade shading to
purple-black along its ridge, a winged crossguard of pinkish metal swept back toward the grip with a glowing pale-blue orb at its centre, a twisted red-gold grip and a
clawed pommel.

Reference: the card, 736 x 736. The dagger runs from the point (170, 62) to the claw pommel's far edge (588, 282): 472 px on the diagonal.
s = card px from the pommel. Axis: y up from the pommel to the point; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 0.46                                       # a long dagger in hand
S_TIP = 472.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.097)
S_POMMEL = (0.0, 34.0)                               # the clawed pommel
S_GRIP = (34.0, 128.0)                               # the twisted grip, 22 px thick
R_GRIP = 11.0 * K * 1.0
S_GUARD = 160.0                                      # the orb's centre (card (435, 198))
WING_HALF = 62.0 * K                                 # the wings span about 124 px
S_BLADE0 = 176.0
BLADE_W0 = 40.0 * K                                  # 40 px across at the guard (the card draws a broad dagger)
BLADE_T0 = 0.85


def y(s):
    return s * K


def build():
    m = ml.Mesh("contingency_plan")
    bl = w.add(m, "blade", "bronze", base=(176, 84, 66), smooth=40, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE0) - 0.5, 1.0, 1.0), (y(S_BLADE0 + 100), 0.82, 0.92), (y(S_TIP - 80), 0.5, 0.75), (y(S_TIP - 14), 0.18, 0.4), (y(S_TIP), 0.0, 0.0)], ml.lozenge(BLADE_W0, BLADE_T0, 0.1))
    cr = w.add(m, "blade_ridge", "obsidian", base=(58, 30, 82), smooth=45)
    ml.loft(cr, "y", (0, 0, 0), [(y(S_BLADE0 + 4), 0.0, 0.0), (y(S_BLADE0 + 16), 0.42, 1.0), (y(S_TIP - 120), 0.30, 1.0), (y(S_TIP - 22), 0.0, 0.0)], ml.lozenge(BLADE_W0, BLADE_T0 * 1.25, 0.2))
    ed = w.add(m, "blade_edges", "steel", base=(236, 140, 110), wear=0.6, smooth=45)
    for sgn in (-1, 1):
        ml.bar(ed, (sgn * BLADE_W0 * 0.46, y(S_BLADE0 + 6), 0.0), (sgn * BLADE_W0 * 0.14, y(S_TIP - 40), 0.0), 0.16, 0.12, up=(0, 0, 1), taper=3.0)
    # ---- the guard: two wings swept back toward the grip with curled tips, a hub ring round the orb, the orb
    gw = w.add(m, "guard_wings", "silver", base=(222, 160, 196), smooth=45)
    for sgn in (-1, 1):
        path = [(sgn * WING_HALF * t, y(S_GUARD) - 5.5 * t ** 1.5 + 1.8 * max(0.0, t - 0.7) ** 1.3 * 6.0, 0.0) for t in np.linspace(0.12, 1.0, 11)]
        ml.sweep(gw, path, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (1.9 * (1.0 - 0.75 * t) if t < 0.98 else 0.1, 0.75 * (1.0 - 0.5 * t)), up=(0, 0, 1))
        fin = [(sgn * WING_HALF * 0.55 * t, y(S_GUARD) + 3.2 * t ** 0.8, 0.0) for t in np.linspace(0.1, 1.0, 7)]       # the small forward fin of each wing
        ml.sweep(gw, fin, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (1.1 * (1.0 - 0.8 * t) if t < 0.98 else 0.05, 0.6 * (1.0 - 0.4 * t)), up=(0, 0, 1))
    gh = w.add(m, "guard_hub", "silver", base=(196, 134, 176), smooth=50)
    ml.lathe(gh, "z", (0, y(S_GUARD), -0.4), [(0.0, 1.9), (0.5, 2.3), (1.0, 2.1), (1.4, 1.7)], sides=14)
    orb = w.add(m, "orb", "crystal", base=(150, 236, 255), glow=True, smooth=60)
    w.ellipsoid(orb, (0, y(S_GUARD), 0.5), 1.5, 1.5, 1.2, sides=14, stacks=10)
    gk = w.add(m, "guard_collar", "silver", base=(180, 120, 160), smooth=45)
    ml.lathe(gk, "y", (0, 0, 0), [(y(S_GUARD) - 2.4, R_GRIP * 1.0), (y(S_GUARD) - 1.6, R_GRIP * 1.5), (y(S_GUARD) + 0.4, R_GRIP * 1.3), (y(S_BLADE0) + 0.4, 0.6)], sides=10, squash=0.8)
    # ---- the grip: red core with a gold wire twisted round it
    gp = w.add(m, "grip", "leather", base=(150, 50, 44), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.9, 0.9), (y(80), 1.1, 1.1), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    gwr = w.add(m, "grip_wire", "gold", base=(228, 160, 74), smooth=60, wear=0.4)
    for ph in (0.0, math.pi):
        pts = w.helix(lambda t: R_GRIP * 1.06, y(S_GRIP[0]) + 0.2, y(S_GRIP[1]) - 0.2, 3.5, 56, phase=ph)
        ml.sweep(gwr, pts, ml.circle(0.2, 6), scales=lambda t: (1.0, 1.0))
    # ---- the pommel: a ball with three claws curling round it
    pb = w.add(m, "pommel_claws", "gold", base=(206, 130, 66), smooth=45)
    w.ellipsoid(pb, (0, y(20.0), 0.0), 1.3, 1.5, 1.3, sides=12, stacks=8)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.5
        pts = [(math.cos(a) * (1.1 + 1.4 * math.sin(math.pi * 0.7 * t)), y(26.0) - 3.8 * t ** 0.9 + 0.5, math.sin(a) * (1.1 + 1.4 * math.sin(math.pi * 0.7 * t))) for t in np.linspace(0.0, 1.0, 9)]
        ml.sweep(pb, pts, ml.circle(1.0, 6), scales=lambda t: (0.36 * (1.0 - 0.8 * t) if t < 0.98 else 0.03, 0.36 * (1.0 - 0.8 * t) if t < 0.98 else 0.03))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("orb"), lambda c: w.set_glow(c, (110, 215, 255), 0.9, core=0.12))
    w.paint_part(atlas, m.part("blade_ridge"), lambda c: w.set_glow(c, (170, 120, 255), 0.35, core=0.1))
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (255, 150, 110), 0.07, core=0.0))
    ref = w.reference("p3/Relics of the world part 3/c139ab132feb10988adbfffe193b7e29.jpg", (150, 40, 610, 300), "contingency_plan")
    w.finish("contingency_plan", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.18, 0.45, 1.0), ref=ref)

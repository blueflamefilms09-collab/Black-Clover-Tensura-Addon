"""Eschaton (card by Forged-Artifacts, "Weapon (rapier), legendary", requires attunement by a bard, sorcerer or wizard): a slender silvery-white rapier whose
blade is studded with cyan diamond crystals along its centre line, an ornate green leaf-like guard with a gem, a green ringed hilt and a spiked pommel gem.
(The card's second form, the segmented whip, has no separate mesh: the held model is always the rapier; the whip's extra reach is a game rule.)

Reference: the card, 735 x 675. The upright rapier: pommel tip at (430, 72), point at (424, 432): 360 px.
s = card px from the pommel. Axis: y up from the pommel to the point; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.15                                       # a rapier in hand
S_TIP = 360.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.32)
S_POMMEL = (0.0, 26.0)                               # the faceted gem pommel with its spike (card y 72..98)
S_GRIP = (26.0, 74.0)                                # the green hilt, three ring bands (card y 98..146)
R_GRIP = 1.05
S_GUARD = 80.0                                       # the guard's centre (card y 152)
GUARD_HALF = 24.0 * K                                # the leaf wings span about 48 px
S_BLADE0 = 92.0                                      # the blade begins under the guard (card y 164)
BLADE_W0, BLADE_W1 = 5.0, 3.5                        # the card draws a broad blade, 24 px wide at the guard (7.7 cm) and 17 px near the point: shown as 5 cm to 3.5 cm for a rapier-like hand
BLADE_T = 0.9
S_DIAMONDS = [(104.0 + 25.0 * i) for i in range(10)]  # a cyan diamond every 25 px along the centre (card y 176, 201 ...)


def y(s):
    return s * K


def build():
    m = ml.Mesh("eschaton")
    bl = w.add(m, "blade", "silver", base=(212, 224, 238), smooth=38, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE0) - 0.8, 1.0, 1.0), (y(S_BLADE0 + 100), 0.82, 0.95), (y(S_TIP - 70), BLADE_W1 / BLADE_W0, 0.85), (y(S_TIP - 14), 0.42, 0.5), (y(S_TIP), 0.0, 0.0)],
            ml.lozenge(BLADE_W0, BLADE_T, 0.25))
    dm = w.add(m, "blade_diamonds", "crystal", base=(90, 236, 226), glow=True, smooth=0)
    for i, s in enumerate(S_DIAMONDS):
        wd = 0.95 * (1.0 - 0.4 * i / len(S_DIAMONDS))
        ml.gem(dm, (0, y(s), BLADE_T * 0.5), wd, long=1.5, sides=4, axis="z")
        ml.gem(dm, (0, y(s), -BLADE_T * 0.5), wd, long=1.5, sides=4, axis="-z")
    nd = w.add(m, "blade_nodules", "silver", base=(188, 232, 238), smooth=0)       # the small leaf-shaped nodules on the card's edges at every other diamond
    for i, s in enumerate(S_DIAMONDS[::2]):
        for sgn in (-1, 1):
            xe = sgn * (BLADE_W0 * (0.5 - 0.1 * i / 5.0) + 0.05)
            ml.gem(nd, (xe, y(s + 12), 0.0), 0.45, long=1.6, sides=4, axis="y")
    rb = w.add(m, "blade_ridge", "silver", base=(240, 246, 252), wear=0.5)
    ml.bar(rb, (0, y(S_BLADE0 + 4), BLADE_T * 0.5), (0, y(S_TIP - 40), BLADE_T * 0.42), 0.16, 0.06, up=(0, 0, 1), taper=2.0)
    # ---- the guard: a green cross bar, two swept leaf wings curling toward the blade, two lower curls, a gem in the middle
    gc = w.add(m, "guard_bar", "bronze", base=(48, 138, 66), smooth=45)
    ml.loft(gc, "x", (0, y(S_GUARD), 0), [(-GUARD_HALF, 0.5, 0.5), (-GUARD_HALF * 0.6, 0.85, 0.85), (0, 1.0, 1.0), (GUARD_HALF * 0.6, 0.85, 0.85), (GUARD_HALF, 0.5, 0.5)], ml.octagon(1.6, 1.4, 0.4))
    for sgn, nm in ((-1, "guard_leaf_l"), (1, "guard_leaf_r")):
        lf = w.add(m, nm, "bronze", base=(62, 160, 80), smooth=40)
        path = [(sgn * (GUARD_HALF * (0.3 + 0.7 * t)), y(S_GUARD) + 4.2 * math.sin(math.pi * 0.5 * t) ** 1.4 + 0.6 * t, 0.0) for t in np.linspace(0, 1, 9)]
        ml.sweep(lf, path, ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (1.5 * (1.0 - 0.8 * t) if t < 0.98 else 0.1, 0.5 * (1.0 - 0.4 * t)), up=(0, 0, 1))
        low = [(sgn * (GUARD_HALF * 0.25 + 4.0 * t), y(S_GUARD) - 3.0 * math.sin(math.pi * 0.55 * t), 0.0) for t in np.linspace(0, 1, 7)]     # the lower curl of the same wing
        ml.sweep(lf, low, ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (1.1 * (1.0 - 0.8 * t) if t < 0.98 else 0.1, 0.5), up=(0, 0, 1))
    gg = w.add(m, "guard_gem", "crystal", base=(120, 255, 232), glow=True, smooth=0)
    ml.gem(gg, (0, y(S_GUARD), 0.85), 0.62, long=1.1, sides=6, axis="z")
    gk = w.add(m, "guard_collar", "silver", base=(206, 214, 226), smooth=45)
    ml.lathe(gk, "y", (0, 0, 0), [(y(S_GUARD) - 1.6, R_GRIP * 1.0), (y(S_GUARD) - 1.0, R_GRIP * 1.5), (y(S_GUARD) + 1.0, R_GRIP * 1.35), (y(S_BLADE0) + 0.4, 0.55)], sides=10, squash=0.7)
    # ---- the hilt: green leather with three silver ring bands, and the pommel gem with its spike
    gp = w.add(m, "grip", "leather", base=(40, 120, 58), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.95, 0.95), (y(S_GRIP[0] + 24), 1.1, 1.1), (y(S_GRIP[1]), 0.95, 0.95)], ml.circle(R_GRIP, 12))
    gb = w.add(m, "grip_bands", "silver", base=(206, 214, 226), wear=0.3)
    for s in (S_GRIP[0] + 2, S_GRIP[0] + 22, S_GRIP[1] - 4):
        ml.collar(gb, y(s), R_GRIP * 1.18, 0.7, lip=0.18, sides=12)
    pg = w.add(m, "pommel_gem", "crystal", base=(110, 255, 226), glow=True, smooth=0)
    ml.gem(pg, (0, y(S_POMMEL[0] + 14), 0.0), 0.95, long=1.2, sides=6, axis="y")
    ps = w.add(m, "pommel_spike", "silver", base=(206, 214, 226), smooth=50)
    ml.lathe(ps, "y", (0, 0, 0), [(y(S_POMMEL[0]) - 0.0, 0.0), (y(S_POMMEL[0]) + 0.6, 0.35), (y(S_POMMEL[0]) + 2.6, 0.6), (y(S_POMMEL[1]) - 0.2, 0.9), (y(S_POMMEL[1]), R_GRIP * 0.8)], sides=8)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (110, 255, 235), 0.10, core=0.0))
    for nm in ("blade_diamonds", "guard_gem", "pommel_gem"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (110, 255, 235), 1.0, core=0.5))
    w.paint_part(atlas, m.part("blade_ridge"), lambda c: w.set_glow(c, (130, 255, 240), 0.5, core=0.3))
    ref = w.reference("p3/Relics of the world part 3/5689cac65309acd1ca603a5af91dcec8.jpg", (385, 60, 460, 440), "eschaton")
    w.finish("eschaton", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.20, 0.5, 1.0), ref=ref)

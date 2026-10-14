"""Humbling Longsword (RPGtoons card, "Longsword, 1d8 slashing damage, versatile (1d10), requires attunement"): a plain steel longsword with a straight crossguard,
a fullered grey blade with three slanted wear marks, a blue wrapped grip and a round grey pommel. The 3D model, built with the owner's method.

Reference: the card, 736 x 748. The sword lies flat: the pommel's far edge at x = 88, the point at x = 652: 564 px.
s = card px from the pommel. Axis: y up from the pommel to the point; faces +z.
"""
import math

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.10
S_TIP = 564.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.195)
S_POMMEL = (0.0, 40.0)                               # the round pommel, about 40 px across (card x 88..128)
R_POMMEL = 19.0 * K
S_GRIP = (40.0, 138.0)                               # the blue grip (card x 128..226), 28 px thick
R_GRIP = 13.0 * K
S_GUARD = 146.0                                      # the guard bar (card x 224..240)
GUARD_HALF = 60.0 * K                                # the bar spans y 176 to 298: 122 px
GUARD_T = 17.0 * K                                   # 17 px deep along the blade
S_BLADE0 = 156.0
BLADE_W0, BLADE_W1 = 33.0 * K, 24.0 * K              # blade 33 px tall at the guard, 24 near the point
BLADE_T = 1.0


def y(s):
    return s * K


def build():
    m = ml.Mesh("humbling_longsword")
    bl = w.add(m, "blade", "steel", base=(168, 172, 182), smooth=38, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE0) - 0.8, 1.0, 1.0), (y(S_BLADE0 + 200), 0.9, 0.95), (y(S_TIP - 120), BLADE_W1 / BLADE_W0, 0.9), (y(S_TIP - 24), 0.45, 0.5), (y(S_TIP), 0.0, 0.0)],
            ml.lozenge(BLADE_W0, BLADE_T, 0.2))
    for face, nm in ((1, "fuller_f"), (-1, "fuller_b")):
        f = w.add(m, nm, "silver", base=(200, 204, 214), wear=0.7)
        ml.fuller(f, (0, y(S_BLADE0 + 6), face * BLADE_T * 0.46), (0, y(S_TIP - 80), face * BLADE_T * 0.38), (0, 0, face), 0.85, 0.14, 0.06, taper=4.0)
    nk = w.add(m, "wear_marks", "steel", base=(120, 124, 136), wear=0.3)       # the three slanted marks of the card at card x 380, 410, 460: slanted grooves as raised ticks
    for s0 in (230.0, 262.0, 312.0):
        ml.bar(nk, (-BLADE_W0 * 0.28, y(s0), BLADE_T * 0.5), (BLADE_W0 * 0.28, y(s0 + 22), BLADE_T * 0.5), 0.26, 0.08, up=(0, 0, 1), taper=0.6)
    gb = w.add(m, "guard_bar", "iron", base=(120, 124, 134), smooth=45)
    ml.loft(gb, "x", (0, y(S_GUARD), 0), [(-GUARD_HALF, 0.8, 0.8), (-GUARD_HALF + 0.9, 1.0, 1.0), (-2.0, 1.0, 1.0), (2.0, 1.0, 1.0), (GUARD_HALF - 0.9, 1.0, 1.0), (GUARD_HALF, 0.8, 0.8)],
            ml.octagon(GUARD_T, 1.7, 0.5))
    for sgn, nm in ((-1, "guard_cap_l"), (1, "guard_cap_r")):
        c = w.add(m, nm, "iron", base=(140, 144, 156), smooth=50)
        ml.lathe(c, "x", (sgn * GUARD_HALF, y(S_GUARD), 0), [(0.0, 0.5), (sgn * 0.4, 0.95), (sgn * 1.0, 0.7), (sgn * 1.5, 0.0)], sides=8, squash=1.0)
    co = w.add(m, "guard_collar", "iron", base=(130, 134, 146), smooth=45)
    ml.lathe(co, "y", (0, 0, 0), [(y(S_GRIP[1]) - 0.2, R_GRIP * 0.9), (y(S_GRIP[1]) + 0.3, R_GRIP * 1.35), (y(S_GUARD) - GUARD_T * 0.5, R_GRIP * 1.25), (y(S_GUARD) + GUARD_T * 0.5, R_GRIP * 1.0), (y(S_BLADE0), R_GRIP * 0.55)], sides=10, squash=0.7)
    gp = w.add(m, "grip", "leather", base=(32, 120, 214), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.95, 0.95), (y(S_GRIP[0] + 40), 1.05, 1.05), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    wr = w.add(m, "grip_wraps", "leather", base=(18, 80, 160), smooth=60, wear=0.3)
    for i in range(8):
        ml.collar(wr, y(S_GRIP[0] + 6 + i * 11.5), R_GRIP * 1.1, 0.5, lip=0.14, sides=12)
    pm = w.add(m, "pommel", "iron", base=(150, 154, 166), smooth=60)
    w.ellipsoid(pm, (0, y(20.0), 0), R_POMMEL, R_POMMEL * 1.05, R_POMMEL, sides=14, stacks=10)
    pb = w.add(m, "pommel_button", "iron", base=(170, 174, 186))
    ml.lathe(pb, "y", (0, 0, 0), [(y(S_POMMEL[1]) - 0.6, R_GRIP * 0.8), (y(S_POMMEL[1]) + 0.1, R_GRIP * 1.15), (y(S_POMMEL[1]) + 0.9, R_GRIP * 1.0)], sides=10)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (200, 225, 255), 0.06, core=0.0))
    for nm in ("fuller_f", "fuller_b"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (200, 225, 255), 0.25, core=0.2))
    ref = w.reference("p3/Relics of the world part 3/ef6022416fb132785275222ce70a3bac.jpg", (60, 160, 700, 320), "humbling_longsword")
    w.finish("humbling_longsword", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.10, 0.5, 1.0), ref=ref)

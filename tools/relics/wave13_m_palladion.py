"""Palladion (card sheet part 3, "Weapon (Longsword), Legendary", requires attunement by a creature of good alignment): a long silver-steel blade with a winged
crossguard (a grey feathered wing on the left, a gold bat-like wing on the right, a crescent at each tip), a small angelic figure at the guard's centre, a
navy grip wrapped in gold and a winged pommel.

Reference: the card, 736 x 1019. The sword stands upright at x = 545: the pommel's wing tips at y = 150, the point at y = 925: 775 px.
s = card px from the pommel. Axis: y up from the pommel to the point; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.20                                       # a longsword in hand
S_TIP = 775.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.155)
S_POMMEL = (0.0, 40.0)                               # the winged pommel (card y 150 to 190)
S_GRIP = (40.0, 205.0)                               # the grip: card y 190 to 355, about 30 px wide
R_GRIP = 15.0 * K * 0.95
S_GUARD = 212.0                                      # the guard's centre bar (card y 362)
WING_SPAN = 158.0 * K                                # the wings reach x = 392 and 708 round the blade at 545: half span 158 px
WING_H0, WING_H1 = 56.0 * K, 24.0 * K                # a wing is about 56 px tall at the root with its feathers, 24 near the tip
S_BLADE0 = 240.0                                     # the blade starts at card y 385
BLADE_W0, BLADE_W1 = 40.0 * K, 30.0 * K              # card x 525..565 at the guard, 530..560 near the end
BLADE_T = 1.1


def y(s):
    return s * K


def build():
    m = ml.Mesh("palladion")
    # ---- 1 the blade with its two-stage taper, 2/3 the fuller ridges
    bl = w.add(m, "blade", "steel", base=(176, 184, 208), smooth=38, uv_mode="loft")
    ml.loft(bl, "y", (0, 0, 0), [(y(S_BLADE0) - 0.8, 1.0, 1.0), (y(S_BLADE0 + 250), 0.92, 0.96), (y(S_TIP - 90), BLADE_W1 / BLADE_W0 * 0.95, 0.9), (y(S_TIP - 22), 0.45, 0.5), (y(S_TIP), 0.0, 0.0)],
            ml.lozenge(BLADE_W0, BLADE_T, 0.18))
    for face, nm in ((1, "fuller_f"), (-1, "fuller_b")):
        f = w.add(m, nm, "silver", base=(210, 216, 236), wear=0.6)
        ml.fuller(f, (0, y(S_BLADE0 + 8), face * BLADE_T * 0.46), (0, y(S_TIP - 120), face * BLADE_T * 0.38), (0, 0, face), 0.9, 0.14, 0.06, taper=4.0)
    # ---- 4 the guard's bar (navy) with its gold-edged centre block
    gb = w.add(m, "guard_bar", "iron", base=(30, 40, 92), smooth=45)
    ml.loft(gb, "x", (0, y(S_GUARD), 0), [(-WING_SPAN * 0.78, 0.7, 0.7), (-WING_SPAN * 0.3, 1.0, 1.0), (0, 1.15, 1.15), (WING_SPAN * 0.3, 1.0, 1.0), (WING_SPAN * 0.78, 0.7, 0.7)], ml.octagon(2.4, 2.4, 0.6))
    # ---- 5/6 the two wings: swept flat plates drooping outwards; 7 their feather ridges; 8/9 the crescent tips
    for sgn, nm, mat, base in ((-1, "wing_grey", "silver", (176, 180, 194)), (1, "wing_gold", "gold", (206, 164, 66))):
        wp = w.add(m, nm, mat, base=base, smooth=40)
        path = [(sgn * WING_SPAN * t, y(S_GUARD) - 1.4 * t * t - 0.4 * t, 0.0) for t in (0.0, 0.2, 0.4, 0.6, 0.8, 1.0)]
        ml.sweep(wp, path, ml.octagon(1.0, 1.0, 0.25), scales=lambda t: ((WING_H0 + (WING_H1 - WING_H0) * t) * (1.0 if t < 0.98 else 0.4), 0.55 * (1.0 - 0.45 * t)), up=(0, 0, 1))
    fr = w.add(m, "wing_feathers", "silver", base=(224, 228, 240), wear=0.4)
    for sgn in (-1, 1):
        for k in range(4):
            tf = 0.18 + 0.2 * k
            x0, y0 = sgn * WING_SPAN * 0.12, y(S_GUARD) + 0.9 - 0.5 * k
            x1, y1 = sgn * WING_SPAN * (0.55 + 0.12 * k), y(S_GUARD) - 1.2 - 0.9 * k
            ml.bar(fr, (x0, y0, 0.42), (x1, y1, 0.42), 0.32, 0.12, up=(0, 0, 1), taper=1.2)
    for sgn, nm, mat, base in ((-1, "tip_grey", "silver", (210, 214, 226)), (1, "tip_gold", "gold", (222, 180, 74))):
        tp = w.add(m, nm, mat, base=base, smooth=45)
        cx, cy = sgn * WING_SPAN, y(S_GUARD) - 1.6
        arc = [(cx + sgn * 1.2 * math.sin(a), cy + 1.9 * math.cos(a) + 0.3, 0.0) for a in np.linspace(-1.9, 1.9, 10)]
        ml.sweep(tp, arc, ml.octagon(0.9, 0.7, 0.2), scales=lambda t: (1.0 - 0.7 * abs(2 * t - 1) ** 2, 1.0))
    # ---- 10 the angel at the guard's centre: body, head, robe skirt (one gold part)
    an = w.add(m, "angel", "gold", base=(228, 196, 110), smooth=55)
    w.ellipsoid(an, (0, y(S_GUARD) + 0.6, 0.9), 0.75, 1.8, 0.45, sides=10, stacks=8)
    w.ellipsoid(an, (0, y(S_GUARD) + 2.7, 0.95), 0.55, 0.62, 0.5, sides=8, stacks=6)
    ml.lathe(an, "y", (0, 0, 0.8), [(y(S_GUARD) - 2.8, 1.0), (y(S_GUARD) - 1.2, 0.55), (y(S_GUARD) - 0.3, 0.3)], sides=8, squash=0.6)
    # ---- 11 the grip, 12 its gold wrap (a spiral), 13 the navy diamonds
    gp = w.add(m, "grip", "leather", base=(28, 38, 92), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.95, 0.95), (y(S_GRIP[0] + 80), 1.08, 1.08), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    gw = w.add(m, "grip_wrap", "gold", base=(214, 170, 72), smooth=60, wear=0.4)
    pts = w.helix(lambda t: R_GRIP * 1.06, y(S_GRIP[0]) + 0.3, y(S_GRIP[1]) - 0.3, 3.0, 56, phase=0.4)
    ml.sweep(gw, pts, ml.circle(0.2, 6), scales=lambda t: (1.0, 1.0))
    dm = w.add(m, "grip_diamonds", "silver", base=(60, 80, 170), smooth=0)
    for i in range(6):
        ys = y(S_GRIP[0] + 14 + i * 26)
        ml.gem(dm, (0, ys, R_GRIP * 1.02), 0.34, long=1.0, sides=4, axis="z")
    # ---- 14 the winged pommel: a small ball with two upswept feathers
    pm = w.add(m, "pommel", "silver", base=(200, 206, 226), smooth=50)
    ml.lathe(pm, "y", (0, 0, 0), [(y(S_POMMEL[0]) + 0.0, 0.2), (y(S_POMMEL[0]) + 0.6, 1.2), (y(S_POMMEL[0]) + 1.8, 1.5), (y(S_POMMEL[1]) - 0.4, 1.0), (y(S_POMMEL[1]), R_GRIP * 1.0)], sides=10)
    for sgn in (-1, 1):
        ml.sweep(pm, [(sgn * 0.8, y(S_POMMEL[0]) + 1.6, 0.0), (sgn * 2.2, y(S_POMMEL[0]) + 3.2, 0.0), (sgn * 2.9, y(S_POMMEL[0]) + 5.6, 0.0)], ml.octagon(0.9, 0.5, 0.15),
                 scales=lambda t: (1.0 - 0.8 * t, 1.0 - 0.5 * t), up=(0, 0, 1))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (255, 240, 190), 0.10, core=0.0))
    for nm in ("fuller_f", "fuller_b"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (255, 240, 190), 0.35, core=0.3))
    w.paint_part(atlas, m.part("angel"), lambda c: w.set_glow(c, (255, 236, 170), 0.55, core=0.4))
    w.paint_part(atlas, m.part("grip_diamonds"), lambda c: w.set_glow(c, (140, 170, 255), 0.7, core=0.3))
    ref = w.reference("p3/Relics of the world part 3/519a8c7cd143e5c5a6d6a3c5dae5c131.jpg", (380, 130, 720, 940), "palladion")
    w.finish("palladion", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.14, 0.45, 1.0), ref=ref)

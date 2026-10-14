"""Dagger of Betrayal (card, "Rare Item - Requires Attunement", 1d8 necrotic, +1, 3d8 special): a spiralling tri-bladed dagger whose twisted purple-pink blades enclose
three glowing red crystals, a curved gold crossguard, a braided brown leather grip with gold bands, and a cluster of red crystals as the pommel (one more at the tip).

Reference: the card, 640 x 1280. The dagger runs from the pommel cluster's far crystal (45, 58) to the red tip (600, 590): 767 px on the diagonal.
s = card px from the pommel. Axis: y up from the pommel to the point; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 0.42                                       # a dagger in hand
S_TIP = 767.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.055)
S_CRYSTALS = (0.0, 75.0)                             # the red pommel cluster (card (45, 58) to (112, 100))
S_CAP = (75.0, 98.0)                                 # the gold cap under it
S_GRIP = (98.0, 222.0)                               # the braided grip, 26 px thick
R_GRIP = 13.0 * K * 1.05
S_BANDS = (100.0, 150.0, 214.0)                      # the three gold bands with a red zigzag
S_GUARD = 240.0                                      # the curved crossguard (card (240, 210))
GUARD_HALF = 62.0 * K
S_BLADE0 = 252.0
R_BLADE = 27.0 * K                                   # the strands wind round a core about 55 px across
TWIST = 1.1                                          # turns over the length
POCKETS = [(350.0, 42.0), (534.0, 62.0)]             # the two glowing red crystals inside the blade: (centre s, half length px)
BLADE_CORE_W, BLADE_CORE_T = 1.3, 0.45


def y(s):
    return s * K


def radius(s):
    """Radius of the strands' helix at s: a bulge round each crystal, narrowing to the point."""
    t = (s - S_BLADE0) / (S_TIP - S_BLADE0)
    r = R_BLADE * (1.0 - 0.55 * t)
    for c, hl in POCKETS:
        d = (s - c) / hl
        r += R_BLADE * 0.45 * math.exp(-d * d * 1.2)
    return r * max(0.0, 1.0 - max(0.0, t - 0.86) / 0.14)


def build():
    m = ml.Mesh("dagger_of_betrayal")
    # ---- 1 the three twisted blade strands (flat ribbons winding round the axis), 2 the dark core between them, 3 the crystals
    st = w.add(m, "blade_strands", "obsidian", base=(150, 70, 170), smooth=60, uv_mode="loft")
    ss = np.linspace(S_BLADE0, S_TIP - 6, 24)
    for k in range(3):
        ph = 2 * math.pi * k / 3
        pts = [(radius(s) * math.cos(ph + 2 * math.pi * TWIST * (s - S_BLADE0) / (S_TIP - S_BLADE0)), y(s), radius(s) * math.sin(ph + 2 * math.pi * TWIST * (s - S_BLADE0) / (S_TIP - S_BLADE0))) for s in ss]
        ml.sweep(st, pts, ml.ellipse(1.0, 0.55, 8), scales=lambda t: (0.95 * (1.0 - 0.6 * t) if t < 0.98 else 0.05, 0.52 * (1.0 - 0.7 * t) if t < 0.98 else 0.03))
    cr = w.add(m, "blade_core", "obsidian", base=(60, 28, 80), smooth=45)
    ml.loft(cr, "y", (0, 0, 0), [(y(S_BLADE0) - 0.3, 1.0, 1.0), (y(480), 0.8, 0.9), (y(S_TIP - 70), 0.35, 0.5), (y(S_TIP - 8), 0.0, 0.0)], ml.lozenge(BLADE_CORE_W, BLADE_CORE_T, 0.2))
    cy = w.add(m, "blade_crystals", "crystal", base=(255, 60, 74), glow=True, smooth=0)
    for c, hl in POCKETS:
        ml.gem(cy, (0, y(c), 0.0), 1.0 if c < 400 else 1.15, long=hl * K / 0.8, sides=6, axis="y")
    ml.gem(cy, (0, y(S_TIP - 14), 0.0), 0.5, long=4.0, sides=6, axis="y")
    # ---- 4 the curved gold guard: swept up at both ends, 5 its centre boss
    gd = w.add(m, "guard", "gold", base=(214, 168, 70), smooth=50)
    path = [(GUARD_HALF * u, y(S_GUARD) + 1.5 * (abs(u) ** 2.2) - 0.2 * (1 - abs(u)), 0.0) for u in np.linspace(-1.0, 1.0, 13)]
    ml.sweep(gd, path, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (1.1 * (1.0 - 0.65 * abs(2 * t - 1) ** 1.6), 1.0 * (1.0 - 0.5 * abs(2 * t - 1))), up=(0, 0, 1))
    gb = w.add(m, "guard_boss", "gold", base=(230, 180, 78), smooth=45)
    ml.lathe(gb, "y", (0, 0, 0), [(y(S_GUARD) - 1.1, 0.9), (y(S_GUARD) - 0.4, 1.3), (y(S_GUARD) + 0.6, 1.2), (y(S_BLADE0) + 0.6, 0.7)], sides=10, squash=0.8)
    # ---- 6 the grip, 7 its braid, 8 the gold bands
    gp = w.add(m, "grip", "leather", base=(110, 56, 40), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.95, 0.95), (y(160), 1.08, 1.08), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    br = w.add(m, "grip_braid", "leather", base=(150, 84, 56), smooth=60, wear=0.3)
    for ph in (0.0, math.pi):
        pts = w.helix(lambda t: R_GRIP * 1.05, y(S_GRIP[0]) + 0.2, y(S_GRIP[1]) - 0.2, 5.0, 40, phase=ph)
        ml.sweep(br, pts, ml.circle(0.16, 5), scales=lambda t: (1.0, 1.0))
    bd = w.add(m, "grip_bands", "gold", base=(214, 168, 70), wear=0.3)
    for s in S_BANDS:
        ml.collar(bd, y(s), R_GRIP * 1.18, 0.8, lip=0.2, sides=12)
    # ---- 9 the pommel cap, 10 the red crystal cluster
    pc = w.add(m, "pommel_cap", "gold", base=(214, 168, 70), smooth=50)
    ml.lathe(pc, "y", (0, 0, 0), [(y(S_CAP[0]), R_GRIP * 0.5), (y(S_CAP[0]) + 0.4, R_GRIP * 1.35), (y(S_CAP[1]) - 0.4, R_GRIP * 1.3), (y(S_CAP[1]), R_GRIP * 1.0)], sides=10)
    pk = w.add(m, "pommel_crystals", "crystal", base=(255, 70, 86), glow=True, smooth=0)
    for i, (ang, tilt, ln) in enumerate(((0, 0.0, 2.6), (72, 0.38, 2.2), (144, 0.42, 2.0), (216, 0.40, 2.3), (288, 0.36, 2.1))):
        a = math.radians(ang)
        d = np.array([math.sin(tilt) * math.cos(a), -math.cos(tilt), math.sin(tilt) * math.sin(a)])
        ml.gem(pk, tuple(np.array([0.0, y(S_CAP[0]) + 0.2, 0.0]) + d * 0.9), 0.40, long=ln / 0.8, sides=5, axis=tuple(d))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("blade_crystals"), lambda c: (w.tint(c.img, (255, 40, 60), 0.85), w.set_glow(c, (255, 30, 50), 1.0, core=0.15)))
    w.paint_part(atlas, m.part("pommel_crystals"), lambda c: (w.tint(c.img, (255, 40, 60), 0.85), w.set_glow(c, (255, 30, 50), 1.0, core=0.15)))
    w.paint_part(atlas, m.part("blade_strands"), lambda c: (w.tint(c.img, (190, 90, 200), 0.5), w.set_glow(c, (255, 60, 90), 0.18, core=0.0)))
    ref = w.reference("p3/Relics of the world part 3/de268e674e8e4fa8958cd7ef570d9c6a.jpg", (20, 20, 640, 620), "dagger_of_betrayal")
    w.finish("dagger_of_betrayal", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.20, 0.45, 1.0), ref=ref)

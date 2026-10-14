"""Mountainguard's Greataxe (card, "Rare Greataxe, (Heavy, 2-handed) requires attunement"): a long dark brown haft with a grey metal butt cap and a gold wrap, a fluted
steel collar, and a double-bladed head: two broad crescent blades of grey steel swept forward, held together by an ornate gold filigree frame that crosses between them.

Reference: the phone screenshot shown at 900 x 2000 (the file is 1080 x 2400). The axe lies flat: the butt cap's far edge at x = 88, the outer edge of the blades at
x = 850: 762 px. s = px from the butt. Axis: y up from the butt to the head; the two blades stand out to +x and -x and curve forward (+y); faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.45                                       # a greataxe in hand
S_TOP = 762.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TOP)               # cm per px (0.19)
R_HAFT = 8.0 * K * 1.1                               # the haft is about 17 px thick
S_CAP = (0.0, 45.0)                                  # the grey butt cap (x 88..133)
S_WRAP = (45.0, 72.0)                                # the gold wrap under it
S_COLLAR = (505.0, 610.0)                            # the fluted collar swelling toward the head (x 593..698)
R_COLLAR = 19.0 * K
S_HEAD = 640.0                                       # the frame's centre / the blades' root (x 728)
# each blade is a long crescent plate lying alongside the haft's axis, 65 to 110 px off it, concave toward the axis (card: the upper one from (570, 740) over (680, 690) to the horn at (850, 722))
OFFS = [(482.0, 85.0), (540.0, 105.0), (592.0, 118.0), (680.0, 114.0), (762.0, 98.0)]   # (s, centre-line offset from the axis in px)
CHORD0, CHORD1 = 92.0 * K, 26.0 * K                  # the plate is about 92 px across at its root and 26 at the horn
BLADE_T = 1.9


def y(s):
    return s * K


def blade_section(sgn):
    """Counter-clockwise (u, v) of a blade plate (width 1): the cutting edge on the outer side (+u for a blade on +x), thick spine toward the axis."""
    ht = BLADE_T / 2
    pts = [(0.5, 0.0), (0.12, ht * 0.55), (-0.5, ht), (-0.5, -ht), (0.12, -ht * 0.55)]
    return pts if sgn > 0 else [(-p[0], p[1]) for p in pts][::-1]


def blade_path(sgn):
    return [(sgn * float(np.interp(s, [p[0] for p in OFFS], [p[1] for p in OFFS])) * K, y(s), 0.0) for s in np.linspace(OFFS[0][0], OFFS[-1][0], 14)]


def build():
    m = ml.Mesh("mountainguards_greataxe")
    hf = w.add(m, "haft", "wood", base=(66, 42, 30), uv_mode="loft")
    ml.loft(hf, "y", (0, 0, 0), [(y(S_CAP[1]) - 0.5, 1.0, 1.0), (y(300), 0.95, 0.95), (y(S_COLLAR[0] + 20), 1.0, 1.0)], ml.circle(R_HAFT, 12))
    hr = w.add(m, "haft_rings", "iron", base=(52, 52, 60), wear=0.4, smooth=55)
    for s in (130.0, 260.0, 400.0, 495.0):
        ml.collar(hr, y(s), R_HAFT * 1.25, 0.7, lip=0.18, sides=12)
    cp = w.add(m, "butt_cap", "steel", base=(150, 154, 166), smooth=55)
    ml.lathe(cp, "y", (0, 0, 0), [(y(S_CAP[0]), R_HAFT * 0.9), (y(S_CAP[0]) + 0.5, R_HAFT * 1.8), (y(22), R_HAFT * 2.0), (y(S_CAP[1]) - 0.4, R_HAFT * 1.5), (y(S_CAP[1]), R_HAFT * 1.2)], sides=14)
    bw = w.add(m, "butt_wrap", "gold", base=(200, 150, 50), smooth=50, wear=0.4)
    for i in range(5):
        ml.collar(bw, y(S_WRAP[0] + i * 5.2), R_HAFT * 1.3, 0.8, lip=0.2, sides=12)
    # ---- the fluted steel collar: a scalloped section swelling from the haft to the head
    co = w.add(m, "collar", "steel", base=(150, 154, 166), smooth=30)
    ml.loft(co, "y", (0, 0, 0), [(y(S_COLLAR[0]), R_HAFT * 1.0 / R_COLLAR, 1.0 * R_HAFT * 1.0 / R_COLLAR), (y(S_COLLAR[0] + 30), 0.55, 0.55), (y(S_COLLAR[0] + 80), 1.0, 1.0), (y(S_COLLAR[1]), 0.82, 0.82)],
            w.scallop(R_COLLAR, 12, 0.14, 48))
    cr = w.add(m, "collar_rings", "iron", base=(90, 92, 104), smooth=55)
    for s, r in ((S_COLLAR[0] + 6, 1.15), (S_COLLAR[1] - 4, 1.0)):
        ml.collar(cr, y(s), R_COLLAR * 0.78 * r, 0.7, lip=0.18, sides=14)
    # ---- the two blades: long crescent plates alongside the axis, edge outward
    for sgn, nm in ((1, "blade_up"), (-1, "blade_down")):
        bl = w.add(m, nm, "steel", base=(168, 172, 184), smooth=40, uv_mode="loft")
        ml.sweep(bl, blade_path(sgn), blade_section(sgn), scales=lambda t: (CHORD0 + (CHORD1 - CHORD0) * t ** 1.3 if t < 0.97 else 0.1, 1.0 if t < 0.95 else 0.5), up=(0, 0, 1))
    # ---- the gold filigree frame: arches from the head's knot to each blade's middle, and an X between the blades' roots and horns
    fr = w.add(m, "filigree", "gold", base=(206, 148, 42), smooth=50)
    for sgn in (-1, 1):
        arch = [(sgn * 108.0 * K * math.sin(math.pi * 0.5 * t), y(640.0 - 48.0 * t), 0.0) for t in np.linspace(0.0, 1.0, 9)]
        ml.sweep(fr, arch, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (1.5, 1.3))
        xs = [(sgn * 85.0 * K * (1 - t) - sgn * 98.0 * K * t, y(482.0 + 280.0 * t), 0.0) for t in np.linspace(0.0, 1.0, 10)]
        ml.sweep(fr, xs, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (1.6, 1.3))
    kn = w.add(m, "filigree_knot", "gold", base=(226, 168, 54), smooth=55)
    w.ellipsoid(kn, (0, y(640.0), 0.0), 2.4, 3.6, 1.5, sides=12, stacks=8)
    ml.gem(kn, (0, y(640.0), 1.4), 0.7, long=1.2, sides=6, axis="z")
    # ---- engraved lines on the blades: two raised ridges following each blade's length
    ri = w.add(m, "blade_inlay", "gold", base=(214, 164, 56), smooth=0)
    for sgn in (-1, 1):
        for face in (-1, 1):
            pts = [(sgn * float(np.interp(sv, [p[0] for p in OFFS], [p[1] for p in OFFS])) * K - sgn * 0.5, y(sv), face * BLADE_T * 0.40) for sv in (520.0, 600.0, 690.0)]
            ml.bar(ri, pts[0], pts[1], 0.4, 0.08, up=(0, 0, face), taper=2.0)
            ml.bar(ri, pts[1], pts[2], 0.4, 0.08, up=(0, 0, face), taper=2.0)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    for nm in ("filigree", "filigree_knot", "blade_inlay"):
        w.paint_part(atlas, m.part(nm), lambda c: w.set_glow(c, (255, 210, 90), 0.25, core=0.2))
    w.paint_part(atlas, m.part("blade_up"), lambda c: w.set_glow(c, (255, 220, 120), 0.06, core=0.0))
    w.paint_part(atlas, m.part("blade_down"), lambda c: w.set_glow(c, (255, 220, 120), 0.06, core=0.0))
    ref = w.reference("p2/Relics of the world part 2/Screenshot_20261013_223233_Opera.jpg", (96, 768, 1056, 1236), "mountainguards_greataxe")
    w.finish("mountainguards_greataxe", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.12, 0.5, 1.0), ref=ref)

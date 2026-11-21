"""Loki's Scepter, 3D model: a slim golden spear-staff 1.5 m long, a leaf-shaped blade on top, two curled prongs cradling the blue Mind Stone just under the blade,
ribbed grip rings, a pointed butt cap. Same method as wave30_m_paradox.py (loft / sweep / lathe, one atlas, glow skin on the gem), run by script, not Blender.

    python3 -B tools/relics/infinity_b_icons.py        # the sprite first (the mesh is placed on it)
    python3 -B tools/relics/infinity_b_scepter.py      (W1_PREVIEW=<dir> also renders a preview)

Units are cm, y up from the butt, the blade faces +z. Parts (8): butt_cap, shaft, grip_rings, collar, prongs, mind_stone, blade, blade_edge.
No part is named like a grip/handle (a mid-shaft grip must not make the runtime flip the staff).
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml        # noqa: E402
import wave1_models as w     # noqa: E402

TOTAL = 150.0
R_SHAFT = 1.05
S_BLADE0 = 118.0          # where the blade starts
BLADE_LEN = 30.0          # to y 148
BLADE_W = 5.2             # widest point
GEM_Y, GEM_R = 113.0, 2.6  # the Mind Stone, sunk in the curl under the blade


def build():
    m = ml.Mesh("lokis_scepter")
    # ---- the butt cap: a short gold point
    cap = w.add(m, "butt_cap", "gold", smooth=45)
    ml.loft(cap, "-y", (0, 7.0, 0), [(0, 1.0, 1.0), (4.0, 1.5, 1.5), (7.0, 0.0, 0.0)], ml.circle(R_SHAFT * 1.5, 10))
    # ---- the shaft: slim round gold with a gentle swell
    sh = w.add(m, "shaft", "gold", smooth=50)
    ml.loft(sh, "y", (0, 6.5, 0), [(0, 1.0, 1.0), (50, 1.08, 1.08), (100, 1.0, 1.0), (S_BLADE0 - 6.5, 0.9, 0.9)], ml.circle(R_SHAFT, 10))
    # ---- grip rings: a ribbed hand-hold in dark gold
    gr = w.add(m, "grip_rings", "gold", base=(150, 110, 40), wear=0.4)
    for i in range(7):
        ml.collar(gr, 52.0 + i * 3.2, R_SHAFT * 1.35, 1.0, lip=0.2, sides=10)
    # ---- the collar under the stone
    co = w.add(m, "collar", "gold", smooth=40)
    ml.collar(co, S_BLADE0 - 10.0, R_SHAFT * 2.0, 2.2, lip=0.35, sides=12)
    ml.collar(co, GEM_Y - 5.0, R_SHAFT * 1.8, 1.4, lip=0.25, sides=12)
    # ---- two prongs curl out from the collar and round the stone, meeting above it
    pr = w.add(m, "prongs", "gold", smooth=45)
    for sgn in (-1, 1):
        path = []
        for i in range(13):
            t = i / 12.0
            a = math.pi * (0.15 + 0.7 * t)                  # sweep from the collar side, up and round the gem
            path.append((sgn * (GEM_R + 0.9) * math.sin(a), GEM_Y - 4.0 + (GEM_R * 2 + 4.0) * (0.5 - 0.5 * math.cos(a)) + 1.0, 0.0))
        ml.sweep(pr, path, ml.circle(0.55, 6), scales=lambda t: (1.0 - 0.35 * t, 1.0 - 0.35 * t))
    # ---- the Mind Stone: a faceted blue crystal, a lathe with a six sided hex profile
    st = w.add(m, "mind_stone", "crystal", base=(70, 150, 255), smooth=0)
    ml.lathe(st, "y", (0, GEM_Y - GEM_R, 0), [(0.0, 0.0), (GEM_R * 0.5, GEM_R * 0.8), (GEM_R * 1.05, GEM_R * 1.2), (GEM_R * 1.85, GEM_R * 0.7), (GEM_R * 2.2, 0.0)], sides=6)
    # ---- the blade: a leaf, widest a third of the way up, a flat lozenge section
    bl = w.add(m, "blade", "steel", base=(224, 184, 76), smooth=40)
    ml.loft(bl, "y", (0, S_BLADE0, 0), [(0, 0.55, 0.55), (BLADE_LEN * 0.08, 0.85, 0.85), (BLADE_LEN * 0.34, 1.0, 1.0), (BLADE_LEN * 0.7, 0.62, 0.62), (BLADE_LEN, 0.0, 0.0)], ml.lozenge(BLADE_W, 0.9, 0.35))
    be = w.add(m, "blade_edge", "gold", smooth=0)
    ml.loft(be, "y", (0, S_BLADE0 + 1.0, 0), [(0, 0.5, 1.0), (BLADE_LEN * 0.4, 0.4, 1.0), (BLADE_LEN * 0.9 - 1.0, 0.0, 0.0)], ml.lozenge(0.5, 1.15, 0.2))

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("mind_stone"), lambda c: w.set_glow(c, (60, 130, 255), 0.7, core=0.25))
    w.paint_part(atlas, m.part("blade_edge"), lambda c: w.set_glow(c, (255, 224, 120), 0.25, core=0.2))
    w.finish("lokis_scepter", m, atlas, real_m=TOTAL / 100.0, glow=("FFFFFF", 0.10, 0.45, 1.0))


if __name__ == "__main__":
    build()

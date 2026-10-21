#!/usr/bin/env python3
"""The 3D models of the 4 weapons of wave 105 (see weapons.py): mesh (weapon_meshes/<id>.txt), painted skin and glow skin (textures/entity/weapon/), the 16 x 16 inventory sprite rendered
from the finished mesh and the two item models. Built with the 1.97.0 weapon kit (new197_kit / new197_kit2 / new197_lib), the same pipeline as the Genesis Demon-Slayer standard,
only simpler: plain materials, a few parts, a small ambient glow where there is a gem or an edge to light.

    python3 -B tools/relics/new_normal_c/models.py [id ...]       (no ids = all; deterministic)
    N197_PREVIEW=<dir> python3 -B tools/relics/new_normal_c/models.py      (also renders a preview sheet of each)"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
REL = os.path.abspath(os.path.join(HERE, ".."))
sys.path.insert(0, REL)
from new197_kit import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, spike, ml, w, Outline, paint, put_glow, sm, grip, pommel, guard_cross, guard_disc,
                        collar_ring, straight_sword, spear_head, shaft, paint_edge_glow, paint_center_glow, new_mesh, new_atlas, lay_and_paint, finish, paint_glow, blade_curve)  # noqa: E402
from new197_kit2 import extrude  # noqa: E402


def glow_parts(atlas, m, names, rgb, strength=0.9, core=0.5):
    for nm in names:
        paint_glow(atlas, m.part(nm), rgb, strength, core)


def fuller(m, name, y0, y1, gap, base, material="steel", zf=0.5, zt=0.4):
    for sgn, suf in ((1, "_f"), (-1, "_b")):
        p = P(m, name + suf, material, base=base, smooth=50)
        ml.ridge_pair(p, (0.0, y0, sgn * zf), (0.0, y1, sgn * zt), (0, 0, sgn), gap, 0.26, 0.1, 5.0)


def build_orc_tusk_cleaver():
    """Orc Tusk Cleaver: a broad curved blade ground from an orc's tusk, iron collar, sinew-wrapped grip and a small iron pommel cap with a cord loop."""
    m = new_mesh("orc_tusk_cleaver")
    L0 = 15.0
    N = 14
    pts = [(-9.0 * (i / N) ** 1.7, L0 + 60.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "bone", pts, lambda t: 4.4 * (1.0 - t ** 1.7) + 0.25, lambda t: 0.9 * (1.0 - 0.45 * t), bevel=0.5, base=(232, 222, 196), smooth=40)
    collar_ring(m, L0 - 1.0, 2.6, 2.0, "iron", (84, 88, 100), name="collar", sides=10)
    grip(m, 1.5, L0 - 2.0, r=1.7, base=(100, 66, 42), wrap_base=(206, 196, 164), n_wraps=9, material="leather", sides=10, swell=1.1)
    lathe_y(m, "pommel", "iron", 1.8, [(0.0, 1.2), (-1.2, 2.0), (-2.6, 1.8), (-3.4, 0.0)], sides=10, base=(84, 88, 100), caps=(True, False))
    collar_ring(m, -2.0, 2.2, 0.6, "leather", (150, 110, 70), name="cord_loop", sides=8)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("orc_tusk_cleaver", m, atlas, "shortsword", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_direwolf_fang_spear():
    """Direwolf Fang Spear: a long ash pole with a curved ivory fang head, a collar of grey fur under it, a leather-bound grip and an iron butt cap."""
    m = new_mesh("direwolf_fang_spear")
    Y_HEAD = 148.0
    shaft(m, 0.0, Y_HEAD, r=1.5, material="wood", base=(138, 112, 82), name="shaft", sides=10)
    wrap(m, "grip_wraps", 30.0, 68.0, 1.5, 10, "leather", (92, 66, 46))
    lathe_y(m, "butt", "iron", -3.5, [(0.0, 0.0), (1.4, 1.5), (3.5, 1.9)], sides=10, base=(84, 88, 100), caps=(False, True))
    collar_ring(m, 100.0, 1.9, 1.4, "iron", (84, 88, 100), name="band_mid", sides=10)
    lathe_y(m, "socket", "iron", Y_HEAD - 7.0, [(0.0, 1.9), (3.0, 2.4), (7.0, 1.6)], sides=10, base=(92, 96, 108))
    fur = P(m, "fur_collar", "cloth", base=(150, 146, 140), smooth=28)
    for k in range(12):
        a = 2 * math.pi * k / 12
        bx, bz = 2.0 * math.cos(a), 2.0 * math.sin(a)
        spike(fur, (bx, Y_HEAD - 5.0, bz), (bx * 2.3, Y_HEAD - 17.0 - (k % 3) * 3.0, bz * 2.3), 0.9, 4)
    N = 12
    pts = [(4.0 * (i / N) ** 1.8, Y_HEAD - 2.0 + 30.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "fang", "bone", pts, lambda t: 2.7 * (1.0 - t ** 1.3) + 0.2, lambda t: 1.3 * (1.0 - 0.6 * t), bevel=0.5, base=(238, 230, 208), smooth=45)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("direwolf_fang_spear", m, atlas, "spear", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_delvers_pickaxe():
    """Delver's Pickaxe: a short oak haft with a long curved steel pick on one side and a small flat hammer face on the other, rope-bound grip and an iron butt cap."""
    m = new_mesh("delvers_pickaxe")
    YC = 58.0
    shaft(m, 0.0, 70.0, r=1.6, material="wood", base=(124, 88, 52), name="haft", sides=10, swell=[(0.5, 1.12)])
    lathe_y(m, "butt", "iron", -2.5, [(0.0, 0.0), (1.2, 1.7), (2.5, 2.1)], sides=10, base=(84, 88, 100), caps=(False, True))
    wrap(m, "rope_grip", 6.0, 32.0, 1.6, 12, "cloth", (190, 170, 120))
    pts = [(-8.0, YC - 3.0), (-8.0, YC + 3.0), (-1.8, YC + 3.2), (4.0, YC + 3.0), (14.0, YC + 1.8), (25.0, YC - 3.0), (14.0, YC - 1.2), (4.0, YC - 3.0), (-1.8, YC - 3.2)]
    extrude(m, "pick", "steel", pts, 1.3, edge=0.7, base=(170, 178, 194), smooth=38)
    hammer = P(m, "hammer_face", "iron", base=(96, 100, 114), smooth=40)
    ml.loft(hammer, "x", (0, YC, 0), [(-11.0, 3.0, 2.4), (-8.0, 3.4, 2.8), (-6.0, 2.6, 2.2)], ml.octagon(2.0, 2.0, 0.45))
    for x in (-3.0, 3.0):
        collar_ring(m, YC + x * 0.0, 2.1, 1.0, "iron", (100, 104, 116), name="clamp_%d" % int(x))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("delvers_pickaxe", m, atlas, "handaxe", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_dungeon_cooks_ladle():
    """Dungeon Cook's Ladle: a long wooden handle with a dented iron bowl at the end, a leather strap, a hanging hook and a rim with a stew-brown surface."""
    m = new_mesh("dungeon_cooks_ladle")
    YH = 72.0
    shaft(m, 0.0, YH, r=1.7, material="wood", base=(138, 92, 52), name="handle", sides=10)
    wrap(m, "strap", 8.0, 30.0, 1.7, 8, "leather", (96, 62, 40))
    lathe_y(m, "butt", "iron", -3.0, [(0.0, 0.0), (1.4, 1.8), (3.0, 2.2)], sides=10, base=(96, 100, 112), caps=(False, True))
    hook = P(m, "hook", "iron", base=(104, 108, 120), smooth=35)
    ml.bar(hook, (0.0, 4.0, 1.6), (0.0, 4.0, 5.0), 0.7, 0.7, up=(0, 1, 0), taper=1.0)
    ml.bar(hook, (0.0, 4.0, 5.0), (0.0, 7.0, 5.0), 0.7, 0.7, up=(0, 0, 1), taper=1.0)
    lathe_y(m, "bowl", "iron", YH - 1.0, [(0.0, 1.8), (2.0, 5.0), (6.0, 8.6), (10.0, 9.6), (12.0, 9.0), (12.4, 8.2)], sides=16, base=(150, 154, 166))
    lathe_y(m, "stew", "bronze", YH + 10.8, [(0.0, 0.0), (0.2, 7.6), (0.5, 8.0)], sides=16, base=(140, 84, 44), caps=(False, True))
    collar_ring(m, YH + 11.4, 9.0, 0.9, "iron", (120, 124, 136), name="rim", sides=16)
    collar_ring(m, YH - 1.0, 2.2, 1.6, "iron", (96, 100, 112), name="socket", sides=10)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("dungeon_cooks_ladle", m, atlas, "hammer", real_m=1.4, glow=("FFFFFF", 0.1, 0.3, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}


def main(only):
    n = 0
    for name, fn in BUILDERS.items():
        if only and name not in only:
            continue
        fn()
        n += 1
    print("built", n)


if __name__ == "__main__":
    main(set(sys.argv[1:]))

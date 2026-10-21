#!/usr/bin/env python3
"""The 3D models of the 6 weapons of wave 104 (see weapons.py): mesh (weapon_meshes/<id>.txt), painted skin and glow skin (textures/entity/weapon/), the 16 x 16 inventory sprite rendered
from the finished mesh and the two item models. Built with the 1.97.0 weapon kit (new197_kit / new197_kit2 / new197_lib), the same pipeline as the Genesis Demon-Slayer standard,
only simpler: plain materials, a few parts, a small ambient glow where there is a gem or an edge to light.

    python3 -B tools/relics/new_normal_b/models.py [id ...]       (no ids = all; deterministic)
    N197_PREVIEW=<dir> python3 -B tools/relics/new_normal_b/models.py      (also renders a preview sheet of each)"""
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


def build_selection_hopefuls_bokken():
    """Selection Hopeful's Bokken: a slightly curved oak training sword with a darker grain, a round black tsuba, a rope-wrapped grip and a worn pommel cap."""
    m = new_mesh("selection_hopefuls_bokken")
    L0 = 22.0
    N = 16
    pts = [(-4.5 * (i / N) ** 2.0, L0 + 72.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "wood", pts, lambda t: 2.0 * (1.0 - 0.3 * t ** 1.5), lambda t: 0.9 * (1.0 - 0.3 * t), bevel=0.5, base=(184, 144, 96), smooth=48)
    guard = P(m, "tsuba", "iron", base=(30, 30, 36), smooth=40, uv_mode="loft")
    ml.lathe(guard, "y", (0, L0 - 0.5, 0), [(-0.5, 2.0), (-0.25, 4.0), (0.25, 4.0), (0.5, 2.0)], sides=18)
    grip(m, 3.0, L0 - 1.2, r=1.55, base=(110, 80, 52), wrap_base=(168, 148, 108), n_wraps=9, material="cloth", sides=10, swell=1.06)
    lathe_y(m, "pommel_cap", "wood", 3.2, [(0.0, 1.0), (-1.0, 1.7), (-2.2, 1.5), (-3.0, 0.0)], sides=10, base=(120, 88, 56), caps=(True, False))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("selection_hopefuls_bokken", m, atlas, "katana", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_genin_academy_kunai():
    """Genin Academy Kunai: a leaf-shaped dark steel blade, a short wrapped grip and a large ring pommel."""
    m = new_mesh("genin_academy_kunai")
    L0 = 14.0
    st = [(L0, -1.6, 1.6, 0.5), (L0 + 3, -2.6, 2.6, 0.55), (L0 + 11, -3.1, 3.1, 0.55), (L0 + 20, -2.6, 2.6, 0.5), (L0 + 28, -1.6, 1.6, 0.42), (L0 + 33, -0.6, 0.6, 0.3), (L0 + 36, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=2.6, t=0.8, h=0.8, bulb=0.6, curve=0.0), grip_len=13.0, pommel_style="ring", pommel_kw=dict(r=2.6),
                         blade_material="iron", blade_base=(74, 80, 92), guard_material="iron", guard_base=(60, 64, 76), grip_base=(34, 34, 44), wrap_base=(84, 96, 130), bevel=0.5, ricasso=1.0)
    collar_ring(m, 4.0, 1.6, 1.0, "cloth", (84, 96, 130), name="cord_loop")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_edge_glow(atlas, m.part("blade"), out, (150, 180, 255), 0.3, 0.4, 0.4, side="edge")
    finish("genin_academy_kunai", m, atlas, "dagger", glow=("FFFFFF", 0.12, 0.35, 1.0))


def build_east_blue_cutlass():
    """East Blue Cutlass: a broad curved single-edged blade, a sweeping brass basket guard, a dark wood grip and a flat brass pommel."""
    m = new_mesh("east_blue_cutlass")
    L0 = 20.0
    N = 14
    pts = [(-7.0 * (i / N) ** 2.0, L0 + 66.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "steel", pts, lambda t: 3.0 * (1.0 - 0.45 * t ** 1.4), lambda t: 0.55 * (1.0 - 0.4 * t), bevel=0.5, base=(170, 178, 190), smooth=48)
    guard = P(m, "basket", "bronze", base=(184, 140, 70), smooth=40)
    for k in range(5):
        a = math.pi * (k - 2) / 5.0
        ml.bar(guard, (0.0, L0 - 1.0, 0.0), (5.5 * math.sin(a), L0 - 7.0, 5.5 * math.cos(a)), 0.7, 0.7, up=(1, 0, 0), taper=1.0)
    guard_cross(m, L0, half=4.4, t=1.0, h=1.0, material="bronze", base=(184, 140, 70), bulb=0.8, curve=0.0, name="quillon")
    grip(m, 4.0, L0 - 1.0, r=1.5, base=(70, 48, 34), wrap_base=(110, 80, 56), n_wraps=7)
    pommel(m, 3.6, "disc", r=2.4, material="bronze", base=(184, 140, 70))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("east_blue_cutlass", m, atlas, "sabre", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_axel_frontier_club():
    """Axel Frontier Club: a thick tapering oak club with iron nails around the head, leather straps on the grip and an iron butt cap."""
    m = new_mesh("axel_frontier_club")
    lathe_y(m, "club", "wood", 0.0, [(0.0, 1.6), (6.0, 1.7), (20.0, 2.0), (40.0, 3.2), (56.0, 4.6), (66.0, 5.0), (72.0, 3.6), (74.0, 0.0)], sides=12, base=(124, 84, 50), caps=(True, False))
    lathe_y(m, "butt", "iron", -2.0, [(0.0, 0.0), (1.2, 1.8), (2.6, 2.0)], sides=10, base=(84, 88, 100), caps=(False, True))
    wrap(m, "grip_wraps", 4.0, 24.0, 1.7, 8, "leather", (90, 58, 38))
    nails = P(m, "nails", "iron", base=(120, 124, 136), smooth=30)
    for row, y in enumerate((52.0, 60.0, 67.0)):
        for k in range(6):
            a = 2 * math.pi * (k + 0.5 * (row % 2)) / 6
            r0 = 4.4 if y < 64 else 4.9
            spike(nails, (r0 * math.cos(a), y, r0 * math.sin(a)), ((r0 + 2.4) * math.cos(a), y, (r0 + 2.4) * math.sin(a)), 0.7, 4)
    collar_ring(m, 44.0, 3.6, 1.2, "iron", (84, 88, 100), name="band")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("axel_frontier_club", m, atlas, "club", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_apprentice_birch_wand():
    """Apprentice Birch Wand: a pale slim birch rod with a small green bud-shaped crystal at the tip, a wrapped grip and a ring of carved leaves near the base."""
    m = new_mesh("apprentice_birch_wand")
    Y1 = 34.0
    rod(m, "wand", "wood", 0.0, Y1, 0.8, 1.0, sides=8, base=(222, 214, 190), smooth=55, swell=[(0.6, 1.1)])
    wrap(m, "grip_wraps", 2.0, 11.0, 1.0, 5, "cloth", (96, 150, 104))
    cradle = P(m, "bud_cradle", "wood", base=(206, 198, 172), smooth=45)
    for k in range(4):
        a = 2 * math.pi * k / 4
        spike(cradle, (0.9 * math.cos(a), Y1 - 1.0, 0.9 * math.sin(a)), (1.8 * math.cos(a), Y1 + 5.0, 1.8 * math.sin(a)), 0.5, 4)
    gem = P(m, "bud_crystal", "crystal", base=(130, 240, 170), glow=True, smooth=28)
    ml.gem(gem, (0.0, Y1 + 4.0, 0.0), 1.5, 1.1, 6)
    leaves = P(m, "leaf_ring", "cloth", base=(100, 160, 108), smooth=30)
    for k in range(5):
        a = 2 * math.pi * k / 5
        spike(leaves, (1.0 * math.cos(a), 14.0, 1.0 * math.sin(a)), (2.6 * math.cos(a), 11.0, 2.6 * math.sin(a)), 0.6, 4)
    collar_ring(m, 14.0, 1.4, 0.8, "cloth", (100, 160, 108), name="leaf_band")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["bud_crystal"], (140, 255, 180), 1.0, 0.6)
    finish("apprentice_birch_wand", m, atlas, "wand", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_hunter_exam_machete():
    """Hunter Exam Machete: a wide single-edged blade with a clipped tip, a green cord-wrapped handle with a lanyard hole and a short black guard."""
    m = new_mesh("hunter_exam_machete")
    L0 = 18.0
    st = [(L0, -0.6, 4.4, 0.55), (L0 + 5, -0.6, 4.8, 0.55), (L0 + 30, -0.6, 5.2, 0.55), (L0 + 50, -0.6, 5.0, 0.5), (L0 + 56, -0.6, 3.6, 0.45), (L0 + 60, -0.6, 0.6, 0.35), (L0 + 61, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=3.4, t=0.9, h=0.9, bulb=0.7, curve=0.0), grip_len=17.0, pommel_style="ring", pommel_kw=dict(r=2.0),
                         blade_material="steel", blade_base=(140, 146, 152), guard_material="iron", guard_base=(40, 42, 50), grip_base=(52, 80, 56), wrap_base=(100, 150, 104), bevel=0.5, ricasso=1.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("hunter_exam_machete", m, atlas, "cleaver", real_m=1.3, glow=("FFFFFF", 0.1, 0.3, 1.0))


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

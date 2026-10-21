#!/usr/bin/env python3
"""The 3D models of the 8 weapons of wave 103 (see weapons.py): mesh (weapon_meshes/<id>.txt), painted skin and glow skin (textures/entity/weapon/), the 16 x 16 inventory sprite rendered
from the finished mesh and the two item models. Built with the 1.97.0 weapon kit (new197_kit / new197_kit2 / new197_lib), the same pipeline as the Genesis Demon-Slayer standard,
only simpler: plain materials, a few parts, a small ambient glow where there is a gem or an edge to light.

    python3 -B tools/relics/new_special_normal_a/models.py [id ...]       (no ids = all; deterministic)
    N197_PREVIEW=<dir> python3 -B tools/relics/new_special_normal_a/models.py      (also renders a preview sheet of each)"""
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


def build_apprentices_training_sword():
    """Apprentice's Training Sword: a plain steel longsword with a rounded tip, a brass crossguard, a leather grip and a nicked orb pommel."""
    m = new_mesh("apprentices_training_sword")
    L0 = 20.0
    st = [(L0, -2.6, 2.6, 0.55), (L0 + 6, -2.7, 2.7, 0.55), (64, -2.5, 2.5, 0.5), (86, -2.4, 2.4, 0.46), (95, -2.0, 2.0, 0.42), (99, -1.2, 1.2, 0.36), (101, 0.0, 0.0, 0.0)]
    straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=6.5, t=1.2, h=1.2, bulb=1.0, curve=0.0), grip_len=19.0, pommel_style="orb", pommel_kw=dict(r=2.1),
                   blade_material="steel", blade_base=(168, 176, 190), guard_material="bronze", guard_base=(176, 130, 66), grip_base=(88, 58, 38), wrap_base=(140, 100, 70), bevel=0.5)
    fuller(m, "fuller", L0 + 8, 82.0, 0.9, (140, 148, 164))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("apprentices_training_sword", m, atlas, "longsword", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_gate_scavengers_knife():
    """Gate Scavenger's Knife: a short dark blade with a faint blue edge, a small cross guard, a black ribbed grip and a loop pommel."""
    m = new_mesh("gate_scavengers_knife")
    L0 = 12.0
    st = [(L0, -1.9, 1.9, 0.5), (L0 + 5, -2.1, 2.0, 0.5), (L0 + 18, -2.0, 1.9, 0.46), (L0 + 27, -1.5, 1.5, 0.4), (L0 + 32, -0.7, 0.7, 0.3), (L0 + 35, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=3.6, t=0.9, h=0.9, bulb=0.7, curve=0.0), grip_len=12.0, pommel_style="ring", pommel_kw=dict(r=1.9),
                         blade_material="obsidian", blade_base=(40, 44, 58), guard_material="iron", guard_base=(78, 84, 104), grip_base=(24, 24, 32), wrap_base=(60, 70, 100), bevel=0.5, ricasso=1.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_edge_glow(atlas, m.part("blade"), out, (130, 170, 255), 0.35, 0.55, 0.4, side="edge")
    finish("gate_scavengers_knife", m, atlas, "dagger", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_adventurers_handaxe():
    """Adventurer's Handaxe: a broad steel axe head with a small beard on a short oak haft, bound with leather straps and an iron butt cap."""
    m = new_mesh("adventurers_handaxe")
    YC = 58.0
    shaft(m, 0.0, 70.0, r=1.6, material="wood", base=(118, 82, 50), name="haft", sides=10, swell=[(0.5, 1.12)])
    lathe_y(m, "butt", "iron", -2.5, [(0.0, 0.0), (1.2, 1.7), (2.5, 2.1)], sides=10, base=(84, 88, 100), caps=(False, True))
    wrap(m, "haft_wraps", 6.0, 30.0, 1.6, 8, "leather", (100, 66, 44))
    pts = [(-1.8, YC - 12.0), (-8.0, YC - 15.0), (-13.0, YC - 8.0), (-14.5, YC), (-13.0, YC + 8.0), (-8.0, YC + 13.0), (-1.8, YC + 9.0), (1.8, YC + 5.0), (1.8, YC - 5.0)]
    extrude(m, "head", "steel", pts, 1.1, edge=0.7, base=(170, 178, 192), smooth=38)
    poll = P(m, "poll", "iron", base=(96, 100, 114), smooth=40)
    ml.loft(poll, "x", (0, YC, 0), [(1.4, 2.4, 2.0), (3.6, 2.6, 2.2), (5.4, 2.0, 1.7)], ml.octagon(2.0, 2.0, 0.45))
    for y in (YC - 7.0, YC + 7.0):
        collar_ring(m, y, 1.9, 1.3, "leather", (110, 72, 46), name="strap_%d" % int(y))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("adventurers_handaxe", m, atlas, "handaxe", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_lizardman_scouts_spear():
    """Lizardman Scout's Spear: a marsh-stained pole with a leaf-shaped bronze head, a fringe of reed cord under the head, bronze bands and a wrapped grip."""
    m = new_mesh("lizardman_scouts_spear")
    Y_HEAD = 150.0
    shaft(m, 0.0, Y_HEAD, r=1.5, material="wood", base=(88, 118, 82), name="shaft", sides=10)
    wrap(m, "grip_wraps", 30.0, 70.0, 1.5, 10, "cloth", (60, 110, 80))
    for y in (6.0, 100.0, 140.0):
        collar_ring(m, y, 1.9, 1.5, "bronze", (190, 140, 78), name="band_%d" % int(y))
    lathe_y(m, "butt", "bronze", -3.5, [(0.0, 0.0), (1.4, 1.5), (3.5, 1.9)], sides=10, base=(180, 132, 72), caps=(False, True))
    lathe_y(m, "socket", "bronze", Y_HEAD - 7.0, [(0.0, 1.9), (3.0, 2.4), (7.0, 1.6)], sides=10, base=(190, 140, 78))
    fringe = P(m, "reed_fringe", "cloth", base=(88, 150, 96), smooth=28)
    for k in range(10):
        a = 2 * math.pi * k / 10
        bx, bz = 2.0 * math.cos(a), 2.0 * math.sin(a)
        spike(fringe, (bx, Y_HEAD - 6.0, bz), (bx * 2.1, Y_HEAD - 21.0 - (k % 3) * 3.0, bz * 2.1), 0.8, 4)
    spear_head(m, Y_HEAD, 26.0, 3.2, t=0.8, base=(196, 148, 82), material="bronze")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("lizardman_scouts_spear", m, atlas, "spear", glow=("FFFFFF", 0.1, 0.3, 1.0))


def build_hedge_wizards_oak_staff():
    """Hedge Wizard's Oak Staff: a gnarled oak staff whose knotted top cradles a small green crystal, with a cord of leaves tied below it."""
    m = new_mesh("hedge_wizards_oak_staff")
    Y1 = 150.0
    rod(m, "staff", "wood", 0.0, Y1, 1.7, 2.0, sides=9, base=(124, 88, 54), smooth=55, swell=[(0.25, 1.18), (0.55, 0.92), (0.8, 1.15)])
    lathe_y(m, "butt", "iron", -3.0, [(0.0, 0.0), (1.2, 1.7), (3.0, 2.0)], sides=9, base=(84, 88, 98), caps=(False, True))
    cradle = P(m, "cradle", "wood", base=(104, 72, 44), smooth=45)
    for k in range(3):
        a = 2 * math.pi * k / 3
        pts = [(2.2 * math.cos(a) * (1 + 1.2 * (i / 7.0)), Y1 - 2.0 + 15.0 * (i / 7.0) ** 1.1, 2.2 * math.sin(a) * (1 + 1.2 * (i / 7.0))) for i in range(8)]
        ml.sweep(cradle, pts, ml.octagon(2.0, 2.0, 0.5), scales=lambda t: (0.9 * (1 - 0.6 * t) + 0.2, 0.9 * (1 - 0.6 * t) + 0.2))
    gem = P(m, "leaf_crystal", "crystal", base=(120, 230, 160), glow=True, smooth=28)
    ml.gem(gem, (0.0, Y1 + 8.0, 0.0), 2.4, 1.7, 6)
    cord = P(m, "leaf_cord", "cloth", base=(70, 130, 74), smooth=30)
    for k in range(6):
        a = 2 * math.pi * k / 6
        spike(cord, (2.0 * math.cos(a), Y1 - 22.0, 2.0 * math.sin(a)), (4.2 * math.cos(a), Y1 - 31.0, 4.2 * math.sin(a)), 0.9, 4)
    collar_ring(m, Y1 - 22.0, 2.4, 1.6, "leather", (90, 120, 70), name="cord_band")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["leaf_crystal"], (130, 255, 180), 1.0, 0.6)
    finish("hedge_wizards_oak_staff", m, atlas, "staff", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_silvered_shortsword():
    """Silvered Shortsword: a broad mirror-bright silver blade with a blue-wrapped grip, an oval guard and a star-engraved pommel disc."""
    m = new_mesh("silvered_shortsword")
    L0 = 16.0
    st = [(L0, -3.0, 3.0, 0.55), (L0 + 5, -3.2, 3.2, 0.55), (L0 + 28, -3.0, 3.0, 0.5), (L0 + 46, -2.4, 2.4, 0.45), (L0 + 54, -1.4, 1.4, 0.36), (L0 + 58, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="disc", guard_kw=dict(r=4.0, thick=0.6), grip_len=16.0, pommel_style="disc", pommel_kw=dict(r=2.6), blade_material="silver",
                         blade_base=(226, 234, 244), guard_material="silver", guard_base=(190, 200, 216), grip_base=(40, 62, 110), wrap_base=(120, 150, 210), bevel=0.5, ricasso=1.5)
    fuller(m, "fuller", L0 + 6, L0 + 46, 1.0, (200, 214, 236), material="silver")
    star = P(m, "pommel_star", "crystal", base=(160, 210, 255), glow=True, smooth=40)
    ml.gem(star, (0.0, 1.0, 0.0), 0.9, 1.0, 5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_edge_glow(atlas, m.part("blade"), out, (170, 210, 255), 0.4, 0.5, 0.4, side="both")
    glow_parts(atlas, m, ["pommel_star"], (170, 215, 255), 1.0, 0.6)
    finish("silvered_shortsword", m, atlas, "shortsword", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_hakurou_school_practice_katana():
    """Hakurou School Practice Katana: a slightly curved steel blade with a clean temper line, a round black tsuba, a navy cord grip and a small white pommel cap."""
    m = new_mesh("hakurou_school_practice_katana")
    L0 = 22.0
    N = 16
    pts = [(-5.5 * (i / N) ** 2.0, L0 + 74.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "steel", pts, lambda t: 1.9 * (1.0 - 0.4 * t ** 1.5), lambda t: 0.45 * (1.0 - 0.35 * t), bevel=0.55, base=(184, 194, 212), smooth=48)
    guard = P(m, "tsuba", "iron", base=(30, 32, 40), smooth=40, uv_mode="loft")
    ml.lathe(guard, "y", (0, L0 - 0.5, 0), [(-0.5, 2.0), (-0.25, 4.4), (0.25, 4.4), (0.5, 2.0)], sides=20)
    collar_ring(m, L0 - 2.2, 1.7, 1.8, "silver", (196, 202, 214), name="habaki", sides=10)
    grip(m, 3.0, L0 - 2.4, r=1.55, base=(28, 40, 76), wrap_base=(60, 84, 150), n_wraps=10, material="cloth", sides=10, swell=1.08)
    lathe_y(m, "pommel_cap", "silver", 3.2, [(0.0, 1.0), (-1.0, 1.7), (-2.2, 1.5), (-3.0, 0.0)], sides=10, base=(232, 234, 240), caps=(True, False))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    bl = m.part("blade")

    def hamon(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - L0) / 74.0, 0, 1)
        cx = -5.5 * t ** 2.0
        wave = 0.35 * np.sin(pc.Y * 0.4)
        d = pc.X - (cx - 0.5 + wave)
        m_ = sm(d, 0.45)
        pc.img[..., :3] = pc.img[..., :3] * (1 - 0.25 * np.clip(sm(d, 1.5), 0, 1)[..., None]) + np.array([200, 220, 255], np.float32) * 0.25 * np.clip(sm(d, 1.5), 0, 1)[..., None]
        put_glow(pc, m_ * (pc.Y > L0 + 4) * 0.5, (150, 200, 255), 0.6, 0.4)
    paint(atlas, bl, hamon)
    finish("hakurou_school_practice_katana", m, atlas, "katana", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_familia_smiths_mace():
    """Familia Smith's Mace: a flanged dark-iron mace head with copper bands on a leather-bound haft, an anvil stamped on the pommel."""
    m = new_mesh("familia_smiths_mace")
    YH = 68.0
    shaft(m, 0.0, YH, r=1.8, material="wood", base=(106, 70, 44), name="haft", sides=10)
    wrap(m, "haft_wraps", 6.0, 38.0, 1.8, 10, "leather", (90, 58, 38))
    lathe_y(m, "pommel", "iron", -4.0, [(0.0, 0.0), (1.6, 2.2), (4.0, 2.9)], sides=12, base=(90, 94, 108), caps=(False, True))
    anvil = P(m, "pommel_anvil", "bronze", base=(206, 120, 60), smooth=40)
    ml.loft(anvil, "x", (0, -3.5, 0), [(-1.4, 0.8, 0.6), (0.0, 1.0, 0.9), (1.4, 0.8, 0.6)], ml.octagon(2.0, 2.0, 0.4))
    head = P(m, "head", "iron", base=(70, 74, 88), smooth=40, uv_mode="loft")
    ml.lathe(head, "y", (0, YH - 2.0, 0), [(0.0, 2.2), (2.0, 4.4), (8.0, 5.2), (13.0, 4.6), (17.0, 2.4), (18.0, 0.0)], sides=14)
    fl = P(m, "flanges", "iron", base=(82, 86, 100), smooth=40)
    for k in range(6):
        a = 2 * math.pi * k / 6
        c, s = math.cos(a), math.sin(a)
        ml.bar(fl, (4.6 * c, YH + 3.0, 4.6 * s), (7.8 * c, YH + 9.0, 7.8 * s), 0.9, 0.7, up=(-s, 0, c), taper=1.0)
    for y in (YH - 2.0, YH + 15.0):
        collar_ring(m, y, 3.5, 1.4, "bronze", (206, 120, 60), name="band_%d" % int(y))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    finish("familia_smiths_mace", m, atlas, "hammer", real_m=1.5, glow=("FFFFFF", 0.1, 0.3, 1.0))


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

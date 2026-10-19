"""Flame Magic (rebuild 1.93.0): the summons / constructs. Space.PROP models (feet at y = 0, y up, front on -z); drawn by client/prop/FlamePropPainter.java:

    flame_lion      Leo Rugiens: the lion of flame (Mereoleona Vermillion): a bounding lion with a mane of flame wedges all round the face, a tail ending in a flame; idle / run clips
    flame_phoenix   the phoenix of flame: a bird with wings of flame feathers that beat, a crest and a fan of long tail feathers; fly / idle clips

    python3 -B tools/gen_flame_summon_geo.py && python3 -B tools/geo_check.py flame_lion flame_phoenix
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, bird  # noqa: E402

KEY = "flame"
FUR = Mat("mane", "D8561C", dark="8E1E0C", light="FFC060", glow="FF8A2A", glow_amount=0.55, ragged=0, seed=111)
FUR2 = Mat("mane", "F07A22", dark="B02A0C", light="FFD070", glow="FFA23A", glow_amount=0.6, ragged=0, seed=112)
MANE = Mat("flame", "FF7C1E", dark="B3200C", light="FFE276", glow="FF9830", glow_amount=1.0, seed=113, grain=0.04)
HOT = Mat("flame", "FFA52E", dark="E0480F", light="FFF2A0", glow="FFC24A", glow_amount=1.0, seed=114, grain=0.04)
DARK = Mat("plate", "3A1210", dark="170605", light="7A2A1A", glow="FF5A18", glow_amount=0.8, seed=115)
GOLD = Mat("metal", "D9A730", dark="6B4609", light="FFEBA3", glow="FFC866", glow_amount=0.6, seam=1, seed=116)
EYE = Mat("plate", "FFE890", dark="FFB040", light="FFFFFF", glow="FFF0A0", glow_amount=1.0, seed=117)
FEATHER = Mat("feather", "E8561C", dark="9A1C0C", light="FFD060", glow="FF9A2E", glow_amount=1.0, seed=118)
FEATHER2 = Mat("feather", "FF8A24", dark="C0300C", light="FFF0A0", glow="FFC24A", glow_amount=1.0, seed=119)


def lion():
    p = Piece(KEY, "lion", density=3)
    a = Anim()
    a.clip("run", length=0.6, loop=True)
    a.clip("idle", length=2.4, loop=True)
    p.box("root", (-5.0, 13.5, -9.0), (10.0, 11.0, 11.0), FUR)                       # chest
    p.box("root", (-4.6, 13.5, 1.0), (9.2, 10.0, 12.0), FUR2)                         # flanks and haunches
    p.box("root", (-3.4, 12.6, -3.0), (6.8, 1.6, 12.0), DARK)                         # the belly
    p.bone("head", (0, 22.0, -10.0))
    p.box("head", (-4.5, 19.5, -19.0), (9.0, 9.0, 9.0), FUR)
    p.box("head", (-2.8, 19.3, -24.6), (5.6, 4.6, 6.0), FUR2)
    p.box("head", (-1.0, 22.6, -25.4), (2.0, 1.5, 1.2), DARK)
    for sx in (0, 1):
        mbox(p, "head", sx, (-4.4, 28.0, -14.0), (2.6, 2.6, 1.2), FUR2)               # ears
        mbox(p, "head", sx, (-3.6, 24.6, -19.3), (1.5, 1.2, 0.6), EYE)                # eyes
    p.bone("jaw", (0, 20.0, -20.0), parent="head")
    p.box("jaw", (-2.3, 17.8, -24.6), (4.6, 1.8, 5.4), FUR)
    p.box("jaw", (-1.7, 19.5, -24.2), (0.8, 1.5, 0.8), GOLD)
    p.box("jaw", (0.9, 19.5, -24.2), (0.8, 1.5, 0.8), GOLD)
    sine_rot(a, "run", "jaw", 0.6, 0, 10.0, phase=0.0)
    sine_rot(a, "idle", "jaw", 2.4, 0, 3.0)
    sine_rot(a, "run", "head", 0.6, 0, 5.0, phase=0.3)
    # the mane: twelve wedges of flame radiating round the head, flickering
    p.bone("mane", (0, 24.0, -11.0))
    for i in range(12):
        ang = i * 30.0
        L = 7.0 if i % 2 == 0 else 5.2
        p.box("mane", (-1.9, 24.0 + 4.6, -12.5), (3.8, L, 3.0), MANE if i % 2 == 0 else HOT, rot=(0, 0, ang), pivot=(0, 24.0, -11.0))
    sine_rot(a, "run", "mane", 0.6, 2, 6.0)
    sine_rot(a, "idle", "mane", 2.4, 2, 3.0)
    # legs: four bones that swing, gallop pairs
    legs = [("fl", -3.4, -6.0, 0.0), ("fr", 3.4, -6.0, 0.5), ("bl", -3.4, 9.0, 0.5), ("br", 3.4, 9.0, 0.0)]
    for n, x, z, ph in legs:
        p.bone(n, (x, 14.0, z))
        p.box(n, (x - 1.9, 2.0, z - 2.0), (3.8, 12.2, 4.0), FUR)
        p.box(n, (x - 2.3, 0.0, z - 4.0), (4.6, 2.4, 6.4), FUR2)
        p.box(n, (x - 2.4, 0.0, z - 4.4), (4.8, 0.8, 1.0), GOLD)
        sine_rot(a, "run", n, 0.6, 0, 38.0, phase=ph)
        sine_rot(a, "idle", n, 2.4, 0, 2.0, phase=ph)
    # the tail: four swaying segments and a flame
    parent = "root"
    z, y = 13.0, 22.0
    for i in range(4):
        b = "t%d" % i
        p.bone(b, (0, y, z), parent=parent)
        w = 2.8 - i * 0.4
        p.box(b, (-w / 2, y - w / 2, z), (w, w, 4.4), FUR if i % 2 else FUR2, rot=(-14, 0, 0), pivot=(0, y, z))
        sine_rot(a, "run", b, 0.6, 1, 12.0, phase=-i * 0.1)
        sine_rot(a, "idle", b, 2.4, 1, 8.0, phase=-i * 0.1)
        parent = b
        z += 4.0
        y += 1.0
    p.bone("tuft", (0, y, z), parent=parent)
    for k, ang in enumerate((-24, 24)):
        p.box("tuft", (-1.2, y - 0.5, z), (2.4, 6.0, 1.6), HOT if k else MANE, rot=(0, ang, 0), pivot=(0, y, z))
    sine_rot(a, "run", "tuft", 0.6, 1, 16.0, phase=-0.4)
    p.save(a)
    return p


def phoenix():
    p = Piece(KEY, "phoenix", density=3)
    a = Anim()
    bird(p, a, FUR2, FEATHER, FEATHER2, GOLD, EYE, MANE, wing_len=24.0, tail_len=30.0, cy=22.0, flap=0.9, feathers=6, crest_n=3)
    p.save(a)
    return p


def main():
    for m in (lion(), phoenix()):
        print("%-18s cubes %3d" % (m.id, m.cube_count()))


if __name__ == "__main__":
    main()

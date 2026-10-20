"""Great Spirits (1.99.0): the blaze of Salamander, the fire spirit: a MASSIVE dragon with the highest pure destructive power of the four.

    python3 -B tools/gen_salamander_blaze.py          # writes spirit_salamander_blaze (geo, animation, texture, glow map)
    python3 -B tools/geo_check.py spirit_salamander_blaze

Verdict of the 1.99.0 review: the 0.102 dragon body (tools/gen_spirit_lords.py: scaled red body, horns, wings, flames down the spine and on the tail) matches the canon, but next to the new
Sylph and Undine it is thin in detail and reads as a lizard, not as the great fire spirit. It is NOT replaced. This script adds a second model with the SAME skeleton (every bone of
spirit_salamander at the same pivot, read from its geo file, and its clips copied) that the renderer draws over the body as an additive layer: a mane of flames down the neck, flame
horns standing on the head, flames along the trailing edge of both wing membranes, a heavier row of flames down the back, a bigger plume on the tail, glowing claws and drifting embers.
The renderer draws the body 1.3 times bigger (client/SpiritLooks.SALAMANDER_SCALE) so it is MASSIVE.
"""
import copy
import json
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from great_spirits_lib import Piece, Anim, Mat, sine_rot, TAU  # noqa: E402
from geo_builder import ROOT  # noqa: E402

KEY = "spirit"

FLAME = Mat("flame", "C83418", dark="7A0E06", light="F06A24", glow="E8741C", glow_amount=0.8, seed=31, grain=0.04)
FLAME_HOT = Mat("flame", "E85414", dark="A81C0A", light="FF9238", glow="FF8E26", glow_amount=0.85, seed=32, grain=0.04)
FLAME_WHITE = Mat("flame", "FF7A20", dark="D03A14", light="FFC050", glow="FFA830", glow_amount=0.9, seed=33, grain=0.03)
HORN = Mat("flame", "FF8A28", dark="C83410", light="FFBC4C", glow="FFB040", glow_amount=0.9, seed=34, grain=0.03)
EMBER = Mat("plate", "FF9A2C", dark="C8400E", light="FFD070", glow="FFA840", glow_amount=0.9, seed=35)


def source_skeleton():
    g = json.load(open(os.path.join(ROOT, "geo", "entity", "spirit_salamander.geo.json")))["minecraft:geometry"][0]
    return g["bones"]


def flame(p, bone, x, y, z, w, h, d, mat=FLAME, rot=None, pivot=None):
    """One tongue of flame: a cube standing on (x, y, z) (its foot centre), w wide, h tall, d deep; `rot` leans it about `pivot` (default its foot)."""
    pv = pivot if pivot is not None else (x, y, z)
    p.box(bone, (x - w / 2, y, z - d / 2), (w, h, d), mat, d=2, rot=rot, pivot=pv)


def main():
    p = Piece(KEY, "salamander_blaze", density=2)
    bones = source_skeleton()
    for b in bones:
        if b["name"] == "root":
            continue
        p.bone(b["name"], b["pivot"], parent=b.get("parent", "root"), rotation=b.get("rotation"))
    # ---- the mane: flames streaming back from the neck and the crown of the head (neck1 pivot (0,19,-10), neck2 (0,22,-14), head (0,24,-18))
    for i, (x, rz) in enumerate(((-3.0, 22), (-1.5, 10), (0.0, 0), (1.5, -10), (3.0, -22))):
        flame(p, "neck1", x, 22.0, -12.0 + i * 0.4, 2.2, 7.5 - abs(i - 2) * 0.8, 2.2, FLAME if i % 2 else FLAME_HOT, rot=(-24, 0, rz))
        flame(p, "neck2", x * 0.8, 25.5, -16.0 + i * 0.3, 2.0, 7.0 - abs(i - 2) * 0.8, 2.0, FLAME_HOT if i % 2 else FLAME, rot=(-28, 0, rz))
    for i, x in enumerate((-2.2, 0.0, 2.2)):
        flame(p, "head", x, 28.0, -22.0 + i * 1.6, 2.4, 8.0 - abs(i - 1) * 1.8, 2.4, FLAME_WHITE if i == 1 else FLAME_HOT, rot=(-30, 0, (i - 1) * 14))
    # ---- flame horns: a crown of five horns of fire standing on the head, longer toward the middle
    for i, (x, rz, hh) in enumerate(((-3.6, 28, 6.0), (-1.9, 12, 8.0), (0.0, 0, 9.5), (1.9, -12, 8.0), (3.6, -28, 6.0))):
        flame(p, "head", x, 28.5, -20.0, 1.5, hh, 1.5, HORN, rot=(-36, 0, rz))
    # ---- the wings: flames along the trailing edge of both membranes and a glow along the struts
    for sx, name in ((1, "wing_l"), (-1, "wing_r")):
        for i, x0 in enumerate((6.0, 9.2, 12.4, 15.6)):
            flame(p, name, sx * x0, 19.6, 14.6, 2.6, 5.5 + 1.2 * (i % 2), 2.6, FLAME if i % 2 else FLAME_HOT, rot=(-14, 0, sx * 6))
        for i, x0 in enumerate((20.0, 23.2, 26.4, 29.6)):
            flame(p, name + "2", sx * x0, 20.0, 13.4, 2.6, 5.0 + 1.0 * (i % 2), 2.6, FLAME_HOT if i % 2 else FLAME, rot=(-14, 0, sx * 8))
        p.box(name, (4 if sx > 0 else -18, 19.2, -4.2), (14, 3.2, 3.0), FLAME_HOT, d=2)                              # the strut burns
        p.box(name + "2", (18 if sx > 0 else -32, 19.2, -4.0), (14, 3.0, 2.8), FLAME_HOT, d=2)
    # ---- the back: a heavy row of flames from the shoulders to the rump
    for i, z in enumerate((-8.0, -5.0, -2.0, 1.0, 4.0, 7.0)):
        flame(p, "body", 0.0, 21.0, z, 3.4 - 0.2 * i, 7.0 - 0.7 * abs(i - 1.5), 3.0, FLAME_WHITE if i % 2 == 0 else FLAME_HOT, rot=(-10 - 2 * i, 0, 0))
        flame(p, "body", -2.2, 20.5, z + 1.0, 2.2, 4.8 - 0.4 * i * 0, 2.2, FLAME, rot=(-8, 0, 12))
        flame(p, "body", 2.2, 20.5, z + 1.0, 2.2, 4.8, 2.2, FLAME, rot=(-8, 0, -12))
    # ---- the tail: a plume that grows toward the tip
    for i, (name, y, z, hh) in enumerate((("tail1", 18.0, 11.0, 5.0), ("tail2", 18.0, 18.0, 6.0), ("tail3", 17.5, 24.0, 7.0), ("tail4", 17.5, 29.0, 8.0))):
        flame(p, name, 0.0, y, z, 3.2, hh, 3.2, FLAME_HOT if i % 2 else FLAME, rot=(-14, 0, 0))
    flame(p, "tailflame", 0.0, 17.0, 32.0, 6.0, 14.0, 6.0, FLAME_WHITE, rot=(-10, 0, 0))
    flame(p, "tailflame", -2.4, 16.5, 32.0, 3.4, 9.0, 3.4, FLAME_HOT, rot=(-10, 0, 14))
    flame(p, "tailflame", 2.4, 16.5, 32.0, 3.4, 9.0, 3.4, FLAME_HOT, rot=(-10, 0, -14))
    # ---- claws that glow, a little fire round each foot
    for name in ("leg_fl", "leg_fr", "leg_bl", "leg_br"):
        b = next(q for q in bones if q["name"] == name)
        px, pz = b["pivot"][0], b["pivot"][2]
        for c in range(3):
            flame(p, name, px + (c - 1) * 1.4, 0.2, pz - 5.2, 1.2, 2.6, 1.2, HORN)
        flame(p, name, px, 0.2, pz - 1.0, 3.0, 3.0, 3.0, FLAME)
    # ---- embers drifting up round the whole body (a bone that rises and wraps; the clips below move it)
    p.bone("embers", (0, 0, 0), parent="root")
    for i in range(10):
        a = i * TAU / 10
        r = 11.0 + 5.0 * ((i * 7) % 4) / 3.0
        p.box("embers", (r * math.cos(a) - 0.7, 4.0 + (i * 5.3) % 24.0, r * math.sin(a) - 0.7), (1.4, 1.4, 1.4), EMBER, d=2)

    # ---- animation: the body's own clips (same bone names), plus the embers
    src = json.load(open(os.path.join(ROOT, "animations", "entity", "spirit_salamander.animation.json")))
    anim = Anim()
    anim.clips = copy.deepcopy(src["animations"])
    names = {b["name"] for b in bones} | {"embers"}
    for cn, cl in anim.clips.items():
        cl["bones"] = {bn: tr for bn, tr in cl["bones"].items() if bn in names}
    for cn in anim.clips:
        L = anim.clips[cn]["animation_length"]
        if cn in ("idle", "move"):
            anim.pos(cn, "embers", {0.0: (0, 0, 0), L / 2: (0, 3.0, 0), L: (0, 0, 0)})
            anim.rot(cn, "embers", {L * i / 4: (0, 360.0 * i / 4 * (1 if cn == "idle" else 2) * 0.25, 0) for i in range(5)})
        else:
            anim.pos(cn, "embers", {0.0: (0, 0, 0), L: (0, 5.0, 0)})
    # flames flicker: a gentle sway on the head and tail flame bones that the body's clips may not move
    p.save(anim)
    print("%-30s cubes %3d  bones %3d" % (p.id, p.cube_count(), len(p.bones)))


if __name__ == "__main__":
    main()

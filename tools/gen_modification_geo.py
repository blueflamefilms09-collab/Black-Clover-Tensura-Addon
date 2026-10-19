"""Modification Magic (Morris Libardirt, the scholar who edits structure) models of the 1.93.0 rebuild: the temporary armour pieces (THE DISSECTOR'S FRAME) and the constructs
(a clawed tendril, a dissection needle, a modified test subject).

    python3 -B tools/gen_modification_geo.py
    python3 -B tools/geo_check.py modification_helm modification_chest modification_tasset modification_pauldron_r modification_pauldron_l \
        modification_vambrace_r modification_vambrace_l modification_greave_r modification_greave_l modification_back modification_halo \
        modification_tendril modification_needle modification_subject

Look: the structure of a body laid open and rebuilt: bone-white plates and ribs over dark steel, orange rune bands (0xFF9B58, the book's colour) running along the edits, round tinted
lenses, a spine with four clawed tendrils (the Spade raid's tendrils), a ring of dissection needles over the head, a forearm that ends in three long claws. Written for the humanoid's
proportions (tools/transmutation_geo_kit.py); the aura painter anchors each piece on its limb.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np
from transmutation_geo_kit import Sheet, Anim, wave, keys, S, skin_texture, vnoise, MATS  # noqa: E402

KEY = "modification"


def helm():
    sh = Sheet(KEY, "helm", 128, 64, seed=301)
    sh.bone("root", pivot=(0, 28, 0))
    sh.bone("lens_l", "root", (2.2, 29.9, -4.9))
    sh.bone("lens_r", "root", (-2.2, 29.9, -4.9))
    # the face stays free: round tinted lenses on a thin frame, a brow plate with an orange rune, a skull-cap of bone with a crown of five needles, a spinal neck guard
    sh.cube("root", (-4.6, 29.2, -4.7), (9.2, 0.8, 0.8), mat="frame", deco=("bands",))
    sh.cube("root", (-4.7, 29.6, -4.2), (0.8, 0.8, 7.6), mat="frame")
    sh.cube("root", (3.9, 29.6, -4.2), (0.8, 0.8, 7.6), mat="frame")
    sh.cube("root", (-0.6, 29.9, -5.0), (1.2, 0.5, 0.6), mat="frame")
    for sx, bone in ((1, "lens_l"), (-1, "lens_r")):
        sh.cube(bone, (sx * 2.2 - 1.6, 28.3, -5.2), (3.2, 3.2, 0.5), mat="rune_orange", deco=("core",))
        sh.cube(bone, (sx * 2.2 - 1.9, 28.0, -5.0), (3.8, 0.5, 0.5), mat="frame")
        sh.cube(bone, (sx * 2.2 - 1.9, 31.2, -5.0), (3.8, 0.5, 0.5), mat="frame")
        sh.cube(bone, (sx * 2.2 - 1.9, 28.0, -5.0), (0.5, 3.7, 0.5), mat="frame")
        sh.cube(bone, (sx * 2.2 + 1.4, 28.0, -5.0), (0.5, 3.7, 0.5), mat="frame")
    sh.cube("root", (-4.4, 31.4, -4.4), (8.8, 1.2, 8.8), mat="bone", deco=("seam",))
    sh.cube("root", (-4.0, 32.6, -4.0), (8.0, 0.8, 8.0), mat="bone")
    sh.cube("root", (-0.7, 31.9, -4.7), (1.4, 1.6, 0.8), mat="rune_orange", deco=("core",))
    for i, (dx, dz, h, lean) in enumerate(((0.0, 0.0, 3.4, 0), (-2.4, -0.5, 2.6, 14), (2.4, -0.5, 2.6, -14))):
        sh.cube("root", (dx - 0.35, 33.4, dz - 0.35), (0.7, h, 0.7), mat="frame", rot=(0, 0, lean), pv=(dx, 33.4, dz))
        sh.cube("root", (dx - 0.45, 33.2 + h * 0.96, dz - 0.45), (0.9, 0.9, 0.9), mat="rune_orange", deco=("core",), rot=(0, 0, lean), pv=(dx, 33.4, dz))
    sh.cube("root", (-4.3, 24.6, 3.6), (8.6, 5.4, 1.2), mat="bone", deco=("seam",))
    for k in range(4):
        sh.cube("root", (-0.9, 25.2 + k * 1.3, 4.7), (1.8, 0.7, 0.8), mat="rune_orange", deco=("core",))
    a = Anim()
    wave(a, "idle", "lens_l", 4.0, 12, x=S(2.0, 0.2))
    wave(a, "idle", "lens_r", 4.0, 12, x=S(2.0, 0.2))
    sh.save(a)


def chest():
    sh = Sheet(KEY, "chest", 192, 96, seed=311)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("core", "root", (0, 19.5, -3.4))
    # the ribcage laid over the chest: six pairs of bone ribs with gaps, a sternum, a steel belly plate, a spine of vertebrae with orange rings, a back plate
    sh.cube("root", (-4.6, 12.0, -2.8), (9.2, 3.0, 5.6), mat="frame", deco=("seam", "bands"))
    for i in range(5):
        y = 15.8 + i * 1.7
        sh.cube("root", (-4.8, y, -3.0 - 0.1 * (i % 2)), (9.6, 0.9, 1.0), mat="bone", deco=("seam",))
        sh.cube("root", (-5.0, y, -2.4), (0.9, 0.9, 5.0), mat="bone")
        sh.cube("root", (4.1, y, -2.4), (0.9, 0.9, 5.0), mat="bone")
    sh.cube("root", (-0.7, 15.2, -3.6), (1.4, 8.6, 0.9), mat="bone", deco=("seam",))
    sh.cube("core", (-1.4, 18.9, -4.1), (2.8, 2.8, 0.8), mat="rune_orange", deco=("core",), rot=(0, 0, 45), pv=(0, 20.3, -3.8))
    sh.cube("root", (-5.0, 22.8, -3.4), (10.0, 1.6, 6.8), mat="bone", deco=("seam",))
    sh.cube("root", (-5.4, 23.6, -3.0), (10.8, 0.9, 6.0), mat="frame")
    sh.cube("root", (-4.8, 13.4, 1.9), (9.6, 10.2, 1.1), mat="frame", deco=("seam", "bands"))
    for k in range(6):
        sh.cube("root", (-1.1, 13.4 + k * 1.6, 3.0), (2.2, 1.2, 1.4), mat="bone", deco=("seam",))
        sh.cube("root", (-1.5, 13.8 + k * 1.6, 3.4), (3.0, 0.4, 0.7), mat="rune_orange", deco=("core",))
    sh.pair("root", "root", [dict(o=(-4.9, 16.0, -2.4), s=(0.9, 6.5, 4.8), mat="frame", deco=("seam", "bands")),
                             dict(o=(-5.1, 21.0, -2.6), s=(1.2, 2.4, 5.2), mat="bone", deco=("seam",))])
    a = Anim()
    keys(a, "idle", "core", "scale", {0.0: (1, 1, 1), 0.75: (1.25, 1.25, 1.25), 1.5: (1, 1, 1)})
    a.clips["idle"]["animation_length"] = 1.5
    sh.save(a)


def tasset():
    sh = Sheet(KEY, "tasset", 128, 64, seed=321)
    sh.bone("root", pivot=(0, 13, 0))
    sh.bone("pf", "root", (0, 12.8, -2.8))
    sh.bone("pb", "root", (0, 12.8, 2.8))
    sh.bone("pl", "root", (4.8, 12.8, 0))
    sh.bone("pr", "root", (-4.8, 12.8, 0))
    sh.cube("root", (-4.8, 11.8, -2.9), (9.6, 1.5, 5.8), mat="frame", deco=("seam", "bands"))
    sh.cube("root", (-1.3, 11.6, -3.6), (2.6, 2.0, 0.8), mat="bone", deco=("seam",))
    sh.cube("root", (-0.6, 11.9, -4.0), (1.2, 1.2, 0.6), mat="rune_orange", deco=("core",))
    for i in range(5):                                           # hanging scalpels and glass vials on the belt
        x = -4.0 + i * 2.0
        if i % 2 == 0:
            sh.cube("pf", (x - 0.4, 7.2, -3.6), (0.8, 4.6, 0.5), mat="bone", deco=(), rot=(-5, 0, 0), pv=(0, 12.8, -2.8))
            sh.cube("pf", (x - 0.6, 6.0, -3.7), (1.2, 1.4, 0.4), mat="frame", rot=(-5, 0, 0), pv=(0, 12.8, -2.8))
        else:
            sh.cube("pf", (x - 0.8, 8.0, -3.8), (1.6, 3.8, 1.2), mat="rune_orange", deco=("core",), rot=(-5, 0, 0), pv=(0, 12.8, -2.8))
            sh.cube("pf", (x - 0.9, 11.7, -3.9), (1.8, 0.7, 1.4), mat="frame", rot=(-5, 0, 0), pv=(0, 12.8, -2.8))
        sh.cube("pb", (x - 0.7, 7.6, 2.9), (1.4, 4.4, 0.6), mat="frame", deco=("seam", "bands"), rot=(5, 0, 0), pv=(0, 12.8, 2.8))
    sh.cube("pl", (4.8, 8.4, -2.0), (0.7, 3.8, 4.0), mat="bone", deco=("seam",), rot=(0, 0, -4), pv=(4.8, 12.8, 0))
    sh.cube("pr", (-5.5, 8.4, -2.0), (0.7, 3.8, 4.0), mat="bone", deco=("seam",), rot=(0, 0, 4), pv=(-4.8, 12.8, 0))
    a = Anim()
    wave(a, "idle", "pf", 3.0, 12, x=S(2.5, 0.2))
    wave(a, "idle", "pb", 3.0, 12, x=S(-2.5, 0.2))
    wave(a, "idle", "pl", 3.0, 12, z=S(-2.0, 0.3))
    wave(a, "idle", "pr", 3.0, 12, z=S(2.0, 0.3))
    sh.save(a)


def pauldrons():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "pauldron_" + side, 128, 64, seed=331 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("stub1", "root", (sx * 9.0, 25.0, 0))
        sh.bone("stub2", "stub1", (sx * 10.8, 27.0, 0))
        specs = [
            dict(o=(-10.2, 21.2, -3.5), s=(5.2, 1.6, 7.0), rot=(0, 0, 10), pv=(-5.5, 22, 0), mat="bone", deco=("seam",)),
            dict(o=(-9.8, 22.6, -3.3), s=(4.8, 1.5, 6.6), rot=(0, 0, 18), pv=(-5.5, 22, 0), mat="frame", deco=("seam", "bands")),
            dict(o=(-8.8, 23.9, -3.0), s=(4.0, 1.4, 6.0), rot=(0, 0, 28), pv=(-5.5, 22, 0), mat="bone", deco=("seam",)),
            dict(o=(-7.2, 25.1, -2.4), s=(3.0, 1.2, 4.8), rot=(0, 0, 40), pv=(-5.5, 22, 0), mat="frame", deco=("seam", "bands")),
        ]
        for sp in specs:
            sp = dict(sp)
            o, s = sp.pop("o"), sp.pop("s")
            rot, pv = sp.pop("rot", None), sp.pop("pv", None)
            if sx > 0:
                o = (-(o[0] + s[0]), o[1], o[2])
                rot = None if rot is None else (rot[0], -rot[1], -rot[2])
                pv = None if pv is None else (-pv[0], pv[1], pv[2])
            sh.cube("root", o, s, rot=rot, pv=pv, **sp)
        # a vertebra stack on the point of the shoulder, and a short clawed tendril growing from it
        for k in range(3):
            sh.cube("stub1", (sx * 8.4 - 1.2 + sx * 0.0, 22.8 + k * 1.1, -1.2), (2.4, 0.9, 2.4), mat="bone", deco=("seam",), rot=(0, 18 * k, 0), pv=(sx * 8.4, 23.2 + k, 0))
        sh.cube("stub1", (sx * 9.2 - 0.9, 26.0, -0.9), (1.8, 2.4, 1.8), mat="frame", rot=(0, 0, -sx * 28), pv=(sx * 9.0, 25.0, 0))
        sh.cube("stub2", (sx * 11.2 - 0.7, 27.0, -0.7), (1.4, 2.4, 1.4), mat="rune_orange", deco=("core",), rot=(0, 0, -sx * 52), pv=(sx * 10.8, 27.0, 0))
        a = Anim()
        wave(a, "idle", "stub1", 3.0, 12, z=S(6.0 * sx, 0.1))
        wave(a, "idle", "stub2", 3.0, 12, z=S(10.0 * sx, 0.3))
        sh.save(a)


def vambraces():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "vambrace_" + side, 128, 64, seed=341 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("claw", "root", (sx * 6.0, 11.5, 0))
        cx = sx * 6.0
        # a bone sleeve with orange bands over a steel cuff, and three long claws that grow out of the knuckles (the limb is edited, not worn)
        sh.cube("root", (cx - 2.7, 14.2, -2.7), (5.4, 5.2, 5.4), mat="bone", deco=("seam",))
        sh.cube("root", (cx - 2.9, 17.8, -2.9), (5.8, 0.9, 5.8), mat="frame", deco=("bands",))
        sh.cube("root", (cx - 2.9, 15.8, -2.9), (5.8, 0.7, 5.8), mat="rune_orange", deco=("core",))
        sh.cube("root", (cx - 2.6, 12.0, -2.6), (5.2, 2.2, 5.2), mat="frame", deco=("seam", "bands"))
        for k in range(3):
            x = cx - 1.7 + k * 1.7
            sh.cube("claw", (x - 0.4, 6.6, -2.4), (0.8, 5.2, 0.8), mat="bone", rot=(-8, 0, 0), pv=(x, 11.6, -2.0))
            sh.cube("claw", (x - 0.3, 3.4, -2.8), (0.6, 3.4, 0.6), mat="bone", rot=(-14, 0, 0), pv=(x, 11.6, -2.0), tone=0.15)
            sh.cube("claw", (x - 0.7, 10.6, -2.8), (1.4, 1.4, 1.2), mat="rune_orange", deco=("core",))
        a = Anim()
        wave(a, "idle", "claw", 2.5, 10, x=S(4.0, 0.2))
        sh.save(a)


def greaves():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "greave_" + side, 128, 64, seed=351 + (sx > 0))
        sh.bone("root", pivot=(0, 12, 0))
        cx = sx * 2.0
        sh.cube("root", (cx - 2.6, 2.6, -2.9), (5.2, 5.8, 5.8), mat="frame", deco=("seam", "bands"))
        sh.cube("root", (cx - 2.2, 3.0, -3.7), (4.4, 5.4, 1.0), mat="bone", deco=("seam",))
        sh.cube("root", (cx - 0.5, 3.6, -4.4), (1.0, 4.4, 0.8), mat="rune_orange", deco=("core",))
        sh.cube("root", (cx - 2.8, 8.0, -3.1), (5.6, 1.0, 6.2), mat="bone", deco=("seam",))
        sh.cube("root", (cx - 2.3, 8.6, -3.8), (4.6, 2.8, 1.2), mat="bone", deco=("seam",))
        sh.cube("root", (cx - 2.9, 0.0, -4.6), (5.8, 2.4, 7.6), mat="frame", deco=("seam", "bands"))
        sh.cube("root", (cx - 0.8, 1.6, 2.6), (1.6, 1.4, 3.6), mat="bone", rot=(-24, 0, 0), pv=(cx, 2.4, 2.6))
        sh.cube("root", (cx - 2.3, 0.0, -5.6), (4.6, 1.5, 1.2), mat="bone")
        sh.save(Anim())


def back():
    """Four clawed tendrils from the upper back, each of four segments that sway on their own beat, over a steel rack with an orange core."""
    sh = Sheet(KEY, "back", 256, 128, seed=361)
    sh.bone("root", pivot=(0, 24, 0))
    sh.cube("root", (-3.4, 14.0, 2.0), (6.8, 9.6, 1.4), mat="frame", deco=("seam", "bands"))
    sh.cube("root", (-1.2, 18.0, 3.2), (2.4, 2.4, 0.8), mat="rune_orange", deco=("core",))
    sh.cube("root", (-4.0, 21.6, 2.2), (8.0, 1.4, 1.8), mat="bone", deco=("seam",))
    specs = [(-5.0, 1, -34), (5.0, 1, 34), (-2.2, 1, -12), (2.2, 1, 12)]
    seg_len, thick = (7.0, 6.0, 5.4, 4.8), (2.2, 1.9, 1.6, 1.3)
    for t, (x0, _, lean) in enumerate(specs):
        base = (x0, 21.0, 4.0)
        prev = "root"
        py = base[1]
        for s in range(4):
            name = "t%d_%d" % (t, s)
            sh.bone(name, prev, (x0, py, 4.4 + 0.3 * s))
            w = thick[s]
            sh.cube(name, (x0 - w / 2, py, 3.6 + 0.3 * s), (w, seg_len[s], w), mat="frame" if s % 2 == 0 else "bone", deco=("seam", "bands") if s % 2 == 0 else ("seam",))
            if s % 2 == 0:
                sh.cube(name, (x0 - w / 2 - 0.15, py + seg_len[s] - 0.6, 3.5 + 0.3 * s), (w + 0.3, 0.6, w + 0.3), mat="rune_orange", deco=("core",))
            py += seg_len[s]
            prev = name
        claw = "t%d_c" % t
        sh.bone(claw, prev, (x0, py, 4.4))
        for k in (-1, 1):
            sh.cube(claw, (x0 + k * 0.9 - 0.3, py, 3.9), (0.6, 4.2, 0.6), mat="bone", rot=(0, 0, -k * 26), pv=(x0, py, 4.2))
    a = Anim()
    for t, (x0, _, lean) in enumerate(specs):
        out = -math.copysign(abs(lean), x0)          # outward: a positive z turn leans an upright limb toward -x
        for s in range(4):
            name = "t%d_%d" % (t, s)
            wave(a, "idle", name, 4.0, 16, x=S(5.0 + s, 0.1 * t + 0.12 * s, off=34 if s == 0 else 12), z=S(5.0, 0.2 * t + 0.1 * s, off=out * (0.6 if s == 0 else 0.15)))
        wave(a, "idle", "t%d_c" % t, 4.0, 16, x=S(8.0, 0.1 * t + 0.5, off=10))
    # lash: the tendrils whip forward over the shoulders and back (starts and ends at rest)
    for t, (x0, _, lean) in enumerate(specs):
        for s in range(4):
            name = "t%d_%d" % (t, s)
            rest = 34 if s == 0 else 12
            keys(a, "lash", name, "rot", {0.0: (rest, 0, 0), 0.3: (rest + 20 + 6 * s, 0, 0), 0.5: (-60 + 8 * s, 0, 0), 0.9: (rest, 0, 0)})
    a.clips["lash"]["animation_length"] = 0.9
    sh.save(a)


def halo():
    sh = Sheet(KEY, "halo", 128, 64, seed=371)
    sh.bone("root", pivot=(0, 36.0, 0))
    sh.bone("spin", "root", (0, 36.0, 0))
    for k in range(8):                                           # eight dissection needles pointing at the head, the ring turning slowly
        a = 2 * math.pi * k / 8
        x, z = 6.4 * math.cos(a), 6.4 * math.sin(a)
        sh.cube("spin", (x - 0.35, 35.65, z - 3.0), (0.7, 0.7, 6.0), mat="frame", rot=(0, -math.degrees(a) + 90, 0), pv=(x, 36.0, z), deco=("seam",))
        sh.cube("spin", (x - 0.5, 35.5, z + 2.4), (1.0, 1.0, 1.0), mat="rune_orange", deco=("core",), rot=(0, -math.degrees(a) + 90, 0), pv=(x, 36.0, z))
    an = Anim()
    wave(an, "idle", "spin", 9.0, 18, y=lambda p: 360 * p)
    wave(an, "idle", "root", 3.0, 12, kind="pos", y=S(0.5))
    sh.save(an)


# ================================================================================================================================ the constructs
def tendril():
    """A clawed tendril out of the ground: an orange-cracked steel mound, eight tapering segments and a claw of three hooks (about 2.6 blocks)."""
    sh = Sheet(KEY, "tendril", 256, 128, seed=381)
    sh.bone("root", pivot=(0, 0, 0))
    sh.cube("root", (-7.0, 0.0, -7.0), (14.0, 2.0, 14.0), mat="frame", deco=("seam", "bands"))
    sh.cube("root", (-5.0, 2.0, -5.0), (10.0, 1.4, 10.0), mat="bone", deco=("seam",))
    for k in range(4):
        sh.cube("root", (6.0 * math.cos(k * math.pi / 2) - 1.0, 2.0, 6.0 * math.sin(k * math.pi / 2) - 1.0), (2.0, 3.0, 2.0), mat="rune_orange", deco=("core",))
    prev = "root"
    y = 3.0
    widths = (5.0, 4.6, 4.2, 3.8, 3.4, 3.0, 2.6, 2.2)
    for s in range(8):
        name = "s%d" % s
        sh.bone(name, prev, (0, y, 0))
        w = widths[s]
        sh.cube(name, (-w / 2, y, -w / 2), (w, 4.6, w), mat="frame" if s % 2 == 0 else "bone", deco=("seam", "bands") if s % 2 == 0 else ("seam",))
        sh.cube(name, (-w / 2 - 0.2, y + 4.1, -w / 2 - 0.2), (w + 0.4, 0.7, w + 0.4), mat="rune_orange", deco=("core",))
        y += 4.6
        prev = name
    sh.bone("claw", prev, (0, y, 0))
    for k in range(3):
        a = 2 * math.pi * k / 3
        sh.cube("claw", (math.cos(a) * 1.2 - 0.5, y, math.sin(a) * 1.2 - 0.5), (1.0, 4.6, 1.0), mat="bone", rot=(math.sin(a) * 24, 0, -math.cos(a) * 24), pv=(0, y, 0))
        sh.cube("claw", (math.cos(a) * 2.8 - 0.4, y + 4.0, math.sin(a) * 2.8 - 0.4), (0.8, 3.0, 0.8), mat="rune_orange", deco=("core",), rot=(math.sin(a) * 50, 0, -math.cos(a) * 50), pv=(0, y, 0))
    an = Anim()
    for s in range(8):
        wave(an, "idle", "s%d" % s, 4.0, 16, x=S(4.0, 0.08 * s, off=0), z=S(4.0, 0.08 * s + 0.25))
    wave(an, "idle", "claw", 4.0, 16, x=S(10.0, 0.7))
    # strike: it rears back, then whips forward and down
    for s in range(8):
        keys(an, "strike", "s%d" % s, "rot", {0.0: (0, 0, 0), 0.35: (-16, 0, 0), 0.55: (22, 0, 0), 0.8: (10, 0, 0), 1.1: (0, 0, 0)})
    keys(an, "strike", "claw", "rot", {0.0: (0, 0, 0), 0.35: (-30, 0, 0), 0.55: (40, 0, 0), 1.1: (0, 0, 0)})
    an.clips["strike"]["animation_length"] = 1.1
    keys(an, "emerge", "root", "scale", {0.0: (1, 0.04, 1), 0.3: (1.05, 0.7, 1.05), 0.55: (1, 1.06, 1), 0.7: (1, 1, 1)}, loop=False)
    an.clips["emerge"]["animation_length"] = 0.7
    keys(an, "wither", "root", "scale", {0.0: (1, 1, 1), 0.6: (1, 0.04, 1)}, loop=False)
    an.clips["wither"]["animation_length"] = 0.6
    sh.save(an)


def needle():
    """A dissection needle: a long steel pin with a ring handle that glows with a rune, an orange tip (point down along -y... the model points +y)."""
    sh = Sheet(KEY, "needle", 96, 64, seed=391)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("pin", "root", (0, 12, 0))
    sh.cube("pin", (-1.4, 0.0, -1.4), (2.8, 1.0, 2.8), mat="bone", deco=("seam",))
    sh.cube("pin", (-1.0, 1.0, -1.0), (2.0, 5.0, 2.0), mat="frame", deco=("seam", "bands"))
    sh.cube("pin", (-0.45, 6.0, -0.45), (0.9, 12.0, 0.9), mat="frame", deco=("seam",), polish=0.9)
    sh.cube("pin", (-0.3, 18.0, -0.3), (0.6, 6.0, 0.6), mat="frame", polish=0.9)
    sh.cube("pin", (-0.2, 24.0, -0.2), (0.4, 3.0, 0.4), mat="rune_orange", deco=("core",))
    sh.cube("pin", (-2.0, -1.4, -0.5), (4.0, 1.0, 1.0), mat="rune_orange", deco=("core",))
    an = Anim()
    wave(an, "idle", "pin", 3.0, 12, kind="pos", y=S(0.6))
    wave(an, "spin", "pin", 0.4, 8, y=lambda p: 360 * p)
    sh.save(an)


def subject():
    """A test subject: a stitched, edited human of two blocks - an arm that ends in a bone blade, an arm with a claw, exposed ribs and spine spikes, orange eyes."""
    sh = Sheet(KEY, "subject", 256, 160, seed=401)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("pelvis", "root", (0, 12, 0))
    sh.bone("chest", "pelvis", (0, 12, 0))
    sh.bone("head", "chest", (0, 24, 0))
    for side, sx in (("r", -1), ("l", 1)):
        sh.bone("thigh_" + side, "pelvis", (sx * 2.0, 12, 0))
        sh.bone("shin_" + side, "thigh_" + side, (sx * 2.0, 6, 0))
        sh.bone("arm_" + side, "chest", (sx * 5.0, 22, 0))
        sh.bone("fore_" + side, "arm_" + side, (sx * 5.0, 16, 0))
    sh.pair("thigh_r", "thigh_l", [dict(o=(-4.0, 5.8, -2.0), s=(4.0, 6.4, 4.0), mat="flesh_edit"), dict(o=(-4.3, 11.0, -2.3), s=(4.6, 1.2, 4.6), mat="frame", deco=("seam", "bands"))])
    sh.pair("shin_r", "shin_l", [dict(o=(-3.8, 0.8, -1.8), s=(3.6, 5.2, 3.6), mat="flesh_edit"), dict(o=(-4.0, 0.0, -2.8), s=(4.0, 1.4, 5.2), mat="bone", deco=("seam",)),
                                 dict(o=(-4.0, 3.0, -2.2), s=(4.0, 0.8, 4.4), mat="frame", deco=("bands",))])
    sh.cube("pelvis", (-4.0, 10.0, -2.2), (8.0, 3.0, 4.4), mat="frame", deco=("seam", "bands"))
    sh.cube("chest", (-4.0, 12.0, -2.0), (8.0, 4.0, 4.0), mat="flesh_edit")
    sh.cube("chest", (-4.4, 16.0, -2.2), (8.8, 7.0, 4.4), mat="flesh_edit")
    for i in range(4):                                           # exposed ribs
        sh.cube("chest", (-4.7, 17.0 + i * 1.5, -2.9), (9.4, 0.8, 1.0), mat="bone", deco=("seam",))
    sh.cube("chest", (-0.6, 17.0, -3.4), (1.2, 5.6, 0.7), mat="bone", deco=("seam",))
    sh.cube("chest", (-1.2, 19.0, -3.9), (2.4, 2.4, 0.6), mat="rune_orange", deco=("core",))
    sh.cube("chest", (-4.2, 12.0, 1.9), (8.4, 11.0, 1.0), mat="frame", deco=("seam", "bands"))
    for k in range(5):
        sh.cube("chest", (-0.6, 12.8 + k * 2.2, 2.8), (1.2, 1.2, 2.4 + (k % 2)), mat="bone", deco=("seam",))
    sh.cube("head", (-3.4, 24.0, -3.4), (6.8, 7.0, 6.8), mat="flesh_edit")
    sh.cube("head", (-3.6, 28.6, -3.6), (7.2, 1.6, 7.2), mat="frame", deco=("seam", "bands"))
    sh.cube("head", (-2.6, 26.6, -3.8), (2.0, 1.0, 0.5), mat="rune_orange", deco=("core",))
    sh.cube("head", (0.6, 26.6, -3.8), (2.0, 1.0, 0.5), mat="rune_orange", deco=("core",))
    sh.cube("head", (-1.8, 24.6, -3.8), (3.6, 0.6, 0.4), mat="bone")
    sh.cube("head", (-0.5, 29.8, -0.5), (1.0, 3.0, 1.0), mat="bone", rot=(0, 0, 10), pv=(0, 29.8, 0))
    # right arm: the forearm is a bone blade
    sh.cube("arm_r", (-8.2, 16.0, -2.0), (4.0, 6.0, 4.0), mat="flesh_edit")
    sh.cube("arm_r", (-8.6, 21.0, -2.4), (4.8, 1.8, 4.8), mat="bone", deco=("seam",))
    sh.cube("fore_r", (-8.0, 7.0, -1.8), (3.6, 8.0, 3.6), mat="frame", deco=("seam", "bands"))
    sh.cube("fore_r", (-7.0, -4.0, -0.7), (1.4, 11.0, 1.4), mat="bone", polish=0.9)
    sh.cube("fore_r", (-6.8, -9.0, -0.5), (1.0, 6.0, 1.0), mat="bone", polish=0.95)
    sh.cube("fore_r", (-6.6, 4.0, -0.9), (0.6, 4.0, 1.8), mat="rune_orange", deco=("core",))
    # left arm: a clawed hand
    sh.cube("arm_l", (4.2, 16.0, -2.0), (4.0, 6.0, 4.0), mat="flesh_edit")
    sh.cube("arm_l", (3.8, 21.0, -2.4), (4.8, 1.8, 4.8), mat="bone", deco=("seam",))
    sh.cube("fore_l", (4.4, 7.0, -1.8), (3.6, 8.0, 3.6), mat="flesh_edit")
    sh.cube("fore_l", (4.2, 5.0, -2.2), (4.4, 2.2, 4.4), mat="bone", deco=("seam",))
    for k in range(3):
        sh.cube("fore_l", (4.6 + k * 1.2, -1.0, -1.4), (0.8, 6.0, 0.8), mat="bone", rot=(-10, 0, 0), pv=(5.2 + k * 1.2, 5.0, -1.4))
    an = Anim()
    L = 3.0
    wave(an, "idle", "chest", L, 12, x=S(1.2), y=S(2.0, 0.2))
    wave(an, "idle", "head", L, 12, y=S(6.0, 0.3), x=S(2.0))
    wave(an, "idle", "arm_r", L, 12, x=S(3.0, 0.2, off=-4), z=S(1.5))
    wave(an, "idle", "arm_l", L, 12, x=S(3.0, 0.5, off=-4), z=S(-1.5))
    wave(an, "idle", "pelvis", L, 12, kind="pos", y=S(0.3, 0.5))
    ph = lambda p: math.sin(2 * math.pi * p)
    L = 0.9
    wave(an, "walk", "thigh_r", L, 12, x=lambda p: 34 * ph(p))
    wave(an, "walk", "thigh_l", L, 12, x=lambda p: -34 * ph(p))
    wave(an, "walk", "shin_r", L, 12, x=lambda p: -40 * max(0.0, math.cos(2 * math.pi * p)))
    wave(an, "walk", "shin_l", L, 12, x=lambda p: -40 * max(0.0, -math.cos(2 * math.pi * p)))
    wave(an, "walk", "arm_r", L, 12, x=lambda p: -30 * ph(p) - 6)
    wave(an, "walk", "arm_l", L, 12, x=lambda p: 30 * ph(p) - 6)
    wave(an, "walk", "chest", L, 12, y=lambda p: 7 * ph(p), x=lambda p: 6)
    wave(an, "walk", "pelvis", L, 12, kind="pos", y=lambda p: 0.6 * math.cos(4 * math.pi * p) - 0.6)
    keys(an, "strike", "arm_r", "rot", {0.0: (-4, 0, 0), 0.25: (-140, 0, -10), 0.4: (-130, 0, -8), 0.5: (-10, 0, 0), 0.7: (-6, 0, 0), 0.9: (-4, 0, 0)})
    keys(an, "strike", "fore_r", "rot", {0.0: (0, 0, 0), 0.25: (-24, 0, 0), 0.5: (0, 0, 0), 0.9: (0, 0, 0)})
    keys(an, "strike", "chest", "rot", {0.0: (0, 0, 0), 0.25: (-8, -12, 0), 0.5: (16, 14, 0), 0.9: (0, 0, 0)})
    keys(an, "strike", "arm_l", "rot", {0.0: (-4, 0, 0), 0.25: (10, 0, 0), 0.5: (-30, 0, 0), 0.9: (-4, 0, 0)})
    an.clips["strike"]["animation_length"] = 0.9
    keys(an, "rise", "root", "scale", {0.0: (1, 0.05, 1), 0.5: (1, 0.75, 1), 0.8: (1.03, 1.05, 1.03), 0.95: (1, 1, 1)}, loop=False)
    keys(an, "rise", "pelvis", "pos", {0.0: (0, -8, 0), 0.8: (0, 0, 0), 0.95: (0, 0, 0)}, loop=False)
    an.clips["rise"]["animation_length"] = 0.95
    keys(an, "crumble", "pelvis", "pos", {0.0: (0, 0, 0), 0.5: (0, -5, 1), 1.0: (0, -11, 2)}, loop=False)
    keys(an, "crumble", "chest", "rot", {0.0: (0, 0, 0), 0.6: (22, 0, -8), 1.0: (40, 8, -12)}, loop=False)
    keys(an, "crumble", "thigh_r", "rot", {0.0: (0, 0, 0), 1.0: (80, 0, -10)}, loop=False)
    keys(an, "crumble", "thigh_l", "rot", {0.0: (0, 0, 0), 1.0: (86, 0, 14)}, loop=False)
    keys(an, "crumble", "root", "scale", {0.0: (1, 1, 1), 0.8: (1, 1, 1), 1.0: (1, 0.6, 1)}, loop=False)
    an.clips["crumble"]["animation_length"] = 1.0
    sh.save(an)


def veins_skin():
    """Orange veins of rewritten structure running over the skin under the frame (additive shell, textures/aura/modification_veins.png)."""
    def painter(part, layer, face, W, H, rng):
        gl = np.array(MATS["frame"]["glow"], float)
        v = vnoise(H, W, rng, 4) if min(W, H) >= 2 else rng.random((H, W))
        m = np.abs(v - 0.5) < 0.05
        v2 = vnoise(H, W, rng, 2.5) if min(W, H) >= 2 else rng.random((H, W))
        m |= np.abs(v2 - 0.62) < 0.03
        return np.zeros((H, W, 3)) + gl, np.where(m, 255, 0), None
    skin_texture("aura/modification_veins", painter, seed=11)


if __name__ == "__main__":
    for fn in (helm, chest, tasset, pauldrons, vambraces, greaves, back, halo, tendril, needle, subject, veins_skin):
        fn()

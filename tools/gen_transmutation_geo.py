"""Transmutation Magic (Grey, the alchemist) models of the 1.93.0 rebuild: the temporary armour pieces (Alchemist's Gilded Plate) and the transmuted constructs.

    python3 -B tools/gen_transmutation_geo.py
    python3 -B tools/geo_check.py transmutation_helm transmutation_chest transmutation_tasset transmutation_pauldron_r transmutation_pauldron_l \
        transmutation_vambrace_r transmutation_vambrace_l transmutation_greave_r transmutation_greave_l transmutation_back transmutation_halo \
        transmutation_spike transmutation_sentinel transmutation_blade

Look: cast brass and dark iron, engraved with transmutation circles whose lines glow teal-green (0x7AF0D8, the colour of the magic's book), glass gems,
rings that turn. Everything is written for the humanoid's proportions (see tools/transmutation_geo_kit.py); the aura painter anchors each piece on its limb.
Files: geo/entity/transmutation_<name>.geo.json, animations/entity/..., textures/entity/transmutation_<name>(_glow).png, textures/aura/transmutation_glyphs(.png / _glow.png).
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from transmutation_geo_kit import Sheet, Anim, wave, keys, S, skin_texture, circle_mask, _glyph_mask, MATS  # noqa: E402

KEY = "transmutation"
SGN = 1.0       # the sign of a yaw turn in the renderer (checked in the preview)


def ring(sh, bone, cy, r, n, h, t, mat="alch_brass", seed_deco=("seam",), **opt):
    """A polygon ring of n boxes (length chord, height h, thickness t) standing on y = cy about the bone's vertical axis."""
    chord = 2 * r * math.tan(math.pi / n) + 0.3
    for k in range(n):
        a = 2 * math.pi * k / n
        cx, cz = r * math.cos(a), r * math.sin(a)
        sh.cube(bone, (cx - chord / 2, cy, cz - t / 2), (chord, h, t), mat=mat, rot=(0, SGN * -math.degrees(a) + 90, 0), pv=(cx, cy, cz), deco=seed_deco, **opt)


def pauldrons():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "pauldron_" + side, 96, 64, seed=31 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("disc", "root", (sx * 10.6, 24.5, 0))
        specs = [
            dict(o=(-10.4, 21.2, -3.6), s=(5.4, 1.6, 7.2), rot=(0, 0, 10), pv=(-5.5, 22, 0), mat="alch_brass"),
            dict(o=(-10.0, 22.6, -3.5), s=(5.0, 1.6, 7.0), rot=(0, 0, 18), pv=(-5.5, 22, 0), mat="alch_iron", deco=("seam", "bands")),
            dict(o=(-9.2, 24.0, -3.4), s=(4.4, 1.6, 6.8), rot=(0, 0, 28), pv=(-5.5, 22, 0), mat="alch_brass"),
            dict(o=(-7.8, 25.3, -3.0), s=(3.4, 1.3, 6.0), rot=(0, 0, 40), pv=(-5.5, 22, 0), mat="alch_gold"),
            dict(o=(-6.6, 25.4, -2.3), s=(2.6, 1.2, 4.6), mat="alch_iron", deco=("seam", "glyph")),
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
        # the circle standing on the shoulder, its face turned outward; it spins about the axis that points out of it
        cx = sx * 10.6
        sh.cube("disc", (cx - 0.3, 21.0, -3.5), (0.6, 7.0, 7.0), mat="alch_glass", deco=("circle", "seam"), circle_face="west" if sx < 0 else "east", rings=(0.92, 0.66), spokes=3, hollow=True)
        sh.cube("disc", (cx - 0.9, 24.0, -0.5), (1.8, 1.0, 1.0), mat="alch_gold")
        a = Anim()
        wave(a, "idle", "disc", 8.0, 16, x=lambda p: 360 * p)
        sh.save(a)


def vambraces():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "vambrace_" + side, 96, 64, seed=41 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("ring", "root", (sx * 6.0, 15.0, 0))
        outer = "west" if sx < 0 else "east"
        cx = sx * 6.0
        # the forearm guard: a tapering sleeve, three gold bands, the alchemist's glove with a circle on the back of the hand
        sh.cube("root", (cx - 2.7, 14.6, -2.7), (5.4, 4.6, 5.4), mat="alch_brass", deco=("seam", "glyph"), glyph_density=0.12)
        sh.cube("root", (cx - 2.9, 18.6, -2.9), (5.8, 1.2, 5.8), mat="alch_gold", deco=())
        sh.cube("root", (cx - 2.9, 14.0, -2.9), (5.8, 1.0, 5.8), mat="alch_gold", deco=())
        sh.cube("root", (cx - 2.6, 12.0, -2.6), (5.2, 2.2, 5.2), mat="alch_iron", deco=("seam",))
        sh.cube("root", (cx - 0.5 + sx * 2.7, 12.4, -2.0), (1.0, 1.6, 4.0), mat="alch_glass", deco=("circle",), circle_face=outer, rings=(0.9, 0.5), spokes=0, ticks=0)
        for k in range(3):
            sh.cube("root", (cx - 2.4 + k * 1.7, 11.3, -3.0), (1.4, 1.2, 1.0), mat="alch_gold", deco=())      # knuckles
        sh.cube("ring", (cx - 3.4, 18.8, -3.4), (6.8, 0.7, 6.8), mat="alch_glass", deco=("edge_glow",))       # a hovering ring round the elbow
        a = Anim()
        wave(a, "idle", "ring", 6.0, 12, y=lambda p: 360 * p)
        sh.save(a)


def greaves():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "greave_" + side, 96, 64, seed=51 + (sx > 0))
        sh.bone("root", pivot=(0, 12, 0))
        cx = sx * 2.0
        sh.cube("root", (cx - 2.6, 2.4, -2.9), (5.2, 6.0, 5.8), mat="alch_brass", deco=("seam", "glyph"), glyph_density=0.12)
        sh.cube("root", (cx - 2.8, 7.9, -3.2), (5.6, 1.0, 6.4), mat="alch_gold", deco=())
        sh.cube("root", (cx - 2.2, 8.4, -3.7), (4.4, 3.0, 1.3), mat="alch_iron", deco=("seam", "glyph"), polish=0.8)      # the knee guard
        sh.cube("root", (cx - 0.9, 8.9, -4.6), (1.8, 1.8, 1.0), mat="alch_glass", deco=("core",))
        sh.cube("root", (cx - 2.9, 0.0, -4.6), (5.8, 2.4, 7.6), mat="alch_iron", deco=("seam",))                      # the sabaton
        sh.cube("root", (cx - 2.4, 0.0, -5.6), (4.8, 1.6, 1.4), mat="alch_brass", deco=())
        sh.cube("root", (cx - 2.8, 2.0, -4.0), (5.6, 0.6, 1.0), mat="alch_gold", deco=())
        sh.save(Anim())


def helm():
    sh = Sheet(KEY, "helm", 128, 64, seed=61)
    sh.bone("root", pivot=(0, 28, 0))
    sh.bone("fin_l", "root", (4.6, 30.0, 1.0))
    sh.bone("fin_r", "root", (-4.6, 30.0, 1.0))
    sh.bone("gem", "root", (0, 31.2, -4.6))
    # an open circlet: the face stays free. A band of brass with glyphs, a front plate with a gem, swept-back fins, a neck guard, a crest of three blades
    sh.cube("root", (-4.7, 29.6, -4.7), (9.4, 1.8, 0.9), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("root", (-4.7, 29.6, 3.8), (9.4, 1.8, 0.9), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("root", (-4.7, 29.6, -3.8), (0.9, 1.8, 7.6), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("root", (3.8, 29.6, -3.8), (0.9, 1.8, 7.6), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("root", (-4.4, 31.4, -4.4), (8.8, 0.8, 8.8), mat="alch_iron", deco=("seam",))
    sh.cube("root", (-1.6, 29.9, -5.3), (3.2, 2.6, 0.8), mat="alch_gold", deco=())
    sh.cube("gem", (-0.9, 30.4, -5.4), (1.8, 1.8, 0.9), mat="alch_glass", deco=("core",))
    sh.cube("root", (-4.4, 24.6, 3.6), (8.8, 5.2, 1.2), mat="alch_brass", deco=("seam", "glyph"), glyph_density=0.1)     # the neck guard
    for i, (hgt, dz) in enumerate(((4.0, -2.0), (5.8, 0.0), (4.0, 2.0))):
        sh.cube("root", (-0.45, 32.2, dz - 0.8), (0.9, hgt, 1.6), mat="alch_gold" if i == 1 else "alch_brass", deco=("edge_glow",) if i == 1 else ())
    sh.cube("fin_l", (4.6, 29.4, 0.4), (0.8, 3.6, 4.0), mat="alch_brass", deco=("seam",), rot=(-18, 0, -8), pv=(4.6, 30.0, 1.0))
    sh.cube("fin_l", (4.6, 28.6, 3.6), (0.8, 2.4, 3.4), mat="alch_gold", deco=(), rot=(-28, 0, -8), pv=(4.6, 30.0, 1.0))
    sh.cube("fin_r", (-5.4, 29.4, 0.4), (0.8, 3.6, 4.0), mat="alch_brass", deco=("seam",), rot=(-18, 0, 8), pv=(-4.6, 30.0, 1.0))
    sh.cube("fin_r", (-5.4, 28.6, 3.6), (0.8, 2.4, 3.4), mat="alch_gold", deco=(), rot=(-28, 0, 8), pv=(-4.6, 30.0, 1.0))
    a = Anim()
    wave(a, "idle", "fin_l", 3.0, 12, z=S(3.0, 0.1))
    wave(a, "idle", "fin_r", 3.0, 12, z=S(-3.0, 0.1))
    wave(a, "idle", "gem", 3.0, 12, kind="pos", z=S(0.15))
    sh.save(a)


def chest():
    sh = Sheet(KEY, "chest", 160, 96, seed=71)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("gem", "root", (0, 20.5, -3.4))
    # gorget, breastplate with the big circle of the magic, lames over the belly, spine ridge and a back plate with a second circle, side straps
    sh.cube("root", (-5.0, 23.4, -3.4), (10.0, 1.8, 6.8), mat="alch_gold", deco=(), polish=0.9)
    sh.cube("root", (-4.6, 19.2, -2.9), (9.2, 4.4, 1.3), mat="alch_brass", deco=("seam",))
    sh.cube("root", (-4.4, 15.2, -2.7), (8.8, 4.2, 1.0), mat="alch_iron", deco=("seam", "glyph"))
    for i in range(3):
        sh.cube("root", (-4.5 + 0.1 * i, 12.2 + i * 1.0, -2.8 - 0.1 * i), (9.0 - 0.2 * i, 1.2, 1.1), mat="alch_gold" if i == 1 else "alch_brass", deco=())
    sh.cube("root", (-2.9, 18.3, -3.9), (5.8, 5.8, 0.9), mat="alch_glass", deco=("circle", "seam"), circle_face="north", rings=(0.92, 0.64), spokes=3, ticks=20)
    sh.cube("gem", (-0.9, 19.6, -4.4), (1.8, 1.8, 0.7), mat="alch_gold", deco=("core",), rot=(0, 0, 45), pv=(0, 20.5, -4.0))
    sh.cube("root", (-4.7, 14.0, 1.9), (9.4, 9.6, 1.1), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("root", (-3.4, 16.0, 3.0), (6.8, 6.8, 0.7), mat="alch_glass", deco=("circle",), circle_face="south", rings=(0.92, 0.6), spokes=3, ticks=16)
    sh.cube("root", (-0.7, 12.4, 3.5), (1.4, 11.0, 0.8), mat="alch_gold", deco=())
    sh.cube("root", (-4.6, 22.0, 1.9), (9.2, 1.6, 1.4), mat="alch_gold", deco=())
    sh.pair("root", "root", [dict(o=(-4.8, 16.5, -2.4), s=(0.9, 6.0, 4.8), mat="alch_iron", deco=("seam",)),
                             dict(o=(-4.9, 20.5, -2.5), s=(1.1, 2.6, 5.0), mat="alch_gold", deco=())])
    a = Anim()
    wave(a, "idle", "gem", 3.0, 12, z=lambda p: 360 * p)
    sh.save(a)


def tasset():
    sh = Sheet(KEY, "tasset", 128, 64, seed=81)
    sh.bone("root", pivot=(0, 13, 0))
    sh.bone("pf", "root", (0, 12.8, -2.8))
    sh.bone("pb", "root", (0, 12.8, 2.8))
    sh.bone("pl", "root", (4.8, 12.8, 0))
    sh.bone("pr", "root", (-4.8, 12.8, 0))
    sh.cube("root", (-4.7, 11.8, -2.8), (9.4, 1.5, 5.6), mat="alch_iron", deco=("seam",))
    sh.cube("root", (-1.2, 11.6, -3.5), (2.4, 2.0, 0.8), mat="alch_gold", deco=())
    sh.cube("root", (-0.6, 11.9, -3.9), (1.2, 1.4, 0.6), mat="alch_glass", deco=("core",))
    for i in range(3):                                           # hanging plates, front and back
        sh.cube("pf", (-3.6 + i * 2.5 - 0.1, 7.6, -3.5), (2.4, 4.6, 0.7), mat="alch_brass" if i != 1 else "alch_gold", deco=("seam", "glyph") if i != 1 else (), rot=(-4, 0, 0), pv=(0, 12.8, -2.8))
        sh.cube("pb", (-3.6 + i * 2.5 - 0.1, 7.8, 2.8), (2.4, 4.4, 0.7), mat="alch_brass", deco=("seam",), rot=(4, 0, 0), pv=(0, 12.8, 2.8))
    sh.cube("pl", (4.7, 8.4, -2.0), (0.7, 4.0, 4.0), mat="alch_iron", deco=("seam", "glyph"), rot=(0, 0, -4), pv=(4.8, 12.8, 0))
    sh.cube("pr", (-5.4, 8.4, -2.0), (0.7, 4.0, 4.0), mat="alch_iron", deco=("seam", "glyph"), rot=(0, 0, 4), pv=(-4.8, 12.8, 0))
    a = Anim()
    wave(a, "idle", "pf", 3.0, 12, x=S(2.5, 0.2))
    wave(a, "idle", "pb", 3.0, 12, x=S(-2.5, 0.2))
    wave(a, "idle", "pl", 3.0, 12, z=S(-2.0, 0.3))
    wave(a, "idle", "pr", 3.0, 12, z=S(2.0, 0.3))
    sh.save(a)


def back():
    """The great circle behind the back: three discs that turn against one another, six glyph stones orbiting, two brass rods that hold them."""
    sh = Sheet(KEY, "back", 192, 128, seed=91)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("outer", "root", (0, 19.0, 5.6))
    sh.bone("mid", "root", (0, 19.0, 5.0))
    sh.bone("core", "root", (0, 19.0, 4.4))
    sh.bone("orbit", "root", (0, 19.0, 6.4))
    sh.cube("root", (-0.8, 12.5, 3.0), (1.6, 12.0, 1.0), mat="alch_brass", deco=("seam",))
    sh.cube("root", (-3.0, 23.0, 2.8), (6.0, 1.0, 1.2), mat="alch_gold", deco=())
    sh.cube("outer", (-11.0, 8.0, 5.3), (22.0, 22.0, 0.6), mat="alch_glass", deco=("circle",), circle_face="both", rings=(0.95, 0.8), spokes=0, ticks=36, hollow=True)
    sh.cube("mid", (-8.0, 11.0, 4.7), (16.0, 16.0, 0.6), mat="alch_iron", deco=("circle", "seam"), circle_face="both", rings=(0.92, 0.7, 0.5), spokes=6, ticks=24, hollow=True)
    sh.cube("core", (-4.5, 14.5, 4.1), (9.0, 9.0, 0.6), mat="alch_glass", deco=("circle",), circle_face="both", rings=(0.9, 0.55), spokes=3, ticks=0, hollow=True)
    for k in range(6):
        a = 2 * math.pi * k / 6
        x, y = 12.4 * math.cos(a), 12.4 * math.sin(a)
        sh.cube("orbit", (x - 1.0, 19.0 + y - 1.4, 6.0), (2.0, 2.8, 1.0), mat="alch_gold" if k % 2 else "alch_brass", deco=("glyph",) if k % 2 == 0 else ("core",), rot=(0, 0, -math.degrees(a) + 90), pv=(x, 19.0 + y, 6.5))
    a = Anim()
    wave(a, "idle", "outer", 20.0, 20, z=lambda p: 360 * p)
    wave(a, "idle", "mid", 20.0, 20, z=lambda p: -720 * p)
    wave(a, "idle", "core", 20.0, 20, z=lambda p: 1080 * p)
    wave(a, "idle", "orbit", 20.0, 20, z=lambda p: 720 * p)
    sh.save(a)


def halo():
    sh = Sheet(KEY, "halo", 128, 64, seed=101)
    sh.bone("root", pivot=(0, 36.5, 0))
    sh.bone("spin", "root", (0, 36.5, 0))
    sh.bone("tab", "spin", (0, 36.5, 0))
    ring(sh, "spin", 36.2, 5.6, 8, 0.7, 0.9, mat="alch_gold", seed_deco=("edge_glow",))
    ring(sh, "tab", 36.0, 4.4, 8, 1.2, 0.6, mat="alch_glass", seed_deco=("core",))
    for k in range(4):
        a = math.pi / 2 * k
        x, z = 7.0 * math.cos(a), 7.0 * math.sin(a)
        sh.cube("spin", (x - 0.5, 36.4, z - 0.5), (1.0, 2.2, 1.0), mat="alch_brass", deco=("glyph",))
    a = Anim()
    wave(a, "idle", "spin", 8.0, 16, y=lambda p: 360 * p)
    wave(a, "idle", "tab", 8.0, 16, y=lambda p: -540 * p)
    wave(a, "idle", "root", 4.0, 16, kind="pos", y=S(0.5))
    sh.save(a)


# ================================================================================================================================ the constructs
def spike():
    """Iron Spikes: a rune disc in the ground and a cluster of iron thrusting out of it."""
    sh = Sheet(KEY, "spike", 160, 96, seed=111)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("rise", "root", (0, 0, 0))
    sh.bone("main", "rise", (0, 0, 0))
    sh.bone("side", "rise", (0, 0, 0))
    sh.cube("root", (-8.0, 0.0, -8.0), (16.0, 0.5, 16.0), mat="alch_glass", deco=("circle",), circle_face="top", rings=(0.95, 0.8), spokes=6, ticks=32, hollow=True)
    for i, (w, h, y) in enumerate(((6.0, 5.0, 0.4), (5.0, 5.0, 5.2), (4.0, 5.0, 10.0), (3.0, 4.5, 15.0), (2.0, 4.0, 19.4), (1.0, 3.0, 23.2))):
        sh.cube("main", (-w / 2, y, -w / 2), (w, h, w), mat="alch_iron", deco=("seam", "glyph") if i < 3 else ("seam",), polish=0.9)
    for k, (ang, lean, hh) in enumerate(((0, 18, 13.0), (90, 22, 11.0), (180, 16, 12.0), (270, 24, 9.5))):
        x, z = 4.2 * math.cos(math.radians(ang)), 4.2 * math.sin(math.radians(ang))
        rx = lean * math.sin(math.radians(ang)) * 1.0
        rz = -lean * math.cos(math.radians(ang)) * 1.0
        sh.cube("side", (x - 1.6, 0.4, z - 1.6), (3.2, hh * 0.55, 3.2), mat="alch_iron", deco=("seam",), rot=(rx, 0, rz), pv=(x, 0.4, z))
        sh.cube("side", (x - 1.0, 0.4 + hh * 0.5, z - 1.0), (2.0, hh * 0.5, 2.0), mat="alch_iron", deco=("seam",), rot=(rx, 0, rz), pv=(x, 0.4, z))
    a = Anim()
    keys(a, "rise", "rise", "scale", {0.0: (1, 0.04, 1), 0.18: (1.05, 1.18, 1.05), 0.3: (1, 0.96, 1), 0.42: (1, 1.0, 1)}, loop=False)
    keys(a, "rise", "rise", "pos", {0.0: (0, -2, 0), 0.18: (0, 0, 0), 0.42: (0, 0, 0)}, loop=False)
    a.clips["rise"]["animation_length"] = 0.42
    keys(a, "fall", "rise", "scale", {0.0: (1, 1, 1), 0.4: (1, 0.05, 1)}, loop=False)
    wave(a, "idle", "main", 3.0, 12, y=S(2.0))
    wave(a, "idle", "side", 3.0, 12, y=S(-3.0))
    sh.save(a)


def sentinel():
    """The Transmuted Sentinel: a two-block golem of iron and stone, circles engraved over it, a teal glow in the eyes and the chest."""
    sh = Sheet(KEY, "sentinel", 256, 160, seed=121)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("pelvis", "root", (0, 17, 0))
    sh.bone("chest", "pelvis", (0, 19, 0))
    sh.bone("head", "chest", (0, 33, 0))
    sh.bone("ring", "chest", (0, 26, 5.5))
    for side, sx in (("r", -1), ("l", 1)):
        sh.bone("thigh_" + side, "pelvis", (sx * 4.2, 17, 0))
        sh.bone("shin_" + side, "thigh_" + side, (sx * 4.2, 9.0, 0))
        sh.bone("arm_" + side, "chest", (sx * 11.0, 31, 0))
        sh.bone("fore_" + side, "arm_" + side, (sx * 12.0, 21, 0))
    # legs
    sh.pair("thigh_r", "thigh_l", [dict(o=(-7.4, 9.0, -3.4), s=(6.4, 8.4, 6.8), mat="alch_iron", deco=("seam", "glyph")),
                                   dict(o=(-7.8, 15.4, -3.8), s=(7.2, 2.0, 7.6), mat="alch_brass", deco=())])
    sh.pair("shin_r", "shin_l", [dict(o=(-7.2, 2.4, -3.2), s=(6.0, 6.8, 6.4), mat="stone"),
                                 dict(o=(-7.6, 3.0, -4.0), s=(6.8, 4.6, 1.2), mat="alch_iron", deco=("seam",)),
                                 dict(o=(-7.8, 0.0, -5.4), s=(7.2, 2.6, 9.6), mat="alch_iron", deco=("seam",))])
    sh.cube("pelvis", (-7.8, 15.0, -4.0), (15.6, 4.0, 8.0), mat="stone")
    sh.cube("pelvis", (-8.2, 17.4, -4.4), (16.4, 1.8, 8.8), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("pelvis", (-2.0, 16.2, -5.2), (4.0, 3.2, 1.0), mat="alch_gold", deco=())
    # torso
    sh.cube("chest", (-8.4, 20.5, -4.6), (16.8, 10.0, 9.2), mat="stone")
    sh.cube("chest", (-9.0, 25.0, -5.2), (18.0, 6.0, 10.4), mat="alch_iron", deco=("seam", "glyph"))
    sh.cube("chest", (-4.4, 22.4, -5.9), (8.8, 8.8, 0.9), mat="alch_glass", deco=("circle",), circle_face="north", rings=(0.92, 0.66), spokes=3, ticks=24)
    sh.cube("chest", (-1.4, 25.2, -6.3), (2.8, 2.8, 0.7), mat="alch_gold", deco=("core",))
    sh.cube("chest", (-7.4, 21.5, 4.4), (14.8, 9.0, 1.2), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("chest", (-10.5, 30.0, -4.0), (21.0, 2.4, 8.0), mat="alch_brass", deco=("seam",))
    sh.cube("ring", (-6.0, 20.0, 0.0), (12.0, 12.0, 0.6), mat="alch_glass", deco=("circle",), circle_face="both", rings=(0.95, 0.7), spokes=6, ticks=28, hollow=True)
    # head
    sh.cube("head", (-5.0, 33.6, -5.0), (10.0, 8.4, 10.0), mat="alch_iron", deco=("seam",))
    sh.cube("head", (-5.4, 40.0, -5.4), (10.8, 1.6, 10.8), mat="alch_brass", deco=("seam", "glyph"))
    sh.cube("head", (-4.2, 37.0, -5.6), (3.2, 1.4, 0.8), mat="alch_glass", deco=("core",))
    sh.cube("head", (1.0, 37.0, -5.6), (3.2, 1.4, 0.8), mat="alch_glass", deco=("core",))
    sh.cube("head", (-1.0, 41.4, -2.0), (2.0, 3.6, 4.0), mat="alch_gold", deco=("edge_glow",))
    # arms
    sh.pair("arm_r", "arm_l", [dict(o=(-17.0, 28.6, -4.8), s=(9.0, 3.6, 9.6), mat="alch_brass", deco=("seam", "glyph"), rot=(0, 0, 10), pv=(-11, 31, 0)),
                               dict(o=(-16.0, 20.6, -4.2), s=(7.4, 9.0, 8.4), mat="stone"),
                               dict(o=(-16.6, 20.0, -4.8), s=(8.6, 2.0, 9.6), mat="alch_iron", deco=("seam",))])
    sh.pair("fore_r", "fore_l", [dict(o=(-16.4, 9.0, -4.4), s=(7.8, 11.0, 8.8), mat="alch_iron", deco=("seam", "glyph")),
                                 dict(o=(-17.6, 3.4, -5.6), s=(10.0, 6.4, 11.2), mat="stone"),
                                 dict(o=(-17.0, 5.0, -6.0), s=(9.0, 3.0, 1.0), mat="alch_brass", deco=("seam",)),
                                 dict(o=(-17.8, 2.6, -2.0), s=(1.4, 1.4, 4.0), mat="alch_glass", deco=("core",))])
    a = Anim()
    L = 3.2
    wave(a, "idle", "chest", L, 12, x=S(1.2), y=S(2.0, 0.2))
    wave(a, "idle", "head", L, 12, y=S(5.0, 0.3), x=S(1.5))
    wave(a, "idle", "arm_r", L, 12, x=S(2.0, 0.2, off=-3), z=S(1.5))
    wave(a, "idle", "arm_l", L, 12, x=S(2.0, 0.2, off=-3), z=S(-1.5))
    wave(a, "idle", "ring", L, 12, z=lambda p: 360 * p)
    wave(a, "idle", "pelvis", L, 12, kind="pos", y=S(0.4, 0.5))
    ph = lambda p: math.sin(2 * math.pi * p)
    L = 1.2
    wave(a, "walk", "thigh_r", L, 12, x=lambda p: 28 * ph(p))
    wave(a, "walk", "thigh_l", L, 12, x=lambda p: -28 * ph(p))
    wave(a, "walk", "shin_r", L, 12, x=lambda p: -36 * max(0.0, math.cos(2 * math.pi * p)))
    wave(a, "walk", "shin_l", L, 12, x=lambda p: -36 * max(0.0, -math.cos(2 * math.pi * p)))
    wave(a, "walk", "arm_r", L, 12, x=lambda p: -22 * ph(p) - 3)
    wave(a, "walk", "arm_l", L, 12, x=lambda p: 22 * ph(p) - 3)
    wave(a, "walk", "chest", L, 12, y=lambda p: 5 * ph(p))
    wave(a, "walk", "pelvis", L, 12, kind="pos", y=lambda p: 0.7 * math.cos(4 * math.pi * p) - 0.7)
    wave(a, "walk", "ring", L, 12, z=lambda p: 360 * p)
    keys(a, "strike", "arm_r", "rot", {0.0: (-3, 0, 0), 0.30: (-150, 0, -8), 0.46: (-140, 0, -6), 0.58: (-20, 0, 0), 0.78: (-12, 0, 0), 1.1: (-3, 0, 0)})
    keys(a, "strike", "fore_r", "rot", {0.0: (0, 0, 0), 0.30: (-30, 0, 0), 0.58: (-6, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "strike", "chest", "rot", {0.0: (0, 0, 0), 0.30: (-10, -12, 0), 0.58: (16, 14, 0), 0.8: (8, 8, 0), 1.1: (0, 0, 0)})
    keys(a, "strike", "head", "rot", {0.0: (0, 0, 0), 0.30: (-8, 10, 0), 0.58: (10, -12, 0), 1.1: (0, 0, 0)})
    keys(a, "strike", "pelvis", "pos", {0.0: (0, 0, 0), 0.30: (0, 0.4, 1.0), 0.58: (0, -1.2, -3.0), 1.1: (0, 0, 0)})
    keys(a, "strike", "arm_l", "rot", {0.0: (-3, 0, 0), 0.30: (10, 0, 0), 0.58: (-24, 0, 0), 1.1: (-3, 0, 0)})
    a.clips["strike"]["animation_length"] = 1.1
    keys(a, "rise", "root", "scale", {0.0: (1, 0.05, 1), 0.5: (1, 0.7, 1), 0.9: (1.04, 1.06, 1.04), 1.1: (1, 1, 1)}, loop=False)
    keys(a, "rise", "pelvis", "pos", {0.0: (0, -10, 0), 0.9: (0, 0, 0), 1.1: (0, 0, 0)}, loop=False)
    keys(a, "rise", "arm_r", "rot", {0.0: (-40, 0, -50), 0.9: (-3, 0, 0), 1.1: (-3, 0, 0)}, loop=False)
    keys(a, "rise", "arm_l", "rot", {0.0: (-40, 0, 50), 0.9: (-3, 0, 0), 1.1: (-3, 0, 0)}, loop=False)
    a.clips["rise"]["animation_length"] = 1.1
    keys(a, "crumble", "pelvis", "pos", {0.0: (0, 0, 0), 0.5: (0, -6, 1), 1.0: (0, -14, 3), 1.3: (0, -16, 3)}, loop=False)
    keys(a, "crumble", "chest", "rot", {0.0: (0, 0, 0), 0.6: (22, 0, -8), 1.3: (44, 10, -14)}, loop=False)
    keys(a, "crumble", "head", "rot", {0.0: (0, 0, 0), 0.6: (16, -20, 6), 1.3: (34, -40, 14)}, loop=False)
    keys(a, "crumble", "arm_r", "rot", {0.0: (-3, 0, 0), 0.7: (50, 0, -30), 1.3: (90, 0, -60)}, loop=False)
    keys(a, "crumble", "arm_l", "rot", {0.0: (-3, 0, 0), 0.7: (30, 0, 40), 1.3: (70, 0, 70)}, loop=False)
    keys(a, "crumble", "thigh_r", "rot", {0.0: (0, 0, 0), 0.6: (50, 0, -6), 1.3: (84, 0, -14)}, loop=False)
    keys(a, "crumble", "thigh_l", "rot", {0.0: (0, 0, 0), 0.6: (44, 0, 6), 1.3: (90, 0, 22)}, loop=False)
    keys(a, "crumble", "root", "scale", {0.0: (1, 1, 1), 1.0: (1, 1, 1), 1.3: (1, 0.7, 1)}, loop=False)
    a.clips["crumble"]["animation_length"] = 1.3
    sh.save(a)


def blade():
    """An alchemic blade: a double-edged brass-and-iron sword with a glowing fuller, hovering point down (the prop painter turns it)."""
    sh = Sheet(KEY, "blade", 96, 64, seed=131)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("sword", "root", (0, 12, 0))
    sh.bone("ring", "sword", (0, 12, 0))
    # model stands upright: point up (+y), grip below. Length ~ 26 px.
    sh.cube("sword", (-0.9, 0.0, -0.9), (1.8, 5.0, 1.8), mat="alch_iron", deco=("seam",))
    sh.cube("sword", (-1.4, -1.4, -1.4), (2.8, 1.6, 2.8), mat="alch_gold", deco=())
    sh.cube("sword", (-4.4, 5.0, -1.0), (8.8, 1.4, 2.0), mat="alch_gold", deco=())
    sh.cube("sword", (-5.0, 4.4, -0.8), (1.0, 2.6, 1.6), mat="alch_brass", deco=())
    sh.cube("sword", (4.0, 4.4, -0.8), (1.0, 2.6, 1.6), mat="alch_brass", deco=())
    for i, (w, h, y) in enumerate(((3.6, 7.0, 6.4), (3.2, 6.0, 13.4), (2.6, 4.0, 19.4), (1.8, 2.4, 23.4), (1.0, 1.4, 25.8))):
        sh.cube("sword", (-w / 2, y, -0.6), (w, h, 1.2), mat="alch_iron", polish=0.95, deco=("seam",) if i < 2 else ())
    sh.cube("sword", (-0.4, 6.8, -0.9), (0.8, 15.0, 1.8), mat="alch_glass", deco=("core",))
    sh.cube("ring", (-5.0, 8.4, -5.0), (10.0, 0.5, 10.0), mat="alch_glass", deco=("circle",), circle_face="top", rings=(0.95, 0.75), spokes=0, ticks=20, hollow=True)
    a = Anim()
    wave(a, "idle", "sword", 3.0, 12, kind="pos", y=S(0.8))
    wave(a, "idle", "ring", 3.0, 12, y=lambda p: 360 * p)
    wave(a, "spin", "sword", 0.6, 12, y=lambda p: 360 * p)
    sh.save(a)


# ================================================================================================================================ the glyph skin (additive shell over the armour)
def glyph_skin():
    def painter(part, layer, face, W, H, rng):
        yy, xx = np.mgrid[0:H, 0:W]
        gl = np.array(MATS["alch_brass"]["glow"], float)
        m = _glyph_mask(W, H, rng, 0.2)
        if part in ("body", "rarm", "larm") and face in ("north", "south") and layer == "base" and W >= 6 and H >= 8:
            m |= circle_mask(W, H, (0.8,), 0, 0) if part == "body" else m
        alpha = np.where(m, 255, 0)
        rgb = np.zeros((H, W, 3)) + gl
        return rgb, alpha, None
    skin_texture("aura/transmutation_glyphs", painter, seed=7)


if __name__ == "__main__":
    for fn in (pauldrons, vambraces, greaves, helm, chest, tasset, back, halo, spike, sentinel, blade, glyph_skin):
        fn()

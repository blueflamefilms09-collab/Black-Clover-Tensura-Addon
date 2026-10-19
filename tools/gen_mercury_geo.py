"""Mercury Magic (Nozel Silva and the Silver Eagles) models of the 1.93.0 rebuild: the temporary armour pieces (SILVER GUARDIAN, the coat of liquid metal that cracks when it is
broken) and the constructs (a floating Silver Blade, the Silver Eagle, a Silver Spear, the Silver Star of Execution cage).

    python3 -B tools/gen_mercury_geo.py
    python3 -B tools/geo_check.py mercury_helm mercury_chest mercury_tasset mercury_pauldron_r mercury_pauldron_l mercury_vambrace_r mercury_vambrace_l mercury_greave_r \
        mercury_greave_l mercury_back mercury_halo mercury_blade mercury_eagle mercury_spear mercury_cage

Look: smooth chrome with a blue-white sheen (0xC8E8FF, the magic's quicksilver), rounded plates built from stacked slabs so the silhouette flows, pooled boots, drops that hang from the
rims, a pair of eagle wings (two segments each, flapping) on the back, a ring of orbiting droplets. Written for the humanoid's proportions (tools/transmutation_geo_kit.py).
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from transmutation_geo_kit import Sheet, Anim, wave, keys, S, skin_texture, vnoise, MATS, ramp  # noqa: E402

KEY = "mercury"


def wing(sh, sx, root, pivot, inner, outer, mat="chrome", mat2="chrome_dark", scale=1.0, y=0.0, zoff=0.0, feathers=5):
    """One articulated wing on the +x side (sx = 1) or -x side (sx = -1): a leading-edge bar, rows of covert feathers on the inner bone, long primaries on the outer bone."""
    px, py, pz = pivot
    sh.bone(inner, root, (sx * px, py, pz))
    sh.bone(outer, inner, (sx * (px + 10.5 * scale), py, pz))

    def box(bone, x0, length, zz, depth, yaw, m, th=0.6, yy=0.0):
        xa = sx * x0 if sx > 0 else sx * x0 - length
        sh.cube(bone, (xa, py + yy, zz), (length, th, depth), mat=m, rot=(0, sx * yaw, 0), pv=(sx * x0, py, zz), deco=("ripple",))

    box(inner, px, 10.5 * scale, pz - 0.6, 1.8, 0, mat2, th=1.0)
    for i in range(feathers):
        box(inner, px + 0.5, (9.4 - 0.7 * i) * scale, pz + 1.2 + i * 1.9 * scale, 2.2 * scale, -4 - 3 * i, mat if i % 2 == 0 else mat2)
    box(outer, px + 10.5 * scale, 10.5 * scale, pz - 0.5, 1.6, 0, mat2, th=0.9)
    for i in range(feathers + 1):
        box(outer, px + 10.5 * scale, (12.0 - 1.0 * i) * scale, pz + 1.0 + i * 1.8 * scale, 2.0 * scale, -6 - 4 * i, mat if i % 2 == 0 else mat2)


def wing_anim(a, clip, length, inner, outer, sx, lift, beat, phase=0.0):
    """Flap about the bone's z axis: positive z raises a +x wing and lowers a -x wing."""
    wave(a, clip, inner, length, 16, z=lambda p: sx * (lift + beat * math.sin(2 * math.pi * (p + phase))))
    wave(a, clip, outer, length, 16, z=lambda p: sx * (0.5 * lift + 1.2 * beat * math.sin(2 * math.pi * (p + phase - 0.12))))


def helm():
    sh = Sheet(KEY, "helm", 128, 64, seed=501)
    sh.bone("root", pivot=(0, 28, 0))
    sh.bone("drip", "root", (0, 29.5, -5.0))
    # a flowing crown: a smooth band, a crest of three tapering slabs swept back, side fins and a bead of mercury that hangs from the brow
    sh.cube("root", (-4.7, 29.6, -4.7), (9.4, 1.6, 9.4), mat="chrome", polish=1.0)
    sh.cube("root", (-4.4, 31.2, -4.4), (8.8, 1.0, 8.8), mat="chrome_dark")
    for i, (w, h, zz) in enumerate(((1.6, 3.2, -3.0), (1.4, 4.4, -0.8), (1.2, 5.2, 1.6), (1.0, 4.0, 3.6))):
        sh.cube("root", (-w / 2, 32.0, zz - 0.9), (w, h, 1.8), mat="chrome", rot=(i * 6, 0, 0), pv=(0, 32.0, zz))
    sh.cube("root", (-4.6, 24.6, 3.6), (9.2, 5.0, 1.3), mat="chrome_dark")
    sh.pair("root", "root", [dict(o=(-5.5, 29.6, -1.0), s=(0.9, 3.6, 4.8), mat="chrome", rot=(0, 0, 8), pv=(-4.7, 30.0, 0)),
                             dict(o=(-5.8, 28.0, 2.6), s=(0.8, 2.4, 3.2), mat="chrome", rot=(-18, 0, 8), pv=(-4.7, 30.0, 0))])
    sh.cube("drip", (-0.8, 28.0, -5.4), (1.6, 2.0, 1.2), mat="quick_core", deco=("core",))
    sh.cube("drip", (-0.5, 27.2, -5.2), (1.0, 1.2, 0.8), mat="chrome")
    a = Anim()
    keys(a, "idle", "drip", "pos", {0.0: (0, 0, 0), 1.0: (0, -0.9, 0), 1.5: (0, 0.3, 0), 3.0: (0, 0, 0)})
    a.clips["idle"]["animation_length"] = 3.0
    sh.save(a)


def chest():
    sh = Sheet(KEY, "chest", 160, 96, seed=511)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("gem", "root", (0, 20.0, -3.6))
    # three rounded plates that step in toward the waist, a collar, a core of quicksilver, a back plate with a ripple ridge
    sh.cube("root", (-5.0, 23.3, -3.4), (10.0, 1.8, 6.8), mat="chrome", polish=1.0)
    sh.cube("root", (-4.8, 19.4, -3.1), (9.6, 4.2, 1.5), mat="chrome")
    sh.cube("root", (-4.6, 15.6, -2.9), (9.2, 4.0, 1.3), mat="chrome_dark")
    sh.cube("root", (-4.4, 12.4, -2.7), (8.8, 3.4, 1.1), mat="chrome")
    sh.cube("gem", (-1.5, 18.6, -4.4), (3.0, 3.0, 1.0), mat="quick_core", deco=("core",), rot=(0, 0, 45), pv=(0, 20.0, -4.0))
    sh.cube("root", (-4.7, 14.0, 1.9), (9.4, 9.4, 1.2), mat="chrome")
    sh.cube("root", (-0.8, 12.4, 3.1), (1.6, 11.0, 0.9), mat="chrome_dark")
    sh.cube("root", (-4.5, 21.0, 2.9), (9.0, 1.4, 0.8), mat="chrome")
    sh.pair("root", "root", [dict(o=(-4.9, 16.0, -2.4), s=(0.9, 6.6, 4.8), mat="chrome_dark"), dict(o=(-5.1, 20.8, -2.6), s=(1.2, 2.6, 5.2), mat="chrome")])
    a = Anim()
    keys(a, "idle", "gem", "scale", {0.0: (1, 1, 1), 1.5: (1.2, 1.2, 1.2), 3.0: (1, 1, 1)})
    a.clips["idle"]["animation_length"] = 3.0
    sh.save(a)


def tasset():
    sh = Sheet(KEY, "tasset", 128, 64, seed=521)
    sh.bone("root", pivot=(0, 13, 0))
    sh.bone("pf", "root", (0, 12.8, -2.8))
    sh.bone("pb", "root", (0, 12.8, 2.8))
    sh.bone("pl", "root", (4.8, 12.8, 0))
    sh.bone("pr", "root", (-4.8, 12.8, 0))
    sh.cube("root", (-4.8, 11.8, -2.9), (9.6, 1.5, 5.8), mat="chrome_dark")
    sh.cube("root", (-1.2, 11.5, -3.6), (2.4, 2.0, 0.8), mat="quick_core", deco=("core",))
    for i in range(4):                                           # drops of different length hang as a skirt
        h = (4.6, 6.0, 6.0, 4.6)[i]
        sh.cube("pf", (-4.0 + i * 2.1, 12.0 - h, -3.6), (2.0, h, 0.7), mat="chrome" if i % 2 == 0 else "chrome_dark", rot=(-4, 0, 0), pv=(0, 12.8, -2.8))
        sh.cube("pf", (-3.9 + i * 2.1, 10.8 - h, -3.7), (1.8, 1.2, 0.8), mat="quick_core", deco=("core",), rot=(-4, 0, 0), pv=(0, 12.8, -2.8)) if i in (1, 2) else None
        sh.cube("pb", (-4.0 + i * 2.1, 12.0 - h + 0.4, 2.9), (2.0, h - 0.4, 0.7), mat="chrome_dark", rot=(4, 0, 0), pv=(0, 12.8, 2.8))
    sh.cube("pl", (4.8, 8.4, -2.2), (0.7, 4.0, 4.4), mat="chrome", rot=(0, 0, -4), pv=(4.8, 12.8, 0))
    sh.cube("pr", (-5.5, 8.4, -2.2), (0.7, 4.0, 4.4), mat="chrome", rot=(0, 0, 4), pv=(-4.8, 12.8, 0))
    a = Anim()
    wave(a, "idle", "pf", 3.0, 12, x=S(3.0, 0.2))
    wave(a, "idle", "pb", 3.0, 12, x=S(-3.0, 0.2))
    wave(a, "idle", "pl", 3.0, 12, z=S(-2.5, 0.3))
    wave(a, "idle", "pr", 3.0, 12, z=S(2.5, 0.3))
    sh.save(a)


def pauldrons():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "pauldron_" + side, 128, 64, seed=531 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("drop", "root", (sx * 9.0, 21.0, 0))
        specs = [
            dict(o=(-10.4, 21.2, -3.8), s=(5.4, 1.6, 7.6), rot=(0, 0, 10), pv=(-5.5, 22, 0), mat="chrome"),
            dict(o=(-10.0, 22.6, -3.6), s=(5.0, 1.5, 7.2), rot=(0, 0, 18), pv=(-5.5, 22, 0), mat="chrome_dark"),
            dict(o=(-9.2, 23.9, -3.3), s=(4.4, 1.4, 6.6), rot=(0, 0, 28), pv=(-5.5, 22, 0), mat="chrome"),
            dict(o=(-7.8, 25.1, -2.8), s=(3.4, 1.2, 5.6), rot=(0, 0, 40), pv=(-5.5, 22, 0), mat="chrome", polish=1.0),
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
        sh.cube("drop", (sx * 9.0 - 0.9, 19.0, -0.9), (1.8, 2.4, 1.8), mat="quick_core", deco=("core",))
        sh.cube("drop", (sx * 9.0 - 0.5, 18.2, -0.5), (1.0, 1.0, 1.0), mat="chrome")
        a = Anim()
        keys(a, "idle", "drop", "pos", {0.0: (0, 0, 0), 1.2: (0, -1.2, 0), 1.8: (0, 0.2, 0), 3.0: (0, 0, 0)})
        a.clips["idle"]["animation_length"] = 3.0
        sh.save(a)


def vambraces():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "vambrace_" + side, 128, 64, seed=541 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("fin", "root", (sx * 8.4, 16.0, 0))
        cx = sx * 6.0
        sh.cube("root", (cx - 2.7, 14.4, -2.7), (5.4, 4.8, 5.4), mat="chrome")
        sh.cube("root", (cx - 2.9, 17.8, -2.9), (5.8, 1.0, 5.8), mat="chrome_dark")
        sh.cube("root", (cx - 2.9, 14.0, -2.9), (5.8, 0.9, 5.8), mat="chrome_dark")
        sh.cube("root", (cx - 2.6, 12.0, -2.6), (5.2, 2.2, 5.2), mat="chrome", polish=1.0)
        sh.cube("root", (cx - 0.7, 15.6, -3.2), (1.4, 1.4, 0.5), mat="quick_core", deco=("core",))
        # a blade fin along the outer edge of the forearm: a flat double-edged shape
        sh.cube("fin", (sx * 8.4 - 0.3, 12.0, -1.2), (0.6, 7.0, 2.4), mat="chrome", polish=1.0, rot=(0, 0, -sx * 8), pv=(sx * 8.4, 16.0, 0))
        sh.cube("fin", (sx * 8.4 - 0.25, 18.6, -0.8), (0.5, 3.0, 1.6), mat="chrome", polish=1.0, rot=(0, 0, -sx * 8), pv=(sx * 8.4, 16.0, 0))
        a = Anim()
        wave(a, "idle", "fin", 3.0, 12, z=S(2.5 * sx, 0.2))
        sh.save(a)


def greaves():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "greave_" + side, 128, 64, seed=551 + (sx > 0))
        sh.bone("root", pivot=(0, 12, 0))
        cx = sx * 2.0
        sh.cube("root", (cx - 2.6, 2.6, -2.9), (5.2, 5.8, 5.8), mat="chrome")
        sh.cube("root", (cx - 2.8, 8.0, -3.1), (5.6, 1.0, 6.2), mat="chrome_dark")
        sh.cube("root", (cx - 2.3, 8.6, -3.8), (4.6, 2.8, 1.3), mat="chrome", polish=1.0)
        sh.cube("root", (cx - 0.8, 9.0, -4.3), (1.6, 1.6, 0.6), mat="quick_core", deco=("core",))
        sh.cube("root", (cx - 3.2, 0.0, -4.9), (6.4, 2.6, 8.4), mat="chrome_dark")          # the pooled boot: wider than the foot, rounded by a second slab
        sh.cube("root", (cx - 2.6, 0.0, -6.0), (5.2, 1.6, 1.4), mat="chrome")
        sh.cube("root", (cx - 2.9, 2.4, -4.2), (5.8, 0.8, 1.0), mat="chrome")
        sh.save(Anim())


def back():
    """A pair of eagle wings on the back (two articulated segments each) that flap, with a quicksilver clasp between the shoulder blades."""
    sh = Sheet(KEY, "back", 256, 160, seed=561)
    sh.bone("root", pivot=(0, 24, 0))
    sh.cube("root", (-3.0, 14.0, 2.2), (6.0, 9.0, 1.4), mat="chrome_dark")
    sh.cube("root", (-1.2, 18.4, 3.4), (2.4, 2.4, 0.8), mat="quick_core", deco=("core",))
    sh.cube("root", (-3.4, 21.6, 2.4), (6.8, 1.2, 1.6), mat="chrome")
    for sx in (1, -1):
        n = "l" if sx > 0 else "r"
        wing(sh, sx, "root", (2.4, 20.0, 3.4), "wi_" + n, "wo_" + n, scale=0.9)
    a = Anim()
    for sx in (1, -1):
        n = "l" if sx > 0 else "r"
        wing_anim(a, "idle", 3.0, "wi_" + n, "wo_" + n, sx, 28, 6)
        wing_anim(a, "flap", 0.6, "wi_" + n, "wo_" + n, sx, 20, 26)
    sh.save(a)


def halo():
    sh = Sheet(KEY, "halo", 128, 64, seed=571)
    sh.bone("root", pivot=(0, 36.0, 0))
    sh.bone("spin", "root", (0, 36.0, 0))
    for k in range(6):                                           # six drops orbit the head, two sizes
        a = 2 * math.pi * k / 6
        x, z = 6.0 * math.cos(a), 6.0 * math.sin(a)
        r = 0.9 if k % 2 == 0 else 0.65
        sh.cube("spin", (x - r, 35.4 + (k % 3) * 0.8 - r, z - r), (2 * r, 2 * r + 0.8, 2 * r), mat="quick_core" if k % 2 == 0 else "chrome", deco=("core",) if k % 2 == 0 else ("ripple",))
    sh.cube("spin", (-0.4, 36.0, -5.6), (0.8, 0.4, 11.2), mat="chrome", polish=1.0)
    sh.cube("spin", (-5.6, 36.0, -0.4), (11.2, 0.4, 0.8), mat="chrome", polish=1.0)
    a = Anim()
    wave(a, "idle", "spin", 6.0, 12, y=lambda p: 360 * p)
    wave(a, "idle", "root", 3.0, 12, kind="pos", y=S(0.5))
    sh.save(a)


# ================================================================================================================================ the constructs
def blade():
    """A Silver Blade: a floating double-edged sword of mercury, a flowing guard, drops that run down the fuller. Upright, point up (+y), about 34 px."""
    sh = Sheet(KEY, "blade", 96, 64, seed=581)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("sword", "root", (0, 14, 0))
    sh.bone("drop1", "sword", (0, 14, 0))
    sh.bone("drop2", "sword", (0, 14, 0))
    sh.cube("sword", (-1.0, 0.0, -1.0), (2.0, 5.0, 2.0), mat="chrome_dark")
    sh.cube("sword", (-1.6, -1.4, -1.6), (3.2, 1.6, 3.2), mat="chrome", polish=1.0)
    sh.cube("sword", (-4.6, 5.0, -1.2), (9.2, 1.4, 2.4), mat="chrome", polish=1.0)
    sh.cube("sword", (-5.4, 4.2, -1.0), (1.2, 3.0, 2.0), mat="chrome_dark")
    sh.cube("sword", (4.2, 4.2, -1.0), (1.2, 3.0, 2.0), mat="chrome_dark")
    for i, (w, h, y) in enumerate(((4.0, 8.0, 6.4), (3.6, 7.0, 14.4), (3.0, 5.0, 21.4), (2.2, 3.4, 26.4), (1.2, 2.0, 29.8))):
        sh.cube("sword", (-w / 2, y, -0.6), (w, h, 1.2), mat="chrome", polish=1.0)
    sh.cube("sword", (-0.4, 6.8, -0.9), (0.8, 20.0, 1.8), mat="quick_core", deco=("core",))
    sh.cube("drop1", (-0.9, 20.0, -1.2), (1.8, 2.2, 0.6), mat="quick_core", deco=("core",))
    sh.cube("drop2", (-0.7, 14.0, 0.7), (1.4, 1.8, 0.6), mat="quick_core", deco=("core",))
    a = Anim()
    wave(a, "idle", "sword", 3.0, 12, kind="pos", y=S(0.8))
    keys(a, "idle", "drop1", "pos", {0.0: (0, 0, 0), 1.5: (0, -14, 0), 1.51: (0, 8, 0), 3.0: (0, 0, 0)})
    keys(a, "idle", "drop1", "scale", {0.0: (1, 1, 1), 1.4: (1, 1, 1), 1.5: (0.2, 0.2, 0.2), 1.51: (0.2, 0.2, 0.2), 3.0: (1, 1, 1)})
    keys(a, "idle", "drop2", "pos", {0.0: (0, 0, 0), 1.5: (0, -9, 0), 3.0: (0, 0, 0)})
    a.clips["idle"]["animation_length"] = 3.0
    wave(a, "spin", "sword", 0.6, 12, y=lambda p: 360 * p)
    sh.save(a)


def eagle():
    """The Silver Eagle: a giant eagle of living mercury. Body 14 x 12 x 28 px, wings two segments each (span about 3.8 blocks at scale 1), a fanned tail, a hooked beak, talons."""
    sh = Sheet(KEY, "eagle", 384, 192, seed=591)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("body", "root", (0, 20, 0))
    sh.bone("neck", "body", (0, 24, -12))
    sh.bone("head", "neck", (0, 28, -16))
    sh.bone("tail", "body", (0, 21, 13))
    sh.bone("leg_l", "body", (3.5, 16, -2))
    sh.bone("leg_r", "body", (-3.5, 16, -2))
    sh.cube("body", (-6.0, 14.0, -13.0), (12.0, 12.0, 26.0), mat="chrome", polish=1.0)
    sh.cube("body", (-5.0, 12.4, -10.0), (10.0, 2.0, 18.0), mat="chrome_dark")
    sh.cube("body", (-5.6, 25.4, -11.0), (11.2, 1.4, 22.0), mat="chrome_dark")
    sh.cube("body", (-4.0, 19.0, -14.0), (8.0, 7.0, 2.0), mat="quick_core", deco=("core",))        # the breast glows
    sh.cube("neck", (-3.4, 23.0, -18.0), (6.8, 7.0, 7.0), mat="chrome")
    sh.cube("neck", (-3.0, 22.0, -13.0), (6.0, 6.0, 4.0), mat="chrome_dark")
    sh.cube("head", (-3.6, 26.0, -23.0), (7.2, 6.4, 7.0), mat="chrome", polish=1.0)
    sh.cube("head", (-1.8, 25.0, -28.4), (3.6, 3.4, 5.6), mat="chrome_dark")                       # the upper beak
    sh.cube("head", (-1.2, 23.0, -29.4), (2.4, 2.8, 2.4), mat="chrome", rot=(18, 0, 0), pv=(0, 25, -28))
    sh.cube("head", (-1.4, 24.0, -26.0), (2.8, 1.2, 4.0), mat="chrome_dark")
    sh.cube("head", (-3.9, 28.4, -22.4), (1.0, 1.6, 1.8), mat="quick_core", deco=("core",))
    sh.cube("head", (2.9, 28.4, -22.4), (1.0, 1.6, 1.8), mat="quick_core", deco=("core",))
    sh.cube("head", (-1.2, 31.8, -21.0), (2.4, 2.4, 5.0), mat="chrome", rot=(-12, 0, 0), pv=(0, 31.8, -17))
    for i in range(5):                                           # the fanned tail
        ang = (i - 2) * 11
        sh.cube("tail", (-1.6, 19.6, 13.0), (3.2, 0.8, 12.0 - abs(i - 2) * 1.2), mat="chrome" if i % 2 == 0 else "chrome_dark", rot=(0, ang, 0), pv=(0, 20, 13))
    for sx, bone in ((1, "leg_l"), (-1, "leg_r")):
        x = sx * 3.5
        sh.cube(bone, (x - 1.2, 8.0, -3.2), (2.4, 8.0, 2.4), mat="chrome_dark")
        for k in (-1, 0, 1):
            sh.cube(bone, (x + k * 1.2 - 0.4, 6.4, -5.6), (0.8, 0.8, 3.0), mat="chrome", rot=(0, k * 12, 0), pv=(x, 7, -3))
        sh.cube(bone, (x - 0.5, 6.0, -1.6), (1.0, 1.0, 2.6), mat="chrome", rot=(0, 0, 0))
    for sx in (1, -1):
        n = "l" if sx > 0 else "r"
        wing(sh, sx, "body", (5.0, 23.0, -2.0), "wi_" + n, "wo_" + n, scale=2.1, y=0, feathers=5)
    a = Anim()
    L = 3.0
    wave(a, "idle", "body", L, 12, kind="pos", y=S(0.8))
    wave(a, "idle", "head", L, 12, x=S(3.0, 0.2), y=S(6.0, 0.4))
    wave(a, "idle", "tail", L, 12, x=S(5.0, 0.1))
    for sx in (1, -1):
        n = "l" if sx > 0 else "r"
        wing_anim(a, "idle", L, "wi_" + n, "wo_" + n, sx, 12, 14)
        wing_anim(a, "flap", 0.9, "wi_" + n, "wo_" + n, sx, 8, 34)
        wing_anim(a, "glide", 3.0, "wi_" + n, "wo_" + n, sx, 4, 3)
        keys(a, "dive", "wi_" + n, "rot", {0.0: (0, 0, sx * -4), 0.5: (0, 0, sx * -24), 1.0: (0, 0, sx * -24)})
        keys(a, "dive", "wo_" + n, "rot", {0.0: (0, 0, sx * -4), 0.5: (0, 0, sx * -40), 1.0: (0, 0, sx * -40)})
    keys(a, "dive", "head", "rot", {0.0: (0, 0, 0), 0.5: (24, 0, 0), 1.0: (24, 0, 0)})
    keys(a, "dive", "body", "rot", {0.0: (0, 0, 0), 0.5: (-26, 0, 0), 1.0: (-26, 0, 0)})
    keys(a, "dive", "tail", "rot", {0.0: (0, 0, 0), 0.5: (20, 0, 0), 1.0: (20, 0, 0)})
    a.clips["dive"]["animation_length"] = 1.0
    keys(a, "form", "root", "scale", {0.0: (0.1, 0.1, 0.1), 0.4: (1.15, 1.0, 1.15), 0.6: (1, 1, 1)}, loop=False)
    a.clips["form"]["animation_length"] = 0.6
    sh.save(a)


def spear():
    """A Silver Spear: a hyper-dense chrome spear, a liquid tail of tapering drops behind it (+y is the point, the tail trails below)."""
    sh = Sheet(KEY, "spear", 96, 64, seed=601)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("spin", "root", (0, 20, 0))
    sh.cube("spin", (-1.0, 8.0, -1.0), (2.0, 22.0, 2.0), mat="chrome", polish=1.0)
    for i, (w, h, y) in enumerate(((2.6, 3.0, 29.0), (2.0, 3.0, 31.8), (1.4, 2.6, 34.2), (0.8, 2.2, 36.2))):
        sh.cube("spin", (-w / 2, y, -w / 2), (w, h, w), mat="chrome", polish=1.0)
    sh.cube("spin", (-1.6, 14.0, -1.6), (3.2, 1.0, 3.2), mat="chrome_dark")
    sh.cube("spin", (-0.5, 12.0, -0.5), (1.0, 18.0, 1.0), mat="quick_core", deco=("core",))
    for i, (w, h, y) in enumerate(((1.8, 3.0, 4.8), (1.4, 2.6, 1.8), (1.0, 2.0, -0.6), (0.6, 1.4, -2.4))):
        sh.cube("root", (-w / 2, y, -w / 2), (w, h, w), mat="quick_core", deco=("core",))
    a = Anim()
    wave(a, "idle", "spin", 0.8, 10, y=lambda p: 360 * p)
    sh.save(a)


def cage():
    """The Silver Star of Execution: a cage of eight tall mercury bars with arrow heads, a ring on top, four silver shield plates between the bars. Radius 24 px, height 44 px."""
    sh = Sheet(KEY, "cage", 192, 128, seed=611)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("bars", "root", (0, 0, 0))
    sh.bone("top", "root", (0, 44, 0))
    sh.bone("shields", "root", (0, 0, 0))
    R = 24.0
    for k in range(8):
        a = 2 * math.pi * k / 8
        x, z = R * math.cos(a), R * math.sin(a)
        sh.cube("bars", (x - 1.3, 0.0, z - 1.3), (2.6, 38.0, 2.6), mat="chrome", polish=1.0)
        sh.cube("bars", (x - 2.0, 0.0, z - 2.0), (4.0, 3.0, 4.0), mat="chrome_dark")
        for j, (w, h, y) in enumerate(((3.4, 2.4, 38.0), (2.4, 2.4, 40.4), (1.4, 2.2, 42.8))):
            sh.cube("bars", (x - w / 2, y, z - w / 2), (w, h, w), mat="chrome", polish=1.0)
        sh.cube("bars", (x - 0.4, 6.0, z - 0.4), (0.8, 30.0, 0.8), mat="quick_core", deco=("core",))
    # the ring on top: eight slabs between the bar heads
    for k in range(8):
        a = 2 * math.pi * (k + 0.5) / 8
        x, z = R * math.cos(a) * 0.96, R * math.sin(a) * 0.96
        sh.cube("top", (x - 9.4 / 2, 0.0, z - 1.0), (9.4, 1.4, 2.0), mat="chrome_dark", rot=(0, -math.degrees(a) + 90, 0), pv=(x, 0.7, z))
    for k in range(4):                                           # four silver shield plates between the bars
        a = 2 * math.pi * (k + 0.5) / 4
        x, z = R * 0.98 * math.cos(a), R * 0.98 * math.sin(a)
        sh.cube("shields", (x - 8.0, 6.0, z - 0.6), (16.0, 24.0, 1.2), mat="chrome", polish=1.0, rot=(0, -math.degrees(a) + 90, 0), pv=(x, 18.0, z))
        sh.cube("shields", (x - 2.0, 15.0, z - 1.2), (4.0, 4.0, 0.6), mat="quick_core", deco=("core",), rot=(0, -math.degrees(a) + 90, 0), pv=(x, 17.0, z))
    an = Anim()
    keys(an, "close", "bars", "scale", {0.0: (1, 0.03, 1), 0.5: (1, 1.1, 1), 0.7: (1, 1, 1)}, loop=False)
    keys(an, "close", "top", "pos", {0.0: (0, -43, 0), 0.5: (0, 3, 0), 0.7: (0, 0, 0)}, loop=False)
    keys(an, "close", "shields", "scale", {0.0: (0.1, 0.03, 0.1), 0.45: (0.1, 0.03, 0.1), 0.8: (1, 1, 1)}, loop=False)
    an.clips["close"]["animation_length"] = 0.8
    wave(an, "idle", "top", 4.0, 12, y=lambda p: 360 * p / 12.0)
    wave(an, "idle", "shields", 3.0, 12, kind="pos", y=S(0.5))
    keys(an, "open", "bars", "scale", {0.0: (1, 1, 1), 0.4: (1, 0.03, 1)}, loop=False)
    keys(an, "open", "shields", "scale", {0.0: (1, 1, 1), 0.3: (0.1, 0.03, 0.1)}, loop=False)
    keys(an, "open", "top", "pos", {0.0: (0, 0, 0), 0.4: (0, -43, 0)}, loop=False)
    an.clips["open"]["animation_length"] = 0.4
    sh.save(an)


def coat_skin():
    """The liquid coat laid over the posed player model: translucent chrome with ripples and a few cracks (textures/aura/mercury_coat.png), and the additive sheen bands (mercury_sheen.png)."""
    def coat(part, layer, face, W, H, rng):
        yy, xx = np.mgrid[0:H, 0:W]
        n = vnoise(H, W, rng, 3) if min(W, H) >= 2 else rng.random((H, W))
        t = 0.45 + 0.25 * np.sin((yy * 0.8 + n * 5.0) * 1.3) + 0.15 * (n - 0.5)
        rgb = ramp(MATS["chrome"]["pal"], t)
        crack = (np.abs(vnoise(H, W, rng, 2.2) - 0.5) < 0.025) if min(W, H) >= 2 else np.zeros((H, W), bool)
        rgb[crack] = (30, 40, 60)
        alpha = np.where(crack, 220, 150)
        return rgb, alpha, None

    def sheen(part, layer, face, W, H, rng):
        yy, xx = np.mgrid[0:H, 0:W]
        band = np.clip(np.sin((yy * 0.55 + xx * 0.25) * 1.6), 0, 1) ** 4
        gl = np.array(MATS["chrome"]["glow"], float)
        return np.zeros((H, W, 3)) + gl, (band * 255).astype(int), None
    skin_texture("aura/mercury_coat", coat, seed=21)
    skin_texture("aura/mercury_sheen", sheen, seed=22)


if __name__ == "__main__":
    for fn in (helm, chest, tasset, pauldrons, vambraces, greaves, back, halo, blade, eagle, spear, cage, coat_skin):
        fn()

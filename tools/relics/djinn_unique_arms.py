"""djinn_unique_arms: the body armour of the twelve Djinn (chest emblem, skirt, pauldron, forearm and greave adornments) and their Half Equip WEAPONS.

Body / arm / leg pieces are authored in the frame of the model part they are drawn on (see djinn_parts.py). The WEAPON is authored in the held item's
own space: the grip at the origin, the blade along +y (item units are px = 1/16 block, the hand's display transform scales it by 0.85), +x across, z
through. It is the one piece that is NOT flipped into model space when exported.
"""
import math

import numpy as np

import djinn_unique as du
import djinn_unique_heads as duh
import djinn_unique_back as dub
from djinn_unique import (ml, C, S, curve, collar, sweep_tube, leaf, spike, feather, tongue, zig, orb, disc, crescent, ring_path, sweep_flat, tube, pf_flame,
                          pf_feather, pf_blade, pf_taper, pf_ribbon, pf_flat, UP, unit, arc_path)
from djinn_unique_heads import blade3
import djinn_parts as dp

FAMILY = {"BAAL": "bolt", "BIFRON": "bolt", "AMON": "flame", "MARCHOSIAS": "flame", "VINEA": "wave", "VASSAGO": "wave", "FOCALOR": "feather", "PAIMON": "feather",
          "SITRI": "ray", "ZAGAN": "thorn", "ZEPAR": "plate", "FURFUR": "bell"}
CHEST = {"BAAL": "cuirass", "ZEPAR": "cuirass", "BIFRON": "cuirass", "VASSAGO": "cuirass", "MARCHOSIAS": "cuirass",
         "AMON": "harness", "VINEA": "harness", "FOCALOR": "harness", "ZAGAN": "harness", "FURFUR": "harness", "SITRI": "harness", "PAIMON": "harness"}
SKIRT = {"AMON": 20.0, "VINEA": 22.0, "FOCALOR": 18.0, "SITRI": 22.0, "PAIMON": 20.0, "FURFUR": 16.0, "ZAGAN": 14.0}


# ============================================================================================ chest
def chest_harness(k):
    """An open harness: a gorget, two pectoral plates, two crossing straps with a ring, a belly plate and a spine bar (the chest bare between)."""
    collar(k.part("chest", "gorget", k.trim()), -0.2, 3.2, 1.9, lip=0.35, sides=S(14), oval=0.85)
    for sgn, nm in ((-1, "r"), (1, "l")):
        pl = k.part("chest", "pec_" + nm, "plate", smooth=40)
        ml.loft(pl, "-y", (sgn * 2.2, -0.8, 2.6), [(0, 0.9, 0.9), (2.4, 1.15, 1.0), (4.2, 0.6, 0.7)], ml.lozenge(3.8, 1.2), up=(0, 0, 1))
        du.bar(k.part("chest", "pec_rune_" + nm, "crystal", layer=2), (sgn * 1.4, -1.4, 3.3), (sgn * 3.2, -3.4, 3.1), 0.32, 0.1, up=(0, 0, 1), taper=0.5)
    for sgn, nm in ((-1, "r"), (1, "l")):
        du.bar(k.part("chest", "strap_" + nm, "cloth"), (sgn * 3.6, 0.2, 2.5), (-sgn * 3.4, -10.5, 2.5), 1.3, 0.35, up=(0, 0, 1), taper=0.6, section="rect")
        du.bar(k.part("chest", "strap_back_" + nm, "cloth"), (sgn * 3.6, 0.2, -2.55), (-sgn * 3.4, -10.5, -2.55), 1.3, 0.35, up=(0, 0, -1), taper=0.6, section="rect")
    collar(k.part("chest", "ring", k.trim()), 3.05, 1.5, 0.5, lip=0.14, sides=S(12), axis="z", origin=(0, -4.6, 0))
    ml.loft(k.part("chest", "belly", "plate"), "-y", (0, -6.8, 2.7), [(0, 1.0, 1.0), (2.6, 0.9, 1.0), (3.8, 0.3, 0.8)], ml.lozenge(4.8, 0.9), up=(0, 0, 1))
    du.bar(k.part("chest", "spine", k.trim()), (0, -0.4, -2.95), (0, -9.2, -2.75), 1.0, 0.35, up=(0, 0, -1), taper=1.2)
    du.gem(k.part("chest", "socket_gem", "crystal", layer=2, smooth=0), (0, -3.0, -3.3), 0.8, long=1.0, sides=6, axis="-z")


def emblem(k, name):
    cx, cy, cz = 0.0, -4.6 if CHEST[name] == "harness" else -2.6, 3.25
    g = lambda r=0.9, ax="z": du.gem(k.part("chest", "emblem_gem", "crystal", layer=2, smooth=0), (cx, cy, cz), r, long=1.2, sides=6, axis=ax)
    if name in ("BAAL", "BIFRON"):
        zig(k.part("chest", "bolt", "crystal", layer=2), [(0.8, cy + 3.2, cz), (-0.7, cy + 1.2, cz), (0.9, cy - 0.4, cz), (-0.8, cy - 2.6, cz)], 1.1, 0.35)
    elif name in ("AMON", "MARCHOSIAS"):
        for i in range(7 if name == "AMON" else 5):
            a = i * 2 * math.pi / (7 if name == "AMON" else 5)
            tg = k.part("chest", f"flame_{i}", "crystal", layer=2, smooth=45)
            tongue(tg, (cx + math.cos(a) * 1.2, cy + math.sin(a) * 1.2, cz), (math.cos(a), math.sin(a), 0.1), 2.6, 1.3, 0.4, 0.4, i, n=3)
        g(0.8)
    elif name in ("VINEA", "VASSAGO"):
        for i in range(7):
            a = math.radians(-60 + i * 20)
            du.bar(k.part("chest", f"rib_{i}", "trim"), (cx, cy - 1.6, cz), (cx + math.sin(a) * 3.0, cy - 1.6 + math.cos(a) * 3.0, cz), 0.4, 0.22, up=(0, 0, 1), taper=0.5)
        du.gem(k.part("chest", "emblem_gem", "crystal", layer=2, smooth=0), (cx, cy - 1.6, cz), 0.8, long=1.2, sides=6, axis="z")
    elif name in ("FOCALOR",):
        path = [(cx + 0.45 * t * math.cos(t * 1.6), cy + 0.45 * t * math.sin(t * 1.6), cz) for t in np.linspace(0.5, 5.4, 9)]
        sweep_tube(k.part("chest", "swirl", k.trim()), path, 0.35, 0.2, 5)
        g(0.7)
    elif name == "ZAGAN":
        for i in range(6):
            a = i * math.pi / 3
            blade3(k.part("chest", f"petal_{i}", "cloth", smooth=45), (cx + math.cos(a) * 0.5, cy + math.sin(a) * 0.5, cz), (math.cos(a), math.sin(a), 0.15), (math.cos(a), math.sin(a), 0.1), 2.4, 1.4, 0.35, wdir=(-math.sin(a), math.cos(a), 0), n=3, prof=pf_blade)
        g(0.7)
    elif name == "ZEPAR":
        du.box(k.part("chest", "fist", "extra"), (cx - 1.7, cy - 1.5, cz - 0.3), (cx + 1.7, cy + 1.5, cz + 0.8), chamfer=0.4)
        for j, x in enumerate((-1.2, -0.4, 0.4, 1.2)):
            du.stud(k.part("chest", f"knuckle_{j}", k.trim()), (cx + x, cy + 1.5, cz + 0.4), 0.45, 0.45, (0, 1, 0), 6)
        g(0.6)
    elif name == "FURFUR":
        for j, r in enumerate((2.5, 1.7)):
            collar(k.part("chest", f"ring_{j}", k.trim()), cz - 0.1, r, 0.3, lip=0.08, sides=S(14), axis="z", origin=(cx, cy, 0))
        g(0.8)
    elif name == "SITRI":
        disc(k.part("chest", "sun", k.trim()), (cx, cy, cz), 1.7, 0.5, "z", 12)
        for i in range(10):
            a = i * math.pi / 5
            spike(k.part("chest", f"ray_{i}", "crystal", glow=(i % 2 == 0)), (cx + math.cos(a) * 1.6, cy + math.sin(a) * 1.6, cz), (math.cos(a), math.sin(a), 0.1), 1.8 + (i % 2) * 0.8, 0.28, 5)
        g(0.8)
    elif name == "PAIMON":
        crescent(k.part("chest", "moon", k.trim()), (cx, cy, cz), 2.0, 1.2, 0.45, 180, 360, "xy", 8)
        g(0.7)
    else:
        g()


def build_skirt(k, name):
    """A long front and back skirt of cloth panels from the belt down (the robes of the reference), hem in the trim."""
    L = SKIRT[name]
    for i, x in enumerate((-2.9, 0.0, 2.9)):
        p = k.part("belt", f"skirt_f{i}", "cloth", uv_mode="loft")
        du.sweep(p, curve([(x, -10.6, 3.0), (x * 1.15, -10.6 - L * 0.5, 3.4), (x * 1.3, -10.6 - L, 3.0)], 5), ml.rect_ring(1.0, 0.25), scales=lambda t: (3.0 * (1 + 0.15 * t), 1.0))
    p = k.part("belt", "skirt_back", "cloth", uv_mode="loft")
    du.sweep(p, curve([(0, -10.6, -3.0), (0, -10.6 - L * 0.5, -3.6), (0, -10.6 - L, -3.2)], 5), ml.rect_ring(1.0, 0.25), scales=lambda t: (8.0 * (1 + 0.2 * t), 1.0))
    for sgn, nm in ((-1, "r"), (1, "l")):
        p = k.part("belt", "skirt_side_" + nm, "cloth", uv_mode="loft")
        du.sweep(p, curve([(sgn * 4.6, -10.6, 0.0), (sgn * 5.2, -10.6 - L * 0.5, 0.0), (sgn * 5.4, -10.6 - L * 0.9, 0.0)], 5), ml.rect_ring(1.0, 0.25), scales=lambda t: (4.0 * (1 + 0.1 * t), 1.0))
    du.sweep(k.part("belt", "hem", k.trim()), [(-3.8, -10.6 - L, 3.1), (0, -10.6 - L, 3.4), (3.8, -10.6 - L, 3.1)], ml.rect_ring(0.8, 0.5))


def build_chest(k, name):
    if CHEST[name] == "cuirass":
        chest_cuirass(k)
    else:
        chest_harness(k)
    emblem(k, name)


def build_belt(k, name):
    belt_light(k)
    if name in SKIRT:
        build_skirt(k, name)


# ============================================================================================ lighter base pieces (about half the quads of djinn_parts')
def chest_cuirass(k):
    """The cuirass of the heavy Djinn: a barrel plate, a gorget, two ab bands, the core gem in a cradle, a spine and shoulder-blade plates."""
    ml.loft(k.part("chest", "plate"), "-y", (0, 0.5, 0), [(0, 1.0, 1.0), (1.8, 1.04, 1.05), (4.5, 1.02, 1.08), (7.5, 0.9, 0.98), (10.0, 0.84, 0.92)], ml.ellipse(4.55, 2.75, S(16)), caps=(False, False))
    collar(k.part("chest", "gorget", k.trim()), -0.2, 3.2, 1.9, lip=0.35, sides=S(14), oval=0.85)
    for i, y in enumerate((-5.2, -8.2)):
        collar(k.part("chest", f"band_{i}", k.trim()), y, 4.2 - i * 0.18, 0.55, lip=0.15, sides=S(16), oval=0.66)
    du.gem(k.part("chest", "core", "crystal", layer=2, smooth=0), (0, -2.6, 3.15), 1.05, long=1.3, sides=5, axis="z")
    collar(k.part("chest", "cradle", k.trim()), 3.0, 1.55, 0.5, lip=0.15, sides=S(12), axis="z", origin=(0, -2.6, 0), oval=1.0)
    for sgn, nm in ((-1, "r"), (1, "l")):
        du.bar(k.part("chest", "rune_" + nm, "crystal", layer=2), (sgn * 0.6, -0.9, 3.0), (sgn * 3.6, -2.4, 2.8), 0.44, 0.22, up=(0, 0, 1))
        ml.loft(k.part("chest", "blade_" + nm, "plate"), "-y", (sgn * 2.3, -0.8, -2.75), [(0, 1.0, 1.0), (3.2, 1.2, 1.0), (6.0, 0.4, 0.6)], ml.lozenge(2.6, 0.7))
    du.bar(k.part("chest", "spine", k.trim()), (0, -0.4, -2.95), (0, -9.2, -2.75), 1.0, 0.35, up=(0, 0, -1), taper=1.2)
    du.gem(k.part("chest", "socket_gem", "crystal", layer=2, smooth=0), (0, -2.6, -3.5), 0.8, long=1.0, sides=5, axis="-z")


def belt_light(k):
    ml.loft(k.part("belt", "band", k.trim()), "-y", (0, -9.4, 0), [(0, 1.0, 1.0), (1.9, 1.0, 1.0)], ml.ellipse(4.5, 2.75, S(16)), caps=(False, False))
    du.gem(k.part("belt", "buckle", "crystal", layer=2, smooth=0), (0, -10.4, 3.0), 1.1, long=1.0, sides=5, axis="z")
    collar(k.part("belt", "buckle_ring", "plate"), 2.8, 1.7, 0.4, lip=0.12, sides=S(12), axis="z", origin=(0, -10.4, 0))
    for i, x in enumerate((-2.9, 0.0, 2.9)):
        L = 5.4 if i == 1 else 4.5
        ml.loft(k.part("belt", f"tasset_{i}", "plate"), "-y", (x, -11.2, 2.95), [(0, 1.0, 1.0), (L * 0.6, 0.92, 1.0), (L, 0.15, 0.8)], ml.lozenge(2.6, 0.55), up=(0, 0, 1))
    du.bar(k.part("belt", "tasset_rune", "crystal", layer=2), (0, -11.6, 3.28), (0, -11.2 - 5.4 * 0.7, 3.2), 0.4, 0.14, up=(0, 0, 1), taper=0.6)
    for sgn, nm in ((-1, "r"), (1, "l")):
        ml.loft(k.part("belt", "hip_" + nm, "plate"), "-y", (sgn * 4.7, -10.6, 0.0), [(0, 1.0, 1.0), (4.0, 0.8, 1.0), (5.2, 0.1, 0.8)], ml.lozenge(0.6, 3.6), up=(1, 0, 0))
    ml.loft(k.part("belt", "rear", "cloth"), "-y", (0, -10.8, -2.95), [(0, 1.0, 1.0), (5.0, 1.1, 1.0), (7.0, 0.5, 0.8)], ml.rect_ring(4.4, 0.4))


def vambrace_light(k):
    for sgn, piece in ((-1, "vambrace_r"), (1, "vambrace_l")):
        cx = sgn * 1.0
        ml.loft(k.part(piece, "tube"), "-y", (cx, -3.0, 0), [(0, 1.0, 1.0), (2.8, 0.95, 0.95), (6.2, 0.82, 0.82)], C(2.75, S(12)), caps=(False, False))
        for i, y in enumerate((-3.0, -8.4)):
            collar(k.part(piece, f"cuff_{i}", k.trim()), y, 2.95 - i * 0.3, 0.7, lip=0.2, sides=S(12), origin=(cx, 0, 0))
        du.bar(k.part(piece, "rune", "crystal", layer=2), (cx, -3.8, 2.7), (cx, -8.0, 2.35), 0.44, 0.22, up=(0, 0, 1))
        du.bar(k.part(piece, "ridge", k.trim()), (cx, -3.4, -2.6), (cx, -8.4, -2.3), 0.9, 0.3, up=(0, 0, -1), taper=1.0)


def gauntlet_light(k):
    for sgn, piece in ((-1, "gauntlet_r"), (1, "gauntlet_l")):
        cx = sgn * 1.0
        ml.loft(k.part(piece, "palm"), "-y", (cx, -9.0, 0), [(0, 1.0, 1.0), (1.6, 1.02, 1.02), (3.2, 0.92, 0.88)], ml.octagon(4.4, 4.4, 0.9), caps=(False, True))
        du.box(k.part(piece, "knuckle", k.trim()), (cx - 2.1, -11.7, 2.0), (cx + 2.1, -10.7, 2.7), chamfer=0.25)
        for j, x in enumerate((-1.2, 1.2)):
            du.stud(k.part(piece, f"stud_{j}", k.trim()), (cx + x, -11.2, 2.7), 0.4, 0.45, (0, 0, 1), 5)
        for j, x in enumerate((-1.2, 0.0, 1.2)):
            spike(k.part(piece, f"claw_{j}", "crystal", layer=2), (cx + x, -11.9, 1.0), (0, -1.0, 0.5), 2.6, 0.5, 4)
        du.box(k.part(piece, "back", "plate"), (cx - 1.9, -10.3, -2.5), (cx + 1.9, -9.2, -1.9), chamfer=0.0)


def greave_light(k):
    for sgn, piece in ((-1, "greave_r"), (1, "greave_l")):
        ml.loft(k.part(piece, "cuisse"), "-y", (0, -0.6, 0), [(0, 1.0, 1.0), (2.6, 1.0, 1.0), (5.2, 0.93, 0.93)], C(2.65, S(12)), caps=(False, False))
        ml.lathe(k.part(piece, "knee", k.trim()), "z", (0, -6.3, 2.0), [(0.0, 1.5), (0.35, 1.7), (0.9, 1.25), (1.3, 0.0)], sides=S(10))
        ml.loft(k.part(piece, "shin"), "-y", (0, -6.6, 0), [(0, 1.0, 1.0), (2.2, 0.95, 0.92), (4.6, 0.86, 0.84)], C(2.5, S(12)), caps=(False, False))
        ml.loft(k.part(piece, "boot", "dark"), "-y", (0, -10.5, 0), [(0, 1.0, 1.0), (1.6, 1.08, 1.12)], ml.octagon(4.5, 4.7, 0.9), caps=(False, True))
        du.box(k.part(piece, "toe", k.trim()), (-2.0, -12.0, 2.2), (2.0, -10.8, 3.3), chamfer=0.0)
        collar(k.part(piece, "band_0", k.trim()), -6.2, 2.75, 0.5, lip=0.14, sides=S(12))
        du.bar(k.part(piece, "rune", "crystal", layer=2), (0, -7.2, 2.4), (0, -10.2, 2.25), 0.4, 0.2, up=(0, 0, 1))


# ============================================================================================ pauldrons
def dome_base(k, piece, sgn, R=4.45, mat="plate", rims=1):
    cx = sgn * 1.0
    ml.lathe(k.part(piece, "dome", mat), "y", (cx + sgn * 0.5, 0, 0), [(-1.0, R - 0.55), (0.2, R), (1.5, R - 0.25), (2.7, R - 1.15), (3.6, R - 2.65), (4.1, 0)], sides=S(14), caps=(False, True))
    for i in range(rims):
        collar(k.part(piece, f"rim_{i}", k.trim()), -1.2 - 1.1 * i, R - 0.15 - 0.3 * i, 0.9, lip=0.25, sides=S(14), origin=(cx + sgn * 0.5, 0, 0))
    du.gem(k.part(piece, "stud", "crystal", layer=2, smooth=0), (cx + sgn * 2.4, 2.9, 0), 0.7, long=1.0, sides=6, axis=(sgn, 1, 0))
    return cx


def pauldron_for(k, name):
    for sgn, piece in ((-1, "pauldron_r"), (1, "pauldron_l")):
        R = 5.4 if name == "ZEPAR" else 4.45
        cx = dome_base(k, piece, sgn, R, "extra" if name in ("ZEPAR", "VASSAGO") else "plate", 2 if name in ("VINEA", "FURFUR") else 1)
        bx = cx + sgn * 2.8
        if name in ("BAAL", "BIFRON"):
            for j, (L, a) in enumerate(((9.0, 55), (6.6, 38), (4.6, 22))):
                r = math.radians(a)
                blade3(k.part(piece, f"fin_{j}", "plate", smooth=45), (bx, 2.4 + j * 0.2, -0.4 - 0.5 * j), (sgn * math.cos(r) * 0.7, math.sin(r), -0.2), (sgn * 0.2, 1.0, -0.5), L, 2.4 - 0.3 * j, 0.5, wdir=(0, 0, 1), n=4)
            zig(k.part(piece, "bolt", "crystal", layer=2), [(bx + sgn * 0.3, 3.0, 0.4), (bx + sgn * 1.4, 5.2, 0.4), (bx + sgn * 0.5, 6.6, 0.4), (bx + sgn * 1.6, 9.6, 0.4)], 0.6, 0.25)
        elif name in ("AMON",):
            for j in range(5):
                tg = k.part(piece, f"flame_{j}", "crystal", layer=2, smooth=45)
                tongue(tg, (cx + sgn * (1.0 + 1.0 * j), 3.0, -0.3 + 0.2 * j), (sgn * 0.25, 1.0, 0.0), 7.0 - abs(j - 2) * 1.4, 2.0, 0.4, 0.9, j * 1.1, n=4)
        elif name == "VINEA":
            for j, (L, c) in enumerate(((8.0, 70), (6.0, 55), (4.2, 40))):
                p = k.part(piece, f"crest_{j}", "cloth", smooth=45)
                feather(p, (bx, 2.0 + j * 0.6, 0.2 * j), 40 - j * 8, L, 2.8 - 0.4 * j, 0.5, c, "xy", 5, pf_blade, sx=sgn)
            orb(k.part(piece, "pearl", "bone", smooth=0), (bx + sgn * 0.6, 3.4, 1.2), 0.9, sides=7)
        elif name in ("FOCALOR", "PAIMON"):
            for j in range(5 if name == "FOCALOR" else 4):
                a = 78 - j * 17
                blade3(k.part(piece, f"fea_{j}", "plate", smooth=45), (bx - sgn * 0.5, 3.0, 0.0), (sgn * math.cos(math.radians(a)), math.sin(math.radians(a)), -0.1), (sgn * math.cos(math.radians(a)) * 0.5, math.sin(math.radians(a)), -0.5), 8.0 - j * 0.6, 1.7, 0.4, wdir=(0, 0, 1), n=4)
            sweep_tube(k.part(piece, "quill", k.trim()), [(bx - sgn * 0.8, 2.9, 0.0), (bx + sgn * 0.8, 3.7, 0.0)], 0.4, 0.3, 5)
        elif name == "ZAGAN":
            for j in range(3):
                spike(k.part(piece, f"thorn_{j}", k.trim()), (bx + sgn * 0.3 * j, 2.4 + j * 0.8, -0.2 * j), (sgn * (1.0 - 0.25 * j), 0.8 + 0.4 * j, 0.0), 4.4 - 0.7 * j, 0.9, 6)
            lf = k.part(piece, "leaf", "cloth")
            P = [(bx, 2.8, 0.6), (bx + sgn * 2.4, 4.4, 1.0), (bx + sgn * 1.0, 7.0, 0.9), (bx - sgn * 0.6, 5.2, 0.7)]
            leaf(lf, P if sgn > 0 else P[::-1])
        elif name == "ZEPAR":
            for j, (dx, dy, L) in enumerate(((3.6, 1.8, 4.2), (3.0, 3.6, 3.2), (3.8, 0.2, 3.0), (1.6, 4.2, 2.8))):
                spike(k.part(piece, f"rock_{j}", "extra"), (cx + sgn * 2.9, 1.0 + dy * 0.3, 0.3 * (j - 1.5)), (sgn * dx, dy, -0.3 + 0.2 * j), L, 1.2 - 0.12 * j, 6)
            for j in range(3):
                du.stud(k.part(piece, f"rivet_{j}", k.trim()), (cx + sgn * (1.5 + 1.4 * j), 3.4 - 0.6 * j, 3.2 - 0.9 * j), 0.5, 0.45, (0, 1, 0.4), 6)
        elif name == "FURFUR":
            ml.lathe(k.part(piece, "cone", "extra"), "y", (cx + sgn * 0.5, 3.4, 0), [(0.0, 1.0), (1.0, 2.0), (2.2, 3.0), (3.0, 3.4)], sides=S(14), caps=(False, False))
            for j, r in enumerate((3.0, 1.9)):
                collar(k.part(piece, f"ring_{j}", k.trim()), 5.6, r, 0.25, lip=0.06, sides=S(14), origin=(cx + sgn * 0.5, 0, 0))
        elif name == "MARCHOSIAS":
            for j in range(3):
                p = k.part(piece, f"fang_{j}", "bone")
                feather(p, (bx - sgn * 0.5, 2.0 + 0.6 * j, 0.5 * (j - 1)), 60 - 18 * j, 6.0 - j, 1.5, 0.9, 25, "xy", 4, pf_blade, sx=sgn)
            for j in range(5):
                spike(k.part(piece, f"fur_{j}", "cloth"), (cx - sgn * 1.0 + sgn * 0.9 * j, 3.6, -1.2), (sgn * 0.2, 1.0, -0.4), 2.6, 0.8, 5)
        elif name == "SITRI":
            for j, a in enumerate(np.linspace(20, 100, 7)):
                r = math.radians(a)
                spike(k.part(piece, f"ray_{j}", "crystal", glow=(j % 2 == 0)), (cx + sgn * 0.5 + sgn * math.cos(r) * 3.4, 1.0 + math.sin(r) * 3.4, 0), (sgn * math.cos(r), math.sin(r), 0.0), 3.4 if j % 2 == 0 else 2.2, 0.5, 5)
        elif name == "VASSAGO":
            for j, a in enumerate((-40, -15, 15, 40)):
                r = math.radians(a)
                spike(k.part(piece, f"tooth_{j}", "bone"), (cx + sgn * 0.5 + sgn * 3.0, 0.4 - j * 0.0, math.sin(r) * 3.2), (sgn * 0.8, -0.8, math.sin(r) * 0.4), 2.2, 0.6, 5)
            du.gem(k.part(piece, "eye", "crystal", layer=2, smooth=0), (cx + sgn * 3.6, 1.8, 0.0), 0.9, long=1.1, sides=6, axis=(sgn, 0.3, 0))
            sweep_tube(k.part(piece, "tentacle", "cloth"), [(cx, 1.6, -3.4), (cx + sgn * 2.4, 2.8, -4.4), (cx + sgn * 4.0, 0.4, -4.8), (cx + sgn * 3.6, -2.4, -4.0)], 0.8, 0.12, 6)


# ============================================================================================ forearms and greaves
def forearm_extra(k, name):
    fam = FAMILY[name]
    for sgn, piece in ((-1, "vambrace_r"), (1, "vambrace_l")):
        cx = sgn * 1.0
        ox = cx + sgn * 2.7
        if fam == "bolt":
            zig(k.part(piece, "bolt", "crystal", layer=2), [(ox, -3.2, 0.0), (ox + sgn * 0.8, -5.2, -0.3), (ox - sgn * 0.2, -6.6, -0.3), (ox + sgn * 0.8, -9.2, -0.5)], 0.7, 0.28)
        elif fam == "flame":
            for j in range(3):
                tg = k.part(piece, f"flame_{j}", "crystal", layer=2, smooth=45)
                tongue(tg, (ox, -3.4 - 2.0 * j, 0.0), (sgn * 0.8, -0.4, 0.0), 4.2 - 0.6 * j, 1.5, 0.4, 0.5, j, n=3)
        elif fam == "wave":
            for j in range(2):
                p = k.part(piece, f"fin_{j}", "cloth", smooth=45)
                feather(p, (ox, -3.6 - 3.0 * j, 0.0), -70 + 10 * j, 4.6, 2.2, 0.4, 30, "xy", 4, pf_blade, sx=sgn)
            orb(k.part(piece, "bubble", "bone", smooth=0), (ox + sgn * 0.8, -3.2, 1.0), 0.7, sides=6)
        elif fam == "feather":
            for j in range(3):
                blade3(k.part(piece, f"fea_{j}", "plate", smooth=45), (ox, -3.2 - 1.8 * j, 0.0), (sgn * 0.6, -0.8, 0.0), (sgn * 0.3, -1.0, -0.3), 5.6 - j * 0.6, 1.4, 0.35, wdir=(0, 0, 1), n=3)
        elif fam == "ray":
            for j, a in enumerate((-20, 0, 20)):
                r = math.radians(a)
                spike(k.part(piece, f"ray_{j}", "crystal", layer=2), (ox, -5.0, 0.0), (sgn * math.cos(r), -0.4 + math.sin(r) * 0.3, math.sin(r) * 0.5), 3.4 - 0.5 * abs(j - 1), 0.45, 5)
        elif fam == "thorn":
            for j in range(3):
                spike(k.part(piece, f"thorn_{j}", k.trim()), (ox, -3.4 - 2.0 * j, 0.0), (sgn * 1.0, -0.2, 0.2 * (j - 1)), 2.4, 0.5, 5)
            lf = k.part(piece, "leaf", "cloth")
            P = [(ox, -3.4, 0.4), (ox + sgn * 2.2, -4.4, 0.6), (ox + sgn * 1.8, -7.0, 0.4), (ox, -6.0, 0.2)]
            leaf(lf, P if sgn > 0 else P[::-1])
        elif fam == "plate":
            du.box(k.part(piece, "slab", "extra"), (cx - 2.9, -3.6, -2.9), (cx + 2.9, -7.6, 2.9), chamfer=0.7)
            for j in range(3):
                du.stud(k.part(piece, f"rivet_{j}", k.trim()), (cx - 1.6 + 1.6 * j, -5.6, 2.9), 0.45, 0.4, (0, 0, 1), 6)
        elif fam == "bell":
            ml.loft(k.part(piece, "bell", k.trim()), "-y", (ox, -3.2, 0.0), [(0, 0.4, 0.4), (1.2, 1.0, 1.0), (2.4, 1.2, 1.2)], C(1.0, S(8)), caps=(False, True))
            sweep_flat(k.part(piece, "ribbon", "cloth", smooth=45), [(ox, -5.6, 0.0), (ox + sgn * 1.4, -8.0, -0.3), (ox + sgn * 1.0, -11.0, -0.6)], 1.4, 0.25, pf_ribbon, (0, 0, 1))


def greave_extra(k, name):
    fam = FAMILY[name]
    for sgn, piece in ((-1, "greave_r"), (1, "greave_l")):
        ox = sgn * 2.6
        if fam == "bolt":
            zig(k.part(piece, "bolt", "crystal", layer=2), [(ox, -6.8, 0.0), (ox + sgn * 0.8, -8.4, 0.0), (ox - sgn * 0.2, -9.6, 0.0), (ox + sgn * 0.6, -11.6, 0.0)], 0.6, 0.25)
        elif fam == "flame":
            for j in range(3):
                tg = k.part(piece, f"flame_{j}", "crystal", layer=2, smooth=45)
                tongue(tg, (ox, -9.6 + 0.6 * j, -0.6 * j + 0.3), (sgn * 0.5, 0.8, 0.0), 4.4 - 0.8 * j, 1.6, 0.4, 0.5, j, n=3)
        elif fam == "wave":
            p = k.part(piece, "fin", "cloth", smooth=45)
            feather(p, (ox, -6.8, 0.0), -60, 6.0, 2.6, 0.4, 25, "xy", 4, pf_blade, sx=sgn)
        elif fam == "feather":
            for j in range(3):
                blade3(k.part(piece, f"fea_{j}", "plate", smooth=45), (ox, -7.0 - 1.4 * j, -0.2), (sgn * 0.6, -0.8, -0.3), (sgn * 0.3, -1.0, -0.5), 5.2 - j * 0.5, 1.3, 0.35, wdir=(0, 0, 1), n=3)
        elif fam == "ray":
            for j in range(3):
                spike(k.part(piece, f"ray_{j}", "crystal", layer=2), (ox, -7.0 - 1.4 * j, 0.0), (sgn * 1.0, -0.3 - 0.2 * j, 0.0), 2.6, 0.4, 5)
        elif fam == "thorn":
            for j in range(2):
                spike(k.part(piece, f"thorn_{j}", k.trim()), (ox, -7.2 - 2.0 * j, 0.0), (sgn * 1.0, -0.2, -0.3), 2.6, 0.55, 5)
        elif fam == "plate":
            du.box(k.part(piece, "shin", "extra"), (-2.9, -7.2, 1.8), (2.9, -10.0, 3.6), chamfer=0.6)
        elif fam == "bell":
            ml.loft(k.part(piece, "bell", k.trim()), "-y", (ox, -7.0, 0.0), [(0, 0.4, 0.4), (1.2, 0.9, 0.9), (2.2, 1.1, 1.1)], C(1.0, S(8)), caps=(False, True))


# ============================================================================================ WEAPONS (item space: grip at the origin, blade along +y, units px)
def w_shaft(k, y0, y1, r=0.65, mat="extra", name="shaft", sides=8):
    ml.loft(k.part("weapon", name, mat), "y", (0, y0, 0), [(0, r, r), (y1 - y0, r, r)], ml.octagon(2.0, 2.0, 0.5), caps=(True, True))


def w_ring(k, y, r, name, mat="trim", h=0.7):
    collar(k.part("weapon", name, mat), y, r, h, lip=0.2, sides=S(10), axis="y")


def spear_blade(k, y0, L, w, t, name="blade", mat="plate", fuller=True):
    """A leaf-shaped spearhead: bevelled section, broad at a third of its length, to a point; a fuller down the middle."""
    ml.loft(k.part("weapon", name, mat, smooth=45), "y", (0, y0, 0), [(0, 0.35, 1.0), (L * 0.12, 0.8, 1.0), (L * 0.36, 1.0, 1.0), (L * 0.72, 0.55, 0.8), (L, 0.0, 0.0)], ml.bevelled(w, t, 0.4), up=(0, 0, 1))
    if fuller:
        ml.ridge_pair(k.part("weapon", name + "_fuller", "trim"), (0, y0 + L * 0.12, t * 0.5), (0, y0 + L * 0.8, t * 0.4), (0, 0, 1), 0.7, 0.2, 0.1, 1.5)
        ml.ridge_pair(k.part("weapon", name + "_fuller_b", "trim"), (0, y0 + L * 0.12, -t * 0.5), (0, y0 + L * 0.8, -t * 0.4), (0, 0, -1), 0.7, 0.2, 0.1, 1.5)


def weapon_baal(k):
    """BAAL_THUNDERLANCE: a 56 px spear (measured on ref 59 tile 1: the lance reaches from the hand to above the head), a leaf head with two lightning prongs."""
    w_shaft(k, -9, 29, 0.7, "plate", "shaft")
    for j, y in enumerate((-4, 1, 6)):
        w_ring(k, y, 1.05, f"wrap_{j}", "cloth", 0.9)
    orb(k.part("weapon", "pommel", "trim", smooth=40), (0, -10.2, 0), 1.3, sides=8)
    du.gem(k.part("weapon", "pommel_gem", "crystal", layer=2, smooth=0), (0, -11.8, 0), 0.7, long=1.0, sides=6, axis="-y")
    w_ring(k, 26.5, 1.45, "collar", "trim", 1.4)
    w_ring(k, 29.5, 1.1, "collar2", "plate", 0.8)
    spear_blade(k, 29.0, 19.0, 4.6, 0.9)
    for sgn, nm in ((1, "l"), (-1, "r")):
        zig(k.part("weapon", "prong_" + nm, "crystal", layer=2), [(sgn * 0.8, 29.5, 0.0), (sgn * 3.2, 32.0, 0.0), (sgn * 2.2, 33.6, 0.0), (sgn * 5.0, 37.6, 0.0), (sgn * 3.6, 38.4, 0.0), (sgn * 6.0, 43.0, 0.0)], 1.2, 0.5, up=(0, 0, 1))
    du.gem(k.part("weapon", "socket_gem", "crystal", layer=2, smooth=0), (0, 28.2, 0.9), 0.8, long=1.2, sides=6, axis="z")
    sweep_flat(k.part("weapon", "tassel", "cloth", smooth=45), [(0.8, 26.0, 0.4), (2.2, 22.0, 0.6), (1.4, 16.0, 0.3), (2.6, 10.0, 0.0)], 1.8, 0.25, pf_ribbon, (0, 0, 1))


def weapon_amon(k):
    """AMON_SUNBRAND: a greatsword 50 px long (ref 59 tile 2: the blade is taller than the figure's torso), broad, with a flame running along both edges."""
    ml.loft(k.part("weapon", "grip", "cloth"), "y", (0, -8, 0), [(0, 0.8, 0.8), (9, 0.8, 0.8)], ml.octagon(2.0, 2.0, 0.5))
    for j in range(4):
        w_ring(k, -7.5 + 2.2 * j, 1.05, f"wrap_{j}", "trim", 0.5)
    disc(k.part("weapon", "pommel", "trim", smooth=40), (0, -9.4, 0), 1.9, 1.4, "y", 10)
    du.gem(k.part("weapon", "pommel_gem", "crystal", layer=2, smooth=0), (0, -10.8, 0), 0.8, long=1.0, sides=6, axis="-y")
    du.box(k.part("weapon", "guard", "trim"), (-5.5, 1.0, -0.9), (5.5, 2.4, 0.9), chamfer=0.4)
    for sgn, nm in ((1, "l"), (-1, "r")):
        sweep_flat(k.part("weapon", "quillon_" + nm, "trim", smooth=45), [(sgn * 5.0, 1.7, 0.0), (sgn * 6.8, 0.8, 0.0), (sgn * 7.6, -1.0, 0.0)], 1.5, 0.9, pf_taper, (0, 0, 1))
        tg = k.part("weapon", "guard_flame_" + nm, "crystal", layer=2, smooth=45)
        tongue(tg, (sgn * 3.0, 2.4, 0.0), (sgn * 0.3, 1.0, 0.0), 3.8, 1.5, 0.4, 0.5, 1.0, n=3)
    du.box(k.part("weapon", "ricasso", "plate"), (-2.5, 2.4, -0.6), (2.5, 6.0, 0.6), chamfer=0.3)
    ml.loft(k.part("weapon", "blade", "plate", smooth=45), "y", (0, 6.0, 0), [(0, 1.0, 1.0), (22, 1.04, 1.0), (36, 0.96, 1.0), (40.5, 0.42, 0.7), (43, 0.0, 0.0)], ml.bevelled(5.6, 1.2, 0.42), up=(0, 0, 1))
    for sgn, nm in ((1, "f"), (-1, "b")):
        ml.ridge_pair(k.part("weapon", "fuller_" + nm, "trim"), (0, 8.0, sgn * 0.6), (0, 38.0, sgn * 0.5), (0, 0, sgn), 1.1, 0.26, 0.12, 3.0)
    for j, y in enumerate((14, 24, 33)):
        du.bar(k.part("weapon", f"inlay_{j}", "trim"), (-1.8, y, 0.62), (1.8, y, 0.62), 0.5, 0.12, up=(0, 0, 1), taper=0.5)
    for i in range(0, 6, 1 if False else 2):
        for sgn, nm in ((1, "l"), (-1, "r")):
            tg = k.part("weapon", f"flame_{nm}{i}", "crystal", layer=2, smooth=45)
            tongue(tg, (sgn * 2.6, 9.0 + i * 6.0, 0.0), (sgn * 0.5, 1.0, 0.0), 6.5 - 0.5 * i, 2.0, 0.4, 0.7 * sgn, i * 1.3 + (0 if sgn > 0 else 2.0), n=4)
    du.gem(k.part("weapon", "heart_gem", "crystal", layer=2, smooth=0), (0, 4.2, 0.7), 0.9, long=1.2, sides=6, axis="z")


def weapon_vinea(k):
    """VINEA_TIDETRIDENT: a trident staff, 52 px (ref 59 tile 3: the staff is taller than the figure), a pearl in a coral cradle under the three prongs."""
    w_shaft(k, -10, 30, 0.62, "plate", "shaft")
    for j, y in enumerate((-2, 2, 6)):
        w_ring(k, y, 1.0, f"wrap_{j}", "cloth", 0.9)
    orb(k.part("weapon", "butt", "bone", smooth=40), (0, -11.0, 0), 1.2, sides=8)
    w_ring(k, 27.0, 1.3, "collar", "trim", 1.2)
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        sweep_tube(k.part("weapon", f"coral_{i}", "bone", smooth=40), [(math.cos(a) * 1.1, 28.0, math.sin(a) * 1.1), (math.cos(a) * 2.2, 31.0, math.sin(a) * 2.2), (math.cos(a) * 1.6, 34.0, math.sin(a) * 1.6)], 0.55, 0.15, 5)
    orb(k.part("weapon", "pearl", "bone", smooth=0), (0, 31.0, 0), 1.7, sides=10)
    du.gem(k.part("weapon", "pearl_core", "crystal", layer=2, smooth=0), (0, 31.0, 0.9), 0.7, long=1.0, sides=6, axis="z")
    spear_blade(k, 34.0, 15.0, 3.0, 0.8, "centre", "plate")
    for sgn, nm in ((1, "l"), (-1, "r")):
        path = [(sgn * 1.2, 28.5, 0.0), (sgn * 4.6, 30.0, 0.0), (sgn * 6.4, 35.0, 0.0), (sgn * 6.0, 41.0, 0.0)]
        sweep_flat(k.part("weapon", "prong_" + nm, "plate", smooth=45), curve(path, 6), 2.3, 0.8, lambda s: (0.9 - 0.4 * s) * (1 - s ** 4) + 0.02, (0, 0, 1))
        spike(k.part("weapon", "tip_" + nm, "plate"), (sgn * 6.0, 40.4, 0.0), (0, 1.0, 0.0), 4.4, 0.9, 6)
        p = k.part("weapon", "fin_" + nm, "cloth", smooth=45)
        leaf(p, [(sgn * 1.4, 27.6, 0.1), (sgn * 5.6, 26.0, 0.1), (sgn * 7.2, 29.6, 0.1), (sgn * 3.0, 30.6, 0.1)] if sgn > 0 else [(sgn * 3.0, 30.6, 0.1), (sgn * 7.2, 29.6, 0.1), (sgn * 5.6, 26.0, 0.1), (sgn * 1.4, 27.6, 0.1)])
        orb(k.part("weapon", "drop_" + nm, "crystal", layer=2, smooth=0), (sgn * 6.4, 36.0, 0.0), 0.55, sides=6)
    sweep_flat(k.part("weapon", "ribbon", "cloth", smooth=45), [(0.6, 25.5, 0.4), (-1.6, 20.0, 0.6), (0.6, 14.0, 0.2), (-1.0, 8.0, 0.0)], 2.2, 0.25, pf_ribbon, (0, 0, 1))


def weapon_focalor(k):
    """FOCALOR_GALEGLAIVE: a thin glaive 58 px long (ref 59 tile 4: the spear is a long needle), a leaf head with a crescent blade, ribbons and wind-rings."""
    w_shaft(k, -10, 32, 0.5, "plate", "shaft")
    for j, y in enumerate((-3, 3)):
        w_ring(k, y, 0.9, f"wrap_{j}", "cloth", 0.9)
    for j, y in enumerate((12, 19, 26)):
        du.sweep(k.part("weapon", f"wind_{j}", "crystal", layer=2), ring_path((0, y, 0), 1.7 + j * 0.2, 0, 360, "xz", 10) and [(1.7 * math.cos(a) + 0.0, y + a * 0.35, 1.7 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 11)], C(0.14, 4))
    w_ring(k, 31.0, 1.2, "collar", "trim", 1.2)
    spear_blade(k, 32.0, 17.0, 3.2, 0.7, "head", "plate")
    crescent(k.part("weapon", "crescent", "plate", smooth=45), (3.4, 38.0, 0.0), 4.4, 2.4, 0.6, 80, 280, "xy", 8)
    du.gem(k.part("weapon", "gem", "crystal", layer=2, smooth=0), (0, 33.0, 0.8), 0.7, long=1.1, sides=6, axis="z")
    for sgn, nm in ((1, "l"), (-1, "r")):
        for j in range(3):
            blade3(k.part("weapon", f"fea_{nm}{j}", "plate", smooth=45), (sgn * 0.6, 31.4 - j * 0.4, 0.2), (sgn * 0.9, 0.5 - j * 0.35, 0.0), (sgn * 0.4, -0.5, 0.0), 6.0 - j, 1.4, 0.3, wdir=(0, 0, 1), n=3)
    for i, off in enumerate((-0.8, 0.8)):
        sweep_flat(k.part("weapon", f"streamer_{i}", "cloth", smooth=45), [(off, 30.0, 0.3), (off * 3, 24.0, 0.5), (off * 2, 17.0, 0.2), (off * 4, 10.0, 0.0)], 2.0, 0.25, pf_ribbon, (0, 0, 1))


def weapon_zagan(k):
    """ZAGAN_BLOOMSTAFF: a living staff 54 px long: two intertwined stems, thorns, three leaves and a flower in bloom at the top."""
    for sgn, nm in ((1, "a"), (-1, "b")):
        path = [(sgn * 0.7 * math.sin(i * 0.9 + (0 if sgn > 0 else math.pi)), -10 + i * 5.6, 0.7 * math.cos(i * 0.9 + (0 if sgn > 0 else math.pi))) for i in range(8)]
        sweep_tube(k.part("weapon", "stem_" + nm, "extra", smooth=40), path, 0.65, 0.5, 6)
    for j, y in enumerate((-2, 3)):
        w_ring(k, y, 1.2, f"wrap_{j}", "cloth", 1.0)
    for j in range(5):
        spike(k.part("weapon", f"thorn_{j}", k.trim()), (0.9 * math.cos(j * 2.4), 6 + j * 4.2, 0.9 * math.sin(j * 2.4)), (math.cos(j * 2.4), 0.3, math.sin(j * 2.4)), 1.8, 0.4, 5)
    for j, (y, sgn) in enumerate(((10, 1), (17, -1), (24, 1))):
        lf = k.part("weapon", f"leaf_{j}", "cloth")
        P = [(0, y, 0), (sgn * 2.4, y + 1.2, 0.2), (sgn * 4.6, y + 3.4, 0.4), (sgn * 2.4, y + 4.6, 0.3)]
        leaf(lf, P if sgn > 0 else P[::-1])
    orb(k.part("weapon", "root", "extra", smooth=40), (0, -11.0, 0), 1.6, sides=8)
    du.gem(k.part("weapon", "bud", "crystal", layer=2, smooth=0), (0, 36.0, 0), 1.9, long=1.5, sides=7, axis="y")
    for i in range(8):
        a = i * 2 * math.pi / 8
        blade3(k.part("weapon", f"petal_{i}", "cloth", smooth=45), (math.cos(a) * 1.2, 34.5, math.sin(a) * 1.2), (math.cos(a) * 0.9, 0.6, math.sin(a) * 0.9), (math.cos(a) * 0.5, 1.0, math.sin(a) * 0.5), 7.0, 2.2, 0.4, wdir=(-math.sin(a), 0, math.cos(a)), n=4, prof=pf_blade)
    for i in range(5):
        a = i * 2 * math.pi / 5 + 0.3
        spike(k.part("weapon", f"stamen_{i}", "crystal", layer=2), (math.cos(a) * 0.6, 36.0, math.sin(a) * 0.6), (math.cos(a) * 0.35, 1.0, math.sin(a) * 0.35), 4.0, 0.3, 5)


def weapon_zepar(k):
    """ZEPAR_GIANTBREAKER: a great axe, haft 46 px with a double-bit head 22 px across (ref 59 tile 6: the heavy rose-glass blade)."""
    w_shaft(k, -10, 30, 1.0, "extra", "haft")
    for j, y in enumerate((-5, 0, 5, 10)):
        w_ring(k, y, 1.6, f"band_{j}", "trim" if j % 2 == 0 else "plate", 0.9)
    orb(k.part("weapon", "counterweight", "extra", smooth=40), (0, -11.5, 0), 2.4, sides=9)
    for i in range(5):
        a = i * 2 * math.pi / 5
        spike(k.part("weapon", f"butt_spike_{i}", k.trim()), (math.cos(a) * 1.9, -11.5, math.sin(a) * 1.9), (math.cos(a) * 0.9, -0.5, math.sin(a) * 0.9), 2.0, 0.6, 5)
    du.box(k.part("weapon", "head_core", "extra"), (-2.4, 24.0, -1.8), (2.4, 34.0, 1.8), chamfer=0.6)
    for sgn, nm in ((1, "l"), (-1, "r")):
        # AXE_BIT: a bearded bit, a filled polygon (outer edge sweeping 22 px across), a glowing edge and a rivet
        P = [(2.0, 32.5), (2.0, 25.5), (5.5, 22.0), (10.5, 19.5), (12.6, 26.0), (12.2, 32.0), (10.0, 38.5), (5.5, 36.0)]
        pts = [(sgn * x, y, 0.0) for (x, y) in P]
        leaf(k.part("weapon", "bit_" + nm, "plate", smooth=45), pts if sgn > 0 else pts[::-1])
        du.sweep(k.part("weapon", "edge_" + nm, "crystal", layer=2), [(sgn * x, y, 0.15) for (x, y) in P[3:7]], ml.lozenge(0.8, 0.5))
        du.bar(k.part("weapon", "bit_rib_" + nm, k.trim()), (sgn * 2.4, 29.0, 0.1), (sgn * 11.0, 29.0, 0.1), 0.7, 0.3, up=(0, 0, 1), taper=1.4)
        du.stud(k.part("weapon", "rivet_" + nm, k.trim()), (sgn * 3.6, 29.0, 1.8), 0.7, 0.6, (0, 0, 1), 6)
    spike(k.part("weapon", "top_spike", "plate"), (0, 33.6, 0), (0, 1.0, 0), 8.0, 1.5, 7)
    du.gem(k.part("weapon", "heart", "crystal", layer=2, smooth=0), (0, 29.0, 1.9), 1.3, long=1.2, sides=6, axis="z")
    collar(k.part("weapon", "neck", k.trim()), 22.5, 1.9, 1.5, lip=0.3, sides=S(10))


def weapon_furfur(k):
    """FURFUR_RUINHORN: a war-horn 48 px long that curves up and flares into a bell 11 px wide, with sound rings spreading from the mouth."""
    path = [(0, -8, 0), (0.0, 0.0, 0.0), (1.4, 10.0, 0.0), (4.4, 20.0, 0.0), (9.0, 27.0, 0.0), (14.0, 31.0, 0.0)]
    pts = curve(path, 8)
    du.sweep(k.part("weapon", "horn", "extra", smooth=40), pts, C(1.0, S(12)), scales=lambda t: (0.55 + 0.35 * t + 4.8 * max(0.0, t - 0.62) ** 1.6 / 0.38 ** 1.6,) * 2)
    for j, t in enumerate((0.1, 0.26, 0.42, 0.58)):
        c = pts[int(len(pts) * t)]
        collar(k.part("weapon", f"band_{j}", k.trim()), 0, 0.9 + 0.3 * j, 0.8, lip=0.2, sides=S(10), axis=(0, 1, 0), origin=tuple(c))
    end = np.array(pts[-1])
    collar(k.part("weapon", "lip", k.trim()), 0, 5.6, 0.9, lip=0.22, sides=S(12), axis=(1.0, 0.6, 0.0), origin=tuple(end - np.array([0.4, 0.3, 0])))
    orb(k.part("weapon", "mouthpiece", "trim", smooth=40), (0, -9.0, 0), 0.9, sides=7)
    du.gem(k.part("weapon", "bell_gem", "crystal", layer=2, smooth=0), tuple(end + np.array([0.4, 0.2, 0])), 1.6, long=1.2, sides=6, axis=(1, 0.6, 0))
    for j, r in enumerate((3.4, 5.4, 7.4)):
        c = end + np.array([2.0 + 2.4 * j, 1.2 + 1.4 * j, 0.0])
        du.sweep(k.part("weapon", f"wave_{j}", "crystal", layer=2), [tuple(c + np.array([math.sin(a) * 0.5, math.cos(a) * r * 0.8, math.sin(a) * r * 0.8 + 0.0])) for a in np.linspace(-1.1, 1.1, 8)], C(0.28, 4))
    sweep_flat(k.part("weapon", "tassel", "cloth", smooth=45), [(0.5, 4.0, 0.4), (-1.2, -1.0, 0.5), (0.6, -6.0, 0.2), (-0.4, -11.0, 0.0)], 1.6, 0.25, pf_ribbon, (0, 0, 1))


def weapon_marchosias(k):
    """MARCHOSIAS_EMBERFANGS: a fist-bar with three curved claws 24 px long, the outer two shorter, flame along their backs."""
    du.box(k.part("weapon", "grip", "extra"), (-4.2, -1.2, -1.0), (4.2, 1.2, 1.0), chamfer=0.4)
    for j, x in enumerate((-3.0, -1.0, 1.0, 3.0)):
        du.stud(k.part("weapon", f"knuckle_{j}", k.trim()), (x, 0.0, 1.0), 0.65, 0.5, (0, 0, 1), 7)
    collar(k.part("weapon", "cuff", "plate"), -7.0, 2.2, 3.0, lip=0.4, sides=S(10), axis="y")
    du.box(k.part("weapon", "guard", "plate"), (-4.6, 1.2, -1.4), (4.6, 3.4, 1.4), chamfer=0.5)
    for j, (x, L, curl) in enumerate(((-3.2, 17.0, 26), (-1.0, 24.0, 22), (1.2, 22.0, 24), (3.4, 15.0, 28))):
        p = k.part("weapon", f"claw_{j}", "bone", smooth=45)
        feather(p, (x, 3.2, 0.0), 86 - x * 3, L, 2.6, 1.0, curl, "xy", 6, pf_blade, sx=1.0)
        tg = k.part("weapon", f"flame_{j}", "crystal", layer=2, smooth=45)
        tongue(tg, (x, 5.0, -0.7), (0.15, 1.0, -0.1), L * 0.7, 1.6, 0.4, 0.6, j * 1.4, n=4)
    du.gem(k.part("weapon", "ember", "crystal", layer=2, smooth=0), (0, 2.4, 1.5), 1.1, long=1.2, sides=6, axis="z")
    for sgn in (-1, 1):
        spike(k.part("weapon", "side_fang_" + ("l" if sgn > 0 else "r"), "bone"), (sgn * 4.6, 2.0, 0.0), (sgn * 0.7, 0.8, 0.0), 5.0, 0.9, 6)


def weapon_sitri(k):
    """SITRI_DAWNBOW: a recurve bow 44 px tall (ref 61: the bow is as tall as the Djinn), gold limbs ending in light blades, a string of light and an arrow."""
    limb = [(0.0, 0.0, 0.0), (-2.0, 7.0, 0.0), (-4.8, 14.0, 0.0), (-4.2, 21.0, 0.0), (-1.2, 25.0, 0.0), (1.8, 26.4, 0.0)]
    for sgn, nm in ((1, "up"), (-1, "dn")):
        pts = [(x, y * sgn, z) for (x, y, z) in limb]
        sweep_flat(k.part("weapon", "limb_" + nm, "plate", smooth=45), curve(pts, 4), 1.6, 1.3, lambda s: 1.0 - 0.55 * s + 0.0, (1, 0, 0) if False else (0, 0, 1), caps=(True, True))
        sweep_flat(k.part("weapon", "limb_inlay_" + nm, "trim", smooth=45), curve([(x, y * sgn, z + 0.55) for (x, y, z) in pts], 4), 0.5, 0.3, pf_taper, (0, 0, 1))
        blade3(k.part("weapon", "tip_blade_" + nm, "crystal", glow=True, smooth=45), (1.8, 26.4 * sgn, 0.0), (0.6, 0.8 * sgn, 0.0), (0.3, 1.0 * sgn, 0.0), 6.0, 1.6, 0.5, wdir=(0, 0, 1), n=3, prof=pf_blade)
    du.box(k.part("weapon", "riser", "trim"), (-1.7, -4.0, -1.1), (1.7, 4.0, 1.1), chamfer=0.4)
    du.gem(k.part("weapon", "riser_gem", "crystal", layer=2, smooth=0), (0, 0, 1.2), 0.9, long=1.2, sides=6, axis="z")
    for j in range(3):
        w_ring(k, -3.2 + 2.4 * j, 1.55, f"wrap_{j}", "cloth", 0.7)
    du.bar(k.part("weapon", "string", "crystal", layer=2), (1.8, 26.4, 0.0), (1.8, -26.4, 0.0), 0.4, 0.2, up=(0, 0, 1))
    ml.loft(k.part("weapon", "arrow", "trim"), "x", (-1.0, 0.0, 0.3), [(0, 0.35, 0.35), (-9.0, 0.3, 0.3)], C(1.0, 5), caps=(False, False)) if False else None
    spike(k.part("weapon", "arrow_head", "crystal", layer=2), (3.2, 0, 0.0), (1.0, 0, 0), 4.0, 0.8, 5)
    for j in range(4):
        spike(k.part("weapon", f"ray_{j}", "crystal", layer=2), (-2.0 - 1.8 * j, 0.0, 0.0), (-1.0, 0.0, 0.0), 2.6, 0.2 + 0.05 * j, 4)


def weapon_bifron(k):
    """BIFRON_STORMGLAIVE: a double-ended glaive: a long zig-zag blade at the front (50 px from the grip), a short one behind the hand (12 px), lightning cores in both."""
    w_shaft(k, -16, 22, 0.7, "plate", "shaft")
    for j, y in enumerate((-8, -4, 0, 4, 8)):
        w_ring(k, y, 1.05, f"wrap_{j}", "cloth" if j % 2 == 0 else "trim", 0.8)
    for sgn, nm, f in ((1, "a", 1.0), (-1, "b", 0.0)):
        if sgn > 0:
            w_ring(k, 22.0, 1.4, "collar_" + nm, "trim", 1.0)
            base, k0 = 22.0, 1.0
        else:
            w_ring(k, -17.0, 1.4, "collar_" + nm, "trim", 1.0)
            base, k0 = -16.0, 0.5
        p = k.part("weapon", "blade_" + nm, "plate", smooth=45)
        path = [(0, base, 0), (1.6 * k0, base + 6.0 * sgn * k0, 0), (3.2 * k0, base + 12.0 * sgn * k0, 0), (3.0 * k0, base + 19.0 * sgn * k0, 0), (0.4 * k0, base + 25.0 * sgn * k0, 0)]
        sweep_flat(p, curve(path, 4), 4.4 * (1.0 if sgn > 0 else 0.8), 0.9, lambda s: (0.5 + 0.5 * math.sin(min(1, s * 1.6) * math.pi / 2)) * (1 - s ** 2.5), (0, 0, 1))
        zz = [(0, base + 1 * sgn, 0.5), (1.4 * k0, base + 6 * sgn * k0, 0.5), (0.2 * k0, base + 9 * sgn * k0, 0.5), (2.8 * k0, base + 15 * sgn * k0, 0.5), (1.4 * k0, base + 17 * sgn * k0, 0.5), (0.4 * k0, base + 23 * sgn * k0, 0.5)]
        zig(k.part("weapon", "core_" + nm, "crystal", layer=2), zz, 0.7, 0.3)
        for s2 in (1, -1):
            spike(k.part("weapon", f"barb_{nm}{'p' if s2 > 0 else 'm'}", "trim"), (s2 * 1.0, base + 2.0 * sgn, 0), (s2 * 1.0, 0.6 * sgn, 0), 4.0 * k0, 0.8, 5)
    du.gem(k.part("weapon", "grip_gem", "crystal", layer=2, smooth=0), (0, 0, 0.9), 0.9, long=1.2, sides=6, axis="z")


def weapon_paimon(k):
    """PAIMON_TWINMOON: a lunar glaive 56 px long: a big crescent over a small one (opening down), a gem between, wind streamers."""
    w_shaft(k, -10, 30, 0.55, "plate", "shaft")
    for j, y in enumerate((-3, 3)):
        w_ring(k, y, 0.95, f"wrap_{j}", "cloth", 0.9)
    orb(k.part("weapon", "butt", "trim", smooth=40), (0, -11.0, 0), 1.2, sides=8)
    w_ring(k, 27.0, 1.2, "collar", "trim", 1.2)
    crescent(k.part("weapon", "moon_big", "plate", smooth=45), (0, 36.0, 0.0), 9.0, 4.4, 1.0, -30, 210, "xy", 14)
    crescent(k.part("weapon", "moon_big_edge", "crystal", layer=2), (0, 36.0, 0.35), 9.0 + 2.0, 0.7, 0.3, -30, 210, "xy", 14)
    crescent(k.part("weapon", "moon_small", "plate", smooth=45), (0, 28.0, 0.0), 5.4, 2.4, 0.8, 150, 390, "xy", 10)
    du.gem(k.part("weapon", "heart", "crystal", layer=2, smooth=0), (0, 33.0, 0.0), 1.6, long=1.2, sides=6, axis="z")
    spike(k.part("weapon", "tip", "plate"), (0, 41.0, 0), (0, 1.0, 0), 7.0, 1.0, 6)
    for i, off in enumerate((-0.7, 0.7)):
        sweep_flat(k.part("weapon", f"streamer_{i}", "cloth", smooth=45), [(off, 26.0, 0.3), (off * 3, 20.0, 0.5), (off * 2, 13.0, 0.2), (off * 4, 6.0, 0.0)], 1.8, 0.25, pf_ribbon, (0, 0, 1))


def weapon_vassago(k):
    """VASSAGO_ABYSSREAPER: a scythe 56 px long: a long haft wound in tentacle cord, a blade 24 px sweeping out of an eye-socket collar."""
    pth = [(0.0, -10.0, 0.0), (0.4, 0.0, 0.0), (0.0, 12.0, 0.0), (-0.4, 24.0, 0.0), (0.0, 33.0, 0.0)]
    sweep_tube(k.part("weapon", "haft", "extra", smooth=40), pth, 0.7, 0.6, 8)
    for j, y in enumerate((-4, 1, 6)):
        w_ring(k, y, 1.1, f"wrap_{j}", "cloth", 0.9)
    orb(k.part("weapon", "skull", "bone", smooth=0), (0, -11.4, 0), 1.6, sides=8)
    for sgn in (-1, 1):
        du.gem(k.part("weapon", "socket_" + ("l" if sgn > 0 else "r"), "crystal", layer=2, smooth=0), (sgn * 0.7, -11.4, 1.3), 0.4, long=0.8, sides=5, axis="z")
    collar(k.part("weapon", "mount", k.trim()), 31.0, 1.8, 2.4, lip=0.4, sides=S(10))
    du.gem(k.part("weapon", "eye", "crystal", layer=2, smooth=0), (0, 33.0, 1.6), 1.2, long=1.1, sides=6, axis="z")
    blade_path = [(0.0, 33.0, 0.0), (4.0, 38.0, 0.0), (10.0, 40.5, 0.0), (17.0, 38.0, 0.0), (22.0, 31.0, 0.0), (23.4, 24.0, 0.0)]
    sweep_flat(k.part("weapon", "blade", "plate", smooth=45), curve(blade_path, 5), 6.0, 1.0, lambda s: (0.35 + 0.65 * math.sin(min(1.0, s * 1.4) * math.pi / 2)) * (1 - s ** 3), (0, 0, 1))
    sweep_flat(k.part("weapon", "edge", "crystal", layer=2), curve([(x, y - 0.0, 0.4) for (x, y, z) in blade_path], 5), 0.8, 0.3, pf_taper, (0, 0, 1))
    spike(k.part("weapon", "back_spike", "trim"), (-0.6, 33.0, 0), (-0.5, 1.0, 0), 7.0, 1.0, 6)
    for i in range(3):
        sweep_tube(k.part("weapon", f"tentacle_{i}", "cloth"), [(0.8, 30.0 - i * 3, 0.5 * i), (2.8, 26.0 - i * 4, 1.2), (1.6, 20.0 - i * 5, -0.6)], 0.5, 0.1, 5)


WEAPONS = {"BAAL": weapon_baal, "AMON": weapon_amon, "VINEA": weapon_vinea, "FOCALOR": weapon_focalor, "ZAGAN": weapon_zagan, "ZEPAR": weapon_zepar,
           "FURFUR": weapon_furfur, "MARCHOSIAS": weapon_marchosias, "SITRI": weapon_sitri, "BIFRON": weapon_bifron, "PAIMON": weapon_paimon, "VASSAGO": weapon_vassago}

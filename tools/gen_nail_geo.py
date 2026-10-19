"""Nail Magic (1.93.0 rebuild): the geo models of the temporary armour and of the two constructs.

    python3 -B tools/gen_nail_geo.py
    python3 -B tools/geo_check.py nail_armor nail_bolt nail_hammer

nail_armor   pieces on the humanoid frame, one root bone per piece: helm (a studded iron cap with a crown of eight long nails), chest
             (a brigandine of overlapping studded plates with a nail-head belt), waist (nail-head tassets), pauldron_r/_l (studded domes
             with four nails each), vambrace_r/_l (rows of nails driven through the bracer), gauntlet_r/_l (five long pale nail-claws each),
             greave_r/_l (studded plates with knee and shin nails), back (the nail wheel: a hub and ten long nails that turn).
             Material: dark studded iron with rusted patches, pale steel nail shafts, ember heat on the heads (the glow map).
             Clips: idle (the wheel turns), move (faster).
nail_bolt    a giant driven nail: a round head, a long tapered shaft and a point. Clips: idle (a slow spin), fly (fast spin).
nail_hammer  the hammer that drives them: a heavy iron head with a glowing face on a long haft. Clips: idle, slam.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from shakudo_kit import Kit, Anim, iron_studs, nail_steel, keratin, emissive, solid, sway, bob, nail      # noqa: E402

IRON = iron_studs()
IRON2 = iron_studs(dark=0x211D1B, rust=0x4F2C1A, head=0x8C8478, pitch=3)
HEAD = iron_studs(dark=0x403A35, rust=0x7A4228, head=0xD0C6B0, pitch=3, heat=0.3)
SHAFT = nail_steel()
SHAFT_H = nail_steel(glow=0xFF9A50)
CLAW = keratin()
EMBER = emissive(0xFFA050, base=0x2A1A10)
WOOD = solid(0x5A3C28, bevel=0.1)


def armor():
    k = Kit("nail", "armor", 256, 256, seed=17)
    # ------------------------------------------------------------------ helm
    k.bone("helm", pivot=(0, 24, 0))
    k.box("helm", (-4, 27, -4), (8, 5, 8), IRON, inflate=0.7)
    k.box("helm", (-5, 26.2, -5.4), (10, 1, 2), IRON2)
    k.pair("helm", "helm", (-6.0, 24.0, -3.0), (1, 5, 6), IRON2, inflate=0.1)
    for i in range(8):                                                               # the crown of nails, nail heads on the cap, points out
        a = 2 * math.pi * i / 8
        x, z = 3.6 * math.cos(a), 3.6 * math.sin(a)
        tilt = 28
        nail(k, "helm", "hn%d" % i, (x, 32.4, z), (tilt * math.sin(a), 0, -tilt * math.cos(a)), 7 if i % 2 == 0 else 5, HEAD, SHAFT_H)
    k.box("helm", (-5, 24.4, 3.0), (10, 2, 2), IRON2, inflate=0.1)
    # ------------------------------------------------------------------ chest
    k.bone("chest", pivot=(0, 24, 0))
    k.box("chest", (-4, 12.5, -2), (8, 11, 4), IRON, inflate=0.8)
    for y in (14, 17, 20):                                                           # overlapping plates
        k.box("chest", (-4, y, -2), (8, 3, 1), IRON2, inflate=1.1)
    k.box("chest", (-5, 22.6, -2.5), (10, 2, 5), IRON2, inflate=0.1)
    for i in range(3):                                                               # a big nail driven into the sternum
        nail(k, "chest", "cn%d" % i, (-2.4 + i * 2.4, 22.0 - (i % 2) * 3, -3.3), (-90, 0, 0), 4, HEAD, SHAFT)
    k.box("chest", (-3.5, 13.5, 2.0), (7, 8, 1), IRON2, inflate=0.9)
    # ------------------------------------------------------------------ waist
    k.bone("waist", pivot=(0, 13, 0))
    k.box("waist", (-4, 11.5, -2), (8, 2, 4), IRON2, inflate=0.9)
    for i in range(-2, 2):
        k.box("waist", (i * 2.0, 8.5, -3.4), (2, 4, 1), IRON, rotation=(-8, 0, 0), pivot=(0, 12, -3))
        k.box("waist", (i * 2.0, 8.5, 2.4), (2, 4, 1), IRON, rotation=(8, 0, 0), pivot=(0, 12, 3))
    nail(k, "waist", "wn_r", (-5.4, 12.2, 0), (0, 0, 80), 5, HEAD, SHAFT_H)
    nail(k, "waist", "wn_l", (5.4, 12.2, 0), (0, 0, -80), 5, HEAD, SHAFT_H)
    # ------------------------------------------------------------------ pauldrons: studded domes, nails
    r, l = k.pair_bones("pauldron", pivot=(-5, 22, 0))
    k.pair(r, l, (-9.4, 21.6, -3.6), (6, 2, 7), IRON, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-10.2, 19.4, -3.4), (6, 2, 7), IRON2, rotation=(0, 0, -14), pivot=(-5, 22, 0))
    k.pair(r, l, (-9.0, 23.0, -2.0), (4, 1, 4), HEAD)
    for nm, sgn in (("_r", -1), ("_l", 1)):
        nail(k, "pauldron" + nm, "pa%s" % nm, (sgn * 8.0, 23.6, 0), (0, 0, -sgn * 30), 8, HEAD, SHAFT_H)
        nail(k, "pauldron" + nm, "pb%s" % nm, (sgn * 7.4, 23.4, -2.4), (-30, 0, -sgn * 40), 6, HEAD, SHAFT)
        nail(k, "pauldron" + nm, "pc%s" % nm, (sgn * 7.4, 23.4, 2.4), (30, 0, -sgn * 40), 6, HEAD, SHAFT)
        nail(k, "pauldron" + nm, "pd%s" % nm, (sgn * 10.6, 19.4, 0), (0, 0, -sgn * 80), 6, HEAD, SHAFT)
    # ------------------------------------------------------------------ vambraces
    r, l = k.pair_bones("vambrace", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 13.2, -2), (4, 6, 4), IRON, inflate=0.75)
    k.pair(r, l, (-8, 17.6, -2), (4, 1, 4), IRON2, inflate=0.95)
    for nm, sgn in (("_r", -1), ("_l", 1)):
        for i in range(3):
            nail(k, "vambrace" + nm, "vn%d%s" % (i, nm), (sgn * 8.8, 14.0 + i * 1.8, 0.0), (0, 0, -sgn * 90), 5 - i, HEAD, SHAFT, head=2)
    # ------------------------------------------------------------------ gauntlets: five long nail-claws
    r, l = k.pair_bones("gauntlet", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 11.6, -2), (4, 3, 4), IRON2, inflate=0.8)
    for nm, sgn in (("_r", -1), ("_l", 1)):
        for i in range(5):
            x = sgn * (4.5 + i * 0.9) - (0 if sgn < 0 else 0)
            nail(k, "gauntlet" + nm, "gc%d%s" % (i, nm), (x, 12.2, -3.0), (-90 + (i - 2) * 6, 0, 0), 6 - abs(i - 2), IRON2, CLAW, head=1)
    # ------------------------------------------------------------------ greaves
    r, l = k.pair_bones("greave", pivot=(-1.9, 12, 0))
    k.pair(r, l, (-4, 6.2, -2), (4, 5, 4), IRON, inflate=0.7)
    k.pair(r, l, (-4, 0.9, -2), (4, 5, 4), IRON2, inflate=0.6)
    k.pair(r, l, (-4, 3.6, -2), (4, 1, 4), IRON, inflate=0.78)
    k.pair(r, l, (-4, 0, -2), (4, 2, 4), IRON, inflate=0.55)
    for nm, sgn in (("_r", -1), ("_l", 1)):
        nail(k, "greave" + nm, "gk" + nm, (sgn * 2.0, 6.0, -3.0), (-90, 0, 0), 5, HEAD, SHAFT_H)
        nail(k, "greave" + nm, "gs" + nm, (sgn * 4.6, 3.0, 0.0), (0, 0, -sgn * 90), 4, HEAD, SHAFT)
    # ------------------------------------------------------------------ back: the nail wheel
    k.bone("back", pivot=(0, 22, 3))
    k.box("back", (-3, 14, 2.4), (6, 8, 1), IRON2, inflate=0.8)
    k.bone("wheel", parent="back", pivot=(0, 24, 5.5))
    k.box("wheel", (-3, 21, 5.0), (6, 6, 2), IRON2)
    k.box("wheel", (-2, 22, 4.4), (4, 4, 1), EMBER)
    for i in range(10):
        a = 2 * math.pi * i / 10
        base = (3.4 * math.cos(a), 24 + 3.4 * math.sin(a), 6.2)
        rz = math.degrees(math.atan2(-math.cos(a), math.sin(a)))                      # +y of the nail bone to the radial direction
        nail(k, "wheel", "wn%d" % i, base, (0, 0, rz), 10 if i % 2 == 0 else 7, HEAD, SHAFT_H if i % 2 == 0 else SHAFT, head=2)
    a = Anim()
    a.clip("idle", length=10.0, loop=True)
    a.rot("idle", "wheel", {0.0: (0, 0, 0), 5.0: (0, 0, 180), 10.0: (0, 0, 360)})
    a.clip("move", length=4.0, loop=True)
    a.rot("move", "wheel", {0.0: (0, 0, 0), 2.0: (0, 0, 180), 4.0: (0, 0, 360)})
    k.save(a)


def bolt():
    k = Kit("nail", "bolt", 128, 128, seed=29)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("nail", parent="root", pivot=(0, 0, 0))
    # lies along z with its point on -z (the prop's front): head at +z
    k.box("nail", (-6, -6, 14), (12, 12, 3), HEAD)
    k.box("nail", (-4, -4, 12), (8, 8, 2), IRON2)
    k.box("nail", (-2, -2, -14), (4, 4, 26), SHAFT_H)
    k.box("nail", (-1.5, -1.5, -22), (3, 3, 8), SHAFT)
    k.box("nail", (-1, -1, -28), (2, 2, 6), SHAFT_H)
    k.box("nail", (-0.5, -0.5, -32), (1, 1, 4), SHAFT_H)
    k.box("nail", (-2.5, -2.5, 6), (5, 5, 1), EMBER)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    a.rot("idle", "nail", {0.0: (0, 0, 0), 1.0: (0, 0, 180), 2.0: (0, 0, 360)})
    a.clip("fly", length=0.5, loop=True)
    a.rot("fly", "nail", {0.0: (0, 0, 0), 0.25: (0, 0, 180), 0.5: (0, 0, 360)})
    k.save(a)


def hammer():
    k = Kit("nail", "hammer", 128, 128, seed=37)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("haft", parent="root", pivot=(0, 4, 0))
    k.box("haft", (-1.5, 0, -1.5), (3, 44, 3), WOOD)
    k.box("haft", (-2, 0, -2), (4, 5, 4), IRON2)
    k.box("haft", (-2, 22, -2), (4, 2, 4), IRON2)
    k.bone("head", parent="haft", pivot=(0, 44, 0))
    k.box("head", (-10, 42, -6), (20, 10, 12), IRON)
    k.box("head", (-11, 41, -7), (3, 12, 14), IRON2)
    k.box("head", (8, 41, -7), (3, 12, 14), IRON2)
    k.box("head", (-9, 52, -5), (18, 1, 10), HEAD)
    k.box("head", (-12, 43, -3), (1, 8, 6), EMBER)
    k.box("head", (11, 43, -3), (1, 8, 6), EMBER)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    bob(a, "idle", "haft", 1.0, 3.0)
    sway(a, "idle", "haft", 2, 2.5, 3.0)
    a.clip("slam", length=0.9, loop=False)
    a.rot("slam", "haft", {0.0: (-95, 0, 0), 0.3: (-110, 0, 0), 0.5: (0, 0, 0), 0.9: (0, 0, 0)})
    a.pos("slam", "haft", {0.0: (0, 20, 0), 0.3: (0, 26, 0), 0.5: (0, 0, 0), 0.9: (0, 0, 0)})
    k.save(a)


if __name__ == "__main__":
    armor()
    bolt()
    hammer()

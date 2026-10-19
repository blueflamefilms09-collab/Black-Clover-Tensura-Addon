"""Hair Magic (1.93.0 rebuild): the geo models of the temporary armour and of the two constructs.

    python3 -B tools/gen_hair_geo.py
    python3 -B tools/geo_check.py hair_armor hair_tendril hair_cocoon

hair_armor    pieces on the humanoid frame, one root bone per piece: helm (a mane: crown volume, a fringe over the brow, two long side locks
              and a long back mass, all swaying), chest (a woven bodice of braided bands with a crossed lacing), waist (a sash of hair with
              hanging strands), pauldron_r/_l (a braid coil and two hanging locks), vambrace_r/_l (hair wound round the forearm, tassels),
              gauntlet_r/_l (hair mitts with strand fringes), greave_r/_l (wrapped leggings with trailing locks), back (a cloak of three
              layered panels that sways, and four living tendrils of five segments each that wave over the shoulders). Material: pink
              strands with a lit gloss and glowing tips (the glow map). Clips: idle (slow sway), move (the cloak and tendrils stream).
hair_tendril  a long grasping tendril of ten segments rooted in the ground, ending in a tuft. Clips: idle (sway), lash (whip forward),
              grab (curls into a fist), rise.
hair_cocoon   an egg of woven hair strands with a glowing seam, wrapped round the caster or an ally. Clips: idle (slow pulse), open (the
              strands unwind and open like petals).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from shakudo_kit import Kit, Anim, hair, emissive, sway, bob, spike, chain_bones, wave      # noqa: E402

HR = hair()
HR2 = hair(dark=0x9C2250, mid=0xE0507F, light=0xFFA8C8)
HR3 = hair(dark=0xE0508A, mid=0xFF8EB6, light=0xFFE0EE, tip_glow=0xFFFFFF)
BAND = hair(dark=0x7A1840, mid=0xC83C70, light=0xFF90B8, flow=0.6)
GEM = emissive(0xFFD0E4, base=0x4A1630)


def armor():
    k = Kit("hair", "armor", 256, 256, seed=11)
    # ------------------------------------------------------------------ helm: the mane
    k.bone("helm", pivot=(0, 24, 0))
    k.box("helm", (-4, 27, -4), (8, 5, 8), HR, inflate=0.8)                               # the crown volume
    k.box("helm", (-4, 31.5, -4), (8, 2, 8), HR3, inflate=0.2)
    for i in range(-2, 2):                                                                  # the fringe over the brow
        k.box("helm", (i * 2.0, 25.6, -5.2), (2, 4 + (i % 2), 1), HR3, rotation=(-4, 0, 0), pivot=(0, 28, -5))
    k.box("helm", (-4.5, 28.4, -5.3), (9, 1, 1), BAND)
    k.pair("helm", "helm", (-5.6, 20.0, -2.5), (2, 9, 4), HR2)                              # side locks falling to the shoulders
    k.bone("mane", parent="helm", pivot=(0, 30, 4))
    k.box("mane", (-5, 16, 3.2), (10, 15, 3), HR, inflate=0.1)                              # the back mass
    k.box("mane", (-4, 10, 3.4), (8, 7, 2), HR3)
    k.box("mane", (-2, 6, 3.6), (4, 5, 1), HR2)
    # ------------------------------------------------------------------ chest: the woven bodice
    k.bone("chest", pivot=(0, 24, 0))
    k.box("chest", (-4, 12.5, -2), (8, 11, 4), HR2, inflate=0.8)
    for i, y in enumerate((14, 16.5, 19, 21.5)):
        k.box("chest", (-4, y, -2), (8, 2, 1), BAND, inflate=1.1)                           # braided bands
    for i in range(3):                                                                      # crossed lacing
        k.box("chest", (-3 + i * 2.5, 14, -3.2), (1, 8, 1), HR3, rotation=(0, 0, 25 if i % 2 == 0 else -25), pivot=(-2.5 + i * 2.5, 18, -3))
    k.box("chest", (-1, 20.2, -3.4), (2, 2, 1), GEM)
    k.box("chest", (-5, 22.6, -2.5), (10, 2, 5), HR, inflate=0.1)
    k.box("chest", (-3.5, 13.5, 2.0), (7, 8, 1), HR2, inflate=0.9)
    # ------------------------------------------------------------------ waist: the sash
    k.bone("waist", pivot=(0, 13, 0))
    k.box("waist", (-4, 11.5, -2), (8, 2, 4), BAND, inflate=0.9)
    for i in range(-2, 2):
        k.box("waist", (i * 2.0, 7.5, -3.4), (2, 5, 1), HR if i % 2 == 0 else HR3, rotation=(-6, 0, 0), pivot=(0, 12, -3))
        k.box("waist", (i * 2.0, 7.5, 2.4), (2, 5, 1), HR if i % 2 == 0 else HR3, rotation=(6, 0, 0), pivot=(0, 12, 3))
    k.pair("waist", "waist", (-5.7, 7.0, -1.5), (1, 6, 3), HR2, rotation=(0, 0, 8), pivot=(-5, 12, 0))
    # ------------------------------------------------------------------ pauldrons: a braid coil and locks
    r, l = k.pair_bones("pauldron", pivot=(-5, 22, 0))
    k.pair(r, l, (-9.6, 21.6, -3.6), (6, 2, 7), HR, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-10.0, 23.2, -3.0), (5, 2, 6), BAND, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-9.0, 24.6, -2.0), (4, 2, 4), HR3, rotation=(0, 0, -8), pivot=(-5, 22, 0))
    k.pair(r, l, (-10.6, 14.0, -2.5), (2, 8, 2), HR2, rotation=(0, 0, -12), pivot=(-9, 22, 0))
    k.pair(r, l, (-10.6, 14.0, 1.0), (2, 7, 2), HR2, rotation=(0, 0, -12), pivot=(-9, 22, 0))
    # ------------------------------------------------------------------ vambraces: hair wound round the forearm
    r, l = k.pair_bones("vambrace", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 13.2, -2), (4, 6, 4), HR, inflate=0.7)
    for i in range(3):
        k.pair(r, l, (-8, 13.8 + i * 1.9, -2), (4, 1, 4), BAND, inflate=0.95)
    k.pair(r, l, (-9.6, 11.0, 0.0), (1, 5, 1), HR3)
    k.pair(r, l, (-9.4, 10.0, -1.2), (1, 4, 1), HR2)
    # ------------------------------------------------------------------ gauntlets: mitts with fringe
    r, l = k.pair_bones("gauntlet", pivot=(-5, 22, 0))
    k.pair(r, l, (-8, 11.6, -2), (4, 3, 4), HR2, inflate=0.8)
    for i in range(4):
        k.pair(r, l, (-7.9 + i * 0.97, 10.0, -3.0), (1, 2 + (i % 2), 1), HR3)
    # ------------------------------------------------------------------ greaves: wrapped leggings and trailing locks
    r, l = k.pair_bones("greave", pivot=(-1.9, 12, 0))
    k.pair(r, l, (-4, 6.2, -2), (4, 5, 4), HR, inflate=0.7)
    k.pair(r, l, (-4, 0.9, -2), (4, 5, 4), HR2, inflate=0.6)
    k.pair(r, l, (-4, 3.4, -2), (4, 1, 4), BAND, inflate=0.78)
    k.pair(r, l, (-4, 0, -2), (4, 2, 4), HR, inflate=0.55)
    k.pair(r, l, (-4.0, -1.0, 2.8), (2, 5, 1), HR3)
    k.pair(r, l, (-2.0, -0.6, 2.8), (2, 4, 1), HR2)
    # ------------------------------------------------------------------ back: the cloak of hair and four tendrils
    k.bone("back", pivot=(0, 24, 2.6))
    cloak = []
    for i, (w, ln, zoff) in enumerate([(9, 22, 3.0), (7, 25, 3.8), (5, 27, 4.6)]):
        nm = "cloak%d" % i
        k.bone(nm, parent="back", pivot=(0, 23, 2.6 + i * 0.6))
        k.box(nm, (-w / 2.0 - (0 if w % 2 == 0 else 0.5), 23 - ln, zoff), (w, ln, 1), (HR, HR3, HR2)[i], inflate=0.1)
        cloak.append(nm)
    tend = []
    for i, (x, tilt) in enumerate([(-3.2, 10), (3.2, -10), (-1.6, 4), (1.6, -4)]):
        names = chain_bones(k, "back", "t%d_" % i, 5, (x, 21.0, 4.0), (0, 2.6, 1.0))
        for j, nm in enumerate(names):
            w = 3 if j < 3 else 2
            k.box(nm, (x - w / 2.0, 21.0 + 2.6 * j, 4.0 + 1.0 * j), (w, 3, w), (HR, HR3)[j % 2])
        tend.append(names)
    a = Anim()
    a.clip("idle", length=4.0, loop=True)
    for i, nm in enumerate(cloak):
        sway(a, "idle", nm, 0, 4 + 2 * i, 4.0, 0.6 * i)
    for i, names in enumerate(tend):
        wave(a, "idle", names, 2 if i % 2 == 0 else 0, 10, 4.0, 0.6, 1.1)
    sway(a, "idle", "mane", 0, 3, 4.0)
    a.clip("move", length=1.6, loop=True)
    for i, nm in enumerate(cloak):
        sway(a, "move", nm, 0, 18 + 6 * i, 1.6, 0.5 * i)
    for i, names in enumerate(tend):
        wave(a, "move", names, 2 if i % 2 == 0 else 0, 22, 1.6, 0.5, 1.1)
    sway(a, "move", "mane", 0, 9, 1.6)
    k.save(a)


def tendril():
    k = Kit("hair", "tendril", 128, 128, seed=27)
    k.bone("root", pivot=(0, 0, 0))
    n = 10
    names = chain_bones(k, "root", "h", n, (0, 0, 0), (0, 7, 0))
    for i, nm in enumerate(names):
        w = max(2, 8 - (i * 6) // n)
        k.box(nm, (-w / 2.0, 7.0 * i, -w / 2.0), (w, 7, w), (HR, HR3, HR2)[i % 3], inflate=0.1)
    k.box(names[-1], (-2.5, 7.0 * (n - 1) + 6, -2.5), (5, 5, 5), HR3)                       # the tuft
    k.box(names[-1], (-1, 7.0 * (n - 1) + 11, -1), (2, 4, 2), GEM)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    wave(a, "idle", names, 0, 6, 3.0, 0.7, 1.05)
    wave(a, "idle", names, 2, 4, 3.0, 0.9, 1.05)
    a.clip("lash", length=0.7, loop=False)
    for i, nm in enumerate(names):
        a.rot("lash", nm, {0.0: (0, 0, 0), 0.2 + 0.02 * i: (-14, 0, 0), 0.4 + 0.02 * i: (22, 0, 0), 0.7: (0, 0, 0)})
    a.clip("grab", length=0.8, loop=False)
    for i, nm in enumerate(names):
        if i >= 4:
            a.rot("grab", nm, {0.0: (0, 0, 0), 0.5: (46, 0, 0), 0.8: (50, 0, 0)})
    a.clip("rise", length=0.5, loop=False)
    a.scale("rise", names[0], {0.0: (1, 0.05, 1), 0.5: (1, 1, 1)})
    k.save(a)


def cocoon():
    k = Kit("hair", "cocoon", 256, 256, seed=39)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("egg", parent="root", pivot=(0, 16, 0))
    # the egg: stacked shells, widest in the middle (box UV: a 16 px deep cube is 64 px wide on the sheet)
    for (y, h, w, m) in [(1, 3, 10, HR3), (4, 4, 14, HR2), (8, 5, 18, HR), (13, 10, 20, HR2), (23, 5, 18, HR), (28, 4, 14, HR3), (32, 3, 8, HR2)]:
        k.box("egg", (-w / 2, y, -w / 2), (w, h, w), m, inflate=0.0)
    k.box("egg", (-0.5, 5, -10.6), (1, 26, 1), GEM)                                         # the glowing seam
    petals = []
    for i in range(6):
        a = 2 * math.pi * i / 6
        nm = "p%d" % i
        k.bone(nm, parent="egg", pivot=(10.5 * math.cos(a), 6, 10.5 * math.sin(a)))
        k.box(nm, (10.5 * math.cos(a) - 2, 6, 10.5 * math.sin(a) - 2), (4, 22, 4), (HR3, HR2)[i % 2])
        petals.append((nm, a))
    an = Anim()
    an.clip("idle", length=3.0, loop=True)
    an.scale("idle", "egg", {0.0: (1, 1, 1), 1.5: (1.04, 1.03, 1.04), 3.0: (1, 1, 1)})
    an.clip("open", length=0.8, loop=False)
    for nm, a in petals:
        an.rot("open", nm, {0.0: (0, 0, 0), 0.8: (-math.sin(a) * 60, 0, -math.cos(a) * 60)})
    an.scale("open", "egg", {0.0: (1, 1, 1), 0.8: (0.6, 0.3, 0.6)})
    k.save(an)


if __name__ == "__main__":
    armor()
    tendril()
    cocoon()

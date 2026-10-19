"""Recombination Magic (Henry Legolant, the Black Bulls' magic house) models of the 1.93.0 rebuild: the temporary armour pieces (THE HOUSE WORN AS ARMOUR) and the constructs
(the Raging Black Bull, a recombined room, the mana drill).

    python3 -B tools/gen_recombination_geo.py
    python3 -B tools/geo_check.py recombination_helm recombination_chest recombination_tasset recombination_pauldron_r recombination_pauldron_l \
        recombination_vambrace_r recombination_vambrace_l recombination_greave_r recombination_greave_l recombination_back recombination_halo \
        recombination_bull recombination_room recombination_drill

Look: dark oak planks and iron brackets, red-brown roof tiles, grey foundation stone and windows that glow warm orange (0xFF9A3C, the book's colour): the base of the Black Bulls
taken apart and put back together on a person: a gabled cap with a chimney and a bull's horns, a wall plate with a door and a window, gable-end shoulders, log sleeves, stilt boots, the
house itself on the back with smoke rising, three rooms circling it, and a ring of loose planks over the head. Written for the humanoid's proportions (tools/transmutation_geo_kit.py).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from transmutation_geo_kit import Sheet, Anim, wave, keys, S  # noqa: E402

KEY = "recombination"


def helm():
    sh = Sheet(KEY, "helm", 160, 80, seed=201)
    sh.bone("root", pivot=(0, 28, 0))
    sh.bone("smoke", "root", (3.0, 38.0, 1.0))
    sh.bone("horn_l", "root", (4.6, 31.0, 0))
    sh.bone("horn_r", "root", (-4.6, 31.0, 0))
    # an open cap: a gabled roof of tiles over a timber band (the face stays free), a chimney with smoke, the horns of the Black Bulls
    sh.cube("root", (-4.8, 29.6, -4.8), (9.6, 1.6, 9.6), mat="oak", deco=("plank",))
    sh.cube("root", (-5.4, 31.2, -5.4), (10.8, 1.0, 10.8), mat="oak_dark", deco=("plank",))
    sh.cube("root", (-5.2, 32.2, -4.6), (10.4, 1.4, 9.2), mat="tile", rot=(0, 0, 0))
    sh.cube("root", (-4.6, 33.6, -3.8), (9.2, 1.4, 7.6), mat="tile")
    sh.cube("root", (-3.6, 35.0, -3.0), (7.2, 1.2, 6.0), mat="tile")
    sh.cube("root", (-2.4, 36.2, -2.2), (4.8, 1.0, 4.4), mat="tile")
    sh.cube("root", (-5.6, 32.0, -0.5), (11.2, 0.8, 1.0), mat="bull_iron", deco=("rivet",))
    sh.cube("root", (-4.6, 24.4, 3.7), (9.2, 5.0, 1.3), mat="oak", deco=("plank",))                  # the neck board
    sh.cube("root", (-1.2, 29.2, -5.6), (2.4, 2.4, 0.9), mat="lamp", deco=("core",))              # a lamp on the brow
    sh.cube("root", (-5.0, 29.0, -5.2), (1.4, 2.6, 0.9), mat="bull_iron", deco=("rivet",))
    sh.cube("root", (3.6, 29.0, -5.2), (1.4, 2.6, 0.9), mat="bull_iron", deco=("rivet",))
    sh.cube("smoke", (2.0, 35.5, 0.2), (2.6, 3.6, 2.6), mat="stone")
    sh.cube("smoke", (1.6, 39.0, -0.2), (3.4, 0.9, 3.4), mat="bull_iron")
    # horns: two curved iron horns made of three segments each
    for (sx, bone) in ((1, "horn_l"), (-1, "horn_r")):
        x0 = sx * 4.8
        sh.cube(bone, (x0 - 0.9 + (0.0 if sx > 0 else 0), 30.0, -0.9), (1.8, 1.8, 1.8), mat="bull_iron")
        sh.cube(bone, (x0 + sx * 1.0 - 0.8, 31.4, -0.8), (1.6, 1.6, 1.6), mat="bull_iron", rot=(0, 0, -sx * 28), pv=(x0, 30.6, 0))
        sh.cube(bone, (x0 + sx * 2.6 - 0.65, 33.0, -0.65), (1.3, 2.4, 1.3), mat="bull_iron", rot=(0, 0, -sx * 52), pv=(x0, 30.6, 0))
        sh.cube(bone, (x0 + sx * 4.0 - 0.5, 35.0, -0.5), (1.0, 2.4, 1.0), mat="stone", rot=(0, 0, -sx * 74), pv=(x0, 30.6, 0))
    a = Anim()
    wave(a, "idle", "smoke", 3.0, 12, kind="pos", y=S(0.5), x=S(0.3, 0.2))
    wave(a, "idle", "horn_l", 3.0, 12, z=S(1.5, 0.1))
    wave(a, "idle", "horn_r", 3.0, 12, z=S(-1.5, 0.1))
    sh.save(a)


def chest():
    sh = Sheet(KEY, "chest", 192, 96, seed=211)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("door", "root", (-2.4, 12.6, -3.3))
    sh.bone("shutter_l", "root", (4.4, 21.0, -3.0))
    sh.bone("shutter_r", "root", (-4.4, 21.0, -3.0))
    # the wall of the house worn on the chest: foundation stone at the belly, oak boards, a door that swings, a window with shutters, a roof-beam collar, beams crossing the back
    sh.cube("root", (-4.7, 12.0, -2.8), (9.4, 2.4, 5.6), mat="stone")
    sh.cube("root", (-4.5, 14.2, -2.7), (9.0, 8.0, 1.0), mat="oak", deco=("plank",))
    sh.cube("root", (-5.0, 22.0, -3.2), (10.0, 1.8, 6.4), mat="oak_dark", deco=("plank",))
    sh.cube("root", (-5.2, 23.6, -3.4), (10.4, 1.2, 6.8), mat="tile")
    sh.cube("door", (-2.4, 12.8, -3.7), (4.8, 8.4, 0.9), mat="oak_dark", deco=("plank", "rivet"))
    sh.cube("door", (0.8, 16.4, -4.2), (0.9, 0.9, 0.7), mat="lamp", deco=("core",))
    sh.cube("root", (-2.9, 12.6, -3.3), (0.6, 8.9, 0.6), mat="bull_iron")
    sh.cube("root", (2.3, 12.6, -3.3), (0.6, 8.9, 0.6), mat="bull_iron")
    sh.cube("root", (-2.9, 21.2, -3.3), (5.8, 0.6, 0.6), mat="bull_iron")
    sh.cube("root", (-3.6, 18.9, -3.5), (2.2, 2.2, 0.5), mat="lamp", deco=("window",), window_faces=("north",))             # the round of the window
    sh.cube("root", (1.4, 18.9, -3.5), (2.2, 2.2, 0.5), mat="lamp", deco=("window",), window_faces=("north",))
    sh.cube("shutter_l", (3.5, 18.6, -3.9), (1.0, 2.8, 0.5), mat="oak", deco=("plank",))
    sh.cube("shutter_r", (-4.5, 18.6, -3.9), (1.0, 2.8, 0.5), mat="oak", deco=("plank",))
    sh.cube("root", (-4.8, 15.0, 1.9), (9.6, 8.6, 1.2), mat="oak", deco=("plank", "rivet"))
    sh.cube("root", (-5.0, 15.0, 3.0), (10.0, 1.0, 0.8), mat="bull_iron", rot=(0, 0, 38), pv=(0, 19.5, 3.4))
    sh.cube("root", (-5.0, 15.0, 3.0), (10.0, 1.0, 0.8), mat="bull_iron", rot=(0, 0, -38), pv=(0, 19.5, 3.4))
    sh.cube("root", (-5.4, 12.4, 1.8), (10.8, 1.2, 1.6), mat="stone")
    sh.pair("root", "root", [dict(o=(-5.0, 14.6, -2.2), s=(1.0, 7.6, 4.4), mat="oak_dark", deco=("plank",)),
                             dict(o=(-5.2, 20.4, -2.6), s=(1.2, 1.8, 5.2), mat="bull_iron", deco=("rivet",))])
    a = Anim()
    keys(a, "idle", "door", "rot", {0.0: (0, 0, 0), 1.5: (0, -4, 0), 3.0: (0, 0, 0)})
    wave(a, "idle", "shutter_l", 3.0, 12, y=S(5.0, 0.2))
    wave(a, "idle", "shutter_r", 3.0, 12, y=S(-5.0, 0.2))
    sh.save(a)


def tasset():
    sh = Sheet(KEY, "tasset", 128, 64, seed=221)
    sh.bone("root", pivot=(0, 13, 0))
    sh.bone("pf", "root", (0, 12.8, -2.8))
    sh.bone("pb", "root", (0, 12.8, 2.8))
    sh.bone("pl", "root", (4.8, 12.8, 0))
    sh.bone("pr", "root", (-4.8, 12.8, 0))
    sh.cube("root", (-4.8, 11.8, -2.9), (9.6, 1.5, 5.8), mat="bull_iron", deco=("seam", "rivet"))
    sh.cube("root", (-1.2, 11.5, -3.6), (2.4, 2.0, 0.8), mat="lamp", deco=("core",))
    for i in range(4):                                           # floorboards hanging as a skirt, the middle ones longer, like a fence
        sh.cube("pf", (-4.0 + i * 2.1, 7.0 + (1.0 if i in (0, 3) else 0), -3.6), (2.0, 5.2 - (1.0 if i in (0, 3) else 0), 0.7), mat="oak", deco=("plank",), rot=(-4, 0, 0), pv=(0, 12.8, -2.8))
        sh.cube("pb", (-4.0 + i * 2.1, 7.4, 2.9), (2.0, 4.8, 0.7), mat="oak_dark", deco=("plank",), rot=(4, 0, 0), pv=(0, 12.8, 2.8))
    sh.cube("pl", (4.8, 8.4, -2.2), (0.7, 4.0, 4.4), mat="oak", deco=("plank",), rot=(0, 0, -4), pv=(4.8, 12.8, 0))
    sh.cube("pr", (-5.5, 8.4, -2.2), (0.7, 4.0, 4.4), mat="oak", deco=("plank",), rot=(0, 0, 4), pv=(-4.8, 12.8, 0))
    a = Anim()
    wave(a, "idle", "pf", 3.0, 12, x=S(2.5, 0.2))
    wave(a, "idle", "pb", 3.0, 12, x=S(-2.5, 0.2))
    wave(a, "idle", "pl", 3.0, 12, z=S(-2.0, 0.3))
    wave(a, "idle", "pr", 3.0, 12, z=S(2.0, 0.3))
    sh.save(a)


def pauldrons():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "pauldron_" + side, 128, 64, seed=231 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("lamp", "root", (sx * 8.8, 25.0, 0))
        specs = [
            dict(o=(-10.0, 21.4, -3.6), s=(5.0, 1.6, 7.2), rot=(0, 0, 10), pv=(-5.5, 22, 0), mat="oak", deco=("plank",)),
            dict(o=(-9.6, 22.8, -3.4), s=(4.6, 1.5, 6.8), rot=(0, 0, 18), pv=(-5.5, 22, 0), mat="tile"),
            dict(o=(-8.6, 24.1, -3.0), s=(3.8, 1.4, 6.0), rot=(0, 0, 28), pv=(-5.5, 22, 0), mat="tile"),
            dict(o=(-7.2, 25.2, -2.4), s=(2.8, 1.2, 4.8), rot=(0, 0, 40), pv=(-5.5, 22, 0), mat="tile"),
            dict(o=(-10.4, 20.8, -3.9), s=(1.0, 2.2, 7.8), mat="bull_iron", deco=("rivet",)),
            dict(o=(-6.4, 25.4, -1.0), s=(2.2, 1.0, 2.0), mat="stone"),
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
        sh.cube("lamp", (sx * 8.8 - 0.9, 25.6, -0.9), (1.8, 2.4, 1.8), mat="lamp", deco=("core",))
        sh.cube("lamp", (sx * 8.8 - 1.2, 28.0, -1.2), (2.4, 0.7, 2.4), mat="bull_iron")
        sh.cube("lamp", (sx * 8.8 - 1.2, 24.9, -1.2), (2.4, 0.7, 2.4), mat="bull_iron")
        a = Anim()
        wave(a, "idle", "lamp", 3.0, 12, z=S(5.0 * sx, 0.1), kind="rot")
        sh.save(a)


def vambraces():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "vambrace_" + side, 128, 64, seed=241 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        cx = sx * 6.0
        # a log sleeve: a half-log with rings, iron straps, a stone gauntlet block, an iron nail-studded cuff
        sh.cube("root", (cx - 2.7, 14.4, -2.7), (5.4, 4.8, 5.4), mat="oak", deco=("plank",))
        sh.cube("root", (cx - 2.9, 17.6, -2.9), (5.8, 0.9, 5.8), mat="bull_iron", deco=("rivet",))
        sh.cube("root", (cx - 2.9, 15.0, -2.9), (5.8, 0.9, 5.8), mat="bull_iron", deco=("rivet",))
        sh.cube("root", (cx - 2.7, 12.0, -2.7), (5.4, 2.4, 5.4), mat="stone")
        sh.cube("root", (cx - 2.2, 11.4, -3.1), (4.4, 1.0, 1.0), mat="bull_iron", deco=("rivet",))
        sh.cube("root", (cx - 2.4, 18.4, -2.4), (4.8, 1.2, 4.8), mat="tile")
        sh.cube("root", (cx + sx * 2.6 - 0.4, 14.8, -1.4), (0.8, 2.6, 2.8), mat="lamp", deco=("window",), window_faces=("west", "east"))
        sh.save(Anim())


def greaves():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet(KEY, "greave_" + side, 128, 64, seed=251 + (sx > 0))
        sh.bone("root", pivot=(0, 12, 0))
        cx = sx * 2.0
        # stilt boots: oak shin posts with iron brackets, a foundation block as the boot, a roof-tile knee cap
        sh.cube("root", (cx - 2.6, 2.6, -2.9), (5.2, 5.6, 5.8), mat="oak", deco=("plank",))
        sh.cube("root", (cx - 2.8, 7.8, -3.1), (5.6, 1.0, 6.2), mat="bull_iron", deco=("rivet",))
        sh.cube("root", (cx - 2.2, 8.4, -3.6), (4.4, 2.6, 1.2), mat="tile")
        sh.cube("root", (cx - 2.8, 4.6, -3.1), (5.6, 0.8, 6.2), mat="bull_iron", deco=("rivet",))
        sh.cube("root", (cx - 3.0, 0.0, -4.8), (6.0, 2.6, 8.0), mat="stone")
        sh.cube("root", (cx - 2.6, 2.4, -4.2), (5.2, 0.8, 1.2), mat="bull_iron", deco=("rivet",))
        sh.save(Anim())


def back():
    """The magic house on the back: two storeys, a gabled roof, a chimney with smoke puffs rising, a glowing window; three rooms circling it."""
    sh = Sheet(KEY, "back", 256, 128, seed=261)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("puff1", "root", (0, 34, 6))
    sh.bone("puff2", "root", (0, 34, 6))
    sh.bone("puff3", "root", (0, 34, 6))
    sh.bone("rooms", "root", (0, 19.0, 6.0))
    sh.bone("flag", "root", (0, 38.0, 6.5))
    sh.cube("root", (-4.0, 12.6, 2.8), (8.0, 1.6, 7.0), mat="stone")
    sh.cube("root", (-3.8, 14.2, 3.0), (7.6, 4.6, 6.6), mat="oak", deco=("plank",))
    sh.cube("root", (-3.6, 18.8, 3.2), (7.2, 4.4, 6.2), mat="oak_dark", deco=("plank",))
    sh.cube("root", (-4.4, 23.0, 2.6), (8.8, 1.4, 7.6), mat="tile")
    sh.cube("root", (-3.8, 24.4, 3.0), (7.6, 1.4, 6.8), mat="tile")
    sh.cube("root", (-3.0, 25.8, 3.6), (6.0, 1.3, 5.6), mat="tile")
    sh.cube("root", (-2.0, 27.1, 4.2), (4.0, 1.2, 4.4), mat="tile")
    sh.cube("root", (-1.2, 28.3, 4.8), (2.4, 1.0, 3.2), mat="tile")
    sh.cube("root", (-2.4, 19.4, 9.7), (2.0, 2.4, 0.5), mat="lamp", deco=("window",), window_faces=("south",))
    sh.cube("root", (0.4, 19.4, 9.7), (2.0, 2.4, 0.5), mat="lamp", deco=("window",), window_faces=("south",))
    sh.cube("root", (-1.2, 14.2, 9.6), (2.4, 3.6, 0.5), mat="oak_dark", deco=("plank",))
    sh.cube("root", (1.6, 28.0, 6.6), (2.4, 4.4, 2.4), mat="stone")
    sh.cube("root", (1.2, 32.2, 6.2), (3.2, 0.9, 3.2), mat="bull_iron")
    sh.cube("root", (-0.7, 12.6, 1.8), (1.4, 11.0, 1.2), mat="bull_iron", deco=("rivet",))
    sh.cube("root", (-3.0, 22.4, 1.8), (6.0, 1.0, 1.2), mat="bull_iron", deco=("rivet",))
    for k in range(3):
        sh.cube("puff%d" % (k + 1), (2.0, 33.4, 6.9), (1.4 + 0.3 * k, 1.4 + 0.3 * k, 1.4 + 0.3 * k), mat="stone", deco=("core",), tone=0.2)
    sh.cube("flag", (-0.3, 36.4, 6.3), (0.6, 2.6, 0.6), mat="bull_iron")
    sh.cube("flag", (0.3, 37.4, 6.4), (3.6, 1.6, 0.4), mat="lamp", deco=("core",))
    for k in range(3):                                           # the rooms: small timber cubes with a window, circling the house
        a = 2 * math.pi * k / 3
        x, y = 11.0 * math.cos(a), 11.0 * math.sin(a)
        sh.cube("rooms", (x - 1.6, 19.0 + y - 1.6, 4.8), (3.2, 3.2, 3.2), mat="oak", deco=("plank",))
        sh.cube("rooms", (x - 2.0, 19.0 + y + 1.4, 4.4), (4.0, 0.9, 4.0), mat="tile")
        sh.cube("rooms", (x - 0.8, 19.0 + y - 0.8, 8.0), (1.6, 1.6, 0.4), mat="lamp", deco=("window",), window_faces=("south",))
    a = Anim()
    frac = lambda x: x - math.floor(x)
    for k in range(3):                      # five smoke puffs per 14 s loop per chimney slot, a third of a puff apart
        ph = lambda p, k=k: frac(p * 5 + k / 3.0)
        wave(a, "idle", "puff%d" % (k + 1), 14.0, 70, kind="pos", x=lambda p, ph=ph: 1.2 * ph(p) * math.sin(6 * ph(p)), y=lambda p, ph=ph: 7.0 * ph(p), z=None)
        wave(a, "idle", "puff%d" % (k + 1), 14.0, 70, kind="scale", x=lambda p, ph=ph: 0.2 + 1.3 * math.sin(math.pi * ph(p)),
             y=lambda p, ph=ph: 0.2 + 1.3 * math.sin(math.pi * ph(p)), z=lambda p, ph=ph: 0.2 + 1.3 * math.sin(math.pi * ph(p)))
    wave(a, "idle", "rooms", 14.0, 14, z=lambda p: 360 * p)
    wave(a, "idle", "flag", 2.0, 12, y=S(8.0))
    sh.save(a)


def halo():
    sh = Sheet(KEY, "halo", 128, 64, seed=271)
    sh.bone("root", pivot=(0, 36.0, 0))
    sh.bone("spin", "root", (0, 36.0, 0))
    for k in range(6):                                           # six loose planks circling the head, each tilted a little
        a = 2 * math.pi * k / 6
        x, z = 6.0 * math.cos(a), 6.0 * math.sin(a)
        sh.cube("spin", (x - 1.6, 35.4 + (k % 2) * 1.2, z - 0.4), (3.2, 0.6, 0.8), mat="oak" if k % 2 else "oak_dark", deco=("plank",), rot=(0, -math.degrees(a), 0), pv=(x, 35.7, z))
    sh.cube("spin", (-0.6, 36.4, -0.6), (1.2, 1.2, 1.2), mat="lamp", deco=("core",))
    a = Anim()
    wave(a, "idle", "spin", 9.0, 18, y=lambda p: 360 * p)
    wave(a, "idle", "root", 3.0, 12, kind="pos", y=S(0.5))
    sh.save(a)


# ================================================================================================================================ the constructs
def bull():
    """The Raging Black Bull: the magic house rebuilt as a bull. Foundation-stone hooves, oak legs, a house for a body (tile roof back, glowing windows, a chimney), an iron-horned head, cannons."""
    sh = Sheet(KEY, "bull", 384, 192, seed=281)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("body", "root", (0, 22, 0))
    sh.bone("neck", "body", (0, 28, -14))
    sh.bone("head", "neck", (0, 28, -20))
    sh.bone("jaw", "head", (0, 22, -28))
    sh.bone("horn_l", "head", (6.5, 33, -22))
    sh.bone("horn_r", "head", (-6.5, 33, -22))
    sh.bone("tail", "body", (0, 24, 17))
    sh.bone("chimney", "body", (6, 38, 4))
    sh.bone("cannon_l", "body", (11, 30, -2))
    sh.bone("cannon_r", "body", (-11, 30, -2))
    for k, (x, z) in {"fr": (-7, -12), "fl": (7, -12), "br": (-7, 11), "bl": (7, 11)}.items():
        sh.bone("leg_" + k, "body", (x, 22, z))
        sh.bone("shin_" + k, "leg_" + k, (x, 11, z))
    # body: two storeys of a house laid on its side as a barrel chest, tile roof over the back, a stone floor under the belly
    sh.cube("body", (-9.0, 18.0, -16.0), (18.0, 6.0, 34.0), mat="stone")
    sh.cube("body", (-10.0, 24.0, -15.0), (20.0, 10.0, 32.0), mat="oak", deco=("plank",))
    sh.cube("body", (-11.0, 33.6, -16.0), (22.0, 2.0, 34.0), mat="oak_dark", deco=("plank",))
    sh.cube("body", (-10.4, 35.6, -15.0), (20.8, 2.0, 32.0), mat="tile")
    sh.cube("body", (-8.4, 37.6, -13.0), (16.8, 2.0, 28.0), mat="tile")
    sh.cube("body", (-5.4, 39.6, -10.0), (10.8, 2.0, 22.0), mat="tile")
    sh.cube("body", (-1.0, 41.4, -8.0), (2.0, 1.4, 18.0), mat="bull_iron", deco=("rivet",))
    for i in range(3):
        for sx in (-1, 1):
            xo = 10.0 if sx > 0 else -10.6
            sh.cube("body", (xo, 27.0, -11.0 + i * 11.0), (0.7, 5.0, 6.0), mat="lamp", deco=("window",), window_faces=("east", "west"))
            sh.cube("body", (xo - 0.1 * sx, 26.4, -11.6 + i * 11.0), (0.9, 6.2, 0.9), mat="bull_iron")
    sh.cube("body", (-3.0, 22.0, 15.6), (6.0, 9.0, 1.0), mat="oak_dark", deco=("plank", "rivet"))     # a door on the rump
    sh.cube("body", (-9.6, 23.4, -15.6), (19.2, 1.0, 1.0), mat="bull_iron", deco=("rivet",))
    sh.cube("body", (-9.6, 33.0, -15.6), (19.2, 1.0, 1.0), mat="bull_iron", deco=("rivet",))
    sh.cube("chimney", (3.6, 38.4, 2.0), (5.0, 8.0, 5.0), mat="stone")
    sh.cube("chimney", (3.0, 46.0, 1.4), (6.2, 1.2, 6.2), mat="bull_iron")
    # neck and head
    sh.cube("neck", (-7.0, 26.0, -22.0), (14.0, 14.0, 10.0), mat="oak", deco=("plank",))
    sh.cube("neck", (-7.6, 36.0, -23.0), (15.2, 2.0, 12.0), mat="tile")
    sh.cube("head", (-6.0, 23.0, -34.0), (12.0, 12.0, 14.0), mat="bull_iron", deco=("seam", "rivet"))
    sh.cube("head", (-5.0, 18.6, -40.0), (10.0, 6.0, 8.0), mat="bull_iron", deco=("seam",))
    sh.cube("head", (-4.8, 18.2, -41.0), (3.6, 3.0, 1.2), mat="lamp", deco=("core",))
    sh.cube("head", (1.2, 18.2, -41.0), (3.6, 3.0, 1.2), mat="lamp", deco=("core",))
    sh.cube("head", (-5.4, 30.0, -35.0), (3.2, 2.4, 1.2), mat="lamp", deco=("core",))
    sh.cube("head", (2.2, 30.0, -35.0), (3.2, 2.4, 1.2), mat="lamp", deco=("core",))
    sh.cube("head", (-6.6, 34.6, -28.0), (13.2, 1.6, 8.0), mat="tile")
    sh.cube("jaw", (-4.6, 16.0, -39.0), (9.2, 2.6, 8.0), mat="stone")
    for sx, bone in ((1, "horn_l"), (-1, "horn_r")):
        x0 = sx * 6.5
        sh.cube(bone, (x0 - 1.6, 32.4, -23.6), (3.2, 3.2, 3.2), mat="stone")
        sh.cube(bone, (x0 + sx * 2.6 - 1.4, 33.0, -23.4), (2.8, 2.8, 2.8), mat="bull_iron", rot=(0, 0, -sx * 18), pv=(x0, 33.6, -22))
        sh.cube(bone, (x0 + sx * 5.4 - 1.1, 34.8, -23.0), (2.2, 5.0, 2.2), mat="bull_iron", rot=(0, 0, -sx * 48), pv=(x0, 33.6, -22))
        sh.cube(bone, (x0 + sx * 7.6 - 0.8, 38.8, -22.8), (1.6, 5.0, 1.6), mat="stone", rot=(-14, 0, -sx * 72), pv=(x0, 33.6, -22))
    # legs
    for k, (x, z) in {"fr": (-7, -12), "fl": (7, -12), "br": (-7, 11), "bl": (7, 11)}.items():
        sh.cube("leg_" + k, (x - 3.4, 10.0, z - 3.6), (6.8, 13.0, 7.2), mat="oak", deco=("plank",))
        sh.cube("leg_" + k, (x - 3.8, 18.6, z - 4.0), (7.6, 1.2, 8.0), mat="bull_iron", deco=("rivet",))
        sh.cube("shin_" + k, (x - 3.0, 2.2, z - 3.2), (6.0, 8.8, 6.4), mat="oak_dark", deco=("plank",))
        sh.cube("shin_" + k, (x - 4.0, 0.0, z - 4.4), (8.0, 3.0, 8.8), mat="stone")
    sh.cube("tail", (-1.0, 22.0, 17.0), (2.0, 2.0, 8.0), mat="bull_iron", deco=("rivet",))
    sh.cube("tail", (-1.6, 20.0, 24.6), (3.2, 3.2, 3.2), mat="lamp", deco=("core",))
    # cannons: opened on the sides for the Ultra Giant Bull; short barrels
    for sx, bone in ((1, "cannon_l"), (-1, "cannon_r")):
        x0 = sx * 11.0
        sh.cube(bone, (x0 + (0.0 if sx > 0 else -3.0) - 0.0, 28.4, -5.0), (3.0, 3.6, 3.6), mat="bull_iron", deco=("rivet",))
        sh.cube(bone, (x0 + sx * 3.0 - 1.2 + (0.6 if sx < 0 else 0), 28.8, -4.6), (2.4, 2.8, 2.8), mat="stone")
        sh.cube(bone, (x0 + sx * 4.6 - 0.8 + (0 if sx > 0 else -0.4), 29.4, -4.0), (1.6, 1.6, 1.6), mat="lamp", deco=("core",))
    a = Anim()
    ph = lambda p: math.sin(2 * math.pi * p)
    L = 3.2
    wave(a, "idle", "body", L, 12, kind="pos", y=S(0.5))
    wave(a, "idle", "neck", L, 12, x=S(2.0, 0.1))
    wave(a, "idle", "head", L, 12, x=S(2.5, 0.35), y=S(2.0))
    wave(a, "idle", "tail", L, 12, y=S(14.0, 0.2))
    wave(a, "idle", "chimney", L, 12, z=S(0.8))
    wave(a, "idle", "horn_l", L, 12, z=S(1.5, 0.1))
    wave(a, "idle", "horn_r", L, 12, z=S(-1.5, 0.1))
    L = 1.1
    for k, sgn in (("fr", 1), ("bl", 1), ("fl", -1), ("br", -1)):
        wave(a, "walk", "leg_" + k, L, 12, x=lambda p, s=sgn: 26 * s * ph(p))
        wave(a, "walk", "shin_" + k, L, 12, x=lambda p, s=sgn: -30 * max(0.0, s * math.cos(2 * math.pi * p)))
    wave(a, "walk", "body", L, 12, kind="pos", y=lambda p: 0.8 * math.cos(4 * math.pi * p) - 0.8)
    wave(a, "walk", "body", L, 12, z=lambda p: 2.0 * ph(p))
    wave(a, "walk", "head", L, 12, x=lambda p: 5 * math.cos(4 * math.pi * p) + 4)
    wave(a, "walk", "tail", L, 12, y=S(20.0, 0.1))
    # charge: the head drops, the legs dig, the whole body lunges forward and the horns come up at the end
    keys(a, "charge", "head", "rot", {0.0: (0, 0, 0), 0.25: (28, 0, 0), 0.55: (30, 0, 0), 0.75: (-16, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "charge", "neck", "rot", {0.0: (0, 0, 0), 0.25: (14, 0, 0), 0.55: (16, 0, 0), 0.75: (-10, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "charge", "body", "pos", {0.0: (0, 0, 0), 0.25: (0, -1.5, 3), 0.55: (0, -1, -6), 0.75: (0, 0, -3), 1.1: (0, 0, 0)})
    keys(a, "charge", "body", "rot", {0.0: (0, 0, 0), 0.25: (-5, 0, 0), 0.55: (6, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "charge", "leg_fr", "rot", {0.0: (0, 0, 0), 0.25: (-24, 0, 0), 0.55: (30, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "charge", "leg_fl", "rot", {0.0: (0, 0, 0), 0.25: (-24, 0, 0), 0.55: (30, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "charge", "leg_br", "rot", {0.0: (0, 0, 0), 0.25: (20, 0, 0), 0.55: (-30, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "charge", "leg_bl", "rot", {0.0: (0, 0, 0), 0.25: (20, 0, 0), 0.55: (-30, 0, 0), 1.1: (0, 0, 0)})
    keys(a, "charge", "tail", "rot", {0.0: (0, 0, 0), 0.25: (-24, 0, 0), 0.6: (26, 0, 0), 1.1: (0, 0, 0)})
    a.clips["charge"]["animation_length"] = 1.1
    keys(a, "fire", "cannon_l", "pos", {0.0: (0, 0, 0), 0.1: (-1.4, 0, 1.6), 0.5: (0, 0, 0)})
    keys(a, "fire", "cannon_r", "pos", {0.0: (0, 0, 0), 0.1: (1.4, 0, 1.6), 0.5: (0, 0, 0)})
    keys(a, "fire", "body", "rot", {0.0: (0, 0, 0), 0.1: (-2, 0, 0), 0.5: (0, 0, 0)})
    a.clips["fire"]["animation_length"] = 0.5
    # assemble: the house rebuilds itself into the bull (the whole model grows out of the ground, the head comes up last)
    keys(a, "assemble", "root", "scale", {0.0: (1, 0.05, 1), 0.7: (1.0, 0.8, 1.0), 1.1: (1.04, 1.06, 1.04), 1.3: (1, 1, 1)}, loop=False)
    keys(a, "assemble", "head", "rot", {0.0: (50, 0, 0), 0.9: (50, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    keys(a, "assemble", "body", "pos", {0.0: (0, -10, 0), 0.8: (0, 0, 0), 1.3: (0, 0, 0)}, loop=False)
    a.clips["assemble"]["animation_length"] = 1.3
    # collapse: the bull folds back into planks
    keys(a, "collapse", "body", "pos", {0.0: (0, 0, 0), 0.6: (0, -6, 0), 1.2: (0, -16, 0)}, loop=False)
    keys(a, "collapse", "head", "rot", {0.0: (0, 0, 0), 0.6: (30, 0, 8), 1.2: (60, 0, 16)}, loop=False)
    keys(a, "collapse", "root", "scale", {0.0: (1, 1, 1), 0.8: (1, 1, 1), 1.2: (1.1, 0.3, 1.1)}, loop=False)
    a.clips["collapse"]["animation_length"] = 1.2
    sh.save(a)


def room():
    """A recombined room: a timber cube with a gabled tile roof, a door, windows, a chimney. Used as the Bulwark's corner rooms and the Room Swap's markers."""
    sh = Sheet(KEY, "room", 160, 96, seed=291)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("house", "root", (0, 0, 0))
    sh.bone("smoke", "house", (4.5, 24, 3.0))
    sh.cube("house", (-8.0, 0.0, -8.0), (16.0, 3.0, 16.0), mat="stone")
    sh.cube("house", (-7.4, 3.0, -7.4), (14.8, 9.0, 14.8), mat="oak", deco=("plank",))
    sh.cube("house", (-8.2, 12.0, -8.2), (16.4, 1.6, 16.4), mat="oak_dark", deco=("plank",))
    sh.cube("house", (-8.6, 13.6, -8.6), (17.2, 1.6, 17.2), mat="tile")
    sh.cube("house", (-7.0, 15.2, -7.0), (14.0, 1.6, 14.0), mat="tile")
    sh.cube("house", (-5.0, 16.8, -5.0), (10.0, 1.6, 10.0), mat="tile")
    sh.cube("house", (-3.0, 18.4, -3.0), (6.0, 1.6, 6.0), mat="tile")
    sh.cube("house", (-1.0, 20.0, -1.0), (2.0, 1.2, 2.0), mat="tile")
    sh.cube("house", (-2.0, 3.0, -7.8), (4.0, 6.6, 0.6), mat="oak_dark", deco=("plank", "rivet"))
    for sx in (-1, 1):
        sh.cube("house", (sx * 4.6 - 1.6 - 0.0, 6.6, -7.9), (3.2, 3.2, 0.5), mat="lamp", deco=("window",), window_faces=("north",))
        sh.cube("house", (sx * 7.4 - (0.3 if sx > 0 else 0.1), 6.0, -2.0), (0.6, 3.6, 4.0), mat="lamp", deco=("window",), window_faces=("east", "west"))
    sh.cube("house", (-8.0, 3.0, -8.0), (1.0, 9.0, 1.0), mat="bull_iron", deco=("rivet",))
    sh.cube("house", (7.0, 3.0, -8.0), (1.0, 9.0, 1.0), mat="bull_iron", deco=("rivet",))
    sh.cube("house", (3.0, 14.0, 1.0), (3.0, 7.0, 3.0), mat="stone")
    sh.cube("smoke", (3.0, 21.0, 1.0), (3.0, 1.0, 3.0), mat="bull_iron")
    a = Anim()
    keys(a, "rise", "house", "scale", {0.0: (1, 0.05, 1), 0.3: (1.06, 1.12, 1.06), 0.5: (1, 1, 1)}, loop=False)
    keys(a, "rise", "house", "pos", {0.0: (0, -6, 0), 0.3: (0, 0, 0), 0.5: (0, 0, 0)}, loop=False)
    a.clips["rise"]["animation_length"] = 0.5
    wave(a, "idle", "house", 3.0, 12, kind="pos", y=S(0.3))
    wave(a, "idle", "smoke", 3.0, 12, kind="pos", y=S(1.0, 0.3))
    keys(a, "swap", "house", "scale", {0.0: (1, 1, 1), 0.2: (0.6, 1.3, 0.6), 0.4: (1.2, 0.8, 1.2), 0.6: (1, 1, 1)}, loop=False)
    a.clips["swap"]["animation_length"] = 0.6
    sh.save(a)


def drill():
    """The mana corkscrew: a drill of stacked, turning rings of compressed mana in a timber-and-iron casing, point first (+y is the point)."""
    sh = Sheet(KEY, "drill", 128, 96, seed=301)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("spin", "root", (0, 12, 0))
    sh.cube("root", (-2.2, 0.0, -2.2), (4.4, 4.0, 4.4), mat="bull_iron", deco=("rivet",))
    sh.cube("root", (-3.0, 4.0, -3.0), (6.0, 1.2, 6.0), mat="oak_dark", deco=("plank",))
    for i, (w, h, y) in enumerate(((5.6, 3.0, 5.2), (4.8, 3.0, 8.2), (4.0, 3.0, 11.2), (3.2, 3.0, 14.2), (2.4, 3.0, 17.2), (1.6, 2.6, 20.2), (0.8, 2.2, 22.8))):
        sh.cube("spin", (-w / 2, y, -w / 2), (w, h, w), mat="lamp" if i % 2 == 0 else "bull_iron", deco=("core",) if i % 2 == 0 else ("seam",), rot=(0, 22 * i, 0), pv=(0, y + h / 2, 0))
    for k in range(3):
        a = 2 * math.pi * k / 3
        sh.cube("spin", (math.cos(a) * 3.2 - 0.5, 7.0, math.sin(a) * 3.2 - 0.5), (1.0, 10.0, 1.0), mat="bull_iron", rot=(0, 0, 0))
    an = Anim()
    wave(an, "idle", "spin", 0.5, 10, y=lambda p: 360 * p)
    sh.save(an)


if __name__ == "__main__":
    for fn in (helm, chest, tasset, pauldrons, vambraces, greaves, back, halo, bull, room, drill):
        fn()

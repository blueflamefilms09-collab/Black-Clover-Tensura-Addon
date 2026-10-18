"""Constructs and summons of the earth family (1.93.0 rebuild): the things the spells of Earth, Rock, Stone, Sandstone, Red Ochre and Mineral magic put in the world.

    python3 -B tools/gen_earth_family_constructs.py [name ...]
    python3 -B tools/geo_check.py earth_golem rock_titan stone_sentinel sandstone_sphinx sandstone_cage ochre_totem mineral_spire rock_boulder mineral_lance

Models (GeoSpec.of(<key>, <name>); the key is the first word of the file name, tools/geo_builder.py writes geo/entity/<key>_<name>.geo.json,
animations/entity/..., textures/entity/<key>_<name>.png + _glow.png):
    earth_golem        Clay Guardian of Earth Magic: a mud-and-grass colossus, 44 px tall, idle / walk / attack (a two-handed slam)
    rock_titan         Boulder Titan of Rock Magic: hunched crag giant with fists to its knees, orbiting rocks, idle / walk / attack / roar
    stone_sentinel     Stone Sentinel: a masonry knight with a tower shield and a block hammer, idle / walk / attack / block
    sandstone_sphinx   Sandstone Sphinx: a quadruped guardian of layered sandstone with a nemes, idle / walk / attack (a pounce)
    sandstone_cage     the cage of the Variable Hardness Prison: a block of five slabs that closes round the target (form / idle / crumble)
    ochre_totem        Ochre Totem: three stacked painted faces, wings, a beak that opens, a sun crown that turns (idle / attack)
    mineral_spire      Mineral Spire: a crystal growth out of ore matrix, one tall prism and a ring of satellites (grow / idle / pulse)
    rock_boulder       the boulder of Rock Bolt (tumbling, idle)
    mineral_lance      the crystal lance of Mineral Lance (a long prism, idle)

Prop space: feet at the origin, y up, the model's front (-z in the file) looks along the prop's forward. Rotation signs follow GeoDraw: a positive x rotation
swings a hanging limb FORWARD, a positive z rotation toward +x.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from earth_family_lib import *  # noqa: E402,F401,F403
from gen_earth_family_armor import prism, aim, ring, fan  # noqa: E402


def humanoid_bones(hunch=0.0, y0=0.0):
    b = [("pelvis", "root", (0, 18, 0)), ("thigh_r", "pelvis", (-5, 18, 0)), ("shin_r", "thigh_r", (-5, 9, 0)), ("thigh_l", "pelvis", (5, 18, 0)), ("shin_l", "thigh_l", (5, 9, 0)),
         ("chest", "pelvis", (0, 20, 0), (-hunch, 0, 0) if hunch else None), ("head", "chest", (0, 38, 0)),
         ("arm_r", "chest", (-11, 36, 0)), ("forearm_r", "arm_r", (-12, 26, 0)), ("arm_l", "chest", (11, 36, 0)), ("forearm_l", "arm_l", (12, 26, 0))]
    return [x if (len(x) < 4 or x[3]) else x[:3] for x in b]


def humanoid_anim(an, walk=30.0, swing=0.0, slam=True, hunch=0.0):
    wave(an, "idle", "chest", 3.0, steps=12, y=S(0.6), kind="pos")
    wave(an, "idle", "arm_r", 3.0, steps=12, z=S(2.5, 0.0), x=S(3.0, 0.25), kind="rot")
    wave(an, "idle", "arm_l", 3.0, steps=12, z=S(-2.5, 0.0), x=S(3.0, 0.75), kind="rot")
    wave(an, "idle", "head", 3.0, steps=12, y=S(4.0, 0.0), kind="rot")
    L = 0.9
    wave(an, "walk", "thigh_r", L, x=S(walk, 0.0), kind="rot")
    wave(an, "walk", "thigh_l", L, x=S(walk, 0.5), kind="rot")
    wave(an, "walk", "shin_r", L, x=lambda p: -0.5 * walk * (0.5 + 0.5 * math.sin(math.tau * (p + 0.25))), kind="rot")
    wave(an, "walk", "shin_l", L, x=lambda p: -0.5 * walk * (0.5 + 0.5 * math.sin(math.tau * (p + 0.75))), kind="rot")
    wave(an, "walk", "arm_r", L, x=S(-0.8 * walk, 0.0), kind="rot")
    wave(an, "walk", "arm_l", L, x=S(-0.8 * walk, 0.5), kind="rot")
    wave(an, "walk", "chest", L, y=S(0.9, 0.0, 2), kind="pos")
    wave(an, "walk", "head", L, y=S(5.0, 0.0), kind="rot")
    if slam:
        for arm in ("arm_r", "arm_l"):
            keys(an, "attack", arm, "rot", {0.0: (0, 0, 0), 0.30: (150, 0, 0), 0.42: (150, 0, 0), 0.52: (20, 0, 0), 0.95: (0, 0, 0)}, length=0.95, loop=False)
        keys(an, "attack", "chest", "rot", {0.0: (0, 0, 0), 0.30: (-8, 0, 0), 0.52: (16, 0, 0), 0.95: (0, 0, 0)}, length=0.95, loop=False)
        keys(an, "attack", "head", "rot", {0.0: (0, 0, 0), 0.30: (-12, 0, 0), 0.52: (10, 0, 0), 0.95: (0, 0, 0)}, length=0.95, loop=False)


# ============================================================================================================================ EARTH GOLEM
def earth_golem():
    rng = np.random.default_rng(2101)
    G = (210, 230, 90)
    E = lambda *a, **k: spec(*a, mat="earth", **k)
    sp = [E((-6, 16, -4), (12, 6, 8), bone="pelvis", grass_side=0.2)]
    for sd, nm in ((-1, "r"), (1, "l")):
        x0 = -8 if sd < 0 else 2
        sp += [E((x0, 9, -3.5), (6, 9, 7), bone="thigh_" + nm), E((x0 - 0.5, 0, -4.5), (7, 9.6, 9), bone="shin_" + nm, grass_side=0.3),
               E((x0 - 0.5, 8.6, -5.0), (7, 2.4, 2.6), bone="shin_" + nm, grass=1.0)]
    sp += [E((-9, 22, -5.5), (18, 14, 11), bone="chest", grass_side=0.5), E((-9.6, 35, -6.2), (19.2, 2.4, 12.4), bone="chest", grass=1.0),
           E((-8, 20, 4.8), (16, 12, 2.4), bone="chest", grass_side=0.8), spec((-2.2, 27, -6.4), (4.4, 4.4, 1.0), bone="chest", mat="earth", shine=1, glow=G),
           spec((-1.2, 28, -6.8), (2.4, 2.4, 0.8), bone="chest", mat="core", glow=G, pal=PAL["earth"])]
    sp += [prism((-6 + i * 3.0, 37.2, -3 + (i % 2) * 5.0), ((i - 2) * 0.2, 1.0, 0.0), 4.0 + (i % 3), 1.4, "grass", bone="chest") for i in range(5)]
    sp += [prism((-7 + i * 3.5, 22.0, 6.8), (0.0, -1.0, 0.3), 9.0 + (i % 2) * 3, 1.5, "earth", bone="chest") for i in range(5)]                  # roots hanging down the back
    sp += [E((-4.5, 36, -4.5), (9, 9, 9), bone="head", grass_side=0.3), E((-5.2, 43.4, -5.2), (10.4, 2, 10.4), bone="head", grass=1.0), E((-4.4, 40.6, -5.6), (8.8, 2, 2.8), bone="head"),
           spec((-3.6, 38.4, -5.2), (2.4, 1.6, 0.8), bone="head", mat="eye", glow=G), spec((1.2, 38.4, -5.2), (2.4, 1.6, 0.8), bone="head", mat="eye", glow=G)]
    sp += [prism((-3.4 + i * 2.2, 44.6, -1 + (i % 2) * 2.6), ((i - 1) * 0.3, 1.0, 0.0), 3.4, 1.0, "grass", bone="head") for i in range(4)]
    for sd, nm in ((-1, "r"), (1, "l")):
        x0 = -17 if sd < 0 else 11
        sp += [E((x0, 29, -4), (6, 9, 8), bone="arm_" + nm, grass_side=0.4), E((x0 + (0 if sd < 0 else 0), 33.6, -4.6), (6, 2, 9.2), bone="arm_" + nm, grass=1.0)]
        sp += [E((x0 - 1.5, 15.5, -5.2), (9, 13, 10.4), bone="forearm_" + nm)] + jagspecs(rng, x0 + 3, 16, -2, 4.0, 3, 4, 3, 2, 3.4, mat="earth", bone="forearm_" + nm)
    an = Anim()
    humanoid_anim(an, walk=26.0)
    return "earth", "golem", sp, humanoid_bones(), an


# ============================================================================================================================ ROCK TITAN
def rock_titan():
    rng = np.random.default_rng(2203)
    G = (255, 150, 60)
    R = lambda *a, **k: spec(*a, mat="rock", **k)
    sp = [R((-7, 15, -5), (14, 8, 10), bone="pelvis", glow=G)]
    for sd, nm in ((-1, "r"), (1, "l")):
        x0 = -9 if sd < 0 else 2
        sp += [R((x0, 7, -4), (7, 11, 8), bone="thigh_" + nm, glow=G), R((x0 - 0.8, 0, -5), (8.6, 8.4, 10), bone="shin_" + nm, glow=G)]
        sp += jagspecs(rng, x0 + 3.5, 6, -4.5, 3, 2.5, 1.2, 3, 2, 4, mat="rock", bone="shin_" + nm)
    sp += [R((-11, 21, -6.5), (22, 15, 13), bone="chest", glow=G, cracks=10), R((-9, 21.5, -8), (18, 9, 2.4), bone="chest", glow=G), R((-10, 18, 6), (20, 17, 3), bone="chest", glow=G, cracks=8)]
    sp += jagspecs(rng, 0, 38.5, 0, 9, 2.4, 6, 8, 3, 6, mat="rock", bone="chest", glow=G, cracks=2) + jagspecs(rng, 0, 22, 8.4, 8, 6, 2, 6, 3, 6, mat="rock", bone="chest")
    sp += [R((-5, 36, -5.5), (10, 8, 10), bone="head", glow=G), R((-5.6, 41.2, -6.4), (11.2, 2.6, 3.2), bone="head"), R((-4.4, 33.4, -6.6), (8.8, 3.0, 3.0), bone="head"),
           spec((-3.8, 38.2, -6.4), (2.4, 1.6, 0.8), bone="head", mat="eye", glow=G), spec((1.4, 38.2, -6.4), (2.4, 1.6, 0.8), bone="head", mat="eye", glow=G)]
    sp += jagspecs(rng, 0, 45, -1, 4, 1.4, 4, 4, 2, 4, mat="rock", bone="head")
    for sd, nm in ((-1, "r"), (1, "l")):
        x0 = -20 if sd < 0 else 12
        sp += jagspecs(rng, x0 + 4, 36, 0, 3.4, 2.6, 3.4, 5, 4, 7, mat="rock", bone="arm_" + nm, glow=G, cracks=2)
        sp += [R((x0 + 0.5, 22, -4.5), (7, 14, 9), bone="forearm_" + nm, glow=G)] + jagspecs(rng, x0 + 4, 14, -1, 5, 5, 5, 5, 4, 8, mat="rock", bone="forearm_" + nm, glow=G, cracks=3)
    orb = []
    for i in range(5):
        a = i * math.tau / 5
        orb += jagspecs(rng, math.sin(a) * 22, 26 + (i % 2) * 8, math.cos(a) * 22, 0.5, 0.5, 0.5, 1, 4, 6, mat="rock", bone="orbit", glow=G, cracks=2)
    an = Anim()
    humanoid_anim(an, walk=22.0, hunch=14.0)
    for arm in ("forearm_r", "forearm_l"):                      # the titan's roar: both fists beat the chest
        keys(an, "roar", arm, "rot", {0.0: (0, 0, 0), 0.25: (70, 0, 0), 0.4: (40, 0, 0), 0.55: (70, 0, 0), 0.7: (40, 0, 0), 1.2: (0, 0, 0)}, length=1.2, loop=False)
    keys(an, "roar", "head", "rot", {0.0: (0, 0, 0), 0.25: (-24, 0, 0), 1.0: (-24, 0, 0), 1.2: (0, 0, 0)}, length=1.2, loop=False)
    keys(an, "idle", "orbit", "rot", {0.0: (0, 0, 0), 6.0: (0, 180, 0), 12.0: (0, 360, 0)}, length=12.0, loop=True)
    keys(an, "walk", "orbit", "rot", {0.0: (0, 0, 0), 3.0: (0, 180, 0), 6.0: (0, 360, 0)}, length=6.0, loop=True)
    for nm, pv in (("idle", None),):
        pass
    bones = humanoid_bones(hunch=14.0) + [("orbit", "root", (0, 30, 0))]
    return "rock", "titan", sp, bones, an


# ============================================================================================================================ STONE SENTINEL
def stone_sentinel():
    G = (150, 230, 255)
    T = lambda *a, **k: spec(*a, mat="stone", **k)
    sp = [T((-6, 16, -4), (12, 6, 8), bone="pelvis", course=4)]
    for sd, nm in ((-1, "r"), (1, "l")):
        x0 = -8 if sd < 0 else 2
        sp += [T((x0, 9, -3.5), (6, 9, 7), bone="thigh_" + nm, course=4), T((x0 - 0.5, 0, -5), (7, 9.8, 10), bone="shin_" + nm, course=4), T((x0 - 0.7, 8.8, -5.4), (7.4, 2.6, 3), bone="shin_" + nm, smooth=1)]
    sp += [T((-8.5, 22, -5), (17, 14, 10), bone="chest", course=5), T((-9.5, 33, -5.6), (19, 3.4, 11.2), bone="chest", smooth=1), spec((-3.5, 24.5, -5.8), (7, 7, 1.2), bone="chest", mat="stone", smooth=1, rune=1, glow=G),
           T((-8, 20, 4), (16, 14, 2.4), bone="chest", rune=1, glow=G)]
    sp += [T((-5, 36, -5), (10, 9, 10), bone="head", course=4), T((-5.4, 43.6, -5.4), (10.8, 2.0, 10.8), bone="head", smooth=1), spec((-3.6, 38.2, -5.4), (7.2, 1.2, 0.8), bone="head", mat="eye", glow=G)]
    for x in (-5.0, -1.4, 2.2):
        sp.append(T((x, 45.6, -5.0), (2.8, 2.6, 10), bone="head", course=3))
    # right arm: the block hammer; left arm: the tower shield
    sp += [T((-17, 29, -4), (6, 9, 8), bone="arm_r", course=3), T((-17.4, 15, -4.4), (7, 13, 8.8), bone="forearm_r", course=4),
           T((-15.8, 11, -2), (3.6, 3, 4), bone="forearm_r", smooth=1), T((-14.8, 7, -1.5), (2.2, 22, 3), bone="forearm_r", smooth=1),         # the haft
           T((-19.5, 22.5, -6.5), (7, 8, 13), bone="forearm_r", rune=1, glow=G, course=3)]
    sp += [T((11, 29, -4), (6, 9, 8), bone="arm_l", course=3), T((11.4, 15, -4.4), (7, 13, 8.8), bone="forearm_l", course=4),
           spec((16.5, 6, -9), (3, 26, 18), bone="forearm_l", mat="stone", course=5, rune=1, glow=G), T((17.5, 32, -7), (2, 3, 14), bone="forearm_l", smooth=1)]
    an = Anim()
    humanoid_anim(an, walk=24.0, slam=False)
    keys(an, "attack", "arm_r", "rot", {0.0: (0, 0, 0), 0.35: (160, 0, 0), 0.5: (160, 0, 0), 0.62: (10, 0, 0), 1.0: (0, 0, 0)}, length=1.0, loop=False)
    keys(an, "attack", "chest", "rot", {0.0: (0, 0, 0), 0.35: (-8, 0, 0), 0.62: (14, 0, 0), 1.0: (0, 0, 0)}, length=1.0, loop=False)
    keys(an, "block", "arm_l", "rot", {0.0: (0, 0, 0), 0.2: (75, 0, -14), 1.0: (75, 0, -14), 1.2: (0, 0, 0)}, length=1.2, loop=False)
    return "stone", "sentinel", sp, humanoid_bones(), an


# ============================================================================================================================ SANDSTONE SPHINX
def sandstone_sphinx():
    G = (255, 224, 140)
    S_ = lambda *a, **k: spec(*a, mat="sandstone", **k)
    D_ = lambda *a, **k: spec(*a, mat="sand_dark", **k)
    bones = [("body", "root", (0, 16, 0)), ("head", "body", (0, 26, -14)), ("fleg_r", "body", (-6, 16, -10)), ("fleg_l", "body", (6, 16, -10)), ("hleg_r", "body", (-6, 16, 11)),
             ("hleg_l", "body", (6, 16, 11)), ("tail1", "body", (0, 18, 20)), ("tail2", "tail1", (0, 18, 26)), ("jaw", "head", (0, 27, -19))]
    sp = [S_((-8, 12, -14), (16, 14, 36), bone="body", bands=5), D_((-8.6, 11, -15), (17.2, 2, 38), bone="body"), S_((-7, 26, -10), (14, 3, 26), bone="body", bands=2),
          D_((-9, 14, -12), (1.8, 8, 30), bone="body"), D_((7.2, 14, -12), (1.8, 8, 30), bone="body")]
    sp += [S_((-5.5, 24, -11), (11, 10, 10), bone="body", bands=3)]                                   # the chest
    sp += [S_((-5, 27, -19), (10, 10, 10), bone="head", bands=3), spec((-3.6, 31, -19.6), (2.6, 1.6, 0.8), bone="head", mat="eye", glow=G, pal=PAL["sandstone"]),
           spec((1.0, 31, -19.6), (2.6, 1.6, 0.8), bone="head", mat="eye", glow=G, pal=PAL["sandstone"]), S_((-1.6, 26, -20.6), (3.2, 4, 2.2), bone="head")]
    sp += [D_((-8.5, 25, -18), (3.4, 12, 12), bone="head"), D_((5.1, 25, -18), (3.4, 12, 12), bone="head"), D_((-7, 36, -19), (14, 2.4, 11), bone="head"), S_((-6, 38.4, -18), (12, 2, 9), bone="head", bands=1),
           D_((-2, 40, -17), (4, 3, 4), bone="head")]                                                   # the nemes
    sp += [S_((-4, 22, -21.6), (8, 4, 3), bone="jaw")]
    for nm, x0, z0, bn in (("fr", -9, -14, "fleg_r"), ("fl", 4, -14, "fleg_l"), ("hr", -9, 8, "hleg_r"), ("hl", 4, 8, "hleg_l")):
        sp += [S_((x0, 6, z0), (5, 10, 6), bone=bn, bands=2), D_((x0 - 0.4, 0, z0 - 3), (5.8, 6, 9), bone=bn)]
    sp += [S_((-1.8, 14, 20), (3.6, 4, 6), bone="tail1"), S_((-1.4, 13, 26), (2.8, 3.2, 6), bone="tail2"), D_((-2.2, 11.4, 31), (4.4, 4.4, 4.4), bone="tail2")]
    an = Anim()
    wave(an, "idle", "body", 4.0, steps=8, y=S(0.5), kind="pos")
    wave(an, "idle", "tail1", 4.0, steps=8, y=S(10.0), kind="rot")
    wave(an, "idle", "tail2", 4.0, steps=8, y=S(14.0, 0.2), kind="rot")
    wave(an, "idle", "head", 4.0, steps=8, y=S(3.0, 0.25), kind="rot")
    L = 0.8
    for bn, ph in (("fleg_r", 0.0), ("hleg_l", 0.0), ("fleg_l", 0.5), ("hleg_r", 0.5)):
        wave(an, "walk", bn, L, x=S(26.0, ph), kind="rot")
    wave(an, "walk", "body", L, y=S(0.8, 0.0, 2), kind="pos")
    wave(an, "walk", "tail1", L, y=S(14.0), kind="rot")
    wave(an, "walk", "head", L, x=S(3.0, 0.0, 2), kind="rot")
    for bn in ("fleg_r", "fleg_l"):
        keys(an, "attack", bn, "rot", {0.0: (0, 0, 0), 0.25: (-30, 0, 0), 0.4: (70, 0, 0), 0.9: (0, 0, 0)}, length=0.9, loop=False)
    keys(an, "attack", "body", "pos", {0.0: (0, 0, 0), 0.25: (0, 2, 3), 0.4: (0, -1, -8), 0.9: (0, 0, 0)}, length=0.9, loop=False)
    keys(an, "attack", "jaw", "rot", {0.0: (0, 0, 0), 0.3: (30, 0, 0), 0.45: (0, 0, 0), 0.9: (0, 0, 0)}, length=0.9, loop=False)
    return "sandstone", "sphinx", sp, bones, an


# ============================================================================================================================ SANDSTONE CAGE
def sandstone_cage():
    G = (255, 224, 140)
    S_ = lambda *a, **k: spec(*a, mat="sandstone", **k)
    D_ = lambda *a, **k: spec(*a, mat="sand_dark", **k)
    bones = [("wall_n", "root", (0, 0, -14)), ("wall_s", "root", (0, 0, 14)), ("wall_e", "root", (14, 0, 0)), ("wall_w", "root", (-14, 0, 0)), ("roof", "root", (0, 30, 0)), ("sigil", "root", (0, 0, 0))]
    sp = [S_((-14, 0, -16), (28, 30, 3), bone="wall_n", bands=6), S_((-14, 0, 13), (28, 30, 3), bone="wall_s", bands=6), S_((13, 0, -14), (3, 30, 28), bone="wall_e", bands=6),
          S_((-16, 0, -14), (3, 30, 28), bone="wall_w", bands=6), D_((-16, 28, -16), (32, 3, 32), bone="roof"), S_((-14, 30.6, -14), (28, 2.4, 28), bone="roof", bands=2)]
    for bn, (x, z, sx, sz) in (("wall_n", (-14, -16.6, 28, 3.6)), ("wall_s", (-14, 12.6, 28, 3.6))):
        sp += [D_((x, 0, z), (sx, 3, sz), bone=bn), D_((x, 26, z), (sx, 3, sz), bone=bn)]
    for bn, (x, z) in (("wall_e", (12.6, -14)), ("wall_w", (-16.6, -14))):
        sp += [D_((x, 0, z), (3.6, 3, 28), bone=bn), D_((x, 26, z), (3.6, 3, 28), bone=bn)]
    sp += [spec((-3, 12, -16.4), (6, 8, 0.8), bone="wall_n", mat="core", glow=G, pal=PAL["sandstone"]), spec((-3, 12, 15.6), (6, 8, 0.8), bone="wall_s", mat="core", glow=G, pal=PAL["sandstone"])]
    sp += [spec((-16, 0, -16), (32, 0.8, 32), bone="sigil", mat="sandstone", shine=1, glow=G, bands=8)]
    an = Anim()
    T_ = 0.7
    for bn, off in (("wall_n", (0, 0, -26)), ("wall_s", (0, 0, 26)), ("wall_e", (26, 0, 0)), ("wall_w", (-26, 0, 0))):
        keys(an, "form", bn, "pos", {0.0: off, T_ * 0.8: (0, 0, 0), T_: (0, 0, 0)}, length=T_, loop=False)
        keys(an, "crumble", bn, "pos", {0.0: (0, 0, 0), 0.5: (off[0] * 0.2, -10, off[2] * 0.2), 0.9: (off[0] * 0.5, -34, off[2] * 0.5)}, length=0.9, loop=False)
    keys(an, "form", "roof", "pos", {0.0: (0, 40, 0), T_ * 0.6: (0, 40, 0), T_: (0, 0, 0)}, length=T_, loop=False)
    keys(an, "crumble", "roof", "pos", {0.0: (0, 0, 0), 0.4: (0, -4, 0), 0.9: (0, -36, 0)}, length=0.9, loop=False)
    wave(an, "idle", "roof", 3.0, steps=8, y=S(0.5), kind="pos")
    wave(an, "idle", "sigil", 3.0, steps=8, y=S(0.2), kind="pos")
    return "sandstone", "cage", sp, bones, an


# ============================================================================================================================ OCHRE TOTEM
def ochre_totem():
    G = (255, 120, 60)
    O = lambda *a, **k: spec(*a, mat="ochre", **k)
    P = lambda *a, **k: spec(*a, mat="ochre_pale", **k)
    bones = [("body", "root", (0, 0, 0)), ("jaw", "body", (0, 28, -8)), ("wing_r", "body", (-6, 18, 0)), ("wing_l", "body", (6, 18, 0)), ("crown", "body", (0, 40, 0))]
    sp = [O((-8, 0, -8), (16, 4, 16), bone="body", paint=True), P((-8.4, 3.6, -8.4), (16.8, 1.2, 16.8), bone="body"),
          O((-5.5, 5, -5.5), (11, 11, 11), bone="body", glyph=1, glow=G), P((-6, 9, -6), (12, 1.2, 12), bone="body"),
          spec((-3.6, 8.4, -6.4), (7.2, 3, 1.0), bone="body", mat="ochre", glyph=1, glow=G, paint=False), O((-5, 16, -5), (10, 10, 10), bone="body", glyph=1, glow=G),
          P((-5.6, 20.4, -5.6), (11.2, 1.2, 11.2), bone="body"),
          O((-5.5, 27, -5.5), (11, 11, 11), bone="body", glyph=1, glow=G), spec((-3.4, 33, -6.4), (2.4, 1.8, 0.8), bone="body", mat="eye", glow=G, pal=PAL["ochre"]),
          spec((1.0, 33, -6.4), (2.4, 1.8, 0.8), bone="body", mat="eye", glow=G, pal=PAL["ochre"]), P((-6, 37.4, -6), (12, 1.6, 12), bone="body"),
          O((-2, 30, -10.5), (4, 4, 5.4), bone="body"), O((-1.6, 27, -10), (3.2, 2.6, 4.8), bone="jaw"), P((-1.4, 26.4, -10.4), (2.8, 0.8, 1.0), bone="jaw")]
    sp += [O((-5.8, 14.5, -4), (1.4, 1.4, 8), bone="body"), O((-5.8, 14.5, -4), (1.4, 1.4, 8), bone="body")]
    for sd, bn in ((-1, "wing_r"), (1, "wing_l")):
        for i in range(4):
            d = (sd * math.cos(math.radians(10 + i * 16)), -math.sin(math.radians(10 + i * 16)) * 0.6, 0.0)
            sp.append(prism((sd * 6, 20 - i * 2.0, -1.0), d, 13 - i * 1.6, 2.2, "ochre_pale" if i % 2 else "ochre", bone=bn, glyph=0))
    sp += [O((-6, 40, -1), (12, 12, 2), bone="crown", glyph=1, glow=G), O((-6, 40, -1), (12, 12, 2), rot=(0, 0, 45), pv=(0, 46, 0), bone="crown")]
    for i in range(8):
        a = i * 45.0
        dx, dy = -math.sin(math.radians(a)), math.cos(math.radians(a))
        sp.append(prism((-dx * 8.0, 46 + dy * 8.0, 0), (-dx, dy, 0.0), 3.6, 1.6, "ochre_pale", bone="crown"))
    an = Anim()
    keys(an, "idle", "crown", "rot", {0.0: (0, 0, 0), 5.0: (0, 0, 180), 10.0: (0, 0, 360)}, length=10.0, loop=True)
    wave(an, "idle", "wing_r", 3.0, steps=8, z=S(4.0), kind="rot")
    wave(an, "idle", "wing_l", 3.0, steps=8, z=S(-4.0), kind="rot")
    wave(an, "idle", "body", 3.0, steps=8, y=S(0.3), kind="pos")
    keys(an, "attack", "jaw", "rot", {0.0: (0, 0, 0), 0.2: (40, 0, 0), 0.5: (40, 0, 0), 0.7: (0, 0, 0)}, length=0.7, loop=False)
    keys(an, "attack", "wing_r", "rot", {0.0: (0, 0, 0), 0.2: (0, 0, 28), 0.5: (0, 0, 28), 0.7: (0, 0, 0)}, length=0.7, loop=False)
    keys(an, "attack", "wing_l", "rot", {0.0: (0, 0, 0), 0.2: (0, 0, -28), 0.5: (0, 0, -28), 0.7: (0, 0, 0)}, length=0.7, loop=False)
    keys(an, "attack", "crown", "rot", {0.0: (0, 0, 0), 0.4: (0, 0, 120), 0.7: (0, 0, 180)}, length=0.7, loop=False)
    return "red_ochre", "totem", sp, bones, an


# ============================================================================================================================ MINERAL SPIRE
def mineral_spire():
    rng = np.random.default_rng(2509)
    G = (120, 220, 255)
    M = lambda *a, **k: spec(*a, mat="matrix", **k)
    mats = ["crystal", "amethyst", "crystal", "pyrite", "malachite", "crystal"]
    bones = [("all", "root", (0, 0, 0)), ("sat", "all", (0, 4, 0))] + [("c%d" % i, "sat", (math.sin(i * 1.047) * 8, 4, math.cos(i * 1.047) * 8)) for i in range(6)]
    sp = [M((-9, 0, -9), (18, 5, 18), bone="all")] + jagspecs(rng, 0, 4, 0, 9, 2, 9, 7, 3, 6, mat="matrix", bone="all")
    sp += [prism((0, 4, 0), (0.0, 1.0, 0.0), 40, 7, "crystal", bone="all", glow=G, core=1.4)]
    sp += [prism((0.4, 20, 0), (0.15, 1.0, 0.2), 22, 4, "amethyst", bone="all", glow=(220, 170, 255))]
    for i in range(6):
        a = i * 1.047 + 0.3
        px, pz = math.sin(a) * 8, math.cos(a) * 8
        sp.append(prism((px, 4, pz), (math.sin(a) * 0.55, 1.0, math.cos(a) * 0.55), 18 - (i % 3) * 3, 4 - (i % 2), mats[i], bone="c%d" % i, glow=G if i % 2 == 0 else (200, 150, 255)))
    an = Anim()
    keys(an, "grow", "all", "scale", {0.0: (0.02, 0.02, 0.02), 0.28: (1.08, 1.08, 1.08), 0.42: (0.97, 0.97, 0.97), 0.55: (1, 1, 1)}, length=0.55, loop=False)
    for i in range(6):
        wave(an, "idle", "c%d" % i, 4.0, steps=8, z=S(1.6, i / 6.0), kind="rot")
    keys(an, "pulse", "all", "scale", {0.0: (1, 1, 1), 0.15: (1.08, 1.08, 1.08), 0.5: (1, 1, 1)}, length=0.5, loop=False)
    return "mineral", "spire", sp, bones, an


# ============================================================================================================================ PROJECTILE BODIES
def rock_boulder():
    rng = np.random.default_rng(2611)
    G = (255, 150, 60)
    sp = [spec((-6, 2, -6), (12, 12, 12), mat="rock", bone="b", glow=G, cracks=6, rot=(14, 22, 8), pv=(0, 8, 0))]
    sp += jagspecs(rng, 0, 8, 0, 5, 5, 5, 8, 4, 7, mat="rock", bone="b", glow=G, cracks=2)
    an = Anim()
    keys(an, "idle", "b", "rot", {0.0: (0, 0, 0), 0.3: (180, 0, 0), 0.6: (360, 0, 0)}, length=0.6, loop=True)
    return "rock", "boulder", sp, [("b", "root", (0, 8, 0))], an


def mineral_lance():
    G = (120, 220, 255)
    sp = [prism((0, 0, 12), (0.0, 0.0, -1.0), 40, 5, "crystal", bone="l", glow=G, core=1.5), prism((0, 0, 12), (0.0, 0.0, 1.0), 12, 3, "amethyst", bone="l", glow=(220, 170, 255)),
          prism((0, 0, 0), (0.0, 0.0, -1.0), 20, 8, "crystal", bone="l", glow=G)]
    for i in range(4):
        a = i * math.pi / 2
        sp.append(prism((math.sin(a) * 3.0, math.cos(a) * 3.0, -2), (math.sin(a) * 0.5, math.cos(a) * 0.5, -1.0), 10, 2.4, "pyrite" if i % 2 else "malachite", bone="l"))
    an = Anim()
    keys(an, "idle", "l", "rot", {0.0: (0, 0, 0), 0.4: (0, 0, 180), 0.8: (0, 0, 360)}, length=0.8, loop=True)
    return "mineral", "lance", sp, [("l", "root", (0, 0, 0))], an


MODELS = {"golem": earth_golem, "titan": rock_titan, "sentinel": stone_sentinel, "sphinx": sandstone_sphinx, "cage": sandstone_cage, "totem": ochre_totem,
          "spire": mineral_spire, "boulder": rock_boulder, "lance": mineral_lance}


def main():
    wanted = sys.argv[1:] or list(MODELS)
    for w in wanted:
        key, name, specs, bones, anim = MODELS[w]()
        mid, sh = build(key, name, 300 + sum(ord(c) for c in name), specs, bones, anim)
        print(mid, sh.m.cube_count(), "cubes", sh.tw, "x", sh.th, "clips", sorted(anim.clips))


if __name__ == "__main__":
    main()

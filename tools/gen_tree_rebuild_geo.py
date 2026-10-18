"""Tree Magic rebuild models: the Heartwood armour set of the temporary armour (Aura.TREE) and the two summons (the Heartwood Guardian, a root lance).

    python3 -B tools/gen_tree_rebuild_geo.py        # writes every model, animation, texture and the skin overlay, runs geo_check

Armour (Space.PLAYER, each piece anchored on the model part it was built for, see client/aura/TreeAura.java):
  tree_helm        bark helm with ear guards, brow ridge and a pair of antlers of living branches carrying leaf clusters (head)
  tree_crown       a ring of leaves and a twig circlet floating above the helm, turning slowly (head)
  tree_chest       layered bark plates with a glowing heartwood tree-ring emblem, belt, back plate, moss patches (body)
  tree_pauld_r/l   burl pauldrons: a knotted bark mound with a sprouting branch and leaves (arms)
  tree_bracer_r/l  bark bracers with a root-fingered gauntlet (arms)
  tree_greave_r/l  log greaves with a knee burl and roots gripping the ground (legs)
  tree_back        a young tree growing from the back: trunk, three limbs, canopy (body)
  tree_aux         four acorns orbiting the waist, each with a heartwood glow (body)
Summons (prop kind TREE_1):
  tree_guardian    the Heartwood Guardian, a three-block treant: trunk body with a ring emblem, two root arms, root legs, a crown of branches; clips idle, slam, rise
  tree_lance       a root lance: a spiral of roots ending in a splintered point; clips idle, thrust
Texture style: pixel-art bark with crevices and knots, heartwood rings, leaf veins, amber sap seams in the glow map.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_lib import *   # noqa: E402,F401,F403

KEY = "tree"
MATS = {
    "bark": dict(kind="bark", ramp=[(26, 16, 9), (56, 36, 20), (94, 62, 36), (136, 96, 58), (184, 144, 94)], glowc=(255, 190, 80), seam=70),
    "bark_dark": dict(kind="bark", ramp=[(18, 11, 6), (40, 26, 14), (68, 44, 26), (100, 68, 40), (140, 102, 64)], glowc=(255, 170, 60), seam=40),
    "ring": dict(kind="ring", ramp=[(70, 40, 16), (120, 74, 34), (170, 116, 56), (214, 164, 92), (250, 224, 150)], glowc=(255, 200, 90), ring_glow=210),
    "leaf": dict(kind="leaf", ramp=[(12, 48, 16), (26, 88, 30), (50, 140, 46), (92, 186, 62), (166, 230, 112)], glowc=(170, 255, 100), vein_glow=80),
    "leaf_dark": dict(kind="leaf", ramp=[(8, 34, 12), (18, 62, 22), (34, 100, 34), (62, 140, 46), (110, 180, 70)], glowc=(140, 240, 90), vein_glow=40),
    "moss": dict(kind="plain", ramp=[(30, 50, 20), (58, 88, 32), (92, 130, 46), (130, 170, 62), (182, 212, 102)]),
    "core": dict(kind="core", ramp=[(120, 60, 10), (200, 120, 20), (255, 190, 60), (255, 230, 140), (255, 250, 220)], glowc=(255, 200, 70)),
    "sap": dict(kind="core", ramp=[(100, 60, 0), (180, 130, 10), (240, 190, 40), (255, 228, 120), (255, 248, 210)], glowc=(255, 210, 90)),
}
cube_total = 0


def new(name, w, h, seed):
    return Atlas(KEY, name, w, h, seed=seed, mats=MATS)


# --------------------------------------------------------------------------------------------------------------------
def build_helm():
    A = new("helm", 128, 128, 11)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("head", "root", (0, 24, 0))
    c = A.cube
    c("head", (-5, 30, -5), (10, 3, 10), "bark", inflate=0.1)                    # the crown plate of the helm, 1 px wider than the head all round
    c("head", (-4, 33, -4), (8, 1, 8), "bark_dark", inflate=0.1)                  # a second, smaller step
    c("head", (-4, 29, -5), (8, 1, 1), "bark_dark", inflate=0.1)                  # the brow ridge over the eyes
    c("head", (-5, 25, -3), (1, 5, 7), "bark", inflate=0.1)                       # ear guards
    c("head", (4, 25, -3), (1, 5, 7), "bark", inflate=0.1)
    c("head", (-4, 25, 4), (8, 6, 1), "bark_dark", inflate=0.1)                   # the neck guard
    c("head", (-1, 31.2, -5.4), (2, 2, 1), "core", rot=(0, 0, 45))                # a heartwood stud on the brow
    A.anim.clip("idle", length=4.0, loop=True)
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        # an antler of living branches: a main limb and a fork, with leaf clusters at the tips
        names = A.limb("ant%s" % n, "head", (sg * 3, 33, 1), (-6, 0, -sg * 22), [5, 4, 4], 3, 1, "bark", share="an", curl=(0, 0, sg * 9))
        fork = A.limb("fork%s" % n, names[0], (sg * 3, 39, 1), (-8, 0, sg * 44), [4, 3], 2, 1, "bark", share="fk")
        leaves(A, names[2], A.tips[names[2]], 3, 3.0, size=(4, 4), seed=sg + 3)
        leaves(A, fork[1], A.tips[fork[1]], 2, 2.5, size=(3, 3), seed=sg + 5)
        A.anim.rot("idle", names[0], sway(4.0, 2.5, 0.1 * sg, base=(-6, 0, -sg * 22)))
        A.anim.rot("idle", fork[0], sway(4.0, 4.0, 0.3 * sg, base=(-8, 0, sg * 44)))
    leaves(A, "head", (0, 34.5, 1), 3, 3.5, size=(3, 3), seed=9)
    return A.save()


def build_crown():
    A = new("crown", 64, 64, 12)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("ring", "root", (0, 38, 0))
    c = A.cube
    for k in range(8):
        a = math.radians(k * 45)
        x, z = 6.5 * math.cos(a), 6.5 * math.sin(a)
        c("ring", (x - 1.5, 37.5, z - 0.5), (3, 1, 1), "bark", rot=(0, -k * 45, 0))                   # the twig band
        A.leaf("ring", (x * 1.18, 38.8 + (k % 2) * 0.6, z * 1.18), (3, 3), "leaf" if k % 2 == 0 else "leaf_dark", rot=(12 * (1 if k % 2 else -1), 90 - k * 45, 20), share="cl%d" % (k % 2))
    for k in range(4):
        a = math.radians(k * 90 + 22)
        c("ring", (6.5 * math.cos(a) - 0.5, 38, 6.5 * math.sin(a) - 0.5), (1, 1, 1), "sap", share="cs")  # a drop of sap between the leaves
    A.anim.clip("idle", length=8.0, loop=True)
    A.anim.rot("idle", "ring", spin(8.0, axis=1))
    return A.save()


def build_chest():
    A = new("chest", 128, 128, 13)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-5, 20, -3), (10, 3, 6), "bark", inflate=0.15)                    # the collar band
    c("torso", (-4.5, 16, -3.4), (9, 4, 1), "bark", inflate=0.1)                  # three stacked bark plates down the front
    c("torso", (-4, 13, -3.4), (8, 3, 1), "bark_dark", inflate=0.1)
    c("torso", (-3, 17.2, -4.6), (6, 6, 1), "ring", inflate=0.1)                  # the heartwood tree-ring emblem
    c("torso", (-1, 19, -5.2), (2, 2, 1), "core", rot=(0, 0, 45))                 # the heart of the ring
    c("torso", (-5, 12, -3), (10, 2, 6), "bark_dark", inflate=0.2)                # belt
    c("torso", (-1, 11.5, -3.7), (2, 3, 1), "ring")
    c("torso", (-4.5, 13, 2), (9, 10, 2), "bark", inflate=0.15)                   # back plate
    c("torso", (-1, 14, 3.4), (2, 8, 1), "bark_dark")                             # a split down the back where the sap shows
    for k, (x, y, w) in enumerate(((-4.5, 21, 3), (2, 15, 3), (-4, 13.5, 2), (1.5, 21.5, 2))):
        c("torso", (x, y, -3.9), (w, 2, 1), "moss", inflate=0.1, share="mo%d" % k)  # moss patches
    A.bone("hang", "torso", (0, 13, -3))
    for k, x in enumerate((-3, 0, 3)):                                            # leaves hanging from the belt like a tabard
        A.leaf("hang", (x, 9.5, -3.2), (3, 4), "leaf" if k != 1 else "leaf_dark", rot=(8, 0, (k - 1) * 8), share="hl%d" % (k % 2))
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "hang", sway(3.0, 3.0, axis=0))
    return A.save()


def build_pauldron(sg):
    n = "r" if sg < 0 else "l"
    A = new("pauld_" + n, 64, 64, 14 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    s((3.5, 23, -3.5), (6, 3, 7), "bark", inflate=0.2, share="pa")
    s((4, 26, -2.5), (5, 2, 5), "bark_dark", inflate=0.1, rot=(0, 0, -8), share="pb")
    s((5, 21.2, -3.5), (5, 1, 7), "bark_dark", share="pc")
    s((8, 24, -2), (2, 3, 4), "bark", inflate=0.1, share="pd")                    # the outer burl
    s((5, 27.4, -0.6), (2, 1, 1), "core", share="pe")
    A.bone("shoot", "arm", (sg * 7, 27, 0), rotation=(0, 0, -sg * 28))
    A.cube("shoot", (sg * 7 - 1, 27, -1), (2, 5, 2), "bark", share="ps")
    leaves(A, "shoot", (sg * 6, 33, 0), 4, 2.6, size=(3, 3), seed=sg + 2)
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "shoot", sway(3.0, 5.0, 0.2, base=(0, 0, -sg * 28)))
    return A.save()


def build_bracer(sg):
    n = "r" if sg < 0 else "l"
    A = new("bracer_" + n, 64, 64, 16 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    s((3.5, 12, -2.5), (5, 6, 5), "bark", inflate=0.3, share="ba")                # vambrace
    s((3.5, 17, -2.5), (5, 1, 5), "bark_dark", inflate=0.4, share="bb")
    s((3.5, 12, -2.5), (5, 1, 5), "bark_dark", inflate=0.4, share="bb")
    s((8.2, 13, -1.5), (1, 4, 3), "ring", share="bc")
    s((4.5, 18, -3), (3, 2, 1), "moss", inflate=0.1, share="bm")
    # the root-fingered gauntlet: a knot of wood with three roots hanging below the hand
    s((3.8, 9, -2.8), (4, 3, 5), "bark_dark", inflate=0.2, share="bg")
    for k, (x, z) in enumerate(((4.3, -2.2), (5.6, -0.5), (4.3, 1.4))):
        A.bone("fi%d" % k, "arm", (sg * (x + 0.5), 9, z), rotation=(10, 0, -sg * 6))
        A.cube("fi%d" % k, (sg * (x + 0.5) - 0.5, 5.0, z - 0.5), (1, 4, 1), "bark", share="bf")
    A.anim.clip("idle", length=3.0, loop=True)
    for k in range(3):
        A.anim.rot("idle", "fi%d" % k, sway(3.0, 6.0, 0.15 * k, axis=0, base=(10, 0, -sg * 6)))
    return A.save()


def build_greave(sg):
    n = "r" if sg < 0 else "l"
    A = new("greave_" + n, 64, 64, 18 + (sg > 0))
    A.bone("leg", pivot=(sg * 1.9, 12, 0))
    s = lambda *a, **k: A.side("leg", sg, *a, **k)
    s((0.3, 1.5, -2.5), (4.4, 7, 5), "bark", inflate=0.3, share="ga")             # the log shin
    s((0.3, 0.5, -2.5), (4.4, 1, 5), "bark_dark", inflate=0.35, share="gb")
    s((0.5, 8.5, -3.4), (4, 3, 2), "bark_dark", inflate=0.2, share="gc")          # the knee burl
    s((1.3, 9.2, -4.6), (2, 2, 1), "core", share="gd")
    s((0, 0, -3.8), (4.4, 2, 2), "bark", inflate=0.1, share="ge")                  # the toe wedge
    s((0.2, 4, 2.4), (1, 4, 1), "moss", share="gm")
    A.bone("root", "leg", (sg * 4.4, 1, 0), rotation=(0, 0, -sg * 70))
    A.cube("root", (sg * 4.4 - 0.5, 0, -0.5), (1, 5, 1), "bark", share="gr")
    A.bone("root2", "leg", (sg * 0.3, 1, -3), rotation=(60, 0, 0))
    A.cube("root2", (sg * 0.3 - 0.5, 0, -3.5), (1, 5, 1), "bark", share="gr2")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "root", sway(3.0, 5.0, 0.2, base=(0, 0, -sg * 70)))
    return A.save()


def build_back():
    A = new("back", 128, 128, 20)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-3, 14, 2), (6, 4, 2), "bark_dark", inflate=0.2)                  # the root-plate the tree grows from
    c("torso", (-1, 15, 3.6), (2, 2, 1), "core")
    trunk = A.limb("tr", "torso", (0, 17, 4), (-14, 0, 0), [6, 5, 4], 4, 3, "bark", share="tt")
    br_l = A.limb("bl", trunk[2], (0, 35, 4), (0, 0, 38), [5, 4], 2, 1, "bark", share="bl")
    br_r = A.limb("br", trunk[2], (0, 35, 4), (0, 0, -38), [5, 4], 2, 1, "bark", share="bl")
    br_m = A.limb("bm", trunk[1], (0, 30, 4), (28, 0, 0), [4, 3], 2, 1, "bark", share="bm")
    leaves(A, trunk[2], A.tips[trunk[2]], 5, 5.0, size=(5, 5), seed=2, up=2)
    leaves(A, br_l[1], A.tips[br_l[1]], 3, 3.0, size=(4, 4), seed=3, up=1)
    leaves(A, br_r[1], A.tips[br_r[1]], 3, 3.0, size=(4, 4), seed=4, up=1)
    leaves(A, br_m[1], A.tips[br_m[1]], 2, 2.5, size=(4, 4), seed=5)
    A.anim.clip("idle", length=5.0, loop=True)
    A.anim.rot("idle", trunk[0], sway(5.0, 1.5, base=(-14, 0, 0), axis=2))
    A.anim.rot("idle", trunk[2], sway(5.0, 3.0, 0.2, base=(0, 0, 0), axis=2))
    A.anim.rot("idle", br_l[0], sway(5.0, 4.0, 0.3, base=(0, 0, 38)))
    A.anim.rot("idle", br_r[0], sway(5.0, 4.0, 0.6, base=(0, 0, -38)))
    return A.save()


def build_aux():
    A = new("aux", 64, 64, 22)
    A.bone("root", pivot=(0, 24, 0))
    for k in range(4):
        A.bone("orb%d" % k, "root", (0, 18, 0), rotation=(0, k * 90, 0))
        A.bone("acorn%d" % k, "orb%d" % k, (11, 18 + 2.5 * (k % 2 - 0.5), 0))
        c = lambda *a, **kw: A.cube("acorn%d" % k, *a, **kw)
        y = 18 + 2.5 * (k % 2 - 0.5)
        c((10, y - 1.5, -1.5), (3, 3, 3), "sap", share="ac")
        c((9.7, y + 1.0, -1.8), (3, 1, 3), "bark_dark", inflate=0.3, share="acap")
        c((10.8, y + 2.3, -0.3), (1, 1, 1), "bark", share="astem")
    A.anim.clip("idle", length=6.0, loop=True)
    for k in range(4):
        A.anim.rot("idle", "orb%d" % k, {round(6.0 * i / 4, 2): (0, k * 90 + 90 * i, 0) for i in range(5)})
    return A.save()


def build_skin():
    S = Skin(KEY, MATS, 3)
    S.put("torso", ("north", "south", "west", "east"), (0, 0, 8, 12), "bark")
    S.put("torso", ("north",), (2, 4, 4, 4), "ring")
    for arm in ("rarm", "larm"):
        S.put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "bark")
        S.put(arm, ("north", "south", "west", "east"), (0, 8, 4, 2), "moss")
    for leg in ("rleg", "lleg"):
        S.put(leg, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "bark_dark")
        S.put(leg, ("north", "south", "west", "east"), (0, 5, 4, 2), "moss")
    S.save()


# --------------------------------------------------------------------------------------------------------------------
# summons
# --------------------------------------------------------------------------------------------------------------------
def build_guardian():
    A = new("guardian", 256, 256, 31)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 20, 0))
    c = A.cube
    # trunk body: three stacked drums narrowing to the chest, a back hump, the ring emblem on the chest
    c("body", (-9, 18, -6), (18, 10, 12), "bark_dark")
    c("body", (-10, 27, -7), (20, 14, 14), "bark")
    c("body", (-8, 41, -6), (16, 6, 12), "bark_dark", inflate=0.4)
    c("body", (-5, 30, -8), (10, 10, 1), "ring")                                   # the emblem
    c("body", (-2, 34, -8.8), (4, 4, 1), "core", rot=(0, 0, 45))
    c("body", (-7, 32, 7), (14, 11, 4), "bark", inflate=0.3)                       # back hump
    c("body", (-12, 40, -5), (3, 5, 10), "bark", rot=(0, 0, 18))                    # shoulder burls
    c("body", (9, 40, -5), (3, 5, 10), "bark", rot=(0, 0, -18))
    for k, (x, y) in enumerate(((-8, 35), (4, 28), (-6, 22), (6, 36))):
        c("body", (x, y, -7.4), (3, 2, 1), "moss", share="gm%d" % k)
    # head: a knot of bark with a hollow face, glowing eyes, a crown of branches and leaves
    A.bone("head", "body", (0, 47, 0))
    c("head", (-5, 46, -5), (10, 9, 9), "bark")
    c("head", (-4, 50, -6), (3, 2, 1), "core", share="eye")
    c("head", (1, 50, -6), (3, 2, 1), "core", share="eye")
    c("head", (-2, 46.5, -6), (4, 2, 1), "bark_dark")
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        hb = A.limb("hb%s" % n, "head", (sg * 3, 55, 0), (0, 0, -sg * 26), [6, 5, 4], 3, 1, "bark", share="hb", curl=(0, 0, sg * 10))
        leaves(A, hb[2], A.tips[hb[2]], 5, 4.0, size=(5, 5), seed=sg + 7, up=2)
    leaves(A, "head", (0, 56, 0), 6, 4.5, size=(5, 5), seed=4, up=2)
    # arms: jointed root arms with a gnarled fist of roots
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("arm_%s" % n, "body", (sg * 12, 40, 0))
        A.bone("fore_%s" % n, "arm_%s" % n, (sg * 14, 24, -1))
        A.side("arm_%s" % n, sg, (10, 24, -4), (7, 17, 8), "bark")
        A.side("arm_%s" % n, sg, (10.5, 33, -4.4), (6, 2, 1), "moss", share="am")
        A.side("fore_%s" % n, sg, (11, 8, -5), (9, 17, 10), "bark_dark")
        for k in range(3):
            A.bone("rt_%s%d" % (n, k), "fore_%s" % n, (sg * (14 + (k - 1) * 3), 8, -1), rotation=((k - 1) * 14, 0, 0))
            A.cube("rt_%s%d" % (n, k), (sg * (14 + (k - 1) * 3) - 1.5, 0, -2), (3, 8, 4), "bark", share="rt%d" % (k % 2))
        A.side("fore_%s" % n, sg, (11, 17, -5.6), (9, 2, 1), "ring", share="fr")
    # legs: two rooted stumps with a spread of roots
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("leg_%s" % n, "root", (sg * 6, 20, 0))
        A.side("leg_%s" % n, sg, (2, 6, -5), (9, 14, 10), "bark")
        A.side("leg_%s" % n, sg, (1, 0, -7), (11, 6, 14), "bark_dark")
        for k in range(3):
            A.bone("lr_%s%d" % (n, k), "leg_%s" % n, (sg * 7, 3, (k - 1) * 5), rotation=(0, 0, -sg * 60))
            A.cube("lr_%s%d" % (n, k), (sg * 7 - 1.5, 0, (k - 1) * 5 - 1.5), (3, 8, 3), "bark", share="lr")
    # idle: breathes, branches sway, eyes pulse (scale); slam: rear back, hammer both fists down; rise: from a crouch
    a = A.anim
    a.clip("idle", length=4.0, loop=True)
    a.pos("idle", "body", loop(4.0, lambda p: (0, 0.8 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "body", loop(4.0, lambda p: (1.2 * math.sin(p * 2 * math.pi), 1.5 * math.sin(p * 2 * math.pi + 1), 0)))
    a.rot("idle", "head", sway(4.0, 3.0, 0.2, axis=1))
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("idle", "arm_%s" % n, sway(4.0, 4.0, 0.1 * sg, axis=0))
        a.rot("idle", "fore_%s" % n, sway(4.0, 5.0, 0.3 * sg, axis=0))
        a.rot("idle", "hb%s_0" % n, sway(4.0, 3.0, 0.2 * sg, base=(0, 0, -sg * 26)))
    a.clip("slam", length=1.2, loop=True)
    a.rot("slam", "body", {0.0: (0, 0, 0), 0.4: (-18, 0, 0), 0.55: (14, 0, 0), 0.7: (10, 0, 0), 1.2: (0, 0, 0)})
    a.pos("slam", "body", {0.0: (0, 0, 0), 0.4: (0, 3, 4), 0.55: (0, -3, -8), 0.7: (0, -2, -6), 1.2: (0, 0, 0)})
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("slam", "arm_%s" % n, {0.0: (0, 0, 0), 0.4: (-150, 0, 0), 0.55: (-10, 0, 0), 0.7: (-20, 0, 0), 1.2: (0, 0, 0)})
        a.rot("slam", "fore_%s" % n, {0.0: (0, 0, 0), 0.4: (-30, 0, 0), 0.55: (10, 0, 0), 0.7: (0, 0, 0), 1.2: (0, 0, 0)})
    a.rot("slam", "head", {0.0: (0, 0, 0), 0.4: (-10, 0, 0), 0.55: (12, 0, 0), 1.2: (0, 0, 0)})
    a.clip("rise", length=1.0, loop=False)
    a.pos("rise", "body", {0.0: (0, -16, 0), 0.6: (0, 2, 0), 1.0: (0, 0, 0)})
    a.scale("rise", "body", {0.0: (0.6, 0.4, 0.6), 0.6: (1.04, 1.06, 1.04), 1.0: (1, 1, 1)})
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("rise", "arm_%s" % n, {0.0: (60, 0, -sg * 20), 1.0: (0, 0, 0)})
    return A.save()


def build_lance():
    A = new("lance", 128, 128, 33)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("shaft", "root", (0, 0, 0))
    c = A.cube
    # a shaft of twisted roots: stacked rotated sections, ending in a splintered point at +y
    for k in range(6):
        y = k * 5
        c("shaft", (-3, y, -3), (6, 5, 6), "bark" if k % 2 == 0 else "bark_dark", rot=(0, 15 * k, 0), share="ls%d" % (k % 2))
        c("shaft", (-2, y, -4.2), (4, 5, 1), "bark_dark", rot=(0, 15 * k, 0), share="lt")
    c("shaft", (-1.5, 30, -1.5), (3, 6, 3), "bark")
    c("shaft", (-1, 36, -1), (2, 6, 2), "bark_dark")
    c("shaft", (-0.5, 42, -0.5), (1, 6, 1), "sap")
    for k in range(4):
        a = k * 90
        A.bone("sp%d" % k, "shaft", (0, 28, 0), rotation=(0, a, 0))
        A.bone("spc%d" % k, "sp%d" % k, (3, 28, 0), rotation=(0, 0, -34))
        A.cube("spc%d" % k, (2, 28, -1), (2, 9, 2), "bark", share="spl")
    c("shaft", (-4, -3, -4), (8, 3, 8), "bark_dark")                                # the root ball at the base
    for k in range(5):
        A.bone("rb%d" % k, "root", (0, 0, 0), rotation=(0, k * 72, 0))
        A.bone("rbc%d" % k, "rb%d" % k, (3, 0, 0), rotation=(0, 0, 62))
        A.cube("rbc%d" % k, (2, -6, -1), (2, 7, 2), "bark", share="rbp")
    a = A.anim
    a.clip("idle", length=2.0, loop=True)
    a.rot("idle", "shaft", loop(2.0, lambda p: (0, 22 * math.sin(p * 2 * math.pi), 0)))
    a.clip("thrust", length=0.6, loop=False)
    a.pos("thrust", "shaft", {0.0: (0, -10, 0), 0.25: (0, 0, 0), 0.6: (0, 2, 0)})
    a.rot("thrust", "shaft", {0.0: (0, 0, 0), 0.6: (0, 180, 0)})
    a.scale("thrust", "shaft", {0.0: (0.4, 0.2, 0.4), 0.25: (1.0, 1.12, 1.0), 0.6: (1, 1, 1)})
    return A.save()


if __name__ == "__main__":
    ids = [build_helm(), build_crown(), build_chest(), build_pauldron(-1), build_pauldron(1), build_bracer(-1), build_bracer(1),
           build_greave(-1), build_greave(1), build_back(), build_aux(), build_guardian(), build_lance()]
    build_skin()
    run_check(ids)
    print("done:", ", ".join(ids))

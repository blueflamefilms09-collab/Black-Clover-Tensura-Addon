"""Mud Magic rebuild models: the Bogplate armour of the temporary armour (Aura.MUD) and the summons (the Mud Golem, the Mud Spout).

    python3 -B tools/gen_mud_rebuild_geo.py         # writes every model, animation, texture and the skin overlay, runs geo_check

Armour (Space.PLAYER, anchored on the model part each piece was built for, see client/aura/MudAura.java):
  mud_helm        a thick mud cowl with ear lumps, a visor slit with two glowing amber eyes, two small horn lumps and a dripping fringe (head)
  mud_crown       four clods of mud orbiting above the head, one with a glowing crack (head)
  mud_chest       three heavy cracked slabs over the chest with amber seams, a pectoral boulder, a sagging belt with drips, a hump on the back (body)
  mud_pauld_r/l   big round mud mounds on the shoulders with drips (arms)
  mud_bracer_r/l  massive gauntlet fists of mud with a drying crust and a drip (arms)
  mud_greave_r/l  heavy wading boots: a big foot block, an ankle ring, a splash flare (legs)
  mud_back        a hunched mud hump with stalactite drips and glowing seams, two clod hands hanging at the sides (body)
  mud_aux         four mud globs orbiting low, dripping (body)
Summons (prop kind MUD_1):
  mud_golem       the Mud Golem: a hulking two-block brute with huge arms, a small head with glowing eyes, cracked amber seams; clips idle, slam, rise
  mud_spout       the Mud Spout: a column of mud rising from the ground with a splash crown on top and drips; clips idle, erupt
Texture style: blotchy brown mud with wet highlights, vertical drip streaks, dried crust, amber glowing cracks.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_lib import *   # noqa: E402,F401,F403

KEY = "mud"
MATS = {
    "mud": dict(kind="mud", ramp=[(24, 14, 8), (56, 36, 22), (94, 60, 38), (136, 90, 58), (186, 138, 98)], glowc=(255, 150, 60), seam=110),
    "wet": dict(kind="mud", ramp=[(16, 10, 6), (40, 26, 16), (70, 46, 30), (108, 72, 48), (160, 116, 82)], glowc=(255, 140, 50), seam=60),
    "dry": dict(kind="mud", ramp=[(58, 42, 28), (100, 76, 52), (142, 112, 78), (184, 154, 114), (224, 200, 160)], glowc=(255, 170, 70), seam=130),
    "ember": dict(kind="core", ramp=[(120, 40, 0), (210, 90, 10), (255, 150, 40), (255, 210, 120), (255, 244, 210)], glowc=(255, 160, 50)),
    "eye": dict(kind="core", ramp=[(130, 70, 0), (220, 150, 10), (255, 210, 50), (255, 240, 140), (255, 252, 220)], glowc=(255, 210, 70)),
}


def new(name, w, h, seed):
    return Atlas(KEY, name, w, h, seed=seed, mats=MATS)


def drip(A, bone, x, y, z, ln, w=1, mat="wet", share="dp"):
    A.cube(bone, (x - w / 2.0, y - ln, z - w / 2.0), (w, ln, w), mat, share="%s%d%d" % (share, ln, w))


# --------------------------------------------------------------------------------------------------------------------
def build_helm():
    A = new("helm", 64, 64, 131)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("head", "root", (0, 24, 0))
    c = A.cube
    c("head", (-5, 29, -5), (10, 4, 10), "mud", inflate=0.15)                          # the cowl
    c("head", (-5, 24, 3), (10, 6, 2), "wet", inflate=0.1)                              # nape
    c("head", (-5, 25, -4), (1, 5, 8), "mud", inflate=0.1)                              # cheek / ear lumps
    c("head", (4, 25, -4), (1, 5, 8), "mud", inflate=0.1)
    c("head", (-5, 28, -5), (10, 1, 1), "dry", inflate=0.1)                            # the brow crust
    c("head", (-3.5, 27.5, -5.3), (2, 1, 1), "eye", share="ey")                         # eyes in the shadow of the brow
    c("head", (1.5, 27.5, -5.3), (2, 1, 1), "eye", share="ey")
    c("head", (-4, 33, -3), (3, 2, 4), "dry", rot=(0, 0, 14), share="hn")               # two horn lumps
    c("head", (1, 33, -3), (3, 2, 4), "dry", rot=(0, 0, -14), share="hn")
    c("head", (-1, 33.4, 2), (2, 2, 2), "ember")
    for k, x in enumerate((-4, -1.5, 1.5, 4)):
        drip(A, "head", x, 29, -5.2, 2 + (k % 2) * 2)
    return A.save()


def build_crown():
    A = new("crown", 64, 64, 132)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("ring", "root", (0, 37, 0))
    for k in range(4):
        a = k * 90.0
        x, z = 6.4 * math.cos(math.radians(a)), 6.4 * math.sin(math.radians(a))
        A.bone("g%d" % k, "ring", (x, 37, z))
        A.cube("g%d" % k, (x - 2, 36, z - 2), (4, 3, 4), "mud", rot=(20 * k, a, 10), share="cg")
        A.cube("g%d" % k, (x - 1, 38.5, z - 1), (2, 2, 2), "dry", share="cd")
        if k == 1:
            A.cube("g%d" % k, (x - 1, 36.8, z - 2.3), (2, 1, 1), "ember")
    A.anim.clip("idle", length=8.0, loop=True)
    A.anim.rot("idle", "ring", spin(8.0, axis=1, turns=1))
    for k in range(4):
        A.anim.pos("idle", "g%d" % k, loop(8.0, lambda p, k=k: (0, 1.4 * math.sin((p + k / 4.0) * 4 * math.pi), 0)))
    return A.save()


def build_chest():
    A = new("chest", 128, 128, 133)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-5.5, 19, -3.6), (11, 4, 3), "mud", inflate=0.1)                        # three heavy slabs
    c("torso", (-5, 15, -3.8), (10, 4, 3), "dry", inflate=0.1)
    c("torso", (-4.5, 12, -3.4), (9, 3, 2), "wet", inflate=0.1)
    c("torso", (-3, 17.5, -5.6), (6, 5, 2), "mud", inflate=0.2)                        # the pectoral boulder
    c("torso", (-1, 19, -6.8), (2, 2, 1), "ember")
    c("torso", (-5.5, 11, -3), (11, 2, 6), "wet", inflate=0.2)                          # the belt, sagging
    c("torso", (-6, 8, -3.2), (3, 3, 4), "mud", share="sg")
    c("torso", (3, 8, -3.2), (3, 3, 4), "mud", share="sg")
    c("torso", (-5, 12, 2), (10, 11, 3), "mud", inflate=0.2)                            # the back hump
    c("torso", (-4, 20, 4), (8, 4, 2), "dry", inflate=0.2)
    for k, x in enumerate((-4, -1, 2.5, 4)):
        drip(A, "torso", x, 19 - (k % 2) * 4, -5.4, 3 + (k % 3))
    A.bone("sag", "torso", (0, 11, -3))
    c("sag", (-1.5, 9, -4), (3, 3, 1), "ember", share="bk")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.scale("idle", "sag", loop(3.0, lambda p: (1, 1 - 0.1 * math.sin(p * 2 * math.pi), 1)))
    return A.save()


def build_pauldron(sg):
    n = "r" if sg < 0 else "l"
    A = new("pauld_" + n, 64, 64, 134 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    s((3.5, 23, -4), (7, 4, 8), "mud", inflate=0.3, share="pa")
    s((4.5, 27, -3), (5, 2, 6), "dry", inflate=0.2, share="pb")
    s((9, 21, -3), (3, 5, 6), "wet", inflate=0.2, share="pc")
    s((5.5, 28.5, -1), (2, 1, 2), "ember", share="pd")
    for k, z in enumerate((-3, 0, 3)):
        drip(A, "arm", sg * 10.4, 22, z, 3 + k % 2, 1, share="pdr")
    return A.save()


def build_bracer(sg):
    n = "r" if sg < 0 else "l"
    A = new("bracer_" + n, 64, 64, 136 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    s((3.5, 12, -2.5), (5, 6, 5), "mud", inflate=0.4, share="ba")
    s((3.5, 17, -2.5), (5, 2, 5), "dry", inflate=0.5, share="bb")
    s((2.5, 5, -4), (7, 7, 8), "wet", inflate=0.3, share="bf")                          # the fist
    s((3.5, 11.5, -4.4), (5, 2, 1), "dry", inflate=0.1, share="bc")
    s((4.5, 7, -4.8), (3, 2, 1), "ember", share="bd")
    for k, z in enumerate((-3, 0, 2.5)):
        drip(A, "arm", sg * 6, 5.5, z, 3 + (k % 2), 1, share="br")
    return A.save()


def build_greave(sg):
    n = "r" if sg < 0 else "l"
    A = new("greave_" + n, 64, 64, 138 + (sg > 0))
    A.bone("leg", pivot=(sg * 1.9, 12, 0))
    s = lambda *a, **k: A.side("leg", sg, *a, **k)
    s((0, 3, -3), (5, 6, 6), "mud", inflate=0.3, share="ga")
    s((-0.5, 0, -4.5), (6, 3, 9), "wet", inflate=0.2, share="gb")
    s((0.2, 9, -3), (4.6, 2, 6), "dry", inflate=0.4, share="gc")
    s((1, 9.5, -4.4), (2, 2, 1), "ember", share="gd")
    for k in range(4):
        a = k * 90.0 + 45
        x, z = sg * 2.4 + 4.2 * math.cos(math.radians(a)), 4.2 * math.sin(math.radians(a))
        A.bone("sp%d" % k, "leg", (x, 1.5, z), rotation=lean(x - sg * 2.4, z, 40))
        A.cube("sp%d" % k, (x - 1.5, 0, z - 0.5), (3, 3, 1), "mud", rot=(0, -a + 90, 0), pivot=(x, 1.5, z), share="gs")
    return A.save()


def build_back():
    A = new("back", 128, 128, 140)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-6, 12, 2.5), (12, 8, 5), "mud", inflate=0.2)                          # the hump
    c("torso", (-5, 19, 3), (10, 5, 4), "dry", inflate=0.2)
    c("torso", (-4, 24, 3.5), (8, 3, 3), "mud", inflate=0.2)
    c("torso", (-1, 14, 7.8), (2, 5, 1), "ember")
    c("torso", (-3, 20, 7.6), (1, 3, 1), "ember")
    for k, x in enumerate((-5, -2, 1, 4)):
        drip(A, "torso", x, 14 - (k % 2), 6, 4 + (k % 3), 1, share="bd")
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("h%s" % n, "torso", (sg * 7, 20, 4), rotation=(0, 0, sg * 8))
        A.cube("h%s" % n, (sg * 7 - 2, 12, 2), (4, 8, 4), "wet", share="hh")
        A.cube("h%s" % n, (sg * 7 - 2.5, 8, 1.5), (5, 5, 5), "mud", share="hf")
        A.cube("h%s" % n, (sg * 7 - 1, 9, 0.5), (2, 2, 1), "ember", share="he")
    A.anim.clip("idle", length=4.0, loop=True)
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.anim.rot("idle", "h%s" % n, sway(4.0, 5.0, 0.2 * sg, axis=0, base=(0, 0, sg * 8)))
    return A.save()


def build_aux():
    A = new("aux", 64, 64, 142)
    A.bone("root", pivot=(0, 24, 0))
    for k in range(4):
        y = 8 + k * 3.0
        A.bone("o%d" % k, "root", (0, y, 0), rotation=(0, k * 90, 0))
        A.bone("g%d" % k, "o%d" % k, (10, y, 0))
        A.cube("g%d" % k, (9, y - 1.5, -1.5), (3, 3, 3), "mud", rot=(20, 20 * k, 10), share="ag")
        A.cube("g%d" % k, (9.5, y - 4, -0.5), (1, 3, 1), "wet", share="ad")
    A.anim.clip("idle", length=6.0, loop=True)
    for k in range(4):
        A.anim.rot("idle", "o%d" % k, {round(6.0 * i / 4, 2): (0, k * 90 + 90 * i * (1 if k % 2 == 0 else -1), 0) for i in range(5)})
    return A.save()


def build_skin():
    S = Skin(KEY, MATS, 7)
    S.put("torso", ("north", "south", "west", "east"), (0, 0, 8, 12), "mud")
    S.put("torso", ("north", "south"), (0, 8, 8, 4), "wet")
    for arm in ("rarm", "larm"):
        S.put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "mud")
        S.put(arm, ("north", "south", "west", "east"), (0, 8, 4, 4), "wet")
    for leg in ("rleg", "lleg"):
        S.put(leg, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "wet")
        S.put(leg, ("north", "south", "west", "east"), (0, 0, 4, 4), "mud")
    S.save()


# --------------------------------------------------------------------------------------------------------------------
def build_golem():
    A = new("golem", 256, 256, 151)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 22, 0))
    c = A.cube
    c("body", (-10, 20, -7), (20, 10, 14), "wet")
    c("body", (-12, 29, -8), (24, 14, 16), "mud")
    c("body", (-9, 42, -7), (18, 5, 14), "dry", inflate=0.3)
    c("body", (-6, 32, -9), (12, 9, 2), "dry")
    c("body", (-1.5, 35, -10), (3, 3, 1), "ember")
    c("body", (-8, 34, 8), (16, 12, 5), "mud", inflate=0.3)                            # back hump
    for k, (x, y) in enumerate(((-9, 34), (3, 28), (-5, 24), (6, 38))):
        c("body", (x, y, -8.4), (3, 4, 1), "ember" if k == 0 else "wet", share="cr%d" % k)
    for k, x in enumerate((-9, -4, 2, 8)):
        drip(A, "body", x, 30 - (k % 2) * 4, -8.2, 4 + (k % 3), 2, share="gd")
    A.bone("head", "body", (0, 48, 0))
    c("head", (-5, 47, -5), (10, 8, 9), "mud")
    c("head", (-5, 52, -6), (10, 1, 2), "dry")
    c("head", (-3.5, 49.5, -6), (2, 2, 1), "eye", share="eye")
    c("head", (1.5, 49.5, -6), (2, 2, 1), "eye", share="eye")
    c("head", (-3, 47.5, -6), (6, 1, 1), "wet")
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("arm_%s" % n, "body", (sg * 14, 42, 0))
        A.bone("fore_%s" % n, "arm_%s" % n, (sg * 17, 26, -1))
        A.side("arm_%s" % n, sg, (12, 26, -5), (9, 18, 10), "mud")
        A.side("arm_%s" % n, sg, (11.5, 40, -6), (11, 4, 12), "dry")
        A.side("fore_%s" % n, sg, (13, 8, -6), (11, 18, 12), "wet")
        A.side("fore_%s" % n, sg, (12, -4, -8), (14, 13, 16), "mud")                    # the fist
        A.side("fore_%s" % n, sg, (14, 3, -8.6), (5, 3, 1), "ember", share="fe")
        for k in range(3):
            drip(A, "fore_%s" % n, sg * (17 + (k - 1) * 3), -4, -2 + k * 3, 4, 2, share="fd")
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("leg_%s" % n, "root", (sg * 6, 22, 0))
        A.side("leg_%s" % n, sg, (1, 8, -6), (10, 14, 12), "mud")
        A.side("leg_%s" % n, sg, (0, 0, -9), (12, 8, 18), "wet")
        A.side("leg_%s" % n, sg, (3, 18, -6.4), (4, 3, 1), "ember", share="le")
    a = A.anim
    a.clip("idle", length=4.0, loop=True)
    a.pos("idle", "body", loop(4.0, lambda p: (0, 0.7 * math.sin(p * 2 * math.pi), 0)))
    a.scale("idle", "body", loop(4.0, lambda p: (1 + 0.01 * math.sin(p * 2 * math.pi), 1 - 0.015 * math.sin(p * 2 * math.pi), 1)))
    a.rot("idle", "head", sway(4.0, 4.0, 0.2, axis=1))
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("idle", "arm_%s" % n, sway(4.0, 3.0, 0.1 * sg, axis=0))
        a.rot("idle", "fore_%s" % n, sway(4.0, 4.0, 0.3 * sg, axis=0))
    a.clip("slam", length=1.2, loop=True)
    a.rot("slam", "body", {0.0: (0, 0, 0), 0.4: (-16, 0, 0), 0.55: (16, 0, 0), 0.7: (10, 0, 0), 1.2: (0, 0, 0)})
    a.pos("slam", "body", {0.0: (0, 0, 0), 0.4: (0, 3, 4), 0.55: (0, -3, -8), 0.7: (0, -2, -6), 1.2: (0, 0, 0)})
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("slam", "arm_%s" % n, {0.0: (0, 0, 0), 0.4: (-155, 0, 0), 0.55: (-8, 0, 0), 0.7: (-18, 0, 0), 1.2: (0, 0, 0)})
        a.rot("slam", "fore_%s" % n, {0.0: (0, 0, 0), 0.4: (-25, 0, 0), 0.55: (8, 0, 0), 0.7: (0, 0, 0), 1.2: (0, 0, 0)})
    a.clip("rise", length=1.1, loop=False)
    a.pos("rise", "body", {0.0: (0, -22, 0), 0.7: (0, 2, 0), 1.1: (0, 0, 0)})
    a.scale("rise", "body", {0.0: (0.6, 0.3, 0.6), 0.7: (1.05, 1.08, 1.05), 1.1: (1, 1, 1)})
    return A.save()


def build_spout():
    A = new("spout", 128, 128, 152)
    A.bone("root", pivot=(0, 0, 0))
    A.cube("root", (-10, 0, -10), (20, 3, 20), "wet")
    col = A.limb("col", "root", (0, 3, 0), (0, 0, 0), [8, 8, 8, 8], 12, 8, "mud", curl=(0, 0, 0), share="cl")
    A.bone("crown", col[3], (0, 35, 0))
    for k in range(8):
        a = k * 45.0
        x, z = 4.0 * math.cos(math.radians(a)), 4.0 * math.sin(math.radians(a))
        A.bone("sp%d" % k, "crown", (x, 34, z), rotation=lean(x, z, 38))
        A.cube("sp%d" % k, (x - 1.5, 34, z - 1.5), (3, 6, 3), "mud" if k % 2 else "dry", rot=(0, a, 0), pivot=(x, 34, z), share="sc%d" % (k % 2))
        A.cube("sp%d" % k, (x - 1, 39.5, z - 1), (2, 2, 2), "wet", share="sd")
    for k in range(8):
        a = k * 45.0 + 22
        drip(A, "root", 8.5 * math.cos(math.radians(a)), 18, 8.5 * math.sin(math.radians(a)), 6, 2, share="sdp")
    A.cube("root", (-1, 14, -6.6), (2, 3, 1), "ember")
    A.anim.clip("idle", length=2.5, loop=True)
    for j, nm in enumerate(col):
        A.anim.rot("idle", nm, loop(2.5, lambda p, j=j: (3 * math.sin(p * 2 * math.pi - j * 0.7), 0, 3 * math.cos(p * 2 * math.pi - j * 0.7))))
    for k in range(8):
        a = k * 45.0
        x, z = 4.0 * math.cos(math.radians(a)), 4.0 * math.sin(math.radians(a))
        A.anim.rot("idle", "sp%d" % k, sway(2.5, 6.0, 0.1 * k, axis=0 if abs(z) > abs(x) else 2, base=lean(x, z, 38)))
    A.anim.clip("erupt", length=0.9, loop=False)
    A.anim.scale("erupt", "root", {0.0: (1.0, 0.05, 1.0), 0.45: (0.9, 1.25, 0.9), 0.9: (1, 1, 1)})
    for k in range(8):
        a = k * 45.0
        x, z = 4.0 * math.cos(math.radians(a)), 4.0 * math.sin(math.radians(a))
        A.anim.rot("erupt", "sp%d" % k, {0.0: lean(x, z, 10), 0.45: lean(x, z, 70), 0.9: lean(x, z, 38)})
    return A.save()


if __name__ == "__main__":
    ids = [build_helm(), build_crown(), build_chest(), build_pauldron(-1), build_pauldron(1), build_bracer(-1), build_bracer(1),
           build_greave(-1), build_greave(1), build_back(), build_aux(), build_golem(), build_spout()]
    build_skin()
    run_check(ids)
    print("done:", ", ".join(ids))

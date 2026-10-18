"""Mucus Magic rebuild models: the Gelplate armour of the temporary armour (Aura.MUCUS) and the summons (the Mucus Construct, the Mucus Tendril).

    python3 -B tools/gen_mucus_rebuild_geo.py       # writes every model, animation, texture and the skin overlay, runs geo_check

Everything of this magic is translucent gel (drawn on the alpha-blended render layer, see client/aura/MucusAura.java): the texture alpha is part of the look.
Armour (Space.PLAYER, anchored on the model part each piece was built for):
  mucus_helm        a rounded gel dome with a clear visor, bubbles in it, a glowing nucleus on the brow and strings dripping from the sides (head)
  mucus_crown       eight bubbles of different sizes circling and bobbing above the head (head)
  mucus_chest       a swollen gel bladder over the chest with a glowing nucleus inside, a belt of beads, a veil of drips down the back (body)
  mucus_pauld_r/l   stacked wobbling gel blobs on the shoulders (arms)
  mucus_bracer_r/l  gel gloves: a thick cuff, a blobby fist and dripping strings (arms)
  mucus_greave_r/l  puddle boots: a wide flat foot blob, an ankle ring, a splash flare (legs)
  mucus_back        four pseudopod tendrils (thick to thin, ending in bubbles) that wave behind the body (body)
  mucus_aux         six bubbles orbiting the body at different heights (body)
Summons (prop kind MUCUS_1):
  mucus_construct   the Mucus Construct: a wobbling gel mound with a glowing nucleus, two sunken eye pits, a skirt of strings; clips idle, absorb, rise
  mucus_tendril     a pseudopod: a thick stalk of five joints ending in a clump of bubbles; clips idle, strike
Texture style: teal translucent gel, bright rims, bubbles with clear centres, a glowing nucleus.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_lib import *   # noqa: E402,F401,F403

KEY = "mucus"
MATS = {
    "gel": dict(kind="gel", alpha=190, ramp=[(30, 110, 100), (60, 170, 150), (110, 220, 190), (170, 245, 220), (232, 255, 248)], glowc=(140, 255, 220), rim_glow=110),
    "gel_dark": dict(kind="gel", alpha=205, ramp=[(18, 80, 84), (40, 130, 126), (80, 184, 168), (140, 226, 204), (210, 250, 238)], glowc=(120, 240, 210), rim_glow=80),
    "gel_pale": dict(kind="gel", alpha=170, ramp=[(70, 150, 150), (110, 200, 190), (160, 232, 216), (210, 250, 238), (246, 255, 252)], glowc=(180, 255, 240), rim_glow=130),
    "string": dict(kind="plain", alpha=150, ramp=[(60, 160, 150), (100, 210, 190), (150, 238, 216), (200, 252, 238), (240, 255, 250)]),
    "nucleus": dict(kind="core", alpha=230, ramp=[(10, 100, 100), (30, 170, 160), (90, 235, 210), (190, 255, 240), (250, 255, 252)], glowc=(110, 255, 225)),
    "eye": dict(kind="core", alpha=240, ramp=[(10, 50, 60), (20, 90, 100), (40, 140, 150), (110, 220, 220), (220, 255, 255)], glowc=(120, 255, 240)),
}


def new(name, w, h, seed):
    return Atlas(KEY, name, w, h, seed=seed, mats=MATS)


def bead(A, bone, c, s, mat="gel_pale", share="bd"):
    A.cube(bone, (c[0] - s / 2.0, c[1] - s / 2.0, c[2] - s / 2.0), (s, s, s), mat, share="%s%d" % (share, s))


def strand(A, bone, x, y, z, ln, w=1, mat="string"):
    A.cube(bone, (x - w / 2.0, y - ln, z - w / 2.0), (w, ln, w), mat, share="st%d%d" % (ln, w))


# --------------------------------------------------------------------------------------------------------------------
def build_helm():
    A = new("helm", 64, 64, 161)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("head", "root", (0, 24, 0))
    c = A.cube
    c("head", (-5, 30, -5), (10, 3, 10), "gel", inflate=0.1)                           # the dome in three steps
    c("head", (-4, 33, -4), (8, 2, 8), "gel_pale", inflate=0.1)
    c("head", (-3, 35, -3), (6, 1, 6), "gel", inflate=0.1)
    c("head", (-5, 25, -4), (1, 5, 8), "gel_dark", inflate=0.1)
    c("head", (4, 25, -4), (1, 5, 8), "gel_dark", inflate=0.1)
    c("head", (-4, 25, 4), (8, 5, 1), "gel_dark", inflate=0.1)
    c("head", (-1.5, 30.4, -5.6), (3, 3, 2), "nucleus", share="nu")
    bead(A, "head", (-3.5, 36.5, 0), 2)
    bead(A, "head", (2.5, 37, 1), 3)
    bead(A, "head", (0, 38.5, -2), 2)
    A.bone("dr", "head", (0, 30, 0))
    for k, x in enumerate((-5, 5, -4.4, 4.4)):
        strand(A, "dr", x, 29, -3 + (k % 2) * 4, 3 + (k % 3) * 2)
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.scale("idle", "dr", loop(3.0, lambda p: (1, 1 + 0.25 * math.sin(p * 2 * math.pi), 1)))
    return A.save()


def build_crown():
    A = new("crown", 64, 64, 162)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("ring", "root", (0, 38, 0))
    for k in range(8):
        a = k * 45.0
        r = 5.5 + (k % 3) * 1.2
        x, z = r * math.cos(math.radians(a)), r * math.sin(math.radians(a))
        A.bone("b%d" % k, "ring", (x, 38, z))
        s = 2 + (k % 3)
        A.cube("b%d" % k, (x - s / 2.0, 38 + (k % 2) * 1.5 - s / 2.0, z - s / 2.0), (s, s, s), "gel_pale" if k % 2 == 0 else "gel", share="cb%d%d" % (s, k % 2))
    A.anim.clip("idle", length=8.0, loop=True)
    A.anim.rot("idle", "ring", spin(8.0, axis=1, turns=1))
    for k in range(8):
        A.anim.pos("idle", "b%d" % k, loop(8.0, lambda p, k=k: (0, 1.5 * math.sin((p + k / 8.0) * 4 * math.pi), 0)))
        A.anim.scale("idle", "b%d" % k, swell(8.0, 0.12, k / 8.0))
    return A.save()


def build_chest():
    A = new("chest", 128, 128, 163)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-5, 14, -4), (10, 9, 3), "gel", inflate=0.2)                           # the bladder in layers
    c("torso", (-4, 15, -5.5), (8, 6, 2), "gel_pale", inflate=0.1)
    c("torso", (-3, 16, -7), (6, 4, 2), "gel", inflate=0.1)
    c("torso", (-2, 17, -3), (4, 4, 3), "nucleus", share="nu")                          # the nucleus, seen through the layers
    c("torso", (-5.5, 11.5, -3.5), (11, 2, 7), "gel_dark", inflate=0.2)                 # belt
    for k in range(5):
        bead(A, "torso", (-4 + k * 2, 10.3, -4.2), 2, share="bb")
    c("torso", (-5, 12, 2), (10, 11, 2), "gel_dark", inflate=0.2)                       # the back
    A.bone("veil", "torso", (0, 23, 3))
    for k, x in enumerate((-4, -2, 0, 2, 4)):
        strand(A, "veil", x, 23, 3.8, 8 + (k % 3) * 3, 1, "string")
    A.bone("bub", "torso", (0, 17, -6))
    bead(A, "bub", (-3.5, 21, -6.2), 2)
    bead(A, "bub", (3.5, 14.5, -6.2), 3)
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.scale("idle", "veil", loop(3.0, lambda p: (1, 1 + 0.12 * math.sin(p * 2 * math.pi), 1)))
    A.anim.scale("idle", "bub", swell(3.0, 0.1))
    return A.save()


def build_pauldron(sg):
    n = "r" if sg < 0 else "l"
    A = new("pauld_" + n, 64, 64, 164 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    A.bone("blob", "arm", (sg * 6.5, 24, 0))
    s = lambda *a, **k: A.side("blob", sg, *a, **k)
    s((3.5, 23, -3.5), (7, 3, 7), "gel", inflate=0.3, share="pa")
    s((4.5, 26, -2.5), (5, 2, 5), "gel_pale", inflate=0.2, share="pb")
    s((5.5, 28, -1.5), (3, 2, 3), "gel", inflate=0.2, share="pc")
    s((8.5, 23, -1), (2, 2, 2), "nucleus", share="pn")
    for k, z in enumerate((-2.5, 0.5, 3)):
        strand(A, "arm", sg * 10.2, 23.5, z, 3 + k % 2)
    A.anim.clip("idle", length=2.4, loop=True)
    A.anim.scale("idle", "blob", loop(2.4, lambda p: (1 + 0.05 * math.sin(p * 2 * math.pi), 1 - 0.07 * math.sin(p * 2 * math.pi), 1 + 0.05 * math.sin(p * 2 * math.pi))))
    return A.save()


def build_bracer(sg):
    n = "r" if sg < 0 else "l"
    A = new("bracer_" + n, 64, 64, 166 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    s((3.5, 12, -2.5), (5, 6, 5), "gel", inflate=0.4, share="ba")
    s((3.5, 17, -2.5), (5, 2, 5), "gel_pale", inflate=0.5, share="bb")
    s((3, 6, -3.5), (6, 6, 7), "gel_dark", inflate=0.3, share="bf")
    s((5, 8, -4.6), (2, 2, 1), "nucleus", share="bn")
    A.bone("st", "arm", (sg * 6, 6, 0))
    for k, z in enumerate((-2.5, 0, 2.5)):
        strand(A, "st", sg * 6.2, 6, z, 4 + (k % 2) * 2)
    A.anim.clip("idle", length=2.4, loop=True)
    A.anim.scale("idle", "st", loop(2.4, lambda p: (1, 1 + 0.2 * math.sin(p * 2 * math.pi), 1)))
    return A.save()


def build_greave(sg):
    n = "r" if sg < 0 else "l"
    A = new("greave_" + n, 64, 64, 168 + (sg > 0))
    A.bone("leg", pivot=(sg * 1.9, 12, 0))
    s = lambda *a, **k: A.side("leg", sg, *a, **k)
    s((0, 2.5, -3), (5, 6, 6), "gel", inflate=0.3, share="ga")
    s((-1, 0, -4.5), (7, 3, 9), "gel_dark", inflate=0.2, share="gb")
    s((0.2, 9, -3), (4.6, 2, 6), "gel_pale", inflate=0.4, share="gc")
    bead(A, "leg", (sg * 2.5, 11.5, -4.2), 2, share="gk")
    for k in range(4):
        a = k * 90.0 + 45
        x, z = sg * 2.4 + 4.6 * math.cos(math.radians(a)), 4.6 * math.sin(math.radians(a))
        A.bone("sp%d" % k, "leg", (x, 1, z), rotation=lean(x - sg * 2.4, z, 50))
        A.cube("sp%d" % k, (x - 1.5, 0, z - 0.5), (3, 2, 1), "gel_pale", rot=(0, -a + 90, 0), pivot=(x, 1, z), share="gs")
    return A.save()


def build_back():
    A = new("back", 128, 128, 170)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    A.cube("torso", (-4, 14, 2), (8, 8, 3), "gel_dark", inflate=0.2)
    A.cube("torso", (-1.5, 17, 4.8), (3, 3, 2), "nucleus", share="bn")
    A.anim.clip("idle", length=4.0, loop=True)
    spec = (((-3, 22, 4), (-60, 0, 30)), ((3, 22, 4), (-60, 0, -30)), ((-2, 16, 4), (-35, 0, 60)), ((2, 16, 4), (-35, 0, -60)))
    for k, (base, tilt) in enumerate(spec):
        sgx = 1 if base[0] > 0 else -1
        t = A.limb("td%d" % k, "torso", base, tilt, [6, 6, 5, 5, 4], 5, 2, "gel" if k % 2 == 0 else "gel_dark", curl=(16, 0, -sgx * 4), share="tp%d" % (k % 2))
        e = A.tips[t[4]]
        A.cube(t[4], (e[0] - 2, e[1], e[2] - 2), (4, 4, 4), "gel_pale", share="te")
        A.cube(t[4], (e[0] - 0.5, e[1] + 1, e[2] - 0.5), (1, 1, 1), "nucleus", share="tn")
        for j in range(5):
            A.anim.rot("idle", t[j], sway(4.0, 6.0 + j, 0.14 * j + 0.25 * k, axis=2, base=(tilt if j == 0 else (16, 0, -sgx * 4))))
    return A.save()


def build_aux():
    A = new("aux", 64, 64, 172)
    A.bone("root", pivot=(0, 24, 0))
    for k in range(6):
        y = 8 + k * 2.8
        A.bone("o%d" % k, "root", (0, y, 0), rotation=(0, k * 60, 0))
        A.bone("b%d" % k, "o%d" % k, (10 + (k % 2) * 2, y, 0))
        s = 2 + (k % 3)
        A.cube("b%d" % k, (9.5 + (k % 2) * 2 - s / 2.0, y - s / 2.0, -s / 2.0), (s, s, s), "gel_pale" if k % 2 == 0 else "gel", share="ab%d%d" % (s, k % 2))
    A.anim.clip("idle", length=6.0, loop=True)
    for k in range(6):
        d = 1 if k % 2 == 0 else -1
        A.anim.rot("idle", "o%d" % k, {round(6.0 * i / 4, 2): (0, k * 60 + 90 * i * d, 0) for i in range(5)})
        A.anim.scale("idle", "b%d" % k, swell(6.0, 0.15, k / 6.0))
    return A.save()


def build_skin():
    S = Skin(KEY, MATS, 8)
    S.put("torso", ("north", "south", "west", "east"), (0, 0, 8, 12), "gel")
    S.put("torso", ("north",), (2, 4, 4, 4), "gel_pale")
    for arm in ("rarm", "larm"):
        S.put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "gel")
    for leg in ("rleg", "lleg"):
        S.put(leg, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "gel_dark")
    S.save()


# --------------------------------------------------------------------------------------------------------------------
def build_construct():
    A = new("construct", 256, 256, 181)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 12, 0))
    c = A.cube
    c("body", (-12, 0, -10), (24, 6, 20), "gel_dark")                                   # the mound, built in tiers
    c("body", (-11, 6, -9), (22, 8, 18), "gel")
    c("body", (-9, 14, -8), (18, 8, 16), "gel_pale")
    c("body", (-6, 22, -6), (12, 5, 12), "gel")
    c("body", (-3, 27, -3), (6, 3, 6), "gel_pale")
    c("body", (-4, 10, -4), (8, 8, 8), "nucleus", share="nu")                            # the nucleus in the middle
    c("body", (-4.5, 17, -9.4), (3, 3, 1), "eye", share="ey")                            # two sunken eye pits
    c("body", (1.5, 17, -9.4), (3, 3, 1), "eye", share="ey")
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("pseudo_%s" % n, "body", (sg * 11, 12, 0), rotation=(0, 0, -sg * 40))
        A.cube("pseudo_%s" % n, (sg * 11 - 3, 12, -3), (6, 7, 6), "gel", share="pa")
        A.cube("pseudo_%s" % n, (sg * 11 - 2.5, 19, -2.5), (5, 5, 5), "gel_pale", share="pb")
    A.bone("skirt", "body", (0, 2, 0))
    for k in range(10):
        a = k * 36.0
        x, z = 10.5 * math.cos(math.radians(a)), 8.5 * math.sin(math.radians(a))
        strand(A, "skirt", x, 2, z, 3 + (k % 3) * 2, 2)
    for k, (x, y, z) in enumerate(((-7, 24, 2), (5, 25, -4), (8, 18, 6), (-8, 15, -7))):
        bead(A, "body", (x, y, z), 3 + (k % 2), share="bu")
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    a.scale("idle", "body", loop(3.0, lambda p: (1 + 0.04 * math.sin(p * 2 * math.pi), 1 - 0.06 * math.sin(p * 2 * math.pi), 1 + 0.04 * math.sin(p * 2 * math.pi))))
    a.scale("idle", "skirt", loop(3.0, lambda p: (1, 1 + 0.3 * math.sin(p * 2 * math.pi + 1), 1)))
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("idle", "pseudo_%s" % n, sway(3.0, 8.0, 0.2 * sg, axis=2, base=(0, 0, -sg * 40)))
    a.clip("absorb", length=1.0, loop=True)
    a.scale("absorb", "body", {0.0: (1, 1, 1), 0.35: (1.28, 0.7, 1.28), 0.6: (0.9, 1.25, 0.9), 1.0: (1, 1, 1)})
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("absorb", "pseudo_%s" % n, {0.0: (0, 0, -sg * 40), 0.35: (-40, 0, -sg * 10), 0.6: (-20, 0, -sg * 30), 1.0: (0, 0, -sg * 40)})
    a.clip("rise", length=1.0, loop=False)
    a.scale("rise", "body", {0.0: (1.4, 0.1, 1.4), 0.6: (0.92, 1.18, 0.92), 1.0: (1, 1, 1)})
    return A.save()


def build_tendril():
    A = new("tendril", 128, 128, 182)
    A.bone("root", pivot=(0, 0, 0))
    A.cube("root", (-5, 0, -5), (10, 3, 10), "gel_dark")
    t = A.limb("td", "root", (0, 3, 0), (0, 0, 0), [8, 8, 7, 6, 5], 8, 3, "gel", curl=(0, 0, 6), share="tp")
    e = A.tips[t[4]]
    for k, (dx, dy, dz, s) in enumerate(((0, 0, 0, 5), (-3, 2, 1, 3), (3, 3, -1, 3), (0, 5, 0, 2))):
        A.cube(t[4], (e[0] + dx - s / 2.0, e[1] + dy - 1, e[2] + dz - s / 2.0), (s, s, s), "gel_pale" if k % 2 == 0 else "nucleus", share="tb%d" % k)
    A.cube(t[2], (A.tips[t[2]][0] - 1, A.tips[t[2]][1] - 3, A.tips[t[2]][2] - 4.2), (2, 2, 2), "nucleus", share="tn")
    a = A.anim
    a.clip("idle", length=2.4, loop=True)
    for j, nm in enumerate(t):
        a.rot("idle", nm, loop(2.4, lambda p, j=j: (5 * math.sin(p * 2 * math.pi - j * 0.6), 0, 6 * (1 if j else 0) + 6 * math.cos(p * 2 * math.pi - j * 0.6))))
    a.clip("strike", length=0.7, loop=True)
    for j, nm in enumerate(t):
        a.rot("strike", nm, {0.0: (-6 - j, 0, 6), 0.25: (-16 - 2 * j, 0, 4), 0.38: (30 + 4 * j, 0, 0), 0.7: (0, 0, 6)})
    return A.save()


if __name__ == "__main__":
    ids = [build_helm(), build_crown(), build_chest(), build_pauldron(-1), build_pauldron(1), build_bracer(-1), build_bracer(1),
           build_greave(-1), build_greave(1), build_back(), build_aux(), build_construct(), build_tendril()]
    build_skin()
    run_check(ids)
    print("done:", ", ".join(ids))

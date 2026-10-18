"""Vine Magic rebuild models: the Thornbine armour of the temporary armour (Aura.VINE) and the summons (the Vine Serpent, the Vine Whip, the Thicket Wall).

    python3 -B tools/gen_vine_rebuild_geo.py        # writes every model, animation, texture and the skin overlay, runs geo_check

Armour (Space.PLAYER, anchored on the model part each piece was built for, see client/aura/VineAura.java):
  vine_helm        a circlet of braided vine with a leaf fringe, two curling tendril horns and a glowing bud on the brow (head)
  vine_crown       a ring of thorns and buds floating above the head, turning (head)
  vine_chest       criss-cross vine bands over the torso with thorn studs and a glowing bud at the crossing, a leaf collar, belt, back bands (body)
  vine_pauld_r/l   a fan of broad leaves over the shoulder with a curling tendril (arms)
  vine_bracer_r/l  spiral vine wraps with a row of thorns and three tendril fingers that grope (arms)
  vine_greave_r/l  wrapped greaves with a thorn knee and tendrils gripping the ground (legs)
  vine_back        four long jointed vine whips arching over the shoulders, thorned, budded at the tips (body)
  vine_aux         five thorn seeds spiralling round the body at different heights (body)
Summons (prop kind VINE_1):
  vine_serpent     the Vine Serpent: a thorned vine-snake, fourteen jointed segments, leaf frill, glowing eyes and fangs; clips idle, strike, rise
  vine_whip        a whip of vine growing from the ground: eight segments, thorns, a bud at the tip; clips idle, lash
  vine_wall        a thicket panel: three twisted columns, diagonal cross vines, thorn rows, leaves; clip idle (sway)
Texture style: pixel-art twisted strands with dark grooves, ivory thorns, lime glow in the buds.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_lib import *   # noqa: E402,F401,F403

KEY = "vine"
MATS = {
    "vine": dict(kind="vine", ramp=[(8, 36, 12), (20, 72, 22), (44, 122, 34), (84, 170, 48), (158, 224, 94)], glowc=(190, 255, 90), thorn_glow=160),
    "vine_dark": dict(kind="vine", ramp=[(6, 26, 10), (14, 50, 18), (30, 88, 26), (58, 128, 36), (104, 176, 62)], glowc=(150, 240, 70), thorn_glow=90),
    "leaf": dict(kind="leaf", ramp=[(10, 52, 14), (26, 98, 28), (56, 152, 44), (104, 202, 62), (178, 240, 112)], glowc=(190, 255, 100), vein_glow=70),
    "leaf_dark": dict(kind="leaf", ramp=[(8, 38, 10), (18, 70, 20), (36, 110, 30), (66, 150, 42), (116, 196, 70)], glowc=(150, 240, 80), vein_glow=40),
    "thorn": dict(kind="thorn", ramp=[(50, 44, 28), (120, 108, 70), (186, 172, 118), (226, 216, 168), (252, 246, 218)]),
    "bud": dict(kind="core", ramp=[(30, 90, 10), (70, 160, 30), (140, 224, 60), (206, 255, 130), (244, 255, 214)], glowc=(196, 255, 100)),
    "eye": dict(kind="core", ramp=[(90, 40, 0), (190, 110, 10), (255, 200, 40), (255, 236, 130), (255, 252, 220)], glowc=(255, 220, 80)),
}


def new(name, w, h, seed):
    return Atlas(KEY, name, w, h, seed=seed, mats=MATS)


def thorn_at(A, bone, base, axis, ln=3, w=2, share="th"):
    A.spike(bone, base, axis, ln, w, "thorn", layers=2, share=share)


# --------------------------------------------------------------------------------------------------------------------
def build_helm():
    A = new("helm", 64, 64, 41)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("head", "root", (0, 24, 0))
    c = A.cube
    c("head", (-5, 29, -5), (10, 2, 1), "vine", inflate=0.1)                       # the braided circlet
    c("head", (-5, 29, 4), (10, 2, 1), "vine", inflate=0.1)
    c("head", (-5, 29, -4), (1, 2, 8), "vine", inflate=0.1)
    c("head", (4, 29, -4), (1, 2, 8), "vine", inflate=0.1)
    c("head", (-4, 31, -5), (8, 1, 1), "vine_dark", inflate=0.1)                    # a second, thinner strand above it
    c("head", (-1, 29.6, -5.8), (2, 2, 1), "bud", rot=(0, 0, 45))                  # the bud on the brow
    for k in range(5):                                                              # the leaf fringe over the brow
        x = -3.5 + k * 1.75
        A.leaf("head", (x, 31.8, -5.4), (2, 3), "leaf" if k % 2 == 0 else "leaf_dark", rot=(-28, 0, (k - 2) * 10), share="fr%d" % (k % 2))
    A.anim.clip("idle", length=4.0, loop=True)
    for sg in (-1, 1):                                                              # two tendril horns that curl like fern fronds
        n = "r" if sg < 0 else "l"
        h = A.limb("horn%s" % n, "head", (sg * 3, 31, 0), (-12, 0, -sg * 22), [5, 4, 3, 3, 2], 3, 1, "vine", curl=(0, 0, sg * 26), share="hn")
        thorn_at(A, h[0], (sg * 4.5, 33, 0), "+x" if sg > 0 else "-x", 2, 2, "ht")
        A.cube(h[4], (A.tips[h[4]][0] - 1, A.tips[h[4]][1], A.tips[h[4]][2] - 1), (2, 2, 2), "bud", share="hb")
        A.anim.rot("idle", h[0], sway(4.0, 3.0, 0.15 * sg, base=(-12, 0, -sg * 22)))
        A.anim.rot("idle", h[2], sway(4.0, 5.0, 0.35 * sg, base=(0, 0, sg * 26)))
    return A.save()


def build_crown():
    A = new("crown", 64, 64, 42)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("ring", "root", (0, 37, 0))
    for k in range(6):
        a = k * 60.0
        x, z = 6.2 * math.cos(math.radians(a)), 6.2 * math.sin(math.radians(a))
        A.bone("t%d" % k, "ring", (x, 36.5, z), rotation=lean(x, z, 32))
        A.spike("t%d" % k, (x, 36.5, z), "+y", 6, 2, "thorn", layers=3, share="ct")
        a2 = math.radians(a + 30)
        A.cube("ring", (5.6 * math.cos(a2) - 1, 36.8, 5.6 * math.sin(a2) - 1), (2, 2, 2), "bud", share="cb")
        A.cube("ring", (6.2 * math.cos(a2) - 0.5, 36.2, 6.2 * math.sin(a2) - 0.5), (1, 1, 1), "vine", share="cv")
    A.anim.clip("idle", length=8.0, loop=True)
    A.anim.rot("idle", "ring", spin(8.0, axis=1, turns=-1))
    return A.save()


def build_chest():
    A = new("chest", 128, 128, 43)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-1, 10, -4), (2, 17, 1), "vine", rot=(0, 0, 38), inflate=0.1, pivot=(0, 18.5, -3.5))      # the criss-cross bands
    c("torso", (-1, 10, -4), (2, 17, 1), "vine", rot=(0, 0, -38), inflate=0.1, pivot=(0, 18.5, -3.5))
    c("torso", (-1.5, 17, -5), (3, 3, 1), "bud", rot=(0, 0, 45))                                          # the bud at the crossing
    for k, (x, y) in enumerate(((-3.5, 21), (3.5, 21), (-3.5, 15), (3.5, 15))):
        thorn_at(A, "torso", (x, y, -4.2), "-z", 2, 2, "ts")
    c("torso", (-5, 12, -3), (10, 2, 6), "vine_dark", inflate=0.2)                                        # belt
    c("torso", (-4.5, 14, 2), (9, 9, 1), "vine_dark", inflate=0.15)                                       # the back
    c("torso", (-1, 10, 2.6), (2, 17, 1), "vine", rot=(0, 0, 36), pivot=(0, 18.5, 3.1))
    c("torso", (-1, 10, 2.6), (2, 17, 1), "vine", rot=(0, 0, -36), pivot=(0, 18.5, 3.1))
    A.bone("collar", "torso", (0, 23, 0))
    for k in range(7):                                                                                     # the leaf collar
        a = -90 + k * 360 / 7.0
        x, z = 5.2 * math.cos(math.radians(a)), 3.8 * math.sin(math.radians(a))
        A.leaf("collar", (x, 24, z), (3, 3), "leaf" if k % 2 == 0 else "leaf_dark", rot=(-30 if z < 0 else 30, a + 90, 20 * (1 if x > 0 else -1)), share="co%d" % (k % 2))
    A.bone("hang", "torso", (0, 13, -3))
    for k, x in enumerate((-3, 0, 3)):
        A.leaf("hang", (x, 9.5, -3.2), (3, 4), "leaf" if k != 1 else "leaf_dark", rot=(8, 0, (k - 1) * 8), share="hl%d" % (k % 2))
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "hang", sway(3.0, 3.0, axis=0))
    A.anim.rot("idle", "collar", sway(3.0, 1.5, axis=1))
    return A.save()


def build_pauldron(sg):
    n = "r" if sg < 0 else "l"
    A = new("pauld_" + n, 64, 64, 44 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    A.side("arm", sg, (3.5, 23, -3), (6, 2, 6), "vine_dark", inflate=0.2, share="pa")
    A.side("arm", sg, (4, 25, -2), (5, 1, 4), "vine", inflate=0.1, share="pb")
    A.bone("fan", "arm", (sg * 6, 25, 0))
    leaves = []
    for k in range(4):
        th = (-10 + 34 * k) if sg > 0 else (10 - 34 * k)
        r = 5 + (k % 2)
        x, y = sg * 6 + r * math.sin(math.radians(th)), 26 + r * math.cos(math.radians(th))
        A.leaf("fan", (x, y, 0), (3, 6), "leaf" if k % 2 == 0 else "leaf_dark", rot=(0, 0, -th), share="pl%d" % (k % 2))
    tend = A.limb("te%s" % n, "arm", (sg * 7.5, 25, 1.5), (-20, 0, -sg * 38), [3, 3, 2, 2], 2, 1, "vine", curl=(0, 0, sg * 30), share="pt")
    A.cube("arm", (sg * 6 - 1, 25.5, -0.5), (2, 2, 2), "bud", share="pbud")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "fan", sway(3.0, 5.0, 0.1 * sg, base=(0, 0, 0)))
    A.anim.rot("idle", tend[1], sway(3.0, 6.0, 0.3, base=(0, 0, sg * 30)))
    return A.save()


def build_bracer(sg):
    n = "r" if sg < 0 else "l"
    A = new("bracer_" + n, 64, 64, 46 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    for k, y in enumerate((12, 14.5, 17)):                                           # three spiral wraps, each turned a little
        s((3.5, y, -2.5), (5, 2, 5), "vine", inflate=0.3, rot=(0, 18 * (k - 1), 0), share="bw")
    s((3.6, 18.6, -2.5), (5, 1, 5), "vine_dark", inflate=0.35, share="bx")
    for k, y in enumerate((13, 15, 17)):
        thorn_at(A, "arm", (sg * 8.7, y, 0), "+x" if sg > 0 else "-x", 2, 2, "bt")
    s((3.5, 9.5, -2.5), (5, 2, 5), "vine_dark", inflate=0.3, share="bg")             # the knuckle wrap
    A.anim.clip("idle", length=3.0, loop=True)
    for k, z in enumerate((-1.6, 0, 1.6)):                                            # three tendril fingers
        f = A.limb("fi%s%d" % (n, k), "arm", (sg * 6, 9.5, z), (180, 0, 0), [3, 3, 2], 1, 1, "vine", curl=(10, 0, sg * 8), share="fg")
        A.anim.rot("idle", f[0], sway(3.0, 8.0, 0.2 * k, axis=0, base=(180, 0, 0)))
    return A.save()


def build_greave(sg):
    n = "r" if sg < 0 else "l"
    A = new("greave_" + n, 64, 64, 48 + (sg > 0))
    A.bone("leg", pivot=(sg * 1.9, 12, 0))
    s = lambda *a, **k: A.side("leg", sg, *a, **k)
    for k, y in enumerate((1, 3.5, 6)):
        s((0.3, y, -2.5), (4.4, 2, 5), "vine", inflate=0.3, rot=(0, 16 * (k - 1), 0), share="gw")
    s((0.4, 8.5, -3), (4, 3, 5), "vine_dark", inflate=0.3, share="gk")
    thorn_at(A, "leg", (sg * 2.5, 10, -3.8), "-z", 3, 2, "gt")
    thorn_at(A, "leg", (sg * 4.9, 5, 0), "+x" if sg > 0 else "-x", 2, 2, "gu")
    s((0, 0, -3.5), (4.4, 1.5 if False else 2, 2), "vine_dark", inflate=0.1, share="ge")
    A.anim.clip("idle", length=3.0, loop=True)
    for k in range(2):
        t = A.limb("gt%s%d" % (n, k), "leg", (sg * (1.5 + 2 * k), 1, -3), (140, 0, sg * (10 + 6 * k)), [3, 3, 2], 1, 1, "vine", curl=(14, 0, 0), share="gtt")
        A.anim.rot("idle", t[0], sway(3.0, 7.0, 0.3 * k, axis=0, base=(140, 0, sg * (10 + 6 * k))))
    return A.save()


def build_back():
    A = new("back", 128, 128, 50)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-4, 14, 2), (8, 6, 2), "vine_dark", inflate=0.2)
    c("torso", (-1, 17, 3.6), (2, 2, 1), "bud")
    A.anim.clip("idle", length=4.0, loop=True)
    spec = (((-3, 21, 3), (-55, 0, 28), 1.0, "a"), ((3, 21, 3), (-55, 0, -28), 1.0, "b"), ((-2, 16, 3), (-30, 0, 55), 0.8, "c"), ((2, 16, 3), (-30, 0, -55), 0.8, "d"))
    for k, (base, tilt, sc, tag) in enumerate(spec):
        sgx = 1 if base[0] > 0 else -1
        segs = [int(6 * sc), int(6 * sc), int(5 * sc), int(5 * sc), int(4 * sc)]
        w = A.limb("wh" + tag, "torso", base, tilt, segs, 3, 1, "vine", curl=(20, 0, -sgx * 4), share="wp" + tag)
        for j in (0, 2):
            thorn_at(A, w[j], (base[0] + 2.2 * sgx, A.tips[w[j]][1] - 2, base[2]), "+x" if sgx > 0 else "-x", 2, 2, "wt" + tag + str(j))
        A.cube(w[4], (A.tips[w[4]][0] - 1, A.tips[w[4]][1], A.tips[w[4]][2] - 1), (2, 2, 2), "bud", share="wb")
        A.leaf(w[3], (A.tips[w[3]][0] + sgx * 2, A.tips[w[3]][1] - 2, A.tips[w[3]][2]), (3, 4), "leaf", rot=(0, 0, -sgx * 25), share="wl")
        for j in range(5):
            A.anim.rot("idle", w[j], sway(4.0, 5.0 + j, 0.12 * j + 0.25 * k, axis=2 if j else 2, base=(tilt if j == 0 else (20, 0, -sgx * 4))))
    return A.save()


def build_aux():
    A = new("aux", 64, 64, 52)
    A.bone("root", pivot=(0, 24, 0))
    for k in range(5):
        y = 12 + k * 3.2
        A.bone("o%d" % k, "root", (0, y, 0), rotation=(0, k * 72, 0))
        A.bone("s%d" % k, "o%d" % k, (9 + (k % 2) * 2, y, 0), rotation=(0, 0, -30))
        A.cube("s%d" % k, (8.5 + (k % 2) * 2, y - 1, -1), (2, 2, 2), "bud", share="ab")
        A.spike("s%d" % k, (9.5 + (k % 2) * 2, y + 1, 0), "+y", 4, 2, "thorn", layers=2, share="at")
    A.anim.clip("idle", length=6.0, loop=True)
    for k in range(5):
        d = 1 if k % 2 == 0 else -1
        A.anim.rot("idle", "o%d" % k, {round(6.0 * i / 4, 2): (0, k * 72 + 90 * i * d, 0) for i in range(5)})
    return A.save()


def build_skin():
    S = Skin(KEY, MATS, 4)
    S.put("torso", ("north", "south", "west", "east"), (0, 0, 8, 12), "vine")
    for arm in ("rarm", "larm"):
        S.put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "vine_dark")
        S.put(arm, ("north", "south", "west", "east"), (0, 5, 4, 1), "thorn")
    for leg in ("rleg", "lleg"):
        S.put(leg, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "vine_dark")
    S.save()


# --------------------------------------------------------------------------------------------------------------------
def build_serpent():
    A = new("serpent", 128, 128, 61)
    A.bone("root", pivot=(0, 0, 0))
    segs = chain_z(A, "sg", "root", (0, 6, 22), 14, 5, 7, 2, 7, 2, "vine", yaw=0.0, mat2="vine_dark", share="sg")
    # chain_z builds towards -z from the tail (z = 22) to the head side; the head sits on the last bone
    tipz = 22 - 14 * 5
    A.bone("head", segs[-1], (0, 6, tipz))
    c = A.cube
    c("head", (-4, 2, tipz - 9), (8, 7, 9), "vine")
    c("head", (-3, 3, tipz - 10), (6, 3, 1), "vine_dark")
    c("head", (-3.5, 6.2, tipz - 8), (2, 2, 1), "eye", share="eye")
    c("head", (1.5, 6.2, tipz - 8), (2, 2, 1), "eye", share="eye")
    A.bone("jaw", "head", (0, 3, tipz - 2))
    c("jaw", (-3, 1, tipz - 9), (6, 2, 8), "vine_dark")
    for sx in (-2.5, 1.5):
        thorn_at(A, "head", (sx + 0.5, 2.5, tipz - 9), "-y", 3, 1, "fang%d" % int(sx))
    A.bone("frill", "head", (0, 7, tipz - 1))
    for k in range(5):
        th = -60 + k * 30
        A.leaf("frill", (6 * math.sin(math.radians(th)), 7 + 5 * math.cos(math.radians(th)) * 0.9, tipz - 1), (4, 6), "leaf" if k % 2 == 0 else "leaf_dark",
               rot=(0, 0, -th), share="fr%d" % (k % 2))
    for k in range(0, 14, 2):                                                      # a ridge of thorns down the back
        A.spike(segs[k], (0, 6 + 1 + (7 - k * 0.35) / 2.0, 22 - k * 5 - 2.5), "+y", 3, 2, "thorn", layers=2, share="rt")
    A.cube(segs[-1], (-1, 5, 22 - 14 * 5 - 5), (2, 2, 2), "bud", share="sb")
    a = A.anim
    a.clip("idle", length=2.4, loop=True)
    for k, nm in enumerate(segs):
        a.rot("idle", nm, loop(2.4, lambda p, k=k: (0, 14 * math.sin(p * 2 * math.pi - k * 0.7), 0)))
    a.rot("idle", "jaw", loop(2.4, lambda p: (6 + 5 * math.sin(p * 4 * math.pi), 0, 0)))
    a.rot("idle", "head", loop(2.4, lambda p: (0, 8 * math.sin(p * 2 * math.pi - 10), 0)))
    a.clip("strike", length=0.8, loop=True)
    for k, nm in enumerate(segs):
        a.rot("strike", nm, {0.0: (0, 0, 0), 0.25: (-8, 16, 0), 0.42: (6, -10, 0), 0.8: (0, 0, 0)})
    a.pos("strike", "head", {0.0: (0, 0, 0), 0.25: (0, 6, 8), 0.42: (0, -2, -14), 0.8: (0, 0, 0)})
    a.rot("strike", "jaw", {0.0: (0, 0, 0), 0.25: (30, 0, 0), 0.42: (-6, 0, 0), 0.8: (0, 0, 0)})
    a.clip("rise", length=1.0, loop=False)
    a.pos("rise", "root", {0.0: (0, -16, 0), 0.7: (0, 1, 0), 1.0: (0, 0, 0)})
    a.scale("rise", "root", {0.0: (0.5, 0.3, 0.5), 0.7: (1.05, 1.05, 1.05), 1.0: (1, 1, 1)})
    return A.save()


def build_whip():
    A = new("whip", 64, 64, 62)
    A.bone("root", pivot=(0, 0, 0))
    A.cube("root", (-4, -2, -4), (8, 3, 8), "vine_dark")
    for k in range(5):
        a = k * 72
        A.bone("rt%d" % k, "root", (3, 0, 0), rotation=(0, a, 0))
        A.bone("rtc%d" % k, "rt%d" % k, (3, 0, 0), rotation=(0, 0, 70))
        A.cube("rtc%d" % k, (2.5, -4, -1), (2, 5, 2), "vine", share="rr")
    w = A.limb("w", "root", (0, 0, 0), (0, 0, 0), [7, 7, 6, 6, 5, 5, 4, 4], 5, 1, "vine", curl=(0, 0, 8), share="wp")
    for j in (1, 3, 5):
        A.spike(w[j], (2.5, A.tips[w[j]][1] - 3, 0), "+x", 3, 2, "thorn", layers=2, share="wt%d" % j)
        A.spike(w[j], (-2.5, A.tips[w[j]][1] - 1.5, 0), "-x", 3, 2, "thorn", layers=2, share="wu%d" % j)
    A.cube(w[7], (A.tips[w[7]][0] - 1.5, A.tips[w[7]][1], A.tips[w[7]][2] - 1.5), (3, 3, 3), "bud", share="wb")
    A.leaf(w[4], (A.tips[w[4]][0] + 3, A.tips[w[4]][1] - 2, 0), (3, 5), "leaf", rot=(0, 0, -30), share="wl")
    A.leaf(w[6], (A.tips[w[6]][0] - 3, A.tips[w[6]][1] - 2, 0), (3, 4), "leaf_dark", rot=(0, 0, 30), share="wl2")
    a = A.anim
    a.clip("idle", length=2.5, loop=True)
    for j, nm in enumerate(w):
        a.rot("idle", nm, loop(2.5, lambda p, j=j: ((5 + j) * math.sin(p * 2 * math.pi - j * 0.6), 0, 8 * (1 if j else 0) + (6 + j) * math.cos(p * 2 * math.pi - j * 0.6))))
    a.clip("lash", length=0.7, loop=True)
    for j, nm in enumerate(w):
        a.rot("lash", nm, {0.0: (-6 - j, 0, 6), 0.25: (-14 - 2 * j, 0, 4), 0.38: (30 + 4 * j, 0, 0), 0.7: (0, 0, 6)})
    return A.save()


def build_wall():
    A = new("wall", 128, 128, 63)
    A.bone("root", pivot=(0, 0, 0))
    c = A.cube
    c("root", (-18, -1, -3), (36, 3, 6), "vine_dark")
    cols = (-13, 0, 13)
    A.anim.clip("idle", length=4.0, loop=True)
    for k, x in enumerate(cols):
        t = A.limb("col%d" % k, "root", (x, 2, 0), (0, 0, 0), [10, 10, 9, 8], 6, 3, "vine", curl=(0, 6 * (1 if k % 2 else -1), 2), share="cl%d" % (k % 2))
        for j in (0, 1, 2):
            A.spike(t[j], (x + 3.5, 8 + j * 9, 0), "+x", 3, 2, "thorn", layers=2, share="ct%d" % j)
            A.spike(t[j], (x - 3.5, 12 + j * 9, 0), "-x", 3, 2, "thorn", layers=2, share="cu%d" % j)
        A.cube(t[3], (A.tips[t[3]][0] - 1.5, A.tips[t[3]][1], -1.5), (3, 3, 3), "bud", share="cbd")
        for j in range(4):
            A.anim.rot("idle", t[j], sway(4.0, 2.0 + j, 0.12 * j + 0.3 * k, axis=0 if j % 2 else 2, base=(0, 6 * (1 if k % 2 else -1), 2) if j else (0, 0, 0)))
    for k, (x0, rz) in enumerate(((-7, 40), (7, -40), (-7, -40), (7, 40))):          # diagonal cross vines between the columns
        c("root", (x0 - 1, 6 + (k // 2) * 12, 1.5), (2, 17, 2), "vine", rot=(0, 0, rz), share="cx%d" % (k % 2))
    for k, (x, y) in enumerate(((-6.5, 14), (6.5, 20), (-6.5, 28), (6.5, 8), (0, 33))):
        A.leaf("root", (x, y, 3), (5, 5), "leaf" if k % 2 == 0 else "leaf_dark", rot=(10, 15 * (k - 2), 20 * (k - 2)), share="wl%d" % (k % 2))
    return A.save()


if __name__ == "__main__":
    ids = [build_helm(), build_crown(), build_chest(), build_pauldron(-1), build_pauldron(1), build_bracer(-1), build_bracer(1),
           build_greave(-1), build_greave(1), build_back(), build_aux(), build_serpent(), build_whip(), build_wall()]
    build_skin()
    run_check(ids)
    print("done:", ", ".join(ids))

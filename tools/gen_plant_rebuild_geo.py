"""Plant Magic rebuild models: the Blossom armour of the temporary armour (Aura.PLANT) and the summons (the Flower Sprite familiar, the Healing Bloom).

    python3 -B tools/gen_plant_rebuild_geo.py       # writes every model, animation, texture and the skin overlay, runs geo_check

Armour (Space.PLAYER, anchored on the model part each piece was built for, see client/aura/PlantAura.java):
  plant_helm        a flower crown: a ring of leaves with five blossoms and two long leaf ribbons hanging to the shoulders (head)
  plant_crown       a halo of eight drifting petals turning above the head, with pollen between them (head)
  plant_chest       a leaf bodice in overlapping rows, a great blossom with a golden heart on the breastbone, a petal skirt that flares and sways, a petal bow on the back (body)
  plant_pauld_r/l   a half-open bloom over each shoulder: five petals round a golden heart, a leaf sprig behind (arms)
  plant_bracer_r/l  leaf sleeves with a petal cuff that flares at the wrist and a small blossom on the back of the hand (arms)
  plant_greave_r/l  leaf wraps with a petal flare at the ankle and a blossom at the knee (legs)
  plant_back        two pairs of petal wings (large over small) that beat slowly, with a pollen heart between them (body)
  plant_aux         six small blossoms drifting round the body, each with a pollen heart (body)
Summons (prop kind PLANT_1):
  plant_familiar    the Flower Sprite: a floating bulb body with a blossom head and a face, six petal hair, leaf arms and wings, a stem tail; clips idle, heal, strike
  plant_bloom       the Healing Bloom: a ground flower, a stem with two leaves and an eight-petal bloom round a glowing heart; clips idle, open
Texture style: soft pink and white petals with veins and a golden glow at the heart, green leaves.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_lib import *   # noqa: E402,F401,F403

KEY = "plant"
MATS = {
    "petal": dict(kind="petal", ramp=[(130, 40, 84), (196, 84, 136), (238, 134, 180), (252, 190, 216), (255, 236, 246)], glowc=(255, 226, 150), heart_glow=60),
    "petal_w": dict(kind="petal", ramp=[(150, 150, 176), (204, 204, 226), (238, 238, 250), (252, 252, 255), (255, 255, 255)], glowc=(255, 240, 190), heart_glow=50),
    "petal_y": dict(kind="petal", ramp=[(160, 110, 20), (214, 160, 44), (248, 206, 80), (255, 232, 140), (255, 248, 210)], glowc=(255, 230, 120), heart_glow=60),
    "leaf": dict(kind="leaf", ramp=[(12, 56, 20), (30, 104, 36), (62, 158, 54), (112, 206, 74), (186, 242, 126)], glowc=(190, 255, 130), vein_glow=40),
    "leaf_dark": dict(kind="leaf", ramp=[(8, 42, 16), (20, 78, 28), (40, 118, 40), (70, 158, 52), (116, 198, 80)], glowc=(160, 240, 110), vein_glow=20),
    "stem": dict(kind="plain", ramp=[(20, 60, 18), (40, 100, 30), (66, 142, 44), (104, 180, 62), (160, 220, 100)]),
    "heart": dict(kind="core", ramp=[(150, 90, 0), (224, 160, 16), (255, 212, 64), (255, 240, 150), (255, 252, 224)], glowc=(255, 224, 110)),
    "face": dict(kind="plain", ramp=[(60, 30, 40), (110, 60, 70), (170, 100, 100), (226, 150, 140), (252, 206, 190)]),
}


def new(name, w, h, seed):
    return Atlas(KEY, name, w, h, seed=seed, mats=MATS)


def blossom(A, bone, c, petals=5, r=2.6, size=(3, 4), mat="petal", heart=2, face="z", mats=None, share="bl", phase=0.0, tilt=0.0):
    """A flat blossom centred on c, facing along -z ('z') or +y ('y'): `petals` petals round a golden heart."""
    mats = mats or (mat,)
    for k in range(petals):
        th = phase + k * 360.0 / petals
        dx, dy = r * math.sin(math.radians(th)), r * math.cos(math.radians(th))
        if face == "z":
            A.leaf(bone, (c[0] + dx, c[1] + dy, c[2]), size, mats[k % len(mats)], rot=(0, 0, -th), share="%s%d%d" % (share, k % len(mats), size[0]))
        else:
            A.cube(bone, (c[0] + dx - size[0] / 2.0, c[1], c[2] + dy - size[1] / 2.0), (size[0], 1, size[1]), mats[k % len(mats)], rot=(tilt, th + 90, 0),
                   share="%s%d%d" % (share, k % len(mats), size[0]))
    if heart:
        if face == "z":
            A.cube(bone, (c[0] - heart / 2.0, c[1] - heart / 2.0, c[2] - 0.8), (heart, heart, 2), "heart", share="%sh%d" % (share, heart))
        else:
            A.cube(bone, (c[0] - heart / 2.0, c[1] - 0.5, c[2] - heart / 2.0), (heart, 2, heart), "heart", share="%sh%d" % (share, heart))


# --------------------------------------------------------------------------------------------------------------------
def build_helm():
    A = new("helm", 64, 64, 71)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("head", "root", (0, 24, 0))
    A.cube("head", (-5, 29, -5), (10, 1, 10), "stem", inflate=0.1)                  # the green band
    for k in range(9):                                                               # the ring of leaves
        a = k * 40.0
        x, z = 5.4 * math.cos(math.radians(a)), 5.4 * math.sin(math.radians(a))
        A.leaf("head", (x, 31, z), (3, 3), "leaf" if k % 2 == 0 else "leaf_dark", rot=(-25, 90 - a, 0), share="hl%d" % (k % 2))
    for k, (x, z, ph) in enumerate(((0, -5.8, 0), (-4.3, -3.6, 20), (4.3, -3.6, 40), (-5.8, 1.5, 10), (5.8, 1.5, 30))):
        a = math.degrees(math.atan2(z, x))
        A.bone("fl%d" % k, "head", (x, 30.5, z), rotation=(0, 90 - a - 90 if False else 0, 0))
        blossom(A, "fl%d" % k, (x, 31.2, z), 5, 1.9, (2, 3), "petal" if k % 2 == 0 else "petal_w", 1, "z", share="hb%d" % (k % 2), phase=ph)
    A.bone("rib", "head", (0, 30, 3))
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        r = A.limb("rb%s" % n, "head", (sg * 4.6, 29, 2), (0, 0, sg * 8), [4, 4, 3], 3, 2, "leaf", depth=1, curl=(-6, 0, -sg * 6), share="rb")
        A.cube(r[2], (A.tips[r[2]][0] - 1, A.tips[r[2]][1], A.tips[r[2]][2] - 0.5), (2, 2, 1), "petal", share="rbp")
    A.anim.clip("idle", length=4.0, loop=True)
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.anim.rot("idle", "rb%s_0" % n, sway(4.0, 5.0, 0.2 * sg, axis=0, base=(0, 0, sg * 8)))
    return A.save()


def build_crown():
    A = new("crown", 64, 64, 72)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("ring", "root", (0, 38, 0))
    for k in range(8):
        a = k * 45.0
        x, z = 6.4 * math.cos(math.radians(a)), 6.4 * math.sin(math.radians(a))
        A.bone("p%d" % k, "ring", (x, 38, z), rotation=lean(x, z, 38 if k % 2 else 18))
        A.cube("p%d" % k, (x - 1.5, 38, z - 0.5), (3, 4, 1), "petal" if k % 2 == 0 else "petal_w", rot=(0, -a + 90, 0), share="cp%d" % (k % 2), pivot=(x, 38, z))
        A.cube("ring", (5.2 * math.cos(math.radians(a + 22)) - 0.5, 38.3, 5.2 * math.sin(math.radians(a + 22)) - 0.5), (1, 1, 1), "heart", share="ch")
    A.anim.clip("idle", length=8.0, loop=True)
    A.anim.rot("idle", "ring", spin(8.0, axis=1, turns=1))
    for k in range(8):
        x, z = 6.4 * math.cos(math.radians(k * 45)), 6.4 * math.sin(math.radians(k * 45))
        A.anim.rot("idle", "p%d" % k, sway(2.0, 6.0, 0.125 * k, axis=0, base=lean(x, z, 38 if k % 2 else 18)) if False else sway(8.0, 6.0, 0.125 * k, axis=2, base=lean(x, z, 38 if k % 2 else 18)))
    return A.save()


def build_chest():
    A = new("chest", 128, 128, 73)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    for row, y in enumerate((20, 17.5, 15, 12.5)):                                    # the leaf bodice: rows of overlapping leaves
        n = 4 - row // 2
        for k in range(n + 1):
            x = (k - n / 2.0) * (8.0 / max(1, n)) if n else 0
            A.leaf("torso", (x, y, -3.5), (3 if row < 2 else 2, 3), "leaf" if (k + row) % 2 == 0 else "leaf_dark", rot=(-12, 0, (k - n / 2.0) * 10), share="bd%d%d" % (row // 2, (k + row) % 2))
    A.bone("bloom", "torso", (0, 18, -4.5))
    blossom(A, "bloom", (0, 18, -4.6), 6, 3.2, (4, 5), "petal", 3, "z", share="bm", mats=("petal", "petal_w"))
    c("torso", (-5, 12, -3), (10, 2, 6), "stem", inflate=0.2)                          # belt
    c("torso", (-4.5, 13, 2), (9, 10, 1), "leaf_dark", inflate=0.2)                    # the back
    A.bone("skirt", "torso", (0, 12, 0))
    for k in range(10):                                                                # the petal skirt: ten petals flaring out round the hips
        a = k * 36.0
        x, z = 4.6 * math.cos(math.radians(a)), 3.4 * math.sin(math.radians(a))
        A.bone("sk%d" % k, "skirt", (x, 12, z), rotation=lean(x, z * 1.3, 28))
        A.cube("sk%d" % k, (x - 1.5, 6, z - 0.5), (3, 6, 1), "petal" if k % 2 == 0 else "petal_w", rot=(0, -a + 90, 0), pivot=(x, 12, z), share="sp%d" % (k % 2))
    A.bone("bow", "torso", (0, 19, 3))
    for sg in (-1, 1):
        A.cube("bow", (sg * 3 - 2 if sg > 0 else sg * 3 - 2, 17, 3.5), (4, 5, 1), "petal", rot=(0, 0, sg * 30), share="bw")
    A.cube("bow", (-1, 18.5, 3.2), (2, 2, 2), "heart", share="bk")
    A.anim.clip("idle", length=3.0, loop=True)
    for k in range(10):
        a = k * 36.0
        x, z = 4.6 * math.cos(math.radians(a)), 3.4 * math.sin(math.radians(a))
        A.anim.rot("idle", "sk%d" % k, sway(3.0, 4.0, 0.1 * k, axis=0 if abs(z) > abs(x) else 2, base=lean(x, z * 1.3, 28)))
    A.anim.scale("idle", "bloom", swell(3.0, 0.05))
    return A.save()


def build_pauldron(sg):
    n = "r" if sg < 0 else "l"
    A = new("pauld_" + n, 64, 64, 74 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    A.side("arm", sg, (3.5, 23, -3), (6, 2, 6), "stem", inflate=0.2, share="pa")
    A.bone("bloom", "arm", (sg * 7, 26, 0))
    blossom(A, "bloom", (sg * 7, 27.5, 0), 5, 3.0, (3, 4), "petal", 2, "z", share="pb", mats=("petal", "petal_w"), phase=sg * 10)
    A.leaf("arm", (sg * 6.5, 26, 2.5), (4, 4), "leaf", rot=(20, 0, -sg * 25), share="plf")
    A.leaf("arm", (sg * 8.5, 24.5, -2.5), (3, 4), "leaf_dark", rot=(-20, 0, -sg * 50), share="plg")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.scale("idle", "bloom", swell(3.0, 0.07, 0.1 * sg))
    A.anim.rot("idle", "bloom", sway(3.0, 5.0, 0.2, axis=1))
    return A.save()


def build_bracer(sg):
    n = "r" if sg < 0 else "l"
    A = new("bracer_" + n, 64, 64, 76 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    s((3.5, 13, -2.5), (5, 5, 5), "leaf_dark", inflate=0.3, share="ba")             # the leaf sleeve
    s((3.5, 17.5, -2.5), (5, 1, 5), "stem", inflate=0.4, share="bb")
    for k in range(4):                                                                # the petal cuff
        a = k * 90.0 + 45
        x, z = sg * 6 + 3.3 * math.cos(math.radians(a)), 3.3 * math.sin(math.radians(a))
        A.bone("cf%d" % k, "arm", (x, 12.5, z), rotation=lean(x - sg * 6, z, 34))
        A.cube("cf%d" % k, (x - 1.5, 8.5, z - 0.5), (3, 4, 1), "petal" if k % 2 == 0 else "petal_w", rot=(0, -a + 90, 0), pivot=(x, 12.5, z), share="cf%d" % (k % 2))
    A.bone("hb", "arm", (sg * 6, 10, -2))
    blossom(A, "hb", (sg * 6, 9.5, -3.1), 5, 1.6, (2, 2), "petal_w", 1, "z", share="hb")
    A.anim.clip("idle", length=3.0, loop=True)
    for k in range(4):
        a = k * 90.0 + 45
        x, z = 3.3 * math.cos(math.radians(a)), 3.3 * math.sin(math.radians(a))
        A.anim.rot("idle", "cf%d" % k, sway(3.0, 5.0, 0.25 * k, axis=0 if abs(z) > abs(x) else 2, base=lean(x, z, 34)))
    return A.save()


def build_greave(sg):
    n = "r" if sg < 0 else "l"
    A = new("greave_" + n, 64, 64, 78 + (sg > 0))
    A.bone("leg", pivot=(sg * 1.9, 12, 0))
    s = lambda *a, **k: A.side("leg", sg, *a, **k)
    s((0.3, 3, -2.5), (4.4, 6, 5), "leaf_dark", inflate=0.3, share="ga")
    s((0.3, 8.5, -2.5), (4.4, 1, 5), "stem", inflate=0.4, share="gb")
    for k in range(4):
        a = k * 90.0 + 45
        x, z = sg * 2.1 + 3.2 * math.cos(math.radians(a)), 3.2 * math.sin(math.radians(a))
        A.bone("gf%d" % k, "leg", (x, 3, z), rotation=lean(x - sg * 2.1, z, 36))
        A.cube("gf%d" % k, (x - 1.5, 0, z - 0.5), (3, 3, 1), "petal" if k % 2 == 0 else "petal_w", rot=(0, -a + 90, 0), pivot=(x, 3, z), share="gf%d" % (k % 2))
    A.bone("kn", "leg", (sg * 2.1, 9, -3))
    blossom(A, "kn", (sg * 2.1, 9.5, -3.6), 5, 1.6, (2, 2), "petal", 1, "z", share="kn")
    return A.save()


def build_back():
    A = new("back", 128, 128, 80)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    A.cube("torso", (-2, 17, 2), (4, 6, 2), "stem", inflate=0.2)
    A.cube("torso", (-1.5, 19.5, 3.8), (3, 3, 1), "heart")
    A.anim.clip("idle", length=4.0, loop=True)
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        for tier, (hy, scale, rzbase, ry) in enumerate(((22, 1.0, 28, 40), (18, 0.65, -10, 55))):
            nm = "w%s%d" % (n, tier)
            A.bone(nm, "torso", (sg * 1.5, hy, 3.4), rotation=(0, sg * -ry, sg * rzbase))
            petals = 5
            for k in range(petals):
                th = -10 + k * 22.0
                ln = int((9 + 4 * math.sin(k / (petals - 1.0) * math.pi)) * scale + 2)
                x, y = 1.5 + sg * 0, 0
                tipx = (sg * 1.5) + sg * (4 + ln * 0.5) * math.cos(math.radians(th))
                tipy = hy + (4 + ln * 0.5) * math.sin(math.radians(th))
                A.cube(nm, (sg * 1.5 + sg * 3 - (0 if sg > 0 else int(ln)), hy - 1.5, 3), (int(ln), 3, 1), "petal" if (k + tier) % 2 == 0 else "petal_w",
                       rot=(0, 0, sg * th), pivot=(sg * 1.5, hy, 3.4), share="wp%d%d%d" % (tier, k, 0))
            A.cube(nm, (sg * 1.5 + sg * 2 - 1, hy - 1, 3), (2, 2, 2), "heart", share="wh%d" % tier)
        A.anim.rot("idle", "w%s0" % n, sway(4.0, 10.0, 0.0, axis=1, base=(0, sg * -40, sg * 28)))
        A.anim.rot("idle", "w%s1" % n, sway(4.0, 8.0, 0.2, axis=1, base=(0, sg * -55, sg * -10)))
    return A.save()


def build_aux():
    A = new("aux", 64, 64, 82)
    A.bone("root", pivot=(0, 24, 0))
    for k in range(6):
        y = 10 + k * 2.8
        A.bone("o%d" % k, "root", (0, y, 0), rotation=(0, k * 60, 0))
        A.bone("f%d" % k, "o%d" % k, (10 + (k % 2) * 2, y, 0))
        blossom(A, "f%d" % k, (10.5 + (k % 2) * 2, y, 0), 5, 1.5, (2, 2), "petal" if k % 2 == 0 else "petal_w", 1, "z", share="ab%d" % (k % 2), phase=k * 13.0)
    A.anim.clip("idle", length=6.0, loop=True)
    for k in range(6):
        d = 1 if k % 2 == 0 else -1
        A.anim.rot("idle", "o%d" % k, {round(6.0 * i / 4, 2): (0, k * 60 + 90 * i * d, 0) for i in range(5)})
    return A.save()


def build_skin():
    S = Skin(KEY, MATS, 5)
    S.put("torso", ("north", "south", "west", "east"), (0, 0, 8, 12), "leaf_dark")
    S.put("torso", ("north",), (2, 2, 4, 4), "petal")
    for arm in ("rarm", "larm"):
        S.put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "leaf_dark")
        S.put(arm, ("north", "south", "west", "east"), (0, 6, 4, 1), "stem")
    for leg in ("rleg", "lleg"):
        S.put(leg, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "leaf_dark")
    S.save()


# --------------------------------------------------------------------------------------------------------------------
def build_familiar():
    A = new("familiar", 128, 128, 91)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 6, 0))
    c = A.cube
    c("body", (-3, 3, -3), (6, 6, 6), "stem")                                          # the bulb body
    c("body", (-2, 1, -2), (4, 2, 4), "leaf_dark")
    c("body", (-2, 4, -3.2), (4, 3, 1), "leaf", share="bl")
    A.bone("head", "body", (0, 9, 0))
    c("head", (-4, 9, -4), (8, 7, 8), "petal_w")                                       # the blossom head
    c("head", (-3, 11, -4.6), (2, 2, 1), "face", share="ey")
    c("head", (1, 11, -4.6), (2, 2, 1), "face", share="ey")
    c("head", (-1, 9.6, -4.6), (2, 1, 1), "face", share="mo")
    c("head", (-1, 15.5, -1), (2, 1, 2), "heart")
    for k in range(6):
        th = k * 60.0
        A.bone("hp%d" % k, "head", (0, 14, 0), rotation=lean(math.sin(math.radians(th)), math.cos(math.radians(th)), 50))
        A.cube("hp%d" % k, (-1.5, 14, -0.5), (3, 6, 1), "petal" if k % 2 == 0 else "petal_w", rot=(0, th, 0), pivot=(0, 14, 0), share="hpp%d" % (k % 2))
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("arm_%s" % n, "body", (sg * 3, 7, 0), rotation=(0, 0, -sg * 30))
        A.cube("arm_%s" % n, (sg * 3 - 0.5, 1, -1), (1, 6, 2), "stem", share="ar")
        A.leaf("arm_%s" % n, (sg * 3, 0.5, 0), (3, 3), "leaf", share="alf")
        A.bone("wing_%s" % n, "body", (sg * 2, 6, 3), rotation=(0, sg * -35, sg * 20))
        for k in range(3):
            A.cube("wing_%s" % n, (sg * 2 + (sg > 0) * 1 - (0 if sg > 0 else 6 + k * 2), 5 + k * 1.2, 3), (6 + k * 2, 3, 1), "petal" if k % 2 == 0 else "petal_w", rot=(0, 0, sg * (k * 22 - 10)), pivot=(sg * 2, 6, 3.4), share="wg%d%d" % (k, 0))
    A.bone("tail", "body", (0, 3, 2))
    t = A.limb("tl", "tail", (0, 3, 2), (-50, 0, 0), [3, 3, 2], 2, 1, "stem", curl=(-12, 0, 10), share="tl")
    A.leaf(t[2], (A.tips[t[2]][0], A.tips[t[2]][1], A.tips[t[2]][2]), (3, 3), "leaf", share="tf")
    a = A.anim
    a.clip("idle", length=2.0, loop=True)
    a.pos("idle", "root", loop(2.0, lambda p: (0, 1.4 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "head", loop(2.0, lambda p: (3 * math.sin(p * 2 * math.pi), 6 * math.sin(p * 2 * math.pi + 1), 0)))
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        a.rot("idle", "wing_%s" % n, loop(2.0, lambda p, sg=sg: (0, sg * (-35 + 18 * math.sin(p * 8 * math.pi)), sg * 20)))
        a.rot("idle", "arm_%s" % n, loop(2.0, lambda p, sg=sg: (12 * math.sin(p * 2 * math.pi + sg), 0, -sg * 30)))
    a.rot("idle", t[0], loop(2.0, lambda p: (-50 + 8 * math.sin(p * 2 * math.pi), 0, 0)))
    a.clip("heal", length=1.0, loop=True)
    a.scale("heal", "head", {0.0: (1, 1, 1), 0.3: (1.25, 1.25, 1.25), 0.6: (1.1, 1.1, 1.1), 1.0: (1, 1, 1)})
    for k in range(6):
        a.rot("heal", "hp%d" % k, {0.0: lean(math.sin(math.radians(k * 60)), math.cos(math.radians(k * 60)), 50), 0.3: lean(math.sin(math.radians(k * 60)), math.cos(math.radians(k * 60)), 85), 1.0: lean(math.sin(math.radians(k * 60)), math.cos(math.radians(k * 60)), 50)})
    a.pos("heal", "root", {0.0: (0, 0, 0), 0.3: (0, 3, 0), 1.0: (0, 0, 0)})
    a.clip("strike", length=0.6, loop=True)
    a.rot("strike", "body", {0.0: (0, 0, 0), 0.2: (-25, 0, 0), 0.35: (35, 0, 0), 0.6: (0, 0, 0)})
    a.pos("strike", "root", {0.0: (0, 0, 0), 0.2: (0, 2, 5), 0.35: (0, -1, -14), 0.6: (0, 0, 0)})
    a.rot("strike", "head", {0.0: (0, 0, 0), 0.35: (0, 360, 0), 0.6: (0, 360, 0)})
    return A.save()


def build_bloom():
    A = new("bloom", 128, 128, 92)
    A.bone("root", pivot=(0, 0, 0))
    A.cube("root", (-3, 0, -3), (6, 2, 6), "stem", inflate=0.2)
    st = A.limb("st", "root", (0, 2, 0), (0, 0, 0), [8, 8, 8], 3, 2, "stem", curl=(0, 0, 3), share="st")
    A.bone("head", st[2], (0, 26, 0))
    blossom(A, "head", (0, 26, 0), 8, 6.5, (6, 8), "petal", 5, "y", share="bp", mats=("petal", "petal_w"), tilt=-28)
    for k in range(8):
        pass
    A.cube("head", (-3, 25, -3), (6, 3, 6), "heart", share="bh")
    A.cube("head", (-2, 28, -2), (4, 2, 4), "petal_y", share="bhh")
    for sg in (-1, 1):
        A.bone("lf%d" % (sg > 0), st[0], (0, 6, 0), rotation=(0, 0, -sg * 50))
        A.cube("lf%d" % (sg > 0), (sg * 1 + (0 if sg > 0 else -9), 6, -2), (9, 1, 4), "leaf", share="lf")
        A.bone("lg%d" % (sg > 0), st[1], (0, 14, 0), rotation=(0, 0, -sg * 60))
        A.cube("lg%d" % (sg > 0), (sg * 1 + (0 if sg > 0 else -8), 14, -2), (8, 1, 3), "leaf_dark", share="lg")
    A.anim.clip("idle", length=4.0, loop=True)
    A.anim.rot("idle", st[0], sway(4.0, 2.0, 0.0, axis=2))
    A.anim.rot("idle", st[2], sway(4.0, 4.0, 0.2, axis=0))
    A.anim.scale("idle", "head", swell(4.0, 0.06))
    A.anim.clip("open", length=1.0, loop=False)
    A.anim.scale("open", "root", {0.0: (0.3, 0.1, 0.3), 0.7: (1.06, 1.08, 1.06), 1.0: (1, 1, 1)})
    A.anim.scale("open", "head", {0.0: (0.4, 0.4, 0.4), 0.6: (1.2, 1.2, 1.2), 1.0: (1, 1, 1)})
    return A.save()


if __name__ == "__main__":
    ids = [build_helm(), build_crown(), build_chest(), build_pauldron(-1), build_pauldron(1), build_bracer(-1), build_bracer(1),
           build_greave(-1), build_greave(1), build_back(), build_aux(), build_familiar(), build_bloom()]
    build_skin()
    run_check(ids)
    print("done:", ", ".join(ids))

"""Poison Plant Magic rebuild models: the Venomflora armour of the temporary armour (Aura.POISON_PLANT) and the summons (the Miasma Bloom, the Trap Maw).

    python3 -B tools/gen_poison_plant_rebuild_geo.py   # writes every model, animation, texture and the skin overlay, runs geo_check

Armour (Space.PLAYER, anchored on the model part each piece was built for, see client/aura/PoisonPlantAura.java):
  poison_plant_helm        a flytrap hood: two toothed lobes arching over the head that open and snap slowly, a glowing bulb between them, a dripping antenna (head)
  poison_plant_crown       six drifting spore pods circling above the head, glowing acid green (head)
  poison_plant_chest       three swollen gourd pods down the chest with glowing veins and thorns, a belt of small pods, a leaf back plate (body)
  poison_plant_pauld_r/l   a spiked pod over each shoulder with a small flytrap mouth on top and three hooked thorns (arms)
  poison_plant_bracer_r/l  toxic leaf vambraces ending in three hooked venom claws that drip (arms)
  poison_plant_greave_r/l  bulb greaves with a thorn knee and hooked toe thorns (legs)
  poison_plant_back        a tall pitcher plant behind the back with an animated lid and a glowing venom pool, plus a stalk with a snapping flytrap head over the shoulder (body)
  poison_plant_aux         four spore pods drifting low round the body (body)
Summons (prop kind POISON_PLANT_1):
  poison_plant_bloom       the Miasma Bloom: a bulb on a stalk, six petals that close and open, thorns, a glowing core; clips idle, open
  poison_plant_maw         the Trap Maw: a carnivorous flytrap on a stalk with two toothed lobes; clips idle, snap
Texture style: mottled violet and olive flesh with ivory thorns, acid-green glowing veins and spots.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from tree_rebuild_lib import *   # noqa: E402,F401,F403

KEY = "poison_plant"
MATS = {
    "violet": dict(kind="toxic", ramp=[(20, 6, 36), (54, 22, 82), (100, 50, 140), (148, 92, 188), (206, 150, 232)], glowc=(190, 255, 60), vein_glow=210),
    "olive": dict(kind="toxic", ramp=[(18, 34, 6), (42, 74, 14), (84, 120, 24), (130, 168, 40), (188, 214, 86)], glowc=(200, 255, 70), vein_glow=170),
    "leaf": dict(kind="leaf", ramp=[(16, 40, 10), (38, 76, 20), (74, 116, 32), (118, 158, 48), (176, 204, 88)], glowc=(190, 255, 70), vein_glow=120),
    "flesh": dict(kind="toxic", ramp=[(50, 6, 30), (104, 16, 54), (156, 36, 78), (204, 74, 108), (240, 138, 156)], glowc=(255, 120, 150), vein_glow=90),
    "thorn": dict(kind="thorn", ramp=[(40, 36, 30), (96, 88, 70), (156, 146, 114), (214, 204, 168), (250, 244, 214)]),
    "venom": dict(kind="core", ramp=[(20, 80, 0), (60, 150, 6), (140, 230, 20), (210, 255, 100), (244, 255, 210)], glowc=(190, 255, 40)),
    "pool": dict(kind="core", ramp=[(10, 60, 10), (30, 120, 24), (80, 200, 40), (160, 250, 90), (230, 255, 190)], glowc=(120, 255, 60)),
}


def new(name, w, h, seed):
    return Atlas(KEY, name, w, h, seed=seed, mats=MATS)


def teeth(A, bone, x0, x1, y, z, n, mat="thorn", share="te", axis="-y", ln=2, w=1):
    for k in range(n):
        x = x0 + (x1 - x0) * (k + 0.5) / n
        A.spike(bone, (x, y, z), axis, ln, w, mat, layers=2, share=share)


def lobe(A, bone, sg, y, z0, length, width, th, mat="violet"):
    """One flytrap lobe: a curved shell of three cubes and a row of teeth along its free edge (a lobe lies on the side sg of the bone)."""
    x = sg * 4.8
    A.cube(bone, (x - th / 2.0, y, z0), (th, width, length), mat, inflate=0.1)
    A.cube(bone, (x - sg * 2 - th / 2.0 + (0 if sg > 0 else 0), y + width - 1, z0), (th, 2, length), "flesh", rot=(0, 0, sg * 28), share="lb")
    for k in range(5):
        A.spike(bone, (x - sg * 0.5, y, z0 + 1 + k * (length - 2) / 4.0), "-y", 2, 1, "thorn", layers=2, share="lt")


# --------------------------------------------------------------------------------------------------------------------
def build_helm():
    A = new("helm", 64, 64, 101)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("head", "root", (0, 24, 0))
    A.cube("head", (-5, 29, -5), (10, 2, 10), "olive", inflate=0.1)                  # the rim the lobes grow from
    A.cube("head", (-1.5, 31, -4.5), (3, 3, 3), "venom", share="hv")                  # the glowing bulb between the lobes
    A.anim.clip("idle", length=3.0, loop=True)
    for sg in (-1, 1):
        n = "r" if sg < 0 else "l"
        A.bone("lobe%s" % n, "head", (sg * 4.5, 31, 0), rotation=(0, 0, -sg * 22))
        A.cube("lobe%s" % n, (sg * 4.5 - 1 + (0 if sg > 0 else -0), 31, -4.5), (2, 6, 9), "violet", inflate=0.1, share="lo")
        A.cube("lobe%s" % n, (sg * 4.5 - 1, 36.5, -4.5), (2, 2, 9), "flesh", share="lr", rot=(0, 0, sg * 24), pivot=(sg * 4.5, 36.5, 0))
        for k in range(5):
            A.spike("lobe%s" % n, (sg * 4.5 - sg * 0.2, 31.2, -4 + k * 2.0), "+y", 4, 1, "thorn", layers=2, share="lt")
        A.anim.rot("idle", "lobe%s" % n, loop(3.0, lambda p, sg=sg: (0, 0, -sg * (22 + 14 * max(0.0, math.sin(p * 2 * math.pi))))))
    t = A.limb("ant", "head", (0, 31, 4), (-24, 0, 0), [5, 4, 3], 2, 1, "olive", curl=(10, 0, 0), share="an")
    A.cube(t[2], (A.tips[t[2]][0] - 1.5, A.tips[t[2]][1], A.tips[t[2]][2] - 1.5), (3, 3, 3), "pool", share="ap")
    A.anim.rot("idle", t[0], sway(3.0, 5.0, 0.2, axis=0, base=(-24, 0, 0)))
    return A.save()


def build_crown():
    A = new("crown", 64, 64, 102)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("ring", "root", (0, 37, 0))
    for k in range(6):
        a = k * 60.0
        x, z = 6.2 * math.cos(math.radians(a)), 6.2 * math.sin(math.radians(a))
        A.bone("p%d" % k, "ring", (x, 37 + (k % 2), z))
        A.cube("p%d" % k, (x - 1.5, 36 + (k % 2), z - 1.5), (3, 3, 3), "venom", share="cv")
        A.cube("p%d" % k, (x - 2, 36.5 + (k % 2), z - 2), (4, 2, 4), "violet", inflate=0.1, rot=(0, a, 0), share="cc")
        A.spike("p%d" % k, (x, 39 + (k % 2), z), "+y", 3, 1, "thorn", layers=2, share="cth")
    A.anim.clip("idle", length=8.0, loop=True)
    A.anim.rot("idle", "ring", spin(8.0, axis=1, turns=1))
    for k in range(6):
        A.anim.pos("idle", "p%d" % k, loop(8.0, lambda p, k=k: (0, 1.2 * math.sin((p + k / 6.0) * 4 * math.pi), 0)))
    return A.save()


def build_chest():
    A = new("chest", 128, 128, 103)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-4, 19, -4.4), (8, 4, 2), "violet", inflate=0.2)                      # three swollen gourd pods
    c("torso", (-3.5, 15, -4.8), (7, 4, 2), "olive", inflate=0.2)
    c("torso", (-3, 12.5, -4.2), (6, 3, 1), "violet", inflate=0.2)
    c("torso", (-1.5, 20, -6.2), (3, 2, 1), "venom", share="vv")
    c("torso", (-1.5, 16, -6.4), (3, 2, 1), "venom", share="vv")
    for k, (x, y) in enumerate(((-4, 23), (4, 23), (-4.5, 17), (4.5, 17))):
        A.spike("torso", (x, y, -3.5), "-z", 3, 2, "thorn", layers=2, share="st")
    c("torso", (-5, 12, -3), (10, 2, 6), "olive", inflate=0.2)                          # belt with pods
    c("torso", (-4.5, 10.5, -3.8), (2, 2, 2), "venom", share="bp")
    c("torso", (2.5, 10.5, -3.8), (2, 2, 2), "venom", share="bp")
    c("torso", (-4.5, 13, 2), (9, 10, 2), "leaf", inflate=0.2)                          # the back plate
    c("torso", (-1, 14, 3.7), (2, 9, 1), "venom")
    A.bone("drip", "torso", (0, 13, -4))
    for k, x in enumerate((-3, 0, 3)):
        c("drip", (x - 0.5, 8.5, -4.3), (1, 3 + (k % 2), 1), "pool", share="dr%d" % (k % 2))
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.scale("idle", "drip", loop(3.0, lambda p: (1, 1 + 0.35 * math.sin(p * 2 * math.pi), 1)))
    return A.save()


def build_pauldron(sg):
    n = "r" if sg < 0 else "l"
    A = new("pauld_" + n, 64, 64, 104 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    A.side("arm", sg, (3.5, 23, -3), (6, 4, 6), "violet", inflate=0.3, share="pa")
    A.side("arm", sg, (4.5, 27, -2), (4, 2, 4), "olive", share="pb")
    A.side("arm", sg, (5.5, 27.4, -1), (2, 2, 2), "venom", share="pc")
    A.bone("jaw", "arm", (sg * 6.5, 27, 0))
    for k, z in enumerate((-2.2, 0, 2.2)):                                             # three hooked thorns
        t = A.limb("th%s%d" % (n, k), "arm", (sg * (7.5 + k * 0.4), 25.5, z), (0, 0, -sg * (30 + 12 * k)), [3, 3], 2, 1, "thorn", curl=(0, 0, sg * 38), share="ph%d" % k)
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.scale("idle", "jaw", swell(3.0, 0.08, 0.15 * sg))
    return A.save()


def build_bracer(sg):
    n = "r" if sg < 0 else "l"
    A = new("bracer_" + n, 64, 64, 106 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    s = lambda *a, **k: A.side("arm", sg, *a, **k)
    s((3.5, 12, -2.5), (5, 6, 5), "leaf", inflate=0.3, share="ba")
    s((3.5, 17.5, -2.5), (5, 1, 5), "olive", inflate=0.4, share="bb")
    s((8.2, 13, -1.5), (1, 4, 3), "venom", share="bc")
    s((3.8, 9, -2.8), (4, 3, 5), "violet", inflate=0.2, share="bg")
    A.anim.clip("idle", length=3.0, loop=True)
    for k, z in enumerate((-1.8, 0, 1.8)):                                              # three hooked venom claws
        t = A.limb("cl%s%d" % (n, k), "arm", (sg * 6, 9.5, z), (180, 0, 0), [3, 3, 2], 2, 1, "thorn", curl=(-24, 0, 0), share="cl")
        A.cube(t[2], (A.tips[t[2]][0] - 0.5, A.tips[t[2]][1], A.tips[t[2]][2] - 0.5), (1, 1, 1), "venom", share="cd")
        A.anim.rot("idle", t[0], sway(3.0, 5.0, 0.2 * k, axis=0, base=(180, 0, 0)))
    return A.save()


def build_greave(sg):
    n = "r" if sg < 0 else "l"
    A = new("greave_" + n, 64, 64, 108 + (sg > 0))
    A.bone("leg", pivot=(sg * 1.9, 12, 0))
    s = lambda *a, **k: A.side("leg", sg, *a, **k)
    s((0.3, 1.5, -2.5), (4.4, 7, 5), "olive", inflate=0.3, share="ga")
    s((0.3, 0.5, -2.5), (4.4, 1, 5), "violet", inflate=0.35, share="gb")
    s((0.5, 8.5, -3.4), (4, 3, 2), "violet", inflate=0.2, share="gc")
    s((1.3, 9.2, -4.6), (2, 2, 1), "venom", share="gd")
    A.spike("leg", (sg * 2.5, 10, -3.6), "-z", 3, 2, "thorn", layers=2, share="gt")
    for k, x in enumerate((0.8, 3.4)):
        t = A.limb("gh%s%d" % (n, k), "leg", (sg * x, 1.5, -3.2), (110, 0, 0), [2, 2], 2, 1, "thorn", curl=(-30, 0, 0), share="gh")
    return A.save()


def build_back():
    A = new("back", 128, 128, 110)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-3, 15, 2), (6, 5, 2), "olive", inflate=0.2)
    # the pitcher: a flared tube behind the back, a venom pool glowing inside, a lid that opens and shuts
    A.bone("pitcher", "torso", (0, 14, 6))
    c("pitcher", (-3, 14, 4), (6, 4, 5), "violet")
    c("pitcher", (-4, 18, 3.5), (8, 8, 6), "olive", inflate=0.2)
    c("pitcher", (-3.5, 26, 4), (7, 6, 5), "violet", inflate=0.2)
    c("pitcher", (-4.5, 31, 3.5), (9, 2, 6), "flesh", inflate=0.2)                      # the rim
    c("pitcher", (-3, 28.5, 5), (6, 3, 3), "pool", share="pp")
    c("pitcher", (-2, 19.5, 9.4), (4, 5, 1), "venom", share="pv")
    A.bone("lid", "pitcher", (0, 33, 3.5))
    c("lid", (-4, 33, 2.5), (8, 1, 7), "violet", rot=(-40, 0, 0), pivot=(0, 33, 3.5), share="ld")
    for k, x in enumerate((-4.2, -1.4, 1.4, 4.2)):
        A.spike("pitcher", (x, 31.5, 3.2), "-y", 2, 1, "thorn", layers=2, share="pt")
    # the stalk with a flytrap head, arching over the left shoulder
    s = A.limb("sk", "torso", (4, 21, 2.5), (-20, 0, -26), [6, 6, 5], 2, 1, "olive", curl=(34, 0, 0), share="sk")
    A.bone("hd", s[2], (A.tips[s[2]][0], A.tips[s[2]][1], A.tips[s[2]][2]), rotation=(40, 0, 0))
    tp = A.tips[s[2]]
    A.cube("hd", (tp[0] - 3, tp[1], tp[2] - 2), (6, 2, 4), "violet", share="hd1")
    A.cube("hd", (tp[0] - 3, tp[1] + 4, tp[2] - 2), (6, 2, 4), "flesh", share="hd2")
    for k in range(4):
        A.spike("hd", (tp[0] - 2.2 + k * 1.5, tp[1] + 2, tp[2] - 1.5), "+y", 2, 1, "thorn", layers=2, share="hdt")
    A.anim.clip("idle", length=3.0, loop=True)
    A.anim.rot("idle", "lid", loop(3.0, lambda p: (-14 * math.sin(p * 2 * math.pi) * 0 + (0 if p < 0.5 else 0) + 12 * math.sin(p * 2 * math.pi), 0, 0)))
    A.anim.rot("idle", s[0], sway(3.0, 4.0, 0.0, axis=2, base=(-20, 0, -26)))
    A.anim.rot("idle", "hd", loop(3.0, lambda p: (40 + 18 * max(0.0, math.sin(p * 4 * math.pi)), 0, 0)))
    A.anim.scale("idle", "pitcher", swell(3.0, 0.02))
    return A.save()


def build_aux():
    A = new("aux", 64, 64, 112)
    A.bone("root", pivot=(0, 24, 0))
    for k in range(4):
        y = 8 + k * 3.0
        A.bone("o%d" % k, "root", (0, y, 0), rotation=(0, k * 90, 0))
        A.bone("s%d" % k, "o%d" % k, (10, y, 0))
        A.cube("s%d" % k, (9, y - 1.5, -1.5), (3, 3, 3), "venom", share="av")
        A.cube("s%d" % k, (8.2, y - 2, -2.2), (4, 2, 4), "violet", rot=(0, 20 * k, 0), share="ac")
        A.spike("s%d" % k, (10.5, y - 2, 0), "-y", 3, 1, "pool", layers=2, share="ad")
    A.anim.clip("idle", length=6.0, loop=True)
    for k in range(4):
        A.anim.rot("idle", "o%d" % k, {round(6.0 * i / 4, 2): (0, k * 90 + 90 * i * (1 if k % 2 == 0 else -1), 0) for i in range(5)})
    return A.save()


def build_skin():
    S = Skin(KEY, MATS, 6)
    S.put("torso", ("north", "south", "west", "east"), (0, 0, 8, 12), "violet")
    S.put("torso", ("north",), (2, 3, 4, 4), "olive")
    for arm in ("rarm", "larm"):
        S.put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "olive")
        S.put(arm, ("north", "south", "west", "east"), (0, 6, 4, 2), "violet")
    for leg in ("rleg", "lleg"):
        S.put(leg, ("north", "south", "west", "east", "top"), (0, 0, 4, 12), "violet")
        S.put(leg, ("north", "south", "west", "east"), (0, 7, 4, 3), "olive")
    S.save()


# --------------------------------------------------------------------------------------------------------------------
def build_bloom():
    A = new("bloom", 128, 128, 121)
    A.bone("root", pivot=(0, 0, 0))
    A.cube("root", (-4, 0, -4), (8, 3, 8), "olive")
    st = A.limb("st", "root", (0, 3, 0), (0, 0, 0), [6, 6], 4, 3, "olive", curl=(0, 0, 4), share="st")
    A.bone("bulb", st[1], (0, 15, 0))
    c = lambda *a, **k: A.cube("bulb", *a, **k)
    c((-5, 13, -5), (10, 8, 10), "violet")
    c((-4, 21, -4), (8, 3, 8), "olive")
    c((-2, 18, -5.4), (4, 3, 1), "venom", share="bv")
    c((-5.4, 16, -2), (1, 3, 4), "venom", share="bw")
    c((4.4, 16, -2), (1, 3, 4), "venom", share="bw")
    for k in range(6):                                                                  # the six petals: they close over the core and open to spit
        th = k * 60.0
        x, z = 4.2 * math.sin(math.radians(th)), 4.2 * math.cos(math.radians(th))
        A.bone("pt%d" % k, "bulb", (x, 22, z), rotation=lean(x, z, 22))
        A.cube("pt%d" % k, (x - 2, 22, z - 0.5), (4, 8, 1), "flesh" if k % 2 else "violet", rot=(0, th + 90, 0), pivot=(x, 22, z), share="pp%d" % (k % 2))
        A.spike("pt%d" % k, (x, 29.5, z), "+y", 3, 1, "thorn", layers=2, share="ptt")
    A.cube("bulb", (-2, 21.5, -2), (4, 5, 4), "pool", share="bp")
    for sg in (-1, 1):
        A.bone("lf%d" % (sg > 0), st[0], (0, 4, 0), rotation=(0, 0, -sg * 55))
        A.cube("lf%d" % (sg > 0), (sg * 1 + (0 if sg > 0 else -9), 4, -2), (9, 1, 4), "leaf", share="lf")
        A.spike("lf%d" % (sg > 0), (sg * 6, 5.5, 0), "+y", 2, 1, "thorn", layers=2, share="lth")
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    a.rot("idle", st[0], sway(3.0, 2.5, 0.0, axis=2))
    a.scale("idle", "bulb", swell(3.0, 0.05))
    for k in range(6):
        th = k * 60.0
        x, z = 4.2 * math.sin(math.radians(th)), 4.2 * math.cos(math.radians(th))
        a.rot("idle", "pt%d" % k, sway(3.0, 3.0, 0.05 * k, axis=2, base=lean(x, z, 22)))
    a.clip("open", length=1.0, loop=False)
    for k in range(6):
        th = k * 60.0
        x, z = 4.2 * math.sin(math.radians(th)), 4.2 * math.cos(math.radians(th))
        a.rot("open", "pt%d" % k, {0.0: lean(x, z, 22), 0.4: lean(x, z, 80), 1.0: lean(x, z, 55)})
    a.scale("open", "bulb", {0.0: (1, 1, 1), 0.3: (1.3, 0.8, 1.3), 0.5: (0.9, 1.3, 0.9), 1.0: (1, 1, 1)})
    return A.save()


def build_maw():
    A = new("maw", 128, 128, 122)
    A.bone("root", pivot=(0, 0, 0))
    A.cube("root", (-4, 0, -4), (8, 3, 8), "olive")
    st = A.limb("st", "root", (0, 3, 0), (0, 0, 0), [6, 6, 6], 4, 3, "olive", curl=(-10, 0, 0), share="st")
    A.bone("head", st[2], (0, 21, 0), rotation=(24, 0, 0))
    A.cube("head", (-4, 20, -3), (8, 3, 6), "violet")
    A.bone("jawu", "head", (0, 23, 3))
    A.bone("jawl", "head", (0, 20, 3))
    # the two lobes: the upper hinged on top, the lower below, a row of teeth on each free edge, the glowing throat between them
    A.cube("jawu", (-6, 23, -9), (12, 2, 12), "flesh", rot=(-8, 0, 0), pivot=(0, 23, 3), share="ju")
    A.cube("jawu", (-5, 25, -8), (10, 1, 10), "violet", rot=(-8, 0, 0), pivot=(0, 23, 3), share="ju2")
    A.cube("jawl", (-6, 17, -9), (12, 2, 12), "flesh", rot=(8, 0, 0), pivot=(0, 20, 3), share="jl")
    A.cube("jawl", (-5, 15.5, -8), (10, 1, 10), "violet", rot=(8, 0, 0), pivot=(0, 20, 3), share="jl2")
    for k in range(6):
        A.spike("jawu", (-5 + k * 2, 23, -8.5), "-y", 3, 1, "thorn", layers=2, share="tu")
        A.spike("jawl", (-5 + k * 2, 19, -8.5), "+y", 3, 1, "thorn", layers=2, share="tl")
    A.cube("head", (-3, 19.5, -7), (6, 3, 4), "pool", share="th")
    for sg in (-1, 1):
        A.bone("lf%d" % (sg > 0), st[0], (0, 5, 0), rotation=(0, 0, -sg * 60))
        A.cube("lf%d" % (sg > 0), (sg * 1 + (0 if sg > 0 else -9), 5, -2), (9, 1, 4), "leaf", share="lf")
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    a.rot("idle", st[0], sway(3.0, 3.0, 0.0, axis=2))
    a.rot("idle", "jawu", loop(3.0, lambda p: (-6 - 8 * math.sin(p * 2 * math.pi), 0, 0)))
    a.rot("idle", "jawl", loop(3.0, lambda p: (6 + 8 * math.sin(p * 2 * math.pi), 0, 0)))
    a.clip("snap", length=0.8, loop=True)
    a.rot("snap", "jawu", {0.0: (-6, 0, 0), 0.3: (-46, 0, 0), 0.42: (6, 0, 0), 0.8: (-6, 0, 0)})
    a.rot("snap", "jawl", {0.0: (6, 0, 0), 0.3: (46, 0, 0), 0.42: (-6, 0, 0), 0.8: (6, 0, 0)})
    a.rot("snap", st[1], {0.0: (0, 0, 0), 0.3: (-20, 0, 0), 0.42: (26, 0, 0), 0.8: (0, 0, 0)})
    return A.save()


if __name__ == "__main__":
    ids = [build_helm(), build_crown(), build_chest(), build_pauldron(-1), build_pauldron(1), build_bracer(-1), build_bracer(1),
           build_greave(-1), build_greave(1), build_back(), build_aux(), build_bloom(), build_maw()]
    build_skin()
    run_check(ids)
    print("done:", ", ".join(ids))

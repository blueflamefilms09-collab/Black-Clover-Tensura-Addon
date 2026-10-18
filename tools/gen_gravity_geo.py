"""Gravity Magic geo models (1.93.0 rebuild): the Demon King's regalia (the temporary armour) and the two things the spells put in the world.

    python3 -B tools/gen_gravity_geo.py           # writes every model, animation and texture, runs geo_check on all of them

Armour (Space.PLAYER, drawn by client/aura/GravityAura.java through SpatialArmourKit and EquipAnimation):
  helm   (head)   a demon king's crown: a black iron band with five tall spikes and two swept horns, heavy cheek guards, a nape plate
  chest  (body)   a slab cuirass built three layers deep, a singularity in a ring of slate on the breast, a belt of hanging weight plates
  arm_r / arm_l   enormous stacked pauldrons (three descending plates and a spike), a thick vambrace with weight studs, a clenched gauntlet with knuckle weights
  leg_r / leg_l   a thigh plate, a knee spike, a heavy greave, a wide sabaton
  back   (body)   a tilted accretion ring (a thick dark ring with a hot inner ring) turning behind the back, three dark orbs on their own orbits, hanging mantle plates; clip idle
  halo   (head)   a tilted halo of two thin rings and three small weights circling it; clip idle
Things the spells make:
  orb    the singularity (Gravity Sphere): a black core of stacked cubes, three accretion rings of different tilt and speed, orbiting lensing shards; clips idle, fire (a stretch along the flight)
  sword  Evil God's Pressure Craft: a great sword of compressed rock: a layered grey blade with violet fracture glow, a rough crossguard and grip; clips idle (hover), launch
The look: black-violet iron, heavy and stacked, glowing from fractures (the glow map is the cracks and the rims) and from the singularity.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_geo_kit import Atlas, MATS, mat, loop, spin, mirror_x, mrot, check   # noqa: E402

KEY = "gravity"

mat("iron", "metal", [(6, 4, 12), (24, 18, 38), (50, 40, 72), (90, 74, 126), (158, 140, 198)], glow=(180, 90, 255), edge=80)
mat("slate", "facet", [(10, 8, 20), (32, 24, 54), (64, 48, 100), (108, 84, 158), (190, 170, 230)], glow=(196, 104, 255), edge=70, cell=14)
mat("weight", "plate", [(4, 3, 8), (16, 12, 28), (34, 26, 52), (62, 50, 92), (120, 104, 160)], glow=(150, 70, 230), edge=50)
mat("singular", "void", [(0, 0, 0), (4, 2, 10), (10, 4, 22), (24, 10, 52), (70, 30, 140)], glow=(210, 120, 255), edge=255, core=30)
mat("blackhole", "void", [(0, 0, 0), (2, 1, 5), (5, 2, 12), (12, 5, 28), (30, 14, 60)], glow=(120, 60, 200), edge=0, core=0)
mat("crack", "energy", [(80, 20, 120), (150, 50, 210), (210, 110, 255), (240, 190, 255), (255, 240, 255)], glow=(220, 130, 255), core=255)
mat("rim", "energy", [(120, 40, 190), (190, 90, 255), (230, 160, 255), (250, 220, 255), (255, 255, 255)], glow=(235, 170, 255), core=255)
mat("rock", "facet", [(30, 30, 38), (66, 66, 80), (108, 108, 124), (150, 150, 168), (210, 210, 224)], glow=(190, 110, 255), edge=40, cell=12, rune=0)
mat("rockhot", "facet", [(40, 20, 60), (90, 50, 130), (150, 100, 200), (200, 160, 240), (250, 230, 255)], glow=(220, 140, 255), edge=140, core=70, cell=12)


def helm():
    A = Atlas(KEY, "helm", 128, 128, seed=31)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 30, -4), (8, 2, 8), "iron", inflate=0.7)                              # crown band
    c("root", (-4, 24.5, 4), (8, 7, 1), "weight", inflate=0.6)                           # nape plate
    for s in (-1, 1):
        c("root", (s * 4.9 - 0.5, 24.5, -3), (1, 5, 6), "iron", inflate=0.3)              # cheek guards
        c("root", (s * 4.9 - 0.5, 29, -3.6), (1, 2, 7), "slate", inflate=0.1)
    for k, (x, h) in enumerate(((-3.2, 5), (-1.1, 7), (1.1, 8), (3.2, 5), (0, 10))):      # the five spikes of the crown
        z = -3.6 if k < 4 else 3.0
        A.spike("root", (x, 32, z if k < 4 else 3.4), "+y", h, 2, "iron" if k % 2 == 0 else "slate", layers=4, share="cs%d" % k)
    for s in (-1, 1):                                                                     # two swept horns
        A.cube("root", (s * 5.4 - 1, 31, -1), (2, 3, 3), "iron", rot=(0, 0, -s * 25), share="hn1")
        A.cube("root", (s * 7.2 - 1, 33.4, -1), (2, 3, 3), "slate", rot=(0, 0, -s * 50), share="hn2")
        A.cube("root", (s * 8.4 - 1, 36, -1), (1, 3, 2), "slate", rot=(0, 0, -s * 70), share="hn3")
    c("root", (-1, 30.4, -4.8), (2, 1, 1), "crack")                                       # the fracture on the brow
    return A.save()


def chest():
    A = Atlas(KEY, "chest", 128, 128, seed=32)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 12, -2), (8, 12, 4), "iron", inflate=0.8)                              # cuirass, three layers deep
    c("root", (-4, 17, -2), (8, 5, 4), "weight", inflate=1.3)
    c("root", (-4, 21, -2), (8, 3, 4), "slate", inflate=1.5)
    c("root", (-4, 12, -2), (8, 2, 4), "slate", inflate=1.4)                              # belt
    A.ring("root", (0, 18.5, -3.4), "z", 3.4, 1, 1, "slate", n=8, share="ring")           # the ring round the singularity
    c("root", (-2, 16.5, -3.9), (4, 4, 1), "singular")
    c("root", (-1, 17.5, -4.1), (2, 2, 1), "rim")
    for s in (-1, 1):
        for k in range(3):                                                                # hanging weight plates (the faulds)
            c("root", (s * 2.6 - 1.2 + s * k * 0.1, 8 + k * 0.3, -2.9), (2.4, 5, 1), "weight", inflate=0.1, share="fauld")
    c("root", (-4.5, 22.6, -2.5), (9, 1, 5), "crack")                                     # a seam of light under the collar
    for k in (-2, 2):
        c("root", (k - 0.5, 15, 3.0), (1, 6, 1), "slate")
    return A.save()


def arm(side, tag):
    A = Atlas(KEY, "arm_" + tag, 128, 128, seed=33 + (side > 0))
    A.bone("root", pivot=(side * 5, 22, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    # three descending pauldron plates and a spike on top
    c("root", (o(-10.2, 7), 22.6, -3.8), (7, 2, 8), "iron", inflate=0.2, rot=(0, 0, side * -8))
    c("root", (o(-10.8, 7), 21.0, -3.6), (7, 2, 7), "slate", inflate=0.2, rot=(0, 0, side * -14))
    c("root", (o(-11.2, 6), 19.4, -3.2), (6, 2, 6), "iron", inflate=0.2, rot=(0, 0, side * -20))
    A.spike("root", (side * 7.6, 24.6, 0), "+y", 5, 3, "slate", layers=3, share="ps" + tag)
    c("root", (o(-9.5, 5), 22.2, -2.6), (5, 2, 5), "weight", inflate=0.4)
    # vambrace with weight studs
    c("root", (o(-8.7, 5.6), 14, -2.8), (6, 5, 5), "iron", inflate=0.3)
    c("root", (o(-8.7, 5.6), 18.2, -2.8), (6, 1, 5), "slate", inflate=0.5)
    for k in range(3):
        c("root", (o(-9.2, 1) + side * (-0.0), 14.6 + k * 1.5, -3.6), (1, 1, 1), "crack", share="stud")
    # the fist
    c("root", (o(-8.9, 6), 11.6, -3.2), (6, 3, 6), "weight", inflate=0.4)
    c("root", (o(-8.5, 5), 11.0, -3.9), (5, 1, 1), "slate")
    c("root", (o(-8.4, 4), 12.6, -3.9), (4, 1, 1), "rim")
    return A.save()


def leg(side, tag):
    A = Atlas(KEY, "leg_" + tag, 128, 128, seed=35 + (side > 0))
    A.bone("root", pivot=(side * 2, 12, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    c("root", (o(-4.6, 6), 0, -4.2), (6, 3, 8), "weight", inflate=0.3)                    # sabaton
    c("root", (o(-4.4, 5), 2.6, -4.6), (5, 1, 2), "slate")
    c("root", (o(-4.5, 5.5), 3, -2.7), (6, 6, 5), "iron", inflate=0.3)                    # greave
    c("root", (o(-4.5, 5.5), 3, -2.7), (6, 1, 5), "slate", inflate=0.6)
    A.spike("root", (side * 2, 8.6, -3.0), "-z", 4, 3, "slate", layers=3, share="kn" + tag)   # knee spike
    c("root", (o(-4.6, 5.6), 9.4, -2.8), (6, 3, 5), "iron", inflate=0.5)                  # thigh plate
    c("root", (o(-4.6, 5.6), 11.4, -2.8), (6, 1, 5), "weight", inflate=0.7)
    c("root", (o(-2.2, 1), 6, -3.6), (1, 4, 1), "crack")
    return A.save()


def back():
    A = Atlas(KEY, "back", 128, 128, seed=37)
    A.bone("root", pivot=(0, 20, 0))
    A.bone("mantle", "root", (0, 24, 2.5))
    for k in range(5):
        x = (k - 2) * 2.6
        A.cube("mantle", (x - 1.2, 6 + abs(k - 2) * 2.0, 2.8 + (k % 2) * 0.4), (2.4, 17 - abs(k - 2) * 2.0, 1), "weight", inflate=0.1, share="mt%d" % k)
    A.cube("mantle", (-5, 22.4, 2.6), (10, 2, 2), "iron", inflate=0.3)
    # the tilted accretion ring: a parent bone tilts it, the child turns it in its own plane
    A.bone("tilt", "root", (0, 19, 7.5), rotation=(68, 0, 0))
    A.bone("accretion", "tilt", (0, 19, 7.5))
    A.ring("accretion", (0, 19, 7.5), "z", 11.5, 2, 2, "iron", n=14, share="acc")
    A.ring("accretion", (0, 19, 7.5), "z", 9.0, 1, 1, "rim", n=12, share="inn")
    for k in range(6):
        a = k * 60.0
        A.cube("accretion", (round(11.5 * math.cos(math.radians(a)) - 1, 1), round(19 + 11.5 * math.sin(math.radians(a)) - 1, 1), 6.5), (2, 2, 2), "crack", rot=(0, 0, a), share="ac")
    # three dark orbs on their own orbits (bones turn about the body's vertical axis)
    for k in range(3):
        A.bone("orbit%d" % k, "root", (0, 12 + k * 6, 4.5))
        r = 12 - k
        A.bone("orb%d" % k, "orbit%d" % k, (r, 12 + k * 6, 4.5))
        A.cube("orb%d" % k, (r - 2, 12 + k * 6 - 2, 2.5), (4, 4, 4), "singular", share="ob%d" % k)
        A.cube("orb%d" % k, (r - 1.2, 12 + k * 6 - 1.2, 1.7), (2.4, 2.4, 1), "rim", share="oc")
        A.cube("orb%d" % k, (r - 3, 12 + k * 6 - 3, 3.5), (6, 6, 2), "singular", rot=(0, 0, 45), share="ow%d" % k)
    a = A.anim
    a.clip("idle", length=9.0, loop=True)
    a.rot("idle", "accretion", spin(9.0, 1, 2, 1))
    for k in range(3):
        a.rot("idle", "orbit%d" % k, spin(9.0, 1 + k, 1, 1 if k % 2 == 0 else -1))
    a.rot("idle", "mantle", loop(9.0, lambda p: (2.5 * math.sin(p * 2 * math.pi), 0, 0)))
    a.rot("idle", "tilt", loop(9.0, lambda p: (3 * math.sin(p * 2 * math.pi), 0, 2 * math.cos(p * 2 * math.pi))))
    return A.save()


def halo():
    A = Atlas(KEY, "halo", 128, 128, seed=38)
    A.bone("root", pivot=(0, 37, 0))
    A.bone("tilt", "root", (0, 37, 0), rotation=(14, 0, 8))
    A.bone("ringa", "tilt", (0, 37, 0))
    A.ring("ringa", (0, 37, 0), "y", 7.0, 1, 1, "iron", n=12, share="ha")
    A.bone("ringb", "tilt", (0, 37, 0))
    A.ring("ringb", (0, 37, 0), "y", 5.0, 1, 1, "rim", n=10, share="hb")
    for k in range(3):
        A.bone("w%d" % k, "tilt", (0, 37, 0))
        a = k * 120.0
        A.cube("w%d" % k, (round(7 * math.cos(math.radians(a)) - 1, 1), 36, round(-7 * math.sin(math.radians(a)) - 1, 1)), (2, 2, 2), "singular", share="hw")
    a = A.anim
    a.clip("idle", length=7.0, loop=True)
    a.rot("idle", "ringa", spin(7.0, 1, 1, 1))
    a.rot("idle", "ringb", spin(7.0, 2, 1, -1))
    for k in range(3):
        a.rot("idle", "w%d" % k, spin(7.0, 2, 1, 1))
    a.pos("idle", "root", loop(7.0, lambda p: (0, 0.6 * math.sin(p * 2 * math.pi), 0)))
    return A.save()


# ====================================================================================================================
def orb():
    A = Atlas(KEY, "orb", 128, 128, seed=41)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("core", "root", (0, 16, 0))
    c = A.cube
    for k, (s, rot) in enumerate(((14, (0, 0, 0)), (14, (45, 0, 0)), (14, (0, 45, 0)), (14, (0, 0, 45)), (12, (45, 45, 0)), (12, (0, 45, 45)))):
        c("core", (-s / 2.0, 16 - s / 2.0, -s / 2.0), (s, s, s), "blackhole", rot=rot, share="sg%d" % k)
    c("core", (-5, 11, -8.5), (10, 10, 1), "rim", rot=(0, 0, 45), share="eye")
    for k, (r, th, w, m, n) in enumerate(((17, 1, 2, "rim", 14), (21, 1, 2, "crack", 16), (25, 1, 1, "iron", 18))):
        A.bone("disc%d" % k, "root", (0, 16, 0), rotation=(0, 0, 0))
        A.bone("tilt%d" % k, "disc%d" % k, (0, 16, 0), rotation=((14, 0, 0), (-22, 0, 0), (35, 0, 0))[k])
        A.bone("spin%d" % k, "tilt%d" % k, (0, 16, 0))
        A.ring("spin%d" % k, (0, 16, 0), "y", r, th, w, m, n=n, share="dk%d" % k)
    for k in range(4):
        A.bone("lens%d" % k, "root", (0, 16, 0))
        a = k * 90.0
        A.spike("lens%d" % k, (round(28 * math.cos(math.radians(a)), 1), 16, round(28 * math.sin(math.radians(a)), 1)), "+y", 5, 2, "rim", layers=3, share="ls")
    a = A.anim
    a.clip("idle", length=6.0, loop=True)
    for k in range(3):
        a.rot("idle", "spin%d" % k, spin(6.0, 1 + k, 1, 1 if k % 2 == 0 else -1))
    for k in range(4):
        a.rot("idle", "lens%d" % k, spin(6.0, 2, 1, 1))
    a.scale("idle", "core", loop(6.0, lambda p: (1 + 0.04 * math.sin(p * 4 * math.pi),) * 3))
    a.clip("fire", length=1.0, loop=True)
    for k in range(3):
        a.rot("fire", "spin%d" % k, spin(1.0, 3 + k, 1, 1))
    a.scale("fire", "core", {0.0: (1, 1, 1.6), 0.5: (1.05, 1.05, 1.8), 1.0: (1, 1, 1.6)})
    return A.save()


def sword():
    A = Atlas(KEY, "sword", 128, 128, seed=43)
    A.bone("root", pivot=(0, 16, 0))
    A.bone("blade", "root", (0, 16, 0))
    c = A.cube
    # the blade lies along -z (the front): five compressed layers of rock tapering to the point, then the point
    z = 0
    layers = ((14, 8, 4), (14, 8, 4), (13, 8, 4), (12, 8, 4), (10, 8, 4), (8, 6, 3), (6, 6, 3), (3, 4, 2))
    for k, (w, ln, th) in enumerate(layers):
        m = "rockhot" if k % 3 == 1 else "rock"
        c("blade", (-w / 2.0, 16 - th / 2.0, z - ln), (w, th, ln), m, share="bl%d" % k)
        c("blade", (-1, 16 - th / 2.0 - 0.2, z - ln), (2, th + 0.4, ln), "crack", share="fr%d" % k)       # the glowing fracture down the middle
        z -= ln
    c("blade", (-1, 15, z - 3), (2, 2, 3), "rockhot")
    for k in range(6):                                                                     # rubble pressed into the flats
        c("blade", ((-1) ** k * 6.0 - 1, 16 + (k % 2) * 2.0 - 1.2, -4 - k * 7), (2, 2, 4), "rock", rot=(0, (k * 17) % 40, (k * 23) % 50), share="rb%d" % (k % 3))
    # guard and grip
    c("root", (-9, 14, 2), (18, 4, 4), "rock", share="gd")
    c("root", (-11, 13, 1), (4, 6, 6), "rockhot", share="ge")
    c("root", (7, 13, 1), (4, 6, 6), "rockhot", share="ge")
    c("root", (-1.5, 14.5, 6), (3, 3, 9), "weight")
    c("root", (-3, 13, 14), (6, 6, 5), "rock", share="pm")
    c("root", (-1, 14.5, 15.5), (2, 2, 2), "crack")
    A.bone("shards", "root", (0, 16, 0))
    for k in range(5):
        a = k * 72.0
        A.bone("sh%d" % k, "shards", (round(11 * math.cos(math.radians(a)), 1), 16, round(-6 + 11 * math.sin(math.radians(a)), 1)))
        c("sh%d" % k, (round(11 * math.cos(math.radians(a)) - 1.5, 1), 14.5, round(-6 + 11 * math.sin(math.radians(a)) - 1.5, 1)), (3, 3, 3), "rockhot", rot=(30, a, 20), share="sd")
    a = A.anim
    a.clip("idle", length=4.0, loop=True)
    a.pos("idle", "root", loop(4.0, lambda p: (0, 1.2 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "shards", spin(4.0, 1, 1, 1))
    a.clip("launch", length=1.0, loop=True)
    a.rot("launch", "shards", spin(1.0, 3, 1, 1))
    a.pos("launch", "blade", loop(1.0, lambda p: (0, 0, 0.8 * math.sin(p * 8 * math.pi))))
    return A.save()


if __name__ == "__main__":
    ids = [helm(), chest(), arm(-1, "r"), arm(1, "l"), leg(-1, "r"), leg(1, "l"), back(), halo(), orb(), sword()]
    check(*ids)

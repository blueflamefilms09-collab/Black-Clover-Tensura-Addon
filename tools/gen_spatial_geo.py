"""Spatial Magic geo models (1.93.0 rebuild): the temporary armour of the Fallen Angel Gate set and the two things the spells put in the world.

    python3 -B tools/gen_spatial_geo.py           # writes every model, animation and texture, runs geo_check on all of them

Armour (Space.PLAYER, drawn by client/aura/SpatialAura.java through SpatialArmourKit and EquipAnimation, anchored on the model part each was built for):
  helm   (head)   pearl circlet with a gate jewel on the brow, a hood panel behind the head and a small portal ring at each ear
  chest  (body)   pearl cuirass, a gold-framed portal disc on the breast, a void belt with two hip gates, a void collar
  arm_r / arm_l   shoulder: a standing portal ring beside the shoulder on a pearl cap; vambrace: void with gold bands; gauntlet: pearl with a gate disc
  leg_r / leg_l   void boot with a pearl toe plate, a pearl greave, a gold ring round the knee with a gate disc, a gold thigh band
  back   (body)   the gate wheel: three concentric portal rings turning at different speeds behind the back (gold, pearl, void), a gate disc in the middle
                  and four rift shards orbiting; a void mantle hangs from the shoulders; clip idle
  halo   (head)   a floating portal ring over the head (gold) with an inner void ring and four rune blocks; clip idle
Things the spells make:
  gate   a standing portal frame (the Fallen Angel Gate): an octagon gold frame on two feet, a second pearl ring turning inside it, the gate disc
         (a lens of folded space), four anchor runes; clips idle, open (grows from nothing), close
  rift   a vertical tear in space (Spatial Cut / Myriad Black / Door of Fate cuts): a long slit of void with a hot white seam, jagged lips of
         shards on both sides, drifting fragments; clips idle, open, close
The look: pale pearl-lilac plates with violet void and a little gold, the gate colour of the Spatial grimoire (0xB088FF). The glow map holds the edge
fire of the plates, the whole of the gate discs and the seams of the rift.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_geo_kit import Atlas, MATS, mat, loop, spin, mirror_x, mrot, check   # noqa: E402

KEY = "spatial"

mat("pearl", "plate", [(60, 50, 100), (116, 106, 168), (186, 178, 228), (228, 224, 250), (255, 255, 255)], glow=(200, 180, 255), edge=70, dots=0.03)
mat("void", "void", [(6, 3, 16), (20, 9, 44), (42, 20, 88), (78, 44, 148), (150, 110, 232)], glow=(170, 120, 255), edge=170, core=40)
mat("gold", "metal", [(70, 48, 18), (136, 98, 38), (210, 166, 72), (250, 216, 128), (255, 248, 204)], glow=(255, 208, 120), edge=110)
mat("gate", "energy", [(26, 8, 64), (66, 28, 136), (136, 80, 228), (200, 160, 255), (255, 242, 255)], glow=(190, 130, 255), core=235)
mat("rune", "energy", [(60, 30, 120), (120, 70, 210), (190, 140, 255), (230, 205, 255), (255, 255, 255)], glow=(214, 180, 255), core=255)
mat("seam", "energy", [(120, 90, 200), (190, 160, 255), (235, 220, 255), (250, 245, 255), (255, 255, 255)], glow=(240, 230, 255), core=255)
mat("lip", "facet", [(14, 6, 34), (36, 16, 76), (70, 38, 138), (122, 80, 206), (200, 170, 255)], glow=(180, 130, 255), edge=120, cell=10)

SIDES = (("r", -1), ("l", 1))


def helm():
    A = Atlas(KEY, "helm", 128, 128, seed=11)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 30, -4), (8, 2, 8), "pearl", inflate=0.6)                              # circlet over the brow
    c("root", (-4, 24, 3.2), (8, 8, 2), "void", inflate=0.4)                              # hood panel behind the head
    c("root", (-5, 32, -3), (10, 2, 9), "pearl", inflate=0.1)                             # cap
    c("root", (-3, 31, -5.2), (6, 3, 2), "pearl", inflate=0.1)                            # brow plate
    c("root", (-1.5, 30.5, -6.4), (3, 3, 1), "gate")                                      # the jewel: a small gate
    c("root", (-5.4, 27, -1), (1, 4, 4), "gold")                                          # ear plates
    c("root", (4.4, 27, -1), (1, 4, 4), "gold")
    for s in (-1, 1):
        A.ring("root", (s * 6.0, 27.5, 0), "x", 2.6, 1, 1, "gold", n=8, share="ear")
    c("root", (-3, 24, 3.8), (6, 1, 3), "gold")
    return A.save()


def chest():
    A = Atlas(KEY, "chest", 128, 128, seed=12)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 12, -2), (8, 12, 4), "pearl", inflate=0.7)                             # cuirass
    c("root", (-4, 22, -2), (8, 2, 4), "void", inflate=1.1)                               # collar
    c("root", (-4, 12, -2), (8, 2, 4), "void", inflate=1.1)                               # belt
    A.frame("root", -3.5, 15.5, -3.6, 7, 7, 1, 1, "gold", share="chestframe")           # the gate frame on the breast
    c("root", (-2.5, 16.5, -3.5), (5, 5, 1), "gate")
    c("root", (-3, 13, -3.6), (6, 1, 1), "gold")
    for s in (-1, 1):
        A.ring("root", (s * 4.9, 13, 0), "x", 2.2, 1, 1, "gold", n=8, share="hip")        # hip gates
        c("root", (s * 4.9 - 0.5, 12.2, -0.5), (1, 1.6, 1), "gate")
    c("root", (-3, 22.4, -3.1), (6, 1, 1), "gold", inflate=0.3)
    for k in (-2, 2):
        c("root", (k - 0.5, 18, 2.9), (1, 4, 1), "gold")                                    # spine ridges
    return A.save()


def arm(side, tag):
    A = Atlas(KEY, "arm_" + tag, 128, 128, seed=13 + (side > 0))
    A.bone("root", pivot=(side * 5, 22, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]

    def rr(r):
        return mrot(r, side)
    # shoulder cap and the standing portal ring beside it
    c("root", (o(-8.9, 6), 22, -3.2), (6, 2, 6), "pearl", inflate=0.2)
    c("root", (o(-9.4, 1), 20.5, -2.8), (1, 4, 5), "pearl", inflate=0.1)
    A.ring("root", (side * 9.8, 23.5, 0), "x", 3.5, 1, 1, "gold", n=8, share="shoulder" + tag)
    c("root", (o(-10.3, 1), 22.5, -1), (1, 2, 2), "gate")
    # vambrace
    c("root", (o(-8.5, 5), 14, -2.5), (5, 5, 5), "void", inflate=0.3)
    c("root", (o(-8.5, 5), 18.6, -2.5), (5, 1, 5), "gold", inflate=0.4)
    c("root", (o(-8.5, 5), 14, -2.5), (5, 1, 5), "gold", inflate=0.4)
    c("root", (o(-8.5, 5), 15.6, -3.3), (5, 2, 1), "pearl", inflate=0.1)
    # gauntlet
    c("root", (o(-8.5, 5), 12, -2.5), (5, 2, 5), "pearl", inflate=0.4)
    c("root", (o(-7.5, 3), 11.4, -3.2), (3, 1, 1), "gate")
    c("root", (o(-8.0, 4), 12.2, -3.4), (4, 1, 1), "gold")
    # elbow spike and forearm gate lens
    A.spike("root", (side * 8.6, 19.8, 2.4), "+z", 3, 2, "pearl", layers=3, share="elbow" + tag)
    c("root", (o(-6.5, 1), 15.5, 2.6), (1, 3, 1), "gate")
    return A.save()


def leg(side, tag):
    A = Atlas(KEY, "leg_" + tag, 128, 128, seed=15 + (side > 0))
    A.bone("root", pivot=(side * 2, 12, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    c("root", (o(-4.4, 5), 0, -3.2), (5, 3, 6), "void", inflate=0.3)                       # boot
    c("root", (o(-4.2, 4), 0, -4.4), (4, 2, 1), "pearl")                                   # toe plate
    c("root", (o(-4.4, 5), 3, -2.5), (5, 6, 5), "pearl", inflate=0.3)                      # greave
    c("root", (o(-4.4, 5), 3, -2.5), (5, 1, 5), "gold", inflate=0.5)
    A.ring("root", (side * 2, 9.2, -3.0), "z", 2.0, 1, 1, "gold", n=8, share="knee" + tag)   # knee ring + gate
    c("root", (o(-2.5, 1), 8.7, -3.4), (1, 1, 1), "gate")
    c("root", (o(-4.4, 5), 10.5, -2.5), (5, 1, 5), "gold", inflate=0.4)                    # thigh band
    c("root", (o(-4.4, 5), 11.3, -2.5), (5, 1, 5), "void", inflate=0.4)
    c("root", (o(-1.0, 1), 3.5, 2.4), (1, 4, 1), "gate")                                   # heel lamp
    return A.save()


def back():
    A = Atlas(KEY, "back", 128, 128, seed=17)
    A.bone("root", pivot=(0, 20, 0))
    A.bone("mantle", "root", (0, 24, 2))
    A.cube("mantle", (-5, 5, 2.6), (10, 18, 1), "void", inflate=0.3)
    A.cube("mantle", (-6, 3, 2.9), (12, 3, 1), "lip", inflate=0.2)
    A.cube("mantle", (-4.5, 22, 2.3), (9, 2, 2), "gold")
    cz = 6.0
    spec = (("ring_a", 10.0, "gold", 1.0), ("ring_b", 7.5, "pearl", -1.4), ("ring_c", 5.0, "void", 2.0))
    for name, r, m, _ in spec:
        A.bone(name, "root", (0, 19, cz))
        A.ring(name, (0, 19, cz), "z", r, 1, 1, m, n=12 if r > 6 else 8, share=name)
    A.bone("disc", "root", (0, 19, cz))
    A.cube("disc", (-3, 16, cz - 0.5), (6, 6, 1), "gate", rot=(0, 0, 45), share="d1")
    A.cube("disc", (-3, 16, cz - 0.5), (6, 6, 1), "gate", share="d2")
    for k in range(4):
        a = k * 90.0
        A.bone("shard%d" % k, "root", (0, 19, cz))
        x, y = 12.5 * math.cos(math.radians(a)), 19 + 12.5 * math.sin(math.radians(a))
        A.spike("shard%d" % k, (round(x, 1), round(y, 1), cz), "+y", 4, 2, "lip", layers=3, share="sh")
    # runes between the rings
    for k in range(6):
        a = k * 60.0 + 30
        A.bone("rune%d" % k, "ring_b", (0, 19, cz))
        A.cube("rune%d" % k, (round(8.8 * math.cos(math.radians(a)) - 0.5, 1), round(19 + 8.8 * math.sin(math.radians(a)) - 0.5, 1), cz - 0.5), (1, 1, 1), "rune", share="ru")
    a = A.anim
    a.clip("idle", length=8.0, loop=True)
    a.rot("idle", "ring_a", spin(8.0, 1, 2, 1))
    a.rot("idle", "ring_b", spin(8.0, 2, 2, -1))
    a.rot("idle", "ring_c", spin(8.0, 3, 2, 1))
    a.rot("idle", "disc", spin(8.0, 1, 2, -1))
    for k in range(4):
        a.rot("idle", "shard%d" % k, spin(8.0, 2, 2, 1))
    a.rot("idle", "mantle", loop(8.0, lambda p: (3 * math.sin(p * 2 * math.pi), 0, 0)))
    a.pos("idle", "root", loop(8.0, lambda p: (0, 0.6 * math.sin(p * 4 * math.pi), 0)))
    return A.save()


def halo():
    A = Atlas(KEY, "halo", 128, 128, seed=18)
    A.bone("root", pivot=(0, 36, 0))
    A.bone("outer", "root", (0, 36, 0))
    A.ring("outer", (0, 36, 0), "y", 6.5, 1, 1, "gold", n=12, share="ho")
    A.bone("inner", "root", (0, 36, 0))
    A.ring("inner", (0, 36, 0), "y", 4.2, 1, 1, "void", n=8, share="hi")
    for k in range(4):
        a = k * 90.0
        A.bone("hr%d" % k, "outer", (0, 36, 0))
        A.cube("hr%d" % k, (round(6.5 * math.cos(math.radians(a)) - 1, 1), 35.4, round(-6.5 * math.sin(math.radians(a)) - 1, 1)), (2, 2, 2), "rune", rot=(0, a, 0), share="hrb")
    a = A.anim
    a.clip("idle", length=6.0, loop=True)
    a.rot("idle", "outer", spin(6.0, 1, 1, 1))
    a.rot("idle", "inner", spin(6.0, 2, 1, -1))
    a.pos("idle", "root", loop(6.0, lambda p: (0, 0.7 * math.sin(p * 2 * math.pi), 0)))
    return A.save()


# ====================================================================================================================
# things the spells make (Space.PROP: the model stands on the prop's feet, y up, the front looks along the prop's forward)
# ====================================================================================================================
def gate():
    A = Atlas(KEY, "gate", 128, 128, seed=21)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("frame", "root", (0, 24, 0))
    c = A.cube
    A.ring("frame", (0, 24, 0), "z", 22, 3, 3, "gold", n=16, share="gf")                   # the gold octagon-ish frame
    A.ring("frame", (0, 24, 0), "z", 19, 1, 2, "pearl", n=16, share="gp")
    for k in range(8):                                                                       # anchor runes on the frame
        a = k * 45.0 + 22.5
        c("frame", (round(22 * math.cos(math.radians(a)) - 1.5, 1), round(24 + 22 * math.sin(math.radians(a)) - 1.5, 1), -2.2), (3, 3, 1), "rune", share="ar")
    A.bone("turn", "root", (0, 24, 0))
    A.ring("turn", (0, 24, 0), "z", 15, 1, 1, "void", n=12, share="gt")
    for k in range(6):
        a = k * 60.0
        c("turn", (round(15 * math.cos(math.radians(a)) - 1, 1), round(24 + 15 * math.sin(math.radians(a)) - 1, 1), -0.5), (2, 2, 1), "rune", share="tr")
    A.bone("disc", "root", (0, 24, 0))
    for k in range(4):                                                                       # the lens of folded space: stacked rotated plates
        r = k * 22.5
        c("disc", (-12, 12, -0.5), (24, 24, 1), "gate", rot=(0, 0, r), share="lens%d" % k)
    c("disc", (-7, 17, -0.8), (14, 14, 1), "seam", rot=(0, 0, 45), share="core")
    c("root", (-26, 0, -4), (10, 2, 8), "pearl")                                             # feet
    c("root", (16, 0, -4), (10, 2, 8), "pearl")
    c("root", (-24, 2, -2), (6, 4, 4), "gold")
    c("root", (18, 2, -2), (6, 4, 4), "gold")
    a = A.anim
    a.clip("idle", length=10.0, loop=True)
    a.rot("idle", "turn", spin(10.0, 2, 2, 1))
    a.rot("idle", "disc", spin(10.0, 1, 2, -1))
    a.rot("idle", "frame", loop(10.0, lambda p: (0, 0, 1.2 * math.sin(p * 2 * math.pi))))
    a.scale("idle", "disc", loop(10.0, lambda p: (1 + 0.04 * math.sin(p * 6 * math.pi),) * 3))
    a.clip("open", length=1.0, loop=False)
    a.scale("open", "root", {0.0: (0.05, 0.05, 1), 0.5: (0.7, 0.2, 1), 1.0: (1, 1, 1)})
    a.rot("open", "turn", {0.0: (0, 0, -180), 1.0: (0, 0, 0)})
    a.clip("close", length=1.0, loop=False)
    a.scale("close", "root", {0.0: (1, 1, 1), 0.5: (0.7, 0.2, 1), 1.0: (0.05, 0.05, 1)})
    a.rot("close", "turn", {0.0: (0, 0, 0), 1.0: (0, 0, 180)})
    return A.save()


def rift():
    A = Atlas(KEY, "rift", 128, 128, seed=23)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("slit", "root", (0, 24, 0))
    c = A.cube
    # the tear: stacked slabs narrowing to both ends, void inside with a hot white seam down the middle
    rows = ((0, 2), (4, 3), (8, 4), (12, 5), (16, 6), (20, 6), (24, 6), (28, 5), (32, 4), (36, 3), (40, 2), (44, 1))
    for i, (y, w) in enumerate(rows):
        c("slit", (-w / 2.0, y, -1), (w, 4, 2), "void", share="t%d" % i)
        c("slit", (-0.5, y, -1.2), (1, 4, 1), "seam", share="s%d" % i)
    # jagged lips: shards on both sides, leaning out and back
    for i, (y, w) in enumerate(rows[1:-1]):
        for s in (-1, 1):
            lean = 12 + 6 * (i % 3)
            c("slit", (s * (w / 2.0 + 1.6) - 1, y + 1, -1.5 + (i % 2)), (2, 4, 3), "lip", rot=(0, 0, -s * lean), share="lp%d" % (i % 3))
    A.bone("frags", "root", (0, 24, 0))
    for k in range(6):
        a = k * 60.0
        x, z = 7 * math.cos(math.radians(a)), 7 * math.sin(math.radians(a))
        A.bone("fr%d" % k, "frags", (round(x, 1), 14 + 6 * (k % 3), round(z, 1)))
        A.spike("fr%d" % k, (round(x, 1), 14 + 6 * (k % 3), round(z, 1)), "+y", 5, 2, "lip", layers=3, share="fg")
    a = A.anim
    a.clip("idle", length=4.0, loop=True)
    a.scale("idle", "slit", loop(4.0, lambda p: (1 + 0.06 * math.sin(p * 4 * math.pi), 1, 1)))
    a.rot("idle", "frags", spin(4.0, 1, 1, 1))
    for k in range(6):
        a.pos("idle", "fr%d" % k, loop(4.0, lambda p, k=k: (0, 1.5 * math.sin((p + k / 6.0) * 2 * math.pi), 0)))
    a.clip("open", length=0.6, loop=False)
    a.scale("open", "slit", {0.0: (0.05, 0.02, 1), 0.3: (0.4, 1.0, 1), 0.6: (1, 1, 1)})
    a.clip("close", length=0.6, loop=False)
    a.scale("close", "slit", {0.0: (1, 1, 1), 0.3: (0.4, 0.7, 1), 0.6: (0.05, 0.02, 1)})
    return A.save()


if __name__ == "__main__":
    ids = [helm(), chest(), arm(-1, "r"), arm(1, "l"), leg(-1, "r"), leg(1, "l"), back(), halo(), gate(), rift()]
    check(*ids)

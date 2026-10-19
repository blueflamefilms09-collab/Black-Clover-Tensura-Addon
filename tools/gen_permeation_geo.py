"""Permeation Magic geo models (1.93.0 rebuild): the Phase Shroud (the temporary armour) and the two things the spells put in the world.

    python3 -B tools/gen_permeation_geo.py        # writes every model, animation and texture, runs geo_check on all of them

Permeation is the magic of passing through: things that are there and are not. The look is a see-through set made of outlines: bars, rungs and rings of pale
cyan light with the body showing through the gaps, wisps instead of cloth. Every piece is a ghost (always translucent).
Armour (Space.PLAYER, drawn by client/aura/PermeationAura.java through SpatialArmourKit and EquipAnimation):
  helm   (head)   three thin horizontal rings stacked over the brow (nothing covers the face), side bars, a hood of wisps streaming back
  chest  (body)   a ribcage lattice: six upright bars with three rungs across the front, a spine ridge, a small core gem at the sternum
  arm_r / arm_l   a hollow pauldron frame, three open rings round the forearm, a ghost glove of thin plates
  leg_r / leg_l   a ladder of two bars and three rungs up the shin, rings at the ankle and the knee
  back   (body)   the phase cloak: layered wisp slabs flowing down the back, swaying, and two lattice wings of long thin feathers (clip idle)
  halo   (head)   three nested thin rings tilted differently turning like a gyroscope (clip idle)
Things the spells make:
  echo   the phase echo (Phase Step): a hollow humanoid of bars and wisps left standing where the caster stepped out of the world; clips idle, fade
  door   the phase door (Phase Through): a doorway frame of thin bars with a hanging membrane of wisps and chips of lattice drifting round it; clips open, idle, close
The glow map carries every outline, the core gems and a faint wash along the wisps.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_geo_kit import Atlas, MATS, mat, loop, spin, mirror_x, mrot, check   # noqa: E402

KEY = "permeation"

mat("bar", "metal", [(30, 90, 110), (70, 160, 184), (130, 220, 238), (200, 246, 255), (255, 255, 255)], glow=(110, 235, 255), edge=190)
mat("wisp", "wisp", [(60, 150, 170), (110, 200, 220), (170, 238, 250), (220, 252, 255), (255, 255, 255)], glow=(150, 240, 255), core=70, alpha=130)
mat("ghost", "glass", [(50, 130, 150), (100, 190, 210), (160, 232, 246), (214, 250, 255), (255, 255, 255)], glow=(140, 240, 255), edge=140, alpha=80)
mat("core", "energy", [(40, 130, 160), (80, 200, 230), (140, 240, 255), (205, 252, 255), (255, 255, 255)], glow=(150, 245, 255), core=255)


def helm():
    A = Atlas(KEY, "helm", 128, 128, seed=101)
    A.bone("root", pivot=(0, 24, 0))
    for k, (r, y) in enumerate(((5.6, 30.0), (5.0, 31.4), (4.4, 32.8))):                   # three thin rings over the brow
        A.ring("root", (0, y, 0), "y", r, 0.7, 0.8, "bar", n=8, share="hr%d" % k)
    c = A.cube
    for s in (-1, 1):
        c("root", (s * 5.2 - 0.4, 26, -2.5), (0.8, 5, 0.8), "bar", share="sb")              # side bars
        c("root", (s * 5.2 - 0.4, 26, 1.5), (0.8, 5, 0.8), "bar", share="sb")
        c("root", (s * 5.2 - 0.4, 30.4, -2.5), (0.8, 0.8, 5), "bar", share="sb2")
    for k, (x, z, h) in enumerate(((-2.5, 4.4, 9), (0, 4.8, 12), (2.5, 4.4, 9), (-1.2, 5.2, 7), (1.2, 5.2, 7))):      # a hood of wisps streaming back
        A.cube("root", (x - 1, 22.5 - h * 0.3, z), (2, h, 1.2), "wisp", rot=(12, 0, 0), share="hw%d" % k)
    c("root", (-0.5, 33.5, -4.2), (1, 1, 1), "core")
    return A.save()


def chest():
    A = Atlas(KEY, "chest", 128, 128, seed=102)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    for k in range(6):                                                                    # six upright bars across the front
        x = -3.6 + k * 1.44
        c("root", (x - 0.4, 12.2, -3.0), (0.8, 11, 0.8), "bar", share="rib")
    for k in range(3):                                                                    # three rungs
        y = 14.0 + k * 3.4
        c("root", (-4.2, y, -3.0), (8.4, 0.8, 0.8), "bar", share="rung")
    c("root", (-4, 12, -2), (8, 12, 4), "ghost", inflate=0.8)                             # a faint skin of glass over it all
    for k in range(4):                                                                    # spine ridge
        c("root", (-0.5, 13 + k * 2.6, 2.9), (1, 1.6, 1), "bar", share="sp")
    c("root", (-4.4, 22.4, -2.4), (8.8, 0.8, 4.8), "bar")                                  # collar rail
    c("root", (-4.4, 12.2, -2.4), (8.8, 0.8, 4.8), "bar")                                  # waist rail
    c("root", (-0.8, 18.4, -3.6), (1.6, 1.6, 1), "core")
    return A.save()


def arm(side, tag):
    A = Atlas(KEY, "arm_" + tag, 128, 128, seed=103 + (side > 0))
    A.bone("root", pivot=(side * 5, 22, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    A.frame("root", o(-9.6, 6), 18.4, -3.2, 6, 5, 6, 0.8, "bar", share="pa")                # the pauldron: a hollow frame
    c("root", (o(-9.6, 6), 22.4, -3.2), (6, 0.8, 6), "bar")
    c("root", (o(-9.0, 5), 22.0, -2.6), (5, 1, 5), "ghost", inflate=0.3)
    for k in range(2):                                                                    # two open rings round the forearm
        A.ring("root", (side * 6, 18.0 - k * 3.4, 0), "y", 3.0, 0.7, 0.8, "bar", n=8, share="fr%d" % k)
    c("root", (o(-8.6, 5), 12.4, -2.5), (5, 0.8, 5), "bar", inflate=0.2)
    for k in range(4):                                                                    # a ghost glove of thin plates
        c("root", (o(-8.4, 3.4), 12.2, -2.4 + k * 1.2), (3.4, 0.8, 0.9), "ghost", share="gl")
    c("root", (o(-6.4, 1), 15.6, -3.4), (1, 1, 1), "core")
    return A.save()


def leg(side, tag):
    A = Atlas(KEY, "leg_" + tag, 128, 128, seed=105 + (side > 0))
    A.bone("root", pivot=(side * 2, 12, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    for s in (-1, 1):                                                                     # two bars and three rungs up the shin
        c("root", (side * 2 + s * 2.3 - 0.4, 1.5, -2.9), (0.8, 8, 0.8), "bar", share="lb")
    for k in range(3):
        c("root", (o(-4.2, 4.8), 2.4 + k * 3.0, -3.0), (4.8, 0.8, 0.8), "bar", share="lr")
    A.ring("root", (side * 2, 1.2, 0), "y", 3.2, 0.7, 0.8, "bar", n=8, share="an" + tag)
    A.ring("root", (side * 2, 9.6, 0), "y", 3.3, 0.7, 0.8, "bar", n=8, share="kn" + tag)
    c("root", (o(-4.4, 5), 1.5, -2.6), (5, 8, 5), "ghost", inflate=0.2)
    c("root", (o(-2.4, 1), 9.2, -3.4), (1, 1, 1), "core")
    return A.save()


def back():
    A = Atlas(KEY, "back", 128, 128, seed=107)
    A.bone("root", pivot=(0, 20, 0))
    A.bone("cloak", "root", (0, 24, 2.5))
    for k in range(7):
        x = (k - 3) * 1.8
        A.cube("cloak", (x - 1, 22 - (16 - abs(k - 3) * 2), 3 + (k % 2) * 0.6), (2, 16 - abs(k - 3) * 2, 0.9), "wisp", share="cl%d" % abs(k - 3))
    A.cube("cloak", (-5, 22.4, 2.6), (10, 1, 1), "bar")
    for side, name in ((-1, "wr"), (1, "wl")):                                            # lattice wings: long thin feathers fanned out
        A.bone(name, "root", (side * 2.5, 22, 3.5))
        for k in range(6):
            ang = 18 + k * 13.0
            ln = 20 - k * 1.8
            A.cube(name, (side * 2.5 - 0.4 + side * 0.0, 22 - 0.4, 3.5), (0.8, ln, 0.8), "bar", rot=(0, 0, side * -ang), share="wf%d" % k)
        A.cube(name, (side * 2.5 - 2, 21.5, 3.4), (4, 1, 1), "bar", share="wr")
    a = A.anim
    a.clip("idle", length=7.0, loop=True)
    a.rot("idle", "cloak", loop(7.0, lambda p: (6 * math.sin(p * 2 * math.pi), 0, 3 * math.sin(p * 4 * math.pi))))
    a.rot("idle", "wr", loop(7.0, lambda p: (0, 8 + 6 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "wl", loop(7.0, lambda p: (0, -8 - 6 * math.sin(p * 2 * math.pi), 0)))
    return A.save()


def halo():
    A = Atlas(KEY, "halo", 128, 128, seed=108)
    A.bone("root", pivot=(0, 37, 0))
    for k, (r, tilt) in enumerate(((7.0, (18, 0, 0)), (5.6, (-18, 0, 14)), (4.2, (0, 0, -22)))):
        A.bone("g%d" % k, "root", (0, 37, 0), rotation=tilt)
        A.bone("gs%d" % k, "g%d" % k, (0, 37, 0))
        A.ring("gs%d" % k, (0, 37, 0), "y", r, 0.7, 0.8, "bar", n=10 - 2 * k, share="gy%d" % k)
        A.cube("gs%d" % k, (r - 0.8, 36.2, -0.8), (1.6, 1.6, 1.6), "core", share="gc")
    a = A.anim
    a.clip("idle", length=6.0, loop=True)
    for k in range(3):
        a.rot("idle", "gs%d" % k, spin(6.0, 1 + k, 1, 1 if k % 2 == 0 else -1))
    a.pos("idle", "root", loop(6.0, lambda p: (0, 0.6 * math.sin(p * 2 * math.pi), 0)))
    return A.save()


# ====================================================================================================================
def echo():
    A = Atlas(KEY, "echo", 128, 128, seed=111)
    A.bone("root", pivot=(0, 0, 0))
    c = A.cube
    # a hollow humanoid: outlines of every part and a wash of wisps inside, a tail of wisps behind
    def outline(x, y, z, w, h, d, share):
        for dx in (0, w - 0.8):
            for dz in (0, d - 0.8):
                c("root", (x + dx, y, z + dz), (0.8, h, 0.8), "bar", share=share + "v")
        for dy in (0, h - 0.8):
            c("root", (x, y + dy, z), (w, 0.8, 0.8), "bar", share=share + "x")
            c("root", (x, y + dy, z + d - 0.8), (w, 0.8, 0.8), "bar", share=share + "x")
            c("root", (x, y + dy, z), (0.8, 0.8, d), "bar", share=share + "z")
            c("root", (x + w - 0.8, y + dy, z), (0.8, 0.8, d), "bar", share=share + "z")
    outline(-4, 24, -4, 8, 8, 8, "hd")
    outline(-4, 12, -2, 8, 12, 4, "bd")
    outline(-8, 12, -2, 4, 12, 4, "ar")
    outline(4, 12, -2, 4, 12, 4, "al")
    outline(-4, 0, -2, 4, 12, 4, "lr")
    outline(0, 0, -2, 4, 12, 4, "ll")
    c("root", (-4, 24, -4), (8, 8, 8), "ghost", inflate=-0.2)
    c("root", (-4, 12, -2), (8, 12, 4), "ghost", inflate=-0.2)
    c("root", (-1, 27.5, -4.2), (2, 1, 1), "core")
    c("root", (-0.8, 17, -2.3), (1.6, 1.6, 1), "core")
    A.bone("tail", "root", (0, 12, 2))
    for k in range(6):
        c("tail", (-2.5 + (k % 2) * 0.5, 10 - k * 1.6, 3 + k * 2.2), (5 - k * 0.6, 3, 4), "wisp", share="tw%d" % k)
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    a.scale("idle", "tail", loop(3.0, lambda p: (1, 1, 1 + 0.18 * math.sin(p * 2 * math.pi))))
    a.pos("idle", "root", loop(3.0, lambda p: (0, 0.8 * math.sin(p * 2 * math.pi), 0)))
    return A.save()


def door():
    A = Atlas(KEY, "door", 128, 128, seed=113)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("frame", "root", (0, 0, 0))
    c = A.cube
    w, h = 28, 48
    c("frame", (-w / 2.0, 0, -1), (2, h, 2), "bar", share="jb")
    c("frame", (w / 2.0 - 2, 0, -1), (2, h, 2), "bar", share="jb")
    c("frame", (-w / 2.0, h - 2, -1), (w, 2, 2), "bar", share="lt")
    c("frame", (-w / 2.0 - 2, 0, -2), (6, 2, 4), "bar", share="ft")
    c("frame", (w / 2.0 - 4, 0, -2), (6, 2, 4), "bar", share="ft")
    for k in range(5):                                                                    # runes up the jambs
        c("frame", (-w / 2.0 + 0.2, 6 + k * 8, -1.4), (1.6, 2, 1), "core", share="rn")
        c("frame", (w / 2.0 - 1.8, 6 + k * 8, -1.4), (1.6, 2, 1), "core", share="rn")
    A.bone("membrane", "root", (0, 24, 0))
    for k in range(12):                                                                   # the membrane: hanging wisp slabs
        c("membrane", (-w / 2.0 + 2, 2 + k * 3.8, -0.4), (w - 4, 3.4, 0.8), "wisp", share="mb%d" % (k % 3))
    A.bone("chips", "root", (0, 24, 0))
    for k in range(6):
        a = k * 60.0
        x, z = 20 * math.cos(math.radians(a)), 10 * math.sin(math.radians(a))
        A.bone("chip%d" % k, "chips", (round(x, 1), 14 + (k % 3) * 9, round(z, 1)))
        c("chip%d" % k, (round(x - 1, 1), 14 + (k % 3) * 9, round(z - 1, 1)), (2, 5, 0.8), "bar", rot=(10, a, 0), share="chip")
    an = A.anim
    an.clip("idle", length=4.0, loop=True)
    an.scale("idle", "membrane", loop(4.0, lambda p: (1 + 0.02 * math.sin(p * 4 * math.pi), 1, 1)))
    an.rot("idle", "chips", spin(4.0, 1, 1, 1))
    an.clip("open", length=0.6, loop=False)
    an.scale("open", "membrane", {0.0: (0.02, 1, 1), 0.6: (1, 1, 1)})
    an.clip("close", length=0.6, loop=False)
    an.scale("close", "membrane", {0.0: (1, 1, 1), 0.6: (0.02, 1, 1)})
    return A.save()


if __name__ == "__main__":
    ids = [helm(), chest(), arm(-1, "r"), arm(1, "l"), leg(-1, "r"), leg(1, "l"), back(), halo(), echo(), door()]
    check(*ids)

"""Slash Magic geo models (1.93.0 rebuild): the Ripper's temporary armour and the three things the spells put in the world.

    python3 -B tools/gen_slash_geo.py             # writes every model, animation and texture, runs geo_check on all of them

Armour (Space.PLAYER, drawn by client/aura/SlashAura.java through SpatialArmourKit and EquipAnimation):
  helm   (head)   a Ripper's cowl: a serrated mouth guard of spikes (the eyes stay free), three swept blade fins on the crown, blade ears
  chest  (body)   slat plates three rows deep with diagonal slash scars glowing green, a bandolier strap with a gem, a ragged skirt of downward blades
  arm_r / arm_l   a spiked angular pauldron, a vambrace, a gauntlet, and the FOREARM BLADE: a long green-edged serrated blade on the outside of the forearm running past the hand (down to 2 px above the soles)
  leg_r / leg_l   a greave with a back fin, a knee spike, a boot with a toe blade
  back   (body)   the Death Scythe slung across the back (a long shaft and a great curved blade with a glowing edge), torn cape tatters, six blade shards orbiting; clip idle
  halo   (head)   a saw-ring of blades standing behind the head, turning; clip idle
Things the spells make:
  crescent   the projected slash (Slash Wave): a wide jagged crescent of green-edged blade segments with spikes on the back edge; clips idle, fly
  scythe     the Death Scythe: a long shaft and a vast curved blade that sweeps round the caster; clips idle, sweep (a full turn)
  ghost      the Ripper Dash after-image: a hollow humanoid of translucent green glass leaning into the dash with blade arms; clip idle
The look: gunmetal-black plates with a toxic green edge (the Slash grimoire's 0x48FF7A); the glow map is every edge, scar and gem.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_geo_kit import Atlas, MATS, mat, loop, spin, mirror_x, mrot, check   # noqa: E402

KEY = "slash"

mat("steel", "metal", [(4, 10, 8), (20, 36, 30), (44, 70, 58), (84, 118, 100), (166, 204, 186)], glow=(72, 255, 122), edge=120)
mat("hide", "plate", [(3, 6, 5), (12, 22, 18), (26, 44, 36), (46, 74, 60), (96, 130, 112)], glow=(60, 220, 110), edge=40)
mat("edge", "energy", [(40, 160, 80), (80, 220, 120), (150, 255, 175), (210, 255, 225), (255, 255, 255)], glow=(130, 255, 170), core=255)
mat("scar", "energy", [(30, 140, 70), (60, 210, 110), (110, 255, 150), (190, 255, 210), (255, 255, 255)], glow=(100, 255, 150), core=235)
mat("blade", "metal", [(10, 30, 20), (40, 90, 60), (90, 160, 120), (170, 230, 195), (240, 255, 248)], glow=(90, 255, 140), edge=90)
mat("glass", "glass", [(30, 120, 70), (60, 180, 100), (110, 230, 150), (180, 255, 205), (240, 255, 245)], glow=(110, 255, 160), edge=170, alpha=140)


def arc_blade(A, bone, centre_y, R, a0, a1, n, w_max, h, mat_body, mat_edge, edge_t=1, share=None, z0=0.0):
    """A crescent lying in the horizontal plane: n segments on a circle of radius R (angles in degrees, 0 = straight ahead, -z), the width (radially) swelling
    to w_max in the middle and going to points at the ends, a hot edge strip on the outer side. The circle's centre is at z = z0."""
    L = 2 * R * math.tan(math.radians(abs(a1 - a0)) / (2 * n)) + 1.2
    for k in range(n):
        t = (k + 0.5) / n
        th = math.radians(a0 + (a1 - a0) * t)
        w = max(1.0, w_max * math.sin(math.pi * t) ** 0.8)
        rc = R - w / 2.0
        c = (rc * math.sin(th), centre_y, z0 - rc * math.cos(th))
        A.box_c(bone, c, (L, h, w), mat_body, rot=(0, -math.degrees(th), 0), share=(share + "b%d" % int(round(w))) if share else None)
        ce = ((R + edge_t / 2.0 - 0.2) * math.sin(th), centre_y, z0 - (R + edge_t / 2.0 - 0.2) * math.cos(th))
        A.box_c(bone, ce, (L, h, edge_t), mat_edge, rot=(0, -math.degrees(th), 0), share=(share + "e") if share else None)


def helm():
    A = Atlas(KEY, "helm", 128, 128, seed=51)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 24, -4), (8, 3, 8), "hide", inflate=0.7)                                  # lower cowl (the mouth and jaw; the eyes stay free)
    c("root", (-3.5, 24.2, -5.2), (7, 2, 1), "steel", inflate=0.1)                           # the guard plate
    for k in range(5):                                                                        # the serrated teeth of the mouth guard
        A.spike("root", (-3 + k * 1.5, 25.6, -5.6), "-y", 2.5, 1.4, "blade", layers=2, share="tooth")
    c("root", (-4, 30, -4), (8, 2, 8), "steel", inflate=0.6)                                 # brow band
    c("root", (-4, 24, 4), (8, 7, 1), "hide", inflate=0.6)                                   # nape
    for k, (x, ang) in enumerate(((-2.4, 14), (0, 0), (2.4, -14))):                          # three swept blade fins on the crown
        A.blade("root", (x - 0.5, 32.4, 1.0 + (1 if k == 1 else 0)), 12 - (2 if k != 1 else 0), 4, 1, "blade", axis="+z", layers=5, taper=0.8, share="fin%d" % k)
        c("root", (x - 0.4, 32.2, 1.5), (1, 1, 9), "edge", rot=(0, ang, 0), share="fe%d" % k)
    for s in (-1, 1):
        A.blade("root", (s * 5.2 - 0.5, 28, -1), 7, 3, 1, "blade", axis="+z", layers=4, taper=0.8, share="ear")
        c("root", (s * 5.2 - 0.5, 29, 5.6), (1, 1, 3), "edge")
    return A.save()


def chest():
    A = Atlas(KEY, "chest", 128, 128, seed=52)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 12, -2), (8, 12, 4), "hide", inflate=0.7)                                  # under-layer
    for k in range(3):                                                                        # slat plates, three rows
        c("root", (-4, 13 + k * 4, -2), (8, 3, 4), "steel", inflate=1.0 + 0.1 * k, share="slat%d" % k)
    c("root", (-4, 22, -2), (8, 2, 4), "hide", inflate=1.3)
    for k in range(3):                                                                        # slash scars across the plates
        c("root", (-3.5 + k * 2.5, 14 + k * 0.6, -3.7), (1, 7, 1), "scar", rot=(0, 0, 28), share="scar%d" % k)
    c("root", (-5, 16, -3.3), (10, 1, 1), "hide", rot=(0, 0, -38))                           # the bandolier strap
    c("root", (-1, 17, -3.9), (2, 2, 1), "edge")
    for k in range(5):                                                                        # a ragged skirt of downward blades
        x = -3.6 + k * 1.8
        A.spike("root", (x, 12.0, -2.6 + (k % 2) * 0.6), "-y", 5 - (k % 3), 1.8, "blade", layers=2, share="sk%d" % (k % 3))
        A.spike("root", (x, 12.0, 2.8 - (k % 2) * 0.6), "-y", 5 - ((k + 1) % 3), 1.8, "blade", layers=2, share="sk%d" % ((k + 1) % 3))
    c("root", (-4.4, 22.8, -2.4), (9, 1, 5), "edge", inflate=0.1)                            # a hot seam under the collar
    return A.save()


def arm(side, tag):
    A = Atlas(KEY, "arm_" + tag, 128, 128, seed=53 + (side > 0))
    A.bone("root", pivot=(side * 5, 22, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    # angular pauldron with spikes
    c("root", (o(-9.4, 6), 22.4, -3.6), (6, 2, 7), "steel", inflate=0.2, rot=(0, 0, side * -10))
    A.spike("root", (side * 8.2, 24.6, 0), "+y", 5, 3, "blade", layers=3, share="ps" + tag)
    A.spike("root", (side * 10.4, 22.4, 0), "+x" if side > 0 else "-x", 5, 3, "blade", layers=3, share="px" + tag)
    c("root", (o(-8.6, 5), 14, -2.5), (5, 6, 5), "hide", inflate=0.4)                         # vambrace
    c("root", (o(-8.6, 5), 14.2, -3.2), (5, 5, 1), "steel", inflate=0.2)
    c("root", (o(-8.6, 5), 12, -2.5), (5, 2, 5), "steel", inflate=0.4)                        # gauntlet
    # the forearm blade: a long blade on the outside of the arm from the elbow past the hand, curving out at the tip
    bx = o(-10.6, 1.6)
    A.blade("root", (bx, 2, -1.6), 18, 3, 1, "blade", axis="+y", layers=7, taper=0.5, share="fb" + tag)
    c("root", (bx + 0.2, 2, -1.2), (1, 18, 1), "edge", inflate=0.0, share="fbe" + tag)
    A.spike("root", (side * 10.2, 2, 0), "-y", 3, 1.6, "edge", layers=2, share="tip" + tag)
    for k in range(4):                                                                        # serrations on the blade's inner edge
        c("root", (bx + (1.6 if side > 0 else -1.0), 2 + k * 5, -0.8), (1, 2, 1), "blade", share="se%d" % (k % 2))
    return A.save()


def leg(side, tag):
    A = Atlas(KEY, "leg_" + tag, 128, 128, seed=55 + (side > 0))
    A.bone("root", pivot=(side * 2, 12, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    c("root", (o(-4.4, 5), 0, -3.4), (5, 3, 6), "hide", inflate=0.3)                          # boot
    A.blade("root", (side * 2 - 0.5, 0.4, -4.6), 4, 2, 1, "blade", axis="-z", layers=3, taper=0.8, share="toe")
    c("root", (o(-4.4, 5), 3, -2.5), (5, 6, 5), "steel", inflate=0.3)                         # greave
    A.blade("root", (side * 2 - 0.5, 3, 2.8), 7, 2, 1, "blade", axis="+z", layers=3, taper=0.8, share="fin" + tag)   # the back fin
    A.spike("root", (side * 2, 9.0, -3.0), "-z", 3, 2, "blade", layers=3, share="kn" + tag)
    c("root", (o(-4.5, 5), 9.6, -2.6), (5, 2, 5), "hide", inflate=0.5)
    c("root", (o(-2.6, 1), 6, -3.3), (1, 3, 1), "scar")
    return A.save()


def back():
    A = Atlas(KEY, "back", 128, 128, seed=57)
    A.bone("root", pivot=(0, 20, 0))
    A.bone("cape", "root", (0, 24, 2.5))
    for k in range(5):
        x = (k - 2) * 2.4
        A.spike("cape", (x, 22, 3.0), "-y", 12 - abs(k - 2) * 2, 2.2, "hide", layers=4, share="cp%d" % abs(k - 2))
    A.cube("cape", (-5, 22.4, 2.6), (10, 2, 2), "steel", inflate=0.3)
    # the Death Scythe slung across the back: shaft on a diagonal, the great blade at the top
    A.bone("scythe", "root", (0, 18, 5.5), rotation=(0, 0, 38))
    A.cube("scythe", (-1, 0, 4.5), (2, 40, 2), "hide", share="shaft")
    A.cube("scythe", (-1.4, 12, 4.1), (2.8, 3, 2.8), "steel", share="grip")
    A.cube("scythe", (-1.4, 26, 4.1), (2.8, 3, 2.8), "steel", share="grip2")
    A.bone("sblade", "scythe", (0, 40, 5.5))
    arc_blade(A, "sblade", 40, 17, -70, 70, 11, 5, 1, "blade", "edge", z0=5.5 + 17 - 4, share="sc")
    A.cube("sblade", (-2, 39, 3.5), (4, 3, 4), "steel", share="boss")
    # six blade shards orbiting the back
    for k in range(6):
        A.bone("orbit%d" % k, "root", (0, 19, 4))
        a = k * 60.0
        A.bone("sh%d" % k, "orbit%d" % k, (round(11 * math.cos(math.radians(a)), 1), 19 + (k % 3) * 3 - 3, round(4 + 11 * math.sin(math.radians(a)), 1)))
        A.blade("sh%d" % k, (round(11 * math.cos(math.radians(a)) - 0.5, 1), 19 + (k % 3) * 3 - 3, round(4 + 11 * math.sin(math.radians(a)), 1)), 6, 2, 1, "blade", axis="+y", layers=3, taper=0.7, share="oshard")
    a = A.anim
    a.clip("idle", length=8.0, loop=True)
    a.rot("idle", "cape", loop(8.0, lambda p: (4 * math.sin(p * 2 * math.pi), 0, 2 * math.sin(p * 4 * math.pi))))
    a.rot("idle", "scythe", loop(8.0, lambda p: (0, 0, 1.5 * math.sin(p * 2 * math.pi))))
    for k in range(6):
        a.rot("idle", "orbit%d" % k, spin(8.0, 1 + k % 2, 1, 1 if k % 2 == 0 else -1))
    return A.save()


def halo():
    A = Atlas(KEY, "halo", 128, 128, seed=58)
    A.bone("root", pivot=(0, 30, 6))
    A.bone("saw", "root", (0, 30, 6))
    n = 10
    for k in range(n):
        a = 360.0 * k / n
        c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
        A.box_c("saw", (7.0 * c, 30 + 7.0 * s, 6.5), (1.6, 3.5, 1), "steel", rot=(0, 0, a - 90), share="sawb")
        # a blade tooth pointing outwards: a slab turned to the radial direction
        A.box_c("saw", (10.2 * c, 30 + 10.2 * s, 6.5), (1.4, 4.5, 1), "blade", rot=(0, 0, a - 90), share="sawo")
    A.ring("saw", (0, 30, 6.5), "z", 6.6, 1, 1, "edge", n=12, share="sring")
    a = A.anim
    a.clip("idle", length=6.0, loop=True)
    a.rot("idle", "saw", spin(6.0, 2, 2, 1))
    a.pos("idle", "root", loop(6.0, lambda p: (0, 0.5 * math.sin(p * 2 * math.pi), 0)))
    return A.save()


# ====================================================================================================================
def crescent():
    A = Atlas(KEY, "crescent", 128, 128, seed=61)
    A.bone("root", pivot=(0, 16, 0))
    A.bone("tilt", "root", (0, 16, 0), rotation=(0, 0, 18))
    A.bone("arc", "tilt", (0, 16, 0))
    arc_blade(A, "arc", 16, 26, -80, 80, 18, 9, 2, "blade", "edge", z0=22, share="cr")
    # the jagged back edge: spikes along the inside of the crescent
    for k in range(9):
        t = (k + 0.5) / 9
        th = math.radians(-70 + 140 * t)
        r = 26 - 9 * math.sin(math.pi * t) ** 0.8 - 0.5
        A.cube("arc", (round(r * math.sin(th) - 0.5, 1), 15.5, round(22 - r * math.cos(th) + 0.5, 1)), (1, 1, 3), "steel", rot=(0, -math.degrees(th), 0), share="jag")
    A.bone("glint", "tilt", (0, 16, -4))
    A.cube("glint", (-1, 15, -6), (2, 2, 2), "edge")
    a = A.anim
    a.clip("idle", length=2.0, loop=True)
    a.scale("idle", "arc", loop(2.0, lambda p: (1, 1 + 0.06 * math.sin(p * 4 * math.pi), 1)))
    a.clip("fly", length=0.5, loop=True)
    a.rot("fly", "tilt", loop(0.5, lambda p: (0, 0, 18 + 360 * p)))
    a.scale("fly", "arc", loop(0.5, lambda p: (1, 1 + 0.1 * math.sin(p * 4 * math.pi), 1)))
    return A.save()


def scythe():
    A = Atlas(KEY, "scythe", 128, 128, seed=62)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("sweep", "root", (0, 0, 0))
    A.cube("sweep", (-1, 4, -24), (2, 46, 2), "hide", share="shaft")
    A.cube("sweep", (-1.5, 20, -24.5), (3, 4, 3), "steel", share="grip")
    A.cube("sweep", (-1.5, 6, -24.5), (3, 4, 3), "steel", share="grip")
    A.cube("sweep", (-3, 48, -26), (6, 4, 6), "steel", share="boss")
    arc_blade(A, "sweep", 51, 30, -20, 120, 20, 9, 2, "blade", "edge", z0=-24 + 30, share="sc")
    for k in range(4):
        A.cube("sweep", (-0.5, 8 + k * 9, -26), (1, 1, 2), "edge", share="rune")
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    a.rot("idle", "sweep", loop(3.0, lambda p: (0, 4 * math.sin(p * 2 * math.pi), 0)))
    a.clip("sweep", length=0.6, loop=False)
    a.rot("sweep", "sweep", {0.0: (0, 0, 0), 0.6: (0, 360, 0)})
    return A.save()


def ghost():
    A = Atlas(KEY, "ghost", 128, 128, seed=63)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("lean", "root", (0, 12, 0), rotation=(-18, 0, 0))
    c = A.cube
    c("lean", (-4, 24, -4), (8, 8, 8), "glass", inflate=0.2)
    c("lean", (-4, 12, -2), (8, 12, 4), "glass", inflate=0.2)
    c("lean", (-1, 28.4, -4.6), (2, 1, 1), "edge")
    c("lean", (-2.2, 16.5, -2.6), (4, 4, 1), "edge")
    for s in (-1, 1):
        c("lean", (s * 5.5 - 1.5 + (0 if s > 0 else 0), 12, -2), (3, 12, 4), "glass", inflate=0.2, rot=(-35, 0, s * 8), share="ga")
        A.blade("lean", (s * 7.0 - 0.5, 16, -4), 18, 3, 1, "edge", axis="-z", layers=6, taper=0.6, share="gb")
    A.bone("lleg", "root", (-2, 12, 0), rotation=(24, 0, 0))
    A.bone("rleg", "root", (2, 12, 0), rotation=(-30, 0, 0))
    c("lleg", (-4, 0, -2), (4, 12, 4), "glass", inflate=0.1)
    c("rleg", (0, 0, -2), (4, 12, 4), "glass", inflate=0.1)
    A.bone("trail", "root", (0, 12, 4))
    for k in range(5):
        c("trail", (-3 + k * 0.3, 10 + k * 2.4, 4 + k * 3), (6 - k, 3, 5), "glass", share="tr%d" % k)
    a = A.anim
    a.clip("idle", length=1.5, loop=True)
    a.scale("idle", "trail", loop(1.5, lambda p: (1, 1, 1 + 0.2 * math.sin(p * 2 * math.pi))))
    return A.save()


if __name__ == "__main__":
    ids = [helm(), chest(), arm(-1, "r"), arm(1, "l"), leg(-1, "r"), leg(1, "l"), back(), halo(), crescent(), scythe(), ghost()]
    check(*ids)

"""Water Magic (rebuild 1.93.0): the temporary armour of the Sea Dragon / Valkyrie Dress (Noelle Silva): crystal-blue scale plate trimmed with pearl, finned pauldrons and
boots, a flaring water skirt, wings of water feathers, a sea-dragon's tail with a fish fin, a halo of droplets and the pearl-and-trident ring behind the back. Pieces are
GeoLite models in whole-body coordinates (tools/water_aura_lib.py); the painter is client/aura/WaterAura.java:

    python3 -B tools/gen_water_aura_geo.py && python3 -B tools/geo_check.py water_arm_helm ...
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y, feather_wing, tail_chain, shell_texture  # noqa: E402

KEY = "water"

AQUA = Mat("scales", "2F7BDB", dark="0F3A82", light="9ADFFF", glow="5CE6FF", glow_amount=0.55, alpha=232, cells=4, seed=31)
AQUA2 = Mat("scales", "2A6FCB", dark="0C2F6E", light="86D2FF", glow="4CD8FF", glow_amount=0.45, alpha=232, cells=3, seed=32)
CRYSTAL = Mat("crystal", "4FA6F0", dark="1B5BB5", light="D6F4FF", glow="8CF0FF", glow_amount=0.8, alpha=205, cells=5, seed=33)
PEARL = Mat("metal", "DCEBFF", dark="7A94B8", light="FFFFFF", glow="C8F6FF", glow_amount=0.6, seam=1, seed=34)
PEARL_P = Mat("plate", "E4F0FF", dark="8098BC", light="FFFFFF", glow="CFF8FF", glow_amount=0.5, inlay=1, seed=35)
DEEP = Mat("scales", "1B5A9C", dark="082B58", light="5CB4F0", glow="38C8F0", glow_amount=0.4, alpha=240, cells=3, seed=36)
FEATHER = Mat("feather", "3C9BEF", dark="1B58B8", light="E0FAFF", glow="7CEFFF", glow_amount=1.0, alpha=200, seed=37)
WATER = Mat("water", "4AA8F4", dark="1F62C0", light="E8FCFF", glow="9CF4FF", glow_amount=0.9, alpha=190, seed=38)
TEAL = Mat("crystal", "20A0B0", dark="0B5868", light="9CF8FF", glow="5CF0FF", glow_amount=0.7, alpha=215, cells=4, seed=39)


def helm():
    p = Piece(KEY, "arm_helm", density=3)
    p.box("root", (-4.75, 29.2, -4.75), (9.5, 1.8, 0.8), PEARL_P)             # the circlet
    p.box("root", (-4.75, 29.2, 3.95), (9.5, 1.8, 0.8), PEARL_P)
    p.box("root", (-4.75, 29.2, -3.95), (0.8, 1.8, 7.9), PEARL_P)
    p.box("root", (3.95, 29.2, -3.95), (0.8, 1.8, 7.9), PEARL_P)
    p.box("root", (-0.7, 31.0, -4.2), (1.4, 5.5, 1.0), AQUA)                  # the fin crest: a tall fin at the brow and two behind it
    p.box("root", (-0.6, 31.0, -1.6), (1.2, 4.2, 1.0), AQUA2)
    p.box("root", (-0.5, 31.0, 1.0), (1.0, 3.0, 1.0), AQUA2)
    for sx in (-1, 1):                                                       # the swept-back temple fins
        p.box("root", (sx * 4.9 - 0.4, 28.2, -1.0), (0.8, 3.8, 5.5), CRYSTAL, rot=(0, sx * -18, 0), pivot=(sx * 4.9, 29.5, -1.0))
    p.box("root", (-0.9, 30.4, -4.95), (1.8, 1.8, 0.7), CRYSTAL, rot=(0, 0, 45), pivot=(0, 31.3, -4.8))     # the brow gem
    p.save()
    return p


def chest():
    p = Piece(KEY, "arm_chest", density=3)
    p.box("root", (-4.6, 16.2, -2.75), (9.2, 7.8, 1.1), AQUA)                 # cuirass of scales
    p.box("root", (-4.4, 14.4, -2.65), (8.8, 1.9, 0.95), PEARL_P)
    p.box("root", (-4.25, 12.4, -2.5), (8.5, 1.9, 0.85), AQUA2)
    p.box("root", (-1.6, 19.0, -3.25), (3.2, 3.2, 0.6), PEARL, rot=(0, 0, 45), pivot=(0, 20.6, -3.0))     # the shell on the chest
    p.box("root", (-0.9, 19.7, -3.7), (1.8, 1.8, 0.6), CRYSTAL, rot=(0, 0, 45), pivot=(0, 20.6, -3.0))
    p.box("root", (-4.8, 23.4, -2.9), (9.6, 1.9, 5.8), PEARL)                  # the collar
    p.box("root", (-4.7, 12.2, 2.05), (9.4, 11.2, 0.95), AQUA2)                # the back plate
    p.box("root", (-0.6, 14.0, 3.0), (1.2, 8.0, 0.5), CRYSTAL)                 # a crystal spine
    mbox(p, "root", 0, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), DEEP)
    mbox(p, "root", 1, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), DEEP)
    p.save()
    return p


def skirt():
    """The Valkyrie skirt: plates of crystal flaring out from the belt, swaying."""
    p = Piece(KEY, "arm_skirt", density=3)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    p.box("root", (-4.7, 11.8, -2.65), (9.4, 1.4, 5.3), PEARL_P)
    plates = [("p0", -2.9, -3.2, 0), ("p1", 0.0, -3.4, 0), ("p2", 2.9, -3.2, 0), ("p3", -4.6, 0.0, 1), ("p4", 4.6, 0.0, 1), ("p5", -2.2, 3.2, 2), ("p6", 2.2, 3.2, 2)]
    for i, (n, cx, cz, kind) in enumerate(plates):
        p.bone(n, (cx, 11.6, cz))
        if kind == 0:
            p.box(n, (cx - 1.5, 7.2, cz - 0.5), (3.0, 4.6, 1.0), CRYSTAL if i % 2 else AQUA, rot=(-14, 0, 0), pivot=(cx, 11.6, cz))
        elif kind == 1:
            p.box(n, (cx - 0.5, 7.2, cz - 1.5), (1.0, 4.6, 3.0), CRYSTAL, rot=(0, 0, 14 if cx < 0 else -14), pivot=(cx, 11.6, cz))
        else:
            p.box(n, (cx - 1.8, 7.2, cz - 0.5), (3.6, 4.6, 1.0), AQUA2, rot=(14, 0, 0), pivot=(cx, 11.6, cz))
        sine_rot(a, "idle", n, 2.4, 0, 5.0, phase=i * 0.14)
    p.save(a)
    return p


def pauldron(side):
    n = "pauldron_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-9.8, 21.0, -3.6), (5.6, 3.6, 7.2), AQUA)           # a shell-shaped cap
    mbox(p, "root", side, (-9.4, 24.0, -3.2), (4.8, 1.3, 6.4), PEARL_P)
    mbox(p, "root", side, (-10.4, 18.9, -2.9), (1.0, 2.5, 5.8), AQUA2)
    mbox(p, "root", side, (-8.6, 24.9, -0.6), (1.2, 5.0, 1.2), CRYSTAL, rot=(0, 0, -12), pivot=(-8.0, 24.9, 0.0))     # the dragon fin standing up from the shoulder
    mbox(p, "root", side, (-8.4, 24.6, 1.2), (1.0, 3.4, 1.0), CRYSTAL, rot=(0, 0, -12), pivot=(-8.0, 24.6, 1.7))
    p.save()
    return p


def vambrace(side):
    n = "vambrace_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.55, 14.3, -2.6), (4.9, 4.9, 5.2), AQUA)
    mbox(p, "root", side, (-8.75, 18.2, -2.8), (5.3, 1.0, 5.6), PEARL_P)
    bx = -8.7 if side == 0 else 8.7
    stepped_spike(p, "root", (bx, 16.6, 0.4), "-x" if side == 0 else "+x", 3.6, 3.0, CRYSTAL, steps=2, thick=0.9)       # a fin on the outer forearm
    p.save()
    return p


def gauntlet(side):
    n = "gauntlet_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.5, 11.5, -2.6), (4.9, 2.8, 5.2), PEARL_P)
    mbox(p, "root", side, (-8.65, 11.4, -3.0), (5.2, 1.0, 1.0), AQUA)
    for k in range(3):                                                        # webbed fins on the knuckles
        cx = (-8.0 + k * 1.5) if side == 0 else (6.5 - k * 1.5)
        p.box("root", (cx, 8.6, -2.5 + 0.3 * (k % 2)), (1.2, 3.0, 0.7), CRYSTAL)
    p.save()
    return p


def greave(side):
    n = "greave_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.5, 6.4, -2.55), (4.9, 4.8, 5.1), AQUA)
    mbox(p, "root", side, (-4.4, 5.2, -3.1), (4.7, 2.2, 1.1), PEARL_P)
    mbox(p, "root", side, (-4.45, 1.5, -2.55), (4.9, 3.6, 5.1), AQUA2)
    mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.6, 6.0), DEEP)
    mbox(p, "root", side, (-5.3, 1.6, 0.4), (0.9, 4.2, 3.4), CRYSTAL, rot=(0, 0, -8), pivot=(-4.6, 2.0, 1.0))        # the heel fin
    p.save()
    return p


def wing(side):
    n = "wing_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=1.9, loop=True)
    fan = [(-60, 13.0, 3.8), (-42, 16.0, 4.2), (-24, 18.0, 4.4), (-6, 17.5, 4.2), (12, 15.0, 3.8), (30, 11.5, 3.3)]
    feather_wing(p, a, side, (2.4, 21.8, 2.6), fan, FEATHER, arm_mat=PEARL, length=1.9, amp=9.0, yaw=24.0, arm_len=10.0, arm_tilt=40.0)
    p.save(a)
    return p


def tail():
    """The sea dragon's tail: translucent segments and a fish fin at the tip."""
    p = Piece(KEY, "arm_tail", density=3)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    y, z, last = tail_chain(p, a, [AQUA, CRYSTAL], 4, 3.8, (11.0, 2.4), 2.6, taper=0.35, droop=1.3, tilt=12.0, wave=15.0, length=2.0)
    p.bone("fin", (0, y, z), parent=last)
    p.box("fin", (-0.4, y - 3.0, z), (0.8, 6.0, 4.4), CRYSTAL, rot=(18, 0, 0), pivot=(0, y, z))
    p.box("fin", (-3.2, y - 0.5, z + 0.5), (6.4, 0.8, 3.4), FEATHER, rot=(0, 0, 0), pivot=(0, y, z))
    sine_rot(a, "idle", "fin", 2.0, 1, 22.0, phase=-0.55)
    p.save(a)
    return p


def halo():
    p = Piece(KEY, "arm_halo", density=3)
    a = Anim()
    a.clip("idle", length=9.0, loop=True)
    p.bone("spin", (0, 34.5, 0))
    for i in range(8):
        p.box("spin", (-1.5, 34.0, -7.4), (3.0, 0.9, 2.2), PEARL_P, rot=(0, i * 45.0, 0), pivot=(0, 34.5, 0))
    for i in range(4):
        p.box("spin", (-0.8, 35.0, -7.2), (1.6, 2.0, 1.6), CRYSTAL, rot=(0, i * 90.0 + 22.5, 0), pivot=(0, 34.5, 0))      # four droplets riding the ring
    spin_y(a, "idle", "spin", 9.0, turns=1, steps=8)
    p.save(a)
    return p


def backdrop():
    """A pearl ring with a trident of crystal behind the back (the Valkyrie's lance rack), turning slowly."""
    p = Piece(KEY, "arm_backdrop", density=3)
    a = Anim()
    a.clip("idle", length=26.0, loop=True)
    cy, cz = 19.0, 9.0
    p.bone("disc", (0, cy, cz))
    for i in range(8):
        p.box("disc", (-3.7, cy + 8.0, cz), (7.4, 1.3, 0.8), PEARL_P, rot=(0, 0, i * 45.0), pivot=(0, cy, cz))
    for k in range(3):                                                        # the trident's three prongs, standing up the middle
        h = 11.0 if k == 1 else 8.0
        p.box("disc", (-0.7 + (k - 1) * 3.0, cy - 2.0, cz - 0.2), (1.4, h, 0.7), CRYSTAL, pivot=(0, cy, cz))
    p.box("disc", (-3.7, cy - 2.4, cz - 0.2), (7.4, 1.0, 0.7), PEARL)
    a.rot("idle", "disc", {26.0 * i / 8: (0, 0, -360.0 * i / 8) for i in range(9)})
    p.save(a)
    return p


def shells():
    shell_texture("water_shell", Mat("water", "3C9BEF", dark="1F62C0", light="E8FCFF", glow="9CF4FF", glow_amount=0.9, alpha=150, seed=41))
    shell_texture("water_haze", Mat("water", "6CC8FF", dark="2E86E0", light="F0FFFF", glow="BFF8FF", glow_amount=1.0, alpha=255, seed=42))


def main():
    shells()
    made = [helm(), chest(), skirt()]
    for s in (0, 1):
        made += [pauldron(s), vambrace(s), gauntlet(s), greave(s), wing(s)]
    made += [tail(), halo(), backdrop()]
    total = 0
    for m in made:
        print("%-28s cubes %3d" % (m.id, m.cube_count()))
        total += m.cube_count()
    print("total cubes", total)


if __name__ == "__main__":
    main()

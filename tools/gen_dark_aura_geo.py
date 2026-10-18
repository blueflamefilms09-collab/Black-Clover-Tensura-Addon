"""Dark Magic (rebuild 1.93.0): the temporary armour of the Dark Cloaked style (Yami Sukehiro): obsidian plate with violet inlay, a horned hood with the face left open, a tattered
cape, spiked pauldrons and clawed gauntlets, wings of tattered shadow, tendrils of darkness at the waist, a black moon turning over the head, two crossed katanas and the
black hole disc behind the back. Pieces are GeoLite models in whole-body coordinates (tools/water_aura_lib.py); the painter is client/aura/DarkAura.java:

    python3 -B tools/gen_dark_aura_geo.py && python3 -B tools/geo_check.py dark_arm_helm ...
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y, feather_wing, tail_chain, shell_texture  # noqa: E402

KEY = "dark"

OBS = Mat("plate", "1E1730", dark="07040E", light="60488C", glow="A24CFF", glow_amount=0.8, inlay=1, seed=51)
OBS2 = Mat("plate", "181226", dark="05030A", light="4E3A74", glow="8A3AE8", glow_amount=0.6, seed=52)
BONE = Mat("metal", "C4B8E0", dark="5E4F86", light="FFFFFF", glow="C8A0FF", glow_amount=0.6, seam=1, seed=53)
VOID = Mat("wisp", "0A0612", dark="020104", light="3A2A60", glow="B060FF", glow_amount=1.0, seed=54)
CLOTH = Mat("cloth", "171028", dark="05030A", light="4A3A74", glow="9A44F0", glow_amount=0.9, ragged=1, trim=1, seed=55)
SHADOW = Mat("feather", "241A3C", dark="07040E", light="7A5CB8", glow="B070FF", glow_amount=1.0, seed=56, alpha=235)
BLADE = Mat("metal", "9AA2B8", dark="2C3044", light="F4F8FF", glow="C0A0FF", glow_amount=0.5, seam=1, seed=57)
VIOLET = Mat("wisp", "7A36D0", dark="3A1478", light="E0B8FF", glow="D090FF", glow_amount=1.0, seed=58)


def helm():
    p = Piece(KEY, "arm_helm", density=3)
    p.box("root", (-4.9, 31.3, -4.9), (9.8, 1.4, 9.8), OBS)                  # the hood: top, back, sides; the face stays open
    p.box("root", (-4.9, 24.4, 3.9), (9.8, 7.6, 1.2), CLOTH)
    p.box("root", (-5.2, 24.4, -3.9), (1.2, 7.6, 7.8), OBS2)
    p.box("root", (4.0, 24.4, -3.9), (1.2, 7.6, 7.8), OBS2)
    p.box("root", (-4.9, 29.6, -5.0), (9.8, 1.6, 1.0), BONE)                  # the brow band
    for sx in (-1, 1):                                                       # the horns: up, then out
        stepped_spike(p, "root", (sx * 3.4, 32.7, -1.0), "+y", 4.6, 1.9, OBS, steps=2, thick=1.6)
        p.box("root", (sx * 3.4 - 0.5 + sx * 0.6, 36.6, -1.4), (1.0, 2.8, 1.0), VIOLET, rot=(0, 0, -sx * 28), pivot=(sx * 3.4, 36.6, -1.0))
    p.save()
    return p


def chest():
    p = Piece(KEY, "arm_chest", density=3)
    p.box("root", (-4.6, 16.4, -2.75), (9.2, 7.6, 1.1), OBS)
    p.box("root", (-4.4, 14.5, -2.65), (8.8, 1.9, 0.95), OBS2)
    p.box("root", (-4.25, 12.5, -2.5), (8.5, 1.9, 0.85), OBS)
    p.box("root", (-1.2, 18.6, -3.25), (2.4, 4.6, 0.6), VIOLET)                # a slit of the void down the breastplate
    p.box("root", (-4.9, 23.4, -2.9), (9.8, 3.6, 5.8), CLOTH)                  # the high collar of the cloak
    p.box("root", (-4.7, 12.2, 2.05), (9.4, 11.2, 0.95), OBS2)
    p.box("root", (-0.5, 14.0, 3.0), (1.0, 8.0, 0.5), VIOLET)
    mbox(p, "root", 0, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), OBS2)
    mbox(p, "root", 1, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), OBS2)
    p.save()
    return p


def cape():
    p = Piece(KEY, "arm_cape", density=2)
    a = Anim()
    a.clip("idle", length=2.2, loop=True)
    p.box("root", (-4.6, 22.2, 2.6), (9.2, 1.8, 1.4), OBS2)                    # the yoke
    for i, cx in enumerate((-3.8, -1.9, 0.0, 1.9, 3.8)):
        n = "c%d" % i
        p.bone(n, (cx, 23.0, 3.2))
        p.box(n, (cx - 1.05, 5.0 + (i % 2) * 1.5, 3.2), (2.1, 17.5 - (i % 2) * 1.5, 0.8), CLOTH)
        sine_rot(a, "idle", n, 2.2, 0, 7.0, phase=i * 0.13)
    p.save(a)
    return p


def pauldron(side):
    n = "pauldron_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-9.8, 21.0, -3.6), (5.6, 3.6, 7.2), OBS)
    mbox(p, "root", side, (-9.3, 24.0, -3.2), (4.7, 1.3, 6.4), OBS2)
    mbox(p, "root", side, (-10.3, 18.9, -2.9), (1.0, 2.5, 5.8), OBS2)
    for z in (-1.9, 1.9):
        bx = -7.3 if side == 0 else 7.3
        stepped_spike(p, "root", (bx, 25.3, z), "+y", 4.4, 1.8, OBS, steps=2, thick=1.4)
    mbox(p, "root", side, (-10.9, 20.4, -0.8), (0.8, 3.4, 1.6), VIOLET)
    p.save()
    return p


def vambrace(side):
    n = "vambrace_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.55, 14.3, -2.6), (4.9, 4.9, 5.2), OBS)
    mbox(p, "root", side, (-8.75, 18.2, -2.8), (5.3, 1.0, 5.6), BONE)
    bx = -8.7 if side == 0 else 8.7
    stepped_spike(p, "root", (bx, 16.6, 0.0), "-x" if side == 0 else "+x", 3.4, 2.4, OBS, steps=2, thick=0.9)
    p.save()
    return p


def gauntlet(side):
    n = "gauntlet_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.5, 11.5, -2.6), (4.9, 2.8, 5.2), OBS2)
    mbox(p, "root", side, (-8.65, 11.4, -3.0), (5.2, 1.0, 1.0), BONE)
    for k in range(4):
        cx = (-8.0 + k * 1.05) if side == 0 else (6.95 - k * 1.05)
        p.box("root", (cx, 7.4, -2.4 + 0.25 * (k % 2)), (0.95, 4.4, 0.8), VIOLET if k % 2 else BONE)
    p.save()
    return p


def greave(side):
    n = "greave_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.5, 6.4, -2.55), (4.9, 4.8, 5.1), OBS)
    mbox(p, "root", side, (-4.4, 5.2, -3.1), (4.7, 2.2, 1.1), BONE)
    sx = -2.0 if side == 0 else 2.0
    stepped_spike(p, "root", (sx, 6.3, -3.1), "-z", 3.0, 2.0, OBS, steps=2, thick=1.6)
    mbox(p, "root", side, (-4.45, 1.5, -2.55), (4.9, 3.6, 5.1), OBS2)
    mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.6, 6.0), OBS)
    mbox(p, "root", side, (-4.6, 1.3, -2.75), (5.2, 0.7, 5.5), VIOLET)
    p.save()
    return p


def wing(side):
    n = "wing_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    fan = [(-58, 17.0, 3.2), (-36, 21.0, 3.6), (-14, 22.0, 3.6), (8, 19.0, 3.2), (28, 14.0, 2.8)]
    feather_wing(p, a, side, (2.4, 21.8, 2.6), fan, SHADOW, arm_mat=OBS, length=2.4, amp=7.0, yaw=26.0, arm_len=12.0, arm_tilt=42.0)
    p.save(a)
    return p


def tendrils():
    """Three tendrils of darkness at the waist, reaching down and back."""
    p = Piece(KEY, "arm_tendrils", density=3)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    for k, (x, ph) in enumerate(((-3.0, 0.0), (0.0, 0.33), (3.0, 0.66))):
        tail_chain(p, a, [VOID, VIOLET], 3, 4.2, (11.0, 2.4), 2.0, taper=0.4, droop=2.4, tilt=22.0, wave=18.0, length=2.0, prefix="t%d_" % k, x=x, phase0=ph)
    p.save(a)
    return p


def halo():
    """The black moon: a dark crescent ring over the head, slowly turning."""
    p = Piece(KEY, "arm_halo", density=3)
    a = Anim()
    a.clip("idle", length=12.0, loop=True)
    p.bone("spin", (0, 34.8, 0))
    for i in range(8):
        p.box("spin", (-1.6, 34.3, -7.6), (3.2, 1.0, 2.4), OBS, rot=(0, i * 45.0, 0), pivot=(0, 34.8, 0))
    for i in range(8):
        p.box("spin", (-1.2, 35.4, -7.0), (2.4, 0.5, 0.7), VIOLET, rot=(0, i * 45.0 + 22.5, 0), pivot=(0, 34.8, 0))
    spin_y(a, "idle", "spin", 12.0, turns=-1, steps=8)
    p.save(a)
    return p


def blades():
    """Two katanas crossed on the back (Yami's blades)."""
    p = Piece(KEY, "arm_blades", density=3)
    cx, cy, cz = 0.0, 19.0, 3.8
    for sg in (-1, 1):
        rot = (0, 0, sg * 38)
        p.box("root", (cx - 0.6, cy - 12.0, cz), (1.2, 5.0, 1.2), OBS, rot=rot, pivot=(cx, cy, cz))             # the grip, low
        p.box("root", (cx - 1.6, cy - 7.0, cz - 0.2), (3.2, 0.7, 1.6), BONE, rot=rot, pivot=(cx, cy, cz))        # the guard
        p.box("root", (cx - 0.45, cy - 6.3, cz + 0.1), (0.9, 19.0, 1.0), BLADE, rot=rot, pivot=(cx, cy, cz))      # the blade
    p.save()
    return p


def backdrop():
    """The black hole behind the back: a dark disc ringed by a violet accretion ring, turning."""
    p = Piece(KEY, "arm_backdrop", density=3)
    a = Anim()
    a.clip("idle", length=16.0, loop=True)
    cy, cz = 19.0, 12.0
    p.bone("disc", (0, cy, cz))
    p.box("disc", (-4.6, cy - 4.6, cz), (9.2, 9.2, 0.8), VOID, rot=(0, 0, 45), pivot=(0, cy, cz))
    for i in range(8):
        p.box("disc", (-2.9, cy + 6.6, cz + 0.2), (5.8, 1.2, 0.6), VIOLET, rot=(0, 0, i * 45.0), pivot=(0, cy, cz))
    p.box("disc", (-2.2, cy - 2.2, cz - 0.2), (4.4, 4.4, 0.6), OBS2, rot=(0, 0, 45), pivot=(0, cy, cz))
    a.rot("idle", "disc", {16.0 * i / 8: (0, 0, 360.0 * i / 8) for i in range(9)})
    p.save(a)
    return p


def shells():
    shell_texture("dark_shell", Mat("wisp", "120C1E", dark="030106", light="4C3878", glow="A24CFF", glow_amount=1.0, alpha=205, seed=61))
    shell_texture("dark_haze", Mat("wisp", "7A3AD0", dark="2C0E60", light="E0B0FF", glow="C080FF", glow_amount=1.0, alpha=255, seed=62))


def main():
    shells()
    made = [helm(), chest(), cape()]
    for s in (0, 1):
        made += [pauldron(s), vambrace(s), gauntlet(s), greave(s), wing(s)]
    made += [tendrils(), halo(), blades(), backdrop()]
    total = 0
    for m in made:
        print("%-28s cubes %3d" % (m.id, m.cube_count()))
        total += m.cube_count()
    print("total cubes", total)


if __name__ == "__main__":
    main()

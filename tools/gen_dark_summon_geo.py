"""Dark Magic (rebuild 1.93.0): the summons / constructs. Space.PROP models (feet at y = 0, y up, front on -z); drawn by client/prop/DarkPropPainter.java:

    dark_black_blade   Dark Cloaked Black Blade: a long katana of darkness (grip, guard, a violet edge) wrapped in flames of shadow that flicker; idle / slash clips
    dark_crescent      Dark Cloaked Avidya Slash: a flat crescent of black blade thrown flat along the ground, tapering to two points
    dark_black_moon    Dark Cloaked Black Moon: a black moon (a cluster of rotated plates for a sphere) with a violet ring and a corona of spikes
    dark_black_hole    Black Hole: a void core with two accretion rings turning against each other

    python3 -B tools/gen_dark_summon_geo.py && python3 -B tools/geo_check.py dark_black_blade dark_crescent dark_black_moon dark_black_hole
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y  # noqa: E402

KEY = "dark"
OBS = Mat("plate", "1E1730", dark="07040E", light="60488C", glow="A24CFF", glow_amount=0.8, inlay=1, seed=101)
BLADE = Mat("metal", "26202E", dark="05030A", light="6A5A94", glow="B070FF", glow_amount=0.7, seam=1, seed=102)
EDGE = Mat("plate", "C8A8FF", dark="7A44D0", light="FFFFFF", glow="D8B0FF", glow_amount=1.0, seed=103)
BONE = Mat("metal", "C4B8E0", dark="5E4F86", light="FFFFFF", glow="C8A0FF", glow_amount=0.6, seam=1, seed=104)
VOID = Mat("wisp", "0A0612", dark="020104", light="3A2A60", glow="B060FF", glow_amount=1.0, seed=105)
VIOLET = Mat("wisp", "7A36D0", dark="3A1478", light="E0B8FF", glow="D090FF", glow_amount=1.0, seed=106)
FLAME = Mat("cloth", "20163A", dark="07040E", light="7A5CB8", glow="B070FF", glow_amount=1.0, ragged=1, seed=107)


def black_blade():
    p = Piece(KEY, "black_blade", density=3)
    a = Anim()
    a.clip("idle", length=1.6, loop=True)
    a.clip("slash", length=0.9, loop=True)
    cy = 16.0
    p.bone("blade", (0, cy, 8.0))
    p.box("blade", (-1.1, cy - 1.1, 4.0), (2.2, 2.2, 11), OBS)                         # the grip, wrapped
    p.box("blade", (-0.9, cy - 0.9, 14.5), (1.8, 1.8, 1.6), BONE)                      # the pommel
    p.box("blade", (-0.9, cy - 3.2, 2.4), (1.8, 6.4, 1.8), BONE)                       # the guard
    p.box("blade", (-0.8, cy - 2.6, -30), (1.6, 5.2, 32.4), BLADE)                     # the blade
    p.box("blade", (-0.9, cy - 2.7, -30), (1.8, 0.8, 32.4), EDGE)                      # the violet edge
    stepped_spike(p, "blade", (0, cy, -30), "-z", 8.0, 1.6, BLADE, steps=3, thick=5.2)
    for i in range(7):                                                                 # flames of shadow along the blade
        b = "f%d" % i
        z = 0.0 - i * 5.0
        p.bone(b, (0, cy, z), parent="blade")
        side = 1 if i % 2 else -1
        p.box(b, (side * 2.4 - 1.4, cy - 1.0, z - 3.0), (2.8, 1.8, 6.0), FLAME, rot=(0, 0, side * 24), pivot=(0, cy, z))
        p.box(b, (-1.0 + side * 0.5, cy + 2.6, z - 2.5), (2.0, 4.0, 5.0 - 0.3 * i), FLAME)
        sine_rot(a, "idle", b, 1.6, 0, 11.0, phase=i * 0.13)
        sine_rot(a, "slash", b, 0.9, 0, 18.0, phase=i * 0.1)
    sine_rot(a, "idle", "blade", 1.6, 0, 2.0, phase=0.25)
    a.rot("slash", "blade", {0.0: (-55, 0, 0), 0.3: (42, 0, 0), 0.9: (-55, 0, 0)})
    p.save(a)
    return p


def crescent():
    p = Piece(KEY, "crescent", density=3)
    a = Anim()
    a.clip("idle", length=1.0, loop=True)
    cy, R = 16.0, 24.0
    p.bone("c", (0, cy, 0))
    n = 11
    for i in range(n):
        th = (i - (n - 1) / 2) * 15.0
        t = 1.0 - abs(th) / 80.0
        depth = max(1.0, 5.5 * t)
        x = R * math.sin(math.radians(th))
        z = -R * math.cos(math.radians(th)) + R
        p.box("c", (x - 3.6, cy - 0.8, z - depth / 2), (7.2, 1.6, depth), BLADE if i % 2 else OBS, rot=(0, -th, 0), pivot=(x, cy, z))
        p.box("c", (x - 3.6, cy - 0.9, z - depth / 2 - 0.5), (7.2, 0.9, 0.9), EDGE, rot=(0, -th, 0), pivot=(x, cy, z))
    sine_rot(a, "idle", "c", 1.0, 1, 2.0)
    p.save(a)
    return p


def black_moon():
    p = Piece(KEY, "black_moon", density=3)
    a = Anim()
    a.clip("idle", length=14.0, loop=True)
    cy = 24.0
    p.bone("core", (0, cy, 0))
    for rot in ((0, 0, 0), (45, 0, 0), (0, 45, 0), (0, 0, 45), (45, 45, 0), (0, 45, 45)):
        p.box("core", (-9.0, cy - 9.0, -9.0), (18.0, 18.0, 18.0), VOID, rot=rot, pivot=(0, cy, 0))
    p.bone("ring", (0, cy, 0), parent="root")
    for i in range(12):
        p.box("ring", (-3.4, cy + 15.0, -0.7), (6.8, 1.6, 1.4), VIOLET, rot=(0, 0, i * 30.0), pivot=(0, cy, 0))
    p.bone("corona", (0, cy, 0), parent="root")
    for i in range(8):
        p.box("corona", (-1.0, cy + 18.5, -1.0), (2.0, 7.0, 2.0), EDGE, rot=(0, 0, i * 45.0 + 22.5), pivot=(0, cy, 0))
    a.rot("idle", "ring", {14.0 * i / 8: (0, 0, 360.0 * i / 8) for i in range(9)})
    a.rot("idle", "corona", {14.0 * i / 8: (0, 0, -360.0 * i / 8) for i in range(9)})
    a.rot("idle", "core", {14.0 * i / 8: (360.0 * i / 8, 360.0 * i / 8, 0) for i in range(9)})
    p.save(a)
    return p


def black_hole():
    p = Piece(KEY, "black_hole", density=3)
    a = Anim()
    a.clip("idle", length=6.0, loop=True)
    cy = 20.0
    p.bone("core", (0, cy, 0))
    for rot in ((0, 0, 0), (45, 0, 45), (0, 45, 0)):
        p.box("core", (-5.5, cy - 5.5, -5.5), (11.0, 11.0, 11.0), VOID, rot=rot, pivot=(0, cy, 0))
    for k, (r, tilt, mat, n) in enumerate(((13.0, 78, VIOLET, 12), (18.0, 76, BONE, 12))):
        b = "disc%d" % k
        p.bone(b, (0, cy, 0), parent="root", rotation=(tilt, 0, 0))
        for i in range(n):
            p.box(b, (-3.8, cy + r, -0.8), (7.6, 1.8, 1.6), mat, rot=(0, 0, i * 360.0 / n), pivot=(0, cy, 0))
        a.rot("idle", b, {6.0 * i / 8: (tilt, 360.0 * i / 8 * (1 if k == 0 else -1), 0) for i in range(9)})
    p.save(a)
    return p


def main():
    for m in (black_blade(), crescent(), black_moon(), black_hole()):
        print("%-22s cubes %3d" % (m.id, m.cube_count()))


if __name__ == "__main__":
    main()

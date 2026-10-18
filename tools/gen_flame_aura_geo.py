"""Flame Magic (rebuild 1.93.0): the temporary armour pieces of the Crimson Lion flame armour (Fuegoleon / Mereoleona's red-and-gold lion plate, a mane of flame, flame wings,
a lion's tail, a crown of flames and the sun-lion disc behind the back). One GeoLite model per piece, authored in whole-body coordinates (see tools/water_aura_lib.py):

    python3 -B tools/gen_flame_aura_geo.py          # writes geo/entity/flame_arm_*.geo.json, animations, textures + _glow maps
    python3 -B tools/geo_check.py flame_arm_helm ...

The pieces and their anchors are listed in client/aura/FlameAura.java (the same names); the delay / direction numbers there are the Djinn full-equip numbers.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y, loop_rot, shell_texture  # noqa: E402

KEY = "flame"

RED = Mat("plate", "A31E27", dark="4C0B12", light="E65440", glow="FF7A22", glow_amount=0.8, inlay=1, seed=1)
RED2 = Mat("plate", "8E1620", dark="3E080E", light="D8443A", glow="FF6A1E", glow_amount=0.6, seed=2)
GOLD = Mat("metal", "D9A730", dark="6B4609", light="FFEBA3", glow="FFC866", glow_amount=0.55, seam=1, seed=3)
GOLD_P = Mat("plate", "E0AE35", dark="70490A", light="FFF0B0", glow="FFD070", glow_amount=0.6, inlay=1, seed=4)
FLAME = Mat("flame", "FF7C1E", dark="B3200C", light="FFE276", glow="FF9830", glow_amount=0.95, seed=5, grain=0.04)
FLAME_HOT = Mat("flame", "FFA52E", dark="E0480F", light="FFF2A0", glow="FFC24A", glow_amount=1.0, seed=6, grain=0.04)
EMBER = Mat("plate", "3A1210", dark="170605", light="7A2A1A", glow="FF5A18", glow_amount=0.9, seed=7)
FEATHER = Mat("feather", "E8561C", dark="9A1C0C", light="FFD060", glow="FF9A2E", glow_amount=0.95, seed=8)
MANE = Mat("mane", "FF8420", dark="B02A0C", light="FFE070", glow="FFA23A", glow_amount=1.0, seed=9)


def helm():
    p = Piece(KEY, "arm_helm", density=3)
    # the crown band round the brow, the skull cap, the crest ridge: red plate, gold trim (the flames on the crest are sprites drawn by FlameAura)
    p.box("root", (-4.75, 29.0, -4.75), (9.5, 2.0, 0.8), GOLD_P)
    p.box("root", (-4.75, 29.0, 3.95), (9.5, 2.0, 0.8), GOLD_P)
    p.box("root", (-4.75, 29.0, -3.95), (0.8, 2.0, 7.9), GOLD_P)
    p.box("root", (3.95, 29.0, -3.95), (0.8, 2.0, 7.9), GOLD_P)
    p.box("root", (-4.5, 31.0, -4.5), (9.0, 1.4, 9.0), RED)
    p.box("root", (-0.7, 32.3, -4.9), (1.4, 1.2, 9.8), GOLD)
    for sx in (-1, 1):                                                   # lion ears
        p.box("root", (sx * 3.3 - 0.9, 32.3, 0.2), (1.8, 1.9, 1.2), RED)
    mbox(p, "root", 0, (-5.05, 24.2, -2.8), (1.0, 4.8, 6.2), RED)          # cheek guards
    mbox(p, "root", 1, (-5.05, 24.2, -2.8), (1.0, 4.8, 6.2), RED)
    p.box("root", (-0.6, 25.5, -4.95), (1.2, 4.2, 0.8), GOLD)              # nose guard
    for sx in (-1, 1):                                                   # brow fangs
        stepped_spike(p, "root", (sx * 3.0, 29.0, -4.9), "-y", 2.4, 1.2, GOLD, steps=2, thick=0.8)
    p.save()
    return p


def chest():
    p = Piece(KEY, "arm_chest", density=3)
    p.box("root", (-4.6, 16.4, -2.75), (9.2, 7.6, 1.1), RED)                # breastplate
    p.box("root", (-4.45, 14.7, -2.65), (8.9, 1.7, 0.95), GOLD_P)            # the lames of the belly
    p.box("root", (-4.3, 12.9, -2.55), (8.6, 1.7, 0.85), RED2)
    p.box("root", (-1.5, 19.0, -3.2), (3.0, 3.0, 0.6), GOLD, rot=(0, 0, 45), pivot=(0, 20.5, -3.0))   # the lion's sun on the chest
    p.box("root", (-4.8, 23.4, -2.9), (9.6, 1.9, 5.8), GOLD)                 # the gorget
    p.box("root", (-4.7, 12.2, 2.05), (9.4, 11.2, 0.95), RED)                # back plate
    p.box("root", (-0.5, 14.0, 3.0), (1.0, 8.0, 0.5), FLAME_HOT)             # the ember vent
    mbox(p, "root", 0, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), RED2)          # side guards
    mbox(p, "root", 1, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), RED2)
    p.save()
    return p


def skirt():
    p = Piece(KEY, "arm_skirt", density=2)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    p.box("root", (-4.7, 11.8, -2.65), (9.4, 1.4, 5.3), GOLD_P)              # the belt
    for i, cx in enumerate((-2.9, 0.0, 2.9)):                             # front tassets, hanging from the belt
        n = "f%d" % i
        p.bone(n, (cx, 11.8, -3.0))
        p.box(n, (cx - 1.4, 8.6, -3.4), (2.8, 3.3, 0.9), RED if i % 2 == 0 else RED2)
        sine_rot(a, "idle", n, 2.0, 0, 6.0, phase=i * 0.2)
    p.bone("b0", (0, 11.8, 2.9))
    p.box("b0", (-3.2, 8.6, 2.55), (6.4, 3.4, 0.9), RED2)
    sine_rot(a, "idle", "b0", 2.0, 0, -6.0, phase=0.4)
    for sx in (-1, 1):
        p.box("root", (sx * 4.2 - 0.5, 8.9, -1.8), (1.0, 3.0, 3.6), RED)
    p.save(a)
    return p


def pauldron(side):
    n = "pauldron_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-9.8, 21.0, -3.6), (5.6, 3.6, 7.2), RED)
    mbox(p, "root", side, (-9.2, 24.0, -3.2), (4.6, 1.3, 6.4), GOLD_P)
    mbox(p, "root", side, (-10.3, 18.8, -2.9), (1.0, 2.6, 5.8), RED2)
    mbox(p, "root", side, (-10.5, 21.4, -0.9), (0.9, 2.0, 1.8), GOLD)                       # the lion's nose
    for dz in (-2.1, 2.1):                                                               # fangs
        mbox(p, "root", side, (-10.6, 19.4, dz - 0.5), (0.8, 1.9, 1.0), GOLD)
    for z in (-1.9, 1.9):                                                                # horn-spikes on top (the flames on them are sprites)
        bx = -7.0 if side == 0 else 7.0
        stepped_spike(p, "root", (bx, 25.3, z), "+y", 3.0, 1.8, GOLD, steps=2, thick=1.4)
    p.save()
    return p


def vambrace(side):
    n = "vambrace_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.55, 14.3, -2.6), (4.9, 4.9, 5.2), RED)
    mbox(p, "root", side, (-8.75, 18.2, -2.8), (5.3, 1.0, 5.6), GOLD_P)
    bx = -8.7 if side == 0 else 8.7
    stepped_spike(p, "root", (bx, 16.6, 0.0), "-x" if side == 0 else "+x", 3.2, 2.4, GOLD, steps=2, thick=0.9)
    p.save()
    return p


def gauntlet(side):
    n = "gauntlet_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.5, 11.5, -2.6), (4.9, 2.8, 5.2), RED2)
    mbox(p, "root", side, (-8.65, 11.4, -3.0), (5.2, 1.0, 1.0), GOLD_P)
    for k in range(4):                                                                   # four claws, one tapering cube each
        cx = (-8.0 + k * 1.05) if side == 0 else (6.95 - k * 1.05)
        p.box("root", (cx, 8.2, -2.4 + 0.25 * (k % 2)), (0.95, 3.4, 0.8), GOLD_P if k % 2 else GOLD)
    p.save()
    return p


def greave(side):
    n = "greave_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.5, 6.4, -2.55), (4.9, 4.8, 5.1), RED)                       # thigh guard
    mbox(p, "root", side, (-4.4, 5.2, -3.1), (4.7, 2.2, 1.1), GOLD_P)                    # knee
    sx = -2.0 if side == 0 else 2.0
    stepped_spike(p, "root", (sx, 6.3, -3.1), "-z", 3.0, 2.0, GOLD, steps=2, thick=1.6)
    mbox(p, "root", side, (-4.45, 1.5, -2.55), (4.9, 3.6, 5.1), RED2)                     # shin
    mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.6, 6.0), RED)                       # boot
    mbox(p, "root", side, (-4.6, 1.3, -2.75), (5.2, 0.8, 5.5), GOLD)                      # flame cuff
    p.save()
    return p


def wing(side):
    n = "wing_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=1.2, loop=True)
    sg = 1.0 if side else -1.0
    P = (sg * 2.4, 21.6, 2.6)
    p.bone("w", P)
    sine_rot(a, "idle", "w", 1.2, 2, -10.0 * sg, 0.0)
    sine_rot(a, "idle", "w", 1.2, 1, 6.0 * sg, 0.15)
    fan = [(-46, 12.5, 3.4), (-26, 15.0, 3.8), (-8, 16.0, 3.8), (12, 14.0, 3.6), (30, 10.5, 3.2)]
    for i, (rz, L, w) in enumerate(fan):
        b = "f%d" % i
        p.bone(b, P, parent="w", rotation=(0, sg * (20 + i * 3), -sg * rz))
        o = (P[0] - L, P[1] - w / 2, P[2]) if side == 0 else (P[0], P[1] - w / 2, P[2])
        p.box(b, o, (L, w, 0.5), FEATHER)
        sine_rot(a, "idle", b, 1.2, 2, 5.0 * sg, 0.22 + i * 0.05)
    p.box("w", (P[0] - (9.0 if side == 0 else 0.0), P[1] - 0.6, P[2] - 0.2), (9.0, 1.2, 1.0), GOLD, rot=(0, 0, sg * 36), pivot=P)   # the wing's arm along the leading edge
    p.save(a)
    return p


def tail():
    p = Piece(KEY, "arm_tail", density=3)
    a = Anim()
    a.clip("idle", length=1.8, loop=True)
    z = 2.4
    y = 11.0
    parent = "root"
    for i in range(4):
        b = "t%d" % i
        p.bone(b, (0, y, z), parent=parent)
        w = 2.4 - i * 0.3
        p.box(b, (-w / 2, y - w / 2, z), (w, w, 3.6), RED2 if i % 2 else RED, rot=(14, 0, 0), pivot=(0, y, z))
        sine_rot(a, "idle", b, 1.8, 1, 14.0, phase=-i * 0.11)
        parent = b
        z += 3.4
        y -= 1.5
    p.bone("tuft", (0, y, z), parent=parent)
    for k, ang in enumerate((-28, 28)):
        p.box("tuft", (-1.1, y - 3.6, z), (2.2, 4.4, 1.4), FLAME_HOT if k else FLAME, rot=(0, ang, 0), pivot=(0, y, z))
    sine_rot(a, "idle", "tuft", 1.8, 1, 18.0, phase=-0.6)
    p.save(a)
    return p


def halo():
    p = Piece(KEY, "arm_halo", density=3)
    a = Anim()
    a.clip("idle", length=8.0, loop=True)
    p.bone("spin", (0, 34.5, 0))
    for i in range(8):
        p.box("spin", (-1.5, 34.0, -7.4), (3.0, 0.9, 2.2), GOLD_P, rot=(0, i * 45.0, 0), pivot=(0, 34.5, 0))          # the ring's segments (the flames of the crown are sprites)
    spin_y(a, "idle", "spin", 8.0, turns=1, steps=8)
    p.save(a)
    return p


def backdrop():
    p = Piece(KEY, "arm_backdrop", density=3)
    a = Anim()
    a.clip("idle", length=24.0, loop=True)
    cy, cz = 19.0, 9.0
    p.bone("disc", (0, cy, cz))
    for i in range(8):                                                   # the ring of the sun, eight long plates
        p.box("disc", (-3.7, cy + 8.0, cz), (7.4, 1.3, 0.8), GOLD_P, rot=(0, 0, i * 45.0), pivot=(0, cy, cz))
    p.box("disc", (-3.0, cy - 3.0, cz - 0.1), (6.0, 6.0, 0.8), RED, rot=(0, 0, 45), pivot=(0, cy, cz))        # the lion's face plate at the centre
    p.box("disc", (-1.6, cy - 1.6, cz - 0.4), (3.2, 3.2, 0.5), FLAME_HOT, rot=(0, 0, 45), pivot=(0, cy, cz))
    a.rot("idle", "disc", {24.0 * i / 8: (0, 0, 360.0 * i / 8) for i in range(9)})
    p.save(a)
    return p


def shells():
    # the body overlays of the armour: a dark ember skin with glowing cracks (solid-ish translucent), and a heat haze (additive)
    shell_texture("flame_shell", Mat("crystal", "3A1210", dark="170605", light="FF9A38", glow="FF6A1C", glow_amount=1.0, cells=14, seamonly=True, alpha=235, seed=21))
    shell_texture("flame_heat", Mat("flame", "FF8A26", dark="D0300C", light="FFE680", glow="FFB040", glow_amount=1.0, alpha=255, seed=22))


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

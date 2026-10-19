"""Water Magic (rebuild 1.93.0): the summons / constructs. Space.PROP models (feet at y = 0, y up, front on -z); drawn by client/prop/WaterPropPainter.java:

    water_sea_dragon   the Sea Dragon of Sea Dragon's Roar: a horned head with a frill, nine body segments that swim in a travelling wave, fins down the back, a fish tail
    water_javelin      the Aqua Javelin: a long lance of pressurised water with a spiral of rings turning round the shaft and fins at the tail

    python3 -B tools/gen_water_summon_geo.py && python3 -B tools/geo_check.py water_sea_dragon water_javelin
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y, loop_rot  # noqa: E402

KEY = "water"
AQUA = Mat("scales", "2F7BDB", dark="0F3A82", light="9ADFFF", glow="5CE6FF", glow_amount=0.5, alpha=225, cells=5, seed=91)
AQUA2 = Mat("scales", "2A6FCB", dark="0C2F6E", light="86D2FF", glow="4CD8FF", glow_amount=0.4, alpha=225, cells=4, seed=92)
PEARL = Mat("metal", "DCEBFF", dark="7A94B8", light="FFFFFF", glow="C8F6FF", glow_amount=0.6, seam=1, seed=93)
CRYSTAL = Mat("crystal", "4FA6F0", dark="1B5BB5", light="D6F4FF", glow="8CF0FF", glow_amount=0.9, alpha=215, cells=5, seed=94)
FIN = Mat("feather", "3C9BEF", dark="1B58B8", light="E0FAFF", glow="7CEFFF", glow_amount=1.0, alpha=205, seed=95)
WATER = Mat("water", "4AA8F4", dark="1F62C0", light="E8FCFF", glow="9CF4FF", glow_amount=0.9, alpha=200, seed=96)
EYE = Mat("plate", "FFFFFF", dark="C8F4FF", light="FFFFFF", glow="E8FFFF", glow_amount=1.0, seed=97)


def sea_dragon():
    p = Piece(KEY, "sea_dragon", density=3)
    a = Anim()
    a.clip("swim", length=1.6, loop=True)
    a.clip("idle", length=2.4, loop=True)
    cy = 18.0
    p.bone("head", (0, cy, -22.0))
    # skull, snout, jaw, teeth, horns, eyes, frill
    p.box("head", (-5, cy - 4.5, -34), (10, 9, 12), AQUA)
    p.box("head", (-3, cy - 3.5, -43), (6, 5, 9.5), AQUA2)
    p.bone("jaw", (0, cy - 3.5, -33.0), parent="head")
    p.box("jaw", (-2.6, cy - 6.4, -43), (5.2, 2.4, 10.5), PEARL)
    for k, x in enumerate((-2.0, 1.2)):
        p.box("head", (x, cy - 3.9, -42.5), (0.8, 1.7, 0.8), PEARL)
        p.box("jaw", (x, cy - 4.2, -42.5), (0.8, 1.7, 0.8), PEARL)
    for sx in (0, 1):
        mbox(p, "head", sx, (-3.6, cy + 3.6, -33), (1.5, 1.5, 7.5), PEARL, rot=(-34, 0, 0), pivot=(-3.0, cy + 4.0, -33))      # horns, swept back
        mbox(p, "head", sx, (-5.35, cy - 1.0, -38.6), (0.7, 1.7, 1.7), EYE)                                                 # the eyes
        mbox(p, "head", sx, (-5.4, cy - 4.5, -26), (0.8, 8.5, 7.0), FIN, rot=(0, 26, 0), pivot=(-5.0, cy, -24))             # the frill, two plates a side
        mbox(p, "head", sx, (-5.4, cy - 3.0, -22), (0.8, 6.5, 6.0), FIN, rot=(0, 40, 0), pivot=(-5.0, cy, -22))
    sine_rot(a, "swim", "jaw", 1.6, 0, 7.0, phase=0.2)
    sine_rot(a, "idle", "jaw", 2.4, 0, 4.0)
    sine_rot(a, "swim", "head", 1.6, 1, 11.0, phase=0.0)
    sine_rot(a, "idle", "head", 2.4, 1, 6.0)
    parent = "head"
    n = 9
    for i in range(n):
        z0 = -22.0 + 9.0 * i
        b = "s%d" % i
        p.bone(b, (0, cy, z0), parent=parent)
        w = 12.0 - i * 0.85
        p.box(b, (-w / 2, cy - w / 2, z0), (w, w, 9.8), AQUA if i % 2 == 0 else AQUA2)
        p.box(b, (-w * 0.3, cy - w / 2 - 1.0, z0 + 0.3), (w * 0.6, 1.4, 9.2), PEARL)
        p.box(b, (-0.35, cy + w / 2, z0 + 1.0), (0.7, 6.2 - i * 0.45, 7.4), FIN)
        if i in (2, 5):
            for sx in (0, 1):
                mbox(p, b, sx, (-w / 2 - 5.0, cy - 1.8, z0 + 1.0), (5.2, 0.7, 4.4), FIN, rot=(0, 0, -16), pivot=(-w / 2, cy - 1.5, z0 + 3))
        sine_rot(a, "swim", b, 1.6, 1, 15.0, phase=-(i + 1) * 0.11)
        sine_rot(a, "swim", b, 1.6, 0, 3.0, phase=-(i + 1) * 0.09 + 0.25)
        sine_rot(a, "idle", b, 2.4, 1, 7.0, phase=-(i + 1) * 0.08)
        parent = b
    z_end = -22.0 + 9.0 * n
    p.bone("fin", (0, cy, z_end), parent=parent)
    p.box("fin", (-0.45, cy - 6.0, z_end), (0.9, 12.0, 6.5), FIN)
    p.box("fin", (-5.0, cy - 0.45, z_end + 0.5), (10.0, 0.9, 5.0), FIN)
    sine_rot(a, "swim", "fin", 1.6, 1, 22.0, phase=-0.98)
    sine_rot(a, "idle", "fin", 2.4, 1, 10.0, phase=-0.7)
    p.save(a)
    return p


def javelin():
    p = Piece(KEY, "javelin", density=3)
    a = Anim()
    a.clip("fly", length=0.7, loop=True)
    a.clip("idle", length=1.4, loop=True)
    cy = 16.0
    p.bone("shaft", (0, cy, 0))
    p.box("shaft", (-1.6, cy - 1.6, -10), (3.2, 3.2, 38), WATER)
    p.box("shaft", (-1.0, cy - 1.0, -12), (2.0, 2.0, 3), PEARL)
    stepped_spike(p, "shaft", (0, cy, -12), "-z", 14.0, 4.6, CRYSTAL, steps=4, thick=4.6)         # the point
    for sx in (0, 1):                                                           # tail fins
        mbox(p, "shaft", sx, (-8.5, cy - 0.4, 22), (7.0, 0.8, 8), FIN, rot=(0, 0, -8), pivot=(-1.5, cy, 24))
    p.box("shaft", (-0.4, cy + 1.5, 22), (0.8, 6.0, 8), FIN)
    p.box("shaft", (-0.4, cy - 7.5, 22), (0.8, 6.0, 8), FIN)
    p.bone("rings", (0, cy, 4), parent="shaft")
    for k in range(5):                                                          # the spiral: five rings of four plates, turning
        z = -8 + k * 8.0
        for q in range(4):
            ang = q * 90.0 + k * 31.0
            p.box("rings", (-4.6, cy - 0.5, z), (3.0, 1.0, 1.4), PEARL if q % 2 else CRYSTAL, rot=(0, 0, ang), pivot=(0, cy, z))
    spin_y(a, "fly", "rings", 0.7, turns=0, steps=2)
    a.rot("fly", "rings", {0.0: (0, 0, 0), 0.35: (0, 0, 180), 0.7: (0, 0, 360)})
    a.rot("idle", "rings", {0.0: (0, 0, 0), 0.7: (0, 0, 180), 1.4: (0, 0, 360)})
    p.save(a)
    return p


def main():
    made = [sea_dragon(), javelin()]
    for m in made:
        print("%-24s cubes %3d" % (m.id, m.cube_count()))


if __name__ == "__main__":
    main()

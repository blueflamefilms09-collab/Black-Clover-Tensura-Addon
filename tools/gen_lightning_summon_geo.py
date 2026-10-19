"""Lightning Magic (rebuild 1.93.0): the summons / constructs. Space.PROP models (feet at y = 0, y up, front on -z); drawn by client/prop/LightningPropPainter.java:

    lightning_thunderbird   Thunderbird Cavalry: a bird of white-blue lightning, gold beak, a crest of bolts, wings of flat bolt feathers that beat, a fan of tail feathers; fly / idle clips
    lightning_lance         Thunder Lance: a gold-and-white spear with a barbed head and a coil of electrodes turning round the shaft

    python3 -B tools/gen_lightning_summon_geo.py && python3 -B tools/geo_check.py lightning_thunderbird lightning_lance
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, bird  # noqa: E402

KEY = "lightning"
ENAMEL = Mat("bolt", "E6ECF8", dark="8794B4", light="FFFFFF", glow="6AB4FF", glow_amount=0.9, seed=121)
GOLD = Mat("metal", "E8C050", dark="7A5A10", light="FFF4B0", glow="FFE070", glow_amount=0.6, seam=1, seed=122)
BOLT = Mat("flame", "8CC8FF", dark="2E6CE8", light="F4FCFF", glow="9CD4FF", glow_amount=1.0, seed=123, grain=0.03)
BOLT_W = Mat("plate", "EAF6FF", dark="9CC8FF", light="FFFFFF", glow="C8E8FF", glow_amount=1.0, seed=124)
FEATHER = Mat("feather", "BCDCFF", dark="3A78F0", light="FFFFFF", glow="8CC8FF", glow_amount=1.0, seed=125)
FEATHER2 = Mat("feather", "E8F4FF", dark="7AA8F0", light="FFFFFF", glow="C8E8FF", glow_amount=1.0, seed=126)
EYE = Mat("plate", "FFF6B0", dark="FFD040", light="FFFFFF", glow="FFF0A0", glow_amount=1.0, seed=127)


def thunderbird():
    p = Piece(KEY, "thunderbird", density=3)
    a = Anim()
    bird(p, a, ENAMEL, FEATHER, FEATHER2, GOLD, EYE, BOLT, wing_len=26.0, tail_len=22.0, cy=22.0, flap=0.7, feathers=6, crest_n=3)
    p.save(a)
    return p


def lance():
    p = Piece(KEY, "lance", density=3)
    a = Anim()
    a.clip("fly", length=0.5, loop=True)
    a.clip("idle", length=1.2, loop=True)
    cy = 16.0
    p.bone("shaft", (0, cy, 0))
    p.box("shaft", (-1.3, cy - 1.3, -8), (2.6, 2.6, 40), ENAMEL)
    p.box("shaft", (-1.8, cy - 1.8, 22), (3.6, 3.6, 3.0), GOLD)                       # the butt cap
    p.box("shaft", (-1.8, cy - 1.8, -10), (3.6, 3.6, 3.0), GOLD)                      # the collar under the head
    stepped_spike(p, "shaft", (0, cy, -10), "-z", 16.0, 3.4, BOLT_W, steps=4, thick=3.4)
    for sx in (0, 1):                                                                 # the barbs, swept back
        mbox(p, "shaft", sx, (-6.0, cy - 0.9, -14), (5.6, 1.8, 4.5), GOLD, rot=(0, 0, 0), pivot=(0, cy, -12))
        mbox(p, "shaft", sx, (-8.0, cy - 0.6, -9), (4.0, 1.2, 4.0), BOLT, rot=(0, 20, 0), pivot=(-3.5, cy, -9))
    p.bone("coil", (0, cy, 6), parent="shaft")
    for k in range(6):                                                                # the coil: six electrodes spiralling round the shaft
        z = -6 + k * 5.4
        ang = k * 60.0
        p.box("coil", (-0.5, cy + 2.4, z), (1.0, 3.2, 1.0), BOLT_W, rot=(0, 0, ang), pivot=(0, cy, z))
        p.box("coil", (-0.5, cy + 4.6, z - 0.3), (1.0, 1.0, 1.6), BOLT, rot=(0, 0, ang), pivot=(0, cy, z))
    a.rot("fly", "coil", {0.0: (0, 0, 0), 0.25: (0, 0, 180), 0.5: (0, 0, 360)})
    a.rot("idle", "coil", {0.0: (0, 0, 0), 0.6: (0, 0, 180), 1.2: (0, 0, 360)})
    p.save(a)
    return p


def main():
    for m in (thunderbird(), lance()):
        print("%-24s cubes %3d" % (m.id, m.cube_count()))


if __name__ == "__main__":
    main()

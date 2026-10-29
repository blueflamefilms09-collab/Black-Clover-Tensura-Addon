"""The canon Millennium Rod (Sennen Shakujo), 3.x remake of the in-hand model. Written under the same id `millennium_rod` (mesh, skins, sprite, item models) as the old build in weapons.py,
which stays defined there (`python3 -B tools/relics/new_wave101/weapons.py --old-rod millennium_rod` runs it).

Look (docs/relics/millennium_canon.md, 'Rod'): a plain gold staff. A slender shaft ending in a sharp spike; a collar under the head; a big round setting that carries the Eye of Wdjat on both
faces (the only light of the rod: a glowing pupil and a faint rim); at each side of the setting a flared neck opening into a crescent blade whose horns turn up and out (the upper horn longer
than the lower); and a short pointed finial rising from the top of the setting. No bands, cord, feathers, halo or threads.

    python3 -B tools/relics/new_wave101/canon_rod.py
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from weapons import (GOLD, GOLD_HI, rod, lathe_y, orb, tube, collar_ring, sweep_blade, ring_blade, spear_head, P, ml, lid,  # noqa: E402
                     new_mesh, new_atlas, lay_and_paint, finish, glow_parts)

GOLD_DK = (196, 146, 46)
EYE_DK = (60, 38, 14)


def build_millennium_rod_canon():
    m = new_mesh("millennium_rod")
    Y1 = 188.0                                   # top of the shaft, under the collar
    CY = Y1 + 19.0                               # centre of the round setting
    R = 10.5                                     # its radius
    # the shaft, a touch slimmer at the foot, and the spike
    rod(m, "shaft", "gold", 0.0, Y1, 1.45, 1.8, sides=12, base=GOLD, smooth=55)
    lathe_y(m, "spike", "gold", -17.0, [(0.0, 0.0), (1.2, 0.55), (6.0, 1.15), (17.0, 1.45)], sides=10, base=GOLD_HI, caps=(False, False))
    for y, r, h in ((7.0, 2.1, 1.6), (12.0, 1.9, 1.2), (Y1 - 40.0, 2.2, 1.6)):
        collar_ring(m, y, r, h, "gold", GOLD_HI, name="ring_%d" % int(y), sides=12)
    # the collar under the head: a bead, a short flared cone that grows into the setting
    lathe_y(m, "collar", "gold", Y1 - 5.0, [(0.0, 1.9), (1.0, 2.8), (2.5, 3.0), (3.5, 2.2), (4.5, 2.2), (6.0, 3.4), (12.0, 5.4), (16.5, 8.2)], sides=12, base=GOLD_HI, caps=(False, False))
    # the round setting: a domed gold disc with a raised rim, seen face on
    disc = P(m, "setting", "gold", base=GOLD, smooth=45, uv_mode="loft")
    ml.lathe(disc, "z", (0.0, CY, 0.0), [(-2.4, R - 2.6), (-1.5, R - 0.4), (-0.6, R), (0.6, R), (1.5, R - 0.4), (2.4, R - 2.6)], sides=24, caps=(True, True))
    for sg, tag in ((1, "f"), (-1, "b")):
        z = sg * 3.0
        face = P(m, "face_" + tag, "gold", base=GOLD_HI, smooth=45, uv_mode="loft")
        ml.lathe(face, "z", (0.0, CY, sg * 1.9), [(0.0, R - 2.4), (sg * 0.5, R - 2.9)], sides=24, caps=(False, True))
        ring_blade(m, "rim_" + tag, "gold", 0.0, CY, R - 1.0, 0.9, 1.2, n=24, base=GOLD_HI)
        # the Eye of Wdjat in relief: almond lids, a dark iris, a lit pupil, the brow and the falcon's tear with its curl
        rim = lid(0.0, CY + 0.6, 6.4, 3.0, z, 1.0, n=10) + lid(0.0, CY + 0.6, 6.4, 2.4, z, -1.0, n=10)[::-1]
        tube(m, "eye_rim_" + tag, "gold", rim, 0.55, 0.55, sides=4, base=GOLD_HI, glow=True)
        orb(m, "iris_" + tag, "obsidian", (0.0, CY + 0.6, z), 2.5, base=EYE_DK, glow=False, rz=0.5, sides=10, stacks=6)
        orb(m, "pupil_" + tag, "crystal", (0.0, CY + 0.6, z + sg * 0.3), 1.2, base=(255, 232, 150), glow=True, rz=0.5, sides=8, stacks=5)
        tube(m, "brow_" + tag, "gold", [(-6.2 + 12.4 * t, CY + 5.6 + 1.3 * math.sin(math.pi * t), z) for t in np.linspace(0, 1, 7)], 0.5, 0.5, sides=4, base=GOLD_HI)
        tear = [(-0.8, CY - 2.3, z), (-0.8, CY - 6.0, z)] + [(3.0 + 2.1 * math.cos(a) * (1 - a / 14.0), CY - 6.2 + 2.1 * math.sin(a) * (1 - a / 14.0), z) for a in np.linspace(0, 8.0, 9)]
        tube(m, "tear_" + tag, "gold", tear, 0.45, 0.45, sides=4, base=GOLD_HI)
    # the short pointed finial on top of the setting
    lathe_y(m, "finial", "gold", CY + R - 1.5, [(0.0, 2.6), (1.5, 2.9), (3.0, 2.2), (4.5, 1.7), (15.0, 0.0)], sides=8, base=GOLD_HI, caps=(False, False))
    # the two crescent blades: one swept arc a side, widest at the neck and closing to a point at both ends; concave side out, the upper horn longer, both turned up and out
    for sx in (-1, 1):
        s = "l" if sx < 0 else "r"
        arc = [(sx * 28.0, CY - 17.0), (sx * 21.0, CY - 8.0), (sx * 16.0, CY + 0.5), (sx * 17.0, CY + 11.5), (sx * 23.0, CY + 23.0), (sx * 29.0, CY + 36.0)]
        neck = P(m, "neck_" + s, "gold", base=GOLD, smooth=45, uv_mode="loft")
        ml.lathe(neck, "x", (sx * (R - 2.0), CY, 0.0), [(0.0, 3.6), (sx * 2.0, 2.4), (sx * 5.5, 2.5), (sx * 9.0, 3.4)], sides=10, caps=(False, False))
        sweep_blade(m, "crescent_" + s, "gold", arc, lambda t: 5.0 * math.sin(math.pi * min(max(t, 0.0), 1.0)) ** 0.75 + 0.15, lambda t: 1.6 * math.sin(math.pi * min(max(t, 0.0), 1.0)) ** 0.5 + 0.2,
                    n=6, bevel=0.5, base=GOLD_HI, smooth=40)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["pupil_f", "pupil_b"], (255, 236, 160), 1.0, 0.6)
    glow_parts(atlas, m, ["eye_rim_f", "eye_rim_b"], (255, 214, 120), 0.6, 0.4)
    finish("millennium_rod", m, atlas, "staff", glow=("FFE9A8", 0.14, 0.45, 1.0))


if __name__ == "__main__":
    build_millennium_rod_canon()

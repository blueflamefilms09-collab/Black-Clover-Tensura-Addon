"""The boss trophy relic (docs/bosses/boss_drops.md): ONE relic shape made in Blender, recoloured per boss by tools/gen_boss_drop_assets.py.

A faceted crystal held by three gold claws over a ringed base: about 20 cm tall, centred on the origin like the Beast Will Core (it keeps that item's `place` line, so the hand, the
display transforms and the size class of a 'handitem' fit). The skin is NEUTRAL (grey metal, near-white crystal) so a per-boss tint gives every trophy its own colour; the crystal glows.

Run (Blender 5.x as a Python module, docs/heroweapon/blender_pipeline.md):
    python3 -B tools/blender/boss_trophy.py --trial tools/boss_drops
Writes trophy_relic.txt, trophy_relic.png and trophy_relic_glow.png into DIR (the neutral source the generator tints, kept in tools/boss_drops); never into the game's assets.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402

import bl_lib as bl  # noqa: E402


def ringpts(r, y, n, off=0.0):
    return [Vector((r * math.cos(off + 2 * math.pi * k / n), y, r * math.sin(off + 2 * math.pi * k / n))) for k in range(n)]


def build():
    crystal = bl.mat("trophy_crystal", bl.rgb(205, 205, 210), rough=0.18, emit=bl.rgb(220, 220, 225), strength=0.6, bump="hammer", bscale=0.5, bstr=0.2, bevel=0.2)
    metal = bl.mat("trophy_metal", bl.rgb(120, 120, 124), metallic=0.85, rough=0.32, bump="hammer", bscale=0.6, bstr=0.22, bevel=0.25, groove=bl.rgb(40, 40, 44), gamount=0.5)
    # the crystal: a long six-sided gem, twisted a half facet between rings
    rows = [(-8.0, 0.4, 0.0), (-4.5, 4.4, 0.5), (1.5, 5.4, 0.0), (7.0, 3.6, 0.5), (11.5, 0.4, 0.0)]
    bl.loft("crystal", [ringpts(r, y, 6, o) for y, r, o in [(y, r, o * math.pi / 6) for y, r, o in rows]], crystal, (True, True), part="relic")
    # the base: a ring with a step
    bl.loft("base", [ringpts(6.2, -10.0, 8), ringpts(6.6, -9.4, 8), ringpts(5.2, -8.2, 8), ringpts(3.0, -8.0, 8)], metal, (True, True), part="relic")
    # three claws: they rise from the base and close over the crystal's shoulder
    for i in range(3):
        a = 2 * math.pi * i / 3 + math.pi / 6
        c, s = math.cos(a), math.sin(a)
        pts = [(4.6 * c, -8.4, 4.6 * s), (6.4 * c, -3.0, 6.4 * s), (6.6 * c, 3.0, 6.6 * s), (4.4 * c, 8.2, 4.4 * s)]
        bl.tube(f"claw_{i}", pts, 0.8, metal, sides=5, sub=2, part="relic", taper=(1.0, 0.25))
    # a small ring round the claws at the equator
    bl.tube("collar", [(6.9 * math.cos(2 * math.pi * k / 9), 0.2, 6.9 * math.sin(2 * math.pi * k / 9)) for k in range(10)], 0.45, metal, sides=4, sub=1, part="relic")


def main(argv):
    trial = os.path.abspath(argv[argv.index("--trial") + 1]) if "--trial" in argv else os.path.join(bl.ROOT, "tools", "boss_drops")
    bl.reset(samples=16)
    build()
    header = os.path.join(bl.MESH_DIR, "beast_will_core.txt")
    bl.finish("trophy_relic", glow=("FFFFFF", 0.1, 0.4, 1.0), header_from=header, skin=256, bake=1024, out_mesh=trial, out_skin=trial,
              preview=os.path.join(trial or os.path.join(bl.ROOT, "tools", "boss_drops"), "trophy_relic_preview.png"))


if __name__ == "__main__":
    main(sys.argv[1:])

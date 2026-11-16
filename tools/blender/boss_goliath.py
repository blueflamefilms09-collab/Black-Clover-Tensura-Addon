"""The Goliath (DanMachi, the floor boss of the 17th floor), MADE IN BLENDER (boss rework wave 2, docs/bosses/rework_wave2_models.md).

A hunched, dark-grey stone giant about 7 blocks tall: short thick legs, a massive chest and shoulders of boulders, long arms with huge fists that hang to the
knees, a small head sunk between the shoulders under a heavy brow, and red glowing eyes; molten veins run through the chest, arms and thighs.

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    bvenv/bin/python -B tools/blender/boss_goliath.py [--trial DIR] [--quick]
Writes entity_meshes/goliath.txt (+ goliath_lod.txt), textures/entity/boss_mesh/goliath(.png, _glow.png) and docs/bosses/previews/goliath_blender*.png.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

ID = "goliath"
PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "bosses", "previews")

# the pivots the game turns the parts about (model units: 1/16 block, y up, face +z, its left = +x)
RIG = {
    "torso": (None, (0.0, 44.0, 0.0)),
    "head": ("torso", (0.0, 90.0, 9.0)),
    "arm_l": ("torso", (30.0, 84.0, 3.0)),
    "arm_r": ("torso", (-30.0, 84.0, 3.0)),
    "forearm_l": ("arm_l", (36.0, 60.0, 4.0)),
    "forearm_r": ("arm_r", (-36.0, 60.0, 4.0)),
    "leg_l": (None, (14.0, 44.0, 0.0)),
    "leg_r": (None, (-14.0, 44.0, 0.0)),
}

POSES = {
    "slam": {"torso": (0.32, 0.0, 0.0), "arm_l": (-2.5, 0.0, -0.25), "arm_r": (-2.5, 0.0, 0.25), "forearm_l": (-0.5, 0, 0), "forearm_r": (-0.5, 0, 0),
             "head": (-0.25, 0, 0)},
    "walk": {"leg_l": (0.42, 0, 0), "leg_r": (-0.42, 0, 0), "arm_l": (-0.32, 0, 0), "arm_r": (0.32, 0, 0), "forearm_l": (-0.3, 0, 0), "forearm_r": (-0.2, 0, 0),
             "torso": (0.05, 0.06, 0.03)},
}


# ================================================================ materials
def stone():
    """Dark-grey stone: chipped (Voronoi bump), mottled (noise), darker in the cracks; a few molten veins (Voronoi edges masked by a slow noise) emit."""
    m = bl.mat("goliath_stone", bl.rgb(66, 65, 70), rough=0.9, bump="hammer", bscale=0.22, bstr=0.55, bevel=0.6, groove=bl.rgb(24, 23, 27), gamount=0.6)
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    co = N.new("ShaderNodeTexCoord").outputs["Object"]
    mott = N.new("ShaderNodeTexNoise")
    mott.inputs["Scale"].default_value = 0.045
    mott.inputs["Detail"].default_value = 6.0
    L.new(co, mott.inputs["Vector"])
    tint = N.new("ShaderNodeMixRGB")
    tint.blend_type = "MULTIPLY"
    tint.inputs[0].default_value = 0.55
    L.new(bs.inputs["Base Color"].links[0].from_socket, tint.inputs[1])
    ramp = N.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].color = (0.55, 0.55, 0.6, 1)
    ramp.color_ramp.elements[1].color = (1.35, 1.32, 1.28, 1)
    L.new(mott.outputs[0], ramp.inputs[0])
    L.new(ramp.outputs[0], tint.inputs[2])
    L.new(tint.outputs[0], bs.inputs["Base Color"])
    # the veins
    vor = N.new("ShaderNodeTexVoronoi")
    vor.feature = "DISTANCE_TO_EDGE"
    vor.inputs["Scale"].default_value = 0.06
    L.new(co, vor.inputs["Vector"])
    edge = bl._math(nt, "LESS_THAN", vor.outputs["Distance"], 0.022)
    region = N.new("ShaderNodeTexNoise")
    region.inputs["Scale"].default_value = 0.04
    L.new(co, region.inputs["Vector"])
    mask = bl._math(nt, "MULTIPLY", edge, bl._math(nt, "GREATER_THAN", region.outputs[0], 0.62))
    bs.inputs["Emission Color"].default_value = bl.rgb(255, 92, 34)
    L.new(bl._math(nt, "MULTIPLY", mask, 5.0), bs.inputs["Emission Strength"])
    return m


def eye():
    return bl.mat("goliath_eye", bl.rgb(255, 70, 40), rough=0.3, emit=bl.rgb(255, 60, 30), strength=14.0, bevel=0.0)


# the modelling helpers (rock sections, limbs, boulders, shards, Displace, mirrored copies) are in bl_entity.py

# ================================================================ the giant
def build():
    be.seed(17)
    st, ey = stone(), eye()
    # ---- torso (pelvis to the neck), hunched forward
    t = be.body_loft("torso", "torso", [
        (37, 21, 13, 0, 0), (45, 24, 15, 0, 0), (54, 21, 14, 0, 1.5), (64, 26, 16, 0, 3.5),
        (74, 31, 18, 0, 5.5), (82, 33, 18, 0, 6.5), (88, 27, 15, 0, 7.0), (93, 15, 10, 0, 8.0)], 12, st, jag=0.07)
    bl.subsurf(t, 1)
    be.rough(t, 1.6)
    for sx in (1, -1):                                                       # shoulder boulders, chest slabs, back spikes
        b = be.boulder(f"shoulder_{sx}", "torso", (31 * sx, 88, 2), 11.5, st, squash=(1.0, 0.8, 0.95), seg=(8, 6))
        be.rough(b, 1.0)
        be.boulder(f"pec_{sx}", "torso", (13 * sx, 76, 17), 10, st, squash=(1.15, 0.75, 0.55), seg=(7, 5), jag=0.1)
        be.shard(f"spike_a{sx}", "torso", (24 * sx, 92, -6), (30 * sx, 104, -12), 4.2, st)
        be.shard(f"spike_b{sx}", "torso", (11 * sx, 88, -12), (13 * sx, 99, -21), 3.6, st)
        be.boulder(f"hip_{sx}", "torso", (19 * sx, 45, 1), 9, st, squash=(0.9, 0.8, 1.1), seg=(7, 5))
    be.shard("spike_c", "torso", (0, 80, -16), (0, 90, -26), 4.0, st)
    be.boulder("belly", "torso", (0, 58, 12), 11, st, squash=(1.3, 0.9, 0.55), seg=(7, 5), jag=0.08)
    # ---- head: a heavy jaw, a deep brow, sunk forward between the shoulders
    h = be.body_loft("head", "head", [
        (87, 9, 8, 0, 11), (91, 11, 9.5, 0, 13), (96, 11.5, 10, 0, 13.5), (100, 11, 10, 0, 13), (104, 8.5, 8, 0, 11.5), (106.5, 4, 4, 0, 10.5)], 8, st, jag=0.06)
    bl.subsurf(h, 1)
    be.rough(h, 0.7, 4.0)
    be.boulder("brow", "head", (0, 101, 21), 9.5, st, squash=(1.25, 0.32, 0.45), seg=(8, 5), jag=0.08)
    be.boulder("jaw", "head", (0, 90, 18.5), 8.5, st, squash=(1.2, 0.45, 0.55), seg=(8, 5), jag=0.1)
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 1.9, ey, center=(4.6 * sx, 98.2, 22.6), depth=0.5, crown=0.3, sides=6, axis="z", rx=2.3, ry=1.3, part="head")
    # ---- arms (left built, right mirrored): upper arm, then forearm with a huge fist
    ua = be.limb("arm_l", "arm_l", [(30, 87, 3), (32, 78, 3), (34, 69, 3.5), (36, 60, 4)], [12.5, 11.5, 10.0, 9.5], 10, st)
    bl.subsurf(ua, 1)
    be.rough(ua, 1.2)
    be.side_copy(ua, "arm_r")
    fa = be.limb("forearm_l", "forearm_l", [(36, 61, 4), (37, 54, 5), (38, 45, 6.5), (38.5, 37, 8)], [9.0, 10.5, 11.0, 9.5], 10, st)
    bl.subsurf(fa, 1)
    be.rough(fa, 1.2)
    be.side_copy(fa, "forearm_r")
    eb = be.shard("elbow_l", "forearm_l", (37, 61, -3), (40, 64, -13), 4.0, st)
    be.side_copy(eb, "forearm_r")
    fist = be.boulder("fist_l", "forearm_l", (39, 28, 10), 11.5, st, squash=(0.85, 0.95, 1.05), seg=(8, 6), jag=0.1)
    be.rough(fist, 0.8, 4.0)
    be.side_copy(fist, "forearm_r")
    for k in range(4):                                                       # knuckles: a row of rocks across the front of the fist
        kn = be.boulder(f"knuckle_l{k}", "forearm_l", (39 + (k - 1.5) * 4.4, 21.5 + abs(k - 1.5) * 0.8, 17.5 - abs(k - 1.5) * 1.2), 3.4, st,
                     squash=(1.0, 0.9, 1.0), seg=(5, 3), jag=0.08)
        be.side_copy(kn, "forearm_r")
    th = be.boulder("thumb_l", "forearm_l", (33, 29, 15), 4.2, st, squash=(0.9, 1.4, 0.9), seg=(5, 4), jag=0.08)
    be.side_copy(th, "forearm_r")
    # ---- legs: thick, short, a knee boulder, a broad foot with toes
    lg = be.limb("leg_l", "leg_l", [(14.5, 46, 0), (15.5, 36, 1), (16, 25, 2), (16, 15, 0.5), (16, 7, 0)], [11.5, 11.0, 9.5, 9.5, 9.0], 10, st)
    bl.subsurf(lg, 1)
    be.rough(lg, 1.2)
    be.side_copy(lg, "leg_r")
    kn = be.boulder("knee_l", "leg_l", (16.5, 25, 9), 6, st, squash=(1.0, 0.9, 0.7), seg=(7, 5))
    be.side_copy(kn, "leg_r")
    ft = be.body_loft("foot_l", "leg_l", [(0, 10, 13, 16, 3), (4, 10.5, 13.5, 16, 3.5), (8, 9, 10, 16, 2), (11, 7, 7, 16, 0)], 10, st, jag=0.08)
    be.rough(ft, 0.6, 4.0)
    be.side_copy(ft, "leg_r")
    for k in range(3):
        toe = be.boulder(f"toe_l{k}", "leg_l", (16 + (k - 1) * 5.5, 2.6, 15.5), 3.2, st, squash=(1.0, 0.85, 1.1), seg=(5, 3), jag=0.08)
        be.side_copy(toe, "leg_r")
    total = sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")
    print("faces before export:", total)


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    bl.reset(samples=8 if quick else 20)
    build()
    pv = trial or PREVIEW_DIR
    previews = {os.path.join(pv, "goliath_blender.png"): (28, None), os.path.join(pv, "goliath_blender_side.png"): (90, None),
                os.path.join(pv, "goliath_blender_slam.png"): (35, "slam")}
    be.finish_entity(ID, RIG, glow=("FF6A2A", 0.1, 0.55, 1.0), skin=1024 if not quick else 512, bake=2048 if not quick else 512,
                     out_mesh=trial, out_skin=trial, previews=previews, poses=POSES)


if __name__ == "__main__":
    main(sys.argv[1:])

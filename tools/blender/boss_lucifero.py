"""Lucifero, King of Devils (Black Clover, Spade Kingdom Raid), MADE IN BLENDER (boss rework wave 3, docs/bosses/rework_wave3.md).

A tall thin figure with grey skin and black patterns over the whole body, long white hair, two pairs of horns on the sides of the head, thin bony hands and two pairs of hooked bat-like
wings (docs/arc/lucifero.md section 2). The second form (`lucifero_form2`: "Lucifero surfaces") has spikes on the shoulders and elbows, thicker horns and a third eye with no pupil on the
forehead. About 2.7 blocks to the top of the head (the hitbox of the boss), the wings and horns reach further.

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    bvenv/bin/python -B tools/blender/boss_lucifero.py [--trial DIR] [--quick]
Writes entity_meshes/lucifero(_form2).txt (+ _lod), textures/entity/boss_mesh/lucifero(_form2)(.png, _glow.png) and docs/bosses/previews/lucifero_blender*.png.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "bosses", "previews")

# model units: 1/16 block, y up, face +z, its left = +x; 43 units = the 2.69-block hitbox
RIG = {
    "torso": (None, (0.0, 22.0, 0.0)),
    "head": ("torso", (0.0, 37.5, 0.5)),
    "arm_l": ("torso", (6.6, 35.0, 0.0)),
    "arm_r": ("torso", (-6.6, 35.0, 0.0)),
    "forearm_l": ("arm_l", (8.4, 26.5, 0.5)),
    "forearm_r": ("arm_r", (-8.4, 26.5, 0.5)),
    "leg_l": (None, (3.2, 21.0, 0.0)),
    "leg_r": (None, (-3.2, 21.0, 0.0)),
    "wing_l": ("torso", (3.0, 33.0, -3.0)),
    "wing_r": ("torso", (-3.0, 33.0, -3.0)),
}

POSES = {
    "slam": {"torso": (0.2, 0.0, 0.0), "arm_l": (-1.6, 0.0, -0.3), "arm_r": (-1.6, 0.0, 0.3), "wing_l": (0, 0, 0.7), "wing_r": (0, 0, -0.7), "head": (-0.2, 0, 0)},
}


def skin(name, base):
    """Grey skin with black patterns over the whole body: a flowing Voronoi-edge mask darkens the colour into black lines and blots."""
    m = bl.mat(name, base, rough=0.62, bump="leather", bscale=0.4, bstr=0.2, bevel=0.15, groove=bl.rgb(60, 58, 68), gamount=0.5)
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    co = N.new("ShaderNodeTexCoord").outputs["Object"]
    warp = N.new("ShaderNodeTexNoise")
    warp.inputs["Scale"].default_value = 0.25
    warp.inputs["Detail"].default_value = 3.0
    L.new(co, warp.inputs["Vector"])
    vor = N.new("ShaderNodeTexVoronoi")
    vor.feature = "DISTANCE_TO_EDGE"
    vor.inputs["Scale"].default_value = 0.34
    L.new(warp.outputs[0], vor.inputs["Vector"])
    line = bl._math(nt, "LESS_THAN", vor.outputs["Distance"], 0.07)
    blot = N.new("ShaderNodeTexNoise")
    blot.inputs["Scale"].default_value = 0.17
    L.new(co, blot.inputs["Vector"])
    mask = bl._math(nt, "MAXIMUM", line, bl._math(nt, "GREATER_THAN", blot.outputs[0], 0.66))
    mix = N.new("ShaderNodeMixRGB")
    mix.inputs[2].default_value = bl.rgb(14, 13, 19)
    L.new(mask, mix.inputs[0])
    L.new(bs.inputs["Base Color"].links[0].from_socket, mix.inputs[1])
    L.new(mix.outputs[0], bs.inputs["Base Color"])
    return m


def build(form2):
    be.seed(23 if not form2 else 29)
    gray = skin("luc_skin", bl.rgb(128, 126, 138))
    hair = bl.mat("luc_hair", bl.rgb(236, 236, 246), rough=0.55, bump="brushed", bscale=2.0, bstr=0.3, bevel=0.1, groove=bl.rgb(150, 150, 170), gamount=0.6)
    horn = bl.mat("luc_horn", bl.rgb(58, 52, 62), rough=0.4, bump="rings", bscale=1.6, bstr=0.35, bevel=0.1, groove=bl.rgb(24, 20, 28), gamount=0.5)
    wing = bl.mat("luc_wing", bl.rgb(46, 36, 62), rough=0.7, bump="leather", bscale=0.6, bstr=0.3, bevel=0.1, groove=bl.rgb(20, 14, 30), gamount=0.6)
    bone = bl.mat("luc_bone", bl.rgb(74, 66, 82), rough=0.5, bevel=0.1)
    cloth = bl.mat("luc_cloth", bl.rgb(30, 26, 40), rough=0.8, bump="wrap", bscale=0.6, bstr=0.3, bevel=0.1, groove=bl.rgb(14, 12, 20))
    eye = bl.mat("luc_eye", bl.rgb(190, 120, 255), rough=0.3, emit=bl.rgb(176, 96, 255), strength=14.0, bevel=0.0)
    third = bl.mat("luc_third", bl.rgb(250, 240, 255), rough=0.3, emit=bl.rgb(230, 210, 255), strength=16.0, bevel=0.0)
    # ---- torso: a narrow chest, long neck, bony shoulders
    t = be.body_loft("torso", "torso", [
        (20, 4.3, 3.0, 0, 0), (25, 3.8, 2.7, 0, 0), (30, 4.8, 3.0, 0, 0.2), (34.5, 6.0, 3.3, 0, 0.3), (37, 3.4, 2.5, 0, 0.5), (39, 2.0, 2.0, 0, 0.8)], 10, gray, jag=0.02)
    bl.subsurf(t, 1)
    for sx in (1, -1):                                                        # ribs
        for k in range(3):
            bl.tube(f"rib_{sx}_{k}", [(0.8 * sx, 31 - 2.4 * k, 3.3), (3.4 * sx, 31 - 2.4 * k, 3.0), (5.2 * sx, 31.4 - 2.4 * k, 1.2)], 0.32, bone, sides=5, sub=2, part="torso")
    skirt = be.body_loft("skirt", "torso", [(14, 5.4, 4.2, 0, 0), (19, 5.0, 3.6, 0, 0), (22, 4.6, 3.2, 0, 0)], 10, cloth, jag=0.03, caps=(False, False))
    bl.solidify(skirt, 0.4)
    bl.plate("sash", [(-4.7, 21.2), (4.7, 21.2), (4.7, 23.0), (-4.7, 23.0)], -0.3, 0.3, bone, part="torso")
    bl.move(bpy.data.objects["sash"], (0, 0, 3.4))
    # ---- head: a narrow face, long white hair down the back, two pairs of horns, violet slit eyes (a third eye in the second form)
    h = be.body_loft("head", "head", [
        (38, 1.9, 1.9, 0, 0.8), (40, 3.0, 3.1, 0, 1.2), (43, 3.5, 3.6, 0, 1.0), (45.5, 3.3, 3.4, 0, 0.4), (47, 2.2, 2.3, 0, 0.0)], 8, gray, jag=0.02)
    bl.subsurf(h, 1)
    bl.tube("nose", [(0, 41.6, 4.4), (0, 41.0, 5.3), (0, 40.2, 5.0)], 0.4, gray, sides=5, sub=2, part="head")
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 0.8, eye, center=(1.7 * sx, 42.3, 4.7), depth=0.5, crown=0.3, sides=6, axis="z", rx=1.0, ry=0.4, part="head")
        bl.tube(f"horn_a{sx}", [(3.2 * sx, 45.6, 0.8), (5.6 * sx, 47.4, 0.2), (7.2 * sx, 50.8 + (2 if form2 else 0), -1.0), (6.6 * sx, 54.0 + (3 if form2 else 0), -2.2)],
                1.0 if not form2 else 1.35, horn, sides=5, sub=2, part="head", taper=(1.0, 0.15))
        bl.tube(f"horn_b{sx}", [(3.6 * sx, 43.2, 0.0), (6.8 * sx, 43.0, -0.6), (9.0 * sx, 41.0, -0.4), (9.6 * sx, 38.4, 0.6)], 0.9 if not form2 else 1.2, horn, sides=5, sub=2,
                part="head", taper=(0.9, 0.12))
        be.boulder(f"ear_{sx}", "head", (3.7 * sx, 42.4, 0.5), 1.0, gray, squash=(1.4, 1.0, 0.6), seg=(5, 3), jag=0.03)
    cap = be.body_loft("hair_cap", "head", [(44.6, 3.7, 3.7, 0, -0.5), (46.2, 3.7, 3.6, 0, 0.0), (47.7, 2.4, 2.4, 0, 0.0)], 8, hair, jag=0.02)
    for k in range(7):                                                        # long hair: strands from the crown to the middle of the back
        a = (k - 3) * 0.38
        x0 = 3.3 * a / 1.2
        bl.tube(f"hair_{k}", [(x0, 46, 0.2), (x0 * 1.3, 40, -3.3), (x0 * 1.45, 32, -4.6), (x0 * 1.3, 24 + abs(k - 3) * 1.1, -4.0)], 1.0, hair, sides=4, sub=2, part="head", taper=(1.0, 0.25))
    if form2:
        bl.gem("third_eye", 1.2, third, center=(0, 44.6, 3.9), depth=0.5, crown=0.3, sides=6, axis="z", part="head")
    # ---- arms: long, thin, bony; hands with long fingers
    ua = be.limb("arm_l", "arm_l", [(6.8, 35, 0), (7.6, 31, 0.3), (8.2, 27, 0.5)], [1.9, 1.5, 1.3], 8, gray, jag=0.02)
    bl.subsurf(ua, 1)
    be.side_copy(ua, "arm_r")
    fa = be.limb("forearm_l", "forearm_l", [(8.4, 27, 0.5), (8.8, 22, 1.0), (9.0, 17.5, 1.5)], [1.3, 1.5, 1.2], 8, gray, jag=0.02)
    bl.subsurf(fa, 1)
    be.side_copy(fa, "forearm_r")
    for k in range(4):
        f = bl.tube(f"finger_l{k}", [(9.0 + (k - 1.5) * 0.7, 17.5, 1.5), (9.2 + (k - 1.5) * 0.9, 14.5, 2.3), (9.2 + (k - 1.5) * 1.0, 12.0, 3.0)], 0.4, bone, sides=5, sub=2,
                    part="forearm_l", taper=(0.45, 0.15))
        be.side_copy(f, "forearm_r")
    th = bl.tube("thumb_l", [(8.4, 18.0, 2.2), (7.4, 15.5, 3.2), (7.0, 13.8, 3.6)], 0.4, bone, sides=5, sub=2, part="forearm_l", taper=(0.45, 0.15))
    be.side_copy(th, "forearm_r")
    if form2:                                                                 # spikes on the shoulders and the elbows
        for sx in (1, -1):
            be.shard(f"shoulder_spike_a{sx}", "torso", (6.0 * sx, 36.0, 0.0), (9.0 * sx, 42.0, -1.0), 1.3, horn)
            be.shard(f"shoulder_spike_b{sx}", "torso", (6.8 * sx, 35.5, -1.0), (11.0 * sx, 39.5, -3.0), 1.1, horn)
        es = be.shard("elbow_spike_l", "forearm_l", (8.6, 27, -0.8), (10.2, 29.5, -4.2), 1.0, horn)
        be.side_copy(es, "forearm_r")
    # ---- legs: long thin legs with a bent knee and clawed feet
    lg = be.limb("leg_l", "leg_l", [(3.4, 22, 0), (3.6, 15, 0.8), (3.6, 9, -0.2), (3.6, 3.0, 0.4)], [2.5, 2.0, 1.6, 1.3], 8, gray, jag=0.02)
    bl.subsurf(lg, 1)
    be.side_copy(lg, "leg_r")
    ft = be.body_loft("foot_l", "leg_l", [(0, 1.4, 2.0, 3.6, 3.2), (1.8, 1.7, 2.4, 3.6, 3.6), (3.2, 1.5, 1.5, 3.6, 1.0)], 8, gray, jag=0.02)
    be.side_copy(ft, "leg_r")
    # ---- two pairs of hooked bat wings (the upper pair large, the lower pair smaller); a bone arm, three fingers and a membrane between them
    for sx_name, sx in (("l", 1),):
        up = [Vector(p) for p in [(3.0, 34, -3.0), (11, 41, -5.0), (20, 50, -6.0), (27, 59, -6.5)]]
        bl.tube("wing_a_arm_l", [tuple(v) for v in up], 0.9, bone, sides=5, sub=3, part="wing_l", taper=(1.0, 0.35))
        tips = [(33, 52, -7.0), (30, 40, -7.0), (22, 31, -6.5)]
        for k, tp in enumerate(tips):
            bl.tube(f"wing_a_finger_l{k}", [tuple(up[3]), ((up[3].x + tp[0]) / 2, (up[3].y + tp[1]) / 2 + 2.0, -6.8), tp], 0.5, bone, sides=5, sub=2, part="wing_l", taper=(0.6, 0.15))
        bl.plate("wing_a_membrane_l", [(11, 41), (27, 59), (33, 52), (27.5, 47), (30, 40), (25.5, 35.5), (22, 31), (14.5, 34), (6, 30)], -6.4, -6.0, wing, part="wing_l")
        low = [(3.0, 31, -3.0), (10, 31, -4.0), (17, 35, -4.5)]
        bl.tube("wing_b_arm_l", low, 0.7, bone, sides=5, sub=3, part="wing_l", taper=(0.8, 0.3))
        for k, tp in enumerate([(22, 38, -5), (20, 25, -5), (13, 18, -4.5)]):
            bl.tube(f"wing_b_finger_l{k}", [low[2], ((low[2][0] + tp[0]) / 2, (low[2][1] + tp[1]) / 2 + 1.5, -4.8), tp], 0.4, bone, sides=5, sub=2, part="wing_l", taper=(0.5, 0.12))
        bl.plate("wing_b_membrane_l", [(10, 31), (17, 35), (22, 38), (19, 31.5), (20, 25), (16, 21), (13, 18), (9, 22), (5, 24)], -4.4, -4.1, wing, part="wing_l")
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == "wing_l"]:
        c = bl.mirror(o, "x", o.name.replace("_l", "_r"))
        c["part"] = "wing_r"
    total = sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")
    print("faces before export:", total)


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    pv = trial or PREVIEW_DIR
    for form2 in (False, True):
        eid = "lucifero_form2" if form2 else "lucifero"
        bl.reset(samples=8 if quick else 20)
        build(form2)
        previews = {os.path.join(pv, f"{eid}_blender.png"): (30, None), os.path.join(pv, f"{eid}_blender_back.png"): (200, None)}
        if not form2:
            previews[os.path.join(pv, "lucifero_blender_slam.png")] = (35, "slam")
        be.finish_entity(eid, RIG, glow=("A66BFF", 0.12, 0.55, 1.0), skin=1024 if not quick else 512, bake=2048 if not quick else 512,
                         out_mesh=trial, out_skin=trial, previews=previews, poses=POSES, cage=0.2)


if __name__ == "__main__":
    main(sys.argv[1:])

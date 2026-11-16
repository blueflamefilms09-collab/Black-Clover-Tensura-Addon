"""The God of Order (relics/order, the judge that visits the holder of a duplicated Unique relic), MADE IN BLENDER (boss rework wave 3, docs/bosses/rework_wave3.md).

A tall serene judge in layered white robes with gold trim, a long gold stole, a mantle and a cape, a closed pale face under a turning gold halo, a ledger in the left hand and a
tall staff with a gold ring in the right. The second model (`god_of_order_gates`: "the open ledger") adds a second, tilted halo and six floating pages. About 3.4 blocks tall
(the hitbox of the boss), the halo a little above.

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    bvenv/bin/python -B tools/blender/boss_god_of_order.py [--trial DIR] [--quick]
Writes entity_meshes/god_of_order(_gates).txt (+ _lod), textures/entity/boss_mesh/god_of_order(_gates)(.png, _glow.png) and docs/bosses/previews/god_of_order_blender*.png.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "bosses", "previews")

# model units: 1/16 block, y up, face +z, its left = +x; 54 units = the 3.4-block hitbox
RIG = {
    "torso": (None, (0.0, 28.0, 0.0)),
    "head": ("torso", (0.0, 46.0, 0.0)),
    "arm_l": ("torso", (7.5, 44.0, 0.0)),
    "arm_r": ("torso", (-7.5, 44.0, 0.0)),
    "forearm_l": ("arm_l", (9.5, 33.0, 0.8)),
    "forearm_r": ("arm_r", (-9.5, 33.0, 0.8)),
    "leg_l": (None, (3.2, 27.0, 0.0)),
    "leg_r": (None, (-3.2, 27.0, 0.0)),
}

POSES = {
    "beam": {"arm_r": (-1.55, 0.0, 0.0), "torso": (0, 0.12, 0), "head": (-0.12, 0, 0)},
    "read": {"arm_l": (-0.35, 0, -1.05), "arm_r": (-0.35, 0, 1.05), "forearm_l": (-0.55, 0, 0), "forearm_r": (-0.55, 0, 0), "head": (-0.3, 0, 0)},
}


def ring_path(r, cy, z, n=18, tilt=0.0, cx=0.0):
    """Points of a circle in the xy plane (centre (cx, cy, z)), closed, tilted about the x axis by tilt."""
    pts = []
    for k in range(n + 1):
        a = 2 * math.pi * k / n
        x, y = r * math.cos(a), r * math.sin(a)
        pts.append((cx + x, cy + y * math.cos(tilt), z + y * math.sin(tilt)))
    return pts


def build(gates):
    be.seed(31)
    robe = bl.mat("goo_robe", bl.rgb(232, 228, 214), rough=0.8, bump="wrap", bscale=0.5, bstr=0.25, bevel=0.1, groove=bl.rgb(170, 164, 148), gamount=0.55)
    cape = bl.mat("goo_cape", bl.rgb(244, 240, 228), rough=0.85, bump="brushed", bscale=0.8, bstr=0.2, bevel=0.1, groove=bl.rgb(190, 184, 168), gamount=0.6)
    gold = bl.mat("goo_gold", bl.rgb(226, 178, 60), metallic=1.0, rough=0.28, bump="rings", bscale=0.8, bstr=0.2, bevel=0.1, groove=bl.rgb(140, 100, 30), gamount=0.6)
    skin = bl.mat("goo_skin", bl.rgb(236, 220, 204), rough=0.6, bevel=0.1)
    page = bl.mat("goo_page", bl.rgb(246, 238, 214), rough=0.8, emit=bl.rgb(232, 185, 35), strength=0.8, bevel=0.1)
    leather = bl.mat("goo_leather", bl.rgb(90, 58, 34), rough=0.7, bump="leather", bscale=0.8, bstr=0.3, bevel=0.1, groove=bl.rgb(46, 28, 16))
    eye = bl.mat("goo_eye", bl.rgb(255, 215, 110), rough=0.3, emit=bl.rgb(255, 196, 70), strength=14.0, bevel=0.0)
    halo = bl.mat("goo_halo", bl.rgb(255, 224, 120), metallic=1.0, rough=0.25, emit=bl.rgb(255, 200, 70), strength=7.0, bevel=0.0)
    # ---- torso: a narrow chest in a high-collared robe, a mantle over the shoulders, a gold stole down the front, a cape behind
    t = be.body_loft("torso", "torso", [
        (26, 5.2, 3.8, 0, 0), (32, 4.6, 3.3, 0, 0), (38, 5.6, 3.5, 0, 0), (43, 7.4, 3.8, 0, 0), (45.5, 4.4, 3.0, 0, 0), (47, 2.8, 2.6, 0, 0)], 10, robe, jag=0.015)
    bl.subsurf(t, 1)
    man = be.body_loft("mantle", "torso", [(41, 8.6, 5.0, 0, 0), (43.5, 8.8, 4.8, 0, -0.2), (45.5, 5.2, 3.8, 0, 0)], 12, cape, jag=0.02, caps=(False, False))
    bl.solidify(man, 0.5)
    bl.plate("stole", [(-1.6, 18.0), (1.6, 18.0), (1.9, 44.0), (-1.9, 44.0)], -0.3, 0.3, gold, part="torso")
    bl.move(bpy.data.objects["stole"], (0, 0, 4.1))
    bl.plate("collar", [(-3.6, 44.2), (3.6, 44.2), (3.0, 47.4), (-3.0, 47.4)], -0.3, 0.3, gold, part="torso")
    bl.move(bpy.data.objects["collar"], (0, 0, 3.0))
    bl.plate("cape", [(-6.4, 44), (6.4, 44), (7.6, 6), (-7.6, 6)], -0.3, 0.3, cape, part="torso")
    bl.move(bpy.data.objects["cape"], (0, 0, -4.6))
    bl.plate("cape_trim", [(-7.8, 6), (7.8, 6), (7.7, 8), (-7.7, 8)], -0.4, 0.4, gold, part="torso")
    bl.move(bpy.data.objects["cape_trim"], (0, 0, -4.6))
    bl.plate("belt", [(-5.6, 27.0), (5.6, 27.0), (5.6, 29.0), (-5.6, 29.0)], -0.3, 0.3, gold, part="torso")
    bl.move(bpy.data.objects["belt"], (0, 0, 3.8))
    # ---- head: a closed pale face, gold hairline, a turning halo behind it (a second, tilted one in the gates form)
    h = be.body_loft("head", "head", [(46, 2.6, 2.6, 0, 0.2), (48, 3.7, 3.7, 0, 0.4), (51, 4.0, 4.2, 0, 0.3), (54, 3.6, 3.8, 0, 0.0), (56, 2.2, 2.4, 0, -0.2)], 10, skin, jag=0.012)
    bl.subsurf(h, 1)
    be.body_loft("hair", "head", [(51.5, 4.15, 4.3, 0, 0.1), (54.5, 3.8, 3.9, 0, -0.2), (56.4, 2.3, 2.5, 0, -0.3)], 10, gold, jag=0.015)
    bl.tube("nose", [(0, 50.6, 4.4), (0, 49.6, 5.2), (0, 48.8, 4.9)], 0.4, skin, sides=5, sub=2, part="head")
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 0.7, eye, center=(1.75 * sx, 51.0, 4.35), depth=0.4, crown=0.3, sides=6, axis="z", rx=0.95, ry=0.32, part="head")
    bl.tube("halo", ring_path(8.5, 53.0, -3.2), 0.65, halo, sides=6, sub=1, part="head")
    for k in range(8):                                                        # rays on the halo
        a = 2 * math.pi * k / 8
        be.shard(f"halo_ray_{k}", "head", (8.5 * math.cos(a) * 1.04, 53.0 + 8.5 * math.sin(a) * 1.04, -3.2), (12.0 * math.cos(a), 53.0 + 12.0 * math.sin(a), -3.2), 0.7, halo, n=4)
    if gates:
        bl.tube("halo2", ring_path(11.0, 53.0, -4.5, tilt=1.0), 0.55, halo, sides=6, sub=1, part="head")
    # ---- arms: wide sleeves; the left hand holds a ledger, the right a staff topped with a gold ring
    ua = be.limb("arm_l", "arm_l", [(7.7, 44, 0), (8.6, 39, 0.4), (9.3, 34, 0.8)], [2.8, 3.4, 3.6], 9, robe, jag=0.015)
    bl.subsurf(ua, 1)
    be.side_copy(ua, "arm_r")
    fa = be.limb("forearm_l", "forearm_l", [(9.5, 34, 0.8), (10.0, 29, 2.0), (10.3, 24, 3.4)], [3.4, 3.6, 3.9], 9, robe, jag=0.015)
    bl.subsurf(fa, 1)
    be.side_copy(fa, "forearm_r")
    trim = bl.lathe("cuff_l", [(3.95, 0), (4.1, 0.6), (4.1, 1.6), (3.9, 2.2)], gold, sides=9, part="forearm_l", origin=(10.3, 23.5, 3.5))
    be.side_copy(trim, "forearm_r")
    hand = be.boulder("hand_l", "forearm_l", (10.4, 21.5, 4.0), 1.7, skin, squash=(0.9, 1.1, 0.9), seg=(6, 4), jag=0.02)
    be.side_copy(hand, "forearm_r")
    bl.plate("ledger_cover", [(-3.2, 0), (3.2, 0), (3.2, 8.4), (-3.2, 8.4)], -0.6, 0.6, leather, part="forearm_l")
    bl.move(bpy.data.objects["ledger_cover"], (10.6, 21.5, 7.0), rot=(52, 0, 0))
    bl.plate("ledger_pages", [(-2.8, 0.4), (2.8, 0.4), (2.8, 8.0), (-2.8, 8.0)], -0.3, 0.3, page, part="forearm_l")
    bl.move(bpy.data.objects["ledger_pages"], (10.6, 21.5, 7.5), rot=(52, 0, 0))
    sx0, sz0 = -10.6, 5.0
    bl.tube("staff", [(sx0, 6, sz0), (sx0, 26, sz0), (sx0, 46, sz0), (sx0, 66, sz0)], 0.8, gold, sides=6, sub=1, part="forearm_r")
    bl.tube("staff_ring", ring_path(3.6, 69.0, sz0, n=14, cx=sx0), 0.55, halo, sides=5, sub=1, part="forearm_r")
    bl.gem("staff_gem", 1.2, eye, center=(sx0, 69.0, sz0), depth=0.8, crown=0.4, sides=6, axis="z", part="forearm_r")
    # ---- legs: the robe's skirt in two halves that swing apart, with a gold hem and small feet
    lg = be.limb("leg_l", "leg_l", [(3.4, 28, 0), (4.0, 18, 0.6), (4.4, 8, 0.8), (4.6, 1.2, 1.0)], [4.2, 5.0, 5.4, 5.7], 10, robe, jag=0.015)
    be.side_copy(lg, "leg_r")
    hem = bl.lathe("hem_l", [(5.8, 0), (5.9, 0.8), (5.8, 1.8), (5.5, 2.6)], gold, sides=10, part="leg_l", origin=(4.6, 0.0, 1.0))
    be.side_copy(hem, "leg_r")
    ft = be.body_loft("foot_l", "leg_l", [(0, 2.0, 3.4, 4.2, 5.4), (1.5, 2.2, 3.5, 4.2, 5.4), (2.6, 1.8, 2.6, 4.2, 4.6)], 8, gold, jag=0.015)
    be.side_copy(ft, "leg_r")
    if gates:                                                                 # the open ledger: six pages floating round the body
        for k in range(6):
            a = 2 * math.pi * k / 6 + 0.3
            cx, cz = 15.0 * math.cos(a), 15.0 * math.sin(a)
            pg = bl.plate(f"page_{k}", [(-2.6, -3.4), (2.6, -3.4), (2.6, 3.4), (-2.6, 3.4)], -0.25, 0.25, page, part="torso")
            bl.move(pg, (cx, 32.0 + 7.0 * math.sin(k * 1.7), cz), rot=(math.degrees(0.25 * math.sin(k)), math.degrees(-a + math.pi / 2), 11))
    total = sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")
    print("faces before export:", total)


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    pv = trial or PREVIEW_DIR
    for gates in (False, True):
        eid = "god_of_order_gates" if gates else "god_of_order"
        bl.reset(samples=8 if quick else 20)
        build(gates)
        previews = {os.path.join(pv, f"{eid}_blender.png"): (28, None), os.path.join(pv, f"{eid}_blender_back.png"): (200, None)}
        if not gates:
            previews[os.path.join(pv, "god_of_order_blender_beam.png")] = (60, "beam")
        be.finish_entity(eid, RIG, glow=("E8B923", 0.1, 0.5, 1.0), skin=1024 if not quick else 512, bake=2048 if not quick else 512,
                         out_mesh=trial, out_skin=trial, previews=previews, poses=POSES, cage=0.2)


if __name__ == "__main__":
    main(sys.argv[1:])

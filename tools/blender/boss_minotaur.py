"""The Minotaur (DanMachi, the Level 2 bull of the 5th floor), MADE IN BLENDER (boss rework wave 2, docs/bosses/rework_wave2_models.md).

A bull-headed brute about 3 blocks tall (the hitbox of docs/arc/minotaur.md): a dark-brown hide over heavy muscle, a hunched hump of dark fur, a bull's head with a long
muzzle, a gold nose ring and great curved horns, red glowing eyes, wraps on the wrists, a leather loincloth, cloven hooves, and in its right fist its natural weapon: a
crude great axe of dungeon stone on a thick haft.

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    bvenv/bin/python -B tools/blender/boss_minotaur.py [--trial DIR] [--quick]
Writes entity_meshes/minotaur.txt (+ minotaur_lod.txt), textures/entity/boss_mesh/minotaur(.png, _glow.png) and docs/bosses/previews/minotaur_blender*.png.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Matrix, Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

ID = "minotaur"
PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "bosses", "previews")

# model units: 1/16 block, y up, face +z, its left = +x; 49.5 units = the 3.09-block hitbox
RIG = {
    "torso": (None, (0.0, 23.0, 0.0)),
    "head": ("torso", (0.0, 41.0, 3.5)),
    "arm_l": ("torso", (12.0, 38.5, 0.0)),
    "arm_r": ("torso", (-12.0, 38.5, 0.0)),
    "forearm_l": ("arm_l", (14.0, 29.0, 0.5)),
    "forearm_r": ("arm_r", (-14.0, 29.0, 0.5)),
    "leg_l": (None, (5.5, 23.0, 0.0)),
    "leg_r": (None, (-5.5, 23.0, 0.0)),
}

POSES = {
    "slam": {"torso": (0.3, 0.0, 0.0), "arm_l": (-2.5, 0.0, -0.2), "arm_r": (-2.5, 0.0, 0.2), "forearm_l": (-0.5, 0, 0), "forearm_r": (-0.5, 0, 0), "head": (-0.25, 0, 0)},
}


def materials():
    hide = bl.mat("mino_hide", bl.rgb(112, 54, 36), rough=0.75, bump="leather", bscale=0.5, bstr=0.35, bevel=0.3, groove=bl.rgb(58, 24, 16), gamount=0.5)
    fur = bl.mat("mino_fur", bl.rgb(52, 30, 22), rough=0.95, bump="brushed", bscale=1.6, bstr=0.6, bevel=0.2, groove=bl.rgb(22, 12, 9), gamount=0.6)
    horn = bl.mat("mino_horn", bl.rgb(214, 196, 160), rough=0.45, bump="rings", bscale=1.4, bstr=0.4, bevel=0.15, groove=bl.rgb(120, 100, 76), gamount=0.55)
    hoof = bl.mat("mino_hoof", bl.rgb(34, 30, 28), rough=0.5, bevel=0.2)
    leather = bl.mat("mino_leather", bl.rgb(74, 52, 34), rough=0.7, bump="wrap", bscale=0.9, bstr=0.4, bevel=0.15, groove=bl.rgb(36, 24, 16))
    gold = bl.mat("mino_gold", bl.rgb(230, 178, 70), metallic=1.0, rough=0.28, bevel=0.1)
    stone = bl.mat("mino_axe_stone", bl.rgb(92, 92, 98), rough=0.7, bump="hammer", bscale=0.6, bstr=0.5, bevel=0.25, groove=bl.rgb(40, 40, 46), gamount=0.6)
    wood = bl.mat("mino_haft", bl.rgb(96, 66, 42), rough=0.7, bump="grain", bscale=1.0, bstr=0.3, bevel=0.15, groove=bl.rgb(52, 34, 22))
    eye = bl.mat("mino_eye", bl.rgb(255, 60, 40), rough=0.3, emit=bl.rgb(255, 40, 24), strength=14.0, bevel=0.0)
    return hide, fur, horn, hoof, leather, gold, stone, wood, eye


def build():
    be.seed(5)
    hide, fur, horn, hoof, leather, gold, stone, wood, eye = materials()
    # ---- torso: pelvis to neck, a barrel chest, hunched; a hump of fur over the shoulders; a loincloth
    t = be.body_loft("torso", "torso", [
        (18.5, 7.5, 5.5, 0, 0), (22, 8.5, 6.0, 0, 0), (26, 8, 5.5, 0, 0.5), (30, 9.5, 6.5, 0, 1.0), (34, 11.5, 7.2, 0, 1.6),
        (38, 12, 7, 0, 1.6), (41, 8.5, 5.5, 0, 1.4), (43, 5, 4, 0, 2.0)], 12, hide, jag=0.03)
    bl.subsurf(t, 1)
    hump = be.boulder("hump", "torso", (0, 40, -3.5), 8.5, fur, squash=(1.3, 0.75, 0.8), seg=(9, 6), jag=0.1)
    be.rough(hump, 0.5, 2.0)
    for sx in (1, -1):
        be.boulder(f"pec_{sx}", "torso", (4.6 * sx, 34.5, 5.6), 4.6, hide, squash=(1.1, 0.75, 0.55), seg=(7, 5), jag=0.04)
        be.boulder(f"delt_{sx}", "torso", (11.5 * sx, 38.5, 0.3), 4.6, hide, squash=(1.0, 0.95, 1.0), seg=(7, 5), jag=0.04)
    be.boulder("abs", "torso", (0, 28.5, 5.2), 5.5, hide, squash=(1.1, 1.0, 0.45), seg=(7, 5), jag=0.04)
    cloth = be.body_loft("loincloth", "torso", [(14, 8.6, 6.4, 0, 0.5), (19, 8.8, 6.6, 0, 0), (21.5, 8.9, 6.6, 0, 0)], 12, leather, jag=0.02, caps=(False, False))
    bl.solidify(cloth, 0.5)
    bl.plate("belt", [(-9.2, 20.5), (9.2, 20.5), (9.2, 22.8), (-9.2, 22.8)], -0.4, 0.4, leather, part="torso")
    bl.move(bpy.data.objects["belt"], (0, 0, 6.9))
    # ---- head: a bull's skull with a long muzzle, a nose ring, curved horns, a forelock of fur, red eyes
    h = be.body_loft("head", "head", [
        (40, 4.4, 4.0, 0, 3.6), (43, 5.2, 4.8, 0, 4.6), (46.5, 5.6, 5.0, 0, 5.0), (50, 4.8, 4.4, 0, 4.4), (52, 3.0, 3.0, 0, 4.0)], 10, hide, jag=0.03)
    bl.subsurf(h, 1)
    muzzle = be.limb("muzzle", "head", [(0, 46.0, 8.0), (0, 44.0, 11.0), (0, 42.0, 14.0)], [3.8, 3.6, 3.2], 8, hide, flat=0.8, jag=0.02)
    bl.subsurf(muzzle, 1)
    bl.tube("nose_ring", [(-1.8, 41.4, 13.6), (0, 40.0, 13.9), (1.8, 41.4, 13.6)], 0.36, gold, sides=6, sub=3, part="head")
    be.boulder("forelock", "head", (0, 51.0, 6.0), 3.6, fur, squash=(1.2, 0.6, 1.0), seg=(7, 4), jag=0.15)
    for sx in (1, -1):
        bl.tube(f"horn_{sx}", [(4.5 * sx, 50.0, 4.8), (8.5 * sx, 51.2, 5.0), (12 * sx, 53.5, 6.5), (13.2 * sx, 57.0, 9.0), (12.2 * sx, 59.5, 11.5)], 1.0, horn,
                sides=7, sub=2, part="head", taper=(1.7, 0.2))
        bl.gem(f"eye_{sx}", 0.8, eye, center=(3.5 * sx, 47.6, 8.7), depth=0.5, crown=0.3, sides=6, axis="z", rx=1.0, ry=0.6, part="head")
        be.boulder(f"ear_{sx}", "head", (6.4 * sx, 48.0, 3.8), 1.5, hide, squash=(1.8, 0.6, 0.8), seg=(6, 4), jag=0.05)
    # ---- arms: thick, long; wraps on the wrists; heavy fists (the right one holds the axe)
    ua = be.limb("arm_l", "arm_l", [(12.2, 39, 0), (13, 35, 0.2), (13.6, 31.5, 0.4), (14, 29, 0.5)], [4.2, 4.0, 3.5, 3.2], 9, hide, jag=0.03)
    bl.subsurf(ua, 1)
    be.side_copy(ua, "arm_r")
    fa = be.limb("forearm_l", "forearm_l", [(14, 29.5, 0.5), (14.4, 26, 1.0), (14.8, 22, 1.6), (15, 19.5, 2.0)], [3.2, 3.6, 3.2, 2.7], 9, hide, jag=0.03)
    bl.subsurf(fa, 1)
    be.side_copy(fa, "forearm_r")
    wrap = bl.lathe("wrap_l", [(2.95, 0), (3.05, 1.2), (3.0, 2.4), (2.9, 3.4)], leather, sides=10, part="forearm_l", origin=(14.8, 19.6, 1.9))
    be.side_copy(wrap, "forearm_r")
    fist = be.boulder("fist_l", "forearm_l", (15.2, 17.2, 2.6), 3.2, hide, squash=(0.85, 1.0, 1.1), seg=(8, 6), jag=0.05)
    be.side_copy(fist, "forearm_r")
    # ---- the great axe in the right fist: a thick haft running forward and down through the fist, a double-bitted head of dungeon stone at its end
    hx = -15.2
    u = Vector((0, -0.4, 0.92)).normalized()                                 # along the haft, from its butt behind the fist to its end in front
    w = Vector((0, -u.z, u.y)) * -1                                          # across the haft in its plane, pointing up-forward
    butt = Vector((hx, 22.6, -3.0))
    end = butt + u * 32.0
    bl.tube("haft", [butt, butt + u * 10, butt + u * 22, end], 1.15, wood, sides=8, sub=1, part="forearm_r")
    bl.tube("haft_wrap", [butt + u * 4.0, butt + u * 6.5, butt + u * 9.0], 1.35, leather, sides=8, sub=1, part="forearm_r")
    half = [(1.4, -3.6), (5.5, -6.8), (9.5, -7.6), (11.0, -2.0), (11.0, 2.0), (9.5, 7.6), (5.5, 6.8), (1.4, 3.6)]
    outline = half + [(-t, s_) for t, s_ in reversed(half)]
    ax = bl.plate("axe_head", outline, -1.5, 1.5, stone, part="forearm_r")
    head = end - u * 5.0
    ax.data.transform(Matrix(((w.x, u.x, 1.0, head.x), (w.y, u.y, 0.0, head.y), (w.z, u.z, 0.0, head.z), (0, 0, 0, 1))))
    bl.outward(ax)
    bl.bevel(ax, 0.5, 1, 30)
    be.boulder("axe_cap", "forearm_r", tuple(end), 1.9, stone, squash=(1.0, 1.0, 1.0), seg=(6, 4), jag=0.12)
    # ---- legs: thick thighs, a backward-bent hock, cloven hooves; fur on the shins
    lg = be.limb("leg_l", "leg_l", [(5.6, 24, 0), (6.0, 18, 0.8), (6.2, 12.5, 1.2), (6.0, 7.5, -0.6), (6.0, 3.0, 0.4)], [4.8, 4.4, 3.6, 2.9, 2.5], 9, hide, jag=0.03)
    bl.subsurf(lg, 1)
    be.side_copy(lg, "leg_r")
    shin = be.boulder("shin_fur_l", "leg_l", (6.0, 6.5, 0.2), 3.3, fur, squash=(1.0, 1.35, 1.0), seg=(7, 5), jag=0.18)
    be.side_copy(shin, "leg_r")
    for sz in (1, -1):
        hf = be.boulder(f"hoof_l{sz}", "leg_l", (6.0 + 1.25 * sz, 1.2, 1.6), 1.9, hoof, squash=(0.75, 0.7, 1.35), seg=(6, 4), jag=0.04)
        be.side_copy(hf, "leg_r")
    total = sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")
    print("faces before export:", total)


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    bl.reset(samples=8 if quick else 20)
    build()
    pv = trial or PREVIEW_DIR
    previews = {os.path.join(pv, "minotaur_blender.png"): (28, None), os.path.join(pv, "minotaur_blender_side.png"): (90, None),
                os.path.join(pv, "minotaur_blender_slam.png"): (35, "slam")}
    be.finish_entity(ID, RIG, glow=("FF3A22", 0.12, 0.5, 1.0), skin=1024 if not quick else 512, bake=2048 if not quick else 512,
                     out_mesh=trial, out_skin=trial, previews=previews, poses=POSES, cage=0.2)


if __name__ == "__main__":
    main(sys.argv[1:])

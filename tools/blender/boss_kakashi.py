"""Kakashi Hatake (boss, docs/bosses/kakashi.md): a PLAYER-SHAPED anime body made in Blender, in the war-time outfit of the Copy Ninja.

Design (original model, from the series' look as the builder remembers it; no wiki could be fetched, see the doc): silver gravity-defying hair, the Konoha forehead protector pushed up and tilted so the
left eye is open, the left eye a Sharingan with the Mangekyo (Kamui) pattern and its scar, a navy face mask up to the nose and a high navy collar, a dark-green flak jacket with chest pockets over a navy
long-sleeved shirt, steel arm guards, gloves with a steel back plate, navy trousers with a kunai holster and bandage wraps on the right thigh and both shins, shinobi sandals, a pouch and the white-hilted
tanto across the lower back. Every piece belongs to one of the eight roles of client/bossmesh/MeshPose, so the body animates with the shared rig (and the new Kakashi curves).

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    python3 -B tools/blender/boss_kakashi.py [--trial DIR] [--quick] [--showcase]
Writes entity_meshes/kakashi(_lod).txt and textures/entity/boss_mesh/kakashi(.png, _glow.png); --showcase also renders docs/boss_showcase/kakashi/model_*.png (bigger renders, several poses).
Model space as bl_entity.py: 1 unit = 1/16 block, y up, face +z, the boss's left = +x.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

SHOWCASE_DIR = os.path.join(bl.ROOT, "docs", "boss_showcase", "kakashi")

RIG = {
    "torso": (None, (0.0, 12.0, 0.0)),
    "head": ("torso", (0.0, 24.4, 0.0)),
    "arm_l": ("torso", (5.8, 22.8, 0.0)),
    "arm_r": ("torso", (-5.8, 22.8, 0.0)),
    "forearm_l": ("arm_l", (5.9, 17.6, 0.0)),
    "forearm_r": ("arm_r", (-5.9, 17.6, 0.0)),
    "leg_l": (None, (2.0, 12.0, 0.0)),
    "leg_r": (None, (-2.0, 12.0, 0.0)),
}

# the same turns the game uses at the moment of the blow (client/bossmesh/MeshPose RAIKIRI / SEALS / THROW / SUSANOO at t = 0.8)
POSES = {
    "raikiri": {"torso": (0.28, -0.35, 0.0), "head": (-0.12, 0.2, 0.0), "arm_r": (-1.5, 0.0, 0.1), "forearm_r": (-0.2, 0.0, 0.0), "arm_l": (-0.95, 0.0, -0.45),
                "forearm_l": (-1.25, 0.0, 0.0), "leg_r": (-0.7, 0.0, 0.0), "leg_l": (0.5, 0.0, 0.0)},
    "seals": {"torso": (0.12, 0.0, 0.0), "head": (0.22, 0.0, 0.0), "arm_r": (-0.55, 0.0, 0.5), "forearm_r": (-1.75, 0.0, 0.0), "arm_l": (-0.55, 0.0, -0.5), "forearm_l": (-1.75, 0.0, 0.0)},
    "throw": {"torso": (0.0, 0.55, 0.0), "arm_r": (-2.5, 0.0, 0.15), "forearm_r": (-0.5, 0.0, 0.0), "arm_l": (-0.5, 0.0, -0.4), "leg_l": (-0.35, 0.0, 0.0), "leg_r": (0.3, 0.0, 0.0)},
    "kamui_eye": {"head": (-0.12, 0.0, 0.0), "arm_l": (-2.55, 0.0, -0.1), "forearm_l": (-1.0, 0.0, 0.0), "torso": (-0.05, 0.0, 0.0)},
    "susanoo": {"torso": (-0.06, 0.0, 0.0), "head": (-0.2, 0.0, 0.0), "arm_l": (-0.15, 0.0, -1.15), "arm_r": (-0.15, 0.0, 1.15), "forearm_l": (-0.5, 0.0, 0.0), "forearm_r": (-0.5, 0.0, 0.0)},
}


def sloft(name, part, rows, mat, n=12, e=2.5, caps=(True, True)):
    """A smooth, slightly squared (superellipse) tube: rows = [(y, half width, half depth, x offset, z offset)]."""
    rings = []
    for y, w, d, ox, oz in rows:
        pts = []
        for k in range(n):
            a = 2 * math.pi * k / n
            c, s = math.cos(a), math.sin(a)
            pts.append(Vector((ox + w * math.copysign(abs(c) ** (2 / e), c), y, oz + d * math.copysign(abs(s) ** (2 / e), s))))
        rings.append(pts)
    return bl.loft(name, rings, mat, caps, part=part)


def mirror_part(ob, part):
    c = bl.mirror(ob, "x", ob.name.replace("_l", "_r"))
    c["part"] = part
    return c


def mirror_all(part, to):
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == part and o.name.endswith("_l")]:
        mirror_part(o, to)


def materials():
    m = {}
    m["skin"] = bl.mat("kk_skin", bl.rgb(236, 200, 172), rough=0.62, bevel=0.1)
    m["hair"] = bl.mat("kk_hair", bl.rgb(206, 212, 226), rough=0.42, bump="brushed", bscale=2.6, bstr=0.4, bevel=0.1, groove=bl.rgb(120, 130, 154), gamount=0.55)
    m["navy"] = bl.mat("kk_navy", bl.rgb(34, 42, 78), rough=0.82, bump="wrap", bscale=0.7, bstr=0.25, bevel=0.1, groove=bl.rgb(14, 18, 40), gamount=0.5)
    m["mask"] = bl.mat("kk_mask", bl.rgb(30, 34, 62), rough=0.88, bump="leather", bscale=0.8, bstr=0.15, bevel=0.1, groove=bl.rgb(12, 14, 30), gamount=0.4)
    m["vest"] = bl.mat("kk_vest", bl.rgb(66, 88, 70), rough=0.78, bump="leather", bscale=0.7, bstr=0.3, bevel=0.1, groove=bl.rgb(28, 40, 32), gamount=0.55)
    m["vestdark"] = bl.mat("kk_vestdark", bl.rgb(42, 58, 46), rough=0.8, bump="leather", bscale=0.7, bstr=0.25, bevel=0.1, groove=bl.rgb(18, 26, 20), gamount=0.5)
    m["steel"] = bl.mat("kk_steel", bl.rgb(166, 174, 188), metallic=0.9, rough=0.34, bump="hammer", bscale=0.9, bstr=0.18, bevel=0.1, groove=bl.rgb(70, 76, 90), gamount=0.5)
    m["glove"] = bl.mat("kk_glove", bl.rgb(24, 26, 36), rough=0.7, bump="leather", bscale=0.9, bstr=0.2, bevel=0.1)
    m["wrap"] = bl.mat("kk_wrap", bl.rgb(226, 222, 206), rough=0.9, bump="wrap", bscale=1.2, bstr=0.35, bevel=0.08, groove=bl.rgb(150, 146, 130), gamount=0.6)
    m["leather"] = bl.mat("kk_leather", bl.rgb(76, 58, 44), rough=0.7, bump="leather", bscale=0.8, bstr=0.3, bevel=0.1, groove=bl.rgb(34, 24, 18), gamount=0.55)
    m["sandal"] = bl.mat("kk_sandal", bl.rgb(28, 34, 66), rough=0.8, bevel=0.1)
    m["dark"] = bl.mat("kk_dark", bl.rgb(10, 10, 16), rough=0.8, bevel=0.0)
    m["scar"] = bl.mat("kk_scar", bl.rgb(150, 92, 86), rough=0.8, bevel=0.0)
    m["iris"] = bl.mat("kk_iris", bl.rgb(34, 30, 34), rough=0.3, bevel=0.0)
    m["sclera"] = bl.mat("kk_sclera", bl.rgb(236, 232, 226), rough=0.4, bevel=0.0)
    m["shari"] = bl.mat("kk_shari", bl.rgb(230, 20, 24), rough=0.25, emit=bl.rgb(255, 28, 24), strength=16.0, bevel=0.0)
    m["blade"] = bl.mat("kk_blade", bl.rgb(230, 236, 246), metallic=0.8, rough=0.25, bevel=0.1)
    return m


# ================================================================ the body
def head(M):
    s, mk = M["skin"], M["mask"]
    sloft("head", "head", [(24.6, 1.8, 1.8, 0, 0.1), (25.6, 2.9, 2.8, 0, 0.3), (27.2, 3.5, 3.3, 0, 0.2), (29.3, 3.8, 3.6, 0, 0.0), (31.3, 3.5, 3.4, 0, -0.1), (32.6, 2.5, 2.5, 0, -0.2)], s, n=14)
    # the mask: the lower face up to the nose bridge, and the nose under it
    sloft("mask", "head", [(24.8, 1.95, 1.95, 0, 0.1), (25.6, 3.05, 2.95, 0, 0.3), (27.2, 3.65, 3.45, 0, 0.2), (28.55, 3.9, 3.7, 0, 0.1)], mk, n=14)
    bl.tube("mask_nose", [(0, 28.4, 3.5), (0, 27.4, 4.3), (0, 26.4, 4.0)], 0.62, mk, sides=5, sub=2, part="head")
    bl.tube("mask_fold_l", [(1.2, 28.3, 3.55), (2.6, 27.0, 3.7), (3.0, 25.8, 3.4)], 0.16, M["dark"], sides=4, sub=2, part="head", detail=True)
    mirror_part(bl.tube("mask_fold_l2", [(1.2, 28.3, 3.55), (2.6, 27.0, 3.7), (3.0, 25.8, 3.4)], 0.16, M["dark"], sides=4, sub=2, part="head", detail=True), "head")
    # a high collar of the shirt
    sloft("neck_collar", "head", [(23.0, 2.7, 2.5, 0, 0), (24.4, 2.35, 2.25, 0, 0.1), (25.7, 2.3, 2.2, 0, 0.1)], M["navy"], n=12)
    # the left eye: a Sharingan with the three-bladed Kamui Mangekyo; the right eye is dark and calm
    for sx, mat_eye in ((1, M["shari"]), (-1, M["iris"])):
        bl.gem(f"eye_white_{sx}", 1.0, M["sclera"], center=(1.8 * sx, 29.3, 3.5), depth=0.4, crown=0.3, sides=8, axis="z", rx=1.05, ry=0.62, part="head")
        bl.gem(f"eye_{sx}", 0.72 if sx > 0 else 0.5, mat_eye, center=(1.8 * sx, 29.3, 3.72), depth=0.4, crown=0.25, sides=8, axis="z", rx=0.72 if sx > 0 else 0.5, ry=0.66 if sx > 0 else 0.5, part="head")
        bl.tube(f"lid_{sx}", [(0.55 * sx, 29.95, 3.62), (1.8 * sx, 30.35, 3.78), (3.1 * sx, 29.9, 3.5)], 0.16, M["dark"], sides=4, sub=2, part="head", detail=True)
    for k in range(3):                                                   # the Kamui Mangekyo: three curved blades round a pupil (black, baked on the red)
        a0 = k * 2 * math.pi / 3
        pts = [(1.8 + 0.5 * math.cos(a0 + u), 29.3 + 0.5 * math.sin(a0 + u) * 0.92, 3.9) for u in (0.0, 0.7, 1.4)]
        bl.tube(f"mangekyo_{k}", pts, 0.1, M["dark"], sides=4, sub=2, part="head", detail=True)
    bl.gem("pupil", 0.18, M["dark"], center=(1.8, 29.3, 3.95), depth=0.4, crown=0.2, sides=6, axis="z", part="head")
    bl.tube("eye_scar", [(2.25, 31.2, 3.7), (1.9, 29.6, 3.95), (1.55, 27.9, 3.7)], 0.13, M["scar"], sides=4, sub=2, part="head", detail=True)
    for sx in (1, -1):
        bl.tube(f"brow_{sx}", [(0.7 * sx, 30.5, 3.5), (1.9 * sx, 30.9, 3.55), (3.2 * sx, 30.4, 3.2)], 0.3, M["hair"], sides=4, sub=2, part="head", taper=(1.0, 0.5))
    # the forehead protector: a cloth band slanted over the head (lower on the left), a steel plate with the leaf spiral, the tails of the knot behind
    pts = [(3.95 * math.sin(a), 31.3 - 0.5 * math.sin(a), 3.75 * math.cos(a) - 0.1) for a in [i * 2 * math.pi / 22 for i in range(23)]]
    bl.tube("hitai_band", pts, 0.78, M["navy"], sides=4, sub=1, part="head")
    sl = -0.5 / 3.9
    plate = [(-1.9, 31.3 + sl * -1.9 - 0.78), (1.9, 31.3 + sl * 1.9 - 0.78), (1.9, 31.3 + sl * 1.9 + 0.78), (-1.9, 31.3 + sl * -1.9 + 0.78)]
    bl.plate("hitai_plate", plate, 3.55, 3.95, M["steel"], part="head")
    spiral = [(0.55 * math.cos(t) * (0.35 + t * 0.12) + 0.0, 31.3 - 0.05 + 0.5 * math.sin(t) * (0.35 + t * 0.12), 4.0) for t in [i * 0.45 for i in range(14)]]
    bl.tube("hitai_leaf", spiral, 0.09, M["dark"], sides=4, sub=1, part="head", detail=True)
    for sx in (1, -1):
        bl.tube(f"hitai_tail_{sx}", [(0.7 * sx, 31.0, -3.8), (2.4 * sx, 27.5, -5.0), (3.4 * sx, 22.5, -5.2)], 0.65, M["navy"], sides=4, sub=3, part="head", taper=(1.0, 0.6))
    # silver hair: a cap and swept spikes
    be.seed(77)
    sloft("hair_cap", "head", [(30.8, 3.95, 3.75, 0, -0.1), (32.3, 3.8, 3.65, 0, -0.15), (33.5, 3.0, 3.0, 0, -0.2), (34.2, 1.5, 1.5, 0, -0.25)], M["hair"], n=12)
    sloft("hair_back", "head", [(30.5, 3.9, 1.6, 0, -2.4), (27.6, 3.4, 1.5, 0, -2.6), (25.4, 2.4, 1.2, 0, -2.2)], M["hair"], n=10)
    for i in range(18):
        a = 2 * math.pi * i / 18 + (0.15 if i % 2 else 0.0)
        h = 31.4 + 1.7 * (i % 3) / 2
        base = (3.5 * math.sin(a), h, 3.4 * math.cos(a) - 0.15)
        up = 3.6 + 1.6 * ((i * 7) % 5) / 4
        out = 1.0 + 0.6 * ((i * 3) % 4) / 3
        tip = (base[0] * 1.0 + out * math.sin(a) * 0.7 + 2.0, base[1] + up, base[2] + out * math.cos(a) * 0.7 - 1.0)
        be.shard(f"hair_spike_{i}", "head", base, tip, 0.95, M["hair"], n=4)
    for k in range(3):                                                    # the fringe over the right brow
        x = -2.4 + k * 1.2
        bl.tube(f"fringe_{k}", [(x, 33.2, 1.6), (x * 1.1, 32.0, 3.9), (x * 1.15 - 0.2, 31.0, 4.15)], 0.62, M["hair"], sides=4, sub=2, part="head", taper=(1.0, 0.2))


def torso(M):
    # the shirt, then the flak jacket over it: closed rings with a collar, pockets, a zip, shoulder straps and a belt
    sloft("shirt", "torso", [(11.4, 3.6, 2.1, 0, 0), (14.5, 3.3, 1.95, 0, 0), (19.0, 3.9, 2.1, 0, 0), (22.6, 4.3, 2.2, 0, 0), (23.7, 3.6, 2.0, 0, 0), (24.6, 1.8, 1.8, 0, 0.05)], M["navy"], n=12)
    sloft("vest", "torso", [(13.4, 3.8, 2.3, 0, 0), (15.0, 3.6, 2.2, 0, 0), (19.0, 4.2, 2.35, 0, 0.0), (22.5, 4.55, 2.4, 0, 0.0), (23.5, 3.9, 2.2, 0, 0.0)], M["vest"], n=14)
    sloft("vest_collar", "torso", [(23.0, 2.9, 2.6, 0, 0.1), (24.4, 2.55, 2.3, 0, 0.1), (25.0, 2.5, 2.25, 0, 0.0)], M["vestdark"], n=12)
    bl.tube("vest_zip", [(0, 23.3, 2.55), (0, 18.5, 2.75), (0, 13.6, 2.6)], 0.18, M["steel"], sides=4, sub=3, part="torso", detail=True)
    for sx in (1, -1):
        for j, (y0, y1) in enumerate(((19.6, 22.0), (14.4, 17.4))):                      # chest pockets and the lower pockets with flaps
            bl.plate(f"pocket_{sx}_{j}", [(1.0 * sx, y0), (3.4 * sx, y0), (3.4 * sx, y1), (1.0 * sx, y1)] if sx > 0 else [(-3.4, y0), (-1.0, y0), (-1.0, y1), (-3.4, y1)], 2.34, 2.72, M["vestdark"],
                     part="torso")
            bl.plate(f"flap_{sx}_{j}", [(1.0 * sx, y1 - 0.8), (3.4 * sx, y1 - 0.8), (3.4 * sx, y1), (1.0 * sx, y1)] if sx > 0 else [(-3.4, y1 - 0.8), (-1.0, y1 - 0.8), (-1.0, y1), (-3.4, y1)], 2.7, 2.86,
                     M["vest"], part="torso")
        bl.tube(f"strap_{sx}", [(3.2 * sx, 23.6, 0.6), (3.3 * sx, 23.7, -1.0)], 0.5, M["vestdark"], sides=4, sub=1, part="torso")
        bl.gem(f"stud_{sx}", 0.28, M["steel"], center=(2.2 * sx, 21.4, 2.76), depth=0.5, crown=0.3, sides=6, axis="z", part="torso")
    # the belt and the back: a pouch, the scroll and the tanto across the lower back
    sloft("belt", "torso", [(12.4, 3.75, 2.3, 0, 0), (13.5, 3.8, 2.35, 0, 0)], M["leather"], n=14)
    bl.gem("buckle", 0.7, M["steel"], center=(0, 13.0, 2.4), depth=0.5, crown=0.3, sides=4, axis="z", part="torso")
    sloft("pouch_back", "torso", [(14.8, 2.6, 0.7, 0, -2.5), (11.6, 2.5, 0.75, 0, -2.5)], M["leather"], n=10)
    sloft("sheath", "torso", [(14.1, 4.6, 0.55, 0.0, -3.1), (14.9, 4.6, 0.55, 0.0, -3.1)], M["dark"], n=10)
    bl.tube("tanto_hilt", [(3.9, 14.4, -3.1), (5.3, 14.45, -3.1), (6.6, 14.5, -3.1)], 0.45, M["wrap"], sides=5, sub=1, part="torso")
    bl.tube("tanto_guard", [(3.55, 14.1, -3.1), (3.55, 14.9, -3.1)], 0.5, M["steel"], sides=5, sub=1, part="torso")
    bl.tube("scroll", [(-3.6, 16.2, -3.0), (3.6, 16.9, -3.0)], 0.75, M["wrap"], sides=6, sub=1, part="torso")
    # the cloth of the vest collar seen from behind and a back panel seam
    bl.tube("back_seam", [(0, 23.0, -2.55), (0, 18.0, -2.6), (0, 14.0, -2.55)], 0.16, M["dark"], sides=4, sub=3, part="torso", detail=True)


def arms(M):
    ua = sloft("arm_l", "arm_l", [(23.4, 2.15, 2.05, 5.8, 0), (21.0, 1.8, 1.85, 5.9, 0), (17.8, 1.6, 1.65, 5.9, 0)], M["navy"], n=10)
    sl = sloft("sleeve_pad_l", "arm_l", [(23.9, 1.7, 1.7, 5.9, 0), (22.6, 2.5, 2.4, 5.9, 0), (21.2, 2.05, 2.0, 5.9, 0)], M["vest"], n=10)
    fa = sloft("forearm_l", "forearm_l", [(17.8, 1.6, 1.65, 5.9, 0), (15.0, 1.5, 1.6, 5.95, 0.1), (12.0, 1.2, 1.3, 6.0, 0.2)], M["navy"], n=10)
    sloft("armguard_l", "forearm_l", [(16.4, 1.78, 1.85, 5.95, 0.05), (14.6, 1.72, 1.8, 5.98, 0.1), (12.6, 1.4, 1.5, 6.02, 0.2)], M["steel"], n=10)
    bl.tube("armguard_strap_l", [(5.95 + 1.8 * math.cos(a), 15.6, 0.1 + 1.85 * math.sin(a)) for a in [i * math.pi / 5 for i in range(11)]], 0.2, M["leather"], sides=4, sub=1, part="forearm_l")
    sloft("hand_l", "forearm_l", [(12.2, 1.2, 1.3, 6.0, 0.2), (10.8, 1.3, 0.95, 6.05, 0.5), (9.6, 1.15, 0.8, 6.1, 0.6)], M["glove"], n=10)
    bl.plate("hand_plate_l", [(6.35, 11.8), (7.45, 11.6), (7.45, 10.2), (6.35, 10.0)], 0.2, 0.9, M["steel"], part="forearm_l")
    for k in range(4):
        bl.tube(f"finger_l{k}", [(5.55 + k * 0.45, 9.8, 0.2 + k * 0.05), (5.55 + k * 0.45, 8.4, 0.5), (5.6 + k * 0.45, 7.2, 0.7)], 0.3, M["skin"], sides=4, sub=1, part="forearm_l", taper=(1.0, 0.7))
    bl.tube("thumb_l", [(5.0, 11.0, 1.1), (4.6, 9.8, 1.4), (4.6, 8.7, 1.5)], 0.34, M["skin"], sides=4, sub=1, part="forearm_l", taper=(1.0, 0.7))
    mirror_all("arm_l", "arm_r")
    mirror_all("forearm_l", "forearm_r")


def legs(M):
    sloft("leg_l", "leg_l", [(12.4, 2.2, 2.3, 2.0, 0), (9.0, 2.05, 2.2, 2.0, 0.05), (6.2, 1.85, 2.0, 2.0, 0.1), (3.2, 1.7, 1.85, 2.0, 0.05), (1.4, 1.55, 1.7, 2.0, 0.05)], M["navy"], n=10)
    for i, y in enumerate((5.6, 4.0, 2.6)):                                    # bandage wraps round the shins
        sloft(f"shin_wrap_l{i}", "leg_l", [(y, 1.92 - 0.1 * i, 2.1 - 0.1 * i, 2.0, 0.1), (y + 1.1, 1.9 - 0.1 * i, 2.08 - 0.1 * i, 2.0, 0.1)], M["wrap"], n=10)
    sloft("foot_l", "leg_l", [(0.0, 1.75, 3.5, 2.0, 1.3), (1.2, 1.85, 3.5, 2.0, 1.3), (2.1, 1.6, 2.5, 2.0, 0.6)], M["sandal"], n=10)
    mirror_all("leg_l", "leg_r")
    # the right leg only: the kunai holster strapped on the thigh, bandage wraps and the loose strap
    sloft("holster", "leg_r", [(10.8, 0.75, 1.8, -4.35, 0.2), (7.6, 0.7, 1.7, -4.3, 0.2)], M["leather"], n=10)
    for i, y in enumerate((10.0, 9.0, 8.0, 7.0)):
        sloft(f"thigh_wrap_{i}", "leg_r", [(y, 2.3, 2.4, -2.0, 0), (y + 0.7, 2.28, 2.38, -2.0, 0)], M["wrap"], n=10)
    for i, x in enumerate((-0.2, 0.7)):
        bl.tube(f"kunai_{i}", [(-4.35 + x * 0.3, 10.9, 0.6 + i * 1.0), (-4.35 + x * 0.3, 12.5, 0.6 + i * 1.0)], 0.28, M["wrap"], sides=4, sub=1, part="leg_r")
        bl.tube(f"kunai_ring_{i}", [(-4.35 + x * 0.3, 12.9, 0.6 + i * 1.0), (-4.35 + x * 0.3, 13.5, 0.6 + i * 1.0)], 0.42, M["steel"], sides=6, sub=1, part="leg_r")
    bl.tube("holster_strap", [(-2.0 + 2.2 * math.cos(a), 11.4, 2.3 * math.sin(a)) for a in [i * math.pi / 6 for i in range(13)]], 0.22, M["leather"], sides=4, sub=1, part="leg_r")


def build():
    be.seed(52)
    M = materials()
    torso(M)
    head(M)
    arms(M)
    legs(M)
    print("faces before export:", sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH" and not o.get("detail")))


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    showcase = "--showcase" in argv
    bl.reset(samples=8 if quick else 20)
    build()
    pv = trial or os.path.join(bl.ROOT, "docs", "bosses", "previews")
    previews = {os.path.join(pv, "kakashi_blender.png"): (28, None)}
    if showcase:                                                          # bigger renders, several views and the signature poses
        be.render_previews.__defaults__ = ((880, 1180), 48)
        for name, (yaw, pose) in {"model_front": (0, None), "model_three_quarter": (32, None), "model_back": (180, None), "model_side": (90, None), "pose_raikiri": (50, "raikiri"),
                                  "pose_seals": (20, "seals"), "pose_throw": (-40, "throw"), "pose_kamui_eye": (20, "kamui_eye"), "pose_susanoo": (15, "susanoo")}.items():
            previews[os.path.join(SHOWCASE_DIR, name + ".png")] = (yaw, pose)
    be.finish_entity("kakashi", RIG, glow=("FF2420", 0.10, 0.6, 1.0), skin=1024 if not quick else 512, bake=2048 if not quick else 512, out_mesh=trial, out_skin=trial,
                     previews=previews, poses=POSES, cage=0.2)


if __name__ == "__main__":
    main(sys.argv[1:])

"""Lucifero v2 (boss drops and Lucifero OP update): a PLAYER-SHAPED body made in Blender, with his armour as SEPARATE parts.

The owner asked for a Lucifero "like the actual anime, not so blocky": normal human proportions (a Minecraft player's, 8 : 12 : 12 units scaled to the 2.69 block hitbox), a real face, long white hair, devil
horns, and armour pieces that are their own meshes (chest plate and tassets, two pauldrons, two bracers, two greaves, a crown), so the same shapes can be worn by the player as the drop set
(docs/bosses/lucifero_v2.md). Three bodies, each with a far (LOD) copy:
  lucifero_v2         the king at the start of the fight: horns, hair, folded wings, the armour
  lucifero_v2_form2   Lucifero surfaces: wings spread, a third eye, longer horns, spikes, glowing veins
  lucifero_v2_gates   the Gates are open: the four wings become arms (Six Limbs), a halo of horns

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    python3 -B tools/blender/boss_lucifero_v2.py [--trial DIR] [--quick] [--only form2]
Writes entity_meshes/<id>(_lod).txt, textures/entity/boss_mesh/<id>(.png, _glow.png) and docs/bosses/previews/<id>_blender*.png.
Model space as bl_entity.py: 1 unit = 1/16 block, y up, face +z, the boss's left = +x; roles by part name (client/bossmesh/MeshPose), extra parts follow their parent.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402
import boss_lucifero as old  # noqa: E402   (its skin shader: grey skin with black patterns)

PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "bosses", "previews")

RIG = {
    "torso": (None, (0.0, 16.0, 0.0)),
    "head": ("torso", (0.0, 33.5, 0.0)),
    "arm_l": ("torso", (8.1, 31.0, 0.0)),
    "arm_r": ("torso", (-8.1, 31.0, 0.0)),
    "forearm_l": ("arm_l", (8.2, 24.5, 0.0)),
    "forearm_r": ("arm_r", (-8.2, 24.5, 0.0)),
    "leg_l": (None, (2.75, 16.0, 0.0)),
    "leg_r": (None, (-2.75, 16.0, 0.0)),
    "wing_l": ("torso", (3.5, 31.0, -3.4)),
    "wing_r": ("torso", (-3.5, 31.0, -3.4)),
    "chest_plate": ("torso", (0.0, 16.0, 0.0)),
    "tasset": ("torso", (0.0, 16.0, 0.0)),
    "pauldron_l": ("arm_l", (8.1, 31.0, 0.0)),
    "pauldron_r": ("arm_r", (-8.1, 31.0, 0.0)),
    "bracer_l": ("forearm_l", (8.2, 24.5, 0.0)),
    "bracer_r": ("forearm_r", (-8.2, 24.5, 0.0)),
    "greave_l": ("leg_l", (2.75, 16.0, 0.0)),
    "greave_r": ("leg_r", (-2.75, 16.0, 0.0)),
    "crown": ("head", (0.0, 33.5, 0.0)),
    "halo": ("head", (0.0, 33.5, 0.0)),
}

POSES = {
    "slam": {"torso": (0.2, 0.0, 0.0), "arm_l": (-1.6, 0.0, -0.3), "arm_r": (-1.6, 0.0, 0.3), "wing_l": (0, 0, 0.5), "wing_r": (0, 0, -0.5), "head": (-0.2, 0, 0)},
}


def sloft(name, part, rows, mat, n=12, e=2.5, caps=(True, True)):
    """A smooth, slightly squared (superellipse) tube: rows = [(y, half width, half depth, x offset, z offset)]. Not the rocky rings of bl_entity: a body, not a boulder."""
    rings = []
    for y, w, d, ox, oz in rows:
        pts = []
        for k in range(n):
            a = 2 * math.pi * k / n
            c, s = math.cos(a), math.sin(a)
            pts.append(Vector((ox + w * math.copysign(abs(c) ** (2 / e), c), y, oz + d * math.copysign(abs(s) ** (2 / e), s))))
        rings.append(pts)
    return bl.loft(name, rings, mat, caps, part=part)


def skin2(name, base):
    """The v1 skin (grey with black patterns) made lighter and finer: thin markings and few blots, so the face and hands stay pale like the anime's Lucifero."""
    m = old.skin(name, base)
    nt = m.node_tree
    for n in nt.nodes:
        if n.bl_idname == "ShaderNodeMath" and n.operation == "LESS_THAN":
            n.inputs[1].default_value = 0.045                                # thinner lines
        if n.bl_idname == "ShaderNodeMath" and n.operation == "GREATER_THAN":
            n.inputs[1].default_value = 0.74                                 # fewer blots
    return m


def mirror_part(ob, part):
    c = bl.mirror(ob, "x", ob.name.replace("_l", "_r"))
    c["part"] = part
    return c


def materials():
    m = {}
    m["skin"] = skin2("luc2_skin", bl.rgb(184, 180, 198))
    m["hair"] = bl.mat("luc2_hair", bl.rgb(238, 238, 248), rough=0.5, bump="brushed", bscale=2.4, bstr=0.35, bevel=0.1, groove=bl.rgb(140, 140, 164), gamount=0.6)
    m["horn"] = bl.mat("luc2_horn", bl.rgb(46, 40, 52), rough=0.35, bump="rings", bscale=1.6, bstr=0.35, bevel=0.1, groove=bl.rgb(18, 14, 22), gamount=0.5)
    m["wing"] = bl.mat("luc2_wing", bl.rgb(44, 34, 62), rough=0.7, bump="leather", bscale=0.6, bstr=0.3, bevel=0.1, groove=bl.rgb(18, 12, 28), gamount=0.6)
    m["bone"] = bl.mat("luc2_bone", bl.rgb(78, 70, 88), rough=0.5, bevel=0.1)
    m["armor"] = bl.mat("luc2_armor", bl.rgb(34, 28, 46), metallic=0.8, rough=0.34, bump="hammer", bscale=0.9, bstr=0.25, bevel=0.12, groove=bl.rgb(14, 10, 22), gamount=0.55)
    m["gold"] = bl.mat("luc2_gold", bl.rgb(190, 150, 62), metallic=1.0, rough=0.28, bevel=0.1)
    m["cloth"] = bl.mat("luc2_cloth", bl.rgb(26, 22, 38), rough=0.85, bump="wrap", bscale=0.6, bstr=0.3, bevel=0.1, groove=bl.rgb(12, 10, 18))
    m["eye"] = bl.mat("luc2_eye", bl.rgb(200, 130, 255), rough=0.3, emit=bl.rgb(176, 96, 255), strength=14.0, bevel=0.0)
    m["third"] = bl.mat("luc2_third", bl.rgb(250, 240, 255), rough=0.3, emit=bl.rgb(230, 210, 255), strength=16.0, bevel=0.0)
    m["rune"] = bl.mat("luc2_rune", bl.rgb(190, 120, 255), rough=0.4, emit=bl.rgb(166, 90, 255), strength=12.0, bevel=0.0)
    m["dark"] = bl.mat("luc2_dark", bl.rgb(10, 8, 14), rough=0.8, bevel=0.0)
    return m


# ================================================================ the body
def body(M, form):
    s = M["skin"]
    # ---- torso (hips 16 .. neck 34.5) and the neck
    t = sloft("torso", "torso", [(15.5, 4.9, 2.9, 0, 0), (19.5, 4.2, 2.7, 0, 0), (24.5, 4.7, 2.9, 0, 0), (29, 5.3, 3.1, 0, 0.1), (32.2, 5.5, 3.0, 0, 0), (33.4, 3.2, 2.4, 0, 0),
                                       (34.8, 1.9, 1.9, 0, 0.1)], s, n=12)
    # ---- head: a narrow jaw, a high brow, pointed ears
    h = sloft("head", "head", [(34.2, 2.0, 2.0, 0, 0.2), (35.4, 3.0, 3.1, 0, 0.5), (37.5, 4.0, 4.0, 0, 0.5), (40.5, 4.6, 4.5, 0, 0.2), (42.8, 4.2, 4.2, 0, 0.0), (44.2, 2.7, 2.8, 0, -0.2)], s, n=14)
    bl.tube("nose", [(0, 39.3, 4.4), (0, 38.0, 5.6), (0, 36.9, 5.3)], 0.42, s, sides=5, sub=2, part="head")
    bl.tube("mouth", [(-1.7, 36.5, 4.5), (0, 36.2, 4.9), (1.7, 36.5, 4.5)], 0.18, M["dark"], sides=4, sub=2, part="head")
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 0.85, M["eye"], center=(2.0 * sx, 39.4, 4.55), depth=0.5, crown=0.3, sides=6, axis="z", rx=1.1, ry=0.42, part="head")
        bl.tube(f"brow_{sx}", [(0.9 * sx, 40.5, 4.7), (2.2 * sx, 40.8, 4.5), (3.7 * sx, 40.3, 3.9)], 0.3, M["dark"], sides=4, sub=2, part="head")
        be.shard(f"ear_{sx}", "head", (4.4 * sx, 38.7, 0.5), (7.4 * sx, 40.6, -0.6), 0.85, s)
        up, lo = (3.0, 4.0) if form else (0.0, 0.0)
        bl.tube(f"horn_a{sx}", [(3.4 * sx, 43.0, 0.6), (5.6 * sx, 45.0, 0.0), (7.0 * sx, 48.2 + up, -1.0), (6.4 * sx, 51.6 + up + lo, -2.4)], 1.0 + (0.3 if form else 0), M["horn"], sides=5, sub=2,
                part="head", taper=(1.0, 0.12))
        bl.tube(f"horn_b{sx}", [(3.8 * sx, 40.8, -0.2), (6.9 * sx, 40.6, -0.8), (9.0 * sx, 38.6, -0.4), (9.6 * sx, 36.0 + (1 if form else 0), 0.8)], 0.85 + (0.25 if form else 0), M["horn"],
                sides=5, sub=2, part="head", taper=(1.0, 0.12))
    # hair: a cap, a fringe over the brow, side locks to the chest and a long fall down the back
    be.seed(31)
    sloft("hair_cap", "head", [(41.0, 4.85, 4.75, 0, 0.2), (43.0, 4.7, 4.6, 0, 0.0), (44.6, 3.1, 3.1, 0, -0.2), (45.4, 1.6, 1.6, 0, -0.3)], M["hair"], n=12, caps=(False, True))
    for k in range(3):
        a = (k - 1) * 2.3
        bl.tube(f"fringe_{k}", [(a, 44.0, 2.2), (a * 1.25, 42.9, 4.9), (a * 1.6 + (k - 1) * 0.4, 40.9, 5.2), (a * 1.8, 39.6, 4.6)], 0.8, M["hair"], sides=5, sub=2, part="head", taper=(1.0, 0.12))
    for sx in (1, -1):
        bl.tube(f"lock_{sx}", [(4.6 * sx, 41.5, 2.0), (5.5 * sx, 36.0, 3.0), (5.6 * sx, 29.0, 3.7), (5.2 * sx, 24.5, 4.0)], 0.85, M["hair"], sides=5, sub=3, part="head", taper=(1.0, 0.18))
    back = sloft("hair_back", "head", [(43.5, 4.9, 1.2, 0, -3.6), (38.0, 5.2, 1.8, 0, -5.0), (31.0, 4.4, 1.7, 0, -4.8), (24.0, 3.4, 1.5, 0, -4.2), (18.5, 1.2, 1.0, 0, -3.8)], M["hair"], n=8,
                 caps=(False, True))
    # ---- arms (slim, long: a king of devils, not a bruiser) and thin clawed hands
    ua = sloft("arm_l", "arm_l", [(32.2, 2.5, 2.4, 8.1, 0), (28.5, 2.0, 2.0, 8.2, 0), (24.8, 1.8, 1.8, 8.2, 0)], s, n=10)
    mirror_part(ua, "arm_r")
    fa = sloft("forearm_l", "forearm_l", [(24.8, 1.8, 1.8, 8.2, 0), (21.0, 1.7, 1.8, 8.3, 0.2), (17.6, 1.25, 1.4, 8.4, 0.3)], s, n=10)
    mirror_part(fa, "forearm_r")
    for k in range(4):
        f = bl.tube(f"finger_l{k}", [(8.4 + (k - 1.5) * 0.62, 17.6, 0.5), (8.45 + (k - 1.5) * 0.8, 15.0, 1.3), (8.5 + (k - 1.5) * 0.9, 12.6, 2.2)], 0.36, M["bone"], sides=4, sub=1,
                    part="forearm_l", taper=(1.0, 0.3))
        mirror_part(f, "forearm_r")
    th = bl.tube("thumb_l", [(7.4, 17.8, 1.0), (6.7, 15.6, 2.0), (6.5, 14.0, 2.6)], 0.36, M["bone"], sides=4, sub=1, part="forearm_l", taper=(1.0, 0.3))
    mirror_part(th, "forearm_r")
    # ---- legs
    lg = sloft("leg_l", "leg_l", [(16.4, 2.7, 2.7, 2.75, 0), (12.0, 2.5, 2.5, 2.75, 0.1), (8.6, 2.1, 2.2, 2.75, 0.2), (4.8, 2.1, 2.2, 2.75, 0.0), (1.4, 1.7, 1.9, 2.75, 0.2)], s, n=10)
    mirror_part(lg, "leg_r")
    ft = sloft("foot_l", "leg_l", [(0.0, 1.7, 3.6, 2.75, 1.4), (1.2, 1.8, 3.6, 2.75, 1.4), (2.0, 1.6, 2.6, 2.75, 0.8)], s, n=10)
    mirror_part(ft, "leg_r")


def wings(M, form):
    """Two pairs of hooked bat wings on each side (as the anime): folded to about half and tilted back at the start, spread in the second form. Gates: see arms()."""
    k = 0.55 if form == 0 else 1.0
    dy = -2.0
    root = Vector((3.5, 31.0, -3.4))

    def P(x, y, z):
        return tuple(root + (Vector((x, y + dy, z)) - Vector((3.0, 34.0 + dy, -3.0))) * k)

    up = [P(3.0, 34, -3.0), P(11, 41, -5.0), P(20, 50, -6.0), P(27, 59, -6.5)]
    bl.tube("wing_a_arm_l", up, 0.95, M["bone"], sides=5, sub=3, part="wing_l", taper=(1.0, 0.35))
    tips = [P(33, 52, -7.0), P(30, 40, -7.0), P(22, 31, -6.5)]
    for i, tp in enumerate(tips):
        mid = tuple((Vector(up[3]) + Vector(tp)) / 2 + Vector((0, 2.0 * k, 0)))
        bl.tube(f"wing_a_finger_l{i}", [up[3], mid, tp], 0.5, M["bone"], sides=4, sub=1, part="wing_l", taper=(0.6, 0.15))
    outline = [P(11, 41, 0), P(27, 59, 0), P(33, 52, 0), P(27.5, 47, 0), P(30, 40, 0), P(25.5, 35.5, 0), P(22, 31, 0), P(14.5, 34, 0), P(6, 30, 0)]
    bl.plate("wing_a_membrane_l", [(p[0], p[1]) for p in outline], P(0, 0, -6.4)[2], P(0, 0, -6.0)[2], M["wing"], part="wing_l")
    low = [P(3.0, 31, -3.0), P(10, 31, -4.0), P(17, 35, -4.5)]
    bl.tube("wing_b_arm_l", low, 0.7, M["bone"], sides=5, sub=3, part="wing_l", taper=(0.8, 0.3))
    for i, tp in enumerate([P(22, 38, -5), P(20, 25, -5), P(13, 18, -4.5)]):
        mid = tuple((Vector(low[2]) + Vector(tp)) / 2 + Vector((0, 1.5 * k, 0)))
        bl.tube(f"wing_b_finger_l{i}", [low[2], mid, tp], 0.4, M["bone"], sides=4, sub=1, part="wing_l", taper=(0.5, 0.12))
    pts = [P(10, 31, 0), P(17, 35, 0), P(22, 38, 0), P(19, 31.5, 0), P(20, 25, 0), P(16, 21, 0), P(13, 18, 0), P(9, 22, 0), P(5, 24, 0)]
    bl.plate("wing_b_membrane_l", [(p[0], p[1]) for p in pts], P(0, 0, -4.4)[2], P(0, 0, -4.1)[2], M["wing"], part="wing_l")
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == "wing_l"]:
        mirror_part(o, "wing_r")


def arms(M):
    """The Gates: his four wings are four long arms, two a side, jointed and clawed (Six Limbs), with a thin leathery fold of wing left between the pair."""
    for name, sh, e, h, th in (("upper", (3.5, 32.0, -3.6), (14.0, 44.0, -7.0), (26.0, 50.0, -3.0), 1.1), ("lower", (3.5, 28.0, -3.6), (13.0, 32.0, -7.5), (24.0, 24.0, -4.5), 0.95)):
        bl.tube(f"warm_{name}_a_l", [sh, tuple((Vector(sh) + Vector(e)) / 2 + Vector((0, 1.5, -0.5))), e], th, M["skin"], sides=5, sub=1, part="wing_l", taper=(1.0, 0.8))
        bl.tube(f"warm_{name}_b_l", [e, tuple((Vector(e) + Vector(h)) / 2 + Vector((0, -1.0, 1.0))), h], th * 0.8, M["skin"], sides=5, sub=1, part="wing_l", taper=(1.0, 0.55))
        for i in range(3):
            sp = (i - 1.0) * 2.4
            bl.tube(f"warm_{name}_claw_l{i}", [h, (h[0] + 2.4, h[1] + sp * 0.5, h[2] + 1.2), (h[0] + 5.6, h[1] + sp * 1.3 - 1.0, h[2] + 2.4)], 0.45, M["bone"], sides=4, sub=1, part="wing_l",
                    taper=(1.0, 0.12))
    bl.plate("warm_fold_l", [(4, 31), (10, 40), (14, 44), (13, 32), (12, 28)], -6.0, -5.6, M["wing"], part="wing_l")
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == "wing_l"]:
        mirror_part(o, "wing_r")


# ================================================================ the armour: separate parts
def armour(M, form):
    A, G, R = M["armor"], M["gold"], M["rune"]
    # chest plate (a shell over the chest and back) with a gold collar, a belt and a rune stone
    cp = sloft("chest_plate", "chest_plate", [(22.0, 4.75, 3.2, 0, 0), (25.5, 5.4, 3.4, 0, 0.1), (29.5, 5.9, 3.6, 0, 0.15), (32.4, 6.0, 3.4, 0, 0.0), (33.6, 3.6, 2.7, 0, 0.0)], A, n=14,
               caps=(True, False))
    bl.tube("collar", [(-3.7, 33.4, 2.4), (-2.6, 34.4, 3.3), (0, 34.6, 3.5), (2.6, 34.4, 3.3), (3.7, 33.4, 2.4)], 0.8, G, sides=5, sub=2, part="chest_plate")
    sloft("belt", "chest_plate", [(20.3, 4.6, 3.0, 0, 0), (21.9, 4.7, 3.1, 0, 0)], G, n=14)
    bl.gem("chest_rune", 1.15, R, center=(0, 27.8, 3.95), depth=0.5, crown=0.3, sides=6, axis="z", part="chest_plate")
    for sx in (1, -1):                                       # a gold ridge from the collar to the belt on each side of the gem
        bl.tube(f"ridge_{sx}", [(1.3 * sx, 32.6, 3.5), (2.2 * sx, 28.0, 3.7), (1.5 * sx, 22.6, 3.4)], 0.28, G, sides=4, sub=3, part="chest_plate")
    # tassets: three hanging plates over the hips (they follow the torso)
    for i, x in enumerate((-3.0, 0.0, 3.0)):
        bl.plate(f"tasset_{i}", [(x - 1.35, 20.0), (x + 1.35, 20.0), (x + 1.1, 13.6 - abs(x) * 0.25), (x - 1.1, 13.6 - abs(x) * 0.25)], 3.0, 3.5, A, part="tasset")
    bl.plate("tasset_back", [(-2.4, 20.0), (2.4, 20.0), (2.0, 12.0), (-2.0, 12.0)], -3.5, -3.0, A, part="tasset")
    # pauldrons: a dome with a gold rim, a lower lame, and spikes
    for sx_name in ("l",):
        d = sloft("pauldron_l", "pauldron_l", [(34.0, 1.9, 1.9, 8.4, 0), (33.3, 3.3, 3.2, 8.5, 0), (31.8, 3.9, 3.5, 8.7, 0), (30.2, 3.3, 3.1, 8.6, 0)], A, n=12, caps=(True, False))
        bl.tube("pauldron_rim_l", [(8.7 + 3.9 * math.cos(a), 30.4, 3.5 * math.sin(a)) for a in [i * math.pi / 5 for i in range(10)]] + [(8.7 + 3.9, 30.4, 0.0)], 0.35, G, sides=4, sub=1,
                part="pauldron_l")
        sloft("pauldron_lame_l", "pauldron_l", [(30.0, 3.1, 2.9, 8.6, 0), (27.6, 2.6, 2.5, 8.5, 0)], A, n=12, caps=(False, False))
        be.shard("pauldron_spike_l", "pauldron_l", (9.4, 34.0, 0.0), (12.0 + (2.5 if form else 0), 38.5 + (3 if form else 0), -1.0), 1.1, A)
        be.shard("pauldron_spike2_l", "pauldron_l", (11.0, 32.8, -1.0), (15.0 + (2 if form else 0), 34.5 + (2 if form else 0), -2.4), 0.9, A)
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == "pauldron_l"]:
        mirror_part(o, "pauldron_r")
    # bracers: a long guard on each forearm with a fin
    br = sloft("bracer_l", "bracer_l", [(23.4, 2.35, 2.4, 8.25, 0.1), (20.5, 2.2, 2.3, 8.3, 0.2), (18.3, 1.75, 1.9, 8.4, 0.3)], A, n=10, caps=(True, True))
    bl.tube("bracer_band_l", [(8.25 + 2.5 * math.cos(a), 21.8, 0.2 + 2.5 * math.sin(a)) for a in [i * math.pi / 5 for i in range(11)]], 0.3, G, sides=4, sub=1, part="bracer_l")
    bl.plate("bracer_fin_l", [(10.4, 23.0), (12.8, 21.0), (10.8, 18.6)], -0.25, 0.25, A, part="bracer_l")
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == "bracer_l"]:
        mirror_part(o, "bracer_r")
    # greaves: knee cop with a spike, a shin guard, a boot with a pointed toe
    sloft("greave_l", "greave_l", [(10.6, 2.65, 2.8, 2.75, 0.1), (7.0, 2.35, 2.6, 2.75, 0.4), (3.0, 2.3, 2.55, 2.75, 0.1), (0.8, 2.1, 2.5, 2.75, 0.2)], A, n=12, caps=(False, False))
    sloft("knee_l", "greave_l", [(12.2, 2.4, 2.4, 2.75, 1.0), (10.6, 3.0, 3.1, 2.75, 1.4), (9.2, 2.5, 2.5, 2.75, 1.0)], A, n=10)
    be.shard("knee_spike_l", "greave_l", (2.75, 10.8, 3.2), (2.75, 11.8, 6.6), 0.9, G)
    sloft("boot_l", "greave_l", [(0.0, 2.0, 4.0, 2.75, 1.6), (1.6, 2.1, 3.9, 2.75, 1.5), (2.4, 1.9, 2.8, 2.75, 0.6)], A, n=10)
    be.shard("boot_toe_l", "greave_l", (2.75, 1.0, 5.2), (2.75, 0.5, 8.0), 0.9, A)
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == "greave_l"]:
        mirror_part(o, "greave_r")
    # crown and horn guards
    band = sloft("crown_band", "crown", [(41.2, 4.95, 4.85, 0, 0.2), (42.9, 4.9, 4.8, 0, 0.0)], G, n=14, caps=(False, False))
    bl.solidify(band, 0.4)
    for i, a in enumerate((-0.9, 0.0, 0.9)):
        be.shard(f"crown_spike_{i}", "crown", (5.0 * math.sin(a), 42.4, 4.8 * math.cos(a) + 0.2), (5.4 * math.sin(a), 46.0 + (1 - abs(a)) * 1.4, 4.9 * math.cos(a) + 0.5), 0.55, G)
    bl.gem("crown_gem", 0.8, R, center=(0, 42.2, 5.2), depth=0.5, crown=0.3, sides=6, axis="z", part="crown")
    for sx in (1, -1):
        bl.tube(f"horn_guard_{sx}", [(3.1 * sx, 42.9, 0.5), (4.1 * sx, 43.8, 0.3)], 1.5, G, sides=6, sub=2, part="crown")
    return


def third_eye_and_veins(M, form):
    bl.gem("third_eye", 1.3, M["third"], center=(0, 42.6, 4.55), depth=0.5, crown=0.3, sides=6, axis="z", part="head")
    for sx in (1, -1):                                       # shoulder spikes in the bone, glowing veins down the arms
        be.shard(f"shoulder_bone_{sx}", "torso", (5.2 * sx, 32.0, -1.0), (8.0 * sx, 36.5, -3.0), 1.0, M["horn"])
        bl.tube(f"vein_arm_{sx}", [(8.0 * sx, 30.0, 1.9), (8.4 * sx, 25.0, 1.8), (8.6 * sx, 19.0, 1.5)], 0.16, M["rune"], sides=4, sub=3, part="arm_l" if sx > 0 else "arm_r", detail=True)


def halo(M):
    """The Gates: a ring of long horns turning behind the head (part 'halo', follows the head)."""
    for i in range(8):
        a = math.pi * 2 * i / 8
        base = (9.2 * math.cos(a), 40.0 + 9.2 * math.sin(a), -5.0)
        tip = (14.5 * math.cos(a), 40.0 + 14.5 * math.sin(a), -5.5)
        be.shard(f"halo_horn_{i}", "halo", base, tip, 0.8, M["horn"])
    bl.tube("halo_ring", [(9.2 * math.cos(i * 2 * math.pi / 12), 40.0 + 9.2 * math.sin(i * 2 * math.pi / 12), -5.0) for i in range(13)], 0.4, M["rune"], sides=3, sub=1, part="halo")


def build(form):
    be.seed(41 + form)
    M = materials()
    body(M, form)
    if form < 2:
        wings(M, form)
    else:
        arms(M)
    armour(M, form)
    if form >= 1:
        third_eye_and_veins(M, form)
    if form == 2:
        halo(M)
    print("faces before export:", sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH" and not o.get("detail")))


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    only = argv[argv.index("--only") + 1] if "--only" in argv else None
    pv = trial or PREVIEW_DIR
    for form, eid in ((0, "lucifero_v2"), (1, "lucifero_v2_form2"), (2, "lucifero_v2_gates")):
        if only and only not in eid:
            continue
        bl.reset(samples=8 if quick else 20)
        build(form)
        previews = {os.path.join(pv, f"{eid}_blender.png"): (30, None), os.path.join(pv, f"{eid}_blender_back.png"): (200, None)}
        if form == 0:
            previews[os.path.join(pv, f"{eid}_blender_slam.png")] = (35, "slam")
        be.finish_entity(eid, RIG, glow=("A66BFF", 0.12, 0.55, 1.0), skin=1024 if not quick else 512, bake=2048 if not quick else 512, out_mesh=trial, out_skin=trial, previews=previews,
                         poses=POSES, cage=0.2)


if __name__ == "__main__":
    main(sys.argv[1:])

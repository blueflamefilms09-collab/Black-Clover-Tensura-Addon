"""Devil forms, group 1 (Black Clover x6, Berserk, Fairy Tail): player-scale devil bodies MADE IN BLENDER (docs/devil_forms/group1.md).

liebe, lucifero, beelzebub, astaroth, megicula, lucifugus, god_hand, etherious: eight upright humanoids about 1.8 to 2.0 blocks tall (header scale 1/16, so 1.0 = the player),
each with its own silhouette, horns, armour, wings, tail, markings and palette. Each is split into the roles torso head arm_l arm_r forearm_l forearm_r leg_l leg_r (+ wing_l wing_r tail)
that the game turns about their pivots (the same format as the boss bodies: tools/blender/bl_entity.py).

Run (Blender 5.x as a Python module, `pip install bpy pillow numpy`):
    python3 -B tools/blender/devil_group1.py [--only liebe,god_hand] [--trial DIR] [--quick]
Writes entity_meshes/devil_<id>(_lod).txt, textures/entity/boss_mesh/devil_<id>(_glow).png and docs/devil_forms/previews/<id>.png (all three go to DIR with --trial).

Model space: 1 unit = 1/16 block, y up, face +z, the devil's left = +x, feet on y = 0. Every builder creates its parts in the order torso, head, arm_l, arm_r, forearm_l, forearm_r,
leg_l, leg_r, wing_l, wing_r, tail (a parent part must appear before its children: be._check_rig).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Euler, Matrix, Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "devil_forms", "previews")


# ================================================================ rig
def make_rig(sx=6.0, shy=23.8, ex=6.6, ey=17.0, hipx=2.4, hipy=13.0, neck=25.4, wing=None, tail=None):
    """{part: (parent, pivot)}: legs are roots (as the boss bodies), everything else hangs from the torso (forearms from the arms)."""
    rig = {
        "torso": (None, (0.0, hipy, 0.0)),
        "head": ("torso", (0.0, neck, 0.0)),
        "arm_l": ("torso", (sx, shy, 0.0)),
        "arm_r": ("torso", (-sx, shy, 0.0)),
        "forearm_l": ("arm_l", (ex, ey, 0.3)),
        "forearm_r": ("arm_r", (-ex, ey, 0.3)),
        "leg_l": (None, (hipx, hipy, 0.0)),
        "leg_r": (None, (-hipx, hipy, 0.0)),
    }
    if wing:
        rig["wing_l"] = ("torso", tuple(wing))
        rig["wing_r"] = ("torso", (-wing[0], wing[1], wing[2]))
    if tail:
        rig["tail"] = ("torso", tuple(tail))
    return rig


# ================================================================ materials
def veined(name, base, line_rgb, scale=0.3, width=0.07, emit=None, strength=0.0, blot=None, rough=0.6, **kw):
    """A bl.mat whose colour is darkened (or painted) along flowing Voronoi-edge lines; with `emit` the lines glow (strength) and so feed the glow skin.
    blot = (scale, threshold) adds blots of the same colour."""
    m = bl.mat(name, base, rough=rough, **kw)
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    co = N.new("ShaderNodeTexCoord").outputs["Object"]
    warp = N.new("ShaderNodeTexNoise")
    warp.inputs["Scale"].default_value = scale * 0.8
    warp.inputs["Detail"].default_value = 3.0
    L.new(co, warp.inputs["Vector"])
    vor = N.new("ShaderNodeTexVoronoi")
    vor.feature = "DISTANCE_TO_EDGE"
    vor.inputs["Scale"].default_value = scale
    L.new(warp.outputs[0], vor.inputs["Vector"])
    mask = bl._math(nt, "LESS_THAN", vor.outputs["Distance"], width)
    if blot:
        bn = N.new("ShaderNodeTexNoise")
        bn.inputs["Scale"].default_value = blot[0]
        L.new(co, bn.inputs["Vector"])
        mask = bl._math(nt, "MAXIMUM", mask, bl._math(nt, "GREATER_THAN", bn.outputs[0], blot[1]))
    mix = N.new("ShaderNodeMixRGB")
    mix.inputs[2].default_value = line_rgb
    L.new(mask, mix.inputs[0])
    src = bs.inputs["Base Color"].links
    if src:
        L.new(src[0].from_socket, mix.inputs[1])
    else:
        mix.inputs[1].default_value = base
    L.new(mix.outputs[0], bs.inputs["Base Color"])
    if emit is not None:
        bs.inputs["Emission Color"].default_value = emit
        L.new(bl._math(nt, "MULTIPLY", mask, strength), bs.inputs["Emission Strength"])
    return m


def glowm(name, rgb255, strength=2.2):
    """A pure emitter in a colour (a lit eye, a rune plate): the bake of it goes to the glow skin."""
    c = bl.rgb(*rgb255)
    return bl.mat(name, c, rough=0.3, emit=c, strength=strength, bevel=0.0)


# ================================================================ builders shared by the eight
def eloft(name, part, rows, n, mat, caps=(True, True), sub=0, e=2.0):
    """Rings up a body piece. rows = [(y, half width, half depth, x offset, z offset)]; e > 2 squares the section (a superellipse), n points a ring."""
    rings = []
    for r in rows:
        y, w, d, ox, oz = r[:5]
        ee = r[5] if len(r) > 5 else e
        ring = []
        for k in range(n):
            a = 2 * math.pi * k / n
            c, s = math.cos(a), math.sin(a)
            ring.append(Vector((ox + w * math.copysign(abs(c) ** (2 / ee), c), y, oz + d * math.copysign(abs(s) ** (2 / ee), s))))
        rings.append(ring)
    ob = bl.loft(name, rings, mat, caps, part=part)
    if sub:
        bl.subsurf(ob, sub)
    return ob


def limb(name, part, path, radii, n, mat, flat=1.0, sub=1):
    """A limb along a path of points with a radius per point (round-ish sections, one subdivision for the roundness)."""
    P = [Vector(p) for p in path]
    rings = []
    for i, p in enumerate(P):
        tan = (P[min(i + 1, len(P) - 1)] - P[max(i - 1, 0)]).normalized()
        side = Vector((0, 0, 1)).cross(tan)
        if side.length < 1e-6:
            side = Vector((1, 0, 0))
        side.normalize()
        fwd = tan.cross(side).normalized()
        r = radii[i]
        rings.append([p + side * (r * math.cos(2 * math.pi * k / n)) + fwd * (r * flat * math.sin(2 * math.pi * k / n)) for k in range(n)])
    ob = bl.loft(name, rings, mat, (True, True), part=part)
    if sub:
        bl.subsurf(ob, sub)
    return ob


def sym(build_left):
    """Runs build_left (it makes objects on the +x side, part names ending _l) and adds the mirrored copy of every new object with the part name ending _r."""
    sc = bpy.context.scene
    before = set(sc.collection.objects)
    build_left()
    for o in [o for o in sc.collection.objects if o not in before and o.type == "MESH"]:
        c = bl.mirror(o, "x", o.name + "_r")
        c["part"] = o["part"][:-2] + "_r" if o["part"].endswith("_l") else o["part"]


def leaf(name, part, base, axis, normal, length, width, thick, mat, shape=None, bend=0.0):
    """A flat, tapered plate (a blade, a feather, a strip): from `base` along `axis`, `width` across, `thick` through `normal`; bend curves it toward the normal."""
    shape = shape or [(0, -0.5), (0.25, -1.0), (0.7, -0.6), (1.0, 0.0), (0.7, 0.6), (0.25, 1.0), (0, 0.5)]
    ax = Vector(axis).normalized()
    nm = Vector(normal)
    nm = (nm - ax * nm.dot(ax)).normalized()
    sd = ax.cross(nm)
    top, bot = [], []
    for a, c in shape:
        p = Vector(base) + ax * (a * length) + sd * (c * width / 2) + nm * (bend * length * a * a)
        top.append(p + nm * thick / 2)
        bot.append(p - nm * thick / 2)
    n = len(shape)
    verts = bot + top
    faces = [(k, (k + 1) % n, (k + 1) % n + n, k + n) for k in range(n)]
    faces += [tuple(range(n - 1, -1, -1)), tuple(range(n, 2 * n))]
    return bl.obj(name, verts, faces, mat, part)


def hand(part, mat, claw, base, n=4, spread=0.9, length=3.6, curl=0.6, radius=0.38, thumb=True, palm=1.1):
    """A left hand (palm blob + fingers + thumb) hanging from the wrist at `base`; the caller mirrors it."""
    bx, by, bz = base
    pm = be.boulder(part + "_palm", part, (bx, by - 0.9, bz + 0.4), palm, mat, squash=(1.0, 1.1, 0.7), seg=(6, 4), jag=0.02)
    for k in range(n):
        o = (k - (n - 1) / 2) * spread * 0.6
        bl.tube(f"{part}_f{k}", [(bx + o, by - 1.2, bz + 0.5), (bx + o * 1.3, by - 1.2 - length * 0.55, bz + 0.5 + curl), (bx + o * 1.4, by - 1.2 - length, bz + 0.5 + curl * 2.0)],
                radius, claw, sides=4, sub=1, part=part, taper=(radius, radius * 0.25))
    if thumb:
        bl.tube(part + "_th", [(bx - 1.0, by - 0.7, bz + 0.9), (bx - 1.6, by - 2.0, bz + 1.4), (bx - 1.7, by - 3.0, bz + 2.0)], radius, claw, sides=4, sub=1, part=part, taper=(radius, radius * 0.25))
    return pm


def head_rows(y0, s=1.0):
    """The rows of a head 7 units tall from the neck at y0: a pointed chin, wide cheeks, a rounded skull (s scales the width)."""
    h = (0.0, 1.1, 3.4, 5.6, 7.0)
    w = (1.6, 2.8, 3.3, 3.1, 2.0)
    d = (1.9, 2.7, 3.0, 2.9, 2.0)
    z = (0.4, 0.5, 0.3, 0.0, -0.2)
    return [(y0 + h[i], w[i] * s, d[i] * s, 0, z[i]) for i in range(5)]


def face(skin, y=28.8, z=3.55, w=2.9, brow=True, nose=True, brow_mat=None, nose_len=1.0):
    """Brow ridge, nose and chin on the head (part head): the shapes that make a skull a face."""
    if brow:
        bl.tube("brow", [(-w, y + 1.4, z - 1.5), (-w * 0.5, y + 1.9, z - 0.2), (0, y + 1.7, z), (w * 0.5, y + 1.9, z - 0.2), (w, y + 1.4, z - 1.5)], 0.7, brow_mat or skin, sides=5, sub=2, part="head")
    if nose:
        bl.tube("nose", [(0, y + 1.0, z - 0.1), (0, y - 0.2, z + 0.9 * nose_len), (0, y - 1.0, z + 0.6 * nose_len)], 0.45, skin, sides=5, sub=2, part="head", taper=(0.5, 0.35))
    be.boulder("chin", "head", (0, y - 3.4, z - 0.9), 1.3, skin, squash=(1.0, 0.8, 0.8), seg=(6, 4), jag=0.0)


def chest(skin, y=21.2, w=2.1, z=2.0, r=2.1):
    """Pectorals and collar bones on the torso."""
    for sx in (1, -1):
        be.boulder(f"pec{sx}", "torso", (w * sx, y, z), r, skin, squash=(1.1, 0.75, 0.6), seg=(7, 5), jag=0.0)
        bl.tube(f"clav{sx}", [(0.6 * sx, y + 3.0, z - 0.3), (2.8 * sx, y + 3.2, z - 0.5), (4.6 * sx, y + 2.8, z - 1.0)], 0.5, skin, sides=5, sub=2, part="torso")


def arm_pair(skin, sx=6.0, up_r=(1.55, 1.25, 1.1), fo_r=(1.1, 1.25, 1.0), ex=6.6, flat=1.0, hand_fn=None, ey=17.0, wy=10.4):
    """arm_l/arm_r then forearm_l/forearm_r (the order the rig needs) from shoulder (sx, 24) to the elbow (ex, ey) to the wrist (ex + .3, wy)."""
    sym_parts = {}

    def upper():
        limb("arm_l", "arm_l", [(sx, 24.4, 0), (sx + 0.4, 20.5, 0.15), (ex, ey - 0.9, 0.3)], list(up_r), 7, skin, flat)
    sym(upper)

    def fore():
        limb("forearm_l", "forearm_l", [(ex, ey + 0.9, 0.3), (ex + 0.15, 13.6, 0.8), (ex + 0.3, wy + 0.3, 1.1)], list(fo_r), 7, skin, flat)
        if hand_fn:
            hand_fn()
    sym(fore)


def leg_pair(skin, hipx=2.4, r=(2.0, 1.7, 1.35, 1.1), bend=0.7, foot=None, toe=4.6, sole=1.4, flat=1.0):
    """leg_l/leg_r: hip (hipx, 13.5) to the knee to the ankle, and a simple foot (a lofted wedge) unless `foot` draws one."""
    def one():
        limb("leg_l", "leg_l", [(hipx, 14.0, 0), (hipx + 0.1, 7.6, bend), (hipx + 0.1, 3.4, -0.2), (hipx + 0.1, 0.9, 0.2)], list(r), 7, skin, flat)
        if foot:
            foot()
        else:
            eloft("foot_l", "leg_l", [(0.0, 1.5, 2.2, hipx, 1.2), (1.6, 1.6, 2.5, hipx, 1.6 + toe * 0.1), (2.6, 1.3, 1.6, hipx, 0.2)], 8, skin, sub=0)
    sym(one)


def bat_wing(part_prefix, bone, memb, arm_pts, tips, outline, z=-6.0, rarm=0.9, rfin=0.5, thick=0.35, curl=2.0):
    """A bat wing for the +x side: a bone arm, one finger to each tip from the arm's end, and a membrane plate (outline in xy) between them."""
    bl.tube(part_prefix + "_arm", arm_pts, rarm, bone, sides=5, sub=3, part="wing_l", taper=(rarm, rarm * 0.35))
    end = Vector(arm_pts[-1])
    for k, tp in enumerate(tips):
        mid = ((end.x + tp[0]) / 2, (end.y + tp[1]) / 2 + curl, z)
        bl.tube(f"{part_prefix}_fi{k}", [tuple(end), mid, (tp[0], tp[1], z)], rfin, bone, sides=5, sub=2, part="wing_l", taper=(rfin, rfin * 0.25))
    bl.plate(part_prefix + "_memb", outline, z - thick / 2, z + thick / 2, memb, part="wing_l")


def hn(name, part, pts, r, mat, taper=(1.0, 0.12), sides=5, sub=2):
    return bl.tube(name, pts, r, mat, sides=sides, sub=sub, part=part, taper=(r * taper[0], r * taper[1]))


# ================================================================ 1. LIEBE
def liebe():
    """Liebe, the Anti-Magic devil (Black Clover). Canon basis (docs/devil_forms/group1.md): the English Wikipedia character list for the role (Asta's devil, the Anti-Magic source, Asta's adopted
    brother) and memory of the anime for the look: a black-bodied devil with white eyes and a wide sharp-toothed grin, horns, ragged dark wings (Asta's Devil Union shows black wings and horns).
    Nothing else is invented: no crown, no seals, no tail (second revision: the first draft had a thorn crown, white seals and a tail that no source supports)."""
    be.seed(101)
    rig = make_rig(sx=5.6, shy=23.6, ex=6.1, hipx=2.2, neck=25.4, wing=(2.6, 22.0, -2.8))
    blk = veined("lie_skin", bl.rgb(34, 34, 38), bl.rgb(18, 18, 22), scale=0.25, width=0.05, rough=0.42, bump="leather", bscale=0.5, bstr=0.15, bevel=0.15, groove=bl.rgb(20, 20, 24), gamount=0.4)
    cloth = bl.mat("lie_cloth", bl.rgb(26, 26, 30), rough=0.8, bump="wrap", bscale=0.6, bstr=0.25, bevel=0.1, groove=bl.rgb(12, 12, 14))
    horn = bl.mat("lie_horn", bl.rgb(20, 20, 24), rough=0.35, bump="rings", bscale=1.4, bstr=0.3, bevel=0.1, groove=bl.rgb(8, 8, 10), gamount=0.5)
    blade = bl.mat("lie_blade", bl.rgb(30, 30, 36), metallic=0.5, rough=0.35, bump="leather", bscale=1.0, bstr=0.2, bevel=0.1)
    white = glowm("lie_white", (225, 232, 255), 2.4)
    eloft("torso", "torso", [(12.6, 3.3, 2.3, 0, 0), (15.5, 2.9, 2.0, 0, 0), (19, 3.2, 2.3, 0, 0.1), (22.4, 4.4, 2.7, 0, 0.2), (24.6, 3.0, 2.3, 0, 0.1), (26.0, 1.6, 1.6, 0, 0)], 10, blk, sub=1)
    chest(blk)
    for k, x in enumerate((-3.2, -1.1, 1.1, 3.2)):                           # loincloth tatters
        leaf(f"tat{k}", "torso", (x, 14.2, 2.4), (0, -1, 0.15), (0, 0, 1), 8.5 - abs(k - 1.5) * 1.2, 2.0, 0.3, cloth, shape=[(0, -0.5), (0.4, -0.55), (1.0, 0.0), (0.4, 0.55), (0, 0.5)])
    eloft("head", "head", head_rows(25.4), 12, blk, sub=1)
    face(blk, y=29.0)
    leaf("grin", "head", (-2.6, 27.0, 3.3), (1, 0, 0), (0, 0, 1), 5.2, 0.9, 0.35, white, shape=[(0, 0), (0.2, -0.9), (0.4, 0.5), (0.6, -0.9), (0.8, 0.5), (1.0, 0)])
    for sx in (1, -1):
        leaf(f"eye{sx}", "head", (0.8 * sx, 29.7, 3.4), (1.0 * sx, 0.45, 0), (0, 0, 1), 2.2, 0.9, 0.35, white, shape=[(0, 0), (0.5, -1), (1.0, 0), (0.5, 1)])
        be.shard(f"ear{sx}", "head", (3.1 * sx, 29.2, 0.3), (7.3 * sx, 31.0, -1.4), 0.9, blk)
        hn(f"horn{sx}", "head", [(2.2 * sx, 31.6, 0.4), (3.6 * sx, 34.2, 0.0), (3.4 * sx, 37.0, -1.0)], 1.0, horn, taper=(1.0, 0.1))
    def claws():
        hand("forearm_l", blk, horn, (6.9, 10.2, 1.1), n=4, length=4.0)
    arm_pair(blk, sx=5.6, up_r=(1.35, 1.1, 1.0), fo_r=(1.0, 1.1, 0.9), ex=6.1, hand_fn=claws)
    leg_pair(blk, hipx=2.2, r=(1.7, 1.4, 1.1, 0.9))

    def cloak():                                                              # ragged dark wings: long tattered blades from the shoulder blades
        for k, (th, ln, w) in enumerate([(10, 20.0, 3.2), (26, 22.0, 3.4), (42, 20.5, 3.4), (58, 17.5, 3.2), (74, 13.5, 2.8)]):
            t = math.radians(th)
            d = Vector((math.sin(t), -math.cos(t), -0.22))
            leaf(f"bl{k}", "wing_l", (3.0 + 0.5 * k, 23.2 - 0.7 * k, -3.0 - 0.15 * k), d, (0, 0, 1), ln, w, 0.3, blade, bend=0.1)
        bl.tube("wing_bone", [(2.6, 23.4, -2.8), (7.0, 26.6, -4.0), (11.6, 26.2, -5.0)], 0.0, horn, sides=5, sub=3, part="wing_l", taper=(0.55, 0.3))
    sym(cloak)
    return rig, ("F0F0FF", 0.10, 0.55, 1.0)


# ================================================================ 2. LUCIFERO
def lucifero():
    """Lucifero, King of Devils, at player scale (Black Clover). Source: the boss's own build notes (docs/arc/lucifero.md) from earlier sessions; the wikis were unreachable.
    A tall thin figure with grey-violet skin and black patterns, long white hair, two pairs of horns, two pairs of hooked bat wings, thin bony hands, shoulder and elbow spikes, a third eye."""
    be.seed(202)
    rig = make_rig(sx=5.6, shy=24.4, ex=6.2, ey=17.6, hipx=2.3, neck=25.6, wing=(2.4, 23.0, -2.6))
    VIO = (150, 80, 255)
    gray = veined("luc_skin", bl.rgb(116, 108, 132), bl.rgb(16, 12, 22), scale=0.3, width=0.07, blot=(0.2, 0.66), rough=0.6, bump="leather", bscale=0.4, bstr=0.2, bevel=0.15,
                  groove=bl.rgb(62, 56, 76), gamount=0.5)
    hair = bl.mat("luc_hair", bl.rgb(236, 236, 246), rough=0.55, bump="brushed", bscale=2.0, bstr=0.3, bevel=0.1, groove=bl.rgb(150, 150, 170), gamount=0.6)
    horn = bl.mat("luc_horn", bl.rgb(52, 44, 60), rough=0.4, bump="rings", bscale=1.6, bstr=0.35, bevel=0.1, groove=bl.rgb(20, 16, 26), gamount=0.5)
    wingm = bl.mat("luc_wing", bl.rgb(60, 34, 92), rough=0.7, bump="leather", bscale=0.6, bstr=0.3, bevel=0.1, groove=bl.rgb(24, 12, 40), gamount=0.6)
    bone = bl.mat("luc_bone", bl.rgb(82, 72, 92), rough=0.5, bevel=0.1)
    cloth = bl.mat("luc_cloth", bl.rgb(34, 26, 46), rough=0.8, bump="wrap", bscale=0.6, bstr=0.3, bevel=0.1, groove=bl.rgb(14, 10, 22))
    eye = glowm("luc_eye", VIO, 2.2)
    third = glowm("luc_third", (230, 200, 255), 2.4)
    # ---- torso: narrow chest, long neck, bony shoulders, ribs that glow
    eloft("torso", "torso", [(12.6, 3.2, 2.3, 0, 0), (15.8, 2.7, 2.0, 0, 0), (19.5, 3.2, 2.3, 0, 0.1), (23.0, 4.6, 2.6, 0, 0.2), (25.2, 3.0, 2.2, 0, 0.3), (27.4, 1.6, 1.6, 0, 0.4)], 10, gray, sub=1)
    chest(gray, y=21.8, w=1.7, r=1.3)
    for sx in (1, -1):
        for k in range(3):
            bl.tube(f"rib{sx}{k}", [(0.7 * sx, 21.8 - 2.0 * k, 2.55), (3.0 * sx, 21.8 - 2.0 * k, 2.35), (4.5 * sx, 22.1 - 2.0 * k, 1.0)], 0.3, bone, sides=4, sub=1, part="torso")
        be.shard(f"sps{sx}a", "torso", (5.4 * sx, 25.0, 0), (8.6 * sx, 30.8, -1.0), 1.25, horn)
        be.shard(f"sps{sx}b", "torso", (5.6 * sx, 24.6, -0.8), (10.4 * sx, 28.4, -3.0), 1.05, horn)
    sk = eloft("skirt", "torso", [(8.0, 4.6, 3.6, 0, 0), (11.0, 4.0, 3.2, 0, 0), (14.0, 3.7, 2.7, 0, 0)], 10, cloth, caps=(False, False))
    bl.solidify(sk, 0.4)
    bl.plate("sash", [(-3.6, 12.6), (3.6, 12.6), (3.6, 14.0), (-3.6, 14.0)], -2.5, 2.5, bone, part="torso")
    # ---- head: narrow face, long white hair, two pairs of horns, violet slits and a third eye
    eloft("head", "head", head_rows(25.6), 12, gray, sub=1)
    face(gray, y=29.0)
    for sx in (1, -1):
        bl.gem(f"eye{sx}", 0.7, eye, center=(1.6 * sx, 29.7, 3.5), depth=0.5, crown=0.3, sides=6, axis="z", rx=1.0, ry=0.4, part="head")
        hn(f"hornA{sx}", "head", [(2.8 * sx, 32.2, 0.4), (4.9 * sx, 34.0, -0.2), (6.2 * sx, 37.3, -1.2), (5.4 * sx, 40.2, -2.2)], 1.05, horn)
        hn(f"hornB{sx}", "head", [(3.1 * sx, 30.0, -0.2), (6.4 * sx, 29.6, -0.8), (8.6 * sx, 27.6, -0.4), (9.0 * sx, 24.8, 0.6)], 1.0, horn, taper=(1.0, 0.12))
        be.boulder(f"ear{sx}", "head", (3.4 * sx, 29.4, 0.2), 0.95, gray, squash=(1.4, 1.0, 0.6), seg=(5, 3), jag=0.03)
    bl.gem("third_eye", 0.95, third, center=(0, 31.6, 3.2), depth=0.5, crown=0.3, sides=6, axis="z", part="head")
    eloft("hair_cap", "head", [(31.2, 3.4, 3.4, 0, -0.4), (32.8, 3.4, 3.3, 0, -0.1), (34.0, 2.2, 2.2, 0, -0.1)], 8, hair)
    for k in range(5):                                                        # long hair from the crown to the middle of the back
        a = (k - 2) * 0.5
        x0 = 3.0 * a / 1.2
        bl.tube(f"hair{k}", [(x0, 33.0, 0.0), (x0 * 1.3, 28.0, -3.4), (x0 * 1.45, 20.0, -4.4), (x0 * 1.3, 11.5 + abs(k - 2) * 1.2, -3.6)], 1.0, hair, sides=4, sub=2, part="head", taper=(1.0, 0.25))

    def bony():
        for k in range(4):
            bl.tube(f"fi{k}", [(6.6 + (k - 1.5) * 0.7, 10.6, 1.4), (6.8 + (k - 1.5) * 0.9, 7.4, 2.2), (6.8 + (k - 1.5) * 1.0, 4.6, 3.0)], 0.38, bone, sides=4, sub=2, part="forearm_l", taper=(0.38, 0.12))
        bl.tube("th", [(6.0, 10.8, 2.2), (5.0, 8.2, 3.2), (4.6, 6.4, 3.6)], 0.38, bone, sides=4, sub=2, part="forearm_l", taper=(0.38, 0.12))
        be.shard("elbow", "forearm_l", (6.4, 17.4, -0.8), (7.6, 20.4, -4.4), 0.95, horn)
    arm_pair(gray, sx=5.6, up_r=(1.5, 1.2, 1.05), fo_r=(1.05, 1.25, 0.95), ex=6.2, ey=17.6, wy=10.6, hand_fn=bony)
    leg_pair(gray, hipx=2.3, r=(1.9, 1.5, 1.2, 1.0))

    def wings():
        up = [(2.4, 24.5, -2.6), (8.2, 30.0, -4.4), (15.0, 37.0, -5.4), (20.5, 44.0, -5.8)]
        bat_wing("wing_l_up", bone, wingm, up, [(26.0, 38.0, 0), (24.0, 29.0, 0), (17.0, 22.0, 0)],
                 [(8.2, 30.0), (20.5, 44.0), (26.0, 38.0), (21.6, 33.8), (24.0, 29.0), (19.6, 25.6), (17.0, 22.0), (11.0, 24.0), (4.0, 22.0)], z=-5.4)
        low = [(2.4, 22.0, -2.6), (8.0, 22.6, -3.6), (13.0, 25.6, -4.0)]
        bat_wing("wing_l_lo", bone, wingm, low, [(17.0, 28.0, 0), (15.6, 18.6, 0), (10.0, 12.6, 0)],
                 [(8.0, 22.6), (13.0, 25.6), (17.0, 28.0), (14.4, 24.0), (15.6, 18.6), (12.4, 16.0), (10.0, 12.6), (7.0, 15.6), (3.6, 17.2)], z=-3.8, rarm=0.7, rfin=0.4)
    sym(wings)
    return rig, ("B26BFF", 0.12, 0.55, 1.0)


# ================================================================ 3. BEELZEBUB
def beelzebub():
    """Beelzebub, ruler of the underworld, Spatial (Black Clover). Sources: wikis unreachable; no fixed canon look remembered, so an original design in the series' style, built on the
    name's 'lord of the flies': a broad blue chitin-armoured devil with a compound-eyed helm, two long antenna horns, four folded arms, two pairs of glassy insect wings and a broken ring of space shards."""
    be.seed(303)
    rig = make_rig(sx=7.0, shy=23.4, ex=7.4, ey=16.8, hipx=2.8, hipy=12.4, neck=25.2, wing=(2.6, 22.0, -3.2))
    CY = (90, 170, 255)
    chit = bl.mat("bee_chitin", bl.rgb(34, 44, 120), metallic=0.5, rough=0.35, bump="scales", bscale=1.0, bstr=0.3, bevel=0.15, groove=bl.rgb(14, 18, 60), gamount=0.6)
    plate = bl.mat("bee_plate", bl.rgb(60, 82, 210), metallic=0.7, rough=0.28, bump="hammer", bscale=0.8, bstr=0.2, bevel=0.15, groove=bl.rgb(28, 40, 130), gamount=0.4)
    dark = bl.mat("bee_dark", bl.rgb(14, 16, 40), metallic=0.4, rough=0.5, bump="leather", bscale=0.6, bstr=0.2, bevel=0.1, groove=bl.rgb(6, 8, 20))
    wingm = bl.mat("bee_wing", bl.rgb(70, 110, 220), rough=0.2, bevel=0.1)
    glow = glowm("bee_glow", CY, 2.4)
    # ---- torso: broad shoulders, a segmented abdomen of overlapping plates
    eloft("torso", "torso", [(11.8, 3.6, 2.6, 0, 0), (14.5, 3.4, 2.4, 0, 0), (18.0, 3.9, 2.8, 0, 0.1), (21.5, 5.4, 3.3, 0, 0.2), (24.2, 4.6, 2.9, 0, 0.1), (25.4, 2.0, 2.0, 0, 0)], 10, dark, sub=1)
    for k, (y, w) in enumerate([(13.4, 3.9), (16.0, 4.0), (18.6, 4.4), (21.2, 5.6)]):          # overlapping chest and belly plates (a beetle's carapace)
        eloft(f"seg{k}", "torso", [(y - 1.4, w - 0.3, 2.9, 0, 0.35), (y, w, 3.3, 0, 0.4), (y + 1.2, w - 0.6, 3.0, 0, 0.3)], 10, chit if k % 2 else plate, caps=(False, False), sub=0, e=2.6)
    bl.plate("rift1", [(-0.3, 14.4), (0.3, 14.4), (0.9, 18.6), (-0.4, 22.6), (0.2, 23.4), (0.3, 18.6)], 3.2, 3.5, glow, part="torso")
    bl.plate("rift2", [(0.3, 19.0), (3.2, 20.4), (3.0, 20.8), (0.2, 19.6)], 3.2, 3.5, glow, part="torso")
    bl.plate("rift3", [(-0.3, 17.2), (-3.0, 16.0), (-3.2, 16.4), (-0.2, 17.8)], 3.2, 3.5, glow, part="torso")
    for sx in (1, -1):                                                        # huge layered pauldrons of spikes, and the second, folded pair of arms
        for k, (r, h, dz) in enumerate([(3.2, 0.0, 0.0), (2.7, 1.4, -0.2), (2.2, 2.6, -0.3)]):
            bl.lathe(f"pa{sx}{k}", [(r, 0.0), (r * 0.8, 0.9), (r * 0.25, 1.5)], chit if k % 2 == 0 else plate, sides=9, part="torso", origin=(6.4 * sx, 24.2 + h, dz), squash=0.85, caps=(False, True))
        for k, (ox, h) in enumerate([(2.0, 0.0), (0.0, 1.3), (-2.0, 0.0)]):
            be.shard(f"pas{sx}{k}", "torso", ((6.6 + ox * 0.4) * sx, 25.8 + h * 0.3, ox * 0.5), ((8.6 + ox * 0.4) * sx, 29.4 + h, ox * 0.9), 0.85, plate)
        bl.tube(f"xarm{sx}", [(3.6 * sx, 21.2, 2.0), (5.0 * sx, 18.4, 3.6), (2.4 * sx, 17.0, 4.2)], 0.9, chit, sides=6, sub=3, part="torso", taper=(1.0, 0.7))
        for k in range(3):
            bl.tube(f"xclaw{sx}{k}", [(2.4 * sx, 17.0, 4.2), ((2.0 - 0.5 * (k - 1)) * sx, 15.8, 4.7), ((1.4 - 0.7 * (k - 1)) * sx, 14.8, 5.0)], 0.3, glow if k == 1 else plate, sides=4, sub=2, part="torso", taper=(0.35, 0.1))
    # ---- head: a helm with a compound-eyed face, mandibles, two long antenna horns, a broken ring of shards behind
    eloft("head", "head", head_rows(25.2), 12, chit, sub=1)
    for sx in (1, -1):
        be.boulder(f"cye{sx}", "head", (2.2 * sx, 29.2, 3.2), 1.7, glow, squash=(0.9, 1.25, 0.8), seg=(7, 5), jag=0.01)
        bl.tube(f"mand{sx}", [(0.9 * sx, 26.8, 3.6), (1.8 * sx, 25.4, 4.8), (0.6 * sx, 24.2, 5.4)], 0.65, plate, sides=5, sub=3, part="head", taper=(1.0, 0.2))
        hn(f"ant{sx}", "head", [(1.6 * sx, 31.8, 0.8), (3.2 * sx, 35.8, 2.2), (6.0 * sx, 38.6, 5.2), (9.4 * sx, 39.0, 8.0)], 0.7, plate, taper=(1.0, 0.1))
        be.shard(f"brow{sx}", "head", (3.4 * sx, 31.0, 1.8), (6.4 * sx, 33.6, 0.0), 1.0, chit)
    be.shard("crest", "head", (0, 31.8, -1.5), (0, 36.6, -5.4), 1.4, plate)
    for k in range(8):                                                        # the ring of broken space: floating shards in a circle behind the head
        a = 2 * math.pi * k / 8 + 0.2
        c = Vector((7.6 * math.cos(a), 29.0 + 7.6 * math.sin(a), -5.0))
        d = Vector((-math.sin(a), math.cos(a), 0))
        leaf(f"shard{k}", "head", c - d * 1.6, d, (0, 0, 1), 3.4 + (k % 3), 1.5, 0.4, glow if k % 2 == 0 else plate, shape=[(0, 0), (0.35, -0.8), (1.0, 0), (0.35, 0.8)])

    def claw_hand():
        hand("forearm_l", chit, plate, (7.7, 10.4, 1.1), n=3, spread=1.2, length=3.6, radius=0.5, palm=1.5)
        for k in range(3):
            be.shard(f"kn{k}", "forearm_l", (7.7 + 1.4 * (k - 1), 12.6, 1.3 + 0.3 * (k == 1)), (8.1 + 1.8 * (k - 1), 14.8, 1.4), 0.5, plate)
    arm_pair(chit, sx=7.0, up_r=(2.0, 1.6, 1.4), fo_r=(1.5, 1.8, 1.45), ex=7.4, ey=16.8, wy=10.6, hand_fn=claw_hand, flat=0.9)
    # ---- legs: heavy, chitin-plated, a spike on each knee
    def foot():
        eloft("foot_l", "leg_l", [(0.0, 1.9, 2.8, 2.9, 1.6), (1.8, 1.9, 3.0, 2.9, 2.0), (3.0, 1.5, 1.8, 2.9, 0.3)], 8, plate, sub=0)
        for k in range(3):
            be.shard(f"toe{k}", "leg_l", (2.9 + (k - 1) * 1.1, 0.9, 4.1), (2.9 + (k - 1) * 1.5, 0.5, 6.8), 0.55, plate)
        be.shard("kneesp", "leg_l", (2.9, 7.8, 2.0), (2.9, 9.0, 5.6), 1.0, plate)
    leg_pair(chit, hipx=2.8, r=(2.4, 2.0, 1.6, 1.3), bend=1.0, foot=foot)
    # ---- two pairs of glassy dragonfly wings with glowing veins
    def wings():
        for k, (tip, base, wd) in enumerate([((17.0, 38.0), (2.6, 23.2), 4.4), ((15.0, 27.0), (2.6, 20.6), 3.4)]):
            z = -3.4 - 0.9 * k
            ax = Vector((tip[0] - base[0], tip[1] - base[1], 0))
            nrm = Vector((-ax.y, ax.x, 0)).normalized()
            pts = [Vector((base[0], base[1], z)), Vector((base[0], base[1], z)) + ax * 0.5 + nrm * wd, Vector((tip[0], tip[1], z)), Vector((base[0], base[1], z)) + ax * 0.5 - nrm * wd * 0.8]
            bl.plate(f"wm{k}", [(p.x, p.y) for p in pts], z - 0.15, z + 0.15, wingm, part="wing_l")
            bl.tube(f"wv{k}", [tuple(pts[0]), tuple((pts[0] + pts[2]) / 2 + Vector((0, 0, 0.0))), tuple(pts[2])], 0.5, glow, sides=4, sub=2, part="wing_l", taper=(0.55, 0.2))
            for j, f in enumerate((0.3, 0.6)):
                a0 = pts[0] + (pts[2] - pts[0]) * f
                bl.tube(f"wvv{k}{j}", [tuple(a0), tuple(a0 + nrm * wd * 0.7 + ax * 0.12)], 0.0, plate, sides=4, sub=1, part="wing_l", taper=(0.3, 0.12))
    sym(wings)
    return rig, ("5A8CFF", 0.10, 0.55, 1.0)


# ================================================================ 4. ASTAROTH
def astaroth():
    """Astaroth, ruler of the underworld, Time (Black Clover). Sources: wikis unreachable; no fixed look remembered, so an original design: a tall robed lord in gold plate, a porcelain face
    mask with closed glowing eyes, long swept horns, a clock-face breastplate, a halo of gears with two hands, wings made of a fan of long clock-hand blades and a pendulum for a tail."""
    be.seed(404)
    rig = make_rig(sx=5.6, shy=24.0, ex=6.2, ey=17.2, hipx=2.3, neck=25.4, wing=(2.4, 22.8, -3.0), tail=(0.0, 13.0, -3.2))
    GOLD = (255, 222, 120)
    ivory = bl.mat("ast_skin", bl.rgb(226, 206, 160), rough=0.45, bump="leather", bscale=0.4, bstr=0.1, bevel=0.12, groove=bl.rgb(150, 124, 80), gamount=0.3)
    gold = bl.mat("ast_gold", bl.rgb(232, 180, 52), metallic=1.0, rough=0.22, bump="hammer", bscale=0.5, bstr=0.12, bevel=0.15, groove=bl.rgb(140, 96, 24), gamount=0.35)
    deep = bl.mat("ast_deep", bl.rgb(66, 46, 18), metallic=0.5, rough=0.4, bump="wrap", bscale=0.5, bstr=0.2, bevel=0.1, groove=bl.rgb(28, 18, 6))
    robe = bl.mat("ast_robe", bl.rgb(236, 224, 188), rough=0.75, bump="wrap", bscale=0.5, bstr=0.25, bevel=0.1, groove=bl.rgb(170, 140, 80), gamount=0.5)
    mask = bl.mat("ast_mask", bl.rgb(246, 242, 232), rough=0.25, bevel=0.2)
    glow = glowm("ast_glow", GOLD, 2.2)
    amber = glowm("ast_amber", (255, 150, 20), 2.6)
    # ---- torso: a slim armoured chest with a clock face, a gold gorget, a long split robe over the legs
    eloft("torso", "torso", [(12.6, 3.0, 2.2, 0, 0), (15.6, 2.7, 2.0, 0, 0), (19.0, 3.3, 2.4, 0, 0.1), (22.6, 4.2, 2.7, 0, 0.2), (24.6, 3.0, 2.3, 0, 0.1), (25.8, 1.6, 1.7, 0, 0)], 10, ivory, sub=1)
    eloft("cuirass", "torso", [(16.0, 3.4, 2.7, 0, 0.1), (19.0, 3.9, 3.0, 0, 0.25), (22.6, 4.8, 3.2, 0, 0.3), (24.2, 4.2, 2.8, 0, 0.2)], 10, gold, caps=(False, False), sub=1, e=2.5)
    bl.lathe("clock", [(2.2, 0.0), (2.3, 0.3), (2.1, 0.5), (0.001, 0.5)], mask, sides=16, part="torso", axis="z", origin=(0, 20.2, 3.2))
    bl.lathe("clock_rim", [(2.45, 0.0), (2.6, 0.4), (2.45, 0.8)], gold, sides=16, part="torso", axis="z", origin=(0, 20.2, 3.0), caps=(False, False))
    for k in range(8):                                                        # eight ticks of the dial
        a = 2 * math.pi * k / 8
        yy = 20.2 + (2.0 if k % 2 == 0 else 1.8)
        bl.plate(f"tick{k}", [(-0.12, 20.2 + 1.55), (0.12, 20.2 + 1.55), (0.12, yy), (-0.12, yy)], 3.75, 3.95, glow, part="torso")
        bpy.data.objects[f"tick{k}"].data.transform(Matrix.Translation((0, 20.2, 0)) @ Matrix.Rotation(-a, 4, "Z") @ Matrix.Translation((0, -20.2, 0)))
    bl.plate("hand_h", [(-0.15, 20.2), (0.15, 20.2), (0.1, 21.4), (-0.1, 21.4)], 3.95, 4.15, gold, part="torso")
    bl.plate("hand_m", [(-0.12, 20.2), (0.12, 20.2), (0.9, 21.4), (0.6, 21.6)], 3.95, 4.15, gold, part="torso")
    for sx in (1, -1):                                                        # layered gold pauldrons and a mantle of shoulder blades
        for k, (r, h) in enumerate([(3.0, 0.0), (2.4, 1.0)]):
            bl.lathe(f"pau{sx}{k}", [(r, 0.0), (r * 0.9, 0.7), (r * 0.3, 1.4)], gold, sides=9, part="torso", origin=(6.0 * sx, 24.2 + h, 0), squash=0.85, caps=(False, True))
        be.shard(f"paus{sx}", "torso", (6.0 * sx, 26.6, 0), (8.2 * sx, 30.6, -0.4), 1.0, gold)
    rb = eloft("robe", "torso", [(1.0, 5.4, 4.0, 0, 0), (5.0, 4.8, 3.6, 0, 0), (9.0, 4.3, 3.3, 0, 0), (13.4, 3.4, 2.6, 0, 0)], 12, robe, caps=(False, False))
    bl.solidify(rb, 0.4)
    for sx in (1, -1):                                                        # the front slit of the robe, fallen open: two long tabards
        leaf(f"tab{sx}", "torso", (0.8 * sx, 13.6, 3.0), (0.3 * sx, -1, 0.05), (0, 0, 1), 12.5, 2.8, 0.35, deep, shape=[(0, -0.5), (0.5, -0.55), (1.0, 0.0), (0.5, 0.55), (0, 0.5)])
    leaf("belt", "torso", (-3.4, 13.8, 2.55), (1, 0, 0), (0, 0, 1), 6.8, 1.4, 0.4, gold, shape=[(0, -0.5), (1, -0.5), (1, 0.5), (0, 0.5)])
    # ---- head: a porcelain mask, closed glowing eyes, a gear halo with two clock hands, long swept horns
    eloft("head", "head", head_rows(25.4), 12, ivory, sub=1)
    eloft("mask", "head", [(26.6, 1.5, 0.6, 0, 3.0), (28.4, 2.7, 0.8, 0, 3.0), (30.4, 2.8, 0.8, 0, 2.7), (31.4, 2.0, 0.5, 0, 2.4)], 8, mask, caps=(True, True))
    for sx in (1, -1):
        leaf(f"eye{sx}", "head", (0.6 * sx, 29.6, 3.78), (1.0 * sx, -0.25, 0), (0, 0, 1), 2.4, 0.8, 0.4, amber, shape=[(0, 0), (0.5, -1), (1.0, 0), (0.5, 0.4)])
        hn(f"horn{sx}", "head", [(2.6 * sx, 31.4, 0.6), (4.8 * sx, 33.0, -0.6), (6.8 * sx, 33.0, -3.6), (7.4 * sx, 30.2, -6.4), (6.2 * sx, 27.4, -7.6)], 1.0, gold, taper=(1.0, 0.1))
    bl.lathe("halo", [(7.6, 0.0), (8.4, 0.3), (8.4, 0.9), (7.6, 1.2)], gold, sides=24, part="head", axis="z", origin=(0, 30.0, -5.6), caps=(False, False))
    for k in range(8):
        a = 2 * math.pi * k / 8
        leaf(f"gear{k}", "head", (8.3 * math.cos(a), 30.0 + 8.3 * math.sin(a), -5.0), (math.cos(a), math.sin(a), 0), (0, 0, 1), 1.6, 1.4, 0.7, gold, shape=[(0, -0.5), (1, -0.35), (1, 0.35), (0, 0.5)])
    bl.lathe("halo_in", [(6.4, 0.0), (6.6, 0.2), (6.4, 0.4)], glow, sides=24, part="head", axis="z", origin=(0, 30.0, -5.5), caps=(False, False))
    leaf("hh", "head", (0, 30.0, -5.2), (0, 1, 0), (0, 0, 1), 4.6, 0.7, 0.4, glow, shape=[(0, -0.5), (0.8, -0.5), (1.0, 0), (0.8, 0.5), (0, 0.5)])
    leaf("hm", "head", (0, 30.0, -5.2), (1, 0.7, 0), (0, 0, 1), 6.2, 0.6, 0.4, glow, shape=[(0, -0.5), (0.8, -0.5), (1.0, 0), (0.8, 0.5), (0, 0.5)])

    def gauntlet():
        hand("forearm_l", gold, gold, (6.5, 10.4, 1.1), n=4, length=3.6)
        eloft("vamb", "forearm_l", [(11.2, 1.7, 1.7, 6.5, 1.0), (14.6, 1.9, 1.9, 6.4, 0.7), (17.2, 1.5, 1.5, 6.2, 0.4)], 8, gold, caps=(False, False), sub=1)
    arm_pair(ivory, sx=5.6, up_r=(1.4, 1.15, 1.0), fo_r=(1.0, 1.1, 0.9), ex=6.2, ey=17.2, wy=10.6, hand_fn=gauntlet)

    def boot():
        eloft("foot_l", "leg_l", [(0.0, 1.5, 2.3, 2.4, 1.3), (1.6, 1.6, 2.6, 2.4, 1.8), (2.6, 1.3, 1.7, 2.4, 0.3)], 8, gold, sub=0)
        for k, (y, r) in enumerate([(4.4, 1.9)]):
            bl.lathe("greave", [(1.7, 0), (1.9, 1.8), (1.5, 3.4)], gold, sides=8, part="leg_l", origin=(2.4, 1.6, 0), caps=(False, False))
    leg_pair(ivory, hipx=2.3, r=(1.7, 1.5, 1.2, 1.0), foot=boot)

    def fan():
        for k, (ang, ln) in enumerate([(118, 13.0), (100, 17.0), (80, 19.0), (60, 17.0), (40, 14.0), (20, 11.0)]):
            a = math.radians(ang)
            d = Vector((math.cos(a), math.sin(a), -0.14))
            leaf(f"ch{k}", "wing_l", (2.4 + 0.4 * k, 23.4 + 0.4 * k, -3.2 - 0.2 * k), d, (0, 0, 1), ln, 2.6, 0.45, gold, shape=[(0, -0.35), (0.65, -0.5), (1.0, 0.0), (0.65, 0.5), (0, 0.35)])
            leaf(f"cg{k}", "wing_l", (2.4 + 0.4 * k, 23.4 + 0.4 * k, -3.45 - 0.2 * k), d, (0, 0, 1), ln * 0.85, 0.5, 0.3, glow, shape=[(0, -0.5), (0.9, -0.4), (1.0, 0.0), (0.9, 0.4), (0, 0.5)])
    sym(fan)
    # ---- tail: a pendulum on a chain
    bl.tube("chain", [(0, 13.0, -3.2), (0, 9.5, -5.0), (0, 5.4, -5.6), (0, 3.6, -5.6)], 0.0, deep, sides=4, sub=3, part="tail", taper=(0.55, 0.45))
    bl.lathe("bob", [(0.001, 0.0), (2.5, 0.2), (2.7, 0.6), (2.5, 1.0), (0.001, 1.2)], gold, sides=16, part="tail", axis="z", origin=(0, 2.4, -6.2))
    bl.lathe("bob_in", [(0.001, 1.15), (1.5, 1.15), (1.5, 1.4), (0.001, 1.4)], glow, sides=12, part="tail", axis="z", origin=(0, 2.4, -6.2))
    return rig, ("FFE07A", 0.10, 0.55, 1.0)


# ================================================================ 5. MEGICULA
def megicula():
    """Megicula, the Curse-Warding devil (Black Clover). Sources: wikis unreachable; no fixed canon look remembered, an original design: a feminine curse-weaver in plum-wine skin with black curse
    patterns, wide ram horns curling round the ears, a mass of braided hair tendrils, a thorned collar, glowing seals on belly and chains, a torn veil skirt, small ragged wings and a thorned tail with a spade tip."""
    be.seed(505)
    rig = make_rig(sx=5.0, shy=23.4, ex=5.6, ey=17.0, hipx=2.5, hipy=13.0, neck=25.2, wing=(2.2, 22.4, -2.6), tail=(0.0, 13.2, -2.8))
    MG = (255, 70, 150)
    skin = veined("meg_skin", bl.rgb(116, 40, 82), bl.rgb(20, 6, 18), scale=0.32, width=0.06, blot=(0.22, 0.7), rough=0.5, bump="leather", bscale=0.4, bstr=0.12, bevel=0.12, groove=bl.rgb(70, 20, 50), gamount=0.4)
    black = bl.mat("meg_black", bl.rgb(24, 12, 26), rough=0.55, bump="rings", bscale=1.2, bstr=0.3, bevel=0.1, groove=bl.rgb(8, 4, 10), gamount=0.5)
    horn = bl.mat("meg_horn", bl.rgb(60, 30, 50), rough=0.35, bump="rings", bscale=1.8, bstr=0.4, bevel=0.1, groove=bl.rgb(24, 10, 20), gamount=0.55)
    hair = bl.mat("meg_hair", bl.rgb(30, 14, 34), rough=0.5, bump="brushed", bscale=2.0, bstr=0.3, bevel=0.1, groove=bl.rgb(60, 20, 56), gamount=0.5)
    veil = bl.mat("meg_veil", bl.rgb(80, 22, 66), rough=0.8, bump="wrap", bscale=0.5, bstr=0.25, bevel=0.1, groove=bl.rgb(30, 8, 28), gamount=0.5)
    iron = bl.mat("meg_iron", bl.rgb(54, 52, 62), metallic=0.9, rough=0.35, bump="hammer", bscale=0.8, bstr=0.2, bevel=0.12)
    seal = glowm("meg_seal", MG, 2.4)
    # ---- torso: a narrow waist, a curved bust line, a belly seal, chains
    eloft("torso", "torso", [(12.4, 3.5, 2.5, 0, 0), (14.6, 2.6, 2.0, 0, 0), (17.2, 2.5, 1.9, 0, 0), (20.4, 3.3, 2.4, 0, 0.2), (23.0, 4.0, 2.7, 0, 0.1), (24.8, 2.8, 2.2, 0, 0.1), (26.0, 1.4, 1.5, 0, 0)], 10, skin, sub=1)
    eloft("bandeau", "torso", [(19.3, 3.2, 2.5, 0, 0.2), (21.0, 3.45, 2.65, 0, 0.25), (22.6, 3.8, 2.7, 0, 0.2)], 10, black, caps=(False, False), sub=0)
    bl.lathe("belly_seal", [(1.5, 0.0), (1.6, 0.25), (1.4, 0.5)], seal, sides=12, part="torso", axis="z", origin=(0, 16.0, 1.85))
    for k in range(6):
        a = 2 * math.pi * k / 6
        bl.plate(f"sp{k}", [(-0.12, 18.0), (0.12, 18.0), (0.12, 18.6), (-0.12, 18.6)], 2.2, 2.4, seal, part="torso")
        bpy.data.objects[f"sp{k}"].data.transform(Matrix.Translation((0, 16.0, 0)) @ Matrix.Rotation(a, 4, "Z") @ Matrix.Translation((0, -16.0, 0)))
    bl.lathe("waist_ring", [(3.2, 0.0), (3.35, 0.4), (3.2, 0.8)], iron, sides=12, part="torso", origin=(0, 13.6, 0), squash=0.75, caps=(False, False))
    for k, x in enumerate((-2.4, -0.4, 1.8)):                                 # chains with seal talismans hanging from the belt
        bl.tube(f"chain{k}", [(x, 13.8, 2.5), (x + 0.3, 10.8, 3.2), (x + 0.1, 7.6, 3.0)], 0.0, iron, sides=4, sub=3, part="torso", taper=(0.3, 0.3))
        bl.plate(f"talis{k}", [(x - 0.8, 6.0), (x + 0.8, 6.0), (x + 0.8, 7.8), (x, 8.6), (x - 0.8, 7.8)], 2.6, 2.95, seal, part="torso")
    vl = eloft("veil", "torso", [(5.0, 6.4, 5.0, 0, 0), (8.0, 5.0, 4.0, 0, 0), (11.0, 4.3, 3.4, 0, 0), (13.6, 3.6, 2.7, 0, 0)], 12, veil, caps=(False, False))
    bl.solidify(vl, 0.35)
    for sx in (1, -1):
        be.shard(f"clv{sx}", "torso", (4.4 * sx, 23.9, 0), (6.4 * sx, 27.2, -1.0), 0.9, horn)
    # ---- head: a heart-shaped face, ram horns, a thorn collar, a tangle of braided hair
    eloft("head", "head", head_rows(25.2), 12, skin, sub=1)
    face(skin, y=28.8)
    for sx in (1, -1):
        bl.gem(f"eye{sx}", 0.75, seal, center=(1.4 * sx, 29.4, 3.4), depth=0.5, crown=0.3, sides=6, axis="z", rx=1.1, ry=0.5, part="head")
        hn(f"ram{sx}", "head", [(2.4 * sx, 30.8, 0.2), (5.6 * sx, 32.2, -0.6), (8.0 * sx, 30.0, -0.4), (8.4 * sx, 26.6, 1.0), (6.4 * sx, 25.2, 2.4), (4.6 * sx, 26.6, 2.2)], 1.5, horn, taper=(1.0, 0.18), sides=5)
        be.shard(f"ear{sx}", "head", (2.9 * sx, 28.4, 0.4), (6.6 * sx, 29.4, -0.8), 0.7, skin)
        be.shard(f"collar{sx}a", "head", (1.8 * sx, 25.4, 1.2), (3.6 * sx, 28.2, 1.8), 0.7, iron)
        be.shard(f"collar{sx}b", "head", (2.0 * sx, 25.2, -0.8), (4.6 * sx, 27.4, -1.4), 0.7, iron)
    bl.lathe("collar", [(1.9, 0.0), (2.2, 0.4), (2.0, 0.9)], iron, sides=10, part="head", origin=(0, 25.3, 0.3), caps=(False, False))
    eloft("hair_cap", "head", [(30.0, 3.1, 3.2, 0, -0.2), (31.6, 3.1, 3.1, 0, -0.1), (32.6, 2.0, 2.0, 0, -0.1)], 10, hair)
    for k in range(7):                                                        # tendrils: long braids down the back, two framing the face
        a = (k - 3) * 0.44
        x0 = 3.0 * math.sin(a * 1.4)
        end = 6.0 + abs(k - 3) * 1.4
        bl.tube(f"br{k}", [(x0, 32.0, -0.4), (x0 * 1.2, 27.0, -3.6 - abs(a)), (x0 * 1.45 + math.sin(k) * 0.6, 19.0, -4.4), (x0 * 1.2 + math.cos(k) * 0.8, end, -3.0)], 0.0, hair, sides=4, sub=2, part="head", taper=(0.85, 0.2))
    for sx in (1, -1):
        bl.tube(f"fr{sx}", [(2.8 * sx, 30.0, 2.4), (3.6 * sx, 26.0, 3.4), (3.0 * sx, 21.6, 3.8)], 0.0, hair, sides=4, sub=2, part="head", taper=(0.75, 0.2))

    def hand_fn():
        hand("forearm_l", skin, black, (5.9, 10.4, 1.1), n=4, length=3.8, radius=0.32)
        be.shard("vsp", "forearm_l", (5.4, 15.4, -0.6), (5.0, 17.4, -3.4), 0.7, iron)
        bl.lathe("vamb", [(1.15, 0.0), (1.3, 1.2), (1.1, 2.4)], black, sides=8, part="forearm_l", origin=(5.85, 12.4, 0.9), caps=(False, False))
    arm_pair(skin, sx=5.0, up_r=(1.3, 1.1, 0.95), fo_r=(0.95, 1.05, 0.85), ex=5.6, ey=17.0, wy=10.6, hand_fn=hand_fn)

    def heel():
        eloft("foot_l", "leg_l", [(0.0, 1.4, 1.8, 2.5, 0.6), (1.4, 1.5, 2.1, 2.5, 1.0), (2.8, 1.2, 1.4, 2.5, -0.2)], 8, black, sub=0)
        be.shard("hoof", "leg_l", (2.5, 1.0, 2.6), (2.5, 0.2, 5.2), 0.9, black)
    leg_pair(skin, hipx=2.5, r=(1.9, 1.5, 1.15, 0.95), foot=heel, bend=0.6)

    def wings():
        up = [(2.2, 23.6, -2.6), (7.0, 29.0, -3.8), (12.0, 33.4, -4.2)]
        bat_wing("wing_l", horn, veil, up, [(16.0, 32.0, 0), (15.0, 24.0, 0)],
                 [(7.0, 29.0), (12.0, 33.4), (16.0, 32.0), (13.0, 28.6), (15.0, 24.0), (11.0, 25.0), (7.0, 21.0), (3.0, 20.0)], z=-4.0, rarm=0.75, rfin=0.4)
    sym(wings)
    bl.tube("tail", [(0, 13.2, -2.8), (0, 9.6, -6.0), (0, 5.6, -9.6), (0, 3.0, -14.0), (0, 4.4, -18.4), (0, 8.4, -20.4)], 0.0, skin, sides=6, sub=3, part="tail", taper=(1.2, 0.35))
    for k, (y, z) in enumerate([(9.6, -6.0), (5.6, -9.6), (3.2, -13.6)]):
        be.shard(f"tthorn{k}", "tail", (0.0, y + 0.8, z - 0.8), (0.0, y + 3.4, z - 1.6), 0.55, iron)
    leaf("spade", "tail", (0, 8.6, -20.5), (0, 0.55, -0.45), (1, 0, 0), 4.6, 3.4, 0.4, seal, shape=[(0, 0), (0.3, -0.9), (0.7, -0.8), (1.0, 0), (0.7, 0.8), (0.3, 0.9)])
    return rig, ("FF4FA0", 0.12, 0.55, 1.0)


# ================================================================ 6. LUCIFUGUS
def lucifugus():
    """Lucifugus (Black Clover). Canon basis (docs/devil_forms/group1.md): the English Wikipedia character list says Lucifugus is one of the highest-ranking devils, serving Lucifero, Astaroth and
    Beelzebub, with Dark Magic (the mod's own attribute for it is Word / Kotodama, which the source does not give). The look is NOT remembered with confidence, so this is an original, restrained
    design: a very tall narrow figure in a high-collared indigo robe with lilac script, a blank white mask with one slit eye and a stitched mouth, two small horns, wide sleeves and wings of hanging
    paper strips. Second revision: the spiral horn and the floating tablets were removed."""
    be.seed(606)
    rig = make_rig(sx=5.2, shy=24.2, ex=6.0, ey=17.4, hipx=2.2, hipy=13.0, neck=25.6, wing=(2.2, 23.6, -3.0))
    LI = (190, 130, 255)
    robe = bl.mat("lug_robe", bl.rgb(38, 28, 82), rough=0.75, bump="wrap", bscale=0.5, bstr=0.25, bevel=0.1, groove=bl.rgb(18, 12, 44), gamount=0.55)
    trim = bl.mat("lug_trim", bl.rgb(210, 190, 250), rough=0.5, bump="cross", bscale=0.8, bstr=0.2, bevel=0.1, groove=bl.rgb(120, 96, 180), gamount=0.4)
    skin = bl.mat("lug_skin", bl.rgb(196, 188, 214), rough=0.5, bump="leather", bscale=0.4, bstr=0.1, bevel=0.1, groove=bl.rgb(120, 112, 140), gamount=0.3)
    mask = bl.mat("lug_mask", bl.rgb(244, 240, 250), rough=0.3, bevel=0.2)
    paper = bl.mat("lug_paper", bl.rgb(238, 232, 250), rough=0.85, bump="grain", bscale=0.4, bstr=0.1, bevel=0.05, groove=bl.rgb(176, 164, 206), gamount=0.3)
    horn = bl.mat("lug_horn", bl.rgb(220, 206, 244), rough=0.3, bump="rings", bscale=1.6, bstr=0.4, bevel=0.1, groove=bl.rgb(130, 110, 176), gamount=0.5)
    glow = glowm("lug_glow", LI, 2.4)
    # ---- torso: a high-collared robe, glowing script columns on the chest and the belt
    eloft("torso", "torso", [(12.4, 3.4, 2.6, 0, 0), (15.4, 3.0, 2.3, 0, 0), (19.0, 3.4, 2.6, 0, 0.1), (22.8, 4.2, 2.9, 0, 0.2), (24.8, 3.2, 2.5, 0, 0.1), (26.4, 2.2, 2.0, 0, 0)], 10, robe, sub=1, e=2.3)
    eloft("collar", "torso", [(24.6, 3.4, 2.8, 0, 0.0), (26.6, 3.2, 2.7, 0, 0.0), (29.0, 3.6, 3.0, 0, -0.2)], 10, trim, caps=(False, False), e=2.4)
    for sx, x in enumerate((-2.0, -0.7, 0.7, 2.0)):                           # columns of glyphs down the chest
        for k in range(4):
            y = 22.0 - k * 2.2
            bl.plate(f"gl{sx}{k}", [(x - 0.28, y), (x + 0.28, y), (x + 0.28, y + 1.5), (x - 0.28, y + 1.5)], 3.0, 3.25, glow, part="torso")
            if (sx + k) % 2 == 0:
                bl.plate(f"glb{sx}{k}", [(x - 0.55, y + 0.8), (x + 0.55, y + 0.8), (x + 0.55, y + 1.1), (x - 0.55, y + 1.1)], 3.0, 3.25, glow, part="torso")
    bl.plate("obi", [(-3.8, 13.4), (3.8, 13.4), (3.8, 15.0), (-3.8, 15.0)], -2.9, 3.0, trim, part="torso")
    sk = eloft("skirt", "torso", [(0.8, 5.4, 4.2, 0, 0), (4.6, 4.8, 3.7, 0, 0), (9.0, 4.3, 3.3, 0, 0), (13.4, 3.7, 2.8, 0, 0)], 12, robe, caps=(False, False))
    bl.solidify(sk, 0.4)
    bl.plate("hem", [(-5.0, 0.8), (5.0, 0.8), (5.0, 1.6), (-5.0, 1.6)], -4.2, 4.2, trim, part="torso")
    # ---- head: a blank white mask, one vertical slit eye, a stitched mouth, a long spiral horn and two small ones
    eloft("head", "head", head_rows(25.6), 12, skin, sub=1)
    eloft("mask", "head", [(26.8, 1.4, 0.5, 0, 3.0), (28.6, 2.7, 0.7, 0, 3.05), (30.8, 2.8, 0.7, 0, 2.8), (32.6, 2.1, 0.5, 0, 2.3)], 8, mask)
    leaf("slit", "head", (0, 29.2, 3.85), (0, 1, 0), (0, 0, 1), 3.4, 0.9, 0.3, glow, shape=[(0, 0), (0.5, -1), (1.0, 0), (0.5, 1)])
    for k in range(5):
        bl.plate(f"stitch{k}", [(-0.16 + (k - 2) * 0.6, 27.2), (0.16 + (k - 2) * 0.6, 27.2), (0.16 + (k - 2) * 0.6, 28.2), (-0.16 + (k - 2) * 0.6, 28.2)], 3.5, 3.8, glow if k % 2 == 0 else trim, part="head")
    for sx in (1, -1):
        hn(f"hs{sx}", "head", [(2.4 * sx, 32.2, 0.4), (4.4 * sx, 35.0, 0.0), (4.0 * sx, 38.0, -0.8)], 1.1, horn, taper=(1.0, 0.1))
        be.shard(f"ear{sx}", "head", (3.1 * sx, 29.8, 0.2), (6.2 * sx, 30.6, -1.0), 0.7, skin)

    def sleeve():                                                             # long hanging sleeves with a glowing cuff and thin hands
        sl = eloft("sleeve_l", "forearm_l", [(18.0, 1.6, 1.8, 6.2, 0.3), (13.0, 2.6, 2.4, 6.6, 0.8), (7.0, 3.4, 2.8, 7.2, 1.2), (3.6, 3.7, 2.8, 7.4, 1.2)], 8, robe, caps=(False, False))
        bl.solidify(sl, 0.35)
        bl.lathe("cuff", [(3.6, 0.0), (3.8, 0.4), (3.6, 0.8)], glow, sides=8, part="forearm_l", origin=(7.4, 3.4, 1.2), squash=0.8, caps=(False, False))
        hand("forearm_l", skin, trim, (7.0, 9.8, 1.2), n=4, length=3.4, radius=0.3)
    arm_pair(robe, sx=5.2, up_r=(1.5, 1.4, 1.3), fo_r=(1.2, 1.3, 1.1), ex=6.0, ey=17.4, wy=10.6, hand_fn=sleeve)
    leg_pair(robe, hipx=2.2, r=(1.8, 1.5, 1.2, 1.0))

    def strips():                                                             # wings: hanging paper strips (ofuda), long ones outermost, each with a glyph
        for k, (x, ln, tilt) in enumerate([(3.0, 21.0, 0.05), (5.6, 25.0, 0.16), (8.4, 28.0, 0.28), (11.2, 26.0, 0.4), (13.8, 22.0, 0.52), (16.0, 17.0, 0.64)]):
            d = Vector((math.sin(tilt), -math.cos(tilt), -0.08))
            base = (x, 29.0 - k * 0.5 - (0 if k < 2 else 0.8), -3.4 - 0.15 * k)
            leaf(f"st{k}", "wing_l", base, d, (0, 0, 1), ln, 2.5, 0.28, paper, shape=[(0, -0.5), (1.0, -0.5), (1.0, -0.2), (0.94, 0.0), (1.0, 0.2), (1.0, 0.5), (0, 0.5)], bend=-0.05)
            for j in range(3):
                p = Vector(base) + d * (ln * (0.2 + 0.25 * j)) + Vector((0, 0, -0.2))
                bl.plate(f"sg{k}{j}", [(p.x - 0.55, p.y - 0.9), (p.x + 0.55, p.y - 0.9), (p.x + 0.55, p.y + 0.9), (p.x - 0.55, p.y + 0.9)], p.z - 0.3, p.z - 0.15, glow, part="wing_l")
        bl.tube("rod", [(2.2, 29.4, -3.0), (8.0, 30.0, -3.8), (14.0, 28.8, -4.8), (17.6, 27.0, -5.4)], 0.0, horn, sides=5, sub=3, part="wing_l", taper=(0.55, 0.35))
    sym(strips)
    return rig, ("B06AFF", 0.10, 0.55, 1.0)


# ================================================================ 7. GOD HAND
def god_hand():
    """The God Hand (Berserk), one figure answering the Behelit (the mod's design: one body for the five). Canon basis (docs/devil_forms/group1.md): the English Wikipedia plot text (the Eclipse,
    the Behelit, Femto as the reborn Griffith) and memory of the manga for the look: Femto's tall horned helm with a swept crest, a dark spiked armour with a long cape, huge white feathered wings,
    and the Behelit, an egg with a face, hanging on the chest. The crimson is the mod's colour (trim, glow, the cape). Second revision: the first draft's black wings, crown of five spires and
    oversized chest egg were removed as invented."""
    be.seed(707)
    rig = make_rig(sx=7.2, shy=23.4, ex=7.8, ey=16.8, hipx=2.8, hipy=12.6, neck=25.2, wing=(3.0, 22.6, -3.6))
    RED = (210, 24, 40)
    flesh = bl.mat("gh_flesh", bl.rgb(60, 14, 20), rough=0.45, bump="leather", bscale=0.6, bstr=0.25, bevel=0.12, groove=bl.rgb(24, 6, 8), gamount=0.5)
    armor = bl.mat("gh_armor", bl.rgb(26, 14, 18), metallic=0.8, rough=0.28, bump="hammer", bscale=0.7, bstr=0.15, bevel=0.15, groove=bl.rgb(10, 4, 6), gamount=0.4)
    trim = bl.mat("gh_trim", bl.rgb(150, 20, 32), metallic=0.8, rough=0.3, bump="brushed", bscale=1.0, bstr=0.2, bevel=0.12)
    bone = bl.mat("gh_bone", bl.rgb(196, 176, 160), rough=0.45, bump="rings", bscale=1.4, bstr=0.3, bevel=0.1, groove=bl.rgb(96, 80, 70), gamount=0.5)
    cape = bl.mat("gh_cape", bl.rgb(92, 10, 18), rough=0.85, bump="wrap", bscale=0.4, bstr=0.3, bevel=0.1, groove=bl.rgb(34, 4, 8), gamount=0.6)
    feather = bl.mat("gh_feather", bl.rgb(236, 230, 222), rough=0.55, bump="brushed", bscale=2.0, bstr=0.3, bevel=0.08, groove=bl.rgb(150, 140, 136), gamount=0.5)
    glow = glowm("gh_glow", RED, 2.3)
    # ---- torso: a deep chest in organic armour, the Behelit set in it with a face
    eloft("torso", "torso", [(11.6, 3.8, 2.8, 0, 0), (14.6, 3.6, 2.6, 0, 0), (18.0, 4.3, 3.0, 0, 0.1), (21.6, 5.8, 3.5, 0, 0.2), (24.4, 5.0, 3.1, 0, 0.1), (25.6, 2.4, 2.2, 0, 0)], 10, flesh, sub=1)
    eloft("cuirass", "torso", [(14.8, 4.0, 3.0, 0, 0.1), (18.0, 4.7, 3.3, 0, 0.3), (22.0, 6.0, 3.8, 0, 0.4), (24.6, 5.2, 3.3, 0, 0.2)], 10, armor, caps=(False, False), sub=1, e=2.6)
    for k, y in enumerate((15.6, 17.6)):                                      # a banded belly
        eloft(f"band{k}", "torso", [(y - 0.7, 4.2, 3.2, 0, 0.2), (y, 4.5, 3.4, 0, 0.3), (y + 0.7, 4.2, 3.2, 0, 0.2)], 10, trim, caps=(False, False), e=2.6)
    for sx in (1, -1):                                                        # the Behelit, a small egg with a face, on a chain round the neck
        bl.tube(f"chain{sx}", [(2.6 * sx, 24.4, 2.4), (1.8 * sx, 22.0, 3.8), (0.0, 20.4, 4.2)], 0.0, trim, sides=4, sub=2, part="torso", taper=(0.3, 0.3))
        leaf(f"beye{sx}", "torso", (0.3 * sx, 20.2, 5.15), (0.8 * sx, -0.15, 0), (0, 0, 1), 0.9, 0.4, 0.3, armor, shape=[(0, 0), (0.5, -1), (1.0, 0), (0.5, 0.5)])
    egg = be.boulder("behelit", "torso", (0, 19.6, 4.0), 1.6, glow, squash=(0.75, 1.25, 0.7), seg=(8, 6), jag=0.0)
    leaf("bmouth", "torso", (-0.5, 18.4, 4.95), (1, 0, 0), (0, 0, 1), 1.0, 0.35, 0.3, armor, shape=[(0, 0), (0.5, -1), (1.0, 0), (0.5, 0.6)])
    for sx in (1, -1):                                                        # huge pauldrons, each topped with spikes
        for k, (r, h) in enumerate([(3.8, 0.0), (3.1, 1.3), (2.4, 2.4)]):
            bl.lathe(f"pa{sx}{k}", [(r, 0.0), (r * 0.9, 0.8), (r * 0.3, 1.5)], armor if k != 1 else trim, sides=9, part="torso", origin=(7.0 * sx, 23.6 + h, 0), squash=0.85, caps=(False, True))
        be.shard(f"pas{sx}a", "torso", (7.0 * sx, 27.0, 0), (8.6 * sx, 33.4, -1.2), 1.3, bone)
    cp = eloft("cape", "torso", [(25.0, 5.6, 1.2, 0, -3.0), (19.0, 6.4, 1.2, 0, -4.2), (11.0, 6.8, 1.2, 0, -5.0), (3.0, 7.2, 1.2, 0, -5.6)], 12, cape, caps=(False, False))
    bl.solidify(cp, 0.4)
    # ---- head: a horned helm with a swept crest and burning slits
    eloft("head", "head", head_rows(25.2), 12, armor, sub=1)
    for sx in (1, -1):
        leaf(f"slit{sx}", "head", (0.5 * sx, 29.2, 4.0), (1.0 * sx, 0.3, 0), (0, 0, 1), 2.6, 0.8, 0.35, glow, shape=[(0, 0), (0.5, -1), (1.0, 0), (0.5, 0.5)])
        hn(f"horn{sx}", "head", [(3.2 * sx, 30.6, 0.6), (6.0 * sx, 32.0, -0.8), (8.4 * sx, 35.4, -3.0), (8.6 * sx, 39.4, -5.0)], 1.4, bone, sides=7, taper=(1.0, 0.1))
        be.shard(f"cheek{sx}", "head", (3.4 * sx, 28.0, 2.6), (5.0 * sx, 25.4, 3.2), 1.0, trim)
    be.shard("crest", "head", (0, 32.0, 0.6), (0, 38.0, -4.2), 1.3, trim)
    be.shard("crest2", "head", (0, 31.6, 2.2), (0, 35.0, 0.6), 0.9, trim)
    be.shard("nasal", "head", (0, 30.0, 3.5), (0, 26.4, 5.0), 0.8, trim)

    def gaunt():
        hand("forearm_l", armor, trim, (7.9, 10.4, 1.1), n=4, length=3.6, radius=0.5, palm=1.5)
        eloft("vamb", "forearm_l", [(10.6, 2.1, 2.0, 7.8, 1.0), (14.2, 2.4, 2.3, 7.7, 0.7), (16.8, 1.8, 1.8, 7.7, 0.4)], 8, armor, caps=(False, False), sub=1)
        be.shard("ksp", "forearm_l", (7.6, 16.4, -1.4), (7.8, 18.8, -4.6), 1.0, bone)
    arm_pair(flesh, sx=7.2, up_r=(2.0, 1.7, 1.5), fo_r=(1.6, 1.8, 1.5), ex=7.8, ey=16.8, wy=10.6, hand_fn=gaunt, flat=0.95)

    def greaves():
        eloft("foot_l", "leg_l", [(0.0, 2.0, 2.8, 2.9, 1.5), (1.8, 2.0, 3.0, 2.9, 1.9), (3.0, 1.6, 1.8, 2.9, 0.2)], 8, armor, sub=0)
        eloft("greave", "leg_l", [(2.0, 2.0, 2.2, 2.9, 0.2), (5.0, 2.2, 2.4, 3.0, 0.5), (8.0, 2.4, 2.5, 3.0, 0.8)], 8, trim, caps=(False, False), sub=0)
        be.shard("kneesp", "leg_l", (2.9, 8.0, 2.0), (2.9, 9.0, 5.6), 1.1, bone)
    leg_pair(armor, hipx=2.8, r=(2.5, 2.1, 1.7, 1.4), bend=0.9, foot=greaves)

    def wings():
        for k, (ang, ln, w) in enumerate([(78, 15.0, 3.2), (62, 20.0, 3.6), (46, 23.0, 3.8), (30, 23.0, 3.6), (14, 20.0, 3.2), (-4, 16.0, 2.8)]):
            a = math.radians(ang)
            d = Vector((math.cos(a), math.sin(a), -0.30))
            base = (3.0 + 0.6 * k, 23.2 - 0.5 * k, -3.6 - 0.25 * k)
            leaf(f"fe{k}", "wing_l", base, d, (0, 0, 1), ln, w, 0.4, feather, shape=[(0, -0.4), (0.3, -0.5), (0.8, -0.5), (1.0, 0.0), (0.8, 0.45), (0.3, 0.5), (0, 0.4)], bend=0.06)
            leaf(f"fr{k}", "wing_l", (base[0], base[1], base[2] - 0.25), d, (0, 0, 1), ln * 0.92, 0.55, 0.3, glow, shape=[(0, -0.5), (0.9, -0.5), (1.0, 0.0), (0.9, 0.5), (0, 0.5)])
        bl.tube("wbone", [(3.0, 23.4, -3.4), (8.0, 27.6, -4.4), (13.0, 30.8, -5.4)], 0.0, bone, sides=5, sub=3, part="wing_l", taper=(0.75, 0.3))
    sym(wings)
    return rig, ("E01830", 0.10, 0.55, 1.0)


# ================================================================ 8. ETHERIOUS
def etherious():
    """An Etherious, a demon of the Books of Zeref (Fairy Tail). Canon basis (docs/devil_forms/group1.md): the English Wikipedia character list (Etherious are demons summoned from books written by
    Zeref; Tartaros' commander Mard Geer; E.N.D.) for the role, memory of the manga for the look: a lean demon in a long dark tailcoat with a high collar, horns, a tail, bat-like wings and spiky hair.
    There is no one fixed Etherious look, so this is a typical Tartaros-style demon, not a named one. Second revision: the invented chest seal (ring and star) was removed."""
    be.seed(808)
    rig = make_rig(sx=5.4, shy=23.6, ex=6.0, ey=17.2, hipx=2.4, neck=25.2, wing=(2.4, 22.8, -2.6), tail=(0.0, 13.0, -3.0))
    VI = (120, 80, 255)
    skin = veined("eth_skin", bl.rgb(52, 36, 86), bl.rgb(20, 12, 40), scale=0.34, width=0.05, blot=(0.2, 0.7), rough=0.5, bump="leather", bscale=0.5, bstr=0.15, bevel=0.12, groove=bl.rgb(30, 20, 56), gamount=0.4)
    coat = bl.mat("eth_coat", bl.rgb(22, 18, 38), rough=0.7, bump="wrap", bscale=0.5, bstr=0.25, bevel=0.1, groove=bl.rgb(8, 6, 16), gamount=0.55)
    trim = bl.mat("eth_trim", bl.rgb(150, 120, 230), metallic=0.8, rough=0.3, bump="brushed", bscale=1.0, bstr=0.2, bevel=0.12)
    horn = bl.mat("eth_horn", bl.rgb(34, 24, 56), rough=0.35, bump="rings", bscale=1.6, bstr=0.4, bevel=0.1, groove=bl.rgb(12, 8, 22), gamount=0.55)
    hair = bl.mat("eth_hair", bl.rgb(210, 200, 240), rough=0.5, bump="brushed", bscale=2.0, bstr=0.3, bevel=0.1, groove=bl.rgb(110, 96, 160), gamount=0.5)
    wingm = bl.mat("eth_wing", bl.rgb(52, 30, 100), rough=0.65, bump="leather", bscale=0.6, bstr=0.3, bevel=0.1, groove=bl.rgb(20, 10, 44), gamount=0.6)
    glow = glowm("eth_glow", VI, 2.3)
    # ---- torso: a bare chest under an open coat, Zeref's seal (ring and star) glowing on it
    eloft("torso", "torso", [(12.6, 3.3, 2.3, 0, 0), (15.6, 2.9, 2.1, 0, 0), (19.0, 3.4, 2.4, 0, 0.1), (22.6, 4.4, 2.7, 0, 0.2), (24.6, 3.0, 2.3, 0, 0.1), (25.8, 1.6, 1.7, 0, 0)], 10, skin, sub=1)
    chest(skin, y=21.0)
    ct = eloft("coat_l", "torso", [(22.8, 5.0, 3.0, 0, -0.2), (18.0, 5.0, 3.2, 0, -0.6), (13.0, 4.6, 3.4, 0, -1.0), (8.0, 4.6, 3.8, 0, -1.6)], 10, coat, caps=(False, False))
    bl.solidify(ct, 0.4)
    me = ct.data                                                              # cut the coat open at the front: drop the quads facing +z near the middle
    import bmesh
    bm = bmesh.new()
    bm.from_mesh(me)
    cut = [f for f in bm.faces if f.calc_center_median().z > 0.8 and abs(f.calc_center_median().x) < 1.8]
    bmesh.ops.delete(bm, geom=cut, context="FACES")
    bm.to_mesh(me)
    bm.free()
    for sx in (1, -1):
        leaf(f"lapel{sx}", "torso", (1.2 * sx, 24.6, 2.9), (0.55 * sx, -1, 0.1), (0, 0, 1), 7.8, 2.4, 0.4, trim, shape=[(0, -0.3), (0.5, -0.5), (1.0, 0.0), (0.5, 0.5), (0, 0.3)])
        be.shard(f"psp{sx}", "torso", (5.2 * sx, 24.2, 0), (7.4 * sx, 28.4, -0.4), 1.0, horn)
    tails = eloft("tails", "torso", [(12.6, 3.9, 2.8, 0, -1.0), (8.0, 4.2, 2.8, 0, -3.0), (2.0, 4.6, 2.2, 0, -4.6)], 8, coat, caps=(False, False))
    bl.solidify(tails, 0.4)
    # ---- head: sharp features, wild white hair swept up, two broad horns sweeping out and up, glowing slit eyes
    eloft("head", "head", head_rows(25.2), 12, skin, sub=1)
    face(skin, y=28.9)
    for sx in (1, -1):
        leaf(f"eye{sx}", "head", (0.7 * sx, 29.6, 3.4), (1.0 * sx, 0.45, 0), (0, 0, 1), 2.1, 0.8, 0.35, glow, shape=[(0, 0), (0.5, -1), (1.0, 0), (0.5, 0.5)])
        hn(f"horn{sx}", "head", [(2.4 * sx, 30.6, 0.2), (6.2 * sx, 31.6, -0.4), (10.0 * sx, 33.0, -1.0), (12.4 * sx, 36.4, -1.8), (12.6 * sx, 40.0, -2.4)], 1.5, horn, taper=(1.0, 0.1), sides=7)
        be.shard(f"ear{sx}", "head", (3.0 * sx, 28.8, 0.4), (6.0 * sx, 29.6, -0.6), 0.65, skin)
    for k in range(9):                                                        # spiky white hair swept up and back
        a = (k - 4) / 4.0
        be.shard(f"hair{k}", "head", (a * 2.6, 32.0 - abs(a) * 0.7, 0.4 - abs(a) * 0.6), (a * 4.6, 35.6 + (1 - abs(a)) * 3.4, -2.8 - abs(a) * 1.6 - 1.2), 1.1, hair)
    for k in range(2):
        be.shard(f"fringe{k}", "head", ((k - 0.5) * 1.8, 32.0, 2.4), ((k - 0.5) * 2.8, 30.6, 4.4), 0.7, hair)

    def rune_hand():
        hand("forearm_l", skin, trim, (6.2, 10.6, 1.1), n=4, length=3.8, radius=0.34)
        for k in range(3):
            bl.plate(f"fr{k}", [(5.6, 12.0 + 1.5 * k), (7.2, 12.0 + 1.5 * k), (7.2, 12.35 + 1.5 * k), (5.6, 12.35 + 1.5 * k)], 1.5, 1.8, glow, part="forearm_l")
        be.shard("ksp", "forearm_l", (5.8, 16.6, -0.8), (5.6, 19.0, -3.8), 0.85, horn)
    arm_pair(skin, sx=5.4, up_r=(1.4, 1.15, 1.0), fo_r=(1.0, 1.15, 0.9), ex=6.0, ey=17.2, wy=10.8, hand_fn=rune_hand)
    leg_pair(skin, hipx=2.4, r=(1.8, 1.5, 1.2, 1.0))

    def wings():
        up = [(2.4, 23.6, -2.6), (7.6, 28.8, -3.8), (12.6, 33.8, -4.4)]
        bat_wing("wing_l", horn, wingm, up, [(18.0, 33.0, 0), (17.0, 25.0, 0), (11.0, 20.0, 0)],
                 [(7.6, 28.8), (12.6, 33.8), (18.0, 33.0), (14.6, 29.6), (17.0, 25.0), (13.4, 24.0), (11.0, 20.0), (8.0, 22.6), (3.6, 20.6)], z=-4.0, rarm=0.8, rfin=0.45)
        for k in range(3):
            be.shard(f"wsp{k}", "wing_l", (7.6 + 4.0 * k, 28.8 + 2.5 * k, -3.8), (8.4 + 4.0 * k, 32.4 + 2.5 * k, -4.4), 0.5, glow)
    sym(wings)
    bl.tube("tail", [(0, 13.0, -3.0), (0, 10.0, -6.8), (0, 6.2, -10.0), (0, 3.4, -14.4), (0, 4.8, -18.6), (0, 9.0, -20.8)], 0.0, skin, sides=6, sub=3, part="tail", taper=(1.1, 0.35))
    leaf("arrow", "tail", (0, 9.0, -20.8), (0, 0.6, -0.4), (1, 0, 0), 5.2, 3.0, 0.45, glow, shape=[(0, -0.15), (0.15, -1.0), (0.55, -0.45), (1.0, 0.0), (0.55, 0.45), (0.15, 1.0), (0, 0.15)])
    return rig, ("7A50FF", 0.12, 0.55, 1.0)


DEVILS = {"liebe": liebe, "lucifero": lucifero, "beelzebub": beelzebub, "astaroth": astaroth, "megicula": megicula, "lucifugus": lucifugus, "god_hand": god_hand, "etherious": etherious}


# ================================================================ preview: a front three-quarter, tighter and brighter than bl_entity's boss previews
def render_front(low, names, rig, atlas, previews, poses, res=(640, 840), samples=40):
    sc = bpy.context.scene
    for o in sc.collection.objects:
        o.hide_render = True
    mat = be._preview_material(atlas)
    mat.node_tree.nodes["Principled BSDF"].inputs["Emission Strength"].default_value = 0.9
    w = bpy.data.worlds.new("preview")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs["Color"].default_value = (0.30, 0.31, 0.36, 1)
    w.node_tree.nodes["Background"].inputs["Strength"].default_value = 1.0
    sc.world = w
    key = bpy.data.objects.new("key", bpy.data.lights.new("key", "SUN"))
    key.data.energy = 4.5
    fill = bpy.data.objects.new("fill", bpy.data.lights.new("fill", "SUN"))
    fill.data.energy = 2.0
    sc.collection.objects.link(key)
    sc.collection.objects.link(fill)
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 50
    sc.collection.objects.link(cam)
    sc.camera = cam
    sc.render.resolution_x, sc.render.resolution_y = res
    sc.cycles.samples = samples
    sc.view_settings.view_transform = "Standard"
    sc.render.image_settings.file_format = "PNG"
    for path, (yaw, pose_name) in previews.items():
        me = low.data.copy()
        me.materials.clear()
        me.materials.append(mat)
        ob = bpy.data.objects.new("pv", me)
        sc.collection.objects.link(ob)
        lo, hi = Vector([min(v.co[i] for v in me.vertices) for i in range(3)]), Vector([max(v.co[i] for v in me.vertices) for i in range(3)])
        mid = (lo + hi) / 2
        height, width = hi.y - lo.y, hi.x - lo.x
        ob.matrix_world = Matrix.Rotation(math.radians(90), 4, "X")
        a = math.radians(yaw)
        c = Vector((mid.x, -mid.z, mid.y - height * 0.02))
        dist = max(height * 1.7, width * 1.6)
        cam.location = c + Vector((math.sin(a) * dist, -math.cos(a) * dist, height * 0.10))
        cam.rotation_euler = (c - cam.location).to_track_quat("-Z", "Y").to_euler()
        key.rotation_euler = Euler((math.radians(55), 0, a + math.radians(40)), "XYZ")
        fill.rotation_euler = Euler((math.radians(70), 0, a - math.radians(70)), "XYZ")
        sc.render.filepath = os.path.abspath(path)
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        bpy.ops.render.render(write_still=True)
        bpy.data.objects.remove(ob)
        bpy.data.meshes.remove(me)
        be._shrink(path)
        print("preview", path, os.path.getsize(path) // 1024, "KB")


be.render_previews = render_front                                             # bl_entity.finish_entity calls it by name


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    only = argv[argv.index("--only") + 1].split(",") if "--only" in argv else list(DEVILS)
    pv = trial or PREVIEW_DIR
    for did in only:
        bl.reset(samples=8 if quick else 20)
        rig, glow = DEVILS[did]()
        previews = {os.path.join(pv, f"{did}.png"): (28, None)}
        if "--nopreview" in argv:
            previews = {}
        elif not quick or "--back" in argv:
            previews[os.path.join(pv, f"{did}_back.png")] = (200, None)
        be.finish_entity("devil_" + did, rig, glow=glow, skin=1024 if not quick else 512, bake=2048 if not quick else 512,
                         out_mesh=trial, out_skin=trial, previews=previews, poses={}, cage=0.2, smooth=50.0)


if __name__ == "__main__":
    main(sys.argv[1:])

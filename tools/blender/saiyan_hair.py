"""Saiyan hair and the Saiyan tail, MADE IN BLENDER (docs/saiyan_hair.md).

Seven hair shapes (base, ss, ss2, ss3, ss4, god, ui) and one three-part tail. Every lock is a real tapered, flat-sectioned blade with thickness, every shape sits on a dome that covers the
player's head layer, the skins are baked by Cycles in a NEUTRAL light grey (the game multiplies the form's colour into it, so one shape serves every form that has it).

Model space (as the devil bodies, tools/blender/bl_entity.py): 1 unit = 1/16 block, y up, face +z, the feet on y = 0, the player's head is the box x -4..4, y 24..32, z -4..4 (the hat layer
is 0.5 larger), the body x -4..4, y 12..24, z -2..2. The files are read by BossMeshDraw like the devil bodies; the part names borrow the roles of MeshPose so the game can route them:
    hair   head = the dome and the standing locks (follows the head), tail = the long hanging locks of Super Saiyan 3 / 4 (follow the head, sway more)
    tail   tail = root segment (follows the body), wing_l = middle segment, wing_r = tip segment (each swings about its own joint, one after the other)

Run (Blender as a Python module, see docs/heroweapon/blender_pipeline.md):
    python3 -B tools/blender/saiyan_hair.py [--only ss,ss3,tail] [--trial DIR] [--quick] [--no-previews]
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
import numpy as np  # noqa: E402
from mathutils import Euler, Matrix, Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

V = Vector
PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "saiyan_hair", "previews")

# the form colours of the previews (tint, glow, glow strength): the same table as saiyan/SaiyanHair.java (a test compares them)
FORMS = {
    "kaioken": ("base", "1A1418", "E03020", 0.35),
    "false_super_saiyan": ("ss", "D8C070", "E0B040", 0.25),
    "super_saiyan": ("ss", "FFD020", "FFD020", 0.55),
    "super_saiyan_full_power": ("ss", "FFDC38", "FFE060", 0.65),
    "super_saiyan_2": ("ss2", "FFEA50", "FFF080", 0.75),
    "super_saiyan_3": ("ss3", "FFB818", "FFC820", 0.70),
    "super_saiyan_4": ("ss4", "2A1218", "C02828", 0.35),
    "super_full_power_super_saiyan_4": ("ss4", "2C1016", "E03838", 0.40),
    "super_saiyan_4_limit_breaker": ("ss4", "301018", "FF4468", 0.50),
    "super_saiyan_god": ("god", "E02828", "FF3030", 0.60),
    "super_saiyan_blue": ("god", "48B8FF", "40B0FF", 0.60),
    "perfected_super_saiyan_blue": ("god", "68D0FF", "80D8FF", 0.70),
    "super_saiyan_blue_kaioken": ("god", "58A8FF", "FF4060", 0.60),
    "ultra_instinct_sign": ("ui", "BCC8E0", "B0C8FF", 0.60),
    "perfected_ultra_instinct": ("ui", "DCE6F8", "E8F0FF", 0.70),
    "true_ultra_instinct": ("ui", "F4F8FF", "FFFFFF", 0.80),
}
TAIL_FUR = "C02020"


# ================================================================ materials
def fur_mat(name, base=222, glow=True, bscale=1.4):
    """A neutral light-grey fur / hair (the tint is multiplied in by the game). glow: the tips (far from the head) bake an emission, the dome none."""
    m = bl.mat(name, bl.rgb(base, base, base), rough=0.55, bump="brushed", bscale=bscale, bstr=0.5, bevel=0.1)
    if glow:
        nt = m.node_tree
        N, L = nt.nodes, nt.links
        bs = N["Principled BSDF"]
        co = N.new("ShaderNodeTexCoord").outputs["Object"]
        d = N.new("ShaderNodeVectorMath")
        d.operation = "DISTANCE"
        d.inputs[1].default_value = (0.0, 28.0, -1.0)
        L.new(co, d.inputs[0])
        mr = N.new("ShaderNodeMapRange")
        mr.inputs["From Min"].default_value = 9.5
        mr.inputs["From Max"].default_value = 16.0
        mr.inputs["To Min"].default_value = 0.0
        mr.inputs["To Max"].default_value = 1.0
        L.new(d.outputs["Value"], mr.inputs["Value"])
        bs.inputs["Emission Color"].default_value = bl.rgb(235, 235, 235)
        L.new(mr.outputs["Result"], bs.inputs["Emission Strength"])
        m["glow"] = True
    return m


# ================================================================ hair geometry
HL = [(0.0, 30.3), (0.45, 30.3), (0.9, 29.4), (1.57, 27.4), (2.4, 26.0), (3.1416, 25.2)]       # the lower edge of the dome by the angle away from the face (radians)
RP = [(24.0, 4.95), (31.5, 4.95), (32.6, 4.85), (33.4, 3.6), (33.9, 2.0)]                       # the dome's half width by height
YTOP = 33.9


def interp(tab, x):
    if x <= tab[0][0]:
        return tab[0][1]
    for (x0, y0), (x1, y1) in zip(tab, tab[1:]):
        if x <= x1:
            return y0 + (y1 - y0) * (x - x0) / (x1 - x0)
    return tab[-1][1]


def cap_pt(a, f):
    """The dome point at azimuth a (radians, 0 = the face, + toward +x) and level f (0 = the hairline, 1 = the crown)."""
    d = abs((a + math.pi) % (2 * math.pi) - math.pi)
    hl = interp(HL, d)
    y = hl + f * (YTOP - hl)
    u = (abs(math.sin(a)) ** 6 + abs(math.cos(a)) ** 6) ** (-1.0 / 6.0)               # a squarish section: the box of the head layer, not a circle
    r = interp(RP, y) * u
    return V((r * math.sin(a), y, r * math.cos(a)))


def dome(mat):
    levels = [0.0, 0.2, 0.4, 0.6, 0.78, 0.9, 1.0]
    n = 16
    rings = [[cap_pt(2 * math.pi * k / n, f) for k in range(n)] for f in levels]
    return bl.loft("dome", rings, mat, caps=(False, True), part="head")


def dirv(az, el):
    a, e = math.radians(az), math.radians(el)
    return V((math.sin(a) * math.cos(e), math.sin(e), math.cos(a) * math.cos(e)))


def blade(name, part, p0, d, length, w, th, mat, bend=(0.0, 0.0, 0.0), rings=4, tip=0.1, wdir=None):
    """A pointed flat lock: from p0 along d for `length`, curving by `bend` (added with t squared), wide `w` and thick `th` (half sizes) at the root, tapering to a point.
    The width lies along `wdir` (default: around the head), so the locks fan out like the anime's spikes."""
    p0, d, bend = V(p0), V(d).normalized(), V(bend)
    pts = [p0 + d * (length * t) + bend * (t * t) for t in [i / (rings - 1) for i in range(rings)]]
    if wdir is None:
        a = math.atan2(p0.x, p0.z)
        wdir = V((math.cos(a), 0.0, -math.sin(a)))
    out = []
    for i, p in enumerate(pts):
        t = i / (rings - 1)
        tan = (pts[min(i + 1, rings - 1)] - pts[max(i - 1, 0)]).normalized()
        wv = V(wdir) - tan * V(wdir).dot(tan)
        if wv.length < 1e-4:
            wv = V((0.0, 1.0, 0.0)) - tan * tan.y
        wv.normalize()
        nv = tan.cross(wv).normalized()
        k = max(1.0 - t, 0.0) ** 0.8
        ww, tt = max(w * k, tip), max(th * k, tip)
        out.append([p + wv * ww, p + nv * tt, p - wv * ww, p - nv * tt])
    return bl.loft(name, out, mat, caps=(True, True), part=part)


COUNT = [0]


def lock(part, mat, az, f, daz, el, length, w, th, droop=0.0, curl=0.0, rings=4, back=0.0, side=0.0, tag="l"):
    """One lock rooted on the dome at (az, f), leaving in direction (daz, el); droop/curl: its tip hangs or curls up by that many units; back: it sweeps backwards; side: toward +x."""
    base = cap_pt(math.radians(az), f)
    d = dirv(daz, el)
    p0 = base - d * 0.8
    COUNT[0] += 1
    return blade(f"{tag}{COUNT[0]}", part, p0, d, length, w, th, mat, bend=(side, curl - droop, -back), rings=rings)


def pair(fn, az, *a, **k):
    """Left and right: a lock at +az and its mirror at -az (a lock at 0 or 180 is made once)."""
    fn(az, 1, *a, **k)
    if az % 180 != 0:
        fn(az, -1, *a, **k)


# ---------------------------------------------------------------- the shapes
def hair_spiky(m, kind):
    """base / ss / ss2 / god / ui: the Dragon Ball crown of upward spikes with bangs, differing in how many, how long, how thick and how they curve."""
    rnd = random.Random({"base": 3, "ss": 5, "ss2": 7, "god": 11, "ui": 13}[kind])
    std = ((0.97, (0, 45, 90, 135, 180), (84, 82, 80, 76, 72)), (0.7, (22, 67, 112, 157), (76, 72, 66, 60)), (0.4, (12, 50, 95, 135, 170), (62, 54, 42, 30, 20)))
    dense = ((0.97, (0, 36, 72, 108, 144, 180), (86, 84, 82, 78, 74, 70)), (0.72, (18, 54, 90, 126, 162), (78, 72, 68, 60, 54)), (0.45, (12, 45, 80, 115, 150), (64, 56, 44, 32, 20)))
    ui = ((0.97, (0, 36, 72, 108, 144, 180), (86, 84, 82, 78, 74, 70)), (0.72, (18, 54, 90, 126, 162), (78, 72, 66, 60, 54)), (0.45, (10, 45, 80, 115, 150), (64, 54, 44, 32, 22)))
    P = dict(
        base=dict(L=0.8, w=1.0, th=1.0, jit=0.05, droop=1.7, curl=0.0, tilt=-6, rows=std, bangs=((0, 4.6, -28, 0.55), (20, 4.2, -20, 0.62))),
        ss=dict(L=1.0, w=1.0, th=1.0, jit=0.06, droop=0.0, curl=0.0, tilt=0, rows=std, bangs=((0, 4.6, -28, 0.55), (20, 4.2, -20, 0.62))),
        ss2=dict(L=1.12, w=0.78, th=0.85, jit=0.16, droop=0.0, curl=0.0, tilt=0, rows=dense, bangs=((14, 6.4, -38, 0.6), (36, 4.4, -22, 0.55))),
        god=dict(L=0.94, w=0.9, th=0.9, jit=0.04, droop=0.0, curl=1.8, tilt=-5, rows=std, bangs=((0, 3.8, -22, 0.5), (16, 5.4, -34, 0.55), (38, 4.0, -18, 0.55))),
        ui=dict(L=1.08, w=0.72, th=0.8, jit=0.09, droop=0.0, curl=2.6, tilt=0, rows=ui, bangs=((0, 5.6, -34, 0.45), (18, 5.2, -30, 0.45), (36, 4.4, -22, 0.45))),
    )[kind]

    def one(az, sgn, f, el, L, w, th, **k):
        a = az * sgn + rnd.uniform(-P["jit"], P["jit"]) * 40.0
        lock("head", m, a, f, a + rnd.uniform(-P["jit"], P["jit"]) * 30.0, el + rnd.uniform(-P["jit"], P["jit"]) * 60.0, L * (1.0 + rnd.uniform(-P["jit"], P["jit"]) * 2.0), w, th, **k)

    for f, azs, els in P["rows"]:
        for az, el in zip(azs, els):
            L = (10.5 if f > 0.9 else 9.5 if f > 0.6 else 7.5) * P["L"]
            el = min(el + P["tilt"], 88)
            pair(lambda a_, s_, az=az, el=el: one(a_, s_, f, el, L, 1.9 * P["w"], 0.95 * P["th"], droop=P["droop"], curl=P["curl"]), az)
    for az, L, el, wsc in P["bangs"]:
        pair(lambda a_, s_, az=az: lock("head", m, a_ * s_, 0.14, a_ * s_ * 0.6, el, L, 1.9 * wsc, 0.7, droop=-0.2), az)
    for az, el in ((75, -60), (100, -70)):
        pair(lambda a_, s_, el=el: lock("head", m, a_ * s_, 0.0, a_ * s_, el, 3.2, 1.2, 0.7), az)


def hair_ss3(m):
    """Super Saiyan 3: the crown of long, heavy spikes and a mane hanging to the knees (the long locks are the part `tail`)."""
    rnd = random.Random(21)
    for f, azs, els, L in ((0.97, (0, 45, 90, 135, 180), (80, 74, 66, 58, 52), 15.0), (0.72, (22, 67, 112, 157), (68, 58, 46, 36), 15.5)):
        for az, el in zip(azs, els):
            pair(lambda a_, s_, az=az, el=el, f=f, L=L: lock("head", m, a_ * s_, f, a_ * s_, el + rnd.uniform(-4, 4), L * (1 + rnd.uniform(-0.1, 0.1)), 2.6, 1.15, curl=1.5), az)
    for az in (110, 135, 160):                                                                       # the mane: long locks down the back and sides
        for k, (f, el, L) in enumerate(((0.55, -78, 25.0), (0.3, -84, 23.0))):
            pair(lambda a_, s_, az=az, f=f, el=el, L=L: lock("tail", m, a_ * s_ + (3 if k else -3), f, a_ * s_, el + rnd.uniform(-3, 3), L + rnd.uniform(-3, 3), 2.7, 1.1, back=1.5, rings=5), az)
    lock("tail", m, 180, 0.4, 180, -82, 26.0, 2.8, 1.1, back=1.8, rings=5)
    for az in (60, 90):                                                                              # the side locks past the face
        pair(lambda a_, s_: lock("head", m, a_ * s_, 0.05, a_ * s_, -70, 8.0, 2.0, 0.9, curl=1.0), az)
    for az, L, el in ((0, 5.0, -24), (22, 4.4, -18)):                                                # a short parted fringe: SS3 has no brow (the skin's own brows cannot be removed)
        pair(lambda a_, s_, L=L, el=el: lock("head", m, a_ * s_, 0.16, a_ * s_ * 0.5, el, L, 1.5, 0.7), az)


def hair_ss4(m):
    """Super Saiyan 4: the wild black mane (swept back, shaggy, hanging to the shoulder blades) and a ruff of fur round the neck."""
    rnd = random.Random(33)
    for f, azs, els, L in ((0.97, (0, 40, 80, 120, 160, 180), (70, 62, 52, 42, 34, 30), 13.0), (0.7, (20, 60, 100, 140), (56, 44, 32, 22), 13.5), (0.42, (20, 80, 140), (40, 20, 2), 11.5)):
        for az, el in zip(azs, els):
            pair(lambda a_, s_, az=az, el=el, f=f, L=L: lock("head", m, a_ * s_, f, a_ * s_, el + rnd.uniform(-5, 5), L * (1 + rnd.uniform(-0.15, 0.15)), 2.4, 1.1, back=3.5, curl=0.6), az)
    for az in (110, 135, 160, 180):
        pair(lambda a_, s_, az=az: lock("tail", m, a_ * s_, 0.45, a_ * s_, -70 + rnd.uniform(-6, 6), 15.0 + rnd.uniform(-2, 3), 2.5, 1.1, back=3.0, rings=5), az)
    for az, L, el, f in ((0, 5.0, -24, 0.14), (16, 6.2, -32, 0.16), (36, 4.2, -16, 0.14)):
        pair(lambda a_, s_, L=L, el=el, f=f: lock("head", m, a_ * s_, f, a_ * s_ * 0.5, el, L, 1.3, 0.7), az)
    for k in range(6):                                                                               # the fur ruff on the nape and shoulders (the front stays free: the face, the chin; part of the head so it turns with it)
        a = 100.0 + k * 160.0 / 5
        r = 5.4
        p0 = V((r * math.sin(math.radians(a)) * 0.9, 25.0, r * math.cos(math.radians(a)) * 0.85))
        d = V((math.sin(math.radians(a)) * 0.6, -0.55, math.cos(math.radians(a)) * 0.6)).normalized()
        blade(f"ruff{k}", "head", p0 - d * 0.6, d, 5.2 + rnd.uniform(-0.8, 1.0), 1.7, 0.9, m, bend=(0.0, -1.0, 0.0), rings=3)


# ================================================================ the tail
TAIL_PATH = [(0.0, 13.6, -1.4), (0.0, 12.4, -5.0), (0.3, 10.6, -9.5), (1.2, 10.4, -14.0), (2.0, 12.6, -18.0), (1.8, 16.0, -20.2), (0.8, 19.2, -20.4)]


def build_tail(mat):
    """A tapered, furred, curved tail in three parts that swing about their own joints: the root (follows the body), the middle and the tip. Real tube rings (7 sides) plus a coat of fur blades."""
    path = bl.smooth_path(TAIL_PATH, 5)
    n = len(path)
    cuts = [0, int(n * 0.36), int(n * 0.68), n - 1]
    names = ["tail", "wing_l", "wing_r"]
    side0 = V((1.0, 0.0, 0.0))
    frames = []
    for i, p in enumerate(path):
        tan = (path[min(i + 1, n - 1)] - path[max(i - 1, 0)]).normalized()
        sd = (side0 - tan * side0.dot(tan)).normalized()
        frames.append((tan, sd, tan.cross(sd).normalized()))

    def radius(i):
        t = i / (n - 1)
        return 0.7 + 1.5 * (1 - t) ** 1.1

    for si, name in enumerate(names):
        rings = []
        for i in range(cuts[si], cuts[si + 1] + 1):
            tan, sd, up = frames[i]
            r = radius(i)
            rings.append([path[i] + sd * (r * math.cos(2 * math.pi * k / 7)) + up * (r * math.sin(2 * math.pi * k / 7)) for k in range(7)])
        bl.loft("tail_" + name, rings, mat, caps=(True, True), part=name)
    rnd = random.Random(41)
    for i in range(1, n - 2, 3):                                                                    # the fur: blades laid along the tail, pointing out and toward the tip
        si = 0 if i < cuts[1] else 1 if i < cuts[2] else 2
        tan, sd, up = frames[i]
        t = i / (n - 1)
        for k in range(4):
            a = 2 * math.pi * (k + 0.5 * (i % 4 == 1)) / 4 + rnd.uniform(-0.25, 0.25)
            rad = sd * math.cos(a) + up * math.sin(a)
            d = (rad * 0.42 + tan * 0.9).normalized()
            blade(f"fur{i}_{k}", names[si], path[i] + rad * radius(i) * 0.55, d, (3.4 - 1.4 * t) * (1 + rnd.uniform(-0.15, 0.15)), 1.5 * (1 - 0.35 * t), 0.7, mat, bend=(0.0, -0.3, 0.0), rings=3, wdir=tan.cross(d))
    last = path[-1]
    blade("tip", "wing_r", last - frames[-1][0] * 0.8, frames[-1][0], 6.0, 1.5, 1.1, mat, rings=3, wdir=V((1.0, 0.0, 0.0)))
    return {"tail": (None, TAIL_PATH[0]), "wing_l": ("tail", tuple(path[cuts[1]])), "wing_r": ("wing_l", tuple(path[cuts[2]]))}


# ================================================================ the builds
def build(shape):
    m = fur_mat("hair_" + shape, glow=(shape != "tail"))
    if shape == "tail":
        return build_tail(fur_mat("tail_fur", base=225, glow=False, bscale=2.2))
    COUNT[0] = 0
    dome(m)
    if shape == "ss3":
        hair_ss3(m)
    elif shape == "ss4":
        hair_ss4(m)
    else:
        hair_spiky(m, shape)
    parts = {o["part"] for o in bpy.context.scene.collection.objects if o.type == "MESH"}
    rig = {"head": (None, (0.0, 26.0, -0.5))}
    if "tail" in parts:
        rig["tail"] = ("head", (0.0, 27.0, -4.5))
    return rig


SHAPES = {"base": 0.40, "ss": 0.40, "ss2": 0.36, "ss3": 0.36, "ss4": 0.36, "god": 0.40, "ui": 0.36, "tail": 0.45}
GLOW = ("FFFFFF", 0.1, 0.5, 1.0)


# ================================================================ previews: a blocky player under the hair, tinted like the game
def _hex(h):
    return bl.rgb(int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def _box(name, lo, hi, color, rough=0.8):
    verts = [(x, y, z) for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])]
    faces = [(0, 1, 3, 2), (4, 6, 7, 5), (0, 4, 5, 1), (2, 3, 7, 6), (0, 2, 6, 4), (1, 5, 7, 3)]
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    m.node_tree.nodes["Principled BSDF"].inputs["Base Color"].default_value = color
    m.node_tree.nodes["Principled BSDF"].inputs["Roughness"].default_value = rough
    me = bpy.data.meshes.new(name)
    me.from_pydata(verts, [], faces)
    me.update()
    me.materials.append(m)
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    bl.outward(ob)
    return ob


def _proxy():
    """A Minecraft-proportioned player (head, body, arms, legs, two eyes): only for the preview, never exported."""
    skin, shirt, pants = bl.rgb(226, 168, 132), bl.rgb(238, 120, 20), bl.rgb(40, 60, 150)
    obs = [_box("p_head", (-4, 24, -4), (4, 32, 4), skin), _box("p_body", (-4, 12, -2), (4, 24, 2), shirt), _box("p_arm_l", (4, 12, -2), (8, 24, 2), shirt), _box("p_arm_r", (-8, 12, -2), (-4, 24, 2), shirt),
           _box("p_leg_l", (0, 0, -2), (4, 12, 2), pants), _box("p_leg_r", (-4, 0, -2), (0, 12, 2), pants),
           _box("p_eye_l", (1, 27.4, 4.02), (3, 28.6, 4.08), bl.rgb(250, 250, 250)), _box("p_eye_r", (-3, 27.4, 4.02), (-1, 28.6, 4.08), bl.rgb(250, 250, 250)),
           _box("p_pupil_l", (1.6, 27.4, 4.09), (2.6, 28.6, 4.12), bl.rgb(20, 20, 40)), _box("p_pupil_r", (-2.6, 27.4, 4.09), (-1.6, 28.6, 4.12), bl.rgb(20, 20, 40)),
           _box("p_mouth", (-1.2, 25.6, 4.02), (1.2, 26.2, 4.06), bl.rgb(150, 70, 60))]
    return obs


def _hair_material(atlas, tint, glow, k):
    m = bpy.data.materials.new("pv_hair")
    m.use_nodes = True
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    t = N.new("ShaderNodeTexImage")
    t.image = be._np_image("pv_hskin", atlas.img)
    t.interpolation = "Closest"
    mix = N.new("ShaderNodeMix")
    mix.data_type = "RGBA"
    mix.blend_type = "MULTIPLY"
    mix.inputs[0].default_value = 1.0
    mix.inputs[7].default_value = _hex(tint)
    L.new(t.outputs["Color"], mix.inputs[6])
    L.new(mix.outputs[2], bs.inputs["Base Color"])
    bs.inputs["Roughness"].default_value = 0.8
    g = N.new("ShaderNodeTexImage")
    g.image = be._np_image("pv_hglow", atlas.glow)
    gm = N.new("ShaderNodeMix")
    gm.data_type = "RGBA"
    gm.blend_type = "MULTIPLY"
    gm.inputs[0].default_value = 1.0
    gm.inputs[7].default_value = _hex(glow)
    L.new(g.outputs["Color"], gm.inputs[6])
    L.new(gm.outputs[2], bs.inputs["Emission Color"])
    bs.inputs["Emission Strength"].default_value = 4.0 * k
    return m


def render_previews(low, names, rig_, atlas, previews, poses, res=(360, 480), samples=24):
    """previews = {png path: (yaw degrees, "tint/glow/strength" or "tail")}. The player proxy is added here, after the bake, so it is never in the mesh."""
    sc = bpy.context.scene
    for o in sc.collection.objects:
        o.hide_render = True
    proxy = _proxy()
    w = bpy.data.worlds.new("preview")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs["Color"].default_value = (0.2, 0.22, 0.27, 1)
    w.node_tree.nodes["Background"].inputs["Strength"].default_value = 1.1
    sc.world = w
    sun = bpy.data.objects.new("sun", bpy.data.lights.new("sun", "SUN"))
    sun.data.energy = 4.0
    sc.collection.objects.link(sun)
    fill = bpy.data.objects.new("fill", bpy.data.lights.new("fill", "SUN"))
    fill.data.energy = 1.6
    sc.collection.objects.link(fill)
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 55
    sc.collection.objects.link(cam)
    sc.camera = cam
    sc.render.resolution_x, sc.render.resolution_y = res
    sc.render.film_transparent = False
    sc.cycles.samples = samples
    sc.view_settings.view_transform = "Standard"
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGB"
    rot = Matrix.Rotation(math.radians(90), 4, "X")
    for o in proxy:
        o.hide_render = False
        o.matrix_world = rot
    cache = {}
    for path, (yaw, spec) in previews.items():
        if spec == "tail":
            mat = _hair_material(atlas, TAIL_FUR, "000000", 0.0)
        else:
            if spec not in cache:
                tint, glow, k = spec.split("/")
                cache[spec] = _hair_material(atlas, tint, glow, float(k))
            mat = cache[spec]
        me = low.data.copy()
        me.materials.clear()
        me.materials.append(mat)
        ob = bpy.data.objects.new("pv", me)
        sc.collection.objects.link(ob)
        ob.matrix_world = rot
        pts = [v.co for v in me.vertices] + [V((-4, 24, -4)), V((4, 32, 4))]
        lo, hi = V([min(p[i] for p in pts) for i in range(3)]), V([max(p[i] for p in pts) for i in range(3)])
        if spec == "tail":
            lo.y, hi.y = 0.0, 32.0
            lo.x, hi.x = -9.0, 9.0
        mid = (lo + hi) / 2
        size = max((hi.y - lo.y) * 1.02, (hi.x - lo.x) * 1.1, (hi.z - lo.z) * 0.9)
        a = math.radians(yaw)
        c = V((mid.x, -mid.z, mid.y))
        dist = size * 2.6
        cam.location = c + V((math.sin(a) * dist, -math.cos(a) * dist, size * 0.06))
        cam.rotation_euler = (c - cam.location).to_track_quat("-Z", "Y").to_euler()
        sun.rotation_euler = Euler((math.radians(50), 0, a + math.radians(32)), "XYZ")
        fill.rotation_euler = Euler((math.radians(60), 0, a - math.radians(110)), "XYZ")
        sc.render.filepath = os.path.abspath(path)
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        bpy.ops.render.render(write_still=True)
        bpy.data.objects.remove(ob)
        bpy.data.meshes.remove(me)
        print("preview", path)


def sheet(paths, out):
    """Pillow: the views side by side in one PNG (quantised to keep the repo small); the single renders are deleted."""
    from PIL import Image
    ims = [Image.open(p).convert("RGB") for p in paths]
    w, h = ims[0].size
    sh = Image.new("RGB", (w * len(ims), h))
    for i, im in enumerate(ims):
        sh.paste(im, (i * w, 0))
    os.makedirs(os.path.dirname(os.path.abspath(out)), exist_ok=True)
    sh.quantize(colors=200, method=Image.Quantize.MEDIANCUT).save(out, optimize=True)
    for p in paths:
        os.remove(p)
    print("sheet", out, os.path.getsize(out) // 1024, "KB")


def grid(paths, out, cols=4):
    from PIL import Image
    ims = [Image.open(p).convert("RGB") for p in paths]
    w, h = ims[0].size
    rows = (len(ims) + cols - 1) // cols
    sh = Image.new("RGB", (w * cols, h * rows))
    for i, im in enumerate(ims):
        sh.paste(im, ((i % cols) * w, (i // cols) * h))
    sh.quantize(colors=220, method=Image.Quantize.MEDIANCUT).save(out, optimize=True)
    for p in paths:
        os.remove(p)
    print("grid", out, os.path.getsize(out) // 1024, "KB")


def shrink_skin(path, colors=256):
    from PIL import Image
    im = Image.open(path).convert("RGBA")
    im.quantize(colors=colors, method=Image.Quantize.FASTOCTREE).save(path, optimize=True)
    return os.path.getsize(path)


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    only = argv[argv.index("--only") + 1].split(",") if "--only" in argv else list(SHAPES)
    pv = trial or PREVIEW_DIR
    be.render_previews = render_previews
    forms_done = []
    for shape in only:
        bl.reset(samples=8 if quick else 20)
        rig = build(shape)
        total = sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")
        print(shape, "faces before export:", total)
        eid = "saiyan_tail" if shape == "tail" else "saiyan_hair_" + shape
        views = {}
        tmp = os.path.join(pv, "_tmp")
        if "--no-previews" not in argv:
            if shape == "tail":
                for y, name in ((0, "front"), (90, "side"), (180, "back"), (145, "three_quarter")):
                    views[os.path.join(tmp, f"tail_{name}.png")] = (y, "tail")
            else:
                ex = next((v for v in FORMS.values() if v[0] == shape), None)
                spec = f"{ex[1]}/{ex[2]}/{ex[3]}"
                for y, name in ((0, "front"), (90, "side"), (180, "back")):
                    views[os.path.join(tmp, f"{shape}_{name}.png")] = (y, spec)
                for fid, (shp, tint, glow, k) in FORMS.items():
                    if shp == shape:
                        views[os.path.join(tmp, f"form_{fid}.png")] = (20, f"{tint}/{glow}/{k}")
        be.finish_entity(eid, rig, glow=GLOW, skin=512, bake=512 if quick else 1024, lod_ratio=SHAPES[shape], out_mesh=trial, out_skin=trial, previews=views, poses={}, cage=0.2)
        for s in (eid + ".png", eid + "_glow.png"):
            print("skin", s, shrink_skin(os.path.join(trial or be.ENTITY_SKIN_DIR, s)) // 1024, "KB")
        if views:
            keys = list(views)
            main_views = [k for k in keys if not os.path.basename(k).startswith("form_")]
            sheet(main_views, os.path.join(pv, ("saiyan_tail.png" if shape == "tail" else f"hair_{shape}.png")))
            for k in keys:
                if os.path.basename(k).startswith("form_"):
                    forms_done.append(k)
    if forms_done and len(only) == len(SHAPES):
        order = {f: i for i, f in enumerate(FORMS)}
        forms_done.sort(key=lambda p: order[os.path.basename(p)[5:-4]])
        grid(forms_done, os.path.join(pv, "all_forms.png"))
    tmp = os.path.join(pv, "_tmp")
    if os.path.isdir(tmp) and not os.listdir(tmp):
        os.rmdir(tmp)


if __name__ == "__main__":
    main(sys.argv[1:])

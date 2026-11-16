"""Devil bodies, group 3 (the Devil Contract system), MADE IN BLENDER: five Blue Exorcist Demon-King kin and two Tensura daemons.

    kin_of_iblis (Fire), kin_of_egyn (Water), kin_of_amaimon (Earth), kin_of_astaroth (Rot), kin_of_lucifer (Light), daemon_noir, daemon_blanc

Every body is an upright humanoid at player scale (1 unit = 1/16 block, 32 units = 2 blocks, y up, face +z, its left = +x), split into the eleven roles the game turns
(torso head arm_l arm_r forearm_l forearm_r leg_l leg_r, plus wing_l wing_r / tail where the devil has them), exported through bl_entity.py to
entity_meshes/devil_<id>.txt (+ _lod), textures/entity/boss_mesh/devil_<id>(.png, _glow.png) and docs/devil_forms/previews/<id>.png.

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    python3 -B tools/blender/devil_group3.py [--only kin_of_iblis,daemon_noir] [--trial DIR] [--quick]
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "devil_forms", "previews")
V = Vector


# ================================================================ materials
def C(c):
    """Hex string 'RRGGBB' or an (r, g, b) 0..255 tuple to a Blender colour."""
    if isinstance(c, str):
        c = (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))
    return bl.rgb(*c)


def M(name, col, rough=0.6, metal=0.0, bump=None, bscale=1.0, bstr=0.3, bevel=0.12, groove=None, gamount=0.6):
    return bl.mat(name, C(col), metallic=metal, rough=rough, bump=bump, bscale=bscale, bstr=bstr, bevel=bevel, groove=C(groove) if groove else None, gamount=gamount)


def E(name, col, strength=8.0):
    """An emissive material: it is baked into the glow skin."""
    return bl.mat(name, C(col), rough=0.3, emit=C(col), strength=strength * 0.17, bevel=0.0)      # the skin keeps the colour: no clipping to white


def add_veins(m, col, scale=0.3, width=0.05, region=0.55, rscale=0.2, strength=5.0):
    """Glowing cracks / lines: Voronoi edges masked by a slow noise feed the emission (as the Goliath's molten veins do)."""
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    co = N.new("ShaderNodeTexCoord").outputs["Object"]
    vor = N.new("ShaderNodeTexVoronoi")
    vor.feature = "DISTANCE_TO_EDGE"
    vor.inputs["Scale"].default_value = scale
    L.new(co, vor.inputs["Vector"])
    edge = bl._math(nt, "LESS_THAN", vor.outputs["Distance"], width)
    reg = N.new("ShaderNodeTexNoise")
    reg.inputs["Scale"].default_value = rscale
    L.new(co, reg.inputs["Vector"])
    mask = bl._math(nt, "MULTIPLY", edge, bl._math(nt, "GREATER_THAN", reg.outputs[0], region))
    bs.inputs["Emission Color"].default_value = C(col)
    L.new(bl._math(nt, "MULTIPLY", mask, strength * 0.17), bs.inputs["Emission Strength"])
    m["glow"] = True
    return m


# ================================================================ shapes
def sec(n, w, d, sq=0.0):
    pts = []
    for k in range(n):
        a = 2 * math.pi * k / n
        c, s = math.cos(a), math.sin(a)
        r = 1.0 + sq * (abs(c * s) * 2) ** 2
        pts.append((w * c * r, d * s * r))
    return pts


def loftrows(name, part, rows, n, mat, sq=0.0, caps=(True, True)):
    """rows = [(y, half width, half depth, x offset, z offset)]: rings up a body piece."""
    rings = []
    for r in rows:
        y, w, d = r[0], r[1], r[2]
        ox = r[3] if len(r) > 3 else 0.0
        oz = r[4] if len(r) > 4 else 0.0
        rings.append([V((ox + u, y, oz + v)) for u, v in sec(n, w, d, sq)])
    return bl.loft(name, rings, mat, caps, part=part)


def limb(name, part, path, radii, n, mat, flat=1.0, sq=0.0, caps=(True, True)):
    """A smooth limb along a path; a radius per point (a number or (half width, half depth))."""
    P = [V(p) for p in path]
    rings = []
    for i, p in enumerate(P):
        tan = (P[min(i + 1, len(P) - 1)] - P[max(i - 1, 0)]).normalized()
        side = V((0, 0, 1)).cross(tan)
        if side.length < 1e-6:
            side = V((1, 0, 0))
        side.normalize()
        fwd = tan.cross(side).normalized()
        r = radii[i]
        rx, rz = (r if isinstance(r, tuple) else (r, r * flat))
        rings.append([p + side * u + fwd * v for u, v in sec(n, rx, rz, sq)])
    return bl.loft(name, rings, mat, caps, part=part)


def tube(name, part, pts, r0, r1, mat, sides=5, sub=2):
    return bl.tube(name, [tuple(p) for p in pts], None, mat, sides=sides, sub=sub, part=part, taper=(r0, r1))


def feather(name, part, base, rz, length, width, mat, tilt=-12.0, thick=0.3, tip=0.0, ry=0.0):
    """A pointed blade / feather lying in the xy plane, base at `base`, pointing up, fanned by rz degrees about z (negative = toward +x), leant back by tilt."""
    out = [(0, 0), (width * 0.5, length * 0.2), (width * 0.55, length * 0.65), (tip, length), (-width * 0.55, length * 0.65), (-width * 0.5, length * 0.2)]
    ob = bl.plate(name, out, -thick, thick, mat, part=part)
    bl.move(ob, base, (tilt, ry, rz))
    return ob


def slab(name, part, outline, t, mat, loc=(0, 0, 0), rot=(0, 0, 0)):
    ob = bl.plate(name, outline, -t, t, mat, part=part)
    bl.move(ob, loc, rot)
    return ob


def torus(name, part, center, R, r, mat, rot=(0, 0, 0), m=16, s=5):
    verts, faces = [], []
    for i in range(m):
        a = 2 * math.pi * i / m
        for j in range(s):
            b = 2 * math.pi * j / s
            rr = R + r * math.cos(b)
            verts.append(V((rr * math.cos(a), rr * math.sin(a), r * math.sin(b))))
    for i in range(m):
        for j in range(s):
            a, b = i * s + j, ((i + 1) % m) * s + j
            faces.append((a, b, ((i + 1) % m) * s + (j + 1) % s, i * s + (j + 1) % s))
    ob = bl.obj(name, verts, faces, mat, part=part)
    bl.move(ob, center, rot)
    return ob


def mirror_part(src, dst):
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.get("part") == src]:
        c = bl.mirror(o, "x", o.name + "_m")
        c["part"] = dst


def interp(rows, y):
    """(half width, half depth, x offset, z offset) of a row list at height y."""
    rows = [tuple(r) + (0.0,) * (5 - len(r)) for r in rows]
    if y <= rows[0][0]:
        return rows[0][1:]
    for a, b in zip(rows, rows[1:]):
        if y <= b[0]:
            t = (y - a[0]) / max(b[0] - a[0], 1e-6)
            return tuple(a[i] + (b[i] - a[i]) * t for i in range(1, 5))
    return rows[-1][1:]


# ================================================================ the shared humanoid
DEF = dict(leg=13.5, torso=10.0, hipw=3.0, hipd=2.4, waistw=2.7, waistd=2.1, chestw=4.0, chestd=2.8, shw=4.4, shd=2.5, neck=1.5, headw=3.4, headd=3.5, headh=6.6,
           stoop=0.0, upper=5.6, lower=5.6, armr=(1.35, 1.2, 1.05), forer=(1.15, 1.3, 1.0), legr=(2.2, 1.95, 1.55, 1.35, 1.25), digi=0.0, sq=0.12, n=10, hand=1.0,
           palm=1.4, flen=2.6, toes=True, skinsub=True)


def humanoid(P, skin, claw=None, handskin=None, headskin=None):
    """Builds torso, head, both arms and legs from proportions; returns the joints and a few surface functions."""
    p = dict(DEF)
    p.update(P)
    leg, tor = p["leg"], p["torso"]
    shy = leg + tor
    st = p["stoop"]
    zo = lambda y: st * max(0.0, (y - leg) / tor) ** 1.4          # noqa: E731
    rows = [(leg - 2.0, p["hipw"], p["hipd"], 0, zo(leg)), (leg + 0.4, p["hipw"] * 1.02, p["hipd"], 0, zo(leg)),
            (leg + tor * 0.38, p["waistw"], p["waistd"], 0, zo(leg + tor * 0.38)), (leg + tor * 0.72, p["chestw"], p["chestd"], 0, zo(leg + tor * 0.72)),
            (shy - 0.6, p["shw"], p["shd"], 0, zo(shy)), (shy + 0.8, p["neck"] * 1.7, p["neck"] * 1.5, 0, zo(shy) + 0.3), (shy + 2.4, p["neck"], p["neck"], 0, zo(shy) + 0.6)]
    t = loftrows("torso", "torso", rows, p["n"], skin, sq=p["sq"])
    if p["skinsub"]:
        bl.subsurf(t, 1)
    hy, hz = shy + 1.8, zo(shy) + 1.0 + st * 0.35
    hw, hd, hh = p["headw"], p["headd"], p["headh"]
    hrows = [(hy, hw * 0.5, hd * 0.5, 0, hz + hd * 0.15), (hy + hh * 0.17, hw * 0.82, hd * 0.88, 0, hz), (hy + hh * 0.45, hw, hd, 0, hz), (hy + hh * 0.72, hw * 0.97, hd * 0.96, 0, hz),
             (hy + hh * 0.92, hw * 0.78, hd * 0.78, 0, hz), (hy + hh, hw * 0.35, hd * 0.35, 0, hz)]
    head = loftrows("head", "head", hrows, 8, headskin or skin, sq=0.1)
    bl.subsurf(head, 1)
    sx, sy, sz = p["shw"] + 0.9, shy - 1.5, zo(shy)
    S = V((sx, sy, sz))
    Ee = V((sx + 0.6, sy - p["upper"], sz + 0.5))
    W = V((sx + 0.3, Ee.y - p["lower"], Ee.z + 1.0))
    a0, a1, a2 = p["armr"]
    f0, f1, f2 = p["forer"]
    ua = limb("arm_l", "arm_l", [S + V((-0.3, 0.9, 0)), (S + Ee) / 2, Ee + V((0, 0.2, 0))], [a0, a1, a2], 8, skin)
    be.side_copy(ua, "arm_r")
    fa = limb("forearm_l", "forearm_l", [Ee + V((0, 0.9, -0.1)), (Ee + W) / 2, W], [f0, f1, f2], 8, skin)
    be.side_copy(fa, "forearm_r")
    claw = claw or skin
    hs = handskin or skin
    k = p["hand"]
    if k > 0:
        pw = p["palm"]
        pal = loftrows("hand_l", "forearm_l", [(W.y + 0.3, f2 * 0.95, f2 * 0.8, W.x, W.z), (W.y - 1.3 * k, pw, pw * 0.6, W.x, W.z + 0.2), (W.y - 2.6 * k, pw * 0.85, pw * 0.5, W.x, W.z + 0.4)], 8, hs)
        be.side_copy(pal, "forearm_r")
        for i in range(4):
            fx = W.x + (i - 1.5) * pw * 0.55
            ln = p["flen"] * (1.0 - 0.12 * abs(i - 1.5))
            pts = [(fx, W.y - 2.4 * k, W.z + 0.3), (fx, W.y - 2.4 * k - ln * 0.55, W.z + 0.8), (fx, W.y - 2.4 * k - ln, W.z + 1.0 + ln * 0.3)]
            f = tube(f"finger_l{i}", "forearm_l", pts, 0.5, 0.28, hs, sides=4, sub=1)
            be.side_copy(f, "forearm_r")
            c = be.shard(f"claw_l{i}", "forearm_l", pts[2], (fx, pts[2][1] - 1.3, pts[2][2] + 0.9), 0.3, claw, n=4)
            be.side_copy(c, "forearm_r")
        th = tube("thumb_l", "forearm_l", [(W.x - pw * 0.7, W.y - 0.8, W.z + 0.5), (W.x - pw * 1.0, W.y - 2.0, W.z + 1.2), (W.x - pw * 0.9, W.y - 3.0, W.z + 2.0)], 0.5, 0.28, hs, sides=4, sub=1)
        be.side_copy(th, "forearm_r")
    # ---- legs
    hx = p["hipw"] * 0.55
    d = p["digi"]
    path = [(hx, leg + 0.6, 0), (hx + 0.1, leg * 0.74, 0.4 + d), (hx + 0.2, leg * 0.52, 0.6 + 2 * d), (hx + 0.1, leg * 0.26, -0.2 - 1.2 * d), (hx, 1.5, 0.6 * d)]
    lg = limb("leg_l", "leg_l", path, list(p["legr"]), 8, skin)
    be.side_copy(lg, "leg_r")
    ft = loftrows("foot_l", "leg_l", [(0, 1.8, 3.4, hx, 1.6 + d), (1.2, 1.9, 3.2, hx, 1.4 + d), (2.5, 1.5, 1.9, hx, 0.4 + d * 0.6)], 8, claw if not p["toes"] else skin)
    be.side_copy(ft, "leg_r")
    J = dict(p=p, shy=shy, leg=leg, hy=hy, hz=hz, hw=hw, hd=hd, hh=hh, S=S, E=Ee, W=W, rows=rows, hrows=hrows, zo=zo, hx=hx)
    J["tz"] = lambda y: (lambda r: r[3] + r[1] * 0.93)(interp(rows, y))              # the front of the torso at height y
    J["tw"] = lambda y: interp(rows, y)[0]
    J["fz"] = lambda y: (lambda r: r[3] + r[1] * 0.9)(interp(hrows, y))              # the front of the head
    J["fw"] = lambda y: interp(hrows, y)[0]
    return J


def rig(J, wings=None, tail=None):
    leg, shy = J["leg"], J["shy"]
    S, E = J["S"], J["E"]
    r = {"torso": (None, (0.0, leg, 0.0)), "head": ("torso", (0.0, shy + 1.0, J["hz"] * 0.4)),
         "arm_l": ("torso", (S.x, S.y, S.z)), "arm_r": ("torso", (-S.x, S.y, S.z)),
         "forearm_l": ("arm_l", (E.x, E.y, E.z)), "forearm_r": ("arm_r", (-E.x, E.y, E.z)),
         "leg_l": (None, (J["hx"], leg, 0.0)), "leg_r": (None, (-J["hx"], leg, 0.0))}
    if wings:
        r["wing_l"] = ("torso", wings)
        r["wing_r"] = ("torso", (-wings[0], wings[1], wings[2]))
    if tail:
        r["tail"] = ("torso", tail)
    return r


def eyes(J, mat, y, x=1.55, rx=1.0, ry=0.4, r=0.8, part="head", tilt=0):
    z = J["fz"](y) + 0.1
    for sx in (1, -1):
        g = bl.gem(f"eye_{sx}", r, mat, center=(0, 0, 0), depth=0.5, crown=0.3, sides=6, axis="z", rx=rx, ry=ry, part=part)
        bl.move(g, (x * sx, y, z), (0, 0, -tilt * sx))                              # turned about its own centre, then placed


def brow(J, y, mat, h=0.7):
    z = J["fz"](y)
    for sx in (1, -1):
        slab(f"brow_{sx}", "head", [(0.6, 0), (3.3, 0.4 + h), (3.4, 1.2 + h), (0.5, 0.9)], 0.45, mat, (0, y, z - 0.1))
    return None


# ================================================================ 1. kin of Iblis, King of Fire
def build_iblis():
    be.seed(101)
    rock = M("ib_rock", (38, 27, 27), rough=0.75, bump="hammer", bscale=0.5, bstr=0.5, groove=(14, 10, 10))
    add_veins(rock, "FF6A2A", scale=0.3, width=0.05, region=0.6, rscale=0.18, strength=7.0)
    obs = M("ib_obsidian", (24, 18, 24), rough=0.25, metal=0.5, bump="hammer", bscale=0.6, bstr=0.2, groove=(8, 6, 10))
    magma, flame, eye = E("ib_magma", "FF6A2A", 7.0), E("ib_flame", "FFB040", 9.0), E("ib_eye", "FFE878", 14.0)
    J = humanoid(dict(leg=13, torso=10.5, hipw=3.6, hipd=2.9, waistw=3.4, waistd=2.6, chestw=5.3, chestd=3.5, shw=5.8, shd=3.1, neck=2.0, headw=3.6, headd=3.8, headh=6.4, stoop=1.1,
                      upper=6.0, lower=5.8, armr=(2.3, 2.1, 1.9), forer=(1.9, 2.1, 1.8), palm=1.9, flen=3.0, legr=(2.7, 2.3, 1.9, 1.7, 1.5)), rock, claw=obs)
    shy = J["shy"]
    # ember heart and the glowing sternum
    bl.gem("heart", 1.8, magma, center=(0, J["leg"] + 7.6, J["tz"](J["leg"] + 7.6) - 0.2), depth=0.5, crown=0.4, sides=6, axis="z", rx=2.0, ry=2.6, part="torso")
    # obsidian belt and loin plates
    belt = loftrows("belt", "torso", [(J["leg"] - 0.4, 3.9, 3.2), (J["leg"] + 1.6, 3.8, 3.1)], 10, obs, caps=(False, False))
    bl.solidify(belt, 0.5)
    for k, x in enumerate((-2.4, 0, 2.4)):
        slab(f"loin_{k}", "torso", [(-1.2, 0), (1.2, 0), (0.9, -5.0), (0, -6.4), (-0.9, -5.0)], 0.4, obs, (x, J["leg"] + 0.4, 3.2 - abs(x) * 0.2))
    # pauldrons (they ride on the arms): a heavy obsidian dome with three flame spikes
    pd = be.boulder("pauldron_l", "arm_l", (J["S"].x + 0.6, shy - 0.4, 0.0), 4.6, obs, squash=(1.2, 0.75, 1.0), seg=(8, 5), jag=0.06)
    be.side_copy(pd, "arm_r")
    br = loftrows("bracer_l", "forearm_l", [(J["E"].y - 0.5, 2.5, 2.4, J["E"].x, J["E"].z), (J["E"].y - 3.2, 2.6, 2.5, J["E"].x + 0.1, J["E"].z + 0.3),
                                              (J["E"].y - 5.4, 2.2, 2.1, J["W"].x, J["W"].z)], 8, obs)
    be.side_copy(br, "forearm_r")
    # head: brow, jaw tusks, two sweeping horns, a mane of flame
    brow(J, J["hy"] + J["hh"] * 0.55, obs, 0.5)
    eyes(J, eye, J["hy"] + J["hh"] * 0.46, x=1.6, rx=1.1, ry=0.45, tilt=14)
    for sx in (1, -1):
        tube(f"horn_{sx}", "head", [(2.4 * sx, J["hy"] + 5.2, J["hz"] + 0.5), (4.6 * sx, J["hy"] + 7.0, J["hz"] - 0.5), (6.0 * sx, J["hy"] + 10.0, J["hz"] - 3.2),
                                    (5.2 * sx, J["hy"] + 11.0, J["hz"] - 6.4)], 1.4, 0.15, obs, sides=6, sub=3)
    for k in range(7):                                                                   # the mane: flame tongues sweeping back and up from the crown
        a = (k - 3) * 0.55
        tube(f"mane_{k}", "head", [(1.5 * a, J["hy"] + 6.2, J["hz"] - 1.0), (2.4 * a, J["hy"] + 8.0, J["hz"] - 3.0), (3.1 * a, J["hy"] + 10.5 - abs(a), J["hz"] - 5.5),
                                   (3.4 * a, J["hy"] + 11.0 - 2 * abs(a), J["hz"] - 7.5)], 1.2, 0.1, flame if k % 2 else magma, sides=4, sub=2)
    # tail: a rope of lava rock ending in a flame tuft
    tp = [(0, J["leg"] - 0.5, -2.6), (0, J["leg"] - 3.0, -7.0), (0, J["leg"] - 4.5, -12.0), (0, J["leg"] - 2.0, -16.5), (0, J["leg"] + 2.0, -18.5)]
    tube("tail", "tail", tp, 1.7, 0.8, rock, sides=6, sub=3)
    for k in range(4):
        tube(f"tuft_{k}", "tail", [tp[-1], (0.8 * (k - 1.5), tp[-1][1] + 2.5, tp[-1][2] - 1.0), (1.4 * (k - 1.5), tp[-1][1] + 5.5 + (k % 2), tp[-1][2] - 2.2)], 1.0, 0.1, flame, sides=4, sub=1)
    return rig(J, tail=(0, J["leg"], -2.0))


# ================================================================ 2. kin of Egyn, King of Water
def build_egyn():
    be.seed(202)
    scale = M("eg_scale", (34, 96, 190), rough=0.35, bump="scales", bscale=0.9, bstr=0.35, groove=(12, 40, 100), gamount=0.55)
    add_veins(scale, "7AE8FF", scale=0.3, width=0.035, region=0.62, rscale=0.2, strength=5.0)
    belly = M("eg_belly", (190, 230, 255), rough=0.3, bump="scales", bscale=0.9, bstr=0.25, groove=(120, 170, 220))
    pearl = M("eg_pearl", (236, 246, 255), rough=0.2, metal=0.3, bump="rings", bscale=1.2, bstr=0.2, groove=(150, 190, 230))
    fin = M("eg_fin", (40, 120, 230), rough=0.4, bump="brushed", bscale=2.0, bstr=0.2, groove=(14, 50, 130))
    dark = M("eg_deep", (10, 28, 70), rough=0.4)
    glow, eye = E("eg_glow", "6AD8FF", 8.0), E("eg_eye", "C8F8FF", 14.0)
    J = humanoid(dict(leg=14.5, torso=10.0, hipw=2.9, hipd=2.2, waistw=2.5, waistd=1.9, chestw=3.9, chestd=2.6, shw=4.3, shd=2.3, neck=1.3, headw=3.0, headd=3.4, headh=6.8,
                      upper=6.2, lower=6.6, armr=(1.25, 1.1, 0.95), forer=(1.05, 1.2, 0.9), palm=1.2, flen=3.6, legr=(2.0, 1.75, 1.4, 1.2, 1.1), digi=0.5), scale, claw=pearl)
    shy, leg = J["shy"], J["leg"]
    # pearl armour: a shell-like breastplate, gill slits, a kelp sash
    for sx in (1, -1):
        for k in range(3):
            slab(f"gill_{sx}_{k}", "torso", [(0, 0), (1.5, 0.2), (1.5, 0.5), (0, 0.3)], 0.25, glow, (1.6 * sx + 0.8 * sx, shy + 0.4 - k * 0.9, 1.3), (0, 0, 0))
    for k, x in enumerate((-2.6, -0.9, 0.9, 2.6)):                                         # the sash: long wavy ribbons
        slab(f"ribbon_{k}", "torso", [(-0.9, 0), (0.9, 0), (1.1, -4.0), (0.4, -8.0), (-0.3, -11.0), (-1.0, -6.0)], 0.2, fin, (x, leg + 0.2, 2.1 + (k % 2) * 0.4), (6 * (k - 1.5), 0, 0))
    # head: a swept dorsal crest, finned ears, coral spikes
    eyes(J, eye, J["hy"] + J["hh"] * 0.46, x=1.4, rx=1.1, ry=0.5, tilt=-12)
    slab("crest", "head", [(0, 0), (0, 5.8), (-1.2, 5.0), (-3.0, 6.8), (-4.4, 4.6), (-6.5, 5.6), (-7.5, 2.0), (-6.0, 0)], 0.35, fin, (0, J["hy"] + J["hh"] - 1.0, J["hz"] + 0.5), (0, -90, 0))
    for sx in (1, -1):
        slab(f"ear_{sx}", "head", [(sx * x, y) for x, y in ((0, 0), (4.5, 1.6), (5.8, -1.0), (4.6, -3.2), (0, -1.6))], 0.25, fin, (3.2 * sx, J["hy"] + J["hh"] * 0.5, J["hz"] - 0.5))
        tube(f"spike_{sx}", "head", [(1.8 * sx, J["hy"] + J["hh"] * 0.95, J["hz"] + 1.6), (2.6 * sx, J["hy"] + J["hh"] + 2.4, J["hz"] + 1.8), (2.8 * sx, J["hy"] + J["hh"] + 4.4, J["hz"] + 1.0)], 0.7, 0.1, pearl, sides=5, sub=2)
    # webbed fin-blades on the forearms
    for sx_name in ("l",):
        for k in range(3):
            fl = feather(f"forefin_l{k}", "forearm_l", (J["E"].x + 1.5, J["E"].y - 1.5 - k * 1.8, J["E"].z - 0.3), -70 + k * 6, 4.6 - k * 0.6, 1.4, fin, tilt=0)
            be.side_copy(fl, "forearm_r")
    # tail: long, with a caudal fin fan
    tp = [(0, leg - 0.5, -2.4), (0, leg - 3.5, -7.0), (0, leg - 7.5, -12.0), (0, leg - 10.0, -17.0), (0, leg - 10.5, -22.0)]
    tube("tail", "tail", tp, 1.5, 0.5, scale, sides=6, sub=3)
    for k in range(5):
        feather(f"tailfin_{k}", "tail", tp[-1], 0, 9.0 - abs(k - 2) * 1.3, 2.2, fin, tilt=-90, ry=(k - 2) * 16, thick=0.3)
    return rig(J, tail=(0, leg, -2.0))


# ================================================================ 3. kin of Amaimon, King of Earth
def build_amaimon():
    """Amaimon, the Earth King (Blue Exorcist): a slim, pale young man with a spiky bowl of dark green-black hair, pointed ears, wide green eyes, a black coat,
    a lollipop in his mouth; the kin's earth element is carried by brown stone gauntlets and greaves (the palette colour), nothing else is invented."""
    be.seed(303)
    skin = M("am_skin", (236, 226, 214), rough=0.55, bump="leather", bscale=0.4, bstr=0.08, bevel=0.1)
    cloth = M("am_cloth", (22, 22, 28), rough=0.7, bump="wrap", bscale=0.6, bstr=0.25, groove=(8, 8, 12))
    trim = M("am_trim", (150, 112, 62), rough=0.8, bump="hammer", bscale=0.5, bstr=0.4, groove=(60, 44, 24), gamount=0.6)
    add_veins(trim, "FFB838", scale=0.3, width=0.035, region=0.66, rscale=0.2, strength=4.0)
    hair = M("am_hair", (24, 38, 28), rough=0.5, bump="brushed", bscale=2.0, bstr=0.2, groove=(8, 14, 10))
    candy = M("am_candy", (232, 90, 140), rough=0.3, bump="rings", bscale=2.0, bstr=0.2, groove=(255, 230, 240))
    eye = E("am_eye", "B8F050", 6.0)
    J = humanoid(dict(leg=14.0, torso=9.6, hipw=2.9, hipd=2.2, waistw=2.5, waistd=1.9, chestw=3.7, chestd=2.5, shw=4.2, shd=2.2, neck=1.3, headw=3.2, headd=3.4, headh=6.4,
                      upper=5.6, lower=5.8, armr=(1.35, 1.2, 1.05), forer=(1.15, 1.25, 1.0), palm=1.3, flen=2.6, legr=(2.2, 1.95, 1.55, 1.35, 1.25)),
                 cloth, claw=trim, handskin=skin, headskin=skin)
    shy, leg = J["shy"], J["leg"]
    # a short black coat with a high collar, brown stone greaves and gauntlets (the earth kin's mark)
    sk = loftrows("skirt", "torso", [(leg + 1.0, 3.3, 2.7), (leg - 3.5, 3.9, 3.1)], 10, cloth, caps=(False, False))
    bl.solidify(sk, 0.35)
    for sx in (1, -1):
        slab(f"collar_{sx}", "torso", [(0, 0), (2.4, 0.5), (2.6, 3.4), (0.4, 2.4)], 0.25, cloth, (0.5 * sx, shy + 0.2, 1.2))
    for k in range(3):
        bl.gem(f"button_{k}", 0.4, trim, center=(0.0, leg + 2.5 + k * 2.6, J["tz"](leg + 2.5 + k * 2.6) + 0.1), depth=0.5, crown=0.5, sides=6, axis="z", part="torso")
    for sx in (1, -1):
        part = "leg_l" if sx > 0 else "leg_r"
        gv = loftrows(f"greave_{sx}", part, [(1.0, 2.0, 2.2, sx * J["hx"], 0.4), (5.5, 1.8, 2.0, sx * J["hx"], 0.1), (8.0, 2.1, 2.3, sx * J["hx"], 0.2)], 8, trim)
    gl = loftrows("gauntlet_l", "forearm_l", [(J["E"].y - 0.8, 1.9, 1.8, J["E"].x, J["E"].z), (J["E"].y - 3.0, 2.0, 1.9, J["E"].x + 0.1, J["E"].z + 0.3), (J["W"].y - 0.2, 1.8, 1.7, J["W"].x, J["W"].z)], 8, trim)
    be.side_copy(gl, "forearm_r")
    # head: pointed ears, wide green eyes, a spiky bowl of dark hair with a long fringe, a lollipop at the mouth
    eyes(J, eye, J["hy"] + J["hh"] * 0.47, x=1.45, rx=0.75, ry=0.6, r=0.8)
    for sx in (1, -1):
        be.shard(f"ear_{sx}", "head", (J["hw"] * 0.92 * sx, J["hy"] + 3.0, J["hz"]), (J["hw"] * sx + 3.6 * sx, J["hy"] + 4.6, J["hz"] - 0.6), 0.8, skin, n=4)
    cap = loftrows("hair_cap", "head", [(J["hy"] + J["hh"] * 0.62, 3.45, 3.6, 0, J["hz"] - 0.2), (J["hy"] + J["hh"] * 0.92, 3.3, 3.5, 0, J["hz"] - 0.1), (J["hy"] + J["hh"] + 0.5, 1.6, 1.8, 0, J["hz"])], 10, hair)
    for k in range(9):                                                                     # spikes round the bowl, a longer fringe at the front
        a = 2 * math.pi * k / 9
        r0 = 3.2
        be.shard(f"spike_{k}", "head", (math.sin(a) * r0, J["hy"] + J["hh"] * 0.8, J["hz"] + math.cos(a) * r0), (math.sin(a) * (r0 + 1.6), J["hy"] + J["hh"] * 0.8 + (1.4 if math.cos(a) < 0.3 else -1.0), J["hz"] + math.cos(a) * (r0 + 1.6)), 0.85, hair, n=4)
    mouth = (0.3, J["hy"] + 1.3, J["fz"](J["hy"] + 1.3) + 0.1)
    tube("lolli_stick", "head", [mouth, (mouth[0] + 1.6, mouth[1] - 0.6, mouth[2] + 2.4), (mouth[0] + 2.4, mouth[1] - 1.0, mouth[2] + 3.6)], 0.2, 0.2, trim, sides=4, sub=1)
    disc = loftrows("lolli_disc", "head", [(0.0, 1.6, 0.3), (0.5, 1.6, 0.3)], 10, candy)
    bl.move(disc, (mouth[0] + 2.4, mouth[1] - 1.0, mouth[2] + 4.0), (90, 0, 0))
    return rig(J)


# ================================================================ 4. kin of Astaroth, King of Rot
def build_astaroth():
    be.seed(404)
    flesh = M("as_flesh", (96, 116, 52), rough=0.8, bump="leather", bscale=0.9, bstr=0.45, groove=(34, 44, 18), gamount=0.55)
    add_veins(flesh, "C8F040", scale=0.26, width=0.035, region=0.66, rscale=0.2, strength=4.0)
    bone = M("as_bone", (190, 184, 150), rough=0.6, bump="grain", bscale=1.0, bstr=0.3, groove=(110, 104, 80))
    rag = M("as_rag", (48, 50, 30), rough=0.95, bump="leather", bscale=0.8, bstr=0.4, groove=(20, 22, 12))
    wingm = M("as_wing", (70, 84, 40), rough=0.8, bump="leather", bscale=0.7, bstr=0.35, groove=(26, 34, 14))
    spore, eye, rot = E("as_spore", "9AE030", 7.0), E("as_eye", "D8FF50", 14.0), E("as_rot", "B8E83A", 6.0)
    J = humanoid(dict(leg=14.0, torso=10.0, hipw=2.7, hipd=2.2, waistw=2.2, waistd=1.8, chestw=3.6, chestd=2.5, shw=4.8, shd=2.5, neck=1.2, headw=3.0, headd=3.8, headh=7.2, stoop=3.4,
                      upper=7.0, lower=7.8, armr=(1.2, 1.0, 0.9), forer=(0.9, 1.0, 0.8), palm=1.1, flen=5.2, legr=(1.9, 1.6, 1.3, 1.1, 1.0), digi=1.0, sq=0.05), flesh, claw=bone, handskin=bone)
    shy, leg = J["shy"], J["leg"]
    st = J["zo"]
    for sx in (1, -1):                                                                   # exposed ribs and a glowing rot cavity
        for k in range(4):
            y = shy - 2.0 - k * 1.7
            tube(f"rib_{sx}_{k}", "torso", [(0.6 * sx, y, J["tz"](y) + 0.2), (3.0 * sx, y, J["tz"](y) - 0.3), (4.3 * sx, y + 0.4, st(y) - 1.0)], 0.5, 0.35, bone, sides=4, sub=2)
    bl.gem("cavity", 1.4, rot, center=(0, leg + 6.0, J["tz"](leg + 6.0) - 0.1), depth=0.5, crown=0.4, sides=6, axis="z", rx=1.6, ry=2.4, part="torso")
    tube("spine", "torso", [(0, leg + 1.0, st(leg + 1.0) - 2.0), (0, leg + 6.0, st(leg + 6.0) - 2.5), (0, shy, st(shy) - 2.4)], 0.55, 0.5, bone, sides=4, sub=3)
    # the tattered shroud: ragged strips down the back and over the shoulders
    for k in range(9):
        x = (k - 4) * 1.15
        ln = 11.0 + (k % 3) * 3.0 - abs(k - 4) * 0.5
        slab(f"shroud_{k}", "torso", [(-0.6, 0), (0.6, 0), (0.7 + 0.2 * (k % 2), -ln * 0.5), (0.2, -ln), (-0.5, -ln * 0.8), (-0.8, -ln * 0.4)], 0.18, rag, (x, shy - 1.2, st(shy) - 3.2 - abs(k - 4) * 0.2), (4, 0, 0))
    # head: a gaunt skull-like face, hollow glowing eyes, a jaw of teeth (decay is shown by ribs, shroud, bone and a rotting glow; no wings, antlers or tail are invented)
    eyes(J, eye, J["hy"] + J["hh"] * 0.5, x=1.3, rx=0.9, ry=0.8, r=0.8)
    for k in range(7):
        be.shard(f"tooth_{k}", "head", ((k - 3) * 0.7, J["hy"] + 0.8, J["fz"](J["hy"] + 0.8) - 0.2), ((k - 3) * 0.7, J["hy"] - 0.8 - (k % 2) * 0.5, J["fz"](J["hy"] + 0.8) + 0.2), 0.28, bone, n=3)
    return rig(J)


# ================================================================ 5. kin of Lucifer, King of Light
def build_lucifer():
    be.seed(505)
    skin = M("lu_skin", (240, 226, 214), rough=0.5, bump="leather", bscale=0.4, bstr=0.1, bevel=0.1)
    coat = M("lu_coat", (244, 240, 232), rough=0.55, bump="wrap", bscale=0.6, bstr=0.2, groove=(190, 184, 170))
    gold = M("lu_gold", (244, 208, 110), rough=0.25, metal=0.9, bump="brushed", bscale=2.0, bstr=0.15, groove=(150, 110, 40))
    dark = M("lu_dark", (30, 26, 40), rough=0.45, bump="wrap", bscale=0.6, bstr=0.2, groove=(12, 10, 18))
    hair = M("lu_hair", (255, 244, 190), rough=0.4, bump="brushed", bscale=2.0, bstr=0.2, groove=(210, 190, 120))
    feath = M("lu_feather", (250, 246, 236), rough=0.5, bump="brushed", bscale=1.6, bstr=0.2, groove=(200, 190, 170))
    glow, eye = E("lu_glow", "FFF0A8", 7.0), E("lu_eye", "FFF8C8", 14.0)
    J = humanoid(dict(leg=14.5, torso=10.5, hipw=3.1, hipd=2.4, waistw=2.6, waistd=2.0, chestw=4.0, chestd=2.7, shw=4.6, shd=2.4, neck=1.35, headw=3.1, headd=3.4, headh=6.6,
                      upper=5.8, lower=6.0, armr=(1.5, 1.4, 1.2), forer=(1.25, 1.2, 1.0), palm=1.3, flen=2.8, legr=(2.4, 2.1, 1.8, 1.6, 1.5)), coat, claw=gold, handskin=skin, headskin=skin)
    shy, leg = J["shy"], J["leg"]
    # tailcoat: a long split skirt, high collar, gold epaulettes, buttons, cravat, trousers
    sk = loftrows("skirt", "torso", [(leg + 1.6, 3.5, 2.8), (leg - 3.0, 4.0, 3.2), (leg - 7.5, 4.3, 3.4)], 10, coat, caps=(False, False))
    bl.solidify(sk, 0.35)
    for sx in (1, -1):
        slab(f"coattail_{sx}", "torso", [(-1.7, 0), (1.7, 0), (2.0, -9.0), (0, -12.5), (-2.0, -9.0)], 0.3, coat, (1.9 * sx, leg + 0.6, -2.7), (6, 0, 0))
        slab(f"lapel_{sx}", "torso", [(0, 0), (2.0, -1.2), (2.2, -4.5), (0, -6.0)], 0.25, dark, (0.6 * sx, shy - 0.8, J["tz"](shy - 1) + 0.15), (0, 0, 0))
        e = be.boulder(f"epaulette_{sx}", "arm_l" if sx > 0 else "arm_r", (sx * (J["S"].x + 0.4), shy - 0.5, 0), 2.3, gold, squash=(1.2, 0.5, 1.0), seg=(6, 4), jag=0.02)
        for k in range(3):
            tube(f"fringe_{sx}_{k}", "arm_l" if sx > 0 else "arm_r", [(sx * (J["S"].x + 1.0 + k * 0.6), shy - 1.0, 0.8 - k * 0.8), (sx * (J["S"].x + 1.2 + k * 0.6), shy - 3.0, 0.8 - k * 0.8)], 0.3, 0.2, gold, sides=4, sub=1)
    for k in range(4):
        bl.gem(f"button_{k}", 0.4, gold, center=(0.0, leg + 2.0 + k * 2.3, J["tz"](leg + 2.0 + k * 2.3) + 0.1), depth=0.5, crown=0.5, sides=6, axis="z", part="torso")
    bl.gem("cravat", 0.9, glow, center=(0, shy + 0.3, J["tz"](shy) + 0.3), depth=0.5, crown=0.5, sides=6, axis="z", part="torso")
    cuff = loftrows("cuff_l", "forearm_l", [(J["W"].y + 1.5, 1.6, 1.5, J["W"].x, J["W"].z), (J["W"].y - 0.3, 1.7, 1.6, J["W"].x, J["W"].z + 0.1)], 8, gold)
    be.side_copy(cuff, "forearm_r")
    # head: pointed ears, swept golden hair, glowing gold eyes, a tall gilded top hat, a halo ring behind
    eyes(J, eye, J["hy"] + J["hh"] * 0.46, x=1.3, rx=0.9, ry=0.38, tilt=-8)
    for sx in (1, -1):
        be.shard(f"ear_{sx}", "head", (J["hw"] * 0.9 * sx, J["hy"] + 3.0, J["hz"]), (J["hw"] * sx + 3.4 * sx, J["hy"] + 4.8, J["hz"] - 0.8), 0.7, skin, n=4)
    for k in range(7):
        a = (k - 3) * 0.4
        tube(f"hair_{k}", "head", [(a * 1.6, J["hy"] + J["hh"] * 0.8, J["hz"] + 2.0), (a * 2.3, J["hy"] + J["hh"] * 0.6, J["hz"] - 3.4), (a * 2.6, J["hy"] + 2.0, J["hz"] - 4.6), (a * 2.4, J["hy"] - 2.5, J["hz"] - 3.6)],
             0.95, 0.2, hair, sides=4, sub=2)
    torus("halo", "head", (0, J["hy"] + J["hh"] * 0.55, J["hz"] - 5.2), 5.4, 0.45, glow, rot=(0, 0, 0))
    # wings: three tiers of long feathers, gold at the tips
    root = V((3.0, shy - 2.0, -3.0))
    for tier, (ln, base_y, n, spread, tl) in enumerate(((16.0, 1.0, 6, 90, -14), (12.5, -2.0, 5, 80, -22), (9.0, -5.0, 4, 66, -30))):
        for k in range(n):
            ang = -(15 + k * (spread - 15) / max(n - 1, 1))
            f = feather(f"feather_{tier}_{k}", "wing_l", (root.x + 1.0 + k * 0.2, root.y + base_y, root.z - tier * 0.5), ang, ln * (1.0 - 0.07 * k), 3.0 - tier * 0.2, feath, tilt=tl + k * 2, thick=0.28)
            if tier == 0:                                                                  # a glowing quill line down the long feathers
                L2 = ln * (1.0 - 0.07 * k)
                b0 = V((root.x + 1.0 + k * 0.2, root.y + base_y, root.z - 0.4))
                d0 = V((-math.sin(math.radians(ang)), math.cos(math.radians(ang)), -0.2))
                tube(f"quill_{k}", "wing_l", [b0 + d0 * L2 * 0.3, b0 + d0 * L2 * 0.9], 0.22, 0.15, glow, sides=4, sub=1)
    tube("wing_bone", "wing_l", [root, V((9, shy + 1.0, root.z - 1.0)), V((15, shy + 3.0, root.z - 1.6))], 0.8, 0.3, gold, sides=5, sub=2)
    mirror_part("wing_l", "wing_r")
    return rig(J, wings=(3.0, shy - 2.0, -3.0))


# ================================================================ 6. daemon of Noir (the black Primordial's line)
def build_noir():
    be.seed(606)
    skin = M("no_skin", (34, 34, 48), rough=0.35, metal=0.45, bump="leather", bscale=0.5, bstr=0.15, groove=(12, 12, 20))
    coat = M("no_coat", (22, 22, 32), rough=0.5, bump="wrap", bscale=0.6, bstr=0.25, groove=(8, 8, 14))
    silver = M("no_silver", (200, 200, 220), rough=0.2, metal=1.0, bump="brushed", bscale=2.0, bstr=0.12, groove=(90, 90, 110))
    horn = M("no_horn", (26, 24, 36), rough=0.2, metal=0.6, bump="rings", bscale=1.5, bstr=0.3, groove=(8, 8, 14))
    blade = M("no_blade", (18, 18, 28), rough=0.2, metal=0.7, bump="brushed", bscale=2.0, bstr=0.15, groove=(6, 6, 12))
    add_veins(skin, "9A88FF", scale=0.22, width=0.025, region=0.66, rscale=0.2, strength=3.0)
    glow, eye = E("no_glow", "9A88FF", 8.0), E("no_eye", "E8E0FF", 16.0)
    J = humanoid(dict(leg=14.5, torso=10.5, hipw=3.0, hipd=2.3, waistw=2.5, waistd=1.9, chestw=3.9, chestd=2.6, shw=4.7, shd=2.3, neck=1.2, headw=3.0, headd=3.4, headh=6.8,
                      upper=6.4, lower=7.0, armr=(1.3, 1.1, 0.95), forer=(1.0, 1.1, 0.9), palm=1.2, flen=4.2, legr=(2.1, 1.8, 1.5, 1.3, 1.2), digi=0.3), skin, claw=silver)
    shy, leg = J["shy"], J["leg"]
    # long tailcoat with silver trim and a high collar; shoulder spikes; vertical glowing seams
    sk = loftrows("skirt", "torso", [(leg + 2.0, 3.5, 2.8), (leg - 4.0, 4.3, 3.4), (leg - 10.5, 5.0, 3.8)], 10, coat, caps=(False, False))
    bl.solidify(sk, 0.35)
    for sx in (1, -1):
        slab(f"coattail_{sx}", "torso", [(-2.0, 0), (2.0, 0), (2.5, -12.0), (0, -17.0), (-2.5, -12.0)], 0.3, coat, (2.2 * sx, leg + 1.0, -3.0), (8, 0, 0))
        slab(f"collar_{sx}", "torso", [(0, 0), (2.6, 0.6), (3.0, 4.2), (0.4, 2.8)], 0.25, coat, (0.6 * sx, shy + 0.2, 1.2), (0, 0, 0))
        tube(f"trim_{sx}", "torso", [(1.0 * sx, shy - 0.5, J["tz"](shy) + 0.1), (1.7 * sx, leg + 5.0, J["tz"](leg + 5.0) + 0.1), (1.9 * sx, leg + 1.5, J["tz"](leg + 1.5) + 0.1)], 0.22, 0.22, glow, sides=4, sub=2)
    bl.gem("sigil", 1.1, glow, center=(0, leg + 7.4, J["tz"](leg + 7.4) + 0.2), depth=0.5, crown=0.5, sides=6, axis="z", part="torso")
    for k, dy in enumerate((0.0, 1.8)):
        cf = loftrows(f"cuff_l{k}", "forearm_l", [(J["W"].y + 2.0 - dy, 1.3, 1.2, J["W"].x, J["W"].z), (J["W"].y + 1.0 - dy, 1.35, 1.25, J["W"].x, J["W"].z)], 8, silver)
        be.side_copy(cf, "forearm_r")
    # head: a smooth mask-like face, burning eyes, a crown of long swept horns, a spiked void ring behind
    eyes(J, eye, J["hy"] + J["hh"] * 0.46, x=1.3, rx=1.0, ry=0.42, tilt=-18)
    for sx in (1, -1):
        tube(f"horn_{sx}", "head", [(2.2 * sx, J["hy"] + 5.4, J["hz"] + 0.6), (3.6 * sx, J["hy"] + 8.4, J["hz"] - 0.4), (3.6 * sx, J["hy"] + 9.5, J["hz"] - 2.4), (2.4 * sx, J["hy"] + 11.5, J["hz"] - 5.8)], 1.2, 0.12, horn, sides=6, sub=3)
    # wings: angular obsidian blades on a bone arm, glowing edge seams
    root = V((3.0, shy - 2.0, -3.0))
    bl.tube("wing_bone", [tuple(root), (9, shy + 1.0, -4.5), (15, shy + 2.5, -5.5), (21, shy + 4.0, -6.0)], None, silver, sides=5, sub=2, part="wing_l", taper=(0.9, 0.35))
    for k, (ang, ln, w) in enumerate(((-112, 17.0, 3.4), (-96, 20.0, 3.8), (-80, 21.0, 3.6), (-64, 19.0, 3.4), (-48, 15.0, 3.0))):
        a = math.radians(ang)
        f = feather(f"blade_{k}", "wing_l", (root.x + 2.5 + k * 3.2, root.y + 2.0 + k * 1.3, root.z - 1.8 - k * 0.3), ang, ln, w, blade, tilt=-10 - k * 3, thick=0.32, tip=0.0)
        tube(f"seam_{k}", "wing_l", [(root.x + 2.5 + k * 3.2, root.y + 2.0 + k * 1.3, root.z - 2.2 - k * 0.3), (root.x + 2.5 + k * 3.2 - math.sin(a) * ln * 0.5, root.y + 2.0 + k * 1.3 + math.cos(a) * ln * 0.5, root.z - 2.6 - k * 0.3)],
             0.2, 0.12, glow, sides=4, sub=1)
    mirror_part("wing_l", "wing_r")
    return rig(J, wings=(3.0, shy - 2.0, -3.0))


# ================================================================ 7. daemon of Blanc (the white Primordial's line)
def build_blanc():
    be.seed(707)
    skin = M("bl_skin", (246, 244, 255), rough=0.4, bump="leather", bscale=0.4, bstr=0.08, groove=(200, 204, 230))
    gown = M("bl_gown", (240, 240, 255), rough=0.5, bump="wrap", bscale=0.5, bstr=0.2, groove=(176, 182, 220))
    lace = M("bl_lace", (255, 255, 255), rough=0.4, bump="scales", bscale=0.8, bstr=0.2, groove=(190, 196, 235))
    hair = M("bl_hair", (250, 250, 255), rough=0.35, bump="brushed", bscale=2.0, bstr=0.2, groove=(190, 196, 235))
    crystal = M("bl_crystal", (200, 224, 255), rough=0.08, metal=0.1, bump="brushed", bscale=2.0, bstr=0.1, groove=(120, 160, 230))
    silver = M("bl_silver", (214, 218, 240), rough=0.2, metal=0.9, bump="brushed", bscale=2.0, bstr=0.12, groove=(120, 124, 160))
    glow, eye = E("bl_glow", "A8C8FF", 7.0), E("bl_eye", "D8ECFF", 15.0)
    J = humanoid(dict(leg=14.0, torso=10.0, hipw=2.9, hipd=2.2, waistw=2.3, waistd=1.8, chestw=3.6, chestd=2.5, shw=4.3, shd=2.2, neck=1.25, headw=3.0, headd=3.3, headh=6.6,
                      upper=5.8, lower=6.2, armr=(1.2, 1.05, 0.95), forer=(1.05, 1.15, 0.95), palm=1.2, flen=3.0, legr=(2.0, 1.75, 1.4, 1.2, 1.1), toes=True), skin, claw=silver)
    shy, leg = J["shy"], J["leg"]
    # the gown: a bodice, a flaring layered skirt (wide enough for the legs to swing), petal hems, a glowing sash
    bod = loftrows("bodice", "torso", [(leg + 3.0, 3.0, 2.5), (leg + 5.5, 3.1, 2.4), (shy - 0.2, 4.4, 2.6)], 10, gown, caps=(False, False))
    bl.solidify(bod, 0.35)
    for tier, (y0, r0, y1, r1) in enumerate(((leg + 1.5, 3.8, leg - 5.0, 6.4), (leg - 3.0, 5.0, leg - 9.5, 7.6), (leg - 8.0, 6.4, 1.2, 8.4))):
        sk = loftrows(f"skirt{tier}", "torso", [(y0, r0, r0 * 0.9), (y1, r1, r1 * 0.9)], 12, lace if tier else gown, caps=(False, False))
        bl.solidify(sk, 0.3)
    for k in range(8):                                                                    # petals on the hem
        a = 2 * math.pi * k / 8
        slab(f"petal_{k}", "torso", [(-1.6, 0), (1.6, 0), (1.2, -3.0), (0, -4.4), (-1.2, -3.0)], 0.2, lace, (math.sin(a) * 8.2, 3.6, math.cos(a) * 7.4), (0, math.degrees(a), 0))
    sash = loftrows("sash", "torso", [(leg + 0.5, 3.0, 2.4), (leg + 1.9, 2.95, 2.35)], 10, glow)
    bl.gem("heart", 1.1, glow, center=(0, leg + 7.4, J["tz"](leg + 7.4) + 0.2), depth=0.5, crown=0.5, sides=6, axis="z", part="torso")
    # long hanging ribbon sleeves on the forearms
    for k in range(2):
        r = slab(f"sleeve_l{k}", "forearm_l", [(-0.6, 0), (0.6, 0), (0.9, -9.0 - k * 2), (0.1, -12.0 - k * 2), (-0.7, -9.0 - k * 2)], 0.15, lace, (J["E"].x + 1.2 + k * 1.0, J["E"].y - 0.5, J["E"].z + (k - 0.5) * 1.4), (0, 90 * 0, 0))
        be.side_copy(r, "forearm_r")
    # head: a calm face, glowing pale eyes, a silver circlet with a gem, curved glass antler horns, long white hair to the knees
    eyes(J, eye, J["hy"] + J["hh"] * 0.46, x=1.3, rx=0.9, ry=0.4, tilt=-6)
    circ = loftrows("circlet", "head", [(J["hy"] + J["hh"] * 0.77, 3.15, 3.35, 0, J["hz"]), (J["hy"] + J["hh"] * 0.86, 3.05, 3.25, 0, J["hz"])], 12, silver)
    bl.gem("circlet_gem", 0.9, glow, center=(0, J["hy"] + J["hh"] * 0.84, J["fz"](J["hy"] + J["hh"] * 0.84) + 0.1), depth=0.5, crown=0.5, sides=6, axis="z", part="head")
    for sx in (1, -1):
        tube(f"horn_{sx}", "head", [(2.4 * sx, J["hy"] + 5.4, J["hz"] + 0.2), (3.8 * sx, J["hy"] + 7.4, J["hz"] - 0.8), (4.0 * sx, J["hy"] + 9.4, J["hz"] - 2.6)], 0.9, 0.12, crystal, sides=5, sub=3)
    for k in range(8):
        a = (k - 3.5) * 0.36
        tube(f"hair_{k}", "head", [(a * 2.2, J["hy"] + J["hh"] * 0.85, J["hz"] + 1.8), (a * 3.4, J["hy"] + J["hh"] * 0.55, J["hz"] - 3.4), (a * 3.8, J["hy"] + 1.0, J["hz"] - 4.6), (a * 3.4, J["hy"] - 8.0, J["hz"] - 5.2),
                                (a * 3.0, J["hy"] - 15.0, J["hz"] - 4.6)], 1.0, 0.18, hair, sides=4, sub=2)
    # wings: the Tensura demon's wings in white (the same bat-blade build as Noir's, smaller): a bone arm and pale blades with a soft glow seam
    root = V((3.0, shy - 2.0, -3.0))
    bl.tube("wing_bone", [tuple(root), (8, shy + 0.5, -4.5), (13, shy + 1.5, -5.5), (18, shy + 3.0, -6.0)], None, silver, sides=5, sub=2, part="wing_l", taper=(0.8, 0.3))
    for k, (ang, ln, w) in enumerate(((-112, 14.0, 3.0), (-96, 16.5, 3.4), (-80, 17.5, 3.2), (-64, 16.0, 3.0), (-48, 12.5, 2.6))):
        a = math.radians(ang)
        bx, by, bz = root.x + 2.2 + k * 2.6, root.y + 1.8 + k * 1.1, root.z - 1.8 - k * 0.3
        feather(f"blade_{k}", "wing_l", (bx, by, bz), ang, ln, w, crystal, tilt=-10 - k * 3, thick=0.3)
        if k % 2 == 0:
            tube(f"seam_{k}", "wing_l", [(bx, by, bz - 0.4), (bx - math.sin(a) * ln * 0.45, by + math.cos(a) * ln * 0.45, bz - 0.8)], 0.2, 0.12, glow, sides=4, sub=1)
    mirror_part("wing_l", "wing_r")
    return rig(J, wings=(3.0, shy - 2.0, -3.0))


# ================================================================ previews (bl_entity's renderer with brighter light and a tighter frame)
def render_previews(low, names, rig_, atlas, previews, poses, res=(520, 700), samples=28):
    from mathutils import Euler, Matrix
    sc = bpy.context.scene
    for o in sc.collection.objects:
        o.hide_render = True
    mat = be._preview_material(atlas)
    mat.node_tree.nodes["Principled BSDF"].inputs["Emission Strength"].default_value = 1.6
    mat.node_tree.nodes["Principled BSDF"].inputs["Roughness"].default_value = 0.65
    w = bpy.data.worlds.new("preview")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs["Color"].default_value = (0.2, 0.22, 0.27, 1)
    w.node_tree.nodes["Background"].inputs["Strength"].default_value = 1.3
    sc.world = w
    sun = bpy.data.objects.new("sun", bpy.data.lights.new("sun", "SUN"))
    sun.data.energy = 4.5
    sc.collection.objects.link(sun)
    fill = bpy.data.objects.new("fill", bpy.data.lights.new("fill", "SUN"))
    fill.data.energy = 1.8
    sc.collection.objects.link(fill)
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 50
    sc.collection.objects.link(cam)
    sc.camera = cam
    sc.render.resolution_x, sc.render.resolution_y = res
    sc.cycles.samples = samples
    sc.view_settings.view_transform = "Standard"
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGB"
    for path, (yaw, _pose) in previews.items():
        me = low.data.copy()
        me.materials.clear()
        me.materials.append(mat)
        ob = bpy.data.objects.new("pv", me)
        sc.collection.objects.link(ob)
        lo, hi = Vector([min(v.co[i] for v in me.vertices) for i in range(3)]), Vector([max(v.co[i] for v in me.vertices) for i in range(3)])
        mid = (lo + hi) / 2
        size = max((hi.y - lo.y) * 1.05, (hi.x - lo.x) * 0.8)
        ob.matrix_world = Matrix.Rotation(math.radians(90), 4, "X")
        a = math.radians(yaw)
        c = Vector((mid.x, -mid.z, mid.y))
        dist = size * 2.3
        cam.location = c + Vector((math.sin(a) * dist, -math.cos(a) * dist, size * 0.12))
        cam.rotation_euler = (c - cam.location).to_track_quat("-Z", "Y").to_euler()
        sun.rotation_euler = Euler((math.radians(48), 0, a + math.radians(30)), "XYZ")
        fill.rotation_euler = Euler((math.radians(60), 0, a - math.radians(110)), "XYZ")
        sc.render.filepath = os.path.abspath(path)
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        bpy.ops.render.render(write_still=True)
        bpy.data.objects.remove(ob)
        bpy.data.meshes.remove(me)
        be._shrink(path)
        print("preview", path, os.path.getsize(path) // 1024, "KB")



DEVILS = {
    "kin_of_iblis": (build_iblis, ("FF6A2A", 0.14, 0.6, 1.0), 0.40),
    "kin_of_egyn": (build_egyn, ("3A8AE8", 0.08, 0.6, 1.0), 0.33),
    "kin_of_amaimon": (build_amaimon, ("FFB838", 0.06, 0.55, 1.0), 0.33),
    "kin_of_astaroth": (build_astaroth, ("6A8A2A", 0.1, 0.55, 1.0), 0.33),
    "kin_of_lucifer": (build_lucifer, ("FFF0A8", 0.1, 0.65, 1.0), 0.33),
    "daemon_noir": (build_noir, ("9A88FF", 0.07, 0.55, 1.0), 0.40),
    "daemon_blanc": (build_blanc, ("C8DCFF", 0.07, 0.65, 1.0), 0.33),
}


def shrink_skin(path, colors=256):
    """The baked skin as a palette PNG (Pillow's fast octree keeps the alpha): about 280 KB at 1024 px instead of 1.2 MB."""
    from PIL import Image
    im = Image.open(path).convert("RGBA")
    im.quantize(colors=colors, method=Image.Quantize.FASTOCTREE).save(path, optimize=True)
    return os.path.getsize(path)


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    only = argv[argv.index("--only") + 1].split(",") if "--only" in argv else list(DEVILS)
    pv = trial or PREVIEW_DIR
    be.render_previews = render_previews
    for did in only:
        fn, glow, lod = DEVILS[did]
        eid = "devil_" + did
        bl.reset(samples=8 if quick else 20)
        r = fn()
        total = sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")
        print(did, "faces before export:", total)
        previews = {os.path.join(pv, did + ".png"): (28, None), os.path.join(pv, did + "_back.png"): (205, None)}
        be.finish_entity(eid, r, glow=glow, skin=1024 if not quick else 512, bake=1024 if not quick else 512, lod_ratio=lod, out_mesh=trial, out_skin=trial, previews=previews, poses={}, cage=0.2)
        for s in (eid + ".png", eid + "_glow.png"):
            print("skin", s, shrink_skin(os.path.join(trial or be.ENTITY_SKIN_DIR, s)) // 1024, "KB")


if __name__ == "__main__":
    main(sys.argv[1:])

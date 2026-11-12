"""Blender weapon pipeline (3.80.0): the hero weapons are MADE IN BLENDER and exported to the mod's weapon mesh format.

Run with a Python that has Blender as a module (`pip install bpy pillow numpy`, Blender 5.x):  python tools/blender/<weapon script>.py

What Blender does here (the part the old code-built meshes could not do):
  - modelling with Blender's own tools: bmesh, modifiers (bevel, subdivision, solidify, decimate), text objects for rune inlays;
  - materials as real shaders (Principled BSDF: metal, roughness, bump textures, the Bevel node for rounded edges);
  - UV unwrapping (smart project, islands packed into one skin);
  - BAKING with Cycles: the lit, shaded, bumped surface (and any bake-only detail floating over it) is rendered into the weapon's
    512 px skin, the emission into its glow skin.
What the mod's own tools still do: the mesh file itself (tools/relics/mesh_lib.py writes the `place` / `grip` / `qn` lines and applies
tools/relics/weapon_fixes.json), and the software preview that shows the mesh as the game draws it.

Coordinates are the mod's: y is the weapon's length (cm), x its width, z its thickness (the faces a player sees are +z and -z).
Cost in game: none beyond the mesh's quads (budget 2800 quad lines per mesh, the cap of MeshCap is 3000); the detail is in the skin.
"""
import math
import os
import sys

import bpy  # noqa: I001  (bpy first: it provides bmesh and mathutils)
import bmesh
import numpy as np
from mathutils import Matrix, Vector

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
import mesh_lib as ml  # noqa: E402

ASSETS = ml.ASSETS
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
SKIN_DIR = os.path.join(ASSETS, "textures", "entity", "weapon")
QUAD_BUDGET = 2800


# ================================================================ scene, light
def reset(samples=24):
    """An empty scene set up for baking: Cycles on the CPU, and a studio world: bright in front of and behind the weapon (its +z / -z
    faces), darker to the sides, slightly brighter from above. A metal bakes as the world seen along its normal, so flats come out
    bright and bevels dark, which is what makes the facets read in the skin."""
    bpy.ops.wm.read_factory_settings(use_empty=True)
    sc = bpy.context.scene
    sc.render.engine = "CYCLES"
    sc.cycles.device = "CPU"
    sc.cycles.samples = samples
    sc.cycles.use_denoising = False
    w = bpy.data.worlds.new("studio")
    sc.world = w
    w.use_nodes = True
    nt = w.node_tree
    N, L = nt.nodes, nt.links
    bg = N["Background"]
    tc = N.new("ShaderNodeTexCoord")
    sep = N.new("ShaderNodeSeparateXYZ")
    L.new(tc.outputs["Generated"], sep.inputs[0])
    az = _math(nt, "ABSOLUTE", sep.outputs["Z"])
    pw = _math(nt, "POWER", az, 1.6)
    up = _math(nt, "MULTIPLY_ADD", sep.outputs["Y"], 0.12, 0.12)          # 0 .. 0.24 from below to above
    side = _math(nt, "ABSOLUTE", sep.outputs["X"])
    band = _math(nt, "MULTIPLY", _math(nt, "POWER", side, 6.0), 0.35)     # a rim light from the sides (edges catch it)
    fac = _math(nt, "ADD", _math(nt, "ADD", _math(nt, "MULTIPLY_ADD", pw, 0.95, 0.16), up), band)
    ramp = N.new("ShaderNodeMixRGB")
    ramp.inputs[1].default_value = (0.03, 0.035, 0.05, 1)
    ramp.inputs[2].default_value = (1.0, 1.0, 1.0, 1)
    L.new(_clamp(nt, fac), ramp.inputs[0])
    L.new(ramp.outputs[0], bg.inputs["Color"])
    bg.inputs["Strength"].default_value = 1.35
    return sc


def _math(nt, op, a, b=None, c=None):
    n = nt.nodes.new("ShaderNodeMath")
    n.operation = op
    for i, v in enumerate((a, b, c)):
        if v is None:
            continue
        if isinstance(v, (int, float)):
            n.inputs[i].default_value = v
        else:
            nt.links.new(v, n.inputs[i])
    return n.outputs[0]


def _clamp(nt, a):
    n = nt.nodes.new("ShaderNodeClamp")
    nt.links.new(a, n.inputs[0])
    return n.outputs[0]


# ================================================================ materials
def rgb(r, g, b):
    """sRGB 0..255 to the linear colour Blender wants."""
    def lin(c):
        c /= 255.0
        return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4
    return (lin(r), lin(g), lin(b), 1.0)


def mat(name, color, metallic=0.0, rough=0.5, emit=None, strength=0.0, bump=None, bscale=1.0, bstr=0.3, bevel=0.14, groove=None, gamount=0.75):
    """A Principled material. bump: 'brushed' (streaks along the length), 'brushed_x', 'hammer', 'leather', 'wrap' (a spiral cord round
    the y axis), 'cross' (two spirals crossing: a braided grip), 'rings' (bands round y), 'scales', 'grain' (wood). groove: the colour
    the bump's low parts are darkened to (a wrap's gaps, a hammer mark's pits). bevel: the radius of the rounded edges the Bevel node
    bakes into the shading (no geometry)."""
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    bs.inputs["Base Color"].default_value = color
    bs.inputs["Metallic"].default_value = metallic
    bs.inputs["Roughness"].default_value = rough
    if emit is not None and strength > 0:
        bs.inputs["Emission Color"].default_value = emit
        bs.inputs["Emission Strength"].default_value = strength
        m["glow"] = True
    normal = None
    if bevel > 0:
        bv = N.new("ShaderNodeBevel")
        bv.samples = 6
        bv.inputs["Radius"].default_value = bevel
        normal = bv.outputs["Normal"]
    if bump:
        tc = N.new("ShaderNodeTexCoord")
        co = tc.outputs["Object"]
        h = _height(nt, bump, co, bscale)
        bp = N.new("ShaderNodeBump")
        bp.inputs["Strength"].default_value = bstr
        bp.inputs["Distance"].default_value = 0.35
        L.new(h, bp.inputs["Height"])
        if normal is not None:
            L.new(normal, bp.inputs["Normal"])
        normal = bp.outputs["Normal"]
        if groove is not None:
            mx = N.new("ShaderNodeMixRGB")
            mx.inputs[1].default_value = groove
            mx.inputs[2].default_value = color
            f = _clamp(nt, _math(nt, "MULTIPLY_ADD", h, gamount, 1.0 - gamount)) if gamount < 1.0 else _clamp(nt, h)
            L.new(f, mx.inputs[0])
            L.new(mx.outputs[0], bs.inputs["Base Color"])
    if normal is not None:
        L.new(normal, bs.inputs["Normal"])
    return m


def _height(nt, kind, co, s):
    """The bump height (0..1) of a pattern over object coordinates."""
    N, L = nt.nodes, nt.links

    def mapped(sx, sy, sz):
        mp = N.new("ShaderNodeMapping")
        mp.inputs["Scale"].default_value = (sx, sy, sz)
        L.new(co, mp.inputs["Vector"])
        return mp.outputs[0]

    def noise(vec, scale, detail=4.0, rough=0.6):
        n = N.new("ShaderNodeTexNoise")
        n.inputs["Scale"].default_value = scale
        n.inputs["Detail"].default_value = detail
        n.inputs["Roughness"].default_value = rough
        L.new(vec, n.inputs["Vector"])
        return n.outputs["Fac"] if "Fac" in n.outputs else n.outputs[0]

    if kind == "brushed":
        return noise(mapped(5.0 * s, 0.06 * s, 5.0 * s), 1.0, 5.0)
    if kind == "brushed_x":
        return noise(mapped(0.06 * s, 5.0 * s, 5.0 * s), 1.0, 5.0)
    if kind == "hammer":
        v = N.new("ShaderNodeTexVoronoi")
        v.inputs["Scale"].default_value = 0.55 * s
        L.new(co, v.inputs["Vector"])
        return _clamp(nt, _math(nt, "MULTIPLY", v.outputs["Distance"], 1.6))
    if kind == "leather":
        return noise(co, 2.2 * s, 8.0, 0.7)
    if kind == "grain":
        return noise(mapped(3.0 * s, 0.12 * s, 3.0 * s), 1.0, 6.0, 0.65)
    sep = N.new("ShaderNodeSeparateXYZ")
    L.new(co, sep.inputs[0])
    x, y, z = sep.outputs
    if kind == "rings":
        return _math(nt, "MULTIPLY_ADD", _math(nt, "SINE", _math(nt, "MULTIPLY", y, 2.2 * s)), 0.5, 0.5)
    ang = _math(nt, "ARCTAN2", x, z)
    if kind == "wrap":
        ph = _math(nt, "ADD", _math(nt, "MULTIPLY", ang, 1.0), _math(nt, "MULTIPLY", y, 2.4 * s))
        return _math(nt, "POWER", _math(nt, "MULTIPLY_ADD", _math(nt, "SINE", ph), 0.5, 0.5), 0.6)
    if kind == "cross":
        a = _math(nt, "SINE", _math(nt, "ADD", _math(nt, "MULTIPLY", ang, 2.0), _math(nt, "MULTIPLY", y, 1.9 * s)))
        b = _math(nt, "SINE", _math(nt, "SUBTRACT", _math(nt, "MULTIPLY", y, 1.9 * s), _math(nt, "MULTIPLY", ang, 2.0)))
        return _math(nt, "POWER", _math(nt, "MULTIPLY_ADD", _math(nt, "MAXIMUM", a, b), 0.5, 0.5), 0.8)
    if kind == "scales":
        a = _math(nt, "SINE", _math(nt, "ADD", _math(nt, "MULTIPLY", ang, 6.0), _math(nt, "MULTIPLY", y, 2.6 * s)))
        b = _math(nt, "SINE", _math(nt, "MULTIPLY", y, 2.6 * s))
        return _math(nt, "MULTIPLY_ADD", _math(nt, "MULTIPLY", a, b), 0.5, 0.5)
    raise ValueError(kind)


# ================================================================ objects
def obj(name, verts, faces, material, part=None, detail=False):
    """A mesh object from points and faces (any face size); faces are turned outward. part: the name its quads carry in the mesh file
    (default: the object's name). detail=True: baked into the skin only, never exported (a rune, an engraving floating on a surface)."""
    me = bpy.data.meshes.new(name)
    me.from_pydata([tuple(v) for v in verts], [], [tuple(f) for f in faces])
    me.update()
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    if material is not None:
        me.materials.append(material)
    ob["part"] = part or name
    if detail:
        ob["detail"] = True
    outward(ob)
    return ob


def outward(ob):
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(ob.data)
    bm.free()
    return ob


def loft(name, rings, material, caps=(True, True), part=None, closed=True, detail=False):
    """Skins a list of rings (each the same number of 3D points) with quads; caps are n-gons (made quads / triangles at export)."""
    n = len(rings[0])
    verts = [p for r in rings for p in r]
    faces = []
    for i in range(len(rings) - 1):
        for k in range(n if closed else n - 1):
            a, b = i * n + k, i * n + (k + 1) % n
            faces.append((a, b, b + n, a + n))
    if caps[0]:
        faces.append(tuple(range(n - 1, -1, -1)))
    if caps[1]:
        o = (len(rings) - 1) * n
        faces.append(tuple(range(o, o + n)))
    return obj(name, verts, faces, material, part, detail)


def ring(y, section, sx=1.0, sz=1.0, ox=0.0, oz=0.0, tilt=0.0):
    """A cross-section (2D points (x, z)) placed at height y; tilt (radians) leans it about the z axis (for a curved limb)."""
    c, s = math.cos(tilt), math.sin(tilt)
    return [Vector((ox + px * sx * c, y + px * sx * s, oz + pz * sz)) for px, pz in section]


def circle(r, n=12, rz=None, start=0.0):
    rz = r if rz is None else rz
    return [(r * math.cos(start + 2 * math.pi * k / n), rz * math.sin(start + 2 * math.pi * k / n)) for k in range(n)]


def lozenge(w, t, flat=0.0):
    """A blade section: half width w, half thickness t; flat > 0 gives a hexagon with a flat of that half width (room for a fuller)."""
    if flat <= 0:
        return [(w, 0), (0, t), (-w, 0), (0, -t)]
    return [(w, 0), (flat, t), (-flat, t), (-w, 0), (-flat, -t), (flat, -t)]


def rrect(w, t, r, n=2):
    """A rounded rectangle, half width w, half thickness t, corner radius r."""
    pts = []
    for cx, cz, a0 in ((w - r, t - r, 0.0), (-(w - r), t - r, 0.5 * math.pi), (-(w - r), -(t - r), math.pi), (w - r, -(t - r), 1.5 * math.pi)):
        for k in range(n + 1):
            a = a0 + 0.5 * math.pi * k / n
            pts.append((cx + r * math.cos(a), cz + r * math.sin(a)))
    return pts


def lathe(name, profile, material, sides=12, part=None, squash=1.0, origin=(0, 0, 0), caps=(True, True), axis="y", detail=False):
    """Turns a profile [(radius, height)] round an axis through origin; squash flattens it in z (an oval grip)."""
    rings = []
    for r, h in profile:
        pts = []
        for k in range(sides):
            a = 2 * math.pi * k / sides
            u, v = max(r, 1e-4) * math.cos(a), max(r, 1e-4) * math.sin(a) * squash
            if axis == "y":
                pts.append(Vector((origin[0] + u, origin[1] + h, origin[2] + v)))
            elif axis == "z":
                pts.append(Vector((origin[0] + u, origin[1] + v, origin[2] + h)))
            else:
                pts.append(Vector((origin[0] + h, origin[1] + u, origin[2] + v)))
        rings.append(pts)
    return loft(name, rings, material, caps, part, detail=detail)


def smooth_path(pts, sub=4):
    """Catmull-Rom through the points, `sub` steps per span."""
    P = [Vector(p) for p in pts]
    if len(P) < 3:
        return P
    out = []
    for i in range(len(P) - 1):
        p0, p1, p2, p3 = P[max(i - 1, 0)], P[i], P[i + 1], P[min(i + 2, len(P) - 1)]
        for k in range(sub):
            t = k / sub
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(P[-1])
    return out


def sweep(name, path, section, material, scale=None, up=(0, 0, 1), part=None, caps=(True, True), twist=None, detail=False):
    """Carries a section (2D points (u, v): u across the path in its plane, v along `up`) along a path. scale: one factor per path point,
    or (su, sv) pairs, or a function of t in 0..1. For a curve drawn in the xy plane with up = z, u is the curve's in-plane normal."""
    P = [Vector(p) for p in path]
    upv = Vector(up).normalized()
    rings = []
    for i, p in enumerate(P):
        t = i / max(len(P) - 1, 1)
        tan = (P[min(i + 1, len(P) - 1)] - P[max(i - 1, 0)]).normalized()
        nrm = upv.cross(tan)
        if nrm.length < 1e-6:
            nrm = Vector((1, 0, 0))
        nrm.normalize()
        bi = tan.cross(nrm).normalized()
        s = scale(t) if callable(scale) else (scale[i] if scale is not None else 1.0)
        su, sv = s if isinstance(s, (tuple, list)) else (s, s)
        sec = section(t) if callable(section) else section
        a = twist(t) if callable(twist) else 0.0
        c, sn = math.cos(a), math.sin(a)
        rings.append([p + nrm * ((u * c - v * sn) * su) + bi * ((u * sn + v * c) * sv) for u, v in sec])
    return loft(name, rings, material, caps, part, detail=detail)


def tube(name, pts, radius, material, sides=8, sub=4, part=None, taper=None, detail=False):
    """A round tube through points (smoothed); radius a number, a list per smoothed point, or a function of t; taper=(r0, r1) is linear."""
    path = smooth_path(pts, sub) if sub > 1 else [Vector(p) for p in pts]
    if taper is not None:
        sc = lambda t: taper[0] + (taper[1] - taper[0]) * t                  # noqa: E731
    elif callable(radius):
        sc = radius
    else:
        sc = lambda t: radius                                              # noqa: E731
    return sweep(name, path, circle(1.0, sides), material, scale=sc, part=part, detail=detail)


def plate(name, outline, z0, z1, material, part=None, detail=False):
    """A flat piece: an outline [(x, y)] in the xy plane, from z0 to z1 (z0 < z1)."""
    n = len(outline)
    verts = [(x, y, z0) for x, y in outline] + [(x, y, z1) for x, y in outline]
    faces = [(k, (k + 1) % n, (k + 1) % n + n, k + n) for k in range(n)]
    faces += [tuple(range(n - 1, -1, -1)), tuple(range(n, 2 * n))]
    return obj(name, verts, faces, material, part, detail)


def gem(name, r, material, center=(0, 0, 0), depth=0.6, crown=0.35, sides=8, axis="z", rx=None, ry=None, part=None):
    """A cut stone lying on a face: a table, a crown and a pavilion; axis = the direction the table faces; rx / ry stretch it (an oval)."""
    rx = r if rx is None else rx
    ry = r if ry is None else ry
    prof = [(0.001, depth * r * 0.9 + crown * r), (0.55, depth * r * 0.9 + crown * r), (1.0, depth * r * 0.9), (0.72, 0.0), (0.001, -depth * r * 0.6)]
    rings = []
    for k, (rr, h) in enumerate(prof):
        off = (math.pi / sides) if k in (1, 3) else 0.0
        pts = []
        for i in range(sides):
            a = off + 2 * math.pi * i / sides
            u, v = rr * rx * math.cos(a), rr * ry * math.sin(a)
            pts.append({"z": Vector((u, v, h)), "y": Vector((u, h, v)), "x": Vector((h, u, v))}[axis] + Vector(center))
        rings.append(pts)
    return loft(name, rings, material, (True, True), part)


def text(name, body, size, material, loc, rot=(0, 0, 0), extrude=0.04, font=None, spacing=1.0, align="CENTER", detail=True, part=None):
    """A Blender text object turned into a mesh (runes, letters). By default a bake-only detail: put it a hair above the surface it marks."""
    cu = bpy.data.curves.new(name, "FONT")
    cu.body = body
    cu.size = size
    cu.extrude = extrude
    cu.align_x = align
    cu.align_y = "CENTER"
    cu.space_character = spacing
    cu.resolution_u = 2
    if font:
        cu.font = bpy.data.fonts.load(font, check_existing=True)
    tmp = bpy.data.objects.new(name + "_t", cu)
    bpy.context.scene.collection.objects.link(tmp)
    bpy.context.view_layer.update()
    me = bpy.data.meshes.new_from_object(tmp.evaluated_get(bpy.context.evaluated_depsgraph_get()))
    bpy.data.objects.remove(tmp)
    ob = bpy.data.objects.new(name, me)
    bpy.context.scene.collection.objects.link(ob)
    if material is not None:
        me.materials.append(material)
    ob["part"] = part or name
    if detail:
        ob["detail"] = True
    move(ob, loc, rot)
    return ob


# ================================================================ editing
def move(ob, loc=(0, 0, 0), rot=(0, 0, 0), scale=(1, 1, 1)):
    """Moves the object's points (objects never keep a transform: the mesh file is in world space). rot in degrees, XYZ order."""
    from mathutils import Euler
    m = Matrix.Translation(Vector(loc)) @ Euler([math.radians(a) for a in rot], "XYZ").to_matrix().to_4x4() @ Matrix.Diagonal((*scale, 1.0))
    ob.data.transform(m)
    if m.determinant() < 0:
        ob.data.flip_normals()
    ob.data.update()
    return ob


def copy(ob, name=None, loc=(0, 0, 0), rot=(0, 0, 0), scale=(1, 1, 1)):
    c = bpy.data.objects.new(name or ob.name + "_c", ob.data.copy())
    bpy.context.scene.collection.objects.link(c)
    for k in ob.keys():
        c[k] = ob[k]
    return move(c, loc, rot, scale)


def mirror(ob, axis="x", name=None):
    """A mirrored copy (same part name), so a left piece gets its right twin."""
    s = {"x": (-1, 1, 1), "y": (1, -1, 1), "z": (1, 1, -1)}[axis]
    return copy(ob, name or ob.name + "_m", scale=s)


def modifier(ob, kind, **kw):
    """Adds one Blender modifier and applies it at once."""
    m = ob.modifiers.new(kind, kind)
    for k, v in kw.items():
        setattr(m, k, v)
    bpy.context.view_layer.update()
    me = bpy.data.meshes.new_from_object(ob.evaluated_get(bpy.context.evaluated_depsgraph_get()))
    ob.modifiers.clear()
    old = ob.data
    ob.data = me
    bpy.data.meshes.remove(old)
    return ob


def bevel(ob, width, segments=1, angle=35.0):
    return modifier(ob, "BEVEL", width=width, segments=segments, limit_method="ANGLE", angle_limit=math.radians(angle))


def subsurf(ob, levels=1):
    return modifier(ob, "SUBSURF", levels=levels)


def solidify(ob, thickness, offset=0.0):
    return modifier(ob, "SOLIDIFY", thickness=thickness, offset=offset)


def decimate(ob, ratio):
    return modifier(ob, "DECIMATE", ratio=ratio)


def quads_of(ob):
    return len(ob.data.polygons)


# ================================================================ finishing: join, unwrap, bake, export
def _select(obs, active):
    bpy.ops.object.select_all(action="DESELECT")
    for o in obs:
        o.select_set(True)
    bpy.context.view_layer.objects.active = active


def _join(obs, name):
    dup = []
    for o in obs:
        d = bpy.data.objects.new(o.name + "_j", o.data.copy())
        bpy.context.scene.collection.objects.link(d)
        dup.append(d)
    _select(dup, dup[0])
    if len(dup) > 1:
        bpy.ops.object.join()
    j = bpy.context.view_layer.objects.active
    j.name = name
    return j


def _tag_parts(obs, names):
    for o in obs:
        me = o.data
        a = me.attributes.get("part_id") or me.attributes.new("part_id", "INT", "FACE")
        idx = names.index(o["part"])
        a.data.foreach_set("value", [idx] * len(me.polygons))


def _clean(ob):
    """N-gons become triangles, triangle pairs become quads again where they are flat enough: the mesh file is quads (a lone triangle
    is written as a quad with a repeated corner)."""
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    ng = [f for f in bm.faces if len(f.verts) > 4]
    if ng:
        res = bmesh.ops.triangulate(bm, faces=ng)
        tris = [f for f in res["faces"] if len(f.verts) == 3]
        bmesh.ops.join_triangles(bm, faces=tris, angle_face_threshold=math.radians(25), angle_shape_threshold=math.radians(60))
    bm.to_mesh(ob.data)
    bm.free()
    ob.data.update()


def _image(name, px):
    img = bpy.data.images.new(name, px, px, alpha=True, float_buffer=False)
    img.generated_color = (0, 0, 0, 0)
    return img


def _pixels(img, px, out):
    """The baked image as float RGBA 0..255, top row first, at `out` px (box-filtered down from the bake size)."""
    a = np.empty(px * px * 4, np.float32)
    img.pixels.foreach_get(a)
    a = a.reshape(px, px, 4)[::-1]
    k = px // out
    if k > 1:
        w = a[..., 3:4]
        num = (a[..., :3] * w).reshape(out, k, out, k, 3).sum(axis=(1, 3))
        den = w.reshape(out, k, out, k, 1).sum(axis=(1, 3))
        rgb_ = np.where(den > 1e-6, num / np.maximum(den, 1e-6), 0.0)
        al = den / (k * k)
        a = np.concatenate([rgb_, al], axis=2)
    return np.clip(a, 0.0, 1.0) * 255.0


def finish(item_id, glow=("FFFFFF", 0.12, 0.5, 1.0), header_from=None, smooth=38.0, skin=512, bake=1024, out_mesh=None, out_skin=None,
           preview=None, write=True, deform=None, extra_meshes=None, budget=QUAD_BUDGET, cage=0.45):
    """Joins every object of the scene into the weapon, unwraps it, bakes its skin and glow skin with Cycles and writes the mod's files.

    header_from: a mesh file whose `place` line is reused (the weapon keeps its length and grip, so it keeps its place in the hand).
    deform: {suffix: function(Vector) -> Vector} writes extra meshes on the same skin with moved points (a bow's draw stages).
    Returns (mesh_lib Mesh, Atlas, stats)."""
    sc = bpy.context.scene
    lows = [o for o in sc.collection.objects if o.type == "MESH" and not o.get("detail")]
    details = [o for o in sc.collection.objects if o.type == "MESH" and o.get("detail")]
    names = []
    for o in lows:
        if o["part"] not in names:
            names.append(o["part"])
    _tag_parts(lows, names)
    low = _join(lows, item_id)
    _clean(low)
    low.data.shade_smooth()
    low.data.set_sharp_from_angle(angle=math.radians(smooth))
    # ---- unwrap into one skin
    _select([low], low)
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.uv.smart_project(angle_limit=math.radians(66), island_margin=0.0, area_weight=0.0, scale_to_bounds=False)
    bpy.ops.uv.average_islands_scale()
    bpy.ops.uv.pack_islands(margin=0.006, rotate=True)
    bpy.ops.object.mode_set(mode="OBJECT")
    # ---- bake
    img, gimg = _image(item_id + "_skin", bake), _image(item_id + "_glow", bake)
    mats = [s.material for s in low.material_slots if s.material]
    nodes = []
    for m in mats:
        n = m.node_tree.nodes.new("ShaderNodeTexImage")
        n.image = img
        m.node_tree.nodes.active = n
        nodes.append(n)
    for o in lows + details:                                                 # the pieces are in the joined copies now: left visible they would shadow them
        o.hide_render = True
    kw = dict(margin=6, use_clear=True)
    if details:
        high = _join(lows + details, item_id + "_high")
        _select([high, low], low)
        kw.update(use_selected_to_active=True, cage_extrusion=cage, max_ray_distance=cage * 3)
    else:
        _select([low], low)
    bpy.ops.object.bake(type="COMBINED", **kw)
    for n in nodes:
        n.image = gimg
    bpy.ops.object.bake(type="EMIT", **kw)
    atlas = ml.Atlas(skin)
    atlas.img = _pixels(img, bake, skin)
    atlas.img[..., 3] = np.where(atlas.img[..., 3] > 8, 255.0, 0.0)
    g = _pixels(gimg, bake, skin)
    lum = g[..., :3].max(axis=2)
    alpha = np.clip(lum * 1.25, 0, 255) * (g[..., 3] > 8)
    atlas.glow = np.concatenate([g[..., :3] * (alpha[..., None] > 2), alpha[..., None]], axis=2).astype(np.float32)
    # ---- to the mod's mesh
    def build(fn=None):
        mesh = ml.Mesh(item_id)
        parts = [ml.Part(n, smooth=smooth) for n in names]
        me = low.data
        uv = me.uv_layers.active.data
        pid = me.attributes["part_id"].data
        cn = me.corner_normals
        for poly in me.polygons:
            p = parts[pid[poly.index].value]
            li = list(poly.loop_indices)
            if len(li) == 3:
                li.append(li[2])
            idx, uvs, nrm = [], [], []
            for l in li:
                v = me.vertices[me.loops[l].vertex_index].co
                v = fn(Vector(v)) if fn else v
                idx.append(len(p.V))
                p.V.append(np.array((v.x, v.y, v.z), float))
                uvs.append((uv[l].uv.x * ml.TS, (1.0 - uv[l].uv.y) * ml.TS))
                nv = cn[l].vector
                nrm.append(np.array((nv.x, nv.y, nv.z), float))
            p.F.append(tuple(idx))
            p.tags.append(None)
            p.UV = (p.UV or []) + [uvs]
            p.N = (p.N or []) + [nrm]
        for p in parts:
            if p.F:
                mesh.add(p)
        mesh.finish = lambda: None                                           # Blender already turned the faces outward and made the normals
        mesh.set_glow(*glow)
        mesh.orient_cfg = "manual"
        return mesh

    mesh = build()
    if header_from:
        with open(header_from) as fh:
            for line in fh:
                if line.startswith("place "):
                    mesh.place(*[float(x) for x in line.split()[1:6]])
                    break
    stats = mesh.stats(atlas)
    lines = sum(stats["quads"].values())
    stats["lines"] = lines
    print(f"{item_id}: {len(mesh.parts)} parts, {lines} quad lines {stats['quads']}, box {stats['bbox_min']} .. {stats['bbox_max']}")
    for p in mesh.parts:
        print(f"   {p.name:16s} {len(p.F)}")
    if lines > budget:
        raise SystemExit(f"{item_id}: {lines} quad lines, over the budget of {budget}")
    if preview:
        os.makedirs(os.path.dirname(os.path.abspath(preview)), exist_ok=True)
        ml.preview(mesh, None, preview, atlas=atlas, normals="vertex")
    if write:
        comment = "made in Blender by tools/blender (bl_lib.py)"
        mesh.write_txt(os.path.join(out_mesh or MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment=comment)
        for suffix, fn in (deform or {}).items():
            m2 = build(fn)
            m2.place_cfg = mesh.place_cfg
            m2.write_txt(os.path.join(out_mesh or MESH_DIR, item_id + suffix + ".txt"), atlas, normals="vertex", comment=comment + " (" + suffix + ")")
        from PIL import Image
        d = out_skin or SKIN_DIR
        os.makedirs(d, exist_ok=True)
        Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, item_id + ".png"), optimize=True)
        Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, item_id + "_glow.png"), optimize=True)
    return mesh, atlas, stats

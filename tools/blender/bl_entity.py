"""Blender entity pipeline (boss rework wave 2): a boss body MADE IN BLENDER, split into parts that the game turns about their pivots.

It builds on bl_lib.py (the hero-weapon pipeline: materials, modelling helpers, join, unwrap, Cycles bake) and only adds what an entity needs:
  * the entity mesh file (assets/anime_multiverse/entity_meshes/<id>.txt, read by mesh/EntityMeshData.java):
        # comment
        entity <id> <scale: model units to blocks>
        glow <RRGGBB> <speed> <min> <max>
        pivot <part> <parent or -> <x> <y> <z>        one per part, parents first
        part <part>                                     then the part's quads:
        qn <layer> (x y z u v nx ny nz) x4              layer 0 = body, 2 = glow copy (the same quad, drawn additive with the glow skin)
    the quads are those of the weapon files (u, v in 128 units), so the same packer and mesh tools read them;
  * a low-detail copy (<id>_lod.txt, Blender's Decimate modifier on the unwrapped body: same skin, same parts);
  * the skins in textures/entity/boss_mesh/<id>.png and <id>_glow.png;
  * preview renders in Blender (Cycles) of the baked model, at rest and posed with the same part turns the game uses (pose = {part: (rx, ry, rz) radians},
    turned about the pivot, parents first, rotation order X then Y then Z, as client/bossmesh/BossMeshDraw does).

Model space: 1 unit = 1/16 block (a "pixel"), y up, the face toward +z, the feet on y = 0; the boss's left is +x.
"""
import math
import os
import random

import bpy  # noqa: I001
import numpy as np
from mathutils import Euler, Matrix, Vector

import bl_lib as bl

ENTITY_MESH_DIR = os.path.join(bl.ASSETS, "entity_meshes")
ENTITY_SKIN_DIR = os.path.join(bl.ASSETS, "textures", "entity", "boss_mesh")
SCALE = 1.0 / 16.0
BUDGET = 2800          # quad lines per mesh, body + glow copies (the runtime refuses more than 3000: EntityMeshData.MAX_QUADS)


def part_names():
    """The part order bl_lib.finish uses for its part_id attribute (first appearance among the exported objects)."""
    names = []
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail") and o["part"] not in names:
            names.append(o["part"])
    return names


def _quads(ob, names, atlas, fn=None):
    """{part: [(layer, [(x y z u v nx ny nz) x4])]} of a joined, unwrapped object (a triangle repeats its last corner)."""
    me = ob.data
    uv = me.uv_layers.active.data
    pid = me.attributes["part_id"].data
    cn = me.corner_normals
    out = {n: [] for n in names}
    for poly in me.polygons:
        li = list(poly.loop_indices)
        if len(li) > 4:
            continue                                                          # bl_lib._clean leaves none; a decimated n-gon is skipped
        if len(li) == 3:
            li.append(li[2])
        vs = []
        for lo in li:
            co = Vector(me.vertices[me.loops[lo].vertex_index].co)
            nv = Vector(cn[lo].vector)
            if fn:
                co, nv = fn(pid[poly.index].value, co, nv)
            vs.append((co.x, co.y, co.z, uv[lo].uv.x * 128.0, (1.0 - uv[lo].uv.y) * 128.0, nv.x, nv.y, nv.z))
        name = names[pid[poly.index].value]
        out[name].append((0, vs))
        if atlas is not None and atlas.has_glow(np.array([(v[3], v[4]) for v in vs])):
            out[name].append((2, vs))
    return out


def write_entity(path, entity_id, names, rig, quads, glow, comment):
    lines = [f"# {comment} ({entity_id}); {len(names)} parts",
             f"entity {entity_id} {SCALE:.6f}",
             "glow %s %s %s %s" % glow]
    for n in names:
        parent, p = rig[n]
        lines.append("pivot %s %s %.3f %.3f %.3f" % (n, parent or "-", p[0], p[1], p[2]))
    count = 0
    for n in names:
        lines.append("part " + n)
        for layer, vs in quads[n]:
            lines.append("qn %d %s" % (layer, " ".join("%.3f %.3f %.3f %.3f %.3f %.4f %.4f %.4f" % v for v in vs)))
            count += 1
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return count


def _check_rig(names, rig):
    for i, n in enumerate(names):
        if n not in rig:
            raise SystemExit(f"part {n} has no pivot")
        parent = rig[n][0]
        if parent is not None and (parent not in names or names.index(parent) >= i):
            raise SystemExit(f"part {n}: its parent {parent} must be a part listed before it")


def finish_entity(entity_id, rig, glow=("FF6A2A", 0.1, 0.55, 1.0), skin=1024, bake=2048, lod_ratio=0.3, out_mesh=None, out_skin=None,
                  previews=None, poses=None, budget=BUDGET, cage=0.45, smooth=50.0):
    """Joins, unwraps and bakes the scene (bl_lib.finish), then writes the entity mesh, its LOD copy and the skins, and renders the previews.
    rig = {part: (parent part or None, pivot (x, y, z))}; previews = {png path: (camera yaw degrees, pose name or None)}; poses = {name: {part: (rx, ry, rz)}}."""
    names = part_names()
    _check_rig(names, rig)
    _mesh, atlas, stats = bl.finish(entity_id, glow=glow, skin=skin, bake=bake, write=False, budget=budget, cage=cage, smooth=smooth)
    low = bpy.data.objects[entity_id]
    comment = "made in Blender by tools/blender (bl_entity.py)"
    md, sd = out_mesh or ENTITY_MESH_DIR, out_skin or ENTITY_SKIN_DIR
    n_full = write_entity(os.path.join(md, entity_id + ".txt"), entity_id, names, rig, _quads(low, names, atlas), glow, comment)
    lod = _decimated(low, lod_ratio)
    lod_q = _quads(lod, names, None)                                          # the far copy has no glow pass
    n_lod = write_entity(os.path.join(md, entity_id + "_lod.txt"), entity_id, names, rig, lod_q, glow, comment + ", Decimate %.2f" % lod_ratio)
    bpy.data.objects.remove(lod)
    from PIL import Image
    os.makedirs(sd, exist_ok=True)
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(sd, entity_id + ".png"), optimize=True)
    Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(sd, entity_id + "_glow.png"), optimize=True)
    print(f"{entity_id}: {n_full} quad lines (LOD {n_lod}), parts {names}")
    if n_full > budget:
        raise SystemExit(f"{entity_id}: {n_full} quad lines, over the budget of {budget}")
    if previews:
        render_previews(low, names, rig, atlas, previews, poses or {})
    return n_full, n_lod, stats


def _decimated(low, ratio):
    d = bpy.data.objects.new(low.name + "_lod", low.data.copy())
    bpy.context.scene.collection.objects.link(d)
    bl.modifier(d, "DECIMATE", ratio=ratio, use_collapse_triangulate=False)
    bm_fix = d.data
    bm_fix.shade_smooth()
    bm_fix.set_sharp_from_angle(angle=math.radians(50))
    return d


# ================================================================ previews (rendered in Blender)
def _np_image(name, arr):
    h, w = arr.shape[:2]
    img = bpy.data.images.new(name, w, h, alpha=True)
    img.pixels.foreach_set((arr[::-1] / 255.0).astype(np.float32).ravel())
    img.pack()
    return img


def _preview_material(atlas):
    m = bpy.data.materials.new("preview_skin")
    m.use_nodes = True
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    t = N.new("ShaderNodeTexImage")
    t.image = _np_image("pv_skin", atlas.img)
    t.interpolation = "Closest"
    g = N.new("ShaderNodeTexImage")
    g.image = _np_image("pv_glow", atlas.glow)
    L.new(t.outputs["Color"], bs.inputs["Base Color"])
    bs.inputs["Roughness"].default_value = 0.9
    L.new(g.outputs["Color"], bs.inputs["Emission Color"])
    bs.inputs["Emission Strength"].default_value = 2.5
    return m


def pose_matrices(names, rig, pose):
    """{part index: world matrix} of a pose: T(pivot) R(X, then Y, then Z) T(-pivot), the parent's first (the game does the same)."""
    out = []
    for n in names:
        parent, p = rig[n]
        rx, ry, rz = pose.get(n, (0.0, 0.0, 0.0))
        local = Matrix.Translation(Vector(p)) @ Euler((rx, ry, rz), "XYZ").to_matrix().to_4x4() @ Matrix.Translation(-Vector(p))
        out.append((out[names.index(parent)] if parent else Matrix.Identity(4)) @ local)
    return out


def render_previews(low, names, rig, atlas, previews, poses, res=(420, 560), samples=20):
    sc = bpy.context.scene
    for o in sc.collection.objects:
        o.hide_render = True
    mat = _preview_material(atlas)
    w = bpy.data.worlds.new("preview")
    w.use_nodes = True
    w.node_tree.nodes["Background"].inputs["Color"].default_value = (0.11, 0.12, 0.15, 1)
    w.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.6
    sc.world = w
    sun = bpy.data.objects.new("sun", bpy.data.lights.new("sun", "SUN"))
    sun.data.energy = 3.2
    sc.collection.objects.link(sun)
    cam = bpy.data.objects.new("cam", bpy.data.cameras.new("cam"))
    cam.data.lens = 50
    sc.collection.objects.link(cam)
    sc.camera = cam
    sc.render.resolution_x, sc.render.resolution_y = res
    sc.render.film_transparent = False
    sc.cycles.samples = samples
    sc.view_settings.view_transform = "Standard"
    sc.render.image_settings.file_format = "PNG"
    sc.render.image_settings.color_mode = "RGB"
    for path, (yaw, pose_name) in previews.items():
        me = low.data.copy()
        me.materials.clear()
        me.materials.append(mat)
        ob = bpy.data.objects.new("pv", me)
        sc.collection.objects.link(ob)
        if pose_name:
            mats = pose_matrices(names, rig, poses[pose_name])
            pid = me.attributes["part_id"].data
            owner = {}
            for poly in me.polygons:
                for vi in poly.vertices:
                    owner[vi] = pid[poly.index].value
            for v in me.vertices:
                v.co = mats[owner.get(v.index, 0)] @ v.co
            me.update()
        lo, hi = Vector([min(v.co[i] for v in me.vertices) for i in range(3)]), Vector([max(v.co[i] for v in me.vertices) for i in range(3)])
        mid, size = (lo + hi) / 2, max(hi - lo)
        # the preview is y-up, front +z: turn it into Blender's z-up world for the camera
        ob.matrix_world = Matrix.Rotation(math.radians(90), 4, "X")
        a = math.radians(yaw)
        c = Vector((mid.x, -mid.z, mid.y))
        dist = size * 2.45
        cam.location = c + Vector((math.sin(a) * dist, -math.cos(a) * dist, size * 0.18))
        cam.rotation_euler = (c - cam.location).to_track_quat("-Z", "Y").to_euler()
        sun.rotation_euler = Euler((math.radians(50), 0, a + math.radians(35)), "XYZ")
        sc.render.filepath = os.path.abspath(path)
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        bpy.ops.render.render(write_still=True)
        bpy.data.objects.remove(ob)
        bpy.data.meshes.remove(me)
        _shrink(path)
        print("preview", path, os.path.getsize(path) // 1024, "KB")


def _shrink(path):
    from PIL import Image
    im = Image.open(path).convert("RGB")
    im.quantize(colors=200, method=Image.Quantize.MEDIANCUT).save(path, optimize=True)


# ================================================================ modelling helpers (rocky / organic body pieces)
RNG = random.Random(17)


def seed(n):
    """Every jagged point comes from this one generator: seed it at the start of a build so a model is the same on every run."""
    RNG.seed(n)


def rock_section(n, w, d, jag=0.12, square=0.35):
    """An irregular, slightly squared ring of n points (half width w, half depth d)."""
    pts = []
    for k in range(n):
        a = 2 * math.pi * k / n
        c, s = math.cos(a), math.sin(a)
        sq = 1.0 + square * (abs(c * s) * 2) ** 2                             # push the diagonals out: a blocky, hewn look
        r = sq * (1.0 + RNG.uniform(-jag, jag))
        pts.append((w * c * r, d * s * r))
    return pts


def body_loft(name, part, rows, n, mat, jag=0.1, caps=(True, True)):
    """rows = [(y, half width, half depth, x offset, z offset)]: rings up a body piece (y up)."""
    rings = [[Vector((ox + u, y, oz + v)) for u, v in rock_section(n, w, d, jag)] for y, w, d, ox, oz in rows]
    return bl.loft(name, rings, mat, caps, part=part)


def limb(name, part, path, radii, n, mat, flat=1.0, jag=0.1):
    """A thick limb along a path of points with a radius per point (rocky ring sections)."""
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
        rings.append([p + side * u + fwd * v for u, v in rock_section(n, r, r * flat, jag)])
    return bl.loft(name, rings, mat, (True, True), part=part)


def boulder(name, part, center, r, mat, squash=(1.0, 1.0, 1.0), seg=(8, 6), jag=0.16):
    """A lumpy rock (a low UV sphere with every point pushed in or out)."""
    verts, faces = [], []
    su, sv = seg
    verts.append(Vector((0, -1, 0)))
    for j in range(1, sv):
        b = -math.pi / 2 + math.pi * j / sv
        for i in range(su):
            a = 2 * math.pi * i / su
            verts.append(Vector((math.cos(b) * math.cos(a), math.sin(b), math.cos(b) * math.sin(a))))
    verts.append(Vector((0, 1, 0)))
    top = len(verts) - 1
    for i in range(su):
        faces.append((0, 1 + (i + 1) % su, 1 + i))
    for j in range(sv - 2):
        for i in range(su):
            a, b = 1 + j * su + i, 1 + j * su + (i + 1) % su
            faces.append((a, b, b + su, a + su))
    o = 1 + (sv - 2) * su
    for i in range(su):
        faces.append((o + i, o + (i + 1) % su, top))
    pts = [Vector(center) + Vector((v.x * r * squash[0], v.y * r * squash[1], v.z * r * squash[2])) * (1 + RNG.uniform(-jag, jag)) for v in verts]
    return bl.obj(name, pts, faces, mat, part=part)


def shard(name, part, base, tip, r, mat, n=5):
    """A rock spike from base to tip."""
    b, t = Vector(base), Vector(tip)
    ax = (t - b).normalized()
    side = Vector((0, 1, 0)).cross(ax)
    if side.length < 1e-6:
        side = Vector((1, 0, 0))
    side.normalize()
    up = ax.cross(side)
    ring = [b + (side * math.cos(2 * math.pi * k / n) + up * math.sin(2 * math.pi * k / n)) * r * (1 + RNG.uniform(-0.2, 0.2)) for k in range(n)]
    verts = ring + [t]
    faces = [(k, (k + 1) % n, n) for k in range(n)] + [tuple(range(n - 1, -1, -1))]
    return bl.obj(name, verts, faces, mat, part=part)


def rough(ob, strength=1.1, scale=7.0):
    """Blender's Displace modifier with a Clouds texture: the stone's uneven, knobbly surface."""
    tex = bpy.data.textures.get("entity_rock") or bpy.data.textures.new("entity_rock", "CLOUDS")
    tex.noise_scale = scale
    tex.noise_depth = 2
    return bl.modifier(ob, "DISPLACE", texture=tex, strength=strength, mid_level=0.5, texture_coords="GLOBAL")


def side_copy(ob, part):
    c = bl.mirror(ob, "x", ob.name.replace("_l", "_r"))
    c["part"] = part
    return c

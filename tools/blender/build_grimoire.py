"""
Black Clover grimoire builder for Blender (4.2 LTS - 5.x).

Builds a stylized, anime-shaded grimoire floating open (spine towards the camera, covers angled back, the way most grimoires are
shown in the anime): beveled boards with a raised double border frame, fleur-de-lis style corner curls, a central medallion with
the clover (3, 4 or 5 leaves), a banded spine, glowing page blocks, a soft aura shell in the magic's colour, inverted-hull
outlines, cel (toon) materials and a bloom compositor setup.

Use it two ways:
  * In Blender: Scripting workspace > Text > Open this file > Run Script (builds the default preset into the current file;
    it clears the scene first). Change PRESET below.
  * Headless:   blender --background --python build_grimoire.py -- --preset yuno --render yuno.png --save yuno.blend
                options: --preset NAME  --render PNG  --save BLEND  --res 1280x960  --samples 32  --open 34 (degrees per cover)

Everything is generated (no image textures), so the result is a clean base to sculpt, UV and hand-paint on top of.
Presets follow the reference art (Black Clover wiki grimoire tables): Fuegoleon (red + gold, fire), Yuno (dark olive + gold,
four-leaf, wind), Asta (black, filthy, five-leaf, red-black anti-magic aura), Noelle (pale blue + silver, water), Yami (purple,
dark magic), Julius (cream + gold, time).
"""
import math
import sys

import bpy  # first: bmesh and mathutils come with it (also when run as the pip 'bpy' module)
import bmesh
from mathutils import Matrix, Vector

PRESET = "fuegoleon"

PRESETS = {
    #           cover base         cover shadow        trim (metal)        aura / magic      leaves  tattered  page glow
    "fuegoleon": dict(cover=(0.55, 0.06, 0.05), shade=(0.25, 0.02, 0.03), trim=(1.00, 0.76, 0.28), aura=(1.00, 0.42, 0.10), leaves=3, tattered=False, pages=2.0),
    "yuno":      dict(cover=(0.16, 0.20, 0.08), shade=(0.06, 0.08, 0.03), trim=(0.95, 0.78, 0.35), aura=(0.30, 1.00, 0.55), leaves=4, tattered=False, pages=2.0),
    "asta":      dict(cover=(0.05, 0.045, 0.04), shade=(0.012, 0.01, 0.01), trim=(0.14, 0.12, 0.11), aura=(0.90, 0.05, 0.08), leaves=5, tattered=True, pages=0.6),
    "noelle":    dict(cover=(0.62, 0.78, 0.92), shade=(0.30, 0.42, 0.62), trim=(0.88, 0.90, 0.96), aura=(0.35, 0.80, 1.00), leaves=3, tattered=False, pages=2.5),
    "yami":      dict(cover=(0.30, 0.06, 0.34), shade=(0.11, 0.02, 0.14), trim=(0.85, 0.70, 0.40), aura=(0.75, 0.20, 1.00), leaves=3, tattered=False, pages=1.6),
    "julius":    dict(cover=(0.86, 0.80, 0.64), shade=(0.55, 0.47, 0.33), trim=(1.00, 0.82, 0.40), aura=(0.70, 0.75, 1.00), leaves=4, tattered=False, pages=3.0),
}

# Book proportions (Blender units, 1 = one book width). Grimoires in the art are close to square and thick-boarded.
W, H = 1.0, 1.22          # cover width (spine to fore-edge) and height
BOARD = 0.065             # board thickness
PAGES = 0.10              # page block thickness per half
SPINE_R = 0.085           # spine radius
OUTLINE = 0.010           # inverted-hull outline thickness


# ------------------------------------------------------------------------------------------------ small helpers
def clear_scene():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob, do_unlink=True)
    for coll in (bpy.data.meshes, bpy.data.curves, bpy.data.materials, bpy.data.lights, bpy.data.cameras, bpy.data.textures):
        for block in list(coll):
            if block.users == 0:
                coll.remove(block)


def link(ob, parent=None):
    bpy.context.scene.collection.objects.link(ob)
    if parent is not None:
        ob.parent = parent
    return ob


def mesh_object(name, bm, parent=None, smooth=False):
    me = bpy.data.meshes.new(name)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    bm.to_mesh(me)
    bm.free()
    if smooth:
        for p in me.polygons:
            p.use_smooth = True
    return link(bpy.data.objects.new(name, me), parent)


def box(name, x0, x1, y0, y1, z0, z1, parent=None, bevel=0.0, segments=2):
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    for v in bm.verts:
        v.co = Vector(((v.co.x + 0.5) * (x1 - x0) + x0, (v.co.y + 0.5) * (y1 - y0) + y0, (v.co.z + 0.5) * (z1 - z0) + z0))
    ob = mesh_object(name, bm, parent)
    if bevel > 0:
        m = ob.modifiers.new("Bevel", "BEVEL")
        m.width = bevel
        m.segments = segments
        m.limit_method = "ANGLE"
    return ob


def disc(name, center, radius, depth, parent=None, segments=48):
    """Flat cylinder whose faces point along -Y/+Y (lying on a cover)."""
    bm = bmesh.new()
    bmesh.ops.create_cone(bm, cap_ends=True, cap_tris=False, segments=segments, radius1=radius, radius2=radius, depth=depth)
    bmesh.ops.rotate(bm, verts=bm.verts, cent=(0, 0, 0), matrix=Matrix.Rotation(math.radians(90), 3, "X"))
    bmesh.ops.translate(bm, verts=bm.verts, vec=Vector(center))
    ob = mesh_object(name, bm, parent)
    m = ob.modifiers.new("Bevel", "BEVEL")
    m.width = depth * 0.35
    m.segments = 2
    m.limit_method = "ANGLE"
    return ob


def curve_to_mesh(name, curve_ob, parent=None):
    """Evaluates a curve object (bevel / fill) into a real mesh so it can take modifiers (outline)."""
    deps = bpy.context.evaluated_depsgraph_get()
    me = bpy.data.meshes.new_from_object(curve_ob.evaluated_get(deps))
    me.name = name
    # the mesh comes out in the curve's local space: bake its placement in (it is built in hinge space, unparented)
    me.transform(Matrix.Translation(curve_ob.location) @ curve_ob.rotation_euler.to_matrix().to_4x4())
    data = curve_ob.data
    bpy.data.objects.remove(curve_ob, do_unlink=True)
    bpy.data.curves.remove(data)
    return link(bpy.data.objects.new(name, me), parent)


def poly_curve(name, points, bevel=0.0, fill_2d=False, extrude=0.0, closed=False):
    cu = bpy.data.curves.new(name, "CURVE")
    cu.dimensions = "2D" if fill_2d else "3D"
    if fill_2d:
        cu.fill_mode = "BOTH"
        cu.extrude = extrude
    cu.bevel_depth = bevel
    cu.bevel_resolution = 2
    sp = cu.splines.new("POLY")
    sp.points.add(len(points) - 1)
    for p, co in zip(sp.points, points):
        p.co = (co[0], co[1], co[2] if len(co) > 2 else 0.0, 1.0)
    sp.use_cyclic_u = closed
    ob = bpy.data.objects.new(name, cu)
    bpy.context.scene.collection.objects.link(ob)
    return ob


def on_cover(points2d, sign, y):
    """Maps 2D cover-space points (u across from the spine, v up) to the right/left board's outer face."""
    return [(sign * u, y, v) for u, v in points2d]


# ------------------------------------------------------------------------------------------------ materials (EEVEE NPR)
def _sock(sockets, identifier):
    for s in sockets:
        if s.identifier == identifier:
            return s
    return sockets[identifier]


def _ramp(nodes, stops, interpolation="CONSTANT"):
    r = nodes.new("ShaderNodeValToRGB")
    r.color_ramp.interpolation = interpolation
    els = r.color_ramp.elements
    while len(els) < len(stops):
        els.new(0.5)
    for el, (pos, col) in zip(els, stops):
        el.position = pos
        el.color = (*col, 1.0) if len(col) == 3 else col
    return r


def _mix(nodes, links, a, b, blend="ADD", factor=1.0):
    m = nodes.new("ShaderNodeMix")
    m.data_type = "RGBA"
    m.blend_type = blend
    m.inputs[0].default_value = factor
    links.new(a, _sock(m.inputs, "A_Color"))
    links.new(b, _sock(m.inputs, "B_Color"))
    return _sock(m.outputs, "Result_Color")


def toon_material(name, base, shade, light=None, rim=None, rim_width=0.55, strength=1.0, grime=0.0, page_lines=False):
    """
    Cel shader: Diffuse -> Shader to RGB -> constant Color Ramp (shadow / base / highlight bands) + Layer Weight rim -> Emission.
    Emission output keeps the colours flat and exact (anime look); the Diffuse BSDF only measures the light.
    """
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    diffuse = nodes.new("ShaderNodeBsdfDiffuse")
    to_rgb = nodes.new("ShaderNodeShaderToRGB")
    links.new(diffuse.outputs["BSDF"], to_rgb.inputs["Shader"])
    light = light or tuple(min(1.0, c * 1.25 + 0.05) for c in base)
    bands = _ramp(nodes, [(0.0, shade), (0.32, base), (0.86, light)])
    links.new(to_rgb.outputs["Color"], bands.inputs["Fac"])
    color = bands.outputs["Color"]
    if page_lines:   # thin dark lines along the page edges (object-space wave bands)
        coord = nodes.new("ShaderNodeTexCoord")
        wave = nodes.new("ShaderNodeTexWave")
        wave.bands_direction = "Y"
        wave.inputs["Scale"].default_value = 60.0
        wave.inputs["Distortion"].default_value = 1.5
        links.new(coord.outputs["Object"], wave.inputs["Vector"])
        lines = _ramp(nodes, [(0.0, (0.78, 0.74, 0.66)), (0.55, (1.0, 1.0, 1.0))])
        links.new(wave.outputs["Fac"], lines.inputs["Fac"])
        color = _mix(nodes, links, color, lines.outputs["Color"], "MULTIPLY")
    if grime > 0:    # filthy / tattered patches (Asta)
        noise = nodes.new("ShaderNodeTexNoise")
        noise.inputs["Scale"].default_value = 9.0
        noise.inputs["Detail"].default_value = 6.0
        dirt = _ramp(nodes, [(0.0, (1, 1, 1)), (0.52, (1, 1, 1)), (0.56, (0.45, 0.40, 0.36))])
        links.new(noise.outputs["Fac"], dirt.inputs["Fac"])
        color = _mix(nodes, links, color, dirt.outputs["Color"], "MULTIPLY", grime)
    if rim:
        lw = nodes.new("ShaderNodeLayerWeight")
        lw.inputs["Blend"].default_value = 0.35
        rim_ramp = _ramp(nodes, [(0.0, (0, 0, 0)), (rim_width, rim)])
        links.new(lw.outputs["Facing"], rim_ramp.inputs["Fac"])
        color = _mix(nodes, links, color, rim_ramp.outputs["Color"], "ADD")
    emit = nodes.new("ShaderNodeEmission")
    emit.inputs["Strength"].default_value = strength
    links.new(color, emit.inputs["Color"])
    links.new(emit.outputs["Emission"], out.inputs["Surface"])
    return mat


def outline_material(color):
    mat = bpy.data.materials.new("Outline")
    mat.use_nodes = True
    mat.use_backface_culling = True
    nodes = mat.node_tree.nodes
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    emit = nodes.new("ShaderNodeEmission")
    emit.inputs["Color"].default_value = (*color, 1.0)
    mat.node_tree.links.new(emit.outputs["Emission"], out.inputs["Surface"])
    return mat


def aura_material(color, strength=2.2):
    """Soft halo behind the book: a radial gradient that fades to fully transparent, so there is no hard edge."""
    mat = bpy.data.materials.new("Aura")
    mat.use_nodes = True
    if hasattr(mat, "surface_render_method"):
        mat.surface_render_method = "BLENDED"
    else:
        mat.blend_method = "BLEND"
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links
    nodes.clear()
    out = nodes.new("ShaderNodeOutputMaterial")
    coord = nodes.new("ShaderNodeTexCoord")
    grad = nodes.new("ShaderNodeTexGradient")
    grad.gradient_type = "SPHERICAL"                 # 1 at the object origin, 0 at distance 1
    links.new(coord.outputs["Object"], grad.inputs["Vector"])
    fade = _ramp(nodes, [(0.0, (0, 0, 0)), (0.75, (0.9, 0.9, 0.9))], "EASE")
    links.new(grad.outputs["Fac"], fade.inputs["Fac"])
    emit = nodes.new("ShaderNodeEmission")
    emit.inputs["Color"].default_value = (*color, 1.0)
    emit.inputs["Strength"].default_value = strength
    clear = nodes.new("ShaderNodeBsdfTransparent")
    mix = nodes.new("ShaderNodeMixShader")
    links.new(fade.outputs["Color"], mix.inputs["Fac"])
    links.new(clear.outputs["BSDF"], mix.inputs[1])
    links.new(emit.outputs["Emission"], mix.inputs[2])
    links.new(mix.outputs["Shader"], out.inputs["Surface"])
    return mat


def give(ob, mat, outline=None):
    ob.data.materials.clear()
    ob.data.materials.append(mat)
    if outline is not None:
        ob.data.materials.append(outline)
        m = ob.modifiers.new("Outline", "SOLIDIFY")
        m.thickness = OUTLINE
        m.offset = 1.0
        m.use_flip_normals = True
        m.use_rim = False
        m.material_offset = 1


# ------------------------------------------------------------------------------------------------ ornaments
def heart_leaf(scale, rot, center, sign, y, n=40):
    """One clover leaf: a heart with its tip at the medallion centre, pointing out at angle rot (radians, 0 = up)."""
    pts = []
    for k in range(n):
        t = 2 * math.pi * k / n
        x = 16 * math.sin(t) ** 3
        v = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        x, v = x / 17.0 * scale * 0.62, ((v + 17) / 30.0 + 0.10) * scale   # tip just off the centre, lobes outward
        c, s = math.cos(rot), math.sin(rot)
        pts.append((center[0] + x * c - v * s, center[1] + x * s + v * c))
    return on_cover(pts, sign, y)


def clover(name, leaves, center, size, sign, y, parent, mat, outline):
    """3 leaves = common (two sides + top), 4 = four-leaf, 5 = five-leaf (devil); with a stem for the three-leaf."""
    parts = []
    if leaves == 3:
        angles = [0.0, math.radians(115), math.radians(-115)]     # shamrock: top leaf, two side leaves, stem below
    else:
        angles = [2 * math.pi * k / leaves for k in range(leaves)]
    for k, a in enumerate(angles):
        pts = heart_leaf(size, a, center, sign, y)
        cu = poly_curve(f"{name}_leaf{k}", [(p[0], p[2]) for p in pts], fill_2d=True, extrude=0.008, closed=True)
        parts.append(cu)
    if leaves == 3:
        stem = [(center[0] + 0.25 * size * t * t, center[1] - t * size * 1.25) for t in [i / 10 for i in range(11)]]
        parts.append(poly_curve(f"{name}_stem", [(sign * u, v) for u, v in stem], bevel=size * 0.07))
    obs = []
    for cu in parts:
        if cu.data.dimensions == "2D":
            cu.rotation_euler = (math.radians(90), 0, 0)        # 2D curves live in XY; stand them on the cover (XZ)
            cu.location = (0, y, 0)
        else:
            for p in cu.data.splines[0].points:
                p.co = (p.co[0], y, p.co[1], 1.0)
        ob = curve_to_mesh(cu.name, cu, parent)
        give(ob, mat, outline)
        obs.append(ob)
    return obs


def corner_curl(name, corner, sign, y, size, flip_u, flip_v, parent, mat, outline):
    """Fleur-de-lis style corner ornament: two mirrored spiral curls meeting in the corner."""
    for k, (au, av) in enumerate(((1, 0), (0, 1))):
        pts = []
        for i in range(36):
            t = i / 35
            ang = t * 4.2
            r = size * (1 - t * 0.85)
            a = (r * math.cos(ang), r * math.sin(ang))
            u = a[0] if au else a[1]
            v = a[1] if au else a[0]
            pts.append((corner[0] + flip_u * (size - u) * 0.9, corner[1] + flip_v * (size - v) * 0.9))
        cu = poly_curve(f"{name}_{k}", [(sign * u, y, v) for u, v in pts], bevel=size * 0.07)
        ob = curve_to_mesh(cu.name, cu, parent)
        give(ob, mat, outline)


# ------------------------------------------------------------------------------------------------ the book
def build_half(sign, front, p, mats, root, open_deg):
    """One board with its page block and ornaments, hinged at the spine. sign +1 = right (front cover), -1 = left (back)."""
    hinge = bpy.data.objects.new("Hinge_" + ("Front" if front else "Back"), None)
    link(hinge, root)
    hinge.rotation_euler = (0, 0, math.radians(open_deg) * sign)

    x0, x1 = (0.03, W) if sign > 0 else (-W, -0.03)
    board = box("Board_" + hinge.name[6:], x0, x1, -BOARD, 0.0, -H / 2, H / 2, hinge, bevel=0.018, segments=3)
    if p["tattered"]:
        sub = board.modifiers.new("Subdiv", "SUBSURF")
        sub.subdivision_type = "SIMPLE"
        sub.levels = sub.render_levels = 4
        tex = bpy.data.textures.new("Tatter", "CLOUDS")
        tex.noise_scale = 0.12
        disp = board.modifiers.new("Tatter", "DISPLACE")
        disp.texture = tex
        disp.strength = 0.025
        board.modifiers.move(board.modifiers.find("Bevel"), 0)
    give(board, mats["cover"], mats["outline"])

    px0, px1 = (0.05, W - 0.035) if sign > 0 else (-W + 0.035, -0.05)
    pages = box("Pages_" + hinge.name[6:], px0, px1, 0.0, PAGES, -H / 2 + 0.03, H / 2 - 0.03, hinge, bevel=0.01)
    give(pages, mats["pages"], mats["outline"])

    # raised double border frame on the outer face
    y = -BOARD
    m_out, m_in, bar = 0.07, 0.13, 0.026
    for k, inset in enumerate((m_out, m_in)):
        w = bar if k == 0 else bar * 0.55
        u0, u1 = 0.03 + inset, W - inset
        v0, v1 = -H / 2 + inset, H / 2 - inset
        parts = [(u0, u1, v1 - w, v1), (u0, u1, v0, v0 + w), (u0, u0 + w, v0, v1), (u1 - w, u1, v0, v1)]
        for j, (a, b, c, d) in enumerate(parts):
            xa, xb = sorted((sign * a, sign * b))
            fr = box(f"Frame{k}{j}_{hinge.name[6:]}", xa, xb, y - 0.022 + k * 0.008, y + 0.002, c, d, hinge, bevel=0.006)
            give(fr, mats["trim"], mats["outline"])

    # corner curls inside the inner frame
    cs = 0.12
    for fu, fv, (cu_, cv) in ((1, 1, (0.03 + m_in + 0.02, -H / 2 + m_in + 0.02)), (-1, 1, (W - m_in - 0.02, -H / 2 + m_in + 0.02)),
                              (1, -1, (0.03 + m_in + 0.02, H / 2 - m_in - 0.02)), (-1, -1, (W - m_in - 0.02, H / 2 - m_in - 0.02))):
        corner_curl(f"Curl{fu}{fv}_{hinge.name[6:]}", (cu_, cv), sign, y - 0.012, cs, fu, fv, hinge, mats["trim"], mats["outline"])

    # medallion: outer ring + inner disc, and the clover on the front cover (a rosette on the back)
    cu_c, cv_c = (0.03 + W) / 2, 0.0
    ring = disc(f"MedallionRing_{hinge.name[6:]}", (sign * cu_c, y - 0.012, cv_c), 0.25, 0.024, hinge)
    give(ring, mats["trim"], mats["outline"])
    inner = disc(f"Medallion_{hinge.name[6:]}", (sign * cu_c, y - 0.022, cv_c), 0.205, 0.012, hinge)
    give(inner, mats["medallion"], mats["outline"])
    if front:
        clover("Clover", p["leaves"], (cu_c, cv_c), 0.10, sign, y - 0.034, hinge, mats["clover"], mats["outline"])
    else:
        rosette = disc("Rosette", (sign * cu_c, y - 0.03, cv_c), 0.06, 0.012, hinge, segments=12)
        give(rosette, mats["trim"], mats["outline"])


def build_spine(p, mats, root):
    bm = bmesh.new()
    segs = 16
    rings = []
    for zi, z in enumerate((-H / 2, H / 2)):
        ring = []
        for i in range(segs + 1):
            a = math.pi * i / segs                     # half circle bulging towards the camera (-Y)
            ring.append(bm.verts.new((SPINE_R * math.cos(a), -SPINE_R * math.sin(a) - BOARD * 0.4, z)))
        rings.append(ring)
    for i in range(segs):
        bm.faces.new((rings[0][i], rings[0][i + 1], rings[1][i + 1], rings[1][i]))
    spine = mesh_object("Spine", bm, root, smooth=True)
    sol = spine.modifiers.new("Thickness", "SOLIDIFY")
    sol.thickness = BOARD * 0.8
    give(spine, mats["cover"], mats["outline"])
    for k, z in enumerate((-H * 0.36, -H * 0.12, H * 0.12, H * 0.36)):   # raised spine bands
        band = box(f"SpineBand{k}", -SPINE_R * 0.95, SPINE_R * 0.95, -SPINE_R - BOARD * 0.55, -BOARD * 0.2, z - 0.018, z + 0.018, root, bevel=0.008)
        give(band, mats["trim"], mats["outline"])


def build_aura(p, mats, root):
    """Glow card behind the book (faces the camera). The compositor bloom adds the haze around the bright edges."""
    bm = bmesh.new()
    bmesh.ops.create_grid(bm, x_segments=1, y_segments=1, size=1.0)
    bmesh.ops.rotate(bm, verts=bm.verts, cent=(0, 0, 0), matrix=Matrix.Rotation(math.radians(90), 3, "X"))
    ob = mesh_object("Aura", bm, root)
    ob.scale = (1.9, 1.0, 1.55)
    ob.location = (0, 0.9, 0)
    give(ob, mats["aura"])
    if hasattr(ob, "visible_shadow"):
        ob.visible_shadow = False


def build(preset=PRESET, open_deg=34.0):
    p = PRESETS[preset]
    clear_scene()
    dark = tuple(c * 0.25 for c in p["shade"])
    mats = {
        "cover": toon_material("Cover", p["cover"], p["shade"], rim=tuple(c * 0.55 for c in p["aura"]), grime=0.85 if p["tattered"] else 0.0),
        "trim": toon_material("Trim", p["trim"], tuple(c * 0.55 for c in p["trim"]), light=(1.0, 0.97, 0.88), rim=(0.25, 0.22, 0.15)),
        # a black (five-leaf) clover needs a lighter field to read against
        "medallion": toon_material("Medallion", (0.20, 0.17, 0.16) if p["leaves"] == 5 else tuple(c * 0.8 for c in p["cover"]),
                                   (0.09, 0.07, 0.07) if p["leaves"] == 5 else tuple(c * 0.5 for c in p["shade"])),
        "clover": toon_material("Clover", (0.02, 0.02, 0.02) if p["leaves"] == 5 else p["trim"],
                                (0.0, 0.0, 0.0) if p["leaves"] == 5 else tuple(c * 0.55 for c in p["trim"]),
                                light=(0.12, 0.10, 0.10) if p["leaves"] == 5 else (1.0, 0.97, 0.88),
                                rim=p["aura"] if p["leaves"] == 5 else None),
        "pages": toon_material("Pages", (1.0, 0.95, 0.80), (0.80, 0.72, 0.58), light=(1.0, 1.0, 0.95), strength=p["pages"] * 0.6, page_lines=True),
        "outline": outline_material(dark),
        "aura": aura_material(p["aura"]),
    }
    root = link(bpy.data.objects.new("Grimoire", None))
    root.rotation_euler = (math.radians(6), math.radians(-5), math.radians(-8))   # floating, not standing on a table
    build_spine(p, mats, root)
    build_half(+1, True, p, mats, root, open_deg)
    build_half(-1, False, p, mats, root, open_deg)
    build_aura(p, mats, root)
    setup_scene(p)
    return root


# ------------------------------------------------------------------------------------------------ camera, lights, render
def setup_scene(p):
    sc = bpy.context.scene
    cam_data = bpy.data.cameras.new("Camera")
    cam_data.lens = 50
    cam = link(bpy.data.objects.new("Camera", cam_data))
    cam.location = (0.35, -4.2, 0.55)
    direction = Vector((0.0, 0.35, 0.0)) - cam.location
    cam.rotation_euler = direction.to_track_quat("-Z", "Y").to_euler()
    sc.camera = cam

    sun_data = bpy.data.lights.new("Key", "SUN")
    sun_data.energy = 3.5
    sun_data.angle = math.radians(3)
    sun = link(bpy.data.objects.new("Key", sun_data))
    sun.rotation_euler = (math.radians(55), math.radians(-25), math.radians(-35))

    world = sc.world or bpy.data.worlds.new("World")
    sc.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs["Color"].default_value = (*(c * 0.06 for c in p["aura"]), 1.0)
        bg.inputs["Strength"].default_value = 1.0

    engines = [e.identifier for e in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items]
    sc.render.engine = "BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in engines else "BLENDER_EEVEE"
    try:
        sc.view_settings.view_transform = "Standard"     # Filmic/AgX would wash out the flat anime colours
        sc.view_settings.look = "None"
    except TypeError:
        pass
    sc.render.film_transparent = False
    setup_bloom(sc)


def setup_bloom(sc):
    """Glare (Bloom) in the compositor: the glow around the book and the bright pages. Handles the 4.x and 5.x compositor APIs."""
    try:
        if hasattr(sc, "compositing_node_group"):               # Blender 5.x: compositor is a node group
            ng = bpy.data.node_groups.new("Grimoire Bloom", "CompositorNodeTree")
            ng.interface.new_socket("Image", in_out="OUTPUT", socket_type="NodeSocketColor")
            rl = ng.nodes.new("CompositorNodeRLayers")
            glare = ng.nodes.new("CompositorNodeGlare")
            out = ng.nodes.new("NodeGroupOutput")
            glare.inputs["Type"].default_value = "Bloom"
            glare.inputs["Threshold"].default_value = 0.85
            glare.inputs["Strength"].default_value = 0.9
            glare.inputs["Size"].default_value = 0.6
            ng.links.new(rl.outputs["Image"], glare.inputs["Image"])
            ng.links.new(glare.outputs["Image"], out.inputs[0])
            sc.compositing_node_group = ng
        else:                                                   # Blender 4.2 - 4.5
            sc.use_nodes = True
            nt = sc.node_tree
            nt.nodes.clear()
            rl = nt.nodes.new("CompositorNodeRLayers")
            glare = nt.nodes.new("CompositorNodeGlare")
            comp = nt.nodes.new("CompositorNodeComposite")
            glare.glare_type = "BLOOM" if "BLOOM" in [i.identifier for i in glare.bl_rna.properties["glare_type"].enum_items] else "FOG_GLOW"
            glare.threshold = 0.85
            glare.size = 7
            nt.links.new(rl.outputs["Image"], glare.inputs["Image"])
            nt.links.new(glare.outputs["Image"], comp.inputs["Image"])
    except Exception as e:   # bloom is a nice-to-have; the model and materials are what matter
        print("[grimoire] bloom setup skipped:", e)


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    opts = {"--preset": PRESET, "--render": None, "--save": None, "--res": "1280x960", "--samples": "32", "--open": "34"}
    for i in range(0, len(argv) - 1, 2):
        opts[argv[i]] = argv[i + 1]
    build(opts["--preset"], float(opts["--open"]))
    sc = bpy.context.scene
    rx, ry = (int(v) for v in opts["--res"].lower().split("x"))
    sc.render.resolution_x, sc.render.resolution_y = rx, ry
    if hasattr(sc, "eevee") and hasattr(sc.eevee, "taa_render_samples"):
        sc.eevee.taa_render_samples = int(opts["--samples"])
    if opts["--save"]:
        bpy.ops.wm.save_as_mainfile(filepath=bpy.path.abspath(opts["--save"]))
    if opts["--render"]:
        sc.render.filepath = bpy.path.abspath(opts["--render"])
        bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    main()

"""
Painting Magic palette & brush builder for Blender (4.2 LTS - 5.x), for the 0.44 remake.

Builds the two props Rill Boismortier manifests with his grimoire, matching the concept: a classic wooden thumb-hole artist's
palette (kidney outline, finger notch, bevelled rim, a gentle dish) holding seven glossy pools of glowing mana paint, and a long
tapered paintbrush (lacquered handle, silver ferrule, bristles drawn to a point, the tip loaded with wet paint). A swept,
glowing paint trail arcs from the brush, the way strokes hang in the air when he paints.

Use it two ways:
  * In Blender: Scripting workspace > Text > Open this file > Run Script (clears the scene, builds everything).
  * Headless:   blender --background --python build_palette_brush.py -- --render palette.png --save palette.blend --glb palette.glb
                options: --render PNG  --save BLEND  --glb GLB (glTF export of the props)  --res 1280x960  --samples 32
                         --engine eevee|cycles  --paint fire|water|ice|wind|earth|lightning|ink (the brush's paint)

Materials are procedural (no image textures): wood is a Wave texture grain over warm maple, the paint is a clear-coated glossy
principled shader with a little emission so it glows like mana (bloom picks it up), metal is a brushed silver. That gives a clean
base to UV and hand-paint, or to bake into the 32 px Minecraft textures (tools/gen_paint_items.py builds the in-game models).
"""
import math
import sys

import bpy  # first: bmesh and mathutils come with it (also when run as the pip 'bpy' module)
import bmesh
from mathutils import Vector

PAINTS = {
    "ink": (0.23, 0.48, 1.0), "fire": (1.0, 0.35, 0.23), "water": (0.29, 0.66, 1.0), "ice": (0.54, 0.90, 1.0),
    "wind": (0.49, 0.94, 0.69), "earth": (0.69, 0.53, 0.29), "lightning": (1.0, 0.90, 0.35),
}
POOL_ORDER = ["ink", "fire", "water", "ice", "wind", "earth", "lightning"]

# palette proportions (Blender units ~ 1 = 40 cm): kidney 1.0 x 0.8, 2.4 cm thick
PW, PH, PT = 1.0, 0.8, 0.024


def clear_scene():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob, do_unlink=True)
    for coll in (bpy.data.meshes, bpy.data.materials, bpy.data.curves, bpy.data.lights, bpy.data.cameras):
        for block in list(coll):
            if block.users == 0:
                coll.remove(block)


def link(ob, parent=None):
    bpy.context.scene.collection.objects.link(ob)
    if parent is not None:
        ob.parent = parent
    return ob


def mesh_object(name, bm, parent=None, smooth=True):
    me = bpy.data.meshes.new(name)
    bm.to_mesh(me)
    bm.free()
    if smooth:
        for poly in me.polygons:
            poly.use_smooth = True
    return link(bpy.data.objects.new(name, me), parent)


def _sock(sockets, *names):
    for n in names:
        if n in sockets:
            return sockets[n]
    return None


# ------------------------------------------------------------------------------------------------ materials
def principled(name, base, rough=0.5, metal=0.0, coat=0.0, emit=None, emit_strength=0.0):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*base, 1.0)
    bsdf.inputs["Roughness"].default_value = rough
    bsdf.inputs["Metallic"].default_value = metal
    c = _sock(bsdf.inputs, "Coat Weight", "Clearcoat")
    if c is not None:
        c.default_value = coat
    if emit is not None:
        e = _sock(bsdf.inputs, "Emission Color", "Emission")
        if e is not None:
            e.default_value = (*emit, 1.0)
        s = _sock(bsdf.inputs, "Emission Strength")
        if s is not None:
            s.default_value = emit_strength
    return mat


def wood_material():
    """Warm maple with a wave-texture grain driving colour and a touch of roughness."""
    mat = principled("Palette Wood", (0.62, 0.38, 0.18), rough=0.45, coat=0.25)
    nt = mat.node_tree
    bsdf = nt.nodes.get("Principled BSDF")
    wave = nt.nodes.new("ShaderNodeTexWave")
    wave.inputs["Scale"].default_value = 4.0
    wave.inputs["Distortion"].default_value = 6.0
    wave.inputs["Detail"].default_value = 3.0
    ramp = nt.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].color = (0.42, 0.24, 0.11, 1)
    ramp.color_ramp.elements[1].color = (0.78, 0.52, 0.28, 1)
    nt.links.new(wave.outputs["Fac"], ramp.inputs["Fac"])
    nt.links.new(ramp.outputs["Color"], bsdf.inputs["Base Color"])
    return mat


def paint_material(name, rgb, glow=0.35):
    """Wet mana paint: saturated, glossy under a clear coat, glowing a little in its own colour."""
    return principled(name, rgb, rough=0.08, coat=1.0, emit=rgb, emit_strength=glow)


# ------------------------------------------------------------------------------------------------ the palette
def kidney_outline(n=96):
    """The palette's outline in XY: an ellipse with a finger notch bitten out of the lower left."""
    pts = []
    notch_c, notch_r = Vector((-0.5 * PW + 0.06, -0.5 * PH + 0.10)), 0.17
    for i in range(n):
        a = 2 * math.pi * i / n
        p = Vector((math.cos(a) * PW / 2, math.sin(a) * PH / 2))
        d = p - notch_c
        if d.length < notch_r:                     # pull the rim in round the notch
            p = notch_c + d.normalized() * notch_r
        pts.append(p)
    return pts


def build_palette(paint_mats):
    bm = bmesh.new()
    outline = kidney_outline()
    hole_c, hole_r = Vector((-0.22, -0.02)), 0.07
    # a fan of quads between the outline and the thumb hole, top and bottom, then the rims
    hole = [hole_c + Vector((math.cos(2 * math.pi * i / len(outline)), math.sin(2 * math.pi * i / len(outline)))) * hole_r
            for i in range(len(outline))]
    # match each outline point to the hole point at the same angle round the hole centre (keeps the quads untangled)
    def ang(p):
        return math.atan2(p.y - hole_c.y, p.x - hole_c.x)
    outline.sort(key=ang)
    hole.sort(key=ang)
    top_o = [bm.verts.new((p.x, p.y, PT / 2)) for p in outline]
    top_h = [bm.verts.new((p.x, p.y, PT / 2)) for p in hole]
    bot_o = [bm.verts.new((p.x, p.y, -PT / 2)) for p in outline]
    bot_h = [bm.verts.new((p.x, p.y, -PT / 2)) for p in hole]
    n = len(outline)
    for i in range(n):
        j = (i + 1) % n
        bm.faces.new((top_o[i], top_o[j], top_h[j], top_h[i]))
        bm.faces.new((bot_o[j], bot_o[i], bot_h[i], bot_h[j]))
        bm.faces.new((bot_o[i], bot_o[j], top_o[j], top_o[i]))      # outer rim
        bm.faces.new((top_h[i], top_h[j], bot_h[j], bot_h[i]))      # thumb hole wall
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    ob = mesh_object("Palette", bm)
    bev = ob.modifiers.new("Rim", "BEVEL")
    bev.width = 0.008
    bev.segments = 3
    bev.limit_method = "ANGLE"
    sub = ob.modifiers.new("Smooth", "SUBSURF")
    sub.levels = sub.render_levels = 1
    bend = ob.modifiers.new("Dish", "SIMPLE_DEFORM")         # a gentle dish, like a real bent-ply palette
    bend.deform_method = "BEND"
    bend.angle = math.radians(12)
    bend.deform_axis = "Y"
    ob.data.materials.append(wood_material())
    # the pools of paint: flattened, slightly lumpy blobs round the far side of the board
    for k, name in enumerate(POOL_ORDER):
        a = math.radians(-60 + k * 38)
        x, y = 0.08 + math.cos(a) * 0.3, math.sin(a) * 0.26
        bpy.ops.mesh.primitive_uv_sphere_add(segments=24, ring_count=12, radius=0.06, location=(x, y, PT / 2 + 0.006))
        blob = bpy.context.active_object
        blob.name = "Paint " + name
        blob.scale = (1.0 + 0.25 * math.sin(k * 2.1), 0.85 + 0.2 * math.cos(k * 1.7), 0.22)
        tex = bpy.data.textures.new("Paint lumps " + name, "CLOUDS")
        tex.noise_scale = 0.05
        disp = blob.modifiers.new("Lumps", "DISPLACE")
        disp.texture = tex
        disp.strength = 0.012
        blob.data.materials.append(paint_mats[name])
        for poly in blob.data.polygons:
            poly.use_smooth = True
        blob.parent = ob
    return ob


# ------------------------------------------------------------------------------------------------ the brush
def lathe(name, profile, segments=32, mat=None, parent=None):
    """Spins a (radius, z) profile round Z into a closed surface of revolution."""
    bm = bmesh.new()
    rings = []
    for r, z in profile:
        rings.append([bm.verts.new((math.cos(2 * math.pi * s / segments) * r, math.sin(2 * math.pi * s / segments) * r, z)) for s in range(segments)])
    for a, b in zip(rings, rings[1:]):
        for s in range(segments):
            t = (s + 1) % segments
            bm.faces.new((a[s], a[t], b[t], b[s]))
    if profile[0][0] > 0:
        bm.faces.new(list(reversed(rings[0])))
    if profile[-1][0] > 0:
        bm.faces.new(rings[-1])
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    ob = mesh_object(name, bm, parent)
    if mat:
        ob.data.materials.append(mat)
    return ob


def build_brush(paint_mat):
    root = link(bpy.data.objects.new("Paintbrush", None))
    lacquer = principled("Brush Lacquer", (0.42, 0.05, 0.07), rough=0.18, coat=1.0)
    silver = principled("Brush Ferrule", (0.86, 0.87, 0.92), rough=0.22, metal=1.0)
    bristle = principled("Bristles", (0.86, 0.76, 0.55), rough=0.7)
    # handle: slim at the end, swelling, then waisting into the ferrule (a classic artist's brush)
    lathe("Handle", [(0.0, 0.0), (0.008, 0.002), (0.011, 0.05), (0.016, 0.30), (0.014, 0.46), (0.011, 0.5)], mat=lacquer, parent=root)
    lathe("Ferrule", [(0.0115, 0.5), (0.0125, 0.505), (0.013, 0.58), (0.0118, 0.6)], mat=silver, parent=root)
    lathe("Bristles", [(0.0115, 0.6), (0.014, 0.63), (0.012, 0.67), (0.0, 0.70)], mat=bristle, parent=root)
    tip = lathe("Paint Load", [(0.0122, 0.655), (0.0135, 0.67), (0.009, 0.69), (0.0, 0.715)], mat=paint_mat, parent=root)
    tip.scale = (1.06, 1.06, 1.0)
    bend = tip.modifiers.new("Flick", "SIMPLE_DEFORM")        # the loaded tip bends a little, as if mid-stroke
    bend.deform_method = "BEND"
    bend.angle = math.radians(14)
    return root


def build_trail(paint_mat, start):
    """A swept stroke of glowing paint hanging in the air from the brush tip."""
    cu = bpy.data.curves.new("Paint Trail", "CURVE")
    cu.dimensions = "3D"
    cu.bevel_depth = 0.018
    cu.bevel_resolution = 4
    sp = cu.splines.new("BEZIER")
    pts = [start, start + Vector((0.25, 0.1, 0.18)), start + Vector((0.55, -0.05, 0.22)), start + Vector((0.8, 0.05, 0.05))]
    sp.bezier_points.add(len(pts) - 1)
    for bp, p in zip(sp.bezier_points, pts):
        bp.co = p
        bp.handle_left_type = bp.handle_right_type = "AUTO"
    for i, bp in enumerate(sp.bezier_points):                 # thick in the middle, drying thin at the ends
        bp.radius = [0.4, 1.2, 1.0, 0.25][i]
    ob = link(bpy.data.objects.new("Paint Trail", cu))
    ob.data.materials.append(paint_mat)
    return ob


# ------------------------------------------------------------------------------------------------ scene
def setup_scene(engine):
    sc = bpy.context.scene
    cam_data = bpy.data.cameras.new("Camera")
    cam_data.lens = 55
    cam = link(bpy.data.objects.new("Camera", cam_data))
    cam.location = (0.45, -2.1, 1.25)
    cam.rotation_euler = (Vector((0.22, 0.05, 0.12)) - cam.location).to_track_quat("-Z", "Y").to_euler()
    sc.camera = cam
    key = link(bpy.data.objects.new("Key", bpy.data.lights.new("Key", "AREA")))
    key.data.energy = 45
    key.data.size = 1.2
    key.location = (-0.9, -1.0, 1.6)
    key.rotation_euler = (Vector((0, 0, 0)) - key.location).to_track_quat("-Z", "Y").to_euler()
    rim = link(bpy.data.objects.new("Rim", bpy.data.lights.new("Rim", "AREA")))
    rim.data.energy = 35
    rim.data.color = (0.75, 0.82, 1.0)
    rim.location = (0.9, 1.1, 0.9)
    rim.rotation_euler = (Vector((0, 0, 0)) - rim.location).to_track_quat("-Z", "Y").to_euler()
    world = sc.world or bpy.data.worlds.new("World")
    sc.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs["Color"].default_value = (0.035, 0.035, 0.06, 1.0)
    engines = [e.identifier for e in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items]
    if engine == "cycles":
        sc.render.engine = "CYCLES"
        sc.cycles.device = "CPU"
    else:
        sc.render.engine = "BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in engines else "BLENDER_EEVEE"
    try:
        sc.view_settings.view_transform = "AgX"
    except TypeError:
        pass
    setup_bloom(sc)


def setup_bloom(sc):
    """Glare in the compositor so the paint glows (4.x and 5.x compositor APIs)."""
    try:
        if hasattr(sc, "compositing_node_group"):
            ng = bpy.data.node_groups.new("Paint Bloom", "CompositorNodeTree")
            ng.interface.new_socket("Image", in_out="OUTPUT", socket_type="NodeSocketColor")
            rl = ng.nodes.new("CompositorNodeRLayers")
            glare = ng.nodes.new("CompositorNodeGlare")
            out = ng.nodes.new("NodeGroupOutput")
            glare.inputs["Type"].default_value = "Bloom"
            glare.inputs["Threshold"].default_value = 0.9
            glare.inputs["Strength"].default_value = 0.7
            ng.links.new(rl.outputs["Image"], glare.inputs["Image"])
            ng.links.new(glare.outputs["Image"], out.inputs[0])
            sc.compositing_node_group = ng
        else:
            sc.use_nodes = True
            nt = sc.node_tree
            nt.nodes.clear()
            rl = nt.nodes.new("CompositorNodeRLayers")
            glare = nt.nodes.new("CompositorNodeGlare")
            comp = nt.nodes.new("CompositorNodeComposite")
            glare.glare_type = "FOG_GLOW"
            glare.threshold = 0.9
            nt.links.new(rl.outputs["Image"], glare.inputs["Image"])
            nt.links.new(glare.outputs["Image"], comp.inputs["Image"])
    except Exception as e:
        print("[palette] bloom setup skipped:", e)


def build(paint="ink"):
    clear_scene()
    mats = {n: paint_material("Paint " + n, rgb) for n, rgb in PAINTS.items()}
    pal = build_palette(mats)
    pal.rotation_euler = (math.radians(18), 0, math.radians(-8))
    brush = build_brush(mats[paint])
    brush.location = (-0.05, -0.22, 0.16)                    # lying across the palette, tip up and to the right
    brush.rotation_euler = (math.radians(0), math.radians(72), math.radians(30))
    bpy.context.view_layer.update()
    tip = brush.matrix_world @ Vector((0, 0, 0.71))
    build_trail(paint_material("Trail " + paint, PAINTS[paint], glow=1.6), tip)
    return pal, brush


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    opts = {"--render": None, "--save": None, "--glb": None, "--res": "1280x960", "--samples": "32", "--engine": "eevee", "--paint": "ink"}
    for i in range(0, len(argv) - 1, 2):
        opts[argv[i]] = argv[i + 1]
    build(opts["--paint"])
    setup_scene(opts["--engine"])
    sc = bpy.context.scene
    rx, ry = (int(v) for v in opts["--res"].lower().split("x"))
    sc.render.resolution_x, sc.render.resolution_y = rx, ry
    if sc.render.engine == "CYCLES":
        sc.cycles.samples = int(opts["--samples"])
    elif hasattr(sc, "eevee") and hasattr(sc.eevee, "taa_render_samples"):
        sc.eevee.taa_render_samples = int(opts["--samples"])
    if opts["--save"]:
        bpy.ops.wm.save_as_mainfile(filepath=bpy.path.abspath(opts["--save"]))
    if opts["--glb"]:
        bpy.ops.export_scene.gltf(filepath=bpy.path.abspath(opts["--glb"]), export_format="GLB", export_apply=True)
    if opts["--render"]:
        sc.render.filepath = bpy.path.abspath(opts["--render"])
        bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    main()

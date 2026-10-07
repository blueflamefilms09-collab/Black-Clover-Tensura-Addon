"""
Dice Magic & Compass Magic props for Blender (4.2 LTS - 5.x), for the 0.45 extended attributes.

Builds, side by side:
  * an elemental d6 of translucent resin (bevelled cube, glass-like resin with a coloured volume swirl), its faces engraved with
    the six elements' numbers, the engraving filled with glowing gold;
  * a d20 of violet resin (bevelled icosahedron) with a swirling galaxy core (emissive noise inside) and gold numbers on all 20
    faces, opposite faces summing to 21 like a real die;
  * a brass navigator's compass (Letoile): a bevelled brass case and ring, a green dial with an eight-point rose, a gold needle
    and a glass cover.

Use it in Blender (Scripting > Run Script) or headless:
    blender --background --python build_dice_compass.py -- --render dice.png --glb dice.glb --engine cycles --samples 32
Everything is procedural (Blender's built-in font for the numbers), so it is a clean base to sculpt, re-material or bake.
"""
import math
import sys

import bpy  # first: bmesh and mathutils come with it (also when run as the pip 'bpy' module)
from mathutils import Matrix, Vector


def clear_scene():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob, do_unlink=True)


def link(ob, parent=None):
    bpy.context.scene.collection.objects.link(ob)
    if parent is not None:
        ob.parent = parent
    return ob


def _sock(sockets, *names):
    for n in names:
        if n in sockets:
            return sockets[n]
    return None


def principled(name, base, rough=0.4, metal=0.0, transmission=0.0, coat=0.0, emit=None, emit_strength=0.0, ior=1.5):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    b = mat.node_tree.nodes.get("Principled BSDF")
    b.inputs["Base Color"].default_value = (*base, 1)
    b.inputs["Roughness"].default_value = rough
    b.inputs["Metallic"].default_value = metal
    t = _sock(b.inputs, "Transmission Weight", "Transmission")
    if t is not None:
        t.default_value = transmission
    i = _sock(b.inputs, "IOR")
    if i is not None:
        i.default_value = ior
    c = _sock(b.inputs, "Coat Weight", "Clearcoat")
    if c is not None:
        c.default_value = coat
    if emit is not None:
        e = _sock(b.inputs, "Emission Color", "Emission")
        if e is not None:
            e.default_value = (*emit, 1)
        s = _sock(b.inputs, "Emission Strength")
        if s is not None:
            s.default_value = emit_strength
    return mat


def galaxy_material():
    """Emissive swirl for the d20 core: a twisted noise through a colour ramp (dark violet -> magenta -> white stars)."""
    mat = principled("Galaxy Core", (0.05, 0.0, 0.1), emit=(0.6, 0.2, 1.0), emit_strength=3.0)
    nt = mat.node_tree
    b = nt.nodes.get("Principled BSDF")
    noise = nt.nodes.new("ShaderNodeTexNoise")
    noise.inputs["Scale"].default_value = 6.0
    noise.inputs["Detail"].default_value = 8.0
    ramp = nt.nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].position = 0.45
    ramp.color_ramp.elements[0].color = (0.02, 0.0, 0.06, 1)
    ramp.color_ramp.elements[1].position = 0.75
    ramp.color_ramp.elements[1].color = (1.0, 0.55, 1.0, 1)
    nt.links.new(noise.outputs["Fac"], ramp.inputs["Fac"])
    e = _sock(b.inputs, "Emission Color", "Emission")
    nt.links.new(ramp.outputs["Color"], e)
    return mat


def bevel(ob, width, segments=4):
    m = ob.modifiers.new("Bevel", "BEVEL")
    m.width = width
    m.segments = segments
    m.limit_method = "ANGLE"
    return m


def engraved_number(text, location, normal, up, size, mat, parent):
    """A gold number on a face: a text curve, extruded a little, lying on the face plane, 'up' toward the given vector."""
    cu = bpy.data.curves.new("Num " + text, "FONT")
    cu.body = text
    cu.align_x = "CENTER"
    cu.align_y = "CENTER"
    cu.size = size
    cu.extrude = size * 0.06
    ob = link(bpy.data.objects.new("Num " + text, cu), parent)
    z = normal.normalized()
    y = (up - z * up.dot(z)).normalized()
    x = y.cross(z)
    ob.matrix_local = Matrix.Translation(location + z * 0.002) @ Matrix((x, y, z)).transposed().to_4x4()
    ob.data.materials.append(mat)
    return ob


def build_d6(gold):
    root = link(bpy.data.objects.new("Elemental d6", None))
    bpy.ops.mesh.primitive_cube_add(size=0.16)
    cube = bpy.context.active_object
    cube.name = "d6 Body"
    cube.parent = root
    bevel(cube, 0.018)
    cube.modifiers.new("Smooth", "SUBSURF").levels = 1
    cube.data.materials.append(principled("Fire Resin", (1.0, 0.35, 0.18), rough=0.08, transmission=0.85, coat=1.0, emit=(1.0, 0.3, 0.1), emit_strength=0.3))
    faces = [(1, Vector((0, 0, 1)), Vector((0, 1, 0))), (6, Vector((0, 0, -1)), Vector((0, 1, 0))), (2, Vector((1, 0, 0)), Vector((0, 0, 1))),
             (5, Vector((-1, 0, 0)), Vector((0, 0, 1))), (3, Vector((0, 1, 0)), Vector((0, 0, 1))), (4, Vector((0, -1, 0)), Vector((0, 0, 1)))]
    for n, nrm, up in faces:
        engraved_number(str(n), nrm * 0.08, nrm, up, 0.07, gold, root)
    return root


def build_d20(gold):
    root = link(bpy.data.objects.new("Fate d20", None))
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=1, radius=0.11)
    ico = bpy.context.active_object
    ico.name = "d20 Body"
    ico.parent = root
    bevel(ico, 0.006, 3)
    ico.data.materials.append(principled("Violet Resin", (0.55, 0.3, 1.0), rough=0.05, transmission=0.9, coat=1.0, emit=(0.4, 0.15, 0.8), emit_strength=0.2))
    bpy.ops.mesh.primitive_ico_sphere_add(subdivisions=3, radius=0.055)
    core = bpy.context.active_object
    core.name = "Galaxy Core"
    core.parent = root
    core.data.materials.append(galaxy_material())
    # numbers: opposite faces sum to 21
    me = ico.data
    cents = [(Vector(sum((me.vertices[v].co for v in p.vertices), Vector())) / 3, p) for p in me.polygons]
    num = {}
    k = 1
    for i, (c, p) in enumerate(cents):
        if i in num:
            continue
        num[i] = k
        for j, (c2, p2) in enumerate(cents):
            if (c2 + c).length < 1e-4:
                num[j] = 21 - k
        k += 1
    for i, (c, p) in enumerate(cents):
        v0 = me.vertices[p.vertices[0]].co
        engraved_number(str(num.get(i, 0)), c, p.normal, v0 - c, 0.042, gold, root)
    return root


def build_compass():
    root = link(bpy.data.objects.new("Compass", None))
    brass = principled("Brass", (0.85, 0.62, 0.28), rough=0.25, metal=1.0)
    dial = principled("Dial", (0.12, 0.45, 0.25), rough=0.5)
    goldm = principled("Needle Gold", (1.0, 0.8, 0.3), rough=0.15, metal=1.0, emit=(1.0, 0.75, 0.3), emit_strength=1.5)
    glass = principled("Glass Cover", (0.95, 1.0, 0.97), rough=0.02, transmission=1.0)
    bpy.ops.mesh.primitive_cylinder_add(vertices=64, radius=0.2, depth=0.035)
    case = bpy.context.active_object
    case.name = "Case"
    case.parent = root
    bevel(case, 0.006)
    case.data.materials.append(brass)
    bpy.ops.mesh.primitive_torus_add(major_radius=0.2, minor_radius=0.014, major_segments=64, minor_segments=12, location=(0, 0, 0.018))
    ring = bpy.context.active_object
    ring.name = "Bezel"
    ring.parent = root
    ring.data.materials.append(brass)
    bpy.ops.mesh.primitive_cylinder_add(vertices=64, radius=0.18, depth=0.004, location=(0, 0, 0.019))
    face = bpy.context.active_object
    face.name = "Dial"
    face.parent = root
    face.data.materials.append(dial)
    # the eight-point rose: flat star points, gold
    for j in range(8):
        a = j * math.pi / 4
        r = 0.15 if j % 2 == 0 else 0.09
        me = bpy.data.meshes.new("Rose")
        pts = [(0, 0, 0.022), (math.cos(a + 0.35) * 0.02, math.sin(a + 0.35) * 0.02, 0.022), (math.cos(a) * r, math.sin(a) * r, 0.022),
               (math.cos(a - 0.35) * 0.02, math.sin(a - 0.35) * 0.02, 0.022)]
        me.from_pydata(pts, [], [(0, 1, 2, 3)])
        ob = link(bpy.data.objects.new("Rose Point", me), root)
        ob.data.materials.append(goldm if j % 2 == 0 else brass)
    # the needle: a slim diamond pointing to the lock, on a pivot
    me = bpy.data.meshes.new("Needle")
    me.from_pydata([(0.16, 0, 0.026), (0, 0.012, 0.026), (-0.16, 0, 0.026), (0, -0.012, 0.026)], [], [(0, 1, 2, 3)])
    needle = link(bpy.data.objects.new("Needle", me), root)
    needle.rotation_euler = (0, 0, math.radians(52))
    needle.data.materials.append(goldm)
    bpy.ops.mesh.primitive_uv_sphere_add(radius=0.19, location=(0, 0, -0.14))
    dome = bpy.context.active_object
    dome.name = "Glass"
    dome.parent = root
    dome.scale = (1, 1, 0.9)
    bisect = dome.modifiers.new("Cut", "BOOLEAN")
    bpy.ops.mesh.primitive_cube_add(size=1, location=(0, 0, -0.48))
    cutter = bpy.context.active_object
    cutter.hide_render = True
    cutter.display_type = "WIRE"
    bisect.object = cutter
    bisect.operation = "DIFFERENCE"
    dome.data.materials.append(glass)
    return root


def setup_scene(engine):
    sc = bpy.context.scene
    cam = link(bpy.data.objects.new("Camera", bpy.data.cameras.new("Camera")))
    cam.data.lens = 42
    cam.location = (0.0, -1.55, 0.95)
    cam.rotation_euler = (Vector((0, 0.0, 0.05)) - cam.location).to_track_quat("-Z", "Y").to_euler()
    sc.camera = cam
    for name, loc, energy, col in (("Key", (-0.8, -0.9, 1.2), 30, (1, 0.96, 0.9)), ("Rim", (0.9, 0.8, 0.7), 22, (0.75, 0.8, 1.0))):
        l = link(bpy.data.objects.new(name, bpy.data.lights.new(name, "AREA")))
        l.data.energy = energy
        l.data.size = 0.8
        l.data.color = col
        l.location = loc
        l.rotation_euler = (Vector((0, 0, 0)) - l.location).to_track_quat("-Z", "Y").to_euler()
    bpy.ops.mesh.primitive_plane_add(size=6, location=(0, 0, -0.105))
    floor = bpy.context.active_object
    floor.data.materials.append(principled("Slate", (0.06, 0.06, 0.08), rough=0.35))
    world = sc.world or bpy.data.worlds.new("World")
    sc.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs["Color"].default_value = (0.02, 0.02, 0.03, 1)
    engines = [e.identifier for e in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items]
    if engine == "cycles":
        sc.render.engine = "CYCLES"
        sc.cycles.device = "CPU"
    else:
        sc.render.engine = "BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in engines else "BLENDER_EEVEE"
    try:
        sc.view_settings.view_transform = "Standard"
    except TypeError:
        pass


def build():
    clear_scene()
    gold = principled("Engraving Gold", (1.0, 0.78, 0.3), rough=0.2, metal=1.0, emit=(1.0, 0.7, 0.25), emit_strength=2.0)
    d6 = build_d6(gold)
    d6.location = (-0.42, 0.05, -0.02)
    d6.rotation_euler = (math.radians(15), math.radians(-10), math.radians(30))
    d20 = build_d20(gold)
    d20.location = (-0.05, 0.1, 0.0)
    d20.rotation_euler = (math.radians(20), math.radians(10), math.radians(-15))
    comp = build_compass()
    comp.location = (0.42, 0.15, 0.05)
    comp.rotation_euler = (math.radians(55), 0, math.radians(-12))


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    opts = {"--render": None, "--save": None, "--glb": None, "--res": "1280x720", "--samples": "32", "--engine": "eevee"}
    for i in range(0, len(argv) - 1, 2):
        opts[argv[i]] = argv[i + 1]
    build()
    setup_scene(opts["--engine"])
    sc = bpy.context.scene
    rx, ry = (int(v) for v in opts["--res"].lower().split("x"))
    sc.render.resolution_x, sc.render.resolution_y = rx, ry
    if sc.render.engine == "CYCLES":
        sc.cycles.samples = int(opts["--samples"])
    if opts["--save"]:
        bpy.ops.wm.save_as_mainfile(filepath=bpy.path.abspath(opts["--save"]))
    if opts["--glb"]:
        bpy.ops.export_scene.gltf(filepath=bpy.path.abspath(opts["--glb"]), export_format="GLB", export_apply=True)
    if opts["--render"]:
        sc.render.filepath = bpy.path.abspath(opts["--render"])
        bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    main()

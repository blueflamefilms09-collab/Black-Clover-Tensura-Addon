"""
Zagred's corrupted five-leaf grimoire (Kotodama Magic, 0.47), procedural, rigged and textured. Blender 4.0+ (tested headless with
the 5.2 'bpy' module). No image files: every material is nodes.

What it builds:
  * the book: one bmesh mesh, an open hardcover with vertex groups
        front_cover  back_cover  spine  pages        (what each part is)
        page_edges                                    (the page block's outer edges: where the smoke clings)
        Cover.Front  Cover.Back  Spine                (deform groups for the rig; the spine/gutter blends between them)
    The outer faces of both covers are densely gridded and ETCHED: indigo starbursts (a big sixteen-ray burst, small eight-ray
    stars, a ring and a border line) are pushed into the leather, and an 'etch' attribute marks the grooves for the shader.
  * the rig: armature 'ZagredRig' (Root > Spine > Cover.Front / Cover.Back, hinged on the spine edges). Action 'OpenClose':
    frame 1 closed, open by 40, held open to 100, closed again by 140. A shape key 'Closed' holds the same closed pose for those who prefer shape keys
    (leave it at 0 while the armature drives the book, or remove the Armature modifier and animate the key instead).
  * the black five-leaf clover: a separate mesh on the front cover (the right one, seen from outside), bone-parented to
    Cover.Front so it closes with the cover.
  * embossed spine panels: three raised pewter bands across the spine, each with an S-scroll of swirls, bone-parented to Spine.
  * the cover shader: indigo leather (noise grain + bump); the etched grooves glow violet, the glow pulsing with a driver on
    #frame ("3 + 2 * sin(frame * 0.12)").
  * smoke: a volume box (Principled Volume, 4D noise animated by #frame, masked to hug the page edges and fade with height)
    AND a particle system emitting from the 'page_edges' vertex group, instancing dark purple-black volumetric puffs.

    blender --background --python build_zagred_grimoire.py -- --render open.png --frame 70 --view open --engine cycles
    blender --background --python build_zagred_grimoire.py -- --render closed.png --frame 140 --view closed --engine cycles
    options: --glb out.glb  --save out.blend  --res 1280x720  --samples 32  --engine cycles|eevee  --frame N  --view open|closed
             --smoke 0 (skip the volumes for a fast look)
"""
import math
import sys

import bpy  # first: bmesh and mathutils come with it (also when run as the pip 'bpy' module)
import bmesh
from mathutils import Matrix, Vector

# ---------------------------------------------------------------------------------------------------------------- dimensions (m)
W, H = 0.21, 0.29          # one cover: width (x, out from the spine) and height (y)
T = 0.012                  # cover board thickness
PT = 0.024                 # one page stack's thickness
G = T + PT                 # hinge offset from the centre line: closing puts the two page stacks face to face at x = 0
M = 0.008                  # page block inset from the cover edges
BULGE = 0.008              # how far the rounded spine bulges out
STEP = 0.0015              # etching grid resolution on the covers' outer faces
DEPTH = 0.0009             # etch depth
OPEN_DEG, CLOSED_DEG = 10.0, 90.0
# the action: closed -> opening -> held open -> closing (frame, degrees each cover is lifted)
KEYS = ((1, CLOSED_DEG), (40, OPEN_DEG), (100, OPEN_DEG), (140, CLOSED_DEG))


# ---------------------------------------------------------------------------------------------------------------- helpers
def clear_scene():
    for ob in list(bpy.data.objects):
        bpy.data.objects.remove(ob, do_unlink=True)


def link(ob):
    bpy.context.scene.collection.objects.link(ob)
    return ob


def _sock(sockets, *names):
    for n in names:
        if n in sockets:
            return sockets[n]
    return None


def smoothstep(a, b, x):
    t = min(1.0, max(0.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def drive(socket, expr):
    """A scripted driver on a node socket's value (simple expressions such as 'sin(frame*0.1)' need no Python auto-run)."""
    fc = socket.driver_add("default_value")
    fc.driver.type = "SCRIPTED"
    fc.driver.expression = expr
    return fc


# ---------------------------------------------------------------------------------------------------------------- the etching
def stars_for(front):
    """(cx, cy, R, rays, rotation, long/short) in cover coordinates (u out from the hinge, v along the height)."""
    main = (W * 0.5, 0.02 if front else 0.0, 0.112, 16, 0.0, True)
    small = [(0.035, 0.115, 0.03, 8, 0.2, False), (0.18, 0.118, 0.026, 8, 0.5, False), (0.03, -0.11, 0.028, 8, 0.1, False),
             (0.182, -0.112, 0.032, 8, 0.35, False), (0.105, -0.122, 0.018, 6, 0.0, False)]
    if not front:
        small = [(W - s[0], -s[1], s[2], s[3], s[4], s[5]) for s in small]
    return [main] + small


def etch_mask(u, v, stars):
    """0..1: how deep the groove is at (u, v)."""
    m = 0.0
    for (cx, cy, R, n, rot, longshort) in stars:
        dx, dy = u - cx, v - cy
        r = math.hypot(dx, dy)
        if r > R:
            continue
        if r < (0.006 if longshort else 0.0025):                             # the burst's heart
            return 1.0
        sector = 2 * math.pi / n
        th = math.atan2(dy, dx) - rot
        k = round(th / sector)
        a = th - k * sector
        Rk = R if (not longshort or k % 2 == 0) else R * 0.6
        if r < Rk:
            hw = 0.0034 * (1 - r / Rk) + 0.0005
            dist = abs(math.sin(a)) * r
            m = max(m, min(1.0, max(0.0, (hw - dist) / 0.0006 + 0.5)))
        if longshort:
            m = max(m, min(1.0, max(0.0, (0.0008 - abs(r - 0.36 * R)) / 0.0005)))
    e = min(u, W - u, v + H / 2, H / 2 - v)                                   # the border line
    m = max(m, min(1.0, max(0.0, (0.0008 - abs(e - 0.012)) / 0.0005)))
    return m


# ---------------------------------------------------------------------------------------------------------------- the book mesh
class Book:
    GROUPS = ("front_cover", "back_cover", "spine", "pages", "page_edges", "Cover.Front", "Cover.Back", "Spine")

    def __init__(self, ob):
        self.ob = ob
        for g in self.GROUPS:
            ob.vertex_groups.new(name=g)
        self.gi = {g: i for i, g in enumerate(self.GROUPS)}
        self.bm = bmesh.new()
        self.dl = self.bm.verts.layers.deform.verify()
        self.etch = self.bm.verts.layers.float.new("etch")
        self.sheet = self.bm.faces.layers.float.new("sheet")                  # 1 on the faces of a block, 0 on its walls (page edges)

    def vert(self, co, groups, etch=0.0):
        v = self.bm.verts.new(co)
        for g, w in groups.items():
            v[self.dl][self.gi[g]] = w
        v[self.etch] = etch
        return v

    def face(self, vs, mat):
        f = self.bm.faces.new(vs)
        f.material_index = mat
        return f

    def grid_solid(self, xs, ys, z_bottom, z_top, groups, mat, outer_etch=None, edge_group=None):
        """A solid block over the grid xs x ys between two height functions (bottom face, top face, four walls)."""
        nx, ny = len(xs), len(ys)
        bot, top = [], []
        for j, y in enumerate(ys):
            rb, rt = [], []
            for i, x in enumerate(xs):
                e = outer_etch(x, y) if outer_etch else 0.0
                gb = dict(groups(x, y))
                rb.append(self.vert((x, y, z_bottom(x, y, e)), gb, e))
                gt = dict(groups(x, y))
                if edge_group and edge_group(i, j, nx, ny):
                    gt["page_edges"] = 1.0
                rt.append(self.vert((x, y, z_top(x, y)), gt))
            bot.append(rb)
            top.append(rt)
        for j in range(ny - 1):
            for i in range(nx - 1):
                self.face([bot[j][i], bot[j + 1][i], bot[j + 1][i + 1], bot[j][i + 1]], mat)[self.sheet] = 1.0   # facing down
                self.face([top[j][i], top[j][i + 1], top[j + 1][i + 1], top[j + 1][i]], mat)[self.sheet] = 1.0   # facing up
        ring = [(i, 0) for i in range(nx - 1)] + [(nx - 1, j) for j in range(ny - 1)] + \
               [(i, ny - 1) for i in range(nx - 1, 0, -1)] + [(0, j) for j in range(ny - 1, 0, -1)]
        for k in range(len(ring)):
            (i0, j0), (i1, j1) = ring[k], ring[(k + 1) % len(ring)]
            self.face([bot[j0][i0], bot[j1][i1], top[j1][i1], top[j0][i0]], mat)
        return bot, top

    def finish(self):
        bmesh.ops.recalc_face_normals(self.bm, faces=self.bm.faces)
        self.bm.to_mesh(self.ob.data)
        self.bm.free()


def frange(a, b, step):
    n = max(1, int(round((b - a) / step)))
    return [a + (b - a) * k / n for k in range(n + 1)]


def build_book(mats):
    me = bpy.data.meshes.new("ZagredGrimoire")
    ob = link(bpy.data.objects.new("ZagredGrimoire", me))
    for m in mats:
        me.materials.append(m)
    b = Book(ob)
    us = frange(0, W, STEP)
    vs = frange(-H / 2, H / 2, STEP)
    for side, name, bone in ((1, "front_cover", "Cover.Front"), (-1, "back_cover", "Cover.Back")):
        stars = stars_for(side > 0)
        xs = [side * (G + u) for u in us]
        if side < 0:
            xs = xs[::-1]

        def u_of(x, s=side):
            return s * x - G
        b.grid_solid(xs, vs,
                     z_bottom=lambda x, y, e: DEPTH * e,                       # the outer face (down while open), etched inward
                     z_top=lambda x, y: T,
                     groups=lambda x, y, n=name, bn=bone: {n: 1.0, bn: 1.0},
                     mat=0, outer_etch=lambda x, y, s=stars, uo=u_of: etch_mask(uo(x), y, s))
        # the page stack on this cover: a little thicker at the gutter than at the fore-edge
        pu = frange(0, W - M, 0.006)
        pxs = [side * (G + u) for u in pu]
        pvs = frange(-H / 2 + M, H / 2 - M, 0.006)
        if side < 0:
            pxs = pxs[::-1]
        b.grid_solid(pxs, pvs,
                     z_bottom=lambda x, y, e: T,
                     z_top=lambda x, y, s=side: T + PT * (1 - 0.1 * ((s * x - G) / (W - M)) ** 1.5),
                     groups=lambda x, y, bn=bone: {"pages": 1.0, bn: 1.0},
                     mat=1,
                     edge_group=lambda i, j, nx, ny, s=side: (j == 0 or j == ny - 1 or (i == nx - 1 if s > 0 else i == 0)))
    # the spine: a rounded strip under the gutter, and the gutter of pages curling down into it (both blend between the covers)
    sx = frange(-G, G, 0.004)
    sy = frange(-H / 2, H / 2, 0.01)

    def blend(x, extra):
        wf = smoothstep(-G, G, x)
        g = {"Cover.Front": wf, "Cover.Back": 1 - wf, "Spine": 0.0}
        g.update(extra)
        return g
    b.grid_solid(sx, sy, z_bottom=lambda x, y, e: -T - BULGE * (1 - (x / G) ** 2), z_top=lambda x, y: 0.0,
                 groups=lambda x, y: blend(x, {"spine": 1.0}), mat=0)
    gx = frange(-G, G, 0.004)
    gy = frange(-H / 2 + M, H / 2 - M, 0.006)
    b.grid_solid(gx, gy, z_bottom=lambda x, y, e: 0.0005, z_top=lambda x, y: T + PT * (1 - 0.8 * (1 - (x / G) ** 2)),
                 groups=lambda x, y: blend(x, {"pages": 1.0}), mat=1)
    b.finish()
    for p in me.polygons:
        p.use_smooth = p.material_index == 1
    return ob


def closed_shape_key(ob):
    """'Closed': the same pose the rig reaches at frame 40, as a shape key (linear blend of the two hinge rotations)."""
    ob.shape_key_add(name="Basis")
    key = ob.shape_key_add(name="Closed")
    gf, gb = ob.vertex_groups["Cover.Front"].index, ob.vertex_groups["Cover.Back"].index
    rf = Matrix.Translation((G, 0, 0)) @ Matrix.Rotation(math.radians(-CLOSED_DEG), 4, "Y") @ Matrix.Translation((-G, 0, 0))
    rb = Matrix.Translation((-G, 0, 0)) @ Matrix.Rotation(math.radians(CLOSED_DEG), 4, "Y") @ Matrix.Translation((G, 0, 0))
    for v in ob.data.vertices:
        w = {g.group: g.weight for g in v.groups}
        wf, wb = w.get(gf, 0.0), w.get(gb, 0.0)
        tot = wf + wb
        if tot <= 0:
            continue
        co = v.co
        key.data[v.index].co = ((rf @ co) * wf + (rb @ co) * wb) / tot
    key.value = 0.0


# ---------------------------------------------------------------------------------------------------------------- clover, panels
def heart_outline(size, n=28):
    pts = []
    for k in range(n):
        t = k / n * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((x / 17 * size, (y + 17) / 17 * size))                     # the point at the origin, lobes along +y
    return pts


def prism(bm, outline, z0, z1):
    """Extrudes a closed 2D outline (x, y) between z0 and z1 into bm."""
    a = [bm.verts.new((x, y, z0)) for x, y in outline]
    b = [bm.verts.new((x, y, z1)) for x, y in outline]
    bm.faces.new(a[::-1])
    bm.faces.new(b)
    n = len(outline)
    for k in range(n):
        bm.faces.new([a[k], a[(k + 1) % n], b[(k + 1) % n], b[k]])


def build_clover(mat):
    """Five heart leaves (the fifth, the devil's, a little longer), a stem and a centre boss, lying flat, extruded 2 mm."""
    bm = bmesh.new()
    for k in range(5):
        ang = math.pi / 2 + k * 2 * math.pi / 5
        size = 0.024 if k != 2 else 0.027
        rot = ang - math.pi / 2
        c, s = math.cos(rot), math.sin(rot)
        pts = [(x * c - y * s, x * s + y * c) for x, y in heart_outline(size)]
        pts = [(x + math.cos(ang) * 0.003, y + math.sin(ang) * 0.003) for x, y in pts]
        prism(bm, pts, 0.0, 0.0022)
    stem = [(-0.002, 0.0), (0.002, 0.0), (0.006, -0.04), (0.002, -0.042)]
    prism(bm, stem, 0.0, 0.0016)
    prism(bm, [(math.cos(a) * 0.0055, math.sin(a) * 0.0055) for a in (k * math.tau / 16 for k in range(16))], 0.0, 0.003)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    me = bpy.data.meshes.new("FiveLeafClover")
    bm.to_mesh(me)
    bm.free()
    me.materials.append(mat)
    ob = link(bpy.data.objects.new("FiveLeafClover", me))
    bev = ob.modifiers.new("Bevel", "BEVEL")
    bev.width = 0.0006
    bev.segments = 2
    bev.limit_method = "ANGLE"
    return ob


def tube(bm, pts, radius, sides=6):
    """A thin tube along a polyline (the swirls)."""
    rings = []
    for i, p in enumerate(pts):
        a, b = pts[max(0, i - 1)], pts[min(len(pts) - 1, i + 1)]
        t = (Vector(b) - Vector(a)).normalized()
        n = t.cross(Vector((0, 0, 1)))
        if n.length < 1e-6:
            n = Vector((1, 0, 0))
        n.normalize()
        bnorm = t.cross(n)
        rings.append([bm.verts.new(Vector(p) + (n * math.cos(k * math.tau / sides) + bnorm * math.sin(k * math.tau / sides)) * radius)
                      for k in range(sides)])
    for r0, r1 in zip(rings, rings[1:]):
        for k in range(sides):
            bm.faces.new([r0[k], r0[(k + 1) % sides], r1[(k + 1) % sides], r1[k]])
    bm.faces.new(rings[0][::-1])
    bm.faces.new(rings[-1])


def spine_z(x, off):
    return -T - BULGE * (1 - (x / G) ** 2) - off


def build_spine_panels(mat):
    """Three raised bands across the spine, following its curve, each with an S-scroll of swirls and ridged ends."""
    bm = bmesh.new()
    xs = frange(-G * 0.94, G * 0.94, 0.004)
    for yc in (-0.088, 0.0, 0.088):
        y0, y1 = yc - 0.03, yc + 0.03
        top = [bm.verts.new((x, y, spine_z(x, 0.0002))) for y in (y0, y1) for x in xs]
        bot = [bm.verts.new((x, y, spine_z(x, 0.0022))) for y in (y0, y1) for x in xs]
        n = len(xs)
        for i in range(n - 1):
            bm.faces.new([top[i], top[i + 1], top[n + i + 1], top[n + i]])
            bm.faces.new([bot[i], bot[n + i], bot[n + i + 1], bot[i + 1]])
            bm.faces.new([top[i], bot[i], bot[i + 1], top[i + 1]])
            bm.faces.new([top[n + i], top[n + i + 1], bot[n + i + 1], bot[n + i]])
        bm.faces.new([top[0], top[n], bot[n], bot[0]])
        bm.faces.new([top[n - 1], bot[n - 1], bot[2 * n - 1], top[2 * n - 1]])
        for ye in (y0 + 0.002, y1 - 0.002):                                    # ridged ends
            tube(bm, [(x, ye, spine_z(x, 0.0024)) for x in xs], 0.0011)
        for s in (-1, 1):                                                      # the S-scroll: two spirals curling opposite ways
            pts = []
            for k in range(70):
                th = k / 69 * 3.2 * math.pi
                r = 0.0015 + 0.0042 * th / math.pi
                x = s * 0.016 + s * r * math.cos(th) * 0.9
                y = yc + r * math.sin(th) * s
                pts.append((x, y, spine_z(x, 0.0028)))
            tube(bm, pts, 0.0007)
        tube(bm, [(x, yc + 0.012 * math.sin(x / G * math.pi), spine_z(x, 0.0028)) for x in frange(-0.012, 0.012, 0.0015)], 0.0006)
    bmesh.ops.recalc_face_normals(bm, faces=bm.faces)
    me = bpy.data.meshes.new("SpinePanels")
    bm.to_mesh(me)
    bm.free()
    me.materials.append(mat)
    for p in me.polygons:
        p.use_smooth = True
    return link(bpy.data.objects.new("SpinePanels", me))


# ---------------------------------------------------------------------------------------------------------------- materials
def principled(name, base, rough=0.5, metal=0.0, emit=None, emit_strength=0.0):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    b = mat.node_tree.nodes.get("Principled BSDF")
    b.inputs["Base Color"].default_value = (*base, 1)
    b.inputs["Roughness"].default_value = rough
    b.inputs["Metallic"].default_value = metal
    if emit is not None:
        _sock(b.inputs, "Emission Color", "Emission").default_value = (*emit, 1)
        _sock(b.inputs, "Emission Strength").default_value = emit_strength
    return mat


def cover_material():
    """Indigo leather; the etched grooves (attribute 'etch') glow violet, pulsing on #frame."""
    mat = principled("Zagred Cover", (0.08, 0.05, 0.25), rough=0.62)
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links
    b = nodes.get("Principled BSDF")
    tc = nodes.new("ShaderNodeTexCoord")
    grain = nodes.new("ShaderNodeTexNoise")
    grain.inputs["Scale"].default_value = 260.0
    grain.inputs["Detail"].default_value = 6.0
    links.new(tc.outputs["Object"], grain.inputs["Vector"])
    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].color = (0.035, 0.02, 0.11, 1)
    ramp.color_ramp.elements[1].color = (0.13, 0.08, 0.36, 1)
    links.new(grain.outputs["Fac"], ramp.inputs["Fac"])
    attr = nodes.new("ShaderNodeAttribute")
    attr.attribute_name = "etch"
    mix = nodes.new("ShaderNodeMix")
    mix.data_type = "RGBA"
    col_in = [s for s in mix.inputs if s.type == "RGBA"]
    links.new(attr.outputs["Fac"], [s for s in mix.inputs if s.name == "Factor" and s.type == "VALUE"][0])
    links.new(ramp.outputs["Color"], col_in[0])
    col_in[1].default_value = (0.32, 0.14, 0.75, 1)
    links.new([s for s in mix.outputs if s.type == "RGBA"][0], b.inputs["Base Color"])
    bump = nodes.new("ShaderNodeBump")
    bump.inputs["Strength"].default_value = 0.25
    bump.inputs["Distance"].default_value = 0.0005
    links.new(grain.outputs["Fac"], bump.inputs["Height"])
    links.new(bump.outputs["Normal"], b.inputs["Normal"])
    pulse = nodes.new("ShaderNodeValue")
    pulse.label = "Pulse (#frame)"
    pulse.outputs[0].default_value = 3.0
    drive(pulse.outputs[0], "3.0 + 2.0*sin(frame*0.12)")
    strength = nodes.new("ShaderNodeMath")
    strength.operation = "MULTIPLY"
    links.new(attr.outputs["Fac"], strength.inputs[0])
    links.new(pulse.outputs[0], strength.inputs[1])
    _sock(b.inputs, "Emission Color", "Emission").default_value = (0.55, 0.25, 1.0, 1)
    links.new(strength.outputs[0], _sock(b.inputs, "Emission Strength"))
    return mat


def pages_material():
    """Grey-violet parchment (the corruption), page lines on the edges (Generated coords stay put while the rig moves)."""
    mat = principled("Zagred Pages", (0.6, 0.56, 0.55), rough=0.85)
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links
    b = nodes.get("Principled BSDF")
    tc = nodes.new("ShaderNodeTexCoord")
    wave = nodes.new("ShaderNodeTexWave")
    wave.wave_type = "BANDS"
    wave.bands_direction = "Z"
    wave.inputs["Scale"].default_value = 160.0
    wave.inputs["Distortion"].default_value = 2.0
    links.new(tc.outputs["Generated"], wave.inputs["Vector"])
    ramp = nodes.new("ShaderNodeValToRGB")
    ramp.color_ramp.elements[0].color = (0.2, 0.17, 0.2, 1)
    ramp.color_ramp.elements[1].color = (0.42, 0.38, 0.38, 1)
    links.new(wave.outputs["Fac"], ramp.inputs["Fac"])
    # the lines only on the page edges: the page faces stay plain, stained parchment
    stain = nodes.new("ShaderNodeTexNoise")
    stain.inputs["Scale"].default_value = 9.0
    links.new(tc.outputs["Generated"], stain.inputs["Vector"])
    face_ramp = nodes.new("ShaderNodeValToRGB")
    face_ramp.color_ramp.elements[0].color = (0.3, 0.26, 0.28, 1)
    face_ramp.color_ramp.elements[1].color = (0.46, 0.42, 0.4, 1)
    links.new(stain.outputs["Fac"], face_ramp.inputs["Fac"])
    sheet = nodes.new("ShaderNodeAttribute")                                   # set per face by the builder; follows the rig
    sheet.attribute_name = "sheet"
    mix = nodes.new("ShaderNodeMix")
    mix.data_type = "RGBA"
    cols = [s for s in mix.inputs if s.type == "RGBA"]
    links.new(sheet.outputs["Fac"], [s for s in mix.inputs if s.name == "Factor" and s.type == "VALUE"][0])
    links.new(ramp.outputs["Color"], cols[0])
    links.new(face_ramp.outputs["Color"], cols[1])
    links.new([s for s in mix.outputs if s.type == "RGBA"][0], b.inputs["Base Color"])
    return mat


def volume_material(name, density, puff=False):
    """Dark purple-black smoke: 4D noise animated by #frame. puff = per-instance sphere falloff; else masked to the page edges."""
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    nt = mat.node_tree
    nodes, links = nt.nodes, nt.links
    for n in list(nodes):
        if n.type != "OUTPUT_MATERIAL":
            nodes.remove(n)
    out = nodes.get("Material Output")
    vol = nodes.new("ShaderNodeVolumePrincipled")
    vol.inputs["Color"].default_value = (0.05, 0.01, 0.09, 1)
    _sock(vol.inputs, "Emission Color").default_value = (0.35, 0.1, 0.7, 1)
    links.new(vol.outputs[0], out.inputs["Volume"])
    tc = nodes.new("ShaderNodeTexCoord")
    noise = nodes.new("ShaderNodeTexNoise")
    noise.noise_dimensions = "4D"
    noise.inputs["Scale"].default_value = 60.0 if puff else 18.0
    noise.inputs["Detail"].default_value = 6.0
    drive(noise.inputs["W"], "frame*0.03")
    links.new(tc.outputs["Object"], noise.inputs["Vector"])
    shaped = nodes.new("ShaderNodeMapRange")
    shaped.inputs["From Min"].default_value = 0.42
    shaped.inputs["From Max"].default_value = 0.7
    links.new(noise.outputs["Fac"], shaped.inputs["Value"])

    def math_node(op, a, b=None):
        m = nodes.new("ShaderNodeMath")
        m.operation = op
        for k, x in enumerate((a, b)):
            if x is None:
                continue
            if isinstance(x, (int, float)):
                m.inputs[k].default_value = x
            else:
                links.new(x, m.inputs[k])
        return m.outputs[0]

    def vmath(op, a, b=None):
        m = nodes.new("ShaderNodeVectorMath")
        m.operation = op
        for k, x in enumerate((a, b)):
            if x is None:
                continue
            if isinstance(x, tuple):
                m.inputs[k].default_value = x
            else:
                links.new(x, m.inputs[k])
        return m.outputs["Vector"] if op not in ("LENGTH", "DOT_PRODUCT", "DISTANCE") else m.outputs["Value"]

    if puff:
        r = vmath("LENGTH", tc.outputs["Object"])
        mask = math_node("MAXIMUM", math_node("SUBTRACT", 1.0, math_node("DIVIDE", r, 0.012)), 0.0)
    else:
        # distance to the page blocks' outline (a rounded box per side, mirrored in x): smoke hugs the page edges
        cx, bx, by = G + (W - M) / 2, (W - M) / 2, H / 2 - M
        p = tc.outputs["Object"]
        a = vmath("ABSOLUTE", p)
        sep = nodes.new("ShaderNodeSeparateXYZ")
        links.new(a, sep.inputs[0])
        dx = math_node("SUBTRACT", math_node("ABSOLUTE", math_node("SUBTRACT", sep.outputs["X"], cx)), bx)
        dy = math_node("SUBTRACT", sep.outputs["Y"], by)
        outside = math_node("SQRT", math_node("ADD", math_node("POWER", math_node("MAXIMUM", dx, 0.0), 2.0),
                                                 math_node("POWER", math_node("MAXIMUM", dy, 0.0), 2.0)))
        inside = math_node("MINIMUM", math_node("MAXIMUM", dx, dy), 0.0)
        sd = math_node("ABSOLUTE", math_node("ADD", outside, inside))
        edge = math_node("EXPONENT", math_node("MULTIPLY", math_node("POWER", math_node("DIVIDE", sd, 0.03), 2.0), -1.0))
        sepz = nodes.new("ShaderNodeSeparateXYZ")
        links.new(p, sepz.inputs[0])
        height = math_node("EXPONENT", math_node("MULTIPLY", math_node("MAXIMUM", math_node("SUBTRACT", sepz.outputs["Z"], 0.03), 0.0), -18.0))
        floor_cut = math_node("GREATER_THAN", sepz.outputs["Z"], -0.03)
        mask = math_node("MULTIPLY", math_node("MULTIPLY", edge, height), floor_cut)
    dens = math_node("MULTIPLY", math_node("MULTIPLY", mask, shaped.outputs["Result"]), density)
    links.new(dens, vol.inputs["Density"])
    links.new(math_node("MULTIPLY", dens, 0.004), _sock(vol.inputs, "Emission Strength"))
    return mat


# ---------------------------------------------------------------------------------------------------------------- rig
def build_rig(book, clover, panels):
    arm = bpy.data.armatures.new("ZagredRig")
    rig = link(bpy.data.objects.new("ZagredRig", arm))
    bpy.context.view_layer.objects.active = rig
    bpy.ops.object.mode_set(mode="EDIT")
    eb = arm.edit_bones
    root = eb.new("Root")
    root.head, root.tail = (0, -H / 2 - 0.03, -0.03), (0, -H / 2 - 0.03, 0.02)
    spine = eb.new("Spine")
    spine.head, spine.tail, spine.parent = (0, -H / 2, 0), (0, H / 2, 0), root
    for name, x in (("Cover.Front", G), ("Cover.Back", -G)):
        bn = eb.new(name)
        bn.head, bn.tail, bn.roll, bn.parent = (x, -H / 2, 0), (x, H / 2, 0), 0.0, spine
    bpy.ops.object.mode_set(mode="OBJECT")
    mod = book.modifiers.new("Armature", "ARMATURE")
    mod.object = rig
    book.parent = rig
    for ob, bone in ((clover, "Cover.Front"), (panels, "Spine")):
        ob.parent = rig
        ob.parent_type = "BONE"
        ob.parent_bone = bone
        b = arm.bones[bone]
        ob.matrix_parent_inverse = (rig.matrix_world @ b.matrix_local @ Matrix.Translation((0, b.length, 0))).inverted()
    # the clover lies on the front cover's outer face (facing -z while open), on the big starburst's centre
    clover.matrix_basis = Matrix.Translation((G + W * 0.5, 0.02, -0.0003)) @ Matrix.Rotation(math.pi, 4, "X")
    # the action 'OpenClose' (KEYS): closed at 1, open by 40, held to 100, closed again at 140
    rig.animation_data_create()
    for frame, deg in KEYS:
        for name, sign in (("Cover.Front", -1), ("Cover.Back", 1)):
            pb = rig.pose.bones[name]
            pb.rotation_mode = "XYZ"
            pb.rotation_euler = (0, math.radians(sign * deg), 0)
            pb.keyframe_insert("rotation_euler", frame=frame)
    if rig.animation_data.action:
        rig.animation_data.action.name = "OpenClose"
    return rig


def smoke(book, density_mat, puff_mat):
    # the volume box round the book
    me = bpy.data.meshes.new("SmokeVolume")
    bm = bmesh.new()
    bmesh.ops.create_cube(bm, size=1.0)
    bmesh.ops.scale(bm, vec=(0.66, 0.42, 0.3), verts=bm.verts)
    bmesh.ops.translate(bm, vec=(0, 0, 0.1), verts=bm.verts)
    bm.to_mesh(me)
    bm.free()
    me.materials.append(density_mat)
    box = link(bpy.data.objects.new("SmokeVolume", me))
    box.display_type = "BOUNDS"
    # the puff the particles instance (kept far away; particle instancing ignores its location)
    bm = bmesh.new()
    bmesh.ops.create_icosphere(bm, subdivisions=2, radius=0.012)
    pm = bpy.data.meshes.new("SmokePuff")
    bm.to_mesh(pm)
    bm.free()
    pm.materials.append(puff_mat)
    puff = link(bpy.data.objects.new("SmokePuff", pm))
    puff.location = (0, 0, -50)
    # the particle system on the page edges
    book.modifiers.new("Smoke", "PARTICLE_SYSTEM")
    ps = book.particle_systems[-1]
    st = ps.settings
    st.name = "PageEdgeSmoke"
    st.count = 500
    st.frame_start, st.frame_end = 1, 140
    st.lifetime, st.lifetime_random = 60, 0.5
    st.emit_from = "VERT"
    st.use_emit_random = True
    st.normal_factor = 0.01
    st.object_align_factor = (0.0, 0.0, 0.012)
    st.factor_random = 0.01
    st.brownian_factor = 0.004
    st.drag_factor = 0.2
    st.effector_weights.gravity = 0.0
    st.render_type = "OBJECT"
    st.instance_object = puff
    st.particle_size = 1.0
    st.size_random = 0.7
    ps.vertex_group_density = "page_edges"
    return box


# ---------------------------------------------------------------------------------------------------------------- scene
def look(ob, target):
    ob.rotation_euler = (Vector(target) - ob.location).to_track_quat("-Z", "Y").to_euler()


def setup_scene(engine, view):
    sc = bpy.context.scene
    cam = link(bpy.data.objects.new("Camera", bpy.data.cameras.new("Camera")))
    cam.data.lens = 40
    if view == "closed":                                                      # the closed book laid flat, front cover up
        rig = bpy.data.objects["ZagredRig"]
        rig.rotation_euler = (0, math.radians(-90), 0)
        rig.location = (W / 2, 0, 0)
        bpy.data.objects["SmokeVolume"].hide_render = True                     # its edge mask is laid out for the open book
        cam.location = (0.05, -0.5, 0.5)
        look(cam, (0.0, 0.0, 0.0))
    else:
        cam.location = (0.0, -0.62, 0.52)
        look(cam, (0.0, 0.0, 0.0))
    sc.camera = cam
    for name, loc, energy, col in (("Key", (-0.5, -0.6, 0.9), 9, (1, 0.95, 0.92)), ("Rim", (0.7, 0.6, 0.5), 12, (0.6, 0.4, 1.0)),
                                   ("Under", (0.0, 0.0, -0.5), 3, (0.5, 0.2, 1.0))):
        l = link(bpy.data.objects.new(name, bpy.data.lights.new(name, "AREA")))
        l.data.energy, l.data.size, l.data.color = energy, 0.7, col
        l.location = loc
        look(l, (0, 0, 0.02))
    me = bpy.data.meshes.new("Floor")
    bm = bmesh.new()
    bmesh.ops.create_grid(bm, x_segments=1, y_segments=1, size=3.0)
    bmesh.ops.translate(bm, vec=(0, 0, -0.12), verts=bm.verts)
    bm.to_mesh(me)
    bm.free()
    me.materials.append(principled("Floor", (0.03, 0.025, 0.04), rough=0.4))
    link(bpy.data.objects.new("Floor", me))
    world = sc.world or bpy.data.worlds.new("World")
    sc.world = world
    world.use_nodes = True
    bg = world.node_tree.nodes.get("Background")
    if bg:
        bg.inputs["Color"].default_value = (0.012, 0.006, 0.02, 1)
    engines = [e.identifier for e in bpy.types.RenderSettings.bl_rna.properties["engine"].enum_items]
    if engine == "cycles":
        sc.render.engine = "CYCLES"
        sc.cycles.device = "CPU"
        sc.cycles.volume_step_rate = 2.0
        sc.cycles.volume_max_steps = 128                                          # a small object: no need for the default 1024
    else:
        sc.render.engine = "BLENDER_EEVEE_NEXT" if "BLENDER_EEVEE_NEXT" in engines else "BLENDER_EEVEE"
    try:
        sc.view_settings.view_transform = "Standard"
    except TypeError:
        pass
    sc.frame_start, sc.frame_end = 1, 140


def build():
    clear_scene()
    cover, pages = cover_material(), pages_material()
    book = build_book([cover, pages])
    closed_shape_key(book)
    clover = build_clover(principled("Black Clover", (0.01, 0.008, 0.012), rough=0.25, emit=(0.25, 0.1, 0.5), emit_strength=0.15))
    panels = build_spine_panels(principled("Pewter", (0.42, 0.38, 0.5), rough=0.3, metal=1.0, emit=(0.5, 0.25, 1.0), emit_strength=0.08))
    build_rig(book, clover, panels)
    smoke(book, volume_material("Page Edge Smoke", 900.0), volume_material("Smoke Puff", 600.0, puff=True))
    return book


def main():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    opts = {"--render": None, "--save": None, "--glb": None, "--res": "1280x720", "--samples": "32", "--engine": "eevee",
            "--frame": "70", "--view": "open", "--smoke": "1"}
    for i in range(0, len(argv) - 1, 2):
        opts[argv[i]] = argv[i + 1]
    build()
    setup_scene(opts["--engine"], opts["--view"])
    if opts["--smoke"] == "0":                                                # a quick look without the (slow) volumes
        bpy.data.objects["SmokeVolume"].hide_render = True
        bpy.data.objects["ZagredGrimoire"].particle_systems[0].settings.render_type = "NONE"
    sc = bpy.context.scene
    rx, ry = (int(v) for v in opts["--res"].lower().split("x"))
    sc.render.resolution_x, sc.render.resolution_y = rx, ry
    if sc.render.engine == "CYCLES":
        sc.cycles.samples = int(opts["--samples"])
    for f in range(1, int(opts["--frame"]) + 1):                              # step so the particles simulate up to the frame
        sc.frame_set(f)
    if opts["--save"]:
        bpy.ops.wm.save_as_mainfile(filepath=bpy.path.abspath(opts["--save"]))
    if opts["--glb"]:
        bpy.ops.export_scene.gltf(filepath=bpy.path.abspath(opts["--glb"]), export_format="GLB", export_apply=True)
    if opts["--render"]:
        sc.render.filepath = bpy.path.abspath(opts["--render"])
        bpy.ops.render.render(write_still=True)


if __name__ == "__main__":
    main()

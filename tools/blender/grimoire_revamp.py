"""The revamped grimoire (docs/grimoire_revamp.md): modelled, unwrapped and baked in Blender, exported as quad lists for the mod's own baker.

Run with a Python that has Blender as a module (pip install bpy pillow numpy), from the repo root:
    python -B tools/blender/grimoire_revamp.py                 # writes into the mod's assets
    python -B tools/blender/grimoire_revamp.py --trial /tmp/gr # look first: files go to /tmp/gr, nothing in the mod is touched

What it makes (model units, 1/16 block, front cover facing +Z, spine on the -X side, the same frame as GrimoireBookPlan):
  * the CLOSED book  : thick bevelled boards with a stepped (layered) cover - board, metal frame ring, leather plateau -, metal corner
                       caps on all eight corners, a round spine with four raised bands, a page block of four staggered slabs with a
                       ribbon bookmark, an octagonal emblem plate on the front and a rosette on the back;
  * the OPEN book    : the same two covers as halves (right half = front cover and emblem plate, left half = back cover and rosette)
                       with their page blocks, and a round hinged spine; the three opening steps are the halves turned about the hinge.
The emblem, the cover design (art pack), the magic sigil, the studs and the fusion filigree stay what they were: GrimoireRevampPlan lays them
over the plateau and the plate dish with the numbers written to the `meta` lines below.

Output: assets/anime_multiverse/grimoire_revamp/revamp_mesh.txt (quads: class, 4 x [x y z u v]) and two 512 px atlases
textures/item/grimoire_book/revamp_atlas.png (+ _tattered), baked with Cycles from the same UV layout. The atlases are NEUTRAL (light grey
leather and metal): the item colour handler tints them, exactly as it tinted the old textures.
"""
import math
import os
import sys

import bpy  # noqa: I001  (bpy first: it provides bmesh and mathutils)
import bmesh
import numpy as np
from mathutils import Matrix, Vector

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import bl_lib as bl  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
BAKE, ATLAS = 1024, 512
KIT_SHIFT = 40.0                       # the open pieces are baked 40 units to the side so they never shade the closed book

# ---- the numbers GrimoireBookPlan used, so the book keeps its place in the hand
BX0, BX1, BY0, BY1 = 2.5, 13.5, 1.3, 14.7      # board footprint
T = 0.7                                          # board thickness (was 0.5)
FRONT_IN, BACK_IN = 8.9, 7.1                     # inner faces of the boards
OPEN_CX, OPEN_CZ, OPEN_W, OPEN_T, OPEN_P = 8.0, 11.0, 7.6, 0.55, 0.9
OPEN_DEGREES = {1: 55.0, 2: 30.0, 3: 14.0}
RING_IN, RING_OUT, RING_H = 0.35, 0.8, 0.24     # the metal frame ring on a cover (inset from the board edge)
PLAT_H = 0.12                                    # the leather plateau inside the ring
CLS = {"L": 0, "M": 1, "F": 2, "R": 3, "P": 4}   # leather (tint 0), metal (tint 1), frame ring (tint 1, dropped on a tattered book), ribbon (tint 2), pages


# ================================================================ small modelling kit (raw model-unit coordinates, y up)
class Kit:
    def __init__(self):
        self.objs = []
        self.meta = {}

    def solid(self, name, verts, faces, mat, cls, grp, cull=(), bevel=0.0, segs=1, shift=(0, 0, 0)):
        ob = bl.obj(name, verts, faces, mat, part=name)
        if bevel > 0:
            bl.bevel(ob, bevel, segs, 35.0)
        if cull:
            _cull(ob, cull)
        if any(shift):
            ob.data.transform(Matrix.Translation(Vector(shift)))
        ob["cls"], ob["grp"] = cls, grp
        self.objs.append(ob)
        return ob

    def box(self, name, x0, y0, z0, x1, y1, z1, mat, cls, grp, cull=(), bevel=0.0, segs=1, shift=(0, 0, 0)):
        v = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
        f = [(0, 3, 2, 1), (4, 5, 6, 7), (0, 1, 5, 4), (2, 3, 7, 6), (1, 2, 6, 5), (0, 4, 7, 3)]
        return self.solid(name, v, f, mat, cls, grp, cull, bevel, segs, shift)


def _cull(ob, dirs, tol=0.93):
    """Deletes the faces that look along one of dirs (the hidden ones: a board's inner face, a cap's underside)."""
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    bm.faces.ensure_lookup_table()
    bm.normal_update()
    kill = [f for f in bm.faces if any(f.normal.dot(Vector(d)) > tol for d in dirs)]
    bmesh.ops.delete(bm, geom=kill, context="FACES")
    bm.to_mesh(ob.data)
    bm.free()


def _cull_if(ob, pred):
    bm = bmesh.new()
    bm.from_mesh(ob.data)
    bm.faces.ensure_lookup_table()
    bm.normal_update()
    kill = [f for f in bm.faces if pred(f)]
    bmesh.ops.delete(bm, geom=kill, context="FACES")
    bm.to_mesh(ob.data)
    bm.free()


def ring_solid(k, name, x0, y0, x1, y1, i0, i1, z0, z1, mat, cls, grp, shift, d):
    """A rectangular frame ring (outer rect inset i0, inner rect inset i1) from z0 to z1; its underside is dropped."""
    ox0, oy0, ox1, oy1 = x0 + i0, y0 + i0, x1 - i0, y1 - i0
    ix0, iy0, ix1, iy1 = x0 + i1, y0 + i1, x1 - i1, y1 - i1
    rect = [(ox0, oy0), (ox1, oy0), (ox1, oy1), (ox0, oy1), (ix0, iy0), (ix1, iy0), (ix1, iy1), (ix0, iy1)]
    verts = [(x, y, z0) for x, y in rect] + [(x, y, z1) for x, y in rect]
    faces = []
    for a, b in ((0, 1), (1, 2), (2, 3), (3, 0)):
        faces.append((a, b, b + 8, a + 8))                       # outer wall
        faces.append((a + 4, b + 4, b + 12, a + 12))             # inner wall
        faces.append((a + 8, b + 8, b + 12, a + 12))             # top
        faces.append((a, b, b + 4, a + 4))                       # underside
    return k.solid(name, verts, faces, mat, cls, grp, cull=((0, 0, -d),), shift=shift)


def plate_solid(k, name, cx, cy, z0, d, r_out, r_in, r_dish, h_rim, h_dish, mat, cls, grp, shift):
    """An eight-sided emblem plate rising from z0 towards d (+1 / -1): outer wall, rim, sloped inner wall, a flat dish."""
    n = 8
    ang = [math.pi * 2 * i / n + math.pi / n for i in range(n)]
    ring = lambda r, z: [(cx + math.cos(a) * r, cy + math.sin(a) * r, z) for a in ang]
    zr, zd = z0 + d * h_rim, z0 + d * h_dish
    verts = ring(r_out, z0) + ring(r_out, zr) + ring(r_in, zr) + ring(r_dish, zd) + [(cx, cy, zd)]
    c = len(verts) - 1
    verts.append((cx, cy, z0))
    c0 = len(verts) - 1
    faces = []
    for i in range(n):
        j = (i + 1) % n
        faces.append((j, i, c0))                                   # underside (dropped below)
        faces.append((i, j, j + n, i + n))                         # outer wall
        faces.append((i + n, j + n, j + 2 * n, i + 2 * n))         # rim top
        faces.append((i + 2 * n, j + 2 * n, j + 3 * n, i + 3 * n))  # slope
        faces.append((i + 3 * n, j + 3 * n, c))                    # dish
    return k.solid(name, verts, faces, mat, cls, grp, cull=((0, 0, -d),), shift=shift)


def half_spine(k, name, cx, zc, r_out, r_in, y0, y1, bulge, segs, mat, cls, grp, shift, radial_cull=True):
    """A half round (an arc of `segs` strips, 180 degrees) about the vertical axis (cx, zc), bulging towards -x (bulge '-x', the closed spine) or +z (bulge 'z', the open one)."""
    verts, faces = [], []
    angs = [-math.pi / 2 + math.pi * i / segs for i in range(segs + 1)]

    def pt(r, a, y):
        return (cx - r * math.cos(a), y, zc + r * math.sin(a)) if bulge == "-x" else (cx + r * math.sin(a), y, zc + r * math.cos(a))
    for r in (r_out, r_in):
        for y in (y0, y1):
            for a in angs:
                verts.append(pt(r, a, y))
    m = segs + 1
    o_lo, o_hi, i_lo, i_hi = 0, m, 2 * m, 3 * m
    for i in range(segs):
        faces.append((o_lo + i, o_lo + i + 1, o_hi + i + 1, o_hi + i))        # outer wall
        faces.append((i_lo + i, i_hi + i, i_hi + i + 1, i_lo + i + 1))        # inner wall
        faces.append((o_hi + i, o_hi + i + 1, i_hi + i + 1, i_hi + i))        # top
        faces.append((o_lo + i, i_lo + i, i_lo + i + 1, o_lo + i + 1))        # bottom
    faces.append((o_lo, o_hi, i_hi, i_lo))                                      # the two ends
    faces.append((o_lo + segs, i_lo + segs, i_hi + segs, o_hi + segs))
    ob = k.solid(name, verts, faces, mat, cls, grp, shift=shift)
    if radial_cull:                                                             # the inner wall and the two ends face the book
        mid = (r_out + r_in) / 2
        _cull_if(ob, lambda f: abs(f.normal.y) < 0.3 and _radial(f, cx + shift[0], zc + shift[2]) < mid)
    return ob


def _radial(f, cx, zc):
    """The smallest distance of a face's corners from the arc axis: an outer wall keeps r_out, the inner wall and the two ends reach r_in (a face centre would cut the outer wall of a coarse arc)."""
    return min(math.hypot(v.co.x - cx, v.co.z - zc) for v in f.verts)


# ================================================================ materials
def materials():
    leather = bl.mat("leather", bl.rgb(228, 226, 222), 0.0, 0.55, bump="leather", bscale=1.6, bstr=0.5, bevel=0.1)
    tattered = bl.mat("leather_t", bl.rgb(170, 166, 160), 0.0, 0.85, bump="leather", bscale=3.4, bstr=1.4, bevel=0.05,
                      groove=bl.rgb(60, 56, 52), gamount=0.55)
    metal = bl.mat("metal", bl.rgb(236, 234, 228), 0.65, 0.34, bump="brushed", bscale=2.0, bstr=0.25, bevel=0.1)
    ribbon = bl.mat("ribbon", bl.rgb(232, 232, 232), 0.0, 0.6, bump="grain", bscale=6.0, bstr=0.35, bevel=0.03)
    pages = bl.mat("pages", bl.rgb(246, 236, 208), 0.0, 0.9, bevel=0.03)
    _page_lines(pages)
    return dict(L=leather, T=tattered, M=metal, F=metal, R=ribbon, P=pages)


def _page_lines(m):
    """Fine page-edge lines (a wave texture across the stack, along z) as bump and as a slight darkening."""
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    tc = N.new("ShaderNodeTexCoord")
    wv = N.new("ShaderNodeTexWave")
    wv.wave_type = "BANDS"
    wv.bands_direction = "Z"
    wv.inputs["Scale"].default_value = 1.0
    wv.inputs["Distortion"].default_value = 0.0
    mp = N.new("ShaderNodeMapping")
    mp.inputs["Scale"].default_value = (0.0, 0.0, 5.5)             # bands run with z, the thickness of the stack
    L.new(tc.outputs["Object"], mp.inputs["Vector"])
    L.new(mp.outputs[0], wv.inputs["Vector"])
    bp = N.new("ShaderNodeBump")
    bp.inputs["Strength"].default_value = 0.6
    bp.inputs["Distance"].default_value = 0.2
    L.new(wv.outputs["Fac"], bp.inputs["Height"])
    L.new(bp.outputs["Normal"], bs.inputs["Normal"])
    mx = N.new("ShaderNodeMixRGB")
    mx.inputs[1].default_value = (0.62, 0.55, 0.42, 1)
    mx.inputs[2].default_value = bl.rgb(246, 236, 208)
    L.new(wv.outputs["Fac"], mx.inputs[0])
    L.new(mx.outputs[0], bs.inputs["Base Color"])


# ================================================================ the covers
def cover(k, mats, tag, grp, x0, x1, y0, y1, z_in, d, plate_scale, rosette, shift, caps=True, board_bevel=0.16):
    """One cover (board, frame ring, plateau, corner caps, emblem plate or rosette). d = +1 faces +Z, -1 faces -Z. Returns the overlay numbers."""
    zo = z_in + d * T
    lo, hi = (z_in, zo) if d > 0 else (zo, z_in)
    k.box(tag + "_board", x0, y0, lo, x1, y1, hi, mats["L"], "L", grp, bevel=board_bevel, segs=1, shift=shift)
    ring_solid(k, tag + "_ring", x0, y0, x1, y1, RING_IN, RING_OUT, zo, zo + d * RING_H, mats["F"], "F", grp, shift, d)
    px0, py0, px1, py1 = x0 + RING_OUT, y0 + RING_OUT, x1 - RING_OUT, y1 - RING_OUT
    zp = zo + d * PLAT_H
    k.box(tag + "_plateau", px0, py0, min(zo, zp), px1, py1, max(zo, zp), mats["L"], "L", grp, cull=((0, 0, -d),), shift=shift)
    if caps:
        for ix, sx in ((x0, 1), (x1, -1)):
            for iy, sy in ((y0, 1), (y1, -1)):
                corner_cap(k, tag + "_cap", ix, iy, sx, sy, zo, d, mats["M"], grp, shift)
    cx, cy = (x0 + x1) / 2, (y0 + y1) / 2
    s = plate_scale
    if rosette:
        plate_solid(k, tag + "_rosette", cx, cy, zp, d, 1.45 * s, 1.1 * s, 0.85 * s, 0.22, 0.1, mats["M"], "M", grp, shift)
        dish = zp + d * 0.1
    else:
        plate_solid(k, tag + "_plate", cx, cy, zp, d, 2.3 * s, 1.95 * s, 1.75 * s, 0.34, 0.2, mats["M"], "M", grp, shift)
        dish = zp + d * 0.2
    return dict(z=zp, rect=(px0, py0, px1, py1), cx=cx, cy=cy, dish=dish)


def corner_cap(k, name, ix, iy, sx, sy, zo, d, mat, grp, shift):
    """A metal corner cap: a flat-topped pyramid over the corner (a chamfer without bevel geometry), 1.7 square, 0.3 high, overhanging the board edge a little."""
    ax0, ax1 = sorted((ix - sx * 0.1, ix + sx * 1.7))
    ay0, ay1 = sorted((iy - sy * 0.1, iy + sy * 1.7))
    t = 0.3
    zt = zo + d * 0.3
    v = [(ax0, ay0, zo), (ax1, ay0, zo), (ax1, ay1, zo), (ax0, ay1, zo), (ax0 + t, ay0 + t, zt), (ax1 - t, ay0 + t, zt), (ax1 - t, ay1 - t, zt), (ax0 + t, ay1 - t, zt)]
    f = [(0, 1, 2, 3), (4, 5, 6, 7), (0, 1, 5, 4), (1, 2, 6, 5), (2, 3, 7, 6), (3, 0, 4, 7)]
    k.solid(name, v, f, mat, "M", grp, cull=((0, 0, -d),), shift=shift)


def pages_closed(k, mats, shift):
    """Four staggered slabs between the boards (visible page layers): top, bottom and fore-edge faces, the stack's ledges closed."""
    x0, x1, y0, y1 = 2.9, 13.1, 1.7, 14.3
    zs = np.linspace(BACK_IN, FRONT_IN, 5)
    inset = [0.0, 0.14, 0.14, 0.0]
    for i in range(4):
        e = inset[i]
        k.box("pages_%d" % i, x0, y0 + e, zs[i], x1 - e, y1 - e, zs[i + 1], mats["P"], "P", "cPages",
              cull=((-1, 0, 0),) + (((0, 0, -1),) if i == 0 else ()) + (((0, 0, 1),) if i == 3 else ()), shift=shift)


def pages_half(k, mats, side, xa, xb, shift):
    """The page block of an open half: three slabs behind the board (towards -Z), the reader's side and the fore edge visible."""
    pa = OPEN_CX + 2.2 if side > 0 else xa + 0.3
    pb = xb - 0.3 if side > 0 else OPEN_CX - 2.2
    zt = OPEN_CZ - T
    zs = np.linspace(zt - OPEN_P, zt, 4)
    inset = [0.0, 0.12, 0.0]
    for i in range(3):
        e = inset[i]
        k.box("hp%s_%d" % ("r" if side > 0 else "l", i), pa if side > 0 else pa + e, 1.7 + e, zs[i], pb - e if side > 0 else pb, 14.3 - e, zs[i + 1],
              mats["P"], "P", "oR" if side > 0 else "oL", cull=((0, 0, 1),) if i == 2 else (), shift=shift)
    return pa, pb, zt - OPEN_P


def ribbon(k, mats, shift):
    """A bookmark ribbon hanging out of the bottom of the page block, curling towards the reader, with a forked tail."""
    w = 0.8
    pts = [(7.1, 2.4, 8.0), (7.1, 1.3, 8.15), (7.05, 0.7, 8.3)]                     # x, y, z along the hang
    verts, faces = [], []
    for x, y, z in pts:
        verts += [(x, y, z), (x + w, y, z)]
    for i in range(len(pts) - 1):
        a = i * 2
        faces.append((a, a + 1, a + 3, a + 2))
    verts.append((7.0 + w / 2, 0.2, 8.42))                                           # a pointed tail
    faces.append((4, 5, 6))
    me = bl.obj("ribbon_s", verts, faces, mats["R"], part="ribbon_s")
    me.data.update()
    bl.solidify(me, 0.06, 0.0)
    me["cls"], me["grp"] = "R", "cRibbon"
    if any(shift):
        me.data.transform(Matrix.Translation(Vector(shift)))
    k.objs.append(me)


def build():
    k = Kit()
    mats = materials()
    # ---------------- the closed book
    front = cover(k, mats, "cF", "cF", BX0, BX1, BY0, BY1, FRONT_IN, +1, 1.0, False, (0, 0, 0))
    back = cover(k, mats, "cB", "cB", BX0, BX1, BY0, BY1, BACK_IN, -1, 1.0, True, (0, 0, 0))
    pages_closed(k, mats, (0, 0, 0))
    zs, rs = 8.0, 1.6                                                     # the round spine: centre z, radius
    half_spine(k, "cSpine", BX0, zs, rs, rs - 0.18, BY0, BY1, "-x", 4, mats["L"], "L", "cSpine", (0, 0, 0))
    for i, (ya, yb) in enumerate(((BY0, BY0 + 0.5), (3.65, 4.15), (6.35, 6.85), (9.05, 9.55), (11.75, 12.25), (BY1 - 0.5, BY1))):
        half_spine(k, "cBand%d" % i, BX0, zs, rs + 0.12, rs - 0.1, ya, yb, "-x", 4, mats["M"], "M", "cSpine", (0, 0, 0))
    ribbon(k, mats, (0, 0, 0))
    # ---------------- the open halves, flat, baked beside the closed book
    shift = (KIT_SHIFT, 0, 0)
    xa_r, xb_r = OPEN_CX + 0.3, OPEN_CX + OPEN_W
    xa_l, xb_l = OPEN_CX - OPEN_W, OPEN_CX - 0.3
    z_in = OPEN_CZ - T
    right = cover_half(k, mats, "oR", xa_r, xb_r, z_in, 0.8, False, shift)
    left = cover_half(k, mats, "oL", xa_l, xb_l, z_in, 0.45, True, shift)
    pa_r, pb_r, zp_r = pages_half(k, mats, +1, xa_r, xb_r, shift)
    pa_l, pb_l, zp_l = pages_half(k, mats, -1, xa_l, xb_l, shift)
    zc = OPEN_CZ - 0.25
    half_spine(k, "oSpine", OPEN_CX, zc, 1.15, 0.9, BY0, BY1, "z", 4, mats["L"], "L", "oSpine", shift)
    for i, y in enumerate((3.95, 8.0, 12.05)):
        half_spine(k, "oBand%d" % i, OPEN_CX, zc, 1.27, 1.0, y - 0.25, y + 0.25, "z", 4, mats["M"], "M", "oSpine", shift)
    k.meta.update({
        "closed.front": front, "closed.back": back, "open.right": right, "open.left": left,
        "open.pagesR": (pa_r, pb_r, zp_r), "open.pagesL": (pa_l, pb_l, zp_l)})
    return k, mats


def cover_half(k, mats, grp, xa, xb, z_in, plate_scale, rosette, shift):
    return cover(k, mats, grp, grp, xa, xb, BY0, BY1, z_in, +1, plate_scale, rosette, shift, caps=True, board_bevel=0.14)


# ================================================================ unwrap, bake, export
def unwrap_bake(k, mats, out_dir, write):
    sc = bpy.context.scene
    names = []
    for ob in k.objs:
        ob.data.polygons.foreach_set("use_smooth", [False] * len(ob.data.polygons))
    grps = sorted({o["grp"] for o in k.objs})
    for ob in k.objs:
        me = ob.data
        a = me.attributes.get("cls_id") or me.attributes.new("cls_id", "INT", "FACE")
        a.data.foreach_set("value", [CLS[ob["cls"]]] * len(me.polygons))
        g = me.attributes.get("grp_id") or me.attributes.new("grp_id", "INT", "FACE")
        g.data.foreach_set("value", [grps.index(ob["grp"])] * len(me.polygons))
    low = bl._join(k.objs, "grimoire")
    bl._clean(low)
    low.data.shade_flat()
    bl._select([low], low)
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.uv.smart_project(angle_limit=math.radians(66), island_margin=0.0, area_weight=0.0, scale_to_bounds=False)
    bpy.ops.uv.average_islands_scale()
    bpy.ops.uv.pack_islands(margin=0.004, rotate=True)
    bpy.ops.object.mode_set(mode="OBJECT")
    for o in k.objs:
        o.hide_render = True
    out = {}
    for variant in ("", "_tattered"):
        if variant:                                                       # the same UVs with the worn leather
            for s in low.material_slots:
                if s.material and s.material.name.startswith("leather") and not s.material.name.endswith("_t"):
                    s.material = mats["T"]
        img = bl._image("revamp" + variant, BAKE)
        for s in low.material_slots:
            if not s.material:
                continue
            nt = s.material.node_tree
            n = nt.nodes.new("ShaderNodeTexImage")
            n.image = img
            nt.nodes.active = n
        bl._select([low], low)
        bpy.ops.object.bake(type="COMBINED", margin=6, use_clear=True)
        px = bl._pixels(img, BAKE, ATLAS)
        px[..., 3] = np.where(px[..., 3] > 8, 255.0, 0.0)
        out[variant] = px
        print("baked", variant or "classic-leather", px.shape)
    if write:
        from PIL import Image
        os.makedirs(out_dir["tex"], exist_ok=True)
        for variant, px in out.items():
            Image.fromarray(np.clip(px, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(out_dir["tex"], "revamp_atlas%s.png" % variant), optimize=True)
    return low, grps


def export(low, grps, k, out_dir, write):
    me = low.data
    me.calc_loop_triangles()
    uv = me.uv_layers.active.data
    cls_id = me.attributes["cls_id"].data
    grp_id = me.attributes["grp_id"].data
    byg = {g: [] for g in grps}
    cls_names = {v: n for n, v in CLS.items()}
    for poly in me.polygons:
        li = list(poly.loop_indices)
        if len(li) == 3:
            li.append(li[2])
        elif len(li) > 4:
            raise SystemExit("an n-gon was left in the mesh")
        pts = []
        for l in li:
            v = me.vertices[me.loops[l].vertex_index].co
            pts.append((v.x, v.y, v.z, uv[l].uv.x, 1.0 - uv[l].uv.y))
        byg[grps[grp_id[poly.index].value]].append((cls_names[cls_id[poly.index].value], pts))

    def line(cls, pts, tf):
        c = [tf(Vector(p[:3])) for p in pts]
        return "q %s %s" % (cls, " ".join("%.4f %.4f %.4f %.4f %.4f" % (c[i].x, c[i].y, c[i].z, pts[i][3], pts[i][4]) for i in range(4)))

    def pose(groups, shifted, turns):
        lines = []
        for g in groups:
            ang = turns.get(g, 0.0)
            m = Matrix.Translation(Vector((-KIT_SHIFT if shifted.get(g) else 0.0, 0, 0)))
            r = Matrix.Translation(Vector((OPEN_CX, 0, OPEN_CZ))) @ Matrix.Rotation(ang, 4, "Y") @ Matrix.Translation(Vector((-OPEN_CX, 0, -OPEN_CZ)))
            tf = (lambda p, r=r, m=m: r @ (m @ p))
            for cls, pts in byg[g]:
                lines.append(line(cls, pts, tf))
        return lines

    text = ["# Grimoire revamp mesh, made in Blender by tools/blender/grimoire_revamp.py (bpy %s). Do not edit by hand." % bpy.app.version_string,
            "# q <class L leather|M metal|F frame ring|R ribbon|P pages> then 4 x (x y z u v): model units 0..16, uv 0..1 over revamp_atlas.png"]
    for key, v in k.meta.items():
        flat = []
        for x in (v.values() if isinstance(v, dict) else v):
            flat += list(x) if isinstance(x, (tuple, list)) else [x]
        text.append("meta %s %s" % (key, " ".join("%.4f" % f for f in flat)))
    counts = {}
    closed = pose(["cF", "cB", "cPages", "cSpine", "cRibbon"], {}, {})
    text.append("mesh closed")
    text += closed
    counts["closed"] = len(closed)
    for step, deg in OPEN_DEGREES.items():
        th = math.radians(deg)
        lines = pose(["oR", "oL", "oSpine"], {"oR": 1, "oL": 1, "oSpine": 1}, {"oR": th, "oL": -th})
        text.append("mesh open%d" % step)
        text += lines
        counts["open%d" % step] = len(lines)
    print("quads:", counts, "(open books are drawn double-sided: x2)")
    if write:
        os.makedirs(out_dir["mesh"], exist_ok=True)
        with open(os.path.join(out_dir["mesh"], "revamp_mesh.txt"), "w") as fh:
            fh.write("\n".join(text) + "\n")
    return counts


def main():
    args = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else sys.argv[1:]
    trial = None
    if "--trial" in args:
        trial = args[args.index("--trial") + 1]
    out_dir = {"mesh": os.path.join(ASSETS, "grimoire_revamp"), "tex": os.path.join(ASSETS, "textures", "item", "grimoire_book")}
    if trial:
        out_dir = {"mesh": trial, "tex": trial}
    bl.reset(24)
    k, mats = build()
    low, grps = unwrap_bake(k, mats, out_dir, True)
    export(low, grps, k, out_dir, True)


if __name__ == "__main__":
    main()

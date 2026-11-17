"""forms_legendary_shield: one real 3D model per transformation form of the Legendary Shield (legendary_shield__<form>), built with forms_kit on top of bl_lib.

    python3 -B tools/blender/forms_legendary_shield.py --only leaf --quick      # geometry only: part table + a fast flat-shaded view (no bake)
    python3 -B tools/blender/forms_legendary_shield.py --only leaf              # bake ONE form into the mod's asset folders (1024 px skin)
    python3 -B tools/blender/forms_legendary_shield.py --only leaf --sheet      # also the composite preview docs/heroweapon/form_previews/legendary_shield_<form>.png

Rules this file follows (owner): the shape and the depth come from GEOMETRY (dished bodies, stepped / bevelled / overlapping parts, real leaves with thickness and
camber, sunken and raised features), not from a picture painted on a flat plate; the skin is 1024 px (baked at 2048 and boxed down) with the skin + glow PNG
<= 350 KB; AO and wear are baked from the real geometry. Everything is seeded: the same script gives the same mesh.
Coordinates are the mod's: y = the shield's height (cm; the size class is measured on it), x width, z thickness (+z faces the viewer / the enemy).
"""
import argparse
import json
import math
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import forms_kit as K  # noqa: E402
import bpy  # noqa: E402
from mathutils import Matrix, Vector as V  # noqa: E402
from bl_lib import rgb  # noqa: E402

K.MAX_PNG_KB = 350
TAU = 2 * math.pi
BASE = "legendary_shield"
ROOT = K.ROOT
MANIFEST = os.path.join(HERE, "forms_manifest_legendary_shield.json")
PREV = os.path.join(ROOT, "docs", "heroweapon", "form_previews")
THREADS = 2
SKIN = 1024
BAKE = 2048


# ================================================================ scene + materials
def begin(samples=16):
    sc = K.begin(samples)
    sc.render.threads_mode = "FIXED"
    sc.render.threads = THREADS
    return sc


def S(name, base, metal=0.0, rough=0.5, **kw):
    """K.surface + a viewport colour (for the fast flat-shaded check)."""
    m = K.surface(name, base, metal, rough, **kw)
    m.diffuse_color = base
    return m


def smoothstep(a, b, x):
    return K.smoothstep(a, b, x)


# ================================================================ geometry helpers shared by the forms
def dish(name, outline, front, back, mat, *, ns=10, nb=3, nth=56, part=None, angle=60.0, s0=0.02, ctr=(0.0, 0.0)):
    """A DISHED SOLID BODY of any star-convex outline: outline(theta) -> (x, y) at the rim; front(s, theta) / back(s, theta) -> z of the front / back surface at the
    normalised radius s (0 centre .. 1 rim). Lofted in rings (front centre -> rim -> back), so the shield has real depth, a real boss and a real rim wall.
    s0 > 0.05 makes a RING-shaped body (a hole in the middle: the rings close back on themselves, a socket for a separate piece)."""
    th = [TAU * i / nth for i in range(nth)]
    O = [outline(a) for a in th]
    rings = []
    for k in range(ns + 1):
        s = s0 + (1.0 - s0) * k / ns
        rings.append([V((ctr[0] + s * o[0], ctr[1] + s * o[1], front(s, a))) for o, a in zip(O, th)])
    for k in range(1, nb + 1):
        s = 1.0 - (1.0 - s0) * k / nb
        rings.append([V((ctr[0] + s * o[0], ctr[1] + s * o[1], back(s, a))) for o, a in zip(O, th)])
    hole = s0 > 0.05
    if hole:
        rings.append(list(rings[0]))
    ob = K.bl.loft(name, rings, mat, (not hole, not hole), part or name)
    return K._finish_obj(ob, part or name, True, angle)


def poly_outline(pts, smooth=6):
    """outline(theta) for a closed control polygon (a kite, a heater, a leaf) around the origin: a closed Catmull-Rom curve, sampled into an angle table."""
    n = len(pts)
    dense = []
    for i in range(n):
        p0, p1, p2, p3 = V(pts[(i - 1) % n]), V(pts[i]), V(pts[(i + 1) % n]), V(pts[(i + 2) % n])
        for k in range(smooth):
            t = k / smooth
            dense.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    tab = sorted(((math.atan2(p.y, p.x) % TAU, p.x, p.y) for p in dense))

    def outline(a):
        a = a % TAU
        lo = len(tab) - 1
        for i, (ta, _, _) in enumerate(tab):
            if ta <= a:
                lo = i
        t0, t1 = tab[lo], tab[(lo + 1) % len(tab)]
        span = (t1[0] - t0[0]) % TAU or 1e-9
        f = ((a - t0[0]) % TAU) / span
        return (t0[1] + (t1[1] - t0[1]) * f, t0[2] + (t1[2] - t0[2]) * f)
    return outline


def band(name, pts, section, mat, part=None, scale=None, angle=55.0, up=(0, 0, 1)):
    """A closed swept band along a closed loop of 3D points (rim ropes, collars): section = [(u outward in plane, v along `up`)]."""
    n = len(pts)
    ring_n = len(section)
    verts, faces = [], []
    upv = V(up)
    for i, p in enumerate(pts):
        tan = (V(pts[(i + 1) % n]) - V(pts[i - 1])).normalized()
        out = tan.cross(upv)
        if out.length < 1e-6:
            out = V((1, 0, 0))
        out.normalize()
        s = scale(i / n) if callable(scale) else 1.0
        su, sv = s if isinstance(s, (tuple, list)) else (s, s)
        for (u, v) in section:
            verts.append(V(p) + out * (u * su) + upv * (v * sv))
    for i in range(n):
        j = (i + 1) % n
        for k in range(ring_n):
            k2 = (k + 1) % ring_n
            faces.append((i * ring_n + k, i * ring_n + k2, j * ring_n + k2, j * ring_n + k))
    ob = K.bl.obj(name, verts, faces, mat, part or name)
    return K._finish_obj(ob, part or name, True, angle)


def rope_section(r=1.0, n=6, flat=1.0):
    return [(r * math.cos(TAU * k / n), r * flat * math.sin(TAU * k / n)) for k in range(n)]


def loop_pts(outline, z, n=64, s=1.0, zf=None):
    out = []
    for i in range(n):
        a = TAU * i / n
        o = outline(a)
        out.append(V((s * o[0], s * o[1], zf(a) if zf else z)))
    return out


def back_gear(P, z, ytop=10.0, y0=-9.0, span=14.0, wood=None, strap_m=None, metal=None, rr=None):
    """The arm side of a shield: a handle bar on two posts, a strap with rivets (what the wielder holds). Cheap (about 120 quads); z = the back surface at the centre."""
    rr = rr or K.rng(5)
    wood = wood or P["leather"]
    K.bar("handle", (-9.5, y0, z - 4.0), (9.5, y0, z - 4.0), 1.5, 1.5, P["leather"], part="back_grip", bevel_w=0.2)
    for sx in (-1, 1):
        K.bar("post%d" % sx, (sx * 9.5, y0, z - 4.0), (sx * 11.0, y0, z - 0.6), 1.5, 1.3, metal or P["leather"], part="back_grip")
    sp = [V((-span - 3 + (2 * span + 6) * t, y0 + 12.0 + 7.0 * math.sin(t * math.pi), z - 1.2 - 1.8 * math.sin(t * math.pi))) for t in [i / 10 for i in range(11)]]
    K.ribbon("arm_strap", sp, 4.6, strap_m or P["leather"], facing=(0, 0, -1), thick=0.4, part="back_strap", rng_=rr, edge_wave=0.03)
    K.rivets("strap_rivets", [(V((-span - 2.5, y0 + 12.0, z - 1.3)), (0, 0, -1)), (V((span + 2.5, y0 + 12.0, z - 1.3)), (0, 0, -1)),
                              (V((-span + 3.0, y0 + 14.5, z - 2.4)), (0, 0, -1)), (V((span - 3.0, y0 + 14.5, z - 2.4)), (0, 0, -1))], metal or P["leather"], r=0.85, detail=False, sides=6)



def revolve(name, prof, mat, *, sides=48, squash=1.0, mod=None, part=None, caps=(False, False), angle=60.0):
    """A body of revolution about the y axis (r, h) with an optional per-vertex modulation mod(phi, k, r, h) -> (r, h) (gores, wrinkles, jagged edges); squash flattens z."""
    rings = []
    for k, (r, h) in enumerate(prof):
        ring = []
        for i in range(sides):
            a = TAU * i / sides
            rr_, hh = mod(a, k, r, h) if mod else (r, h)
            rr_ = max(rr_, 1e-3)
            ring.append(V((rr_ * math.cos(a), hh, rr_ * math.sin(a) * squash)))
        rings.append(ring)
    ob = K.bl.loft(name, rings, mat, caps, part or name)
    return K._finish_obj(ob, part or name, True, angle)


def rev_frame(prof, squash, modf=None):
    """(h, phi) -> (point, outward normal) on a body made by revolve(): for placing patches, rivets, gems on its surface."""
    pts = sorted(((h, r) for r, h in prof))

    def P(h, a):
        r = K.interp(pts, h)
        if modf:
            r, h = modf(a, 0, r, h)
        return V((r * math.cos(a), h, r * math.sin(a) * squash))

    def F(h, a):
        e = 0.05
        p = P(h, a)
        du = P(h + e, a) - P(h - e, a)
        dv = P(h, a + e) - P(h, a - e)
        n = dv.cross(du)
        if n.dot(p - V((0, p.y, 0))) < 0:
            n = -n
        return p, n.normalized()
    return F


def on_surface(ob, F, h, a, up=(0, 1, 0)):
    """Moves an object built at the origin facing +z onto the surface of a revolved body at (h, phi)."""
    p, n = F(h, a)
    K.xf(ob, K.frame(p, n, up))
    return ob

# ================================================================ fast flat-shaded view (Workbench), to judge the geometry before a 10 minute bake
def quick(path, view="front", size=(760, 760), samples=10):
    """A fast matte Cycles view of the raw geometry (every material swapped for a plain diffuse of its viewport colour, AO-ish studio light): judge shape and depth before the bake."""
    import numpy as np
    sc = bpy.context.scene
    old = (sc.camera, sc.render.resolution_x, sc.render.resolution_y, sc.cycles.samples, sc.cycles.use_denoising)
    obs = [o for o in sc.collection.objects if o.type == "MESH" and not o.get("detail")]
    allp = np.array([tuple(v.co) for o in obs for v in o.data.vertices])
    lo, hi = allp.min(axis=0), allp.max(axis=0)
    ctr = V(((lo + hi) / 2).tolist())
    ext = max(hi[0] - lo[0], hi[1] - lo[1]) * 1.12
    swaps = []
    cache = {}
    for o in obs:
        for sl in o.material_slots:
            m = sl.material
            if m is None:
                continue
            key = m.name
            if key not in cache:
                q = bpy.data.materials.new("q_" + key)
                q.use_nodes = True
                bs = q.node_tree.nodes["Principled BSDF"]
                c = tuple(m.diffuse_color)
                bs.inputs["Base Color"].default_value = c
                bs.inputs["Roughness"].default_value = 0.55
                if m.get("glow"):
                    bs.inputs["Emission Color"].default_value = c
                    bs.inputs["Emission Strength"].default_value = 0.6
                cache[key] = q
            swaps.append((sl, m))
            sl.material = cache[key]
    yaw, pit = {"front": (0, 0), "three_quarter": (-38, 20), "side": (-90, 0), "back": (180, 0), "tq2": (40, 25)}[view]
    cd = bpy.data.cameras.new("qcam")
    cd.type = "ORTHO"
    cd.ortho_scale = ext
    cam = bpy.data.objects.new("qcam", cd)
    sc.collection.objects.link(cam)
    d = V((math.sin(math.radians(yaw)) * math.cos(math.radians(pit)), math.sin(math.radians(pit)), math.cos(math.radians(yaw)) * math.cos(math.radians(pit))))
    cpos = ctr + d * 400.0
    cam.matrix_world = K.frame(cpos, cpos - ctr, (0, 1, 0))
    sc.camera = cam
    sc.render.resolution_x, sc.render.resolution_y = size
    sc.cycles.samples = samples
    sc.cycles.use_denoising = True
    try:
        sc.cycles.denoiser = "OPENIMAGEDENOISE"
    except Exception:
        pass
    sc.view_settings.view_transform = "Standard"
    sc.render.filepath = path
    sc.render.image_settings.file_format = "PNG"
    bpy.ops.render.render(write_still=True)
    bpy.data.objects.remove(cam)
    for sl, m in swaps:
        sl.material = m
    sc.camera, sc.render.resolution_x, sc.render.resolution_y, sc.cycles.samples, sc.cycles.use_denoising = old


# ================================================================ bake: texel-density weighted unwrap, 1024 px skin
WEIGHTS = {}


def _weight_uvs():
    """Called inside bl.finish right before the island packing (edit mode): scales the UV islands of each part by sqrt(weight), so the faces that are looked at
    (the front, the hero parts) get more skin than the ones that are not (the back of the shield, hidden undersides). Per part, about one fixed point, so the islands stay whole."""
    import numpy as np
    ob = bpy.context.view_layer.objects.active
    bpy.ops.object.mode_set(mode="OBJECT")
    me = ob.data
    names = []
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail") and o.get("part") is not None and o["part"] not in names:
            names.append(o["part"])
    pid = np.zeros(len(me.polygons), np.int32)
    me.attributes["part_id"].data.foreach_get("value", pid)
    uvl = me.uv_layers.active.data
    uv = np.zeros(len(me.loops) * 2, np.float32)
    uvl.foreach_get("uv", uv)
    uv = uv.reshape(-1, 2)
    lp = np.zeros(len(me.loops), np.int32)
    for p in me.polygons:
        lp[p.loop_start:p.loop_start + p.loop_total] = pid[p.index]
    w = np.ones(len(names), np.float32)
    for i, n in enumerate(names):
        for key, val in WEIGHTS.items():
            if n == key or n.startswith(key):
                w[i] = val
    f = np.sqrt(w[lp])[:, None]
    uv = (uv - 0.5) * f + 0.5
    uvl.foreach_set("uv", uv.ravel())
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")


class _UVOps:
    def __getattr__(self, n):
        if n == "pack_islands":
            def packed(**kw):
                _weight_uvs()
                return bpy.ops.uv.pack_islands(**kw)
            return packed
        return getattr(bpy.ops.uv, n)


class _Ops:
    uv = _UVOps()

    def __getattr__(self, n):
        return getattr(bpy.ops, n)


class _BpyShim:
    ops = _Ops()

    def __getattr__(self, n):
        return getattr(bpy, n)


def finish(mesh_id, glow=("FFFFFF", 0.12, 0.45, 1.0), weights=None):
    WEIGHTS.clear()
    WEIGHTS.update(weights or {})
    K.bl.bpy = _BpyShim()
    try:
        r = K.finish_form(mesh_id, BASE, glow=glow, bake=BAKE, skin=SKIN, cage=0.6)
    finally:
        K.bl.bpy = bpy
    trim(mesh_id)
    return r


def trim(mesh_id, limit_kb=345.0):
    """Skin + glow must stay under the PNG limit: if the packed skin is over it, re-quantise the (already palette) skin to fewer colours until it fits."""
    from PIL import Image
    d = os.path.join(K.ASSETS, "textures", "entity", "weapon") if hasattr(K, "ASSETS") else None
    if not d or not os.path.isdir(d):
        d = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "entity", "weapon")
    sk, gl = os.path.join(d, mesh_id + ".png"), os.path.join(d, mesh_id + "_glow.png")
    for cols in (200, 160, 128, 96):
        tot = (os.path.getsize(sk) + (os.path.getsize(gl) if os.path.exists(gl) else 0)) / 1024.0
        if tot <= limit_kb:
            break
        im = Image.open(sk).convert("RGBA")
        a = im.getchannel("A")
        q = im.convert("RGB").quantize(colors=cols - 1, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
        import numpy as np
        idx = np.asarray(q).copy()
        idx[np.asarray(a) < 128] = cols - 1
        pal = q.getpalette()[: (cols - 1) * 3]
        pal += [0] * ((cols - 1) * 3 - len(pal)) + [0, 0, 0]
        out = Image.fromarray(idx, "P")
        out.putpalette(pal)
        out.save(sk, optimize=True, transparency=bytes([255] * (cols - 1) + [0]))
    print("[trim] %s skin+glow %.1f KB" % (mesh_id, (os.path.getsize(sk) + (os.path.getsize(gl) if os.path.exists(gl) else 0)) / 1024.0))


# ================================================================ composite preview sheet (software preview of the real game files)
def sheet(mesh_id, form, y0=-34.0, y1=36.0, focus_a=None, focus_b=None):
    sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
    import preview_mesh as pm
    from PIL import Image, ImageDraw
    tmp = os.path.join(PREV, "_tmp_" + form)
    os.makedirs(tmp, exist_ok=True)
    tiles = []
    for tag, views, focus in (("full", ("profile", "three_quarter", "back"), None), ("close", ("profile", "three_quarter", "edge"), focus_a or (-8.0, 22.0))):
        for v in views:
            p = os.path.join(tmp, "%s_%s.png" % (tag, v))
            pm.render_preview(mesh_id, None, p, views=(v,), panel=(560, 560), ss=2, normals="vertex", focus=focus, label=False)
            tiles.append((tag + " " + v, Image.open(p).convert("RGB")))
    w, h = tiles[0][1].size
    out = Image.new("RGB", (w * 3, h * 2), (40, 44, 56))
    d = ImageDraw.Draw(out)
    for i, (n, im) in enumerate(tiles):
        out.paste(im, ((i % 3) * w, (i // 3) * h))
        d.text(((i % 3) * w + 6, (i // 3) * h + 4), n, fill=(255, 255, 255))
    path = os.path.join(PREV, "legendary_shield_%s.png" % form)
    out.save(path)
    for f in os.listdir(tmp):
        os.remove(os.path.join(tmp, f))
    os.rmdir(tmp)
    return path


# ================================================================ driver
BUILDERS = {}


def run(only, quick_only, do_sheet, samples, views):
    entries = {}
    for key in only:
        fn, mesh_id, form, kw = BUILDERS[key]
        t0 = time.time()
        begin(samples)
        fn()
        print("   parts:", K.quad_table(16))
        print("[%s] geometry: ~%d body quads in %.1f s" % (mesh_id, K.quad_estimate(), time.time() - t0))
        if views:
            os.makedirs(views, exist_ok=True)
            for v in ("front", "three_quarter", "side"):
                quick(os.path.join(views, "%s_%s.png" % (key, v)), v)
        if quick_only:
            continue
        finish(mesh_id, glow=kw.get("glow", ("FFFFFF", 0.12, 0.45, 1.0)), weights=kw.get("weights"))
        entries[mesh_id] = {"weapon": BASE, "form": form, "class": "shield"}
        K.write_manifest(MANIFEST, entries)
        if do_sheet:
            print("sheet:", sheet(mesh_id, form, focus_a=kw.get("focus")))


def main(argv):
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--views", help="DIR: write fast flat-shaded views of the geometry there")
    ap.add_argument("--sheet", action="store_true")
    ap.add_argument("--samples", type=int, default=16)
    ap.add_argument("--threads", type=int, default=2)
    a = ap.parse_args(argv)
    global THREADS
    THREADS = a.threads
    run(a.only.split(","), a.quick, a.sheet, a.samples, a.views)


# FORMS_BEGIN
# ================================================================ 1. LEAF SHIELD
def leaf(name, base, dirn, length, width, mat, *, rows=12, serrate=0.08, curl=0.0, droop=0.0, bend=(0, 0, 0), camber=1.8, fold=0.0, thick=0.5, part="leaves", seed=0, facing=(0, 0, 1)):
    """A real leaf: a solid blade with thickness, a raised rib, a cupped section (edges curl toward the viewer by `camber`), serrated edge, curl and droop."""
    L, R, C = K.leaf_rows(base, dirn, length, width, bend=bend, curl=curl, rows=rows, serrate=serrate, seed=seed, droop=droop, facing=facing)
    d = V(dirn).normalized()
    ac = d.cross(V(facing)).normalized()
    hwl = width * 0.5

    def prof(t):
        return math.sin(math.pi * min(1.0, t ** 0.75)) ** 0.7

    def wob(q, t):
        i = min(rows, max(0, int(round(t * rows))))
        lat = (q - C[i]).dot(ac)
        u = max(-1.25, min(1.25, lat / (hwl * prof(i / rows) + 0.5)))
        return V(facing) * (camber * u * u - fold * (1.0 - min(1.0, abs(u))) ** 2) * (0.35 + 0.65 * math.sin(math.pi * min(1.0, 0.15 + 0.85 * i / rows)))
    return K.strip(name, list(zip(L, R)), mat, (-1.0, -0.45, 0.0, 0.45, 1.0), (0.0, 0.85, 1.0, 0.85, 0.0), thick, part=part, up=facing, wobble=wob, angle=62)


def leaf_shield():
    P = {}
    P["leaf"] = S("leaf", rgb(70, 170, 52), 0.0, 0.46, tramp=[(0.0, rgb(24, 90, 34)), (0.45, rgb(66, 168, 50)), (1.0, rgb(150, 205, 70))], feather=(11.0, 2.3, 0.4, 0.9), mottle=0.8, mottle_color=rgb(30, 110, 40),
                  bump="leather", bump_scale=0.35, bump_strength=0.3, wear=0.6, wear_color=rgb(206, 196, 84), wear_metal=0.0, dirt=0.6, dirt_color=(0.01, 0.03, 0.01, 1), fine=0.5, seed=71, bevel=0.1, ao_dist=3.0, sss=0.15)
    P["leaf_dark"] = S("leaf_dark", rgb(30, 110, 60), 0.0, 0.5, tramp=[(0.0, rgb(14, 60, 36)), (0.5, rgb(32, 112, 62)), (1.0, rgb(92, 160, 80))], feather=(9.0, 4.0, 0.55, 0.8), mottle=0.9, mottle_color=rgb(16, 70, 50),
                       bump="leather", bump_scale=0.4, bump_strength=0.3, wear=0.5, wear_color=rgb(160, 170, 70), wear_metal=0.0, dirt=0.7, dirt_color=(0.005, 0.02, 0.01, 1), seed=72, bevel=0.1, ao_dist=3.0)
    P["leaf_gold"] = S("leaf_gold", rgb(210, 170, 50), 0.0, 0.5, tramp=[(0.0, rgb(140, 80, 24)), (0.5, rgb(214, 160, 44)), (1.0, rgb(230, 206, 90))], feather=(11.0, 5.0, 0.5, 0.9), mottle=0.8, mottle_color=rgb(150, 60, 20),
                       bump="leather", bump_scale=0.4, bump_strength=0.3, wear=0.7, wear_color=rgb(120, 80, 30), wear_metal=0.0, dirt=0.6, seed=73, bevel=0.1, ao_dist=3.0)
    P["rib"] = S("rib", rgb(186, 214, 100), 0.0, 0.42, bump="grain", bump_scale=1.2, bump_strength=0.3, mottle=0.7, mottle_color=rgb(110, 160, 60), wear=0.5, wear_color=rgb(236, 240, 160), wear_metal=0.0, dirt=0.6,
                 dirt_color=(0.01, 0.03, 0.01, 1), seed=74, bevel=0.1)
    P["bark"] = S("bark", rgb(92, 60, 34), 0.0, 0.7, bump="grain", bump_scale=1.6, bump_strength=0.8, groove=rgb(26, 14, 8), groove_amount=0.8, mottle=0.9, mottle_color=rgb(60, 70, 36), wear=0.6, wear_color=rgb(150, 112, 70), wear_metal=0.0,
                  dirt=0.9, fine=0.7, seed=75, bevel=0.1, ao_dist=2.0)
    P["vine"] = S("vine", rgb(60, 110, 40), 0.0, 0.55, bump="grain", bump_scale=1.4, bump_strength=0.5, mottle=0.9, mottle_color=rgb(30, 70, 30), wear=0.5, wear_color=rgb(170, 190, 90), wear_metal=0.0, dirt=0.7, seed=76, bevel=0.05)
    P["gold"] = S("leaf_gold_metal", rgb(214, 170, 70), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 232, 160), dirt=0.9, dirt_color=(0.02, 0.03, 0.01, 1), scratches=0.7, bump="hammer", bump_scale=0.6, bump_strength=0.2, mottle=0.8,
                  mottle_color=rgb(90, 110, 40), fine=0.5, seed=77, bevel=0.16)
    P["gem"] = S("emerald", rgb(40, 220, 90), 0.0, 0.12, emit=rgb(60, 255, 110), emit_strength=0.7, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(190, 255, 150), seed=78, bevel=0.0)
    P["berry"] = S("berry", rgb(200, 24, 40), 0.0, 0.22, mottle=0.9, mottle_color=rgb(110, 8, 28), bump="leather", bump_scale=0.7, bump_strength=0.2, wear=0.4, wear_color=rgb(255, 150, 150), wear_metal=0.0, seed=79, bevel=0.0, rim=(rgb(255, 170, 170), 0.5))
    P["petal"] = S("petal", rgb(250, 240, 220), 0.0, 0.5, tramp=[(0.0, rgb(250, 214, 120)), (0.4, rgb(255, 246, 232)), (1.0, rgb(255, 196, 214))], feather=(7.0, 3.0, 0.2, 0.3), seed=80, bevel=0.0, wear=0.3, wear_color=rgb(255, 255, 250), wear_metal=0.0)
    P["pollen"] = S("pollen", rgb(255, 235, 110), 0.0, 0.3, emit=rgb(255, 235, 120), emit_strength=0.8, emit_flicker=0.4, seed=81, bevel=0.0)
    P["mote"] = S("mote", rgb(160, 255, 140), 0.0, 0.2, emit=rgb(150, 255, 130), emit_strength=0.9, emit_flicker=0.5, seed=82, bevel=0.0)
    P["leather"] = S("leaf_leather", rgb(84, 58, 32), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(26, 14, 6), groove_amount=0.8, wear=0.8, wear_color=rgb(150, 110, 70), wear_metal=0.0, dirt=0.9,
                     mottle=0.8, mottle_color=rgb(46, 26, 12), seed=83, bevel=0.0)
    rr = K.rng(9101)
    Y0, Y1 = -26.0, 33.0
    NY, NC, NBC, PITCH = 48, 12, 3, 3.75
    FQ, SLANT = 11.0, 2.3

    def span(y):
        return (y - Y0) / (Y1 - Y0)

    def cx(y):
        t = span(y)
        return 3.2 * math.sin(t * math.pi * 0.95) - 1.6 * t * t

    def hw0(y, side):                               # nominal half width (no teeth)
        t = min(max(span(y), 0.0), 1.0)
        w = 27.0 * math.sin(math.pi * min(1.0, t ** 0.7)) ** 0.8 * smoothstep(0.0, 0.3, t) ** 0.7
        return max(w * (1.08 if side < 0 else 0.93), 0.7)

    def phase(u, y):
        return (y - Y0 - 7.0 - 12.0 * abs(u) ** 1.15) / 5.2

    def ztop(u, y, puff=True):                      # the cupped, ridged, quilted blade the wielder holds up
        t = min(max(span(y), 0.0), 1.0)
        env = math.sin(math.pi * min(1.0, 0.08 + 0.92 * t)) ** 0.55
        au = abs(u)
        z = 10.0 * env * (1.0 - 0.92 * au ** 1.5) + 1.3 * math.exp(-(u / 0.075) ** 2) * env
        z -= 9.0 * max(0.0, t - 0.5) ** 2 * 2.0
        if puff:
            z += 1.15 * (0.5 - 0.5 * math.cos(TAU * phase(u, y))) * smoothstep(0.07, 0.22, au) * (1.0 - smoothstep(0.82, 1.0, au)) * env
        return z + 0.9 * K.noise3((u * 20.0, y, 0), 0.07, 3, 2)

    import bmesh
    bm = bmesh.new()
    top, back, fuv = [], [], []
    for j in range(NY + 1):
        y = Y0 + (Y1 - Y0) * j / NY
        row = []
        for c in range(NC + 1):
            u = -1.0 + 2.0 * c / NC
            side = -1 if u < 0 else 1
            w = hw0(y, side)
            if abs(u) > 0.999:                       # the serrated margin: a saw tooth every PITCH, left and right out of step; one tooth torn off the right
                ph = ((j + (1 if side > 0 else 0)) % 3) / 3.0
                dep = min(1.8, 0.075 * w + 0.15) * (1.0 - ph) * (3.0 if (side > 0 and 25 <= j <= 27) else 1.0)
                w = max(0.7, w - dep)
            row.append(bm.verts.new((cx(y) + u * w, y, ztop(u, y))))
            fuv.append(((((y - Y0 - 7.0) / 5.2) + 0.25) / FQ, u, 1.0))
        top.append(row)
    for j in range(NY + 1):
        y = Y0 + (Y1 - Y0) * j / NY
        row = []
        for k in range(NBC + 1):
            u = -1.0 + 2.0 * k / NBC
            if k in (0, NBC):
                row.append(None)
                continue
            w = 0.5 * (hw0(y, -1) + hw0(y, 1))
            row.append(bm.verts.new((cx(y) + u * w, y, ztop(u, y, puff=False) - 1.7)))
            fuv.append((0.0, 0.0, 1.0))
        back.append(row)
    for j in range(NY + 1):                           # the back boundary sits right under the top boundary
        y = Y0 + (Y1 - Y0) * j / NY
        for k, c in ((0, 0), (NBC, NC)):
            q = top[j][c].co
            back[j][k] = bm.verts.new((q.x, q.y, q.z - 1.7))
            fuv.append((0.0, 0.0, 1.0))
    for j in range(NY):
        for c in range(NC):
            bm.faces.new((top[j][c], top[j][c + 1], top[j + 1][c + 1], top[j + 1][c]))
        for k in range(NBC):
            bm.faces.new((back[j][k], back[j + 1][k], back[j + 1][k + 1], back[j][k + 1]))
        bm.faces.new((top[j][0], back[j][0], back[j + 1][0], top[j + 1][0]))
        bm.faces.new((top[j][NC], top[j + 1][NC], back[j + 1][NBC], back[j][NBC]))
    bm.faces.new(top[0] + back[0][::-1])
    bm.faces.new(top[NY][::-1] + back[NY])
    ob = K.mk_bm("main_leaf", bm, P["leaf"], "main_leaf", False, True, 62.0)
    at = ob.data.attributes.new("fuv", "FLOAT_VECTOR", "POINT")
    import numpy as np
    at.data.foreach_set("vector", np.asarray(fuv, np.float32).ravel())

    def zl(x, y, lift=0.0):                          # surface height at world (x, y) of the main leaf (for things that sit on it)
        t = cx(y)
        u = max(-1.0, min(1.0, (x - t) / hw0(y, 1 if x > t else -1)))
        return ztop(u, y, puff=False) + lift
    # ---- rib and side veins: raised tubes that follow the crown and cast their own shade
    mid = [V((cx(y), y, ztop(0.0, y, puff=False) + 0.9)) for y in [Y0 + 1.5 + i * 4.9 for i in range(13)]]
    K.tapered("midrib", mid, 2.0, 0.3, P["rib"], sides=6, sub=2, part="veins")
    for i in range(9):
        for side in (-1, 1):
            pts = []
            for u in (0.0, 0.3, 0.62, 0.9):
                yy = Y0 + 7.0 + 5.2 * i + 12.0 * u ** 1.15
                pts.append(V((cx(yy) + side * u * hw0(yy, side), yy, ztop(side * u, yy, puff=False) + 0.9 + 1.0 * (0.5 - 0.5 * math.cos(TAU * phase(u, yy))) * 0.0)))
            K.tapered("vein%d_%d" % (i, side), pts, 0.78 - 0.04 * i, 0.14, P["rib"], sides=4, sub=1, part="veins")
    # ---- the leaf-fan behind: layered, cupped, different lengths and tilts, with a gold autumn leaf and a dark one
    pivot = V((-1.0, -21.0, 0.0))
    fan = [(15, 33, 15.0, "leaf_dark"), (36, 40, 17.0, "leaf"), (56, 44, 18.0, "leaf_dark"), (72, 47, 18.5, "leaf"), (90, 41, 17.0, "leaf_gold"), (108, 46, 18.0, "leaf_dark"), (124, 43, 17.5, "leaf"), (142, 40, 16.0, "leaf_dark"), (163, 32, 14.0, "leaf")]
    for i, (ang, ln, wd, m) in enumerate(fan):
        a = math.radians(ang + rr.uniform(-3, 3))
        d = V((math.cos(a), math.sin(a), 0.0))
        side = V((-d.y, d.x, 0.0))
        leaf("fan%d" % i, pivot + V((0, 0, -2.0 - 0.55 * i + 2.0 * (i % 2))), d, ln * rr.uniform(0.93, 1.07), wd, P[m], rows=9, curl=-rr.uniform(1.0, 4.0), droop=-rr.uniform(0.5, 3.0), bend=side * rr.uniform(-5.0, 5.0), camber=3.8,
             fold=2.0, seed=100 + i, part="fan_leaves")
    for i, (ang, ln, wd) in enumerate(((198, 15, 8), (232, 18, 9), (306, 17, 8.5), (342, 14, 7.5))):       # leaves at the foot, round the stalk
        a = math.radians(ang)
        d = V((math.cos(a), math.sin(a), 0.0))
        leaf("foot%d" % i, pivot + V((0, -3, -3.0 - 0.5 * i)), d, ln, wd, P["leaf" if i % 2 else "leaf_dark"], rows=8, camber=1.5, fold=0.5, seed=140 + i, part="foot_leaves", curl=-1.0, droop=-1.0)
    # ---- stalk: a knotted, tapering bark branch, wrapped in a vine, with sprouts
    stalk = [(0.8, -22.0, 0.0), (1.4, -28.0, -0.8), (-1.0, -33.0, -1.4), (-4.8, -37.0, -1.4)]
    K.wood_branch("stalk", stalk, 2.5, 0.9, P["bark"], seed=11, knots=3, part="stalk")
    K.vine("stalk_vine", [(1.0, -22.0, 2.2), (-1.4, -26.5, 1.4), (2.6, -30.5, 0.6), (-2.2, -34.0, 0.2)], 0.55, P["vine"], leaves=3, leaf_mat=P["leaf"], rng_=rr, leaf_len=3.4, part="vines")
    # ---- boss and gem at the heart of the leaf: a bark collar with claws round the emerald
    gx, gy = cx(-6.0) + 0.5, -6.0
    gz = zl(gx, gy) + 0.2
    collar = K.bl.lathe("collar", [(7.4, -0.8), (7.6, 0.6), (6.8, 2.0), (5.8, 2.9), (5.0, 2.5), (4.6, 1.8)], P["bark"], sides=20, axis="z", caps=(False, False), part="collar")
    K._finish_obj(collar, "collar", True, 50.0)
    K.bl.move(collar, (gx, gy, gz))
    for k in range(6):                              # bark knuckles round the collar
        a = TAU * k / 6 + rr.uniform(-0.1, 0.1)
        K.blob("knuckle%d" % k, (gx + 7.3 * math.cos(a), gy + 7.3 * math.sin(a), gz + 0.9), (1.9, 1.9, 1.5), P["bark"], seed=k, lump=0.2, rings=4, segs=6, part="collar")
    K.gem_setting("emerald", (gx, gy, gz + 2.4), 3.9, P["gem"], P["gold"], normal=(0, 0, 1), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
    # ---- vines winding over the face with small leaves and berries
    def vine_path(x0, y_a, y_b, amp, ph):
        out = []
        for i in range(7):
            t = i / 6
            yy = y_a + (y_b - y_a) * t
            xx = cx(yy) + amp * math.sin(t * 5.0 + ph) * (0.4 + 0.6 * math.sin(math.pi * t)) + x0
            out.append((xx, yy, zl(xx, yy) + 1.0))
        return out
    K.vine("vineA", vine_path(-7.0, -16.0, 24.0, 8.0, 0.0), 0.7, P["vine"], leaves=4, leaf_mat=P["leaf_dark"], rng_=rr, leaf_len=4.2, part="vines")
    K.vine("vineB", vine_path(8.0, -10.0, 16.0, 7.0, 2.0), 0.6, P["vine"], leaves=3, leaf_mat=P["leaf_gold"], rng_=rr, leaf_len=3.6, part="vines")
    for i, (x, y) in enumerate(((-10.5, 12.0), (-8.0, 14.4), (-12.0, 15.0), (10.0, 5.0), (12.5, 7.0))):    # sweet berries in two clusters, with calyx stems
        z = zl(x, y)
        K.blob("berry%d" % i, (x, y, z + 1.6), (1.6, 1.55, 1.5), P["berry"], seed=i, lump=0.06, rings=5, segs=7, part="berries")
        K.tapered("stem%d" % i, [V((x, y, z + 3.0)), V((x + 0.3, y + 0.5, z + 3.9)), V((x - 1.0, y + 1.2, z + 2.2))], 0.18, 0.1, P["vine"], sides=4, sub=1, part="berries")
    # ---- two blossoms with real petals and a pollen heart
    for fi, (fx, fy, ang0) in enumerate(((7.0, 21.0, 0.3), (-9.0, -16.0, 1.0))):
        fz = zl(fx, fy) + 1.6
        for k in range(5):
            a = ang0 + TAU * k / 5
            d = V((math.cos(a), math.sin(a), 0.35))
            leaf("petal%d_%d" % (fi, k), V((fx, fy, fz)), d, 5.0, 2.8, P["petal"], rows=3, serrate=0.0, camber=0.9, thick=0.15, part="blossom", facing=(0, 0, 1))
        K.blob("pollen%d" % fi, (fx, fy, fz + 0.6), (1.1, 1.1, 0.9), P["pollen"], seed=fi, lump=0.1, rings=5, segs=7, part="blossom")
    # ---- drifting healing spores
    K.shard_cloud("mote", (0, 4, 8), (36.0, 38.0, 6.0), 7, (0.5, 0.95), P["mote"], rr, avoid=lambda p: abs(p.x) < 31 and -27 < p.y < 34 and p.z < 11.5, part="spores", stretch=1.4, max_count=7, sides=4)
    # ---- the arm side: a bark brace across the back and the strap
    K.wood_branch("brace", [(-15.0, -10.0, -2.8), (-5.0, -8.2, -3.6), (6.0, -8.5, -3.6), (16.0, -11.0, -2.8)], 1.7, 1.5, P["bark"], seed=14, knots=2, part="brace")
    back_gear(P, -3.6, y0=-9.0, rr=rr)
    return K.quad_estimate()


BUILDERS["leaf"] = (leaf_shield, "legendary_shield__leaf_shield", "leaf_shield", {"weights": {"back": 0.35, "brace": 0.5, "fan_leaves": 0.8}, "glow": ("FFFFFF", 0.12, 0.45, 1.0), "focus": (-14.0, 22.0)})

# ================================================================ 2. ORANGE BALLOON SHIELD
def balloon_shield():
    P = {}
    P["rubber"] = S("balloon", rgb(255, 104, 14), 0.0, 0.16, bump="leather", bump_scale=0.5, bump_strength=0.12, mottle=0.7, mottle_color=rgb(255, 160, 40), wear=0.35, wear_color=rgb(255, 205, 120), wear_metal=0.0,
                    dirt=0.5, dirt_color=(0.35, 0.06, 0.0, 1), seed=91, bevel=0.0, ao_dist=4.0, rim=(rgb(255, 200, 110), 0.45), sss=0.25)
    P["rubber_pale"] = S("balloon_pale", rgb(255, 176, 70), 0.0, 0.2, bump="leather", bump_scale=0.6, bump_strength=0.2, mottle=0.6, mottle_color=rgb(255, 120, 30), wear=0.4, wear_color=rgb(255, 230, 170), wear_metal=0.0,
                         dirt=0.5, dirt_color=(0.3, 0.07, 0.0, 1), seed=92, bevel=0.0, rim=(rgb(255, 220, 150), 0.4))
    P["tape"] = S("seam_tape", rgb(196, 62, 10), 0.0, 0.3, bump="cross", bump_scale=0.9, bump_strength=0.4, wear=0.5, wear_color=rgb(240, 150, 70), wear_metal=0.0, dirt=0.7, dirt_color=(0.1, 0.02, 0.0, 1), seed=93, bevel=0.0)
    P["steel"] = S("balloon_steel", rgb(196, 202, 214), 1.0, 0.26, wear=1.0, wear_color=rgb(250, 252, 255), dirt=0.9, dirt_color=(0.02, 0.02, 0.03, 1), scratches=0.9, bump="brushed", bump_scale=0.8, bump_strength=0.12, mottle=0.7,
                   mottle_color=rgb(120, 130, 150), fine=0.5, seed=94, bevel=0.16, ao_dist=2.0)
    P["gold"] = S("balloon_gold", rgb(226, 170, 60), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 232, 160), dirt=0.9, dirt_color=(0.05, 0.03, 0.0, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.2, mottle=0.8,
                  mottle_color=rgb(120, 70, 20), fine=0.5, seed=95, bevel=0.16)
    P["gem"] = S("balloon_gem", rgb(40, 220, 90), 0.0, 0.12, emit=rgb(60, 255, 110), emit_strength=0.7, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(190, 255, 150), seed=96, bevel=0.0)
    P["string"] = S("string", rgb(228, 214, 190), 0.0, 0.7, bump="wrap", bump_strength=0.8, groove=rgb(90, 70, 50), groove_amount=0.8, dirt=0.6, seed=97, bevel=0.0)
    P["ribbon"] = S("ribbon", rgb(250, 196, 40), 0.0, 0.45, bump="cross", bump_scale=1.2, bump_strength=0.35, wear=0.5, wear_color=rgb(255, 235, 150), wear_metal=0.0, dirt=0.7, dirt_color=(0.1, 0.05, 0.0, 1), seed=98, bevel=0.0)
    P["leather"] = S("balloon_leather", rgb(120, 66, 30), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(30, 14, 6), groove_amount=0.8, wear=0.8, wear_color=rgb(190, 130, 80), wear_metal=0.0, dirt=0.9,
                     mottle=0.8, mottle_color=rgb(60, 30, 14), seed=99, bevel=0.0)
    P["thread"] = S("thread", rgb(250, 240, 220), 0.0, 0.6, bump="wrap", bump_strength=0.5, seed=100, bevel=0.0)
    P["mini"] = S("mini_balloon", rgb(255, 150, 30), 0.0, 0.14, mottle=0.6, mottle_color=rgb(255, 210, 90), rim=(rgb(255, 230, 170), 0.5), seed=101, bevel=0.0, wear=0.3, wear_color=rgb(255, 240, 200), wear_metal=0.0)
    rr = K.rng(2202)
    SQ = 0.46
    prof = [(1.9, -32.0), (2.3, -30.2), (3.2, -28.0), (5.2, -25.6), (9.4, -22.0), (15.2, -17.0), (21.0, -10.5), (25.6, -3.0), (27.6, 4.0), (27.2, 11.0), (24.4, 18.0), (19.4, 24.0), (13.0, 28.6), (6.4, 31.4), (0.01, 32.4)]

    def mod(a, k, r, h):                              # eight gores: each panel is pillowed between its seams; the neck is gathered into wrinkles
        fac = K.smoothstep(-19.0, -29.0, h)
        gore = 1.0 + 0.045 * math.cos(8.0 * a) * (1.0 - 0.4 * fac)
        wr = 1.0 + 0.16 * fac * math.cos(11.0 * a + h * 0.09) + 0.05 * fac * math.cos(17.0 * a - h * 0.2)
        return r * gore * wr, h + 0.8 * math.sin(3.0 * a + h * 0.1) * fac
    body = revolve("balloon_body", prof, P["rubber"], sides=64, squash=SQ, mod=mod, part="balloon", angle=70.0)
    F = rev_frame(prof, SQ, mod)
    # ---- seam tape: a raised bead along each crease, with a few darker pale stitches
    for g in range(8):
        a = TAU * (g + 0.5) / 8
        pts = []
        for h in (-21.0, -12.0, -3.0, 6.0, 15.0, 23.0, 29.0, 31.8):
            p, n = F(h, a)
            pts.append(p + n * 0.25)
        K.tapered("seam%d" % g, pts, 0.55, 0.3, P["tape"], sides=4, sub=2, part="seams")
    # ---- three patches stitched on, real raised lenses with a ring of thread stitches
    for i, (h, a, rx, ry) in enumerate(((10.0, math.radians(62), 6.6, 5.0), (-10.0, math.radians(118), 5.4, 4.2), (21.0, math.radians(104), 4.4, 3.4))):
        pt = K.blob("patch%d" % i, (0, 0, 0), (rx, ry, 0.8), P["rubber_pale"], seed=i + 5, lump=0.05, rings=5, segs=14, part="patches")
        on_surface(pt, F, h, a, up=(0, 1, 0))
        p0, n0 = F(h, a)
        M = K.frame(p0, n0, (0, 1, 0))
        for k in range(14):
            t = TAU * k / 14
            K.tapered("stitch%d_%d" % (i, k), [M @ V((rx * 0.92 * math.cos(t) - 0.5, ry * 0.92 * math.sin(t), 0.55)), M @ V((rx * 0.92 * math.cos(t) + 0.5, ry * 0.92 * math.sin(t), 0.75))], 0.22, 0.2, P["thread"], sides=4, sub=1, part="stitches")
    # ---- the frame band round the silhouette (a steel hoop with rivets) and the gem boss on the face
    loop = []
    for h in [-21.0 + 53.4 * k / 26 for k in range(27)]:
        p, n = F(h, 0.0)
        loop.append(V((p.x + 0.8, p.y, 0.0)))
    for h in [32.2 - 53.2 * k / 26 for k in range(27)]:
        p, n = F(h, math.pi)
        loop.append(V((p.x - 0.8, p.y, 0.0)))
    band("hoop", loop, [(-0.7, -2.4), (0.8, -2.4), (1.3, -1.2), (1.5, 0.0), (1.3, 1.2), (0.8, 2.4), (-0.7, 2.4)][:7], P["steel"], part="hoop", angle=50.0)
    for i in range(0, len(loop), 5):
        p = loop[i]
        side = 1 if p.x > 0 else -1
        K.rivets("hoop_rivet%d" % i, [(V((p.x + side * 1.4, p.y, 1.2)), (side, 0, 0.5)), (V((p.x + side * 1.4, p.y, -1.2)), (side, 0, -0.5))], P["gold"], r=0.85, detail=False, sides=6, rng_=rr, jitter=0.1)
    gp, gn = F(4.0, math.pi / 2)
    ring = K.bl.lathe("boss_ring", [(7.4, -0.4), (7.8, 0.6), (7.0, 1.8), (5.6, 2.6), (4.8, 2.0), (4.8, 0.8)], P["gold"], sides=24, axis="z", caps=(False, False), part="boss")
    K._finish_obj(ring, "boss", True, 50.0)
    K.xf(ring, K.frame(gp - gn * 0.4, gn, (0, 1, 0)))
    K.gem_setting("balloon_jewel", gp + gn * 1.2, 3.9, P["gem"], P["gold"], normal=tuple(gn), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
    # ---- the tied neck: a lip, a knot, the string and two curling ribbon tails
    lip = revolve("neck_lip", [(2.0, -30.6), (3.3, -31.6), (3.6, -32.6), (2.6, -33.4), (1.4, -33.2)], P["rubber"], sides=14, part="knot")
    K.blob("knot", (0.4, -34.2, 0.0), (3.4, 2.6, 2.9), P["rubber"], seed=7, lump=0.2, rings=6, segs=9, part="knot")
    K.blob("knot2", (-1.2, -33.0, 1.0), (2.2, 1.8, 2.0), P["tape"], seed=9, lump=0.2, rings=5, segs=8, part="knot")
    K.tube("string_a", [(0.0, -31.2, 3.0), (3.2, -33.4, 3.2), (0.0, -35.2, 2.8), (-3.4, -34.0, 2.4), (-1.0, -31.8, 3.0)], 0.38, P["string"], sides=5, sub=3, part="strings")
    for i, (sx, ang) in enumerate(((1, 0.35), (-1, -0.5))):
        path = [V((sx * (1.2 + 0.9 * k), -34.0 - 0.8 * k * (1.0 + 0.3 * math.sin(k * 0.9)), 1.0 + 1.6 * math.sin(k * 0.7 + i))) for k in range(9)]
        K.ribbon("tail%d" % i, path, lambda t: 3.0 * (1.0 - 0.3 * t) + 0.4, P["ribbon"], facing=(0, 0, 1), thick=0.1, part="tails", fray=0.8, rng_=rr, edge_wave=0.12, notch=1.2)
    # ---- two small balloons tied to the hoop on strings (bounded silhouette variation)
    for i, (bx, by, sx) in enumerate(((-37.0, 15.0, -1), (37.0, 3.0, 1))):
        K.blob("mini%d" % i, (bx, by + 7.0, 3.0), (4.6, 5.6, 4.2), P["mini"], seed=20 + i, lump=0.08, rings=6, segs=10, part="minis")
        K.blob("mini_knot%d" % i, (bx - sx * 0.2, by + 1.2, 3.0), (1.2, 1.0, 1.1), P["tape"], seed=22 + i, lump=0.1, rings=4, segs=6, part="minis")
        K.tube("mini_string%d" % i, [(sx * 25.8, by - 9.0, 1.0), (sx * 31.0, by - 5.0, 2.0), (bx - sx * 1.0, by - 1.0, 3.0), (bx, by + 1.2, 3.0)], 0.28, P["string"], sides=4, sub=3, part="strings")
    back_gear(P, -11.0, y0=-7.0, rr=rr, metal=P["steel"])
    return K.quad_estimate()


BUILDERS["balloon"] = (balloon_shield, "legendary_shield__orange_balloon_shield", "orange_balloon_shield", {"weights": {"back": 0.4, "balloon": 1.3}, "focus": (-12.0, 24.0)})

# ================================================================ 3. EGG SHIELD
EGG_H, EGG_R = 34.0, 25.0


def egg_r(h):
    c = max(-1.0, min(1.0, h / EGG_H))
    a = math.acos(c)
    return EGG_R * math.sin(a) * (1.0 - 0.17 * c)


def egg_piece(name, mat, part, lo, hi, lo_in, hi_in, n_o, sides, sq, thick):
    """A solid eggshell piece between two height functions of phi (the rim of a break is jagged: lo / hi depend on phi); outer wall, rim, inner wall in one closed loft."""
    rings = []
    for pass_ in (0, 1):
        for k in range(n_o + 1):
            f = k / n_o if pass_ == 0 else 1.0 - k / n_o
            ring = []
            for i in range(sides):
                a = TAU * i / sides
                if pass_ == 0:
                    h = lo(a) + (hi(a) - lo(a)) * f
                    r = egg_r(h)
                    ring.append(V((r * math.cos(a), h, r * math.sin(a) * sq)))
                else:
                    h = lo_in(a) + (hi_in(a) - lo_in(a)) * f
                    r = egg_r(h)
                    ring.append(V(((r - thick) * math.cos(a), h, (r * sq - thick * 0.7) * math.sin(a))))
            rings.append(ring)
    ob = K.bl.loft(name, rings, mat, (True, True), part)
    return K._finish_obj(ob, part, True, 58.0)


def egg_shield():
    P = {}
    P["shell"] = S("eggshell", rgb(240, 224, 190), 0.0, 0.34, bump="grain", bump_scale=2.2, bump_strength=0.2, mottle=0.9, mottle_color=rgb(208, 176, 132), wear=0.7, wear_color=rgb(255, 250, 235), wear_metal=0.0,
                   dirt=1.0, dirt_color=(0.2, 0.12, 0.05, 1), fine=0.6, seed=111, bevel=0.1, ao_dist=5.0, sss=0.2)
    P["speck"] = S("speckle", rgb(120, 76, 44), 0.0, 0.4, seed=112, bevel=0.0)
    P["yolk"] = S("yolk", rgb(255, 176, 20), 0.0, 0.1, emit=rgb(255, 170, 30), emit_strength=0.55, emit_flicker=0.3, mottle=0.6, mottle_color=rgb(255, 220, 90), rim=(rgb(255, 240, 160), 0.5), seed=113, bevel=0.0)
    P["fluff"] = S("fluff", rgb(255, 214, 60), 0.0, 0.8, bump="leather", bump_scale=0.4, bump_strength=0.6, mottle=0.9, mottle_color=rgb(255, 240, 150), wear=0.4, wear_color=rgb(255, 250, 200), wear_metal=0.0, dirt=0.5,
                    dirt_color=(0.2, 0.1, 0.0, 1), seed=114, bevel=0.0, fine=0.6)
    P["beak"] = S("beak", rgb(255, 150, 40), 0.0, 0.3, wear=0.5, wear_color=rgb(255, 210, 140), wear_metal=0.0, seed=115, bevel=0.05)
    P["eye"] = S("chick_eye", rgb(14, 10, 12), 0.0, 0.05, rim=(rgb(255, 255, 255), 0.4), seed=116, bevel=0.0)
    P["bark"] = S("nest_wood", rgb(110, 76, 44), 0.0, 0.7, bump="grain", bump_scale=1.6, bump_strength=0.8, groove=rgb(30, 18, 10), groove_amount=0.8, mottle=0.9, mottle_color=rgb(70, 52, 28), wear=0.6, wear_color=rgb(190, 150, 100),
                  wear_metal=0.0, dirt=0.9, fine=0.7, seed=117, bevel=0.1, ao_dist=2.0)
    P["bark2"] = S("nest_wood2", rgb(150, 112, 66), 0.0, 0.7, bump="grain", bump_scale=1.4, bump_strength=0.8, groove=rgb(40, 24, 12), groove_amount=0.8, mottle=0.9, mottle_color=rgb(100, 76, 40), wear=0.6, wear_color=rgb(220, 190, 130),
                   wear_metal=0.0, dirt=0.9, fine=0.7, seed=118, bevel=0.1, ao_dist=2.0)
    P["gold"] = S("egg_gold", rgb(222, 168, 62), 1.0, 0.3, wear=1.0, wear_color=rgb(255, 234, 164), dirt=0.9, dirt_color=(0.04, 0.03, 0.0, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.2, mottle=0.8,
                  mottle_color=rgb(110, 70, 24), fine=0.5, seed=119, bevel=0.18)
    P["gem"] = S("egg_gem", rgb(40, 220, 90), 0.0, 0.12, emit=rgb(60, 255, 110), emit_strength=0.7, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(190, 255, 150), seed=120, bevel=0.0)
    P["leather"] = S("egg_leather", rgb(100, 64, 34), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(26, 14, 6), groove_amount=0.8, wear=0.8, wear_color=rgb(170, 120, 80), wear_metal=0.0, dirt=0.9,
                     mottle=0.8, mottle_color=rgb(50, 28, 12), seed=121, bevel=0.0)
    rr = K.rng(3303)
    SQ, T = 0.5, 1.7
    teeth = [rr.uniform(1.4, 3.4) for _ in range(12)]

    def hc(a):                                       # the crack: low at the front, high at the back, jagged, with two bigger bites
        sa, ca = math.sin(a), math.cos(a)
        t = (a / TAU * 12.0)
        i = int(t) % 12
        fr = t - int(t)
        zig = teeth[i] * (fr if fr < 0.5 else 1.0 - fr) * 2.0 - 1.6
        bite = -4.2 * math.exp(-((a - 2.2) / 0.16) ** 2) - 3.0 * math.exp(-((a - 0.6) / 0.12) ** 2)
        return 8.5 - 6.5 * sa + 3.0 * ca + zig + bite
    egg_piece("shell_lower", P["shell"], "shell", lambda a: -EGG_H, hc, lambda a: -EGG_H + T, hc, 9, 56, SQ, T)
    # ---- the cap: the same jagged break, hinged at the back rim and swung open, slipped aside
    cap = egg_piece("shell_cap", P["shell"], "shell_cap", hc, lambda a: EGG_H, hc, lambda a: EGG_H - T, 7, 56, SQ, T)
    hinge = V((1.5, hc(math.radians(270)), -egg_r(hc(math.radians(270))) * SQ))
    M = Matrix.Translation(hinge + V((-2.0, 1.2, -0.5))) @ Matrix.Rotation(math.radians(-41), 4, "X") @ Matrix.Rotation(math.radians(9), 4, "Z") @ Matrix.Translation(-hinge)
    K.xf(cap, M)
    # ---- speckles baked into the shell (bake only), on both pieces
    for i in range(70):
        a = rr.uniform(0, TAU)
        h = rr.uniform(-31, 31)
        r = egg_r(h)
        pt = V((r * math.cos(a), h, r * math.sin(a) * SQ))
        if h < hc(a) - 1.0 or h > hc(a) + 1.0:
            sp = K.blob("sp%d" % i, (0, 0, 0), (rr.uniform(0.3, 0.8),) * 2 + (0.25,), P["speck"], seed=i, lump=0.1, rings=3, segs=5, detail=True, part="speckles")
            n = V((math.cos(a), 0.0, math.sin(a) / SQ)).normalized()
            K.xf(sp, K.frame(pt, n, (0, 1, 0)))
            if h > hc(a):
                K.xf(sp, M)
    # ---- inside: a glowing yolk and the hatchling peeking over the broken rim
    K.blob("yolk", (-1.0, -9.0, 0.5), (14.5, 11.0, 7.0), P["yolk"], seed=3, lump=0.06, rings=8, segs=14, part="yolk")
    hx, hy, hz = -3.0, 4.5, 3.0
    K.blob("chick_head", (hx, hy, hz), (6.4, 6.0, 5.6), P["fluff"], seed=4, lump=0.2, rings=7, segs=11, part="chick")
    for i in range(10):
        a = TAU * i / 10 + rr.uniform(-0.2, 0.2)
        b0 = V((hx + 3.4 * math.cos(a), hy + 4.2 + 1.0 * math.sin(a), hz - 0.5 + 2.0 * math.sin(a)))
        K.tapered("tuft%d" % i, [b0, b0 + V((0.4 * math.cos(a), 1.6, 0.2 * math.sin(a))), b0 + V((1.4 * math.cos(a), 3.4, 0.8 * math.sin(a)))], 0.55, 0.05, P["fluff"], sides=3, sub=1, part="chick")
    K.tapered("beak_up", [V((hx, hy - 0.4, hz + 5.0)), V((hx, hy - 0.1, hz + 7.4)), V((hx, hy - 0.9, hz + 9.0))], 1.5, 0.12, P["beak"], sides=4, sub=1, part="chick")
    K.tapered("beak_low", [V((hx, hy - 1.8, hz + 4.6)), V((hx, hy - 1.9, hz + 6.4)), V((hx, hy - 2.0, hz + 7.9))], 1.1, 0.1, P["beak"], sides=4, sub=1, part="chick")
    for sx in (-1, 1):
        K.blob("chick_eye%d" % sx, (hx + sx * 3.1, hy + 1.2, hz + 3.9), (1.15, 1.25, 1.0), P["eye"], seed=sx + 9, lump=0.02, rings=5, segs=8, part="chick")
        for k in range(3):                          # down feathers on the wings
            K.feather("wing%d_%d" % (sx > 0, k), V((hx + sx * 5.2, hy - 3.6 - 1.4 * k, hz - 0.5)), V((sx * 0.9, -0.5 - 0.3 * k, 0.5)), 5.2 - 0.6 * k, 2.0, P["fluff"], curl=1.2, serrate=0.12, rows=4, thick=0.14, seed=k + 5 * sx, part="chick")
    # ---- the brass hoop round the waist with a jewel plate, and a nest of twigs the egg sits in
    hh = -13.0
    hoop = [V(((egg_r(hh) + 0.3) * math.cos(TAU * i / 48), hh, (egg_r(hh) * SQ + 0.2) * math.sin(TAU * i / 48))) for i in range(48)]
    band("hoop", hoop, [(-0.3, -2.0), (0.9, -2.0), (1.4, -0.8), (1.4, 0.8), (0.9, 2.0), (-0.3, 2.0)], P["gold"], part="hoop", up=(0, -1, 0), angle=50.0)
    for i in range(0, 48, 3):
        a = TAU * i / 48
        n = V((math.cos(a), 0, math.sin(a))).normalized()
        K.rivets("hrivet%d" % i, [(V(((egg_r(hh) + 1.5) * math.cos(a), hh + 1.1, (egg_r(hh) * SQ + 1.3) * math.sin(a))), tuple(n))], P["gold"], r=0.6, detail=False, sides=5)
    K.gem_setting("egg_jewel", (0.0, hh, egg_r(hh) * SQ + 1.4), 3.0, P["gem"], P["gold"], normal=(0, 0, 1), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
    for i in range(16):                              # nest twigs hugging the lower egg
        a0 = rr.uniform(0, TAU)
        y_ = rr.uniform(-31.0, -17.0)
        pts = []
        for k in range(5):
            a = a0 + (k - 2) * 0.42
            rad = egg_r(y_) + 1.6 + 0.8 * math.sin(k + i)
            pts.append((rad * math.cos(a), y_ + 1.4 * (k - 2) * rr.uniform(0.2, 1.0) + 1.2 * math.sin(k * 1.7 + i), (rad * SQ + 1.5) * math.sin(a)))
        K.wood_branch("twig%d" % i, pts, rr.uniform(0.7, 1.1), 0.35, P["bark" if i % 2 else "bark2"], seed=i, sides=5, knots=1, part="nest", sub=1, bump=0.1)
    for i in range(7):                               # stray sticks poking out of the nest
        a = math.radians(rr.choice((200, 215, 232, 250, 290, 308, 325, 345, 10)) + rr.uniform(-6, 6))
        y_ = rr.uniform(-32.0, -22.0)
        r0 = egg_r(y_) * 0.9
        p0 = V((r0 * math.cos(a), y_, r0 * SQ * math.sin(a)))
        d = V((math.cos(a), rr.uniform(-0.4, 0.2), 0.6 * math.sin(a) + 0.2)).normalized()
        K.wood_branch("stick%d" % i, [tuple(p0), tuple(p0 + d * 7.0), tuple(p0 + d * 15.0 + V((0, rr.uniform(-2, 2), 0)))], 0.85, 0.3, P["bark2"], seed=30 + i, sides=5, knots=1, part="nest", sub=1)
    # ---- chips and shards of broken shell
    for i in range(7):
        pos = V((rr.uniform(-26, 26), rr.uniform(-6, 30), rr.uniform(8, 22))) if i < 4 else V((rr.uniform(-30, 30), rr.uniform(-30, -22), rr.uniform(6, 16)))
        K.shard("chip%d" % i, pos, rr.uniform(1.4, 2.6), P["shell"], rr, part="chips", sides=4, stretch=1.2)
    back_gear(P, -13.0, y0=-8.0, rr=rr, metal=P["gold"])
    return K.quad_estimate()


BUILDERS["egg"] = (egg_shield, "legendary_shield__egg_shield", "egg_shield", {"weights": {"back": 0.4, "shell": 1.2}, "focus": (-14.0, 24.0)})

# ================================================================ 4. ZOMBIE SHIELD
def stitched_seam(name, pts2d, zfun, welt_m, thread_m, rr, sp=1.9, span=1.25):
    """A raised welt along a surface path with real thread stitches crossing it (tapered, slightly tilted, each a little different)."""
    pts = [V((x, y, zfun(x, y) + 0.45)) for (x, y) in pts2d]
    K.tapered(name + "_welt", pts, 0.62, 0.5, welt_m, sides=4, sub=3, part="seams", bulge=lambda t: 1.0 + 0.25 * math.sin(t * 17.0))
    sm = K.bl.smooth_path(pts, 3)
    acc, last = 0.0, sm[0]
    n = 0
    for p in sm[1:]:
        acc += (p - last).length
        last = p
        if acc >= sp:
            acc = 0.0
            i = sm.index(p)
            tan = (sm[min(i + 1, len(sm) - 1)] - sm[max(i - 1, 0)]).normalized()
            side = V((-tan.y, tan.x, 0.0))
            tilt = rr.uniform(-0.35, 0.35)
            a = p + side * span * (1.0 + 0.15 * rr.uniform(-1, 1)) + tan * tilt * span
            b = p - side * span * (1.0 + 0.15 * rr.uniform(-1, 1)) - tan * tilt * span
            K.tapered("%s_st%d" % (name, n), [a + V((0, 0, 0.15)), p + V((0, 0, 0.62)), b + V((0, 0, 0.15))], 0.2, 0.2, thread_m, sides=4, sub=1, part="stitches")
            n += 1


def zombie_shield():
    P = {}
    P["flesh"] = S("zflesh", rgb(112, 132, 84), 0.0, 0.52, bump="leather", bump_scale=0.7, bump_strength=0.55, mottle=1.0, mottle_color=rgb(96, 62, 104), veins=0.16, vein_color=rgb(120, 40, 66), vein_strength=0.0,
                   wear=0.5, wear_color=rgb(176, 188, 128), wear_metal=0.0, dirt=1.0, dirt_color=(0.03, 0.025, 0.01, 1), fine=0.8, seed=131, bevel=0.1, ao_dist=3.5, sss=0.2, groove=rgb(40, 30, 24), groove_amount=0.6)
    P["flesh2"] = S("zflesh_dark", rgb(86, 76, 82), 0.0, 0.5, bump="leather", bump_scale=0.9, bump_strength=0.65, mottle=1.0, mottle_color=rgb(60, 40, 70), wear=0.5, wear_color=rgb(150, 150, 110), wear_metal=0.0, dirt=1.0,
                    dirt_color=(0.02, 0.015, 0.012, 1), fine=0.8, seed=132, bevel=0.1, ao_dist=3.0, groove=rgb(30, 20, 20), groove_amount=0.6)
    P["raw"] = S("zraw", rgb(150, 44, 52), 0.0, 0.28, bump="leather", bump_scale=0.6, bump_strength=0.4, mottle=0.9, mottle_color=rgb(90, 20, 40), rim=(rgb(255, 120, 120), 0.35), wear=0.4, wear_color=rgb(210, 100, 90), wear_metal=0.0,
                 dirt=0.6, seed=133, bevel=0.05)
    P["bone"] = S("zbone", rgb(214, 200, 164), 0.0, 0.5, bump="grain", bump_scale=1.5, bump_strength=0.35, mottle=1.0, mottle_color=rgb(120, 96, 60), wear=0.8, wear_color=rgb(240, 232, 204), wear_metal=0.0, dirt=1.0,
                  dirt_color=(0.06, 0.04, 0.02, 1), scratches=0.5, fine=0.7, seed=134, bevel=0.14, ao_dist=2.5)
    P["thread"] = S("zthread", rgb(24, 20, 22), 0.0, 0.6, bump="wrap", bump_strength=0.7, wear=0.4, wear_color=rgb(90, 80, 70), wear_metal=0.0, seed=135, bevel=0.0)
    P["cloth"] = S("zcloth", rgb(160, 144, 108), 0.0, 0.8, bump="cross", bump_scale=1.2, bump_strength=0.6, groove=rgb(50, 40, 28), groove_amount=0.7, mottle=1.0, mottle_color=rgb(96, 70, 40), dirt=1.0, dirt_color=(0.05, 0.03, 0.01, 1),
                   wear=0.7, wear_color=rgb(210, 200, 170), wear_metal=0.0, seed=136, bevel=0.0)
    P["sclera"] = S("zsclera", rgb(226, 220, 168), 0.0, 0.2, mottle=0.9, mottle_color=rgb(170, 90, 70), veins=0.1, vein_color=rgb(150, 30, 40), vein_strength=0.0, rim=(rgb(255, 255, 220), 0.3), seed=137, bevel=0.0, dirt=0.5, dirt_color=(0.2, 0.1, 0.0, 1))
    P["iris"] = S("ziris", rgb(100, 255, 50), 0.0, 0.1, emit=rgb(110, 255, 60), emit_strength=0.8, emit_flicker=0.5, mottle=0.9, mottle_color=rgb(220, 255, 100), seed=138, bevel=0.0)
    P["pupil"] = S("zpupil", rgb(6, 6, 4), 0.0, 0.05, seed=139, bevel=0.0)
    P["ooze"] = S("zooze", rgb(150, 190, 40), 0.0, 0.08, transmission=0.85, ior=1.12, emit=rgb(140, 200, 40), emit_strength=0.25, emit_flicker=0.4, rim=(rgb(230, 255, 130), 0.6), mottle=0.7, mottle_color=rgb(90, 130, 30), seed=140, bevel=0.0)
    P["nail"] = S("znail", rgb(46, 36, 28), 0.0, 0.4, wear=0.7, wear_color=rgb(120, 100, 70), wear_metal=0.0, seed=141, bevel=0.05)
    P["leather"] = S("zleather", rgb(70, 52, 40), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(20, 12, 8), groove_amount=0.8, wear=0.8, wear_color=rgb(130, 100, 70), wear_metal=0.0, dirt=0.9, mottle=0.8,
                     mottle_color=rgb(36, 24, 16), seed=142, bevel=0.0)
    P["iron"] = S("ziron", rgb(90, 84, 80), 1.0, 0.45, wear=1.0, wear_color=rgb(190, 170, 150), dirt=1.0, dirt_color=(0.03, 0.02, 0.01, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.25, mottle=1.0, mottle_color=rgb(80, 40, 20),
                  fine=0.6, seed=143, bevel=0.16)
    rr = K.rng(4404)
    R0 = 29.0
    bites = ((math.radians(52), 4.6, 0.20), (math.radians(197), 5.4, 0.16), (math.radians(262), 4.0, 0.18))

    def R(a):
        r = R0 * (1.0 + 0.035 * math.sin(3.0 * a + 1.0) + 0.025 * math.sin(7.0 * a + 0.4))
        for (ba, bd, bw) in bites:
            d = math.atan2(math.sin(a - ba), math.cos(a - ba))
            r -= bd * math.exp(-(d / bw) ** 2)
        return r

    def outline(a):
        r = R(a)
        return (r * math.cos(a), r * math.sin(a))

    def zf(s, a):
        dome = 7.0 * (1.0 - s ** 2) ** 0.9
        crater = 10.8 * K.smoothstep(0.50, 0.34, s)
        lip = 2.6 * math.exp(-((s - 0.50) / 0.06) ** 2)
        lump = 2.6 * K.noise3((s * math.cos(a) * 3.2, s * math.sin(a) * 3.2, 1.7), 1.0, 5, 2) * K.smoothstep(0.3, 0.6, s) + 0.6 * K.noise3((s * math.cos(a) * 9, s * math.sin(a) * 9, 3.0), 1.0, 7, 1)
        return dome - crater + lip + lump + 1.2 * K.smoothstep(0.8, 1.0, s)

    def zb(s, a):
        return -2.4 - 5.5 * (1.0 - s ** 2) + 0.8 * K.noise3((s * math.cos(a) * 2.5, s * math.sin(a) * 2.5, 9.0), 1.0, 3, 2)

    def dz(x, y):
        a = math.atan2(y, x) % TAU
        s = min(1.0, math.hypot(x, y) / R(a))
        return zf(s, a)
    dish("zbody", outline, zf, zb, P["flesh"], ns=12, nb=3, nth=64, part="flesh", angle=65.0)
    # ---- the rim: a rolled, rotten band with a darker welt, and bone showing in the three bites
    rim = []
    for i in range(60):
        a = TAU * i / 60
        o = outline(a)
        rim.append(V((o[0], o[1], 0.4)))
    band("rim_roll", rim, rope_section(2.3, 6, 1.1), P["flesh2"], part="rim", scale=lambda t: 1.0 + 0.18 * K.noise3((t * 30, 0, 0), 1.0, 3), angle=62.0)
    for i, (bx, by, rx, ry) in enumerate(((-13.0, -17.0, 4.6, 3.8), (15.0, 14.0, 4.0, 3.2), (-20.0, 6.0, 3.2, 2.8), (17.0, -9.0, 3.4, 2.8), (6.0, 21.0, 3.0, 2.4), (-7.0, 20.5, 2.6, 2.2))):      # swollen boils and lumps on the face
        K.blob("boil%d" % i, (bx, by, dz(bx, by) + 0.2), (rx, ry, 2.3), P["flesh2" if i % 2 else "raw"], seed=80 + i, lump=0.16, rings=5, segs=9, part="boils")
    for k, (ba, bd, bw) in enumerate(bites):
        c = outline(ba)
        for j in range(5):
            aj = ba + (j - 2) * bw * 0.55
            o = outline(aj)
            d = V((math.cos(ba), math.sin(ba), 0.0))
            K.tapered("bonespike%d_%d" % (k, j), [V((o[0], o[1], 0.0)), V((o[0], o[1], 0.0)) + d * 1.4 + V((0, 0, 0.8)), V((o[0], o[1], 0.0)) + d * rr.uniform(3.5, 6.0) + V((0, 0, rr.uniform(0.5, 2.0)))], 1.0, 0.1,
                      P["bone"], sides=4, sub=1, part="bone_spikes")
        K.blob("raw%d" % k, (c[0] * 0.97, c[1] * 0.97, 1.2), (3.2, 3.2, 1.2), P["raw"], seed=k + 3, lump=0.2, rings=4, segs=8, part="raw")
    # ---- the open chest: a rib cage arching over the crater with a sternum, and the eye behind it
    ecx, ecy = 1.2, -0.8
    K.blob("eyeball", (ecx, ecy, 0.4), (6.3, 6.3, 6.0), P["sclera"], seed=5, lump=0.03, rings=8, segs=12, part="eye")
    K.blob("iris", (ecx + 0.2, ecy, 5.2), (3.5, 3.5, 1.5), P["iris"], seed=6, lump=0.04, rings=6, segs=10, part="eye_iris")
    K.blob("pupil", (ecx + 0.2, ecy, 6.5), (1.1, 1.9, 0.7), P["pupil"], seed=7, lump=0.02, rings=4, segs=8, part="eye_iris")
    for i, yy in enumerate((-10.5, -5.5, -0.5, 4.5, 9.5)):
        xe = math.sqrt(max(4.0, 14.2 ** 2 - yy * yy)) * 0.98
        top = 11.2 - 0.12 * abs(yy)
        dr = 4.6 + 0.5 * i                              # ribs droop away from the sternum like a real cage
        pts = [V((-xe, yy - dr, dz(-xe, yy - dr * 0.6) + 0.2)), V((-xe * 0.62, yy - dr * 0.35, top * 0.8)), V((0.0, yy + 0.9, top)), V((xe * 0.62, yy - dr * 0.4, top * 0.78)), V((xe, yy - dr * 1.05, dz(xe, yy - dr * 0.6) + 0.2))]
        K.tapered("rib%d" % i, pts, 1.5, 1.05, P["bone"], sides=5, sub=2, part="ribs", bulge=lambda t, i=i: 0.75 + 0.4 * math.sin(math.pi * t) + 0.08 * math.sin(i + t * 9))
        for sx in (-1, 1):
            K.blob("rib_knob%d_%d" % (i, sx), (sx * xe, yy - dr, dz(sx * xe, yy - dr * 0.6) + 0.4), (1.7, 1.7, 1.4), P["bone"], seed=i + sx, lump=0.15, rings=3, segs=5, part="ribs")
    K.bone("sternum", (0.0, -13.0, 10.2), (0.0, 13.0, 10.8), 1.5, P["bone"], knob=1.4, sub=4, sides=6, part="ribs", bow=(0.0, 0.0, 1.0), seed=3)
    # ---- patchwork: three stitched seams
    def arc(r0, a0, r1, a1, n=9, wob=0.0):
        out = []
        for i in range(n):
            t = i / (n - 1)
            r = r0 + (r1 - r0) * t
            a = math.radians(a0 + (a1 - a0) * t) + wob * math.sin(t * 7.0)
            out.append((r * math.cos(a), r * math.sin(a)))
        return out
    stitched_seam("sA", arc(27.0, 20.0, 15.6, 38.0, 9, 0.04), dz, P["flesh2"], P["thread"], rr)
    stitched_seam("sB", arc(27.5, 160.0, 15.4, 172.0, 8, 0.05), dz, P["flesh2"], P["thread"], rr)
    stitched_seam("sC", arc(26.5, 312.0, 15.8, 285.0, 8, 0.05), dz, P["flesh2"], P["thread"], rr)
    # ---- the rotten hand gripping the rim at the top left
    ha = math.radians(138.0)
    d = V((math.cos(ha), math.sin(ha), 0.0))
    pp = V((-d.y, d.x, 0.0))
    ro = outline(ha)
    rimp = V((ro[0], ro[1], 0.0))
    pc = rimp + d * 5.6 + V((0, 0, -0.8))
    K.blob("palm", tuple(pc), (6.2, 6.6, 2.7), P["flesh"], seed=8, lump=0.14, rings=6, segs=10, part="hand")
    K.tapered("wrist", [pc + d * 2.0, pc + d * 6.5 + V((0, 0, -1.0)), pc + d * 10.0 + V((0, 0, -2.0))], 3.6, 2.9, P["flesh"], sides=7, sub=2, part="hand")
    for j in range(5):
        aw = ha + math.radians(-6 + j * 3.0)
        ow = outline(aw)
        K.tapered("wristbone%d" % j, [V((ow[0], ow[1], -2.0)) + d * 10.0 + pp * (j - 2.0) * 1.3, V((ow[0], ow[1], -2.0)) + d * 12.5 + pp * (j - 2.0) * 1.2, V((ow[0], ow[1], -2.0)) + d * rr.uniform(14.0, 16.5) + pp * (j - 2.0) * 1.1],
                  1.1, 0.12, P["bone"], sides=4, sub=1, part="hand")
    for k in range(5):
        u = (k - 2.0) * 2.45
        thumb = (k == 4)
        base = pc - d * 1.5 + pp * u * 0.8 + V((0, 0, 0.4))
        lenv = (10.5 + rr.uniform(-1.4, 1.6)) * (0.74 if thumb else 1.0)
        over = rimp + d * 0.2 + pp * u * 1.05 + V((0, 0, 4.6))
        tip = rimp - d * lenv + pp * u * (1.35 + 0.03 * lenv)
        tip = V((tip.x, tip.y, dz(tip.x, tip.y) + 1.2))
        mid = over + (tip - over) * 0.45 + V((0, 0, 2.4))
        mid2 = over + (tip - over) * 0.78 + V((0, 0, 1.4)) + pp * rr.uniform(-0.9, 0.9)
        K.tapered("finger%d" % k, [base, over, mid, mid2, tip], 1.65, 0.8, P["flesh"], sides=5, sub=1, part="fingers", bulge=lambda t: 1.0 + 0.2 * math.sin(t * 11.0))
        K.blob("knuckle%d" % k, tuple(over + V((0, 0, 0.1))), (1.8, 1.8, 1.5), P["flesh2"], seed=k + 20, lump=0.2, rings=4, segs=6, part="fingers")
        K.tapered("nail%d" % k, [tip + (mid - tip).normalized() * 0.8, tip - (mid - tip).normalized() * 0.5 + V((0, 0, -0.8)), tip - (mid - tip).normalized() * 2.6 + V((0, 0, -1.4))], 0.8, 0.12, P["nail"], sides=4, sub=1, part="fingers")
    # ---- ooze dripping off the lower rim
    for i, ang in enumerate((228, 254, 284, 305)):
        a = math.radians(ang)
        o = outline(a)
        K.drip("drip%d" % i, (o[0] * 0.995, o[1] * 0.995 - 1.2, 0.9), rr.uniform(4.5, 8.0), rr.uniform(1.0, 1.6), P["ooze"], seed=60 + i, sides=7, part="ooze")
    K.shard_cloud("gob", (0, -4, 6), (30.0, 34.0, 8.0), 6, (0.8, 1.4), P["ooze"], rr, avoid=lambda p: math.hypot(p.x, p.y) < 33.0 and abs(p.z) < 12 and p.z > -8.0, part="ooze", stretch=0.7, sides=4, max_count=6)
    # ---- bandage tails knotted on the rim at the lower right
    for i, (ang, ln) in enumerate(((-24, 17), (-46, 12))):
        a = math.radians(ang)
        o = outline(a)
        path = [V((o[0] + 0.9 * k * math.cos(a) * 0.6, o[1] - k * ln / 8.0 + 0.4 * math.sin(k + i), 2.4 + 0.9 * math.sin(k * 0.8 + i))) for k in range(9)]
        K.ribbon("bandage%d" % i, path, lambda t: 4.2 * (1.0 - 0.35 * t) + 0.5, P["cloth"], facing=(0, 0, 1), thick=0.12, part="bandage", fray=1.2, rng_=rr, edge_wave=0.1, notch=1.4 if i == 0 else 0.0)
        K.blob("bandage_knot%d" % i, (o[0], o[1], 2.4), (2.0, 2.0, 1.4), P["cloth"], seed=70 + i, lump=0.2, rings=4, segs=7, part="bandage")
    back_gear(P, -7.8, y0=-9.0, rr=rr, strap_m=P["leather"], metal=P["iron"])
    return K.quad_estimate()


BUILDERS["zombie"] = (zombie_shield, "legendary_shield__zombie_shield", "zombie_shield", {"weights": {"back": 0.4, "flesh": 1.3}, "focus": (-14.0, 22.0)})

# ================================================================ 5. CHIMERA VIPER SHIELD
def new_objects(fn):
    """Runs fn() and returns the scene objects it created (to place a whole sub-assembly with one transform)."""
    sc = bpy.context.scene
    before = set(sc.collection.objects)
    fn()
    return [o for o in sc.collection.objects if o not in before]


def seg_sweep(name, path, rad, mat, *, sides=8, spacing=1.2, period=3.6, ridge=0.07, part=None, angle=60.0, up=(0, 0, 1)):
    """A banded snake body: rings every `spacing` cm along a smoothed path, the radius = rad(t) with a belly-plate ridge every `period` cm (real geometry, not paint)."""
    pts = [V(p) for p in path]
    lens = [0.0]
    for a, b in zip(pts, pts[1:]):
        lens.append(lens[-1] + (b - a).length)
    tot = lens[-1]
    n = max(4, int(tot / spacing))
    out = []
    j = 0
    for i in range(n + 1):
        s = tot * i / n
        while j < len(pts) - 2 and lens[j + 1] < s:
            j += 1
        f = (s - lens[j]) / max(lens[j + 1] - lens[j], 1e-6)
        out.append(pts[j].lerp(pts[j + 1], f))
    scale = lambda t: rad(t) * (1.0 + ridge * math.cos(TAU * (t * tot) / period))      # noqa: E731
    ob = K.bl.sweep(name, out, K.bl.circle(1.0, sides), mat, scale=scale, up=up, part=part or name, caps=(True, True))
    return K._finish_obj(ob, part or name, True, angle), tot


def head_tube(name, mat, part, stations, sides=10):
    """A closed head / jaw loft: stations [(z, half width, half height, y centre)], closed with a rounded cap at each end."""
    rings = []
    for k, (z, w, h, yc) in enumerate(stations):
        rings.append([V((w * math.cos(TAU * i / sides), yc + h * math.sin(TAU * i / sides), z)) for i in range(sides)])
    ob = K.bl.loft(name, rings, mat, (True, True), part)
    return K._finish_obj(ob, part, True, 55.0)


def viper_shield():
    P = {}
    P["skin"] = S("viper_skin", rgb(52, 104, 56), 0.0, 0.38, bump="scales", bump_scale=1.1, bump_strength=0.55, mottle=1.0, mottle_color=rgb(14, 30, 22), wear=0.6, wear_color=rgb(150, 190, 90), wear_metal=0.0, dirt=0.9, dirt_color=(0.0, 0.015, 0.005, 1),
                  fine=0.5, seed=151, bevel=0.1, ao_dist=2.5, rim=(rgb(120, 220, 120), 0.25))
    P["skin2"] = S("viper_skin2", rgb(86, 120, 44), 0.0, 0.36, bump="scales", bump_scale=1.4, bump_strength=0.6, mottle=1.0, mottle_color=rgb(40, 60, 20), wear=0.6, wear_color=rgb(200, 200, 100), wear_metal=0.0, dirt=0.9, dirt_color=(0.0, 0.015, 0.0, 1),
                   fine=0.5, seed=152, bevel=0.1, ao_dist=2.5)
    P["plate"] = S("viper_plate", rgb(40, 44, 58), 1.0, 0.34, bump="hammer", bump_scale=0.7, bump_strength=0.22, mottle=1.0, mottle_color=rgb(20, 56, 48), wear=1.0, wear_color=rgb(170, 190, 200), dirt=1.0, dirt_color=(0.0, 0.01, 0.01, 1),
                   scratches=0.8, fine=0.5, seed=153, bevel=0.2, ao_dist=3.0)
    P["scale"] = S("viper_scale", rgb(40, 110, 92), 0.6, 0.28, wear=1.0, wear_color=rgb(170, 240, 200), wear_metal=0.8, dirt=0.9, scratches=0.6, mottle=1.0, mottle_color=rgb(60, 30, 100), seed=154, bevel=0.14, bump="scales", bump_scale=1.0, bump_strength=0.3)
    P["bone"] = S("viper_bone", rgb(226, 214, 178), 0.0, 0.4, bump="grain", bump_scale=1.4, bump_strength=0.3, mottle=0.9, mottle_color=rgb(150, 120, 70), wear=0.7, wear_color=rgb(250, 244, 220), wear_metal=0.0, dirt=0.9, dirt_color=(0.05, 0.03, 0.01, 1),
                  seed=155, bevel=0.1)
    P["horn"] = S("viper_horn", rgb(60, 48, 40), 0.0, 0.4, bump="grain", bump_scale=2.0, bump_strength=0.5, mottle=1.0, mottle_color=rgb(150, 120, 80), wear=0.8, wear_color=rgb(200, 180, 140), wear_metal=0.0, dirt=0.8, seed=156, bevel=0.1)
    P["eye"] = S("viper_eye", rgb(255, 196, 30), 0.0, 0.1, emit=rgb(255, 190, 30), emit_strength=0.8, emit_flicker=0.3, mottle=0.7, mottle_color=rgb(255, 120, 20), seed=157, bevel=0.0)
    P["pupil"] = S("viper_pupil", rgb(4, 4, 4), 0.0, 0.05, seed=158, bevel=0.0)
    P["mouth"] = S("viper_mouth", rgb(170, 40, 60), 0.0, 0.3, bump="leather", bump_scale=0.6, bump_strength=0.3, mottle=0.8, mottle_color=rgb(90, 14, 40), rim=(rgb(255, 130, 140), 0.4), seed=159, bevel=0.0, dirt=0.8, dirt_color=(0.05, 0.0, 0.02, 1))
    P["tongue"] = S("viper_tongue", rgb(200, 24, 48), 0.0, 0.25, bump="leather", bump_scale=0.5, bump_strength=0.3, mottle=0.6, mottle_color=rgb(120, 10, 30), rim=(rgb(255, 120, 120), 0.4), seed=160, bevel=0.0)
    P["venom"] = S("viper_venom", rgb(140, 255, 60), 0.0, 0.08, emit=rgb(130, 255, 60), emit_strength=0.9, emit_flicker=0.4, seed=161, bevel=0.0)
    P["gem"] = S("viper_gem", rgb(40, 230, 100), 0.0, 0.12, emit=rgb(60, 255, 120), emit_strength=0.7, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(190, 255, 150), seed=162, bevel=0.0)
    P["gold"] = S("viper_gold", rgb(196, 150, 60), 1.0, 0.32, wear=0.9, wear_color=rgb(250, 226, 150), dirt=0.9, dirt_color=(0.03, 0.02, 0.0, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.2, mottle=0.8, mottle_color=rgb(60, 80, 40),
                  fine=0.5, seed=163, bevel=0.16)
    P["leather"] = S("viper_leather", rgb(50, 44, 36), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(14, 10, 6), groove_amount=0.8, wear=0.8, wear_color=rgb(110, 90, 60), wear_metal=0.0, dirt=0.9, mottle=0.8,
                     mottle_color=rgb(24, 20, 14), seed=164, bevel=0.0)
    rr = K.rng(5505)
    R0 = 28.0

    def outline(a):
        return (R0 * (1.0 + 0.012 * math.sin(5.0 * a)) * math.cos(a), R0 * (1.0 + 0.012 * math.sin(5.0 * a)) * math.sin(a))

    def zf(s, a):
        return 5.2 * (1.0 - s ** 2) ** 0.8 + 1.1 * math.exp(-((s - 0.86) / 0.07) ** 2) - 1.6 * K.smoothstep(0.55, 0.0, s) * 0.0 + 0.7 * K.noise3((s * math.cos(a) * 3, s * math.sin(a) * 3, 2.0), 1.0, 4, 2)

    def zb(s, a):
        return -3.0 - 5.0 * (1.0 - s ** 2)

    def dz(x, y):
        a = math.atan2(y, x)
        return zf(min(1.0, math.hypot(x, y) / R0), a)
    dish("vbody", outline, zf, zb, P["plate"], ns=11, nb=3, nth=56, part="plate", angle=62.0)
    # ---- armour scales over the whole face (baked from real overlapping tiles), growing outward from the centre
    rows = [(r, int(round(TAU * r / 3.05))) for r in [3.5 + 2.55 * k for k in range(10)]]
    for k, (r, cnt) in enumerate(rows):
        for j in range(cnt):
            a = TAU * (j + (0.5 if k % 2 else 0.0)) / cnt + rr.uniform(-0.03, 0.03)
            x, y = r * math.cos(a), r * math.sin(a)
            e = 0.4
            nx = (dz(x - e, y) - dz(x + e, y)) / (2 * e)
            ny = (dz(x, y - e) - dz(x, y + e)) / (2 * e)
            n = V((nx, ny, 1.0)).normalized()
            K.scale_plate("sc%d_%d" % (k, j), V((x, y, dz(x, y) + 0.12)), n, 1.55 * (1.0 + 0.08 * rr.uniform(-1, 1)), P["scale"], up=(-math.cos(a), -math.sin(a), 0.0), rows=2, thick=0.18, part="scales_baked", tilt=16, shape="point", detail=True)
    hoop = [V(outline(TAU * i / 56)[0], outline(TAU * i / 56)[1], 0.0) if False else V((outline(TAU * i / 56)[0], outline(TAU * i / 56)[1], 0.0)) for i in range(56)]
    band("iron_hoop", hoop, [(-0.6, -2.4), (0.9, -2.4), (1.5, -1.0), (1.5, 1.0), (0.9, 2.4), (-0.6, 2.4)], P["plate"], part="hoop", angle=50.0)
    for i in range(0, 56, 4):
        a = TAU * i / 56
        o = outline(a)
        K.rivets("hr%d" % i, [(V((o[0] * 1.045, o[1] * 1.045, 0.0)), (math.cos(a), math.sin(a), 0.0))], P["gold"], r=0.95, detail=False, sides=5, rng_=rr, jitter=0.1)
    # ---- the serpent: tail tip inside the rim, one coil right round it, then the neck rises across the face to the head
    coil = []
    N = 40
    for i in range(N + 1):
        t = i / N
        ang = math.radians(250.0 - 340.0 * t)
        r = 24.2 - 7.0 * math.exp(-t * 10.0) + 0.5 * math.sin(t * 9.0)
        x, y = r * math.cos(ang), r * math.sin(ang)
        coil.append(V((x, y, dz(x, y) + 3.0 + 0.9 * math.sin(t * 6.0) + (0.0 if t > 0.1 else -0.8))))
    neck = [V((0.0, -24.0, dz(0, -24) + 3.4)), V((4.2, -16.5, 7.4)), V((-3.5, -8.0, 8.4)), V((3.0, -1.0, 8.8)), V((0.2, 4.0, 8.4))]
    full = K.bl.smooth_path(coil + neck[1:], 2)
    n_all = len(full)

    def rad(t):
        tail = 0.55 + 2.9 * K.smoothstep(0.0, 0.22, t) ** 0.7 + 0.9 * K.smoothstep(0.84, 1.0, t)
        return tail * (1.0 + 0.1 * K.noise3((t * 12, 0, 0), 1.0, 3))
    body, tot = seg_sweep("serpent", full, rad, P["skin"], sides=7, spacing=1.2, period=3.4, ridge=0.075, part="serpent", angle=58.0)
    # ---- the head: built facing +z at the origin, then set up on the neck looking out of the shield
    hx, hy, hz = 0.2, 9.3, 14.9

    def head():
        yc = 0.0
        sk = [(-7.5, 2.9, 2.5, 0.0), (-6.4, 3.8, 2.9, 0.1), (-4.0, 5.2, 3.0, 0.2), (-1.0, 4.6, 2.5, 0.2), (2.0, 3.4, 2.0, 0.1), (5.0, 2.5, 1.7, 0.0), (8.0, 1.8, 1.4, -0.2), (9.6, 1.1, 1.0, -0.5), (10.4, 0.4, 0.4, -0.7)]
        head_tube("skull", P["skin"], "head", sk, 10)
        jaw = [(-4.0, 3.2, 1.2, -2.0), (-1.0, 3.6, 1.35, -2.2), (3.0, 2.8, 1.2, -2.2), (6.5, 1.8, 1.0, -2.2), (9.0, 1.1, 0.8, -2.2), (10.0, 0.4, 0.4, -2.2)]
        j = new_objects(lambda: head_tube("jaw", P["skin2"], "jaw", jaw, 8))
        hinge = V((0.0, -1.2, -3.8))
        Mj = Matrix.Translation(hinge) @ Matrix.Rotation(math.radians(33), 4, "X") @ Matrix.Translation(-hinge)
        for o in j:
            K.xf(o, Mj)
        # mouth lining + tongue on the lower jaw, palate under the skull
        K.blob("palate", (0.0, -1.9, 3.0), (3.3, 0.9, 6.8), P["mouth"], seed=3, lump=0.08, rings=5, segs=8, part="mouth")
        def tongue():
            K.tapered("tongue", [V((0, -2.0, 0.5)), V((0, -3.0, 6.0)), V((0.3, -3.6, 12.0)), V((-0.3, -3.2, 17.0))], 0.8, 0.3, P["tongue"], sides=5, sub=2, part="tongue")
            for sx in (-1, 1):
                K.tapered("fork%d" % sx, [V((-0.3, -3.2, 17.0)), V((sx * 0.9, -3.4, 19.0)), V((sx * 1.8, -3.5, 20.6))], 0.32, 0.05, P["tongue"], sides=4, sub=1, part="tongue")
        for o in new_objects(tongue):
            K.xf(o, Mj)
        # fangs (big, curved, venom at the tips) and rows of small teeth
        for sx in (-1, 1):
            K.tapered("fang%d" % sx, [V((sx * 1.7, -1.4, 5.8)), V((sx * 1.8, -4.2, 6.9)), V((sx * 1.7, -7.6, 6.6)), V((sx * 1.6, -9.6, 5.6))], 0.9, 0.06, P["bone"], sides=5, sub=2, part="fangs")
            K.blob("venom%d" % sx, (sx * 1.6, -11.4, 5.4), (0.55, 1.1, 0.55), P["venom"], seed=sx + 4, lump=0.08, rings=4, segs=6, part="venom")
            for k in range(5):
                zz = -2.0 + 2.1 * k
                K.tapered("tooth%d_%d" % (sx > 0, k), [V((sx * (3.2 - 0.25 * k), -1.5, zz)), V((sx * (3.2 - 0.25 * k), -2.7, zz + 0.4)), V((sx * (3.2 - 0.25 * k), -3.5, zz + 0.9))], 0.45, 0.04, P["bone"], sides=3, sub=1, part="fangs")
        for o in new_objects(lambda: [K.tapered("ltooth%d_%d" % (sx > 0, k), [V((sx * (2.8 - 0.2 * k), -3.2, -2.0 + 2.3 * k)), V((sx * (2.8 - 0.2 * k), -2.0, -1.8 + 2.3 * k)), V((sx * (2.8 - 0.2 * k), -1.0, -1.5 + 2.3 * k))], 0.4, 0.04, P["bone"], sides=3, sub=1, part="fangs") for sx in (-1, 1) for k in range(4)]):
            K.xf(o, Mj)
        # eyes, brow ridges, the jewel on the brow, chimera horns and a crest
        for sx in (-1, 1):
            K.blob("eye%d" % sx, (sx * 4.2, 1.5, -1.2), (1.5, 1.6, 1.8), P["eye"], seed=sx + 7, lump=0.03, rings=6, segs=9, part="eyes")
            K.blob("pupil%d" % sx, (sx * 5.3, 1.5, -1.0), (0.4, 1.35, 0.55), P["pupil"], seed=sx + 9, lump=0.01, rings=4, segs=6, part="eyes")
            K.tapered("brow%d" % sx, [V((sx * 2.4, 2.8, -3.4)), V((sx * 4.6, 3.5, -1.2)), V((sx * 3.8, 3.0, 1.8)), V((sx * 2.6, 2.4, 3.8))], 0.9, 0.4, P["skin2"], sides=5, sub=2, part="head")
            K.tapered("horn%d" % sx, [V((sx * 3.2, 2.4, -4.6)), V((sx * 6.4, 4.8, -8.6)), V((sx * 9.6, 8.4, -10.4)), V((sx * 10.6, 13.0, -9.0)), V((sx * 8.8, 15.6, -7.2))], 1.4, 0.08, P["horn"], sides=6, sub=2, part="horns",
                      bulge=lambda t: 1.0 + 0.12 * math.sin(t * 20.0))
            for k in range(4):                          # growth rings on the horns
                pass
        K.gem_setting("brow_gem", (0.0, 3.1, 0.4), 1.5, P["gem"], P["gold"], normal=(0, 0.9, 0.35), claws=5, sides=6, part="gem", rng_=rr, up=(0, 0, 1), claw_r=0.5)
        K.spike_array("crest", [V((0.0, 2.7 - 0.0 * k, -6.8 + 1.2 * k)) for k in range(5)] + [V((0.0, 3.0, -7.4))], [V((0.0, 1.0, -0.35)) for k in range(5)] + [V((0, 1, -0.6))], [4.2, 3.6, 3.1, 2.6, 2.2, 2.0], 0.75, P["horn"], rr, bend=0.15, sides=4, part="horns")
    objs = new_objects(head)
    Mh = K.frame((hx, hy, hz), (0.0, 0.62, 0.78), (0, 1, 0)) @ Matrix.Scale(1.15, 4)
    for o in objs:
        K.xf(o, Mh)
    # spines down the neck and a dorsal ridge on the coil
    K.spike_array("neck_spine", [V((0.0, -12.0 + 3.0 * k, 8.8 + 1.0 * k)) + V((0, 0, 3.0)) for k in range(5)], [V((0.0, 0.6, 1.0)) for k in range(5)], 3.2, 0.7, P["horn"], rr, bend=0.2, sides=4, part="horns")
    K.shard_cloud("fleck", (0, 4, 12), (34.0, 36.0, 10.0), 6, (0.7, 1.2), P["venom"], rr, avoid=lambda p: math.hypot(p.x, p.y) < 33.0 and p.z < 22.0, part="venom", stretch=1.0, max_count=6, sides=4)
    back_gear(P, -8.0, y0=-9.0, rr=rr, strap_m=P["leather"], metal=P["gold"])
    return K.quad_estimate()


BUILDERS["viper"] = (viper_shield, "legendary_shield__chimera_viper_shield", "chimera_viper_shield", {"weights": {"back": 0.4, "plate": 1.1, "serpent": 1.2, "head": 1.4, "jaw": 1.4}, "focus": (-8.0, 26.0)})

# ================================================================ 6. NEEDLE SHIELD
def hexcell(name, rc, h, mat, part, rr, depth=1.4, inset=0.8):
    """One honeycomb cell, a chamfered hexagonal prism standing proud of the surface: base ring (buried), full ring at the surface, a chamfered top ring and a flat top.
    The corners are jittered a little, so no two cells are twins. Built at the origin facing +z."""
    import bmesh
    bm = bmesh.new()
    rings = []
    for (z, k) in ((-depth, 0.97), (0.0, 1.0), (h * 0.7, 0.97), (h, inset)):
        rings.append([bm.verts.new((rc * k * math.cos(TAU * i / 6 + 0.5236) * (1.0 + rr.uniform(-0.05, 0.05)), rc * k * math.sin(TAU * i / 6 + 0.5236) * (1.0 + rr.uniform(-0.05, 0.05)), z + rr.uniform(-0.06, 0.06) * (z != -depth))) for i in range(6)])
    for r in range(3):
        for i in range(6):
            j = (i + 1) % 6
            bm.faces.new((rings[r][i], rings[r][j], rings[r + 1][j], rings[r + 1][i]))
    bm.faces.new(rings[3])
    bm.faces.new(rings[0][::-1])
    return K.mk_bm(name, bm, mat, part, False, True, 40.0)


def needle_shield():
    P = {}
    P["base"] = S("needle_base", rgb(38, 36, 40), 1.0, 0.5, wear=1.0, wear_color=rgb(150, 150, 156), dirt=1.0, dirt_color=(0.01, 0.008, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.6, bump_strength=0.25, mottle=1.0,
                  mottle_color=rgb(60, 36, 20), fine=0.6, seed=171, bevel=0.2, ao_dist=3.0)
    P["steel"] = S("needle_steel", rgb(200, 204, 212), 1.0, 0.22, wear=1.0, wear_color=rgb(252, 252, 255), dirt=0.6, dirt_color=(0.02, 0.015, 0.01, 1), scratches=0.9, bump="brushed", bump_scale=0.7, bump_strength=0.12, mottle=0.7,
                   mottle_color=rgb(120, 126, 140), fine=0.4, seed=172, bevel=0.12, ao_dist=2.0)
    P["amber"] = S("needle_amber", rgb(236, 150, 24), 0.0, 0.12, transmission=0.55, ior=1.3, bump="hammer", bump_scale=0.8, bump_strength=0.15, mottle=0.9, mottle_color=rgb(150, 70, 8), rim=(rgb(255, 214, 110), 0.55), wear=0.6, wear_color=rgb(255, 224, 140),
                   wear_metal=0.0, dirt=0.8, dirt_color=(0.1, 0.03, 0.0, 1), seed=173, bevel=0.12, ao_dist=1.5)
    P["amber2"] = S("needle_amber_deep", rgb(190, 100, 14), 0.0, 0.14, transmission=0.4, ior=1.3, mottle=0.9, mottle_color=rgb(110, 44, 6), rim=(rgb(255, 170, 70), 0.5), wear=0.6, wear_color=rgb(255, 200, 100), wear_metal=0.0, dirt=0.9,
                    dirt_color=(0.08, 0.02, 0.0, 1), seed=174, bevel=0.12, ao_dist=1.5)
    P["gold"] = S("needle_gold", rgb(210, 160, 60), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 232, 160), dirt=0.9, dirt_color=(0.04, 0.03, 0.0, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.2, mottle=0.8, mottle_color=rgb(110, 70, 20),
                  fine=0.5, seed=175, bevel=0.16)
    P["gem"] = S("needle_gem", rgb(40, 230, 100), 0.0, 0.12, emit=rgb(60, 255, 120), emit_strength=0.7, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(190, 255, 150), seed=176, bevel=0.0)
    P["leather"] = S("needle_leather", rgb(74, 50, 30), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(20, 12, 6), groove_amount=0.8, wear=0.8, wear_color=rgb(140, 100, 66), wear_metal=0.0, dirt=0.9, mottle=0.8,
                     mottle_color=rgb(40, 24, 12), seed=177, bevel=0.0)
    rr = K.rng(6606)
    R0 = 26.5

    def outline(a):
        r = R0 * (1.0 + 0.012 * math.sin(6.0 * a + 0.3))
        return (r * math.cos(a), r * math.sin(a))

    def zf(s, a):
        return 7.5 * (1.0 - s ** 2) ** 0.85 + 0.9 * math.exp(-((s - 0.9) / 0.06) ** 2) + 0.4 * K.noise3((s * math.cos(a) * 3, s * math.sin(a) * 3, 4.0), 1.0, 3, 2)

    def zb(s, a):
        return -3.0 - 6.0 * (1.0 - s ** 2)

    def dz(x, y):
        return zf(min(1.0, math.hypot(x, y) / R0), math.atan2(y, x))

    def nrm(x, y):
        e = 0.4
        return V(((dz(x - e, y) - dz(x + e, y)) / (2 * e), (dz(x, y - e) - dz(x, y + e)) / (2 * e), 1.0)).normalized()
    dish("nbody", outline, zf, zb, P["base"], ns=11, nb=3, nth=56, part="body", angle=60.0)
    # ---- the honeycomb: chamfered hexagonal cells stand on the face in a hex grid, taller toward the centre, amber and iron alternating, a needle in every cell
    rc, gap = 4.15, 0.55
    dx, dy = 2.0 * (rc + gap) * 0.866, 1.5 * (rc + gap)
    cells = []
    for row in range(-7, 8):
        for col in range(-7, 8):
            x = col * dx + (dx * 0.5 if row % 2 else 0.0)
            y = row * dy
            if math.hypot(x, y) < R0 - 3.2:
                cells.append((x, y))
    for ci, (x, y) in enumerate(cells):
        d = math.hypot(x, y)
        h = 1.6 + 1.4 * (1.0 - d / R0) + rr.uniform(0.0, 0.5)
        mat = P["amber"] if (ci % 3 != 0) else P["amber2"]
        c = hexcell("cell%d" % ci, rc, h, mat, "cells_amber" if mat is P["amber"] else "cells_deep", rr)
        n = nrm(x, y)
        K.xf(c, K.frame(V((x, y, dz(x, y))), n, (0, 1, 0)))
    # ---- a needle from nearly every cell: straight at the middle, flaring outward toward the rim, lengths in a rhythm of long / short
    cnt = 0
    for ci, (x, y) in enumerate(cells):
        d = math.hypot(x, y)
        if d < 5.0:
            continue
        if ci % 2 and d > 14.0:
            continue
        n = nrm(x, y)
        out = V((x, y, 0.0)).normalized() if d > 0.1 else V((0, 1, 0))
        flare = 0.12 + 0.75 * (d / R0) ** 1.6
        dr = (n + out * flare).normalized()
        base = V((x, y, dz(x, y))) + n * (1.5 + 1.4 * (1.0 - d / R0))
        ln = (7.0 + 9.0 * (1.0 - d / R0) + 4.0 * rr.random()) * (1.25 if ci % 3 == 0 else 0.8)
        mid = base + dr * ln * 0.5 + V((rr.uniform(-0.25, 0.25), rr.uniform(-0.25, 0.25), 0.0))
        tip = base + dr * ln + V((rr.uniform(-0.5, 0.5), rr.uniform(-0.5, 0.5), 0.0))
        K.tapered("needle%d" % cnt, [base, mid, tip], 0.8, 0.04, P["steel"], sides=5, sub=1, part="needles")
        cnt += 1
    # ---- the boss: a stepped, riveted steel collar with the emerald and a crown of long needles
    boss = K.bl.lathe("boss", [(7.6, -0.5), (8.0, 0.8), (7.4, 2.0), (6.4, 2.8), (6.6, 3.4), (5.4, 4.2), (4.8, 3.6)], P["steel"], sides=24, axis="z", caps=(False, False), part="boss")
    K._finish_obj(boss, "boss", True, 50.0)
    K.bl.move(boss, (0, 0, dz(0, 0) + 1.4))
    K.gem_setting("needle_jewel", (0.0, 0.0, dz(0, 0) + 5.6), 3.9, P["gem"], P["gold"], normal=(0, 0, 1), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
    for k in range(8):
        a = TAU * k / 8 + 0.2
        b0 = V((7.4 * math.cos(a), 7.4 * math.sin(a), dz(0, 0) + 3.4))
        K.tapered("crown%d" % k, [b0, b0 + V((3.0 * math.cos(a), 3.0 * math.sin(a), 6.0)), b0 + V((7.0 * math.cos(a), 7.0 * math.sin(a), 13.0 + 2.0 * (k % 2)))], 0.85, 0.05, P["steel"], sides=5, sub=1, part="crown")
    # ---- the rim: a steel hoop with studs, and long needles pointing out round the edge (shorter at the top and the bottom, so the shield stays in its size class)
    hoop = [V((outline(TAU * i / 56)[0], outline(TAU * i / 56)[1], 0.0)) for i in range(56)]
    band("hoop", hoop, [(-0.6, -2.6), (0.9, -2.6), (1.7, -1.2), (1.9, 0.0), (1.7, 1.4), (0.9, 2.8), (-0.6, 2.8)], P["steel"], part="hoop", angle=50.0)
    for i in range(0, 56, 2):
        a = TAU * i / 56
        o = outline(a)
        K.rivets("stud%d" % i, [(V((o[0] * 1.07, o[1] * 1.07, 1.4)), (math.cos(a), math.sin(a), 0.5))], P["gold"], r=0.8, detail=False, sides=5, rng_=rr, jitter=0.1)
    for k in range(17):
        a = TAU * (k + 0.5 * (k % 2)) / 17 + rr.uniform(-0.04, 0.04)
        o = outline(a)
        ln = 8.0 + 9.0 * abs(math.cos(a)) ** 1.4 + rr.uniform(-1.0, 1.0)
        d = V((math.cos(a), math.sin(a), 0.12))
        b0 = V((o[0] * 1.02, o[1] * 1.02, 0.0))
        K.tapered("rimneedle%d" % k, [b0 - d * 1.5, b0 + d * ln * 0.45, b0 + d * ln + V((0, 0, rr.uniform(-0.8, 1.2)))], 1.4, 0.05, P["steel"], sides=6, sub=1, part="rim_needles")
        col = K.bl.lathe("collar%d" % k, [(1.8, -1.0), (1.55, 0.0), (1.2, 1.6)], P["gold"], sides=6, axis="y", caps=(False, False), part="collars")
        K._finish_obj(col, "collars", True, 50.0)
        K.xf(col, K.frame(b0 + d * 0.4, d, (0, 0, 1)) @ Matrix.Rotation(math.radians(90), 4, "X"))
    # ---- honey running off the lower edge
    for i, ang in enumerate((232, 258, 292)):
        a = math.radians(ang)
        o = outline(a)
        K.drip("honey%d" % i, (o[0] * 1.02, o[1] * 1.02 - 2.0, 1.0), rr.uniform(4.0, 6.5), rr.uniform(1.0, 1.5), P["amber"], seed=90 + i, sides=7, part="honey")
    back_gear(P, -9.0, y0=-9.0, rr=rr, strap_m=P["leather"], metal=P["steel"])
    return K.quad_estimate()


BUILDERS["needle"] = (needle_shield, "legendary_shield__needle_shield", "needle_shield", {"weights": {"back": 0.4, "body": 0.8, "cells": 1.2, "needles": 1.3}, "focus": (-12.0, 24.0)})

# ================================================================ 7. LIGHT METAL SHIELD
def lightmetal_shield():
    P = {}
    P["silver"] = S("lm_silver", rgb(222, 228, 238), 1.0, 0.16, wear=1.0, wear_color=rgb(255, 255, 255), dirt=0.5, dirt_color=(0.03, 0.04, 0.05, 1), scratches=0.9, bump="brushed", bump_scale=0.8, bump_strength=0.1, mottle=0.8,
                    mottle_color=rgb(150, 190, 214), fine=0.35, seed=181, bevel=0.2, ao_dist=3.0)
    P["silver2"] = S("lm_silver_dark", rgb(150, 164, 184), 1.0, 0.22, wear=1.0, wear_color=rgb(238, 244, 255), dirt=0.8, dirt_color=(0.02, 0.03, 0.04, 1), scratches=0.9, bump="brushed", bump_scale=0.7, bump_strength=0.15, mottle=0.9,
                     mottle_color=rgb(80, 110, 150), fine=0.4, seed=182, bevel=0.16, ao_dist=2.5)
    P["gold"] = S("lm_gold", rgb(226, 196, 110), 1.0, 0.26, wear=0.9, wear_color=rgb(255, 244, 190), dirt=0.8, dirt_color=(0.03, 0.025, 0.0, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.18, mottle=0.8,
                  mottle_color=rgb(150, 110, 50), fine=0.5, seed=183, bevel=0.16)
    P["cyan"] = S("lm_cyan", rgb(90, 220, 255), 0.0, 0.1, emit=rgb(90, 230, 255), emit_strength=0.9, emit_flicker=0.3, mottle=0.6, mottle_color=rgb(200, 255, 255), seed=184, bevel=0.0)
    P["gem"] = S("lm_gem", rgb(80, 170, 255), 0.0, 0.1, emit=rgb(100, 190, 255), emit_strength=0.7, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(210, 240, 255), seed=185, bevel=0.0)
    P["leather"] = S("lm_leather", rgb(58, 62, 78), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(16, 18, 26), groove_amount=0.8, wear=0.8, wear_color=rgb(120, 126, 150), wear_metal=0.0, dirt=0.9, mottle=0.8,
                     mottle_color=rgb(30, 34, 44), seed=186, bevel=0.0)
    rr = K.rng(7707)
    kite = [(0, 34), (13, 28), (24, 18), (27, 6), (23, -8), (13, -22), (3, -30), (0, -34), (-3, -30), (-13, -22), (-23, -8), (-27, 6), (-24, 18), (-13, 28)]
    outline = poly_outline(kite, smooth=8)
    NF = 20

    def zf(s, a):
        fl = 1.7 * math.cos(NF * a) * smoothstep(0.22, 0.62, s) * (1.0 - smoothstep(0.78, 0.86, s))
        return 6.5 * (1.0 - s ** 1.7) + fl + 1.3 * math.exp(-((s - 0.92) / 0.05) ** 2)

    def zb(s, a):
        return -2.8 - 5.0 * (1.0 - s ** 2)
    dish("lmbody", outline, zf, zb, P["silver"], ns=13, nb=3, nth=52, part="body", angle=55.0)
    # borders: a rounded steel bead on the rim, a second bead inside it, and the glowing inlay between them (all real swept geometry)
    n = 44
    band("lm_bead_out", loop_pts(outline, 0, n=n, s=0.965, zf=lambda a: zf(0.965, a) + 0.45), rope_section(1.35, 6, 0.9), P["silver2"], part="beads", angle=55.0)
    band("lm_bead_in", loop_pts(outline, 0, n=n, s=0.84, zf=lambda a: zf(0.84, a) + 0.35), rope_section(0.95, 6, 0.9), P["gold"], part="beads", angle=55.0)
    band("lm_inlay", loop_pts(outline, 0, n=n, s=0.9, zf=lambda a: zf(0.9, a) + 0.5), rope_section(0.6, 5, 0.8), P["cyan"], part="inlay", angle=55.0)
    K.rivets("lm_rivets", [(V((outline(TAU * i / n)[0] * 0.965, outline(TAU * i / n)[1] * 0.965, zf(0.965, TAU * i / n) + 1.5)), (0, 0, 1)) for i in range(0, n, 4)], P["gold"], r=0.8, detail=False, sides=6, rng_=rr, jitter=0.05)
    # twelve raised gold rays run from the star to the inlay, long and short in turn (real ridges standing on the fluted face)
    for k in range(12):
        a = TAU * (k + 0.5) / 12
        o = outline(a)
        f0, f1 = (0.34, 0.8) if k % 2 == 0 else (0.45, 0.66)
        p0 = V((o[0] * f0, o[1] * f0, zf(f0, a) + 0.5))
        p1 = V((o[0] * f1, o[1] * f1, zf(f1, a) + 0.5))
        K.bar("lm_ray%d" % k, p0, p1, 1.0, 0.7, P["gold"], part="rays", taper=0.25, bevel_w=0.2)
    # central star boss: stacked, bevelled star plates, a cyan core ring and the gem
    z0 = zf(0.0, 0.0) + 0.2
    star = [((11.0 if k % 2 == 0 else 5.2) * math.cos(TAU * k / 16 + 1.5708), (11.0 if k % 2 == 0 else 5.2) * math.sin(TAU * k / 16 + 1.5708)) for k in range(16)]
    K.stepped_plate("lm_star", star, z0, P["silver2"], steps=3, step_h=0.7, inset=1.5, part="star", bevel_w=0.18)
    K.stepped_plate("lm_star_gold", [(p[0] * 0.5, p[1] * 0.5) for p in star], z0 + 2.1, P["gold"], steps=2, step_h=0.6, inset=0.7, part="star", bevel_w=0.15)
    ring = K.bl.lathe("lm_corering", [(4.6, 0.0), (4.9, 0.6), (4.3, 1.2), (3.9, 0.8)], P["cyan"], sides=24, axis="z", caps=(False, False), part="inlay")
    K._finish_obj(ring, "inlay", True, 50.0)
    K.bl.move(ring, (0, 0, z0 + 3.0))
    K.gem_setting("lm_jewel", (0.0, 0.0, z0 + 4.4), 3.0, P["gem"], P["gold"], normal=(0, 0, 1), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.4)
    # fluted wings: layered blades sweep out from both sides, like a wing of light metal
    for sx in (-1, 1):
        for k in range(5):
            base = V((sx * (22.0 + 0.6 * k), 8.0 - 5.2 * k, 1.0 - 0.8 * k))
            d = V((sx * 1.0, 0.55 - 0.28 * k, -0.15 - 0.05 * k))
            K.feather("lm_wing%d_%d" % (sx > 0, k), base, d, 25.0 - 3.0 * k + 1.5 * (k % 2), 6.2 - 0.4 * k, P["silver2" if k % 2 else "silver"], shaft_material=P["gold"], curl=0.7, serrate=0.1, rows=7, thick=0.45,
                      seed=k + 7 * sx, part="wings", ridge=1.8)
    # three small kite shields hover ahead of the face, each with a glowing core
    for i, (ox, oy, sc) in enumerate(((-33.0, 20.0, 0.2), (34.0, 10.0, 0.17), (-31.0, -14.0, 0.15))):
        pts = [(ox + sc * x, oy + sc * y) for x, y in ((0, 34), (24, 18), (27, 0), (13, -22), (0, -34), (-13, -22), (-27, 0), (-24, 18))]
        K.stepped_plate("lm_mini%d" % i, pts, 4.0 + i, P["silver"], steps=2, step_h=0.6, inset=0.5, part="minis", bevel_w=0.14)
        K.stepped_plate("lm_minic%d" % i, [(ox + sc * 0.45 * x, oy + sc * 0.45 * y) for x, y in ((0, 34), (24, 18), (27, 0), (13, -22), (0, -34), (-13, -22), (-27, 0), (-24, 18))], 5.5 + i, P["cyan"], steps=1, step_h=0.4, part="minis", bevel_w=0.08)
    back_gear(P, -7.8, y0=-9.0, rr=rr, strap_m=P["leather"], metal=P["silver2"])
    return K.quad_estimate()


BUILDERS["lightmetal"] = (lightmetal_shield, "legendary_shield__light_metal_shield", "light_metal_shield", {"weights": {"back": 0.4, "body": 1.0, "wings": 1.0, "star": 1.3}, "glow": ("A8F0FF", 0.12, 0.45, 1.0), "focus": (-12.0, 24.0)})


# ================================================================ 8. SOUL EATER SHIELD
def small_skull(name, pos, s, P, rr, face=(0, 0, 1)):
    K.blob(name + "_cran", pos, (1.9 * s, 1.7 * s, 1.8 * s), P["bone"], seed=int(abs(pos.x * 7 + pos.y)), lump=0.1, rings=7, segs=9, part="skulls", squash=(1, 1, 1))
    K.blob(name + "_jaw", pos + V((0, -1.7 * s, 0.5 * s)), (1.2 * s, 0.7 * s, 1.1 * s), P["bone"], seed=3, lump=0.1, rings=6, segs=8, part="skulls")
    for sx in (-1, 1):
        K.blob("%s_eye%d" % (name, sx), pos + V((sx * 0.7 * s, -0.1 * s, 1.5 * s)), (0.5 * s, 0.55 * s, 0.4 * s), P["dark"], seed=5, lump=0.05, rings=4, segs=6, part="skulls_eyes")


def souleater_shield():
    P = {}
    P["armor"] = S("se_armor", rgb(34, 30, 42), 1.0, 0.4, wear=1.0, wear_color=rgb(160, 150, 175), dirt=1.0, dirt_color=(0.01, 0.005, 0.015, 1), scratches=0.8, bump="hammer", bump_scale=0.7, bump_strength=0.3, mottle=1.0,
                   mottle_color=rgb(60, 24, 70), fine=0.5, seed=191, bevel=0.2, ao_dist=3.0)
    P["bone"] = S("se_bone", rgb(220, 210, 184), 0.0, 0.42, bump="grain", bump_scale=1.4, bump_strength=0.3, mottle=0.9, mottle_color=rgb(140, 110, 70), wear=0.7, wear_color=rgb(250, 244, 224), wear_metal=0.0, dirt=0.9,
                  dirt_color=(0.05, 0.03, 0.02, 1), seed=192, bevel=0.1)
    P["gum"] = S("se_gum", rgb(110, 24, 52), 0.0, 0.32, bump="leather", bump_scale=0.7, bump_strength=0.4, mottle=0.9, mottle_color=rgb(50, 8, 30), rim=(rgb(220, 90, 120), 0.35), seed=193, bevel=0.0, dirt=0.8, dirt_color=(0.04, 0.0, 0.02, 1))
    P["throat"] = S("se_throat", rgb(110, 40, 200), 0.0, 0.2, emit=rgb(150, 70, 255), emit_strength=1.0, emit_flicker=0.5, mottle=0.9, mottle_color=rgb(210, 150, 255), seed=194, bevel=0.0)
    P["eye"] = S("se_eye", rgb(200, 60, 255), 0.0, 0.1, emit=rgb(190, 80, 255), emit_strength=1.0, emit_flicker=0.4, mottle=0.7, mottle_color=rgb(255, 180, 255), seed=195, bevel=0.0)
    P["wisp"] = S("se_wisp", rgb(150, 220, 255), 0.0, 0.1, emit=rgb(140, 220, 255), emit_strength=0.85, emit_flicker=0.6, mottle=0.7, mottle_color=rgb(230, 255, 255), seed=196, bevel=0.0)
    P["horn"] = S("se_horn", rgb(46, 36, 40), 0.0, 0.38, bump="grain", bump_scale=2.0, bump_strength=0.5, mottle=1.0, mottle_color=rgb(120, 90, 80), wear=0.8, wear_color=rgb(190, 170, 150), wear_metal=0.0, dirt=0.8, seed=197, bevel=0.1)
    P["dark"] = S("se_dark", rgb(6, 4, 8), 0.0, 0.08, seed=198, bevel=0.0)
    P["leather"] = S("se_leather", rgb(48, 38, 44), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(14, 8, 12), groove_amount=0.8, wear=0.8, wear_color=rgb(110, 84, 90), wear_metal=0.0, dirt=0.9, mottle=0.8,
                     mottle_color=rgb(24, 16, 20), seed=199, bevel=0.0)
    rr = K.rng(8808)
    R0 = 27.0
    S0 = 0.5

    def outline(a):
        r = R0 * (1.0 + 0.035 * math.cos(8.0 * a) + 0.02 * math.sin(3.0 * a + 0.5))
        return (r * math.cos(a), r * math.sin(a))

    def zf(s, a):
        t = (s - S0) / (1.0 - S0)
        return 8.0 - 6.6 * t ** 0.85 + 1.4 * math.exp(-((s - 0.965) / 0.04) ** 2) + 0.8 * math.cos(8 * a) * math.exp(-((s - 0.78) / 0.1) ** 2)

    def zb(s, a):
        return -2.5 - 4.0 * ((s - S0) / (1.0 - S0))
    dish("sebody", outline, zf, zb, P["armor"], ns=12, nb=3, nth=56, part="body", angle=55.0, s0=S0)
    RL = R0 * S0
    # the maw: an emissive funnel throat deep in the ring, a gum band round its lip, two rows of teeth
    # the throat is a thin closed funnel wall (a swept closed section), so its inner face is lit from the maw side and its outer face from behind
    tp = [(RL - 0.4, 7.2), (RL - 1.4, 3.0), (RL * 0.45, -2.5), (1.4, -5.5)]
    sect = [(r - RL - 0.35, z) for r, z in tp] + [(r - RL + 0.35, z) for r, z in tp[::-1]]
    band("se_throat", [V((RL * math.cos(TAU * i / 24), RL * math.sin(TAU * i / 24), 0.0)) for i in range(24)], sect, P["throat"], part="throat", angle=50.0)
    band("se_gum", loop_pts(outline, 0, n=48, s=S0 * 1.0, zf=lambda a: zf(S0, a) + 0.1), rope_section(1.9, 6, 0.8), P["gum"], part="gum", angle=55.0)
    nf = 16
    for k in range(nf):
        a = TAU * (k + 0.5) / nf
        c, s_ = math.cos(a), math.sin(a)
        big = (k % 2 == 0)
        base = V((RL * c * 1.04, RL * s_ * 1.04, zf(S0, a) + 0.9))
        ln = (8.0 + 3.0 * abs(math.sin(2 * a)) + rr.uniform(-0.8, 0.8)) if big else (4.0 + rr.uniform(-0.5, 0.5))
        inward = V((-c, -s_, 0.0))
        d = (inward * (0.62 if big else 0.4) + V((0, 0, 0.85))).normalized()
        mid = base + d * ln * 0.55 + inward * 0.7
        tip = base + d * ln + inward * 2.4
        K.tapered("fang%d" % k, [base - V((0, 0, 0.8)), mid, tip], (1.75 if big else 1.0), 0.05, P["bone"], sides=6, sub=2, part="fangs")
    # a row of small teeth between the big fangs, set in the gum
    for k in range(nf * 2):
        a = TAU * (k + 0.25) / (nf * 2)
        c, s_ = math.cos(a), math.sin(a)
        base = V((RL * c * 1.02, RL * s_ * 1.02, zf(S0, a) + 1.0))
        K.tapered("tooth%d" % k, [base, base + V((-c * 0.5, -s_ * 0.5, 1.6)), base + V((-c * 1.3, -s_ * 1.3, 2.9))], 0.7, 0.05, P["bone"], sides=4, sub=1, part="fangs")
    # two glowing slit eyes under armoured brows
    for sx in (-1, 1):
        ec = V((sx * 10.5, 16.0, 0.0))
        ec.z = zf(math.hypot(ec.x, ec.y) / R0, math.atan2(ec.y, ec.x)) + 0.4
        K.blob("se_eye%d" % sx, ec, (4.0, 1.7, 1.4), P["eye"], seed=7 + sx, lump=0.04, rings=8, segs=12, part="eyes")
        K.blob("se_pupil%d" % sx, ec + V((0, 0, 1.0)), (0.55, 1.5, 0.5), P["dark"], seed=9, lump=0.03, rings=6, segs=8, part="eyes")
        K.bar("se_brow%d" % sx, V((sx * 4.0, 18.0 + 0.2, ec.z + 1.6)), V((sx * 17.0, 20.5, ec.z + 1.2)), 2.4, 1.8, P["armor"], part="brows", bevel_w=0.3)
    # horns, bone spikes round the rim, four small skulls on the face
    for sx in (-1, 1):
        K.tapered("se_horn%d" % sx, [V((sx * 14.0, 22.0, 2.0)), V((sx * 21.0, 28.0, 1.0)), V((sx * 22.0, 34.0, -1.0)), V((sx * 17.0, 39.0, -3.0))], 3.2, 0.1, P["horn"], sides=8, sub=3, part="horns")
        K.tapered("se_horn_low%d" % sx, [V((sx * 22.0, 6.0, 1.0)), V((sx * 29.0, 4.0, -1.0)), V((sx * 33.0, -2.0, -3.0))], 2.0, 0.1, P["horn"], sides=7, sub=2, part="horns")
    for k in range(14):
        a = TAU * (k + 0.5) / 14
        o = outline(a)
        d = V((math.cos(a), math.sin(a), 0.25)).normalized()
        b0 = V((o[0] * 0.99, o[1] * 0.99, zf(0.99, a) + 0.4))
        ln = 4.0 + 1.6 * (1 + math.sin(a * 3)) * 0.5 + rr.uniform(0, 1.0) if math.sin(a) > -0.3 else 3.0
        K.tapered("rimspike%d" % k, [b0, b0 + d * ln * 0.5, b0 + d * ln], 1.5, 0.06, P["bone"], sides=6, sub=1, part="spikes")
    for i, ang in enumerate((40, 140, 220, 320)):
        a = math.radians(ang)
        r_ = R0 * 0.8
        p = V((r_ * math.cos(a), r_ * math.sin(a), 0.0))
        p.z = zf(0.8, a) + 1.8
        small_skull("se_skull%d" % i, p, 1.0 if i % 2 else 0.85, P, rr)
    # soul wisps rising out of the maw, thin glowing tubes that curl and thin to nothing
    for i in range(6):
        a = TAU * i / 6 + 0.4
        c, s_ = math.cos(a), math.sin(a)
        p0 = V((c * 5.0, s_ * 5.0, 1.0))
        path = [p0, p0 + V((c * 3 + 1.5, s_ * 3, 7.0)), p0 + V((c * 7 - 2.0, s_ * 6 + 1.0, 13.0 + i)), p0 + V((c * 9 + 1.0, s_ * 8 - 1.5, 19.0 + i))]
        K.tapered("wisp%d" % i, path, 0.8, 0.05, P["wisp"], sides=5, sub=3, part="wisps")
    back_gear(P, -7.0, y0=-9.0, rr=rr, strap_m=P["leather"], metal=P["armor"])
    return K.quad_estimate()


BUILDERS["souleater"] = (souleater_shield, "legendary_shield__soul_eater_shield", "soul_eater_shield", {"weights": {"back": 0.4, "body": 1.0, "fangs": 1.3, "eyes": 1.2, "skulls": 1.2}, "glow": ("C890FF", 0.12, 0.45, 1.0), "focus": (-12.0, 28.0)})
# FORMS_END

if __name__ == "__main__":
    main(sys.argv[1:])

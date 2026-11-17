"""Weapon-form models of the LEGENDARY SPEAR, built with tools/blender/forms_kit.py (one model per form of data/anime_multiverse/hero/forms/legendary_spear.json).

    python3 -B tools/blender/forms_legendary_spear.py [--only hunter,filolial,...] [--trial DIR] [--quick] [--samples N] [--bake PX] [--skin PX]

mesh ids (docs/heroweapon/form_models.md): legendary_spear__<form>. The Lust forms are curse forms and keep the existing legendary_spear_curse look.
Every form keeps the base spear's `place` line (same hand, same size class) and its length (y 0 .. ~335 cm). Everything is seeded.

Quality rules of this family (owner: "real sculpted 3D objects, not a picture painted on a flat plate"): shape and depth come from geometry (stepped, bevelled,
overlapping, curved parts that shade themselves); no single flat cards (every feather / flame / membrane has thickness and curl); skins are 1024 px (bake at 2x,
boxed down); the shaft gets a lower share of the skin than the head (per-part texel weights), so the head is crisp.
"""
import json
import math
import os
import shutil
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402
import bmesh  # noqa: E402
import numpy as np  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

bl = K.bl
BASE = "legendary_spear"
HERE = os.path.dirname(os.path.abspath(__file__))
MANIFEST = os.path.join(HERE, "forms_manifest_legendary_spear.json")
PREVIEW_DIR = os.path.join(K.ROOT, "docs", "heroweapon", "form_previews")
K.MAX_PNG_KB = 350

# share of the skin (linear UV scale) per part-name prefix: the shaft is a long, plain cylinder, the head is what the eye reads
WEIGHTS = [("shaft", 0.42), ("grip", 0.7), ("butt", 0.75), ("head", 1.35), ("socket", 1.1)]


def part_weight(name):
    for pre, w in WEIGHTS:
        if name.startswith(pre):
            return w
    return 1.0


# ================================================================ driver: bake with per-part texel density
def begin(samples):
    sc = K.begin(samples)
    sc.render.threads_mode = "FIXED"
    sc.render.threads = 2
    return sc


def lathe(name, prof, mat, part, sides=10, angle=42.0, squash=1.0, caps=(True, True), origin=(0, 0, 0), axis="y"):
    ob = bl.lathe(name, prof, mat, sides=sides, part=part, squash=squash, caps=caps, origin=origin, axis=axis)
    return K._finish_obj(ob, part, True, angle)


def _uv_weight(low, names):
    """Scales every UV island about its centre by its part's weight (after the even-density unwrap, before packing)."""
    bm = bmesh.from_edit_mesh(low.data)
    uvl = bm.loops.layers.uv.active
    pid = bm.faces.layers.int["part_id"]
    bm.faces.ensure_lookup_table()
    seen = set()

    def same(a, b):
        return abs(a[0] - b[0]) < 1e-5 and abs(a[1] - b[1]) < 1e-5
    for f0 in bm.faces:
        if f0.index in seen:
            continue
        seen.add(f0.index)
        isl, stack = [f0], [f0]
        while stack:
            f = stack.pop()
            for l in f.loops:
                a, b = l.vert, l.link_loop_next.vert
                ua, ub = l[uvl].uv, l.link_loop_next[uvl].uv
                for g in l.edge.link_faces:
                    if g is f or g.index in seen:
                        continue
                    ok = False
                    for m in g.loops:
                        if m.vert is b and m.link_loop_next.vert is a:
                            ok = same(m[uvl].uv, ub) and same(m.link_loop_next[uvl].uv, ua)
                        elif m.vert is a and m.link_loop_next.vert is b:
                            ok = same(m[uvl].uv, ua) and same(m.link_loop_next[uvl].uv, ub)
                        if ok:
                            break
                    if ok:
                        seen.add(g.index)
                        isl.append(g)
                        stack.append(g)
        w = part_weight(names[f0[pid]])
        if abs(w - 1.0) < 1e-3:
            continue
        pts = [l[uvl].uv for f in isl for l in f.loops]
        cx = sum(p[0] for p in pts) / len(pts)
        cy = sum(p[1] for p in pts) / len(pts)
        done = set()
        for f in isl:
            for l in f.loops:
                key = id(l)
                u = l[uvl].uv
                u[0], u[1] = cx + (u[0] - cx) * w, cy + (u[1] - cy) * w
    bmesh.update_edit_mesh(low.data)


def finish_w(item_id, base_id, trial=None, bake=2048, skin=1024, cage=0.5, glow=("FFFFFF", 0.12, 0.45, 1.0), colors=256):
    """bl.finish with per-part texel weights (head crisp, shaft coarse) and a 1024 skin. Writes the mesh + skin + glow like bl.finish; then the kit's PNG pack."""
    t0 = time.time()
    mesh_dir = os.path.join(trial, "weapon_meshes") if trial else bl.MESH_DIR
    skin_dir = os.path.join(trial, "textures", "entity", "weapon") if trial else bl.SKIN_DIR
    os.makedirs(mesh_dir, exist_ok=True)
    os.makedirs(skin_dir, exist_ok=True)
    ml = bl.ml
    sc = bpy.context.scene
    lows = [o for o in sc.collection.objects if o.type == "MESH" and not o.get("detail")]
    details = [o for o in sc.collection.objects if o.type == "MESH" and o.get("detail")]
    names = []
    for o in lows:
        if o["part"] not in names:
            names.append(o["part"])
    bl._tag_parts(lows, names)
    low = bl._join(lows, item_id)
    bl._clean(low)
    low.data.shade_smooth()
    low.data.set_sharp_from_angle(angle=math.radians(38.0))
    bl._select([low], low)
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.uv.smart_project(angle_limit=math.radians(66), island_margin=0.0, area_weight=0.0, scale_to_bounds=False)
    bpy.ops.uv.average_islands_scale()
    _uv_weight(low, names)
    bpy.ops.uv.pack_islands(margin=0.003, rotate=True)
    bpy.ops.object.mode_set(mode="OBJECT")
    img, gimg = bl._image(item_id + "_skin", bake), bl._image(item_id + "_glow", bake)
    nodes = []
    for m in [s.material for s in low.material_slots if s.material]:
        n = m.node_tree.nodes.new("ShaderNodeTexImage")
        n.image = img
        m.node_tree.nodes.active = n
        nodes.append(n)
    for o in lows + details:
        o.hide_render = True
    kw = dict(margin=8, use_clear=True)
    if details:
        high = bl._join(lows + details, item_id + "_high")
        bl._select([high, low], low)
        kw.update(use_selected_to_active=True, cage_extrusion=cage, max_ray_distance=cage * 3)
    else:
        bl._select([low], low)
    bpy.ops.object.bake(type="COMBINED", **kw)
    for n in nodes:
        n.image = gimg
    bpy.ops.object.bake(type="EMIT", **kw)
    atlas = ml.Atlas(skin)
    atlas.img = bl._pixels(img, bake, skin)
    atlas.img[..., 3] = np.where(atlas.img[..., 3] > 8, 255.0, 0.0)
    g = bl._pixels(gimg, bake, skin)
    lum = g[..., :3].max(axis=2)
    alpha = np.clip(lum * 1.25, 0, 255) * (g[..., 3] > 8)
    atlas.glow = np.concatenate([g[..., :3] * (alpha[..., None] > 2), alpha[..., None]], axis=2).astype(np.float32)

    mesh = ml.Mesh(item_id)
    parts = [ml.Part(n, smooth=38.0) for n in names]
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
    mesh.finish = lambda: None
    mesh.set_glow(*glow)
    mesh.orient_cfg = "manual"
    with open(os.path.join(bl.MESH_DIR, base_id + ".txt")) as fh:
        for line in fh:
            if line.startswith("place "):
                mesh.place(*[float(x) for x in line.split()[1:6]])
                break
    stats = mesh.stats(atlas)
    print("%s: %d parts, %d quad lines, box %s .. %s" % (item_id, len(mesh.parts), sum(stats["quads"].values()), stats["bbox_min"], stats["bbox_max"]))
    mesh.write_txt(os.path.join(mesh_dir, item_id + ".txt"), atlas, normals="vertex", comment="made in Blender by tools/blender (forms_legendary_spear.py)")
    from PIL import Image
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(skin_dir, item_id + ".png"), optimize=True)
    Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(skin_dir, item_id + "_glow.png"), optimize=True)
    raw = os.path.join(trial or bl.ROOT, "_raw_bake") if trial else os.path.join(os.environ.get("TMPDIR", "/tmp"), "spear_raw_bake")
    os.makedirs(raw, exist_ok=True)
    for f in (item_id + "_glow.png", item_id + ".png"):
        shutil.copy(os.path.join(skin_dir, f), os.path.join(raw, f))
    glow_kb = pack_fit(os.path.join(skin_dir, item_id + "_glow.png"), 70.0)       # glow first: the skin gets what is left of the 350 KB (K.MAX_PNG_KB)
    skin_kb = pack_fit(os.path.join(skin_dir, item_id + ".png"), K.MAX_PNG_KB - 4.0 - glow_kb)
    if skin_kb + glow_kb > K.MAX_PNG_KB and not trial:                                # a noisy bake: box both PNGs down to 768 px and pack again
        repack(item_id, base_id, size=768)
    return K.report(item_id, base_id, mesh_dir, skin_dir, mesh, atlas, time.time() - t0), mesh_dir, skin_dir


def pack_fit(path, budget_kb):
    """Packs a skin with the kit's palette pack: smaller palettes (and a stronger denoise) until it fits `budget_kb` (skin + glow share the 350 KB limit)."""
    src = path + ".src"
    shutil.copy(path, src)
    for denoise in (0.75, 1.0):
        for colors in (256, 192, 160, 128, 112, 96, 80, 64, 56, 48, 40, 32):
            shutil.copy(src, path)
            b, a_, ps = K.pack_png(path, colors=colors, denoise=denoise)
            if a_ <= budget_kb:
                break
        if a_ <= budget_kb:
            break
    os.remove(src)
    print("[pack] %s %.0f KB -> %.0f KB with %d colours, denoise %.2f (psnr %.1f dB)" % (os.path.basename(path), b, a_, colors, denoise, ps))
    return a_


def repack(item_id, base_id, glow_budget=70.0, size=None):
    """Re-runs the PNG pack on the raw bake of an already built form (no re-bake): glow first, the skin gets the rest of the 350 KB. `size` boxes the
    skin and glow down (the mesh UVs are in 128 space, so any square size works) when even a 32-colour palette of the noisy bake does not fit."""
    from PIL import Image
    raw = os.path.join(os.environ.get("TMPDIR", "/tmp"), "spear_raw_bake")
    for f in (item_id + ".png", item_id + "_glow.png"):
        if size:
            Image.open(os.path.join(raw, f)).convert("RGBA").resize((size, size), Image.BOX).save(os.path.join(bl.SKIN_DIR, f))
        else:
            shutil.copy(os.path.join(raw, f), os.path.join(bl.SKIN_DIR, f))
    g = pack_fit(os.path.join(bl.SKIN_DIR, item_id + "_glow.png"), glow_budget)
    pack_fit(os.path.join(bl.SKIN_DIR, item_id + ".png"), K.MAX_PNG_KB - 4.0 - g)
    return K.report(item_id, base_id, bl.MESH_DIR, bl.SKIN_DIR)


# ================================================================ shape check: a Workbench render of the scene (seconds, no bake)
def shape_view(prefix, focus=(205.0, 340.0), views=("front", "three_quarter", "side"), size=(520, 700), full=False, samples=10):
    """Fast clay renders (Cycles, few samples, flat material colours) of the build so far: silhouette and depth of every part, before the long bake.
    Destroys the materials of the scene: use it only in --shape runs."""
    from PIL import Image
    for m in bpy.data.materials:
        col, emit = (0.5, 0.5, 0.5, 1.0), 0.0
        for n in m.node_tree.nodes:
            if n.type == "RGB":
                col = n.outputs[0].default_value[:]
                break
        bs = m.node_tree.nodes["Principled BSDF"]
        if m.get("glow"):
            emit = 0.7
        m.node_tree.nodes.clear()
        out = m.node_tree.nodes.new("ShaderNodeOutputMaterial")
        bs = m.node_tree.nodes.new("ShaderNodeBsdfPrincipled")
        bs.inputs["Base Color"].default_value = col
        bs.inputs["Roughness"].default_value = 0.45
        bs.inputs["Metallic"].default_value = 0.15
        if emit:
            bs.inputs["Emission Color"].default_value = col
            bs.inputs["Emission Strength"].default_value = emit
        m.node_tree.links.new(bs.outputs[0], out.inputs[0])
    y0, y1 = (0.0, 340.0) if full else focus
    outs = []
    for v in views:
        path = "%s_%s.png" % (prefix, v)
        K.beauty(path, view=v, size=size, samples=samples, focus=(0, (y0 + y1) / 2, 0, (y1 - y0) * (1.02 if not full else 0.55)), details=False)
        outs.append(Image.open(path).convert("RGB"))
    sheet = Image.new("RGB", (sum(i.width for i in outs), outs[0].height))
    x = 0
    for i in outs:
        sheet.paste(i, (x, 0))
        x += i.width
    sheet.save(prefix + "_sheet.png")
    return prefix + "_sheet.png"


# ================================================================ small shared helpers
def ring_band(name, y, r, h, mat, part, sides=10, sq=1.0, lip=0.18):
    return lathe(name, [(r - 0.12, y - h), (r + lip, y - h * 0.7), (r + lip * 1.2, y), (r + lip, y + h * 0.7), (r - 0.12, y + h)], mat, part, sides, 50.0, sq, (False, False))


def claw_gem(name, y, r, gem_mat, metal, rr, part, claws=4, sides=8, stretch=1.25, z=0.0, claw_len=1.0, claw_r=0.55, collar=True, csides=5, csub=2):
    """A faceted stone standing in `claws` real, bent claws (the base spear's red jewel, kept in every form)."""
    prof = [(0.01, y - r * stretch), (r * 0.62, y - r * 0.72 * stretch), (r, y - r * 0.12), (r, y + r * 0.12), (r * 0.62, y + r * 0.72 * stretch), (0.01, y + r * stretch)]
    g = lathe(name, prof, gem_mat, part, sides, 22.0)
    if z:
        bl.move(g, (0, 0, z))
    for k in range(claws):
        a = TAU * (k + rr.uniform(-0.1, 0.1)) / claws + TAU / (2 * claws)
        c, s = math.cos(a), math.sin(a)
        pts = [V((r * 0.95 * c, y - r * stretch * 0.95, r * 0.95 * s + z)), V((r * 1.32 * c, y - r * 0.35, r * 1.32 * s + z)), V((r * 1.3 * c, y + r * 0.55, r * 1.3 * s + z)),
               V((r * 0.72 * c, y + r * stretch * 1.02 * claw_len, r * 0.72 * s + z))]
        K.tapered("%s_claw%d" % (name, k), pts, claw_r, 0.12, metal, sides=csides, sub=csub, part=part + "_claws")
    if collar:
        ring_band(name + "_collar", y - r * stretch * 0.9, r * 0.95, 0.5, metal, part + "_claws", sides=sides * 2)


def hanging(name, top, length, width, mat, rr, part, sway=2.0, fray=0.7, notch=0.0, face=(0, 0, 1), n=9, wave=0.1, thick=0.1):
    """A cloth / hide / feather-less strip hanging from `top`: a gently swinging path, ragged end."""
    path = []
    ph = rr.uniform(0, 3)
    for k in range(n):
        t = k / (n - 1)
        path.append(V((top[0] + sway * math.sin(t * 2.2 + ph) * t, top[1] - length * t, top[2] + sway * 0.5 * math.sin(t * 3.1 + ph) * t)))
    return K.ribbon(name, path, (lambda t: width * (1.0 - 0.35 * t) + 0.2), mat, facing=face, thick=thick, part=part, fray=fray, rng_=rr, edge_wave=wave, notch=notch)


def sweep_arc(name, pts, r0, r1, mat, part, sides=6, sub=3, bumps=0.0, seed=0):
    return K.tube(name, [tuple(p) for p in pts], r0, mat, sides=sides, sub=sub, part=part, taper=(r0, r1), bumps=bumps, seed=seed)


def facet_lathe(name, prof, mat, part, sides=6, rot=0.0):
    """A faceted crystal / prism: a lathe with few sides and hard edges (every facet shades itself)."""
    ob = bl.lathe(name, prof, mat, sides=sides, part=part)
    if rot:
        bl.move(ob, rot=(0, math.degrees(rot), 0))
    return K._finish_obj(ob, part, True, 18.0)


def place_obj(ob, loc=(0, 0, 0), rot=(0, 0, 0), scale=(1, 1, 1)):
    return bl.move(ob, loc, rot, scale)


# ================================================================ 1. HUNTER'S SPEAR: bone, sinew and flint
def hunter_palette():
    P = {}
    P["bark"] = K.surface("bark", rgb(96, 66, 38), 0.0, 0.82, bump="grain", bump_scale=1.0, bump_strength=0.9, groove=rgb(30, 18, 10), groove_amount=0.8, mottle=1.0, mottle_color=rgb(52, 34, 18),
                          wear=0.6, wear_color=rgb(168, 128, 84), wear_metal=0.0, dirt=0.9, dirt_color=(0.012, 0.007, 0.004, 1), fine=0.5, seed=1, bevel=0.0, ao_dist=1.5)
    P["bone"] = K.surface("hbone", rgb(224, 208, 172), 0.0, 0.5, bump="grain", bump_scale=1.4, bump_strength=0.35, mottle=1.0, mottle_color=rgb(126, 92, 56), wear=0.8, wear_color=rgb(246, 238, 214),
                          wear_metal=0.0, dirt=1.0, dirt_color=(0.06, 0.04, 0.02, 1), scratches=0.5, fine=0.6, seed=2, bevel=0.14, ao_dist=2.0)
    P["bone2"] = K.surface("hbone2", rgb(176, 150, 112), 0.0, 0.55, bump="grain", bump_scale=1.2, bump_strength=0.4, mottle=1.0, mottle_color=rgb(78, 52, 32), wear=0.8, wear_color=rgb(226, 206, 168),
                           wear_metal=0.0, dirt=1.0, scratches=0.4, fine=0.6, seed=3, bevel=0.14)
    P["flint"] = K.surface("flint", rgb(44, 54, 68), 0.0, 0.14, wear=0.55, wear_radius=0.35, wear_color=rgb(170, 176, 182), wear_metal=0.0, mottle=1.0, mottle_color=rgb(150, 126, 92), bump="hammer", bump_scale=0.3,
                           bump_strength=0.4, scratches=0.3, dirt=0.7, dirt_color=(0.01, 0.012, 0.016, 1), fine=0.4, seed=4, bevel=0.18, ao_dist=2.5)
    P["sinew"] = K.surface("sinew", rgb(206, 170, 116), 0.0, 0.6, bump="wrap", bump_scale=1.0, bump_strength=0.9, groove=rgb(70, 44, 18), groove_amount=0.9, wear=0.7, wear_color=rgb(240, 214, 160),
                           wear_metal=0.0, dirt=0.9, dirt_color=(0.03, 0.018, 0.008, 1), fine=0.6, seed=5, bevel=0.0)
    P["hide"] = K.surface("hide", rgb(112, 74, 42), 0.0, 0.66, bump="leather", bump_scale=1.4, bump_strength=0.8, groove=rgb(34, 20, 10), groove_amount=0.8, wear=0.8, wear_color=rgb(176, 130, 84),
                          wear_metal=0.0, dirt=0.9, mottle=0.9, mottle_color=rgb(60, 36, 20), fine=0.5, seed=6, bevel=0.0)
    P["pitch"] = K.surface("pitch", rgb(34, 24, 16), 0.0, 0.2, wear=0.8, wear_color=rgb(120, 82, 40), wear_metal=0.0, mottle=0.8, mottle_color=rgb(86, 54, 24), bump="leather", bump_scale=0.6, bump_strength=0.4,
                           dirt=0.5, seed=7, bevel=0.0)
    P["feather"] = K.surface("eagle", rgb(122, 86, 50), 0.0, 0.5, feather=(9, 4, 0.5, 0.6), tramp=[(0.0, rgb(236, 226, 206)), (0.18, rgb(236, 226, 206)), (0.22, rgb(70, 46, 26)), (0.6, rgb(130, 92, 52)),
                                                                                                  (0.78, rgb(30, 20, 12)), (1.0, rgb(214, 196, 164))], mottle=0.6, mottle_color=rgb(56, 36, 20), seed=8, bevel=0.0, dirt=0.3)
    P["ruby"] = K.surface("hruby", rgb(214, 22, 22), 0.2, 0.08, emit=rgb(255, 56, 40), emit_strength=0.8, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(255, 110, 70), seed=9, bevel=0.0)
    P["cord"] = K.surface("hcord", rgb(142, 104, 62), 0.0, 0.7, bump="wrap", bump_strength=0.8, groove=rgb(40, 24, 10), groove_amount=0.9, wear=0.7, dirt=0.8, seed=10, bevel=0.0)
    return P


def hunters_spear(opts):
    begin(opts["samples"])
    P = hunter_palette()
    rr = K.rng(1101)
    # ---------------------------------------------------------------- the shaft: two lengths of knotty ash with the grip between them
    def sway(y):
        return 0.55 * math.sin(y * 0.034) * min(1.0, abs(y - 112) / 40.0)
    ptsA = [(sway(y), y, 0.25 * math.sin(y * 0.05)) for y in range(30, 99, 17)] + [(0, 99.5, 0)]
    ptsB = [(0, 124.5, 0)] + [(sway(y) + 0.5 * math.sin(y * 0.021), y, 0.2 * math.sin(y * 0.041)) for y in range(140, 262, 18)]
    K.wood_branch("shaftA", ptsA, 2.6, 2.3, P["bark"], seed=11, sides=10, knots=3, part="shaft_a", bump=0.14, sub=3)
    K.wood_branch("shaftB", ptsB, 2.3, 1.8, P["bark"], seed=12, sides=10, knots=4, part="shaft_b", bump=0.14, sub=3)
    for i, (y, a, ln) in enumerate(((56, 0.8, 2.4), (150, -1.9, 2.0), (178, 2.4, 2.6), (203, -0.6, 1.8), (228, 1.7, 2.2))):      # cut twig stubs with a pale scar
        c, s = math.cos(a), math.sin(a)
        K.tapered("stub%d" % i, [V((1.7 * c, y, 1.7 * s)), V((2.5 * c, y + 0.8, 2.5 * s)), V((3.5 * c, y + 1.6, 3.5 * s))], 0.85, 0.5, P["bone2"], sides=6, sub=1, part="shaft_stub")
    # ---------------------------------------------------------------- grip: hide over the wood, sinew wound over the hide, bone rings and a hanging thong
    K.grip_wrapped("grip", 98.0, 126.0, 2.35, P["hide"], P["sinew"], turns=8.0, cord=0.36, bands=(99.2, 124.8), band_mat=P["bone"], swell=0.1, part="grip", seed=3)
    for i, y in enumerate((112.0,)):
        K.helix_wrap("grip_cord%d" % i, y, y + 4.0, 2.85, P["cord"], turns=3.0, cord=0.3, part="grip_cord", seed=i, sub=6)
    # ---------------------------------------------------------------- butt: a carved bone cap with a spike, a binding of hide with a fringe, fangs on thongs
    prof = [(0.01, 0.0), (0.7, 0.6), (1.6, 2.4), (2.3, 4.0), (2.9, 5.6), (3.1, 7.2), (2.6, 8.6), (3.3, 9.6), (3.6, 11.2), (3.2, 13.0), (2.8, 14.6), (2.6, 16.5)]
    lathe("butt_cap", prof, P["bone"], "butt_cap", 12, 38.0)
    for k in range(3):                                                     # carved ribs and a dark inlay between them
        ring_band("butt_rib%d" % k, 2.2 + k * 2.1, 2.2 + 0.3 * k, 0.5, P["bone2"], "butt_cap", 12)
    K.helix_wrap("butt_bind", 16.5, 40.0, 2.75, P["hide"], turns=5.0, cord=0.7, part="butt_bind", seed=4, sub=4)
    K.helix_wrap("butt_bind2", 18.0, 38.5, 3.0, P["sinew"], turns=6.0, cord=0.3, part="butt_bind", seed=5, sub=4)
    for i in range(6):                                                     # hide fringe hanging from the binding
        a = TAU * i / 6 + rr.uniform(-0.12, 0.12)
        hanging("butt_fringe%d" % i, (3.0 * math.cos(a), 36.5, 3.0 * math.sin(a)), rr.uniform(12, 20), rr.uniform(1.0, 1.7), P["hide"], rr, "butt_fringe", sway=2.4, fray=1.2, n=6,
                face=(math.cos(a + 1.57), 0, math.sin(a + 1.57)), thick=0.13)
    for i, a in enumerate((0.5, 2.7, 4.4)):                                # wolf fangs tied on thongs
        x, z = 3.0 * math.cos(a), 3.0 * math.sin(a)
        K.tube("fang_cord%d" % i, [(x, 36.0, z), (x * 1.2, 32.0, z * 1.2), (x * 1.5, 27.0 - i, z * 1.5)], 0.17, P["cord"], sides=4, sub=2, part="butt_fringe")
        K.tapered("fang%d" % i, [V((x * 1.5, 28.0 - i, z * 1.5)), V((x * 1.6 + 0.5, 25.0 - i, z * 1.6)), V((x * 1.55 + 1.2, 21.0 - i, z * 1.55))], 0.85, 0.05, P["bone"], sides=6, sub=2, part="butt_fringe")
    # ---------------------------------------------------------------- socket: the shaft splits into fingers that hold the flint, a bone ferrule, sinew lashing, pitch
    lathe("socket_ferrule", [(1.9, 236.0), (2.9, 237.0), (3.4, 239.5), (3.1, 242.5), (3.5, 245.0), (3.0, 247.5), (2.5, 249.0)], P["bone"], "socket_ferrule", 12, 40.0)
    for k in range(3):
        ring_band("socket_rib%d" % k, 238.5 + k * 3.4, 3.1 + (0.25 if k == 1 else 0), 0.55, P["bone2"], "socket_ferrule", 12)
    fing = [(2.35, 0.0, 1.0), (2.35, math.pi, 1.0), (2.1, math.pi / 2, 0.85), (2.1, -math.pi / 2, 0.85)]
    for k, (rad, a, sc) in enumerate(fing):
        c, s = math.cos(a), math.sin(a)
        H = 38.0 * sc + rr.uniform(-2, 2)
        K.tapered("socket_finger%d" % k, [V((rad * 0.8 * c, 246.0, rad * 0.8 * s)), V((rad * 1.2 * c, 258.0, rad * 1.2 * s)), V((rad * 1.25 * c, 270.0, rad * 1.25 * s)), V((rad * 0.9 * c, 246.0 + H, rad * 0.9 * s))],
                  1.55, 0.35, P["bark"], sides=6, sub=3, part="socket_finger")
    K.helix_wrap("head_lash", 251.0, 281.0, 3.4, P["sinew"], turns=14.0, cord=0.92, part="head_lash", seed=7, squash=0.8, sub=4)
    K.helix_wrap("head_lash2", 252.0, 280.0, 3.7, P["cord"], turns=4.0, cord=0.3, part="head_lash", seed=8, squash=0.8, sub=4)
    for y, sx in ((250.4, 1.0), (281.6, 0.8)):
        K.blob("pitch%d" % int(y), (0.0, y, 0.0), (3.9 * sx, 1.3, 3.1 * sx), P["pitch"], seed=int(y), lump=0.2, rings=6, segs=12, part="head_pitch")
    # ---------------------------------------------------------------- boar-tusk cross guard: two tusks of different length and curl, lashed to the ferrule
    for side, (ln, rise, br) in ((-1, (27.0, 21.0, 0.0)), (1, (22.0, 16.0, 0.8))):
        pts = [V((side * 2.8, 248.5, 0.0)), V((side * (ln * 0.34), 246.0 + br, 0.7)), V((side * ln * 0.78, 249.0 + rise * 0.25, 0.5)), V((side * ln, 249.0 + rise, 0.0))]
        K.tube("tusk%d" % side, [tuple(p) for p in pts], 2.2, P["bone"], sides=8, sub=4, part="head_tusk", taper=(2.35, 0.1), bumps=0.1, seed=20 + side)
        K.blob("tusk_root%d" % side, (side * 3.8, 247.8, 0.0), (2.6, 2.1, 2.3), P["bone2"], seed=21 + side, lump=0.14, rings=6, segs=9, part="head_tusk")
        for kq, tq in enumerate((0.16, 0.3)):
            p = V(pts[0]).lerp(V(pts[1]), tq * 2)
            ring_band("tusk_cord%d_%d" % (side, kq), 0.0, 1.5, 0.28, P["sinew"], "head_tusk", 8)
            ob = bpy.data.objects["tusk_cord%d_%d" % (side, kq)]
            bl.move(ob, (p.x, p.y - 0.0, p.z), (0, 0, -90 * side))
    # ring of fangs round the ferrule (a trophy collar) and two eagle feathers on cords
    K.spike_array("collar_fang", [V((3.5 * math.cos(a), 236.5, 3.5 * math.sin(a))) for a in [TAU * k / 9 + 0.2 for k in range(9)]],
                  [V((0.8 * math.cos(a), -0.7, 0.8 * math.sin(a))) for a in [TAU * k / 9 + 0.2 for k in range(9)]], 6.0, 0.62, P["bone"], rr, bend=0.4, sides=5, part="head_fangs", curve=0.05)
    for i, (x, z, ln) in enumerate(((-4.2, 3.0, 25.0), (4.4, 2.4, 21.0))):
        K.tube("feather_cord%d" % i, [(x * 0.75, 247.0, z * 0.6), (x, 244.0, z), (x * 1.3, 239.0, z * 1.2)], 0.2, P["cord"], sides=4, sub=2, part="head_cord")
        K.feather("eagle%d" % i, V((x * 1.3, 239.5, z * 1.2)), V((0.2 * (1 if x > 0 else -1), -1.0, 0.15)), ln, 5.6, P["feather"], facing=(0, 0, 1), curl=1.6, serrate=0.2, rows=10, thick=0.24, ridge=2.2,
                  seed=30 + i, part="head_feather", droop=0.7)
    K.gem_setting("ruby", (0.0, 254.2, 4.0), 2.3, P["ruby"], P["bone"], normal=(0, 0, 1), claws=5, sides=8, part="head_gem", rng_=rr, claw_r=0.5)
    # ---------------------------------------------------------------- the flint: a knapped leaf, pressure-flaked edges, faceted faces (flat-shaded facets, a central arris)
    stL = [(262, 2.4), (272, 3.0), (280, 4.6), (288, 7.4), (300, 8.5), (314, 6.7), (326, 3.9), (336, 0.5)]
    notchL = [(286, 1.0, 2.8, "step"), (293, 1.2, 3.4, "chip"), (300, 0.8, 2.4, "step"), (308, 1.1, 3.0, "step"), (316, 0.9, 2.4, "chip"), (322, 0.8, 2.4, "v")]
    notchR = [(284, 1.0, 2.8, "chip"), (291, 0.8, 2.2, "step"), (298, 1.3, 3.2, "step"), (306, 1.0, 2.6, "chip"), (313, 1.2, 3.4, "step"), (320, 0.9, 2.4, "chip"), (328, 0.6, 2.0, "v")]
    L = K.blade_edge(-1, stL, 262, 336, notches=notchL, jitter=0.12, res=3.6, seed=51, tip=(-0.6, 337.5))
    R = K.blade_edge(1, stL, 262, 336, notches=notchR, jitter=0.12, res=3.6, seed=52, tip=(0.9, 336.0))
    rows = K.merge_rows(L, R)
    K.strip("head_flint", rows, P["flint"], (-1.0, -0.62, -0.1, 0.1, 0.62, 1.0), (0.0, 0.7, 1.0, 1.0, 0.7, 0.0), lambda t: 1.6 * (1.0 - 0.5 * t ** 2.0) + 0.1,
            part="head_blade", up=(0, 0, 1), angle=30.0, wobble=lambda q, t: (0.0, 0.0, 0.3 * K.noise3(q, 0.11, 5, 3) * (1.0 - 0.5 * t)))
    # conchoidal flake terraces: stepped, bevelled chips laid diagonally from each edge toward the arris (real relief that shades itself)
    def surf_z(x, y):
        t = (y - 262.0) / 74.0
        v = min(abs(x) / max(K.interp(stL, y), 0.3), 1.0)
        h = K.interp([(0.1, 1.0), (0.34, 0.86), (0.68, 0.6), (1.0, 0.0)], v)
        return (1.6 * (1.0 - 0.5 * t ** 2.0) + 0.1) * h
    for s_ in (1, -1):
        for i in range(7):
            y = 287.0 + i * 6.2 + rr.uniform(-1.0, 1.0)
            sx = 1 if (i + (0 if s_ > 0 else 1)) % 2 == 0 else -1
            hw_ = K.interp(stL, y)
            cx = sx * hw_ * rr.uniform(0.38, 0.6)
            ln, wd = rr.uniform(6.0, 9.5), rr.uniform(2.4, 3.8)
            ang = math.radians(rr.uniform(52, 72)) * (1 if sx > 0 else -1) + math.pi / 2
            out = []
            for k in range(7):
                a = TAU * k / 7
                px, py = ln * 0.5 * math.cos(a) * rr.uniform(0.85, 1.1), wd * 0.5 * math.sin(a) * rr.uniform(0.85, 1.1)
                out.append((cx + px * math.cos(ang) - py * math.sin(ang), y + px * math.sin(ang) + py * math.cos(ang)))
            z0 = surf_z(cx, y) - 0.18
            out = [(x_, y_) for x_, y_ in out]
            if s_ > 0:
                K.plate("flake%d_%d" % (s_, i), out, z0, z0 + 0.32, P["flint"], part="head_blade", bevel_w=0.0)
            else:
                K.plate("flake%d_%d" % (s_, i), out, -z0 - 0.32, -z0, P["flint"], part="head_blade", bevel_w=0.0)
    return K.quad_estimate()


def plain_shaft(mat, y0, y1, r, part="shaft", sides=10, bands=(), band_mat=None, sq=0.95, taper=0.0, swell=0.03, rings=3, band_h=0.8):
    """The long shaft in two lengths (the grip, y 98..126, sits between them). Few quads: the skin is spent on the head."""
    def rad(y):
        return r * (1.0 - taper * (y - y0) / (y1 - y0))
    for i, (a, b) in enumerate(((y0, 98.0), (126.0, y1))):
        prof = [(rad(a + (b - a) * k / rings) * (1.0 + swell * (k % 2)), a + (b - a) * k / rings) for k in range(rings + 1)]
        lathe("%s_%d" % (part, i), prof, mat, part, sides, 42.0, sq, (False, False))
    for j, y in enumerate(bands):
        ring_band("%s_band%d" % (part, j), y, rad(y), band_h, band_mat or mat, part + "_bands", sides, sq)


# ================================================================ 2. FILOLIAL SPEAR: beak, wings, plumage
def filolial_palette():
    P = {}
    P["pearl"] = K.surface("fpearl", rgb(244, 238, 226), 0.25, 0.34, wear=0.8, wear_color=rgb(255, 255, 250), wear_metal=0.4, dirt=0.6, dirt_color=(0.06, 0.04, 0.03, 1), scratches=0.4, bump="brushed",
                           bump_strength=0.1, mottle=0.7, mottle_color=rgb(236, 190, 190), seed=101, bevel=0.14, ao_dist=2.0)
    P["gold"] = K.surface("fgold", rgb(236, 176, 62), 1.0, 0.26, wear=0.85, wear_color=rgb(255, 238, 170), dirt=0.8, dirt_color=(0.05, 0.025, 0.006, 1), scratches=0.6, bump="hammer", bump_scale=0.6,
                          bump_strength=0.16, mottle=0.7, mottle_color=rgb(170, 100, 28), seed=102, bevel=0.18, ao_dist=2.0)
    P["beak"] = K.surface("fbeak", rgb(250, 190, 64), 0.1, 0.3, wear=0.7, wear_color=rgb(255, 232, 150), wear_metal=0.0, dirt=0.7, dirt_color=(0.06, 0.03, 0.005, 1), mottle=1.0, mottle_color=rgb(214, 110, 30),
                          bump="grain", bump_scale=1.4, bump_strength=0.25, cramp=("y", 238.0, 290.0, [(0.0, rgb(214, 120, 36)), (0.5, rgb(250, 190, 64)), (1.0, rgb(255, 226, 130))]), seed=103, bevel=0.12, ao_dist=2.0)
    P["fe_white"] = K.surface("fe_white", rgb(250, 244, 236), 0.0, 0.5, feather=(10, 4, 0.5, 0.5), tramp=[(0.0, rgb(246, 236, 226)), (0.55, rgb(255, 250, 244)), (0.85, rgb(252, 214, 220)), (1.0, rgb(244, 160, 184))],
                              mottle=0.5, mottle_color=rgb(220, 196, 190), dirt=0.3, wear=0.3, seed=104, bevel=0.0, fine=0.3)
    P["fe_pink"] = K.surface("fe_pink", rgb(244, 140, 170), 0.0, 0.5, feather=(10, 4, 0.55, 0.6), tramp=[(0.0, rgb(252, 214, 220)), (0.4, rgb(246, 150, 180)), (0.8, rgb(228, 100, 150)), (1.0, rgb(250, 190, 210))],
                             mottle=0.5, mottle_color=rgb(200, 90, 130), dirt=0.3, seed=105, bevel=0.0, fine=0.3)
    P["fe_blue"] = K.surface("fe_blue", rgb(130, 190, 240), 0.0, 0.5, feather=(10, 4, 0.55, 0.6), tramp=[(0.0, rgb(240, 248, 255)), (0.4, rgb(150, 206, 246)), (0.85, rgb(90, 150, 226)), (1.0, rgb(200, 232, 255))],
                             mottle=0.5, mottle_color=rgb(60, 110, 190), dirt=0.3, seed=106, bevel=0.0, fine=0.3)
    P["fe_gold"] = K.surface("fe_gold", rgb(255, 214, 120), 0.0, 0.45, feather=(10, 4, 0.5, 0.7), tramp=[(0.0, rgb(255, 250, 226)), (0.5, rgb(255, 224, 140)), (1.0, rgb(240, 166, 70))], mottle=0.5, mottle_color=rgb(200, 130, 50),
                             dirt=0.3, seed=107, bevel=0.0, fine=0.3)
    P["egg"] = K.surface("fegg", rgb(250, 232, 206), 0.0, 0.3, mottle=1.0, mottle_color=rgb(150, 92, 56), veins=1.8, vein_color=rgb(150, 82, 52), emit=rgb(255, 196, 120), emit_strength=0.25, emit_flicker=0.5,
                         wear=0.3, wear_color=rgb(255, 250, 240), wear_metal=0.0, seed=108, bevel=0.0, bump="leather", bump_scale=0.8, bump_strength=0.2)
    P["wood"] = K.surface("fwood", rgb(222, 196, 156), 0.0, 0.55, bump="grain", bump_scale=1.0, bump_strength=0.45, groove=rgb(120, 86, 52), groove_amount=0.7, mottle=1.0, mottle_color=rgb(172, 128, 86), wear=0.6,
                          wear_color=rgb(250, 238, 206), wear_metal=0.0, dirt=0.7, seed=109, bevel=0.1)
    P["wrap"] = K.surface("fwrap", rgb(236, 120, 158), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(120, 40, 70), groove_amount=0.9, dirt=0.7, wear=0.6, wear_color=rgb(255, 200, 214),
                          wear_metal=0.0, mottle=0.7, mottle_color=rgb(170, 70, 110), fine=0.5, seed=110, bevel=0.0)
    P["cord"] = K.surface("fcord", rgb(255, 232, 170), 0.0, 0.5, bump="wrap", bump_strength=0.8, groove=rgb(150, 100, 50), groove_amount=0.8, wear=0.7, dirt=0.6, seed=111, bevel=0.0)
    P["ribbon"] = K.surface("fribbon", rgb(120, 190, 240), 0.0, 0.55, bump="cross", bump_scale=0.8, bump_strength=0.5, groove=rgb(50, 100, 170), groove_amount=0.8, wear=0.5, wear_color=rgb(210, 236, 255), wear_metal=0.0,
                            dirt=0.5, mottle=0.7, mottle_color=rgb(70, 130, 210), seed=112, bevel=0.0)
    return P


def filolial_spear(opts):
    begin(opts["samples"])
    P = filolial_palette()
    rr = K.rng(2202)
    # ---------------------------------------------------------------- shaft, grip, butt
    plain_shaft(P["wood"], 20.0, 218.0, 2.35, bands=(58.0, 97.5, 126.5, 170.0, 206.0), band_mat=P["gold"], taper=0.04)
    K.grip_wrapped("grip", 98.0, 126.0, 2.45, P["wrap"], P["cord"], turns=7.0, cord=0.38, bands=(99.0, 125.0), band_mat=P["gold"], swell=0.1, part="grip", seed=3)
    lathe("butt_cap", [(0.01, 0.0), (1.0, 0.8), (2.2, 2.8), (3.0, 5.2), (3.5, 8.2), (3.3, 11.6), (2.8, 15.0), (2.5, 19.0)], P["gold"], "butt_cap", 12, 40.0)
    ring_band("butt_ring", 6.0, 3.2, 0.7, P["pearl"], "butt_cap", 12)
    for k in range(3):                                                      # three golden toes of the filolial foot grip the butt
        a = TAU * k / 3 + 0.3
        c, s_ = math.cos(a), math.sin(a)
        K.tapered("butt_toe%d" % k, [V((2.6 * c, 15.0, 2.6 * s_)), V((4.8 * c, 10.0, 4.8 * s_)), V((5.6 * c, 5.0, 5.6 * s_)), V((3.4 * c, 1.8, 3.4 * s_))], 1.3, 0.2, P["beak"], sides=6, sub=3, part="butt_toes")
    # plumes: a fan of long tail feathers from a gold ring at y 52, hanging down: pink, blue, white, gold; thick and curled
    ring_band("plume_ring", 52.0, 2.7, 1.6, P["gold"], "butt_ring", 12)
    mats = [P["fe_pink"], P["fe_blue"], P["fe_white"], P["fe_gold"], P["fe_pink"], P["fe_blue"], P["fe_white"]]
    for i, (ang, ln, rot) in enumerate(((-70, 38.0, 0.3), (-44, 44.0, 1.0), (-20, 36.0, -0.2), (6, 46.0, 0.6), (30, 38.0, 0.0), (52, 42.0, 1.1), (76, 34.0, -0.5))):
        a = math.radians(ang)
        K.feather("plume%d" % i, V((2.7 * math.cos(a + 1.0), 51.0, 2.7 * math.sin(a + 1.0))), V((0.32 * math.cos(a), -1.0, 0.32 * math.sin(a))).normalized(), ln, 5.8 + rr.uniform(-0.6, 0.8), mats[i],
                  facing=(math.sin(a), 0, -math.cos(a)), curl=2.2 * (1 if i % 2 else -1), serrate=0.24, rows=9, thick=0.26, ridge=2.4, seed=300 + i, part="butt_plumes", droop=1.0,
                  bend=V((2.0 * math.cos(a), 0, 2.0 * math.sin(a))))
    # pink-and-blue ribbons with golden bells at the grip
    for i, (x, col) in enumerate(((-2.8, P["ribbon"]), (2.9, P["wrap"]))):
        hanging("ribbon%d" % i, (x, 125.0, 1.2), rr.uniform(26, 34), 1.7, col, rr, "grip_ribbon", sway=2.0, fray=0.8, n=9, notch=1.2, thick=0.12)
        K.blob("bell%d" % i, (x * 1.6, 91.0 - i * 4, 1.6), (1.1, 1.2, 1.1), P["gold"], seed=i, lump=0.05, rings=6, segs=8, part="grip_ribbon")
    # ---------------------------------------------------------------- socket: golden cup under the beak, and the egg in its toes
    lathe("socket_cup", [(2.0, 216.0), (3.2, 218.0), (4.4, 222.0), (5.0, 228.0), (4.6, 233.0), (3.4, 236.0), (2.8, 238.0)], P["gold"], "socket_cup", 12, 40.0)
    for k in range(4):
        ring_band("socket_ring%d" % k, 218.5 + 5.2 * k, 3.2 + 0.4 * (k % 3), 0.6, P["pearl"], "socket_cup", 12)
    K.blob("egg", (0.0, 246.0, 4.2), (3.4, 4.5, 3.4), P["egg"], seed=5, lump=0.05, rings=8, segs=11, part="socket_egg")
    for k in range(5):
        a = TAU * (k + 0.4) / 5
        c, s_ = math.cos(a), math.sin(a)
        K.tapered("egg_toe%d" % k, [V((3.2 * c, 240.5, 4.2 + 3.2 * s_)), V((5.1 * c, 243.0, 4.2 + 5.1 * s_)), V((4.9 * c, 248.0, 4.2 + 4.9 * s_)), V((2.6 * c, 251.8, 4.2 + 2.6 * s_))], 0.85, 0.15, P["beak"], sides=5, sub=2, part="socket_egg_toes")
    # ---------------------------------------------------------------- the beak: an upper mandible that hooks over the blade root and a shorter lower one
    def mandible(name, z0, zs, ln, w0, t0, hook, y0):
        path = [V((0.0, y0 + ln * u, z0 + zs * u + hook * u ** 3)) for u in [i / 11 for i in range(12)]]
        ob = bl.sweep(name, path, [(-1.0, -0.2), (-0.75, -0.8), (0.0, -1.0), (0.75, -0.8), (1.0, -0.2), (0.8, 0.5), (0.0, 1.0), (-0.8, 0.5)], P["beak"], up=(1, 0, 0),
                      scale=lambda t: (w0 * (1 - 0.9 * t ** 0.9) + 0.08, t0 * (1 - 0.92 * t ** 0.8) + 0.08), part="head_beak", caps=(True, True))
        return K._finish_obj(ob, "head_beak", True, 55.0)
    mandible("beak_up", 5.4, 0.8, 40.0, 3.0, 4.6, -3.2, 244.0)
    mandible("beak_dn", 3.0, 0.2, 20.0, 2.6, 3.2, 1.6, 244.0)
    # ---------------------------------------------------------------- the blade: a long, serrated quill (feather-vane edges, raised rachis)
    stL = [(240, 2.6), (250, 4.0), (262, 6.8), (278, 8.8), (296, 8.6), (312, 6.6), (326, 3.8), (338, 0.5)]
    nL = [(y, 1.5, 5.0, "tooth") for y in (252, 260, 268, 277, 286, 295, 304, 313, 322)]
    nR = [(y, 1.5, 5.0, "tooth") for y in (255, 263, 272, 281, 290, 299, 308, 317, 326)]
    L = K.blade_edge(-1, stL, 240, 338, notches=nL, jitter=0.05, res=8.0, seed=61, tip=(-0.3, 340.0))
    R = K.blade_edge(1, stL, 240, 338, notches=nR, jitter=0.05, res=8.0, seed=62, tip=(0.7, 339.0))
    K.strip("head_quill", K.merge_rows(L, R), P["pearl"], (-1.0, -0.74, -0.16, 0.0, 0.16, 0.74, 1.0), (0.0, 0.7, 0.92, 1.35, 0.92, 0.7, 0.0), lambda t: 1.3 * (1.0 - 0.45 * t ** 2) + 0.1,
            part="head_blade", up=(0, 0, 1), angle=40.0, wobble=lambda q, t: (0.0, 0.0, 0.5 * math.cos(q.x * 0.28) - 0.4 + 0.2 * K.noise3(q, 0.1, 7, 2)))
    K.tube("rachis", [(0.0, 252.0, 1.9), (0.0, 290.0, 2.0), (0.0, 336.0, 1.1)], 0.5, P["gold"], sides=6, sub=3, part="head_rachis", taper=(0.7, 0.18))
    # ---------------------------------------------------------------- the wings: three tiers of thick, curled feathers per side, a gold wing-bone; left and right differ
    for side in (1, -1):
        rs = K.rng(50 + side)
        K.tube("wingbone%d" % side, [(side * 5.0, 246.0, 1.0), (side * 14.0, 252.0, 2.4), (side * 25.0, 266.0, 3.0), (side * 31.0, 282.0, 2.4)], 1.0, P["gold"], sides=6, sub=3, part="head_wingbone", taper=(1.6, 0.5), bumps=0.1, seed=side)
        K.blob("wingjoint%d" % side, (side * 25.0, 266.0, 3.0), (1.9, 1.9, 1.7), P["gold"], seed=3 + side, lump=0.08, rings=6, segs=8, part="head_wingbone")
        tiers = [(5.5, 245.0, (24, 80), 6 if side > 0 else 5, 40.0, 6.6, 0.9, P["fe_white"], P["fe_pink"]),
                 (6.0, 247.0, (10, 66), 5 if side > 0 else 6, 31.0, 6.0, 0.3, P["fe_pink"], P["fe_white"]),
                 (6.5, 248.0, (-6, 52), 5, 22.0, 5.2, -0.2, P["fe_blue"], P["fe_white"])]
        for ti, (cx, cy, (a0, a1), cnt, ln, wd, dz, m1, m2) in enumerate(tiers):
            a0m, a1m = (a0, a1) if side > 0 else (180 - a0, 180 - a1)
            K.fan_feathers("wing%d_%d_" % (side, ti), (side * cx, cy, 0.4 + dz + 1.2 * ti), cnt, a0m, a1m, ln, wd, m1, rs, part="head_wings", len_jit=0.16, ang_jit=3.0, curl=3.0, layer_dz=0.45,
                           serrate=0.2, rows=8, thick=0.26, mat2=m2, droop=-1.2)
    # head crest feathers rising behind the blade
    for i, (x, ln, bz) in enumerate(((-3.0, 38.0, -9.0), (0.5, 46.0, -12.0), (3.4, 34.0, -8.0))):
        K.feather("crest%d" % i, V((x, 242.0, -3.0)), V((x * 0.05, 1.0, -0.12)), ln, 5.2, [P["fe_pink"], P["fe_gold"], P["fe_blue"]][i], facing=(1, 0, 0), curl=2.5 * (-1 if i % 2 else 1), serrate=0.22, rows=10, thick=0.24,
                  ridge=2.0, seed=400 + i, part="head_crest", bend=V((0, 0, bz)))
    return K.quad_estimate()


# ================================================================ shared: crystals, jagged blades, energy tendrils
def crystal(name, base, direction, length, r, mat, part, sides=6, tip=0.55, rr_=None, facet=18.0, taper=0.78, flat=1.0, up=None):
    """A faceted prism crystal (hard edges, every face shades itself) standing at `base`, pointing along `direction`, with a pyramid tip. flat < 1 squashes
    it into a crystal blade (the thin axis is the frame's y, so `up` picks which way it is thin)."""
    prof = [(r * 0.55, 0.0), (r * 0.95, length * 0.1), (r, length * 0.22), (r * taper, length * (1.0 - tip * 0.35)), (r * 0.5, length * (1.0 - tip * 0.15)), (0.02, length)]
    ob = bl.lathe(name, prof, mat, sides=sides, part=part, axis="z", squash=flat)
    if rr_:
        for v in ob.data.vertices:
            v.co += V((rr_.uniform(-1, 1), rr_.uniform(-1, 1), 0)) * r * 0.06
    K.xf(ob, K.frame(base, direction, up or ((0, 1, 0) if abs(V(direction).normalized().y) < 0.95 else (1, 0, 0))))
    return K._finish_obj(ob, part, True, facet)


def jag_blade(name, mat, y0, y1, stations, nl, nr, seed, tipL, tipR, part, bend=None, thick=1.6, sec=None, angle=24.0, wob=0.25, jitter=0.18, res=4.5):
    """A blade with an independent left and right edge (notches, chips, broken tip), optionally bent sideways by bend(y): a split, splintered or hooked tine."""
    L = K.blade_edge(-1, stations, y0, y1, notches=nl, jitter=jitter, res=res, seed=seed, tip=tipL)
    R = K.blade_edge(1, stations, y0, y1, notches=nr, jitter=jitter, res=res, seed=seed + 1, tip=tipR)
    if bend:
        L = [V((p.x + bend(p.y), p.y, 0)) for p in L]
        R = [V((p.x + bend(p.y), p.y, 0)) for p in R]
    vs, hs = sec or ((-1.0, -0.7, -0.12, 0.12, 0.7, 1.0), (0.0, 0.75, 1.0, 1.0, 0.75, 0.0))
    th = thick if callable(thick) else (lambda t: thick * (1.0 - 0.5 * t ** 2) + 0.08)
    return K.strip(name, K.merge_rows(L, R), mat, vs, hs, th, part=part, up=(0, 0, 1), angle=angle,
                   wobble=(lambda q, t: (0.0, 0.0, wob * K.noise3(q, 0.13, seed, 3))) if wob else None)


def tendril(name, pts, r0, r1, mat, part, sides=5, sub=2, jag=0.0, rr_=None):
    """A writhing / forking energy tendril: a tapered tube through points, optionally broken into zigzags (lightning)."""
    P_ = [V(p) for p in pts]
    if jag and rr_:
        out = []
        for a, b in zip(P_[:-1], P_[1:]):
            out.append(a)
            for k in (1, 2):
                out.append(a.lerp(b, k / 3.0) + V((rr_.uniform(-jag, jag), rr_.uniform(-jag, jag), rr_.uniform(-jag, jag))))
        out.append(P_[-1])
        P_ = out
    return K.tapered(name, P_, r0, r1, mat, sides=sides, sub=sub, part=part)


# ================================================================ 3. CHAOS SPEAR: a split, splintered point of black iron and amethyst
def chaos_palette():
    P = {}
    P["iron"] = K.surface("ciron", rgb(32, 24, 44), 1.0, 0.34, wear=1.0, wear_color=rgb(176, 120, 255), wear_metal=1.0, dirt=0.9, dirt_color=(0.005, 0.003, 0.01, 1), scratches=0.8, bump="hammer", bump_scale=0.5,
                          bump_strength=0.25, mottle=1.0, mottle_color=rgb(60, 24, 88), veins=0.4, vein_color=rgb(190, 90, 255), vein_strength=0.0, seed=201, bevel=0.2, ao_dist=2.5, fine=0.4)
    P["iron2"] = K.surface("ciron2", rgb(52, 40, 70), 1.0, 0.42, wear=0.9, wear_color=rgb(200, 150, 255), wear_metal=1.0, dirt=1.0, dirt_color=(0.005, 0.003, 0.01, 1), scratches=0.6, bump="brushed", bump_strength=0.15,
                           mottle=1.0, mottle_color=rgb(30, 16, 48), seed=202, bevel=0.16)
    P["blade"] = K.surface("cblade", rgb(22, 16, 34), 0.9, 0.22, wear=0.8, wear_color=rgb(150, 100, 210), wear_metal=1.0, dirt=0.9, dirt_color=(0.004, 0.002, 0.01, 1), scratches=0.9, bump="hammer", bump_scale=0.38,
                           bump_strength=0.3, mottle=1.0, mottle_color=rgb(44, 20, 70), veins=None, vein_color=rgb(214, 110, 255), vein_strength=0.0, seed=203, bevel=0.2, ao_dist=3.0, fine=0.5,
                           cramp=("y", 250.0, 340.0, [(0.0, rgb(14, 10, 22)), (0.6, rgb(30, 20, 48)), (1.0, rgb(60, 34, 92))]))
    P["ame"] = K.surface("ame", rgb(150, 70, 230), 0.1, 0.12, wear=0.8, wear_color=rgb(240, 214, 255), wear_metal=0.0, mottle=1.0, mottle_color=rgb(90, 30, 170), emit=rgb(170, 90, 255), emit_strength=0.45, emit_flicker=0.7,
                         veins=0.9, vein_color=rgb(230, 190, 255), vein_strength=0.4, dirt=0.4, dirt_color=(0.02, 0.0, 0.05, 1), seed=204, bevel=0.1, ao_dist=1.5)
    P["ame2"] = K.surface("ame2", rgb(96, 36, 170), 0.1, 0.12, wear=0.8, wear_color=rgb(200, 160, 255), wear_metal=0.0, mottle=1.0, mottle_color=rgb(50, 14, 100), emit=rgb(130, 60, 230), emit_strength=0.4, emit_flicker=0.7,
                          dirt=0.5, seed=205, bevel=0.1)
    P["ender"] = K.surface("ender", rgb(40, 230, 190), 0.1, 0.1, emit=rgb(60, 255, 200), emit_strength=0.75, emit_flicker=0.5, mottle=1.0, mottle_color=rgb(20, 120, 140), veins=1.4, vein_color=rgb(210, 255, 240),
                           vein_strength=0.8, seed=206, bevel=0.0)
    P["tendril"] = K.surface("tendril", rgb(110, 40, 190), 0.0, 0.3, emit=rgb(190, 100, 255), emit_strength=0.8, emit_flicker=0.9, mottle=1.0, mottle_color=rgb(40, 10, 90), seed=207, bevel=0.0)
    P["wrap"] = K.surface("cwrap", rgb(30, 22, 40), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(6, 3, 10), groove_amount=0.9, dirt=0.8, mottle=0.7, mottle_color=rgb(18, 10, 28), wear=0.6,
                          wear_color=rgb(110, 70, 150), wear_metal=0.0, fine=0.6, seed=208, bevel=0.0)
    P["cord"] = K.surface("ccord", rgb(150, 60, 220), 0.0, 0.5, bump="wrap", bump_strength=0.8, groove=rgb(40, 10, 70), groove_amount=0.8, wear=0.7, wear_color=rgb(220, 170, 255), wear_metal=0.0, dirt=0.6, seed=209, bevel=0.0)
    return P


def chaos_spear(opts):
    begin(opts["samples"])
    P = chaos_palette()
    rr = K.rng(3303)
    # ---------------------------------------------------------------- shaft (hex, like a column of dark crystal), grip, butt crystals
    plain_shaft(P["iron2"], 18.0, 216.0, 2.45, sides=6, bands=(52.0, 97.0, 127.0, 162.0, 196.0), band_mat=P["iron"], band_h=1.2, sq=1.0, taper=0.08, swell=0.06)
    K.grip_wrapped("grip", 98.0, 126.0, 2.5, P["wrap"], P["cord"], turns=6.0, cord=0.4, bands=(99.0, 125.0), band_mat=P["iron"], swell=0.09, part="grip", seed=3, sides=8)
    lathe("butt_cap", [(0.01, 6.0), (1.4, 7.0), (3.0, 9.6), (3.4, 14.0), (2.6, 18.0), (2.4, 20.0)], P["iron"], "butt_cap", 8, 30.0)
    for i, (a, ln, tilt) in enumerate(((0.2, 15.0, 0.38), (2.5, 11.0, 0.5), (4.2, 13.0, 0.28), (5.6, 8.0, 0.6))):
        crystal("butt_crystal%d" % i, (2.2 * math.cos(a), 10.0, 2.2 * math.sin(a)), (math.cos(a) * tilt, -1.0, math.sin(a) * tilt), ln, 1.6 + 0.4 * (i % 2), P["ame"] if i % 2 == 0 else P["ame2"], "butt_crystals", sides=5, rr_=rr)
    # cracks of violet light: split iron plates bolted over the shaft at intervals
    for i, y in enumerate((150.0, 186.0)):
        for k in range(2):
            a = TAU * k / 2 + i * 0.9
            K.bar("plate%d_%d" % (i, k), (2.5 * math.cos(a), y - 6.0, 2.5 * math.sin(a)), (2.9 * math.cos(a + 0.2), y + 7.0, 2.9 * math.sin(a + 0.2)), 0.9, 0.5, P["iron"], part="shaft_plates", taper=0.5, up=(math.cos(a), 0, math.sin(a)))
    # ---------------------------------------------------------------- socket: a cracked iron collar, amethyst clusters, the ender eye in black claws
    lathe("socket_collar", [(2.2, 214.0), (3.6, 216.0), (4.8, 220.0), (4.2, 226.0), (5.2, 232.0), (4.6, 240.0), (3.4, 248.0), (2.6, 252.0)], P["iron"], "socket_collar", 8, 28.0)
    for k, y in enumerate((218.0, 230.0, 244.0)):
        ring_band("socket_ring%d" % k, y, 4.4 + 0.3 * k, 0.7, P["iron2"], "socket_collar", 8)
    for i in range(6):
        a = TAU * i / 6 + rr.uniform(-0.2, 0.2) + 0.3
        yy = 221.0 + rr.uniform(0, 26.0)
        up_ = rr.uniform(0.2, 0.9)
        crystal("socket_ame%d" % i, (4.2 * math.cos(a), yy, 4.2 * math.sin(a)), (math.cos(a), up_, math.sin(a)), rr.uniform(10.0, 19.0), rr.uniform(1.4, 2.4), P["ame"] if i % 3 else P["ame2"], "socket_crystals", sides=5, rr_=rr)
    claw_gem("socket_eye", 238.0, 3.2, P["ender"], P["iron"], rr, "socket_eye", claws=5, sides=8, stretch=1.1, z=4.4, claw_len=1.2, claw_r=0.6, collar=False)
    # ---------------------------------------------------------------- the crown: stepped, jagged iron plates round the base of the three tines
    crown = [(-9.5, 252.0), (-7.0, 256.0), (-8.0, 262.0), (-3.0, 260.0), (0.0, 268.0), (3.5, 261.0), (8.2, 264.0), (7.0, 257.0), (10.0, 251.0), (4.0, 247.0), (-3.5, 247.0)]
    for s_ in (1, -1):
        K.stepped_plate("crown%d" % s_, crown, 1.4 * s_ if s_ > 0 else -1.4, P["iron"], steps=2, step_h=0.9, inset=1.2, part="head_crown", up=s_)
    # ---------------------------------------------------------------- the three tines of the split point: a long broken-edged central blade, a curved left tine, a short right tine with a hook
    jag_blade("head_main", P["blade"], 256.0, 340.0, [(256, 3.6), (266, 6.8), (282, 8.2), (300, 7.0), (318, 4.6), (330, 2.4), (340, 0.4)],
              [(268, 2.2, 5.0, "v"), (280, 3.0, 7.0, "tooth"), (293, 1.9, 4.5, "step"), (306, 2.6, 6.0, "v"), (317, 1.6, 4.0, "chip"), (327, 1.8, 4.0, "tooth")],
              [(262, 1.8, 4.0, "chip"), (274, 2.6, 6.0, "step"), (288, 3.2, 7.0, "tooth"), (301, 1.8, 4.2, "v"), (313, 2.4, 5.0, "step"), (323, 1.5, 3.6, "v")],
              11, (-1.6, 342.0), (0.8, 338.0), "head_blade", res=6.5, bend=lambda y: 1.4 * math.sin((y - 256) * 0.05), thick=lambda t: 1.9 * (1.0 - 0.45 * t ** 1.6) + 0.1)
    for s_, (xo, ln, tipx, seed) in ((-1, (-9.0, 62.0, -20.0, 21)), (1, (9.0, 42.0, 16.0, 31))):
        jag_blade("head_tine%d" % s_, P["blade"], 250.0, 250.0 + ln, [(250, 2.8), (258, 4.4), (250 + ln * 0.5, 4.0), (250 + ln * 0.8, 2.4), (250 + ln, 0.3)],
                  [(262, 1.0, 3.0, "v"), (272, 1.2, 4.0, "step")] if s_ < 0 else [(262, 1.0, 3.0, "chip"), (270, 1.4, 3.5, "tooth")],
                  [(264, 1.2, 3.5, "tooth"), (276, 1.0, 3.0, "v")] if s_ < 0 else [(266, 1.0, 3.0, "v")],
                  seed, (tipx * 0.0 - 0.4, 250.0 + ln + 1.0), (0.8 + (3.2 if s_ > 0 else 0), 250.0 + ln - (4.0 if s_ > 0 else 0)), "head_blade", res=6.0,
                  bend=lambda y, xo=xo, tipx=tipx, ln=ln: xo + (tipx - xo) * ((y - 250.0) / ln) ** 1.7, thick=1.3, wob=0.18)
    # splintered barbs standing off the edges of the main blade (real spikes: the silhouette is torn, not a smooth leaf)
    bpts = [V((-6.8, 281.0, 0)), V((-7.6, 296.0, 0)), V((-5.2, 311.0, 0)), V((7.0, 270.0, 0)), V((7.8, 289.0, 0)), V((5.6, 306.0, 0)), V((3.4, 322.0, 0))]
    bdir = [V((-1.0, 0.7, 0.1)), V((-1.0, 0.45, -0.1)), V((-0.9, 0.6, 0.1)), V((1.0, 0.6, -0.1)), V((1.0, 0.8, 0.1)), V((1.0, 0.5, 0.0)), V((0.9, 0.7, -0.1))]
    K.spike_array("head_barb", bpts, bdir, [8.0, 7.0, 6.0, 7.5, 9.0, 6.5, 5.0], 1.3, P["blade"], rr, bend=0.5, sides=4, part="head_blade", curve=0.12)
    # a hooked barb on the long left tine (Hooked Pierce): a real curved hook, bone-white iron
    K.tapered("hook", [V((-15.0, 280.0, 0.0)), V((-24.0, 286.0, 0.3)), V((-27.5, 276.0, 0.0)), V((-22.0, 270.0, 0.2))], 1.5, 0.15, P["blade"], sides=5, sub=3, part="head_hook")
    # ---------------------------------------------------------------- amethyst clusters growing from the root of the blades, and floating splinters
    for i in range(6):
        a = rr.uniform(-1.1, 1.1) + (0 if i % 2 else math.pi)
        base = V((rr.uniform(-8, 8), rr.uniform(255, 266), rr.choice((-1, 1)) * 2.4))
        crystal("head_ame%d" % i, base, V((rr.uniform(-0.7, 0.7), rr.uniform(0.4, 1.0), rr.choice((-1, 1)) * rr.uniform(0.5, 1.3))), rr.uniform(11.0, 22.0), rr.uniform(1.4, 2.5), P["ame"] if i % 2 else P["ame2"], "head_crystals", sides=5, rr_=rr)

    def blade_zone(p):
        return abs(p.x) < 12.0 and 240 < p.y < 345 and abs(p.z) < 5.0
    K.shard_cloud("splinter", (0, 292, 0), (30.0, 54.0, 13.0), 12, (0.9, 1.7), P["ame"], rr, avoid=blade_zone, part="head_splinters", stretch=2.4, sides=4, max_count=12,
                  flow=lambda p: V((-p.x * 0.2, 1.0, rr.uniform(-0.5, 0.5))), mat2=P["blade"], kinds=2)
    # violet lightning forking off the three points
    for i, (b, d) in enumerate(((V((-1.4, 338.0, 0.4)), V((-5, 14, 3))), (V((-19.0, 306.0, 0.0)), V((-12, 12, -3))), (V((14.5, 293.0, 0.0)), V((10, 13, 4))), (V((-9, 268.0, 1.0)), V((-14, 4, 6))), (V((8, 270.0, -1.0)), V((13, 6, -5))))):
        tendril("bolt%d" % i, [b, b + d * 0.5, b + d], 1.0, 0.1, P["tendril"], "head_energy", sides=4, sub=1, jag=1.8, rr_=rr)
    return K.quad_estimate()


def torus(name, center, R, r, mat, part, plane="xy", n=40, sides=8, scale=None, twist=None, angle=50.0, tilt=0.0):
    """A ring (torus) of radius R, tube radius r (or scale(t) factor), lying in the xy plane (faces the viewer), the yz plane, or tilted about x by `tilt` degrees."""
    pts = []
    for i in range(n):
        a = TAU * i / n
        if plane == "xy":
            pts.append(V((R * math.cos(a), R * math.sin(a), 0.0)))
        else:
            pts.append(V((0.0, R * math.sin(a), R * math.cos(a))))
    pts.append(pts[0])
    ob = bl.sweep(name, pts, bl.circle(1.0, sides), mat, scale=scale or (lambda t: r), up=(0, 0, 1) if plane == "xy" else (1, 0, 0), part=part, caps=(False, False), twist=twist)
    if tilt:
        bl.move(ob, rot=(tilt, 0, 0))
    bl.move(ob, center)
    return K._finish_obj(ob, part, True, angle)


# ================================================================ 4. METEOR SPEAR: a burning rock on an obsidian point, a comet tail of flame
def meteor_palette():
    P = {}
    P["rock"] = K.surface("mrock", rgb(62, 48, 44), 0.0, 0.9, bump="hammer", bump_scale=0.35, bump_strength=0.9, groove=rgb(14, 8, 6), groove_amount=0.9, mottle=1.0, mottle_color=rgb(104, 62, 40), wear=0.9,
                          wear_color=rgb(210, 120, 60), wear_metal=0.0, dirt=1.0, dirt_color=(0.004, 0.002, 0.002, 1), scratches=0.4, veins=0.22, vein_color=rgb(255, 120, 20), vein_strength=0.0, fine=0.8, seed=301,
                          bevel=0.0, ao_dist=2.5)
    P["rock2"] = K.surface("mrock2", rgb(38, 30, 30), 0.0, 0.85, bump="hammer", bump_scale=0.5, bump_strength=0.8, groove=rgb(8, 4, 4), groove_amount=0.9, mottle=1.0, mottle_color=rgb(88, 52, 36), wear=0.9,
                           wear_color=rgb(230, 130, 50), wear_metal=0.0, dirt=1.0, veins=0.3, vein_color=rgb(255, 150, 30), vein_strength=0.0, fine=0.7, seed=302, bevel=0.0, ao_dist=2.5)
    P["glass"] = K.surface("mglass", rgb(14, 12, 18), 0.6, 0.1, wear=1.0, wear_color=rgb(150, 70, 40), wear_metal=1.0, dirt=0.6, scratches=0.7, mottle=1.0, mottle_color=rgb(26, 10, 10), veins=None,
                           vein_color=rgb(255, 120, 24), vein_strength=0.0, seed=303, bevel=0.14, ao_dist=2.0, cramp=("y", 280.0, 345.0, [(0.0, rgb(8, 6, 10)), (0.7, rgb(18, 10, 12)), (1.0, rgb(52, 20, 10))]))
    P["lava"] = K.surface("mlava", rgb(255, 120, 20), 0.0, 0.5, emit=rgb(255, 120, 24), emit_strength=1.3, emit_flicker=0.9, mottle=1.0, mottle_color=rgb(255, 210, 90), seed=304, bevel=0.0,
                          bump="leather", bump_scale=0.7, bump_strength=0.3)
    P["flame"] = K.surface("mflame", rgb(230, 70, 14), 0.0, 0.5, feather=(7, 3, 0.35, 0.4), tramp=[(0.0, rgb(255, 170, 60)), (0.35, rgb(235, 100, 22)), (0.75, rgb(180, 40, 10)), (1.0, rgb(90, 16, 8))],
                           emit=rgb(255, 130, 24), emit_strength=0.55, temit=[(0.0, rgb(255, 170, 70)), (0.3, rgb(240, 100, 24)), (0.55, rgb(0, 0, 0)), (1.0, rgb(0, 0, 0))], emit_flicker=0.7, seed=305, bevel=0.0)
    P["flame2"] = K.surface("mflame2", rgb(255, 170, 40), 0.0, 0.5, feather=(7, 3, 0.3, 0.5), tramp=[(0.0, rgb(255, 210, 110)), (0.5, rgb(255, 150, 50)), (1.0, rgb(220, 80, 16))],
                            emit=rgb(255, 190, 70), emit_strength=0.5, temit=[(0.0, rgb(255, 200, 100)), (0.3, rgb(255, 130, 40)), (0.55, rgb(0, 0, 0)), (1.0, rgb(0, 0, 0))], emit_flicker=0.6, seed=306, bevel=0.0)
    P["iron"] = K.surface("miron", rgb(34, 28, 28), 1.0, 0.5, wear=1.0, wear_color=rgb(210, 130, 70), wear_metal=1.0, dirt=1.0, dirt_color=(0.004, 0.002, 0.002, 1), scratches=0.7, bump="hammer", bump_strength=0.3,
                          mottle=1.0, mottle_color=rgb(80, 36, 20), seed=307, bevel=0.16, ao_dist=2.0)
    P["rod"] = K.surface("mrod", rgb(255, 170, 30), 0.3, 0.3, wear=0.8, wear_color=rgb(255, 240, 150), wear_metal=0.2, mottle=1.0, mottle_color=rgb(200, 80, 10), bump="rings", bump_scale=0.9, bump_strength=0.4,
                         dirt=0.5, seed=308, bevel=0.1)
    P["gstone"] = K.surface("mgstone", rgb(255, 214, 90), 0.1, 0.15, wear=0.8, wear_color=rgb(255, 250, 200), wear_metal=0.0, emit=rgb(255, 210, 90), emit_strength=0.7, emit_flicker=0.5, mottle=1.0, mottle_color=rgb(255, 150, 30),
                            seed=309, bevel=0.06)
    P["char"] = K.surface("mchar", rgb(30, 24, 22), 0.0, 0.8, bump="grain", bump_scale=1.0, bump_strength=0.8, groove=rgb(6, 4, 3), groove_amount=0.9, mottle=1.0, mottle_color=rgb(80, 40, 22), veins=0.35,
                          vein_color=rgb(255, 110, 20), vein_strength=0.0, wear=0.5, wear_color=rgb(150, 80, 40), wear_metal=0.0, dirt=0.8, seed=310, bevel=0.0)
    P["wrap"] = K.surface("mwrap", rgb(28, 20, 18), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(6, 3, 2), groove_amount=0.9, dirt=0.8, mottle=0.7, mottle_color=rgb(60, 26, 14), wear=0.6,
                          wear_color=rgb(160, 80, 40), wear_metal=0.0, fine=0.6, seed=311, bevel=0.0)
    P["cord"] = K.surface("mcord", rgb(220, 100, 20), 0.0, 0.5, bump="wrap", bump_strength=0.8, groove=rgb(70, 24, 6), groove_amount=0.8, wear=0.7, wear_color=rgb(255, 190, 90), wear_metal=0.0, dirt=0.6, seed=312, bevel=0.0)
    return P


def meteor_spear(opts):
    begin(opts["samples"])
    P = meteor_palette()
    rr = K.rng(4404)
    # ---------------------------------------------------------------- shaft: charred, ember-veined, banded with blackened iron; a grip of black leather; a cooling rock butt with lava drips
    plain_shaft(P["char"], 22.0, 216.0, 2.45, sides=8, bands=(56.0, 97.5, 127.0, 166.0, 200.0), band_mat=P["iron"], band_h=1.1, sq=1.0, taper=0.06, swell=0.08)
    K.grip_wrapped("grip", 98.0, 126.0, 2.5, P["wrap"], P["cord"], turns=6.0, cord=0.4, bands=(99.0, 125.0), band_mat=P["iron"], swell=0.09, part="grip", seed=3, sides=8)
    for i, (c, r_, y, sd) in enumerate(((V((0, 10.0, 0)), 3.8, 10.0, 1), (V((1.8, 5.0, 1.0)), 2.6, 5.0, 2), (V((-2.0, 15.0, -1.2)), 2.7, 15.0, 3))):
        K.blob("butt_rock%d" % i, c, (r_, r_ * 1.15, r_), P["rock2"], seed=40 + sd, lump=0.3, rings=7, segs=10, part="butt_rock")
    K.blob("butt_lava", (0.0, 3.6, 2.4), (2.3, 2.0, 2.0), P["lava"], seed=7, lump=0.2, rings=6, segs=9, part="butt_lava")
    for i, a in enumerate((0.7, 3.4, 5.3)):
        K.drip("butt_drip%d" % i, (3.0 * math.cos(a), 22.0, 3.0 * math.sin(a)), rr.uniform(8, 15), 1.0, P["lava"], seed=60 + i, sides=6, part="butt_lava")
    # rock fragments crusted on the shaft where the meteor's tail drags
    for i in range(6):
        yy = rr.uniform(150, 214)
        a = rr.uniform(0, TAU)
        K.blob("crust%d" % i, (3.3 * math.cos(a), yy, 3.3 * math.sin(a)), (rr.uniform(1.5, 3.0),) * 3, P["rock"] if i % 2 else P["rock2"], seed=70 + i, lump=0.35, rings=5, segs=7, part="shaft_crust")
    # ---------------------------------------------------------------- socket: a blackened iron cup, four blaze rods sweeping up round the rock, glowstone clusters
    lathe("socket_cup", [(2.2, 214.0), (3.6, 216.0), (5.2, 222.0), (4.8, 230.0), (6.0, 238.0), (5.4, 246.0), (4.0, 252.0)], P["iron"], "socket_cup", 10, 36.0)
    for k, y in enumerate((219.0, 228.0, 240.0)):
        ring_band("socket_ring%d" % k, y, 5.2 + 0.3 * k, 0.8, P["rod"], "socket_cup", 10)
    for i in range(4):
        a = TAU * i / 4 + 0.4
        crystal("gstone%d" % i, (5.6 * math.cos(a), 238.0 + rr.uniform(-4, 6), 5.6 * math.sin(a)), (math.cos(a), rr.uniform(0.3, 0.9), math.sin(a)), rr.uniform(6.0, 11.0), rr.uniform(1.1, 1.7), P["gstone"], "socket_gstones", sides=4, rr_=rr)
    # ---------------------------------------------------------------- the meteor: a mass of overlapping, cratered rocks, magma in the cracks, lava pools in the craters
    rocks = [((0, 270, 0), (13.5, 19.0, 11.5), 7), ((-8.5, 258, 3), (8.0, 10.0, 7.0), 8), ((8.0, 280, -2), (7.5, 9.5, 7.0), 9), ((-7.0, 281, 3), (6.5, 8.0, 6.0), 10), ((6.5, 257, 4), (6.5, 8.0, 6.0), 11),
             ((0.5, 291, 3.5), (6.0, 7.0, 5.5), 12), ((-3.5, 250, -3), (7.0, 6.0, 6.0), 13), ((3.0, 268, 8.0), (5.0, 6.0, 4.0), 14)]
    for i, (c, rad, sd) in enumerate(rocks):
        K.blob("head_rock%d" % i, c, rad, P["rock"] if i % 3 else P["rock2"], seed=sd, lump=0.34, lump_scale=0.07, rings=7, segs=10, part="head_rock")
    for i, (x, y, z) in enumerate(((-4.0, 272.0, 10.0), (5.0, 262.0, 8.5), (1.0, 284.0, 8.0), (-7.0, 258.0, 7.5))):
        K.blob("head_pool%d" % i, (x, y, z), (2.8, 2.2, 1.0), P["lava"], seed=80 + i, lump=0.2, rings=5, segs=7, part="head_lava")
    # ---------------------------------------------------------------- the obsidian point: a glassy faceted blade driven through the rock, edges chipped, orange in the cracks
    jag_blade("head_glass", P["glass"], 276.0, 344.0, [(276, 4.0), (286, 5.8), (300, 7.0), (316, 5.6), (330, 3.0), (344, 0.3)],
              [(292, 1.2, 4.0, "step"), (305, 1.0, 3.0, "chip"), (318, 1.4, 4.0, "v"), (328, 0.8, 2.6, "step")], [(288, 1.0, 3.0, "chip"), (298, 1.6, 4.4, "step"), (311, 1.1, 3.4, "v"), (322, 1.0, 3.0, "chip")],
              17, (-0.8, 345.0), (0.8, 343.0), "head_blade", res=6.0, thick=lambda t: 2.0 * (1.0 - 0.5 * t ** 1.6) + 0.1, angle=16.0, wob=0.2, jitter=0.12,
              sec=((-1.0, -0.75, -0.2, 0.0, 0.2, 0.75, 1.0), (0.0, 0.6, 0.95, 1.2, 0.95, 0.6, 0.0)))
    # ---------------------------------------------------------------- the tail: flames streaming back from the rock down the shaft, thick and curled (no flat cards), and forward licks along the point
    tails = [(-10.0, 262.0, 52.0, 3.4, -0.35), (-4.0, 252.0, 66.0, 3.8, -0.1), (3.0, 252.0, 58.0, 3.8, 0.12), (9.5, 262.0, 48.0, 3.2, 0.4), (-12.0, 274.0, 44.0, 2.8, -0.6), (11.0, 276.0, 40.0, 2.8, 0.62),
             (0.0, 246.0, 74.0, 4.2, 0.0), (-7.0, 246.0, 50.0, 3.0, -0.25), (6.5, 247.0, 46.0, 3.0, 0.3)]
    for i, (x, y, ln, rad, ax) in enumerate(tails):
        z0 = rr.uniform(-5.0, 5.0)
        sw = rr.uniform(-5.0, 5.0)
        pts = [(x, y, z0), (x + ax * 0.25 * ln + sw, y - 0.3 * ln, z0 * 1.2), (x + ax * 0.5 * ln - sw, y - 0.62 * ln, z0 * 0.8 + sw * 0.4), (x + ax * 0.9 * ln + sw * 0.5, y - ln, z0 * 0.5)]
        K.tapered("tail%d" % i, pts, rad, 0.25, P["flame"] if i % 2 == 0 else P["flame2"], sides=6, sub=2, part="head_flames", bulge=lambda t: 0.55 + 1.1 * math.sin(min(t * 2.4, math.pi / 2)) * (1 - 0.35 * t))
    for i, (x, y, ln) in enumerate(((-13.0, 282.0, 22.0), (13.5, 270.0, 20.0), (-9.0, 296.0, 16.0), (9.0, 300.0, 14.0))):
        K.tapered("lick%d" % i, [(x, y, 0.0), (x * 1.05, y + 0.35 * ln, 1.5), (x * 0.9 - x * 0.12, y + 0.7 * ln, -1.0), (x * 0.7, y + ln, 0.0)], 2.2, 0.2, P["flame2"], sides=5, sub=2, part="head_flames",
                  bulge=lambda t: 0.6 + 0.9 * math.sin(min(t * 2.2, math.pi / 2)))
    # ---------------------------------------------------------------- orbiting rock chunks (static, bounded)
    def zone(p):
        return abs(p.x) < 16.0 and 236 < p.y < 306 and abs(p.z) < 14.0 or p.y > 300 and abs(p.x) < 9
    K.shard_cloud("chunk", (0, 276, 0), (32.0, 52.0, 16.0), 12, (1.2, 2.6), P["rock"], rr, avoid=zone, part="head_chunks", stretch=1.3, sides=5, max_count=12, mat2=P["lava"], kinds=3,
                  flow=lambda p: V((-p.z * 0.1, -1.0, p.x * 0.1)))
    return K.quad_estimate()


# ================================================================ 5. PORTAL SPEAR: an obsidian-and-gold blade driven through ringed portals
def portal_palette():
    P = {}
    P["obs"] = K.surface("pobs", rgb(18, 14, 30), 0.8, 0.2, wear=1.0, wear_color=rgb(170, 130, 255), wear_metal=1.0, dirt=0.8, dirt_color=(0.003, 0.002, 0.008, 1), scratches=0.8, bump="hammer", bump_scale=0.5,
                         bump_strength=0.2, mottle=1.0, mottle_color=rgb(50, 26, 90), seed=401, bevel=0.2, ao_dist=2.5, fine=0.4)
    P["gold"] = K.surface("pgold", rgb(222, 164, 54), 1.0, 0.26, wear=0.9, wear_color=rgb(255, 238, 170), dirt=0.85, dirt_color=(0.04, 0.02, 0.005, 1), scratches=0.7, bump="hammer", bump_scale=0.55, bump_strength=0.18,
                          mottle=0.8, mottle_color=rgb(150, 82, 22), seed=402, bevel=0.18, ao_dist=2.0)
    P["blade"] = K.surface("pblade", rgb(30, 24, 52), 0.9, 0.18, wear=1.0, wear_color=rgb(190, 160, 255), wear_metal=1.0, dirt=0.7, scratches=0.8, bump="brushed", bump_strength=0.12, mottle=1.0,
                           mottle_color=rgb(70, 36, 120), seed=403, bevel=0.2, ao_dist=2.5, cramp=("y", 280.0, 345.0, [(0.0, rgb(22, 18, 40)), (0.7, rgb(40, 30, 74)), (1.0, rgb(110, 90, 190))]))
    P["portal"] = K.surface("pportal", rgb(120, 70, 255), 0.0, 0.3, emit=rgb(140, 90, 255), emit_strength=0.75, emit_flicker=0.8, mottle=1.0, mottle_color=rgb(30, 160, 255), veins=0.8, vein_color=rgb(210, 240, 255),
                            vein_strength=0.9, seed=404, bevel=0.0)
    P["arm"] = K.surface("parm", rgb(80, 70, 190), 0.0, 0.3, emit=rgb(120, 130, 255), emit_strength=0.4, emit_flicker=0.6, mottle=1.0, mottle_color=rgb(50, 120, 220), seed=405, bevel=0.0)
    P["void"] = K.surface("pvoid", rgb(8, 4, 20), 0.2, 0.3, mottle=1.0, mottle_color=rgb(40, 20, 90), bump="leather", bump_scale=0.6, bump_strength=0.4, seed=406, bevel=0.0)
    P["pearl"] = K.surface("ppearl", rgb(40, 220, 180), 0.1, 0.1, emit=rgb(60, 255, 200), emit_strength=0.7, emit_flicker=0.5, mottle=1.0, mottle_color=rgb(20, 120, 140), veins=1.3, vein_color=rgb(220, 255, 240),
                           vein_strength=0.7, seed=407, bevel=0.0)
    P["stone"] = K.surface("pstone", rgb(150, 220, 255), 0.1, 0.15, wear=0.8, wear_color=rgb(240, 252, 255), wear_metal=0.0, emit=rgb(120, 200, 255), emit_strength=0.5, emit_flicker=0.5, mottle=1.0,
                           mottle_color=rgb(60, 120, 220), seed=408, bevel=0.06)
    P["wrap"] = K.surface("pwrap", rgb(28, 20, 44), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(6, 3, 12), groove_amount=0.9, dirt=0.8, mottle=0.7, mottle_color=rgb(16, 10, 30), wear=0.6,
                          wear_color=rgb(120, 80, 180), wear_metal=0.0, fine=0.6, seed=409, bevel=0.0)
    P["cord"] = K.surface("pcord", rgb(222, 164, 54), 0.2, 0.5, bump="wrap", bump_strength=0.8, groove=rgb(70, 40, 8), groove_amount=0.8, wear=0.7, wear_color=rgb(255, 232, 150), wear_metal=0.0, dirt=0.6, seed=410, bevel=0.0)
    return P


def portal_spear(opts):
    begin(opts["samples"])
    P = portal_palette()
    rr = K.rng(5505)
    plain_shaft(P["obs"], 18.0, 218.0, 2.4, sides=8, bands=(48.0, 97.5, 127.0, 160.0, 196.0), band_mat=P["gold"], band_h=1.0, sq=1.0, taper=0.05, swell=0.05)
    K.grip_wrapped("grip", 98.0, 126.0, 2.5, P["wrap"], P["cord"], turns=6.0, cord=0.38, bands=(99.0, 125.0), band_mat=P["gold"], swell=0.09, part="grip", seed=3, sides=8)
    lathe("butt_cap", [(0.01, 0.0), (1.2, 1.2), (2.6, 4.0), (3.5, 8.0), (3.6, 12.0), (2.9, 16.0), (2.5, 20.0)], P["gold"], "butt_cap", 10, 40.0)
    ring_band("butt_ring", 8.0, 3.4, 0.7, P["obs"], "butt_cap", 10)
    K.blob("butt_pearl", (0.0, 2.8, 0.0), (2.2, 2.0, 2.2), P["pearl"], seed=3, lump=0.05, rings=8, segs=10, part="butt_pearl")
    # a small ring of runes round the shaft at 160: a floating mini portal ring that the shaft passes through
    torus("mini_ring", (0.0, 160.0, 0.0), 7.5, 0.7, P["gold"], "shaft_ring", plane="xy", n=20, sides=6, tilt=65.0)
    for i in range(3):
        a = TAU * i / 3 + 0.5
        K.blob("mini_stone%d" % i, (7.5 * math.cos(a), 160.0 + 7.5 * math.sin(a) * math.sin(math.radians(65)), 7.5 * math.sin(a) * math.cos(math.radians(65))), (1.0, 1.0, 1.0), P["stone"], seed=i, lump=0.05, rings=4, segs=6, part="shaft_ring")
    # ---------------------------------------------------------------- socket: a gold-banded obsidian collar with an ender pearl in claws
    lathe("socket_collar", [(2.2, 214.0), (3.6, 216.0), (4.6, 222.0), (4.2, 228.0), (5.0, 234.0), (4.0, 242.0), (3.0, 248.0)], P["obs"], "socket_collar", 10, 34.0)
    for k, y in enumerate((218.0, 226.0, 236.0)):
        ring_band("socket_ring%d" % k, y, 4.3 + 0.3 * k, 0.8, P["gold"], "socket_collar", 10)
    claw_gem("socket_pearl", 230.0, 3.0, P["pearl"], P["gold"], rr, "socket_pearl", claws=5, sides=10, stretch=1.0, z=4.5, claw_len=1.1, claw_r=0.55, collar=False, csides=4, csub=1)
    # ---------------------------------------------------------------- the great portal: an obsidian-and-gold ring facing the wielder, a funnel of void inside it with spiral arms of light
    YC = 266.0
    R1 = 19.5
    torus("ring_main", (0.0, YC, 0.0), R1, 2.4, P["obs"], "head_ring", n=28, sides=7, scale=lambda t: 2.4 * (1.0 + 0.12 * math.sin(t * TAU * 12)))
    torus("ring_gold_out", (0.0, YC, 0.0), R1 + 1.5, 0.8, P["gold"], "head_ring", n=24, sides=5)
    torus("ring_gold_in", (0.0, YC, 1.2), R1 - 2.3, 0.7, P["gold"], "head_ring", n=24, sides=5)
    for k in range(4):                                                         # rune plates (stepped, bevelled) and studs round the ring
        a = TAU * k / 4 + 0.5
        for s_ in (1,):
            pl = K.plate("ring_plate%d_%d" % (k, s_), [(-1.9, -3.0), (1.9, -3.0), (2.4, 0.0), (1.9, 3.0), (-1.9, 3.0), (-2.4, 0.0)], 2.1 * s_ if s_ > 0 else -2.6, 2.6 if s_ > 0 else -2.1, P["gold"], part="head_ring_plates", bevel_w=0.22)
            bl.move(pl, (R1 * math.cos(a), R1 * math.sin(a), 0.0), (0, 0, math.degrees(a) - 90.0))
    for k, a in enumerate((math.pi / 2, math.pi, 1.5 * math.pi, 0.0)):         # four ender stones at the points of the ring
        claw_gem("ring_stone%d" % k, 0.0, 2.1, P["stone"], P["gold"], rr, "head_ring_stones", claws=3, sides=5, stretch=1.2, collar=False, csides=4, csub=1)
        for ob in [o for o in bpy.context.scene.collection.objects if o.name.startswith("ring_stone%d" % k)]:
            bl.move(ob, (R1 * math.cos(a), YC + R1 * math.sin(a), 2.8), (0, 0, 0))
            if ob.name.startswith("ring_stone%d" % k):
                pass
    # the funnel: a dished annulus of void, with four spiral arms of light winding into the centre
    prof = [(R1 - 2.6, 0.0), (R1 - 4.5, -1.0), (R1 - 8.0, -2.4), (R1 - 11.5, -4.6), (R1 - 14.5, -7.8), (3.6, -11.0), (3.0, -11.5)]
    fun = bl.lathe("funnel", [(r_, h) for r_, h in prof], P["void"], sides=40, part="head_funnel", axis="z", caps=(False, False))
    bl.move(fun, (0.0, YC, 0.0))
    K._finish_obj(fun, "head_funnel", True, 50.0)
    for arm in range(4):
        pts = []
        for i in range(10):
            t = i / 9
            rad = (R1 - 3.5) * (1.0 - t) ** 0.9 + 3.6 * t
            ang = arm * TAU / 4 + 1.9 * TAU * t * 0.75
            h = -(1.0 + 10.0 * t ** 1.3) * 1.0 + 0.5
            pts.append(V((rad * math.cos(ang), YC + rad * math.sin(ang), h)))
        K.tapered("arm%d" % arm, pts, 1.1, 0.35, P["arm"], sides=5, sub=1, part="head_arms")
    # ---------------------------------------------------------------- the second ring edge-on to the first (an armillary), and a third small ring round the blade
    torus("ring_cross", (0.0, YC, 0.0), 13.0, 1.5, P["gold"], "head_ring2", plane="yz", n=20, sides=5, scale=lambda t: 1.5 * (1.0 + 0.2 * math.sin(t * TAU * 8)))
    for k in range(4):
        a = TAU * k / 4 + 0.3
        K.rivets("ring_cross_rivet%d" % k, [(V((0.0, YC + 13.0 * math.sin(a), 13.0 * math.cos(a))) + V((1.4, 0, 0)), (1, 0, 0)), (V((0.0, YC + 13.0 * math.sin(a), 13.0 * math.cos(a))) + V((-1.4, 0, 0)), (-1, 0, 0))],
                 P["obs"], r=0.7, part="head_ring2", detail=False, sides=6)
    torus("ring_far", (0.0, 312.0, 0.0), 9.0, 1.0, P["gold"], "head_ring3", n=30, sides=6, tilt=-35.0)
    # ---------------------------------------------------------------- the blade: a slender obsidian-and-gold kite through the centre of the rings
    jag_blade("head_blade", P["blade"], 244.0, 344.0, [(244, 2.2), (258, 3.0), (274, 3.4), (292, 5.8), (310, 6.4), (326, 4.0), (344, 0.4)],
              [(300, 0.9, 3.0, "chip"), (316, 0.8, 2.6, "step")], [(296, 0.8, 2.6, "step"), (312, 1.0, 3.0, "chip")], 23, (-0.5, 345.0), (0.6, 343.0), "head_blade",
              thick=lambda t: 1.8 * (1.0 - 0.45 * t ** 1.6) + 0.1, angle=40.0, wob=0.0, jitter=0.06, res=5.5,
              sec=((-1.0, -0.78, -0.3, 0.0, 0.3, 0.78, 1.0), (0.0, 0.62, 0.95, 1.25, 0.95, 0.62, 0.0)))
    K.engrave_lines("blade_line", [[(0.0, 290.0), (0.0, 336.0)]], P["gold"], side=1, z=2.2, w=0.35, part="head_inlay", detail=False)
    K.engrave_lines("blade_line_b", [[(0.0, 290.0), (0.0, 336.0)]], P["gold"], side=-1, z=2.2, w=0.35, part="head_inlay", detail=False)
    # four ender pearls and shards drifting round the rings (bounded)
    for i, (x, y, z, r_) in enumerate(((-26.0, 292.0, 4.0, 2.4), (25.0, 244.0, -3.0, 2.0), (27.0, 300.0, 2.0, 1.8))):
        K.blob("pearl%d" % i, (x, y, z), (r_, r_, r_), P["pearl"], seed=i + 5, lump=0.04, rings=6, segs=8, part="head_pearls")
    K.shard_cloud("shard", (0, 275, 0), (30.0, 55.0, 12.0), 10, (0.9, 1.6), P["portal"], rr, avoid=lambda p: math.hypot(p.x, p.y - YC) < 24.0 and abs(p.z) < 14 or abs(p.x) < 8 and p.y > 244,
                  part="head_shards", stretch=2.0, sides=4, max_count=7, flow=lambda p: V((-(p.y - YC) * 0.1, (p.x) * 0.1, 0.3)))
    return K.quad_estimate()


# ================================================================ shared: compact material helper, a blade standing off the shaft axis, standard lower spear
def mat(name, col, metal, rough, seed, **kw):
    """A worn, baked material with sensible defaults (edge wear, grime, scratches) so each palette stays short."""
    kw.setdefault("wear", 0.9)
    kw.setdefault("dirt", 0.8)
    kw.setdefault("scratches", 0.5)
    kw.setdefault("mottle", 0.9)
    kw.setdefault("bevel", 0.16)
    kw.setdefault("ao_dist", 2.0)
    if "wear_color" not in kw:
        kw["wear_color"] = tuple(min(1.0, c * 1.6 + 0.15) for c in col[:3]) + (1.0,)
    if "mottle_color" not in kw:
        kw["mottle_color"] = tuple(c * 0.5 for c in col[:3]) + (1.0,)
    if metal >= 0.8:
        kw.setdefault("wear_metal", 1.0)
    return K.surface(name, col, metal, rough, seed=seed, **kw)


def off_blade(name, m, length, stations, seed, part, at=(0, 0, 0), rot=(0, 0, 0), thick=1.4, nl=(), nr=(), sec=None, res=4.0, angle=30.0, wob=0.0, jitter=0.08):
    """A jag_blade built along +y from 0..length, then rotated (degrees, XYZ) and moved: prongs, fins, vanes standing off the spear axis."""
    ob = jag_blade(name, m, 0.0, length, stations, list(nl), list(nr), seed, (0.0, length), (0.0, length), part, thick=thick, sec=sec, angle=angle, wob=wob, jitter=jitter, res=res)
    bl.move(ob, rot=rot)
    bl.move(ob, at)
    return ob


def lower_spear(P, shaft, band, wrap, cord, y_top=218.0, r=2.4, bands=(48.0, 97.5, 127.0, 160.0, 196.0), butt=None, sides=8):
    plain_shaft(shaft, 18.0, y_top, r, sides=sides, bands=bands, band_mat=band, band_h=1.0, sq=1.0, taper=0.05, swell=0.05)
    K.grip_wrapped("grip", 98.0, 126.0, 2.5, wrap, cord, turns=7.0, cord=0.38, bands=(99.0, 125.0), band_mat=band, swell=0.09, part="grip", seed=3, sides=sides)


# ================================================================ 6. TRIAINA: a gold-and-pearl trident, three leaf blades, sapphires at the fork
def triaina_palette():
    P = {}
    P["pearl"] = mat("tpearl", rgb(240, 238, 232), 0.3, 0.3, 601, bump="brushed", bump_strength=0.1, mottle_color=rgb(170, 200, 230), scratches=0.5, dirt_color=(0.05, 0.05, 0.06, 1))
    P["gold"] = mat("tgold", rgb(232, 172, 60), 1.0, 0.25, 602, bump="hammer", bump_scale=0.55, bump_strength=0.18, dirt_color=(0.05, 0.025, 0.006, 1), mottle_color=rgb(160, 90, 24), scratches=0.7)
    P["blade"] = mat("tblade", rgb(226, 232, 244), 0.9, 0.16, 603, bump="brushed", bump_strength=0.1, wear_color=rgb(255, 255, 255), mottle_color=rgb(120, 160, 220),
                     cramp=("y", 250.0, 345.0, [(0.0, rgb(200, 210, 232)), (0.7, rgb(230, 236, 250)), (1.0, rgb(150, 190, 255))]), scratches=0.8)
    P["sapph"] = mat("tsapph", rgb(40, 110, 240), 0.1, 0.1, 604, emit=rgb(70, 150, 255), emit_strength=0.6, emit_flicker=0.5, veins=1.2, vein_color=rgb(200, 230, 255), vein_strength=0.5, bevel=0.06, dirt=0.3)
    P["wrap"] = mat("twrap", rgb(22, 52, 70), 0.0, 0.6, 605, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(4, 14, 22), groove_amount=0.9, bevel=0.0, fine=0.6)
    P["cord"] = mat("tcord", rgb(232, 172, 60), 0.2, 0.5, 606, bump="wrap", bump_strength=0.8, groove=rgb(70, 40, 8), groove_amount=0.8, bevel=0.0, wear_metal=0.0)
    P["glow"] = mat("tglow", rgb(120, 190, 255), 0.0, 0.3, 607, emit=rgb(150, 210, 255), emit_strength=0.8, emit_flicker=0.7, bevel=0.0, wear=0.0, dirt=0.0, scratches=0.0)
    return P


def triaina_spear(opts):
    begin(opts["samples"])
    P = triaina_palette()
    rr = K.rng(6606)
    lower_spear(P, P["pearl"], P["gold"], P["wrap"], P["cord"])
    lathe("butt_cap", [(0.01, 0.0), (1.4, 1.5), (2.8, 5.0), (3.4, 10.0), (3.0, 16.0), (2.5, 20.0)], P["gold"], "butt_cap", 9, 40.0)
    for i in range(3):                                                           # three sapphire toes: the number three runs through every part
        a = TAU * i / 3 + 0.3
        crystal("butt_toe%d" % i, (2.6 * math.cos(a), 8.0, 2.6 * math.sin(a)), (1.4 * math.cos(a), -1.0, 1.4 * math.sin(a)), 8.0, 1.3, P["sapph"], "butt_toes", sides=5, rr_=rr)
    # ---------------------------------------------------------------- socket: a flared gold cup with three swept fins, a sapphire in claws
    lathe("socket_cup", [(2.2, 212.0), (3.6, 214.0), (5.4, 222.0), (4.6, 230.0), (6.4, 238.0), (5.6, 246.0), (4.4, 252.0)], P["gold"], "socket_cup", 12, 36.0)
    for k, y in enumerate((218.0, 232.0)):
        ring_band("socket_ring%d" % k, y, 5.0 + 0.9 * k, 0.9, P["pearl"], "socket_cup", 12)
    for i in range(3):
        a = TAU * i / 3 + 0.5
        c, s_ = math.cos(a), math.sin(a)
        K.tapered("fin%d" % i, [(5.0 * c, 220.0, 5.0 * s_), (8.5 * c, 232.0, 8.5 * s_), (9.0 * c, 246.0, 9.0 * s_), (5.0 * c, 258.0, 5.0 * s_)], 1.5, 0.35, P["pearl"], sides=6, sub=3, part="socket_fins",
                  bulge=lambda t: 0.8 + 0.7 * math.sin(t * math.pi))
    claw_gem("fork_gem", 260.0, 4.0, P["sapph"], P["gold"], rr, "fork_gem", claws=6, sides=8, stretch=1.35)
    # ---------------------------------------------------------------- the centre blade: a long pearl leaf with a gold midrib
    jag_blade("head_centre", P["blade"], 262.0, 346.0, [(262, 2.4), (274, 4.4), (294, 6.4), (316, 5.2), (334, 2.8), (346, 0.3)],
              [(300, 0.8, 2.6, "chip"), (318, 0.7, 2.2, "step")], [(290, 0.7, 2.4, "step"), (312, 0.8, 2.8, "chip")], 61, (0.0, 346.0), (0.1, 345.0), "head_blade",
              thick=lambda t: 2.0 * (1.0 - 0.5 * t ** 1.5) + 0.1, angle=40.0, wob=0.0, jitter=0.06, res=5.0, sec=((-1.0, -0.78, -0.3, 0.0, 0.3, 0.78, 1.0), (0.0, 0.62, 0.95, 1.25, 0.95, 0.62, 0.0)))
    K.tapered("midrib", [(0.0, 266.0, 2.0), (0.0, 300.0, 2.4), (0.0, 334.0, 1.4)], 0.55, 0.2, P["gold"], sides=5, sub=3, part="head_rib")
    # ---------------------------------------------------------------- the two curved prongs: gold arms out and up, each ending in its own pearl blade; sapphire studs along the arm
    for sx in (1, -1):
        arm = [(sx * 3.5, 255.0, 0.0), (sx * 11.0, 262.0, 0.0), (sx * 18.0, 276.0, 0.0), (sx * 20.5, 292.0, 0.0), (sx * 19.0, 306.0, 0.0)]
        K.tapered("prong%d" % sx, arm, 2.1, 1.1, P["gold"], sides=7, sub=3, part="head_prongs", bulge=lambda t: 1.0 + 0.12 * math.sin(t * 12.0))
        off_blade("prong_blade%d" % sx, P["blade"], 36.0 + (2.0 if sx > 0 else 0.0), [(0, 2.2), (12, 4.2), (24, 3.4), (33, 1.6), (38, 0.3)], 70 + sx, "head_blade", at=(sx * 19.0, 304.0, 0.0), rot=(0, 0, -sx * 5.0),
                  thick=1.5, nl=[(18, 0.6, 2.0, "chip")] if sx > 0 else [(22, 0.6, 2.0, "step")], nr=[(14, 0.6, 2.0, "step")] if sx > 0 else [(16, 0.6, 2.0, "chip")], res=5.0, angle=40.0)
        ring_band("prong_ring%d" % sx, 276.0, 0.0, 0.0, P["pearl"], "head_prongs", 6) if False else None
        for k, (px, py) in enumerate(((11.0, 262.0), (18.0, 276.0), (20.5, 292.0))):
            claw_gem("stud%d_%d" % (sx, k), py, 1.6, P["sapph"], P["gold"], rr, "head_studs", claws=3, sides=6, stretch=1.2, z=2.6, collar=False)
            for o in [o for o in bpy.context.scene.collection.objects if o.name.startswith("stud%d_%d" % (sx, k))]:
                bl.move(o, (sx * px, 0.0, 0.0))
    # a gold bridge between the prongs under the centre blade, like a trident's cross-piece
    K.tapered("bridge", [(-11.0, 262.0, 0.0), (-5.0, 268.0, 0.0), (0.0, 270.0, 0.0), (5.0, 268.0, 0.0), (11.0, 262.0, 0.0)], 1.3, 1.3, P["gold"], sides=6, sub=3, part="head_bridge")
    K.shard_cloud("spark", (0, 290, 0), (30.0, 50.0, 10.0), 8, (0.8, 1.4), P["glow"], rr, avoid=lambda p: abs(p.x) < 24 and 250 < p.y < 345, part="head_sparks", stretch=2.0, sides=4, max_count=8,
                  flow=lambda p: V((0, 1, 0)))
    return K.quad_estimate()


# ================================================================ 7. BRUENACK: a three-vaned crimson lance on a crescent guard, flame licking the vanes
def bruenack_palette():
    P = {}
    P["steel"] = mat("bsteel", rgb(150, 24, 28), 0.85, 0.2, 701, bump="brushed", bump_strength=0.1, wear_color=rgb(255, 200, 170), mottle_color=rgb(70, 8, 10), scratches=0.8, veins=0.3, vein_color=rgb(255, 110, 40), vein_strength=0.0,
                     cramp=("y", 236.0, 346.0, [(0.0, rgb(90, 14, 18)), (0.6, rgb(170, 30, 30)), (1.0, rgb(255, 120, 60))]))
    P["dark"] = mat("bdark", rgb(34, 22, 24), 0.9, 0.4, 702, bump="hammer", bump_scale=0.5, bump_strength=0.2, wear_color=rgb(200, 130, 110), mottle_color=rgb(70, 14, 14), dirt_color=(0.004, 0.002, 0.002, 1))
    P["gold"] = mat("bgold", rgb(226, 160, 52), 1.0, 0.26, 703, bump="hammer", bump_scale=0.55, bump_strength=0.18, dirt_color=(0.05, 0.025, 0.006, 1), mottle_color=rgb(150, 80, 22))
    P["ember"] = mat("bember", rgb(255, 90, 24), 0.1, 0.15, 704, emit=rgb(255, 110, 30), emit_strength=0.9, emit_flicker=0.7, veins=1.0, vein_color=rgb(255, 230, 150), vein_strength=0.6, bevel=0.06, dirt=0.3)
    P["flame"] = K.surface("bflame", rgb(255, 100, 24), 0.0, 0.5, feather=(7, 3, 0.35, 0.4), tramp=[(0.0, rgb(255, 230, 130)), (0.4, rgb(255, 130, 30)), (1.0, rgb(190, 30, 14))],
                           emit=rgb(255, 130, 30), emit_strength=1.0, temit=[(0.0, rgb(255, 230, 130)), (0.5, rgb(255, 130, 30)), (1.0, rgb(200, 40, 12))], emit_flicker=0.7, seed=705, bevel=0.0)
    P["cloth"] = mat("bcloth", rgb(150, 18, 24), 0.0, 0.7, 706, bump="cross", bump_scale=1.2, bump_strength=0.4, groove=rgb(40, 4, 6), groove_amount=0.8, bevel=0.0, fine=0.5, wear_metal=0.0)
    P["wrap"] = mat("bwrap", rgb(26, 18, 18), 0.0, 0.6, 707, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(6, 3, 3), groove_amount=0.9, bevel=0.0, fine=0.6, wear_metal=0.0)
    P["cord"] = mat("bcord", rgb(226, 160, 52), 0.2, 0.5, 708, bump="wrap", bump_strength=0.8, groove=rgb(70, 40, 8), groove_amount=0.8, bevel=0.0, wear_metal=0.0)
    return P


def bruenack_spear(opts):
    begin(opts["samples"])
    P = bruenack_palette()
    rr = K.rng(7707)
    lower_spear(P, P["dark"], P["gold"], P["wrap"], P["cord"], y_top=232.0)
    lathe("butt_cap", [(0.01, 0.0), (1.2, 1.4), (2.4, 4.0), (3.0, 9.0), (2.7, 15.0), (2.5, 20.0)], P["gold"], "butt_cap", 9, 40.0)
    K.blob("butt_gem", (0.0, 2.6, 0.0), (2.0, 2.2, 2.0), P["ember"], seed=4, lump=0.05, rings=7, segs=9, part="butt_gem")
    for i, a in enumerate((0.9, 3.0, 5.2)):
        hanging("butt_tassel%d" % i, (2.0 * math.cos(a), 12.0, 2.0 * math.sin(a)), 22.0 + 6 * i, 1.5, P["cloth"], rr, "butt_tassel", sway=1.6, fray=0.6, n=7)
    # ---------------------------------------------------------------- crescent guard: a gold moon cradling the lance foot, an ember in claws, tasselled
    pts = [(-17.0, 262.0, 0.0), (-12.0, 249.0, 0.0), (-6.0, 241.0, 0.0), (0.0, 238.5, 0.0), (6.0, 241.0, 0.0), (12.0, 249.0, 0.0), (17.0, 262.0, 0.0)]
    K.tapered("guard", pts, 2.6, 0.5, P["gold"], sides=8, sub=3, part="guard", bulge=lambda t: 0.5 + 1.0 * math.sin(t * math.pi) ** 0.7 + 0.1)
    lathe("lance_socket", [(2.4, 230.0), (4.0, 234.0), (5.2, 240.0), (4.4, 248.0), (3.2, 254.0)], P["gold"], "socket", 10, 38.0)
    ring_band("socket_ring0", 236.0, 4.6, 0.8, P["dark"], "socket", 10)
    claw_gem("guard_gem", 246.0, 3.4, P["ember"], P["gold"], rr, "guard_gem", claws=5, sides=8, stretch=1.3, z=4.4, collar=False)
    for sx in (1, -1):
        hanging("guard_tassel%d" % sx, (sx * 16.0, 258.0, 0.0), 30.0, 3.0, P["cloth"], rr, "guard_tassel", sway=3.0, fray=0.8, notch=0.3, n=9)
    # ---------------------------------------------------------------- the lance: a fluted crimson core and three swept vanes at 120 degrees, each with its own chipped edge
    lathe("core", [(2.6, 250.0), (2.9, 270.0), (2.6, 296.0), (1.8, 322.0), (0.9, 338.0), (0.04, 349.0)], P["steel"], "head_core", 8, 30.0)
    for i in range(3):
        rot = (0, 120.0 * i + 15.0, 0)
        off_blade("vane%d" % i, P["steel"], 92.0 - 4.0 * (i % 2), [(0, 2.2), (14, 7.5), (34, 9.4), (58, 7.2), (78, 3.4), (92, 0.3)], 80 + i, "head_blade", at=(0, 252.0, 0.0), rot=rot, thick=1.3,
                  nl=[(28, 0.9, 3.0, "step"), (50, 0.8, 2.6, "chip")] if i != 1 else [(36, 1.0, 3.0, "chip")], nr=[(22, 0.8, 2.6, "chip"), (44, 0.9, 3.0, "step")] if i != 2 else [(40, 0.9, 3.0, "step")],
                  res=5.0, angle=35.0, sec=((-1.0, -0.7, -0.15, 0.15, 0.7, 1.0), (0.0, 0.7, 1.0, 1.0, 0.7, 0.0)))
    K.helix_wrap("core_cord", 252.0, 326.0, 3.0, P["gold"], turns=6.0, cord=0.38, part="head_cord", seed=5, rng_=rr, sub=4, sides=5)
    # flame tongues curling up the vanes (real volume; the glow is baked from emission)
    for i in range(6):
        a = TAU * i / 6 + 0.4
        x0, z0, y0 = 5.0 * math.cos(a), 5.0 * math.sin(a), 256.0 + 6.0 * (i % 3)
        L_ = 40.0 + 10.0 * (i % 2)
        K.tapered("flame%d" % i, [(x0, y0, z0), (x0 * 1.9, y0 + 0.3 * L_, z0 * 1.9), (x0 * 1.2 + 2.0, y0 + 0.6 * L_, z0 * 1.2 - 2.0), (x0 * 0.5, y0 + L_, z0 * 0.5)], 2.0, 0.2, P["flame"], sides=6, sub=3, part="head_flames",
                  bulge=lambda t: 0.55 + 1.0 * math.sin(min(t * 2.4, math.pi / 2)) * (1 - 0.3 * t))
    return K.quad_estimate()


# ================================================================ 8. CLOCKWORK SPEAR: a brass clock-face and cog head, springs on the shaft
def clockwork_palette():
    P = {}
    P["brass"] = mat("kbrass", rgb(200, 150, 62), 1.0, 0.3, 801, bump="hammer", bump_scale=0.5, bump_strength=0.15, dirt_color=(0.04, 0.025, 0.008, 1), mottle_color=rgb(120, 76, 20), scratches=0.8)
    P["copper"] = mat("kcopper", rgb(176, 96, 58), 1.0, 0.34, 802, bump="brushed", bump_strength=0.12, dirt_color=(0.03, 0.012, 0.006, 1), mottle_color=rgb(60, 110, 90), scratches=0.7, mottle=1.0)
    P["steel"] = mat("ksteel", rgb(120, 128, 138), 1.0, 0.3, 803, bump="brushed", bump_strength=0.14, dirt_color=(0.006, 0.006, 0.008, 1), scratches=0.9, wear_color=rgb(236, 240, 246),
                     cramp=("y", 288.0, 346.0, [(0.0, rgb(90, 98, 108)), (0.7, rgb(150, 158, 168)), (1.0, rgb(210, 218, 226))]))
    P["enamel"] = mat("kenamel", rgb(232, 222, 196), 0.0, 0.35, 804, bump="grain", bump_scale=2.0, bump_strength=0.1, mottle_color=rgb(160, 130, 80), dirt_color=(0.03, 0.02, 0.01, 1), wear_metal=0.0, bevel=0.1)
    P["dark"] = mat("kdark", rgb(40, 30, 26), 0.9, 0.45, 805, bump="hammer", bump_scale=0.5, bump_strength=0.2, mottle_color=rgb(80, 56, 30), wear_color=rgb(190, 140, 80))
    P["amber"] = mat("kamber", rgb(255, 170, 40), 0.1, 0.1, 806, emit=rgb(255, 180, 60), emit_strength=0.7, emit_flicker=0.4, veins=1.0, vein_color=rgb(255, 240, 180), vein_strength=0.4, bevel=0.06, dirt=0.3)
    P["wrap"] = mat("kwrap", rgb(48, 30, 22), 0.0, 0.6, 807, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(10, 5, 3), groove_amount=0.9, bevel=0.0, fine=0.6, wear_metal=0.0)
    P["cord"] = mat("kcord", rgb(200, 150, 62), 0.2, 0.5, 808, bump="wrap", bump_strength=0.8, groove=rgb(60, 36, 8), groove_amount=0.8, bevel=0.0, wear_metal=0.0)
    return P


def clockwork_spear(opts):
    begin(opts["samples"])
    P = clockwork_palette()
    rr = K.rng(8808)
    lower_spear(P, P["dark"], P["brass"], P["wrap"], P["cord"], y_top=216.0, bands=(48.0, 97.5, 127.0))
    lathe("butt_cap", [(0.01, 0.0), (1.4, 1.2), (3.0, 4.0), (3.5, 10.0), (3.0, 16.0), (2.5, 20.0)], P["brass"], "butt_cap", 10, 40.0)
    # gears threaded on the shaft and springs between them
    for k, (y, ro, ri, tc, m) in enumerate(((56.0, 7.2, 5.8, 12, "copper"), (176.0, 8.0, 6.4, 14, "brass"))):
        K.gear("shaft_gear%d" % k, (0.0, y, 0.0), ro, ri, tc, 1.3, P[m], hole=2.5, normal=(0, 0, 1), part="shaft_gear", rng_=rr, spokes=0, bevel_w=0.14)
    K.spring("spring0", (0.0, 150.0, 0.0), (0.0, 168.0, 0.0), 3.6, 0.5, 5.0, P["steel"], sides=5, part="shaft_spring")
    K.spring("spring1", (0.0, 66.0, 0.0), (0.0, 92.0, 0.0), 3.6, 0.5, 6.0, P["steel"], sides=5, part="shaft_spring")
    # ---------------------------------------------------------------- socket: brass drum with rivets, key winder on the side
    lathe("socket_drum", [(2.2, 214.0), (3.8, 216.0), (5.6, 222.0), (5.2, 232.0), (6.2, 240.0), (4.6, 248.0)], P["brass"], "socket_drum", 12, 36.0)
    for k, y in enumerate((220.0, 236.0)):
        ring_band("socket_ring%d" % k, y, 5.6 + 0.3 * k, 0.9, P["copper"], "socket_drum", 12)
    K.tapered("key_stem", [(5.6, 228.0, 0.0), (10.0, 228.0, 0.0)], 0.8, 0.8, P["steel"], sides=6, sub=2, part="socket_key")
    K.gear("key_wing", (11.0, 228.0, 0.0), 3.6, 1.8, 6, 0.9, P["brass"], hole=0.0, normal=(1, 0, 0), part="socket_key", rng_=rr, bevel_w=0.1)
    # ---------------------------------------------------------------- the clock head: a big rear cog, a stepped bezel, an enamel face with 12 hour bars and two hands
    YC = 272.0
    K.gear("rear_cog", (0.0, YC, -3.4), 22.0, 18.5, 14, 2.0, P["copper"], hole=0.0, normal=(0, 0, 1), part="head_cog", rng_=rr, spokes=0, bevel_w=0.2)
    K.gear("side_cog", (19.0, YC - 15.0, -1.6), 9.5, 7.8, 9, 1.8, P["brass"], hole=2.0, normal=(0, 0, 1), part="head_cog", rng_=rr, spokes=0, bevel_w=0.16)
    torus("bezel_out", (0.0, YC, 0.0), 17.5, 1.8, P["brass"], "head_bezel", n=30, sides=7, scale=lambda t: 1.8 * (1.0 + 0.06 * math.sin(t * TAU * 20)))
    torus("bezel_in", (0.0, YC, 0.6), 15.2, 0.9, P["copper"], "head_bezel", n=32, sides=6)
    fc = bl.lathe("face", [(0.01, 0.0), (15.0, 0.0), (15.2, -0.9), (14.0, -1.4), (0.01, -1.6)], P["enamel"], sides=36, part="head_face", axis="z", caps=(True, True))
    bl.move(fc, (0.0, YC, 0.0))
    K._finish_obj(fc, "head_face", True, 40.0)
    for k in range(12):
        a = TAU * k / 12
        r0, r1 = (11.2, 14.0) if k % 3 == 0 else (12.6, 14.0)
        K.bar("hour%d" % k, (r0 * math.cos(a), YC + r0 * math.sin(a), 0.8), (r1 * math.cos(a), YC + r1 * math.sin(a), 0.8), 0.45 if k % 3 else 0.7, 0.4, P["dark"], part="head_hours", bevel_w=0.08, detail=True)
    K.tapered("hand_min", [(0.0, YC, 1.4), (-1.0, YC + 6.0, 1.6), (0.0, YC + 12.5, 1.8)], 1.1, 0.25, P["dark"], sides=6, sub=2, part="head_hands")
    K.tapered("hand_hour", [(0.0, YC, 2.2), (2.4, YC - 3.0, 2.3), (4.6, YC - 6.0, 2.4)], 1.5, 0.35, P["dark"], sides=6, sub=2, part="head_hands")
    K.blob("hand_pin", (0.0, YC, 2.8), (1.8, 1.8, 1.4), P["amber"], seed=2, lump=0.04, rings=6, segs=9, part="head_pin")
    # ---------------------------------------------------------------- the blade through the clock, rising out of its top; a spike below, a chipped, engraved steel leaf
    jag_blade("head_blade", P["steel"], 286.0, 346.0, [(286, 3.0), (298, 5.4), (316, 5.0), (332, 2.6), (346, 0.3)], [(304, 0.8, 2.6, "chip"), (322, 0.7, 2.2, "step")],
              [(300, 0.7, 2.4, "step"), (318, 0.8, 2.8, "chip")], 91, (0.0, 346.0), (0.1, 345.0), "head_blade", thick=lambda t: 1.9 * (1.0 - 0.5 * t ** 1.5) + 0.1, angle=40.0, wob=0.0, jitter=0.06, res=5.0,
              sec=((-1.0, -0.78, -0.3, 0.0, 0.3, 0.78, 1.0), (0.0, 0.62, 0.95, 1.25, 0.95, 0.62, 0.0)))
    K.tapered("head_post", [(0.0, 248.0, 0.0), (0.0, 272.0, 0.5), (0.0, 290.0, 0.0)], 2.2, 1.8, P["brass"], sides=8, sub=3, part="head_post")
    for i, (x, y, z) in enumerate(((-23.0, 300.0, 3.0), (24.0, 246.0, 2.0))):
        K.gear("float_gear%d" % i, (x, y, z), 3.4 + i * 0.4, 2.6 + i * 0.3, 7, 0.9, P["brass"], hole=0.7, normal=(0, 0, 1), part="head_floatgears", rng_=rr, bevel_w=0.1)
    return K.quad_estimate()


# ================================================================ 9. DRAGON MINUTE HAND: a clock-hand blade held in a dragon's jaws
def dragon_palette():
    P = {}
    P["black"] = mat("dblack", rgb(26, 22, 28), 1.0, 0.3, 901, bump="scales", bump_scale=0.8, bump_strength=0.3, wear_color=rgb(210, 170, 110), mottle_color=rgb(70, 40, 40), dirt_color=(0.003, 0.002, 0.003, 1), scratches=0.8)
    P["gold"] = mat("dgold", rgb(222, 164, 54), 1.0, 0.26, 902, bump="hammer", bump_scale=0.55, bump_strength=0.18, dirt_color=(0.05, 0.025, 0.006, 1), mottle_color=rgb(150, 82, 22), scratches=0.7)
    P["blade"] = mat("dblade", rgb(36, 30, 36), 0.95, 0.2, 903, bump="brushed", bump_strength=0.12, wear=0.4, wear_color=rgb(170, 130, 90), mottle_color=rgb(60, 18, 16), scratches=0.9, veins=None, vein_color=rgb(255, 150, 50),
                     vein_strength=0.0, cramp=("y", 244.0, 346.0, [(0.0, rgb(26, 22, 28)), (0.6, rgb(56, 40, 40)), (1.0, rgb(130, 70, 50))]))
    P["bone"] = mat("dbone", rgb(214, 196, 160), 0.0, 0.5, 904, bump="grain", bump_scale=1.4, bump_strength=0.35, mottle_color=rgb(120, 90, 56), dirt_color=(0.06, 0.04, 0.02, 1), wear_metal=0.0, bevel=0.14)
    P["scale"] = mat("dscale", rgb(150, 28, 24), 0.7, 0.35, 905, bump="scales", bump_scale=0.7, bump_strength=0.35, mottle_color=rgb(60, 8, 8), wear_color=rgb(255, 170, 100), dirt_color=(0.01, 0.002, 0.002, 1))
    P["eye"] = mat("deye", rgb(255, 170, 30), 0.1, 0.1, 906, emit=rgb(255, 190, 60), emit_strength=1.0, emit_flicker=0.5, veins=1.0, vein_color=rgb(255, 250, 200), vein_strength=0.5, bevel=0.06, dirt=0.2)
    P["ember"] = mat("dember", rgb(255, 80, 20), 0.0, 0.3, 907, emit=rgb(255, 110, 30), emit_strength=0.8, emit_flicker=0.8, veins=1.0, vein_color=rgb(255, 230, 150), vein_strength=0.6, bevel=0.0, dirt=0.1)
    P["wrap"] = mat("dwrap", rgb(30, 18, 18), 0.0, 0.6, 908, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(6, 3, 3), groove_amount=0.9, bevel=0.0, fine=0.6, wear_metal=0.0)
    P["cord"] = mat("dcord", rgb(222, 164, 54), 0.2, 0.5, 909, bump="wrap", bump_strength=0.8, groove=rgb(70, 40, 8), groove_amount=0.8, bevel=0.0, wear_metal=0.0)
    return P


def dragon_spear(opts):
    begin(opts["samples"])
    P = dragon_palette()
    rr = K.rng(9909)
    lower_spear(P, P["black"], P["gold"], P["wrap"], P["cord"], y_top=224.0)
    lathe("butt_cap", [(0.01, 0.0), (1.4, 1.4), (3.0, 4.0), (3.6, 10.0), (3.0, 16.0), (2.5, 20.0)], P["gold"], "butt_cap", 10, 40.0)
    for i in range(4):                                                           # the dragon's tail spines at the butt
        a = TAU * i / 4 + 0.6
        crystal("butt_spine%d" % i, (2.6 * math.cos(a), 10.0, 2.6 * math.sin(a)), (1.2 * math.cos(a), -1.0, 1.2 * math.sin(a)), 8.0, 1.0, P["bone"], "butt_spines", sides=4, rr_=rr)
    # scale armour climbing the shaft below the head
    for k in range(10):
        y = 168.0 + 5.6 * k
        for j in range(5):
            a = TAU * j / 5 + 0.6 * k
            K.scale_plate("sc%d_%d" % (k, j), (3.0 * math.cos(a), y, 3.0 * math.sin(a)), (math.cos(a), 0.0, math.sin(a)), 2.0 + 0.1 * (k % 2), P["scale"], thick=0.2, rows=3, part="shaft_scales", tilt=16.0)
    # ---------------------------------------------------------------- the dragon head at the socket: skull, horns swept back, jaws open round the blade foot, fangs, a burning eye
    YH = 236.0
    K.blob("skull", (0.0, YH, -1.0), (7.6, 7.4, 6.4), P["black"], seed=5, lump=0.1, rings=9, segs=12, part="dragon_skull", squash=(1, 1, 1))
    lathe("neck_post", [(2.4, 222.0), (3.6, 226.0), (3.2, 236.0), (2.6, 246.0), (2.6, 256.0)], P["gold"], "dragon_neck", 10, 36.0)
    ring_band("neck_ring0", 246.0, 3.2, 0.9, P["bone"], "dragon_neck", 10)
    for sx in (1, -1):
        K.tapered("horn%d" % sx, [(sx * 4.0, YH + 2.0, -2.0), (sx * 9.0, YH - 3.0, -5.0), (sx * 13.0, YH - 14.0, -8.0), (sx * 12.0, YH - 26.0, -9.0)], 2.1, 0.3, P["bone"], sides=7, sub=3, part="dragon_horns",
                  bulge=lambda t: 1.0 + 0.2 * math.sin(t * 14.0))
        K.tapered("crest%d" % sx, [(sx * 2.5, YH + 4.0, -4.0), (sx * 6.5, YH + 12.0, -9.0), (sx * 8.0, YH + 20.0, -14.0)], 1.5, 0.25, P["gold"], sides=6, sub=3, part="dragon_horns")
        K.blob("eye%d" % sx, (sx * 4.4, YH + 1.6, 3.4), (1.4, 1.1, 1.0), P["eye"], seed=7 + sx, lump=0.03, rings=6, segs=8, part="dragon_eye")
        K.tapered("brow%d" % sx, [(sx * 2.4, YH + 3.8, 4.6), (sx * 5.0, YH + 3.6, 4.2), (sx * 7.0, YH + 2.0, 2.0)], 1.0, 0.4, P["black"], sides=5, sub=2, part="dragon_skull")
    K.tapered("jaw_up", [(0.0, YH + 2.0, 3.0), (0.0, YH + 3.5, 8.0), (0.0, YH + 7.0, 12.5), (0.0, YH + 12.0, 14.5)], 3.6, 1.1, P["black"], sides=8, sub=3, part="dragon_jaw", bulge=lambda t: 1.0 + 0.15 * math.sin(t * 6.0))
    K.tapered("jaw_dn", [(0.0, YH - 2.0, 3.0), (0.0, YH - 6.0, 7.5), (0.0, YH - 8.0, 11.0), (0.0, YH - 7.0, 14.0)], 3.0, 0.9, P["black"], sides=8, sub=3, part="dragon_jaw")
    for k, (y, z, sd) in enumerate(((YH + 4.0, 10.0, 1), (YH + 9.0, 13.0, 2), (YH - 5.5, 9.0, 3), (YH - 7.5, 12.4, 4))):
        for sx in (1, -1):
            crystal("fang%d_%d" % (k, sx), (sx * 1.9, y, z), (sx * 0.15, -1.0 if k < 2 else 1.0, 0.3), 3.0 + 0.5 * (k % 2), 0.5, P["bone"], "dragon_fangs", sides=4, rr_=rr)
    # ---------------------------------------------------------------- the minute-hand blade rising from the jaws: a long, lobed hand-shape, scalloped edges, a pivot ring and a ruby eye
    jag_blade("head_blade", P["blade"], 250.0, 346.0, [(250, 2.4), (258, 4.6), (266, 9.0), (276, 11.4), (288, 8.4), (304, 4.6), (326, 2.6), (346, 0.3)],
              [(270, 0.9, 3.0, "tooth"), (282, 0.8, 2.6, "tooth"), (296, 0.8, 2.4, "tooth"), (314, 0.6, 2.2, "chip")], [(264, 0.8, 2.6, "step"), (276, 0.9, 3.0, "tooth"), (290, 0.8, 2.6, "tooth"), (308, 0.7, 2.2, "chip")], 95,
              (0.0, 346.0), (0.2, 345.0), "head_blade", thick=lambda t: 1.9 * (1.0 - 0.5 * t ** 1.5) + 0.1, angle=30.0, wob=0.0, jitter=0.06, res=4.0,
              sec=((-1.0, -0.78, -0.3, 0.0, 0.3, 0.78, 1.0), (0.0, 0.62, 0.95, 1.25, 0.95, 0.62, 0.0)))
    torus("pivot_ring", (0.0, 270.0, 0.0), 4.4, 1.0, P["gold"], "head_pivot", n=24, sides=6)
    torus("pivot_outer", (0.0, 270.0, 0.0), 7.2, 0.6, P["gold"], "head_pivot", n=28, sides=5, tilt=0.0)
    K.blob("pivot_eye", (0.0, 270.0, 1.9), (2.0, 2.0, 1.6), P["ember"], seed=9, lump=0.05, rings=7, segs=9, part="head_eye")
    for k in range(12):                                                          # twelve hour studs on a broken arc round the pivot
        a = TAU * k / 12 + 0.2
        if 1.2 < a < 2.0 or 4.3 < a < 5.1:
            continue
        K.blob("stud%d" % k, (13.0 * math.cos(a), 270.0 + 13.0 * math.sin(a), 0.0), (0.9, 0.9, 0.9), P["gold"], seed=k, lump=0.03, rings=5, segs=7, part="head_studs")
    # two wing-bone spines sweeping up beside the blade, with a thick curled fin between the fingers
    for sx in (1, -1):
        for k, (L_, o) in enumerate(((46.0, 0.0), (34.0, 5.0), (24.0, 9.0))):
            K.tapered("wing%d_%d" % (sx, k), [(sx * (6.0 + o), 262.0 + 5 * k, -2.0), (sx * (15.0 + o * 1.5), 262.0 + 5 * k + 0.4 * L_, -3.0), (sx * (18.0 + o * 1.2), 262.0 + 5 * k + 0.75 * L_, -3.5),
                                                   (sx * (13.0 + o), 262.0 + 5 * k + L_, -3.0)], 1.5 - 0.2 * k, 0.2, P["gold"] if k % 2 == 0 else P["bone"], sides=6, sub=3, part="head_wings")
    return K.quad_estimate()


# ================================================================ driver
BUILDERS = {}


def preview(mesh_id, trial):
    """Composite preview PNG: full weapon (profile / three-quarter / edge) over a close-up of the head and the socket."""
    from PIL import Image
    pm = os.path.join(K.ROOT, "tools", "relics", "preview_mesh.py")
    mp = os.path.join(trial, "weapon_meshes", mesh_id + ".txt") if trial else mesh_id
    outs = []
    for tag, views, focus in (("full", "profile,three_quarter,edge", None), ("head", "profile,three_quarter", "205,340"), ("socket", "profile,three_quarter", "225,290")):
        out = os.path.join(trial or "/tmp", "pv_%s_%s.png" % (mesh_id, tag))
        cmd = [sys.executable, "-B", pm, mp, "--out", out, "--views", views, "--normals", "vertex"]
        if focus:
            cmd += ["--focus", focus]
        subprocess.run(cmd, check=True, capture_output=True)
        outs.append(Image.open(out).convert("RGB"))
    return outs


def glow_estimate():
    """Quads whose material emits (the bake gives such a quad a glow copy when any emission pixel lies under it): the glow part of the 5000 budget, before the bake."""
    n = 0
    t = {}
    for o in bpy.context.scene.collection.objects:
        if o.type != "MESH" or o.get("detail"):
            continue
        for p in o.data.polygons:
            if p.material_index < len(o.material_slots) and o.material_slots[p.material_index].material and o.material_slots[p.material_index].material.get("glow"):
                k = len(p.vertices)
                c = 1 if k <= 4 else (k - 2 + 1) // 2
                n += c
                t[o.get("part", o.name)] = t.get(o.get("part", o.name), 0) + c
    return n, sorted(t.items(), key=lambda kv: -kv[1])[:8]


def run(only, trial, quick, samples, bake, skin, shape=None, full=False):
    entries = {}
    for key in only:
        fn, mesh_id, form = BUILDERS[key]
        t0 = time.time()
        q = fn({"samples": samples})
        print("   parts:", K.quad_table(14))
        ge, gt = glow_estimate()
        print("   glow-material quads: %d  %s" % (ge, gt))
        print("[%s] built geometry: ~%d body quads + ~%d glow = ~%d in %.1f s" % (mesh_id, q, ge, q + ge, time.time() - t0))
        if shape:
            os.makedirs(shape, exist_ok=True)
            print("shape sheet:", shape_view(os.path.join(shape, key), full=full))
        if quick or shape:
            continue
        rep, md, sd = finish_w(mesh_id, BASE, trial=trial, bake=bake, skin=skin)
        entries[mesh_id] = {"weapon": BASE, "form": form, "class": "spear"}
        if not trial:
            K.write_manifest(MANIFEST, {mesh_id: entries[mesh_id]})


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--trial")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--shape", help="DIR: Workbench shape renders of the geometry (no bake)")
    ap.add_argument("--full", action="store_true", help="shape renders of the whole weapon")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--bake", type=int, default=1024)
    ap.add_argument("--skin", type=int, default=1024)
    a = ap.parse_args(argv)
    run(a.only.split(","), a.trial, a.quick, a.samples, a.bake, a.skin, a.shape, a.full)


BUILDERS["hunter"] = (hunters_spear, "legendary_spear__hunters_spear", "hunters_spear")
BUILDERS["filolial"] = (filolial_spear, "legendary_spear__filolial_spear", "filolial_spear")
BUILDERS["chaos"] = (chaos_spear, "legendary_spear__chaos_spear", "chaos_spear")
BUILDERS["meteor"] = (meteor_spear, "legendary_spear__meteor_spear", "meteor_spear")
BUILDERS["portal"] = (portal_spear, "legendary_spear__portal_spear", "portal_spear")
BUILDERS["triaina"] = (triaina_spear, "legendary_spear__triaina", "triaina")
BUILDERS["bruenack"] = (bruenack_spear, "legendary_spear__bruenack", "bruenack")
BUILDERS["clockwork"] = (clockwork_spear, "legendary_spear__clockwork_spear", "clockwork_spear")
BUILDERS["dragon"] = (dragon_spear, "legendary_spear__dragon_minute_hand", "dragon_minute_hand")

if __name__ == "__main__":
    main(sys.argv[1:])

"""Shared helpers of the vassal-form and redo builders (forms_vassal_scythe.py, forms_vassal_sewing_set.py, forms_vassal_staff.py, forms_vassal_whip.py,
forms_redo.py). Own file: it only IMPORTS forms_kit.py / forms_legendary_sword.py (UV weighting, cupped plume), it edits nothing that exists.

    material factories (metal, leather, wrapmat, bonemat, glassy, glowmat), crescent() (a solid curved scythe blade), rows_along(), contact sheet, driver.
"""
import json
import math
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import bpy  # noqa: E402,F401
import forms_kit as K  # noqa: E402
import forms_legendary_sword as LS  # noqa: E402  (UV island weighting + the cupped plume; importing it sets the 350 KB / 1024 px rules)
from forms_kit import V, rgb, TAU  # noqa: E402

ROOT = K.ROOT
SHEET_DIR = os.path.join(ROOT, "docs", "heroweapon", "form_previews")
plume = LS.plume
weights = LS.weights


def begin(opts):
    return LS.begin(opts if isinstance(opts, dict) else {"samples": opts})


def scale_head(y0, k, ramp=14.0):
    """Grows everything above y0 by the factor k (x, z and the stretch along y), eased in over `ramp` cm so the shaft swells into the head instead of stepping
    (a staff whose head would otherwise read tiny on a long thin shaft). Applies to body and bake-only pieces alike."""
    for o in bpy.context.scene.collection.objects:
        if o.type != "MESH":
            continue
        me = o.data
        for v in me.vertices:
            f = 1.0 + (k - 1.0) * K.smoothstep(y0, y0 + ramp, v.co.y)
            if f != 1.0:
                v.co = V((v.co.x * f, y0 + (v.co.y - y0) * f, v.co.z * f))
        me.update()


def repack(mesh_id, trial=None):
    """If skin + glow are over the PNG budget, packs the skin again with fewer colours and a stronger denoise until it fits."""
    d = os.path.join(trial, "textures", "entity", "weapon") if trial else K.bl.SKIN_DIR
    sk, gl = os.path.join(d, mesh_id + ".png"), os.path.join(d, mesh_id + "_glow.png")
    for colors, dn in ((200, 0.8), (160, 0.9), (128, 1.0), (96, 1.0)):
        kb = (os.path.getsize(sk) + os.path.getsize(gl)) / 1024.0
        if kb <= K.MAX_PNG_KB:
            break
        b, a, ps = K.pack_png(sk, colors=colors, denoise=dn)
        print("[repack] %s %.0f -> %.0f KB with %d colours (psnr %.1f)" % (mesh_id, b, a, colors, ps))


def finish(mesh_id, base, trial=None):
    """K.finish_form with the UV island weighting patched in (a copy of LS.finish with the base id as a parameter)."""
    names = []
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail") and o["part"] not in names:
            names.append(o["part"])
    LS.PART_NAMES[0] = names
    real = K.bl.bpy
    K.bl.bpy = LS._BpyProxy()
    try:
        r = K.finish_form(mesh_id, base, trial=trial, bake=LS.BAKE_PX[0], skin=LS.SKIN_PX[0])
        repack(mesh_id, trial)
        return r
    finally:
        K.bl.bpy = real


# ================================================================ materials
def _lighter(c, k=1.8, a=0.08):
    return tuple(min(1.0, x * k + a) for x in c[:3]) + (1.0,)


def _darker(c, k=0.5):
    return tuple(x * k for x in c[:3]) + (1.0,)


def metal(name, col, rough=0.3, wear=0.9, seed=1, bump="hammer", **kw):
    d = dict(wear=wear, wear_color=_lighter(col), dirt=0.8, dirt_color=(0.02, 0.015, 0.012, 1), scratches=0.7, bump=bump, bump_scale=0.6, bump_strength=0.2, mottle=0.7,
             mottle_color=_darker(col), fine=0.5, seed=seed, bevel=0.16, ao_dist=2.0)
    d.update(kw)
    return K.surface(name, col, 1.0, rough, **d)


def leather(name, col, seed=1, **kw):
    d = dict(bump="leather", bump_scale=1.2, bump_strength=0.6, groove=_darker(col, 0.3), groove_amount=0.8, wear=0.7, wear_color=_lighter(col, 1.5, 0.05), wear_metal=0.0,
             dirt=0.8, mottle=0.8, mottle_color=_darker(col, 0.55), seed=seed, bevel=0.0)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.62, **d)


def wrapmat(name, col, seed=1, **kw):
    d = dict(bump="cross", bump_scale=1.0, bump_strength=0.9, groove=_darker(col, 0.25), groove_amount=0.9, dirt=0.8, dirt_color=(0.01, 0.003, 0.003, 1), mottle=0.8,
             mottle_color=_darker(col, 0.5), wear=0.6, wear_color=_lighter(col, 1.5, 0.05), wear_metal=0.0, fine=0.6, seed=seed, bevel=0.0)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.62, **d)


def bonemat(name, col, seed=1, **kw):
    d = dict(bump="grain", bump_scale=1.5, bump_strength=0.3, mottle=1.0, mottle_color=_darker(col, 0.5), wear=0.7, wear_color=_lighter(col, 1.2, 0.04), wear_metal=0.0,
             dirt=1.0, dirt_color=(0.05, 0.03, 0.02, 1), scratches=0.5, fine=0.7, seed=seed, bevel=0.14, ao_dist=2.5)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.55, **d)


def glowmat(name, col, strength=1.4, flicker=0.6, seed=1, **kw):
    d = dict(emit=col, emit_strength=strength, emit_flicker=flicker, mottle=0.8, mottle_color=_lighter(col, 1.3, 0.2), seed=seed, bevel=0.0)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.3, **d)


def glassy(name, col, emit=None, strength=0.3, seed=1, **kw):
    d = dict(transmission=0.9, ior=1.15, rim=(_lighter(col, 1.4, 0.2), 0.7), mottle=0.6, mottle_color=_darker(col, 0.6), seed=seed, bevel=0.0)
    if emit is not None:
        d.update(emit=emit, emit_strength=strength, emit_flicker=0.5)
    d.update(kw)
    return K.surface(name, col, 0.0, 0.08, **d)


# ================================================================ geometry helpers
def smooth2(pts, sub=5):
    """Catmull-Rom through 2D points -> list of 3D V (z = 0)."""
    return [V((p.x, p.y, 0.0)) for p in K.bl.smooth_path([V((q[0], q[1], 0.0)) for q in pts], sub)]


def normals2(P, side=1):
    """Unit 2D normals of a polyline (rotated +90 deg times side)."""
    out = []
    n = len(P)
    for i in range(n):
        t = (P[min(i + 1, n - 1)] - P[max(i - 1, 0)]).normalized()
        out.append(V((-t.y * side, t.x * side, 0.0)))
    return out


def crescent(name, mat, spine, width, *, thick, side=1, sub=6, z=0.0, vs=(-1.0, -0.82, -0.25, 0.8, 1.0), hs=(0.45, 1.0, 0.92, 0.5, 0.0), part="blade", wobble=None, detail=False):
    """A solid curved blade (a scythe, a tusk, a horn, a claw): `spine` = 2D points of the back; `width(t)` = distance from the back to the cutting edge, toward the
    side chosen by `side` (+1 left of travel, -1 right); `thick(t)` = half thickness. The back is a thick flat, the edge a sharp bevel (hs 0 at the edge)."""
    P = smooth2(spine, sub)
    N = normals2(P, side)
    n = len(P)
    rows = []
    for i, (p, nn) in enumerate(zip(P, N)):
        t = i / max(n - 1, 1)
        w = max(width(t), 0.05)
        rows.append((V((p.x, p.y, z)), V((p.x + nn.x * w, p.y + nn.y * w, z))))
    return K.strip(name, rows, mat, vs, hs, thick, part=part, up=(0, 0, 1), wobble=wobble, detail=detail)


def along(P, t):
    """A point at the share t (0..1) of a polyline's index range."""
    s = t * (len(P) - 1)
    i = min(int(s), len(P) - 2)
    return V(P[i]).lerp(V(P[i + 1]), s - i)


def tangent(P, t):
    s = t * (len(P) - 1)
    i = min(max(int(s), 0), len(P) - 2)
    return (V(P[i + 1]) - V(P[i])).normalized()


def lathe_part(name, prof, mat, sides=10, part=None, squash=1.0, axis="y", caps=(True, True), angle=45.0, origin=(0, 0, 0)):
    ob = K.bl.lathe(name, prof, mat, sides=sides, part=part or name, squash=squash, axis=axis, caps=caps, origin=origin)
    return K._finish_obj(ob, part, True, angle)


def vertebra_haft(prefix, y0, y1, r, mat, collar_mat, *, seg=9.0, sides=8, part="shaft", rr=None):
    """A haft of knuckled segments (bone, bamboo, reed) with a collar ring every few segments. Returns the number of segments."""
    prof = []
    y = y0
    k = 0
    while y < y1 - 0.5:
        h = min(seg, y1 - y)
        rad = r * (1.0 + 0.12 * math.sin(k * 1.3))
        prof += [(rad * 0.8, y), (rad * 1.18, y + h * 0.18), (rad * 0.94, y + h * 0.55), (rad * 1.14, y + h * 0.86), (rad * 0.82, y + h)]
        y += h
        k += 1
    lathe_part(prefix, prof, mat, sides=sides, part=part, angle=50.0)
    return k


def ring_torus(name, center, R, r, mat, *, normal=(0, 0, 1), sides=6, segs=20, part=None, up=(0, 1, 0), squash=1.0):
    """A torus ring (real geometry) of major radius R, minor radius r, facing `normal`, at `center`."""
    prof = [(r * math.cos(TAU * k / sides) + R, r * math.sin(TAU * k / sides) * squash) for k in range(sides)]
    rings = []
    for i in range(segs):
        a = TAU * i / segs
        rings.append([V((px * math.cos(a), px * math.sin(a), pz)) for px, pz in prof])
    ob = K.bl.loft(name, rings, mat, (False, False), part or name)
    K.xf(ob, K.frame(center, normal, up))
    return K._finish_obj(ob, part or name, True, 60.0)


def disc_dome(name, center, r, h, mat, *, normal=(0, 0, 1), sides=16, rings=4, part=None, up=(0, 1, 0), profile=None):
    """A flattened dome / bowl: lathe round z (profile (radius share, height share)), placed facing `normal`."""
    prof = profile or [(1.0, 0.0), (0.95, 0.45), (0.7, 0.8), (0.35, 0.96), (0.001, 1.0)]
    ob = K.bl.lathe(name, [(max(a, 1e-3) * r, b * h) for a, b in prof], mat, sides=sides, axis="z", caps=(False, True), part=part or name)
    K.xf(ob, K.frame(center, normal, up))
    return K._finish_obj(ob, part or name, True, 60.0)


# ================================================================ driver
def run_family(argv, builders, manifest_name, bake_default=2048):
    """builders: {key: (fn, mesh_id, base_id, form_id, class)}. Flags: --only a,b  --trial DIR  --beauty DIR  --quick  --samples N  --threads N  --bake PX  --sheet."""
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(builders))
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--samples", type=int, default=20)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--bake", type=int, default=bake_default)
    ap.add_argument("--sheet", action="store_true", help="only (re)draw the contact sheet from the meshes that exist")
    a = ap.parse_args(argv)
    LS.THREADS[0] = max(1, min(2, a.threads))
    LS.BAKE_PX[0] = a.bake
    manifest = os.path.join(HERE, "forms_manifest_%s.json" % manifest_name)
    if a.sheet:
        print(contact_sheet(manifest_name, builders, a.trial))
        return
    for key in a.only.split(","):
        fn, mesh_id, base, form, cls = builders[key]
        t0 = time.time()
        q = fn({"samples": a.samples})
        print("   parts:", K.quad_table(16))
        print("[%s] built geometry: ~%d body quads in %.1f s" % (mesh_id, q, time.time() - t0))
        if a.beauty:
            os.makedirs(a.beauty, exist_ok=True)
            for view in ("three_quarter", "front"):
                K.beauty(os.path.join(a.beauty, "%s_%s.png" % (key, view)), view=view, samples=max(8, a.samples // 2), size=(560, 900))
        if not a.quick:
            finish(mesh_id, base, trial=a.trial)
            if not a.trial:
                K.write_manifest(manifest, {mesh_id: {"weapon": base, "form": form, "class": cls}})


def contact_sheet(manifest_name, builders, trial=None, cols=2):
    """docs/heroweapon/form_previews/<family>.png: the base weapon first, then each form (profile + three-quarter), drawn by the repo's software preview."""
    sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
    import preview_mesh as pm
    from PIL import Image, ImageDraw
    if trial:
        pm.ASSETS = trial
    tmp = os.path.join(trial or ROOT, "_sheet_tmp")
    os.makedirs(tmp, exist_ok=True)
    ids = []
    for key, (fn, mesh_id, base, form, cls) in builders.items():
        if base not in ids:
            ids.append(base)
        ids.append(mesh_id)
    tiles = []
    for mid in ids:
        mesh_path = os.path.join(trial, "weapon_meshes", mid + ".txt") if trial and os.path.exists(os.path.join(trial, "weapon_meshes", mid + ".txt")) else mid
        if mesh_path == mid:
            pm.ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
        elif trial:
            pm.ASSETS = trial
        out = os.path.join(tmp, mid + ".png")
        try:
            pm.render_preview(mesh_path, None, out, views=("profile", "three_quarter"), normals="vertex", panel=(330, 600))
        except Exception as e:                                       # a missing form is skipped, the sheet shows what exists
            print("[sheet] skipped", mid, e)
            continue
        im = Image.open(out).convert("RGB")
        d = ImageDraw.Draw(im)
        d.rectangle((0, 0, im.width, 14), fill=(30, 34, 46))
        d.text((6, 2), mid, fill=(255, 255, 160))
        tiles.append(im)
        os.remove(out)
    rows = [tiles[i:i + cols] for i in range(0, len(tiles), cols)]
    W = max(sum(t.width for t in r) for r in rows)
    H = sum(max(t.height for t in r) for r in rows)
    sheet = Image.new("RGB", (W, H), (52, 58, 72))
    y = 0
    for r in rows:
        x = 0
        for t in r:
            sheet.paste(t, (x, y))
            x += t.width
        y += max(t.height for t in r)
    os.makedirs(SHEET_DIR, exist_ok=True)
    path = os.path.join(SHEET_DIR, manifest_name + ".png")
    sheet.save(path, optimize=True)
    try:
        os.rmdir(tmp)
    except OSError:
        pass
    return path

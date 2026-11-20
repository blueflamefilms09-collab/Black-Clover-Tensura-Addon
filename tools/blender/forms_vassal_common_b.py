"""Shared driver for the Vassal form builders (axe, book, carriage, chain): the same bake driver as forms_legendary_sword.py (1024 px skin, UV island
weighting, one form per run, 1-2 Cycles threads), parametrised on the base weapon, plus a contact-sheet writer.  Own file: forms_kit.py / bl_lib.py are not edited.

A family script does:
    import forms_vassal_common as C
    C.setup(base="vassal_axe", size_class="battleaxe", manifest=<path>, sheet="vassal_axe")
    ... build functions  fn(opts) -> quad estimate, registered with  C.register("timber", fn, "timber_axe")
    if __name__ == "__main__": C.main(sys.argv[1:])
"""
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import rgb, TAU  # noqa: E402,F401
from mathutils import Vector  # noqa: E402


def V(*a):
    """Vector from V(x, y, z) or V((x, y, z)) (forms_kit's V takes one argument)."""
    return Vector(a[0]) if len(a) == 1 else Vector(a)

HERE = os.path.dirname(os.path.abspath(__file__))
CFG = {"base": None, "class": None, "manifest": None, "sheet": None, "glow": ("FFFFFF", 0.14, 0.45, 1.0)}
THREADS = [2]
K.MAX_PNG_KB = 350
BAKE_PX, SKIN_PX = [2048], [1024]
UV_WEIGHT = {}
PART_NAMES = [None]
BUILDERS = {}


def setup(base, size_class, manifest, sheet, glow=None):
    CFG.update(base=base, manifest=manifest, sheet=sheet)
    CFG["class"] = size_class
    if glow:
        CFG["glow"] = glow


def register(key, fn, form_id):
    BUILDERS[key] = (fn, "%s__%s" % (CFG["base"], form_id), form_id)


def begin(opts):
    sc = K.begin(opts["samples"])
    sc.render.threads_mode = "FIXED"
    sc.render.threads = THREADS[0]
    UV_WEIGHT.clear()
    return sc


def weights(**kw):
    UV_WEIGHT.update(kw)


class _UvProxy:
    def __init__(self, uv):
        self._uv = uv

    def __getattr__(self, k):
        if k == "average_islands_scale":
            return self._avg
        return getattr(self._uv, k)

    def _avg(self, *a, **kw):
        r = self._uv.average_islands_scale(*a, **kw)
        try:
            _weigh_islands()
        except Exception as e:                                      # a failed weighting only costs density, never the build
            print("[uv-weight] skipped:", e)
        return r


class _OpsProxy:
    def __init__(self, ops):
        self._ops = ops

    def __getattr__(self, k):
        if k == "uv":
            return _UvProxy(self._ops.uv)
        return getattr(self._ops, k)


class _BpyProxy:
    def __getattr__(self, k):
        if k == "ops":
            return _OpsProxy(bpy.ops)
        return getattr(bpy, k)


def _weigh_islands():
    import bmesh
    low = bpy.context.edit_object
    bm = bmesh.from_edit_mesh(low.data)
    uvl = bm.loops.layers.uv.active
    pid = bm.faces.layers.int.get("part_id")
    if pid is None or not UV_WEIGHT:
        return
    faces = list(bm.faces)
    parent = {f.index: f.index for f in faces}

    def find(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a
    for e in bm.edges:
        lf = e.link_faces
        if len(lf) != 2:
            continue
        f0, f1 = lf
        uv0 = {}
        for l in f0.loops:
            if l.edge == e:
                uv0[l.vert.index] = tuple(l[uvl].uv)
        ok = True
        for l in f1.loops:
            if l.edge == e:
                u = uv0.get(l.vert.index)
                if u is None or abs(u[0] - l[uvl].uv.x) > 1e-5 or abs(u[1] - l[uvl].uv.y) > 1e-5:
                    ok = False
        if ok:
            parent[find(f0.index)] = find(f1.index)
    groups = {}
    for f in faces:
        groups.setdefault(find(f.index), []).append(f)
    names = PART_NAMES[0]
    for fl in groups.values():
        nm = names[fl[0][pid]] if names and fl[0][pid] < len(names) else None
        w = UV_WEIGHT.get(nm, 1.0)
        if abs(w - 1.0) < 1e-3:
            continue
        us = [l[uvl] for f in fl for l in f.loops]
        cx = sum(u.uv.x for u in us) / len(us)
        cy = sum(u.uv.y for u in us) / len(us)
        for u in us:
            u.uv.x = cx + (u.uv.x - cx) * w
            u.uv.y = cy + (u.uv.y - cy) * w
    bmesh.update_edit_mesh(low.data)


def finish(mesh_id, trial=None):
    names = []
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail") and o["part"] not in names:
            names.append(o["part"])
    PART_NAMES[0] = names
    real = K.bl.bpy
    K.bl.bpy = _BpyProxy()
    try:
        return K.finish_form(mesh_id, CFG["base"], trial=trial, bake=BAKE_PX[0], skin=SKIN_PX[0], glow=CFG["glow"])
    finally:
        K.bl.bpy = real


def contact_sheet(mesh_ids, out_png):
    """Base weapon first, then each form: profile + three-quarter of the mod's software preview (the skins that ship), one row per mesh."""
    sys.path.insert(0, os.path.join(K.ROOT, "tools", "relics"))
    import tempfile
    import preview_mesh as pm
    from PIL import Image, ImageDraw
    rows = []
    tmp = tempfile.mkdtemp(prefix="vsheet_")
    for i, mid in enumerate([CFG["base"]] + list(mesh_ids)):
        p = os.path.join(tmp, "r%d.png" % i)
        try:
            pm.render_preview(mid, None, p, views=("profile", "three_quarter"), normals="vertex", panel=(300, 420), label=False)
            im = Image.open(p).convert("RGB")
            ImageDraw.Draw(im).text((6, 4), mid.split("__")[-1] if "__" in mid else mid + " (base)", fill=(255, 255, 255))
            rows.append(im)
        except Exception as e:
            print("[sheet] skipped", mid, e)
    if not rows:
        return
    cols = 2
    w, h = rows[0].size
    nr = (len(rows) + cols - 1) // cols
    sheet = Image.new("RGB", (w * cols, h * nr), (52, 58, 72))
    for i, im in enumerate(rows):
        sheet.paste(im, ((i % cols) * w, (i // cols) * h))
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    sheet.save(out_png, optimize=True)
    print("[sheet]", out_png)


def run(only, trial, beauty, quick, samples):
    for key in only:
        fn, mesh_id, form = BUILDERS[key]
        t0 = time.time()
        q = fn({"samples": samples})
        print("   parts:", K.quad_table(16))
        print("[%s] built geometry: ~%d body quads in %.1f s" % (mesh_id, q, time.time() - t0))
        if beauty:
            os.makedirs(beauty, exist_ok=True)
            for view in ("three_quarter", "front"):
                K.beauty(os.path.join(beauty, "%s_%s.png" % (key, view)), view=view, samples=max(8, samples // 2), size=(560, 900))
        if not quick:
            finish(mesh_id, trial=trial)
            if not trial:
                K.write_manifest(CFG["manifest"], {mesh_id: {"weapon": CFG["base"], "form": form, "class": CFG["class"]}})


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--sheet", action="store_true", help="only write the contact sheet from the meshes already in the mod")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--bake", type=int, default=2048)
    a = ap.parse_args(argv)
    THREADS[0] = max(1, min(2, a.threads))
    BAKE_PX[0] = a.bake
    if a.sheet:
        contact_sheet([BUILDERS[k][1] for k in BUILDERS], os.path.join(K.ROOT, "docs", "heroweapon", "form_previews", CFG["sheet"] + ".png"))
        return
    run(a.only.split(","), a.trial, a.beauty, a.quick, a.samples)
    if not a.trial and not a.quick and not a.beauty:
        contact_sheet([BUILDERS[k][1] for k in BUILDERS if os.path.exists(os.path.join(K.bl.MESH_DIR, BUILDERS[k][1] + ".txt"))],
                      os.path.join(K.ROOT, "docs", "heroweapon", "form_previews", CFG["sheet"] + ".png"))


# ================================================================ shared shapes
def snake_head(name, base, direction, size, scale_mat, fang_mat, eye_mat, *, up=(0, 0, 1), part="heads", jaw_open=0.35, horn=False, rng_=None):
    """A serpent head: a wedge skull (lofted, widest behind the eyes), a lower jaw that hangs open by `jaw_open`, two real fangs, two glowing eyes. ~120 quads.
    Built pointing along `direction` from `base`; `up` is the head's top. size = head length (cm)."""
    d = V(direction).normalized()
    u = V(up).normalized()
    s = d.cross(u).normalized()
    u = s.cross(d).normalized()
    L = size
    # skull rings along d: (position along 0..1, half width, top height, bottom height)
    prof = [(0.0, 0.17, 0.17, 0.13), (0.2, 0.23, 0.23, 0.14), (0.42, 0.20, 0.18, 0.10), (0.72, 0.13, 0.11, 0.07), (0.93, 0.09, 0.07, 0.05), (1.0, 0.05, 0.04, 0.03)]
    rings = []
    for t, w, ht, hb in prof:
        c = V(base) + d * (L * t)
        ring = []
        for k in range(8):
            a = TAU * k / 8
            ca, sa = math.cos(a), math.sin(a)
            ring.append(c + s * (L * w * ca) + u * (L * (ht if sa > 0 else hb) * sa))
        rings.append(ring)
    K._loft(name + "_skull", rings, scale_mat, (True, True), part, False, True, 60.0)
    # lower jaw: a thinner wedge hanging open
    jr = []
    for t, w, hb in ((0.12, 0.15, 0.06), (0.40, 0.13, 0.05), (0.75, 0.08, 0.035), (0.97, 0.035, 0.02)):
        c = V(base) + d * (L * t) - u * (L * (0.15 + jaw_open * t * 0.8))
        ring = []
        for k in range(6):
            a = TAU * k / 6
            ring.append(c + s * (L * w * math.cos(a)) + u * (L * hb * math.sin(a)))
        jr.append(ring)
    K._loft(name + "_jaw", jr, scale_mat, (True, True), part, False, True, 60.0)
    for sg in (1, -1):
        p0 = V(base) + d * (L * 0.80) + s * (L * 0.05 * sg) - u * (L * 0.06)
        K.tapered("%s_fang%d" % (name, sg), [p0, p0 - u * (L * 0.12) + d * (L * 0.02), p0 - u * (L * 0.24) + d * (L * 0.02)], L * 0.03, 0.03, fang_mat, sides=4, sub=1, part=part)
        pe = V(base) + d * (L * 0.32) + s * (L * 0.17 * sg) + u * (L * 0.10)
        K.blob("%s_eye%d" % (name, sg), pe, (L * 0.07, L * 0.07, L * 0.07), eye_mat, seed=3, lump=0.0, rings=3, segs=6, part=part + "_eyes")
        if horn:
            ph = V(base) + d * (L * 0.15) + s * (L * 0.12 * sg) + u * (L * 0.2)
            K.tapered("%s_horn%d" % (name, sg), [ph, ph - d * (L * 0.18) + s * (L * 0.1 * sg) + u * (L * 0.12), ph - d * (L * 0.42) + s * (L * 0.16 * sg) + u * (L * 0.10)], L * 0.06, 0.04, fang_mat, sides=4, sub=2, part=part)


# ================================================================ shared materials and shapes (own helpers, short names)
def _lighter(c, k=0.35):
    return tuple(min(255, int(v + (255 - v) * k)) for v in c)


def _darker(c, k=0.55):
    return tuple(int(v * k) for v in c)


def mat(name, color, metal=0.0, rough=0.5, **kw):
    """K.surface with sane worn defaults; colour in sRGB 0..255. Pass any K.surface keyword to override."""
    seed = (sum(ord(ch) * (i + 3) for i, ch in enumerate(name)) % 97) + 1
    d = dict(wear=0.85 if metal else 0.5, wear_color=rgb(*_lighter(color)), dirt=0.6, scratches=0.55 if metal else 0.0, mottle=0.6,
             mottle_color=rgb(*_darker(color)), fine=0.3, seed=seed, bevel=0.14, ao_dist=1.6)
    d.update(kw)
    return K.surface(name, rgb(*color), metal, rough, **d)


def glowmat(name, color, strength=0.8, flicker=0.5, **kw):
    seed = (sum(ord(ch) * (i + 5) for i, ch in enumerate(name)) % 89) + 3
    d = dict(emit=rgb(*color), emit_strength=strength, emit_flicker=flicker, mottle=0.5, mottle_color=rgb(*_lighter(color, 0.6)), seed=seed, bevel=0.0)
    d.update(kw)
    return K.surface(name, rgb(*color), 0.0, 0.25, **d)


def ring_x(x, w, h, cy, r=3.0, cz=0.0):
    """A rounded-rect ring in the plane x = const (for solids lying along x): half width w along y, half height h along z, centred at (x, cy, cz)."""
    return [V((x, cy + u, cz + v)) for u, v in K.bl.rrect(w, h, min(r, w * 0.9, h * 0.9))]


def ring_y(y, w, h, r=3.0, cx=0.0, cz=0.0):
    return [V((cx + u, y, cz + v)) for u, v in K.bl.rrect(w, h, min(r, w * 0.9, h * 0.9))]


def ring_z(z, w, h, cx, cy, r=3.0):
    return [V((cx + u, cy + v, z)) for u, v in K.bl.rrect(w, h, min(r, w * 0.9, h * 0.9))]


def loft(name, rings, material, part=None, caps=(True, True), angle=45.0):
    return K._loft(name, rings, material, caps, part or name, False, True, angle)


def fan_rows(inner, outer):
    """Rows (l, r) from an inner polyline to an outer polyline (equal length), as 3D points: the cheek of an axe head, a wing, a blade of any outline."""
    return [(V((a[0], a[1], 0)), V((b[0], b[1], 0))) for a, b in zip(inner, outer)]


def smooth2(pts, sub=3):
    return [(p.x, p.y) for p in K.bl.smooth_path([V((x, y, 0)) for x, y in pts], sub)]


def edge_band(name, rows, f0, material, thick, part, vs=(-1.0, 0.0, 1.0), hs=(0.55, 1.0, 0.0), lift=0.22, up=(0, 0, 1)):
    """A bright band along the outer share (f0..1) of fan rows: the glowing edge of a blade, a lit rim."""
    r2 = [(l + (r - l) * f0, r) for l, r in rows]
    return K.strip(name, r2, material, vs, hs, (lambda t: thick(t) + lift) if callable(thick) else thick + lift, part=part, up=up, angle=60)


def box_x(name, x0, x1, cy, hy, hz, material, part, taper=1.0, r=2.0, cz=0.0):
    """A bevelled bar lying along x between x0 and x1, half sizes hy (y) and hz (z), the far end scaled by `taper`."""
    rings = [ring_x(x0, hy, hz, cy, r, cz), ring_x(x1, hy * taper, hz * taper, cy, r * taper, cz)]
    return loft(name, rings, material, part)


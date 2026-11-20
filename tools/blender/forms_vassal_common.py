"""Shared driver for the vassal form builders (forms_vassal_*.py): the scene/bake helpers of forms_legendary_sword.py copied here (that file is not edited),
with the base weapon id set per family: `import forms_vassal_common as C`; `C.BASE[0] = "vassal_katana"`."""
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

BASE = ["vassal_katana"]
THREADS = [2]
K.MAX_PNG_KB = 350      # owner: 1024 skins, 350 KB for skin + glow
BAKE_PX, SKIN_PX = [2048], [1024]
UV_WEIGHT = {}          # part name -> linear texel-density factor


# ================================================================ driver helpers
def begin(opts):
    sc = K.begin(opts["samples"])
    sc.render.threads_mode = "FIXED"
    sc.render.threads = THREADS[0]
    UV_WEIGHT.clear()
    return sc


def weights(**kw):
    UV_WEIGHT.update(kw)


class _UvProxy:
    """Wraps bpy.ops.uv so that, right after bl_lib's `average_islands_scale`, every UV island is scaled by the weight of its part (blade bigger, embers smaller):
    the same 512 px skin, spent where the eye looks. pack_islands then fits everything back into the tile."""

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
    ob = bpy.context.edit_object
    names = None
    low = ob
    bm = bmesh.from_edit_mesh(low.data)
    uvl = bm.loops.layers.uv.active
    pid = bm.faces.layers.int.get("part_id")
    if pid is None or not UV_WEIGHT:
        return
    names = list(low.get("part_names") or [])
    # island = faces joined by a shared edge whose UVs coincide
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
    part_names = PART_NAMES[0]
    for fl in groups.values():
        nm = part_names[fl[0][pid]] if part_names and fl[0][pid] < len(part_names) else None
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


PART_NAMES = [None]


def finish(mesh_id, trial=None):
    """K.finish_form with the island weighting patched in (bl_lib sees the proxy as its `bpy`)."""
    names = []
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail") and o["part"] not in names:
            names.append(o["part"])
    PART_NAMES[0] = names
    real = K.bl.bpy
    K.bl.bpy = _BpyProxy()
    try:
        return K.finish_form(mesh_id, BASE[0], trial=trial, bake=BAKE_PX[0], skin=SKIN_PX[0])
    finally:
        K.bl.bpy = real


def _smart_shrink(folder, only=None):
    """K.shrink_pngs replacement: packs the skin and the glow skin with the kit's pack_png, and goes down in colours / up in denoise until skin + glow fit K.MAX_PNG_KB."""
    names = [only + ".png", only + "_glow.png"] if only else [f for f in sorted(os.listdir(folder)) if f.endswith(".png") and "preview" not in f]
    raw = {n: open(os.path.join(folder, n), "rb").read() for n in names}
    for colors, den in ((256, 0.6), (192, 0.8), (160, 0.9), (128, 1.0), (96, 1.0), (64, 1.0), (48, 1.0), (40, 1.0), (32, 1.0)):
        total = 0.0
        for n in names:
            path = os.path.join(folder, n)
            with open(path, "wb") as fh:
                fh.write(raw[n])
            b, a_, ps = K.pack_png(path, colors, den)
            total += a_
        print("[pack] %s colors=%d denoise=%.1f -> %.0f KB" % (only, colors, den, total))
        if total <= K.MAX_PNG_KB:
            break


K.shrink_pngs = _smart_shrink

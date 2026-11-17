"""Weapon-form models of the LEGENDARY SWORD, built with tools/blender/forms_kit.py (same family as the phoenix sword exemplar).

    python3 -B tools/blender/forms_legendary_sword.py --only flash [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids (docs/heroweapon/form_models.md): legendary_sword__<form>. Forms here: flash_sword, wind_king_katana, meteor_sword, assassin_sword,
spirit_tortoise_sword, tortoise_carapace_sword, hundred_sword, thunder_god_sword (phoenix_sword is the exemplar in forms_exemplars.py; the four curse forms
keep the existing legendary_sword_curse look). Every form keeps the base sword's `place` (same hand, same size class: longsword).
Seeded: the same seed gives the same weapon. One form per run: the bake uses 1 or 2 Cycles threads (the machine is shared).
"""
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

MANIFEST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "forms_manifest_legendary_sword.json")
BASE = "legendary_sword"
THREADS = [2]
K.MAX_PNG_KB = 350      # owner: 1024 skins, 350 KB for skin + glow
BAKE_PX, SKIN_PX = [2048], [1024]
UV_WEIGHT = {}          # part name -> linear texel-density factor (the blade gets more skin than the floating bits)


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
        return K.finish_form(mesh_id, BASE, trial=trial, bake=BAKE_PX[0], skin=SKIN_PX[0])
    finally:
        K.bl.bpy = real


def ray_rows(c, d, length, width, n=6, curve=0.0, tipw=0.0):
    """Rows (left, right) of a flat lancet pointing from c along d (xy plane): widest near 30 %, a needle tip; optional sideways curve (cm at the tip)."""
    d = V(d).normalized()
    s = V((-d.y, d.x, 0))
    rows = []
    for i in range(n + 1):
        t = i / n
        w = width * 0.5 * (math.sin(math.pi * min(1.0, t ** 0.6)) ** 0.8 if t < 1 else 0.0)
        w = max(w, tipw if t >= 1 else w)
        p = V(c) + d * (length * t) + s * (curve * t * t)
        rows.append((p - s * w, p + s * w))
    return rows


def ray(name, c, d, length, width, mat, thick=0.35, part="rays", curve=0.0, z=0.0, n=6):
    rows = [(l + V((0, 0, z)), r + V((0, 0, z))) for l, r in ray_rows(c, d, length, width, n, curve)]
    return K.strip(name, rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=(0, 0, 1), angle=50)


def plume(name, base, direction, length, width, material, *, shaft_material=None, bend=(0, 0, 0), curl=0.0, rows=8, serrate=0.2, facing=(0, 0, 1), thick=0.3, part=None, seed=0, profile=None, droop=0.0,
          cup=0.22, **_ignored):
    """A feather / flame tongue with a real body: a CUPPED vane (the edges lifted toward `facing`, a lens section thick in the middle), serrated barbs, and a round tapering quill along the spine of it.
    Same call as K.feather; this one is not a flat card. ~rows * 8 + 30 quads."""
    L, R, C = K.leaf_rows(base, direction, length, width, bend=bend, curl=curl, rows=rows, serrate=serrate, facing=facing, seed=seed, profile=profile, droop=droop)
    fz = V(facing).normalized()
    wmax = max((r - l).length for l, r in zip(L, R)) or 1.0
    rws = []
    for l, r in zip(L, R):
        w = (r - l).length / wmax
        rws.append((l + fz * (cup * width * w * w * 0.5), r + fz * (cup * width * w * w * 0.5)))
    K.strip(name, rws, material, (-1.0, -0.5, 0.0, 0.5, 1.0), (0.0, 0.75, 1.0, 0.75, 0.0), lambda t: thick * (1.0 - 0.55 * t), part=part or name, up=facing, angle=70)
    q = [c + fz * (thick * 0.9) for c in C]
    K.tapered(name + "_quill", q[:-1], width * 0.07 + 0.12, 0.05, shaft_material or material, sides=5, sub=1, part=part or name)


# ================================================================ 1. FLASH SWORD  (glowstone + quartz: a blade that catches the light)
def flash_palette():
    P = {}
    P["steel"] = K.surface("flash_steel", rgb(92, 106, 128), 1.0, 0.26, wear=1.0, wear_color=rgb(255, 255, 255), dirt=0.45, dirt_color=(0.03, 0.04, 0.06, 1), scratches=0.9,
                           bump="brushed", bump_strength=0.1, mottle=0.5, mottle_color=rgb(150, 190, 235), fine=0.3, seed=31, bevel=0.2, ao_dist=1.6,
                           cramp=("y", 32.0, 150.0, [(0.0, rgb(78, 88, 108)), (0.5, rgb(124, 140, 166)), (1.0, rgb(90, 150, 220))]), wear_radius=0.5)
    P["gold"] = K.surface("flash_gold", rgb(214, 156, 52), 1.0, 0.28, wear=0.9, wear_color=rgb(255, 245, 205), dirt=0.7, dirt_color=(0.05, 0.03, 0.006, 1), scratches=0.6,
                          bump="hammer", bump_scale=0.7, bump_strength=0.16, mottle=0.7, mottle_color=rgb(160, 96, 26), fine=0.4, seed=32, bevel=0.16, ao_dist=1.8)
    P["gold_dark"] = K.surface("flash_gold_dark", rgb(170, 118, 44), 1.0, 0.38, wear=0.9, wear_color=rgb(245, 200, 100), dirt=0.9, dirt_color=(0.03, 0.015, 0.004, 1), scratches=0.5,
                               bump="brushed", bump_strength=0.12, mottle=0.7, mottle_color=rgb(90, 52, 16), seed=33, bevel=0.14)
    P["quartz"] = K.surface("quartz", rgb(120, 190, 235), 0.0, 0.05, emit=rgb(120, 200, 255), emit_strength=0.4, emit_flicker=0.5, mottle=0.9, mottle_color=rgb(255, 255, 255),
                            seed=34, bevel=0.0, rim=(rgb(255, 255, 255), 0.6))
    P["gem"] = K.surface("glowstone", rgb(255, 210, 90), 0.0, 0.1, emit=rgb(255, 196, 70), emit_strength=1.1, emit_flicker=0.5, mottle=0.9, mottle_color=rgb(255, 245, 190), seed=35, bevel=0.0)
    P["beam"] = K.surface("beam", rgb(255, 240, 190), 0.0, 0.4, emit=rgb(255, 238, 180), emit_strength=1.0, emit_flicker=0.9, seed=36, bevel=0.0,
                          emit_ramp=("y", 32.0, 150.0, [(0.0, rgb(255, 200, 80)), (0.55, rgb(255, 245, 205)), (1.0, rgb(190, 235, 255))]))
    P["wrap"] = K.surface("flash_wrap", rgb(226, 220, 204), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(96, 84, 64), groove_amount=0.8, dirt=0.7,
                          dirt_color=(0.03, 0.025, 0.018, 1), mottle=0.8, mottle_color=rgb(170, 150, 110), wear=0.6, wear_color=rgb(255, 250, 235), wear_metal=0.0, fine=0.6, seed=37, bevel=0.0)
    P["core"] = K.surface("flash_core", rgb(110, 96, 70), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8, seed=38, bevel=0.0)
    P["cord"] = K.surface("flash_cord", rgb(235, 180, 60), 0.5, 0.45, bump="wrap", bump_strength=0.7, groove=rgb(80, 46, 10), groove_amount=0.8, wear=0.7, dirt=0.6, seed=39, bevel=0.0, fine=0.5)
    P["cloth"] = K.surface("flash_cloth", rgb(235, 238, 245), 0.0, 0.7, bump="cross", bump_scale=1.4, bump_strength=0.4, dirt=0.5, mottle=0.7, mottle_color=rgb(150, 170, 210), wear=0.5,
                           wear_color=rgb(255, 255, 255), wear_metal=0.0, seed=40, bevel=0.0)
    return P


def flash_sword(opts):
    begin(opts)
    weights(blade=1.3, blade_glow=0.8, guard=1.1, rays=1.1, halo=1.0, crystals=0.9, glints=0.6)
    P = flash_palette()
    rr = K.rng(5101)
    ymid = 31.0
    # ---------------------------------------------------------------- blade: a slim lancet, the right edge broken into rising steps (a flash), the left clean; both with a fine bevel
    def hw(y):
        return K.interp([(32, 5.4), (38, 6.2), (62, 6.0), (96, 5.5), (122, 4.6), (138, 3.2), (146, 1.7), (150, 0.4)], y)

    stL = [(y, hw(y)) for y in (32, 38, 62, 96, 122, 138, 146, 150)]
    notchR = [(52, 0.7, 2.4, "chip"), (74, 0.9, 5.0, "step"), (96, 0.8, 4.0, "step"), (116, 0.8, 4.0, "step"), (133, 0.7, 3.2, "step")]
    notchL = [(60, 0.6, 2.0, "chip"), (88, 0.6, 2.2, "chip"), (124, 0.7, 2.4, "v")]
    L = K.blade_edge(-1, stL, 32, 150, notches=notchL, jitter=0.05, res=12.0, seed=41, tip=(-0.3, 151.0))
    R = K.blade_edge(1, stL, 32, 150, notches=notchR, jitter=0.05, res=12.0, seed=42, tip=(0.5, 149.5))
    th = lambda t: 1.15 * (1.0 - 0.5 * t ** 2.0)                                                  # noqa: E731
    K.blade("blade", P["steel"], L, R, thick=th, fuller=0.2, fuller_depth=0.55, fuller_off=0.0, seed=7, wob=0.04, bevel_w=0.3)
    # the bright inlay lying in the fuller: REAL geometry (a raised lozenge bar), both faces, glows
    for s_ in (1, -1):
        rowsI = []
        for y in range(40, 146, 5):
            c = V((0.15 * math.sin(y * 0.09), y, 0.0))
            w = 0.7 * (1.0 - 0.55 * ((y - 40) / 106.0))
            rowsI.append((c - V((w, 0, 0)), c + V((w, 0, 0))))
        K.strip("inlay%d" % s_, rowsI, P["beam"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: 0.86 * (1 - 0.25 * t), part="blade_glow", up=(0, 0, s_))
    for s_ in (1, -1):
        # a ricasso cap: stepped gold plate with real rivets, over the blade root
        cap = [(-4.6, 32.2), (4.6, 32.2), (5.3, 38.0), (3.4, 46.0), (0.0, 48.6), (-3.4, 46.0), (-5.3, 38.0)]
        K.stepped_plate("ricasso%d" % s_, cap, 1.0 * s_, P["gold"], steps=2, step_h=0.5, inset=0.8, part="guard_cap", up=s_)
        K.rivets("ricasso_rv%d" % s_, [(V((x, y, 1.75 * s_)), (0, 0, s_)) for x, y in ((-3.4, 36.0), (3.4, 36.0), (-2.2, 42.5), (2.2, 42.5))], P["gold_dark"], r=0.7, rng_=rr, jitter=0.15, detail=False)
    # ---------------------------------------------------------------- quartz clusters growing from the spine and edges: the light is caught and thrown back
    spots = [(-1, 48, 5.0, 4.6), (1, 58, 4.4, 6.0), (-1, 70, 6.2, 5.2), (1, 82, 4.8, 4.2), (-1, 92, 5.4, 6.4), (1, 104, 4.4, 5.0), (-1, 114, 4.8, 4.0), (1, 124, 3.8, 4.4), (-1, 132, 3.2, 3.4)]
    for i, (sx, y, sz, ln) in enumerate(spots):
        for k in range(1 + (i % 2)):
            ang = math.radians(rr.uniform(30, 75))
            d = V((sx * math.sin(ang), math.cos(ang) * 0.85, rr.uniform(-0.4, 0.5) + 0.0))
            base = V((sx * (hw(y) - 0.4 + 0.5 * k), y - 1.0 * k, rr.uniform(-0.5, 0.5)))
            K.shard("quartz%d_%d" % (i, k), base + d * (ln * 0.4), sz * 0.33 * (1.0 - 0.2 * k), P["quartz"], rr, part="crystals", sides=6, stretch=ln / sz * 1.1, dir_=d)
    # ---------------------------------------------------------------- the halo: a thin gold ring behind the guard, ticks on it, and sun rays that carry the flash sideways
    hr = 17.5
    cy = ymid + 1.0
    ring = K.bl.lathe("halo_ring", [(hr - 0.9, -0.5), (hr - 0.4, -0.95), (hr + 0.4, -0.95), (hr + 0.9, -0.5), (hr + 0.4, 0.0), (hr - 0.4, 0.0), (hr - 0.9, -0.5)], P["gold_dark"], sides=44, axis="z", caps=(False, False), part="halo")
    K._finish_obj(ring, "halo", True, 40.0)
    K.bl.move(ring, (0, cy, -1.9))
    for k in range(18):
        a = TAU * k / 18 + rr.uniform(-0.02, 0.02)
        if abs(math.sin(a)) > 0.94 and math.sin(a) > 0:                                               # leave the way for the blade
            continue
        ln = (3.4 if k % 4 == 0 else 1.9) * rr.uniform(0.85, 1.2)
        K.tapered("tick%d" % k, [V(((hr + 0.2) * math.cos(a), cy + (hr + 0.2) * math.sin(a), -1.9)), V(((hr + ln * 0.5) * math.cos(a), cy + (hr + ln * 0.5) * math.sin(a), -1.9)),
                                 V(((hr + ln) * math.cos(a), cy + (hr + ln) * math.sin(a), -1.9))], 0.5, 0.06, P["gold"], sides=4, sub=1, part="halo")
    for k in range(5):                                                                              # spokes inside the halo, between rays
        a = TAU * (k + 0.5) / 5 + 0.1
        K.bar("spoke%d" % k, (5.0 * math.cos(a), cy + 5.0 * math.sin(a), -1.7), ((hr - 3.4) * math.cos(a), cy + (hr - 3.4) * math.sin(a), -1.7), 0.35, 0.3, P["gold_dark"], part="halo", taper=0.7)
    # long swept rays: the crossguard (left and right are not twins)
    for sx, base_ang, ln, wd in ((1, 4, 31.0, 4.4), (-1, 176, 27.0, 4.0)):
        a = math.radians(base_ang)
        ray("xray%d" % sx, V((sx * 3.5, ymid - 0.4, 0.2)), V((math.cos(a), math.sin(a) * 0.35 + 0.1, 0)), ln, wd, P["gold"], thick=0.9, part="guard", curve=sx * 0.0 + 2.2)
    for k, (ang, ln, wd) in enumerate(((28, 17.0, 2.6), (48, 11.0, 2.0), (140, 15.0, 2.6), (120, 9.5, 1.8), (-30, 15.0, 2.6), (-150, 13.0, 2.4), (-52, 9.0, 1.9), (-128, 10.5, 1.9), (68, 8.0, 1.6), (112, 6.5, 1.4))):
        a = math.radians(ang)
        ray("ray%d" % k, V((5.0 * math.cos(a), ymid + 5.0 * math.sin(a), 0.6 + 0.2 * (k % 2))), V((math.cos(a), math.sin(a), 0)), ln, wd, P["gold"] if k % 2 else P["steel"], thick=0.6, part="rays", curve=rr.uniform(-1.5, 1.5))
    # the guard hub: a stepped gold disc with a glowstone in claws, rivets and a collar over the blade root
    disc = [(6.5 * math.cos(TAU * i / 14 + 0.1) * (1 + 0.04 * (i % 2)), ymid + 6.5 * math.sin(TAU * i / 14 + 0.1) * (1 + 0.04 * (i % 2))) for i in range(14)]
    for s in (1, -1):
        K.stepped_plate("hub%d" % s, disc, 0.5 * s, P["gold"], steps=2, step_h=0.9, inset=1.1, part="guard", up=s)
        K.gem_setting("glow%d" % s, (0, ymid, 3.2 * s), 3.0, P["gem"], P["gold"], normal=(0, 0, s), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
        K.rivets("hub_rivets%d" % s, [(V((5.2 * math.cos(a), ymid + 5.2 * math.sin(a), (0.5 + 0.7 * 1) * s - 0.1 * s)), (0, 0, s)) for a in [TAU * k / 7 + 0.2 for k in range(7)]], P["gold_dark"], r=0.55, rng_=rr, detail=False)
    collar = K.bl.lathe("collar", [(1.0, ymid - 3.5), (3.4, ymid - 3.0), (4.8, ymid - 1.5), (4.8, ymid + 2.8), (3.6, ymid + 4.2), (1.0, ymid + 4.6)], P["gold_dark"], sides=10, part="guard", squash=0.4)
    K._finish_obj(collar, "guard", True, 45.0)
    # ---------------------------------------------------------------- grip: ivory wraps, gold cord and bands, a prism pommel in a claw cage, two long white tails
    K.grip_wrapped("grip", 8.5, 26.5, 2.0, P["core"], P["wrap"], turns=8.0, cord=0.38, bands=(9.4, 17.4, 25.4), band_mat=P["gold"], swell=0.16, part="grip", seed=4)
    pom = K.bl.lathe("pommel_seat", [(0.01, 0.4), (2.2, 0.5), (3.4, 1.5), (3.6, 2.6), (2.4, 3.4), (2.2, 4.0)], P["gold_dark"], sides=12, part="pommel")
    K._finish_obj(pom, "pommel")
    pr = K.bl.lathe("prism", [(0.01, -4.6), (1.9, -3.0), (2.5, -0.5), (2.5, 3.0), (1.6, 5.4), (0.01, 7.6)], P["quartz"], sides=6, part="crystals", origin=(0, 2.0, 0))
    K._finish_obj(pr, "crystals", True, 20.0)
    for k in range(6):
        a = TAU * k / 6 + 0.25
        K.tapered("pcage%d" % k, [V((2.8 * math.cos(a), 1.2, 2.8 * math.sin(a))), V((3.7 * math.cos(a), 4.0, 3.7 * math.sin(a))), V((1.6 * math.cos(a), 8.6, 1.6 * math.sin(a)))], 0.5, 0.2, P["gold"], sides=5, part="pommel")
    for i, (ang, ln) in enumerate(((-30, 21), (14, 26), (40, 17))):
        a = math.radians(ang)
        path = [V((2.6 * math.sin(a) * (1 + 0.3 * k) + 0.3 * k * math.sin(a), 1.5 - k * ln / 9.0, 2.2 * math.cos(a) * 0.5 + 0.7 * math.sin(k * 0.9 + i))) for k in range(10)]
        K.ribbon("tail%d" % i, path, lambda t: 1.9 * (1.0 - 0.45 * t) + 0.25, P["cloth"], facing=(math.cos(a), 0, -math.sin(a)), thick=0.06, part="pommel_tails", fray=0.7, rng_=rr, edge_wave=0.1, notch=1.1 if i == 1 else 0.0)
    # ---------------------------------------------------------------- floating quartz splinters round the blade (bounded: 9)
    K.shard_cloud("float", (0, 92, 0), (17.0, 58.0, 7.0), 6, (0.9, 1.5), P["quartz"], rr, avoid=lambda p: abs(p.x) < hw(min(max(p.y, 32), 150)) + 7.0 or p.y < 40, part="glints", stretch=2.2, sides=5, max_count=6,
                  flow=lambda p: V((0.15, 1.0, rr.uniform(-0.2, 0.2))))
    return K.quad_estimate()


# ================================================================ 2. HEAVENLY WIND KING KATANA  (feathers + string: a curved single-edged blade, a feather crown, wind that spirals up it)
def wind_palette():
    P = {}
    P["steel"] = K.surface("wind_steel", rgb(150, 168, 176), 1.0, 0.2, wear=1.0, wear_color=rgb(235, 250, 250), dirt=0.55, dirt_color=(0.02, 0.035, 0.04, 1), scratches=0.9, bump="brushed",
                           bump_strength=0.1, mottle=0.5, mottle_color=rgb(70, 110, 120), fine=0.3, seed=51, bevel=0.2, ao_dist=1.6,
                           cramp=("y", 32.0, 150.0, [(0.0, rgb(112, 128, 134)), (0.55, rgb(168, 188, 196)), (1.0, rgb(150, 215, 215))]), wear_radius=0.5)
    P["bronze"] = K.surface("wind_bronze", rgb(54, 104, 98), 1.0, 0.34, wear=0.95, wear_color=rgb(210, 235, 200), dirt=0.85, dirt_color=(0.008, 0.02, 0.018, 1), scratches=0.7, bump="hammer",
                            bump_scale=0.7, bump_strength=0.2, mottle=0.9, mottle_color=rgb(28, 70, 66), fine=0.5, seed=52, bevel=0.18, ao_dist=2.0)
    P["gold"] = K.surface("wind_gold", rgb(220, 170, 64), 1.0, 0.28, wear=0.9, wear_color=rgb(255, 240, 190), dirt=0.7, dirt_color=(0.05, 0.03, 0.006, 1), scratches=0.6, bump="hammer",
                          bump_scale=0.7, bump_strength=0.16, mottle=0.6, mottle_color=rgb(150, 96, 30), seed=53, bevel=0.16, ao_dist=1.8)
    P["feather"] = K.surface("wind_feather", rgb(220, 240, 236), 0.0, 0.55, wear=0.5, wear_color=rgb(255, 255, 250), wear_metal=0.0, dirt=0.35, dirt_color=(0.03, 0.05, 0.05, 1), feather=(14, 5, 0.7, 0.8),
                             tramp=[(0.0, rgb(100, 160, 160)), (0.45, rgb(232, 246, 242)), (1.0, rgb(120, 214, 204))], mottle=0.6, mottle_color=rgb(150, 200, 196), seed=54, bevel=0.06, fine=0.4)
    P["feather2"] = K.surface("wind_feather2", rgb(80, 170, 160), 0.0, 0.5, wear=0.5, wear_color=rgb(220, 255, 245), wear_metal=0.0, dirt=0.4, feather=(14, 5, 0.7, 0.8),
                              tramp=[(0.0, rgb(30, 90, 90)), (0.5, rgb(70, 170, 156)), (1.0, rgb(200, 250, 236))], mottle=0.6, mottle_color=rgb(30, 110, 110), seed=55, bevel=0.06, fine=0.4)
    P["wind"] = K.surface("wind_glow", rgb(150, 235, 220), 0.0, 0.3, emit=rgb(120, 240, 215), emit_strength=0.8, emit_flicker=0.7, seed=56, bevel=0.0,
                          emit_ramp=("y", 20.0, 150.0, [(0.0, rgb(80, 200, 180)), (0.6, rgb(150, 245, 225)), (1.0, rgb(220, 255, 250))]))
    P["ito"] = K.surface("wind_ito", rgb(236, 236, 226), 0.0, 0.6, bump="wrap", bump_strength=0.8, groove=rgb(110, 120, 110), groove_amount=0.8, dirt=0.7, dirt_color=(0.03, 0.03, 0.025, 1), mottle=0.8,
                         mottle_color=rgb(150, 190, 180), wear=0.6, wear_color=rgb(255, 255, 250), wear_metal=0.0, fine=0.5, seed=57, bevel=0.0)
    P["ito2"] = K.surface("wind_ito2", rgb(60, 140, 130), 0.0, 0.6, bump="wrap", bump_strength=0.8, groove=rgb(14, 50, 46), groove_amount=0.8, dirt=0.7, mottle=0.8, mottle_color=rgb(30, 90, 84), wear=0.6,
                          wear_color=rgb(170, 230, 215), wear_metal=0.0, fine=0.5, seed=58, bevel=0.0)
    P["same"] = K.surface("wind_same", rgb(210, 200, 176), 0.0, 0.6, bump="scales", bump_scale=1.2, bump_strength=0.9, groove=rgb(70, 62, 46), groove_amount=0.8, dirt=0.8, mottle=0.8, mottle_color=rgb(150, 140, 110), seed=59, bevel=0.0)
    P["cord"] = K.surface("wind_cord", rgb(40, 130, 120), 0.0, 0.6, bump="wrap", bump_strength=0.7, groove=rgb(10, 46, 44), groove_amount=0.8, dirt=0.6, mottle=0.7, mottle_color=rgb(20, 80, 74), seed=60, bevel=0.0, fine=0.4)
    P["gem"] = K.surface("wind_jade", rgb(70, 220, 190), 0.0, 0.08, emit=rgb(90, 240, 205), emit_strength=0.9, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(200, 255, 235), seed=61, bevel=0.0)
    return P


def wind_king_katana(opts):
    begin(opts)
    weights(blade=1.35, wind=0.7, plumage=1.1, tsuba=1.15, floats=0.6)
    P = wind_palette()
    rr = K.rng(5202)
    ymid = 31.0
    SORI = 5.2

    def xc(y):                                                    # the curve of the blade: the edge side (+x) bows out, the tip comes back
        u = (y - 32.0) / 118.0
        return SORI * 4.0 * u * (1.0 - u) * (1.0 - 0.25 * u)

    def hw(y):
        return K.interp([(32, 3.9), (40, 4.1), (90, 3.7), (125, 3.3), (134, 3.5), (141, 2.4), (146, 1.2), (150.5, 0.3)], y)
    # ---------------------------------------------------------------- blade: single edge, shinogi ridge, a groove along the spine, a swept kissaki (tip)
    stL = [(y, hw(y)) for y in (32, 40, 90, 125, 134, 141, 146, 150.5)]
    L = K.blade_edge(-1, stL, 32, 150.5, notches=[(61, 0.35, 1.8, "chip"), (101, 0.3, 1.6, "chip")], jitter=0.04, res=8.0, seed=63, tip=(0.2, 151.0))
    R = K.blade_edge(1, stL, 32, 150.5, notches=[(76, 0.4, 2.0, "chip"), (118, 0.35, 1.8, "chip")], jitter=0.04, res=8.0, seed=64, tip=(0.9, 150.0))
    L = [V((p.x + xc(p.y), p.y, 0)) for p in L]
    R = [V((p.x + xc(p.y), p.y, 0)) for p in R]
    sec = ((-1.0, -0.9, -0.62, -0.5, -0.24, -0.12, 0.66, 1.0), (0.55, 0.9, 1.0, 0.55, 0.55, 1.0, 0.55, 0.0))
    K.blade("blade", P["steel"], L, R, thick=lambda t: 1.15 * (1.0 - 0.45 * t ** 2.0), section=sec, seed=9, wob=0.03)
    # a glowing line of wind lying in the spine groove: real geometry
    for s_ in (1, -1):
        rowsI = []
        for y in range(40, 140, 5):
            c = V((xc(y) - hw(y) * 0.37, y, 0.0))
            w = 0.34 * (1.0 - 0.6 * ((y - 40) / 100.0))
            rowsI.append((c - V((w, 0, 0)), c + V((w, 0, 0))))
        K.strip("windline%d" % s_, rowsI, P["wind"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: 0.45 * (1 - 0.3 * t), part="wind_line", up=(0, 0, s_))
    # habaki (blade collar): gold, stepped, with a cut line, both faces
    for s_ in (1, -1):
        hab = [(-4.4, 31.6), (4.6, 31.6), (4.8, 40.6), (3.4, 43.6), (-3.2, 43.6), (-4.6, 40.6)]
        K.stepped_plate("habaki%d" % s_, hab, 1.0 * s_, P["gold"], steps=2, step_h=0.6, inset=0.7, part="habaki", up=s_, bevel_w=0.0)
    # ---------------------------------------------------------------- the feather crown on the guard: a lobed bronze tsuba, three steps, a ring of 14 real feathers radiating on both faces
    lobes = []
    for i in range(24):
        a = TAU * i / 24
        r = 10.0 + 1.4 * math.cos(a * 8.0 + 0.3) * (1.0 if i % 2 == 0 else 0.9)
        lobes.append((r * math.cos(a), ymid + r * math.sin(a)))
    for s_ in (1, -1):
        K.stepped_plate("tsuba%d" % s_, lobes, 0.4 * s_, P["bronze"], steps=2, step_h=0.8, inset=1.6, part="tsuba", bevel_w=0.0, up=s_)
        K.gem_setting("jade%d" % s_, (0, ymid + 0.0, 2.7 * s_), 2.6, P["gem"], P["gold"], normal=(0, 0, s_), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.5)
        K.rivets("tsuba_rv%d" % s_, [(V((6.0 * math.cos(a), ymid + 6.0 * math.sin(a), 1.65 * s_ + 0.0)), (0, 0, s_)) for a in [TAU * k / 5 + 0.4 for k in range(5)]], P["gold"], r=0.6, rng_=rr, detail=False)
    for side in (1, -1):
        rs = K.rng(80 + side)
        for k in range(5):
            ang = 90 + side * (34 + k * 32.0) if k < 4 else 270 + side * 4
            ang += rs.uniform(-4, 4)
            a = math.radians(ang)
            d = V((math.cos(a), math.sin(a), 0))
            ln = rs.uniform(9.0, 14.0) * (0.75 if 235 < ang % 360 < 305 else 1.0)
            tilt = rs.uniform(0.15, 0.45)
            plume("crown%d_%d" % (side, k), V((7.2 * math.cos(a), ymid + 7.2 * math.sin(a), side * (1.6 + 0.25 * (k % 3)))), V((d.x, d.y, side * tilt)), ln, rs.uniform(2.6, 3.6), P["feather"] if k % 2 else P["feather2"],
                      facing=(0, 0, side), curl=side * rs.uniform(1.4, 3.0), serrate=0.2, rows=4, thick=0.24, seed=300 + k + 20 * side, part="plumage", droop=0.5 * side)
    # ---------------------------------------------------------------- feathers laid along the spine, sweeping back like a wing; each with its own tilt and curl
    for k in range(6):
        y0 = 46 + k * 16.0 + rr.uniform(-2, 2)
        ln = 27.0 - k * 2.0 + rr.uniform(-2, 3)
        ang = math.radians(rr.uniform(12, 26))
        d = V((-math.sin(ang), math.cos(ang), rr.uniform(-0.15, 0.15)))
        zf = (1 if k % 2 else -1)
        plume("spine%d" % k, V((xc(y0) - hw(y0) * 0.95, y0, zf * 0.9)), d, ln, rr.uniform(5.6, 7.2), P["feather"] if k % 3 else P["feather2"], facing=(0.25, 0, zf), curl=zf * rr.uniform(1.0, 2.6), serrate=0.22, rows=6,
                  thick=0.3, seed=400 + k, part="plumage", droop=0.4 * zf, bend=V((-rr.uniform(1.0, 3.0), 0, 0)))
    # ---------------------------------------------------------------- wind: three tapering, glowing streams spiralling up round the blade (real tubes, a thick root and a needle end)
    for w in range(3):
        pts = []
        ph = w * TAU / 3 + 0.6
        for i in range(14):
            t = i / 13.0
            y = 38 + t * 108
            a = ph + t * TAU * 1.4
            r = 5.4 + 2.2 * math.sin(t * math.pi) + 0.8 * K.noise3((t * 5, w, 0), 1.0, 3)
            pts.append(V((xc(y) + r * math.cos(a), y, 0.55 * r * math.sin(a))))
        K.tapered("windstream%d" % w, pts, 1.15, 0.06, P["wind"], sides=5, sub=1, part="wind", bulge=lambda t: math.sin(math.pi * min(1.0, 0.15 + 0.85 * t)) ** 0.6)
    # ---------------------------------------------------------------- tsuka: same-skin core, a diamond wrap of two crossing ito cords, menuki, fuchi and kashira, a long tasselled cord
    K.grip_wrapped("grip", 5.0, 27.0, 1.9, P["same"], P["ito"], turns=7.0, cord=0.44, bands=(), band_mat=P["gold"], swell=0.1, part="grip", seed=7, sides=10)
    K.helix_wrap("grip_wrap2", 5.4, 26.6, 1.9 * 1.07, P["ito2"], turns=4.0, cord=0.32, part="grip", seed=8, squash=0.92)
    for i, y in enumerate((6.0, 26.6)):
        c = K.bl.lathe("fuchi%d" % i, [(1.7, y - 0.8), (2.5, y - 0.4), (2.7, y), (2.5, y + 0.4), (1.7, y + 0.8)], P["gold"], sides=12, part="grip_rings", squash=0.92)
        K._finish_obj(c, "grip_rings", True, 45.0)
    for i, y in enumerate((12.0, 19.0)):
        K.blob("menuki%d" % i, (0.3, y, 2.1), (1.0, 1.5, 0.55), P["gold"], seed=i + 3, lump=0.1, rings=5, segs=8, part="grip_menuki")
    kb = K.bl.lathe("kashira", [(0.01, 1.2), (1.8, 1.6), (2.6, 3.0), (2.7, 4.6), (2.0, 5.4), (1.9, 5.8)], P["bronze"], sides=14, part="pommel", squash=0.92)
    K._finish_obj(kb, "pommel", True, 45.0)
    for i, (ang, ln) in enumerate(((-25, 20.0), (8, 26.0))):
        a = math.radians(ang)
        pts = [V((0.6 * math.sin(a) * k + 1.4 * math.sin(a), 1.0 - k * ln / 8.0, 0.2 * k * math.cos(a) + 0.9 * math.sin(k * 0.9 + i))) for k in range(9)]
        K.tapered("cord%d" % i, pts, 0.42, 0.28, P["cord"], sides=5, sub=1, part="pommel_cords")
        K.blob("tassel%d" % i, pts[-1] + V((0, -1.2, 0)), (0.9, 1.9, 0.9), P["feather2"], seed=i, lump=0.08, rings=4, segs=6, part="pommel_cords")
    # long drooping tail feathers from the kashira
    for i, (ang, ln) in enumerate(((-18, 26.0), (22, 21.0))):
        a = math.radians(ang)
        plume("tailf%d" % i, V((1.5 * math.sin(a), 0.5, -1.0)), V((math.sin(a) * 0.5, -1.0, -0.1)), ln, 4.0, P["feather"] if i else P["feather2"], facing=(1, 0, 0), curl=2.0, serrate=0.2, rows=6, thick=0.26, seed=500 + i,
                  part="pommel_plumes", bend=V((2.0 * math.sin(a), 0, 1.5)))
    # a few feather fragments drifting in the wind (bounded)
    for i in range(3):
        plume("drift%d" % i, V((rr.choice((-1, 1)) * rr.uniform(10, 16), rr.uniform(60, 140), rr.uniform(-4, 4))), V((rr.uniform(-0.3, 0.3), 1, rr.uniform(-0.3, 0.3))), rr.uniform(5, 8), 2.0, P["feather"], facing=(0, 0, 1),
                  curl=1.4, serrate=0.18, rows=4, thick=0.2, seed=600 + i, part="floats")
    return K.quad_estimate()


# ================================================================ 3. METEOR SWORD  (a slab of fallen star: broad rough blade, amethyst clusters, a rock pommel with a burning tail)
def meteor_palette():
    P = {}
    P["rock"] = K.surface("meteor_iron", rgb(46, 40, 50), 0.4, 0.5, wear=1.0, wear_color=rgb(170, 160, 175), wear_metal=1.0, dirt=0.95, dirt_color=(0.006, 0.005, 0.008, 1), scratches=0.5, bump="hammer",
                          bump_scale=0.45, bump_strength=0.4, mottle=1.0, mottle_color=rgb(96, 60, 100), fine=0.7, seed=71, bevel=0.2, ao_dist=2.6,
                          cramp=("y", 32.0, 150.0, [(0.0, rgb(44, 40, 52)), (0.6, rgb(66, 58, 72)), (1.0, rgb(96, 80, 106))]), wear_radius=0.6)
    P["rock2"] = K.surface("meteor_rock", rgb(48, 42, 46), 0.2, 0.75, wear=0.9, wear_color=rgb(150, 130, 120), wear_metal=0.4, dirt=1.0, dirt_color=(0.004, 0.003, 0.004, 1), bump="hammer", bump_scale=0.35,
                           bump_strength=0.7, mottle=1.0, mottle_color=rgb(120, 72, 50), fine=0.8, seed=72, bevel=0.1, ao_dist=2.2)
    P["gold"] = K.surface("meteor_gold", rgb(226, 170, 56), 1.0, 0.26, wear=0.9, wear_color=rgb(255, 242, 190), dirt=0.8, dirt_color=(0.05, 0.03, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7, bump_strength=0.18,
                          mottle=0.7, mottle_color=rgb(150, 90, 24), fine=0.4, seed=73, bevel=0.16, ao_dist=1.8)
    P["gold_dark"] = K.surface("meteor_gold_dark", rgb(150, 100, 38), 1.0, 0.4, wear=0.9, wear_color=rgb(230, 180, 90), dirt=0.9, dirt_color=(0.03, 0.015, 0.004, 1), scratches=0.5, bump="brushed", bump_strength=0.12,
                               mottle=0.7, mottle_color=rgb(80, 44, 14), seed=74, bevel=0.14)
    P["ame"] = K.surface("amethyst", rgb(150, 70, 230), 0.0, 0.06, emit=rgb(170, 90, 255), emit_strength=0.6, emit_flicker=0.6, mottle=0.9, mottle_color=rgb(230, 190, 255), seed=75, bevel=0.0, rim=(rgb(230, 200, 255), 0.5))
    P["molten"] = K.surface("molten", rgb(255, 120, 24), 0.0, 0.5, emit=rgb(255, 120, 24), emit_strength=1.6, emit_flicker=0.8, mottle=0.9, mottle_color=rgb(255, 220, 90), seed=76, bevel=0.0,
                            emit_ramp=("y", -30.0, 12.0, [(0.0, rgb(255, 60, 8)), (0.6, rgb(255, 130, 24)), (1.0, rgb(255, 220, 110))]))
    P["tail"] = K.surface("meteor_tail", rgb(255, 110, 20), 0.0, 0.5, emit=rgb(255, 110, 20), emit_strength=1.0, emit_flicker=0.8, mottle=0.8, mottle_color=rgb(255, 200, 70), seed=77, bevel=0.0,
                          feather=(7, 3, 0.4, 0.4), tramp=[(0.0, rgb(255, 210, 90)), (0.5, rgb(255, 100, 18)), (1.0, rgb(150, 30, 10))], temit=[(0.0, rgb(255, 190, 70)), (1.0, rgb(255, 60, 12))])
    P["wrap"] = K.surface("meteor_wrap", rgb(40, 30, 40), 0.0, 0.62, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(10, 6, 10), groove_amount=0.9, dirt=0.8, dirt_color=(0.01, 0.006, 0.01, 1),
                          mottle=0.8, mottle_color=rgb(80, 50, 80), wear=0.6, wear_color=rgb(150, 110, 140), wear_metal=0.0, fine=0.6, seed=78, bevel=0.0)
    P["core"] = K.surface("meteor_core", rgb(60, 40, 40), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8, seed=79, bevel=0.0)
    return P


def meteor_sword(opts):
    begin(opts)
    weights(blade=1.3, guard=1.1, crystals=0.9, tail=0.8, debris=0.5, pommel=1.1)
    P = meteor_palette()
    rr = K.rng(5303)
    ymid = 31.0

    def hw(y):
        return K.interp([(32, 8.0), (40, 9.8), (70, 10.2), (100, 9.4), (124, 8.2), (138, 5.6), (146, 2.6), (152, 0.6)], y)
    # ---------------------------------------------------------------- blade: a broad slab, chopped edges, a slanted broken tip; faceted section and rough, displaced surface
    stL = [(y, hw(y)) for y in (32, 40, 70, 100, 124, 138, 146, 152)]
    notchL = [(46, 1.8, 4.0, "chip"), (58, 2.8, 7.0, "step"), (76, 2.0, 5.0, "v"), (90, 3.2, 8.0, "step"), (108, 2.2, 6.0, "chip"), (122, 2.8, 6.0, "step"), (134, 1.6, 3.0, "v")]
    notchR = [(44, 2.4, 6.0, "step"), (62, 1.6, 3.0, "chip"), (80, 3.0, 8.0, "step"), (96, 1.8, 4.0, "v"), (112, 3.0, 7.0, "step"), (128, 2.0, 4.0, "chip"), (140, 1.4, 3.0, "v")]
    L = K.blade_edge(-1, stL, 32, 150, notches=notchL, jitter=0.35, res=5.0, seed=81, tip=(-3.4, 143.5))
    R = K.blade_edge(1, stL, 32, 152, notches=notchR, jitter=0.35, res=5.0, seed=82, tip=(1.6, 152.0))
    sec = ((-1.0, -0.8, -0.3, 0.25, 0.8, 1.0), (0.0, 0.7, 1.0, 0.9, 0.7, 0.0))
    ob = K.blade("blade", P["rock"], L, R, thick=lambda t: 2.3 * (1.0 - 0.4 * t ** 2.0), section=sec, seed=5, wob=0.18, bevel_w=0.2)
    K.displace(ob, 0.5, 0.11, 7, 3, mask=lambda co: 0.35 + 0.65 * K.smoothstep(34.0, 60.0, co.y))
    # amethyst clusters grown out of both faces, and into the edges
    for i, (x, y, s_) in enumerate(((-4.0, 54, 1), (4.5, 76, -1), (-5.5, 98, 1), (3.0, 116, -1), (5.0, 130, 1), (-3.5, 66, -1))):
        zs = 2.2 * (1 - 0.4 * ((y - 32) / 118.0) ** 2) * s_
        for k in range(2):
            d = V((rr.uniform(-0.5, 0.5), rr.uniform(-0.2, 0.8), s_ * rr.uniform(0.6, 1.0)))
            c = V((x + rr.uniform(-1.2, 1.2), y + rr.uniform(-1.5, 1.5), zs + s_ * 0.2))
            K.shard("ame%d_%d" % (i, k), c + d * 1.0, rr.uniform(1.4, 2.1), P["ame"], rr, part="crystals", sides=6, stretch=rr.uniform(2.2, 3.0), dir_=d)
    # molten seams between slabs: chunky real bars lying in the surface breaks, both faces
    for s_ in (1, -1):
        for i, (x0, y0, x1, y1) in enumerate(((-7.0, 66, 2.0, 72), (-6.0, 116, 1.0, 124))):
            z = 2.0 * (1 - 0.4 * ((y0 - 32) / 118.0) ** 2) * s_ * 0.92
            K.bar("seam%d_%d" % (i, s_), (x0, y0, z), (x1, y1, z), 0.34, 0.4, P["molten"], part="molten", taper=0.4, bevel_w=0.0)
    # ---------------------------------------------------------------- guard: two jagged stone wings, stepped and chipped, left and right different, a gold eight-point star on a gold plate between them
    wingR = [(3.0, ymid - 5.0), (9.0, ymid - 7.2), (15.5, ymid - 4.0), (22.0, ymid - 6.2), (27.0, ymid - 1.0), (23.0, ymid + 2.0), (26.0, ymid + 6.0), (17.0, ymid + 5.4), (11.0, ymid + 8.8), (3.0, ymid + 5.0)]
    wingL = [(-3.0, ymid - 5.0), (-8.0, ymid - 8.0), (-13.0, ymid - 3.5), (-19.5, ymid - 5.0), (-23.5, ymid + 0.5), (-19.0, ymid + 3.0), (-21.0, ymid + 8.0), (-14.0, ymid + 5.0), (-9.0, ymid + 9.6), (-3.0, ymid + 5.0)]
    for s_ in (1, -1):
        K.stepped_plate("wingR%d" % s_, wingR, 0.6 * s_, P["rock"], steps=2, step_h=1.0, inset=1.3, part="guard", up=s_, bevel_w=0.0)
        K.stepped_plate("wingL%d" % s_, wingL, 0.6 * s_, P["rock"], steps=2, step_h=1.0, inset=1.3, part="guard", up=s_, bevel_w=0.0)
        star = []
        for i in range(16):
            a = TAU * i / 16 + 0.2
            r = 8.6 if i % 2 == 0 else 4.2
            star.append((r * math.cos(a), ymid + r * math.sin(a)))
        K.stepped_plate("star%d" % s_, star, 1.4 * s_, P["gold"], steps=2, step_h=0.8, inset=0.7, part="guard_star", up=s_, bevel_w=0.0)
        K.gem_setting("sky%d" % s_, (0, ymid, 3.9 * s_), 3.2, P["ame"], P["gold"], normal=(0, 0, s_), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.55)
        K.rivets("wing_rv%d" % s_, [(V((x, y, 2.8 * s_)), (0, 0, s_)) for x, y in ((12.0, ymid - 1.0), (18.5, ymid - 0.5), (-10.0, ymid - 0.5), (-15.5, ymid + 0.0))], P["gold_dark"], r=0.8, rng_=rr, jitter=0.2, detail=False)
    # rock chunks and amethyst splinters grown on the wings
    for i in range(5):
        sx = rr.choice((-1, 1))
        K.blob("chunk%d" % i, (sx * rr.uniform(8, 20), ymid + rr.uniform(-4, 6), rr.uniform(-1.0, 1.0) + 2.0 * sx * 0.0), (rr.uniform(1.6, 3.0), rr.uniform(1.4, 2.4), rr.uniform(1.0, 1.8)), P["rock2"], seed=i, lump=0.3, rings=6, segs=9, part="guard")
    for i in range(3):
        sx = -1 if i % 2 else 1
        d = V((sx * rr.uniform(0.3, 0.8), rr.uniform(0.2, 1.0), rr.uniform(-0.3, 0.5)))
        K.shard("wing_ame%d" % i, V((sx * rr.uniform(10, 22), ymid + rr.uniform(-2, 4), 2.6)), rr.uniform(0.9, 1.3), P["ame"], rr, part="crystals", sides=6, stretch=2.8, dir_=d)
    # ---------------------------------------------------------------- grip and the meteor pommel
    K.grip_wrapped("grip", 12.0, 27.0, 2.1, P["core"], P["wrap"], turns=7.0, cord=0.42, bands=(13.0, 20.0, 26.2), band_mat=P["gold"], swell=0.14, part="grip", seed=7)
    head = K.blob("pommel_rock", (0, 6.0, 0), (6.4, 6.4, 6.0), P["rock2"], seed=11, lump=0.34, lump_scale=0.07, rings=10, segs=14, part="pommel")
    for i in range(4):                                                   # molten pits: glowing lumps pushed into the rock
        a, b_ = rr.uniform(0, TAU), rr.uniform(-0.6, 0.6)
        c = V((5.6 * math.cos(a) * math.cos(b_), 6.0 + 5.6 * math.sin(b_), 5.6 * math.sin(a) * math.cos(b_)))
        K.blob("pit%d" % i, c, (1.5, 1.4, 1.2), P["molten"], seed=20 + i, lump=0.25, rings=4, segs=6, part="pommel_molten")
    ring = K.bl.lathe("pommel_band", [(5.0, 8.4), (6.6, 8.6), (7.0, 9.6), (6.6, 10.6), (5.0, 10.8), (4.2, 9.6)], P["gold"], sides=14, part="pommel")
    K._finish_obj(ring, "pommel", True, 45.0)
    # the burning tail of the star: flame tongues streaming down from the rock, wide and layered
    for i, (ang, ln, wd) in enumerate(((-24, 34.0, 5.6), (-4, 44.0, 6.6), (16, 36.0, 5.8), (36, 24.0, 4.0))):
        a = math.radians(ang)
        plume("tail%d" % i, V((3.0 * math.sin(a), 3.0, -2.0 + 1.0 * (i % 3))), V((math.sin(a) * 0.7, -1.0, 0.05 * (i % 3 - 1))), ln, wd, P["tail"], facing=(1, 0, 0) if i % 2 else (0, 0, 1), curl=rr.uniform(-2.0, 2.0),
                  serrate=0.14, rows=5, thick=0.5, seed=700 + i, part="tail", bend=V((rr.uniform(-3, 3), 0, rr.uniform(-2, 2))), profile=lambda t: (min(1.0, t / 0.15)) * (1 - t) ** 0.65)
    K.shard_cloud("debris", (0, -8, 0), (14.0, 22.0, 8.0), 7, (0.8, 1.7), P["rock2"], rr, avoid=lambda p: abs(p.x) < 5 and p.y > -2, part="debris", stretch=1.6, sides=5, max_count=5,
                  flow=lambda p: V((-p.x * 0.05, -1.0, rr.uniform(-0.3, 0.3))))
    K.shard_cloud("embers", (0, 100, 0), (22.0, 55.0, 9.0), 6, (0.5, 1.1), P["molten"], rr, avoid=lambda p: abs(p.x) < hw(min(max(p.y, 32), 150)) + 3.0 or p.y < 36, part="debris", stretch=1.8, sides=3, max_count=4,
                  flow=lambda p: V((0.2, -1.0, 0.0)))
    return K.quad_estimate()


# ================================================================ 4. ASSASSIN SWORD  (ender pearl + ink + spider eye: a slim black saw-backed blade, a spider-leg guard, an ender pearl, a scarf)
def assassin_palette():
    P = {}
    P["steel"] = K.surface("assassin_steel", rgb(34, 32, 46), 0.85, 0.3, wear=1.0, wear_color=rgb(200, 160, 255), wear_metal=1.0, dirt=0.8, dirt_color=(0.004, 0.003, 0.008, 1), scratches=0.9, bump="brushed",
                           bump_strength=0.12, mottle=0.7, mottle_color=rgb(64, 40, 100), fine=0.3, seed=91, bevel=0.2, ao_dist=1.6,
                           cramp=("y", 32.0, 150.0, [(0.0, rgb(28, 26, 38)), (0.6, rgb(44, 38, 64)), (1.0, rgb(90, 56, 150))]), wear_radius=0.45)
    P["dark"] = K.surface("assassin_dark", rgb(24, 22, 30), 1.0, 0.36, wear=1.0, wear_color=rgb(150, 120, 200), dirt=0.9, dirt_color=(0.003, 0.003, 0.006, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.2,
                          mottle=0.8, mottle_color=rgb(50, 36, 80), fine=0.5, seed=92, bevel=0.18, ao_dist=2.0)
    P["leg"] = K.surface("assassin_chitin", rgb(30, 24, 40), 0.5, 0.35, wear=1.0, wear_color=rgb(170, 110, 220), wear_metal=0.6, dirt=0.8, dirt_color=(0.003, 0.002, 0.006, 1), bump="scales", bump_scale=1.5, bump_strength=0.3,
                         mottle=0.9, mottle_color=rgb(70, 30, 90), seed=93, bevel=0.1, ao_dist=1.4)
    P["silver"] = K.surface("assassin_silver", rgb(150, 150, 168), 1.0, 0.3, wear=0.9, wear_color=rgb(235, 235, 255), dirt=0.8, dirt_color=(0.01, 0.01, 0.02, 1), scratches=0.8, bump="hammer", bump_scale=0.7, bump_strength=0.15,
                            seed=94, bevel=0.16, ao_dist=1.8)
    P["glow"] = K.surface("assassin_glow", rgb(190, 90, 255), 0.0, 0.4, emit=rgb(176, 80, 255), emit_strength=1.2, emit_flicker=0.8, seed=95, bevel=0.0,
                          emit_ramp=("y", 32.0, 150.0, [(0.0, rgb(130, 40, 220)), (0.7, rgb(190, 90, 255)), (1.0, rgb(230, 170, 255))]))
    P["eye"] = K.surface("spider_eye", rgb(220, 20, 90), 0.0, 0.05, emit=rgb(255, 40, 120), emit_strength=1.0, emit_flicker=0.6, mottle=0.9, mottle_color=rgb(255, 150, 200), seed=96, bevel=0.0)
    P["pearl"] = K.surface("ender_pearl", rgb(24, 150, 130), 0.0, 0.08, emit=rgb(30, 220, 170), emit_strength=0.7, emit_flicker=0.7, mottle=1.0, mottle_color=rgb(120, 40, 200), seed=97, bevel=0.0, rim=(rgb(160, 255, 230), 0.5))
    P["ink"] = K.surface("ink", rgb(6, 5, 10), 0.0, 0.08, mottle=0.5, mottle_color=rgb(30, 20, 50), seed=98, bevel=0.0, rim=(rgb(120, 90, 190), 0.6))
    P["wrap"] = K.surface("assassin_wrap", rgb(30, 26, 38), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(6, 4, 10), groove_amount=0.9, dirt=0.8, dirt_color=(0.005, 0.004, 0.008, 1),
                          mottle=0.8, mottle_color=rgb(60, 40, 90), wear=0.6, wear_color=rgb(150, 110, 200), wear_metal=0.0, fine=0.6, seed=99, bevel=0.0)
    P["cord"] = K.surface("assassin_cord", rgb(120, 60, 200), 0.3, 0.45, bump="wrap", bump_strength=0.7, groove=rgb(30, 10, 60), groove_amount=0.8, wear=0.7, dirt=0.6, seed=100, bevel=0.0, fine=0.5)
    P["cloth"] = K.surface("assassin_cloth", rgb(18, 16, 24), 0.0, 0.75, bump="cross", bump_scale=1.4, bump_strength=0.5, dirt=0.5, mottle=0.8, mottle_color=rgb(50, 30, 80), wear=0.6, wear_color=rgb(120, 80, 190),
                           wear_metal=0.0, seed=101, bevel=0.0, tramp=[(0.0, rgb(16, 14, 22)), (0.7, rgb(24, 20, 34)), (1.0, rgb(90, 50, 150))])
    return P


def assassin_sword(opts):
    begin(opts)
    weights(blade=1.4, blade_glow=0.8, guard=1.1, legs=1.0, pommel=1.0, floats=0.6)
    P = assassin_palette()
    rr = K.rng(5404)
    ymid = 31.0

    def xc(y):
        return 2.6 * ((y - 32.0) / 118.0) ** 2.0

    def hw(y):
        return K.interp([(32, 4.4), (40, 5.0), (70, 4.8), (100, 4.2), (125, 3.3), (140, 2.0), (151, 0.3)], y)
    # ---------------------------------------------------------------- blade: slim, a back edge of saw teeth, a hooked talon tip, a deep fuller with a glowing line in it
    stL = [(y, hw(y)) for y in (32, 40, 70, 100, 125, 140, 151)]
    teeth = [(y, 1.3 + 0.3 * ((y // 6) % 2), 5.4, "tooth") for y in range(50, 132, 6)]
    L = K.blade_edge(-1, stL, 32, 151, notches=[(62, 0.5, 2.0, "chip"), (92, 0.5, 2.0, "chip"), (118, 0.6, 2.4, "v")], jitter=0.04, res=7.0, seed=111, tip=(-0.8, 152.0))
    R = K.blade_edge(1, stL, 32, 151, notches=teeth, jitter=0.04, res=7.0, seed=112, tip=(0.7, 150.0))
    L = [V((p.x + xc(p.y), p.y, 0)) for p in L]
    R = [V((p.x + xc(p.y), p.y, 0)) for p in R]
    K.blade("blade", P["steel"], L, R, thick=lambda t: 1.0 * (1.0 - 0.45 * t ** 2.0), fuller=0.3, fuller_depth=0.6, fuller_off=-0.15, seed=11, wob=0.03, bevel_w=0.28)
    for s_ in (1, -1):
        rowsI = []
        for y in range(40, 144, 3):
            c = V((xc(y) - 0.15 * hw(y), y, 0.0))
            w = 0.45 * (1.0 - 0.6 * ((y - 40) / 104.0))
            rowsI.append((c - V((w, 0, 0)), c + V((w, 0, 0))))
        K.strip("glowline%d" % s_, rowsI, P["glow"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: 0.4 * (1 - 0.3 * t), part="blade_glow", up=(0, 0, s_))
    # ---------------------------------------------------------------- guard: a plate hub set with a cluster of spider eyes, and six jointed spider legs (real tapering tubes with a knee) that form the crossguard
    hub = [(-5.4, ymid - 3.0), (-3.0, ymid - 5.4), (3.0, ymid - 5.4), (5.4, ymid - 3.0), (6.0, ymid + 2.0), (3.4, ymid + 5.8), (-3.4, ymid + 5.8), (-6.0, ymid + 2.0)]
    for s_ in (1, -1):
        K.stepped_plate("hub%d" % s_, hub, 0.5 * s_, P["dark"], steps=3, step_h=0.6, inset=0.9, part="guard", up=s_)
        K.gem_setting("eye%d" % s_, (0, ymid + 0.6, 2.6 * s_), 2.1, P["eye"], P["silver"], normal=(0, 0, s_), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.45)
        for k, (x, y, r) in enumerate(((-2.8, ymid + 3.4, 0.8), (2.8, ymid + 3.6, 0.8), (-3.6, ymid - 2.0, 0.7), (3.6, ymid - 2.2, 0.7))):
            K.gem_setting("eyes%d_%d" % (s_, k), (x, y, 2.0 * s_), r, P["eye"], P["silver"], normal=(0, 0, s_), claws=0, sides=6, bezel=False, part="gem", rng_=rr, claw_r=0.4)
    legs = [(1, 7.0, ymid + 2.0, 12.0, 11.0, 24.0, ymid - 2.0, 4.0), (1, 6.0, ymid - 1.0, 11.0, 4.0, 18.0, ymid - 12.0, 1.0), (1, 5.4, ymid + 4.0, 8.0, 16.0, 13.0, ymid + 22.0, -1.2),
            (-1, 7.0, ymid + 2.5, 11.0, 13.0, 21.0, ymid - 0.5, 4.5), (-1, 6.0, ymid - 1.0, 9.0, 3.0, 14.0, ymid - 14.0, 1.5), (-1, 5.4, ymid + 4.0, 7.0, 13.0, 10.0, ymid + 20.0, -1.0)]
    for i, (sx, x0, y0, kx, ky, tx, ty, z) in enumerate(legs):
        pts = [V((sx * x0, y0, 0.0)), V((sx * (x0 + (kx - x0) * 0.6), y0 + (ky - y0 + 8) * 0.55, 0.6)), V((sx * kx, ymid + ky, 1.0 * (1 if i % 2 else -1) * 0.4)),
               V((sx * (kx + (tx - kx) * 0.5), ymid + ky + (ty - ymid - ky) * 0.35, z * 0.4)), V((sx * tx, ty, z * 0.2))]
        K.tapered("leg%d" % i, pts, 2.3 - 0.15 * (i % 3), 0.2, P["leg"], sides=7, sub=2, part="legs")
        K.blob("knee%d" % i, pts[2], (2.2, 2.2, 2.2), P["leg"], seed=i, lump=0.12, rings=5, segs=8, part="legs")
    # ink: three drips hanging from the lower edge of the hub, and one on the blade root
    for i, (x, ln, r) in enumerate(((-3.2, 9.0, 1.25), (0.8, 14.0, 1.5), (3.8, 7.0, 1.1))):
        K.drip("ink%d" % i, (x, ymid - 5.0, 0.4), ln, r, P["ink"], seed=30 + i, sides=7, part="ink")
    # ---------------------------------------------------------------- grip: black cross wrap, a violet cord, dark bands; the pommel is an ender pearl in a claw cage, with a long torn scarf
    K.grip_wrapped("grip", 8.5, 26.5, 1.9, P["wrap"], P["cord"], turns=9.0, cord=0.34, bands=(9.4, 17.4, 25.4), band_mat=P["dark"], swell=0.12, part="grip", seed=4)
    seat = K.bl.lathe("pommel_seat", [(0.01, 0.4), (2.0, 0.6), (3.0, 1.6), (3.2, 2.8), (2.2, 3.8), (2.0, 4.4)], P["dark"], sides=12, part="pommel")
    K._finish_obj(seat, "pommel")
    K.blob("pearl", (0, 3.6, 0), (3.1, 3.1, 3.1), P["pearl"], seed=14, lump=0.06, rings=10, segs=14, part="pearl")
    for k in range(5):
        a = TAU * k / 5 + 0.4
        K.tapered("pcage%d" % k, [V((3.0 * math.cos(a), 1.0, 3.0 * math.sin(a))), V((4.7 * math.cos(a), 3.6, 4.7 * math.sin(a))), V((2.8 * math.cos(a), 7.2, 2.8 * math.sin(a)))], 0.5, 0.2, P["silver"], sides=5, part="pommel")
    for i, (ang, ln) in enumerate(((-24, 30), (16, 38), (40, 24))):
        a = math.radians(ang)
        path = [V((2.4 * math.sin(a) * (1 + 0.4 * k), 1.0 - k * ln / 11.0, 1.2 * math.cos(a) * 0.4 + 0.9 * math.sin(k * 0.8 + i))) + V((1.2 * math.sin(a) * k * 0.4, 0, 0)) for k in range(12)]
        K.ribbon("scarf%d" % i, path, lambda t: 2.4 * (1.0 - 0.4 * t) + 0.4, P["cloth"], facing=(math.cos(a), 0, -math.sin(a)), thick=0.14, part="pommel_scarf", fray=0.8, rng_=rr, edge_wave=0.12, notch=1.4 if i == 1 else 0.0)
    K.shard_cloud("shard", (0, 96, 0), (14.0, 56.0, 7.0), 6, (0.8, 1.4), P["glow"], rr, avoid=lambda p: abs(p.x) < hw(min(max(p.y, 32), 150)) + 4.0 or p.y < 40, part="floats", stretch=2.4, sides=4, max_count=6,
                  flow=lambda p: V((0.1, 1.0, rr.uniform(-0.3, 0.3))))
    return K.quad_estimate()


# ================================================================ shell helpers (the tortoise forms): hexagonal scutes laid on a dome, real geometry
def hex_outline(r, rot=0.0, jit=0.0, rng_=None):
    pts = []
    for i in range(6):
        a = rot + TAU * i / 6
        k = r * (1.0 + (rng_.uniform(-jit, jit) if rng_ else 0.0))
        pts.append((k * math.cos(a), k * math.sin(a)))
    return pts


def shell_dome(name, cx, cy, R, H, side, base_mat, plate_mat, rim_mat, rr, *, hexr=3.4, plate_h=0.55, part="shell", jit=0.08, rim=True):
    """A half dome of radius R and height H facing +z (side=1) or -z: a lathe of dark (glowing) base covered with bevelled hexagonal scutes (each its own thick plate laid tangent to the dome, a gap between them) and a rim band.
    Returns the number of scutes."""
    def f(rho):
        return H * math.sqrt(max(0.0, 1.0 - (rho / R) ** 2))
    prof = [(max(R * i / 6, 0.01), side * f(R * i / 6)) for i in range(7)][::-1]
    dome = K.bl.lathe(name + "_base", prof, base_mat, sides=16, axis="z", caps=(False, False), part=part + "_base", origin=(cx, cy, 0))
    K._finish_obj(dome, part + "_base", True, 50.0)
    sp = hexr * 1.732 * 1.07
    n_plates = 0
    for r_ in range(-4, 5):
        for q_ in range(-5, 6):
            x = sp * (q_ + r_ * 0.5)
            y = sp * 0.866 * r_
            rho = math.hypot(x, y)
            if rho > R - hexr * 0.6:
                continue
            s = max(0.28, math.sqrt(max(0.0, 1.0 - (rho / R) ** 2)))
            nrm = V((H * x / (R * R * s), H * y / (R * R * s), side))
            nrm.normalize()
            hh = hexr * (0.93 + 0.1 * rr.random())
            pl = K.plate("%s_hex%d" % (name, n_plates), hex_outline(hh, rr.uniform(0, 0.25), jit, rr), 0.0, plate_h * rr.uniform(0.85, 1.25), plate_mat, part=part, bevel_w=0.0)
            K.xf(pl, K.frame(V((cx + x, cy + y, side * f(rho) - side * 0.1)), nrm, (0, 1, 0)))
            n_plates += 1
    if rim:
        rm = K.bl.lathe(name + "_rim", [(R - 0.6, -side * 0.4), (R + 0.4, -side * 0.1), (R + 0.9, side * 0.9), (R + 0.3, side * 1.5), (R - 0.7, side * 1.0)], rim_mat, sides=24, axis="z", caps=(False, False),
                        part=part + "_rim", origin=(cx, cy, 0))
        K._finish_obj(rm, part + "_rim", True, 40.0)
    return n_plates


# ================================================================ 5. SPIRIT TORTOISE SWORD  (a heavy patient blade: scutes down the spine, a shell guard with flippers, a tortoise-head pommel)
def tortoise_palette():
    P = {}
    P["steel"] = K.surface("tortoise_steel", rgb(78, 100, 98), 1.0, 0.32, wear=1.0, wear_color=rgb(200, 235, 220), dirt=0.8, dirt_color=(0.006, 0.014, 0.012, 1), scratches=0.8, bump="hammer", bump_scale=0.5,
                           bump_strength=0.2, mottle=0.9, mottle_color=rgb(36, 74, 70), fine=0.4, seed=111, bevel=0.2, ao_dist=2.0, wear_radius=0.55,
                           cramp=("y", 32.0, 150.0, [(0.0, rgb(62, 82, 80)), (0.6, rgb(90, 118, 112)), (1.0, rgb(120, 160, 150))]))
    P["scute"] = K.surface("scute", rgb(76, 86, 44), 0.2, 0.5, wear=1.0, wear_color=rgb(236, 214, 130), wear_metal=0.3, dirt=0.95, dirt_color=(0.01, 0.012, 0.004, 1), scratches=0.5, bump="hammer", bump_scale=0.5,
                         bump_strength=0.3, mottle=1.0, mottle_color=rgb(120, 96, 40), fine=0.6, seed=112, bevel=0.2, ao_dist=1.8, wear_radius=0.5)
    P["scute2"] = K.surface("scute2", rgb(52, 66, 40), 0.2, 0.5, wear=1.0, wear_color=rgb(210, 200, 120), wear_metal=0.3, dirt=0.95, dirt_color=(0.006, 0.01, 0.004, 1), bump="hammer", bump_scale=0.5, bump_strength=0.3,
                          mottle=1.0, mottle_color=rgb(100, 120, 50), fine=0.6, seed=113, bevel=0.2, ao_dist=1.8)
    P["seam"] = K.surface("shell_seam", rgb(20, 90, 80), 0.0, 0.5, emit=rgb(40, 200, 170), emit_strength=0.6, emit_flicker=0.8, mottle=0.9, mottle_color=rgb(120, 255, 220), seed=114, bevel=0.0)
    P["bronze"] = K.surface("tortoise_bronze", rgb(150, 112, 52), 1.0, 0.34, wear=0.95, wear_color=rgb(245, 225, 150), dirt=0.9, dirt_color=(0.02, 0.02, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7,
                            bump_strength=0.22, mottle=1.0, mottle_color=rgb(40, 100, 86), fine=0.5, seed=115, bevel=0.18, ao_dist=2.0)
    P["bronze_dark"] = K.surface("tortoise_bronze_dark", rgb(78, 62, 34), 1.0, 0.42, wear=0.9, wear_color=rgb(200, 170, 100), dirt=1.0, dirt_color=(0.01, 0.012, 0.006, 1), bump="brushed", bump_strength=0.14,
                                 mottle=1.0, mottle_color=rgb(30, 80, 70), seed=116, bevel=0.16)
    P["skin"] = K.surface("tortoise_skin", rgb(94, 120, 72), 0.0, 0.55, bump="scales", bump_scale=1.1, bump_strength=0.9, groove=rgb(20, 34, 18), groove_amount=0.8, dirt=0.8, dirt_color=(0.006, 0.01, 0.004, 1),
                          mottle=1.0, mottle_color=rgb(150, 150, 80), wear=0.5, wear_color=rgb(190, 200, 130), wear_metal=0.0, fine=0.5, seed=117, bevel=0.0, ao_dist=1.6)
    P["eye"] = K.surface("tortoise_eye", rgb(255, 190, 40), 0.0, 0.05, emit=rgb(255, 190, 40), emit_strength=0.9, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 240, 150), seed=118, bevel=0.0)
    P["gem"] = K.surface("tortoise_gem", rgb(40, 200, 170), 0.0, 0.06, emit=rgb(60, 230, 190), emit_strength=0.9, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(200, 255, 235), seed=119, bevel=0.0)
    P["wrap"] = K.surface("tortoise_wrap", rgb(44, 64, 44), 0.0, 0.62, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(10, 20, 10), groove_amount=0.9, dirt=0.8, dirt_color=(0.006, 0.01, 0.006, 1),
                          mottle=0.8, mottle_color=rgb(30, 50, 30), wear=0.6, wear_color=rgb(150, 170, 110), wear_metal=0.0, fine=0.6, seed=120, bevel=0.0)
    P["core"] = K.surface("tortoise_core", rgb(60, 48, 30), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8, seed=121, bevel=0.0)
    P["cord"] = K.surface("tortoise_cord", rgb(200, 160, 70), 0.5, 0.45, bump="wrap", bump_strength=0.7, groove=rgb(70, 40, 10), groove_amount=0.8, wear=0.7, dirt=0.6, seed=122, bevel=0.0, fine=0.5)
    return P


def spirit_tortoise_sword(opts):
    begin(opts)
    weights(blade=1.35, blade_plates=1.25, shell=1.1, shell_base=0.9, flippers=1.05, skull=1.1, grip=1.0)
    P = tortoise_palette()
    rr = K.rng(5505)
    ymid = 31.0

    def hw(y):
        return K.interp([(32, 8.0), (38, 8.6), (80, 8.2), (118, 7.4), (136, 6.0), (145, 3.4), (151, 0.6)], y)
    # ---------------------------------------------------------------- blade: broad and heavy, thick bevelled flats, a short blunt-shouldered point; a glowing core strip under a double row of hexagonal scutes
    stL = [(y, hw(y)) for y in (32, 38, 80, 118, 136, 145, 151)]
    L = K.blade_edge(-1, stL, 32, 151, notches=[(64, 0.7, 3.0, "chip"), (98, 0.8, 4.0, "step"), (126, 0.6, 2.4, "chip")], jitter=0.08, res=8.0, seed=131, tip=(-0.6, 152.0))
    R = K.blade_edge(1, stL, 32, 151, notches=[(52, 0.6, 2.4, "chip"), (84, 0.9, 4.0, "step"), (112, 0.7, 3.0, "chip"), (132, 0.6, 2.4, "v")], jitter=0.08, res=8.0, seed=132, tip=(0.8, 150.5))
    th = lambda t: 1.6 * (1.0 - 0.35 * t ** 2.0)                                                  # noqa: E731
    K.blade("blade", P["steel"], L, R, thick=th, section=((-1.0, -0.82, -0.5, 0.5, 0.82, 1.0), (0.0, 0.8, 1.0, 1.0, 0.8, 0.0)), seed=3, wob=0.05)
    for s_ in (1, -1):
        rowsI = []
        for y in range(40, 143, 6):
            w = 0.62 * hw(y)
            rowsI.append((V((-w, y, 0)), V((w, y, 0))))
        K.strip("core%d" % s_, rowsI, P["seam"], (-1.0, -0.5, 0.5, 1.0), (0.0, 1.0, 1.0, 0.0), lambda t: 1.62 * (1.0 - 0.35 * t ** 2.0), part="blade_core", up=(0, 0, s_))
        # the spine armour: overlapping, stepped scutes (each a raised hexagonal plate, every other one a little higher so they lie like roof tiles), shrinking toward the tip
        for i in range(12):
            y = 43.0 + i * 8.4 + rr.uniform(-0.4, 0.4)
            w = 0.6 * hw(y) * (0.96 + 0.08 * rr.random())
            ln = 8.8 - 0.1 * i
            outl = [(-w * 0.62, y - ln / 2), (w * 0.62, y - ln / 2), (w, y), (w * 0.62, y + ln / 2), (-w * 0.62, y + ln / 2), (-w, y)]
            zz = 1.6 * (1.0 - 0.35 * ((y - 32.0) / 118.0) ** 2.0) - 0.15 + (0.3 if i % 2 else 0.0)
            K.stepped_plate("scute%d_%d" % (s_, i), outl, s_ * zz, P["scute"] if i % 3 else P["scute2"], steps=2, step_h=0.6, inset=1.1, part="blade_plates", up=s_, bevel_w=0.0)
    # marginal scutes: clamp plates riding both edges like the rim of a shell, so the silhouette itself is scalloped
    for sx in (-1, 1):
        for i, y in enumerate(range(41, 141, 11 if sx > 0 else 12)):
            yy = y + rr.uniform(-1.0, 1.0)
            xe = sx * (hw(yy) - 0.6)
            ln = 7.0 - 0.02 * (yy - 40)
            o = [(xe - sx * 2.6, yy - ln / 2), (xe + sx * 1.2, yy - ln / 2 + 0.6), (xe + sx * 1.9, yy), (xe + sx * 1.2, yy + ln / 2 - 0.6), (xe - sx * 2.6, yy + ln / 2)]
            zt = 1.6 * (1.0 - 0.35 * ((yy - 32.0) / 118.0) ** 2.0) + 0.35
            K.plate("margin%d_%d" % (sx, i), o, -zt, zt, P["scute2"] if i % 2 else P["bronze"], part="blade_margin", bevel_w=0.0)
    # ---------------------------------------------------------------- guard: a shell dome of hexagonal scutes on each face with a glowing seam beneath, two bronze flippers, rivets, a jade in the crown
    for s_ in (1, -1):
        shell_dome("dome%d" % s_, 0.0, ymid, 11.0, 6.0, s_, P["seam"], P["scute"], P["bronze"], rr, hexr=2.8, plate_h=0.7, part="shell")
        K.gem_setting("jade%d" % s_, (0, ymid, s_ * 6.4), 2.1, P["gem"], P["bronze"], normal=(0, 0, s_), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.5)
    flipR = [(8.0, ymid + 3.0), (14.0, ymid + 4.8), (21.0, ymid + 2.0), (26.5, ymid - 4.0), (26.0, ymid - 9.5), (23.4, ymid - 6.5), (22.8, ymid - 11.5), (20.4, ymid - 7.6), (18.8, ymid - 10.6), (16.4, ymid - 5.8), (11.0, ymid - 5.0), (8.0, ymid - 2.4)]
    flipL = [(-8.0, ymid + 3.0), (-13.0, ymid + 5.4), (-19.5, ymid + 3.0), (-24.0, ymid - 2.0), (-24.5, ymid - 8.0), (-21.6, ymid - 5.6), (-21.0, ymid - 11.0), (-18.4, ymid - 6.8), (-16.4, ymid - 9.0), (-14.6, ymid - 4.6), (-10.0, ymid - 4.0), (-8.0, ymid - 2.0)]
    for s_ in (1, -1):
        K.stepped_plate("flipR%d" % s_, flipR, 0.2 * s_, P["bronze"], steps=2, step_h=0.8, inset=1.2, part="flippers", up=s_, bevel_w=0.0)
        K.stepped_plate("flipL%d" % s_, flipL, 0.2 * s_, P["bronze"], steps=2, step_h=0.8, inset=1.2, part="flippers", up=s_, bevel_w=0.0)
        K.rivets("flip_rv%d" % s_, [(V((x, y, 1.9 * s_)), (0, 0, s_)) for x, y in ((14.5, ymid + 0.5), (19.5, ymid - 1.2), (-13.5, ymid + 0.8), (-18.0, ymid - 1.5))], P["bronze_dark"], r=0.8, rng_=rr, jitter=0.2, detail=False)
    # ---------------------------------------------------------------- grip: thick, dark green wrap with bronze bands, a short neck of scaled skin rings, the head of the tortoise as the pommel
    K.grip_wrapped("grip", 12.0, 27.0, 2.2, P["core"], P["wrap"], turns=6.0, cord=0.46, bands=(12.8, 19.5, 26.4), band_mat=P["bronze"], swell=0.12, part="grip", seed=5)
    for i, y in enumerate((9.0, 11.0)):
        nr = K.bl.lathe("neck%d" % i, [(2.6, y - 0.9), (3.2, y - 0.4), (3.3, y), (3.2, y + 0.4), (2.6, y + 0.9)], P["skin"], sides=12, part="skull_neck")
        K._finish_obj(nr, "head_neck", True, 40.0)
    K.blob("head", (0, 1.0, 0.0), (3.9, 6.2, 3.5), P["skin"], seed=21, lump=0.12, lump_scale=0.07, rings=10, segs=14, part="skull")
    K.tapered("beak_up", [V((0, -2.0, 1.1)), V((0, -6.0, 1.5)), V((0, -9.6, 0.8))], 2.2, 0.4, P["bronze_dark"], sides=6, sub=2, part="skull")
    K.tapered("beak_dn", [V((0, -2.0, -1.1)), V((0, -5.4, -1.2)), V((0, -8.0, -0.6))], 1.7, 0.35, P["bronze_dark"], sides=6, sub=2, part="skull")
    for sx in (-1, 1):
        K.gem_setting("eye%d" % sx, (sx * 3.0, 0.2, 1.5), 0.95, P["eye"], P["bronze"], normal=(sx * 0.8, 0.0, 0.5), claws=3, sides=6, bezel=False, part="skull", rng_=rr, claw_r=0.4)
        K.tapered("brow%d" % sx, [V((sx * 2.2, 1.6, 2.4)), V((sx * 3.4, 0.6, 2.0)), V((sx * 3.9, -1.0, 0.8))], 0.55, 0.2, P["bronze_dark"], sides=5, sub=2, part="skull")
    for i in range(5):                                                   # a ridge of scale spikes down the back of the head
        K.tapered("ridge%d" % i, [V((0, 2.0 + i * 1.1, 3.0 - 0.15 * i)), V((0, 2.4 + i * 1.1, 3.9 - 0.2 * i)), V((0, 2.1 + i * 1.1, 4.4 - 0.2 * i))], 0.55, 0.1, P["bronze"], sides=4, sub=1, part="skull")
    # a tail of cord and a small charm
    for i, (ang, ln) in enumerate(((-30, 12.0), (12, 16.0))):
        a = math.radians(ang)
        pts = [V((1.0 * math.sin(a) * k * 0.6, 5.0 + k * 0.5, -3.0 - k * ln / 7.0 * 0.0 + 0.6 * math.sin(k + i))) for k in range(7)]
        K.tapered("cordtail%d" % i, [V((0.8 * math.sin(a) * k, 6.0 + 0.0 * k, 3.0 - k * 1.6)) for k in range(6)], 0.5, 0.3, P["cord"], sides=6, sub=2, part="grip_cord")
    return K.quad_estimate()


# ================================================================ 6. SPIRIT TORTOISE CARAPACE SWORD  (the shell worn as armour: a blade clad in overlapping plates, a spiked shell crown, ant mandibles, four swords afloat)
def mini_sword(name, c, tilt_deg, length, mats, rr, z=0.0, width=2.6, part="floating"):
    """A small whole sword (lancet blade with a lens section, guard bar, wrapped grip, pommel stone) standing at c, point up, tilted in the xy plane."""
    a = math.radians(tilt_deg)
    d = V((math.sin(a), math.cos(a), 0.0))
    s = V((math.cos(a), -math.sin(a), 0.0))
    c = V(c)
    gl = length * 0.2
    rows = [(l, r) for l, r in ray_rows(c + d * (gl + 0.5), d, length - gl, width, 6)]
    rows = [(l + V((0, 0, z)), r + V((0, 0, z))) for l, r in rows]
    K.strip(name + "_blade", rows, mats["steel"], (-1.0, -0.15, 0.15, 1.0), (0.0, 0.8, 0.8, 0.0), 0.5, part=part + "_blade", up=(0, 0, 1), angle=50)
    pl_ = [c + d * (gl + 1.0 + k * (length - gl - 6) / 4.0) + V((0, 0, z)) for k in range(5)]
    K.strip(name + "_line", [(p - s * 0.18, p + s * 0.18) for p in pl_], mats["glow"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.62, part=part + "_glow", up=(0, 0, 1))
    K.bar(name + "_guard", c + d * gl - s * (width * 1.1) + V((0, 0, z)), c + d * gl + s * (width * 1.1) + V((0, 0, z)), 0.45, 0.5, mats["gold"], part=part, taper=0.8, bevel_w=0.0)
    K.tapered(name + "_grip", [c + V((0, 0, z)) + d * 0.6, c + V((0, 0, z)) + d * (gl * 0.5), c + V((0, 0, z)) + d * (gl - 0.3)], 0.55, 0.5, mats["wrap"], sides=5, sub=1, part=part)
    K.blob(name + "_pom", c + V((0, 0, z)) - d * 0.4, (0.8, 0.8, 0.8), mats["gem"], seed=3, lump=0.05, rings=4, segs=6, part=part + "_glow")


def spirit_tortoise_carapace_sword(opts):
    begin(opts)
    weights(blade=1.3, armour=1.2, blade_core=1.0, crown=1.1, mandibles=1.1, floating=0.8, floating_blade=0.9, pommel=1.0)
    P = tortoise_palette()
    P["chitin"] = K.surface("chitin", rgb(78, 36, 24), 0.5, 0.3, wear=1.0, wear_color=rgb(255, 176, 70), wear_metal=0.8, dirt=0.9, dirt_color=(0.01, 0.004, 0.002, 1), scratches=0.7, bump="scales", bump_scale=1.4,
                            bump_strength=0.3, mottle=1.0, mottle_color=rgb(130, 66, 30), fine=0.5, seed=141, bevel=0.14, ao_dist=1.8, wear_radius=0.5)
    P["chitin2"] = K.surface("chitin2", rgb(46, 54, 36), 0.5, 0.32, wear=1.0, wear_color=rgb(220, 200, 110), wear_metal=0.8, dirt=0.9, dirt_color=(0.005, 0.008, 0.003, 1), scratches=0.6, bump="scales", bump_scale=1.4,
                             bump_strength=0.3, mottle=1.0, mottle_color=rgb(96, 100, 50), fine=0.5, seed=142, bevel=0.14, ao_dist=1.8)
    P["core"] = K.surface("carapace_core", rgb(255, 150, 40), 0.0, 0.5, emit=rgb(255, 150, 40), emit_strength=0.8, emit_flicker=0.8, mottle=0.9, mottle_color=rgb(255, 220, 110), seed=143, bevel=0.0)
    P["lgold"] = P["bronze"]
    rr = K.rng(5606)
    ymid = 31.0

    def hw(y):
        return K.interp([(32, 7.4), (38, 8.0), (80, 7.8), (120, 6.8), (138, 5.0), (146, 2.6), (151, 0.5)], y)
    stL = [(y, hw(y)) for y in (32, 38, 80, 120, 138, 146, 151)]
    L = K.blade_edge(-1, stL, 32, 151, notches=[(60, 0.7, 3.0, "chip"), (102, 0.8, 4.0, "step"), (128, 0.6, 2.4, "chip")], jitter=0.08, res=9.0, seed=151, tip=(-0.8, 152.0))
    R = K.blade_edge(1, stL, 32, 151, notches=[(50, 0.6, 2.4, "chip"), (88, 0.9, 4.0, "step"), (116, 0.7, 3.0, "chip")], jitter=0.08, res=9.0, seed=152, tip=(0.9, 150.5))
    th = lambda t: 1.5 * (1.0 - 0.4 * t ** 2.0)                                                   # noqa: E731
    K.blade("blade", P["steel"], L, R, thick=th, section=((-1.0, -0.8, -0.3, 0.3, 0.8, 1.0), (0.0, 0.8, 1.0, 1.0, 0.8, 0.0)), seed=4, wob=0.05)
    for s_ in (1, -1):
        rowsI = [(V((-0.24 * hw(y), y, 0)), V((0.24 * hw(y), y, 0))) for y in range(40, 143, 7)]
        K.strip("core%d" % s_, rowsI, P["core"], (-1.0, -0.5, 0.5, 1.0), (0.0, 1.0, 1.0, 0.0), lambda t: 1.52 * (1.0 - 0.4 * t ** 2.0), part="blade_core", up=(0, 0, s_))
        # armour: two staggered columns of overlapping pointed plates, each pointing at the tip, its far edge lifted so the next plate lies over it
        for row, y in enumerate([42.0 + k * 8.2 for k in range(12)]):
            for col, cx in enumerate((-1, 1)):
                yy = y + (2.6 if col else 0.0) + rr.uniform(-0.4, 0.4)
                if yy > 136:
                    continue
                size = 0.66 * hw(yy) * (0.95 + 0.1 * rr.random())
                zz = 1.5 * (1.0 - 0.4 * ((yy - 32.0) / 118.0) ** 2.0)
                K.scale_plate("arm%d_%d_%d" % (s_, row, col), V((cx * hw(yy) * 0.47, yy, s_ * zz + s_ * 0.2)), V((0, 0.0, s_)), size, P["chitin"] if (row + col) % 3 else P["chitin2"], up=(0, -1, 0), thick=0.34, rows=2, part="armour",
                                aspect=1.25, tilt=-20.0 * s_, shape="pointed")
    # ---------------------------------------------------------------- guard: a tall spiked shell crown on each face, two mandibles of the giant ant curving up round the blade root
    for s_ in (1, -1):
        shell_dome("dome%d" % s_, 0.0, ymid, 12.0, 6.6, s_, P["core"], P["chitin2"], P["bronze"], rr, hexr=3.1, plate_h=0.8, part="crown")
        K.gem_setting("jade%d" % s_, (0, ymid, s_ * 7.0), 2.2, P["core"], P["bronze"], normal=(0, 0, s_), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.5)
    bases, dirs = [], []
    for k in range(11):
        ang = 20 + k * 33.0
        if abs(((ang % 360) - 90)) < 22 or abs(((ang % 360) - 270)) < 26:
            continue
        a = math.radians(ang)
        bases.append(V((12.6 * math.cos(a), ymid + 12.6 * math.sin(a), 0.4)))
        dirs.append(V((math.cos(a), math.sin(a), 0.0)))
    K.spike_array("crownspike", bases, dirs, 5.5, 1.5, P["chitin"], rr, bend=0.6, sides=5, part="crown_spikes", len_jit=0.3, curve=0.05)
    for sx, ln_k in ((1, 1.0), (-1, 0.82)):
        pts = [V((sx * 8.0, ymid + 2.0, 0.0)), V((sx * 15.5 * ln_k, ymid + 7.0, 0.8)), V((sx * 19.0 * ln_k, ymid + 21.0 * ln_k, 0.4)), V((sx * 14.0 * ln_k, ymid + 36.0 * ln_k, 0.0)), V((sx * 8.2, ymid + 44.0 * ln_k, 0.0))]
        K.tapered("mandible%d" % sx, pts, 2.7, 0.25, P["chitin"], sides=7, sub=2, part="mandibles", bulge=lambda t: 1.0 + 0.3 * math.sin(t * math.pi))
        for k in range(6):
            t0 = 0.22 + k * 0.11
            pp = K.bl.smooth_path(pts, 6)
            p = pp[int(t0 * (len(pp) - 1))]
            K.tapered("tooth%d_%d" % (sx, k), [p - V((sx * 0.4, 0, 0)), p - V((sx * 2.0, -0.8, 0)), p - V((sx * 3.3, -2.2, 0))], 0.8, 0.08, P["chitin2"], sides=4, sub=1, part="mandibles")
    # ---------------------------------------------------------------- grip, a chitin ball pommel with thorns, and four whole swords floating round the blade
    K.grip_wrapped("grip", 11.0, 27.0, 2.2, P["core"] if False else P["wrap"], P["wrap"], turns=6.0, cord=0.46, bands=(11.8, 19.0, 26.2), band_mat=P["bronze"], swell=0.12, part="grip", seed=6)
    K.blob("pommel_ball", (0, 5.0, 0), (5.6, 5.6, 5.2), P["chitin"], seed=31, lump=0.12, lump_scale=0.08, rings=7, segs=10, part="pommel")
    for k in range(6):
        a = TAU * k / 6 + 0.3
        K.tapered("thorn%d" % k, [V((4.4 * math.cos(a), 4.0, 4.4 * math.sin(a))), V((6.4 * math.cos(a), 1.0, 6.4 * math.sin(a))), V((8.0 * math.cos(a), -2.0, 8.0 * math.sin(a)))], 1.1, 0.1, P["chitin2"], sides=5, sub=1, part="pommel")
    nk = K.bl.lathe("pommel_band", [(3.2, 9.2), (4.6, 9.4), (4.8, 10.4), (4.2, 11.0), (3.2, 10.8)], P["bronze"], sides=12, part="pommel")
    K._finish_obj(nk, "pommel", True, 45.0)
    mats = {"steel": P["steel"], "glow": P["seam"], "gold": P["bronze"], "wrap": P["wrap"], "gem": P["gem"]}
    for i, (x, y, tilt, z) in enumerate(((-19.0, 100.0, 10.0, -1.5), (20.0, 120.0, -9.0, 1.0), (-19.5, 74.0, 14.0, 2.0))):
        mini_sword("fsword%d" % i, (x, y, 0.0), tilt, 27.0 - 2.0 * (i % 2), mats, rr, z=z)
    return K.quad_estimate()


# ================================================================ 7. HUNDRED SWORD  (the blade that splits into a hundred: a clean longsword with a fan of whole swords standing behind its guard, and more afloat)
def hundred_palette():
    P = {}
    P["steel"] = K.surface("hundred_steel", rgb(108, 118, 132), 1.0, 0.24, wear=1.0, wear_color=rgb(240, 246, 255), dirt=0.55, dirt_color=(0.02, 0.025, 0.035, 1), scratches=0.9, bump="brushed", bump_strength=0.1,
                           mottle=0.5, mottle_color=rgb(70, 86, 110), fine=0.3, seed=161, bevel=0.2, ao_dist=1.6, wear_radius=0.5,
                           cramp=("y", 32.0, 150.0, [(0.0, rgb(86, 94, 108)), (0.5, rgb(128, 140, 158)), (1.0, rgb(150, 176, 206))]))
    P["steel2"] = K.surface("hundred_steel2", rgb(120, 112, 100), 1.0, 0.26, wear=1.0, wear_color=rgb(255, 240, 210), dirt=0.6, dirt_color=(0.03, 0.025, 0.02, 1), scratches=0.9, bump="brushed", bump_strength=0.1,
                            mottle=0.6, mottle_color=rgb(90, 70, 50), fine=0.3, seed=162, bevel=0.2, ao_dist=1.6, wear_radius=0.5)
    P["gold"] = K.surface("hundred_gold", rgb(226, 172, 60), 1.0, 0.28, wear=0.9, wear_color=rgb(255, 244, 196), dirt=0.8, dirt_color=(0.05, 0.03, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7, bump_strength=0.16,
                          mottle=0.7, mottle_color=rgb(150, 92, 26), fine=0.4, seed=163, bevel=0.16, ao_dist=1.8)
    P["gold_dark"] = K.surface("hundred_gold_dark", rgb(140, 96, 40), 1.0, 0.4, wear=0.9, wear_color=rgb(225, 180, 96), dirt=0.9, dirt_color=(0.03, 0.015, 0.004, 1), scratches=0.5, bump="brushed", bump_strength=0.12,
                               mottle=0.7, mottle_color=rgb(76, 42, 14), seed=164, bevel=0.14)
    P["glow"] = K.surface("hundred_glow", rgb(170, 220, 255), 0.0, 0.4, emit=rgb(150, 210, 255), emit_strength=0.9, emit_flicker=0.7, seed=165, bevel=0.0,
                          emit_ramp=("y", 32.0, 150.0, [(0.0, rgb(255, 220, 140)), (0.5, rgb(190, 230, 255)), (1.0, rgb(140, 200, 255))]))
    P["gem"] = K.surface("hundred_gem", rgb(70, 150, 255), 0.0, 0.06, emit=rgb(80, 160, 255), emit_strength=0.9, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(200, 230, 255), seed=166, bevel=0.0)
    P["wrap"] = K.surface("hundred_wrap", rgb(226, 222, 212), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(90, 84, 70), groove_amount=0.8, dirt=0.7, dirt_color=(0.03, 0.025, 0.018, 1),
                          mottle=0.8, mottle_color=rgb(160, 150, 120), wear=0.6, wear_color=rgb(255, 250, 235), wear_metal=0.0, fine=0.6, seed=167, bevel=0.0)
    P["core"] = K.surface("hundred_core", rgb(80, 70, 60), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8, seed=168, bevel=0.0)
    P["cord"] = K.surface("hundred_cord", rgb(235, 182, 64), 0.5, 0.45, bump="wrap", bump_strength=0.7, groove=rgb(80, 46, 10), groove_amount=0.8, wear=0.7, dirt=0.6, seed=169, bevel=0.0, fine=0.5)
    return P


def fan_sword(name, c, tilt_deg, length, width, mats, z, part="fan", glow=False, hilt=True):
    """A whole sword for the fan: lancet blade with a ridge, a guard bar, a short wrapped grip; standing at c, point along tilt (deg from +y, positive to -x...), in the plane z."""
    a = math.radians(tilt_deg)
    d = V((-math.sin(a), math.cos(a), 0.0))
    s = V((math.cos(a), math.sin(a), 0.0))
    c = V(c)
    gl = 9.0 if hilt else 0.0
    rows = [(l + V((0, 0, z)), r + V((0, 0, z))) for l, r in ray_rows(c + d * gl, d, length, width, 5)]
    K.strip(name + "_blade", rows, mats["blade"], (-1.0, -0.2, 0.2, 1.0), (0.0, 0.9, 0.9, 0.0), 0.55, part=part + "_blade", up=(0, 0, 1), angle=50)
    if hilt:
        K.bar(name + "_guard", c + d * (gl - 0.3) - s * (width * 0.9) + V((0, 0, z)), c + d * (gl - 0.3) + s * (width * 0.9) + V((0, 0, z)), 0.5, 0.55, mats["gold"], part=part + "_hilt", taper=0.8, bevel_w=0.0)
        K.tapered(name + "_grip", [c + V((0, 0, z)), c + V((0, 0, z)) + d * (gl * 0.5), c + V((0, 0, z)) + d * (gl - 0.8)], 0.6, 0.55, mats["wrap"], sides=5, sub=1, part=part + "_hilt")
    if glow:
        pl_ = [c + d * (gl + 2.0 + k * (length - 10) / 3.0) + V((0, 0, z)) for k in range(4)]
        K.strip(name + "_line", [(p - s * 0.2, p + s * 0.2) for p in pl_], mats["glow"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.72, part=part + "_glow", up=(0, 0, 1))


def hundred_sword(opts):
    begin(opts)
    weights(blade=1.35, blade_glow=0.9, guard=1.1, fan_blade=1.15, fan_hilt=1.0, fan_glow=0.8, floats=0.8, grip=1.0)
    P = hundred_palette()
    rr = K.rng(5707)
    ymid = 31.0

    def hw(y):
        return K.interp([(32, 5.8), (38, 6.5), (70, 6.2), (110, 5.4), (132, 4.0), (144, 2.2), (150, 0.4)], y)
    # ---------------------------------------------------------------- the main blade: a clean heroic longsword, stepped chips only on the right edge, a luminous fuller
    stL = [(y, hw(y)) for y in (32, 38, 70, 110, 132, 144, 150)]
    L = K.blade_edge(-1, stL, 32, 150, notches=[(66, 0.5, 2.0, "chip"), (118, 0.5, 2.0, "chip")], jitter=0.05, res=12.0, seed=171, tip=(-0.4, 151.0))
    R = K.blade_edge(1, stL, 32, 150, notches=[(54, 0.7, 2.6, "chip"), (84, 0.9, 4.0, "step"), (108, 0.7, 3.0, "chip"), (130, 0.7, 2.6, "v")], jitter=0.05, res=12.0, seed=172, tip=(0.6, 149.5))
    K.blade("blade", P["steel"], L, R, thick=lambda t: 1.3 * (1.0 - 0.45 * t ** 2.0), fuller=0.22, fuller_depth=0.55, fuller_off=0.0, seed=7, wob=0.04, bevel_w=0.3)
    for s_ in (1, -1):
        rowsI = []
        for y in range(40, 146, 5):
            c = V((0.1 * math.sin(y * 0.08), y, 0.0))
            rowsI.append((c - V((0.7 * (1 - 0.5 * (y - 40) / 106.0), 0, 0)), c + V((0.7 * (1 - 0.5 * (y - 40) / 106.0), 0, 0))))
        K.strip("inlay%d" % s_, rowsI, P["glow"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: 0.95 * (1 - 0.3 * t), part="blade_glow", up=(0, 0, s_))
        cap = [(-4.8, 32.2), (4.8, 32.2), (5.6, 38.0), (3.6, 47.0), (0.0, 50.0), (-3.6, 47.0), (-5.6, 38.0)]
        K.stepped_plate("ricasso%d" % s_, cap, 1.15 * s_, P["gold"], steps=2, step_h=0.5, inset=0.8, part="guard_cap", up=s_, bevel_w=0.0)
        K.rivets("ricasso_rv%d" % s_, [(V((x, y, 2.0 * s_)), (0, 0, s_)) for x, y in ((-3.6, 36.0), (3.6, 36.0), (-2.4, 43.5), (2.4, 43.5))], P["gold_dark"], r=0.7, rng_=rr, jitter=0.15, detail=False)
    # ---------------------------------------------------------------- guard: a wide crescent crown of gold plates, a blue stone in claws
    cres = [(-22.0, ymid + 4.0), (-17.0, ymid - 1.5), (-9.0, ymid - 5.0), (0.0, ymid - 6.0), (9.0, ymid - 5.0), (17.0, ymid - 1.5), (22.5, ymid + 5.0), (19.0, ymid + 6.2), (12.0, ymid + 2.2), (5.0, ymid + 5.8), (0.0, ymid + 8.5),
            (-5.0, ymid + 5.8), (-12.0, ymid + 2.2), (-19.5, ymid + 6.0)]
    for s_ in (1, -1):
        K.stepped_plate("crown%d" % s_, cres, 0.7 * s_, P["gold"], steps=2, step_h=0.9, inset=1.3, part="guard", up=s_, bevel_w=0.0)
        K.gem_setting("sapphire%d" % s_, (0, ymid + 0.8, 3.1 * s_), 2.8, P["gem"], P["gold"], normal=(0, 0, s_), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
        K.rivets("crown_rv%d" % s_, [(V((x, y, 2.5 * s_)), (0, 0, s_)) for x, y in ((-14.0, ymid - 0.5), (14.0, ymid - 0.5), (-8.0, ymid - 3.0), (8.0, ymid - 3.0))], P["gold_dark"], r=0.75, rng_=rr, jitter=0.15, detail=False)
    # ---------------------------------------------------------------- the fan: 15 whole swords stand behind the guard in two layers (left and right not twins), each its own length, width and lean
    mats = {"blade": P["steel"], "gold": P["gold"], "wrap": P["wrap"], "glow": P["glow"]}
    mats2 = {"blade": P["steel2"], "gold": P["gold_dark"], "wrap": P["wrap"], "glow": P["glow"]}
    angs = [-74, -60, -47, -35, -24, -14, 14, 24, 34, 45, 57, 68, 80, -85]
    for i, ang in enumerate(angs):
        ang_ = ang + rr.uniform(-2.5, 2.5)
        ln = (45.0 - 0.2 * abs(ang_) + rr.uniform(-4, 5)) * (0.9 if i % 2 else 1.0)
        x0 = 4.0 * (1 if ang_ > 0 else -1) * -1 * 0.0 + 0.0
        dd = V((-math.sin(math.radians(ang_)), math.cos(math.radians(ang_)), 0.0))
        fan_sword("fan%d" % i, V((0.0, ymid + 0.0, 0.0)) + dd * 9.0, ang_, ln, 3.6 + 0.8 * rr.random(), mats if i % 2 else mats2, z=-2.6 - 0.6 * (i % 3), part="fan", glow=(i % 3 == 0))
    # ---------------------------------------------------------------- a swarm of small swords adrift round the tip (bounded: 8)
    for i in range(8):
        x = rr.choice((-1, 1)) * rr.uniform(10, 19)
        y = rr.uniform(78, 146)
        fan_sword("swarm%d" % i, V((x, y, 0)), rr.uniform(-30, 30), rr.uniform(11, 16), 2.2, mats, z=rr.uniform(-4, 4), part="floats", hilt=False)
    # ---------------------------------------------------------------- grip: white wraps, gold cord, a crowned pommel with a stone
    K.grip_wrapped("grip", 8.5, 26.5, 2.0, P["core"], P["wrap"], turns=8.0, cord=0.38, bands=(9.4, 17.4, 25.4), band_mat=P["gold"], swell=0.16, part="grip", seed=4)
    pom = K.bl.lathe("pommel_cup", [(0.01, 0.4), (2.4, 0.5), (3.8, 1.6), (4.0, 3.0), (3.0, 4.4), (2.2, 5.0)], P["gold_dark"], sides=14, part="pommel")
    K._finish_obj(pom, "pommel")
    K.blob("pommel_stone", (0, 3.4, 0), (2.4, 2.4, 2.4), P["gem"], seed=4, lump=0.06, rings=6, segs=8, part="gem")
    for k in range(7):
        a = TAU * k / 7 + 0.2
        K.tapered("pcrown%d" % k, [V((3.6 * math.cos(a), 2.4, 3.6 * math.sin(a))), V((4.4 * math.cos(a), 4.0, 4.4 * math.sin(a))), V((3.4 * math.cos(a), 6.2, 3.4 * math.sin(a)))], 0.5, 0.1, P["gold"], sides=4, sub=1, part="pommel")
    return K.quad_estimate()


# ================================================================ 8. THUNDER GOD SWORD  (a forked bolt of a blade, gold lightning wings, rods and a magic crystal, a nether star pommel)
def thunder_palette():
    P = {}
    P["steel"] = K.surface("thunder_steel", rgb(34, 48, 84), 0.85, 0.28, wear=1.0, wear_color=rgb(170, 225, 255), wear_metal=1.0, dirt=0.8, dirt_color=(0.004, 0.006, 0.014, 1), scratches=0.9, bump="brushed",
                           bump_strength=0.12, mottle=0.8, mottle_color=rgb(60, 90, 150), fine=0.3, seed=181, bevel=0.2, ao_dist=1.6, wear_radius=0.5,
                           cramp=("y", 32.0, 150.0, [(0.0, rgb(30, 42, 74)), (0.6, rgb(46, 66, 116)), (1.0, rgb(86, 130, 200))]))
    P["gold"] = K.surface("thunder_gold", rgb(238, 184, 52), 1.0, 0.26, wear=0.9, wear_color=rgb(255, 246, 200), dirt=0.8, dirt_color=(0.05, 0.03, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7, bump_strength=0.16,
                          mottle=0.7, mottle_color=rgb(160, 96, 22), fine=0.4, seed=182, bevel=0.16, ao_dist=1.8)
    P["gold_dark"] = K.surface("thunder_gold_dark", rgb(150, 100, 34), 1.0, 0.4, wear=0.9, wear_color=rgb(235, 185, 90), dirt=0.9, dirt_color=(0.03, 0.015, 0.004, 1), scratches=0.5, bump="brushed", bump_strength=0.12,
                               mottle=0.7, mottle_color=rgb(80, 44, 12), seed=183, bevel=0.14)
    P["rod"] = K.surface("thunder_rod", rgb(150, 160, 176), 1.0, 0.26, wear=0.9, wear_color=rgb(240, 248, 255), dirt=0.7, dirt_color=(0.01, 0.012, 0.02, 1), scratches=0.8, bump="brushed", bump_strength=0.12, seed=184, bevel=0.14, ao_dist=1.4)
    P["bolt"] = K.surface("thunder_bolt", rgb(190, 230, 255), 0.0, 0.4, emit=rgb(170, 220, 255), emit_strength=1.1, emit_flicker=0.8, seed=185, bevel=0.0,
                          emit_ramp=("y", 32.0, 150.0, [(0.0, rgb(255, 214, 90)), (0.5, rgb(200, 235, 255)), (1.0, rgb(160, 215, 255))]))
    P["crystal"] = K.surface("magic_crystal", rgb(120, 210, 255), 0.0, 0.05, emit=rgb(120, 210, 255), emit_strength=0.8, emit_flicker=0.6, mottle=0.9, mottle_color=rgb(230, 250, 255), seed=186, bevel=0.0, rim=(rgb(255, 255, 255), 0.6))
    P["star"] = K.surface("nether_star", rgb(240, 214, 140), 0.0, 0.2, emit=rgb(255, 220, 130), emit_strength=0.8, emit_flicker=0.5, mottle=0.9, mottle_color=rgb(255, 255, 255), seed=187, bevel=0.0)
    P["wrap"] = K.surface("thunder_wrap", rgb(26, 32, 52), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(6, 8, 14), groove_amount=0.9, dirt=0.8, dirt_color=(0.004, 0.005, 0.01, 1),
                          mottle=0.8, mottle_color=rgb(50, 70, 120), wear=0.6, wear_color=rgb(140, 170, 220), wear_metal=0.0, fine=0.6, seed=188, bevel=0.0)
    P["core"] = K.surface("thunder_core", rgb(40, 40, 52), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8, seed=189, bevel=0.0)
    P["cord"] = K.surface("thunder_cord", rgb(240, 190, 60), 0.5, 0.45, bump="wrap", bump_strength=0.7, groove=rgb(80, 46, 10), groove_amount=0.8, wear=0.7, dirt=0.6, seed=190, bevel=0.0, fine=0.5)
    return P


def bolt_rows(pts, w0, w1, z=0.0):
    """Rows (left, right) along a polyline in the xy plane, width tapering w0 -> w1 (a flat lightning strip)."""
    P_ = [V(p) for p in pts]
    n = len(P_)
    rows = []
    for i, p in enumerate(P_):
        t = i / max(n - 1, 1)
        tan = (P_[min(i + 1, n - 1)] - P_[max(i - 1, 0)])
        tan.z = 0.0
        tan.normalize()
        s = V((-tan.y, tan.x, 0.0))
        w = (w0 + (w1 - w0) * t) * 0.5
        rows.append((p - s * w + V((0, 0, z)), p + s * w + V((0, 0, z))))
    return rows


def zig(p0, p1, k, amp, rr, z=0.0):
    """A jagged line from p0 to p1 with k kinks of about amp cm (xy)."""
    a, b = V(p0), V(p1)
    d = (b - a)
    s = V((-d.y, d.x, 0.0)).normalized()
    out = [a]
    for i in range(1, k + 1):
        t = i / (k + 1)
        out.append(a + d * t + s * (amp * (1 if i % 2 else -1) * rr.uniform(0.6, 1.2)))
    out.append(b)
    return [V((p.x, p.y, z)) for p in out]


def thunder_god_sword(opts):
    begin(opts)
    weights(blade=1.35, blade_glow=0.9, wings=1.1, bolts=0.8, crystal=0.9, rods=0.9, grip=1.0, star=0.9)
    P = thunder_palette()
    rr = K.rng(5808)
    ymid = 31.0
    KINKS = [(32, 0.0), (58, 0.0), (72, 5.0), (86, -3.0), (101, 5.5), (116, -2.5), (131, 3.5), (151, 0.0)]

    def xc(y):
        return K.interp(KINKS, y)

    def hw(y):
        return K.interp([(32, 5.4), (40, 6.4), (72, 6.0), (110, 5.2), (130, 4.0), (142, 2.4), (151, 0.4)], y)
    # ---------------------------------------------------------------- blade: the whole body is one bolt: the centre line zigzags at sharp kinks, the edges bite in at every kink, the tip forks
    ks = [k[0] for k in KINKS]
    stL = [(y, hw(y)) for y in sorted(set(ks + [32, 40, 72, 110, 130, 142, 151]))]
    notL = [(y + 3.0, 0.9, 3.0, "tooth") for y in (58, 72, 86, 101, 116)]
    notR = [(y - 3.0, 0.9, 3.0, "tooth") for y in (72, 86, 101, 116, 131)]
    L = K.blade_edge(-1, stL, 32, 151, notches=notL, jitter=0.05, res=6.0, seed=191, tip=(-0.4, 150.5))
    R = K.blade_edge(1, stL, 32, 151, notches=notR, jitter=0.05, res=6.0, seed=192, tip=(0.8, 149.0))
    L = [V((p.x + xc(p.y), p.y, 0)) for p in L]
    R = [V((p.x + xc(p.y), p.y, 0)) for p in R]
    K.blade("blade", P["steel"], L, R, thick=lambda t: 1.5 * (1.0 - 0.45 * t ** 2.0), fuller=0.22, fuller_depth=0.55, seed=3, wob=0.04, bevel_w=0.3)
    for s_ in (1, -1):
        rowsI = []
        for y in sorted(set(list(range(40, 148, 5)) + [58, 72, 86, 101, 116, 131])):
            c = V((xc(y), y, 0.0))
            w = 0.7 * (1 - 0.45 * (y - 40) / 108.0)
            rowsI.append((c - V((w, 0, 0)), c + V((w, 0, 0))))
        K.strip("inlay%d" % s_, rowsI, P["bolt"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: 1.05 * (1 - 0.25 * t), part="blade_glow", up=(0, 0, s_))
    # the fork: two spare tines of the bolt branch off at the kinks (left and right differ), each a flat lightning strip with a real thickness
    for i, (y0, sx, ln, ang) in enumerate(((101, 1, 22.0, 52), (116, -1, 20.0, 48), (86, -1, 15.0, 58), (131, 1, 14.0, 40))):
        x0 = xc(y0) + sx * (hw(y0) - 0.8)
        a = math.radians(ang)
        end = V((x0 + sx * math.sin(a) * ln, y0 + math.cos(a) * ln, 0))
        pts = zig(V((x0, y0, 0)), end, 2, 1.4, rr)
        K.strip("tine%d" % i, bolt_rows(pts, 3.2, 0.3), P["steel"], (-1.0, -0.3, 0.3, 1.0), (0.0, 1.0, 1.0, 0.0), 0.9, part="blade_tines", up=(0, 0, 1), angle=50)
    # ---------------------------------------------------------------- guard: a navy hub and two wings of gold lightning (three bolts a side, never twins), a magic crystal standing on each face
    hub = [(-5.8, ymid - 2.4), (-3.4, ymid - 5.6), (3.4, ymid - 5.6), (5.8, ymid - 2.4), (6.4, ymid + 2.2), (3.6, ymid + 6.2), (-3.6, ymid + 6.2), (-6.4, ymid + 2.2)]
    for s_ in (1, -1):
        K.stepped_plate("hub%d" % s_, hub, 0.6 * s_, P["steel"], steps=2, step_h=0.9, inset=1.0, part="guard", up=s_, bevel_w=0.0)
        cr = K.bl.lathe("crystal%d" % s_, [(0.01, 0.0), (2.5, 0.8), (2.9, 3.0), (2.4, 6.0), (1.2, 8.4), (0.01, 9.6)], P["crystal"], sides=6, axis="z", part="crystal")
        K._finish_obj(cr, "crystal", True, 20.0)
        K.xf(cr, K.frame(V((0, ymid + 0.3, 1.2 * s_)), V((0, 0, s_)), (0, 1, 0)))
        for k in range(6):
            a = TAU * k / 6 + 0.3
            K.tapered("ccl%d_%d" % (s_, k), [V((3.5 * math.cos(a), ymid + 0.3 + 3.5 * math.sin(a), 1.2 * s_)), V((3.7 * math.cos(a), ymid + 0.3 + 3.7 * math.sin(a), 4.0 * s_)), V((2.4 * math.cos(a), ymid + 0.3 + 2.4 * math.sin(a), 6.0 * s_))],
                      0.55, 0.12, P["gold"], sides=4, sub=1, part="crystal_claws")
    wings = {1: [(5.0, 1.0, 6, 10), (13.0, 7.5, 4, 8), (20.0, 5.0, 3, 6)], -1: [(5.0, 1.5, 5, 9), (12.0, 8.5, 4, 8), (18.0, 2.0, 3, 6)]}
    for sx, bolts_ in wings.items():
        for k, (x1, dy, wlen, width) in enumerate(bolts_):
            # three bolts per wing: an upper sweep, a middle spear, a lower hook
            ends = {0: (sx * 31.0, ymid + 12.0), 1: (sx * 34.0, ymid - 2.0), 2: (sx * 26.0, ymid - 15.0)}
            if sx < 0:
                ends = {0: (sx * 27.0, ymid + 14.0), 1: (sx * 30.0, ymid - 1.0), 2: (sx * 23.0, ymid - 13.0)}
            e = ends[k]
            st = (sx * 5.0, ymid + (3.0 - 3.0 * k))
            pts = zig(V((st[0], st[1], 0)), V((e[0], e[1], 0)), 4 + (k == 1), 3.2 + k * 0.5, rr)
            for s_ in (1,):
                K.strip("wing%d_%d" % (sx, k), bolt_rows(pts, 11.0 - 2.0 * k, 0.5), P["gold"], (-1.0, -0.35, 0.35, 1.0), (0.0, 1.0, 1.0, 0.0), 1.3 - 0.15 * k, part="wings", up=(0, 0, 1), angle=50)
    # two lightning rods standing on the guard, a copper-like coil round each
    for sx, (x, top, ph) in ((1, (10.5, 56.0, 0.0)), (-1, (-9.0, 50.0, 1.0))):
        K.tapered("rod%d" % sx, [V((x, ymid + 4.0, 0.0)), V((x * 1.04, ymid + (top - ymid) * 0.5, 0.2)), V((x * 1.08, top, 0.0))], 0.8, 0.14, P["rod"], sides=6, sub=2, part="rods")
        K.spring("coil%d" % sx, V((x * 1.01, ymid + 7.0, 0.0)), V((x * 1.03, ymid + 20.0, 0.0)), 1.5, 0.28, 6, P["gold"], sides=4, part="rods", sub=6)
        K.blob("rodtip%d" % sx, (x * 1.08, top + 0.8, 0.0), (0.9, 1.2, 0.9), P["bolt"], seed=sx + 3, lump=0.05, rings=4, segs=6, part="rod_tips")
    # ---------------------------------------------------------------- grip and the nether-star pommel
    K.grip_wrapped("grip", 11.0, 26.5, 2.0, P["core"], P["wrap"], turns=7.0, cord=0.38, bands=(11.8, 18.5, 25.6), band_mat=P["gold"], swell=0.14, part="grip", seed=5)
    star = []
    for i in range(16):
        a = TAU * i / 16 + 0.2
        r = 8.0 if i % 2 == 0 else 3.4
        star.append((r * math.cos(a), 3.0 + r * math.sin(a)))
    for s_ in (1, -1):
        K.stepped_plate("star%d" % s_, star, 0.2 * s_, P["star"], steps=3, step_h=0.55, inset=0.5, part="star", up=s_, bevel_w=0.0)
    ring2 = K.bl.lathe("star_seat", [(1.6, 9.6), (2.8, 9.2), (3.0, 10.4), (2.2, 11.0), (1.6, 10.8)], P["gold_dark"], sides=10, part="pommel")
    K._finish_obj(ring2, "pommel", True, 45.0)
    # ---------------------------------------------------------------- loose bolts arcing round the blade (bounded: 5)
    for i, (x, y0, y1, z) in enumerate(((-13.0, 56.0, 98.0, 4.0), (14.0, 84.0, 124.0, -5.0), (-11.0, 110.0, 142.0, -4.0), (13.0, 44.0, 74.0, 5.5), (-15.0, 80.0, 104.0, -6.0))):
        pts = zig(V((x, y0, 0)), V((x + rr.uniform(-3, 3), y1, 0)), 4, 2.6, rr, z=z)
        K.strip("arc%d" % i, bolt_rows(pts, 1.8, 0.2), P["bolt"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.5, part="bolts", up=(0, 0, 1), angle=50)
    return K.quad_estimate()


# ================================================================ driver
BUILDERS = {}


def run(only, trial, beauty, quick, samples):
    entries = {}
    for key in only:
        fn, mesh_id, form = BUILDERS[key]
        t0 = time.time()
        q = fn({"samples": samples})
        print("   parts:", K.quad_table(14))
        print("[%s] built geometry: ~%d body quads in %.1f s" % (mesh_id, q, time.time() - t0))
        if beauty:
            os.makedirs(beauty, exist_ok=True)
            for view in ("three_quarter", "front"):
                K.beauty(os.path.join(beauty, "%s_%s.png" % (key, view)), view=view, samples=max(8, samples // 2), size=(560, 900))
        if not quick:
            finish(mesh_id, trial=trial)
            entries[mesh_id] = {"weapon": BASE, "form": form, "class": "longsword"}
            if not trial:                                           # each finished form is written at once, so an interrupted run loses nothing
                K.write_manifest(MANIFEST, {mesh_id: entries[mesh_id]})


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--bake", type=int, default=2048)
    a = ap.parse_args(argv)
    THREADS[0] = max(1, min(2, a.threads))
    BAKE_PX[0] = a.bake
    run(a.only.split(","), a.trial, a.beauty, a.quick, a.samples)


BUILDERS["flash"] = (flash_sword, "legendary_sword__flash_sword", "flash_sword")
BUILDERS["wind"] = (wind_king_katana, "legendary_sword__wind_king_katana", "wind_king_katana")
BUILDERS["meteor"] = (meteor_sword, "legendary_sword__meteor_sword", "meteor_sword")
BUILDERS["assassin"] = (assassin_sword, "legendary_sword__assassin_sword", "assassin_sword")
BUILDERS["tortoise"] = (spirit_tortoise_sword, "legendary_sword__spirit_tortoise_sword", "spirit_tortoise_sword")
BUILDERS["carapace"] = (spirit_tortoise_carapace_sword, "legendary_sword__tortoise_carapace_sword", "tortoise_carapace_sword")
BUILDERS["hundred"] = (hundred_sword, "legendary_sword__hundred_sword", "hundred_sword")
BUILDERS["thunder"] = (thunder_god_sword, "legendary_sword__thunder_god_sword", "thunder_god_sword")

if __name__ == "__main__":
    main(sys.argv[1:])

"""The pipeline and the geometry kit of the God melee (a) upgrade.

The upgraded mesh of an item = the OLD builder of tools/relics/new197_god*.py (every old part stays) + the new parts an `extras(m, ctx)` function adds before the UVs are laid
out, drawn on a 1024 skin instead of 512 (a skin of 4 times the texels), plus the post painters (`post(m, atlas, ctx)`: the new parts' glow, the engraved detail, the second emissive
map). The old builder is run with three names patched (new_atlas, lay_and_paint, finish) so that nothing of the shipped assets is touched: the results go to
weapon_meshes/up/<id>.txt and textures/entity/weapon/up/<id>*.png. A second run with `lod=True` writes the low-detail mesh (<id>_lod) the renderer draws from far away.
"""
import contextlib
import math
import re
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
RELICS = os.path.abspath(os.path.join(HERE, "..", "..", "relics"))
sys.path.insert(0, RELICS)
sys.path.insert(0, HERE)
import common as C                                                     # noqa: E402
import mesh_lib as ml                                                  # noqa: E402
import wave1_models as w                                               # noqa: E402
import new197_lib as L                                                 # noqa: E402
import new197_kit2 as K                                                # noqa: E402
import new197_god as G1                                                # noqa: E402
import new197_god2 as G2                                               # noqa: E402
import new197_ranged as RG                                             # noqa: E402

CUR = None                                                             # the build in progress (Ctx)


class Ctx:
    """State of one build run."""

    def __init__(self, item_id, extras, post, lod, size, ppu_max):
        self.item_id, self.extras, self.post, self.lod = item_id, extras, post, lod
        self.size, self.ppu_max = size, ppu_max
        self.tube_cap = None
        self.detail = 0.4 if lod else 1.0                              # tessellation factor of the new parts
        self.m = None
        self.atlas = None
        self.orbits = []                                               # [(layer, pivot, axis, deg per tick, phase deg, glow flag)]
        self.veins = None                                              # float32 RGBA like atlas.glow: the second emissive map
        self.base = C.read_mesh(C.mesh_path(item_id))["head"]         # the place / glow / grip lines of the shipped mesh
        self.result = {}
        self.rig = None


def S(n, lo=3):
    """A tessellation count scaled for the level of detail of the running build."""
    return max(lo, int(round(n * (CUR.detail if CUR else 1.0))))


# ================================================================ a denser skin packer (MaxRects, best short side fit) for the upgraded skins
def pack_maxrects(sizes, W, H, pad):
    """Packs (w, h) boxes into W x H with `pad` px between and round the edge: MaxRects with the best-short-side-fit rule, tallest first. Returns [(x, y)] or None when they do not fit.
    The shelf packer of mesh_lib wastes the space beside long thin strips (a staff shaft, a spear haft); this one gives the same atlas a larger px-per-cm."""
    n = len(sizes)
    order = sorted(range(n), key=lambda i: (-max(sizes[i]), -min(sizes[i])))
    free = [(pad, pad, W - 2 * pad, H - 2 * pad)]
    pos = [None] * n
    for i in order:
        w, h = sizes[i]
        best = None
        for (fx, fy, fw, fh) in free:
            if w <= fw and h <= fh:
                key = (min(fw - w, fh - h), max(fw - w, fh - h), fy, fx)
                if best is None or key < best[0]:
                    best = (key, fx, fy)
        if best is None:
            return None
        _k, x, y = best
        pos[i] = (x, y)
        bx0, by0, bx1, by1 = x, y, x + w + pad, y + h + pad
        new = []
        for (fx, fy, fw, fh) in free:
            if bx0 >= fx + fw or bx1 <= fx or by0 >= fy + fh or by1 <= fy:
                new.append((fx, fy, fw, fh))
                continue
            if bx0 > fx:
                new.append((fx, fy, bx0 - fx, fh))
            if bx1 < fx + fw:
                new.append((bx1, fy, fx + fw - bx1, fh))
            if by0 > fy:
                new.append((fx, fy, fw, by0 - fy))
            if by1 < fy + fh:
                new.append((fx, by1, fw, fy + fh - by1))
        # drop rectangles contained in another
        free = [r for k, r in enumerate(new) if not any(j != k and new[j][0] <= r[0] and new[j][1] <= r[1] and new[j][0] + new[j][2] >= r[0] + r[2] and new[j][1] + new[j][3] >= r[1] + r[3] and (new[j] != r or j < k) for j in range(len(new)))]
    return pos


# ================================================================ patched names of the old builders
def _new_atlas(size=512):
    return ml.Atlas(CUR.size)


def _lay_and_paint(m, atlas, along=None):
    CUR.m, CUR.atlas = m, atlas
    if CUR.extras:
        CUR.extras(m, CUR)
    if CUR.lod:
        lod_reduce(m, CUR)
    atlas.layout(m, ppu_max=CUR.ppu_max)
    atlas.paint(m, along)
    CUR.veins = np.zeros_like(atlas.glow)


def _finish(item_id, m, atlas, cls=None, glow=None, **kw):
    CUR.m, CUR.atlas = m, atlas
    write_outputs(CUR, m, atlas)


def _finish_crossbow(item_id, m, atlas, rig, glow=None, real_m=1.1):
    CUR.m, CUR.atlas, CUR.rig = m, atlas, rig
    write_outputs(CUR, m, atlas, rig=rig)


def _wrap_tube(orig, cap):
    """The old builders' long helix tubes (160 points round a spear shaft) drawn with at most `cap` points: the same line, a quarter of the quads."""
    def tube(m, name, material, pts, r0, r1=None, sides=8, **kw):
        if len(pts) > cap:
            k = (len(pts) - 1) / float(cap - 1)
            pts = [pts[min(len(pts) - 1, int(round(i * k)))] for i in range(cap)]
        return orig(m, name, material, pts, r0, r1, sides, **kw)
    return tube


@contextlib.contextmanager
def patched(ctx):
    global CUR
    CUR = ctx
    saved = []
    for mod in (G1, G2):
        for name, fn in (("new_atlas", _new_atlas), ("lay_and_paint", _lay_and_paint), ("finish", _finish)):
            saved.append((mod, name, getattr(mod, name)))
            setattr(mod, name, fn)
    if ctx.tube_cap:
        for mod in (G1, G2):
            saved.append((mod, "tube", mod.tube))
            mod.tube = _wrap_tube(mod.tube, ctx.tube_cap)
    saved.append((RG, "finish_crossbow", RG.finish_crossbow))
    RG.finish_crossbow = _finish_crossbow
    saved.append((ml, "pack_shelf", ml.pack_shelf))
    ml.pack_shelf = pack_maxrects
    lod_saved = []
    if ctx.lod:
        lod_saved = lod_patch()
    try:
        yield
    finally:
        for mod, name, fn in saved:
            setattr(mod, name, fn)
        for obj, name, fn in lod_saved:
            setattr(obj, name, fn)
        CUR = None


# ================================================================ the level of detail of the OLD parts (a run with lod=True)
def lod_patch():
    """Clamps the tessellation of everything the old builders make (round sections, lathes, sweeps, gems, studs) for the low-detail mesh."""
    saved = []

    def put(obj, name, fn):
        saved.append((obj, name, getattr(obj, name)))
        setattr(obj, name, fn)

    o_round, o_lathe, o_sweep, o_collar, o_gem, o_stud, o_ell = ml.round, ml.lathe, ml.sweep, ml.collar, ml.gem, ml.stud, w.ellipsoid

    def rnd(r, n=12, start=0.0):
        return o_round(r, max(4, min(n, 6)), start)

    def lathe(part, axis, origin, profile, sides=12, caps=(True, True), start=0.0, squash=1.0):
        prof = list(profile)
        if len(prof) > 5:
            idx = sorted(set([0, len(prof) - 1] + list(np.linspace(0, len(prof) - 1, 5).astype(int))))
            prof = [prof[i] for i in idx]
        return o_lathe(part, axis, origin, prof, max(4, min(sides, 6)), caps, start, squash)

    def sweep(part, path, section, scales=None, caps=(True, True), up=None):
        P = list(path)
        k = max(1, int(math.ceil((len(P) - 1) / 7.0)))
        if k > 1:
            idx = list(range(0, len(P), k))
            if idx[-1] != len(P) - 1:
                idx.append(len(P) - 1)
            if not callable(scales) and scales is not None:
                scales = [scales[i] for i in idx]
            P = [P[i] for i in idx]
        return o_sweep(part, P, section, scales, caps, up)

    def collar(part, s, r, h, lip=0.0, sides=16, axis="y", origin=(0, 0, 0), oval=1.0, caps=(True, True)):
        return o_collar(part, s, r, h, 0.0, max(4, min(sides, 6)), axis, origin, oval, caps)

    def gem(part, center, r, long=1.4, sides=6, axis="y"):
        return o_gem(part, center, r, long, 4, axis)

    def stud(part, pos, r, h, normal=(0, 0, 1), sides=8, dome=0.55):
        return o_stud(part, pos, r, h, normal, 4, dome)

    def ell(part, center, rx, ry, rz, sides=14, stacks=10):
        return o_ell(part, center, rx, ry, rz, 6, 4)

    o_part = ml.Mesh.part

    def part(self, name):
        for p in self.parts:
            if p.name == name:
                return p
        stub = ml.Part(name)                        # a part the low-detail mesh dropped: painters find nothing to paint
        stub.V = [np.zeros(3)]
        return stub

    put(ml.Mesh, "part", part)
    put(ml, "round", rnd)
    put(ml, "circle", rnd)
    put(ml, "lathe", lathe)
    put(ml, "sweep", sweep)
    put(ml, "collar", collar)
    put(ml, "gem", gem)
    put(ml, "stud", stud)
    put(w, "ellipsoid", ell)
    return saved


def lod_reduce(m, ctx):
    """Drops parts of the mesh until it fits the low-detail budget (part faces; the written file adds the glow-layer copies). Order: the added detail first, then the small old
    parts, then (last) the item's own keep list; the essential parts (blade, shaft, head ...) are never dropped."""
    budget = ctx.lod_budget
    essential = ctx.keep_words
    soft = getattr(ctx, "soft_keep", ())

    def key(p):
        tier = (0 if p.name.startswith("up_") else 1) + (2 if any(k in p.name for k in soft) else 0)
        size = (np.ptp(np.array(p.V), axis=0).max() if p.V else 0.0) * (len(p.F) ** 0.35)
        return (tier, size)

    for _ in range(400):
        if sum(len(p.F) * (2.0 if p.glow else 1.4) for p in m.parts) <= budget * 1.4:
            break
        cand = [p for p in m.parts if not re.match(essential, p.name)]
        if not cand:
            break
        m.parts.remove(min(cand, key=key))


# ================================================================ writing the results
def _save_png(arr, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(path, optimize=True)


def write_outputs(ctx, m, atlas, rig=None):
    """Runs the post painters, places the mesh exactly where the shipped mesh sits, writes <id>.txt (or <id>_lod.txt), the skin, the glow and the veins map."""
    if ctx.post:
        ctx.post(m, atlas, ctx)
    suffix = "_lod" if ctx.lod else ""
    base = ctx.base
    m.place(*[float(x) for x in base["place"]])
    g = base.get("glow", ["FFFFFF", "0.15", "0.45", "1.0"])
    cfg = ctx.glow_cfg if getattr(ctx, "glow_cfg", None) else (g[0], float(g[1]), float(g[2]), float(g[3]))
    m.set_glow(*cfg)
    os.makedirs(C.UP_MESH_DIR, exist_ok=True)
    path = os.path.join(C.UP_MESH_DIR, ctx.item_id + suffix + ".txt")
    comment = "generated by tools/visual_upgrade/god_melee_a (%s)" % ("low detail" if ctx.lod else "full detail")
    if rig is not None:
        import ranged_meshes as rm
        paths = rm.emit_states(m, rig, "crossbow", ctx.item_id + suffix, out_dir=C.UP_MESH_DIR, atlas=atlas, normals="vertex")
        for suf, p in list(paths.items()):
            if ctx.lod and suf:                                       # the low-detail mesh is drawn for every draw state: only the rest mesh is kept
                os.remove(p)
                paths.pop(suf)
                continue
            _post_header(p, ctx)
        ctx.result["paths"] = paths
        n = 0
    else:
        n = m.write_txt(path, atlas, normals="vertex", comment=comment)
        _post_header(path, ctx)
        ctx.result["paths"] = {"": path}
    skin = os.path.join(C.UP_SKIN_DIR, ctx.item_id + suffix + ".png")
    _save_png(atlas.img, skin)
    _save_png(atlas.glow, os.path.join(C.UP_SKIN_DIR, ctx.item_id + suffix + "_glow.png"))
    if not ctx.lod and ctx.veins is not None:
        _save_png(ctx.veins, os.path.join(C.UP_SKIN_DIR, ctx.item_id + "_veins.png"))
    ctx.result.update({"quads": n, "ppu": atlas.ppu})
    print("%s%s: %d parts, atlas %d px at %.1f px/cm, quads %s" % (ctx.item_id, suffix, len(m.parts), atlas.size, atlas.ppu or 0, ctx.result.get("quads")))


def _post_header(path, ctx):
    """Copies the `grip` / `orient` lines of the shipped mesh when the new one has none, and adds the `orbit` lines of the orbiting groups after the glow line."""
    lines = open(path, encoding="utf-8").read().split("\n")
    head = ctx.base
    out = []
    have = {l.split()[0] for l in lines if l.split()}
    for i, l in enumerate(lines):
        out.append(l)
        t = l.split()
        if t and t[0] == "place":
            if "grip" not in have and "grip" in head:
                out.append("grip " + " ".join(head["grip"]))
            if "orient" in head and "orient" not in have:
                out.append("orient " + " ".join(head["orient"]))
    if ctx.orbits and not ctx.lod:
        extra = []
        for (layer, pivot, axis, speed, phase, glow) in ctx.orbits:
            extra.append("orbit %d %.3f %.3f %.3f %.4f %.4f %.4f %.3f %.2f %d" % (layer, *pivot, *axis, speed, phase, 1 if glow else 0))
        gi = next((k for k, l in enumerate(out) if l.startswith("glow ")), 2)
        out[gi + 1:gi + 1] = extra
    with open(path, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(out))


def run(item_id, builder, extras, post, lod=False, lod_budget=None, keep_words=r"^(blade|head|shaft|haft|stock|limb_[lr]?|prong_[a-z]|face|rim|grip|core|strand_\d|hub)$", size=None, ppu_max=64.0, glow_cfg=None, tube_cap=None, soft_keep=()):
    """Builds one item (or its low-detail mesh) through the old builder. Returns the context (result paths, orbit table, the atlas)."""
    ctx = Ctx(item_id, extras, post, lod, size or (256 if lod else 1024), 24.0 if lod else ppu_max)
    ctx.lod_budget = lod_budget or 10 ** 9
    ctx.keep_words = keep_words
    ctx.glow_cfg = glow_cfg
    ctx.tube_cap = tube_cap
    ctx.soft_keep = soft_keep
    with patched(ctx):
        builder()
    return ctx


# ================================================================ geometry kit (every function adds named parts to the mesh)
def mk(m, name, material="steel", base=None, glow=False, smooth=40, layer=0, uv_mode=None, wear=0.5):
    p = ml.Part(name, layer=layer, material=material, base=base, glow=glow, smooth=smooth, uv_mode=uv_mode, wear=wear)
    m.add(p)
    return p


def V(*a):
    return np.array(a, float)


def circle_pts(c, R, normal, n, a0=0.0, a1=360.0, up=None):
    """Points of a circle (or arc, degrees) of radius R about c in the plane perpendicular to normal."""
    _a, e1, e2 = ml._frame(normal, up)
    c = np.asarray(c, float)
    return [c + R * (math.cos(math.radians(a0 + (a1 - a0) * i / n)) * e1 + math.sin(math.radians(a0 + (a1 - a0) * i / n)) * e2) for i in range(n + 1)]


def tube3(m, name, pts, r0, r1=None, material="gold", base=None, glow=False, sides=5, smooth=55, layer=0, closed=False, tip=False, wear=0.4):
    """A round tube along a 3D polyline."""
    p = mk(m, name, material, base, glow, smooth, layer, uv_mode="loft", wear=wear)
    r1 = r0 if r1 is None else r1
    sd = max(3, S(sides, 3))

    def sc(t):
        if tip and t > 0.97:
            return (0.0, 0.0)
        r = r0 + (r1 - r0) * t
        return (r, r)
    ml.sweep(p, [tuple(q) for q in pts], ml.circle(1.0, sd), scales=sc, caps=(not closed, not closed))
    return p


def ring3d(m, name, c, R, r, normal, material="gold", base=None, glow=False, n=28, sides=5, layer=0, smooth=55, a0=0.0, a1=360.0, up=None):
    """A torus-like ring (or an arc of one) about c, in the plane perpendicular to normal."""
    pts = circle_pts(c, R, normal, S(n, 8), a0, a1, up)
    return tube3(m, name, pts, r, r, material, base, glow, sides, smooth, layer, closed=(a1 - a0 >= 359.9))


def leaf(m, name, p0, p1, half_w, half_t, up=(0, 0, 1), material="gold", base=None, glow=False, smooth=42, layer=0, belly=0.35, bevel=0.5, wear=0.5, tip=True):
    """A flat leaf / feather / blade from p0 to p1: widest at `belly` of its length, closes to a point."""
    p = mk(m, name, material, base, glow, smooth, layer, wear=wear)
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    L = float(np.linalg.norm(p1 - p0))
    st = [(0.0, 0.25 * half_w, 0.6 * half_t), (L * belly * 0.5, 0.8 * half_w, half_t), (L * belly, half_w, half_t), (L * 0.75, 0.62 * half_w, 0.8 * half_t)]
    st.append((L, 0.0, 0.0) if tip else (L, 0.25 * half_w, 0.4 * half_t))
    ml.loft(p, p1 - p0, p0, st, ml.bevelled(2.0, 2.0, bevel), up=up)
    return p


def shard(m, name_or_part, c, r, long=1.6, axis="y", sides=5, material="crystal", base=(180, 140, 255), glow=True, smooth=25, layer=0):
    """A cut crystal shard (a gem) pointing along `axis` (a name or a vector). Pass an existing Part to add to it."""
    p = name_or_part if not isinstance(name_or_part, str) else mk(m, name_or_part, material, base, glow, smooth, layer)
    ml.gem(p, tuple(c), r, long, S(sides, 4), axis)
    return p


def spike3(part, base_pt, tip_pt, r, sides=5, up=None):
    b, t = np.asarray(base_pt, float), np.asarray(tip_pt, float)
    L = float(np.linalg.norm(t - b))
    ml.loft(part, t - b, b, [(0.0, 1.0, 1.0), (L * 0.98, 0.0, 0.0)], ml.circle(r, S(sides, 3)), up=up)


def blocks_ring(m, name, c, R, n, wd, ht, depth, material="gold", base=None, glow=False, axis_up=(0, 1, 0), smooth=30, layer=0):
    """n small raised blocks standing radially round a ring (runes on a band, teeth of a crown); width wd along the ring, height ht along axis_up, standing out by depth."""
    p = mk(m, name, material, base, glow, smooth, layer)
    c = np.asarray(c, float)
    for i in range(S(n, 4)):
        a = 2 * math.pi * i / S(n, 4)
        d = V(math.cos(a), 0, math.sin(a))
        ml.loft(p, d, c + d * (R - depth * 0.3), [(0.0, wd / 2, ht / 2), (depth * 0.7, wd / 2, ht / 2), (depth, wd / 2 * 0.7, ht / 2 * 0.7)], ml.octagon(2.0, 2.0, 0.35), up=axis_up)
    return p


def scroll2d(c, plane_axes, turns, r0, r1, n, phase=0.0, lift=0.0):
    """Points of a spiral (a filigree scroll) round c in the plane spanned by two unit vectors (e1, e2): radius r0 -> r1 over `turns` turns; lift moves it along e1 x e2."""
    e1, e2 = (np.asarray(a, float) for a in plane_axes)
    nrm = np.cross(e1, e2)
    c = np.asarray(c, float)
    out = []
    for i in range(n + 1):
        t = i / n
        a = phase + 2 * math.pi * turns * t
        r = r0 + (r1 - r0) * t
        out.append(c + r * (math.cos(a) * e1 + math.sin(a) * e2) + nrm * lift * t)
    return out


def orbit_part(ctx, m, name, k, pivot, axis, speed, phase=0.0, glow=True, material="gold", base=None, smooth=45):
    """A part of orbiting group k (drawn rotating about `pivot` round `axis`, `speed` degrees per tick). Returns the Part (layer 3 + k)."""
    layer = 3 + k
    if ctx.lod:                                                       # the low-detail mesh has no orbiting groups: a part that is never added to the mesh
        return ml.Part(name, layer=layer, material=material, base=base, glow=glow, smooth=smooth)
    if not any(o[0] == layer for o in ctx.orbits):
        ctx.orbits.append((layer, tuple(float(x) for x in pivot), tuple(float(x) for x in axis), float(speed), float(phase), bool(glow)))
    return mk(m, name, material, base, glow, smooth, layer)


def chain_links(m, name, p0, p1, n, r, material="iron", base=None, up=(0, 0, 1), sag=0.0):
    """A chain: n small links alternately turned a quarter, from p0 to p1 (optionally sagging)."""
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    d = p1 - p0
    L = float(np.linalg.norm(d))
    ax = d / max(L, 1e-9)
    _a, e1, e2 = ml._frame(ax, up)
    n = max(2, S(n, 3))
    p = mk(m, name, material, base, False, 50, uv_mode="loft")
    for i in range(n):
        t = (i + 0.5) / n
        c = p0 + d * t + V(0, -sag * math.sin(math.pi * t), 0)
        nrm = e1 if i % 2 == 0 else e2
        pts = circle_pts(c, r, nrm, 8, up=ax)
        ml.sweep(p, [tuple(x) for x in pts], ml.circle(1.0, 4), scales=lambda t_, r=r: (r * 0.3, r * 0.3), caps=(False, False))
    return p


def tuft(m, name, c, n, length, spread, material="cloth", base=(200, 60, 60), seed=1, glow=False, down=True):
    """A tassel / tuft of flat strands hanging from c."""
    rng = np.random.default_rng(seed)
    p = mk(m, name, material, base, glow, 40, wear=0.3)
    c = np.asarray(c, float)
    for i in range(S(n, 2)):
        a = 2 * math.pi * (i + rng.random() * 0.6) / S(n, 2)
        dx, dz = math.cos(a) * spread, math.sin(a) * spread
        tip = c + V(dx, (-1 if down else 1) * length * (0.8 + 0.4 * rng.random()), dz)
        ml.loft(p, tip - c, c, [(0.0, 0.5, 0.18), (length * 0.5, 0.42, 0.14), (float(np.linalg.norm(tip - c)), 0.0, 0.0)], ml.bevelled(2.0, 2.0, 0.5), up=(0, 0, 1))
    return p


def ribbon(m, name, p0, length, width, wave, phase=0.0, material="cloth", base=(220, 220, 230), n=9, axis=(0, -1, 0), side=(1, 0, 0), glow=False, wear=0.3, thick=0.07):
    """A flat strip (a pennant, a cape, a silk) hanging from p0 along `axis`, rippling sideways with amplitude `wave`."""
    p0 = np.asarray(p0, float)
    ax = np.asarray(axis, float)
    ax /= np.linalg.norm(ax)
    sd = np.asarray(side, float)
    n = S(n, 4)
    pts = [tuple(p0 + ax * length * i / n + sd * wave * math.sin(phase + i * 0.9) * (i / n)) for i in range(n + 1)]
    p = mk(m, name, material, base, glow, 55, wear=wear)
    ml.sweep(p, pts, ml.rect_ring(2.0, 2.0), scales=lambda t: (width * (0.5 + 0.5 * (1 - t * 0.5)), thick), caps=(True, True), up=(0, 0, 1))
    return p


def hex_ring(m, name, c, R, r, normal, material="silver", base=None, glow=False, layer=0, up=None, sides=4):
    """A hexagonal ring (a gyroscope ring) about c in the plane perpendicular to normal."""
    pts = circle_pts(c, R, normal, 6, up=up)
    return tube3(m, name, pts, r, r, material, base, glow, sides, 30, layer, closed=True)

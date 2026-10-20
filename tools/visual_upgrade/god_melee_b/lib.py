"""The pipeline of the visual-upgrade slice `god_melee_b` (release 1.102.0): it re-runs the ORIGINAL builder of each weapon (tools/rift/pocket_weapon_models.py,
tools/relics/wave30_m_paradox.py) with three hooks, so the upgraded weapon is the old weapon plus new parts, never a different weapon:

  1. the skin atlas is 1024 (was 512) and, just before the UV layout, `extras.geometry_<id>(m)` adds the new parts (filigree, crystals, rings, rivets ...);
  2. the builder's own painting runs unchanged on every part (old and new); its `finish` is captured instead of written (the old files stay as they are);
  3. afterwards the slice paints on top (`detail_pass`: grain, ambient occlusion, bevel rim, scratches; the item's own engravings and the second emissive map)
     and writes NEW files only, under the sub-folder `up/`:
        assets/nusmp/weapon_meshes/up/<id>.txt            the full-detail mesh (place / grip copied from the old mesh, `orient manual`)
        assets/nusmp/weapon_meshes/up/<id>_lod.txt        the level-of-detail mesh (vertex clustering, <= 14% of the quads)
        assets/nusmp/weapon_meshes/up/<id>_orbit.txt      one piece of the orbiting parts (drawn N times by the renderer)
        assets/nusmp/textures/entity/weapon/up/<id>.png, _glow.png (core glow, additive), _glow2.png (energy veins, translucent emissive), <id>_orbit*.png
        assets/nusmp/visual_upgrade/<id>.json             the data table of the effects (read by client/GodMeleeBRender and client/vupgrade/godb/*)

The renderer reaches them through the mesh id `up/<id>` (see WeaponRenderer.meshId); the old mesh <id> is still the fallback when `visual.upgrade` is off.
"""
import json
import math
import os
import sys
import zlib

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
for sub in ("relics", "rift"):
    sys.path.insert(0, os.path.join(ROOT, "tools", sub))

import mesh_lib as ml                      # noqa: E402
import wave1_models as w                   # noqa: E402
import new197_lib as nl                    # noqa: E402
import new197_kit as nk                    # noqa: E402

ASSETS = ml.ASSETS
MESH_DIR = os.path.join(ASSETS, "weapon_meshes", "up")
TEX_DIR = os.path.join(ASSETS, "textures", "entity", "weapon", "up")
DATA_DIR = os.path.join(ASSETS, "visual_upgrade")
ATLAS_SIZE = 1024
LOD_FRACTION = 0.14                        # the level-of-detail mesh keeps at most this share of the body quads (the brief: <= 15% of the vertices)


# ================================================================ the harness
_OrigAtlas = ml.Atlas


class UpAtlas(_OrigAtlas):
    """The atlas of the upgrade: always `size_override` px, and the geometry hook runs once, right before the UV layout."""
    size_override = ATLAS_SIZE
    hook = None

    def __init__(self, size=1024, **kw):
        super().__init__(UpAtlas.size_override, **kw)

    def layout(self, mesh, *a, **k):
        h, UpAtlas.hook = UpAtlas.hook, None
        if h:
            h(mesh)
        return super().layout(mesh, *a, **k)


def run_old_builder(item_id, geometry_hook, size=ATLAS_SIZE):
    """Runs the original builder of the weapon with the hooks; returns {"m", "atlas", "glow"} (the builder's finish is captured, nothing of the old assets is written)."""
    cap = {}
    UpAtlas.hook = geometry_hook
    UpAtlas.size_override = size
    ml.Atlas = UpAtlas
    try:
        if item_id == "paradox":
            import wave30_m_paradox as mod
            saved = w.finish

            def fake_w(iid, m, atlas, real_m, fit=False, glow=("FFFFFF", 0.14, 0.55, 1.0), ref=None, **kw):
                cap.update(m=m, atlas=atlas, glow=glow)
            w.finish = fake_w
            try:
                mod.build()
            finally:
                w.finish = saved
        else:
            import pocket_weapon_models as pm
            saved = pm.finish

            def fake(iid, m, atlas, cls, glow=("FFFFFF", 0.16, 0.45, 1.0), **kw):
                cap.update(m=m, atlas=atlas, glow=glow)
            pm.finish = fake
            try:
                pm.BUILDERS[item_id]()
            finally:
                pm.finish = saved
    finally:
        ml.Atlas = _OrigAtlas
        UpAtlas.hook = None
    if "m" not in cap:
        raise RuntimeError("the builder of %s did not reach finish" % item_id)
    return cap


def old_header(item_id):
    """The `place` and `grip` lines of the shipped old mesh (kept so the upgraded weapon sits in the hand exactly like the old one)."""
    place, grip = None, None
    for line in open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt"), encoding="utf-8"):
        v = line.split()
        if v and v[0] == "place":
            place = [float(x) for x in v[1:6]]
        elif v and v[0] == "grip":
            grip = line.strip()
    return place, grip


# ================================================================ painting helpers
def paint_g2(atlas, g2, part, fn):
    """Runs a painter of nl.paint with the SECOND emissive map as its glow view."""
    saved = atlas.glow
    atlas.glow = g2
    try:
        nl.paint(atlas, part, fn)
    finally:
        atlas.glow = saved


def glow_part2(atlas, g2, part, rgb, strength=0.9, core=0.5):
    paint_g2(atlas, g2, part, lambda pc: w.set_glow(pc.c, rgb, strength, core=core))


def part_names(m):
    return [p.name for p in m.parts]


def hash_seed(name, k=0):
    return (zlib.crc32(name.encode()) + 7919 * k) & 0xFFFFFFFF


def detail_pass(m, atlas, amount=1.0):
    """The generic skin detail over EVERY part, old and new (the brief's 'bevel / AO / wear'): per UV island a fine grain, a dark ambient-occlusion band along the
    island edge, a light bevel rim just inside it (lit from the upper left) and a few hairline scratches. The glow skins are not touched."""
    for p in m.parts:
        if p.layer == 1 or not getattr(p, "groups", None):
            continue
        soft = p.material in ("cloth", "leather")
        for gi, g in enumerate(p.groups):
            x, y, wd, ht = g.rect
            if wd < 5 or ht < 5:
                continue
            img = atlas.img[y:y + ht, x:x + wd]
            rng = np.random.default_rng(hash_seed(p.name, gi))
            yy, xx = np.mgrid[0:ht, 0:wd].astype(np.float32)
            d = np.minimum.reduce([xx, yy, wd - 1 - xx, ht - 1 - yy])
            grain = (rng.random((ht, wd)).astype(np.float32) - 0.5) * (9.0 if soft else 15.0) * amount
            ao = np.clip(1.0 - d / 3.2, 0.0, 1.0) ** 1.5 * (0.18 if soft else 0.30) * amount
            rim = np.clip(1.0 - np.abs(d - 3.0) / 1.2, 0.0, 1.0) * (xx + yy < wd + ht) * (0.05 if soft else 0.09) * amount      # a bevel highlight on the upper-left half of the island
            col = img[..., :3]
            col += grain[..., None]
            col *= (1.0 - ao)[..., None]
            col += (255.0 - col) * rim[..., None]
            if not soft and wd * ht > 400:
                for _ in range(int(2 + wd * ht / 3000)):
                    ang = rng.uniform(0, math.pi)
                    cx, cy = rng.uniform(0, wd), rng.uniform(0, ht)
                    ln = rng.uniform(4, 12) * max(1.0, min(wd, ht) / 40)
                    t = np.abs((xx - cx) * math.sin(ang) - (yy - cy) * math.cos(ang))
                    s = ((xx - cx) * math.cos(ang) + (yy - cy) * math.sin(ang))
                    m_ = np.clip(1.0 - t / 0.7, 0, 1) * (np.abs(s) < ln)
                    col += (230.0 - col) * (m_ * 0.16 * amount)[..., None]
            np.clip(img[..., :3], 0, 255, out=img[..., :3])


# ================================================================ masks for engraving
def vine_mask(X, Y, cx, amp, period, phase, width, y0, y1):
    """A sine vine winding down a blade: |X - (cx + amp sin(2 pi (Y - y0) / period + phase))| < width, between y0 and y1."""
    c = cx + amp * np.sin(2 * math.pi * (Y - y0) / period + phase)
    return nl.sm(X - c, width) * nl.step(Y, y0, y0 + 2.0) * (1.0 - nl.step(Y, y1 - 2.0, y1))


def dot_mask(X, Y, x0, y0, dx, dy, r, nx, ny):
    """A lattice of dots."""
    gx = np.round((X - x0) / dx)
    gy = np.round((Y - y0) / dy)
    ok = (gx >= 0) & (gx < nx) & (gy >= 0) & (gy < ny)
    d = np.hypot(X - (x0 + gx * dx), Y - (y0 + gy * dy))
    return np.clip(1.0 - d / r, 0, 1) * ok


def chevron_mask(X, Y, cx, y0, y1, step_y, slope, width):
    """Nested chevrons pointing toward the tip between y0 and y1."""
    t = (Y - y0)
    k = np.round(t / step_y)
    yc = y0 + k * step_y + slope * np.abs(X - cx)
    return nl.sm(Y - yc, width) * (Y > y0 - 0.1) * (Y < y1)


def ring_mask(X, Y, cx, cy, r, width):
    return nl.sm(np.hypot(X - cx, Y - cy) - r, width)


def engrave(pc, mask, depth=0.5, rim=0.15):
    """Darkens the skin under a mask (a cut groove) with a light rim on one side."""
    nl.darken(pc, mask, depth)
    sh = np.roll(mask, 1, axis=1)
    pc.img[..., :3] += (255.0 - pc.img[..., :3]) * (np.clip(sh - mask, 0, 1) * rim)[..., None]


def lit(pc, mask, rgb, strength=0.9, core=0.5):
    """Emissive line on the CORE glow map."""
    nl.put_glow(pc, mask, rgb, strength, core)


def lit2(pc, mask, rgb, strength=0.9, core=0.5):
    """Emissive line on the pc's glow view (use inside paint_g2 for the vein map)."""
    nl.put_glow(pc, mask, rgb, strength, core)


# ================================================================ geometry helpers (new parts)
P = nl.P
V = np.array


def ring_pts(cx, y, cz, r, n=24, plane="xz", a0=0.0):
    """A closed ring of points: plane 'xz' (round the y axis), 'xy' (facing z), 'yz' (facing x)."""
    out = []
    for i in range(n + 1):
        a = a0 + 2 * math.pi * i / n
        if plane == "xz":
            out.append((cx + r * math.cos(a), y, cz + r * math.sin(a)))
        elif plane == "xy":
            out.append((cx + r * math.cos(a), y + r * math.sin(a), cz))
        else:
            out.append((cx, y + r * math.sin(a), cz + r * math.cos(a)))
    return out


def tube_ring(m, name, material, cx, y, cz, r, tube_r, n=24, plane="xz", base=None, glow=False, sides=6, smooth=60, a0=0.0):
    """A thin ring (a halo, a hoop, a gear rim) as a swept tube."""
    return nl.tube(m, name, material, ring_pts(cx, y, cz, r, n, plane, a0), tube_r, tube_r, sides=sides, base=base, glow=glow, smooth=smooth)


def gem_ring(m, name, material, cx, y, cz, r, n, gem_r, gem_len=1.3, base=None, glow=True, plane="xz", sides=4, tilt_out=True):
    """n small cut gems set on a circle (the eyes of a ring, the beads of a halo, rivets of a band). Each gem is a double cone (axis y or radial)."""
    p = P(m, name, material, base=base, glow=glow, smooth=28)
    for i in range(n):
        a = 2 * math.pi * i / n
        if plane == "xz":
            c = (cx + r * math.cos(a), y, cz + r * math.sin(a))
            ml.gem(p, c, gem_r, gem_len, sides, "y")
        elif plane == "xy":
            c = (cx + r * math.cos(a), y + r * math.sin(a), cz)
            ml.gem(p, c, gem_r, gem_len, sides, "x" if abs(math.cos(a)) > 0.7 else "y")
        else:
            c = (cx, y + r * math.sin(a), cz + r * math.cos(a))
            ml.gem(p, c, gem_r, gem_len, sides, "y")
    return p


def spike_ring(m, name, material, cx, y, cz, r0, r1, n, length, rad, base=None, glow=False, rise=0.0, sides=4, a0=0.0):
    """n spikes radiating from a circle of radius r0 to r1 (a crown, a collar of thorns)."""
    p = P(m, name, material, base=base, glow=glow, smooth=20)
    for i in range(n):
        a = a0 + 2 * math.pi * i / n
        b = (cx + r0 * math.cos(a), y, cz + r0 * math.sin(a))
        t = (cx + r1 * math.cos(a), y + rise, cz + r1 * math.sin(a))
        nl.spike(p, b, t, rad, sides)
    return p


def ribbon(m, name, material, pts, half_w, half_t=0.12, base=None, glow=False, smooth=40, up=(0, 0, 1), taper=True):
    """A flat band (cloth, a streamer, a flowing line) swept along a polyline."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth)

    def sc(t):
        k = 1.0 - 0.8 * t ** 2 if taper else 1.0
        return (half_w * k + 0.05, half_t)
    ml.sweep(p, pts, ml.bevelled(2.0, 2.0, 0.4), scales=sc, up=up)
    return p


def vine_pts(x0, x1, y0, y1, amp, turns, z=0.0, n=14, phase=0.0, axis_x=False):
    """A sine curve for filigree: from (x0, y0) to (x1, y1) swinging +-amp across the path."""
    out = []
    dx, dy = x1 - x0, y1 - y0
    L = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / L, dx / L
    for i in range(n + 1):
        t = i / n
        s = amp * math.sin(2 * math.pi * turns * t + phase) * math.sin(math.pi * t) ** 0.5
        out.append((x0 + dx * t + nx * s, y0 + dy * t + ny * s, z))
    return out


def raised(m, name, material, pts, w_, h_, base=None, glow=False, smooth=45, sides=None):
    """A raised thin line (filigree, a rune stroke) along a polyline lying on a flat face: a flat-top bar swept along pts, h_ high."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth)
    ml.sweep(p, pts, ml.bevelled(2.0, 2.0, 0.45), scales=lambda t: (w_ * (0.55 + 0.45 * math.sin(math.pi * min(1.0, max(0.0, t))) ** 0.5), h_), up=(0, 0, 1))
    return p


# ================================================================ the writers
class _BothGlow:
    """has_glow for the mesh writer: a quad is also drawn in the glow layer if EITHER emissive map has a pixel under it."""

    def __init__(self, atlas, g2):
        self.atlas, self.g2, self.k, self.size = atlas, g2, atlas.k, atlas.size

    def has_glow(self, uv, thresh=2.0):
        if self.atlas.has_glow(uv, thresh):
            return True
        k = self.k
        u0, u1 = int(max(0, math.floor(uv[:, 0].min() * k))), int(min(self.size, math.ceil(uv[:, 0].max() * k) + 1))
        v0, v1 = int(max(0, math.floor(uv[:, 1].min() * k))), int(min(self.size, math.ceil(uv[:, 1].max() * k) + 1))
        return bool(self.g2[v0:v1, u0:u1, 3].max(initial=0) > thresh)


def write_mesh(mesh_id, m, atlas, g2, place, grip_line, glow_cfg, orient_manual=True):
    """Writes weapon_meshes/up/<mesh_id>.txt in the game's format with the OLD mesh's place and grip."""
    os.makedirs(MESH_DIR, exist_ok=True)
    m.place(*place)
    m.set_glow(*glow_cfg)
    m.orient_cfg = "manual" if orient_manual else None
    path = os.path.join(MESH_DIR, mesh_id + ".txt")
    n = m.write_txt(path, _BothGlow(atlas, g2) if g2 is not None else atlas, normals="vertex", comment="generated by tools/visual_upgrade/god_melee_b (release 1.102.0)")
    lines = open(path, encoding="utf-8").read().split("\n")
    lines = [l for l in lines if not l.startswith("grip ")]
    if grip_line:
        lines.insert(2, grip_line)
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines))
    return path, n


def save_textures(mesh_id, atlas, g2, with_glow2=True):
    os.makedirs(TEX_DIR, exist_ok=True)
    out = {}
    p = os.path.join(TEX_DIR, mesh_id + ".png")
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(p, optimize=True)
    out["skin"] = p
    if atlas.glow[..., 3].max() > 0:
        p = os.path.join(TEX_DIR, mesh_id + "_glow.png")
        Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(p, optimize=True)
        out["glow"] = p
    if with_glow2 and g2 is not None and g2[..., 3].max() > 0:
        p = os.path.join(TEX_DIR, mesh_id + "_glow2.png")
        Image.fromarray(np.clip(g2, 0, 255).astype(np.uint8), "RGBA").save(p, optimize=True)
        out["glow2"] = p
    return out


# ================================================================ level of detail
def read_game_mesh(path):
    """(header lines, quads) of a game mesh file; a quad is [layer, P(4x3), UV(4x2), N(4x3)]. Only `qn` lines."""
    head, quads = [], []
    for line in open(path, encoding="utf-8"):
        s = line.strip()
        if not s:
            continue
        v = s.split()
        if v[0] == "qn":
            a = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
            quads.append([int(v[1]), a[:, :3], a[:, 3:5], a[:, 5:8]])
        elif v[0] != "#" or not s.startswith("# part"):
            head.append(s)
    return head, quads


def _canon(keys):
    """The cyclic rotation of a 4-tuple of cluster keys that starts at the smallest key (the winding is kept)."""
    i = min(range(4), key=lambda k: keys[k])
    return tuple(keys[(i + k) % 4] for k in range(4))


def decimate(quads, cell):
    """Vertex clustering at grid size `cell`: every vertex goes to the mean of its cell; quads with fewer than three distinct corners vanish; equal quads merge. The glow-layer
    twin of a kept body quad is kept too. Returns the new quads (layer 0 / 1, 2 for the glow twins)."""
    acc = {}
    for q in quads:
        for c in range(4):
            key = tuple(np.floor(q[1][c] / cell).astype(int))
            a = acc.setdefault(key, [np.zeros(3), 0])
            a[0] += q[1][c]
            a[1] += 1
    cen = {k: v[0] / v[1] for k, v in acc.items()}
    out, seen = [], set()
    prev = None
    for q in quads:
        keys = [tuple(np.floor(q[1][c] / cell).astype(int)) for c in range(4)]
        if q[0] == 2:
            if prev is not None and prev[0] and prev[1] is not None:
                out.append([2, prev[1][1], prev[1][2], prev[1][3]])
            continue
        prev = (False, None)
        if len(set(keys)) < 3:
            continue
        sig = (q[0], _canon(keys))
        if sig in seen:
            continue
        seen.add(sig)
        P_ = np.array([cen[k] for k in keys])
        entry = [q[0], P_, q[2], q[3]]
        out.append(entry)
        prev = (True, entry)
    return out


def make_lod(src_path, dst_path, fraction=LOD_FRACTION):
    """Writes the level-of-detail twin of a mesh file: the SMALLEST clustering cell size that keeps at most `fraction` of the body quads. Returns (lod body quads, source body quads)."""
    head, quads = read_game_mesh(src_path)
    body = sum(1 for q in quads if q[0] in (0, 1))
    lo, hi = 0.2, 40.0
    best = None
    for _ in range(18):
        mid = (lo + hi) / 2
        res = decimate(quads, mid)
        n = sum(1 for q in res if q[0] in (0, 1))
        if n <= fraction * body:
            best, hi = res, mid
        else:
            lo = mid
    if best is None:
        best = decimate(quads, hi)
    lines = [l for l in head if not l.startswith("# generated")]
    lines.insert(0, "# level of detail of %s (vertex clustering, release 1.102.0)" % os.path.basename(src_path))
    for layer, P_, UV, N in best:
        lines.append("qn %d %s" % (layer, " ".join("%.3f %.3f %.3f %.3f %.3f %.4f %.4f %.4f" % (*P_[k], *UV[k], *N[k]) for k in range(4))))
    open(dst_path, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    return sum(1 for q in best if q[0] in (0, 1)), body


def count_quads(path):
    q = {0: 0, 1: 0, 2: 0}
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if v and v[0] in ("q", "qn"):
            q[int(v[1])] += 1
    return q


def welded_count(path):
    pts = set()
    for line in open(path, encoding="utf-8"):
        v = line.split()
        if v and v[0] == "qn" and int(v[1]) in (0, 1):
            for k in range(4):
                o = 2 + 8 * k
                pts.add((round(float(v[o]), 2), round(float(v[o + 1]), 2), round(float(v[o + 2]), 2)))
    return len(pts)

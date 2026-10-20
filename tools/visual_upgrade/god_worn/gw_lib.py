"""gw_lib: the model container, detail levels and writer of the God worn upgrade.

A relic is built by a function `build(b, D)` (see b_back.py ...): `b` is an Item that hands out sets (one per body anchor: `b.set("body", "body")`, `b.arm("arm")` for the
right arm with its mirrored left copy ...), `D` is the Detail level (full, held, lod) that scales the subdivision counts, so ONE function describes the full model, the
held model (<= 2500 vertices, what is shown in the hand, on the ground and in an item frame) and the LOD model (<= 15% of the full vertices, drawn beyond 12 blocks).

The writer produces the same quad records as gen_worn_lib.write_model (so the game's WornMesh parser reads them), plus
    skin <id>                    the relic's own skin: textures/relic_worn/god/<id>.png (albedo) and <id>_glow.png (emissive map), 8 x 8 cells of 64 texels (skin.py)
and the quad flags LIT = 64 (the quad is also drawn in the emissive pass sampling the glow map, in the glow slot colour: engraved runes, veins, filigree) and ADD = 128 (additive pass only).
Bits 8, 16 and 32 belong to the Charms / Genesis models (GLOWMAP, STAR, HALO); WornMesh.java is the one table of all eight flags and a test checks that no bit is used twice.
"""
import hashlib
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "..", "relics"))
import gen_worn_lib as L  # noqa: E402
import mesh_lib as ml  # noqa: E402
from mesh_lib import Part, smooth_normals  # noqa: E402
import skin as SK  # noqa: E402

MAIN, TRIM, GLOW, DARK, ACCENT, NONE = L.MAIN, L.TRIM, L.GLOW, L.DARK, L.ACCENT, L.NONE
EMIT, SHELL, HOT, LIT, ADD = 1, 2, 4, 64, 128      # 8, 16, 32 are GLOWMAP, STAR, HALO of the Charms / Genesis models (WornMesh.java). ADD: drawn only in the additive pass (a halo, a soft sprite): never in the lit solid pass

ROOT = ml.ROOT
ASSETS = ml.ASSETS
WORN_DIR = os.path.join(ASSETS, "relic_worn")
SKIN_DIR = os.path.join(ASSETS, "textures", "relic_worn", "god")
FILE_QUADS = 1150                       # most quads of one model file (the shared WornMeshTest accepts 5000 vertices = 1250 quads)

ANCHOR_OF = {"head": "head", "body": "body", "rarm": "rarm", "larm": "larm", "rleg": "rleg", "lleg": "lleg"}


class Detail:
    """The level of a build: 'full' (worn, near), 'held' (hand, ground, frame: <= 2500 vertices) or 'lod' (worn beyond 12 blocks: <= 15% of the full vertices)."""

    def __init__(self, level, scale=1.0):
        assert level in ("full", "held", "lod")
        self.level = level
        self.scale = scale                                        # the build driver fits this per relic and level so the quad counts land on their targets (build.fit_scales)
        self.k = {"full": 1.0, "held": 0.52, "lod": 0.26}[level] * scale
        self.q = self.k * self.k                                  # element counts scale with the square (a grid of k x k)
        self.full = level == "full"
        self.lod = level == "lod"
        self.held = level == "held"

    def dim(self, n, lo=2):
        """A grid dimension / segment count."""
        lo = lo if self.full else max(2, lo // 2)
        return max(lo, int(round(n * self.k)))

    def cnt(self, n, lo=1):
        """How many repeated elements (links, scales, shards)."""
        return max(lo, int(round(n * self.q)))

    def sides(self, n, lo=3):
        """The sides of a round section."""
        lo = lo if self.full else max(3, lo - 1)
        return max(lo, int(round(n * (1.0 if self.full else 0.67 if self.held else 0.5))))

    def pick(self, full, held=None, lod=None):
        if self.full:
            return full
        if self.held:
            return full if held is None else held
        return (held if lod is None else lod) if held is not None or lod is not None else full


class GSet:
    """The geometry of one anchor of one relic: parts with slot, tile, flags, animation group; groups are local to the set."""

    def __init__(self, item, key, anchor, pair=None, rank=3):
        self.item, self.key, self.anchor, self.pair = item, key, anchor, pair          # pair: the mirrored second anchor ("larm") or None
        self.items = []
        self.groups = []
        self.rank, self._lvl = rank, 1             # rank: 3 full, 2 held, 1 lod; _lvl: the detail level the parts being added need (1 every level, 2 held and full, 3 full only)

    def lvl(self, n):
        """`with gs.lvl(3):` parts added inside exist at the full level only, lvl(2) at held and full, lvl(1) at every level (the default)."""
        gs = self

        class _Ctx:
            def __enter__(self_):
                self_.old = gs._lvl
                gs._lvl = n

            def __exit__(self_, *a):
                gs._lvl = self_.old
        return _Ctx()

    def group(self, name, kind, pivot=(0, 0, 0), a=0.0, b=0.0, c=0.0):
        self.groups.append((name, kind, tuple(float(x) for x in pivot), float(a), float(b), float(c)))
        return len(self.groups)

    def add(self, part, slot=MAIN, tile="metal", flags=0, group=0, uv="tile", density=8.0, smooth=40.0):
        if part is None or not part.F or self._lvl > self.rank:
            return None
        part.smooth = smooth
        self.items.append((part, slot, tile, flags, group, uv, density))
        return part

    def quads(self):
        return sum(len(p.F) for (p, *_r) in self.items)


class Item:
    """What a builder fills: the sets of one relic at one detail level."""

    def __init__(self, rid, D):
        self.rid, self.D = rid, D
        self.sets = []
        import items as _it
        self.hands_only_one = _it.BY_ID[rid][2] == "HANDS"

    def set(self, key, anchor, pair=None):
        rank = {"full": 3, "held": 2, "lod": 1}[self.D.level]
        if self.D.held and self.hands_only_one:
            # a pair of gloves is held by ONE cuff (hold style `cuff`, only=1): the held form is its first set, once
            pair = None
            if self.sets:
                rank = 0
        s = GSet(self.rid, key, anchor, pair, rank)
        self.sets.append(s)
        return s

    def quads(self):
        return sum(s.quads() * (2 if s.pair else 1) for s in self.sets)

    def vertices(self):
        return 4 * self.quads()


# ---------------------------------------------------------------- uv mapping with the 64 texel cells
def _dom(n):
    ax = int(np.argmax(np.abs(n)))
    return ax, (1 if n[ax] >= 0 else -1)


def _ab(ax, sg, p):
    """The two coordinates a fitted cell is stretched over, oriented so a decal reads left to right for a viewer looking at the face."""
    x, y, z = p
    if ax == 0:
        return (-sg * z, y)
    if ax == 1:
        return (x, z)
    return (sg * x, y)


def quad_uv(part, f, tile, uv_mode, density, fit_box):
    idx = part.F[f]
    P = [part.V[i] for i in idx]
    n = part.face_normal(f)
    ax, sg = _dom(n)
    ab = np.array([_ab(ax, sg, p) for p in P])
    ti = SK.CELL_INDEX[tile]
    cx0, cy0 = (ti % SK.COLS) * SK.C, (ti // SK.COLS) * SK.C
    if SK.is_tileable(tile) and uv_mode == "tile":
        c = (ab.max(axis=0) + ab.min(axis=0)) / 2.0
        span = (ab.max(axis=0) - ab.min(axis=0)) * density
        k = np.ones(2)
        for d in range(2):
            if span[d] > SK.PERIOD:
                k[d] = SK.PERIOD / span[d]
        fr = (c * density) % SK.PERIOD
        half = span * k / 2.0
        fr = np.clip(fr, half - SK.GUT + 0.01, SK.PERIOD + SK.GUT - half - 0.01)
        out = []
        for q in ab:
            off = (q - c) * density * k
            u, v = fr + off
            out.append((cx0 + SK.GUT + u, cy0 + SK.GUT + v))
    else:
        lo, hi = fit_box[(ax, sg)]
        ext = np.maximum(hi - lo, 1e-6)
        out = []
        for q in ab:
            t = (q - lo) / ext
            out.append((cx0 + 2 + t[0] * (SK.C - 4), cy0 + 2 + (1.0 - t[1]) * (SK.C - 4)))
    return [(u / SK.SKIN, v / SK.SKIN) for (u, v) in out]


def _fit_boxes(part):
    ext = {}
    for f, idx in enumerate(part.F):
        ax, sg = _dom(part.face_normal(f))
        ab = np.array([_ab(ax, sg, part.V[i]) for i in idx])
        lo, hi = ab.min(axis=0), ab.max(axis=0)
        e = ext.get((ax, sg))
        ext[(ax, sg)] = (lo, hi) if e is None else (np.minimum(e[0], lo), np.maximum(e[1], hi))
    return ext


def _fmt(v, nd):
    s = ("%." + str(nd) + "f") % v
    if s.startswith("-0.") and float(s) == 0.0:
        s = s[1:]
    if "." in s:
        s = s.rstrip("0").rstrip(".")
    return s if s not in ("", "-") else "0"


def write_file(path, name, anchor, skin_id, groups, items):
    lines = []
    quads = []
    for (part, slot, tile, flags, group, uvm, dens) in items:
        part.ensure_outward()
        smooth_normals(part, part.smooth)
        fit_box = _fit_boxes(part)
        for f, idx in enumerate(part.F):
            uv = quad_uv(part, f, tile, uvm, dens, fit_box)
            rec = []
            for k in range(4):
                p = part.V[idx[k]]
                nrm = part.N[f][k]
                rec.append((p[0], -p[1], p[2], uv[k][0], uv[k][1], nrm[0], -nrm[1], nrm[2]))
            rec = [rec[0], rec[3], rec[2], rec[1]]
            quads.append((group, slot, flags, rec))
    lines.append("# worn relic model %s: %d quads, %d vertices; generated by tools/visual_upgrade/god_worn, do not edit" % (name, len(quads), len(quads) * 4))
    lines.append("model " + name)
    lines.append("anchor " + anchor)
    lines.append("skin " + skin_id)
    for gi, (gname, kind, pv, a, b, c) in enumerate(groups, 1):
        lines.append("grp %d %s %s %s %s %s %s %s %s" % (gi, gname, kind, _fmt(pv[0], 3), _fmt(-pv[1], 3), _fmt(pv[2], 3), _fmt(a, 4), _fmt(b, 4), _fmt(c, 4)))
    for (g, slot, flags, rec) in quads:
        nums = []
        for r in rec:
            nums += [_fmt(r[0], 2), _fmt(r[1], 2), _fmt(r[2], 2), _fmt(r[3], 4), _fmt(r[4], 4), _fmt(r[5], 2), _fmt(r[6], 2), _fmt(r[7], 2)]
        lines.append("q %d %d %d %s" % (g, slot, flags, " ".join(nums)))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return len(quads)


def write_set(gs, suffix, skin_id, out_dir=WORN_DIR):
    """Writes one set as one or more model files (split under FILE_QUADS, whole parts only). Returns [(model name, quads)]."""
    chunks, cur, curq = [], [], 0
    for it in gs.items:
        q = len(it[0].F)
        if q > FILE_QUADS:
            raise ValueError("%s/%s: one part has %d quads (> %d)" % (gs.item, gs.key, q, FILE_QUADS))
        if curq + q > FILE_QUADS and cur:
            chunks.append(cur)
            cur, curq = [], 0
        cur.append(it)
        curq += q
    if cur:
        chunks.append(cur)
    out = []
    for ci, ch in enumerate(chunks):
        name = "gw_%s_%s%s%s" % (gs.item, gs.key, suffix, "" if ci == 0 else "_%d" % (ci + 1))
        used = sorted({it[4] for it in ch if it[4]})
        remap = {g: i + 1 for i, g in enumerate(used)}
        groups = [gs.groups[g - 1] for g in used]
        items = [(it[0], it[1], it[2], it[3], remap.get(it[4], 0), it[5], it[6]) for it in ch]
        n = write_file(os.path.join(out_dir, name + ".txt"), name, gs.anchor, skin_id, groups, items)
        out.append((name, n))
    return out


def pieces_of(gs, names):
    """The catalog pieces of a written set: a pair is written for the right anchor and mirrored for the left one."""
    out = []
    for (name, _n) in names:
        out.append("%s@%s" % (name, gs.anchor))
        if gs.pair:
            out.append("%s@%s~" % (name, gs.pair))
    return out

"""The x3 blocky worn models (3.9.0): every worn relic model of assets/nusmp/relic_worn is voxelised on a small grid, hidden faces are dropped, coplanar faces are merged, and the big flat
faces get a raised frame + panel + glow inlay (the Saint Valkyrie Dress look). Every quad keeps the colour SLOT and the atlas TILE of the quad it came from, so a relic keeps its own colours
(palette main / trim / glow) and only takes the blocky shape. Output: assets/nusmp/relic_worn_x3/<name>.txt + index.txt (the game picks a model's x3 version when the file is listed).

    python3 tools/relics/gen_worn_x3.py [--only name,name] [--out DIR] [--report]

Only the ARMOUR relics (head, chest, feet) get this style (the owner: "the armor pieces get the blocky style, nothing else"); capes / cloaks / mantles and the visual-upgrade models that carry their own
painted skin are left as they were. Pure Python 3 + numpy.
"""
import json
import math
import os
import re
import sys
from collections import defaultdict

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_worn_lib as L  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
WORN = os.path.join(ASSETS, "relic_worn")
DEFAULT_OUT = os.path.join(ASSETS, "relic_worn_x3")

EMIT, SHELL, HOT = 1, 2, 4
MAIN, TRIM, GLOW, DARK, ACC, NONE = 0, 1, 2, 3, 4, 5
# the quads of a model are rebuilt at about this many times the original count (frames and inlays bring it up from the voxel shell)
TARGET = 3.0
MAX_VERTS = 6000                      # the hard cap of one x3 model: solid vertices + emissive vertices (UpgradeCaps.X3_MODEL_VERTICES = 6400)
GRIDS = (0.375, 0.5, 0.625, 0.75, 1.0, 1.25, 1.5, 2.0)


# ------------------------------------------------------------------------------------------------ reading
class Model:
    def __init__(self, name, anchor, groups, quads):
        self.name, self.anchor, self.groups, self.quads = name, anchor, groups, quads      # groups: list of the raw "grp" lines; quads: (g, slot, flags, P[4,3], UV[4,2])


def read_model(path):
    name = os.path.splitext(os.path.basename(path))[0]
    anchor, groups, quads, skin = "body", [], [], False
    for line in open(path):
        v = line.split()
        if not v or v[0].startswith("#"):
            continue
        if v[0] == "model":
            name = v[1]
        elif v[0] == "anchor":
            anchor = v[1]
        elif v[0] == "grp":
            groups.append(line.strip())
        elif v[0] in ("skin", "glow", "star", "lod"):
            skin = True
        elif v[0] == "q":
            g, slot, flags = int(v[1]), int(v[2]), int(v[3])
            P = np.array([[float(x) for x in v[4 + k * 8:7 + k * 8]] for k in range(4)])
            UV = np.array([[float(x) for x in v[7 + k * 8:9 + k * 8]] for k in range(4)])
            quads.append((g, slot, flags, P, UV))
    m = Model(name, anchor, groups, quads)
    m.skin = skin
    return m


def tile_of(uv):
    """The atlas tile name a quad samples (the cell of the centre of its UVs)."""
    cu, cv = uv[:, 0].mean() * L.ATLAS_W, uv[:, 1].mean() * L.ATLAS_H
    col, row = min(L.COLS - 1, max(0, int(cu // L.CELL))), min(L.ROWS - 1, max(0, int(cv // L.CELL)))
    return L.TILES[row * L.COLS + col]


# ------------------------------------------------------------------------------------------------ voxelising
def voxelise(model, h):
    """{(group, i, j, k): (slot, tile, flags)} - every cell of side h that a quad passes through. An emissive quad wins over a plain one in the same cell."""
    vox = {}
    pri = {}
    for (g, slot, flags, P, UV) in model.quads:
        tile = tile_of(UV)
        e1 = max(np.linalg.norm(P[1] - P[0]), np.linalg.norm(P[2] - P[3]))
        e2 = max(np.linalg.norm(P[3] - P[0]), np.linalg.norm(P[2] - P[1]))
        nu, nv = int(math.ceil(e1 / (h * 0.45))) + 1, int(math.ceil(e2 / (h * 0.45))) + 1
        us = np.linspace(0, 1, nu)[:, None, None]
        vs = np.linspace(0, 1, nv)[None, :, None]
        pts = (1 - vs) * ((1 - us) * P[0] + us * P[1]) + vs * ((1 - us) * P[3] + us * P[2])
        idx = np.floor(pts.reshape(-1, 3) / h + 1e-6).astype(int)
        keep = (flags & (EMIT | SHELL | HOT))
        p = 2 if flags & EMIT else (1 if slot == TRIM else 0)
        for key in {tuple(r) for r in idx.tolist()}:
            k = (g,) + key
            if k not in vox or p > pri[k]:
                vox[k] = (slot, tile, keep)
                pri[k] = p
    return vox


def exposed_faces(vox):
    """{(g, axis, sign, plane): {(a, b): attr}} the voxel faces that have no voxel of the same group behind them."""
    faces = defaultdict(dict)
    for (g, i, j, k), attr in vox.items():
        c = (i, j, k)
        for axis in range(3):
            for sign in (-1, 1):
                n = list(c)
                n[axis] += sign
                if (g, n[0], n[1], n[2]) in vox:
                    continue
                plane = c[axis] + (1 if sign > 0 else 0)
                a, b = [c[x] for x in range(3) if x != axis]
                faces[(g, axis, sign, plane)][(a, b)] = attr
    return faces


def greedy(cells):
    """Merges equal-attribute unit cells {(a, b): attr} into rectangles (a0, b0, a1, b1, attr) (a1, b1 exclusive)."""
    out = []
    left = dict(cells)
    for (a, b) in sorted(cells):
        if (a, b) not in left:
            continue
        attr = left[(a, b)]
        a1 = a + 1
        while left.get((a1, b)) == attr:
            a1 += 1
        b1 = b + 1
        while all(left.get((x, b1)) == attr for x in range(a, a1)):
            b1 += 1
        for x in range(a, a1):
            for y in range(b, b1):
                del left[(x, y)]
        out.append((a, b, a1, b1, attr))
    return out


# ------------------------------------------------------------------------------------------------ emitting quads
class Emit:
    """Collects quads (group, slot, flags, 4 x (x y z u v nx ny nz)). box() writes the faces of an axis-aligned box that are not skipped; the UVs come from the tile cell."""

    def __init__(self):
        self.q = []

    def face(self, fi, x0, y0, z0, x1, y1, z1, slot, tile, flags=0, g=0, dens=1.0):
        faces = [((x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (0, 0, -1)), ((x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1), (0, 0, 1)),
                 ((x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1), (-1, 0, 0)), ((x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0), (1, 0, 0)),
                 ((x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0), (0, -1, 0)), ((x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1), (0, 1, 0))]
        f = faces[fi]
        pts, n = f[:4], f[4]
        ti = L.TILE[tile]
        cx0, cy0 = (ti % L.COLS) * L.CELL, (ti // L.COLS) * L.CELL
        ax = 0 if n[0] else (1 if n[1] else 2)
        ab = [((p[2], p[1]) if ax == 0 else ((p[0], p[2]) if ax == 1 else (p[0], p[1]))) for p in pts]
        lo, hi = np.min(ab, axis=0), np.max(ab, axis=0)
        c = (lo + hi) / 2
        span = np.minimum((hi - lo) * dens, L.PERIOD - 1)
        fr = (c * dens) % L.PERIOD
        half = span / 2
        fr = np.clip(fr, half + 0.5, L.PERIOD - half - 0.5)
        rec = []
        for k, p in enumerate(pts):
            off = np.clip((np.array(ab[k]) - c) * dens, -half, half)
            u, v = fr + off
            rec.append((p[0], p[1], p[2], (cx0 + L.GUT + u) / L.ATLAS_W, (cy0 + L.GUT + v) / L.ATLAS_H, n[0], n[1], n[2]))
        self.q.append((g, slot, flags, rec))

    def box(self, x0, y0, z0, x1, y1, z1, slot, tile, flags=0, g=0, skip=(), dens=1.0):
        x0, x1 = sorted((x0, x1))
        y0, y1 = sorted((y0, y1))
        z0, z1 = sorted((z0, z1))
        for fi in range(6):
            if fi not in skip:
                self.face(fi, x0, y0, z0, x1, y1, z1, slot, tile, flags, g, dens)


def fmt(x):
    s = "%.3f" % x
    s = s.rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def fuv(x):
    return "%.4f" % x


def write_model(path, name, anchor, groups, quads, note):
    lines = [f"# x3 blocky worn model {name}: {len(quads)} quads, {len(quads) * 4} vertices; {note}; generated by tools/relics/gen_worn_x3.py, do not edit", f"model {name}", f"anchor {anchor}"]
    lines += groups
    for (g, slot, flags, rec) in quads:
        lines.append("q %d %d %d %s" % (g, slot, flags, " ".join("%s %s %s %s %s %s %s %s" % (fmt(r[0]), fmt(r[1]), fmt(r[2]), fuv(r[3]), fuv(r[4]), fmt(r[5]), fmt(r[6]), fmt(r[7])) for r in rec)))
    with open(path, "w") as f:
        f.write("\n".join(lines) + "\n")


# ------------------------------------------------------------------------------------------------ the blocky model
AXFACE = {(0, -1): 2, (0, 1): 3, (1, -1): 4, (1, 1): 5, (2, -1): 0, (2, 1): 1}      # (axis, sign) -> face index of Emit.face


def rect_box(axis, plane, a0, b0, a1, b1, h):
    """The two corners of a rectangle on the given plane (all in model pixels) as x0,y0,z0,x1,y1,z1."""
    lo, hi = [0, 0, 0], [0, 0, 0]
    others = [x for x in range(3) if x != axis]
    lo[axis] = hi[axis] = plane * h
    lo[others[0]], hi[others[0]] = a0 * h, a1 * h
    lo[others[1]], hi[others[1]] = b0 * h, b1 * h
    return lo + hi


def blockify(model, h):
    """The plain voxel shell of a model: (list of (g, axis, sign, plane, a0, b0, a1, b1, attr), the voxel dict)."""
    vox = voxelise(model, h)
    rects = []
    for (g, axis, sign, plane), cells in exposed_faces(vox).items():
        for (a0, b0, a1, b1, attr) in greedy(cells):
            rects.append((g, axis, sign, plane, a0, b0, a1, b1, attr))
    return rects, vox


def build(model, report=None):
    n0 = len(model.quads)
    best = None
    for h in GRIDS:                                                  # the finest grid whose plain shell stays within 2.2x the original count and well under the vertex cap
        rects, vox = blockify(model, h)
        ok = len(rects) <= max(2.2 * n0, 40) and 4 * len(rects) + 4 * sum(1 for r in rects if r[8][2] & EMIT) <= MAX_VERTS * 0.85
        best = (h, rects, vox)
        if ok:
            break
    h, rects, vox = best
    em = Emit()
    allp = np.concatenate([q[3] for q in model.quads])
    centre = (allp.min(axis=0) + allp.max(axis=0)) / 2
    for (g, axis, sign, plane, a0, b0, a1, b1, (slot, tile, flags)) in rects:
        x0, y0, z0, x1, y1, z1 = rect_box(axis, plane, a0, b0, a1, b1, h)
        em.face(AXFACE[(axis, sign)], x0, y0, z0, x1, y1, z1, slot, tile, flags, g)
    base = len(em.q)
    budget = int(max(n0 * TARGET, base * 1.25) - base)
    # details on the biggest outward faces first: a raised trim frame, a raised panel of the face's own colour, a glow inlay line
    cand = []
    for r in rects:
        g, axis, sign, plane, a0, b0, a1, b1, attr = r
        wa, wb = (a1 - a0) * h, (b1 - b0) * h
        if min(wa, wb) < 1.0 or wa * wb < 2.0:
            continue
        cx = [0, 0, 0]
        others = [x for x in range(3) if x != axis]
        cx[axis] = plane * h
        cx[others[0]], cx[others[1]] = (a0 + a1) * h / 2, (b0 + b1) * h / 2
        out = (np.array(cx) - centre)[axis] * sign
        if out < -0.01 and attr[1] != "gem":                         # the inside of a band or a shell is not decorated
            continue
        cand.append((wa * wb, r))
    cand.sort(key=lambda t: -t[0])
    used = 0
    for area, r in cand:
        if used >= budget:
            break
        if verts(em) > MAX_VERTS - 400:                                # one face detail is at most ~25 quads
            break
        used += detail(em, r, h)
    if report is not None:
        report.update(grid=h, base=base, final=len(em.q), orig=n0)
    return em.q


def verts(em):
    return 4 * len(em.q) + 4 * sum(1 for q in em.q if q[2] & EMIT)


def detail(em, r, h):
    """A raised frame, a panel and (on big faces) a glow inlay on one merged face. Returns the quads added."""
    g, axis, sign, plane, a0, b0, a1, b1, (slot, tile, flags) = r
    before = len(em.q)
    if tile in ("gem", "white", "black"):                              # a stone or a flat colour: just a little raised
        t0, t1 = 0.0, 0.25
        edge = 0.2
        body = (slot, tile, flags)
        frame = None
    else:
        body = (slot, tile, flags)
        frame = (TRIM, "metal", 0)
        edge = 0.18
    lo = np.array(rect_box(axis, plane, a0, b0, a1, b1, h))
    others = [x for x in range(3) if x != axis]
    wa, wb = (a1 - a0) * h, (b1 - b0) * h

    def slab(inset, t0, t1, slot_, tile_, flags_, only_outer=False):
        """A box over the face, inset from its edges, standing out from t0 to t1; its back face is never drawn."""
        lo2, hi2 = lo.copy()[:3], lo.copy()[3:]
        lo2[others[0]] += inset
        hi2[others[0]] -= inset
        lo2[others[1]] += inset
        hi2[others[1]] -= inset
        if hi2[others[0]] - lo2[others[0]] < 0.2 or hi2[others[1]] - lo2[others[1]] < 0.2:
            return
        p0, p1 = lo2[axis] + sign * t0, lo2[axis] + sign * t1
        lo2[axis], hi2[axis] = min(p0, p1), max(p0, p1)
        back = AXFACE[(axis, -sign)]
        em.box(lo2[0], lo2[1], lo2[2], hi2[0], hi2[1], hi2[2], slot_, tile_, flags_, g, skip=(back,))

    if frame is not None:
        slab(edge, 0.0, 0.2, *frame)
        slab(edge + 0.45, 0.2, 0.45, *body)
        if min(wa, wb) >= 3.0 and wa * wb >= 9.0:                    # one thin glow line along the long side, through the middle
            long_a = wa >= wb
            span = (wb if long_a else wa) / 2 - 0.12
            lo2, hi2 = lo.copy()[:3], lo.copy()[3:]
            c_other = (lo2[others[1]] + hi2[others[1]]) / 2 if long_a else (lo2[others[0]] + hi2[others[0]]) / 2
            if long_a:
                lo2[others[0]] += edge + 0.9
                hi2[others[0]] -= edge + 0.9
                lo2[others[1]], hi2[others[1]] = c_other - 0.15, c_other + 0.15
            else:
                lo2[others[1]] += edge + 0.9
                hi2[others[1]] -= edge + 0.9
                lo2[others[0]], hi2[others[0]] = c_other - 0.15, c_other + 0.15
            if hi2[others[0]] - lo2[others[0]] > 0.2 or hi2[others[1]] - lo2[others[1]] > 0.2:
                p0, p1 = lo[axis] + sign * 0.45, lo[axis] + sign * 0.6
                lo2[axis], hi2[axis] = min(p0, p1), max(p0, p1)
                em.box(lo2[0], lo2[1], lo2[2], hi2[0], hi2[1], hi2[2], GLOW, "white", EMIT, g, skip=(AXFACE[(axis, -sign)],))
    else:
        slab(edge, 0.0, 0.3, *body)
    return len(em.q) - before


# ------------------------------------------------------------------------------------------------ which models
CAPE_WORDS = re.compile(r"(cape|cloak|mantle|shawl)", re.I)


def armour_models():
    """The models the FEET armour relics wear (3.9.0: the owner narrowed the blocky style to the feet): the FEET looks and the `relic` entries whose id is in no Curios tag and whose pieces sit on
    a leg (`@rleg` / `@lleg`). Only the leg-anchored pieces are taken, so a shared decoration on such a relic (deco_flames) stays as it was. Everything else (head, chest, amulets, belts, gloves,
    charms, back pieces) is not part of the x3 style."""
    tagdir = os.path.join(ROOT, "src", "main", "resources", "data", "curios", "tags", "item")
    curios = set()
    for fn in os.listdir(tagdir):
        for x in json.load(open(os.path.join(tagdir, fn)))["values"]:
            curios.add((x["id"] if isinstance(x, dict) else x).split(":")[1])
    models = set()
    for line in open(os.path.join(WORN, "catalog.txt")):
        v = line.split()
        if len(v) < 3:
            continue
        if v[0] == "look" and v[1] == "FEET":
            pieces = v[3:]
        elif v[0] == "relic" and v[1] not in curios:
            pieces = v[2:]
        else:
            continue
        for p in pieces:
            name, _, anchor = p.rstrip("~").partition("@")
            if anchor in ("rleg", "lleg"):
                models.add(name)
    return models


def selected():
    armour = armour_models()
    out, skipped = [], {"cape": [], "not-armour": [], "skin": []}
    for fn in sorted(os.listdir(WORN)):
        if not fn.endswith(".txt") or fn == "catalog.txt":
            continue
        name = fn[:-4]
        if name not in armour:
            skipped["not-armour"].append(name)
            continue
        m = read_model(os.path.join(WORN, fn))
        if m.skin:
            skipped["skin"].append(name)
        elif CAPE_WORDS.search(name):
            skipped["cape"].append(name)
        else:
            out.append(name)
    return out, skipped


def wing_groups(m):
    """The group ids of the wings of a model (`grp <id> wings ...`): the owner wants wings left as they were, so they are copied unchanged and never made blocky."""
    return {int(g.split()[1]) for g in m.groups if len(g.split()) > 2 and "wing" in g.split()[2].lower()}


def raw_quads(path, ids):
    """The `q` lines of a classic model that belong to the given groups, copied as they are."""
    return [l.rstrip("\n") for l in open(path) if l.startswith("q ") and int(l.split()[1]) in ids]


def main(argv):
    only = None
    out = DEFAULT_OUT
    report = "--report" in argv
    for i, a in enumerate(argv):
        if a == "--only":
            only = set(argv[i + 1].split(","))
        if a == "--out":
            out = argv[i + 1]
    os.makedirs(out, exist_ok=True)
    if not only:
        for fn in os.listdir(out):
            if fn.endswith(".txt"):
                os.remove(os.path.join(out, fn))              # a model that is no longer selected must not stay behind
    names, skipped = selected()
    if only:
        names = [n for n in names if n in only]
    done = []
    rep = {}
    for n in names:
        src = os.path.join(WORN, n + ".txt")
        m = read_model(src)
        if not m.quads:
            continue
        wings = wing_groups(m)
        if wings:                                                      # wings stay exactly as they were: only the rest of the boot is blocky
            m.quads = [q for q in m.quads if q[0] not in wings]
        r = {}
        q = build(m, r)
        dst = os.path.join(out, n + ".txt")
        write_model(dst, n, m.anchor, m.groups, q, "grid %s px, was %d quads%s" % (r["grid"], r["orig"], ", wings kept as they were" if wings else ""))
        if wings:
            with open(dst, "a") as f:
                f.write("\n".join(raw_quads(src, wings)) + "\n")
        done.append(n)
        rep[n] = r
    if not only:
        open(os.path.join(out, "index.txt"), "w").write("# the models that have an x3 version (relic_worn_x3/<name>.txt); written by tools/relics/gen_worn_x3.py\n" + "\n".join(done) + "\n")
        open(os.path.join(out, "skipped.txt"), "w").write("\n".join("%s: %s" % (k, " ".join(v)) for k, v in skipped.items()) + "\n")
    print("x3 models: %d  skipped: %s" % (len(done), {k: len(v) for k, v in skipped.items()}))
    if report:
        tot0 = sum(r["orig"] for r in rep.values())
        tot1 = sum(r["final"] for r in rep.values())
        print("quads %d -> %d (x%.2f)" % (tot0, tot1, tot1 / max(1, tot0)))
        for n, r in sorted(rep.items(), key=lambda kv: -kv[1]["final"])[:15]:
            print("  %-34s %4d -> %4d  grid %s" % (n, r["orig"], r["final"], r["grid"]))


if __name__ == "__main__":
    main(sys.argv[1:])

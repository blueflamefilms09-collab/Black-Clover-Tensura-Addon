"""The pipeline of the visual-upgrade slice `djinn_dark_weapons` (release 1.108.0): the 36 dark Djinn weapons.

The old weapons are baked meshes (the Djinn armour set's weapon piece, tools/relics/djinn_weapons.py), not part-structured builders, so the upgrade is built on the BAKED file:

  1. every old quad stays (position, normal, shape), so the silhouette, the size and the way the weapon sits in the hand are the old ones;
  2. the old skin islands (clusters of quads whose UV boxes touch) are cut out, enlarged 2x (bicubic colour, hard alpha) and repacked, together with the UV groups of the new parts, into a
     NEW 1024 px skin (a MaxRects packer), so old and new surfaces have a similar texel density; the old quads get the new UVs;
  3. new parts (tools/visual_upgrade/djinn_dark_weapons/dkit.py, built with the owner's mesh_lib method on the frame of the old weapon, `frame.py`) are added; they are painted with the
     mesh_lib materials, then every surface (old and new) gets grain / ambient occlusion / bevel / scratches (`detail_pass` of god_melee_b, `detail_old` here for the enlarged old islands);
  4. a core glow map, a second emissive map (energy veins) and the files are written as NEW files under up/: weapon_meshes/up/<id>.txt, _lod.txt, _orbit.txt, textures/entity/weapon/up/<id>*.png,
     assets/nusmp/visual_upgrade/<id>.json (the table client/GodMeleeBRender reads). The old files are not touched.
"""
import importlib.util
import json
import math
import os
import sys
import zlib

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
sys.path.insert(0, os.path.join(ROOT, "tools", "relics"))
sys.path.insert(0, HERE)

import mesh_lib as ml                      # noqa: E402
import wave1_models as w                   # noqa: E402
import new197_lib as nl                    # noqa: E402


def _load(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    mod = importlib.util.module_from_spec(spec)
    sys.modules[name] = mod
    spec.loader.exec_module(mod)
    return mod


gb = _load("gb_lib", os.path.join(ROOT, "tools", "visual_upgrade", "god_melee_b", "lib.py"))      # make_lod, detail_pass, counters, geometry helpers (reused, not copied)

ASSETS = ml.ASSETS
MESH_DIR = os.path.join(ASSETS, "weapon_meshes", "up")
TEX_DIR = os.path.join(ASSETS, "textures", "entity", "weapon", "up")
DATA_DIR = os.path.join(ASSETS, "visual_upgrade")
SIZE = 1024
K_ATLAS = SIZE / 128.0                     # texels per UV unit (the renderer divides UV by 128)
LOD_FRACTION = 0.14


# ================================================================ the old mesh
class OldMesh:
    def __init__(self, item_id):
        self.id = item_id
        self.head = {"place": None, "glow": None, "grip": None}
        self.quads = []                                                        # layer-0 body quads: dict(P, UV, N)
        for line in open(os.path.join(ASSETS, "weapon_meshes", item_id + ".txt"), encoding="utf-8"):
            v = line.split()
            if not v or v[0] == "#":
                continue
            if v[0] == "place":
                self.head["place"] = [float(x) for x in v[1:6]]
            elif v[0] == "glow":
                self.head["glow"] = line.strip()
            elif v[0] == "grip":
                self.head["grip"] = line.strip()
            elif v[0] == "qn" and int(v[1]) in (0, 2):
                a = np.array([float(x) for x in v[2:34]]).reshape(4, 8)
                self.quads.append({"layer": int(v[1]), "P": a[:, :3].copy(), "UV": a[:, 3:5].copy(), "N": a[:, 5:8].copy(), "twin": False})
        # a layer-2 quad on the same corners as a body quad is that quad's glow twin; the others are glow-only parts (an energy arc) and stay quads of their own
        body = {tuple(np.round(q["P"], 3).ravel()): q for q in self.quads if q["layer"] == 0}
        keep = []
        for q in self.quads:
            if q["layer"] == 2:
                b = body.get(tuple(np.round(q["P"], 3).ravel()))
                if b is not None:
                    b["twin"] = True
                    continue
            keep.append(q)
        self.quads = keep
        self.body_count = sum(1 for q in self.quads if q["layer"] == 0)
        td = os.path.join(ASSETS, "textures", "entity", "weapon")
        self.skin = np.asarray(Image.open(os.path.join(td, item_id + ".png")).convert("RGBA"), np.float32)
        self.glow = np.asarray(Image.open(os.path.join(td, item_id + "_glow.png")).convert("RGBA"), np.float32)
        self.clusters = self._cluster()

    def _cluster(self):
        """Groups of quads whose UV boxes (in texels, 2 px margin) overlap: the old skin islands. Each: rect (x, y, w, h), quad indices, mask of the copied texels."""
        n = len(self.quads)
        boxes = np.zeros((n, 4), np.int64)
        for i, q in enumerate(self.quads):
            u = q["UV"] * K_ATLAS
            boxes[i] = (max(0, math.floor(u[:, 0].min()) - 2), max(0, math.floor(u[:, 1].min()) - 2), min(SIZE, math.ceil(u[:, 0].max()) + 2), min(SIZE, math.ceil(u[:, 1].max()) + 2))
        parent = list(range(n))

        def find(a):
            while parent[a] != a:
                parent[a] = parent[parent[a]]
                a = parent[a]
            return a

        for i in range(n):
            for j in range(i + 1, n):
                if boxes[i, 0] < boxes[j, 2] and boxes[j, 0] < boxes[i, 2] and boxes[i, 1] < boxes[j, 3] and boxes[j, 1] < boxes[i, 3]:
                    a, b = find(i), find(j)
                    if a != b:
                        parent[a] = b
        groups = {}
        for i in range(n):
            groups.setdefault(find(i), []).append(i)
        out = []
        for idx in groups.values():
            x0, y0 = int(boxes[idx, 0].min()), int(boxes[idx, 1].min())
            x1, y1 = int(boxes[idx, 2].max()), int(boxes[idx, 3].max())
            mask = np.zeros((y1 - y0, x1 - x0), bool)
            for i in idx:
                b = boxes[i]
                mask[b[1] - y0:b[3] - y0, b[0] - x0:b[2] - x0] = True
            out.append({"rect": (x0, y0, x1 - x0, y1 - y0), "quads": idx, "mask": mask, "pos": None, "k": 1})
        out.sort(key=lambda c: -c["rect"][2] * c["rect"][3])
        return out


# ================================================================ packing
def pack_maxrects(sizes, W, H, pad):
    """MaxRects, best short side fit, biggest first (the packer of the upgraded skins of god_melee_a, written the same way). Returns [(x, y)] or None."""
    n = len(sizes)
    order = sorted(range(n), key=lambda i: (-max(sizes[i]), -min(sizes[i])))
    free = [(pad, pad, W - 2 * pad, H - 2 * pad)]
    pos = [None] * n
    for i in order:
        wd, ht = sizes[i]
        best = None
        for (fx, fy, fw, fh) in free:
            if wd <= fw and ht <= fh:
                key = (min(fw - wd, fh - ht), max(fw - wd, fh - ht), fy, fx)
                if best is None or key < best[0]:
                    best = (key, fx, fy)
        if best is None:
            return None
        _k, x, y = best
        pos[i] = (x, y)
        bx0, by0, bx1, by1 = x, y, x + wd + pad, y + ht + pad
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
        free = [r for k, r in enumerate(new) if not any(j != k and new[j][0] <= r[0] and new[j][1] <= r[1] and new[j][0] + new[j][2] >= r[0] + r[2]
                                                         and new[j][1] + new[j][3] >= r[1] + r[3] and (new[j] != r or j < k) for j in range(len(new)))]
    return pos


# ================================================================ the atlas
def upsample(a, W, H):
    """An RGBA float patch resized to W x H texels (an enlargement by about 2x, or 1x = a copy): colour bicubic on premultiplied values, alpha nearest (the cut-out islands keep a hard edge)."""
    h, wd = a.shape[:2]
    if (W, H) == (wd, h):
        return a.copy()
    al = a[..., 3:4] / 255.0
    pre = a[..., :3] * al
    chans = [np.asarray(Image.fromarray(np.ascontiguousarray(pre[..., c])).resize((W, H), Image.BICUBIC), np.float32) for c in range(3)]
    alp_soft = np.asarray(Image.fromarray(np.ascontiguousarray(a[..., 3])).resize((W, H), Image.BICUBIC), np.float32) / 255.0
    alp_hard = np.asarray(Image.fromarray(np.ascontiguousarray(a[..., 3])).resize((W, H), Image.NEAREST), np.float32)
    rgb = np.stack(chans, 2) / np.maximum(alp_soft[..., None], 0.05)
    out = np.concatenate([np.clip(rgb, 0, 255), alp_hard[..., None]], axis=2)
    out[alp_hard <= 0] = 0
    return out


class DAtlas(ml.Atlas):
    """mesh_lib's Atlas with the old islands pre-placed: `layout_old` packs the enlarged old islands and the UV groups of the new parts together."""

    def layout_old(self, mesh, old, ppu_max=22.0, ppu_min=4.0, pad=3):
        mesh.finish()
        groups = []
        for p in mesh.parts:
            if p.layer == 1:
                p.UV = [[(0.0, 0.0)] * 4 for _ in p.F]
                p.groups = []
                continue
            groups += ml.plan_uv(p, "dominant", True)
            p.UV = [None] * len(p.F)
        best = None
        for k in (2.0, 1.5, 1.0):
            osz = [(int(math.ceil(c["rect"][2] * k)), int(math.ceil(c["rect"][3] * k))) for c in old.clusters]
            lo, hi = ppu_min, ppu_max
            found = None
            for _ in range(20):
                mid = (lo + hi) / 2
                pos = pack_maxrects(osz + [g.px_size(mid) for g in groups], self.size, self.size, pad)
                if pos is not None:
                    found = (mid, pos, k)
                    lo = mid
                else:
                    hi = mid
            if found is not None and found[0] >= 8.0:                        # the largest enlargement of the old islands that still leaves the new parts 8 texels per unit
                best = found
                break
            if best is None and found is not None:
                best = found
        if best is None:
            raise ValueError("%s: the old islands and the new parts do not fit one skin" % old.id)
        ppu, pos, k = best
        no = len(old.clusters)
        for c, (x, y) in zip(old.clusters, pos[:no]):
            c["pos"], c["k"] = (x, y), k
            c["W"], c["H"] = int(math.ceil(c["rect"][2] * k)), int(math.ceil(c["rect"][3] * k))
            c["fx"], c["fy"] = c["W"] / c["rect"][2], c["H"] / c["rect"][3]
        for g, (x, y) in zip(groups, pos[no:]):
            wd, ht = g.px_size(ppu)
            g.rect = (x, y, wd, ht)
            ml._assign_uv(g, ppu, self.k)
        self.groups, self.ppu, self.old_k = groups, ppu, k
        # paste the enlarged old islands (skin and core glow) and remap the old quads' UV
        for c in old.clusters:
            x0, y0, wd, ht = c["rect"]
            nx, ny = c["pos"]
            for src, dst in ((old.skin, self.img), (old.glow, self.glow)):
                patch = upsample(src[y0:y0 + ht, x0:x0 + wd] * c["mask"][..., None], c["W"], c["H"])
                dst[ny:ny + c["H"], nx:nx + c["W"]] = patch
            for qi in c["quads"]:
                q = old.quads[qi]
                px = q["UV"] * K_ATLAS
                q["UV2"] = np.stack([(nx + (px[:, 0] - x0) * c["fx"]) / K_ATLAS, (ny + (px[:, 1] - y0) * c["fy"]) / K_ATLAS], axis=1)
        return ppu


# ================================================================ painting helpers for the old islands
def _erode_distance(mask, maxd=5):
    """Chebyshev distance (1..maxd) of every set texel from the outside of the mask."""
    d = np.zeros(mask.shape, np.int8)
    cur = mask.copy()
    for i in range(1, maxd + 1):
        d[cur] = i
        e = cur.copy()
        e[1:] &= cur[:-1]
        e[:-1] &= cur[1:]
        e[:, 1:] &= cur[:, :-1]
        e[:, :-1] &= cur[:, 1:]
        cur = e
        if not cur.any():
            break
    return d


def detail_old(atlas, old, amount=1.0):
    """The generic skin detail over the enlarged old islands (what gb.detail_pass does for the new parts): a fine grain, a dark ambient-occlusion band along the island edge, a light bevel rim
    just inside it, hairline scratches. Works on the painted texels of each island, whatever its shape."""
    for ci, c in enumerate(old.clusters):
        nx, ny = c["pos"]
        x0, y0, wd, ht = c["rect"]
        W, H = c["W"], c["H"]
        view = atlas.img[ny:ny + H, nx:nx + W]
        m = view[..., 3] > 128
        if m.sum() < 30:
            continue
        rng = np.random.default_rng(zlib.crc32(old.id.encode()) + 31 * ci)
        d = _erode_distance(m, 5).astype(np.float32)
        grain = (rng.random((H, W)).astype(np.float32) - 0.5) * 14.0 * amount
        ao = np.clip(1.0 - (d - 1.0) / 3.0, 0.0, 1.0) * m * 0.30 * amount
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        rim = (np.abs(d - 3.0) < 0.6) * m * ((xx + yy) < (W + H) * 0.5) * 0.10 * amount
        col = view[..., :3]
        col += (grain * m)[..., None]
        col *= (1.0 - ao)[..., None]
        col += (255.0 - col) * rim[..., None]
        if m.sum() > 900:
            for _ in range(int(2 + m.sum() / 2500)):
                ang = rng.uniform(0, math.pi)
                cx, cy = rng.uniform(0, W), rng.uniform(0, H)
                ln = rng.uniform(5, 16)
                t = np.abs((xx - cx) * math.sin(ang) - (yy - cy) * math.cos(ang))
                s = (xx - cx) * math.cos(ang) + (yy - cy) * math.sin(ang)
                mm = np.clip(1.0 - t / 0.7, 0, 1) * (np.abs(s) < ln) * m
                col += (230.0 - col) * (mm * 0.14 * amount)[..., None]
        np.clip(view[..., :3], 0, 255, out=view[..., :3])


def paint_old_veins(atlas, g2, old, rgb, density=0.5, seed=0, strength=0.9):
    """Energy veins on the enlarged old islands: thin ridges of a noise field (the zero set of two sines warped by noise), lit in the Djinn's colour on the second emissive map, only on
    texels that are painted and at least 2 texels inside their island."""
    for ci, c in enumerate(old.clusters):
        nx, ny = c["pos"]
        x0, y0, wd, ht = c["rect"]
        W, H = c["W"], c["H"]
        view = atlas.img[ny:ny + H, nx:nx + W]
        m = view[..., 3] > 128
        if m.sum() < 400:
            continue
        d = _erode_distance(m, 3)
        rng = np.random.default_rng(zlib.crc32(old.id.encode()) * 7 + 977 * ci + seed)
        yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
        ph1, ph2 = rng.uniform(0, 6.28, 2)
        f1, f2 = rng.uniform(0.05, 0.11), rng.uniform(0.04, 0.09)
        warp = nl_noise(H, W, 14.0, rng) * 5.0
        v = np.sin((xx * f1 + yy * f2 * 0.6 + warp) + ph1) * np.sin((yy * f1 * 0.8 - xx * f2 * 0.5 + warp * 0.7) + ph2)
        ridge = np.clip(1.0 - np.abs(v) / 0.085, 0.0, 1.0) * (rng.random() < 2.0)
        sel = ridge * m * (d >= 2) * (nl_noise(H, W, 22.0, rng) > (1.0 - density))
        if sel.max() <= 0.01:
            continue
        sel = np.asarray(Image.fromarray((sel * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.5)), np.float32) / 255.0
        a = g2[ny:ny + H, nx:nx + W]
        col = np.array(rgb, np.float32)
        hot = col + (255 - col) * 0.5
        out = np.zeros((H, W, 4), np.float32)
        out[..., :3] = col + (hot - col) * sel[..., None]
        out[..., 3] = np.clip(sel, 0, 1) * 255 * strength
        a[...] = np.maximum(a, out)


def nl_noise(h, wd, cell, rng):
    gh, gw = max(2, int(h / cell) + 2), max(2, int(wd / cell) + 2)
    g = rng.random((gh, gw)).astype(np.float32)
    return np.clip(np.asarray(Image.fromarray(g).resize((wd, h), Image.BICUBIC), np.float32), 0, 1)


def tint_old(atlas, old, rgb, k=0.22):
    """A slight colour cast of the Djinn's hue over the enlarged old islands, so the old and the new surfaces share one palette."""
    for c in old.clusters:
        nx, ny = c["pos"]
        x0, y0, wd, ht = c["rect"]
        view = atlas.img[ny:ny + c["H"], nx:nx + c["W"]]
        m = (view[..., 3] > 128)[..., None]
        lum = view[..., :3].mean(axis=2, keepdims=True) / 128.0
        tinted = np.clip(lum * np.array(rgb, np.float32), 0, 255)
        view[..., :3] = np.where(m, view[..., :3] * (1 - k) + tinted * k, view[..., :3])


# ================================================================ writers
class BothGlow:
    """has_glow for the writer: a quad also has a layer-2 (glow) twin if EITHER emissive map has a texel under it."""

    def __init__(self, atlas, g2):
        self.atlas, self.g2 = atlas, g2

    def has_glow(self, uv, thresh=2.0):
        k, size = K_ATLAS, SIZE
        u0, u1 = int(max(0, math.floor(uv[:, 0].min() * k))), int(min(size, math.ceil(uv[:, 0].max() * k) + 1))
        v0, v1 = int(max(0, math.floor(uv[:, 1].min() * k))), int(min(size, math.ceil(uv[:, 1].max() * k) + 1))
        return bool(self.atlas.glow[v0:v1, u0:u1, 3].max(initial=0) > thresh or self.g2[v0:v1, u0:u1, 3].max(initial=0) > thresh)


def _qn(layer, P, UV, N):
    return "qn %d %s" % (layer, " ".join("%.3f %.3f %.3f %.3f %.3f %.4f %.4f %.4f" % (*P[k], *UV[k], *N[k]) for k in range(4)))


def write_mesh(mesh_id, old, mesh, atlas, g2, body=True, comment=""):
    """weapon_meshes/up/<mesh_id>.txt: the old quads (new UV) + the new parts of `mesh` (body) -- or only `mesh`'s parts (an orbit piece). Glow twins by BothGlow."""
    os.makedirs(MESH_DIR, exist_ok=True)
    bg = BothGlow(atlas, g2)
    lines = ["# generated by tools/visual_upgrade/djinn_dark_weapons (release 1.108.0)" + comment]
    if body:
        lines.append("place %.5f %.5f %.3f %.6f %.3f" % tuple(old.head["place"]))
    else:
        lines.append("place 0.00000 0.00000 90.000 0.020000 0.000")
    lines.append("orient manual")
    if old.head["grip"] and body:
        lines.append(old.head["grip"])
    lines.append(old.head["glow"] or "glow FFFFFF 0.14 0.55 1.0")
    n = 0
    if body:
        for q in old.quads:
            if q["layer"] == 2:                                              # a glow-only part of the old weapon (energy arcs, flames)
                lines.append(_qn(2, q["P"], q["UV2"], q["N"]))
                continue
            lines.append(_qn(0, q["P"], q["UV2"], q["N"]))
            n += 1
            if q["twin"] or bg.has_glow(q["UV2"]):
                lines.append(_qn(2, q["P"], q["UV2"], q["N"]))
    for (layer, P, UV, N, _p) in mesh.iter_quads("vertex", bg):
        lines.append(_qn(layer, P, UV, N))
        n += layer == 0
    path = os.path.join(MESH_DIR, mesh_id + ".txt")
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    return path, n


def save_textures(mesh_id, atlas, g2):
    os.makedirs(TEX_DIR, exist_ok=True)
    out = {}
    for suffix, arr in (("", atlas.img), ("_glow", atlas.glow), ("_glow2", g2)):
        if suffix and arr[..., 3].max() <= 0:
            continue
        p = os.path.join(TEX_DIR, mesh_id + suffix + ".png")
        Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(p, optimize=True)
        out[suffix or "skin"] = p
    return out


def hash_seed(name, k=0):
    return (zlib.crc32(name.encode()) + 7919 * k) & 0xFFFFFFFF


# ================================================================ level of detail (god_melee_b's vertex clustering, extended for glow-only quads)
def decimate(quads, cell):
    """Vertex clustering at grid size `cell` (every vertex goes to the mean of its cell; quads with fewer than three distinct corners vanish; equal quads merge). A layer-2 quad on the
    same corners as the body quad before it is its glow twin (kept with it); any other layer-2 quad is a glow-only part and is clustered like a body quad."""
    acc = {}
    for q in quads:
        for c in range(4):
            key = tuple(np.floor(q[1][c] / cell).astype(int))
            a = acc.setdefault(key, [np.zeros(3), 0])
            a[0] += q[1][c]
            a[1] += 1
    cen = {k: v[0] / v[1] for k, v in acc.items()}
    out, seen = [], set()
    prev = None                                                      # (original corners, kept entry or None) of the last body quad
    for q in quads:
        keys = [tuple(np.floor(q[1][c] / cell).astype(int)) for c in range(4)]
        if q[0] == 2 and prev is not None and np.allclose(q[1], prev[0], atol=1e-3):
            if prev[1] is not None:
                out.append([2, prev[1][1], prev[1][2], prev[1][3]])
            continue
        if len(set(keys)) < 3:
            if q[0] != 2:
                prev = (q[1], None)
            continue
        sig = (q[0], gb._canon(keys))
        if sig in seen:
            if q[0] != 2:
                prev = (q[1], None)
            continue
        seen.add(sig)
        entry = [q[0], np.array([cen[k] for k in keys]), q[2], q[3]]
        out.append(entry)
        if q[0] != 2:
            prev = (q[1], entry)
    return out


def make_lod(src_path, dst_path, fraction=LOD_FRACTION):
    """Writes the level-of-detail twin of a mesh file: the SMALLEST clustering cell that keeps at most `fraction` of the body quads. Returns (lod body quads, source body quads)."""
    head, quads = gb.read_game_mesh(src_path)
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
    lines.insert(0, "# level of detail of %s (vertex clustering, release 1.108.0)" % os.path.basename(src_path))
    for layer, P_, UV, N in best:
        lines.append(_qn(layer, P_, UV, N))
    open(dst_path, "w", encoding="utf-8", newline="\n").write("\n".join(lines) + "\n")
    return sum(1 for q in best if q[0] in (0, 1)), body

"""The item builder of the genesis_weapons upgrade: parts, groups, the atlas, painting, the writer of `weapon_meshes_vu/<id>.txt` and the effect spec.

An item module defines `build(it)` and fills an `Item` with parts. The same function is called twice: once at full detail and once for the level-of-detail mesh
(`it.lod` is set, `it.D` is small, `it.full` is False: ornaments that only matter up close are skipped by `if it.full:`).

Mesh conventions are the game's (tools/relics/mesh_lib.py, docs/weapon_scale_orientation.md): centimetres, y along the weapon (butt low, head high), the cutting edge
toward -x, the flat faces +z. The new mesh is placed with the `place` line of the old one (so every display transform, oversize rule and bow display still fits) and
the same unit frame (core y range = the old mesh's y range).
"""
import math
import os
import sys
import zlib

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.path.insert(0, os.path.join(HERE, "..", "..", "relics"))
import mesh_lib as ml                                                    # noqa: E402
from common import *                                                     # noqa: E402,F401,F403
import vu_tex as tx                                                      # noqa: E402

LOD_D = 0.14
FULL_D = 1.0
HALO_CELLS = {"glow": 0, "ring": 1, "thick_ring": 2, "star": 3, "shaft": 4, "streak": 5, "rays": 6, "diamond": 7, "crescent": 8, "runes": 9,
              "bolt": 10, "feather": 11, "flame": 12, "hex": 13, "sparkle": 14, "solid": 15}


class VuAtlas(ml.Atlas):
    """mesh_lib's atlas with a density per part (small ornaments and blade faces get more texels per cm than a haft), a vein layer and halo parts that take no skin space."""

    def __init__(self, size):
        super().__init__(size, bleed=1, pad=4)
        self.veins = np.zeros((size, size, 4), np.float32)

    def layout(self, mesh, ppu=None, mode="dominant", separate_signs=True, ppu_max=60.0):
        mesh.finish()
        groups = []
        for p in mesh.parts:
            if p.layer != 0:
                p.UV = [[(0.0, 0.0)] * 4 for _ in p.F]
                p.groups = []
                continue
            groups += ml.plan_uv(p, mode, separate_signs)
            p.UV = [None] * len(p.F)
        dens = [getattr(g.part, "density", 1.0) for g in groups]
        lo, hi = 0.05, ppu_max
        best = None
        for _ in range(24):
            mid = ppu if ppu else (lo + hi) / 2
            pos = ml.pack_shelf([g.px_size(mid * d) for g, d in zip(groups, dens)], self.size, self.size, self.pad)
            if pos is not None:
                best = (mid, pos)
                if ppu:
                    break
                lo = mid
            else:
                if ppu:
                    break
                hi = mid
        if best is None:
            raise ValueError("the parts do not fit the skin: lower the part count or raise the atlas size")
        mid, pos = best
        for g, d, (x, y) in zip(groups, dens, pos):
            w, h = g.px_size(mid * d)
            g.rect = (x, y, w, h)
            ml._assign_uv(g, mid * d, self.k)
        self.groups, self.ppu = groups, mid
        return mid

    def has(self, buf, uv, thresh=24.0):
        k = self.k
        u0, u1 = int(max(0, math.floor(uv[:, 0].min() * k))), int(min(self.size, math.ceil(uv[:, 0].max() * k) + 1))
        v0, v1 = int(max(0, math.floor(uv[:, 1].min() * k))), int(min(self.size, math.ceil(uv[:, 1].max() * k) + 1))
        return bool(buf[v0:v1, u0:u1, 3].max(initial=0) > thresh)


class Item:
    def __init__(self, item_id, lod=False, tex=1024, mesh_id=None):
        self.id = item_id
        self.mesh_id = mesh_id or item_id
        self.lod = lod
        self.full = not lod
        self.D = LOD_D if lod else FULL_D
        self.tex = 256 if lod else tex
        self.mesh = ml.Mesh(self.mesh_id)
        self.groups = {}                 # name -> dict (pivot, anim, tint, lodSkip, draw)
        self.flares = []                 # (x, y, z, size, rgb, cell, phase)
        self.layers = []                 # the ambient effect layers of the spec
        self.idle = []
        self.draw = None
        self.swing = None
        self.acts = {}
        self.sound = None
        self.glow = ("FFFFFF", 0.14, 0.55, 1.0)
        self.core = None                 # (ymin, ymax): the core extent the placement matches to the old mesh
        self.notes = []
        self.rng = np.random.default_rng(zlib.crc32(item_id.encode()))

    # ---- detail scaling
    def n(self, k, lo=3):
        """A count (sides, stations, repeats) scaled by the detail level; full detail returns k."""
        return int(k) if self.full else max(lo, int(round(k * self.D)))

    # ---- parts
    def part(self, name, material="steel", layer=0, group="", base=None, glow=False, smooth=45.0, uv_mode=None, along=None, wear=0.5, density=1.0, mat=None, painters=None):
        p = ml.Part(name, layer=layer, material=material, base=base, glow=glow, smooth=smooth, uv_mode=uv_mode, along=along, wear=wear)
        p.group = group
        p.density = density
        p.mat = mat or {}
        p.painters = list(painters or [])
        p.cell = None
        p.tint = None
        self.mesh.add(p)
        return p

    def clone(self, src, name, matrix=None, offset=(0, 0, 0), group=None):
        c = src.transformed(matrix, offset, name)
        for a in ("group", "density", "mat", "painters", "cell", "tint"):
            setattr(c, a, getattr(src, a))
        if group is not None:
            c.group = group
        self.mesh.add(c)
        return c

    def define_group(self, name, pivot=(0, 0, 0), anim=None, tint=None, lod_skip=True, draw=None, alpha=1.0):
        """An animated set of parts (every part with `group=name`). anim = [{'kind': 'spin'|'bob'|'sway'|'pulse'|'flicker', ...}] (see UpgradeSpec)."""
        g = {"pivot": [round(float(v), 3) for v in pivot], "anim": anim or [], "lodSkip": bool(lod_skip)}
        if tint:
            g["tint"] = tint
        if draw:
            g["draw"] = draw
        if alpha != 1.0:
            g["alpha"] = alpha
        self.groups[name] = g

    def flare(self, pos, size, rgb="FFFFFF", cell="star", phase=0.0, pulse=0.35):
        if self.full:
            self.flares.append((round(pos[0], 3), round(pos[1], 3), round(pos[2], 3), round(size, 3), rgb, HALO_CELLS[cell], round(phase, 3), round(pulse, 3)))

    # ---- placement
    def place_like(self, old_mesh_path, y0=None, y1=None):
        d = parse_mesh(old_mesh_path)
        self.old_place = d["place"]
        self.core = (self.old_place[4] if y0 is None else y0, y1)
        return d

    # ---- the pipeline
    def finish(self, out_mesh_dir, out_tex_dir, painters_default=None, report=True):
        m = self.mesh
        m.finish()
        atlas = VuAtlas(self.tex)
        atlas.layout(m)
        for p in m.parts:
            if p.layer != 0:
                continue
            for g in p.groups:
                ctx = tx.Ctx(atlas, p, g)
                tx.base_paint(ctx)
                for fn in p.painters:
                    fn(ctx)
        self.atlas = atlas
        return atlas

    def stats(self):
        q = {}
        for p in self.mesh.parts:
            q[p.name] = len(p.F)
        return q


# ================================================================ writing
def _fmt(v):
    return "%.3f" % v


def write_vu(it, atlas, path, skin_name, lod_name=None, place=None, comment=""):
    """Writes the item's mesh in the upgrade dialect (see client/relics/item/vu/VuMesh.java)."""
    m = it.mesh
    m.finish()
    lines = ["# %s: genesis_weapons visual upgrade (tools/visual_upgrade/genesis_weapons); %d parts %s" % (it.mesh_id, len(m.parts), comment), "vu 1"]
    ox, oy, ang, sc, ymin = place
    lines.append("place %.5f %.5f %.3f %.6f %.3f" % (ox, oy, ang, sc, ymin))
    lines.append("glow %s %s %s %s" % it.glow)
    lines.append("skin " + skin_name)
    if lod_name:
        lines.append("lod " + lod_name)
    for (x, y, z, size, rgb, cell, ph, pu) in it.flares:
        lines.append("fl %.3f %.3f %.3f %.3f %s %d %.3f %.3f" % (x, y, z, size, rgb, cell, ph, pu))
    order = []
    for p in m.parts:
        if p.group not in order:
            order.append(p.group)
    counts = {0: 0, 1: 0, 2: 0, 3: 0, 4: 0}
    for g in order:
        lines.append("g " + (g or "-"))
        for p in m.parts:
            if p.group != g:
                continue
            for f, idx in enumerate(p.F):
                P = np.array([p.V[i] for i in idx])
                N = np.array(p.N[f])
                if p.layer == 0:
                    UV = np.array(p.UV[f])
                    emit = [0]
                    if p.glow or atlas.has(atlas.glow, UV):
                        emit.append(2)
                    if atlas.has(atlas.veins, UV):
                        emit.append(3)
                elif p.layer == 1:
                    UV = np.zeros((4, 2))
                    emit = [1]
                else:
                    UV = np.array(p.cardUV[f])
                    N = np.tile(np.array([0.0, 0.0, 1.0]), (4, 1))
                    emit = [4]
                for layer in emit:
                    counts[layer] += 1
                    lines.append("qn %d %s" % (layer, " ".join("%s %s %s %.3f %.3f %.3f %.3f %.3f" % (_fmt(P[k][0]), _fmt(P[k][1]), _fmt(P[k][2]), UV[k][0], UV[k][1], N[k][0], N[k][1], N[k][2]) for k in range(4))))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    return counts


def save_png(arr, path, half=False):
    img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    if half:
        img = img.resize((img.width // 2, img.height // 2), Image.BOX)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, optimize=True)
    return path


def save_textures(atlas, tex_dir, name, half_glow=True):
    """skin <name>.png at full size; <name>_glow.png and <name>_veins.png at half size (the mesh UVs are normalised, so the sizes may differ)."""
    out = [save_png(atlas.img, os.path.join(tex_dir, name + ".png"))]
    if atlas.glow[..., 3].max() > 0:
        out.append(save_png(atlas.glow, os.path.join(tex_dir, name + "_glow.png"), half_glow))
    if atlas.veins[..., 3].max() > 0:
        out.append(save_png(atlas.veins, os.path.join(tex_dir, name + "_veins.png"), half_glow))
    return out

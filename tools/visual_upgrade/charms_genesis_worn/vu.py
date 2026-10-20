"""vu: the model builder of the visual upgrade. One `VU` is one model (its parts, animation groups, additive fx quads, starfield parts), laid out on its own skin and
written in the quad format of the worn relic library (tools/relics/gen_worn_lib.py) with the extra header records the game reads for a visual upgrade model:

    skin <path>  glowtex <path>  startex <path>  lod <model>          (paths under assets/nusmp/textures/, the lod model as vu/<name>)
    q <group> <slot> <flags> x y z u v nx ny nz (x4)                    flags 1 emissive 2 shell 4 hot (pulses) 8 GLOWMAP 16 STAR 32 HALO (see WornMesh)

A part is a mesh_lib Part drawn with the skin (+ the glow map where its quads have light under them); a star part is drawn only in the scrolling nebula pass; an fx quad is
a flat soft-light shape (ring, rune ring, beam, flare, portal ripple ...) drawn additively and tinted by its colour slot. Every one belongs to an animation group.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import geo  # noqa: E402
import fxtex  # noqa: E402
import metrics as MT  # noqa: E402
ml = geo.ml

EMIT, SHELL, HOT, GLOWMAP, STAR, HALO = 1, 2, 4, 8, 16, 32
MAIN, TRIM, GLOW, DARK, ACCENT, NONE = range(6)
KINDS = ("bob", "spin", "swing", "sway", "pulse", "wave", "orbit", "tumble")

ASSETS = MT.ASSETS
MODEL_DIR = MT.VU_MODELS
TEX_DIR = MT.TEX_VU


class VU:
    def __init__(self, rid, lod=False, skin=512, seed=1):
        self.rid, self.lod, self.size, self.seed = rid, lod, skin, seed
        self.mesh = ml.Mesh(rid)
        self.groups = [None]
        self.pgroup, self.pslot, self.pflags, self.plod = {}, {}, {}, {}
        self.stars = []                  # (Part, group)
        self.fxq = []                    # dict(group, slot, P (4x3), uv (4x2), n (3), double)
        self.decos = []
        self.atlas = None
        self._names = set()
        self.layers = []                 # the ambient layers this model declares (written into the fx json by the item)
        self.pal = None                  # (main, trim, glow) of the relic: the additive quads are toned by it

    # ------------------------------------------------------------ construction
    def S(self, n, lo=3, f=None):
        """A segment count: n at full detail, about a third of it (never below `lo`) in the lod model."""
        if not self.lod:
            return int(n)
        return max(lo, int(round(n * (f if f is not None else 0.34))))

    def L(self, full, low=0):
        """A repeat count: `full` at full detail, `low` in the lod model."""
        return low if self.lod else full

    def grp(self, name, kind, pivot=(0, 0, 0), a=0.0, b=0.0, c=0.0):
        assert kind in KINDS, kind
        self.groups.append(dict(name=name, kind=kind, pivot=tuple(float(x) for x in pivot), a=a, b=b, c=c))
        return len(self.groups) - 1

    def P(self, name, material="steel", group=0, base=None, slot=MAIN, flags=0, glow=False, uv="loft", wear=0.5, smooth=35.0, lod=False):
        """A new skin part (not yet filled): build into it with the mesh_lib / geo helpers. lod=True keeps it in the lod model (the far silhouette); the lod model
        drops every other part, so mark only the pieces that make the shape."""
        n, k = name, 2
        while n in self._names:
            n = "%s_%d" % (name, k)
            k += 1
        self._names.add(n)
        p = geo.Part(n, material=material, base=base, glow=glow, smooth=smooth, uv_mode=uv, wear=wear)
        self.mesh.add(p)
        self.pgroup[n] = group
        self.pslot[n] = slot
        self.pflags[n] = flags
        self.plod[n] = lod
        return p

    def star(self, name, group=0, lod=True):
        """A part drawn only in the nebula pass (a core, a cosmos in a gem, a sky). lod=False leaves it out of the lod model."""
        p = geo.Part("star_" + name, material="steel")
        self.stars.append((p, group))
        self.plod["star_" + name] = lod
        return p

    def deco(self, fn, *a, **kw):
        """Registers a painter (paint.deco_*) to run on the laid out skin."""
        self.decos.append((fn, a, kw))

    # ------------------------------------------------------------ additive fx quads
    def _lum(self, slot):
        """Luminance 0..1 of a colour slot of the relic as the game computes it (WornPalette.slots: dark = main x 0.38, accent = the mean of trim and glow)."""
        if self.pal is None:
            return 0.5
        main, trim, glow = ((c >> 16 & 255, c >> 8 & 255, c & 255) for c in self.pal)
        rgb = {MAIN: main, TRIM: trim, GLOW: glow, DARK: tuple(0.38 * x for x in main), ACCENT: tuple((a + b) / 2 for a, b in zip(trim, glow))}.get(slot, glow)
        return (0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]) / 255.0

    def tone(self, slot):
        """The additive layers add up, so a bright palette would burn white: take the wanted colour slot if its light is moderate, else the nearest slot that is."""
        if self.pal is None:
            return slot
        order = [slot] + [s for s in (GLOW, ACCENT, TRIM, DARK) if s != slot]
        for s in order:
            if 0.14 <= self._lum(s) <= 0.66:
                return s
        return min(order, key=lambda s: abs(self._lum(s) - 0.4))

    def fx_quad(self, group, slot, P4, cell, normal=(0, 0, 1), uvrot=0, double=True, lod=False):
        u0, v0, u1, v1 = fxtex.cell_uv(cell)
        uv = [(u0, v0), (u1, v0), (u1, v1), (u0, v1)]
        uv = uv[uvrot % 4:] + uv[:uvrot % 4]
        slot = self.tone(slot)
        self.fxq.append(dict(group=group, slot=slot, P=np.array(P4, float), uv=np.array(uv), n=geo.unit(normal), double=double, lod=lod))

    def fx_disc(self, group, slot, center, normal, size, cell, rot=0.0, lod=False):
        """One flat square of side `size` (a halo ring, a rune ring, a ripple, a lens flare) in the plane perpendicular to `normal`."""
        u, v, n = geo.basis(normal)
        a = math.radians(rot)
        u, v = u * math.cos(a) + v * math.sin(a), -u * math.sin(a) + v * math.cos(a)
        c = geo.v3(center)
        h = size / 2
        self.fx_quad(group, slot, [c - u * h - v * h, c + u * h - v * h, c + u * h + v * h, c - u * h + v * h], cell, n, lod=lod)

    def fx_cross(self, group, slot, center, axis, size, cell, n=3, rot=0.0, lod=False):
        """n squares through the axis at equal angles (a flare or a spark that reads from every side)."""
        u, v, a = geo.basis(axis)
        for i in range(n):
            th = math.radians(rot + 180.0 * i / n)
            nrm = u * math.cos(th) + v * math.sin(th)
            self.fx_disc(group, slot, center, nrm, size, cell, rot=0.0, lod=lod and i < 2)

    def fx_beam(self, group, slot, base, tip, width, cell="beam", n=2, lod=False):
        """A light shaft from base to tip: n crossed quads, the bright core across u, fading at both ends along v."""
        b, t = geo.v3(base), geo.v3(tip)
        d = geo.unit(t - b)
        u, v, _ = geo.basis(d)
        for i in range(n):
            th = math.pi * i / n
            side = (u * math.cos(th) + v * math.sin(th)) * width / 2
            nrm = geo.unit(np.cross(side, d))
            self.fx_quad(group, slot, [b - side, b + side, t + side, t - side], cell, nrm, uvrot=3, lod=lod and i < 1)

    # ------------------------------------------------------------ build
    def build(self):
        m = self.mesh
        m.parts = [p for p in m.parts if p.F and (not self.lod or self.plod.get(p.name))]
        if self.lod:
            self.fxq = [q for q in self.fxq if q["lod"]]
            self.stars = [(p, g) for (p, g) in self.stars if p.F and self.plod.get(p.name, True)]
        m.finish()
        self.atlas = ml.Atlas(self.size, bleed=1, pad=2)
        self.atlas.layout(m, ppu_max=((14.0 if self.size <= 512 else 28.0) if not self.lod else 6.0))
        self.atlas.paint(m)
        for fn, a, kw in self.decos:
            kw = dict(kw)
            lodok = kw.pop("_lod", False)
            if self.lod and not lodok:
                continue
            fn(self, *a, **kw)
        for p, _g in self.stars:
            if p.F:
                p.ensure_outward()
                ml.smooth_normals(p, 35.0)
        return self

    # ------------------------------------------------------------ quads
    def _star_uv(self, p, f):
        n = p.face_normal(f)
        ax = int(np.argmax(np.abs(n)))
        out = []
        for i in p.F[f]:
            x, y, z = p.V[i]
            a, b = ((z, y) if ax == 0 else ((x, z) if ax == 1 else (x, y)))
            out.append((0.5 + a * 0.045, 0.5 + b * 0.045))
        return out

    def _lit(self, uv):
        """Does the glow map carry enough light under this quad to be worth redrawing it additively (some coverage, not a stray pixel)?"""
        a = self.atlas
        k = a.k
        u0, u1 = int(max(0, math.floor(uv[:, 0].min() * k))), int(min(a.size, math.ceil(uv[:, 0].max() * k) + 1))
        v0, v1 = int(max(0, math.floor(uv[:, 1].min() * k))), int(min(a.size, math.ceil(uv[:, 1].max() * k) + 1))
        reg = a.glow[v0:v1, u0:u1, 3]
        if reg.size == 0:
            return False
        return bool(reg.max() > 60 and (reg > 40).mean() > 0.12)

    def quads(self):
        """Every quad: dict(group, slot, flags, P, UV (0..1), N)."""
        out = []
        at = self.atlas
        for p in self.mesh.parts:
            g, s, fl0 = self.pgroup[p.name], self.pslot[p.name], self.pflags[p.name]
            for f, idx in enumerate(p.F):
                P = [p.V[i] for i in idx]
                UV = [(u / ml.TS, v / ml.TS) for (u, v) in p.UV[f]]
                N = p.N[f]
                fl = fl0
                if p.glow or self._lit(np.array(p.UV[f])):
                    fl |= GLOWMAP
                out.append(dict(group=g, slot=s, flags=fl, P=P, UV=UV, N=N))
        for p, g in self.stars:
            for f, idx in enumerate(p.F):
                out.append(dict(group=g, slot=MAIN, flags=STAR, P=[p.V[i] for i in idx], UV=self._star_uv(p, f), N=p.N[f]))
        for q in self.fxq:
            nrm = [q["n"]] * 4
            out.append(dict(group=q["group"], slot=q["slot"], flags=HALO, P=list(q["P"]), UV=[tuple(x) for x in q["uv"]], N=nrm))
            if q["double"]:
                out.append(dict(group=q["group"], slot=q["slot"], flags=HALO, P=[q["P"][0], q["P"][3], q["P"][2], q["P"][1]],
                                UV=[tuple(q["uv"][0]), tuple(q["uv"][3]), tuple(q["uv"][2]), tuple(q["uv"][1])], N=[-q["n"]] * 4))
        return out

    def part_counts(self):
        return {p.name: len(p.F) for p in self.mesh.parts} | {p.name: len(p.F) for p, _g in self.stars} | {"fx": len(self.fxq) * 2}

    def stats(self):
        qs = self.quads()
        return {"quads": len(qs), "glowmap": sum(1 for q in qs if q["flags"] & GLOWMAP), "star": sum(1 for q in qs if q["flags"] & STAR),
                "halo": sum(1 for q in qs if q["flags"] & HALO), "groups": len(self.groups) - 1, "parts": len(self.mesh.parts),
                "vertices": 4 * len(qs), "drawn": 4 * (len(qs) + sum(1 for q in qs if q["flags"] & GLOWMAP))}

    # ------------------------------------------------------------ output
    def write_textures(self, name=None):
        name = name or self.rid
        os.makedirs(TEX_DIR, exist_ok=True)
        skin = np.clip(self.atlas.img, 0, 255).astype(np.uint8)
        Image.fromarray(skin, "RGBA").save(os.path.join(TEX_DIR, name + ".png"), optimize=True)
        gl = np.clip(self.atlas.glow, 0, 255).astype(np.uint8)
        Image.fromarray(gl, "RGBA").save(os.path.join(TEX_DIR, name + "_glow.png"), optimize=True)
        return int((skin[..., 3] > 0).sum()), int(((gl[..., :3].max(axis=2) > 8) & (gl[..., 3] > 0)).sum())

    def write_model(self, file_name, tex_name, anchor="body", scale=1.0, offset=(0, 0, 0), star_tex=None, lod_name=None, comment=""):
        """Writes assets/nusmp/visual_upgrade/models/<file_name>.txt. scale/offset move the model (a worn copy of a hand model)."""
        os.makedirs(MODEL_DIR, exist_ok=True)
        qs = self.quads()
        off = geo.v3(offset)

        def tp(p):
            return geo.v3(p) * scale + off

        lines = ["# visual upgrade model %s: %d quads, %d vertices (%s); generated by tools/visual_upgrade/charms_genesis_worn, do not edit" % (file_name, len(qs), 4 * len(qs), comment),
                 "model vu/%s" % file_name, "anchor %s" % anchor, "skin visual_upgrade/%s.png" % tex_name, "glowtex visual_upgrade/%s_glow.png" % tex_name]
        if star_tex and any(q["flags"] & STAR for q in qs):
            lines.append("startex visual_upgrade/%s" % star_tex)
        if lod_name:
            lines.append("lod vu/%s" % lod_name)
        for gi in range(1, len(self.groups)):
            g = self.groups[gi]
            px, py, pz = tp(g["pivot"])
            a = g["a"] * (scale if g["kind"] == "bob" else 1.0)
            lines.append("grp %d %s %s %.3f %.3f %.3f %.4f %.4f %.4f" % (gi, g["name"], g["kind"], px, -py, pz, a, g["b"], g["c"]))
        for q in qs:
            rec = []
            for k in range(4):
                x, y, z = tp(q["P"][k])
                nx, ny, nz = q["N"][k]
                rec.append((x, -y, z, q["UV"][k][0], q["UV"][k][1], nx, -ny, nz))
            rec = [rec[0], rec[3], rec[2], rec[1]]
            lines.append("q %d %d %d %s" % (q["group"], q["slot"], q["flags"], " ".join("%.3f %.3f %.3f %.4f %.4f %.3f %.3f %.3f" % r for r in rec)))
        with open(os.path.join(MODEL_DIR, file_name + ".txt"), "w", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")
        return len(qs)


def mesh_bounds(vm):
    pts = [v for p in vm.mesh.parts for v in p.V]
    for p, _ in vm.stars:
        pts += list(p.V)
    for q in vm.fxq:
        pts += list(q["P"])
    a = np.array(pts)
    return a.min(axis=0), a.max(axis=0)

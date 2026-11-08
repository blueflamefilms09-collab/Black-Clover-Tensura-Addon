#!/usr/bin/env python3
"""Minecraft-style detailed forms (3.61.0): many small, angled, layered cubes with shaped silhouettes (stepped curves, layered fur, feathers, plates with trim), painted with tone tiles.
Box -> six cards for the preview; the geo export writes the same boxes as real cubes (forms_export.py). Frame: whole-body file frame (1/16 block, y up, feet 0, front -z, +x left)."""
import math, sys
import numpy as np
sys.dont_write_bytecode = True
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from forms_lib import *
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
from firecast_geo_lib import frame as _frame, rot_matrix, euler_of

TEXD = 3.0       # texels of a tone tile per model unit (the tile is mapped in windows so small cubes keep small, sharp texels)


class Box:
    __slots__ = ("part", "bone", "c", "R", "s", "mat", "dt", "faces", "glow")

    def __init__(s, part, bone, c, R, size, mat, dt=0, faces=None):
        s.part, s.bone, s.c, s.R, s.s, s.mat, s.dt, s.faces = part, bone, V(*c), np.asarray(R, float), V(*size), mat, dt, faces


class Scene:
    def __init__(s, sheet):
        s.sh = sheet; s.boxes = []; s.cards_extra = []; s.bonedefs = {}; s.cur = "root"

    def def_bone(s, name, pivot, parent=None, anim=()):
        """an animated group: pivot in the file frame, an optional parent bone, anim = [(kind 'rot'|'scale', axis 0..2, amplitude, cycles per 4.8 s clip, phase 0..1)]"""
        s.bonedefs[name] = {"pivot": tuple(float(v) for v in pivot), "parent": parent, "anim": list(anim)}

    def in_bone(s, name):
        """with sc.in_bone('mane1'): every box made inside belongs to that bone"""
        sc = s

        class _Ctx:
            def __enter__(self): self.prev = sc.cur; sc.cur = name
            def __exit__(self, *a): sc.cur = self.prev
        return _Ctx()

    # -------------------------------------------------------------- primitives
    def cube(s, part, center, size, mat, rot=None, dt=0, bone=None, faces=None):
        """rot: None, (rx, ry, rz) degrees (file order) or a 3x3 matrix whose columns are the local axes. size = local (x, y, z)."""
        s.sh.mat(mat)
        if rot is None: R = np.eye(3)
        elif len(np.shape(rot)) == 2: R = np.asarray(rot, float)
        else: R = rot_matrix(*rot)
        b = Box(part, bone or s.cur, center, R, size, mat, dt, faces)
        s.boxes.append(b)
        return b

    def seg(s, part, p0, p1, w, h, mat, up=(0, 1, 0), pad=0.0, dt=0, bone=None, roll=0.0, faces=None):
        """a box laid from p0 to p1 (local z along it, `pad` longer), w wide (local x) and h high (local y)"""
        p0, p1 = V(*p0), V(*p1); n = float(np.linalg.norm(p1 - p0))
        R = _frame(p1 - p0, up, roll)
        return s.cube(part, (p0 + p1) / 2, (w, h, n + pad), mat, R, dt, bone, faces)

    def chain(s, part, pts, ws, hs, mat, up=(0, 1, 0), pad=.5, dts=None, bone=None, mat2=None):
        """stepped curve: one box per leg of the polyline, widths/heights per point (taper); mat2 alternates (banding)."""
        for i in range(len(pts) - 1):
            at = lambda a, j: a if np.isscalar(a) else a[min(j, len(a) - 1)]
            w = (at(ws, i) + at(ws, i + 1)) / 2
            h = (at(hs, i) + at(hs, i + 1)) / 2
            m = mat2 if (mat2 is not None and i % 2) else mat
            bn = bone[min(i, len(bone) - 1)] if isinstance(bone, (list, tuple)) else bone
            s.seg(part, pts[i], pts[i + 1], w, h, m, up, pad, 0 if dts is None else dts[i], bn)

    # -------------------------------------------------------------- expansion
    def cards(s):
        out = []
        for k, b in enumerate(s.boxes):
            X, Y, Z = b.R[:, 0], b.R[:, 1], b.R[:, 2]; sx, sy, sz = b.s
            for fi, (ax, ra, ua, fw, fh, sg) in enumerate(((X, Z, Y, sz, sy, 1), (X, Z, Y, sz, sy, -1), (Y, X, Z, sx, sz, 1), (Y, X, Z, sx, sz, -1), (Z, X, Y, sx, sy, 1), (Z, X, Y, sx, sy, -1))):
                if b.faces is not None and fi not in b.faces: continue
                nrm = ax * sg
                half = (sx, sy, sz)[(0, 0, 1, 1, 2, 2)[fi]] / 2
                kk = int(np.clip(_tone_k(nrm) + b.dt, 0, TONES - 1))
                n = b.mat.size
                fwn = min(1.0, max(fw * TEXD / n, .12)); fhn = min(1.0, max(fh * TEXD / n, .12))
                ox = ((k * 37 + fi * 11) % 9) / 9 * (1 - fwn); oy = ((k * 53 + fi * 7) % 9) / 9 * (1 - fhn)
                out.append(Card(b.c + nrm * half, ra, ua, fw, fh, "t_%s_%d" % (b.mat.name, kk), (ox, oy, ox + fwn, oy + fhn), b.part, b.bone))
        return out + s.cards_extra


def _tone_k(nrm):
    d = float(np.dot(unit(nrm), LIGHT))
    return int(np.clip(round((d + .6) / 1.5 * (TONES - 1)), 0, TONES - 1))


def mirror_x(sc, part_from, part_to, bones=None):
    """add a mirrored copy (x -> -x) of the boxes of one part under another part name (left/right pairs)"""
    new = []
    for b in list(sc.boxes):
        if b.part != part_from: continue
        M = np.diag([-1., 1., 1.])
        R = M @ b.R @ np.diag([-1., 1., 1.])        # keep a proper rotation: mirror the centre and flip the local x
        new.append(Box(part_to, b.bone, M @ b.c, R, b.s, b.mat, b.dt, b.faces))
    sc.boxes += new

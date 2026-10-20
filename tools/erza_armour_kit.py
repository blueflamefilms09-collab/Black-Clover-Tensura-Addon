"""Geometry kit of the Erza armour remake (1.99.0): GeoLite pieces made of many small oriented boxes, with a texture sheet per surface at the Djinn resolution or higher.

    from erza_armour_kit import Piece, Mat, Decal, ...
    p = Piece("erza_heart_kreuz", "chest", density=10, skin="body")
    p.shell("root", S, nu, nv, mat, decals=[Decal("heart", ...)])            # a curved plate: a grid of thin panels on the parametric surface S(u, v), one painted SHEET over all of them
    p.cbox("root", (0, 18, -3), (3, 2, 1), mat, eul=(10, 0, 0))               # a box by its centre, rotated (degrees: x first, then y, then z, about the centre)
    p.oriented("root", centre, direction, normal, length, width, thick, mat)  # a blade / feather / spike segment whose long axis follows `direction`
    p.save(anim)                                                              # culls hidden faces, packs the atlas, paints every face and sheet, writes geo + texture + glow (+ animation)

Frame of the files (the same as tools/geo_builder.py and tools/water_aura_lib.py): model pixels = 1/16 block, y UP, origin at the feet, the front on -z, +x the model's LEFT (the right arm is at
x = -5, the head is y 24 .. 32). A pair is built by mirroring x (side = 0 right, 1 left) with `mx`.

Rotation of a cube (client/geo/GeoDraw): in the file's y-up frame a cube turns by R = Rz(c) . Ry(b) . Rx(a) about its pivot (degrees a, b, c): a point is turned about x first, then y, then z.

SHEETS. A shell is a grid of flat panels that follow a curved surface (a breastplate, a dome, a tube); each panel is a thin box whose OUTER face is mapped to its own cell of ONE painted sheet, so an emblem, a
trim line or a row of rivets runs across the panels without a seam, and the texture density is the same everywhere (`density` texels per model pixel; the Djinn's atlas is 5.4 to 7.3). Only the outer face
of a panel is written, plus the thickness faces on the rim of the sheet; the faces the player can never see are not written at all, so the drawn polygon count is the honest number the audit compares
with the Djinn.

On the north face of a box the texture's u runs toward +x and v runs down; a shell builds each panel so its local x is the direction to the viewer's right (seen from outside) and its local y the up
tangent, so a sheet is always read the way it is painted.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import ROOT, Anim  # noqa: E402
import erza_armour_paint as PT  # noqa: E402
from erza_armour_paint import Mat, Decal  # noqa: E402,F401
import water_aura_lib as WL  # noqa: E402

sine_rot, spin_y, loop_rot = WL.sine_rot, WL.spin_y, WL.loop_rot

FACES = ("west", "north", "east", "south", "up", "down")
BEDROCK = {"west": "east", "north": "north", "east": "west", "south": "south", "up": "up", "down": "down"}
NORMALS = {"west": (-1, 0, 0), "east": (1, 0, 0), "north": (0, 0, -1), "south": (0, 0, 1), "up": (0, 1, 0), "down": (0, -1, 0)}
# the vanilla player's skin boxes in the body frame (feet y = 0): what a solid overlay has to stand off from
SKIN = {"head": ((-4, 24, -4), (4, 32, 4)), "body": ((-4, 12, -2), (4, 24, 2)), "arm_r": ((-8, 12, -2), (-4, 24, 2)), "arm_l": ((4, 12, -2), (8, 24, 2)),
        "leg_r": ((-3.9, 0, -2), (0.1, 12, 2)), "leg_l": ((-0.1, 0, -2), (3.9, 12, 2))}
OUT = ROOT
V3 = np.array


# ====================================================================================================================== maths
def rot_xyz(a, b, c):
    """The rotation matrix of a cube turned by (a, b, c) degrees: Rz(c) . Ry(b) . Rx(a)."""
    a, b, c = math.radians(a), math.radians(b), math.radians(c)
    ca, sa, cb, sb, cc, sc = math.cos(a), math.sin(a), math.cos(b), math.sin(b), math.cos(c), math.sin(c)
    return np.array([[cc * cb, cc * sb * sa - sc * ca, cc * sb * ca + sc * sa],
                     [sc * cb, sc * sb * sa + cc * ca, sc * sb * ca - cc * sa],
                     [-sb, cb * sa, cb * ca]])


def euler_of(M):
    """(a, b, c) degrees with rot_xyz(a, b, c) == M."""
    b = -math.asin(max(-1.0, min(1.0, M[2][0])))
    if abs(math.cos(b)) > 1e-6:
        a = math.atan2(M[2][1], M[2][2])
        c = math.atan2(M[1][0], M[0][0])
    else:
        a = math.atan2(-M[1][2], M[1][1])
        c = 0.0
    return tuple(round(math.degrees(v), 3) for v in (a, b, c))


def unit(v):
    v = np.asarray(v, np.float64)
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def frame(length_dir, normal, length_axis=1, thick_axis=2):
    """The matrix whose columns are the images of the local x, y, z axes: the long axis along `length_dir`, the thin axis along `normal` (made square to it)."""
    d = unit(length_dir)
    n = np.asarray(normal, np.float64)
    n = n - d * float(np.dot(n, d))
    if np.linalg.norm(n) < 1e-6:                      # a normal along the length: pick any
        n = np.array([0.0, 0.0, 1.0]) if abs(d[2]) < 0.9 else np.array([1.0, 0.0, 0.0])
        n = n - d * float(np.dot(n, d))
    n = unit(n)
    wi = ({0, 1, 2} - {length_axis, thick_axis}).pop()
    cols = [None, None, None]
    cols[length_axis], cols[thick_axis] = d, n
    cyc = (length_axis, thick_axis, wi) in ((0, 1, 2), (1, 2, 0), (2, 0, 1))
    cols[wi] = np.cross(d, n) if cyc else np.cross(n, d)
    return np.stack(cols, axis=1)


def mx(x, side):
    """The x of a right-side authored value for a pair: side 0 = right (as authored), 1 = left (mirrored)."""
    return -x if side else x


def mv(p, side):
    return (mx(p[0], side), p[1], p[2])


def mrot(e, side):
    return (e[0], -e[1], -e[2]) if side else tuple(e)


def superellipse(theta, a, bf, bb=None, n=2.0):
    """The point (x, z) at angle theta (radians, 0 = the front, turning toward +x, pi = the back) of a superellipse: half width a, half depth bf at the front and bb at the back, exponent n."""
    bb = bf if bb is None else bb
    s, c = math.sin(theta), math.cos(theta)
    e = 2.0 / n
    return (a * math.copysign(abs(s) ** e, s), -(bf if c >= 0 else bb) * math.copysign(abs(c) ** e, c))


# ====================================================================================================================== cubes, sheets, pieces
class Sheet:
    """A painted surface: w x h model pixels at `d` texels per pixel, painted as one image (decals in 0..1 sheet coordinates)."""

    def __init__(self, piece, mat, w, h, decals=(), d=None, tag="sheet"):
        self.mat, self.decals = mat, tuple(decals)
        self.d = d or piece.density
        self.w_px, self.h_px = max(0.5, w), max(0.5, h)
        self.wt = max(4, int(math.ceil(self.w_px * self.d - 1e-6)))
        self.ht = max(4, int(math.ceil(self.h_px * self.d - 1e-6)))
        self.tag, self.pos, self.used = tag, None, False


class Cube:
    __slots__ = ("bone", "c", "s", "eul", "R", "mat", "d", "decals", "glass", "keep", "faces", "own", "facemap")

    def __init__(self, bone, c, s, eul, mat, d, decals, glass, keep):
        self.bone, self.c, self.s, self.eul = bone, np.asarray(c, np.float64), np.asarray(s, np.float64), tuple(eul)
        self.R = rot_xyz(*eul)
        self.mat, self.d, self.decals, self.glass, self.keep = mat, d, decals or {}, glass, keep
        self.faces = {f: True for f in FACES}
        self.own = {}                                    # face -> (x, y, w, h) texel rect of its own painted face
        self.facemap = {}                                # face -> (sheet, tx0, ty0, tx1, ty1): a window into a sheet


class Piece:
    """One model: bones with oriented boxes and shells; save() culls, packs, paints and writes."""

    def __init__(self, key, name, density=10.0, skin=None, glow_lift=1.0):
        self.key, self.name, self.density, self.skin, self.glow_lift = key, name, density, skin, glow_lift
        self.bones, self.order, self.cubes, self.sheets = {}, [], [], []
        self.bone("root", (0, 0, 0))
        self.report = {}

    @property
    def id(self):
        return f"{self.key}_arm_{self.name}"

    def bone(self, name, pivot=(0, 0, 0), parent=None, rotation=None):
        b = {"name": name, "pivot": [float(v) for v in pivot]}
        if parent:
            b["parent"] = parent
        elif name != "root":
            b["parent"] = "root"
        if rotation:
            b["rotation"] = [float(v) for v in rotation]
        self.bones[name] = b
        self.order.append(name)
        return b

    # ------------------------------------------------------------------ boxes
    def cbox(self, bone, centre, size, mat, eul=(0, 0, 0), decals=None, d=None, glass=False, keep=None):
        """A box by its centre. `decals`: {face: [Decal, ...]}. `keep`: faces that must be drawn whatever the culling says (a tuple of face names)."""
        cu = Cube(bone, centre, size, eul, mat, d or self.density, decals, glass, keep or ())
        self.cubes.append(cu)
        return cu

    def box(self, bone, origin, size, mat, rot=None, pivot=None, **kw):
        """The 1.93.0 call: `origin` is the minimum corner, `rot` degrees about `pivot` (default the bone's pivot)."""
        o = np.asarray(origin, np.float64)
        s = np.asarray(size, np.float64)
        c = o + s / 2.0
        if rot:
            pv = np.asarray(pivot if pivot is not None else self.bones[bone]["pivot"], np.float64)
            M = rot_xyz(*rot)
            c2 = pv + M @ (c - pv)
            return self.cbox(bone, c2, s, mat, eul=tuple(rot), **kw)
        return self.cbox(bone, c, s, mat, **kw)

    def oriented(self, bone, centre, direction, normal, length, width, thick, mat, length_axis=1, thick_axis=2, **kw):
        """A box whose long axis points along `direction` and whose thin axis along `normal` (a blade, a feather, a spike segment, a strap)."""
        size = [0.0, 0.0, 0.0]
        wi = ({0, 1, 2} - {length_axis, thick_axis}).pop()
        size[length_axis], size[thick_axis], size[wi] = length, thick, width
        M = frame(direction, normal, length_axis, thick_axis)
        return self.cbox(bone, centre, size, mat, eul=euler_of(M), **kw)

    def between(self, bone, p0, p1, width, thick, mat, normal=(0, 0, -1), **kw):
        """A strap / bar / spike segment from p0 to p1 (centre-line), `width` across and `thick` through (along `normal`)."""
        p0, p1 = np.asarray(p0, np.float64), np.asarray(p1, np.float64)
        d = p1 - p0
        L = float(np.linalg.norm(d))
        return self.oriented(bone, (p0 + p1) / 2.0, d, normal, L, width, thick, mat, **kw)

    def cube_count(self):
        return len(self.cubes)

    # ------------------------------------------------------------------ sheets and shells
    def sheet(self, mat, w, h, decals=(), d=None, tag=None):
        sh = Sheet(self, mat, w, h, decals, d, tag or "sheet%d" % len(self.sheets))
        self.sheets.append(sh)
        return sh

    def shell(self, bone, S, nu, nv, mat, thick=0.55, u_range=(0.0, 1.0), v_range=(0.0, 1.0), ref=None, decals=(), rim="lrtb", overlap=1.07, glass=False, d=None, sheet=None,
              rim_mat=None, tag=None, double=False):
        """A curved plate: nu x nv thin boxes following the surface S(u, v) -> (x, y, z) (the OUTER face lies on the surface), sharing one painted sheet.
        `ref`: a point (or a function of the surface point) that is inside the surface, to tell outside from inside; `rim`: which sides of the plate show their thickness (l r t b, seen from outside);
        `sheet`: a sheet to paint on (default: one is made from the plate's size); `double`: also write the inner face (a translucent plate that can be seen from both sides)."""
        u0, u1 = u_range
        v0, v1 = v_range
        ue = np.linspace(u0, u1, nu + 1)
        ve = np.linspace(v0, v1, nv + 1)
        G = [[np.asarray(S(u, v), np.float64) for v in ve] for u in ue]
        info = []
        for i in range(nu):
            for j in range(nv):
                uc, vc = (ue[i] + ue[i + 1]) / 2.0, (ve[j] + ve[j + 1]) / 2.0
                Pc = np.asarray(S(uc, vc), np.float64)
                Tu = (G[i + 1][j] + G[i + 1][j + 1] - G[i][j] - G[i][j + 1]) / 2.0
                Tv = (G[i][j + 1] + G[i + 1][j + 1] - G[i][j] - G[i + 1][j]) / 2.0
                n = unit(np.cross(Tu, Tv))
                if ref is not None:
                    rp = ref(Pc) if callable(ref) else np.asarray(ref, np.float64)
                    if np.dot(n, Pc - rp) < 0:
                        n = -n
                info.append((i, j, Pc, Tu, Tv, n))
        # u reversed? the panel's local x is n x up-tangent (the viewer's right); the sheet runs along it
        cu0 = info[len(info) // 2]
        rev = bool(np.dot(cu0[3], np.cross(cu0[5], unit(cu0[4]))) < 0)
        # arc lengths of the sheet (mid row / mid column) for uniform texel density
        jm, im = nv // 2, nu // 2
        wcell = [float(np.linalg.norm(G[i + 1][jm] - G[i][jm])) for i in range(nu)]
        hcell = [float(np.linalg.norm(G[im][j + 1] - G[im][j])) for j in range(nv)]
        wtot, htot = sum(wcell), sum(hcell)
        if sheet is None:
            sheet = self.sheet(mat, wtot, htot, decals, d, tag)
        sheet.used = True
        Wt, Ht = sheet.wt, sheet.ht
        xb = [0]
        for w_ in wcell:
            xb.append(xb[-1] + w_)
        xb = [int(round(v / wtot * Wt)) for v in xb]
        yb = [0]
        for h_ in hcell:
            yb.append(yb[-1] + h_)
        yb = [int(round(v / htot * Ht)) for v in yb]                # from the bottom row up
        made = []
        for (i, j, Pc, Tu, Tv, n) in info:
            w = float(np.linalg.norm(Tu)) * overlap
            h = float(np.linalg.norm(Tv)) * overlap
            M = frame(Tv, -n, 1, 2)
            cen = Pc - n * (thick / 2.0)
            te = min(thick, max(0.2, 0.9 * min(w, h)))                  # a sliver of a panel (the top of a dome) is never thicker than it is wide
            cen = Pc - n * (te / 2.0)
            cu = self.cbox(bone, cen, (w, h, te), mat, eul=euler_of(M), glass=glass, d=d)
            for f in FACES:
                cu.faces[f] = False
            cu.faces["north"] = True
            ii = (nu - 1 - i) if rev else i                           # sheet column of this panel, left to right as the viewer sees it
            ix0, ix1 = (xb[nu - 1 - i], xb[nu - i]) if rev else (xb[i], xb[i + 1])
            iy0, iy1 = Ht - yb[j + 1], Ht - yb[j]                    # row 0 is the top
            cu.facemap["north"] = (sheet, ix0, iy0, max(ix1, ix0 + 1), max(iy1, iy0 + 1))
            left, right = (i == nu - 1, i == 0) if rev else (i == 0, i == nu - 1)
            top, bottom = j == nv - 1, j == 0
            if "l" in rim and left:
                cu.faces["west"] = True
            if "r" in rim and right:
                cu.faces["east"] = True
            if "t" in rim and top:
                cu.faces["up"] = True
            if "b" in rim and bottom:
                cu.faces["down"] = True
            if double:
                cu.faces["south"] = True
                cu.facemap["south"] = (sheet, ix0, iy0, max(ix1, ix0 + 1), max(iy1, iy0 + 1))
            if rim_mat is not None:
                cu.mat = rim_mat
            made.append(cu)
        return made

    # ------------------------------------------------------------------ motion groups
    @staticmethod
    def _animated(anim):
        out = set()
        if anim is not None:
            for clip in anim.clips.values():
                out |= set(clip["bones"].keys())
        return out

    def _group(self, bone, animated):
        """The animated bones in the chain of a bone: boxes with the same chain move together."""
        chain, b = [], bone
        while b:
            if b in animated:
                chain.append(b)
            b = self.bones[b].get("parent")
        return tuple(sorted(chain))

    # ------------------------------------------------------------------ culling
    def _cull(self, anim):
        animated = self._animated(anim)
        groups = {}
        for i, cu in enumerate(self.cubes):
            groups.setdefault(self._group(cu.bone, animated), []).append(i)
        skin = SKIN.get(self.skin) if self.skin else None
        gi = np.linspace(-0.38, 0.38, 3)
        eps = 0.07
        for g, idx in groups.items():
            solid = [i for i in idx if not self.cubes[i].glass]
            if not solid:
                continue
            C = np.stack([self.cubes[i].c for i in solid])
            S = np.stack([self.cubes[i].s for i in solid]) / 2.0 - 0.012
            R = np.stack([self.cubes[i].R for i in solid])
            where = {i: k for k, i in enumerate(solid)}
            for i in idx:
                cu = self.cubes[i]
                if cu.glass:
                    continue
                for f in FACES:
                    if not cu.faces[f] or f in cu.keep or f in cu.facemap:
                        continue
                    n = np.array(NORMALS[f], np.float64)
                    ax = int(np.argmax(np.abs(n)))
                    ts = [a for a in range(3) if a != ax]
                    pts = []
                    for a in gi:
                        for b in gi:
                            q = np.zeros(3)
                            q[ax] = n[ax] * (cu.s[ax] / 2.0 + eps)
                            q[ts[0]], q[ts[1]] = a * cu.s[ts[0]], b * cu.s[ts[1]]
                            pts.append(cu.c + cu.R @ q)
                    pts = np.array(pts)                                         # 9 x 3
                    loc = np.einsum("njk,pnj->pnk", R, pts[:, None, :] - C[None, :, :])   # R^T (p - c)
                    inside = np.all(np.abs(loc) <= S[None, :, :], axis=2)                 # p x n
                    inside[:, where[i]] = False
                    hidden = inside.any(axis=1)
                    if skin is not None and not g:
                        lo, hi = skin
                        hidden |= np.all((pts >= np.array(lo)) & (pts <= np.array(hi)), axis=1)
                    if bool(hidden.all()):
                        cu.faces[f] = False

    # ------------------------------------------------------------------ validation
    def _validate(self):
        """Boxes with a face in the plane of a skin face, over it: z fighting. Returns a list of warnings."""
        warns = []
        if not self.skin:
            return warns
        lo, hi = (np.array(v, np.float64) for v in SKIN[self.skin])
        for cu in self.cubes:
            if cu.glass or any(abs(e) > 1e-6 for e in cu.eul):
                continue
            a0, a1 = cu.c - cu.s / 2.0, cu.c + cu.s / 2.0
            for ax in range(3):
                o = [k for k in range(3) if k != ax]
                if not (a0[o[0]] < hi[o[0]] and a1[o[0]] > lo[o[0]] and a0[o[1]] < hi[o[1]] and a1[o[1]] > lo[o[1]]):
                    continue
                for plane in (lo[ax], hi[ax]):
                    for edge in (a0[ax], a1[ax]):
                        if abs(edge - plane) < 0.03:
                            warns.append("%s: a face of the box at %s lies %.3f from the skin plane (axis %d)" % (self.id, np.round(cu.c, 2).tolist(), edge - plane, ax))
        return warns

    # ------------------------------------------------------------------ atlas
    @staticmethod
    def _dims(cu, f):
        sx, sy, sz = cu.s
        return {"west": (sz, sy), "east": (sz, sy), "north": (sx, sy), "south": (sx, sy), "up": (sx, sz), "down": (sx, sz)}[f]

    def _layout(self, rects):
        """Shelf packing of (key, w, h) into the smallest power-of-two rectangle with a one texel gap. Returns (W, H, {key: (x, y)})."""
        items = sorted(rects, key=lambda r: (-r[2], -r[1]))
        best = None
        sizes = [32, 64, 128, 256, 512, 1024, 2048, 4096]
        for W in sizes:
            if any(r[1] + 1 > W for r in items):
                continue
            x = y = row = 0
            pos = {}
            for key, w, h in items:
                if x + w + 1 > W:
                    x, y, row = 0, y + row + 1, 0
                pos[key] = (x, y)
                x += w + 1
                row = max(row, h)
            Hn = y + row + 1
            H = next((s for s in sizes if s >= Hn), None)
            if H is None or H > 4096:
                continue
            area = W * H
            if best is None or area < best[0] * best[1] or (area == best[0] * best[1] and abs(W - H) < abs(best[0] - best[1])):
                best = (W, H, pos)
        if best is None:
            raise ValueError(self.id + ": the faces do not fit in 4096 px")
        return best

    # ------------------------------------------------------------------ writing
    def save(self, anim=None, root=None):
        self._cull(anim)
        warns = self._validate()
        rects = []
        for sh in self.sheets:
            if sh.used:
                rects.append((("sheet", id(sh)), sh.wt, sh.ht))
        for i, cu in enumerate(self.cubes):
            for f in FACES:
                if not cu.faces[f] or f in cu.facemap:
                    continue
                fw, fh = self._dims(cu, f)
                w = max(2, int(math.ceil(fw * cu.d - 1e-6)))
                h = max(2, int(math.ceil(fh * cu.d - 1e-6)))
                rects.append(((i, f), w, h))
        W, H, pos = self._layout(rects)
        img = np.zeros((H, W, 4), np.uint8)
        glow = np.zeros((H, W, 4), np.uint8)
        painted = 0
        for sh in self.sheets:
            if not sh.used:
                continue
            x, y = pos[("sheet", id(sh))]
            a, g = PT.paint(sh.mat, sh.wt, sh.ht, "north", "%s/%s" % (self.id, sh.tag), (sh.w_px, sh.h_px), sh.decals)
            img[y:y + sh.ht, x:x + sh.wt] = a
            glow[y:y + sh.ht, x:x + sh.wt] = g
            sh.pos = (x, y)
            painted += sh.wt * sh.ht
        for (key, w, h) in rects:
            if key[0] == "sheet":
                continue
            i, f = key
            cu = self.cubes[i]
            x, y = pos[key]
            fw, fh = self._dims(cu, f)
            a, g = PT.paint(cu.mat, w, h, f, "%s/%d" % (self.id, i), (fw, fh), cu.decals.get(f, ()))
            if f == "down":
                a, g = a[::-1], g[::-1]                                  # the loader stores the bottom net flipped
            img[y:y + h, x:x + w] = a
            glow[y:y + h, x:x + w] = g
            cu.own[f] = (x, y, w, h)
            painted += w * h
        bones = []
        faces = 0
        for n in self.order:
            b = dict(self.bones[n])
            b["cubes"] = []
            for cu in self.cubes:
                if cu.bone != n:
                    continue
                o = cu.c - cu.s / 2.0
                c = {"origin": [round(float(v), 4) for v in o], "size": [round(float(v), 4) for v in cu.s]}
                if any(abs(e) > 1e-6 for e in cu.eul):
                    c["rotation"] = [round(float(v), 3) for v in cu.eul]
                    c["pivot"] = [round(float(v), 4) for v in cu.c]
                uv = {}
                for f in FACES:
                    if not cu.faces[f]:
                        continue
                    if f in cu.facemap:
                        sh, x0, y0, x1, y1 = cu.facemap[f]
                        uv[BEDROCK[f]] = {"uv": [sh.pos[0] + x0, sh.pos[1] + y0], "uv_size": [x1 - x0, y1 - y0]}
                    else:
                        x, y, w, h = cu.own[f]
                        uv[BEDROCK[f]] = {"uv": [x, y], "uv_size": [w, h]}
                    faces += 1
                c["uv"] = uv
                b["cubes"].append(c)
            bones.append(b)
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.id, "texture_width": W, "texture_height": H,
                            "visible_bounds_width": 6, "visible_bounds_height": 6, "visible_bounds_offset": [0, 1, 0]}, "bones": bones}]}
        root = root or OUT
        os.makedirs(os.path.join(root, "geo", "entity"), exist_ok=True)
        os.makedirs(os.path.join(root, "textures", "entity"), exist_ok=True)
        with open(os.path.join(root, "geo", "entity", self.id + ".geo.json"), "w") as fh:
            json.dump(geo, fh, separators=(",", ":"))
        Image.fromarray(img, "RGBA").save(os.path.join(root, "textures", "entity", self.id + ".png"), optimize=True)
        gl = glow if self.glow_lift == 1.0 else np.clip(glow.astype(np.float32) * self.glow_lift, 0, 255).astype(np.uint8)
        Image.fromarray(gl, "RGBA").save(os.path.join(root, "textures", "entity", self.id + "_glow.png"), optimize=True)
        apath = os.path.join(root, "animations", "entity", self.id + ".animation.json")
        if anim is not None and anim.clips:
            os.makedirs(os.path.dirname(apath), exist_ok=True)
            with open(apath, "w") as fh:
                json.dump(anim.data(), fh, indent=1)
        elif os.path.exists(apath):
            os.remove(apath)
        self.report = {"id": self.id, "cubes": len(self.cubes), "faces": faces, "tex": (W, H), "painted": painted, "warnings": warns}
        for w_ in warns:
            print("  WARNING", w_)
        return self.report

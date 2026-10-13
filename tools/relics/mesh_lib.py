"""mesh_lib: the owner's model method as a library (pure Python 3 + numpy + PIL, no Blender).

It builds the baked meshes that client/WeaponRenderer draws (assets/nusmp/weapon_meshes/<id>.txt) from real cross-sections
instead of boxes, in the order of the method: silhouette, sections and bevels, raised parts, texture.

    from mesh_lib import *
    m, blade = Mesh("my_sword"), Part("blade", material="steel")
    loft(blade, "y", (0, 0, 0), [(0, 1, 1), (60, .7, .8), (80, 0, 0)], bevelled(4.6, .9, .45))    # a tapering blade with a point
    m.add(blade)
    m.add(Part("guard", material="gold")); lathe(m.parts[-1], "x", (0, -1, 0), [(-6, .3), (-5, 1.2), (5, 1.2), (6, .3)], sides=12)
    atlas = Atlas(1024); atlas.layout(m); atlas.paint(m)                          # projected UVs + a painted skin per part
    m.place_on_sprite("textures/item/my_sword.png", real_m=1.1)                   # onto the old icon's diagonal, like gen_weapon_models
    m.write_txt("my_sword.txt", atlas); atlas.save("my_sword", root=ASSETS)       # the mesh + <id>.png + <id>_glow.png
    preview(m, atlas=atlas, reference_png="card.png", out_png="my_sword_preview.png")

Conventions (the same as tools/gen_weapon_models.py): units are centimetres (any unit works, `place` derives the scale), y runs along
the weapon (the grip below, the blade above), x across it, z through it (+z toward the viewer in hand). Quads are counter-clockwise
seen from outside: the game culls back faces. The renderer stores ONE normal per quad ("q" lines); `write_txt(normals="vertex")` writes
per-vertex smoothed normals as "qn" lines, which need the small WeaponRenderer patch printed by `RENDERER_PATCH`.

UV space is always 128 x 128 (the renderer divides by 128) whatever the PNG size (the skins are 512 / 1024), so a part's UVs are in
"UV units" = pixels / (size / 128).
"""
import math
import os
import sys
import zlib

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
TS = 128

__all__ = ["Part", "Mesh", "Atlas", "Ring", "Loft", "UVGroup", "Ruler", "ASSETS", "ROOT", "TS", "RENDERER_PATCH", "MATERIALS",
           "rect_ring", "bevelled", "lozenge", "octagon", "circle", "round_section", "ellipse", "rounded_rect", "scale_sec", "shift_sec", "resample",
           "loft", "sweep", "lathe", "box", "collar", "stud", "rivet", "bar", "ridge_pair", "fuller", "ring_band", "gem",
           "smooth_normals", "plan_uv", "project_uv", "pack_shelf", "paint_material", "glow_from_mask", "sprite_axis", "sprite_box", "preview"]                                               # the renderer's UV space
EPS = 1e-9

RENDERER_PATCH = """\
// client/WeaponRenderer.java, parse(): add next to case "q" (per-vertex normals; emit() already reads v[5..7] as the normal)
case "qn" -> {
    int layer = Integer.parseInt(v[1]);
    for (int k = 0; k < 4; k++) {
        int o = 2 + k * 8;
        mesh.layers[layer].add(new float[]{Float.parseFloat(v[o]), Float.parseFloat(v[o + 1]), Float.parseFloat(v[o + 2]),
                Float.parseFloat(v[o + 3]), Float.parseFloat(v[o + 4]), Float.parseFloat(v[o + 5]), Float.parseFloat(v[o + 6]), Float.parseFloat(v[o + 7])});
    }
}
"""


# ================================================================ small maths
def _v(p):
    return np.asarray(p, np.float64)


def _unit(v):
    v = _v(v)
    n = np.linalg.norm(v)
    return v / n if n > EPS else v


_AXES = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1), "-x": (-1, 0, 0), "-y": (0, -1, 0), "-z": (0, 0, -1)}
_FRAMES = {"x": ((0, 1, 0), (0, 0, 1)), "y": ((1, 0, 0), (0, 0, 1)), "z": ((1, 0, 0), (0, 1, 0))}      # (u, v) of a section per named axis


def _area2(pts):
    p = np.asarray(pts, np.float64)
    return 0.5 * float(np.sum(p[:, 0] * np.roll(p[:, 1], -1) - np.roll(p[:, 0], -1) * p[:, 1]))


def _frame(axis, up=None, frame=None):
    """(a, e_u, e_v) for a named axis ('x', 'y', '-z' ...) or a vector. Named axes: y -> (x, z); x -> (y, z); z -> (x, y).
    For a vector axis e_v is `up` projected off the axis (default +z, or +y if the axis is along z) and e_u = a x e_v."""
    if isinstance(axis, str):
        a = _v(_AXES[axis])
        if frame is None:
            eu, ev = _FRAMES[axis.lstrip("-")]
            return a, _v(eu), _v(ev)
    else:
        a = _unit(axis)
    if frame is not None:
        return a, _unit(frame[0]), _unit(frame[1])
    up = _v((0, 0, 1) if up is None else up)
    if abs(float(np.dot(_unit(up), a))) > 0.98:
        up = _v((0, 1, 0)) if abs(a[1]) < 0.9 else _v((1, 0, 0))
    ev = _unit(up - a * np.dot(up, a))
    return a, np.cross(a, ev), ev


def _rodrigues(v, k, ang):
    k = _unit(k)
    return v * math.cos(ang) + np.cross(k, v) * math.sin(ang) + k * np.dot(k, v) * (1 - math.cos(ang))


# ================================================================ 2D sections (counter-clockwise rings of (u, v))
def rect_ring(w, t):
    return [(w / 2, -t / 2), (w / 2, t / 2), (-w / 2, t / 2), (-w / 2, -t / 2)]


def bevelled(w, t, bevel=0.35):
    """A flat-ground blade section: hexagon, edge tips at (+-w/2, 0), flats of half thickness t/2; `bevel` = share of the half width that is ground."""
    hw, ht = w / 2, t / 2
    f = hw * (1 - bevel)
    return [(hw, 0), (f, ht), (-f, ht), (-hw, 0), (-f, -ht), (f, -ht)]


def lozenge(w, t, ridge=0.0):
    """A diamond (4 points); with `ridge` > 0 the two ridges are flattened to a strip of that share of the width (6 points)."""
    hw, ht = w / 2, t / 2
    if ridge <= 0:
        return [(hw, 0), (0, ht), (-hw, 0), (0, -ht)]
    r = hw * ridge
    return [(hw, 0), (r, ht), (-r, ht), (-hw, 0), (-r, -ht), (r, -ht)]


def octagon(w, d, chamfer):
    """A w x d rectangle with the four corners cut by `chamfer`."""
    hw, hd, c = w / 2, d / 2, min(chamfer, w / 2 - 1e-4, d / 2 - 1e-4)
    return [(hw, -hd + c), (hw, hd - c), (hw - c, hd), (-hw + c, hd), (-hw, hd - c), (-hw, -hd + c), (-hw + c, -hd), (hw - c, -hd)]


def round(r, n=12, start=0.0):                                           # noqa: A001 (the name is the method's)
    return [(r * math.cos(start + 2 * math.pi * i / n), r * math.sin(start + 2 * math.pi * i / n)) for i in range(n)]


circle = round                                                           # `from mesh_lib import *` does not export `round` (it would hide the builtin): use circle()
round_section = round


def ellipse(a, b, n=12, start=0.0):
    return [(a * math.cos(start + 2 * math.pi * i / n), b * math.sin(start + 2 * math.pi * i / n)) for i in range(n)]


def rounded_rect(w, t, r, n=3):
    """A w x t rectangle with rounded corners (n segments per corner)."""
    hw, ht, r = w / 2, t / 2, min(r, w / 2 - 1e-4, t / 2 - 1e-4)
    out = []
    for cx, cy, a0 in ((hw - r, -ht + r, -90), (hw - r, ht - r, 0), (-hw + r, ht - r, 90), (-hw + r, -ht + r, 180)):
        for k in range(n + 1):
            a = math.radians(a0 + 90 * k / n)
            out.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return out


def scale_sec(pts, su, sv=None):
    sv = su if sv is None else sv
    return [(p[0] * su, p[1] * sv) for p in pts]


def shift_sec(pts, du, dv):
    return [(p[0] + du, p[1] + dv) for p in pts]


def resample(pts, n):
    """A closed polygon resampled to n points by arc length (starts at the first point): lets different sections be lofted together."""
    p = np.asarray(pts, np.float64)
    q = np.vstack([p, p[:1]])
    seg = np.linalg.norm(np.diff(q, axis=0), axis=1)
    cum = np.concatenate([[0], np.cumsum(seg)])
    out = []
    for s in np.linspace(0, cum[-1], n, endpoint=False):
        i = min(int(np.searchsorted(cum, s, side="right") - 1), len(seg) - 1)
        t = (s - cum[i]) / max(seg[i], EPS)
        out.append(tuple(q[i] + (q[i + 1] - q[i]) * t))
    return out


# ================================================================ rings, parts
class Ring:
    """Vertex indices of one section (indices may repeat: a collapsed section is one point)."""

    def __init__(self, idx, closed=True):
        self.idx = list(idx)
        self.closed = closed

    def __len__(self):
        return len(self.idx)

    def __getitem__(self, i):
        return self.idx[i]


class Loft:
    """What loft() / sweep() built: its rings and faces (the side faces; caps are separate)."""

    def __init__(self, rings, faces, record):
        self.rings, self.faces, self.record = rings, faces, record
        self.start, self.end = rings[0], rings[-1]


def _key(p):
    return (int(round_(p[0] * 1e4)), int(round_(p[1] * 1e4)), int(round_(p[2] * 1e4)))


def round_(x):
    return math.floor(x + 0.5)


class Part:
    """One named piece of the weapon (a blade, a collar, a rivet ...): its own vertices, quads, material, layer and smoothing.
    layer 0 body, 1 void (position only, the dimensional-void shader), 2 glow only. glow=True also draws it in the glow layer.
    Triangles are quads with a repeated vertex."""

    def __init__(self, name, layer=0, material="steel", base=None, glow=False, smooth=35.0, uv_mode=None, along=None, wear=0.5):
        self.name, self.layer, self.material, self.base, self.glow = name, layer, material, base, glow
        self.smooth, self.uv_mode, self.along, self.wear = smooth, uv_mode, along, wear
        self.V = []                  # [np.array(3)]
        self.F = []                  # [(i0, i1, i2, i3)]
        self.tags = []               # per face: None or ("loft", record, [(station, ring index) x 4])
        self.UV = None               # per face: 4 x (u, v) in UV units, set by Atlas.layout / project_uv
        self.N = None                # per face: 4 normals, set by smooth_normals
        self.groups = []

    # ------------------------------------------------------------ building
    def add_vertex(self, p):
        self.V.append(_v(p))
        return len(self.V) - 1

    def add_ring(self, points, closed=True):
        return Ring([self.add_vertex(p) for p in points], closed)

    def _newell(self, idx):
        n = np.zeros(3)
        for k in range(4):
            a, b = self.V[idx[k]], self.V[idx[(k + 1) % 4]]
            n += np.cross(a, b)
        return n * 0.5

    def add_face(self, i0, i1, i2, i3, tag=None):
        idx = (i0, i1, i2, i3)
        if np.linalg.norm(self._newell(idx)) < 1e-9:
            return None
        self.F.append(idx)
        self.tags.append(tag)
        self.UV = self.N = None
        return len(self.F) - 1

    def bridge(self, a, b, flip=False, tag=None):
        """Quads between two rings of equal length: (a[i], a[i+1], b[i+1], b[i]), counter-clockwise from outside when a ring runs
        counter-clockwise seen from the side b lies on. flip reverses. tag(order) -> tag, order = [('a'|'b', i) x 4] in face order."""
        if len(a) != len(b):
            raise ValueError(f"{self.name}: rings of {len(a)} and {len(b)} points; use resample() so the sections have the same count")
        n = len(a)
        out = []
        for i in range(n if a.closed else n - 1):
            j = (i + 1) % n
            if not flip:
                order = [("a", i), ("a", j), ("b", j), ("b", i)]
            else:
                order = [("a", i), ("b", i), ("b", j), ("a", j)]
            idx = [(a if r == "a" else b)[k] for (r, k) in order]
            f = self.add_face(*idx, tag=tag(order) if tag else None)
            if f is not None:
                out.append(f)
        return out

    def cap(self, ring, outward=None, flip=False):
        """Closes a planar ring with a fan from its centroid (fine for convex / star-shaped rings). `outward` = the way the cap faces."""
        ids = []
        for i in ring.idx:
            if i not in ids:
                ids.append(i)
        if len(ids) < 3:
            return []
        pts = [self.V[i] for i in ids]
        n = np.zeros(3)
        for k in range(len(pts)):
            n += np.cross(pts[k], pts[(k + 1) % len(pts)])
        if outward is not None and np.dot(n, _v(outward)) < 0:
            flip = not flip
        if flip:
            ids = ids[::-1]
        c = self.add_vertex(np.mean(pts, axis=0))
        out = []
        for k in range(len(ids)):
            f = self.add_face(c, ids[k], ids[(k + 1) % len(ids)], ids[(k + 1) % len(ids)])
            if f is not None:
                out.append(f)
        return out

    def fan(self, ring, tip, flip=False):
        """A cone of triangles from a ring to a point; faces point away from the ring's centre."""
        ids = []
        for i in ring.idx:
            if i not in ids:
                ids.append(i)
        t = self.add_vertex(tip)
        c = np.mean([self.V[i] for i in ids], axis=0)
        score = 0.0
        for k in range(len(ids)):
            a, b = self.V[ids[k]], self.V[ids[(k + 1) % len(ids)]]
            score += float(np.dot(np.cross(b - a, self.V[t] - a), a - c))
        if score < 0:
            flip = not flip
        if flip:
            ids = ids[::-1]
        return [f for f in (self.add_face(ids[k], ids[(k + 1) % len(ids)], t, t) for k in range(len(ids))) if f is not None]

    # ------------------------------------------------------------ geometry queries
    def face_normal(self, f):
        n = self._newell(self.F[f])
        m = np.linalg.norm(n)
        return n / m if m > EPS else np.array([0.0, 1.0, 0.0])

    def bbox(self):
        p = np.array(self.V)
        return p.min(axis=0), p.max(axis=0)

    def signed_volume(self):
        vol = 0.0
        for (a, b, c, d) in self.F:
            A, B, C, D = (self.V[k] for k in (a, b, c, d))
            vol += np.dot(A, np.cross(B, C)) + np.dot(A, np.cross(C, D))
        return vol / 6.0

    def is_closed(self):
        edges = {}
        for f in self.F:
            for k in range(4):
                a, b = _key(self.V[f[k]]), _key(self.V[f[(k + 1) % 4]])
                if a == b:
                    continue
                e = (a, b) if a < b else (b, a)
                edges[e] = edges.get(e, 0) + 1
        return bool(edges) and all(c == 2 for c in edges.values())

    def _flip_all(self):
        self.F = [(f[0], f[3], f[2], f[1]) for f in self.F]
        self.tags = [None if t is None else (t[0], t[1], [t[2][0], t[2][3], t[2][2], t[2][1]]) for t in self.tags]
        self.UV = self.N = None

    def ensure_outward(self):
        """If the part is a closed surface wound inside out, flips every face."""
        if self.is_closed() and self.signed_volume() < 0:
            self._flip_all()
            return True
        return False

    # ------------------------------------------------------------ copies
    def copy(self, name=None):
        c = Part(name or self.name, self.layer, self.material, self.base, self.glow, self.smooth, self.uv_mode, self.along, self.wear)
        c.V = [v.copy() for v in self.V]
        c.F = list(self.F)
        c.tags = list(self.tags)
        return c

    def transformed(self, matrix=None, offset=(0, 0, 0), name=None):
        """A copy with p -> matrix @ p + offset (faces re-wound if the matrix mirrors)."""
        c = self.copy(name)
        M = np.eye(3) if matrix is None else _v(matrix)
        c.V = [M @ v + _v(offset) for v in c.V]
        if np.linalg.det(M) < 0:
            c._flip_all()
        return c

    def translated(self, dx, dy, dz, name=None):
        return self.transformed(None, (dx, dy, dz), name)

    def mirrored(self, axis="x", name=None):
        M = np.eye(3)
        M["xyz".index(axis), "xyz".index(axis)] = -1
        return self.transformed(M, name=name)

    def rotated(self, axis, degrees, about=(0, 0, 0), name=None):
        a = math.radians(degrees)
        k = _unit(_AXES[axis] if isinstance(axis, str) else axis)
        R = np.array([_rodrigues(e, k, a) for e in np.eye(3)]).T
        c = self.copy(name)
        c.V = [R @ (v - _v(about)) + _v(about) for v in c.V]
        return c

    def counts(self):
        return len(self.F)


# ================================================================ lofts
def _norm_stations(stations):
    out = []
    for s in stations:
        if isinstance(s, (int, float)):
            out.append((float(s), 1.0, 1.0, 0.0, 0.0))
        else:
            t = tuple(float(x) for x in s) + (1.0, 1.0, 0.0, 0.0)[len(s) - 1:]
            out.append(t[:5])
    return out


def _build(part, frames, infos, section, caps, name):
    """Common engine of loft() and sweep(): frames = [(origin, e_u, e_v, a)], infos = [(s, su, sv, du, dv)]."""
    rings, flats = [], []
    for (o, eu, ev, a), (s, su, sv, du, dv) in zip(frames, infos):
        pts = section(s) if callable(section) else section
        pts = [(p[0] * su + du, p[1] * sv + dv) for p in pts]
        flats.append(pts)
        ext = max(max(abs(p[0] - pts[0][0]), abs(p[1] - pts[0][1])) for p in pts)
        if ext < 1e-7:
            i = part.add_vertex(o + eu * pts[0][0] + ev * pts[0][1])
            rings.append(Ring([i] * len(pts)))
        else:
            rings.append(part.add_ring([o + eu * p[0] + ev * p[1] for p in pts]))
    n = len(rings[0])
    if any(len(r) != n for r in rings):
        raise ValueError(f"{name}: the section function returned different point counts per station; use resample(pts, n)")
    area = next((_area2(p) for p in flats if abs(_area2(p)) > 1e-9), 1.0)
    o0, eu0, ev0, a0 = frames[0]
    h = 1.0 if float(np.dot(np.cross(eu0, ev0), a0)) > 0 else -1.0
    travel = 1.0 if float(np.dot(frames[-1][0] - frames[0][0], a0)) >= 0 else -1.0
    flip = area * h * travel < 0
    # unrolled coordinates for the 'loft' UV mode: arc length round each ring, distance along the spine
    arcs, perims = [], []
    for r in rings:
        P = np.array([part.V[i] for i in r.idx] + [part.V[r.idx[0]]])
        seg = np.linalg.norm(np.diff(P, axis=0), axis=1)
        arc = np.concatenate([[0], np.cumsum(seg)])
        arcs.append(arc)
        perims.append(float(arc[-1]))
    cen = [np.mean([part.V[i] for i in r.idx], axis=0) for r in rings]
    along = [0.0]
    for k in range(1, len(cen)):
        along.append(along[-1] + float(np.linalg.norm(cen[k] - cen[k - 1])))
    record = {"arc": arcs, "perim": perims, "along": along, "length": along[-1], "n": n}
    faces = []
    for j in range(len(rings) - 1):
        def tag(order, j=j):
            idxs = [i for (_r, i) in order]
            wrap = (n - 1) in idxs and 0 in idxs                      # the seam quad: its 0 is the ring's end, arc index n
            return ("loft", record, [(j + (0 if r == "a" else 1), n if (wrap and i == 0) else i) for (r, i) in order])
        faces += part.bridge(rings[j], rings[j + 1], flip=flip, tag=tag)
    if caps[0] and len(set(rings[0].idx)) > 2:
        part.cap(rings[0], outward=-travel * a0)
    if caps[1] and len(set(rings[-1].idx)) > 2:
        part.cap(rings[-1], outward=travel * frames[-1][3])
    return Loft(rings, faces, record)


def loft(part, axis, origin, stations, section, caps=(True, True), up=None, frame=None):
    """Lofts a surface along an axis through bridged rings.

    axis      'x' | 'y' | 'z' (a leading '-' runs it the other way) or a vector; named axes use u/v = (x,z) for y, (y,z) for x, (x,y) for z
    origin    a point on the axis where s = 0
    stations  s (distance along the axis) or (s, su, sv) or (s, su, sv, du, dv): the section is scaled by su, sv and moved by du, dv in
              (u, v). A scale of 0 in both closes the loft to a point (a blade tip).
    section   a list of (u, v) counter-clockwise, or a function f(s) -> such a list (same point count at every station)
    caps      (start, end): fan caps at the ends"""
    a, eu, ev = _frame(axis, up, frame)
    infos = _norm_stations(stations)
    o = _v(origin)
    frames = [(o + a * s[0], eu, ev, a) for s in infos]
    return _build(part, frames, infos, section, caps, part.name)


def sweep(part, path, section, scales=None, caps=(True, True), up=None):
    """Lofts a section along a polyline (a curved blade, a bow limb, a tentacle) with parallel-transported frames.
    path = [(x, y, z)]; scales = None | [(su, sv)] per point | f(t in 0..1) -> (su, sv)."""
    P = [_v(p) for p in path]
    n = len(P)
    tang = []
    for k in range(n):
        d = P[min(k + 1, n - 1)] - P[max(k - 1, 0)]
        tang.append(_unit(d))
    a0, eu0, ev0 = _frame(tang[0], up)
    ev = ev0
    frames, infos = [], []
    for k in range(n):
        if k > 0:
            c = np.cross(tang[k - 1], tang[k])
            if np.linalg.norm(c) > 1e-9:
                ang = math.acos(max(-1.0, min(1.0, float(np.dot(tang[k - 1], tang[k])))))
                ev = _unit(_rodrigues(ev, c, ang))
        ev = _unit(ev - tang[k] * np.dot(ev, tang[k]))
        eu = np.cross(tang[k], ev)
        frames.append((P[k], eu, ev, tang[k]))
        t = k / max(1, n - 1)
        su, sv = (1.0, 1.0) if scales is None else (scales(t) if callable(scales) else scales[k])
        infos.append((t, su, sv, 0.0, 0.0))
    return _build(part, frames, infos, section, caps, part.name)


def lathe(part, axis, origin, profile, sides=12, caps=(True, True), start=0.0, squash=1.0):
    """A surface of revolution: profile = [(s, radius)] along the axis (a pommel, a dome, a gem, a haft). squash scales the v radius (an oval)."""
    sec = round(1.0, sides, start)
    return loft(part, axis, origin, [(s, r, r * squash) for (s, r) in profile], sec, caps=caps)


def box(part, p0, p1, chamfer=0.0):
    """An axis-aligned block (optionally with chamfered long edges), as a loft along y."""
    x0, y0, z0 = p0
    x1, y1, z1 = p1
    sec = octagon(abs(x1 - x0), abs(z1 - z0), chamfer) if chamfer > 0 else rect_ring(abs(x1 - x0), abs(z1 - z0))
    return loft(part, "y", ((x0 + x1) / 2, 0, (z0 + z1) / 2), [min(y0, y1), max(y0, y1)], sec)


# ================================================================ raised details (each is built into a Part you name)
def collar(part, s, r, h, lip=0.0, sides=16, axis="y", origin=(0, 0, 0), oval=1.0, caps=(True, True)):
    """A band round an axis from s to s+h; with `lip` the rim bands taper in (a rim band / binding ring / ferrule)."""
    lip = min(lip, h / 2 - 1e-4) if lip > 0 else 0.0
    prof = [(s, r), (s + h, r)] if lip <= 0 else [(s, r - lip), (s + lip, r), (s + h - lip, r), (s + h, r - lip)]
    return lathe(part, axis, origin, prof, sides, caps, squash=oval)


def stud(part, pos, r, h, normal=(0, 0, 1), sides=8, dome=0.55):
    """A domed stud / rivet standing out of a surface at `pos` along `normal` (its base is buried in the surface)."""
    n = _unit(normal)
    return loft(part, n, pos, [(-h * 0.15, r), (0, r), (h * 0.55, r * (1 - dome * 0.35)), (h, r * (1 - dome))], round(1.0, sides), caps=(False, True))


def rivet(part, pos, r, normal=(0, 0, 1), sides=8):
    return stud(part, pos, r, r * 0.7, normal, sides, dome=0.8)


def bar(part, p0, p1, w, h, up=None, taper=0.0, section="lozenge", end_scale=0.35):
    """A thin raised bar from p0 to p1: width w, height h out of the surface along `up` (a ridge, a rib, a bead). The lower half is buried.
    taper = length of the tapered ends."""
    p0, p1 = _v(p0), _v(p1)
    L = float(np.linalg.norm(p1 - p0))
    sec = lozenge(w, 2 * h) if section == "lozenge" else rect_ring(w, 2 * h) if section == "rect" else octagon(w, 2 * h, min(w, 2 * h) * 0.3)
    if taper > 0 and L > 2 * taper:
        st = [(0, end_scale, end_scale), (taper, 1, 1), (L - taper, 1, 1), (L, end_scale, end_scale)]
    else:
        st = [0, L]
    return loft(part, p1 - p0, p0, st, sec, up=up)


def ridge_pair(part, p0, p1, normal, gap, w=0.14, h=0.07, taper=0.35):
    """An engraved line modelled as two raised ridges `gap` apart (the dark groove between them is painted)."""
    p0, p1, nrm = _v(p0), _v(p1), _unit(normal)
    side = _unit(np.cross(nrm, p1 - p0))
    for sgn in (-1, 1):
        bar(part, p0 + side * sgn * gap / 2, p1 + side * sgn * gap / 2, w, h, up=nrm, taper=taper)


def fuller(part, p0, p1, normal, width, ridge_w=None, h=None, taper=1.5):
    """A fuller (blood groove) between two ridges, narrowing at both ends."""
    return ridge_pair(part, p0, p1, normal, width, ridge_w or width * 0.16, h or width * 0.1, taper)


def ring_band(part, s, r, h, axis="y", origin=(0, 0, 0), sides=16):
    """A flat bound ring (cord / wire wrap): a collar with a soft lip."""
    return collar(part, s, r, h, lip=h * 0.35, sides=sides, axis=axis, origin=origin)


def gem(part, center, r, long=1.4, sides=6, axis="y"):
    """A cut gem: two cones with a short girdle."""
    c = _v(center)
    return lathe(part, axis, c, [(-r * long, 0.0), (-r * 0.15, r), (r * 0.15, r), (r * long * 0.7, 0.0)], sides, caps=(False, False))


# ================================================================ normals
def smooth_normals(part, angle_deg=35.0):
    """Per-vertex normals by angle: at each welded position the normals of the faces within `angle_deg` of a face are averaged (weighted by the
    corner angle); sharper creases stay hard. Sets part.N[face] = 4 unit normals."""
    nf = [part.face_normal(f) for f in range(len(part.F))]
    cos_t = math.cos(math.radians(angle_deg))
    w = []
    for f, idx in enumerate(part.F):
        ws = []
        for k in range(4):
            p, a, b = part.V[idx[k]], part.V[idx[(k + 1) % 4]], part.V[idx[(k - 1) % 4]]
            e1, e2 = a - p, b - p
            l1, l2 = np.linalg.norm(e1), np.linalg.norm(e2)
            if l1 < 1e-9 or l2 < 1e-9:
                ws.append(1e-4)
            else:
                ws.append(max(1e-4, math.acos(max(-1.0, min(1.0, float(np.dot(e1, e2) / (l1 * l2)))))))
        w.append(ws)
    buckets = {}
    for f, idx in enumerate(part.F):
        for k in range(4):
            buckets.setdefault(_key(part.V[idx[k]]), []).append((f, k))
    N = []
    for f, idx in enumerate(part.F):
        out = []
        for k in range(4):
            acc = np.zeros(3)
            for (g, d) in buckets[_key(part.V[idx[k]])]:
                if float(np.dot(nf[f], nf[g])) >= cos_t - 1e-9:
                    acc += nf[g] * w[g][d]
            out.append(_unit(acc) if np.linalg.norm(acc) > EPS else nf[f])
        N.append(out)
    part.N = N
    return N


# ================================================================ UV: groups, packing, projection
def _dom_ab(axis, sign, p):
    """(a, b) of a point on a dominant-axis plane: a to the right, b up, as a viewer in front of that face sees it."""
    x, y, z = p
    if axis == 2:
        return (x if sign >= 0 else -x), y
    if axis == 0:
        return (-z if sign >= 0 else z), y
    return x, (-z if sign >= 0 else z)


class UVGroup:
    """A rectangle of the skin that one set of faces of one part maps onto (a dominant-axis plane, or one unrolled loft)."""

    def __init__(self, part, kind, key, faces, ext, origin=None):
        self.part, self.kind, self.key, self.faces, self.ext, self.origin = part, kind, key, faces, ext, origin
        self.rect = None            # (x, y, w, h) in px
        self.ppu = None

    def px_size(self, ppu):
        return max(1, int(math.ceil(self.ext[0] * ppu - 1e-9))), max(1, int(math.ceil(self.ext[1] * ppu - 1e-9)))

    def local_px(self, p3):
        """Dominant groups: the pixel (within the group's rect) of a model-space point on this plane."""
        a, b = _dom_ab(self.key[0], self.key[1], p3)
        return (a - self.origin[0]) * self.ppu, (self.origin[1] - b) * self.ppu


def plan_uv(part, mode="dominant", separate_signs=True):
    """The UV groups of a part. mode 'dominant' = the owner's method (each face projected on the axis its normal is closest to, one rect per
    axis and side); 'loft' = lofted faces unrolled as one strip per loft (round parts wrap without seams), caps and loose faces dominant."""
    groups, rest = [], []
    mode = part.uv_mode or mode
    if mode == "loft":
        byrec = {}
        for f, t in enumerate(part.tags):
            if t is not None:
                byrec.setdefault(id(t[1]), (t[1], []))[1].append(f)
            else:
                rest.append(f)
        for rid, (rec, fs) in byrec.items():
            groups.append(UVGroup(part, "loft", rid, fs, (max(rec["perim"]) if max(rec["perim"]) > 1e-6 else 1e-3, max(rec["length"], 1e-3)), rec))
    else:
        rest = list(range(len(part.F)))
    dom = {}
    for f in rest:
        n = part.face_normal(f)
        ax = int(np.argmax(np.abs(n)))
        sg = (1 if n[ax] >= 0 else -1) if separate_signs else 0
        dom.setdefault((ax, sg), []).append(f)
    for key, fs in dom.items():
        ab = np.array([_dom_ab(key[0], key[1], part.V[i]) for f in fs for i in part.F[f]])
        lo, hi = ab.min(axis=0), ab.max(axis=0)
        groups.append(UVGroup(part, "dom", key, fs, (max(float(hi[0] - lo[0]), 1e-3), max(float(hi[1] - lo[1]), 1e-3)), (float(lo[0]), float(hi[1]))))
    part.groups = groups
    return groups


def pack_shelf(sizes, W, H, pad):
    """Shelf-packs (w, h) boxes into W x H with `pad` px between and round the edge; returns [(x, y)] or None if they do not fit."""
    order = sorted(range(len(sizes)), key=lambda i: (-sizes[i][1], -sizes[i][0]))
    pos = [None] * len(sizes)
    x = y = pad
    row = 0
    for i in order:
        w, h = sizes[i]
        if w + 2 * pad > W or h + 2 * pad > H:
            return None
        if x + w + pad > W:
            x, y, row = pad, y + row + pad, 0
        if y + h + pad > H:
            return None
        pos[i] = (x, y)
        x += w + pad
        row = max(row, h)
    return pos


def _assign_uv(group, ppu, k):
    """Writes the UVs (UV units) of a group's faces, given its rect and px-per-unit."""
    part = group.part
    if part.UV is None or len(part.UV) != len(part.F):
        part.UV = [None] * len(part.F)
    x0, y0, w, h = group.rect
    group.ppu = ppu
    for f in group.faces:
        idx = part.F[f]
        uv = []
        if group.kind == "dom":
            for k4 in range(4):
                a, b = _dom_ab(group.key[0], group.key[1], part.V[idx[k4]])
                uv.append(((x0 + (a - group.origin[0]) * ppu) / k, (y0 + (group.origin[1] - b) * ppu) / k))
        else:
            rec = group.origin
            pmax = max(rec["perim"])
            for (j, i) in part.tags[f][2]:
                shift = (pmax - rec["perim"][j]) / 2
                uv.append(((x0 + (shift + rec["arc"][j][i]) * ppu) / k, (y0 + (rec["length"] - rec["along"][j]) * ppu) / k))
        part.UV[f] = uv


def project_uv(part, rect, size=1024, mode="dominant", separate_signs=True, ppu=None, pad=3):
    """Projects one part's UVs into a rectangle (x, y, w, h in px) of a size x size skin, at the largest px-per-unit that fits.
    Returns the px-per-unit used."""
    groups = plan_uv(part, mode, separate_signs)
    k = size / TS
    x0, y0, w, h = rect
    lo, hi = 0.05, 64.0
    best = None
    if ppu is not None:
        lo = hi = ppu
    for _ in range(18):
        mid = hi if ppu is not None else (lo + hi) / 2
        pos = pack_shelf([g.px_size(mid) for g in groups], w, h, pad)
        if pos is not None:
            best = (mid, pos)
            if ppu is not None:
                break
            lo = mid
        else:
            hi = mid
    if best is None:
        raise ValueError(f"{part.name}: does not fit in the rect {rect}")
    mid, pos = best
    for g, (px, py) in zip(groups, pos):
        gw, gh = g.px_size(mid)
        g.rect = (x0 + px, y0 + py, gw, gh)
        _assign_uv(g, mid, k)
    return mid


# ================================================================ mesh
class Mesh:
    """The weapon: parts in draw order, the placement on the old icon, the glow pulse."""

    def __init__(self, name):
        self.name = name
        self.parts = []
        self.place_cfg = None
        self.glow_cfg = None

    def add(self, part):
        self.parts.append(part)
        return part

    def part(self, name):
        return next(p for p in self.parts if p.name == name)

    def set_glow(self, rgb="FFFFFF", speed=0.12, lo=0.35, hi=1.0):
        """The additive layer's tint (it MULTIPLIES the glow PNG: keep FFFFFF to use the PNG's own colours) and its brightness pulse."""
        self.glow_cfg = (rgb if isinstance(rgb, str) else "%06X" % rgb, speed, lo, hi)

    def place(self, ox, oy, angle, scale, ymin):
        """The raw `place` line: item-space origin of y = ymin, the axis angle (deg, 45 = the sprite diagonal), item units per model unit."""
        self.place_cfg = (float(ox), float(oy), float(angle), float(scale), float(ymin))

    def y_range(self):
        ys = [v[1] for p in self.parts for v in p.V]
        return min(ys), max(ys)

    def bbox(self):
        lo = np.min([p.bbox()[0] for p in self.parts], axis=0)
        hi = np.max([p.bbox()[1] for p in self.parts], axis=0)
        return lo, hi

    def mirror_x(self):
        self.parts = [p.mirrored("x") for p in self.parts]

    def place_on_sprite(self, icon_png, real_m=None, grip=0.4, fit=False, hand_scale=0.85):
        """Places the mesh like tools/gen_weapon_models.write(): its y axis on the old 16x16 icon's diagonal, so the old display transforms fit.
        real_m = the whole weapon's length in metres in hand (1 block = 1 m, the hand scales by 0.85), grown about `grip` (fraction along the
        icon's axis); fit=True for an upright relic centred on its icon's box."""
        ymin, ymax = self.y_range()
        if fit:
            lo, hi = self.bbox()
            x0, y0, x1, y1 = sprite_box(icon_png)
            scale = min((x1 - x0) / max(hi[0] - lo[0], 1e-6), (y1 - y0) / max(ymax - ymin, 1e-6)) * 0.95
            cx = (lo[0] + hi[0]) / 2
            ox, oy, angle = (x0 + x1) / 2 - cx * scale, (y0 + y1) / 2 - (ymax - ymin) * scale / 2, 90.0
        else:
            origin, angle, length = sprite_axis(icon_png)
            scale = length / max(ymax - ymin, 1e-6)
            if real_m:
                kk = real_m / (hand_scale * length)
                d = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))])
                origin = origin + d * (grip * length) * (1 - kk)
                scale *= kk
            ox, oy = float(origin[0]), float(origin[1])
        self.place(ox, oy, angle, scale, ymin)
        return self.place_cfg

    # ------------------------------------------------------------ finishing / output
    def finish(self):
        """Winds closed parts outward and computes the smoothed normals; call before layout / write (the others call it for you)."""
        for p in self.parts:
            p.ensure_outward()
            if p.N is None or len(p.N) != len(p.F):
                smooth_normals(p, p.smooth if p.smooth else 0.0)

    def iter_quads(self, normals="quad", atlas=None):
        """Yields (layer, P 4x3, UV 4x2, N 4x3, part) exactly as write_txt writes them (body quads get a glow copy where the part is glow
        or the skin's glow layer has pixels under the quad)."""
        self.finish()
        for p in self.parts:
            if p.UV is None or len(p.UV) != len(p.F) or any(u is None for u in p.UV):
                raise ValueError(f"{p.name}: no UVs; run Atlas.layout(mesh) or project_uv(part, rect) first")
            for f, idx in enumerate(p.F):
                P = np.array([p.V[i] for i in idx])
                UV = np.array(p.UV[f])
                if normals == "vertex":
                    N = np.array(p.N[f])
                else:
                    N = np.tile(p.face_normal(f), (4, 1))
                if p.layer == 0:
                    yield 0, P, UV, N, p
                    if p.glow or (atlas is not None and atlas.has_glow(UV)):
                        yield 2, P, UV, N, p
                else:
                    yield p.layer, P, UV, N, p

    def stats(self, atlas=None):
        c = {0: 0, 1: 0, 2: 0}
        for (layer, *_rest) in self.iter_quads(atlas=atlas):
            c[layer] += 1
        lo, hi = self.bbox()
        return {"parts": len(self.parts), "quads": c, "bbox_min": lo.round(3).tolist(), "bbox_max": hi.round(3).tolist(), "length": round_(1000 * float(hi[1] - lo[1])) / 1000}

    def write_txt(self, path, atlas=None, normals="quad", comment=None):
        """Writes the mesh in the game's format (see client/WeaponRenderer). normals='quad': one normal per quad, readable by the current
        renderer. normals='vertex': smoothed per-vertex "qn" lines (needs RENDERER_PATCH in WeaponRenderer.parse)."""
        if self.place_cfg is None:
            raise ValueError("call place(...) or place_on_sprite(...) first")
        lines = [f"# {comment or 'generated by tools/relics/mesh_lib.py'} ({self.name}); {len(self.parts)} parts"]
        lines.append("place %.5f %.5f %.3f %.6f %.3f" % self.place_cfg)
        quads = list(self.iter_quads(normals, atlas))
        if any(q[0] == 2 for q in quads):
            g = self.glow_cfg or ("FFFFFF", 0.12, 0.35, 1.0)
            lines.append("glow %s %s %s %s" % (g[0], g[1], g[2], g[3]))
        for (layer, P, UV, N, _p) in quads:
            if normals == "vertex":
                lines.append("qn %d %s" % (layer, " ".join("%.3f %.3f %.3f %.3f %.3f %.4f %.4f %.4f" % (*P[k], *UV[k], *N[k]) for k in range(4))))
            else:
                lines.append("q %d %s %.3f %.3f %.3f" % (layer, " ".join("%.3f %.3f %.3f %.3f %.3f" % (*P[k], *UV[k]) for k in range(4)), *N[0]))
        os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
        with open(path, "w", newline="\n") as fh:
            fh.write("\n".join(lines) + "\n")
        return len(quads)


# ================================================================ placement on the old sprite (as tools/gen_weapon_models.py)
def _sprite_alpha(icon_png):
    return np.asarray(Image.open(icon_png).convert("RGBA"))


def sprite_axis(icon_png):
    """(origin, angle deg, length) of the icon's diagonal in item space (0..1), the principal axis of its opaque pixels."""
    a = _sprite_alpha(icon_png)
    S = a.shape[0]
    ys, xs = np.nonzero(a[..., 3] > 40)
    P = np.stack([(xs + 0.5) / S, 1 - (ys + 0.5) / S], 1)
    c = P.mean(0)
    d = np.linalg.svd(P - c)[2][0]
    if d[0] < 0:
        d = -d
    t = (P - c) @ d
    return c + d * t.min(), math.degrees(math.atan2(d[1], d[0])), float(t.max() - t.min())


def sprite_box(icon_png):
    a = _sprite_alpha(icon_png)
    S = a.shape[0]
    ys, xs = np.nonzero(a[..., 3] > 40)
    return xs.min() / S, 1 - (ys.max() + 1) / S, (xs.max() + 1) / S, 1 - ys.min() / S


class Ruler:
    """Step 1 of the method: pixels on the reference -> centimetres. Ruler((x0, y0), (x1, y1), real_cm); ruler.cm(px_length)."""

    def __init__(self, a, b, real_cm):
        self.px = math.hypot(b[0] - a[0], b[1] - a[1])
        self.real_cm = real_cm

    def cm(self, px):
        return px * self.real_cm / self.px

    def between(self, a, b):
        return self.cm(math.hypot(b[0] - a[0], b[1] - a[1]))


# ================================================================ materials
def _smoothstep(x, a, b):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def _noise(rng, A, B, ca, cb):
    """Smooth value noise in 0..1, shape (A, B), features ca px along axis 0 and cb px along axis 1."""
    ga, gb = max(2, int(math.ceil(A / ca)) + 1), max(2, int(math.ceil(B / cb)) + 1)
    g = rng.random((ga, gb), dtype=np.float32)
    return np.clip(np.asarray(Image.fromarray(g).resize((B, A), Image.BICUBIC)), 0, 1)


# kind -> parameters. type 'metal' uses base/hi/streak/grain/blotch/band*/edge/scratches; the others have their own recipes.
MATERIALS = {
    "steel":    dict(type="metal", base=(146, 154, 168), hi=(238, 244, 252), streak=.14, grain=.06, blotch=.09, band=.50, band_pos=-.35, band_w=.30, edge=.20, scratches=18),
    "iron":     dict(type="metal", base=(74, 78, 92), hi=(170, 178, 196), streak=.12, grain=.08, blotch=.15, band=.30, band_pos=-.30, band_w=.28, edge=.22, scratches=12),
    "silver":   dict(type="metal", base=(190, 196, 208), hi=(252, 253, 255), streak=.09, grain=.05, blotch=.06, band=.60, band_pos=-.30, band_w=.32, edge=.12, scratches=10),
    "gold":     dict(type="metal", base=(212, 168, 66), hi=(255, 238, 154), streak=.09, grain=.05, blotch=.10, band=.62, band_pos=-.30, band_w=.30, edge=.16, scratches=8),
    "bronze":   dict(type="metal", base=(158, 104, 58), hi=(236, 184, 112), streak=.10, grain=.07, blotch=.20, band=.42, band_pos=-.32, band_w=.30, edge=.16, scratches=10),
    "obsidian": dict(type="metal", base=(30, 26, 40), hi=(160, 140, 210), streak=.05, grain=.03, blotch=.06, band=.75, band_pos=-.25, band_w=.22, edge=.10, scratches=4),
    "stone":    dict(type="metal", base=(112, 110, 116), hi=(190, 188, 196), streak=.05, grain=.16, blotch=.22, band=.18, band_pos=-.2, band_w=.4, edge=.05, scratches=6),
    "wood":     dict(type="wood", base=(112, 74, 42), hi=(170, 124, 76)),
    "bone":     dict(type="bone", base=(224, 212, 178), hi=(250, 244, 222)),
    "crystal":  dict(type="crystal", base=(96, 190, 228), hi=(236, 252, 255)),
    "leather":  dict(type="leather", base=(96, 62, 38), hi=(150, 104, 66)),
    "cloth":    dict(type="cloth", base=(150, 40, 48), hi=(214, 96, 104)),
}


def paint_material(kind, size, along="v", seed=0, base=None, wear=0.5):
    """A painted material patch (float32 RGBA 0..255, shape (h, w, 4)): brushed / grained along the form with a highlight band running along it,
    edge wear, scratches and large patina; never a flat tint. `along` = 'v' (texture rows run along the form) or 'u'. base overrides the colour."""
    w, h = (size, size) if isinstance(size, int) else (int(size[0]), int(size[1]))
    S = dict(MATERIALS[kind])
    if base is not None:
        S["base"] = tuple(base)
    A, B = (h, w) if along == "v" else (w, h)
    rng = np.random.default_rng((int(seed) * 7919 + zlib.crc32(kind.encode())) & 0xFFFFFFFF)
    x = np.linspace(-1, 1, B, dtype=np.float32)[None, :]
    t = np.linspace(0, 1, A, dtype=np.float32)[:, None]
    base_c, hi_c = np.array(S["base"], np.float32), np.array(S["hi"], np.float32)
    ns = _noise(rng, A, B, 90, 1.6) - .5
    nb = _noise(rng, A, B, 40, 40) - .5
    nf = _noise(rng, A, B, 1.5, 1.5) - .5
    typ = S["type"]
    if typ == "metal":
        shade = 1 + S["streak"] * 2 * ns + S["blotch"] * 2 * nb + S["grain"] * 2 * nf
        band = np.exp(-((x - S["band_pos"]) / S["band_w"]) ** 2) * (0.75 + 0.5 * (_noise(rng, A, B, 120, 40) - .5))
        band = np.clip(band * S["band"], 0, 1)
        col = base_c * shade[..., None]
        col = col + (hi_c - col) * band[..., None]
        e = _smoothstep(np.abs(x), 0.78, 1.0)
        col = col * (1 + S["edge"] * 2 * wear * e)[..., None]
        for _ in range(int(S["scratches"] * wear * max(1.0, A * B / 65536))):
            c, a0, ln = int(rng.integers(0, B)), int(rng.integers(0, A)), int(rng.integers(8, 60))
            col[a0:a0 + ln, c] += (hi_c - col[a0:a0 + ln, c]) * float(rng.uniform(.12, .35)) * wear * 2
    elif typ == "wood":
        warp = _noise(rng, A, B, 60, 8) - .5
        rings = np.sin((x * 5.5 + warp * 3.2) * 2 * math.pi)
        pores = (nf > 0.18).astype(np.float32)
        shade = 1 + 0.16 * rings + 0.30 * nb - 0.20 * pores + 0.10 * ns
        col = base_c * shade[..., None]
        col = col + (hi_c - col) * np.clip(0.5 * np.exp(-((x + .3) / .35) ** 2), 0, 1)[..., None] * 0.6
        col = col * (1 - 0.25 * _smoothstep(np.abs(x), 0.8, 1.0))[..., None]
    elif typ == "bone":
        crack = (np.abs(_noise(rng, A, B, 50, 3) - .5) < 0.012).astype(np.float32)
        shade = 1 + 0.16 * nb * 2 + 0.07 * nf * 2 + 0.06 * ns * 2 - 0.35 * crack
        col = base_c * shade[..., None]
        col = col + (hi_c - col) * np.clip(np.exp(-((x + .3) / .35) ** 2) * 0.45, 0, 1)[..., None]
        col = col * (1 - 0.18 * _smoothstep(np.abs(x), 0.8, 1.0))[..., None]
    elif typ == "crystal":
        ids = np.floor(x * 3.2 + t * 1.4 + nb * 0.45 + 8).astype(np.int64)
        vals = rng.random(64)
        facet = 0.62 + 0.5 * vals[ids % 64]
        frac = (x * 3.2 + t * 1.4 + nb * 0.45 + 8) % 1.0
        line = np.clip(1 - np.minimum(frac, 1 - frac) / 0.035, 0, 1)
        core = np.exp(-((x - 0.1) / .22) ** 2)
        col = base_c * facet[..., None]
        col = col + (hi_c - col) * np.clip(core * 0.45 + line * 0.7, 0, 1)[..., None]
        col = col * (1 - 0.3 * _smoothstep(np.abs(x), 0.82, 1.0))[..., None]
    elif typ == "leather":
        peb = _noise(rng, A, B, 3.2, 3.2)
        crease = 0.6 * np.exp(-((_noise(rng, A, B, 24, 24) - .5) / 0.025) ** 2)
        shade = 0.82 + 0.34 * peb + 0.1 * nb * 2 - 0.3 * crease
        col = base_c * shade[..., None]
        col = col + (hi_c - col) * np.clip(np.exp(-((x + .35) / .3) ** 2) * 0.32, 0, 1)[..., None]
        edge = _smoothstep(np.abs(x), 0.86, 1.0)
        col = col * (1 - 0.45 * edge)[..., None]
        ai = np.arange(A)[:, None]
        stitch = (np.abs(np.abs(x) - 0.76) < 0.045) & ((ai % 6) < 3)
        col = np.where(stitch[..., None], np.array([206, 190, 150], np.float32) * (0.85 + 0.3 * nf[..., None]), col)
    else:                                                                       # cloth
        ai, bi = np.arange(A)[:, None], np.arange(B)[None, :]
        weave = (np.sin(ai * math.pi / 1.5) * np.sin(bi * math.pi / 1.5)) * 0.045
        folds = np.sin((x * 3.0 + _noise(rng, A, B, 70, 30) * 2.6) * math.pi) * 0.14
        shade = 1 + weave + folds + 0.12 * nb
        col = base_c * shade[..., None]
        col = col + (hi_c - col) * np.clip(np.maximum(folds, 0) * 1.6, 0, 1)[..., None] * 0.5
    col = np.clip(col, 0, 255)
    out = np.concatenate([col, np.full((A, B, 1), 255, np.float32)], axis=2).astype(np.float32)
    return out if along == "v" else out.transpose(1, 0, 2)


def glow_from_mask(mask, rgb, strength=1.0, halo=1.6, core_white=0.35):
    """Float RGBA (0..255) glow patch from a mask (bool / 0..1, shape (h, w)): a hot core in `rgb` (blended a little toward white) and a soft
    halo of `halo` px; the pixels the additive layer draws."""
    m = np.clip(np.asarray(mask, np.float32), 0, 1)
    hl = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(halo)), np.float32) / 255
    a = np.clip(np.maximum(m, hl * 0.8) * strength, 0, 1)
    c = np.array(rgb, np.float32)
    core = c + (255 - c) * core_white
    colr = c[None, None, :] + (core - c)[None, None, :] * m[..., None]
    return np.concatenate([colr, (a * 255)[..., None]], axis=2).astype(np.float32)


# ================================================================ atlas
class Atlas:
    """The weapon's skin (size x size px, the mesh UVs stay in 128 space) and its glow layer; lays out every part's UV groups, paints them."""

    def __init__(self, size=1024, bleed=1, pad=4):
        self.size, self.k, self.bleed, self.pad = size, size / TS, bleed, pad
        self.img = np.zeros((size, size, 4), np.float32)
        self.glow = np.zeros((size, size, 4), np.float32)
        self.groups = []
        self.ppu = None

    def layout(self, mesh, ppu=None, mode="dominant", separate_signs=True, ppu_max=24.0):
        """Plans every part's UV groups and packs them into the skin at the largest px-per-model-unit that fits (or `ppu`); sets all UVs."""
        mesh.finish()
        groups = []
        for p in mesh.parts:
            if p.layer == 1:                                          # the void layer is position only: no skin space
                p.UV = [[(0.0, 0.0)] * 4 for _ in p.F]
                p.groups = []
                continue
            groups += plan_uv(p, mode, separate_signs)
            p.UV = [None] * len(p.F)
        lo, hi = 0.05, ppu_max
        best = None
        for _ in range(22):
            mid = ppu if ppu else (lo + hi) / 2
            pos = pack_shelf([g.px_size(mid) for g in groups], self.size, self.size, self.pad)
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
            raise ValueError("the parts do not fit the skin; reduce the part count / sizes or raise the atlas size")
        mid, pos = best
        for g, (x, y) in zip(groups, pos):
            w, h = g.px_size(mid)
            g.rect = (x, y, w, h)
            _assign_uv(g, mid, self.k)
        self.groups, self.ppu = groups, mid
        return mid

    def paint(self, mesh, along=None):
        """Fills every group with its part's material, grained along the longer side of the group (or part.along)."""
        b = self.bleed
        for g in self.groups:
            x, y, w, h = g.rect
            p = g.part
            al = p.along or along or ("v" if g.kind == "loft" or g.ext[1] >= g.ext[0] else "u")
            patch = paint_material(p.material, (w + 2 * b, h + 2 * b), al, seed=zlib.crc32(p.name.encode()) % 9973 + g.key[0] if g.kind == "dom" else zlib.crc32(p.name.encode()) % 9973, base=p.base, wear=p.wear)
            self.img[y - b:y + h + b, x - b:x + w + b] = patch

    def has_glow(self, uv, thresh=2.0):
        """True if the skin's glow layer has pixels under a quad (uv 4x2 in UV units)."""
        k = self.k
        u0, u1 = int(max(0, math.floor(uv[:, 0].min() * k))), int(min(self.size, math.ceil(uv[:, 0].max() * k) + 1))
        v0, v1 = int(max(0, math.floor(uv[:, 1].min() * k))), int(min(self.size, math.ceil(uv[:, 1].max() * k) + 1))
        return bool(self.glow[v0:v1, u0:u1, 3].max(initial=0) > thresh)

    # ------------------------------------------------------------ drawing on the skin
    def group_for(self, part, axis="z", sign=1):
        """The dominant group of `part` facing +-axis ('x' | 'y' | 'z'), for line() / paint_in()."""
        ax = "xyz".index(axis)
        for g in part.groups:
            if g.kind == "dom" and g.key[0] == ax and g.key[1] in (sign, 0):
                return g
        raise KeyError(f"{part.name} has no {sign:+d}{axis} group")

    def paint_in(self, group, fn):
        """fn(img_view, glow_view, group) edits the group's rect of the skin and of the glow layer in place (float32 views)."""
        x, y, w, h = group.rect
        fn(self.img[y:y + h, x:x + w], self.glow[y:y + h, x:x + w], group)

    def line(self, part, p0, p1, width, axis="z", sign=1, dark=0.55, rim=0.14, glow_rgb=None, glow_strength=1.0):
        """Draws a line between two model-space points on a face of `part`: an engraved groove (darkens by `dark`, a light rim) and / or a
        glow line (`glow_rgb`). width in model units."""
        g = self.group_for(part, axis, sign)
        x, y, w, h = g.rect

        def draw(img, glow, grp):
            a, b = grp.local_px(p0), grp.local_px(p1)
            wpx = max(1.0, width * grp.ppu)
            mk = Image.new("L", (w, h), 0)
            ImageDraw.Draw(mk).line([a, b], fill=255, width=max(1, int(round_(wpx))))
            m = np.asarray(mk.filter(ImageFilter.GaussianBlur(0.6)), np.float32)[..., None] / 255
            if dark:
                img[..., :3] *= 1 - dark * m
                mk2 = np.asarray(mk.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(0.6)), np.float32)[..., None] / 255
                img[..., :3] += 255 * rim * np.clip(mk2 - m, 0, 1) * 0.6
            if glow_rgb is not None:
                gl = glow_from_mask(m[..., 0], glow_rgb, glow_strength, halo=max(1.0, wpx))
                glow[...] = np.maximum(glow, gl)

        self.paint_in(g, draw)

    # ------------------------------------------------------------ output
    def save(self, item_id, root=ASSETS):
        """Writes textures/entity/weapon/<id>.png and (if the glow layer has pixels) <id>_glow.png under root; returns the paths."""
        d = os.path.join(root, "textures", "entity", "weapon")
        os.makedirs(d, exist_ok=True)
        out = []
        p = os.path.join(d, item_id + ".png")
        Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8), "RGBA").save(p, optimize=True)
        out.append(p)
        if self.glow[..., 3].max() > 0:
            p = os.path.join(d, item_id + "_glow.png")
            Image.fromarray(np.clip(self.glow, 0, 255).astype(np.uint8), "RGBA").save(p, optimize=True)
            out.append(p)
        return out


# ================================================================ preview (software rasteriser in preview_mesh.py)
def preview(mesh, reference_png=None, out_png="preview.png", atlas=None, **kw):
    """Renders profile / three-quarter / edge / top views side by side (flat-lit, z-buffered, back faces culled like the game), with the
    reference image beside them. Returns out_png. See preview_mesh.render_preview for the options."""
    sys.path.insert(0, HERE)
    import preview_mesh
    return preview_mesh.render_preview(mesh, reference_png, out_png, atlas=atlas, **kw)

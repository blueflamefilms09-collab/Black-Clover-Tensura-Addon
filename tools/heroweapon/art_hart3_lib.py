"""art_hart3_lib: the helpers behind tools/heroweapon/art_hart3.py (the art of the Vassal Carriage, Fan, Scythe, Katana and Book).

Why: the five weapons are built with the owner's model method (tools/relics/mesh_lib.py: real cross-sections, named parts, smoothed per-vertex normals, a painted
skin and an additive glow skin) and they need the same dozen small tools again and again (an oriented quad, a tapered rod, a cut gem, a torus, a lobed disc, a
decal drawn in model coordinates, the item model files, the preview). They live here once so each weapon function in art_hart3.py reads as a list of parts.

Nothing here is random: every noise comes from mesh_lib's seeded generators, so a second run writes byte-identical files.

Units are centimetres of the model (mesh_lib derives the in-game scale from the sprite and the class length), y runs along the weapon (butt low, head high),
mesh -x is forward (docs/weapon_scale_orientation.md).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RELICS = os.path.join(ROOT, "tools", "relics")
sys.path.insert(0, RELICS)
import mesh_lib as ml                                                    # noqa: E402  (the owner's model method)
import wave1_models as w                                                 # noqa: E402  (add / paint_part / set_glow / tint / noise2 helpers of the method)
import display_presets as dp                                             # noqa: E402  (the first person rule of the oversized weapons)
import preview_mesh as pm                                                # noqa: E402  (the software preview)

ASSETS = ml.ASSETS
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
SKIN_DIR = os.path.join(ASSETS, "textures", "entity", "weapon")
ICON_DIR = os.path.join(ASSETS, "textures", "item")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
CLASSES_OUT = os.path.join(HERE, "classes_hart3.json")
SCRATCH = os.environ.get("HART3_SCRATCH", os.path.join(ROOT, "build", "hart3"))      # previews and temporary files (never committed)

V = np.array


def v3(x, y, z):
    return np.array([x, y, z], float)


def unit(v):
    v = np.asarray(v, float)
    n = float(np.linalg.norm(v))
    return v / n if n > 1e-12 else v


def lerp(a, b, t):
    return a + (b - a) * t


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


# ================================================================ parts and surfaces
def add(m, name, material, **kw):
    """A new named part of the mesh (mesh_lib.Part with the method's keywords: base, glow, smooth, uv_mode, wear)."""
    return m.add(ml.Part(name, material=material, **kw))


def quad(part, a, b, c, d, outward):
    """One quad a-b-c-d wound counter-clockwise seen from the side `outward` points to (the game culls back faces)."""
    pts = [np.asarray(q, float) for q in (a, b, c, d)]
    n = np.cross(pts[2] - pts[0], pts[3] - pts[1])
    ids = [part.add_vertex(q) for q in pts]
    if float(np.dot(n, np.asarray(outward, float))) < 0:
        ids = [ids[0], ids[3], ids[2], ids[1]]
    return part.add_face(*ids)


def grid_surface(part, grid, outward):
    """A surface through a grid of points (rows x cols): one quad per cell, facing `outward` (a vector, or f(cell centre) -> vector)."""
    for i in range(len(grid) - 1):
        for j in range(len(grid[i]) - 1):
            a, b, c, d = grid[i][j], grid[i][j + 1], grid[i + 1][j + 1], grid[i + 1][j]
            o = outward(np.mean([a, b, c, d], axis=0)) if callable(outward) else outward
            quad(part, a, b, c, d, o)


def rod(part, p0, p1, r0, r1=None, sides=8, caps=(True, True), up=None, squash=1.0):
    """A tapered round rod from p0 (radius r0) to p1 (radius r1): a spoke, a pole, a tassel strand."""
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    ln = float(np.linalg.norm(p1 - p0))
    r1 = r0 if r1 is None else r1
    return ml.loft(part, p1 - p0, p0, [(0.0, r0, r0 * squash), (ln, r1, r1 * squash)], ml.circle(1.0, sides), caps=caps, up=up)


def tube(part, pts, radius, sides=8, caps=(True, True), up=None, squash=1.0):
    """A tube along a polyline; radius is a number or f(t in 0..1). A radius of 0 at the last point closes it to a point."""
    def sc(t):
        r = radius(t) if callable(radius) else radius
        return (r, r * squash)
    return ml.sweep(part, pts, ml.circle(1.0, sides), scales=sc, caps=caps, up=up)


def circle_path(center, radius, normal, n=24, start=0.0, sweep_deg=360.0, closed=True, e1=None):
    """Points of a circle or an arc round `center` in the plane whose normal is `normal` (counter-clockwise seen from the tip of the normal)."""
    nrm = unit(normal)
    ref = np.asarray(e1, float) if e1 is not None else (np.array([0.0, 1.0, 0.0]) if abs(nrm[1]) < 0.9 else np.array([1.0, 0.0, 0.0]))
    a = unit(ref - nrm * np.dot(ref, nrm))
    b = np.cross(nrm, a)
    pts = []
    steps = n if (closed and abs(sweep_deg - 360.0) < 1e-6) else n
    for i in range(steps + 1):
        th = math.radians(start + sweep_deg * i / steps)
        pts.append(tuple(np.asarray(center, float) + radius * (math.cos(th) * a + math.sin(th) * b)))
    return pts


def sweep_pieces(part, path, section, scales, cuts, up=None):
    """mesh_lib.sweep, built as several lofts cut at the path points `cuts` (indices, first 0, last len - 1). Why: the skin lays out every loft as its own strip, and
    one 200 cm wall in one strip is a very tall rectangle that wastes the atlas; but a plain sweep per piece takes its end tangents from its own points only, so
    two pieces meet at a slightly different angle and leave a hairline gap. Here the frames are computed once along the whole path (the same parallel transport as
    mesh_lib.sweep), and each piece takes its slice, so the pieces share their end rings exactly. Returns the Loft of each piece (caps only at the two far ends)."""
    P = [np.asarray(p, float) for p in path]
    n = len(P)
    tang = [ml._unit(P[min(k + 1, n - 1)] - P[max(k - 1, 0)]) for k in range(n)]
    a0, eu0, ev0 = ml._frame(tang[0], up)
    ev = ev0
    frames, infos = [], []
    for k in range(n):
        if k > 0:
            c = np.cross(tang[k - 1], tang[k])
            if np.linalg.norm(c) > 1e-9:
                ang = math.acos(max(-1.0, min(1.0, float(np.dot(tang[k - 1], tang[k])))))
                ev = ml._unit(ml._rodrigues(ev, c, ang))
        ev = ml._unit(ev - tang[k] * np.dot(ev, tang[k]))
        eu = np.cross(tang[k], ev)
        frames.append((P[k], eu, ev, tang[k]))
        su, sv = scales[k]
        infos.append((k / max(1, n - 1), su, sv, 0.0, 0.0))
    out = []
    for j in range(len(cuts) - 1):
        i0, i1 = cuts[j], cuts[j + 1]
        out.append(ml._build(part, frames[i0:i1 + 1], infos[i0:i1 + 1], section, (j == 0, j == len(cuts) - 2), part.name))
    return out


def torus(part, center, R, r, normal, n=24, sides=8, e1=None, squash=1.0):
    """A ring of round section: major radius R, minor radius r, lying in the plane whose normal is `normal`."""
    pts = circle_path(center, R, normal, n, e1=e1)
    return ml.sweep(part, pts, ml.circle(1.0, sides), scales=lambda t: (r, r * squash), caps=(False, False), up=normal)


def spike(part, base, direction, length, r, sides=5, r_tip=0.0, caps=(True, False)):
    """A cone / thorn from `base` along `direction`: radius r at the base, r_tip (0 = a point) at the end."""
    d = unit(direction)
    return ml.loft(part, d, base, [(0.0, r, r), (length, r_tip, r_tip)], ml.circle(1.0, sides), caps=caps)


def polar_prism(part, axis, origin, half, radius_fn, n=48, bevel=0.06, squash=1.0, stations=None):
    """A thick disc whose outline is r(theta) (a lobed tsuba, a flower, a gear), extruded `half` each way along `axis`, rim bevelled by `bevel`
    (a share of the radius). The outline is star-shaped about the origin, so the caps are plain fans."""
    sec = [(radius_fn(2 * math.pi * i / n) * math.cos(2 * math.pi * i / n), radius_fn(2 * math.pi * i / n) * math.sin(2 * math.pi * i / n) * squash) for i in range(n)]
    st = stations or [(-half, 1.0 - bevel, 1.0 - bevel), (-half * 0.55, 1.0, 1.0), (half * 0.55, 1.0, 1.0), (half, 1.0 - bevel, 1.0 - bevel)]
    return ml.loft(part, axis, origin, st, sec)


def cabochon(part, center, r, h, axis="z", sides=14, squash=1.0, band=0.12, steps=5):
    """A domed gem (a polished cabochon): a short straight girdle and a dome of height h on top, standing out of its setting along `axis`."""
    prof = [(-band * r, r), (0.0, r)]
    for k in range(1, steps + 1):
        a = (math.pi / 2) * k / steps
        prof.append((h * math.sin(a), r * math.cos(a)))
    return ml.lathe(part, axis, center, prof, sides, caps=(True, True), squash=squash)


def bicone(part, center, r, half_len, sides=6, axis="z", girdle=0.12):
    """A cut gem pointed at both ends, symmetric about its centre (a stone set through a thin plate so it shows on both faces)."""
    return ml.lathe(part, axis, center, [(-half_len, 0.0), (-girdle * r, r), (girdle * r, r), (half_len, 0.0)], sides, caps=(False, False))


def cut_gem(part, center, r, long=1.5, sides=6, axis="y"):
    """A brilliant-cut gem: two cones round a short girdle (mesh_lib.gem), long = how far the points stand out in units of r."""
    return ml.gem(part, center, r, long=long, sides=sides, axis=axis)


def bezel(part, center, r_in, r_out, h, axis="z", sides=16, squash=1.0, lip=0.3):
    """A raised setting ring round a gem: a band of height h, rim bands tapering in."""
    return ml.collar(part, 0.0, r_out, h, lip=min(lip, h / 2 - 1e-3), sides=sides, axis=axis, origin=center, oval=squash)


def bezier(p0, p1, p2, p3, n=24):
    """Points of a cubic Bezier curve (2D or 3D)."""
    p0, p1, p2, p3 = (np.asarray(p, float) for p in (p0, p1, p2, p3))
    out = []
    for i in range(n + 1):
        t = i / n
        out.append(tuple(((1 - t) ** 3) * p0 + 3 * ((1 - t) ** 2) * t * p1 + 3 * (1 - t) * t * t * p2 + (t ** 3) * p3))
    return out


def to3(pts, z=0.0):
    """A 2D polyline (x, y) as 3D points at depth z."""
    return [(float(p[0]), float(p[1]), z) for p in pts]


def polyline_lengths(pts):
    P = np.asarray(pts, float)
    seg = np.linalg.norm(np.diff(P, axis=0), axis=1)
    return np.concatenate([[0.0], np.cumsum(seg)])


def sample_path(pts, t):
    """Point and unit tangent at fraction t (0..1, by arc length) of a polyline."""
    P = np.asarray(pts, float)
    cum = polyline_lengths(P)
    s = float(np.clip(t, 0.0, 1.0)) * cum[-1]
    k = int(np.clip(np.searchsorted(cum, s, side="right") - 1, 0, len(P) - 2))
    f = (s - cum[k]) / max(cum[k + 1] - cum[k], 1e-12)
    return P[k] + (P[k + 1] - P[k]) * f, unit(P[k + 1] - P[k])


def resample_path(pts, n):
    """n+1 points spread evenly by arc length along a polyline (the sweeps want evenly spaced frames)."""
    return [tuple(sample_path(pts, i / n)[0]) for i in range(n + 1)]


class Centreline:
    """Why: a curved blade is painted in model coordinates, so a texel has to be turned back into "how far along the blade, how far across it". This holds a
    dense copy of the blade's centre line with its tangents and its +u unit normals (u = tangent x z, the same side mesh_lib's sweep uses), and maps points
    both ways: at(t, u) -> (x, y) and locate(X, Y) -> (t, u) for arrays of points (the nearest sample)."""

    def __init__(self, pts, samples=360):
        P = np.asarray(resample_path(pts, samples), float)
        self.P = P[:, :2]
        self.cum = polyline_lengths(P)
        self.length = float(self.cum[-1])
        T = np.gradient(self.P, axis=0)
        T /= np.maximum(np.linalg.norm(T, axis=1, keepdims=True), 1e-12)
        self.T = T
        self.N = np.stack([T[:, 1], -T[:, 0]], axis=1)             # tangent x z for a tangent in the xy plane

    def at(self, t, u):
        t = np.clip(np.asarray(t, float), 0.0, 1.0)
        k = np.clip(t * (len(self.P) - 1), 0, len(self.P) - 1)
        i0 = np.floor(k).astype(int)
        i1 = np.minimum(i0 + 1, len(self.P) - 1)
        f = (k - i0)[..., None]
        c = self.P[i0] * (1 - f) + self.P[i1] * f
        n = self.N[i0] * (1 - f) + self.N[i1] * f
        n = n / np.maximum(np.linalg.norm(n, axis=-1, keepdims=True), 1e-12)
        return c + n * np.asarray(u, float)[..., None]

    def locate(self, X, Y, chunk=4096):
        """(t, u, dist) for arrays X, Y: t in 0..1 along the line at the nearest sample, u the signed offset along +normal, dist the distance to that sample."""
        X = np.asarray(X, float).ravel()
        Y = np.asarray(Y, float).ravel()
        t = np.empty_like(X)
        u = np.empty_like(X)
        d = np.empty_like(X)
        for a in range(0, len(X), chunk):
            q = np.stack([X[a:a + chunk], Y[a:a + chunk]], axis=1)
            dd = ((q[:, None, :] - self.P[None, :, :]) ** 2).sum(axis=2)
            k = dd.argmin(axis=1)
            t[a:a + chunk] = k / (len(self.P) - 1)
            r = q - self.P[k]
            u[a:a + chunk] = (r * self.N[k]).sum(axis=1)
            d[a:a + chunk] = np.sqrt(dd[np.arange(len(k)), k])
        return t, u, d


def offset_path(pts, offsets, side=(0.0, 0.0, 1.0)):
    """The path displaced sideways by offsets[i] (a number or f(t)) in the plane of the path: along unit(side x tangent) so +offset is to the
    left of the travel direction seen from `side`."""
    P = [np.asarray(p, float) for p in pts]
    n = len(P)
    out = []
    for k in range(n):
        tg = unit(P[min(k + 1, n - 1)] - P[max(k - 1, 0)])
        nrm = unit(np.cross(np.asarray(side, float), tg))
        off = offsets(k / max(1, n - 1)) if callable(offsets) else offsets[k]
        out.append(tuple(P[k] + nrm * off))
    return out


# ================================================================ sections
def section(points_unit):
    """A (u, v) polygon already given in unit width and unit thickness (u and v from -0.5 to 0.5): a thin wrapper that checks the winding."""
    return [tuple(p) for p in points_unit]


def single_edge(bevel=0.30, spine_round=0.10, spine_flat=0.55):
    """The section of a one-edged blade, unit width (u: -0.5 the cutting edge, +0.5 the spine) and unit thickness (v): a flat-ground edge bevel
    on both faces, flat sides, a squared and slightly rounded spine. Counter-clockwise."""
    e = -0.5
    s = 0.5
    r = spine_round
    f = spine_flat * 0.5
    return [(e, 0.0), (e + bevel, 0.5), (s - r, 0.5), (s, f), (s, -f), (s - r, -0.5), (e + bevel, -0.5)]


def double_edge(bevel=0.30, ridge=0.0):
    """A symmetric double-edged blade section (hexagon), unit width and thickness; with ridge > 0 a flat centre strip."""
    hw = 0.5
    f = hw * (1 - bevel)
    return [(hw, 0.0), (f, 0.5), (-f, 0.5), (-hw, 0.0), (-f, -0.5), (f, -0.5)]


def rounded_box(w, t, r=0.25, n=2):
    return ml.rounded_rect(w, t, r, n)


# ================================================================ painting
class Decal:
    """Why: drawing in model coordinates. A decal is a transparent overlay the size of one UV group of the skin. mesh_lib projects every face on the axis its
    normal is closest to (a, b = x, y on a z face; -z, y on an x face; x, -z on a y face, mirrored on the negative faces so a viewer in front sees it upright),
    so `m(x, y, z)` turns a model point into the group's (a, b) and `px` into pixels; shapes are drawn three times larger and shrunk, so edges are soft.
    `rgb` is the body overlay, `glow` the additive glow overlay (alpha = intensity), `cut` a mask of texels made fully transparent (openwork)."""

    SS = 3

    def __init__(self, group):
        self.g = group
        x, y, wd, ht = group.rect
        self.w, self.h = wd, ht
        size = (wd * self.SS, ht * self.SS)
        self.rgb = Image.new("RGBA", size, (0, 0, 0, 0))
        self.glow = Image.new("RGBA", size, (0, 0, 0, 0))
        self.cut = Image.new("L", size, 0)
        self.d = ImageDraw.Draw(self.rgb)
        self.dg = ImageDraw.Draw(self.glow)
        self.dc = ImageDraw.Draw(self.cut)

    def m(self, x, y, z=0.0):
        """The group's (a, b) of a model point."""
        ax, sg = self.g.key
        if ax == 2:
            return (x if sg >= 0 else -x), y
        if ax == 0:
            return (-z if sg >= 0 else z), y
        return x, (-z if sg >= 0 else z)

    def px(self, a, b):
        g = self.g
        return ((a - g.origin[0]) * g.ppu * self.SS, (g.origin[1] - b) * g.ppu * self.SS)

    def pm(self, x, y, z=0.0):
        return self.px(*self.m(x, y, z))

    def pxs(self, pts):
        return [self.px(a, b) for (a, b) in pts]

    def pms(self, pts3):
        return [self.pm(*p) for p in pts3]

    def width(self, cm):
        return max(1, int(round(cm * self.g.ppu * self.SS)))

    def line(self, pts, cm, rgb=None, glow=None, joint="curve"):
        """A polyline through (a, b) points of the group (for a z face: x, y)."""
        P = self.pxs(pts)
        wd = self.width(cm)
        if rgb is not None:
            self.d.line(P, fill=tuple(rgb), width=wd, joint=joint)
        if glow is not None:
            self.dg.line(P, fill=tuple(glow), width=wd, joint=joint)

    def line_m(self, pts3, cm, rgb=None, glow=None, joint="curve"):
        """A polyline through model points (x, y, z)."""
        P = self.pms(pts3)
        wd = self.width(cm)
        if rgb is not None:
            self.d.line(P, fill=tuple(rgb), width=wd, joint=joint)
        if glow is not None:
            self.dg.line(P, fill=tuple(glow), width=wd, joint=joint)

    def polygon(self, pts, rgb=None, glow=None):
        P = self.pxs(pts)
        if rgb is not None:
            self.d.polygon(P, fill=tuple(rgb))
        if glow is not None:
            self.dg.polygon(P, fill=tuple(glow))

    def polygon_m(self, pts3, rgb=None, glow=None, cut=False):
        P = self.pms(pts3)
        if rgb is not None:
            self.d.polygon(P, fill=tuple(rgb))
        if glow is not None:
            self.dg.polygon(P, fill=tuple(glow))
        if cut:
            self.dc.polygon(P, fill=255)

    def ellipse_m(self, c3, r_a, r_b, rgb=None, glow=None, cut=False):
        """An ellipse about a model point with radii r_a, r_b along the group's a and b axes."""
        a, b = self.m(*c3)
        (x0, y0), (x1, y1) = self.px(a - r_a, b + r_b), self.px(a + r_a, b - r_b)
        if rgb is not None:
            self.d.ellipse([x0, y0, x1, y1], fill=tuple(rgb))
        if glow is not None:
            self.dg.ellipse([x0, y0, x1, y1], fill=tuple(glow))
        if cut:
            self.dc.ellipse([x0, y0, x1, y1], fill=255)

    def erase_ellipse_m(self, c3, r_a, r_b):
        """Takes an ellipse out of both overlays (a crescent is a disc minus a disc)."""
        a, b = self.m(*c3)
        (x0, y0), (x1, y1) = self.px(a - r_a, b + r_b), self.px(a + r_a, b - r_b)
        self.d.ellipse([x0, y0, x1, y1], fill=(0, 0, 0, 0))
        self.dg.ellipse([x0, y0, x1, y1], fill=(0, 0, 0, 0))

    def commit(self, img, glow):
        """Composites the overlays onto the group's views of the skin (float RGBA arrays)."""
        ov = np.asarray(self.rgb.resize((self.w, self.h), Image.LANCZOS), np.float32)
        al = ov[..., 3:4] / 255.0
        img[..., :3] = img[..., :3] * (1 - al) + ov[..., :3] * al
        go = np.asarray(self.glow.resize((self.w, self.h), Image.LANCZOS), np.float32)
        glow[...] = np.maximum(glow, go)
        ct = np.asarray(self.cut.resize((self.w, self.h), Image.LANCZOS), np.float32) / 255.0
        if ct.max() > 0:
            img[..., 3] = img[..., 3] * (1 - np.clip(ct * 1.6, 0, 1))
            glow[..., 3] = glow[..., 3] * (1 - np.clip(ct * 1.6, 0, 1))


def decal(atlas, part, axis, sign, fn):
    """Runs fn(decal) on the dominant-axis group of `part` facing `sign` along `axis` ('x' | 'y' | 'z'), then composites it onto the skin."""
    g = atlas.group_for(part, axis, sign)
    x, y, wd, ht = g.rect
    d = Decal(g)
    fn(d)
    d.commit(atlas.img[y:y + ht, x:x + wd], atlas.glow[y:y + ht, x:x + wd])


def model_coords(ctx):
    """For a dominant-axis group (a paint_part context): arrays (X, Y, Z) of the model coordinates of its texels; the axis the group faces is None."""
    ax, sg = ctx.g.key
    A, B = ctx.A, ctx.B
    if ax == 2:
        return (A if sg >= 0 else -A), B, None
    if ax == 0:
        return None, B, (-A if sg >= 0 else A)
    return A, None, (-B if sg >= 0 else B)


def ring_coords(c):
    """For a loft group (a paint_part context): per texel the face of the section ring it lies on and where on that face, all in cm. Returns (seg, u, seglen):
    seg = index of the ring segment (the section's point k to k + 1), u = cm from that segment's start, seglen = the segment's length at this place.
    mesh_lib unrolls a loft as a strip, a row = how far along the loft, a column = how far round the ring (shorter rings are centred, leaving blank margins),
    so the rows can be turned back into rings; a wall swept with a rectangle keeps its inner face, outer face, top and bottom apart this way."""
    rec = c.g.origin
    along = np.asarray(rec["along"], float)
    arcs = np.asarray(rec["arc"], float)
    perim = np.asarray(rec["perim"], float)
    pmax = float(perim.max())
    h, wd = c.img.shape[:2]
    seg = np.zeros((h, wd), int)
    u = np.zeros((h, wd), np.float32)
    ln = np.ones((h, wd), np.float32)
    for r in range(h):
        b = float(c.B[r, 0])
        j = int(np.clip(np.searchsorted(along, b, side="right") - 1, 0, len(along) - 2))
        f = float(np.clip((b - along[j]) / max(along[j + 1] - along[j], 1e-9), 0.0, 1.0))
        row = arcs[j] * (1 - f) + arcs[j + 1] * f
        shift = (pmax - (perim[j] * (1 - f) + perim[j + 1] * f)) / 2
        a = c.A[r] - shift
        k = np.clip(np.searchsorted(row, a, side="right") - 1, 0, len(row) - 2)
        seg[r] = k
        u[r] = a - row[k]
        ln[r] = np.maximum(row[k + 1] - row[k], 1e-6)
    return seg, u, ln


def glow_part(atlas, part, rgb, strength=1.0, core=0.35):
    """The whole part glows in `rgb` (a hot core a little toward white): gems, runes, lantern glass."""
    w.paint_part(atlas, part, lambda c: w.set_glow(c, rgb, strength, core=core))


def paint_gem(atlas, part, rgb, glow_rgb, strength=0.9, core=0.15, facets=6):
    """Paints a cut gem: the body in deep `rgb` with every facet its own brightness and lighter edges, and the glow skin in `glow_rgb` (a saturated colour,
    only `core` of the way to white, so the stone stays coloured instead of burning out). A loft group (a lathed gem) is split into `facets` bands."""
    def fn(c):
        S = c.S
        T = c.T if c.kind == "loft" else np.full(S.shape, 0.5, np.float32)
        f = np.floor(S * facets)
        shade = 0.62 + 0.38 * ((f * 0.37 + 0.11) % 1.0)
        edge = np.abs(S * facets - np.round(S * facets))
        lit = 0.85 + 0.35 * (1 - np.abs(T - 0.5) * 2)
        base = np.array(rgb, np.float32)[None, None, :] * (shade * lit)[..., None]
        base += 70.0 * np.exp(-(edge / 0.05) ** 2)[..., None]
        c.img[..., :3] = np.clip(base, 0, 255)
        w.set_glow(c, glow_rgb, strength, mask=0.55 + 0.45 * shade, core=core)
    w.paint_part(atlas, part, fn)


def shade_img(img, k):
    img[..., :3] = np.clip(img[..., :3] * k, 0, 255)


def blend(img, rgb, amount):
    """Mixes `rgb` into the colour of a patch by `amount` (a float or an array shaped like the patch)."""
    a = np.asarray(amount, np.float32)
    if a.ndim == 2:
        a = a[..., None]
    img[..., :3] = img[..., :3] * (1 - a) + np.array(rgb, np.float32) * a


def lay(m, size=512, ppu=None):
    """Lays out and paints the skin of a mesh with the method's materials; returns the atlas (512 px per look, mesh_lib's rule)."""
    atlas = ml.Atlas(size)
    atlas.layout(m, ppu=ppu)
    atlas.paint(m)
    return atlas


# ================================================================ item models and files
DISPLAY_DIAGONAL = {                                    # vanilla's handheld transforms, as models/item/brooch_pin.json (a weapon on the sprite's diagonal)
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
DISPLAY_UPRIGHT = {                                     # an upright relic in the hand (models/item/spirit_charm.json: the book, the charm, the viola)
    "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f, indent=2)
        f.write("\n")


DISPLAY_CARRIAGE = {                                    # a model on its wheels, nose to the right of the cube (place angle 0): held upright and facing forward
    "thirdperson_righthand": {"rotation": [0, 90, 90], "translation": [0, 3.5, 0.5], "scale": [0.80, 0.80, 0.80]},
    "thirdperson_lefthand": {"rotation": [0, -90, -90], "translation": [0, 3.5, 0.5], "scale": [0.80, 0.80, 0.80]},
    # first person: vanilla's [0, -90, 25] points an item's +x (the carriage's nose) at the camera and puts its centre on the hand at the bottom edge of the
    # screen, so only the roof shows. Turned 108 degrees the other way the carriage drives away from the player and is seen from behind and a little from
    # the left, and the translation lifts it to the lower right third of the picture (checked with a software copy of ItemInHandRenderer's chain).
    "firstperson_righthand": {"rotation": [6, 108, 0], "translation": [-1.6, 6.4, -2.2], "scale": [0.70, 0.70, 0.70]},
    "firstperson_lefthand": {"rotation": [6, -108, 0], "translation": [-1.6, 6.4, -2.2], "scale": [0.70, 0.70, 0.70]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
DISPLAYS = {"diagonal": DISPLAY_DIAGONAL, "upright": DISPLAY_UPRIGHT, "carriage": DISPLAY_CARRIAGE}


def write_item_models(item_id, upright, third_scale=None, display=None):
    """models/item/<id>_icon.json (the flat inventory icon of the 16 x 16 sprite) and models/item/<id>.json (the in-hand model: builtin/entity, drawn by
    WeaponRenderer from weapon_meshes/<id>.txt) with the display transforms (`display` names one of DISPLAYS; else diagonal or upright by `upright`); the first
    person scale of a long mesh is set by the oversized-weapon rule (tools/relics/display_presets.py), so a later run of that tool changes nothing."""
    disp = json.loads(json.dumps(DISPLAYS[display] if display else (DISPLAY_UPRIGHT if upright else DISPLAY_DIAGONAL)))
    if third_scale is not None:
        for k in ("thirdperson_righthand", "thirdperson_lefthand"):
            if k in disp:
                disp[k]["scale"] = [third_scale] * 3
    model = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id}, "display": disp}
    mesh_path = os.path.join(MESH_DIR, item_id + ".txt")
    if os.path.exists(mesh_path):
        info = dp.read_mesh(mesh_path)
        if info is not None:
            plan = dp.plan_for(item_id, model, info)
            model = dp.apply_plan(model, plan)
    write_json(os.path.join(MODEL_DIR, item_id + ".json"), model)
    icon = {"parent": "minecraft:item/generated" if upright else "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}
    write_json(os.path.join(MODEL_DIR, item_id + "_icon.json"), icon)
    return model


def place_upright(m, scale, y_centre=0.5):
    """Places an upright mesh (angle 90) by hand: item units per model unit = `scale`, the mesh box centred in the item's cube. Used where the mesh is
    larger than the sprite's box would allow (mesh_lib.place_on_sprite(fit=True) shrinks it to the box)."""
    lo, hi = m.bbox()
    ymin, ymax = lo[1], hi[1]
    ox = 0.5 - (lo[0] + hi[0]) / 2 * scale
    oy = y_centre - (ymax - ymin) * scale / 2
    m.place(ox, oy, 90.0, scale, ymin)
    return m.place_cfg


def rebleed(atlas):
    """mesh_lib fills the one pixel ring round every UV group with the part's base material before the painters run, so after painting that ring still holds
    the unpainted colour, and a nearest-texel sampler that lands on a group's border draws a hairline of it (a pale line along a seam). This copies each
    group's painted edge texels outward again, on the skin and on the glow skin."""
    b = atlas.bleed
    for g in atlas.groups:
        x, y, wd, ht = g.rect
        for img in (atlas.img, atlas.glow):
            for k in range(1, b + 1):
                img[y - k, x:x + wd] = img[y, x:x + wd]
                img[y + ht - 1 + k, x:x + wd] = img[y + ht - 1, x:x + wd]
            for k in range(1, b + 1):
                img[y - b:y + ht + b, x - k] = img[y - b:y + ht + b, x]
                img[y - b:y + ht + b, x + wd - 1 + k] = img[y - b:y + ht + b, x + wd - 1]


def write_mesh(m, atlas, item_id, glow=("FFFFFF", 0.12, 0.4, 1.0), comment=None):
    """Writes weapon_meshes/<id>.txt (smoothed per-vertex normals, the method's part comments and grip directive) and the two skins."""
    rebleed(atlas)
    m.set_glow(*glow)
    os.makedirs(MESH_DIR, exist_ok=True)
    n = m.write_txt(os.path.join(MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment=comment or "generated by tools/heroweapon/art_hart3.py")
    atlas.save(item_id)
    return n


def preview_files(item_id, out_png, views=("profile", "three_quarter", "edge", "top"), focus=None, panel=None, ss=2):
    """Renders the mesh and skins exactly as written to the game files (the same parser as the game's) into out_png."""
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    return pm.render_preview(os.path.join(MESH_DIR, item_id + ".txt"), None, out_png, views=views, normals="vertex", focus=focus, panel=panel, ss=ss)


def stats_line(item_id, m, atlas):
    st = m.stats(atlas)
    return "%s: %d parts, quads %s, length %.1f cm, skin %d px at %.2f px/cm" % (item_id, st["parts"], st["quads"], st["length"], atlas.size, atlas.ppu)

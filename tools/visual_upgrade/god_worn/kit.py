"""kit: the geometry vocabulary of the God worn upgrade. Every function builds into a GSet (gw_lib) at a Detail level and returns what a caller may need.

Frame (y UP, front is -z, 1 unit = 1 model pixel): see tools/relics/gen_worn_lib.py. Body: torso x -4..4, y -12..0, z -2..2, the back is +z. Cloth is authored outside the
torso (z >= 2.8) because every on-body model is drawn at 0.85 (WornPlacer.BODY_SCALE) and then pushed out of the skin's second layer by WornMesh.clearOf.
"""
import _paths  # noqa: F401
import math

import numpy as np

import gen_worn_lib as L
import mesh_lib as ml
from mesh_lib import Part, sweep, loft, circle, octagon, bevelled, lozenge, ellipse, rect_ring, lathe
from gw_lib import MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT, LIT, ADD

A = np.array


def unit(v):
    v = np.asarray(v, float)
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def basis(d, hint=(0, 1, 0)):
    d = unit(d)
    h = A(hint, float)
    if abs(float(np.dot(d, unit(h)))) > 0.95:
        h = A((1.0, 0, 0))
    u = unit(np.cross(d, h))
    v = np.cross(d, u)
    return d, u, v


def rg(seed):
    return np.random.default_rng(int(seed) & 0xFFFFFFFF)


# ================================================================ rings and links
def link(part, c, u, v, a, b, r, n=10, m=4):
    """A closed tube (a chain link, a ring, a halo band) round the ellipse c + u a cos t + v b sin t, tube radius r, n x m quads. u, v are unit vectors of the ring's plane."""
    c, u, v = A(c, float), unit(u), unit(v)
    nrm = unit(np.cross(u, v))
    rings = []
    for i in range(n):
        t = 2 * math.pi * i / n
        p = c + u * a * math.cos(t) + v * b * math.sin(t)
        rad = unit(u * math.cos(t) * b + v * math.sin(t) * a)
        pts = []
        for k in range(m):
            ph = 2 * math.pi * k / m + math.pi / m
            pts.append(part.add_vertex(p + (rad * math.cos(ph) + nrm * math.sin(ph)) * r))
        rings.append(ml.Ring(pts))
    for i in range(n):
        j = (i + 1) % n
        for k in range(m):
            l = (k + 1) % m
            part.add_face(rings[i][k], rings[j][k], rings[j][l], rings[i][l])
    part.ensure_outward()
    return part


def disc(part, c, u, v, a, b, n=10, thick=0.4):
    """A flat lens disc (two fans) of half sizes a, b in the plane (u, v), `thick` thick along the normal: 2 n quads."""
    c, u, v = A(c, float), unit(u), unit(v)
    nrm = unit(np.cross(u, v))
    top = part.add_vertex(c + nrm * thick / 2)
    bot = part.add_vertex(c - nrm * thick / 2)
    ring_t = [part.add_vertex(c + u * a * math.cos(2 * math.pi * i / n) + v * b * math.sin(2 * math.pi * i / n) + nrm * thick * 0.1) for i in range(n)]
    for i in range(n):
        j = (i + 1) % n
        part.add_face(ring_t[i], ring_t[j], top, top)
        part.add_face(ring_t[j], ring_t[i], bot, bot)
    part.ensure_outward()
    return part


def spike(part, base, tip, r, sides=5, mid=0.35, mid_r=1.0):
    return L.cone(part, tuple(base), tuple(tip), r, sides, mid=mid, mid_r=mid_r)


def crystal(part, base, d, ln, r, sides=6, tip=0.55):
    """A hexagonal crystal: a prism with a pointed end, from `base` along d."""
    base = A(base, float)
    d = unit(d)
    t = base + d * ln
    st = [(0.0, 1.0, 1.0), (ln * (1 - tip), 1.0, 1.0), (ln, 0.0, 0.0)]
    return loft(part, t - base, base, st, circle(r, sides), caps=(True, True))


def beam(part, p0, p1, w, h, up=None, sides=None):
    """A flat bar (a blade, a strap, a plate edge) from p0 to p1, w wide and h thick."""
    p0, p1 = A(p0, float), A(p1, float)
    return loft(part, p1 - p0, p0, [0.0, float(np.linalg.norm(p1 - p0))], rect_ring(w, h), caps=(True, True), up=up)


def tube(part, pts, r, sides=4, taper=None, caps=(True, True)):
    n = len(pts)
    sc = [(1.0, 1.0)] * n if taper is None else [(taper(i / max(1, n - 1)),) * 2 for i in range(n)]
    return sweep(part, pts, circle(r, sides), scales=sc, caps=caps)


def bezier(p0, p1, p2, p3, n):
    return L.bez3(tuple(p0), tuple(p1), tuple(p2), tuple(p3), n)


def catenary(p0, p1, sag, n):
    p0, p1 = A(p0, float), A(p1, float)
    out = []
    for i in range(n + 1):
        t = i / n
        out.append(tuple(p0 + (p1 - p0) * t - A((0, 1, 0)) * sag * 4 * t * (1 - t)))
    return out


def dia(part, c, rx, ry, rz):
    return L.dia(part, tuple(c), rx, ry, rz)


# ================================================================ cloth
def cloth(gs, D, fn, nu, nv, slot=MAIN, tile="cloth", flags=SHELL, group=0, thick=-0.5, name="cloth", density=8.0, uv="tile"):
    p = Part(name)
    L.sheet(p, fn, D.dim(nu), D.dim(nv), thick)
    return gs.add(p, slot, tile, flags, group, uv, density)


def cloth_bands(gs, D, fn, nu, nv, rows, pivot, swing, tile="cloth", slot=MAIN, flags=SHELL, thick=-0.5, name="drape", cols=1, density=8.0, lining=None):
    """A hanging sheet fn(u, v) cut into `rows` horizontal bands (and `cols` columns), each band its own swing group about `pivot`: the hem trails the stride more than the shoulders
    (swing = (a at the first band, a at the last band, idle breathing b)), a rough cloth-physics look from static meshes. Bands overlap a little so no gap opens as they turn.
    Returns the groups."""
    a0, a1, b = swing
    groups = []
    rows = D.dim(rows, 2)
    nvr = max(1, D.dim(nv) // rows)
    ncols = cols
    for r in range(rows):
        t = r / max(1, rows - 1)
        g = gs.group("%s%d" % (name, r), "swing", pivot, a0 + (a1 - a0) * t, b * (0.6 + 0.8 * t), 0.0)
        groups.append(g)
        v0, v1 = r / rows, (r + 1) / rows
        if r > 0:
            v0 -= 0.012
        for c in range(ncols):
            u0, u1 = c / ncols, (c + 1) / ncols

            def sub(u, v, v0=v0, v1=v1, u0=u0, u1=u1):
                return fn(u0 + (u1 - u0) * u, v0 + (v1 - v0) * v)
            p = Part("%s%d_%d" % (name, r, c))
            L.sheet(p, sub, max(1, D.dim(nu) // ncols), nvr, thick)
            gs.add(p, slot, tile, flags, g, "tile", density)
    return groups


def hem_trim(gs, D, fn, v0, nu, group, tile="trim", slot=TRIM, flags=0, thick=-0.62, name="hem", uv="fit", density=8.0):
    p = Part(name)
    L.sheet(p, lambda u, v: fn(u, v0 + (1.0 - v0) * v), D.dim(nu), 1, thick)
    return gs.add(p, slot, tile, flags, group, uv, density)


def edge_cord(gs, D, fn, u, v0, v1, r, group, tile="gilt", slot=TRIM, n=8, name="edge", dz=-0.4, dx=0.0):
    pts = [fn(u, v0 + (v1 - v0) * k / n) for k in range(D.dim(n) + 1)]
    pts = [(p[0] + dx, p[1], p[2] + dz) for p in pts]
    pts = [pts[int(round(i * (len(pts) - 1) / max(1, D.dim(n))))] for i in range(D.dim(n) + 1)] if False else pts
    p = Part(name)
    tube(p, pts, r, 4)
    return gs.add(p, slot, tile, 0, group)


# ================================================================ collars, yokes, shoulder cowls
def yoke(gs, D, a=5.2, b=3.2, y0=-2.2, y1=0.5, t=0.6, tile="cloth", slot=MAIN, flare=0.5, name="yoke", flags=SHELL):
    p = Part(name)
    L.band(p, y0, y1, a, b, t, D.dim(24, 8), 0.1, flare, 3.0)
    return gs.add(p, slot, tile, flags)


def stand_collar(gs, D, y0=0.0, y1=7.0, a=5.0, b=3.5, spread=2.5, tile="gilt", slot=TRIM, back_only=True, n=24, name="collar", flags=SHELL):
    """A tall collar that stands behind the head and flares outward at the top (half ring from the left shoulder round the back to the right shoulder)."""
    p = Part(name)
    rings = []
    nn = D.dim(n, 8)
    for i in range(nn + 1):
        th = math.pi * (0.0 + i / nn) if back_only else 2 * math.pi * i / nn
        ca, sa = math.cos(th), math.sin(th)
        pts = []
        for (r_off, y, fl) in ((0.0, y0, 0.0), (0.55, y0, 0.0), (0.55 - 0.1, y1, spread), (0.0, y1, spread)):
            x = (a + r_off + fl) * ca
            z = (b + r_off + fl * 0.8) * sa
            pts.append(p.add_vertex((x, y, z)))
        rings.append(ml.Ring(pts))
    for i in range(nn):
        for k in range(4):
            l = (k + 1) % 4
            p.add_face(rings[i][k], rings[i + 1][k], rings[i + 1][l], rings[i][l])
    p.ensure_outward()
    return gs.add(p, slot, tile, flags)


def dome(gs, D, c, rx, ry, rz, slot=MAIN, tile="plate", flags=SHELL, lat=5, lon=10, name="dome", sq=1.0, uv="tile", cap=True):
    """A shoulder cowl / pauldron dome: a spheroid, half-hidden under the cloth."""
    p = Part(name)
    prof = [(-ry * math.cos(math.pi * i / lat * (0.5 if not cap else 1.0)) , rx * math.sin(math.pi * i / lat * (0.5 if not cap else 1.0))) for i in range(lat + 1)]
    lathe(p, "y", c, [(s, max(rr, 0.0)) for s, rr in prof], sides=D.sides(lon, 6), squash=rz / max(rx, 1e-6))
    return gs.add(p, slot, tile, flags, 0, uv)


def layered_cowl(gs, D, sx, tiers, slot=MAIN, tile="plate", trim_tile="gilt", flags=SHELL, name="cowl", x0=5.6, y0=1.2, group=0):
    """Overlapping shoulder cowls on one side: each tier a flattened dome lower and wider than the one above, with a trim rim."""
    for k, (dy, rr, hh) in enumerate(tiers):
        p = Part("%s%d_%d" % (name, sx, k))
        lathe(p, "y", (sx * x0, y0 + dy, 0.2), [(-hh, rr * 0.35), (-hh * 0.55, rr * 0.92), (0.0, rr), (hh * 0.35, rr * 0.82), (hh, rr * 0.45)], sides=D.sides(14, 6), squash=0.8)
        gs.add(p, slot if k % 2 == 0 else ACCENT, tile, flags, group)
        rim = Part("%srim%d_%d" % (name, sx, k))
        L.torus(rim, (sx * x0, y0 + dy - hh * 0.05, 0.2), rr * 0.96, 0.28, D.dim(14, 8), 4, 1.0, 0.8)
        gs.add(rim, TRIM, trim_tile, 0, group)


# ================================================================ chains, scales, feathers, shards
def chain_run(gs, D, pts, a=1.05, b=0.7, r=0.26, tile="chain", slot=TRIM, flags=0, group=0, name="chain", spacing=None, plane_hint=(0, 0, 1), n=8, m=4, uv="fit"):
    """A chain of alternating links along a polyline: each link an elliptic tube ring, every other one turned a quarter about the run. Returns the link count."""
    P = [A(p, float) for p in pts]
    total = sum(float(np.linalg.norm(P[i + 1] - P[i])) for i in range(len(P) - 1))
    sp = spacing or (a * 1.55)
    count = max(2, int(total / sp))
    count = max(2, D.cnt(count, 2))
    part = Part(name)
    cum = [0.0]
    for i in range(len(P) - 1):
        cum.append(cum[-1] + float(np.linalg.norm(P[i + 1] - P[i])))
    for k in range(count):
        s = (k + 0.5) / count * cum[-1]
        i = max(0, min(len(P) - 2, int(np.searchsorted(cum, s, side="right") - 1)))
        t = (s - cum[i]) / max(cum[i + 1] - cum[i], 1e-9)
        c = P[i] + (P[i + 1] - P[i]) * t
        d = unit(P[i + 1] - P[i])
        hint = A(plane_hint, float) if k % 2 == 0 else unit(np.cross(d, A(plane_hint, float)))
        dd, u, v = basis(d, hint)
        link(part, c, d, v if k % 2 == 0 else u, a * (sp / (a * 1.55)) ** 0.0, b, r, D.sides(n, 6), max(3, D.sides(m, 3)))
        if part.F and len(part.F) > 1100:
            gs.add(part, slot, tile, flags, group, uv)
            part = Part(name + "x")
    gs.add(part, slot, tile, flags, group, uv)
    return count


def scale_plate(part, c, n_out, up, w, h, thick=0.5, tilt=0.35):
    """One overlapping armour scale (a kite with a raised ridge) centred at c on a surface with outward normal n_out: 5 quads (two ridge faces, two flank faces, the tip)."""
    c = A(c, float)
    n_out = unit(n_out)
    up = unit(up - n_out * float(np.dot(up, n_out)))
    side = unit(np.cross(up, n_out))
    top = c + up * h * 0.5
    tip = c - up * h * 0.5 + n_out * (thick * tilt)
    l = c - side * w * 0.5 + up * h * 0.1
    r = c + side * w * 0.5 + up * h * 0.1
    ridge_t = c + up * h * 0.45 + n_out * thick * 0.5
    ridge_m = c + n_out * thick
    vt, vtip, vl, vr, vrt, vrm = (part.add_vertex(x) for x in (top, tip, l, r, ridge_t, ridge_m))
    part.add_face(vrt, vl, vrm, vrm)
    part.add_face(vrt, vrm, vr, vr)
    part.add_face(vl, vtip, vrm, vrm)
    part.add_face(vrm, vtip, vr, vr)
    part.add_face(vt, vl, vrt, vrt)
    part.add_face(vt, vrt, vr, vr)
    return part


def scale_field(gs, D, fn, nu, nv, w, h, slot=MAIN, tile="scale", flags=SHELL, group=0, name="scales", thick=0.6, stagger=True, uv="fit", skip=None):
    """Rows of overlapping scales over a surface fn(u, v) -> (x, y, z), staggered like roof tiles, lower rows over upper ones."""
    nu, nv = D.dim(nu), D.dim(nv)
    part = Part(name)
    eps = 0.01

    def P(u, v):
        return A(fn(min(max(u, 0.0), 1.0), min(max(v, 0.0), 1.0)), float)

    for j in range(nv):
        for i in range(nu):
            u = (i + (0.5 if (stagger and j % 2) else 0.0) + 0.5) / nu
            v = (j + 0.5) / nv
            if u > 1.0 or (skip and skip(u, v)):
                continue
            c = P(u, v)
            du = P(u + eps, v) - P(u - eps, v)
            dv = P(u, v + eps) - P(u, v - eps)
            nrm = unit(np.cross(du, dv))
            if nrm[2] < 0 and False:
                nrm = -nrm
            scale_plate(part, c, nrm, -unit(dv), w, h, thick)
            if len(part.F) > 1100:
                gs.add(part, slot, tile, flags, group, uv)
                part = Part(name + "x")
    return gs.add(part, slot, tile, flags, group, uv)


def feather_fan(gs, D, base, d, n, ln, spread, w, slot=MAIN, tile="feather", flags=SHELL, group=0, name="fan", curl=0.0, thick=0.3, ranks=1, dz=0.0):
    """A fan of feathers from a base point: n feathers over `spread` (radians) about direction d, length ln, width w."""
    base = A(base, float)
    dd, u, v = basis(d, (0, 0, 1))
    p = Part(name)
    nn = D.cnt(n, 2)
    for rk in range(ranks):
        for k in range(nn):
            t = (k - (nn - 1) / 2.0) / max(1.0, (nn - 1) / 2.0)
            ang = t * spread
            dirv = unit(dd * math.cos(ang) + u * math.sin(ang))
            tip = base + dirv * ln * (1.0 - 0.18 * abs(t) - 0.12 * rk)
            L.feather(p, tuple(base + dd * 0.2 * rk), tuple(tip), w * (1 - 0.15 * rk), thick, 4 if D.full else 3, curl=curl, dz=dz)
    return gs.add(p, slot, tile, flags, group, "fit")


def shards(gs, D, centre, R, n, size, y_amp=1.5, kind="orbit", speed=2.0, slot=GLOW, tile="crystal", flags=EMIT | HOT, name="shards", tall=1.8, phase=0.0, groups=1):
    if tile in ("spark", "soft", "ember", "ring_glow", "flame", "stars"):
        flags |= ADD
    """Orbiting shards (octahedra): `groups` orbit groups, each with n/groups shards on the circle of radius R round `centre`."""
    cx, cy, cz = centre
    n = D.cnt(n, 2)
    gids = []
    for gi in range(groups):
        sp = speed * (1 + 0.35 * gi) * (1 if gi % 2 == 0 else -1)
        g = gs.group("%s%d" % (name, gi), kind, (cx, cy, cz), sp, 0.0, 0.0)
        gids.append(g)
        part = Part("%s%d" % (name, gi))
        per = max(1, n // groups)
        for k in range(per):
            a = 2 * math.pi * k / per + phase + gi * 0.9
            c = (cx + R * math.cos(a), cy + y_amp * math.sin(k * 2.3 + gi), cz + R * math.sin(a))
            dia(part, c, size, size * tall, size)
        gs.add(part, slot, tile, flags, g, "fit")
    return gids


def ring_halo(gs, D, c, R, r, plane="xz", n=24, m=4, slot=GLOW, tile="ring_glow", flags=EMIT | HOT | ADD, group=0, name="halo", uv="fit"):
    p = Part(name)
    L.torus(p, tuple(c), R, r, D.dim(n, 8), 4, plane=plane)
    return gs.add(p, slot, tile, flags, group, uv)


def rune_ring(gs, D, c, R, h, n=32, group=0, slot=GLOW, tile="rune_band", flags=LIT | EMIT, thick=0.5, name="runes", tilt_a=0.0):
    p = Part(name)
    L.band(p, c[1] - h / 2, c[1] + h / 2, R, R, thick, D.dim(n, 8), 0.0, 0.0, 2.0)
    for i in range(len(p.V)):
        v = p.V[i]
        p.V[i] = A((v[0] + c[0], v[1], v[2] + c[2]), float)
    return gs.add(p, slot, tile, flags, group, "fit")


def ribbon(gs, D, pts, w, slot=MAIN, tile="silk", flags=SHELL, group=0, name="ribbon", thick=0.25, side=(1, 0, 0)):
    """A flat ribbon along a polyline, its width across `side` (projected off the path)."""
    P = [A(p, float) for p in pts]
    left, right = [], []
    s = unit(A(side, float))
    for i, p in enumerate(P):
        d = unit(P[min(i + 1, len(P) - 1)] - P[max(i - 1, 0)])
        sv = unit(s - d * float(np.dot(s, d)))
        t = i / max(1, len(P) - 1)
        ww = w * (1.0 - 0.55 * t)
        left.append(tuple(p - sv * ww / 2))
        right.append(tuple(p + sv * ww / 2))
    part = Part(name)
    L.quad_strip(part, left, right, thick)
    return gs.add(part, slot, tile, flags, group, "fit")


def gem(gs, D, c, r, long=1.4, sides=6, axis="z", slot=GLOW, tile="gem", flags=EMIT | HOT, group=0, name="gem"):
    p = Part(name)
    ml.gem(p, tuple(c), r, long, D.sides(sides, 4), axis)
    return gs.add(p, slot, tile, flags, group, "fit")


def filigree(gs, D, c, normal, R, turns=2.2, r=0.17, segs=36, arms=2, slot=TRIM, tile="gilt", flags=0, group=0, name="filigree", plane_up=(0, 1, 0), lit=True):
    """Spiral scrollwork: `arms` spiral tubes growing from the centre c in the plane normal to `normal`, `segs` segments each (4 sided: 4 quads a segment)."""
    d, u, v = basis(normal, plane_up)
    part = Part(name)
    nseg = D.dim(segs, 8)
    for arm in range(arms):
        pts = []
        for i in range(nseg + 1):
            t = i / nseg
            a = turns * 2 * math.pi * t + arm * math.pi * 2 / arms
            rr = R * (0.12 + 0.88 * t)
            pts.append(A(c, float) + u * rr * math.cos(a) + v * rr * math.sin(a) + d * 0.3 * math.sin(t * 6))
        tube(part, pts, r, D.sides(4, 3), taper=lambda t: 1.0 - 0.5 * t)
    gs.add(part, slot, tile, flags | (LIT if lit and tile in ("gilt", "metal", "rune_band") else 0), group, "tile")
    return part


def wisp(gs, D, base, d, ln, r, n=4, group=0, slot=GLOW, tile="shadow", flags=EMIT | LIT, name="wisp", bend=1.5, seed=1, wave=True):
    """Flame-like tendrils rising from base: n tubes that taper and bend, each a swaying sweep."""
    rr = rg(seed)
    base = A(base, float)
    dd, u, v = basis(d, (0, 0, 1))
    part = Part(name)
    n = D.cnt(n, 2)
    for k in range(n):
        off = u * rr.uniform(-1.5, 1.5) + v * rr.uniform(-0.7, 0.7)
        bnd = u * rr.uniform(-bend, bend) + v * rr.uniform(-bend, bend)
        L_ = ln * rr.uniform(0.7, 1.15)
        pts = bezier(base + off, base + off + dd * L_ * 0.35 + bnd * 0.4, base + off + dd * L_ * 0.7 + bnd, base + off + dd * L_ + bnd * 1.3, D.dim(7, 3))
        tube(part, pts, r * rr.uniform(0.8, 1.2), D.sides(4, 3), taper=lambda t: 1.0 - 0.92 * t ** 0.8, caps=(True, False))
    gs.add(part, slot, tile, flags, group, "tile")
    return part

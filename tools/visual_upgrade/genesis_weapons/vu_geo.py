"""Geometry builders for the genesis_weapons upgrade, on top of tools/relics/mesh_lib.py (loft, sweep, lathe): layered blades with fullers and teeth, tori,
tubes, cones, cut gems, feather fans, ribbons, chains, halo cards. Every builder takes the `Item` so counts scale with the detail level (`it.n`).
"""
import math

import numpy as np

import vu_item as vi
from vu_item import ml, HALO_CELLS


def unit(v):
    v = np.asarray(v, float)
    n = np.linalg.norm(v)
    return v / n if n > 1e-12 else v


def basis(normal):
    """(u, v, n): an orthonormal frame whose third axis is `normal`."""
    n = unit(normal)
    a = np.array([0.0, 1.0, 0.0]) if abs(n[1]) < 0.9 else np.array([1.0, 0.0, 0.0])
    u = unit(np.cross(a, n))
    v = np.cross(n, u)
    return u, v, n


def catmull(pts, n):
    """A Catmull-Rom spline through the points, n samples per segment (an array (len, dims))."""
    P = np.asarray(pts, float)
    if len(P) < 3:
        return P
    Q = np.vstack([2 * P[0] - P[1], P, 2 * P[-1] - P[-2]])
    out = []
    for i in range(1, len(Q) - 2):
        p0, p1, p2, p3 = Q[i - 1], Q[i], Q[i + 1], Q[i + 2]
        for k in range(n):
            t = k / n
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(P[-1])
    return np.array(out)


def rot_about(v, axis, deg):
    return ml._rodrigues(np.asarray(v, float), np.asarray(axis, float), math.radians(deg))


# ================================================================ blades
def blade_section(bevel=0.45, groove=0.0, edge_hone=0.0):
    """A unit blade section (half width 1, half thickness 1), counter-clockwise: edge tips at (-1, 0) and (1, 0), flats of height 1; with `groove` (share of the half width)
    a fuller down the middle of both faces."""
    f = 1.0 - bevel
    top = [(-1.0, 0.0), (-f, 1.0)]
    if groove > 0:
        g = groove
        top += [(-g * 1.15, 1.0), (-g * 0.7, 0.35), (g * 0.7, 0.35), (g * 1.15, 1.0)]
    top += [(f, 1.0), (1.0, 0.0)]
    bot = [(f, -1.0)]
    if groove > 0:
        g = groove
        bot += [(g * 1.15, -1.0), (g * 0.7, -0.35), (-g * 0.7, -0.35), (-g * 1.15, -1.0)]
    bot += [(-f, -1.0)]
    pts = top + bot
    # counter-clockwise: the top row runs from the left tip to the right tip, so reverse it to run (right tip -> left) over the top
    ring = [(-x, y) for (x, y) in pts]
    return ring[::-1] if _signed_area(ring) < 0 else ring


def _signed_area(pts):
    p = np.asarray(pts, float)
    return 0.5 * float(np.sum(p[:, 0] * np.roll(p[:, 1], -1) - np.roll(p[:, 0], -1) * p[:, 1]))


def blade(it, p, stations, section=None, bevel=0.45, groove=0.0, caps=(True, True)):
    """A blade along y. stations = [(y, hw_left, hw_right, half_thickness)] (edge at -x = hw_left, spine at +x = hw_right); a repeated y makes a step (a tooth, a notch);
    hw both 0 closes it to a point. Returns the Loft."""
    sec = section or blade_section(bevel, groove)
    st = []
    for (y, hl, hr, ht) in stations:
        hw = (hl + hr) / 2.0
        st.append((y, max(hw, 1e-4) if (hl + hr) > 0 else 0.0, ht if (hl + hr) > 0 else 0.0, (hr - hl) / 2.0, 0.0))
    return ml.loft(p, "y", (0, 0, 0), st, sec, caps=caps)


def blade_by(it, p, y0, y1, hw, ht, n=40, shift=None, bevel=0.45, groove=0.0, tip=0.12, tip_style="point", edge=None, hw_spine=None):
    """A blade from functions of t in 0..1: hw(t), ht(t), shift(t) (the centre line), edge(t) added on the edge side only (teeth, a curved edge).
    The last `tip` share of the length closes to a point (point), or to a flat (flat)."""
    n = it.n(n, 6)
    st = []
    for i in range(n + 1):
        t = i / n
        y = y0 + (y1 - y0) * t
        h = hw(t)
        e = edge(t) if edge else 0.0
        sp = hw_spine(t) if hw_spine else h
        cx = shift(t) if shift else 0.0
        th = ht(t)
        if tip > 0 and t > 1 - tip and tip_style == "point":
            k = max(0.0, (1 - t) / tip)
            h, sp, th, e = h * k, sp * k, th * k, e * k
        st.append((y, h + e - cx, sp + cx, th))
    return blade(it, p, st, bevel=bevel, groove=groove, caps=(True, tip_style == "flat" or tip <= 0))


# ================================================================ rods, tubes, lathes
def lathe_y(it, p, y, profile, sides=14, x=0.0, z=0.0, caps=(True, True), squash=1.0):
    """A surface of revolution about the y axis through (x, z): profile = [(dy, radius)]."""
    return ml.lathe(p, "y", (x, y, z), profile, sides=it.n(sides, 5), caps=caps, squash=squash)


def rod(it, p, y0, y1, r0, r1=None, sides=10, x=0.0, z=0.0, swell=None, caps=(True, True), squash=1.0):
    r1 = r0 if r1 is None else r1
    ts = sorted(set([0.0, 1.0] + [t for t, _ in (swell or [])]))
    sw = {t: f for t, f in (swell or [])}
    st = [(y0 + (y1 - y0) * t, (r0 + (r1 - r0) * t) * sw.get(t, 1.0), (r0 + (r1 - r0) * t) * sw.get(t, 1.0) * squash, x, z) for t in ts]
    return ml.loft(p, "y", (0, 0, 0), st, ml.circle(1.0, it.n(sides, 5)), caps=caps)


def tube(it, p, pts, r0, r1=None, sides=8, tip=False, thick=1.0, scale=None, up=None, caps=(False, False)):
    """A round tube along a polyline [(x, y, z)], radius r0 to r1; tip closes the end to a point."""
    r1 = r0 if r1 is None else r1

    def sc(t):
        if scale is not None:
            s = scale(t)
            return (r0 * s, r0 * s * thick)
        if tip and t > 0.97:
            return (0.0, 0.0)
        r = r0 + (r1 - r0) * t
        return (r, r * thick)
    return ml.sweep(p, [tuple(q) for q in pts], ml.circle(1.0, it.n(sides, 4)), scales=sc, up=up, caps=caps if not tip else (False, False))


def torus(it, p, center, R, r, normal=(0, 0, 1), major=48, minor=8, a0=0.0, a1=360.0, tilt=None, r_scale=None, start_vec=None):
    """A ring / arc of radius R round `center` in the plane with this normal, tube radius r (a function of t via r_scale)."""
    u, v, n = basis(normal)
    if start_vec is not None:
        u = unit(np.asarray(start_vec, float) - n * np.dot(start_vec, n))
        v = np.cross(n, u)
    c = np.asarray(center, float)
    m = it.n(major, 8)
    pts = []
    for i in range(m + 1):
        a = math.radians(a0 + (a1 - a0) * i / m)
        pts.append(tuple(c + R * (math.cos(a) * u + math.sin(a) * v)))
    sc = (lambda t: (r_scale(t), r_scale(t))) if r_scale else (lambda t: (r, r))
    return ml.sweep(p, pts, ml.circle(1.0, it.n(minor, 4)), scales=sc, caps=(False, False))


def cone(p, base, tip, r, sides=5, up=None, r2=0.0):
    b, t = np.asarray(base, float), np.asarray(tip, float)
    L = float(np.linalg.norm(t - b))
    if L < 1e-6:
        return
    ml.loft(p, t - b, b, [(0.0, 1.0, 1.0), (L * 0.985, r2 / max(r, 1e-6), r2 / max(r, 1e-6)) if r2 > 0 else (L * 0.985, 0.0, 0.0)], ml.circle(r, sides), up=up)


def spike_ring(it, p, center, normal, n, r_in, r_out, length, rad, sides=4, tilt=0.0, phase=0.0):
    """n spikes round a ring: from radius r_in to r_out + along the normal for `length` (a crown, a sunburst, thorns)."""
    u, v, nn = basis(normal)
    c = np.asarray(center, float)
    for i in range(it.n(n, 3)):
        a = phase + 2 * math.pi * i / it.n(n, 3)
        d = math.cos(a) * u + math.sin(a) * v
        b = c + d * r_in
        t = c + d * r_out + nn * length * math.cos(tilt) + d * length * math.sin(tilt)
        cone(p, b, t, rad, sides)


def gem(it, p, c, r, long=1.4, sides=8, axis="y", squash=1.0):
    """A cut gem: crown, girdle, pavilion."""
    c = np.asarray(c, float)
    prof = [(-r * long, 0.0), (-r * 0.35 * long, r * 0.86), (-r * 0.05, r), (r * 0.12, r), (r * 0.5 * long, r * 0.6), (r * 0.62 * long, r * 0.34)]
    return ml.lathe(p, axis, c, prof, sides=it.n(sides, 4), caps=(False, True), squash=squash)


def orb(it, p, c, rx, ry=None, rz=None, sides=14, stacks=9):
    """A faceted ellipsoid centred on c."""
    ry = rx if ry is None else ry
    rz = rx if rz is None else rz
    c = np.asarray(c, float)
    sides, stacks = it.n(sides, 5), it.n(stacks, 4)
    prof = []
    for i in range(stacks + 1):
        a = -math.pi / 2 + math.pi * i / stacks
        prof.append((ry * math.sin(a), max(1e-4, math.cos(a))))
    st = [(s, 1.0 * r * rx, r * rz) for (s, r) in prof]
    st[0] = (st[0][0], 0.0, 0.0)
    st[-1] = (st[-1][0], 0.0, 0.0)
    return ml.loft(p, "y", c, st, ml.circle(1.0, sides), caps=(False, False))


def ribbon(it, p, pts, width, thick=0.12, w_fn=None, sections=None, up=(0, 0, 1)):
    """A flat strip along a path (cloth, a comet tail, a banner): width (a function of t optional), thin."""
    sec = ml.rect_ring(1.0, 1.0)
    return ml.sweep(p, [tuple(q) for q in pts], sec, scales=lambda t: ((w_fn(t) if w_fn else width), thick), up=up)


def plate(it, p, c, u_vec, v_vec, hw, hh, thick, n_u=1, bevel=0.0):
    """A thin box plate centred on c spanned by two vectors (a rune plate, a shield tile)."""
    c = np.asarray(c, float)
    uu, vv = unit(u_vec), unit(v_vec)
    nn = np.cross(uu, vv)
    sec = ml.octagon(2 * hw, 2 * thick, min(hw, thick) * bevel) if bevel > 0 else ml.rect_ring(2 * hw, 2 * thick)
    return ml.loft(p, vv, c - vv * hh, [(0.0, 1, 1), (2 * hh, 1, 1)], sec, frame=(uu, nn))


def bar3(p, p0, p1, w, h, up=None, taper=0.0):
    return ml.bar(p, p0, p1, w, h, up=up, taper=taper)


def studs(p, pts, r, h, normal=(0, 0, 1), sides=6, dome=0.55):
    for pt in pts:
        ml.stud(p, pt, r, h, normal, sides, dome)


def feather(it, p, root, direction, length, width, thick=0.25, curl=0.0, side=(0, 0, 1), n=10, tip="point"):
    """A lens-shaped plate (a feather, a leaf, a petal, a flame tongue) from `root` along `direction`, curling toward `side` by `curl` (fraction of length)."""
    d = unit(direction)
    s = unit(np.asarray(side, float) - d * np.dot(side, d))
    r0 = np.asarray(root, float)
    pts = []
    m = it.n(n, 3)
    for i in range(m + 1):
        t = i / m
        pts.append(tuple(r0 + d * length * t + s * curl * length * t * t))
    w0 = np.cross(d, s)

    def sc(t):
        k = math.sin(math.pi * min(1.0, t * 0.92 + 0.04)) ** 0.7
        if t > 0.97:
            return (0.0, 0.0)
        return (width * (0.35 + 0.65 * k), thick)
    return ml.sweep(p, pts, ml.bevelled(1.0, 1.0, 0.5), scales=sc, up=tuple(unit(np.cross(d, w0)) if np.linalg.norm(np.cross(d, w0)) > 1e-6 else (0, 0, 1)))


def feather_fan(it, p, root, axis_dir, spread_deg, n, length, width, thick=0.25, curl=0.0, side=(0, 0, 1), swing_axis=(0, 0, 1), len_fn=None, n_st=10):
    """n feathers fanned about swing_axis through `root` from -spread/2 to +spread/2 round axis_dir."""
    count = it.n(n, 3)
    for i in range(count):
        t = i / max(1, count - 1)
        ang = -spread_deg / 2 + spread_deg * t
        d = rot_about(axis_dir, swing_axis, ang)
        L = length * (len_fn(t) if len_fn else 1.0)
        feather(it, p, root, d, L, width, thick, curl, side, n=n_st)


def chain(it, p, pts, link_len=1.6, link_w=1.0, tube_r=0.16, sides=5, up=(0, 0, 1)):
    """A chain along a polyline: alternating links turned 90 degrees, each a stadium of a swept circle."""
    P = [np.asarray(q, float) for q in pts]
    seg = [float(np.linalg.norm(P[i + 1] - P[i])) for i in range(len(P) - 1)]
    total = sum(seg)
    count = max(2, int(total / (link_len * 0.78)))
    count = count if it.full else max(2, int(count * 0.4))

    def at(s):
        acc = 0.0
        for i, L in enumerate(seg):
            if acc + L >= s or i == len(seg) - 1:
                t = (s - acc) / max(L, 1e-9)
                return P[i] + (P[i + 1] - P[i]) * min(1.0, max(0.0, t)), unit(P[i + 1] - P[i])
            acc += L
        return P[-1], unit(P[-1] - P[-2])
    for k in range(count):
        c, d = at(total * (k + 0.5) / count)
        a = unit(np.cross(d, up))
        b = np.cross(a, d)
        w = a if k % 2 == 0 else b
        loop = []
        m = 12 if it.full else 6
        for i in range(m + 1):
            th = 2 * math.pi * i / m
            hl, hw = link_len / 2, link_w / 2
            x = math.cos(th) * hw
            y = math.sin(th) * hl
            loop.append(tuple(c + w * x + d * y))
        ml.sweep(p, loop, ml.circle(tube_r, sides if it.full else 3), caps=(False, False))


def ladder_rungs(p, y0, y1, n, width, h, z=0.0):
    for i in range(n):
        y = y0 + (y1 - y0) * (i + 0.5) / n
        ml.bar(p, (-width / 2, y, z), (width / 2, y, z), 0.3, h, up=(0, 0, 1), taper=0.2)


# ================================================================ halo cards (layer 4, additive, no skin)
def card(it, name, group, center, u_vec, v_vec, hw, hh, cell="glow", tint=None, double=True):
    """A flat additive card (a glow disc, a ring, a light shaft, a flare) centred on c, spanned by u / v; both faces. Needs a group (animated or static)."""
    if not it.full:
        return None
    p = next((q for q in it.mesh.parts if q.name == name), None)
    if p is None:
        p = it.part(name, "steel", layer=4, group=group, smooth=0.0)
        p.cell = cell
        p.cardUV = []
        p.ensure_outward = lambda: False                      # two crossed faces look closed to the winding check; a card must keep both windings as built
    c = np.asarray(center, float)
    uu, vv = unit(u_vec), unit(v_vec)
    corners = [c - uu * hw - vv * hh, c + uu * hw - vv * hh, c + uu * hw + vv * hh, c - uu * hw + vv * hh]
    cu = HALO_CELLS[cell]
    u0, v0, u1, v1 = (cu % 4) * 32.0 + 0.5, (cu // 4) * 32.0 + 0.5, (cu % 4) * 32.0 + 31.5, (cu // 4) * 32.0 + 31.5
    uvs = [(u0, v1), (u1, v1), (u1, v0), (u0, v0)]
    idx = [p.add_vertex(q) for q in corners]
    if p.add_face(*idx) is not None:
        p.cardUV.append(list(uvs))
    if double:
        idx2 = [p.add_vertex(q) for q in corners[::-1]]
        if p.add_face(*idx2) is not None:
            p.cardUV.append(list(uvs[::-1]))
    return p


def card_cross(it, name, group, center, axis, hw, hh, cell="shaft", n=2):
    """n cards crossing along `axis` (a light shaft that reads from every side): spanned by the axis and a rotating perpendicular."""
    ax = unit(axis)
    u, v, _ = basis(ax)
    for i in range(n):
        a = math.pi * i / n
        w = math.cos(a) * u + math.sin(a) * v
        card(it, name, group, center, w, ax, hw, hh, cell)


def ring_cards(it, name, group, center, normal, radius, count, size, cell="glow"):
    """count small glow cards on a ring (beads of light), each facing along `normal` and crossed."""
    u, v, n = basis(normal)
    for i in range(count):
        a = 2 * math.pi * i / count
        c = np.asarray(center, float) + radius * (math.cos(a) * u + math.sin(a) * v)
        card(it, name, group, c, u, v, size, size, cell)
        card(it, name, group, c, u, n, size, size, cell)

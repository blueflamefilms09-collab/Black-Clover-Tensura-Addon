"""geo: geometry helpers for the visual upgrade models, on top of tools/relics/mesh_lib.py (Part, loft, lathe, sweep) and gen_worn_lib.py (torus, band, horn ...).

Everything is built in the authoring frame of the relic models: Y UP, front is -Z, 1 unit = 1 model pixel (1/16 block). A primitive is built ONCE in its own frame
(`prim_*`: axis along +y, centred on the origin) and `merge()`d many times at other places; the merged copies keep the loft records of the source, so the repeated copies
share one unrolled UV island of the skin (a hundred filigree leaves cost the texels of one).
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
RELICS = os.path.abspath(os.path.join(HERE, "..", "..", "relics"))
sys.path.insert(0, RELICS)
import mesh_lib as ml  # noqa: E402
from mesh_lib import Part, loft, lathe, sweep, circle, ellipse, rect_ring, octagon, lozenge, bevelled, resample, scale_sec, shift_sec  # noqa: E402
import gen_worn_lib as GL  # noqa: E402

bez, bez3 = GL.bez, GL.bez3


def v3(p):
    return np.asarray(p, np.float64)


def unit(v):
    v = v3(v)
    n = float(np.linalg.norm(v))
    return v / n if n > 1e-12 else v


def basis(normal):
    """Two unit vectors perpendicular to `normal` and to each other (right-handed with normal): (u, v, n)."""
    n = unit(normal)
    ref = np.array([0.0, 1.0, 0.0]) if abs(n[1]) < 0.9 else np.array([1.0, 0.0, 0.0])
    u = unit(np.cross(ref, n))
    v = np.cross(n, u)
    return u, v, n


def rot_matrix(axis, deg):
    """The 3x3 rotation about `axis` (a vector or 'x' 'y' 'z') by `deg` degrees (right-handed)."""
    if isinstance(axis, str):
        axis = {"x": (1, 0, 0), "y": (0, 1, 0), "z": (0, 0, 1)}[axis]
    k = unit(axis)
    a = math.radians(deg)
    K = np.array([[0, -k[2], k[1]], [k[2], 0, -k[0]], [-k[1], k[0], 0]])
    return np.eye(3) + math.sin(a) * K + (1 - math.cos(a)) * (K @ K)


def align_y(direction):
    """A rotation that turns +y onto `direction`."""
    d = unit(direction)
    y = np.array([0.0, 1.0, 0.0])
    c = float(np.dot(y, d))
    if c > 0.999999:
        return np.eye(3)
    if c < -0.999999:
        return rot_matrix("x", 180)
    ax = np.cross(y, d)
    return rot_matrix(ax, math.degrees(math.acos(c)))


def merge(dst, src, M=None, offset=(0, 0, 0)):
    """Appends a copy of `src` (a Part) to `dst`, transformed p -> M p + offset; the loft tags of the source are kept (shared UV island)."""
    M = np.eye(3) if M is None else v3(M)
    base = len(dst.V)
    for v in src.V:
        dst.V.append(M @ v + v3(offset))
    flip = float(np.linalg.det(M)) < 0
    for f, t in zip(src.F, src.tags):
        idx = (f[0] + base, f[1] + base, f[2] + base, f[3] + base)
        if flip:
            idx = (idx[0], idx[3], idx[2], idx[1])
        dst.F.append(idx)
        dst.tags.append(t if not flip or t is None else (t[0], t[1], [t[2][0], t[2][3], t[2][2], t[2][1]]))
    dst.UV = dst.N = None
    return dst


def new_part(name="tmp", material="steel", **kw):
    return Part(name, material=material, **kw)


# ------------------------------------------------------------------ primitives (axis +y, centred on the origin unless noted)
def prim_lathe(profile, sides=8, squash=1.0, caps=(True, True)):
    p = new_part("prim")
    lathe(p, "y", (0, 0, 0), profile, sides=sides, squash=squash, caps=caps)
    return p


def prim_gem(r, long=1.4, sides=6, girdle=0.15):
    """A cut gem: point down, point up, a girdle band between."""
    p = new_part("gem")
    lathe(p, "y", (0, 0, 0), [(-r * long, 0.0), (-r * girdle, r), (r * girdle, r), (r * long * 0.7, 0.0)], sides=sides, caps=(False, False))
    return p


def prim_octa(r, h):
    """A bipyramid (8 triangles): the cheapest faceted jewel; half height h, girdle radius r, 4 sides."""
    p = new_part("octa")
    t, b = p.add_vertex((0, h, 0)), p.add_vertex((0, -h, 0))
    eq = [p.add_vertex(d) for d in ((r, 0, 0), (0, 0, r), (-r, 0, 0), (0, 0, -r))]
    for i in range(4):
        j = (i + 1) % 4
        p.add_face(eq[i], eq[j], t, t)
        p.add_face(eq[j], eq[i], b, b)
    p.ensure_outward()
    return p


def prim_crystal(r, h, sides=6, tip=0.45):
    """A hexagonal crystal column with pointed ends, from y = -h/2 to h/2."""
    p = new_part("crystal")
    lathe(p, "y", (0, 0, 0), [(-h / 2, 0.0), (-h / 2 + h * tip * 0.5, r), (h / 2 - h * tip, r), (h / 2, 0.0)], sides=sides, caps=(False, False))
    return p


def prim_cone(r, h, sides=6, caps=True):
    p = new_part("cone")
    lathe(p, "y", (0, 0, 0), [(-h / 2, r), (h / 2, 0.0)], sides=sides, caps=(caps, False))
    return p


def prim_sphere(r, lat=6, lon=10, squash=1.0):
    p = new_part("sphere")
    GL.sph(p, (0, 0, 0), r, lat=lat, lon=lon, squash=squash)
    return p


def prim_box(w, h, d, chamfer=0.0):
    p = new_part("box")
    sec = octagon(w, d, chamfer) if chamfer > 0 else rect_ring(w, d)
    loft(p, "y", (0, 0, 0), [-h / 2, h / 2], sec)
    return p


def prim_tube(path, r, sides=6, scales=None, caps=(True, True)):
    p = new_part("tube")
    sweep(p, path, circle(r, sides), scales=scales, caps=caps)
    return p


def prim_torus(R, r, n=16, m=6, plane="xz"):
    """A ring in the xz plane (axis y) by default; 'xy' puts its axis on z, 'yz' on x."""
    p = new_part("torus")
    GL.torus(p, (0, 0, 0), R, r, n=n, m=m, plane=plane)
    return p


def prim_link(R, r, n=10, m=5, squash=0.62):
    """One chain link: a squashed torus, long along y, axis z."""
    p = new_part("link")
    GL.torus(p, (0, 0, 0), R, r, n=n, m=m, a_scale=squash, b_scale=1.0, plane="xy")
    return p


def prim_leaf(length, width, thick, sides=4, bend=0.0):
    """A leaf / petal / feather blade along +y from the origin: a lens loft with a gentle bend toward +z."""
    p = new_part("leaf")
    st = [(0.0, 0.25, 1.0), (length * 0.35, 1.0, 1.0), (length * 0.75, 0.62, 0.9), (length, 0.0, 0.0)]
    loft(p, "y", (0, 0, 0), st, ellipse(width / 2, thick / 2, sides * 2), caps=(True, True))
    if bend:
        for i, v in enumerate(p.V):
            p.V[i] = v3((v[0], v[1], v[2] + bend * (v[1] / max(length, 1e-6)) ** 2 * length))
    return p


def prim_blade(length, width, thick, tip=0.25):
    """A flat blade / sword / spike: bevelled section, point at +y, base at the origin."""
    p = new_part("blade")
    loft(p, "y", (0, 0, 0), [(0.0, 1.0, 1.0), (length * (1 - tip), 1.0, 1.0), (length, 0.0, 0.0)], bevelled(width, thick, 0.5), caps=(True, True))
    return p


def prim_card(w, h, t):
    """A thin card standing in the xy plane (w along x, h along y), centred."""
    p = new_part("card")
    loft(p, "z", (0, 0, 0), [-t / 2, t / 2], rect_ring(w, h))
    return p


def prim_spike(r, h, sides=4):
    p = new_part("spike")
    lathe(p, "y", (0, 0, 0), [(0.0, r), (h, 0.0)], sides=sides, caps=(True, False))
    return p


# ------------------------------------------------------------------ placing
def place(dst, src, pos=(0, 0, 0), up=None, spin=0.0, scale=1.0):
    """merge() with a scale, a turn about the primitive's own axis (`spin` degrees) and an orientation (+y onto `up`)."""
    M = np.eye(3) * scale
    if spin:
        M = rot_matrix("y", spin) @ M
    if up is not None:
        M = align_y(up) @ M
    return merge(dst, src, M, pos)


def scatter_ring(dst, src, n, R, y=0.0, up_out=True, tilt=0.0, phase=0.0, scale=1.0, spin=0.0, axis="y", center=(0, 0, 0), alt=None):
    """n copies round a circle of radius R in the plane perpendicular to `axis` ('y': the xz plane); each copy's +y points outward (up_out) or along the axis.
    alt = (scale2, tilt2): every second copy uses these instead."""
    c = v3(center)
    for i in range(n):
        th = phase + 2 * math.pi * i / n
        sc, tl = scale, tilt
        if alt and i % 2:
            sc, tl = alt
        if axis == "y":
            radial = v3((math.cos(th), 0, math.sin(th)))
            up_axis = v3((0, 1, 0))
            pos = c + radial * R + up_axis * y
        elif axis == "z":
            radial = v3((math.cos(th), math.sin(th), 0))
            up_axis = v3((0, 0, 1))
            pos = c + radial * R + up_axis * y
        else:
            radial = v3((0, math.cos(th), math.sin(th)))
            up_axis = v3((1, 0, 0))
            pos = c + radial * R + up_axis * y
        d = radial * math.cos(math.radians(tl)) + up_axis * math.sin(math.radians(tl)) if up_out else up_axis
        place(dst, src, pos, up=d, spin=spin + math.degrees(th), scale=sc)
    return dst


def ring_part(dst, c, R, r, normal=(0, 1, 0), n=24, m=6, squash=(1.0, 1.0)):
    """A torus with centre c, major radius R, minor r, in the plane perpendicular to `normal`, merged into dst."""
    t = prim_torus(R, r, n=n, m=m)
    if squash != (1.0, 1.0):
        for i, v in enumerate(t.V):
            t.V[i] = v3((v[0] * squash[0], v[1], v[2] * squash[1]))
    M = align_y(normal)
    return merge(dst, t, M, c)


def chain(dst, path, link_r=0.9, wire=0.28, every=1.7, n=8, m=5):
    """A chain of links along a polyline: alternate links turned 90 degrees about the path direction; one link primitive, many copies."""
    link = prim_link(link_r, wire, n=n, m=m)
    pts = [v3(p) for p in path]
    seg = [float(np.linalg.norm(pts[i + 1] - pts[i])) for i in range(len(pts) - 1)]
    total = sum(seg)
    cnt = max(2, int(total / (every * link_r)))
    for k in range(cnt):
        s = total * (k + 0.5) / cnt
        acc = 0.0
        for i, L in enumerate(seg):
            if s <= acc + L or i == len(seg) - 1:
                t = (s - acc) / max(L, 1e-9)
                p = pts[i] + (pts[i + 1] - pts[i]) * t
                d = unit(pts[i + 1] - pts[i])
                break
            acc += L
        # the link is long along its local y, its axis along z: put local y on the path direction, spin every second one a quarter turn about it
        M = align_y(d)
        if k % 2:
            M = M @ rot_matrix("y", 90)
        merge(dst, link, M, p)
    return dst


def arc_pts(c, R, a0, a1, n, plane="xz", y=0.0):
    out = []
    for i in range(n + 1):
        a = math.radians(a0 + (a1 - a0) * i / n)
        if plane == "xz":
            out.append((c[0] + R * math.cos(a), c[1] + y, c[2] + R * math.sin(a)))
        elif plane == "xy":
            out.append((c[0] + R * math.cos(a), c[1] + R * math.sin(a), c[2] + y))
        else:
            out.append((c[0] + y, c[1] + R * math.cos(a), c[2] + R * math.sin(a)))
    return out


def helix_pts(c, R, h, turns, n, phase=0.0, taper=0.0):
    out = []
    for i in range(n + 1):
        t = i / n
        a = phase + turns * 2 * math.pi * t
        r = R * (1 - taper * t)
        out.append((c[0] + r * math.cos(a), c[1] + h * (t - 0.5), c[2] + r * math.sin(a)))
    return out


def scroll_pts(p0, p1, amp, waves, n, up=(0, 0, 1)):
    """A sine scroll between two points (filigree wire), bulging along `up`."""
    p0, p1 = v3(p0), v3(p1)
    d = p1 - p0
    side = unit(np.cross(d, v3(up)))
    out = []
    for i in range(n + 1):
        t = i / n
        out.append(tuple(p0 + d * t + side * amp * math.sin(t * waves * math.pi) * (1 - 0.3 * abs(2 * t - 1))))
    return out


def tube_path(dst, path, r, sides=5, taper=(1.0, 1.0), caps=(True, True)):
    """A tube along a polyline merged into dst (tapering from taper[0] to taper[1] times r)."""
    n = len(path)
    sc = [((1 - i / (n - 1)) * taper[0] + (i / (n - 1)) * taper[1],) * 2 for i in range(n)]
    sweep(dst, path, circle(r, sides), scales=sc, caps=caps)
    return dst


def facet_ball(dst, c, r, lat=4, lon=6, squash=1.0):
    """A low-poly faceted ball merged into dst."""
    return merge(dst, prim_sphere(r, lat, lon, squash), None, c)


def bbox_of(parts):
    lo = np.min([p.bbox()[0] for p in parts if p.V], axis=0)
    hi = np.max([p.bbox()[1] for p in parts if p.V], axis=0)
    return lo, hi

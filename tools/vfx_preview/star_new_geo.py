"""
STAR MAGIC remake (phase 1), PREVIEW ONLY: the small 3D geometry kit of direction C ("real 3D toon meshes") and its per-quad toon shader.
Everything is built in code (no mesh files): a 3D four-point star, a thin n-gon tube, a flat annulus, a lance (hex shaft, pointed head, fins), a faceted glass panel.
A mesh quad = (4 world points, a material name).  Triangles are quads with a repeated vertex (counted as 4 vertices, like the sdw3 loader would).
The shader decides ONE colour per quad (3 flat tones from a fixed key light, an emissive core where the quad faces the camera, a pale rim where it grazes), exactly like the
fire / water C direction; no texture, no per-vertex light.
"""
import math

import numpy as np

UP = np.array([0.0, 1.0, 0.0])


def V(*a):
    return np.array(a, float)


def norm(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def frame(axis):
    """two unit vectors perpendicular to `axis`"""
    a = norm(np.asarray(axis, float))
    h = V(0, 1, 0) if abs(a[1]) < 0.92 else V(1, 0, 0)
    u = norm(np.cross(a, h))
    v = np.cross(a, u)
    return u, v


MATS = {                       # base colour 0..1
    "gold": np.array([1.00, 0.82, 0.38]),
    "white": np.array([1.00, 0.97, 0.88]),
    "blue": np.array([0.52, 0.76, 1.00]),
    "deep": np.array([0.30, 0.46, 0.95]),
}
WHITE = np.array([1.0, 0.99, 0.94])
RIM = np.array([0.80, 0.92, 1.00])
SHADE = np.array([0.30, 0.34, 0.72])


def shade(o, pts, mat, outward=None, alpha=1.0, emissive=False):
    """append one toon-coloured quad to o.m (camera-relative points)"""
    pts = np.asarray(pts, float)
    c = pts.mean(axis=0)
    n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
    if np.linalg.norm(n) < 1e-9:
        n = np.cross(pts[2] - pts[0], pts[3] - pts[0])
    n = norm(n)
    base = MATS[mat]
    vdir = norm(o.cam - c)
    d = float(np.dot(n, vdir))
    f = abs(d)
    nf = n if d >= 0 else -n
    lam = float(np.dot(nf, o.light))
    if emissive:
        col = base
    elif lam > 0.30:
        col = base * 0.45 + WHITE * 0.55
    elif lam > -0.15:
        col = base
    else:
        col = base * 0.55 + SHADE * 0.45
    if not emissive:
        if f > 0.90 and lam > -0.15:
            col = col * 0.35 + WHITE * 0.65
        elif f < 0.28:
            col = col * 0.25 + RIM * 0.75
    o.m.append({"p": pts - o.cam, "rgb": np.clip(col, 0, 1), "a": alpha, "d": float(np.linalg.norm(c - o.cam))})


def quads_star(c, ex, ey, ez, L, w, t, spin=0.0):
    """a 3D four-point star: 8 ring points (outer at L, inner at w, alternating 45 degrees) and two apexes at +-t on `ez`: 16 triangles"""
    ring = []
    for i in range(8):
        a = spin + i * math.pi / 4
        r = L if i % 2 == 0 else w
        ring.append(c + (ex * math.cos(a) + ey * math.sin(a)) * r)
    top, bot = c + ez * t, c - ez * t
    out = []
    for i in range(8):
        a, b = ring[i], ring[(i + 1) % 8]
        out.append(([a, b, top, top], "white" if i % 2 == 0 else "gold"))
        out.append(([b, a, bot, bot], "gold" if i % 2 == 0 else "white"))
    return out


def quads_tube(a, b, r0, r1, sides=4, mat="blue", rot=0.0, cap=False):
    a, b = np.asarray(a, float), np.asarray(b, float)
    u, v = frame(b - a)
    out = []
    ra = [a + (u * math.cos(rot + k * 2 * math.pi / sides) + v * math.sin(rot + k * 2 * math.pi / sides)) * r0 for k in range(sides)]
    rb = [b + (u * math.cos(rot + k * 2 * math.pi / sides) + v * math.sin(rot + k * 2 * math.pi / sides)) * r1 for k in range(sides)]
    for k in range(sides):
        k2 = (k + 1) % sides
        out.append(([ra[k], ra[k2], rb[k2], rb[k]], mat))
    if cap:
        for k in range(sides):
            k2 = (k + 1) % sides
            out.append(([rb[k], rb[k2], b, b], mat))
    return out


def quads_annulus(c, normal, r_in, r_out, segs=16, mat="gold", phase=0.0):
    u, v = frame(normal)
    out = []
    for k in range(segs):
        a0, a1 = phase + k * 2 * math.pi / segs, phase + (k + 1) * 2 * math.pi / segs
        d0, d1 = u * math.cos(a0) + v * math.sin(a0), u * math.cos(a1) + v * math.sin(a1)
        out.append(([c + d0 * r_in, c + d0 * r_out, c + d1 * r_out, c + d1 * r_in], mat))
    return out


def quads_bipyramid(c, axis, r, h, sides=4, mat="gold", rot=0.0):
    """small diamond bead: `sides` triangles up and down"""
    ax = norm(axis)
    u, v = frame(ax)
    ring = [c + (u * math.cos(rot + k * 2 * math.pi / sides) + v * math.sin(rot + k * 2 * math.pi / sides)) * r for k in range(sides)]
    out = []
    for k in range(sides):
        k2 = (k + 1) % sides
        out.append(([ring[k], ring[k2], c + ax * h, c + ax * h], mat))
        out.append(([ring[k2], ring[k], c - ax * h, c - ax * h], mat))
    return out


def quads_lance(tail, head, r, head_len, fin=0.0, mat_shaft="blue", mat_head="white"):
    """a hex shaft from `tail` to the start of the head, a pointed hex head to `head`, optional three fins at the tail: the spear of light of Quartile Hasta"""
    tail, head = np.asarray(tail, float), np.asarray(head, float)
    ax = norm(head - tail)
    neck = head - ax * head_len
    out = quads_tube(tail, neck, r * 0.8, r, sides=6, mat=mat_shaft)
    u, v = frame(ax)
    ring = [neck + (u * math.cos(k * math.pi / 3) + v * math.sin(k * math.pi / 3)) * r * 1.35 for k in range(6)]
    ring0 = [neck + (u * math.cos(k * math.pi / 3) + v * math.sin(k * math.pi / 3)) * r for k in range(6)]
    for k in range(6):
        k2 = (k + 1) % 6
        out.append(([ring0[k], ring0[k2], ring[k2], ring[k]], mat_head))
        out.append(([ring[k], ring[k2], head, head], mat_head))
    if fin > 0:
        for k in range(3):
            a = k * 2 * math.pi / 3 + math.pi / 2
            d = u * math.cos(a) + v * math.sin(a)
            p0, p1 = tail + ax * 0.0, tail + ax * (head_len * 0.9)
            out.append(([p0 + d * r * 0.4, p1 + d * r * 0.8, p1 + d * (r * 0.8 + fin), p0 + d * (r * 0.8 + fin * 1.4)], "gold"))
    return out


def quads_panel(c, ax, ay, hw, hh, nx=3, ny=2, mat="blue"):
    """a faceted pane: nx * ny quads, the facets are the tiles of the glass"""
    out = []
    for i in range(nx):
        for j in range(ny):
            x0, x1 = -hw + 2 * hw * i / nx, -hw + 2 * hw * (i + 1) / nx
            y0, y1 = -hh + 2 * hh * j / ny, -hh + 2 * hh * (j + 1) / ny
            out.append(([c + ax * x0 + ay * y0, c + ax * x1 + ay * y0, c + ax * x1 + ay * y1, c + ax * x0 + ay * y1], mat))
    return out

"""Small geometry helpers for tools/gen_shishou_assets.py (numpy and PIL only): contours of a mask, polygon simplification, ear clipping, and the extrusion of a flat polygon into a mesh_lib Part.

Everything here is pure and deterministic. Coordinates of a polygon are (x, y) pairs; the extrusion puts the polygon in the plane z = const (front +z), counter-clockwise seen from +z, which is how mesh_lib wants
its faces (counter-clockwise seen from outside).
"""
import math

import numpy as np
from PIL import Image, ImageFilter


# ================================================================ masks
def erode(mask, px):
    """The mask shrunk by about px pixels (a square structuring element)."""
    if px <= 0:
        return mask.copy()
    im = Image.fromarray((mask * 255).astype(np.uint8), "L").filter(ImageFilter.MinFilter(2 * int(px) + 1))
    return np.asarray(im) > 127


def dilate(mask, px):
    if px <= 0:
        return mask.copy()
    im = Image.fromarray((mask * 255).astype(np.uint8), "L").filter(ImageFilter.MaxFilter(2 * int(px) + 1))
    return np.asarray(im) > 127


def close(mask, px):
    return erode(dilate(mask, px), px)


def open_(mask, px):
    return dilate(erode(mask, px), px)


def fill_holes(mask):
    """Fills every hole of the mask (flood fill of the outside from the border)."""
    h, w = mask.shape
    pad = np.pad(~mask, 1, constant_values=True)
    seen = np.zeros(pad.shape, bool)
    stack = [(0, 0)]
    seen[0, 0] = True
    while stack:
        y, x = stack.pop()
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            ny, nx = y + dy, x + dx
            if 0 <= ny < pad.shape[0] and 0 <= nx < pad.shape[1] and pad[ny, nx] and not seen[ny, nx]:
                seen[ny, nx] = True
                stack.append((ny, nx))
    return ~seen[1:-1, 1:-1]


def largest_component(mask):
    """Keeps only the biggest 4-connected component."""
    h, w = mask.shape
    lab = np.zeros(mask.shape, np.int32)
    best, best_n, cur = 0, 0, 0
    for y0 in range(h):
        for x0 in range(w):
            if mask[y0, x0] and lab[y0, x0] == 0:
                cur += 1
                n = 0
                stack = [(y0, x0)]
                lab[y0, x0] = cur
                while stack:
                    y, x = stack.pop()
                    n += 1
                    for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        ny, nx = y + dy, x + dx
                        if 0 <= ny < h and 0 <= nx < w and mask[ny, nx] and lab[ny, nx] == 0:
                            lab[ny, nx] = cur
                            stack.append((ny, nx))
                if n > best_n:
                    best, best_n = cur, n
    return lab == best


# ================================================================ contours
def trace_loops(mask):
    """The boundary loops of a boolean mask as lists of corner points (x, y) in pixel-corner coordinates (y down), outer loops and holes alike."""
    h, w = mask.shape
    pad = np.pad(mask, 1)
    edges = {}
    ys, xs = np.nonzero(mask)
    for y, x in zip(ys, xs):
        py, px = y + 1, x + 1
        if not pad[py - 1, px]:
            edges.setdefault((x, y), []).append((x + 1, y))
        if not pad[py, px + 1]:
            edges.setdefault((x + 1, y), []).append((x + 1, y + 1))
        if not pad[py + 1, px]:
            edges.setdefault((x + 1, y + 1), []).append((x, y + 1))
        if not pad[py, px - 1]:
            edges.setdefault((x, y + 1), []).append((x, y))
    loops = []
    while edges:
        start = next(iter(edges))
        loop = [start]
        cur = start
        prev_dir = None
        while True:
            outs = edges.get(cur)
            if not outs:
                break
            if len(outs) > 1 and prev_dir is not None:
                def turn(n):
                    d = (n[0] - cur[0], n[1] - cur[1])
                    return prev_dir[0] * d[1] - prev_dir[1] * d[0]
                outs.sort(key=turn, reverse=True)
            nxt = outs.pop(0)
            if not outs:
                del edges[cur]
            prev_dir = (nxt[0] - cur[0], nxt[1] - cur[1])
            cur = nxt
            if cur == start:
                break
            loop.append(cur)
        if len(loop) >= 4:
            loops.append(loop)
    return loops


def area(poly):
    a = 0.0
    n = len(poly)
    for i in range(n):
        x0, y0 = poly[i]
        x1, y1 = poly[(i + 1) % n]
        a += x0 * y1 - x1 * y0
    return a / 2.0


def simplify(poly, eps):
    """Douglas-Peucker on a closed polygon."""
    pts = list(poly)
    if len(pts) < 8:
        return pts
    far = max(range(len(pts)), key=lambda i: (pts[i][0] - pts[0][0]) ** 2 + (pts[i][1] - pts[0][1]) ** 2)

    def dp(seq):
        if len(seq) < 3:
            return seq
        a, b = np.array(seq[0], float), np.array(seq[-1], float)
        ab = b - a
        L = np.linalg.norm(ab)
        best, bi = -1.0, 0
        for i in range(1, len(seq) - 1):
            p = np.array(seq[i], float)
            d = abs(ab[0] * (p[1] - a[1]) - ab[1] * (p[0] - a[0])) / L if L > 1e-9 else np.linalg.norm(p - a)
            if d > best:
                best, bi = d, i
        if best > eps:
            return dp(seq[:bi + 1])[:-1] + dp(seq[bi:])
        return [seq[0], seq[-1]]
    first = dp(pts[:far + 1])
    second = dp(pts[far:] + [pts[0]])
    return first[:-1] + second[:-1]


def outer_polygon(mask, eps=0.9):
    """The outline of the largest blob of a mask, simplified; pixel-corner coordinates (x, y down)."""
    loops = trace_loops(mask)
    if not loops:
        return []
    loop = max(loops, key=lambda l: abs(area(l)))
    return simplify(loop, eps)


# ================================================================ triangulation and extrusion
def ear_clip(poly):
    """Triangles (index triples) of a simple polygon (any orientation) by ear clipping."""
    n = len(poly)
    if n < 3:
        return []
    idx = list(range(n))
    if area(poly) < 0:
        idx.reverse()
    tris = []

    def cross(o, a, b):
        return (poly[a][0] - poly[o][0]) * (poly[b][1] - poly[o][1]) - (poly[a][1] - poly[o][1]) * (poly[b][0] - poly[o][0])

    def inside(p, a, b, c):
        def side(p1, p2, p3):
            return (poly[p1][0] - poly[p3][0]) * (poly[p2][1] - poly[p3][1]) - (poly[p2][0] - poly[p3][0]) * (poly[p1][1] - poly[p3][1])
        d1, d2, d3 = side(p, a, b), side(p, b, c), side(p, c, a)
        neg = d1 < -1e-12 or d2 < -1e-12 or d3 < -1e-12
        pos = d1 > 1e-12 or d2 > 1e-12 or d3 > 1e-12
        return not (neg and pos)

    guard = 0
    while len(idx) > 3 and guard < 20000:
        guard += 1
        ear = False
        m = len(idx)
        for k in range(m):
            a, b, c = idx[(k - 1) % m], idx[k], idx[(k + 1) % m]
            if cross(a, b, c) <= 1e-12:
                continue
            if any(inside(p, a, b, c) for p in idx if p not in (a, b, c)):
                continue
            tris.append((a, b, c))
            del idx[k]
            ear = True
            break
        if not ear:
            idx.pop(0)                                                  # a degenerate sliver: drop a vertex and go on
    if len(idx) == 3:
        tris.append((idx[0], idx[1], idx[2]))
    return tris


def extrude(part, poly, z0, z1, scale=None, front=True, back=True, walls=True):
    """Extrudes the polygon (model x, y) between z0 and z1 into the part: a front face at z1 (+z), a back face at z0 and walls. Returns the polygon's triangles.
    Counter-clockwise seen from +z; faces are quads (a triangle repeats its last vertex)."""
    pts = [tuple(p) for p in poly]
    if area(pts) < 0:
        pts.reverse()
    tris = ear_clip(pts)
    vf = [part.add_vertex((x, y, z1)) for (x, y) in pts]
    vb = [part.add_vertex((x, y, z0)) for (x, y) in pts]
    if front:
        for a, b, c in tris:
            part.add_face(vf[a], vf[b], vf[c], vf[c])
    if back:
        for a, b, c in tris:
            part.add_face(vb[a], vb[c], vb[b], vb[b])
    if walls:
        n = len(pts)
        for i in range(n):
            j = (i + 1) % n
            part.add_face(vb[i], vb[j], vf[j], vf[i])
    return tris

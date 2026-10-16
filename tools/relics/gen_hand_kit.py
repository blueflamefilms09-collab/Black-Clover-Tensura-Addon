"""gen_hand_kit: the shared kit of the hand-model generators (gen_hand_sig.py, gen_hand_trinkets.py, gen_hand_consumables.py).

A hand model is a relic seen as an OBJECT: a spoon, a flask, a lyre. It is written in exactly the file format of the worn relic models (tools/relics/gen_worn_lib.py,
class WModel and write_model: quads with a colour slot, a tile of the shared atlas, flags and an animation group), into assets/nusmp/relic_hand/<name>.txt, and drawn by the
same code (WornLibDraw.drawItem), tinted by the relic's own palette. The atlas is the worn one, so nothing new is painted.

Authoring frame (as the worn models): Y UP, the FRONT of the object is -Z (the side the onlooker sees), 1 unit = 1 model pixel. The origin is anywhere near the middle of the
object; the game centres and scales the object by its bounds. Draw an upright thing upright: the top of a flask is +Y, the handle of a spoon is -Y.
Colour slots: MAIN (relic main colour), TRIM, GLOW (emissive when flagged), DARK (main darkened), ACCENT (trim and glow mixed), NONE (the tile's own colours: a duck-yellow,
an orange, a black, a gold tile exist as fixed colours).
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_worn_lib as L  # noqa: E402
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)  # noqa: E402,F401
import mesh_lib as ml  # noqa: E402

HAND_DIR = os.path.join(L.ASSETS, "relic_hand")
MODEL_CAP = 2600                 # vertices of one hand model (the same cap as a worn ensemble)


def registry():
    """(models dict, decorator) for a generator module."""
    models = {}

    def model(fn):
        models[fn.__name__] = fn
        return fn
    return models, model


def new(name):
    """A model container anchored on the body (no offset: the game fits the object by its bounds)."""
    return WModel(name, "body")


def add(m, name, part_fn, slot=MAIN, tile="metal", flags=0, group=0, uv="tile", smooth=40.0, density=4.0):
    """Builds a part with part_fn(Part) and adds it to the model; returns the part."""
    p = Part(name)
    part_fn(p)
    return m.add(p, slot, tile, flags, group, uv, density, smooth)


# ---------------------------------------------------------------- shapes
def cyl(m, name, c, r, h, slot=MAIN, tile="metal", axis="y", sides=10, flags=0, group=0, caps=(True, True), uv="tile", r2=None, squash=1.0):
    """A cylinder (or cone frustum when r2 is given: r at the start, r2 at the end) of height h centred on c along an axis."""
    p = Part(name)
    prof = [(-h / 2, r), (h / 2, r if r2 is None else r2)]
    lathe(p, axis, c, prof, sides=sides, caps=caps, squash=squash)
    return m.add(p, slot, tile, flags, group, uv)


def ball(m, name, c, r, slot=MAIN, tile="metal", lat=5, lon=8, squash=1.0, flags=0, group=0, uv="tile"):
    p = Part(name)
    L.sph(p, c, r, lat, lon, squash)
    return m.add(p, slot, tile, flags, group, uv)


def ring(m, name, c, R, r, slot=TRIM, tile="gilt", plane="xy", n=14, k=5, flags=0, group=0, a_scale=1.0, b_scale=1.0):
    """A torus: plane 'xy' is a ring facing the onlooker (axis z), 'xz' a flat ring (axis y), 'yz' a ring seen from the side."""
    p = Part(name)
    L.torus(p, c, R, r, n, k, a_scale, b_scale, plane)
    return m.add(p, slot, tile, flags, group)


def block(m, name, p0, p1, slot=MAIN, tile="metal", chamfer=0.0, flags=0, group=0, uv="tile"):
    p = Part(name)
    ml.box(p, p0, p1, chamfer)
    return m.add(p, slot, tile, flags, group, uv)


def jewel(m, name, c, r, slot=GLOW, long=1.4, sides=6, axis="y", flags=EMIT | HOT, group=0, tile="gem"):
    p = Part(name)
    ml.gem(p, c, r, long, sides, axis)
    return m.add(p, slot, tile, flags, group, "fit")


def octa(m, name, c, rx, ry, rz, slot=GLOW, flags=EMIT | HOT, group=0, tile="gem"):
    p = Part(name)
    L.dia(p, c, rx, ry, rz)
    return m.add(p, slot, tile, flags, group, "fit")


def fit_scales(scales, n):
    """A per-point scale list stretched or squeezed (linearly) to n points, so a profile written for 8 points also fits a path of 9; None is all ones."""
    if scales is None:
        return [(1.0, 1.0)] * n
    if len(scales) == n:
        return list(scales)
    out = []
    for i in range(n):
        t = i / max(1, n - 1) * (len(scales) - 1)
        k = min(int(t), len(scales) - 2) if len(scales) > 1 else 0
        f = t - k
        a, b = scales[k], scales[min(k + 1, len(scales) - 1)]
        out.append((a[0] + (b[0] - a[0]) * f, a[1] + (b[1] - a[1]) * f))
    return out


def rod(m, name, pts, r, slot=TRIM, tile="metal", sides=6, flags=0, group=0, scales=None, caps=(True, True)):
    """A round bar along a polyline."""
    p = Part(name)
    sc = fit_scales(scales, len(pts))
    sweep(p, pts, circle(r, sides), scales=sc, caps=caps)
    return m.add(p, slot, tile, flags, group)


def spike(m, name, base, tip, r, slot=TRIM, tile="metal", sides=5, mid=0.5, mid_r=0.7, flags=0, group=0):
    p = Part(name)
    L.cone(p, base, tip, r, sides, mid, mid_r)
    return m.add(p, slot, tile, flags, group)


def disc(m, name, c, r, t, slot=MAIN, tile="metal", axis="z", sides=14, flags=0, group=0, bevel=0.2, uv="tile"):
    """A flat round plate of thickness t, its face looking along the axis, with a small bevel."""
    p = Part(name)
    b = min(bevel, r * 0.4, t * 0.45)
    lathe(p, axis, c, [(-t / 2, r - b), (-t / 2 + b * 0.4, r), (t / 2 - b * 0.4, r), (t / 2, r - b)], sides=sides, caps=(True, True))
    return m.add(p, slot, tile, flags, group, uv)


def bezier(*pts, n=7):
    """A quadratic (3 points) or cubic (4 points) Bezier sampled at n+1 points; a trailing integer is n (K.bezier(a, b, c, d, 8))."""
    if isinstance(pts[-1], int):
        pts, n = pts[:-1], pts[-1]
    if len(pts) == 3:
        return L.bez(pts[0], pts[1], pts[2], n)
    return L.bez3(pts[0], pts[1], pts[2], pts[3], n)


def helix(c, r, y0, y1, turns, n=24, phase=0.0, r_end=None):
    out = []
    for i in range(n + 1):
        t = i / n
        a = phase + 2 * math.pi * turns * t
        rr = r if r_end is None else r + (r_end - r) * t
        out.append((c[0] + rr * math.cos(a), y0 + (y1 - y0) * t, c[2] + rr * math.sin(a)))
    return out


def turn(part, axis, deg, about=(0.0, 0.0, 0.0)):
    """Rotates the vertices of a part about an axis through a point (a ring built round y turned to face the front, a limb tilted)."""
    a = math.radians(deg)
    c, s_ = math.cos(a), math.sin(a)
    o = np.array(about, float)
    R = {"x": np.array([[1, 0, 0], [0, c, -s_], [0, s_, c]]), "y": np.array([[c, 0, s_], [0, 1, 0], [-s_, 0, c]]), "z": np.array([[c, -s_, 0], [s_, c, 0], [0, 0, 1]])}[axis]
    part.V = [o + R @ (np.asarray(v, float) - o) for v in part.V]
    part.UV = part.N = None
    return part


def band_z(m, name, c, a, y_t, width, slot=MAIN, tile="gilt", n=16, flags=0, group=0, p=2.0):
    """A finger-ring band facing the front: a ring of outer radius a, wall y_t thick, width wide along z, centred on c (its hole looks along z)."""
    part = Part(name)
    L.band(part, -width / 2, width / 2, a, a, y_t, n, 0.0, 0.0, p)
    turn(part, "x", 90)
    part.V = [np.asarray(v, float) + np.asarray(c, float) for v in part.V]
    return m.add(part, slot, tile, flags, group)


# ---------------------------------------------------------------- writing
def write_all(models, assign, only=None, quiet=True):
    """Writes every model of `models` (dict name -> builder) into the hand folder; returns {name: vertex count}."""
    out = {}
    for name, fn in models.items():
        if only and name not in only:
            continue
        m = fn()
        out[name] = L.write_model(m, os.path.join(HAND_DIR, name + ".txt"))
    return out

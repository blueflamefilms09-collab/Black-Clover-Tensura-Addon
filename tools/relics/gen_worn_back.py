"""The worn BACK models: cloaks, capes, mantles, wings. Authoring frame in gen_worn_lib (body origin at the neck line, torso x -4..4, y -12..0, z -2..2, the back is +z)."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


def _yoke(m, a=5.15, b=3.15, y0=-2.2, y1=0.5, t=0.55, tile="cloth", slot=MAIN, flare=0.5, n=None):
    """The collar round the top of the torso that every cloak and cape hangs from."""
    p = Part("yoke")
    L.band(p, y0, y1, a, b, t, n or L.RN, 0.1, flare, 3.0)
    m.add(p, slot, tile, SHELL)


def _hem_trim(m, fn, nu, v0=0.93, tile="gilt", slot=TRIM, group=0, thick=0.3, name="hem"):
    """A trim strip along the bottom of a sheet fn(u, v): the last part of v, a little proud of the cloth."""
    p = Part(name)
    L.sheet(p, lambda u, v: fn(u, v0 + (1.0 - v0) * v), nu, 1, thick)
    m.add(p, slot, tile, 0, group=group)


def _clasp(m, y=-1.5, z=-2.65, r=0.62, gem=True):
    """Two short cords over the shoulders meeting at a jewelled clasp on the chest."""
    for sx in (-1, 1):
        c = Part("cord%d" % sx)
        pts = L.bez3((sx * 3.9, 0.4, -0.4), (sx * 4.5, -0.2, -2.2), (sx * 2.6, -1.1, -3.3), (sx * 0.9, -1.45, -3.4), 4)
        sweep(c, pts, circle(0.28, 4), scales=[(1.0, 1.0)] * len(pts), caps=(False, False))
        m.add(c, TRIM, "gilt")
    s = Part("clasp_set")
    L.torus(s, (0, y, z - 0.75), r * 1.15, 0.2, 6, 4, plane="xy")
    m.add(s, TRIM, "gilt")
    if gem:
        g = Part("clasp_gem")
        L.ml.gem(g, (0, y, z - 0.75), r * 0.75, 0.7, 4, "z")
        m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")


def _cloak_sheet(width_top, width_bot, y_top, y_bot, z0, flare, folds, amp, hem_wave=0.0):
    """The surface function of a hanging cloak sheet (u across, v down)."""
    def fn(u, v):
        x = (u - 0.5) * (width_top + (width_bot - width_top) * v ** 0.85)
        y = y_top + (y_bot - y_top) * v
        edge = abs(2 * u - 1)
        fold = amp * (0.25 + v) * math.sin(2 * math.pi * folds * u + 0.6) * (1 - 0.35 * edge)
        z = z0 + 0.65 + flare * v ** 1.5 + fold + 0.9 * (1 - v) ** 3 * (edge ** 2) * -1.0 + 0.5 * edge ** 2 * v
        return (x, y, z)
    return fn


@model
def cloak_long():
    """A long travelling cloak: a collar yoke with a cord clasp at the chest, a back sheet falling to the shins in soft folds, a trimmed hem, and the lower half that
    swings with the wearer's stride and waves in the wind."""
    m = WModel("cloak_long", "body")
    _yoke(m)
    _clasp(m)
    g = m.group("drape", "swing", (0, 0.2, 2.6), 14.0, 1.6, 0.0)
    fn = _cloak_sheet(9.2, 13.6, -0.2, -22.0, 2.75, 1.5, 3.0, 0.45)
    sh = Part("drape")
    L.sheet(sh, fn, 10, 7, -0.5)
    m.add(sh, MAIN, "cloth", SHELL, group=g)
    _hem_trim(m, fn, 10, group=g, thick=-0.62)
    return m


@model
def cape_short():
    """A short cape to the knees: a rounded collar yoke, a narrow back sheet with a pleat, a bold trim all round the hem and a clasp."""
    m = WModel("cape_short", "body")
    _yoke(m, tile="silk")
    _clasp(m, gem=True)
    g = m.group("drape", "swing", (0, 0.2, 2.6), 18.0, 1.4, 0.0)
    fn = _cloak_sheet(8.8, 10.8, -0.2, -15.0, 2.7, 0.9, 2.0, 0.3)
    sh = Part("drape")
    L.sheet(sh, fn, 8, 5, -0.5)
    m.add(sh, MAIN, "silk", SHELL, group=g)
    _hem_trim(m, fn, 8, v0=0.9, group=g, thick=-0.62)
    # side trims
    for sx, u in ((-1, 0.0), (1, 1.0)):
        t = Part("edge%d" % sx)
        pts = [fn(u, k / 6.0) for k in range(7)]
        pts = [(p[0] - sx * 0.2 * 0, p[1], p[2] - 0.15) for p in pts]
        sweep(t, pts, circle(0.34, 4), scales=[(1.0, 1.0)] * 7, caps=(True, True))
        m.add(t, TRIM, "gilt", 0, group=g)
    return m


@model
def mantle_shoulder():
    """A broad shoulder mantle: a fur-trimmed collar, two layered shoulder cowls over the arms and a short back drape, fastened with a chain and a stone."""
    m = WModel("mantle_shoulder", "body")
    _yoke(m, 5.3, 3.25, -2.6, 0.7, 0.7, "fur", MAIN, 0.7)
    col = Part("fur_collar")
    L.band(col, -0.4, 1.4, 5.25, 3.2, 0.85, L.RN, 0.25, 0.4, 3.0)
    m.add(col, TRIM, "fur", SHELL)
    for sx in (-1, 1):
        for k, (y, rr) in enumerate(((1.0, 4.2), (-1.4, 3.6), (-3.7, 2.9))):
            cw = Part("cowl%d_%d" % (sx, k))
            L.sph(cw, (sx * 6.0, y, 0.0), rr * 0.55, 3, 8, squash=0.55)
            m.add(cw, MAIN if k != 1 else TRIM, "cloth" if k else "plate", SHELL)
        spike = Part("pauldron_spike%d" % sx)
        L.cone(spike, (sx * 6.4, 2.0, 0.0), (sx * 7.4, 5.6, 0.2), 0.8, 5, mid=0.4, mid_r=0.8)
        m.add(spike, TRIM, "gilt")
    g = m.group("drape", "swing", (0, 0.2, 2.6), 10.0, 1.0, 0.0)
    fn = _cloak_sheet(9.0, 10.2, -0.5, -9.5, 2.75, 0.5, 2.0, 0.25)
    sh = Part("drape")
    L.sheet(sh, fn, 8, 4, -0.5)
    m.add(sh, MAIN, "fur", SHELL, group=g)
    _clasp(m, y=-2.0)
    return m


@model
def wings_feathered():
    """A pair of feathered wings folded on the back: three ranks of long feathers fanning out and down from a wing bone, white-gold, gently flapping at rest."""
    m = WModel("wings_feathered", "body")
    for sx in (-1, 1):
        g = m.group("wing%d" % sx, "sway", (sx * 2.4, -1.0, 2.8), 4.0 * sx, 0.07, 0.0)
        bone = Part("bone%d" % sx)
        pts = L.bez3((sx * 2.2, -1.0, 2.8), (sx * 5.2, 3.0, 3.6), (sx * 9.0, 4.8, 4.2), (sx * 13.0, 3.0, 4.5), 7)
        sweep(bone, pts, circle(0.55, 5), scales=[(1.1, 1.1)] + [(1.0, 1.0)] * 5 + [(0.5, 0.5), (0.2, 0.2)][:1] + [(0.0, 0.0)], caps=(True, True))
        m.add(bone, TRIM, "bone", SHELL, group=g)
        ranks = ((0.0, 15.0, 0.9), (0.38, 12.0, 1.0), (0.7, 9.0, 1.1))
        for r_i, (lift, ln, w) in enumerate(ranks):
            for k in range(6):
                t = 0.1 + k * 0.16
                bx = sx * (2.4 + t * 10.4)
                by = 3.4 - 2.4 * t ** 1.4 - lift
                bz = 3.6 + 0.5 * r_i
                ang = math.radians(-8 - 12 * (1 - t) * 0 - 22 * t)
                tipx = bx + sx * ln * 0.2 * (1 - t) * 0.6 + sx * 3.0 * t
                tipy = by - ln * (0.85 - 0.25 * t)
                f = Part("feather%d_%d_%d" % (sx, r_i, k))
                L.feather(f, (bx, by, bz), (tipx, tipy, bz + 1.0 + 0.5 * r_i), w * (0.9 - 0.2 * r_i * 0.3), 0.3, 4, curl=0.0 * sx)
                m.add(f, MAIN if r_i != 1 else ACCENT, "feather", SHELL, group=g, uv="fit")
    return m


@model
def wings_moth():
    """Moth wings: two big dusty fore-wings and two small hind-wings on delicate veins, with eye-spots, a soft glow along the edge, trembling at rest."""
    m = WModel("wings_moth", "body")
    for sx in (-1, 1):
        g = m.group("wing%d" % sx, "sway", (sx * 1.8, -1.0, 2.8), 5.0 * sx, 0.14, 0.0)
        fore = Part("fore%d" % sx)

        def ffn(u, v, sx=sx):
            # u along the leading edge from the body outward, v from leading edge to trailing edge
            span = 17.0 * u
            lead = 3.0 + 2.2 * math.sin(u * 3.0) - 1.2 * u
            trail = lead - 15.0 * (1 - 0.3 * u) * v
            y = lead + (trail - lead) * (v if True else 0)
            x = sx * (1.8 + span * (1.0 - 0.25 * v))
            return (x, y - 0.4 * v * v * 0.0, 3.0 + 0.9 * u + 0.3 * v)
        L.sheet(fore, ffn, 6, 4, 0.2, True)
        m.add(fore, MAIN, "moth", SHELL | 0, group=g, uv="fit")
        hind = Part("hind%d" % sx)

        def hfn(u, v, sx=sx):
            span = 11.0 * u
            x = sx * (1.6 + span * (1.0 - 0.1 * v))
            y = -3.5 - 9.0 * v * (1 - 0.3 * u)
            return (x, y + 1.2 * math.sin(u * 3.1), 3.0 + 0.5 * u)
        L.sheet(hind, hfn, 4, 3, 0.2, True)
        m.add(hind, ACCENT, "moth", SHELL, group=g, uv="fit")
        edge = Part("glow_edge%d" % sx)
        pts = [ffn(k / 8.0, 0.0) for k in range(9)]
        pts = [(p[0], p[1] + 0.1, p[2] + 0.5) for p in pts]
        sweep(edge, pts, circle(0.28, 4), scales=[(1.0, 1.0)] * 9, caps=(True, True))
        m.add(edge, GLOW, "soft", EMIT, group=g, uv="fit")
        for k in range(2):
            es = Part("eyespot%d_%d" % (sx, k))
            L.dia(es, (sx * (9.0 + 3.5 * k), -1.0 - 4.2 * k, 3.9 + 0.3 * k), 1.1, 1.1, 0.12)
            m.add(es, GLOW, "gem", EMIT | HOT, group=g, uv="fit")
    return m


@model
def cloak_hood_down():
    """A cloak with its hood thrown back: the long cloak's body plus a bunched hood at the nape with a pointed tip."""
    m = WModel("cloak_hood_down", "body")
    _yoke(m, tile="cloth")
    _clasp(m)
    g = m.group("drape", "swing", (0, 0.2, 2.6), 14.0, 1.4, 0.0)
    fn = _cloak_sheet(9.4, 13.0, -0.2, -21.0, 2.75, 1.3, 3.5, 0.5)
    sh = Part("drape")
    L.sheet(sh, fn, 10, 7, -0.5)
    m.add(sh, MAIN, "cloth", SHELL, group=g)
    _hem_trim(m, fn, 10, group=g, thick=-0.62)
    hd = Part("hood")
    L.sph(hd, (0, 1.0, 3.0), 3.0, 3, 8, squash=0.9)
    m.add(hd, MAIN, "cloth", SHELL)
    tip = Part("hood_tip")
    L.cone(tip, (0, 1.0, 4.0), (0, -3.6, 6.2), 2.2, 5, mid=0.4, mid_r=0.8)
    m.add(tip, MAIN, "cloth", SHELL, group=g)
    return m

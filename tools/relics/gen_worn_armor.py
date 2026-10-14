"""The worn models of the mod's ARMOUR items (the robe sets, the Black Clover mage gear, the generic fallback pieces) and the signature chest and boot models of the
armour relics that only had the model of their look.

Same method and the same file format as the other gen_worn_*.py files (docs/relic_visuals.md, docs/armor_build.md): lofted, smooth-shaded, named parts, a colour slot and a
material tile per part, an emissive glow channel; authored y up, front is -z, relative to the pivot of the body part the model is attached to. A garment is drawn by the
game ON the player in place of the painted armour box, so its torso, sleeves, legs and boots are sized to sit one pixel outside the body (the layer above the skin).

    body  origin at the neck line, torso x -4..4, y -12..0, z -2..2         (garment: x +-4.7, z +-2.7)
    head  origin at the neck, head box x -4..4, y 0..8, z -4..4
    rarm  origin at the shoulder pivot, arm x -3..1, y -10..2, z -2..2       (the left arm is the mirror, `~` in the catalog)
    rleg  origin at the hip pivot, leg x -2..2, y -12..0                     (both legs wear the same model)
"""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


def _rr(w, d, r=1.6, n=2):
    """The torso / limb section: a rounded rectangle (12 points for n = 2), counter-clockwise."""
    return L.ml.rounded_rect(w, d, r, n)


# ================================================================ emblems (an extruded outline on a plate: the set's badge)
def _heart_outline(n=14):
    pts = []
    for i in range(n):
        t = 2 * math.pi * i / n
        x = 16 * math.sin(t) ** 3
        y = 13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t)
        pts.append((x / 17.0, y / 17.0 + 0.04))
    return pts[::-1] if L.ml._area2(pts) < 0 else pts


def _spade_outline():
    h = _heart_outline(14)
    pts = [(x, -y * 0.9 + 0.12) for (x, y) in h]                              # the heart upside down is the blade of the spade
    pts = pts[::-1] if L.ml._area2(pts) < 0 else pts
    # a stem under it: replace the points nearest the bottom with a stem
    out = []
    for (x, y) in pts:
        out.append((x, y))
    out.insert(0, (0.0, 0.0))
    return [(x, y) for (x, y) in L.ml.resample(_hull_ccw(out), 14)]


def _hull_ccw(pts):
    c = np.mean(np.array(pts), axis=0)
    return sorted(pts, key=lambda p: math.atan2(p[1] - c[1], p[0] - c[0]))


def _star_outline(points, r0, r1, phase=math.pi / 2):
    out = []
    for i in range(points * 2):
        a = phase + math.pi * i / points
        r = r1 if i % 2 == 0 else r0
        out.append((r * math.cos(a), r * math.sin(a)))
    return out


def _diamond_outline():
    return [(0.0, 1.0), (-0.78, 0.2), (0.0, -1.0), (0.78, 0.2)][::-1]


def _clover_outline():
    out = []
    for i in range(16):
        a = 2 * math.pi * i / 16
        r = 0.55 + 0.45 * abs(math.cos(2 * a))
        out.append((r * math.cos(a + math.pi / 4), r * math.sin(a + math.pi / 4)))
    return out


def _bull_outline():
    """A bull's head seen from the front: a broad brow with two horns sweeping up and out and a muzzle."""
    return [(0.0, -1.0), (0.4, -0.7), (0.45, -0.3), (0.95, 0.0), (1.1, 0.3), (1.05, 0.65), (0.82, 0.42), (0.48, 0.30),
            (0.0, 0.34), (-0.48, 0.30), (-0.82, 0.42), (-1.05, 0.65), (-1.1, 0.3), (-0.95, 0.0), (-0.45, -0.3), (-0.4, -0.7)][::-1]


def _eagle_outline():
    """Spread wings: a body point at the middle, two wings with three feather tips each."""
    return [(0.0, 0.55), (0.45, 0.5), (1.1, 0.46), (0.8, 0.22), (1.12, 0.0), (0.6, -0.12), (0.2, -0.6), (0.0, -0.9),
            (-0.2, -0.6), (-0.6, -0.12), (-1.12, 0.0), (-0.8, 0.22), (-1.1, 0.46), (-0.45, 0.5)][::-1]


def _lion_outline():
    """A mane: a ring of pointed tufts."""
    return _star_outline(8, 0.78, 1.12)


def _peacock_outline():
    """A fan of five feathers rising from a small base."""
    out = [(-0.2, -0.9), (0.2, -0.9)]
    for k in range(5):
        a = math.radians(-50 + k * 25)
        out.append((0.95 * math.sin(a) + 0.12, 0.95 * math.cos(a) - 0.2))
    return _hull_ccw(out)


def _sun_outline():
    return _star_outline(8, 0.62, 1.1)


def _devil_outline():
    """A horned sigil: an inverted triangle with two horns."""
    return [(0.0, -1.0), (0.55, -0.1), (0.95, 0.9), (0.55, 0.5), (0.28, 0.15), (0.0, 0.25), (-0.28, 0.15), (-0.55, 0.5), (-0.95, 0.9), (-0.55, -0.1)][::-1]


OUTLINES = {"heart": _heart_outline, "spade": _spade_outline, "star": lambda: _star_outline(5, 0.45, 1.0), "diamond": _diamond_outline, "clover": _clover_outline,
            "bull": _bull_outline, "eagle": _eagle_outline, "lion": _lion_outline, "peacock": _peacock_outline, "sun": _sun_outline, "devil": _devil_outline}


def _badge(m, kind, c, size, plate=True, group=0, glow_gem=True, outward=-1.0):
    """The set's badge at c = (x, y, z): an outlined plate (TRIM, gilt) with a raised inner outline (MAIN) and a stone (GLOW). `outward` -1 faces -z (the front)."""
    pts = OUTLINES[kind]()
    pts = [(p[0] * size, p[1] * size) for p in pts]
    if L.ml._area2(pts) < 0:
        pts = pts[::-1]
    base = Part("badge_plate")
    loft(base, "z" if outward > 0 else "-z", c, [(0.0, 1.0, 1.0), (0.55, 1.0, 1.0)], pts, caps=(False, True))
    m.add(base, TRIM, "gilt", SHELL, group=group, uv="fit")
    inner = Part("badge_inner")
    pts2 = [(p[0] * 0.66, p[1] * 0.66) for p in pts]
    cc = (c[0], c[1], c[2] + (-0.45 if outward < 0 else 0.45))
    loft(inner, "z" if outward > 0 else "-z", cc, [(0.0, 1.0, 1.0), (0.4, 1.0, 1.0)], pts2, caps=(False, True))
    m.add(inner, MAIN, "plate", 0, group=group, uv="fit")
    if glow_gem:
        g = Part("badge_stone")
        L.dia(g, (c[0], c[1], c[2] + (-1.1 if outward < 0 else 1.1)), size * 0.2, size * 0.26, 0.4)
        m.add(g, GLOW, "gem", EMIT | HOT, group=group, uv="fit")


def _emblem_model(name, kind, size=2.6, anchor="body", at=(0.0, -4.6, -3.0)):
    m = WModel(name, anchor)
    _badge(m, kind, at, size)
    return m


for _kind, _size in (("heart", 2.3), ("spade", 2.4), ("star", 2.4), ("diamond", 2.4), ("clover", 2.3), ("bull", 2.5), ("eagle", 3.0), ("lion", 2.5), ("peacock", 2.6),
                     ("sun", 2.5), ("devil", 2.5)):
    def _mk(kind=_kind, size=_size):
        def f():
            return _emblem_model("emblem_" + kind, kind, size)
        f.__doc__ = "The %s badge on the chest: a plate with a raised inner outline and a lit stone." % kind
        f.__name__ = "emblem_" + kind
        return f
    MODELS["emblem_" + _kind] = _mk()


# ================================================================ chest garments (body anchor)
def _torso(m, y_top=0.5, y_bot=-12.1, flare_waist=0.96, tile="cloth", slot=MAIN, w=9.5, d=5.5, name="torso", extra_stations=None):
    """The garment's trunk: a lofted tube from the shoulders to the hem of the shirt, with a slight waist."""
    p = Part(name)
    st = [(y_top, 0.94, 0.9), (0.0, 1.0, 1.0), (-4.0, 1.02, 1.0), (-8.0, flare_waist, 0.96), (y_bot, 1.0, 0.98)]
    if extra_stations:
        st = extra_stations
    loft(p, "y", (0, 0, 0), st, _rr(w, d, 1.8, 2), caps=(False, False))
    m.add(p, slot, tile, SHELL)
    return p


def _neckline(m, tile="cloth", r=3.0, h=1.4, flare=0.5, slot=MAIN):
    """A standing collar round the neck."""
    c = Part("collar")
    L.band(c, 0.3, 0.3 + h, r + 0.3, r * 0.78 + 0.2, 0.4, 10, 0.0, flare, 3.2)
    m.add(c, slot, tile, SHELL)


def _trim_front(m, y0, y1, w=1.0, z=-2.9, tile="gilt", slot=TRIM):
    """A vertical trim strip down the front (the opening of a robe, a placket)."""
    t = Part("front_trim")
    loft(t, "y", (0, 0, z), [(y0, 1.0, 1.0), (y1, 1.0, 1.0)], octagon(w, 0.5, 0.15))
    m.add(t, slot, tile, 0)


def _hem(m, y, w, d, tile="gilt", slot=TRIM, h=0.7, group=0, name="hem"):
    t = Part(name)
    L.band(t, y - h / 2, y + h / 2, w, d, 0.3, 10, 0.0, 0.0, 3.0)
    m.add(t, slot, tile, 0, group=group)


def _sash(m, y=-9.2, w=4.95, d=2.95, tile="silk", slot=TRIM, knot=True):
    s = Part("sash")
    L.band(s, y - 1.1, y + 0.8, w, d, 0.3, 10, 0.0, 0.0, 3.2)
    m.add(s, slot, tile, SHELL)
    if knot:
        k = Part("sash_knot")
        L.sph(k, (4.2, y, -2.2), 0.95, 3, 7, squash=0.9)
        m.add(k, slot, tile, 0)
        for sx in (0, 1):
            tl = Part("sash_tail%d" % sx)
            loft(tl, "-y", (4.3 - 0.4 * sx, y - 0.6, -2.3 - 0.2 * sx), [(0.0, 0.5, 0.18), (3.5, 0.6, 0.18), (4.6, 0.1, 0.1)], octagon(1.0, 1.0, 0.25))
            m.add(tl, slot, tile, 0)


def _skirt(m, y0, y1, w0, w1, d0, d1, tile="cloth", slot=MAIN, group=0, slit=True, name="skirt", z_shift=0.0):
    """The long skirt of a robe or coat: a lofted flare from the waist to the hem; with `slit` it is split at the front (two panels) so the legs show and move."""
    gk = group
    for sx in (-1, 1):
        # front-to-back panel along the side of the body: u 0 = front centre, 1 = back centre
        def fn2(u, v, sx=sx):
            r_x = (w0 + (w1 - w0) * v)
            r_z = (d0 + (d1 - d0) * v)
            a = math.pi * u
            x = sx * r_x * math.sin(a) * (1.0 if slit else 1.0)
            z = -r_z * math.cos(a) + z_shift
            y = y0 + (y1 - y0) * v + (0.0 if slit else 0.0)
            return (x, y, z)
        p = Part("%s%d" % (name, sx))
        L.sheet(p, fn2, 5, 3, 0.0 + 0.35 * sx * -1)
        m.add(p, slot, tile, SHELL, group=gk)


@model
def chest_robe():
    """A robe over the whole body: a lofted trunk with a stand collar, a front trim, a sash with a knot and a long two-panel skirt that swings with the stride, a hem band."""
    m = WModel("chest_robe", "body")
    _torso(m)
    _neckline(m)
    _trim_front(m, 0.4, -12.0, 1.2)
    _sash(m)
    g = m.group("skirt", "swing", (0, -11.5, 0), 6.0, 0.9, 0.0)
    _skirt(m, -11.4, -22.2, 4.9, 5.7, 2.75, 3.3, group=g)
    _hem(m, -22.2, 5.75, 3.35, group=g)
    return m


def _sleeve(m, y_top=2.2, y_bot=-10.4, tile="cloth", slot=MAIN, flare=0.0, cuff=True, w=5.0):
    """A sleeve lofted round the right arm (arm box 4 x 4 about x = -1): shoulder to wrist, an optional cuff band."""
    p = Part("sleeve")
    loft(p, "y", (-1.0, 0.0, 0.0), [(y_top, 0.94, 0.94), (y_top - 1.2, 1.0, 1.0), (-4.0, 0.98, 0.98), (-8.0, 1.0 + flare * 0.6, 1.0 + flare * 0.6),
                                    (y_bot, 1.02 + flare, 1.02 + flare)], _rr(w, w, 1.5, 2), caps=(False, False))
    m.add(p, slot, tile, SHELL)
    if cuff:
        c = Part("cuff")
        L.band(c, y_bot - 0.1, y_bot + 1.3, w / 2 + 0.05 + flare * 1.2, w / 2 + 0.05 + flare * 1.2, 0.3, 12, 0.0, 0.0, 3.2)
        c.V = [v + np.array([-1.0, 0, 0]) for v in c.V]
        m.add(c, TRIM, "gilt", 0)


@model
def sleeve_plain():
    """A cloth sleeve with a trim cuff."""
    m = WModel("sleeve_plain", "rarm")
    _sleeve(m)
    return m


@model
def sleeve_wide():
    """A wide robe sleeve: tight at the shoulder, flaring to a deep cuff with a trim border, swaying with the arm."""
    m = WModel("sleeve_wide", "rarm")
    g = m.group("cuff", "sway", (-1.0, -5.0, 0.0), 4.0, 0.08, 0.0)
    p = Part("sleeve")
    loft(p, "y", (-1.0, 0.0, 0.0), [(2.2, 0.94, 0.94), (1.0, 1.0, 1.0), (-4.0, 1.0, 1.0)], _rr(5.0, 5.0, 1.5, 2), caps=(False, False))
    m.add(p, MAIN, "cloth", SHELL)
    q = Part("flare")
    loft(q, "y", (-1.0, -4.0, 0.0), [(0.0, 1.0, 1.0), (-2.4, 1.15, 1.15), (-4.8, 1.55, 1.5), (-6.6, 1.85, 1.7)], _rr(5.0, 5.0, 1.5, 2), caps=(False, False))
    m.add(q, MAIN, "silk", SHELL, group=g)
    b = Part("border")
    L.band(b, -10.9, -10.4, 4.6, 4.4, 0.3, 12, 0.0, 0.0, 3.0)
    b.V = [v + np.array([-1.0, 0, 0]) for v in b.V]
    m.add(b, TRIM, "gilt", 0, group=g)
    return m


@model
def sleeve_plated():
    """A plated sleeve: cloth to the elbow, a stepped plate vambrace over the forearm, a fitted shoulder cap with a trim edge."""
    m = WModel("sleeve_plated", "rarm")
    _sleeve(m, cuff=False)
    for k, (y, rr) in enumerate(((-4.4, 2.75), (-6.2, 2.72), (-8.0, 2.72))):
        pl = Part("vambrace%d" % k)
        loft(pl, "y", (-1.0, y, 0.0), [(0.0, 1.0, 1.0), (-1.9, 0.97, 0.97)], octagon(2 * rr, 2 * rr, rr * 0.5), caps=(False, False))
        m.add(pl, MAIN if k != 1 else TRIM, "plate" if k != 1 else "gilt", SHELL)
    cap = Part("shoulder_cap")
    L.sph(cap, (-1.0, 1.9, 0.0), 3.3, 3, 10, squash=0.55)
    m.add(cap, MAIN, "plate", SHELL)
    return m


@model
def chest_tunic():
    """A short tunic: a lofted trunk with a round collar, a belt with a buckle and a small hanging tabard flap at the front."""
    m = WModel("chest_tunic", "body")
    _torso(m, y_bot=-12.1)
    _neckline(m, h=1.0)
    b = Part("belt")
    L.band(b, -10.0, -8.7, 4.85, 2.85, 0.3, 14, 0.0, 0.0, 3.2)
    m.add(b, DARK, "leather", 0)
    bk = Part("buckle")
    loft(bk, "z", (0, -9.35, -3.1), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], octagon(1.8, 1.7, 0.4))
    m.add(bk, TRIM, "gilt", SHELL, uv="fit")
    fl = Part("tabard")
    L.sheet(fl, lambda u, v: ((u - 0.5) * 6.0, -10.0 - v * 5.8, -2.8 - 0.3 * v), 3, 3, 0.35)
    m.add(fl, TRIM, "cloth", SHELL)
    return m


@model
def chest_coat():
    """A long coat: a lofted trunk with lapels crossing the chest, a high collar, buttons, and a long skirt open at the front that flares and swings with the stride."""
    m = WModel("chest_coat", "body")
    _torso(m)
    _neckline(m, h=1.8, flare=0.8)
    for sx in (-1, 1):
        lp = Part("lapel%d" % sx)
        L.sheet(lp, lambda u, v, sx=sx: (sx * (0.4 + u * 3.8), -0.4 - v * 6.2 + u * 0.8, -2.75 - 0.15 * u), 3, 3, 0.35 * sx)
        m.add(lp, TRIM, "cloth", SHELL)
    for k in range(4):
        b = Part("button%d" % k)
        L.dia(b, (0.0, -3.0 - k * 2.2, -3.0), 0.32, 0.32, 0.25)
        m.add(b, GLOW, "gem", EMIT, uv="fit")
    g = m.group("skirt", "swing", (0, -11.5, 0), 7.0, 0.9, 0.0)
    _skirt(m, -11.4, -21.6, 4.85, 5.9, 2.7, 3.4, group=g)
    _hem(m, -21.6, 5.95, 3.45, group=g)
    return m


@model
def chest_mantle():
    """A mantle over a fitted tunic: a draped shoulder cape that hangs over the upper arms and the back, a clasp at the throat, a trim border, a long back cape that billows with the stride."""
    m = WModel("chest_mantle", "body")
    _torso(m, w=9.4)
    _neckline(m, h=1.3)
    # the mantle over the shoulders: a hood-less cape, a half dome
    sh = Part("mantle")
    def mfn(u, v):
        th = math.pi * (0.0 + u) * 2.0                       # all the way round the neck
        r = 3.4 + v * 2.7 + 0.5 * v * v
        y = 0.9 - v * 4.2 - 1.0 * v * v * (0.5 + 0.5 * math.sin(th - 1.6))
        return (r * 1.3 * math.cos(th), y, r * 0.86 * math.sin(th))
    L.sheet(sh, mfn, 14, 2, 0.4)
    m.add(sh, MAIN, "silk", SHELL)
    bd = Part("mantle_border")
    L.band(bd, -4.5, -4.1, 6.6 * 1.3 * 0.99, 6.6 * 0.86 * 0.99, 0.35, 12, 0.0, 0.0, 3.0)
    m.add(bd, TRIM, "gilt", 0)
    cl = Part("clasp")
    L.ml.gem(cl, (0, -0.6, -3.5), 0.8, 0.9, 6, "z")
    m.add(cl, GLOW, "gem", EMIT | HOT, uv="fit")
    ch = Part("chain")
    L.band(ch, -1.0, -0.6, 3.6, 3.0, 0.2, 10, 0.0, 0.0, 3.0)
    m.add(ch, TRIM, "gilt", 0)
    # the back cape
    g = m.group("cape", "swing", (0, -1.0, 2.6), 18.0, 1.3, 0.0)
    cp = Part("cape")
    def cfn(u, v):
        x = (u - 0.5) * (9.0 + 2.5 * v)
        return (x, -1.4 - v * 17.5, 2.75 + 0.7 * v + 0.9 * math.sin(u * math.pi) * v)
    L.sheet(cp, cfn, 4, 4, -0.4)
    m.add(cp, MAIN, "silk", SHELL, group=g)
    cb = Part("cape_border")
    L.sheet(cb, lambda u, v: ((u - 0.5) * 11.5, -18.8 - 0.4 * v, 3.45 + 0.0 * v + 0.9 * math.sin(u * math.pi)), 6, 1, -0.4)
    m.add(cb, TRIM, "gilt", 0, group=g)
    return m


@model
def chest_devil():
    """A bound coat of a devil's mage: a torn long coat with a jagged hem, a high collar with spikes, chains crossing the chest and a lit sigil."""
    m = WModel("chest_devil", "body")
    _torso(m)
    c = Part("collar")
    L.band(c, 0.2, 3.4, 5.0, 3.1, 0.5, 12, 0.35, 0.7, 3.2)
    m.add(c, MAIN, "cloth", SHELL)
    for k in range(5):
        a = math.pi * (0.15 + 0.7 * k / 4)
        sp = Part("spike%d" % k)
        L.cone(sp, (4.2 * math.cos(a + math.pi), 3.2, 2.6 * math.sin(a + math.pi) * 1.0), (5.0 * math.cos(a + math.pi), 6.3 + (k % 2), 3.2 * math.sin(a + math.pi)), 0.55, 4, mid=0.4, mid_r=0.9)
        m.add(sp, TRIM, "horn", SHELL)
    for sx in (-1, 1):
        s = Part("chain%d" % sx)
        pts = L.bez((sx * 4.0, 0.0, -2.95), (0.0, -4.0, -3.3), (-sx * 4.0, -12.0, -2.95), 6)
        L.beads(s, pts, 0.38, 5, every=2, bead=1.0, thin=0.7)
        m.add(s, TRIM, "chain", 0)
    sg = Part("sigil")
    L.ml.gem(sg, (0, -5.8, -3.4), 1.1, 1.1, 6, "z")
    m.add(sg, GLOW, "gem", EMIT | HOT, uv="fit")
    g = m.group("tails", "swing", (0, -11.5, 0), 7.0, 1.0, 0.0)
    for k in range(6):
        x = -3.5 + k * 1.4
        for zs in (-1, 1):
            t = Part("tail%d_%d" % (k, zs))
            hh = 7.5 + ((k * 5) % 4) * 1.6
            loft(t, "-y", (x, -11.5, zs * (2.85 + 0.25 * abs(k - 3) / 3.0)), [(0.0, 0.7, 0.18), (hh * 0.7, 0.8, 0.18), (hh, 0.0, 0.0)], rect_ring(1.0, 1.0))
            m.add(t, MAIN if (k + zs) % 2 else DARK, "cloth", SHELL, group=g)
    return m


# ================================================================ head pieces (head anchor)
def _cowl_shell(m, open_=0.66, top=9.4, drape=-2.8, r=5.2, tile="cloth", slot=MAIN, back_drop=2.0, edges=True):
    """A soft hood: straight walls from a drape over the shoulders to the crown, a rounded dome, a face opening cut in the front."""
    def prof(v):
        y = drape + v * (top - drape)
        if y < 0.4:
            rr = r + (0.4 - y) * 0.85
        elif y < top - 3.2:
            rr = r
        else:
            t = min(1.0, (y - (top - 3.2)) / 3.2)
            rr = r * math.sqrt(max(0.0, 1.0 - t * t)) + 0.05
        return rr, y

    def fn(u, v):
        th = 1.5 * math.pi + open_ + u * (2 * math.pi - 2 * open_)
        rr, y = prof(v)
        x, z = L._sq(th, rr, rr * 0.98, 3.2)
        return (x, y, z + back_drop * max(0.0, math.sin(th)) * max(0.0, (0.4 - y) / 3.2))
    sh = Part("shell")
    L.sheet(sh, fn, 16, 8, 0.5)
    m.add(sh, slot, tile, SHELL)
    if not edges:
        return prof
    # the trimmed edge of the face opening
    for sx, th in ((1, 1.5 * math.pi + open_), (-1, 1.5 * math.pi - open_)):
        pts = []
        for i in range(7):
            rr, y = prof(i / 6.0 * 0.88)
            x, z = L._sq(th, rr + 0.25, rr * 0.98 + 0.25, 3.2)
            pts.append((x, y, z))
        t = Part("edge%d" % sx)
        sweep(t, pts, circle(0.42, 4), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
        m.add(t, TRIM, "gilt")
    rr, y = prof(0.88)
    pts = []
    for i in range(5):
        th = 1.5 * math.pi - open_ + 2 * open_ * i / 4.0
        x, z = L._sq(th, rr + 0.25, rr * 0.98 + 0.25, 3.2)
        pts.append((x, y, z))
    br = Part("brow")
    sweep(br, pts, circle(0.42, 4), scales=[(1.0, 1.0)] * 5, caps=(True, True))
    m.add(br, TRIM, "gilt")
    return prof


@model
def hood_cowl():
    """A mage's cowl: a deep soft hood that drapes over the shoulders with a trimmed face opening, a clasp stone at the brow and a short fold at the back."""
    m = WModel("hood_cowl", "head")
    _cowl_shell(m)
    fold = Part("back_fold")
    L.sheet(fold, lambda u, v: ((u - 0.5) * 7.0, 1.0 - v * 3.0, 5.0 + 0.7 * v), 4, 2, -0.4)
    m.add(fold, MAIN, "cloth", SHELL)
    cl = Part("clasp")
    L.ml.gem(cl, (0, 8.6, -5.5), 0.55, 0.9, 6, "z")
    m.add(cl, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


def _horn(m, name, base, tip, r, slot=TRIM, tile="horn", mid=(0.0, 0.0, 0.0)):
    h = Part(name)
    pts = L.bez(base, (mid[0], mid[1], mid[2]), tip, 6)
    L.horn(h, pts, r, r * 0.15, 6)
    m.add(h, slot, tile, SHELL)


@model
def hood_horned():
    """A hood with a pair of short curved horns breaking out of the cloth at the brow and a gilt brow plate: the Black Bull's hood."""
    m = WModel("hood_horned", "head")
    _cowl_shell(m, top=9.0)
    for sx in (-1, 1):
        _horn(m, "horn%d" % sx, (sx * 3.6, 7.8, -2.6), (sx * 7.4, 12.8, -3.2), 1.05, mid=(sx * 6.9, 9.2, -3.2))
    bp = Part("brow_plate")
    loft(bp, "z", (0, 7.6, -5.35), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], [(-2.2, -0.9), (2.2, -0.9), (1.5, 0.9), (0.0, 1.5), (-1.5, 0.9)][::-1])
    m.add(bp, TRIM, "gilt", SHELL, uv="fit")
    g = Part("stone")
    L.dia(g, (0, 7.7, -5.95), 0.45, 0.55, 0.3)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def hat_mage():
    """A pointed mage's hat with a wide soft brim, a band with a badge stone and a point that droops back."""
    m = WModel("hat_mage", "head")
    brim = Part("brim")
    def brimfn(u, v):
        th = 2 * math.pi * u
        r = 4.9 + v * 4.6
        return (r * math.cos(th), 7.5 + 0.25 * math.sin(th * 2) * v - 0.5 * v * (1 - math.cos(th)) * 0.5 - 0.6 * v * v, r * math.sin(th))
    L.sheet(brim, brimfn, 20, 2, 0.35)
    m.add(brim, MAIN, "cloth", SHELL)
    cone = Part("cone")
    def conefn(u, v):
        th = 2 * math.pi * u
        r = 4.5 * (1 - v) ** 0.9 + 0.1
        return (r * math.cos(th), 7.7 + v * 9.0 - (v ** 5) * 2.2, r * math.sin(th) + (v ** 2.4) * 4.0)
    L.sheet(cone, conefn, 16, 6, 0.35)
    m.add(cone, MAIN, "cloth", SHELL)
    bd = Part("band")
    L.band(bd, 8.0, 9.2, 4.42, 4.42, 0.15, L.RN, 0.0, 0.0, 2.0)
    m.add(bd, TRIM, "silk")
    st = Part("stone")
    L.ml.gem(st, (0, 8.6, -4.8), 0.55, 0.9, 5, "z")
    m.add(st, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def hat_peacock():
    """A soft round hat with a turned brim and a fan of five peacock feathers rising from the band over the ear: the Coral Peacock's hat."""
    m = WModel("hat_peacock", "head")
    cap = Part("cap")
    L.sph(cap, (0, 6.0, 0), 4.9, 4, 12, squash=0.95)
    m.add(cap, MAIN, "silk", SHELL)
    br = Part("brim")
    L.band(br, 6.4, 7.1, 4.8, 4.8, 0.9, L.RN, 0.0, 0.5, 2.4)
    m.add(br, MAIN, "silk", SHELL)
    bd = Part("band")
    L.band(bd, 6.2, 7.2, 4.95, 4.95, 0.12, L.RN, 0.0, 0.0, 2.4)
    m.add(bd, TRIM, "gilt")
    g = m.group("fan", "sway", (4.5, 7.5, -1.0), 3.5, 0.07, 0.0)
    for k in range(5):
        a = math.radians(-40 + 20 * k)
        f = Part("feather%d" % k)
        L.feather(f, (4.4, 7.6, -1.0), (4.4 + 7.2 * math.sin(a) + 1.0, 7.6 + 7.2 * math.cos(a) + 1.5, -1.0 + 0.4 * k - 0.8), 0.95, 0.25, 5)
        m.add(f, MAIN if k % 2 else ACCENT, "feather", SHELL, group=g, uv="fit")
        eye = Part("eye%d" % k)
        L.dia(eye, (4.4 + 6.0 * math.sin(a) + 0.9, 7.6 + 6.0 * math.cos(a) + 1.2, -1.2 + 0.4 * k - 0.8), 0.45, 0.6, 0.18)
        m.add(eye, GLOW, "gem", EMIT, group=g, uv="fit")
    return m


@model
def circlet_dawn():
    """A laurel circlet round the brow with a rayed sun disc at the front and two swept golden blades at the temples: the Golden Dawn's mark."""
    m = WModel("circlet_dawn", "head")
    bd = Part("band")
    L.band(bd, 6.4, 7.5, 4.6, 4.6, 0.4, L.RN, 0.1, 0.1, 3.0)
    m.add(bd, TRIM, "gilt", SHELL)
    for sx in (-1, 1):
        for k in range(5):
            lf = Part("leaf%d_%d" % (sx, k))
            a = math.radians(70 + k * 24)
            x, z = sx * 4.65 * math.sin(math.radians(60 + k * 22)), -4.65 * math.cos(math.radians(60 + k * 22))
            L.feather(lf, (x, 6.7, z), (x + sx * 0.4, 8.7 + 0.2 * k, z - 0.0), 0.55, 0.2, 3)
            m.add(lf, MAIN, "gilt", 0, uv="fit")
    d = Part("disc")
    loft(d, "-z", (0, 7.9, -4.75), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], L.ml.circle(1.25, 12), caps=(True, True))
    m.add(d, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(10):
        a = 2 * math.pi * k / 10
        ry = Part("ray%d" % k)
        L.cone(ry, (1.45 * math.cos(a), 7.9 + 1.45 * math.sin(a), -4.95), (2.6 * math.cos(a), 7.9 + 2.6 * math.sin(a), -5.05), 0.32, 4, mid=0.3, mid_r=1.0)
        m.add(ry, TRIM, "gilt", SHELL, uv="fit")
    for sx in (-1, 1):
        bl = Part("blade%d" % sx)
        L.feather(bl, (sx * 4.7, 7.2, -1.4), (sx * 8.6, 10.4, 0.8), 0.9, 0.3, 5)
        m.add(bl, TRIM, "gilt", SHELL, uv="fit")
    return m


@model
def crest_eagle():
    """A silver headband with a fan of tall feathers at the back of the head and two small swept wings at the temples: the Silver Eagle's crest."""
    m = WModel("crest_eagle", "head")
    bd = Part("band")
    L.band(bd, 6.0, 7.0, 4.55, 4.55, 0.35, L.RN, 0.0, 0.0, 3.0)
    m.add(bd, TRIM, "gilt", SHELL)
    g = m.group("crest", "sway", (0, 7.0, 4.0), 3.5, 0.06, 0.0)
    for k in range(7):
        a = math.radians(-60 + 20 * k)
        f = Part("plume%d" % k)
        L.feather(f, (0.0, 7.4, 4.3), (7.0 * math.sin(a), 7.4 + 8.4 * math.cos(a) - 0.5 * abs(k - 3), 5.4 + 0.0 * k), 1.0, 0.3, 5)
        m.add(f, MAIN if k % 2 == 0 else ACCENT, "feather", SHELL, group=g, uv="fit")
    for sx in (-1, 1):
        for k in range(3):
            w = Part("wing%d_%d" % (sx, k))
            L.feather(w, (sx * 4.5, 6.6 - 0.2 * k, -0.4), (sx * (8.2 - 0.3 * k), 7.8 + 0.9 * k + 0.0, 1.2 + 0.5 * k), 0.8, 0.22, 4)
            m.add(w, MAIN, "feather", SHELL, uv="fit")
    g2 = Part("stone")
    L.ml.gem(g2, (0, 6.5, -4.95), 0.6, 0.9, 6, "z")
    m.add(g2, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def hood_mane():
    """A lion-mane hood: a fitted cap ringed with fur tufts that stand out round the face and fall to the shoulders, round ears and a brow plate: the Crimson Lion's hood."""
    m = WModel("hood_mane", "head")
    cap = Part("cap")
    L.sph(cap, (0, 6.4, 0), 4.9, 4, 12, squash=0.72)
    m.add(cap, MAIN, "fur", SHELL)
    n = 22
    for k in range(n):
        th = 2 * math.pi * k / n
        x, z = 4.9 * math.cos(th), 4.9 * math.sin(th)
        if z < -1.0 and abs(x) < 2.6:
            continue                                                 # keep the face clear
        h = 3.4 + 1.4 * ((k * 5) % 3)
        t = Part("tuft%d" % k)
        L.cone(t, (x, 6.4, z), (x * 1.45, 6.4 - h, z * 1.45), 1.2, 5, mid=0.4, mid_r=0.9)
        m.add(t, MAIN if k % 2 else TRIM, "fur", SHELL)
    for sx in (-1, 1):
        e = Part("ear%d" % sx)
        L.sph(e, (sx * 3.3, 9.4, 0.4), 1.4, 3, 8, squash=0.8)
        m.add(e, MAIN, "fur", SHELL)
        ei = Part("ear_in%d" % sx)
        L.sph(ei, (sx * 3.3, 9.3, -0.4), 0.75, 2, 6, squash=0.7)
        m.add(ei, TRIM, "gilt", 0)
    bp = Part("brow")
    loft(bp, "z", (0, 7.6, -4.6), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], [(-2.0, -0.7), (2.0, -0.7), (1.4, 0.9), (-1.4, 0.9)][::-1])
    m.add(bp, TRIM, "gilt", SHELL, uv="fit")
    g = Part("stone")
    L.dia(g, (0, 7.8, -5.2), 0.45, 0.5, 0.3)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def crown_diamond():
    """A crown of crystal shards: a silver band with a ring of tall faceted shards, a big diamond at the front and a cold lit core."""
    m = WModel("crown_diamond", "head")
    bd = Part("band")
    L.band(bd, 6.3, 7.5, 4.55, 4.55, 0.4, L.RN, 0.1, 0.1, 3.0)
    m.add(bd, TRIM, "gilt", SHELL)
    n = 9
    for k in range(n):
        th = 2 * math.pi * k / n - math.pi / 2
        x, z = 4.8 * math.cos(th), 4.8 * math.sin(th)
        h = 4.6 if k == 0 else 2.6 + 1.2 * ((k * 3) % 3)
        s = Part("shard%d" % k)
        L.dia(s, (x, 7.4 + h / 2, z), 0.85, h / 2, 0.85)
        m.add(s, GLOW if k == 0 else ACCENT, "crystal", EMIT if k == 0 else SHELL, uv="fit")
    core = Part("core")
    L.dia(core, (0, 9.2, 0.0), 1.4, 2.2, 1.4)
    m.add(core, GLOW, "crystal", EMIT | HOT, uv="fit")
    return m


@model
def helm_war():
    """A war helm: a plated cap with a raised crest ridge, a nose guard, cheek plates and a trimmed brow band, with a lit visor slit: the Spade war coat's helm."""
    m = WModel("helm_war", "head")
    cap = Part("cap")
    loft(cap, "y", (0, 0, 0), [(3.0, 1.0, 1.0), (7.0, 1.04, 1.04), (8.5, 1.0, 1.0), (9.6, 0.66, 0.66), (10.2, 0.2, 0.2)], _rr(9.6, 9.6, 3.0, 2), caps=(False, True))
    m.add(cap, MAIN, "plate", SHELL)
    bd = Part("band")
    L.band(bd, 6.0, 7.1, 4.75, 4.75, 0.3, L.RN, 0.0, 0.0, 3.4)
    m.add(bd, TRIM, "gilt")
    rd = Part("ridge")
    loft(rd, "z", (0, 10.4, 0.0), [(-4.0, 0.6, 1.0), (4.0, 0.6, 1.0)], [(-0.6, 0.0), (0.0, 1.6), (0.6, 0.0)] + [(0.0, -0.3)])
    m.add(rd, TRIM, "gilt", SHELL)
    ng = Part("nose")
    loft(ng, "y", (0, 7.2, -5.0), [(0.0, 0.5, 0.2), (-4.2, 0.45, 0.2)], octagon(1.0, 1.0, 0.25))
    m.add(ng, TRIM, "plate", SHELL)
    for sx in (-1, 1):
        ch = Part("cheek%d" % sx)
        loft(ch, "y", (sx * 4.75, 3.4, -0.3), [(0.0, 0.2, 2.6), (-3.8, 0.2, 2.2), (-4.6, 0.1, 1.2)], octagon(1.0, 1.0, 0.2))
        m.add(ch, MAIN, "plate", SHELL)
        ey = Part("slit%d" % sx)
        L.dia(ey, (sx * 1.6, 4.9, -5.2), 0.8, 0.25, 0.15)
        m.add(ey, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def helm_devil():
    """A horned devil helm: a dark cap with a jagged crown ridge, two big curved horns, cheek spikes and glowing slits: the Devil-bound coat's helm."""
    m = WModel("helm_devil", "head")
    cap = Part("cap")
    loft(cap, "y", (0, 0, 0), [(3.0, 1.0, 1.0), (7.0, 1.04, 1.04), (8.5, 1.0, 1.0), (9.6, 0.66, 0.66), (10.1, 0.2, 0.2)], _rr(9.6, 9.6, 3.0, 2), caps=(False, True))
    m.add(cap, MAIN, "plate", SHELL)
    for sx in (-1, 1):
        _horn(m, "horn%d" % sx, (sx * 3.4, 8.0, -1.0), (sx * 9.0, 15.0, -3.0), 1.5, mid=(sx * 9.4, 10.5, -1.2))
        _horn(m, "hornb%d" % sx, (sx * 3.4, 7.0, 1.4), (sx * 7.6, 11.2, 4.6), 1.0, mid=(sx * 8.4, 8.4, 3.6))
        for k in range(2):
            sp = Part("cheek%d_%d" % (sx, k))
            L.cone(sp, (sx * 4.5, 3.6 - 1.7 * k, -2.0 + 0.3 * k), (sx * 6.6, 2.2 - 2.2 * k, -3.6 + 0.4 * k), 0.7, 4, mid=0.4, mid_r=0.9)
            m.add(sp, TRIM, "horn", SHELL)
        ey = Part("slit%d" % sx)
        L.dia(ey, (sx * 1.7, 4.9, -4.9), 0.9, 0.3, 0.2)
        m.add(ey, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(5):
        sp = Part("crown%d" % k)
        L.cone(sp, (0.0, 9.7, -2.4 + k * 1.4), (0.0, 12.2 - 0.5 * abs(k - 2), -2.4 + k * 1.4), 0.7, 4, mid=0.4, mid_r=0.9)
        m.add(sp, TRIM, "horn", SHELL)
    return m


# ================================================================ legs (rleg anchor; both legs wear the same model) and the hip band (body anchor)
def _leg_tube(m, y0=0.3, y1=-11.0, w=4.7, tile="cloth", slot=MAIN, taper=0.96, flare_k=0.0):
    p = Part("leg")
    loft(p, "y", (0, 0, 0), [(y0, 1.0, 1.0), (-1.5, 1.04, 1.04), (-6.0, 1.0, 1.0), (y1 + 2.0, taper, taper), (y1, taper + flare_k, taper + flare_k)], _rr(w, w, 1.4, 2), caps=(False, False))
    m.add(p, slot, tile, SHELL)
    return p


@model
def legs_cloth():
    """Trousers: a lofted tube from the hip to the ankle with a trim stripe down the outer side, a knee patch and a cuff."""
    m = WModel("legs_cloth", "rleg")
    _leg_tube(m)
    st = Part("stripe")
    loft(st, "y", (2.55, 0, 0), [(-0.5, 1.0, 1.0), (-10.5, 1.0, 1.0)], octagon(0.5, 2.2, 0.15))
    m.add(st, TRIM, "gilt")
    kp = Part("knee")
    L.sph(kp, (0.0, -5.2, -2.1), 1.5, 3, 8, squash=0.55)
    m.add(kp, TRIM, "cloth", SHELL)
    cf = Part("cuff")
    L.band(cf, -11.2, -10.5, 2.4, 2.4, 0.2, 12, 0.0, 0.0, 3.2)
    m.add(cf, TRIM, "gilt")
    return m


@model
def legs_wrapped():
    """Robe trousers gathered at the knee: a wide thigh, a tied knee wrap with a tassel, a fitted shin and a trim hem."""
    m = WModel("legs_wrapped", "rleg")
    p = Part("leg")
    loft(p, "y", (0, 0, 0), [(0.3, 1.0, 1.0), (-2.0, 1.1, 1.08), (-4.6, 1.02, 1.0), (-6.4, 0.93, 0.93), (-9.0, 0.97, 0.97), (-11.0, 1.02, 1.02)], _rr(4.9, 4.9, 1.4, 2), caps=(False, False))
    m.add(p, MAIN, "silk", SHELL)
    kw = Part("knee_wrap")
    L.band(kw, -6.1, -5.2, 2.5, 2.5, 0.25, 12, 0.0, 0.0, 3.2)
    m.add(kw, TRIM, "stripe")
    g = m.group("tassel", "swing", (0, -5.4, -2.5), 6.0, 0.9, 0.0)
    ts = Part("tassel")
    loft(ts, "-y", (0.0, -5.5, -2.6), [(0.0, 0.4, 0.2), (2.6, 0.5, 0.2), (3.4, 0.0, 0.0)], octagon(1.0, 1.0, 0.25))
    m.add(ts, TRIM, "silk", 0, group=g)
    h = Part("hem")
    L.band(h, -11.2, -10.6, 2.5, 2.5, 0.2, 12, 0.0, 0.0, 3.2)
    m.add(h, TRIM, "gilt")
    return m


@model
def legs_plated():
    """Armoured leggings: three overlapping hip tassets, a flared knee cop with a spike, a plated shin shell and cloth between the plates."""
    m = WModel("legs_plated", "rleg")
    _leg_tube(m, tile="cloth", slot=DARK)
    for k, y in enumerate((0.4, -1.6, -3.4)):
        pl = Part("tasset%d" % k)
        loft(pl, "y", (0, y, 0), [(0.0, 0.94, 0.94), (-1.3, 1.1 + 0.03 * k, 1.1), (-2.0, 1.12 + 0.03 * k, 1.1)], octagon(4.9, 4.9, 1.5), caps=(False, False))
        m.add(pl, MAIN if k != 1 else TRIM, "plate" if k != 1 else "gilt", SHELL)
    knee = Part("knee")
    L.sph(knee, (0.0, -6.0, -1.5), 2.4, 3, 8, squash=0.7)
    m.add(knee, MAIN, "plate", SHELL)
    ks = Part("knee_spike")
    L.cone(ks, (0.0, -6.0, -3.3), (0.0, -6.2, -5.2), 0.7, 5, mid=0.4, mid_r=0.8)
    m.add(ks, TRIM, "gilt")
    sh = Part("shin")
    loft(sh, "y", (0, -7.6, -0.2), [(0.0, 0.9, 0.9), (-3.3, 0.86, 0.86)], octagon(4.6, 4.3, 1.3), caps=(False, False))
    m.add(sh, MAIN, "plate", SHELL)
    g = Part("stone")
    L.ml.gem(g, (0.0, -6.0, -3.3), 0.45, 0.8, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def legs_devil():
    """Torn black leggings with bone-spike knees, chain loops round the thigh and glowing cracks down the shin."""
    m = WModel("legs_devil", "rleg")
    _leg_tube(m, slot=MAIN, tile="cloth")
    for k in range(2):
        s = Part("knee_spike%d" % k)
        L.cone(s, (-1.2 + 2.4 * k, -5.6, -2.2), (-1.7 + 3.4 * k, -4.4 - 0.4 * k, -4.7), 0.65, 4, mid=0.4, mid_r=0.9)
        m.add(s, TRIM, "horn", SHELL)
    for y in (-1.5, -8.0):
        c = Part("chain%.0f" % y)
        L.torus(c, (0.0, y, 0.0), 2.65, 0.28, 12, 4, plane="xz", a_scale=1.0, b_scale=1.0)
        m.add(c, TRIM, "chain")
    cr = Part("crack")
    loft(cr, "y", (0.0, -8.0, -2.4), [(0.0, 0.2, 0.1), (-2.2, 0.35, 0.1), (-3.0, 0.0, 0.0)], octagon(1.0, 1.0, 0.25))
    m.add(cr, GLOW, "soft", EMIT | HOT, uv="fit")
    return m


@model
def hip_sash():
    """A waist sash with a trim edge and a short hanging flap at each hip."""
    m = WModel("hip_sash", "body")
    b = Part("band")
    L.band(b, -12.6, -9.4, 4.85, 2.85, 0.3, 14, 0.0, 0.0, 3.2)
    m.add(b, MAIN, "silk", SHELL)
    tr = Part("edge")
    L.band(tr, -9.6, -9.1, 4.9, 2.9, 0.34, 14, 0.0, 0.0, 3.2)
    m.add(tr, TRIM, "gilt")
    g = m.group("flaps", "swing", (0, -12.4, 0), 6.0, 0.9, 0.0)
    for sx in (-1, 1):
        f = Part("flap%d" % sx)
        L.sheet(f, lambda u, v, sx=sx: (sx * (4.2 + 0.2 * v), -12.4 - v * 5.0, -1.6 + u * 3.2), 2, 3, 0.35 * sx)
        m.add(f, TRIM, "silk", SHELL, group=g)
    g2 = Part("stone")
    L.dia(g2, (0.0, -11.0, -3.1), 0.55, 0.7, 0.3)
    m.add(g2, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def hip_plated():
    """A plated belt: a wide band with a buckle plate and a stone, a front fauld of three plates and two side hip guards."""
    m = WModel("hip_plated", "body")
    b = Part("band")
    L.band(b, -12.8, -9.2, 4.85, 2.85, 0.35, 14, 0.0, 0.0, 3.2)
    m.add(b, DARK, "leather", SHELL)
    for k in range(3):
        f = Part("fauld%d" % k)
        loft(f, "y", (0, -11.2 - 1.5 * k, -3.0 + 0.0 * k), [(0.0, 1.0 + 0.1 * k, 0.4), (-1.5, 1.0 + 0.1 * k, 0.4)], octagon(4.8, 1.0, 0.3), caps=(False, False))
        m.add(f, MAIN if k % 2 == 0 else TRIM, "plate" if k % 2 == 0 else "gilt", SHELL)
    for sx in (-1, 1):
        hg = Part("guard%d" % sx)
        loft(hg, "y", (sx * 4.9, -10.2, 0.0), [(0.0, 0.4, 1.8), (-3.4, 0.45, 1.9), (-4.5, 0.2, 1.0)], octagon(1.0, 1.0, 0.3))
        m.add(hg, MAIN, "plate", SHELL)
    bk = Part("buckle")
    loft(bk, "z", (0.0, -10.6, -3.3), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], octagon(2.6, 2.2, 0.6))
    m.add(bk, TRIM, "gilt", SHELL, uv="fit")
    g = Part("stone")
    L.ml.gem(g, (0.0, -10.6, -3.9), 0.6, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


# ================================================================ boots (rleg anchor)
@model
def boots_soft():
    """Soft cloth shoes: a fitted cloth shaft with an ankle wrap and tie, a rounded toe and a thin sole."""
    m = WModel("boots_soft", "rleg")
    sh = Part("shaft")
    loft(sh, "y", (0, -7.0, 0), [(0.0, 1.0, 1.0), (-2.6, 0.98, 0.98), (-4.6, 1.02, 1.02)], _rr(4.7, 4.7, 1.3, 2), caps=(False, True))
    m.add(sh, MAIN, "cloth", SHELL)
    tp = Part("toe")
    loft(tp, "-z", (0, -10.9, -2.0), [(0.0, 1.0, 1.0), (1.2, 0.98, 0.9), (2.3, 0.78, 0.6), (2.8, 0.0, 0.0)], octagon(4.7, 2.7, 0.9), caps=(False, True))
    m.add(tp, MAIN, "cloth", SHELL)
    sl = Part("sole")
    loft(sl, "y", (0, -11.9, -0.4), [(0.0, 1.0, 1.0), (-0.7, 1.0, 1.0)], octagon(4.9, 7.4, 0.9))
    m.add(sl, DARK, "leather")
    wr = Part("wrap")
    L.band(wr, -8.2, -7.2, 2.5, 2.5, 0.2, 12, 0.0, 0.0, 3.2)
    m.add(wr, TRIM, "stripe")
    return m


@model
def boots_tall():
    """Tall riding boots: a shaft that hugs the shin to the knee, a turned-over cuff with trim, a stitched toe cap, a heel and a lit stone at the ankle strap."""
    m = WModel("boots_tall", "rleg")
    sh = Part("shaft")
    loft(sh, "y", (0, -1.6, 0), [(0.0, 1.0, 1.0), (-3.0, 0.97, 0.97), (-6.6, 1.02, 1.02), (-8.2, 1.0, 1.0)], _rr(4.75, 4.75, 1.3, 2), caps=(False, True))
    m.add(sh, MAIN, "leather", SHELL)
    cf = Part("cuff")
    loft(cf, "y", (0, -1.2, 0), [(0.0, 0.98, 0.98), (-0.4, 1.1, 1.1), (-1.9, 1.1, 1.1), (-2.3, 1.0, 1.0)], _rr(5.2, 5.2, 1.4, 2), caps=(False, False))
    m.add(cf, TRIM, "leather", SHELL)
    tp = Part("toe")
    loft(tp, "-z", (0, -11.2, -2.0), [(0.0, 1.0, 1.0), (1.2, 0.98, 0.9), (2.3, 0.8, 0.6), (2.8, 0.0, 0.0)], octagon(4.7, 2.6, 0.9), caps=(False, True))
    m.add(tp, MAIN, "leather", SHELL)
    tc = Part("toe_cap")
    loft(tc, "-z", (0, -11.3, -2.3), [(0.0, 1.0, 1.0), (1.2, 0.95, 0.85), (2.1, 0.75, 0.55), (2.6, 0.0, 0.0)], octagon(4.9, 2.3, 0.8), caps=(False, True))
    m.add(tc, TRIM, "gilt", SHELL)
    sl = Part("sole")
    loft(sl, "y", (0, -11.9, -0.4), [(0.0, 1.0, 1.0), (-0.7, 1.0, 1.0)], octagon(4.9, 7.4, 0.9))
    m.add(sl, DARK, "leather")
    hl = Part("heel")
    loft(hl, "y", (0, -11.2, 2.3), [(0.0, 1.0, 1.0), (-1.2, 0.9, 0.9)], octagon(4.7, 1.2, 0.4))
    m.add(hl, TRIM, "leather")
    st = Part("strap")
    L.band(st, -9.0, -8.2, 2.5, 2.5, 0.2, 12, 0.0, 0.0, 3.2)
    m.add(st, TRIM, "gilt")
    g = Part("stone")
    L.dia(g, (0.0, -8.6, -2.9), 0.45, 0.55, 0.3)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def boots_war():
    """War boots: a steel sabaton with banded toe plates, an armoured shin shell with a ridge, a flared ankle fin on each side and a heel spur."""
    m = WModel("boots_war", "rleg")
    sh = Part("shin")
    loft(sh, "y", (0, -3.0, 0), [(0.0, 0.98, 0.98), (-4.0, 0.94, 0.94), (-7.2, 1.0, 1.0)], octagon(4.9, 4.7, 1.4), caps=(False, True))
    m.add(sh, MAIN, "plate", SHELL)
    rg = Part("ridge")
    loft(rg, "y", (0, -3.2, -2.5), [(0.0, 0.5, 0.45), (-5.2, 0.5, 0.45), (-6.2, 0.0, 0.0)], octagon(1.0, 1.0, 0.25))
    m.add(rg, TRIM, "gilt")
    for k in range(3):
        bd = Part("toe_band%d" % k)
        loft(bd, "-z", (0, -10.8 + 0.2 * k, -1.4 - 0.8 * k), [(0.0, 1.0 - 0.06 * k, 1.0), (0.9, 1.0 - 0.1 * k, 0.9)], octagon(4.9, 3.0, 1.0), caps=(False, True))
        m.add(bd, MAIN if k % 2 == 0 else TRIM, "plate" if k % 2 == 0 else "gilt", SHELL)
    for sx in (-1, 1):
        fn = Part("fin%d" % sx)
        loft(fn, "y", (sx * 2.5, -7.0, 0.4), [(0.0, 0.2, 1.2), (-2.4, 0.2, 1.7), (-4.0, 0.1, 0.5)], octagon(1.0, 1.0, 0.2))
        m.add(fn, TRIM, "plate")
    sl = Part("sole")
    loft(sl, "y", (0, -11.9, -0.4), [(0.0, 1.0, 1.0), (-0.7, 1.0, 1.0)], octagon(4.9, 7.6, 0.9))
    m.add(sl, DARK, "plate")
    sp = Part("spur")
    L.cone(sp, (0.0, -10.6, 2.4), (0.0, -10.9, 4.4), 0.5, 4, mid=0.4, mid_r=0.9)
    m.add(sp, TRIM, "gilt")
    g = Part("stone")
    L.ml.gem(g, (0.0, -3.8, -2.6), 0.5, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def boots_devil():
    """Clawed boots: a black shin shell with bone spikes, a long toe that ends in three claws, a heel spike and lit rune bands round the ankle."""
    m = WModel("boots_devil", "rleg")
    sh = Part("shaft")
    loft(sh, "y", (0, -3.4, 0), [(0.0, 0.98, 0.98), (-4.0, 0.95, 0.95), (-7.4, 1.0, 1.0)], octagon(4.9, 4.7, 1.4), caps=(False, True))
    m.add(sh, MAIN, "plate", SHELL)
    tp = Part("toe")
    loft(tp, "-z", (0, -11.0, -2.0), [(0.0, 1.0, 1.0), (1.4, 0.95, 0.85), (2.6, 0.7, 0.5), (3.2, 0.0, 0.0)], octagon(4.7, 2.6, 0.9), caps=(False, True))
    m.add(tp, MAIN, "plate", SHELL)
    for k in range(3):
        c = Part("claw%d" % k)
        L.cone(c, (-1.4 + 1.4 * k, -11.1, -4.6), (-1.8 + 1.8 * k, -11.5, -7.4), 0.5, 4, mid=0.35, mid_r=0.9)
        m.add(c, TRIM, "horn", SHELL)
    for k in range(3):
        sp = Part("spike%d" % k)
        L.cone(sp, (-2.4 if k < 2 else 2.4, -4.6 - 1.6 * (k % 2), 0.4), ((-3.8 if k < 2 else 3.8), -4.0 - 2.4 * (k % 2), 1.0), 0.55, 4, mid=0.4, mid_r=0.9)
        m.add(sp, TRIM, "horn", SHELL)
    hs = Part("heel_spike")
    L.cone(hs, (0.0, -9.6, 2.4), (0.0, -8.8, 5.4), 0.7, 4, mid=0.4, mid_r=0.9)
    m.add(hs, TRIM, "horn", SHELL)
    for y in (-7.6, -9.2):
        rb = Part("rune%.1f" % y)
        L.band(rb, y, y + 0.5, 2.6, 2.6, 0.15, 12, 0.0, 0.0, 3.0)
        m.add(rb, GLOW, "rune", EMIT | HOT, density=3.0)
    sl = Part("sole")
    loft(sl, "y", (0, -11.9, -0.4), [(0.0, 1.0, 1.0), (-0.7, 1.0, 1.0)], octagon(4.9, 7.6, 0.9))
    m.add(sl, DARK, "plate")
    return m


# ================================================================ the catalog of the armour items
# id, hood, chest, sleeves, legs, hip, boots, emblem, palette (main, trim, glow), grade tier, particle theme
SETS = [
    ("magic_knight_robe_junior", "hood_cowl", "chest_tunic", "sleeve_plain", "legs_cloth", "hip_sash", "boots_soft", "emblem_clover", (0x54546C, 0xCCCCCC, 0x90B4E0), 2, None),
    ("magic_knight_robe_senior", "hood_cowl", "chest_robe", "sleeve_wide", "legs_wrapped", "hip_sash", "boots_tall", "emblem_clover", (0xE6E6EA, 0xB42424, 0xFFB0A0), 3, "petal"),
    ("golden_dawn_mantle", "circlet_dawn", "chest_mantle", "sleeve_plain", "legs_cloth", "hip_sash", "boots_tall", "emblem_sun", (0xC8AE5C, 0x6C84B4, 0xFFE890), 4, "star"),
    ("black_bull_robe", "hood_horned", "chest_mantle", "sleeve_plain", "legs_cloth", "hip_sash", "boots_tall", "emblem_bull", (0x242424, 0xE4B454, 0xE4B454), 3, "spark"),
    ("silver_eagle_cloak", "crest_eagle", "chest_robe", "sleeve_wide", "legs_wrapped", "hip_sash", "boots_tall", "emblem_eagle", (0x5E9AC8, 0x9C7CD0, 0xB8E8FF), 4, "star"),
    ("crimson_lion_mantle", "hood_mane", "chest_mantle", "sleeve_plated", "legs_plated", "hip_plated", "boots_war", "emblem_lion", (0x842424, 0xE0A040, 0xFF6A30), 4, "ember"),
    ("coral_peacock_robe", "hat_peacock", "chest_robe", "sleeve_wide", "legs_wrapped", "hip_sash", "boots_soft", "emblem_peacock", (0x3CB4B4, 0x9C54B4, 0x6CF0E0), 4, "wisp"),
    ("heart_kingdom_uniform", "hat_mage", "chest_coat", "sleeve_plain", "legs_cloth", "hip_sash", "boots_tall", "emblem_heart", (0xE49CB4, 0x54B49C, 0xFFB8D8), 3, "petal"),
    ("diamond_mage_coat", "crown_diamond", "chest_tunic", "sleeve_plain", "legs_cloth", "hip_plated", "boots_tall", "emblem_diamond", (0x6C543C, 0xCCB484, 0x9AF0FF), 3, "snow"),
    ("spade_war_coat", "helm_war", "chest_coat", "sleeve_plated", "legs_plated", "hip_plated", "boots_war", "emblem_spade", (0x3C3C3C, 0x9C2424, 0xFF5050), 5, "spark"),
    ("devil_bound_coat", "helm_devil", "chest_devil", "sleeve_plated", "legs_devil", "hip_plated", "boots_devil", "emblem_devil", (0x0C0C0C, 0xCC0C24, 0xFF2038), 6, "ember"),
]


def catalog_items(cat):
    """The four pieces of each robe set (hood, robe, leggings, boots): the item ids are the ones NUItems registers (id_hood, id, id_leggings, id_boots)."""
    for (sid, hood, chest, sleeve, legs, hip, boots, emblem, pal, tier, theme) in SETS:
        cat.item(sid + "_hood", (hood,), pal, tier)
        cat.item(sid, (chest, sleeve + "@rarm", sleeve + "@larm~", emblem), pal, tier)
        cat.item(sid + "_leggings", (hip, legs + "@rleg", legs + "@lleg"), pal, tier)
        cat.item(sid + "_boots", (boots + "@rleg", boots + "@lleg"), pal, tier)
        if theme:
            for suffix in ("_hood", "", "_leggings", "_boots"):
                cat.theme(sid + suffix, theme)


# Curios-worn items of the mod that are not relics (the dungeon relics and the charter grimoires): the existing library models in their own palettes.
CURIOS = [
    ("mantle_of_the_scavenger", "BACK", ("mantle_shoulder",), (0x6B5A3A, 0xC9A25A, 0xFFD27A), 3),
    ("band_of_the_spatial_anchor", "HANDS", ("bracelet_charms@rarm", "ring_gem@larm~"), (0x3A2E5A, 0xB0A0E8, 0x9A7CFF), 4),
    ("devil_bound_sheath", "BELT", ("belt_buckled",), (0x2A1218, 0x8A2030, 0xFF4050), 4),
    ("gamblers_grimoire_page", "CHARM", ("charm_tome",), (0x7A1E2A, 0xE8C060, 0xFFD870), 3),
    ("amulet_of_the_juggernauts_blindness", "NECK", ("amulet_medallion",), (0x3A3A44, 0xA0A0B0, 0x60E0FF), 4),
    ("glutton_chalice", "CHARM", ("charm_flask",), (0x5A3A6A, 0xD0B060, 0x9AFF70), 3),
    ("charter_grimoire_dante", "CHARM", ("charm_tome",), (0x5A1020, 0xD0A040, 0xFF5030), 4),
    ("charter_grimoire_vanica", "CHARM", ("charm_tome",), (0x2A2A5A, 0xC0C8E8, 0x7FA8FF), 4),
    ("charter_grimoire_zenon", "CHARM", ("charm_tome",), (0x1A1A2E, 0x9A7CFF, 0xC8A0FF), 4),
    ("charter_grimoire_lucius", "CHARM", ("charm_tome",), (0x3A2A10, 0xE8C870, 0xFFE890), 4),
    ("charter_grimoire_devil_avatar", "CHARM", ("charm_tome",), (0x2A0A10, 0xCC1030, 0xFF3050), 5),
    ("charter_grimoire_goliath", "CHARM", ("charm_tome",), (0x3A3A30, 0xA0A090, 0xFFD060), 4),
    ("charter_grimoire_charybdis_spawn", "CHARM", ("charm_tome",), (0x0A3A4A, 0x60C0D0, 0x6AFFE8), 4),
]


def catalog_curios(cat):
    for key, slot, pieces, pal, tier in CURIOS:
        cat.item(key, pieces, pal, tier)
        cat.slot_of(key, slot)

"""The worn NECK and CHEST-brooch models: amulets, pendants, lockets, crests, brooches. Body anchor (origin at the neck line, front is -z)."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)

MODELS = {}
PZ = -3.15                     # the front plane of a hanging pendant: just outside a chest plate (z -3)


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


def _chain(m, drop=3.6, z=PZ, r=0.2, tile="chain", slot=TRIM, wide=3.6, name="chain"):
    """The chain round the neck: from the back of the collar over each shoulder, down the chest to a V that ends at the pendant ring (0, -drop, z)."""
    for sx in (-1, 1):
        c = Part("%s%d" % (name, sx))
        pts = L.bez3((sx * 1.8, 0.2, 2.45), (sx * wide + 0.8, -0.2, 0.2), (sx * (wide - 0.2), -1.4, z + 0.4), (0.0, -drop + 0.5, z + 0.05), 7)
        sweep(c, pts, circle(r, 4), scales=[(1.0, 1.0), (0.7, 0.7)] * 4, caps=(True, True))
        m.add(c, slot, tile)
    back = Part(name + "_back")
    pts = L.bez3((-1.8, 0.2, 2.45), (-1.0, 0.1, 2.9), (1.0, 0.1, 2.9), (1.8, 0.2, 2.45), 4)
    sweep(back, pts, circle(r, 4), scales=[(1.0, 1.0), (0.7, 0.7)] * 3, caps=(True, True))
    m.add(back, slot, tile)


def _hang(m, tag, y, z=PZ):
    """The little bail ring a pendant hangs from."""
    r = Part("bail_" + tag)
    L.torus(r, (0, y + 0.35, z), 0.42, 0.14, 6, 4, plane="yz")
    m.add(r, TRIM, "gilt")


@model
def amulet_medallion():
    """A round medallion in a beaded frame: an outer ring, an engraved plate, a raised rim, a faceted stone in the middle and four small studs, on a chain."""
    m = WModel("amulet_medallion", "body")
    _chain(m, 3.3)
    _hang(m, "m", -3.3 - 0.4)
    c = (0.0, -5.0, PZ)
    ring = Part("ring")
    L.torus(ring, c, 1.7, 0.32, 14, 5, plane="xy")
    m.add(ring, TRIM, "gilt")
    disc = Part("plate")
    loft(disc, "z", (0, -5.0, PZ - 0.1), [(0.0, 1.7, 1.7), (0.3, 1.7, 1.7)], L.circle(1.0, 14))
    m.add(disc, MAIN, "plate", SHELL, uv="fit")
    g = Part("stone")
    L.ml.gem(g, (0, -5.0, PZ - 0.4), 0.85, 0.9, 6, "z")
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    for k in range(4):
        a = math.pi / 4 + k * math.pi / 2
        s = Part("stud%d" % k)
        L.dia(s, (1.3 * math.cos(a), -5.0 + 1.3 * math.sin(a), PZ - 0.35), 0.25, 0.25, 0.25)
        m.add(s, ACCENT, "gilt", uv="fit")
    return m


@model
def pendant_drop():
    """A teardrop pendant: a shaped bezel and a faceted drop stone hanging from a short bail, with a small bead above it."""
    m = WModel("pendant_drop", "body")
    _chain(m, 3.2)
    bead = Part("bead")
    L.dia(bead, (0, -3.6, PZ), 0.45, 0.45, 0.45)
    m.add(bead, TRIM, "gilt", uv="fit")
    set_ = Part("setting")
    loft(set_, "y", (0, -4.1, PZ - 0.1), [(0.0, 0.7, 0.5), (-1.2, 1.35, 0.8), (-3.0, 1.1, 0.7), (-4.3, 0.0, 0.0)], L.circle(1.0, 8))
    set_.F = set_.F
    m.add(set_, TRIM, "gilt")
    drop = Part("drop")
    loft(drop, "y", (0, -4.4, PZ - 0.35), [(0.0, 0.55, 0.45), (-1.0, 1.05, 0.7), (-2.4, 0.8, 0.55), (-3.6, 0.0, 0.0)], L.circle(1.0, 6))
    m.add(drop, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def locket_oval():
    """An oval locket: a domed shell with an engraved cover, a hinge on one side and a clasp stone at the lower edge, on a fine chain."""
    m = WModel("locket_oval", "body")
    _chain(m, 3.2, r=0.17)
    _hang(m, "l", -3.6)
    sh = Part("shell")
    loft(sh, "z", (0, -5.4, PZ - 0.05), [(0.0, 1.45, 1.85), (0.5, 1.5, 1.9), (0.95, 1.2, 1.55), (1.15, 0.0, 0.0)], L.circle(1.0, 12))
    m.add(sh, MAIN, "gilt", SHELL)
    rim = Part("rim")
    L.torus(rim, (0, -5.4, PZ - 0.05), 1.65, 0.22, 12, 4, plane="xy")
    m.add(rim, TRIM, "gilt")
    hinge = Part("hinge")
    L.ml.gem(hinge, (1.6, -5.4, PZ - 0.1), 0.28, 2.0, 4, "y")
    m.add(hinge, TRIM, "gilt")
    g = Part("clasp")
    L.dia(g, (0.0, -7.1, PZ - 0.45), 0.4, 0.5, 0.3)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    h = Part("heart")
    L.dia(h, (0.0, -5.4, PZ - 1.15), 0.55, 0.65, 0.2)
    m.add(h, GLOW, "gem", EMIT, uv="fit")
    return m


@model
def brooch_chest():
    """A brooch pinned over the heart: a leafy gold setting with a large cabochon and a ring of small studs; no chain."""
    m = WModel("brooch_chest", "body")
    c = (-2.2, -4.2, PZ - 0.1)
    set_ = Part("setting")
    L.torus(set_, c, 1.35, 0.34, 12, 4, plane="xy")
    m.add(set_, TRIM, "gilt")
    for k in range(6):
        a = k * math.pi / 3 + 0.3
        f = Part("petal%d" % k)
        L.cone(f, (c[0] + 1.2 * math.cos(a), c[1] + 1.2 * math.sin(a), c[2]), (c[0] + 2.4 * math.cos(a), c[1] + 2.4 * math.sin(a), c[2] - 0.2), 0.45, 4, mid=0.4, mid_r=0.9)
        m.add(f, MAIN, "gilt")
    g = Part("cabochon")
    L.sph(g, (c[0], c[1], c[2] - 0.2), 0.95, 3, 8, squash=0.55)
    m.add(g, GLOW, "gem", EMIT | HOT, uv="fit")
    return m


@model
def brooch_badge():
    """A star badge pinned to the chest: a five-pointed plate with a rim and a small centre rivet (a sheriff's star)."""
    m = WModel("brooch_badge", "body")
    cx, cy, cz = -2.2, -4.4, PZ - 0.05
    st = Part("star")
    pts = []
    for k in range(10):
        a = math.pi / 2 + k * math.pi / 5
        r = 2.1 if k % 2 == 0 else 0.95
        pts.append((r * math.cos(a), r * math.sin(a)))
    loft(st, "z", (cx, cy, cz), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], pts)
    m.add(st, MAIN, "plate", SHELL, uv="fit")
    for k in range(5):
        a = math.pi / 2 + k * 2 * math.pi / 5
        b = Part("ball%d" % k)
        L.dia(b, (cx + 2.1 * math.cos(a), cy + 2.1 * math.sin(a), cz - 0.45), 0.3, 0.3, 0.3)
        m.add(b, TRIM, "gilt", uv="fit")
    rv = Part("rivet")
    L.dia(rv, (cx, cy, cz - 0.7), 0.5, 0.5, 0.3)
    m.add(rv, GLOW, "gem", EMIT, uv="fit")
    return m


@model
def amulet_crest():
    """A heraldic crest hanging low on the chest: a shield-shaped plate with a raised border, a central eye-gem, two small wings and a chain."""
    m = WModel("amulet_crest", "body")
    _chain(m, 3.0)
    _hang(m, "c", -3.4)
    sh = Part("shield")
    pts = [(-1.8, 1.6), (1.8, 1.6), (1.9, -0.6), (1.1, -2.2), (0.0, -3.0), (-1.1, -2.2), (-1.9, -0.6)]
    loft(sh, "z", (0, -6.5, PZ - 0.05), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], pts)
    m.add(sh, MAIN, "plate", SHELL, uv="fit")
    bd = Part("border")
    loft(bd, "z", (0, -6.5, PZ - 0.45), [(0.0, 1.0, 1.0), (0.3, 0.86, 0.86), (0.34, 0.6, 0.6)], pts, caps=(False, False))
    m.add(bd, TRIM, "gilt")
    eye = Part("eye")
    L.ml.gem(eye, (0, -6.3, PZ - 0.7), 0.95, 0.9, 6, "z")
    m.add(eye, GLOW, "gem", EMIT | HOT, uv="fit")
    for sx in (-1, 1):
        w = Part("wing%d" % sx)
        L.feather(w, (sx * 1.5, -5.6, PZ - 0.5), (sx * 3.6, -4.2, PZ - 0.2), 0.7, 0.2, 4)
        m.add(w, TRIM, "feather", uv="fit")
    return m


@model
def amulet_charm_cluster():
    """A cluster of three small charms (a stone, a fang and a feather) on a cord, hanging together at the breast."""
    m = WModel("amulet_charm_cluster", "body")
    _chain(m, 3.0, tile="leather", name="cord", r=0.24)
    for k, (dx, dy) in enumerate(((-1.4, -4.2), (0.0, -5.2), (1.4, -4.2))):
        st = Part("bind%d" % k)
        L.torus(st, (dx, dy + 0.9, PZ), 0.3, 0.12, 6, 4, plane="yz")
        m.add(st, TRIM, "gilt")
    s = Part("stone")
    L.dia(s, (-1.4, -4.9, PZ - 0.1), 0.6, 0.9, 0.5)
    m.add(s, GLOW, "gem", EMIT | HOT, uv="fit")
    fang = Part("fang")
    L.cone(fang, (0.0, -5.7, PZ - 0.1), (0.0, -8.4, PZ - 0.3), 0.55, 5, mid=0.3, mid_r=0.9)
    m.add(fang, MAIN, "bone")
    fe = Part("feather")
    L.feather(fe, (1.4, -4.9, PZ - 0.1), (1.7, -8.0, PZ - 0.2), 0.7, 0.18, 4)
    m.add(fe, ACCENT, "feather", uv="fit")
    return m

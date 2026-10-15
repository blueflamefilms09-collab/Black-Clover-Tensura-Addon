"""Signature worn HEAD models of the part 4 armour relics (waves 25-30): masks, helms, goggles, a bucket, a crown, a jellyfish and the Eye of Vecna.

Why: every armour relic (HEAD, CHEST, FEET) must wear its own recognisable model, not only the generic model of its look (ArmorLooksTest). The twenty head relics of
part 4 had only the look's model, so a Mask of Difference and a Mask of Indifference, a bucket and a witchdoctor's totem all looked like the same circlet or helm.
Each model here is read off the card art of its relic (shape first, palette second: the palette is the relic's own, the tiles only give the material): masks are a
curved face plate in front of the face (the head box is x -4..4, y 0..8, z -4..4, front -z) with dark eye slits, helms are a plated cap with their horns and spikes,
and the glow slot lights only what glows on the card (a gem, an eye, a lens, a rune band). Same method and file format as gen_worn_head.py / gen_worn_armor_relics.py;
every model stays under about 1800 vertices (the ensemble cap is 2600). Nothing here replaces or changes an older model: the module is only added to the builders."""
import math

import numpy as np

import gen_worn_lib as L
from gen_worn_lib import (WModel, Part, loft, lathe, sweep, circle, octagon, rect_ring, MAIN, TRIM, GLOW, DARK, ACCENT, NONE, EMIT, SHELL, HOT)
import gen_worn_armor as A

MODELS = {}


def model(fn):
    MODELS[fn.__name__] = fn
    return fn


# ================================================================ helpers
def _face(m, y0, y1, wf, bulge=1.2, slot=MAIN, tile="plate", nu=6, nv=6, thick=0.5, z0=-4.45, bf=None, name="face", flags=SHELL):
    """A curved mask plate in front of the face from y0 to y1; wf(v) is its half width (v 0 chin .. 1 brow), bf(v) scales the bulge. Returns zat(x, v): the front z there."""
    def kb(v):
        return bf(v) if bf else 1.0

    def fn(u, v):
        t = 2 * u - 1
        wr = max(0.0, abs(t) - 0.86) / 0.14
        return (t * wf(v), y0 + (y1 - y0) * v, z0 - bulge * kb(v) * (1 - t * t) + 2.2 * wr * wr)
    p = Part(name)
    L.sheet(p, fn, nu, nv, thick)
    m.add(p, slot, tile, flags)

    def zat(x, v):
        t = max(-1.0, min(1.0, x / wf(v)))
        return z0 - bulge * kb(v) * (1 - t * t) - thick * 0.0
    return zat


def _eye(m, name, x, y, z, rx, ry, rz=0.16, slot=DARK, tile="metal", flags=0):
    p = Part(name)
    L.dia(p, (x, y, z), rx, ry, rz)
    m.add(p, slot, tile, flags, uv="fit")


def _glow(m, name, c, rx, ry, rz, tile="gem"):
    p = Part(name)
    L.dia(p, c, rx, ry, rz)
    m.add(p, GLOW, tile, EMIT | HOT, uv="fit")


def _strap(m, y=4.2, r=4.5, gap=52.0, tile="leather", slot=DARK, rad=0.32, name="strap"):
    """A cord round the sides and back of the head (the front is the mask)."""
    pts = []
    n = 10
    a0, a1 = math.radians(-90 + gap), math.radians(270 - gap)
    for i in range(n + 1):
        th = a0 + (a1 - a0) * i / n
        x, z = L._sq(th, r, r, 3.4)
        pts.append((x, y, z))
    p = Part(name)
    L.tube(p, pts, rad, 4)
    m.add(p, slot, tile)


def _cap(m, y0=3.0, y1=10.0, tile="plate", slot=MAIN, flags=SHELL, w=9.6, name="cap"):
    cap = Part(name)
    loft(cap, "y", (0, 0, 0), [(y0, 1.0, 1.0), (7.0, 1.04, 1.04), (y1 - 1.6, 1.0, 1.0), (y1 - 0.6, 0.66, 0.66), (y1, 0.2, 0.2)], A._rr(w, w, 3.0, 2), caps=(False, True))
    m.add(cap, slot, tile, flags)


def _ribbon(m, name, x, y0, z, length, width, curl, group=0, slot=ACCENT, tile="silk", nv=4):
    sx = 1 if x >= 0 else -1

    def fn(u, v):
        return (x + sx * curl * v * v * length * 0.35, y0 - length * v, z + (u - 0.5) * width + 0.4 * math.sin(v * 5.0 + x))
    p = Part(name)
    L.sheet(p, fn, 1, nv, 0.18)
    m.add(p, slot, tile, SHELL, group=group)


def _tentacle(m, name, p0, p1, p2, p3, r, group, slot=ACCENT, tile="vein", sides=4, n=6):
    pts = L.bez3(p0, p1, p2, p3, n)
    sc = [(1.0 - 0.8 * i / n,) * 2 for i in range(n + 1)]
    p = Part(name)
    L.tube(p, pts, r, sides, sc)
    m.add(p, slot, tile, SHELL, group=group)


def _horn(m, name, pts, r0, r1=0.12, slot=ACCENT, tile="horn", sides=6):
    h = Part(name)
    L.horn(h, pts, r0, r1, sides)
    m.add(h, slot, tile, SHELL)


# ================================================================ masks
@model
def mask_indifference():
    """Mask of Indifference: a blank grey-white face of coarse angular facets (few big flat planes, like folded paper), a sharp nose ridge, slit eyes and a flat mouth slit, held by a dark strap."""
    m = WModel("mask_indifference", "head")
    zat = _face(m, 1.0, 8.2, lambda v: 2.3 + 2.1 * v ** 0.7, bulge=1.5, tile="plate", nu=4, nv=5, thick=0.55)
    nz = Part("nose")
    loft(nz, "y", (0, 2.4, zat(0, 0.2) - 0.3), [(0.0, 0.5, 0.9), (2.6, 0.7, 1.4), (4.0, 0.2, 0.5)], octagon(1.0, 1.4, 0.3))
    m.add(nz, TRIM, "plate", SHELL)
    for sx in (-1, 1):
        _eye(m, "slit%d" % sx, sx * 1.9, 5.6, zat(sx * 1.9, 0.65) - 0.25, 1.15, 0.38)
        br = Part("brow%d" % sx)
        loft(br, "x", (sx * 1.1, 6.5, zat(sx * 2.6, 0.8) - 0.2), [(0.0, 0.5, 0.35), (sx * 3.2, 0.5, 0.35)], rect_ring(1.0, 1.0))
        m.add(br, TRIM, "plate", SHELL)
    _eye(m, "mouth", 0, 2.6, zat(0, 0.25) - 0.2, 1.1, 0.18)
    _strap(m, 4.6)
    return m


@model
def mask_difference():
    """Mask of Difference: a blue domino mask with swept-up eye corners and cheek wings, filigree swirls of the trim colour on the brow, two big eye holes and two rolled ribbons that sway at the sides."""
    m = WModel("mask_difference", "head")
    zat = _face(m, 2.6, 7.6, lambda v: 3.2 + 1.5 * math.sin(math.pi * (0.15 + 0.7 * v)) ** 0.9, bulge=1.1, tile="scale", nu=6, nv=4, thick=0.45)
    for sx in (-1, 1):
        _eye(m, "hole%d" % sx, sx * 1.9, 5.0, zat(sx * 1.9, 0.5) - 0.2, 1.25, 0.6)
        w = Part("wing%d" % sx)
        L.cone(w, (sx * 4.1, 6.0, -3.6), (sx * 5.6, 9.2, -3.0), 0.8, 4, mid=0.4, mid_r=0.9)
        m.add(w, MAIN, "scale", SHELL)
        for k in range(2):
            sp = Part("swirl%d_%d" % (sx, k))
            pts = L.bez3((sx * (0.6 + 0.9 * k), 7.0, zat(sx * 1.0, 0.9) - 0.25), (sx * (1.6 + 1.6 * k), 8.2 - 0.3 * k, zat(sx * 2.0, 0.9) - 0.3),
                         (sx * (3.0 + 0.4 * k), 7.8, zat(sx * 3.0, 0.9) - 0.1), (sx * (2.8 - 0.8 * k), 6.8, zat(sx * 2.8, 0.7) - 0.1), 6)
            L.tube(sp, pts, 0.17, 4)
            m.add(sp, ACCENT, "gilt", EMIT)
        ck = Part("cheek%d" % sx)
        pts = L.bez3((sx * 1.0, 3.5, zat(sx, 0.2) - 0.2), (sx * 2.0, 3.8, zat(sx * 2, 0.25) - 0.2), (sx * 3.0, 3.6, zat(sx * 3, 0.25) - 0.1), (sx * 3.3, 4.4, zat(sx * 3.2, 0.3)), 5)
        L.tube(ck, pts, 0.15, 4)
        m.add(ck, ACCENT, "gilt", EMIT)
    g = m.group("ribbons", "sway", (0, 5.0, -2.0), 5.0, 0.1, 0.0)
    for sx in (-1, 1):
        _ribbon(m, "ribbon%d" % sx, sx * 4.5, 5.2, -2.0, 11.0, 1.5, 0.9, g, ACCENT, "silk")
    _strap(m, 5.0, gap=46)
    return m


@model
def trickster_mask():
    """Trickster Mask: a painted skull mask (teal bone) with big red-ringed eye sockets that glow, two black stripes running down through the eyes, a row of fangs, a lower jaw plate and a pink rune on the brow."""
    m = WModel("trickster_mask", "head")
    zat = _face(m, 2.6, 8.2, lambda v: 3.2 + 1.3 * v ** 0.6, bulge=1.4, tile="bone", nu=6, nv=5, thick=0.5)
    for sx in (-1, 1):
        _eye(m, "socket%d" % sx, sx * 1.9, 5.3, zat(sx * 1.9, 0.5) - 0.2, 1.35, 1.05)
        rg = Part("ring%d" % sx)
        L.torus(rg, (sx * 1.9, 5.3, zat(sx * 1.9, 0.5) - 0.22), 1.45, 0.24, 10, 4, plane="xy")
        m.add(rg, ACCENT, "gem", EMIT | HOT, uv="fit")
        bar = Part("stripe%d" % sx)
        loft(bar, "y", (sx * 3.0, 3.4, zat(sx * 3.0, 0.4) - 0.15), [(0.0, 0.7, 0.3), (5.0, 0.7, 0.3)], rect_ring(1.0, 1.0))
        m.add(bar, DARK, "metal", SHELL)
    _eye(m, "nose", 0, 3.9, zat(0, 0.3) - 0.2, 0.55, 0.8)
    for k in range(7):
        x = -2.4 + 0.8 * k
        t = Part("fang%d" % k)
        L.cone(t, (x, 3.0, zat(x, 0.1) - 0.15), (x, 0.9 if k % 2 else 1.4, zat(x, 0.1) - 0.4), 0.38, 4, mid=0.4, mid_r=0.9)
        m.add(t, TRIM, "bone", SHELL, uv="fit")
    jw = Part("jaw")
    loft(jw, "y", (0, 0.4, zat(0, 0.0) + 0.3), [(0.0, 0.5, 0.5), (1.8, 0.8, 0.7)], octagon(5.6, 1.0, 0.4))
    m.add(jw, DARK, "bone", SHELL)
    _glow(m, "rune", (0, 7.2, zat(0, 0.85) - 0.3), 0.35, 0.9, 0.2, "rune")
    _strap(m, 4.4, gap=50)
    return m


@model
def bucket_of_disguise():
    """Bucket of Disguise: a plain wooden pail worn over the head, two dark iron hoops, two round black eye holes cut in front and an open dark mouth on top."""
    m = WModel("bucket_of_disguise", "head")
    bk = Part("bucket")
    lathe(bk, "y", (0, 0, 0), [(-0.5, 5.5), (5.0, 5.75), (11.0, 6.0)], sides=14, caps=(False, False))
    m.add(bk, MAIN, "wood", SHELL)
    for nm, y0, a in (("hoop_low", 1.0, 5.6), ("hoop_high", 8.1, 5.95)):
        h = Part(nm)
        L.band(h, y0, y0 + 1.2, a, a, 0.3, 14, 0.0, 0.0, 2.0)
        m.add(h, DARK, "metal")
    inner = Part("inside")
    loft(inner, "y", (0, 10.7, 0), [(0.0, 1.0, 1.0), (0.2, 1.0, 1.0)], octagon(10.6, 10.6, 2.8))
    m.add(inner, DARK, "wood", uv="fit")
    for sx in (-1, 1):
        e = Part("hole%d" % sx)
        loft(e, "-z", (sx * 1.7, 5.0, -5.65), [(0.0, 1.0, 1.0), (0.45, 1.0, 1.0)], circle(0.85, 8))
        m.add(e, DARK, "black", uv="fit")
    return m


@model
def corpse_mask():
    """Corpse Mask: a pale cracked grey face with purple swirl horns curling off the brow, purple spiral marks on the cheeks, dark eye slits, a thin red mouth, and two long ragged ribbons that sway."""
    m = WModel("corpse_mask", "head")
    zat = _face(m, 1.2, 8.2, lambda v: 2.5 + 1.9 * math.sin(math.pi * (0.25 + 0.55 * v)), bulge=1.4, tile="bone", nu=6, nv=6, thick=0.5)
    nz = Part("nose")
    loft(nz, "y", (0, 2.9, zat(0, 0.3) - 0.2), [(0.0, 0.6, 0.8), (2.0, 0.7, 1.1), (3.0, 0.3, 0.5)], octagon(1.0, 1.2, 0.3))
    m.add(nz, MAIN, "bone", SHELL)
    for sx in (-1, 1):
        _eye(m, "slit%d" % sx, sx * 1.9, 5.3, zat(sx * 1.9, 0.6) - 0.2, 1.2, 0.5)
        _horn(m, "curl%d" % sx, L.bez3((sx * 2.3, 6.8, zat(sx * 2.3, 0.8) - 0.1), (sx * 3.4, 9.0, -5.0), (sx * 1.6, 10.6, -4.6), (sx * 0.9, 9.2, -4.4), 8), 0.5, 0.2, ACCENT, "gilt", 5)
        sp = Part("cheek%d" % sx)
        L.torus(sp, (sx * 2.9, 3.0, zat(sx * 2.9, 0.25) - 0.2), 0.9, 0.22, 8, 4, plane="xy")
        m.add(sp, ACCENT, "gilt", SHELL)
        ck = Part("crack%d" % sx)
        loft(ck, "y", (sx * 0.9, 7.8, zat(sx * 0.9, 0.9) - 0.1), [(0.0, 0.2, 0.2), (-2.0, 0.15, 0.2)], rect_ring(1.0, 1.0))
        m.add(ck, DARK, "metal")
    _eye(m, "lips", 0, 2.2, zat(0, 0.15) - 0.2, 1.1, 0.3, slot=ACCENT, tile="silk")
    g = m.group("ribbons", "sway", (0, 5.0, -1.5), 5.0, 0.1, 0.0)
    for sx in (-1, 1):
        _ribbon(m, "ribbon%d" % sx, sx * 4.4, 5.0, -2.0, 14.0, 1.2, 0.5, g, DARK, "silk", nv=5)
    return m


@model
def mask_of_the_witchdoctor():
    """Mask of the Witchdoctor: a tall carved totem of three wooden planks, a green gem on the brow, slit eyes and a bar mouth, two bone horns and a fan of green leaves above each side."""
    m = WModel("mask_of_the_witchdoctor", "head")
    pl = Part("plank")
    loft(pl, "y", (0, 0.6, -4.95), [(0.0, 0.55, 1.0), (3.0, 1.0, 1.0), (12.0, 1.0, 1.0), (13.4, 0.62, 1.0)], octagon(6.2, 1.5, 0.5))
    m.add(pl, MAIN, "wood", SHELL)
    for sx in (-1, 1):
        sd = Part("side%d" % sx)
        loft(sd, "y", (sx * 3.6, 0.8, -4.6), [(0.0, 0.5, 1.0), (2.0, 1.0, 1.0), (8.0, 1.0, 1.0), (9.4, 0.5, 1.0)], octagon(2.6, 1.4, 0.4))
        m.add(sd, TRIM, "wood", SHELL)
        _eye(m, "slit%d" % sx, sx * 1.5, 5.2, -5.75, 1.0, 0.32)
        _horn(m, "horn%d" % sx, L.bez3((sx * 4.2, 6.0, -4.3), (sx * 6.2, 5.8, -4.4), (sx * 7.4, 7.0, -4.2), (sx * 7.0, 9.0, -4.0), 6), 0.9, 0.2, ACCENT, "bone", 5)
        for k in range(3):
            lf = Part("leaf%d_%d" % (sx, k))
            a = math.radians(-20 + 32 * k)
            L.feather(lf, (sx * 2.6, 12.0, -4.6), (sx * (2.6 + 4.2 * math.sin(a) + 1.2), 12.0 + 3.8 * math.cos(a), -4.4), 0.95, 0.3, 5)
            m.add(lf, ACCENT, "feather", SHELL, uv="fit")
    _eye(m, "mouth", 0, 2.2, -5.75, 1.6, 0.3)
    _glow(m, "gem", (0, 9.4, -5.85), 0.55, 0.9, 0.3)
    _strap(m, 4.2, gap=48)
    return m


@model
def mask_of_the_raven_queens_disciple():
    """Mask of the Raven Queen's Disciple: a half mask in dark blue steel with two swept brow points, big oval eye holes, a long beak-like chin spike of silver and a purple gem on the brow that glows."""
    m = WModel("mask_of_the_raven_queens_disciple", "head")
    zat = _face(m, 3.2, 8.0, lambda v: 3.4 + 1.2 * math.sin(math.pi * (0.2 + 0.8 * v)), bulge=1.3, tile="plate", nu=6, nv=4, thick=0.45)
    for sx in (-1, 1):
        _eye(m, "hole%d" % sx, sx * 1.9, 5.5, zat(sx * 1.9, 0.5) - 0.2, 1.3, 0.95)
        w = Part("brow%d" % sx)
        L.cone(w, (sx * 3.0, 7.6, -4.6), (sx * 5.4, 9.2, -3.4), 0.9, 4, mid=0.4, mid_r=0.9)
        m.add(w, TRIM, "metal", SHELL)
        c = Part("cheek%d" % sx)
        L.cone(c, (sx * 3.2, 4.4, -4.5), (sx * 4.4, 1.4, -3.6), 0.7, 4, mid=0.4, mid_r=0.9)
        m.add(c, MAIN, "plate", SHELL)
    bk = Part("beak")
    L.cone(bk, (0, 4.0, -5.0), (0, -3.4, -6.4), 1.4, 4, mid=0.35, mid_r=0.8)
    m.add(bk, TRIM, "metal", SHELL)
    bridge = Part("bridge")
    loft(bridge, "y", (0, 3.6, zat(0, 0.2) - 0.2), [(0.0, 0.6, 0.8), (3.0, 0.5, 0.8)], octagon(1.0, 1.0, 0.25))
    m.add(bridge, MAIN, "plate")
    _glow(m, "gem", (0, 7.4, zat(0, 0.9) - 0.35), 0.5, 0.85, 0.3)
    _strap(m, 5.2, gap=55)
    return m


@model
def mask_of_mind_melting():
    """Mask of Mind Melting: a tall squid-skinned hood with a fin crest, three red eyes stacked up its brow (they glow), dark side sockets, a fanged dark maw and four long curling tentacles that sway."""
    m = WModel("mask_of_mind_melting", "head")
    hd = Part("hood")
    L.sph(hd, (0, 5.2, 0.0), 5.3, 5, 10, squash=1.2)
    m.add(hd, MAIN, "scale", SHELL)
    mw = Part("maw")
    loft(mw, "z", (0, 2.4, -5.0), [(0.0, 0.9, 1.0), (-0.6, 0.9, 1.0)], octagon(4.6, 3.2, 1.0))
    m.add(mw, DARK, "black", uv="fit")
    for k in range(4):
        x = -1.6 + 1.07 * k
        t = Part("fang%d" % k)
        L.cone(t, (x, 3.9, -5.5), (x, 2.6, -5.8), 0.38, 4, mid=0.4, mid_r=0.9)
        m.add(t, NONE, "white", SHELL, uv="fit")
    for k in range(3):
        x = -1.0 + 1.0 * k
        t = Part("lowfang%d" % k)
        L.cone(t, (x - 0.1, 0.9, -5.4), (x, 2.2, -5.6), 0.35, 4, mid=0.4, mid_r=0.9)
        m.add(t, NONE, "white", SHELL, uv="fit")
    for k, (y, z) in enumerate(((10.2, -3.3), (8.8, -4.3), (7.4, -5.0))):
        e = Part("eye%d" % k)
        L.sph(e, (0, y, z - 0.25), 0.72, 3, 8)
        m.add(e, GLOW, "gem", EMIT | HOT, uv="fit")
    for sx in (-1, 1):
        _eye(m, "socket%d" % sx, sx * 2.6, 4.9, -5.1, 1.15, 0.55)
    for k in range(3):
        f = Part("fin%d" % k)
        L.cone(f, (0, 11.0 - 0.5 * k, 0.5 + 1.6 * k), (0, 14.0 - 1.3 * k, 1.5 + 2.4 * k), 0.75, 4, mid=0.35, mid_r=0.9)
        m.add(f, ACCENT, "vein", SHELL)
    g = m.group("tentacles", "sway", (0, 2.0, -3.0), 6.0, 0.1, 0.0)
    for i, (sx, x0, z0, x1, z1) in enumerate(((-1, 2.4, -4.6, 5.0, -6.0), (1, 2.4, -4.6, 5.0, -6.0), (-1, 3.8, -1.5, 6.4, -1.0), (1, 3.8, -1.5, 6.4, -1.0))):
        _tentacle(m, "tent%d" % i, (sx * x0, 2.5, z0), (sx * x1, -1.0, z0 - 0.4), (sx * (x1 - 3.0), -5.0, z1), (sx * (x1 + 0.4), -9.0 + (i % 2) * 1.5, z1 + 1.0), 0.65, g)
    return m


@model
def frank():
    """Frank: a carved brown rabbit mask with two tall ears (one bent over, the inner ear in the second colour), round white-rimmed eye holes with a faint pale glow, a muzzle with a dark nose and a cheek ruff."""
    m = WModel("frank", "head")
    zat = _face(m, 1.2, 8.0, lambda v: 2.9 + 1.4 * math.sin(math.pi * (0.2 + 0.7 * v)), bulge=1.3, tile="wood", nu=6, nv=5, thick=0.5)
    mz = Part("muzzle")
    L.sph(mz, (0, 3.0, zat(0, 0.3) - 0.6), 1.6, 3, 8, squash=0.8)
    m.add(mz, MAIN, "wood", SHELL)
    _eye(m, "nose", 0, 3.6, zat(0, 0.3) - 1.9, 0.5, 0.35, 0.3)
    for sx in (-1, 1):
        ey = Part("eyehole%d" % sx)
        loft(ey, "-z", (sx * 2.0, 5.3, zat(sx * 2.0, 0.6) - 0.1), [(0.0, 1.0, 1.0), (0.3, 1.0, 1.0)], circle(1.15, 8))
        m.add(ey, DARK, "black", uv="fit")
        rm = Part("rim%d" % sx)
        L.torus(rm, (sx * 2.0, 5.3, zat(sx * 2.0, 0.6) - 0.2), 1.3, 0.22, 10, 4, plane="xy")
        m.add(rm, TRIM, "white", SHELL)
        _glow(m, "spark%d" % sx, (sx * 2.0, 5.3, zat(sx * 2.0, 0.6) - 0.4), 0.3, 0.3, 0.12, "soft")
        ch = Part("ruff%d" % sx)
        L.cone(ch, (sx * 3.0, 2.8, -4.6), (sx * 5.2, 2.2, -4.4), 0.8, 4, mid=0.4, mid_r=0.9)
        m.add(ch, ACCENT, "fur", SHELL)
    g = m.group("ears", "sway", (0, 8.0, -1.0), 2.5, 0.1, 0.0)
    ear = Part("ear_r")
    L.feather(ear, (-1.8, 7.5, -1.4), (-2.6, 17.5, -1.0), 1.5, 0.45, 6)
    m.add(ear, MAIN, "wood", SHELL, group=g, uv="fit")
    ei = Part("ear_r_in")
    L.feather(ei, (-1.8, 8.5, -1.9), (-2.5, 16.2, -1.4), 0.8, 0.2, 6)
    m.add(ei, ACCENT, "fur", 0, group=g, uv="fit")
    bend = Part("ear_l")
    pts = L.bez3((1.8, 7.5, -1.4), (2.4, 12.0, -1.0), (4.0, 15.0, -1.0), (6.6, 15.0, -1.2), 7)
    sc = [(1.0, 1.0), (1.0, 1.0), (0.95, 0.95), (0.85, 0.85), (0.72, 0.72), (0.6, 0.6), (0.45, 0.45), (0.0, 0.0)]
    sweep(bend, pts, L.ellipse(1.5, 0.45, 8), scales=sc, caps=(True, True))
    m.add(bend, MAIN, "wood", SHELL, group=g)
    return m


# ================================================================ helms and crowns
@model
def helmet_of_headbutting():
    """Helmet of Headbutting: a riveted iron helm with a brow band and nasal guard, two big curled ram horns with wooden bands, two bone tusks, a thick white beard hanging from the chin and a small green gem."""
    m = WModel("helmet_of_headbutting", "head")
    _cap(m, 3.0, 9.4, "plate")
    br = Part("brow")
    L.band(br, 5.0, 6.4, 4.8, 4.8, 0.55, L.RN, 0.0, 0.0, 3.6)
    m.add(br, TRIM, "gilt", SHELL)
    for k in range(7):
        th = math.radians(-150 + 50 * k) if k < 7 else 0
        x, z = L._sq(math.radians(-90 - 60 + 20 * k), 5.35, 5.35, 3.6)
        rv = Part("rivet%d" % k)
        L.dia(rv, (x, 5.7, z), 0.35, 0.35, 0.35)
        m.add(rv, TRIM, "metal", uv="fit")
    ng = Part("nasal")
    loft(ng, "y", (0, 5.2, -5.1), [(0.0, 0.7, 0.3), (-3.4, 0.55, 0.3)], octagon(1.0, 1.0, 0.25))
    m.add(ng, TRIM, "plate", SHELL)
    for sx in (-1, 1):
        _horn(m, "ram%d" % sx, L.bez3((sx * 4.4, 7.2, 0.0), (sx * 8.6, 8.8, 0.4), (sx * 9.4, 4.8, -2.0), (sx * 7.0, 4.4, -3.2), 6), 1.35, 0.35, TRIM, "horn", 6)
        bn = Part("hornband%d" % sx)
        L.torus(bn, (sx * 6.8, 8.1, 0.2), 1.35, 0.26, 8, 4, plane="yz")
        m.add(bn, DARK, "wood", SHELL)
        _horn(m, "tusk%d" % sx, L.bez(( sx * 4.8, 6.0, -2.2), (sx * 7.0, 6.6, -3.6), (sx * 7.6, 8.8, -5.0), 5), 0.7, 0.1, TRIM, "bone", 5)
        cg = Part("cheek%d" % sx)
        loft(cg, "y", (sx * 4.9, 0.8, -1.0), [(0, 0.5, 2.0), (3.0, 0.4, 2.0), (4.2, 0.2, 1.2)], rect_ring(1.0, 1.0))
        m.add(cg, TRIM, "plate", SHELL)
    bd = Part("beard")
    loft(bd, "-y", (0, 3.2, -4.7), [(0.0, 1.0, 1.0), (2.2, 0.95, 1.0), (4.6, 0.7, 0.8), (6.2, 0.15, 0.3)], octagon(6.4, 1.8, 0.7))
    m.add(bd, NONE, "white", SHELL, uv="fit")
    _glow(m, "gem", (0, 7.0, -5.35), 0.4, 0.55, 0.25)
    return m


@model
def the_mad_kings_crown():
    """The Mad King's Crown: a jagged purple helm-crown with a ring of tall blade spikes sweeping back, long cheek blades, a lit rune band round the dome and a big red diamond on the brow that glows."""
    m = WModel("the_mad_kings_crown", "head")
    _cap(m, 3.4, 9.6, "plate", MAIN)
    rb = Part("runes")
    L.band(rb, 7.2, 8.2, 4.85, 4.85, 0.25, L.RN, 0.0, 0.0, 3.4)
    m.add(rb, GLOW, "rune", EMIT, density=3.0)
    n = 7
    for k in range(n):
        th = math.radians(-90 + 360 * (k + 0.5) / n)
        x, z = 4.3 * math.cos(th), 4.3 * math.sin(th)
        h = 6.8 if k in (0, 6) else 8.4 if k in (1, 5) else 6.0
        sp = Part("blade%d" % k)
        L.cone(sp, (x, 8.8, z), (x * 1.15, 8.8 + h, z * 1.15 + 1.8 + 0.4 * abs(k - 3)), 1.15, 4, mid=0.45, mid_r=0.7)
        m.add(sp, DARK, "plate", SHELL)
    for sx in (-1, 1):
        ch = Part("cheek%d" % sx)
        loft(ch, "y", (sx * 4.75, 4.6, -0.8), [(0.0, 0.2, 2.8), (-3.0, 0.2, 2.4), (-6.0, 0.14, 1.2), (-7.4, 0.05, 0.3)], octagon(1.0, 1.0, 0.2))
        m.add(ch, MAIN, "plate", SHELL)
        bw = Part("brow%d" % sx)
        loft(bw, "x", (sx * 0.6, 7.0, -4.9), [(0.0, 0.8, 0.5), (sx * 3.6, 0.5, 0.4)], rect_ring(1.0, 1.0))
        m.add(bw, TRIM, "gilt", SHELL)
    _glow(m, "diamond", (0, 6.0, -5.2), 1.0, 2.5, 0.55)
    return m


@model
def death_knights_helmet():
    """Death Knight's Helmet: a blue-steel skull helm: round eye sockets, a nose cavity, a row of fangs, two curved horns, two spiked brow plates, long cheek blades running down past the chin and a purple gem on the brow."""
    m = WModel("death_knights_helmet", "head")
    _cap(m, 3.0, 9.6, "plate")
    zat = _face(m, 0.6, 7.6, lambda v: 3.3 + 1.1 * v ** 0.7, bulge=1.5, tile="plate", nu=6, nv=5, thick=0.45, z0=-4.5)
    for sx in (-1, 1):
        _eye(m, "socket%d" % sx, sx * 1.95, 5.0, zat(sx * 1.95, 0.7) - 0.25, 1.15, 1.0)
        _horn(m, "horn%d" % sx, L.bez3((sx * 4.2, 8.2, -0.8), (sx * 7.6, 8.6, -1.0), (sx * 8.2, 11.4, -1.4), (sx * 6.4, 13.8, -1.6), 8), 1.1, 0.2, ACCENT, "plate", 6)
        sp = Part("crest%d" % sx)
        L.cone(sp, (sx * 1.6, 8.0, -4.4), (sx * 3.0, 11.2, -3.2), 1.0, 4, mid=0.4, mid_r=0.9)
        m.add(sp, TRIM, "metal", SHELL)
        cb = Part("blade%d" % sx)
        loft(cb, "y", (sx * 4.5, 4.0, -2.0), [(0.0, 0.3, 2.2), (-3.5, 0.3, 2.0), (-6.5, 0.2, 1.2), (-8.0, 0.05, 0.3)], octagon(1.0, 1.0, 0.2))
        m.add(cb, MAIN, "plate", SHELL)
    _eye(m, "nose", 0, 3.4, zat(0, 0.45) - 0.2, 0.55, 0.85)
    for k in range(7):
        x = -2.4 + 0.8 * k
        t = Part("fang%d" % k)
        L.cone(t, (x, 2.5, zat(x, 0.2) - 0.1), (x, 0.4, zat(x, 0.2) - 0.3), 0.36, 4, mid=0.4, mid_r=0.9)
        m.add(t, TRIM, "bone", SHELL, uv="fit")
    _glow(m, "gem", (0, 8.2, -4.9), 0.55, 0.95, 0.35)
    return m


@model
def helm_of_whispers():
    """Helm of Whispers: a rust-brown spiked helm with three tall spikes, a long spike jutting out of one side, an angular visor plate ending in a chin point, and a crown of green wisps that rise and sway."""
    m = WModel("helm_of_whispers", "head")
    _cap(m, 3.0, 9.2, "plate")
    for k, (z, h) in enumerate(((-2.0, 5.0), (0.4, 6.4), (2.6, 4.6))):
        sp = Part("spike%d" % k)
        L.cone(sp, (0, 9.0, z), (0, 9.0 + h, z + 1.0), 1.0, 4, mid=0.4, mid_r=0.8)
        m.add(sp, TRIM, "horn", SHELL)
    ss = Part("sidespike")
    L.cone(ss, (-4.6, 6.4, -0.4), (-11.0, 6.8, -0.8), 1.1, 4, mid=0.35, mid_r=0.8)
    m.add(ss, TRIM, "horn", SHELL)
    for sx in (-1, 1):
        vz = Part("visor%d" % sx)
        loft(vz, "y", (sx * 3.0, 5.6, -4.3), [(0.0, 0.3, 1.6), (-3.0, 0.3, 1.5), (-5.2, 0.15, 0.7)], octagon(1.0, 1.0, 0.2))
        m.add(vz, MAIN, "plate", SHELL)
        _eye(m, "slit%d" % sx, sx * 1.7, 5.2, -5.15, 1.0, 0.3, slot=GLOW, tile="soft", flags=EMIT | HOT)
    cp = Part("chinpoint")
    L.cone(cp, (0, 2.2, -4.6), (0, -3.4, -6.0), 1.5, 4, mid=0.35, mid_r=0.8)
    m.add(cp, MAIN, "plate", SHELL)
    br = Part("brow")
    L.band(br, 6.2, 7.0, 4.8, 4.8, 0.45, L.RN, 0.0, 0.0, 3.4)
    m.add(br, TRIM, "horn", SHELL)
    g = m.group("wisps", "sway", (0, 9.0, 0.0), 6.0, 0.15, 0.0)
    for k in range(5):
        a = k * 1.3
        x0, z0 = 2.8 * math.cos(a), 2.8 * math.sin(a)
        pts = [(x0 + 0.5 * math.sin(i * 1.4 + a), 9.0 + 2.1 * i, z0 + 0.5 * math.cos(i * 1.4 + a)) for i in range(7)]
        w = Part("wisp%d" % k)
        L.tube(w, pts, 0.55, 4, [(1.0 - 0.15 * i,) * 2 for i in range(7)])
        m.add(w, GLOW, "flame", EMIT | HOT, group=g, uv="fit")
    return m


@model
def mind_stone():
    """Mind Stone: a pale silver Greek helm with nasal and cheek plates, wreathed by a ring of lime leaves on the crown, a few leaves drifting above and a stone set in the brow that glows."""
    m = WModel("mind_stone", "head")
    _cap(m, 2.8, 9.0, "metal")
    for sx in (-1, 1):
        ch = Part("cheek%d" % sx)
        loft(ch, "y", (sx * 4.75, 3.6, -0.3), [(0.0, 0.2, 2.6), (-3.8, 0.2, 2.2), (-4.6, 0.1, 1.2)], octagon(1.0, 1.0, 0.2))
        m.add(ch, MAIN, "plate", SHELL)
    zat = _face(m, 1.8, 6.4, lambda v: 3.3 + 0.9 * v, bulge=1.0, tile="metal", nu=6, nv=4, thick=0.4, z0=-4.4)
    for sx in (-1, 1):
        _eye(m, "eyehole%d" % sx, sx * 1.9, 4.8, zat(sx * 1.9, 0.6) - 0.2, 1.2, 0.8)
    ng = Part("nasal")
    loft(ng, "y", (0, 6.2, zat(0, 0.9) - 0.4), [(0.0, 0.7, 0.3), (-4.2, 0.55, 0.3)], octagon(1.0, 1.0, 0.25))
    m.add(ng, TRIM, "gilt", SHELL)
    ar = Part("arch")
    L.band(ar, 6.0, 6.7, 4.8, 4.8, 0.3, L.RN, 0.0, 0.0, 3.4)
    m.add(ar, TRIM, "gilt", SHELL)
    n = 11
    for k in range(n):
        th = 2 * math.pi * k / n
        x, z = 4.4 * math.cos(th), 4.4 * math.sin(th)
        lf = Part("leaf%d" % k)
        L.feather(lf, (x, 8.4, z), (x * 1.25, 8.4 + 3.4 + 0.5 * (k % 2), z * 1.25), 0.9, 0.3, 4)
        m.add(lf, ACCENT, "feather", SHELL, uv="fit")
    g = m.group("drift", "bob", (0, 12.0, 0.0), 0.6, 0.8, 0.0)
    for k, (x, y, z) in enumerate(((-3.2, 13.2, -1.0), (2.6, 14.4, 1.2), (0.4, 12.4, 3.2))):
        lf = Part("float%d" % k)
        L.feather(lf, (x, y, z), (x + 1.0, y + 3.0, z), 0.8, 0.28, 4)
        m.add(lf, ACCENT, "feather", SHELL, group=g, uv="fit")
    _glow(m, "stone", (0, 7.4, -5.2), 0.5, 0.8, 0.3)
    return m


@model
def the_orb_of_ancient_sight():
    """The Orb of Ancient Sight: a tan crown with four triangle points and skull-and-horn badges on its band, and above it a floating red orb with an eye in it, ringed by rays, that bobs."""
    m = WModel("the_orb_of_ancient_sight", "head")
    bd = Part("band")
    L.band(bd, 5.6, 8.6, 4.55, 4.55, 0.55, L.RN, 0.1, 0.25, 3.0)
    m.add(bd, MAIN, "leather", SHELL)
    for nm, y in (("trim_low", 5.6), ("trim_high", 8.4)):
        tr = Part(nm)
        L.band(tr, y, y + 0.5, 4.6, 4.6, 0.7, L.RN, 0.0, 0.25, 3.0)
        m.add(tr, TRIM, "gilt")
    for i, (x, z) in enumerate(((0.0, -4.9), (4.9, 0.0), (-4.9, 0.0), (0.0, 4.9))):
        sp = Part("point%d" % i)
        L.cone(sp, (x, 8.4, z), (x, 11.6, z), 1.3, 4, mid=0.4, mid_r=0.75)
        m.add(sp, MAIN, "leather", SHELL)
    for i, (x, z, ang) in enumerate(((0.0, -5.5, 0), (4.5, -3.2, 1), (-4.5, -3.2, 1))):
        sk = Part("skull%d" % i)
        L.sph(sk, (x, 7.0, z), 0.9, 3, 6, squash=1.1)
        m.add(sk, TRIM, "bone", uv="fit")
        for sx in (-1, 1):
            hn = Part("skhorn%d_%d" % (i, sx))
            L.cone(hn, (x + sx * 0.7, 7.5, z), (x + sx * 1.7, 8.2, z), 0.3, 4, mid=0.4, mid_r=0.9)
            m.add(hn, TRIM, "bone", uv="fit")
    g = m.group("orb", "bob", (0, 16.0, 0.0), 1.0, 0.7, 0.0)
    orb = Part("orb")
    L.sph(orb, (0, 16.0, 0.0), 2.7, 4, 10)
    m.add(orb, GLOW, "soft", EMIT | HOT, group=g, uv="fit")
    eye = Part("eye")
    L.dia(eye, (0, 16.0, -2.5), 2.1, 1.0, 0.3)
    m.add(eye, ACCENT, "gem", EMIT, group=g, uv="fit")
    pu = Part("pupil")
    L.dia(pu, (0, 16.0, -2.85), 0.3, 1.1, 0.2)
    m.add(pu, DARK, "black", 0, group=g, uv="fit")
    for k in range(8):
        a = 2 * math.pi * k / 8
        rv = Part("ray%d" % k)
        L.dia(rv, (3.9 * math.cos(a), 16.0 + 3.9 * math.sin(a), 0.0), 0.2 + 0.25 * abs(math.cos(a)), 0.2 + 0.25 * abs(math.sin(a)), 0.2)
        m.add(rv, GLOW, "spark", EMIT, group=g, uv="fit")
    return m


@model
def jellyfish_cap():
    """Jellyfish Cap: a pale translucent bell dome on the head with a scalloped rim and a glowing crown gem, a bright inner bell, and six long oral arms and frills dangling round the face that sway."""
    m = WModel("jellyfish_cap", "head")
    bell = Part("bell")
    L.sph(bell, (0, 7.4, 0.0), 5.6, 4, 10, squash=0.85)
    m.add(bell, MAIN, "pearl", SHELL)
    core = Part("inner")
    L.sph(core, (0, 8.4, 0.0), 3.0, 3, 8, squash=0.8)
    m.add(core, GLOW, "soft", EMIT, uv="fit")
    rim = Part("rim")
    L.torus(rim, (0, 6.5, 0.0), 5.3, 0.5, 10, 4, plane="xz", a_scale=1.0, b_scale=1.0)
    m.add(rim, TRIM, "pearl", SHELL)
    for k in range(6):
        a = 2 * math.pi * k / 6 + 0.3
        s = Part("scallop%d" % k)
        L.dia(s, (5.2 * math.cos(a), 6.2, 5.2 * math.sin(a)), 0.9, 0.7, 0.9)
        m.add(s, TRIM, "pearl", SHELL, uv="fit")
    _glow(m, "crown_gem", (0, 12.6, 0.0), 0.7, 0.8, 0.7, "gem")
    g = m.group("arms", "sway", (0, 6.5, 0.0), 6.0, 0.15, 0.0)
    for k in range(6):
        a = 2 * math.pi * k / 6 + 0.3
        x, z = 5.0 * math.cos(a), 5.0 * math.sin(a)
        _tentacle(m, "arm%d" % k, (x, 6.4, z), (x * 1.15, 2.0, z * 1.15), (x * 0.9, -3.0, z * 0.9), (x * 1.2 + 0.6, -7.0 - 2.0 * (k % 2), z * 1.2), 0.5, g, ACCENT, "pearl")
    return m


# ================================================================ eyes, lenses, goggles
@model
def soul_searchers():
    """Soul Searchers: round wire-rimmed sunglasses with stained-glass lenses that glow in many facets, a double brow bar, thin temple arms hooking behind the ears and a small hanging cross charm."""
    m = WModel("soul_searchers", "head")
    for sx in (-1, 1):
        rg = Part("rim%d" % sx)
        L.torus(rg, (sx * 2.1, 4.9, -4.95), 1.75, 0.28, 10, 4, plane="xy")
        m.add(rg, TRIM, "gilt", SHELL)
        ln = Part("lens%d" % sx)
        loft(ln, "-z", (sx * 2.1, 4.9, -4.9), [(0.0, 1.0, 1.0), (0.3, 1.0, 1.0)], circle(1.7, 10))
        m.add(ln, GLOW, "crystal", EMIT | HOT, uv="fit")
        for k in range(3):
            fc = Part("facet%d_%d" % (sx, k))
            a = 2.1 * k
            L.dia(fc, (sx * 2.1 + 0.7 * math.cos(a), 4.9 + 0.7 * math.sin(a), -5.3), 0.6, 0.6, 0.15)
            m.add(fc, ACCENT, "crystal", EMIT, uv="fit")
        arm = Part("arm%d" % sx)
        L.tube(arm, [(sx * 3.8, 5.3, -4.9), (sx * 4.5, 5.2, -3.0), (sx * 4.5, 5.0, -0.4), (sx * 4.5, 3.6, 0.9)], 0.22, 4)
        m.add(arm, DARK, "metal")
        tp = Part("tip%d" % sx)
        L.sph(tp, (sx * 4.5, 3.4, 1.0), 0.45, 2, 6)
        m.add(tp, DARK, "leather", uv="fit")
    br = Part("bridge")
    L.tube(br, [(-0.4, 5.4, -4.95), (0.0, 5.7, -5.0), (0.4, 5.4, -4.95)], 0.2, 4)
    m.add(br, TRIM, "gilt")
    bar = Part("browbar")
    L.tube(bar, [(-3.6, 6.9, -4.9), (-1.2, 7.1, -5.05), (1.2, 7.1, -5.05), (3.6, 6.9, -4.9)], 0.22, 4)
    m.add(bar, DARK, "metal")
    g = m.group("charm", "swing", (4.5, 4.6, -3.2), 6.0, 0.8, 0.0)
    ch = Part("cross")
    loft(ch, "y", (4.7, 4.4, -3.3), [(0.0, 0.7, 0.7), (-1.6, 0.7, 0.7)], rect_ring(0.35, 0.35))
    m.add(ch, TRIM, "gilt", group=g, uv="fit")
    cb = Part("crossbar")
    loft(cb, "x", (4.3, 3.7, -3.3), [(0.0, 0.7, 0.7), (0.8, 0.7, 0.7)], rect_ring(0.35, 0.35))
    m.add(cb, TRIM, "gilt", group=g, uv="fit")
    return m


@model
def lens_of_straight_creepin():
    """Lens of Straight Creepin': a big red monocle (glowing lens in a dark rim) over one eye, a thin wire hooked over the ear, and a long bead chain that sways down from the rim ending in a small engraved cap."""
    m = WModel("lens_of_straight_creepin", "head")
    ln = Part("lens")
    loft(ln, "-z", (-2.0, 4.8, -4.9), [(0.0, 1.0, 1.0), (0.5, 1.0, 1.0)], circle(2.15, 12))
    m.add(ln, GLOW, "soft", EMIT | HOT, uv="fit")
    rg = Part("rim")
    L.torus(rg, (-2.0, 4.8, -5.05), 2.25, 0.3, 12, 4, plane="xy")
    m.add(rg, DARK, "metal", SHELL)
    hk = Part("hook")
    L.tube(hk, [(-2.4, 7.0, -5.0), (-4.5, 8.6, -3.4), (-4.7, 6.2, 0.4), (-4.6, 3.6, 1.2)], 0.2, 4)
    m.add(hk, DARK, "metal")
    g = m.group("chain", "sway", (-2.0, 2.6, -5.0), 5.0, 0.1, 0.0)
    ch = Part("chain")
    L.beads(ch, L.bez3((-2.0, 2.5, -5.1), (-4.5, -1.0, -5.5), (0.0, -4.0, -5.6), (-3.2, -8.5, -5.4), 11), 0.28, 4, every=2, bead=1.0, thin=0.5)
    m.add(ch, DARK, "chain", group=g)
    cp = Part("cap")
    loft(cp, "-z", (-3.2, -8.8, -5.4), [(0.0, 1.0, 1.0), (0.6, 1.0, 1.0)], circle(1.1, 8))
    m.add(cp, DARK, "metal", group=g, uv="fit")
    _glow(m, "engrave", (-3.2, -8.8, -6.1), 0.35, 0.35, 0.1, "soft")
    return m


@model
def goggles_of_dwarvenkin():
    """Goggles of Dwarvenkin: a copper-framed pair of goggles with two tilted diamond amber lenses that glow, a cream strap with a buckle round the head, side blocks and white fur tufts over the lenses."""
    m = WModel("goggles_of_dwarvenkin", "head")
    for sx in (-1, 1):
        fr = Part("frame%d" % sx)
        L.dia(fr, (sx * 2.2, 4.8, -4.95), 2.55, 2.55, 0.5)
        m.add(fr, TRIM, "gilt", SHELL, uv="fit")
        ln = Part("lens%d" % sx)
        L.dia(ln, (sx * 2.2, 4.8, -5.2), 1.95, 1.95, 0.4)
        m.add(ln, GLOW, "crystal", EMIT | HOT, uv="fit")
        sb = Part("block%d" % sx)
        loft(sb, "y", (sx * 4.4, 3.4, -3.7), [(0.0, 1.0, 1.0), (3.0, 1.0, 1.0)], octagon(1.4, 2.4, 0.4))
        m.add(sb, TRIM, "gilt", SHELL)
        for k in range(4):
            t = Part("tuft%d_%d" % (sx, k))
            L.feather(t, (sx * (0.8 + 1.1 * k), 7.1, -4.9), (sx * (1.4 + 1.5 * k), 8.7 + 0.3 * (k % 2), -4.6), 0.5, 0.2, 3, curl=0.2 * sx)
            m.add(t, NONE, "white", 0, uv="fit")
    br = Part("bridge")
    loft(br, "x", (-1.2, 4.8, -4.9), [(0.0, 0.7, 0.5), (2.4, 0.7, 0.5)], rect_ring(1.0, 1.0))
    m.add(br, TRIM, "gilt")
    sp = Part("strap")
    L.band(sp, 4.2, 5.8, 4.4, 4.4, 0.35, L.RN, 0.0, 0.0, 3.4)
    m.add(sp, MAIN, "leather")
    bk = Part("buckle")
    L.torus(bk, (4.6, 5.0, -1.6), 0.9, 0.22, 8, 4, plane="yz", a_scale=1.0, b_scale=0.8)
    m.add(bk, TRIM, "gilt", SHELL)
    return m


@model
def eye_of_vecna():
    """Eye of Vecna: a brass monocle plate on the right of the face holding a bare eyeball with a pink glowing iris and a dark pupil, a red optic nerve that trails down and sways, and a gilt brow plate set with three ruby diamonds."""
    m = WModel("eye_of_vecna", "head")
    eb = Part("eyeball")
    L.sph(eb, (-2.0, 4.8, -5.3), 1.75, 4, 10)
    m.add(eb, NONE, "white", SHELL, uv="fit")
    bz = Part("bezel")
    L.torus(bz, (-2.0, 4.8, -5.0), 2.1, 0.5, 12, 4, plane="xy")
    m.add(bz, TRIM, "gilt", SHELL)
    ir = Part("iris")
    loft(ir, "-z", (-2.0, 4.8, -7.0), [(0.0, 1.0, 1.0), (0.18, 1.0, 1.0)], circle(1.1, 10))
    m.add(ir, GLOW, "soft", EMIT | HOT, uv="fit")
    pu = Part("pupil")
    loft(pu, "-z", (-2.0, 4.8, -7.15), [(0.0, 1.0, 1.0), (0.12, 1.0, 1.0)], circle(0.45, 8))
    m.add(pu, DARK, "black", uv="fit")
    g = m.group("nerve", "sway", (-2.0, 3.0, -5.0), 6.0, 0.1, 0.0)
    _tentacle(m, "nerve", (-2.0, 3.0, -5.6), (-3.0, 0.0, -5.8), (-1.0, -3.0, -5.6), (-2.4, -7.0, -5.3), 0.45, g, ACCENT, "vein", 5, 7)
    pl = Part("brow")
    loft(pl, "y", (0, 6.5, -4.55), [(0.0, 1.0, 1.0), (1.5, 1.0, 1.0)], octagon(8.8, 1.2, 0.5))
    m.add(pl, TRIM, "gilt", SHELL)
    for k, x in enumerate((-2.6, 0.0, 2.6)):
        rb = Part("ruby%d" % k)
        L.dia(rb, (x, 7.25, -5.3), 0.75, 0.9 if k == 1 else 0.7, 0.35)
        m.add(rb, GLOW, "gem", EMIT | HOT, uv="fit")
    sd = Part("link")
    loft(sd, "y", (-3.6, 6.5, -4.5), [(0.0, 1.0, 1.0), (-1.2, 1.0, 1.0)], octagon(1.0, 1.0, 0.25))
    m.add(sd, TRIM, "gilt")
    bd = Part("band")
    L.band(bd, 6.4, 7.8, 4.45, 4.45, 0.3, L.RN, 0.0, 0.0, 3.4)
    m.add(bd, DARK, "leather")
    return m

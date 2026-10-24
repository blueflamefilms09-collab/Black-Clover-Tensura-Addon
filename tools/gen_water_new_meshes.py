#!/usr/bin/env python3
"""
WATER remake, direction C "real 3D toon meshes", PHASE A = PREVIEW ONLY (nothing is registered in the game, nothing goes to assets/).

The pieces of the Sea Dragon spells as REAL geometry (thickness, a front and a back, hard facets), authored in the repo's own mesh vocabulary: the `Mesh` class of
tools/gen_sdw3_models.py (the same `q` quad lines, loft / box / ring, the same file format read by SpiritDiveWind3Mesh):

  q <layer> (x y z u v) x 4  nx ny nz  r g b
  layers here:  0 water body (toon shaded: 3 tones by the light + hard specular + rim)   1 emissive core / glow (drawn additive)   2 foam and teeth (toon shaded, white palette)

Units are model pixels (1/16 block); y runs along the piece (snout / tip at +y), x across it, z through it. Every piece comes in a hi and a lo version (`_lo`: fewer sides / segments) for the LOD.
The toon shading is done per quad on the CPU (one tone lookup from the quad normal, like the face shading of the existing mesh pieces), no shader, no texture beyond a solid atlas cell.

    python tools/gen_water_new_meshes.py     ->  docs/previews/vfx_remake/water_new/meshes/<name>.txt  +  counts.txt   (the numbers the budgets use)
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_sdw3_models import Mesh, add, mul, norm, cross, sub, mean, dot   # noqa: E402  (the repo's own mesh vocabulary)

ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "water_new", "meshes")
W = (255, 255, 255)
UV = (0, 0, 1, 1)


def sec(y, a, b, cx=0.0, cz=0.0, n=6, rot=0.0):
    """an elliptical section at height y: half width a (x), half height b (z), n points"""
    return [(cx + a * math.cos(rot + 2 * math.pi * i / n), y, cz + b * math.sin(rot + 2 * math.pi * i / n)) for i in range(n)]


def pt(y, cx=0.0, cz=0.0, n=6):
    return [(cx, y, cz)] * n


def tri(m, layer, a, b, c, rgb=W, ref=None):
    m.quad(layer, [a, b, c, c], [(0, 0), (1, 0), (0.5, 1), (0.5, 1)], rgb, ref=ref)


def blade(m, layer, pts, rgb=W):
    """a flat double-sided blade (one quad, or a triangle when two points repeat); no ref: the normal is whatever the winding gives, the drawer lights both sides"""
    m.quad(layer, pts, [(0, 0), (1, 0), (1, 1), (0, 1)], rgb)


def loft(m, layer, secs, rgb=W):
    m.loft(layer, secs, rgb, UV, caps=False)


# =================================================================================================== dragon
def dragon_head_upper(n=8, lo=False):
    m = Mesh("dragon_head_upper" + ("_lo" if lo else ""))
    S = [(-8, 7.5, 8.5, 0), (6, 9.0, 9.5, 1.2), (18, 7.4, 7.0, 1.2), (30, 5.4, 4.8, 0.2), (40, 3.6, 3.0, -1.0)]
    if lo:
        S = [S[0], S[1], S[3], S[4]]
        n = 6
    secs = [sec(y, a, b, 0, cz, n) for (y, a, b, cz) in S] + [pt(44, 0, -1.4, n)]
    loft(m, 0, secs)
    # two swept-back horns (four-sided pyramids bent back), dorsal fins down the neck
    for s in (-1, 1):
        base = [(s * 6.0 + dx, -4 + dy, 8.5) for dx, dy in ((-1.5, -1.5), (1.5, -1.5), (1.5, 1.5), (-1.5, 1.5))]
        tip = [(s * 13.0, -26, 15.0)] * 4
        loft(m, 0, [base, tip])
    for i, y in enumerate((-2, -9) if lo else (-2, -9, -16)):
        h = 9 - i * 1.6
        blade(m, 0, [(0, y + 4, 9), (0, y - 3, 9), (0, y - 5.5, 9 + h), (0, y + 1.5, 9 + h * 0.4)])
    # teeth: flat fangs along both sides of the upper jaw (layer 2, white), pointing down (-z)
    nt = 2 if lo else 3
    for s in (-1, 1):
        for i in range(nt):
            y = 16 + i * (22 / nt) + 3
            x = s * (4.6 - 0.07 * (y - 10))
            blade(m, 2, [(x, y - 2.2, -2.2), (x, y + 2.2, -2.2), (x, y + 1.0, -9.5 + 0.6 * i), (x, y - 0.6, -9.5 + 0.6 * i)])
    # the eye: a small emissive plate on each side (layer 1)
    for s in (-1, 1):
        blade(m, 1, [(s * 8.6, 7.5, 5.0), (s * 9.0, 12.5, 4.2), (s * 8.4, 12.5, 2.0), (s * 8.2, 7.5, 2.4)])
    # a glowing crest line along the top of the skull (layer 1)
    blade(m, 1, [(-0.8, -6, 9.2), (0.8, -6, 9.2), (0.6, 30, 5.2), (-0.6, 30, 5.2)])
    return m


def dragon_head_lower(n=6, lo=False):
    """the lower jaw, pivot (hinge) at the origin; it hangs along +y and is turned down by the drawer"""
    m = Mesh("dragon_head_lower" + ("_lo" if lo else ""))
    S = [(0, 6.2, 4.6, 0), (12, 5.6, 4.0, 0.2), (24, 4.4, 3.2, 0.6), (36, 3.0, 2.2, 1.0)]
    if lo:
        S = [S[0], S[2], S[3]]
        n = 4
    secs = [sec(y, a, b, 0, cz, n) for (y, a, b, cz) in S] + [pt(40, 0, 1.2, n)]
    loft(m, 0, secs)
    nt = 2 if lo else 3
    for s in (-1, 1):
        for i in range(nt):
            y = 12 + i * (22 / nt) + 3
            x = s * (3.9 - 0.06 * (y - 10))
            blade(m, 2, [(x, y - 2.2, 2.5), (x, y + 2.2, 2.5), (x, y + 1.0, 9.5 - 0.6 * i), (x, y - 0.6, 9.5 - 0.6 * i)])
    return m


def _seg(name, banded, lo):
    n = 4 if lo else 6
    m = Mesh(name + ("_lo" if lo else ""))
    loft(m, 0, [sec(-7, 8.0, 7.0, 0, 0, n), sec(7, 7.2, 6.2, 0, 0, n)])
    blade(m, 0, [(0, -7, 6.6), (0, 7, 5.9), (0, 3, 13.5), (0, -3, 12.0)])
    if banded:                                                           # a white foam band standing out of the middle
        loft(m, 2, [sec(-1.2, 8.7, 7.7, 0, 0, n), sec(1.2, 8.7, 7.7, 0, 0, n)])
    blade(m, 1, [(-1.0, -7, 7.1), (1.0, -7, 7.1), (0.9, 7, 6.4), (-0.9, 7, 6.4)])      # glowing spine line (layer 1)
    return m


def dragon_seg(n=5, lo=False):
    """a plain body piece of the water dragon: a short tapered tube, a dorsal fin, a glowing spine line"""
    return _seg("dragon_seg", False, lo)


def dragon_seg_b(n=5, lo=False):
    """the same with a white foam band (alternate pieces carry it: the banded body of the Roar still)"""
    return _seg("dragon_seg_b", True, lo)


def dragon_tail(n=5, lo=False):
    m = Mesh("dragon_tail" + ("_lo" if lo else ""))
    n = 4 if lo else 6
    loft(m, 0, [sec(0, 7, 6, 0, 0, n), sec(16, 4.4, 3.6, 0, 0, n), pt(34, 0, 0, n)])
    for s in (-1, 1):
        blade(m, 0, [(0, 14, 0), (s * 11, 24, 0), (s * 6, 34, 0), (0, 22, 0)])
    return m


# =================================================================================================== generic pieces
def gem(lo=False):
    """a droplet drawn as a stretched octahedron: tip at +y"""
    m = Mesh("gem" + ("_lo" if lo else ""))
    r = 1.0
    ring = [(r, 0, 0), (0, 0, r), (-r, 0, 0), (0, 0, -r)]
    top, bot = (0, 2.0, 0), (0, -1.0, 0)
    for i in range(4):
        tri(m, 0, ring[i], ring[(i + 1) % 4], top, ref=(0, 0.2, 0))
        tri(m, 0, ring[(i + 1) % 4], ring[i], bot, ref=(0, 0.2, 0))
    return m


def orb(lo=False, n=None, rings=None):
    """a sphere / bubble of radius 1 (loft of rings, poles on y)"""
    n = n or (6 if lo else 8)
    rings = rings or (3 if lo else 5)
    m = Mesh("orb" + ("_lo" if lo else ""))
    secs = [pt(-1.0, 0, 0, n)]
    for i in range(1, rings + 1):
        a = -math.pi / 2 + math.pi * i / (rings + 1)
        secs.append(sec(math.sin(a), math.cos(a), math.cos(a), 0, 0, n))
    secs.append(pt(1.0, 0, 0, n))
    loft(m, 0, secs)
    return m


def orb_xs():
    """a small bubble: 5 sides, 2 rings (15 quads)"""
    m = orb(True, n=5, rings=2)
    m.name = "orb_xs"
    return m


def sphere(lo=False):
    m = orb(lo, n=8 if lo else 10, rings=5 if lo else 7)
    m.name = "sphere" + ("_lo" if lo else "")
    return m


def ripple(lo=False, flecks=True):
    """a ridged ring on the ground (radius 16, ridge 2.4 high, 4 wide each side), n segments of two quads; some foam flecks on the crest"""
    n = 14 if lo else 24
    m = Mesh("ripple" + ("_lo" if lo else ""))
    R, w, h = 16.0, 0.9, 1.7
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n
        P = lambda a, r, y: (r * math.cos(a), y, r * math.sin(a))
        lay = 2 if (flecks and i % 4 == 0) else 0
        m.quad(lay, [P(a0, R - w, 0), P(a1, R - w, 0), P(a1, R, h), P(a0, R, h)], [(0, 0), (1, 0), (1, 1), (0, 1)], W, ref=(0, -2, 0) if False else None)
        m.quad(lay, [P(a0, R, h), P(a1, R, h), P(a1, R + w, 0), P(a0, R + w, 0)], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
    return m


def horn(lo=False):
    """a splash-crown horn / thrown wave crest: a bent, tapering pyramid, curling outward (+x) and up (+y)"""
    m = Mesh("horn" + ("_lo" if lo else ""))
    P = [((0, 0, 0), 4.4), ((5.0, 13, 0), 3.0), ((14.0, 25, 0), 1.0)]
    if lo:
        P = [P[0], P[2]]
    secs = []
    for (c, r) in P:
        secs.append([(c[0] - r, c[1], c[2]), (c[0], c[1], c[2] + r * 0.8), (c[0] + r, c[1], c[2]), (c[0], c[1], c[2] - r * 0.8)])
    secs.append([(P[-1][0][0] + 3.6, P[-1][0][1] + 4.0, 0)] * 4)
    loft(m, 0, secs)
    return m


def wisp(lo=False):
    """a flat curved water ribbon (double sided), 40 long along +y, curling in +x"""
    m = Mesh("wisp" + ("_lo" if lo else ""))
    n = 4 if lo else 7
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        def c(t):
            return (3.5 * math.sin(t * 3.0), -20 + 40 * t, 0), 2.6 * (1 - t) + 0.3
        (p0, w0), (p1, w1) = c(t0), c(t1)
        blade(m, 0, [(p0[0] - w0, p0[1], 0), (p1[0] - w1, p1[1], 0), (p1[0] + w1, p1[1], 0), (p0[0] + w0, p0[1], 0)])
    return m


def lance(lo=False):
    """Aqua Javelin: a long six-sided spear of water, a wide leaf head, two barbs, four tail fins, a glowing core (tip +y)"""
    n = 6
    m = Mesh("lance" + ("_lo" if lo else ""))
    secs = [sec(-34, 1.4, 1.4, n=n), sec(-6, 2.2, 2.2, n=n), sec(12, 6.0, 3.4, n=n), sec(30, 3.0, 2.0, n=n), pt(50, n=n)]
    if lo:
        secs = [secs[0], secs[2], secs[3], secs[4]]
    loft(m, 0, secs)
    for s in (-1, 1):
        blade(m, 0, [(s * 5.5, 10, 0), (s * 11, 4, 0), (s * 3, 6, 0), (s * 3, 6, 0)])
    if not lo:
        for ang in (0, 90, 180, 270):
            a = math.radians(ang)
            ca, sa = math.cos(a), math.sin(a)
            blade(m, 0, [(0, -34, 0), (0, -14, 0), (ca * 7, -26, sa * 7), (ca * 9, -34, sa * 9)])
    # core: a thin emissive spindle along the axis
    loft(m, 1, [sec(-30, 0.5, 0.5, n=4), sec(8, 1.5, 1.5, n=4), pt(46, n=4)])
    return m


def dome(lo=False):
    """Nest of the Sea Dragon: a hemisphere of radius 16 with three spiral ridges winding up it (they turn when the drawer turns the piece) and a foam collar"""
    n = 8 if lo else 12
    rings = 3 if lo else 4
    m = Mesh("dome" + ("_lo" if lo else ""))
    secs = []
    for i in range(rings + 1):
        a = (math.pi / 2) * i / (rings + 1) * (rings + 1) / (rings + 0.0) * 0.999 if False else (math.pi / 2) * i / (rings + 1)
        secs.append(sec(16 * math.sin(a), 16 * math.cos(a), 16 * math.cos(a), 0, 0, n))
    secs.append(pt(16, 0, 0, n))
    loft(m, 0, secs)
    # spiral ridges (layer 2, white foam): raised two-quad ridges following a spiral on the surface
    steps = 6 if lo else 10
    for k in range(3):
        base = 2 * math.pi * k / 3
        prev = None
        for i in range(steps + 1):
            t = i / steps
            lat = math.radians(8 + 72 * t)
            lon = base + t * 2.4 * math.pi * (1 if True else -1)
            wd = (3.0 - 1.8 * t) / 16.0
            def P(lo_, la, r):
                return (r * math.cos(la) * math.cos(lo_), r * math.sin(la), r * math.cos(la) * math.sin(lo_))
            cur = (P(lon - wd, lat, 16.0), P(lon, lat, 17.6), P(lon + wd, lat, 16.0))
            if prev is not None:
                m.quad(2, [prev[0], prev[1], cur[1], cur[0]], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
                if not lo:
                    m.quad(2, [prev[1], prev[2], cur[2], cur[1]], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
            prev = cur
    return m


def feather(lo=False):
    """a long water feather (a Valkyrie wing blade): a tapered four-sided blade bent a little; tip +y, thickness 2"""
    m = Mesh("feather" + ("_lo" if lo else ""))
    P = [((0, 0, 0), (3.2, 1.0)), ((1.2, 14, 0), (4.0, 1.2)), ((3.6, 28, 0), (3.0, 1.0)), ((7.5, 40, 0), (1.4, 0.6))]
    if lo:
        P = [P[0], P[2], P[3]]
    secs = []
    for (c, (wx, wz)) in P:
        secs.append([(c[0] - wx, c[1], c[2]), (c[0], c[1], c[2] + wz), (c[0] + wx, c[1], c[2]), (c[0], c[1], c[2] - wz)])
    secs.append([(P[-1][0][0] + 2.5, P[-1][0][1] + 6, 0)] * 4)
    loft(m, 0, secs)
    loft(m, 1, [[(c[0] - wx * 0.35, c[1], c[2]), (c[0], c[1], c[2] + wz * 0.5), (c[0] + wx * 0.35, c[1], c[2]), (c[0], c[1], c[2] - wz * 0.5)] for (c, (wx, wz)) in P[:3]])
    return m


MESHES = [dragon_head_upper, dragon_head_lower, dragon_seg, dragon_seg_b, dragon_tail, gem, orb, orb_xs, sphere, ripple, horn, wisp, lance, dome, feather]


def main():
    os.makedirs(OUT, exist_ok=True)
    lines = ["Water remake, direction C: mesh pieces (quads; verts = 4 x quads; layer 0 body / 1 glow / 2 foam)", ""]
    lines.append("%-24s %6s %6s %6s %6s" % ("piece", "quads", "L0", "L1", "L2"))
    for fn in MESHES:
        for lo in (False, True):
            m = fn(lo=lo) if "lo" in fn.__code__.co_varnames else fn()
            c = [0, 0, 0]
            for q in m.q:
                c[q[0]] += 1
            with open(os.path.join(OUT, m.name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
                f.write(m.text())
            lines.append("%-24s %6d %6d %6d %6d" % (m.name, len(m.q), c[0], c[1], c[2]))
            if "lo" not in fn.__code__.co_varnames:
                break
    with open(os.path.join(OUT, "counts.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()

#!/usr/bin/env python3
"""
WATER remake, hybrid "BC" = PREVIEW ONLY (nothing is registered in the game, nothing goes to assets/).

The solid cores of the hybrid: the C pieces of tools/gen_water_new_meshes.py re-cut for a core that is WRAPPED in glass (the old pieces stay, these are new files with the prefix `bc_`).
What is different from the C pieces (the weak spots listed in docs/vfx_remake/water_new.md section 10):

  * seam      the dragon skull has a round occiput and a NECK that runs back into the first body piece (no flat box back); a thin collar of foam covers the join
  * two-sided every flat blade (fangs, fins, tail fins, barbs) is written as a front AND a back quad, so the pieces do not depend on the render type's culling; closed spindles (feathers) need no back faces
  * dome      the spiral ridges are flat foam bands lying on the hull (one quad per step, no tent), wide enough to read, plus a foam collar at the base
  * feathers  a Valkyrie wing is one piece: an arm bone and six feather spindles fanned along it (not one root with a star of blades); a left and a right file
  * horns     broad, rounded water tongues (thick radially, wide sideways, five sections) instead of flat spikes
  * ripples   a low, thin ridge (the old one read as a concrete kerb)
  * hulls     bc_head_hull / bc_sphere: slightly larger translucent copies (glass palette, fresnel alpha) that wrap the solid core

Same file format and layers as the C pieces: q <layer> (x y z u v) x4 nx ny nz r g b ;  layer 0 body, 1 emissive, 2 foam.  Units are 1/16 block, y along the piece.

    python tools/gen_water_bc_meshes.py   ->  docs/previews/vfx_remake/water_new/meshes/bc_*.txt  +  counts_bc.txt
                                          ->  src/main/resources/assets/nusmp/wbc_meshes/bc_*.txt   (the game set, read by vfx/WaterBcMeshes; the two Valkyrie wing files are NOT shipped: the wings are deferred)
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_sdw3_models import Mesh                                                    # noqa: E402
from gen_water_new_meshes import sec, pt, W, loft, OUT                              # noqa: E402  (same vocabulary as C)

GAME = os.path.join(os.path.abspath(os.path.join(HERE, "..")), "src", "main", "resources", "assets", "nusmp", "wbc_meshes")
NOT_SHIPPED = ("wing_r", "wing_l")                                                  # the Valkyrie wings are remade separately (owner decision), so no wing mesh goes into the game


def blade2(m, layer, pts, rgb=W):
    """a flat blade as a front quad AND a back quad (the same four points, reversed winding)"""
    uv = [(0, 0), (1, 0), (1, 1), (0, 1)]
    m.quad(layer, pts, uv, rgb)
    m.quad(layer, [pts[0], pts[3], pts[2], pts[1]], uv, rgb)


def ring_fin(m, layer, y0, y1, h, z0, rgb=W):
    """a dorsal fin standing on the back (z0 = skin height): swept back, two-sided"""
    blade2(m, layer, [(0, y1, z0), (0, y0, z0), (0, y0 - 0.45 * (y1 - y0) - 2.0, z0 + h), (0, y1 - 0.2 * (y1 - y0), z0 + h * 0.45)], rgb)


# =================================================================================================== dragon
def head_upper(lo=False):
    m = Mesh("bc_head_upper" + ("_lo" if lo else ""))
    n = 4 if lo else 6
    # (y, half width, half height, centre z): neck at the back, round skull, long snout
    S = [(-30, 7.6, 6.6, 0.0), (-8, 8.4, 8.6, 0.6), (6, 9.4, 9.6, 1.2), (18, 7.4, 7.0, 1.2), (30, 5.4, 4.8, 0.2), (40, 3.6, 3.0, -1.0)]
    if lo:
        S = [S[0], S[2], S[4], S[5]]
    secs = [sec(y, a, b, 0, cz, n) for (y, a, b, cz) in S] + [pt(45, 0, -1.4, n)]
    loft(m, 0, secs)
    # two swept-back horns, bent in two sections
    for s in (-1, 1):
        base = [(s * 6.2 + dx, -2 + dy, 8.6) for dx, dy in ((-1.7, -1.7), (1.7, -1.7), (1.7, 1.7), (-1.7, 1.7))]
        mid = [(s * 9.4 + dx, -14 + dy, 12.5) for dx, dy in ((-1.1, -1.1), (1.1, -1.1), (1.1, 1.1), (-1.1, 1.1))]
        tip = [(s * 15.0, -30, 17.0)] * 4
        loft(m, 0, [base, tip])
    # dorsal fins down the skull and neck, shrinking backwards (two-sided)
    fins = ((4, -6, 11, 9.2),) if lo else ((4, -6, 11, 9.2), (-8, -18, 9.5, 8.8), (-20, -32, 7.5, 7.2))
    for (y0, y1, h, z0) in fins:
        ring_fin(m, 0, y1, y0, h, z0)
    # fangs along the upper jaw, pointing down (layer 2, two-sided)
    nt = 1 if lo else 2
    for s in (-1, 1):
        for i in range(nt):
            y = 18 + i * (20 / nt) + 3
            x = s * (4.6 - 0.07 * (y - 10))
            blade2(m, 2, [(x, y - 2.4, -2.0), (x, y + 2.4, -2.0), (x, y + 1.0, -10.5 + 0.6 * i), (x, y - 0.6, -10.5 + 0.6 * i)])
    # eyes (one emissive plate each side) and the glowing crest line
    for s in (-1, 1):
        m.quad(1, [(s * 8.8, 7.5, 5.2), (s * 9.2, 12.5, 4.4), (s * 8.6, 12.5, 2.0), (s * 8.4, 7.5, 2.4)], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
    m.quad(1, [(-0.8, -6, 9.8), (0.8, -6, 9.8), (0.6, 30, 5.4), (-0.6, 30, 5.4)], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
    # the collar: a thin foam ring over the join of skull and neck
    loft(m, 2, [sec(-9.5, 8.9, 9.1, 0, 0.5, n), sec(-7.5, 8.9, 9.1, 0, 0.5, n)])
    return m


def head_lower(lo=False):
    m = Mesh("bc_head_lower" + ("_lo" if lo else ""))
    n = 4 if lo else 6
    S = [(0, 6.2, 4.6, 0), (12, 5.6, 4.0, 0.2), (24, 4.4, 3.2, 0.6), (36, 3.0, 2.2, 1.0)]
    if lo:
        S = [S[0], S[2], S[3]]
    secs = [sec(y, a, b, 0, cz, n) for (y, a, b, cz) in S] + [pt(41, 0, 1.2, n)]
    loft(m, 0, secs)
    nt = 1 if lo else 2
    for s in (-1, 1):
        for i in range(nt):
            y = 14 + i * (22 / nt) + 3
            x = s * (3.9 - 0.06 * (y - 10))
            blade2(m, 2, [(x, y - 2.4, 2.2), (x, y + 2.4, 2.2), (x, y + 1.0, 10.5 - 0.6 * i), (x, y - 0.6, 10.5 - 0.6 * i)])
    return m


def seg(banded, lo=False):
    """a barrel-shaped body piece: two rings of sides round a bulge, a two-sided dorsal fin, a glowing spine line, optionally a foam band"""
    m = Mesh("bc_seg_b" if banded else "bc_seg")
    if lo:
        m.name += "_lo"
    n = 4 if lo else 6
    secs = [sec(-8, 7.2, 6.3, 0, 0, n), sec(0, 8.4, 7.4, 0, 0, n), sec(8, 7.2, 6.3, 0, 0, n)]
    if lo:
        secs = [secs[0], secs[2]]
    loft(m, 0, secs)
    blade2(m, 0, [(0, -8, 6.4), (0, 8, 6.0), (0, 3, 13.0), (0, -3, 11.5)])
    if banded:
        loft(m, 2, [sec(-1.4, 9.2, 8.2, 0, 0, n), sec(1.4, 9.2, 8.2, 0, 0, n)])
    m.quad(1, [(-1.0, -8, 7.0), (1.0, -8, 7.0), (0.9, 8, 6.6), (-0.9, 8, 6.6)], [(0, 0), (1, 0), (1, 1), (0, 1)], W)
    return m


def tail(lo=False):
    m = Mesh("bc_tail" + ("_lo" if lo else ""))
    n = 4 if lo else 6
    loft(m, 0, [sec(0, 7, 6, 0, 0, n), sec(16, 4.4, 3.6, 0, 0, n), pt(36, 0, 0, n)])
    for s in (-1, 1):
        blade2(m, 0, [(0, 14, 0), (s * 11, 24, 0), (s * 6, 35, 0), (0, 22, 0)])
    return m


def head_hull(lo=False):
    """a translucent copy of the skull and neck, 14 percent larger (drawn with the glass palette and a fresnel alpha)"""
    m = Mesh("bc_head_hull" + ("_lo" if lo else ""))
    n = 5 if lo else 6
    S = [(-24, 8.4, 7.6, 0.0), (-6, 10.4, 10.6, 0.7), (18, 8.6, 8.0, 1.2), (36, 4.8, 3.8, -0.4)]
    if lo:
        S = [S[0], S[1], S[3]]
    loft(m, 0, [sec(y, a, b, 0, cz, n) for (y, a, b, cz) in S] + [pt(46, 0, -1.4, n)])
    return m


# =================================================================================================== generic pieces
def horn(lo=False):
    """a broad water tongue of a crown: thick radially (+x), wide sideways (z), five sections curling outward and up, a round-ish tip"""
    m = Mesh("bc_horn" + ("_lo" if lo else ""))
    P = [((0.0, 0.0), 3.4, 5.6), ((3.6, 12.0), 3.2, 5.0), ((10.0, 22.0), 2.4, 3.4), ((19.0, 28.0), 1.4, 1.8)]
    if lo:
        P = [P[0], P[2], P[3]]
    k = 4 if lo else 5
    secs = []
    for (c, rx, rz) in P:
        secs.append([(c[0] + rx * math.cos(2 * math.pi * i / k), c[1], rz * math.sin(2 * math.pi * i / k)) for i in range(k)])
    secs.append([(P[-1][0][0] + 3.2, P[-1][0][1] - 1.0, 0)] * k)
    loft(m, 0, secs)
    return m


def horn_tip(lo=False):
    """where the tip of bc_horn is (model px), used by the preview to hang a droplet there"""
    return (23.2, 27.0)


def ripple(lo=False):
    """a low thin ridged ring on the ground (radius 16, ridge 1.1 high, 3 wide), foam flecks every fifth segment"""
    n = 12 if lo else 22
    m = Mesh("bc_ripple" + ("_lo" if lo else ""))
    R, w, h = 16.0, 0.8, 1.1
    uv = [(0, 0), (1, 0), (1, 1), (0, 1)]
    for i in range(n):
        a0, a1 = 2 * math.pi * i / n, 2 * math.pi * (i + 1) / n

        def P(a, r, y):
            return (r * math.cos(a), y, r * math.sin(a))
        lay = 2 if i % 5 == 0 else 0
        m.quad(lay, [P(a0, R - w, 0), P(a1, R - w, 0), P(a1, R, h), P(a0, R, h)], uv, W)
        m.quad(lay, [P(a0, R, h), P(a1, R, h), P(a1, R + w, 0), P(a0, R + w, 0)], uv, W)
    return m


def sphere_hull(lo=False):
    """a bubble of radius 1, poles on y: the glass hull of the Cradle (10 x 7 sides hi, 8 x 5 lo)"""
    n, rings = (8, 5) if lo else (10, 7)
    m = Mesh("bc_sphere" + ("_lo" if lo else ""))
    secs = [pt(-1.0, 0, 0, n)]
    for i in range(1, rings + 1):
        a = -math.pi / 2 + math.pi * i / (rings + 1)
        secs.append(sec(math.sin(a), math.cos(a), math.cos(a), 0, 0, n))
    secs.append(pt(1.0, 0, 0, n))
    loft(m, 0, secs)
    return m


def dome(lo=False):
    """Nest of the Sea Dragon: a smooth hemisphere (radius 16), three wide spiral foam BANDS lying on it (they turn when the drawer turns the piece), a foam collar at the base"""
    n = 8 if lo else 12
    rings = 3 if lo else 4
    m = Mesh("bc_dome" + ("_lo" if lo else ""))
    secs = []
    for i in range(rings + 1):
        a = (math.pi / 2) * i / (rings + 1)
        secs.append(sec(16 * math.sin(a), 16 * math.cos(a), 16 * math.cos(a), 0, 0, n))
    secs.append(pt(16, 0, 0, n))
    loft(m, 0, secs)
    steps = 5 if lo else 8
    uv = [(0, 0), (1, 0), (1, 1), (0, 1)]

    def P(lon, lat, r):
        return (r * math.cos(lat) * math.cos(lon), r * math.sin(lat), r * math.cos(lat) * math.sin(lon))
    for k in range(3):
        base = 2 * math.pi * k / 3
        prev = None
        for i in range(steps + 1):
            t = i / steps
            lat = math.radians(8 + 56 * t)
            lon = base + t * 1.9 * math.pi
            half = (2.8 - 1.0 * t) / (16.5 * math.cos(lat))                      # constant physical width (2.0 px, thinning to 1.2) whatever the latitude
            cur = (P(lon - half, lat, 16.5), P(lon + half, lat, 16.5))
            if prev is not None:
                m.quad(2, [prev[0], prev[1], cur[1], cur[0]], uv, W)
            prev = cur
    cn = 8 if lo else 12
    for i in range(cn):                                                         # collar: a flat foam ring just outside the hull
        a0, a1 = 2 * math.pi * i / cn, 2 * math.pi * (i + 1) / cn
        m.quad(2, [(15.6 * math.cos(a0), 0.4, 15.6 * math.sin(a0)), (15.6 * math.cos(a1), 0.4, 15.6 * math.sin(a1)),
                   (18.2 * math.cos(a1), 0.4, 18.2 * math.sin(a1)), (18.2 * math.cos(a0), 0.4, 18.2 * math.sin(a0))], uv, W)
    return m


def lance(lo=False):
    """Aqua Javelin: a six-sided shaft, a leaf head with two two-sided barbs, four two-sided tail fins, a glowing core spindle, a foam collar behind the head"""
    n = 6
    m = Mesh("bc_lance" + ("_lo" if lo else ""))
    secs = [sec(-34, 1.4, 1.4, n=n), sec(-6, 2.2, 2.2, n=n), sec(12, 6.4, 3.6, n=n), sec(30, 3.0, 2.0, n=n), pt(52, n=n)]
    if lo:
        secs = [secs[0], secs[2], secs[3], secs[4]]
    loft(m, 0, secs)
    for s in (-1, 1):
        blade2(m, 0, [(s * 5.8, 10, 0), (s * 12, 3, 0), (s * 3, 6, 0), (s * 3, 6, 0)])
    if not lo:
        for ang in (0, 90, 180, 270):
            a = math.radians(ang)
            ca, sa = math.cos(a), math.sin(a)
            blade2(m, 0, [(0, -34, 0), (0, -14, 0), (ca * 7, -26, sa * 7), (ca * 9, -34, sa * 9)])
        loft(m, 2, [sec(-8, 2.9, 2.9, n=n), sec(-5, 2.9, 2.9, n=n)])
    loft(m, 1, [sec(-30, 0.5, 0.5, n=4), sec(8, 1.5, 1.5, n=4), pt(46, n=4)])
    return m


def wing(side, lo=False):
    """one Valkyrie wing in player space (x outward, y up, z forward; roots on the back at z = -3): an arm bone from the shoulder blade up and out, and six feather spindles fanned
    along it (long ones low, short ones at the wrist, each leaning on its neighbour); closed diamonds, so no culling question. side = +1 right, -1 left."""
    m = Mesh("bc_wing_%s" % ("r" if side > 0 else "l") + ("_lo" if lo else ""))
    arm = [(3, 18, -3), (10, 25, -4), (18, 33, -5)]
    # arm bone: a thin four-sided bar through the points
    bar = []
    for (x, y, z) in arm:
        bar.append([(x - 1.3, y, z), (x, y + 1.3, z), (x + 1.3, y, z), (x, y - 1.3, z - 0.6)])
    loft(m, 0, [[(side * p[0], p[1], p[2]) for p in s] for s in bar])
    feathers = [(0, 0.0, 58, 5.4, 4), (0, 0.2, 54, 5.4, 18), (0, 0.42, 48, 5.2, 32), (0, 0.62, 42, 4.8, 46), (0, 0.82, 36, 4.4, 60), (0, 1.0, 30, 4.0, 76)]
    if lo:
        feathers = [feathers[0], feathers[2], feathers[4], feathers[5]]
    for i, (_, t, ln, wd, ang) in enumerate(feathers):
        k = t * (len(arm) - 1)
        j = min(int(k), len(arm) - 2)
        f = k - j
        r = tuple(arm[j][c] * (1 - f) + arm[j + 1][c] * f for c in range(3))
        a = math.radians(ang)
        d = (math.cos(a), math.sin(a), -0.10)
        nx = (-math.sin(a), math.cos(a), 0.0)
        z0 = -1.2 * i
        c0 = (r[0], r[1], r[2] + z0)
        mid = tuple(c0[c] + d[c] * ln * 0.45 for c in range(3))
        tip = tuple(c0[c] + d[c] * ln + (0, -0.04 * ln, 0)[c] for c in range(3))
        ring = [tuple(mid[c] + nx[c] * wd for c in range(3)), (mid[0], mid[1], mid[2] + 1.6), tuple(mid[c] - nx[c] * wd for c in range(3)), (mid[0], mid[1], mid[2] - 1.6)]
        root = [c0] * 4
        tipr = [tip] * 4
        loft(m, 0, [[(side * p[0], p[1], p[2]) for p in s] for s in (root, ring, tipr)])
    return m


# =================================================================================================== build
MESHES = [("head_upper", head_upper, True), ("head_lower", head_lower, True), ("seg", lambda lo=False: seg(False, lo), True), ("seg_b", lambda lo=False: seg(True, lo), True),
          ("tail", tail, True), ("head_hull", head_hull, True), ("horn", horn, True), ("ripple", ripple, True), ("sphere", sphere_hull, True), ("dome", dome, True),
          ("lance", lance, True), ("wing_r", lambda lo=False: wing(1, lo), True), ("wing_l", lambda lo=False: wing(-1, lo), True)]


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(GAME, exist_ok=True)
    lines = ["Water remake, hybrid BC: mesh cores (quads; verts = 4 x quads; layer 0 body / 1 glow / 2 foam)", "", "%-22s %6s %6s %6s %6s" % ("piece", "quads", "L0", "L1", "L2")]
    for name, fn, has_lo in MESHES:
        for lo in ((False, True) if has_lo else (False,)):
            m = fn(lo=lo)
            c = [0, 0, 0]
            for q in m.q:
                c[q[0]] += 1
            with open(os.path.join(OUT, m.name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
                f.write(m.text())
            if name not in NOT_SHIPPED:
                with open(os.path.join(GAME, m.name + ".txt"), "w", encoding="utf-8", newline="\n") as f:
                    f.write(m.text())
            lines.append("%-22s %6d %6d %6d %6d" % (m.name, len(m.q), c[0], c[1], c[2]))
    with open(os.path.join(OUT, "counts_bc.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()

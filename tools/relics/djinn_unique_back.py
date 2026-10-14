"""djinn_unique_back: the wings, tails, halos, back pieces and mantles of the twelve Djinn (each its own builder; the shapes are the point).

Frames (author space, see djinn_unique.py): wings: the wing root, the wing runs along +x (the character's left, sgn = +1) or -x; the outer segment's
origin is the joint 11 px out. Tail: the root of the tail, the chain runs along -z (the back), one segment is 6 px long. Halo: its centre, flat in
xz. Backdrop: the centre of the piece, drawn 2 px above the neck and 5.5 px behind the back, in the xy plane facing +z. Mantle: the neck centre.
"""
import math

import numpy as np

import djinn_unique as du
from djinn_unique import (ml, C, S, curve, collar, sweep_tube, leaf, spike, feather, tongue, zig, orb, disc, crescent, ring_path, sweep_flat, tube, pf_flame,
                          pf_feather, pf_blade, pf_taper, pf_ribbon, pf_flat, UP, unit, arc_path, INNER, OUTER)


# ============================================================================================ wing helpers
def side_of(sgn):
    return "l" if sgn > 0 else "r"


def wing_bones(k, sgn, mat="plate", r_in=1.35, joint=True, shoulder=True, mat_j=None):
    sd = side_of(sgn)
    inn, out = f"wing_in_{sd}", f"wing_out_{sd}"
    ax = "x" if sgn > 0 else "-x"
    ml.loft(k.part(inn, "bone", mat), ax, (0, 0, 0), [(0, r_in, r_in), (5.5, r_in * 0.74, r_in * 0.74), (INNER, r_in * 0.6, r_in * 0.6)], C(1.0, S(8)), caps=(True, True))
    ml.loft(k.part(out, "bone", mat), ax, (0, 0, 0), [(0, r_in * 0.6, r_in * 0.6), (7, r_in * 0.46, r_in * 0.46), (OUTER, r_in * 0.15, r_in * 0.15)], C(1.0, S(8)))
    if shoulder:
        ml.lathe(k.part(inn, "shoulder", k.trim()), ax, (0, 0, 0), [(-1.2, 0.0), (-0.6, 1.9), (0.8, 1.9), (1.4, 0.0)], sides=S(10))
    if joint:
        ml.lathe(k.part(out, "joint", mat_j or k.trim()), ax, (0, 0, 0), [(-0.9, 0.0), (-0.3, 1.2), (0.3, 1.2), (0.9, 0.0)], sides=S(8))


def wf(k, seg, nm, sgn, bx, ang, L, w, t=0.5, curl=0.0, mat="plate", n=5, prof=pf_feather, by=0.0, bz=0.0, layer=0, glow=False, smooth=45):
    """One wing blade (feather / flame / vane) of segment `seg`: starts bx px along the bone, heads `ang` degrees from the bone, curling `curl`."""
    p = k.part(f"wing_{seg}_{side_of(sgn)}", nm, mat, smooth=smooth, layer=layer, glow=glow)
    feather(p, (sgn * bx, by, bz), ang, L, w, t, curl, "xy", n, prof, sx=sgn)
    return p


def wcore(k, seg, nm, sgn, bx, ang, L, w, curl=0.0, n=4, bz=0.25):
    """A glowing core line along a blade (glow layer only): a thin copy of the blade, raised 0.25 toward the viewer."""
    p = k.part(f"wing_{seg}_{side_of(sgn)}", nm, "crystal", layer=2, smooth=45)
    feather(p, (sgn * bx, 0.0, bz), ang, L * 0.92, w * 0.22, 0.3, curl, "xy", n, pf_taper, sx=sgn)


# ============================================================================================ WINGS
def wings_baal(k, sgn):
    """BAAL_STORMRAVEN: dark blade-feathers with a jagged lightning core: 4 short inner, 7 long outer, the longest nearly 2 x the wing's bone."""
    wing_bones(k, sgn, "plate")
    for j, (a, s, L, w) in enumerate(((-14, 0.15, 9.5, 2.3), (-32, 0.4, 10.5, 2.4), (-50, 0.65, 10.0, 2.3), (-68, 0.88, 9.0, 2.0))):
        wf(k, "in", f"f{j}", sgn, s * INNER, a, L, w, 0.55, -10, "plate")
    for j, (a, s, L, w) in enumerate(((14, 0.0, 14, 2.1), (0, 0.1, 18, 2.5), (-14, 0.2, 21, 2.7), (-28, 0.32, 22, 2.8), (-44, 0.46, 20, 2.6), (-60, 0.6, 17, 2.4), (-76, 0.74, 13, 2.1))):
        wf(k, "out", f"f{j}", sgn, s * OUTER, a, L, w, 0.6, -14, "plate")
        if j in (1, 3, 5):
            p = k.part(f"wing_out_{side_of(sgn)}", f"bolt{j}", "crystal", layer=2)
            a0 = math.radians(a - 7 * 0.5)
            c0 = np.array([sgn * s * OUTER, 0.0, 0.3])
            d = np.array([sgn * math.cos(math.radians(a - 4)), math.sin(math.radians(a - 4)), 0.0])
            nrm = np.array([-d[1], d[0] * 1.0, 0.0])
            pts = [tuple(c0 + d * L * f + nrm * off) for f, off in ((0.1, 0), (0.3, 0.7), (0.45, -0.5), (0.65, 0.6), (0.82, -0.3), (0.95, 0))]
            zig(p, pts, 0.55, 0.2)


def wings_amon(k, sgn):
    """AMON_FLAMEWING: flame tongues, glow layer only, 3 inner and 6 outer, each with its own wobble phase."""
    wing_bones(k, sgn, "extra", 1.2)
    for j, (a, s, L, w) in enumerate(((-6, 0.1, 7, 2.6), (-28, 0.45, 7.5, 2.8), (-52, 0.8, 6.5, 2.4))):
        p = k.part(f"wing_in_{side_of(sgn)}", f"t{j}", "crystal", layer=2, smooth=45)
        tongue(p, (sgn * s * INNER, 0, 0), (sgn * math.cos(math.radians(a)), math.sin(math.radians(a)), 0), L, w, 0.5, 1.2 * sgn, j * 1.7, n=6)
    for j, (a, s, L, w) in enumerate(((16, 0.0, 11, 2.8), (-2, 0.12, 14, 3.2), (-20, 0.25, 16, 3.4), (-40, 0.4, 14, 3.2), (-60, 0.55, 11, 2.8))):
        p = k.part(f"wing_out_{side_of(sgn)}", f"t{j}", "crystal", layer=2, smooth=45)
        tongue(p, (sgn * s * OUTER, 0, 0), (sgn * math.cos(math.radians(a)), math.sin(math.radians(a)), 0), L, w, 0.5, 1.2 * sgn, j * 1.3, n=5)
        p2 = k.part(f"wing_out_{side_of(sgn)}", f"c{j}", "dark", smooth=45)
        tongue(p2, (sgn * s * OUTER, 0, -0.35), (sgn * math.cos(math.radians(a)), math.sin(math.radians(a)), 0), L * 0.5, w * 0.4, 0.35, 0.8 * sgn, j * 1.3, n=3)


def wings_vinea(k, sgn):
    """VINEA_TIDEVEIL: three long water ribbons with droplets, and a fan fin at the end of the outer segment."""
    wing_bones(k, sgn, "extra", 1.1)
    sd = side_of(sgn)
    for j, (ang, ln, w) in enumerate(((-16, 24, 4.6), (-40, 27, 4.8), (-66, 25, 4.4))):
        seg = "in" if j == 0 else "out"
        rb = k.part(f"wing_{seg}_{sd}", f"ribbon_{j}", "cloth", uv_mode="loft")
        a = math.radians(ang)
        path = [(sgn * 2.0, 0, 0), (sgn * (2 + ln * 0.45) * math.cos(a), (2 + ln * 0.45) * math.sin(a) * 0.6, 0.6), (sgn * ln * math.cos(a) * 0.92, ln * math.sin(a), 0.2),
                (sgn * (ln + 2) * math.cos(a) * 0.95, (ln + 2) * math.sin(a) * 1.02, -0.4)]
        pts = curve(path, 4)
        du.sweep(rb, pts, ml.rect_ring(1.0, 0.18), scales=lambda t, w=w: (w * (0.35 + 0.65 * math.sin(min(1, t * 1.15) * math.pi * 0.85)), 1.0))
        tr = k.part(f"wing_{seg}_{sd}", f"edge_{j}", "crystal", layer=2)
        du.sweep(tr, pts, ml.rect_ring(0.35, 0.3), scales=lambda t: (1.0, 1.0))
        for q in range(2):
            c = pts[int(len(pts) * (0.5 + 0.22 * q))]
            orb(k.part(f"wing_{seg}_{sd}", f"drop_{j}{q}", "crystal", layer=2, smooth=0), (c[0], c[1] - 0.8, c[2] + 0.2), 0.55 - 0.08 * q, sides=6)
    fan = k.part(f"wing_out_{sd}", "fan", "cloth")
    P = [(sgn * 1.0, 0.0, -0.2), (sgn * 9.0, 7.0, -0.2), (sgn * 15.0, 3.0, -0.2), (sgn * 17.0, -4.0, -0.2), (sgn * 8.0, -3.0, -0.2)]
    leaf(fan, P if sgn > 0 else P[::-1])
    for i, (ex, ey) in enumerate(((9.0, 7.0), (15.0, 3.0), (17.0, -4.0))):
        du.bar(k.part(f"wing_out_{sd}", f"fanrib_{i}", "trim"), (sgn * 1.0, 0, 0.1), (sgn * ex, ey, 0.1), 0.4, 0.14, up=(0, 0, 1), taper=1.0)


def wings_focalor(k, sgn):
    """FOCALOR_GALEBLADES: three huge curved scimitar blades per wing, thin and wide, and a rank of short blades in front of them."""
    wing_bones(k, sgn, "plate", 1.1)
    for j, (a, s, L, w) in enumerate(((-18, 0.25, 11.5, 3.0), (-50, 0.65, 10.0, 2.6))):
        wf(k, "in", f"b{j}", sgn, s * INNER, a, L, w, 0.4, -34, "plate", 6, pf_blade)
        wcore(k, "in", f"c{j}", sgn, s * INNER, a, L, w, -34)
    for j, (a, s, L, w) in enumerate(((22, 0.0, 22, 3.6), (-4, 0.1, 25, 4.0), (-32, 0.28, 23, 3.8), (-60, 0.5, 18, 3.2))):
        wf(k, "out", f"b{j}", sgn, s * OUTER, a, L, w, 0.45, -46, "plate", 7, pf_blade)
        wcore(k, "out", f"c{j}", sgn, s * OUTER, a, L, w, -46, 5)
    for j in range(4):
        wf(k, "out", f"s{j}", sgn, (0.12 + 0.2 * j) * OUTER, -80 + j * 8, 6.5, 1.5, 0.4, -20, "trim", 4, pf_taper, bz=0.5)


def leaf_blade(part, base, ang, L, W, sx=1.0, curl=0.0, n=5):
    """A flat double-sided leaf: an elliptical outline of 2n+2 points along a curved midrib (planar enough for a plant)."""
    mid = arc_path(base, ang, L, curl, n, "xy", sx)
    outl, back = [], []
    for i, p in enumerate(mid):
        s = i / n
        d = np.array(mid[min(i + 1, n)]) - np.array(mid[max(i - 1, 0)])
        d = unit(d)
        nrm = np.array([-d[1], d[0], 0.0])
        wv = W * 0.5 * math.sin(math.pi * min(1.0, s * 0.92 + 0.04)) ** 0.8
        outl.append(tuple(np.array(p) + nrm * wv))
        back.append(tuple(np.array(p) - nrm * wv))
    return outl + back[::-1]


def wings_zagan(k, sgn):
    """ZAGAN_LEAFWING: broad leaves on the bones with a midrib each, and a vine that spirals the outer bone."""
    wing_bones(k, sgn, "extra", 1.2)
    sd = side_of(sgn)
    for j, (a, s, L, W) in enumerate(((-20, 0.2, 10, 5.0), (-48, 0.55, 10, 4.6), (-76, 0.85, 8.5, 3.8))):
        lf = k.part(f"wing_in_{sd}", f"leaf{j}", "cloth")
        P = leaf_blade(lf, (sgn * s * INNER, 0, 0), a, L, W, sgn, -12 * 0.5)
        leaf(lf, P if sgn > 0 else P[::-1])
        wf(k, "in", f"rib{j}", sgn, s * INNER, a, L * 0.95, 0.4, 0.3, -6, "trim", 4, pf_taper, bz=0.12)
    for j, (a, s, L, W) in enumerate(((20, 0.0, 14, 5.4), (2, 0.1, 18, 6.4), (-18, 0.22, 20, 7.0), (-38, 0.38, 18, 6.4), (-58, 0.55, 15, 5.4), (-78, 0.72, 11.5, 4.2))):
        lf = k.part(f"wing_out_{sd}", f"leaf{j}", "cloth")
        P = leaf_blade(lf, (sgn * s * OUTER, 0, 0), a, L, W, sgn, -14)
        leaf(lf, P if sgn > 0 else P[::-1])
        wf(k, "out", f"rib{j}", sgn, s * OUTER, a, L * 0.95, 0.5, 0.3, -7, "trim", 4, pf_taper, bz=0.12)
        if j in (1, 3):
            wcore(k, "out", f"vein{j}", sgn, s * OUTER, a - 2, L, 4.0, -14)
    vp = [(sgn * (1.0 + 3.2 * i), 1.2 * math.sin(i * 1.5), 1.2 * math.cos(i * 1.5)) for i in range(6)]
    sweep_tube(k.part(f"wing_out_{sd}", "vine", "extra"), [(sgn * 0.5 * i * 2.4, 1.1 * math.sin(i * 1.3), 1.1 * math.cos(i * 1.3)) for i in range(6)], 0.5, 0.3, 5)


def shard(part, base, direction, L, w, sections=6):
    """A faceted crystal shard: a chamfered section swelling and closing to a point."""
    d = unit(direction)
    ml.loft(part, d, base, [(0, w * 0.35, w * 0.25), (L * 0.2, w * 0.5, w * 0.35), (L * 0.7, w * 0.4, w * 0.28), (L, 0.0, 0.0)], C(1.0, 6), up=(0, 0, 1))


def wings_zepar(k, sgn):
    """ZEPAR_SHARDWING: heavy crystal shards on thick bones (a fan of 3 inner and 6 outer, one huge)."""
    wing_bones(k, sgn, "extra", 1.5)
    sd = side_of(sgn)
    for j, (a, s, L, w) in enumerate(((-16, 0.2, 9, 3.0), (-42, 0.55, 9.5, 3.2), (-68, 0.85, 8, 2.8))):
        shard(k.part(f"wing_in_{sd}", f"s{j}", "crystal", glow=(j == 1), smooth=40), (sgn * s * INNER, 0, 0), (sgn * math.cos(math.radians(a)), math.sin(math.radians(a)), 0), L, w)
    for j, (a, s, L, w) in enumerate(((24, 0.0, 14, 3.4), (8, 0.1, 21, 4.4), (-10, 0.22, 26, 5.0), (-30, 0.36, 22, 4.6), (-52, 0.52, 17, 4.0), (-74, 0.7, 12, 3.2))):
        shard(k.part(f"wing_out_{sd}", f"s{j}", "crystal", glow=(j in (2, 4)), smooth=40), (sgn * s * OUTER, 0, 0), (sgn * math.cos(math.radians(a)), math.sin(math.radians(a)), 0), L, w)
    for j in range(3):
        du.stud(k.part(f"wing_out_{sd}", f"stud{j}", k.trim()), (sgn * (2.0 + 4.5 * j), 0.1, 0.9), 0.7, 0.5, (0, 0, 1), 7)


def wings_furfur(k, sgn):
    """FURFUR_SONICARCS: concentric sound-wave crescents standing on each segment, the glow running along them."""
    wing_bones(k, sgn, "extra", 1.1)
    sd = side_of(sgn)
    for seg, radii, ctr in (("in", (4.0, 7.0), (0.0, 0.0, 0.0)), ("out", (7.5, 11.5, 16.5, 21.5), (0.0, 0.0, 0.0))):
        for j, R in enumerate(radii):
            a0, a1 = (-78 + j * 6, 18 - j * 3)
            path = ring_path(ctr, R, a0, a1, "xy", 8)
            if sgn < 0:
                path = [(-x, y, z) for (x, y, z) in path]
            p = k.part(f"wing_{seg}_{sd}", f"arc{j}", "plate", smooth=45)
            sweep_flat(p, path, 1.8 + 0.25 * j, 0.55, lambda s: math.sin(math.pi * min(1.0, s * 0.98 + 0.01)) ** 0.7, (0, 0, 1))
            if j % 2 == 0:
                q = k.part(f"wing_{seg}_{sd}", f"glow{j}", "crystal", layer=2)
                sweep_flat(q, [(x, y, z + 0.3) for (x, y, z) in path], 0.5, 0.3, lambda s: math.sin(math.pi * min(1.0, s * 0.98 + 0.01)) ** 0.7, (0, 0, 1))
    for i in range(3):
        orb(k.part(f"wing_out_{sd}", f"note{i}", "crystal", layer=2, smooth=0), (sgn * (6 + 5.5 * i), -4.0 - 2 * i, 0.4), 0.7 - 0.1 * i, sides=6)


def wings_marchosias(k, sgn):
    """MARCHOSIAS_EMBERBAT: four clawed fingers and a ragged, burnt membrane with glowing cracks."""
    wing_bones(k, sgn, "plate", 1.3)
    sd = side_of(sgn)
    tips = [(sgn * 1.0, 16.0), (sgn * 10.0, 14.0), (sgn * 20.0, 7.0), (sgn * 26.0, -3.0), (sgn * 20.0, -13.0)]
    root = (sgn * 0.0, 0.0)
    for i, t in enumerate(tips[:4]):
        ml.loft(k.part(f"wing_out_{sd}", f"finger{i}", "plate", smooth=45), (t[0] - sgn * 11.0, t[1], 0.0), (sgn * 0.0, 0.0, 0.0), [(0, 0.6, 0.6), (float(np.hypot(t[0] - sgn * 11.0, t[1])), 0.12, 0.12)], C(1.0, S(6)))
        spike(k.part(f"wing_out_{sd}", f"claw{i}", "bone"), (t[0] - sgn * 11.0, t[1], 0.0), (sgn * 0.3, 1.0, 0.0) if i < 2 else (sgn * 1.0, 0.1 - i * 0.2, 0.0), 2.4, 0.5, 5)
    # the membrane, split into panels between fingers; the trailing edge is scalloped inwards
    ends = [(t[0] - sgn * 11.0, t[1]) for t in tips[:4]]
    sc = lambda a, b, d: ((a[0] + b[0]) / 2 - sgn * d, (a[1] + b[1]) / 2 - d * 0.6)
    panels = [[(0, 0), ends[0], sc(ends[0], ends[1], 2.4), ends[1]], [(0, 0), ends[1], sc(ends[1], ends[2], 2.8), ends[2]],
              [(0, 0), ends[2], sc(ends[2], ends[3], 3.0), ends[3], (sgn * -2.0, -9.0), (sgn * -9.0, -6.0)]]
    for j, poly in enumerate(panels):
        pl = k.part(f"wing_out_{sd}", f"panel{j}", "cloth", uv_mode="dominant")
        P = [(x, y, -0.1) for (x, y) in poly]
        leaf(pl, P if sgn > 0 else P[::-1])
        du.bar(k.part(f"wing_out_{sd}", f"crack{j}", "crystal", layer=2), (0, 0, 0.1), (ends[j][0] * 0.8, ends[j][1] * 0.8, 0.1), 0.35, 0.1, up=(0, 0, 1), taper=1.0)
    inner = k.part(f"wing_in_{sd}", "web", "cloth")
    P = [(0.0, 0.2, -0.1), (sgn * 11.0, 0.2, -0.1), (sgn * 9.0, -6.0, -0.1), (sgn * 4.0, -8.0, -0.1), (sgn * 0.0, -4.0, -0.1)]
    leaf(inner, P if sgn > 0 else P[::-1])


def wings_sitri(k, sgn):
    """SITRI_SUNFAN: every blade starts at the shoulder and fans out like a ray: seven long on the outer segment, four short on the inner."""
    wing_bones(k, sgn, "trim", 1.1)
    for j, a in enumerate((-70, -52, -34, -16)):
        wf(k, "in", f"r{j}", sgn, 0.1, a, 9.0 - 0.8 * j, 2.2, 0.5, -4, "plate", 4, pf_blade)
        wcore(k, "in", f"c{j}", sgn, 0.1, a, 9.0 - 0.8 * j, 2.2, -4)
    for j, a in enumerate((-82, -62, -42, -22, -4, 14, 30)):
        L = 15.0 + 5.0 * math.sin((j + 0.5) * math.pi / 7)
        wf(k, "out", f"r{j}", sgn, 0.2, a, L, 2.6, 0.55, -4, "plate", 5, pf_blade)
        wcore(k, "out", f"c{j}", sgn, 0.2, a, L, 2.6, -4)
        if j % 2 == 0:
            orb(k.part(f"wing_out_{side_of(sgn)}", f"tip{j}", "crystal", layer=2, smooth=0), (sgn * (0.2 + math.cos(math.radians(a)) * (L + 0.4)), math.sin(math.radians(a)) * (L + 0.4), 0.0), 0.7, sides=6)


def bolt_polygon(k, seg, sd, sgn, nm, base, steps, w, mat="plate", glow=False):
    """A lightning-bolt vane: a zigzag of bars, each leg tapering."""
    pts = [tuple(np.array(base, float) + np.array([sgn * dx, dy, 0.0])) for dx, dy in steps]
    zig(k.part(f"wing_{seg}_{sd}", nm, mat, layer=2 if glow else 0), pts, w, 0.45)


def wings_bifron(k, sgn):
    """BIFRON_FORKWING: zig-zag lightning vanes, three on the inner segment and four on the outer, each a chain of tapering bars."""
    wing_bones(k, sgn, "plate", 1.2)
    sd = side_of(sgn)
    inner = [((0.15, 0.0), [(0, 0), (3, -2), (2, -4), (5, -7), (4, -8), (7.5, -12)]), ((0.5, 0.0), [(0, 0), (3, -3), (2, -5), (5, -9.5), (3.6, -10), (6, -15)]),
             ((0.85, 0.0), [(0, 0), (2, -3), (1, -5), (3, -9), (1.5, -9.5), (3, -13.5)])]
    for j, ((s, _), steps) in enumerate(inner):
        bolt_polygon(k, "in", sd, sgn, f"b{j}", (s * INNER, 0, 0), steps, 2.6)
        bolt_polygon(k, "in", sd, sgn, f"g{j}", (s * INNER, 0, 0.25), steps, 0.6, "crystal", True)
    outer = [(0.0, [(0, 0), (4, 3), (3, 5), (8, 8), (7, 9.5), (13, 13)]), (0.1, [(0, 0), (5, 0), (4, -2), (10, -2), (9, -4), (17, -3)]),
             (0.25, [(0, 0), (5, -3), (3.6, -5), (9, -8), (7.5, -9.5), (15, -14)]), (0.4, [(0, 0), (4, -4), (2.4, -6), (7, -11), (5, -12.5), (11, -19)]),
             (0.6, [(0, 0), (3, -4), (1.5, -6.5), (5, -11.5), (3, -13), (7, -20)])]
    for j, (s, steps) in enumerate(outer):
        bolt_polygon(k, "out", sd, sgn, f"b{j}", (s * OUTER, 0, 0), steps, 2.8)
        bolt_polygon(k, "out", sd, sgn, f"g{j}", (s * OUTER, 0, 0.25), steps, 0.7, "crystal", True)


def wings_paimon(k, sgn):
    """PAIMON_SKYWING: a big angel wing: a row of short coverts, then eight long primaries sweeping into a crescent, tips curling down."""
    wing_bones(k, sgn, "plate", 1.2)
    for j in range(5):
        wf(k, "in", f"cov{j}", sgn, (0.1 + 0.2 * j) * INNER, -30 - j * 6, 7.5 - 0.5 * j, 2.6, 0.5, -6, "cloth", 4, pf_feather, bz=0.5)
    for j, a in enumerate((-3, -12, -22, -32, -42, -52, -62, -74)):
        s = 0.0 + 0.1 * j
        L = 15.0 + 8.0 * math.sin((j + 0.7) * math.pi / 8.6)
        wf(k, "out", f"pri{j}", sgn, s * OUTER, a, L, 2.5, 0.55, -26, "plate", 6, pf_feather)
        if j % 2 == 0:
            wf(k, "out", f"cov{j}", sgn, s * OUTER, a + 6, L * 0.45, 2.3, 0.5, -10, "cloth", 4, pf_feather, bz=0.5)
        if j in (1, 4, 6):
            wcore(k, "out", f"c{j}", sgn, s * OUTER, a, L, 2.5, -26, 5)


def wings_vassago(k, sgn):
    """VASSAGO_JELLYWING: a webbed dome-fan of five curved ribs, and five tentacles hanging from its rim with a glowing bead each."""
    wing_bones(k, sgn, "plate", 1.3)
    sd = side_of(sgn)
    ribs = []
    for j, a in enumerate((26, 6, -14, -34, -54)):
        ends = []
        path = arc_path((0, 0, 0), a, 21.0 - abs(a) * 0.1, -30, 6, "xy", sgn)
        ribs.append(path)
        du.tube_direct(k.part(f"wing_out_{sd}", f"rib{j}", "extra"), path, 0.55, 0.15, 5)
    for j in range(4):
        a0, a1 = ribs[j], ribs[j + 1]
        pl = k.part(f"wing_out_{sd}", f"web{j}", "cloth", uv_mode="dominant")
        P = [a0[0]] + [a0[i] for i in range(2, 7, 2)] + [a1[i] for i in range(6, 1, -2)]
        # drape the rim between two ribs a little inwards: a scalloped dome
        P = [tuple(p) for p in P]
        P = [(p[0], p[1], -0.25) for p in P]
        leaf(pl, P if sgn > 0 else P[::-1])
        du.bar(k.part(f"wing_out_{sd}", f"vein{j}", "crystal", layer=2), (0, 0, 0.0), (a0[4][0] * 0.8 + a1[4][0] * 0.2, a0[4][1] * 0.8 + a1[4][1] * 0.2, 0.05), 0.3, 0.1, up=(0, 0, 1), taper=1.0)
    for j, path in enumerate(ribs):
        e = np.array(path[-1])
        pth = [tuple(e), tuple(e + (sgn * 0.8, -4.0, 0.3)), tuple(e + (sgn * -0.6, -8.0, 0.0)), tuple(e + (sgn * 0.8, -12.0 - j % 2 * 3, -0.2))]
        sweep_tube(k.part(f"wing_out_{sd}", f"tent{j}", "cloth"), pth, 0.65, 0.1, 5)
        orb(k.part(f"wing_out_{sd}", f"bead{j}", "crystal", layer=2, smooth=0), pth[2], 0.5, sides=6)
    pend = k.part(f"wing_in_{sd}", "web", "cloth")
    P = [(0.0, 0.2, -0.1), (sgn * 11.0, 0.2, -0.1), (sgn * 8.0, -5.0, -0.1), (sgn * 2.0, -4.0, -0.1)]
    leaf(pend, P if sgn > 0 else P[::-1])


WINGS = {"BAAL": wings_baal, "AMON": wings_amon, "VINEA": wings_vinea, "FOCALOR": wings_focalor, "ZAGAN": wings_zagan, "ZEPAR": wings_zepar, "FURFUR": wings_furfur,
         "MARCHOSIAS": wings_marchosias, "SITRI": wings_sitri, "BIFRON": wings_bifron, "PAIMON": wings_paimon, "VASSAGO": wings_vassago}


# ============================================================================================ TAILS (a segment is 6 px along -z)
def seg_core(k, r, mat="plate", name="seg", sides=8, taper=0.8):
    ml.loft(k.part("tail_seg", name, mat), "-z", (0, 0, 0), [(0, r, r), (3.0, r * 0.95, r * 0.95), (6.0, r * taper, r * taper)], C(1.0, S(sides)), caps=(False, False))


def tail_baal(k):
    seg_core(k, 1.15)
    collar(k.part("tail_seg", "ring", k.trim()), -0.25, 1.3, 0.7, lip=0.2, sides=S(8), axis="-z")
    ml.loft(k.part("tail_seg", "fin", "plate", smooth=45), "-z", (0, 1.0, 0), [(0, 0.2, 0.2), (3, 1.0, 1.0), (6, 0.2, 0.2)], ml.lozenge(0.4, 2.2), up=(1, 0, 0))
    zig(k.part("tail_tip", "bolt", "crystal", layer=2), [(0, 0, 0), (0.8, 0.5, -3), (-0.8, -0.3, -5.4), (0.4, 0.2, -8.5), (0, 0, -12)], 1.2, 0.5, up=(1, 0, 0))
    ml.loft(k.part("tail_tip", "barb", "plate"), "-z", (0, 0, -9.0), [(0, 0.6, 0.6), (2.5, 2.0, 0.8), (4.5, 0.0, 0.0)], ml.lozenge(1.0, 0.5), up=(0, 1, 0))


def tail_amon(k):
    p = k.part("tail_seg", "flame", "crystal", layer=2, smooth=45)
    tongue(p, (0, 0, 0), (0, 0.25, -1.0), 7.0, 2.8, 0.6, 0.8, 0.0, plane="yz", n=5)
    p2 = k.part("tail_seg", "flame2", "crystal", layer=2, smooth=45)
    tongue(p2, (0, 0, 0), (0, -0.2, -1.0), 6.0, 2.4, 0.6, 0.8, 2.0, plane="xz", n=5)
    seg_core(k, 0.9, "extra", "core", 6, 0.9)
    tg = k.part("tail_tip", "big", "crystal", layer=2, smooth=45)
    tongue(tg, (0, 0, 0), (0, 0.5, -1.0), 13.0, 4.8, 0.7, 1.4, 0.4, plane="yz", n=7)
    tg2 = k.part("tail_tip", "big2", "crystal", layer=2, smooth=45)
    tongue(tg2, (0, 0, 0), (0.0, 0.0, -1.0), 10.0, 4.0, 0.7, 1.2, 2.4, plane="xz", n=6)


def tail_vinea(k):
    seg_core(k, 1.1, "cloth", "seg", 8, 0.85)
    for sgn, nm in ((1, "l"), (-1, "r")):
        ml.loft(k.part("tail_seg", "fin_" + nm, "cloth", smooth=45), "-z", (sgn * 0.9, 0, 0), [(0, 0.2, 0.2), (3, 1.0, 1.0), (6, 0.2, 0.2)], ml.lozenge(2.6, 0.3), up=(0, 1, 0))
    orb(k.part("tail_seg", "pearl", "crystal", layer=2, smooth=0), (0, 0, -3.0), 0.55, sides=6)
    for sgn, nm in ((1, "l"), (-1, "r")):
        p = k.part("tail_tip", "fluke_" + nm, "cloth", smooth=45)
        sweep_flat(p, [(0, 0, -2.0), (sgn * 3.0, 0, -4.2), (sgn * 6.0, 0, -7.8), (sgn * 7.2, 0, -11.5)], 4.0, 0.4, lambda s: (0.6 + 0.9 * s) * (1 - s ** 3) + 0.05, (0, 1, 0))
        e = k.part("tail_tip", "edge_" + nm, "crystal", layer=2)
        sweep_flat(e, [(0, 0.2, -2.0), (sgn * 3.0, 0.2, -4.2), (sgn * 6.0, 0.2, -7.8), (sgn * 7.2, 0.2, -11.5)], 0.6, 0.3, pf_taper, (0, 1, 0))
    ml.loft(k.part("tail_tip", "stem", "cloth"), "-z", (0, 0, 0), [(0, 1.0, 1.0), (3.0, 0.6, 0.6)], C(1.0, S(8)))


def tail_focalor(k):
    for i, off in enumerate((-0.9, 0.9)):
        p = k.part("tail_seg", f"ribbon_{i}", "cloth", smooth=45)
        sweep_flat(p, [(off, 0, 0), (off * 1.6, 0.4, -2.0), (off * 2.2, 0, -4.0), (off * 2.6, -0.4, -6.0)], 2.0, 0.3, pf_ribbon, (0, 1, 0))
    seg_core(k, 0.55, "trim", "cord", 6, 0.8)
    for i, a in enumerate((-30, 0, 30)):
        p = k.part("tail_tip", f"streamer_{i}", "cloth", smooth=45)
        r = math.radians(a)
        sweep_flat(p, [(0, 0, 0), (math.sin(r) * 2.0, 0.3, -3.0), (math.sin(r) * 4.4, -0.2, -6.5), (math.sin(r) * 6.4, 0.2, -10.5 - (1 - abs(i - 1)) * 2.0)], 2.2, 0.3, pf_feather, (0, 1, 0))
    orb(k.part("tail_tip", "bead", "crystal", layer=2, smooth=0), (0, 0, -0.5), 0.7, sides=6)


def tail_zagan(k):
    seg_core(k, 0.95, "extra", "vine", 6, 0.85)
    for sgn, nm in ((1, "l"), (-1, "r")):
        lf = k.part("tail_seg", "leaf_" + nm, "cloth")
        P = [(sgn * 0.8, 0.0, -1.0), (sgn * 3.0, 0.5, -2.0), (sgn * 4.0, 0.8, -4.0), (sgn * 1.6, 0.3, -3.6)]
        leaf(lf, P if sgn > 0 else P[::-1])
    for j in range(3):
        spike(k.part("tail_seg", f"thorn_{j}", k.trim()), (0, 0.8, -0.8 - j * 2.0), (0, 1.0, -0.4), 1.4, 0.4, 5)
    bud = k.part("tail_tip", "bud", "crystal", layer=2, smooth=0)
    du.gem(bud, (0, 0, -3.0), 1.6, long=1.8, sides=6, axis="-z")
    for i in range(5):
        a = i * 2 * math.pi / 5
        pt = k.part("tail_tip", f"petal_{i}", "cloth", smooth=45)
        du.sweep_flat(pt, [(math.cos(a) * 0.8, math.sin(a) * 0.8, -2.0), (math.cos(a) * 2.0, math.sin(a) * 2.0, -4.0), (math.cos(a) * 1.4, math.sin(a) * 1.4, -7.0)], 1.8, 0.3, pf_blade, (-math.sin(a), math.cos(a), 0))


def tail_zepar(k):
    seg_core(k, 1.35, "extra", "link", 7, 0.9)
    for i, a in enumerate((0, 90, 180, 270)):
        r = math.radians(a)
        spike(k.part("tail_seg", f"studs_{i}", k.trim()), (math.sin(r) * 1.2, math.cos(r) * 1.2, -2.0), (math.sin(r), math.cos(r), -0.2), 1.5, 0.45, 5)
    orb(k.part("tail_tip", "ball", "extra", smooth=40), (0, 0, -3.2), 3.0, sides=10)
    for i in range(10):
        a, b = i * 2.399, math.acos(1 - 2 * (i + 0.5) / 10)
        d = np.array([math.sin(b) * math.cos(a), math.sin(b) * math.sin(a), math.cos(b)])
        spike(k.part("tail_tip", f"spike_{i}", k.trim()), tuple(np.array([0, 0, -3.2]) + d * 2.6), tuple(d), 2.4, 0.7, 5)


def tail_furfur(k):
    seg_core(k, 0.5, "trim", "cord", 6, 0.8)
    sweep_flat(k.part("tail_seg", "ribbon", "cloth", smooth=45), [(0, 0.8, 0), (0, 1.6, -2.0), (0, 0.8, -4.0), (0, 1.4, -6.0)], 1.6, 0.3, pf_ribbon, (1, 0, 0))
    ml.loft(k.part("tail_tip", "bell", k.trim()), "-z", (0, 0, 0), [(0, 0.4, 0.4), (2.0, 1.6, 1.6), (5.0, 3.2, 3.2), (6.0, 3.5, 3.5)], C(1.0, S(12)), caps=(True, False))
    collar(k.part("tail_tip", "lip", "plate"), -5.8, 3.6, 0.5, lip=0.1, sides=S(12), axis="-z")
    du.gem(k.part("tail_tip", "clapper", "crystal", layer=2, smooth=0), (0, 0, -5.0), 0.8, long=1.0, sides=6, axis="z")


def tail_marchosias(k):
    ml.loft(k.part("tail_seg", "fur", "cloth", smooth=45), "-z", (0, 0, 0), [(0, 1.2, 1.2), (3, 2.3, 2.3), (6, 1.9, 1.9)], C(1.0, S(8)), caps=(False, False))
    for i in range(6):
        a = i * math.pi / 3
        spike(k.part("tail_seg", f"tuft_{i}", "cloth"), (math.cos(a) * 1.6, math.sin(a) * 1.6, -1.0 - (i % 2) * 2.0), (math.cos(a) * 0.6, math.sin(a) * 0.6, -1.0), 3.0, 0.9, 5)
    for i in range(5):
        a = i * 2 * math.pi / 5
        tg = k.part("tail_tip", f"flame_{i}", "crystal", layer=2, smooth=45)
        tongue(tg, (math.cos(a) * 0.9, math.sin(a) * 0.9, 0), (math.cos(a) * 0.3, math.sin(a) * 0.3, -1.0), 10.0 - (i % 2) * 2.5, 3.4, 0.5, 0.9, i, plane="xy" if i % 2 else "yz", n=6)


def tail_bifron(k):
    seg_core(k, 1.0, "plate", "seg", 6, 0.85)
    for sgn, nm in ((1, "u"), (-1, "d")):
        ml.loft(k.part("tail_seg", "fin_" + nm, "plate", smooth=45), "-z", (0, sgn * 0.9, 0), [(0, 0.2, 0.2), (3, 1.0, 1.0), (6, 0.2, 0.2)], ml.lozenge(0.35, 1.8), up=(1, 0, 0))
    for sgn, nm in ((1, "l"), (-1, "r")):
        zig(k.part("tail_tip", "prong_" + nm, "plate"), [(0, 0, 0), (sgn * 1.6, 0, -3.0), (sgn * 1.0, 0, -5.0), (sgn * 3.4, 0, -9.0), (sgn * 2.4, 0, -10.5), (sgn * 4.6, 0, -15.0)], 1.5, 0.5, up=(0, 1, 0))
        zig(k.part("tail_tip", "glow_" + nm, "crystal", layer=2), [(0, 0.2, 0), (sgn * 1.6, 0.2, -3.0), (sgn * 1.0, 0.2, -5.0), (sgn * 3.4, 0.2, -9.0), (sgn * 2.4, 0.2, -10.5), (sgn * 4.6, 0.2, -15.0)], 0.4, 0.2, up=(0, 1, 0))


def tail_paimon(k):
    ml.loft(k.part("tail_seg", "stem", "trim"), "-z", (0, 0, 0), [(0, 0.5, 0.5), (6, 0.45, 0.45)], C(1.0, S(6)), caps=(False, False))
    for i, a in enumerate((-40, -14, 14, 40)):
        r = math.radians(a)
        f = k.part("tail_seg", f"plume_{i}", "plate", smooth=45)
        sweep_flat(f, [(0, 0, -0.5), (math.sin(r) * 2.2, 0.2, -3.2), (math.sin(r) * 4.4, 0.0, -5.8)], 1.9, 0.3, pf_feather, (0, 1, 0))
    for i, a in enumerate((-34, -11, 11, 34)):
        r = math.radians(a)
        f = k.part("tail_tip", f"plume_{i}", "cloth", smooth=45)
        sweep_flat(f, [(0, 0, 0), (math.sin(r) * 2.4, 0.3, -4.0), (math.sin(r) * 5.0, 0.0, -9.0 - 3.0 * (1 - abs(a) / 40))], 2.4, 0.3, pf_feather, (0, 1, 0))
    du.gem(k.part("tail_tip", "jewel", "crystal", layer=2, smooth=0), (0, 0.4, -1.0), 0.6, long=1.0, sides=6, axis="z")


def tail_vassago(k):
    seg_core(k, 1.1, "cloth", "tentacle", 7, 0.78)
    for j in range(2):
        orb(k.part("tail_seg", f"sucker_{j}", "bone", smooth=0), (0, -1.0, -1.6 - j * 2.4), 0.55, sides=6)
    pth = [(0, 0, 0), (0, -1.0, -3.0), (0.8, -3.0, -5.6), (0.2, -5.4, -6.4), (-0.8, -6.6, -5.2), (-0.4, -5.8, -3.6)]
    sweep_tube(k.part("tail_tip", "hook", "cloth"), pth, 0.9, 0.12, 6)
    orb(k.part("tail_tip", "eye", "crystal", layer=2, smooth=0), (-0.4, -5.6, -3.4), 0.8, sides=7)


TAILS = {"BAAL": tail_baal, "AMON": tail_amon, "VINEA": tail_vinea, "FOCALOR": tail_focalor, "ZAGAN": tail_zagan, "ZEPAR": tail_zepar, "FURFUR": tail_furfur,
         "MARCHOSIAS": tail_marchosias, "BIFRON": tail_bifron, "PAIMON": tail_paimon, "VASSAGO": tail_vassago}
# Sitri has no tail


# ============================================================================================ HALOS (flat in xz, centre at the origin)
HALO_R = 6.4          # ref 61: 1.6 x the head width, about 12.8 px across


def ring_xz(R, y=0.0, n=16):
    return [(R * math.cos(a), y, R * math.sin(a)) for a in np.linspace(0, 2 * math.pi, n + 1)]


def halo_amon(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HALO_R), C(0.55, S(6)))
    du.sweep(k.part("halo", "inner", "crystal", layer=2), ring_xz(HALO_R * 0.74, 0.0), C(0.3, S(5)))
    for i in range(9):
        a = i * 2 * math.pi / 9
        tg = k.part("halo", f"flame_{i}", "crystal", layer=2, smooth=45)
        tongue(tg, (HALO_R * math.cos(a), 0.0, HALO_R * math.sin(a)), (math.cos(a) * 0.2, 1.0, math.sin(a) * 0.2), 3.4 + (i % 3) * 0.8, 1.5, 0.4, 0.5, i, plane="xy" if i % 2 else "yz", n=4)


def halo_vinea(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HALO_R), C(0.4, S(6)))
    for i in range(8):
        a = i * 2 * math.pi / 8
        orb(k.part("halo", f"drop_{i}", "crystal", layer=2, smooth=0), (HALO_R * math.cos(a), -0.2 - (i % 2) * 0.6, HALO_R * math.sin(a)), 0.8 if i % 2 == 0 else 0.55, sides=6)


def halo_focalor(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HALO_R), C(0.5, S(6)))
    for i in range(4):
        a = i * math.pi / 2
        p = k.part("halo", f"swirl_{i}", "plate", smooth=45)
        path = [(HALO_R * math.cos(a + t * 0.9), 0.35 * i * 0 + t * 1.0, HALO_R * math.sin(a + t * 0.9)) for t in np.linspace(0, 1, 6)]
        sweep_flat(p, path, 2.4, 0.4, pf_blade, (0, 1, 0) if False else (0.0, 1.0, 0.0))


def halo_zagan(k):
    du.sweep(k.part("halo", "vine", "extra", smooth=40), ring_xz(HALO_R, 0.0, 14), C(0.5, S(6)))
    for i in range(8):
        a = i * 2 * math.pi / 8
        c = (HALO_R * math.cos(a), 0.0, HALO_R * math.sin(a))
        orb(k.part("halo", f"bud_{i}", "crystal", glow=(i % 2 == 0), smooth=0), (c[0], 0.5, c[2]), 0.8 if i % 2 == 0 else 0.55, sides=6)
        lf = k.part("halo", f"leaf_{i}", "cloth")
        P = [c, (c[0] * 1.2 - math.sin(a) * 0.9, 0.2, c[2] * 1.2 + math.cos(a) * 0.9), (c[0] * 1.5, 0.0, c[2] * 1.5), (c[0] * 1.2 + math.sin(a) * 0.9, 0.2, c[2] * 1.2 - math.cos(a) * 0.9)]
        leaf(lf, P)


def halo_furfur(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HALO_R), C(0.45, S(6)))
    for i in range(6):
        a = i * math.pi / 3
        c = np.array([HALO_R * math.cos(a), 0.0, HALO_R * math.sin(a)])
        d = np.array([math.cos(a), 0.0, math.sin(a)])
        for sg in (-1, 1):
            side = np.array([-math.sin(a), 0, math.cos(a)])
            du.bar(k.part("halo", f"fork_{i}{sg}", "plate"), tuple(c + side * 0.5 * sg), tuple(c + side * 0.5 * sg + d * 2.8), 0.35, 0.3, up=(0, 1, 0), taper=0.4)
        du.gem(k.part("halo", f"note_{i}", "crystal", layer=2, smooth=0), tuple(c + d * 3.2), 0.5, long=0.9, sides=6)


def halo_sitri(k):
    """SITRI_LENS: a thin ring of a lens outline with points at the sides (ref 61), tilted slightly by the game."""
    pts = [(1.28 * HALO_R * math.cos(a) * (1.0 + 0.08 * math.cos(2 * a) ** 2), 0.0, 0.82 * HALO_R * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 21)]
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), pts, C(0.42, S(6)))
    for sgn, nm in ((1, "l"), (-1, "r")):
        spike(k.part("halo", "point_" + nm, k.trim()), (sgn * 1.28 * HALO_R, 0, 0), (sgn * 1.0, 0.0, 0.0), 3.0, 0.5, 5)
    du.gem(k.part("halo", "sun", "crystal", layer=2, smooth=0), (0, 0.0, -0.82 * HALO_R), 0.8, long=1.1, sides=6, axis="z")


def halo_bifron(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HALO_R * 0.9), C(0.4, S(6)))
    for i in range(8):
        a = i * 2 * math.pi / 8
        c = np.array([HALO_R * 0.9 * math.cos(a), 0.0, HALO_R * 0.9 * math.sin(a)])
        d = np.array([math.cos(a), 0.0, math.sin(a)])
        side = np.array([-math.sin(a), 0, math.cos(a)])
        zig(k.part("halo", f"bolt_{i}", "crystal", layer=2), [tuple(c), tuple(c + d * 1.2 + side * 0.7), tuple(c + d * 2.0 - side * 0.5), tuple(c + d * 3.6 + side * 0.3)], 0.6, 0.25, up=(0, 1, 0))


def halo_paimon(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HALO_R), C(0.4, S(6)))
    for i in range(4):
        a = i * math.pi / 2 + math.pi / 4
        p = k.part("halo", f"moon_{i}", "plate", smooth=45)
        ctr = (HALO_R * math.cos(a), 0.0, HALO_R * math.sin(a))
        crescent(p, ctr, 1.5, 0.9, 0.35, math.degrees(a) + 190, math.degrees(a) + 350, "xz", 6)
    for i in range(4):
        a = i * math.pi / 2
        orb(k.part("halo", f"star_{i}", "crystal", layer=2, smooth=0), (HALO_R * math.cos(a), 0.2, HALO_R * math.sin(a)), 0.5, sides=6)


HALOS = {"AMON": halo_amon, "VINEA": halo_vinea, "FOCALOR": halo_focalor, "ZAGAN": halo_zagan, "FURFUR": halo_furfur, "SITRI": halo_sitri, "BIFRON": halo_bifron, "PAIMON": halo_paimon}


def halo_dark(k, spikes=9):
    """The Dark Djinn's crown ring (ref 60 / 61): a black ring edged in gold, with black thorns turned inwards and a gold point turned out; a gem
    in the Djinn's accent colour at the front."""
    R = HALO_R
    du.sweep(k.part("halo", "ring", "plate", smooth=40), ring_xz(R, 0.0, 16), C(0.62, S(6)))
    du.sweep(k.part("halo", "edge_top", k.trim()), ring_xz(R * 1.01, 0.5, 16), C(0.26, S(5)))
    du.sweep(k.part("halo", "edge_bot", k.trim()), ring_xz(R * 1.01, -0.5, 16), C(0.26, S(5)))
    for i in range(spikes):
        a = i * 2 * math.pi / spikes
        c = np.array([R * math.cos(a), 0.0, R * math.sin(a)])
        d = np.array([math.cos(a), 0.0, math.sin(a)])
        spike(k.part("halo", f"thorn_{i}", "plate"), tuple(c), tuple(-d + np.array([0, 0.25, 0])), 2.4 + (i % 2) * 0.9, 0.55, 5)
        spike(k.part("halo", f"tip_{i}", k.trim()), tuple(c), tuple(d + np.array([0, 0.5, 0])), 1.6 + (i % 2) * 0.6, 0.38, 5)
    du.gem(k.part("halo", "gem", "crystal", layer=2, smooth=0), (0, 0.0, R + 0.2), 0.8, long=1.1, sides=6, axis="z")


# ============================================================================================ BACKDROPS (a vertical piece behind the back; centre at the origin, facing +z)
BAAL_RING_R = 12.0      # ref 59 tile 1: the blood ring is 0.9 x the head-to-hip height


def ring_xy(R, z=0.0, n=18, a0=0, a1=360):
    return ring_path((0, 0, z), R, a0, a1, "xy", n)


def back_baal(k):
    du.sweep(k.part("backdrop", "ring", "plate", smooth=40), ring_xy(BAAL_RING_R, 0.0, 20), C(0.8, S(6)))
    du.sweep(k.part("backdrop", "ring_edge", "crystal", layer=2), ring_xy(BAAL_RING_R, 0.35, 20), C(0.3, S(5)))
    du.sweep(k.part("backdrop", "inner", k.trim()), ring_xy(BAAL_RING_R * 0.82, 0.0, 20), C(0.35, S(5)))
    for i in range(8):
        a = i * math.pi / 4 + 0.2
        c = np.array([BAAL_RING_R * math.cos(a), BAAL_RING_R * math.sin(a), 0.0])
        d = np.array([math.cos(a), math.sin(a), 0.0])
        side = np.array([-math.sin(a), math.cos(a), 0.0])
        zig(k.part("backdrop", f"bolt_{i}", "crystal", layer=2), [tuple(c), tuple(c + d * 1.8 + side * 0.9), tuple(c + d * 3.0 - side * 0.7), tuple(c + d * 5.0 + side * 0.4)], 0.9, 0.35)
        spike(k.part("backdrop", f"thorn_{i}", "plate"), tuple(c), tuple(d), 3.0, 0.8, 5)
    for i in range(4):
        a = i * math.pi / 2 + 0.2
        du.gem(k.part("backdrop", f"gem_{i}", "crystal", layer=2, smooth=0), (BAAL_RING_R * 0.82 * math.cos(a), BAAL_RING_R * 0.82 * math.sin(a), 0.0), 0.9, long=1.1, sides=6, axis="z")


def back_amon(k):
    """AMON_ARCH: an arch of flame standing over the shoulders: eleven tongues along a semicircle and a thin gold arc under them."""
    R = 10.5
    du.sweep(k.part("backdrop", "arc", k.trim(), layer=2), ring_xy(R, 0.0, 14, -10, 190), C(0.5, S(6)))
    for i, a in enumerate(np.linspace(-6, 186, 11)):
        r = math.radians(a)
        c = (R * math.cos(r), R * math.sin(r), 0.0)
        tg = k.part("backdrop", f"flame_{i}", "crystal", layer=2, smooth=45)
        L = 4.6 + 2.2 * math.sin(math.radians(min(180, max(0, a))))
        tongue(tg, c, (math.cos(r), math.sin(r), 0.0), L, 2.2, 0.5, 1.0, i * 0.9, n=4)
        tg2 = k.part("backdrop", f"core_{i}", "dark", smooth=45)
        tongue(tg2, (c[0], c[1], -0.4), (math.cos(r), math.sin(r), 0.0), L * 0.45, 1.2, 0.4, 0.6, i * 0.9, n=3)


def back_vinea(k):
    """VINEA_BUBBLEARCH: a thin arc over the shoulders carrying seven pearl bubbles of falling size, a wave-crest blade at each foot."""
    R = 10.0
    du.sweep(k.part("backdrop", "arc", "trim"), ring_xy(R, 0.0, 14, -8, 188), C(0.35, S(5)))
    for i, a in enumerate(np.linspace(0, 180, 7)):
        r = math.radians(a)
        rr = 1.9 - 0.45 * abs(a - 90) / 90
        orb(k.part("backdrop", f"bubble_{i}", "bone", smooth=0), (R * math.cos(r), R * math.sin(r), 0.0), rr, sides=8)
        orb(k.part("backdrop", f"core_{i}", "crystal", layer=2, smooth=0), (R * math.cos(r) + 0.3, R * math.sin(r) + 0.3, 0.9), rr * 0.4, sides=6)
    for sgn, nm in ((1, "l"), (-1, "r")):
        p = k.part("backdrop", "crest_" + nm, "cloth", smooth=45)
        sweep_flat(p, [(sgn * R, 0, 0), (sgn * (R + 2.6), -2.6, 0), (sgn * (R + 3.6), -6.2, 0), (sgn * (R + 2.2), -9.4, 0)], 3.4, 0.5, pf_feather, (0, 0, 1))


def back_focalor(k):
    """FOCALOR_CYCLONE: two thin rings crossing at an angle, with six swept blades riding them: the gale round the shoulders."""
    for ri, (R, tilt) in enumerate(((9.5, 62), (8.0, -58))):
        path = ring_xy(R, 0.0, 18)
        t = math.radians(tilt)
        rot = np.array([[1, 0, 0], [0, math.cos(t), -math.sin(t)], [0, math.sin(t), math.cos(t)]])
        path = [tuple(rot @ np.array(p)) for p in path]
        du.sweep(k.part("backdrop", f"ring_{ri}", "plate", smooth=40), path, C(0.45, S(6)))
        du.sweep(k.part("backdrop", f"edge_{ri}", "crystal", layer=2), [(x, y, z + 0.2) for (x, y, z) in path], C(0.2, S(5)))
        for j in range(3):
            a = j * 2 * math.pi / 3 + ri
            c = rot @ np.array([R * math.cos(a), R * math.sin(a), 0.0])
            tdir = rot @ np.array([-math.sin(a), math.cos(a), 0.0])
            pb = k.part("backdrop", f"blade_{ri}{j}", "cloth", smooth=45)
            sweep_flat(pb, [tuple(c), tuple(c + tdir * 2.4 + (0, 0, 0.4)), tuple(c + tdir * 5.0 + (0, 0, 0.0)), tuple(c + tdir * 8.0 + (0, 0, -0.6))], 2.4, 0.4, pf_blade, (0, 0, 1))


def back_zagan(k):
    """ZAGAN_THORNARCH: a vine arch with long thorns, four flower buds and a trailing leaf at each foot."""
    R = 10.0
    pts = ring_xy(R, 0.0, 16, -12, 192)
    pts = [(x + 0.7 * math.sin(i * 1.7), y, z + 0.7 * math.cos(i * 1.7)) for i, (x, y, z) in enumerate(pts)]
    sweep_tube(k.part("backdrop", "vine", "extra", smooth=40), pts, 0.8, 0.6, 6)
    for i, a in enumerate(np.linspace(0, 180, 9)):
        r = math.radians(a)
        c = np.array([R * math.cos(r), R * math.sin(r), 0.0])
        spike(k.part("backdrop", f"thorn_{i}", "trim"), tuple(c), (math.cos(r), math.sin(r), 0.2 * (1 if i % 2 else -1)), 3.2 - 0.8 * (i % 2), 0.7, 5)
    for i, a in enumerate((25, 70, 110, 155)):
        r = math.radians(a)
        c = np.array([(R + 1.6) * math.cos(r), (R + 1.6) * math.sin(r), 0.6])
        orb(k.part("backdrop", f"bud_{i}", "crystal", layer=2, smooth=0), tuple(c), 1.2, sides=7)
        for j in range(5):
            b = j * 2 * math.pi / 5
            pt = k.part("backdrop", f"petal_{i}{j}", "cloth", smooth=45)
            sweep_flat(pt, [tuple(c), tuple(c + np.array([math.cos(b), math.sin(b), 0.4]) * 1.6), tuple(c + np.array([math.cos(b), math.sin(b), 0.3]) * 3.0)], 1.6, 0.3, pf_blade, (0, 0, 1))
    for sgn, nm in ((1, "l"), (-1, "r")):
        lf = k.part("backdrop", "foot_leaf_" + nm, "cloth")
        P = [(sgn * R, 0.0, 0.0), (sgn * (R + 3.5), -2.0, 0.0), (sgn * (R + 3.0), -7.0, 0.0), (sgn * (R - 0.5), -4.0, 0.0)]
        leaf(lf, P if sgn > 0 else P[::-1])


def back_zepar(k):
    """ZEPAR_SHARDCROWN: a ring of eight big crystal shards radiating from behind the shoulders, around a stone disc."""
    ds = k.part("backdrop", "disc", "extra", smooth=40)
    disc(ds, (0, 0, -0.4), 5.0, 1.6, "z", 14)
    collar(k.part("backdrop", "disc_rim", k.trim()), 0.6, 5.2, 0.5, lip=0.12, sides=S(14), axis="z", origin=(0, 0, -0.4))
    du.gem(k.part("backdrop", "disc_gem", "crystal", layer=2, smooth=0), (0, 0, 0.8), 1.5, long=1.2, sides=6, axis="z")
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        L = 12.0 if i % 2 == 0 else 8.5
        shard(k.part("backdrop", f"shard_{i}", "crystal", glow=(i % 2 == 0), smooth=40), (5.2 * math.cos(a), 5.2 * math.sin(a), 0.0), (math.cos(a), math.sin(a), 0.0), L, 3.2 if i % 2 == 0 else 2.4)


def back_furfur(k):
    """FURFUR_RESONANCE: three concentric rings stepped back along z (a speaker's waves), ticks between them."""
    for i, (R, z) in enumerate(((5.5, 0.6), (8.5, -0.6), (11.5, -1.8))):
        du.sweep(k.part("backdrop", f"wave_{i}", "plate", smooth=40), ring_xy(R, z, 12, 12, 348), C(0.5 - 0.08 * i, 4))
        if i == 2:
            du.sweep(k.part("backdrop", f"edge_{i}", "crystal", layer=2), ring_xy(R, z + 0.3, 12, 12, 348), C(0.2, 4))
    for i in range(8):
        a = i * math.pi / 4
        du.bar(k.part("backdrop", f"tick_{i}", k.trim()), (6.0 * math.cos(a), 6.0 * math.sin(a), 0.3), (10.5 * math.cos(a), 10.5 * math.sin(a), -1.4), 0.5, 0.25, up=(0, 0, 1), taper=0.8)
    du.gem(k.part("backdrop", "core", "crystal", layer=2, smooth=0), (0, 0, 0.6), 1.6, long=1.2, sides=6, axis="z")


def back_marchosias(k):
    """MARCHOSIAS_EMBERMANE: a row of seven fire spikes down the spine, tallest at the nape, glowing; the author frame's y runs down the back."""
    for i in range(7):
        y = 3.0 - i * 2.4
        L = 7.5 - i * 0.7
        tg = k.part("backdrop", f"mane_{i}", "crystal", layer=2, smooth=45)
        tongue(tg, (0, y, -0.4), (0.0, 0.25, -1.0), L, 2.6 - i * 0.12, 0.6, 0.7, i * 0.8, plane="yz", n=5)
        sp = k.part("backdrop", f"spike_{i}", "plate", smooth=40)
        spike(sp, (0, y, -0.2), (0.0, 0.1, -1.0), L * 0.5, 0.9, 5)
        du.stud(k.part("backdrop", f"base_{i}", k.trim()), (0, y, 0.6), 0.8, 0.5, (0, 0, 1), 6)


def back_sitri(k):
    """SITRI_SUNARMS: the two extra golden arms of ref 61 (an upper arm, an elbow, a forearm with a vambrace, a hand) raised out to the sides, and a
    sun disc with twelve rays behind the head."""
    ds = k.part("backdrop", "sun", "trim", smooth=40)
    disc(ds, (0, 5.0, -0.8), 4.4, 1.0, "z", 14)
    du.gem(k.part("backdrop", "sun_gem", "crystal", layer=2, smooth=0), (0, 5.0, 0.0), 1.5, long=1.2, sides=6, axis="z")
    for i in range(12):
        a = i * math.pi / 6
        L = 7.0 if i % 2 == 0 else 4.6
        spike(k.part("backdrop", f"ray_{i}", "crystal", glow=(i % 2 == 0), smooth=40), (4.6 * math.cos(a), 5.0 + 4.6 * math.sin(a), -0.8), (math.cos(a), math.sin(a), 0.0), L, 0.9 if i % 2 == 0 else 0.7, 5)
    for sgn, nm in ((1, "l"), (-1, "r")):
        sh = np.array([sgn * 4.4, -1.0, -0.6])
        el = sh + np.array([sgn * 4.6, -0.6, 1.6])
        wr = el + np.array([sgn * 3.4, 3.2, 3.2])
        tube(k.part("backdrop", "upper_" + nm, "plate", smooth=40), [tuple(sh), tuple((sh + el) / 2 + (0, 0.4, 0.4)), tuple(el)], 1.15, 0.95, 8)
        orb(k.part("backdrop", "elbow_" + nm, k.trim(), smooth=40), tuple(el), 1.1, sides=8)
        tube(k.part("backdrop", "fore_" + nm, "plate", smooth=40), [tuple(el), tuple((el + wr) / 2), tuple(wr)], 1.0, 0.8, 8)
        for j, f in enumerate((0.25, 0.7)):
            c = el + (wr - el) * f
            collar(k.part("backdrop", f"band_{nm}{j}", k.trim()), 0, 1.15, 0.7, lip=0.2, sides=S(8), axis=tuple(wr - el), origin=tuple(c))
        du.box(k.part("backdrop", "hand_" + nm, "plate"), (wr[0] - 0.9, wr[1] - 0.5, wr[2] - 0.7), (wr[0] + 0.9, wr[1] + 1.8, wr[2] + 0.7), chamfer=0.3)
        du.gem(k.part("backdrop", "hand_gem_" + nm, "crystal", layer=2, smooth=0), (wr[0], wr[1] + 0.6, wr[2] + 0.8), 0.45, long=0.8, sides=6, axis="z")
        sh_p = k.part("backdrop", "cuff_" + nm, k.trim())
        collar(sh_p, 0, 1.4, 0.8, lip=0.2, sides=S(8), axis=tuple(el - sh), origin=tuple(sh))


def back_bifron(k):
    """BIFRON_STORMFAN: five great lightning bolts fanning out from behind the head, the glow running down each."""
    for i, a in enumerate((-64, -32, 0, 32, 64)):
        r = math.radians(a)
        d = np.array([math.sin(r), math.cos(r), 0.0])
        side = np.array([math.cos(r), -math.sin(r), 0.0])
        L = 16.0 - abs(a) * 0.08
        pts = [np.array([0.0, 4.0, 0.0]) + d * L * f + side * off for f, off in ((0.05, 0), (0.28, 1.5), (0.42, -1.2), (0.65, 1.8), (0.8, -1.0), (1.0, 0.6))]
        zig(k.part("backdrop", f"bolt_{i}", "plate"), [tuple(p) for p in pts], 2.2, 0.8)
        zig(k.part("backdrop", f"core_{i}", "crystal", layer=2), [(p[0], p[1], p[2] + 0.5) for p in pts], 0.7, 0.3)
    du.gem(k.part("backdrop", "node", "crystal", layer=2, smooth=0), (0, 4.0, 0.2), 1.3, long=1.2, sides=6, axis="z")


def back_paimon(k):
    """PAIMON_MOON: a full moon disc behind the head with a thin ring round it and stars on the ring (ref 59 tile 9)."""
    disc(k.part("backdrop", "moon", "bone", smooth=40), (0, 5.0, -0.8), 5.6, 1.0, "z", 16)
    du.sweep(k.part("backdrop", "ring", k.trim()), ring_xy(8.2, -0.4, 20), C(0.35, S(5)))
    du.sweep(k.part("backdrop", "ring_glow", "crystal", layer=2), ring_xy(8.2, -0.1, 20), C(0.18, S(5)))
    for j, (x, y, r) in enumerate(((-1.8, 6.4, 1.0), (1.5, 3.8, 0.7), (1.9, 6.6, 0.45), (-1.5, 3.4, 0.5))):
        du.sweep(k.part("backdrop", f"crater_{j}", "plate"), [(x + r * math.cos(a), 5.0 + (y - 5.0) + r * math.sin(a), 0.0) for a in np.linspace(0, 2 * math.pi, 9)], C(0.14, 4))
    for i in range(5):
        a = i * 2 * math.pi / 5 + 0.3
        c = (8.2 * math.cos(a), 5.0 * 0 + 8.2 * math.sin(a), -0.4)
        du.gem(k.part("backdrop", f"star_{i}", "crystal", layer=2, smooth=0), c, 0.55, long=1.3, sides=4, axis="z")


def back_vassago(k):
    """VASSAGO_REAPERMOON: a huge scythe-moon, a crescent blade standing upright behind the shoulders (ref 59 tile 7), with a spike on each horn."""
    cres = k.part("backdrop", "blade", "plate", smooth=40)
    crescent(cres, (0, 0, 0), 12.0, 5.0, 0.9, 30, 330, "xy", 18)
    crescent(k.part("backdrop", "edge", "crystal", layer=2), (0, 0, 0.3), 12.0 + 1.6, 0.8, 0.3, 30, 330, "xy", 18)
    for sgn, nm in ((1, "a"), (-1, "b")):
        a = math.radians(30 if sgn > 0 else 330)
        spike(k.part("backdrop", "horn_" + nm, k.trim()), (12.0 * math.cos(a), 12.0 * math.sin(a), 0.0), (math.cos(a) * 0.3, math.sin(a) * 0.3 + (1 if sgn > 0 else -1) * 0.9, 0.0), 4.0, 1.0, 6)
    du.sweep(k.part("backdrop", "inner_ring", k.trim()), ring_xy(8.0, 0.0, 12, 40, 320), C(0.3, 4))
    for i, a in enumerate(range(70, 300, 38)):                      # six teeth along the inner edge, a gold band across the blade under each pair
        r = math.radians(a)
        spike(k.part("backdrop", f"tooth_{i}", "bone"), (9.6 * math.cos(r), 9.6 * math.sin(r), 0.0), (-math.cos(r), -math.sin(r), 0.0), 2.4, 0.55, 5)
    for i, a in enumerate((100, 180, 260)):
        r = math.radians(a)
        collar(k.part("backdrop", f"band_{i}", k.trim()), 0, 2.6, 0.8, lip=0.2, sides=5, axis=(-math.sin(r), math.cos(r), 0.0), origin=(12.0 * math.cos(r), 12.0 * math.sin(r), 0.0))
    du.gem(k.part("backdrop", "eye", "crystal", layer=2, smooth=0), (0, 0, 0.5), 1.6, long=1.1, sides=6, axis="z")


BACKS = {"BAAL": back_baal, "AMON": back_amon, "VINEA": back_vinea, "FOCALOR": back_focalor, "ZAGAN": back_zagan, "ZEPAR": back_zepar, "FURFUR": back_furfur,
         "MARCHOSIAS": back_marchosias, "SITRI": back_sitri, "BIFRON": back_bifron, "PAIMON": back_paimon, "VASSAGO": back_vassago}


# ============================================================================================ MANTLES (body frame: the neck centre; the body is x +-4, y 0..-12, z +-2)
def cape(k, length=26.0, width=9.0, mat="cloth", wave=1.0, hem=None):
    p = k.part("mantle", "cape", mat, uv_mode="loft")
    path = [(0, -0.5, -2.6), (0, -4.0, -3.6), (0, -10.0, -4.4 * wave), (0, -18.0, -5.2 * wave), (0, -length, -4.6 * wave)]
    du.sweep(p, curve(path, 6), ml.rect_ring(1.0, 0.3), scales=lambda t: (width * (0.75 + 0.5 * t), 1.0))
    if hem:
        du.sweep(k.part("mantle", "hem", hem), curve([(0, -length - 0.2, -4.6 * wave)], 2) if False else [(-width * 0.62, -length, -4.6 * wave), (0, -length - 0.2, -4.6 * wave), (width * 0.62, -length, -4.6 * wave)], ml.rect_ring(0.8, 0.5))


def mantle_amon(k):
    cape(k, 26.0, 8.0, "cloth", 1.0, k.trim())
    du.sweep(k.part("mantle", "sash", "cloth"), curve([(-4.2, -2.0, 2.4), (0.0, -6.0, 2.9), (4.4, -10.5, 2.4)], 6), ml.rect_ring(2.2, 0.3))
    for sgn in (-1, 1):
        du.sweep(k.part("mantle", "collar_" + ("l" if sgn > 0 else "r"), k.trim()), curve([(sgn * 1.6, 0.3, 2.4), (sgn * 3.4, 0.1, 1.2), (sgn * 4.4, -0.2, -1.4)], 4), ml.rect_ring(1.0, 0.7))


def mantle_vinea(k):
    """VINEA_SCARVES: two long water-scarves draping from the shoulders down the front and trailing behind in waves."""
    for sgn, nm in ((1, "l"), (-1, "r")):
        p = k.part("mantle", "scarf_" + nm, "cloth", uv_mode="loft")
        path = [(sgn * 3.6, 0.4, -1.0), (sgn * 4.6, -1.6, 1.4), (sgn * 4.0, -7.0, 3.0), (sgn * 5.6, -13.0, 2.4), (sgn * 4.6, -20.0, 0.0), (sgn * 6.6, -26.0, -3.0)]
        du.sweep(p, curve(path, 6), ml.rect_ring(1.0, 0.25), scales=lambda t: (2.8 * (1 - 0.35 * t), 1.0))
        du.sweep(k.part("mantle", "edge_" + nm, "crystal", layer=2), curve([(x, y, z + 0.2) for (x, y, z) in path], 6), ml.rect_ring(0.3, 0.2))
    for i in range(3):
        orb(k.part("mantle", f"drop_{i}", "crystal", layer=2, smooth=0), (-5.0 + i * 5.0, -3.0 - i * 2.0, 3.6), 0.6, sides=6)


def mantle_focalor(k):
    """FOCALOR_SERPENT: a scarf coiling round the neck and shoulder, then streaming behind (ref 59 tile 4: the serpent of white cloth)."""
    path = [(-4.6, -1.0, 2.6), (-2.0, 0.6, 3.2), (2.0, 0.8, 3.0), (4.6, -0.8, 1.8), (4.2, -3.4, -1.6), (0.0, -2.0, -3.8), (-3.6, -4.2, -3.0), (-5.0, -9.0, -4.4), (-2.0, -15.0, -6.0),
            (3.0, -20.0, -7.0)]
    du.sweep(k.part("mantle", "scarf", "cloth", uv_mode="loft"), curve(path, 6), ml.rect_ring(1.0, 0.3), scales=lambda t: (2.6 * (1 - 0.5 * t), 1.0))
    du.sweep(k.part("mantle", "scarf_edge", "crystal", layer=2), curve([(x, y, z + 0.25) for (x, y, z) in path], 6), ml.rect_ring(0.25, 0.2))
    orb(k.part("mantle", "knot", k.trim(), smooth=40), (4.4, -1.0, 2.2), 1.0, sides=7)


def mantle_zagan(k):
    """ZAGAN_LEAFRUFF: ten overlapping leaves round the neck and shoulders, big at the back."""
    for i in range(10):
        a = i * 2 * math.pi / 10
        c = np.array([math.sin(a) * 3.8, 0.2, math.cos(a) * 2.4])
        d = np.array([math.sin(a) * 1.0, -0.8, math.cos(a) * 0.6])
        lf = k.part("mantle", f"leaf_{i}", "cloth")
        side = unit(np.cross(d, (0, 1, 0)))
        L = 5.0 + (2.0 if math.cos(a) < 0 else 0.0)
        P = [tuple(c), tuple(c + d * L * 0.3 + side * 1.8), tuple(c + d * L * 0.75 + side * 1.4), tuple(c + d * L), tuple(c + d * L * 0.75 - side * 1.4), tuple(c + d * L * 0.3 - side * 1.8)]
        leaf(lf, P)
        du.bar(k.part("mantle", f"vein_{i}", "trim"), tuple(c), tuple(c + d * L * 0.85 + (0, 0, 0.1)), 0.3, 0.12, up=(0, 0, 1), taper=1.0)


def mantle_zepar(k):
    """ZEPAR_GORGET: a heavy gorget collar with six big spikes sweeping up and out."""
    collar(k.part("mantle", "gorget", "extra"), -0.3, 3.8, 1.6, lip=0.3, sides=S(14), oval=0.62)
    collar(k.part("mantle", "rim", k.trim()), 1.1, 4.0, 0.5, lip=0.12, sides=S(14), oval=0.62)
    for i in range(6):
        a = i * math.pi / 3 + math.pi / 6
        spike(k.part("mantle", f"spike_{i}", "extra"), (math.sin(a) * 3.6, 0.8, math.cos(a) * 2.3), (math.sin(a) * 1.1, 0.9, math.cos(a) * 0.5), 4.6 if i % 3 == 1 else 3.4, 1.0, 6)


def mantle_furfur(k):
    """FURFUR_STREAMERS: four ribbons from the shoulders down the back, a small bell at the end of each."""
    for i, x in enumerate((-3.4, -1.2, 1.2, 3.4)):
        p = k.part("mantle", f"ribbon_{i}", "cloth", uv_mode="loft")
        L = 20.0 - 3.0 * (i % 2)
        path = [(x, -0.4, -2.2), (x * 1.2, -4.0, -3.4), (x * 1.5, -10.0, -4.4), (x * 1.8, -L, -3.8)]
        du.sweep(p, curve(path, 6), ml.rect_ring(1.0, 0.25), scales=lambda t: (1.8 - 0.9 * t, 1.0))
        ml.loft(k.part("mantle", f"bell_{i}", k.trim()), "-y", (x * 1.8, -L, -3.8), [(0, 0.4, 0.4), (1.2, 0.9, 0.9), (2.2, 1.0, 1.0)], C(1.0, S(8)), caps=(False, True))


def mantle_marchosias(k):
    """MARCHOSIAS_RUFF: a big fur ruff: fourteen fur blades round the neck, long over the shoulders."""
    for i in range(14):
        a = i * 2 * math.pi / 14
        c = np.array([math.sin(a) * 3.7, 0.1, math.cos(a) * 2.5])
        d = np.array([math.sin(a) * 1.0, -0.5, math.cos(a) * 0.45])
        L = 5.2 + (1.8 if abs(math.sin(a)) > 0.6 else 0.0)
        spike(k.part("mantle", f"fur_{i}", "cloth", smooth=45), tuple(c), tuple(d), L, 1.2, 5)
    collar(k.part("mantle", "band", "extra"), -0.4, 3.6, 0.9, lip=0.2, sides=S(14), oval=0.66)


def mantle_paimon(k):
    """PAIMON_FURCOLLAR: the white fur collar of ref 60: sixteen broad flat blades draped over the shoulders and chest."""
    for i in range(16):
        a = i * 2 * math.pi / 16
        c = np.array([math.sin(a) * 3.6, 0.3, math.cos(a) * 2.4])
        d = np.array([math.sin(a) * 1.0, -0.55, math.cos(a) * 0.5])
        L = 5.6 + (2.2 if abs(math.sin(a)) > 0.5 else 0.0)
        p = k.part("mantle", f"fur_{i}", "cloth", smooth=45)
        side = unit(np.cross(d, (0, 1, 0)))
        sweep_flat(p, [tuple(c), tuple(c + d * L * 0.5 + (0, -0.4, 0)), tuple(c + d * L + (0, -1.2, 0))], 2.8, 0.7, pf_feather, tuple(unit(np.cross(d, side))))
    collar(k.part("mantle", "band", k.trim()), -0.3, 3.5, 0.6, lip=0.15, sides=S(14), oval=0.66)


def mantle_vassago(k):
    """VASSAGO_SHROUD: a tattered shroud from the shoulders, its hem torn into seven long tails."""
    p = k.part("mantle", "shroud", "cloth", uv_mode="loft")
    path = [(0, -0.5, -2.6), (0, -5.0, -3.8), (0, -12.0, -4.8)]
    du.sweep(p, curve(path, 6), ml.rect_ring(1.0, 0.3), scales=lambda t: (9.5 + 2.0 * t, 1.0))
    for i, x in enumerate(np.linspace(-5.0, 5.0, 7)):
        t = k.part("mantle", f"tail_{i}", "cloth", smooth=45)
        sweep_flat(t, [(x, -11.5, -4.6), (x * 1.1, -16.0 - (i % 3) * 3.0, -5.0), (x * 1.2, -21.0 - (i % 3) * 4.5, -4.6)], 2.4, 0.3, pf_taper, (0, 0, 1))
    for sgn in (-1, 1):
        orb(k.part("mantle", "clasp_" + ("l" if sgn > 0 else "r"), "bone", smooth=0), (sgn * 3.2, -0.2, 2.0), 0.9, sides=7)


MANTLES = {"AMON": mantle_amon, "VINEA": mantle_vinea, "FOCALOR": mantle_focalor, "ZAGAN": mantle_zagan, "ZEPAR": mantle_zepar, "FURFUR": mantle_furfur,
           "MARCHOSIAS": mantle_marchosias, "PAIMON": mantle_paimon, "VASSAGO": mantle_vassago}

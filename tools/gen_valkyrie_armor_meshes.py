#!/usr/bin/env python3
"""
VALKYRIE ARMOUR (Noelle's Valkyrie Dress) as a player-fitted 3D render-layer armour = PREVIEW ONLY (nothing is registered in the game, nothing goes to src/ or assets/).

The reference is the anime still of the Valkyrie Dress (docs/previews/vfx_remake/valkyrie/valkyrie_ref_crop.png, the same picture as Valkyrie_Dress.webp of the elemzip pack):
two long, narrow, translucent crystal-blade wings sweeping up and outwards from the shoulder blades in a V, a spiked tiara, a short ruffled petal bodice and skirt in frosty aqua-white, broad layered
shoulder pieces, ice-cuffed gauntlets and spiky water greaves, a glowing flat grimoire at the hip.  Details and sources: docs/vfx_remake/valkyrie_wings.md.

Authored with the `Mesh` class of tools/gen_sdw3_models.py, written in the same `q` text format (q <layer> (x y z u v)x4 nx ny nz r g b), layers: 0 crystal body (3-tone toon, emissive floor),
1 emissive (glow edges, inner veins, tips; drawn additive/emissive), 2 foam / frost highlights.  Units: model pixels (1/16 block), REST pose of the vanilla player model:
y up, feet at y = 0, torso y 12..24 (x +-4, z +-2), head y 24..32, arms hang from the shoulder pivots (+-6, 22, 0), legs from (+-2, 12, 0), FRONT = +z.
Only the LEFT (+x) copy of a paired piece is written (`valk_<piece>.txt`); the right copy is the mirror in x (the layer draws copies = 2, like ModeArmorWindV2).

Three levels of detail per piece: no suffix = NEAR, `_mid`, `_lo` = FAR.  The wings carry NO stored sway weights: the layer derives the weight of a vertex from its distance to the wing hinge
(w = |v - hinge| / wing length) and bends / sweeps it by that much, so the flutter needs no extra data.

    python tools/gen_valkyrie_armor_meshes.py   ->  docs/previews/vfx_remake/valkyrie/v1/meshes/valk_*.txt, pieces.json, counts.txt
"""
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_sdw3_models import Mesh, WHITE as REGION, add, sub, mul, cross, dot, norm, mean    # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
OUT = os.path.join(ROOT, "docs", "previews", "vfx_remake", "valkyrie", "v1", "meshes")        # v1 (stiff faceted crystal wings), kept; the redo is tools/gen_valkyrie_armor_v2.py

W = (255, 255, 255)          # frost white
P = (214, 244, 255)          # pale aqua
A = (170, 226, 250)          # aqua
D = (122, 196, 238)          # deep aqua
G = (200, 255, 255)          # glow
UVQ = [(0, 1), (1, 1), (1, 0), (0, 0)]


def hsh(*a):
    s = 0.0
    for i, x in enumerate(a):
        s += (x + 1.37) * (12.9898 + 7.7 * i)
    v = math.sin(s) * 43758.5453
    return v - math.floor(v)


def q(m, layer, pts, rgb, ref=None):
    m.quad(layer, pts, [(0, 1), (1, 1), (1, 0), (0, 0)], rgb, ref=ref)


# ============================================================================================ primitives
def ring(y, hx, hz, n=8, cx=0.0, cz=0.0, rot=0.0, sq=0.5):
    """a rounded-rectangle ring of n points (superellipse, exponent 1/sq): the section of a box-ish piece."""
    out = []
    for k in range(n):
        a = rot + 2 * math.pi * k / n
        c, s = math.cos(a), math.sin(a)
        out.append((cx + hx * math.copysign(abs(c) ** sq, c), y, cz + hz * math.copysign(abs(s) ** sq, s)))
    return out


def loft(m, layer, secs, rgb, closed=True):
    n = len(secs[0])
    cs = [mean(s) for s in secs]
    for i in range(len(secs) - 1):
        ref = mean([cs[i], cs[i + 1]])
        for j in range(n if closed else n - 1):
            k = (j + 1) % n
            q(m, layer, [secs[i][j], secs[i][k], secs[i + 1][k], secs[i + 1][j]], rgb, ref=ref)


def leaf(m, layer, base, d, o, L, wd, th, rgb, stages=1, rgb_tip=None):
    """a petal / thorn: triangular section (left, right, ridge toward o) tapering to a point at base + d*L.  stages 1: 3 quads, 2: a bulge at 0.4 of the length (6 quads)."""
    d = norm(d)
    side = norm(cross(d, o))
    nr = norm(cross(side, d))
    prof = [(0.0, 0.8, 1.0), (0.45, 1.0, 1.0), (0.82, 0.55, 0.7), (1.0, 0.0, 0.0)] if stages > 2 else [(0.0, 0.9, 1.0), (0.42, 1.0, 1.0), (1.0, 0.0, 0.0)] if stages > 1 else [(0.0, 1.0, 1.0), (1.0, 0.0, 0.0)]
    secs = []
    for f, wf, tf in prof:
        c = add(base, mul(d, L * f))
        secs.append([add(c, mul(side, wd * wf)), sub(c, mul(side, wd * wf)), add(c, mul(nr, th * tf))])
    ref = mean([secs[0][0], secs[0][1], secs[-1][0], add(base, mul(nr, -th * 0.3))])
    for i in range(len(secs) - 1):
        for j in range(3):
            k = (j + 1) % 3
            col = rgb if rgb_tip is None or i == 0 else rgb_tip
            q(m, layer, [secs[i][j], secs[i][k], secs[i + 1][k], secs[i + 1][j]], col, ref=add(ref, mul(d, 0.0)))


def slab(m, layer, c, ax, ay, hw, hh, ht, rgb):
    """an oriented box: centre c, width axis ax, height axis ay, half sizes hw, hh, ht (along cross(ax, ay))."""
    ax, ay = norm(ax), norm(ay)
    az = norm(cross(ax, ay))
    pts = {}
    for sx in (-1, 1):
        for sy in (-1, 1):
            for sz in (-1, 1):
                pts[(sx, sy, sz)] = add(c, add(mul(ax, sx * hw), add(mul(ay, sy * hh), mul(az, sz * ht))))
    faces = [((1, -1, -1), (1, 1, -1), (1, 1, 1), (1, -1, 1)), ((-1, -1, 1), (-1, 1, 1), (-1, 1, -1), (-1, -1, -1)), ((-1, 1, -1), (-1, 1, 1), (1, 1, 1), (1, 1, -1)),
             ((-1, -1, 1), (-1, -1, -1), (1, -1, -1), (1, -1, 1)), ((-1, -1, -1), (-1, 1, -1), (1, 1, -1), (1, -1, -1)), ((1, -1, 1), (1, 1, 1), (-1, 1, 1), (-1, -1, 1))]
    for f in faces:
        q(m, layer, [pts[k] for k in f], rgb, ref=c)


def petal_ring(m, y, hx, hz, n, flare_deg, L, wd, th, rgb, rot=0.0, stages=1, droop=0.0, front_bias=0.0, rgb_tip=None, sway_seed=0):
    """n petals round an elliptical ring at height y, each flaring out by flare_deg from straight down and growing from the surface."""
    for k in range(n):
        a = rot + 2 * math.pi * k / n
        c, s = math.cos(a), math.sin(a)
        base = (hx * c, y, hz * s)
        o = norm((c / hx, 0.0, s / hz))
        f = math.radians(flare_deg + 10 * (hsh(k, n, y) - 0.5))
        lk = L * (0.9 + 0.2 * hsh(k, y, 3)) * (1.0 + front_bias * max(0.0, s))
        d = norm(add(mul(o, math.sin(f)), (0.0, -math.cos(f) - droop, 0.0)))
        leaf(m, 0, base, d, o, lk, wd, th, rgb if k % 2 == 0 else P, stages=stages, rgb_tip=rgb_tip)



def ruffle(m, y, hx, hz, n, flare, L, rise, rgb, rgb2, rot=0.0, droop=0.0, lay=0, cx=0.0):
    """one tier of a frill: n folded petals (two quads each, a ridge raised `rise` px at the top centre) hanging from an elliptical ring at height y; the tip of petal j sits half a petal
    along the ring, flared out by `flare` px and L px lower, so the tiers read as scalloped frosty feathers, not thorns."""
    for j in range(n):
        a0 = rot + 2 * math.pi * j / n
        a1 = rot + 2 * math.pi * (j + 1) / n
        am = (a0 + a1) / 2
        p0 = (cx + hx * math.cos(a0), y, hz * math.sin(a0))
        p1 = (cx + hx * math.cos(a1), y, hz * math.sin(a1))
        o = norm((math.cos(am) / hx, 0.0, math.sin(am) / hz))
        k = 0.94 + 0.12 * hsh(j, n, int(y * 3))
        tip = (cx + hx * math.cos(am) + o[0] * flare * k, y - L * k - droop, hz * math.sin(am) + o[2] * flare * k)
        ridge = (mean([p0, p1])[0] + o[0] * rise, y - 0.35, mean([p0, p1])[2] + o[2] * rise)
        c = rgb if j % 2 == 0 else rgb2
        ref = (mean([p0, p1, tip])[0] - o[0] * 1.6, mean([p0, p1, tip])[1], mean([p0, p1, tip])[2] - o[2] * 1.6)
        tg = norm(sub(p1, p0))
        half = 0.17 * math.dist(p0, p1)
        tl, tr_ = sub(tip, mul(tg, half)), add(tip, mul(tg, half))
        tm = (tip[0] + o[0] * 0.5, tip[1] - 0.9, tip[2] + o[2] * 0.5)
        q(m, lay, [p0, ridge, tm, tl], c, ref=ref)
        q(m, lay, [ridge, p1, tr_, tm], rgb2 if j % 2 == 0 else rgb, ref=ref)

# ============================================================================================ the wings
def lead_curve(s, H, L, th0, th1, lean, u):
    """position and unit direction of the leading edge at u (0 hinge .. 1 tip): leaning out from the vertical by th0..th1 degrees, falling back by `lean` per unit length."""
    n = 48
    p = H
    d = None
    for i in range(int(n * u) + 1):
        uu = i / n
        th = math.radians(th0 + (th1 - th0) * uu ** 1.5)
        d = norm((s * math.sin(th), math.cos(th), -lean))
        if i < int(n * u):
            p = add(p, mul(d, L / n))
    rem = u * n - int(n * u)
    if rem > 1e-9 and d is not None:
        p = add(p, mul(d, L / n * rem))
    return p, d


def wing(name, s, H, L, th0, th1, lean, wmax, tilt, U, lam, nv, thick, ribs, down=False, shift=1.8, facet=0.22, seed=0):
    """A thick translucent crystal wing blade.  Leading edge = a curved spar from the hinge H to the tip; the membrane hangs off it toward the body, as wide as wmax near the root, with a saw of
    pointed lobes along the trailing edge (lam = lobe factor per column), front and back faces faceted by a deterministic jitter, closed by edge strips (layer 1: the glowing outline), with glowing
    veins (layer 1 ribbons) inside the glass: one along the spar and one fanning out to each lobe tip."""
    m = Mesh(name)
    cols = []
    for i, u in enumerate(U):
        p, d = lead_curve(s, H, L, th0, th1, lean, u)
        if down:
            d = (d[0], -d[1], d[2])
        cols.append([u, p, d])
    if down:                                         # the lower lobes hang from the hinge: mirror the curve in y about the hinge
        for c in cols:
            c[1] = (c[1][0], 2 * H[1] - c[1][1], c[1][2])
    G3 = []
    N = len(cols)
    tp = math.radians(tilt)
    for i, (u, p, d) in enumerate(cols):
        n0 = (0.0, math.sin(tp), math.cos(tp))
        n = norm(sub(n0, mul(d, dot(n0, d))))
        a = norm(cross(n, d))
        if a[0] * s > 0:
            a = mul(a, -1)
        n = norm(cross(d, a)) if dot(cross(d, a), n0) > 0 else norm(cross(a, d))
        w = wmax * ((1 - u) ** 0.9) * (0.62 + 0.38 * min(1.0, u / 0.25)) * lam[i]
        hs = thick * (1.0 - 0.55 * u)
        row = []
        for j in range(nv + 1):
            v = j / nv
            pos = add(p, mul(a, w * v))
            tipk = 1.0 if lam[i] >= 0.99 else 0.0
            pos = add(pos, mul(d, shift * tipk * v ** 3 * (1 - u)))              # lobe tips lean forward along the wing: pointed teeth
            hh = hs * (1.0 - 0.72 * v) + (0.28 * hs if j % 2 == 1 and 0 < j < nv else 0.0)
            jf = facet * hs * (hsh(i, j, seed) - 0.5) * 2 * (1 if 0 < j < nv else 0.4)
            jb = facet * hs * (hsh(i, j, seed + 7) - 0.5) * 2 * (1 if 0 < j < nv else 0.4)
            row.append((pos, add(pos, mul(n, hh + jf)), sub(pos, mul(n, hh + jb)), n, a, d))
        G3.append(row)
    # faces
    for i in range(N - 1):
        for j in range(nv):
            mid = [G3[i][j][0], G3[i + 1][j][0], G3[i + 1][j + 1][0], G3[i][j + 1][0]]
            ref = mean(mid)
            tone = [W, P, A][(i + j) % 3] if j else W
            q(m, 0, [G3[i][j][1], G3[i + 1][j][1], G3[i + 1][j + 1][1], G3[i][j + 1][1]], tone, ref=ref)
            q(m, 0, [G3[i][j][2], G3[i + 1][j][2], G3[i + 1][j + 1][2], G3[i][j + 1][2]], [P, A, D][(i + j) % 3], ref=ref)
    # glowing outline: leading edge and the saw of the trailing edge (layer 1)
    for i in range(N - 1):
        ref = mean([G3[i][nv // 2 + 0][0], G3[i + 1][nv // 2][0]])
        q(m, 1, [G3[i][0][1], G3[i + 1][0][1], G3[i + 1][0][2], G3[i][0][2]], G, ref=ref)
        q(m, 1, [G3[i][nv][1], G3[i + 1][nv][1], G3[i + 1][nv][2], G3[i][nv][2]], G, ref=ref)
    # veins inside the glass (layer 1 ribbons in the mid plane, seen through the front face)
    def rib(path, ww):
        for k in range(len(path) - 1):
            (p0, a0), (p1, a1) = path[k], path[k + 1]
            q(m, 1, [sub(p0, mul(a0, ww)), add(p0, mul(a0, ww)), add(p1, mul(a1, ww)), sub(p1, mul(a1, ww))], G)
    def at(i, v):
        j = min(nv, max(0, int(round(v * nv))))
        a_ = G3[i][j][4]
        return (G3[i][j][0], a_)
    if ribs >= 1:
        rib([(add(G3[i][0][0], mul(G3[i][0][4], 0.5)), G3[i][0][5]) for i in range(N - 1)], 0.38)          # the bone: along the spar
    tips = [i for i in range(1, N - 1) if lam[i] >= 0.99]
    for ti in tips[:max(0, ribs - 1)]:
        path = []
        for k in range(4):
            f = k / 3
            i = int(round(ti * f)) if k else 0
            v = (0.15 + 0.85 * f) if k < 3 else 1.0
            jj = min(nv, int(round(v * nv)))
            pos = add(G3[i][jj][0], mul(G3[i][jj][3], 0.0))
            path.append((pos, G3[i][jj][4]))
        path[-1] = (G3[ti][nv][0], G3[ti][nv][4])
        rib(path, 0.28)
    # frost sparkle: a few foam facets (layer 2) on the front face of the thick part
    return m


def wing_main(lod):
    s = 1.0
    if lod == 0:
        U = [0.0, 0.1, 0.2, 0.3, 0.42, 0.54, 0.66, 0.78, 0.9, 1.0]
        lam = [0.82, 0.62, 1.0, 0.64, 1.0, 0.66, 1.0, 0.7, 0.96, 0.0]
        nv, ribs = 2, 4
    elif lod == 1:
        U = [0.0, 0.18, 0.36, 0.56, 0.78, 1.0]
        lam = [0.82, 0.6, 1.0, 0.7, 1.0, 0.0]
        nv, ribs = 2, 2
    else:
        U = [0.0, 0.34, 0.68, 1.0]
        lam = [0.82, 1.0, 1.0, 0.0]
        nv, ribs = 1, 0
    return wing("valk_wing_main" + ["", "_mid", "_lo"][lod], s, (3.4, 21.6, -2.8), 34.0, 22.0, 46.0, 0.08, 15.0, 10.0, U, lam, nv, 1.25, ribs, seed=3)


def wing_low(lod):
    if lod == 0:
        U = [0.0, 0.2, 0.42, 0.68, 1.0]
        lam = [0.8, 0.62, 1.0, 1.0, 0.0]
        nv, ribs = 1, 2
    elif lod == 1:
        U = [0.0, 0.34, 0.68, 1.0]
        lam = [0.8, 1.0, 1.0, 0.0]
        nv, ribs = 1, 1
    else:
        U = [0.0, 0.5, 1.0]
        lam = [0.8, 1.0, 0.0]
        nv, ribs = 1, 0
    return wing("valk_wing_low" + ["", "_mid", "_lo"][lod], 1.0, (2.2, 15.5, -3.0), 14.0, 12.0, 38.0, 0.15, 5.0, 10.0, U, lam, nv, 0.9, ribs, down=True, seed=11)


WING_L = {"main": 34.0, "low": 14.0}


# ============================================================================================ crown / bodice / skirt / ...
def crown(lod):
    """a spiked tiara: a band round the brow of the head and nine thorns (the ones over the face the tallest) + a pair of fin blades over the ears."""
    m = Mesh("valk_crown" + ["", "_mid", "_lo"][lod])
    yb = 30.6
    hx, hz = 4.55, 4.55
    n_ring = 8 if lod < 2 else 6
    secs = [ring(yb - 0.9, hx, hz, n_ring, rot=math.pi / n_ring), ring(yb + 0.9, hx - 0.25, hz - 0.25, n_ring, rot=math.pi / n_ring)]
    loft(m, 0, secs, W)
    heights = [13.5, 9.5, 6.5, 5.0, 4.4, 5.0, 6.5, 9.5, 12.0] if lod < 2 else [13.5, 8.5, 5.0, 5.0, 8.5]
    n = len(heights)
    for k, h in enumerate(heights):
        a = math.pi / 2 - math.radians(-150 + 300 * k / (n - 1))
        c, s_ = math.cos(a), math.sin(a)
        base = (hx * 0.96 * c, yb + 0.6, hz * 0.96 * s_)
        o = norm((c, 0.0, s_))
        d = norm((c * 0.22, 1.0, s_ * 0.22))
        wd = 1.15 if lod < 2 else 1.3
        leaf(m, 0, base, d, o, h, wd, 0.8, W, stages=1)
    for sx in ((1, -1) if lod < 2 else ()):                                    # fin blades over the ears (the wing-like ornaments of the dress), both sides
        base = (sx * 4.4, 27.6, -0.6)
        leaf(m, 0, base, norm((sx * 0.45, 0.55, -0.85)), (sx, 0, 0), 7.5 if lod == 0 else 6.0, 1.15, 0.5, P, stages=2 if lod == 0 else 1)
        if lod == 0:
            leaf(m, 0, add(base, (0, 1.9, 0.6)), norm((sx * 0.55, 0.65, -0.55)), (sx, 0, 0), 5.0, 0.8, 0.4, A, stages=1)
    # a glowing brow gem on the band
    if lod < 2:
        slab(m, 1, (0.0, yb + 0.1, 4.7), (1, 0, 0), (0, 1, 0), 0.55, 0.55, 0.3, G)
    return m


def bodice(lod):
    """the ruffled petal bodice: a shell round the torso, a fin collar, two to three rows of petals fanning over it (the frills of the dress), a glowing belt."""
    m = Mesh("valk_bodice" + ["", "_mid", "_lo"][lod])
    ys = [24.5, 20.5, 16.0, 12.7]
    hxz = [(4.7, 2.75), (4.95, 2.95), (4.4, 2.7), (4.5, 2.75)]
    n = 8 if lod == 0 else 6
    secs = [ring(y, hx, hz, n, rot=math.pi / n) for y, (hx, hz) in zip(ys, hxz)]
    if lod == 1:
        secs = [secs[0], secs[1], secs[3]]
    if lod == 2:
        secs = [secs[0], secs[1], secs[3]]
    loft(m, 0, secs, A)
    # belt (emissive band)
    loft(m, 1, [ring(13.6, 4.62, 2.82, n, rot=math.pi / n), ring(12.7, 4.62, 2.82, n, rot=math.pi / n)], G)
    # the standing fin collar round the neck
    nc = {0: 9, 1: 6, 2: 4}[lod]
    for k in range(nc):
        a = 2 * math.pi * k / nc + 0.3
        c, s_ = math.cos(a), math.sin(a)
        if s_ < -0.55:
            continue
        o = norm((c, 0, s_))
        d = norm((c * 0.8, 1.0, s_ * 0.8))
        leaf(m, 0, (3.0 * c, 24.3, 1.9 * s_), d, o, 3.6 if s_ > 0 else 2.6, 0.9, 0.5, W)
    rows = {0: [(22.8, 4.95, 2.95, 8, 1.1, 3.0, 1.0), (19.8, 5.15, 3.15, 8, 1.3, 3.4, 1.1), (16.8, 4.9, 3.0, 8, 1.5, 3.6, 1.1)], 1: [(21.0, 5.0, 3.0, 6, 1.2, 3.6, 1.0), (17.4, 5.0, 3.0, 6, 1.5, 4.4, 1.1)], 2: [(19.8, 5.1, 3.1, 5, 1.3, 5.0, 1.0)]}[lod]
    for i, (y, hx, hz, nn, fl, L, rs) in enumerate(rows):
        ruffle(m, y, hx, hz, nn, fl, L, rs, W, P, rot=0.26 * i)
    return m


def skirt(lod):
    """the short flared petal skirt: layers of long petals from the waist, each ring longer and wider than the one above; the lowest ring ends at the knee height."""
    m = Mesh("valk_skirt" + ["", "_mid", "_lo"][lod])
    rings = {0: [(13.0, 4.85, 3.05, 8, 1.2, 4.2, 1.0), (12.4, 5.0, 3.2, 9, 2.4, 4.8, 1.1), (11.8, 5.15, 3.35, 10, 3.6, 5.2, 1.2)], 1: [(12.8, 4.9, 3.1, 7, 1.4, 4.4, 1.0), (12.0, 5.1, 3.3, 7, 3.2, 5.2, 1.2)], 2: [(12.5, 5.0, 3.2, 6, 2.6, 5.2, 1.0)]}[lod]
    for i, (y, hx, hz, nn, fl, L, rs) in enumerate(rings):
        ruffle(m, y, hx, hz, nn, fl, L, rs, [W, P, A][i % 3], [P, A, W][i % 3], rot=0.22 * i)
    return m


def pauldron(lod):
    """a broad layered shoulder piece on the left arm (pivot (6, 22, 0)): three stacked lames that flare outward, a fin pointing out and up, a glowing edge."""
    m = Mesh("valk_pauldron" + ["", "_mid", "_lo"][lod])
    cx = 6.0
    lames = [(24.9, 3.2, 3.1, 0.0), (23.5, 3.6, 3.4, 0.5), (22.1, 4.0, 3.7, 1.1)]
    if lod == 2:
        lames = lames[::2]
    n = 6
    for i, (y, hx, hz, off) in enumerate(lames):
        top = ring(y, hx, hz, n, cx=cx + off, rot=math.pi / n)
        bot = ring(y - 1.6, hx + 0.35, hz + 0.3, n, cx=cx + off + 0.3, rot=math.pi / n)
        loft(m, 0, [top, bot], [W, P, A][i % 3])
        if lod == 0 and i == len(lames) - 1:
            loft(m, 1, [bot, [(p[0] * 1.0, p[1] - 0.25, p[2]) for p in bot]], G)
    m_top = ring(25.1, 2.9, 2.7, n, cx=cx, rot=math.pi / n)
    loft(m, 0, [m_top, ring(24.8, 3.5, 3.25, n, cx=cx, rot=math.pi / n)], W)
    leaf(m, 0, (cx + 3.4, 23.8, 0.0), norm((1.0, 0.65, -0.1)), (0, 1, 0), 6.5 if lod == 0 else 5.0, 1.8, 0.7, P, stages=2 if lod == 0 else 1)
    if lod == 0:
        leaf(m, 0, (cx + 3.0, 22.6, 0.4), norm((1.0, 0.25, 0.2)), (0, 1, 0), 4.5, 1.3, 0.6, A, stages=1)
    return m


def gauntlet(lod):
    """the ice cuff of the left forearm (arm pivot (6, 22, 0), hand at y 10): a flared sleeve and three thorns over the back of the hand."""
    m = Mesh("valk_gauntlet" + ["", "_mid", "_lo"][lod])
    cx = 6.0
    n = 6 if lod < 2 else 4
    secs = [ring(18.6, 2.55, 2.55, n, cx=cx, rot=math.pi / n), ring(15.2, 2.85, 2.85, n, cx=cx, rot=math.pi / n), ring(11.8, 3.3, 3.2, n, cx=cx, rot=math.pi / n)]
    if lod == 2:
        secs = [secs[0], secs[2]]
    loft(m, 0, secs, A)
    if lod < 2:
        loft(m, 1, [secs[-1], [(p[0], p[1] - 0.3, p[2]) for p in secs[-1]]], G)
        if lod == 0:
            ruffle(m, 15.4, 2.95, 2.95, 6, 0.9, 2.8, 0.7, W, P, rot=0.3, cx=cx)
    for k, (dx, dz) in enumerate(((1.0, 0.0), (0.8, 0.6), (0.8, -0.6))[: 3 if lod == 0 else 2]):
        leaf(m, 0, (cx + dx * 2.6, 14.5 + k * 0.4, dz * 2.4), norm((dx * 0.9, -0.55, dz * 0.9)), (dx, 0, dz), 5.2, 1.05, 0.55, W, stages=1)
    return m


def greave(lod):
    """the spiky water boot of the left leg (pivot (2, 12, 0), foot at y 0): flared sleeve rings and fin thorns pointing out and down, a frosted toe."""
    m = Mesh("valk_greave" + ["", "_mid", "_lo"][lod])
    cx = 2.0
    n = 6 if lod < 2 else 4
    secs = [ring(10.4, 2.45, 2.45, n, cx=cx, rot=math.pi / n), ring(6.6, 2.85, 2.85, n, cx=cx, rot=math.pi / n), ring(3.0, 3.1, 3.0, n, cx=cx, rot=math.pi / n), ring(0.4, 2.7, 3.2, n, cx=cx, rot=math.pi / n)]
    if lod == 1:
        secs = [secs[0], secs[1], secs[3]]
    if lod == 2:
        secs = [secs[0], secs[3]]
    loft(m, 0, secs, A)
    if lod < 2:
        loft(m, 1, [ring(10.6, 2.5, 2.5, n, cx=cx, rot=math.pi / n), ring(9.8, 2.5, 2.5, n, cx=cx, rot=math.pi / n)], G)
        if lod == 0:
            ruffle(m, 7.4, 2.9, 2.9, 6, 0.9, 3.4, 0.7, W, P, rot=0.2, cx=cx)
    thorns = [((1.0, 0.0), 8.6), ((0.7, 0.7), 6.0), ((0.7, -0.7), 5.4), ((1.0, 0.0), 3.4)][: (4 if lod == 0 else 2 if lod == 1 else 1)]
    for (dx, dz), y in thorns:
        leaf(m, 0, (cx + dx * 2.8, y, dz * 2.7), norm((dx * 0.8, -0.6, dz * 0.8)), (dx, 0, dz), 5.6, 1.1, 0.6, W, stages=1)
    return m


def grimoire(lod):
    """Noelle's glowing grimoire floating open at her left hip (body bone): two page slabs on a spine, a bright page sheet."""
    m = Mesh("valk_grimoire" + ["", "_mid", "_lo"][lod])
    c = (9.2, 11.8, 3.2)
    ax = norm((0.55, 0.0, 0.84))
    ay = norm((-0.22, 0.95, 0.2))
    slab(m, 0, c, ax, ay, 4.2, 4.8, 0.45, P)
    slab(m, 1, add(c, mul(norm(cross(ax, ay)), 0.52)), ax, ay, 3.6, 4.2, 0.12, G)
    if lod < 2:
        slab(m, 0, sub(c, mul(ax, 4.4)), ax, ay, 0.45, 5.0, 0.7, A)
    return m


# ============================================================================================ build
PIECES = [  # name, builder, bone, copies (mirror), pass, alpha, optional
    ("crown", crown, "head", 1, "plate", 0.92, False),
    ("bodice", bodice, "body", 1, "plate", 0.9, False),
    ("skirt", skirt, "body", 1, "plate", 0.86, False),
    ("pauldron", pauldron, "arm", 2, "plate", 0.92, False),
    ("gauntlet", gauntlet, "arm", 2, "plate", 0.9, False),
    ("greave", greave, "leg", 2, "plate", 0.88, False),
    ("wing_main", wing_main, "wing_hinge", 2, "glass", 0.72, False),
    ("wing_low", wing_low, "wing_hinge_low", 2, "glass", 0.7, False),
    ("grimoire", grimoire, "body", 1, "plate", 0.95, True),
]
NAMES = ["", "_mid", "_lo"]


def build_all():
    """{(piece, lod): Mesh}"""
    out = {}
    for name, fn, *_ in PIECES:
        for lod in range(3):
            out[(name, lod)] = fn(lod)
    return out


def counts(meshes):
    tot = {}
    for (name, lod), m in meshes.items():
        copies = next(p[3] for p in PIECES if p[0] == name)
        opt = next(p[6] for p in PIECES if p[0] == name)
        tot.setdefault(lod, [0, 0, 0])
        tot[lod][0] += len(m.q) * copies
        tot[lod][1] += 0 if opt else len(m.q) * copies
    return tot


def main():
    os.makedirs(OUT, exist_ok=True)
    ms = build_all()
    lines = ["Valkyrie armour (preview only): quads per piece and level (LEFT copy written; copies = 2 means the layer draws the mirror as well); layer 0 body, 1 emissive, 2 foam", "",
             "%-14s %-6s %6s %6s %6s %6s %8s" % ("piece", "level", "quads", "L0", "L1", "L2", "x copies")]
    manifest = {"units": "px (1/16 block), vanilla player REST pose, y up, feet y=0, front +z", "pieces": []}
    for name, fn, bone, copies, pas, alpha, opt in PIECES:
        entry = {"name": name, "bone": bone, "copies": copies, "pass": pas, "alpha": alpha, "optional": opt, "levels": {}}
        for lod in range(3):
            m = ms[(name, lod)]
            c = [0, 0, 0]
            for qd in m.q:
                c[qd[0]] += 1
            fn_ = "valk_%s%s.txt" % (name, NAMES[lod])
            with open(os.path.join(OUT, fn_), "w", encoding="utf-8", newline="\n") as f:
                f.write(m.text())
            lines.append("%-14s %-6s %6d %6d %6d %6d %8d" % (name, ["near", "mid", "far"][lod], len(m.q), c[0], c[1], c[2], len(m.q) * copies))
            entry["levels"][["near", "mid", "far"][lod]] = {"file": fn_, "quads": len(m.q)}
        manifest["pieces"].append(entry)
    tot = counts(ms)
    lines += ["", "TOTAL drawn quads per wearer (both copies; the grimoire is optional and listed apart)"]
    for lod in range(3):
        allq, core = tot[lod][0], tot[lod][1]
        lines.append("%-6s %5d quads (%5d vertices), without the grimoire %5d" % (["near", "mid", "far"][lod], allq, allq * 4, core))
        manifest.setdefault("totals", {})[["near", "mid", "far"][lod]] = {"quads": allq, "vertices": allq * 4, "quads_without_grimoire": core}
    with open(os.path.join(OUT, "counts.txt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    with open(os.path.join(OUT, "..", "pieces.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(manifest, f, indent=1)
    print("\n".join(lines))


if __name__ == "__main__":
    main()

"""Legendary Bow forms, third look (4.59.0, docs/heroweapon/bow_designs.md): six bows rebuilt so that no two read alike, and the Sagittarius symbol set ON the bow.

    python3 -B tools/blender/forms_legendary_bow_v3.py [--only sagittarius,...] [--trial DIR] [--quick] [--lowres] [--samples N] [--no-check]

This module only ADDS: it imports forms_legendary_bow.py (the second look, whose builders stay untouched and still run from that file), lets a bow state its stave as a function
`sh["fn"](y)` instead of the belly / power / curl numbers (bow_x is wrapped, old shapes are unchanged), registers the new builders under the same form ids and runs the same
driver (bake, three draw stages, 1024 px skin + glow, 5000 quad lines, 350 KB). Part names keep the prefix rules (limb* flexes, string*, arrow*, everything else is rigid).

Every builder ends with connectivity(): every object of the bow must touch another one (or sit inside it) so that something is wired to the riser or a limb; a floating part is an error
(the string and the nocked arrow are exempt).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_legendary_bow as F  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU, bl  # noqa: E402
import bpy  # noqa: E402

HALF = F.HALF
at, stave, band, lsec, rsec, limb_pts, riser, hand_binding, noisy, shift = F.at, F.stave, F.band, F.lsec, F.rsec, F.limb_pts, F.riser, F.hand_binding, F.noisy, F.shift

_old_bow_x = F.bow_x


def _bow_x(y, sh):
    """The old belly / power / curl shape, or the builder's own `fn(y)` when the shape dict carries one."""
    return sh["fn"](y) if "fn" in sh else _old_bow_x(y, sh)


F.bow_x = _bow_x
bow_x = _bow_x


def prof(belly, xt, p, flick, cs, riser_h, xt_lo=None, p_lo=None, flick_lo=None, belly_lo=None):
    """A stave function of y: flat at x = belly over the riser, then x falls to the tip's x `xt` with exponent p (+ a tip flick toward the target from `cs` on). `_lo` values shape the lower limb."""
    def fn(y):
        up = y >= 0
        u = max(0.0, (abs(y) - riser_h) / (HALF - riser_h))
        b = belly if up or belly_lo is None else belly_lo
        t0 = xt if up or xt_lo is None else xt_lo
        pw = p if up or p_lo is None else p_lo
        fl = flick if up or flick_lo is None else flick_lo
        c = max(0.0, (u - cs) / (1.0 - cs))
        return t0 + (b - t0) * (1.0 - u ** pw) + fl * c * c
    return fn


# ================================================================ the check: nothing floats
def connectivity(tol=0.8, free=("string", "arrow"), strict=True):
    """Union-find over the bow's objects: two touch when their surfaces cross (BVH overlap) or are closer than `tol` units (0.007 blocks). Every object must be in the one island that holds the
    riser; the string and the arrow are exempt. Returns the list of floating object names (and raises when `strict`)."""
    from mathutils.bvhtree import BVHTree
    obs = [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and not o.get("detail") and len(o.data.polygons)]
    trees, boxes, cos = [], [], []
    for o in obs:
        co = [tuple(v.co) for v in o.data.vertices]
        polys = []
        for p in o.data.polygons:
            vs = list(p.vertices)
            for k in range(1, len(vs) - 1):
                polys.append((vs[0], vs[k], vs[k + 1]))
        trees.append(BVHTree.FromPolygons(co, polys, all_triangles=True))
        boxes.append((min(c[0] for c in co) - tol, min(c[1] for c in co) - tol, min(c[2] for c in co) - tol, max(c[0] for c in co) + tol, max(c[1] for c in co) + tol, max(c[2] for c in co) + tol))
        cos.append(co)
    n = len(obs)
    par = list(range(n))

    def find(a):
        while par[a] != a:
            par[a] = par[par[a]]
            a = par[a]
        return a
    for i in range(n):
        bi = boxes[i]
        for j in range(i + 1, n):
            bj = boxes[j]
            if bi[0] > bj[3] or bj[0] > bi[3] or bi[1] > bj[4] or bj[1] > bi[4] or bi[2] > bj[5] or bj[2] > bi[5]:
                continue
            if find(i) == find(j):
                continue
            hit = bool(trees[i].overlap(trees[j]))
            if not hit:
                for c in cos[i][::max(1, len(cos[i]) // 80)]:
                    loc = trees[j].find_nearest(c)
                    if loc[0] is not None and loc[3] <= tol:
                        hit = True
                        break
            if not hit:
                for c in cos[j][::max(1, len(cos[j]) // 80)]:
                    loc = trees[i].find_nearest(c)
                    if loc[0] is not None and loc[3] <= tol:
                        hit = True
                        break
            if hit:
                par[find(i)] = find(j)
    core = next((i for i in range(n) if obs[i].name == "riser_core"), 0)          # the body: everything must be in the island of the riser's core (a "riser_*" name is no excuse)
    main = find(core)
    isl = {}
    for i in range(n):
        if find(i) != main:
            isl.setdefault(find(i), []).append(obs[i].name)
    # an island made only of exempt parts (the string, the arrow) is fine; any other island is a floating part
    bad = sorted(nm for ns in isl.values() if not all(x.startswith(free) for x in ns) for nm in ns if not nm.startswith(free))
    print("[connect] %d objects, %d islands apart from the bow body, floating: %s" % (n, len(isl), bad[:30] if bad else "none"))
    if bad and strict:
        raise RuntimeError("floating parts: %s" % bad[:40])
    return bad


# ================================================================ shared bits
def star_plate(name, cx, cy, z, r, mat, part, pts=4, thick=0.4, inner=0.38):
    outline = []
    for i in range(pts * 2):
        a = TAU * i / (pts * 2) + math.pi / 2
        rad = r if i % 2 == 0 else r * inner
        outline.append((cx + rad * math.cos(a), cy + rad * math.sin(a)))
    return K.plate(name, outline, z - thick, z + thick, mat, part=part, bevel_w=0.0)


def disc(name, c, r, z0, z1, mat, part, sides=28):
    """A thin round plate in the xy plane from z0 to z1 (any sign), a closed solid."""
    ob = bl.lathe(name, [(0.05, z0), (r, z0), (r, z1), (0.05, z1)], mat, sides=sides, axis="z", origin=(c[0], c[1], 0.0), part=part)
    return K._finish_obj(ob, part, True, 50.0)


def annulus(name, c, r0, r1, z0, z1, mat, part, sides=28):
    """A raised ring (inner wall, top, outer wall; the underside is hidden against what it sits on)."""
    ob = bl.lathe(name, [(r0, z0), (r0, z1), (r1, z1), (r1, z0)], mat, sides=sides, axis="z", origin=(c[0], c[1], 0.0), caps=(False, False), part=part)
    return K._finish_obj(ob, part, True, 50.0)


def face_pt(P, t, v, wd, hh, s, lift=0.1, k=0.32):
    """A point on the +z (s = 1) or -z face of a limb made with lsec: along t, across v (-1..1), `lift` above the skin; the wires and stars follow the limb through it."""
    p, tn, nr = at(P, t)
    w = wd(t)
    xo = 0.025 * w + v * 0.525 * w
    z = hh(t) * (1.0 - k * (0.55 - xo / w) / 1.05)
    return p + nr * xo + V((0, 0, s * (z + lift)))


def round_sec(w, h, n=10):
    return [(w * math.cos(TAU * k / n), h * math.sin(TAU * k / n)) for k in range(n)]


# ================================================================ SAGITTARIUS: a recurve with the zodiac medallion and the archer's glyph raised on the riser, the constellation inlaid along the limbs
def sagittarius_v3(o):
    sh = dict(belly=28.0, riser=15.0)
    sh["fn"] = prof(28.0, 0.0, 2.3, 12.0, 0.55, 15.0)
    F.begin(o["samples"])
    P = F.sag_palette()
    rr = K.rng(9909)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = lambda t: 5.0 - 2.4 * t ** 0.85           # noqa: E731
    hh = lambda t: 2.2 - 0.9 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_navy%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.32), P["navy"], "limb_navy")
        noisy(ob, 0.06, 0.06, 101 + s)
        for k, t in enumerate((0.06, 0.38, 0.88)):
            sc = lambda tt, t=t: lsec(wd(t) + 0.25, hh(t) + 0.2, 0.32)       # noqa: E731
            band("limb_band%d_%d" % (s, k), L[s], t, sc, 0.7 - 0.2 * t, P["gold"] if k % 2 == 0 else P["gold2"], "limb_band", width=0.02)
    # the constellation, lifted onto the faces of both limbs (both sides): stars as gold-rimmed plates with a light stud, joined by thin raised wire that follows the limb
    chains = {1: [(0.14, 0.1), (0.29, -0.55), (0.45, 0.5), (0.62, -0.4), (0.8, 0.35)], -1: [(0.16, -0.2), (0.33, 0.55), (0.52, -0.45), (0.74, 0.3)]}
    big = {(1, 2), (-1, 1), (1, 4)}
    for s in (1, -1):
        for z in (1, -1):
            for ci, chain in enumerate(chains[s]):
                p = face_pt(L[s], chain[0], chain[1], wd, hh, z, 0.0)
                r = 3.3 if (s, ci) in big else 2.6
                n = "limb_cstar%d_%d_%d" % (s, ci, z)
                star_plate(n, p.x, p.y, p.z, r, P["gold"], "limb_cstar", pts=4 if ci % 2 == 0 else 5, thick=0.5)
                K.blob("limb_cstud%d_%d_%d" % (s, ci, z), tuple(p + V((0, 0, 0.45 * z))), (0.7, 0.7, 0.5), P["star"], seed=ci, lump=0.0, rings=3, segs=5, part="limb_cstud", detail=True)
            for ci in range(len(chains[s]) - 1):
                a, b = chains[s][ci], chains[s][ci + 1]
                pts = [tuple(face_pt(L[s], a[0] + (b[0] - a[0]) * u, a[1] + (b[1] - a[1]) * u, wd, hh, z, 0.05)) for u in (0.0, 0.25, 0.5, 0.75, 1.0)]
                K.tube("limb_cwire%d_%d_%d" % (s, ci, z), pts, 0.3, P["line"], sides=4, sub=1, part="limb_cwire")
    # the horse's head on the upper tip, mane streaming back along the limb
    pe, tn, nr = at(L[1], 1.0)
    hc = V((pe.x + 4.2, pe.y - 0.6, 0.0))
    K.tube("limb_hneck", [tuple(pe - tn * 4.0), tuple(pe), tuple(pe + nr * 2.0 + tn * 0.8), tuple(hc - V((1.5, 0, 0)))], 1.3, P["gold"], sides=8, sub=3, part="limb_horse", taper=(1.7, 1.3))
    K.blob("limb_hskull", tuple(hc), (3.0, 2.1, 1.7), P["gold"], seed=7, lump=0.08, rings=8, segs=10, part="limb_horse")
    K.tapered("limb_hmuzzle", [hc + V((1.5, -0.2, 0)), hc + V((4.2, -1.0, 0)), hc + V((6.0, -1.8, 0))], 1.5, 0.95, P["gold"], sides=8, sub=2, part="limb_horse")
    K.blob("limb_hnose", tuple(hc + V((6.0, -1.9, 0))), (1.2, 1.0, 1.1), P["gold2"], seed=8, lump=0.08, rings=6, segs=8, part="limb_horse")
    for z in (1, -1):
        K.tapered("limb_hear%d" % z, [hc + V((-0.8, 1.6, 0.9 * z)), hc + V((-1.2, 3.1, 1.3 * z)), hc + V((-0.4, 4.4, 1.0 * z))], 0.65, 0.08, P["gold"], sides=5, sub=2, part="limb_horse")
        K.blob("limb_heye%d" % z, tuple(hc + V((1.8, 0.5, 1.5 * z))), (0.5, 0.5, 0.35), P["star"], seed=z + 4, lump=0.0, rings=4, segs=6, part="limb_horse")
    for k in range(4):
        pth = [hc + V((-1.0, 1.2, (k - 1.5) * 0.6)) + (tn * -(2.0 * j)) + nr * (0.9 * j * 0.3 * (1 + 0.2 * k)) + V((0.5 * math.sin(j * 0.7 + k), 0, 0.3 * math.cos(j * 0.6 + k))) for j in range(9)]
        K.ribbon("limb_hmane%d" % k, pth, lambda u: 2.2 * (1 - 0.6 * u) + 0.3, P["mane"], facing=(0, 0, 1), thick=0.16, part="limb_horse", fray=0.6, rng_=rr, edge_wave=0.12, notch=0.5)
    pe2, tn2, nr2 = at(L[-1], 1.0)                                          # the flowing tail on the lower tip
    K.tapered("limb_nockL", [pe2 - tn2 * 4.0, pe2, pe2 + nr2 * 1.2], 1.3, 0.4, P["gold"], sides=6, sub=2, part="limb_nock")
    for k in range(5):
        pth = [pe2 + V((0, 0, (k - 2) * 0.7)) + tn2 * (-2.0 * j) + nr2 * (1.0 * j * (0.3 + 0.1 * k) * 0.4 + 0.5 * math.sin(j * 0.5 + k)) + V((0, 0, 0.6 * math.sin(j * 0.7 + k * 1.3))) for j in range(10)]
        K.ribbon("limb_tail%d" % k, pth, lambda u: 2.4 * (1 - 0.65 * u) + 0.3, P["mane"], facing=(0, 0, 1), thick=0.16, part="limb_horse", fray=0.8, rng_=rr, edge_wave=0.12, notch=0.6)
    # the riser: a broad enamel body with gold shoulders, the medallion on both faces
    riser("riser_core", sh, [(-17.0, 2.7, 2.9), (-12.0, 3.7, 3.3), (-8.0, 3.2, 3.1), (2.0, 3.3, 3.2), (4.0, 5.4, 3.5), (11.0, 6.6, 3.6), (17.0, 5.4, 3.3), (22.0, 3.4, 2.7)], P["navy"])
    hand_binding(sh, P["navy"], P["gold"], -9.8, 2.4, 2.6, bands=(-9.3, -4.5, 1.6), band_mat=P["gold"], turns=7, cord=0.3)
    yc, R = 11.4, 8.5
    ang = math.radians(45)
    d = V((math.cos(ang), math.sin(ang), 0))
    sd = V((-d.y, d.x, 0))
    c0 = V((bx, yc, 0))
    for z in (1, -1):
        disc("riser_disc%d" % z, (bx, yc), R, z * 3.0, z * 4.05, P["gold2"], "riser_disc")
        annulus("riser_zodiac%d" % z, (bx, yc), R * 0.82, R, z * 3.9, z * 4.6, P["gold"], "riser_zodiac")
        annulus("riser_zodiac_in%d" % z, (bx, yc), R * 0.5, R * 0.56, z * 3.9, z * 4.4, P["gold"], "riser_zodiac", sides=20)
        for i in range(12):                                                 # the twelve signs: studs on the ring, baked into the skin
            a = TAU * i / 12
            K.blob("sign%d_%d" % (z, i), (bx + R * 0.91 * math.cos(a), yc + R * 0.91 * math.sin(a), z * 4.7), (0.55, 0.55, 0.4), P["star"] if i == 8 else P["gold2"], seed=i, lump=0.0, rings=3, segs=5, part="riser_sign", detail=True)
        zz = z * 4.62
        K.bar("riser_glyph_shaft%d" % z, tuple(c0 - d * 6.6 + V((0, 0, zz))), tuple(c0 + d * 6.2 + V((0, 0, zz))), 0.55, 0.5, P["gold"], part="riser_glyph", taper=1.0, bevel_w=0.08)
        hp = c0 + d * 7.6
        K.plate("riser_glyph_head%d" % z, [tuple((hp + d * 1.4)[:2]), tuple((hp - d * 1.6 + sd * 3.0)[:2]), tuple((hp - d * 0.5)[:2]), tuple((hp - d * 1.6 - sd * 3.0)[:2])], zz - 0.5, zz + 0.5, P["gold"], part="riser_glyph", bevel_w=0.08)
        K.bar("riser_glyph_cross%d" % z, tuple(c0 - d * 4.2 - sd * 3.8 + V((0, 0, zz))), tuple(c0 - d * 4.2 + sd * 3.8 + V((0, 0, zz))), 0.45, 0.5, P["gold"], part="riser_glyph", bevel_w=0.08)
        K.gem_setting("gem%d" % z, (bx, yc, z * 4.75), 3.0, P["gem"], P["gold"], normal=(0, 0, z), claws=6, sides=8, part="jewel", rng_=rr, claw_r=0.5)
    F.string(sh, P["string"], 0.4)
    F.arrow(sh, P["gold2"], P["gold"], P["mane"], 3.3, "broad", cap_mat=P["navy"], glow_mat=None)
    return sh


# ================================================================ HUNTER'S BOW: two real horns for limbs, a beast's skull on the riser
def hunters_palette_v3():
    S = K.surface
    P = {}
    P["horn"] = S("v3horn", rgb(64, 48, 38), 0.0, 0.42, wear=0.8, wear_color=rgb(170, 146, 116), dirt=0.8, dirt_color=(0.03, 0.02, 0.01, 1), scratches=0.6, bump="grain", bump_scale=0.9, bump_strength=0.32,
                  mottle=1.0, mottle_color=rgb(122, 96, 68), fine=0.5, seed=231, bevel=0.2, ao_dist=2.0,
                  cramp=("y", -84.0, 84.0, [(0.0, rgb(30, 22, 18)), (0.22, rgb(86, 66, 50)), (0.5, rgb(206, 190, 154)), (0.78, rgb(86, 66, 50)), (1.0, rgb(30, 22, 18))]))
    P["ivory"] = S("v3ivory", rgb(232, 220, 188), 0.0, 0.5, wear=0.8, wear_color=rgb(252, 246, 222), dirt=0.9, dirt_color=(0.05, 0.03, 0.012, 1), scratches=0.7, bump="grain", bump_scale=0.9, bump_strength=0.28,
                   mottle=1.0, mottle_color=rgb(160, 120, 70), fine=0.6, seed=232, bevel=0.2, ao_dist=2.2, groove=rgb(120, 86, 46), groove_amount=0.6)
    P["rust"] = S("v3rust", rgb(132, 42, 28), 0.0, 0.6, bump="leather", bump_scale=1.3, bump_strength=0.8, groove=rgb(40, 12, 8), groove_amount=0.9, wear=0.8, wear_color=rgb(196, 110, 70), dirt=0.9,
                  mottle=0.8, mottle_color=rgb(70, 20, 12), seed=233, bevel=0.0, fine=0.5)
    P["fur"] = S("v3fur", rgb(196, 192, 182), 0.0, 0.9, wear=0.3, wear_color=rgb(250, 248, 240), feather=(15, 3, 0.8, 0.3), dirt=0.5, tramp=[(0.0, rgb(60, 56, 52)), (0.5, rgb(190, 186, 176)), (1.0, rgb(250, 248, 240))],
                 mottle=0.8, mottle_color=rgb(110, 104, 96), seed=234, bevel=0.06, fine=0.5)
    P["gold"] = S("v3hgold", rgb(190, 146, 58), 1.0, 0.34, wear=0.9, wear_color=rgb(250, 226, 150), dirt=0.9, dirt_color=(0.04, 0.025, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7, bump_strength=0.2,
                  mottle=0.8, mottle_color=rgb(110, 70, 20), seed=235, bevel=0.18, ao_dist=2.0)
    P["gem"] = S("v3hgem", rgb(244, 168, 14), 0.1, 0.06, emit=rgb(255, 190, 40), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 230, 90), seed=236, bevel=0.0)
    P["eye"] = S("v3heye", rgb(255, 120, 40), 0.0, 0.3, emit=rgb(255, 110, 30), emit_strength=1.0, seed=237, bevel=0.0)
    P["string"] = S("v3hstr", rgb(214, 186, 128), 0.0, 0.6, bump="wrap", bump_strength=0.9, groove=rgb(90, 60, 30), groove_amount=0.9, seed=238, bevel=0.0)
    return P


def hunters_v3(o):
    sh = dict(belly=38.0, riser=12.0)
    sh["fn"] = prof(38.0, 4.0, 1.75, 6.0, 0.7, 12.0)
    F.begin(o["samples"])
    P = hunters_palette_v3()
    rr = K.rng(1303)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = lambda t: 5.6 * (1 - t) ** 0.8 + 0.6         # noqa: E731
    hh = lambda t: 4.6 * (1 - t) ** 0.8 + 0.55         # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_horn%d" % s, L[s], lambda t: round_sec(wd(t), hh(t), 10), P["horn"], "limb_horn")
        noisy(ob, 0.3, 0.07, 5 + s)
        for k, t in enumerate((0.08, 0.3, 0.56)):                    # growth rings: a red leather lashing at the root, two bone rings
            band("limb_ring%d_%d" % (s, k), L[s], t, lambda tt, t=t: round_sec(wd(t), hh(t), 10), 0.5 * (1 - 0.4 * t), P["rust"] if k == 0 else P["ivory"], "limb_ring", width=0.03 if k == 0 else 0.014)
        F.spike_row("limb_fang%d_" % s, L[s], (0.3, 0.46, 0.62, 0.78), 2.2, 7.0, 0.95, P["ivory"], rr, "limb_fang", lean=0.55, bend=1.0, dz=0.5)
        pe, tn, nr = at(L[s], 1.0)                                                       # the nock: a bone cap on the horn's point
        K.tapered("limb_nock%d" % s, [pe - tn * 4.0, pe, pe + tn * 0.8 + nr * 1.6], 0.9, 0.3, P["ivory"], sides=6, sub=2, part="limb_nock")
        bl.lathe("limb_cap%d" % s, [(1.0, 0), (1.3, 0.4), (1.3, 1.5), (0.9, 1.9)], P["gold"], sides=8, axis="y", origin=(pe.x - 0.3, pe.y - s * 2.6, 0), part="limb_nock")
    # the riser: a bone core, the skull of a great beast on the front, a ruff of grey fur
    riser("riser_core", sh, [(-14.0, 2.6, 2.8), (-11.0, 3.5, 3.3), (-7.0, 3.1, 3.1), (3.0, 3.1, 3.1), (5.0, 4.6, 3.6), (10.0, 5.6, 3.6), (15.0, 4.0, 3.3), (18.0, 2.6, 2.7)], P["ivory"])
    hand_binding(sh, P["rust"], P["ivory"], -9.8, 2.6, 2.7, bands=(-9.2, -4.0, 1.8), band_mat=P["ivory"], turns=8)
    F.jewel(sh, P["gem"], P["gold"], rr, y=9.0, r=3.6, claws=7)
    sk = V((bx + 7.0, 9.5, 0.0))
    K.blob("riser_skull", tuple(sk), (6.0, 6.6, 4.4), P["ivory"], seed=11, lump=0.07, rings=9, segs=12, part="riser_skull")
    K.tapered("riser_snout", [sk + V((3.0, -0.8, 0)), sk + V((8.0, -2.0, 0)), sk + V((12.0, -3.0, 0))], 4.2, 2.4, P["ivory"], sides=8, sub=2, part="riser_skull")
    K.tapered("riser_jaw", [sk + V((2.0, -5.0, 0)), sk + V((7.0, -6.6, 0)), sk + V((11.0, -6.4, 0))], 2.2, 1.2, P["ivory"], sides=6, sub=2, part="riser_skull")
    for z in (1, -1):
        K.blob("riser_socket%d" % z, tuple(sk + V((2.0, 1.4, 3.4 * z))), (1.5, 1.7, 1.0), P["horn"], seed=z + 3, lump=0.05, rings=5, segs=7, part="riser_skull")
        K.blob("riser_eye%d" % z, tuple(sk + V((2.3, 1.4, 3.9 * z))), (0.55, 0.65, 0.4), P["eye"], seed=z + 5, lump=0.0, rings=4, segs=5, part="riser_skull")
        K.tapered("riser_brow%d" % z, [sk + V((-2.0, 4.0, 2.4 * z)), sk + V((2.0, 4.8, 3.2 * z)), sk + V((6.0, 3.4, 2.6 * z))], 0.9, 0.35, P["ivory"], sides=5, sub=1, part="riser_skull")
        for i, fx in enumerate((8.4, 11.0)):                                       # the fangs of the upper jaw
            K.tapered("riser_tusk%d_%d" % (z, i), [sk + V((fx, -3.2, 1.6 * z)), sk + V((fx + 0.4, -6.8 - i, 1.8 * z)), sk + V((fx + 0.2, -9.2 - 1.6 * i, 1.6 * z))], 0.95, 0.07, P["ivory"], sides=5, sub=1, part="riser_tusk")
    F.tuft("riser_fur_a", V((bx + 0.2, -9.6, -2.2)), V((0.0, -1.0, -0.15)), 7, 11.0, 3.8, P["fur"], rr, "riser_fur", spread=58.0, facing=(0, 0, 1), curl=1.0, rows=6, thick=0.22)
    F.tuft("riser_fur_b", V((bx + 0.2, -9.6, 2.2)), V((0.0, -1.0, 0.15)), 7, 10.5, 3.8, P["fur"], rr, "riser_fur", spread=58.0, facing=(0, 0, 1), curl=-1.0, rows=6, thick=0.22)
    F.string(sh, P["string"], 0.42)
    F.arrow(sh, P["ivory"], P["horn"], P["fur"], 3.3, "barb", cap_mat=P["rust"])
    return sh


# ================================================================ SCOUT'S BOW: a slim straight-handled reflex bow of olive wood with a brass spyglass lashed to the riser
def scouts_palette_v3():
    S = K.surface
    P = {}
    P["wood"] = S("v3olive", rgb(98, 112, 54), 0.0, 0.45, wear=0.8, wear_color=rgb(200, 190, 120), dirt=0.8, dirt_color=(0.02, 0.025, 0.01, 1), scratches=0.7, bump="grain", bump_scale=1.2, bump_strength=0.3,
                  mottle=1.0, mottle_color=rgb(52, 66, 28), fine=0.5, seed=251, bevel=0.15, ao_dist=2.0, groove=rgb(40, 50, 20), groove_amount=0.7)
    P["wood2"] = S("v3olive_dark", rgb(54, 62, 34), 0.0, 0.5, wear=0.8, wear_color=rgb(150, 150, 90), dirt=0.9, bump="grain", bump_strength=0.35, mottle=1.0, mottle_color=rgb(30, 36, 18), seed=252, bevel=0.15)
    P["brass"] = S("v3brass", rgb(210, 164, 66), 1.0, 0.26, wear=0.9, wear_color=rgb(255, 232, 160), dirt=0.8, dirt_color=(0.04, 0.03, 0.008, 1), scratches=0.8, bump="hammer", bump_scale=0.8, bump_strength=0.18,
                   mottle=1.0, mottle_color=rgb(120, 80, 24), fine=0.6, seed=253, bevel=0.18, ao_dist=1.8)
    P["lens"] = S("v3lens", rgb(150, 220, 255), 0.0, 0.05, transmission=0.5, ior=1.45, rim=(rgb(240, 255, 255), 0.8), emit=rgb(120, 200, 255), emit_strength=0.5, seed=254, bevel=0.0)
    P["canvas"] = S("v3canvas", rgb(196, 176, 126), 0.0, 0.8, bump="wrap", bump_scale=1.0, bump_strength=0.8, groove=rgb(80, 66, 36), groove_amount=0.9, dirt=0.8, wear=0.5, wear_color=rgb(236, 224, 184), mottle=0.8, mottle_color=rgb(110, 90, 50),
                    seed=255, bevel=0.0, fine=0.5)
    P["leather"] = S("v3sleather", rgb(70, 50, 32), 0.0, 0.62, bump="leather", bump_scale=1.3, bump_strength=0.8, groove=rgb(22, 14, 8), groove_amount=0.9, wear=0.8, wear_color=rgb(150, 112, 70), dirt=0.9,
                     mottle=0.8, mottle_color=rgb(40, 26, 14), seed=256, bevel=0.0, fine=0.5)
    P["flag"] = S("v3flag", rgb(176, 52, 36), 0.0, 0.7, feather=(9, 2, 0.35, 0.2), tramp=[(0.0, rgb(120, 30, 20)), (1.0, rgb(214, 90, 58))], seed=257, bevel=0.04, mottle=0.6, mottle_color=rgb(90, 24, 16))
    P["gem"] = S("v3sgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=259, bevel=0.0)
    P["string"] = S("v3sstring", rgb(226, 214, 176), 0.0, 0.6, bump="wrap", bump_strength=0.8, groove=rgb(110, 90, 50), groove_amount=0.8, seed=260, bevel=0.0)
    return P


def scouts_v3(o):
    sh = dict(belly=15.0, riser=24.0)
    sh["fn"] = prof(15.0, 4.0, 1.25, 4.0, 0.75, 24.0)
    F.begin(o["samples"])
    P = scouts_palette_v3()
    rr = K.rng(2303)
    L = {1: limb_pts(1, sh, 22), -1: limb_pts(-1, sh, 22)}
    wd = lambda t: 2.5 - 1.3 * t ** 0.8            # noqa: E731
    hh = lambda t: 1.9 - 0.9 * t                   # noqa: E731
    bx = bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_wood%d" % s, L[s], lambda t: lsec(wd(t), hh(t), 0.25), P["wood"], "limb_wood")
        noisy(ob, 0.1, 0.05, 11 + s)
        stave("limb_lam%d" % s, L[s], lambda t: [(x + wd(t) * 0.2, z * 1.14 + (0.18 if z > 0 else -0.18)) for x, z in lsec(wd(t) * 0.28, hh(t) * 0.85, 0.2)], P["wood2"], "limb_lam")
        for k, (t0, t1, ph) in enumerate(((0.06, 0.34, 0.0), (0.5, 0.78, 1.0))):         # canvas lashing wound round the limb
            F.path_helix("limb_wrap%d_%d" % (s, k), L[s], t0, t1, 9.0, wd((t0 + t1) / 2) + 0.35, hh((t0 + t1) / 2) + 0.3, 0.32, P["canvas"], "limb_wrap", per=5, phase=ph)
        for k, t in enumerate((0.36, 0.48, 0.9)):
            band("limb_brass%d_%d" % (s, k), L[s], t, lambda tt, t=t: lsec(wd(t) + 0.1, hh(t) + 0.05, 0.25), 0.5, P["brass"], "limb_brass", width=0.012 if k < 2 else 0.04)
        for k, t in enumerate((0.15, 0.23, 0.31, 0.58, 0.66, 0.74)):                       # brass studs down the back, a ranging scale
            p, tn, nr = at(L[s], t)
            K.blob("limb_stud%d_%d" % (s, k), tuple(p + nr * (wd(t) * 0.35) + V((0, 0, hh(t) * 0.95))), (0.6, 0.6, 0.45), P["brass"], seed=k, lump=0.0, rings=3, segs=5, part="limb_stud")
        pe, tn, nr = at(L[s], 1.0)
        K.tapered("limb_nock%d" % s, [pe - tn * 4.0, pe, pe + nr * 1.6 + tn * 0.6], 1.15, 0.35, P["brass"], sides=6, sub=2, part="limb_nock")
    pe, tn, nr = at(L[1], 1.0)                                                           # a small red pennant on the upper tip
    path = [pe + tn * -1.5 + nr * 1.2 + V((0, 0, 0.0))] + [pe + tn * -(2.0 + 2.0 * j) + nr * (2.0 + 0.9 * j) + V((0, 0, 1.0 * math.sin(j * 0.9))) for j in range(1, 8)]
    K.ribbon("limb_pennant", path, lambda u: 3.6 * (1 - 0.7 * u) + 0.5, P["flag"], facing=(0, 0, 1), thick=0.12, part="limb_pennant", fray=0.0, rng_=rr, edge_wave=0.15, notch=0.7)
    # the riser: a long straight olive handle, a leather and canvas wrap, the stone, a brass spyglass held on two straps
    riser("riser_core", sh, [(-27.0, 2.2, 2.4), (-23.0, 3.0, 2.8), (-12.0, 2.7, 2.7), (2.6, 2.7, 2.7), (4.6, 3.6, 3.0), (14.0, 3.8, 3.0), (23.0, 3.0, 2.8), (27.0, 2.2, 2.4)], P["wood"])
    hand_binding(sh, P["leather"], P["canvas"], -10.0, 2.6, 2.6, bands=(-9.4, 1.4), band_mat=P["brass"], turns=8, cord=0.45)
    F.jewel(sh, P["gem"], P["brass"], rr, y=7.6, r=3.2, claws=5)
    sc = V((bx + 6.4, 17.0, 0.0))
    bl.lathe("riser_scope", [(0.9, -14.0), (1.5, -13.0), (1.6, -6.0), (1.9, 2.0), (1.9, 8.0), (2.5, 11.5), (2.7, 14.0), (2.3, 14.4)], P["brass"], sides=12, axis="y", origin=(sc.x, sc.y, 0.0), part="riser_scope")
    K.bl.lathe("riser_scope_lens", [(0.05, 14.45), (2.1, 14.5), (2.1, 14.9), (0.05, 15.0)], P["lens"], sides=12, axis="y", origin=(sc.x, sc.y, 0.0), part="riser_scope")
    for i, dy in enumerate((-7.5, 5.0)):                                                  # two straps: leather rings round the tube, a brass tie back to the riser
        bl.lathe("riser_strap%d" % i, [(1.7, dy - 0.7), (2.15, dy - 0.4), (2.15, dy + 0.4), (1.7, dy + 0.7)], P["leather"], sides=12, axis="y", origin=(sc.x, sc.y, 0.0), part="riser_strap")
        K.bar("riser_tie%d" % i, tuple(V((bx + 1.8, sc.y + dy, 0.0))), tuple(V((sc.x - 1.5, sc.y + dy, 0.0))), 0.8, 0.9, P["brass"], part="riser_strap", bevel_w=0.1)
    for dy in (-12.0, 9.6):
        bl.lathe("riser_scope_ring%d" % int(dy), [(1.8, dy - 0.3), (2.1, dy - 0.2), (2.1, dy + 0.2), (1.8, dy + 0.3)], P["brass"], sides=12, axis="y", origin=(sc.x, sc.y, 0.0), part="riser_scope")
    F.string(sh, P["string"], 0.38)
    F.arrow(sh, P["wood"], P["brass"], P["flag"], 3.3, "bodkin", cap_mat=P["leather"])
    return sh


# ================================================================ BIRD HUNTING: a compound bow, stiff swept limbs, cam wheels on the tips, a cable, a sight window, a stabiliser rod
def bird_palette_v3():
    S = K.surface
    P = {}
    P["carbon"] = S("v3carbon", rgb(40, 42, 50), 0.35, 0.35, wear=0.8, wear_color=rgb(150, 156, 170), wear_metal=1.0, dirt=0.7, dirt_color=(0.004, 0.004, 0.006, 1), scratches=0.7, bump="cross", bump_scale=1.0, bump_strength=0.25,
                    mottle=0.8, mottle_color=rgb(70, 76, 92), fine=0.4, seed=261, bevel=0.18, ao_dist=1.8)
    P["orange"] = S("v3orange", rgb(240, 112, 22), 0.3, 0.36, wear=0.9, wear_color=rgb(255, 200, 120), wear_metal=1.0, dirt=0.8, dirt_color=(0.03, 0.01, 0.0, 1), scratches=0.6, bump="brushed", bump_strength=0.1,
                    mottle=0.8, mottle_color=rgb(170, 60, 10), seed=262, bevel=0.16, ao_dist=1.8)
    P["alloy"] = S("v3alloy", rgb(196, 202, 212), 1.0, 0.26, wear=0.9, wear_color=rgb(255, 255, 255), dirt=0.8, dirt_color=(0.01, 0.012, 0.02, 1), scratches=0.9, bump="brushed", bump_strength=0.15, mottle=0.7, mottle_color=rgb(110, 120, 140),
                   seed=263, bevel=0.18, ao_dist=1.8)
    P["cable"] = S("v3cable", rgb(96, 100, 112), 0.5, 0.45, bump="wrap", bump_strength=0.7, groove=rgb(20, 22, 28), groove_amount=0.9, seed=264, bevel=0.0)
    P["pin"] = S("v3pin", rgb(110, 255, 120), 0.0, 0.3, emit=rgb(110, 255, 120), emit_strength=1.0, seed=265, bevel=0.0)
    P["pin2"] = S("v3pin2", rgb(255, 150, 40), 0.0, 0.3, emit=rgb(255, 150, 40), emit_strength=1.0, seed=266, bevel=0.0)
    P["gem"] = S("v3bgem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=267, bevel=0.0)
    P["string"] = S("v3bstr", rgb(232, 232, 238), 0.0, 0.4, emit=rgb(200, 220, 255), emit_strength=0.3, bump="wrap", bump_strength=0.5, seed=268, bevel=0.0)
    return P


def bird_v3(o):
    sh = dict(belly=21.0, riser=26.0)
    sh["fn"] = prof(21.0, 11.0, 1.0, 0.0, 0.9, 26.0)
    F.begin(o["samples"])
    P = bird_palette_v3()
    rr = K.rng(4303)
    L = {1: limb_pts(1, sh, 22), -1: limb_pts(-1, sh, 22)}
    wd = lambda t: 3.4 - 0.8 * t                   # noqa: E731
    hh = lambda t: 2.7 - 0.5 * t                   # noqa: E731
    bx = bow_x(0, sh)
    xt = bow_x(HALF, sh)
    cams = {}
    for s in (1, -1):
        stave("limb_carbon%d" % s, L[s], lambda t: rsec(wd(t), hh(t), 0.9, 2), P["carbon"], "limb_carbon")
        stave("limb_stripe%d" % s, L[s], lambda t: rsec(0.8, hh(t) * 1.07 + 0.1, 0.3, 1), P["orange"], "limb_stripe")
        for k, t in enumerate((0.06, 0.93)):
            band("limb_collar%d_%d" % (s, k), L[s], t, lambda tt, t=t: rsec(wd(t) + 0.1, hh(t) + 0.1, 0.9, 2), 0.7, P["alloy"], "limb_collar", width=0.03)
        c = V((xt + 3.0, s * (HALF - 3.2), 0.0))                                           # the cam: an eccentric wheel on two plates, an axle through the tip
        outline = [(c.x + 5.2 * (1 + 0.42 * math.cos(a)) * math.cos(a), c.y + 5.2 * (1 + 0.42 * math.cos(a)) * math.sin(a)) for a in [TAU * i / 18 for i in range(18)]]
        for z in (1, -1):
            K.plate("limb_cam%d_%d" % (s, z), outline, z * 1.9, z * 3.3, P["orange"], part="limb_cam", bevel_w=0.15)
        ob = bl.lathe("limb_axle%d" % s, [(0.05, -4.4), (1.9, -4.4), (1.9, -3.6), (1.2, -3.6), (1.2, 3.6), (1.9, 3.6), (1.9, 4.4), (0.05, 4.4)], P["alloy"], sides=10, axis="z", origin=(c.x, c.y, 0.0), part="limb_cam")
        K._finish_obj(ob, "limb_cam", True, 50.0)
        cams[s] = c
    # the riser: a cut-out frame of two rails between pocket blocks, a hub with the stone, a sight window, a forward stabiliser
    ys = [-26.0 + 52.0 * i / 8 for i in range(9)]
    front = [V((bx + 2.6 + 3.4 * (1 - (y / 26.0) ** 2), y, 0.0)) for y in ys]
    rear = [V((bx - 3.0, y, 0.0)) for y in ys]
    stave("riser_rail_f", front, lambda t: rsec(1.5, 2.9, 0.6, 1), P["carbon"], "riser_rail")
    stave("riser_rail_r", rear, lambda t: rsec(1.5, 2.9, 0.6, 1), P["carbon"], "riser_rail")
    riser("riser_pocketU", sh, [(21.0, 4.6, 3.4), (26.0, 4.9, 3.6), (31.0, 3.6, 3.1)], P["alloy"])
    riser("riser_pocketL", sh, [(-31.0, 3.6, 3.1), (-26.0, 4.9, 3.6), (-21.0, 4.6, 3.4)], P["alloy"])
    disc("riser_hub", (bx, 7.6), 5.4, -3.1, 3.1, P["carbon"], "riser_hub", sides=20)
    annulus("riser_hub_ring_a", (bx, 7.6), 4.4, 5.6, 2.6, 3.4, P["orange"], "riser_hub", sides=20)
    annulus("riser_hub_ring_b", (bx, 7.6), 4.4, 5.6, -3.4, -2.6, P["orange"], "riser_hub", sides=20)
    hand_binding(sh, P["carbon"], P["orange"], -10.0, 2.4, 2.4, bands=(-9.4, 1.4), band_mat=P["alloy"], turns=8, cord=0.4)
    F.jewel(sh, P["gem"], P["alloy"], rr, y=7.6, r=3.3, claws=6, z=3.3)
    rc = V((bx + 7.4, 17.5, 0.0))                                                          # the sight window: a ring facing the archer, three fibre pins, a bracket back to the rail
    ob = bl.lathe("riser_sight", [(3.1, -0.5), (3.1, 0.5), (2.4, 0.5), (2.4, -0.5), (3.1, -0.5)], P["alloy"], sides=16, axis="x", origin=(rc.x, rc.y, 0.0), caps=(False, False), part="riser_sight")
    K._finish_obj(ob, "riser_sight", True, 50.0)
    K.bar("riser_sight_arm", tuple(V((bx + 3.4, 17.5, 0.0))), tuple(V((rc.x, rc.y - 2.8, 0.0))), 0.9, 1.0, P["alloy"], part="riser_sight", bevel_w=0.1)
    for i, a in enumerate((math.pi / 2, math.pi / 2 + TAU / 3, math.pi / 2 + 2 * TAU / 3)):
        p0 = rc + V((0.0, 2.6 * math.cos(a), 2.6 * math.sin(a)))
        K.tapered("riser_pin%d" % i, [p0, p0 - V((0.0, 1.5 * math.cos(a), 1.5 * math.sin(a)))], 0.3, 0.22, P["pin"] if i != 1 else P["pin2"], sides=4, sub=1, part="riser_sight")
    K.tube("riser_stab", [tuple(V((bx + 4.8, -9.0, 0.0))), tuple(V((bx + 30.0, -9.0, 0.0)))], 0.8, P["carbon"], sides=6, sub=1, part="riser_stab")
    for i, dx in enumerate((15.0, 21.0)):
        bl.lathe("riser_damp%d" % i, [(0.9, dx - 0.8), (1.9, dx - 0.5), (1.9, dx + 0.5), (0.9, dx + 0.8)], P["orange"], sides=10, axis="x", origin=(bx, -9.0, 0.0), part="riser_stab")
    bl.lathe("riser_weight", [(0.9, 25.0), (2.3, 25.5), (2.3, 31.0), (1.4, 32.2)], P["alloy"], sides=12, axis="x", origin=(bx, -9.0, 0.0), part="riser_stab")
    # the cable from cam to cam, held out in front of the riser by a guard rod
    up, lo = cams[1], cams[-1]
    cab = [up + V((6.0, -1.5, -2.7)), V((bx + 8.0, 40.0, -2.7)), V((bx + 9.4, 14.0, -2.7)), V((bx + 9.4, -14.0, -2.7)), V((bx + 8.0, -40.0, -2.7)), lo + V((6.0, 1.5, -2.7))]
    K.tube("limb_cable", [tuple(p) for p in cab], 0.45, P["cable"], sides=5, sub=3, part="limb_cable")
    K.tube("riser_guard", [tuple(V((bx + 2.8, 0.0, -2.7))), tuple(V((bx + 9.4, 0.0, -2.7)))], 0.9, P["alloy"], sides=6, sub=1, part="riser_guard")
    F.string(sh, P["string"], 0.4)
    F.arrow(sh, P["carbon"], P["alloy"], P["orange"], 3.3, "broad", cap_mat=P["orange"])
    return sh


# ================================================================ FROZEN RAIN: a short heavy prod, thick faceted ice limbs set in steel, a stirrup ring, icicles hung from the limbs
def frozen_palette_v3():
    S = K.surface
    P = F.frozen_palette()
    P["steel"] = S("v3fsteel", rgb(112, 124, 140), 1.0, 0.3, wear=0.9, wear_color=rgb(220, 232, 245), dirt=0.8, dirt_color=(0.0, 0.01, 0.025, 1), scratches=0.9, bump="hammer", bump_scale=0.8, bump_strength=0.2,
                   mottle=0.8, mottle_color=rgb(60, 80, 110), seed=271, bevel=0.2, ao_dist=1.8)
    P["glacier"] = S("v3glacier", rgb(150, 218, 255), 0.0, 0.1, transmission=0.3, ior=1.3, rim=(rgb(240, 255, 255), 0.9), emit=rgb(110, 190, 255), emit_strength=0.3, mottle=0.9, mottle_color=rgb(80, 170, 240),
                     bump="grain", bump_scale=0.6, bump_strength=0.3, wear=0.6, wear_color=rgb(255, 255, 255), seed=272, bevel=0.1, ao_dist=2.0)
    return P


def frozen_v3(o):
    sh = dict(belly=36.0, riser=10.0)
    sh["fn"] = prof(36.0, 6.0, 1.3, 2.0, 0.8, 10.0)
    F.begin(o["samples"])
    P = frozen_palette_v3()
    rr = K.rng(7803)
    L = {1: limb_pts(1, sh, 22), -1: limb_pts(-1, sh, 22)}
    wd = lambda t: 5.8 - 2.6 * t                   # noqa: E731
    hh = lambda t: 4.6 - 2.2 * t                   # noqa: E731
    bx = bow_x(0, sh)
    hexsec = lambda t: [(wd(t) * math.cos(TAU * k / 6 + 0.52), hh(t) * math.sin(TAU * k / 6 + 0.52)) for k in range(6)]      # noqa: E731
    for s in (1, -1):
        ob = stave("limb_ice%d" % s, L[s], hexsec, P["glacier"], "limb_ice", smooth=False)
        noisy(ob, 0.3, 0.09, 81 + s)
        stave("limb_core%d" % s, L[s], lambda t: rsec(wd(t) * 0.3, hh(t) * 0.45, 0.4, 1), P["core"], "limb_core")
        for k, t in enumerate((0.06, 0.38, 0.7, 0.93)):
            band("limb_steel%d_%d" % (s, k), L[s], t, lambda tt, t=t: [(x * 1.08, z * 1.08) for x, z in hexsec(t)], 0.4, P["steel"], "limb_steel", width=0.026 if k in (0, 3) else 0.012)
        for k, t in enumerate((0.2, 0.34, 0.5, 0.64, 0.78)):                                  # crystal growth along the back of the limb
            p, tn, nr = at(L[s], t)
            a = rr.uniform(0.0, 0.7)
            d = (nr * math.cos(a) + tn * math.sin(a) * 0.8 + V((0, 0, rr.uniform(-0.6, 0.6)))).normalized()
            K.shard("limb_cryst%d_%d" % (s, k), p + nr * (wd(t) * 0.5) + V((0, 0, hh(t) * 0.5 * (1 if k % 2 else -1))), (3.4 - 1.4 * t) * rr.uniform(0.9, 1.3), P["crystal"] if k % 2 else P["ice2"], rr,
                    part="limb_cryst", sides=6, stretch=3.2, dir_=d)
        for k, t in enumerate((0.16, 0.3, 0.44, 0.58, 0.72, 0.84)):                          # icicles: frozen rain, hung from the front edge of the limb and falling (-y)
            p, tn, nr = at(L[s], t)
            base = p + nr * (wd(t) * 0.55) + V((0, 0, hh(t) * 0.45 * (1 if k % 2 else -1)))
            ln = (24.0 - 11.0 * t) * rr.uniform(0.8, 1.2)
            K.tapered("limb_icicle%d_%d" % (s, k), [base, base + V((1.2, -ln * 0.5, 0.1)), base + V((1.6, -ln, 0.0))], 1.7 - 0.6 * t, 0.05, P["ice2"], sides=5, sub=1, part="limb_icicle")
        pe, tn, nr = at(L[s], 1.0)
        K.tapered("limb_nock%d" % s, [pe - tn * 5.0, pe, pe + tn * 0.9 + nr * 2.2, pe + tn * 0.2 + nr * 4.4], 1.5, 0.2, P["steel"], sides=6, sub=3, part="limb_nock")
    p, tn, nr = at(L[-1], 0.84)                                                              # the stirrup: a steel ring hanging off the front of the lower limb
    sc = p + nr * (wd(0.84) + 4.0)
    K.tube("limb_stirrup", [tuple(sc + V((6.0 * math.cos(a), 6.0 * math.sin(a), 0.0))) for a in [TAU * i / 14 for i in range(15)]], 1.0, P["steel"], sides=6, sub=1, part="limb_stirrup")
    # the riser: a block of glacier ice in steel, the stone in it, a hex plate and a snowflake above
    riser("riser_core", sh, [(-15.0, 2.9, 3.2), (-11.0, 3.9, 3.8), (-7.0, 3.4, 3.6), (3.0, 3.4, 3.6), (5.0, 5.4, 4.4), (11.0, 6.0, 4.4), (16.0, 4.4, 3.8), (19.0, 3.0, 3.2)], P["core"])
    hand_binding(sh, P["core"], P["steel"], -10.5, 2.4, 3.0, bands=(-10.0, -4.5, 1.8), band_mat=P["steel"], turns=7, cord=0.32)
    F.jewel(sh, P["gem"], P["steel"], rr, y=8.0, r=3.8, claws=6, z=3.9)
    for z in (1, -1):
        K.stepped_plate("riser_hex%d" % z, [(bx + 4.4 * math.cos(TAU * i / 6 + 0.52), 16.5 + 4.4 * math.sin(TAU * i / 6 + 0.52)) for i in range(6)], 3.2 if z > 0 else -3.2 - 1.5, P["steel"], steps=3, step_h=0.5, inset=0.8, part="riser_hex", up=z)
    for k in range(7):                                                                         # a burst of glacier crystals off the back of the riser
        a = math.radians(-50 + 14 * k + rr.uniform(-4, 4))
        d = V((math.cos(a) * 0.9, math.sin(a) * 0.7 - 0.05, rr.uniform(-0.45, 0.45))).normalized()
        K.shard("riser_burst%d" % k, V((bx + 4.0, -2.0 + 1.6 * k - 6.0, rr.uniform(-1.2, 1.2))), 2.6 + 0.5 * math.sin(k * 1.3), P["crystal"] if k % 2 else P["ice2"], rr, part="riser_burst", sides=6, stretch=3.8, dir_=d)
    F.string(sh, P["string"], 0.45)
    F.arrow(sh, P["ice"], P["crystal"], P["frost"], 3.3, "bodkin", cap_mat=P["steel"], glow_mat=None)
    return sh


# ================================================================ EAGLE PIERCER: an asymmetric raptor bow: a long slender upper limb in overlapping armour scales ending in an eagle's head, a heavy straight lower limb that ends in talons, a lance at the riser
def eagle_palette_v3():
    S = K.surface
    P = {}
    P["lacq"] = S("v3lacq", rgb(54, 34, 26), 0.2, 0.34, wear=0.8, wear_color=rgb(160, 110, 80), dirt=0.8, dirt_color=(0.01, 0.006, 0.004, 1), scratches=0.7, bump="grain", bump_scale=1.0, bump_strength=0.25,
                  mottle=1.0, mottle_color=rgb(92, 58, 40), fine=0.4, seed=281, bevel=0.16, ao_dist=2.0)
    P["plate"] = S("v3plate", rgb(92, 84, 88), 1.0, 0.3, wear=0.95, wear_color=rgb(238, 200, 120), dirt=0.8, dirt_color=(0.01, 0.008, 0.01, 1), scratches=0.9, bump="hammer", bump_scale=0.9, bump_strength=0.2,
                   mottle=0.8, mottle_color=rgb(50, 44, 56), seed=282, bevel=0.2, ao_dist=1.6)
    P["white"] = S("v3white", rgb(240, 236, 226), 0.0, 0.42, wear=0.7, wear_color=rgb(255, 255, 250), dirt=0.8, dirt_color=(0.03, 0.025, 0.02, 1), scratches=0.5, bump="grain", bump_strength=0.15, mottle=0.9, mottle_color=rgb(190, 180, 160),
                   seed=283, bevel=0.14, ao_dist=2.0)
    P["gold"] = S("v3egold", rgb(232, 178, 56), 1.0, 0.28, wear=0.9, wear_color=rgb(255, 240, 170), dirt=0.9, dirt_color=(0.04, 0.025, 0.006, 1), scratches=0.7, bump="hammer", bump_scale=0.7, bump_strength=0.2,
                  mottle=0.9, mottle_color=rgb(130, 84, 20), seed=284, bevel=0.18, ao_dist=1.8)
    P["eye"] = S("v3eeye", rgb(255, 150, 20), 0.0, 0.3, emit=rgb(255, 140, 20), emit_strength=1.2, seed=285, bevel=0.0)
    P["gem"] = S("v3egem", rgb(244, 172, 20), 0.1, 0.06, emit=rgb(255, 196, 50), emit_strength=0.7, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 232, 100), seed=286, bevel=0.0)
    P["string"] = S("v3estr", rgb(230, 214, 176), 0.0, 0.5, bump="wrap", bump_strength=0.8, groove=rgb(90, 70, 40), groove_amount=0.8, seed=287, bevel=0.0)
    return P


def eagle_v3(o):
    sh = dict(belly=24.0, riser=12.0)
    sh["fn"] = prof(24.0, 5.0, 1.6, 5.0, 0.7, 12.0, xt_lo=10.0, p_lo=1.0, flick_lo=0.0)
    F.begin(o["samples"])
    P = eagle_palette_v3()
    rr = K.rng(5303)
    L = {1: limb_pts(1, sh, 24), -1: limb_pts(-1, sh, 24)}
    wd = {1: lambda t: 3.4 - 1.7 * t, -1: lambda t: 4.6 - 1.7 * t}            # the upper limb is slender, the lower one heavy
    hh = {1: lambda t: 2.3 - 1.0 * t, -1: lambda t: 3.5 - 1.3 * t}
    bx = bow_x(0, sh)
    ob = stave("limb_lacq1", L[1], lambda t: lsec(wd[1](t), hh[1](t), 0.3), P["lacq"], "limb_lacq")
    noisy(ob, 0.08, 0.06, 301)
    ob = stave("limb_lacq-1", L[-1], lambda t: rsec(wd[-1](t), hh[-1](t), 1.2, 2), P["lacq"], "limb_lacq")
    noisy(ob, 0.08, 0.06, 302)
    for k in range(9):                                                                          # armour scales, overlapping toward the tip, on both faces of the upper limb
        t = 0.08 + 0.1 * k
        p, tn, nr = at(L[1], t)
        for z in (1, -1):
            K.scale_plate("limb_scale%d_%d" % (k, z), p + V((0, 0, z * (hh[1](t) * 0.96))), (0, 0, z), wd[1](t) * 1.3, P["plate"] if k % 2 == 0 else P["white"], up=tuple(-tn), thick=0.16, rows=3, aspect=1.5, tilt=16.0,
                          shape="round" if k % 3 else "point", part="limb_scale")
    for k, t in enumerate((0.04, 0.5, 0.93)):
        band("limb_gold1_%d" % k, L[1], t, lambda tt, t=t: lsec(wd[1](t) + 0.12, hh[1](t) + 0.1, 0.3), 0.45, P["gold"], "limb_gold", width=0.016 if k == 1 else 0.03)
    for k, t in enumerate((0.1, 0.4, 0.7)):                                                    # the lower limb: gold collars and three steel plates
        band("limb_gold-1_%d" % k, L[-1], t, lambda tt, t=t: rsec(wd[-1](t) + 0.12, hh[-1](t) + 0.1, 1.2, 2), 0.5, P["gold"], "limb_gold", width=0.03)
    for k, t in enumerate((0.16, 0.3, 0.44, 0.58, 0.72, 0.86)):                              # the lower limb wears big pointed plates, white and steel
        p, tn, nr = at(L[-1], t)
        for z in (1, -1):
            K.scale_plate("limb_lplate%d_%d" % (k, z), p + V((0, 0, z * (hh[-1](t) * 0.97))), (0, 0, z), wd[-1](t) * 0.95, P["white"] if k % 2 else P["plate"], up=tuple(-tn), thick=0.2, rows=3, aspect=1.5, tilt=14.0, shape="point", part="limb_lplate")
    # the eagle's head on the upper tip: white head, a gold hooked beak, a burning eye
    pe, tn, nr = at(L[1], 1.0)
    hc = V((pe.x + 2.6, pe.y - 1.0, 0.0))
    K.tube("limb_hneck", [tuple(pe - tn * 4.0), tuple(pe), tuple(pe + nr * 1.2), tuple(hc - V((1.6, 0, 0)))], 1.4, P["lacq"], sides=8, sub=3, part="limb_head", taper=(1.6, 1.3))
    K.blob("limb_hskull", tuple(hc), (3.3, 2.6, 2.4), P["white"], seed=17, lump=0.06, rings=8, segs=10, part="limb_head")
    K.tapered("limb_hbeak", [hc + V((2.3, 0.2, 0)), hc + V((5.8, -0.2, 0)), hc + V((8.0, -2.0, 0)), hc + V((8.4, -4.8, 0))], 2.1, 0.12, P["gold"], sides=8, sub=3, part="limb_head")
    for z in (1, -1):
        K.blob("limb_heye%d" % z, tuple(hc + V((1.4, 0.8, 2.1 * z))), (0.6, 0.6, 0.45), P["eye"], seed=z + 9, lump=0.0, rings=4, segs=6, part="limb_head")
        K.tapered("limb_hbrow%d" % z, [hc + V((-0.6, 1.9, 1.9 * z)), hc + V((2.0, 1.5, 2.4 * z)), hc + V((4.0, 0.6, 1.8 * z))], 0.8, 0.3, P["plate"], sides=5, sub=1, part="limb_head")
    for i in range(3):
        K.tapered("limb_hcrest%d" % i, [hc + V((-1.8, 0.8 + 0.5 * i, (i - 1) * 0.9)), hc + V((-4.2, 1.6 + 0.8 * i, (i - 1) * 1.1)), hc + V((-6.4, 1.2 + 1.2 * i, (i - 1) * 1.3))], 0.7, 0.07, P["lacq"], sides=5, sub=1, part="limb_head")
    # the lower tip: an eagle's foot, three talons forward and one back
    pe2, tn2, nr2 = at(L[-1], 1.0)
    for i, (dz, ln) in enumerate(((-1.8, 8.0), (0.0, 9.5), (1.8, 8.0))):
        K.tapered("limb_talon%d" % i, [pe2 - tn2 * 1.5 + V((0, 0, dz * 0.5)), pe2 + nr2 * 3.0 + V((0, 0.5, dz)), pe2 + nr2 * (ln * 0.8) + V((0.0, -1.6, dz * 1.3)), pe2 + nr2 * (ln * 0.9) + V((0.0, -4.4, dz * 1.2))], 1.15, 0.1, P["gold"], sides=5, sub=2, part="limb_foot")
    K.tapered("limb_nockL", [pe2 - tn2 * 3.0, pe2, pe2 + nr2 * 0.6], 1.3, 0.4, P["gold"], sides=6, sub=2, part="limb_foot")
    # the riser: lacquer, a hub with the stone held in three talons per face, a lance that points at the target
    riser("riser_core", sh, [(-15.0, 2.8, 3.0), (-11.0, 3.8, 3.5), (-7.0, 3.3, 3.3), (3.0, 3.3, 3.3), (5.0, 5.0, 3.8), (11.0, 5.8, 3.8), (16.0, 4.2, 3.4), (19.0, 2.8, 2.9)], P["lacq"])
    hand_binding(sh, P["lacq"], P["gold"], -10.0, 2.5, 2.7, bands=(-9.4, -4.0, 1.7), band_mat=P["plate"], turns=8, cord=0.34)
    F.jewel(sh, P["gem"], P["gold"], rr, y=8.0, r=3.8, claws=6, z=3.5)
    for z in (1, -1):
        for k, dx in enumerate((-1.0, 0.0, 1.0)):
            ax = 3.1 * dx
            K.tapered("riser_claw%d_%d" % (z, k), [V((bx + ax * 1.0, 1.0, 3.0 * z)), V((bx + ax * 1.55, 5.5, 4.4 * z)), V((bx + ax * 1.45, 10.5, 5.2 * z)), V((bx + ax * 0.7, 13.6, 4.4 * z))], 1.05, 0.12, P["gold"], sides=5, sub=2, part="riser_claw")
    K.tapered("riser_lance", [V((bx + 1.0, 13.0, 0.0)), V((bx + 12.0, 15.2, 0.0)), V((bx + 24.0, 17.6, 0.0)), V((bx + 36.0, 19.8, 0.0))], 2.8, 0.1, P["plate"], sides=6, sub=3, part="riser_lance")
    bl.lathe("riser_lance_ring", [(2.4, 7.0), (3.3, 7.5), (3.3, 9.0), (2.2, 9.6)], P["gold"], sides=10, axis="x", origin=(bx + 3.0, 14.2, 0.0), part="riser_lance")
    F.string(sh, P["string"], 0.4)
    F.arrow(sh, P["plate"], P["gold"], P["white"], 3.3, "bodkin", cap_mat=P["lacq"])
    return sh


# ================================================================ DRIVER
BUILDERS = {"sagittarius": sagittarius_v3, "hunters_bow": hunters_v3, "scouts_bow": scouts_v3, "bird_hunting": bird_v3, "frozen_rain": frozen_v3, "eagle_piercer": eagle_v3}


def _wrap(fn, check):
    def run(o):
        sh = fn(o)
        if check:
            connectivity()
        return sh
    return run


def selftest():
    """The check passes on the Sagittarius, and it names a part left floating 3 units off the riser (the failure the owner reported)."""
    sh = sagittarius_v3({"samples": 4})
    assert connectivity(strict=False) == [], "the Sagittarius has a floating part"
    K.blob("limb_test_float", (bow_x(0, sh) + 12.0, 11.4, 0.0), (1.0, 1.0, 1.0), bpy.data.materials[0], seed=1, lump=0.0, rings=4, segs=6, part="limb_test_float")
    bad = connectivity(strict=False)
    assert bad == ["limb_test_float"], bad
    print("[selftest] connectivity: the bow is one piece, a floating star is found")


def main(argv):
    import argparse
    if "--selftest" in argv:
        return selftest()
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--trial")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--lowres", action="store_true")
    ap.add_argument("--samples", type=int, default=16)
    ap.add_argument("--no-check", action="store_true")
    a = ap.parse_args(argv)
    for k, fn in BUILDERS.items():
        F.FORMS[k] = (_wrap(fn, not a.no_check), k)
    F.run(a.only.split(","), a.trial, a.quick, a.lowres, a.samples)


if __name__ == "__main__":
    main(sys.argv[1:])

"""The Genesis Demon-Slayer Sword (Black Clover, Asta's anti-magic blade) at the standard of the Genesis weapons of 1.102.0, in the old model's own frame.

Lore the model follows (docs/demon_slayer_genesis_spec.md, DemonSlayerSwordItem): "a crude, dirty greatsword with a split running up from the guard, forged around nothing.
Where other blades cut flesh, this one cuts the concept of an existence. The dark spots on its face are holes into that absence, and when it strikes at full force it shatters
into the void it came from." A five-leaf clover on the pommel blooms crimson near great existences (Resonance 0..4), the blade breaks into four shards during Black Meteorite,
and a Nullification Field of 15 blocks surrounds the wielder.

Frame (model units = the old pixel model's 1/64 block, tools/gen_demon_slayer_textures.py): x across, y along the sword (pommel -26 .. tip 49), z through (the blade is +-1).
The placement is the old renderer's exactly: translate(0.516, 0.523, 0.5), rotate z -45 degrees, scale 0.98/64, translate(0, -11.75, 0) -> the `place` line below.
Blade: a broad slab (13 units at the guard, 11 at the point) with a chiselled slanted point, a framed panel on both faces, chipped edges, riveted patches and a split (a slot) from
y 3 to 19. The blade is cut into the four shards of the old fracture (y 1 | 11 | 22 | 33 | 49): the shards are groups `s1..s3` posed by the `fracture` animation of the renderer
(client/relics/item/vu/ds/DsDynamics). Palette: 1E1A22 black iron, DC122E crimson (the old CRIMSON), FF3A4A ember, 070305 ink.
"""
import math

import numpy as np

import vu_geo as G
import vu_geo2 as G2
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (-30.0, 50.0)
PLACE = (0.516, 0.523, 45.0, 0.98 / 64.0, 11.75)          # ox, oy, angle, scale, ymin: the old DemonSlayerMesh#draw transform
CRIM = (220, 18, 46)
EMBER = (255, 58, 74)
INK = (7, 3, 5)
IRON = (34, 30, 38)

Y0, Y1, TIP_R = 1.0, 49.0, 46.0
CUTS = [1.0, 11.0, 22.0, 33.0, 49.0]
SLOT_X, SLOT_Y0, SLOT_Y1 = 1.0, 3.0, 19.0
HT = 1.0
# chips in the edges: (start y, length, depth): the edge drops by `depth` at once and climbs back over `length` (a hammered-out notch)
LCHIPS = [(7.6, 3.0, 0.9), (13.6, 2.4, 0.6), (17.4, 3.6, 1.3), (26.2, 3.2, 0.8), (30.7, 2.0, 1.1), (36.6, 4.2, 0.7), (41.4, 2.6, 1.2), (44.2, 1.8, 0.5)]
RCHIPS = [(5.2, 2.6, 0.8), (10.4, 3.5, 1.1), (23.0, 3.0, 0.7), (28.5, 4.0, 1.3), (34.7, 2.2, 0.9), (39.4, 3.0, 0.6)]
# the dark spots (holes into the absence), as in the old model: (cx, cy, rx, ry) front and back
SPOTS_FRONT = [(-3.5, 25.5, 1.6, 1.6), (-4.5, 23.6, 0.7, 0.6), (-5.2, 25.8, 0.4, 0.8), (3.0, 30.5, 1.1, 1.6), (1.6, 30.8, 0.6, 0.8), (3.0, 32.5, 0.55, 0.5),
               (-2.0, 38.0, 1.1, 1.1), (-2.0, 39.6, 0.5, 0.5), (4.0, 14.0, 1.0, 1.0), (4.0, 15.6, 0.5, 0.5), (-4.0, 6.0, 1.0, 1.0), (1.0, 43.0, 1.0, 1.0), (-0.5, 43.0, 0.5, 0.5)]
SPOTS_BACK = [(-3.0, 31.5, 1.1, 1.6), (-1.5, 31.5, 0.5, 0.5), (3.2, 24.5, 1.2, 1.6), (-4.0, 15.0, 1.0, 1.0), (2.0, 40.0, 1.0, 1.0)]
# raised patches riveted over the old wounds: (x0, y0, x1, y1) front and back
PLATES_FRONT = [(-5.4, 13.2, -2.4, 16.8), (1.6, 35.4, 5.0, 40.2), (-4.8, 43.4, -1.4, 46.2), (2.6, 17.2, 5.0, 20.4), (-5.0, 28.6, -2.4, 31.4)]
PLATES_BACK = [(-5.2, 5.0, -2.0, 9.4), (2.0, 26.8, 5.0, 31.0), (-4.6, 17.4, -1.2, 20.6), (2.2, 12.4, 5.2, 16.8), (-5.0, 36.0, -2.0, 40.0), (-1.0, 43.6, 2.4, 46.0)]

SLAB = [(1, 0.55), (0.92, 0.98), (0.62, 1.0), (0.5, 0.86), (-0.5, 0.86), (-0.62, 1.0), (-0.92, 0.98), (-1, 0.55),
        (-1, -0.55), (-0.92, -0.98), (-0.62, -1.0), (-0.5, -0.86), (0.5, -0.86), (0.62, -1.0), (0.92, -0.98), (1, -0.55)]
SLAB_LOD = [(1, 0.5), (0.9, 1.0), (-0.9, 1.0), (-1, 0.5), (-1, -0.5), (-0.9, -1.0), (0.9, -1.0), (1, -0.5)]


def hw(y):
    return 6.5 - (y - Y0) / (Y1 - Y0)


def chip(chips, y, post=True):
    d = 0.0
    for (y0, L, dep) in chips:
        if (y0 <= y if post else y0 < y) and y < y0 + L:
            d = max(d, dep * (1.0 - (y - y0) / L))
    return d


def seg_of(y):
    for i in range(4):
        if CUTS[i] <= y < CUTS[i + 1]:
            return i
    return 3


def grp(i):
    return "" if i == 0 else "s%d" % i


def stations(ya, yb, side, step, full):
    """[(y, left, right, half_thickness)] of a blade piece over [ya, yb]: side 'full' | 'L' | 'R' (a bar either side of the split)."""
    ys = {(round(ya, 4), 1), (round(yb, 4), 0)}
    y = ya + step
    while y < yb - 1e-6:
        ys.add((round(y, 4), 1))
        y += step
    for chips in ((LCHIPS if side != "R" else []), (RCHIPS if side != "L" else [])):
        for (y0, L, dep) in chips:
            for yy in ((y0, 0), (y0, 1), (y0 + L, 1)) if full else ():
                if ya + 1e-6 < yy[0] < yb - 1e-6:
                    ys.add((round(yy[0], 4), yy[1]))
    for yy in ((46.0, 46.75, 47.5, 48.25) if full else (46.0,)):
        if ya + 1e-6 < yy < yb - 1e-6:
            ys.add((yy, 1))
    out = []
    for (y, post) in sorted(ys, key=lambda t: (t[0], t[1])):
        pre = (post == 0)
        # at a jump the pre station has the value just below it, the post station the value just above; at the ends the piece takes the post / pre value that lies inside it
        if abs(y - ya) < 1e-6:
            pre = False
        if abs(y - yb) < 1e-6:
            pre = True
        hl = hw(y) - chip(LCHIPS, y, not pre)
        hr = hw(y) - chip(RCHIPS, y, not pre)
        ht = HT
        if y > TIP_R:
            hr = min(hr, 5.5 - 11.0 * (y - TIP_R) / 3.0)
            ht = HT * (1.0 - 0.5 * (y - TIP_R) / 3.0)
        if side == "L":
            hr = -SLOT_X
        elif side == "R":
            hl = -SLOT_X
        out.append((y, hl, hr, ht))
    return out


def plane_poly(cx, cy, rx, ry, seed, n=8):
    """An irregular, jagged hole: n points round the centre, alternating long and short."""
    rng = np.random.default_rng(seed)
    pts = []
    for k in range(n):
        a = 2 * math.pi * k / n + rng.uniform(-0.18, 0.18)
        r = (1.0 if k % 2 == 0 else rng.uniform(0.45, 0.75)) * rng.uniform(0.85, 1.12)
        pts.append((cx + rx * r * math.cos(a), cy + ry * r * math.sin(a)))
    return pts


def holes(front):
    lst = SPOTS_FRONT if front else SPOTS_BACK
    return [plane_poly(c[0], c[1], c[2], c[3], 100 + i + (0 if front else 50)) for i, c in enumerate(lst)]


def clover_mask(ctx, cx, cy, r, rot=0.0):
    """A five-leaf clover as a mask: five lobes round a centre and a short stem."""
    m = np.zeros(ctx.shape, np.float32)
    for k in range(5):
        a = rot + math.pi / 2 + 2 * math.pi * k / 5
        m = np.maximum(m, tx.mask_ellipse(ctx, cx + 0.55 * r * math.cos(a), cy + 0.55 * r * math.sin(a), 0.5 * r, 0.5 * r, 0.8))
    m = np.maximum(m, tx.mask_polyline(ctx, [(cx, cy), (cx + 0.1 * r, cy - 1.15 * r)], 0.16 * r, 0.8))
    return m


def rivet(p, pos, r, h, normal, sides=5):
    """A small domed rivet head: a frustum standing out of the surface along `normal` (two rings and a cap)."""
    return ml.loft(p, normal, pos, [(0.0, r, r), (h, r * 0.55, r * 0.55)], ml.circle(1.0, sides), caps=(False, True))


def build(it, state=""):
    it.old_place = PLACE
    it.core = (PLACE[4], None)
    full = it.full
    sec = SLAB if full else SLAB_LOD
    step = 2.6 if full else 60.0

    # ---------------------------------------------------------------- the blade, in the four shards of the fracture
    parts = {}

    def bpart(kind, i):
        key = (kind, i)
        if key not in parts:
            p = it.part("%s_%d" % (kind, i), "iron", base=IRON, group=grp(i), smooth=40, density=1.35 if kind == "blade" else 1.1, wear=0.7, mat=dict(dent=0.16, patina=0.12))
            p.painters.append(blade_paint if kind == "blade" else plate_paint)
            parts[key] = p
        return parts[key]

    for i in range(4):
        ya, yb = CUTS[i], CUTS[i + 1]
        cuts = [ya]
        for e in (SLOT_Y0, SLOT_Y1):
            if ya < e < yb:
                cuts.append(e)
        cuts.append(yb)
        for a, b in zip(cuts[:-1], cuts[1:]):
            mid = (a + b) / 2
            in_slot = SLOT_Y0 < mid < SLOT_Y1
            for side in (("L", "R") if in_slot else ("full",)):
                bottom = (a in CUTS[1:-1] and True) or abs(a - SLOT_Y1) < 1e-6 or abs(a - Y0) < 1e-6
                top = (b in CUTS[1:-1]) or abs(b - SLOT_Y0) < 1e-6
                if side != "full":                                       # the bars sit on the solid piece below and carry the solid piece above: no caps inside the shard
                    bottom = a in CUTS[1:-1]
                    top = b in CUTS[1:-1]
                G.blade(it, bpart("blade", i), stations(a, b, side, step, full), section=sec, caps=(full and bottom, full and top))

    # the split's void (layer 1): the dimensional void between the bars, both faces, in the shard it belongs to
    for i in range(4):
        a, b = max(CUTS[i], SLOT_Y0), min(CUTS[i + 1], SLOT_Y1)
        if a >= b:
            continue
        vd = it.part("slot_void_%d" % i, "steel", layer=1, group=grp(i))
        n = 4 if full else 1
        ys = np.linspace(a, b, n + 1)
        for sgn in (1, -1):
            for k in range(n):
                v = [vd.add_vertex((x, y, 0.0)) for (x, y) in ((-SLOT_X, ys[k]), (SLOT_X, ys[k]), (SLOT_X, ys[k + 1]), (-SLOT_X, ys[k + 1]))]
                vd.add_face(*(v if sgn > 0 else v[::-1]))

    # the dark spots (layer 1): jagged holes on the faces, in the shard they lie in
    for front in (True, False):
        lst = SPOTS_FRONT if front else SPOTS_BACK
        for j, poly in enumerate(holes(front)):
            if not full and lst[j][2] < 0.9:
                continue
            cy = lst[j][1]
            vd = it.part("hole_%s%d" % ("f" if front else "b", j), "steel", layer=1, group=grp(seg_of(cy)))
            z = (HT + 0.045) * (1 if front else -1)
            c = vd.add_vertex((lst[j][0], cy, z))
            ring = [vd.add_vertex((x, y, z)) for (x, y) in poly]
            n = len(ring)
            for k in range(0, n, 2):
                a, b, d = ring[k], ring[(k + 1) % n], ring[(k + 2) % n]
                vd.add_face(*((c, a, b, d) if front else (c, d, b, a)))

    # patches riveted over the old wounds, with their rivets; and a row of studs along the border of the panel
    if full:
        for front in (True, False):
            for (x0, y0, x1, y1) in (PLATES_FRONT if front else PLATES_BACK):
                i = seg_of((y0 + y1) / 2)
                zs = 1 if front else -1
                pl = bpart("plate", i)
                G.plate(it, pl, ((x0 + x1) / 2, (y0 + y1) / 2, zs * (HT + 0.02)), (1, 0, 0), (0, 1, 0), (x1 - x0) / 2, (y1 - y0) / 2, 0.22, bevel=0.35)
                rv = bpart("rivet", i)
                for (px, py) in ((x0 + 0.5, y0 + 0.5), (x1 - 0.5, y0 + 0.5), (x1 - 0.5, y1 - 0.5), (x0 + 0.5, y1 - 0.5)):
                    rivet(rv, (px, py, zs * (HT + 0.2)), 0.3, 0.26, (0, 0, zs), 5)
            for k in range(8):
                yy = 4.6 + k * 5.6
                if yy > 44.5:
                    continue
                for sx in (-1, 1):
                    xx = sx * (hw(yy) - 0.42)
                    if SLOT_Y0 - 0.8 < yy < SLOT_Y1 + 0.8 and abs(xx) < 2.0:
                        continue
                    zs2 = 1 if front else -1
                    rivet(bpart("rivet", seg_of(yy)), (xx, yy, zs2 * (HT + 0.0)), 0.26, 0.2, (0, 0, zs2), 4)

    # teeth in the walls of the split: small fangs pointing into it
    if full:
        for i in range(2):
            fg = bpart("plate", i)
            for k in range(6):
                yy = SLOT_Y0 + 1.4 + k * 2.4
                if seg_of(yy) != i:
                    continue
                for sx in (-1, 1):
                    G.cone(fg, (sx * SLOT_X, yy, 0.0), (sx * 0.1, yy + 0.5, 0.0), 0.34, 3)

    # ---------------------------------------------------------------- crossguard: a forged bar, hooked quillons, collar, rivets
    gd = it.part("guard", "iron", base=(40, 36, 44), smooth=40, density=1.3, wear=0.8, mat=dict(dent=0.2))
    gd.painters.append(guard_paint)
    ml.loft(gd, "x", (0, -0.5, 0), [-5.0, 5.0], ml.octagon(5.0, 4.4, 0.9), frame=((0, 1, 0), (0, 0, 1)))
    for sx in (-1, 1):
        n = it.n(9, 2)
        path = []
        for k in range(n + 1):
            t = k / n
            path.append((sx * (4.2 + 8.0 * t), -0.5 + 2.4 * t ** 2.2 - 0.6 * math.sin(math.pi * t), 0.0))
        ml.sweep(gd, path, ml.octagon(2.0, 2.0, 0.55), scales=lambda t: (2.55 - 0.6 * t, 2.1 - 0.7 * t), up=(0, 0, 1), caps=(False, True))
        G.cone(gd, (sx * 12.0, 1.2, 0.0), (sx * 12.6, 4.6, 0.0), 1.0, 4)
        if full:
            for dz in (-1.2, 1.2):
                rivet(gd, (sx * 8.6, -0.1, dz * 1.9 / 1.2 * 1.0), 0.45, 0.35, (0, 0, 1 if dz > 0 else -1), 5)
    ml.loft(gd, "y", (0, 0, 0), [(-3.3, 2.1, 1.7), (-1.9, 2.8, 2.2), (-0.6, 2.8, 2.2), (0.8, 1.6, 1.3)], ml.octagon(2.0, 2.0, 0.6), caps=(True, True))
    rc = it.part("ricasso", "iron", base=(48, 42, 50), smooth=40, density=1.2, wear=0.7, mat=dict(dent=0.2))
    rc.painters.append(guard_paint)
    ml.loft(rc, "y", (0, 0, 0), [(1.0, 4.9, 1.7), (2.0, 5.9, 1.75), (2.9, 6.3, 1.7)], ml.octagon(2.0, 2.0, 0.5), caps=(False, False))

    # ---------------------------------------------------------------- grip: leather core, two cloth wraps, bands
    gr = it.part("grip", "leather", base=(46, 28, 26), smooth=50, uv_mode="loft", density=1.2)
    ml.lathe(gr, "y", (0, 0, 0), [(-22.0, 1.25), (-21.0, 1.5), (-12.0, 1.55), (-3.0, 1.5), (-2.2, 1.85)], sides=it.n(12, 6), caps=(False, False))
    for ph, nm, col in (((0.0, "wrap_a", (52, 14, 20)), (math.pi, "wrap_b", (24, 18, 24))) if full else ()):
        wp = it.part(nm, "cloth", base=col, smooth=55, uv_mode="loft", density=1.3)
        n_w = it.n(48, 4)
        helix = []
        for i in range(n_w + 1):
            t = i / n_w
            a = 6.0 * 2 * math.pi * t + ph
            y = -21.2 + 18.2 * t
            r = 1.72 + 0.12 * math.sin(math.pi * t)
            helix.append((r * math.cos(a), y, r * math.sin(a)))
        G.tube(it, wp, helix, 0.36, sides=4)
    for y in ((-3.6, -9.8, -15.8, -21.4) if full else ()):
        bd = it.part("band_%d" % int(-y), "iron", base=(54, 48, 56), smooth=50, uv_mode="loft", density=1.3)
        ml.collar(bd, y - 0.45, 1.95, 0.9, lip=0.25, sides=10)

    # ---------------------------------------------------------------- pommel: a coin of black iron with the five-leaf clover in relief, a crimson stone, claws, a cord ring
    pm = it.part("pommel", "iron", base=(40, 36, 44), smooth=45, density=1.3, wear=0.8, mat=dict(dent=0.2))
    pm.painters.append(pommel_paint)
    ml.lathe(pm, "z", (0, -24.0, 0), [(-2.7, 2.3), (-2.3, 3.5), (2.3, 3.5), (2.7, 2.3)], sides=it.n(16, 6), caps=(True, True))
    ml.lathe(pm, "y", (0, -21.5, 0), [(0.0, 1.9), (-0.8, 2.3), (-1.4, 2.0)], sides=it.n(12, 6), caps=(False, False))
    st = it.part("pommel_stone", "crystal", base=CRIM, glow=True, smooth=25, density=1.5)
    st.painters.append(stone_paint)
    ml.gem(st, (0, -28.2, 0), 1.45, 1.35, it.n(8, 5), "y")
    cl = it.part("pommel_claws", "iron", base=(54, 48, 56), smooth=40)
    for k in range(4 if full else 0):
        a = math.pi / 4 + k * math.pi / 2
        G.cone(cl, (2.2 * math.cos(a) * 0.55, -26.6, 2.2 * math.sin(a) * 0.55), (1.0 * math.cos(a), -29.4, 1.0 * math.sin(a)), 0.55, 4)
    if full:
        rg = it.part("pommel_ring", "iron", base=(50, 46, 54), smooth=45, uv_mode="loft")
        G.torus(it, rg, (0, -31.4, 0), 1.3, 0.34, (0, 0, 1), major=14, minor=4)

    # the clover of the pommel, in relief on both faces (group `clover`: it grows with the Resonance of the stack)
    it.define_group("clover", pivot=(0.0, -24.0, 0.0), anim=[{"kind": "resonance", "amp": 0.13, "min": 0.3, "max": 1.0, "period": 60.0}], lod_skip=False)
    cv = it.part("clover", "iron", base=(16, 12, 18), group="clover", smooth=45, density=1.6, wear=0.6)
    cv.painters.append(clover_paint)
    for zs in (1, -1):
        for k in range(5):
            a = math.pi / 2 + 2 * math.pi * k / 5
            c = (1.05 * math.cos(a), -24.0 + 1.05 * math.sin(a), zs * 2.64)
            prof = [(0.0, 0.98), (zs * 0.3, 0.9), (zs * 0.58, 0.0)] if full else [(0.0, 0.98), (zs * 0.58, 0.0)]
            ml.lathe(cv, "z", c, prof, sides=8 if full else 5, caps=(False, False))
        if full:
            ml.bar(cv, (0.0, -25.0, zs * 2.7), (0.1, -27.0, zs * 2.7), 0.5, 0.22, up=(0, 0, zs), taper=0.3)

    # ---------------------------------------------------------------- torn cloth hanging from the guard (group `rags`)
    it.define_group("rags", pivot=(0.0, -3.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 7.0, "period": 70.0}, {"kind": "sway", "axis": [1, 0, 0], "deg": 5.0, "period": 95.0, "phase": 0.3}],
                    lod_skip=True, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    if full:
        rg_ = it.part("rags", "cloth", base=(86, 22, 30), group="rags", smooth=60, uv_mode="loft", density=1.0)
        rg_.painters.append(rag_paint)
        for (x0, z0, bend, ln) in ((1.8, 2.3, 2.6, 15.0), (-2.1, 2.3, -2.0, 12.0), (0.4, -2.4, 3.0, 13.5), (-1.2, -2.4, -3.2, 10.5)):
            pts = []
            for k in range(9):
                t = k / 8
                pts.append((x0 + bend * t ** 1.5 + 0.5 * math.sin(t * 5 + x0), -3.2 - ln * t, z0 + (0.4 if z0 > 0 else -0.4) * t * 3 + 0.35 * math.sin(t * 4 + bend)))
            ml.sweep(rg_, pts, ml.rect_ring(1.0, 1.0), scales=lambda t: (1.05 * (1.0 - 0.5 * t) * (1.0 - 0.22 * math.sin(t * 17)), 0.07), up=(0, 0, 1), caps=(False, False))

    # ---------------------------------------------------------------- orbiting anti-magic shards (group `orbit`), drifting blade fragments (group `fragments`), orbiting leaves (group `leaves`)
    it.define_group("orbit", pivot=(0.0, 26.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 24.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.4, "period": 110.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 4, "ticks": 14})
    if full:
        sh = it.part("orbit_shards", "obsidian", base=(12, 8, 14), group="orbit", smooth=30, density=1.0)
        sh.painters.append(shard_paint)
        G2.shard_helix(it, sh, (0.0, 26.0, 0.0), (0, 1, 0), 11.5, 9, 30.0, 3.2, 0.95, sides=4, phase=0.4, turns=1.6, tilt=0.45)
    it.define_group("fragments", pivot=(0.0, 46.0, 0.0), anim=[{"kind": "bob", "axis": [0, 1, 0], "amp": 1.6, "period": 100.0}, {"kind": "sway", "axis": [0, 0, 1], "deg": 9.0, "period": 85.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 6, "ticks": 14})
    if full:
        fr = it.part("fragments", "iron", base=(30, 26, 34), group="fragments", smooth=35, density=1.0, wear=0.9)
        fr.painters.append(fragment_paint)
        rng = np.random.default_rng(404)
        for k, (c, ang, sz) in enumerate((((9.5, 44.0, 1.2), 25, (1.5, 2.1)), ((-10.5, 40.0, -1.0), -30, (1.3, 1.9)), ((9.0, 33.0, -2.0), 70, (1.1, 1.6)), ((-9.5, 51.0, 2.0), 120, (1.2, 1.7)),
                                         ((11.5, 24.0, 1.5), -50, (1.0, 1.4)), ((-11.5, 17.0, -1.5), 15, (1.0, 1.5)))):
            a = math.radians(ang)
            uu, vv = np.array([math.cos(a), math.sin(a), 0.2]), np.array([-math.sin(a), math.cos(a), 0.1])
            nn = np.cross(uu, vv)
            nn = nn / np.linalg.norm(nn)
            uu = uu / np.linalg.norm(uu)
            vv = np.cross(nn, uu)
            ring = []
            for j in range(7):
                ang2 = 2 * math.pi * j / 7 + rng.uniform(-0.2, 0.2)
                r = rng.uniform(0.55, 1.0)
                ring.append((sz[0] * r * math.cos(ang2), sz[1] * r * math.sin(ang2)))
            ml.loft(fr, tuple(nn), np.array(c) - nn * 0.21, [(0.0, 1.0, 1.0), (0.42, 0.78, 0.78)], ring, frame=(tuple(uu), tuple(vv)), caps=(True, True))
    it.define_group("leaves", pivot=(0.0, -24.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": -34.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.1, "period": 90.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    if full:
        lv = it.part("leaves", "obsidian", base=(14, 8, 12), group="leaves", smooth=35, density=1.0)
        lv.painters.append(leaf_paint)
        for k in range(5):
            a = 2 * math.pi * k / 5
            c = np.array([8.2 * math.cos(a), -24.0 + 2.5 * math.sin(a * 2), 8.2 * math.sin(a)])
            tang = np.array([-math.sin(a), 0, math.cos(a)])
            for off in (-0.55, 0.55):
                G.orb(it, lv, c + tang * off, 0.7, 0.2, 0.85, sides=6, stacks=3)
            G.cone(lv, c - np.array([math.cos(a), 0, math.sin(a)]) * 0.3, c - np.array([math.cos(a), 0, math.sin(a)]) * 1.6, 0.5, 4)

    # ---------------------------------------------------------------- halo cards (additive, layer 4) and flares
    it.define_group("ring_guard", pivot=(0.0, -0.5, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 16.0}, {"kind": "breathe", "min": 0.4, "max": 0.9, "period": 120.0}], tint="DC122E", lod_skip=True,
                    alpha=0.9, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "guard_runes", "ring_guard", (0.0, -0.5, -0.4), (1, 0, 0), (0, 1, 0), 13.5, 13.5, "runes")
    it.define_group("ring_inner", pivot=(0.0, -0.5, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": -28.0}, {"kind": "breathe", "min": 0.3, "max": 0.8, "period": 90.0}], tint="FF3A4A", lod_skip=True,
                    alpha=0.8)
    G.card(it, "guard_ring", "ring_inner", (0.0, -0.5, 0.35), (1, 0, 0), (0, 1, 0), 11.0, 11.0, "thick_ring")
    G.card(it, "guard_ring2", "ring_inner", (0.0, -0.5, -0.35), (1, 0, 0), (0, 1, 0), 8.0, 8.0, "ring")
    it.define_group("bloom", pivot=(0.0, -24.0, 0.0), anim=[{"kind": "resonance", "amp": 0.34, "min": 0.22, "max": 1.0, "period": 60.0}], tint="DC122E", lod_skip=True, alpha=1.0)
    for zs in (1, -1):
        G.card(it, "bloom_%s" % ("f" if zs > 0 else "b"), "bloom", (0.0, -24.0, zs * 2.95), (1, 0, 0), (0, 1, 0), 5.2, 5.2, "glow")
        G.card(it, "bloom_star_%s" % ("f" if zs > 0 else "b"), "bloom", (0.0, -24.0, zs * 3.0), (1, 0, 0), (0, 1, 0), 3.8, 3.8, "star")
    it.define_group("aura", anim=[{"kind": "breathe", "min": 0.2, "max": 0.6, "period": 80.0}], tint="DC122E", lod_skip=True, alpha=0.7)
    G.card_cross(it, "blade_aura", "aura", (-0.5, 25.0, 0.0), (0, 1, 0), 8.0, 24.0, "shaft", n=2)
    it.define_group("arcs", pivot=(0.0, 26.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 5.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": 50.0}], tint="FF3A4A", lod_skip=True, alpha=0.95)
    for k in range(4):
        a = k * 1.6
        G.card(it, "void_arc_%d" % k, "arcs", (8.2 * math.cos(a), 12.0 + 11.0 * k, 8.2 * math.sin(a)), (math.cos(a + 1.57), 0, math.sin(a + 1.57)), (0, 1, 0), 3.4, 7.0, "bolt")
    it.define_group("wake", anim=[{"kind": "flicker", "min": 0.1, "max": 0.65, "rate": 3.0}], tint="DC122E", lod_skip=True, alpha=0.6)
    for sgn in (1, -1):
        G.card(it, "blade_wake", "wake", (-5.6, 26.0, sgn * 0.5), (1, 0, 0), (0, 1, 0), 3.8, 25.0, "streak")
    it.define_group("field", pivot=(0.0, 24.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 14.0}, {"kind": "breathe", "min": 0.25, "max": 0.7, "period": 130.0}], tint="DC122E", lod_skip=True,
                    alpha=0.8, draw={"from_scale": 0.0, "delay": 6, "ticks": 16})
    G.card(it, "field_ring", "field", (0.0, 24.0, 0.0), (1, 0, 0), (0, 0, 1), 19.0, 19.0, "thick_ring")
    G.card(it, "field_ring_hi", "field", (0.0, 35.0, 0.0), (1, 0, 0), (0, 0, 1), 12.5, 12.5, "ring")
    it.define_group("field_low", pivot=(0.0, 24.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": -22.0}, {"kind": "breathe", "min": 0.2, "max": 0.55, "period": 100.0}], tint="8A0A1E", lod_skip=True,
                    alpha=0.7)
    G.card(it, "field_ring_lo", "field_low", (0.0, 13.0, 0.0), (1, 0, 0), (0, 0, 1), 15.0, 15.0, "ring")
    # the shards (the old shardPose: drift out along the blade and sideways, tilt, shiver) and the light between them while Black Meteorite runs (alpha follows the fracture)
    SD = {0: (0.0, 0.0, 0.0), 1: (-0.5, 2.6, 0.9), 2: (1.1, 5.2, -0.7), 3: (-0.6, 7.8, 0.6)}
    TILT = {1: (6.0, 5.0), 2: (-8.0, -6.0), 3: (7.0, 4.0)}
    for k in (1, 2, 3):
        d = tuple(0.5 * (SD[k - 1][j] + SD[k][j]) for j in range(3))
        L = math.sqrt(sum(c * c for c in d))
        it.define_group("crack_%d" % k, pivot=(0.0, CUTS[k], 0.0), anim=[{"kind": "fracture", "axis": [d[0] / L, d[1] / L, d[2] / L], "amp": L, "deg": 0.0, "rate": 0.0, "period": 40.0, "phase": k * 0.17},
                                                                      {"kind": "fracture_glow", "min": 0.0, "max": 1.0, "period": 12.0, "phase": k * 0.31}], tint="DC122E", lod_skip=False, alpha=1.0)
        G.card(it, "crack_light_%d" % k, "crack_%d" % k, (0.0, CUTS[k], 0.0), (1, 0, 0), (0, 1, 0), hw(CUTS[k]) + 2.2, 3.6, "glow")
        G.card(it, "crack_flash_%d" % k, "crack_%d" % k, (0.0, CUTS[k], 0.0), (1, 0, 0), (0, 0, 1), hw(CUTS[k]) + 1.0, 3.0, "star")
        d = SD[k]
        L = math.sqrt(sum(c * c for c in d))
        it.define_group("s%d" % k, pivot=(0.0, (CUTS[k] + CUTS[k + 1]) / 2, 0.0), anim=[{"kind": "fracture", "axis": [d[0] / L, d[1] / L, d[2] / L], "amp": L, "deg": TILT[k][0], "rate": TILT[k][1],
                                                                                         "period": 40.0, "phase": k * 0.17}], lod_skip=False)

    it.flare((0.0, -24.0, 3.25), 4.6, "FF3A4A", "star", phase=0.0, pulse=0.8)
    it.flare((0.0, -24.0, -3.25), 4.6, "FF3A4A", "star", phase=0.5, pulse=0.8)
    it.flare((0.0, -28.4, 0.0), 3.4, "FFB0B8", "sparkle", phase=0.2, pulse=0.6)
    it.flare((-5.4, 48.6, 0.0), 5.0, "FF3A4A", "star", phase=0.4, pulse=0.6)
    it.flare((0.0, 19.4, 1.3), 3.6, "FF6A78", "sparkle", phase=0.7, pulse=0.7)
    it.flare((-11.8, 1.0, 0.0), 3.0, "FF3A4A", "sparkle", phase=0.9, pulse=0.5)
    it.flare((11.8, 1.0, 0.0), 3.0, "FF3A4A", "sparkle", phase=0.15, pulse=0.5)

    it.glow = ("FFFFFF", 0.14, 0.4, 1.0)
    it.layers = [
        {"id": "anti_magic_shards", "kind": "orbit_shards", "group": "orbit", "note": "nine shards of black glass with crimson hearts circling the blade"},
        {"id": "guard_rune_ring", "kind": "halo_ring", "group": "ring_guard", "note": "a rune disc turning behind the crossguard"},
        {"id": "guard_rings", "kind": "halo_ring", "group": "ring_inner", "note": "two crimson rings counter-turning on the guard"},
        {"id": "absence_holes", "kind": "distortion", "layer": "void", "note": "the dark spots and the split are holes into the absence (dimensional void shader, reality shimmer)"},
        {"id": "blade_aura", "kind": "light_shafts", "group": "aura", "note": "crossed shafts of crimson light along the blade, breathing"},
        {"id": "void_arcs", "kind": "energy_arcs", "group": "arcs", "note": "four flickering crimson bolts round the blade"},
        {"id": "field_rings", "kind": "halo_ring", "group": "field", "note": "the Nullification Field: two thin rings turning at the height of the blade"},
        {"id": "field_floor", "kind": "halo_ring", "group": "field_low", "note": "a dim ring below, counter-turning"},
        {"id": "clover_leaves", "kind": "orbit_shards", "group": "leaves", "note": "five clover leaves of black glass circling the pommel"},
        {"id": "drifting_fragments", "kind": "orbit_shards", "group": "fragments", "note": "six chips of the blade hanging in the air near the point, as if it were about to shatter"},
        {"id": "torn_cloth", "kind": "cloth", "group": "rags", "note": "four rags of cloth swaying from the guard"},
        {"id": "pommel_bloom", "kind": "halo_ring", "group": "bloom", "note": "the crimson bloom behind the clover; it grows and quickens with the Resonance"},
        {"id": "blade_wake", "kind": "light_shafts", "group": "wake", "note": "a flickering afterimage along the edge"},
        {"id": "hot_flares", "kind": "lens_flare", "flares": "clover, stone, point, split, guard", "note": "star flares on the clover, the stone, the point, the split and the guard ends"},
        {"id": "absence_motes", "kind": "motes", "particle": "dust", "rgb": "DC122E", "where": "blade", "rate": 0.55, "radius": 1.6, "note": "crimson motes snuffed out as they rise off the blade"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "8A0A1E", "radius": 1.2, "note": "a ring of dark crimson motes where the field meets the floor"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse", "note": "core glow breathes 0.4 to 1.0"},
        {"id": "vein_flow", "kind": "vein_wave", "note": "crimson light flows up the cracks and the edge"},
        {"id": "bloom_resonance", "kind": "group_resonance", "group": "bloom", "note": "the bloom grows and quickens with the Resonance of the stack (0..4)"},
        {"id": "clover_grows", "kind": "group_resonance", "group": "clover", "note": "the clover in relief swells with the Resonance"},
        {"id": "shard_orbit", "kind": "group_spin", "group": "orbit", "note": "the shards orbit and bob"},
        {"id": "leaf_orbit", "kind": "group_spin", "group": "leaves", "note": "the leaves circle the pommel against the shards"},
        {"id": "fragment_drift", "kind": "group_bob", "group": "fragments", "note": "the chips hang and sway"},
        {"id": "rag_sway", "kind": "group_sway", "group": "rags", "note": "the cloth sways"},
        {"id": "guard_ring_turn", "kind": "group_spin", "group": "ring_guard", "note": "the rune disc turns"},
        {"id": "inner_ring_turn", "kind": "group_spin", "group": "ring_inner", "note": "the rings turn against it"},
        {"id": "field_turn", "kind": "group_spin", "group": "field", "note": "the field rings turn"},
        {"id": "arc_flicker", "kind": "group_flicker", "group": "arcs"},
        {"id": "aura_breath", "kind": "group_breathe", "group": "aura"},
        {"id": "wake_flicker", "kind": "group_flicker", "group": "wake"},
        {"id": "flare_pulse", "kind": "flare_pulse"},
    ]
    it.draw = {"ticks": 16, "flash": "DC122E", "groups": ["orbit", "leaves", "ring_guard", "field", "rags", "fragments"],
               "note": "the shards and the leaves spiral out of the blade, the rune disc and the field unfold, the cloth drops; a crimson flash"}
    it.swing = {"trail": "absence_cut", "rgb": "DC122E", "rgb2": "070305", "length": 2.2, "width": 0.5, "life": 11, "particles": "void_mote"}
    it.sound = "nusmp:item.genesis.demon_slayer.absence_hum_loop"
    it.acts = {
        "conceptual_severance": {"burst": "severance", "pattern": "line", "rgb": "DC122E", "ring": 6.0, "particles": 40, "sound": "nusmp:item.genesis.demon_slayer.severance"},
        "black_meteorite": {"burst": "meteor_charge", "pattern": "cone", "rgb": "FF3A4A", "ring": 7.0, "particles": 52, "sound": "nusmp:item.genesis.demon_slayer.meteorite"},
        "black_divider": {"burst": "divider", "pattern": "nova", "rgb": "9A1028", "ring": 5.5, "particles": 44, "sound": "nusmp:item.genesis.demon_slayer.divider"},
    }


# ================================================================ painters
def _noise(ctx, cell_units, salt, octaves=3):
    return tx.fbm(ctx.shape, max(2.0, cell_units * ctx.ppu), ctx.seed + salt, octaves)


def blade_paint(ctx):
    """Black iron, grime and rust in the crevices, worn bright rims, a crimson rim light on every outline, the framed panel, engraved lore, holes with charred, glowing rims, cracks."""
    if ctx.kind != "dom" or ctx.axis != 2:
        e = ctx.edge(max(1.0, 0.5 * ctx.ppu))
        tx.put(ctx.glow, e * 0.55, CRIM, 0.7, 0.4, halo=0.6)
        return
    X, Y, sg = ctx.X, ctx.Y, (ctx.sign or 1)
    front = sg > 0
    h = np.maximum(6.5 - (Y - Y0) / (Y1 - Y0), 0.6)
    u = X / h
    # grime, rust and a warm dust in the panel, clean worn metal at the rim
    grime = np.clip(_noise(ctx, 5.5, 11, 4) * 1.5 - 0.35, 0, 1)
    blot = np.clip(_noise(ctx, 1.7, 12, 3) * 1.6 - 0.55, 0, 1)
    rust = np.clip(_noise(ctx, 3.0, 13, 3) * 2.0 - 1.1, 0, 1)
    ctx.img[..., :3] *= (1.0 - 0.5 * grime[..., None] - 0.2 * blot[..., None])
    ctx.img[..., :3] += (np.array([74, 30, 22], np.float32) - ctx.img[..., :3]) * (0.35 * rust)[..., None]
    # the framed panel: a chiselled line on both edges of the inset, darker inside
    for s in (-0.5, 0.5):
        tx.engrave(ctx, tx.soft(u - s, 0.03), 0.6)
    ctx.img[..., :3] *= (1.0 - 0.18 * (np.abs(u) < 0.5)[..., None])
    # the rim light of the silhouette and the worn bright metal on the edge
    silhouette = ((np.abs(u) > 0.72) | (Y > 45.2) | ((np.abs(X) < 1.7) & (Y < 19.6)) | (Y < 1.8)).astype(np.float32)        # the outline, not the seams between the shards
    e1 = ctx.edge(max(1.0, 0.45 * ctx.ppu)) * silhouette
    e2 = ctx.edge(max(2.0, 1.4 * ctx.ppu)) * silhouette
    ctx.img[..., :3] += (np.array([150, 140, 146], np.float32) - ctx.img[..., :3]) * (0.30 * e1 * (0.4 + 0.6 * _noise(ctx, 1.0, 14, 2)))[..., None]
    tx.put(ctx.glow, e1 * 0.95, CRIM, 0.9, 0.3, halo=0.9 * max(1.0, ctx.ppu / 24.0))
    tx.put(ctx.glow, e2 * 0.22, (160, 10, 34), 0.5, 0.2)
    tx.put(ctx.vein, e1 * 0.9, EMBER, 0.9, 0.7)
    # engraved lore: rune bands and the sentence of the lore, a clover at the foot
    if front:
        txt = "FORGED AROUND NOTHING"
        m = tx.mask_text(ctx, txt, 0.4, 33.0, 0.8, rot=90.0, fname="DejaVuSans-Bold.ttf", spacing=1.35)
        m2 = np.zeros(ctx.shape, np.float32)
        for i, gl in enumerate(tx.rune_glyphs(5, 77)):
            m2 = np.maximum(m2, tx.mask_text(ctx, gl, -2.4, 24.0 + i * 5.2, 0.7, rot=90.0, fname="DejaVuSans.ttf"))
        cm = clover_mask(ctx, 3.9, 7.0, 1.5)
    else:
        txt = "WHERE MAGIC WANTS TO BE"
        m = tx.mask_text(ctx, txt, -0.2, 32.0, 0.8, rot=-90.0, fname="DejaVuSans-Bold.ttf", spacing=1.35)
        m2 = np.zeros(ctx.shape, np.float32)
        for i, gl in enumerate(tx.rune_glyphs(5, 91)):
            m2 = np.maximum(m2, tx.mask_text(ctx, gl, 2.4, 23.0 + i * 5.4, 0.7, rot=-90.0, fname="DejaVuSans.ttf"))
        cm = clover_mask(ctx, -3.9, 7.0, 1.5)
    valid = ((Y > 20.2) | (np.abs(X) > 1.2)).astype(np.float32)
    for mk, depth in ((m, 0.65), (m2, 0.5)):
        tx.engrave(ctx, mk * valid, depth)
        tx.put(ctx.glow, mk * valid * 0.35, CRIM, 0.45, 0.2)
        tx.put(ctx.vein, mk * valid * 0.5, EMBER, 0.55, 0.5)
    tx.engrave(ctx, cm, 0.85)
    tx.put(ctx.glow, cm * 0.7, CRIM, 0.8, 0.12, halo=0.8)
    tx.put(ctx.vein, cm * 0.9, EMBER, 0.9, 0.8)
    # the holes into the absence: charred, glowing rims and cracks running out of them
    cracks = np.zeros(ctx.shape, np.float32)
    for i, (poly, spec) in enumerate(zip(holes(front), SPOTS_FRONT if front else SPOTS_BACK)):
        pts = [ctx.plane(x, y) for (x, y) in poly]
        hm = tx.mask_polygon(ctx, pts, 0.8)
        if hm.max() <= 0:
            continue
        rim = np.clip(tx.blur(hm, max(1.5, 0.9 * ctx.ppu)) * 1.7 - hm, 0, 1) * (1 - hm)
        ctx.img[..., :3] *= (1.0 - 0.8 * rim[..., None])
        ctx.img[..., :3] = ctx.img[..., :3] * (1 - hm[..., None]) + np.array([7, 3, 6], np.float32) * hm[..., None]
        tx.put(ctx.glow, np.clip(rim * 1.4, 0, 1) ** 1.6, CRIM, 0.95, 0.55, halo=1.1 * max(1.0, ctx.ppu / 24.0))
        tx.put(ctx.vein, np.clip(rim * 1.2, 0, 1) ** 2.0, EMBER, 0.9, 0.8)
        if spec[2] >= 1.0:
            cmask = tx.crack_star(ctx, ctx.plane(spec[0], spec[1]), spec[2] * 3.2, n=7, seed=300 + i, width=0.12, branch=2, jag=0.34)
            cracks = np.maximum(cracks, cmask * (1 - hm))
    tx.engrave(ctx, cracks, 0.75)
    tx.put(ctx.glow, cracks * 0.8, CRIM, 0.8, 0.45, halo=0.7)
    tx.put(ctx.vein, cracks, EMBER, 0.95, 0.85)
    # hammer scars: short bright scratches
    scr = np.clip(tx.vnoise(ctx.shape, max(2.0, 0.5 * ctx.ppu), ctx.seed + 15, cell_b=max(6.0, 4.0 * ctx.ppu)) - 0.74, 0, 1) * 4.0
    ctx.img[..., :3] += (np.array([160, 150, 156], np.float32) - ctx.img[..., :3]) * (0.35 * np.clip(scr, 0, 1))[..., None]


def plate_paint(ctx):
    if ctx.kind == "dom" and ctx.axis == 2:
        n = np.clip(_noise(ctx, 2.0, 21, 3) * 1.5 - 0.3, 0, 1)
        ctx.img[..., :3] *= (1.0 - 0.35 * n[..., None])
    e = ctx.edge(max(1.0, 0.4 * ctx.ppu))
    ctx.img[..., :3] += (np.array([150, 140, 146], np.float32) - ctx.img[..., :3]) * (0.25 * e)[..., None]


def guard_paint(ctx):
    n = np.clip(_noise(ctx, 3.0, 31, 3) * 1.6 - 0.4, 0, 1)
    ctx.img[..., :3] *= (1.0 - 0.4 * n[..., None])
    e = ctx.edge(max(1.0, 0.5 * ctx.ppu))
    ctx.img[..., :3] += (np.array([150, 140, 146], np.float32) - ctx.img[..., :3]) * (0.28 * e)[..., None]
    if ctx.kind == "dom" and ctx.axis == 2:
        X, Y = ctx.X, ctx.Y
        for sx in (-1, 1):
            m = tx.mask_text(ctx, "ᚠᚢᚦᚨᚱ", sx * 8.0, -0.5, 0.9, rot=0.0, fname="DejaVuSans.ttf")
            tx.engrave(ctx, m, 0.7)
            tx.put(ctx.glow, m * 0.7, CRIM, 0.8, 0.4, halo=0.6)
        line = tx.soft(Y + 0.5, 0.12) * (np.abs(X) < 11.5)
        tx.put(ctx.vein, line * 0.8, EMBER, 0.8, 0.7)


def pommel_paint(ctx):
    n = np.clip(_noise(ctx, 2.5, 41, 3) * 1.5 - 0.3, 0, 1)
    ctx.img[..., :3] *= (1.0 - 0.4 * n[..., None])
    e = ctx.edge(max(1.0, 0.5 * ctx.ppu))
    ctx.img[..., :3] += (np.array([150, 140, 146], np.float32) - ctx.img[..., :3]) * (0.3 * e)[..., None]
    tx.put(ctx.glow, e * 0.8, CRIM, 0.85, 0.5, halo=0.8)
    tx.put(ctx.vein, e * 0.8, EMBER, 0.8, 0.7)
    if ctx.kind == "dom" and ctx.axis == 2:
        X, Y = ctx.X, ctx.Y
        r = np.sqrt(X ** 2 + (Y + 24.0) ** 2)
        ring = tx.soft(r - 2.0, 0.12)
        tx.engrave(ctx, ring, 0.6)
        tx.put(ctx.glow, ring * 0.7, CRIM, 0.8, 0.5, halo=0.7)


def clover_paint(ctx):
    """Near-black clover in relief with a glowing crimson vein in each leaf."""
    ctx.img[..., :3] = ctx.img[..., :3] * 0.55
    e = ctx.edge(max(1.0, 0.45 * ctx.ppu))
    c = ctx.cover()
    tx.put(ctx.glow, e * 0.85, CRIM, 0.9, 0.6, halo=0.9)
    tx.put(ctx.glow, np.clip(c - e, 0, 1) * 0.25, (120, 6, 22), 0.5, 0.3)
    tx.put(ctx.vein, np.clip(e * 0.9, 0, 1), EMBER, 0.9, 0.8)
    tx.put(ctx.vein, tx.soft(ctx.S - 0.5, 0.06) * np.clip(c, 0, 1), EMBER, 0.8, 0.8)


def stone_paint(ctx):
    nz = tx.fbm(ctx.shape, 6.0, ctx.seed + 1, 3)
    ctx.img[..., :3] = tx.lerp3((90, 4, 18), (255, 90, 110), np.clip(nz * 1.4, 0, 1))
    ctx.img[..., 3] = 255
    tx.put(ctx.glow, np.ones(ctx.shape, np.float32), CRIM, 1.0, 0.8, halo=0.6)
    tx.put(ctx.vein, tx.caustics(ctx, 5.0, 4, 2.0), (255, 200, 205), 0.9, 0.9)


def rag_paint(ctx):
    n = np.clip(_noise(ctx, 1.5, 51, 3) * 1.4 - 0.2, 0, 1)
    ctx.img[..., :3] *= (1.0 - 0.45 * n[..., None])
    thread = tx.vnoise(ctx.shape, max(1.0, 0.25 * ctx.ppu), ctx.seed + 52, cell_b=max(4.0, 3.0 * ctx.ppu))
    ctx.img[..., :3] *= (0.8 + 0.4 * thread[..., None])
    tx.put(ctx.vein, np.clip(tx.soft(ctx.S - 0.5, 0.06), 0, 1) * 0.4, CRIM, 0.5, 0.5)


def shard_paint(ctx):
    cv = tx.caustics(ctx, 4.0, 6, 2.0)
    ctx.img[..., :3] += (np.array([150, 20, 44], np.float32) - ctx.img[..., :3]) * (0.55 * cv)[..., None]
    e = ctx.edge(max(1.0, 0.5 * ctx.ppu))
    tx.put(ctx.glow, np.clip(0.35 + 0.65 * e, 0, 1) * 0.8, CRIM, 0.9, 0.6, halo=0.6)
    tx.put(ctx.vein, cv, EMBER, 0.8, 0.8)


def fragment_paint(ctx):
    n = np.clip(_noise(ctx, 1.4, 61, 3) * 1.5 - 0.3, 0, 1)
    ctx.img[..., :3] *= (1.0 - 0.4 * n[..., None])
    e = ctx.edge(max(1.0, 0.5 * ctx.ppu))
    tx.put(ctx.glow, e * 0.9, CRIM, 0.9, 0.55, halo=0.8)
    tx.put(ctx.vein, e, EMBER, 0.9, 0.8)


def leaf_paint(ctx):
    cv = tx.caustics(ctx, 3.0, 8, 2.0)
    ctx.img[..., :3] += (np.array([130, 14, 40], np.float32) - ctx.img[..., :3]) * (0.4 * cv)[..., None]
    e = ctx.edge(max(1.0, 0.5 * ctx.ppu))
    tx.put(ctx.glow, np.clip(0.3 + 0.7 * e, 0, 1) * 0.8, CRIM, 0.9, 0.6, halo=0.6)
    tx.put(ctx.vein, cv * 0.8, EMBER, 0.8, 0.8)


def spec(s):
    s["id"] = "demon_slayer_sword"
    s["slice"] = "demon_slayer"
    s["note"] = ("The Genesis Demon-Slayer. Groups s1..s3 are the shards of Black Meteorite (anim `fracture`, driven by the stack's MeteorUntil); `clover` and `bloom` follow the stack's Resonance "
                 "(anim `resonance`); the renderer is client/relics/item/vu/ds/DemonSlayerVu. Anim kinds `fracture`, `fracture_glow` and `resonance` are read by DsDynamics: fracture = drift "
                 "along `axis` by `amp` x fracture, a turn of `deg` about z and `rate` about x, a shiver of the same period; fracture_glow = alpha follows the fracture; resonance = scale 1 + amp x "
                 "resonance and alpha lerp(min, max, resonance / 4) pulsing faster with it.")

"""The eight BACK relics: cloaks and mantles. Body anchor; the back is +z; every function builds the full, the held and the LOD model from the same code (Detail)."""
import _paths  # noqa: F401
import math

import numpy as np

import gen_worn_lib as L
import mesh_lib as ml
from mesh_lib import Part, sweep, loft, circle
from gw_lib import MAIN, TRIM, GLOW, DARK, ACCENT, EMIT, SHELL, HOT, LIT
import kit as K
from kit import A, unit

def gat(groups, i):
    return groups[min(i, len(groups) - 1)]


PIVOT = (0.0, 0.2, 2.8)             # where a hanging drape is hinged: the shoulders at the back


def cloak_fn(p):
    """The surface of a hanging cloak: u across (0..1), v down (0..1)."""
    w0, w1, ytop, ybot, z0 = p["w0"], p["w1"], p.get("ytop", -0.3), -p["length"], p.get("z0", 2.9)
    flare, folds, amp = p.get("flare", 1.6), p.get("folds", 3.0), p.get("amp", 0.5)
    folds2, amp2 = p.get("folds2", 7.0), p.get("amp2", 0.12)
    hem = p.get("hem", lambda u: 0.0)
    ph = p.get("phase", 0.6)

    def fn(u, v):
        edge = abs(2 * u - 1)
        x = (u - 0.5) * (w0 + (w1 - w0) * v ** 0.85)
        y = ytop + (ybot + hem(u) - ytop) * v
        fold = amp * (0.25 + v) * math.sin(2 * math.pi * folds * u + ph) * (1 - 0.35 * edge) + amp2 * v * math.sin(2 * math.pi * folds2 * u + 1.3 * ph)
        z = z0 + 0.65 + flare * v ** 1.5 + fold - 0.9 * (1 - v) ** 3 * edge ** 2 + 0.5 * edge ** 2 * v
        return (x, y, z)
    return fn


def cloak_core(gs, D, p):
    """Yoke, cord clasp, a banded swinging drape with a trim along the hem and two edge cords. Returns (fn, groups)."""
    K.yoke(gs, D, tile=p.get("yoke_tile", "silk"), slot=p.get("yoke_slot", MAIN))
    fn = cloak_fn(p)
    groups = K.cloth_bands(gs, D, fn, p.get("nu", 20), p.get("nv", 24), p.get("rows", 4), PIVOT, p.get("swing", (4.0, 14.0, 1.4)), tile=p["tile"], slot=p.get("slot", MAIN),
                           cols=p.get("cols", 2), thick=p.get("thick", -0.55), density=p.get("density", 8.0))
    g_last = groups[-1]
    K.hem_trim(gs, D, fn, p.get("hem_v", 0.93), p.get("nu", 20), g_last, tile=p.get("hem_tile", "hem"), slot=TRIM, thick=p.get("thick", -0.55) - 0.12)
    for sx, u in ((-1, 0.0), (1, 1.0)):
        K.edge_cord(gs, D, fn, u, 0.0, 1.0, 0.3, g_last, tile="gilt", n=10, name="edge%d" % sx)
    return fn, groups


def clasp_cords(gs, D, y=-1.6, z=-3.4, r=0.65, gem_slot=GLOW):
    """Two cords over the shoulders to a ringed gem on the chest."""
    for sx in (-1, 1):
        c = Part("cord%d" % sx)
        pts = L.bez3((sx * 3.9, 0.5, -0.2), (sx * 4.8, -0.2, -2.4), (sx * 2.8, -1.1, -3.4), (sx * 0.9, y, z), D.dim(6, 3))
        sweep(c, pts, circle(0.3, D.sides(4, 3)), scales=[(1.0, 1.0)] * len(pts), caps=(True, True))
        gs.add(c, TRIM, "rope")
    s = Part("clasp_ring")
    L.torus(s, (0, y, z - 0.6), r * 1.25, 0.24, D.dim(10, 6), 4, plane="xy")
    gs.add(s, TRIM, "gilt")
    K.gem(gs, D, (0, y, z - 0.6), r * 0.8, 0.8, 6, "z", slot=GLOW)


def crest_disc(gs, D, c, R, tile="crest", flags=LIT, normal=(0, 0, 1), group=0, slot=TRIM, name="crest", thick=0.45, n=14):
    """A round plate that carries the relic's own crest (decal cell) with its glow-map lines."""
    p = Part(name)
    d, u, v = K.basis(normal, (0, 1, 0))
    K.disc(p, c, u, v, R, R, D.dim(n, 8), thick)
    gs.add(p, slot, tile, flags, group, "fit")
    ring = Part(name + "_rim")
    K.link(ring, c, u, v, R * 1.02, R * 1.02, 0.22, D.dim(n, 8), 4)
    gs.add(ring, TRIM, "gilt")


def hanging_tablets(gs, D, centre, R, n, size, tile="rune_a", speed=1.6, y_span=6.0, slot=GLOW, name="tablets", groups=2):
    """Floating rune tablets circling the wearer: flat plates (EMIT | LIT, sampling a rune decal) on ORBIT groups, bobbing in height."""
    cx, cy, cz = centre
    gids = []
    n = D.cnt(n, 2)
    for gi in range(groups):
        sp = speed * (1 if gi % 2 == 0 else -0.7)
        g = gs.group("%s%d" % (name, gi), "orbit", (cx, cy, cz), sp, 0.0, 0.0)
        gids.append(g)
        part = Part("%s%d" % (name, gi))
        per = max(1, n // groups)
        for k in range(per):
            a = 2 * math.pi * k / per + gi * 1.1
            cc = A((cx + R * math.cos(a), cy + y_span * (math.sin(k * 1.9 + gi) * 0.5), cz + R * math.sin(a)))
            tang = A((-math.sin(a), 0, math.cos(a)))
            rad = A((math.cos(a), 0, math.sin(a)))
            K.disc(part, cc, tang, A((0, 1, 0)), size, size * 1.5, 4, 0.3)
        gs.add(part, slot, tile, EMIT | LIT, g, "fit")
    return gids


# ================================================================ 1. Flamme's Founder Cloak
def flammes_founder_cloak(b, D):
    gs = b.set("body", "body")
    p = dict(w0=9.4, w1=16.5, length=23.0, folds=3.0, amp=0.55, flare=1.8, tile="damask", hem=lambda u: -1.6 * abs(math.sin(math.pi * 5 * u)) + 0.9, rows=4, cols=2, nu=16, nv=20,
             swing=(4.0, 15.0, 1.5))
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        clasp_cords(gs, D)
        # the high founder's collar: a standing collar behind the head with star points
        K.stand_collar(gs, D, 0.2, 6.5, 5.0, 3.4, 2.8, tile="gilt", slot=TRIM)
        pts = Part("collar_stars")
        for k in range(D.cnt(9, 3)):
            th = math.pi * (k + 0.5) / D.cnt(9, 3)
            base = A((8.3 * math.cos(th), 6.5, 6.2 * math.sin(th)))
            K.spike(pts, base, base + A((1.6 * math.cos(th), 3.4, 1.2 * math.sin(th))), 0.42, 4)
        gs.add(pts, GLOW, "spark", EMIT | HOT, 0, "fit")
        # the founder's crest on the back
        crest_disc(gs, D, (0, -6.5, 4.6), 3.4, "crest", normal=(0, 0, 1), group=gat(groups, 1))
        for sx in (-1, 1):                                   # shoulder cowls: three tiers at full detail
            K.layered_cowl(gs, D, sx, [(0.0, 4.2, 1.0)], tile="plate", slot=MAIN)
    with gs.lvl(3):
        for sx in (-1, 1):
            K.filigree(gs, D, (sx * 6.2, 1.6, 0.4), (sx * 0.3, 1, 0.1), 3.6, turns=2.0, r=0.2, segs=14, arms=2, name="epaulette%d" % sx)
            K.gem(gs, D, (sx * 6.2, 2.0, 0.4), 0.9, 1.2, 6, "y", name="epgem%d" % sx)
            K.layered_cowl(gs, D, sx, [(-2.4, 3.6, 0.9), (-4.6, 3.0, 0.8)], tile="plate", slot=ACCENT, name="cowlb")
        for sx in (-1, 1):                                   # side embroidery: a column of panel decals down each side of the cloak
            pn = Part("panel%d" % sx)
            L.sheet(pn, lambda u, v, sx=sx: (sx * (3.2 + 2.2 * v) + (u - 0.5) * 3.0, -4.0 - 14.0 * v, fn(0.5, v)[2] + 0.55), 2, 8, -0.1)
            gs.add(pn, TRIM, "panel", LIT, gat(groups, 2), "fit")
    with gs.lvl(2):
        # mana: rune tablets orbiting the wearer, a halo of starlight
        hanging_tablets(gs, D, (0, -6.0, 0.0), 9.0, 10, 0.9, "rune_a")
        K.shards(gs, D, (0.0, -9.0, 1.0), 7.5, 8, 0.45, kind="orbit", speed=-2.2, tile="spark", groups=2)
        K.ring_halo(gs, D, (0, 4.8, 3.2), 5.2, 0.18, plane="xz", n=28)
        pulse = gs.group("founder_pulse", "pulse", (0, -6.5, 4.6), 0.07, 0.09, 0.0)
        pc = Part("crest_star")
        K.dia(pc, (0, -6.5, 5.3), 1.2, 1.2, 0.4)
        gs.add(pc, GLOW, "spark", EMIT | HOT, pulse, "fit")


BUILDERS = {"flammes_founder_cloak": flammes_founder_cloak}

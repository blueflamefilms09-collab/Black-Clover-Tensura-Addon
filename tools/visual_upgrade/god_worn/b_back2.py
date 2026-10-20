"""BACK relics 2..8: the Overlord mantle, the Pactbreaker shroud, the Shadow Monarch's mantle, the slime cloak, the winter mantle, the dragon scale, the chain mantle."""
import _paths  # noqa: F401
import math

import numpy as np

import gen_worn_lib as L
import mesh_lib as ml
from mesh_lib import Part, loft, lathe
from gw_lib import MAIN, TRIM, GLOW, DARK, ACCENT, EMIT, SHELL, HOT, LIT
import kit as K
from kit import A
from b_back import cloak_fn, cloak_core, clasp_cords, crest_disc, hanging_tablets, gat, PIVOT


# ---------------------------------------------------------------- shared pieces of the mantles and cloaks
def hang_chain(gs, D, top, length, r=0.24, a=0.8, b=0.55, swing=(7.0, 1.2), tile="chain", slot=TRIM, name="hchain", sway_phase=0.0):
    """A chain hanging straight down from `top`, hinged at its top (its own swing group): links alternate turning."""
    g = gs.group(name, "swing", top, swing[0], swing[1], sway_phase)
    pts = [(top[0], top[1] - length * k / 4.0, top[2]) for k in range(5)]
    K.chain_run(gs, D, pts, a=a, b=b, r=r, tile=tile, slot=slot, group=g, name=name, plane_hint=(1, 0, 0), spacing=a * 1.5)
    return g


def pauldron_plate(gs, D, sx, c=(6.4, 1.2, 0.2), rx=3.6, ry=2.2, rz=3.0, rim_tile="gilt", slot=MAIN, tile="pauldron", lames=3, drop=1.9, name="paul"):
    """A shoulder guard: a domed cap with `lames` overlapping lames beneath it, a trim rim on every plate."""
    x0, y0, z0 = c
    lames = lames if D.full else (1 if D.held else 0)
    cap = Part("%scap%d" % (name, sx))
    lathe(cap, "y", (sx * x0, y0 - ry * 0.2, z0), [(-ry * 0.2, rx * 0.85), (0.0, rx), (ry * 0.55, rx * 0.8), (ry, rx * 0.35), (ry * 1.1, 0.0)], sides=D.sides(14, 6), squash=rz / rx)
    gs.add(cap, slot, tile, SHELL, 0, "tile")
    for k in range(lames):
        pl = Part("%slame%d_%d" % (name, sx, k))
        yy = y0 - 0.8 - k * drop
        sc = 1.0 - 0.1 * (k + 1)
        lathe(pl, "y", (sx * x0, yy, z0), [(-0.5, rx * sc * 0.8), (0.0, rx * sc), (0.4, rx * sc * 0.92)], sides=D.sides(14, 6), squash=rz / rx)
        gs.add(pl, slot if k % 2 == 0 else ACCENT, tile, SHELL, 0, "tile")
        rim = Part("%srim%d_%d" % (name, sx, k))
        L.torus(rim, (sx * x0, yy - 0.45, z0), rx * sc * 0.97, 0.2, D.dim(14, 8), 4, 1.0, rz / rx)
        gs.add(rim, TRIM, rim_tile)


def veins_overlay(gs, D, fn, group, flags=LIT, tile="vein", nu=8, nv=10, dz=0.1, slot=MAIN, u0=0.06, u1=0.94, v0=0.04, v1=0.96, thick=-0.05):
    """A thin skin hugging a drape that carries the glowing veins (glow map) of the relic."""
    part = Part("veins")
    L.sheet(part, lambda u, v: tuple(np.array(fn(u0 + (u1 - u0) * u, v0 + (v1 - v0) * v)) + np.array((0.0, 0.0, dz))), D.dim(nu), D.dim(nv), thick)
    gs.add(part, slot, tile, flags, group, "tile", 5.0)


def shoulder_blades(gs, D, sx, n=4, base=(5.2, 1.0, 0.4), length=7.0, sweep=2.5, w=0.8, tile="metal", slot=TRIM, flags=0, name="blade", curve=1.4, group=0):
    """Curved blades fanned up and out from a shoulder."""
    part = Part("%s%d" % (name, sx))
    nn = n if D.full else D.cnt(n, 2)
    for k in range(nn):
        t = k / max(1, nn - 1)
        ang = math.radians(10 + 55 * t)
        ln = length * (1.0 - 0.28 * t)
        p0 = A((sx * base[0], base[1], base[2]))
        d = A((sx * math.sin(ang), math.cos(ang), -0.15 + 0.5 * t))
        pts = K.bezier(p0, p0 + d * ln * 0.4, p0 + d * ln * 0.75 + A((sx * curve, 0, 0.5)), p0 + d * ln + A((sx * sweep, 0.6, 0.9)), D.dim(6, 3))
        K.tube(part, pts, w * (1 - 0.1 * t), D.sides(4, 3), taper=lambda q: 1.0 - 0.96 * q ** 0.9, caps=(True, False))
    gs.add(part, slot, tile, flags, group, "tile")


# ================================================================ 2. Mantle of the Overlord
def mantle_of_the_overlord(b, D):
    gs = b.set("body", "body")
    p = dict(w0=10.0, w1=21.0, length=26.0, folds=5.0, amp=0.9, folds2=11.0, amp2=0.28, flare=2.4, tile="velvet", hem=lambda u: -4.5 * (1 - abs(2 * u - 1)) ** 1.5 + 0.8 * math.sin(u * 40),
             rows=5, cols=2, nu=18, nv=24, swing=(3.0, 15.0, 1.6), yoke_tile="velvet", hem_tile="trim")
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        for k, (ln, w0, w1, z) in enumerate(((9.0, 11.0, 16.0, 3.5), (15.0, 11.5, 18.5, 3.9))):      # two shorter capelets over the drape: more folds than cloth
            q = dict(p, w0=w0, w1=w1, length=ln, z0=z, folds=4.0 + k, amp=0.6, rows=2 + k, cols=2, nu=14, nv=12, tile="damask", slot=TRIM if k == 0 else MAIN,
                     hem=lambda u, k=k: -1.0 * abs(math.sin(math.pi * (6 + 2 * k) * u)))
            cap_fn = cloak_fn(q)
            gg = K.cloth_bands(gs, D, cap_fn, q["nu"], q["nv"], q["rows"], PIVOT, (3.0, 8.0 + 3 * k, 1.2), tile=q["tile"], slot=q["slot"], cols=2, name="capelet%d_" % k)
            K.hem_trim(gs, D, cap_fn, 0.9, q["nu"], gg[-1], tile="trim", slot=TRIM if k else ACCENT, thick=-0.62, name="caphem%d" % k)
        K.stand_collar(gs, D, 0.4, 8.5, 5.0, 3.5, 4.2, tile="velvet", slot=MAIN, name="fan_collar")
        spk = Part("collar_spikes")
        cnt = D.cnt(11, 5)
        for k in range(cnt):
            th = math.pi * (k + 0.5) / cnt
            base = A((9.4 * math.cos(th), 8.5, 7.2 * math.sin(th)))
            K.spike(spk, base, base + A((2.4 * math.cos(th), 5.0 + 1.6 * math.sin(th * 3), 1.8 * math.sin(th))), 0.6, 4)
        gs.add(spk, TRIM, "plate")
        clasp_cords(gs, D, y=-1.8, z=-3.5, r=0.9)
        crest_disc(gs, D, (0, -1.8, -4.3), 1.7, "crest", normal=(0, 0, -1), name="skullclasp")
        crest_disc(gs, D, (0, -8.0, 4.7), 3.6, "crest", normal=(0, 0, 1), group=gat(groups, 1), name="backskull")
    with gs.lvl(3):
        for sx in (-1, 1):
            pauldron_plate(gs, D, sx, tile="pauldron")
            shoulder_blades(gs, D, sx, 4, tile="metal", slot=TRIM)
        veins_overlay(gs, D, fn, gat(groups, 2), nu=10, nv=12)
        for sx in (-1, 1):
            K.wisp(gs, D, (sx * 6.5, 3.0, 1.0), (0, 1, 0), 7.0, 0.55, 3, 0, GLOW, "shadow", EMIT | LIT, "wisp%d" % sx, seed=20 + sx)
    with gs.lvl(2):
        hanging_tablets(gs, D, (0, -7.0, 0.0), 9.5, 8, 1.0, "rune_b", speed=1.2)
        K.ring_halo(gs, D, (0, 7.0, 5.2), 6.0, 0.2, plane="xz", n=24)


# ================================================================ 3. Pactbreaker Shroud
def pactbreaker_shroud(b, D):
    gs = b.set("body", "body")
    saw = lambda u: -(2.2 + 3.6 * abs(math.sin(7.0 * u + 0.4)) ** 1.5) * (0.5 + 0.5 * abs(math.sin(19.0 * u)))
    p = dict(w0=9.6, w1=18.0, length=25.0, folds=4.0, amp=0.75, folds2=9.0, amp2=0.22, flare=2.0, tile="shadow", hem=saw, rows=5, cols=3, nu=21, nv=24, swing=(3.0, 16.0, 1.6), yoke_tile="shadow",
             hem_tile="trim")
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        hd = Part("hood")                                               # the hood bunched at the nape
        lathe(hd, "y", (0, 2.0, 4.0), [(-2.8, 1.5), (-1.0, 3.6), (1.0, 3.9), (3.0, 2.4), (4.6, 0.0)], sides=D.sides(14, 6), squash=0.9)
        gs.add(hd, MAIN, "shadow", SHELL)
        nt = D.cnt(9, 3)
        for k in range(nt):                                             # torn shroud tails that flutter
            u = (k + 0.5) / nt
            g = gs.group("tail%d" % k, "wave", (0, -9.0, 3.4), 1.8 + 0.2 * (k % 3), 0.07 + 0.01 * (k % 4), 0.35)
            x = (u - 0.5) * 14.0
            pts = [(x + 0.4 * math.sin(i * 1.3 + k), -11.0 - i * 3.2, 4.4 + 0.5 * math.sin(i + k) - 0.1 * i) for i in range(D.dim(6, 3))]
            K.ribbon(gs, D, pts, 1.8 - 0.4 * (k % 3), MAIN, "shadow", SHELL, g, "tail%d" % k)
        clasp_cords(gs, D, y=-1.4, z=-3.4, r=0.7, gem_slot=GLOW)
        crest_disc(gs, D, (0, -8.0, 4.7), 3.6, "crest", normal=(0, 0, 1), group=gat(groups, 1), name="brokenseal")
    with gs.lvl(3):
        veins_overlay(gs, D, fn, gat(groups, 2), nu=12, nv=14, tile="vein")
        for sx in (-1, 1):                                              # snapped chains hanging from the shoulders
            hang_chain(gs, D, (sx * 6.6, 1.2, 1.2), 11.0, name="snap%d" % sx, swing=(6.0, 1.4), sway_phase=sx)
        K.shards(gs, D, (0, -8.0, 0.0), 8.5, 9, 0.5, kind="orbit", speed=1.6, tile="rune_b", groups=3, flags=EMIT | LIT)
    with gs.lvl(2):
        hanging_tablets(gs, D, (0, -4.0, 0.0), 10.5, 8, 1.3, "rune_a", speed=-1.4)
        K.ring_halo(gs, D, (0, 8.0, 3.6), 6.0, 0.2, plane="xz", n=24)


# ================================================================ 4. Shadow Monarch's Mantle
def shadow_monarchs_mantle(b, D):
    gs = b.set("body", "body")
    jag = lambda u: -(1.5 + 7.0 * (abs(((u * 9.0) % 1.0) - 0.5) * 2.0) ** 1.3) + 2.0 * math.sin(u * 7)
    p = dict(w0=9.6, w1=19.0, length=26.0, folds=4.0, amp=0.8, folds2=10.0, amp2=0.25, flare=2.2, tile="shadow", hem=jag, rows=5, cols=3, nu=21, nv=24, swing=(3.0, 17.0, 1.6), yoke_tile="shadow",
             hem_tile="trim")
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        K.stand_collar(gs, D, 0.4, 7.5, 5.0, 3.6, 3.2, tile="shadow", slot=MAIN, name="col")
        for sx in (-1, 1):
            shoulder_blades(gs, D, sx, 5, length=9.0, sweep=3.2, w=1.0, tile="crystal", slot=TRIM)
            pauldron_plate(gs, D, sx, tile="pauldron", lames=2)
        crest_disc(gs, D, (0, -8.0, 4.7), 3.6, "crest", normal=(0, 0, 1), group=gat(groups, 1), name="monarch")
        for k, (x, y) in enumerate(((-5.0, 5.2), (5.0, 5.2), (0.0, 9.5), (-2.8, 8.0), (2.8, 8.0))):      # the crown of shadow-flames standing up behind the head
            K.wisp(gs, D, (x, y, 5.2), (0.0, 1.0, 0.0), 6.0 + 1.5 * (k % 2), 0.8, 3, 0, GLOW, "shadow", EMIT | LIT, "flame%d" % k, seed=40 + k)
    with gs.lvl(3):
        veins_overlay(gs, D, fn, gat(groups, 2), nu=12, nv=14, tile="vein")
        for sx in (-1, 1):
            K.wisp(gs, D, (sx * 6.0, 2.5, 1.0), (sx * 0.3, 1.0, 0.0), 8.0, 0.7, 4, 0, GLOW, "shadow", EMIT | LIT, "sw%d" % sx, seed=60 + sx)
        K.shards(gs, D, (0.0, -6.0, 0.0), 9.0, 10, 0.55, kind="orbit", speed=1.9, tile="crystal", groups=3, slot=DARK, flags=EMIT)
    with gs.lvl(2):
        hanging_tablets(gs, D, (0, -10.0, 0.0), 11.0, 8, 1.2, "rune_a")
        K.ring_halo(gs, D, (0, -22.0, 3.8), 11.5, 0.25, plane="xz", n=28)


# ================================================================ 5. Shadow's Slime Cloak
def shadows_slime_cloak(b, D):
    gs = b.set("body", "body")
    blob = lambda u: -(1.0 + 3.8 * abs(math.sin(math.pi * 6 * u)) ** 0.6)
    p = dict(w0=9.6, w1=17.0, length=22.0, folds=3.5, amp=0.7, folds2=6.0, amp2=0.18, flare=1.6, tile="slime", hem=blob, rows=4, cols=3, nu=21, nv=24, swing=(5.0, 16.0, 2.2), yoke_tile="slime",
             hem_tile="trim", thick=-0.9)
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        nd = D.cnt(8, 3)
        for k in range(nd):                                             # drips: beads on thin strings that bob
            u = (k + 0.5) / nd
            top = fn(u, 0.97)
            g = gs.group("drip%d" % k, "bob", (top[0], top[1], top[2]), 0.9 + 0.1 * (k % 3), 0.07 + 0.01 * k, k * 0.8)
            bd = Part("drip%d" % k)
            L.sph(bd, (top[0], top[1] - 2.2 - (k % 3), top[2] - 0.3), 0.75 + 0.2 * (k % 2), 4, 8)
            gs.add(bd, ACCENT, "slime", SHELL | EMIT, g, "tile")
        crest_disc(gs, D, (0, -7.0, 4.8), 3.2, "crest", normal=(0, 0, 1), group=gat(groups, 1), name="slimeface")
        nuc = gs.group("nucleus", "pulse", (0, -7.0, 4.2), 0.12, 0.1, 0.0)
        core = Part("nucleus")
        L.sph(core, (0, -7.0, 4.2), 1.8, 4, 10)
        gs.add(core, GLOW, "soft", EMIT | HOT, nuc, "fit")
        for sx in (-1, 1):                                              # tendrils over the shoulders to the chest, each ending in a blob
            part = Part("tendril%d" % sx)
            pts = K.bezier(A((sx * 5.0, 1.0, 3.0)), A((sx * 7.6, 0.6, 0.0)), A((sx * 5.0, -1.0, -3.8)), A((sx * 1.6, -4.5, -3.6)), D.dim(10, 4))
            K.tube(part, pts, 0.9, D.sides(6, 4), taper=lambda t: 1.0 - 0.55 * t)
            gs.add(part, MAIN, "slime", SHELL)
            tipb = Part("tip%d" % sx)
            L.sph(tipb, pts[-1], 1.2, 4, 8)
            gs.add(tipb, ACCENT, "slime", SHELL | EMIT)
    with gs.lvl(3):
        sw = Part("slime_sword")
        K.beam(sw, A((-6.0, -11.0, -2.8)), A((-8.0, -24.0, -3.4)), 1.7, 0.7)
        gs.add(sw, TRIM, "crystal", SHELL | LIT)
        edge = gs.group("blade_pulse", "pulse", (-7.0, -17.0, -3.0), 0.05, 0.08, 0.0)
        eg = Part("blade_edge")
        K.beam(eg, A((-6.0, -11.0, -3.3)), A((-8.0, -24.0, -3.9)), 0.4, 0.3)
        gs.add(eg, GLOW, "soft", EMIT | HOT, edge, "fit")
        for sx in (-1, 1):
            pauldron_plate(gs, D, sx, tile="slime", lames=2, name="blob")
        veins_overlay(gs, D, fn, gat(groups, 2), nu=10, nv=12, tile="vein")
        K.shards(gs, D, (0.0, -7.0, 1.0), 9.0, 8, 0.7, kind="bob", speed=0.0, tile="pearl", groups=1)
    with gs.lvl(2):
        hanging_tablets(gs, D, (0, -8.0, 0.0), 10.0, 6, 0.9, "rune_b", speed=1.0)


# ================================================================ 6. Silent Winter Mantle
def silent_winter_mantle(b, D):
    gs = b.set("body", "body")
    frill = lambda u: -(0.8 + 1.8 * abs(math.sin(math.pi * 9 * u)) ** 2)
    p = dict(w0=10.4, w1=15.0, length=15.0, folds=4.0, amp=0.55, folds2=8.0, amp2=0.14, flare=1.2, tile="frost", hem=frill, rows=3, cols=3, nu=21, nv=18, swing=(3.0, 11.0, 1.2), yoke_tile="fur",
             hem_tile="trim", yoke_slot=TRIM)
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        col = Part("fur_ruff")                                          # the frost-fur ruff round the neck
        L.band(col, -1.0, 2.2, 5.2, 3.3, 1.2, D.dim(24, 8), 0.3, 0.8, 3.0)
        gs.add(col, TRIM, "fur", SHELL)
        for sx in (-1, 1):
            pauldron_plate(gs, D, sx, tile="frost", lames=3, rim_tile="gilt", slot=MAIN, name="ice")
        crown = Part("ice_crown")
        cnt = D.cnt(15, 6)
        rr = K.rg(7)
        for k in range(cnt):
            th = 2 * math.pi * k / cnt
            ln = 4.0 + 3.2 * (k % 3 == 0) + 1.5 * rr.random()
            base = A((5.6 * math.cos(th), 1.2, 4.1 * math.sin(th)))
            tilt = A((0.55 * math.cos(th), 0.0, 0.5 * math.sin(th)))
            K.crystal(crown, base, A((0, 1, 0)) + tilt, ln, 0.5 + 0.2 * (k % 2), 6)
        gs.add(crown, TRIM, "crystal", SHELL | LIT, 0, "tile")
        crest_disc(gs, D, (0, -4.6, 4.5), 3.2, "crest", normal=(0, 0, 1), group=gat(groups, 1), name="flake")
    with gs.lvl(3):
        for sx in (-1, 1):                                              # icicles hanging from the shoulder cowls
            ic = Part("icicles%d" % sx)
            ni = D.cnt(10, 4)
            for k in range(ni):
                a = math.pi * 2 * k / ni
                x0 = sx * 6.4 + 3.2 * math.cos(a)
                z0 = 0.2 + 2.6 * math.sin(a)
                K.crystal(ic, A((x0, -2.2, z0)), A((0, -1, 0)), 2.5 + 2.0 * ((k * 5) % 3) / 2.0, 0.42, 5, tip=0.7)
            gs.add(ic, TRIM, "crystal", SHELL | LIT)
        veins_overlay(gs, D, fn, gat(groups, 2), nu=10, nv=8, tile="frost")
        K.shards(gs, D, (0.0, -3.0, 1.0), 9.5, 10, 0.55, kind="orbit", speed=0.9, tile="spark", groups=2)
        for sx in (-1, 1):
            K.filigree(gs, D, (sx * 6.0, 2.4, 0.4), (sx * 0.3, 1, 0.1), 3.0, turns=1.6, r=0.17, segs=14, arms=3, tile="metal", name="lace%d" % sx)
    with gs.lvl(2):
        hanging_tablets(gs, D, (0, -6.0, 0.0), 10.0, 8, 0.9, "rune_a", speed=0.8)
        K.ring_halo(gs, D, (0, 4.0, 4.0), 5.8, 0.2, plane="xz", n=24)


# ================================================================ 7. Six-Faced Dragon's Scale
def dragon_head(gs, D, c, d, size, name, slot=MAIN, tile="scale"):
    """A small horned dragon head (snout wedge, two horns, glowing eyes) looking along d from c."""
    c, d = A(c, float), K.unit(d)
    dd, u, v = K.basis(d, (0, 1, 0))
    p = Part(name)
    st = [(0.0, 0.55, 0.45), (size * 0.5, 0.95, 0.75), (size * 1.0, 0.75, 0.55), (size * 1.35, 0.35, 0.28), (size * 1.5, 0.0, 0.0)]
    loft(p, dd, c, st, ml.ellipse(size * 0.5, size * 0.4, D.sides(8, 5)), caps=(True, True), up=(0, 1, 0))
    gs.add(p, slot, tile, SHELL)
    for sgn in (-1, 1):
        h = Part("%sh%d" % (name, sgn))
        pts = K.bezier(c + u * sgn * size * 0.3 + v * size * 0.3 - dd * size * 0.1, c + u * sgn * size * 0.7 + v * size * 0.7 - dd * size * 0.2,
                       c + u * sgn * size * 0.9 + v * size * 1.1 - dd * size * 0.7, c + u * sgn * size * 0.7 + v * size * 1.5 - dd * size * 1.1, D.dim(5, 3))
        K.tube(h, pts, size * 0.14, D.sides(4, 3), taper=lambda t: 1.0 - 0.95 * t, caps=(True, False))
        gs.add(h, TRIM, "horn")
        e = Part("%se%d" % (name, sgn))
        K.dia(e, c + u * sgn * size * 0.28 + v * size * 0.18 + dd * size * 0.5, size * 0.1, size * 0.05, size * 0.1)
        gs.add(e, GLOW, "spark", EMIT | HOT, 0, "fit")
    return p


def six_faced_dragons_scale(b, D):
    gs = b.set("body", "body")
    p = dict(w0=10.0, w1=14.0, length=17.0, folds=3.0, amp=0.5, flare=1.4, tile="scale", hem=lambda u: -2.0 * abs(math.sin(math.pi * 7 * u)), rows=3, cols=3, nu=15, nv=16, swing=(3.0, 11.0, 1.2),
             yoke_tile="scale", yoke_slot=TRIM, hem_tile="trim", density=6.0)
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        sf = cloak_fn(dict(p, z0=3.2))
        K.scale_field(gs, D, sf, 9, 10, 2.8, 3.0, slot=ACCENT, tile="scale", group=gat(groups, 0), name="back_scales", thick=0.7)
        for sx in (-1, 1):                                              # the mantle's shoulders: domes covered in scales
            dm = lambda u, v, sx=sx: (sx * (3.5 + 4.2 * math.sin(math.pi * v * 0.9)), 2.4 - 4.5 * v ** 1.2, -2.8 + 5.6 * u)
            K.scale_field(gs, D, dm, 5, 6, 2.6, 2.6, slot=MAIN, tile="scale", name="shoulder_scales%d" % sx, thick=0.8)
            shoulder_blades(gs, D, sx, 4, length=7.0, tile="horn", slot=TRIM, base=(5.0, 1.8, 0.4), name="spine")
        # six faces: two over each shoulder and two on the back of the collar
        for k, (pos, dr) in enumerate((((-6.2, 2.8, -1.2), (-0.6, 0.2, -1)), ((6.2, 2.8, -1.2), (0.6, 0.2, -1)), ((-6.6, 2.4, 2.0), (-1, 0.2, 0.4)), ((6.6, 2.4, 2.0), (1, 0.2, 0.4)),
                                       ((-2.2, 5.6, 4.2), (-0.3, 0.3, 1)), ((2.2, 5.6, 4.2), (0.3, 0.3, 1)))):
            dragon_head(gs, D, pos, dr, 2.6, "face%d" % k, slot=ACCENT if k % 2 else MAIN)
        crest_disc(gs, D, (0, -5.0, 4.6), 3.0, "crest", normal=(0, 0, 1), group=gat(groups, 1), name="scaleemblem")
        K.stand_collar(gs, D, 0.3, 5.0, 5.0, 3.5, 1.8, tile="scale", slot=TRIM, name="ridge")
    with gs.lvl(3):
        spine = Part("ridge_spines")
        ns = D.cnt(13, 5)
        for k in range(ns):
            t = k / max(1, ns - 1)
            y = 3.0 - 20.0 * t
            K.spike(spine, A((0, y, 4.0)), A((0, y + 1.3, 6.6 + 1.2 * (1 - t))), 0.55 * (1 - 0.4 * t), 4)
        gs.add(spine, TRIM, "gilt")
        veins_overlay(gs, D, fn, gat(groups, 2), nu=10, nv=8, tile="rust", dz=0.15)
        K.shards(gs, D, (0.0, -4.0, 0.0), 9.0, 10, 0.55, kind="orbit", speed=1.4, tile="ember", groups=2)
    with gs.lvl(2):
        K.ring_halo(gs, D, (0, 3.2, 4.0), 6.4, 0.22, plane="xz", n=24)
        hanging_tablets(gs, D, (0, -6.0, 0.0), 10.0, 6, 1.0, "rune_a", speed=1.1)


# ================================================================ 8. Urek Mazino's Chain Mantle
def urek_mazinos_chain_mantle(b, D):
    gs = b.set("body", "body")
    p = dict(w0=10.0, w1=13.0, length=14.0, folds=3.0, amp=0.35, flare=0.9, tile="leather", hem=lambda u: -1.0 * abs(math.sin(math.pi * 4 * u)), rows=3, cols=3, nu=12, nv=12, swing=(2.0, 8.0, 1.0),
             yoke_tile="leather", hem_tile="trim")
    fn, groups = cloak_core(gs, D, p)
    with gs.lvl(2):
        for sx in (-1, 1):
            pauldron_plate(gs, D, sx, c=(6.6, 1.4, 0.2), rx=4.0, rz=3.4, tile="pauldron", lames=4, drop=1.7, slot=MAIN, name="iron")
        # chain swags: shoulder to shoulder across the chest and the back, and one over each shoulder
        K.chain_run(gs, D, K.catenary((-6.2, 0.5, -2.8), (6.2, 0.5, -2.8), 5.0, 14), a=1.2, b=0.8, r=0.34, tile="chain", slot=TRIM, name="swag_front", plane_hint=(0, 0, 1))
        K.chain_run(gs, D, K.catenary((-6.2, 0.5, 3.6), (6.2, 0.5, 3.6), 6.5, 14), a=1.2, b=0.8, r=0.34, tile="chain", slot=TRIM, name="swag_back", plane_hint=(0, 0, 1))
        for sx in (-1, 1):
            K.chain_run(gs, D, K.catenary((sx * 6.4, 3.6, -2.4), (sx * 6.4, 3.6, 3.0), 3.0, 8), a=1.0, b=0.7, r=0.3, tile="chain", slot=TRIM, name="swag_side%d" % sx, plane_hint=(1, 0, 0))
        crest_disc(gs, D, (0, -2.4, -4.0), 2.2, "crest", normal=(0, 0, -1), name="towerplate")
        crest_disc(gs, D, (0, -6.0, 4.4), 3.4, "crest", normal=(0, 0, 1), group=gat(groups, 1), name="towerback")
        for sx in (-1, 1):                                              # the great shackles at the ends of the swags
            part = Part("shackle%d" % sx)
            K.link(part, A((sx * 6.4, 0.6, -3.0)), A((1, 0, 0)), A((0, 1, 0)), 1.5, 1.8, 0.5, D.dim(12, 6), 4)
            gs.add(part, TRIM, "metal")
    with gs.lvl(3):
        nc = D.cnt(13, 5)
        for k in range(nc):                                             # a curtain of hanging chains down the back, each its own swing
            x = -7.5 + 15.0 * k / max(1, nc - 1)
            hang_chain(gs, D, (x, -2.0, 4.2), 15.0 + 2.0 * math.sin(k * 1.7), name="curt%d" % k, swing=(6.0 + (k % 3) * 3.0, 1.0 + 0.2 * (k % 4), k * 0.5), r=0.22, a=0.75, b=0.5)
        for sx in (-1, 1):
            hang_chain(gs, D, (sx * 6.8, -1.0, 0.2), 9.0, name="side%d" % sx, swing=(8.0, 1.5, 0.0), r=0.3, a=1.0, b=0.7)
        veins_overlay(gs, D, fn, gat(groups, 2), nu=8, nv=6, tile="rust", dz=0.12)
        K.shards(gs, D, (0.0, -3.0, 0.0), 9.5, 8, 0.55, kind="orbit", speed=1.2, tile="metal", groups=2, slot=TRIM, flags=0)
    with gs.lvl(2):
        K.ring_halo(gs, D, (0, 2.6, 4.4), 6.0, 0.22, plane="xz", n=24)
        hanging_tablets(gs, D, (0, -7.0, 0.0), 10.5, 6, 1.0, "rune_a", speed=0.9)


BUILDERS = {"mantle_of_the_overlord": mantle_of_the_overlord, "pactbreaker_shroud": pactbreaker_shroud, "shadow_monarchs_mantle": shadow_monarchs_mantle,
            "shadows_slime_cloak": shadows_slime_cloak, "silent_winter_mantle": silent_winter_mantle, "six_faced_dragons_scale": six_faced_dragons_scale,
            "urek_mazinos_chain_mantle": urek_mazinos_chain_mantle}

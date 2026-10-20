"""BELT, CHEST, FEET and NECK relics: the Avalon scabbard, the silvered sash, the Berserker shard, Orsted's armour, the wind walker greaves, the White Whistle."""
import _paths  # noqa: F401
import math

import numpy as np

import gen_worn_lib as L
import mesh_lib as ml
from mesh_lib import Part, loft, lathe, sweep, circle
from gw_lib import MAIN, TRIM, GLOW, DARK, ACCENT, EMIT, SHELL, HOT, LIT, ADD
import kit as K
from kit import A
from b_back import crest_disc, hanging_tablets, gat
from b_back2 import hang_chain, pauldron_plate, veins_overlay, shoulder_blades


def waist_band(gs, D, y0, y1, a=5.3, b=3.1, t=0.7, tile="leather", slot=MAIN, name="band", n=28):
    p = Part(name)
    L.band(p, y0, y1, a, b, t, D.dim(n, 10), 0.1, 0.15, 3.0)
    return gs.add(p, slot, tile, SHELL)


def hang_plate(gs, D, top, w, h, group, tile="plate", slot=MAIN, name="tasset", thick=0.7, rim=True):
    """A plate hanging from `top` (its top edge centre), bent a little outward at the bottom; its own swing group."""
    x, y, z = top
    p = Part(name)
    L.sheet(p, lambda u, v: (x + (u - 0.5) * w * (1 - 0.2 * v), y - h * v, z - 0.35 * v * v * h * 0.1 - 0.8 * math.sin(v * math.pi) * 0.3), 2, max(2, D.dim(3)), thick)
    gs.add(p, slot, tile, SHELL, group, "fit")
    if rim:
        r = Part(name + "r")
        K.beam(r, A((x - w * 0.4, y - h, z)), A((x + w * 0.4, y - h, z)), 0.5, 0.5)
        gs.add(r, TRIM, "gilt", 0, group)


def pendants(gs, D, n, tops, tiles, slot=TRIM, name="charm", swing=(10.0, 1.6), length=2.2, size=0.9, phase=0.6):
    """Small charms (a teardrop on a short cord) that each swing from their own hinge: `tops` is a function k, n -> the hinge position."""
    cnt = D.cnt(n, 3)
    for k in range(cnt):
        top = tops(k, cnt)
        g = gs.group("%s%d" % (name, k), "swing", tuple(top), swing[0] + 2.0 * (k % 3), swing[1] + 0.2 * (k % 4), k * phase)
        part = Part("%s%d" % (name, k))
        K.tube(part, [tuple(top - A((0, length * i / 3.0, 0))) for i in range(4)], 0.14, D.sides(4, 3))
        lathe(part, "y", tuple(top - A((0, length + size * 0.4, 0))), [(-size * 1.3, 0.0), (-size * 0.5, size * 0.55), (size * 0.2, size * 0.5), (size * 0.9, 0.0)], sides=D.sides(6, 4))
        gs.add(part, slot if k % 2 == 0 else ACCENT, tiles[k % len(tiles)], SHELL, g, "tile")


# ================================================================ BELT 1: Excalibur's Scabbard Avalon
def excaliburs_scabbard_avalon(b, D):
    gs = b.set("body", "body")
    waist_band(gs, D, -11.4, -8.6, 5.2, 3.0, 0.8, "leather", MAIN, "belt")
    rim = Part("belt_rims")
    for yy in (-8.55, -11.45):
        L.band(rim, yy - 0.12, yy + 0.12, 5.35, 3.15, 0.35, D.dim(28, 10), 0.0, 0.0, 3.0)
    gs.add(rim, TRIM, "gilt")
    with gs.lvl(2):
        for k in range(D.cnt(7, 3)):                                                    # belt plates round the front
            a = math.radians(-70 + 140 * k / max(1, D.cnt(7, 3) - 1))
            pl = Part("plate%d" % k)
            c = A((5.5 * math.sin(a), -10.0, -3.3 * math.cos(a)))
            K.disc(pl, c, A((math.cos(a), 0, math.sin(a))), A((0, 1, 0)), 0.9, 1.1, 6, 0.5)
            gs.add(pl, TRIM, "plate", 0, 0, "fit")
        crest_disc(gs, D, (0, -10.0, -3.9), 1.8, "crest", normal=(0, 0, -1), name="buckle")
        K.filigree(gs, D, (0, -10.0, -3.4), (0, 0, -1), 4.2, turns=1.4, r=0.17, segs=22, arms=2, name="buckle_vine", plane_up=(0, 1, 0))
    # the scabbard: a lofted sheath hung at the left hip, tilted outward, with gold bands, a chape and enamel panels
    top, tip = A((6.2, -10.6, -0.4)), A((8.6, -25.6, 1.4))
    axis = tip - top
    ln = float(np.linalg.norm(axis))
    secs = [(0.0, 1.0, 0.55), (ln * 0.04, 1.35, 0.7), (ln * 0.12, 1.15, 0.62), (ln * 0.5, 1.0, 0.55), (ln * 0.88, 0.62, 0.38), (ln, 0.0, 0.0)]
    sh = Part("sheath")
    loft(sh, axis, top, secs, ml.ellipse(1.6, 0.9, D.sides(12, 6)), caps=(True, True), up=(0, 0, 1))
    gs.add(sh, MAIN, "weave2", SHELL, 0, "tile", 8.0)
    d, u, v = K.basis(axis, (0, 0, 1))
    with gs.lvl(2):
        for k, t in enumerate((0.03, 0.22, 0.48, 0.72, 0.92)):                          # gold bands
            c = top + d * ln * t
            f = [1.35, 1.15, 1.02, 0.85, 0.5][k]
            band = Part("bandg%d" % k)
            K.link(band, c, u, v, 1.62 * f, 0.95 * f, 0.26, D.dim(14, 8), 4)
            gs.add(band, TRIM, "gilt")
        panel = Part("enamel_panel")
        L.sheet(panel, lambda uu, vv: tuple(top + d * ln * (0.1 + 0.62 * vv) + u * (uu - 0.5) * 2.2 * (1 - 0.4 * vv) + v * (-0.98 + 0.0)), 2, D.dim(10), 0.1)
        gs.add(panel, TRIM, "panel", LIT, 0, "fit")
        chape = Part("chape")
        K.spike(chape, top + d * ln * 0.96, tip + d * 1.6, 0.75, 6, mid=0.5, mid_r=1.0)
        gs.add(chape, TRIM, "gilt")
        K.gem(gs, D, top + d * ln * 0.03 + v * -1.0, 0.7, 1.0, 6, "z", slot=GLOW, name="mouth_gem")
    with gs.lvl(3):
        K.filigree(gs, D, tuple(top + d * ln * 0.4 - v * 1.0), tuple(-v), 6.0, turns=2.6, r=0.14, segs=36, arms=2, name="sheath_vine", plane_up=tuple(d))
        for sgn in (-1, 1):
            hang = Part("hanger%d" % sgn)
            pts = K.bezier(A((sgn * 3.0, -11.0, -3.0)), A((sgn * 4.0, -12.0, -2.6)), A((5.0 + sgn * 0.5, -11.5, -1.6)), top + d * 1.0 + u * 0.4 * sgn, D.dim(6, 3))
            K.tube(hang, pts, 0.38, D.sides(4, 3))
            gs.add(hang, TRIM, "leather")
        tass = gs.group("tassel", "swing", tuple(top + A((0.5, 0.5, -1.0))), 9.0, 1.8, 0.0)
        for k in range(3):
            tp = Part("tassel%d" % k)
            K.tube(tp, [tuple(top + A((0.5, 0.4, -1.4 + 0.2 * k)) - A((0, 1.0 * i, 0))) for i in range(5)], 0.26, 4, taper=lambda t: 1.0 - 0.5 * t)
            gs.add(tp, TRIM, "silk", 0, tass)
        pendants(gs, D, 12, lambda k, n: A((5.8 * math.sin(math.radians(-100 + 200 * k / max(1, n - 1))), -11.5, -3.3 * math.cos(math.radians(-100 + 200 * k / max(1, n - 1))))),
                 ["feathery", "marble", "scale", "crystal", "pearl", "bone", "silk", "lace", "damask", "weave2", "horn", "glass"], name="leafcharm")
        for k in range(D.cnt(8, 3)):                                                    # fairy lights: each a mote of its own bobbing at its own pace
            cc = top + d * ln * (0.12 + 0.1 * k) + u * (1.2 + 0.4 * math.sin(k * 2.1)) + v * (-1.4 - 0.5 * (k % 3))
            gb = gs.group("fairy%d" % k, "bob", tuple(cc), 0.9 + 0.12 * (k % 4), 0.07 + 0.012 * k, k * 0.9)
            fp = Part("fairy%d" % k)
            K.dia(fp, tuple(cc), 0.32, 0.5, 0.32)
            gs.add(fp, GLOW, "spark", EMIT | HOT | ADD, gb, "fit")
        # Avalon: light that rings the scabbard
        cx = tuple(top + d * ln * 0.45)
        K.shards(gs, D, cx, 3.8, 10, 0.35, y_amp=7.0, kind="orbit", speed=2.4, tile="spark", groups=2, name="fairy")
        for k, t in enumerate((0.25, 0.5, 0.75)):
            ring = gs.group("avalon_ring%d" % k, "pulse", tuple(top + d * ln * t), 0.08, 0.07 + 0.015 * k, k * 1.3)
            rp = Part("avalon_ring%d" % k)
            K.link(rp, top + d * ln * t, u, v, 2.6, 1.9, 0.14, D.dim(16, 8), 4)
            gs.add(rp, GLOW, "ring_glow", EMIT | HOT | ADD, ring, "fit")
    with gs.lvl(2):
        pulse = gs.group("avalon_core", "pulse", tuple(top + d * ln * 0.5), 0.1, 0.06, 0.0)
        lg = Part("glyph_line")
        K.beam(lg, top + d * ln * 0.14 - v * 1.05, top + d * ln * 0.86 - v * 1.05, 0.5, 0.2)
        gs.add(lg, GLOW, "rune_band", EMIT | LIT | HOT, pulse, "fit")


# ================================================================ BELT 2: Silvered Sash of Reflections
def hex_plate(gs, D, c, d, up, r, thick=0.5, group=0, tile="glass", slot=MAIN, name="mirror", flags=SHELL | LIT):
    """A hexagonal mirror plate facing d."""
    dd, u, v = K.basis(d, up)
    p = Part(name)
    K.disc(p, c, u, v, r, r, 6, thick)
    gs.add(p, slot, tile, flags, group, "fit")


def silvered_sash_of_reflections(b, D):
    gs = b.set("body", "body")
    waist_band(gs, D, -11.3, -8.7, 5.2, 3.0, 0.7, "metal", MAIN, "waist")
    # the bandolier: a row of mirror plates from the right shoulder over the chest to the left hip, strung on a silver chain, and the same on the back
    for side, z, face in (("front", -3.4, (0, 0, -1)), ("back", 3.4, (0, 0, 1))):
        path = K.bezier((-4.0 if side == "front" else 4.0, 0.2, z), (-3.0 if side == "front" else 3.0, -3.0, z * 1.12), (3.0 if side == "front" else -3.0, -7.0, z * 1.12), (5.0 if side == "front" else -5.0, -10.4, z), 24)
        n = D.cnt(18, 5)
        K.chain_run(gs, D, path, a=0.8, b=0.55, r=0.17, tile="chain", slot=TRIM, name="sashchain_" + side, plane_hint=(1, 0, 0), spacing=1.1)
        for k in range(n):
            t = (k + 0.5) / n
            i = min(len(path) - 1, int(t * (len(path) - 1)))
            hex_plate(gs, D, path[i], face, (0, 1, 0), 1.35 - 0.25 * abs(t - 0.5), group=0, name="mirror_%s%d" % (side, k))
    with gs.lvl(2):
        for k in range(D.cnt(10, 4)):                                                  # mirror plates round the waist
            a = 2 * math.pi * k / D.cnt(10, 4)
            c = A((5.8 * math.cos(a), -10.0, 3.6 * math.sin(a)))
            hex_plate(gs, D, c, (math.cos(a), 0, math.sin(a)), (0, 1, 0), 1.2, name="waist_mirror%d" % k)
        crest_disc(gs, D, (4.8, -10.0, -3.2), 1.7, "crest", normal=(0.4, 0, -1), name="knot")
        # the tails: strands of glass links ending in a shard, swaying at the left hip
        nt = D.cnt(9, 3)
        for k in range(nt):
            top = A((3.6 + 0.8 * k * 0.5, -11.0, -3.4 + 0.9 * (k % 3)))
            g = gs.group("tail%d" % k, "swing", tuple(top), 8.0 + 2.0 * (k % 3), 1.4 + 0.2 * (k % 4), k * 0.7)
            ln = 4.0 + 1.6 * ((k * 5) % 4)
            K.chain_run(gs, D, [tuple(top), tuple(top - A((0, ln, 0)))], a=0.55, b=0.4, r=0.13, tile="chain", slot=TRIM, group=g, name="tailchain%d" % k, plane_hint=(1, 0, 0), spacing=0.85)
            sh = Part("tailshard%d" % k)
            K.crystal(sh, top - A((0, ln, 0)), (0, -1, 0), 2.4, 0.5, 4, tip=0.6)
            gs.add(sh, MAIN, "glass", SHELL | LIT, g, "fit")
    with gs.lvl(3):
        K.shards(gs, D, (0.0, -6.0, 0.0), 8.0, 12, 0.5, y_amp=4.0, kind="orbit", speed=1.7, tile="glass", slot=MAIN, groups=3, flags=SHELL | LIT, name="reflect")
        for k in range(D.cnt(6, 2)):
            ring = gs.group("glint%d" % k, "pulse", (0, -6.0, 0), 0.12, 0.07 + 0.01 * k, k * 1.0)
            rp = Part("glint%d" % k)
            K.dia(rp, A((6.5 * math.cos(k * 1.05), -2.0 - k * 1.8, 5.0 * math.sin(k * 1.05))), 0.3, 1.3, 0.3)
            gs.add(rp, GLOW, "spark", EMIT | HOT | ADD, ring, "fit")
        K.filigree(gs, D, (0, -10.0, -3.5), (0, 0, -1), 3.6, turns=1.6, r=0.14, segs=24, arms=3, tile="metal", name="engr")
        for k in range(D.cnt(6, 3)):                                                    # glints that wander over the glass, each bobbing at its own pace
            cc = A((4.6 * math.cos(k * 1.9), -2.0 - 1.4 * k, -3.9 if k % 2 == 0 else 3.9))
            gb = gs.group("glint_b%d" % k, "bob", tuple(cc), 0.7 + 0.1 * k, 0.08 + 0.01 * k, k * 1.1)
            gp = Part("glint_b%d" % k)
            K.dia(gp, tuple(cc), 0.3, 0.55, 0.3)
            gs.add(gp, GLOW, "spark", EMIT | HOT | ADD, gb, "fit")
        pendants(gs, D, 10, lambda k, n: A((5.9 * math.cos(2 * math.pi * k / n), -11.5, 3.5 * math.sin(2 * math.pi * k / n))), ["glass", "crystal", "marble", "pearl", "lace", "stars", "hammered", "weave2"], slot=MAIN,
                 name="mirrorcharm", swing=(8.0, 1.4), size=1.0)


# ================================================================ CHEST helpers
def cuirass(gs, D, y0=-10.6, y1=-0.8, a=5.15, b=3.15, tile="plate", slot=MAIN, name="cuirass", rows=1):
    """The front and back plates of a cuirass: a flared band round the torso, with a raised belly line."""
    p = Part(name)
    L.band(p, y0, y1, a, b, 0.7, D.dim(28, 10), 0.0, 0.0, 3.2)
    gs.add(p, slot, tile, SHELL, 0, "tile", 6.0)
    for k in range(D.cnt(4, 2) if rows else 0):
        ridge = Part("%sridge%d" % (name, k))
        yy = y1 - 1.5 - k * 2.2
        L.band(ridge, yy - 0.22, yy + 0.22, a + 0.35, b + 0.35, 0.3, D.dim(28, 10), 0.0, 0.0, 3.2)
        gs.add(ridge, TRIM, "gilt")


def collar_guard(gs, D, spikes=9, h=3.4, ring=5.2, tile="plate", slot=MAIN, spike_tile="horn", name="gorget"):
    p = Part(name)
    L.band(p, -1.4, 1.6, 4.8, 3.2, 0.8, D.dim(24, 8), 0.2, 0.6, 3.0)
    gs.add(p, slot, tile, SHELL)
    sp = Part(name + "_spikes")
    n = D.cnt(spikes, 4)
    for k in range(n):
        th = 2 * math.pi * k / n
        base = A((5.3 * math.cos(th), 1.4, 3.7 * math.sin(th)))
        K.spike(sp, base, base + A((1.4 * math.cos(th), h * (0.7 + 0.5 * (k % 2)), 1.1 * math.sin(th))), 0.5, 4)
    gs.add(sp, TRIM, spike_tile)


# ================================================================ CHEST 1: Berserker Armor Shard
def berserker_armor_shard(b, D):
    gs = b.set("body", "body")
    cuirass(gs, D, tile="rust", slot=MAIN, name="shardplate")
    collar_guard(gs, D, 11, 4.4, spike_tile="crystal", tile="rust")
    with gs.lvl(2):
        crest_disc(gs, D, (0, -4.4, -3.9), 2.6, "crest", normal=(0, 0, -1), name="clawsigil")
        # the shard: jagged plates fanned over the chest, each a flat spike with a lit crack
        sh = Part("shards")
        rr = K.rg(11)
        for k in range(D.cnt(9, 3)):
            x = -4.2 + 8.4 * (k + 0.5) / D.cnt(9, 3)
            base = A((x, -1.0 - 0.6 * (k % 3), -3.3))
            K.crystal(sh, base, A((0.2 * rr.uniform(-1, 1), -1.0, -0.25)), 5.5 + 2.0 * (k % 4), 0.9, 4, tip=0.7)
        gs.add(sh, TRIM, "rust", SHELL | LIT)
        for k in range(D.cnt(7, 3)):                                                    # spine ridge spikes down the back
            spp = Part("spine%d" % k)
            K.spike(spp, A((0, -1.5 - 1.4 * k, 3.4)), A((0, -0.4 - 1.4 * k, 6.4)), 0.6, 4)
            gs.add(spp, TRIM, "horn")
        gk = gs.group("tasset", "swing", (0, -10.6, 0), 8.0, 1.6, 0.0)
        for k in range(D.cnt(7, 3)):                                                    # tassets: hanging plates below the belt
            a = -80 + 160 * k / max(1, D.cnt(7, 3) - 1)
            hang_plate(gs, D, (5.6 * math.sin(math.radians(a)), -10.6, -3.4 * math.cos(math.radians(a))), 2.3, 6.0 + (k % 2) * 1.5, gk, "rust", MAIN, "tas%d" % k)
    with gs.lvl(3):
        veins = Part("blood_veins")
        L.band(veins, -10.2, -1.6, 5.4, 3.4, 0.2, D.dim(28, 10), 0.0, 0.0, 3.2)
        gs.add(veins, MAIN, "vein", LIT, 0, "tile", 5.0)
        K.shards(gs, D, (0.0, -6.0, 0.0), 8.0, 10, 0.5, y_amp=4.0, kind="orbit", speed=2.2, tile="ember", groups=2, name="rage")
        for k in range(D.cnt(5, 2)):
            ch = hang_chain(gs, D, (-4.0 + 2.0 * k, -11.0, -3.2), 7.0, name="bchain%d" % k, swing=(7.0, 1.5, k))
    # the pauldrons: a pair, big spiked
    arm = b.set("arm", "rarm", pair="larm")
    pauldron_plate(arm, D, -1, c=(1.4, 2.4, 0.0), rx=4.6, ry=2.6, rz=3.6, tile="rust", lames=3, drop=1.6, slot=MAIN, name="beast")
    sp = Part("pspikes")
    for k in range(D.cnt(5, 2)):
        a = math.radians(-40 + 80 * k / max(1, D.cnt(5, 2) - 1))
        base = A((-1.4 - 3.0 * math.cos(a) * 0.0 - 2.0 * math.sin(a) * 0, 4.2, 2.6 * math.sin(a)))
        K.spike(sp, base, base + A((-2.4 * math.cos(a), 5.5 - 0.8 * abs(math.sin(a) * 3), 3.0 * math.sin(a))), 0.9, 4)
    arm.add(sp, TRIM, "horn")
    with arm.lvl(3):
        K.wisp(arm, D, (-1.4, 4.6, 0.0), (0, 1, 0), 4.0, 0.5, 3, 0, GLOW, "flame", EMIT | LIT, "ember", seed=3)


# ================================================================ CHEST 2: Orsted's Dragon-God Armor
def orsteds_dragon_god_armor(b, D):
    gs = b.set("body", "body")
    cuirass(gs, D, tile="plate", slot=MAIN, name="dragonplate")
    collar_guard(gs, D, 9, 3.4, spike_tile="horn", tile="plate")
    with gs.lvl(2):
        crest_disc(gs, D, (0, -4.6, -4.0), 3.0, "crest", normal=(0, 0, -1), name="dragonrelief")
        sc = lambda u, v: ((u - 0.5) * 9.6, -11.0 + 9.4 * v, -3.5 - 0.5 * math.sin(math.pi * u))
        K.scale_field(gs, D, sc, 7, 7, 2.4, 2.4, slot=ACCENT, tile="scale", name="dragonscales", thick=0.5)
        gk = gs.group("tasset", "swing", (0, -10.8, 0), 9.0, 1.4, 0.0)
        for k in range(D.cnt(8, 3)):                                                    # tassets
            a = -85 + 170 * k / max(1, D.cnt(8, 3) - 1)
            hang_plate(gs, D, (5.7 * math.sin(math.radians(a)), -10.8, -3.5 * math.cos(math.radians(a))), 2.4, 7.5 + (k % 2) * 1.5, gk, "plate", MAIN, "tas%d" % k)
        p = dict(w0=9.2, w1=12.5, length=15.0, folds=3.0, amp=0.4, flare=1.0, tile="velvet", hem=lambda u: -2.0 * abs(math.sin(math.pi * 5 * u)), rows=3, cols=2, nu=12, nv=12, swing=(3.0, 12.0, 1.2),
                 slot=TRIM)
        from b_back import cloak_fn
        fn = cloak_fn(p)
        K.cloth_bands(gs, D, fn, p["nu"], p["nv"], 3, (0, 0.2, 2.8), p["swing"], tile="velvet", slot=TRIM, cols=2, name="cape")
        wing = gs.group("flange", "sway", (0, -2.0, 3.5), 2.5, 0.06, 0.0)
        for sx in (-1, 1):                                                              # dragon-wing flanges behind the shoulders
            fp = Part("flange%d" % sx)
            for k in range(D.cnt(5, 2)):
                t = k / 4.0
                p0 = A((sx * 3.0, -1.0 - 0.5 * k, 3.8))
                K.spike(fp, p0, p0 + A((sx * (4.0 + 5.0 * (1 - t)), 4.0 + 3.0 * (1 - abs(2 * t - 1)) - 1.2 * k, 1.4)), 0.7, 4)
            gs.add(fp, MAIN, "horn", SHELL, wing)
    with gs.lvl(3):
        veins = Part("curse_veins")
        L.band(veins, -10.2, -1.6, 5.4, 3.4, 0.2, D.dim(28, 10), 0.0, 0.0, 3.2)
        gs.add(veins, MAIN, "vein", LIT, 0, "tile", 5.0)
        hanging_tablets(gs, D, (0, -6.0, 0.0), 9.2, 12, 0.9, "rune_b", speed=1.3, groups=3)
        K.ring_halo(gs, D, (0, -4.0, 4.6), 7.0, 0.22, plane="xy", n=28)
    arm = b.set("arm", "rarm", pair="larm")
    pauldron_plate(arm, D, -1, c=(1.4, 2.4, 0.0), rx=4.4, ry=2.4, rz=3.5, tile="pauldron", lames=4, drop=1.5, slot=MAIN, name="dragon")
    for k, (dx, dz) in enumerate(((0, -1.6), (0, 1.6))):                                 # a horn forward and back
        hp = Part("horn%d" % k)
        base = A((-1.4 + dx, 4.6, dz))
        pts = K.bezier(base, base + A((-1.0, 2.5, dz * 0.8)), base + A((-3.0, 4.2, dz * 1.5)), base + A((-5.0, 4.8, dz * 2.0)), D.dim(6, 3))
        K.tube(hp, pts, 0.8, D.sides(5, 3), taper=lambda t: 1.0 - 0.95 * t, caps=(True, False))
        arm.add(hp, TRIM, "horn")
    with arm.lvl(2):
        K.filigree(arm, D, (-1.4, 3.2, 0.0), (0, 1, 0), 3.0, turns=1.6, r=0.16, segs=24, arms=3, name="pfil")
        K.gem(arm, D, (-1.4, 3.6, 0.0), 0.8, 1.1, 6, "y", name="pgem")


# ================================================================ FEET: Greaves of the Wind Walker
def greaves_of_the_wind_walker(b, D):
    leg = b.set("leg", "rleg", pair="lleg")
    # shin guard: a lofted shell down the front of the leg, boot foot, calf plate, knee cop
    sh = Part("shin")
    loft(sh, (0, -1, 0), (0, -5.4, -0.3), [(0.0, 1.0, 1.0), (1.8, 0.95, 1.0), (4.2, 0.82, 0.92), (5.2, 0.8, 0.9)], ml.ellipse(2.75, 2.7, D.sides(14, 6)), caps=(False, False))
    leg.add(sh, MAIN, "plate", SHELL, 0, "tile", 6.0)
    boot = Part("boot")
    loft(boot, (0, 0, -1), (0, -10.6, 2.4), [(0.0, 1.0, 1.0), (3.0, 1.05, 1.05), (6.0, 0.95, 0.85), (7.0, 0.4, 0.5)], ml.ellipse(2.8, 1.7, D.sides(12, 6)), caps=(True, True))
    leg.add(boot, MAIN, "leather", SHELL)
    with leg.lvl(2):
        knee = Part("knee")
        L.sph(knee, (0, -4.7, -2.7), 2.1, 4, D.sides(10, 6), squash=0.7)
        leg.add(knee, TRIM, "gilt", SHELL)
        for yy, rr in ((-5.8, 3.0), (-7.6, 2.95), (-9.4, 2.9)):                          # trim rings
            r = Part("ring%.0f" % -yy)
            L.torus(r, (0, yy, 0), rr, 0.24, D.dim(18, 8), 4)
            leg.add(r, TRIM, "gilt")
        crest_disc(leg, D, (0, -7.4, -3.2), 1.5, "crest", normal=(0, 0, -1), name="windsigil")
        band = Part("curse_band")                                                       # the cursed band: dark, cracked, lit
        L.band(band, -8.9, -8.0, 2.95, 2.95, 0.3, D.dim(18, 8), 0.0, 0.0, 3.0)
        leg.add(band, DARK, "vein", LIT, 0, "tile", 5.0)
    # wings: a fan of long feathers sweeping back from each ankle, and a smaller fan from the calf
    wg = leg.group("anklewing", "sway", (2.4, -9.0, 1.0), 5.0, 0.09, 0.0)
    K.feather_fan(leg, D, (2.6, -9.0, 0.6), (0.6, 0.15, 1.0), 8, 9.0, 1.3, 1.1, slot=ACCENT, tile="feather", group=wg, name="wingA", ranks=2, curl=0.15)
    wg2 = leg.group("calfwing", "sway", (2.8, -6.6, 0.8), 6.0, 0.08, 1.2)
    K.feather_fan(leg, D, (2.8, -6.6, 0.8), (0.9, 0.3, 0.7), 6, 6.0, 1.0, 0.9, slot=MAIN, tile="feather", group=wg2, name="wingB", ranks=2)
    wg3 = leg.group("heelwing", "sway", (-0.0, -10.4, 2.3), 4.0, 0.12, 2.0)
    K.feather_fan(leg, D, (0.0, -10.4, 2.3), (0.0, 0.2, 1.0), 5, 6.0, 0.9, 0.8, slot=ACCENT, tile="feather", group=wg3, name="wingC")
    with leg.lvl(3):
        for k in range(2):                                                              # spinning wind rings
            g = leg.group("windring%d" % k, "spin", (0, -6.4 - 3.0 * k, 0), 7.0 * (1 if k == 0 else -1), 0.0, 0.0)
            rp = Part("windring%d" % k)
            L.torus(rp, (0, -6.4 - 3.0 * k, 0), 3.9 + 0.5 * k, 0.12, D.dim(26, 10), 4)
            leg.add(rp, GLOW, "ring_glow", EMIT | HOT | ADD, g, "fit")
            sp = Part("windstreaks%d" % k)
            for j in range(D.cnt(4, 2)):
                a = 2 * math.pi * j / D.cnt(4, 2)
                pts = [(math.cos(a + 0.4 * i / 5) * (3.9 + 0.5 * k), -6.4 - 3.0 * k + 0.3 * math.sin(i), math.sin(a + 0.4 * i / 5) * (3.9 + 0.5 * k)) for i in range(6)]
                K.tube(sp, pts, 0.2, 4, taper=lambda t: 1.0 - 0.9 * t)
            leg.add(sp, GLOW, "soft", EMIT | ADD, g, "fit")
        rb = leg.group("trail", "wave", (0, -9.0, 2.0), 1.6, 0.14, 0.45)
        for j in range(D.cnt(3, 2)):
            pts = [(-1.2 + 1.2 * j, -10.0 - 0.1 * i, 2.5 + 2.6 * i) for i in range(D.dim(6, 3))]
            K.ribbon(leg, D, pts, 1.4, GLOW, "soft", EMIT | ADD, rb, "trail%d" % j, side=(1, 0, 0))
        K.filigree(leg, D, (0, -7.2, -3.0), (0, 0, -1), 2.4, turns=1.6, r=0.12, segs=20, arms=2, name="shinfil")
        K.shards(leg, D, (0, -6.0, 0.0), 4.4, 5, 0.3, y_amp=3.0, kind="orbit", speed=3.2, tile="spark", groups=1, name="breeze")


# ================================================================ NECK: White Whistle
def white_whistle(b, D):
    gs = b.set("body", "body")
    # the cord: a braided loop round the neck down to the whistle
    cord = Part("cord")
    pts = K.bezier(A((-3.0, 1.0, -1.0)), A((-3.8, -1.5, -3.6)), A((-1.2, -3.2, -3.8)), A((0.0, -4.4, -3.7)), D.dim(8, 4))
    pts2 = K.bezier(A((3.0, 1.0, -1.0)), A((3.8, -1.5, -3.6)), A((1.2, -3.2, -3.8)), A((0.0, -4.4, -3.7)), D.dim(8, 4))
    for path in (pts, pts2):
        K.tube(cord, path, 0.34, D.sides(6, 4))
    back = K.bezier(A((-3.0, 1.0, -1.0)), A((-3.4, 1.8, 2.6)), A((3.4, 1.8, 2.6)), A((3.0, 1.0, -1.0)), D.dim(10, 4))
    K.tube(cord, back, 0.34, D.sides(6, 4))
    gs.add(cord, TRIM, "rope", 0, 0, "tile", 10.0)
    with gs.lvl(2):
        for k in range(D.cnt(7, 3)):                                                    # beads strung on the cord
            t = (k + 1) / (D.cnt(7, 3) + 1)
            pnt = K.bezier(A((-3.0, 1.0, -1.0)), A((-3.8, -1.5, -3.6)), A((-1.2, -3.2, -3.8)), A((0.0, -4.4, -3.7)), 12)[int(t * 12)]
            bd = Part("bead%d" % k)
            L.sph(bd, pnt, 0.55, 3, 8)
            gs.add(bd, ACCENT, "pearl", SHELL)
    top = A((0.0, -4.7, -3.7))
    wh = Part("whistle")
    lathe(wh, "y", tuple(top - A((0, 0.4, 0))), [(0.0, 0.5), (-1.0, 0.95), (-3.0, 1.15), (-6.0, 1.35), (-7.6, 1.5), (-8.2, 0.9), (-8.6, 0.0)], sides=D.sides(14, 6), squash=0.75)
    gs.add(wh, MAIN, "bone", SHELL, 0, "tile", 6.0)
    with gs.lvl(2):
        mp = Part("mouthpiece")
        K.beam(mp, top + A((0, 0.4, 0)), top + A((0, 1.9, 0.6)), 0.9, 0.8)
        gs.add(mp, TRIM, "gilt")
        for yy, rr in ((-0.9, 1.0), (-4.3, 1.22), (-7.0, 1.45)):                          # gold bands
            bnd = Part("wband%.0f" % -yy)
            L.torus(bnd, tuple(top + A((0, yy, 0))), rr, 0.2, D.dim(14, 8), 4, 1.0, 0.75)
            gs.add(bnd, TRIM, "gilt")
        hole = Part("windhole")
        K.dia(hole, top + A((0, -2.2, -0.9)), 0.45, 0.7, 0.2)
        gs.add(hole, GLOW, "spark", EMIT | HOT | LIT, 0, "fit")
        crest_disc(gs, D, tuple(top + A((0, -5.0, -1.2))), 0.75, "crest", normal=(0, 0, -1), name="abyssmark", n=10)
    with gs.lvl(3):
        K.filigree(gs, D, tuple(top + A((0, -3.6, -1.3))), (0, 0, -1), 1.8, turns=1.8, r=0.08, segs=24, arms=2, name="wfil", plane_up=(0, 1, 0))
        for k in range(3):                                                              # sound rings that swell outward from the whistle
            g = gs.group("sound%d" % k, "pulse", tuple(top + A((0, -4.0, -1.0))), 0.28, 0.1 + 0.02 * k, k * 2.0)
            rp = Part("sound%d" % k)
            K.link(rp, top + A((0, -4.0, -1.2)), A((1, 0, 0)), A((0, 0, 1)), 2.4 + 1.1 * k, 2.4 + 1.1 * k, 0.1, D.dim(22, 8), 4)
            gs.add(rp, GLOW, "ring_glow", EMIT | HOT | ADD, g, "fit")
        for k in range(D.cnt(6, 2)):
            ch = Part("note%d" % k)
            K.dia(ch, top + A((2.2 * math.cos(k * 1.1), -6.0 + 1.2 * k, -2.0 * math.sin(k * 0.9 + 1.0) - 1.4)), 0.28, 0.5, 0.28)
            gs.add(ch, GLOW, "spark", EMIT | HOT | ADD, gs.group("note%d" % k, "bob", tuple(top), 0.8, 0.07 + 0.01 * k, k), "fit")
        K.shards(gs, D, tuple(top + A((0, -4.0, 0.0))), 3.4, 8, 0.26, y_amp=3.2, kind="orbit", speed=2.6, tile="pearl", groups=2, name="breath")
        K.ring_halo(gs, D, tuple(top + A((0, -4.0, -1.0))), 3.6, 0.1, plane="xy", n=24)


BUILDERS = {"excaliburs_scabbard_avalon": excaliburs_scabbard_avalon, "silvered_sash_of_reflections": silvered_sash_of_reflections, "berserker_armor_shard": berserker_armor_shard,
            "orsteds_dragon_god_armor": orsteds_dragon_god_armor, "greaves_of_the_wind_walker": greaves_of_the_wind_walker, "white_whistle": white_whistle}

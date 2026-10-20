"""Magic Emperor's Black Halberd (Wave 71, Genesis, Black Clover): a halberd of the dark that waits behind the five-leaf clover: black as a dead star, edged with a magenta that is
not light.

An axe-blade and a spear tip in dark star-metal with magenta edges, a spiked five-leaf clover for a guard, a gem socket, and the dimensional void showing through rifts of the
blades (the Genesis Demon-Slayer's shader). The axe is a crescent swept from the socket, a long hook-spike balances it behind. Three dark orbs (the Dark Triad) circle the head, a
five-leaf rune clover turns behind it. Palette: 14101C black, C02A5A magenta, FF4A8A bright magenta.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (160.0, 244.0)
Y_HEAD = 176.0
Y_TIP = 242.0
MAG = (255, 74, 138)
DEEP = (192, 42, 90)


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- the haft: star-metal, spiral inlays of magenta, a wrapped grip with clover medallions
    sh = it.part("haft", "starmetal", base=(20, 16, 28), smooth=50, uv_mode="loft", density=1.15, wear=0.6)
    G.rod(it, sh, -2.0, Y_HEAD, 1.9, 2.1, sides=18, swell=[(0.3, 1.15), (0.31, 1.0), (0.62, 1.15), (0.63, 1.0)])

    def haft_paint(ctx):
        if ctx.kind != "loft":
            return
        sp = np.abs(np.sin(ctx.B * 0.42 + ctx.S * 6.2832 * 2.0)) < 0.045
        m = tx.blur(sp.astype(np.float32), 0.7)
        tx.engrave(ctx, m, 0.5)
        tx.put(ctx.vein, m, MAG, 0.95, 0.6, halo=0.8)
        tx.put(ctx.glow, m * 0.5, DEEP, 0.7, 0.4)
        tx.stars(ctx, 0.0025, 3, (255, 200, 230), 1.0, 0.9)
    sh.painters.append(haft_paint)
    gr = it.part("grip_wrap", "leather", base=(24, 18, 30), smooth=55, uv_mode="loft", density=1.1)
    for i in range(it.n(22, 6)):
        ml.collar(gr, 10.0 + 3.0 * i, 2.22, 1.7, lip=0.3 if full else 0.0, sides=it.n(12, 5), caps=(False, False))
    cm = it.part("clover_medallions", "gold", base=(150, 40, 90), smooth=40, uv_mode="loft")
    for y in (40.0, 85.0, 130.0):
        ml.collar(cm, y, 2.7, 3.0, lip=0.8 if full else 0.0, sides=it.n(14, 5), caps=(full, full))
        if full:
            for k in range(5):
                a = 2 * math.pi * k / 5
                G.orb(it, cm, (2.5 * math.cos(a), y + 1.5, 2.5 * math.sin(a)), 0.55, sides=6, stacks=3)
    bt = it.part("butt", "starmetal", base=(30, 22, 40), smooth=40, uv_mode="loft")
    ml.lathe(bt, "y", (0, 0, 0), [(-6.0, 0.0), (-4.0, 1.4), (-1.0, 2.5), (3.0, 2.0), (8.0, 1.95)], sides=it.n(12, 5), caps=(False, False))

    # ---------------------------------------------------------------- the spear blade: a long leaf with a fuller and magenta edges
    def hw(t):
        return 5.2 * math.sin(math.pi * min(1.0, t * 1.0 / 0.4 if t < 0.4 else 1.0)) ** 0.5 * (1 - t) ** 0.6 + 0.6 * (1 - t)
    sp = it.part("spear_blade", "starmetal", base=(18, 14, 26), smooth=40, density=1.2, wear=0.6)
    G.blade_by(it, sp, Y_HEAD + 6.0, Y_TIP, lambda t: max(0.05, 4.8 * (1 - 0.9 * t ** 1.3) + 0.9), lambda t: 0.85 - 0.35 * t, n=46, bevel=0.55, groove=0.4 if full else 0.0, tip=0.12)

    def blade_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            tx.tint(ctx, (200, 70, 130), 0.2)
            return
        X, Y = ctx.X, ctx.Y
        t = np.clip((Y - (Y_HEAD + 6.0)) / (Y_TIP - Y_HEAD - 6.0), 0, 1)
        half = np.maximum(4.8 * (1 - 0.9 * t ** 1.3) + 0.9, 0.2)
        u = X / half
        edge = np.maximum(tx.soft(u - 1.0, 0.14), tx.soft(u + 1.0, 0.14))
        ctx.img[..., :3] += (np.array([255, 120, 180], np.float32) - ctx.img[..., :3]) * (0.55 * edge)[..., None]
        tx.put(ctx.glow, edge * 0.9, MAG, 0.95, 0.6, halo=1.4)
        tx.stars(ctx, 0.004, 5, (255, 190, 230), 1.0, 1.0)
        web = tx.caustics(ctx, 22.0, 4, 2.6)
        tx.put(ctx.vein, web * tx.soft(u, 0.45), (255, 140, 190), 0.8, 0.6)
        m = np.zeros(ctx.shape, np.float32)
        for i, g in enumerate(tx.rune_glyphs(6, 31 + int(ctx.sign))):
            m = np.maximum(m, tx.mask_text(ctx, g, 0.0, Y_HEAD + 14.0 + i * 8.6, 1.0, rot=90.0, fname="DejaVuSans.ttf"))
        tx.engrave(ctx, m * (np.abs(u) < 0.35), 0.6)
        tx.put(ctx.glow, m * (np.abs(u) < 0.35), MAG, 0.9, 0.5)
    sp.painters.append(blade_paint)

    # ---------------------------------------------------------------- the crescent axe, swept from the socket toward -x
    ax = it.part("axe_blade", "starmetal", base=(22, 16, 32), smooth=40, uv_mode="loft", density=1.3, wear=0.6)
    cxr, cyr, R = -1.5, 205.0, 21.5
    arc_pts = []
    n_a = it.n(60, 10)
    a0, a1 = 92.0, 268.0
    for i in range(n_a + 1):
        a = math.radians(a0 + (a1 - a0) * i / n_a)
        arc_pts.append((cxr + R * math.cos(a), cyr + R * math.sin(a), 0.0))
    SEC = G.blade_section(0.55, 0.0)
    ml.sweep(ax, arc_pts, SEC, scales=lambda t: (max(0.2, 8.6 * abs(math.sin(math.pi * t)) ** 0.8 + 0.3), 0.85 * (0.45 + 0.55 * abs(math.sin(math.pi * t)))), up=(0, 0, 1), caps=(False, False))
    marks = G.section_marks(SEC, 8.0, 0.8)

    def axe_paint(ctx):
        if ctx.kind != "loft":
            return
        S, T = ctx.S, ctx.T
        edge = np.maximum(tx.soft(S - 0.0, 0.05), np.maximum(tx.soft(S - 1.0, 0.05), tx.soft(S - 0.5, 0.05)))
        ctx.img[..., :3] += (np.array([255, 120, 180], np.float32) - ctx.img[..., :3]) * (0.55 * edge)[..., None]
        tx.put(ctx.glow, edge * np.abs(np.sin(np.pi * np.clip(T, 0, 1))) ** 0.5, MAG, 0.95, 0.6, halo=1.4)
        tx.stars(ctx, 0.004, 9, (255, 190, 230), 1.0, 1.0)
        web = tx.caustics(ctx, 26.0, 6, 2.4)
        tx.put(ctx.vein, web * (0.2 + 0.8 * tx.soft(S - 0.25, 0.3)) * 0.8, (255, 140, 190), 0.8, 0.6)
    ax.painters.append(axe_paint)
    # the void rifts through both blades (layer 1)
    rng = np.random.default_rng(7)
    for nm, y0, y1 in (("void_spear", Y_HEAD + 18.0, Y_HEAD + 52.0),):
        vp = it.part(nm, "steel", layer=1)
        for sgn in (1, -1):
            ys = np.linspace(y0, y1, 7 if full else 3)
            jag = [float(rng.uniform(-0.4, 0.4)) for _ in ys]
            wd = [0.9 * (0.4 + 0.6 * math.sin(math.pi * k / (len(ys) - 1))) for k in range(len(ys))]
            z = sgn * 0.8
            ia = [vp.add_vertex((jag[k] - wd[k], ys[k], z)) for k in range(len(ys))]
            ib = [vp.add_vertex((jag[k] + wd[k], ys[k], z)) for k in range(len(ys))]
            for k in range(len(ys) - 1):
                if sgn > 0:
                    vp.add_face(ia[k], ib[k], ib[k + 1], ia[k + 1])
                else:
                    vp.add_face(ia[k], ia[k + 1], ib[k + 1], ib[k])
    va = it.part("void_axe", "steel", layer=1)
    for sgn in (1, -1):
        n_v = 9 if full else 4
        L = [(cxr + (R + off) * math.cos(math.radians(a0 + 22.0 + (a1 - a0 - 44.0) * k / (n_v - 1))), cyr + (R + off) * math.sin(math.radians(a0 + 22.0 + (a1 - a0 - 44.0) * k / (n_v - 1))))
             for k in range(n_v) for off in (-2.2,)]
        Rr = [(cxr + (R + off) * math.cos(math.radians(a0 + 22.0 + (a1 - a0 - 44.0) * k / (n_v - 1))), cyr + (R + off) * math.sin(math.radians(a0 + 22.0 + (a1 - a0 - 44.0) * k / (n_v - 1))))
              for k in range(n_v) for off in (2.2,)]
        ia = [va.add_vertex((x, y, sgn * 0.62)) for (x, y) in L]
        ib = [va.add_vertex((x, y, sgn * 0.62)) for (x, y) in Rr]
        for k in range(n_v - 1):
            if sgn > 0:
                va.add_face(ia[k], ib[k], ib[k + 1], ia[k + 1])
            else:
                va.add_face(ia[k], ia[k + 1], ib[k + 1], ib[k])

    # the hook-spike behind: a curved fang swept toward +x
    hk = it.part("hook_spike", "starmetal", base=(24, 18, 34), smooth=40, uv_mode="loft", density=1.2)
    sp_pts = [(3.0 + 18.6 * (i / 14.0) ** 1.1, Y_HEAD + 8.0 + 24.0 * (i / 14.0) ** 1.9 - 3.0 * (i / 14.0), 0.0) for i in range(it.n(14, 5) + 1)]
    sp_pts = [(sp_pts[i][0], sp_pts[i][1], 0.0) for i in range(len(sp_pts))]
    ml.sweep(hk, sp_pts, G.blade_section(0.5, 0.0), scales=lambda t: (max(0.05, 3.4 * (1 - t) ** 0.9), 0.9 * (1 - t) ** 0.8), up=(0, 0, 1), caps=(False, False))
    hk.painters.append(lambda ctx: (tx.put(ctx.glow, tx.soft(ctx.S - 0.0, 0.06) * 0.8, MAG, 0.9, 0.6, halo=1.0), tx.stars(ctx, 0.004, 3, (255, 190, 230), 1.0, 1.0)))

    # ---------------------------------------------------------------- the five-leaf clover guard with spiked tips and a gem socket
    cl = it.part("clover_guard", "gold", base=(40, 28, 52), smooth=40, uv_mode="loft", density=1.3, wear=0.7)
    tips = it.part("clover_spikes", "gold", base=(120, 30, 76), smooth=25)
    for k in range(5):
        a = math.radians(90 + 72 * k)
        d = np.array([math.cos(a), math.sin(a), 0.0])
        G.feather(it, cl, np.array([0.0, Y_HEAD, 0.0]) + d * 1.5, d, 11.0, 5.6, thick=0.95, curl=0.0, side=(0, 0, 1), n=7)
        tp = np.array([0.0, Y_HEAD, 0.0]) + d * 12.4
        G.cone(tips, tp, tp + d * 4.4, 0.9, 5)
        if full:
            sd = np.array([-d[1], d[0], 0.0])
            for sgn in (-1, 1):
                G.cone(tips, np.array([0.0, Y_HEAD, 0.0]) + d * 8.0 + sd * sgn * 3.6, np.array([0.0, Y_HEAD, 0.0]) + d * 9.5 + sd * sgn * 5.4, 0.5, 4)

    def clover_paint(ctx):
        if ctx.kind != "loft":
            return
        edge = np.maximum(tx.soft(ctx.S, 0.08), tx.soft(ctx.S - 1.0, 0.08))
        tx.put(ctx.glow, edge * 0.8, MAG, 0.9, 0.6, halo=0.8)
        tx.put(ctx.vein, tx.soft(ctx.S - 0.5, 0.1) * 0.6, (255, 140, 190), 0.8, 0.6)
        ctx.img[..., :3] = ctx.img[..., :3] * 0.8 + np.array([60, 20, 50], np.float32) * 0.2
    cl.painters.append(clover_paint)
    sock = it.part("gem_socket", "starmetal", base=(30, 22, 42), smooth=40, uv_mode="loft", density=1.3)
    ml.lathe(sock, "y", (0, 0, 0), [(Y_HEAD - 3.0, 2.2), (Y_HEAD - 1.0, 3.6), (Y_HEAD + 2.0, 3.8), (Y_HEAD + 5.0, 2.6), (Y_HEAD + 8.0, 2.0)], sides=it.n(16, 6), caps=(False, False))
    gem = it.part("socket_gem", "crystal", base=(255, 60, 130), smooth=30, density=1.4)
    for sgn in (1, -1):
        ml.gem(gem, (0.0, Y_HEAD, sgn * 1.4), 2.7, 0.7, 8 if full else 5, axis=(0, 0, sgn))

    def gem_paint(ctx):
        n = tx.fbm(ctx.shape, 6.0, ctx.seed + 2, 3)
        ctx.img[..., :3] = tx.lerp3((100, 8, 50), (255, 100, 170), np.clip(n * 1.5, 0, 1))
        tx.put(ctx.glow, np.ones(ctx.shape, np.float32), MAG, 1.0, 0.8, halo=0.6)
        tx.put(ctx.vein, tx.caustics(ctx, 5.0, 3, 2.0), (255, 220, 235), 0.9, 0.9)
    gem.painters.append(gem_paint)

    # ---------------------------------------------------------------- the Dark Triad: three orbs round the head, a rune clover turning behind it
    it.define_group("triad", pivot=(0.0, Y_HEAD + 6.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 75.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.2, "period": 90.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 14})
    if full:
        tr = it.part("triad_orbs", "crystal", group="triad", base=(60, 12, 50), smooth=45, uv_mode="loft")
        tg = it.part("triad_halos", "gold", group="triad", base=(200, 50, 110), smooth=40, uv_mode="loft")
        for k in range(3):
            a = 2 * math.pi * k / 3
            c = (15.0 * math.cos(a), Y_HEAD + 6.0 + 2.0 * math.sin(a * 2), 15.0 * math.sin(a))
            G.orb(it, tr, c, 2.6, sides=12, stacks=8)
            G.torus(it, tg, c, 3.7, 0.2, (math.cos(a + 1.0), 0.3, math.sin(a + 1.0)), major=18, minor=4)

        def orb_paint(ctx):
            n = tx.fbm(ctx.shape, 8.0, ctx.seed + 1, 3)
            tx.stars(ctx, 0.01, 1, (255, 200, 230), 1.0, 1.0)
            tx.put(ctx.glow, np.clip(n * 1.2, 0, 1) * 0.75, MAG, 0.9, 0.6, halo=0.5)
            tx.put(ctx.vein, tx.caustics(ctx, 6.0, 2, 2.0) * 0.8, (255, 160, 200), 0.8, 0.7)
        tr.painters.append(orb_paint)
        tg.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), MAG, 0.9, 0.7))
    it.define_group("clover_rune", pivot=(0.0, Y_HEAD, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 7.0}, {"kind": "breathe", "min": 0.3, "max": 0.7, "period": 150.0}], tint="FF4A8A",
                    lod_skip=True, alpha=0.8, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "clover_rune_card", "clover_rune", (0.0, Y_HEAD, -1.8), (1, 0, 0), (0, 1, 0), 24.0, 24.0, "runes")
    G.card(it, "clover_rune_card", "clover_rune", (0.0, Y_HEAD, 1.8), (1, 0, 0), (0, -1, 0), 24.0, 24.0, "runes")
    it.define_group("edge_glare", anim=[{"kind": "breathe", "min": 0.25, "max": 0.7, "period": 85.0}], tint="FF4A8A", lod_skip=True, alpha=0.7)
    for sgn in (1, -1):
        G.card(it, "axe_glare", "edge_glare", (cxr - R - 4.0, cyr, sgn * 0.9), (0, 1, 0), (-1, 0, 0), 22.0, 6.0, "streak")
    G.card(it, "spear_glare", "edge_glare", (0.0, Y_HEAD + 30.0, 0.0), (1, 0, 0), (0, 1, 0), 8.0, 38.0, "glow")
    G.card(it, "spear_glare", "edge_glare", (0.0, Y_HEAD + 30.0, 0.0), (0, 0, 1), (0, 1, 0), 8.0, 38.0, "glow")
    it.define_group("rift", pivot=(0.0, Y_HEAD + 20.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 4.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": -40.0}], tint="FF8ABA", lod_skip=True, alpha=0.9)
    for k in range(4):
        a = k * 1.7
        G.card(it, "rift_%d" % k, "rift", (7.0 * math.cos(a), Y_HEAD + 14.0 + 11.0 * k, 7.0 * math.sin(a)), (math.cos(a + 1.5), 0, math.sin(a + 1.5)), (0, 1, 0), 4.5, 8.0, "bolt")
    it.flare((0.0, Y_HEAD, 3.4), 9.0, "FF4A8A", "star", phase=0.0, pulse=0.6)
    it.flare((0.0, Y_HEAD, -3.4), 9.0, "FF4A8A", "star", phase=0.5, pulse=0.6)
    it.flare((cxr + (R + 8.6) * math.cos(math.radians(180)), cyr, 0.0), 6.0, "FF8ABA", "sparkle", phase=0.2)
    it.flare((0.0, Y_TIP - 1.0, 0.0), 6.0, "FF8ABA", "sparkle", phase=0.7)

    it.glow = ("FFFFFF", 0.16, 0.4, 1.0)
    it.layers = [
        {"id": "dark_triad", "kind": "orbit_shards", "group": "triad", "note": "three dark orbs with magenta rings circling the head"},
        {"id": "clover_rune", "kind": "halo_ring", "group": "clover_rune", "note": "a five-leaf rune disc turning behind the guard, both faces"},
        {"id": "void_rifts", "kind": "distortion", "layer": "void", "note": "rifts of the dimensional void in the spear blade and the axe (reality shimmer)"},
        {"id": "edge_glare", "kind": "light_shafts", "group": "edge_glare", "note": "magenta glare along the axe edge and the spear blade, breathing"},
        {"id": "rift_arcs", "kind": "energy_arcs", "group": "rift", "note": "four flickering arcs of dark lightning round the head"},
        {"id": "gem_flares", "kind": "lens_flare", "flares": "gem", "note": "star flares in the gem and on the edges"},
        {"id": "dark_motes", "kind": "motes", "particle": "dust", "rgb": "C02A5A", "where": "blade", "rate": 0.5, "radius": 2.0, "note": "magenta motes drifting off the blades"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "14101C", "radius": 1.0, "note": "a ring of dark motes on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "inlay_flow", "kind": "vein_wave", "note": "magenta runs through the spiral inlays of the haft and the blades"},
        {"id": "triad_orbit", "kind": "group_spin", "group": "triad"},
        {"id": "clover_turn", "kind": "group_spin", "group": "clover_rune"},
        {"id": "rift_flicker", "kind": "group_flicker", "group": "rift"},
        {"id": "glare_breath", "kind": "group_breathe", "group": "edge_glare"},
        {"id": "gem_pulse", "kind": "flare_pulse"},
    ]
    it.draw = {"ticks": 16, "flash": "FF4A8A", "groups": ["triad", "clover_rune"], "note": "the three orbs spiral out from the socket and the clover opens behind the guard"}
    it.swing = {"trail": "dead_star", "rgb": "FF4A8A", "rgb2": "14101C", "length": 2.2, "width": 0.45, "life": 11, "particles": "dark_mote"}
    it.sound = "nusmp:item.genesis.black_halberd.void_hum_loop"
    it.acts = {
        "emperors_sweep": {"burst": "emperor_sweep", "pattern": "cone", "rgb": "FF4A8A", "ring": 6.0, "particles": 40, "sound": "nusmp:item.genesis.black_halberd.sweep"},
        "dimensional_rift": {"burst": "dimension_rift", "pattern": "column", "rgb": "C02A5A", "ring": 5.0, "particles": 40, "sound": "nusmp:item.genesis.black_halberd.rift"},
        "dark_triad": {"burst": "dark_triad", "pattern": "spiral", "rgb": "FF8ABA", "ring": 4.5, "particles": 45, "sound": "nusmp:item.genesis.black_halberd.triad"},
    }

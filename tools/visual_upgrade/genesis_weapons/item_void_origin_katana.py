"""Void Origin Katana (Wave 71, Genesis, That Time I Got Reincarnated as a Slime): a katana of a blue-black that is not a colour so much as an absence. It was drawn once by a lord who
did not need to be anything else.

A long curved blade of blue-black that is half void: the dimensional void of the Genesis Demon-Slayer fills the flat of the blade and only the temper line (the hamon) and the edge
are cyan; a ring tsuba, a habaki of cyan light, a diamond-wrapped hilt in black and cyan cord, a cyan crystal kashira; rings of torn space turn about the blade and afterimages of
the cut trail behind it. Palette: 101828 blue-black, 6AE0FF cyan, 40C8FF light.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (14.0, 52.0)
Y_T = 26.0                       # the tsuba
Y_TIP = 126.0
CYAN = (106, 224, 255)
LIGHT = (64, 200, 255)


def cx(t):
    return -7.5 * t ** 2.2


def hw(t):
    return 2.3 * (1.0 - 0.4 * t ** 1.5)


def ht(t):
    return 0.55 * (1.0 - 0.4 * t)


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- the blade: a ridge-lined curved blade, the flat given to the void, a cyan hamon at the edge
    SEC = [(1.0, 0.0), (0.9, 0.55), (0.62, 1.0), (-0.38, 1.0), (-1.0, 0.0), (-0.38, -1.0), (0.62, -1.0), (0.9, -0.55)]
    bl = it.part("blade", "starmetal", base=(14, 22, 44), smooth=38, density=1.5, wear=0.6)
    n = it.n(96, 10)
    st = []
    for i in range(n + 1):
        t = i / n
        y = Y_T + 1.0 + (Y_TIP - Y_T - 1.0) * t
        h, th, c = hw(t), ht(t), cx(t)
        if t > 0.9:
            k = (1.0 - t) / 0.1
            h, th = h * k, th * k
        st.append((y, h - c, h + c, th))
    G.blade(it, bl, st, section=SEC[::-1] if False else [(-x, y) for (x, y) in SEC], caps=(True, True))

    def blade_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            return
        X, Y = ctx.X, ctx.Y
        t = np.clip((Y - Y_T - 1.0) / (Y_TIP - Y_T - 1.0), 0, 1)
        c = -7.5 * t ** 2.2
        h = np.maximum(2.3 * (1.0 - 0.4 * t ** 1.5), 0.15)
        u = (X - c) / h                                                   # -1 at the edge, +1 at the spine
        # the hamon: a wavy temper line of cyan just above the edge, with flecks of nie light
        wave = -0.55 + 0.12 * np.sin(Y * 0.5) + 0.07 * np.sin(Y * 1.9 + 1.0)
        line = tx.soft(u - wave, 0.1)
        zone = np.clip(1.0 - np.maximum(u - wave, 0.0) * 3.0, 0, 1) * (u < wave + 0.6)
        ctx.img[..., :3] = ctx.img[..., :3] * (1 - 0.35 * zone[..., None]) + np.array([40, 140, 200], np.float32) * (0.35 * zone)[..., None]
        ctx.img[..., :3] += (np.array([200, 245, 255], np.float32) - ctx.img[..., :3]) * (0.6 * line)[..., None]
        fleck = tx.fbm(ctx.shape, 3.0, ctx.seed + 3, 2) > 0.8
        tx.put(ctx.glow, np.clip(line + 0.35 * zone * fleck, 0, 1), CYAN, 1.0, 0.7, halo=1.2)
        tx.put(ctx.vein, np.clip(line * 1.2, 0, 1), (230, 255, 255), 0.9, 0.9, halo=0.6)
        edge = tx.soft(u + 1.0, 0.09)
        ctx.img[..., :3] += (np.array([220, 250, 255], np.float32) - ctx.img[..., :3]) * (0.7 * edge)[..., None]
        tx.put(ctx.glow, edge * 0.8, (190, 245, 255), 0.9, 0.8, halo=0.9)
        # engraved lines of the ridge (shinogi) and a faint star dust in the blue-black
        shin = tx.soft(u - 0.28, 0.035)
        tx.engrave(ctx, shin, 0.5)
        tx.stars(ctx, 0.0025, 4, (190, 235, 255), 1.0, 0.9)
        m = np.zeros(ctx.shape, np.float32)
        for i, g in enumerate(tx.rune_glyphs(5, 66 + int(ctx.sign))):
            yy = Y_T + 14.0 + i * 16.0
            tt = (yy - Y_T - 1.0) / (Y_TIP - Y_T - 1.0)
            m = np.maximum(m, tx.mask_text(ctx, g[0], -7.5 * tt ** 2.2 + 0.98, yy, 0.9, rot=90.0, fname="DejaVuSans.ttf"))
        tx.engrave(ctx, m, 0.7)
    bl.painters.append(blade_paint)

    # the void in the flat of the blade (layer 1): a long ribbon on each face, between the shinogi and the spine, the whole length
    for sgn in (1, -1):
        vd = it.part("void_flat_%s" % ("a" if sgn > 0 else "b"), "steel", layer=1)
        m_ = 18 if full else 6
        ia, ib = [], []
        for k in range(m_ + 1):
            t = 0.04 + 0.86 * k / m_
            y = Y_T + 1.0 + (Y_TIP - Y_T - 1.0) * t
            c = cx(t)
            w0 = hw(t) * 0.38
            w1 = hw(t) * 0.86
            z = sgn * (ht(t) * (1.0 - 0.0) + 0.045)
            ia.append(vd.add_vertex((c + w0 * 0.55, y, z)))
            ib.append(vd.add_vertex((c + w1 * 0.86, y, z)))
        for k in range(m_):
            if sgn > 0:
                vd.add_face(ia[k], ib[k], ib[k + 1], ia[k + 1])
            else:
                vd.add_face(ia[k], ia[k + 1], ib[k + 1], ib[k])

    # ---------------------------------------------------------------- tsuba (a ring guard of black and cyan), habaki, seppa
    ts = it.part("tsuba_ring", "starmetal", base=(10, 18, 36), smooth=45, uv_mode="loft", density=1.3)
    G.torus(it, ts, (0.0, Y_T, 0.0), 5.2, 0.85, (0, 1, 0), major=36, minor=8)
    td = it.part("tsuba_disc", "starmetal", base=(12, 20, 40), smooth=40, uv_mode="loft", density=1.3)
    ml.lathe(td, "y", (0, Y_T, 0), [(-0.45, 2.6), (-0.2, 4.7), (0.2, 4.7), (0.45, 2.6)], sides=it.n(28, 8), caps=(True, True))
    if full:
        tf = it.part("tsuba_fret", "starmetal", base=(16, 28, 54), smooth=40, uv_mode="loft", density=1.2)
        G.torus(it, tf, (0.0, Y_T, 0.0), 3.0, 0.42, (0, 1, 0), major=28, minor=6)
        for k in range(8):
            a = 2 * math.pi * k / 8 + 0.2
            G.tube(it, tf, [(3.0 * math.cos(a), Y_T, 3.0 * math.sin(a)), (4.0 * math.cos(a), Y_T, 4.0 * math.sin(a)), (5.0 * math.cos(a), Y_T, 5.0 * math.sin(a))], 0.3, 0.25, sides=5)
        tf.painters.append(lambda ctx: tx.put(ctx.vein, np.ones(ctx.shape, np.float32) * 0.5, CYAN, 0.7, 0.6))
        tg = it.part("tsuba_pierce", "crystal", base=(100, 220, 255), smooth=25)
        for k in range(8):
            a = 2 * math.pi * k / 8
            ml.gem(tg, (3.7 * math.cos(a), Y_T + 0.0, 3.7 * math.sin(a)), 0.5, 1.0, 4, axis=(0, 1, 0))
        tg.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), CYAN, 0.9, 0.7))

    def tsuba_paint(ctx):
        tx.put(ctx.glow, tx.soft(ctx.S - 0.5, 0.12) * 0.5, CYAN, 0.7, 0.5, halo=0.5)
        tx.put(ctx.vein, tx.soft(ctx.S - 0.5, 0.06) * 0.8, (210, 250, 255), 0.8, 0.8)
    ts.painters.append(tsuba_paint)
    hb = it.part("habaki", "silver", base=(70, 200, 255), smooth=40, uv_mode="loft", density=1.3)
    ml.lathe(hb, "y", (0, 0, 0), [(Y_T - 2.6, 1.9), (Y_T - 2.2, 2.4), (Y_T - 0.6, 2.4), (Y_T - 0.4, 1.9)], sides=it.n(14, 6), caps=(False, False), squash=0.55)
    hb.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.55, CYAN, 0.8, 0.7))
    sp = it.part("seppa", "silver", base=(180, 200, 220), smooth=40, uv_mode="loft")
    for y in (Y_T - 1.1, Y_T + 1.1):
        ml.collar(sp, y - 0.2, 2.5, 0.4, lip=0.0, sides=it.n(14, 6), oval=0.62)

    # ---------------------------------------------------------------- the hilt: black samegawa under a diamond wrap of black and cyan cord, menuki, fuchi, a crystal kashira
    tk = it.part("tsuka", "leather", base=(10, 14, 26), smooth=55, uv_mode="loft", density=1.2)
    ml.loft(tk, "y", (0, 0, 0), [(3.0, 1.55, 1.2), (8.0, 1.75, 1.35), (14.0, 1.7, 1.3), (Y_T - 3.4, 1.55, 1.2)], ml.circle(1.0, it.n(16, 6)), caps=(False, False))
    cd = it.part("tsukamaki", "cloth", base=(14, 26, 48), smooth=55, uv_mode="loft", density=1.3)
    cd2 = it.part("tsukamaki_cyan", "cloth", base=(60, 190, 240), smooth=55, uv_mode="loft", density=1.3)
    n_w = it.n(44, 8)
    for ph, part in ((0.0, cd), (math.pi, cd2)):
        for sgn in (1, -1):
            helix = []
            for i in range(n_w + 1):
                t = i / n_w
                a = sgn * 4.0 * 2 * math.pi * t + ph
                y = 3.6 + (Y_T - 7.0) * t
                r = 1.45 + 0.35 * math.sin(math.pi * min(1.0, t * 1.3))
                helix.append((r * math.cos(a) * 1.0, y, r * math.sin(a) * 0.78))
            G.tube(it, part, helix, 0.2, sides=4)
    cd2.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.45, CYAN, 0.7, 0.5))
    if full:
        mn = it.part("menuki", "gold", base=(100, 220, 255), smooth=30)
        for sgn in (1, -1):
            ml.gem(mn, (0.0, 14.0, sgn * 2.4), 0.7, 1.2, 6, axis=(0, 0, sgn))
        mn.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), CYAN, 1.0, 0.8, halo=0.4))
    fu = it.part("fuchi", "silver", base=(30, 60, 90), smooth=40, uv_mode="loft")
    ml.lathe(fu, "y", (0, 0, 0), [(Y_T - 4.0, 1.5), (Y_T - 3.4, 2.1), (Y_T - 2.6, 1.9)], sides=it.n(14, 6), caps=(False, False), squash=0.8)
    ka = it.part("kashira", "crystal", base=(60, 200, 255), smooth=35, uv_mode="loft", density=1.4)
    ml.lathe(ka, "y", (0, 0, 0), [(3.3, 1.5), (2.2, 2.0), (0.8, 1.7), (-0.8, 1.0), (-1.4, 0.0)], sides=it.n(16, 6), caps=(True, False), squash=0.8)

    def ka_paint(ctx):
        n_ = tx.fbm(ctx.shape, 6.0, ctx.seed + 1, 3)
        ctx.img[..., :3] = tx.lerp3((20, 110, 190), (180, 245, 255), np.clip(n_ * 1.4, 0, 1))
        tx.put(ctx.glow, np.ones(ctx.shape, np.float32), CYAN, 1.0, 0.8, halo=0.5)
        tx.put(ctx.vein, tx.caustics(ctx, 5.0, 4, 2.0), (255, 255, 255), 0.9, 0.9)
    ka.painters.append(ka_paint)

    # ---------------------------------------------------------------- the rings of torn space, the afterimages, the halo layer
    it.define_group("rifts", pivot=(0.0, 76.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 48.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.6, "period": 130.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 16})
    if full:
        rf = it.part("rift_rings", "starmetal", group="rifts", base=(20, 50, 90), smooth=45, uv_mode="loft")
        rg = it.part("rift_gems", "crystal", group="rifts", base=(100, 220, 255), smooth=25)
        for k, (y, r) in enumerate(((50.0, 6.2), (78.0, 7.2), (106.0, 5.2))):
            t = (y - Y_T - 1.0) / (Y_TIP - Y_T - 1.0)
            c = np.array([cx(t), y, 0.0])
            G.torus(it, rf, c, r, 0.34, (0.2, 1.0, 0.1), major=36, minor=5)
            for j in range(4):
                a = 2 * math.pi * j / 4 + k
                G.bipyramid(rg, c + np.array([r * math.cos(a), 0.4 * math.sin(a * 2), r * math.sin(a)]), (math.cos(a), 0, math.sin(a)), 1.1, 0.32, sides=4)
        rf.painters.append(lambda ctx: (tx.put(ctx.glow, tx.soft(ctx.S - 0.5, 0.4) * 0.6, CYAN, 0.8, 0.6), tx.put(ctx.vein, np.ones(ctx.shape, np.float32) * 0.5, (210, 250, 255), 0.7, 0.7)))
        rg.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (200, 250, 255), 1.0, 0.9, halo=0.4))
    it.define_group("rift_rings", pivot=(0.0, 76.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": -35.0}, {"kind": "breathe", "min": 0.3, "max": 0.75, "period": 100.0}], tint="6AE0FF",
                    lod_skip=True, alpha=0.85)
    for k, (y, r) in enumerate(((50.0, 8.0), (78.0, 9.5), (106.0, 7.0))):
        t = (y - Y_T - 1.0) / (Y_TIP - Y_T - 1.0)
        G.card(it, "rift_card_%d" % k, "rift_rings", (cx(t), y, 0.0), (1, 0, 0), (0, 0, 1), r, r, "ring")
    it.define_group("afterimage", anim=[{"kind": "flicker", "min": 0.1, "max": 0.7, "rate": 3.0}], tint="6AE0FF", lod_skip=True, alpha=0.6)
    for sgn in (1, -1):
        G.card(it, "afterimage", "afterimage", (cx(0.5) - 2.2, 76.0, sgn * 0.3), (1, 0, 0), (0, 1, 0), 4.5, 52.0, "glow")
    it.define_group("tsuba_disc", pivot=(0.0, Y_T, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 25.0}, {"kind": "breathe", "min": 0.3, "max": 0.75, "period": 90.0}], tint="6AE0FF", lod_skip=True,
                    alpha=0.8, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "tsuba_runes", "tsuba_disc", (0.0, Y_T + 0.3, 0.0), (1, 0, 0), (0, 0, 1), 10.0, 10.0, "runes")
    it.define_group("tear", pivot=(0.0, 76.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 5.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": 60.0}], tint="BFF4FF", lod_skip=True, alpha=0.9)
    for k in range(4):
        a = k * 1.6
        t = (36.0 + 22.0 * k - Y_T - 1.0) / (Y_TIP - Y_T - 1.0)
        G.card(it, "tear_%d" % k, "tear", (cx(t) + 3.6 * math.cos(a), 36.0 + 22.0 * k, 3.6 * math.sin(a)), (math.cos(a + 1.57), 0, math.sin(a + 1.57)), (0, 1, 0), 3.6, 7.5, "bolt")
    it.flare((0.0, 0.0, 1.6), 5.0, "6AE0FF", "star", phase=0.0, pulse=0.7)
    it.flare((cx(1.0), Y_TIP - 2.0, 0.0), 6.5, "BFF4FF", "star", phase=0.4, pulse=0.6)
    it.flare((0.0, 14.0, 2.9), 4.0, "6AE0FF", "sparkle", phase=0.7)

    it.glow = ("FFFFFF", 0.17, 0.45, 1.0)
    it.layers = [
        {"id": "void_flat", "kind": "distortion", "layer": "void", "note": "the dimensional void fills the flat of the blade along its whole length (reality shimmer)"},
        {"id": "rift_rings", "kind": "orbit_rings", "group": "rifts", "note": "three rings of torn space with gems turning round the blade"},
        {"id": "rift_cards", "kind": "halo_ring", "group": "rift_rings", "note": "rings of cyan light on the same heights, counter-turning"},
        {"id": "afterimage", "kind": "light_shafts", "group": "afterimage", "note": "a flickering afterimage of the cut along the blade"},
        {"id": "tsuba_runes", "kind": "halo_ring", "group": "tsuba_disc", "note": "a rune disc turning on the tsuba"},
        {"id": "tears", "kind": "energy_arcs", "group": "tear", "note": "four flickering tears of space round the blade"},
        {"id": "blade_flares", "kind": "lens_flare", "flares": "kashira, point, hilt", "note": "star flares on the kashira, the point and the hilt gem"},
        {"id": "void_motes", "kind": "motes", "particle": "dust", "rgb": "6AE0FF", "where": "blade", "rate": 0.6, "radius": 1.8, "note": "cyan motes falling out of the blade"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "40C8FF", "radius": 1.0, "note": "a ring of cyan motes on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "hamon_flow", "kind": "vein_wave", "note": "light runs along the hamon and the edge"},
        {"id": "rift_orbit", "kind": "group_spin", "group": "rifts"},
        {"id": "ring_counter", "kind": "group_spin", "group": "rift_rings"},
        {"id": "tsuba_turn", "kind": "group_spin", "group": "tsuba_disc"},
        {"id": "afterimage_flicker", "kind": "group_flicker", "group": "afterimage"},
        {"id": "tear_flicker", "kind": "group_flicker", "group": "tear"},
        {"id": "ring_breath", "kind": "group_breathe", "group": "rift_rings"},
    ]
    it.draw = {"ticks": 14, "flash": "6AE0FF", "groups": ["rifts", "tsuba_disc"], "note": "a tear of space opens round the blade as it is drawn; the rings spiral in"}
    it.swing = {"trail": "dimension_cut", "rgb": "6AE0FF", "rgb2": "101828", "length": 2.0, "width": 0.4, "life": 12, "particles": "void_mote"}
    it.sound = "nusmp:item.genesis.void_origin.absence_loop"
    it.acts = {
        "dimension_cut": {"burst": "dimension_cut", "pattern": "line", "rgb": "6AE0FF", "ring": 7.0, "particles": 40, "sound": "nusmp:item.genesis.void_origin.cut"},
        "void_step": {"burst": "void_step", "pattern": "inward", "rgb": "40C8FF", "ring": 4.0, "particles": 36, "sound": "nusmp:item.genesis.void_origin.step"},
        "world_sever": {"burst": "world_sever", "pattern": "nova", "rgb": "BFF4FF", "ring": 7.0, "particles": 50, "sound": "nusmp:item.genesis.void_origin.sever"},
    }

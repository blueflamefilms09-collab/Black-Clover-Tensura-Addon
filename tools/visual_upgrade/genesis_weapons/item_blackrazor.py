"""Blackrazor (D&D legend, Genesis): a sword of black glass that is hungry. What it kills it keeps, and what it keeps it gives back to whoever dares to hold it.

Lore the model follows (the RelicDef text and docs/new_weapons_197.md): a black glass greatsword with a wide smooth blade and violet inner light, a skull-eye guard
and a chain-wrapped grip, a violet eye for a pommel; the dimensional void shows through three rifts of the blade (as on the Genesis Demon-Slayer).
Palette: 0C0A12 (glass), 5A2A8A (inner light), 9A4AFF (soul violet).
"""
import math

import numpy as np

import vu_geo as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (24.0, 56.0)
VIOLET = (154, 74, 255)
LILAC = (206, 150, 255)

Y_GRIP0, Y_GUARD, Y_BLADE0, Y_TIP = 2.9, 36.0, 40.0, 150.0


def hw(t):
    # a broad leaf: 6.3 at the ricasso, belly to 7.0, a long taper
    return 6.3 + 0.8 * math.sin(math.pi * min(1.0, t / 0.45)) * (1 - t) ** 0.5 - 2.6 * max(0.0, t - 0.55) ** 1.6 * 2.1


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- blade: black glass, fuller, serrated spine
    bl = it.part("blade", "obsidian", base=(16, 12, 26), smooth=40, density=1.15, wear=0.55)

    def spine(t):
        saw = 0.0
        if 0.16 < t < 0.78 and full:
            k = (t - 0.16) / 0.62 * 17.0
            saw = 0.55 * (k % 1.0) * (1.0 if int(k) % 1 == 0 else 1.0)
        return hw(t) + saw

    def edge_wave(t):
        return 0.12 * math.sin(t * 40.0) if full and 0.1 < t < 0.9 else 0.0
    G.blade_by(it, bl, Y_BLADE0, Y_TIP, hw, lambda t: 0.78 - 0.30 * t, n=66, bevel=0.5, groove=0.42 if full else 0.0, tip=0.13, edge=edge_wave, hw_spine=spine)

    # the void rifts (layer 1): jagged strips on both faces, the dimensional void of the Genesis Demon-Slayer shows through
    rng = np.random.default_rng(11)
    for i, (a, b, w_) in enumerate(((54.0, 74.0, 1.15), (88.0, 106.0, 1.3), (118.0, 134.0, 1.0))):
        rift = it.part("void_rift_%d" % i, "steel", layer=1, group="")
        for sgn in (1, -1):
            ys = np.linspace(a, b, 8 if full else 3)
            jag = [float(rng.uniform(-0.55, 0.55)) for _ in ys]
            wd = [w_ * (0.35 + 0.65 * math.sin(math.pi * k / (len(ys) - 1))) for k in range(len(ys))]
            zz = sgn * (0.80 - 0.30 * ((a + b) / 2 - Y_BLADE0) / (Y_TIP - Y_BLADE0) + 0.06)
            ia = [rift.add_vertex((jag[k] - wd[k] - 0.4, ys[k], zz)) for k in range(len(ys))]
            ib = [rift.add_vertex((jag[k] + wd[k] - 0.4, ys[k], zz)) for k in range(len(ys))]
            for k in range(len(ys) - 1):
                if sgn > 0:
                    rift.add_face(ia[k], ib[k], ib[k + 1], ia[k + 1])
                else:
                    rift.add_face(ia[k], ia[k + 1], ib[k + 1], ib[k])

    def blade_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            if ctx.kind == "dom" and ctx.axis == 0:                         # the edge / spine bevel strip: lit steel-black
                ctx.img[..., :3] *= 1.18
            return
        X, Y = ctx.X, ctx.Y
        t = np.clip((Y - Y_BLADE0) / (Y_TIP - Y_BLADE0), 0, 1)
        half = np.maximum(6.3 + 0.8 * np.sin(np.pi * np.minimum(1.0, t / 0.45)) * (1 - t) ** 0.5 - 2.6 * np.maximum(0.0, t - 0.55) ** 1.6 * 2.1, 0.3)
        u = X / half
        inner = np.abs(u) < 0.42
        # inner violet light: a caustic web in the fuller, souls drifting in the glass
        web = tx.caustics(ctx, 26.0, 3, 2.4)
        web2 = tx.caustics(ctx, 11.0, 4, 3.2)
        fine = tx.fbm(ctx.shape, 20.0, ctx.seed + 8, 4)
        souls = np.clip((fine - 0.58) * 3.2, 0, 1)
        deep = tx.soft(u, 1.0)
        veins = np.clip(web * 0.8 + web2 * 0.55 + souls * 0.6, 0, 1) * (0.25 + 0.75 * inner) * ctx.cover()
        ctx.img[..., :3] = ctx.img[..., :3] * (1 - 0.35 * deep[..., None] * (0.3 + 0.7 * inner[..., None])) + np.array([62, 24, 110], np.float32) * (0.25 * veins * deep)[..., None]
        tx.put(ctx.vein, veins * 0.9, VIOLET, 1.0, 0.45, halo=1.3)
        tx.put(ctx.glow, np.clip(souls * 0.5, 0, 1) * deep, (110, 40, 200), 0.6, 0.3, halo=1.0)
        # a bright honed edge on the -x side and a dark spine
        edge = tx.soft(u + 1.0, 0.07)
        ctx.img[..., :3] += (np.array([220, 200, 255], np.float32)[None, None, :] - ctx.img[..., :3]) * (0.55 * edge)[..., None]
        tx.put(ctx.glow, edge * 0.8, LILAC, 0.9, 0.5, halo=1.8)
        # engraved runes down the fuller (the lore: what it kills it keeps), glowing
        glyphs = tx.rune_glyphs(14, 5 + int(ctx.sign) + 3)
        m = np.zeros(ctx.shape, np.float32)
        for i, g in enumerate(glyphs):
            m = np.maximum(m, tx.mask_text(ctx, g, 0.0 if ctx.sign >= 0 else 0.0, 52.0 + i * 6.4, 1.15, rot=90.0, fname="DejaVuSans.ttf", spacing=1.0))
        tx.engrave(ctx, m * inner, 0.65)
        tx.put(ctx.glow, m * inner, LILAC, 0.95, 0.55, halo=0.9)
    bl.painters.append(blade_paint)

    # ---------------------------------------------------------------- the guard: a horned bar with a skull at its heart
    gd = it.part("guard_horns", "iron", base=(40, 30, 58), smooth=38, density=1.1)
    pts = []
    for x in np.linspace(-13.0, 13.0, 15):
        pts.append((x, Y_GUARD + 0.028 * x * x, 0.0))
    left = [(-13.0, Y_GUARD + 4.7, 0.0), (-16.5, Y_GUARD + 8.0, 0.0), (-18.8, Y_GUARD + 13.6, 0.0), (-19.2, Y_GUARD + 19.5, 0.0)]
    right = [(13.0, Y_GUARD + 4.7, 0.0), (16.5, Y_GUARD + 8.0, 0.0), (18.8, Y_GUARD + 13.6, 0.0), (19.2, Y_GUARD + 19.5, 0.0)]
    path = G.catmull(left[::-1] + [(-8.0, Y_GUARD + 1.6, 0.0), (0.0, Y_GUARD, 0.0), (8.0, Y_GUARD + 1.6, 0.0)] + right, it.n(5, 2))
    ml.sweep(gd, [tuple(q) for q in path], ml.octagon(1.0, 1.0, 0.22),
             scales=lambda t: (max(0.12, 1.9 - 0.9 * abs(2 * t - 1) ** 1.5) * (0.35 if (t < 0.04 or t > 0.96) else 1.0), 1.55 - 0.5 * abs(2 * t - 1)), up=(0, 0, 1))
    # the guard plates: layered shields over the root of the blade, front and back
    pl = it.part("guard_plates", "iron", base=(48, 36, 70), smooth=30, density=1.1)
    for sgn in (1, -1):
        for k, (w_, h_, y_) in enumerate(((7.4, 3.2, Y_GUARD + 3.4), (5.6, 2.6, Y_GUARD + 6.2), (3.6, 2.0, Y_GUARD + 8.6))):
            ml.loft(pl, "y", (0, y_, sgn * (1.5 + 0.35 * k)), [(-h_ / 2, 1.0, 1.0), (h_ / 2, 1.0, 1.0)], ml.octagon(w_, 1.0 + 0.15 * k, 0.35))
    if full:
        sp = it.part("guard_teeth", "bone", base=(210, 200, 222), smooth=30)
        for sgn in (-1, 1):
            for k in range(5):
                x = sgn * (6.2 + 2.2 * k)
                y = Y_GUARD + 0.028 * x * x + 0.9
                G.cone(sp, (x, y, 0.0), (x + sgn * 0.5, y + 3.0 + 0.4 * k, 0.0), 0.55, 5)
        # the side flanges: layered blades below the guard bar (feather fan, the black wings)
        wg = it.part("guard_wings", "obsidian", base=(24, 16, 40), smooth=36)
        for sgn in (-1, 1):
            G.feather_fan(it, wg, (sgn * 5.0, Y_GUARD - 1.2, 0.0), (sgn * 0.7, -0.7, 0.0), 70.0, 5, 9.0, 1.6, thick=0.35, curl=0.0, side=(0, 0, 1), swing_axis=(0, 0, 1), n_st=5)

    # the skull: cranium, brow, jaw, teeth, sockets and the violet eyes (both faces)
    sk = it.part("skull", "bone", base=(228, 220, 240), smooth=42, wear=0.8)
    G.orb(it, sk, (0.0, Y_GUARD + 3.9, 0.0), 3.5, 4.0, 2.7, sides=12, stacks=8)
    jaw = it.part("skull_jaw", "bone", base=(214, 206, 230), smooth=40)
    G.orb(it, jaw, (0.0, Y_GUARD + 0.9, 0.7), 2.4, 1.6, 2.0, sides=10, stacks=6)
    if full:
        tp = it.part("skull_teeth", "bone", base=(240, 236, 250), smooth=25)
        for sgn in (1, -1):
            for k in range(6):
                x = -1.75 + 0.7 * k
                ml.box(tp, (x - 0.22, Y_GUARD - 0.3, sgn * 2.5 - 0.15), (x + 0.22, Y_GUARD + 1.1, sgn * 2.5 + 0.15))
        brow = it.part("skull_brow", "bone", base=(220, 212, 236), smooth=30)
        for sgn in (1, -1):
            ml.bar(brow, (-3.3, Y_GUARD + 5.3, sgn * 2.45), (3.3, Y_GUARD + 5.3, sgn * 2.45), 0.7, 0.3, up=(0, 0, sgn), taper=1.0)
            G.cone(brow, (0.0, Y_GUARD + 3.6, sgn * 2.55), (0.0, Y_GUARD + 2.3, sgn * 2.9), 0.55, 4)
    eyes = it.part("skull_eyes", "crystal", base=(190, 100, 255), smooth=30)
    for sgn in (1, -1):
        for sx in (-1, 1):
            ml.gem(eyes, (sx * 1.35, Y_GUARD + 4.2, sgn * 2.3), 0.62, 1.25, 4 if not full else 6, axis="z" if sgn > 0 else "-z")

    def eyes_paint(ctx):
        tx.put(ctx.glow, np.ones(ctx.shape, np.float32), VIOLET, 1.0, 0.7)
        tx.put(ctx.vein, np.ones(ctx.shape, np.float32) * 0.6, LILAC, 0.8, 0.8)
    eyes.painters.append(eyes_paint)

    def skull_paint(ctx):
        crack = tx.caustics(ctx, 30.0, 6, 3.0)
        ctx.img[..., :3] *= (1.0 - 0.45 * crack[..., None])
        tx.tint(ctx, (225, 215, 245), 0.4)
    sk.painters.append(skull_paint)
    if full:
        # horns of the skull, curling back over the guard
        hn = it.part("skull_horns", "bone", base=(200, 190, 215), smooth=40)
        for sx in (-1, 1):
            G.tube(it, hn, [(sx * 2.4, Y_GUARD + 6.2, 0.0), (sx * 4.0, Y_GUARD + 9.5, 0.0), (sx * 5.2, Y_GUARD + 14.0, 0.0), (sx * 4.6, Y_GUARD + 18.2, 0.0)], 0.85, sides=6, tip=True)

    # ---------------------------------------------------------------- grip: leather under a spiralling chain
    gp = it.part("grip", "leather", base=(26, 22, 34), smooth=50)
    G.rod(it, gp, Y_GRIP0 + 2.4, Y_GUARD - 1.6, 1.6, 1.8, sides=10, swell=[(0.5, 1.12)])
    ch = it.part("grip_chain", "iron", base=(120, 120, 140), smooth=50, uv_mode="loft", wear=0.8, density=1.2)
    turns, r0 = 2.6, 2.15
    helix = []
    for i in range(it.n(22, 8) + 1):
        t = i / it.n(22, 8)
        a = turns * 2 * math.pi * t
        helix.append((r0 * math.cos(a), Y_GRIP0 + 4.0 + (Y_GUARD - 3.5 - Y_GRIP0 - 4.0) * t, r0 * math.sin(a)))
    G.chain(it, ch, helix, link_len=2.1, link_w=1.25, tube_r=0.24, sides=4, up=(0, 1, 0))
    cl = it.part("grip_collars", "gold", base=(120, 90, 160), smooth=40)
    ml.collar(cl, Y_GUARD - 1.9, 2.5, 1.6, lip=0.35, sides=it.n(14, 6))
    ml.collar(cl, Y_GRIP0 + 2.0, 2.4, 1.6, lip=0.35, sides=it.n(14, 6))
    # ---------------------------------------------------------------- pommel: a violet eye in a spiked socket
    pm_ = it.part("pommel_socket", "iron", base=(38, 28, 54), smooth=45, density=1.2)
    G.orb(it, pm_, (0.0, Y_GRIP0 + 3.1, 0.0), 3.3, 3.1, 3.0, sides=14, stacks=9)
    if full:
        sr = it.part("pommel_spikes", "iron", base=(60, 46, 84), smooth=30)
        G.spike_ring(it, sr, (0.0, Y_GRIP0 + 3.1, 0.0), (0, 1, 0), 8, 2.6, 3.4, 0.0, 0.5, 4)
        for k in range(8):
            a = 2 * math.pi * k / 8 + 0.3
            G.cone(sr, (3.1 * math.cos(a), Y_GRIP0 + 3.1, 3.1 * math.sin(a)), (4.6 * math.cos(a), Y_GRIP0 + 2.2, 4.6 * math.sin(a)), 0.5, 4)
    iris = it.part("pommel_eye", "crystal", base=(130, 60, 230), smooth=35, density=1.3)
    for sgn in (1, -1):
        ml.lathe(iris, "z" if sgn > 0 else "-z", (0.0, Y_GRIP0 + 3.1, sgn * 2.0), [(0.0, 2.3), (0.45, 2.0), (0.9, 1.1), (1.15, 0.0)], sides=it.n(16, 6), caps=(False, False))

    def eye_paint(ctx):
        if ctx.kind != "dom":
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32), VIOLET, 0.9, 0.5)
            return
        X, Y = ctx.X, ctx.Y - (Y_GRIP0 + 3.1)
        r = np.hypot(X, Y)
        slit = np.exp(-(X / 0.28) ** 2) * (np.abs(Y) < 1.7)
        ring = tx.soft(r - 1.55, 0.14)
        ctx.img[..., :3] = ctx.img[..., :3] * 0.5 + np.array([110, 40, 200], np.float32) * 0.5
        ctx.img[..., :3] *= (1 - 0.9 * slit)[..., None]
        tx.put(ctx.glow, np.clip(1.0 - r / 2.4, 0, 1) * (1 - 0.8 * slit) + ring * 0.6, VIOLET, 1.0, 0.7, halo=1.0)
        tx.put(ctx.vein, ring, LILAC, 0.9, 0.8)
    iris.painters.append(eye_paint)
    pcap = it.part("pommel_cap", "gold", base=(120, 90, 160), smooth=40)
    ml.lathe(pcap, "y", (0.0, Y_GRIP0, 0.0), [(0.0, 0.0), (0.5, 1.3), (1.2, 1.6)], sides=it.n(10, 5), caps=(False, False))

    # ---------------------------------------------------------------- spine thorns, rune plates
    if full:
        th = it.part("spine_thorns", "obsidian", base=(20, 14, 34), smooth=35)
        for k in range(9):
            t = 0.16 + k * 0.075
            y = Y_BLADE0 + (Y_TIP - Y_BLADE0) * t
            x = hw(t) + 0.3
            G.cone(th, (x - 0.4, y, 0.0), (x + 2.4, y + 4.8 - 0.25 * k, 0.0), 0.75, 5)
        rp = it.part("ricasso_plates", "iron", base=(52, 40, 76), smooth=35)
        for sgn in (1, -1):
            for sx in (-1, 1):
                ml.loft(rp, "y", (sx * 3.6, 0, sgn * 0.9), [(Y_BLADE0 - 0.5, 1.0, 1.0), (Y_BLADE0 + 12.0, 0.7, 1.0)], ml.octagon(1.4, 0.4, 0.1))

    # ---------------------------------------------------------------- the group of orbiting soul wisps and the shard ring
    it.define_group("wisps", pivot=(0, 94, 0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 38.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.6, "period": 90.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 4, "ticks": 14})
    if full:
        wp = it.part("soul_wisps", "crystal", group="wisps", base=(180, 130, 255), smooth=40)
        for k in range(8):
            a = 2 * math.pi * k / 8
            r = 10.0 + 1.6 * (k % 3)
            y = 56.0 + k * 11.0
            cx, cz = r * math.cos(a), r * math.sin(a)
            ml.lathe(wp, "y", (cx, y, cz), [(-3.4, 0.0), (-1.7, 0.55), (-0.3, 1.1), (1.0, 0.85), (2.0, 0.0)], sides=8, caps=(False, False))
        sh = it.part("glass_shards", "obsidian", group="wisps", base=(30, 20, 54), smooth=20)
        for k in range(10):
            a = 2 * math.pi * k / 10 + 0.31
            r = 14.5
            y = 64.0 + k * 8.2
            G.cone(sh, (r * math.cos(a), y - 2.2, r * math.sin(a)), (r * math.cos(a), y + 2.2, r * math.sin(a)), 0.7, 4)
            G.cone(sh, (r * math.cos(a), y + 2.2, r * math.sin(a)), (r * math.cos(a), y + 4.4, r * math.sin(a)), 0.7, 4)

        def wisp_paint(ctx):
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * (0.5 + 0.5 * ctx.T), LILAC, 0.9, 0.6, halo=0.5)
        wp.painters.append(wisp_paint)

    # ---------------------------------------------------------------- the halo layer: aura shafts, the rune disc, arcs, flares
    it.define_group("aura", anim=[{"kind": "breathe", "min": 0.25, "max": 0.7, "period": 70.0}], tint="8A3CFF", lod_skip=True, alpha=0.8)
    G.card(it, "aura_xy", "aura", (-0.5, 96.0, 0.0), (1, 0, 0), (0, 1, 0), 13.0, 62.0, "shaft")
    G.card(it, "aura_xy", "aura", (-0.5, 96.0, 0.0), (1, 0, 0), (0, -1, 0), 11.0, 60.0, "shaft")
    G.card(it, "aura_zy", "aura", (-0.5, 96.0, 0.0), (0, 0, 1), (0, 1, 0), 11.0, 60.0, "glow")
    it.define_group("rune_disc", pivot=(0, 38, 0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": -22.0}, {"kind": "breathe", "min": 0.35, "max": 0.85, "period": 120.0}],
                    tint="B070FF", lod_skip=True, alpha=0.9, draw={"from_scale": 0.0, "delay": 2, "ticks": 12})
    G.card(it, "rune_disc_card", "rune_disc", (0.0, 38.0, -0.6), (1, 0, 0), (0, 1, 0), 21.0, 21.0, "runes")
    G.card(it, "rune_disc_card_b", "rune_disc", (0.0, 38.0, 0.6), (1, 0, 0), (0, 1, 0), 15.0, 15.0, "hex")
    it.define_group("arcs", pivot=(0, 94, 0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 5.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": 55.0}], tint="C890FF", lod_skip=True,
                    alpha=0.95)
    for k in range(4):
        y = 66.0 + k * 20.0
        a = k * 1.7
        G.card(it, "arc_%d" % k, "arcs", (4.2 * math.cos(a), y, 4.2 * math.sin(a)), (math.cos(a + 1.5), 0, math.sin(a + 1.5)), (0, 1, 0), 5.5, 10.0, "bolt")
    for sx in (-1, 1):
        it.flare((sx * 1.35, Y_GUARD + 4.2, 3.0), 4.2, "B070FF", "star", phase=0.5 * sx)
        it.flare((sx * 1.35, Y_GUARD + 4.2, -3.0), 4.2, "B070FF", "star", phase=0.5 * sx + 0.5)
    it.flare((0.0, Y_GRIP0 + 3.1, 3.2), 5.5, "9A4AFF", "star", phase=0.2)
    it.flare((0.0, Y_GRIP0 + 3.1, -3.2), 5.5, "9A4AFF", "star", phase=0.7)
    it.flare((-1.0, Y_TIP - 3.0, 0.0), 6.0, "C890FF", "sparkle", phase=0.0, pulse=0.5)

    it.glow = ("FFFFFF", 0.17, 0.45, 1.0)
    it.layers = [
        {"id": "soul_wisps", "kind": "orbit_shards", "group": "wisps", "note": "eight teardrops of captured soul and ten glass shards circling the blade"},
        {"id": "rune_disc", "kind": "halo_ring", "group": "rune_disc", "note": "a rune circle and a hexagon turning behind the skull guard"},
        {"id": "void_rifts", "kind": "distortion", "layer": "void", "note": "three rifts of the dimensional void through the glass (reality shimmer)"},
        {"id": "blade_aura", "kind": "light_shafts", "group": "aura", "note": "crossed shafts of violet light along the whole blade, breathing"},
        {"id": "soul_arcs", "kind": "energy_arcs", "group": "arcs", "note": "four flickering bolts round the blade"},
        {"id": "eye_flares", "kind": "lens_flare", "flares": "eyes", "note": "star flares in the skull eyes and the pommel eye"},
        {"id": "soul_motes", "kind": "motes", "particle": "dust", "rgb": "9A4AFF", "where": "blade", "rate": 0.5, "note": "violet motes rising off the blade"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "5A2A8A", "radius": 0.9, "note": "a ring of dark violet motes round the feet"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse", "note": "core glow breathes 0.45 to 1.0"},
        {"id": "vein_flow", "kind": "vein_wave", "speed": 0.35, "freq": 0.06, "note": "violet light flows up the blade's veins"},
        {"id": "wisp_orbit", "kind": "group_spin", "group": "wisps", "note": "souls and shards orbit and bob"},
        {"id": "rune_turn", "kind": "group_spin", "group": "rune_disc", "note": "the rune disc turns against the souls"},
        {"id": "arc_flicker", "kind": "group_flicker", "group": "arcs"},
        {"id": "aura_breath", "kind": "group_breathe", "group": "aura"},
        {"id": "eye_pulse", "kind": "flare_pulse"},
    ]
    it.draw = {"ticks": 16, "flash": "9A4AFF", "groups": ["wisps", "rune_disc"], "note": "the souls spiral out of the blade and the rune disc unfolds as it is drawn"}
    it.swing = {"trail": "violet_glass", "rgb": "9A4AFF", "rgb2": "0C0A12", "length": 1.35, "width": 0.34, "life": 9, "particles": "soul_wisp"}
    it.sound = "nusmp:item.genesis.blackrazor.hunger_loop"
    it.acts = {
        "devour_soul": {"burst": "soul_drain", "rgb": "9A4AFF", "ring": 3.0, "particles": 26, "sound": "nusmp:item.genesis.blackrazor.devour"},
        "void_cleave": {"burst": "void_rift_slash", "rgb": "5A2A8A", "ring": 4.5, "particles": 34, "sound": "nusmp:item.genesis.blackrazor.void_cleave"},
        "hunger_eternal": {"burst": "souls_rise", "rgb": "C890FF", "ring": 5.5, "particles": 40, "sound": "nusmp:item.genesis.blackrazor.hunger_eternal"},
    }

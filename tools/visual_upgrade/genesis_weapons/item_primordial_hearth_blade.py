"""Primordial Hearth Blade (Wave 71, Genesis, DanMachi): a black knife with a hearth-fire in its blade, blessed by a goddess who watched it grow. It is only ever as strong as the one
holding it needs it to be.

A short black dagger whose blade is cut like a flame and carries the goddess's blessing as lines of gold script; a hearth orb burning in a gold ring under the guard, two
ribbons of the goddess flowing from it, a red-wrapped grip, a flame finial for a pommel, tongues of fire that stand on the blade and embers that circle it. Palette: FFB04A gold fire,
3A2A1A black, FFD070 bright gold.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (6.0, 54.0)
Y_G = 14.0
Y_TIP = 54.0
HY = Y_G + 1.6
GOLD = (255, 176, 74)
BRIGHT = (255, 208, 112)
EMBER = (255, 120, 40)


def hw(t):
    return 2.6 * (1.0 - 0.45 * t) + 0.35 * math.sin(t * 28.0) * (1.0 - t)


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- the blade: black, cut like a flame, gold script and a flame fuller
    bl = it.part("blade", "obsidian", base=(24, 20, 24), smooth=38, density=1.4, wear=0.7)

    def edge(t):
        return 0.55 * math.sin(t * 36.0 + 1.0) * (1.0 - t) ** 0.7 if full else 0.0
    G.blade_by(it, bl, Y_G + 4.4, Y_TIP, lambda t: 3.7 * (1.0 - 0.5 * t ** 1.2), lambda t: 0.6 * (1.0 - 0.4 * t), n=92, bevel=0.5, groove=0.4 if full else 0.0, tip=0.16, edge=edge)

    def blade_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            ctx.img[..., :3] *= 1.15
            return
        X, Y = ctx.X, ctx.Y
        t = np.clip((Y - Y_G - 4.4) / (Y_TIP - Y_G - 4.4), 0, 1)
        half = np.maximum(3.7 * (1.0 - 0.5 * t ** 1.2), 0.2)
        u = X / half
        # a flame in the fuller: licks of gold rising, brighter toward the point
        lick = 0.5 + 0.5 * np.sin(Y * 1.1 + 2.2 * np.sin(Y * 0.37 + u * 2.0))
        flame = np.clip(1.0 - np.abs(u) / (0.42 + 0.25 * lick), 0, 1) * (0.35 + 0.65 * t ** 0.6)
        ctx.img[..., :3] = ctx.img[..., :3] * (1 - 0.55 * flame[..., None]) + np.array([255, 150, 50], np.float32) * (0.55 * flame)[..., None]
        tx.put(ctx.glow, flame, GOLD, 0.95, 0.6, halo=0.9)
        tx.put(ctx.vein, np.clip(flame * lick, 0, 1), (255, 236, 170), 0.9, 0.8)
        # the goddess's blessing: lines of divine script engraved down both faces
        m = np.zeros(ctx.shape, np.float32)
        glyphs = tx.rune_glyphs(5, 17 + int(ctx.sign))
        for i, g in enumerate(glyphs):
            for sx in (-1, 1):
                m = np.maximum(m, tx.mask_text(ctx, g, sx * 2.1, Y_G + 9.0 + i * 7.2, 0.8, rot=90.0, fname="DejaVuSans.ttf"))
        outside = (np.abs(u) > 0.55).astype(np.float32)
        tx.engrave(ctx, m * outside, 0.7)
        tx.put(ctx.glow, m * outside, BRIGHT, 0.9, 0.6)
        edge_m = np.maximum(tx.soft(u - 1.0, 0.1), tx.soft(u + 1.0, 0.1))
        ctx.img[..., :3] += (np.array([255, 220, 160], np.float32) - ctx.img[..., :3]) * (0.45 * edge_m)[..., None]
        tx.put(ctx.glow, edge_m * 0.6, BRIGHT, 0.8, 0.6, halo=0.8)
    bl.painters.append(blade_paint)

    # ---------------------------------------------------------------- the guard: flame horns, a ring and the hearth orb
    gd = it.part("guard", "gold", base=(240, 180, 80), smooth=38, density=1.4, wear=0.6)
    pts = [(-8.0, Y_G + 3.8, 0.0), (-6.2, Y_G + 0.2, 0.0), (-3.0, Y_G - 1.6, 0.0), (0.0, Y_G - 2.0, 0.0), (3.0, Y_G - 1.6, 0.0), (6.2, Y_G + 0.2, 0.0), (8.0, Y_G + 3.8, 0.0)]
    path = G.catmull(pts, it.n(6, 2))
    ml.sweep(gd, [tuple(q) for q in path], ml.octagon(1.0, 1.0, 0.25), scales=lambda t: (max(0.25, 1.3 - 0.9 * abs(2 * t - 1) ** 2.0 + 0.1), 0.95 - 0.35 * abs(2 * t - 1)), up=(0, 0, 1))
    if full:
        fl = it.part("guard_flames", "gold", base=(255, 196, 96), smooth=30)
        for sx in (-1, 1):
            for k in range(3):
                x = sx * (4.4 + 1.6 * k)
                G.feather(it, fl, (x, Y_G + 0.6 + 0.7 * k, 0.0), (sx * 0.3, 1.0, 0.0), 4.2 + 1.4 * (2 - k), 0.9, thick=0.4, curl=sx * 0.12, side=(0, 0, 1), n=5)
    ring = it.part("hearth_ring", "gold", base=(250, 196, 100), smooth=45, uv_mode="loft", density=1.3)
    G.torus(it, ring, (0.0, HY, 0.0), 3.5, 0.55, (0, 0, 1), major=28, minor=6)
    G.torus(it, ring, (0.0, HY, 0.0), 3.5, 0.55, (1, 0, 0), major=28, minor=6)
    orb = it.part("hearth_orb", "crystal", base=(255, 150, 50), smooth=45, uv_mode="loft", density=1.5)
    G.orb(it, orb, (0.0, HY, 0.0), 2.3, sides=14, stacks=9)

    def orb_paint(ctx):
        n = tx.fbm(ctx.shape, 8.0, ctx.seed + 2, 4)
        ctx.img[..., :3] = tx.lerp3((200, 60, 10), (255, 230, 150), np.clip(n * 1.5 - 0.1, 0, 1))
        tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (255, 190, 90), 1.0, 0.8, halo=0.5)
        tx.put(ctx.vein, np.clip(n * 1.4, 0, 1), (255, 240, 190), 0.9, 0.9)
    orb.painters.append(orb_paint)

    # ---------------------------------------------------------------- the grip: black leather under a red wrap, gold bands, a flame finial
    gp = it.part("grip", "leather", base=(40, 24, 24), smooth=50, uv_mode="loft")
    G.rod(it, gp, 3.2, Y_G - 2.4, 1.45, 1.6, sides=12, swell=[(0.5, 1.15)])
    wr = it.part("grip_wrap", "cloth", base=(200, 40, 34), smooth=55, uv_mode="loft", density=1.3)
    n_w = it.n(44, 8)
    helix = [(1.7 * math.cos(5.5 * 2 * math.pi * i / n_w), 4.0 + (Y_G - 6.4) * i / n_w, 1.7 * math.sin(5.5 * 2 * math.pi * i / n_w)) for i in range(n_w + 1)]
    G.tube(it, wr, helix, 0.34, sides=5)
    wr.painters.append(lambda ctx: (ctx.img.__setitem__((Ellipsis, slice(0, 3)), ctx.img[..., :3] * (0.7 + 0.5 * (0.5 + 0.5 * np.sin(ctx.B * 8.0 + ctx.S * 16.0))[..., None])), tx.put(ctx.glow, tx.soft(ctx.S - 0.5, 0.2) * 0.2, (255, 90, 60), 0.4, 0.3)))
    gb = it.part("grip_bands", "gold", base=(250, 200, 110), smooth=40, uv_mode="loft")
    for y in (3.4, 7.4, 11.0):
        ml.collar(gb, y, 1.95, 0.9, lip=0.25 if full else 0.0, sides=it.n(14, 6))
    pm_ = it.part("pommel", "gold", base=(250, 190, 100), smooth=40, uv_mode="loft", density=1.3)
    ml.lathe(pm_, "y", (0, 0, 0), [(-1.2, 0.0), (-0.4, 1.3), (1.0, 2.3), (2.2, 2.0), (3.2, 1.6)], sides=it.n(14, 6), caps=(False, False))
    if full:
        ff = it.part("pommel_flame", "gold", base=(255, 210, 120), smooth=30)
        for k in range(5):
            a = 2 * math.pi * k / 5
            d = np.array([math.cos(a) * 0.35, -1.0, math.sin(a) * 0.35])
            G.feather(it, ff, (1.2 * math.cos(a), 0.6, 1.2 * math.sin(a)), d, 3.6, 0.9, thick=0.35, curl=0.0, side=(0, 0, 1), n=4)

    # ---------------------------------------------------------------- the ribbons of the goddess (they stream behind the guard) and the embers
    it.define_group("ribbons", pivot=(0.0, Y_G - 1.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 7.0, "period": 60.0}, {"kind": "sway", "axis": [1, 0, 0], "deg": 5.0, "period": 90.0, "phase": 0.3}],
                    lod_skip=False)
    rb = it.part("ribbons", "cloth", group="ribbons", base=(250, 190, 90), smooth=40, uv_mode="loft")
    for k, sx in enumerate((-1, 1)):
        pts = [(sx * 3.4, Y_G - 1.6, 0.0), (sx * 5.2, Y_G - 6.0, 1.4), (sx * 7.0, Y_G - 11.0, 0.2), (sx * 7.8, Y_G - 16.5, -1.6), (sx * 7.2, Y_G - 22.0, -0.6), (sx * 5.8, Y_G - 27.0, 1.0)]
        path = G.catmull(pts, it.n(4, 2))
        ml.sweep(rb, [tuple(q) for q in path], ml.rect_ring(1.0, 1.0), scales=lambda t: (1.1 - 0.4 * t, 0.07), up=(0, 0, 1), caps=(False, False))

    def ribbon_paint(ctx):
        stripe = 0.5 + 0.5 * np.sin(ctx.B * 1.6)
        ctx.img[..., :3] = tx.lerp3((200, 70, 40), (255, 220, 140), stripe) * 1.0
        tx.put(ctx.glow, np.clip(1.0 - ctx.T, 0, 1) * 0.35, GOLD, 0.5, 0.4)
    rb.painters.append(ribbon_paint)
    it.define_group("embers", pivot=(0.0, 34.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 55.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.2, "period": 60.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 12})
    if full:
        em = it.part("embers", "crystal", group="embers", base=(255, 150, 60), smooth=40, uv_mode="loft")
        for k in range(7):
            a = 2 * math.pi * k / 7
            r = 6.5 + 1.0 * (k % 2)
            G.teardrop(it, em, (r * math.cos(a), 22.0 + 5.0 * k, r * math.sin(a)), (0, -1, 0), 2.6, 0.55, sides=6)
        em.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * (0.5 + 0.5 * ctx.T), (255, 180, 70), 1.0, 0.7, halo=0.4))

    # ---------------------------------------------------------------- the halo layer: tongues of fire on the blade, a hearth halo, a rune ring
    it.define_group("tongues", pivot=(0.0, 36.0, 0.0), anim=[{"kind": "flicker", "min": 0.35, "max": 1.0, "rate": 7.0}, {"kind": "pulse", "amp": 0.08, "period": 18.0}], tint="FFA030", lod_skip=True, alpha=0.9)
    for k, (y, h) in enumerate(((24.0, 6.0), (31.0, 7.0), (39.0, 7.5), (46.0, 6.0))):
        G.card(it, "tongue_%d" % k, "tongues", (0.0, y + h * 0.5, 0.0), (1, 0, 0), (0, 1, 0), 2.4, h, "flame")
        G.card(it, "tongue_%d" % k, "tongues", (0.0, y + h * 0.5, 0.0), (0, 0, 1), (0, 1, 0), 2.4, h, "flame")
    it.define_group("hearth_glow", pivot=(0.0, HY, 0.0), anim=[{"kind": "breathe", "min": 0.3, "max": 0.85, "period": 50.0}], tint="FFA030", lod_skip=True, alpha=0.8)
    G.card(it, "hearth_glow", "hearth_glow", (0.0, HY, 0.0), (1, 0, 0), (0, 1, 0), 9.0, 9.0, "glow")
    G.card(it, "hearth_glow", "hearth_glow", (0.0, HY, 0.0), (0, 0, 1), (0, 1, 0), 9.0, 9.0, "glow")
    it.define_group("falna", pivot=(0.0, HY, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 30.0}, {"kind": "breathe", "min": 0.3, "max": 0.7, "period": 110.0}], tint="FFD070", lod_skip=True,
                    alpha=0.85, draw={"from_scale": 0.0, "delay": 2, "ticks": 12})
    G.card(it, "falna_ring", "falna", (0.0, HY, 0.0), (1, 0, 0), (0, 0, 1), 10.0, 10.0, "runes")
    it.define_group("sparks", pivot=(0.0, 30.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 5.0}], tint="FFE8A0", lod_skip=True, alpha=0.9)
    for k in range(5):
        G.card(it, "spark_%d" % k, "sparks", (3.0 * math.cos(k * 1.7), 20.0 + 7.0 * k, 3.0 * math.sin(k * 1.7)), (1, 0, 0), (0, 1, 0), 1.6, 1.6, "sparkle")
    it.flare((0.0, HY, 3.2), 5.0, "FFB04A", "star", phase=0.0, pulse=0.7)
    it.flare((0.0, HY, -3.2), 5.0, "FFB04A", "star", phase=0.5, pulse=0.7)
    it.flare((0.0, Y_TIP - 1.0, 0.0), 3.6, "FFD070", "sparkle", phase=0.25)

    it.glow = ("FFFFFF", 0.17, 0.45, 1.0)
    it.layers = [
        {"id": "blade_flames", "kind": "light_shafts", "group": "tongues", "note": "four tongues of fire standing on the blade, flickering and swelling"},
        {"id": "hearth_glow", "kind": "light_shafts", "group": "hearth_glow", "note": "a crossed glow round the hearth orb, breathing"},
        {"id": "falna_ring", "kind": "halo_ring", "group": "falna", "note": "a ring of divine script turning round the hearth"},
        {"id": "embers", "kind": "orbit_shards", "group": "embers", "note": "seven embers circling the blade"},
        {"id": "ribbons", "kind": "mesh_sway", "group": "ribbons", "note": "the goddess's ribbons stream and sway behind the guard"},
        {"id": "sparks", "kind": "energy_arcs", "group": "sparks", "note": "five sparks flickering round the blade"},
        {"id": "hearth_flares", "kind": "lens_flare", "flares": "hearth", "note": "star flares in the hearth orb and on the point"},
        {"id": "embers_up", "kind": "motes", "particle": "flame", "rgb": "FFB04A", "where": "blade", "rate": 0.7, "radius": 0.7, "note": "fire motes rising off the blade"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "FFB04A", "radius": 0.8, "note": "a ring of ember motes on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "falna_flow", "kind": "vein_wave", "note": "the blessing runs up the blade as a wave of light"},
        {"id": "tongues_flicker", "kind": "group_flicker", "group": "tongues"},
        {"id": "falna_turn", "kind": "group_spin", "group": "falna"},
        {"id": "ember_orbit", "kind": "group_spin", "group": "embers"},
        {"id": "ribbon_sway", "kind": "group_sway", "group": "ribbons"},
        {"id": "hearth_breath", "kind": "group_breathe", "group": "hearth_glow"},
        {"id": "spark_flicker", "kind": "group_flicker", "group": "sparks"},
    ]
    it.draw = {"ticks": 12, "flash": "FFB04A", "groups": ["embers", "falna"], "note": "the hearth flares as the knife is drawn and the embers spiral out"}
    it.swing = {"trail": "hearth_fire", "rgb": "FFB04A", "rgb2": "FF5A20", "length": 0.6, "width": 0.25, "life": 7, "particles": "ember"}
    it.sound = "nusmp:item.genesis.hearth_blade.fire_loop"
    it.acts = {
        "falna_flurry": {"burst": "falna_flurry", "pattern": "cone", "rgb": "FFB04A", "ring": 3.5, "particles": 32, "sound": "nusmp:item.genesis.hearth_blade.flurry"},
        "hearth_dash": {"burst": "hearth_dash", "pattern": "line", "rgb": "FF8A30", "ring": 5.0, "particles": 30, "sound": "nusmp:item.genesis.hearth_blade.dash"},
        "goddess_blessing": {"burst": "goddess_blessing", "pattern": "column", "rgb": "FFD070", "ring": 3.5, "particles": 40, "sound": "nusmp:item.genesis.hearth_blade.blessing"},
    }

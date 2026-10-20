"""Sword of the First Dawn (Wave 71, Genesis, original): the sword that was shown to the world before there were mornings. It is the dawn in a hand, and the dawn is not kind to the night.

A long white-gold sword: the blade a bar of dawnlight (deep amber at the guard, white at the point) held in a gold frame of two rails and rungs, a rising sun set in the guard
with a sunburst of rays and swept wings, a white-leather grip wound with gold wire, a sun-orb pommel; a halo of dawn turns round the blade, shafts of morning light fan from the
sun and motes of daylight rise. Palette: F0F4FF white, E8C050 gold, FFF4B0 dawn.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (14.0, 62.0)
Y_G = 26.0
Y_TIP = 127.0
DAWN = (255, 244, 176)
AMBER = (255, 150, 60)
GOLD = (232, 192, 80)


def bar_hw(t):
    return 3.0 * (1.0 - 0.18 * t) if t < 0.86 else 3.0 * 0.82 * (1.0 - t) / 0.14


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- the blade: a bar of dawnlight in a gold frame
    bar = it.part("dawn_bar", "silver", base=(255, 246, 220), smooth=40, density=1.5, wear=0.3, mat={"dent": 0.06, "streak": 0.03})
    G.blade_by(it, bar, Y_G + 4.0, Y_TIP, lambda t: max(0.05, 2.7 * (1.0 - 0.16 * t) if t < 0.86 else 2.7 * 0.84 * (1.0 - t) / 0.14), lambda t: 0.55 - 0.15 * t, n=40, bevel=0.5, groove=0.35 if full else 0.0, tip=0.14)

    def bar_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            return
        X, Y = ctx.X, ctx.Y
        t = np.clip((Y - Y_G - 4.0) / (Y_TIP - Y_G - 4.0), 0, 1)
        col = tx.lerp3(AMBER, (255, 252, 240), np.clip(t * 1.25, 0, 1))
        shimmer = tx.fbm(ctx.shape, 16.0, ctx.seed + 3, 4)
        ctx.img[..., :3] = col * (0.82 + 0.3 * shimmer[..., None])
        ctx.img[..., 3] = 255
        rays = 0.5 + 0.5 * np.sin(Y * 0.9 + X * 4.0 + shimmer * 6.0)
        half = np.maximum(2.7 * (1.0 - 0.16 * t), 0.2)
        u = X / half
        tx.put(ctx.glow, np.clip(0.55 + 0.45 * rays, 0, 1) * (1 - 0.3 * np.abs(u)), tx.lerp3(AMBER, DAWN, t)[0, 0] if False else (255, 214, 120), 0.95, 0.6, halo=0.6)
        tx.put(ctx.glow, np.clip(t - 0.4, 0, 1) * 0.9, (255, 252, 230), 0.9, 0.8)
        tx.put(ctx.vein, np.clip(rays - 0.5, 0, 1) * 2.0, (255, 255, 235), 0.9, 0.7)
    bar.painters.append(bar_paint)
    fr = it.part("frame_rails", "gold", base=(236, 196, 90), smooth=40, uv_mode="loft", density=1.3, wear=0.5)
    for sx in (-1, 1):
        n = it.n(30, 6)
        pts = [(sx * (bar_hw(i / n) + 0.75) * (1.0 if (i / n) < 0.86 else 1.0), Y_G + 1.0 + (Y_TIP - 8.0 - Y_G) * i / n, 0.0) for i in range(n + 1)]
        pts = [(x * (1.0 if i / n < 0.9 else (1 - ((i / n) - 0.9) / 0.1)), y, z) for i, (x, y, z) in enumerate(pts)]
        ml.sweep(fr, pts, ml.octagon(1.0, 1.0, 0.3), scales=lambda t: (0.7 - 0.25 * t, 0.9 - 0.3 * t), up=(0, 0, 1), caps=(True, True))
    cap = it.part("frame_tip", "gold", base=(250, 214, 110), smooth=35, density=1.3)
    G.bipyramid(cap, (0.0, Y_TIP - 5.0, 0.0), (0, 1, 0), 6.5, 0.9, sides=4)
    if full:
        rg = it.part("frame_rungs", "gold", base=(246, 208, 100), smooth=35, density=1.2)
        for k in range(10):
            y = Y_G + 12.0 + 8.8 * k
            hwk = bar_hw((y - Y_G - 4.0) / (Y_TIP - Y_G - 4.0)) + 0.7
            ml.bar(rg, (-hwk, y, 0.0), (hwk, y, 0.0), 0.7, 0.62, up=(0, 0, 1), taper=0.6)
            for sx in (-1, 1):
                G.orb(it, rg, (sx * hwk, y, 0.0), 0.62, sides=6, stacks=4)
        gm = it.part("frame_gems", "crystal", base=(255, 240, 160), smooth=25)
        for k in range(5):
            y = Y_G + 17.0 + 17.0 * k
            ml.gem(gm, (0.0, y, 0.52), 0.8, 1.1, 6, axis=(0, 0, 1))
            ml.gem(gm, (0.0, y, -0.52), 0.8, 1.1, 6, axis=(0, 0, -1))
        gm.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), DAWN, 1.0, 0.9, halo=0.5))

    def rail_paint(ctx):
        if ctx.kind != "loft":
            return
        m = G.text_along(ctx, "LVX PRIMA ANTE NOCTEM · ", ctx.perim * 0.5, 0.45, 1.05, fname="DejaVuSerif-Bold.ttf")
        tx.engrave(ctx, m, 0.6)
        tx.put(ctx.glow, m * 0.8, (255, 240, 190), 0.8, 0.6)
        tx.put(ctx.glow, tx.soft(ctx.S - 0.0, 0.1) * 0.5 + tx.soft(ctx.S - 1.0, 0.1) * 0.5, (255, 236, 170), 0.6, 0.5)
    fr.painters.append(rail_paint)

    # ---------------------------------------------------------------- the guard: a rising sun with a sunburst and swept wings
    sun = it.part("rising_sun", "gold", base=(250, 214, 110), smooth=40, uv_mode="loft", density=1.5)
    ml.lathe(sun, "z", (0.0, Y_G, 0.0), [(-1.6, 0.0), (-1.0, 3.9), (0.0, 4.4), (1.0, 3.9), (1.6, 0.0)], sides=it.n(24, 8), caps=(False, False))
    sc = it.part("sun_core", "crystal", base=(255, 220, 130), smooth=30, density=1.5)
    for sgn in (1, -1):
        ml.lathe(sc, "z" if sgn > 0 else "-z", (0.0, Y_G, sgn * 1.4), [(0.0, 3.3), (0.5, 2.9), (1.1, 1.7), (1.5, 0.0)], sides=it.n(20, 8), caps=(False, False))

    def sun_paint(ctx):
        if ctx.kind == "dom" and ctx.axis == 2:
            X, Y = ctx.X, ctx.Y - Y_G
            r = np.hypot(X, Y)
            ang = np.arctan2(Y, X)
            ray = (0.5 + 0.5 * np.cos(ang * 12)) ** 2
            ctx.img[..., :3] = tx.lerp3((255, 120, 40), (255, 250, 210), np.clip(1.0 - r / 3.3, 0, 1)) * (0.85 + 0.25 * ray[..., None])
            ctx.img[..., 3] = 255
            tx.put(ctx.glow, np.clip(1.0 - r / 3.4, 0, 1), (255, 220, 130), 1.0, 0.85, halo=1.0)
            tx.put(ctx.vein, np.clip(ray * (r < 3.0), 0, 1) * 0.8, (255, 255, 230), 0.9, 0.8)
        else:
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.7, (255, 214, 120), 0.8, 0.7)
    sc.painters.append(sun_paint)
    if full:
        sb = it.part("sunburst", "gold", base=(255, 224, 130), smooth=25, density=1.2)
        for zz in (0.55, -0.55):
            G.star_plate(it, sb, (0.0, Y_G, zz * 4.0), (0, 0, math.copysign(1, zz)), 9.6, 5.0, 12, 0.5, phase=0.1)
            G.star_plate(it, sb, (0.0, Y_G, zz * 3.0), (0, 0, math.copysign(1, zz)), 7.4, 5.0, 12, 0.4, phase=0.28)
        sb.painters.append(lambda ctx: (tx.put(ctx.glow, tx.soft(ctx.S - 0.5, 0.45) * 0.6, DAWN, 0.8, 0.6, halo=0.4)))
    for sgn, nm in ((-1, "l"), (1, "r")):
        it.define_group("wing_" + nm, pivot=(sgn * 4.5, Y_G - 2.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 2.5 * sgn, "period": 120.0}], lod_skip=False)
        wg = it.part("guard_wing_" + nm, "gold", group="wing_" + nm, base=(246, 210, 110), smooth=35, uv_mode="loft", density=1.2)
        G.feather_fan(it, wg, (sgn * 4.5, Y_G - 2.0, 0.0), (sgn * 1.0, 0.25, 0.0), 62.0, 6, 11.5, 2.5, thick=0.5, curl=0.08, side=(0, 0, 1), n_st=5, len_fn=lambda t: 0.7 + 0.3 * math.sin(math.pi * t))
        if full:
            lw = it.part("guard_wing_low_" + nm, "gold", group="wing_" + nm, base=(240, 200, 100), smooth=35, uv_mode="loft")
            G.feather_fan(it, lw, (sgn * 4.0, Y_G - 3.0, 0.0), (sgn * 0.8, -0.8, 0.0), 40.0, 3, 8.0, 2.0, thick=0.4, curl=0.1, side=(0, 0, 1), n_st=4)

        def wing_paint(ctx):
            if ctx.kind != "loft":
                return
            barb = 0.5 + 0.5 * np.sin(ctx.B * 5.0 + np.abs(ctx.S - 0.5) * 38.0)
            ctx.img[..., :3] *= (0.8 + 0.3 * barb[..., None])
            tx.tint(ctx, (255, 236, 170), 0.3 * ctx.T)
            tx.put(ctx.glow, tx.soft(ctx.S, 0.09) * (0.3 + 0.7 * ctx.T), DAWN, 0.8, 0.6, halo=0.5)
        wg.painters.append(wing_paint)

    # ---------------------------------------------------------------- the grip: white leather, gold wire, a sun-orb pommel
    gp = it.part("grip", "leather", base=(244, 240, 232), smooth=50, uv_mode="loft", density=1.2)
    G.rod(it, gp, 3.0, Y_G - 4.4, 1.45, 1.6, sides=12, swell=[(0.5, 1.14)])
    gw = it.part("grip_wire", "gold", base=(240, 200, 100), smooth=55, uv_mode="loft", density=1.2)
    n_w = it.n(60, 8)
    helix = [(1.78 * math.cos(7.0 * 2 * math.pi * i / n_w), 3.6 + (Y_G - 8.4) * i / n_w, 1.78 * math.sin(7.0 * 2 * math.pi * i / n_w)) for i in range(n_w + 1)]
    G.tube(it, gw, helix, 0.2, sides=4)
    po = it.part("pommel", "gold", base=(250, 214, 110), smooth=45, uv_mode="loft", density=1.4)
    G.orb(it, po, (0.0, 1.9, 0.0), 2.9, 2.6, 2.9, sides=14, stacks=9)
    pe = it.part("pommel_core", "crystal", base=(255, 230, 150), smooth=30)
    G.orb(it, pe, (0.0, 1.9, 0.0), 1.7, 1.5, 1.7, sides=10, stacks=6)
    pe.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (255, 224, 140), 1.0, 0.85, halo=0.5))
    if full:
        pr = it.part("pommel_rays", "gold", base=(255, 226, 130), smooth=25)
        G.rays(it, pr, (0.0, 1.9, 0.0), (0, 0, 1), 12, 3.0, 2.0, 0.7, 0.4, long_every=2, long_k=1.5)
        G.rays(it, pr, (0.0, 1.9, 0.0), (1, 0, 0), 12, 3.0, 2.0, 0.7, 0.4, long_every=2, long_k=1.5)
    gc = it.part("grip_collar", "gold", base=(246, 208, 100), smooth=40, uv_mode="loft")
    ml.collar(gc, Y_G - 4.8, 2.0, 1.4, lip=0.35 if full else 0.0, sides=it.n(14, 6))
    ml.collar(gc, 4.2, 2.0, 1.4, lip=0.35 if full else 0.0, sides=it.n(14, 6))

    # ---------------------------------------------------------------- the dawn halo, the sunburst rays, the morning shafts
    it.define_group("halo", pivot=(0.0, 74.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 22.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 2.0, "period": 160.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 16})
    if full:
        hr = it.part("dawn_halo", "gold", group="halo", base=(255, 232, 150), smooth=45, uv_mode="loft")
        G.torus(it, hr, (0.0, 74.0, 0.0), 7.8, 0.45, (0, 1, 0), major=44, minor=6)
        hs = it.part("dawn_halo_suns", "crystal", group="halo", base=(255, 240, 170), smooth=30)
        for k in range(8):
            a = 2 * math.pi * k / 8
            G.orb(it, hs, (7.8 * math.cos(a), 74.0, 7.8 * math.sin(a)), 0.8, sides=6, stacks=4)
        hr.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), DAWN, 0.95, 0.8, halo=0.5))
        hs.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (255, 250, 220), 1.0, 0.9, halo=0.4))
    it.define_group("sun_rays", pivot=(0.0, Y_G, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 9.0}, {"kind": "breathe", "min": 0.35, "max": 0.8, "period": 100.0}], tint="FFE49A", lod_skip=True,
                    alpha=0.55, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "sun_rays_card", "sun_rays", (0.0, Y_G, -3.0), (1, 0, 0), (0, 1, 0), 26.0, 26.0, "rays")
    G.card(it, "sun_rays_card", "sun_rays", (0.0, Y_G, 3.0), (1, 0, 0), (0, -1, 0), 26.0, 26.0, "rays")
    it.define_group("dawn_glow", anim=[{"kind": "breathe", "min": 0.2, "max": 0.55, "period": 95.0}], tint="FFD890", lod_skip=True, alpha=0.8)
    G.card(it, "dawn_glow_a", "dawn_glow", (0.0, 74.0, 0.0), (1, 0, 0), (0, 1, 0), 9.0, 56.0, "glow")
    G.card(it, "dawn_glow_a", "dawn_glow", (0.0, 74.0, 0.0), (0, 0, 1), (0, 1, 0), 9.0, 56.0, "glow")
    it.define_group("shafts", pivot=(0.0, Y_G, 0.0), anim=[{"kind": "breathe", "min": 0.2, "max": 0.6, "period": 70.0, "phase": 0.3}, {"kind": "spin", "axis": [0, 0, 1], "dps": -5.0}], tint="FFF0B8",
                    lod_skip=True, alpha=0.55)
    for k in range(8):
        a = 2 * math.pi * k / 8 + 0.2
        G.card(it, "shaft_%d" % k, "shafts", (math.cos(a) * 17.0, Y_G + math.sin(a) * 17.0, -1.5), (-math.sin(a), math.cos(a), 0), (-math.cos(a), -math.sin(a), 0), 3.0, 15.0, "shaft")
    it.define_group("sparkles", pivot=(0.0, 74.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 4.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": 40.0}], tint="FFFFFF", lod_skip=True, alpha=0.9)
    for k in range(6):
        a = k * 1.05
        G.card(it, "sparkle_%d" % k, "sparkles", (6.0 * math.cos(a), 36.0 + 17.0 * k, 6.0 * math.sin(a)), (math.cos(a + 1.57), 0, math.sin(a + 1.57)), (0, 1, 0), 2.2, 2.2, "sparkle")
    it.flare((0.0, Y_G, 4.0), 12.0, "FFE49A", "star", phase=0.0, pulse=0.6)
    it.flare((0.0, Y_G, -4.0), 12.0, "FFE49A", "star", phase=0.5, pulse=0.6)
    it.flare((0.0, Y_TIP - 2.0, 0.0), 7.0, "FFFFFF", "star", phase=0.25, pulse=0.6)
    it.flare((0.0, 1.9, 3.2), 5.0, "FFE49A", "star", phase=0.7)

    it.glow = ("FFFFFF", 0.2, 0.55, 1.0)
    it.layers = [
        {"id": "dawn_halo", "kind": "orbit_shards", "group": "halo", "note": "a ring of dawn with eight small suns turning round the blade"},
        {"id": "sun_rays", "kind": "light_shafts", "group": "sun_rays", "note": "a turning fan of rays behind and before the rising sun"},
        {"id": "morning_shafts", "kind": "light_shafts", "group": "shafts", "note": "eight shafts of morning light fanning from the sun, breathing"},
        {"id": "dawn_glow", "kind": "light_shafts", "group": "dawn_glow", "note": "a soft column of light along the whole blade"},
        {"id": "sparkles", "kind": "energy_arcs", "group": "sparkles", "note": "sparkles of daylight flickering up the blade"},
        {"id": "sun_flares", "kind": "lens_flare", "flares": "sun", "note": "star flares in the sun, at the point and in the pommel"},
        {"id": "wing_sway", "kind": "mesh_sway", "group": "wing_l", "note": "the guard wings sway"},
        {"id": "daylight_motes", "kind": "motes", "particle": "end_rod", "rgb": "FFF4B0", "where": "blade", "rate": 0.8, "radius": 2.0, "note": "motes of daylight rising off the blade"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "FFF4B0", "radius": 1.0, "note": "a ring of dawn light on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "dawn_flow", "kind": "vein_wave", "note": "light runs up the bar from the sun to the point"},
        {"id": "halo_turn", "kind": "group_spin", "group": "halo"},
        {"id": "rays_turn", "kind": "group_spin", "group": "sun_rays"},
        {"id": "shaft_breath", "kind": "group_breathe", "group": "shafts"},
        {"id": "glow_column", "kind": "group_breathe", "group": "dawn_glow"},
        {"id": "sparkle_flicker", "kind": "group_flicker", "group": "sparkles"},
        {"id": "wing_sway", "kind": "group_sway", "group": "wing_l"},
    ]
    it.draw = {"ticks": 16, "flash": "FFE49A", "groups": ["halo", "sun_rays"], "note": "the sun rises in the guard and the halo of dawn settles round the blade as the sword is drawn"}
    it.swing = {"trail": "dawnlight", "rgb": "FFF4B0", "rgb2": "E8C050", "length": 1.8, "width": 0.4, "life": 11, "particles": "daylight"}
    it.sound = "nusmp:item.genesis.first_dawn.choir_loop"
    it.acts = {
        "dawnlight_cut": {"burst": "dawnlight_cut", "pattern": "cone", "rgb": "FFF4B0", "ring": 5.0, "particles": 40, "sound": "nusmp:item.genesis.first_dawn.cut"},
        "first_light": {"burst": "first_light", "pattern": "nova", "rgb": "FFE49A", "ring": 5.5, "particles": 46, "sound": "nusmp:item.genesis.first_dawn.light"},
        "daybreak": {"burst": "daybreak", "pattern": "column", "rgb": "FFFFFF", "ring": 6.0, "particles": 50, "sound": "nusmp:item.genesis.first_dawn.daybreak"},
    }

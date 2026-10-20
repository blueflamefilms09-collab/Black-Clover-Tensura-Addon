"""Starforged Aeon Bow (Wave 71, Genesis): a bow forged in a falling star and strung with a comet's tail. The arrow it looses has already arrived.

The model keeps the old bow's frame (limbs along y from -66 to 66, the grip at x = 31, the string plane at x = 0, draw toward -x, the place line 225 degrees of vanilla's bow display):
white-gold crescent limbs set with flat stars and drawn with constellations, a riser with a glowing blue star core in a gold ring, a string of comet tail with a streak of
light, finials with star gems, a ring of small stars turning round the core, and (in the draw stages) a star-headed arrow that rides the string. Four meshes share one skin:
rest, pull1, pull2, pull3 (the string, the limb tips and the arrow move; nothing else). Palette: E8F0FF white gold, E8C040 gold, A8D8FF star blue.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
STATES = [("starforged_aeon_bow", ""), ("starforged_aeon_bow_pull1", "pull1"), ("starforged_aeon_bow_pull2", "pull2"), ("starforged_aeon_bow_pull3", "pull3")]
FOCUS = (-24.0, 24.0)
XG = 31.0                                   # the grip's x
HALF = 66.0                                 # the limb tips' |y|
BLUE = (168, 216, 255)
GOLD = (232, 192, 64)
WHITE = (232, 240, 255)
ARROW_LEN = 71.5
DRAW = {"": 0.0, "pull1": 0.30, "pull2": 0.75, "pull3": 1.0}


def limb_x(y, tip_x):
    t = abs(y) / HALF
    return XG - (XG - tip_x) * t ** 1.45 + 2.6 * max(0.0, (t - 0.8) / 0.2) ** 2


def build(it, state=""):
    full = it.full
    f = DRAW[state]
    fl = 1.0 if state == "" else f                      # the rest mesh also lays out the arrow parts (not written), as drawn at full draw
    tip_x = -0.2 - 9.0 * f
    # ---------------------------------------------------------------- the limbs: white-gold crescents set with stars
    for sgn, nm in ((1, "limb_top"), (-1, "limb_bottom")):
        lb = it.part(nm, "silver", base=(238, 240, 250), smooth=42, uv_mode="loft", density=1.25, wear=0.6)
        n = it.n(44, 8)
        pts = [(limb_x(sgn * 4.0 + sgn * (HALF - 4.0) * i / n, tip_x), sgn * 4.0 + sgn * (HALF - 4.0) * i / n, 0.0) for i in range(n + 1)]
        ml.sweep(lb, pts, G.blade_section(0.5, 0.0), scales=lambda t: (3.2 - 2.2 * t ** 1.3 + 0.35, 1.4 - 0.8 * t), up=(0, 0, 1), caps=(True, True))

        def limb_paint(ctx):
            if ctx.kind != "loft":
                return
            S, B, T = ctx.S, ctx.B, ctx.T
            # white to gold toward the tip, gold edges, a constellation across the face
            tx.tint(ctx, (255, 236, 190), 0.55 * T ** 1.5)
            edge = np.maximum(tx.soft(S, 0.07), np.maximum(tx.soft(S - 0.5, 0.07), tx.soft(S - 1.0, 0.07)))
            ctx.img[..., :3] += (np.array(GOLD, np.float32) - ctx.img[..., :3]) * (0.55 * edge)[..., None]
            rr = np.random.default_rng(ctx.seed + 11)
            pts_c = [(float(rr.uniform(0.2, 0.8)) * ctx.perim, float(rr.uniform(3, ctx.length - 3))) for _ in range(9)]
            m = np.zeros(ctx.shape, np.float32)
            for i in range(len(pts_c) - 1):
                m = np.maximum(m, tx.mask_polyline(ctx, [pts_c[i], pts_c[i + 1]], 0.13))
            for (a, b) in pts_c:
                m = np.maximum(m, tx.mask_ellipse(ctx, a, b, 0.3, 0.3))
            tx.engrave(ctx, m * 0.6, 0.4)
            tx.put(ctx.vein, m, BLUE, 0.9, 0.7, halo=0.7)
            tx.put(ctx.glow, edge * (0.2 + 0.5 * T), (255, 236, 180), 0.6, 0.5, halo=0.6)
        lb.painters.append(limb_paint)
        # flat stars set along the limb's front and back, gold wire along both edges
        if full:
            st = it.part(nm + "_stars", "gold", base=(250, 224, 130), smooth=25, density=1.2)
            for k in range(6):
                y = sgn * (14.0 + 8.6 * k)
                x = limb_x(y, tip_x)
                sz = 2.4 - 0.15 * k
                for zz in (1.35 - 0.4 * (y / HALF), -1.35 + 0.4 * (y / HALF)):
                    G.star_plate(it, st, (x, y, math.copysign(abs(zz) - 0.12, zz)), (0, 0, math.copysign(1, zz)), sz, None, 5, 0.35, phase=0.3 * k)
            st.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.9, (255, 244, 200), 0.9, 0.7, halo=0.4))
            wire = it.part(nm + "_wire", "gold", base=(246, 214, 110), smooth=50, uv_mode="loft")
            for side in (-1, 1):
                wp = []
                for i in range(31):
                    yy = sgn * (6.0 + 56.0 * i / 30.0)
                    t_ = abs(yy) / HALF
                    wp.append((limb_x(yy, tip_x) + side * (2.5 - 1.7 * t_), yy, 0.0))
                G.tube(it, wire, wp, 0.2, sides=4)
            wire.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.4, (255, 236, 170), 0.5, 0.5))
        # the finial: a gold crescent cap with a star gem
        tipy = sgn * HALF
        fn = it.part(nm + "_finial", "gold", base=(246, 210, 100), smooth=35, uv_mode="loft", density=1.2)
        ml.lathe(fn, "y" if sgn > 0 else "-y", (limb_x(tipy, tip_x), tipy - sgn * 1.5, 0.0), [(0.0, 1.0), (1.4, 1.6), (3.0, 1.2), (4.4, 0.0)], sides=it.n(10, 5), caps=(False, False))
        gm = it.part(nm + "_gem", "crystal", base=(168, 216, 255), smooth=25)
        ml.gem(gm, (limb_x(tipy, tip_x) - 0.1, tipy - sgn * 0.5, 1.1), 0.8, 1.1, 6 if full else 4, axis=(0, 0, 1))
        ml.gem(gm, (limb_x(tipy, tip_x) - 0.1, tipy - sgn * 0.5, -1.1), 0.8, 1.1, 6 if full else 4, axis=(0, 0, -1))
        gm.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), BLUE, 1.0, 0.8, halo=0.4))

    # ---------------------------------------------------------------- the riser, the grip and the star core
    rs = it.part("riser", "silver", base=(240, 238, 232), smooth=35, uv_mode="loft", density=1.3, wear=0.6)
    ml.loft(rs, "y", (XG, 0.0, 0.0), [(-16.0, 1.4, 1.1), (-11.0, 2.8, 2.4), (-4.0, 3.2, 2.7), (4.0, 3.2, 2.7), (11.0, 2.8, 2.4), (16.0, 1.4, 1.1)], ml.octagon(2.0, 2.0, 0.5), caps=(True, True))
    gp = it.part("grip", "leather", base=(244, 238, 226), smooth=50, uv_mode="loft", density=1.2)
    for i in range(it.n(8, 3)):
        ml.collar(gp, -9.5 + 2.4 * i, 3.55, 1.5, lip=0.35 if full else 0.0, sides=it.n(12, 5), axis="y", origin=(XG, 0.0, 0.0), oval=0.85, caps=(False, False))
    gp.painters.append(lambda ctx: (tx.tint(ctx, (255, 244, 226), 0.3)))
    ringp = it.part("core_ring", "gold", base=(246, 214, 110), smooth=45, uv_mode="loft", density=1.3)
    G.torus(it, ringp, (XG, 0.0, 0.0), 5.6, 0.62, (0, 0, 1), major=36, minor=6)
    G.torus(it, ringp, (XG, 0.0, 0.0), 4.0, 0.42, (0, 0, 1), major=28, minor=5)
    if full:
        rv = it.part("core_rays", "gold", base=(252, 230, 140), smooth=25)
        for zz in (0.9, -0.9):
            G.star_plate(it, rv, (XG, 0.0, zz), (0, 0, math.copysign(1, zz)), 11.0, 6.4, 12, 0.5, phase=0.1)
    core = it.part("star_core", "crystal", base=(120, 180, 255), smooth=25, density=1.4)
    for sgn in (1, -1):
        ml.gem(core, (XG, 0.0, sgn * 2.2), 3.3, 0.9, 10 if full else 5, axis=(0, 0, sgn))

    def core_paint(ctx):
        if ctx.kind == "dom" and ctx.axis == 2:
            X, Y = ctx.X - XG, ctx.Y
            r = np.hypot(X, Y)
            ang = np.arctan2(Y, X)
            star = np.clip(1.0 - r / (1.2 + 2.0 * np.abs(np.cos(ang * 2.5)) ** 3), 0, 1)
            ctx.img[..., :3] = tx.lerp3((40, 80, 200), (220, 240, 255), np.clip(1.0 - r / 3.4, 0, 1))
            tx.put(ctx.glow, np.clip(0.6 + star, 0, 1), BLUE, 1.0, 0.8, halo=0.8)
            tx.put(ctx.vein, np.clip(star * 1.2, 0, 1), (255, 255, 255), 0.9, 0.9)
        else:
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.8, BLUE, 0.9, 0.7)
    core.painters.append(core_paint)
    if full:
        fil = it.part("riser_filigree", "gold", base=(250, 222, 130), smooth=45, uv_mode="loft")
        for sy in (1, -1):
            for sx in (1, -1):
                G.spiral_scroll(it, fil, (XG + sx * 3.0, sy * 12.0, 2.2), 2.4, 0.5, 1.5, (0, 0, 1), 0.25, z=0.0, n=14, start=1.0 * sx)
                G.spiral_scroll(it, fil, (XG + sx * 3.0, sy * 12.0, -2.2), 2.4, 0.5, 1.5, (0, 0, -1), 0.25, z=0.0, n=14, start=1.0 * sx)
        ct = it.part("comet_ribbons", "cloth", base=(210, 232, 255), smooth=40, uv_mode="loft")
        for sgn in (1, -1):
            tp = [(tip_x, sgn * HALF, 0.0), (tip_x - 3.0, sgn * (HALF + 4.0), 1.0), (tip_x - 6.0, sgn * (HALF + 9.0), -0.6), (tip_x - 8.0, sgn * (HALF + 15.0), 0.8), (tip_x - 8.5, sgn * (HALF + 22.0), -0.2)]
            ml.sweep(ct, [tuple(q) for q in G.catmull(tp, 3)], ml.rect_ring(1.0, 1.0), scales=lambda t: (1.2 * (1.0 - 0.85 * t), 0.06), up=(0, 0, 1), caps=(False, False))
        ct.painters.append(lambda ctx: (tx.put(ctx.glow, np.clip(1.0 - ctx.T, 0, 1) * 0.5, (230, 244, 255), 0.8, 0.6), tx.put(ctx.vein, np.clip(1.0 - ctx.T, 0, 1) * 0.6, BLUE, 0.7, 0.6)))
    ar = it.part("arrow_rest", "gold", base=(240, 208, 110), smooth=40)
    ml.box(ar, (XG - 4.6, -0.8, -0.5), (XG - 1.8, 0.8, 0.5))

    # ---------------------------------------------------------------- the string of comet tail: a V to the nock, a streak of light along it
    nock_x = -0.2 - 36.3 * f
    strg = it.part("string", "cloth", base=(236, 244, 255), smooth=60, uv_mode="loft", density=1.0)
    spts = []
    for i in range(it.n(10, 4) + 1):
        y = HALF - 2.0 * HALF * i / it.n(10, 4)
        t = 1.0 - abs(y) / HALF
        spts.append((tip_x + (nock_x - tip_x) * t, y, 0.0))
    G.tube(it, strg, spts, 0.2, sides=5)
    strg.painters.append(lambda ctx: (tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.9, (230, 244, 255), 1.0, 0.9, halo=0.4), tx.put(ctx.vein, np.ones(ctx.shape, np.float32), BLUE, 0.9, 0.7)))
    nk = it.part("nock_bead", "gold", base=(250, 224, 130), smooth=40, uv_mode="loft")
    G.orb(it, nk, (nock_x, 0.0, 0.0), 0.6, sides=8, stacks=5)

    # ---------------------------------------------------------------- the arrow: a star-headed shaft that rides the string (drawn stages; laid out in the rest skin, not written there)
    ax0 = -0.2 - 36.3 * fl
    shf = it.part("arrow_shaft", "gold", base=(250, 232, 170), smooth=50, uv_mode="loft", density=1.2)
    ml.loft(shf, "x", (ax0, 0.0, 0.0), [(0.0, 0.5, 0.5), (ARROW_LEN - 7.0, 0.5, 0.5)], ml.circle(1.0, it.n(10, 5)), caps=(True, True))
    hd = it.part("arrow_head", "silver", base=(240, 244, 255), smooth=25, density=1.3)
    G.bipyramid(hd, (ax0 + ARROW_LEN - 4.0, 0.0, 0.0), (1, 0, 0), 3.8, 1.5, sides=4)
    G.cone(hd, (ax0 + ARROW_LEN - 7.5, 0.0, 0.0), (ax0 + ARROW_LEN - 4.0, 0.0, 0.0), 0.6, 4)
    fth = it.part("arrow_fletching", "silver", base=(250, 250, 255), smooth=35, uv_mode="loft")
    for k in range(3):
        a = 2 * math.pi * k / 3 + math.pi / 2
        d = np.array([1.0, 0.0, 0.0])
        G.feather(it, fth, (ax0 + 1.0, 0.45 * math.cos(a), 0.45 * math.sin(a)), d, 9.0, 1.2, thick=0.12, curl=0.0, side=(0, math.cos(a), math.sin(a)), n=4)
    for part in (shf, hd, fth):
        part.skip_write = (state == "")
    hd.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (255, 250, 220), 1.0, 0.9, halo=0.5))
    shf.painters.append(lambda ctx: tx.put(ctx.vein, tx.soft(ctx.S - 0.5, 0.25), BLUE, 0.9, 0.7))

    # ---------------------------------------------------------------- the stars circling the core, the comet tails and the halo layer
    it.define_group("stars", pivot=(XG, 0.0, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 40.0}, {"kind": "bob", "axis": [0, 0, 1], "amp": 0.8, "period": 90.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 14})
    if full:
        sp = it.part("orbit_stars", "crystal", group="stars", base=(220, 236, 255), smooth=20)
        for k in range(7):
            a = 2 * math.pi * k / 7
            G.bipyramid(sp, (XG + 9.0 * math.cos(a), 9.0 * math.sin(a), 0.4 * math.sin(a * 3)), (math.cos(a), math.sin(a), 0.0), 1.5, 0.5, sides=4)
            G.star_plate(it, sp, (XG + 12.5 * math.cos(a + 0.45), 12.5 * math.sin(a + 0.45), 0.0), (0, 0, 1), 1.6, None, 4, 0.3)
        sp.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (235, 244, 255), 1.0, 0.8, halo=0.4))
    it.define_group("core_halo", pivot=(XG, 0.0, 0.0), anim=[{"kind": "breathe", "min": 0.3, "max": 0.8, "period": 80.0}, {"kind": "spin", "axis": [0, 0, 1], "dps": -8.0}], tint="A8D8FF", lod_skip=True, alpha=0.8)
    G.card(it, "core_halo_card", "core_halo", (XG, 0.0, -1.0), (1, 0, 0), (0, 1, 0), 16.0, 16.0, "rays")
    G.card(it, "core_halo_card", "core_halo", (XG, 0.0, 1.0), (1, 0, 0), (0, -1, 0), 16.0, 16.0, "rays")
    G.card(it, "core_halo_glow", "core_halo", (XG, 0.0, 0.0), (1, 0, 0), (0, 1, 0), 11.0, 11.0, "glow")
    G.card(it, "core_halo_glow", "core_halo", (XG, 0.0, 0.0), (0, 0, 1), (0, 1, 0), 11.0, 11.0, "glow")
    it.define_group("zodiac", pivot=(XG, 0.0, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 11.0}, {"kind": "breathe", "min": 0.3, "max": 0.65, "period": 120.0}], tint="E8F0FF", lod_skip=True,
                    alpha=0.8, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "zodiac_card", "zodiac", (XG, 0.0, -2.8), (1, 0, 0), (0, 1, 0), 14.0, 14.0, "runes")
    G.card(it, "zodiac_card", "zodiac", (XG, 0.0, 2.8), (1, 0, 0), (0, -1, 0), 14.0, 14.0, "runes")
    it.define_group("comet_tails", anim=[{"kind": "flicker", "min": 0.35, "max": 1.0, "rate": 3.0}], tint="CFE6FF", lod_skip=True, alpha=0.75)
    for sgn in (1, -1):
        G.card(it, "tail_%d" % sgn, "comet_tails", (tip_x + 8.0, sgn * (HALF + 3.0), 0.0), (0, 1, 0), (1, 0, 0), 2.6, 11.0, "streak")
        G.card(it, "tail_%d" % sgn, "comet_tails", (tip_x + 8.0, sgn * (HALF + 3.0), 0.0), (0, 0, 1), (1, 0, 0), 2.6, 11.0, "streak")
    G.card(it, "string_streak", "comet_tails", ((tip_x + nock_x) * 0.5 - 0.2, 0.0, 0.0), (0, 0, 1), (0, 1, 0), 0.9, HALF * 0.98, "streak") if False else None
    it.define_group("twinkle", pivot=(XG, 0.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 4.0}], tint="FFFFFF", lod_skip=True, alpha=0.9)
    for k, (x, y) in enumerate(((20.0, 22.0), (12.0, -34.0), (4.0, 46.0), (9.0, -50.0), (24.0, 8.0), (2.0, 20.0))):
        G.card(it, "twinkle_%d" % k, "twinkle", (limb_x(y, tip_x) - 0.5 + 0.0 * x, y, 1.8), (1, 0, 0), (0, 1, 0), 1.9, 1.9, "star")
    it.flare((XG, 0.0, 3.8), 9.0, "A8D8FF", "star", phase=0.0, pulse=0.6)
    it.flare((XG, 0.0, -3.8), 9.0, "A8D8FF", "star", phase=0.5, pulse=0.6)
    it.flare((limb_x(HALF, tip_x), HALF, 1.4), 4.5, "FFFFFF", "star", phase=0.3)
    it.flare((limb_x(-HALF, tip_x), -HALF, 1.4), 4.5, "FFFFFF", "star", phase=0.8)

    it.glow = ("FFFFFF", 0.14, 0.45, 1.0)
    it.layers = [
        {"id": "orbit_stars", "kind": "orbit_shards", "group": "stars", "note": "seven small stars and seven star plates circling the core"},
        {"id": "core_rays", "kind": "light_shafts", "group": "core_halo", "note": "a turning fan of rays and a glow round the star core, breathing"},
        {"id": "zodiac", "kind": "halo_ring", "group": "zodiac", "note": "a zodiac ring of runes turning behind the riser, both faces"},
        {"id": "comet_tails", "kind": "light_shafts", "group": "comet_tails", "note": "comet tails streaming off both limb tips, flickering"},
        {"id": "twinkle", "kind": "energy_arcs", "group": "twinkle", "note": "stars twinkling along the limbs"},
        {"id": "tip_flares", "kind": "lens_flare", "flares": "core and tips", "note": "star flares in the core and on both finials"},
        {"id": "star_motes", "kind": "motes", "particle": "end_rod", "rgb": "A8D8FF", "where": "blade", "rate": 0.7, "radius": 1.2, "note": "glints of starlight drifting off the bow"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "A8D8FF", "radius": 1.0, "note": "a ring of pale blue stars on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "comet_flow", "kind": "vein_wave", "note": "light runs along the string and the constellations"},
        {"id": "star_orbit", "kind": "group_spin", "group": "stars"},
        {"id": "zodiac_turn", "kind": "group_spin", "group": "zodiac"},
        {"id": "core_fan", "kind": "group_spin", "group": "core_halo"},
        {"id": "tail_flicker", "kind": "group_flicker", "group": "comet_tails"},
        {"id": "twinkle", "kind": "group_flicker", "group": "twinkle"},
        {"id": "core_breath", "kind": "group_breathe", "group": "core_halo"},
    ]
    it.draw = {"ticks": 16, "flash": "A8D8FF", "groups": ["stars", "zodiac"], "note": "the stars drop into orbit and the zodiac opens behind the riser when the bow is drawn from the hand"}
    it.swing = {"trail": "comet", "rgb": "A8D8FF", "rgb2": "E8C040", "length": 1.3, "width": 0.3, "life": 10, "particles": "star_glint"}
    it.sound = "nusmp:item.genesis.aeon_bow.starsong_loop"
    it.acts = {
        "meteor_arrow": {"burst": "meteor_arrow", "pattern": "line", "rgb": "A8D8FF", "ring": 7.0, "particles": 36, "sound": "nusmp:item.genesis.aeon_bow.meteor"},
        "constellation_volley": {"burst": "constellation_volley", "pattern": "cone", "rgb": "E8F0FF", "ring": 6.0, "particles": 44, "sound": "nusmp:item.genesis.aeon_bow.volley"},
        "starfall": {"burst": "starfall", "pattern": "column", "rgb": "E8C040", "ring": 6.0, "particles": 46, "sound": "nusmp:item.genesis.aeon_bow.starfall"},
    }

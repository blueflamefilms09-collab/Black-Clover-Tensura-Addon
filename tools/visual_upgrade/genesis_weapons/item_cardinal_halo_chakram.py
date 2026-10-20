"""Cardinal Halo Chakram (Wave 71, Genesis, The Rising of the Shield Hero): a ring of gold and sea-glass, the throwing halo of a cardinal weapon. Thrown from above it falls
like a verdict.

The model keeps the old layout (a ring on a short hand grip with a cross-bar guard, ring centre at y 52) and builds on it: a bevelled gold blade ring engraved with the
verdict, an enamelled inner ring, sea-glass studs, gold spokes round a cyan eye hub, a feathered pair of wings at the crown, a floating halo of its own turning about the ring
with small suns on it, rays of light. Palette: E8D8A0 gold, 2AA8D8 sea glass, 8AE8FF cyan light.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (28.0, 92.0)
C = (0.0, 52.0, 0.0)
R_BLADE, R_INNER, R_OUT = 36.0, 27.0, 40.0
CYAN = (138, 232, 255)
SEA = (42, 168, 216)
GOLD = (232, 216, 160)


def build(it, state=""):
    full = it.full
    cx, cy, cz = C
    # ---------------------------------------------------------------- the blade ring: gold, bevelled, a fuller of sea-glass light
    SEC = G.blade_section(0.55, 0.34)
    marks = G.section_marks(SEC, 3.4, 0.85)
    rb = it.part("ring_blade", "gold", base=(222, 196, 120), smooth=40, uv_mode="loft", density=1.15, wear=0.7)
    n_major = it.n(72, 18)
    pts = [(cx + R_BLADE * math.cos(2 * math.pi * i / n_major), cy + R_BLADE * math.sin(2 * math.pi * i / n_major), 0.0) for i in range(n_major + 1)]
    ml.sweep(rb, pts, SEC if full else ml.bevelled(2.0, 2.0, 0.5), scales=lambda t: (3.4, 0.85), up=(0, 0, 1), caps=(False, False))

    def blade_paint(ctx):
        if ctx.kind != "loft":
            return
        S, B = ctx.S, ctx.B
        top, bot = marks["top"], marks["bot"]
        # the fuller: enamel of sea-glass with a flowing caustic web, front and back
        for s0 in (top, bot):
            band = tx.soft(S - s0, 0.07)
            web = tx.caustics(ctx, 10.0, 5 + int(s0 * 10), 2.2)
            tx.inlay(ctx, (band > 0.1).astype(np.float32) * np.float32(1.0), SEA, 0.5, 0.45)
            tx.put(ctx.glow, band * (0.35 + 0.65 * web), CYAN, 0.9, 0.5, halo=0.8)
            tx.put(ctx.vein, band * web, (230, 255, 255), 0.9, 0.7)
            # the verdict engraved on the shoulder of the bevel, in two scripts
        m = G.text_along(ctx, "IUDICIUM CARDINALIS CADIT E CAELO ", ctx.perim * (marks["top"] * 0.55), 0.62, 1.45, fname="DejaVuSerif-Bold.ttf")
        m = np.maximum(m, G.text_along(ctx, "".join(tx.rune_glyphs(2, 99)), ctx.perim * (marks["bot"] + 0.12), 0.6, 1.55))
        tx.engrave(ctx, m * 0.9, 0.6)
        tx.put(ctx.glow, m * 0.7, (255, 240, 190), 0.7, 0.5)
        edge = np.maximum(tx.soft(S - marks["tip0"], 0.035), tx.soft(S - 1.0, 0.035))
        edge = np.maximum(edge, tx.soft(S - marks["tip1"], 0.035))
        tx.put(ctx.glow, edge * 0.8, (255, 250, 220), 0.9, 0.7, halo=1.2)
        ctx.img[..., :3] += (255 - ctx.img[..., :3]) * (0.5 * edge)[..., None]
    rb.painters.append(blade_paint)

    # the outer filigree ring and the inner band
    ob = it.part("ring_outer", "gold", base=(236, 218, 150), smooth=45, uv_mode="loft", density=1.1)
    G.torus(it, ob, C, R_OUT, 0.62, (0, 0, 1), major=60, minor=6)
    ib = it.part("ring_inner", "gold", base=(214, 190, 120), smooth=45, uv_mode="loft", density=1.15)
    G.torus(it, ib, C, R_INNER, 1.05, (0, 0, 1), major=56, minor=8)

    def band_paint(ctx):
        if ctx.kind != "loft":
            return
        m = G.text_along(ctx, "".join(tx.rune_glyphs(4, 7)), ctx.perim * 0.5, 0.55, 1.2)
        tx.engrave(ctx, m, 0.55)
        tx.put(ctx.vein, m, CYAN, 0.8, 0.6)
    ib.painters.append(band_paint)
    ob.painters.append(lambda ctx: tx.put(ctx.glow, tx.soft(ctx.S - 0.5, 0.18) * 0.5, (255, 244, 200), 0.5, 0.5))

    # sea-glass studs on both faces of the ring, set in gold bezels
    sg = it.part("sea_glass", "crystal", base=(46, 170, 220), smooth=25, uv_mode="loft")
    bz = it.part("sea_glass_bezels", "gold", base=(226, 202, 130), smooth=35, uv_mode="loft")
    n_st = it.n(12, 4)
    for k in range(n_st):
        a = 2 * math.pi * k / n_st + 0.05
        pc = np.array([cx + (R_BLADE - 0.4) * math.cos(a), cy + (R_BLADE - 0.4) * math.sin(a)])
        for sgn in (1, -1):
            ml.gem(sg, (pc[0], pc[1], sgn * 1.2), 1.45, 0.9, 6 if full else 4, axis=(0, 0, sgn))
            if full:
                ml.collar(bz, 0.0, 1.9, 0.5, lip=0.0, sides=8, axis=(0, 0, sgn), origin=(pc[0], pc[1], sgn * 0.7), caps=(False, False))

    def glass_paint(ctx):
        n = tx.fbm(ctx.shape, 6.0, ctx.seed + 2, 3)
        web = tx.caustics(ctx, 7.0, 3, 2.0)
        tx.tint(ctx, (60, 190, 230), 0.4)
        tx.put(ctx.glow, np.clip(0.55 + 0.45 * web, 0, 1), CYAN, 0.95, 0.6, halo=0.5)
        tx.put(ctx.vein, web, (255, 255, 255), 0.7, 0.8)
    sg.painters.append(glass_paint)

    # ---------------------------------------------------------------- spokes and the hub
    sp = it.part("spokes", "gold", base=(228, 206, 140), smooth=35, density=1.2)
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        d = np.array([math.cos(a), math.sin(a), 0.0])
        c0 = np.array(C) + d * 5.5
        c1 = np.array(C) + d * (R_INNER - 0.4)
        ml.loft(sp, c1 - c0, c0, [(0.0, 1.0, 1.0), (float(np.linalg.norm(c1 - c0)), 1.0, 1.0)], ml.octagon(2.4, 1.0, 0.3), up=(0, 0, 1))
    if full:
        sc = it.part("spoke_scrolls", "gold", base=(240, 220, 160), smooth=45, uv_mode="loft")
        for k in range(4):
            a = math.pi / 2 * k + math.pi / 4
            d = np.array([math.cos(a), math.sin(a), 0.0])
            side = np.array([-d[1], d[0], 0.0])
            G.spiral_scroll(it, sc, np.array(C) + d * 17.0 + side * 2.6, 2.2, 0.5, 1.5, (0, 0, 1), 0.28, z=0.6, n=14, start=a)
    hub = it.part("hub", "gold", base=(236, 216, 150), smooth=40, uv_mode="loft", density=1.2)
    ml.lathe(hub, "z", (cx, cy, -1.4), [(0.0, 4.6), (0.8, 5.0), (1.6, 4.1), (2.8, 3.8)], sides=it.n(20, 8), caps=(True, True))
    eye = it.part("hub_eye", "crystal", base=(60, 190, 232), smooth=30, density=1.3)
    for sgn in (1, -1):
        ml.lathe(eye, "z" if sgn > 0 else "-z", (cx, cy, sgn * 1.3), [(0.0, 3.4), (0.7, 3.0), (1.4, 1.8), (1.8, 0.0)], sides=it.n(18, 6), caps=(False, False))

    def eye_paint(ctx):
        if ctx.kind != "dom":
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.7, CYAN, 0.8, 0.5)
            return
        X, Y = ctx.X - cx, ctx.Y - cy
        r = np.hypot(X, Y)
        ang = np.arctan2(Y, X)
        ray = (0.5 + 0.5 * np.cos(ang * 8)) ** 3
        ctx.img[..., :3] = tx.lerp3((20, 90, 150), (190, 250, 255), np.clip(1.0 - r / 3.4, 0, 1))
        ctx.img[..., 3] = 255
        tx.put(ctx.glow, np.clip(1.0 - r / 3.4, 0, 1) * (0.6 + 0.4 * ray), CYAN, 1.0, 0.8, halo=1.2)
        tx.put(ctx.vein, tx.soft(r - 2.4, 0.2), (255, 255, 255), 0.9, 0.9)
    eye.painters.append(eye_paint)

    # ---------------------------------------------------------------- the hand grip: leather under gold, a cross-bar guard, a pommel star
    gp = it.part("grip", "leather", base=(70, 48, 36), smooth=50, uv_mode="loft")
    G.rod(it, gp, 1.0, 20.0, 1.55, 1.7, sides=10, swell=[(0.5, 1.12)])
    gw = it.part("grip_wraps", "gold", base=(214, 190, 120), smooth=50, uv_mode="loft")
    for i in range(it.n(8, 3)):
        ml.collar(gw, 2.2 + 2.1 * i, 1.85, 0.9, lip=0.0, sides=it.n(8, 5), caps=(False, False))
    gd = it.part("cross_guard", "gold", base=(236, 214, 140), smooth=35, density=1.2)
    ml.loft(gd, "x", (0.0, 21.5, 0.0), [(-8.0, 0.35, 0.35), (-6.0, 1.0, 1.0), (0.0, 1.35, 1.35), (6.0, 1.0, 1.0), (8.0, 0.35, 0.35)], ml.octagon(2.4, 2.2, 0.55))
    pm_ = it.part("pommel", "gold", base=(228, 206, 140), smooth=40, uv_mode="loft")
    ml.lathe(pm_, "y", (0.0, 0.0, 0.0), [(-3.0, 0.0), (-2.0, 1.2), (-0.4, 2.2), (1.4, 1.7)], sides=it.n(12, 5), caps=(False, False))
    if full:
        ps = it.part("pommel_star", "gold", base=(250, 230, 170), smooth=30)
        G.rays(it, ps, (0.0, 1.0, 0.0), (0, 0, 1), 8, 1.4, 2.3, 0.9, 0.5, long_every=2, long_k=1.4)
        G.rays(it, ps, (0.0, 1.0, 0.0), (1, 0, 0), 8, 1.4, 2.0, 0.9, 0.5, long_every=2, long_k=1.3)

    # ---------------------------------------------------------------- the crown: a pair of wings of verdict and a rain of rays
    if full:
        top_y = cy + R_OUT
        for sgn in (-1, 1):
            wn = it.part("wing_" + ("l" if sgn < 0 else "r"), "gold", base=(240, 226, 170), smooth=35, uv_mode="loft")
            G.feather_fan(it, wn, (sgn * 2.5, top_y + 0.5, 0.0), (sgn * 0.9, 0.4, 0.0), 70.0, 6, 15.0, 3.2, thick=0.4, curl=0.05, side=(0, 0, 1), n_st=5)

            def wing_paint(ctx):
                if ctx.kind != "loft":
                    return
                barb = 0.5 + 0.5 * np.sin(ctx.B * 5.0 + np.abs(ctx.S - 0.5) * 40.0)
                ctx.img[..., :3] *= (0.8 + 0.3 * barb[..., None])
                tx.put(ctx.glow, tx.soft(ctx.S, 0.1) * ctx.T * 0.7, (255, 240, 190), 0.7, 0.5, halo=0.5)
            wn.painters.append(wing_paint)
        rr = it.part("verdict_rays", "gold", base=(250, 232, 172), smooth=25)
        G.rays(it, rr, C, (0, 0, 1), 18, R_OUT + 0.6, 4.2, 1.8, 0.4, phase=0.0, long_every=3, long_k=1.7)
        cr = it.part("crown_cross", "gold", base=(240, 220, 150), smooth=35)
        ml.bar(cr, (0.0, cy + R_OUT + 0.5, 0.0), (0.0, cy + R_OUT + 8.5, 0.0), 1.5, 0.6, up=(0, 0, 1), taper=2.0)
        ml.bar(cr, (-3.2, cy + R_OUT + 5.2, 0.0), (3.2, cy + R_OUT + 5.2, 0.0), 1.1, 0.5, up=(0, 0, 1), taper=1.0)

    # ---------------------------------------------------------------- the floating halo: a ring of light about the weapon with small suns on it
    it.define_group("halo", pivot=C, anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 16.0}, {"kind": "bob", "axis": [0, 0, 1], "amp": 0.8, "period": 110.0}], lod_skip=True,
                    draw={"from_scale": 1.6, "delay": 0, "ticks": 16})
    if full:
        hr = it.part("halo_ring", "gold", group="halo", base=(255, 240, 190), smooth=45, uv_mode="loft")
        G.torus(it, hr, C, R_OUT + 4.6, 0.42, (0, 0, 1), major=44, minor=5)
        hs = it.part("halo_suns", "crystal", group="halo", base=(255, 245, 200), smooth=30)
        for k in range(8):
            a = 2 * math.pi * k / 8
            G.orb(it, hs, (cx + (R_OUT + 4.6) * math.cos(a), cy + (R_OUT + 4.6) * math.sin(a), 0.0), 1.15 + 0.35 * (k % 2), sides=6, stacks=4)

        def halo_paint(ctx):
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (255, 244, 200), 0.95, 0.7, halo=0.6)
        hr.painters.append(halo_paint)
        hs.painters.append(halo_paint)
    it.define_group("verdict", pivot=C, anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": -11.0}, {"kind": "breathe", "min": 0.35, "max": 0.85, "period": 100.0}], tint="8AE8FF", lod_skip=True,
                    alpha=0.6, draw={"from_scale": 0.0, "delay": 4, "ticks": 14})
    G.card(it, "verdict_disc", "verdict", (cx, cy, -0.9), (1, 0, 0), (0, 1, 0), 34.0, 34.0, "runes")
    G.card(it, "verdict_disc", "verdict", (cx, cy, 0.9), (1, 0, 0), (0, -1, 0), 34.0, 34.0, "runes")
    it.define_group("glory", pivot=C, anim=[{"kind": "breathe", "min": 0.2, "max": 0.6, "period": 80.0}], tint="BFF4FF", lod_skip=True, alpha=0.5)
    G.card(it, "glory_card", "glory", (cx, cy, -1.6), (1, 0, 0), (0, 1, 0), 38.0, 38.0, "glow")
    G.card(it, "glory_card", "glory", (cx, cy, 1.6), (1, 0, 0), (0, 1, 0), 26.0, 26.0, "rays")
    it.define_group("pillar", anim=[{"kind": "breathe", "min": 0.15, "max": 0.5, "period": 120.0}], tint="FFF2C0", lod_skip=True, alpha=0.7)
    G.card(it, "pillar_card", "pillar", (cx, cy + 58.0, 0.0), (1, 0, 0), (0, -1, 0), 6.0, 38.0, "shaft")
    G.card(it, "pillar_card_b", "pillar", (cx, cy + 58.0, 0.0), (0, 0, 1), (0, -1, 0), 6.0, 38.0, "shaft")
    it.define_group("sparks", pivot=C, anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 60.0}, {"kind": "flicker", "min": 0.1, "max": 1.0, "rate": 4.0}], tint="FFFFFF", lod_skip=True, alpha=0.9)
    for k in range(6):
        a = 2 * math.pi * k / 6
        G.card(it, "spark_%d" % k, "sparks", (cx + 31.0 * math.cos(a), cy + 31.0 * math.sin(a), 0.0), (1, 0, 0), (0, 1, 0), 3.2, 3.2, "star")
    for k in range(4):
        a = math.pi / 2 * k
        it.flare((cx + R_BLADE * math.cos(a), cy + R_BLADE * math.sin(a), 1.8), 5.0, "BFF4FF", "star", phase=k * 0.25)
    it.flare((cx, cy, 2.6), 8.0, "FFFFFF", "star", phase=0.0, pulse=0.6)
    it.flare((cx, cy, -2.6), 8.0, "FFFFFF", "star", phase=0.5, pulse=0.6)

    it.glow = ("FFFFFF", 0.17, 0.5, 1.0)
    it.layers = [
        {"id": "floating_halo", "kind": "halo_ring", "group": "halo", "note": "a second ring with eight small suns turning about the weapon"},
        {"id": "verdict_disc", "kind": "halo_ring", "group": "verdict", "note": "a rune disc of the verdict turning in the ring, both faces"},
        {"id": "glory", "kind": "light_shafts", "group": "glory", "note": "a soft glow and a fan of rays behind the hub"},
        {"id": "light_pillar", "kind": "light_shafts", "group": "pillar", "note": "a pillar of light falling from the crown"},
        {"id": "sparks", "kind": "energy_arcs", "group": "sparks", "note": "six star sparks running round the inner ring"},
        {"id": "stud_flares", "kind": "lens_flare", "flares": "studs", "note": "star flares on the four cardinal studs and the hub"},
        {"id": "sea_glass_motes", "kind": "motes", "particle": "end_rod", "rgb": "8AE8FF", "where": "blade", "rate": 0.6, "note": "glints of cyan light off the ring"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "E8D8A0", "radius": 1.0, "note": "a ring of gold motes on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "vein_flow", "kind": "vein_wave", "note": "light flows round the sea-glass fuller"},
        {"id": "halo_turn", "kind": "group_spin", "group": "halo"},
        {"id": "verdict_turn", "kind": "group_spin", "group": "verdict"},
        {"id": "sparks_run", "kind": "group_spin", "group": "sparks"},
        {"id": "glory_breath", "kind": "group_breathe", "group": "glory"},
        {"id": "flare_pulse", "kind": "flare_pulse"},
    ]
    it.draw = {"ticks": 16, "flash": "8AE8FF", "groups": ["halo", "verdict"], "note": "the halo drops over the ring from above, the rune disc unfolds"}
    it.swing = {"trail": "halo_arc", "rgb": "E8D8A0", "rgb2": "2AA8D8", "length": 1.1, "width": 0.4, "life": 10, "particles": "gold_glint"}
    it.sound = "nusmp:item.genesis.cardinal_halo.ring_loop"
    it.acts = {
        "halo_throw": {"burst": "halo_ring_out", "pattern": "ring", "rgb": "E8D8A0", "ring": 3.5, "particles": 28, "sound": "nusmp:item.genesis.cardinal_halo.throw"},
        "judgement_ring": {"burst": "verdict_circle", "pattern": "nova", "rgb": "8AE8FF", "ring": 5.0, "particles": 40, "sound": "nusmp:item.genesis.cardinal_halo.judgement"},
        "cardinal_descent": {"burst": "light_fall", "pattern": "column", "rgb": "FFF2C0", "ring": 5.0, "particles": 44, "sound": "nusmp:item.genesis.cardinal_halo.descent"},
    }

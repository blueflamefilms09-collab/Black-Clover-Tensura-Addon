"""Astralis - The Eye of the Universe (Wave 1, Genesis): a silver staff whose gem is the eye of the universe. Your spells hit harder, it lets a mage hold more than one
bond, and its Astral Arts are Blinding Stars, Dimension Door, Scrying and Planeshift.

The model follows the card the old mesh was built from (silver staff, purple beaded grip, swept silver filigree wings cradling a faceted gem in a ring) and builds on it what
the lore says: the gem IS an eye, a pale iris round a slit of deep space, gimballed in an armillary of three turning rings carrying planets, a halo of star shards, the
winged filigree and a rune zodiac turning behind it. Palette: C9CFE0 silver, 7A3CC8 purple, C48CFF eye glow.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (150.0, 188.0)
EYE = (0.0, 170.0, 0.0)
R_EYE = 6.4
VIOLET = (196, 140, 255)
PURPLE = (122, 60, 200)
STARW = (225, 232, 255)


def build(it, state=""):
    full = it.full
    cx, cy, cz = EYE
    # ---------------------------------------------------------------- the foot: a spiked cap with a star point
    ft = it.part("foot_cap", "silver", base=(188, 196, 216), smooth=40, uv_mode="loft")
    ml.lathe(ft, "y", (0, 0, 0), [(0.0, 0.0), (1.4, 1.6), (3.6, 2.9), (6.0, 2.2), (9.5, 1.7)], sides=it.n(14, 6), caps=(False, False))
    if full:
        fs = it.part("foot_fins", "silver", base=(210, 216, 232), smooth=30)
        for k in range(4):
            a = math.pi / 2 * k
            G.cone(fs, (2.5 * math.cos(a), 5.5, 2.5 * math.sin(a)), (4.6 * math.cos(a), 1.2, 4.6 * math.sin(a)), 0.55, 4)

    # ---------------------------------------------------------------- the shaft: silver, engraved with a star chart
    sh = it.part("shaft", "silver", base=(196, 202, 218), smooth=50, uv_mode="loft", density=1.2)
    G.rod(it, sh, 9.0, 98.0, 1.55, 1.7, sides=14, swell=[(0.33, 1.18), (0.34, 1.0), (0.66, 1.18), (0.67, 1.0)])
    rng = np.random.default_rng(21)
    chart = [(float(rng.uniform(0.2, 9.4)), float(rng.uniform(6, 86))) for _ in range(34)]

    def shaft_paint(ctx):
        if ctx.kind != "loft":
            return
        m = np.zeros(ctx.shape, np.float32)
        pts = [(a % ctx.perim, b) for (a, b) in chart if b < ctx.length]
        for i, (a, b) in enumerate(pts):
            d = sorted(range(len(pts)), key=lambda j: (pts[j][0] - a) ** 2 + ((pts[j][1] - b) * 0.15) ** 2)[1:3]
            for j in d:
                m = np.maximum(m, tx.mask_polyline(ctx, [(a, b), pts[j]], 0.14))
            m = np.maximum(m, tx.mask_ellipse(ctx, a, b, 0.32, 0.32))
        tx.engrave(ctx, m, 0.5)
        tx.put(ctx.vein, m, VIOLET, 0.9, 0.7, halo=0.8)
    sh.painters.append(shaft_paint)
    cl = it.part("shaft_collars", "gold", base=(176, 186, 226), smooth=40)
    for y in (9.0 + 0.33 * 89.0, 9.0 + 0.66 * 89.0, 98.0):
        ml.collar(cl, y - 1.0, 2.35, 2.0, lip=0.45 if full else 0.0, sides=it.n(12, 5))
    if full:
        gs = it.part("collar_gems", "crystal", base=(90, 120, 230), smooth=25)
        for y in (9.0 + 0.33 * 89.0, 9.0 + 0.66 * 89.0):
            for k in range(4):
                a = math.pi / 2 * k + 0.4
                ml.gem(gs, (2.3 * math.cos(a), y, 2.3 * math.sin(a)), 0.55, 1.2, 6, axis=(math.cos(a), 0, math.sin(a)))

        def gem_glow(ctx):
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (110, 150, 255), 0.8, 0.6)
        gs.painters.append(gem_glow)

    # ---------------------------------------------------------------- the grip: six purple beads and silver cages
    gr = it.part("grip_beads", "crystal", base=(112, 56, 190), smooth=60, uv_mode="loft")
    prof = [(98.0, 1.9)]
    nb = 6
    for i in range(nb):
        a, b = 98.0 + 30.0 * i / nb, 98.0 + 30.0 * (i + 1) / nb
        prof += [(a + (b - a) * 0.2, 3.9), ((a + b) / 2, 4.3), (b - (b - a) * 0.2, 3.9), (b, 2.1)]
    ml.lathe(gr, "y", (0, 0, 0), prof, sides=it.n(12, 5), caps=(False, False))

    def bead_paint(ctx):
        n = tx.fbm(ctx.shape, 14.0, ctx.seed + 3, 4)
        web = tx.caustics(ctx, 14.0, 5, 2.6)
        tx.put(ctx.glow, np.clip(web * 0.7 + (n - 0.5) * 0.4, 0, 1) * 0.55, PURPLE, 0.6, 0.4, halo=0.6)
        ctx.img[..., :3] *= (0.75 + 0.5 * n[..., None])
        tx.put(ctx.vein, web * 0.6, VIOLET, 0.8, 0.6)
    gr.painters.append(bead_paint)
    if full:
        cage = it.part("grip_cage", "silver", base=(206, 212, 232), smooth=40, uv_mode="loft")
        for k in range(6):
            a = 2 * math.pi * k / 6
            G.tube(it, cage, [(4.5 * math.cos(a), 98.0 + 30.0 * i / 8.0, 4.5 * math.sin(a)) for i in range(9)], 0.22, sides=4)
    fe = it.part("ferrule", "silver", base=(206, 212, 228), smooth=45, uv_mode="loft")
    ml.lathe(fe, "y", (0, 0, 0), [(128.0, 2.1), (129.4, 4.6), (132.0, 3.7), (140.0, 2.5), (148.0, 1.3)], sides=it.n(16, 6), caps=(False, False))

    # ---------------------------------------------------------------- the eye: a sphere with the iris on both faces, a slit of deep space for the pupil
    eye = it.part("eye", "crystal", base=(228, 224, 246), smooth=40, density=1.4, wear=0.2)
    st = []
    nst = it.n(14, 5)
    for i in range(nst + 1):
        a = -math.pi / 2 + math.pi * i / nst
        st.append((R_EYE * math.sin(a), R_EYE * math.cos(a), R_EYE * math.cos(a)))
    st[0] = (st[0][0], 0.0, 0.0)
    st[-1] = (st[-1][0], 0.0, 0.0)
    ml.loft(eye, "z", EYE, st, ml.circle(1.0, it.n(24, 8)), caps=(False, False))

    def eye_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            ctx.img[..., :3] = ctx.img[..., :3] * 0.5 + np.array([200, 190, 235], np.float32) * 0.5
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.25, VIOLET, 0.35, 0.3)
            return
        X, Y = ctx.X - cx, ctx.Y - cy
        r = np.hypot(X, Y) / R_EYE
        th = np.arctan2(Y, X)
        # sclera: pearl with a faint vein web and a darker limb
        web = tx.caustics(ctx, 16.0, 11, 3.0)
        scl = np.array([236, 232, 250], np.float32)[None, None, :] * (1.0 - 0.28 * np.clip(r, 0, 1) ** 2)[..., None]
        scl = scl * (1.0 - 0.18 * web[..., None]) + np.array([180, 130, 230], np.float32) * (0.22 * web)[..., None]
        # iris: gold to violet fibres
        fib = 0.5 + 0.5 * np.sin(th * 46.0 + tx.fbm(ctx.shape, 6.0, ctx.seed + 5, 3) * 7.0)
        iris_t = np.clip((r - 0.2) / 0.42, 0, 1)
        iris = tx.lerp3((250, 210, 120), (110, 50, 190), iris_t) * (0.65 + 0.5 * fib[..., None])
        iris_mask = tx.smoothstep(0.64 - r, -0.02, 0.02)
        limbal = tx.soft(r - 0.63, 0.05)
        col = scl * (1 - iris_mask[..., None]) + iris * iris_mask[..., None]
        col = col * (1 - 0.7 * limbal[..., None])
        # pupil: a vertical slit full of stars
        pup = np.clip(1.0 - (np.abs(X) / (0.16 * R_EYE) + np.abs(Y) / (0.55 * R_EYE)) ** 1.6, 0, 1)
        pm = (pup > 0.05).astype(np.float32)
        space = tx.lerp3((6, 6, 24), (40, 20, 90), np.clip(0.5 + 0.5 * tx.fbm(ctx.shape, 9.0, ctx.seed + 8, 3), 0, 1))
        col = col * (1 - pm[..., None]) + space * pm[..., None]
        ctx.img[..., :3] = col
        ctx.img[..., 3] = 255
        sp = np.zeros(ctx.shape, np.float32)
        rr = np.random.default_rng(ctx.seed + 4)
        n_st = 70
        xs, ys = rr.integers(0, ctx.w, n_st), rr.integers(0, ctx.h, n_st)
        for x, y in zip(xs, ys):
            sp[y, x] = float(rr.uniform(0.4, 1.0))
        sp = np.maximum(sp, np.roll(sp, 1, axis=1) * 0.5)
        sp *= pm
        ctx.img[..., :3] += (255 - ctx.img[..., :3]) * sp[..., None]
        # glow: the iris ring and the stars of the pupil, soft white core
        tx.put(ctx.glow, np.clip(iris_mask * (0.25 + 0.5 * fib) , 0, 1) * (1 - pm), (230, 180, 255), 0.8, 0.5, halo=1.2)
        tx.put(ctx.glow, sp, STARW, 1.0, 0.8, halo=0.8)
        tx.put(ctx.glow, pup * 0.5, (120, 80, 255), 0.6, 0.3)
        tx.put(ctx.vein, limbal, VIOLET, 1.0, 0.6, halo=1.0)
    eye.painters.append(eye_paint)

    # lids and lashes: two brows of silver round the eye, a fringe of long rays
    lid = it.part("eye_lids", "silver", base=(214, 220, 238), smooth=35, uv_mode="loft", density=1.2)
    G.torus(it, lid, EYE, R_EYE + 1.15, 0.85, (0, 0, 1), major=18, minor=6, a0=18.0, a1=162.0)
    G.torus(it, lid, EYE, R_EYE + 1.15, 0.85, (0, 0, 1), major=18, minor=6, a0=198.0, a1=342.0)
    if full:
        lash = it.part("eye_lashes", "silver", base=(232, 236, 250), smooth=25)
        for sgn in (1, -1):
            G.rays(it, lash, (cx, cy, sgn * 0.9), (0, 0, 1), 14, R_EYE + 1.9, 3.2, 0.7, 0.25, phase=0.07)
        lg = it.part("eye_lash_tips", "crystal", base=(210, 170, 255), smooth=20)
        for sgn in (1, -1):
            for k in range(7):
                a = 2 * math.pi * k / 7 + 0.07
                ml.gem(lg, (cx + (R_EYE + 6.9) * math.cos(a), cy + (R_EYE + 6.9) * math.sin(a), sgn * 0.9), 0.4, 1.3, 4, axis=(math.cos(a), math.sin(a), 0))

        def tips(ctx):
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32), VIOLET, 0.9, 0.7)
        lg.painters.append(tips)

    # ---------------------------------------------------------------- the armillary: three turning rings with planets
    ring_defs = [("ring_a", 14.8, 0.46, (0, 0, 1), (0, 1, 0), 18.0, (200, 206, 224)), ("ring_b", 12.6, 0.42, (1, 0, 0), (0, 1, 0), -27.0, (214, 196, 120)),
                 ("ring_c", 10.6, 0.38, (0, 0.7, 0.7), (1, 0, 0), 35.0, (180, 160, 240))]
    for gi, (name, R, rt, normal, axis, dps, base) in enumerate(ring_defs):
        it.define_group(name, pivot=EYE, anim=[{"kind": "spin", "axis": list(axis), "dps": dps, "phase": gi * 40.0}], lod_skip=True, draw={"from_scale": 0.2, "delay": gi * 3, "ticks": 14})
        rp = it.part(name, "silver" if gi != 1 else "gold", group=name, base=base, smooth=45, uv_mode="loft", density=1.2)
        G.torus(it, rp, EYE, R, rt, normal, major=44, minor=6)
        u, v, nn = G.basis(normal)
        pl = it.part(name + "_planets", "crystal", group=name, base=(150, 120, 255), smooth=40, uv_mode="loft")
        for k in range(2):
            a = 2.4 * k + gi * 1.3
            c = np.asarray(EYE, float) + R * (math.cos(a) * u + math.sin(a) * v)
            G.orb(it, pl, c, 0.95 + 0.25 * gi, sides=10, stacks=6)
        notch = it.part(name + "_nodes", "gold", group=name, base=(230, 210, 140), smooth=40)
        if full:
            for k in range(7):
                a = 2 * math.pi * k / 7 + 0.3
                c = np.asarray(EYE, float) + R * (math.cos(a) * u + math.sin(a) * v)
                ml.stud(notch, c, 0.4, 0.5, tuple(math.cos(a) * u + math.sin(a) * v), 5)

        def ring_paint(ctx, R=R):
            if ctx.kind != "loft":
                return
            m = G.text_along(ctx, "".join(tx.rune_glyphs(3, int(R * 10))), ctx.perim * 0.5, 0.5, 1.5)
            tx.engrave(ctx, m * 0.85, 0.55)
            tx.put(ctx.vein, m, VIOLET, 0.9, 0.6)
        rp.painters.append(ring_paint)

        def planet_paint(ctx):
            n = tx.fbm(ctx.shape, 8.0, ctx.seed + 1, 3)
            tx.tint(ctx, (170, 140, 255), 0.5)
            tx.put(ctx.glow, np.clip(n * 1.4, 0, 1) * 0.8, (170, 140, 255), 0.9, 0.5)
        pl.painters.append(planet_paint)

    # ---------------------------------------------------------------- the winged filigree: feathers swept from the ring's foot
    for sgn, nm in ((-1, "wing_l"), (1, "wing_r")):
        it.define_group(nm, pivot=(sgn * 7.0, 166.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 3.0 * sgn, "period": 130.0}], lod_skip=False)
        wp = it.part(nm, "silver", group=nm, base=(206, 212, 232), smooth=35, uv_mode="loft", density=1.1)
        G.feather_fan(it, wp, (sgn * 7.0, 166.0, 0.0), (sgn * 1.0, 0.32, 0.0), 78.0, 8, 25.0, 4.4, thick=0.5, curl=0.04, side=(0, 0, 1), n_st=6, len_fn=lambda t: 0.72 + 0.28 * math.sin(math.pi * t))
        if full:
            lw = it.part(nm + "_low", "silver", group=nm, base=(190, 198, 222), smooth=35, uv_mode="loft")
            G.feather_fan(it, lw, (sgn * 5.0, 162.0, 0.0), (sgn * 1.0, -0.7, 0.0), 60.0, 5, 15.0, 3.0, thick=0.4, curl=0.05, side=(0, 0, 1), n_st=5)

        def wing_paint(ctx):
            if ctx.kind != "loft":
                return
            t = ctx.T
            barb = 0.5 + 0.5 * np.sin(ctx.B * 5.4 + np.abs(ctx.S - 0.5) * 40.0)
            tx.tint(ctx, (235, 222, 255), 0.45 * t)
            ctx.img[..., :3] *= (0.78 + 0.3 * barb[..., None])
            edge = tx.soft(ctx.S - 0.0, 0.08) + tx.soft(ctx.S - 1.0, 0.08)
            tx.put(ctx.glow, np.clip(edge * 0.8 * t, 0, 1), (210, 170, 255), 0.7, 0.5, halo=0.6)
            tx.put(ctx.vein, barb * t * 0.5, VIOLET, 0.6, 0.5)
        wp.painters.append(wing_paint)
        if full:
            cu = it.part(nm + "_curl", "silver", base=(210, 216, 232), smooth=45, uv_mode="loft")
            G.spiral_scroll(it, cu, (sgn * 7.0, 156.0, 0.0), 3.4, 0.8, 1.6, (0, 0, 1), 0.36, start=0.5 * sgn, n=22)

    # the strut from the ferrule to the ring's foot, the eye's cradle
    cr = it.part("cradle", "silver", base=(208, 214, 232), smooth=45, uv_mode="loft", density=1.2)
    G.tube(it, cr, [(0.0, 148.0, 0.0), (0.0, 154.0, 0.0), (0.0, 158.0, 0.0), (0.0, 163.6, 0.0)], 1.25, 0.9, sides=10)
    if full:
        pr = it.part("cradle_prongs", "silver", base=(222, 226, 244), smooth=30)
        for k in range(6):
            a = math.pi / 3 * k + 0.5
            G.cone(pr, (0.0, 150.0, 0.0), (3.6 * math.cos(a), 160.0, 3.6 * math.sin(a)), 0.45, 4)

    # ---------------------------------------------------------------- the star shards orbiting the staff
    it.define_group("stars", pivot=(0, 126, 0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 24.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.8, "period": 100.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 5, "ticks": 14})
    if full:
        sp = it.part("star_shards", "crystal", group="stars", base=(210, 200, 255), smooth=20)
        G.shard_helix(it, sp, (0, 126, 0), (0, 1, 0), 8.5, 14, 52.0, 2.8, 0.7, sides=4, turns=2.4, tilt=0.5)
        sp2 = it.part("star_shards_big", "crystal", group="stars", base=(180, 160, 255), smooth=20)
        G.shard_helix(it, sp2, (0, 126, 0), (0, 1, 0), 12.0, 5, 40.0, 4.4, 1.1, sides=4, turns=1.2, tilt=0.25, phase=1.0)

        def shard_paint(ctx):
            tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * (0.4 + 0.6 * ctx.T), STARW, 0.95, 0.7, halo=0.5)
        sp.painters.append(shard_paint)
        sp2.painters.append(shard_paint)

    # ---------------------------------------------------------------- the halo layer: eye aura, zodiac, a pillar of starlight
    it.define_group("eye_aura", pivot=EYE, anim=[{"kind": "breathe", "min": 0.35, "max": 0.9, "period": 90.0}], tint="B070FF", lod_skip=True, alpha=0.85)
    G.card(it, "eye_aura_card", "eye_aura", (cx, cy, -0.4), (1, 0, 0), (0, 1, 0), 24.0, 24.0, "glow")
    G.card(it, "eye_aura_card", "eye_aura", (cx, cy, 0.4), (1, 0, 0), (0, 1, 0), 17.0, 17.0, "rays")
    it.define_group("zodiac", pivot=EYE, anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 9.0}, {"kind": "breathe", "min": 0.4, "max": 0.85, "period": 140.0}], tint="C48CFF", lod_skip=True,
                    alpha=0.9, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "zodiac_card", "zodiac", (cx, cy, -2.2), (1, 0, 0), (0, 1, 0), 21.0, 21.0, "runes")
    G.card(it, "zodiac_card", "zodiac", (cx, cy, 2.2), (1, 0, 0), (0, -1, 0), 21.0, 21.0, "runes")
    it.define_group("pillar", anim=[{"kind": "breathe", "min": 0.15, "max": 0.55, "period": 110.0}], tint="9A7CFF", lod_skip=True, alpha=0.7)
    G.card(it, "pillar_xy", "pillar", (0.0, 118.0, 0.0), (1, 0, 0), (0, 1, 0), 9.0, 54.0, "shaft")
    G.card(it, "pillar_zy", "pillar", (0.0, 118.0, 0.0), (0, 0, 1), (0, 1, 0), 9.0, 54.0, "shaft")
    it.define_group("comets", pivot=EYE, anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": -50.0}, {"kind": "flicker", "min": 0.2, "max": 1.0, "rate": 3.0}], tint="E0E8FF", lod_skip=True, alpha=0.9)
    for k in range(3):
        a = k * 2.09
        G.card(it, "comet_%d" % k, "comets", (cx + 18.0 * math.cos(a), cy + 4.0 * math.sin(a * 2), 18.0 * math.sin(a)), (math.cos(a + 1.57), 0, math.sin(a + 1.57)), (0, 1, 0), 7.0, 0.9, "streak")
    it.flare((cx, cy, 4.8), 16.0, "C48CFF", "star", phase=0.0, pulse=0.6)
    it.flare((cx, cy, -4.8), 16.0, "C48CFF", "star", phase=0.5, pulse=0.6)
    it.flare((cx, cy, 0.0), 9.0, "FFFFFF", "sparkle", phase=0.25, pulse=0.5)

    it.glow = ("FFFFFF", 0.14, 0.55, 1.0)
    it.layers = [
        {"id": "armillary", "kind": "orbit_rings", "group": "ring_a", "note": "three rings of an armillary, each turning about its own axis, carrying planets"},
        {"id": "star_shards", "kind": "orbit_shards", "group": "stars", "note": "nineteen crystal stars spiralling round the staff"},
        {"id": "zodiac", "kind": "halo_ring", "group": "zodiac", "note": "a rune zodiac turning behind the eye"},
        {"id": "eye_aura", "kind": "light_shafts", "group": "eye_aura", "note": "a breathing aura and a fan of rays round the eye"},
        {"id": "starlight_pillar", "kind": "light_shafts", "group": "pillar", "note": "a pillar of starlight down the staff"},
        {"id": "comets", "kind": "energy_arcs", "group": "comets", "note": "three comet streaks circling the eye, flickering"},
        {"id": "eye_flares", "kind": "lens_flare", "flares": "eye", "note": "star flares in both faces of the eye"},
        {"id": "wing_sway", "kind": "mesh_sway", "group": "wing_l", "note": "the filigree wings sway"},
        {"id": "star_motes", "kind": "motes", "particle": "end_rod", "rgb": "C48CFF", "where": "head", "rate": 0.6, "note": "glints of starlight drifting off the head"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "7A3CC8", "radius": 1.0, "note": "a ring of purple stars on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "constellation_flow", "kind": "vein_wave", "note": "light runs through the star chart"},
        {"id": "ring_a_turn", "kind": "group_spin", "group": "ring_a"},
        {"id": "ring_b_turn", "kind": "group_spin", "group": "ring_b"},
        {"id": "ring_c_turn", "kind": "group_spin", "group": "ring_c"},
        {"id": "shard_orbit", "kind": "group_spin", "group": "stars"},
        {"id": "zodiac_turn", "kind": "group_spin", "group": "zodiac"},
        {"id": "wing_sway", "kind": "group_sway", "group": "wing_l"},
        {"id": "eye_breath", "kind": "group_breathe", "group": "eye_aura"},
    ]
    it.draw = {"ticks": 18, "flash": "C48CFF", "groups": ["ring_a", "ring_b", "ring_c", "stars", "zodiac"], "note": "the armillary opens ring by ring and the star shards spiral out when the staff is drawn"}
    it.swing = {"trail": "starlight", "rgb": "C48CFF", "rgb2": "7A3CC8", "length": 1.0, "width": 0.3, "life": 9, "particles": "glint"}
    it.sound = "nusmp:item.genesis.astralis.hum_loop"
    it.acts = {
        "astral_cast": {"burst": "blinding_stars", "pattern": "nova", "rgb": "E0E8FF", "ring": 4.0, "particles": 36, "sound": "nusmp:item.genesis.astralis.cast"},
        "astral_pick": {"burst": "constellation_ring", "pattern": "ring", "rgb": "C48CFF", "ring": 2.5, "particles": 22, "sound": "nusmp:item.genesis.astralis.pick"},
    }

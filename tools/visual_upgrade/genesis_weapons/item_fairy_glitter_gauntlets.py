"""Fairy Glitter Gauntlets (Wave 71, Genesis, Fairy Tail): gauntlets of a glittering white that grow bright in a crowd. The Fairy Law they carry is the oldest magic of the guild:
it will not hurt a friend.

The pair stands side by side as the old mesh did (hands up, backs toward the viewer, centres at x = -12 and +12, forearms y 0 to 23, fists y 26 to 41). Each gauntlet: a layered
white vambrace with gold lames, a flared cuff carrying fairy wings of feathered plates, a back-of-hand plate with a gold eight-pointed star and a warm gem, fingers armoured
in plates, a crest of winged knuckle spikes, and a halo of glitter circling the fist. Palette: F8F0FF white, FFD070 gold, FFE8A0 star light.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (14.0, 46.0)
WHITE = (240, 236, 232)
GOLD = (255, 208, 112)
LIGHT = (255, 232, 160)
PINK = (255, 190, 220)


def hand(it, sgn, nm):
    """One gauntlet at x = sgn * 12; sgn -1 is the left one."""
    full = it.full
    X0 = 12.0 * sgn
    tag = "l" if sgn < 0 else "r"
    # ---- the vambrace: a tapering oval tube, overlapped by gold-edged lames
    fa = it.part("forearm_" + tag, "silver", base=WHITE, smooth=45, uv_mode="loft", density=1.15, wear=0.5)
    ml.loft(fa, "y", (X0, 0, 0), [(0.0, 5.6, 4.9), (6.0, 5.9, 5.3), (14.0, 6.6, 5.8), (23.0, 7.0, 6.2)], ml.circle(1.0, it.n(20, 8)), caps=(True, False))
    la = it.part("lames_" + tag, "silver", base=(248, 244, 240), smooth=45, uv_mode="loft", density=1.1, wear=0.6)
    lg = it.part("lame_trim_" + tag, "gold", base=GOLD, smooth=40, uv_mode="loft", density=1.1) if full else la
    for k in range(it.n(4, 3)):
        y = 3.0 + 4.9 * k
        r = (5.7 + 1.3 * k / 3.0)
        ml.lathe(la, "y", (X0, y, 0.0), [(0.0, r - 0.2), (1.3, r + 0.9), (4.4, r + 0.55), (5.0, r + 0.1)], sides=it.n(20, 8), caps=(False, False), squash=0.9)
        if full:
            ml.lathe(lg, "y", (X0, y, 0.0), [(4.4, r + 0.6), (4.7, r + 0.75), (5.1, r + 0.75), (5.3, r + 0.1)], sides=it.n(20, 8), caps=(False, False), squash=0.9)
    if full:
        rg = it.part("vambrace_ridge_" + tag, "gold", base=GOLD, smooth=40)
        ml.bar(rg, (X0, 1.0, 5.6), (X0, 22.0, 6.6), 1.4, 0.55, up=(0, 0, 1), taper=3.0)
        for sx in (-1, 1):
            ml.bar(rg, (X0 + sx * 6.1, 2.0, 1.0), (X0 + sx * 7.1, 22.0, 1.0), 1.0, 0.5, up=(sx, 0, 0), taper=3.0)

    def vam_paint(ctx):
        if ctx.kind != "loft":
            return
        S, B = ctx.S, ctx.B
        # a filigree of fairy-tail scrolls and a gold inlay band down the outside
        s = tx.fbm(ctx.shape, 14.0, ctx.seed + 1, 3)
        swirl = np.abs(np.sin(B * 1.1 + np.sin(S * 12.0) * 1.7)) < 0.07
        m = swirl.astype(np.float32) * tx.soft(S - 0.5, 0.35)
        tx.engrave(ctx, tx.blur(m, 0.6), 0.5)
        tx.put(ctx.vein, tx.blur(m, 0.6) * 0.8, LIGHT, 0.8, 0.6)
        tx.tint(ctx, (255, 244, 238), 0.25)
        ctx.img[..., :3] *= (0.92 + 0.14 * s[..., None])
    fa.painters.append(vam_paint)

    def lame_paint(ctx):
        tx.put(ctx.glow, tx.soft(ctx.T - 0.5, 0.3) * 0.35, LIGHT, 0.5, 0.5)
    lg.painters.append(lame_paint)

    # ---- the cuff: a flared bell with a gold rim
    cf = it.part("cuff_" + tag, "gold", base=(250, 224, 150), smooth=40, uv_mode="loft", density=1.15)
    ml.lathe(cf, "y", (X0, 0, 0), [(21.0, 7.2), (22.6, 8.6), (25.0, 8.2), (26.4, 6.6)], sides=it.n(22, 8), caps=(False, False), squash=0.86)
    if full:
        rv = it.part("cuff_gems_" + tag, "crystal", base=(255, 220, 150), smooth=25)
        for k in range(8):
            a = 2 * math.pi * k / 8
            ml.gem(rv, (X0 + 8.4 * math.cos(a), 23.8, 7.2 * math.sin(a)), 0.5, 1.2, 4, axis=(math.cos(a), 0, math.sin(a)))
        rv.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), LIGHT, 0.9, 0.7))

    # ---- the hand: the back plate, a boss with the guild star, finger plates, thumb
    fp = it.part("fist_" + tag, "silver", base=(244, 240, 236), smooth=40, uv_mode="loft", density=1.15)
    ml.loft(fp, "y", (X0, 0, 0), [(26.0, 6.9, 5.6), (31.0, 7.1, 5.8), (36.0, 6.7, 5.5), (41.0, 6.0, 4.8)], ml.octagon(2.0, 2.0, 0.5), caps=(True, True))
    bp = it.part("back_plate_" + tag, "silver", base=(250, 246, 240), smooth=35, density=1.3, wear=0.6)
    ml.loft(bp, "z", (X0, 33.0, 5.1), [(0.0, 5.8, 7.0), (0.9, 5.5, 6.7), (1.4, 4.8, 5.9)], ml.circle(1.0, it.n(18, 8)), caps=(True, True))

    def plate_paint(ctx):
        if ctx.kind == "dom" and ctx.axis == 2 and ctx.sign >= 0:
            X, Y = ctx.X - X0, ctx.Y - 33.0
            r = np.hypot(X, Y)
            ang = np.arctan2(Y, X)
            star = np.clip(1.0 - r / (3.6 + 2.2 * (np.cos(ang * 4 - 0.4) ** 8)), 0, 1)
            rim = tx.soft(r - 5.2, 0.35)
            ctx.img[..., :3] = ctx.img[..., :3] * (1 - 0.8 * (star > 0.02)[..., None]) + np.array(GOLD, np.float32) * (0.8 * (star > 0.02))[..., None]
            tx.engrave(ctx, rim, 0.0, color=GOLD)
            tx.put(ctx.glow, np.clip(star * 1.3, 0, 1), LIGHT, 0.7, 0.5, halo=0.8)
            tx.put(ctx.vein, rim, (255, 250, 220), 0.9, 0.8)
        else:
            tx.tint(ctx, (255, 244, 236), 0.3)
    bp.painters.append(plate_paint)
    if full:
        st = it.part("guild_star_" + tag, "gold", base=GOLD, smooth=25, density=1.3)
        G.rays(it, st, (X0, 33.0, 6.2), (0, 0, 1), 16, 0.4, 3.0, 1.5, 0.5, phase=0.0, long_every=2, long_k=1.55)
        gm = it.part("star_gem_" + tag, "crystal", base=(255, 224, 150), smooth=25)
        ml.gem(gm, (X0, 33.0, 7.2), 1.5, 1.2, 8, axis="z")
        gm.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), (255, 236, 180), 0.7, 0.6, halo=0.3))
        fg = it.part("finger_plates_" + tag, "silver", base=(246, 242, 238), smooth=30, density=1.1)
        for f in range(4):
            fx = X0 + (-4.2 + 2.8 * f) * 1.0
            for s_ in range(3):
                y0 = 28.0 + 4.2 * s_
                ml.loft(fg, "y", (fx, y0, 5.0 + 0.1 * s_), [(0.0, 1.0, 1.0), (3.5, 1.0, 1.0)], ml.octagon(2.4, 1.0, 0.3))
        gb = it.part("finger_bands_" + tag, "gold", base=GOLD, smooth=30)
        for f in range(4):
            fx = X0 + (-4.2 + 2.8 * f)
            for s_ in range(3):
                ml.bar(gb, (fx - 1.2, 28.0 + 4.2 * s_ + 3.6, 5.6), (fx + 1.2, 28.0 + 4.2 * s_ + 3.6, 5.6), 0.35, 0.28, up=(0, 0, 1), taper=0.5)
    tm = it.part("thumb_" + tag, "silver", base=(244, 240, 236), smooth=40, uv_mode="loft")
    G.tube(it, tm, [(X0 - sgn * 6.4, 27.0, 1.0), (X0 - sgn * 7.8, 31.0, 1.0), (X0 - sgn * 8.0, 35.0, 1.0)], 1.7, 1.2, sides=8)
    # ---- the knuckle crest: winged spikes over the fingers
    kn = it.part("knuckles_" + tag, "gold", base=(255, 220, 140), smooth=25, density=1.1)
    for f in range(4):
        fx = X0 + (-4.2 + 2.8 * f)
        G.cone(kn, (fx, 40.4, 4.2), (fx, 44.8 + (0.8 if f in (1, 2) else 0.0), 4.4), 1.0, 5)
        if full:
            G.cone(kn, (fx - 0.6, 41.0, 3.0), (fx - 1.2, 43.4, 2.4), 0.5, 4)
            G.cone(kn, (fx + 0.6, 41.0, 3.0), (fx + 1.2, 43.4, 2.4), 0.5, 4)
    if full:
        kw = it.part("knuckle_wings_" + tag, "silver", base=(252, 248, 244), smooth=30, uv_mode="loft")
        G.feather_fan(it, kw, (X0 + sgn * 6.2, 40.0, 3.4), (sgn * 0.8, 0.6, 0.0), 50.0, 4, 8.0, 2.0, thick=0.3, curl=0.05, side=(0, 0, 1), n_st=4)

    # ---- the fairy wings from the cuff: layered feathers sweeping out and back
    it.define_group("wing_" + tag, pivot=(X0 + sgn * 7.0, 24.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 3.5 * sgn, "period": 110.0, "phase": 0.2 * sgn}], lod_skip=False,
                    draw={"from_scale": 0.15, "delay": 2, "ticks": 14})
    wg = it.part("wing_feathers_" + tag, "silver", group="wing_" + tag, base=(250, 246, 250), smooth=35, uv_mode="loft", density=1.1)
    G.feather_fan(it, wg, (X0 + sgn * 7.0, 24.0, 0.0), (sgn * 1.0, 0.45, 0.0), 80.0, 7, 21.0, 3.6, thick=0.4, curl=0.04, side=(0, 0, 1), n_st=6, len_fn=lambda t: 0.62 + 0.38 * math.sin(math.pi * t))
    if full:
        wl = it.part("wing_lower_" + tag, "silver", group="wing_" + tag, base=(244, 238, 248), smooth=35, uv_mode="loft")
        G.feather_fan(it, wl, (X0 + sgn * 6.4, 22.0, 0.0), (sgn * 1.0, -0.6, 0.0), 60.0, 4, 13.0, 2.8, thick=0.35, curl=0.05, side=(0, 0, 1), n_st=5)
        wt = it.part("wing_tips_" + tag, "crystal", group="wing_" + tag, base=(255, 220, 240), smooth=20)
        for k in range(7):
            t = k / 6.0
            ang = math.radians(-40.0 + 80.0 * t)
            d = np.array([sgn * math.cos(ang) * 1.0, math.sin(ang) * 0.9 + 0.45, 0.0])
            d = d / np.linalg.norm(d)
            L = 21.0 * (0.62 + 0.38 * math.sin(math.pi * t)) + 0.6
            p0 = np.array([X0 + sgn * 7.0, 24.0, 0.0]) + d * L
            ml.gem(wt, p0, 0.42, 1.4, 4, axis=tuple(d))
        wt.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), PINK, 0.9, 0.8))

    def wing_paint(ctx):
        if ctx.kind != "loft":
            return
        barb = 0.5 + 0.5 * np.sin(ctx.B * 4.8 + np.abs(ctx.S - 0.5) * 36.0)
        ctx.img[..., :3] *= (0.85 + 0.2 * barb[..., None])
        tx.tint(ctx, (255, 232, 246), 0.35 * ctx.T)
        tx.put(ctx.glow, tx.soft(ctx.S, 0.09) * (0.3 + 0.7 * ctx.T), (255, 226, 246), 0.7, 0.5, halo=0.6)
        tx.put(ctx.vein, barb * ctx.T * 0.4, PINK, 0.5, 0.5)
    wg.painters.append(wing_paint)

    # ---- the glitter: a halo of crystal motes circling the fist
    it.define_group("glitter_" + tag, pivot=(X0, 34.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 70.0 * sgn}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.4, "period": 70.0, "phase": 0.3 * sgn}],
                    lod_skip=True, draw={"from_scale": 0.0, "delay": 4, "ticks": 14})
    if full:
        gl = it.part("glitter_" + tag, "crystal", group="glitter_" + tag, base=(255, 240, 196), smooth=20)
        G.shard_helix(it, gl, (X0, 34.0, 0.0), (0, 1, 0), 10.5, 12, 22.0, 1.5, 0.45, sides=4, turns=1.8, tilt=0.6)
        gl.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), LIGHT, 1.0, 0.8, halo=0.4))
    it.define_group("burst_" + tag, pivot=(X0, 34.0, 0.0), anim=[{"kind": "breathe", "min": 0.3, "max": 0.8, "period": 70.0, "phase": 0.25 * sgn}, {"kind": "spin", "axis": [0, 0, 1], "dps": 14.0 * sgn}],
                    tint="FFE8A0", lod_skip=True, alpha=0.5)
    G.card(it, "burst_card_" + tag, "burst_" + tag, (X0, 34.0, 8.6), (1, 0, 0), (0, 1, 0), 10.0, 10.0, "rays")
    G.card(it, "burst_card_" + tag, "burst_" + tag, (X0, 34.0, 8.2), (1, 0, 0), (0, 1, 0), 6.5, 6.5, "glow")
    it.flare((X0, 33.0, 8.8), 6.0, "FFE8A0", "star", phase=0.0 if sgn < 0 else 0.5, pulse=0.6)


def build(it, state=""):
    hand(it, -1, "l")
    hand(it, 1, "r")
    # a shared halo of the guild: a fairy ring over both fists and a veil of glitter between them
    it.define_group("law", pivot=(0.0, 33.0, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": -12.0}, {"kind": "breathe", "min": 0.3, "max": 0.7, "period": 120.0}], tint="FFD070", lod_skip=True,
                    alpha=0.8, draw={"from_scale": 0.0, "delay": 6, "ticks": 14})
    G.card(it, "law_ring", "law", (0.0, 33.0, 9.5), (1, 0, 0), (0, 1, 0), 21.0, 21.0, "runes")
    G.card(it, "law_ring", "law", (0.0, 33.0, 9.3), (1, 0, 0), (0, 1, 0), 15.0, 15.0, "hex")
    it.define_group("veil", pivot=(0.0, 30.0, 0.0), anim=[{"kind": "flicker", "min": 0.1, "max": 0.9, "rate": 3.0}], tint="FFFFFF", lod_skip=True, alpha=0.9)
    for k in range(6):
        G.card(it, "veil_%d" % k, "veil", (-10.0 + 4.0 * k, 28.0 + 7.0 * (k % 3), 7.6), (1, 0, 0), (0, 1, 0), 3.4, 3.4, "sparkle")
    it.glow = ("FFFFFF", 0.18, 0.5, 1.0)
    it.layers = [
        {"id": "fairy_wings", "kind": "mesh_sway", "group": "wing_l", "note": "two pairs of feathered wings sway at the cuffs"},
        {"id": "glitter_orbit", "kind": "orbit_shards", "group": "glitter_l", "note": "a spiral of crystal glitter circling each fist, counter-rotating"},
        {"id": "star_bursts", "kind": "light_shafts", "group": "burst_l", "note": "a turning fan of rays behind each guild star"},
        {"id": "law_ring", "kind": "halo_ring", "group": "law", "note": "the Fairy Law: a rune ring over both fists"},
        {"id": "sparkle_veil", "kind": "energy_arcs", "group": "veil", "note": "a veil of sparkles flickering between the hands"},
        {"id": "star_flares", "kind": "lens_flare", "flares": "stars", "note": "star flares on both guild stars"},
        {"id": "glitter_motes", "kind": "motes", "particle": "end_rod", "rgb": "FFE8A0", "where": "blade", "rate": 0.8, "radius": 0.6, "note": "glitter falling from the fists"},
        {"id": "shoulder_glow", "kind": "shoulder_glow", "particle": "dust", "rgb": "FFD070", "rate": 0.4, "note": "a warm glow at the shoulders"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "FFE8A0", "radius": 1.0, "note": "a ring of gold glitter on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "vein_flow", "kind": "vein_wave", "note": "light runs through the filigree of the vambraces"},
        {"id": "glitter_orbit_l", "kind": "group_spin", "group": "glitter_l"},
        {"id": "glitter_orbit_r", "kind": "group_spin", "group": "glitter_r"},
        {"id": "wing_sway", "kind": "group_sway", "group": "wing_l"},
        {"id": "law_turn", "kind": "group_spin", "group": "law"},
        {"id": "burst_breath", "kind": "group_breathe", "group": "burst_l"},
        {"id": "veil_flicker", "kind": "group_flicker", "group": "veil"},
    ]
    it.draw = {"ticks": 16, "flash": "FFE8A0", "groups": ["wing_l", "wing_r", "glitter_l", "glitter_r", "law"], "note": "the wings unfold and the glitter spirals out as the gauntlets are drawn"}
    it.swing = {"trail": "glitter", "rgb": "FFE8A0", "rgb2": "FFFFFF", "length": 0.8, "width": 0.3, "life": 8, "particles": "glitter"}
    it.sound = "nusmp:item.genesis.fairy_glitter.shimmer_loop"
    it.acts = {
        "glitter_barrage": {"burst": "glitter_barrage", "pattern": "cone", "rgb": "FFE8A0", "ring": 4.5, "particles": 40, "sound": "nusmp:item.genesis.fairy_glitter.barrage"},
        "fairy_glitter": {"burst": "fairy_glitter", "pattern": "nova", "rgb": "FFFFFF", "ring": 5.0, "particles": 46, "sound": "nusmp:item.genesis.fairy_glitter.glitter"},
        "law_of_the_guild": {"burst": "guild_law", "pattern": "ring", "rgb": "FFD070", "ring": 5.5, "particles": 40, "sound": "nusmp:item.genesis.fairy_glitter.law"},
    }

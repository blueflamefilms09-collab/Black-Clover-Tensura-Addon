"""Gae Bolg, Heart-Seeker (Wave 71, Genesis, Celtic myth): the barbed spear of the shadow isle, a hair of rope and a thorn of the sea beast. A wound from it will not close,
and it goes for the heart.

A black spear of sea-beast bone wound in blood-red sinew, ogham cut down the shaft, a long head that twists as it narrows and carries exactly thirty barbs (the act Thirty
Barbs), a wound of red light running up it, a heart-core glowing in the socket, a hair of crimson rope streaming from the socket, and a crown of thorns turning round the head with
drops of blood hanging in the air. Palette: 8A1A2A blood, 1A1A1A black bone, FF2A4A heart light.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (176.0, 258.0)
Y_S = 170.0
Y_TIP = 254.0
RED = (255, 42, 74)
BLOOD = (138, 26, 42)


def twisted_star(lobes, r_out, r_in, twist_per_cm, n=4):
    """A fluted section as a function of the station s (cm along the axis): a star of `lobes` lobes turning with the length."""
    def sec(s):
        pts = []
        m = lobes * 4
        for i in range(m):
            a = 2 * math.pi * i / m + s * twist_per_cm
            k = i % 4
            r = r_out if k == 0 else (r_in if k == 2 else (r_out + r_in) * 0.5)
            pts.append((r * math.cos(a), r * math.sin(a)))
        return pts
    return sec


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- shaft: black bone, ogham, a spiral of blood-red sinew
    sh = it.part("shaft", "obsidian", base=(30, 24, 30), smooth=50, uv_mode="loft", density=1.1, wear=0.8)
    G.rod(it, sh, 6.0, Y_S - 2.0, 1.7, 1.85, sides=14, swell=[(0.25, 1.1), (0.5, 1.0), (0.75, 1.1)])
    ogham = "ᚁᚂᚃᚄᚅᚆᚇᚈᚉᚊᚋᚌᚍᚎᚏᚐᚑᚒᚓᚔ"

    def shaft_paint(ctx):
        if ctx.kind != "loft":
            return
        spiral = np.abs(np.sin(ctx.B * 0.5 + ctx.S * 6.2832 * 1.0)) < 0.05
        tx.engrave(ctx, tx.blur(spiral.astype(np.float32), 0.7), 0.5)
        m = G.text_along(ctx, ogham, ctx.perim * 0.5, 0.75, 1.55, b0=20.0, b1=ctx.length - 20.0, fname="DejaVuSans.ttf", rot=90.0)
        tx.engrave(ctx, m, 0.7)
        tx.put(ctx.glow, m * 0.9, RED, 0.9, 0.55, halo=0.7)
        tx.put(ctx.vein, np.maximum(m, tx.blur(spiral.astype(np.float32), 0.7) * 0.5), RED, 0.9, 0.6)
    sh.painters.append(shaft_paint)
    sn = it.part("sinew_wrap", "cloth", base=(150, 24, 40), smooth=55, uv_mode="loft", density=1.2, wear=0.4)
    turns, r0 = 5.5, 2.0
    n_pts = it.n(88, 12)
    helix = [(r0 * math.cos(turns * 2 * math.pi * i / n_pts), 40.0 + 70.0 * i / n_pts, r0 * math.sin(turns * 2 * math.pi * i / n_pts)) for i in range(n_pts + 1)]
    G.tube(it, sn, helix, 0.34, 0.34, sides=5)
    if full:
        helix2 = [(r0 * math.cos(turns * 2 * math.pi * i / n_pts + math.pi), 40.0 + 70.0 * i / n_pts, r0 * math.sin(turns * 2 * math.pi * i / n_pts + math.pi)) for i in range(n_pts + 1)]
        G.tube(it, sn, helix2, 0.3, 0.3, sides=5)
        kn = it.part("sinew_knots", "cloth", base=(190, 36, 58), smooth=45)
        for k in range(8):
            y = 44.0 + 66.0 * k / 7.0
            G.orb(it, kn, (0.0, y, 0.0), 2.35, 0.9, 2.35, sides=10, stacks=4)

    def sinew_paint(ctx):
        weave = 0.5 + 0.5 * np.sin(ctx.B * 9.0 + ctx.S * 18.0)
        ctx.img[..., :3] *= (0.7 + 0.5 * weave[..., None])
        tx.put(ctx.glow, np.clip(weave - 0.7, 0, 1) * 0.5, RED, 0.5, 0.3)
    sn.painters.append(sinew_paint)
    bt = it.part("butt", "iron", base=(60, 44, 52), smooth=45, uv_mode="loft")
    ml.lathe(bt, "y", (0, 0, 0), [(-6.0, 0.0), (-3.0, 1.2), (0.0, 2.4), (4.0, 2.0), (8.0, 1.85)], sides=it.n(12, 6), caps=(False, False))
    if full:
        bs = it.part("butt_barbs", "iron", base=(120, 40, 56), smooth=25)
        for k in range(6):
            a = math.pi / 3 * k
            G.cone(bs, (2.0 * math.cos(a), 3.0, 2.0 * math.sin(a)), (3.6 * math.cos(a), -3.4, 3.6 * math.sin(a)), 0.5, 4)
    # a ferrule of bone rings below the head
    fr = it.part("shaft_rings", "bone", base=(90, 36, 44), smooth=40, uv_mode="loft")
    for y in (118.0, 122.0, 150.0, 154.0, 158.0):
        ml.collar(fr, y, 2.25, 1.6, lip=0.4 if full else 0.0, sides=it.n(12, 5))

    # ---------------------------------------------------------------- the socket: a flared crimson iron cup with a heart-core
    so = it.part("socket", "iron", base=(98, 20, 38), smooth=40, uv_mode="loft", density=1.2)
    ml.lathe(so, "y", (0, 0, 0), [(Y_S - 5.0, 1.9), (Y_S - 3.0, 3.6), (Y_S + 1.0, 4.4), (Y_S + 5.0, 3.6), (Y_S + 9.0, 2.6), (Y_S + 12.0, 2.2)], sides=it.n(18, 6), caps=(False, False))
    if full:
        lf = it.part("socket_leaves", "iron", base=(160, 40, 60), smooth=30)
        for k in range(8):
            a = math.pi / 4 * k
            G.cone(lf, (3.0 * math.cos(a), Y_S + 0.5, 3.0 * math.sin(a)), (5.6 * math.cos(a), Y_S + 11.0, 5.6 * math.sin(a)), 0.9, 4)
    core = it.part("heart_core", "crystal", base=(255, 40, 70), smooth=35, density=1.3)
    G.orb(it, core, (0.0, Y_S + 7.0, 0.0), 2.8, 3.2, 2.8, sides=12, stacks=8)

    def core_paint(ctx):
        n = tx.fbm(ctx.shape, 8.0, ctx.seed + 4, 3)
        ctx.img[..., :3] = tx.lerp3((120, 8, 28), (255, 70, 90), np.clip(n * 1.4, 0, 1))
        tx.put(ctx.glow, np.ones(ctx.shape, np.float32), RED, 1.0, 0.8, halo=0.5)
        tx.put(ctx.vein, tx.caustics(ctx, 6.0, 5, 2.0), (255, 200, 210), 0.9, 0.8)
    core.painters.append(core_paint)

    # ---------------------------------------------------------------- the head: a twisted fluted thorn with thirty barbs
    hd = it.part("head", "iron", base=(112, 14, 30), smooth=45, uv_mode="loft", density=1.3, wear=0.6)
    n_st = it.n(46, 8)
    st = []
    for i in range(n_st + 1):
        t = i / n_st
        y = Y_S + 10.0 + 74.0 * t
        r = 3.4 * (1 - t) ** 0.78 + 0.0
        st.append((y, r, r))
    st[-1] = (st[-1][0], 0.0, 0.0)
    ml.loft(hd, "y", (0, 0, 0), st, lambda s: twisted_star(4, 1.0, 0.5, 0.05)(s), caps=(False, False))

    def head_paint(ctx):
        if ctx.kind != "loft":
            return
        T, S, B = ctx.T, ctx.S, ctx.B
        flute = 0.5 + 0.5 * np.cos(S * 6.2832 * 4.0)
        ctx.img[..., :3] = ctx.img[..., :3] * (0.55 + 0.6 * flute[..., None])
        tx.tint(ctx, (255, 60, 80), 0.35 * T)
        # the wound that will not close: a seam of red light along each ridge, a heartbeat travelling up it
        seam = tx.soft(S * 4.0 - np.round(S * 4.0), 0.12) * (0.4 + 0.6 * T)
        web = tx.caustics(ctx, 9.0, 8, 2.2)
        tx.put(ctx.glow, np.clip(seam * 0.9 + web * 0.2 * T, 0, 1), RED, 0.95, 0.5, halo=0.9)
        tx.put(ctx.vein, np.clip(seam * 1.0, 0, 1), (255, 150, 170), 1.0, 0.7, halo=0.7)
    hd.painters.append(head_paint)
    # the thirty barbs: five rows of six, curving back toward the socket
    if full:
        bb = it.part("barbs", "bone", base=(120, 30, 46), smooth=25, uv_mode="loft", density=1.1)
        for row in range(5):
            t = 0.1 + 0.15 * row
            y = Y_S + 10.0 + 74.0 * t
            r = 3.4 * (1 - t) ** 0.78
            for k in range(6):
                a = 2 * math.pi * k / 6 + row * 0.52
                base = (r * 0.85 * math.cos(a), y, r * 0.85 * math.sin(a))
                tip = ((r + 4.6 - 0.45 * row) * math.cos(a), y - 6.4 + 0.5 * row, (r + 4.6 - 0.45 * row) * math.sin(a))
                G.cone(bb, base, tip, 0.85 - 0.08 * row, 5)

        def barb_paint(ctx):
            tx.tint(ctx, (255, 90, 110), 0.25 * ctx.T)
            tx.put(ctx.glow, np.clip(1.0 - ctx.T * 1.2, 0, 1) * 0.35, RED, 0.5, 0.4)
        bb.painters.append(barb_paint)

    # ---------------------------------------------------------------- the hair of rope: ribbons of crimson rope streaming from the socket
    it.define_group("hair", pivot=(0, Y_S - 4.0, 0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 5.0, "period": 70.0}, {"kind": "sway", "axis": [1, 0, 0], "deg": 4.0, "period": 95.0, "phase": 0.3}],
                    lod_skip=False)
    hp = it.part("rope_hair", "cloth", group="hair", base=(160, 22, 42), smooth=45, uv_mode="loft")
    n_h = it.n(12, 4)
    for k in range(n_h):
        a = 2 * math.pi * k / n_h
        pts = [(3.2 * math.cos(a), Y_S - 4.0, 3.2 * math.sin(a))]
        for j in range(1, 6):
            pts.append((3.2 * math.cos(a) + 1.2 * j * math.cos(a) + 0.5 * math.sin(a * 2 + j), Y_S - 4.0 - 4.4 * j, 3.2 * math.sin(a) + 1.2 * j * math.sin(a)))
        G.tube(it, hp, pts, 0.42, 0.14, sides=4, tip=True)
    hp.painters.append(lambda ctx: tx.put(ctx.glow, tx.soft(ctx.T - 1.0, 0.5) * 0.3, RED, 0.4, 0.3))

    # ---------------------------------------------------------------- the crown of thorns and the hanging drops of blood
    it.define_group("crown", pivot=(0, Y_S + 40.0, 0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 42.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.6, "period": 120.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 14})
    if full:
        cr = it.part("thorn_crown", "bone", group="crown", base=(110, 22, 40), smooth=40, uv_mode="loft")
        G.torus(it, cr, (0, Y_S + 40.0, 0), 11.0, 0.55, (0, 1, 0), major=40, minor=5)
        ct = it.part("crown_thorns", "bone", group="crown", base=(150, 34, 54), smooth=25)
        for k in range(18):
            a = 2 * math.pi * k / 18
            c = (11.0 * math.cos(a), Y_S + 40.0, 11.0 * math.sin(a))
            G.cone(ct, c, (11.0 * math.cos(a) + 2.6 * math.cos(a), Y_S + 40.0 + (4.4 if k % 2 == 0 else -4.0), 11.0 * math.sin(a) + 2.6 * math.sin(a)), 0.55, 4)
        cr.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.5, RED, 0.6, 0.4))
    it.define_group("drops", pivot=(0, Y_S + 30.0, 0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": -26.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 2.2, "period": 80.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 5, "ticks": 14})
    if full:
        dr = it.part("blood_drops", "crystal", group="drops", base=(190, 20, 44), smooth=45, uv_mode="loft")
        for k in range(9):
            a = 2 * math.pi * k / 9
            r = 7.0 + 1.6 * (k % 3)
            G.teardrop(it, dr, (r * math.cos(a), Y_S + 18.0 + 6.0 * k, r * math.sin(a)), (0, -1, 0), 4.2 - 0.2 * (k % 3), 0.9, sides=7)
        dr.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * (0.4 + 0.6 * ctx.T), RED, 0.9, 0.6, halo=0.4))

    # ---------------------------------------------------------------- the halo layer
    it.define_group("heartbeat", pivot=(0, Y_S + 7.0, 0), anim=[{"kind": "breathe", "min": 0.15, "max": 0.85, "period": 40.0}], tint="FF2A4A", lod_skip=True, alpha=0.85)
    G.card(it, "heart_glow", "heartbeat", (0, Y_S + 7.0, 0.0), (1, 0, 0), (0, 1, 0), 12.0, 12.0, "glow")
    G.card(it, "heart_glow", "heartbeat", (0, Y_S + 7.0, 0.0), (0, 0, 1), (0, 1, 0), 12.0, 12.0, "glow")
    it.define_group("tip_beam", anim=[{"kind": "breathe", "min": 0.2, "max": 0.7, "period": 70.0}], tint="FF4A6A", lod_skip=True, alpha=0.8)
    G.card(it, "tip_beam_a", "tip_beam", (0, Y_TIP + 14.0, 0.0), (1, 0, 0), (0, 1, 0), 4.0, 16.0, "shaft")
    G.card(it, "tip_beam_a", "tip_beam", (0, Y_TIP + 14.0, 0.0), (1, 0, 0), (0, -1, 0), 4.0, 16.0, "shaft")
    G.card(it, "tip_beam_b", "tip_beam", (0, Y_TIP + 14.0, 0.0), (0, 0, 1), (0, 1, 0), 4.0, 16.0, "shaft")
    it.define_group("wound_arcs", pivot=(0, Y_S + 50.0, 0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 5.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": 70.0}], tint="FF6A8A", lod_skip=True, alpha=0.9)
    for k in range(4):
        a = k * 1.57
        G.card(it, "wound_arc_%d" % k, "wound_arcs", (4.4 * math.cos(a), Y_S + 30.0 + 14.0 * k, 4.4 * math.sin(a)), (math.cos(a + 1.5), 0, math.sin(a + 1.5)), (0, 1, 0), 3.4, 7.0, "bolt")
    it.flare((0.0, Y_S + 7.0, 3.0), 6.5, "FF4A6A", "star", phase=0.0, pulse=0.7)
    it.flare((0.0, Y_S + 7.0, -3.0), 6.5, "FF4A6A", "star", phase=0.5, pulse=0.7)
    it.flare((0.0, Y_TIP - 1.0, 0.0), 5.0, "FFB0C0", "sparkle", phase=0.25, pulse=0.5)

    it.glow = ("FFFFFF", 0.22, 0.4, 1.0)
    it.layers = [
        {"id": "thorn_crown", "kind": "halo_ring", "group": "crown", "note": "a crown of eighteen thorns turning round the head"},
        {"id": "blood_drops", "kind": "orbit_shards", "group": "drops", "note": "nine drops of blood hanging and circling the head"},
        {"id": "rope_hair", "kind": "mesh_sway", "group": "hair", "note": "twelve ropes of crimson hair stream and sway from the socket"},
        {"id": "heartbeat", "kind": "light_shafts", "group": "heartbeat", "note": "the heart-core beats: a crossed glow pulsing at about 1.5 beats a second"},
        {"id": "tip_beam", "kind": "light_shafts", "group": "tip_beam", "note": "a thin beam of red light off the point"},
        {"id": "wound_arcs", "kind": "energy_arcs", "group": "wound_arcs", "note": "four flickering arcs of the wound that will not close"},
        {"id": "heart_flares", "kind": "lens_flare", "flares": "heart", "note": "star flares in the heart-core and on the point"},
        {"id": "blood_motes", "kind": "motes", "particle": "dust", "rgb": "FF2A4A", "where": "blade", "rate": 0.6, "radius": 1.8, "note": "red motes rising off the head"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "8A1A2A", "radius": 1.0, "note": "a ring of dark red motes on the ground"},
    ]
    it.idle = [
        {"id": "heartbeat", "kind": "group_breathe", "group": "heartbeat", "note": "the core beats"},
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "wound_flow", "kind": "vein_wave", "note": "a beat of light runs up the seams of the head"},
        {"id": "crown_turn", "kind": "group_spin", "group": "crown"},
        {"id": "drops_orbit", "kind": "group_spin", "group": "drops"},
        {"id": "hair_sway", "kind": "group_sway", "group": "hair"},
        {"id": "arc_flicker", "kind": "group_flicker", "group": "wound_arcs"},
        {"id": "beam_breath", "kind": "group_breathe", "group": "tip_beam"},
    ]
    it.draw = {"ticks": 16, "flash": "FF2A4A", "groups": ["crown", "drops"], "note": "the crown of thorns snaps open and the drops fall into their orbit as the spear is drawn"}
    it.swing = {"trail": "blood_thorn", "rgb": "FF2A4A", "rgb2": "8A1A2A", "length": 1.8, "width": 0.3, "life": 10, "particles": "blood_mote"}
    it.sound = "nusmp:item.genesis.gae_bolg.heartbeat_loop"
    it.acts = {
        "heart_thrust": {"burst": "heart_thrust", "pattern": "line", "rgb": "FF2A4A", "ring": 6.0, "particles": 30, "sound": "nusmp:item.genesis.gae_bolg.thrust"},
        "thirty_barbs": {"burst": "thirty_barbs", "pattern": "cone", "rgb": "FF4A6A", "ring": 7.0, "particles": 30, "sound": "nusmp:item.genesis.gae_bolg.barbs"},
        "wound_that_will_not_close": {"burst": "open_wound", "pattern": "inward", "rgb": "8A1A2A", "ring": 4.0, "particles": 40, "sound": "nusmp:item.genesis.gae_bolg.wound"},
    }

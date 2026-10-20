"""Starsplitter Hammer (Wave 71, Genesis, The Seven Deadly Sins): a hammer-head the size of a boulder on an iron haft: the stars are said to split on it, and by night it remembers.

A colossal hand-hewn bronze-gold head, its two striking faces split by star-shaped cracks that glow and show the void, a violet band of runes round its belly, rune plates
on its flanks, a spiked rim, an iron haft with a wrapped grip and a lanyard of chain; rocks of fallen stars orbit the head, a map of constellations turns behind it and a
ring of gravity breathes round it. Palette: C8A860 bronze, 3A2A5A violet, FFE070 star yellow.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (112.0, 182.0)
HY = 157.5                       # the head's centre height
HX = 43.8                        # half length of the head
YEL = (255, 224, 112)
VIOLET = (140, 100, 230)


def head_profile():
    return [(-HX, 15.0), (-HX + 1.4, 17.2), (-36.0, 19.6), (-28.0, 20.8), (-16.0, 21.8), (-8.0, 22.6), (8.0, 22.6), (16.0, 21.8), (28.0, 20.8), (36.0, 19.6), (HX - 1.4, 17.2), (HX, 15.0)]


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- the head: a hand-hewn lathe along x, bronze-gold
    hd = it.part("head", "bronze", base=(206, 172, 98), smooth=48, uv_mode="loft", density=1.4, wear=0.8, mat={"dent": 0.14, "patina": 0.3})
    prof = head_profile()
    ml.lathe(hd, "x", (0.0, HY, 0.0), prof, sides=it.n(28, 8), caps=(True, True), squash=0.88)
    if full:
        G.perturb(hd, 0.55, seed=3, freq=0.3)

    def head_paint(ctx):
        if ctx.kind == "dom" and ctx.axis == 0:                                  # a striking face: split by star cracks that glow and show the void
            Y, Z = ctx.Y - HY, ctx.Z
            r = np.hypot(Y, Z)
            m = tx.crack_star(ctx, (0.0 if True else 0.0, ctx.group.origin[1] - (ctx.group.ext[1] / 2.0)), 15.0, n=9, seed=int(ctx.sign + 3), width=0.5)
            tx.engrave(ctx, m, 0.0, color=(14, 8, 28))
            ctx.img[..., :3] *= (0.75 + 0.25 * np.clip(r / 15.0, 0, 1))[..., None]
            tx.put(ctx.glow, m, YEL, 1.0, 0.7, halo=1.6)
            tx.put(ctx.vein, m, (255, 250, 200), 0.9, 0.9)
            tx.stars(ctx, 0.004, 3, (255, 240, 190), 1.0, 1.0)
            tx.put(ctx.glow, tx.soft(r - 13.8, 0.7) * 0.5, YEL, 0.6, 0.5)
            return
        if ctx.kind != "loft":
            return
        S, B = ctx.S, ctx.B                                                      # B along the length, S round the head
        T = B / max(ctx.length, 1e-6)
        # the violet band of runes round the belly: an inlay with engraved glyphs that glow
        band = tx.soft(T - 0.5, 0.085)
        inlay_m = (band > 0.25).astype(np.float32)
        tx.inlay(ctx, inlay_m, (58, 40, 96), 0.45, 0.5)
        m = G.text_along(ctx, "".join(tx.rune_glyphs(5, 41)), 0.0, 1.1, 2.2, fname="DejaVuSans.ttf", rot=0.0) if False else np.zeros(ctx.shape, np.float32)
        glyphs = tx.rune_glyphs(8, 9)
        n_g = 14
        for i in range(n_g):
            a = (i + 0.5) * ctx.perim / n_g
            m = np.maximum(m, tx.mask_text(ctx, glyphs[i % 8][0], a, ctx.length * 0.5, 1.9, rot=0.0, fname="DejaVuSans.ttf"))
        tx.engrave(ctx, m * (band > 0.25), 0.4, color=(200, 170, 255))
        tx.put(ctx.glow, m * (band > 0.25), (190, 150, 255), 0.95, 0.6, halo=0.8)
        tx.put(ctx.vein, np.maximum(m, tx.soft(T - 0.5, 0.09) * 0.3), VIOLET, 0.85, 0.6)
        # star scars: cracks on the flanks that remember the night
        sc = tx.crack_star(ctx, (ctx.perim * 0.25, ctx.length * 0.28), 12.0, n=6, seed=17, width=0.3) + tx.crack_star(ctx, (ctx.perim * 0.75, ctx.length * 0.72), 12.0, n=6, seed=29, width=0.3)
        sc = np.clip(sc, 0, 1)
        tx.engrave(ctx, sc, 0.0, color=(24, 16, 40))
        tx.put(ctx.glow, sc * 0.8, YEL, 0.8, 0.6, halo=1.0)
    hd.painters.append(head_paint)

    # the star-split faces: the void shows through the cracks (layer 1), a flat star on each end
    for sgn in (1, -1):
        vd = it.part("void_face_%s" % ("r" if sgn > 0 else "l"), "steel", layer=1)
        pts = []
        for i in range(14):
            a = 2 * math.pi * i / 14
            r = 11.0 if i % 2 == 0 else 4.6
            pts.append((r * math.cos(a), r * math.sin(a)))
        cx = vd.add_vertex((sgn * (HX + 0.06), HY, 0.0))
        ring = [vd.add_vertex((sgn * (HX + 0.06), HY + y * 0.88, z)) for (y, z) in pts]
        for i in range(len(ring)):
            if sgn > 0:
                vd.add_face(cx, ring[(i + 1) % len(ring)], ring[(i + 1) % len(ring)], ring[i])
            else:
                vd.add_face(cx, ring[i], ring[(i + 1) % len(ring)], ring[(i + 1) % len(ring)])

    # ribs, the violet band and the spiked rims
    rb = it.part("head_ribs", "gold", base=(236, 200, 110), smooth=40, uv_mode="loft", density=1.2)
    for s_, r_ in ((-36.0, 19.7), (-30.0, 20.5), (30.0, 20.5), (36.0, 19.7)):
        ml.lathe(rb, "x", (s_ - 0.55, HY, 0.0), [(0.0, r_ + 0.3), (0.45, r_ + 1.0), (1.1, r_ + 1.0), (1.4, r_ + 0.3)], sides=it.n(28, 8), caps=(False, False), squash=0.88)
    if full:
        sp = it.part("rim_spikes", "bronze", base=(220, 184, 108), smooth=25)
        for sgn in (1, -1):
            for k in range(10):
                a = 2 * math.pi * k / 10 + 0.15
                bx = sgn * (HX - 0.6)
                G.cone(sp, (bx, HY + 14.0 * math.cos(a) * 0.88, 14.0 * math.sin(a)), (bx + sgn * 6.0, HY + 15.5 * math.cos(a) * 0.88, 15.5 * math.sin(a)), 1.5, 5)
        rv = it.part("rim_rivets", "gold", base=(250, 220, 130), smooth=40)
        for sgn in (1, -1):
            for k in range(16):
                a = 2 * math.pi * k / 16
                ml.stud(rv, (sgn * (HX - 3.6), HY + 17.0 * math.cos(a) * 0.88, 17.0 * math.sin(a)), 0.9, 0.7, (sgn * 0.3, math.cos(a) * 0.9, math.sin(a) * 0.9), 6)
    # the rune plates on both flanks
    pl = it.part("rune_plates", "gold", base=(120, 86, 160), smooth=35, density=1.3)
    for sgn in (1, -1):
        for x in (-20.0, 20.0):
            ml.box(pl, (x - 6.5, HY - 5.0, sgn * 19.0 - 0.8 if sgn < 0 else sgn * 19.0 - 0.8 + 0.0), (x + 6.5, HY + 5.0, sgn * 19.0 + 0.8 if sgn > 0 else sgn * 19.0 + 0.8), 0.4)

    def plate_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            return
        m = np.zeros(ctx.shape, np.float32)
        gl = tx.rune_glyphs(9, 77 + int(ctx.sign))
        for i, g in enumerate(gl[:6]):
            m = np.maximum(m, tx.mask_text(ctx, g[0], ((ctx.group.origin[0] + ctx.group.ext[0] / 2) - 5.0 + (i % 3) * 5.0) if ctx.sign >= 0 else 0.0, 0.0, 1.0, fname="DejaVuSans.ttf") * 0)
        edge = ctx.edge(2.0)
        tx.put(ctx.glow, edge * 0.7, (200, 160, 255), 0.8, 0.6, halo=0.8)
        tx.tint(ctx, (150, 110, 230), 0.4)
    pl.painters.append(plate_paint)

    # ---------------------------------------------------------------- the haft: iron, a wrapped grip, a socket collar, a butt spike with a chain lanyard
    hf = it.part("haft", "iron", base=(70, 66, 84), smooth=50, uv_mode="loft", density=1.15, wear=0.8)
    G.rod(it, hf, -6.0, HY - 20.0, 3.3, 3.5, sides=14, swell=[(0.2, 1.12), (0.24, 1.0)])

    def haft_paint(ctx):
        if ctx.kind != "loft":
            return
        m = np.zeros(ctx.shape, np.float32)
        rr = np.random.default_rng(ctx.seed)
        pts = [(float(rr.uniform(0.5, ctx.perim - 0.5)), float(rr.uniform(2, ctx.length - 2))) for _ in range(22)]
        for i in range(len(pts) - 1):
            m = np.maximum(m, tx.mask_polyline(ctx, [pts[i], pts[i + 1]], 0.14))
        for a, b in pts:
            m = np.maximum(m, tx.mask_ellipse(ctx, a, b, 0.35, 0.35))
        tx.engrave(ctx, m * 0.7, 0.4)
        tx.put(ctx.vein, m, YEL, 0.8, 0.6)
    hf.painters.append(haft_paint)
    gw = it.part("grip_wrap", "leather", base=(60, 38, 30), smooth=55, uv_mode="loft", density=1.2)
    for i in range(it.n(22, 6)):
        ml.collar(gw, 18.0 + 2.6 * i, 3.62, 1.7, lip=0.3 if full else 0.0, sides=it.n(14, 5), caps=(False, False))
    gb = it.part("grip_bands", "bronze", base=(206, 172, 98), smooth=40, uv_mode="loft")
    for y in (14.0, 77.0, HY - 30.0):
        ml.collar(gb, y, 4.1, 3.0, lip=0.7 if full else 0.0, sides=it.n(16, 6))
    so = it.part("socket", "bronze", base=(200, 160, 90), smooth=40, uv_mode="loft", density=1.2)
    ml.lathe(so, "y", (0, 0, 0), [(HY - 24.0, 3.8), (HY - 20.0, 6.6), (HY - 14.0, 7.2), (HY - 8.0, 6.4)], sides=it.n(16, 6), caps=(False, False))
    bt = it.part("butt", "bronze", base=(190, 150, 90), smooth=40, uv_mode="loft")
    ml.lathe(bt, "y", (0, 0, 0), [(-6.0, 0.0), (-4.0, 2.0), (-0.5, 4.4), (4.0, 3.9), (8.0, 3.5)], sides=it.n(14, 6), caps=(False, False))
    if full:
        rg = it.part("lanyard_ring", "bronze", base=(210, 176, 100), smooth=45, uv_mode="loft")
        G.torus(it, rg, (4.2, 1.0, 0.0), 2.0, 0.45, (0, 0, 1), major=20, minor=5)
    it.define_group("chain", pivot=(4.2, 0.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 8.0, "period": 70.0}, {"kind": "sway", "axis": [1, 0, 0], "deg": 5.0, "period": 90.0}], lod_skip=True)
    if full:
        ch = it.part("lanyard_chain", "iron", group="chain", base=(110, 104, 124), smooth=45, uv_mode="loft")
        G.chain(it, ch, [(4.2, -1.0, 0.0), (5.0, -8.0, 0.4), (5.4, -16.0, 0.0), (5.0, -24.0, -0.4)], link_len=2.2, link_w=1.3, tube_r=0.3, sides=4, up=(0, 0, 1))
        tsl = it.part("lanyard_tassel", "cloth", group="chain", base=(110, 70, 170), smooth=40, uv_mode="loft")
        for k in range(5):
            a = 2 * math.pi * k / 5
            G.tube(it, tsl, [(5.0 + 0.5 * math.cos(a), -25.0, 0.5 * math.sin(a)), (5.4 + 1.0 * math.cos(a), -29.0, 1.0 * math.sin(a)), (5.3 + 1.4 * math.cos(a), -33.0, 1.4 * math.sin(a))], 0.5, 0.15, sides=4, tip=True)

    # ---------------------------------------------------------------- rocks of fallen stars circling the head; the constellation map and the gravity ring
    it.define_group("meteors", pivot=(0.0, HY, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 28.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 2.2, "period": 130.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 16})
    if full:
        mt = it.part("meteor_rocks", "basalt", group="meteors", base=(60, 50, 80), smooth=45, uv_mode="loft")
        for k in range(6):
            a = 2 * math.pi * k / 6 + 0.3
            r = 40.0 + 4.0 * (k % 2)
            c = (r * math.cos(a), HY + 9.0 * math.sin(k * 1.9), r * math.sin(a))
            G.orb(it, mt, c, 3.6 + 0.6 * (k % 3), 3.1, 3.4, sides=8, stacks=5)
        G.perturb(mt, 1.1, seed=5, freq=0.9)

        def rock_paint(ctx):
            sc = tx.crack_star(ctx, (ctx.perim * 0.5, ctx.length * 0.5), 6.0, n=5, seed=3, width=0.3)
            tx.put(ctx.glow, np.clip(sc, 0, 1), YEL, 0.95, 0.7, halo=0.8)
            tx.put(ctx.vein, np.clip(sc, 0, 1), (255, 250, 210), 0.9, 0.8)
        mt.painters.append(rock_paint)
        tl = it.part("meteor_trails", "crystal", group="meteors", base=(255, 224, 140), smooth=30, uv_mode="loft")
        for k in range(6):
            a = 2 * math.pi * k / 6 + 0.3
            r = 40.0 + 4.0 * (k % 2)
            c = np.array([r * math.cos(a), HY + 9.0 * math.sin(k * 1.9), r * math.sin(a)])
            tdir = np.array([math.sin(a), 0.0, -math.cos(a)])
            G.teardrop(it, tl, c - tdir * 6.0, tdir, 9.0, 1.4, sides=6)
        tl.painters.append(lambda ctx: tx.put(ctx.glow, np.clip(1.0 - ctx.T, 0, 1) * 0.9, YEL, 0.9, 0.6, halo=0.5))
    it.define_group("starmap", pivot=(0.0, HY, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 6.0}, {"kind": "breathe", "min": 0.25, "max": 0.55, "period": 150.0}], tint="FFE070", lod_skip=True,
                    alpha=0.6, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "starmap_card", "starmap", (0.0, HY, -24.0), (1, 0, 0), (0, 1, 0), 34.0, 34.0, "runes")
    G.card(it, "starmap_card", "starmap", (0.0, HY, 24.0), (1, 0, 0), (0, -1, 0), 34.0, 34.0, "runes")
    it.define_group("gravity", pivot=(0.0, HY, 0.0), anim=[{"kind": "breathe", "min": 0.15, "max": 0.6, "period": 100.0}, {"kind": "pulse", "amp": 0.06, "period": 100.0}], tint="8C64E6", lod_skip=True, alpha=0.7)
    G.card(it, "gravity_ring", "gravity", (0.0, HY, 0.0), (1, 0, 0), (0, 0, 1), 52.0, 52.0, "thick_ring")
    it.define_group("face_glow", pivot=(0.0, HY, 0.0), anim=[{"kind": "breathe", "min": 0.3, "max": 0.8, "period": 75.0}], tint="FFE070", lod_skip=True, alpha=0.8)
    for sgn in (1, -1):
        G.card(it, "face_glow_%d" % sgn, "face_glow", (sgn * (HX + 1.0), HY, 0.0), (0, 1, 0), (0, 0, 1), 24.0, 24.0, "glow")
        G.card(it, "face_glow_%d" % sgn, "face_glow", (sgn * (HX + 1.4), HY, 0.0), (0, 1, 0), (0, 0, 1), 18.0, 18.0, "rays")
    it.define_group("sparks", pivot=(0.0, HY, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 5.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": -45.0}], tint="FFF2B0", lod_skip=True, alpha=0.9)
    for k in range(5):
        a = 2 * math.pi * k / 5
        G.card(it, "spark_%d" % k, "sparks", (30.0 * math.cos(a), HY + 8.0 * math.sin(k * 2.0), 30.0 * math.sin(a)), (math.cos(a + 1.57), 0, math.sin(a + 1.57)), (0, 1, 0), 4.0, 4.0, "star")
    for sgn in (1, -1):
        it.flare((sgn * (HX + 1.6), HY, 0.0), 13.0, "FFE070", "star", phase=0.0 if sgn > 0 else 0.5, pulse=0.6)

    it.glow = ("FFFFFF", 0.13, 0.4, 1.0)
    it.layers = [
        {"id": "meteor_orbit", "kind": "orbit_shards", "group": "meteors", "note": "six rocks of fallen stars with comet tails circling the head"},
        {"id": "starmap", "kind": "halo_ring", "group": "starmap", "note": "a constellation map turning behind and before the head"},
        {"id": "gravity_ring", "kind": "halo_ring", "group": "gravity", "note": "a ring of gravity breathing round the head, horizontal"},
        {"id": "face_glow", "kind": "light_shafts", "group": "face_glow", "note": "the star-cracks glow and breathe on both striking faces"},
        {"id": "void_cracks", "kind": "distortion", "layer": "void", "note": "the dimensional void shows through the star-split faces (reality shimmer)"},
        {"id": "star_sparks", "kind": "energy_arcs", "group": "sparks", "note": "five star sparks flickering round the head"},
        {"id": "face_flares", "kind": "lens_flare", "flares": "faces", "note": "star flares on both striking faces"},
        {"id": "lanyard_sway", "kind": "mesh_sway", "group": "chain", "note": "the chain lanyard swings"},
        {"id": "star_dust", "kind": "motes", "particle": "end_rod", "rgb": "FFE070", "where": "blade", "rate": 0.5, "radius": 1.6, "note": "star dust falling off the head"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "FFE070", "radius": 1.1, "note": "a ring of star dust on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "rune_flow", "kind": "vein_wave", "note": "light runs through the cracks and the rune band"},
        {"id": "meteor_orbit", "kind": "group_spin", "group": "meteors"},
        {"id": "starmap_turn", "kind": "group_spin", "group": "starmap"},
        {"id": "gravity_breath", "kind": "group_breathe", "group": "gravity"},
        {"id": "face_breath", "kind": "group_breathe", "group": "face_glow"},
        {"id": "spark_flicker", "kind": "group_flicker", "group": "sparks"},
        {"id": "chain_sway", "kind": "group_sway", "group": "chain"},
    ]
    it.draw = {"ticks": 18, "flash": "FFE070", "groups": ["meteors", "starmap"], "note": "the meteors spiral in and the star map opens as the hammer is drawn"}
    it.swing = {"trail": "falling_star", "rgb": "FFE070", "rgb2": "3A2A5A", "length": 1.8, "width": 0.5, "life": 12, "particles": "star_dust"}
    it.sound = "nusmp:item.genesis.starsplitter.night_loop"
    it.acts = {
        "meteor_smash": {"burst": "meteor_smash", "pattern": "nova", "rgb": "FFE070", "ring": 6.0, "particles": 46, "sound": "nusmp:item.genesis.starsplitter.smash"},
        "gravity_well": {"burst": "gravity_well", "pattern": "inward", "rgb": "8C64E6", "ring": 6.0, "particles": 46, "sound": "nusmp:item.genesis.starsplitter.well"},
        "starsplit": {"burst": "starsplit", "pattern": "column", "rgb": "FFF2B0", "ring": 6.0, "particles": 50, "sound": "nusmp:item.genesis.starsplitter.split"},
    }

"""Nekrathul's Pact Scythe (Wave 78, Genesis, the exclusive tombs): the scythe that reaped the devil's pacts: every contract it cuts, it keeps. Through the black blade runs the void the
Pact-Breaker opened and never closed.

A tall black snath bound in gold and written over with the clauses of the contracts it has cut, a devil's mask on the socket with horns and violet eyes, a huge crescent of black blade
with the dimensional void running through it, a chain with a wax-sealed pact hanging from the point, strips of parchment contracts streaming from the collar, souls circling
inside the crescent and a seal circle of the pact turning behind the head. Palette: 1C0E2C black, A04AFF violet, C070FF soul light.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (150.0, 216.0)
Y_S = 206.0                    # the socket
VIOLET = (160, 74, 255)
SOUL = (192, 112, 255)


def blade_path(n):
    pts = []
    for i in range(n + 1):
        t = i / n
        x = -66.0 * t
        y = Y_S + 9.0 * math.sin(math.pi * min(1.0, t * 1.15)) * (1 - t) ** 0.3 - 52.0 * t ** 2.4
        pts.append((x, y, 0.0))
    return pts


def width(t):
    return 2.0 + 10.6 * math.sin(math.pi * min(1.0, t * 0.92 + 0.04)) ** 0.8 * (1 - 0.1 * t) if t < 0.93 else 2.0 * (1 - t) / 0.07


def build(it, state=""):
    full = it.full
    # ---------------------------------------------------------------- the snath: black, bound in gold, written over with clauses
    sn = it.part("snath", "starmetal", base=(22, 14, 36), smooth=50, uv_mode="loft", density=1.2, wear=0.7)
    G.rod(it, sn, 2.0, Y_S - 4.0, 1.7, 1.85, sides=16, swell=[(0.15, 1.08), (0.16, 1.0), (0.5, 1.08), (0.51, 1.0)])
    clauses = "SI QVIS CONTRAHAT · ANIMA MEA · VNDE REDEAT · "

    def snath_paint(ctx):
        if ctx.kind != "loft":
            return
        m = G.text_along(ctx, clauses, ctx.perim * 0.5, 0.52, 1.2, b0=24.0, b1=ctx.length - 10.0, fname="DejaVuSerif-Bold.ttf")
        m2 = G.text_along(ctx, clauses[::-1], ctx.perim * 0.18, 0.5, 1.2, b0=24.0, b1=ctx.length - 10.0, fname="DejaVuSerif-Bold.ttf")
        mm = np.maximum(m, m2)
        tx.engrave(ctx, mm, 0.7)
        tx.put(ctx.glow, mm * 0.9, SOUL, 0.9, 0.5, halo=0.7)
        tx.put(ctx.vein, mm, VIOLET, 0.9, 0.7)
        tx.stars(ctx, 0.0015, 3, (220, 190, 255), 1.0, 0.8)
    sn.painters.append(snath_paint)
    gb = it.part("snath_bands", "gold", base=(160, 120, 70), smooth=40, uv_mode="loft", density=1.2)
    for y in (6.0, 30.0, 64.0, 100.0, 134.0, 168.0, Y_S - 14.0):
        ml.collar(gb, y, 2.2, 2.6, lip=0.6 if full else 0.0, sides=it.n(16, 6))
    gr = it.part("snath_grips", "leather", base=(30, 20, 40), smooth=55, uv_mode="loft", density=1.2)
    for i in range(it.n(14, 4)):
        ml.collar(gr, 84.0 + 2.1 * i - 4.0, 1.98, 1.4, lip=0.3 if full else 0.0, sides=it.n(12, 5), caps=(False, False))
        ml.collar(gr, 14.0 + 2.1 * i, 1.98, 1.4, lip=0.3 if full else 0.0, sides=it.n(12, 5), caps=(False, False))
    bt = it.part("butt", "gold", base=(150, 110, 66), smooth=40, uv_mode="loft")
    ml.lathe(bt, "y", (0, 0, 0), [(-6.0, 0.0), (-3.0, 1.4), (0.0, 2.6), (3.0, 2.2), (7.0, 1.85)], sides=it.n(14, 6), caps=(False, False))
    if full:
        bs = it.part("butt_claws", "starmetal", base=(30, 20, 50), smooth=25)
        for k in range(5):
            a = 2 * math.pi * k / 5
            G.cone(bs, (1.8 * math.cos(a), 2.0, 1.8 * math.sin(a)), (3.4 * math.cos(a), -3.6, 3.4 * math.sin(a)), 0.5, 4)

    # ---------------------------------------------------------------- the socket and the devil's mask
    so = it.part("socket", "gold", base=(150, 112, 66), smooth=40, uv_mode="loft", density=1.3)
    ml.lathe(so, "y", (0, 0, 0), [(Y_S - 10.0, 2.0), (Y_S - 7.0, 3.8), (Y_S - 3.0, 4.2), (Y_S + 0.0, 3.6), (Y_S + 3.0, 2.8), (Y_S + 6.0, 2.2)], sides=it.n(18, 6), caps=(False, False))
    mk = it.part("devil_mask", "bone", base=(210, 196, 214), smooth=42, density=1.4, wear=0.8)
    G.orb(it, mk, (0.0, Y_S - 3.0, 3.6), 3.0, 3.6, 2.0, sides=12, stacks=8)
    G.orb(it, mk, (0.0, Y_S - 3.0, -3.6), 3.0, 3.6, 2.0, sides=12, stacks=8)
    eyes = it.part("mask_eyes", "crystal", base=(190, 100, 255), smooth=30)
    for sgn in (1, -1):
        for sx in (-1, 1):
            ml.gem(eyes, (sx * 1.3, Y_S - 1.8, sgn * 5.1), 0.6, 1.2, 6 if full else 4, axis=(0, 0, sgn))
    eyes.painters.append(lambda ctx: (tx.put(ctx.glow, np.ones(ctx.shape, np.float32), SOUL, 1.0, 0.8, halo=0.4), tx.put(ctx.vein, np.ones(ctx.shape, np.float32) * 0.6, (240, 210, 255), 0.8, 0.8)))
    if full:
        hn = it.part("mask_horns", "bone", base=(190, 170, 196), smooth=40, uv_mode="loft")
        for sgn in (1, -1):
            for sx in (-1, 1):
                G.tube(it, hn, [(sx * 2.4, Y_S, sgn * 3.6), (sx * 4.6, Y_S + 3.6, sgn * 3.4), (sx * 5.6, Y_S + 8.4, sgn * 3.2), (sx * 4.8, Y_S + 12.6, sgn * 3.0)], 0.7, sides=6, tip=True)
        tt = it.part("mask_teeth", "bone", base=(240, 232, 244), smooth=25)
        for sgn in (1, -1):
            for k in range(6):
                ml.box(tt, (-1.8 + 0.7 * k - 0.2, Y_S - 6.4, sgn * 5.4 - 0.16), (-1.8 + 0.7 * k + 0.2, Y_S - 5.0, sgn * 5.4 + 0.16))

    def mask_paint(ctx):
        crack = tx.caustics(ctx, 26.0, 7, 3.0)
        ctx.img[..., :3] *= (1.0 - 0.45 * crack[..., None])
        tx.tint(ctx, (225, 210, 245), 0.4)
    mk.painters.append(mask_paint)

    # ---------------------------------------------------------------- the blade: a crescent of black, the void running through it, the clauses written along it
    bl = it.part("blade", "starmetal", base=(14, 8, 26), smooth=40, uv_mode="loft", density=1.5, wear=0.6)
    n = it.n(70, 12)
    path = blade_path(n)
    SEC = [(-1.0, 0.0), (-0.4, 0.55), (0.5, 0.8), (1.0, 0.55), (1.0, -0.55), (0.5, -0.8), (-0.4, -0.55)]
    ml.sweep(bl, path, SEC[::-1], scales=lambda t: (max(0.12, width(t)), 0.62 * (0.4 + 0.6 * min(1.0, 4.0 * t * (1 - t) + 0.2)) + 0.12), up=(0, 0, 1), caps=(True, True))

    MK = G.section_marks(SEC[::-1], 6.0, 0.5)

    def blade_paint(ctx):
        if ctx.kind != "loft":
            return
        S, T, B = ctx.S, ctx.T, ctx.B
        edge = tx.soft(S - MK["tip1"], 0.05)
        ctx.img[..., :3] += (np.array([210, 170, 255], np.float32) - ctx.img[..., :3]) * (0.55 * edge)[..., None]
        tx.put(ctx.glow, edge * np.clip(np.sin(np.pi * T) * 1.4, 0, 1), SOUL, 0.95, 0.7, halo=1.3)
        web = tx.caustics(ctx, 30.0, 5, 2.4)
        tx.put(ctx.vein, web * (0.3 + 0.7 * tx.soft(S - 0.4, 0.5)) * 0.8, (210, 150, 255), 0.85, 0.6)
        m = G.text_along(ctx, "PACTVM · FREGI · RETINEO · ", ctx.perim * 0.4, 0.62, 1.35, b0=6.0, b1=ctx.length - 6.0, fname="DejaVuSerif-Bold.ttf")
        tx.engrave(ctx, m, 0.7)
        tx.put(ctx.glow, m * 0.8, (230, 190, 255), 0.85, 0.6)
        tx.stars(ctx, 0.003, 8, (225, 190, 255), 1.0, 1.0)
    bl.painters.append(blade_paint)
    # the void (layer 1) along the blade, on both faces
    for sgn in (1, -1):
        vd = it.part("void_blade_%s" % ("a" if sgn > 0 else "b"), "steel", layer=1)
        m_ = 20 if full else 6
        ia, ib = [], []
        for k in range(m_ + 1):
            t = 0.08 + 0.80 * k / m_
            i = int(round(t * n))
            px, py, _ = path[i]
            w = width(t)
            z = sgn * (0.62 * (0.4 + 0.6 * min(1.0, 4.0 * t * (1 - t) + 0.2)) + 0.12) * 0.9 + sgn * 0.08
            ia.append(vd.add_vertex((px, py + w * 0.15, z)))
            ib.append(vd.add_vertex((px, py + w * 0.62, z)))
        for k in range(m_):
            if sgn > 0:
                vd.add_face(ia[k], ib[k], ib[k + 1], ia[k + 1])
            else:
                vd.add_face(ia[k], ia[k + 1], ib[k + 1], ib[k])
    if full:
        sp = it.part("spine_notches", "starmetal", base=(30, 16, 50), smooth=25)
        for k in range(12):
            t = 0.1 + 0.07 * k
            i = int(round(t * n))
            px, py, _ = path[i]
            nx, ny, _ = path[min(n, i + 1)]
            d = np.array([nx - px, ny - py, 0.0])
            d = d / np.linalg.norm(d)
            up = np.array([-d[1], d[0], 0.0])
            if up[1] < 0:
                up = -up
            G.cone(sp, (px, py + width(t) * 0.9, 0.0), (px + d[0] * 1.6, py + width(t) * 0.9 + 2.4 + 0.2 * (k % 3) + up[1] * 0.2, 0.0), 0.7, 4)
        gm = it.part("pact_seal", "gold", base=(150, 112, 66), smooth=35, uv_mode="loft", density=1.3)
        ml.lathe(gm, "z", (-3.0, Y_S - 1.0, 0.0), [(-0.8, 4.6), (-0.3, 5.4), (0.3, 5.4), (0.8, 4.6)], sides=20, caps=(True, True))

    # ---------------------------------------------------------------- the pact chain with a wax-sealed contract, and strips of contracts streaming from the collar
    it.define_group("chain", pivot=(-62.0, Y_S - 30.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 3.0, "period": 90.0}, {"kind": "sway", "axis": [1, 0, 0], "deg": 4.0, "period": 70.0}],
                    lod_skip=True)
    if full:
        ch = it.part("pact_chain", "gold", group="chain", base=(160, 122, 74), smooth=45, uv_mode="loft", wear=0.8)
        tip = path[-1]
        G.chain(it, ch, [(tip[0] + 2.0, tip[1] - 1.0, 0.0), (tip[0] + 5.0, tip[1] - 8.0, 0.4), (tip[0] + 10.0, tip[1] - 12.0, 0.0), (tip[0] + 16.0, tip[1] - 12.0, -0.4), (tip[0] + 22.0, tip[1] - 8.0, 0.0)],
                link_len=2.5, link_w=1.5, tube_r=0.3, sides=4, up=(0, 0, 1))
        sl = it.part("wax_seal", "cloth", group="chain", base=(150, 30, 80), smooth=45, uv_mode="loft")
        ml.lathe(sl, "z", (tip[0] + 24.0, tip[1] - 6.0, 0.0), [(-0.7, 2.4), (-0.2, 2.8), (0.5, 2.8), (0.9, 2.2)], sides=14, caps=(True, True))
        sl.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.5, (255, 90, 190), 0.8, 0.6, halo=0.4))
    it.define_group("contracts", pivot=(0.0, Y_S - 6.0, 0.0), anim=[{"kind": "sway", "axis": [0, 0, 1], "deg": 6.0, "period": 55.0}, {"kind": "sway", "axis": [1, 0, 0], "deg": 5.0, "period": 80.0, "phase": 0.4}],
                    lod_skip=False)
    ct = it.part("contract_strips", "cloth", group="contracts", base=(214, 196, 160), smooth=40, uv_mode="loft", density=1.2)
    for k in range(it.n(5, 2)):
        a = 2 * math.pi * k / 5 + 0.4
        pts = [(2.6 * math.cos(a), Y_S - 8.0, 2.6 * math.sin(a)), (3.8 * math.cos(a), Y_S - 16.0, 3.8 * math.sin(a) + 0.6), (4.6 * math.cos(a), Y_S - 26.0, 4.6 * math.sin(a) - 0.5),
               (4.4 * math.cos(a), Y_S - 36.0, 4.4 * math.sin(a) + 0.4)]
        ml.sweep(ct, [tuple(q) for q in G.catmull(pts, it.n(4, 2))], ml.rect_ring(1.0, 1.0), scales=lambda t: (1.6 * (1.0 - 0.3 * t), 0.06), up=(math.cos(a), 0, math.sin(a)), caps=(False, False))

    def contract_paint(ctx):
        ctx.img[..., :3] *= 0.9 + 0.2 * tx.fbm(ctx.shape, 5.0, ctx.seed + 1, 3)[..., None]
        m = G.text_along(ctx, "CONTRACTVS", ctx.perim * 0.5, 0.35, 0.8, fname="DejaVuSerif-Bold.ttf", rot=90.0)
        tx.engrave(ctx, m, 0.7)
        tx.put(ctx.glow, m * 0.6, SOUL, 0.7, 0.5)
        tx.put(ctx.glow, tx.soft(ctx.T - 1.0, 0.25) * 0.3, SOUL, 0.5, 0.4)
    ct.painters.append(contract_paint)

    # ---------------------------------------------------------------- the souls inside the crescent, the seal circle of the pact, arcs and the halo layer
    it.define_group("souls", pivot=(-30.0, Y_S - 24.0, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 30.0}, {"kind": "bob", "axis": [0, 0, 1], "amp": 1.4, "period": 110.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 14})
    if full:
        wp = it.part("soul_wisps", "crystal", group="souls", base=(190, 120, 255), smooth=40, uv_mode="loft")
        for k in range(7):
            a = 2 * math.pi * k / 7
            r = 13.0 + 3.0 * (k % 3)
            d = np.array([-math.sin(a), math.cos(a), 0.0])
            G.teardrop(it, wp, (-30.0 + r * math.cos(a), Y_S - 24.0 + r * math.sin(a), 0.6 * math.sin(k * 2.0)), d, 5.4, 1.1, sides=7)
        wp.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * (0.4 + 0.6 * ctx.T), SOUL, 1.0, 0.7, halo=0.5))
    it.define_group("seal", pivot=(-30.0, Y_S - 30.0, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": -8.0}, {"kind": "breathe", "min": 0.25, "max": 0.6, "period": 130.0}], tint="B070FF", lod_skip=True,
                    alpha=0.6, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "seal_card", "seal", (-30.0, Y_S - 30.0, -2.4), (1, 0, 0), (0, 1, 0), 26.0, 26.0, "runes")
    G.card(it, "seal_card", "seal", (-30.0, Y_S - 30.0, 2.4), (1, 0, 0), (0, -1, 0), 26.0, 26.0, "runes")
    G.card(it, "seal_hex", "seal", (-30.0, Y_S - 30.0, 2.0), (1, 0, 0), (0, 1, 0), 17.0, 17.0, "hex")
    it.define_group("edge_glow", anim=[{"kind": "breathe", "min": 0.2, "max": 0.6, "period": 90.0}], tint="C070FF", lod_skip=True, alpha=0.7)
    for sgn in (1, -1):
        G.card(it, "edge_glow", "edge_glow", (-32.0, Y_S - 20.0, sgn * 0.6), (1, 0, 0), (0, 1, 0), 30.0, 7.0, "streak")
    it.define_group("arcs", pivot=(-30.0, Y_S - 8.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 5.0}, {"kind": "spin", "axis": [0, 0, 1], "dps": 40.0}], tint="D8A8FF", lod_skip=True, alpha=0.9)
    for k in range(4):
        a = k * 1.57 + 0.4
        G.card(it, "arc_%d" % k, "arcs", (-30.0 + 20.0 * math.cos(a), Y_S - 8.0 + 20.0 * math.sin(a), 0.0), (math.cos(a + 1.57), math.sin(a + 1.57), 0), (math.cos(a), math.sin(a), 0), 3.6, 6.0, "bolt")
    for sgn in (1, -1):
        for sx in (-1, 1):
            it.flare((sx * 1.3, Y_S - 1.8, sgn * 5.4), 4.0, "C070FF", "star", phase=0.2 * sx + (0.5 if sgn < 0 else 0.0))
    it.flare((path[-1][0], path[-1][1], 0.0), 6.0, "D8A8FF", "star", phase=0.0, pulse=0.6)
    it.flare((-30.0, Y_S - 12.0, 3.0), 7.0, "B070FF", "sparkle", phase=0.4, pulse=0.6)

    it.glow = ("FFFFFF", 0.16, 0.45, 1.0)
    it.layers = [
        {"id": "souls", "kind": "orbit_shards", "group": "souls", "note": "seven souls circling inside the crescent"},
        {"id": "pact_seal", "kind": "halo_ring", "group": "seal", "note": "a seal circle of the pact (runes and a hexagon) turning behind the head, both faces"},
        {"id": "void_blade", "kind": "distortion", "layer": "void", "note": "the void the Pact-Breaker opened runs through the blade (reality shimmer)"},
        {"id": "edge_glow", "kind": "light_shafts", "group": "edge_glow", "note": "a violet glow along the cutting edge, breathing"},
        {"id": "pact_arcs", "kind": "energy_arcs", "group": "arcs", "note": "four flickering arcs of pact-light round the crescent"},
        {"id": "contract_strips", "kind": "mesh_sway", "group": "contracts", "note": "five parchment contracts stream and sway from the collar"},
        {"id": "pact_chain", "kind": "mesh_sway", "group": "chain", "note": "the chain with its wax seal swings from the point"},
        {"id": "mask_flares", "kind": "lens_flare", "flares": "mask eyes, point", "note": "star flares in the devil's eyes and at the point"},
        {"id": "soul_motes", "kind": "motes", "particle": "dust", "rgb": "C070FF", "where": "blade", "rate": 0.6, "radius": 2.0, "note": "violet soul motes rising off the blade"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "A04AFF", "radius": 1.0, "note": "a ring of violet motes on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "clause_flow", "kind": "vein_wave", "note": "light runs through the clauses on the snath and the blade"},
        {"id": "soul_orbit", "kind": "group_spin", "group": "souls"},
        {"id": "seal_turn", "kind": "group_spin", "group": "seal"},
        {"id": "arc_flicker", "kind": "group_flicker", "group": "arcs"},
        {"id": "edge_breath", "kind": "group_breathe", "group": "edge_glow"},
        {"id": "contract_sway", "kind": "group_sway", "group": "contracts"},
        {"id": "chain_sway", "kind": "group_sway", "group": "chain"},
    ]
    it.draw = {"ticks": 16, "flash": "C070FF", "groups": ["souls", "seal"], "note": "the seal circle unfolds behind the head and the souls spiral in as the scythe is drawn"}
    it.swing = {"trail": "reaping_arc", "rgb": "C070FF", "rgb2": "1C0E2C", "length": 2.4, "width": 0.5, "life": 13, "particles": "soul_mote"}
    it.sound = "nusmp:item.genesis.pact_scythe.whisper_loop"
    it.acts = {
        "pact_breaking_reap": {"burst": "pact_reap", "pattern": "cone", "rgb": "C070FF", "ring": 7.0, "particles": 46, "sound": "nusmp:item.genesis.pact_scythe.reap"},
        "soul_tithe": {"burst": "soul_tithe", "pattern": "inward", "rgb": "A04AFF", "ring": 4.5, "particles": 40, "sound": "nusmp:item.genesis.pact_scythe.tithe"},
    }

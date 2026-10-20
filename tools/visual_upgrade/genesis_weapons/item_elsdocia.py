"""Elsdocia (MagicWeaponItem.Kind.ELSDOCIA, Genesis grade since 1.97.0): the sword of the Wizard Kings, a broad blade of fractured crystal on a bronze ricasso cut with a clover.

The old build (1.29.0, from the owner's reference render) is kept and exceeded: the same proportions (pommel bulb and slim dark grip, a 39 cm guard bar with flared ends, a bronze ricasso
with the clover, a 68 cm blade of crystal, y 0 to 125) but the blade is now a mosaic of raised facet plates, each tilted and lit on its own, with a fractured edge, cracks of cyan light
between them and a bright core under the plates; the guard carries bosses and swept wings of its own, the clover is a raised bronze rim round a dark inlay; a ring of crystal shards
turns round the blade, a rune circle of the Wizard Kings turns behind the guard, and the held glitter of the learned magics (client/ElsdociaGlitterClient) is untouched.
Palette: 5EC8FF crystal light, BD8A4A bronze, DFF6FF white glass.
"""
import math

import numpy as np

import vu_geo2 as G
import vu_tex as tx
from vu_item import ml

TEX = 2048
FOCUS = (28.0, 76.0)
Y_G = 31.4                      # the guard bar's centre
Y_R0, Y_R1 = 32.0, 57.0         # the ricasso
Y_B0, Y_TIP = 57.0, 125.0       # the crystal blade
CY = (94, 200, 255)
WHITE = (223, 246, 255)
BRONZE = (189, 138, 74)


def edge_jit(i, n, seed):
    rng = np.random.default_rng(seed)
    return rng.uniform(-0.55, 0.55, n + 1)[i]


def blade_hw(y):
    t = (y - Y_B0) / (Y_TIP - Y_B0)
    return 8.5 if t < 0.81 else 8.5 * max(0.0, (1.0 - t) / 0.19) ** 0.9


def facet(part, x0, x1, y0, y1, zb, zt, rng, tilt=0.16, inset=0.12, sgn=1):
    """A raised crystal plate: a four-sided frustum with its own tilt (the corners of its top at slightly different heights); closed, so the winding check turns it outward."""
    w, h = x1 - x0, y1 - y0
    b = [part.add_vertex((x0, y0, zb)), part.add_vertex((x1, y0, zb)), part.add_vertex((x1, y1, zb)), part.add_vertex((x0, y1, zb))]
    ix, iy = w * inset, h * inset
    zs = [zt + sgn * rng.uniform(-tilt, tilt) for _ in range(4)]
    t = [part.add_vertex((x0 + ix, y0 + iy, zs[0])), part.add_vertex((x1 - ix, y0 + iy, zs[1])), part.add_vertex((x1 - ix, y1 - iy, zs[2])), part.add_vertex((x0 + ix, y1 - iy, zs[3]))]
    if sgn > 0:
        part.add_face(t[0], t[1], t[2], t[3])
        part.add_face(b[0], b[3], b[2], b[1])
    else:
        part.add_face(t[0], t[3], t[2], t[1])
        part.add_face(b[0], b[1], b[2], b[3])
    for i in range(4):
        j = (i + 1) % 4
        if sgn > 0:
            part.add_face(b[i], b[j], t[j], t[i])
        else:
            part.add_face(b[i], t[i], t[j], b[j])


def build(it, state=""):
    full = it.full
    rng = np.random.default_rng(1290)
    # ---------------------------------------------------------------- the crystal blade: a fractured lens, a bright core, a mosaic of raised plates
    n = it.n(64, 8)
    st = []
    for i in range(n + 1):
        y = Y_B0 - 0.5 + (Y_TIP - Y_B0 + 0.5) * i / n
        h = blade_hw(y)
        j = 0.0 if h < 1.0 else edge_jit(i, n, 3)
        k = 0.0 if h < 1.0 else edge_jit(i, n, 4)
        if i == n:
            h, j, k = 0.0, 0.0, 0.0
        st.append((y, max(0.0, h + j), max(0.0, h + k), 0.55 if h > 0.5 else 0.0))
    bl = it.part("crystal_blade", "crystal", base=(40, 110, 230), smooth=30, density=1.5, wear=0.2, mat={"dent": 0.05})
    G.blade(it, bl, st, section=G.blade_section(0.82, 0.0), caps=(True, True))
    core = it.part("crystal_core", "crystal", base=(120, 210, 255), smooth=40, density=1.2)
    G.blade_by(it, core, Y_B0 + 2.0, Y_TIP - 8.0, lambda t: 2.4 * (1.0 - 0.7 * t) + 0.6, lambda t: 0.88, n=24, bevel=0.5, tip=0.25)

    def crystal_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            return
        X, Y = ctx.X, ctx.Y
        f1, f2, ids = tx.voronoi(ctx.shape, max(4.0, 3.2 * ctx.ppu), ctx.seed + 3)
        crack = np.clip(1.0 - (f2 - f1) / max(1.5, 0.12 * ctx.ppu * 3.2), 0, 1)
        shade = 0.7 + 0.5 * ids
        col = tx.lerp3((22, 76, 214), (110, 196, 255), np.clip(0.2 + 0.7 * ids, 0, 1)) * shade[..., None]
        ctx.img[..., :3] = col
        ctx.img[..., 3] = 255
        ctx.img[..., :3] += (np.array([120, 215, 255], np.float32) - ctx.img[..., :3]) * (0.55 * crack)[..., None]
        edge = ctx.edge(2.0)
        tx.put(ctx.glow, np.clip(crack * 0.8 + edge * 0.5, 0, 1), CY, 0.95, 0.6, halo=0.9)
        tx.put(ctx.vein, crack, WHITE, 0.9, 0.8)
        tx.stars(ctx, 0.0025, 3, (255, 255, 255), 1.0, 1.0)
    bl.painters.append(crystal_paint)
    core.painters.append(lambda ctx: (tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.75, (170, 230, 255), 0.9, 0.7, halo=0.8), tx.put(ctx.vein, np.ones(ctx.shape, np.float32) * 0.6, WHITE, 0.8, 0.9)))
    # the mosaic: three columns by ten rows of raised plates on each face
    fct = it.part("facet_plates", "crystal", base=(60, 140, 240), smooth=16, density=1.4)
    if full:
        rows = 15
        for sgn in (1, -1):
            for r in range(rows):
                y0 = Y_B0 + 1.0 + (Y_TIP - Y_B0 - 14.0) * r / rows
                y1 = Y_B0 + 1.0 + (Y_TIP - Y_B0 - 14.0) * (r + 1) / rows - 0.25
                hw = blade_hw((y0 + y1) / 2) - 0.7
                cuts = sorted([-hw] + list(rng.uniform(-hw * 0.75, hw * 0.75, 3)) + [hw])
                for c in range(len(cuts) - 1):
                    x0, x1 = cuts[c] + 0.12, cuts[c + 1] - 0.12
                    if x1 - x0 < 0.5:
                        continue
                    if rng.random() < 0.55:                              # split along a diagonal: two plates
                        xm = (x0 + x1) / 2 + rng.uniform(-0.3, 0.3)
                        facet(fct, x0, xm, y0, y1, sgn * 0.5, sgn * 0.78, rng, 0.14, 0.1, sgn)
                        facet(fct, xm + 0.2, x1, y0, y1, sgn * 0.5, sgn * 0.78, rng, 0.14, 0.1, sgn)
                    else:
                        facet(fct, x0, x1, y0, y1, sgn * 0.5, sgn * 0.84, rng, 0.16, 0.12, sgn)
    if full:                                                                     # raised ridges along the seams between the rows of plates, and across the blade's heart
        rd = it.part("crack_ridges", "crystal", base=(150, 214, 255), smooth=25, density=1.2)
        for sgn in (1, -1):
            for r in range(1, 15):
                y = Y_B0 + 1.0 + (Y_TIP - Y_B0 - 14.0) * r / 15 - 0.12
                hw_ = blade_hw(y) - 0.9
                ml.bar(rd, (-hw_, y, sgn * 0.9), (hw_, y, sgn * 0.9), 0.32, 0.2, up=(0, 0, sgn), taper=0.8)
            for k in range(5):
                x = -5.0 + 2.5 * k
                ml.bar(rd, (x, Y_B0 + 4.0, sgn * 0.9), (x * 0.6, Y_TIP - 22.0, sgn * 0.9), 0.28, 0.18, up=(0, 0, sgn), taper=2.0)
        rd.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * 0.7, CY, 0.8, 0.6, halo=0.4))
    def facet_paint(ctx):
        if ctx.kind != "dom":
            return
        X, Y = ctx.X, ctx.Y
        f1, f2, ids = tx.voronoi(ctx.shape, max(3.0, 2.0 * ctx.ppu), ctx.seed + 9)
        ctx.img[..., :3] = tx.lerp3((30, 92, 226), (130, 206, 255), np.clip(0.15 + 0.75 * ids, 0, 1)) * (0.75 + 0.35 * ids)[..., None]
        ctx.img[..., 3] = 255
        edge = ctx.edge(1.6)
        ctx.img[..., :3] += (np.array([170, 226, 255], np.float32) - ctx.img[..., :3]) * (0.4 * edge)[..., None]
        tx.put(ctx.glow, edge * 0.7, CY, 0.8, 0.6, halo=0.6)
        tx.put(ctx.vein, edge * 0.7, WHITE, 0.8, 0.8)
    fct.painters.append(facet_paint)

    # ---------------------------------------------------------------- the bronze ricasso with the clover
    rc = it.part("ricasso", "bronze", base=BRONZE, smooth=35, density=1.4, wear=0.7, mat={"dent": 0.2})
    G.blade(it, rc, [(Y_R0, 7.2, 7.2, 0.95), (Y_R0 + 6.0, 7.2, 7.2, 0.95), (Y_R1, 8.4, 8.4, 0.8), (Y_R1 + 0.5, 8.4, 8.4, 0.5)], section=ml.octagon(2.0, 2.0, 0.55), caps=(True, True))
    CLY = 40.3

    def ricasso_paint(ctx):
        if ctx.kind != "dom" or ctx.axis != 2:
            return
        X, Y = ctx.X, ctx.Y - CLY
        # a four-leaf clover cut: four lobes and a stem, a dark inlay with an ember of cyan
        m = np.zeros(ctx.shape, np.float32)
        for (ox, oy) in ((-1.7, 1.5), (1.7, 1.5), (-1.7, -1.5), (1.7, -1.5)):
            m = np.maximum(m, tx.mask_ellipse(ctx, ox, CLY + oy, 2.1, 1.9))
        m = np.maximum(m, tx.mask_polyline(ctx, [(0.0, CLY - 1.0), (0.4, CLY - 4.4)], 0.9))
        tx.inlay(ctx, m, (20, 30, 52), 0.3, 0.7)
        tx.put(ctx.glow, m * 0.45, CY, 0.7, 0.4, halo=1.0)
        rim = np.clip(tx.blur(m, 1.8) - m, 0, 1)
        ctx.img[..., :3] += (np.array([255, 224, 150], np.float32) - ctx.img[..., :3]) * (0.8 * np.clip(rim * 3.0, 0, 1))[..., None]
        tx.engrave(ctx, tx.mask_text(ctx, "ELSDOCIA", 0.0, Y_R0 + 5.0, 1.15, fname="DejaVuSerif-Bold.ttf") + tx.mask_text(ctx, "REX MAGORVM", 0.0, Y_R1 - 3.4, 0.9, fname="DejaVuSerif-Bold.ttf"), 0.6)
    rc.painters.append(ricasso_paint)
    if full:
        cl = it.part("clover_rims", "bronze", base=(232, 188, 110), smooth=40, density=1.3)
        for sgn in (1, -1):
            for (ox, oy) in ((-1.7, 1.5), (1.7, 1.5), (-1.7, -1.5), (1.7, -1.5)):
                G.torus(it, cl, (ox, CLY + oy, sgn * 1.0), 2.25, 0.3, (0, 0, 1), major=18, minor=5)
    # ---------------------------------------------------------------- the guard bar: bronze, bosses, swept wings
    gb = it.part("guard_bar", "bronze", base=(200, 150, 84), smooth=35, uv_mode="loft", density=1.3, wear=0.7)
    ml.loft(gb, "x", (0.0, Y_G, 0.0), [(-19.6, 0.9, 0.9), (-16.0, 1.2, 1.1), (-6.0, 1.1, 1.0), (0.0, 1.5, 1.35), (6.0, 1.1, 1.0), (16.0, 1.2, 1.1), (19.6, 0.9, 0.9)], ml.octagon(2.0, 2.0, 0.5))
    if full:
        for sx in (-1, 1):
            wg = it.part("guard_wing_" + ("l" if sx < 0 else "r"), "bronze", base=(218, 168, 96), smooth=35, uv_mode="loft", density=1.2)
            G.feather_fan(it, wg, (sx * 17.0, Y_G, 0.0), (sx * 1.0, 0.6, 0.0), 78.0, 6, 11.0, 2.4, thick=0.5, curl=0.08, side=(0, 0, 1), n_st=4, len_fn=lambda t: 0.7 + 0.3 * math.sin(math.pi * t))
            ge = it.part("guard_end_" + ("l" if sx < 0 else "r"), "bronze", base=(225, 180, 104), smooth=35)
            ml.loft(ge, "y", (sx * 19.4, Y_G, 0.0), [(-2.2, 0.0, 0.0), (-1.5, 1.4, 1.1), (1.6, 1.5, 1.2), (2.2, 0.0, 0.0)], ml.octagon(2.0, 2.0, 0.4))
        bs = it.part("guard_bosses", "gold", base=(250, 210, 130), smooth=40)
        for x in (-12.0, -6.0, 0.0, 6.0, 12.0):
            for sgn in (1, -1):
                ml.stud(bs, (x, Y_G, sgn * 1.2), 0.9 if x else 1.5, 0.8, (0, 0, sgn), 8, 0.7)
        gem = it.part("guard_gem", "crystal", base=(120, 210, 255), smooth=25)
        for sgn in (1, -1):
            ml.gem(gem, (0.0, Y_G, sgn * 1.9), 1.1, 1.0, 8, axis=(0, 0, sgn))
        gem.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), CY, 1.0, 0.8, halo=0.5))
    gb.painters.append(lambda ctx: (tx.put(ctx.vein, tx.soft(ctx.S - 0.25, 0.1) * 0.5 + tx.soft(ctx.S - 0.75, 0.1) * 0.5, CY, 0.7, 0.6)))

    # ---------------------------------------------------------------- the grip and the pommel bulb
    gp = it.part("grip", "leather", base=(40, 30, 36), smooth=50, uv_mode="loft", density=1.2)
    G.rod(it, gp, 5.8, Y_G - 1.8, 1.55, 1.7, sides=12, swell=[(0.5, 1.12)])
    gw = it.part("grip_wire", "bronze", base=(210, 170, 100), smooth=55, uv_mode="loft", density=1.2)
    n_w = it.n(60, 8)
    G.tube(it, gw, [(1.9 * math.cos(8.0 * 2 * math.pi * i / n_w), 6.6 + (Y_G - 10.0) * i / n_w, 1.9 * math.sin(8.0 * 2 * math.pi * i / n_w)) for i in range(n_w + 1)], 0.2, sides=4)
    pb = it.part("pommel_bulb", "bronze", base=(200, 150, 84), smooth=45, uv_mode="loft", density=1.4, wear=0.7)
    G.orb(it, pb, (0.0, 2.9, 0.0), 3.1, 2.9, 3.1, sides=16, stacks=10)
    pr = it.part("pommel_ring", "gold", base=(240, 200, 124), smooth=45, uv_mode="loft")
    G.torus(it, pr, (0.0, 2.9, 0.0), 3.15, 0.38, (1, 0, 0), major=24, minor=5)
    pg = it.part("pommel_gem", "crystal", base=(110, 205, 255), smooth=25)
    for sgn in (1, -1):
        ml.gem(pg, (0.0, 2.9, sgn * 2.7), 1.2, 0.9, 8, axis=(0, 0, sgn))
    pg.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32), CY, 1.0, 0.85, halo=0.5))
    gc = it.part("grip_collars", "gold", base=(236, 196, 120), smooth=40, uv_mode="loft")
    ml.collar(gc, 5.6, 2.1, 1.3, lip=0.3 if full else 0.0, sides=it.n(14, 6))
    ml.collar(gc, Y_G - 3.0, 2.2, 1.3, lip=0.3 if full else 0.0, sides=it.n(14, 6))

    # ---------------------------------------------------------------- the ring of crystal shards, the rune circle of the Wizard Kings, the halo layer
    it.define_group("shards", pivot=(0.0, 92.0, 0.0), anim=[{"kind": "spin", "axis": [0, 1, 0], "dps": 30.0}, {"kind": "bob", "axis": [0, 1, 0], "amp": 1.8, "period": 120.0}], lod_skip=True,
                    draw={"from_scale": 0.0, "delay": 3, "ticks": 16})
    if full:
        sh = it.part("crystal_shards", "crystal", group="shards", base=(150, 214, 255), smooth=20)
        G.shard_helix(it, sh, (0.0, 92.0, 0.0), (0, 1, 0), 13.5, 14, 56.0, 3.4, 0.9, sides=4, turns=2.0, tilt=0.35)
        sh2 = it.part("crystal_shards_big", "crystal", group="shards", base=(190, 236, 255), smooth=20)
        G.shard_helix(it, sh2, (0.0, 92.0, 0.0), (0, 1, 0), 17.5, 6, 44.0, 5.2, 1.3, sides=4, turns=1.0, tilt=0.2, phase=1.3)
        for q in (sh, sh2):
            q.painters.append(lambda ctx: tx.put(ctx.glow, np.ones(ctx.shape, np.float32) * (0.4 + 0.6 * ctx.T), (190, 236, 255), 1.0, 0.7, halo=0.4))
    it.define_group("circle", pivot=(0.0, Y_G + 2.0, 0.0), anim=[{"kind": "spin", "axis": [0, 0, 1], "dps": 10.0}, {"kind": "breathe", "min": 0.3, "max": 0.7, "period": 130.0}], tint="5EC8FF", lod_skip=True,
                    alpha=0.7, draw={"from_scale": 0.0, "delay": 2, "ticks": 14})
    G.card(it, "circle_card", "circle", (0.0, Y_G + 2.0, -2.4), (1, 0, 0), (0, 1, 0), 24.0, 24.0, "runes")
    G.card(it, "circle_card", "circle", (0.0, Y_G + 2.0, 2.4), (1, 0, 0), (0, -1, 0), 24.0, 24.0, "runes")
    G.card(it, "circle_hex", "circle", (0.0, Y_G + 2.0, 2.0), (1, 0, 0), (0, 1, 0), 15.0, 15.0, "hex")
    it.define_group("blade_light", anim=[{"kind": "breathe", "min": 0.2, "max": 0.55, "period": 100.0}], tint="8AD8FF", lod_skip=True, alpha=0.7)
    G.card(it, "blade_light", "blade_light", (0.0, 92.0, 0.0), (1, 0, 0), (0, 1, 0), 14.0, 42.0, "glow")
    G.card(it, "blade_light", "blade_light", (0.0, 92.0, 0.0), (0, 0, 1), (0, 1, 0), 10.0, 42.0, "glow")
    it.define_group("glints", pivot=(0.0, 92.0, 0.0), anim=[{"kind": "flicker", "min": 0.0, "max": 1.0, "rate": 4.0}, {"kind": "spin", "axis": [0, 1, 0], "dps": 25.0}], tint="FFFFFF", lod_skip=True, alpha=0.9)
    for k in range(6):
        a = k * 1.1
        G.card(it, "glint_%d" % k, "glints", (5.5 * math.cos(a), 66.0 + 10.5 * k, 5.5 * math.sin(a)), (math.cos(a + 1.57), 0, math.sin(a + 1.57)), (0, 1, 0), 2.3, 2.3, "sparkle")
    it.define_group("lancets", pivot=(0.0, 92.0, 0.0), anim=[{"kind": "flicker", "min": 0.1, "max": 1.0, "rate": 3.0}], tint="BFEAFF", lod_skip=True, alpha=0.8)
    for sgn in (1, -1):
        G.card(it, "crack_light", "lancets", (0.0, 98.0, sgn * 0.9), (1, 0, 0), (0, 1, 0), 9.0, 22.0, "rays")
    it.flare((0.0, Y_G, 3.0), 6.0, "5EC8FF", "star", phase=0.0, pulse=0.6)
    it.flare((0.0, Y_G, -3.0), 6.0, "5EC8FF", "star", phase=0.5, pulse=0.6)
    it.flare((0.0, 2.9, 3.6), 4.5, "5EC8FF", "star", phase=0.25)
    it.flare((0.0, Y_TIP - 2.0, 0.0), 6.0, "FFFFFF", "sparkle", phase=0.6, pulse=0.6)

    it.glow = ("FFFFFF", 0.1, 0.45, 1.0)
    it.layers = [
        {"id": "crystal_shards", "kind": "orbit_shards", "group": "shards", "note": "twenty shards of crystal spiralling round the blade"},
        {"id": "wizard_circle", "kind": "halo_ring", "group": "circle", "note": "a rune circle of the Wizard Kings (runes and a hexagon) turning behind the guard, both faces"},
        {"id": "blade_light", "kind": "light_shafts", "group": "blade_light", "note": "a soft column of crystal light along the blade, breathing"},
        {"id": "crack_light", "kind": "light_shafts", "group": "lancets", "note": "lancets of light from the cracks, flickering"},
        {"id": "glints", "kind": "energy_arcs", "group": "glints", "note": "six glints of light running up the blade"},
        {"id": "gem_flares", "kind": "lens_flare", "flares": "guard gem, pommel, point", "note": "star flares in the guard gem, the pommel and at the point"},
        {"id": "crystal_dust", "kind": "motes", "particle": "end_rod", "rgb": "5EC8FF", "where": "blade", "rate": 0.6, "radius": 2.0, "note": "crystal dust falling off the blade (beside the held glitter of the learned magics)"},
        {"id": "ground_cast", "kind": "ground_glow", "particle": "dust", "rgb": "5EC8FF", "radius": 1.0, "note": "a ring of crystal motes on the ground"},
    ]
    it.idle = [
        {"id": "glow_breath", "kind": "glow_pulse"},
        {"id": "crack_flow", "kind": "vein_wave", "note": "light runs through the cracks between the plates"},
        {"id": "shard_orbit", "kind": "group_spin", "group": "shards"},
        {"id": "circle_turn", "kind": "group_spin", "group": "circle"},
        {"id": "column_breath", "kind": "group_breathe", "group": "blade_light"},
        {"id": "lancet_flicker", "kind": "group_flicker", "group": "lancets"},
        {"id": "glint_flicker", "kind": "group_flicker", "group": "glints"},
    ]
    it.draw = {"ticks": 16, "flash": "5EC8FF", "groups": ["shards", "circle"], "note": "the shards spiral out of the blade and the rune circle opens behind the guard as the sword is drawn"}
    it.swing = {"trail": "crystal_arc", "rgb": "5EC8FF", "rgb2": "DFF6FF", "length": 2.3, "width": 0.5, "life": 12, "particles": "crystal_glint"}
    it.sound = "nusmp:item.genesis.elsdocia.crystal_hum_loop"
    it.acts = {
        "legacy_release": {"burst": "legacy_release", "pattern": "nova", "rgb": "5EC8FF", "ring": 6.0, "particles": 46, "sound": "nusmp:item.genesis.elsdocia.release"},
        "legacy_gate": {"burst": "legacy_gate", "pattern": "ring", "rgb": "DFF6FF", "ring": 4.5, "particles": 40, "sound": "nusmp:item.genesis.elsdocia.gate"},
    }

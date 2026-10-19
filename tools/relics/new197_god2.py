"""God grade weapons of 1.97.0, second half: the Wand of the Wisdom King, the Dragon Force Twin Fangs, the Calamity Wave Aegis, Hellblaze, the Minotaur King's Labrys, the Dawnpiercer Crossbow."""
import math

import numpy as np

from new197_kit2 import *            # noqa: F401,F403
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft, extrude, arc_pts, ring_blade,
                         catmull, sweep_blade, cuboid, guard_cross, guard_wings, guard_disc, guard_leaf, guard_horns, guard_ring, pommel, straight_sword)
from new197_lib import paint, put_glow, sm, step, darken, paint_glow, paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, new_mesh, new_atlas, lay_and_paint, finish
from new197_legend import glow_parts, fuller_pair
from new197_god import helix_pts


def build_wand_of_the_wisdom_king():
    """Wand of the Wisdom King: a slim six-sided wand of pale blue crystal with silver and violet bands, a claw cradle and a floating core crystal with an orbiting ring."""
    m = new_mesh("wand_of_the_wisdom_king")
    Y1 = 34.0
    p = P(m, "shaft", "crystal", base=(176, 206, 255), glow=True, smooth=20, uv_mode="loft")
    ml.loft(p, "y", (0, 0, 0), [(0.0, 0.9, 0.9), (6.0, 1.4, 1.4), (Y1 * 0.6, 1.3, 1.3), (Y1, 0.8, 0.8)], ml.circle(1.0, 6))
    for y in (7.0, 18.0, Y1 - 3.0):
        collar_ring(m, y, 1.9, 1.6, "silver", (214, 220, 240), name="band_%d" % int(y), sides=6)
    grip(m, 0.0, 6.0, r=1.5, base=(70, 60, 140), wrap_base=(200, 200, 240), n_wraps=3, sides=8)
    lathe_y(m, "butt", "silver", -2.0, [(0.0, 0.0), (0.8, 1.4), (2.0, 1.6)], sides=8, base=(210, 216, 238), caps=(False, True))
    cradle = P(m, "cradle", "silver", base=(210, 216, 240), smooth=40)
    for k in range(4):
        a = math.pi / 2 * k
        pts = [(1.0 * math.cos(a) * (1 + 2.0 * i / 6.0), Y1 + 8.0 * (i / 6.0) ** 1.3, 1.0 * math.sin(a) * (1 + 2.0 * i / 6.0)) for i in range(7)]
        ml.sweep(cradle, pts, ml.octagon(2.0, 2.0, 0.5), scales=lambda t: (0.5 * (1 - 0.6 * t) + 0.1, 0.5 * (1 - 0.6 * t) + 0.1))
    core = P(m, "core_crystal", "crystal", base=(150, 190, 255), glow=True, smooth=25)
    ml.gem(core, (0.0, Y1 + 8.0, 0.0), 3.0, 1.8, 8)
    tube(m, "orbit_ring", "silver", [(6.0 * math.cos(a), Y1 + 8.0 + 2.0 * math.sin(a), 6.0 * math.sin(a) * 0.4 + 0.0) for a in np.linspace(0, 2 * math.pi, 25)], 0.35, 0.35, sides=6, base=(220, 226, 250), glow=True)
    sat = P(m, "satellites", "crystal", base=(190, 150, 255), glow=True, smooth=25)
    for k in range(3):
        a = 2 * math.pi * k / 3
        ml.gem(sat, (6.0 * math.cos(a), Y1 + 8.0 + 2.0 * math.sin(a), 2.4 * math.sin(a)), 0.9, 1.3, 4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["core_crystal", "orbit_ring"], (180, 210, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["satellites"], (210, 170, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["shaft"], (150, 190, 255), 0.3, 0.3)
    finish("wand_of_the_wisdom_king", m, atlas, "wand", glow=("FFFFFF", 0.17, 0.5, 1.0))


def build_dragon_force_twin_fangs():
    """Dragon Force Twin Fangs: two curved dragon-fang daggers side by side, bone-white blades with ember-lit spines, red cord grips and ring pommels."""
    m = new_mesh("dragon_force_twin_fangs")
    for sx, sf in ((-7.5, "_l"), (7.5, "_r")):
        N = 14
        pts = [(sx - 8.5 * (i / N) ** 2.0, 12.0 + 40.0 * i / N) for i in range(N + 1)]
        blade_curve(m, "blade" + sf, "bone", pts, lambda t: 3.2 * (1.0 - 0.7 * t ** 1.2) + 0.2, lambda t: 0.6 * (1.0 - 0.5 * t), bevel=0.5, base=(238, 230, 212), smooth=45)
        ember = P(m, "ember_spine" + sf, "obsidian", base=(255, 110, 60), glow=True, smooth=40)
        for i in range(2, N - 2):
            x, y = pts[i]
            ml.bar(ember, (x + 2.5 * (1.0 - 0.7 * (i / N) ** 1.2), y, 0.5), (pts[i + 1][0] + 2.5 * (1.0 - 0.7 * ((i + 1) / N) ** 1.2), pts[i + 1][1], 0.5), 0.5, 0.15, up=(0, 0, 1))
        gd = P(m, "guard" + sf, "gold", base=(180, 48, 40), smooth=40)
        ml.loft(gd, "x", (0, 11.5, 0), [(sx - 4.5, 0.9, 1.4), (sx - 3.0, 1.2, 1.8), (sx + 3.0, 1.2, 1.8), (sx + 4.5, 0.9, 1.4)], ml.octagon(2.0, 2.0, 0.5))
        gp = P(m, "grip" + sf, "cloth", base=(190, 40, 36), smooth=55, uv_mode="loft")
        ml.loft(gp, "y", (sx, 0, 0), [(2.0, 1.3, 1.3), (6.0, 1.6, 1.6), (11.0, 1.4, 1.4)], ml.circle(1.0, 8))
        tube(m, "pommel_ring" + sf, "gold", [(sx + 2.2 * math.cos(a), 0.4, 2.2 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 15)], 0.5, 0.5, sides=6, base=(232, 186, 76))
        cd = P(m, "cord" + sf, "cloth", base=(240, 224, 196), wear=0.3, smooth=60)
        for i in range(5):
            ml.collar(cd, 2.9 + i * 1.7, 1.5, 0.55, lip=0.16, sides=8, origin=(sx, 0, 0), caps=(False, False))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["ember_spine_l", "ember_spine_r"], (255, 120, 60), 1.0, 0.5)
    for sf in ("_l", "_r"):
        paint_glow(atlas, m.part("blade" + sf), (255, 150, 90), 0.12, 0.3)
    finish("dragon_force_twin_fangs", m, atlas, "dagger", glow=("FFFFFF", 0.17, 0.4, 1.0))


def build_calamity_wave_aegis():
    """Calamity Wave Aegis: a heater shield of sea-blue steel with a gold border, a sapphire boss, three gold wave crests across the face, rim studs and a strap behind."""
    m = new_mesh("calamity_wave_aegis")
    left = [(-25.0, 36.0), (-26.0, 24.0), (-24.5, 8.0), (-20.0, -10.0), (-11.0, -28.0)]
    outline = left + [(0.0, -42.0)] + [(-x, y) for (x, y) in reversed(left)] + [(0.0, 39.0)]
    scaled = lambda k: [(x * k, y * k) for (x, y) in outline]
    extrude(m, "rim", "gold", scaled(1.07), 1.1, edge=0.97, base=(232, 188, 80), z0=-0.4)
    extrude(m, "face", "steel", outline, 1.7, edge=0.9, base=(40, 88, 168), smooth=35, z0=0.6)
    boss = P(m, "boss", "gold", base=(236, 194, 86), smooth=45, uv_mode="loft")
    ml.lathe(boss, "z", (0, 8.0, 2.0), [(0.0, 7.0), (1.6, 6.2), (3.4, 3.6), (4.2, 0.0)], sides=16, caps=(True, False))
    sap = P(m, "sapphire", "crystal", base=(80, 170, 255), glow=True, smooth=35)
    ml.gem(sap, (0.0, 8.0, 5.8), 3.0, 1.0, 8, axis="z")
    crest = P(m, "wave_crests", "gold", base=(236, 194, 86), glow=True, smooth=40)
    for k, y0 in enumerate((-8.0, -18.0, -28.0)):
        wid = 17.0 - 5.0 * k
        pts = [(-wid + 2.0 * wid * i / 12.0, y0 + 2.6 * math.sin(i * 0.9 + k), 2.5) for i in range(13)]
        ml.sweep(crest, pts, ml.lozenge(2.0, 2.0), scales=lambda t: (0.55 * (1 - 0.6 * abs(2 * t - 1) ** 2), 0.5), up=(0, 0, 1))
    stu = P(m, "studs", "gold", base=(236, 194, 86), smooth=40)
    for i, (x, y) in enumerate(outline[:-1]):
        ml.stud(stu, (x * 0.93, y * 0.93, 2.3), 0.8, 0.8, (0, 0, 1), 6, 0.5)
    strap = P(m, "strap", "leather", base=(80, 56, 38), smooth=50, uv_mode="loft")
    ml.sweep(strap, [(-11.0, 6.0, -2.0), (-11.0, 6.0, -6.0), (11.0, 6.0, -6.0), (11.0, 6.0, -2.0)], ml.rounded_rect(1.0, 1.0, 0.3, 2), scales=lambda t: (2.4, 0.7), caps=(True, True))
    ml.sweep(strap, [(-11.0, -10.0, -2.0), (-11.0, -10.0, -6.0), (11.0, -10.0, -6.0), (11.0, -10.0, -2.0)], ml.rounded_rect(1.0, 1.0, 0.3, 2), scales=lambda t: (2.4, 0.7), caps=(True, True))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["sapphire", "wave_crests"], (110, 190, 255), 0.95, 0.55)

    def sea(pc):
        if not pc.face:
            return
        n = w.noise2(pc.X.shape, 7, 9)
        wv = 0.5 + 0.5 * np.sin(pc.Y * 0.5 + np.sin(pc.X * 0.3) * 2.0)
        pc.img[..., :3] = pc.img[..., :3] * (0.82 + 0.28 * wv[..., None]) + np.array([20, 60, 100], np.float32) * 0.2 * n[..., None]
        put_glow(pc, np.clip(wv - 0.8, 0, 1) * 0.6, (100, 180, 255), 0.5, 0.5)
    paint(atlas, m.part("face"), sea)
    finish("calamity_wave_aegis", m, atlas, "shield", glow=("FFFFFF", 0.15, 0.45, 1.0), shield=True)


def build_hellblaze_the_demon_sin():
    """Hellblaze, the Demon Sin: a black broadsword with a jagged, torn edge, lava-red veins in the fuller, a horned demon guard and a flame-red wrapped grip."""
    m = new_mesh("hellblaze_the_demon_sin")
    L0 = 26.0
    st = [(L0, -4.0, 4.0, 0.7)]
    y = L0 + 6.0
    k = 0
    while y < 104.0:
        a = -5.2 if k % 2 == 0 else -3.8
        b = 5.0 if k % 2 == 0 else 3.9
        st += [(y, a, b, 0.78), (y + 3.0, -3.6 - (k % 3) * 0.4, 4.0 + 0.3 * (k % 2), 0.78)]
        y += 6.0
        k += 1
    st += [(110.0, -4.6, 3.8, 0.6), (118.0, -3.2, 2.8, 0.5), (126.0, -1.4, 1.2, 0.34), (130.0, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="horns", guard_kw=dict(span=10.0, rise=9.0, t=1.2), grip_len=24.0, pommel_style="spike", pommel_kw=dict(r=2.6), blade_material="obsidian",
                         blade_base=(34, 28, 44), guard_material="iron", guard_base=(44, 34, 56), grip_base=(40, 28, 36), wrap_base=(190, 50, 36), bevel=0.45, ricasso=2.0)
    fuller_pair(m, "vein", L0 + 4.0, 116.0, 1.6, base=(255, 90, 40), material="obsidian", w_=0.45, h=0.2, zf=0.88, zt=0.8)
    flame = P(m, "guard_flames", "obsidian", base=(255, 110, 50), glow=True, smooth=30)
    for sx in (-1, 1):
        spike(flame, (sx * 2.2, L0 + 1.0, 0.0), (sx * 4.5, L0 + 12.0, 0.0), 1.0, 4)
        spike(flame, (sx * 4.0, L0 + 0.5, 0.0), (sx * 8.0, L0 + 8.0, 0.0), 0.9, 4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def lava(pc):
        if not pc.face:
            return
        n = w.noise2(pc.X.shape, 3, 7)
        crack = np.clip(1.0 - np.abs(n - 0.5) * 9.0, 0, 1) * np.clip(1.0 - (pc.Y - L0) / 120.0, 0.2, 1)
        darken(pc, crack * 0.0, 0.0)
        pc.img[..., :3] = pc.img[..., :3] * (1 - crack[..., None] * 0.5) + np.array([255, 90, 30], np.float32) * crack[..., None] * 0.5
        put_glow(pc, crack * 0.9, (255, 100, 40), 0.9, 0.5)
    paint(atlas, blade, lava)
    paint_edge_glow(atlas, blade, out, (255, 90, 40), 0.45, 0.6, 0.5, side="edge")
    glow_parts(atlas, m, ["vein_f", "vein_b", "guard_flames"], (255, 100, 40), 1.0, 0.5)
    finish("hellblaze_the_demon_sin", m, atlas, "longsword", glow=("FFFFFF", 0.17, 0.4, 1.0))


def build_minotaur_kings_labrys():
    """Minotaur King's Labrys: a huge double-headed axe of dark iron with serrated edges, long horn tips, ember cracks, a bone-and-leather haft and a horned pommel."""
    m = new_mesh("minotaur_kings_labrys")
    YC = 118.0
    shaft(m, 0.0, 130.0, r=2.2, material="bone", base=(226, 214, 184), name="haft", sides=10)
    wrap(m, "haft_wraps", 6.0, 60.0, 2.2, 10, "leather", (80, 50, 34))
    for y in (70.0, 100.0):
        collar_ring(m, y, 2.8, 2.0, "iron", (70, 66, 78), name="band_%d" % int(y))
    for sx in (-1, 1):
        spike(P(m, "pommel_horn_%s" % ("r" if sx > 0 else "l"), "bone", base=(236, 224, 196), smooth=35), (sx * 1.6, 2.0, 0.0), (sx * 8.5, 12.0, 0.0), 1.8, 6)
    lathe_y(m, "butt", "iron", -6.0, [(0.0, 0.0), (2.0, 2.4), (6.0, 3.0)], sides=10, base=(70, 66, 78), caps=(False, True))
    for sgn, nm in ((1, "blade_r"), (-1, "blade_l")):
        pts = [(sgn * 4.0, YC - 8.0)]
        for a in np.linspace(-72, 72, 21):
            r = 33.0 + (3.2 if int((a + 72) / 7.2) % 2 == 0 else 0.0)                    # the serrated rim
            pts.append((sgn * (4.0 + r * math.cos(math.radians(a))), YC + 31.0 * math.sin(math.radians(a))))
        pts.append((sgn * 4.0, YC + 8.0))
        if sgn < 0:
            pts = pts[::-1]
        extrude(m, nm, "iron", pts, 1.8, edge=0.8, base=(74, 70, 84), smooth=35)
        horns = P(m, "horns_" + ("r" if sgn > 0 else "l"), "bone", base=(236, 224, 196), smooth=35)
        spike(horns, (sgn * 30.0, YC + 28.0, 0.0), (sgn * 38.0, YC + 46.0, 0.0), 1.8, 6)
        spike(horns, (sgn * 30.0, YC - 28.0, 0.0), (sgn * 38.0, YC - 46.0, 0.0), 1.8, 6)
    hub = P(m, "hub", "iron", base=(60, 56, 70), smooth=45, uv_mode="loft")
    ml.lathe(hub, "z", (0, YC, 0), [(-3.2, 0.0), (-2.6, 6.0), (2.6, 6.0), (3.2, 0.0)], sides=14, caps=(False, False))
    eyeg = P(m, "hub_gem", "crystal", base=(255, 110, 60), glow=True, smooth=30)
    ml.gem(eyeg, (0.0, YC, 3.2), 2.4, 1.0, 6, axis="z")
    cracks = P(m, "ember_cracks", "obsidian", base=(255, 100, 40), glow=True, smooth=35)
    for sgn in (1, -1):
        for z0 in (1.95, -1.95):
            sg = 1 if z0 > 0 else -1
            for k in range(4):
                a = math.radians(-50 + 33 * k)
                p0 = (sgn * (9.0 + 3 * k), YC + 8.0 * math.sin(a) * 1.2, z0)
                p1 = (sgn * (24.0 + 4 * k), YC + 22.0 * math.sin(a), z0)
                ml.bar(cracks, p0, p1, 0.5, 0.15, up=(0, 0, sg), taper=1.2)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["hub_gem", "ember_cracks"], (255, 110, 50), 1.0, 0.5)
    finish("minotaur_kings_labrys", m, atlas, "battleaxe", glow=("FFFFFF", 0.15, 0.4, 1.0))


def build_dawnpiercer_crossbow():
    """Dawnpiercer Crossbow: a dark-wood stock, a gold sun-wing prod swept forward with feather ridges, a steel latch and rail, a glowing sunstone and a gilt bolt."""
    Y_BUTT, Y_MUZZLE, Y_PROD, SPAN, SWEEP, FLEX, Y_NOCK = -26.0, 36.0, 20.0, 25.0, 14.0, 3.0, 2.0
    STOCK_T = 2.4
    m = new_mesh("dawnpiercer_crossbow")
    sk = P(m, "stock", "wood", base=(74, 50, 38), smooth=40, uv_mode="loft")
    ys = [Y_BUTT, Y_BUTT + 5, -8, 4, 16, 28, Y_MUZZLE]
    wd = [2.8, 2.9, 2.3, 2.1, 2.0, 1.8, 1.5]
    ml.loft(sk, "y", (0, 0, 0), [(y, wd[i], STOCK_T * (1.0 if i < 5 else 0.85)) for i, y in enumerate(ys)], ml.rounded_rect(2.0, 2.0, 0.55, 2))
    rl = P(m, "rail", "steel", base=(176, 182, 196), wear=0.7, smooth=45)
    ml.fuller(rl, (0, Y_BUTT + 8, STOCK_T + 0.1), (0, Y_MUZZLE - 2.0, STOCK_T * 0.85 + 0.1), (0, 0, 1), 0.9, 0.28, 0.2, taper=3.0)
    gilt = P(m, "stock_gilt", "gold", base=(236, 192, 84), glow=True, smooth=40)
    for sgn in (-1, 1):
        ml.bar(gilt, (sgn * 2.6, Y_BUTT + 4, 0.0), (sgn * 2.0, Y_PROD - 6.0, 0.0), 0.5, 0.15, up=(sgn, 0, 0), taper=3.0)

    def limb_path(sgn):
        pts = []
        for i in range(13):
            t = i / 12
            pts.append((sgn * (1.5 + (SPAN - 1.5) * t), Y_PROD + SWEEP * t ** 1.7 - 1.2 * math.sin(math.pi * t), 0.0))
        return pts
    for sgn, nm in ((-1, "limb_l"), (1, "limb_r")):
        lb = P(m, nm, "gold", base=(232, 184, 76), smooth=42, uv_mode="loft")
        ml.sweep(lb, limb_path(sgn), ml.bevelled(2.0, 1.0, 0.5), scales=lambda t: (5.0 - 3.6 * t ** 0.8 if t < 0.985 else 0.0, 0.7 * (1 - 0.35 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    fe = P(m, "limb_feathers", "gold", base=(250, 214, 120), glow=True, smooth=25)
    for sgn in (-1, 1):
        pts = limb_path(sgn)
        for k in range(5):
            i = 2 + 2 * k
            px, py, _ = pts[i]
            ml.bar(fe, (px, py - 1.0, 0.7), (px + sgn * 1.5, py - 5.2 + 0.6 * k, 0.7), 1.6 - 0.2 * k, 0.12, up=(0, 0, 1), taper=1.8, end_scale=0.1)
    pm = P(m, "prod_mount", "steel", base=(168, 172, 192), wear=0.5, smooth=45)
    ml.loft(pm, "x", (0, Y_PROD - 0.5, 0), [-3.4, -2.6, 2.6, 3.4], ml.octagon(5.6, 4.0, 1.0))
    st = P(m, "string", "cloth", base=(236, 224, 190), wear=0.1, smooth=70)
    tl, tr = limb_path(-1)[-1], limb_path(1)[-1]
    A, B = (tl[0], tl[1] - 0.2, 0.9), (tr[0], tr[1] - 0.2, 0.9)
    ml.sweep(st, [(A[0] + (B[0] - A[0]) * i / 10, A[1], 0.9) for i in range(11)], ml.circle(1.0, 4), scales=lambda t: (0.14, 0.14))
    gp = P(m, "grip", "wood", base=(60, 42, 34), smooth=50)
    ml.loft(gp, "-z", (0, -4.0, -STOCK_T * 0.7), [0, 3, 8, 13], ml.rounded_rect(3.2, 3.8, 0.8, 2), caps=(True, True))
    tg = P(m, "trigger_guard", "gold", base=(236, 192, 84), wear=0.5, smooth=60)
    ml.sweep(tg, [(0, 1.6, -STOCK_T * 0.9), (0, 3.4, -STOCK_T - 1.8), (0, 3.0, -STOCK_T - 3.4), (0, -0.4, -STOCK_T - 3.4), (0, -2.0, -STOCK_T - 1.2)], ml.circle(1.0, 5), scales=lambda t: (0.28, 0.28))
    sr = P(m, "stirrup", "steel", base=(188, 192, 210), wear=0.5, smooth=55)
    ml.sweep(sr, [(1.8 * math.sin(a), Y_MUZZLE + 1.2 + 2.6 * math.cos(a) * 0.6 + 1.0, 0.0) for a in np.linspace(0, 2 * math.pi, 15)], ml.circle(1.0, 5), scales=lambda t: (0.42, 0.42), caps=(False, False))
    sun = P(m, "sunstone", "crystal", base=(255, 214, 110), glow=True, smooth=0)
    ml.gem(sun, (0, 9.0, STOCK_T + 0.3), 1.4, 1.5, 8, axis="y")
    nd = P(m, "bolt_shaft", "gold", base=(250, 220, 130), wear=0.3, smooth=45)
    ml.lathe(nd, "y", (0, 0, STOCK_T + 0.55), [(Y_NOCK + 0.6, 0.22), (Y_NOCK + 1.4, 0.3), (Y_NOCK + 34.0, 0.3), (Y_NOCK + 38.0, 0.0)], sides=6)
    tp = P(m, "bolt_tip", "crystal", base=(255, 230, 150), glow=True, smooth=0)
    ml.lathe(tp, "y", (0, 0, STOCK_T + 0.55), [(Y_NOCK + 30.0, 0.0), (Y_NOCK + 30.6, 0.44), (Y_NOCK + 37.0, 0.4), (Y_NOCK + 39.0, 0.0)], sides=6)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["sunstone", "bolt_tip"], (255, 220, 120), 1.0, 0.6)
    glow_parts(atlas, m, ["limb_feathers", "stock_gilt"], (255, 210, 110), 0.6, 0.45)
    glow_parts(atlas, m, ["limb_l", "limb_r"], (255, 200, 100), 0.15, 0.3)
    import ranged_meshes as rm
    from new197_ranged import finish_crossbow
    rig = rm.Rig(tip_a=A, tip_b=B, draw_dir=(0, -1, 0), max_draw=A[1] - Y_NOCK - FLEX, flex=FLEX, grip=(0.0, Y_PROD, 0.0), limb_axis=(1, 0, 0), limb_span=SPAN)
    finish_crossbow("dawnpiercer_crossbow", m, atlas, rig, glow=("FFFFFF", 0.17, 0.45, 1.0), real_m=1.1)


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

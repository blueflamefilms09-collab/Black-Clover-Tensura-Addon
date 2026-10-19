"""God grade weapons of 1.97.0, first half: the Staff of the Magi, the Hammer of Thunderbolts, Kusanagi, Gungnir, Leviathan's Crown Trident, the Wizard King's Vow."""
import math

import numpy as np

from new197_kit2 import *            # noqa: F401,F403
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft, extrude, arc_pts, ring_blade,
                         catmull, sweep_blade, cuboid, hammer_head, guard_cross, guard_wings, guard_disc, guard_leaf, guard_horns, guard_ring, pommel, straight_sword, spear_head)
from new197_lib import paint, put_glow, sm, step, darken, paint_glow, paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, new_mesh, new_atlas, lay_and_paint, finish
from new197_legend import glow_parts, fuller_pair


def helix_pts(r, y0, y1, turns, n, phase=0.0, x=0.0, z=0.0):
    """A polyline spiralling about the y axis."""
    return [(x + r * math.cos(phase + 2 * math.pi * turns * i / n), y0 + (y1 - y0) * i / n, z + r * math.sin(phase + 2 * math.pi * turns * i / n)) for i in range(n + 1)]


def build_staff_of_the_magi():
    """Staff of the Magi: a shaft of two violet strands twisted round a gold core, gold bands, and a great crystal floating in a claw cradle with an orbiting ring and gems."""
    m = new_mesh("staff_of_the_magi")
    Y1 = 205.0
    rod(m, "core", "gold", 0.0, Y1, 1.5, 1.5, sides=8, base=(214, 170, 70), smooth=55)
    for k, ph in enumerate((0.0, math.pi)):
        tube(m, "strand_%d" % k, "obsidian", helix_pts(2.2, 4.0, Y1 - 4.0, 5.0, 60, ph), 1.25, 1.25, sides=7, base=(98, 58, 150), smooth=60)
    for y in (14.0, 70.0, 130.0, Y1 - 2.0):
        collar_ring(m, y, 3.4, 2.4, "gold", (232, 186, 76), name="band_%d" % int(y))
    lathe_y(m, "butt", "gold", -7.0, [(0.0, 0.0), (2.0, 2.0), (7.0, 3.0)], sides=10, base=(226, 180, 70), caps=(False, True))
    cradle = P(m, "cradle", "gold", base=(236, 192, 80), smooth=40)
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        pts = [(3.0 * math.cos(a) * (1 + 1.6 * (i / 8.0)), Y1 + 2.0 + 20.0 * (i / 8.0) ** 1.2, 3.0 * math.sin(a) * (1 + 1.6 * (i / 8.0))) for i in range(9)]
        ml.sweep(cradle, pts, ml.octagon(2.0, 2.0, 0.5), scales=lambda t: (1.1 * (1 - 0.7 * t) + 0.15, 1.1 * (1 - 0.7 * t) + 0.15))
    big = P(m, "magi_crystal", "crystal", base=(170, 110, 255), glow=True, smooth=25)
    ml.gem(big, (0.0, Y1 + 20.0, 0.0), 6.0, 2.2, 6)
    halo = [(11.0 * math.cos(a), Y1 + 20.0 + 4.0 * math.sin(a * 2) * 0.0, 11.0 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 25)]
    tube(m, "halo", "gold", halo, 0.6, 0.6, sides=6, base=(236, 200, 100), glow=True)
    sat = P(m, "satellites", "crystal", base=(210, 160, 255), glow=True, smooth=25)
    for k in range(3):
        a = 2 * math.pi * k / 3
        ml.gem(sat, (11.0 * math.cos(a), Y1 + 20.0, 11.0 * math.sin(a)), 1.5, 1.4, 4)
    runes = P(m, "runes", "gold", base=(255, 220, 120), glow=True, smooth=30)
    for i in range(10):
        y = 24.0 + i * 17.0
        ml.bar(runes, (2.4, y, 1.2), (3.2, y + 4.0, 0.0), 0.5, 0.2, up=(1, 0, 0), taper=1.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["magi_crystal", "satellites"], (190, 130, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["halo", "runes"], (255, 220, 130), 0.9, 0.5)
    glow_parts(atlas, m, ["strand_0", "strand_1"], (160, 100, 240), 0.3, 0.3)
    finish("staff_of_the_magi", m, atlas, "staff", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_hammer_of_thunderbolts():
    """Hammer of Thunderbolts: a huge blue-grey steel head with gold bands and lightning engraved on the faces, on an iron haft with a lanyard ring."""
    m = new_mesh("hammer_of_thunderbolts")
    YH = 112.0
    rod(m, "haft", "iron", 0.0, YH + 4.0, 2.2, 2.2, sides=10, base=(82, 88, 104), smooth=55)
    wrap(m, "haft_wraps", 6.0, 50.0, 2.2, 9, "leather", (120, 90, 56))
    lathe_y(m, "butt", "gold", -5.0, [(0.0, 0.0), (1.8, 2.2), (5.0, 3.0)], sides=10, base=(232, 190, 80), caps=(False, True))
    tube(m, "lanyard_ring", "gold", [(4.8 * math.cos(a), 6.0, 4.8 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)], 0.6, 0.6, sides=6, base=(232, 190, 80))
    hammer_head(m, YH + 11.0, w_=46.0, h=24.0, d=24.0, material="steel", base=(122, 134, 156), name="head")
    for sx, nm in ((-1, "band_l"), (1, "band_r")):
        lathe_y(m, nm, "gold", YH, [(0.0, 12.5), (2.0, 13.0), (24.0, 13.0)], sides=4, base=(232, 190, 80), squash=1.0, x=0.0) if False else None
    bands = P(m, "bands", "gold", base=(232, 188, 76), smooth=38)
    for sx in (-1, 1):
        ml.loft(bands, "x", (0, YH + 11.0, 0), [(sx * 16.0, 1.04 * 12.0, 1.04 * 12.0), (sx * 16.0 + sx * 2.4, 1.04 * 12.0, 1.04 * 12.0)], ml.octagon(2.0, 2.0, 0.45))
        ml.loft(bands, "x", (0, YH + 11.0, 0), [(sx * 6.5, 1.04 * 12.0, 1.04 * 12.0), (sx * 6.5 + sx * 2.4, 1.04 * 12.0, 1.04 * 12.0)], ml.octagon(2.0, 2.0, 0.45))
    sock = P(m, "socket", "gold", base=(226, 180, 70), smooth=40, uv_mode="loft")
    ml.collar(sock, YH - 2.0, 4.0, 8.0, lip=1.0, sides=12)
    bolt = P(m, "bolt_inlay", "silver", base=(170, 214, 255), glow=True, smooth=40)
    zig = [(-5.0, YH + 16.0), (-1.0, YH + 12.0), (-3.0, YH + 10.0), (2.0, YH + 5.0), (0.0, YH + 3.0), (5.0, YH - 1.0)]
    for z0 in (12.4, -12.4):
        sg = 1 if z0 > 0 else -1
        for (a, b) in zip(zig[:-1], zig[1:]):
            ml.bar(bolt, (a[0] * 2.2, a[1], z0), (b[0] * 2.2, b[1], z0), 1.0, 0.18, up=(0, 0, sg), taper=0.0)
    gems = P(m, "storm_gems", "crystal", base=(120, 190, 255), glow=True, smooth=30)
    for sx in (-1, 1):
        ml.gem(gems, (sx * 23.6, YH + 11.0, 0.0), 2.2, 1.0, 6, axis="x")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    head = m.part("head")

    def storm(pc):
        n = w.noise2(pc.A.shape, 6, 3)
        pc.img[..., :3] = pc.img[..., :3] * (0.85 + 0.3 * n[..., None]) + np.array([20, 30, 50], np.float32) * 0.2
        put_glow(pc, np.clip(n - 0.62, 0, 1) * 0.5, (120, 190, 255), 0.6, 0.5)
    paint(atlas, head, storm)
    glow_parts(atlas, m, ["bolt_inlay", "storm_gems"], (140, 205, 255), 1.0, 0.6)
    finish("hammer_of_thunderbolts", m, atlas, "hammer", glow=("FFFFFF", 0.17, 0.45, 1.0))


def build_kusanagi_the_grass_cutter():
    """Kusanagi, the Grass-Cutter: a long curved jade-tinted blade whose hamon is a line of wind, a cloud-pattern guard, a wrapped hilt in white and green and a jade pommel."""
    m = new_mesh("kusanagi_the_grass_cutter")
    L0 = 26.0
    N = 20
    pts = [(-9.0 * (i / N) ** 2.2, L0 + 96.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "silver", pts, lambda t: 2.2 * (1.0 - 0.45 * t ** 1.5), lambda t: 0.5 * (1.0 - 0.4 * t), bevel=0.6, base=(196, 234, 214), smooth=48)
    spine = [(x + 2.0 * (1 - 0.45 * (i / N) ** 1.5), y) for i, (x, y) in enumerate(pts)]
    bar(m, "spine_ridge", "silver", (spine[0][0], spine[0][1], 0.0), (spine[-6][0], spine[-6][1], 0.0), 0.6, 0.9, up=(0, 0, 1), taper=4.0, base=(150, 200, 176))
    pts2 = [(r * math.cos(a), L0 - 0.5, r * math.sin(a)) for r in (5.2,) for a in np.linspace(0, 2 * math.pi, 25)]
    guard = P(m, "guard", "gold", base=(40, 110, 84), smooth=40, uv_mode="loft")
    ml.lathe(guard, "y", (0, L0 - 0.5, 0), [(-0.6, 3.0), (-0.3, 5.6), (0.3, 5.6), (0.6, 3.0)], sides=22)
    cloud = P(m, "cloud_inlay", "gold", base=(236, 200, 96), glow=True, smooth=40)
    for k in range(8):
        a = 2 * math.pi * k / 8
        ml.gem(cloud, (4.4 * math.cos(a), L0 + 0.2, 4.4 * math.sin(a)), 0.6, 1.0, 5)
    collar_ring(m, L0 - 2.4, 1.9, 2.0, "gold", (214, 180, 80), name="habaki", sides=10)
    grip(m, 3.0, L0 - 2.6, r=1.7, base=(240, 240, 232), wrap_base=(40, 120, 90), n_wraps=11, material="cloth", sides=10, swell=1.1)
    pom = P(m, "pommel", "crystal", base=(110, 220, 170), glow=True, smooth=40, uv_mode="loft")
    ml.lathe(pom, "y", (0, 3.2, 0), [(0.0, 1.0), (-1.4, 2.0), (-3.0, 1.8), (-4.2, 0.0)], sides=8, caps=(True, False))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    bl = m.part("blade")

    def hamon(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - L0) / 96.0, 0, 1)
        cx = -9.0 * t ** 2.2
        wave = 0.5 * np.sin(pc.Y * 0.35) * (1 - 0.3 * t)
        d = pc.X - (cx - 0.9 + wave)
        m_ = sm(d, 0.55)
        pc.img[..., :3] = pc.img[..., :3] * (1 - 0.3 * np.clip(sm(d, 1.8), 0, 1)[..., None]) + np.array([150, 220, 190], np.float32) * 0.3 * np.clip(sm(d, 1.8), 0, 1)[..., None]
        put_glow(pc, m_ * (pc.Y > L0 + 4), (130, 255, 200), 0.9, 0.5)
    paint(atlas, bl, hamon)
    glow_parts(atlas, m, ["cloud_inlay"], (255, 226, 120), 0.9, 0.5)
    glow_parts(atlas, m, ["pommel"], (120, 255, 200), 0.9, 0.5)
    finish("kusanagi_the_grass_cutter", m, atlas, "katana", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_gungnir_the_unerring():
    """Gungnir, the Unerring: a long golden spear with a rune-carved leaf head, a blue-wrapped grip and a spiral gold wire round the shaft, a blue gem at the socket."""
    m = new_mesh("gungnir_the_unerring")
    Y_S = 176.0
    shaft(m, 0.0, Y_S, r=1.8, material="wood", base=(52, 66, 120), name="shaft", sides=10)
    tube(m, "spiral", "gold", helix_pts(2.2, 20.0, Y_S - 8.0, 14.0, 160), 0.38, 0.38, sides=5, base=(236, 196, 90), smooth=70)
    wrap(m, "grip_wraps", 40.0, 88.0, 1.8, 12, "leather", (230, 214, 150))
    lathe_y(m, "butt", "gold", -6.0, [(0.0, 0.0), (2.0, 2.0), (6.0, 2.4)], sides=10, base=(236, 196, 90), caps=(False, True))
    lathe_y(m, "socket", "gold", Y_S - 6.0, [(0.0, 2.2), (3.0, 3.4), (7.0, 3.6), (14.0, 2.0)], sides=12, base=(240, 202, 98))
    out = spear_head(m, Y_S + 6.0, 54.0, 5.6, t=1.0, base=(238, 224, 168), material="gold", leaf=0.42, name="head", bevel=0.5)
    fl = fuller_pair(m, "head_fuller", Y_S + 12.0, Y_S + 44.0, 1.4, base=(255, 236, 170), material="gold", w_=0.3, h=0.12, zf=1.0, zt=0.5)
    gem = P(m, "socket_gem", "crystal", base=(100, 160, 255), glow=True, smooth=35)
    ml.gem(gem, (0.0, Y_S + 6.0, 3.6), 1.6, 1.3, 6)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    head = m.part("head")
    paint_center_glow(atlas, head, out, (255, 226, 130), 0.5, Y_S + 12.0, Y_S + 46.0, 0.9, 0.5)
    paint_runes(atlas, head, out, (255, 210, 110), Y_S + 14.0, Y_S + 44.0, 6, seed=21, size=0.9, side=2.0)
    paint_edge_glow(atlas, head, out, (255, 240, 180), 0.5, 0.5, 0.5, side="both")
    glow_parts(atlas, m, ["head_fuller_f", "head_fuller_b"], (255, 226, 130), 0.9, 0.5)
    glow_parts(atlas, m, ["socket_gem"], (130, 180, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["spiral"], (255, 220, 120), 0.35, 0.4)
    finish("gungnir_the_unerring", m, atlas, "spear", glow=("FFFFFF", 0.14, 0.45, 1.0))


def build_leviathans_crown_trident():
    """Leviathan's Crown Trident: a three-pronged crown of blue steel and coral with barbed prongs, a ring of coral spikes at the base, a pearl and a wave-etched shaft."""
    m = new_mesh("leviathans_crown_trident")
    Y_S = 170.0
    shaft(m, 0.0, Y_S, r=1.8, material="iron", base=(34, 96, 140), name="shaft", sides=10)
    for y in (12.0, 80.0, 130.0):
        collar_ring(m, y, 2.3, 2.0, "gold", (60, 200, 190), name="band_%d" % int(y))
    lathe_y(m, "butt", "gold", -6.0, [(0.0, 0.0), (2.0, 2.0), (6.0, 2.4)], sides=10, base=(60, 190, 184), caps=(False, True))
    lathe_y(m, "socket", "gold", Y_S - 6.0, [(0.0, 2.2), (3.0, 3.8), (8.0, 4.0), (14.0, 2.4)], sides=12, base=(64, 196, 190))
    # the central prong and the two curved side prongs
    cen = spear_head(m, Y_S + 8.0, 52.0, 2.6, t=0.9, base=(56, 150, 214), material="steel", leaf=0.3, name="prong_c", bevel=0.5)
    for sx, nm in ((-1, "prong_l"), (1, "prong_r")):
        ctrl = [(sx * 3.0, Y_S + 8.0), (sx * 10.0, Y_S + 16.0), (sx * 16.0, Y_S + 30.0), (sx * 16.0, Y_S + 46.0), (sx * 14.0, Y_S + 56.0)]
        sweep_blade(m, nm, "steel", ctrl, lambda t: 2.4 * (1 - t) ** 0.7 + 0.3, lambda t: 0.8 * (1 - 0.5 * t), n=5, bevel=0.5, base=(56, 150, 214), smooth=45)
    barbs = P(m, "barbs", "steel", base=(120, 200, 240), smooth=30)
    for sx in (-1, 1):
        for k in range(3):
            y = Y_S + 32.0 + k * 7.0
            spike(barbs, (sx * 1.6, y, 0.0), (sx * 6.0, y - 5.0, 0.0), 0.8, 4)
    coral = P(m, "coral", "bone", base=(244, 130, 110), smooth=35)
    for k in range(9):
        a = 2 * math.pi * k / 9
        spike(coral, (3.6 * math.cos(a), Y_S + 2.0, 3.6 * math.sin(a)), (7.4 * math.cos(a), Y_S + 14.0 + (k % 3) * 3.0, 7.4 * math.sin(a)), 1.0, 5)
    orb(m, "pearl", "crystal", (0.0, Y_S + 9.0, 4.2), 2.2, base=(240, 250, 255), glow=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)

    def wave(pc):
        v = pc.B / 5.0 if not pc.face else pc.Y / 5.0
        band = 0.5 + 0.5 * np.sin(v)
        pc.img[..., :3] = pc.img[..., :3] * (0.85 + 0.3 * band[..., None])
        put_glow(pc, np.clip(band - 0.7, 0, 1), (90, 230, 230), 0.6, 0.5)
    paint(atlas, m.part("shaft"), wave)
    for nm in ("prong_c", "prong_l", "prong_r"):
        paint_glow(atlas, m.part(nm), (70, 220, 230), 0.4, 0.4)
    glow_parts(atlas, m, ["pearl"], (230, 250, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["coral"], (255, 130, 100), 0.25, 0.3)
    finish("leviathans_crown_trident", m, atlas, "trident", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_wizard_kings_vow():
    """Wizard King's Vow: a slim white rapier with a four-leaf clover guard of green and gold, a basket ring, a needle blade with a glowing line and a mana gem pommel."""
    m = new_mesh("wizard_kings_vow")
    L0 = 24.0
    st = [(L0, -1.5, 1.5, 0.5), (L0 + 6, -1.5, 1.5, 0.48), (60, -1.3, 1.3, 0.42), (96, -1.0, 1.0, 0.36), (112, -0.6, 0.6, 0.26), (122, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="leaf", guard_kw=dict(r=5.4), grip_len=22.0, pommel_style="orb", pommel_kw=dict(r=2.2), blade_material="silver", blade_base=(246, 244, 255),
                         guard_material="gold", guard_base=(72, 176, 104), grip_base=(246, 246, 250), wrap_base=(232, 192, 80), bevel=0.55, ricasso=1.0)
    ring = [(5.0 * math.cos(a) + 0.0, L0 - 1.0, 5.0 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 21)]
    tube(m, "basket_ring", "gold", [(6.4 * math.cos(a), L0 - 2.4 + 3.0 * math.sin(a) * 0.0, 6.4 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 21)], 0.5, 0.5, sides=6, base=(236, 196, 84))
    tube(m, "basket_bar", "gold", [(-6.4, L0 - 2.4, 0.0), (-7.2, L0 - 12.0, 0.0), (-3.0, L0 - 20.0, 0.0)], 0.5, 0.5, sides=6, base=(236, 196, 84))
    leaf = P(m, "leaf_gems", "crystal", base=(120, 230, 150), glow=True, smooth=40)
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        ml.gem(leaf, (dx * 4.0, L0 + 0.4, dz * 4.0), 0.9, 1.1, 5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_center_glow(atlas, blade, out, (150, 255, 190), 0.4, L0 + 8, 108.0, 0.9, 0.5)
    paint_edge_glow(atlas, blade, out, (200, 255, 220), 0.4, 0.5, 0.5, side="both")
    paint_gradient(atlas, blade, (200, 240, 220), (255, 255, 255), L0, 122.0, 0.4)
    glow_parts(atlas, m, ["leaf_gems", "pommel", "guard"], (130, 255, 170), 0.55, 0.45)
    finish("wizard_kings_vow", m, atlas, "rapier", glow=("FFFFFF", 0.15, 0.5, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

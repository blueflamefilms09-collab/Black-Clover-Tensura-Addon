"""Genesis grade weapons of 1.97.0, first half: Blackrazor, Gae Bolg, the Primordial Hearth Blade, the Void Origin Katana, the Sword of the First Dawn.
Like the Genesis Demon-Slayer they carry void parts (layer 1: the dimensional void showing through the metal) and a living glow."""
import math

import numpy as np

from new197_kit2 import *            # noqa: F401,F403
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft, extrude, arc_pts, ring_blade,
                         catmull, sweep_blade, cuboid, guard_cross, guard_wings, guard_disc, guard_leaf, guard_horns, guard_ring, pommel, straight_sword, spear_head, void_poly, void_strip)
from new197_lib import paint, put_glow, sm, step, darken, paint_glow, paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, new_mesh, new_atlas, lay_and_paint, finish
from new197_legend import glow_parts, fuller_pair
from new197_god import helix_pts


def void_ribbon(m, name, pts, half, z):
    """A ribbon of the void along a polyline [(x, y)] (going up), half width `half`, lying at +z and -z on both faces of a blade."""
    p = ml.Part(name, layer=1)
    for sgn in (1, -1):
        a = [(x - half, y) for (x, y) in pts]
        b = [(x + half, y) for (x, y) in pts]
        ia = [p.add_vertex((x, y, sgn * z)) for (x, y) in a]
        ib = [p.add_vertex((x, y, sgn * z)) for (x, y) in b]
        for i in range(len(pts) - 1):
            if sgn > 0:
                p.add_face(ia[i], ib[i], ib[i + 1], ia[i + 1])
            else:
                p.add_face(ia[i], ia[i + 1], ib[i + 1], ib[i])
    m.add(p)
    return p


def build_blackrazor():
    """Blackrazor: a greatsword of black glass with violet-lit edges and three rifts of the void, a horned guard with a skull, a chain-wrapped grip and a violet eye pommel; soul wisps drift beside it."""
    m = new_mesh("blackrazor")
    L0 = 38.0
    st = [(L0, -4.1, 4.1, 1.0), (L0 + 10, -4.6, 4.6, 1.0), (110, -4.6, 4.6, 0.95), (130, -4.1, 4.1, 0.85), (140, -2.7, 2.7, 0.6), (147, -1.0, 1.0, 0.3), (150, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="horns", guard_kw=dict(span=13.0, rise=9.0, t=1.5), grip_len=34.0, pommel_style="eye", pommel_kw=dict(r=3.0), blade_material="obsidian",
                         blade_base=(18, 14, 28), guard_material="obsidian", guard_base=(40, 28, 58), grip_base=(24, 20, 34), wrap_base=(120, 120, 140), bevel=0.4, ricasso=4.0)
    skull = P(m, "skull", "bone", base=(226, 220, 236), smooth=45)
    w.ellipsoid(skull, (0.0, L0 + 3.4, 1.0), 3.0, 3.2, 2.6, sides=10, stacks=7)
    eyes = P(m, "skull_eyes", "crystal", base=(190, 90, 255), glow=True, smooth=30)
    for sx in (-1, 1):
        ml.gem(eyes, (sx * 1.2, L0 + 4.2, 3.4), 0.55, 1.2, 4, axis="z")
    for i, (a, b, hw, jg) in enumerate(((52.0, 70.0, 1.1, 0.5), (86.0, 100.0, 1.2, 0.5), (114.0, 128.0, 1.0, 0.45))):
        void_strip(m, "void_%d" % i, a, b, hw, 1.04, cx=0.0, jag=jg, n=7)
    wisps = P(m, "soul_wisps", "crystal", base=(170, 120, 255), glow=True, smooth=25)
    for k, (x, y, z) in enumerate(((9.0, 60.0, 3.0), (-9.5, 84.0, -3.0), (8.5, 108.0, 2.0), (-8.0, 124.0, 3.0), (9.5, 76.0, -2.5))):
        ml.gem(wisps, (x, y, z), 0.9 + 0.2 * (k % 3), 1.5, 4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_edge_glow(atlas, blade, out, (170, 90, 255), 0.7, 0.9, 0.5, side="both")
    paint_runes(atlas, blade, out, (190, 120, 255), L0 + 10, 136.0, 10, seed=31, size=1.2, side=2.6, strength=0.8)

    def glass(pc):
        if not pc.face:
            return
        n = w.noise2(pc.X.shape, 4, 11)
        pc.img[..., :3] = pc.img[..., :3] * (0.8 + 0.5 * n[..., None]) + np.array([40, 20, 80], np.float32) * (n[..., None] * 0.5)
        put_glow(pc, np.clip(n - 0.7, 0, 1) * 0.8, (150, 70, 255), 0.6, 0.5)
    paint(atlas, blade, glass)
    glow_parts(atlas, m, ["skull_eyes", "pommel_eye", "soul_wisps"], (190, 110, 255), 1.0, 0.6)
    finish("blackrazor", m, atlas, "greatsword", glow=("FFFFFF", 0.17, 0.4, 1.0))


def build_gae_bolg_heartseeker():
    """Gae Bolg, Heart-Seeker: a black shaft with blood-red sinew wrap and a long twisted crimson thorn for a head, ringed with barbs that curve back, a red core glowing within."""
    m = new_mesh("gae_bolg_heartseeker")
    Y_S = 170.0
    shaft(m, 0.0, Y_S, r=1.7, material="obsidian", base=(28, 24, 32), name="shaft", sides=10)
    cord = P(m, "sinew_wrap", "cloth", base=(150, 24, 40), wear=0.3, smooth=60)
    for i in range(34):
        ml.collar(cord, 24.0 + i * 3.0, 1.85, 0.9, lip=0.25, sides=8, caps=(False, False))
    lathe_y(m, "butt", "iron", -6.0, [(0.0, 0.0), (2.0, 2.0), (6.0, 2.4)], sides=10, base=(40, 34, 48), caps=(False, True))
    lathe_y(m, "socket", "iron", Y_S - 4.0, [(0.0, 2.0), (3.0, 3.6), (8.0, 3.2), (12.0, 2.4)], sides=12, base=(52, 40, 60))
    thorn = P(m, "thorn", "bone", base=(150, 20, 40), smooth=40, uv_mode="loft", glow=True)
    ml.loft(thorn, "y", (0, 0, 0), [(Y_S + 6.0, 4.4, 4.4), (Y_S + 18.0, 4.0, 4.0), (Y_S + 40.0, 3.0, 3.0), (Y_S + 62.0, 1.6, 1.6), (Y_S + 84.0, 0.0, 0.0)], ml.circle(1.0, 8))
    barbs = P(m, "barbs", "iron", base=(70, 16, 28), smooth=30)
    for k in range(16):
        a = k * 2.4
        y = Y_S + 14.0 + k * 3.8
        r = 4.2 * (1.0 - (y - Y_S) / 96.0)
        spike(barbs, (r * math.cos(a), y, r * math.sin(a)), ((r + 8.0) * math.cos(a), y - 11.0, (r + 8.0) * math.sin(a)), 1.4, 4)
    tassel = P(m, "tassel", "cloth", base=(170, 26, 44), smooth=28)
    for k in range(10):
        a = 2 * math.pi * k / 10
        spike(tassel, (3.0 * math.cos(a), Y_S + 4.0, 3.0 * math.sin(a)), (5.8 * math.cos(a), Y_S - 14.0 - (k % 3) * 4.0, 5.8 * math.sin(a)), 0.9, 4)
    core = P(m, "heart_core", "crystal", base=(255, 40, 70), glow=True, smooth=30)
    ml.gem(core, (0.0, Y_S + 12.0, 2.5), 1.4, 1.6, 6)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)

    def thorn_paint(pc):
        t = np.clip(pc.B / 60.0, 0, 1) if not pc.face else np.clip((pc.Y - Y_S) / 66.0, 0, 1)
        n = w.noise2(pc.A.shape, 3, 7)
        veins = np.clip(1.0 - np.abs(np.sin(pc.A * 0.9 + pc.B * 0.25) * 3.0) , 0, 1) * 0.0
        pc.img[..., :3] = np.array([150, 18, 36], np.float32) * (0.7 + 0.6 * n[..., None]) * (1.0 - 0.3 * t[..., None])
        put_glow(pc, np.clip(n * 1.2 - 0.25, 0, 1) * (0.9 - 0.4 * t), (255, 40, 70), 0.9, 0.5)
    paint(atlas, m.part("thorn"), thorn_paint)
    glow_parts(atlas, m, ["heart_core"], (255, 60, 90), 1.0, 0.6)
    glow_parts(atlas, m, ["tassel", "barbs"], (255, 40, 70), 0.3, 0.3)
    finish("gae_bolg_heartseeker", m, atlas, "spear", glow=("FFFFFF", 0.2, 0.45, 1.0))


def build_primordial_hearth_blade():
    """Primordial Hearth Blade: a short black dagger whose blade is cut like a flame, gold fire running through it, a hearth orb at the guard and a red-wrapped grip with a ring."""
    m = new_mesh("primordial_hearth_blade")
    L0 = 14.0
    st = [(L0, -2.0, 2.0, 0.55), (L0 + 5, -2.9, 2.2, 0.58), (L0 + 10, -1.8, 3.0, 0.58), (L0 + 15, -3.1, 2.0, 0.56), (L0 + 20, -1.7, 2.6, 0.52), (L0 + 26, -2.6, 1.4, 0.46),
          (L0 + 31, -1.3, 1.6, 0.4), (L0 + 36, -0.6, 0.6, 0.3), (L0 + 40, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="disc", guard_kw=dict(r=4.0, thick=0.7, notches=8), grip_len=14.0, pommel_style="ring", pommel_kw=dict(r=2.2), blade_material="obsidian",
                         blade_base=(26, 22, 26), guard_material="gold", guard_base=(236, 176, 66), grip_base=(52, 30, 26), wrap_base=(200, 52, 40), bevel=0.5, ricasso=1.2)
    hearth = P(m, "hearth_orb", "crystal", base=(255, 196, 90), glow=True, smooth=35)
    w.ellipsoid(hearth, (0.0, L0 + 1.2, 0.0), 1.9, 1.5, 1.9, sides=10, stacks=7)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def flame(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - L0) / 40.0, 0, 1)
        u = pc.X - out.c(pc.Y)
        f1 = sm(u - 0.9 * np.sin(pc.Y * 0.55), 0.55)
        f2 = sm(u + 0.8 * np.sin(pc.Y * 0.55 + 1.6), 0.4)
        m_ = np.maximum(f1, f2) * (1.0 - 0.3 * t)
        pc.img[..., :3] = pc.img[..., :3] * (1 - m_[..., None] * 0.7) + np.array([255, 176, 60], np.float32) * m_[..., None] * 0.7
        put_glow(pc, m_, (255, 170, 60), 1.0, 0.6)
    paint(atlas, blade, flame)
    paint_edge_glow(atlas, blade, out, (255, 190, 90), 0.4, 0.6, 0.5, side="edge")
    glow_parts(atlas, m, ["hearth_orb"], (255, 196, 90), 1.0, 0.6)
    finish("primordial_hearth_blade", m, atlas, "dagger", glow=("FFFFFF", 0.17, 0.45, 1.0))


def build_void_origin_katana():
    """Void Origin Katana: a long blue-black blade with a cyan hamon and a ribbon of the void along the flat, a cyan ring guard, a black-and-cyan cord grip and a cyan pommel."""
    m = new_mesh("void_origin_katana")
    L0 = 26.0
    N = 22
    pts = [(-7.5 * (i / N) ** 2.2, L0 + 100.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "obsidian", pts, lambda t: 2.3 * (1.0 - 0.4 * t ** 1.5), lambda t: 0.52 * (1.0 - 0.4 * t), bevel=0.6, base=(12, 18, 40), smooth=48)
    void_ribbon(m, "void_ribbon", [(x + 0.3, y) for (x, y) in pts[3:-4]], 0.55, 0.44)
    guard_ring(m, L0 - 0.4, r=5.0, tube_r=0.8, material="gold", base=(70, 220, 255), name="guard", glow=True)
    gdisc = P(m, "guard_disc", "obsidian", base=(18, 24, 48), smooth=40, uv_mode="loft")
    ml.lathe(gdisc, "y", (0, L0 - 0.5, 0), [(-0.5, 2.6), (-0.2, 4.6), (0.2, 4.6), (0.5, 2.6)], sides=20)
    collar_ring(m, L0 - 2.4, 1.9, 2.0, "silver", (60, 200, 240), name="habaki", sides=10)
    grip(m, 3.0, L0 - 2.6, r=1.7, base=(14, 18, 30), wrap_base=(60, 200, 240), n_wraps=11, material="cloth", sides=10, swell=1.1)
    pom = P(m, "pommel", "crystal", base=(60, 200, 255), glow=True, smooth=40, uv_mode="loft")
    ml.lathe(pom, "y", (0, 3.2, 0), [(0.0, 1.0), (-1.4, 2.0), (-3.0, 1.8), (-4.2, 0.0)], sides=8, caps=(True, False))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    bl = m.part("blade")

    def hamon(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - L0) / 100.0, 0, 1)
        cx = -7.5 * t ** 2.2
        wave = 0.45 * np.sin(pc.Y * 0.42) * (1 - 0.3 * t)
        d = pc.X - (cx - 1.0 + wave)
        m_ = sm(d, 0.5)
        zone = sm(d, 2.0)
        pc.img[..., :3] = pc.img[..., :3] * (1 - 0.3 * zone[..., None]) + np.array([40, 120, 190], np.float32) * 0.3 * zone[..., None]
        put_glow(pc, m_ * (pc.Y > L0 + 4), (60, 210, 255), 1.0, 0.55)
    paint(atlas, bl, hamon)
    glow_parts(atlas, m, ["guard", "pommel"], (70, 220, 255), 0.9, 0.5)
    finish("void_origin_katana", m, atlas, "katana", glow=("FFFFFF", 0.17, 0.45, 1.0))


def build_sword_of_the_first_dawn():
    """Sword of the First Dawn: a broad white-gold blade of dawnlight that runs from deep orange at the guard to white at the tip, a rising sun at the root, a sunburst guard with swept wings and a floating halo ring."""
    m = new_mesh("sword_of_the_first_dawn")
    L0 = 26.0
    st = [(L0, -3.2, 3.2, 0.6), (L0 + 8, -3.8, 3.8, 0.62), (72, -3.7, 3.7, 0.56), (104, -3.1, 3.1, 0.48), (118, -2.1, 2.1, 0.4), (124, -1.0, 1.0, 0.3), (127, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="wings", guard_kw=dict(span=12.0, feathers=5, rise=10.0, t=1.0), grip_len=24.0, pommel_style="orb", pommel_kw=dict(r=2.8), blade_material="silver",
                         blade_base=(250, 244, 232), guard_material="gold", guard_base=(240, 196, 84), grip_base=(246, 242, 232), wrap_base=(240, 190, 80), bevel=0.55, ricasso=2.0, glow_blade=True)
    sun = P(m, "rising_sun", "crystal", base=(255, 200, 90), glow=True, smooth=30, uv_mode="loft")
    ml.lathe(sun, "z", (0, L0 + 1.0, 0.0), [(-0.1, 0.0), (0.2, 3.2), (1.0, 3.0), (1.9, 0.0)], sides=16, caps=(False, False))
    for k in range(10):
        a = math.pi * (k + 0.5) / 10
        spike(sun, (3.0 * math.cos(a), L0 + 1.0 + 3.0 * math.sin(a), 1.0), (6.0 * math.cos(a), L0 + 1.0 + 6.0 * math.sin(a), 1.0), 0.45, 4)
    tube(m, "dawn_halo", "gold", [(7.5 * math.cos(a), 74.0, 7.5 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 25)], 0.5, 0.5, sides=6, base=(255, 224, 140), glow=True)
    inl = P(m, "frame", "gold", base=(250, 214, 100), glow=True, smooth=40)
    for sgn in (1, -1):
        for sx in (-1, 1):
            ml.bar(inl, (sx * 3.3, L0 + 9, sgn * 0.6), (sx * 2.9, 112.0, sgn * 0.5), 0.45, 0.13, up=(0, 0, sgn), taper=6.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def dawn(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - L0) / 101.0, 0, 1)
        u = np.abs(out.u(pc.X, pc.Y))
        lo, hi = np.array([255, 150, 60], np.float32), np.array([255, 252, 235], np.float32)
        col = lo * (1 - t[..., None]) + hi * t[..., None]
        pc.img[..., :3] = col * (0.82 + 0.25 * (1 - u[..., None]))
        put_glow(pc, np.clip(0.55 + 0.4 * (1 - u), 0, 1), (255, 200, 110), 1.0, 0.65)
        d = np.hypot(pc.X, (pc.Y - (L0 + 26.0)) * 0.8)
        put_glow(pc, np.clip(1.0 - (d - 3.0) / 1.2, 0, 1) * 0.0, (255, 230, 150), 0.0, 0.0)
    paint(atlas, blade, dawn)
    paint_runes(atlas, blade, out, (255, 150, 60), L0 + 12, 100.0, 7, seed=41, size=1.0, side=1.0, strength=0.7)
    glow_parts(atlas, m, ["rising_sun", "dawn_halo", "frame", "pommel"], (255, 214, 110), 0.95, 0.6)
    finish("sword_of_the_first_dawn", m, atlas, "longsword", glow=("FFF4C0", 0.2, 0.6, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

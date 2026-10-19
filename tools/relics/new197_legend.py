"""The 13 Legend grade weapons of 1.97.0 as 3D models (see new197_roster.py; built by new197_build.py). One function per weapon id, named build_<id>."""
import math

import numpy as np

from new197_kit import *            # noqa: F401,F403
from new197_kit import (P, plate, blade_curve, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, void_plate, ml, w, Outline, paint, put_glow, sm, step,
                        darken, grip, pommel, guard_cross, guard_wings, guard_disc, guard_leaf, guard_horns, guard_ring, collar_ring, straight_sword, spear_head, shaft,
                        paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, paint_glow, shade, new_mesh, new_atlas, lay_and_paint, finish)


def fuller_pair(m, name, y0, y1, gap, base=(210, 220, 235), material="silver", w_=0.28, h=0.12, zf=0.5, zt=0.4):
    """The two ridges of a fuller on each face (named <name>_f / <name>_b, glowing)."""
    out = []
    for sgn, suf in ((1, "_f"), (-1, "_b")):
        p = P(m, name + suf, material, base=base, glow=True, smooth=50)
        ml.ridge_pair(p, (0.0, y0, sgn * zf), (0.0, y1, sgn * zt), (0, 0, sgn), gap, w_, h, 5.0)
        out.append(p)
    return out


def glow_parts(atlas, m, names, rgb, strength=0.9, core=0.5):
    for nm in names:
        paint_glow(atlas, m.part(nm), rgb, strength, core)


def build_vorpal_sword():
    """Vorpal Sword: a long, very thin blade with a deep glowing fuller, a small curved cross guard and an eye gem pommel; its edge hums a pale blue."""
    m = new_mesh("vorpal_sword")
    L0, L1 = 20.0, 112.0
    st = [(L0, -2.7, 2.7, 0.5), (L0 + 6, -2.8, 2.6, 0.5), (60, -2.6, 2.4, 0.42), (92, -2.1, 2.0, 0.36), (104, -1.5, 1.5, 0.3), (110, -0.8, 0.8, 0.22), (L1, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=7.5, t=1.1, h=1.0, bulb=1.1, curve=1.8), grip_len=19.0, pommel_style="eye", pommel_kw=dict(r=2.3),
                         blade_material="steel", blade_base=(176, 190, 214), guard_material="silver", guard_base=(52, 66, 106), grip_base=(34, 40, 66), wrap_base=(120, 150, 210), bevel=0.55)
    fuller_pair(m, "fuller", L0 + 8, 96.0, 1.1, base=(190, 214, 255), material="silver")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_edge_glow(atlas, blade, out, (140, 205, 255), 0.55, 0.85, 0.5, side="edge")
    paint_center_glow(atlas, blade, out, (150, 210, 255), 0.5, L0 + 8, 96.0, 0.9, 0.5)
    paint_gradient(atlas, blade, (140, 150, 190), (200, 220, 255), L0, 108.0, 0.35)
    glow_parts(atlas, m, ["fuller_f", "fuller_b"], (150, 215, 255), 0.9, 0.5)
    glow_parts(atlas, m, ["pommel_eye"], (120, 200, 255), 1.0, 0.6)
    finish("vorpal_sword", m, atlas, "longsword", glow=("FFFFFF", 0.14, 0.5, 1.0))


def build_holy_avenger():
    """Holy Avenger: a crusader's longsword: a broad white-gold blade with a glowing sun fuller and gold runes, swept feather wings for a guard, a sun-disc pommel."""
    m = new_mesh("holy_avenger")
    L0 = 24.0
    st = [(L0, -3.5, 3.5, 0.6), (L0 + 8, -3.6, 3.6, 0.62), (66, -3.4, 3.4, 0.55), (96, -2.8, 2.8, 0.48), (108, -1.8, 1.8, 0.4), (115, -0.9, 0.9, 0.3), (118, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="wings", guard_kw=dict(span=10.0, feathers=4, rise=8.5, t=0.9), grip_len=23.0, pommel_style="disc", pommel_kw=dict(r=3.6),
                         blade_material="silver", blade_base=(244, 240, 226), guard_material="gold", guard_base=(228, 182, 72), grip_base=(238, 232, 216), wrap_base=(214, 172, 74), bevel=0.5)
    fuller_pair(m, "fuller", L0 + 10, 100.0, 1.3, base=(255, 236, 170), material="gold")
    sun = P(m, "sun_gem", "crystal", base=(255, 230, 130), glow=True, smooth=40)
    ml.gem(sun, (0.0, L0 + 2.6, 0.9), 1.1, 1.3, 6)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_center_glow(atlas, blade, out, (255, 224, 130), 0.55, L0 + 10, 100.0, 0.85, 0.5)
    paint_edge_glow(atlas, blade, out, (255, 236, 170), 0.45, 0.55, 0.5, side="both")
    paint_runes(atlas, blade, out, (255, 216, 110), L0 + 14, 92.0, 9, seed=11, size=0.9, side=1.6)
    paint_gradient(atlas, blade, (255, 236, 190), (255, 255, 245), L0, 116.0, 0.45)
    glow_parts(atlas, m, ["fuller_f", "fuller_b", "sun_gem", "pommel"], (255, 220, 120), 0.8, 0.5)
    finish("holy_avenger", m, atlas, "longsword", glow=("FFFFFF", 0.13, 0.5, 1.0))


def build_luck_blade():
    """Luck Blade: a short leaf-shaped shortsword of green steel with a gold four-leaf clover guard and a clover inlaid in the blade."""
    m = new_mesh("luck_blade")
    L0 = 16.0
    st = [(L0, -1.7, 1.7, 0.55), (L0 + 6, -3.4, 3.4, 0.62), (36, -4.4, 4.4, 0.66), (46, -4.0, 4.0, 0.6), (56, -2.8, 2.8, 0.5), (64, -1.2, 1.2, 0.34), (68, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="leaf", guard_kw=dict(r=4.6), grip_len=16.0, pommel_style="orb", pommel_kw=dict(r=2.3), blade_material="steel", blade_base=(104, 196, 124),
                         guard_material="gold", guard_base=(232, 200, 70), grip_base=(44, 96, 56), wrap_base=(232, 208, 100), bevel=0.55, ricasso=1.0)
    rim = P(m, "leaf_rim", "gold", base=(240, 214, 96), glow=True, smooth=45)
    for sgn in (1, -1):
        ml.bar(rim, (0.0, L0 + 6, sgn * 0.5), (0.0, 60.0, sgn * 0.3), 0.34, 0.1, up=(0, 0, sgn), taper=4.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def clover(pc):
        if not pc.face:
            return
        cy = 38.0
        mk = np.zeros_like(pc.X)
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            mk = np.maximum(mk, np.clip(1.4 - np.hypot(pc.X - dx * 1.5, pc.Y - cy - dy * 1.5), 0, 1))
        mk = np.maximum(mk, sm(pc.X, 0.3) * (pc.Y > cy - 5) * (pc.Y < cy - 1.2))
        darken(pc, mk, 0.3)
        put_glow(pc, mk, (255, 226, 110), 0.95, 0.55)
    paint(atlas, blade, clover)
    paint_edge_glow(atlas, blade, out, (200, 255, 170), 0.5, 0.55, 0.5, side="both")
    paint_gradient(atlas, blade, (70, 150, 90), (170, 240, 160), L0, 66.0, 0.5)
    glow_parts(atlas, m, ["leaf_rim", "pommel", "guard"], (255, 224, 110), 0.5, 0.4)
    finish("luck_blade", m, atlas, "shortsword", glow=("FFFFFF", 0.16, 0.45, 1.0))


def build_frostbrand():
    """Frostbrand: a curved ice-blue sabre (the tip bends toward the spine) with a rime-white edge, a disc guard ringed with ice shards and an icicle pommel."""
    m = new_mesh("frostbrand")
    L0 = 20.0
    N = 18
    pts = [(10.0 * (i / N) ** 2.0, L0 + 88.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "crystal", pts, lambda t: 3.7 * (1.0 - 0.6 * t ** 1.3), lambda t: 0.5 * (1.0 - 0.45 * t), bevel=0.5, base=(150, 214, 244), smooth=48)
    guard_disc(m, L0 - 0.6, r=5.2, thick=0.8, material="silver", base=(190, 226, 248), name="guard")
    ice = P(m, "ice_shards", "crystal", base=(214, 244, 255), glow=True, smooth=30)
    for sx in (-1, 1):
        for dx, dy, ln in ((3.6, 0.6, 5.5), (5.2, -0.5, 4.0), (2.2, 1.8, 4.5)):
            spike(ice, (sx * dx, L0 - 0.4 + dy, 0.0), (sx * (dx + ln * 0.5), L0 + dy + ln * 0.9, 0.0), 0.8, 5)
    for i in range(6):
        t = 0.2 + 0.12 * i
        x, y = pts[int(t * N)]
        spike(ice, (x + 3.3 * (1 - 0.6 * t), y, 0.0), (x + 3.3 * (1 - 0.6 * t) + 2.8, y + 3.2, 0.0), 0.5, 4)
    grip(m, 3.0, L0 - 1.0, base=(34, 70, 120), wrap_base=(214, 234, 255), n_wraps=6)
    pm_ = P(m, "pommel", "crystal", base=(190, 232, 255), glow=True, smooth=35, uv_mode="loft")
    ml.lathe(pm_, "y", (0, 3.4, 0), [(0.0, 0.8), (-1.2, 1.9), (-3.0, 1.6), (-6.0, 0.0)], sides=7, caps=(True, False))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def rime(pc):
        if not pc.face:
            return
        n = w.noise2(pc.X.shape, 3, 5)
        m_ = np.clip(n * 1.3 - 0.55, 0, 1) * 0.6
        pc.img[..., :3] = pc.img[..., :3] * (1 - m_[..., None]) + np.array([235, 248, 255], np.float32) * m_[..., None]
        put_glow(pc, np.clip(n * 1.2 - 0.5, 0, 1) * 0.5, (150, 220, 255), 0.7, 0.5)
    paint(atlas, blade, rime)
    glow_parts(atlas, m, ["ice_shards", "pommel"], (150, 225, 255), 0.85, 0.55)
    finish("frostbrand", m, atlas, "sabre", glow=("FFFFFF", 0.14, 0.45, 1.0))


def build_dragon_slayer():
    """Dragon Slayer: a slab of dark steel, notched by teeth along the edge, red at the tang; a spiked crimson crossguard of horns and a long two-handed grip."""
    m = new_mesh("dragon_slayer")
    L0 = 40.0
    st = [(L0, -5.2, 4.6, 0.9)]
    y = L0 + 6
    while y < 128.0:
        w0 = -5.6 + 0.5 * min(1.0, (y - L0) / 90.0)
        st += [(y, w0, 4.8, 0.95), (y + 0.8, w0 + 2.6, 4.8, 0.95), (y + 3.4, w0, 4.8, 0.95)]
        y += 10.0
    st += [(136, -5.0, 4.4, 0.8), (146, -4.2, 3.6, 0.6), (154, -1.8, 1.8, 0.35), (158, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="horns", guard_kw=dict(span=11.0, rise=7.0, t=1.3), grip_len=34.0, pommel_style="spike", pommel_kw=dict(r=2.8), blade_material="iron",
                         blade_base=(104, 108, 122), guard_material="iron", guard_base=(150, 34, 40), grip_base=(60, 30, 30), wrap_base=(190, 60, 50), bevel=0.4, ricasso=5.0)
    fuller_pair(m, "fuller", L0 + 6, 132.0, 2.0, base=(200, 60, 50), material="iron", w_=0.5, h=0.2, zf=0.95, zt=0.9)
    tooth = P(m, "teeth", "bone", base=(230, 220, 196), smooth=30)
    for i in range(7):
        yy = L0 + 14 + i * 14.0
        spike(tooth, (4.4, yy, 0.0), (7.0, yy + 5.0, 0.0), 0.9, 5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_gradient(atlas, blade, (214, 60, 40), (100, 106, 122), L0, 70.0, 0.75)
    paint_center_glow(atlas, blade, out, (255, 90, 50), 0.7, L0 + 6, 132.0, 0.6, 0.4)
    paint_runes(atlas, blade, out, (255, 120, 70), L0 + 16, 120.0, 8, seed=5, size=1.3, side=-1.0, strength=0.7)
    glow_parts(atlas, m, ["fuller_f", "fuller_b"], (255, 90, 50), 0.8, 0.4)
    finish("dragon_slayer", m, atlas, "greatsword", glow=("FFFFFF", 0.12, 0.35, 1.0))


def build_sunsword():
    """Sunsword: a plain gold hilt with a sun-disc guard wakes a blade of solid sunlight: a bright white-yellow core with a gold frame and rays."""
    m = new_mesh("sunsword")
    L0 = 20.0
    st = [(L0, -2.8, 2.8, 0.55), (L0 + 6, -3.5, 3.5, 0.6), (70, -3.4, 3.4, 0.55), (100, -3.0, 3.0, 0.5), (112, -2.0, 2.0, 0.4), (118, -0.9, 0.9, 0.3), (121, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="disc", guard_kw=dict(r=5.6, thick=0.9, notches=12), grip_len=20.0, pommel_style="orb", pommel_kw=dict(r=2.4), blade_material="crystal",
                         blade_base=(255, 236, 150), guard_material="gold", guard_base=(236, 186, 60), grip_base=(120, 70, 36), wrap_base=(240, 206, 100), bevel=0.55, glow_blade=True)
    for sgn, suf in ((1, "_f"), (-1, "_b")):
        fr = P(m, "frame" + suf, "gold", base=(250, 210, 90), glow=True, smooth=40)
        for sx in (-1, 1):
            ml.bar(fr, (sx * 3.1, L0 + 7, sgn * 0.62), (sx * 2.6, 106.0, sgn * 0.5), 0.5, 0.14, up=(0, 0, sgn), taper=5.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def sunlight(pc):
        if not pc.face:
            return
        u = np.abs(out.u(pc.X, pc.Y))
        core = np.clip(1.0 - u * 1.1, 0, 1)
        pc.img[..., :3] = np.array([255, 244, 190], np.float32) * (0.78 + 0.3 * core[..., None])
        put_glow(pc, np.clip(0.5 + core * 0.6, 0, 1), (255, 232, 120), 1.0, 0.7)
    paint(atlas, blade, sunlight)
    glow_parts(atlas, m, ["frame_f", "frame_b", "guard", "pommel"], (255, 214, 90), 0.8, 0.5)
    finish("sunsword", m, atlas, "longsword", glow=("FFF0A0", 0.2, 0.6, 1.0))


def build_crimson_lions_flame_lance():
    """Crimson Lion's Flame Lance: a red lacquered shaft, a gold lion-head socket with a streaming red mane, a flame-shaped leaf head that burns."""
    m = new_mesh("crimson_lions_flame_lance")
    Y_SOCKET, Y_HEAD = 168.0, 184.0
    shaft(m, 0.0, Y_SOCKET, r=1.7, material="wood", base=(170, 36, 30), name="shaft", sides=10)
    for y in (6.0, 70.0, 110.0):
        collar_ring(m, y, 2.1, 1.6, "gold", (232, 186, 70), name="band_%d" % int(y))
    lathe_y(m, "butt", "gold", -4.0, [(0.0, 0.0), (1.5, 1.6), (4.0, 2.0)], sides=10, base=(226, 180, 66), caps=(False, True))
    lathe_y(m, "socket", "gold", Y_SOCKET - 8.0, [(0.0, 2.0), (4.0, 3.2), (8.0, 3.6), (16.0, 2.2)], sides=12, base=(236, 190, 72))
    mane = P(m, "mane", "cloth", base=(196, 40, 34), smooth=28, glow=True)
    for k in range(14):
        a = 2 * math.pi * k / 14
        bx, bz = 3.4 * math.cos(a), 3.4 * math.sin(a)
        spike(mane, (bx, Y_SOCKET + 1.0, bz), (bx * 1.9, Y_SOCKET - 15.0 - (k % 3) * 5.0, bz * 1.9), 1.0, 4)
    jaw = P(m, "lion_face", "gold", base=(240, 196, 76), smooth=45)
    w.ellipsoid(jaw, (0.0, Y_SOCKET + 5.0, 1.2), 2.8, 3.0, 2.8, sides=10, stacks=7)
    for sx in (-1, 1):
        spike(jaw, (sx * 1.6, Y_SOCKET + 6.5, 3.4), (sx * 2.0, Y_SOCKET + 7.6, 6.0), 0.5, 4)
    ys = [0.0, 0.08, 0.2, 0.34, 0.5, 0.66, 0.8, 0.92, 1.0]
    ws = [0.25, 0.6, 1.0, 0.8, 0.95, 0.62, 0.5, 0.2, 0.0]
    st = [(Y_HEAD + 38.0 * a, -4.4 * b, 4.4 * b, 0.9 * (0.5 + 0.5 * b) if b else 0.0) for a, b in zip(ys, ws)]
    plate(m, "head", "steel", st, bevel=0.5, base=(255, 150, 60), glow=True, smooth=45)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    out = Outline(st)
    head = m.part("head")

    def flame(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - Y_HEAD) / 38.0, 0, 1)
        u = np.abs(out.u(pc.X, pc.Y))
        col = np.array([255, 70, 20], np.float32) * (1 - t[..., None]) + np.array([255, 220, 90], np.float32) * t[..., None]
        pc.img[..., :3] = col * (0.7 + 0.3 * (1 - u[..., None]))
        put_glow(pc, np.clip(0.75 - 0.3 * u, 0, 1), (255, 140, 40), 1.0, 0.55)
    paint(atlas, head, flame)
    glow_parts(atlas, m, ["mane"], (255, 80, 40), 0.7, 0.4)
    finish("crimson_lions_flame_lance", m, atlas, "spear", glow=("FFFFFF", 0.18, 0.5, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

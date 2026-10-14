"""Time Bender Dadao (card "NATURAL 1 OFFICIAL", a greatsword-class weapon with three tiers of attunement): a broad black single-edged dadao whose back edge is
notched and whose blade carries a glowing cyan vein, a small curved guard with two hooks, a long white-wrapped handle with black bindings, a ring pommel, and a
ring of blue time shards swirling at the guard.

Reference: the card, 736 x 733. The weapon stands upright at x = 75: the point at y = 5, the ring pommel's far edge at y = 702: 697 px.
s = card px from the pommel. Axis: y up from the pommel to the point; the blade bows toward -x; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 1.30                                       # a dadao is a two-handed sabre: about 1.3 m
S_TIP = 697.0
K = w.px_to_cm(TOTAL_M * 100.0, S_TIP)               # cm per card px (0.187)
S_RING = (0.0, 52.0)                                 # the ring pommel (card y 650..702)
S_GRIP = (52.0, 300.0)                               # the handle: card y 402..650, 19 px thick
R_GRIP = 9.5 * K * 1.15
S_GUARD = 305.0                                      # the guard's centre (card y 397)
GUARD_HALF = 30.0 * K
S_BLADE0 = 318.0                                     # the blade from card y 384
BLADE_W0, BLADE_W1 = 40.0 * K, 34.0 * K              # width 40 px at the base, 34 at the belly (card x 55..110)
BLADE_T0 = 1.1                                       # back thickness
BOW = 4.0                                            # the blade's bow toward the back, cm, at the point
BELLY_S = 540.0                                      # the widest point


def y(s):
    return s * K


def bow(s):
    t = max(0.0, (s - S_BLADE0) / (S_TIP - S_BLADE0))
    return -BOW * t ** 2.2


def edge_section(w0, t):
    """A dadao section: the edge at +x, the flat back at -x (counter-clockwise)."""
    hw, ht = w0 / 2, t / 2
    return [(hw, 0.0), (hw * 0.35, ht * 0.75), (-hw, ht), (-hw, -ht), (hw * 0.35, -ht * 0.75)]


def build():
    m = ml.Mesh("time_bender_dadao")
    # ---- 1 the blade: swept along its bow, widest at the belly, a clipped point
    bl = w.add(m, "blade", "obsidian", base=(26, 28, 36), smooth=42, uv_mode="loft")
    ss = np.linspace(S_BLADE0, S_TIP, 16)
    path = [(bow(s), y(s), 0.0) for s in ss]

    def wid(t):
        s = S_BLADE0 + t * (S_TIP - S_BLADE0)
        base = 0.72 + 0.62 * math.sin(math.pi * min(1.0, (s - S_BLADE0) / (BELLY_S - S_BLADE0 + 1e-6)) * 0.5)      # narrow at the guard, a broad belly (card: 28 px to 56 px)
        if t > 0.8:
            base *= max(0.0, 1.0 - ((t - 0.8) / 0.2) ** 1.7 * 1.0)
        return base
    ml.sweep(bl, path, edge_section(BLADE_W0, BLADE_T0), scales=lambda t: (max(0.0, wid(t)), 1.0 if t < 0.93 else max(0.0, 1.0 - (t - 0.93) * 12)), up=(0, 0, 1))
    # ---- 2 the glowing vein (a raised tube along the blade's centre on the +z face), 3 the spine ridge on the back
    vn = w.add(m, "blade_vein", "crystal", base=(60, 205, 255), glow=True, smooth=55, uv_mode="loft")
    vp = [(bow(s) + 0.2, y(s), BLADE_T0 * 0.48) for s in np.linspace(S_BLADE0 + 6, S_TIP - 40, 14)]
    ml.sweep(vn, vp, ml.circle(1.0, 6), scales=lambda t: (0.30 * (1.0 - 0.5 * t), 0.30 * (1.0 - 0.5 * t)), caps=(True, True))
    sr = w.add(m, "blade_spine", "iron", base=(60, 64, 78), wear=0.6, smooth=50)
    sp = [(bow(s) - BLADE_W0 * 0.5 * wid((s - S_BLADE0) / (S_TIP - S_BLADE0)) + 0.1, y(s), 0.0) for s in np.linspace(S_BLADE0 + 4, S_TIP - 70, 12)]
    ml.sweep(sr, sp, ml.circle(1.0, 6), scales=lambda t: (0.2, 0.55 * (1.0 - 0.3 * t)))
    # ---- 4 the notched back edge: four little teeth near the base
    tt = w.add(m, "back_teeth", "iron", base=(50, 54, 68), smooth=0)
    for i in range(4):
        s = S_BLADE0 + 26 + i * 30
        ml.gem(tt, (bow(s) - BLADE_W0 * 0.5 - 0.1, y(s), 0.0), 0.5, long=1.2, sides=4, axis="-x")
    # ---- 5 the habaki collar, 6 the guard bar, 7 its two curved hooks
    hb = w.add(m, "blade_collar", "iron", base=(70, 74, 90), smooth=45)
    ml.lathe(hb, "y", (0, 0, 0), [(y(S_GUARD) + 0.2, R_GRIP * 1.1), (y(S_GUARD) + 1.0, R_GRIP * 1.4), (y(S_BLADE0) + 0.6, R_GRIP * 1.1), (y(S_BLADE0) + 1.4, R_GRIP * 0.7)], sides=10, squash=0.7)
    gb = w.add(m, "guard_bar", "iron", base=(34, 36, 46), smooth=45)
    ml.loft(gb, "x", (0, y(S_GUARD), 0), [(-GUARD_HALF, 0.4, 0.4), (-GUARD_HALF * 0.5, 0.8, 0.8), (0, 1.0, 1.0), (GUARD_HALF * 0.5, 0.8, 0.8), (GUARD_HALF, 0.4, 0.4)], ml.octagon(1.3, 1.1, 0.35))
    gh = w.add(m, "guard_hooks", "iron", base=(52, 56, 70), smooth=45)
    for sgn, up in ((-1, 1), (1, -1)):
        hp = [(sgn * (GUARD_HALF + 4.0 * math.sin(math.pi * 0.5 * t)), y(S_GUARD) + up * (2.6 * t ** 1.4), 0.0) for t in np.linspace(0, 1, 8)]
        ml.sweep(gh, hp, ml.circle(1.0, 6), scales=lambda t: (0.42 * (1.0 - 0.6 * t), 0.42 * (1.0 - 0.6 * t)))
    # ---- 8 the white handle, 9 its black bindings
    gp = w.add(m, "grip", "leather", base=(236, 236, 232), uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(y(S_GRIP[0]), 0.95, 0.95), (y(S_GRIP[0] + 120), 1.02, 1.02), (y(S_GRIP[1]), 1.0, 1.0)], ml.circle(R_GRIP, 12))
    bn = w.add(m, "grip_bindings", "leather", base=(24, 24, 30), wear=0.3, smooth=55)
    for s0, n in ((S_GRIP[1] - 22.0, 3), (S_GRIP[0] + 150.0, 2), (S_GRIP[0] + 12.0, 2)):                    # card: bindings at y 410, 525, 625 with diamond studs
        for i in range(n):
            ml.collar(bn, y(s0 + i * 9.0), R_GRIP * 1.16, 0.55, lip=0.15, sides=12)
    # ---- 10 the ring pommel (a torus in the blade plane), 11 its neck
    rg = w.add(m, "pommel_ring", "iron", base=(30, 32, 42), smooth=55)
    rc, rr = y(S_RING[0] + 22.0), 20.0 * K * 0.95
    rp = [(rr * math.cos(a), rc + rr * math.sin(a), 0.0) for a in [2 * math.pi * i / 20 for i in range(20)]]
    rp.append(rp[0])
    ml.sweep(rg, rp, ml.circle(0.5, 8), caps=(False, False))
    nk = w.add(m, "pommel_neck", "iron", base=(44, 46, 60), smooth=50)
    ml.lathe(nk, "y", (0, 0, 0), [(y(S_RING[1] - 12), 0.5), (y(S_GRIP[0]) - 0.2, R_GRIP * 0.85), (y(S_GRIP[0]) + 0.3, R_GRIP * 1.2)], sides=10)
    # ---- 12 the ring of time shards round the guard: little glowing crystals on a tilted ring
    sh = w.add(m, "time_shards", "crystal", base=(110, 220, 255), glow=True, smooth=0)
    for i in range(14):
        a = 2 * math.pi * i / 14
        rad = 5.6 + (0.8 if i % 2 else 0.0)
        cx, cz = rad * math.cos(a), rad * math.sin(a) * 0.9
        ml.gem(sh, (cx, y(S_GUARD) - 1.0 + 1.4 * math.sin(a * 2), cz), 0.28 + 0.1 * (i % 3), long=1.6, sides=4, axis="y")

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    w.paint_part(atlas, m.part("blade_vein"), lambda c: w.set_glow(c, (70, 215, 255), 1.0, core=0.5))
    w.paint_part(atlas, m.part("time_shards"), lambda c: w.set_glow(c, (110, 225, 255), 1.0, core=0.5))
    w.paint_part(atlas, m.part("blade"), lambda c: w.set_glow(c, (40, 160, 230), 0.05, core=0.0))
    ref = w.reference("p3/Relics of the world part 3/f122a6c90f5b6048fac812a05ba3315b.jpg", (0, 0, 200, 733), "time_bender_dadao")
    w.finish("time_bender_dadao", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.22, 0.5, 1.0), ref=ref)

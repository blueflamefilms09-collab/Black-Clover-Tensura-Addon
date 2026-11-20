"""REDO of two exemplar forms that read as flat PNG cards (owner): the same mesh ids, the new mesh REPLACES the old one.

    python3 -B tools/blender/forms_redo.py --only slime [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N] [--sheet]

legendary_shield__slime_shield : a real DOMED shield. A thick sculpted dome of translucent slime (lumps, rolling bulges, three overflowing lobes, pseudopod arms with bulb ends, long
                                 drips, bubbles and a nucleus baked inside the glass-like body), a thick riveted bronze rim torus with clamps, the nucleus standing proud in a claw cage.
legendary_sword__phoenix_sword : a FLAME-SHAPED blade (a flamberge: both edges lick upward in alternating tongues over an S-bend, thick lens section with a molten spine), a heart guard
                                 with the ruby, wings of cupped, quilled, layered plumes with real thickness (not flat cards), tail plumes, wrapped grip, flame tongues.
Palettes come from forms_exemplars.py (imported, not edited). Same place lines as the base weapons (they are reused from the base mesh files).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
import forms_v_common as C  # noqa: E402
import forms_exemplars as E  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402


# ================================================================ SLIME SHIELD (legendary_shield)
def slime_shield(opts):
    C.begin(opts["samples"])
    P = E.slime_palette()
    rr = K.rng(77)
    # ---- the gel dome: thick, convex, lumpy; a glassy shader so the bake sees the nucleus and the bubbles inside
    prof = [(0.01, 16.5), (3.0, 16.3), (7.0, 15.4), (11.0, 13.8), (15.0, 11.6), (19.0, 9.0), (23.0, 6.2), (26.5, 3.6), (28.8, 1.6), (29.6, 0.2), (28.6, -1.4), (20.0, -1.6), (0.01, -1.6)]
    body = K.bl.lathe("gel_body", prof, P["gel"], sides=40, axis="z", caps=(False, False), part="gel")
    K._finish_obj(body, "gel", True, 70.0)
    K.displace(body, 1.5, 0.12, 5, 3, mask=lambda co: K.smoothstep(2.0, 10.0, math.hypot(co.x, co.y)) * (1.0 - K.smoothstep(25.0, 29.5, math.hypot(co.x, co.y))) + 0.12)
    # ---- inside the gel (bake only): the nucleus' halo, bubbles of every size, a swirl of denser gel
    K.blob("core_halo", (0, 0, 6.5), (7.4, 7.4, 4.4), P["core"], seed=3, lump=0.1, detail=True, part="inside")
    for i in range(38):
        a, rd = rr.uniform(0, TAU), rr.uniform(5.0, 26.0)
        sz = rr.choice((0.5, 0.8, 1.1, 1.6, 2.4, 3.4)) * rr.uniform(0.8, 1.2)
        K.blob("bub%d" % i, (rd * math.cos(a), rd * math.sin(a), rr.uniform(1.4, 3.0 + 9.0 * (1 - rd / 30))), (sz, sz, sz), P["bubble"], seed=i, lump=0.08, rings=6, segs=8, detail=True, part="inside")
    # ---- the nucleus: a lumpy glowing core standing proud in a bronze claw cage
    K.blob("core", (0, 0, 14.6), (5.6, 5.6, 5.0), P["core"], seed=9, lump=0.12, rings=9, segs=14, part="core")
    cage = K.bl.lathe("core_ring", [(7.8, 12.2), (8.0, 13.6), (7.0, 15.2), (6.4, 14.8), (7.2, 13.4), (7.2, 12.2), (7.8, 12.2)], P["bronze"], sides=22, axis="z", caps=(False, False), part="core_cage")
    K._finish_obj(cage, "core_cage", True, 60.0)
    for k in range(7):
        a = TAU * k / 7 + rr.uniform(-0.1, 0.1)
        K.tapered("cage_claw%d" % k, [V((7.5 * math.cos(a), 7.5 * math.sin(a), 13.6)), V((7.6 * math.cos(a), 7.6 * math.sin(a), 16.8)), V((4.0 * math.cos(a), 4.0 * math.sin(a), 19.0))], 0.8, 0.2, P["bronze"], sides=4, sub=2, part="core_cage")
    # ---- rolling bulges on the dome, three overflow lobes spilling over the rim, pseudopod arms with bulb ends, long drips, loose droplets
    for i, (a, rd, sz) in enumerate(((0.6, 17.0, 4.8), (1.9, 20.0, 3.6), (3.2, 15.5, 5.2), (4.3, 21.0, 3.4), (5.2, 18.0, 4.4), (2.5, 24.0, 2.8), (5.9, 24.5, 2.6), (0.0, 9.0, 3.0), (3.9, 10.0, 3.2))):
        K.blob("bulge%d" % i, (rd * math.cos(a), rd * math.sin(a), 6.5 + 7.0 * (1 - rd / 30)), (sz * 1.2, sz, sz * 0.8), P["gel2"] if i % 3 == 0 else P["gel"], seed=40 + i, lump=0.12, rings=7, segs=11, part="gel")
    for i, a in enumerate((math.radians(35), math.radians(150), math.radians(262))):
        K.blob("lobe%d" % i, (31.0 * math.cos(a), 31.0 * math.sin(a), 2.8), (5.6, 4.2, 3.4), P["gel"], seed=60 + i, lump=0.15, rings=7, segs=10, part="gel")
    for i, ang in enumerate((20, 100, 190, 285, 330)):
        a = math.radians(ang)
        ca, sa = math.cos(a), math.sin(a)
        pts = [V((29.0 * ca, 29.0 * sa, 2.0)), V((34.0 * ca, 34.0 * sa, 3.0)), V((38.0 * ca - 2.0 * sa, 38.0 * sa + 2.0 * ca, 0.5)), V((40.0 * ca - 5.0 * sa, 40.0 * sa + 5.0 * ca, -3.5))]
        K.tapered("pseudo%d" % i, pts, 3.4, 1.0, P["gel"], sides=7, sub=3, part="pseudopods", bulge=lambda t: 1.0 + 0.25 * math.sin(math.pi * t))
        e = pts[-1]
        K.blob("pseudo_bulb%d" % i, tuple(e), (2.8, 2.8, 2.6), P["gel2"] if i % 2 else P["gel"], seed=70 + i, lump=0.12, rings=6, segs=9, part="pseudopods")
    for i, ang in enumerate((205, 222, 238, 252, 266, 281, 296, 313, 330)):
        a = math.radians(ang + rr.uniform(-3, 3))
        K.drip("drip%d" % i, (31.0 * math.cos(a), 31.0 * math.sin(a), 0.5), rr.uniform(6.0, 14.0), rr.uniform(1.3, 2.2), P["gel2"] if i % 4 == 1 else P["gel"], seed=50 + i, sides=7, part="drips")
    K.shard_cloud("droplet", (0, -6, 6), (36.0, 38.0, 8.0), 10, (0.8, 1.7), P["gel"], rr, avoid=lambda p: math.hypot(p.x, p.y) < 34.0 and abs(p.z) < 17 and p.z > -6.5, part="droplets", stretch=0.7, sides=4, max_count=10)
    # ---- the rim: a thick bronze torus, plate joints, clamps with rivets, a ring of studs, a patina
    C.ring_torus("rim_torus", V((0, 0, 1.4)), 30.6, 3.0, P["bronze"], normal=(0, 0, 1), sides=8, segs=48, part="rim", squash=1.15)
    for k in range(12):
        a = TAU * k / 12 + 0.02
        K.bar("joint%d" % k, (30.6 * math.cos(a), 30.6 * math.sin(a), 4.9), (31.2 * math.cos(a), 31.2 * math.sin(a), -2.2), 0.3, 1.0, P["bronze_dark"], part="rim", taper=1.0, bevel_w=0.0)
    for k in range(8):
        a = TAU * (k + 0.5) / 8 + rr.uniform(-0.04, 0.04)
        c = K.plate("clamp%d" % k, [(-2.6, -2.7), (2.6, -2.7), (3.0, 2.7), (-3.0, 2.7)], -0.5, 0.5, P["bronze_dark"], part="rim_clamps", bevel_w=0.28)
        K.bl.move(c, (29.0, 0, 4.9))
        K.bl.move(c, rot=(0, 0, math.degrees(a)))
        K.rivets("clamp_rivets%d" % k, [(V((29.0 * math.cos(a) - dy * math.sin(a), 29.0 * math.sin(a) + dy * math.cos(a), 5.5)), (0, 0, 1)) for dy in (-1.7, 1.7)], P["bronze"], r=0.8, rng_=rr, detail=False, sides=5)
    ring_pts = [(V((30.6 * math.cos(TAU * k / 36), 30.6 * math.sin(TAU * k / 36), 4.55)), (0, 0, 1)) for k in range(36)]
    K.rivets("rim_studs", ring_pts, P["bronze"], r=0.55, rng_=rr, detail=True, jitter=0.25)
    # ---- back: a cup, an arm strap with rivets, a handle bar
    cup = K.bl.lathe("back_cup", [(0.01, -6.4), (7, -6.6), (15, -5.8), (22, -4.4), (27, -2.4), (28.0, -0.6)], P["bronze_dark"], sides=32, axis="z", caps=(False, True), part="back")
    K._finish_obj(cup, "back", True, 50.0)
    K.bar("handle", (-9, 0.0, -9.0), (9, 0.0, -9.0), 1.5, 1.5, P["leather"], part="handle_bar", bevel_w=0.2)
    for sx in (-1, 1):
        K.bar("handle_post%d" % sx, (sx * 9.0, 0, -9.0), (sx * 10.5, 0, -5.8), 1.4, 1.2, P["bronze"], part="handle_bar")
    sp = [V((-17 + 34 * t, 9.0 * math.sin(t * math.pi) - 4.0, -6.0 - 2.6 * math.sin(t * math.pi) * 0.4)) for t in [i / 10 for i in range(11)]]
    K.ribbon("arm_strap", sp, 4.4, P["leather"], facing=(0, 0, -1), thick=0.4, part="arm_strap", rng_=rr, edge_wave=0.03)
    K.rivets("strap_rivets", [(V((-16.5, -4.0, -6.1)), (0, 0, -1)), (V((16.5, -4.0, -6.1)), (0, 0, -1)), (V((-14.0, -1.0, -6.4)), (0, 0, -1)), (V((14.0, -1.0, -6.4)), (0, 0, -1))], P["bronze"], r=0.8, detail=False, sides=6)
    return K.quad_estimate()


# ================================================================ PHOENIX SWORD (legendary_sword)
def phoenix_sword(opts):
    C.begin(opts["samples"])
    P = E.phoenix_palette()
    rr = K.rng(2026)
    ymid = 31.0

    def bend(y):                                                  # a lazy S and a tip that licks left like a flame
        return 1.4 * math.sin((y - 32) / 118 * math.pi * 1.5) + 6.0 * max(0.0, (y - 108) / 42.0) ** 2.0

    def hw(y):
        return K.interp([(32, 6.2), (40, 7.0), (60, 6.8), (84, 6.2), (110, 5.4), (128, 4.4), (142, 2.6), (150, 0.4)], y)

    def tongue(y, phase, amp, period):                           # smooth flame licks: each swell leans up the blade and ends in a point
        u = ((y - 34.0) / period + phase) % 1.0
        w = math.sin(math.pi * u ** 0.8) ** 1.6
        return amp * w * K.smoothstep(34.0, 52.0, y) * (1.0 - K.smoothstep(132.0, 148.0, y))
    ys = [32.0 + i * 2.7 for i in range(0, 44)] + [150.0]
    Lp = [V((-(hw(y) + tongue(y, 0.0, 3.6, 15.0)) + bend(y), y, 0.0)) for y in ys]
    Rp = [V(((hw(y) + tongue(y, 0.45, 3.2, 17.0)) + bend(y), y, 0.0)) for y in ys]
    Lp[-1] = V((-1.4 + bend(150), 151.0, 0.0))
    Rp[-1] = V((0.8 + bend(150), 149.0, 0.0))
    blade = K.blade("blade", P["steel"], Lp, Rp, thick=lambda t: 1.6 * (1.0 - 0.5 * t ** 2.0), fuller=0.22, fuller_depth=0.55, fuller_off=0.05, seed=3, wob=0.18, bevel_w=0.22)
    # molten spine and a row of flame-feather ridges standing on the flat (real geometry), on both faces
    for s in (1, -1):
        pts = [(bend(y) + 0.15 * math.sin(y * 0.3), y) for y in range(40, 140, 4)]
        K.seam("fuller_glow%d" % s, pts, P["fuller"], w=0.6, depth=0.14, z=1.55 * 0.5 * s, side=s, part="blade_glow", detail=True)
    # tongues of flame standing out of the edges (the silhouette)
    for side, ph, amp, per in ((-1, 0.0, 3.4, 15.0), (1, 0.45, 3.0, 17.0)):
        k = 0
        for y in [34.0 + per * (j + 0.95 - ph * (side > 0)) for j in range(0, 8)]:
            if y > 140 or y < 48:
                continue
            x = side * (hw(y) + tongue(y, ph, amp, per) * 0.9) + bend(y)
            ang = math.radians(rr.uniform(34, 52))
            d = V((side * math.sin(ang), math.cos(ang), 0.0))
            K.feather("tongue%d_%d" % (side, k), V((x, y, rr.choice((-0.4, 0.0, 0.4)))), d, rr.uniform(6.0, 10.0) * (1.0 + 0.25 * (y - 32) / 118), rr.uniform(2.2, 3.2), P["flame"], facing=(0, 0, 1), curl=rr.uniform(-1.0, 1.2),
                      serrate=0.1, rows=5, thick=0.2, seed=200 + k + (0 if side < 0 else 40), part="flames", bend=V((-side * rr.uniform(0.0, 2.5), rr.uniform(0.5, 2.0), 0)), profile=lambda t: (min(1.0, t / 0.18)) * (1 - t) ** 0.7)
            k += 1
    for s in (1, -1):                                                       # rivets and plate seams at the ricasso
        K.rivets("ricasso_rivets%d" % s, [(V((x, y, 1.62 * s)), (0, 0, s)) for x, y in ((-4.6, 36), (4.6, 36), (-5.2, 42), (5.2, 42))], P["gold"], r=0.7, rng_=rr, jitter=0.2)
    # ---- guard: a heart plate with the ruby, a bar, and the WINGS: three tiers of cupped, quilled, layered plumes with real thickness
    heart = [(-6.4, ymid - 1.5), (-7.2, ymid + 2.6), (-5.0, ymid + 6.3), (-2.0, ymid + 7.0), (0.0, ymid + 8.6), (2.0, ymid + 7.0), (5.0, ymid + 6.3), (7.2, ymid + 2.6), (6.4, ymid - 1.5), (3.6, ymid - 4.6), (0, ymid - 5.6), (-3.6, ymid - 4.6)]
    for s in (1, -1):
        K.stepped_plate("heart%d" % s, heart, 1.1 * s, P["gold"], steps=2, step_h=0.8, inset=1.1, part="guard", up=s)
        K.gem_setting("ruby%d" % s, (0, ymid + 1.0, 3.4 * s), 3.1, P["gem"], P["gold"], normal=(0, 0, s), claws=5, sides=8, part="gem", rng_=rr, claw_r=0.5)
    K.bar("guard_bar", (-13.5, ymid - 0.5, 0), (13.5, ymid - 0.5, 0), 2.4, 1.5, P["gold_dark"], part="guard", taper=0.6)
    tiers = [(7.0, ymid + 2.6, (14, 70), 4, 24.0, 6.6, 1.4), (7.5, ymid + 0.4, (-4, 38), 3, 23.0, 6.0, 2.0), (6.5, ymid - 2.0, (-24, -62), 3, 18.0, 5.4, 2.6)]
    n_f = 0
    for side in (1, -1):
        rs = K.rng(40 + side)
        for ti, (cx, cy, (a0, a1), cnt, ln, wd, dz) in enumerate(tiers):
            for j in range(cnt):
                u = j / max(cnt - 1, 1)
                a = math.radians(K.lerp(a0, a1, u) + rs.uniform(-4, 4))
                if side < 0:
                    a = math.pi - a
                d = V((math.cos(a), math.sin(a), 0.0))
                C.plume("wing%d_%d_%d" % (side, ti, j), V((side * cx, cy, dz + 0.45 * (j % 2))), d, ln * rs.uniform(0.86, 1.1), wd * rs.uniform(0.88, 1.12), P["wingf"] if (j + ti) % 2 == 0 else P["wingf2"],
                        facing=(0, 0, 1), curl=rs.uniform(1.0, 3.0), serrate=0.18, rows=4, thick=0.34, seed=rs.randint(0, 999), part="wings", droop=rs.uniform(0.5, 1.5), cup=0.32)
                n_f += 1
    # ---- grip, wraps, pommel, tail plumes
    for o in K.grip_wrapped("grip", 8.5, 26.5, 2.0, P["wrap"], P["cord"], turns=8.0, cord=0.36, bands=(9.4, 17.4, 25.4), band_mat=P["gold"], swell=0.16, part="grip", seed=3):
        pass
    pom = K.bl.lathe("pommel_cage", [(0.01, 0.0), (2.0, 0.3), (3.3, 1.4), (3.6, 3.0), (3.0, 4.6), (2.2, 5.6), (2.3, 7.0), (2.6, 8.0)], P["gold_dark"], sides=12, part="pommel")
    K._finish_obj(pom, "pommel")
    for k in range(6):
        a = TAU * k / 6 + 0.2
        K.tapered("cage%d" % k, [V((2.5 * math.cos(a), 1.0, 2.5 * math.sin(a))), V((4.5 * math.cos(a), 3.4, 4.5 * math.sin(a))), V((2.6 * math.cos(a), 6.4, 2.6 * math.sin(a)))], 0.45, 0.32, P["gold"], sides=5, part="pommel")
    K.blob("pommel_ember", (0, 3.4, 0), (2.7, 2.6, 2.7), P["gem"], seed=4, lump=0.1, part="gem", rings=8, segs=10)
    for i, (ang, ln) in enumerate(((-26, 24.0), (26, 22.0))):
        a = math.radians(ang)
        C.plume("tailplume%d" % i, V((2.0 * math.sin(a), 0.5, -1.0 * math.cos(a))), V((math.sin(a) * 0.7, -1.0, 0.1)), ln, 4.8, P["plume"], facing=(1, 0, 0), curl=1.6, serrate=0.22, rows=6, thick=0.3, seed=300 + i, part="pommel_plumes",
                bend=V((1.5 * math.sin(a), 0, 1.0)), cup=0.3)
    for i, (ang, ln) in enumerate(((-24, 17), (30, 15))):
        a = math.radians(ang)
        path = [V((2.2 * math.sin(a) * (1 + 0.4 * k), 24.5 - k * ln / 9.0, 2.0 * math.cos(a) * 0.4 + 0.6 * math.sin(k * 0.9 + i))) + V((1.5 * math.sin(a) * k * 0.4, 0, 0)) for k in range(10)]
        K.ribbon("tail%d" % i, path, lambda t: 1.5 * (1.0 - 0.4 * t) + 0.3, P["wrap"], facing=(0, 0, 1), thick=0.06, part="grip_tails", fray=0.7, rng_=rr, edge_wave=0.08, notch=0.9 if i == 1 else 0.0)

    def blade_zone(p):
        return abs(p.x - bend(p.y)) < hw(min(max(p.y, 32), 150)) + 4.0 and 28 < p.y < 152 and abs(p.z) < 3.5
    K.shard_cloud("ember", (0, 92, 0), (20.0, 62.0, 9.0), 4, (0.45, 1.0), P["ember"], rr, avoid=lambda p: blade_zone(p) or (p.y < 38 and abs(p.x) < 28), part="embers", flow=lambda p: V((-(p.x) * 0.2 + rr.uniform(-1, 1), 1.0, rr.uniform(-1, 1) * 0.5)), stretch=1.8, max_count=4, sides=3)
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["slime"] = (slime_shield, "legendary_shield__slime_shield", "legendary_shield", "slime_shield", "shield")
BUILDERS["phoenix"] = (phoenix_sword, "legendary_sword__phoenix_sword", "legendary_sword", "phoenix_sword", "longsword")

if __name__ == "__main__":
    C.run_family(sys.argv[1:], BUILDERS, "redo")

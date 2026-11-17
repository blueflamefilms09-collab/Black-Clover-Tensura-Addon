"""The three EXEMPLAR weapon forms, built with tools/blender/forms_kit.py. They set the bar the other form builders must reach.

    python3 -B tools/blender/forms_exemplars.py [--only phoenix,slime,dragon] [--trial DIR] [--beauty DIR] [--quick] [--samples N]

mesh ids (docs/heroweapon/form_models.md): legendary_sword__phoenix_sword, legendary_shield__slime_shield, vassal_scythe__demon_dragon_scythe.
--trial DIR   write meshes + skins into DIR (weapon_meshes/, textures/entity/weapon/) instead of the mod's assets
--beauty DIR  write Cycles beauty stills (before the bake) into DIR
--quick       no bake, only the beauty stills and the quad estimate (look first)
Seeded: the same seed gives the same weapon (deterministic).
"""
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

MANIFEST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "forms_manifest_exemplars.json")


# ================================================================ 1. PHOENIX SWORD (legendary_sword)
def phoenix_palette(S=1):
    P = {}
    P["gold"] = K.surface("gold", rgb(255, 196, 72), 1.0, 0.26, wear=0.85, wear_color=rgb(255, 244, 200), dirt=0.75, dirt_color=(0.06, 0.025, 0.004, 1), scratches=0.7,
                          bump="hammer", bump_scale=0.7, bump_strength=0.2, mottle=0.7, mottle_color=rgb(150, 82, 22), fine=0.6, seed=1, bevel=0.18, ao_dist=2.0)
    P["gold_dark"] = K.surface("gold_dark", rgb(168, 104, 36), 1.0, 0.4, wear=0.8, wear_color=rgb(240, 180, 80), dirt=0.9, dirt_color=(0.03, 0.012, 0.004, 1), scratches=0.5,
                               bump="brushed", bump_strength=0.12, mottle=0.7, mottle_color=rgb(90, 46, 14), seed=2, bevel=0.14)
    P["steel"] = K.surface("phoenix_steel", rgb(96, 14, 10), 0.9, 0.34, wear=1.0, wear_color=rgb(255, 170, 70), wear_metal=0.7, dirt=0.9, dirt_color=(0.01, 0.003, 0.003, 1),
                           scratches=0.8, bump="hammer", bump_scale=0.5, bump_strength=0.24, mottle=1.0, mottle_color=rgb(30, 6, 6), veins=0.34, vein_color=rgb(255, 120, 24), fine=0.6, seed=3, bevel=0.2,
                           cramp=("y", 30.0, 152.0, [(0.0, rgb(40, 8, 8)), (0.35, rgb(96, 14, 10)), (0.75, rgb(190, 50, 12)), (1.0, rgb(255, 150, 40))]), ao_dist=2.5)
    P["flame"] = K.surface("flame_feather", rgb(190, 34, 12), 0.2, 0.5, wear=0.5, wear_color=rgb(255, 210, 90), mottle=0.5, mottle_color=rgb(110, 14, 6), dirt=0.2, feather=(7, 3, 0.45, 0.4),
                           tramp=[(0.0, rgb(150, 18, 6)), (0.5, rgb(255, 84, 12)), (1.0, rgb(255, 214, 90))], emit=rgb(255, 120, 20), emit_strength=1.3, temit=[(0.0, rgb(255, 50, 8)), (1.0, rgb(255, 190, 60))],
                           emit_flicker=0.7, seed=4, bevel=0.08)
    P["plume"] = K.surface("plume", rgb(214, 44, 18), 0.35, 0.42, wear=0.7, wear_color=rgb(255, 176, 60), wear_metal=0.8, mottle=0.9, mottle_color=rgb(120, 16, 8), dirt=0.55, scratches=0.35,
                           feather=(13, 5, 0.65, 0.7), tramp=[(0.0, rgb(70, 8, 8)), (0.35, rgb(170, 26, 14)), (0.8, rgb(240, 90, 24)), (1.0, rgb(255, 190, 70))], seed=5, bevel=0.08, fine=0.4)
    P["wingf"] = K.surface("wing_feather", rgb(255, 190, 70), 1.0, 0.3, wear=0.8, wear_color=rgb(255, 244, 190), dirt=0.6, dirt_color=(0.05, 0.02, 0.004, 1), scratches=0.5, feather=(13, 6, 0.75, 0.9),
                           tramp=[(0.0, rgb(130, 76, 24)), (0.4, rgb(220, 150, 50)), (1.0, rgb(255, 228, 130))], mottle=0.5, mottle_color=rgb(150, 80, 20), seed=12, bevel=0.1, fine=0.5)
    P["wingf2"] = K.surface("wing_feather2", rgb(200, 60, 24), 0.8, 0.34, wear=0.8, wear_color=rgb(255, 200, 120), dirt=0.6, scratches=0.5, feather=(13, 6, 0.75, 0.9),
                            tramp=[(0.0, rgb(70, 14, 10)), (0.5, rgb(190, 50, 20)), (1.0, rgb(255, 170, 60))], mottle=0.5, mottle_color=rgb(90, 14, 8), seed=13, bevel=0.1, fine=0.5)
    P["gem"] = K.surface("ruby", rgb(210, 20, 16), 0.1, 0.06, emit=rgb(255, 70, 20), emit_strength=1.2, emit_flicker=0.5, mottle=0.8, mottle_color=rgb(255, 120, 40), seed=6, bevel=0.0)
    P["wrap"] = K.surface("wrap", rgb(92, 14, 14), 0.0, 0.62, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(22, 4, 4), groove_amount=0.9, dirt=0.8, dirt_color=(0.01, 0.003, 0.003, 1),
                          mottle=0.8, mottle_color=rgb(40, 6, 6), wear=0.6, wear_color=rgb(170, 70, 40), wear_metal=0.0, fine=0.6, seed=7, bevel=0.0)
    P["cord"] = K.surface("cord", rgb(230, 170, 60), 0.5, 0.45, bump="wrap", bump_strength=0.7, groove=rgb(70, 36, 8), groove_amount=0.8, wear=0.7, dirt=0.6, seed=8, bevel=0.0, fine=0.5)
    P["ember"] = K.surface("ember", rgb(255, 150, 40), 0.0, 0.5, emit=rgb(255, 150, 40), emit_strength=2.0, emit_flicker=0.6, seed=9, bevel=0.0)
    P["fuller"] = K.surface("fuller_glow", rgb(255, 120, 20), 0.0, 0.5, emit=rgb(255, 130, 24), emit_strength=1.8, emit_flicker=0.9, seed=10, bevel=0.0,
                            emit_ramp=("y", 32.0, 150.0, [(0.0, rgb(255, 70, 8)), (0.6, rgb(255, 150, 30)), (1.0, rgb(255, 230, 130))]))
    P["rune"] = K.surface("rune_glow", rgb(255, 140, 30), 0.0, 0.5, emit=rgb(255, 150, 40), emit_strength=1.6, bevel=0.0)
    P["iron"] = K.surface("black_iron", rgb(40, 24, 24), 1.0, 0.5, wear=0.9, wear_color=rgb(190, 150, 120), dirt=0.8, scratches=0.5, bump="hammer", bump_strength=0.2, mottle=0.5, seed=11, bevel=0.15)
    return P


def phoenix_sword(opts):
    K.begin(opts["samples"])
    P = phoenix_palette()
    rr = K.rng(2026)
    ymid = 31.0
    # ---------------------------------------------------------------- blade: a flame-forged edge, hooked tip, off-centre fuller, tongue-notched asymmetric edges
    def hw(y):
        return K.interp([(32, 6.4), (38, 7.2), (46, 6.6), (62, 6.2), (84, 6.0), (106, 5.5), (124, 4.7), (138, 3.4), (146, 1.8), (150, 0.5)], y)

    def bend(y):                                                 # the whole blade breathes: a lazy S and a tip that hooks left like a flame
        return 0.7 * math.sin((y - 32) / 118 * math.pi * 1.4) + 5.0 * max(0.0, (y - 112) / 38.0) ** 2.2

    stL = [(y, hw(y)) for y in (32, 38, 46, 84, 124, 138, 146, 150)]
    notchL = [(47, 1.0, 3.0, "chip"), (60, 1.6, 4.0, "tooth"), (73, 1.2, 3.0, "step"), (92, 1.7, 5.0, "tooth"), (106, 1.3, 3.2, "chip"), (120, 1.1, 3.0, "v"), (133, 1.0, 3.0, "tooth")]
    notchR = [(42, 0.9, 2.6, "v"), (55, 1.3, 3.2, "chip"), (68, 1.9, 5.0, "tooth"), (84, 1.0, 3.0, "step"), (99, 1.0, 2.6, "chip"), (112, 1.8, 5.0, "tooth"), (127, 0.9, 2.8, "v"), (139, 0.8, 2.4, "v")]
    L = K.blade_edge(-1, stL, 32, 150, notches=notchL, jitter=0.12, res=11.0, seed=11, tip=(-1.2, 151.0))
    R = K.blade_edge(1, stL, 32, 150, notches=notchR, jitter=0.12, res=11.0, seed=12, tip=(0.6, 149.0))
    L = [V((p.x + bend(p.y), p.y, 0)) for p in L]
    R = [V((p.x + bend(p.y), p.y, 0)) for p in R]
    blade = K.blade("blade", P["steel"], L, R, thick=lambda t: 1.35 * (1.0 - 0.45 * t ** 2.2), fuller=0.22, fuller_depth=0.5, fuller_off=0.1, seed=3, wob=0.14, bevel_w=0.2)
    # the glowing fuller: a hot channel lying in the groove (baked + glow), on both faces, following the bend
    for s in (1, -1):
        pts = [(bend(y) + 0.1 * hw(y) + 0.25 * math.sin(y * 0.4), y) for y in range(40, 138, 4)]
        K.seam("fuller_glow%d" % s, pts, P["fuller"], w=0.5, depth=0.12, z=1.35 * 0.55 * s * (1.0 - 0.45 * 0.3), side=s, part="blade_glow", detail=True)
    # rivets and plate seams along the blade spine / ricasso
    for s in (1, -1):
        K.rivets("ricasso_rivets%d" % s, [(V((x, y, 1.38 * s)), (0, 0, s)) for x, y in ((-4.6, 36), (4.6, 36), (-5.2, 42), (5.2, 42), (-4.4, 49), (4.4, 49))], P["gold"], r=0.7, rng_=rr, jitter=0.2)
        K.engrave_lines("ricasso_line%d" % s, [[(-6.0, 34.5), (6.0, 34.5)], [(-6.2, 51.5), (-3, 52.5), (0, 51.8), (3, 52.4), (6.1, 51.6)]], P["gold_dark"], side=s, z=1.37, w=0.1, part="blade")
    # ---------------------------------------------------------------- the plumage laid on the blade: overlapping plume plates, bigger at the guard, thinning up the blade
    for s in (1, -1):
        for col, sx in enumerate((-1, 1)):
            for i in range(3):
                y0 = 36.0 + i * 11.0 + rr.uniform(-1.5, 1.5) + (3.5 if col else 0.0)
                ln = 26.0 - i * 3.6 + rr.uniform(-1.5, 1.5)
                wd = 5.6 - i * 0.6 + rr.uniform(-0.4, 0.4)
                x0 = sx * (2.6 + 0.35 * i + rr.uniform(-0.3, 0.4))
                d = V((sx * math.sin(math.radians(6 + 3 * i + rr.uniform(-2, 3))), math.cos(math.radians(8)), 0))
                dz = 1.38 * s * 0.92 + s * (0.05 + 0.2 * i)
                K.feather("plume%d%d%d" % (s, col, i), V((x0 + bend(y0), y0, dz)), d, ln, wd, P["plume"], facing=(0, 0, s), curl=0.9 * s, serrate=0.24, rows=6,
                          thick=0.1, seed=100 + i * 7 + col * 3 + (0 if s > 0 else 50), part="plumage", droop=0.5 * s)
    # tongues of flame licking off both edges (real geometry: the silhouette)
    for side, notches in ((-1, notchL), (1, notchR)):
        for k, (ny, nd, nw, kind) in enumerate(notches):
            if k % 2 == 1 and kind in ("v", "chip") and rr.random() < 0.5:
                continue
            y = ny + rr.uniform(-1, 1)
            x = side * (hw(y) - nd * 0.4) + bend(y)
            ang = math.radians(rr.uniform(38, 66))
            d = V((side * math.sin(ang), math.cos(ang), 0))
            z = rr.choice((-0.5, 0.0, 0.5))
            K.feather("tongue%d_%d" % (side, k), V((x, y, z)), d, rr.uniform(5.0, 10.0) * (1.0 + 0.3 * (y - 32) / 118), rr.uniform(1.8, 3.0), P["flame"], facing=(0, 0, 1), curl=rr.uniform(-1.2, 1.4),
                      serrate=0.1, rows=5, thick=0.12, seed=200 + k + (0 if side < 0 else 40), part="flames",
                      bend=V((-side * rr.uniform(0.0, 2.5), rr.uniform(0.5, 2.0), 0)), profile=lambda t: (min(1.0, t / 0.18)) * (1 - t) ** 0.7)
    # ---------------------------------------------------------------- guard: a plate heart with a ruby and two spread wings of gold plumage (left and right are NOT twins)
    heart = [(-6.4, ymid - 1.5), (-7.2, ymid + 2.6), (-5.0, ymid + 6.3), (-2.0, ymid + 7.0), (0.0, ymid + 8.6), (2.0, ymid + 7.0), (5.0, ymid + 6.3), (7.2, ymid + 2.6), (6.4, ymid - 1.5), (3.6, ymid - 4.6), (0, ymid - 5.6), (-3.6, ymid - 4.6)]
    for s in (1, -1):
        K.stepped_plate("heart%d" % s, heart, 1.1 * s if s > 0 else -1.1, P["gold"], steps=2, step_h=0.8, inset=1.1, part="guard", up=s)
        K.gem_setting("ruby%d" % s, (0, ymid + 1.0, 3.4 * s if s > 0 else -3.4), 3.1, P["gem"], P["gold"], normal=(0, 0, s), claws=5, sides=8, part="gem", rng_=rr, claw_r=0.5)
        K.rivets("heart_rivets%d" % s, [(V((x, y, (1.1 + 0.62 * 3 - 1.6) * s)), (0, 0, s)) for x, y in ((-5.4, ymid - 0.5), (5.4, ymid - 0.5), (-3.2, ymid - 3.4), (3.2, ymid - 3.4), (-3.4, ymid + 5.0), (3.4, ymid + 5.0))], P["gold_dark"], r=0.62, rng_=rr)
        K.engrave_lines("heart_line%d" % s, [[(-5.8, ymid - 0.8), (-4.0, ymid + 5.0), (-1.2, ymid + 6.4)], [(5.8, ymid - 0.8), (4.0, ymid + 5.0), (1.2, ymid + 6.4)]], P["gold_dark"], side=s, z=2.9, w=0.09, part="guard")
    # bar under the plumage
    K.bar("guard_bar", (-13.5, ymid - 0.5, 0), (13.5, ymid - 0.5, 0), 2.4, 1.5, P["gold_dark"], part="guard", taper=0.6)
    K.bar("guard_barL", (-13.5, ymid - 0.5, 0), (-24.0, ymid + 2.5, 0.2), 1.4, 1.1, P["gold_dark"], part="guard", taper=0.5)
    # scrollwork arms of gold (real geometry) that curl out under the plumage, on both faces: left and right are drawn separately
    for side in (1, -1):
        rs2 = K.rng(70 + side)
        for k, (y0, rise, span) in enumerate(((ymid + 0.8, 4.5, 20.0), (ymid - 3.2, -2.5, 14.0))):
            end = (side * (7.5 + span), y0 + rise)
            pts = [(side * 7.0, y0), (side * (7.5 + span * 0.35), y0 + rise * 0.4 + rs2.uniform(-0.6, 0.6)), (side * (7.5 + span * 0.72), y0 + rise * 0.85), end]
            curl = K.spiral((end[0], end[1] - 1.9 * (1 if rise > 0 else -1) * 0.0 - 1.9), 1.9, 0.5, 1.15, start=math.pi / 2, n=9, flip=-side)
            for fs_ in (1, -1):
                K.filigree("scroll%d_%d_%d" % (side, k, fs_), pts + curl[1:], P["gold"], w=0.62, h=0.5, z=2.55, side=fs_, sub=3, part="guard_scroll", taper=(1.0, 0.7), detail=True)
    # runes inlaid in the blade flat, both faces (a baked, glowing relief)
    for s_ in (1, -1):
        K.runes("runes%d" % s_, K.RUNES[:7] if s_ > 0 else K.RUNES[7:14], 3.1, P["rune"], (bend(80) - 3.9, 82.0, 1.30 * s_), rot=(0, 0 if s_ > 0 else 180, 90), spacing=1.5, extrude=0.07)
    # wing plumage: three tiers each side, different seeds and counts left and right
    tiers = [(7.0, ymid + 2.6, (16, 74), 5, 22.0, 5.2, 0.0), (7.5, ymid + 0.4, (-2, 40), 4, 21.0, 4.8, 0.35), (6.5, ymid - 2.0, (-24, -62), 3, 16.0, 4.4, 0.7)]
    for side in (1, -1):
        rs = K.rng(40 + side)
        for ti, (cx, cy, (a0, a1), cnt, ln, wd, dz) in enumerate(tiers):
            cnt2 = cnt + (1 if side < 0 and ti == 0 else 0)
            a0m, a1m = (a0, a1) if side > 0 else (180 - a0, 180 - a1)
            fs = K.fan_feathers("wing%d_%d_" % (side, ti), (side * cx, cy, 0.9 + dz), cnt2, a0m, a1m, ln, wd, P["wingf"], rs, part="wings", len_jit=0.18, ang_jit=4, curl=2.4, layer_dz=0.3,
                                serrate=0.16, rows=5, thick=0.12, cascade=0.0, mat2=P["wingf2"])
    # ---------------------------------------------------------------- grip, wraps, pommel
    for o in K.grip_wrapped("grip", 8.5, 26.5, 2.0, P["wrap"], P["cord"], turns=8.0, cord=0.36, bands=(9.4, 17.4, 25.4), band_mat=P["gold"], swell=0.16, part="grip", seed=3):
        pass
    # hanging cloth tails from under the guard and from the pommel: torn crimson ribbons and three gold-tipped tail plumes
    for i, (ang, ln) in enumerate(((-24, 17), (12, 21), (34, 15))):
        a = math.radians(ang)
        path = [V((2.2 * math.sin(a) * (1 + 0.4 * k), 24.5 - k * ln / 9.0, 2.0 * math.cos(a) * 0.4 + 0.6 * math.sin(k * 0.9 + i))) + V((1.5 * math.sin(a) * k * 0.4, 0, 0)) for k in range(10)]
        K.ribbon("tail%d" % i, path, lambda t: 1.5 * (1.0 - 0.4 * t) + 0.3, P["wrap"], facing=(0, 0, 1), thick=0.06, part="grip_tails", fray=0.7, rng_=rr, edge_wave=0.08, notch=0.9 if i == 1 else 0.0)
    pom = K.bl.lathe("pommel_cage", [(0.01, 0.0), (2.0, 0.3), (3.3, 1.4), (3.6, 3.0), (3.0, 4.6), (2.2, 5.6), (2.3, 7.0), (2.6, 8.0)], P["gold_dark"], sides=12, part="pommel")
    K._finish_obj(pom, "pommel")
    for k in range(6):
        a = TAU * k / 6 + 0.2
        K.tapered("cage%d" % k, [V((2.5 * math.cos(a), 1.0, 2.5 * math.sin(a))), V((4.5 * math.cos(a), 3.4, 4.5 * math.sin(a))), V((2.6 * math.cos(a), 6.4, 2.6 * math.sin(a)))], 0.45, 0.32, P["gold"], sides=5, part="pommel")
    K.blob("pommel_ember", (0, 3.4, 0), (2.7, 2.6, 2.7), P["gem"], seed=4, lump=0.1, part="gem", rings=8, segs=10)
    for i, (ang, ln) in enumerate(((-30, 22.0), (0, 27.0), (28, 20.0))):
        a = math.radians(ang)
        K.feather("tailplume%d" % i, V((2.0 * math.sin(a), 0.5, -1.0 * math.cos(a))), V((math.sin(a) * 0.7, -1.0, 0.1)), ln * 0.55, 3.8, P["plume"], facing=(1, 0, 0), curl=1.6, serrate=0.22, rows=7, thick=0.1,
                  seed=300 + i, part="pommel_plumes", bend=V((1.5 * math.sin(a), 0, 1.0)))
    # ---------------------------------------------------------------- floating embers and feather fragments (bounded: 24)
    def blade_zone(p):
        return abs(p.x - bend(p.y)) < hw(min(max(p.y, 32), 150)) + 2.0 and 28 < p.y < 152 and abs(p.z) < 3.5

    def swirl(p):
        return V((-(p.x) * 0.2 + rr.uniform(-1, 1), 1.0, rr.uniform(-1, 1) * 0.5))
    K.shard_cloud("ember", (0, 92, 0), (20.0, 62.0, 9.0), 12, (0.45, 1.0), P["ember"], rr, avoid=lambda p: blade_zone(p) or (p.y < 38 and abs(p.x) < 28), part="embers", flow=swirl, stretch=1.8, max_count=12, sides=3)
    for i in range(2):                                           # a few drifting feather fragments
        K.feather("drift%d" % i, V((rr.choice((-1, 1)) * rr.uniform(10, 17), rr.uniform(70, 138), rr.uniform(-4, 4))), V((rr.uniform(-0.4, 0.4), 1, rr.uniform(-0.3, 0.3))), rr.uniform(5, 8), 1.8, P["flame"],
                  facing=(0, 0, 1), curl=1.0, serrate=0.15, rows=5, thick=0.08, seed=400 + i, part="embers")
    return K.quad_estimate()


# ================================================================ 2. SLIME SHIELD (legendary_shield)
def slime_palette():
    P = {}
    P["gel"] = K.surface("gel", rgb(70, 205, 190), 0.0, 0.10, transmission=0.92, ior=1.12, rim=(rgb(210, 255, 240), 0.7), mottle=0.6, mottle_color=rgb(40, 150, 200), bump="leather", bump_scale=0.35,
                         bump_strength=0.18, seed=21, bevel=0.0)
    P["gel2"] = K.surface("gel_green", rgb(120, 220, 120), 0.0, 0.10, transmission=0.9, ior=1.12, rim=(rgb(230, 255, 220), 0.7), mottle=0.5, mottle_color=rgb(60, 180, 160), seed=22, bevel=0.0)
    P["bronze"] = K.surface("bronze", rgb(176, 138, 68), 1.0, 0.32, wear=0.9, wear_color=rgb(250, 226, 150), dirt=0.9, dirt_color=(0.02, 0.03, 0.02, 1), scratches=0.8, bump="hammer", bump_scale=0.6, bump_strength=0.22,
                            mottle=1.0, mottle_color=rgb(50, 120, 100), fine=0.6, seed=23, bevel=0.2, ao_dist=2.0)
    P["bronze_dark"] = K.surface("bronze_dark", rgb(96, 74, 40), 1.0, 0.42, wear=0.9, wear_color=rgb(200, 170, 100), dirt=1.0, dirt_color=(0.02, 0.03, 0.02, 1), scratches=0.6, bump="brushed", bump_strength=0.14,
                                 mottle=1.0, mottle_color=rgb(40, 90, 80), seed=24, bevel=0.16)
    P["core"] = K.surface("core", rgb(120, 230, 60), 0.0, 0.2, emit=rgb(120, 230, 50), emit_strength=0.75, emit_flicker=0.6, mottle=0.9, mottle_color=rgb(230, 250, 90), seed=25, bevel=0.0, bump="leather", bump_strength=0.1)
    P["bubble"] = K.surface("bubble", rgb(120, 225, 205), 0.0, 0.05, emit=rgb(70, 200, 190), emit_strength=0.3, seed=26, bevel=0.0)
    P["leather"] = K.surface("leather", rgb(86, 56, 30), 0.0, 0.6, bump="leather", bump_scale=1.4, bump_strength=0.7, groove=rgb(26, 14, 6), groove_amount=0.8, wear=0.8, wear_color=rgb(150, 110, 70), wear_metal=0.0, dirt=0.9,
                             mottle=0.8, mottle_color=rgb(46, 26, 12), seed=27, bevel=0.0)
    return P


def slime_shield(opts):
    K.begin(opts["samples"])
    P = slime_palette()
    rr = K.rng(77)
    # ---- the gel body: a lens of gel, lumpy, with a crater round the core; glassy shader, so the bake sees the bubbles and the nucleus inside it
    prof = [(0.01, 10.6), (3.0, 10.4), (7.0, 9.9), (11.0, 9.0), (15.0, 7.8), (19.0, 6.4), (22.5, 5.0), (25.5, 3.6), (27.6, 2.4), (28.6, 1.2), (28.6, 0.2), (26.0, -0.3), (14.0, -0.3), (0.01, -0.3)]
    body = K.bl.lathe("gel_body", prof, P["gel"], sides=34, axis="z", caps=(False, False), part="gel")
    K._finish_obj(body, "gel", True, 70.0)
    K.displace(body, 1.3, 0.11, 5, 3, mask=lambda co: K.smoothstep(2.0, 12.0, math.hypot(co.x, co.y)) * (1.0 - K.smoothstep(24.0, 28.5, math.hypot(co.x, co.y))) + 0.15)
    # ---- inside the gel (bake only): the nucleus' halo, bubbles of every size
    K.blob("core_halo", (0, 0, 5.2), (6.4, 6.4, 3.6), P["core"], seed=3, lump=0.1, detail=True, part="inside")
    for i in range(34):
        a, rd = rr.uniform(0, TAU), rr.uniform(4.0, 25.5)
        sz = rr.choice((0.5, 0.8, 1.1, 1.6, 2.4, 3.2)) * rr.uniform(0.8, 1.2)
        K.blob("bub%d" % i, (rd * math.cos(a), rd * math.sin(a), rr.uniform(2.0, 4.0 + 5.0 * (1 - rd / 30))), (sz, sz, sz), P["bubble"], seed=i, lump=0.08, rings=6, segs=8, detail=True, part="inside")
    # ---- nucleus: a lumpy glowing core standing proud of the gel in a brass claw cage
    K.blob("core", (0, 0, 10.2), (5.0, 5.0, 4.4), P["core"], seed=9, lump=0.12, rings=9, segs=14, part="core")
    prof2 = [(7.2, 8.6), (7.4, 10.0), (6.4, 11.4), (5.8, 11.0), (6.6, 9.8), (6.6, 8.6), (7.2, 8.6)]
    cage = K.bl.lathe("core_ring", prof2, P["bronze"], sides=20, axis="z", caps=(False, False), part="core_cage")
    K._finish_obj(cage, "core_cage", True, 60.0)
    for k in range(7):
        a = TAU * k / 7 + rr.uniform(-0.1, 0.1)
        K.tapered("cage_claw%d" % k, [V((6.8 * math.cos(a), 6.8 * math.sin(a), 10.0)), V((6.9 * math.cos(a), 6.9 * math.sin(a), 13.0)), V((3.6 * math.cos(a), 3.6 * math.sin(a), 15.0))], 0.7, 0.18, P["bronze"], sides=4, sub=2, part="core_cage")
    # ---- gel bulges, drips over the rim, floating droplets
    for i, (a, rd, sz) in enumerate(((0.6, 17.0, 4.4), (1.9, 20.0, 3.2), (3.2, 15.5, 4.8), (4.3, 21.0, 3.0), (5.2, 18.0, 4.0), (2.5, 24.0, 2.4), (5.9, 24.5, 2.2))):
        K.blob("bulge%d" % i, (rd * math.cos(a), rd * math.sin(a), 4.2 + 8.0 * (1 - rd / 30)), (sz * 1.1, sz, sz * 0.7), P["gel2"] if i % 3 == 0 else P["gel"], seed=40 + i, lump=0.12, rings=6, segs=10, part="gel")
    for i, ang in enumerate((205, 222, 238, 252, 266, 281, 296, 313, 330)):
        a = math.radians(ang + rr.uniform(-3, 3))
        K.drip("drip%d" % i, (30.4 * math.cos(a), 30.4 * math.sin(a), 0.9), rr.uniform(4.5, 10.5), rr.uniform(1.0, 1.8), P["gel2"] if i % 4 == 1 else P["gel"], seed=50 + i, sides=7, part="drips")
    for i in range(3):                                           # overflow lobes spilling over the rim onto its face
        a = math.radians(rr.uniform(0, 360))
        K.blob("lobe%d" % i, (29.6 * math.cos(a), 29.6 * math.sin(a), 2.6), (3.4, 2.2, 2.0), P["gel"], seed=60 + i, lump=0.15, rings=5, segs=8, part="gel")
    K.shard_cloud("droplet", (0, -6, 4), (31.0, 33.0, 8.0), 12, (0.7, 1.6), P["gel"], rr, avoid=lambda p: math.hypot(p.x, p.y) < 33.0 and abs(p.z) < 11 and p.z > -6.5, part="droplets", stretch=0.7, sides=4, max_count=12)
    # ---- reinforced rim: a riveted bronze band in plates with clamps, a stud ring, and a patina
    rim = K.bl.lathe("rim_band", [(27.6, -2.2), (29.4, -2.6), (31.0, -1.8), (31.4, 0.2), (30.6, 2.4), (28.8, 3.2), (27.4, 2.4), (27.6, -2.2)], P["bronze"], sides=48, axis="z", caps=(False, False), part="rim")
    K._finish_obj(rim, "rim", True, 50.0)
    for k in range(12):                                          # plate joints: dark gaps between the 12 segments of the band
        a = TAU * k / 12 + 0.02
        K.bar("joint%d" % k, (30.4 * math.cos(a), 30.4 * math.sin(a), 3.0), (31.6 * math.cos(a), 31.6 * math.sin(a), -1.0), 0.25, 0.9, P["bronze_dark"], part="rim", taper=1.0, bevel_w=0.0)
    for k in range(8):                                           # chunky clamps
        a = TAU * (k + 0.5) / 8 + rr.uniform(-0.04, 0.04)
        c = K.plate("clamp%d" % k, [(-2.4, -2.5), (2.4, -2.5), (2.8, 2.5), (-2.8, 2.5)], -0.4, 0.4, P["bronze_dark"], part="rim_clamps", bevel_w=0.25)
        K.bl.move(c, (30.1, 0, 3.2))
        K.bl.move(c, rot=(0, 0, math.degrees(a)))
        K.rivets("clamp_rivets%d" % k, [(V((30.1 * math.cos(a) - dy * math.sin(a), 30.1 * math.sin(a) + dy * math.cos(a), 3.7)), (0, 0, 1)) for dy in (-1.6, 1.6)], P["bronze"], r=0.75, rng_=rr, detail=False, sides=5)
    ring_pts = [(V((29.4 * math.cos(TAU * k / 36), 29.4 * math.sin(TAU * k / 36), 3.25)), (math.cos(TAU * k / 36) * 0.3, math.sin(TAU * k / 36) * 0.3, 1)) for k in range(36)]
    K.rivets("rim_studs", ring_pts, P["bronze"], r=0.5, rng_=rr, detail=True, jitter=0.25)
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


# ================================================================ 3. DEMON DRAGON SCYTHE (vassal_scythe)
def dragon_palette():
    P = {}
    P["bone"] = K.surface("dragon_bone", rgb(214, 196, 164), 0.0, 0.55, bump="grain", bump_scale=1.5, bump_strength=0.3, mottle=1.0, mottle_color=rgb(110, 80, 52), wear=0.7, wear_color=rgb(240, 228, 200), wear_metal=0.0,
                          dirt=1.0, dirt_color=(0.05, 0.03, 0.02, 1), scratches=0.5, fine=0.7, seed=31, bevel=0.14, ao_dist=2.5)
    P["bone_dark"] = K.surface("bone_dark", rgb(86, 62, 48), 0.0, 0.5, bump="grain", bump_scale=1.2, bump_strength=0.35, mottle=1.0, mottle_color=rgb(30, 20, 18), wear=0.8, wear_color=rgb(190, 160, 120), wear_metal=0.0,
                               dirt=1.0, scratches=0.5, fine=0.7, seed=32, bevel=0.14)
    P["membrane"] = K.surface("membrane", rgb(120, 22, 26), 0.0, 0.45, tramp=[(0.0, rgb(40, 6, 12)), (0.5, rgb(110, 20, 28)), (1.0, rgb(190, 52, 24))], mottle=1.0, mottle_color=rgb(30, 6, 14),
                              veins=0.13, vein_color=rgb(255, 110, 20), vein_strength=0.6, bump="leather", bump_scale=0.5, bump_strength=0.35, wear=0.6, wear_color=rgb(230, 130, 70), wear_metal=0.0, dirt=0.7, dirt_color=(0.01, 0.003, 0.005, 1),
                              seed=33, bevel=0.06, fine=0.5)
    P["iron"] = K.surface("demon_iron", rgb(46, 40, 48), 1.0, 0.42, wear=1.0, wear_color=rgb(200, 190, 180), dirt=1.0, dirt_color=(0.01, 0.008, 0.01, 1), scratches=0.9, bump="hammer", bump_scale=0.55, bump_strength=0.25,
                          mottle=0.9, mottle_color=rgb(24, 20, 30), fine=0.6, seed=34, bevel=0.2, ao_dist=2.5)
    P["gold"] = K.surface("dragon_gold", rgb(206, 150, 56), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 230, 160), dirt=0.9, dirt_color=(0.04, 0.02, 0.005, 1), scratches=0.7, bump="hammer", bump_strength=0.2, mottle=0.8,
                          mottle_color=rgb(120, 60, 20), fine=0.6, seed=35, bevel=0.18)
    P["scale"] = K.surface("dragon_scale", rgb(120, 24, 30), 0.6, 0.36, wear=1.0, wear_color=rgb(240, 120, 70), wear_metal=0.8, dirt=0.9, scratches=0.8, bump="scales", bump_scale=1.0, bump_strength=0.4, mottle=1.0,
                           mottle_color=rgb(40, 8, 12), fine=0.6, seed=36, bevel=0.14)
    P["wrap"] = K.surface("wrap", rgb(36, 28, 32), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(8, 4, 6), groove_amount=0.9, dirt=0.8, mottle=0.7, mottle_color=rgb(20, 14, 16), wear=0.6, wear_color=rgb(110, 70, 60),
                          wear_metal=0.0, fine=0.6, seed=37, bevel=0.0)
    P["cord"] = K.surface("cord_red", rgb(170, 30, 28), 0.0, 0.5, bump="wrap", bump_strength=0.8, groove=rgb(50, 6, 6), groove_amount=0.8, wear=0.7, wear_color=rgb(210, 110, 80), wear_metal=0.0, dirt=0.7, seed=38, bevel=0.0, fine=0.5)
    P["soul"] = K.surface("soul", rgb(110, 190, 255), 0.0, 0.3, emit=rgb(90, 170, 255), emit_strength=0.9, emit_flicker=0.6, mottle=0.8, mottle_color=rgb(200, 235, 255), seed=39, bevel=0.0)
    P["ember"] = K.surface("orb", rgb(255, 120, 30), 0.0, 0.2, emit=rgb(255, 120, 30), emit_strength=1.8, emit_flicker=0.8, mottle=0.9, mottle_color=rgb(255, 220, 120), seed=40, bevel=0.0)
    P["edge"] = K.surface("talon_edge", rgb(200, 190, 190), 1.0, 0.25, wear=1.0, wear_color=rgb(255, 250, 240), dirt=0.6, scratches=0.9, bump="brushed", bump_strength=0.1, seed=41, bevel=0.1)
    return P


def bone_path(p0, p1, bow, n=5):
    """Quadratic-bowed points from p0 to p1 (bow = the middle pushed by that vector)."""
    a, b, c = V(p0), V(p1), V(bow)
    return [a.lerp(b, i / (n - 1)) + c * (4 * (i / (n - 1)) * (1 - i / (n - 1))) for i in range(n)]


def along(poly, s):
    t = s * (len(poly) - 1)
    i = min(int(t), len(poly) - 2)
    return V(poly[i]).lerp(V(poly[i + 1]), t - i)


def dragon_scythe(opts):
    K.begin(opts["samples"])
    P = dragon_palette()
    rr = K.rng(1313)
    # ---- the haft: a spine of vertebrae with black-iron collars and dorsal spines, a wrapped grip, a clawed butt round an ember orb
    prof = []
    y = 76.0
    for i in range(14):
        h = 9.0
        r = 2.25 + 0.25 * math.sin(i * 1.3)
        prof += [(r * 0.82, y), (r * 1.2, y + h * 0.2), (r * 0.95, y + h * 0.55), (r * 1.18, y + h * 0.85)]
        y += h
    shaft = K.bl.lathe("shaft", prof, P["bone_dark"], sides=8, part="shaft")
    K._finish_obj(shaft, "shaft", True, 50.0)
    for i in range(0, 14, 3):
        yy = 76.0 + i * 9.0
        c = K.bl.lathe("shaft_collar%d" % i, [(2.1, yy - 1.2), (3.1, yy - 0.9), (3.3, yy), (3.1, yy + 0.9), (2.1, yy + 1.2)], P["iron"], sides=8, part="collars")
        K._finish_obj(c, "collars", True, 50.0)
    K.spike_array("dorsal", [V((2.0, 82.0 + 12.0 * i, 0)) for i in range(10)], [V((1.0, 0.45, 0.05 * rr.uniform(-1, 1))) for i in range(10)], [4.5 + 2.5 * math.sin(i * 0.8) for i in range(10)], 1.0, P["bone_dark"], rr, bend=0.5, sides=4, part="dorsal")
    K.grip_wrapped("grip", 20.0, 72.0, 2.15, P["wrap"], P["cord"], turns=9.0, cord=0.62, bands=(21.5, 46.0, 70.0), band_mat=P["iron"], swell=0.1, part="grip", seed=5)
    K.blob("orb", (0, 6.0, 0), (3.4, 3.4, 3.4), P["ember"], seed=2, lump=0.08, rings=8, segs=10, part="pommel")
    for k in range(3):
        a = TAU * k / 3 + 0.5
        K.tapered("claw%d" % k, [V((2.8 * math.cos(a), 0.0, 2.8 * math.sin(a))), V((5.2 * math.cos(a), 3.0, 5.2 * math.sin(a))), V((4.3 * math.cos(a), 8.5, 4.3 * math.sin(a))), V((1.6 * math.cos(a), 10.5, 1.6 * math.sin(a)))], 1.1, 0.1, P["bone_dark"], sides=5, sub=2, part="pommel")
    for k in range(5):
        a = TAU * k / 5
        K.scale_plate("butt_scale%d" % k, (3.2 * math.cos(a), 13.4, 3.2 * math.sin(a)), (math.cos(a), -0.3, math.sin(a)), 1.9, P["scale"], up=(0, -1, 0), part="pommel", rows=3, tilt=20)
    # ---- socket: the dragon-collar that grips the wing: claws and rivets
    ycol = 206.0
    sock = K.bl.lathe("socket", [(2.2, ycol - 6.0), (3.6, ycol - 5.0), (4.4, ycol - 2.0), (4.2, ycol + 2.0), (3.6, ycol + 5.0), (2.6, ycol + 6.5)], P["iron"], sides=10, part="socket")
    K._finish_obj(sock, "socket", True, 45.0)
    K.spike_array("socket_claw", [V((3.4 * math.cos(a), ycol - 5.0, 3.4 * math.sin(a))) for a in [TAU * k / 6 for k in range(6)]],
                  [V((0.9 * math.cos(a), -0.6, 0.9 * math.sin(a))) for a in [TAU * k / 6 for k in range(6)]], 6.5, 0.9, P["gold"], rr, bend=0.6, part="socket")
    K.rivets("socket_rivets", [(V((4.35 * math.cos(a), ycol - 1.0 + 3.0 * (k % 2), 4.35 * math.sin(a))), (math.cos(a), 0, math.sin(a))) for k, a in enumerate([TAU * k / 8 for k in range(8)])], P["gold"], r=0.55, rng_=rr, detail=False, sides=5)
    # ---- the wing: a dragon wing is the blade. Its leading arm bone arches out over the haft (the scythe's curved back), the wrist sits at the apex, rib-fingers fan
    # down to the cutting edge, and the membrane between them has a scalloped trailing edge, torn and notched, edged with a talon-steel rim.
    O = [V(p) for p in K.bl.smooth_path([V((-1.0, ycol + 4.0, 0)), V((-26.0, 247.0, 0)), V((-66.0, 243.0, 0)), V((-96.0, 214.0, 0)), V((-109.0, 178.0, 0))], 8)]   # arm curve

    def O_at(s):
        return along(O, s)

    def inward(s):                                    # unit vector from the arm curve toward the cutting edge (down/in)
        t = (O_at(min(s + 0.01, 1.0)) - O_at(max(s - 0.01, 0.0))).normalized()
        n = V((-t.y, t.x, 0))
        return n.normalized()
    width = lambda s: 33.0 * math.sin(math.pi * min(max(s, 0.0), 1.0)) ** 0.85 + 0.6
    rib_s = [0.16, 0.31, 0.47, 0.64, 0.8]
    scal = 6.5
    n_rows = 34
    Lr, Rr = [], []
    for j in range(n_rows + 1):
        s = j / n_rows
        sc = min(abs(((s - rib_s[0] * 0.5) / ((rib_s[1] - rib_s[0]))) % 1.0 * 2.0 - 1.0), 1.0)      # scallop: 0 at a rib tip, 1 between
        bite = scal * (1.0 - sc) ** 1.6 * math.sin(math.pi * s) ** 0.6 * (1.0 + 0.5 * K.noise3((s * 9, 0, 0), 1.0, 5))
        w = width(s) - bite
        p = O_at(s)
        Lr.append(p)
        Rr.append(p + inward(s) * max(w, 0.4) + V((0, 0, 0)))
    # the arm bone, the rib fingers, claws
    K.tube("arm", [tuple(p) for p in O[::2]], 1.0, P["bone"], sides=8, sub=1, part="wing_bone", taper=(3.6, 1.9), bumps=0.12, seed=1)
    for i in range(1, 5):
        p = O[i * len(O) // 5]
        K.blob("knob%d" % i, tuple(p), (2.7, 2.7, 2.7), P["bone"], seed=i, lump=0.15, rings=5, segs=8, part="wing_bone")
    W = O_at(0.47) + V((0, 0.5, 0))
    for i, s in enumerate(rib_s):
        tip = O_at(s) + inward(s) * (width(s) + 1.5)
        pts = bone_path(W, tip, V((-5.0 + 8.0 * (s - 0.47), -3.0, 0)) * 1.0, 5)
        for zs in (1.3, -1.3):
            K.tube("rib%d_%d" % (i, zs > 0), [tuple(p + V((0, 0, zs))) for p in pts], 1.0, P["bone"], sides=5, sub=1, part="wing_bone", taper=(2.0, 0.6), bumps=0.14, seed=10 + i)
        K.tapered("rclaw%d" % i, [pts[-1], pts[-1] + inward(s) * 3.0 + V((-2.0, 0, 0.8)), pts[-1] + inward(s) * 8.0 + V((-3.5, 0, 0))], 0.8, 0.07, P["bone_dark"], sides=4, sub=1, part="wing_bone")
    K.strip("membrane", list(zip(Lr, Rr)), P["membrane"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.38, part="blade_membrane", up=(0, 0, 1), angle=60,
            wobble=lambda q, t: (0, 0, 0.9 * math.sin(math.pi * t) + 0.6 * K.noise3(q, 0.07, 3, 2)))
    K.tube("talon_edge", [tuple(q + V((0, 0, 0.4))) for q in Rr], 0.5, P["edge"], sides=4, sub=1, part="blade_edge")
    # leading-edge dorsal teeth (a ridge of dragon spines on the arm's outer curve), varied
    base_pts, dirs = [], []
    for j in range(3, n_rows - 2, 3):
        s = j / n_rows
        base_pts.append(O_at(s))
        dirs.append(-inward(s) + (O_at(min(s + 0.02, 1.0)) - O_at(s)).normalized() * 0.5)
    K.spike_array("tooth", base_pts, dirs, [5.5 + 3.5 * rr.random() for _ in base_pts], 1.1, P["bone_dark"], rr, bend=0.7, sides=4, part="teeth", curve=0.1)
    # a claw-guard bell under the head: scales, claws, and torn cloth tails
    bell = K.bl.lathe("bell", [(2.4, 72.0), (4.8, 73.0), (7.2, 76.0), (7.8, 80.0), (6.4, 84.0), (3.4, 86.0), (2.4, 87.0)], P["iron"], sides=12, part="socket_bell")
    K._finish_obj(bell, "socket_bell", True, 45.0)
    for k in range(6):
        a = TAU * k / 6 + 0.2
        K.scale_plate("bell_scale%d" % k, (6.0 * math.cos(a), 79.0, 6.0 * math.sin(a)), (math.cos(a), 0.15, math.sin(a)), 2.3, P["scale"], up=(0, 1, 0), part="socket_bell", rows=3, tilt=18)
    K.spike_array("bell_claw", [V((7.3 * math.cos(a), 76.0, 7.3 * math.sin(a))) for a in [TAU * k / 6 + 0.4 for k in range(6)]],
                  [V((0.9 * math.cos(a), -0.8, 0.9 * math.sin(a))) for a in [TAU * k / 6 + 0.4 for k in range(6)]], 7.0, 0.9, P["gold"], rr, bend=0.6, part="socket_bell")
    for i, (ang, ln) in enumerate(((20, 24), (150, 30), (260, 20))):
        a = math.radians(ang)
        path = [V((4.0 * math.cos(a) + 0.5 * k * math.cos(a), 74.0 - k * ln / 9.0, 4.0 * math.sin(a) + 0.5 * k * math.sin(a) + 0.8 * math.sin(k * 0.8 + i))) for k in range(10)]
        K.ribbon("tail%d" % i, path, lambda t: 2.4 * (1.0 - 0.45 * t) + 0.3, P["cord"], facing=(math.cos(a + 1.57), 0, math.sin(a + 1.57)), thick=0.06, part="socket_tails", fray=0.9, rng_=rr, edge_wave=0.1, notch=1.2 if i == 1 else 0.0)
    # the fourteen souls: a drifting ring of blue flames bound to the haft
    for i in range(14):
        yy = 88.0 + i * 8.0 + rr.uniform(-2, 2)
        a = i * 2.4 + rr.uniform(-0.3, 0.3)
        rd = 7.0 + 4.0 * rr.random() + 0.04 * (yy - 88.0)
        c = V((rd * math.cos(a), yy, rd * math.sin(a)))
        K.tapered("soul%d" % i, [c, c + V((0.4 * math.cos(a), 2.2, 0.4 * math.sin(a))), c + V((1.4 * math.cos(a + 1), 5.0, 1.4 * math.sin(a + 1))), c + V((2.0 * math.cos(a + 1.4), 8.0, 2.0 * math.sin(a + 1.4)))],
                  1.7, 0.05, P["soul"], sides=5, sub=1, part="souls", bulge=lambda t: 0.55 + 0.75 * math.sin(math.pi * (0.1 + 0.8 * t)))
    return K.quad_estimate()


# ================================================================ driver
BUILDERS = {}


def run(only, trial, beauty, quick, samples):
    entries = {}
    for key in only:
        fn, mesh_id, base, form, cls = BUILDERS[key]
        t0 = time.time()
        q = fn({"samples": samples})
        print("   parts:", K.quad_table())
        print("[%s] built geometry: ~%d body quads in %.1f s" % (mesh_id, q, time.time() - t0))
        if beauty:
            os.makedirs(beauty, exist_ok=True)
            for view in ("three_quarter", "front"):
                K.beauty(os.path.join(beauty, "%s_%s.png" % (key, view)), view=view, samples=24, size=(720, 1000))
        if not quick:
            K.finish_form(mesh_id, base, trial=trial)
            entries[mesh_id] = {"weapon": base, "form": form, "class": cls}
    if entries and not trial:
        K.write_manifest(MANIFEST, entries)


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--samples", type=int, default=24)
    a = ap.parse_args(argv)
    run(a.only.split(","), a.trial, a.beauty, a.quick, a.samples)


BUILDERS["phoenix"] = (phoenix_sword, "legendary_sword__phoenix_sword", "legendary_sword", "phoenix_sword", "longsword")
BUILDERS["slime"] = (slime_shield, "legendary_shield__slime_shield", "legendary_shield", "slime_shield", "shield")
BUILDERS["dragon"] = (dragon_scythe, "vassal_scythe__demon_dragon_scythe", "vassal_scythe", "demon_dragon_scythe", "scythe")

if __name__ == "__main__":
    main(sys.argv[1:])

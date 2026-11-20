"""Weapon-form models of the VASSAL WHIP (Raphtalia's... the whip of the Whip Hero), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_whip.py --only vine [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N] [--sheet]

mesh ids: vassal_whip__vine_whip, __storm_whip, __morning_star_whip, __hourglass_whip. Every form keeps the base whip's place line (class `coilwhip`, grip 0.212): the handle stands at
y -24 .. 40 in the hand, the lash coils up from the guard to y <= 198 (the base's own length: the class maximum). Silhouettes: vine = a double vine with thorns, leaves and flowers
and a grapple curl; storm = a braided copper cable in a lazy S with a lightning-rod tip and forked bolts; morning star = a heavy iron chain ending in a great spiked ball; hourglass =
a segmented brass lash carrying a dragon hourglass and ending in a gear-ringed gate. Sources: form names + docs/heroweapon/weapon_whip.md.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
import forms_v_common as C  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

BASE = "vassal_whip"
CX = 3.0


def handle(M, rr, *, grip_r=2.8, gold="gold", wrap="wrap", core="core", pommel_prof=None, guard_prof=None):
    """The shared handle: a braided grip y -8..32 between gold rings, a flared guard at y 34..42 and a pommel cap at the foot (y -24 .. -8)."""
    for o in K.grip_wrapped("grip", -8.0, 32.0, grip_r, M[core], M[wrap], turns=9.0, cord=0.62, bands=(-7.0, 8.0, 31.0), band_mat=M[gold], swell=0.1, part="grip", seed=3, sides=10):
        pass
    C.lathe_part("pommel", pommel_prof or [(0.3, -24.0), (3.0, -22.0), (4.8, -17.0), (4.2, -11.0), (3.0, -8.0)], M[gold], sides=12, part="pommel", angle=55.0)
    C.lathe_part("guard", guard_prof or [(2.6, 32.0), (5.2, 34.0), (7.4, 38.0), (6.0, 42.0), (3.0, 43.5)], M[gold], sides=14, part="guard", angle=50.0)


def coil_path(y0, y1, r0, r1, turns, *, n=72, wob=0.0, seed=0, cx=CX, tip_curl=0.0, squash=1.0):
    """Points of a coil from y0 to y1: radius r0 -> r1, `turns` turns; a tip curl bends the last stretch outward and back."""
    pts = []
    for i in range(n + 1):
        t = i / n
        a = TAU * turns * t + 0.6
        r = r0 + (r1 - r0) * t ** 0.8 + wob * math.sin(t * 17.0 + seed)
        y = y0 + (y1 - y0) * t
        p = V((cx + r * math.cos(a), y, r * math.sin(a) * squash))
        if tip_curl and t > 0.86:
            u = (t - 0.86) / 0.14
            p = p + V((tip_curl * math.sin(u * math.pi) * 0.5, -tip_curl * 0.5 * u * u, tip_curl * 0.6 * u))
        pts.append(p)
    return pts


# ================================================================ 1. VINE WHIP
def vine_whip(opts):
    C.begin(opts["samples"])
    rr = K.rng(801)
    M = {}
    M["core"] = C.leather("vi_core", rgb(86, 56, 30), seed=1)
    M["wrap"] = C.wrapmat("vi_wrap", rgb(60, 110, 44), seed=2)
    M["gold"] = C.metal("vi_gold", rgb(176, 140, 70), 0.34, seed=3, mottle_color=rgb(50, 90, 50))
    M["bark"] = K.surface("vi_bark", rgb(74, 100, 40), 0.0, 0.7, bump="grain", bump_scale=2.0, bump_strength=0.8, groove=rgb(26, 40, 14), groove_amount=0.8, mottle=1.0, mottle_color=rgb(40, 70, 22), wear=0.5, wear_color=rgb(150, 170, 90),
                          wear_metal=0.0, dirt=0.6, fine=0.7, seed=4, bevel=0.0)
    M["vine"] = K.surface("vi_vine", rgb(78, 160, 54), 0.0, 0.5, bump="grain", bump_scale=3.0, bump_strength=0.5, mottle=1.0, mottle_color=rgb(40, 100, 30), wear=0.5, wear_color=rgb(190, 230, 120), wear_metal=0.0, dirt=0.5,
                          seed=5, bevel=0.0, rim=(rgb(210, 255, 150), 0.3))
    M["leaf"] = K.surface("vi_leaf", rgb(60, 150, 50), 0.0, 0.5, bump="leather", bump_scale=1.6, bump_strength=0.3, veins=0.2, vein_color=rgb(190, 240, 110), vein_strength=0.0, mottle=0.9, mottle_color=rgb(28, 90, 28), seed=6, bevel=0.0)
    M["thorn"] = C.bonemat("vi_thorn", rgb(200, 170, 110), seed=7, mottle_color=rgb(90, 60, 30))
    M["petal"] = K.surface("vi_petal", rgb(236, 90, 140), 0.0, 0.45, mottle=1.0, mottle_color=rgb(255, 190, 210), bump="leather", bump_strength=0.2, emit=rgb(255, 120, 170), emit_strength=0.5, emit_flicker=0.5, seed=8, bevel=0.0,
                           tramp=None)
    M["core_g"] = C.glowmat("vi_core_g", rgb(255, 230, 110), 1.6, 0.4, seed=9)
    M["glow"] = C.glowmat("vi_glow", rgb(150, 255, 120), 1.2, 0.6, seed=10)
    handle(M, rr, gold="gold", wrap="wrap", core="core", pommel_prof=[(0.3, -24.0), (2.0, -22.5), (3.0, -20.0), (1.6, -15.0)], guard_prof=[(2.6, 32.0), (4.0, 34.0), (5.0, 38.0), (3.0, 41.0), (2.6, 42.0)])
    # the pommel is a flower BUD: layered petals closing over a glowing seed
    for k in range(7):
        a = TAU * k / 7
        L, R, Cc = K.leaf_rows(V((3.0 * math.cos(a), -9.5, 3.0 * math.sin(a))), V((0.55 * math.cos(a), -1.0, 0.55 * math.sin(a))), 17.0, 6.8, rows=6, facing=(math.cos(a + 1.57), 0, math.sin(a + 1.57)), curl=-2.0, bend=V((-0.5 * math.cos(a), 0, -0.5 * math.sin(a))))
        K.strip("bud%d" % k, list(zip(L, R)), M["petal"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.28, part="bud", up=(math.cos(a), 0, math.sin(a)), angle=60)
    K.blob("seed", (0.0, -18.0, 0.0), (2.4, 3.0, 2.4), M["core_g"], seed=2, lump=0.05, rings=6, segs=8, part="seed")
    # a guard of curled tendrils (spirals) and two leaves
    for sgn in (1, -1):
        for j in range(2):
            pts = [V((sgn * 4.0, 36.0 + j * 2.0, 1.0 - j * 2.0)), V((sgn * 9.0, 34.0 + j * 3.0, 2.0 - 4.0 * j)), V((sgn * 12.0, 40.0 + j * 3.0, 1.0 - 2.0 * j)), V((sgn * 9.0, 44.0 + j * 3.0, 0.0)), V((sgn * 6.5, 41.0 + j * 3.0, 0.0))]
            K.tapered("tendril%d_%d" % (sgn, j), pts, 0.9, 0.2, M["vine"], sides=5, sub=3, part="tendrils")
        L, R, Cc = K.leaf_rows(V((sgn * 4.0, 38.0, 1.0)), V((sgn * 0.8, 0.5, 0.3)), 15.0, 7.0, rows=6, facing=(0, 0, 1), curl=2.0, droop=1.0)
        K.strip("guard_leaf%d" % sgn, list(zip(L, R)), M["leaf"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.12, part="leaves", up=(0, 0, 1), angle=60)
    # ---- the LASH: two vines twisted round each other in a long loose coil, thorned, leafed and flowered, ending in a grapple curl
    P1 = coil_path(46.0, 192.0, 7.0, 15.0, 3.6, n=70, wob=0.8, seed=1, tip_curl=18.0)
    P2 = coil_path(46.0, 192.0, 7.0, 15.0, 3.6, n=70, wob=0.8, seed=2, tip_curl=18.0)
    P2 = [V((2 * CX - p.x, p.y, -p.z)) for p in P1]
    K.tube("vine_a", [tuple(p) for p in P1[::2]] + [tuple(P1[-1])], 1.0, M["vine"], sides=6, sub=1, part="vine", taper=(2.4, 0.5), bumps=0.12, seed=1)
    K.tube("vine_b", [tuple(p) for p in P2[::2]] + [tuple(P2[-1])], 1.0, M["bark"], sides=6, sub=1, part="vine", taper=(2.0, 0.45), bumps=0.14, seed=2)
    for P, nm in ((P1, "a"), (P2, "b")):
        for j, i in enumerate(range(6, 66, 6)):
            p = P[i]
            tg = (P[i + 1] - P[i - 1]).normalized()
            out = V((p.x - CX, 0.0, p.z)).normalized()
            side = tg.cross(out).normalized()
            if j % 2 == 0:
                K.tapered("thorn_%s%d" % (nm, j), [p, p + out * 1.8 + tg * 0.5, p + out * 4.2 + tg * 1.6], 0.75, 0.05, M["thorn"], sides=4, sub=1, part="thorns")
            else:
                L, R, Cc = K.leaf_rows(p, (out + side * 0.5 + tg * 0.4).normalized(), 9.0 - 3.0 * i / 66, 4.2, rows=5, facing=tg.cross(out), curl=1.0, droop=0.6, seed=i)
                K.strip("leaf_%s%d" % (nm, j), list(zip(L, R)), M["leaf"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.09, part="leaves", up=tg.cross(out), angle=60)
    for k, i in enumerate((18, 38, 58)):                                    # three flowers along the lash, and a bud at the tip
        p = P1[i]
        out = V((p.x - CX, 0.0, p.z)).normalized()
        for pet in range(5):
            a = TAU * pet / 5 + k
            d = (out * 0.6 + V((math.cos(a) * 0.5, 0.2, math.sin(a) * 0.5))).normalized()
            L, R, Cc = K.leaf_rows(p + out * 1.0, d, 5.0, 2.6, rows=4, facing=out, curl=0.8)
            K.strip("petal%d_%d" % (k, pet), list(zip(L, R)), M["petal"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.1, part="flowers", up=out, angle=60)
        K.blob("flower_core%d" % k, tuple(p + out * 1.6), (1.1, 1.1, 1.1), M["core_g"], seed=k, lump=0.04, rings=4, segs=6, part="seed")
    K.blob("tip_bud", tuple(P1[-1]), (1.6, 2.2, 1.6), M["petal"], seed=4, lump=0.05, rings=5, segs=7, part="flowers")
    K.shard_cloud("pollen", (CX, 120.0, 0), (22.0, 70.0, 12.0), 8, (0.5, 0.9), M["glow"], rr, avoid=lambda p: abs(p.x - CX) < 14.0 and False, part="pollen", stretch=1.4, sides=3, max_count=8)
    return K.quad_estimate()


# ================================================================ 2. STORM WHIP
def storm_whip(opts):
    C.begin(opts["samples"])
    rr = K.rng(802)
    M = {}
    M["core"] = C.leather("st_core", rgb(40, 40, 52), seed=11)
    M["wrap"] = C.wrapmat("st_wrap", rgb(150, 70, 36), seed=12)
    M["copper"] = C.metal("st_copper", rgb(196, 110, 62), 0.3, seed=13, mottle_color=rgb(46, 150, 130), mottle=1.0, wear_color=rgb(255, 190, 130))
    M["copper_d"] = C.metal("st_copper_d", rgb(122, 70, 44), 0.4, seed=14, mottle_color=rgb(40, 110, 100), mottle=1.0)
    M["rod"] = C.metal("st_rod", rgb(210, 214, 226), 0.2, wear=1.0, seed=15, bump="brushed", bump_strength=0.08, scratches=0.9)
    M["ame"] = C.glassy("st_ame", rgb(150, 70, 235), emit=rgb(170, 90, 255), strength=1.1, seed=16, mottle_color=rgb(240, 200, 255))
    M["bolt"] = C.glowmat("st_bolt", rgb(190, 160, 255), 2.2, 0.6, seed=17, mottle_color=rgb(255, 255, 255))
    M["cable"] = K.surface("st_cable", rgb(188, 104, 58), 1.0, 0.32, bump="wrap", bump_scale=2.4, bump_strength=0.9, groove=rgb(60, 28, 14), groove_amount=0.8, mottle=0.8, mottle_color=rgb(60, 140, 120), wear=0.8,
                           wear_color=rgb(255, 200, 140), dirt=0.6, scratches=0.5, seed=18, bevel=0.0)
    handle(M, rr, gold="copper", wrap="wrap", core="core", pommel_prof=[(0.3, -24.0), (3.4, -22.0), (5.2, -17.0), (4.6, -11.0), (3.0, -8.0)], guard_prof=[(2.6, 32.0), (4.4, 34.0), (6.0, 38.0), (4.6, 42.0), (3.0, 43.5)])
    K.blob("pommel_gem", (0.0, -17.0, 0.0), (3.4, 3.8, 3.4), M["ame"], seed=3, lump=0.03, rings=7, segs=10, part="gem")
    for k in range(4):                                                      # copper prongs round the pommel gem
        a = TAU * k / 4 + 0.4
        K.tapered("pommel_claw%d" % k, [V((3.2 * math.cos(a), -9.0, 3.2 * math.sin(a))), V((5.0 * math.cos(a), -16.0, 5.0 * math.sin(a))), V((2.6 * math.cos(a), -23.0, 2.6 * math.sin(a)))], 0.9, 0.3, M["copper"], sides=5, sub=2, part="pommel")
    # lightning rods on the guard, a coil round each, amethyst tips
    for sgn, (x, top) in ((1, (9.0, 66.0)), (-1, (-9.0, 60.0))):
        K.tapered("rod%d" % sgn, [V((x * 0.7, 40.0, 0.0)), V((x * 1.05, 52.0, 0.0)), V((x * 1.1, top, 0.0))], 0.9, 0.12, M["rod"], sides=6, sub=2, part="rods")
        K.spring("coil%d" % sgn, V((x * 0.82, 44.0, 0.0)), V((x * 1.0, 56.0, 0.0)), 1.9, 0.3, 5, M["copper"], sides=4, part="rods", sub=6)
        K.blob("rodtip%d" % sgn, (x * 1.1, top + 0.8, 0.0), (0.9, 1.3, 0.9), M["ame"], seed=sgn + 3, lump=0.03, rings=4, segs=6, part="gem")
    # ---- the LASH: a braided copper cable (three strands round a core) in a long lazy S, a rod at its tip with a forked bolt
    def s_path(y0, y1, n=64):
        pts = []
        for i in range(n + 1):
            t = i / n
            pts.append(V((CX + 15.0 * math.sin(t * math.pi * 2.6 + 0.4) * (0.55 + 0.6 * t) + 4.0 * t, y0 + (y1 - y0) * t, 5.0 * math.sin(t * math.pi * 3.7))))
        return pts
    P = s_path(46.0, 186.0)
    K.tube("cable_core", [tuple(p) for p in P[::2]] + [tuple(P[-1])], 1.0, M["copper_d"], sides=6, sub=1, part="cable", taper=(3.0, 1.6))
    n = len(P)
    for s in range(3):
        pts = []
        for i in range(n):
            t = i / (n - 1)
            tg = (P[min(i + 1, n - 1)] - P[max(i - 1, 0)]).normalized()
            nrm = tg.cross(V((0, 0, 1)))
            nrm = nrm.normalized() if nrm.length > 1e-3 else V((1, 0, 0))
            bi = tg.cross(nrm).normalized()
            a = TAU * (s / 3 + 7.0 * t)
            rr_ = 3.7 * (1.0 - 0.4 * t)
            pts.append(P[i] + nrm * (math.cos(a) * rr_) + bi * (math.sin(a) * rr_))
        K.tube("strand%d" % s, [tuple(p) for p in pts[::2]] + [tuple(pts[-1])], 1.0, M["cable"], sides=5, sub=1, part="cable", taper=(1.9, 1.0))
    for j, i in enumerate((12, 30, 48)):                                    # copper ferrules binding the cable
        c = P[i]
        tg = (P[i + 1] - P[i - 1]).normalized()
        o = K.bl.lathe("ferrule%d" % j, [(2.5, -1.0), (3.1, -0.6), (3.1, 0.6), (2.5, 1.0)], M["copper"], sides=8, axis="z", caps=(False, False), part="ferrules")
        K.xf(o, K.frame(c, tg, (0, 0, 1)))
        K._finish_obj(o, "ferrules", True, 60.0)
    tipc = P[-1]
    K.tapered("tip_rod", [tipc, tipc + V((0.5, 4.0, 0.0)), tipc + V((1.0, 10.0, 0.0))], 1.3, 0.1, M["rod"], sides=6, sub=1, part="tip")
    K.blob("tip_gem", tuple(tipc + V((1.0, 11.0, 0.0))), (1.2, 1.5, 1.2), M["ame"], seed=2, lump=0.03, rings=5, segs=7, part="gem")
    # bolts: a forked jump from the tip and arcs jumping off the cable (bake + glow)
    def bolt(name, a, b, segs=6, j=3.0, w=0.9):
        pts = [a]
        for i in range(1, segs):
            pts.append(a.lerp(b, i / segs) + V((rr.uniform(-1, 1), rr.uniform(-1, 1), rr.uniform(-1, 1))) * j)
        pts.append(b)
        K.tube(name, [tuple(p) for p in pts], w, M["bolt"], sides=4, sub=1, part="bolts", taper=(w, w * 0.4))
    t0 = tipc + V((1.0, 12.0, 0.0))
    bolt("fork_a", t0, t0 + V((-13.0, 13.0, 3.0)))
    bolt("fork_b", t0, t0 + V((12.0, 12.0, -3.0)))
    bolt("fork_c", t0 + V((-4.0, 4.0, 1.0)), t0 + V((-12.0, -1.0, -4.0)), segs=4)
    for i, (ia, off) in enumerate(((18, V((22.0, 8.0, 4.0))), (34, V((-24.0, 10.0, -3.0))), (52, V((21.0, 11.0, 4.0))), (26, V((-17.0, -10.0, 5.0))))):
        bolt("arc%d" % i, P[ia], P[ia] + off, segs=5, j=2.6, w=0.75)
    K.shard_cloud("spark", (CX, 120.0, 0), (24.0, 70.0, 10.0), 8, (0.5, 0.9), M["bolt"], rr, part="sparks", stretch=1.6, sides=3, max_count=8, avoid=lambda p: abs(p.x - CX) < 8.0 and False)
    return K.quad_estimate()


# ================================================================ 3. MORNING STAR WHIP
def morning_star_whip(opts):
    C.begin(opts["samples"])
    rr = K.rng(803)
    M = {}
    M["core"] = C.leather("ms_core", rgb(36, 30, 30), seed=21)
    M["wrap"] = C.wrapmat("ms_wrap", rgb(110, 30, 26), seed=22)
    M["iron"] = C.metal("ms_iron", rgb(86, 84, 92), 0.38, seed=23, bump_strength=0.3)
    M["iron_d"] = C.metal("ms_iron_d", rgb(44, 42, 48), 0.45, seed=24, bump_strength=0.3)
    M["steel"] = C.metal("ms_steel", rgb(196, 200, 212), 0.22, wear=1.0, seed=25, bump="brushed", bump_strength=0.1, scratches=0.9)
    M["brass"] = C.metal("ms_brass", rgb(190, 146, 66), 0.3, seed=26)
    M["ember"] = C.glowmat("ms_ember", rgb(255, 110, 40), 1.6, 0.8, seed=27, emit_cracks=0.0)
    M["gem"] = C.glassy("ms_gem", rgb(255, 80, 40), emit=rgb(255, 90, 40), strength=1.0, seed=28)
    handle(M, rr, gold="iron", wrap="wrap", core="core", pommel_prof=[(0.3, -24.0), (3.4, -22.0), (5.4, -17.0), (5.0, -11.0), (3.0, -8.0)], guard_prof=[(2.6, 32.0), (5.0, 34.0), (7.0, 37.0), (6.2, 41.0), (3.0, 43.0)])
    for k in range(6):                                                      # knuckle spikes on the guard, studs on the pommel
        a = TAU * k / 6
        K.tapered("guard_spike%d" % k, [V((6.0 * math.cos(a), 38.0, 6.0 * math.sin(a))), V((9.0 * math.cos(a), 40.0, 9.0 * math.sin(a))), V((13.0 * math.cos(a), 39.0, 13.0 * math.sin(a)))], 1.4, 0.1, M["steel"], sides=4, sub=1, part="guard_spikes")
    for k in range(5):
        a = TAU * k / 5 + 0.3
        K.tapered("pommel_spike%d" % k, [V((3.4 * math.cos(a), -16.0, 3.4 * math.sin(a))), V((5.6 * math.cos(a), -18.0, 5.6 * math.sin(a))), V((8.0 * math.cos(a), -20.5, 8.0 * math.sin(a)))], 1.0, 0.1, M["steel"], sides=4, sub=1, part="guard_spikes")
    K.blob("pommel_ember", (0.0, -22.5, 0.0), (1.8, 1.4, 1.8), M["gem"], seed=2, lump=0.04, rings=5, segs=7, part="gem")
    # ---- the CHAIN: heavy links in a big sagging arc, a swivel, and the GREAT STAR at its end
    path = [V((CX + 1.0, 44.0, 0.0)), V((CX + 14.0, 70.0, 6.0)), V((CX + 24.0, 104.0, -3.0)), V((CX + 12.0, 140.0, -6.0)), V((CX - 8.0, 164.0, 2.0)), V((CX - 6.0, 176.0, 3.0))]
    K.chain("chain", path, 11.0, 7.0, 1.5, M["iron"], max_links=17, part="chain", up=(0, 0, 1), sides=5, segs=8)
    K.chain("chain_core", [V((CX + 1.0, 44.0, 0.0)), V((CX + 8.0, 52.0, 2.0))], 6.0, 4.0, 1.0, M["iron_d"], max_links=2, part="chain", up=(1, 0, 0), sides=4, segs=6)
    sx, sy, sz_ = CX - 6.0, 183.0, 3.0
    K.blob("star_ball", (sx, sy, sz_), (11.0, 11.0, 11.0), M["iron_d"], seed=5, lump=0.06, rings=10, segs=14, part="star")
    for sgn in (1, -1):
        C.ring_torus("star_band%d" % sgn, V((sx, sy, sz_)), 11.0, 1.0, M["iron"], normal=(0, 1, 0.0) if sgn > 0 else (1, 0, 0.0), sides=5, segs=18, part="star")
    # 22 spikes spread over the ball (a fibonacci sphere), tipped bright
    nsp = 22
    for i in range(nsp):
        y = 1 - 2 * (i + 0.5) / nsp
        r_ = math.sqrt(max(0.0, 1 - y * y))
        a = i * 2.399963
        d = V((r_ * math.cos(a), y, r_ * math.sin(a)))
        base = V((sx, sy, sz_)) + d * 10.4
        K.tapered("spike%d" % i, [base, base + d * 4.0, base + d * 13.0], 2.4, 0.12, M["steel"], sides=4, sub=1, part="spikes")
    K.tapered("ring_eye", [V((sx + 1.0, sy - 7.0, sz_)), V((sx + 2.0, sy - 12.0, sz_)), V((sx - 1.0, sy - 14.0, sz_))], 1.2, 1.2, M["iron"], sides=5, sub=1, part="star")
    K.blob("star_glow", (sx, sy, sz_ + 8.6), (2.2, 2.2, 1.0), M["ember"], seed=3, lump=0.05, rings=4, segs=7, part="star_glow")
    for j in range(5):                                                      # embers in the chain links
        K.blob("ember%d" % j, tuple(path[1].lerp(path[4], 0.15 + 0.17 * j) + V((0.0, 0.0, 1.5))), (0.7, 0.7, 0.7), M["ember"], seed=j, lump=0.02, rings=4, segs=6, part="star_glow", detail=True)
    return K.quad_estimate()


# ================================================================ 4. HOURGLASS SAND WHIP
def hourglass_whip(opts):
    C.begin(opts["samples"])
    rr = K.rng(804)
    M = {}
    M["core"] = C.leather("hg_core", rgb(70, 46, 30), seed=31)
    M["wrap"] = C.wrapmat("hg_wrap", rgb(30, 70, 100), seed=32)
    M["brass"] = C.metal("hg_brass", rgb(200, 154, 70), 0.3, seed=33)
    M["brass_d"] = C.metal("hg_brass_d", rgb(130, 94, 44), 0.4, seed=34)
    M["glass"] = C.glassy("hg_glass", rgb(190, 225, 255), emit=rgb(190, 225, 255), strength=0.12, seed=35)
    M["sand"] = C.glowmat("hg_sand", rgb(255, 214, 110), 1.5, 0.5, seed=36, mottle_color=rgb(255, 245, 180))
    M["sand_dull"] = K.surface("hg_sand_d", rgb(226, 190, 110), 0.0, 0.8, bump="grain", bump_scale=4.0, bump_strength=0.6, mottle=1.0, mottle_color=rgb(160, 120, 60), emit=rgb(255, 214, 110), emit_strength=0.55, emit_flicker=0.5, seed=37, bevel=0.0)
    M["portal"] = C.glowmat("hg_portal", rgb(255, 190, 90), 1.7, 0.7, seed=38, mottle_color=rgb(255, 255, 200))
    M["ender"] = C.glassy("hg_ender", rgb(60, 220, 190), emit=rgb(70, 240, 200), strength=1.0, seed=39)
    M["dragon"] = C.bonemat("hg_dragon", rgb(120, 96, 60), seed=40, mottle_color=rgb(50, 36, 20))
    handle(M, rr, gold="brass", wrap="wrap", core="core", pommel_prof=[(0.3, -24.0), (3.0, -22.5), (5.6, -18.0), (5.6, -12.0), (3.4, -8.0)], guard_prof=[(2.6, 32.0), (5.0, 34.0), (7.0, 38.0), (5.6, 42.0), (3.0, 43.5)])
    # the pommel: a CLOCK FACE (a gear-toothed disc with hands and hour studs), ender pearl in the middle
    cg = K.gear("pommel_gear", (0.0, -15.0, 0.0), 8.4, 6.8, 14, 1.6, M["brass"], normal=(0, 0, 1), part="clock", rng_=rr, wear=0.06, bevel_w=0.0)
    cf = K.bl.lathe("clock_face", [(0.05, 1.7), (6.2, 1.5), (6.8, 1.1)], M["brass_d"], sides=20, axis="z", caps=(False, True), part="clock")
    K.xf(cf, K.Matrix.Translation(V((0.0, -15.0, 0.0))))
    K._finish_obj(cf, "clock", True, 50.0)
    for k in range(12):
        a = TAU * k / 12
        K.blob("hour%d" % k, (5.2 * math.cos(a), -15.0 + 5.2 * math.sin(a), 1.9), (0.5, 0.5, 0.4), M["brass"], seed=k, lump=0.0, rings=3, segs=5, part="clock", detail=True)
    K.bar("hand_a", (0.0, -15.0, 2.0), (3.6, -11.0, 2.0), 0.35, 0.25, M["brass"], part="clock", taper=0.4)
    K.bar("hand_b", (0.0, -15.0, 2.2), (-4.8, -17.0, 2.2), 0.3, 0.25, M["brass"], part="clock", taper=0.4)
    K.blob("clock_pearl", (0.0, -15.0, 2.4), (1.0, 1.0, 0.8), M["ender"], seed=2, lump=0.0, rings=4, segs=6, part="gem")
    # guard: a ring gate with three little gears
    C.ring_torus("guard_ring", V((0.0, 42.0, 0.0)), 9.0, 0.9, M["brass"], normal=(0, 0, 1), sides=6, segs=22, part="guard")
    for k in range(3):
        a = TAU * k / 3 + 0.5
        K.gear("guard_gear%d" % k, (9.0 * math.cos(a) + 0.0, 42.0 + 9.0 * math.sin(a), 0.0), 3.4, 2.5, 7, 0.7, M["brass_d"], normal=(0, 0, 1), part="guard", rng_=rr, wear=0.05, bevel_w=0.0)
    # ---- the LASH: a segmented brass cable coiling up, with sand-glass collars, flowing sand in strands
    P = coil_path(46.0, 198.0, 6.0, 13.0, 3.2, n=76, wob=0.4, seed=3)
    seg_n = len(P)
    K.tube("lash", [tuple(p) for p in P[::2]] + [tuple(P[-1])], 1.0, M["brass_d"], sides=6, sub=1, part="lash", taper=(1.6, 0.9), bumps=0.0)
    for j, i in enumerate(range(8, seg_n - 10, 9)):
        c = P[i]
        tg = (P[min(i + 1, seg_n - 1)] - P[i - 1]).normalized()
        o = K.bl.lathe("seg%d" % j, [(1.5, -1.6), (2.4, -1.2), (2.7, 0.0), (2.4, 1.2), (1.5, 1.6)], M["brass"], sides=8, axis="z", caps=(False, False), part="lash_rings")
        K.xf(o, K.frame(c, tg, (0, 0, 1)))
        K._finish_obj(o, "lash_rings", True, 60.0)
    # sand trickling off the lash in thin glowing threads
    for j, i in enumerate((14, 30, 44, 58)):
        top = P[i]
        K.tube("sand_thread%d" % j, [tuple(top), tuple(top + V((1.0, -4.0, 1.0))), tuple(top + V((1.6, -10.0, 0.5)))], 0.35, M["sand"], sides=4, sub=2, part="sand", taper=(0.45, 0.15))
    # ---- the DRAGON HOURGLASS riding the lash at mid-length: two glass bulbs full of glowing sand in a gold frame with four dragon claws
    hy, hr, hh = 118.0, 10.5, 38.0
    ctr = V((CX + 10.0, hy, 0.0))
    top_b = [(0.6, 0.0), (3.5, 3.0), (hr, 9.0), (hr * 0.95, 15.0), (6.0, 18.0), (1.6, 19.0)]
    for sgn in (1, -1):
        prof = [(a, ctr.y + sgn * b) for a, b in top_b]
        gb = K.bl.lathe("bulb%d" % sgn, [(a, h) for a, h in prof], M["glass"], sides=14, origin=(ctr.x, 0, 0), part="glass", caps=(True, True))
        K._finish_obj(gb, "glass", True, 50.0)
    K.blob("sand_low", (ctr.x, hy - 11.0, 0.0), (6.6, 7.0, 6.6), M["sand_dull"], seed=3, lump=0.05, rings=7, segs=10, part="sand_pile", cut=None)
    K.blob("sand_up", (ctr.x, hy + 13.0, 0.0), (6.0, 4.0, 6.0), M["sand_dull"], seed=4, lump=0.05, rings=6, segs=10, part="sand_pile")
    K.tube("sand_stream", [(ctr.x, hy + 8.0, 0.0), (ctr.x, hy, 0.0), (ctr.x, hy - 8.0, 0.0)], 0.45, M["sand"], sides=5, sub=2, part="sand")
    for sgn in (1, -1):
        C.lathe_part("cap%d" % sgn, [(2.0, sgn * 19.0 + hy - sgn * 0.0), (hr + 2.0, sgn * 20.0 + hy), (hr + 2.4, sgn * 22.0 + hy), (hr, sgn * 23.5 + hy), (3.0, sgn * 23.8 + hy)], M["brass"], sides=14, part="frame", origin=(ctr.x, 0, 0), angle=50.0)
    for k in range(4):
        a = TAU * k / 4 + math.pi / 4
        ca, sa = math.cos(a), math.sin(a)
        K.tapered("claw%d" % k, [V((ctr.x + (hr + 2.0) * ca, hy + 22.0, (hr + 2.0) * sa)), V((ctr.x + (hr + 5.0) * ca, hy + 12.0, (hr + 5.0) * sa)), V((ctr.x + (hr + 2.5) * ca, hy, (hr + 2.5) * sa)),
                                  V((ctr.x + (hr + 5.0) * ca, hy - 12.0, (hr + 5.0) * sa)), V((ctr.x + (hr + 2.0) * ca, hy - 22.0, (hr + 2.0) * sa))], 1.4, 1.4, M["dragon"], sides=6, sub=3, part="frame")
        K.tapered("clawtip%d" % k, [V((ctr.x + (hr + 2.0) * ca, hy + 22.0, (hr + 2.0) * sa)), V((ctr.x + (hr + 2.0) * ca * 0.6, hy + 26.0, (hr + 2.0) * sa * 0.6)), V((ctr.x + 2.0 * ca, hy + 27.0, 2.0 * sa))], 1.3, 0.1, M["brass"], sides=4, sub=1, part="frame")
    K.tube("link_in", [tuple(P[int(seg_n * 0.45)]), (ctr.x - 2.0, hy + 24.0, 0.0)], 0.9, M["brass_d"], sides=5, sub=1, part="lash")
    # ---- the GATE at the tip: a gear-ringed portal with a spiral of sand inside (the door in the lash)
    gx, gy = CX + 2.0, 184.0
    C.ring_torus("gate_ring", V((gx, gy, 0.0)), 13.0, 1.5, M["brass"], normal=(0, 0, 1), sides=6, segs=24, part="gate")
    C.ring_torus("gate_inner", V((gx, gy, 0.0)), 10.0, 0.6, M["portal"], normal=(0, 0, 1), sides=4, segs=28, part="gate_glow")
    for k in range(6):
        a = TAU * k / 6
        K.gear("gate_gear%d" % k, (gx + 13.0 * math.cos(a), gy + 13.0 * math.sin(a), 0.0), 3.6 if k % 2 else 2.8, 2.7 if k % 2 else 2.1, 8 if k % 2 else 6, 0.8, M["brass_d"] if k % 2 else M["brass"], normal=(0, 0, 1), part="gate", rng_=rr, wear=0.05, bevel_w=0.0)
    sp = K.spiral((gx, gy), 1.0, 9.0, 2.3, n=28)
    K.tube("gate_swirl", [(x, y, 0.0) for x, y in sp], 0.5, M["portal"], sides=4, sub=1, part="gate_glow", taper=(0.4, 0.75))
    K.tube("gate_join", [tuple(P[-1]), (gx - 6.0, gy - 8.0, 0.0)], 0.9, M["brass_d"], sides=5, sub=1, part="lash")
    K.shard_cloud("grain", (CX, 120.0, 0), (24.0, 70.0, 10.0), 8, (0.5, 0.9), M["sand"], rr, part="grains", stretch=1.4, sides=3, max_count=8, avoid=lambda p: False)
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["vine"] = (vine_whip, "vassal_whip__vine_whip", BASE, "vine_whip", "coilwhip")
BUILDERS["storm"] = (storm_whip, "vassal_whip__storm_whip", BASE, "storm_whip", "coilwhip")
BUILDERS["star"] = (morning_star_whip, "vassal_whip__morning_star_whip", BASE, "morning_star_whip", "coilwhip")
BUILDERS["hourglass"] = (hourglass_whip, "vassal_whip__hourglass_whip", BASE, "hourglass_whip", "coilwhip")

if __name__ == "__main__":
    C.run_family(sys.argv[1:], BUILDERS, "vassal_whip")

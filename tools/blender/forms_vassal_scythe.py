"""Weapon-form models of the VASSAL SCYTHE (L'Arc Berg's scythe), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_scythe.py --only small [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N] [--sheet]

mesh ids: vassal_scythe__small_scythe, __demon_elephant_scythe, __demon_dragon_scythe (REPLACES the exemplar membrane of forms_exemplars.py under the same id),
__scythe_zero. Every form keeps the base scythe's place line (same hand, size class `scythe`, grip 0.132): the shaft stands on the same axis, the blade bends to -x.
Sources: the form names + the Shield Hero lore of docs/heroweapon/weapon_scythe.md (the wikis cannot be opened from the build machine).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
import forms_v_common as C  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

BASE = "vassal_scythe"


# ================================================================ 1. SMALL SCYTHE: L'Arc's carry form, light, hinged, with a floating second scythe
def small_scythe(opts):
    C.begin(opts["samples"])
    rr = K.rng(501)
    M = {}
    M["steel"] = C.metal("sm_steel", rgb(168, 184, 200), 0.24, wear=1.0, seed=3, bump="brushed", bump_strength=0.1, scratches=0.9)
    M["steel_d"] = C.metal("sm_steel_d", rgb(70, 80, 94), 0.36, seed=4)
    M["brass"] = C.metal("sm_brass", rgb(196, 150, 70), 0.3, seed=5)
    M["wrap"] = C.wrapmat("sm_wrap", rgb(40, 96, 104), seed=6)
    M["leather"] = C.leather("sm_leather", rgb(110, 70, 40), seed=7)
    M["glow"] = C.glowmat("sm_glow", rgb(90, 235, 225), 1.5, 0.6, seed=8)
    M["gem"] = C.glassy("sm_gem", rgb(70, 220, 210), emit=rgb(80, 230, 220), strength=0.9, seed=9)
    # ---- haft: slim, with a leather-and-teal grip, a HINGE (the fold line) with a locking sleeve
    C.lathe_part("shaft_low", [(1.7, 4.0), (2.0, 20.0), (1.9, 120.0), (1.85, 138.0)], M["steel_d"], sides=10, part="shaft")
    C.lathe_part("shaft_up", [(1.85, 162.0), (1.8, 176.0), (2.0, 192.0)], M["steel_d"], sides=10, part="shaft")
    for o in K.grip_wrapped("grip", 12.0, 112.0, 2.15, M["leather"], M["wrap"], turns=13.0, cord=0.55, bands=(13.0, 40.0, 78.0, 111.0), band_mat=M["brass"], swell=0.06, part="grip", seed=5):
        pass
    # the hinge: two knuckle barrels, a pin with domed heads, a sprung locking sleeve and its catch
    for k, (yy, rx) in enumerate(((141.0, 2.6), (149.0, 2.6), (157.0, 2.6))):
        C.lathe_part("knuckle%d" % k, [(2.0, yy - 3.2), (rx + 0.4, yy - 2.8), (rx + 0.6, yy), (rx + 0.4, yy + 2.8), (2.0, yy + 3.2)], M["brass"], sides=10, part="hinge")
    K.bar("hinge_pin", (-4.6, 149.0, 0.0), (4.6, 149.0, 0.0), 0.7, 0.7, M["steel"], part="hinge", bevel_w=0.1)
    C.lathe_part("lock_sleeve", [(3.0, 123.0), (3.5, 124.0), (3.4, 136.0), (3.9, 137.5), (3.3, 139.0)], M["brass"], sides=12, part="hinge")
    K.spring("hinge_spring", V((0, 140.0, 3.6)), V((0, 156.0, 3.6)), 1.0, 0.22, 5, M["steel"], sides=4, part="hinge", sub=6)
    # butt: a short spike with a clip ring and a leather lanyard with a bead
    C.lathe_part("butt", [(0.2, 0.0), (1.1, 3.0), (2.4, 8.0), (2.0, 12.0)], M["steel_d"], sides=8, part="butt")
    C.ring_torus("lanyard_ring", V((0, 9.0, 0)), 3.0, 0.45, M["brass"], normal=(1, 0, 0), sides=5, segs=14, part="butt")
    path = [V((3.0 + 0.6 * math.sin(k * 0.9), 8.0 - k * 2.3, 0.7 * math.sin(k * 0.7))) for k in range(6)]
    K.ribbon("lanyard", path, lambda t: 1.6 * (1.0 - 0.45 * t) + 0.3, M["wrap"], facing=(0, 0, 1), thick=0.07, part="lanyard", fray=0.6, rng_=rr, notch=0.8)
    K.blob("lanyard_bead", (3.6, -2.0, 0.4), (0.9, 1.1, 0.9), M["gem"], seed=2, lump=0.05, rings=5, segs=7, part="lanyard")
    # ---- collar + gem + a short pick on the back
    C.lathe_part("collar", [(1.9, 176.0), (3.6, 178.0), (4.2, 184.0), (3.8, 192.0), (2.6, 198.0), (1.8, 200.0)], M["brass"], sides=12, part="collar")
    K.blob("collar_gem", (0.0, 187.0, 4.0), (2.2, 2.6, 1.6), M["gem"], seed=4, lump=0.05, rings=6, segs=8, part="gem")
    K.tapered("back_pick", [V((2.2, 194.0, 0.0)), V((8.0, 200.0, 0.2)), V((15.0, 198.0, 0.0)), V((19.0, 190.0, 0.0))], 1.5, 0.12, M["steel"], sides=4, sub=2, part="pick")
    # ---- the head: a compact hooked sickle, thick-backed, edge on the inside, a ridge and a row of small teeth on the back
    spine = [(1.5, 192.0), (-4.0, 214.0), (-20.0, 226.0), (-42.0, 224.0), (-60.0, 208.0), (-67.0, 186.0), (-63.0, 168.0)]
    wd = lambda t: 13.5 * math.sin(math.pi * min(1.0, t * 1.12 + 0.02)) ** 0.8 * (1.0 - 0.25 * t) + 0.6      # noqa: E731
    C.crescent("blade", M["steel"], spine, wd, thick=lambda t: 1.35 * (1.0 - 0.55 * t ** 1.5), side=1, sub=7)
    K.rivets("blade_rivets", [(V((x, y, 1.25)), (0, 0, 1)) for x, y in ((-6.0, 207.0), (-18.0, 218.0), (-30.0, 221.0))], M["brass"], r=0.7, rng_=rr, detail=True)
    sp = C.smooth2(spine, 7)
    teeth_b, teeth_d = [], []
    for i in range(6, len(sp) - 8, 5):
        n = C.normals2(sp, 1)[i]
        teeth_b.append(V((sp[i].x - n.x * 0.5, sp[i].y - n.y * 0.5, 0.0)))
        teeth_d.append(V((-n.x, -n.y, 0.0)) + C.tangent(sp, i / (len(sp) - 1)) * 0.2)
    K.spike_array("tooth", teeth_b, teeth_d, 5.0, 0.9, M["steel_d"], rr, bend=0.3, sides=4, part="teeth")
    # glowing rune line inlaid in the blade flat (bake + glow)
    for s in (1, -1):
        pts = [(p.x + C.normals2(sp, 1)[i].x * 5.0, p.y + C.normals2(sp, 1)[i].y * 5.0) for i, p in enumerate(sp) if 6 < i < len(sp) - 8 and i % 2 == 0]
        K.seam("rune_line%d" % s, pts, M["glow"], w=0.35, depth=0.1, z=1.3 * s, side=s, part="blade_glow", detail=True)
    # ---- the FLOATING second scythe: a smaller sickle hanging beside the haft in a thin orbit ring
    C.ring_torus("orbit_ring", V((0, 126.0, 0)), 24.0, 0.3, M["glow"], normal=(0, 1, 0.1), sides=4, segs=30, part="orbit")
    fs = [(0.0, 0.0), (-6.0, 8.0), (-18.0, 12.0), (-28.0, 5.0), (-31.0, -6.0)]
    fl = C.crescent("float_blade", M["steel"], fs, lambda t: 5.6 * math.sin(math.pi * min(1.0, t * 1.1 + 0.03)) ** 0.8 + 0.3, thick=lambda t: 0.8 * (1.0 - 0.5 * t), side=1, sub=5, part="float")
    K.xf(fl, K.Matrix.Translation(V((24.0, 134.0, 0.0))) @ K.Matrix.Scale(-1.0, 4, V((1, 0, 0))) @ K.Matrix.Rotation(math.radians(-14), 4, "Z"))
    K.tube("float_haft", [V((24.0, 134.0, 0.0)), V((24.6, 118.0, 0.0)), V((25.0, 96.0, 0.0))], 0.75, M["steel_d"], sides=6, sub=2, part="float")
    K.tube("float_grip", [V((24.8, 104.0, 0.0)), V((25.0, 92.0, 0.0))], 1.1, M["wrap"], sides=6, sub=1, part="float")
    K.blob("float_gem", (24.4, 132.0, 1.0), (1.2, 1.2, 1.2), M["gem"], seed=6, lump=0.05, rings=5, segs=7, part="gem")
    return K.quad_estimate()



def edge_line(name, P, N, off, mat, z, part, w=0.32, every=2, lo=0, hi=None):
    """A glowing inlay line (bake + glow) following a blade, on both faces; P/N = 3D points and 2D normals of the blade's centre line, off = offset toward the edge."""
    hi = hi or len(P)
    pts = [(P[i].x + N[i].x * off(i / (len(P) - 1)), P[i].y + N[i].y * off(i / (len(P) - 1))) for i in range(lo, hi, every)]
    for s_ in (1, -1):
        K.seam("%s%d" % (name, s_), pts, mat, w=w, depth=0.1, z=z * s_, side=s_, part=part, detail=True)


# ================================================================ 2. DEMON ELEPHANT SCYTHE: a tusk for a blade, a hide of armour, a foot for a butt
def elephant_scythe(opts):
    C.begin(opts["samples"])
    rr = K.rng(502)
    M = {}
    M["ivory"] = C.bonemat("el_ivory", rgb(236, 224, 192), seed=11, bump="rings", bump_scale=1.1, bump_strength=0.3, mottle_color=rgb(140, 100, 60))
    M["hide"] = C.leather("el_hide", rgb(118, 112, 106), seed=12, bump_scale=0.7, bump_strength=1.0, mottle_color=rgb(60, 52, 50))
    M["hide_d"] = C.leather("el_hide_d", rgb(64, 58, 56), seed=13, bump_scale=0.9)
    M["iron"] = C.metal("el_iron", rgb(78, 74, 80), 0.4, seed=14, bump_strength=0.28)
    M["edge"] = C.metal("el_edge", rgb(214, 220, 230), 0.2, wear=1.0, seed=15, bump="brushed", bump_strength=0.1, scratches=0.9)
    M["gold"] = C.metal("el_gold", rgb(210, 164, 74), 0.3, seed=16)
    M["wrap"] = C.wrapmat("el_wrap", rgb(92, 52, 34), seed=17)
    M["glow"] = C.glowmat("el_glow", rgb(255, 208, 112), 1.5, 0.5, seed=18)
    M["gem"] = C.glassy("el_gem", rgb(255, 190, 70), emit=rgb(255, 190, 70), strength=1.0, seed=19)
    # ---- haft: thick hide-clad shaft, a wrapped grip, iron bands with studs
    C.lathe_part("shaft", [(2.6, 8.0), (3.4, 22.0), (3.3, 150.0), (3.6, 178.0)], M["hide"], sides=12, part="shaft", angle=60.0)
    for o in K.grip_wrapped("grip", 14.0, 112.0, 3.45, M["hide_d"], M["wrap"], turns=11.0, cord=0.8, bands=(16.0, 62.0, 110.0), band_mat=M["iron"], swell=0.05, part="grip", seed=6, sides=12):
        pass
    for k, y in enumerate((130.0, 150.0, 168.0)):
        C.lathe_part("band%d" % k, [(3.3, y - 1.8), (4.4, y - 1.2), (4.5, y), (4.4, y + 1.2), (3.3, y + 1.8)], M["iron"], sides=12, part="bands")
    K.rivets("band_studs", [(V((4.55 * math.cos(a), y, 4.55 * math.sin(a))), (math.cos(a), 0, math.sin(a))) for y in (130.0, 150.0, 168.0) for a in [TAU * k / 8 + 0.2 * (y > 140) for k in range(8)]], M["gold"], r=0.55, rng_=rr, detail=True)
    # ---- butt: the elephant's FOOT: a round pad with toenails of ivory
    C.lathe_part("foot", [(2.2, 0.0), (6.4, 0.4), (7.6, 3.5), (6.6, 7.0), (3.8, 9.6), (3.0, 11.0)], M["hide"], sides=14, part="foot", angle=60.0)
    nb = [V((7.3 * math.cos(a), 2.2, 7.3 * math.sin(a))) for a in [TAU * k / 5 + 0.3 for k in range(5)]]
    K.spike_array("toenail", nb, [V((math.cos(a), -0.15, math.sin(a))) for a in [TAU * k / 5 + 0.3 for k in range(5)]], 3.6, 1.3, M["ivory"], rr, bend=0.2, sides=5, part="foot", len_jit=0.1)
    # ---- head: an armoured collar of layered plates, two ear-shields, eyes
    C.lathe_part("collar", [(3.5, 172.0), (6.4, 174.0), (7.4, 184.0), (6.6, 194.0), (4.6, 200.0), (3.0, 203.0)], M["iron"], sides=12, part="collar", angle=60.0)
    for ring_i, (yy, rad_, cnt) in enumerate(((176.5, 6.4, 8), (184.0, 7.5, 8), (191.5, 6.9, 7))):
        for k in range(cnt):
            a = TAU * (k + 0.5 * (ring_i % 2)) / cnt
            K.scale_plate("hide_plate%d_%d" % (ring_i, k), (rad_ * math.cos(a), yy + 3.4, rad_ * math.sin(a)), (math.cos(a), 0.2, math.sin(a)), 2.4, M["hide_d"], up=(0, 1, 0), part="plates", rows=3, tilt=22, thick=0.2)
    ear = [(x * 1.3, y * 1.3) for x, y in ((0, -14), (9, -10), (15, 0), (15, 11), (9, 19), (0, 21), (-8, 14), (-10, 3), (-6, -8))]
    for sz in (1, -1):
        for layer, (grow, dz) in enumerate(((1.0, 0.0), (0.74, 0.7))):
            pl = K.plate("ear%d_%d" % (sz, layer), [(x * grow, y * grow) for x, y in ear], 0.0, 0.9, M["hide"] if layer == 0 else M["iron"], part="ears" if layer == 0 else "ear_plates", bevel_w=0.25)
            K.xf(pl, K.Matrix.Translation(V((2.0, 188.0, sz * (6.0 + dz)))) @ K.Matrix.Rotation(math.radians(-sz * 22), 4, "Y"))
        K.rivets("ear_rivets%d" % sz, [(V((2.0 + x * 0.86, 188.0 + y * 0.86, sz * 9.2)), (0.0, 0.0, sz)) for x, y in ear[1:8]], M["gold"], r=0.6, rng_=rr, detail=True)
        K.blob("eye%d" % sz, (-3.0, 196.0, sz * 6.4), (1.4, 1.4, 1.1), M["gem"], seed=3 + sz, lump=0.04, rings=5, segs=7, part="gem")
    # ---- the TUSK blade: a thick ivory tusk curving to a point, a steel cutting edge inlaid on the inside, growth rings, iron clamps, glowing edge line
    sp = [(-3.0, 198.0), (-14.0, 226.0), (-44.0, 244.0), (-82.0, 230.0), (-103.0, 196.0), (-108.0, 158.0)]
    P = C.smooth2(sp, 6)
    N = C.normals2(P, 1)
    rad = lambda t: 5.8 - 5.3 * t ** 1.5       # noqa: E731
    K.tube("tusk", [tuple(p) for p in P[::2]] + [tuple(P[-1])], 1.0, M["ivory"], sides=10, sub=1, part="tusk", taper=(5.8, 0.5), bumps=0.05, seed=4)
    ed = [(p.x + n.x * rad(i / (len(P) - 1)) * 0.78, p.y + n.y * rad(i / (len(P) - 1)) * 0.78) for i, (p, n) in enumerate(zip(P, N))]
    C.crescent("tusk_edge", M["edge"], ed[2::2], lambda t: 1.5 + 1.1 * (1.0 - t), thick=lambda t: 0.9 * (1.0 - 0.6 * t), side=1, sub=1, part="tusk_edge", hs=(0.8, 1.0, 1.0, 0.8, 0.0))
    for i in (6, 16, 28):
        t = i / (len(P) - 1)
        c = P[i]
        tg = (P[i + 1] - P[i - 1]).normalized()
        o = K.bl.lathe("clamp%d" % i, [(rad(t) * 1.06, -1.2), (rad(t) * 1.28, -0.8), (rad(t) * 1.3, 0.0), (rad(t) * 1.28, 0.8), (rad(t) * 1.06, 1.2)], M["iron"], sides=10, axis="z", caps=(False, False), part="tusk_clamps")
        K.xf(o, K.frame(c, tg, (0, 0, 1)))
        K._finish_obj(o, "tusk_clamps", True, 60.0)
    edge_line("tusk_glow", P, N, lambda t: rad(t) * 0.8 + 0.2, M["glow"], 0.9, "tusk_edge", w=0.28, every=2, lo=3, hi=len(P) - 6)
    sp2 = C.smooth2([(3.0, 194.0), (15.0, 208.0), (24.0, 226.0), (22.0, 248.0)], 5)
    K.tube("tusk2", [tuple(p) for p in sp2[::2]] + [tuple(sp2[-1])], 1.0, M["ivory"], sides=9, sub=1, part="tusk", taper=(4.4, 0.4), bumps=0.05, seed=5)
    # hanging flaps of hide from the collar
    for i in range(3):
        a = math.radians(40 + 130 * i)
        path = [V((7.6 * math.cos(a) + 0.4 * k * math.cos(a), 176.0 - k * 3.2, 7.6 * math.sin(a) + 0.4 * k * math.sin(a))) for k in range(8)]
        K.ribbon("flap%d" % i, path, lambda t: 3.8 * (1.0 - 0.4 * t) + 0.4, M["hide_d"], facing=(math.cos(a + 1.57), 0, math.sin(a + 1.57)), thick=0.2, part="flaps", fray=0.6, rng_=rr, edge_wave=0.06, notch=0.8 if i == 1 else 0.0)
    return K.quad_estimate()


# ================================================================ 3. DEMON DRAGON SCYTHE (REDO): a dragon-bone blade, thick geometry, a dragon skull for a socket
def dragon_scythe(opts):
    C.begin(opts["samples"])
    rr = K.rng(1313)
    M = {}
    M["bone"] = C.bonemat("dr_bone", rgb(214, 196, 164), seed=31, bevel=0.14)
    M["bone_d"] = C.bonemat("dr_bone_d", rgb(88, 62, 48), seed=32, mottle_color=rgb(30, 20, 18))
    M["scale"] = K.surface("dr_scale", rgb(104, 22, 28), 0.5, 0.38, wear=1.0, wear_color=rgb(238, 118, 66), wear_metal=0.8, dirt=0.9, scratches=0.8, bump="scales", bump_scale=0.9, bump_strength=0.55,
                           mottle=1.0, mottle_color=rgb(34, 6, 10), fine=0.6, seed=36, bevel=0.14, veins=0.14, vein_color=rgb(255, 120, 22), vein_strength=0.9)
    M["iron"] = C.metal("dr_iron", rgb(48, 42, 50), 0.42, seed=34, bump_strength=0.25)
    M["gold"] = C.metal("dr_gold", rgb(206, 150, 56), 0.3, seed=35)
    M["wrap"] = C.wrapmat("dr_wrap", rgb(36, 28, 32), seed=37)
    M["cord"] = C.wrapmat("dr_cord", rgb(170, 30, 28), seed=38, bump="wrap")
    M["soul"] = C.glowmat("dr_soul", rgb(110, 190, 255), 1.1, 0.6, seed=39)
    M["ember"] = C.glowmat("dr_ember", rgb(255, 120, 30), 1.8, 0.8, seed=40)
    # ---- haft of vertebrae, a wrapped grip, a clawed butt round an ember orb
    C.vertebra_haft("spine", 76.0, 166.0, 2.3, M["bone_d"], M["iron"], seg=10.0, sides=8, part="shaft")
    for i in range(0, 9, 3):
        yy = 76.0 + i * 10.0
        C.lathe_part("collar%d" % i, [(2.1, yy - 1.2), (3.1, yy - 0.9), (3.3, yy), (3.1, yy + 0.9), (2.1, yy + 1.2)], M["iron"], sides=8, part="collars")
    K.spike_array("dorsal", [V((2.0, 84.0 + 12.0 * i, 0)) for i in range(7)], [V((1.0, 0.45, 0.05 * rr.uniform(-1, 1))) for i in range(7)], [4.5 + 2.5 * math.sin(i * 0.8) for i in range(7)], 1.0, M["bone_d"], rr, bend=0.5, sides=4, part="dorsal")
    for o in K.grip_wrapped("grip", 18.0, 72.0, 2.2, M["wrap"], M["cord"], turns=9.0, cord=0.62, bands=(19.0, 46.0, 70.0), band_mat=M["iron"], swell=0.1, part="grip", seed=5):
        pass
    K.blob("orb", (0, 8.0, 0), (3.4, 3.4, 3.4), M["ember"], seed=2, lump=0.08, rings=8, segs=10, part="pommel")
    for k in range(3):
        a = TAU * k / 3 + 0.5
        K.tapered("claw%d" % k, [V((2.6 * math.cos(a), 2.0, 2.6 * math.sin(a))), V((5.4 * math.cos(a), 5.0, 5.4 * math.sin(a))), V((4.4 * math.cos(a), 10.5, 4.4 * math.sin(a))), V((1.6 * math.cos(a), 14.0, 1.6 * math.sin(a)))], 1.2, 0.1, M["bone_d"], sides=5, sub=2, part="pommel")
    # ---- the DRAGON SKULL that holds the blade: a socket sleeve, cranium, snout, open lower jaw with teeth, two swept horns, glowing eyes
    C.lathe_part("socket", [(2.4, 164.0), (3.8, 166.0), (4.6, 172.0), (4.4, 180.0), (3.4, 186.0)], M["iron"], sides=10, part="socket")
    K.blob("cranium", (-1.0, 200.0, 0.0), (8.6, 7.4, 6.8), M["bone"], seed=7, lump=0.12, rings=9, segs=14, part="skull")
    K.tapered("snout", [V((4.0, 201.0, 0.0)), V((14.0, 203.0, 0.0)), V((25.0, 200.0, 0.0))], 5.6, 3.2, M["bone"], sides=8, sub=2, part="skull", bulge=lambda t: 1.0 + 0.2 * math.sin(math.pi * t))
    K.tapered("jaw", [V((0.0, 192.0, 0.0)), V((11.0, 190.0, 0.0)), V((22.0, 192.0, 0.0))], 3.6, 2.0, M["bone"], sides=8, sub=2, part="skull")
    K.spike_array("fang_up", [V((9.0 + 3.6 * i, 197.5, sz * 2.9)) for i in range(5) for sz in (1, -1)], [V((0.1, -1.0, 0.0))] * 10, 4.2, 0.62, M["bone"], rr, bend=0.15, sides=4, part="teeth", len_jit=0.3)
    K.spike_array("fang_dn", [V((7.0 + 3.6 * i, 194.5, sz * 2.4)) for i in range(4) for sz in (1, -1)], [V((0.1, 1.0, 0.0))] * 8, 3.4, 0.55, M["bone"], rr, bend=0.15, sides=4, part="teeth", len_jit=0.3)
    for sz in (1, -1):
        K.tapered("horn%d" % sz, [V((-3.0, 205.0, sz * 4.4)), V((-14.0, 215.0, sz * 7.0)), V((-27.0, 222.0, sz * 6.0)), V((-40.0, 220.0, sz * 4.0))], 2.4, 0.12, M["bone_d"], sides=6, sub=2, part="horns")
        K.blob("eye%d" % sz, (7.0, 205.0, sz * 5.6), (1.5, 1.2, 1.1), M["ember"], seed=3 + sz, lump=0.03, rings=5, segs=7, part="gem")
        K.tapered("brow%d" % sz, [V((3.0, 207.0, sz * 5.4)), V((8.0, 209.0, sz * 5.8)), V((12.0, 207.0, sz * 4.6))], 1.1, 0.4, M["bone_d"], sides=5, sub=1, part="skull")
    # ---- the BLADE: not a membrane: a thick scaled body with a bone arm for a back, rib ridges standing on both faces, a scalloped talon edge, magma in the grooves
    sp = [(-4.0, 204.0), (-12.0, 236.0), (-48.0, 254.0), (-88.0, 236.0), (-110.0, 198.0), (-108.0, 166.0)]
    P = C.smooth2(sp, 9)
    N = C.normals2(P, 1)
    n = len(P)
    bites = 5

    def wid(t):
        base = 28.0 * math.sin(math.pi * min(1.0, t * 1.04 + 0.01)) ** 0.8 + 1.2
        sc = 0.5 + 0.5 * math.cos(TAU * (bites * (t - 0.1)))
        bite = 11.0 * (sc ** 1.5) * math.sin(math.pi * t) ** 0.5 if t > 0.12 else 0.0
        return max(base - bite, 0.8)
    C.crescent("blade", M["scale"], sp, wid, thick=lambda t: 2.5 * (1.0 - 0.45 * t ** 1.4) + 0.2, side=1, sub=9, hs=(0.55, 1.0, 0.88, 0.45, 0.0),
               wobble=lambda q, t: (0, 0, 0.35 * K.noise3(q, 0.09, 6, 2)))
    arm = [tuple(p + V((-n_.x * 0.3, -n_.y * 0.3, 0.0))) for p, n_ in zip(P[::3], N[::3])] + [tuple(P[-1])]
    K.tube("arm", arm, 1.0, M["bone"], sides=9, sub=1, part="wing_bone", taper=(4.2, 1.1), bumps=0.1, seed=1)
    for i in range(6, n - 8, 9):
        K.blob("knob%d" % i, tuple(P[i] - N[i] * 0.4), (2.9, 2.9, 2.9), M["bone"], seed=i, lump=0.18, rings=5, segs=8, part="wing_bone")
    bd, bdir = [], []
    for i in range(10, n - 6, 8):
        bd.append(V(P[i] - N[i] * 0.9))
        bdir.append(V((-N[i].x, -N[i].y, 0.0)) + C.tangent(P, i / (n - 1)) * 0.35)
    K.spike_array("tooth", bd, bdir, [6.0 + 3.5 * rr.random() for _ in bd], 1.2, M["bone_d"], rr, bend=0.6, sides=4, part="teeth", curve=0.08)
    R0 = V(P[int(n * 0.1)]) + N[int(n * 0.1)] * 2.0
    for k, tk in enumerate((0.2, 0.4, 0.6, 0.8)):
        i = int(tk * (n - 1))
        tip = V(P[i]) + N[i] * (wid(tk) * 0.95 + 0.3)
        mid = R0.lerp(tip, 0.55) + C.tangent(P, tk) * (-3.0)
        for sz in (1, -1):
            pts = [tuple(R0 + V((0, 0, sz * 2.6))), tuple(mid + V((0, 0, sz * 2.9))), tuple(tip + V((0, 0, sz * 2.0)))]
            K.tube("rib%d_%d" % (k, sz), pts, 1.0, M["bone"], sides=5, sub=3, part="wing_bone", taper=(1.9, 0.5), bumps=0.12, seed=10 + k)
        K.tapered("talon%d" % k, [tip, tip + N[i] * 2.5 + C.tangent(P, tk) * 1.5, tip + N[i] * 7.0 + C.tangent(P, tk) * 4.2], 0.9, 0.06, M["bone_d"], sides=4, sub=1, part="teeth")
    edge_line("edge_glow", P, N, lambda t: wid(t) - 1.3, M["ember"], 1.9, "blade_glow", w=0.3, every=2, lo=8, hi=n - 6)
    # ---- cloth tails and the souls
    for i, (ang, ln) in enumerate(((20, 24), (150, 30), (260, 20))):
        a = math.radians(ang)
        path = [V((5.0 * math.cos(a) + 0.5 * k * math.cos(a), 168.0 - k * ln / 9.0, 5.0 * math.sin(a) + 0.5 * k * math.sin(a) + 0.8 * math.sin(k * 0.8 + i))) for k in range(10)]
        K.ribbon("tail%d" % i, path, lambda t: 2.4 * (1.0 - 0.45 * t) + 0.3, M["cord"], facing=(math.cos(a + 1.57), 0, math.sin(a + 1.57)), thick=0.08, part="tails", fray=0.9, rng_=rr, edge_wave=0.1, notch=1.2 if i == 1 else 0.0)
    for i in range(10):
        yy = 84.0 + i * 8.5 + rr.uniform(-2, 2)
        a = i * 2.4 + rr.uniform(-0.3, 0.3)
        rd = 7.0 + 4.0 * rr.random()
        c = V((rd * math.cos(a), yy, rd * math.sin(a)))
        K.tapered("soul%d" % i, [c, c + V((0.4 * math.cos(a), 2.2, 0.4 * math.sin(a))), c + V((1.4 * math.cos(a + 1), 5.0, 1.4 * math.sin(a + 1))), c + V((2.0 * math.cos(a + 1.4), 8.0, 2.0 * math.sin(a + 1.4)))],
                  1.7, 0.05, M["soul"], sides=5, sub=1, part="souls", bulge=lambda t: 0.55 + 0.75 * math.sin(math.pi * (0.1 + 0.8 * t)))
    return K.quad_estimate()


# ================================================================ 4. SCYTHE ZERO: a poison-steeped null scythe: a hollow ring of nothing in the hook, dripping toxin, a flask for a collar
def zero_scythe(opts):
    C.begin(opts["samples"])
    rr = K.rng(504)
    M = {}
    M["blade"] = K.surface("z_blade", rgb(150, 138, 168), 0.9, 0.3, wear=1.0, wear_color=rgb(236, 250, 210), wear_metal=1.0, dirt=1.0, dirt_color=(0.02, 0.05, 0.01, 1), scratches=0.8, bump="brushed", bump_strength=0.12,
                           mottle=1.0, mottle_color=rgb(46, 90, 40), fine=0.7, seed=51, bevel=0.14, veins=0.1, vein_color=rgb(150, 255, 80), vein_strength=0.5)
    M["dark"] = C.metal("z_dark", rgb(44, 36, 56), 0.4, seed=52, mottle_color=rgb(20, 40, 18))
    M["bone"] = C.bonemat("z_bone", rgb(206, 208, 186), seed=53, mottle_color=rgb(70, 90, 40))
    M["wrap"] = C.wrapmat("z_wrap", rgb(150, 154, 140), seed=54, mottle_color=rgb(60, 90, 40))
    M["glass"] = C.glassy("z_glass", rgb(150, 230, 120), emit=rgb(150, 255, 80), strength=0.6, seed=55)
    M["tox"] = C.glowmat("z_tox", rgb(150, 255, 80), 1.8, 0.7, seed=56)
    M["ring"] = C.glowmat("z_ring", rgb(200, 110, 255), 1.6, 0.5, seed=57, mottle_color=rgb(160, 255, 120))
    M["cork"] = C.leather("z_cork", rgb(120, 90, 60), seed=58)
    M["brass"] = C.metal("z_brass", rgb(150, 140, 80), 0.36, seed=59, mottle_color=rgb(40, 80, 30))
    # ---- haft: pale, bandaged, stained, with brass ferrules; a needle butt with a drop of poison
    C.lathe_part("shaft", [(1.6, 14.0), (2.1, 30.0), (2.0, 130.0), (2.2, 170.0), (2.4, 176.0)], M["dark"], sides=10, part="shaft")
    for o in K.grip_wrapped("grip", 20.0, 124.0, 2.3, M["dark"], M["wrap"], turns=15.0, cord=0.7, bands=(22.0, 54.0, 90.0, 122.0), band_mat=M["brass"], swell=0.08, part="grip", seed=7):
        pass
    for i in range(4):
        path = [V((2.6 + 0.4 * math.sin(k + i), 120.0 - 20.0 * i - k * 2.8, 2.0 * math.sin(k * 0.8 + i * 2))) for k in range(9)]
        K.ribbon("bandage%d" % i, path, lambda t: 1.8 * (1.0 - 0.5 * t) + 0.3, M["wrap"], facing=(0, 0, 1), thick=0.05, part="bandages", fray=0.5, rng_=rr, edge_wave=0.12)
    K.tapered("needle", [V((0, 14.0, 0)), V((0, 6.0, 0)), V((0, 0.0, 0))], 2.0, 0.12, M["dark"], sides=6, sub=1, part="butt")
    # ---- the flask collar: a big glass flask of toxin (the bake sees the liquid), a cork, a brass cage, bubbles
    fl = K.bl.lathe("flask", [(1.8, 176.0), (5.0, 178.0), (7.2, 184.0), (7.6, 190.0), (6.4, 196.0), (3.4, 200.0), (2.2, 203.0)], M["glass"], sides=14, part="flask", caps=(True, True))
    K._finish_obj(fl, "flask", True, 60.0)
    K.blob("flask_liquid", (0.0, 187.0, 0.0), (5.4, 6.2, 5.4), M["tox"], seed=8, lump=0.06, rings=7, segs=10, part="liquid", detail=True)
    for i in range(7):
        a, yy = rr.uniform(0, TAU), rr.uniform(182.0, 194.0)
        r_ = rr.uniform(0.5, 1.4)
        K.blob("bub%d" % i, (rr.uniform(1.0, 4.0) * math.cos(a), yy, rr.uniform(1.0, 4.0) * math.sin(a)), (r_, r_, r_), M["tox"], seed=i, lump=0.05, rings=4, segs=6, part="liquid", detail=True)
    C.lathe_part("cork", [(2.0, 202.0), (3.0, 203.0), (2.9, 208.0), (2.2, 209.0)], M["cork"], sides=10, part="cork")
    for k in range(6):
        a = TAU * k / 6 + 0.2
        K.tapered("cage%d" % k, [V((2.4 * math.cos(a), 178.0, 2.4 * math.sin(a))), V((7.8 * math.cos(a), 188.0, 7.8 * math.sin(a))), V((3.4 * math.cos(a), 200.0, 3.4 * math.sin(a)))], 0.5, 0.3, M["brass"], sides=4, sub=2, part="cage")
    # ---- the blade: a long thin crescent eaten by corrosion (bites out of the edge), poison running off it; the edge glows pale violet
    sp = [(-5.0, 202.0), (-14.0, 230.0), (-44.0, 246.0), (-82.0, 234.0), (-104.0, 204.0), (-108.0, 170.0)]
    P = C.smooth2(sp, 9)
    N = C.normals2(P, 1)
    n = len(P)

    def wid(t):
        base = 17.0 * math.sin(math.pi * min(1.0, t * 1.04 + 0.01)) ** 0.75 + 0.8
        bite = 0.0
        for (c, w_, d) in ((0.26, 0.05, 5.0), (0.42, 0.04, 4.0), (0.55, 0.06, 6.0), (0.7, 0.04, 4.5), (0.83, 0.04, 3.5)):
            u = abs(t - c) / w_
            if u < 1.0:
                bite = max(bite, d * (1.0 - u * u))
        return max(base - bite, 0.7)
    C.crescent("blade", M["blade"], sp, wid, thick=lambda t: 1.0 * (1.0 - 0.5 * t) + 0.15, side=1, sub=9, hs=(0.55, 1.0, 0.9, 0.45, 0.0),
               wobble=lambda q, t: (0, 0, 0.3 * K.noise3(q, 0.1, 9, 2)))
    K.tube("back", [tuple(p + V((-N[i].x * 0.5, -N[i].y * 0.5, 0))) for i, p in list(enumerate(P))[::3]] + [tuple(P[-1])], 1.0, M["dark"], sides=7, sub=1, part="back", taper=(2.6, 0.7), bumps=0.1, seed=2)
    idx = list(range(8, n - 6, 6))
    K.spike_array("barb", [V(P[i] - N[i] * 0.4) for i in idx], [V((-N[i].x, -N[i].y, 0)) + C.tangent(P, i / (n - 1)) * 0.5 for i in idx], [5.0 + 3.0 * rr.random() for _ in idx], 0.7, M["bone"], rr, bend=0.4, sides=4, part="barbs")
    edge_line("edge_glow", P, N, lambda t: wid(t) - 0.9, M["ring"], 1.2, "blade_glow", w=0.28, every=2, lo=8, hi=n - 6)
    for k, tk in enumerate((0.26, 0.42, 0.55, 0.7, 0.83)):                     # drips of toxin hang from the bitten edge
        i = int(tk * (n - 1))
        top = V(P[i]) + N[i] * (wid(tk) + 0.3)
        K.drip("drip%d" % k, (top.x, top.y, 0.0), rr.uniform(5.0, 9.0), rr.uniform(0.7, 1.1), M["tox"], seed=20 + k, sides=6, part="drips")
    # ---- the ZERO: a hollow ring of void hung in the hook, bound to the blade by three thin threads; the "0" of Scythe 0
    C.ring_torus("zero_ring", V((-48.0, 188.0, 0.0)), 17.0, 2.2, M["ring"], normal=(0, 0, 1), sides=6, segs=30, part="zero_ring", squash=1.2)
    C.ring_torus("zero_ring2", V((-48.0, 188.0, 0.0)), 17.0, 0.9, M["dark"], normal=(0, 0, 1), sides=5, segs=30, part="zero_ring_core", squash=2.4)
    for k, a in enumerate((math.radians(80), math.radians(150), math.radians(215))):
        K.tapered("thread%d" % k, [V((-48.0 + 17.0 * math.cos(a), 188.0 + 17.0 * math.sin(a), 0.0)), V((-48.0 + 24.0 * math.cos(a), 188.0 + 25.0 * math.sin(a), 0.0)), V(P[int(n * (0.34 + 0.16 * k))])], 0.4, 0.16, M["ring"], sides=4, sub=2, part="zero_ring_core")
    K.shard_cloud("mote", (-48.0, 188.0, 0.0), (15.0, 15.0, 3.0), 7, (0.5, 0.9), M["tox"], rr, avoid=lambda p: math.hypot(p.x + 48.0, p.y - 188.0) > 13.0, part="motes", stretch=1.4, sides=3, max_count=7)
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["small"] = (small_scythe, "vassal_scythe__small_scythe", BASE, "small_scythe", "scythe")
BUILDERS["elephant"] = (elephant_scythe, "vassal_scythe__demon_elephant_scythe", BASE, "demon_elephant_scythe", "scythe")
BUILDERS["dragon"] = (dragon_scythe, "vassal_scythe__demon_dragon_scythe", BASE, "demon_dragon_scythe", "scythe")
BUILDERS["zero"] = (zero_scythe, "vassal_scythe__scythe_zero", BASE, "scythe_zero", "scythe")

if __name__ == "__main__":
    C.run_family(sys.argv[1:], BUILDERS, "vassal_scythe")

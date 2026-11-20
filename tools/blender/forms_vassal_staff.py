"""Weapon-form models of the VASSAL STAFF (Rishia's staff), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_staff.py --only focus [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N] [--sheet]

mesh ids: vassal_staff__focus_staff, __rune_staff, __mirror_staff, __archmage_staff. Every form keeps the base staff's place line (class `staff`, grip 0.381): the shaft stands on the
same axis (butt at y -9, the hand at y ~110), the head sits at y 230..295 inside x +-36. Silhouettes: focus = an armillary of three gimbal rings round a double-terminated amethyst, a lens on
an arm; rune = a lantern of gold and stone holding a glowing prism, rune stones in orbit, raised rune plates on the shaft; mirror = a tall oval mirror in a pearl-beaded frame with a floating
second mirror; archmage = the seven stars of the Dipper in gold settings over a crystal spire, with deep-dark tendrils. Sources: form names + docs/heroweapon/weapon_staff.md.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
import forms_v_common as C  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

BASE = "vassal_staff"


def staff_shaft(M, rr, *, top=238.0, r=2.8, wrap_turns=11.0, cord=0.6, gold="gold", wrapm="wrap", ring_every=(63.0, 105.0, 147.0), butt_mat=None):
    """The shared lower staff: butt spike at y -9, a wrapped grip around the hand (y 60..150), a plain upper shaft to `top`."""
    C.lathe_part("shaft", [(1.0, -9.0), (1.9, -4.0), (2.1, 6.0), (r, 40.0), (r - 0.15, 120.0), (r, 200.0), (r + 0.2, top)], M["shaft"], sides=10, part="shaft")
    for o in K.grip_wrapped("grip", 60.0, 150.0, r + 0.2, M["shaft"], M[wrapm], turns=wrap_turns, cord=cord, bands=ring_every, band_mat=M[gold], swell=0.08, part="grip", seed=4):
        pass
    C.lathe_part("butt", [(0.2, -9.0), (1.5, -6.5), (2.7, -1.5), (3.0, 2.0), (2.2, 5.0)], M[butt_mat or gold], sides=10, part="butt")
    C.lathe_part("top_collar", [(r + 0.2, top - 6.0), (r + 1.8, top - 4.0), (r + 2.4, top - 1.0), (r + 1.4, top + 2.0), (r, top + 3.0)], M[gold], sides=12, part="collar")


def spark(name, p0, p1, mat, rr, *, segs=5, jitter=1.6, w=0.35, part="sparks"):
    """A lightning-bolt tube: a zig-zag between two points (a jump of charge)."""
    a, b = V(p0), V(p1)
    pts = [a]
    for i in range(1, segs):
        t = i / segs
        pts.append(a.lerp(b, t) + V((rr.uniform(-1, 1), rr.uniform(-1, 1), rr.uniform(-1, 1))) * jitter)
    pts.append(b)
    return K.tube(name, [tuple(p) for p in pts], w, mat, sides=4, sub=1, part=part, taper=(w, w * 0.4))


# ================================================================ 1. AMETHYST FOCUS STAFF
def focus_staff(opts):
    C.begin(opts["samples"])
    rr = K.rng(701)
    M = {}
    M["shaft"] = C.metal("fo_shaft", rgb(40, 38, 70), 0.4, seed=11, mottle_color=rgb(20, 20, 50))
    M["wrap"] = C.wrapmat("fo_wrap", rgb(34, 48, 110), seed=12)
    M["silver"] = C.metal("fo_silver", rgb(196, 202, 220), 0.22, wear=1.0, seed=13, bump="brushed", bump_strength=0.1, scratches=0.9)
    M["gold"] = C.metal("fo_gold", rgb(218, 176, 96), 0.28, seed=14)
    M["ame"] = C.glassy("fo_amethyst", rgb(150, 60, 220), emit=rgb(170, 80, 255), strength=0.9, seed=15, mottle_color=rgb(230, 170, 255))
    M["ame_d"] = C.glassy("fo_amethyst_d", rgb(110, 40, 180), emit=rgb(140, 60, 230), strength=0.6, seed=16)
    M["lapis"] = K.surface("fo_lapis", rgb(26, 52, 160), 0.2, 0.2, veins=0.25, vein_color=rgb(230, 200, 100), mottle=0.9, mottle_color=rgb(10, 20, 90), seed=17, bevel=0.05, wear=0.4, wear_color=rgb(120, 150, 230), wear_metal=0.0)
    M["spark"] = C.glowmat("fo_spark", rgb(190, 150, 255), 2.0, 0.7, seed=18, mottle_color=rgb(255, 255, 255))
    M["glass"] = C.glassy("fo_glass", rgb(190, 215, 255), emit=rgb(170, 200, 255), strength=0.15, seed=19)
    staff_shaft(M, rr, top=232.0)
    for k, y in enumerate((172.0, 190.0, 208.0)):                         # lapis studs set in gold bezels up the shaft
        for j in range(3):
            a = TAU * j / 3 + k * 0.6
            K.blob("lapis%d_%d" % (k, j), (2.7 * math.cos(a), y, 2.7 * math.sin(a)), (1.4, 1.4, 1.4), M["lapis"], seed=k * 3 + j, lump=0.04, rings=5, segs=7, part="lapis")
    # ---- a four-claw silver cradle from the collar holding the focus crystal
    for k in range(4):
        a = TAU * k / 4 + math.pi / 4
        ca, sa = math.cos(a), math.sin(a)
        K.tapered("claw%d" % k, [V((3.0 * ca, 236.0, 3.0 * sa)), V((9.0 * ca, 241.0, 9.0 * sa)), V((10.0 * ca, 252.0, 10.0 * sa)), V((6.0 * ca, 259.0, 6.0 * sa))], 1.5, 0.45, M["silver"], sides=5, sub=3, part="cradle")
    # ---- the focus: a double-terminated amethyst, a ring of satellite crystals and three tilted gimbal rings (an armillary of lenses)
    prof = [(0.05, -19.0), (4.4, -9.0), (6.6, -1.0), (6.6, 6.0), (4.6, 14.0), (0.05, 24.0)]
    cr = K.bl.lathe("focus_crystal", [(a, b + 252.0) for a, b in prof], M["ame"], sides=6, part="crystal")
    K._finish_obj(cr, "crystal", True, 30.0)
    for k in range(6):
        a = TAU * k / 6 + 0.3
        K.shard("sat%d" % k, (8.8 * math.cos(a), 248.0 + 6.0 * rr.random(), 8.8 * math.sin(a)), 2.3, M["ame_d"], rr, part="crystal", sides=5, stretch=2.4, dir_=V((math.cos(a) * 0.35, 1.0, math.sin(a) * 0.35)))
    for k, (R, tilt, roll, r_) in enumerate(((20.0, 0.0, 0.0, 0.9), (16.5, 62.0, 25.0, 0.8), (13.0, 118.0, 70.0, 0.7))):
        t_ = K.Matrix.Translation(V((0, 252.0, 0))) @ K.Matrix.Rotation(math.radians(roll), 4, "Y") @ K.Matrix.Rotation(math.radians(tilt), 4, "X")
        prof2 = [(R + r_ * math.cos(TAU * j / 6), r_ * math.sin(TAU * j / 6)) for j in range(6)]
        rings = []
        for i in range(30):
            a = TAU * i / 30
            rings.append([V((px * math.cos(a), px * math.sin(a), pz)) for px, pz in prof2])
        ring = K.bl.loft("gimbal%d" % k, rings, M["silver"] if k != 1 else M["gold"], (False, False), "gimbal")
        K.xf(ring, t_ @ K.Matrix.Rotation(math.radians(90), 4, "X"))
        K._finish_obj(ring, "gimbal", True, 60.0)
        for sgn in (1, -1):                                                 # a gold pivot bolt where each ring meets its axis
            p = t_ @ K.Matrix.Rotation(math.radians(90), 4, "X") @ V((sgn * R, 0, 0))
            K.blob("pivot%d_%d" % (k, sgn), tuple(p), (1.5, 1.5, 1.5), M["gold"], seed=k, lump=0.03, rings=5, segs=7, part="gimbal")
    # ---- a LENS on a swing arm (the analysis): a thick ring with a glass disc, hung on a gold arm from the shaft
    K.tube("lens_arm", [(2.6, 226.0, 0.0), (-8.0, 228.0, 1.0), (-18.0, 234.0, 1.0), (-24.0, 240.0, 0.0)], 0.9, M["gold"], sides=6, sub=3, part="lens")
    C.ring_torus("lens_ring", V((-30.0, 246.0, 0.0)), 8.5, 1.1, M["gold"], normal=(0, 0, 1), sides=6, segs=24, part="lens")
    ld = K.bl.lathe("lens_glass", [(0.05, 1.5), (4.6, 1.15), (8.0, 0.4), (8.4, 0.0), (8.0, -0.4), (4.6, -1.15), (0.05, -1.5)], M["glass"], sides=20, axis="z", caps=(False, False), part="lens_glass")
    K.xf(ld, K.Matrix.Translation(V((-30.0, 246.0, 0.0))))
    K._finish_obj(ld, "lens_glass", True, 50.0)
    K.tapered("lens_handle", [V((-30.0, 238.0, 0.0)), V((-27.0, 232.0, 0.0)), V((-24.5, 240.0 - 6.0, 0.0))], 0.8, 0.8, M["gold"], sides=5, sub=1, part="lens")
    # ---- sparks jumping from the rings to the shaft head and the lens (bake + glow)
    for i, (p0, p1) in enumerate((((13.0, 262.0, 1.0), (4.0, 238.0, 2.0)), ((-14.0, 268.0, 2.0), (-24.0, 252.0, 1.0)), ((20.0, 250.0, 0.0), (26.0, 236.0, 3.0)), ((-6.0, 275.0, 3.0), (6.0, 288.0, 2.0)))):
        spark("spark%d" % i, p0, p1, M["spark"], rr, jitter=1.4)
    K.shard_cloud("mote", (0, 262.0, 0), (34.0, 30.0, 8.0), 8, (0.6, 1.1), M["ame"], rr, avoid=lambda p: abs(p.x) < 22.0 and 228.0 < p.y < 285.0, part="motes", stretch=1.6, sides=3, max_count=8)
    C.scale_head(222.0, 1.38)
    return K.quad_estimate()


# ================================================================ 2. RUNIC STAFF
def rune_staff(opts):
    C.begin(opts["samples"])
    rr = K.rng(702)
    M = {}
    M["shaft"] = C.metal("ru_shaft", rgb(52, 40, 34), 0.5, seed=21, mottle_color=rgb(28, 20, 16), wear_color=rgb(150, 110, 70))
    M["wrap"] = C.wrapmat("ru_wrap", rgb(110, 56, 28), seed=22)
    M["gold"] = C.metal("ru_gold", rgb(212, 164, 78), 0.3, seed=23)
    M["stone"] = K.surface("ru_stone", rgb(96, 84, 78), 0.0, 0.7, bump="grain", bump_scale=1.2, bump_strength=0.5, mottle=1.0, mottle_color=rgb(46, 40, 38), wear=0.7, wear_color=rgb(170, 154, 140), wear_metal=0.0,
                           dirt=0.9, fine=0.8, seed=24, bevel=0.1)
    M["rune"] = C.glowmat("ru_rune", rgb(255, 170, 60), 1.8, 0.5, seed=25)
    M["prism"] = C.glassy("ru_prism", rgb(255, 214, 120), emit=rgb(255, 200, 90), strength=1.1, seed=26, mottle_color=rgb(255, 255, 220))
    M["crystal"] = C.glassy("ru_crystal", rgb(230, 90, 220), emit=rgb(240, 100, 230), strength=0.8, seed=27)
    M["chain"] = C.metal("ru_chain", rgb(150, 150, 160), 0.3, seed=28)
    staff_shaft(M, rr, top=216.0)
    # ---- raised rune plates round the shaft (real plates + baked glowing runes), a rune ring at the foot of the head
    for k in range(6):
        a = TAU * k / 6
        y = 160.0 + 9.0 * (k % 2)
        pl = K.plate("rune_plate%d" % k, [(-2.0, -5.5), (2.0, -5.5), (2.4, 5.5), (-2.4, 5.5)], 0.0, 0.8, M["stone"], part="rune_plates", bevel_w=0.15)
        K.xf(pl, K.frame(V((2.8 * math.cos(a), y, 2.8 * math.sin(a))), V((math.cos(a), 0, math.sin(a))), (0, 1, 0)))
        t = K.bl.text("rune_t%d" % k, K.RUNES[k * 3], 3.4, M["rune"], (0, 0, 0), extrude=0.06, font=K.FONT_RUNIC, detail=True, part="rune_plates")
        K.xf(t, K.frame(V((2.8 * math.cos(a), y, 2.8 * math.sin(a))) + V((math.cos(a), 0, math.sin(a))) * 0.85, V((math.cos(a), 0, math.sin(a))), (0, 1, 0)))
    # ---- the LANTERN head: a floor ring, four curved gold posts, a pointed roof, a hanging prism of glowstone, hanging rune tablets on short chains
    C.lathe_part("floor", [(3.0, 217.0), (11.0, 219.0), (13.0, 221.0), (12.0, 224.0), (4.0, 225.0)], M["gold"], sides=12, part="lantern")
    for k in range(4):
        a = TAU * k / 4 + math.pi / 4
        ca, sa = math.cos(a), math.sin(a)
        K.tapered("post%d" % k, [V((12.0 * ca, 222.0, 12.0 * sa)), V((16.0 * ca, 238.0, 16.0 * sa)), V((14.0 * ca, 258.0, 14.0 * sa)), V((6.0 * ca, 272.0, 6.0 * sa))], 1.5, 0.9, M["gold"], sides=6, sub=3, part="lantern")
    C.lathe_part("roof", [(1.4, 296.0), (3.0, 289.0), (7.5, 279.0), (10.0, 272.0), (8.0, 270.0), (3.0, 272.0)], M["gold"], sides=8, part="lantern", angle=35.0)
    for k in range(4):
        a = TAU * k / 4
        K.blob("roof_gem%d" % k, (8.0 * math.cos(a + 0.78), 273.5, 8.0 * math.sin(a + 0.78)), (1.3, 1.3, 1.3), M["crystal"], seed=k, lump=0.03, rings=5, segs=7, part="gem")
    K.blob("finial", (0, 297.0, 0), (1.4, 2.0, 1.4), M["crystal"], seed=3, lump=0.03, rings=5, segs=7, part="gem")
    pr = K.bl.lathe("prism", [(0.05, 236.0), (6.4, 244.0), (7.0, 252.0), (6.4, 260.0), (0.05, 268.0)], M["prism"], sides=6, part="prism")
    K._finish_obj(pr, "prism", True, 30.0)
    for k in range(3):
        a = TAU * k / 3 + 0.4
        K.tube("chain%d" % k, [(0.0, 270.0, 0.0), (3.5 * math.cos(a), 268.0, 3.5 * math.sin(a)), (6.0 * math.cos(a), 264.0, 6.0 * math.sin(a))], 0.35, M["chain"], sides=4, sub=2, part="chains")
    # hanging rune tablets under the floor, on chains
    for k in range(4):
        a = TAU * k / 4 + 0.2
        x, z = 9.0 * math.cos(a), 9.0 * math.sin(a)
        K.tube("tchain%d" % k, [(x, 219.0, z), (x * 1.1, 213.0, z * 1.1), (x * 1.2, 207.0, z * 1.2)], 0.35, M["chain"], sides=4, sub=1, part="chains")
        pl = K.plate("tablet%d" % k, [(-2.2, -3.6), (2.2, -3.6), (2.6, 3.6), (-2.6, 3.6)], 0.0, 1.0, M["stone"], part="tablets", bevel_w=0.15)
        K.xf(pl, K.frame(V((x * 1.2, 203.4, z * 1.2)), V((math.cos(a), 0, math.sin(a))), (0, 1, 0)))
        t = K.bl.text("tablet_rune%d" % k, K.RUNES[12 + k], 3.6, M["rune"], (0, 0, 0), extrude=0.06, font=K.FONT_RUNIC, detail=True, part="tablets")
        K.xf(t, K.frame(V((x * 1.2, 203.4, z * 1.2)) + V((math.cos(a), 0, math.sin(a))) * 1.05, V((math.cos(a), 0, math.sin(a))), (0, 1, 0)))
    # ---- floating rune stones in orbit (bounded: 7), an orbit ring of glow
    for k in range(7):
        a = TAU * k / 7 + 0.3
        rd = 26.0 + 5.0 * math.sin(k * 1.7)
        c = V((rd * math.cos(a) * 0.9, 250.0 + 14.0 * math.sin(k * 2.3), rd * math.sin(a) * 0.55 + (6.0 if k % 2 else -6.0)))
        sz = rr.uniform(2.6, 3.8)
        K.shard("orbit%d" % k, tuple(c), sz, M["stone"], rr, part="orbit_stones", sides=4, stretch=1.3, dir_=V((math.sin(a), 0.5, math.cos(a))))
        K.blob("orbit_core%d" % k, tuple(c), (sz * 0.55, sz * 0.55, sz * 0.55), M["rune"], seed=k, lump=0.03, rings=4, segs=6, part="orbit_glow")
    C.ring_torus("field_ring", V((0, 214.0, 0)), 20.0, 0.5, M["rune"], normal=(0, 1, 0), sides=4, segs=32, part="field")
    C.scale_head(205.0, 1.38)
    return K.quad_estimate()


# ================================================================ 3. MIRROR STAFF
def mirror_staff(opts):
    C.begin(opts["samples"])
    rr = K.rng(703)
    M = {}
    M["shaft"] = C.metal("mi_shaft", rgb(150, 156, 176), 0.3, seed=31, bump="brushed", bump_strength=0.1)
    M["wrap"] = C.wrapmat("mi_wrap", rgb(60, 70, 110), seed=32)
    M["silver"] = C.metal("mi_silver", rgb(214, 220, 232), 0.18, wear=1.0, seed=33, bump="brushed", bump_strength=0.08, scratches=0.8)
    M["gold"] = C.metal("mi_gold", rgb(206, 168, 100), 0.26, seed=34)
    M["mirror"] = K.surface("mi_mirror", rgb(236, 242, 255), 1.0, 0.04, wear=0.3, wear_color=rgb(255, 255, 255), scratches=0.5, mottle=0.5, mottle_color=rgb(170, 200, 240), seed=35, bevel=0.0, rim=(rgb(190, 230, 255), 0.3),
                           emit=rgb(120, 190, 255), emit_strength=0.12)
    M["pearl"] = C.glassy("mi_pearl", rgb(250, 244, 240), emit=rgb(255, 235, 245), strength=0.35, seed=36, transmission=0.15)
    M["quartz"] = C.glassy("mi_quartz", rgb(235, 240, 255), emit=rgb(190, 220, 255), strength=0.5, seed=37)
    M["glow"] = C.glowmat("mi_glow", rgb(170, 230, 255), 1.5, 0.4, seed=38, mottle_color=rgb(255, 255, 255))
    staff_shaft(M, rr, top=226.0, gold="gold")
    # the neck: a swan-neck of twisted silver rods carrying the mirror
    for k in range(2):
        a = k * math.pi
        pts = [V((2.4 * math.cos(a), 226.0, 2.4 * math.sin(a))), V((3.5 * math.cos(a + 1.0), 234.0, 3.5 * math.sin(a + 1.0)), ), V((3.0 * math.cos(a + 2.2), 242.0, 3.0 * math.sin(a + 2.2))), V((0.0, 252.0, 0.0))]
        K.tube("neck%d" % k, [tuple(p) for p in pts], 1.0, M["silver"], sides=6, sub=3, part="neck", taper=(1.3, 0.9))
    # ---- the great mirror: a tall oval frame (a gold bead-moulding), pearls round it, a domed mirror glass, scroll crown and a hanging drop
    cx, cy, a_, b_ = 0.0, 260.0, 17.5, 25.0
    ov = [(cx + a_ * math.cos(TAU * i / 40), cy + b_ * math.sin(TAU * i / 40)) for i in range(40)]
    for rr_, rad, mat, nm in ((1.0, 1.3, "gold", "frame"), (0.84, 0.65, "silver", "inner")):
        ring = [V((cx + a_ * rr_ * math.cos(TAU * i / 40), cy + b_ * rr_ * math.sin(TAU * i / 40), 0.0)) for i in range(41)]
        K.tube(nm, [tuple(p) for p in ring[:-1]] + [tuple(ring[0])], rad, M[mat], sides=7, sub=1, part="frame")
    for i in range(20):
        t = TAU * i / 20
        K.blob("pearl%d" % i, (cx + (a_ + 1.9) * math.cos(t), cy + (b_ + 1.9) * math.sin(t), 0.0), (1.15, 1.15, 1.15), M["pearl"], seed=i, lump=0.02, rings=5, segs=7, part="pearls")
    gl = K.bl.lathe("glass", [(0.05, 1.6), (6.0, 1.45), (11.0, 1.0), (14.0, 0.5), (14.4, 0.0), (14.0, -0.5), (11.0, -1.0), (6.0, -1.45), (0.05, -1.6)], M["mirror"], sides=28, axis="z", caps=(False, False), part="glass")
    K.xf(gl, K.Matrix.Translation(V((cx, cy, 0.0))) @ K.Matrix.Diagonal(V((1.15, 1.7, 1.0)).to_4d()))
    K._finish_obj(gl, "glass", True, 50.0)
    for sgn in (1, -1):                                                    # scroll crown + side volutes
        K.filigree("crown%d" % sgn, [(sgn * 2.0, 287.0), (sgn * 6.0, 293.0), (sgn * 12.0, 292.0), (sgn * 13.0, 286.0), (sgn * 9.0, 284.0)], M["gold"], w=0.9, h=0.7, z=0.0, side=1, sub=3, part="crown", taper=(1.0, 0.6))
        K.filigree("volute%d" % sgn, [(sgn * 20.0, 244.0), (sgn * 26.0, 238.0), (sgn * 30.0, 244.0), (sgn * 27.0, 250.0), (sgn * 23.0, 247.0)], M["gold"], w=0.9, h=0.7, z=0.0, side=1, sub=3, part="crown", taper=(1.0, 0.6))
    K.blob("crown_gem", (0.0, 289.0, 0.0), (2.0, 2.6, 1.6), M["quartz"], seed=3, lump=0.03, rings=6, segs=8, part="gem")
    K.shard("quartz_a", (-22.0, 262.0, 0.0), 3.4, M["quartz"], rr, part="quartz", sides=5, stretch=2.6, dir_=V((-0.5, 1.0, 0.0)))
    K.shard("quartz_b", (22.0, 274.0, 0.0), 3.0, M["quartz"], rr, part="quartz", sides=5, stretch=2.6, dir_=V((0.5, 1.0, 0.0)))
    # ---- the FLOAT MIRROR: a second, smaller oval mirror hanging apart in a thin ring of glow, three mirror shards drifting
    fx, fy, fa, fb = 26.0, 214.0, 8.5, 12.5
    for rr_, rad, mat, nm in ((1.0, 0.9, "gold", "fframe"),):
        ring = [V((fx + fa * rr_ * math.cos(TAU * i / 28), fy + fb * rr_ * math.sin(TAU * i / 28), 5.0)) for i in range(28)]
        K.tube(nm, [tuple(p) for p in ring] + [tuple(ring[0])], rad, M[mat], sides=6, sub=1, part="float_mirror")
    fg = K.bl.lathe("fglass", [(0.05, 1.0), (4.0, 0.85), (7.0, 0.3), (7.4, 0.0), (7.0, -0.3), (4.0, -0.85), (0.05, -1.0)], M["mirror"], sides=18, axis="z", caps=(False, False), part="float_mirror")
    K.xf(fg, K.Matrix.Translation(V((fx, fy, 5.0))) @ K.Matrix.Diagonal(V((1.15, 1.7, 1.0)).to_4d()))
    K._finish_obj(fg, "float_mirror", True, 50.0)
    C.ring_torus("float_halo", V((fx, fy, 5.0)), 15.0, 0.3, M["glow"], normal=(0, 0, 1), sides=4, segs=26, part="halo", squash=1.0)
    for i, (x, y, z, sz) in enumerate(((-30.0, 236.0, 6.0, 3.6), (-26.0, 286.0, -6.0, 2.8), (32.0, 292.0, 5.0, 3.0))):
        sh = K.plate("shard%d" % i, [(0, sz * 1.8), (sz * 0.8, 0.0), (0, -sz * 1.4), (-sz * 0.7, 0.2)], -0.35, 0.35, M["mirror"], part="shards", bevel_w=0.08)
        K.xf(sh, K.Matrix.Translation(V((x, y, z))) @ K.Matrix.Rotation(math.radians(20 + 35 * i), 4, "Z") @ K.Matrix.Rotation(math.radians(25), 4, "Y"))
    C.scale_head(215.0, 1.38)
    return K.quad_estimate()


# ================================================================ 4. SEVEN STAR ARCHMAGE STAFF
def archmage_staff(opts):
    C.begin(opts["samples"])
    rr = K.rng(704)
    M = {}
    M["shaft"] = C.metal("ar_shaft", rgb(30, 26, 52), 0.4, seed=41, mottle_color=rgb(14, 12, 34))
    M["wrap"] = C.wrapmat("ar_wrap", rgb(50, 36, 110), seed=42)
    M["gold"] = C.metal("ar_gold", rgb(226, 188, 100), 0.26, seed=43)
    M["gold_d"] = C.metal("ar_gold_d", rgb(150, 112, 54), 0.38, seed=44)
    M["crystal"] = C.glassy("ar_crystal", rgb(200, 120, 255), emit=rgb(190, 110, 255), strength=1.0, seed=45, mottle_color=rgb(255, 230, 255))
    M["star"] = C.glowmat("ar_star", rgb(255, 236, 160), 1.8, 0.4, seed=46, mottle_color=rgb(255, 255, 255))
    M["echo"] = C.glowmat("ar_echo", rgb(60, 220, 230), 1.7, 0.6, seed=47, mottle_color=rgb(170, 255, 255))
    M["sculk"] = K.surface("ar_sculk", rgb(14, 44, 54), 0.0, 0.5, bump="leather", bump_scale=1.2, bump_strength=0.5, veins=0.2, vein_color=rgb(60, 220, 230), vein_strength=0.8, mottle=0.8, mottle_color=rgb(6, 20, 28), seed=48, bevel=0.0)
    M["breath"] = C.glowmat("ar_breath", rgb(210, 90, 255), 1.5, 0.7, seed=49)
    staff_shaft(M, rr, top=232.0)
    for k in range(3):                                                      # magic-crystal inlay lines running up the shaft, glowing
        a = TAU * k / 3
        pts = [(2.5 * math.cos(a + 0.8 * t / 6), 160.0 + 12.0 * t) for t in range(0, 7)]
        K.tube("inlay%d" % k, [(2.65 * math.cos(a + 0.5 * t / 6), 162.0 + 11.0 * t, 2.65 * math.sin(a + 0.5 * t / 6)) for t in range(0, 7)], 0.3, M["crystal"], sides=4, sub=1, part="inlay")
    # ---- the central crystal spire (pure magic crystal), a cluster of echo shards at its base
    cr = K.bl.lathe("spire", [(0.05, 234.0), (4.4, 242.0), (6.0, 256.0), (5.0, 272.0), (2.8, 284.0), (0.05, 298.0)], M["crystal"], sides=6, part="spire")
    K._finish_obj(cr, "spire", True, 30.0)
    for k in range(5):
        a = TAU * k / 5 + 0.4
        K.shard("echo_shard%d" % k, (7.0 * math.cos(a), 244.0 + 4.0 * rr.random(), 7.0 * math.sin(a)), 2.6, M["echo"], rr, part="echo", sides=5, stretch=2.8, dir_=V((math.cos(a) * 0.6, 1.0, math.sin(a) * 0.6)))
    # ---- THE SEVEN STARS: the Dipper (bowl: Dubhe, Merak, Phecda, Megrez; handle: Alioth, Mizar, Alkaid), each an eight-point gold star with a glowing core, joined by thin arcs
    stars = [(28.0, 270.0), (31.0, 249.0), (14.0, 244.0), (11.0, 262.0), (-6.0, 271.0), (-20.0, 281.0), (-33.0, 270.0)]
    sizes = [5.0, 4.4, 4.6, 4.0, 5.2, 5.6, 6.2]
    for i, ((x, y), sz) in enumerate(zip(stars, sizes)):
        pts = []
        for j in range(16):
            a = TAU * j / 16 + math.pi / 2
            r = sz if j % 2 == 0 else sz * 0.42
            pts.append((x + r * math.cos(a), y + r * math.sin(a)))
        K.stepped_plate("star%d" % i, pts, -0.6, M["gold"], steps=2, step_h=0.6, inset=0.5, part="stars", up=1, bevel_w=0.0)
        K.stepped_plate("star%db" % i, pts, 0.6, M["gold_d"], steps=2, step_h=0.6, inset=0.5, part="stars", up=-1, bevel_w=0.0)
        K.blob("core%d" % i, (x, y, 0.0), (sz * 0.38, sz * 0.38, sz * 0.5), M["star"], seed=i, lump=0.03, rings=5, segs=8, part="cores")
    for i in range(6):
        a, b = stars[i], stars[i + 1] if i != 3 else stars[0]
        if i == 3:
            a, b = stars[3], stars[0]
        mid = ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2 + 2.0)
        K.tube("link%d" % i, [(a[0], a[1], 0.0), (mid[0], mid[1], 0.0), (b[0], b[1], 0.0)], 0.45, M["gold"], sides=5, sub=3, part="links")
    K.tube("link_join", [(stars[3][0], stars[3][1], 0.0), (stars[3][0] - 3.0, stars[3][1] + 1.0, 0.0), (stars[4][0], stars[4][1], 0.0)], 0.45, M["gold"], sides=5, sub=2, part="links")
    K.tube("hang_a", [(0.0, 252.0, 0.0), (6.0, 249.0, 0.0), (stars[3][0] - 1.0, stars[3][1] - 3.0, 0.0)], 0.7, M["gold"], sides=5, sub=3, part="links")
    C.ring_torus("halo", V((0, 262.0, -3.0)), 30.0, 0.35, M["star"], normal=(0, 0, 1), sides=4, segs=36, part="halo")
    # ---- deep-dark tendrils (sculk): curling shoots off the collar, echo-lit; and dragon's breath: purple wisps spiralling up the spire
    for k in range(5):
        a = TAU * k / 5 + 0.3
        pts = [V((3.0 * math.cos(a), 228.0, 3.0 * math.sin(a))), V((9.0 * math.cos(a), 222.0, 9.0 * math.sin(a))), V((15.0 * math.cos(a + 0.4), 226.0, 15.0 * math.sin(a + 0.4))), V((17.0 * math.cos(a + 0.9), 236.0, 17.0 * math.sin(a + 0.9)))]
        K.tapered("tendril%d" % k, pts, 1.3, 0.2, M["sculk"], sides=5, sub=3, part="sculk")
        K.blob("tbulb%d" % k, tuple(pts[-1]), (1.0, 1.0, 1.0), M["echo"], seed=k, lump=0.03, rings=4, segs=6, part="echo")
    for k in range(3):
        a0 = TAU * k / 3
        pts = [V((7.0 * math.cos(a0 + t * 0.9), 240.0 + t * 8.0, 7.0 * math.sin(a0 + t * 0.9))) for t in range(0, 6)]
        K.tapered("breath%d" % k, pts, 0.9, 0.12, M["breath"], sides=4, sub=2, part="breath")
    K.shard_cloud("mote", (0, 262.0, 0), (36.0, 32.0, 6.0), 8, (0.6, 1.1), M["star"], rr, avoid=lambda p: abs(p.x) < 36.0 and abs(p.y - 262.0) < 14.0 and False, part="motes", stretch=1.6, sides=3, max_count=8)
    C.scale_head(222.0, 1.38)
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["focus"] = (focus_staff, "vassal_staff__focus_staff", BASE, "focus_staff", "staff")
BUILDERS["rune"] = (rune_staff, "vassal_staff__rune_staff", BASE, "rune_staff", "staff")
BUILDERS["mirror"] = (mirror_staff, "vassal_staff__mirror_staff", BASE, "mirror_staff", "staff")
BUILDERS["archmage"] = (archmage_staff, "vassal_staff__archmage_staff", BASE, "archmage_staff", "staff")

if __name__ == "__main__":
    C.run_family(sys.argv[1:], BUILDERS, "vassal_staff")

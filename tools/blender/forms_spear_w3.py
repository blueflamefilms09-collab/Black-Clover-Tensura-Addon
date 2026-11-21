"""Style wave 3 of the LEGENDARY SPEAR family (docs/heroweapon/style/wave3.md): the base spear, its curse look (Lust), the Portal spear and the Dragon Minute Hand.
REPLACEMENTS: the previous meshes and skins are kept in weapon_meshes/classic_style3/ and textures/entity/weapon/classic_style3/ (client option vassalForms.classicStyleWave3).

    python3 -B tools/blender/forms_spear_w3.py [--only base,curse,portal,dragon] [--trial DIR] [--quick] [--shape DIR] [--samples N]

Every spear keeps the length (y 0 .. ~335), the place line and the hand wrap (y 98..126) of the family; the ten fused ornament rows are gone.
  legendary_spear                    bare ash shaft with ONE hide binding, a plain iron socket with one small red stone, a long leaf head with a fuller and TWO hooked arms of unequal length
  legendary_spear_curse              the same builder, black iron: a thorned vine up the shaft (a magenta line in it), a third small barb on the long arm, the stone turned into an eye
  legendary_spear__portal_spear      an obsidian ring BROKEN open with uneven thickness round a void funnel (three violet arms), one pearl in the socket, a slim dark blade; no gold, no cross ring
  legendary_spear__dragon_minute_hand  one long clock-hand needle (the hero), a dragon-scale socket with one long and one broken horn, Fitoria's feather as the one-off; no scale rows, no studs
Palette per weapon: 3 base materials + ONE named accent (wave2_kit.Palette). Seeded.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import wave3_kit as W3  # noqa: E402
import forms_legendary_spear as FS  # noqa: E402
import forms_kit as K  # noqa: E402
import wave2_kit as W  # noqa: E402
from forms_kit import V, TAU  # noqa: E402
from bl_lib import rgb  # noqa: E402

bl = K.bl
BASE = "legendary_spear"


def arm(name, pts, w, t, mat, part="head_arms"):
    """A swept, hooked arm: a lozenge section that thins to a needle tip along the smoothed path (the base spear's wings, with a hook at the end)."""
    path = bl.smooth_path([(x, y, 0.0) for x, y in pts], 3)
    ob = bl.sweep(name, path, bl.lozenge(1.0, 1.0, flat=0.35), mat, scale=lambda u: (w * (1 - u) ** 0.75 + 0.1, t * (1 - u) ** 0.6 + 0.08), part=part)
    return K._finish_obj(ob, part, True, 40.0)


# ================================================================ 1/2. LEGENDARY SPEAR and its curse look
def spear_palette(curse):
    if not curse:
        P = W.Palette("gem", ["steel", "wood", "iron"], ["hide"])
        P["steel"] = W.steel("ls_steel", rgb(204, 212, 226), seed=611, rough=0.22, bevel=0.2, ao_dist=2.5, mottle_color=rgb(140, 156, 186), scratches=0.7)
        P["wood"] = W.wood("ls_ash", rgb(176, 138, 92), seed=612)
        P["iron"] = W.iron("ls_iron", rgb(64, 68, 80), seed=613, mottle_color=rgb(88, 70, 62))
        P["hide"] = W.hide("ls_hide", rgb(110, 40, 36), seed=614)
        P["gem"] = W.accent_gem("ls_gem", rgb(214, 28, 38), rgb(255, 70, 60), 0.75, seed=615)
    else:
        P = W.Palette("sin", ["steel", "wood", "iron"], ["hide"])
        P["steel"] = W.steel("lc_steel", rgb(76, 70, 96), seed=621, rough=0.34, bevel=0.2, ao_dist=2.5, mottle_color=rgb(34, 28, 46), scratches=0.8, wear_color=rgb(150, 130, 176))
        P["wood"] = W.wood("lc_wood", rgb(58, 44, 52), seed=622, groove=rgb(20, 12, 18))
        P["iron"] = W.iron("lc_iron", rgb(34, 30, 42), seed=623, mottle_color=rgb(60, 40, 64))
        P["hide"] = W.hide("lc_hide", rgb(42, 28, 48), seed=624)
        P["sin"] = W.accent_gem("lc_sin", rgb(220, 40, 170), rgb(255, 70, 210), 0.9, seed=625)
    return P.check()


def legendary_spear(opts, curse=False):
    FS.begin(opts["samples"])
    P = spear_palette(curse)
    acc = P["sin"] if curse else P["gem"]
    rr = K.rng(8123 if curse else 8121)
    # shaft: bare wood in four short lengths, the hide-wrapped grip (y 98..126), ONE binding and nothing else
    FS.plain_shaft(P["wood"], 18.0, 218.0, 2.35, sides=10, taper=0.04, swell=0.035)
    K.grip_wrapped("grip", 98.0, 126.0, 2.55, P["hide"], P["hide"], turns=6.0, cord=0.46, bands=(), swell=0.08, part="grip", seed=3, sides=10)
    FS.lathe("butt_cap", [(0.01, 0.0), (1.4, 1.2), (2.6, 4.6), (2.9, 9.0), (2.5, 13.5), (2.4, 17.0)], P["iron"], "butt_cap", 10, 40.0)
    # socket: a plain iron collar that swells unevenly and holds one small stone
    FS.lathe("socket_collar", [(2.3, 214.0), (3.3, 217.0), (3.8, 224.0), (3.2, 231.0), (3.6, 238.0), (3.0, 245.0), (2.3, 252.0)], P["iron"], "socket_collar", 10, 36.0)
    W.jewel("socket_gem", (0.0, 228.0, 3.45), 2.0 if not curse else 2.3, acc, P["iron"], mount="bezel", axis="z", part="socket_gem")
    # the head: a long leaf blade with a fuller (a channel each face), one chip on each edge at different heights
    st = [(250, 3.0), (256, 6.2), (264, 9.4), (276, 11.0), (290, 9.6), (304, 6.8), (316, 4.0), (325, 1.9), (332, 0.3)]
    nl = [(286, 0.9, 2.8, "chip")] + ([(301, 0.8, 2.2, "step")] if curse else [])
    nr = [(272, 0.7, 2.2, "chip"), (309, 0.8, 2.6, "step")]
    FS.jag_blade("head_blade", P["steel"], 250.0, 332.0, st, nl, nr, 31 if not curse else 33, (-0.3, 332.0), (0.4, 331.0), "head_blade", thick=lambda t: 1.9 * (1.0 - 0.5 * t ** 1.5) + 0.1,
                 angle=40.0, wob=0.0, jitter=0.05, res=5.0, sec=((-1.0, -0.86, -0.5, -0.3, 0.3, 0.5, 0.86, 1.0), (0.0, 0.7, 1.0, 0.5, 0.5, 1.0, 0.7, 0.0)))
    # two hooked arms below the head: a long one on the left, a short one on the right (graded, not a mirror)
    arm("arm_long", [(-2.6, 247.0), (-9.0, 240.0), (-17.0, 241.0), (-22.5, 249.0), (-22.0, 259.0), (-17.5, 264.5), (-13.0, 262.0)], 3.0, 1.1, P["steel"])
    arm("arm_short", [(2.6, 247.0), (8.0, 243.0), (14.0, 245.0), (16.5, 251.5), (14.0, 257.0), (10.5, 258.5)], 2.3, 0.9, P["steel"])
    if curse:
        arm("arm_barb", [(-9.5, 243.0), (-12.5, 250.0), (-11.0, 256.0), (-8.0, 257.0)], 1.5, 0.7, P["steel"])
        # a thorned vine winds up the shaft to the socket, a magenta line in it
        turns, y0, y1 = 3.2, 146.0, 232.0
        helix = lambda u, r: V((r * math.cos(TAU * turns * u), y0 + (y1 - y0) * u, r * math.sin(TAU * turns * u)))     # noqa: E731
        K.tube("shaft_vine", [helix(i / 26.0, 3.1) for i in range(27)], 0.62, P["iron"], sides=4, sub=1, part="shaft_vine", taper=(0.5, 0.75))
        K.tube("shaft_vine_light", [helix(i / 26.0, 3.7) for i in range(27)], 0.26, P["sin"], sides=3, sub=1, part="shaft_vine")
        for k, u in enumerate((0.1, 0.24, 0.31, 0.5, 0.58, 0.77, 0.9)):                                        # thorns at uneven spacing, graded length
            b, t = helix(u, 3.2), helix(u + 0.012, 5.4 + 0.5 * (k % 3))
            K.tapered("thorn%d" % k, [b, b.lerp(t, 0.5) + V((0.0, 0.6, 0.0)), t + V((0.0, 2.0, 0.0))], 0.55, 0.08, P["steel"], sides=3, sub=1, part="shaft_thorns")
    return K.quad_estimate()


# ================================================================ 3. PORTAL SPEAR
def portal_palette():
    P = W.Palette("portal", ["obs", "blade", "wrap"], ["void"])
    P["obs"] = W.iron("pw_obs", rgb(22, 18, 34), seed=641, mottle_color=rgb(46, 30, 80), wear_color=rgb(150, 120, 230), bevel=0.2)
    P["blade"] = W.steel("pw_blade", rgb(46, 40, 70), seed=642, rough=0.2, mottle_color=rgb(28, 22, 48), wear_color=rgb(190, 170, 255), scratches=0.8,
                         cramp=("y", 250.0, 345.0, [(0.0, rgb(24, 20, 40)), (0.7, rgb(50, 42, 80)), (1.0, rgb(112, 92, 190))]))
    P["wrap"] = W.hide("pw_wrap", rgb(36, 28, 56), seed=643)
    P["void"] = K.surface("pw_void", rgb(8, 4, 20), 0.2, 0.3, mottle=1.0, mottle_color=rgb(36, 20, 80), bump="leather", bump_scale=0.6, bump_strength=0.4, seed=644, bevel=0.0)
    P["portal"] = W.accent_gem("pw_portal", rgb(120, 76, 255), rgb(150, 100, 255), 0.85, seed=645, veins=0.8, vein_color=rgb(210, 240, 255), vein_strength=0.9)
    return P.check()


def arc_ring(name, center, R, a0, a1, r_fn, mat, part, n=30, sides=7):
    """An OPEN ring (a torus with a gap) in the xy plane round `center`, tube radius r_fn(t) so its thickness is uneven."""
    pts = [V((R * math.cos(a0 + (a1 - a0) * i / (n - 1)), R * math.sin(a0 + (a1 - a0) * i / (n - 1)), 0.0)) for i in range(n)]
    ob = bl.sweep(name, pts, bl.circle(1.0, sides), mat, scale=r_fn, up=(0, 0, 1), part=part, caps=(True, True))
    bl.move(ob, center)
    return K._finish_obj(ob, part, True, 50.0)


def portal_spear(opts):
    FS.begin(opts["samples"])
    P = portal_palette()
    rr = K.rng(8131)
    FS.plain_shaft(P["obs"], 18.0, 218.0, 2.4, sides=8, taper=0.05, swell=0.04)
    K.grip_wrapped("grip", 98.0, 126.0, 2.5, P["wrap"], P["wrap"], turns=6.0, cord=0.42, bands=(), swell=0.08, part="grip", seed=3, sides=8)
    FS.lathe("butt_cap", [(0.01, 0.0), (1.2, 1.2), (2.6, 4.0), (3.4, 8.0), (3.3, 12.5), (2.5, 19.0)], P["obs"], "butt_cap", 10, 40.0)
    FS.lathe("socket_collar", [(2.2, 214.0), (3.6, 216.0), (4.4, 222.0), (3.9, 229.0), (4.6, 235.0), (3.6, 242.0), (2.8, 248.0)], P["obs"], "socket_collar", 10, 34.0)
    W.jewel("socket_pearl", (0.0, 227.0, 4.3), 2.6, P["portal"], P["obs"], mount="claws", axis="z", part="socket_pearl")
    YC, R1 = 268.0, 19.0
    # the great ring: broken open at the lower right, thick at the left and thin where it was cracked, one cut-off stub beyond the gap
    arc_ring("head_ring", (0.0, YC, 0.0), R1, 0.55, TAU - 0.35, lambda t: 3.6 * (1.0 - 0.4 * math.sin(t * math.pi * 0.9) ** 2) * (0.6 + 0.4 * math.cos(t * 3.0)) + 1.0, P["obs"], "head_ring", n=34)
    arc_ring("head_ring_stub", (0.0, YC, 0.0), R1 + 0.2, TAU - 0.12, TAU + 0.2, lambda t: 1.7 - 0.9 * t, P["obs"], "head_ring_stub", n=5, sides=6)
    # the funnel of void behind it and three spiral arms of violet light winding into the centre (the accent)
    prof = [(R1 - 2.4, 0.0), (R1 - 4.4, -1.0), (R1 - 8.0, -2.6), (R1 - 11.5, -5.0), (R1 - 14.4, -8.2), (3.6, -11.0), (3.0, -11.5)]
    fun = bl.lathe("funnel", prof, P["void"], sides=36, part="head_funnel", axis="z", caps=(False, False))
    bl.move(fun, (0.0, YC, 0.0))
    K._finish_obj(fun, "head_funnel", True, 50.0)
    for a_ in range(3):
        pts = []
        for i in range(10):
            t = i / 9
            rad = (R1 - 3.4) * (1.0 - t) ** 0.9 + 3.6 * t
            ang = a_ * TAU / 3 + 1.8 * TAU * t * 0.7
            pts.append(V((rad * math.cos(ang), YC + rad * math.sin(ang), -(1.0 + 10.0 * t ** 1.3) + 0.5)))
        K.tapered("head_arm%d" % a_, pts, 1.0 - 0.1 * a_, 0.3, P["portal"], sides=5, sub=1, part="head_arms")
    # a slim dark blade through the middle of the ring
    FS.jag_blade("head_blade", P["blade"], 244.0, 344.0, [(244, 2.2), (258, 3.0), (274, 3.4), (292, 4.6), (310, 4.8), (326, 3.0), (344, 0.4)], [(300, 0.8, 2.6, "chip")], [(314, 0.9, 2.8, "step")], 24, (-0.5, 345.0),
                 (0.6, 343.0), "head_blade", thick=lambda t: 1.8 * (1.0 - 0.45 * t ** 1.6) + 0.1, angle=40.0, wob=0.0, jitter=0.05, res=5.5,
                 sec=((-1.0, -0.86, -0.5, -0.3, 0.3, 0.5, 0.86, 1.0), (0.0, 0.7, 1.0, 0.5, 0.5, 1.0, 0.7, 0.0)))
    K.shard_cloud("head_shard", (0, 275, 0), (30.0, 55.0, 12.0), 5, (0.9, 1.5), P["portal"], rr, avoid=lambda p: math.hypot(p.x, p.y - YC) < 24.0 and abs(p.z) < 14 or abs(p.x) < 8 and p.y > 244,
                  part="head_shards", stretch=2.0, sides=4, max_count=4, flow=lambda p: V((-(p.y - YC) * 0.1, p.x * 0.1, 0.3)))
    return K.quad_estimate()


# ================================================================ 4. DRAGON MINUTE HAND
def dragon_palette():
    P = W.Palette("ember", ["scale", "hand", "bone"], ["feather", "cord"])
    P["scale"] = W.hide("dm_scale", rgb(74, 22, 22), seed=651, bump="scales", bump_scale=0.8, bump_strength=0.55, wear_color=rgb(190, 90, 70), mottle_color=rgb(30, 8, 10))
    P["hand"] = W.steel("dm_hand", rgb(40, 34, 40), seed=652, rough=0.24, mottle_color=rgb(60, 24, 20), wear_color=rgb(200, 150, 120), scratches=0.9, wear=0.45,
                        cramp=("y", 244.0, 346.0, [(0.0, rgb(26, 22, 28)), (0.55, rgb(48, 38, 42)), (1.0, rgb(120, 62, 44))]))
    P["bone"] = W.paper("dm_bone", rgb(206, 190, 158), seed=653, bump="grain", bump_scale=1.4, bump_strength=0.35, mottle_color=rgb(110, 80, 50))
    P["feather"] = K.surface("dm_feather", rgb(244, 238, 226), 0.0, 0.5, feather=(13, 6, 0.7, 0.8), tramp=[(0.0, rgb(150, 196, 190)), (0.45, rgb(246, 240, 228)), (1.0, rgb(255, 250, 244))], mottle=0.5,
                             mottle_color=rgb(214, 200, 176), wear=0.3, wear_color=rgb(255, 255, 250), wear_metal=0.0, seed=654, bevel=0.06, fine=0.4)
    P["cord"] = W.cord("dm_cord", rgb(120, 70, 44), seed=655)
    P["ember"] = W.accent_gem("dm_ember", rgb(255, 120, 30), rgb(255, 150, 40), 0.9, seed=656)
    return P.check()


def dragon_spear(opts):
    FS.begin(opts["samples"])
    P = dragon_palette()
    rr = K.rng(8141)
    FS.plain_shaft(P["scale"], 18.0, 224.0, 2.4, sides=8, taper=0.05, swell=0.04)
    K.grip_wrapped("grip", 98.0, 126.0, 2.5, P["cord"], P["cord"], turns=6.0, cord=0.4, bands=(), swell=0.08, part="grip", seed=3, sides=8)
    FS.lathe("butt_cap", [(0.01, 0.0), (1.4, 1.4), (3.0, 4.0), (3.4, 9.0), (3.0, 15.0), (2.5, 19.0)], P["hand"], "butt_cap", 10, 40.0)
    # the dragon-scale socket: a tapering sheath with two short rows of real scales (five, then three, unequal), one long and one broken horn
    FS.lathe("socket_sheath", [(2.4, 214.0), (3.8, 222.0), (5.2, 232.0), (4.8, 242.0), (3.2, 252.0)], P["scale"], "socket_sheath", 9, 34.0)
    for r_, (y, cnt, ph, size) in enumerate(((223.0, 5, 0.3, 2.6), (235.0, 3, 1.5, 2.2))):
        for j in range(cnt):
            a = ph + TAU * j / cnt + rr.uniform(-0.12, 0.12)
            rad = 3.9 if r_ == 0 else 5.0
            K.scale_plate("socket_scale%d_%d" % (r_, j), (rad * math.cos(a), y + rr.uniform(-0.8, 0.8), rad * math.sin(a)), (math.cos(a), 0.0, math.sin(a)), size * rr.uniform(0.9, 1.15), P["scale"], thick=0.2, rows=3,
                          part="socket_scales", tilt=18.0)
    K.tapered("socket_horn_long", [V((-3.8, 244.0, -1.0)), V((-9.0, 250.0, -2.5)), V((-12.5, 262.0, -3.5)), V((-11.0, 274.0, -4.0))], 2.0, 0.25, P["bone"], sides=7, sub=3, part="socket_horn",
              bulge=lambda t: 1.0 + 0.18 * math.sin(t * 12.0))
    K.tapered("socket_horn_stub", [V((3.8, 244.0, -1.0)), V((6.2, 248.0, -2.0)), V((6.8, 252.0, -2.4))], 1.7, 0.7, P["bone"], sides=6, sub=1, part="socket_horn")
    # THE HERO: one long minute-hand needle, a spade counter-weight below the pivot, a pivot ring with one ember
    st = [(246, 3.0), (253, 5.2), (260, 7.4), (266, 5.6), (274, 3.5), (300, 2.7), (325, 1.7), (346, 0.3)]
    FS.jag_blade("head_blade", P["hand"], 246.0, 346.0, st, [(284, 0.6, 2.4, "chip"), (310, 0.5, 2.0, "chip")], [(256, 0.7, 2.4, "step")], 95, (0.0, 346.0), (0.3, 345.5), "head_blade",
                 thick=lambda t: 2.0 * (1.0 - 0.55 * t ** 1.4) + 0.1, angle=34.0, wob=0.0, jitter=0.04, res=5.0, sec=((-1.0, -0.8, -0.4, 0.0, 0.4, 0.8, 1.0), (0.0, 0.62, 0.95, 1.2, 0.95, 0.62, 0.0)))
    FS.torus("head_pivot", (0.0, 261.0, 0.0), 4.2, 1.0, P["hand"], "head_pivot", n=22, sides=6)
    W.jewel("head_eye", (0.0, 261.0, 2.4), 1.7, P["ember"], P["hand"], mount="sunk", axis="z", part="head_eye")
    # Fitoria's feather, the one-off: tied below the socket, hanging down the right side
    K.tapered("feather_knot", [V((3.2, 214.0, 2.0)), V((4.0, 211.0, 2.8)), V((3.4, 207.0, 2.2))], 0.7, 0.5, P["cord"], sides=5, sub=1, part="socket_cord")
    K.feather("socket_feather", V((3.6, 211.0, 2.7)), V((0.34, -1.0, 0.22)), 38.0, 8.2, P["feather"], rows=11, serrate=0.14, facing=(0, 0, 1), part="socket_feather", seed=77, thick=0.16, curl=1.4,
              bend=V((2.5, 0.0, 3.0)))
    return K.quad_estimate()


# ================================================================ driver
BUILDERS = {
    "base": (legendary_spear, "legendary_spear", "legendary_spear"),
    "curse": (lambda o: legendary_spear(o, True), "legendary_spear_curse", "legendary_spear"),
    "portal": (portal_spear, "legendary_spear__portal_spear", "portal_spear"),
    "dragon": (dragon_spear, "legendary_spear__dragon_minute_hand", "dragon_minute_hand"),
}


def run(only, trial, quick, samples, bake, skin, shape=None):
    for key in only:
        fn, mesh_id, form = BUILDERS[key]
        q = fn({"samples": samples})
        print("   solid head: factor %.2f, thinnest extent %.4f -> %.4f blocks" % FS.solidify_heads())
        print("   parts:", K.quad_table(16))
        ge, gt = FS.glow_estimate()
        print("[%s] built geometry: ~%d body quads + ~%d glow" % (mesh_id, q, ge), gt)
        if shape:
            os.makedirs(shape, exist_ok=True)
            print("shape sheet:", FS.shape_view(os.path.join(shape, key), full=False))
        if quick or shape:
            continue
        # FS.finish_w reads the place line of weapon_meshes/<base>.txt: the classic copy is where the old line is safe
        FS_base = BASE
        FS.finish_w(mesh_id, FS_base, trial=trial, bake=bake, skin=skin)
        W3.mark_solid(mesh_id, trial)


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--trial")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--shape")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--bake", type=int, default=2048)
    ap.add_argument("--skin", type=int, default=1024)
    a = ap.parse_args(argv)
    run(a.only.split(","), a.trial, a.quick, a.samples, a.bake, a.skin, a.shape)


if __name__ == "__main__":
    main(sys.argv[1:])

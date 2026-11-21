"""Style wave 3 of the VASSAL KATANA forms siderite_katana and white_tiger_katana (docs/heroweapon/style/wave3.md). REPLACEMENTS: the previous meshes and skins are kept in
weapon_meshes/classic_style3/ and textures/entity/weapon/classic_style3/ (client option vassalForms.classicStyleWave3).

    python3 -B tools/blender/forms_katana_w3.py [--only siderite,tiger] [--trial DIR] [--wb DIR] [--quick] [--samples N]

Both keep the base katana's hilt (tsuka y 1..28), guard height (y 30), blade start (y 31) and place line; the blade stays thin (katanas v2 proportions).
  siderite_katana     Stardust Blade: a dark meteoric blade (Widmanstaetten veins painted in the iron) with ONE glowing tempering line along the edge; three crust lumps of different size;
                      a plain irregular iron tsuba; no star halo, no star studs, no orbit ring, no gem, no star cord (they were 18 star plates and a 26 hf noise skin).
  white_tiger_katana  a pale blade whose temper line is a row of eleven tiger stripes of unequal length along the edge only; three graded claw bites on the spine; a dark tiger-paw guard with
                      four ivory claws and ONE amber eye; the striped tail kept as the one-off; no phantom blade, no fur ruff, no tiger face, no gold.
Palette per weapon: 3 base materials + ONE named accent (wave2_kit.Palette). Seeded.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import wave3_kit as W3  # noqa: E402
import forms_kit as K  # noqa: E402
import wave2_kit as W  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402
import forms_vassal_common as C  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
import forms_vassal_katana as FK  # noqa: E402
from forms_vassal_katana import hilt, sc, ws, YG  # noqa: E402
from forms_vassal_run import main  # noqa: E402

BASE = "vassal_katana"


def hamon_line(name, xc, hw, W_, ys, mat, wave, off, w=0.26, thick=1.0, Y0=31.0, Y1=108.0, part="blade_glow"):
    """A thin glowing tempering line along the edge half of both faces (bake-only: no quads), following the curve of the blade, waving like a notare."""
    for s_ in (1, -1):
        rows = []
        for y in ys:
            c = V((xc(y) + hw(y) * off + wave * W_ * math.sin(y * 0.43 + 0.8), y, 0.0))
            rows.append((c - V((w * W_ * 2.2, 0, 0)), c + V((w * W_ * 2.2, 0, 0))))
        zt = thick * W_ * (1.0 - 0.45 * ((ys[len(ys) // 2] - Y0) / (Y1 - Y0)) ** 2.0)
        K.strip("%s%d" % (name, s_), [(l + V((0, 0, s_ * (zt - 0.05))), r + V((0, 0, s_ * (zt - 0.05)))) for l, r in rows], mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.28, part=part, up=(0, 0, s_), detail=True)


# ================================================================ 1. SIDERITE KATANA: the Stardust Blade
def sid_palette():
    P = W.Palette("hamon", ["iron", "dark", "wrap"], ["crust"])
    P["iron"] = W.iron("sw_iron", rgb(46, 50, 62), seed=801, wear=0.6, wear_color=rgb(170, 184, 206), veins=0.6, vein_color=rgb(120, 132, 160), bump_strength=0.1, mottle=0.9, mottle_color=rgb(28, 30, 40),
                       cramp=("y", 31.0, 108.0, [(0.0, rgb(34, 38, 48)), (0.5, rgb(52, 58, 72)), (1.0, rgb(78, 90, 116))]), bevel=0.2)
    P["dark"] = W.iron("sw_dark", rgb(32, 34, 42), seed=802, mottle_color=rgb(54, 44, 40))
    P["wrap"] = W.hide("sw_wrap", rgb(40, 40, 52), seed=803)
    P["crust"] = W.iron("sw_crust", rgb(52, 44, 44), seed=804, rough=0.8, mottle_color=rgb(96, 62, 46), bump="hammer", bump_strength=0.3)
    P["hamon"] = W.accent_glow("sw_hamon", rgb(150, 200, 255), 1.25, seed=805)
    return P.check()


def siderite(opts):
    C.begin(opts)
    C.weights(blade=1.4, guard=1.1, crust=0.8, grip=1.0)
    P = sid_palette()
    rr = K.rng(8301)
    Y0, Y1 = 31.0, 108.0
    Wd = ws(0.42)                                                                              # a little broader than the old blade: a heavy meteor sword
    hw = lambda y: Wd * K.interp([(31, 4.8), (44, 5.3), (75, 5.1), (96, 4.7), (103, 3.9), (106, 1.9), (108, 0.4)], y)           # noqa: E731
    stations = [(y, hw(y)) for y in (31, 44, 75, 96, 103, 106, 108)]
    notL = sc([(52.0, 1.0, 3.4, "chip"), (75.0, 0.8, 2.6, "step"), (94.0, 0.9, 3.0, "chip")], Wd * 1.3)               # a rough spine, three bites at unequal distances
    notR = sc([(70.0, 0.45, 1.8, "chip")], Wd)
    xc = Lb.katana_blade("blade", P["iron"], Y0, Y1, stations, 4.6 * (0.7 if FK.THIN else 1.0), notL=notL, notR=notR, seed=51, tipL=(-0.8 * Wd, 108.8), tipR=(1.2 * Wd, 107.4), thick=1.6 * Wd,
                         fuller=1.0, jit=0.07)
    for k, (y, rx, ry) in enumerate(((47.0, 2.1, 3.0), (66.0, 1.5, 2.2), (88.0, 1.0, 1.5))):                    # three lumps of fallen rock fused to the spine, graded
        K.blob("crust%d" % k, (xc(y) - hw(y) + rr.uniform(-0.1, 0.5), y, rr.uniform(-0.4, 0.4) * Wd), (rx * 0.62, ry, rx * 0.56), P["crust"], seed=80 + k, lump=0.28, lump_scale=0.14, rings=6, segs=9, part="crust")
    hamon_line("hamon", xc, hw, Wd, [38.0 + 3.0 * i for i in range(24)], P["hamon"], 0.5, 0.52, Y0=Y0, Y1=Y1)
    for s_ in (1, -1):
        hab = [(x * (Wd * 1.25 if FK.THIN else 1.0), y) for x, y in ((-4.6, 30.6), (4.8, 30.6), (5.0, 35.0), (3.4, 38.0), (-3.2, 38.0), (-5.0, 35.0))]
        K.stepped_plate("habaki%d" % s_, hab, 1.6 * Wd * s_, P["dark"], steps=2, step_h=0.6 * (0.7 if FK.THIN else 1.0), inset=0.9 * Wd, part="habaki", up=s_, bevel_w=0.0)
    # a plain, slightly lopsided iron tsuba
    Lb.disc_guard("tsuba", YG - 0.6, 4.2, P["dark"], "guard", steps=2, step_h=0.65, inset=0.9, lobes=3, lobe_amp=0.06, rr=rr)
    hilt(P["dark"], P["wrap"], P["dark"], P["dark"], seed=8, turns=8.0, bands=(2.6, 26.6))
    cp = [V(p) for p in ((0.0, -3.4, 0.0), (1.2, -7.0, 0.7), (1.8, -12.0, 0.2))]
    K.tapered("sageo", cp, 0.4, 0.28, P["wrap"], sides=5, sub=3, part="tassel")
    K.blob("menuki", (0.0, 15.5, 1.9), (0.8, 1.3, 0.45), P["crust"], seed=7, lump=0.2, rings=4, segs=6, part="menuki")                 # one small fused fleck on the grip (HeroKatanasTest lore parts)
    for k, (x, y, z, r) in enumerate(((-3.4, 80.0, 2.4, 0.95), (3.0, 90.0, -2.6, 0.7), (-2.2, 99.0, 2.8, 0.5))):                       # three shards of the meteor still circling the upper blade, graded (above the 45..70 percent stretch the size rule measures the blade on)
        K.blob("orbit%d" % k, (x, y, z), (r, r * 1.3, r * 0.9), P["crust"], seed=90 + k, lump=0.3, lump_scale=0.2, rings=4, segs=6, part="orbit")
    return K.quad_estimate()


# ================================================================ 2. WHITE TIGER KATANA
def tiger_palette():
    P = W.Palette("amber", ["steel", "dark", "ivory"], ["tail", "bone"])
    P["steel"] = W.steel("wt_steel", rgb(206, 213, 224), seed=811, rough=0.22, mottle_color=rgb(150, 172, 200), cramp=("y", 31.0, 106.0, [(0.0, rgb(176, 186, 202)), (0.6, rgb(222, 230, 240)), (1.0, rgb(196, 224, 244))]))
    P["dark"] = W.iron("wt_dark", rgb(28, 28, 34), seed=812, mottle_color=rgb(54, 52, 58))
    P["ivory"] = W.cord("wt_ivory", rgb(232, 228, 216), seed=813)
    P["tail"] = K.surface("wt_tail", rgb(236, 232, 222), 0.0, 0.8, bump="rings", bump_scale=1.5, bump_strength=0.9, groove=rgb(14, 14, 18), groove_amount=1.0, dirt=0.5, mottle=0.6, mottle_color=rgb(190, 180, 165),
                          fine=0.6, seed=814, bevel=0.0)
    P["bone"] = W.paper("wt_bone", rgb(236, 230, 214), seed=815, bump="grain", bump_scale=1.4, bump_strength=0.3, mottle_color=rgb(170, 150, 120))
    P["amber"] = W.accent_gem("wt_amber", rgb(255, 170, 30), rgb(255, 190, 50), 0.9, seed=816)
    return P.check()


def white_tiger(opts):
    C.begin(opts)
    C.weights(blade=1.4, guard=1.1, paw=1.1, stripes=1.0, tail=0.8)
    P = tiger_palette()
    rr = K.rng(8311)
    Y0, Y1 = 31.0, 106.0
    Wd = ws(0.5)
    stations = [(y, h * Wd) for y, h in ((31, 3.7), (40, 4.0), (60, 3.9), (90, 3.5), (96, 3.4), (100, 2.6), (103.5, 1.3), (106, 0.3))]
    notL = sc([(54.0, 1.5, 5.0, "tooth"), (67.0, 1.2, 4.2, "tooth"), (80.0, 0.8, 3.0, "tooth")], Wd)             # three claw bites, graded (the silhouette)
    notR = sc([(84.0, 0.3, 1.6, "chip")], Wd)
    xc = Lb.katana_blade("blade", P["steel"], Y0, Y1, stations, 2.6 * (0.7 if FK.THIN else 1.0), notL=notL, notR=notR, seed=11, tipL=(-0.2 * Wd, 106.4), tipR=(0.9 * Wd, 105.4), thick=1.0 * Wd, fuller=1.0)
    # the temper line is a row of tiger stripes along the EDGE half only (both faces, bake-only), unequal gaps and lengths, thick at the edge and tapering inward
    ys = W.positions(37.0, 99.0, (1.0, 1.7, 0.8, 1.3, 1.9, 0.9, 1.4, 1.1, 1.8, 1.0))
    for s_ in (1, -1):
        for k, y in enumerate(ys):
            half = 3.0 * Wd * (1.0 - 0.45 * (y - 31) / 75.0)
            x1 = xc(y) + half * 0.95
            x0 = xc(y) + half * rr.uniform(-0.05, 0.4)
            slant = rr.uniform(2.0, 3.4) * Wd
            wid = rr.uniform(0.9, 1.5) * Wd
            rows = []
            for j in range(6):
                t = j / 5.0
                cx = x1 + (x0 - x1) * t
                cy = y + slant * t * t
                w = wid * (1.0 - t ** 1.5) + 0.04
                rows.append((V((cx, cy - w, 0.0)), V((cx, cy + w, 0.0))))
            zt = 1.0 * Wd * (1.0 - 0.45 * ((y - Y0) / (Y1 - Y0)) ** 2.0)
            K.strip("stripe%d_%d" % (s_, k), [(l + V((0, 0, s_ * (zt - 0.05))), r + V((0, 0, s_ * (zt - 0.05)))) for l, r in rows], P["dark"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.28, part="stripes",
                    up=(0, 0, s_), detail=True)
    for s_ in (1, -1):
        hab = [(x * (Wd * 1.1 if FK.THIN else 1.0), y) for x, y in ((-4.2, 30.6), (4.4, 30.6), (4.6, 35.0), (2.8, 38.0), (-2.6, 37.0), (-4.4, 34.5))]
        K.stepped_plate("habaki%d" % s_, hab, 1.05 * Wd * s_, P["dark"], steps=2, step_h=0.55 * (0.7 if FK.THIN else 1.0), inset=0.8 * Wd, part="habaki", up=s_, bevel_w=0.0)
    # the guard: a dark tiger paw (a pad and four toes of different size) standing on the blade, four ivory claws, ONE amber eye on the pad
    def ell(cx, cy, rx, ry, n=18):
        return [(cx + rx * math.cos(TAU * i / n), cy + ry * math.sin(TAU * i / n)) for i in range(n)]
    ang0 = 0.16                                                                                   # the whole paw is turned and off-centre: left and right are not twins

    def T(x, y):
        c, s_ = math.cos(ang0), math.sin(ang0)
        k = 0.8                                                                                   # outline <= 1/14 of the length (HeroKatanasTest)
        return (k * (c * x - s_ * (y - YG)) + 0.4, k * (s_ * x + c * (y - YG)) + YG)
    pad = [T(*p) for p in ell(0.0, YG - 1.2, 3.6, 2.8)]                                           # small enough that the outline stays a katana's (HeroKatanasTest: <= 1/13 of the length)
    toes = [(-3.1, YG + 1.4, 1.5, 1.9), (-1.1, YG + 3.6, 1.7, 1.7), (1.3, YG + 3.5, 1.3, 2.1), (3.2, YG + 1.2, 1.2, 1.4)]
    for s_ in (1, -1):
        K.stepped_plate("paw_pad%d" % s_, pad, 0.6 * s_, P["dark"], steps=3, step_h=0.65, inset=0.9, part="pad", up=s_, bevel_w=0.0)
        for k, (cx, cy, rx, ry) in enumerate(toes):                                                # one part name per toe: four different toes, never a row of identical ones
            K.stepped_plate("paw_toe%d_%d" % (k, s_), [T(*p) for p in ell(cx, cy, rx, ry, 12)], 0.6 * s_, P["dark"], steps=2, step_h=0.65, inset=0.6, part="toe_" + "abcd"[k], up=s_, bevel_w=0.0)
    for k, (cx, cy, rx, ry) in enumerate(toes):
        a = math.atan2(cy - (YG - 1.2), cx)
        bx, by = T(cx + math.cos(a) * rx * 0.8, cy + math.sin(a) * ry * 0.8)
        dx, dy = math.cos(a) * 0.3, math.sin(a) + 0.3
        Lb.claw("paw_claw%d" % k, V((bx, by, 0.0)), V((math.cos(ang0) * dx - math.sin(ang0) * dy, math.sin(ang0) * dx + math.cos(ang0) * dy, 0.0)), (3.0, 2.1, 2.6, 3.5)[k], 0.5, P["bone"],
                curl=1.2 * (1 if k < 2 else -1), side=(0, 0, 1), part="claw_" + "abcd"[k], sides=5)
    K.gem_setting("eye", V((0.4, YG - 1.0, 3.0)), 1.1, P["amber"], P["dark"], normal=(0, 0, 1), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.4)
    hilt(P["dark"], P["ivory"], P["dark"], P["dark"], seed=4, bands=(2.6, 26.6))
    tp = [V(p) for p in ((0.0, 0.0, 0.0), (1.0, -5.0, 0.8), (3.4, -10.5, 1.4), (4.6, -16.0, 0.2), (3.6, -21.0, -1.2))]
    K.tapered("tail", tp, 1.0, 0.55, P["tail"], sides=7, sub=3, part="tassel", bulge=lambda t: 1.0 + 0.2 * math.sin(t * 9.0))
    K.blob("menuki", (0.0, 11.0, 1.95), (0.8, 1.3, 0.45), P["bone"], seed=8, lump=0.15, rings=4, segs=6, part="menuki")                         # one small ivory fleck on the grip (HeroKatanasTest lore parts)
    K.blob("kashira_gem", (0.0, -0.7, 0.0), (1.0, 0.8, 1.0), P["amber"], seed=9, lump=0.05, rings=4, segs=7, part="kashira_gem")
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["siderite"] = (siderite, "vassal_katana__siderite_katana", "siderite_katana")
BUILDERS["tiger"] = (white_tiger, "vassal_katana__white_tiger_katana", "white_tiger_katana")

if __name__ == "__main__":
    W3.use_classic_headers()
    main(sys.argv[1:], BUILDERS, BASE, "katana", W3.MANIFEST)

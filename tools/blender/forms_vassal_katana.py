"""Weapon-form models of the VASSAL KATANA (base `vassal_katana`, size class katana), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_katana.py --only tiger [--trial DIR] [--wb DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids (docs/heroweapon/form_models.md): vassal_katana__<form>:
  white_tiger_katana   pale clawed blade with tiger stripes, a phantom second blade, a tiger-face guard with fur, a striped tail
  soul_eater_katana    black blade grown with a rib-cage of bone, a skull guard, soul flames licking up the edge, a wisp tail
  siderite_katana      a long, heavy meteor-iron blade (Widmanstaetten lattice), an eight-point star guard, star studs, a chipped spine
  sakura_stone_katana  a pink stone blade, a five-petal guard, cherry branches round the hilt, a blossom crown, drifting petals
Every form keeps the base's hilt (tsuka y 1..28, guard y ~30, blade from y 31, edge toward +x) and place line. Seeded: the same seed, the same weapon.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402
import forms_vassal_common as C  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
from forms_vassal_run import main  # noqa: E402

MANIFEST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "forms_manifest_vassal_katana.json")
BASE = "vassal_katana"
YG = 30.0                      # guard height (the base's tsuba)


def hilt(core, wrap, band, kash, *, wrap2=None, seed=3, turns=8.0, r=1.8, bands=(2.4, 14.5, 26.6), kash_r=2.6):
    """The tsuka, identical in place to the base's (y 1.5..27.5): swelling core, wound cord, bands, and a kashira cap."""
    Lb.tsuka(None, 1.5, 27.5, r, seed, None, wrap, core, band, turns, bands)
    if wrap2 is not None:
        K.helix_wrap("grip_wrap2", 2.0, 27.0, r * 1.07, wrap2, turns=turns * 0.5, cord=0.3, part="grip", seed=seed + 5, squash=0.92)
    Lb.lathe("kashira", [(0.01, -0.6), (1.6, -0.5), (kash_r, 0.6), (kash_r + 0.1, 2.0), (2.0, 3.2), (1.85, 3.7)], kash, "pommel", sides=14, squash=0.92)
    Lb.lathe("fuchi", [(1.7, 27.6), (2.5, 28.0), (2.8, 28.8), (2.6, 29.6), (1.9, 29.9)], band, "guard_collar", sides=14, squash=0.92)


# ================================================================ 1. WHITE TIGER KATANA
def tiger_palette():
    P = {}
    P["steel"] = Lb.metal("tiger_steel", rgb(206, 214, 226), rgb(255, 255, 255), 101, rough=0.22, mottle=rgb(150, 175, 205), dirt=0.35, dirt_col=(0.05, 0.06, 0.08, 1),
                          cramp=("y", 31.0, 106.0, [(0.0, rgb(178, 188, 204)), (0.6, rgb(222, 230, 240)), (1.0, rgb(190, 225, 245))]))
    P["black"] = Lb.metal("tiger_black", rgb(22, 24, 30), rgb(120, 130, 150), 102, rough=0.4, dirt=0.3, dirt_col=(0.0, 0.0, 0.0, 1), bump="hammer", bs=0.12)
    P["gold"] = Lb.hammered("tiger_gold", rgb(226, 178, 66), rgb(255, 244, 200), 103, mottle=rgb(160, 100, 30))
    P["fur"] = Lb.paint("tiger_fur", rgb(236, 232, 220), rgb(255, 255, 250), 104, rough=0.8, dirt_col=(0.05, 0.045, 0.04, 1), mottle=rgb(190, 180, 160), bump="leather")
    P["fur_dark"] = Lb.paint("tiger_fur_dark", rgb(30, 28, 32), rgb(120, 118, 124), 105, rough=0.8, bump="leather")
    P["ito"] = Lb.cloth("tiger_ito", rgb(238, 236, 228), rgb(110, 108, 100), 106, mottle=rgb(170, 165, 150), bump="wrap", scale=1.0)
    P["ito_b"] = Lb.cloth("tiger_ito_b", rgb(28, 28, 34), rgb(4, 4, 6), 107, mottle=rgb(60, 60, 70), bump="wrap", scale=1.0)
    P["core"] = Lb.paint("tiger_core", rgb(60, 56, 58), rgb(150, 145, 140), 108, bump="leather")
    P["tail"] = K.surface("tiger_tail", rgb(236, 232, 222), 0.0, 0.8, bump="rings", bump_scale=1.5, bump_strength=0.9, groove=rgb(14, 14, 18), groove_amount=1.0, dirt=0.5, mottle=0.6,
                          mottle_color=rgb(190, 180, 165), fine=0.6, seed=109, bevel=0.0)
    P["ghost"] = Lb.glow("tiger_ghost", rgb(160, 225, 255), 0.85, 110, flicker=0.7, ramp=("y", 38.0, 98.0, [(0.0, rgb(120, 190, 255)), (0.6, rgb(190, 240, 255)), (1.0, rgb(235, 255, 255))]))
    P["eye"] = Lb.gem("tiger_eye", rgb(255, 176, 30), 1.0, 111)
    P["ice"] = Lb.glow("tiger_ice", rgb(170, 230, 255), 0.55, 112, flicker=0.5)
    return P


def white_tiger(opts):
    C.begin(opts)
    C.weights(blade=1.35, ghost=0.85, guard=1.1, tiger=1.15, tail=0.8, fur=0.9)
    P = tiger_palette()
    rr = K.rng(7101)
    Y0, Y1 = 31.0, 106.0
    stations = [(y, h) for y, h in ((31, 3.7), (40, 4.0), (60, 3.9), (90, 3.5), (96, 3.4), (100, 2.6), (103.5, 1.3), (106, 0.3))]
    # claw bites cut into the spine (the left, -x edge): three deep, slanted, tiger-claw nicks; the edge side gets small chips
    notL = [(52.0, 1.5, 5.0, "tooth"), (64.0, 1.6, 5.4, "tooth"), (77.0, 1.4, 5.0, "tooth"), (89.0, 0.9, 3.4, "v")]
    notR = [(58.0, 0.35, 1.8, "chip"), (82.0, 0.3, 1.6, "chip")]
    xc = Lb.katana_blade("blade", P["steel"], Y0, Y1, stations, 4.4, notL=notL, notR=notR, seed=11, tipL=(-0.2, 106.4), tipR=(0.9, 105.4), thick=1.05, fuller=1.0)
    # tiger stripes: black bake-only relief bands slanting across both faces, thick at the spine and tapering to a point toward the edge (free in quads)
    for s_ in (1, -1):
        for k in range(12):
            y = 38.0 + k * 5.7 + rr.uniform(-0.7, 0.7)
            half = 3.0 * (1.0 - 0.45 * (y - 31) / 75.0)
            x0 = xc(y) - half * rr.uniform(0.85, 1.0)
            x1 = xc(y) + half * rr.uniform(0.2, 0.55) * (1 if k % 3 else 0.8)
            slant = rr.uniform(2.0, 3.6)
            wid = rr.uniform(0.8, 1.35)
            rows = []
            for j in range(6):
                t = j / 5.0
                cx = x0 + (x1 - x0) * t
                cy = y + slant * t * t
                w = wid * (1.0 - t ** 1.6) + 0.04
                rows.append((V((cx, cy - w, 0.0)), V((cx, cy + w, 0.0))))
            zt = 1.0 * (1.0 - 0.45 * ((y - Y0) / (Y1 - Y0)) ** 2.0)
            K.strip("stripe%d_%d" % (s_, k), [(l + V((0, 0, s_ * (zt - 0.05))), r + V((0, 0, s_ * (zt - 0.05)))) for l, r in rows], P["black"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.28,
                    part="stripes", up=(0, 0, s_), detail=True)
    # the phantom second blade (Double Sword): a slimmer pale echo behind and to the edge side, slightly shorter, glowing, tied to the habaki by two light rods
    gst = [(38, 2.4), (46, 2.8), (80, 2.6), (90, 2.3), (95, 1.5), (99, 0.3)]
    GL = K.blade_edge(-1, gst, 38, 99, jitter=0.03, res=6.0, seed=21, tip=(0.2, 99.6))
    GR = K.blade_edge(1, gst, 38, 99, jitter=0.03, res=6.0, seed=22, tip=(0.6, 98.6))
    K.blade("ghost", P["ghost"], [V((p.x + xc(p.y) * 0.9 + 7.4, p.y, -2.6)) for p in GL], [V((p.x + xc(p.y) * 0.9 + 7.4, p.y, -2.6)) for p in GR], thick=0.4, section=((-1.0, -0.5, 0.5, 1.0), (0.0, 1.0, 1.0, 0.0)),
            part="ghost", seed=5, wob=0.05)
    for k, (yy, dx) in enumerate(((40.0, 0.0), (50.0, 0.8))):
        K.tapered("lightrod%d" % k, [V((3.0, 34.0 + k * 3, 0.4)), V((5.6, yy, -1.4)), V((7.4 + dx, yy + 1.0, -2.6))], 0.5, 0.18, P["ice"], sides=5, sub=1, part="ghost")
    # habaki + ricasso: a white-gold collar with a cut-out tiger-claw step
    for s_ in (1, -1):
        hab = [(-4.2, 30.6), (4.4, 30.6), (4.6, 39.0), (2.6, 43.0), (-2.4, 41.0), (-4.4, 38.0)]
        K.stepped_plate("habaki%d" % s_, hab, 1.05 * s_, P["gold"], steps=2, step_h=0.55, inset=0.8, part="habaki", up=s_, bevel_w=0.0)
        K.rivets("hab_rv%d" % s_, [(V((x, y, 2.0 * s_)), (0, 0, s_)) for x, y in ((-2.6, 34.0), (2.8, 34.0), (0.0, 38.0))], P["black"], r=0.6, rng_=rr, jitter=0.15, detail=False)
    # ---------------------------------------------------------------- guard: a lobed round tsuba, four tiger-claw wings (upper pair swept up, lower pair down), a fur ruff
    Lb.disc_guard("tsuba", YG - 0.6, 5.8, P["gold"], "guard", steps=2, step_h=0.65, inset=0.9, lobes=5, lobe_amp=0.07, rr=rr)
    for sx, ang, ln, wd, mat in ((1, 22, 20.0, 5.4, "steel"), (-1, 158, 16.5, 4.8, "steel"), (1, -30, 12.0, 3.6, "black"), (-1, -150, 10.0, 3.4, "black")):
        a = math.radians(ang)
        Lb.lancet("wing%d_%d" % (sx, ang), V((sx * 4.2, YG + 0.2, 0.0)), V((math.cos(a), math.sin(a), 0)), ln, wd, P[mat], thick=0.85, part="guard", curve=sx * (2.4 if ang > 0 else -1.8), z=0.0, n=8, belly=0.4)
    for sx, ang, ln, wd in ((1, 52, 10.0, 2.8), (-1, 128, 8.0, 2.4)):                       # short second row on the top wings: the stripe-edged feathers of a fan
        a = math.radians(ang)
        Lb.lancet("wing2_%d" % sx, V((sx * 3.8, YG + 1.2, 0.9)), V((math.cos(a), math.sin(a), 0)), ln, wd, P["black"], thick=0.5, part="guard", curve=sx * 1.2, z=0.0, n=6)
    for s_ in (1, -1):
        K.gem_setting("eye%d" % s_, V((0, YG + 0.2, 2.9 * s_)), 1.5, P["eye"], P["gold"], normal=(0, 0, s_), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.4)
    # fur ruff round the tsuba rim (in the xz plane, tilted up): white tufts with dark tips
    bases, dirs = [], []
    for i in range(22):
        a = TAU * i / 22 + rr.uniform(-0.1, 0.1)
        bases.append(V((5.2 * math.cos(a), YG + 0.4, 4.6 * math.sin(a))))
        dirs.append(V((math.cos(a), 0.55 + 0.25 * math.sin(i * 1.7), math.sin(a))))
    Lb.fur("ruff", bases, dirs, 3.6, 0.55, P["fur"], rr, part="fur", tip_mat=P["fur_dark"])
    # ---------------------------------------------------------------- the tiger face on the guard hub (both faces, a little different): brow, ears, nose, fangs
    for s_ in (1, -1):
        zb = 2.2 * s_
        K.blob("skull%d" % s_, (0.0, YG + 0.3, zb + 0.6 * s_), (3.6, 3.2, 1.9), P["fur"], seed=20 + s_, lump=0.05, rings=7, segs=10, part="tiger")
        for ex in (-1, 1):
            K.blob("ear%d_%d" % (s_, ex), (ex * 3.3, YG + 3.1, zb), (1.1, 1.3, 0.7), P["fur_dark"], seed=30 + ex, lump=0.08, rings=5, segs=8, part="tiger")
            K.blob("cheek%d_%d" % (s_, ex), (ex * 2.8, YG - 1.2, zb + 0.3 * s_), (1.3, 1.1, 0.9), P["fur"], seed=40 + ex, lump=0.1, rings=5, segs=8, part="tiger")
            Lb.claw("fang%d_%d" % (s_, ex), V((ex * 0.9, YG - 2.4, zb + 1.3 * s_)), V((ex * 0.12, -1.0, 0.2 * s_)), 2.4, 0.38, P["fur"], curl=0.5 * s_, side=(0, 0, s_), part="tiger", sides=4)
        K.blob("nose%d" % s_, (0.0, YG - 0.7, zb + 1.9 * s_), (0.8, 0.6, 0.5), P["fur_dark"], seed=50, lump=0.05, rings=4, segs=6, part="tiger")
        for k in range(3):                                                                   # brow stripes
            for ex in (-1, 1):
                K.strip("brow%d_%d_%d" % (s_, k, ex), [(V((ex * (0.6 + 0.3 * k), YG + 1.4 + k * 0.5, zb + 1.5 * s_)), V((ex * (1.4 + 0.5 * k), YG + 1.6 + k * 0.55, zb + 1.3 * s_))),
                                                      (V((ex * (2.0 + 0.6 * k), YG + 1.7 + k * 0.55, zb + 1.0 * s_)), V((ex * (2.8 + 0.5 * k), YG + 2.4 + k * 0.4, zb + 0.8 * s_)))],
                        P["black"], (-1.0, 0.0, 1.0), (0.4, 1.0, 0.4), 0.14, part="tiger", up=(0, 0, s_), detail=True)
    # ---------------------------------------------------------------- hilt: ivory cord over a dark core with a black diamond cord, kashira with a tiger-paw cap, a striped tail with a fur tuft
    hilt(P["core"], P["ito"], P["gold"], P["gold"], wrap2=P["ito_b"], seed=4)
    for k, (ang, rad) in enumerate(((30, 2.0), (90, 2.2), (150, 2.0))):                     # three paw pads round the cap
        a = math.radians(ang)
        K.blob("pad%d" % k, (rad * math.cos(a) * 0.5, 0.3, rad * math.sin(a) * 0.5 + 0.4), (0.9, 0.5, 0.7), P["fur_dark"], seed=60 + k, lump=0.06, rings=4, segs=6, part="pommel")
    tp = [V(p) for p in ((0.0, 0.0, 0.0), (1.0, -5.0, 0.8), (3.4, -10.5, 1.4), (4.6, -16.0, 0.2), (3.6, -21.0, -1.2), (2.4, -25.0, -0.8))]
    K.tapered("tail", tp, 1.0, 0.55, P["tail"], sides=7, sub=3, part="tail_sageo", bulge=lambda t: 1.0 + 0.2 * math.sin(t * 9.0))
    Lb.fur("tailtuft", [tp[-1]] * 7, [V((math.sin(k), -1.0, math.cos(k) * 0.6)) for k in range(7)], 4.0, 0.5, P["fur"], rr, part="tail_sageo", tip_mat=P["fur_dark"])
    K.gem_setting("pommelgem", V((0, -1.0, 0.0)), 1.2, P["ice"], P["gold"], normal=(0, -1, 0), claws=4, sides=6, part="pommel_gem", rng_=rr, up=(0, 0, 1), claw_r=0.4)
    # ice-glints along the edge (bounded: 7)
    for k in range(7):
        y = 45 + k * 8.0
        K.shard("glint%d" % k, V((xc(y) + 4.4 + rr.uniform(-0.4, 0.8), y, rr.uniform(-0.6, 0.6))), 0.55, P["ice"], rr, part="blade_glints", sides=4, stretch=2.4, dir_=V((0.4, 1.0, 0.0)))
    return K.quad_estimate()


# ================================================================ 2. SOUL EATER KATANA
def soul_palette():
    P = {}
    P["iron"] = Lb.metal("soul_iron", rgb(36, 40, 50), rgb(150, 190, 190), 201, rough=0.3, mottle=rgb(18, 60, 60), dirt=0.6, dirt_col=(0.0, 0.01, 0.01, 1),
                         cramp=("y", 31.0, 104.0, [(0.0, rgb(28, 30, 40)), (0.6, rgb(42, 52, 62)), (1.0, rgb(40, 84, 84))]))
    P["dark"] = Lb.hammered("soul_dark", rgb(60, 70, 80), rgb(170, 210, 205), 202, mottle=rgb(20, 50, 50), dirt_col=(0.0, 0.01, 0.01, 1))
    P["bone"] = Lb.paint("soul_bone", rgb(218, 208, 178), rgb(255, 252, 234), 203, rough=0.6, dirt_col=(0.09, 0.06, 0.02, 1), mottle=rgb(150, 130, 90), bump="leather")
    P["bone2"] = Lb.paint("soul_bone2", rgb(176, 166, 140), rgb(240, 232, 205), 204, rough=0.6, dirt_col=(0.07, 0.05, 0.02, 1), mottle=rgb(110, 96, 70), bump="leather")
    P["soul"] = Lb.glow("soul_glow", rgb(100, 255, 205), 0.9, 205, flicker=0.8, ramp=("y", 31.0, 110.0, [(0.0, rgb(60, 200, 170)), (0.6, rgb(110, 255, 210)), (1.0, rgb(200, 255, 240))]))
    P["soul2"] = Lb.glow("soul_glow2", rgb(150, 120, 255), 0.8, 206, flicker=0.8)
    P["orb"] = Lb.gem("soul_orb", rgb(120, 255, 215), 1.2, 207)
    P["wrap"] = Lb.cloth("soul_wrap", rgb(70, 96, 92), rgb(10, 24, 24), 208, mottle=rgb(34, 54, 50), bump="wrap", scale=1.0)
    P["core"] = Lb.paint("soul_core", rgb(36, 36, 42), rgb(120, 130, 135), 209, bump="leather")
    P["wisp"] = Lb.glow("soul_wisp", rgb(130, 255, 215), 0.6, 210, flicker=0.9, ramp=("y", -26.0, 2.0, [(0.0, rgb(60, 160, 170)), (0.5, rgb(110, 240, 215)), (1.0, rgb(180, 255, 235))]))
    return P


def soul_eater(opts):
    C.begin(opts)
    C.weights(blade=1.35, flames=0.7, guard=1.0, skull=1.15, ribs=1.0, wisps=0.7)
    P = soul_palette()
    rr = K.rng(7202)
    Y0, Y1 = 31.0, 104.5
    hw = lambda y: K.interp([(31, 4.2), (42, 4.6), (70, 4.3), (92, 3.8), (98, 3.5), (101.5, 2.2), (104.5, 0.4)], y)     # noqa: E731
    stations = [(y, hw(y)) for y in (31, 42, 70, 92, 98, 101.5, 104.5)]
    notL = [(46.0, 0.5, 2.0, "chip"), (62.0, 0.6, 2.4, "chip"), (79.0, 0.5, 2.0, "chip")]
    notR = [(52.0, 1.1, 4.4, "tooth"), (66.0, 1.2, 4.8, "tooth"), (80.0, 1.1, 4.4, "tooth"), (92.0, 0.9, 3.4, "tooth")]       # the blade bites: a row of hungry teeth along the edge
    xc = Lb.katana_blade("blade", P["iron"], Y0, Y1, stations, 4.0, notL=notL, notR=notR, seed=31, tipL=(-1.2, 105.2), tipR=(1.0, 103.6), thick=1.15, fuller=1.0)
    for s_ in (1, -1):                                                                        # a soul channel glowing down the groove: real geometry
        rows = []
        for y in range(40, 100, 4):
            c = V((xc(y) - hw(y) * 0.34, y + (y % 8) * 0.0, 0.0))
            w = 0.36 * (1.0 - 0.55 * ((y - 40) / 60.0))
            rows.append((c - V((w, 0, 0)), c + V((w, 0, 0))))
        K.strip("channel%d" % s_, rows, P["soul"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: 0.55 * (1 - 0.3 * t), part="blade_glow", up=(0, 0, s_))
    for s_ in (1, -1):
        hab = [(-4.4, 30.6), (4.6, 30.6), (4.8, 38.6), (3.2, 42.0), (-3.4, 42.0), (-4.8, 38.6)]
        K.stepped_plate("habaki%d" % s_, hab, 1.1 * s_, P["dark"], steps=2, step_h=0.55, inset=0.8, part="habaki", up=s_, bevel_w=0.0)
        K.rivets("hab_rv%d" % s_, [(V((x, y, 2.1 * s_)), (0, 0, s_)) for x, y in ((-2.8, 34.0), (2.8, 34.0), (0.0, 38.5))], P["bone"], r=0.6, rng_=rr, jitter=0.15, detail=False)
    # the rib cage: seven curved ribs grow from the spine (alternating front and back) with a vertebra at each root
    for k in range(7):
        y = 38.0 + k * 8.6
        zs = 1.0 if k % 2 == 0 else -1.0
        sp = V((xc(y) - hw(y) * 0.92, y, 0.0))
        ln = 12.5 - k * 0.9
        tip = sp + V((-ln * 0.82, ln * 0.62, zs * (2.6 + 0.2 * (k % 3))))
        K.bone("rib%d" % k, sp, tip, 0.5 + 0.03 * (6 - k), P["bone"], knob=1.6, sub=3, sides=6, part="ribs", bow=V((-0.5 * ln * 0.2, ln * 0.28, zs * ln * 0.22)), seed=k)
        K.blob("vert%d" % k, (sp.x - 0.1, y, 0.0), (1.2, 1.1, 1.0), P["bone2"], seed=70 + k, lump=0.1, rings=5, segs=8, part="ribs")
    # soul flames licking up the edge side, each with its own sway and height
    for k, y in enumerate((38.0, 49.0, 60.0, 72.0, 84.0, 95.0)):
        zs = 1 if k % 2 else -1
        ln = rr.uniform(9.0, 14.0) * (1.0 - 0.03 * k)
        Lb.flame("flame%d" % k, V((xc(y) + hw(y) - 0.4, y, zs * 0.5)), V((0.45 + 0.05 * (k % 3), 1.0, zs * 0.2)), ln, rr.uniform(3.4, 4.6), P["soul"] if k % 3 else P["soul2"], bend=V((rr.uniform(1.5, 4.0), 0, 0)),
                  curl=zs * 1.6, facing=(0, 0, zs), seed=k, rows=7)
    # a captured soul: a bright orb held in a cage of bone claws between the rib roots
    oc = V((xc(56) - 6.2, 56.0, 0.0))
    K.blob("soulorb", oc, (1.9, 1.9, 1.9), P["orb"], seed=8, lump=0.05, rings=6, segs=10, part="orb")
    for k in range(5):
        a = TAU * k / 5 + 0.3
        Lb.claw("cage%d" % k, oc + V((2.4 * math.cos(a), -1.8, 2.4 * math.sin(a))), V((-0.25 * math.cos(a), 1.0, -0.25 * math.sin(a))), 4.4, 0.34, P["bone"], curl=0.0, side=(math.cos(a), 0, math.sin(a)), part="orb_cage", sides=4)
    # ---------------------------------------------------------------- guard: one long femur as the crossguard, a skull on the hub with glowing sockets and teeth, curved horns
    K.bone("femur", V((-11.5, YG + 0.6, -0.3)), V((13.5, YG - 0.2, 0.2)), 0.95, P["bone"], knob=1.9, sub=4, sides=8, part="guard", bow=V((0.0, 1.4, 0.0)), seed=3)
    K.bone("femur2", V((-6.5, YG - 3.2, -2.2)), V((7.5, YG + 3.0, -2.0)), 0.7, P["bone2"], knob=1.8, sub=3, sides=6, part="guard", bow=V((0.0, -1.0, 0.0)), seed=5)
    K.blob("cranium", (0.0, YG + 0.9, 1.0), (3.7, 3.5, 3.1), P["bone"], seed=21, lump=0.06, rings=8, segs=12, part="skull")
    K.blob("jaw", (0.0, YG - 2.3, 1.1), (2.5, 1.5, 2.1), P["bone2"], seed=22, lump=0.07, rings=5, segs=10, part="skull")
    for ex in (-1, 1):
        K.gem_setting("socket%d" % ex, V((ex * 1.5, YG + 1.1, 3.6)), 0.95, P["soul"], P["bone2"], normal=(0, 0.1, 1), claws=0, sides=6, part="skull_eyes", rng_=rr, claw_r=0.3)
        K.blob("cheekb%d" % ex, (ex * 2.9, YG - 0.5, 1.6), (1.1, 1.0, 1.0), P["bone"], seed=23 + ex, lump=0.08, rings=4, segs=8, part="skull")
        for hs, (ang, ln) in ((ex, (55 if ex > 0 else 125, 8.0 + ex * 0.8)),):
            a = math.radians(ang)
            pts = [V((ex * 2.6, YG + 3.0, 0.4)), V((ex * 4.6, YG + 6.0, 0.2)), V((ex * 7.6, YG + 7.6 + ex * 0.5, -0.2)), V((ex * 10.6, YG + 6.4, -0.6))]
            K.tapered("horn%d" % ex, pts, 0.9, 0.08, P["bone2"], sides=6, sub=2, part="skull")
    for k in range(7):
        x = -2.0 + k * 0.66
        Lb.claw("tooth%d" % k, V((x, YG - 2.0, 3.0)), V((0.0, -1.0, 0.3)), 0.95 + 0.25 * (k % 2), 0.2, P["bone"], part="skull_teeth", sides=4)
    K.blob("nasal", (0.0, YG - 0.3, 3.7), (0.45, 0.6, 0.35), P["core"], seed=24, lump=0.02, rings=4, segs=6, part="skull")
    # ---------------------------------------------------------------- hilt: dark core, teal bandage cord, bone bands, a vertebra pommel, three wisps hanging
    hilt(P["core"], P["wrap"], P["bone"], P["bone"], seed=6, turns=9.0)
    for i in range(4):
        r_ = 1.9 - 0.1 * i
        Lb.lathe("vertebra%d" % i, [(r_ * 0.7, -0.6 - 1.3 * i), (r_, -0.9 - 1.3 * i), (r_ * 0.8, -1.3 - 1.3 * i), (r_ * 0.7, -1.6 - 1.3 * i)], P["bone"], "pommel_vert", sides=8, squash=0.9)
        Lb.claw("spine%d" % i, V((0.0, -1.1 - 1.3 * i, -r_ * 0.8)), V((0.0, -0.3, -1.0)), 1.8, 0.3, P["bone2"], part="pommel_vert", sides=4)
    K.blob("pomorb", (0.0, -7.2, 0.0), (1.4, 1.4, 1.4), P["orb"], seed=9, lump=0.04, rings=5, segs=8, part="pommel_orb")
    for k, (dx, ln, w, bend, mat) in enumerate(((0.0, 22.0, 2.8, 3.0, "wisp"), (-1.4, 15.0, 2.2, -3.4, "soul2"), (1.5, 18.0, 2.4, 2.4, "wisp"))):
        Lb.flame("wisp%d" % k, V((dx, -7.8, 0.4 * k - 0.4)), V((0.0, -1.0, 0.0)), ln, w, P[mat], bend=V((bend, 0, 0)), curl=0.8 * (1 if k % 2 else -1), facing=(0, 0, 1), seed=40 + k, part="wisps", rows=8)
    return K.quad_estimate()


# ================================================================ 3. SIDERITE KATANA
def sid_palette():
    P = {}
    P["iron"] = K.surface("sid_iron", rgb(78, 84, 98), 1.0, 0.32, wear=1.0, wear_color=rgb(210, 220, 235), dirt=0.6, dirt_color=(0.01, 0.01, 0.02, 1), scratches=0.8, bump="hammer", bump_scale=0.5,
                          bump_strength=0.2, mottle=0.8, mottle_color=rgb(40, 44, 56), fine=0.5, seed=301, bevel=0.2, ao_dist=2.0, veins=0.7, vein_color=rgb(190, 200, 215), wear_radius=0.55,
                          cramp=("y", 31.0, 108.0, [(0.0, rgb(60, 66, 80)), (0.5, rgb(92, 100, 118)), (1.0, rgb(110, 124, 150))]))
    P["crust"] = Lb.paint("sid_crust", rgb(46, 40, 42), rgb(150, 130, 120), 302, rough=0.85, dirt_col=(0.0, 0.0, 0.0, 1), mottle=rgb(96, 62, 46), bump="hammer")
    P["silver"] = Lb.metal("sid_silver", rgb(214, 220, 234), rgb(255, 255, 255), 303, rough=0.2, mottle=rgb(130, 150, 200), dirt=0.45, dirt_col=(0.02, 0.03, 0.06, 1))
    P["navy"] = Lb.metal("sid_navy", rgb(28, 36, 74), rgb(120, 140, 220), 304, rough=0.3, dirt=0.4, dirt_col=(0.0, 0.0, 0.02, 1), bump="hammer", bs=0.12)
    P["star"] = Lb.glow("sid_star", rgb(150, 200, 255), 1.0, 305, flicker=0.6, ramp=("y", 31.0, 108.0, [(0.0, rgb(120, 160, 255)), (0.6, rgb(170, 215, 255)), (1.0, rgb(230, 245, 255))]))
    P["gem"] = Lb.gem("sid_gem", rgb(110, 170, 255), 1.1, 306)
    P["wrap"] = Lb.cloth("sid_wrap", rgb(36, 46, 92), rgb(6, 8, 24), 307, mottle=rgb(70, 86, 160), bump="cross", scale=1.0)
    P["core"] = Lb.paint("sid_core", rgb(30, 30, 40), rgb(110, 120, 140), 308, bump="leather")
    P["cord"] = Lb.cloth("sid_cord", rgb(196, 204, 232), rgb(60, 70, 110), 309, mottle=rgb(120, 130, 180), bump="wrap", scale=1.0)
    return P


def siderite(opts):
    C.begin(opts)
    C.weights(blade=1.4, star=1.2, guard=1.1, studs=0.8, crust=0.7)
    P = sid_palette()
    rr = K.rng(7303)
    Y0, Y1 = 31.0, 108.0
    hw = lambda y: K.interp([(31, 4.8), (44, 5.3), (75, 5.1), (96, 4.7), (103, 3.9), (106, 1.9), (108, 0.4)], y)           # noqa: E731
    stations = [(y, hw(y)) for y in (31, 44, 75, 96, 103, 106, 108)]
    notL = [(50.0, 0.9, 3.0, "chip"), (58.0, 0.7, 2.0, "chip"), (68.0, 1.0, 3.4, "step"), (80.0, 0.8, 2.4, "chip"), (92.0, 0.9, 3.0, "step"), (101.0, 0.6, 2.0, "chip")]     # the spine is rough meteor crust
    notR = [(56.0, 0.5, 2.0, "chip"), (74.0, 0.6, 2.4, "chip"), (97.0, 0.5, 2.0, "chip")]
    xc = Lb.katana_blade("blade", P["iron"], Y0, Y1, stations, 3.6, notL=notL, notR=notR, seed=51, tipL=(-0.8, 108.8), tipR=(1.2, 107.4), thick=1.55, fuller=1.0, jit=0.07)
    # crust nodules on the spine: lumps of fallen rock fused to the metal
    for k in range(7):
        y = 46.0 + k * 9.0 + rr.uniform(-2, 2)
        K.blob("crust%d" % k, (xc(y) - hw(y) + rr.uniform(-0.2, 0.6), y, rr.uniform(-0.7, 0.7)), (rr.uniform(1.2, 2.2), rr.uniform(1.6, 3.0), rr.uniform(1.1, 1.8)), P["crust"], seed=80 + k, lump=0.28,
               lump_scale=0.14, rings=6, segs=9, part="crust")
    # star studs: four-point stars lying on both faces along the blade (glowing), a few little ones between
    prev = {1: None, -1: None}
    for s_ in (1, -1):
        for k in range(9 if s_ > 0 else 7):
            y = 40.0 + k * (7.4 if s_ > 0 else 9.0) + rr.uniform(-1, 1)
            x = xc(y) + rr.uniform(-1.2, 1.2) + (0.8 if s_ < 0 else 0.0)
            big = (k % 3 == 0)
            ro = 1.9 if big else 1.15
            th = 1.55 * (1.0 - 0.45 * ((y - Y0) / (Y1 - Y0)) ** 2.0)
            pts = Lb.star_outline(4, ro, ro * 0.32, x, y, phase=rr.uniform(0, 1.5), jit=0.1, rng_=rr)
            K.plate("stud%d_%d" % (s_, k), pts, s_ * (th - 0.08) if s_ > 0 else s_ * (th - 0.08) - 0.5, s_ * (th - 0.08) + (0.5 if s_ > 0 else 0.0), P["star"], part="studs", bevel_w=0.0)
    for s_ in (1, -1):
        hab = [(-5.0, 30.6), (5.2, 30.6), (5.6, 40.0), (3.8, 44.0), (-3.6, 44.0), (-5.6, 40.0)]
        K.stepped_plate("habaki%d" % s_, hab, 1.6 * s_, P["silver"], steps=2, step_h=0.6, inset=0.9, part="habaki", up=s_, bevel_w=0.0)
    # ---------------------------------------------------------------- guard: an eight-point star plate (three steps, both faces), a gem hub, rivets, and an orbit ring behind it with three bead-stars
    star = Lb.star_outline(8, 13.0, 5.6, 0.0, YG, phase=math.pi / 8 + 0.07, jit=0.14, rng_=rr)
    for s_ in (1, -1):
        K.stepped_plate("star%d" % s_, star, 0.6 * s_, P["silver"], steps=3, step_h=0.7, inset=1.1, part="star", up=s_, bevel_w=0.0)
        K.gem_setting("hub%d" % s_, V((0, YG, 2.5 * s_ + 0.0)), 2.6, P["gem"], P["navy"], normal=(0, 0, s_), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
        K.rivets("star_rv%d" % s_, [(V((6.8 * math.cos(a), YG + 6.8 * math.sin(a), 2.65 * s_)), (0, 0, s_)) for a in [TAU * k / 8 + 0.2 for k in range(8)]], P["navy"], r=0.55, rng_=rr, jitter=0.15, detail=False)
    Lb.ring_torus("orbit", (0.0, YG, -2.9), 16.0, 0.5, P["navy"], "orbit", plane="xy", sides=5, segs=30)
    for k, ang in enumerate((25, 140, 265)):
        a = math.radians(ang)
        K.blob("bead%d" % k, (16.0 * math.cos(a), YG + 16.0 * math.sin(a), -2.9), (1.3, 1.3, 1.3), P["star"], seed=90 + k, lump=0.05, rings=5, segs=8, part="orbit_beads")
    # ---------------------------------------------------------------- hilt: navy cord over a dark core, silver bands, an eight-point star kashira standing on edge, a beaded star cord
    hilt(P["core"], P["wrap"], P["silver"], P["navy"], wrap2=P["cord"], seed=8, turns=8.0, bands=(2.6, 14.5, 26.6))
    st2 = Lb.star_outline(6, 6.4, 2.6, 0.0, 1.0, phase=0.2, jit=0.1, rng_=rr)
    for s_ in (1, -1):
        K.stepped_plate("kstar%d" % s_, st2, 0.3 * s_, P["silver"], steps=2, step_h=0.5, inset=0.6, part="pommel_star", up=s_, bevel_w=0.0)
    cp = [V(p) for p in ((0.0, -4.0, 0.0), (1.4, -8.0, 0.8), (2.0, -13.0, 0.0), (0.6, -18.0, -0.8))]
    K.tapered("starcord", cp, 0.36, 0.26, P["cord"], sides=5, sub=3, part="pommel_cord")
    for k, p in enumerate(cp[1:]):
        K.blob("cbead%d" % k, p, (0.9, 0.9, 0.9), P["star"], seed=95 + k, lump=0.04, rings=4, segs=6, part="pommel_cord")
    # stardust: bounded glints streaming off the edge
    K.shard_cloud("dust", (xc(70) + 8.0, 72.0, 0.0), (6.0, 36.0, 4.0), 8, (0.55, 1.0), P["star"], rr, avoid=lambda p: p.x < xc(max(31, min(108, p.y))) + hw(max(31, min(108, p.y))) + 1.2,
                  part="dust", stretch=2.4, sides=4, max_count=8, flow=lambda p: V((0.25, 1.0, rr.uniform(-0.2, 0.2))))
    return K.quad_estimate()


# ================================================================ 4. SAKURA STONE OF DESTINY KATANA
def sakura_palette():
    P = {}
    P["stone"] = K.surface("sak_stone", rgb(255, 160, 196), 0.0, 0.1, mottle=0.9, mottle_color=rgb(255, 225, 238), seed=401, bevel=0.1,
                           rim=(rgb(255, 235, 245), 0.5), scratches=0.6, veins=0.5, vein_color=rgb(255, 250, 252), vein_strength=0.0, wear=0.8, wear_color=rgb(255, 245, 250), wear_metal=0.0,
                           cramp=("y", 31.0, 105.0, [(0.0, rgb(230, 110, 160)), (0.55, rgb(255, 160, 198)), (1.0, rgb(255, 205, 224))]))
    P["rose"] = Lb.hammered("sak_rose", rgb(232, 158, 138), rgb(255, 236, 220), 402, mottle=rgb(170, 100, 90), dirt_col=(0.05, 0.02, 0.015, 1))
    P["silver"] = Lb.metal("sak_silver", rgb(232, 226, 232), rgb(255, 255, 255), 403, rough=0.22, mottle=rgb(210, 170, 190), dirt=0.4, dirt_col=(0.05, 0.02, 0.03, 1))
    P["petal"] = K.surface("sak_petal", rgb(255, 196, 214), 0.0, 0.6, tramp=[(0.0, rgb(255, 238, 244)), (0.6, rgb(255, 190, 212)), (1.0, rgb(255, 142, 184))], wear=0.4, wear_color=rgb(255, 250, 252),
                           wear_metal=0.0, mottle=0.7, mottle_color=rgb(255, 120, 170), seed=404, bevel=0.0, fine=0.3)
    P["petal2"] = K.surface("sak_petal2", rgb(255, 150, 190), 0.0, 0.6, tramp=[(0.0, rgb(255, 220, 232)), (0.6, rgb(255, 150, 190)), (1.0, rgb(226, 96, 150))], wear=0.4, wear_color=rgb(255, 245, 250),
                            wear_metal=0.0, mottle=0.7, mottle_color=rgb(210, 80, 140), seed=405, bevel=0.0, fine=0.3)
    P["heart"] = Lb.glow("sak_heart", rgb(255, 214, 120), 0.9, 406, flicker=0.4)
    P["stamen"] = Lb.paint("sak_stamen", rgb(255, 232, 190), rgb(255, 255, 240), 407, rough=0.5)
    P["bark"] = K.surface("sak_bark", rgb(74, 48, 44), 0.0, 0.8, bump="grain", bump_scale=1.4, bump_strength=0.7, groove=rgb(22, 12, 10), groove_amount=0.8, dirt=0.7, mottle=0.8, mottle_color=rgb(120, 86, 76),
                          fine=0.6, seed=408, bevel=0.0)
    P["leaf"] = Lb.paint("sak_leaf", rgb(96, 150, 84), rgb(190, 230, 170), 409, rough=0.6, mottle=rgb(56, 110, 60))
    P["gem"] = Lb.gem("sak_gem", rgb(255, 120, 176), 1.3, 410)
    P["wrap"] = Lb.cloth("sak_wrap", rgb(236, 130, 164), rgb(88, 20, 44), 411, mottle=rgb(255, 190, 210), bump="wrap", scale=1.0)
    P["core"] = Lb.paint("sak_core", rgb(86, 56, 48), rgb(170, 130, 110), 412, bump="grain")
    P["cord"] = Lb.cloth("sak_cord", rgb(255, 214, 224), rgb(150, 90, 110), 413, mottle=rgb(255, 160, 190), bump="wrap", scale=1.0)
    return P


def sakura(opts):
    C.begin(opts)
    C.weights(blade=1.3, flower=1.15, blossoms=1.0, petals=0.9, branch=0.9)
    P = sakura_palette()
    rr = K.rng(7404)
    Y0, Y1 = 31.0, 104.5
    hw = lambda y: K.interp([(31, 3.9), (40, 4.2), (80, 3.8), (95, 3.5), (100, 2.6), (103, 1.2), (104.5, 0.3)], y)        # noqa: E731
    stations = [(y, hw(y)) for y in (31, 40, 80, 95, 100, 103, 104.5)]
    notR = [(y, 0.5, 2.6, "chip") for y in (47, 54, 61, 68, 75, 82, 89)]                       # a wavy, rippling edge, like a hamon turned into the outline
    notL = [(56.0, 0.6, 2.2, "chip"), (84.0, 0.5, 2.0, "chip")]
    xc = Lb.katana_blade("blade", P["stone"], Y0, Y1, stations, 4.6, notL=notL, notR=notR, seed=61, tipL=(-0.2, 105.0), tipR=(0.9, 104.0), thick=1.1, fuller=1.0, res=8.0)
    P["line"] = Lb.glow("sak_line", rgb(255, 170, 210), 0.9, 414, flicker=0.7, ramp=("y", 31.0, 105.0, [(0.0, rgb(255, 120, 180)), (0.6, rgb(255, 190, 220)), (1.0, rgb(255, 235, 245))]))
    for s_ in (1, -1):                                                                          # a glowing line of light down the stone's groove
        rows = []
        for y in range(40, 100, 4):
            c = V((xc(y) - hw(y) * 0.34, y, 0.0))
            w = 0.34 * (1.0 - 0.55 * ((y - 40) / 60.0))
            rows.append((c - V((w, 0, 0)), c + V((w, 0, 0))))
        K.strip("line%d" % s_, rows, P["line"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), lambda t: 0.5 * (1 - 0.3 * t), part="blade_glow", up=(0, 0, s_))
    for s_ in (1, -1):
        hab = [(-4.0, 30.6), (4.2, 30.6), (4.4, 38.0), (2.4, 41.0), (-2.4, 41.0), (-4.2, 38.0)]
        K.stepped_plate("habaki%d" % s_, hab, 1.0 * s_, P["rose"], steps=2, step_h=0.5, inset=0.8, part="habaki", up=s_, bevel_w=0.0)
    # petals grow from the spine like a leafy ridge (alternating faces), blossoms sit on the flats
    for k in range(9):
        y = 40.0 + k * 7.2 + rr.uniform(-1, 1)
        zs = 1 if k % 2 else -1
        ln = rr.uniform(6.0, 9.5) * (1.0 - 0.03 * k)
        Lb.petal("ridge%d" % k, V((xc(y) - hw(y) + 0.3, y, zs * 0.5)), V((-0.55, 0.8, zs * 0.28)), ln, rr.uniform(3.4, 4.6), P["petal"] if k % 2 else P["petal2"], facing=(0, 0, zs),
                 curl=zs * 1.4, cup=0.3, thick=0.12, part="petals", seed=k, rows=5, simple=True)
    for k, (y, zs, sz) in enumerate(((50.0, 1, 5.4), (72.0, -1, 4.8), (92.0, 1, 4.2))):
        th = 1.1 * (1.0 - 0.45 * ((y - Y0) / (Y1 - Y0)) ** 2.0)
        Lb.blossom("bl%d" % k, V((xc(y) + 0.4, y, zs * (th + 0.15))), V((0.0, 0.0, zs)), sz, P["petal"] if k % 2 else P["petal2"], P["heart"], P["stamen"], rr, part="blossoms", up=(0, 1, 0))
    # ---------------------------------------------------------------- guard: a five-petal sakura flower plate (notched petals, veins), a gem heart, no petal straight down
    for k in range(5):
        ang = 8.0 + 72.0 * k + rr.uniform(-5, 5)
        a = math.radians(ang)
        ln = (13.0 if k != 4 else 6.5) * rr.uniform(0.9, 1.05)
        for s_ in (1, -1):
            Lb.petal("gp%d_%d" % (k, s_), V((3.0 * math.cos(a), YG + 3.0 * math.sin(a), s_ * (0.4 + 0.25 * (k % 2)))), V((math.cos(a), math.sin(a), s_ * 0.12)), ln, 8.0 if k != 4 else 5.5,
                     P["petal"] if (k + (s_ > 0)) % 2 else P["petal2"], facing=(0, 0, s_), curl=s_ * 1.6, cup=0.3, thick=0.28, part="flower", seed=k * 3 + (s_ > 0), rows=6)
    for s_ in (1, -1):
        K.gem_setting("heart%d" % s_, V((0, YG, 1.8 * s_)), 2.4, P["gem"], P["rose"], normal=(0, 0, s_), claws=5, sides=8, part="gem", rng_=rr, claw_r=0.5)
        for k in range(6):
            a = TAU * k / 6 + 0.2
            p0 = V((3.4 * math.cos(a), YG + 3.4 * math.sin(a), s_ * 2.2))
            K.tapered("stam%d_%d" % (s_, k), [p0, p0 + V((1.6 * math.cos(a), 1.6 * math.sin(a), s_ * 1.2)), p0 + V((2.6 * math.cos(a), 2.6 * math.sin(a), s_ * 1.9))], 0.12, 0.05, P["stamen"], sides=3, sub=1,
                      part="flower_stamens")
    # ---------------------------------------------------------------- hilt: pink cord over a wood core, a cherry branch winding up it with blossoms and leaves
    hilt(P["core"], P["wrap"], P["rose"], P["rose"], wrap2=P["cord"], seed=9, turns=8.0)
    bp = []
    for i in range(18):
        t = i / 17.0
        a = TAU * 2.1 * t + 0.4
        bp.append(V((2.5 * math.cos(a), 3.0 + 25.0 * t, 2.5 * math.sin(a) * 0.9)))
    K.wood_branch("branch", bp, 0.62, 0.3, P["bark"], seed=4, sides=6, knots=2, part="branch", sub=2)
    for k, t in enumerate((0.38, 0.85)):
        a = TAU * 2.1 * t + 0.4
        p = V((2.7 * math.cos(a), 3.0 + 25.0 * t, 2.7 * math.sin(a) * 0.9))
        Lb.blossom("hb%d" % k, p, V((math.cos(a), 0.3, math.sin(a))), 3.6 - 0.3 * k, P["petal"] if k % 2 else P["petal2"], P["heart"], P["stamen"], rr, part="blossoms", rows=3)
        if k % 2 == 0:
            Lb.petal("leaf%d" % k, p, V((math.cos(a + 0.9), 0.6, math.sin(a + 0.9))), 3.6, 1.8, P["leaf"], facing=(0, 1, 0), curl=0.5, cup=0.2, thick=0.08, part="leaves", seed=k, notch=False, rows=4)
    # pommel: a pink stone of destiny in a ring of five petals, and a long cord with petals
    K.gem_setting("destiny", V((0, -1.2, 0.0)), 2.3, P["gem"], P["rose"], normal=(0, -1, 0), claws=5, sides=8, part="pommel_gem", rng_=rr, up=(0, 0, 1), claw_r=0.5)
    for k in range(5):
        a = TAU * k / 5 + 0.3
        Lb.petal("pp%d" % k, V((1.8 * math.cos(a), 0.4, 1.8 * math.sin(a))), V((math.cos(a) * 0.8, -0.65, math.sin(a) * 0.8)), 4.2, 2.6, P["petal"] if k % 2 else P["petal2"], facing=(0, 1, 0), curl=0.8,
                 cup=0.25, thick=0.1, part="pommel_petals", seed=k, rows=4, simple=True)
    cp = [V(p) for p in ((0.0, -3.2, 0.0), (1.0, -7.0, 0.8), (2.4, -12.0, 0.2), (1.6, -17.0, -0.8), (1.2, -21.0, -0.2))]
    K.tapered("cord", cp, 0.34, 0.24, P["cord"], sides=5, sub=3, part="pommel_cord")
    for k, p in enumerate(cp[1:]):
        Lb.petal("cp%d" % k, p, V((0.5 * (1 if k % 2 else -1), -1.0, 0.1)), 3.0, 2.0, P["petal2"] if k % 2 else P["petal"], facing=(0, 0, 1), curl=0.6, cup=0.25, thick=0.08, part="pommel_cord", seed=k + 9, rows=4, simple=True)
    # drifting petals (bounded: 9)
    for k in range(7):
        side = 1 if k % 2 else -1
        y = 46.0 + k * 6.2 + rr.uniform(-2, 2)
        p = V((xc(y) + side * rr.uniform(8.0, 15.0), y, rr.uniform(-5, 5)))
        Lb.petal("fl%d" % k, p, V((rr.uniform(-0.5, 0.5), rr.uniform(0.4, 1.0), rr.uniform(-0.5, 0.5))), 3.2, 2.2, P["petal"] if k % 2 else P["petal2"], facing=(rr.uniform(-0.3, 0.3), 0.2, 1.0), curl=0.7, cup=0.3,
                 thick=0.08, part="floats", seed=70 + k, rows=4, simple=True)
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["tiger"] = (white_tiger, "vassal_katana__white_tiger_katana", "white_tiger_katana")
BUILDERS["soul"] = (soul_eater, "vassal_katana__soul_eater_katana", "soul_eater_katana")
BUILDERS["siderite"] = (siderite, "vassal_katana__siderite_katana", "siderite_katana")
BUILDERS["sakura"] = (sakura, "vassal_katana__sakura_stone_katana", "sakura_stone_katana")

if __name__ == "__main__":
    main(sys.argv[1:], BUILDERS, BASE, "katana", MANIFEST)

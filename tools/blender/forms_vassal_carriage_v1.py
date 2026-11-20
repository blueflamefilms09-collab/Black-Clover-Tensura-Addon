"""Weapon-form models of the VASSAL CARRIAGE (base vassal_carriage, size class handitem), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_carriage.py --only filolial [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids: vassal_carriage__filolial_carriage, __war_carriage, __hourglass_sand_carriage, __queens_chariot.
Mesh space of the base carriage (vassal_b.py): +y the nose (f forward, 0..120), -x up (u = -x, the ground at x = 0), z the width (wheels at z = +-33, centre f 38, u 20.4),
the draught pole tip at f 119.9.  Every form keeps that frame, the wheel stance and the size class.  Designs: Filolial Carriage = a light open carriage under a roof of
shingled feathers with a filolial head on the pole, wings over the wheels and a plume tail; War Carriage = an iron-plated ram on spiked wheels; Hourglass Sand Carriage =
a cabin that is a giant hourglass on clockwork gear wheels with a portal ring at the back; Filolial Queen's Chariot = an open golden chariot with great spread wings, a crown and wing-spoked wheels.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_vassal_common_b as C  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import TAU, rgb  # noqa: E402
from forms_vassal_common_b import V  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
C.setup("vassal_carriage", "handitem", os.path.join(HERE, "forms_manifest_vassal_carriage.json"), "vassal_carriage", glow=("FFFFFF", 0.12, 0.45, 1.0))
mat, glowmat = C.mat, C.glowmat
WU, WF = 20.4, 38.0                                                                              # wheel centre (up, forward), as in the base carriage


def Cc(u, f, s):
    """Carriage coordinates (u up, f forward, s side) to mesh space."""
    return V(-u, f, s)


def slab(name, u0, u1, f0, f1, s0, s1, material, part, bevel=0.2):
    return K.plate(name, [(-u0, f0), (-u1, f0), (-u1, f1), (-u0, f1)], min(s0, s1), max(s0, s1), material, part=part, bevel_w=bevel)


def side_plate(name, outline_uf, s0, s1, material, part, bevel=0.2):
    """A plate in the (u, f) plane, from side s0 to s1: outline [(f, u)] as in the base hull table."""
    return K.plate(name, [(-u, f) for f, u in outline_uf], min(s0, s1), max(s0, s1), material, part=part, bevel_w=bevel)


def wheel(name, s0, sg, R, tyre, hub, spoke, *, spokes=10, hub_r=4.4, spoke_w=0.9, rim_t=1.6, rr=None, part="wheels", spoke_fn=None):
    """A wheel in the plane z = s0 (axis z) at the base's wheel centre: a tyre, a felloe, spokes (bars, or spoke_fn(angle) for custom ones) and a hub with a boss."""
    ctr = tuple(Cc(WU, WF, s0))
    K.bl.lathe(name + "_tyre", [(R - rim_t * 1.5, -1.9), (R, -1.5), (R + 0.3, 0.0), (R, 1.5), (R - rim_t * 1.5, 1.9)], tyre, sides=26, origin=ctr, axis="z", part=part, caps=(False, False))
    K.bl.lathe(name + "_felloe", [(R - rim_t * 1.5 - 0.1, -1.2), (R - rim_t * 2.6, -1.2), (R - rim_t * 2.6, 1.2), (R - rim_t * 1.5 - 0.1, 1.2)], spoke, sides=22, origin=ctr, axis="z", part=part, caps=(False, False))
    for k in range(spokes):
        a = TAU * k / spokes
        if spoke_fn:
            spoke_fn(name, k, a, ctr)
            continue
        K.bar("%s_sp%d" % (name, k), Cc(WU, WF, s0) + V(-hub_r * 0.6 * math.cos(a), hub_r * 0.6 * math.sin(a), 0), Cc(WU, WF, s0) + V(-(R - rim_t * 2.4) * math.cos(a), (R - rim_t * 2.4) * math.sin(a), 0),
              spoke_w, 0.9, spoke, part=part, taper=0.75, bevel_w=0.0, up=(0, 0, 1))
    K.bl.lathe(name + "_hub", [(0.01, -2.2), (hub_r * 0.8, -2.0), (hub_r, -1.0), (hub_r, 1.6), (hub_r * 0.7, 2.6), (0.01, 3.0)], hub, sides=12, origin=ctr, axis="z", part=part + "_hub")


def axle(P, mat_, s=30.0, r=1.9):
    K.bl.lathe("axle", [(r, -s), (r, s)], mat_, sides=8, origin=tuple(Cc(WU, WF, 0.0)), axis="z", caps=(False, False), part="axle")


# ================================================================ 1. FILOLIAL CARRIAGE
def filolial_palette():
    P = {}
    P["cream"] = mat("fc_cream", (246, 240, 224), 0.0, 0.5, bump="grain", bump_scale=0.5, bump_strength=0.25, groove=rgb(200, 188, 160), groove_amount=0.6, dirt=0.5, wear=0.5, wear_color=rgb(255, 255, 250), wear_metal=0.0)
    P["pink"] = mat("fc_pink", (236, 140, 170), 0.0, 0.6, bump="cross", bump_scale=1.0, bump_strength=0.5, groove=rgb(150, 60, 90), groove_amount=0.6, dirt=0.5, wear=0.4, wear_metal=0.0, wear_color=rgb(255, 210, 225))
    P["gold"] = mat("fc_gold", (244, 196, 80), 1.0, 0.28, bump="hammer", bump_scale=0.7, bump_strength=0.12, dirt=0.5, mottle_color=rgb(170, 110, 40))
    P["wood"] = mat("fc_wood", (206, 160, 104), 0.0, 0.5, bump="grain", bump_scale=0.6, bump_strength=0.4, groove=rgb(120, 80, 40), groove_amount=0.6, dirt=0.6, wear=0.4, wear_metal=0.0)
    P["f1"] = mat("fc_f1", (246, 240, 236), 0.0, 0.55, wear=0.3, wear_metal=0.0, dirt=0.3, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(214, 170, 190)), (0.5, rgb(250, 244, 240)), (1.0, rgb(255, 196, 214))],
                  temit=[(0.0, rgb(0, 0, 0)), (0.7, rgb(40, 10, 24)), (1.0, rgb(255, 120, 170))], mottle=0.5, mottle_color=rgb(230, 190, 205), bevel=0.06, fine=0.4)
    P["f2"] = mat("fc_f2", (244, 150, 184), 0.0, 0.55, wear=0.3, wear_metal=0.0, dirt=0.3, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(180, 80, 120)), (0.5, rgb(244, 150, 184)), (1.0, rgb(255, 232, 160))],
                  temit=[(0.0, rgb(0, 0, 0)), (0.7, rgb(40, 20, 6)), (1.0, rgb(255, 210, 120))], mottle=0.5, mottle_color=rgb(200, 100, 140), bevel=0.06, fine=0.4)
    P["beak"] = mat("fc_beak", (250, 196, 64), 0.0, 0.4, bump="hammer", bump_strength=0.1, dirt=0.4, wear_metal=0.0)
    P["eye"] = glowmat("fc_eye", (255, 150, 190), 1.0, 0.3)
    P["lit"] = glowmat("fc_lit", (255, 160, 200), 0.9, 0.6)
    return P


def filolial_carriage(opts):
    C.begin(opts)
    C.weights(feathers=1.2, wings=1.2, plume=1.1, wheels=1.0, head=1.2, hull=1.0, roof=1.1)
    P = filolial_palette()
    rr = K.rng(7201)
    # wheels: light cream rims, slim spokes, a tuft of feathers on each hub, an axle
    for nm, s0, sg in (("wl", -33.0, -1), ("wr", 33.0, 1)):
        wheel(nm, s0, sg, 19.4, P["gold"], P["gold"], P["cream"], spokes=10, spoke_w=0.7, rr=rr)
        for k in range(6):
            a = TAU * k / 6 + 0.3
            ctr = Cc(WU, WF, s0 + 3.2 * sg)
            d = V(-math.cos(a), math.sin(a), 0.5 * sg)
            K.feather("%s_tuft%d" % (nm, k), ctr, d, 9.0, 3.6, P["f1"] if k % 2 else P["f2"], rows=5, serrate=0.15, facing=(0, 0, sg), part="tufts", seed=20 + k, thick=0.12, curl=0.8 * sg)
    axle(P, P["wood"])
    # a slim hull: floor, two low swept sides with a gold edge, a rear back-rest and a dash
    slab("floor", 12.5, 14.4, 0.4, 86.0, -25.0, 25.0, P["wood"], "hull")
    top = [(0.0, 34.0), (10.0, 32.0), (22.0, 26.0), (34.0, 24.0), (48.0, 27.0), (62.0, 33.0), (76.0, 36.0), (87.1, 33.0)]
    for sg in (1, -1):
        outl = [(0.0, 12.3), (87.1, 12.3)] + [(f, u) for f, u in reversed(top)]
        side_plate("side%d" % sg, outl, 24.8 * sg, 26.6 * sg, P["cream"], "hull")
        K.tube("sidetrim%d" % sg, [Cc(u + 0.6, f, 26.9 * sg) for f, u in top], 0.75, P["gold"], sides=6, sub=3, part="trim")
        for k, (f, u) in enumerate(top[3:5]):
            K.gem_setting("hl%d_%d" % (sg, k), Cc(u - 4.0, f, 27.0 * sg), 1.1, P["lit"], P["gold"], normal=(0, 0, sg), claws=4, sides=6, part="lamps", rng_=rr, bezel=False, claw_r=0.4)
    for nm, f0, f1, back in (("bench_front", 54.0, 70.0, 27.0), ("bench_rear", 9.6, 27.0, 33.0)):
        half = 12.0 if "front" in nm else 23.0
        slab(nm, 14.4, 19.0, f0, f1, -half, half, P["pink"], "seats", bevel=0.9)
        slab(nm + "_back", 17.0, back, f0 - 1.6, f0 + 0.8, -half, half, P["pink"], "seats", bevel=0.9)
    # roof of shingled feathers: rows from front to back, each side a layer of feathers lying outward and down, two layers deep
    for r, f in enumerate((6.0, 18.0, 30.0, 42.0, 54.0, 66.0, 78.0)):
        for sg in (1, -1):
            for layer in range(2):
                base = Cc(64.0 - 2.0 * layer + 0.6 * (r % 2), f + 5.0 * layer, 0.5 * sg)
                K.feather("roof%d_%d_%d" % (r, sg, layer), base, V(0.30 + 0.08 * layer, 0.04 * (1 - 2 * (r % 2)), sg), 34.0 - 2.0 * layer, 13.0, P["f1"] if (r + layer) % 2 == 0 else P["f2"], rows=6, serrate=0.14,
                          facing=(-1, 0, 0), part="roof", seed=100 + r * 7 + layer + (3 if sg < 0 else 0), thick=0.14, curl=-1.0, droop=-1.2)
    K.tube("ridge", [Cc(66.0, 2.0, 0.0), Cc(67.5, 44.0, 0.0), Cc(66.0, 88.0, 0.0)], 1.3, P["gold"], sides=6, sub=4, part="trim")
    for f, s in ((6.0, 24.0), (6.0, -24.0), (74.0, 24.0), (74.0, -24.0)):
        K.tube("post%d_%d" % (f, s), [Cc(14.4, f, s), Cc(40.0, f, s * 1.01), Cc(63.0, f, s * 0.7)], 0.9, P["gold"], sides=6, sub=2, part="posts")
    # wings over the wheels: a fan of five feathers each side, sweeping back and up
    for sg in (1, -1):
        for k in range(6):
            a = math.radians(25.0 + 24.0 * k)
            d = V(-math.sin(a) * 0.8, -math.cos(a) * 0.75 + 0.15, sg * (0.5 + 0.4 * math.cos(a * 1.2)))
            K.feather("wing%d_%d" % (sg, k), Cc(40.0 + 1.0 * k, 34.0 - 3.0 * k * 0.0 + 2.0, 28.5 * sg), d, 26.0 + 4.0 * (k % 3) - 2.0 * (k > 3), 9.0 - 0.4 * k, P["f1"] if k % 2 == 0 else P["f2"], rows=7, serrate=0.18,
                      facing=(0, 0, sg), part="wings", seed=140 + k + (7 if sg < 0 else 0), thick=0.14, curl=1.2 * sg, bend=V(0, -2.0, 2.0 * sg))
    # plume tail rising from the rear: a fan of feathers in the plane of the carriage
    for k in range(7):
        a = math.radians(-8.0 + 17.0 * k)
        d = V(-math.cos(a), -math.sin(a) * 0.9 - 0.05, 0.0)
        K.feather("tail%d" % k, Cc(30.0, 3.0, 0.4 * (k % 2) - 0.2), d, 26.0 + 3.0 * (3 - abs(3 - k)), 8.6, P["f2"] if k % 2 else P["f1"], rows=8, serrate=0.18, facing=(0, 0, 1), part="plume", seed=160 + k, thick=0.14, curl=1.5, bend=V(0, -3.0, 0))
    # the pole and, at its tip, a filolial's head on a curved neck: a beak, glowing eyes, a crest
    K.tube("pole", [Cc(11.0, 78.0, 0), Cc(13.5, 90.0, 0), Cc(19.5, 103.0, 0), Cc(26.0, 112.0, 0)], 1.8, P["wood"], sides=8, sub=3, taper=(2.0, 1.6), part="pole")
    K.tapered("neck", [Cc(24.0, 108.0, 0), Cc(32.0, 112.0, 0), Cc(42.0, 111.0, 0), Cc(50.0, 116.0, 0)], 3.4, 2.4, P["f1"], sides=8, sub=3, part="head")
    for k in range(5):                                                                              # neck ruff: little feathers along the neck
        t = 0.2 + 0.18 * k
        K.feather("ruff%d" % k, Cc(26.0 + 24.0 * t, 109.0 + 7.0 * t, 0.0), V(-0.3, -0.5, 1.0 if k % 2 else -1.0), 6.0, 3.0, P["f2"], rows=4, serrate=0.1, facing=(0, 1, 0), part="head", seed=200 + k, thick=0.1)
    K.blob("head", Cc(53.0, 119.0, 0.0), (4.4, 5.6, 4.2), P["f1"], seed=3, lump=0.05, rings=8, segs=12, part="head")
    K.tapered("beak", [Cc(53.0, 123.0, 0.0), Cc(52.5, 128.0, 0.0), Cc(51.0, 132.0, 0.0)], 2.6, 0.2, P["beak"], sides=6, sub=2, part="head")
    for sg in (1, -1):
        K.blob("eye%d" % sg, Cc(55.0, 120.0, 3.4 * sg), (1.0, 1.0, 1.0), P["eye"], seed=sg + 4, lump=0.0, rings=4, segs=6, part="eyes")
    for k in range(3):
        K.feather("crest%d" % k, Cc(57.0, 117.0, 0.0), V(-0.8, -0.4 - 0.3 * k, (k - 1) * 0.5), 11.0 - 2.0 * k, 3.2, P["f2"], rows=5, serrate=0.12, facing=(0, 0, 1), part="head", seed=210 + k, thick=0.1, curl=1.0)
    return K.quad_estimate()


# ================================================================ 2. WAR CARRIAGE
def war_palette():
    P = {}
    P["iron"] = mat("wc_iron", (70, 74, 84), 1.0, 0.42, bump="hammer", bump_scale=0.6, bump_strength=0.4, groove=rgb(20, 22, 28), groove_amount=0.7, dirt=0.9, scratches=0.7, mottle=0.8, mottle_color=rgb(96, 50, 30),
                    wear_color=rgb(190, 196, 206), wear_radius=0.5)
    P["plate"] = mat("wc_plate", (104, 108, 118), 1.0, 0.36, bump="brushed", bump_strength=0.2, dirt=0.85, scratches=0.8, mottle=0.8, mottle_color=rgb(120, 60, 30), wear_color=rgb(210, 214, 222))
    P["wood"] = mat("wc_wood", (104, 68, 38), 0.0, 0.7, bump="grain", bump_scale=0.5, bump_strength=0.7, groove=rgb(36, 20, 10), groove_amount=0.8, dirt=0.8, wear=0.45, wear_metal=0.0)
    P["brass"] = mat("wc_brass", (176, 128, 54), 1.0, 0.36, bump="hammer", bump_strength=0.16, dirt=0.8)
    P["chain"] = mat("wc_chain", (120, 126, 138), 1.0, 0.36, bump="hammer", bump_strength=0.2, dirt=0.9, scratches=0.6)
    P["cloth"] = mat("wc_cloth", (150, 30, 30), 0.0, 0.7, bump="cross", bump_scale=1.2, bump_strength=0.5, dirt=0.8, wear=0.5, wear_metal=0.0, bevel=0.0, mottle=0.9, mottle_color=rgb(60, 10, 10))
    P["ember"] = glowmat("wc_ember", (255, 130, 40), 1.0, 0.8)
    P["lit"] = glowmat("wc_lit", (255, 170, 70), 0.85, 0.6)
    P["slit"] = mat("wc_slit", (8, 6, 8), 0.0, 0.8, dirt=0.0, wear=0.0, bevel=0.0)
    return P


def war_carriage(opts):
    C.begin(opts)
    C.weights(plates=1.1, ram=1.2, wheels=1.0, chains=0.9, banner=0.9, roof=1.0, spikes=1.0)
    P = war_palette()
    rr = K.rng(7202)

    def spiked(name, k, a, ctr):                                                                    # a plank spoke carrying an iron blade at the rim
        K.bar("%s_sp%d" % (name, k), V(ctr[0] - 4.0 * math.cos(a), ctr[1] + 4.0 * math.sin(a), ctr[2]), V(ctr[0] - 15.5 * math.cos(a), ctr[1] + 15.5 * math.sin(a), ctr[2]), 1.5, 1.4, P["wood"], part="wheels", taper=0.8, bevel_w=0.0)
    for nm, s0, sg in (("wl", -33.0, -1), ("wr", 33.0, 1)):
        wheel(nm, s0, sg, 19.4, P["iron"], P["iron"], P["wood"], spokes=8, hub_r=5.0, rr=rr, spoke_fn=spiked)
        for k in range(8):                                                                          # long iron spikes on the rim
            a = TAU * k / 8 + 0.2
            p0 = Cc(WU, WF, s0) + V(-18.4 * math.cos(a), 18.4 * math.sin(a), 0.0)
            K.tapered("%s_spike%d" % (nm, k), [p0, p0 + V(-5.0 * math.cos(a), 5.0 * math.sin(a), 1.0 * sg), p0 + V(-10.5 * math.cos(a), 10.5 * math.sin(a), 1.4 * sg)], 1.7, 0.1, P["plate"], sides=4, sub=1, part="spikes")
        for k in range(4):                                                                          # hub blades pointing out sideways
            a = TAU * k / 4 + 0.7
            p0 = Cc(WU, WF, s0 + 2.8 * sg)
            K.tapered("%s_hubblade%d" % (nm, k), [p0, p0 + V(-2.0 * math.cos(a), 2.0 * math.sin(a), 3.0 * sg), p0 + V(-3.0 * math.cos(a), 3.0 * math.sin(a), 7.5 * sg)], 1.4, 0.1, P["plate"], sides=4, sub=1, part="spikes")
    axle(P, P["iron"], r=2.4)
    # the hull: a plank floor, two iron-plated sloped sides in layers with rivets, arrow slits, a rear door
    slab("floor", 11.0, 13.6, 0.4, 86.0, -26.0, 26.0, P["wood"], "hull")
    slab("chassis", 5.0, 11.0, 0.4, 84.0, -22.0, 22.0, P["iron"], "hull")
    top = [(0.0, 48.0), (14.0, 48.0), (30.0, 44.0), (60.0, 44.0), (78.0, 40.0), (87.1, 34.0)]
    for sg in (1, -1):
        outl = [(0.0, 11.0), (87.1, 11.0)] + [(f, u) for f, u in reversed(top)]
        for layer in range(3):
            inset = 1.8 * layer
            o2 = [(f, u - (inset if u > 20 else 0.0) * 0.8) for f, u in outl]
            side_plate("hull%d_%d" % (sg, layer), o2, (26.0 + 1.1 * layer) * sg, (26.0 + 1.1 * (layer + 1)) * sg, P["plate"] if layer % 2 == 0 else P["iron"], "plates", bevel=0.25)
        for k, (f, u) in enumerate(((14.0, 30.0), (30.0, 32.0), (46.0, 32.0), (62.0, 30.0))):                      # arrow slits (dark, glowing from within)
            slab("slit%d_%d" % (sg, k), u, u + 7.0, f, f + 2.2, 29.2 * sg, 29.8 * sg, P["slit"], "slits", bevel=0.0)
            slab("slitglow%d_%d" % (sg, k), u + 1.0, u + 6.0, f + 0.5, f + 1.7, 29.0 * sg - 0.1, 29.4 * sg + 0.1, P["ember"], "slit_glow", bevel=0.0)
        rivs = [(Cc(u, f, 29.6 * sg), (0, 0, sg)) for f in (4.0, 22.0, 38.0, 54.0, 70.0, 82.0) for u in (16.0, 27.0, 40.0)]
        K.rivets("hrv%d" % sg, rivs[:14], P["brass"], r=1.2, rng_=rr, jitter=0.2, detail=False)
    # a planked, iron-strapped roof with a hatch
    slab("roof", 48.0, 50.4, 2.0, 86.0, -28.0, 28.0, P["wood"], "roof", bevel=0.4)
    for f in (10.0, 30.0, 50.0, 70.0):
        slab("strap%d" % f, 47.6, 51.2, f, f + 3.4, -28.8, 28.8, P["iron"], "roof_straps", bevel=0.2)
    slab("hatch", 50.4, 54.0, 36.0, 54.0, -9.0, 9.0, P["iron"], "roof", bevel=0.5)
    K.rivets("hatch_rv", [(Cc(54.0, f, s), (-1, 0, 0)) for f in (38.0, 52.0) for s in (-7.0, 7.0)], P["brass"], r=1.1, rng_=rr, detail=False)
    # the ram: an iron-shod log slung under the nose, a spiked cap with a bed of embers, iron bands
    K.bl.lathe("ram", [(0.01, 78.0), (8.5, 79.0), (9.6, 86.0), (10.6, 100.0), (11.2, 108.0), (10.0, 111.0)], P["wood"], sides=12, origin=tuple(Cc(16.0, 0, 0)), axis="y", part="ram", caps=(True, False))
    K.bl.lathe("ram_cap", [(10.4, 105.0), (13.2, 108.0), (13.4, 116.0), (11.0, 121.0), (6.0, 124.0), (0.01, 126.0)], P["iron"], sides=12, origin=tuple(Cc(16.0, 0, 0)), axis="y", part="ram_cap")
    for y in (90.0, 99.0):
        K.bl.lathe("ramband%d" % y, [(10.3, y - 1.4), (11.4, y - 0.8), (11.4, y + 0.8), (10.3, y + 1.4)], P["iron"], sides=12, origin=tuple(Cc(16.0, 0, 0)), axis="y", part="ram_bands")
    for k in range(7):
        a = TAU * k / 7 + 0.2
        p0 = Cc(16.0, 0, 0) + V(-13.0 * math.cos(a), 111.0, 13.0 * math.sin(a))
        K.tapered("ramspike%d" % k, [p0, p0 + V(-4.0 * math.cos(a), 3.0, 4.0 * math.sin(a)), p0 + V(-6.5 * math.cos(a), 9.0, 6.5 * math.sin(a))], 2.2, 0.15, P["plate"], sides=4, sub=1, part="spikes")
    K.blob("ram_ember", Cc(16.0, 124.0, 0.0), (4.4, 3.0, 4.4), P["ember"], seed=2, lump=0.2, rings=6, segs=10, part="embers")
    for sg in (1, -1):                                                                              # struts from the hull to the ram
        K.tube("strut%d" % sg, [Cc(24.0, 78.0, 20.0 * sg), Cc(20.0, 90.0, 11.0 * sg), Cc(17.0, 100.0, 9.0 * sg)], 1.8, P["iron"], sides=6, sub=2, part="ram_bands")
    # chains swagged along both sides of the hull
    for sg in (1, -1):
        K.chain("chain%d" % sg, [Cc(36.0, 10.0, 31.5 * sg), Cc(30.0, 28.0, 32.5 * sg), Cc(36.0, 46.0, 32.0 * sg), Cc(30.0, 64.0, 32.5 * sg), Cc(36.0, 80.0, 31.5 * sg)], 8.0, 5.0, 1.0, P["chain"], max_links=10, part="chains", up=(0, 0, 1))
    # a tattered banner on a pole at the rear corner
    K.tube("banner_pole", [Cc(50.0, 6.0, -22.0), Cc(74.0, 6.0, -22.0), Cc(92.0, 6.0, -22.0)], 1.0, P["iron"], sides=6, sub=2, part="banner")
    K.tapered("banner_tip", [Cc(91.0, 6.0, -22.0), Cc(96.0, 6.0, -22.0), Cc(99.0, 6.0, -22.0)], 1.4, 0.1, P["plate"], sides=4, sub=1, part="banner")
    path = [Cc(90.0 - 1.0 * k, 6.0 + 4.4 * k, -22.0 + 0.8 * math.sin(k * 0.9)) for k in range(8)]
    K.ribbon("banner", path, 14.0, P["cloth"], facing=(0, 0, 1), thick=0.1, part="banner", fray=2.5, rng_=rr, edge_wave=0.12, notch=3.0)
    return K.quad_estimate()


# ================================================================ 3. HOURGLASS SAND CARRIAGE
def hourglass_palette():
    P = {}
    P["brass"] = mat("hg_brass", (214, 164, 70), 1.0, 0.28, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.65, scratches=0.6, mottle_color=rgb(130, 84, 28), wear_color=rgb(255, 238, 180))
    P["dark"] = mat("hg_dark", (50, 40, 36), 1.0, 0.4, bump="hammer", bump_strength=0.2, dirt=0.9)
    P["wood"] = mat("hg_wood", (92, 58, 36), 0.0, 0.5, bump="grain", bump_strength=0.5, groove=rgb(30, 16, 8), groove_amount=0.8, dirt=0.7, wear=0.4, wear_metal=0.0)
    P["glass"] = mat("hg_glass", (190, 215, 235), 0.0, 0.05, transmission=0.9, ior=1.3, rim=(rgb(255, 255, 255), 0.5), dirt=0.0, wear=0.0, mottle=0.4, mottle_color=rgb(220, 240, 255), bevel=0.0)
    P["sand"] = mat("hg_sand", (236, 196, 120), 0.0, 0.85, bump="hammer", bump_scale=2.0, bump_strength=0.4, dirt=0.3, wear=0.1, wear_metal=0.0, mottle=0.9, mottle_color=rgb(200, 150, 80), bevel=0.0)
    P["sandglow"] = glowmat("hg_sandglow", (255, 214, 120), 0.7, 0.8)
    P["face"] = mat("hg_face", (238, 228, 200), 0.0, 0.5, dirt=0.5, wear=0.2, wear_metal=0.0, bevel=0.0)
    P["portal"] = glowmat("hg_portal", (150, 90, 255), 0.9, 0.9)
    P["portal2"] = glowmat("hg_portal2", (80, 40, 200), 0.8, 0.9)
    P["velvet"] = mat("hg_velvet", (60, 36, 110), 0.0, 0.7, bump="cross", bump_strength=0.4, dirt=0.5, wear_metal=0.0, bevel=0.0)
    return P


def hourglass_carriage(opts):
    C.begin(opts)
    C.weights(glass=1.1, frame=1.0, wheels=1.2, portal=1.2, sand=1.0, gears=1.0)
    P = hourglass_palette()
    rr = K.rng(7203)
    HF = 44.0                                                                                       # the hourglass stands on the chassis at f = 44
    # clockwork wheels: a toothed outer gear, a ring with twelve numeral studs, an inner cog, gold spokes, two glowing hands
    def cog_spokes(name, k, a, ctr):
        K.bar("%s_sp%d" % (name, k), V(ctr[0] - 4.0 * math.cos(a), ctr[1] + 4.0 * math.sin(a), ctr[2]), V(ctr[0] - 14.6 * math.cos(a), ctr[1] + 14.6 * math.sin(a), ctr[2]), 0.8, 0.8, P["brass"], part="wheels", taper=0.7, bevel_w=0.0)
    for nm, s0, sg in (("wl", -33.0, -1), ("wr", 33.0, 1)):
        wheel(nm, s0, sg, 17.0, P["brass"], P["brass"], P["brass"], spokes=6, hub_r=4.2, rr=rr, rim_t=1.2, spoke_fn=cog_spokes)
        K.gear(nm + "_gear", Cc(WU, WF, s0 + 0.4 * sg), 21.0, 18.0, 14, 1.0, P["dark"], normal=(0, 0, 1), part="gears", rng_=rr, wear=0.03, bevel_w=0.0)
        K.gear(nm + "_cog", Cc(WU, WF, s0 + 1.2 * sg), 11.0, 9.0, 8, 0.8, P["brass"], normal=(0, 0, 1), part="gears", rng_=rr, wear=0.03, bevel_w=0.0)
        for k in range(12):
            a = TAU * k / 12
            q = Cc(WU, WF, s0 + 2.0 * sg) + V(-15.0 * math.cos(a), 15.0 * math.sin(a), 0)
            K.rivets("%s_num%d" % (nm, k), [(q, (0, 0, sg))], P["sandglow"] if k % 3 == 0 else P["face"], r=1.1 if k % 3 == 0 else 0.8, sides=6, rng_=rr, detail=False)
        for ang, ln, w in ((20.0, 10.0, 0.7), (-70.0, 7.0, 0.9)):
            a = math.radians(ang)
            K.bar("%s_hand%d" % (nm, int(ang)), Cc(WU, WF, s0 + 3.2 * sg), Cc(WU, WF, s0 + 3.2 * sg) + V(-ln * math.cos(a), ln * math.sin(a), 0), w, 0.4, P["sandglow"], part="hands", taper=0.5, bevel_w=0.0)
    axle(P, P["brass"])
    # chassis: a brass beam deck, the draught pole with a gear collar, a yoke
    slab("deck", 12.0, 14.4, 0.4, 88.0, -24.0, 24.0, P["brass"], "frame", bevel=0.4)
    slab("beam", 6.0, 12.0, 0.4, 84.0, -8.0, 8.0, P["dark"], "frame", bevel=0.3)
    K.tube("pole", [Cc(10.0, 82.0, 0), Cc(13.5, 94.0, 0), Cc(19.5, 106.0, 0), Cc(26.6, 115.2, 0)], 1.8, P["brass"], sides=8, sub=3, taper=(2.1, 1.5), part="pole")
    K.tube("yoke", [Cc(28.4, 113.4, -17.6), Cc(26.4, 115.2, -12.0), Cc(26.6, 115.4, 0.0), Cc(26.4, 115.2, 12.0), Cc(28.4, 113.4, 17.6)], 1.3, P["brass"], sides=6, sub=2, part="pole")
    K.gear("pole_gear", Cc(18.0, 98.0, 0), 5.0, 3.6, 10, 0.8, P["dark"], normal=(0, 1, 0), part="gears", rng_=rr, wear=0.03, bevel_w=0.0)
    # the hourglass: two glass bulbs and a neck inside a brass cage of three twisted pillars between a toothed base and a crown
    prof_glass = [(0.01, 15.0), (8.0, 15.4), (16.5, 18.6), (21.4, 26.0), (22.0, 32.0), (18.0, 39.0), (10.0, 43.5), (4.0, 46.6), (2.6, 48.0), (4.0, 49.4), (10.0, 52.5), (18.0, 57.0), (22.0, 64.0), (21.4, 70.0), (16.5, 77.4), (8.0, 80.6), (0.01, 81.0)]
    K.bl.lathe("glass", [(r, -u) for r, u in prof_glass], P["glass"], sides=16, origin=tuple(V(0, HF, 0)), axis="x", part="glass")
    sand_bot = [(0.01, 15.6), (6.0, 15.8), (14.0, 18.4), (18.0, 22.0), (15.0, 24.5), (9.0, 27.0), (3.0, 29.0), (0.01, 30.0)]
    K.bl.lathe("sand_bottom", [(r, -u) for r, u in sand_bot], P["sand"], sides=14, origin=tuple(V(0, HF, 0)), axis="x", part="sand")
    sand_top = [(0.01, 77.0), (7.0, 76.0), (13.0, 71.0), (16.0, 66.0), (10.0, 60.0), (4.0, 54.0), (2.0, 51.0), (0.01, 50.0)]
    K.bl.lathe("sand_top", [(r, -u) for r, u in sand_top], P["sand"], sides=14, origin=tuple(V(0, HF, 0)), axis="x", part="sand")
    K.tube("stream", [Cc(52.0, HF, 0.0), Cc(40.0, HF + 0.4, 0.0), Cc(30.0, HF, 0.0)], 0.5, P["sandglow"], sides=5, sub=3, part="sand_stream")
    for sg_, ang in enumerate((0.0, 120.0, 240.0)):
        a0 = math.radians(ang + 30.0)
        pts = [Cc(14.0 + 68.0 * t, HF + 25.0 * math.cos(a0 + 0.9 * t * 2), 25.0 * math.sin(a0 + 0.9 * t * 2)) for t in [i / 10.0 for i in range(11)]]
        K.tube("pillar%d" % sg_, pts, 1.3, P["brass"], sides=6, sub=2, part="frame")
    for u0, u1, rad in ((12.0, 14.6, 27.0), (80.6, 83.0, 27.0)):
        C.loft("plate_%d" % u0, [[V(-uu, HF + r * math.cos(a), r * math.sin(a)) for a in [TAU * k / 20 for k in range(20)]] for uu, r in ((u0, rad - 2.0), (u0, rad), (u1, rad), (u1, rad - 1.6))], P["brass"], "frame")
    K.gear("base_gear", Cc(14.8, HF, 0.0), 27.0, 24.5, 16, 0.8, P["brass"], normal=(1, 0, 0), part="gears", rng_=rr, wear=0.03, bevel_w=0.0)
    K.bl.lathe("crown", [(14.0, -83.0), (8.0, -86.0), (3.0, -90.0), (0.01, -93.0)], P["brass"], sides=14, origin=tuple(V(0, HF, 0)), axis="x", part="crown", caps=(False, True))
    K.gem_setting("crown_gem", Cc(93.0, HF, 0.0), 2.6, P["portal"], P["brass"], normal=(-1, 0, 0), up=(0, 1, 0), claws=6, sides=8, part="crown", rng_=rr, claw_r=0.5)
    for sg in (1, -1):                                                                              # a velvet seat inside the lower bulb, lit by sand-light
        slab("seat%d" % sg, 16.0, 20.0, HF - 10.0 + 12.0 * (sg > 0), HF - 4.0 + 12.0 * (sg > 0), -8.0, 8.0, P["velvet"], "seat", bevel=0.8)
    # the portal gate behind: a standing ring, runes in it, a swirl of violet light filling it
    PU, PF = 36.0, -4.0
    K.bl.lathe("gate", [(25.0, -1.4), (28.2, -1.4), (29.0, 0.0), (28.2, 1.4), (25.0, 1.4)], P["brass"], sides=32, origin=tuple(Cc(PU, PF, 0.0)), axis="y", part="gate", caps=(False, False))
    K.bl.lathe("gate_in", [(0.01, 0.0), (24.8, 0.0)], P["portal"], sides=32, origin=tuple(Cc(PU, PF + 0.4, 0.0)), axis="y", part="portal", caps=(False, False))
    for k, rad in enumerate((6.0, 12.0, 18.0)):
        K.bl.lathe("gate_r%d" % k, [(rad, -0.2), (rad + 1.3, 0.6), (rad + 2.4, 0.2)], P["portal2"], sides=24, origin=tuple(Cc(PU, PF - 0.6, 0.0)), axis="y", part="portal_rings", caps=(False, False))
    for k in range(12):
        a = TAU * k / 12 + 0.1
        q = Cc(PU, PF - 1.6, 0.0) + V(-27.0 * math.cos(a), 0.0, 27.0 * math.sin(a))
        K.rivets("gate_stud%d" % k, [(q, (0, -1, 0))], P["sandglow"], r=1.3, sides=6, rng_=rr, detail=False)
    K.bar("gate_foot", Cc(10.0, PF, -9.0), Cc(10.0, PF, 9.0), 1.6, 3.4, P["dark"], part="gate", bevel_w=0.2)
    return K.quad_estimate()


# ================================================================ 4. FILOLIAL QUEEN'S CHARIOT
def queen_palette():
    P = {}
    P["gold"] = mat("qc_gold", (252, 204, 84), 1.0, 0.22, bump="hammer", bump_scale=0.7, bump_strength=0.12, dirt=0.45, mottle_color=rgb(184, 120, 36), wear_color=rgb(255, 248, 210), scratches=0.5)
    P["gold2"] = mat("qc_gold2", (196, 140, 50), 1.0, 0.32, bump="hammer", bump_strength=0.16, dirt=0.75, mottle_color=rgb(110, 70, 20))
    P["white"] = mat("qc_white", (250, 246, 238), 0.3, 0.4, bump="brushed", bump_strength=0.1, dirt=0.3, wear=0.5, wear_color=rgb(255, 255, 255), mottle=0.5, mottle_color=rgb(226, 214, 190))
    P["f1"] = mat("qc_f1", (252, 246, 232), 0.0, 0.5, wear=0.3, wear_metal=0.0, dirt=0.25, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(220, 190, 120)), (0.5, rgb(252, 246, 232)), (1.0, rgb(255, 214, 120))],
                  temit=[(0.0, rgb(0, 0, 0)), (0.7, rgb(40, 24, 6)), (1.0, rgb(255, 200, 90))], mottle=0.5, mottle_color=rgb(240, 214, 160), bevel=0.06, fine=0.4)
    P["f2"] = mat("qc_f2", (250, 200, 90), 0.0, 0.5, wear=0.3, wear_metal=0.0, dirt=0.25, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(190, 120, 40)), (0.5, rgb(250, 204, 96)), (1.0, rgb(255, 120, 210))],
                  temit=[(0.0, rgb(0, 0, 0)), (0.7, rgb(40, 14, 30)), (1.0, rgb(255, 110, 210))], mottle=0.5, mottle_color=rgb(220, 150, 60), bevel=0.06, fine=0.4)
    P["gem"] = glowmat("qc_gem", (230, 100, 255), 1.0, 0.5, rim=(rgb(255, 210, 255), 0.5))
    P["lit"] = glowmat("qc_lit", (255, 214, 110), 0.9, 0.6)
    P["velvet"] = mat("qc_velvet", (120, 30, 110), 0.0, 0.65, bump="cross", bump_strength=0.4, dirt=0.4, wear_metal=0.0, bevel=0.0)
    return P


def queens_chariot(opts):
    C.begin(opts)
    C.weights(wings=1.3, crown=1.2, wheels=1.1, hull=1.0, plates=1.0, pole=0.9)
    P = queen_palette()
    rr = K.rng(7204)

    def wing_spokes(name, k, a, ctr):                                                               # every spoke is a little gold feather blade
        sg = 1 if ctr[2] > 0 else -1
        K.feather("%s_fsp%d" % (name, k), V(ctr[0] - 3.4 * math.cos(a), ctr[1] + 3.4 * math.sin(a), ctr[2]), V(-math.cos(a), math.sin(a), 0), 14.6, 4.6, P["gold"] if k % 2 else P["f2"], rows=5, serrate=0.12, facing=(0, 0, sg),
                  part="wheels", seed=300 + k + (9 if sg < 0 else 0), thick=0.22, curl=0.8 * sg)
    for nm, s0, sg in (("wl", -33.0, -1), ("wr", 33.0, 1)):
        wheel(nm, s0, sg, 19.4, P["gold"], P["gold"], P["gold"], spokes=8, hub_r=4.6, rr=rr, spoke_fn=wing_spokes)
        K.gem_setting("%s_jewel" % nm, Cc(WU, WF, s0 + 3.4 * sg), 2.5, P["gem"], P["gold"], normal=(0, 0, sg), claws=6, sides=8, part="gems", rng_=rr, claw_r=0.5)
    axle(P, P["gold2"])
    # the open body: gold floor, a high swept prow, a tall rear back, sides scaled with golden armour plates
    slab("floor", 12.0, 14.4, 0.4, 88.0, -25.0, 25.0, P["gold2"], "hull", bevel=0.4)
    prow = [(0.0, 12.0), (88.0, 12.0), (92.0, 24.0), (88.0, 40.0), (80.0, 52.0), (66.0, 44.0), (50.0, 36.0), (36.0, 31.0), (20.0, 33.0), (6.0, 42.0), (0.0, 46.0)]
    for sg in (1, -1):
        side_plate("side%d" % sg, prow, 24.6 * sg, 26.6 * sg, P["white"], "hull")
        K.tube("rim%d" % sg, [Cc(u + 0.5, f, 27.0 * sg) for f, u in prow[2:-1]], 1.0, P["gold"], sides=6, sub=2, part="hull_rim")
        for r_, (f, u) in enumerate(((60.0, 30.0), (46.0, 25.0), (32.0, 24.0), (18.0, 27.0), (72.0, 38.0), (8.0, 36.0))):
            for k in range(2):
                K.scale_plate("sc%d_%d_%d" % (sg, r_, k), Cc(u - 0.0, f, (27.2 + 0.4 * k) * sg), V(0, 0, sg), 4.4 - 0.4 * k, P["gold"] if (r_ + k) % 2 == 0 else P["f2"], up=(-1, 0, 0), thick=0.22, rows=3, part="plates", tilt=18.0, shape="round")
    slab("seat", 14.4, 18.4, 8.0, 30.0, -16.0, 16.0, P["velvet"], "seat", bevel=0.9)
    slab("back", 16.0, 44.0, 2.0, 5.0, -22.0, 22.0, P["velvet"], "seat", bevel=1.0)
    for sg in (1, -1):
        K.tube("back_edge%d" % sg, [Cc(14.4, 3.5, 21.0 * sg), Cc(46.0, 3.5, 21.0 * sg)], 1.2, P["gold"], sides=6, sub=2, part="hull_rim")
    # the great wings: six feathers a side in two layers, sweeping out and up from behind the seat
    for sg in (1, -1):
        for k in range(7):
            a = math.radians(-5.0 + 20.0 * k)
            d = V(-math.sin(a) * 0.9 - 0.1, -0.25 * math.cos(a), sg * math.cos(a))
            K.feather("wing%d_%d" % (sg, k), Cc(40.0 + 1.5 * k, 8.0 + 1.0 * k, 22.0 * sg), d, 36.0 + 7.0 * (3 - abs(3 - k)), 11.0 - 0.5 * k, P["f1"] if k % 2 == 0 else P["f2"], rows=8, serrate=0.18, facing=(0, 0, sg), part="wings",
                      seed=320 + k + (11 if sg < 0 else 0), thick=0.16, curl=1.5 * sg, bend=V(0, -2.0, 3.0 * sg))
        for k in range(4):
            K.feather("wingu%d_%d" % (sg, k), Cc(44.0, 8.0, 20.0 * sg), V(-0.7 - 0.1 * k, -0.35, sg * (0.3 + 0.18 * k)), 24.0 - 3.0 * k, 8.0, P["f2"] if k % 2 else P["f1"], rows=6, serrate=0.16, facing=(0, 0, sg), part="wings",
                      seed=340 + k + (5 if sg < 0 else 0), thick=0.16, curl=1.0 * sg)
    # a crown on a post over the prow, with a glowing dragon-breath stone, and a tall crest of feathers behind the seat
    K.tube("crown_post", [Cc(40.0, 80.0, 0.0), Cc(54.0, 82.0, 0.0), Cc(66.0, 84.0, 0.0)], 1.3, P["gold"], sides=8, sub=2, part="crown")
    K.bl.lathe("crown_ring", [(5.8, -66.0), (7.2, -64.5), (7.6, -62.0), (6.6, -60.5), (5.0, -62.0)], P["gold"], sides=14, origin=tuple(Cc(0.0, 84.0, 0.0)), axis="x", part="crown", caps=(False, False))
    for k in range(9):
        a = TAU * k / 9
        p0 = Cc(62.0, 84.0, 0.0) + V(0.0, 6.4 * math.cos(a), 6.4 * math.sin(a))
        K.tapered("prong%d" % k, [p0, p0 + V(-4.0, 0.8 * math.cos(a), 0.8 * math.sin(a)), p0 + V(-8.0 - 3.0 * (k % 2), 0.2 * math.cos(a), 0.2 * math.sin(a))], 0.9, 0.12, P["gold"], sides=4, sub=1, part="crown")
    K.gem_setting("crown_gem", Cc(64.0, 84.0, 0.0), 3.4, P["gem"], P["gold"], normal=(-1, 0, 0), up=(0, 1, 0), claws=6, sides=8, part="crown", rng_=rr, claw_r=0.5)
    for k in range(5):
        a = math.radians(20.0 + 12.0 * k)
        K.feather("crest%d" % k, Cc(44.0, 4.0, 0.0), V(-math.cos(a) * 0.8, -math.sin(a) * 0.5 - 0.1, (k - 2) * 0.12), 24.0 - 2.0 * abs(2 - k), 7.0, P["f2"] if k % 2 else P["f1"], rows=7, serrate=0.16, facing=(0, 0, 1), part="wings", seed=360 + k, thick=0.14, curl=1.2)
    # the pole: gold, with a feathered collar, a harness yoke with a gem
    K.tube("pole", [Cc(11.0, 84.0, 0), Cc(13.5, 94.0, 0), Cc(19.5, 106.0, 0), Cc(26.6, 115.2, 0)], 1.9, P["gold"], sides=8, sub=3, taper=(2.2, 1.6), part="pole")
    K.tube("yoke", [Cc(28.4, 113.4, -17.6), Cc(26.4, 115.2, -12.0), Cc(26.6, 115.4, 0.0), Cc(26.4, 115.2, 12.0), Cc(28.4, 113.4, 17.6)], 1.5, P["gold"], sides=6, sub=2, part="pole")
    K.gem_setting("yoke_gem", Cc(28.4, 115.4, 0.0), 2.0, P["gem"], P["gold"], normal=(-1, 0, 0), up=(0, 1, 0), claws=6, sides=8, part="gems", rng_=rr, claw_r=0.5)
    for sg in (1, -1):
        K.feather("collar%d" % sg, Cc(19.0, 104.0, 0.0), V(-0.2, 0.9, sg * 1.0), 11.0, 4.6, P["f1"], rows=6, serrate=0.14, facing=(1, 0, 0), part="pole", seed=380 + (3 if sg < 0 else 0), thick=0.14)
    return K.quad_estimate()


C.register("filolial", filolial_carriage, "filolial_carriage")
C.register("war", war_carriage, "war_carriage")
C.register("hourglass", hourglass_carriage, "hourglass_sand_carriage")
C.register("queen", queens_chariot, "queens_chariot")

if __name__ == "__main__":
    C.main(sys.argv[1:])

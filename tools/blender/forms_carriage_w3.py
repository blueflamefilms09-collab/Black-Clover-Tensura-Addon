"""Style wave 3 of the VASSAL CARRIAGE forms war_carriage and queens_chariot (docs/heroweapon/style/wave3.md). REPLACEMENTS: the previous meshes and skins are kept in
weapon_meshes/classic_style3/ and textures/entity/weapon/classic_style3/ (client option vassalForms.classicStyleWave3).

    python3 -B tools/blender/forms_carriage_w3.py [--only war,queen] [--trial DIR] [--beauty DIR] [--quick] [--samples N]

Both keep the base carriage's frame (+y the nose, -x up, z the width, wheels at z = +-33, pole tip at f ~120, place line) and its size class.
  war_carriage     a Roman war chariot (weapon_carriage.md): ONE big scarred oak hull open at the back with a rising D-shaped prow, torn plank edges (different on each side), three iron straps of
                   unequal spacing, two irregular rivet rows, three arrows stuck in the right side, plain iron-shod wheels, a draught pole, and ONE tall standard planted in the middle of the hull (the item's display pivot lies on it: hold audit NONE rule) with a torn red
                   banner and a hanging lamp. No ram, roof, crenellations, hung shields, chains, spikes, spiked wheels, brass or ember slits (they were 16 rivet rows and ten fittings).
  queens_chariot   Filolial Queen's Chariot: a pearl-white open chariot with a THIN gold rim (gold is only the rim, the crown and the hub caps), a long wing of seven feathers on the left and a
                   shorter one of four on the right (not twins), three crest feathers, one crown of five prongs over ONE rose stone on a mast standing from the floor (it also puts the item's display pivot on the model: hold audit NONE rule); plain turned wheels. No scale plates, scrolls, lamps, jewelled
                   wheels or feather spokes.
Palette per weapon: 3 base materials + ONE named accent + at most 2 utility materials (wave2_kit.Palette). Seeded.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import wave3_kit as W3  # noqa: E402
import forms_kit as K  # noqa: E402
import wave2_kit as W  # noqa: E402
from forms_kit import rgb, TAU  # noqa: E402
import forms_vassal_carriage as FC  # noqa: E402  (registers the old builders; they are not touched)
import forms_vassal_common_b as C  # noqa: E402
from forms_vassal_common_b import V  # noqa: E402
from forms_vassal_carriage import Cc, WU, WF, slab, side_plate, wheel2, leaf_spring, lantern, axle  # noqa: E402

C.setup("vassal_carriage", "handitem", W3.MANIFEST, "vassal_carriage_w3", glow=("FFFFFF", 0.12, 0.45, 1.0))
BUILDERS = {}


def register(key, fn, form_id):
    BUILDERS[key] = (fn, "vassal_carriage__%s" % form_id, form_id)


# ================================================================ 1. WAR CARRIAGE
def war_palette():
    P = W.Palette("cloth", ["oak", "iron", "hide"], ["soot", "lamp"])
    P["oak"] = W.wood("wc3_oak", rgb(112, 76, 44), seed=901, bump_strength=0.6)
    P["iron"] = W.iron("wc3_iron", rgb(72, 76, 86), seed=902, wear=0.7, mottle_color=rgb(96, 52, 32), scratches=0.7)
    P["hide"] = W.hide("wc3_hide", rgb(84, 56, 40), seed=903)
    P["cloth"] = W.hide("wc3_cloth", rgb(150, 30, 30), seed=904, bump="cross", bump_strength=0.4, bevel=0.0)
    P["soot"] = W.scratch("wc3_soot", (0.02, 0.016, 0.014, 1.0))
    P["lamp"] = W.accent_gem("wc3_lamp", rgb(255, 170, 70), rgb(255, 190, 90), 0.8, seed=905)
    return P.check()


def war_carriage(opts):
    C.begin(opts)
    C.weights(hull=1.2, wheels=1.0, banner=1.0, straps=0.9, arrows=0.8)
    P = war_palette()
    rr = K.rng(8401)
    for nm, s0, sg in (("wl", -33.0, -1), ("wr", 33.0, 1)):                                      # plain iron-shod oak wheels, eight spokes, no spikes
        wheel2(nm, s0, sg, 19.4, P["iron"], P["iron"], P["oak"], spokes=8, hub_r=5.0, rr=rr, band=P["iron"], stud_mat=P["iron"], nave_band=P["iron"])
    axle(P, P["iron"], r=2.4)
    for sg in (1, -1):
        leaf_spring("spring%d" % sg, sg, P["iron"], P["iron"], rise=5.0)
    slab("floor", 11.0, 13.6, 0.4, 86.0, -26.0, 26.0, P["oak"], "hull")
    slab("chassis", 5.0, 11.0, 0.4, 84.0, -20.0, 20.0, P["iron"], "hull")
    # the hull: oak sides open at the back, rising to a rounded prow; the torn top edge differs on the two sides (the scars)
    prow = {1: [(0, 26), (8, 24), (14, 29), (26, 27), (40, 33), (52, 30), (66, 40), (76, 45), (84, 44), (89.5, 36), (91.5, 22), (90, 11), (0, 11)],
            -1: [(0, 28), (10, 27), (21, 22), (28, 28), (44, 34), (58, 38), (70, 43), (80, 46), (88, 41), (91.5, 30), (91.0, 20), (89, 11), (0, 11)]}
    for sg in (1, -1):
        side_plate("hull%d" % sg, prow[sg], 24.0 * sg, 26.6 * sg, P["oak"], "hull", bevel=0.3)
        top = [(f, u) for f, u in prow[sg][:-2]]
        K.tube("rim%d" % sg, [Cc(u + 0.3, f, 27.0 * sg) for f, u in top[3:]], 0.9, P["iron"], sides=5, sub=2, part="straps")
        for k, f in enumerate((22.0, 49.0, 71.0) if sg > 0 else (17.0, 38.0, 77.0)):                # three iron straps, unequal spacing, different on each side
            u1 = min(u for ff, u in top if ff >= f) if False else next(u for ff, u in top if ff >= f)
            K.bar("strap%d_%d" % (sg, k), Cc(11.6, f, 27.0 * sg), Cc(u1 - 1.2, f, 27.0 * sg), 2.2, 0.9, P["iron"], part="straps", taper=0.9, bevel_w=0.1, up=(0, 0, 1))
        for f, u, rad in ((46.0, 30.0, 2.6), (14.0, 18.0, 1.8)) if sg > 0 else ((62.0, 28.0, 3.0), (30.0, 16.0, 1.6)):      # scorched patches (baked, free)
            pts = [(-(u + rad * math.sin(a) * 1.2), f + rad * math.cos(a)) for a in [TAU * i / 9 for i in range(9)]]
            W.flat_plate("scorch%d_%d_%d" % (sg, int(f), int(u)), pts, 26.6 * sg + 0.02 * sg, P["soot"], 0.05)
    # the two irregular rivet rows (bake-only), one along the rim, one low
    for sg in (1, -1):
        for nm, ff0, ff1, gaps, u in (("rv_a", 6.0, 86.0, (1.0, 2.2, 0.8, 1.5, 2.6, 1.1), 18.0), ("rv_b", 28.0, 70.0, (1.4, 0.9, 2.1, 1.0), 14.0)):
            fs = W.positions(ff0, ff1, gaps)
            K.rivets("%s%d" % (nm, sg), [(Cc(u + (3.0 if nm == "rv_a" else 0.0), f, 26.7 * sg), (0, 0, sg)) for f in fs], P["iron"], r=0.9, rng_=rr, jitter=0.2, detail=True)
    slab("dash", 11.0, 44.0, 86.0, 89.6, -22.0, 22.0, P["oak"], "hull", bevel=0.4)
    # three arrows stuck in the right side at different angles (one bent shaft, one broken)
    for k, (f, u, dz, ang, ln) in enumerate(((30.0, 21.0, 1.0, 20.0, 15.0), (58.0, 27.0, 1.0, -28.0, 11.0), (74.0, 20.0, 1.0, 8.0, 7.0))):
        a = math.radians(ang)
        p0 = Cc(u, f, 26.4)
        p1 = p0 + V(-math.sin(a) * 2.0, math.cos(a) * 2.0, 1.0) * (ln / 2.2)
        K.tapered("arrow%d" % k, [p0, p1], 0.5, 0.42, P["oak"], sides=4, sub=1, part="arrows")
        if k != 2:
            K.feather("arrow_fletch%d" % k, p1 + V(0.0, 0.0, 0.2), V(p1 - p0).normalized(), 4.0, 1.4, P["hide"], rows=3, serrate=0.05, facing=(1, 0, 0), part="arrows", seed=40 + k, thick=0.06)
    # the draught pole with a yoke
    K.tube("pole", [Cc(11.0, 84.0, 0), Cc(13.5, 94.0, 0), Cc(19.5, 106.0, 0), Cc(26.6, 115.2, 0)], 1.9, P["oak"], sides=8, sub=3, taper=(2.3, 1.6), part="pole")
    K.tube("yoke", [Cc(28.4, 113.4, -17.6), Cc(26.4, 115.2, -12.0), Cc(26.6, 115.4, 0.0), Cc(26.4, 115.2, 12.0), Cc(28.4, 113.4, 17.6)], 1.4, P["iron"], sides=6, sub=2, part="pole")
    # THE ONE-OFF: a tall standard at the rear corner: pole, a crossbar, a torn red banner hanging from it, a spearhead cap, one lamp
    K.tube("banner_pole", [Cc(12.0, 46.0, 0.0), Cc(54.0, 46.0, 0.0), Cc(96.0, 46.0, 0.0)], 1.1, P["oak"], sides=6, sub=2, part="banner")
    K.tapered("banner_tip", [Cc(95.0, 46.0, 0.0), Cc(100.0, 46.0, 0.0), Cc(104.0, 46.0, 0.0)], 1.5, 0.1, P["iron"], sides=4, sub=1, part="banner")
    K.bar("banner_bar", Cc(90.0, 46.0, -8.0), Cc(90.0, 46.0, 8.0), 1.0, 1.0, P["oak"], part="banner", taper=0.9, bevel_w=0.0)
    path = [Cc(89.0 - 1.2 * k, 46.0 + 0.4 * math.sin(k * 0.8), 0.0 + 0.7 * math.sin(k * 0.9)) for k in range(9)]
    K.ribbon("banner", path, 11.0, P["cloth"], facing=(0, 1, 0), thick=0.1, part="banner", fray=3.2, rng_=rr, edge_wave=0.14, notch=4.0)
    lantern("lamp", Cc(13.6, 8.0, -20.0), P["iron"], P["lamp"], post=8.0)
    return K.quad_estimate()


# ================================================================ 2. FILOLIAL QUEEN'S CHARIOT
def queen_palette():
    P = W.Palette("gem", ["pearl", "gold", "velvet"], ["f1", "f2"])
    P["pearl"] = W.steel("qc3_pearl", rgb(246, 242, 234), seed=911, rough=0.34, bevel=0.12, mottle_color=rgb(222, 210, 188), wear=0.35, wear_color=rgb(255, 255, 252), dirt=0.4, scratches=0.3)
    P["gold"] = W.iron("qc3_gold", rgb(232, 184, 74), seed=912, rough=0.28, wear_color=rgb(255, 238, 190), mottle_color=rgb(170, 110, 36))
    P["velvet"] = W.hide("qc3_velvet", rgb(120, 34, 100), seed=913, bump="cross", bump_strength=0.4, bevel=0.0)
    P["f1"] = K.surface("qc3_f1", rgb(252, 246, 232), 0.0, 0.5, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(220, 190, 150)), (0.5, rgb(252, 246, 232)), (1.0, rgb(255, 232, 200))], mottle=0.5,
                        mottle_color=rgb(240, 214, 170), wear=0.3, wear_color=rgb(255, 255, 250), wear_metal=0.0, dirt=0.25, seed=914, bevel=0.06, fine=0.4)
    P["f2"] = K.surface("qc3_f2", rgb(246, 196, 200), 0.0, 0.5, feather=(14, 5, 0.7, 0.8), tramp=[(0.0, rgb(200, 130, 140)), (0.5, rgb(248, 200, 206)), (1.0, rgb(255, 226, 214))], mottle=0.5,
                        mottle_color=rgb(226, 160, 170), wear=0.3, wear_color=rgb(255, 250, 250), wear_metal=0.0, dirt=0.25, seed=915, bevel=0.06, fine=0.4)
    P["gem"] = W.accent_gem("qc3_gem", rgb(230, 100, 220), rgb(250, 130, 240), 0.85, seed=916)
    return P.check()


def queens_chariot(opts):
    C.begin(opts)
    C.weights(wings=1.3, crown=1.2, wheels=1.0, hull=1.0, pole=0.9)
    P = queen_palette()
    rr = K.rng(8411)
    for nm, s0, sg in (("wl", -33.0, -1), ("wr", 33.0, 1)):                                      # plain pale wheels, gold only on the hub cap
        wheel2(nm, s0, sg, 19.4, P["pearl"], P["gold"], P["pearl"], spokes=8, hub_r=4.6, rr=rr, band=P["pearl"], stud_mat=P["pearl"], nave_band=P["gold"])
    axle(P, P["pearl"])
    for sg in (1, -1):
        leaf_spring("spring%d" % sg, sg, P["pearl"], P["pearl"], rise=6.0)
    slab("floor", 12.0, 14.4, 0.4, 88.0, -25.0, 25.0, P["pearl"], "hull", bevel=0.4)
    prow = [(0.0, 12.0), (88.0, 12.0), (92.0, 24.0), (88.0, 40.0), (80.0, 52.0), (66.0, 44.0), (50.0, 36.0), (36.0, 31.0), (20.0, 33.0), (6.0, 42.0), (0.0, 46.0)]
    for sg in (1, -1):
        side_plate("side%d" % sg, prow, 24.6 * sg, 26.6 * sg, P["pearl"], "hull")
        K.tube("rim%d" % sg, [Cc(u + 0.5, f, 27.0 * sg) for f, u in prow[2:-1]], 0.7, P["gold"], sides=5, sub=2, part="hull_rim")
    slab("seat", 14.4, 18.4, 8.0, 30.0, -16.0, 16.0, P["velvet"], "seat", bevel=0.9)
    slab("back", 16.0, 44.0, 2.0, 5.0, -22.0, 22.0, P["velvet"], "seat", bevel=1.0)
    # wings, NOT twins: seven feathers on the left (sg -1), four on the right, graded lengths
    for sg, count in ((-1, 7), (1, 4)):
        for k in range(count):
            a = math.radians(-5.0 + (20.0 if sg < 0 else 26.0) * k)
            d = V(-math.sin(a) * 0.9 - 0.1, -0.25 * math.cos(a), sg * math.cos(a))
            ln = (36.0 + 7.0 * (3 - abs(3 - k))) if sg < 0 else (28.0 + 5.0 * (1.5 - abs(1.5 - k)))
            K.feather("wing%d_%d" % (sg, k), Cc(40.0 + 1.5 * k, 8.0 + 1.0 * k, 22.0 * sg), d, ln, 11.0 - 0.5 * k, P["f1"] if k % 2 == 0 else P["f2"], rows=6, serrate=0.18, facing=(-1, 0, 0), part="wings",
                      seed=420 + k + (11 if sg < 0 else 0), thick=0.16, curl=1.5 * sg, bend=V(0, -2.0, 3.0 * sg))
    for k in range(3):                                                                           # a crest behind the seat
        a = math.radians(24.0 + 14.0 * k)
        K.feather("crest%d" % k, Cc(44.0, 4.0, 0.0), V(-math.cos(a) * 0.8, -math.sin(a) * 0.5 - 0.1, (k - 1) * 0.14), 24.0 - 3.0 * k, 7.0, P["f2"] if k % 2 else P["f1"], rows=7, serrate=0.16,
                  facing=(0, 0, 1), part="wings", seed=430 + k, thick=0.14, curl=1.2)
    # one crown of five prongs over ONE rose stone on a mast standing from the floor (it also puts the item's display pivot on the model: hold audit NONE rule) (the accent)
    K.tube("crown_post", [Cc(14.0, 70.0, 0.0), Cc(40.0, 70.6, 0.0), Cc(66.0, 72.0, 0.0)], 1.2, P["pearl"], sides=8, sub=2, part="crown")
    K.bl.lathe("crown_ring", [(5.4, -66.0), (6.8, -64.5), (7.2, -62.0), (6.2, -60.5), (4.8, -62.0)], P["gold"], sides=14, origin=tuple(Cc(0.0, 72.0, 0.0)), axis="x", part="crown", caps=(False, False))
    for k in range(5):
        a = TAU * k / 5 + 0.3
        p0 = Cc(62.0, 72.0, 0.0) + V(0.0, 6.0 * math.cos(a), 6.0 * math.sin(a))
        K.tapered("prong%d" % k, [p0, p0 + V(-4.0, 0.8 * math.cos(a), 0.8 * math.sin(a)), p0 + V(-8.0 - 3.0 * (k % 2), 0.2 * math.cos(a), 0.2 * math.sin(a))], 0.9, 0.12, P["gold"], sides=4, sub=1, part="crown")
    K.gem_setting("crown_gem", Cc(64.0, 72.0, 0.0), 3.0, P["gem"], P["gold"], normal=(-1, 0, 0), up=(0, 1, 0), claws=5, sides=8, part="crown", rng_=rr, claw_r=0.5)
    K.tube("pole", [Cc(11.0, 84.0, 0), Cc(13.5, 94.0, 0), Cc(19.5, 106.0, 0), Cc(26.6, 115.2, 0)], 1.9, P["pearl"], sides=8, sub=3, taper=(2.2, 1.6), part="pole")
    K.tube("yoke", [Cc(28.4, 113.4, -17.6), Cc(26.4, 115.2, -12.0), Cc(26.6, 115.4, 0.0), Cc(26.4, 115.2, 12.0), Cc(28.4, 113.4, 17.6)], 1.4, P["pearl"], sides=6, sub=2, part="pole")
    C.ring_y(0, 1, 1, 0) if False else None
    K.bl.lathe("pole_collar", [(2.4, -100.0), (2.9, -99.4), (2.9, -97.4), (2.4, -96.8)], P["gold"], sides=10, origin=tuple(Cc(0.0, 0.0, 0.0)), axis="x", part="pole", caps=(False, False))
    return K.quad_estimate()


register("war", war_carriage, "war_carriage")
register("queen", queens_chariot, "queens_chariot")


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--only", default=",".join(BUILDERS))
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--bake", type=int, default=2048)
    a = ap.parse_args(argv)
    C.THREADS[0] = max(1, min(2, a.threads))
    C.BAKE_PX[0] = a.bake
    W3.use_classic_headers()
    C.BUILDERS.clear()
    C.BUILDERS.update(BUILDERS)
    C.run(a.only.split(","), a.trial, a.beauty, a.quick, a.samples)


if __name__ == "__main__":
    main(sys.argv[1:])

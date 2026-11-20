"""Bow of Pride, its own stave (4.65.0, docs/heroweapon/curse_bow.md): legendary_bow_curse no longer shares the base bow's stave.

    python3 -B tools/blender/forms_legendary_bow_curse.py [--trial DIR] [--quick] [--lowres] [--samples N] [--no-check]

Scripted bpy (Blender as a Python module, no Blender window). Same pipeline as the third-look bows (forms_legendary_bow.py driver, forms_legendary_bow_v3.py helpers and connectivity check):
bake, rest mesh + three draw stages, 1024 px skin + glow skin, 5000 quad lines, 350 KB. The mesh id is `legendary_bow_curse` and the place line is the one the curse bow already ships
with (0.92426 0.92426 225 0.00625 -84.239), so it stays the size it was.

The stave: the riser sits in a notch (x 24) and the two limbs swell FORWARD into razor-edged horn blades (x 49 at y +-40) before they fall back to the tips (x 6); each limb carries
graded barbs on its outer edge and a violet sin-vein on both faces, each tip a forward-hooked horn. An almond eye in a bezel on the riser, a three-blade crown. Nothing is named wrap /
grip / handle (a bow is held at its riser); limb* flexes, string* and arrow* move in the draw stages, everything else is rigid.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_legendary_bow as F  # noqa: E402
import forms_legendary_bow_v3 as V3  # noqa: E402
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU, bl  # noqa: E402

HALF, RISER_H = F.HALF, 12.0
XT, BELLY, BULGE = 6.0, 24.0, 25.0
PLACE = (0.92426, 0.92426, 225.0, 0.00625, -84.239)          # the curse bow's own place line (legendary_bow_curse.txt of 4.64.0)
GLOW = ("FFFFFF", 0.12, 0.5, 1.0)
at, stave, band, limb_pts, riser, hand_binding, noisy = F.at, F.stave, F.band, F.limb_pts, F.riser, F.hand_binding, F.noisy


def curse_fn(y):
    """x of the stave at height y: flat at the riser, the limbs swell forward by BULGE (the horn blades) and fall back to the tip's x."""
    u = max(0.0, (abs(y) - RISER_H) / (HALF - RISER_H))
    base = XT + (BELLY - XT) * (1.0 - u ** 1.7)
    return base + BULGE * math.sin(math.pi * u ** 0.8) ** 1.25


def curse_palette():
    S = K.surface
    P = {}
    P["horn"] = S("cbhorn", rgb(26, 20, 38), 0.55, 0.36, wear=0.9, wear_color=rgb(150, 120, 196), dirt=0.9, dirt_color=(0.012, 0.006, 0.02, 1), scratches=0.8, bump="hammer", bump_scale=0.8, bump_strength=0.22,
                  mottle=1.0, mottle_color=rgb(78, 36, 120), fine=0.5, seed=301, bevel=0.2, ao_dist=2.0, groove=rgb(10, 6, 18), groove_amount=0.8,
                  cramp=("y", -84.0, 84.0, [(0.0, rgb(14, 10, 22)), (0.3, rgb(44, 26, 70)), (0.5, rgb(86, 44, 138)), (0.7, rgb(44, 26, 70)), (1.0, rgb(14, 10, 22))]))
    P["iron"] = S("cbiron", rgb(30, 26, 40), 1.0, 0.42, wear=0.9, wear_color=rgb(168, 160, 196), dirt=0.9, dirt_color=(0.01, 0.006, 0.014, 1), scratches=0.9, bump="hammer", bump_scale=0.7, bump_strength=0.2,
                  mottle=0.9, mottle_color=rgb(60, 30, 96), fine=0.5, seed=302, bevel=0.18, ao_dist=1.8)
    P["steel"] = S("cbsteel", rgb(150, 146, 172), 1.0, 0.3, wear=0.8, wear_color=rgb(236, 232, 252), dirt=0.8, dirt_color=(0.02, 0.012, 0.03, 1), scratches=0.9, bump="brushed", bump_scale=0.8, bump_strength=0.16,
                   mottle=0.8, mottle_color=rgb(86, 56, 130), fine=0.5, seed=303, bevel=0.16, ao_dist=1.8)
    P["gold"] = S("cbgold", rgb(150, 106, 46), 1.0, 0.36, wear=0.9, wear_color=rgb(236, 200, 120), dirt=1.0, dirt_color=(0.03, 0.015, 0.004, 1), scratches=0.8, bump="hammer", bump_scale=0.7, bump_strength=0.2,
                  mottle=1.0, mottle_color=rgb(70, 34, 60), fine=0.5, seed=304, bevel=0.18, ao_dist=2.0)
    P["sin"] = S("cbsin", rgb(110, 60, 190), 0.0, 0.4, emit=rgb(190, 110, 255), emit_strength=2.6, emit_flicker=0.35, mottle=0.8, mottle_color=rgb(255, 150, 255), seed=305, bevel=0.0)
    P["eye"] = S("cbeye", rgb(255, 60, 200), 0.1, 0.08, emit=rgb(255, 70, 210), emit_strength=1.6, emit_flicker=0.4, mottle=0.8, mottle_color=rgb(255, 190, 255), seed=306, bevel=0.0)
    P["pupil"] = S("cbpupil", rgb(6, 3, 10), 0.0, 0.3, seed=307, bevel=0.0)
    P["wrap"] = S("cbwrap", rgb(34, 26, 46), 0.0, 0.6, bump="wrap", bump_scale=1.0, bump_strength=0.9, groove=rgb(96, 50, 150), groove_amount=1.0, wear=0.5, wear_color=rgb(110, 90, 140), dirt=0.8, seed=308, bevel=0.0, fine=0.4)
    P["string"] = S("cbstring", rgb(230, 205, 255), 0.0, 0.5, emit=rgb(205, 150, 255), emit_strength=1.6, seed=309, bevel=0.0)
    P["ink"] = S("cbink", rgb(24, 16, 36), 0.0, 0.6, mottle=0.8, mottle_color=rgb(70, 36, 110), seed=310, bevel=0.04, fine=0.4)
    return P


def blade_sec(w, h):
    """A razor-edged limb section: a sharp forward edge (toward the target), a thick spine behind it."""
    return [(w * 1.7, 0.0), (w * 0.35, h), (-w * 0.7, h * 0.7), (-w, 0.0), (-w * 0.7, -h * 0.7), (w * 0.35, -h)]


def almond(cx, cy, hw, hl, n=10, p=1.5):
    """A lens outline: pointed at the top and the bottom (x = hw (1 - |y/hl|^p))."""
    right = [(cx + hw * (1.0 - abs(k / n) ** p), cy + hl * k / n) for k in range(-n, n + 1)]
    left = [(cx - hw * (1.0 - abs(k / n) ** p), cy + hl * k / n) for k in range(n - 1, -n, -1)]
    return right + left


def bow_of_pride(o):
    sh = dict(belly=BELLY, riser=RISER_H)
    sh["fn"] = curse_fn
    F.begin(o["samples"])
    P = curse_palette()
    rr = K.rng(3837)
    L = {1: limb_pts(1, sh, 26), -1: limb_pts(-1, sh, 26)}
    wd = lambda t: 4.3 * (1 - t) ** 0.6 + 1.2          # noqa: E731
    hh = lambda t: 3.2 * (1 - t) ** 0.7 + 0.8          # noqa: E731
    bx = V3.bow_x(0, sh)
    for s in (1, -1):
        ob = stave("limb_blade%d" % s, L[s], lambda t: blade_sec(wd(t), hh(t)), P["horn"], "limb_blade")
        noisy(ob, 0.05, 0.06, 301 + s)
        # collars: tarnished gold at the root, iron mid-limb, steel near the tip
        for k, (t, m) in enumerate(((0.05, P["gold"]), (0.36, P["iron"]), (0.7, P["steel"]))):
            band("limb_collar%d_%d" % (s, k), L[s], t, lambda tt, t=t: blade_sec(wd(t) + 0.1, hh(t) + 0.1), 0.4 * (1 - 0.3 * t), m, "limb_collar", width=0.02)
        # the sin-vein: a thin violet line of light down both faces
        for z in (1, -1):
            pts = []
            for i in range(14):
                t = 0.07 + 0.83 * i / 13
                p, tn, nr = at(L[s], t)
                pts.append(tuple(p + nr * (-0.05 * wd(t)) + V((0, 0, z * (hh(t) * 0.86 + 0.18)))))
            K.tube("limb_vein%d_%d" % (s, z), pts, 0.34, P["sin"], sides=4, sub=1, part="limb_vein", taper=(0.34, 0.2))
        # barbs: graded, leaning toward the tip, rooted inside the blade's outer edge
        for k, (t, ln) in enumerate(((0.16, 11.0), (0.3, 16.0), (0.46, 15.0), (0.62, 11.5), (0.78, 8.0))):
            p, tn, nr = at(L[s], t)
            base = p + nr * (wd(t) * 1.15)
            d = (nr * 0.74 + tn * 0.68).normalized()
            pts = [base, base + d * ln * 0.45, base + d * ln * 0.8 + tn * 1.6, base + d * ln + tn * 3.4]
            K.tapered("limb_barb%d_%d" % (s, k), pts, 2.0 - 0.15 * k, 0.1, P["horn"] if k % 2 else P["iron"], sides=4, sub=2, part="limb_barb")
        # the tip: a steel nock, and a horn that hooks forward past it
        pe, tn, nr = at(L[s], 1.0)
        K.tapered("limb_nock%d" % s, [pe - tn * 4.0, pe, pe + tn * 0.9], 1.25, 0.45, P["steel"], sides=6, sub=2, part="limb_nock")
        hk = [V((pe.x, pe.y - s * 3.0, 0.0)), V((pe.x + 0.2, pe.y + s * 3.4, 0.0)), V((pe.x + 3.6, pe.y + s * 8.2, 0.0)), V((pe.x + 10.5, pe.y + s * 11.4, 0.0)), V((pe.x + 17.0, pe.y + s * 11.2, 0.0))]
        K.tapered("limb_hook%d" % s, hk, 1.7, 0.1, P["steel"], sides=4, sub=3, part="limb_hook")
    # the riser: black iron round an almond eye, a crown of three blades in front
    riser("riser_core", sh, [(-17.0, 2.6, 2.8), (-12.0, 3.6, 3.3), (-8.0, 3.1, 3.1), (2.0, 3.2, 3.2), (4.0, 5.2, 3.8), (10.0, 6.2, 3.9), (16.0, 4.8, 3.5), (20.0, 3.0, 2.8)], P["iron"])
    hand_binding(sh, P["wrap"], P["sin"], -9.8, 2.4, 2.6, bands=(-9.3, -4.4, 1.7), band_mat=P["gold"], turns=8, cord=0.3)
    yc = 9.5
    for z in (1, -1):
        K.plate("riser_bezel%d" % z, almond(bx, yc, 6.4, 12.6), z * 3.2, z * 4.3, P["steel"], part="riser_bezel", bevel_w=0.15)
        K.plate("riser_lens%d" % z, almond(bx, yc, 4.8, 10.0), z * 4.1, z * 4.8, P["gold"], part="riser_bezel", bevel_w=0.1)
        K.gem_setting("gem%d" % z, (bx, yc, z * 4.8), 3.6, P["eye"], P["gold"], normal=(0, 0, z), claws=6, sides=8, part="jewel", rng_=rr, claw_r=0.45)
        K.plate("riser_pupil%d" % z, almond(bx, yc, 1.1, 4.4, n=6, p=1.2), z * 6.1, z * 6.7, P["pupil"], part="riser_pupil", bevel_w=0.0)
    for k, (a, ln, r0) in enumerate(((-36, 15.0, 2.4), (0, 21.0, 2.8), (36, 15.0, 2.4))):
        c, sn = math.cos(math.radians(a)), math.sin(math.radians(a))
        b0 = V((bx + 4.0 * c, yc + 8.0 * sn * 0.9, 0.0))
        d = V((c, sn, 0.0))
        K.tapered("riser_crown%d" % k, [b0 - d * 2.0, b0 + d * ln * 0.4 + V((0, sn * 0.6, 0)), b0 + d * ln * 0.75 + V((0, sn * 2.0, 0)), b0 + d * ln + V((0, sn * 4.0, 0))], r0, 0.08, P["steel"], sides=4, sub=2, part="riser_crown")
    F.string(sh, P["string"], 0.4)
    F.arrow(sh, P["ink"], P["steel"], P["ink"], 3.3, "barb", cap_mat=P["iron"])
    return sh


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--trial")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--lowres", action="store_true")
    ap.add_argument("--samples", type=int, default=16)
    ap.add_argument("--no-check", action="store_true")
    a = ap.parse_args(argv)
    F.PLACE = PLACE                                    # finish_bow reads the module's PLACE
    sh = bow_of_pride({"samples": a.samples})
    if not a.no_check:
        V3.connectivity()
    print("   parts:", K.quad_table(20))
    print("[legendary_bow_curse] built geometry: ~%d body quads" % K.quad_estimate())
    if a.quick:
        return
    F.finish_bow("legendary_bow_curse", sh, a.trial, a.lowres, GLOW)


if __name__ == "__main__":
    main(sys.argv[1:])

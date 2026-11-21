"""Style wave 3 of the LEGENDARY SWORD form phoenix_sword (docs/heroweapon/style/wave3.md). REPLACEMENT of phoenix_sword() in forms_exemplars.py, which stays untouched; the previous mesh and
skin are kept in weapon_meshes/classic_style3/ and textures/entity/weapon/classic_style3/ (client option vassalForms.classicStyleWave3).

    python3 -B tools/blender/forms_phoenix_w3.py [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--bake N] [--skin N]

The old sword was the exemplar of "everything": gold heart guard with ruby, gold scroll arms, two three-tier wings, plumage plates on both faces, runes, ember cloud, three tail plumes (sameness 61.1).
Now (style guide, Phoenix Gale Blade): ONE blade of hot metal graded from black at the guard to red to ember yellow at the tip, painted into the skin (no gold anywhere); a flame-cut flamberge edge with
unequal notches; ONE swept feather guard, five graded feathers sweeping back from the left and a short plain quillon on the right (not a pair); a tiny ruby; a leather-wrapped grip; a pommel with one
plume. Fire glow only: the fuller line and the feather tips. Same place line, same blade span (y 32..150), same grip (y 8.5..26.5).
Palette: heat-steel + black iron + hide + utility plume feather + ONE named accent (fire glow); wave2_kit.Palette.
"""
import math
import os
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import wave3_kit as W3  # noqa: E402
import forms_kit as K  # noqa: E402
import wave2_kit as W  # noqa: E402
from forms_kit import rgb, TAU  # noqa: E402
from mathutils import Vector  # noqa: E402


def V(*a):
    return Vector(a[0]) if len(a) == 1 else Vector(a)


MESH_ID = "legendary_sword__phoenix_sword"


def palette():
    P = W.Palette("fire", ["steel", "iron", "wrap"], ["plume"])
    P["steel"] = K.surface("px3_steel", rgb(70, 18, 12), 0.9, 0.34, wear=1.0, wear_color=rgb(255, 176, 80), wear_metal=0.7, dirt=0.9, dirt_color=(0.01, 0.003, 0.003, 1), scratches=0.8, bump="hammer",
                           bump_scale=0.5, bump_strength=0.22, mottle=1.0, mottle_color=rgb(24, 6, 6), fine=0.6, seed=931, bevel=0.2,
                           cramp=("y", 30.0, 152.0, [(0.0, rgb(26, 12, 12)), (0.22, rgb(60, 14, 10)), (0.6, rgb(150, 34, 12)), (0.88, rgb(214, 84, 20)), (1.0, rgb(255, 168, 60))]), ao_dist=2.5)
    P["iron"] = W.iron("px3_iron", rgb(40, 28, 28), seed=932, mottle_color=rgb(60, 24, 16), wear_color=rgb(190, 150, 120))
    P["wrap"] = W.hide("px3_wrap", rgb(78, 22, 18), seed=933, bump="cross", bump_strength=0.8, bevel=0.0)
    P["plume"] = K.surface("px3_plume", rgb(190, 34, 12), 0.2, 0.5, wear=0.5, wear_color=rgb(255, 210, 90), mottle=0.5, mottle_color=rgb(100, 14, 6), dirt=0.3, feather=(11, 4, 0.55, 0.5),
                           tramp=[(0.0, rgb(60, 10, 8)), (0.45, rgb(190, 42, 14)), (1.0, rgb(255, 150, 40))], seed=934, bevel=0.08, fine=0.4)
    P["fire"] = K.surface("px3_fire", rgb(255, 120, 20), 0.0, 0.5, emit=rgb(255, 130, 24), emit_strength=1.7, emit_flicker=0.9, seed=935, bevel=0.0,
                          emit_ramp=("y", 32.0, 150.0, [(0.0, rgb(255, 60, 8)), (0.6, rgb(255, 140, 30)), (1.0, rgb(255, 224, 120))]))
    return P.check()


def phoenix_sword(opts):
    K.begin(opts["samples"])
    P = palette()
    rr = K.rng(9031)
    ymid = 31.0

    def hw(y):
        return K.interp([(32, 5.6), (40, 6.2), (56, 5.6), (84, 5.5), (110, 5.0), (128, 4.2), (140, 3.0), (147, 1.6), (150, 0.5)], y)

    def bend(y):                                                         # one lazy S and a tip that flicks left
        return 0.9 * math.sin((y - 32) / 118 * math.pi * 1.3) + 4.0 * max(0.0, (y - 118) / 32.0) ** 2.2
    stL = [(y, hw(y)) for y in (32, 40, 56, 84, 110, 128, 140, 147, 150)]
    notchL = [(52, 1.2, 4.0, "tooth"), (79, 1.6, 5.0, "tooth"), (101, 1.0, 3.0, "chip"), (124, 1.4, 4.0, "v")]
    notchR = [(46, 0.9, 3.0, "v"), (66, 1.7, 5.0, "tooth"), (90, 1.0, 3.0, "step"), (112, 1.5, 4.5, "tooth"), (134, 0.8, 2.6, "v")]
    L = [V((p.x + bend(p.y), p.y, 0)) for p in K.blade_edge(-1, stL, 32, 150, notches=notchL, jitter=0.12, res=11.0, seed=41, tip=(-1.2, 151.0))]
    R = [V((p.x + bend(p.y), p.y, 0)) for p in K.blade_edge(1, stL, 32, 150, notches=notchR, jitter=0.12, res=11.0, seed=42, tip=(0.6, 149.0))]
    K.blade("blade", P["steel"], L, R, thick=lambda t: 1.3 * (1.0 - 0.45 * t ** 2.2), fuller=0.2, fuller_depth=0.5, fuller_off=0.1, seed=3, wob=0.14, bevel_w=0.2)
    for s in (1, -1):                                                    # the one glowing line: the fuller, a hot channel (baked + glow)
        pts = [(bend(y) + 0.1 * hw(y) + 0.25 * math.sin(y * 0.4), y) for y in range(42, 134, 4)]
        K.seam("fuller_fire%d" % s, pts, P["fire"], w=0.45, depth=0.12, z=1.3 * 0.5 * s, side=s, part="blade_glow", detail=True)
    # the guard: a short plain iron quillon on the right, ONE swept fan of five graded feathers on the left, a tiny ruby on the plate
    for s in (1, -1):
        K.stepped_plate("guard_plate%d" % s, [(-4.2, ymid - 2.0), (4.4, ymid - 2.0), (5.0, ymid + 2.2), (2.0, ymid + 4.4), (-3.0, ymid + 4.0), (-4.8, ymid + 1.0)], 1.0 * s, P["iron"], steps=2, step_h=0.7,
                        inset=0.9, part="guard", up=s)
    K.bar("quillon", (3.6, ymid, 0), (11.5, ymid + 0.6, 0.2), 1.9, 1.5, P["iron"], part="guard", taper=0.55)
    K.gem_setting("ruby", V((0.4, ymid + 1.1, 1.9)), 1.15, P["fire"], P["iron"], normal=(0, 0, 1), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.3)
    for k, (ang, ln, wd) in enumerate(((158, 27.0, 5.0), (172, 24.0, 4.6), (186, 20.5, 4.4), (201, 16.5, 4.0), (216, 12.0, 3.6))):
        a = math.radians(ang)
        K.feather("guardf%d" % k, V((-3.6, ymid + 1.4 - 0.7 * k, 0.5 * (1 if k % 2 else -1))), V(math.cos(a), math.sin(a) * 0.55 + 0.22, 0.0), ln, wd, P["plume"], facing=(0, 0, 1), curl=1.5 * (1 if k % 2 else -1),
                  serrate=0.2, rows=6, thick=0.13, seed=600 + k, part="guard_feathers", bend=V(-1.0, 1.4 + 0.3 * k, 0.0))
    # grip: leather wrap with cord, two iron bands at uneven stations, a plain pommel with ONE plume
    K.grip_wrapped("grip", 8.5, 26.5, 2.0, P["wrap"], P["wrap"], turns=8.0, cord=0.34, bands=(10.4, 24.6), band_mat=P["iron"], swell=0.14, part="grip", seed=3)
    pom = K.bl.lathe("pommel", [(0.01, 0.0), (1.8, 0.3), (3.0, 1.6), (3.1, 3.4), (2.4, 5.4), (2.3, 7.6), (2.5, 8.2)], P["iron"], sides=10, part="pommel")
    K._finish_obj(pom, "pommel")
    K.feather("tailplume", V(0.4, 0.5, -0.8), V(0.45, -1.0, 0.1), 17.0, 5.6, P["plume"], facing=(0, 0, 1), curl=1.6, serrate=0.22, rows=7, thick=0.1, seed=611, part="pommel_plume", bend=V(1.2, 0.0, 1.0))
    return K.quad_estimate()


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--bake", type=int, default=2048)
    ap.add_argument("--skin", type=int, default=1024)
    a = ap.parse_args(argv)
    W3.use_classic_headers()
    t0 = time.time()
    q = phoenix_sword({"samples": a.samples})
    print("   parts:", K.quad_table(16))
    print("[%s] built geometry: ~%d body quads in %.1f s" % (MESH_ID, q, time.time() - t0))
    if a.beauty:
        os.makedirs(a.beauty, exist_ok=True)
        for view in ("three_quarter", "front"):
            K.beauty(os.path.join(a.beauty, "phoenix_%s.png" % view), view=view, samples=max(8, a.samples // 2), size=(560, 900))
    if not a.quick:
        K.finish_form(MESH_ID, "legendary_sword", trial=a.trial, bake=a.bake, skin=a.skin)
        if not a.trial:
            K.write_manifest(W3.MANIFEST, {MESH_ID: {"weapon": "legendary_sword", "form": "phoenix_sword", "class": "longsword"}})


if __name__ == "__main__":
    main(sys.argv[1:])

"""Style wave 2 of the LEGENDARY SHIELD form "light_metal_shield" (docs/heroweapon/style/wave2.md). REPLACEMENT of lightmetal_shield() in forms_legendary_shield.py, which stays in the
repo untouched; the previous mesh and skin are kept in weapon_meshes/classic_style/ and textures/entity/weapon/classic_style/.

    python3 -B tools/blender/forms_shield_w2.py [--quick] [--samples N] [--trial DIR]

The audit found the old form at sameness 69.1: a symmetric kite with 20 even flutes, twelve even gold rays, a gold star boss with a gem, mirrored 5+5 wings and three hovering minis.
Now: a plain brushed-silver kite with ONE hero, a single long swept wing of light-metal blades on the LEFT (a short folded 3-blade wing on the right, not a mirror), a keel ridge that
leans (not centred), two hammered dents, a dark-steel rim bead with unevenly spaced rivets, ONE cyan stone sunk in a plain round boss, and two minis of different size (one lit).
Palette: brushed silver + dark steel + leather + ONE named accent (cyan). No gold. Same kite outline and height (y -34..34), same back gear, same part names (body, beads, star, gem, wings, minis).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import forms_legendary_shield as FS  # noqa: E402
import forms_kit as K  # noqa: E402
import wave2_kit as W  # noqa: E402
from forms_legendary_shield import V, TAU, smoothstep, poly_outline, dish, band, rope_section, loop_pts, back_gear  # noqa: E402
from bl_lib import rgb  # noqa: E402


def lightmetal_shield_w2():
    P = W.Palette("cyan", ["silver", "dark", "leather"], ["silver2"])
    P["silver"] = W.steel("lm_silver", rgb(206, 213, 226), seed=181, rough=0.2, bevel=0.2, ao_dist=3.0, mottle_color=rgb(150, 164, 190), wear=0.8, scratches=0.7, dirt=0.6)
    P["silver2"] = W.steel("lm_silver_dark", rgb(150, 160, 178), seed=182, bevel=0.14, ao_dist=2.5, mottle_color=rgb(96, 108, 132), dirt=0.8)
    P["dark"] = W.iron("lm_dark", rgb(58, 64, 78), seed=183, mottle_color=rgb(84, 70, 62), bevel=0.14)
    P["cyan"] = W.accent_gem("lm_cyan", rgb(70, 196, 235), rgb(100, 225, 255), 0.8, seed=184)
    P["leather"] = W.hide("lm_leather", rgb(58, 52, 50), seed=186)
    P.check()
    rr = K.rng(7707)
    kite = [(0, 34), (13, 28), (24, 18), (27, 6), (23, -8), (13, -22), (3, -30), (0, -34), (-3, -30), (-13, -22), (-23, -8), (-27, 6), (-24, 18), (-13, 28)]
    outline = poly_outline(kite, smooth=8)
    dents = ((10.0, -9.0, 4.6, 0.9), (-8.0, -19.0, 3.4, 0.7))

    def zf(s, a):
        o = outline(a)
        x, y = s * o[0], s * o[1]
        keel = 1.5 * math.exp(-((x + 0.11 * y) / 5.5) ** 2) * smoothstep(0.1, 0.4, s) * (1.0 - smoothstep(0.72, 0.84, s))
        dent = sum(dp * math.exp(-(((x - dx) ** 2 + (y - dy) ** 2) / (dr * dr))) for dx, dy, dr, dp in dents)
        return 6.2 * (1.0 - s ** 1.7) + keel - dent + 1.3 * math.exp(-((s - 0.92) / 0.05) ** 2)

    def zb(s, a):
        return -2.8 - 5.0 * (1.0 - s ** 2)
    dish("lmbody", outline, zf, zb, P["silver"], ns=13, nb=3, nth=52, part="body", angle=55.0)
    n = 44
    band("lm_bead_out", loop_pts(outline, 0, n=n, s=0.965, zf=lambda a: zf(0.965, a) + 0.45), rope_section(1.35, 6, 0.9), P["dark"], part="beads", angle=55.0)
    band("lm_bead_in", loop_pts(outline, 0, n=n, s=0.84, zf=lambda a: zf(0.84, a) + 0.35), rope_section(0.7, 5, 0.9), P["silver2"], part="beads", angle=55.0)
    rv = (0, 3, 7, 10, 15, 18, 23, 27, 30, 36, 39)                                                  # rivets at uneven steps, plain dark iron
    K.rivets("lm_rivets", [(V((outline(TAU * i / n)[0] * 0.965, outline(TAU * i / n)[1] * 0.965, zf(0.965, TAU * i / n) + 1.5)), (0, 0, 1)) for i in rv], P["dark"], r=0.8, detail=False, sides=6, rng_=rr, jitter=0.12)
    # the boss: a plain round stepped plate and ONE cyan stone, sunk with four claws (the accent)
    z0 = zf(0.0, 0.0) + 0.2
    disc = [(7.6 * math.cos(TAU * k / 14), 7.6 * math.sin(TAU * k / 14)) for k in range(14)]
    K.stepped_plate("lm_star", disc, z0, P["dark"], steps=2, step_h=0.8, inset=1.4, part="star", bevel_w=0.2)
    K.gem_setting("lm_jewel", (0.0, 0.0, z0 + 2.1), 2.6, P["cyan"], P["dark"], normal=(0, 0, 1), claws=4, sides=8, part="gem", rng_=rr, claw_r=0.36)
    # the hero: ONE long swept wing of light-metal blades on the left (5, graded, alternating brushed / dark), a short folded wing of 3 on the right
    for sx, count, L0, dl in ((-1, 5, 25.5, 3.4), (1, 3, 17.0, 3.2)):
        for k in range(count):
            base = V((sx * (22.0 + 0.6 * k), 9.0 - 5.6 * k, 1.0 - 0.8 * k))
            d = V((sx * 1.0, 0.5 - 0.3 * k + (0.18 if sx > 0 else 0.0), -0.15 - 0.05 * k))
            K.feather("lm_wing%d_%d" % (sx > 0, k), base, d, L0 - dl * k + (1.2 if k % 2 else 0.0), 6.4 - 0.5 * k, P["silver2" if k % 2 else "silver"], shaft_material=P["dark"], curl=0.7 if sx < 0 else 0.4,
                      serrate=0.1, rows=7, thick=0.45, seed=k + 7 * sx, part="wings", ridge=1.8)
    # two hovering minis of unequal size; only the big one carries the glow
    for i, (ox, oy, sc) in enumerate(((-33.0, 22.0, 0.2), (33.0, -4.0, 0.12))):
        pts = [(ox + sc * x, oy + sc * y) for x, y in ((0, 34), (24, 18), (27, 0), (13, -22), (0, -34), (-13, -22), (-27, 0), (-24, 18))]
        K.stepped_plate("lm_mini%d" % i, pts, 4.0 + i, P["silver"], steps=2, step_h=0.6, inset=0.5, part="minis", bevel_w=0.14)
        if i == 0:
            K.stepped_plate("lm_minic%d" % i, [(ox + sc * 0.45 * x, oy + sc * 0.45 * y) for x, y in ((0, 34), (24, 18), (27, 0), (13, -22), (0, -34), (-13, -22), (-27, 0), (-24, 18))], 5.5 + i, P["cyan"], steps=1, step_h=0.4, part="minis", bevel_w=0.08)
    back_gear(P, -7.8, y0=-9.0, rr=rr, strap_m=P["leather"], metal=P["dark"])
    return K.quad_estimate()


FS.BUILDERS["lightmetal"] = (lightmetal_shield_w2, "legendary_shield__light_metal_shield", "light_metal_shield",
                             {"weights": {"back": 0.4, "body": 1.0, "wings": 1.0, "star": 1.3}, "glow": ("A8F0FF", 0.12, 0.45, 1.0), "focus": (-12.0, 24.0)})


def main(argv):
    FS.main(["--only", "lightmetal"] + list(argv))


if __name__ == "__main__":
    main(sys.argv[1:])

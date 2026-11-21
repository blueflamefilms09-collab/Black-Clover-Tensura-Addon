"""Style wave 3 of the VASSAL MIRROR form yata_mirror (docs/heroweapon/style/wave3.md). REPLACEMENT of yata_mirror() in forms_vassal_mirror.py, which stays untouched; the previous mesh and skin are
kept in weapon_meshes/classic_style3/ and textures/entity/weapon/classic_style3/ (client option vassalForms.classicStyleWave3).

    python3 -B tools/blender/forms_mirror_w3.py [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--bake N]

The audit (style guide, row 19): "Sacred bronze mirror: a large round disc with a heavy engraved rim and a worn handle, a different outline from the white tiger and envious mirrors, delete gold dots".
The old Yata mirror was a gold octagon with eight kaleidoscope glass facets, sixteen sun rays + sixteen corona rays, eight red corner jewels, 16 studs, a rope and four paper streamers.
Now: a sacred BRONZE mirror (no gold) with the eight-lobed outline of the old stories (eight rounded lobes, three of them proud, one chipped), a heavy patinated rim ring, a polished face, ONE
engraved sun lit amber (a ring and eight rays of unequal length), one half-circle of straw shimenawa rope with three paper shide of unequal length, a worn red-cord handle. No rays outside the
outline, no studs, no jewels. Same place line, same handle (grip y 6..23.6), same head height class (top y < 86.8).
Palette: bronze + patina + straw + utility paper and red cord + ONE named accent (the lit sun); wave2_kit.Palette.
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
import forms_vassal_common as C  # noqa: E402
import forms_vassal_lib as Lb  # noqa: E402
import forms_vassal_mirror as FM  # noqa: E402  (its builders stay registered and untouched)
from forms_vassal_run import main  # noqa: E402

from mathutils import Vector  # noqa: E402


def V(*a):
    return Vector(a[0]) if len(a) == 1 else Vector(a)


BASE = "vassal_mirror"


def palette():
    P = W.Palette("sun", ["bronze", "patina", "straw"], ["paper", "red"])
    P["bronze"] = W.iron("ym3_bronze", rgb(176, 124, 60), seed=941, rough=0.3, wear=0.8, wear_color=rgb(246, 214, 140), mottle_color=rgb(110, 84, 40), dirt=0.6, scratches=0.18)
    P["patina"] = W.iron("ym3_patina", rgb(70, 94, 78), seed=942, rough=0.5, wear=0.7, wear_color=rgb(170, 150, 90), mottle_color=rgb(40, 70, 60), dirt=0.8)
    P["straw"] = W.cord("ym3_straw", rgb(204, 172, 96), seed=943)
    P["paper"] = W.paper("ym3_paper", rgb(238, 234, 222), seed=944)
    P["red"] = W.hide("ym3_red", rgb(140, 30, 28), seed=945, bump="cross", bump_strength=0.7)
    P["sun"] = W.accent_glow("ym3_sun", rgb(255, 176, 60), strength=1.5, seed=946)
    P["grip_core"] = P["patina"]
    P["grip_wrap"] = P["red"]
    P["trim"] = P["bronze"]
    P["drop"] = P["red"]
    return P.check()


def yata_mirror_w3(opts):
    C.begin(opts)
    C.weights(head_glass=1.2, head_rim=1.0, sun=1.1, rope=0.9, paper=0.8)
    P = palette()
    rr = K.rng(9041)
    cy = 52.0
    amp = (1.7, 1.2, 1.9, 1.0, 1.6, 0.7, 1.8, 1.3)                                               # lobe heights: unequal, one (index 5) chipped low

    def outline(scale=1.0, n=96):
        pts = []
        for i in range(n):
            a = TAU * i / n
            k = int(((a + TAU / 16) % TAU) / (TAU / 8))
            c = abs(math.cos(4.0 * a)) ** 0.55
            pts.append((scale * (10.9 + amp[k % 8] * c) * math.cos(a), cy + scale * (10.9 + amp[k % 8] * c) * math.sin(a)))
        return pts
    FM.handle(P, rr, wrap_turns=5.0, collar_mat=P["bronze"])
    K.plate("back", outline(0.97), -2.3, -1.0, P["patina"], part="back_plate", bevel_w=0.2)
    K.plate("body", outline(1.0), -1.0, 0.5, P["bronze"], part="head_rim", bevel_w=0.18)
    K.plate("face", FM.ell(0.0, cy, 8.9, 8.9, 40), 0.5, 0.8, P["bronze"], part="head_glass", bevel_w=0.1)
    Lb.ring_torus("rim", (0.0, cy, 0.9), 9.7, 1.0, P["patina"], "head_rim", sides=7, segs=48)
    # the one hero: an engraved sun lit amber: a ring and eight rays of unequal length (bake + glow, flat on the face)
    Lb.ring_torus("sun_ring", (0.0, cy, 0.85), 2.9, 0.28, P["sun"], "sun", sides=5, segs=24)
    for k, ln in enumerate((4.6, 3.0, 5.4, 3.4, 4.2, 2.8, 5.0, 3.6)):
        a = TAU * k / 8 + 0.2
        Lb.lancet("sun_ray%d" % k, V((3.5 * math.cos(a), cy + 3.5 * math.sin(a), 0.0)), V((math.cos(a), math.sin(a), 0.0)), ln, 1.1, P["sun"], thick=0.12, part="sun", z=0.86, n=4)
    # ONE half-circle of straw shimenawa on the lower left with three paper shide of different length
    arc = []
    for i in range(25):
        t = i / 24.0
        a = math.radians(150.0 + 190.0 * t)
        rr_ = 11.8 + 0.35 * math.sin(t * TAU * 5.0)
        arc.append(V((rr_ * math.cos(a), cy + rr_ * math.sin(a), 1.5 + 0.3 * math.cos(t * TAU * 5.0))))
    K.tube("rope", arc, 0.62, P["straw"], sides=6, sub=1, part="rope")
    for k, (ti, ln) in enumerate(((0.18, 9.0), (0.5, 6.0), (0.82, 7.5))):
        top = arc[int(ti * 24)]
        path = [top + V((0.7 * math.sin(j * 1.5) * (1 if k % 2 else -1), -j * ln / 6.0, 0.5 * math.cos(j * 1.5))) for j in range(7)]
        K.ribbon("shide%d" % k, path, 2.2, P["paper"], facing=(0, 0, 1), thick=0.06, part="paper", fray=0.5, rng_=rr, edge_wave=0.15)
    return K.quad_estimate()


BUILDERS = {"yata": (yata_mirror_w3, "vassal_mirror__yata_mirror", "yata_mirror")}

if __name__ == "__main__":
    W3.use_classic_headers()
    main(sys.argv[1:], BUILDERS, BASE, "shield", W3.MANIFEST)

"""Style wave 2 of the VASSAL STAFF forms: rune_staff and archmage_staff (docs/heroweapon/style/wave2.md). REPLACEMENT of the builders in forms_vassal_staff.py, which stay in the
repo untouched (the base staff keeps its place line, so these keep the same hand and size class). The previous meshes and skins are kept in weapon_meshes/classic_style/.

    python3 -B tools/blender/forms_staff_w2.py --only rune [--trial DIR] [--quick] [--samples N] [--bake PX]

Idea / not-like (guide rule 10):
  rune_staff      the one with a wooden crook that holds ONE hanging iron lantern with an amber rune stone; NOT a gold lantern head with seven orbiting stones and rune plates all round.
  archmage_staff  the one with a tall violet crystal spire in an uneven iron claw cradle and the Dipper as seven iron stars on one curved arm; NOT gold stars + echo shards + tendrils + halo.
Palette: wood + iron + cloth, accent amber (rune) / violet (archmage). Glow on the one stone only (the spire / the rune stone). No gold.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
import forms_v_common as C  # noqa: E402
import wave2_kit as W  # noqa: E402
from forms_kit import rgb, TAU  # noqa: E402
from forms_vassal_common_b import V  # noqa: E402

BASE = "vassal_staff"


def shaft(P, top, wrap=(70.0, 128.0), tail=True, seed=1):
    """A dark wood pole thinner at the foot, ONE cloth wrap with a hanging tail, an iron ferrule (the shared lower staff of both forms)."""
    pts = [V(0, -9.0, 0), V(0, 0.0, 0), V(0.3, 90.0, 0.2), V(0.0, top, 0)]
    K.tube("shaft", [tuple(p) for p in pts], 1.0, P["wood"], sides=10, sub=4, part="shaft", taper=(2.9, 4.2))
    C.lathe_part("ferrule", [(0.2, -9.0), (1.5, -6.5), (2.9, -1.5), (3.3, 3.0), (2.9, 9.0)], P["iron"], sides=10, part="ferrule")
    for o in K.grip_wrapped("grip", wrap[0], wrap[1], 4.4, P["wood"], P["cloth"], turns=10.0, cord=0.55, bands=(), band_mat=P["iron"], swell=0.06, part="grip", seed=seed):
        pass
    if tail:
        K.ribbon("wrap_tail", [V(4.4, wrap[1] - 4.0, 1.0), V(7.2, wrap[1] - 16.0, 2.0), V(6.0, wrap[1] - 30.0, 2.6), V(8.6, wrap[1] - 42.0, 1.6)], 2.0, P["cloth"], facing=(1, 0, 0), thick=0.08, part="grip", fray=0.5, rng_=K.rng(seed + 3))


def star_plate(name, cx, cy, r, mat, part="stars"):
    pts, n = [], 4
    for k in range(2 * n):
        a = math.pi * k / n + math.pi / 2
        rr = r if k % 2 == 0 else r * 0.38
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return K.stepped_plate(name, pts, -0.45, mat, steps=2, step_h=0.45, inset=0.35, part=part, up=1, bevel_w=0.0) + K.stepped_plate(name + "b", pts, 0.45, mat, steps=2, step_h=0.45, inset=0.35, part=part, up=-1, bevel_w=0.0)


# ================================================================ rune staff: a crook with one hanging lantern
def rune_staff(opts):
    C.begin(opts)
    C.weights(shaft=1.0, lantern=1.2, stone=1.1, runes=0.9, grip=0.9, crook=1.1)
    P = W.Palette("amber", ["wood", "iron", "cloth"], ["slate"])
    P["wood"] = W.wood("rs_wood", rgb(92, 66, 46), seed=211)
    P["iron"] = W.iron("rs_iron", rgb(60, 62, 72), seed=212, mottle_color=rgb(86, 62, 48))
    P["cloth"] = W.cord("rs_cloth", rgb(122, 76, 44), seed=213)
    P["slate"] = K.surface("rs_slate", rgb(96, 92, 90), 0.0, 0.75, bump="grain", bump_scale=1.2, bump_strength=0.5, mottle=0.9, mottle_color=rgb(52, 50, 52), wear=0.6, wear_color=rgb(160, 152, 144),
                           wear_metal=0.0, dirt=0.8, fine=0.6, seed=214, bevel=0.1)
    P["amber"] = W.accent_gem("rs_amber", rgb(255, 170, 60), rgb(255, 160, 50), 1.1, seed=215, transmission=0.0)
    P["scratch"] = W.scratch("rs_scratch")
    P.check()
    # ---- shaft up to y 282, then a shepherd's crook sweeping to the left: the lantern hangs from its tip
    shaft(P, 282.0, seed=5)
    crook = [V(0.0, 270.0, 0), V(-1.2, 296.0, 0), V(-9.0, 318.0, 0), V(-23.0, 327.0, 0), V(-33.0, 320.0, 0), V(-35.0, 311.0, 0)]
    K.tube("crook", [tuple(p) for p in crook], 1.0, P["wood"], sides=9, sub=4, part="crook", taper=(4.0, 2.2))
    for y, k in ((279.0, 1.0), (286.0, 0.8)):                                              # two iron bands where the crook is scarfed on: unequal
        C.lathe_part("scarf%d" % int(y), [(3.6 * k, y - 1.4), (4.9 * k, y - 0.9), (4.9 * k, y + 0.9), (3.6 * k, y + 1.4)], P["iron"], sides=10, part="crook")
    # three slate rune plates on the shaft, at uneven heights and angles (the runes are scored, not lit)
    for k, (a, y) in enumerate(((0.3, 168.0), (2.5, 176.0), (4.4, 160.0))):
        pl = K.plate("rune_plate%d" % k, [(-2.0, -5.0), (2.0, -5.0), (2.2, 5.0 + k), (-2.2, 5.0 + k)], 0.0, 0.9, P["slate"], part="runes", bevel_w=0.15)
        K.xf(pl, K.frame(V(3.4 * math.cos(a), y, 3.4 * math.sin(a)), V(math.cos(a), 0, math.sin(a)), (0, 1, 0)))
        t = K.bl.text("rune_t%d" % k, K.RUNES[k * 4], 3.4, P["scratch"], (0, 0, 0), extrude=0.05, font=K.FONT_RUNIC, detail=True, part="runes")
        K.xf(t, K.frame(V(3.4 * math.cos(a), y, 3.4 * math.sin(a)) + V(math.cos(a), 0, math.sin(a)) * 0.95, V(math.cos(a), 0, math.sin(a)), (0, 1, 0)))
    # ---- the lantern: an iron ring and a short chain from the crook tip, four posts of unequal weight, a cap, a floor, the amber stone inside
    lx, ly = -35.0, 311.0
    C.ring_torus("hang_ring", V(lx, ly - 1.0, 0), 2.6, 0.55, P["iron"], normal=(0, 0, 1), sides=6, segs=14, part="lantern")
    C.ring_torus("hang_ring2", V(lx, ly - 4.2, 0), 2.2, 0.5, P["iron"], normal=(1, 0, 0), sides=6, segs=14, part="lantern")
    base_y = ly - 33.0
    C.lathe_part("lan_cap", [(0.3, ly - 6.5), (2.2, ly - 7.0), (5.6, ly - 9.2), (8.6, ly - 11.4), (8.2, ly - 13.2)], P["iron"], sides=8, part="lantern", origin=(lx, 0, 0))
    C.lathe_part("lan_floor", [(8.0, base_y + 4.4), (7.0, base_y + 2.0), (3.0, base_y), (0.3, base_y - 3.6)], P["iron"], sides=8, part="lantern", origin=(lx, 0, 0))
    for k, (a, rr_) in enumerate(((0.4, 1.35), (2.1, 1.05), (3.5, 1.35), (5.2, 0.9))):
        K.tapered("post%d" % k, [V(lx + 8.0 * math.cos(a), base_y + 4.0, 8.0 * math.sin(a)), V(lx + 9.2 * math.cos(a), base_y + 14.0, 9.2 * math.sin(a)), V(lx + 8.2 * math.cos(a), ly - 13.0, 8.2 * math.sin(a))], rr_, rr_ * 0.8, P["iron"], sides=6, sub=2, part="lantern")
    st = K.bl.lathe("rune_stone", [(0.05, base_y + 6.5), (4.6, base_y + 10.5), (5.2, base_y + 17.0), (4.4, base_y + 22.5), (0.05, ly - 15.0)], P["amber"], sides=6, part="stone", origin=(lx, 0, 0))
    K._finish_obj(st, "stone", True, 30.0)
    return K.quad_estimate()


# ================================================================ archmage staff: a violet spire in an uneven cradle, the Dipper on one arm
def archmage_staff(opts):
    C.begin(opts)
    C.weights(shaft=1.0, cradle=1.1, spire=1.3, stars=1.0, grip=0.9, arm=0.9)
    P = W.Palette("violet", ["wood", "iron", "cloth"], ["silver"])
    P["wood"] = W.wood("am_wood", rgb(58, 44, 52), seed=221, mottle_color=rgb(34, 26, 34))
    P["iron"] = W.iron("am_iron", rgb(52, 54, 70), seed=222, mottle_color=rgb(72, 56, 70))
    P["cloth"] = W.cord("am_cloth", rgb(54, 56, 110), seed=223)
    P["silver"] = W.steel("am_silver", rgb(176, 182, 200), seed=224, bevel=0.1)
    P["violet"] = W.accent_gem("am_violet", rgb(170, 100, 240), rgb(190, 110, 255), 1.0, seed=225, transmission=0.0)
    P.check()
    shaft(P, 236.0, wrap=(76.0, 140.0), seed=7)
    C.lathe_part("top_collar", [(3.9, 230.0), (5.6, 233.0), (6.2, 240.0), (4.6, 246.0)], P["iron"], sides=12, part="cradle")
    # ---- the spire: a tall faceted crystal (the hero), leaning a few degrees, held by five claws of unequal reach
    sp = K.bl.lathe("spire", [(0.05, 238.0), (5.4, 250.0), (7.6, 270.0), (6.0, 292.0), (3.2, 308.0), (0.05, 326.0)], P["violet"], sides=6, part="spire")
    K._finish_obj(sp, "spire", True, 30.0)
    K.xf(sp, K.Matrix.Translation(V(0, 238.0, 0)) @ K.Matrix.Rotation(math.radians(-5.0), 4, "Z") @ K.Matrix.Translation(V(0, -238.0, 0)))
    for k, (a, reach) in enumerate(((0.2, 1.0), (1.4, 0.7), (2.8, 1.15), (4.0, 0.85), (5.2, 0.6))):
        ca, sa = math.cos(a), math.sin(a)
        K.tapered("claw%d" % k, [V(3.5 * ca, 242.0, 3.5 * sa), V(9.5 * ca, 249.0, 9.5 * sa), V(10.0 * ca, 249.0 + 24.0 * reach, 10.0 * sa), V(6.0 * ca, 252.0 + 32.0 * reach, 6.0 * sa)], 1.7, 0.5, P["iron"], sides=5, sub=3, part="cradle")
    # ---- THE DIPPER on one curved arm to the right: seven small iron stars (graded 6.4 .. 3.4), thin iron links, only these are not on the centre line
    arm = [V(4.0, 244.0, 0), V(14.0, 252.0, 0), V(26.0, 262.0, 0), V(34.0, 278.0, 0), V(30.0, 294.0, 0)]
    K.tube("arm", [tuple(p) for p in arm], 1.0, P["iron"], sides=6, sub=4, part="arm", taper=(1.3, 0.6))
    stars = [(14.0, 253.0, 4.4), (25.0, 262.0, 5.0), (33.5, 275.0, 6.4), (32.0, 290.0, 4.0), (45.0, 284.0, 5.6), (47.5, 270.0, 3.6), (41.0, 261.0, 3.4)]
    for i, (x, y, r) in enumerate(stars):
        star_plate("star%d" % i, x, y, r, P["silver"])
    for a, b in ((0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 3)):
        K.tube("link%d%d" % (a, b), [(stars[a][0], stars[a][1], 0.0), ((stars[a][0] + stars[b][0]) / 2.0, (stars[a][1] + stars[b][1]) / 2.0 + 0.8, 0.0), (stars[b][0], stars[b][1], 0.0)], 0.3, P["iron"], sides=4, sub=2, part="arm")
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["rune"] = (rune_staff, "vassal_staff__rune_staff", BASE, "rune_staff", "staff")
BUILDERS["archmage"] = (archmage_staff, "vassal_staff__archmage_staff", BASE, "archmage_staff", "staff")

if __name__ == "__main__":
    C.run_family(sys.argv[1:], BUILDERS, "vassal_staff", bake_default=1024)

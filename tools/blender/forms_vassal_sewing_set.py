"""Weapon-form models of the VASSAL SEWING SET (S'yne's needle and thread), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_sewing_set.py --only silk [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N] [--sheet]

mesh ids: vassal_sewing_set__silk_thread_set, __spider_wire_set, __ariadnes_thread. Every form keeps the base set's place line (class `rapier`, grip 0.188): a thimble-and-spool
stand you hold at y 0..40 and a needle standing up to y ~110, the scissors lying to the -x side. The silhouette of each: silk = an ornate needle with a long eye and a bow of thread,
a ball of wool and stork shears; spider = a web disc, spider-leg cage, a fang needle and pincer shears; Ariadne = a labyrinth disc, bull horns, a golden needle with a star and the red thread.
Sources: the form names and docs/heroweapon/weapon_sewingset.md (the wikis cannot be opened from the build machine).
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
import forms_v_common as C  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

BASE = "vassal_sewing_set"
Y0 = -11.6


# ---------------------------------------------------------------- shared parts
def stand(M, rr, *, thimble_prof=None, spool_r=7.2, flange_r=9.0, y_spool=(6.0, 38.0), prefix=""):
    """Thimble pedestal (y -11.6 .. 8), spool with two flanges, cap: the part the hand holds."""
    tp = thimble_prof or [(4.2, Y0), (6.8, Y0 + 1.5), (7.8, -3.0), (7.4, 3.0), (6.4, 8.5)]
    C.lathe_part(prefix + "thimble", tp, M["thimble"], sides=16, part="thimble", angle=60.0)
    ys0, ys1 = y_spool
    C.lathe_part(prefix + "spool", [(5.6, ys0), (spool_r, ys0 + 1.0), (spool_r + 0.4, (ys0 + ys1) / 2), (spool_r, ys1 - 1.0), (5.6, ys1)], M["thread"], sides=16, part="spool", angle=60.0)
    for k, y in enumerate((ys0, ys1)):
        C.lathe_part(prefix + "flange%d" % k, [(5.0, y - 1.6), (flange_r - 0.8, y - 1.3), (flange_r, y - 0.6), (flange_r, y + 0.6), (flange_r - 0.8, y + 1.3), (5.0, y + 1.6)], M["flange"], sides=18, part="flange", angle=55.0)
    C.lathe_part(prefix + "cap", [(4.0, ys1 + 1.0), (5.6, ys1 + 2.0), (4.4, ys1 + 5.0), (1.6, ys1 + 6.5)], M["flange"], sides=14, part="cap")


def shears(prefix, pivot, ang, L, blade_mat, handle_mat, *, z=0.0, spread=7.0, ring_r=3.4, curve=0.0, wide=1.7, part="shears"):
    """A pair of scissors lying in the xy plane: two blades crossing at the pivot, two ring handles; ang = direction of the blades (deg, 0 = +x)."""
    a0 = math.radians(ang)
    for sgn in (1, -1):
        a = a0 + math.radians(sgn * spread * 0.5)
        d = V((math.cos(a), math.sin(a), 0.0))
        side = V((-d.y, d.x, 0.0))
        p0 = V((pivot[0], pivot[1], z))
        mid = p0 + d * L * 0.5 + side * (curve * sgn)
        tip = p0 + d * L + side * (curve * 0.4 * sgn)
        K.tapered(prefix + "_blade%d" % sgn, [p0, mid, tip], wide, 0.12, blade_mat, sides=4, sub=2, part=part)
        ah = a0 + math.pi - math.radians(sgn * spread * 1.4)
        dh = V((math.cos(ah), math.sin(ah), 0.0))
        e = p0 + dh * (L * 0.34)
        K.tapered(prefix + "_arm%d" % sgn, [p0, p0 + dh * L * 0.18, e], 0.8, 0.8, handle_mat, sides=6, sub=1, part=part)
        C.ring_torus(prefix + "_ring%d" % sgn, e + dh * ring_r * 0.9, ring_r, 0.55, handle_mat, normal=(0, 0, 1), sides=5, segs=14, part=part)
    K.blob(prefix + "_pivot", (pivot[0], pivot[1], z + 0.7), (1.1, 1.1, 0.6), handle_mat, seed=1, lump=0.03, rings=4, segs=7, part=part)


def helix_thread(name, y0, y1, r, mat, turns, *, wob=0.5, seed=0, tip=(0.0, 0.0), part="thread", sides=4, cord=0.34):
    """A thread spiralling up a needle (real geometry), radius wobbling, ending free."""
    rr = K.rng(seed)
    pts = []
    n = int(turns * 7)
    for i in range(n + 1):
        t = i / n
        a = TAU * turns * t + 0.3
        rad = r * (1.0 + 0.35 * t) + wob * math.sin(t * 9.0 + seed)
        pts.append(V((rad * math.cos(a) + tip[0] * t * t, y0 + (y1 - y0) * t, rad * math.sin(a) + tip[1] * t * t)))
    return K.tube(name, [tuple(p) for p in pts], cord, mat, sides=sides, sub=1, part=part, taper=(cord, cord * 0.7))


# ================================================================ 1. SILK THREAD SET
def silk_set(opts):
    C.begin(opts["samples"])
    rr = K.rng(601)
    M = {}
    M["thimble"] = C.metal("sk_thimble", rgb(214, 170, 96), 0.3, seed=61, bump="hammer", bump_scale=2.2, bump_strength=0.4)
    M["flange"] = C.metal("sk_gold", rgb(226, 180, 96), 0.28, seed=62)
    M["gold_d"] = C.metal("sk_gold_d", rgb(168, 118, 56), 0.38, seed=63)
    M["thread"] = K.surface("sk_silk_wound", rgb(244, 228, 222), 0.0, 0.34, bump="wrap", bump_scale=3.4, bump_strength=1.2, groove=rgb(196, 140, 150), groove_amount=0.8, mottle=0.8, mottle_color=rgb(236, 170, 188),
                            dirt=0.4, wear=0.3, wear_color=rgb(255, 255, 255), wear_metal=0.0, seed=64, bevel=0.0, rim=(rgb(255, 235, 245), 0.4))
    M["silk"] = C.glowmat("sk_silk", rgb(255, 196, 220), 1.1, 0.4, seed=65, mottle_color=rgb(255, 255, 255))
    M["needle"] = C.metal("sk_needle", rgb(232, 232, 238), 0.2, wear=1.0, seed=66, bump="brushed", bump_strength=0.08, scratches=0.9)
    M["wool"] = K.surface("sk_wool", rgb(190, 70, 92), 0.0, 0.8, bump="wrap", bump_scale=2.6, bump_strength=1.4, groove=rgb(80, 20, 34), groove_amount=0.9, mottle=0.9, mottle_color=rgb(236, 160, 120), fine=0.9, seed=67, bevel=0.0)
    M["ribbon"] = C.leather("sk_ribbon", rgb(220, 96, 128), seed=68, bump_strength=0.2, bump_scale=2.0)
    M["pearl"] = C.glassy("sk_pearl", rgb(250, 240, 236), emit=rgb(255, 214, 230), strength=0.35, seed=69, transmission=0.2)
    stand(M, rr)
    # pearls set round the thimble rim, a gold filigree collar on the spool
    for k in range(10):
        a = TAU * k / 10
        K.blob("pearl%d" % k, (8.1 * math.cos(a), 2.4, 8.1 * math.sin(a)), (1.25, 1.25, 1.25), M["pearl"], seed=k, lump=0.02, rings=5, segs=7, part="pearls")
    # ---- the needle: slim silver shaft rising from the cap, a LONG gold-framed eye (a slot with a scroll top), silk threaded through and a big loose bow
    ny0 = 44.0
    K.tapered("needle_shaft", [V((0, ny0, 0)), V((0, 70.0, 0)), V((0, 100.0, 0)), V((0, 111.0, 0))], 1.5, 0.05, M["needle"], sides=8, sub=2, part="needle", bulge=lambda t: 1.0 + 0.5 * math.sin(math.pi * min(1.0, t * 1.4)) ** 1.5)
    eye = [(0.0, 56.0), (3.4, 60.0), (3.8, 72.0), (2.4, 82.0), (0.0, 85.0), (-2.4, 82.0), (-3.8, 72.0), (-3.4, 60.0)]
    ep = C.smooth2(eye + [eye[0]], 3)
    K.tube("eye_frame", [tuple(p) for p in ep], 0.6, M["flange"], sides=6, sub=1, part="eye")
    K.filigree("eye_scroll_a", [(-4.0, 76.0), (-6.5, 80.0), (-8.0, 84.0), (-6.0, 88.0), (-3.5, 86.0)], M["flange"], w=0.5, h=0.45, z=0.0, side=1, sub=3, part="eye", taper=(1.0, 0.7))
    K.filigree("eye_scroll_b", [(4.0, 76.0), (6.5, 80.0), (8.0, 84.0), (6.0, 88.0), (3.5, 86.0)], M["flange"], w=0.5, h=0.45, z=0.0, side=1, sub=3, part="eye", taper=(1.0, 0.7))
    K.blob("needle_gem", (0, 92.0, 0.0), (1.6, 2.4, 1.6), M["pearl"], seed=3, lump=0.03, rings=6, segs=8, part="pearls")
    # the thread: out of the spool, up through the eye, a big floating bow and two trailing ends
    th = [V((6.5, 36.0, 0.0)), V((13.0, 46.0, 2.0)), V((9.0, 62.0, 3.0)), V((2.0, 70.0, 0.9)), V((-2.0, 70.0, -0.9)), V((-10.0, 82.0, -3.0)), V((-6.0, 98.0, -1.0)), V((4.0, 100.0, 1.0)), V((9.0, 90.0, 2.0)),
          V((2.0, 80.0, 1.2))]
    K.tube("silk_thread", [tuple(p) for p in th], 0.5, M["silk"], sides=5, sub=4, part="silk", taper=(0.55, 0.4))
    helix_thread("silk_wrap", 46.0, 58.0, 2.1, M["silk"], 3.5, seed=2, part="silk")
    K.tube("silk_tail", [(-6.5, 36.0, 0.0), (-15.0, 28.0, 3.0), (-22.0, 18.0, 1.0), (-26.0, 6.0, -2.0), (-24.0, -6.0, 0.0)], 0.5, M["silk"], sides=5, sub=4, part="silk")
    # ribbon bow tied on the spool, two long tails with swallow ends
    for sgn in (1, -1):
        loop = [V((0, 22.0, 8.0)), V((sgn * 6.0, 28.0, 10.0)), V((sgn * 12.0, 26.0, 8.0)), V((sgn * 8.0, 20.0, 8.6)), V((0, 22.0, 8.0))]
        K.ribbon("bow%d" % sgn, [V(q) for q in K.bl.smooth_path(loop, 3)], 3.2, M["ribbon"], facing=(0, 0, 1), thick=0.1, part="bow", edge_wave=0.05)
        K.ribbon("bowtail%d" % sgn, [V((sgn * 1.0 * k * 0.8, 21.0 - k * 3.6, 8.2 + 0.4 * math.sin(k * 0.8 + sgn))) for k in range(8)], lambda t: 3.0 * (1.0 - 0.4 * t) + 0.4, M["ribbon"], facing=(0, 0, 1), thick=0.08,
                 part="bow", fray=0.0, rng_=rr, notch=1.1, edge_wave=0.05)
    K.blob("bow_knot", (0, 22.0, 8.2), (2.0, 2.0, 1.4), M["ribbon"], seed=5, lump=0.1, rings=5, segs=8, part="bow")
    # ---- a ball of wool with a loose strand, a skein hank of silk hanging from the spool, and STORK shears with a gold handle
    K.blob("wool", (-17.0, 12.0, 4.0), (7.0, 7.0, 7.0), M["wool"], seed=8, lump=0.1, rings=9, segs=12, part="wool")
    K.tube("wool_strand", [(-17.0, 18.0, 9.0), (-12.0, 24.0, 7.0), (-8.0, 28.0, 3.0), (-3.0, 24.0, 2.0)], 0.5, M["wool"], sides=5, sub=4, part="wool")
    for k in range(3):
        a = 0.9 + k * 2.1
        K.tube("hank%d" % k, [(8.0 * math.cos(a), 6.0, 8.0 * math.sin(a)), (11.0 * math.cos(a), -2.0, 11.0 * math.sin(a)), (9.0 * math.cos(a + 0.2), -9.0, 9.0 * math.sin(a + 0.2)), (7.0 * math.cos(a), -4.0, 7.0 * math.sin(a))],
               0.55, M["silk"], sides=5, sub=3, part="silk")
    shears("shears", (-30.0, 2.0), 192.0, 30.0, M["needle"], M["flange"], z=-1.0, spread=9.0, ring_r=3.6, curve=0.6)
    # stork head on the shears' pivot
    K.tapered("stork_neck", [V((-30.0, 2.0, 0.4)), V((-31.0, 6.0, 1.2)), V((-35.0, 8.5, 1.4))], 1.1, 0.5, M["flange"], sides=6, sub=1, part="shears")
    K.blob("stork_eye", (-35.4, 8.8, 2.2), (0.5, 0.5, 0.5), M["pearl"], seed=2, lump=0.0, rings=4, segs=6, part="pearls")
    return K.quad_estimate()


# ================================================================ 2. SPIDER WIRE SET
def spider_set(opts):
    C.begin(opts["samples"])
    rr = K.rng(602)
    M = {}
    M["thimble"] = C.bonemat("sp_thimble", rgb(34, 30, 38), seed=71, mottle_color=rgb(70, 60, 80), bump="scales", bump_scale=1.4, bump_strength=0.5)
    M["flange"] = C.metal("sp_flange", rgb(86, 90, 104), 0.34, seed=72)
    M["thread"] = K.surface("sp_web_wound", rgb(224, 232, 242), 0.0, 0.45, bump="wrap", bump_scale=4.0, bump_strength=1.1, groove=rgb(120, 140, 170), groove_amount=0.8, mottle=0.8, mottle_color=rgb(150, 176, 210), seed=73, bevel=0.0)
    M["web"] = C.glowmat("sp_web", rgb(180, 220, 255), 1.4, 0.5, seed=74, mottle_color=rgb(255, 255, 255))
    M["chitin"] = K.surface("sp_chitin", rgb(34, 26, 30), 0.3, 0.3, bump="scales", bump_scale=1.2, bump_strength=0.4, mottle=1.0, mottle_color=rgb(150, 110, 40), wear=0.8, wear_color=rgb(210, 190, 140), wear_metal=0.2, dirt=0.6,
                            rim=(rgb(120, 140, 200), 0.5), seed=75, bevel=0.1, fine=0.6)
    M["band"] = C.bonemat("sp_band", rgb(220, 206, 170), seed=76, bump_strength=0.2)
    M["fang"] = K.surface("sp_fang", rgb(24, 20, 24), 0.5, 0.2, bump="grain", bump_scale=1.0, bump_strength=0.3, mottle=0.8, mottle_color=rgb(110, 60, 30), wear=1.0, wear_color=rgb(236, 226, 200), wear_metal=0.0, seed=77, bevel=0.1, rim=(rgb(120, 140, 200), 0.4))
    M["venom"] = C.glowmat("sp_venom", rgb(150, 255, 150), 1.5, 0.6, seed=78)
    M["eye"] = C.glowmat("sp_eye", rgb(255, 60, 60), 1.6, 0.4, seed=79)
    stand(M, rr, thimble_prof=[(4.2, Y0), (6.6, Y0 + 1.5), (8.4, -2.0), (7.6, 3.0), (6.4, 8.5)])
    # ---- a cage of four SPIDER LEGS climbing the spool: femur up, a knee, tibia hooking in over the spool, banded, with hairs
    for k in range(4):
        a = TAU * k / 4 + 0.45
        ca, sa = math.cos(a), math.sin(a)
        pts = [V((7.6 * ca, 2.0, 7.6 * sa)), V((12.0 * ca, 9.0, 12.0 * sa)), V((15.5 * ca, 22.0, 15.5 * sa)), V((14.0 * ca, 36.0, 14.0 * sa)), V((9.4 * ca, 44.0, 9.4 * sa)), V((6.0 * ca, 41.0, 6.0 * sa))]
        K.tube("leg%d" % k, [tuple(p) for p in pts], 1.0, M["chitin"], sides=7, sub=3, part="legs", taper=(1.9, 0.55), bumps=0.08, seed=80 + k)
        for j, tt in enumerate((0.3, 0.55)):
            c = C.along(K.bl.smooth_path(pts, 3), tt)
            K.blob("knee%d_%d" % (k, j), tuple(c), (1.7, 1.7, 1.7), M["band"], seed=k + j, lump=0.1, rings=5, segs=7, part="legs")
        base_pts = [V(C.along(K.bl.smooth_path(pts, 3), tt)) for tt in (0.2, 0.4, 0.6, 0.75)]
        K.spike_array("hair%d" % k, base_pts, [V((ca, 0.2, sa))] * 4, 2.6, 0.2, M["chitin"], rr, bend=0.4, sides=3, part="legs")
    # ---- the FANG needle: a curved black fang with a hollow tip dripping venom, wound with web
    K.tapered("fang", [V((0.0, 44.0, 0.0)), V((0.5, 66.0, 0.0)), V((-1.5, 90.0, 0.0)), V((-6.0, 110.0, 0.0))], 3.4, 0.1, M["fang"], sides=8, sub=3, part="needle", bulge=lambda t: 1.0 - 0.25 * t)
    K.tapered("fang_core", [V((0.0, 48.0, 0.0)), V((0.4, 66.0, 0.0)), V((-1.4, 88.0, 0.0))], 0.8, 0.3, M["web"], sides=5, sub=2, part="needle_glow")
    K.blob("venom_drop", (-6.4, 112.0, 0.0), (0.9, 1.3, 0.9), M["venom"], seed=2, lump=0.03, rings=5, segs=6, part="venom")
    helix_thread("web_wrap", 48.0, 92.0, 3.7, M["web"], 6.0, seed=3, part="web")
    # ---- the ORB WEB: radial spokes and sticky spiral hung behind the needle: a net that holds a crowd
    cx, cy, R = -12.0, 74.0, 33.0
    nsp = 11
    ang = [TAU * i / nsp + rr.uniform(-0.12, 0.12) for i in range(nsp)]
    for i, a in enumerate(ang):
        pts = [V((cx + 3.0 * math.cos(a), cy + 3.0 * math.sin(a), -2.4)), V((cx + R * 0.5 * math.cos(a), cy + R * 0.5 * math.sin(a), -2.6)), V((cx + R * math.cos(a), cy + R * math.sin(a), -2.4))]
        K.tube("spoke%d" % i, [tuple(p) for p in pts], 0.42, M["web"], sides=4, sub=1, part="web", taper=(0.55, 0.3))
    for ring_i, rad_ in enumerate((8.0, 14.5, 21.0, 27.5, 33.0)):
        for i in range(nsp):
            a0, a1 = ang[i], ang[(i + 1) % nsp] + (TAU if i == nsp - 1 else 0.0)
            sag = 0.22 * rad_ * 0.1
            pts = [V((cx + (rad_ - sag) * math.cos(a0), cy + (rad_ - sag) * math.sin(a0), -2.5)), V((cx + (rad_ * 0.93 - sag) * math.cos((a0 + a1) / 2), cy + (rad_ * 0.93 - sag) * math.sin((a0 + a1) / 2), -2.5)),
                   V((cx + (rad_ - sag) * math.cos(a1), cy + (rad_ - sag) * math.sin(a1), -2.5))]
            K.tube("strand%d_%d" % (ring_i, i), [tuple(p) for p in pts], 0.3, M["web"], sides=3, sub=1, part="web_rings", taper=(0.34, 0.3))
    for i in range(6):                                                    # dew beads on the web
        a, rd = rr.uniform(0, TAU), rr.choice((14.5, 21.0, 27.5))
        K.blob("dew%d" % i, (cx + rd * math.cos(a), cy + rd * math.sin(a), -2.5), (0.8, 0.8, 0.8), M["web"], seed=i, lump=0.02, rings=4, segs=6, part="dew")
    # ---- pincer shears: two black chelicerae with a toothed inner edge and venom glands for finger rings
    for sgn in (1, -1):
        d = V((-0.9, 0.12 * sgn, 0.0))
        p0 = V((-6.0, 6.0 + sgn * 1.0, 0.5))
        pts = [p0, p0 + V((-14.0, sgn * 4.0, 0.0)), p0 + V((-30.0, sgn * 2.0, 0.0)), p0 + V((-40.0, -sgn * 5.0, 0.0))]
        K.tube("pincer%d" % sgn, [tuple(p) for p in pts], 1.0, M["fang"], sides=6, sub=3, part="pincers", taper=(2.6, 0.2))
        K.spike_array("pincer_tooth%d" % sgn, [V(C.along(K.bl.smooth_path(pts, 3), t)) for t in (0.45, 0.6, 0.75)], [V((0.0, -sgn, 0.0))] * 3, 3.0, 0.5, M["band"], rr, bend=0.2, sides=4, part="pincers")
        K.blob("gland%d" % sgn, (-4.0, 6.0 + sgn * 5.5, 0.5), (3.0, 2.2, 2.4), M["venom"], seed=sgn + 3, lump=0.1, rings=6, segs=8, part="venom")
    K.blob("spider_eye_a", (6.0, 5.0, 8.4), (1.0, 1.0, 0.8), M["eye"], seed=1, lump=0.0, rings=4, segs=6, part="eyes")
    K.blob("spider_eye_b", (3.4, 7.2, 8.6), (0.8, 0.8, 0.7), M["eye"], seed=2, lump=0.0, rings=4, segs=6, part="eyes")
    return K.quad_estimate()


# ================================================================ 3. ARIADNE'S THREAD
def ariadne_set(opts):
    C.begin(opts["samples"])
    rr = K.rng(603)
    M = {}
    M["thimble"] = C.metal("ar_thimble", rgb(210, 160, 70), 0.3, seed=91, bump="hammer", bump_scale=2.0, bump_strength=0.35)
    M["flange"] = C.metal("ar_gold", rgb(232, 184, 80), 0.26, seed=92)
    M["gold_d"] = C.metal("ar_gold_d", rgb(150, 100, 40), 0.4, seed=93)
    M["marble"] = K.surface("ar_marble", rgb(226, 220, 208), 0.0, 0.4, bump="grain", bump_scale=1.0, bump_strength=0.15, veins=0.18, vein_color=rgb(120, 110, 120), mottle=0.8, mottle_color=rgb(170, 150, 130), wear=0.5,
                            wear_color=rgb(255, 250, 240), wear_metal=0.0, dirt=0.6, seed=94, bevel=0.1)
    M["thread"] = K.surface("ar_red_wound", rgb(170, 22, 28), 0.0, 0.4, bump="wrap", bump_scale=3.4, bump_strength=1.2, groove=rgb(70, 6, 10), groove_amount=0.8, mottle=0.8, mottle_color=rgb(230, 70, 40), seed=95, bevel=0.0,
                            rim=(rgb(255, 120, 90), 0.4))
    M["red"] = C.glowmat("ar_red", rgb(255, 70, 50), 1.4, 0.5, seed=96, mottle_color=rgb(255, 190, 90))
    M["gold_glow"] = C.glowmat("ar_goldglow", rgb(255, 214, 110), 1.5, 0.5, seed=97)
    M["needle"] = C.metal("ar_needle", rgb(240, 214, 130), 0.22, wear=1.0, seed=98, bump="brushed", bump_strength=0.08, scratches=0.9)
    M["gem"] = C.glassy("ar_gem", rgb(255, 90, 70), emit=rgb(255, 90, 60), strength=0.9, seed=99)
    M["horn"] = C.bonemat("ar_horn", rgb(210, 196, 160), seed=100, bump="grain", mottle_color=rgb(60, 40, 30))
    stand(M, rr)
    # ---- bull's horns springing from the thimble rim and curling round the spool (the Minotaur of the labyrinth): real tapered horns, banded in gold
    for sgn in (1, -1):
        pts = [V((sgn * 7.0, 6.0, 0.0)), V((sgn * 13.5, 10.0, 0.0)), V((sgn * 16.0, 22.0, 0.5)), V((sgn * 11.0, 34.0, 1.0)), V((sgn * 5.0, 38.0, 1.0))]
        K.tapered("horn%d" % sgn, pts, 2.6, 0.2, M["horn"], sides=8, sub=3, part="horns")
        for j, y in enumerate((11.0, 20.0)):
            cc = C.along(K.bl.smooth_path(pts, 3), (y - 6.0) / 32.0)
            o = K.bl.lathe("hornband%d_%d" % (sgn, j), [(2.2, -0.6), (2.9, -0.3), (2.9, 0.3), (2.2, 0.6)], M["flange"], sides=8, axis="z", caps=(False, False), part="horns")
            K.xf(o, K.frame(cc, (0, 1, 0), (0, 0, 1)))
            K._finish_obj(o, "horns", True, 60.0)
    # ---- the golden needle: a compass needle with a star at the top and a crescent eye, the red thread wound round it
    K.tapered("needle_shaft", [V((0, 44.0, 0)), V((0, 70.0, 0)), V((0, 98.0, 0)), V((0, 110.0, 0))], 1.5, 0.06, M["needle"], sides=8, sub=2, part="needle", bulge=lambda t: 1.0 + 0.4 * math.sin(math.pi * min(1.0, t * 1.3)) ** 1.5)
    star = []
    for i in range(16):
        a = TAU * i / 16 + math.pi / 2
        r = 8.0 if i % 2 == 0 else 2.6
        star.append((r * math.cos(a), 56.0 + r * math.sin(a)))
    for z0, z1 in ((0.0, 0.9), (-0.9, 0.0)):
        K.plate("star%d" % (z0 < 0), star, z0, z1, M["flange"], part="star", bevel_w=0.15)
    K.blob("star_gem", (0.0, 56.0, 1.2), (2.0, 2.0, 1.4), M["gem"], seed=3, lump=0.03, rings=6, segs=8, part="gem")
    K.tube("eye_crescent", [tuple(p) for p in C.smooth2([(-3.0, 78.0), (-3.6, 84.0), (-1.0, 88.0), (2.4, 86.0), (3.0, 80.0)], 3)], 0.55, M["flange"], sides=6, sub=1, part="eye")
    helix_thread("red_wrap", 62.0, 100.0, 2.3, M["red"], 5.5, seed=4, part="red", cord=0.4)
    # ---- the LABYRINTH: a Cretan labyrinth disc behind the needle: concentric gold walls with a path of gaps, a red thread winding in the path from the edge to the centre
    cx, cy = 0.0, 74.0
    rads = [5.0, 9.0, 13.0, 17.0, 21.0, 25.0, 29.0]
    gaps = [0.0, 2.6, 5.2, 1.4, 4.0, 0.9, 3.6]
    for i, rad_ in enumerate(rads):
        g = gaps[i]
        a0, a1 = g + 0.34, g + TAU - 0.34
        pts = [V((cx + rad_ * math.cos(a0 + (a1 - a0) * k / 16), cy + rad_ * math.sin(a0 + (a1 - a0) * k / 16), -2.4)) for k in range(17)]
        K.tube("wall%d" % i, [tuple(p) for p in pts], 0.5, M["flange"] if i % 2 == 0 else M["gold_d"], sides=5, sub=1, part="labyrinth")
        gp = [V((cx + rad_ * math.cos(a0 - 0.0), cy + rad_ * math.sin(a0), -2.4)), V((cx + rads[max(i - 1, 0)] * math.cos(a0), cy + rads[max(i - 1, 0)] * math.sin(a0), -2.4))]
    for k in range(7):                                                    # radial spokes tie the walls (the minotaur's corridors)
        a = TAU * k / 7 + 0.2
        K.tube("rad%d" % k, [(cx + 5.0 * math.cos(a), cy + 5.0 * math.sin(a), -2.4), (cx + 29.0 * math.cos(a), cy + 29.0 * math.sin(a), -2.4)], 0.32, M["gold_d"], sides=4, sub=1, part="labyrinth")
    thr = []
    for i in range(0, 130):
        t = i / 129
        a = TAU * 3.0 * t
        r = 3.0 + 27.0 * (1 - t)
        thr.append(V((cx + r * math.cos(a), cy + r * math.sin(a), -1.4 - 0.5 * math.sin(t * 20))))
    K.tube("clew", [tuple(p) for p in thr[::4]] + [tuple(thr[-1])], 0.5, M["red"], sides=4, sub=2, part="red", taper=(0.55, 0.4))
    K.tube("clew_tail", [tuple(thr[0]), (cx + 24.0, cy - 22.0, 0.0), (cx + 12.0, 38.0, 4.0), (6.5, 36.0, 0.0)], 0.5, M["red"], sides=5, sub=4, part="red")
    K.plate("hub", [(5.2 * math.cos(TAU * i / 12), 74.0 + 5.2 * math.sin(TAU * i / 12)) for i in range(12)], -2.9, -1.9, M["gold_d"], part="labyrinth", bevel_w=0.2)
    K.blob("hub_gem", (cx, cy, -2.6), (3.2, 3.2, 1.2), M["gem"], seed=2, lump=0.04, rings=5, segs=8, part="gem")
    K.shard_cloud("star_mote", (cx, cy, 0.0), (32.0, 36.0, 3.0), 9, (0.5, 0.9), M["gold_glow"], rr, avoid=lambda p: math.hypot(p.x - cx, p.y - cy) < 30.0, part="motes", stretch=1.6, sides=3, max_count=9)
    # ---- golden shears of the Fates (laurel handles), and a red ribbon bow
    shears("shears", (-32.0, 4.0), 190.0, 32.0, M["needle"], M["flange"], z=-1.0, spread=8.0, ring_r=3.8, curve=0.5)
    for sgn in (1, -1):
        K.ribbon("tie%d" % sgn, [V((sgn * 0.8 * k, 24.0 - k * 3.2, 8.4 + 0.5 * math.sin(k + sgn))) for k in range(8)], lambda t: 2.8 * (1.0 - 0.4 * t) + 0.4, M["thread"], facing=(0, 0, 1), thick=0.08, part="bow", rng_=rr, notch=1.0, edge_wave=0.05)
    K.blob("tie_knot", (0, 25.0, 8.4), (2.0, 2.0, 1.4), M["thread"], seed=5, lump=0.1, rings=5, segs=8, part="bow")
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["silk"] = (silk_set, "vassal_sewing_set__silk_thread_set", BASE, "silk_thread_set", "rapier")
BUILDERS["spider"] = (spider_set, "vassal_sewing_set__spider_wire_set", BASE, "spider_wire_set", "rapier")
BUILDERS["ariadne"] = (ariadne_set, "vassal_sewing_set__ariadnes_thread", BASE, "ariadnes_thread", "rapier")

if __name__ == "__main__":
    C.run_family(sys.argv[1:], BUILDERS, "vassal_sewing_set")

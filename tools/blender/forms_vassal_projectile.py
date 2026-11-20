"""Weapon-form models of the VASSAL PROJECTILE (base `vassal_projectile`, a crystal throwing blade, size class dagger), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_projectile.py --only kunai [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids (docs/heroweapon/form_models.md): vassal_projectile__<form>:
  kunai               a dark iron leaf blade with a bright edge, a wrapped grip, a ring pommel with paper tags, a smaller twin kunai tied to the ring, a long cord tail
  chakram             a big golden war ring (a sharp steel rim, a gold band, spoked copper inner ring, a magisteel hub) with crescent blades round it, on a short handle
  hatchet             a wooden haft lashed with rawhide to an iron head with a knapped flint edge and a flint-spiked poll, a thong with beads and feathers
  over_custom_arsenal an emerald blade in a gold guard, a golden chakram halo behind it, a fan of four kunai, a small hatchet charm on a chain, a gold ring pommel
Every form keeps the base's hold (grip y 15..41, a ring or cap pommel at y ~6, a cord to y ~ -24.6) and place line; blade y from ~44. Seeded: the same seed, the same weapon.
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

MANIFEST = os.path.join(os.path.dirname(os.path.abspath(__file__)), "forms_manifest_vassal_projectile.json")
BASE = "vassal_projectile"
SEC_DIAMOND = ((-1.0, -0.5, 0.0, 0.5, 1.0), (0.0, 0.72, 1.0, 0.72, 0.0))         # a ridged diamond section (a kunai leaf)
SEC_FULLER = ((-1.0, -0.86, -0.34, -0.2, 0.2, 0.34, 0.86, 1.0), (0.0, 0.8, 1.0, 0.5, 0.5, 1.0, 0.8, 0.0))


def leaf_blade(name, mat, stations, y1, off, tilt=0.0, s=1.0, thick=1.8, sec=SEC_DIAMOND, seed=0, part="blade", jit=0.03, res=4.0, wob=0.03, tipL=None, tipR=None):
    """A leaf / dagger blade grown from `off` along +y (tilted `tilt` degrees toward +x), scaled by s. stations [(local y, half width)]. Returns (T, L, R) with T mapping local -> world."""
    a = math.radians(-tilt)
    ca, sa = math.cos(a), math.sin(a)
    off = V(off)

    def T(p):
        return V((off.x + s * (p.x * ca - p.y * sa), off.y + s * (p.x * sa + p.y * ca), off.z + s * p.z))
    Lp = K.blade_edge(-1, stations, 0.0, y1, jitter=jit, res=res, seed=seed + 1, tip=tipL)
    Rp = K.blade_edge(1, stations, 0.0, y1, jitter=jit, res=res, seed=seed + 2, tip=tipR)
    K.blade(name, mat, [T(p) for p in Lp], [T(p) for p in Rp], thick=(lambda t: thick * s * (1.0 - 0.35 * t * t)), section=sec, part=part, seed=seed + 3, wob=wob)
    return T, Lp, Rp


def edge_glow(name, mat, Lp, Rp, T, s, thick, sec, f=(0.64, 0.9), part="edge", real=False):
    """Thin glowing lines hugging both edge bevels of a leaf blade, on both faces (bake-only: the relief and glow go into the skin, no quads)."""
    rows = K.merge_rows(Lp, Rp)
    if real:
        rows = rows[::2] + ([rows[-1]] if len(rows) % 2 == 0 else [])
    n = len(rows)
    for sd in (1, -1):
        for side in (-1, 1):
            rr_ = []
            for i, (l, r) in enumerate(rows):
                t = i / max(n - 1, 1)
                c = (l + r) * 0.5
                e = l if side < 0 else r
                p0, p1 = c + (e - c) * f[0], c + (e - c) * f[1]
                th = thick * (1.0 - 0.35 * t * t)
                h = K.interp(list(zip(sec[0], sec[1])), side * 0.5 * (f[0] + f[1]))
                zz = sd * (th * h * 0.9 + (0.12 * s if real else 0.0))
                rr_.append((T(V((p0.x, p0.y, zz))), T(V((p1.x, p1.y, zz)))))
            K.strip("%s%d%d" % (name, sd, side), rr_, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), (0.2 * s if real else 0.16 * s), part=part, up=(0, 0, sd), detail=not real)


def fin(name, c, d, length, width, mat, thick=0.9, part="spikes", curve=0.0, n=7, hook=0.0):
    """A crescent fin / sawtooth blade grown from a rim: widest AT the base (fused to the ring), swept sideways by `curve`, ending in a hooked point (a returning-edge tooth)."""
    d = V(d).normalized()
    sv = V((-d.y, d.x, 0))
    rows = []
    for i in range(n + 1):
        t = i / n
        w = width * 0.5 * (1.0 - t ** 1.25) if i < n else 0.0
        p = V(c) + d * (length * t) + sv * (curve * t * t) + sv * (hook * t ** 4)
        rows.append((p - sv * w * 0.35, p + sv * w * 1.65))
    return K.strip(name, rows, mat, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=(0, 0, 1), angle=50)


def cord_tail(name, mat, pts, r0, r1, part="pommel_cord", sub=3):
    return K.tapered(name, [V(p) for p in pts], r0, r1, mat, sides=5, sub=sub, part=part)


# ================================================================ 1. KUNAI
def kunai_pal():
    P = {}
    P["iron"] = Lb.metal("kn_iron", rgb(54, 62, 74), rgb(190, 205, 222), 601, rough=0.3, mottle=rgb(36, 46, 60), dirt=0.55, dirt_col=(0.0, 0.0, 0.01, 1), bump="hammer", bs=0.08)
    P["edge"] = Lb.metal("kn_edge", rgb(196, 210, 224), rgb(255, 255, 255), 602, rough=0.16, mottle=rgb(120, 150, 180), dirt=0.3)
    P["steel"] = Lb.metal("kn_steel", rgb(120, 134, 150), rgb(235, 240, 248), 603, rough=0.26, dirt=0.5)
    P["cord"] = Lb.cloth("kn_cord", rgb(168, 50, 38), rgb(44, 10, 6), 604, mottle=rgb(205, 100, 66), bump="wrap", scale=1.0)
    P["core"] = Lb.paint("kn_core", rgb(34, 28, 28), rgb(110, 100, 92), 605, bump="leather")
    P["glow"] = Lb.glow("kn_glow", rgb(70, 236, 226), 0.9, 606, flicker=0.55)
    P["gem"] = Lb.gem("kn_gem", rgb(40, 214, 230), 1.0, 607)
    P["paper"] = Lb.paint("kn_paper", rgb(238, 228, 200), rgb(255, 250, 235), 608, rough=0.85, mottle=rgb(205, 185, 150), dirt_col=(0.2, 0.12, 0.06, 1), bump="cross")
    P["seal"] = Lb.paint("kn_seal", rgb(176, 26, 22), rgb(240, 120, 90), 609, rough=0.6)
    return P


KU_ST = [(0, 4.0), (5, 7.4), (16, 9.4), (28, 8.6), (42, 6.4), (56, 3.5), (64, 1.5), (69, 0.1)]


def kunai(opts):
    C.begin(opts)
    C.weights(blade=1.4, edge=1.0, grip=0.9, pommel=1.0, twin=0.85, tags=0.7)
    P = kunai_pal()
    rr = K.rng(7801)
    # ---- the main leaf blade (y 44 .. 113) with a bright edge line and engraved blood-groove lines baked in
    T, Lp, Rp = leaf_blade("blade", P["iron"], KU_ST, 69.0, (0, 44.0, 0), 0.0, 1.0, thick=1.9, seed=11, res=3.0, tipL=(-0.1, 69.0), tipR=(0.2, 68.6))
    edge_glow("edge", P["glow"], Lp, Rp, T, 1.0, 1.9, SEC_DIAMOND)
    for sd in (1, -1):
        K.seam("ridge%d" % sd, [(0.0, 49.0), (0.2, 70.0), (0.0, 96.0), (0.1, 108.0)], P["edge"], w=0.3, depth=0.12, z=1.85 * sd * 0.98, side=sd, part="engrave", detail=True)
    for sd in (1, -1):                                                                          # a small kanji-like punch row
        for k in range(5):
            y = 55.0 + k * 4.2
            K.seam("punch%d_%d" % (sd, k), [(-1.6, y), (1.6, y + 0.4)], P["edge"], w=0.22, depth=0.1, z=1.8 * sd, side=sd, part="engrave", detail=True)
    # collar between the grip and the blade, with the tang
    Lb.lathe("collar", [(1.7, 39.8), (3.2, 40.4), (4.4, 42.0), (3.6, 43.4), (2.0, 44.4), (0.01, 44.6)], P["steel"], "collar", sides=14, squash=0.8)
    K.stepped_plate("tang_guard", [(-5.4, 41.5), (5.4, 41.5), (4.0, 45.4), (-4.0, 45.4)], 0.9, P["steel"], steps=2, step_h=0.5, inset=0.6, part="collar", bevel_w=0.0, up=1)
    K.stepped_plate("tang_guard2", [(-5.4, 41.5), (5.4, 41.5), (4.0, 45.4), (-4.0, 45.4)], -0.9, P["steel"], steps=2, step_h=0.5, inset=0.6, part="collar", bevel_w=0.0, up=-1)
    for sx in (-1, 1):                                                                         # two small guard fins swept toward the blade
        Lb.lancet("fin%d" % sx, V((sx * 4.4, 42.6, 0.0)), V((sx * 0.5, 1.0, 0.0)), 7.5, 2.6, P["iron"], thick=0.7, part="collar", curve=sx * -1.6, z=0.0, n=6, belly=0.35)
    # ---- the grip (y 15..41): leather core, a wound cord, bands
    K.grip_wrapped("grip", 15.0, 40.0, 3.0, P["core"], P["cord"], turns=11.0, cord=0.42, bands=(15.6, 27.6, 39.4), band_mat=P["steel"], swell=0.1, part="grip", seed=3, sides=10)
    # ---- the ring pommel and the cord tail (to y -24.6)
    Lb.lathe("pommel_neck", [(0.01, 11.0), (2.3, 11.6), (3.3, 13.4), (3.6, 15.0)], P["steel"], "pommel", sides=12)
    Lb.ring_torus("pommel_ring", (0, 5.6, 0), 7.4, 1.8, P["iron"], "pommel", sides=8, segs=30)
    Lb.ring_torus("pommel_ring_in", (0, 5.6, 0), 5.4, 0.42, P["edge"], "pommel", sides=5, segs=24)
    K.gem_setting("pommel_gem", V((0, 13.3, 2.0)), 1.5, P["gem"], P["steel"], normal=(0, 0, 1), claws=4, sides=6, part="pommel_gem", rng_=rr, claw_r=0.4)
    cp = [(0.0, -1.6, 0.0), (-1.0, -7.0, 1.0), (1.6, -13.0, -0.6), (-0.8, -19.0, 0.6), (0.2, -23.2, 0.0)]
    cord_tail("tail", P["cord"], cp, 0.55, 0.36)
    Lb.lathe("tail_bead", [(0.01, -22.6), (1.2, -22.9), (1.7, -23.8), (1.3, -24.5), (0.01, -24.633)], P["gem"], "pommel_cord", sides=9)
    Lb.fur("tassel", [V((0.2 * math.cos(a), -23.0, 0.2 * math.sin(a))) for a in (TAU * k / 9 for k in range(9))],
           [V((0.35 * math.cos(a), -1.0, 0.35 * math.sin(a))) for a in (TAU * k / 9 for k in range(9))], 4.0, 0.34, P["cord"], rr, part="pommel_cord", jit=0.2)
    for k, y in enumerate((-9.0, -15.0)):                                                        # a few cord knots on the tail
        Lb.lathe("knot%d" % k, [(0.01, y - 0.7), (0.9, y - 0.4), (1.0, y + 0.1), (0.9, y + 0.6), (0.01, y + 0.8)], P["cord"], "pommel_cord", sides=7)
    # ---- the twin kunai (the "second throw"): a smaller leaf blade beside the main one, behind it, with its own wrapped handle and a ring tied to the main ring by a short chain
    TX, TZ = 17.4, -4.4
    T2, Lp2, Rp2 = leaf_blade("twin_blade", P["iron"], KU_ST, 69.0, (TX, 52.0, TZ), 5.0, 0.64, thick=1.9, seed=31, part="twin", tipL=(-0.1, 69.0), tipR=(0.2, 68.6))
    edge_glow("twin_edge", P["glow"], Lp2, Rp2, T2, 0.64, 1.9, SEC_DIAMOND, part="twin")
    Lb.lathe("twin_collar", [(1.0, 49.4), (2.2, 50.4), (2.6, 51.8), (1.6, 52.6), (0.01, 52.8)], P["steel"], "twin", sides=10, origin=(TX, 0, TZ))
    K.helix_wrap("twin_wrap", 35.0, 49.4, 1.65, P["cord"], turns=7.0, cord=0.3, part="twin", seed=17, axis_off=(TX, TZ))
    Lb.lathe("twin_core", [(0.01, 33.8), (1.5, 34.2), (1.7, 36.0), (1.55, 49.6), (1.7, 49.8), (0.01, 50.0)], P["core"], "twin", sides=9, origin=(TX, 0, TZ))
    Lb.ring_torus("twin_ring", (TX, 28.6, TZ), 4.8, 1.15, P["iron"], "twin", sides=6, segs=22)
    K.chain("twin_chain", [V((7.6, 8.4, -0.5)), V((12.0, 14.0, -2.6)), V((TX - 2.6, 25.0, TZ + 0.4))], 2.2, 1.5, 0.28, P["steel"], max_links=6, part="twin", sides=4, segs=6)
    cord_tail("twin_tail", P["cord"], [(TX, 23.8, TZ), (TX + 1.0, 17.0, TZ + 0.6), (TX - 0.8, 10.0, TZ), (TX + 0.4, 5.0, TZ + 0.3)], 0.4, 0.26, part="twin")
    Lb.fur("twin_tassel", [V((TX + 0.4 + 0.2 * math.cos(a), 5.0, TZ + 0.3 + 0.2 * math.sin(a))) for a in (TAU * k / 6 for k in range(6))],
           [V((0.3 * math.cos(a), -1.0, 0.3 * math.sin(a))) for a in (TAU * k / 6 for k in range(6))], 3.2, 0.28, P["cord"], rr, part="twin", jit=0.2)
    # ---- paper tags (the sealed tags of a kunai) tied to the ring, fluttering
    for k, (px, side) in enumerate(((-7.2, -1), (6.4, 1))):
        path = [V((px, 7.0 - 1.0 * k, 0.4)), V((px + side * 1.0, 2.0, 2.8)), V((px + side * 1.8, -4.0 - k, 1.0)), V((px + side * 1.0, -10.0 - 2 * k, 3.2)), V((px + side * 2.2, -15.0 - 3 * k, 1.6))]
        K.ribbon("tag%d" % k, path, 2.6, P["paper"], facing=(0, 0, 1), thick=0.07, part="tags", fray=0.5, rng_=rr, edge_wave=0.25)
        K.ribbon("seal%d" % k, [V((p.x, p.y, p.z + 0.12)) for p in path[1:3]], 1.2, P["seal"], facing=(0, 0, 1), thick=0.05, part="tags", rng_=rr)
    return K.quad_estimate()


# ================================================================ 2. CHAKRAM
def chakram_pal():
    P = {}
    P["gold"] = Lb.hammered("ck_gold", rgb(228, 176, 58), rgb(255, 242, 196), 611, mottle=rgb(168, 104, 28))
    P["copper"] = Lb.metal("ck_copper", rgb(196, 98, 58), rgb(250, 190, 140), 612, rough=0.3, mottle=rgb(120, 52, 30), dirt=0.55, dirt_col=(0.03, 0.01, 0.0, 1), bump="hammer", bs=0.1)
    P["steel"] = Lb.metal("ck_steel", rgb(210, 220, 232), rgb(255, 255, 255), 613, rough=0.14, mottle=rgb(130, 160, 190), dirt=0.3)
    P["magi"] = Lb.metal("ck_magi", rgb(116, 188, 200), rgb(235, 255, 255), 614, rough=0.2, mottle=rgb(60, 130, 150), dirt=0.4)
    P["wrap"] = Lb.cloth("ck_wrap", rgb(118, 38, 28), rgb(30, 8, 4), 615, mottle=rgb(170, 80, 50), bump="wrap", scale=1.0)
    P["core"] = Lb.paint("ck_core", rgb(46, 30, 24), rgb(130, 100, 80), 616, bump="leather")
    P["glow"] = Lb.glow("ck_glow", rgb(255, 170, 54), 0.95, 617, flicker=0.6)
    P["gem"] = Lb.gem("ck_gem", rgb(255, 176, 56), 1.05, 618)
    P["ruby"] = Lb.gem("ck_ruby", rgb(236, 56, 44), 0.95, 619)
    return P


CK_Y, CK_R = 72.0, 27.0


def chakram(opts):
    C.begin(opts)
    C.weights(rim=1.35, band=1.2, inner=1.0, spikes=1.0, grip=0.85, hub=1.0)
    P = chakram_pal()
    rr = K.rng(7901)
    cen = (0.0, CK_Y, 0.0)
    # ---- the war ring: a sharp steel rim, a gold band inside it, a copper inner ring on spokes, a magisteel hub
    Lb.flat_ring("rim", cen, CK_R, [(-2.2, -1.1), (-2.2, 1.1), (0.8, 1.3), (2.9, 0.0), (0.8, -1.3)], P["steel"], "rim", segs=44)
    Lb.flat_ring("band", cen, CK_R - 4.6, [(-2.3, -1.7), (-2.3, 1.7), (2.3, 1.7), (2.3, -1.7)], P["gold"], "band", segs=48)
    Lb.flat_ring("band_in", cen, CK_R - 7.4, [(-0.55, -1.2), (-0.55, 1.2), (0.55, 1.2), (0.55, -1.2)], P["copper"], "band", segs=44)
    Lb.flat_ring("inner", cen, 13.0, [(-1.6, -1.3), (-1.6, 1.3), (1.6, 1.3), (1.6, -1.3)], P["copper"], "inner", segs=28)
    Lb.flat_ring("inner_rim", cen, 13.0, [(-0.45, -1.7), (-0.45, 1.7), (0.45, 1.7), (0.45, -1.7)], P["gold"], "inner", segs=28)
    # engraved wave and rivet rows on the band (baked relief / real domes)
    for sd in (1, -1):
        wp = [(-(CK_R - 4.6 + 0.9 * math.sin(a * 12)) * math.sin(a), CK_Y + (CK_R - 4.6 + 0.9 * math.sin(a * 12)) * math.cos(a)) for a in (TAU * i / 96 for i in range(97))]
        K.filigree("wave%d" % sd, [(p[0], p[1]) for p in wp], P["copper"], w=0.34, h=0.18, z=1.68 * sd, side=sd, sub=1, part="band", detail=True, taper=(1.0, 1.0))
    pts = []
    for i in range(8):
        a = TAU * (i + 0.5) / 8
        for sd in (1, -1):
            pts.append((V((((CK_R - 4.6) * math.cos(a)), CK_Y + (CK_R - 4.6) * math.sin(a), 1.7 * sd)), (0, 0, sd)))
    K.rivets("band_rv", pts, P["steel"], r=0.55, part="band_rivets", detail=False, sides=5, height=0.5, rng_=rr, jitter=0.15)
    # four jewels on the band
    for k in range(2):
        a = TAU * k / 2 + math.pi / 2
        for sd in (1, -1):
            K.gem_setting("bg%d_%d" % (k, sd), V(((CK_R - 4.6) * math.cos(a), CK_Y + (CK_R - 4.6) * math.sin(a), 1.65 * sd)), 1.7, P["ruby"] if k % 2 else P["gem"], P["gold"], normal=(0, 0, sd),
                          claws=3, sides=6, part="gem", rng_=rr, claw_r=0.4, up=(math.cos(a), math.sin(a), 0))
    # ---- crescent blades round the outside: eight long swept ones and eight short ones between (every one swings the same way, a returning edge)
    for k in range(8):
        a = TAU * k / 8 + 0.1
        d = V((math.cos(a), math.sin(a), 0))
        base = V(cen) + d * (CK_R + 0.6)
        fin("crescent%d" % k, base, d, 15.0, 7.0, P["steel"], thick=1.1, part="spikes", curve=7.0, n=7, hook=4.0)
    for k in range(8):
        a = TAU * (k + 0.5) / 8 + 0.1
        d = V((math.cos(a), math.sin(a), 0))
        fin("short%d" % k, V(cen) + d * (CK_R + 0.6), d, 8.0, 4.4, P["magi"], thick=0.9, part="spikes", curve=3.6, n=5, hook=2.0)
    # ---- the spokes: six copper blades curving from the hub to the inner ring, a magisteel hub with a gem
    for k in range(6):
        a = TAU * k / 6
        d = V((math.cos(a), math.sin(a), 0))
        s_ = V((-d.y, d.x, 0))
        p = [V(cen) + d * (4.8 + 8.6 * t) + s_ * (3.0 * math.sin(t * math.pi)) for t in (0.0, 0.34, 0.67, 1.0)]
        K.tapered("spoke%d" % k, p, 1.1, 0.8, P["copper"], sides=5, sub=2, part="inner")
    Lb.lathe("hub_disc", [(0.01, -1.6), (5.6, -1.6), (6.0, 0.0), (5.2, 1.8), (2.6, 2.4), (0.01, 2.6)], P["magi"], "hub", sides=18, axis="z", origin=cen)
    for sd in (1, -1):
        K.gem_setting("hub_gem%d" % sd, V((0, CK_Y, 2.2 * sd)), 2.7, P["gem"], P["gold"], normal=(0, 0, sd), claws=6, sides=8, part="hub_gem", rng_=rr, claw_r=0.5)
        for k in range(4):
            a = TAU * k / 4 + math.pi / 4
            K.blob("hub_dot%d_%d" % (sd, k), (4.8 * math.cos(a), CK_Y + 4.8 * math.sin(a), 1.9 * sd), (0.55, 0.55, 0.45), P["gold"], seed=k, lump=0.02, rings=3, segs=5, part="hub")
    # ---- the handle: a collar joining the ring's foot, a leather and cord grip (y 15..41), gold bands, a cap, a long tasselled tail
    Lb.lathe("foot_collar", [(1.8, 38.4), (3.4, 39.2), (4.2, 42.2), (3.2, 44.2), (1.7, 45.6)], P["gold"], "collar", sides=14, squash=0.8)
    for sd in (1, -1):
        K.stepped_plate("foot_plate%d" % sd, [(-4.2, 40.0), (4.2, 40.0), (3.2, 47.0), (-3.2, 47.0)], 1.3 * sd, P["gold"], steps=2, step_h=0.5, inset=0.55, part="collar", bevel_w=0.0, up=sd)
    K.grip_wrapped("grip", 15.0, 39.2, 3.1, P["core"], P["wrap"], turns=10.0, cord=0.42, bands=(15.6, 27.0, 38.6), band_mat=P["gold"], swell=0.1, part="grip", seed=5, sides=10)
    Lb.lathe("pommel_cap", [(0.01, 5.0), (1.8, 5.4), (3.2, 7.4), (3.7, 10.0), (3.5, 13.0), (3.2, 15.0)], P["gold"], "pommel", sides=14)
    Lb.lathe("pommel_drop", [(0.01, 1.0), (1.6, 1.8), (2.4, 3.2), (2.0, 5.0), (0.01, 5.2)], P["copper"], "pommel", sides=12)
    K.gem_setting("pommel_gem", V((0, 6.6, 3.0)), 1.5, P["ruby"], P["gold"], normal=(0, 0, 1), claws=4, sides=6, part="pommel_gem", rng_=rr, claw_r=0.4)
    cord_tail("tail", P["wrap"], [(0.0, 1.2, 0.0), (0.8, -5.0, 0.8), (-1.2, -11.0, -0.4), (1.0, -17.0, 0.6), (0.0, -22.2, 0.0)], 0.6, 0.38)
    Lb.lathe("tail_bead", [(0.01, -22.6), (1.3, -22.9), (1.9, -23.8), (1.4, -24.5), (0.01, -24.633)], P["ruby"], "pommel_cord", sides=9)
    Lb.fur("tassel", [V((0.25 * math.cos(a), -22.4, 0.25 * math.sin(a))) for a in (TAU * k / 10 for k in range(10))],
           [V((0.4 * math.cos(a), -1.0, 0.4 * math.sin(a))) for a in (TAU * k / 10 for k in range(10))], 4.4, 0.36, P["gold"], rr, part="pommel_cord", jit=0.2)
    for k, y in enumerate((-6.0, -12.5, -18.0)):
        Lb.lathe("bead%d" % k, [(0.01, y - 0.9), (1.2, y - 0.5), (1.45, y), (1.2, y + 0.5), (0.01, y + 0.9)], P["gold"], "pommel_cord", sides=8)
    # ---- small chain-and-bell charms hung from the ring's lower rim (they swing)
    for sx in (-1, 1):
        K.tapered("charm%d" % sx, [V((sx * 9.0, CK_Y - CK_R + 3.2, 0.0)), V((sx * 11.4, CK_Y - CK_R - 1.5, 0.4)), V((sx * 13.2, CK_Y - CK_R - 6.0, 0.0))], 0.3, 0.26, P["gold"], sides=4, sub=1, part="charm")
        K.blob("bell%d" % sx, (sx * 13.4, CK_Y - CK_R - 7.4, 0.0), (1.1, 1.3, 1.1), P["gold"], seed=sx + 3, lump=0.02, rings=5, segs=8, part="charm")
    return K.quad_estimate()


# ================================================================ 3. HATCHET
def hatchet_pal():
    P = {}
    P["wood"] = K.surface("ht_wood", rgb(124, 78, 44), 0.0, 0.75, bump="grain", bump_scale=1.5, bump_strength=0.85, groove=rgb(34, 18, 8), groove_amount=0.9, dirt=0.65, mottle=0.8, mottle_color=rgb(176, 120, 72),
                          fine=0.5, seed=621, bevel=0.0)
    P["iron"] = Lb.metal("ht_iron", rgb(66, 70, 78), rgb(200, 206, 214), 622, rough=0.36, mottle=rgb(44, 48, 56), dirt=0.7, dirt_col=(0.01, 0.01, 0.01, 1), bump="hammer", bs=0.14)
    P["steel"] = Lb.metal("ht_steel", rgb(168, 176, 188), rgb(250, 252, 255), 623, rough=0.22, dirt=0.5)
    P["flint"] = K.surface("ht_flint", rgb(54, 60, 66), 0.2, 0.18, bump="hammer", bump_scale=0.6, bump_strength=0.5, mottle=0.9, mottle_color=rgb(130, 140, 148), dirt=0.3, seed=624, bevel=0.1,
                           rim=(rgb(170, 190, 205), 0.6))
    P["hide"] = Lb.cloth("ht_hide", rgb(214, 186, 140), rgb(70, 46, 24), 625, mottle=rgb(150, 110, 70), bump="wrap", scale=1.0)
    P["leather"] = Lb.paint("ht_leather", rgb(78, 46, 28), rgb(180, 130, 90), 626, bump="leather")
    P["bone"] = Lb.paint("ht_bone", rgb(226, 214, 184), rgb(255, 250, 230), 627, rough=0.7, mottle=rgb(170, 150, 110), dirt_col=(0.1, 0.07, 0.03, 1))
    P["feather"] = Lb.paint("ht_feather", rgb(176, 64, 40), rgb(250, 190, 120), 628, rough=0.8, mottle=rgb(230, 150, 80))
    P["feather2"] = Lb.paint("ht_feather2", rgb(236, 226, 206), rgb(255, 255, 250), 629, rough=0.8, mottle=rgb(170, 160, 140))
    P["ember"] = Lb.glow("ht_ember", rgb(255, 128, 36), 1.0, 630, flicker=0.8)
    P["gem"] = Lb.gem("ht_gem", rgb(255, 120, 40), 0.95, 631)
    return P


HT_BY = 78.0                                                                                  # the head's lower edge


def hatchet(opts):
    C.begin(opts)
    C.weights(head=1.3, flint=1.0, shaft=0.9, grip=0.9, lashing=0.8, feathers=0.7, emberline=0.6)
    P = hatchet_pal()
    rr = K.rng(8001)
    # ---- the haft: a wooden shaft from the knob (y 9) to above the head (y 119); the grip wrap (y 15..41) sits on it
    hp = [V(p) for p in ((0.0, 9.0, 0.0), (0.3, 30.0, 0.3), (-0.2, 55.0, 0.0), (0.2, 80.0, -0.2), (0.0, 108.0, 0.0), (0.0, 119.0, 0.0))]
    K.wood_branch("shaft", hp, 2.25, 1.7, P["wood"], seed=5, sides=10, knots=3, part="shaft", sub=5)
    K.grip_wrapped("grip", 15.0, 41.0, 2.95, P["leather"], P["hide"], turns=12.0, cord=0.46, bands=(15.4, 28.0, 40.6), band_mat=P["iron"], swell=0.08, part="grip", seed=7, sides=10)
    Lb.lathe("knob", [(0.01, 4.4), (1.8, 4.8), (3.6, 6.6), (4.0, 9.4), (3.2, 12.4), (2.6, 14.4)], P["wood"], "pommel", sides=12)
    Lb.lathe("knob_cap", [(0.01, 4.0), (2.0, 4.2), (2.6, 5.4), (1.8, 6.4), (0.01, 6.6)], P["iron"], "pommel", sides=10)
    # ---- the head: an iron bearded axe head on the haft, edge toward +x, a hammer-poll toward -x; stepped plates both faces
    head = [(-3.4, HT_BY + 4.0), (-3.4, 106.0), (0.0, 108.4), (6.0, 111.6), (12.6, 114.6), (17.4, 109.6), (19.8, 99.0), (19.0, 88.0), (15.4, 79.0), (9.0, HT_BY - 1.4), (3.4, HT_BY + 1.0)]
    for sd in (1, -1):
        K.stepped_plate("head%d" % sd, head, 1.1 * sd, P["iron"], steps=3, step_h=0.5, inset=0.75, part="head", bevel_w=0.0, up=sd)
    poll = [(-3.6, 87.0), (-9.4, 88.6), (-10.2, 99.0), (-9.0, 101.4), (-3.6, 102.0)]
    for sd in (1, -1):
        K.stepped_plate("poll%d" % sd, poll, 1.0 * sd, P["iron"], steps=2, step_h=0.5, inset=0.6, part="head", bevel_w=0.0, up=sd)
    for sd in (1, -1):
        for k, (x, y) in enumerate(((-6.4, 90.6), (-6.4, 98.6))):
            K.rivets("poll_rv%d_%d" % (sd, k), [(V((x, y, 1.0 * sd + 0.4 * sd)), (0, 0, sd))], P["steel"], r=0.7, part="head", detail=False, sides=6, height=0.6, rng_=rr)
        K.rivets("head_rv%d" % sd, [(V((x, y, 1.6 * sd)), (0, 0, sd)) for x, y in ((0.0, 83.0), (0.0, 104.0), (8.0, 100.0), (8.0, 86.0))], P["steel"], r=0.6, part="head", detail=False, sides=6, height=0.6, rng_=rr)
    # the knapped flint edge: a row of chipped shards set along the curved cutting edge
    edge = [(16.4, 111.0), (19.2, 105.0), (21.0, 99.0), (21.2, 93.0), (20.4, 87.4), (18.6, 82.4), (15.6, 78.4), (11.4, 76.2)]
    ep = []
    for i in range(len(edge) - 1):
        for j in range(3):
            t = j / 3.0
            ep.append((edge[i][0] + (edge[i + 1][0] - edge[i][0]) * t, edge[i][1] + (edge[i + 1][1] - edge[i][1]) * t))
    ep.append(edge[-1])
    for i, (x, y) in enumerate(ep):
        K.shard("flint%d" % i, V((x, y, rr.uniform(-0.3, 0.3))), 1.9 * rr.uniform(0.8, 1.2), P["flint"], rr, part="flint", sides=5, stretch=1.7, dir_=(0.8, 0.0, rr.uniform(-0.4, 0.4)))
    # a flint spike at the poll
    K.shard("poll_spike", V((-12.6, 94.0, 0.0)), 5.0, P["flint"], rr, part="flint", sides=6, stretch=2.2, dir_=(-1.0, 0.1, 0.0))
    # glowing ember cracks: a thin glow line along the head's flat, bake-only on both faces
    for sd in (1, -1):
        K.seam("crack%d" % sd, [(5.4, 108.0), (12.0, 102.0), (14.6, 96.0), (12.4, 90.0), (8.4, 85.0)], P["ember"], w=0.5, depth=0.18, z=1.12 * sd + 0.6 * sd * 0.0, side=sd, part="emberline", detail=True)
    for sd in (1, -1):
        K.gem_setting("eye%d" % sd, V((0.0, 96.0, 1.8 * sd)), 1.5, P["gem"], P["iron"], normal=(0, 0, sd), claws=4, sides=6, part="gem", rng_=rr, claw_r=0.4)
    # a ruff of fur under the head and three bone teeth on the poll
    Lb.fur("ruff", [V((2.7 * math.cos(a), HT_BY - 3.0, 2.7 * math.sin(a))) for a in (TAU * k / 16 for k in range(16))], [V((math.cos(a), -0.55, math.sin(a))) for a in (TAU * k / 16 for k in range(16))],
           5.0, 0.55, P["feather2"], rr, part="lashing", tip_mat=P["wood"])
    for k, y in enumerate((90.0, 94.6, 99.2)):
        K.tapered("tooth%d" % k, [V((-9.6, y, 0.0)), V((-12.4, y + 0.4, 0.0)), V((-15.0, y + 1.4, 0.0))], 1.1, 0.12, P["bone"], sides=5, sub=1, part="flint")
    # ---- the lashing: rawhide wound round the haft above and below the head, plus a cross-wrap over the poll, with cord tails
    K.helix_wrap("lash_lo", HT_BY - 11.0, HT_BY + 3.0, 2.5, P["hide"], turns=9.0, cord=0.46, part="lashing", seed=11, sub=5)
    K.helix_wrap("lash_hi", 104.0, 116.0, 2.2, P["hide"], turns=8.0, cord=0.44, part="lashing", seed=12, sub=5)
    K.tapered("lash_tail0", [V(p) for p in ((2.4, HT_BY - 9.0, 1.0), (4.4, HT_BY - 14.0, 2.6), (3.6, HT_BY - 19.0, 1.6))], 0.5, 0.2, P["hide"], sides=4, sub=2, part="lashing")
    K.tapered("lash_tail1", [V(p) for p in ((-2.2, 106.0, 1.0), (-4.2, 101.0, 2.8), (-3.6, 96.4, 1.6))], 0.5, 0.2, P["hide"], sides=4, sub=2, part="lashing")
    K.tapered("lash_tail2", [V(p) for p in ((1.6, 115.0, 1.0), (3.4, 118.4, 2.0), (3.0, 122.0, 1.0))], 0.4, 0.15, P["hide"], sides=4, sub=2, part="lashing")
    # ---- the wrist thong: from the knob to y -24.6, with bone beads, a flint arrowhead charm and feathers
    cord_tail("thong", P["hide"], [(0.0, 4.2, 0.0), (1.0, -2.0, 0.8), (-1.0, -8.0, -0.4), (1.2, -14.0, 0.6), (0.0, -19.0, 0.0), (0.2, -22.0, 0.0)], 0.5, 0.36)
    for k, y in enumerate((-4.0, -10.4, -16.0)):
        Lb.lathe("bbead%d" % k, [(0.01, y - 1.0), (1.2, y - 0.6), (1.5, y), (1.2, y + 0.6), (0.01, y + 1.0)], P["bone"], "pommel_cord", sides=8)
    pts = [(0, -22.4), (-2.0, -23.4), (0.0, -24.633), (2.0, -23.4)]
    K.shard("charm_flint", V((0.0, -23.2, 0.0)), 2.6, P["flint"], rr, part="pommel_cord", sides=5, stretch=1.8, dir_=(0, -1, 0))
    for k, (dx, ln, col) in enumerate(((-1.0, 15.0, "feather"), (0.6, 18.0, "feather2"), (1.8, 13.0, "feather"), (-1.9, 12.0, "feather2"), (0.2, 16.0, "feather"))):
        base = V((dx * 0.4, 1.0 - 3.0 * k, 0.5 * (k - 1)))
        d = V((dx * 0.35, -1.0, 0.18 * (k - 1)))
        rows = Lb.lancet_rows(base, d, ln * 0.55, 3.8, n=8, curve=0.6 * dx, belly=0.4)
        K.strip("feather%d" % k, [(l + V((0, 0, 0.6 * (k - 1))), r + V((0, 0, 0.6 * (k - 1)))) for l, r in rows], P[col], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.12, part="feathers", up=(0, 0, 1), angle=60)
        K.tapered("quill%d" % k, [base, base + d * ln * 0.3, base + d * ln * 0.55], 0.14, 0.06, P["bone"], sides=4, sub=1, part="feathers")
    return K.quad_estimate()


# ================================================================ 4. OVER CUSTOM ARSENAL
def arsenal_pal():
    P = {}
    P["emerald"] = K.surface("oc_emerald", rgb(34, 188, 112), 0.55, 0.14, mottle=0.9, mottle_color=rgb(150, 255, 200), seed=641, bevel=0.1, rim=(rgb(190, 255, 220), 0.5),
                             wear=0.5, wear_color=rgb(220, 255, 235), scratches=0.5, fine=0.4)
    P["emerald2"] = Lb.metal("oc_emerald2", rgb(20, 110, 70), rgb(150, 240, 190), 642, rough=0.24, mottle=rgb(40, 170, 110), dirt=0.5, dirt_col=(0.0, 0.02, 0.01, 1), bump="hammer", bs=0.1)
    P["gold"] = Lb.hammered("oc_gold", rgb(232, 182, 62), rgb(255, 246, 204), 643, mottle=rgb(170, 108, 30))
    P["white"] = Lb.metal("oc_white", rgb(222, 230, 238), rgb(255, 255, 255), 644, rough=0.18, mottle=rgb(150, 180, 200), dirt=0.3)
    P["steel"] = Lb.metal("oc_steel", rgb(150, 164, 178), rgb(250, 252, 255), 645, rough=0.25, dirt=0.45)
    P["wood"] = K.surface("oc_wood", rgb(104, 64, 40), 0.0, 0.7, bump="grain", bump_scale=1.5, bump_strength=0.8, groove=rgb(30, 16, 8), groove_amount=0.9, dirt=0.6, mottle=0.7, mottle_color=rgb(160, 108, 66),
                          fine=0.5, seed=646, bevel=0.0)
    P["wrap"] = Lb.cloth("oc_wrap", rgb(22, 96, 66), rgb(4, 30, 20), 647, mottle=rgb(60, 170, 110), bump="wrap", scale=1.0)
    P["core"] = Lb.paint("oc_core", rgb(26, 40, 34), rgb(110, 150, 130), 648, bump="leather")
    P["glow"] = Lb.glow("oc_glow", rgb(80, 255, 170), 0.95, 649, flicker=0.55)
    P["gem"] = Lb.gem("oc_gem", rgb(50, 240, 150), 1.1, 650)
    P["dia"] = Lb.gem("oc_dia", rgb(176, 232, 255), 1.0, 651)
    return P


def arsenal(opts):
    C.begin(opts)
    C.weights(blade=1.4, edge=0.9, guard=1.1, fan=0.9, halo=1.0, charm=0.8, grip=0.9, pommel=1.0)
    P = arsenal_pal()
    rr = K.rng(8101)
    # ---- the central emerald blade (y 44 .. 124): a broad leaf with a central ridge, a fuller on each face, glowing edge lines and baked runes
    ST = [(0, 5.4), (6, 9.6), (18, 11.4), (34, 10.2), (52, 7.8), (68, 4.6), (76, 2.0), (80, 0.1)]
    T, Lp, Rp = leaf_blade("blade", P["emerald"], ST, 80.0, (0, 44.0, 0), 0.0, 1.0, thick=2.5, sec=SEC_FULLER, seed=41, tipL=(-0.1, 80.0), tipR=(0.3, 79.4), res=9.0)
    edge_glow("edge", P["glow"], Lp, Rp, T, 1.0, 2.5, SEC_FULLER, f=(0.8, 0.93), real=True)
    # ---- the gold crescent guard with a stone, and a collar
    gp = [V(p) for p in ((-15.6, 58.0, 0.0), (-12.0, 49.0, 0.4), (-6.0, 43.2, 0.7), (0.0, 41.4, 0.8), (6.0, 43.2, 0.7), (12.0, 49.0, 0.4), (15.6, 58.0, 0.0))]
    K.tapered("guard_arc", gp[:4], 0.9, 2.1, P["gold"], sides=8, sub=3, part="guard")
    K.tapered("guard_arc2", gp[::-1][:4], 0.9, 2.1, P["gold"], sides=8, sub=3, part="guard")
    for sd in (1, -1):
        K.stepped_plate("guard_plate%d" % sd, [(-6.6, 42.0), (6.6, 42.0), (4.4, 49.0), (0.0, 51.4), (-4.4, 49.0)], 1.5 * sd, P["gold"], steps=3, step_h=0.5, inset=0.7, part="guard", bevel_w=0.0, up=sd)
        K.gem_setting("guard_gem%d" % sd, V((0.0, 46.4, 2.9 * sd)), 2.2, P["gem"], P["gold"], normal=(0, 0, sd), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.45)
        K.rivets("guard_rv%d" % sd, [(V((x, y, 1.7 * sd)), (0, 0, sd)) for x, y in ((-4.6, 44.4), (4.6, 44.4), (-3.0, 48.4), (3.0, 48.4))], P["white"], r=0.5, part="guard", detail=False, sides=5, height=0.45, rng_=rr)
    for sx in (-1, 1):
        Lb.lancet("guard_fin%d" % sx, V((sx * 13.6, 52.0, 0.0)), V((sx * 0.35, 1.0, 0.0)), 12.0, 3.6, P["emerald2"], thick=0.7, part="guard", curve=sx * -2.4, z=0.0, n=7, belly=0.35)
    Lb.lathe("collar", [(1.7, 39.8), (3.2, 40.4), (4.6, 42.0), (3.8, 43.6), (2.0, 44.6), (0.01, 44.8)], P["gold"], "collar", sides=14, squash=0.8)
    # ---- the halo: a golden chakram ring behind the blade (a sun disc), crescent blades round it
    HY, HR, HZ = 90.0, 20.0, -5.4
    hc = (0.0, HY, HZ)
    Lb.flat_ring("halo_rim", hc, HR, [(-1.7, -0.9), (-1.7, 0.9), (0.6, 1.0), (2.3, 0.0), (0.6, -1.0)], P["white"], "halo", segs=36)
    Lb.flat_ring("halo_band", hc, HR - 3.5, [(-1.7, -1.3), (-1.7, 1.3), (1.7, 1.3), (1.7, -1.3)], P["gold"], "halo", segs=32)
    for k in range(6):
        a = TAU * k / 6 + 0.15
        d = V((math.cos(a), math.sin(a), 0))
        fin("hcres%d" % k, V(hc) + d * (HR + 0.5), d, 9.0, 4.6, P["white"], thick=0.8, part="halo", curve=4.2, n=4, hook=2.0)
    for k in range(2):
        a = TAU * k / 2 + math.pi / 2
        K.gem_setting("hg%d" % k, V((0 + (HR - 3.5) * math.cos(a), HY + (HR - 3.5) * math.sin(a), HZ + 1.3)), 1.4, P["dia"] if k % 2 else P["gem"], P["gold"], normal=(0, 0, 1), claws=4, sides=6, part="halo_gem", rng_=rr, claw_r=0.4,
                      up=(math.cos(a), math.sin(a), 0))
    for k in range(6):                                                                         # six spokes tying the halo's band to the blade behind
        a = TAU * k / 6 + 0.5
        d = V((math.cos(a), math.sin(a), 0))
        K.tapered("hspoke%d" % k, [V(hc) + d * 4.0, V(hc) + d * 9.0 + V((0, 0, 0.4)), V(hc) + d * (HR - 3.2)], 0.65, 0.5, P["gold"], sides=5, sub=2, part="halo")
    # ---- the fan: four kunai behind the guard, each a different length and metal, all tipped with a glow edge
    fan = [(-48.0, 46.0, "emerald2", 0.70), (-27.0, 56.0, "steel", 0.84), (27.0, 56.0, "steel", 0.84), (48.0, 46.0, "emerald2", 0.70)]
    KST = [(0, 3.6), (4, 6.2), (14, 7.6), (26, 6.8), (40, 4.8), (52, 2.6), (60, 1.0), (64, 0.1)]
    for k, (tilt, ln, mname, sc) in enumerate(fan):
        T2, Lq, Rq = leaf_blade("fan%d" % k, P[mname], KST, 64.0, (math.sin(math.radians(tilt)) * -0.0, 49.0, -2.8 - 0.4 * abs(k - 1.5)), tilt, sc, thick=1.7, sec=((-1.0, 0.0, 1.0), (0.0, 1.0, 0.0)), seed=51 + k * 5, part="fan", tipL=(-0.1, 64.0),
                                tipR=(0.2, 63.6), res=13.0)
        base = T2(V((0, -1.0, 0)))
        K.blob("fan_pin%d" % k, (base.x, base.y, base.z), (1.7, 1.7, 1.2), P["gold"], seed=k, lump=0.03, rings=5, segs=8, part="fan")
    # ---- the small hatchet charm, hung on a gold chain at the guard's left side
    HX, HZ2 = -18.4, 2.6
    K.chain("charm_chain", [V((-14.8, 57.0, 0.4)), V((-17.0, 56.4, 1.6)), V((HX, 54.6, HZ2))], 1.6, 1.2, 0.26, P["gold"], max_links=3, part="charm", sides=4, segs=6)
    K.tapered("charm_haft", [V((HX, 14.0, HZ2)), V((HX + 0.1, 34.0, HZ2)), V((HX, 53.0, HZ2))], 1.35, 1.1, P["wood"], sides=8, sub=3, part="charm")
    sc2 = 0.55
    hh = [(-3.4, 40.0), (-3.4, 52.0), (0.0, 53.4), (6.0, 56.0), (11.0, 52.0), (12.0, 45.0), (10.4, 38.0), (6.0, 34.0), (3.0, 37.0)]
    hh = [(HX + x * sc2 * 1.0, y) for x, y in hh]
    for sd in (1, -1):
        K.stepped_plate("charm_head%d" % sd, hh, HZ2 + 0.9 * sd, P["steel"], steps=1, step_h=0.35, inset=0.5, part="charm", bevel_w=0.0, up=sd)
        K.gem_setting("charm_gem%d" % sd, V((HX + 0.4, 46.4, HZ2 + 1.8 * sd)), 0.9, P["gem"], P["gold"], normal=(0, 0, sd), claws=0, sides=6, part="charm", rng_=rr, claw_r=0.4)
    K.blob("charm_cap", (HX, 14.0, HZ2), (1.9, 1.5, 1.9), P["gold"], seed=8, lump=0.03, rings=5, segs=8, part="charm")
    # ---- the grip (y 15..41) and a gold ring pommel with a big stone, a cord tail
    K.grip_wrapped("grip", 15.0, 40.0, 3.05, P["core"], P["wrap"], turns=11.0, cord=0.42, bands=(15.6, 27.6, 39.4), band_mat=P["gold"], swell=0.1, part="grip", seed=9, sides=10)
    Lb.lathe("pommel_neck", [(0.01, 11.0), (2.4, 11.6), (3.4, 13.4), (3.6, 15.0)], P["gold"], "pommel", sides=12)
    Lb.ring_torus("pommel_ring", (0, 5.6, 0), 7.4, 1.8, P["gold"], "pommel", sides=6, segs=22)
    Lb.ring_torus("pommel_ring_in", (0, 5.6, 0), 5.5, 0.4, P["white"], "pommel", sides=5, segs=24)
    for sd in (1, -1):
        K.gem_setting("pommel_gem%d" % sd, V((0.0, 5.6, 0.0 + 0.0)) + V((0, 7.4, 1.9 * sd)) * 0.0 + V((0, 7.4, 1.6 * sd)), 1.9, P["gem"], P["gold"], normal=(0, 0, sd), claws=4, sides=6, part="pommel_gem", rng_=rr, claw_r=0.4)
    cord_tail("tail", P["wrap"], [(0.0, -1.6, 0.0), (-1.0, -7.0, 1.0), (1.6, -13.0, -0.6), (-0.8, -19.0, 0.6), (0.2, -23.2, 0.0)], 0.55, 0.36)
    Lb.lathe("tail_bead", [(0.01, -22.6), (1.3, -22.9), (1.8, -23.8), (1.4, -24.5), (0.01, -24.633)], P["gem"], "pommel_cord", sides=9)
    Lb.fur("tassel", [V((0.2 * math.cos(a), -23.0, 0.2 * math.sin(a))) for a in (TAU * k / 9 for k in range(9))],
           [V((0.35 * math.cos(a), -1.0, 0.35 * math.sin(a))) for a in (TAU * k / 9 for k in range(9))], 4.0, 0.34, P["gold"], rr, part="pommel_cord", jit=0.2)
    return K.quad_estimate()


BUILDERS = {}
BUILDERS["kunai"] = (kunai, "vassal_projectile__kunai", "kunai")
BUILDERS["chakram"] = (chakram, "vassal_projectile__chakram", "chakram")
BUILDERS["hatchet"] = (hatchet, "vassal_projectile__hatchet", "hatchet")
BUILDERS["arsenal"] = (arsenal, "vassal_projectile__over_custom_arsenal", "over_custom_arsenal")

if __name__ == "__main__":
    main(sys.argv[1:], BUILDERS, BASE, "dagger", MANIFEST)

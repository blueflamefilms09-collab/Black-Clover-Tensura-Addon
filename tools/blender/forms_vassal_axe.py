"""Weapon-form models of the VASSAL AXE (base vassal_axe, size class battleaxe), built with tools/blender/forms_kit.py.

    python3 -B tools/blender/forms_vassal_axe.py --only timber [--trial DIR] [--beauty DIR] [--quick] [--samples N] [--threads N]

mesh ids (docs/heroweapon/form_models.md): vassal_axe__timber_axe, __pipe_axe, __shine_axe, __curse_axe.
Every form keeps the base axe's frame (mesh space of vassal_axe: haft up the y axis from the butt at y = -12 to the finial at 207.5, the head hub at y 127..173, the cutting
edge on the -x side, the poll on +x): same hand, same grip, same size class.  Designs: Timber Axe = a woodcutter's felling axe (log haft, flint, tornado scrolls),
Pipe Axe = an iron-pipe haft and a guillotine / headsman's blade, Shine Axe = a radiant double-bit axe with a halo of rays, Curse Axe = a ragged, chained, bone-spiked cleaver.
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
C.setup("vassal_axe", "battleaxe", os.path.join(HERE, "forms_manifest_vassal_axe.json"), "vassal_axe", glow=("FFFFFF", 0.14, 0.45, 1.0))
mat, glowmat = C.mat, C.glowmat


def grip_and_butt(P, rr, r=4.7, y0=18.0, y1=74.0, butt="gold"):
    """The hand's part of every axe: a wrapped grip (parts named grip*) and a butt cap."""
    K.grip_wrapped("grip", y0, y1, r, P["core"], P["wrap"], turns=9.0, cord=0.9, bands=(y0 + 0.8, y1 - 0.8), band_mat=P[butt], swell=0.05, part="grip", seed=5, sides=8)


def spike_cap(P, y0, y1, w, t, mat_, part="finial"):
    C.loft("cap", [K.bl.ring(y, K.bl.lozenge(wd, th, flat=wd * 0.3)) for y, wd, th in
                   ((y0, w * 0.55, t * 0.8), (y0 + (y1 - y0) * 0.12, w, t), (y0 + (y1 - y0) * 0.5, w * 0.66, t * 0.7), (y0 + (y1 - y0) * 0.86, w * 0.26, t * 0.32), (y1, 0.1, 0.06))], mat_, part)


# ================================================================ 1. TIMBER AXE  (a woodcutter's felling axe: log haft, flint, tornado scrolls, ivy)
def timber_palette():
    P = {}
    P["bark"] = mat("t_bark", (96, 66, 38), 0.0, 0.8, bump="grain", bump_scale=0.8, bump_strength=1.0, groove=rgb(34, 20, 10), groove_amount=0.8, dirt=0.9, wear=0.45, mottle=0.8, mottle_color=rgb(56, 70, 36))
    P["logend"] = mat("t_logend", (190, 144, 88), 0.0, 0.7, bump="rings", bump_scale=1.2, bump_strength=0.9, groove=rgb(92, 56, 24), groove_amount=0.8, dirt=0.7, wear=0.4)
    P["iron"] = mat("t_iron", (92, 98, 108), 1.0, 0.4, bump="hammer", bump_scale=0.7, bump_strength=0.25, groove=rgb(30, 32, 40), groove_amount=0.6, dirt=0.8, mottle_color=rgb(60, 50, 40))
    P["steel_dark"] = mat("t_steeld", (60, 66, 72), 1.0, 0.5, bump="brushed", bump_strength=0.1, dirt=0.9, wear=0.2)
    P["steel"] = mat("t_steel", (168, 180, 186), 1.0, 0.3, bump="brushed", bump_strength=0.1, dirt=0.5, scratches=0.45, cramp=("x", -90.0, -12.0, [(0.0, rgb(214, 224, 228)), (0.7, rgb(176, 190, 196)), (1.0, rgb(150, 170, 164))]))
    P["wrap"] = mat("t_wrap", (150, 112, 66), 0.0, 0.7, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(60, 38, 16), groove_amount=0.9, dirt=0.8, wear_metal=0.0)
    P["core"] = mat("t_core", (90, 62, 36), 0.0, 0.7, bump="leather", bump_strength=0.5, dirt=0.8)
    P["gold"] = mat("t_gold", (214, 160, 60), 1.0, 0.3, bump="hammer", bump_scale=0.7, bump_strength=0.15, dirt=0.7)
    P["flint"] = mat("t_flint", (74, 82, 90), 0.0, 0.3, mottle=0.9, mottle_color=rgb(150, 160, 168), dirt=0.3, wear=0.8, wear_color=rgb(190, 198, 204), bevel=0.2)
    P["leaf"] = mat("t_leaf", (76, 150, 58), 0.0, 0.55, bump="leather", bump_strength=0.3, mottle=0.8, mottle_color=rgb(170, 190, 70), dirt=0.3, wear=0.4, wear_metal=0.0)
    P["gem"] = glowmat("t_gem", (96, 236, 120), 1.0, 0.5, rim=(rgb(220, 255, 220), 0.5))
    P["lit"] = glowmat("t_lit", (50, 225, 100), 0.7, 0.6)
    return P


def timber_axe(opts):
    C.begin(opts)
    C.weights(blade=1.4, edge_glow=0.8, scrolls=0.7, haft=0.9, grip=1.0, poll=1.0, leaves=0.7, flint=0.6, inlays=0.6, lashing=0.7)
    P = timber_palette()
    rr = K.rng(7001)
    # ---------------------------------------------------------------- the log haft: bark, knots, a slight sway; the head's tenon runs up through the hub
    K.wood_branch("haft", [(0.0, -6.0, 0.0), (1.4, 36.0, 0.5), (-1.0, 84.0, -0.4), (0.5, 128.0, 0.2), (0.0, 176.0, 0.0)], 5.0, 4.3, P["bark"], seed=11, sides=10, knots=4, part="haft", bump=0.10)
    grip_and_butt(P, rr, r=4.55, butt="iron")
    # butt: the cut end of the log, ringed with an iron band, with bark lip
    C.loft("logend", [K.bl.ring(y, K.bl.circle(r, 12)) for y, r in ((-12.0, 0.01), (-11.4, 4.6), (-9.0, 6.4), (-3.0, 6.8), (2.0, 5.6), (5.0, 5.0))], P["logend"], "butt")
    C.loft("butt_band", [K.bl.ring(y, K.bl.circle(r, 12)) for y, r in ((-5.4, 6.7), (-4.4, 7.4), (-1.0, 7.4), (0.0, 6.7))], P["iron"], "butt_band")
    K.rivets("butt_rv", [(V((7.45 * math.cos(a), -2.7, 7.45 * math.sin(a))), (math.cos(a), 0, math.sin(a))) for a in [TAU * k / 6 + 0.3 for k in range(6)]], P["gold"], r=0.8, rng_=rr, jitter=0.2, detail=False)
    # ivy winding up the haft, leaves along it
    pts = [V((5.4 * math.cos(a), 78.0 + 3.3 * a, 5.4 * math.sin(a))) for a in [0.8 * k for k in range(0, 11)]]
    K.vine("ivy", pts, 0.55, P["leaf"], leaves=3, leaf_mat=P["leaf"], rng_=rr, leaf_len=7.5, part="leaves")
    # iron langets run down the haft from the head (flat straps, rivets), bracket fungi and moss grow on the log
    for sx in (1, -1):
        K.bar("langet%d" % sx, V(sx * 5.0, 128.0, 0.0), V(sx * 5.0, 96.0, 0.0), 2.2, 0.9, P["iron"], part="langets", bevel_w=0.1, up=(sx, 0, 0), taper=0.8)
        K.rivets("langet_rv%d" % sx, [(V(sx * 5.8, y, 0.0), (sx, 0, 0)) for y in (124.0, 114.0, 104.0)], P["gold"], r=0.8, rng_=rr, detail=False, sides=5)
    for k, (y, ang, rr_) in enumerate(((84.0, 70.0, 5.2), (93.0, 250.0, 4.0))):
        a = math.radians(ang)
        K.blob("shelf%d" % k, V(5.6 * math.cos(a), y, 5.6 * math.sin(a)), (rr_, 1.4, rr_), P["bark"], seed=k + 5, lump=0.12, rings=4, segs=8, part="fungi", squash=(1, 1, 1))
        K.blob("shelf_cap%d" % k, V(5.6 * math.cos(a), y + 0.9, 5.6 * math.sin(a)), (rr_ * 0.85, 0.8, rr_ * 0.85), P["leaf"], seed=k + 9, lump=0.1, rings=3, segs=8, part="fungi")
    # ---------------------------------------------------------------- the hub: a squared wooden eye in iron straps, stepped plates on both faces, a leaf-set emerald
    C.loft("eye", [C.ring_y(y, 14.0 * s, 10.5 * s, 3.0) for y, s in ((126.0, 0.92), (129.0, 1.0), (171.0, 1.0), (174.0, 0.92))], P["bark"], "hub")
    for y0, y1 in ((125.8, 130.0), (170.0, 174.2)):
        C.loft("strap_%d" % y0, [C.ring_y(y0, 14.6, 11.2, 3.0), C.ring_y(y1, 14.6, 11.2, 3.0)], P["iron"], "straps")
    for sgn in (1, -1):
        outline = [(-12.0, 133.0), (12.0, 133.0), (13.5, 150.0), (12.0, 167.0), (-12.0, 167.0), (-13.5, 150.0)]
        K.stepped_plate("face%d" % sgn, outline, 10.4 * sgn, P["iron"], steps=2, step_h=0.9, inset=1.5, part="hub_plates", up=sgn)
        K.rivets("face_rv%d" % sgn, [(V((x, y, 12.3 * sgn)), (0, 0, sgn)) for x, y in ((-9.5, 136.5), (9.5, 136.5), (-9.5, 163.5), (9.5, 163.5))], P["gold"], r=0.9, rng_=rr, jitter=0.15, detail=False)
        K.gem_setting("gem%d" % sgn, (0, 150.0, 11.9 * sgn), 6.2, P["gem"], P["gold"], normal=(0, 0, sgn), claws=6, sides=8, part="gem", rng_=rr, claw_r=0.5)
        # three carved leaf fins around the setting
        for k, ang in enumerate((90.0, 270.0)):
            a = math.radians(ang)
            L_, R_, Cc = K.leaf_rows(V((8.2 * math.cos(a), 150.0 + 8.2 * math.sin(a), 11.8 * sgn)), V((math.cos(a), math.sin(a), 0)), 6.0, 3.6, rows=4, facing=(0, 0, sgn), curl=1.0 * sgn, seed=k)
            K.strip("fin%d_%d" % (sgn, k), list(zip(L_, R_)), P["leaf"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.35, part="leaves", up=(0, 0, sgn))
    # ---------------------------------------------------------------- the felling blade (v2): a broad cheek with a hooked beard and a curled toe, a stepped section (raised rim, dished field,
    # ground bevel), a chipped edge, tree-rings and tornado scrolls on the cheeks, leaf-glass inlaid along the rim
    outer = [(-66, 207), (-76, 199), (-83, 188), (-88, 174), (-91, 158), (-92, 142), (-91, 126), (-88, 112), (-83, 100), (-76, 90), (-67, 82), (-57, 76.5), (-47, 73.0), (-40, 71.0), (-37.5, 74.5)]
    outer = C.smooth2(outer, 2)
    n = len(outer)
    outer = [(x + (1.0 * math.sin(i * 1.7) if 3 < i < n - 6 else 0.0), y) for i, (x, y) in enumerate(outer)]
    for i in (int(n * 0.22), int(n * 0.42), int(n * 0.60)):                                   # three chips taken out of the edge
        x, y = outer[i]
        outer[i] = (x + 3.0, y)
        outer[i + 1] = (outer[i + 1][0] + 1.6, outer[i + 1][1])
    inner = []
    for i in range(n):                                                                            # the hub side runs straight, then bows out in a concave throat under the beard
        t = i / (n - 1.0)
        if t < 0.68:
            inner.append((-12.0, 176.0 - 50.0 * t / 0.68))
        else:
            u = (t - 0.68) / 0.32
            inner.append((-12.0 - 22.0 * u ** 1.5, 126.0 - 47.0 * u))
    rows = C.fan_rows(inner, outer)
    th = lambda t: 5.8 * (0.34 + 0.66 * math.sin(math.pi * min(1.0, max(0.0, t * 0.92 + 0.04))) ** 0.55)     # noqa: E731
    SEC = ((-1.0, -0.55, -0.05, 0.42, 0.55, 0.64, 0.72, 0.88, 1.0), (0.82, 0.92, 0.86, 0.84, 1.0, 1.0, 0.86, 0.42, 0.0))
    K.strip("blade", rows, P["steel"], SEC[0], SEC[1], th, part="blade", up=(0, 0, 1), angle=55)
    C.edge_band("edge", rows, 0.95, P["lit"], th, "edge_glow", vs=(-1.0, 0.0, 1.0), hs=(0.5, 1.0, 0.0), lift=0.2)
    for sgn in (1, -1):
        # a row of leaf-glass inlays along the raised rim, and tree-ring lines in the dished field (baked)
        for k in range(2, n - 4, 3):
            (ox, oy), (ix, iy) = outer[k], inner[k]
            f_ = 0.60                                                                           # the raised rim sits at 55..72 % of the way to the edge
            cx, cy = ix + (ox - ix) * (0.5 + 0.5 * f_ + 0.0), iy + (oy - iy) * (0.5 + 0.5 * f_)
            tg = V((outer[k + 1][0] - outer[k - 1][0], outer[k + 1][1] - outer[k - 1][1], 0)).normalized()
            nm = V((-tg.y, tg.x, 0))
            zt = th(k / (n - 1.0)) * 1.0
            pl = [(cx - tg.x * 3.0, cy - tg.y * 3.0), (cx + nm.x * 1.1, cy + nm.y * 1.1), (cx + tg.x * 3.2, cy + tg.y * 3.2), (cx - nm.x * 1.1, cy - nm.y * 1.1)]
            K.plate("inlay%d_%d" % (sgn, k), pl, *sorted((zt * sgn - 0.1 * sgn, zt * sgn + 0.45 * sgn)), P["lit"], part="inlays", bevel_w=0.0)
        for r_ in (9.0, 15.0, 21.0, 27.0, 33.0):
            pts = [(-47.0 + r_ * math.cos(a), 150.0 + r_ * 1.15 * math.sin(a)) for a in [math.radians(110 + 14 * i) for i in range(16)]]
            K.engrave_lines("ring%d_%d_" % (sgn, int(r_)), [pts], P["steel_dark"], side=sgn, z=4.9, w=0.16, part="rings", detail=True)
    # tornado scrolls on both cheeks (real raised beads, glowing green) and flint arrowheads set in the cheek
    for sgn in (1, -1):
        for k, (cx, cy, r0, r1, turns, flip) in enumerate(((-48.0, 150.0, 2.5, 19.0, 1.7, 1), (-70.0, 118.0, 1.5, 8.0, 1.3, -1), (-70.0, 184.0, 1.5, 8.0, 1.3, -1))):
            pts2 = K.spiral((cx, cy), r0, r1, turns, start=0.4 * k, n=9 if k == 0 else 6, flip=flip)
            K.filigree("scroll%d_%d" % (sgn, k), pts2, P["lit"], w=0.8, h=0.55, z=5.4, side=sgn, sub=2, taper=(0.5, 1.3), part="scrolls")
        for k, (x, y, sz, ang) in enumerate(((-30.0, 136.0, 2.3, 80), (-80.0, 146.0, 2.0, 180), (-54.0, 98.0, 2.2, 250))):
            a = math.radians(ang)
            K.shard("flint%d_%d" % (sgn, k), V((x, y, 4.8 * sgn)), sz, P["flint"], rr, part="flint", sides=5, stretch=1.5, dir_=V((math.cos(a), math.sin(a), 0.25 * sgn)))
    # the beard is bound to the haft with a lashing of cord, and a leaf-green tassel hangs from the hook
    K.helix_wrap("lash", 80.0, 100.0, 5.6, P["wrap"], turns=6.0, cord=0.5, part="lashing", seed=9, rng_=rr, sub=3, sides=5)
    K.tube("tassel", [V(-37.5, 74.0, 0.0), V(-36.0, 66.0, 1.0), V(-35.0, 57.0, 0.0)], 0.9, P["wrap"], sides=5, sub=2, part="lashing", taper=(0.9, 0.5))
    K.blob("tassel_nut", V(-35.0, 55.0, 0.0), (2.0, 2.4, 2.0), P["leaf"], seed=3, lump=0.1, rings=4, segs=6, part="leaves")
    # ---------------------------------------------------------------- the poll: a squared maul face on +x, banded, with a stub of a wedge above it
    C.box_x("poll", 12.0, 34.0, 150.0, 15.5, 11.0, P["iron"], "poll", taper=1.0, r=3.0)
    C.box_x("poll_face", 33.0, 38.5, 150.0, 14.0, 10.0, P["steel"], "poll", taper=0.92, r=2.5)
    for x0, x1 in ((17.0, 19.8), (27.0, 29.8)):
        C.box_x("pband%d" % x0, x0, x1, 150.0, 16.4, 11.9, P["gold"], "poll_bands", r=2.5)
    K.rivets("poll_rv", [(V((x, 150.0 + dy, 11.5 * s)), (0, 0, s)) for x in (23.0,) for dy in (-9.0, 0.0, 9.0) for s in (1, -1)], P["gold"], r=0.8, rng_=rr, detail=False)
    # a little wedge-hook (the back spur of a felling axe's neck)
    K.tapered("spur", [V((14.0, 168.0, 0)), V((24.0, 178.0, 0)), V((33.0, 176.0, 0)), V((37.0, 168.0, 0))], 2.8, 0.3, P["iron"], sides=5, sub=2, part="poll")
    # ---------------------------------------------------------------- crown: a flint arrowhead on a wooden shoulder, a sprig of leaves at its base
    C.loft("neck", [K.bl.ring(y, K.bl.circle(r, 10)) for y, r in ((172.0, 5.2), (176.0, 6.2), (182.0, 4.4))], P["bark"], "finial")
    spike_cap(P, 180.0, 207.5, 8.0, 4.4, P["flint"], "finial")
    for k, a in enumerate((20.0, 130.0, 250.0)):
        a = math.radians(a)
        L_, R_, Cc = K.leaf_rows(V((0, 180.0, 0)), V((math.cos(a) * 0.8, 0.9, math.sin(a) * 0.55)), 12.0, 5.0, rows=4, facing=(0, 0, 1), curl=1.5, seed=k + 30)
        K.strip("sprig%d" % k, list(zip(L_, R_)), P["leaf"], (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), 0.3, part="leaves", up=(0, 0, 1))
    return K.quad_estimate()


def ray(name, c, d, length, width, mat_, thick=0.5, part="rays", curve=0.0, z=0.0, n=5):
    """A flat lancet from c along d in the xy plane (widest near a third, a needle tip): the rays of a sun, a halo, a fan of light."""
    d = V(d).normalized()
    s = V((-d.y, d.x, 0))
    rows = []
    for i in range(n + 1):
        t = i / n
        w = width * 0.5 * (math.sin(math.pi * min(1.0, t ** 0.6)) ** 0.8 if t < 1 else 0.0)
        p = V(c) + d * (length * t) + s * (curve * t * t) + V((0, 0, z))
        rows.append((p - s * w, p + s * w))
    return K.strip(name, rows, mat_, (-1.0, 0.0, 1.0), (0.0, 1.0, 0.0), thick, part=part, up=(0, 0, 1), angle=50)


# ================================================================ 2. PIPE AXE  (an iron pipe haft with flanges, valve and gauge; a guillotine blade on a pipe T-junction; a chain to throw)
def pipe_palette():
    P = {}
    P["pipe"] = mat("p_pipe", (112, 124, 138), 1.0, 0.34, bump="brushed", bump_strength=0.15, dirt=0.85, scratches=0.5, mottle=0.8, mottle_color=rgb(60, 52, 44), wear_radius=0.5)
    P["dark"] = mat("p_dark", (52, 58, 70), 1.0, 0.42, bump="hammer", bump_scale=0.6, bump_strength=0.3, groove=rgb(20, 22, 28), groove_amount=0.6, dirt=0.9)
    P["blade"] = mat("p_blade", (150, 160, 170), 1.0, 0.3, bump="brushed", bump_strength=0.12, dirt=0.6, scratches=0.5, cramp=("x", -92.0, -12.0, [(0.0, rgb(200, 210, 216)), (0.5, rgb(140, 152, 164)), (1.0, rgb(96, 110, 126))]))
    P["brass"] = mat("p_brass", (196, 146, 62), 1.0, 0.3, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.8)
    P["wrap"] = mat("p_wrap", (70, 72, 78), 0.0, 0.75, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(14, 14, 18), groove_amount=0.9, dirt=0.8, wear_metal=0.0)
    P["core"] = mat("p_core", (48, 50, 58), 1.0, 0.5, bump="leather", bump_strength=0.3, dirt=0.8)
    P["rubber"] = mat("p_rubber", (30, 32, 38), 0.0, 0.8, bump="rings", bump_strength=0.3, dirt=0.5)
    P["face"] = mat("p_face", (232, 228, 214), 0.0, 0.5, dirt=0.4, wear=0.2, bevel=0.0)
    P["lit"] = glowmat("p_lit", (90, 200, 255), 0.8, 0.6)
    P["needle"] = glowmat("p_needle", (255, 110, 70), 1.0, 0.3)
    return P


def pipe_axe(opts):
    C.begin(opts)
    C.weights(blade=1.4, edge_glow=0.8, haft=0.9, grip=1.0, flanges=0.9, gauge=0.8, chain=0.7, tjoint=1.0, porthole=1.0, pinions=0.9, coil=0.8)
    P = pipe_palette()
    rr = K.rng(7002)

    def flange(name, y, r=7.6, th=1.1, bolts=8, part="flanges", nr=5.0):
        C.loft(name, [K.bl.ring(yy, K.bl.circle(rad, 14)) for yy, rad in ((y - th, nr + 0.4), (y - th, r), (y + th, r), (y + th, nr + 0.4))], P["pipe"], part)
        K.rivets(name + "_bolts", [(V((r * 0.78 * math.cos(a), y + th, r * 0.78 * math.sin(a))), (0, 1, 0)) for a in [TAU * k / bolts + rr.uniform(-0.05, 0.05) for k in range(bolts)]], P["dark"], r=0.9, sides=6, rng_=rr, detail=True)

    # ---------------------------------------------------------------- the pipe haft: sections of different bore, flanges, a hand-wheel valve and a gauge
    hf = K.bl.lathe("haft", [(0.01, -9.0), (3.4, -8.5), (4.6, -6.0), (5.0, -2.0), (5.0, 78.0), (5.4, 80.0), (5.4, 98.0), (5.0, 100.0), (5.0, 175.0)], P["pipe"], sides=14, part="haft")
    K._finish_obj(hf, "haft")
    K.grip_wrapped("grip", 18.0, 74.0, 5.0, P["core"], P["wrap"], turns=10.0, cord=0.9, bands=(18.8, 73.2), band_mat=P["brass"], swell=0.02, part="grip", seed=7, sides=8)
    for y in (6.0, 77.0, 99.0, 126.0):
        flange("fl%d" % y, y, r=7.9 if y > 70 else 7.2)
    C.loft("butt_nut", [K.bl.ring(y, K.bl.circle(r, 6)) for y, r in ((-12.0, 0.01), (-11.4, 3.2), (-8.2, 4.4), (-5.0, 4.4), (-3.4, 0.01))], P["brass"], "butt")
    # the chain to throw: a ring at the butt, a few heavy links hanging in a curve
    K.link("butt_ring", V((0.0, -2.0, 6.0)), V((0, 1, 0)), V((0, 0, 1)), 9.0, 6.4, 1.0, P["pipe"], sides=5, segs=10, part="chain")
    K.chain("chain", [V(0, -2.0, 11.0), V(-5, -6.0, 14.0), V(-12, -7.0, 11.0), V(-19, 0.0, 6.0), V(-21, 10.0, 2.0)], 7.4, 4.8, 1.0, P["pipe"], max_links=6, part="chain", up=(0, 0, 1))
    # a bypass loop of brass pipe with a hand wheel, and a pressure gauge on the haft's face
    K.tube("bypass", [V(5.0, 84.0, 0.0), V(13.0, 84.0, 2.0), V(15.5, 92.0, 3.0), V(13.0, 108.0, 2.0), V(5.0, 108.0, 0.0)], 1.9, P["brass"], sides=8, sub=3, part="valve")
    K.gear("wheel", V(16.0, 100.0, 3.0), 6.6, 5.0, 6, 0.9, P["dark"], normal=(1, 0, 0), part="valve", rng_=rr, wear=0.04)
    K.bl.lathe("wheel_hub", [(0.01, -0.2), (1.8, 0.0), (1.8, 3.4), (0.01, 3.6)], P["brass"], sides=8, origin=(16.0, 100.0, 3.0), axis="x", part="valve")
    gz = 5.3
    K.bl.lathe("gauge_body", [(0.01, 0.0), (6.4, 0.0), (6.6, 1.4), (5.6, 2.4), (0.01, 2.4)], P["brass"], sides=16, origin=(0.0, 112.0, gz), axis="z", part="gauge", caps=(False, True))
    K.bl.lathe("gauge_face", [(0.01, 2.3), (5.2, 2.3)], P["face"], sides=16, origin=(0.0, 112.0, gz), axis="z", part="gauge", caps=(False, True))
    K.bl.lathe("gauge_dome", [(5.6, 2.4), (5.0, 3.2), (3.0, 3.9), (0.01, 4.1)], P["pipe"], sides=16, origin=(0.0, 112.0, gz), axis="z", part="gauge", caps=(False, False))
    K.plate("needle", [(-0.5, -1.2), (0.5, -1.2), (0.2, 4.4), (-0.2, 4.4)], gz + 2.35, gz + 2.7, P["needle"], part="gauge_needle")
    # ---------------------------------------------------------------- the head: a pipe T-junction through the hub (poll end on +x), a vent stack above
    K.bl.lathe("tpipe", [(0.01, -17.0), (9.2, -16.0), (10.0, -12.0), (10.0, 36.0), (9.0, 40.0), (5.6, 46.0), (0.01, 49.0)], P["pipe"], sides=14, origin=(0.0, 150.0, 0.0), axis="x", part="tjoint")
    for x in (-13.0, 14.5, 33.0):
        C.loft("tfl%d" % x, [[V((xx, 150.0 + rad * math.cos(a), rad * math.sin(a)) ) for a in [TAU * k / 14 for k in range(14)]] for xx, rad in ((x - 1.2, 10.4), (x - 1.2, 13.6), (x + 1.2, 13.6), (x + 1.2, 10.4))], P["pipe"], "flanges")
    K.rivets("tfl_bolts", [(V((14.5 + 1.2, 150.0 + 12.2 * math.cos(a), 12.2 * math.sin(a))), (1, 0, 0)) for a in [TAU * k / 8 + 0.2 for k in range(8)]], P["dark"], r=1.0, rng_=rr, detail=False)
    for sgn in (1, -1):
        K.stepped_plate("hubplate%d" % sgn, [(-9.0, 138.0), (9.0, 138.0), (11.0, 150.0), (9.0, 162.0), (-9.0, 162.0), (-11.0, 150.0)], 10.2 * sgn, P["dark"], steps=2, step_h=0.8, inset=1.4, part="hub_plates", up=sgn)
    C.loft("vent", [K.bl.ring(y, K.bl.circle(r, 12)) for y, r in ((173.0, 5.4), (178.0, 4.8), (192.0, 4.4), (194.0, 6.2), (197.0, 6.4), (199.0, 4.8))], P["pipe"], "tjoint")
    spike_cap(P, 198.0, 207.5, 6.0, 5.0, P["brass"], "finial")
    for sgn in (1, -1):
        K.tube("vent_pipe%d" % sgn, [V(5.0 * sgn, 176.0, 0.0), V(9.0 * sgn, 182.0, 3.0), V(10.0 * sgn, 196.0, 3.0), V(7.5 * sgn, 203.0, 2.0)], 1.5, P["brass"], sides=6, sub=3, part="vents")
    # ---------------------------------------------------------------- the blade: a guillotine, a slanted heavy edge, spine plates, brace pipes, bolts
    outer = [(-70, 205), (-73, 190), (-77, 172), (-81, 150), (-85, 128), (-89, 108), (-92, 95)]
    outer = [(x + 1.0 * math.sin(i * 2.3), y) for i, (x, y) in enumerate(outer)]
    outer[2] = (outer[2][0] + 2.4, outer[2][1])                                                  # a bite out of the edge
    n = len(outer)
    inner = [(-14.0, 176.0 - 52.0 * i / (n - 1)) for i in range(n)]
    rows = C.fan_rows(inner, outer)
    th = lambda t: 4.6 * (0.45 + 0.55 * math.sin(math.pi * min(1.0, max(0.0, t * 0.9 + 0.05))) ** 0.5)       # noqa: E731
    K.strip("blade", rows, P["blade"], (-1.0, -0.5, 0.0, 0.46, 0.60, 0.70, 0.84, 1.0), (0.84, 0.96, 0.88, 0.84, 1.0, 0.98, 0.46, 0.0), th, part="blade", up=(0, 0, 1), angle=55)
    C.edge_band("edge", rows, 0.93, P["lit"], th, "edge_glow", lift=0.2)
    # portholes of brass and glowing steam-glass (the back one wide, the front one small), two meshing pinions and a spring coil round the haft
    for sgn, (px_, py_, pr_) in ((-1, (-48.0, 150.0, 9.0)), (1, (-60.0, 168.0, 7.0))):
        zc = (5.9 if sgn > 0 else 5.9) * sgn
        K.bl.lathe("port_ring%d" % sgn, [(pr_ * 0.62, 0.0), (pr_, 0.0), (pr_ * 1.06, 1.0 * sgn), (pr_ * 0.95, 2.2 * sgn), (pr_ * 0.72, 2.4 * sgn), (pr_ * 0.62, 1.4 * sgn)], P["brass"], sides=16, origin=(px_, py_, zc), axis="z", part="porthole", caps=(False, False))
        K.bl.lathe("port_glass%d" % sgn, [(0.01, 1.1 * sgn), (pr_ * 0.66, 1.1 * sgn)], P["lit"], sides=16, origin=(px_, py_, zc), axis="z", part="porthole_glass", caps=(False, True))
        K.rivets("port_bolts%d" % sgn, [(V(px_ + pr_ * 0.84 * math.cos(TAU * k / 8), py_ + pr_ * 0.84 * math.sin(TAU * k / 8), zc + 2.2 * sgn), (0, 0, sgn)) for k in range(8)], P["dark"], r=0.7, rng_=rr, detail=True)
    K.gear("pinion_a", V(-31.0, 134.0, 6.5), 8.2, 6.4, 8, 0.6, P["brass"], normal=(0, 0, 1), part="pinions", rng_=rr, wear=0.03, bevel_w=0.0)
    K.gear("pinion_b", V(-44.5, 136.0, 6.5), 5.6, 4.2, 6, 0.6, P["dark"], normal=(0, 0, 1), part="pinions", rng_=rr, wear=0.03, bevel_w=0.0)
    K.spring("coil", V(0.0, 102.0, 0.0), V(0.0, 124.0, 0.0), 6.4, 0.75, 6, P["brass"], sides=5, part="coil", sub=5)
    for sgn in (1, -1):
        K.stepped_plate("spine%d" % sgn, [(-66.0, 180.0), (-22.0, 182.0), (-22.0, 122.0), (-76.0, 124.0)], 4.4 * sgn, P["dark"], steps=2, step_h=0.7, inset=2.0, part="spine_plates", up=sgn)
        K.rivets("spine_rv%d" % sgn, [(V((x, y, 6.0 * sgn)), (0, 0, sgn)) for x in (-30.0, -50.0, -68.0) for y in (128.0, 150.0, 176.0)], P["brass"], r=1.1, rng_=rr, jitter=0.15, detail=False)
        for k, (y, xs) in enumerate(((140.0, -58.0), (158.0, -44.0), (112.0, -66.0))):                       # steam vent slots, glowing
            K.plate("slot%d_%d" % (sgn, k), [(xs, y), (xs + 12.0, y), (xs + 12.0, y + 1.6), (xs, y + 1.6)], 4.5 * sgn - 0.2, 4.5 * sgn + 0.6, P["lit"], part="slots", detail=True)
    # a steam manifold running over the front spine plate: pipe, elbows, a valve, and bolts
    mp = [V(-18.0, 168.0, 8.0), V(-34.0, 168.0, 8.4), V(-34.0, 150.0, 8.4), V(-52.0, 150.0, 8.4), V(-52.0, 132.0, 8.4), V(-70.0, 132.0, 8.0)]
    K.tube("manifold", mp, 1.5, P["brass"], sides=8, sub=2, part="manifold")
    for q in mp[1:-1]:
        K.blob("elbow%d" % int(q.y + q.x), q, (2.3, 2.3, 2.3), P["brass"], seed=int(q.y), lump=0.0, rings=3, segs=6, part="manifold")
    K.gear("manvalve", V(-43.0, 150.0, 10.0), 3.6, 2.6, 6, 0.7, P["dark"], normal=(0, 0, 1), part="manifold", rng_=rr, wear=0.04)
    K.tube("brace_top", [V(-16.0, 177.5, 0.0), V(-42.0, 191.0, 0.0), V(-68.0, 204.0, 0.0)], 2.1, P["brass"], sides=6, sub=3, part="braces")
    K.tube("brace_bot", [V(-16.0, 124.0, 0.0), V(-52.0, 109.5, 0.0), V(-90.0, 95.5, 0.0)], 2.1, P["brass"], sides=6, sub=3, part="braces")
    # ---------------------------------------------------------------- the poll: the pipe's capped end with a ring of nuts, and two weights stacked on it
    K.bl.lathe("cap", [(0.01, 36.0), (11.0, 36.5), (11.6, 40.0), (10.2, 46.0), (6.6, 52.0), (0.01, 54.0)], P["dark"], sides=14, origin=(0.0, 150.0, 0.0), axis="x", part="poll")
    for k in range(6):
        a = TAU * k / 6
        K.rivets("capnut%d" % k, [(V((40.5, 150.0 + 8.6 * math.cos(a), 8.6 * math.sin(a))), (1, 0, 0))], P["brass"], r=1.7, sides=6, rng_=rr, detail=False, height=0.8)
    return K.quad_estimate()


# ================================================================ 3. SHINE AXE  (a radiant double-bit axe: ivory and gold, a sun burst round a great emerald, a fan of light above)
def shine_palette():
    P = {}
    P["ivory"] = mat("s_ivory", (236, 230, 212), 0.55, 0.32, bump="brushed", bump_strength=0.1, dirt=0.35, scratches=0.4, mottle=0.5, mottle_color=rgb(214, 196, 150), wear_color=rgb(255, 255, 250))
    P["gold"] = mat("s_gold", (250, 196, 66), 1.0, 0.24, bump="hammer", bump_scale=0.7, bump_strength=0.14, dirt=0.55, mottle_color=rgb(170, 110, 30), wear_color=rgb(255, 245, 200))
    P["blade"] = mat("s_blade", (236, 238, 242), 0.7, 0.3, bump="brushed", bump_strength=0.08, dirt=0.3, scratches=0.4, cramp=("x", -92.0, 62.0, [(0.0, rgb(250, 252, 255)), (0.5, rgb(214, 222, 232)), (1.0, rgb(250, 240, 214))]))
    P["wrap"] = mat("s_wrap", (238, 232, 218), 0.0, 0.65, bump="cross", bump_scale=1.0, bump_strength=0.8, groove=rgb(120, 104, 70), groove_amount=0.8, dirt=0.5, wear_metal=0.0)
    P["core"] = mat("s_core", (150, 120, 70), 0.0, 0.6, bump="leather", bump_strength=0.4, dirt=0.7)
    P["emerald"] = glowmat("s_emerald", (60, 240, 150), 1.0, 0.5, rim=(rgb(220, 255, 235), 0.6))
    P["diamond"] = glowmat("s_diamond", (225, 245, 255), 0.9, 0.5, rim=(rgb(255, 255, 255), 0.8))
    P["sun"] = glowmat("s_sun", (255, 214, 96), 0.85, 0.6)
    return P


def shine_axe(opts):
    C.begin(opts)
    C.weights(blade=1.3, blade_l=1.3, blade_r=1.0, edge_glow=0.8, sun=1.0, rays=1.0, grip=1.0, gem=1.0, arcs=0.8, corona=0.9, root_band=0.7)
    P = shine_palette()
    rr = K.rng(7003)
    # ---------------------------------------------------------------- haft: ivory shaft, a gold spiral flute, wrapped grip, sun-burst butt
    hf = K.bl.lathe("haft", [(0.01, -4.0), (3.4, -3.0), (4.4, 0.0), (4.4, 78.0), (4.9, 82.0), (4.9, 118.0), (4.4, 122.0), (4.4, 175.0)], P["ivory"], sides=12, part="haft")
    K._finish_obj(hf, "haft")
    K.helix_wrap("flute", 82.0, 122.0, 5.0, P["gold"], turns=4.5, cord=0.7, part="haft_gold", seed=3, rng_=rr)
    K.grip_wrapped("grip", 18.0, 74.0, 4.7, P["core"], P["wrap"], turns=9.0, cord=0.9, bands=(18.8, 73.2), band_mat=P["gold"], swell=0.05, part="grip", seed=6, sides=8)
    disc = [(r * math.cos(TAU * i / 24), -0.5 + r * math.sin(TAU * i / 24)) for i in range(24) for r in ((7.0 if i % 2 == 0 else 3.4),)]
    for s_ in (1, -1):
        K.stepped_plate("butt_star%d" % s_, [(r * math.cos(TAU * i / 20 + 0.1), -0.8 + r * math.sin(TAU * i / 20 + 0.1)) for i in range(20) for r in ((10.4 if i % 2 == 0 else 4.4),)], 0.4 * s_, P["gold"], steps=2, step_h=1.0, inset=0.9, part="butt", up=s_, bevel_w=0.0)
    C.loft("butt_cap", [K.bl.ring(y, K.bl.circle(r, 12)) for y, r in ((-5.0, 0.01), (-4.4, 2.6), (-1.0, 4.8), (2.0, 4.8), (4.0, 4.4))], P["gold"], "butt")
    # ---------------------------------------------------------------- the hub with sun-burst plates and a great emerald (diamond studs on the burst, gold collars)
    C.loft("eye", [C.ring_y(y, 13.0 * s, 11.0 * s, 3.0) for y, s in ((127.0, 0.92), (130.0, 1.0), (170.0, 1.0), (173.0, 0.92))], P["ivory"], "hub")
    for y0, y1 in ((126.2, 130.2), (169.8, 173.8)):
        C.loft("collar_%d" % y0, [C.ring_y(y0, 13.7, 11.7, 3.2), C.ring_y(y1, 13.7, 11.7, 3.2)], P["gold"], "hub_trim")
    for sgn in (1, -1):
        star = [(18.0 * (1.0 if i % 2 == 0 else 0.62) * math.cos(TAU * i / 32 + 0.05), 150.0 + 18.0 * (1.0 if i % 2 == 0 else 0.62) * math.sin(TAU * i / 32 + 0.05)) for i in range(32)]
        star = [(x, y) for x, y in star]
        K.stepped_plate("burst%d" % sgn, star, 10.6 * sgn, P["gold"], steps=2, step_h=0.9, inset=1.0, part="sun", up=sgn, bevel_w=0.0)
        K.gem_setting("emerald%d" % sgn, (0, 150.0, 13.2 * sgn), 9.4, P["emerald"], P["gold"], normal=(0, 0, sgn), claws=8, sides=10, crown=0.35, part="gem", rng_=rr, claw_r=0.55)
        K.rivets("dia%d" % sgn, [(V((14.2 * math.cos(TAU * k / 8 + 0.2), 150.0 + 14.2 * math.sin(TAU * k / 8 + 0.2), 11.6 * sgn)), (0, 0, sgn)) for k in range(8)], P["diamond"], r=1.5, rng_=rr, detail=True)   # diamond studs, baked
    # ---------------------------------------------------------------- the two bits (v2): true crescent moons whose horns curl back toward the haft, a gold root band, two raised filigree arcs,
    # a corona of lancets round the outer rim and studs of light (baked) along the inner arc; the great bit on -x, a smaller one on +x
    def crescent_rows(xc, Rx, Ry, W, thm, n, flip):
        outer, inner = [], []
        for i in range(n):
            th_ = thm * (1.0 - 2.0 * i / (n - 1.0))
            ox, oy = xc - flip * Rx * math.cos(th_), 150.0 + Ry * math.sin(th_)
            w = W * max(0.0, math.cos(th_ * math.pi / (2.0 * thm))) ** 0.85
            outer.append((ox, oy))
            inner.append((ox + flip * w, oy))
        return inner, outer

    def crescent(tag, xc, Rx, Ry, W, thm, flip, thickness, n=24, spikes=7):
        inner, outer = crescent_rows(xc, Rx, Ry, W, thm, n, flip)
        rows = C.fan_rows(inner, outer)
        if flip > 0:                                                                          # rows run hub -> edge: keep l = hub side for both bits
            pass
        th = lambda t: thickness * (0.30 + 0.70 * math.sin(math.pi * min(1.0, max(0.0, t * 0.96 + 0.02))) ** 0.5)       # noqa: E731
        sec_v = (-1.0, -0.55, 0.05, 0.50, 0.70, 0.86, 1.0)
        sec_h = (0.80, 1.0, 0.96, 0.86, 0.66, 0.34, 0.0)
        if flip < 0:
            sec_v = tuple(-v for v in reversed(sec_v))
            sec_h = tuple(reversed(sec_h))
        K.strip("blade" + tag, rows, P["blade"], sec_v, sec_h, th, part="blade" + tag, up=(0, 0, 1), angle=55)
        edge_rows = [(l + (r - l) * 0.93, r) for l, r in rows] if flip > 0 else [(r, l + (r - l) * 0.93) for l, r in rows]
        K.strip("edge" + tag, edge_rows, P["sun"], (-1.0, 0.0, 1.0), (0.4, 1.0, 0.0) if flip > 0 else (0.0, 1.0, 0.4), lambda t: th(t) + 0.25, part="edge_glow", up=(0, 0, 1), angle=60)
        root = [(l, l + (r - l) * 0.14) for l, r in rows[2:-2]]
        K.strip("root" + tag, root, P["gold"], (-1.0, 0.0, 1.0), (0.7, 1.0, 0.7), lambda t: th(0.1 + 0.8 * t) + 0.3, part="root_band", up=(0, 0, 1), angle=60)
        for f_ in (0.30, 0.58):                                                               # raised gold arcs following the crescent
            arc = [(l + (r - l) * (f_ - 0.012), l + (r - l) * (f_ + 0.012)) for l, r in rows[3:-3:2]]
            K.strip("arc%s_%d" % (tag, int(f_ * 100)), arc, P["gold"], (-1.0, 0.0, 1.0), (0.5, 1.0, 0.5), lambda t: th(0.12 + 0.76 * t) * 0.92 + 0.35, part="arcs", up=(0, 0, 1), angle=60)
        for sg in (1, -1):                                                                    # studs of light along the inner arc (baked into the skin, free)
            pts = []
            for k in range(3, n - 3, 2):
                l, r = rows[k]
                q = l + (r - l) * 0.44
                pts.append((V((q.x, q.y, (th(k / (n - 1.0)) * 0.9 + 0.3) * sg)), (0, 0, sg)))
            K.rivets("stud%s_%d" % (tag, sg), pts, P["diamond"], r=1.0, rng_=rr, detail=True)
        for k in range(spikes):                                                               # the corona: lancets standing off the outer rim
            i = int(round(2 + (n - 5) * (k + 0.5) / spikes))
            (ox, oy), (ox2, oy2) = outer[i], outer[min(n - 1, i + 1)]
            tg = V((ox2 - ox, oy2 - oy, 0)).normalized()
            nrm = V((flip * tg.y, -flip * tg.x, 0))
            if nrm.x * flip > 0:
                nrm = -nrm
            ln = 9.0 + 9.0 * math.sin(math.pi * (k + 0.5) / spikes)
            ray("cor%s_%d" % (tag, k), V(ox, oy, 0) - nrm * 1.0, nrm, ln, 3.4, P["gold"] if k % 2 == 0 else P["blade"], thick=0.7, part="corona", z=0.0, n=3)
        return rows, th
    crescent("_l", -20.0, 66.0, 64.0, 40.0, 1.66, 1, 5.2, n=28, spikes=8)
    crescent("_r", 14.0, 46.0, 46.0, 26.0, 1.66, -1, 4.4, n=22, spikes=5)
    for tag, x0, x1, hw, hz in (("l", -12.0, -52.0, 20.0, 3.6), ("r", 12.0, 36.0, 15.0, 3.2)):            # the necks that carry the crescents: stepped ivory plates in gold collars, a bar of light along each
        pl = [(x0, 150.0 - hw * 0.8), (x1, 150.0 - hw), (x1, 150.0 + hw), (x0, 150.0 + hw * 0.8)]
        for sg in (1, -1):
            K.stepped_plate("neck%s%d" % (tag, sg), pl, hz * 0.2 * sg, P["ivory"], steps=3, step_h=hz * 0.28, inset=1.4, part="necks", up=sg, bevel_w=0.15)
            K.filigree("neckvein%s%d" % (tag, sg), [(x0 + (x1 - x0) * 0.05, 150.0), (x0 + (x1 - x0) * 0.5, 150.0 + 3.0), (x1 + (x0 - x1) * 0.02, 150.0)], P["sun"], w=0.9, h=0.5, z=hz * 1.2, side=sg, sub=2, taper=(1.2, 0.5), part="veins")
    # ---------------------------------------------------------------- a fan of light above the hub, a smaller one below: golden lancets
    for k, (ang, ln, wd) in enumerate(((90, 34.0, 5.2), (66, 28.0, 4.2), (114, 28.0, 4.2), (44, 20.0, 3.4), (136, 20.0, 3.4))):
        a = math.radians(ang)
        ray("ray%d" % k, V(0.0 + 3.0 * math.cos(a), 174.0 + 2.0 * math.sin(a), 0.0), V(math.cos(a), math.sin(a), 0), ln, wd, P["gold"] if k % 2 == 0 else P["blade"], thick=0.9, part="rays", z=0.0)
    for k, (ang, ln, wd) in enumerate(((-90, 20.0, 3.6), (-62, 15.0, 3.0), (-118, 15.0, 3.0))):
        a = math.radians(ang)
        ray("rayd%d" % k, V(3.0 * math.cos(a), 126.0 + 2.0 * math.sin(a), 0.0), V(math.cos(a), math.sin(a), 0), ln, wd, P["gold"], thick=0.8, part="rays", z=0.0)
    return K.quad_estimate()


# ================================================================ 4. CURSE AXE  (a ragged cleaver of black iron: bone spikes, chains, a bleeding crack, a slit eye)
def curse_palette():
    P = {}
    P["iron"] = mat("c_iron", (46, 40, 50), 1.0, 0.42, bump="hammer", bump_scale=0.6, bump_strength=0.4, groove=rgb(12, 10, 14), groove_amount=0.7, dirt=0.95, scratches=0.7, mottle=0.9, mottle_color=rgb(98, 30, 24),
                    wear_color=rgb(150, 130, 150), wear_radius=0.5)
    P["blade"] = mat("c_blade", (70, 62, 72), 1.0, 0.36, bump="hammer", bump_scale=0.5, bump_strength=0.35, dirt=0.9, scratches=0.8, mottle=0.9, mottle_color=rgb(120, 34, 28), wear_color=rgb(190, 170, 190),
                     cramp=("x", -96.0, -12.0, [(0.0, rgb(130, 112, 130)), (0.5, rgb(70, 62, 72)), (1.0, rgb(40, 34, 44))]))
    P["bone"] = mat("c_bone", (206, 192, 160), 0.0, 0.55, bump="grain", bump_scale=1.5, bump_strength=0.5, groove=rgb(90, 76, 52), groove_amount=0.7, dirt=0.9, wear=0.6, wear_metal=0.0, mottle=0.9, mottle_color=rgb(120, 90, 54))
    P["wood"] = mat("c_wood", (40, 30, 30), 0.0, 0.7, bump="grain", bump_strength=0.7, groove=rgb(10, 6, 6), groove_amount=0.8, dirt=0.9, wear=0.4)
    P["wrap"] = mat("c_wrap", (66, 20, 26), 0.0, 0.7, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(14, 4, 6), groove_amount=0.9, dirt=0.9, wear_metal=0.0)
    P["chain"] = mat("c_chain", (84, 80, 92), 1.0, 0.4, bump="hammer", bump_strength=0.2, dirt=0.9, scratches=0.7, mottle_color=rgb(90, 40, 30))
    P["lit"] = glowmat("c_lit", (235, 18, 48), 0.8, 0.8)
    P["eye"] = glowmat("c_eye", (255, 50, 20), 0.9, 0.5, rim=(rgb(255, 150, 60), 0.25))
    P["slit"] = mat("c_slit", (6, 4, 8), 0.0, 0.5, dirt=0.0, wear=0.0, bevel=0.0)
    return P


def curse_axe(opts):
    C.begin(opts)
    C.weights(blade=1.4, cracks=0.8, edge_glow=0.8, haft=0.8, grip=1.0, bone=1.0, chains=0.8, gem=1.0, thorns=0.9, skull=1.1, lids=1.0, teeth=0.9, jaw=1.0, ribs=0.8)
    P = curse_palette()
    rr = K.rng(7004)
    # ---------------------------------------------------------------- haft: black wood in a barbed chain, vertebrae under the head, a bone butt
    K.wood_branch("haft", [(0.0, -4.0, 0.0), (-1.0, 40.0, 0.6), (1.2, 90.0, -0.5), (0.0, 130.0, 0.0), (0.0, 176.0, 0.0)], 4.8, 4.4, P["wood"], seed=21, sides=9, knots=3, part="haft", bump=0.14)
    K.grip_wrapped("grip", 18.0, 74.0, 4.6, P["wood"], P["wrap"], turns=10.0, cord=0.95, bands=(18.8, 73.2), band_mat=P["iron"], swell=0.06, part="grip", seed=8, sides=8)
    for k in range(3):                                                                              # vertebrae stacked on the haft below the head
        y = 80.0 + 12.0 * k
        K.bone("vert%d" % k, V(0, y, 0), V(0, y + 8.0, 0), 3.4, P["bone"], knob=0.9, sub=2, sides=8, part="bone", bow=V(0.4 * (-1) ** k, 0, 0), seed=k)
        K.tapered("proc%d" % k, [V(3.0, y + 4.0, 0), V(6.5, y + 4.5, 1.5), V(9.0, y + 2.0, 2.0)], 1.0, 0.1, P["bone"], sides=4, sub=1, part="bone")
    K.bone("butt_bone", V(0, -12.0, 0), V(0, 14.0, 0), 4.6, P["bone"], knob=1.2, sub=4, sides=9, part="bone", seed=5)
    K.spike_array("butt_thorn", [V(3.8 * math.cos(a), 2.0 + 3.0 * k, 3.8 * math.sin(a)) for k, a in enumerate([TAU * j / 3 + 0.3 for j in range(3)])], [V(math.cos(a), 0.15, math.sin(a)) for a in [TAU * j / 3 + 0.3 for j in range(3)]], 6.5, 1.1, P["bone"], rr, part="thorns", sides=4)
    # barbed chain wound over the grip (real links), a second hanging from each side of the head
    hp = [V(5.6 * math.cos(a), 24.0 + 3.8 * a, 5.6 * math.sin(a)) for a in [0.8 * k for k in range(0, 9)]]
    K.chain("wound", hp, 7.0, 4.0, 0.8, P["chain"], max_links=6, part="chains", up=(0, 1, 0), sides=4, segs=5)
    K.chain("hang_r", [V(13.0, 140.0, 4.0), V(24.0, 124.0, 7.0), V(26.0, 104.0, 6.0), V(20.0, 88.0, 3.0)], 9.0, 5.6, 1.1, P["chain"], max_links=6, part="chains", up=(0, 0, 1), sides=4, segs=5)
    K.chain("hang_l", [V(-14.0, 128.0, -4.0), V(-26.0, 112.0, -8.0), V(-30.0, 94.0, -6.0)], 8.0, 5.0, 1.0, P["chain"], max_links=5, part="chains", up=(0, 0, 1), sides=4, segs=5)
    # ---------------------------------------------------------------- the hub: a black block, a slit eye in bone claws, horns
    C.loft("eye_block", [C.ring_y(y, 14.0 * s, 12.0 * s, 3.0) for y, s in ((126.0, 0.9), (129.0, 1.0), (171.0, 1.0), (174.0, 0.9))], P["iron"], "hub")
    for sgn in (1, -1):
        K.stepped_plate("hubp%d" % sgn, [(-11.0, 134.0), (11.0, 132.0), (13.0, 150.0), (10.0, 168.0), (-11.0, 166.0), (-13.0, 150.0)], 12.0 * sgn, P["iron"], steps=2, step_h=0.9, inset=1.6, part="hub_plates", up=sgn)
        K.gem_setting("eye%d" % sgn, (0, 150.0, 13.6 * sgn), 7.0, P["eye"], P["bone"], normal=(0, 0, sgn), claws=7, sides=8, crown=0.3, part="gem", rng_=rr, claw_r=0.8)
        z0, z1 = sorted((19.1 * sgn, 19.7 * sgn))
        K.plate("slit%d" % sgn, [(-0.5, 146.5), (0.5, 146.5), (0.9, 150.0), (0.5, 153.5), (-0.5, 153.5), (-0.9, 150.0)], z0, z1, P["slit"], part="gem_slit")
    for sgn in (1, -1):
        K.tapered("hubhorn%d" % sgn, [V(10.0 * sgn, 172.0, 6.0), V(18.0 * sgn, 183.0, 9.0), V(14.0 * sgn, 198.0, 7.0)], 3.4, 0.2, P["bone"], sides=6, sub=3, part="bone")
    sgn = 0
    if True:
        # the skull's face (v2): a brow ridge with three spikes, upper and lower lids round the eye, cheek horns, a row of upper teeth, and a lower jaw standing either side of the haft
        for si in (1, -1):
            zf = 14.2 * si
            brow = [V(11.5 * math.cos(math.radians(a)), 150.0 + 9.2 + 6.4 * math.sin(math.radians(a)), zf) for a in (170, 140, 110, 90, 70, 40, 10)]
            K.tube("brow%d_%d" % (sgn, si), brow, 1.7, P["bone"], sides=5, sub=1, part="skull", taper=(1.5, 1.5))
            for k, bx in enumerate((-8.0, 0.0, 8.0)):
                K.tapered("browspike%d_%d_%d" % (sgn, si, k), [V(bx, 160.0, zf), V(bx * 1.15, 166.0, zf + 1.0 * si), V(bx * 1.3, 172.0 + 1.2 * (k == 1), zf + 1.6 * si)], 1.5, 0.12, P["bone"], sides=4, sub=1, part="skull")
            lid_u = [V(8.0 * math.cos(math.radians(a)), 150.0 + 6.4 * math.sin(math.radians(a)), zf + 0.4 * si) for a in (200, 160, 120, 90, 60, 20, -20)]
            lid_l = [V(7.0 * math.cos(math.radians(a)), 150.0 + 5.4 * math.sin(math.radians(a)), zf + 0.4 * si) for a in (190, 230, 270, 310, 350)]
            K.tube("lidu%d_%d" % (sgn, si), lid_u, 1.2, P["iron"], sides=4, sub=1, part="lids")
            K.tube("lidl%d_%d" % (sgn, si), lid_l, 1.0, P["iron"], sides=4, sub=1, part="lids")
            for sx in (1, -1):
                K.tapered("cheek%d_%d_%d" % (sgn, si, sx), [V(10.5 * sx, 146.0, zf), V(14.0 * sx, 140.0, zf + 0.6 * si), V(16.5 * sx, 130.0, zf - 0.4 * si)], 2.0, 0.15, P["bone"], sides=4, sub=1, part="skull")
            K.bar("palate%d_%d" % (sgn, si), V(-12.5, 136.5, zf), V(12.5, 136.5, zf), 1.1, 1.2, P["bone"], part="skull", bevel_w=0.1, up=(0, 0, 1))
            for k in range(5):
                tx = -10.0 + 5.0 * k
                K.tapered("tooth%d_%d_%d" % (sgn, si, k), [V(tx, 136.0, zf), V(tx, 132.0, zf), V(tx + 0.2, 127.5 - 1.5 * (k % 2 == 0), zf)], 1.25, 0.12, P["bone"], sides=4, sub=1, part="teeth")
    for si in (1, -1):                                                                         # the lower jaw: a chin plate before and behind the haft, upward fangs, rivets of bone
        chin = [(-12.5, 125.0), (12.5, 125.0), (10.5, 114.0), (5.5, 105.0), (0.0, 101.0), (-5.5, 105.0), (-10.5, 114.0)]
        K.stepped_plate("jaw%d" % si, chin, 7.2 * si, P["bone"], steps=2, step_h=1.1, inset=0.9, part="jaw", up=si, bevel_w=0.15)
        for k in range(5):
            tx = -9.0 + 4.5 * k
            K.tapered("fang%d_%d" % (si, k), [V(tx, 125.0, 8.2 * si), V(tx, 129.0, 8.4 * si), V(tx, 133.5 + 1.5 * (k % 2), 8.6 * si)], 1.2, 0.12, P["bone"], sides=4, sub=1, part="teeth")
        K.tapered("jawhorn%d_l" % si, [V(-11.0, 120.0, 8.0 * si), V(-19.0, 112.0, 8.0 * si), V(-25.0, 98.0, 8.0 * si)], 2.4, 0.15, P["bone"], sides=5, sub=2, part="jaw")
        K.tapered("jawhorn%d_r" % si, [V(11.0, 120.0, 8.0 * si), V(19.0, 112.0, 8.0 * si), V(25.0, 98.0, 8.0 * si)], 2.4, 0.15, P["bone"], sides=5, sub=2, part="jaw")
    # ---------------------------------------------------------------- the blade: a ragged cleaver, teeth bitten into the edge, a chunk gone, a long crack that bleeds light
    base = C.smooth2([(-62, 205), (-76, 188), (-84, 166), (-88, 146), (-86, 124), (-80, 104), (-70, 88), (-58, 76)], 3)
    outer = []
    for i, (x, y) in enumerate(base):                                                            # real saw teeth: a long hooked fang every other row, a short stub between
        tooth = -12.0 * ((i * 7 + 3) % 5) / 4.0 * (1 if i % 2 == 0 else 0.25)
        outer.append((x + tooth + 1.2 * math.sin(i * 1.3), y))
    n = len(outer)
    for i in range(int(n * 0.42), int(n * 0.55)):                                                    # a bitten-out piece
        outer[i] = (outer[i][0] + 16.0 * math.sin(math.pi * (i - n * 0.42) / (n * 0.13)), outer[i][1])
    inner = [(-13.0, 176.0 - 52.0 * i / (n - 1)) for i in range(n)]
    rows = C.fan_rows(inner, outer)
    th = lambda t: 6.8 * (0.30 + 0.70 * math.sin(math.pi * min(1.0, max(0.0, t * 0.92 + 0.04))) ** 0.5)       # noqa: E731
    K.strip("blade", rows, P["blade"], (-1.0, -0.55, 0.0, 0.40, 0.56, 0.66, 0.78, 0.90, 1.0), (0.86, 0.96, 0.9, 0.86, 1.0, 0.96, 0.62, 0.30, 0.0), th, part="blade", up=(0, 0, 1), angle=55)
    C.edge_band("edge", rows, 0.95, P["lit"], th, "edge_glow", lift=0.2)
    for sg_ in (1, -1):                                                                          # bone ribs raking across the cheek toward the teeth
        for k, (ya, yb) in enumerate(((168.0, 186.0), (150.0, 150.0), (132.0, 116.0))):
            K.tapered("rib%d_%d" % (sg_, k), [V(-18.0, ya, 6.4 * sg_), V(-40.0, (ya + yb) * 0.5, 7.4 * sg_), V(-64.0, yb * 0.97 + 4.0, 6.0 * sg_)], 1.5, 0.2, P["bone"], sides=5, sub=2, part="ribs")
    for sgn in (1, -1):
        for k, pts in enumerate((((-18, 168), (-32, 162), (-38, 152), (-52, 150), (-58, 138), (-72, 134)), ((-30, 128), (-40, 120), (-44, 108), (-56, 104)))):
            K.filigree("crack%d_%d" % (sgn, k), pts, P["lit"], w=0.7, h=0.5, z=6.5, side=sgn, sub=2, taper=(1.3, 0.3), part="cracks")
    # a long bone beard hooking down from the heel, and a bone-spike poll on +x
    K.tapered("beard", [V(-30.0, 120.0, 0.0), V(-44.0, 92.0, 1.0), V(-60.0, 70.0, 0.5), V(-76.0, 62.0, 0.0)], 5.0, 0.3, P["bone"], sides=5, sub=2, part="bone")
    for k, (dy, ang, ln) in enumerate(((10.0, 12.0, 36.0), (-8.0, -18.0, 30.0), (0.0, 38.0, 22.0))):
        a = math.radians(ang)
        K.tapered("poll_horn%d" % k, [V(14.0, 150.0 + dy * 0.4, 0.0), V(14.0 + ln * 0.5, 150.0 + dy + ln * math.sin(a) * 0.4, 1.5), V(14.0 + ln, 150.0 + dy + ln * math.sin(a), 0.0)], 4.6 - k, 0.2, P["bone"], sides=5, sub=2, part="bone")
    # crown of thorns above the head
    ang = [-38.0, -18.0, 0.0, 18.0, 38.0]
    K.spike_array("thorn", [V(0.0, 174.0, 0.0)] * 3, [V(math.sin(math.radians(a)), math.cos(math.radians(a)), 0.15 * (k - 1)) for k, a in enumerate((-30.0, 0.0, 30.0))], [26.0, 33.0, 26.0], 3.0, P["iron"], rr, part="thorns", sides=5, bend=1.5)
    # cursed light dripping from the edge
    for k, (x, y) in enumerate(((-78.0, 112.0), (-84.0, 136.0))):
        K.drip("drip%d" % k, V(x, y, 0.0), 16.0 + 4.0 * k, 1.5, P["lit"], seed=k + 4, sides=6, part="drips")
    return K.quad_estimate()


C.register("timber", timber_axe, "timber_axe")
C.register("pipe", pipe_axe, "pipe_axe")
C.register("shine", shine_axe, "shine_axe")
C.register("curse", curse_axe, "curse_axe")

if __name__ == "__main__":
    C.main(sys.argv[1:])

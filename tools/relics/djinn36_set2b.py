"""djinn36_set2b: Kimaris, Amdusias, Belial."""
from djinn36_lib import *                                     # noqa: F401,F403
import djinn36_lib as L
from djinn36_reg import register

HR = dub.HALO_R


def ring_xz(R_, y=0.0, n=12):
    return dub.ring_xz(R_, y, n)


def ring_xy(R_, z=0.0, n=14, a0=0, a1=360):
    return dub.ring_xy(R_, z, n, a0, a1)


def leaf_poly(base, ang, Ln, W, sgn=1.0, curl=0.0, n=3):
    return dub.leaf_blade(None, base, ang, Ln, W, sgn, curl, n)


# ============================================================================================ KIMARIS: the rider of the dark steed
def head_kimaris(k):
    head_base(k, "open", nasal=False)
    du.box(k.part("helm", "chamfron", "plate"), (-2.5, 4.9, 3.9), (2.5, 8.2, 5.3), chamfer=0.7)                 # a steed's face plate across the brow
    du.bar(k.part("helm", "chamfron_ridge", k.trim()), (0, 8.0, 5.4), (0, 5.2, 5.5), 0.8, 0.3, up=(0, 0, 1), taper=0.6)
    for sgn in (-1, 1):
        gemz(k, "helm", f"eye_{sgn}", (sgn * 1.6, 6.5, 5.5), 0.55, 1.0)
        orb(k.part("helm", f"nostril_{sgn}", "plate", smooth=0), (sgn * 0.8, 5.2, 5.5), 0.35, sides=4)
        du.bar(k.part("helm", f"bridle_{sgn}", "cloth"), (sgn * 2.5, 6.0, 4.6), (sgn * 4.2, 4.4, 1.0), 0.8, 0.2, up=(0, 0, 1), section="rect")
    for sgn, nm in ((1, "l"), (-1, "r")):                                                                    # tall horse ears
        spike(k.part("ears", "ear_" + nm, "plate"), (sgn * 2.8, 7.8, 0.2), (sgn * 0.2, 1.0, -0.1), 5.4, 1.4, 4)
        spike(k.part("ears", "ear_in_" + nm, "crystal", layer=2), (sgn * 2.8, 8.0, 0.7), (sgn * 0.2, 1.0, -0.1), 3.6, 0.7, 4)
    for i, x in enumerate((-2.8, -1.4, 0.0, 1.4, 2.8)):                                                      # a flaming mane down the back of the head
        tongue(k.part("helm", f"mane_{i}", "crystal", layer=2, smooth=45), (x, 8.4, -0.5 - 0.6 * abs(x)), (0, 0.5, -1.0), 10.0 - 1.4 * abs(x), 2.2, 0.5, 0.8, i * 1.3, plane="yz", n=4)
    hair_blades(k, (-2.4, 0.0, 2.4), 8.0, -1.2, 9.0, 2.0, (0, 0.5, -0.9), (0, -0.8, -0.6), mat="cloth", spread=0.4, name="mane")
    du.box(k.part("crest", "chamfron", "plate"), (-1.8, 5.2, 4.0), (1.8, 7.4, 5.0), chamfer=0.5)
    gemz(k, "crest", "eye", (0, 6.4, 5.2), 0.5, 1.0)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_kimaris(k):
    dua.chest_cuirass(k)
    pts = arc_pts((0.0, -7.2, 3.1), 2.3, -60, 240, 10)
    du.tube_direct(k.part("chest", "horseshoe", k.trim(), smooth=40), pts, 0.5, 0.5, 4)
    for i, a in enumerate((-30, 20, 70, 110, 160, 210)):
        glow_orb(k, "chest", f"nail_{i}", (2.3 * math.cos(rad(a)), -7.2 + 2.3 * math.sin(rad(a)), 3.5), 0.22, sides=4)


def pauldron_kimaris(k):
    def fn(k, piece, sgn, cx, bx):
        for dz in (-1.2, 1.2):
            spike(k.part(piece, f"ear_{dz}", "plate"), (bx - sgn * 0.6, 2.6, dz), (sgn * 0.3, 1.0, 0), 4.6, 1.0, 4)
        for i in range(3):
            spike(k.part(piece, f"mane_{i}", "cloth"), (bx - sgn * 1.6, 2.4, -1.8 - 0.4 * i), (sgn * 0.1, 0.5, -1.0), 4.6 - i, 0.8, 4)
        glow_orb(k, piece, "gem", (bx + sgn * 0.6, 3.8, 2.4), 0.6, sides=5)
    L.pauldrons(k, fn, 4.6, "plate", 1)


def wings_kimaris(k, sgn):
    """KIMARIS_BATWING: the dark steed's wing: bone fingers with a scalloped black membrane between them, a hooked claw at the wrist, a fringe of fire along the edge."""
    bone_wing(k, sgn, "plate", 1.3)
    sdp = sd(sgn)
    spike(k.part(f"wing_out_{sdp}", "claw", "bone"), (0, 0.6, 0.2), (sgn * 0.5, 1.0, 0), 4.6, 0.8, 4)
    inner = [(0.0, 0.3), (sgn * 10.0, 0.3), (sgn * 8.0, -5.4), (sgn * 3.0, -7.0), (0.0, -3.6)]
    lf(k, f"wing_in_{sdp}", "mem", "cloth", [(x, y, -0.1) for x, y in inner])
    fingers = []
    for j, (a, Ln) in enumerate(((30, 17.0), (8, 24.0), (-14, 24.0), (-38, 19.0), (-62, 13.0))):
        path = arc_path((0, 0, 0), a, Ln, -16, 3, "xy", sgn)
        du.tube_direct(k.part(f"wing_out_{sdp}", f"finger_{j}", "plate", smooth=40), path, 0.5, 0.1, 4)
        fingers.append(np.array(path[-1]))
    for j in range(len(fingers) - 1):
        a, b = fingers[j], fingers[j + 1]
        mid = (a + b) / 2 * 0.74
        lf(k, f"wing_out_{sdp}", f"mem_{j}", "cloth", [(0, 0, -0.15), tuple(a + (0, 0, -0.15)), (mid[0], mid[1], -0.15), tuple(b + (0, 0, -0.15))])
        rib(k, f"wing_out_{sdp}", f"edge_{j}", "crystal", tuple(a + (0, 0, 0.1)), (mid[0], mid[1], 0.1), 0.4, 0.14, layer=2, taper=0.5)
        rib(k, f"wing_out_{sdp}", f"edge_b{j}", "crystal", (mid[0], mid[1], 0.1), tuple(b + (0, 0, 0.1)), 0.4, 0.14, layer=2, taper=0.5)
        tongue(k.part(f"wing_out_{sdp}", f"fire_{j}", "crystal", layer=2, smooth=45), (mid[0], mid[1], 0.0), (mid[0] * 0.3, mid[1] * 0.3, 0.0), 3.4, 1.4, 0.4, 0.5, j, n=3)


def tail_kimaris(k):
    seg_core(k, 0.8, "plate", "seg", 6, 0.9)
    for i, off in enumerate((-0.9, 0.0, 0.9)):
        sweep_flat(k.part("tail_seg", f"hair_{i}", "cloth", smooth=45), [(off, 0.3, -0.2), (off * 1.8, -0.6, -3.0), (off * 2.6, -1.8, -6.0)], 1.5, 0.3, pf_ribbon, (1, 0, 0))
    for i in range(4):
        tongue(k.part("tail_tip", f"fire_{i}", "crystal", layer=2, smooth=45), (0, 0, 0), ((i - 1.5) * 0.25, -0.2 * (i % 2), -1.0), 11.0 - 2.0 * (i % 2), 2.8, 0.5, 0.9, i * 1.1, plane="xy" if i % 2 else "yz", n=5)
    for i, x in enumerate((-1.2, 1.2)):
        sweep_flat(k.part("tail_tip", f"hair_{i}", "cloth", smooth=45), [(x * 0.4, 0, 0), (x * 1.2, -1.0, -4.0), (x * 2.0, -2.4, -9.0)], 1.8, 0.3, pf_ribbon, (1, 0, 0))


def back_kimaris(k):
    """KIMARIS_HORSESHOE: a great horseshoe stands open-end down behind the shoulders, its nails glowing, two long pennons flying from its crown."""
    R_ = 12.5
    pts = arc_pts((0.0, -2.0, 0.0), R_, -62, 242, 14)
    du.tube_direct(k.part("backdrop", "shoe", "plate", smooth=40), pts, 1.2, 1.2, 5)
    du.tube_direct(k.part("backdrop", "shoe_in", k.trim()), [(x * 0.84, (y + 2.0) * 0.84 - 2.0, z + 0.5) for (x, y, z) in pts], 0.4, 0.4, 4)
    for i, a in enumerate(np.linspace(-40, 220, 8)):
        r = rad(a)
        glow_orb(k, "backdrop", f"nail_{i}", (R_ * math.cos(r), -2.0 + R_ * math.sin(r), 1.2), 0.55, sides=5)
    for sgn in (-1, 1):
        spike(k.part("backdrop", f"calk_{sgn}", "plate"), (sgn * R_ * 0.47, -2.0 - R_ * 0.88, 0), (0, -1.0, 0), 3.4, 1.0, 4)
    for sgn in (-1, 1):
        ribbon(k, "backdrop", f"pennon_{sgn}", "cloth", [(sgn * 3.0, R_ - 2.0, 0.6), (sgn * 6.0, R_ + 1.0, 0.8), (sgn * 10.0, R_ - 1.0, 0.6), (sgn * 14.0, R_ - 4.0, 0.6)], 3.0, 0.25, 0.7)
        rib(k, "backdrop", f"pennon_edge_{sgn}", "crystal", (sgn * 3.0, R_ - 0.6, 0.8), (sgn * 14.0, R_ - 2.6, 0.8), 0.4, 0.14, layer=2, taper=2.0)


def mantle_kimaris(k):
    cape(k, 26.0, 9.0, "cloth", 1.2, k.trim())
    jag_hem(k, "mantle", "rag", "cloth", -26.2, -5.6, 5.6, 4.6, 6, 0.5)


def weapon_kimaris(k):
    """KIMARIS_LANCE: a cavalry lance 3.6 blocks long: a hand-guard cone, a long fluted shaft, a slim blade, a swallow-tailed pennon edged in fire, a void in the socket."""
    dua.w_shaft(k, -14, 44, 0.8, "plate", "shaft")
    for j, y in enumerate((-10, -6, -2)):
        dua.w_ring(k, y, 1.15, f"wrap_{j}", "cloth", 1.2)
    ml.lathe(k.part("weapon", "vamplate", "plate", smooth=40), "y", (0, 2.0, 0), [(0.0, 3.8), (2.0, 3.0), (5.0, 1.2), (7.0, 0.9)], sides=S(12), caps=(False, False))
    collar(k.part("weapon", "vamplate_rim", k.trim()), 1.6, 3.9, 0.7, lip=0.2, sides=S(12))
    glow_orb(k, "weapon", "vamplate_gem", (0, 2.4, 3.6), 0.5, sides=4)
    orb(k.part("weapon", "butt", k.trim(), smooth=40), (0, -15.2, 0), 1.5, sides=8)
    dua.w_ring(k, 43.0, 1.5, "socket", "trim", 1.8)
    glow_orb(k, "weapon", "socket_gem", (0, 45.0, 1.6), 0.8)
    dua.spear_blade(k, 46.0, 16.0, 3.2, 0.9, "blade", "plate")
    pen = [(1.0, 40.0, 0.0), (12.0, 42.0, 0.0), (9.0, 38.0, 0.0), (12.4, 33.6, 0.0), (1.0, 35.0, 0.0)]
    lf(k, "weapon", "pennon", "cloth", pen)
    rib(k, "weapon", "pennon_edge", "crystal", (1.0, 40.3, 0.2), (12.0, 42.2, 0.2), 0.5, 0.18, layer=2, taper=1.0)
    rib(k, "weapon", "pennon_edge_b", "crystal", (1.0, 34.7, 0.2), (12.4, 33.4, 0.2), 0.5, 0.18, layer=2, taper=1.0)
    tongue(k.part("weapon", "fire", "crystal", layer=2, smooth=45), (12.0, 38.0, 0.0), (1.0, 0.0, 0.0), 4.4, 2.2, 0.4, 0.5, 0.0, n=3)


register("KIMARIS", head=head_kimaris, chest=chest_kimaris, pauldron=pauldron_kimaris, wings=wings_kimaris, tail=tail_kimaris, back=back_kimaris, mantle=mantle_kimaris, weapon=weapon_kimaris)


# ============================================================================================ AMDUSIAS: the maestro of the unicorn horn
def head_amdusias(k):
    head_base(k, "circlet", nasal=False, cheeks=False)
    helix_horn(k, "helm", "unicorn", "bone", (0.0, 6.4, 4.0), (0.0, 1.0, 0.38), 13.0, 1.0, 0.06, turns=4.5, ridge_r=0.3, steps=16)
    collar(k.part("helm", "horn_ring", k.trim()), 0.0, 1.5, 0.8, lip=0.2, sides=S(10), axis=(0, 1.0, 0.38), origin=(0, 6.2, 3.9))
    glow_orb(k, "helm", "horn_tip", (0.0, 6.4 + 13.0 * 0.93, 4.0 + 13.0 * 0.35), 0.4, sides=4)
    hair_blades(k, (-3.0, -1.5, 0.0, 1.5, 3.0), 8.0, -1.0, 12.0, 2.0, (0, 0.5, -0.9), (0, -0.9, -0.5), mat="cloth", spread=0.7, name="mane")
    for sgn, nm in ((1, "l"), (-1, "r")):
        blade3(k.part("ears", "ear_" + nm, "cloth", smooth=45), (sgn * 3.6, 7.0, -0.2), (sgn * 0.6, 0.8, 0.0), (sgn * 0.8, 0.6, -0.1), 6.0, 2.2, 0.5, wdir=(0, 0, 1), n=4)
        blade3(k.part("ears", "ear_in_" + nm, "crystal", layer=2, smooth=45), (sgn * 3.7, 7.2, 0.2), (sgn * 0.6, 0.8, 0.0), (sgn * 0.8, 0.6, -0.1), 4.4, 0.9, 0.3, wdir=(0, 0, 1), n=3)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # a tuning fork at each temple
        for dz in (-0.8, 0.8):
            du.bar(k.part(piece, f"prong_{dz}", "trim"), (sgn * 4.5, 5.6, dz), (sgn * 4.7, 10.2, dz), 0.5, 0.35, up=(1, 0, 0), taper=0.4, section="rect")
        orb(k.part(piece, "base", k.trim(), smooth=0), (sgn * 4.5, 5.2, 0), 0.8, sides=5)
        glow_orb(k, piece, "tip", (sgn * 4.7, 10.6, 0), 0.4, sides=4)
    helix_horn(k, "crest", "unicorn", "bone", (0, 5.8, 4.4), (0, 1.0, 0.4), 6.0, 0.8, 0.05, turns=3, steps=10)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_amdusias(k):
    dua.chest_harness(k)
    for sgn in (-1, 1):                                                                                     # a lyre: two curved arms, a crossbar, five strings of light
        tapered_tube(k, "chest", f"arm_{sgn}", k.trim(), [(sgn * 0.9, -13.0, 3.0), (sgn * 2.2, -11.0, 3.0), (sgn * 2.0, -8.6, 3.0), (sgn * 0.7, -7.6, 3.0)], 0.3, 0.26, 4)
    du.bar(k.part("chest", "yoke", k.trim()), (-2.3, -7.6, 3.0), (2.3, -7.6, 3.0), 0.5, 0.3, up=(0, 0, 1))
    for i, x in enumerate((-1.2, -0.6, 0.0, 0.6, 1.2)):
        du.bar(k.part("chest", f"string_{i}", "crystal", layer=2), (x, -7.8, 3.2), (x * 0.7, -12.6, 3.2), 0.16, 0.1, up=(0, 0, 1))


def pauldron_amdusias(k):
    def fn(k, piece, sgn, cx, bx):
        for j, h in enumerate((4.6, 6.4, 5.0)):                                                              # three organ pipes
            x = bx - sgn * 1.4 + sgn * 1.5 * j
            ml.loft(k.part(piece, f"pipe_{j}", "plate", smooth=40), "y", (x, 2.4, -0.4 * j), [(0, 1, 1), (h, 1, 1)], C(0.62, S(8)), caps=(False, True))
            glow_orb(k, piece, f"mouth_{j}", (x, 2.4 + h * 0.3, 0.6 - 0.4 * j), 0.28, sides=4)
            collar(k.part(piece, f"pipe_ring_{j}", k.trim()), 2.4 + h * 0.55, 0.78, 0.5, lip=0.1, sides=S(8), origin=(x, 0, -0.4 * j))
    L.pauldrons(k, fn, 4.4, "plate", 1)


def wings_amdusias(k, sgn):
    """AMDUSIAS_HARP: each wing is a harp: a curved frame of silver, strings of light drawn from the bone to the frame, a spray of feathers at its tip."""
    bone_wing(k, sgn, "plate", 1.1)
    sdp = sd(sgn)
    for seg, B, ang, Ln, curl, ns, spacing in (("in", INNER, 18.0, 15.0, -70, 5, 2.0), ("out", OUTER, 44.0, 29.0, -86, 9, 1.55)):
        frame = arc_path((0, 0, 0), ang, Ln, curl, 7, "xy", sgn)
        du.tube_direct(k.part(f"wing_{seg}_{sdp}", "frame", "trim", smooth=40), frame, 0.6, 0.3, 4)
        for i in range(ns):
            f = (i + 1) / (ns + 1)
            tgt = np.array(frame[int(round(f * 7))])
            src = np.array([sgn * spacing * (i + 1) * 0.8, 0.0, 0.0]) if seg == "out" else np.array([sgn * spacing * (i + 1) * 0.9, 0.0, 0.0])
            rib(k, f"wing_{seg}_{sdp}", f"string_{i}", "crystal", tuple(src + (0, 0, 0.3)), tuple(tgt + (0, 0, 0.3)), 0.26, 0.1, layer=2, taper=0.0)
        if seg == "out":
            fan(k, f"wing_{seg}_{sdp}", "tip", tuple(frame[-1]), (-30, -52, -74, -96), (7.0, 8.0, 7.0, 5.5), 1.8, "cloth", -10, 3, sgn)
            glow_orb(k, f"wing_{seg}_{sdp}", "tuner", tuple(np.array(frame[0]) + (0, 0.3, 0.3)), 0.6, sides=5)
    lf(k, "wing_in_" + sdp, "web", "cloth", [(0, -0.2, -0.1), (sgn * 9.0, -1.0, -0.1), (sgn * 6.0, -5.0, -0.1), (0, -3.0, -0.1)])


def tail_amdusias(k):
    seg_core(k, 0.6, "trim", "cord", 6, 0.8)
    sweep_flat(k.part("tail_seg", "ribbon", "cloth", smooth=45), [(0, 0.6, 0), (0, 1.6, -2.0), (0, 0.6, -4.0), (0, 1.4, -6.0)], 1.8, 0.3, pf_ribbon, (1, 0, 0))
    glow_orb(k, "tail_seg", "note", (0, 0.2, -3.0), 0.45, sides=4)
    clef = [(0, math.sin(t) * (0.8 + 0.5 * t) * 1.2, -1.0 - (1.0 - math.cos(t)) * (0.8 + 0.5 * t) * 1.2 - 0.6 * t) for t in np.linspace(0, 4.2, 12)]
    du.tube_direct(k.part("tail_tip", "clef", "trim", smooth=40), clef, 0.5, 0.2, 4)
    orb(k.part("tail_tip", "head", "crystal", layer=2, smooth=0), clef[-1], 0.8, sides=5)
    sweep_flat(k.part("tail_tip", "stem", "cloth", smooth=45), [(0, 0, 0), (0, 1.0, -4.0), (0, 0.4, -8.0)], 1.4, 0.3, pf_ribbon, (1, 0, 0))


def halo_amdusias(k):
    du.sweep(k.part("halo", "ring", k.trim()), ring_xz(HR), C(0.3, S(5)))
    for i in range(6):
        a = 2 * PI * i / 6
        c = (HR * math.cos(a), 0.0, HR * math.sin(a))
        orb(k.part("halo", f"note_{i}", "crystal", layer=2, smooth=0), (c[0], 0.2, c[2]), 0.7, sides=5)
        du.bar(k.part("halo", f"stem_{i}", k.trim()), (c[0], 0.4, c[2]), (c[0], 4.0 + (i % 2), c[2]), 0.3, 0.2, up=(math.cos(a), 0, math.sin(a)), taper=0.4)
        if i % 2 == 0:
            sweep_flat(k.part("halo", f"flag_{i}", "crystal", layer=2), [(c[0], 4.0, c[2]), (c[0] - math.sin(a) * 1.6, 3.2, c[2] + math.cos(a) * 1.6), (c[0] - math.sin(a) * 1.8, 2.0, c[2] + math.cos(a) * 1.8)], 0.8, 0.2, pf_taper, (0, 1, 0))


def back_amdusias(k):
    """AMDUSIAS_ORGAN: the pipes of an organ fan out behind the shoulders: eleven pipes of falling length on a curved bar, a glowing mouth on each, a lyre-crest above."""
    base_r = 7.0
    du.tube_direct(k.part("backdrop", "bar", k.trim(), smooth=40), arc_pts((0.0, -9.0, -0.3), base_r, 10, 170, 8), 0.7, 0.7, 4)
    for i, a in enumerate(np.linspace(14, 166, 11)):
        r = rad(a)
        Ln = 10.0 + 12.0 * math.sin(r) ** 1.2
        foot = np.array([base_r * math.cos(r), -9.0 + base_r * math.sin(r), -0.3])
        d = np.array([math.cos(r) * 0.35, 1.0, 0.0])
        d = d / np.linalg.norm(d)
        ml.loft(k.part("backdrop", f"pipe_{i}", "plate", smooth=40), tuple(d), tuple(foot), [(0, 1, 1), (Ln, 1, 1)], C(1.1, S(8)), caps=(False, True))
        collar(k.part("backdrop", f"band_{i}", k.trim()), Ln * 0.75, 1.35, 0.6, lip=0.12, sides=S(8), axis=tuple(d), origin=tuple(foot))
        glow_orb(k, "backdrop", f"mouth_{i}", tuple(foot + d * Ln * 0.28 + (0, 0, 1.0)), 0.4, sides=4)
    for sgn in (-1, 1):
        tapered_tube(k, "backdrop", f"lyre_{sgn}", k.trim(), [(sgn * 1.4, 17.0, -0.3), (sgn * 3.0, 19.4, -0.3), (sgn * 2.6, 22.4, -0.3), (sgn * 0.8, 23.8, -0.3)], 0.4, 0.3, 4)
    glow_orb(k, "backdrop", "lyre_gem", (0.0, 20.6, 0.0), 0.9)


def mantle_amdusias(k):
    for sgn, nm in ((1, "l"), (-1, "r")):                                                                    # a tailcoat: two long tails
        ribbon(k, "mantle", "tail_" + nm, "cloth", [(sgn * 1.4, -4.0, -2.6), (sgn * 2.4, -10.0, -3.6), (sgn * 2.8, -17.0, -4.4), (sgn * 2.0, -24.0, -4.0)], 4.0, 0.3, 0.3)
        rib(k, "mantle", "edge_" + nm, "crystal", (sgn * 1.4, -4.0, -2.3), (sgn * 2.0, -23.0, -3.7), 0.4, 0.14, layer=2, taper=2.0, up=(0, 0, 1))
    du.sweep(k.part("mantle", "collar", k.trim()), curve([(-3.2, 0.2, -1.8), (0.0, 0.4, -2.8), (3.2, 0.2, -1.8)], 3), ml.rect_ring(1.0, 0.7))


def weapon_amdusias(k):
    """AMDUSIAS_BATON: a conductor's baton-rapier 2.6 blocks long: a cork grip, a tuning-fork guard, a blade that is a spiral horn, a note for the pommel."""
    ml.loft(k.part("weapon", "grip", "extra"), "y", (0, -8, 0), [(0, 0.8, 0.8), (9, 0.9, 0.9)], ml.octagon(2.0, 2.0, 0.5))
    for j in range(3):
        dua.w_ring(k, -7.0 + 2.8 * j, 1.0, f"wrap_{j}", "cloth", 0.6)
    orb(k.part("weapon", "pommel", k.trim(), smooth=40), (0, -9.2, 0), 1.5, sides=8)
    glow_orb(k, "weapon", "note", (0, -10.6, 0), 0.6)
    for dz in (-1.0, 1.0):                                                                                   # the fork: two prongs rising from the hilt
        sweep_tube(k.part("weapon", f"prong_{dz}", k.trim(), smooth=40), [(0.0, 1.0, dz * 0.4), (dz * 0.0 + 3.6, 2.0, dz), (4.8, 6.4, dz), (4.4, 10.4, dz)], 0.45, 0.34, 4)
        sweep_tube(k.part("weapon", f"prong_b_{dz}", k.trim(), smooth=40), [(0.0, 1.0, dz * 0.4), (-3.6, 2.0, dz), (-4.8, 6.4, dz), (-4.4, 10.4, dz)], 0.45, 0.34, 4)
    du.box(k.part("weapon", "yoke", "plate"), (-1.4, 0.4, -1.2), (1.4, 2.4, 1.2), chamfer=0.4)
    helix_horn(k, "weapon", "horn", "bone", (0.0, 2.4, 0.0), (0.0, 1.0, 0.0), 40.0, 1.3, 0.06, turns=6, ridge_r=0.38, steps=22)
    glow_orb(k, "weapon", "tip", (0, 41.6, 0), 0.5, sides=4)
    sweep_flat(k.part("weapon", "ribbon", "cloth", smooth=45), [(0.8, 3.0, 0.5), (2.6, -3.0, 0.7), (1.0, -9.0, 0.3)], 1.8, 0.25, pf_ribbon, (0, 0, 1))


register("AMDUSIAS", head=head_amdusias, chest=chest_amdusias, pauldron=pauldron_amdusias, wings=wings_amdusias, tail=tail_amdusias, halo=halo_amdusias, back=back_amdusias,
         mantle=mantle_amdusias, weapon=weapon_amdusias)


# ============================================================================================ BELIAL: the lord of hollow thrones
def head_belial(k):
    head_base(k, "dome", mat="plate", cheeks=False, nasal=False)
    brow_gem(k, 5.8, 0.6)
    for i, rot in enumerate((0, 60, 120)):                                                                   # a crown that is only its frame: three hoops, hanging empty in the air
        p = k.part("helm", f"hoop_{i}", k.trim(), smooth=40)
        du.tube_direct(p, ring_path((0, 12.4, 0), 3.9, 0, 360, "xy", 10), 0.32, 0.32, 4)
        xform(k, p, rot=("y", rot, (0, 12.4, 0)))
    glow_orb(k, "helm", "hollow_spark", (0.0, 12.4, 0.0), 0.5, sides=4)
    for i in range(4):
        a = 2 * PI * i / 4 + PI / 4
        du.bar(k.part("helm", f"chain_{i}", "plate"), (math.cos(a) * 3.4, 8.2, math.sin(a) * 3.4), (math.cos(a) * 2.4, 11.0, math.sin(a) * 2.4), 0.3, 0.2, up=(0, 0, 1))
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # horns that curl round on themselves into a hoop
        loop = arc_pts((sgn * 6.0, 8.0, -0.8), 3.4, 200, -40, 10) if sgn > 0 else arc_pts((sgn * 6.0, 8.0, -0.8), 3.4, -20, 220, 10)[::-1]
        tapered_tube(k, piece, "loop", "plate", [(sgn * 3.2, 6.2, 0.0)] + [(x, y, z) for (x, y, z) in loop][::2], 0.9, 0.16, 5)
    for sgn in (-1, 1):
        du.bar(k.part("crest", f"hoop_{sgn}", k.trim()), (sgn * 1.8, 5.6, 4.4), (sgn * 1.0, 8.4, 4.3), 0.4, 0.3, up=(0, 0, 1), taper=0.3)
    ring_at(k, "crest", "ring", k.trim(), (0.0, 7.4, 4.6), 1.6, 0.3, "z", 8)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_belial(k):
    dua.chest_cuirass(k)
    ring_at(k, "chest", "hollow", k.trim(), (0.0, -7.2, 3.1), 2.3, 0.4, "z", 10)
    for i in range(4):
        a = i * PI / 2 + PI / 4
        du.bar(k.part("chest", f"bar_{i}", "crystal", layer=2), (math.cos(a) * 2.3, -7.2 + math.sin(a) * 2.3, 3.3), (math.cos(a) * 3.4, -7.2 + math.sin(a) * 3.4, 3.3), 0.3, 0.2, up=(0, 0, 1))


def pauldron_belial(k):
    def fn(k, piece, sgn, cx, bx):
        p = k.part(piece, "hoop", k.trim(), smooth=40)
        du.tube_direct(p, arc_pts((bx - sgn * 0.4, 6.0, 0.0), 3.0, -60, 240, 10), 0.4, 0.4, 4)
        spike(k.part(piece, "horn", "plate"), (bx + sgn * 0.8, 2.8, -1.4), (sgn * 0.8, 0.8, -0.5), 5.2, 0.9, 4)
        glow_orb(k, piece, "spark", (bx - sgn * 0.4, 6.0, 0.0), 0.4, sides=4)
    L.pauldrons(k, fn, 4.6, "extra", 1)


def wings_belial(k, sgn):
    """BELIAL_FRAME: a wing of nothing but its frame: five bone fingers and an outline that closes them, three struts across, a rim of light, shreds of black cloth hanging from it."""
    bone_wing(k, sgn, "extra", 1.35)
    sdp = sd(sgn)
    tips = []
    for j, (a, Ln) in enumerate(((34, 14.0), (10, 21.0), (-12, 24.0), (-36, 20.0), (-62, 14.0))):
        path = arc_path((0, 0, 0), a, Ln, -20, 4, "xy", sgn)
        du.tube_direct(k.part(f"wing_out_{sdp}", f"finger_{j}", "extra", smooth=40), path, 0.55, 0.14, 4)
        tips.append(np.array(path[-1]))
        rib(k, f"wing_out_{sdp}", f"rim_{j}", "crystal", (0, 0, 0.35), tuple(np.array(path[-1]) * 0.92 + (0, 0, 0.35)), 0.28, 0.1, layer=2, taper=1.0)
    outline = [tuple(t) for t in tips]
    du.tube_direct(k.part(f"wing_out_{sdp}", "outline", "extra", smooth=40), outline, 0.4, 0.4, 4)
    for j, f in enumerate((0.35, 0.6, 0.82)):
        rib(k, f"wing_out_{sdp}", f"strut_{j}", "extra", tuple(tips[0] * f), tuple(tips[-1] * f), 0.34, 0.16)
    for j in range(3):
        t = tips[1 + j]
        lf(k, f"wing_out_{sdp}", f"shred_{j}", "cloth", [tuple(t), tuple(t + (sgn * 1.4, -1.0, 0)), tuple(t + (sgn * 0.6, -5.4 - j, -0.2)), tuple(t + (-sgn * 0.8, -2.0, 0))])
    tapered_tube(k, f"wing_in_{sdp}", "inner_a", "extra", [(0, 0.2, 0), (sgn * 5.0, -3.6, 0), (sgn * 10.0, -6.4, 0)], 0.5, 0.2, 4)
    tapered_tube(k, f"wing_in_{sdp}", "inner_b", "extra", [(sgn * 10.0, 0.0, 0), (sgn * 10.0, -6.4, 0)], 0.4, 0.2, 4)
    rib(k, f"wing_in_{sdp}", "inner_rim", "crystal", (0, 0.2, 0.3), (sgn * 10.0, -6.2, 0.3), 0.26, 0.1, layer=2, taper=1.0)


def tail_belial(k):
    p = k.part("tail_seg", "link", k.trim(), smooth=40)
    du.tube_direct(p, ring_path((0, 0, -2.4), 1.3, 0, 360, "yz", 6), 0.38, 0.38, 4)
    du.bar(k.part("tail_seg", "bar", "plate"), (0, 0, 0), (0, 0, -1.2), 0.5, 0.3, up=(1, 0, 0))
    du.tube_direct(k.part("tail_tip", "ring", "plate", smooth=40), ring_path((0, 0, -3.4), 3.2, 0, 360, "xy", 10), 0.6, 0.6, 4)
    for i in range(6):
        a = 2 * PI * i / 6
        spike(k.part("tail_tip", f"spike_{i}", k.trim()), (3.2 * math.cos(a), 3.2 * math.sin(a), -3.4), (math.cos(a), math.sin(a), 0), 2.6, 0.45, 4)
    glow_orb(k, "tail_tip", "spark", (0, 0, -3.4), 0.5, sides=4)


def halo_belial(k):
    for i in range(3):
        a0 = i * 120 + 8
        pts = [(HR * math.cos(rad(a)), 0.0, HR * math.sin(rad(a))) for a in np.linspace(a0, a0 + 92, 6)]
        du.tube_direct(k.part("halo", f"arc_{i}", k.trim(), layer=2, smooth=40), pts, 0.34, 0.34, 4)
        for j, a in enumerate((a0 + 6, a0 + 46, a0 + 86)):
            spike(k.part("halo", f"fang_{i}{j}", "plate"), (HR * math.cos(rad(a)), 0.0, HR * math.sin(rad(a))), (0, -1.0, 0), 2.2 + (j == 1), 0.4, 4)


def back_belial(k):
    """BELIAL_HOLLOW: the throne with nobody in it: two slender pillars and a pointed arch drawn in thin metal, a crown hanging empty in the middle, chains with open shackles."""
    for sgn in (-1, 1):
        tapered_tube(k, "backdrop", f"pillar_{sgn}", "plate", [(sgn * 8.2, -13.0, 0), (sgn * 8.2, 0.0, 0), (sgn * 8.2, 10.0, 0)], 0.8, 0.8, 5)
        tapered_tube(k, "backdrop", f"arch_{sgn}", "plate", [(sgn * 8.2, 10.0, 0), (sgn * 7.0, 14.0, 0), (sgn * 4.6, 17.2, 0), (0.0, 19.4, 0)], 0.8, 0.5, 5)
        tapered_tube(k, "backdrop", f"arch_in_{sgn}", "crystal", [(sgn * 6.8, 9.4, 0.3), (sgn * 5.8, 12.6, 0.3), (sgn * 3.6, 15.0, 0.3), (0.0, 16.4, 0.3)], 0.22, 0.2, 4)
        spike(k.part("backdrop", f"fin_{sgn}", k.trim()), (sgn * 8.2, 10.4, 0), (0, 1.0, 0), 5.0, 0.8, 4)
        orb(k.part("backdrop", f"foot_{sgn}", k.trim(), smooth=0), (sgn * 8.2, -13.4, 0), 1.3, sides=5)
    du.tube_direct(k.part("backdrop", "crown_ring", k.trim(), smooth=40), ring_path((0, 6.0, 0), 4.2, 0, 360, "xy", 10), 0.6, 0.6, 4)
    for i in range(5):
        a = rad(90 + 72 * i)
        spike(k.part("backdrop", f"crown_spire_{i}", k.trim()), (4.2 * math.cos(a), 6.0 + 4.2 * math.sin(a), 0), (math.cos(a), math.sin(a), 0), 3.0, 0.5, 4)
    glow_orb(k, "backdrop", "hollow_spark", (0.0, 6.0, 0.0), 0.7, sides=5)
    for sgn in (-1, 1):
        tapered_tube(k, "backdrop", f"chain_{sgn}", "plate", [(sgn * 2.0, 17.4, 0.0), (sgn * 2.8, 12.0, 0.2), (sgn * 3.0, 4.0, 0.0)], 0.28, 0.28, 4)
        p = k.part("backdrop", f"shackle_{sgn}", k.trim(), smooth=40)
        du.tube_direct(p, arc_pts((sgn * 3.0, 2.4, 0.0), 1.6, -30, 210, 6), 0.35, 0.35, 4)


def mantle_belial(k):
    cape(k, 26.0, 9.0, "cloth", 1.1, k.trim())
    jag_hem(k, "mantle", "rag", "cloth", -26.2, -5.4, 5.8, 6.6, 7, 0.7)


def weapon_belial(k):
    """BELIAL_HOLLOWBLADE: a greatsword 3.2 blocks long whose blade is only an outline: two edges meeting at the tip, three bars across, and in the empty middle the crimson void."""
    ml.loft(k.part("weapon", "grip", "cloth"), "y", (0, -8, 0), [(0, 0.85, 0.85), (10, 0.85, 0.85)], ml.octagon(2.0, 2.0, 0.5))
    for j in range(4):
        dua.w_ring(k, -7.5 + 2.4 * j, 1.05, f"wrap_{j}", "trim", 0.5)
    orb(k.part("weapon", "pommel", k.trim(), smooth=40), (0, -9.4, 0), 1.6, sides=8)
    glow_orb(k, "weapon", "pommel_gem", (0, -10.8, 0), 0.6)
    du.box(k.part("weapon", "guard", k.trim()), (-5.4, 1.4, -0.9), (5.4, 2.8, 0.9), chamfer=0.4)
    for sgn, nm in ((1, "l"), (-1, "r")):
        sweep_flat(k.part("weapon", "horn_" + nm, "plate", smooth=45), [(sgn * 4.6, 2.2, 0), (sgn * 6.4, 4.4, 0), (sgn * 6.2, 8.0, 0), (sgn * 4.4, 10.6, 0)], 1.6, 0.8, pf_taper, (0, 0, 1))
    du.box(k.part("weapon", "ricasso", "plate"), (-2.6, 2.8, -0.6), (2.6, 6.4, 0.6), chamfer=0.3)
    for sgn, nm in ((1, "l"), (-1, "r")):
        edge = [(sgn * 3.0, 6.4, 0.0), (sgn * 3.5, 20.0, 0.0), (sgn * 2.7, 36.0, 0.0), (sgn * 1.2, 46.0, 0.0), (0.0, 54.0, 0.0)]
        tapered_tube(k, "weapon", "edge_" + nm, "plate", edge, 0.8, 0.2, 5)
        rib(k, "weapon", "rim_" + nm, "crystal", (sgn * 2.7, 8.0, 0.7), (sgn * 0.3, 52.0, 0.5), 0.34, 0.14, layer=2, taper=4.0)
    for j, y in enumerate((14.0, 26.0, 38.0)):
        w = 3.5 - 0.35 * j
        du.bar(k.part("weapon", f"cross_{j}", k.trim()), (-w, y, 0.0), (w, y, 0.0), 0.7, 0.5, up=(0, 0, 1), section="rect")
    lf(k, "weapon", "hollow", "crystal", [(-2.6, 6.8, 0.0), (-3.1, 20.0, 0.0), (-2.3, 36.0, 0.0), (0.0, 52.0, 0.0), (2.3, 36.0, 0.0), (3.1, 20.0, 0.0), (2.6, 6.8, 0.0)], layer=2)
    glow_orb(k, "weapon", "heart", (0, 5.0, 0.8), 0.8)
    sweep_flat(k.part("weapon", "tassel", "cloth", smooth=45), [(0.8, -8.5, 0.3), (-2.0, -14.0, 0.6), (0.4, -20.0, 0.2)], 2.0, 0.25, pf_ribbon, (0, 0, 1))


register("BELIAL", head=head_belial, chest=chest_belial, pauldron=pauldron_belial, wings=wings_belial, tail=tail_belial, halo=halo_belial, back=back_belial, mantle=mantle_belial,
         weapon=weapon_belial)

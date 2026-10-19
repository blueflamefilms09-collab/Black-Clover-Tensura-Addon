"""djinn36_set1: Bael, Agares, Valefor, Barbatos, Gusion, Vepar (the first six new Djinn). One builder per piece per Djinn; the registry SETS (djinn36_build)
collects them. Frames and units: djinn_unique.py."""
from djinn36_lib import *                                     # noqa: F401,F403
import djinn36_lib as L
from djinn36_reg import register

HR = dub.HALO_R


def ring_xz(R_, y=0.0, n=12):
    return dub.ring_xz(R_, y, n)


def banner(k, seg, sgn, nm, bx, ang, Ln, w, mat="cloth", notch=0.16, bz=0.0, trim="trim", glow_gem=True):
    """A swallow-tailed standard hung on a wing: a flat cloth polygon with a gold edge and a gem."""
    piece = wing_part(sgn, seg)
    a = rad(ang)
    d = np.array([sgn * math.cos(a), math.sin(a), 0.0])
    n = np.array([-d[1], d[0], 0.0])
    b = np.array([sgn * bx, 0.0, bz])
    pts = [b - n * w / 2, b + d * Ln - n * w / 2, b + d * Ln * (1 - notch), b + d * Ln + n * w / 2, b + n * w / 2]
    lf(k, piece, nm, mat, pts)
    z = np.array([0, 0, 0.12])
    for s2, e in ((-1, "a"), (1, "b")):
        rib(k, piece, f"{nm}_e{e}", trim, b + n * s2 * w / 2 + z, b + d * Ln + n * s2 * w / 2 + z, 0.46, 0.16, taper=0.5)
    if glow_gem:
        gemz(k, piece, nm + "_gem", tuple(b + d * Ln * 0.3 + z), 0.55, 1.0)


# ============================================================================================ BAEL: the king of the first throne
def head_bael(k):
    head_base(k, "circlet", nasal=False)
    brow_gem(k, 6.0, 0.8)
    for i, a in enumerate(np.linspace(-120, 120, 9)):                       # the crown: nine spires on the circlet, the front one tallest
        r = rad(a)
        Lh = 4.6 if i == 4 else 2.6 + (i % 2) * 0.9
        base = (math.sin(r) * 4.5, 6.0, math.cos(r) * 4.5)
        spike(k.part("helm", f"spire_{i}", k.trim()), base, (math.sin(r) * 0.1, 1.0, math.cos(r) * 0.08), Lh, 0.72, 5)
        orb(k.part("helm", f"tip_{i}", "crystal", layer=2, smooth=0), (base[0], 6.0 + Lh + 0.35, base[2]), 0.42, sides=5)
    tapered_tube(k, "helm", "arch_x", k.trim(), [(-4.4, 6.2, 0.0), (-3.0, 9.6, 0.0), (0.0, 11.4, 0.0), (3.0, 9.6, 0.0), (4.4, 6.2, 0.0)], 0.42, 0.42, 5)
    tapered_tube(k, "helm", "arch_z", k.trim(), [(0.0, 6.2, -4.4), (0.0, 9.6, -3.0), (0.0, 11.4, 0.0), (0.0, 9.6, 3.0), (0.0, 6.2, 4.4)], 0.42, 0.42, 5)
    orb(k.part("helm", "orb", "plate", smooth=0), (0.0, 12.0, 0.0), 1.0, sides=6)
    glow_orb(k, "helm", "orb_core", (0.0, 12.0, 0.0), 0.6)
    for sgn, nm in ((1, "l"), (-1, "r")):                                    # the cat's ears of the king
        spike(k.part("ears", "cat_" + nm, "cloth"), (sgn * 2.8, 7.7, -1.4), (sgn * 0.3, 1.0, -0.1), 3.8, 1.5, 4)
        spike(k.part("ears", "cat_in_" + nm, "crystal", layer=2), (sgn * 2.8, 7.9, -1.0), (sgn * 0.3, 1.0, -0.1), 2.5, 0.75, 4)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                       # ceremonial plumes falling from the temples
        for j in range(2):
            blade3(k.part(piece, f"plume_{j}", "cloth", smooth=45), (sgn * 4.4, 5.2 - 0.9 * j, -0.6), (sgn * 0.7, -0.15, -0.7), (sgn * 0.2, -1.0, -0.35), 8.5 - 1.6 * j, 1.8, 0.35, wdir=(0, 0, 1), n=5)
    for i, x in enumerate((-1.8, 0.0, 1.8)):                                 # half crest: a three-spire circlet
        spike(k.part("crest", f"spire_{i}", k.trim()), (x, 5.8, 4.4), (0, 1.0, 0.05), 2.6 if x == 0 else 1.8, 0.55, 5)
    gemz(k, "crest", "jewel", (0, 5.6, BROW_Z), 0.8, 1.2)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_bael(k):
    dua.chest_cuirass(k)
    cx, cy, cz = 0.0, -6.8, 3.15                                              # a crown on the breastplate: a base bar and five spires
    du.bar(k.part("chest", "crown_base", k.trim()), (cx - 2.6, cy, cz), (cx + 2.6, cy, cz), 1.0, 0.3, up=(0, 0, 1), taper=0.3)
    for i, x in enumerate((-2.4, -1.2, 0.0, 1.2, 2.4)):
        spike(k.part("chest", f"crown_{i}", k.trim()), (x, cy, cz), (0, 1.0, 0.1), 2.6 if i == 2 else 1.7 + 0.2 * (i % 2), 0.42, 4)
        glow_orb(k, "chest", f"crown_gem{i}", (x, cy + (3.0 if i == 2 else 2.0), cz + 0.1), 0.3, sides=5)


def belt_bael(k):
    dua.belt_light(k)
    dua.SKIRT["BAEL"] = 18.0
    dua.build_skirt(k, "BAEL")


def pauldron_bael(k):
    def fn(k, piece, sgn, cx, bx):
        for i in range(6):
            a = 2 * PI * i / 6
            spike(k.part(piece, f"spire_{i}", k.trim()), (cx + sgn * 0.5 + math.cos(a) * 2.5, 2.2, math.sin(a) * 2.5), (math.cos(a) * 0.15, 1.0, math.sin(a) * 0.15), 3.2, 0.5, 5)
        glow_orb(k, piece, "orb", (cx + sgn * 0.5, 4.4, 0), 0.8)
        du.sweep(k.part(piece, "fringe", "cloth", uv_mode="loft"), curve([(bx, -0.5, 2.6), (bx + sgn * 0.6, -3.4, 2.9), (bx + sgn * 0.4, -6.4, 2.4)], 4), ml.rect_ring(1.0, 0.25), scales=lambda t: (4.0 * (1 - 0.3 * t), 1.0))
    L.pauldrons(k, fn, 5.0, "plate", 2)


def wings_bael(k, sgn):
    """BAEL_STANDARDS: the wing is a rank of royal standards, swallow-tailed cloth hung from a gilded bone, a gem on each."""
    bone_wing(k, sgn, "trim", 1.2)
    for j, (ang, s, Ln, w) in enumerate(((-50, 0.2, 12.5, 4.0), (-86, 0.7, 11.0, 3.6))):
        banner(k, "in", sgn, f"b{j}", s * INNER, ang, Ln, w, bz=0.1 * j)
    for j, (ang, s, Ln, w) in enumerate(((14, 0.0, 16.0, 4.4), (-12, 0.12, 19.5, 5.0), (-38, 0.28, 19.0, 5.0), (-64, 0.48, 16.0, 4.4), (-88, 0.7, 11.5, 3.6))):
        banner(k, "out", sgn, f"b{j}", s * OUTER, ang, Ln, w, bz=0.1 * j)


def halo_bael(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HR), C(0.5, S(6)))
    for i in range(7):
        a = 2 * PI * i / 7
        spike(k.part("halo", f"spire_{i}", k.trim(), layer=2), (HR * math.cos(a), 0, HR * math.sin(a)), (0, 1.0, 0), 2.8 if i == 0 else 2.0, 0.45, 5)
        orb(k.part("halo", f"pearl_{i}", "crystal", layer=2, smooth=0), (HR * math.cos(a), (2.8 if i == 0 else 2.0) + 0.3, HR * math.sin(a)), 0.38, sides=5)


def back_bael(k):
    """BAEL_THRONE: the high back of a throne stands behind the shoulders: two fluted pillars, a pointed double arch, an upholstered panel, a sun-rune."""
    for sgn in (-1, 1):
        ml.loft(k.part("backdrop", f"pillar_{sgn}", "plate"), "y", (sgn * 8.2, -12.0, 0), [(0, 1.0, 1.0), (22.0, 1.0, 1.0)], ml.octagon(2.2, 1.8, 0.55), caps=(True, True))
        orb(k.part("backdrop", f"foot_{sgn}", k.trim(), smooth=0), (sgn * 8.2, -12.4, 0), 1.7, sides=6)
        arch = [(sgn * 8.2, 10.0, 0), (sgn * 7.4, 13.4, 0), (sgn * 5.4, 16.0, 0), (sgn * 2.7, 17.6, 0), (0.0, 18.2, 0)]
        tapered_tube(k, "backdrop", f"arch_{sgn}", "plate", arch, 0.9, 0.7, 6)
        tapered_tube(k, "backdrop", f"arch_in_{sgn}", k.trim(), [(sgn * 6.6, 9.6, 0.3), (sgn * 5.9, 12.4, 0.3), (sgn * 3.8, 14.6, 0.3), (0.0, 15.8, 0.3)], 0.4, 0.35, 5)
        spike(k.part("backdrop", f"fin_{sgn}", k.trim()), (sgn * 8.2, 10.6, 0), (0, 1.0, 0), 4.2, 0.9, 5)
    extrude(k, "backdrop", "panel", "cloth", [(-7.2, -10.5), (7.2, -10.5), (7.2, 9.0), (3.6, 14.0), (0, 15.2), (-3.6, 14.0), (-7.2, 9.0)], -0.7, 0.2)
    disc(k.part("backdrop", "sun", k.trim()), (0, 3.0, 0.4), 3.4, 0.6, "z", 14)
    disc(k.part("backdrop", "sun_core", "crystal", layer=2), (0, 3.0, 0.8), 2.5, 0.3, "z", 12)
    for i in range(12):
        a = 2 * PI * i / 12
        spike(k.part("backdrop", f"ray_{i}", "crystal", layer=2), (3.9 * math.cos(a), 3.0 + 3.9 * math.sin(a), 0.7), (math.cos(a), math.sin(a), 0), 2.6 if i % 2 == 0 else 1.6, 0.34, 4)
    orb(k.part("backdrop", "crown_orb", k.trim(), smooth=0), (0, 19.2, 0), 1.2, sides=6)
    glow_orb(k, "backdrop", "crown_core", (0, 19.2, 0), 0.7)


def mantle_bael(k):
    cape(k, 24.0, 9.0, "cloth", 1.0, k.trim())
    for sgn in (-1, 1):
        for j in range(3):
            orb(k.part("mantle", f"fur_{sgn}_{j}", "bone", smooth=0), (sgn * (2.6 + 1.3 * j), 0.3, 0.4 - 0.4 * j), 1.5 - 0.15 * j, sides=6)


register("BAEL", head=head_bael, chest=chest_bael, belt=belt_bael, pauldron=pauldron_bael, wings=wings_bael, halo=halo_bael, back=back_bael, mantle=mantle_bael)


# ============================================================================================ AGARES: the rider of crocodile tides
def head_agares(k):
    head_base(k, "cowl", mat="plate", nasal=False)
    du.box(k.part("helm", "snout", "extra"), (-3.7, 5.3, 3.9), (3.7, 7.6, 5.9), chamfer=0.7)                 # the upper jaw lies across the brow
    for i, x in enumerate((-3.0, -1.8, -0.6, 0.6, 1.8, 3.0)):
        spike(k.part("helm", f"tooth_{i}", "bone"), (x, 5.4, 5.3), (0, -1.0, 0.15), 1.7 - 0.2 * abs(i - 2.5) / 2.5, 0.38, 4)
    for sgn in (-1, 1):                                                                                      # the eyes of a crocodile stand on the top of the head
        orb(k.part("helm", f"eye_{sgn}", "extra", smooth=0), (sgn * 2.4, 8.6, 1.4), 1.25, sides=6)
        glow_orb(k, "helm", f"eye_gem_{sgn}", (sgn * 2.4, 9.0, 1.9), 0.5)
        orb(k.part("helm", f"nostril_{sgn}", "extra", smooth=0), (sgn * 1.2, 7.8, 5.4), 0.55, sides=5)
    for i in range(5):                                                                                       # a row of scutes down the crown
        du.stud(k.part("helm", f"scute_{i}", k.trim()), (0, 8.9 - 0.15 * i, 1.0 - 1.6 * i), 0.7 - 0.06 * i, 1.0, (0, 1.0, 0.1), 5)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                       # two ranks of back-swept dorsal spines
        for j in range(4):
            spike(k.part(piece, f"spine_{j}", "bone"), (sgn * (3.2 - 0.2 * j), 7.2 - 0.7 * j, -0.6 - 1.2 * j), (sgn * 0.5, 0.6, -1.0), 4.2 - 0.5 * j, 0.7, 4)
    for sgn, nm in ((1, "l"), (-1, "r")):
        lf(k, "ears", "fin_" + nm, "cloth", [(sgn * 4.2, 2.8, 0.6), (sgn * 6.8, 4.6, -0.2), (sgn * 7.2, 2.2, -1.2), (sgn * 5.0, 0.6, -0.4)])
        rib(k, "ears", "fin_rib_" + nm, "crystal", (sgn * 4.4, 2.8, 0.7), (sgn * 7.0, 3.4, -0.7), layer=2)
    du.box(k.part("crest", "snout", "extra"), (-2.8, 5.4, 4.0), (2.8, 7.0, 5.1), chamfer=0.5)
    for i, x in enumerate((-2.0, -0.7, 0.7, 2.0)):
        spike(k.part("crest", f"tooth_{i}", "bone"), (x, 5.5, 4.8), (0, -1.0, 0.1), 1.2, 0.32, 4)
    gemz(k, "crest", "eye", (0, 7.4, 5.0), 0.5, 1.0)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_agares(k):
    dua.chest_cuirass(k)
    for r_ in range(3):                                                                                        # a hide of scutes: three rows of studs
        for c_ in range(5 - r_ % 2):
            x = (c_ - (4 - r_ % 2) / 2) * 1.5
            du.stud(k.part("chest", f"scute_{r_}{c_}", k.trim()), (x, -6.0 - 0.9 * r_, 3.0), 0.5, 0.5, (0, 0, 1), 5)


def pauldron_agares(k):
    def fn(k, piece, sgn, cx, bx):
        for i, a in enumerate(np.linspace(-70, 70, 6)):                                                      # a jaw clamps the shoulder: teeth ring its rim
            r = rad(a)
            spike(k.part(piece, f"tooth_{i}", "bone"), (cx + sgn * 3.6, -1.0, math.sin(r) * 3.4), (sgn * 0.5, -1.0, 0), 2.6, 0.5, 4)
        orb(k.part(piece, "eye", "extra", smooth=0), (cx + sgn * 0.5, 4.0, 1.4), 1.2, sides=6)
        glow_orb(k, piece, "eye_gem", (cx + sgn * 0.5, 4.4, 1.9), 0.5)
        for j in range(3):
            du.stud(k.part(piece, f"scute_{j}", k.trim()), (cx + sgn * (0.8 + 1.1 * j), 3.4 - 0.5 * j, -1.2), 0.5, 0.8, (0, 1.0, -0.3), 5)
    L.pauldrons(k, fn, 4.8, "extra", 1)


def wings_agares(k, sgn):
    """AGARES_SAIL: a dorsal sail of spines on each side: seven barbed spines, a scalloped membrane between them, a glowing vein down each spine."""
    bone_wing(k, sgn, "extra", 1.2)
    sdp = sd(sgn)
    for seg, spec, B in (("in", ((-6, 0.15, 11.0), (-34, 0.5, 11.0), (-62, 0.85, 9.0)), INNER), ("out", ((24, 0.0, 15.0), (6, 0.12, 20.0), (-12, 0.25, 22.0), (-30, 0.4, 20.0), (-48, 0.55, 16.0), (-66, 0.72, 12.0)), OUTER)):
        tips = []
        for j, (ang, s, Ln) in enumerate(spec):
            a = rad(ang)
            d = np.array([sgn * math.cos(a), math.sin(a), 0.0])
            b = np.array([sgn * s * B, 0.0, 0.0])
            tip = b + d * Ln
            tips.append((b, tip, d))
            rib(k, f"wing_{seg}_{sdp}", f"spine_{j}", "extra", tuple(b), tuple(tip), 0.9, 0.34, taper=2.0)
            rib(k, f"wing_{seg}_{sdp}", f"vein_{j}", "crystal", tuple(b + (0, 0, 0.3)), tuple(b + d * Ln * 0.9 + (0, 0, 0.3)), 0.34, 0.12, layer=2)
            spike(k.part(f"wing_{seg}_{sdp}", f"barb_{j}", "bone"), tuple(b + d * Ln * 0.55), tuple(d + np.array([0, -0.9, 0])), 3.0, 0.4, 4)
        for j in range(len(tips) - 1):                                                                         # the membrane, scalloped between two spines
            (b0, t0, d0), (b1, t1, d1) = tips[j], tips[j + 1]
            mid = (t0 + t1) / 2 - np.array([0, 0.0, 0]) + (b1 - b0) * 0.0
            sag = unit((t0 - b0) + (t1 - b1)) * -2.0
            lf(k, f"wing_{seg}_{sdp}", f"mem_{j}", "cloth", [tuple(b0 + (0, 0, -0.2)), tuple(t0 + (0, 0, -0.2)), tuple(mid + sag + (0, 0, -0.2)), tuple(t1 + (0, 0, -0.2)), tuple(b1 + (0, 0, -0.2))])


def tail_agares(k):
    seg_core(k, 1.6, "extra", "seg", 8, 0.78)
    for sgn in (-1, 1):
        spike(k.part("tail_seg", f"scute_{sgn}", "bone"), (sgn * 0.9, 1.0, -1.0), (sgn * 0.5, 1.0, -0.4), 1.9, 0.5, 4)
    spike(k.part("tail_seg", "ridge", k.trim()), (0, 1.4, -0.6), (0, 1.0, -0.3), 2.4, 0.55, 4)
    ml.loft(k.part("tail_tip", "stem", "extra"), "-z", (0, 0, 0), [(0, 1.3, 1.3), (6.0, 0.7, 0.7), (12.0, 0.1, 0.1)], C(1.0, S(8)), caps=(False, False))
    for i in range(4):
        spike(k.part("tail_tip", f"ridge_{i}", k.trim()), (0, 0.9 - 0.1 * i, -1.0 - 2.4 * i), (0, 1.0, -0.4), 2.0 - 0.3 * i, 0.45, 4)


def back_agares(k):
    """AGARES_MAW: the open jaws of a crocodile ring the shoulders: the upper jaw arches over the head with its teeth turned in, the lower jaw below."""
    R_ = 11.0
    for nm, a0, a1, tdir, mat in (("upper", 12, 168, -1, "extra"), ("lower", 192, 348, 1, "extra")):
        pts = arc_pts((0, -1.0, 0), R_, a0, a1, 14)
        sweep_flat(k.part("backdrop", nm, mat, smooth=40), pts, 3.2, 1.6, lambda s: 0.5 + 0.5 * math.sin(math.pi * min(1, s * 1.05)), (0, 0, 1))
        sweep_flat(k.part("backdrop", nm + "_edge", "crystal", layer=2), [(x, y, z + 0.9) for (x, y, z) in pts], 0.6, 0.3, pf_taper, (0, 0, 1))
        for i, a in enumerate(np.linspace(a0 + 8, a1 - 8, 9)):
            r = rad(a)
            base = (R_ * 0.9 * math.cos(r), -1.0 + R_ * 0.9 * math.sin(r), 0.0)
            spike(k.part("backdrop", f"{nm}_tooth_{i}", "bone"), base, (-math.cos(r), -math.sin(r), 0), 3.4 if i % 2 == 0 else 2.4, 0.7, 4)
    for sgn in (-1, 1):                                                                                      # the hinge of the jaw, and an eye
        orb(k.part("backdrop", f"hinge_{sgn}", k.trim(), smooth=0), (sgn * R_, -1.0, 0), 1.8, sides=6)
        glow_orb(k, "backdrop", f"eye_{sgn}", (sgn * 6.0, 9.4, 1.4), 0.8)
        orb(k.part("backdrop", f"brow_{sgn}", "extra", smooth=0), (sgn * 6.0, 9.4, 0.6), 1.6, sides=6)


register("AGARES", head=head_agares, chest=chest_agares, pauldron=pauldron_agares, wings=wings_agares, tail=tail_agares, back=back_agares)


# ============================================================================================ VALEFOR: the thief of storm-hearts
def head_valefor(k):
    head_base(k, "dome", mat="cloth", cheeks=False, neckguard=False, brim=False)                          # a thief's hood
    ml.lathe(k.part("helm", "peak", "cloth"), "y", (0, 9.4, -1.0), [(0.0, 3.0), (1.6, 1.6), (4.2, 0.0)], sides=S(10), caps=(False, False))
    ribbon(k, "helm", "scarf", "cloth", [(0.0, 1.4, -4.2), (0.2, 0.0, -6.8), (-1.0, -3.0, -8.4), (1.2, -7.0, -9.2), (0.0, -11.0, -9.0)], 3.0, 0.25, 0.4)
    ribbon(k, "helm", "scarf_b", "trim", [(0.0, 1.0, -4.2), (0.6, -1.0, -7.2), (-0.8, -4.0, -8.4), (1.0, -8.0, -9.0)], 1.0, 0.2, 0.2)
    du.bar(k.part("helm", "mask_band", "dark"), (-4.1, 4.9, 3.5), (4.1, 4.9, 3.5), 0.9, 0.25, up=(0, 0, 1), section="rect")
    du.bar(k.part("helm", "mask_band2", "dark"), (-4.1, 4.9, 3.5), (-4.1, 4.9, -3.5), 0.9, 0.25, up=(-1, 0, 0), section="rect")
    gemz(k, "helm", "brow", (0, 6.0, BROW_Z), 0.7, 1.2)
    for sgn, nm in ((1, "l"), (-1, "r")):                                                                    # the long ears of the ass
        blade3(k.part("ears", "ass_" + nm, "cloth", smooth=45), (sgn * 3.4, 7.8, -0.6), (sgn * 0.55, 0.85, -0.1), (sgn * 0.75, 0.6, -0.25), 9.0, 2.2, 0.5, wdir=(0, 0, 1), n=5)
        blade3(k.part("ears", "ass_in_" + nm, "crystal", layer=2, smooth=45), (sgn * 3.6, 8.0, -0.2), (sgn * 0.55, 0.85, -0.1), (sgn * 0.75, 0.6, -0.25), 7.0, 0.9, 0.3, wdir=(0, 0, 1), n=4)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                       # a small bolt at each temple
        zig(k.part(piece, "bolt", "crystal", layer=2), [(sgn * 4.3, 4.6, 1.0), (sgn * 5.6, 6.0, 0.8), (sgn * 4.8, 6.8, 0.8), (sgn * 6.4, 9.0, 0.6)], 0.9, 0.35)
    lf(k, "crest", "mask", "dark", [(-3.2, 5.0, 4.5), (3.2, 5.0, 4.5), (2.0, 6.8, 4.5), (-2.0, 6.8, 4.5)])
    gemz(k, "crest", "eye", (0, 5.9, 4.7), 0.5, 1.0)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_valefor(k):
    dua.chest_harness(k)
    key(k, "chest", "key", "plate", (0.0, -7.6, 3.1), 0.0, L=7.0, bow_r=1.1, w=0.5)                          # a great key worn on the chest, a heart in its bow
    heart(k, "chest", "heart", (0.0, -7.6, 3.5), 0.6)


def pauldron_valefor(k):
    def fn(k, piece, sgn, cx, bx):
        for i in range(3):                                                                                    # lock-picks standing from the shoulder
            spike(k.part(piece, f"pick_{i}", "plate"), (bx + sgn * 0.3 * i, 2.4, -0.8 + 0.8 * i), (sgn * (0.2 + 0.1 * i), 1.0, 0), 6.0 - 1.0 * i, 0.28, 4)
            glow_orb(k, piece, f"pick_tip_{i}", (bx + sgn * (0.3 * i + 0.2 * (6.4 - i)), 8.4 - i, -0.8 + 0.8 * i), 0.22, sides=4)
        heart(k, piece, "heart", (bx, 4.0, 2.4), 0.9)
    L.pauldrons(k, fn, 4.2, "plate", 1)


def wings_valefor(k, sgn):
    """VALEFOR_RAGS: the wing is a storm-torn cloak: long ragged strips with a saw-toothed lower edge, lightning running down three of them."""
    bone_wing(k, sgn, "plate", 1.1)
    rngs = np.random.default_rng(7)
    for seg, spec, B in (("in", ((-20, 0.1, 12.0, 3.4), (-48, 0.5, 12.0, 3.2), (-76, 0.85, 10.0, 3.0)), INNER),
                         ("out", ((18, 0.0, 17.0, 3.6), (-4, 0.1, 21.0, 4.0), (-24, 0.22, 22.0, 4.0), (-44, 0.36, 21.0, 3.8), (-64, 0.52, 17.0, 3.6), (-84, 0.7, 12.0, 3.0)), OUTER)):
        for j, (ang, s, Ln, w) in enumerate(spec):
            a = rad(ang)
            d = np.array([sgn * math.cos(a), math.sin(a), 0.0])
            n = np.array([-d[1], d[0], 0.0])
            b = np.array([sgn * s * B, 0.0, 0.0])
            pts = [b - n * w / 2]
            teeth = 5
            for q in range(teeth + 1):                                                                       # the ragged end
                f = 1.0 - 0.12 * (q % 2) - 0.05 * rngs.random()
                pts.append(b + d * Ln * f - n * w / 2 + n * w * q / teeth)
            pts.append(b + n * w / 2)
            lf(k, wing_part(sgn, seg), f"rag_{j}", "cloth", pts)
            if j % 2 == 0:
                bolt = [tuple(b + d * Ln * f + n * off + (0, 0, 0.2)) for f, off in ((0.05, 0.0), (0.28, 0.7), (0.4, -0.6), (0.62, 0.7), (0.76, -0.5), (0.96, 0.0))]
                zig(k.part(wing_part(sgn, seg), f"bolt_{j}", "crystal", layer=2), bolt, 0.6, 0.2)


def tail_valefor(k):
    ml.lathe(k.part("tail_seg", "link", "plate"), "z", (0, 0, 0.5), [(-0.4, 0.8), (0.0, 1.1), (0.4, 0.8)], sides=S(8), caps=(False, False), squash=1.3)
    du.bar(k.part("tail_seg", "bar", k.trim()), (0, 0, -0.5), (0, 0, -6.0), 0.5, 0.35, up=(0, 1, 0), taper=0.8)
    glow_orb(k, "tail_seg", "bead", (0, 0.5, -3.0), 0.45, sides=5)
    sweep_tube(k.part("tail_tip", "hook", "plate", smooth=40), [(0, 0, 0), (0, 0, -3.0), (0, 1.8, -6.0), (0, 3.6, -5.0), (0, 3.8, -3.2)], 0.7, 0.18, 6)
    heart(k, "tail_tip", "heart", (0, 0.4, -1.2), 1.1)


def halo_valefor(k):
    du.sweep(k.part("halo", "ring", "plate", layer=0, smooth=40), ring_xz(HR), C(0.3, S(5)))
    for i in range(6):
        a = 2 * PI * i / 6
        zig(k.part("halo", f"bolt_{i}", "crystal", layer=2), [(HR * math.cos(a), 0.0, HR * math.sin(a)), (HR * math.cos(a + 0.2), 1.6, HR * math.sin(a + 0.2)), (HR * math.cos(a - 0.1), 2.6, HR * math.sin(a - 0.1)), (HR * math.cos(a + 0.12), 4.4, HR * math.sin(a + 0.12))], 0.55, 0.3, up=(math.cos(a), 0, math.sin(a)))
        heart(k, "halo", f"heart_{i}", (HR * math.cos(a + 0.5), 0.2, HR * math.sin(a + 0.5)), 0.7)


def back_valefor(k):
    """VALEFOR_KEYS: three great keys cross behind the shoulders, a heart of storm in the middle held by chains of lightning."""
    for i, ang in enumerate((-48, 0, 48)):
        key(k, "backdrop", f"key{i}", "plate" if i != 1 else "extra", (math.sin(rad(-ang)) * 2.0, -9.0 + (2.0 if i != 1 else 0), 0), ang, L=24.0 if i == 1 else 21.0, bow_r=2.5 if i == 1 else 2.0)
    heart(k, "backdrop", "heart", (0.0, 2.0, 0.6), 3.0)
    for i in range(6):
        a = 2 * PI * i / 6 + 0.3
        zig(k.part("backdrop", f"chain_{i}", "crystal", layer=2), [(3.6 * math.cos(a), 2.0 + 3.6 * math.sin(a), 0.4), (5.4 * math.cos(a + 0.1), 2.0 + 5.4 * math.sin(a + 0.1), 0.4), (6.4 * math.cos(a - 0.08), 2.0 + 6.4 * math.sin(a - 0.08), 0.4), (8.8 * math.cos(a), 2.0 + 8.8 * math.sin(a), 0.4)], 0.8, 0.3)


register("VALEFOR", head=head_valefor, chest=chest_valefor, pauldron=pauldron_valefor, wings=wings_valefor, tail=tail_valefor, halo=halo_valefor, back=back_valefor)

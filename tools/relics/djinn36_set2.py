"""djinn36_set2: Shax, Buer, Andrealphus."""
from djinn36_lib import *                                     # noqa: F401,F403
import djinn36_lib as L
from djinn36_reg import register

HR = dub.HALO_R


def ring_xz(R_, y=0.0, n=12):
    return dub.ring_xz(R_, y, n)


def ring_xy(R_, z=0.0, n=18, a0=0, a1=360):
    return dub.ring_xy(R_, z, n, a0, a1)


def leaf_poly(base, ang, Ln, W, sgn=1.0, curl=0.0, n=3):
    return dub.leaf_blade(None, base, ang, Ln, W, sgn, curl, n)


# ============================================================================================ SHAX: the silencer of the dove
def dove_flat(k, piece, name, c, span=3.0, mat="plate", plane="xy"):
    """A small dove with open wings seen from the front: a body, two swept wings, a fan tail and a beak (about `span` px wide each side)."""
    cx, cy, cz = c
    orb(k.part(piece, name + "_b", mat, smooth=0), (cx, cy, cz), span * 0.28, sides=5)
    for sgn in (-1, 1):
        lf(k, piece, f"{name}_w{sgn}", mat, [(cx + sgn * 0.2, cy, cz), (cx + sgn * span * 0.8, cy + span * 0.55, cz), (cx + sgn * span, cy + span * 0.2, cz), (cx + sgn * span * 0.6, cy - span * 0.2, cz)])
    spike(k.part(piece, name + "_t", mat, smooth=0), (cx, cy - span * 0.15, cz), (0, -1.0, 0), span * 0.7, span * 0.2, 4)


def head_shax(k):
    head_base(k, "open", nasal=False)
    du.box(k.part("helm", "veil", "bone"), (-3.5, 0.6, 3.9), (3.5, 2.5, 4.6), chamfer=0.4)                  # the silence: a veil sewn over the mouth
    for i, x in enumerate((-2.4, -0.8, 0.8, 2.4)):
        du.bar(k.part("helm", f"stitch_{i}", "crystal", layer=2), (x, 0.8, 4.7), (x, 2.3, 4.7), 0.28, 0.12, up=(0, 0, 1))
    for sgn in (-1, 1):                                                                                      # a dove's wings spread above the brow
        fan(k, "helm", f"dw_{sgn}_", (sgn * 1.2, 6.6, 3.7), (62, 44, 26, 8), (5.6, 6.6, 6.4, 5.0), 1.6, "plate", -16, 4, sgn)
    brow_gem(k, 6.0, 0.7)
    hair_blades(k, (-3.0, -1.5, 0.0, 1.5, 3.0), 8.0, -1.0, 9.0, 2.0, (0, 0.45, -0.9), (0, -0.7, -0.7), mat="cloth", spread=0.6, name="down")
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        for j in range(3):
            blade3(k.part(piece, f"fea_{j}", "plate", smooth=45), (sgn * 4.3, 5.4 - 0.5 * j, -0.2), (sgn * 0.55, 0.4 - 0.3 * j, -0.7), (sgn * 0.2, -0.9, -0.5), 8.0 - 1.4 * j, 1.5, 0.4, wdir=(0, 0, 1), n=4)
    for sgn in (-1, 1):
        fan(k, "crest", f"dw_{sgn}_", (sgn * 0.8, 6.2, 4.2), (60, 38, 16), (3.6, 4.2, 3.4), 1.2, "plate", -12, 3, sgn)
    gemz(k, "crest", "jewel", (0, 5.6, BROW_Z), 0.7, 1.2)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_shax(k):
    dua.chest_harness(k)
    dove_flat(k, "chest", "dove", (0.0, -9.6, 3.2), 3.2)
    glow_orb(k, "chest", "dove_eye", (0.0, -9.5, 3.6), 0.3, sides=4)


def pauldron_shax(k):
    def fn(k, piece, sgn, cx, bx):
        fan(k, piece, "fea", (bx - sgn * 1.0, 2.4, 0.0), (86, 68, 50, 32), (8.0, 8.6, 8.0, 6.6), 2.0, "plate", -14, 4, sgn, bz=0.35)
        orb(k.part(piece, "pearl", "bone", smooth=0), (bx - sgn * 1.0, 3.0, 2.4), 0.8, sides=6)
    L.pauldrons(k, fn, 4.4, "plate", 1)


def wings_shax(k, sgn):
    """SHAX_DOVEWING: a short broad dove's wing: a rounded paddle of seven primaries of nearly one length, rows of rounded coverts, a small alula."""
    bone_wing(k, sgn, "plate", 1.1)
    sdp = sd(sgn)
    for j, (a, s) in enumerate(((-18, 0.2), (-44, 0.5), (-70, 0.8))):
        wf(k, "in", f"cov{j}", sgn, s * INNER, a, 8.0 - 0.6 * j, 3.2, 0.5, -10, "cloth", 3, pf_feather, bz=0.2)
        wf(k, "in", f"pri{j}", sgn, s * INNER, a - 6, 10.0 - 0.6 * j, 2.6, 0.5, -10, "plate", 3, pf_feather)
    for j, (a, s, Ln) in enumerate(((20, 0.0, 13.0), (6, 0.08, 16.0), (-8, 0.17, 17.5), (-22, 0.28, 18.0), (-36, 0.4, 17.0), (-50, 0.52, 15.5), (-66, 0.66, 12.5))):
        wf(k, "out", f"pri{j}", sgn, s * OUTER, a, Ln, 3.0, 0.55, -22, "plate", 4, pf_feather)
        if j in (1, 3, 5):
            wcore(k, "out", f"core{j}", sgn, s * OUTER, a, Ln, 3.0, -22, 3)
    for j in range(5):
        P = leaf_poly((sgn * (0.5 + 0.18 * j) * OUTER, 0, 0.5), -8 - 12 * j, 7.0 - 0.7 * j, 3.6, sgn, -14, 3)
        lf(k, f"wing_out_{sdp}", f"covert_{j}", "cloth", P)
    lf(k, f"wing_out_{sdp}", "alula", "plate", leaf_poly((sgn * 0.2, 0, 0.7), 62, 5.0, 2.2, sgn, 6, 3))


def tail_shax(k):
    ml.loft(k.part("tail_seg", "quill", "trim"), "-z", (0, 0, 0), [(0, 0.5, 0.5), (6, 0.45, 0.45)], C(1.0, S(6)), caps=(False, False))
    for i, a in enumerate((-26, 0, 26)):
        r = rad(a)
        sweep_flat(k.part("tail_seg", f"fea_{i}", "plate", smooth=45), [(0, 0, -0.4), (math.sin(r) * 2.0, 0.1 * i, -3.2), (math.sin(r) * 4.0, 0, -5.8)], 1.8, 0.3, pf_feather, (0, 1, 0))
    for i, a in enumerate((-36, -18, 0, 18, 36)):
        r = rad(a)
        sweep_flat(k.part("tail_tip", f"fan_{i}", "plate" if i % 2 == 0 else "cloth", smooth=45), [(0, 0, 0), (math.sin(r) * 2.4, 0.3, -4.0), (math.sin(r) * 5.2, 0, -9.0 - 2.5 * (1 - abs(a) / 40))], 2.4, 0.3, pf_feather, (0, 1, 0))
    glow_orb(k, "tail_tip", "pearl", (0, 0.4, -1.0), 0.5, sides=5)


def halo_shax(k):
    du.sweep(k.part("halo", "ring", "plate"), ring_xz(HR), C(0.3, S(5)))
    for i in range(5):
        a = 2 * PI * i / 5
        c = np.array([HR * math.cos(a), 0.0, HR * math.sin(a)])
        t = np.array([-math.sin(a), 0.0, math.cos(a)])
        up = np.array([0, 1.0, 0])
        orb(k.part("halo", f"dove_b{i}", "crystal", layer=2, smooth=0), tuple(c), 0.6, sides=5)
        for sg in (-1, 1):
            lf(k, "halo", f"dove_w{i}{sg}", "plate", [tuple(c + t * sg * 0.3), tuple(c + t * sg * 2.0 + up * 1.2), tuple(c + t * sg * 3.2 + up * 0.4), tuple(c + t * sg * 1.6 - up * 0.2)])
        spike(k.part("halo", f"dove_t{i}", "plate", smooth=0), tuple(c), tuple(-c / np.linalg.norm(c) + np.array([0, -0.3, 0])), 1.6, 0.35, 4)


def back_shax(k):
    """SHAX_WREATH: a wreath of olive leaves stands behind the shoulders, three doves in flight on it, a long ribbon trailing from its foot."""
    R_ = 10.5
    du.tube_direct(k.part("backdrop", "wreath", "extra", smooth=40), ring_xy(R_, 0.0, 14, 0, 360), 0.45, 0.45, 4)
    for i in range(16):
        a = 2 * PI * i / 16
        c = np.array([R_ * math.cos(a), R_ * math.sin(a), 0.0])
        d = np.array([math.cos(a), math.sin(a), 0.0])
        t = np.array([-d[1], d[0], 0.0])
        for sg, nm in ((1, "o"), (-1, "i")):
            o = d * sg
            lf(k, "backdrop", f"leaf_{i}{nm}", "cloth" if (i + (sg > 0)) % 2 else "plate", [tuple(c), tuple(c + o * 2.0 + t * 1.2), tuple(c + o * 4.2 + t * 0.7), tuple(c + o * 2.0 - t * 0.6)])
    for i, a in enumerate((90, 210, 330)):
        r = rad(a)
        dove_flat(k, "backdrop", f"dove{i}", (R_ * math.cos(r) * 0.55, R_ * math.sin(r) * 0.55, 0.6), 4.0, "plate")
    ribbon(k, "backdrop", "ribbon", "cloth", [(0.0, -R_, 0.2), (1.6, -R_ - 4.0, 0.4), (-1.4, -R_ - 8.0, 0.2), (1.2, -R_ - 12.0, 0.0)], 3.4, 0.25, 0.4)
    for i in range(3):
        glow_orb(k, "backdrop", f"seed_{i}", (R_ * math.cos(rad(60 + 120 * i)), R_ * math.sin(rad(60 + 120 * i)), 0.3), 0.5, sides=5)


def mantle_shax(k):
    cape(k, 22.0, 8.0, "cloth", 1.0, "plate")
    for sgn in (-1, 1):
        fan(k, "mantle", f"collar{sgn}_", (sgn * 3.6, 0.3, -1.0), (70, 50, 30), (4.4, 4.8, 4.4), 1.4, "plate", -10, 3, sgn, bz=0.3)


register("SHAX", head=head_shax, chest=chest_shax, pauldron=pauldron_shax, wings=wings_shax, tail=tail_shax, halo=halo_shax, back=back_shax, mantle=mantle_shax)


# ============================================================================================ BUER: the healer of the wheel
def head_buer(k):
    head_base(k, "circlet", cheeks=False, nasal=False)
    brow_gem(k, 5.9, 0.7)
    L.wheel(k, "helm", "wheel", (0.0, 10.6, 1.0), 3.4, 5, 0.42, "trim", "extra", 0.9, 90, True, 10)         # a small wheel stands on the crown
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # herb sprigs: a stem and leaf pairs
        stem = [(sgn * 3.4, 6.2, 0.2), (sgn * 4.4, 8.6, -0.4), (sgn * 4.2, 11.0, -1.0), (sgn * 3.0, 13.4, -1.6)]
        tapered_tube(k, piece, "stem", "extra", stem, 0.55, 0.12, 5)
        for j in range(3):
            p = np.array(stem[j + 1])
            for sg in (-1, 1):
                lf(k, piece, f"leaf_{j}{sg}", "cloth", [tuple(p), tuple(p + (sg * 1.0, 0.4, 0.2)), tuple(p + (sg * 2.6, 1.0 + 0.5 * j, 0.2)), tuple(p + (sg * 1.0, 1.8, 0.0))])
    for sgn, nm in ((1, "l"), (-1, "r")):
        lf(k, "ears", "leaf_" + nm, "cloth", leaf_poly((sgn * 4.2, 3.2, 0.0), 12 if sgn > 0 else 12, 6.0, 3.2, sgn, 14, 4))
        rib(k, "ears", "vein_" + nm, "crystal", (sgn * 4.4, 3.3, 0.1), (sgn * 9.4, 5.6, 0.1), layer=2)
    for sgn in (-1, 1):
        ribbon(k, "helm", f"braid_{sgn}", "cloth", [(sgn * 2.4, 6.6, -3.8), (sgn * 3.0, 3.0, -5.2), (sgn * 2.2, -1.0, -5.6), (sgn * 3.0, -5.0, -5.0), (sgn * 2.4, -9.0, -4.6)], 1.8, 0.4, 0.5)
        for j in range(3):
            glow_orb(k, "helm", f"bead_{sgn}{j}", (sgn * (2.8 - 0.3 * j), 2.5 - 3.2 * j, -5.4 + 0.2 * j), 0.4, sides=4)
    L.wheel(k, "crest", "wheel", (0.0, 8.0, 4.2), 2.0, 5, 0.3, "trim", "extra", 0.6, 90, False, 8)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_buer(k):
    dua.chest_harness(k)
    L.wheel(k, "chest", "wheel", (0.0, -9.8, 3.1), 2.5, 5, 0.36, "trim", "extra", 0.7, 90, True, 10)
    plus(k, "chest", "cross", (0.0, -9.8, 3.5), 0.9, 0.4)


def pauldron_buer(k):
    def fn(k, piece, sgn, cx, bx):
        for j in range(4):
            a = 40 + 18 * j
            st = arc_path((bx - sgn * 0.4, 2.8, 0.2 * (j - 1.5)), a, 6.0 - 0.4 * j, 14, 3, "xy", sgn)
            du.tube_direct(k.part(piece, f"herb_{j}", "extra"), st, 0.3, 0.1, 4)
            lf(k, piece, f"herb_leaf_{j}", "cloth", leaf_poly(st[-1], a + 10, 3.4, 2.0, sgn, 8, 3))
        ml.lathe(k.part(piece, "vial", "crystal", layer=2), "y", (bx + sgn * 0.6, 2.6, 2.2), [(0.0, 0.3), (0.6, 0.9), (1.8, 0.9), (2.4, 0.4), (3.0, 0.4)], sides=S(8), caps=(True, True))
    L.pauldrons(k, fn, 4.4, "plate", 1)


def wings_buer(k, sgn):
    """BUER_FRONDS: the wing is fronds of healing herb: each a spine with pairs of small leaves marching to a tip, the longest at the middle."""
    bone_wing(k, sgn, "extra", 1.2)
    sdp = sd(sgn)
    for seg, spec, B in (("in", ((-24, 0.15, 9.0), (-62, 0.6, 8.5)), INNER),
                         ("out", ((24, 0.0, 14.0), (6, 0.1, 18.0), (-12, 0.22, 21.0), (-32, 0.38, 20.0), (-52, 0.55, 16.0), (-74, 0.72, 11.0)), OUTER)):
        for j, (ang, s, Ln) in enumerate(spec):
            sp = arc_path((sgn * s * B, 0, 0), ang, Ln, -16, 4, "xy", sgn)
            du.tube_direct(k.part(f"wing_{seg}_{sdp}", f"spine_{j}", "extra", smooth=40), sp, 0.46, 0.1, 4)
            for q in range(1, 5):
                p = np.array(sp[q])
                d = unit(np.array(sp[min(q + 1, 4)]) - np.array(sp[q - 1]))
                nrm = np.array([-d[1], d[0], 0.0])
                Lh = (3.8 - 0.4 * q) * (1.0 + 0.1 * (j % 2))
                for sg in (-1, 1):
                    lf(k, f"wing_{seg}_{sdp}", f"leaf_{j}{q}{sg}", "cloth", [tuple(p), tuple(p + nrm * sg * 0.9 + d * 1.0), tuple(p + nrm * sg * Lh + d * 1.6), tuple(p + nrm * sg * 0.5 + d * 2.2)])
            if j % 2 == 0:
                glow_orb(k, f"wing_{seg}_{sdp}", f"bud_{j}", tuple(np.array(sp[-1]) + (0, 0.5, 0.3)), 0.42, sides=5)


def halo_buer(k):
    du.sweep(k.part("halo", "ring", "extra"), ring_xz(HR), C(0.3, S(5)))
    for i in range(4):
        a = 2 * PI * i / 4
        c = (HR * math.cos(a), 0.0, HR * math.sin(a))
        du.bar(k.part("halo", f"cross_a{i}", "crystal", layer=2), (c[0], -1.6, c[2]), (c[0], 1.6, c[2]), 0.8, 0.4, up=(math.cos(a), 0, math.sin(a)), section="rect")
        du.bar(k.part("halo", f"cross_b{i}", "crystal", layer=2), (c[0] - math.sin(a) * 1.6, 0.0, c[2] + math.cos(a) * 1.6), (c[0] + math.sin(a) * 1.6, 0.0, c[2] - math.cos(a) * 1.6), 0.8, 0.4, up=(0, 1, 0), section="rect")
        lf(k, "halo", f"leaf_{i}", "cloth", [(c[0] * 1.0, 0, c[2] * 1.0), (c[0] * 1.2, 0.8, c[2] * 1.2 + 1.0), (c[0] * 1.45, 0.4, c[2] * 1.45), (c[0] * 1.2, 0.6, c[2] * 1.2 - 1.0)])


def back_buer(k):
    """BUER_WHEEL: the great wheel of the healer stands behind the shoulders: a double rim, a hub of light, five spokes running out into leaves, five healing crosses on the rim."""
    R_ = 10.5
    L.wheel(k, "backdrop", "wheel", (0.0, 0.0, 0.0), R_, 5, 0.7, "plate", "extra", 2.4, 90, True, 14)
    du.tube_direct(k.part("backdrop", "rim_in", k.trim()), ring_xy(R_ * 0.78, 0.4, 12), 0.34, 0.34, 4)
    glow_orb(k, "backdrop", "hub_gem", (0, 0, 0.9), 1.3)
    for i in range(5):
        a = rad(90) + 2 * PI * i / 5 + PI / 5
        plus(k, "backdrop", f"cross_{i}", (R_ * 0.9 * math.cos(a), R_ * 0.9 * math.sin(a), 0.7), 1.4, 0.6)


def weapon_buer(k):
    """BUER_WHEELSTAFF: a healer's staff 2.9 blocks long: a wrapped shaft with a serpent coiled round it, and a five-spoked wheel of herbs for the head."""
    dua.w_shaft(k, -10, 28, 0.72, "extra", "shaft")
    for j, y in enumerate((-6, -2, 2)):
        dua.w_ring(k, y, 1.05, f"wrap_{j}", "cloth", 1.0)
    orb(k.part("weapon", "butt", k.trim(), smooth=40), (0, -11.2, 0), 1.4, sides=8)
    coil = [(1.3 * math.cos(i * 0.95), 4.0 + 2.3 * i, 1.3 * math.sin(i * 0.95)) for i in range(11)]
    du.tube_direct(k.part("weapon", "serpent", "plate", smooth=40), coil, 0.55, 0.38, 4)
    orb(k.part("weapon", "snake_head", "plate", smooth=0), (coil[-1][0], coil[-1][1] + 0.6, coil[-1][2]), 0.85, sides=5)
    glow_orb(k, "weapon", "snake_eye", (coil[-1][0] + 0.5, coil[-1][1] + 0.9, coil[-1][2] + 0.4), 0.2, sides=4)
    dua.w_ring(k, 27.5, 1.6, "neck", "trim", 1.6)
    L.wheel(k, "weapon", "wheel", (0.0, 36.0, 0.0), 6.4, 5, 0.7, "plate", "extra", 1.8, 90, True, 12)
    glow_orb(k, "weapon", "hub_gem", (0, 36.0, 1.0), 1.1)
    for i in range(5):
        a = rad(90) + 2 * PI * i / 5 + PI / 5
        glow_orb(k, "weapon", f"seed_{i}", (6.4 * math.cos(a), 36.0 + 6.4 * math.sin(a), 0.4), 0.45, sides=5)
    spike(k.part("weapon", "tip", k.trim()), (0, 42.8, 0), (0, 1.0, 0), 4.0, 0.7, 5)
    sweep_flat(k.part("weapon", "tassel", "cloth", smooth=45), [(0.8, 26.0, 0.4), (2.4, 21.0, 0.6), (1.4, 15.0, 0.3)], 1.8, 0.25, pf_ribbon, (0, 0, 1))


register("BUER", head=head_buer, chest=chest_buer, pauldron=pauldron_buer, wings=wings_buer, halo=halo_buer, back=back_buer, weapon=weapon_buer)


# ============================================================================================ ANDREALPHUS: the peacock of the stars
def peacock_eye(k, piece, name, c, r=1.0):
    cx, cy, cz = c
    extrude(k, piece, name + "_o", "trim", ngon(cx, cy, r, 6), cz - 0.2, cz + 0.2)
    orb(k.part(piece, name + "_i", "crystal", layer=2, smooth=0), (cx, cy, cz + 0.3), r * 0.6, sides=4)


def head_andrealphus(k):
    head_base(k, "circlet", nasal=False)
    brow_gem(k, 5.9, 0.7)
    for i, (x, h) in enumerate(((-1.8, 7.0), (0.0, 9.4), (1.8, 7.0))):                                      # the aigrette: three stalks, a tuft at the end of each
        tapered_tube(k, "helm", f"stalk_{i}", k.trim(), [(x, 6.4, 3.8), (x * 1.4, 6.4 + h * 0.5, 3.4), (x * 1.8, 6.4 + h, 2.8)], 0.3, 0.18, 4)
        orb(k.part("helm", f"tuft_{i}", "plate", smooth=0), (x * 1.8, 6.6 + h, 2.8), 0.9, sides=5)
        glow_orb(k, "helm", f"tuft_gem_{i}", (x * 1.8, 6.8 + h, 3.2), 0.5, sides=5)
    fan(k, "helm", "crown", (0.0, 8.0, -1.0), (150, 130, 110, 90, 70, 50, 30), (7.0, 8.0, 8.6, 9.0, 8.6, 8.0, 7.0), 1.6, "plate", -26, 4, 1.0, bz=0.1)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # swept plumes from the temples, each ending in an eye
        pl = [(sgn * 4.3, 5.6, -0.2), (sgn * 6.4, 5.0, -2.6), (sgn * 7.6, 2.4, -5.2), (sgn * 6.4, -1.4, -6.8)]
        sweep_flat(k.part(piece, "plume", "plate", smooth=45), curve(pl, 4), 2.6, 0.5, pf_feather, (0, 0, 1))
        peacock_eye(k, piece, "eye", (sgn * 6.4, -1.4, -6.6), 1.0)
    tapered_tube(k, "crest", "stalk", k.trim(), [(0, 6.2, 4.2), (0, 8.6, 4.0), (0, 10.6, 3.4)], 0.28, 0.16, 4)
    orb(k.part("crest", "tuft", "plate", smooth=0), (0, 10.8, 3.4), 0.8, sides=5)
    glow_orb(k, "crest", "tuft_gem", (0, 11.0, 3.8), 0.45, sides=5)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_andrealphus(k):
    dua.chest_cuirass(k)
    peacock_eye(k, "chest", "eye", (0.0, -7.4, 3.0), 2.2)
    for i in range(8):
        a = 2 * PI * i / 8
        spike(k.part("chest", f"ray_{i}", "crystal", layer=2), (2.6 * math.cos(a), -7.4 + 2.6 * math.sin(a) * 1.0, 3.0), (math.cos(a), math.sin(a), 0.1), 1.6, 0.26, 4)


def pauldron_andrealphus(k):
    def fn(k, piece, sgn, cx, bx):
        fan(k, piece, "fea", (bx - sgn * 1.0, 2.6, 0.0), (90, 72, 54, 36), (8.0, 8.8, 8.0, 6.6), 2.0, "plate", -18, 4, sgn, bz=0.3)
        for j, (ang, Ln) in enumerate(((90, 8.0), (54, 8.0))):
            r = rad(ang)
            peacock_eye(k, piece, f"eye{j}", (bx - sgn * 1.0 + sgn * math.cos(r) * Ln * 0.9, 2.6 + math.sin(r) * Ln * 0.9, 0.3 * j + 0.4), 0.7)
    L.pauldrons(k, fn, 4.4, "plate", 1)


def wings_andrealphus(k, sgn):
    """ANDREALPHUS_TRAIN: the wing is a peacock's train: long plumes hanging and sweeping back, each ending in a ringed eye of light."""
    bone_wing(k, sgn, "extra", 1.1)
    sdp = sd(sgn)
    for j, (a, s, Ln) in enumerate(((-30, 0.2, 10.0), (-62, 0.7, 9.0))):
        wf(k, "in", f"pl{j}", sgn, s * INNER, a, Ln, 2.8, 0.5, -14, "plate", 4, pf_feather)
        glow_orb(k, f"wing_in_{sdp}", f"eye{j}", (sgn * (s * INNER + math.cos(rad(a - 8)) * Ln * 0.95), math.sin(rad(a - 8)) * Ln * 0.95, 0.3), 0.7, sides=5)
    for j, (a, s, Ln) in enumerate(((20, 0.0, 16.0), (-2, 0.12, 22.0), (-24, 0.28, 25.0), (-46, 0.46, 22.0), (-70, 0.68, 17.0))):
        wf(k, "out", f"pl{j}", sgn, s * OUTER, a, Ln, 3.0, 0.55, -20, "plate", 4, pf_feather)
        wcore(k, "out", f"core{j}", sgn, s * OUTER, a, Ln, 3.0, -20, 3)
        tipa = rad(a - 14)
        peacock_eye(k, f"wing_out_{sdp}", f"eye{j}", (sgn * (s * OUTER + math.cos(tipa) * Ln * 0.9), math.sin(tipa) * Ln * 0.9, 0.3), 1.2 - 0.05 * j)


def tail_andrealphus(k):
    ml.loft(k.part("tail_seg", "quill", "trim"), "-z", (0, 0, 0), [(0, 0.6, 0.6), (6, 0.5, 0.5)], C(1.0, S(6)), caps=(False, False))
    for sg in (-1, 1):
        sweep_flat(k.part("tail_seg", f"fea_{sg}", "plate", smooth=45), [(0, 0, -0.4), (sg * 1.6, 0.0, -3.0), (sg * 2.6, 0.0, -6.0)], 1.8, 0.3, pf_feather, (0, 1, 0))
    for i, a in enumerate((-44, -22, 0, 22, 44)):
        r = rad(a)
        sweep_flat(k.part("tail_tip", f"fan_{i}", "plate", smooth=45), [(0, 0, 0), (math.sin(r) * 2.8, 0.2, -4.4), (math.sin(r) * 6.0, 0, -10.0 - 2.0 * (1 - abs(a) / 44))], 2.6, 0.3, pf_feather, (0, 1, 0))
        orb(k.part("tail_tip", f"eye_{i}", "crystal", layer=2, smooth=0), (math.sin(r) * 6.0, 0.5, -10.0 - 2.0 * (1 - abs(a) / 44)), 0.7, sides=5)


def halo_andrealphus(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HR), C(0.34, S(5)))
    for i in range(8):
        a = 2 * PI * i / 8
        shard(k, "halo", f"star_{i}", "crystal", (HR * math.cos(a), 0.0, HR * math.sin(a)), (math.cos(a) * 0.5, 1.0, math.sin(a) * 0.5), 2.2 + (i % 2) * 1.6, 0.9, 0.4, layer=2)
        glow_orb(k, "halo", f"dot_{i}", (HR * 0.8 * math.cos(a + 0.4), 0.0, HR * 0.8 * math.sin(a + 0.4)), 0.3, sides=4)


def back_andrealphus(k):
    """ANDREALPHUS_FAN: the peacock fan: eleven long plumes spread over the shoulders in a half circle, each ringed with an eye, a spray of stars between."""
    base = (0.0, -7.0, -0.2)
    for i, a in enumerate(np.linspace(10, 170, 11)):
        r = rad(a)
        Ln = 20.0 + 2.5 * math.sin(r)
        d = (math.cos(r), math.sin(r), 0.0)
        end = (base[0] + d[0] * Ln, base[1] + d[1] * Ln, 0.0)
        du.bar(k.part("backdrop", f"quill_{i}", k.trim()), base, end, 0.36, 0.2, up=(0, 0, 1), taper=1.0)
        sweep_flat(k.part("backdrop", f"vane_{i}", "plate" if i % 2 == 0 else "cloth", smooth=45), [(base[0] + d[0] * 3.0, base[1] + d[1] * 3.0, 0.1), (base[0] + d[0] * Ln * 0.6, base[1] + d[1] * Ln * 0.6, 0.1), end], 2.6, 0.3, lambda s: 0.5 + 0.5 * math.sin(math.pi * min(1, s * 1.05)), (0, 0, 1))
        peacock_eye(k, "backdrop", f"eye_{i}", (end[0], end[1], 0.2), 1.3)
    for i in range(6):
        a = rad(20 + 28 * i)
        glow_orb(k, "backdrop", f"star_{i}", (math.cos(a) * 12.0, -7.0 + math.sin(a) * 12.0, 0.5), 0.35, sides=4)


register("ANDREALPHUS", head=head_andrealphus, chest=chest_andrealphus, pauldron=pauldron_andrealphus, wings=wings_andrealphus, tail=tail_andrealphus, halo=halo_andrealphus,
         back=back_andrealphus)

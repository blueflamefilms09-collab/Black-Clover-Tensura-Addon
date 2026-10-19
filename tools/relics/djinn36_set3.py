"""djinn36_set3: Decarabia, Seere, Dantalion."""
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


def star_outline(k, piece, name, c, R_, mat, tube=0.5, layer=0, skip=2):
    """A pentagram: five bars joining every second point of a pentagon."""
    cx, cy, cz = c
    P = [(cx + R_ * math.cos(rad(90 + 72 * i)), cy + R_ * math.sin(rad(90 + 72 * i)), cz) for i in range(5)]
    for i in range(5):
        a, b = P[i], P[(i + skip) % 5]
        du.bar(k.part(piece, f"{name}_{i}", mat, layer=layer), a, b, tube * 2.0, tube * 0.8, up=(0, 0, 1), taper=0.0, section="rect")
    return P


def claw(k, piece, name, base, ang, Ln, w, sgn=1.0, mat="plate", teeth=True, open_deg=34.0, bz=0.0):
    """A crab's pincer in the xy plane: a fixed finger and a moving one opening about the base, curling in towards each other, teeth on their inner edges."""
    for s2, nm, ln in ((1, "a", Ln), (-1, "b", Ln * 0.8)):
        a = ang + s2 * open_deg / 2
        pts = arc_path((base[0], base[1], base[2] + bz), a, ln, -s2 * 26.0, 5, "xy", sgn)
        sweep_flat(k.part(piece, f"{name}_{nm}", mat, smooth=45), curve(pts, 4), w, 1.0, lambda s: (0.55 + 0.45 * math.sin(math.pi * min(1.0, s * 1.2))) * (1 - s ** 3) + 0.05, (0, 0, 1))
        if teeth:
            for i in range(3):
                p = np.array(pts[1 + i])
                d = unit(np.array(pts[min(i + 2, 5)]) - np.array(pts[i]))
                nrm = np.array([d[1], -d[0], 0.0])
                if nrm[1] * s2 > 0:
                    nrm = -nrm
                spike(k.part(piece, f"{name}_{nm}_t{i}", "bone"), tuple(p), tuple(nrm * 0.9 + d * 0.2), 1.5, 0.32, 4)


# ============================================================================================ DECARABIA: the star crab of the deep
def head_decarabia(k):
    head_base(k, "dome", mat="plate", cheeks=False, nasal=False)
    for i in range(5):                                                                                       # ridge of the carapace
        du.stud(k.part("helm", f"scute_{i}", k.trim()), (0, 9.4 - 0.2 * i, 2.8 - 1.7 * i), 0.8 - 0.07 * i, 0.9, (0, 1.0, 0.1), 5)
    extrude(k, "helm", "star", "trim", star_poly(0, 6.6, 1.9, 0.8, 5), 4.4, 4.9)                              # the star on the brow
    glow_orb(k, "helm", "star_gem", (0.0, 6.6, 5.0), 0.45, sides=4)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # eyes on stalks
        stalk = [(sgn * 2.0, 8.2, 1.8), (sgn * 3.4, 10.6, 2.4), (sgn * 4.2, 12.6, 3.0)]
        tapered_tube(k, piece, "stalk", "bone", stalk, 0.5, 0.36, 5)
        orb(k.part(piece, "eye", "bone", smooth=0), (sgn * 4.3, 13.0, 3.1), 1.0, sides=6)
        glow_orb(k, piece, "pupil", (sgn * 4.4, 13.2, 3.8), 0.5, sides=5)
        tapered_tube(k, piece, "antenna", "trim", [(sgn * 1.0, 8.0, 3.4), (sgn * 1.6, 10.0, 6.0), (sgn * 1.2, 12.4, 8.4)], 0.22, 0.1, 4)
    for sgn, nm in ((1, "l"), (-1, "r")):                                                                    # small pincers at the temples
        claw(k, "ears", "pincer_" + nm, (sgn * 4.2, 2.8, 0.6), 0, 4.4, 1.6, sgn, "plate", True, 56.0)
    hair_blades(k, (-2.0, 0.0, 2.0), 8.0, -2.4, 7.0, 2.0, (0, 0.3, -1.0), (0, -0.9, -0.5), mat="cloth", spread=0.3, name="whisker")
    extrude(k, "crest", "star", "trim", star_poly(0, 6.4, 1.5, 0.65, 5), 4.4, 4.8)
    glow_orb(k, "crest", "gem", (0.0, 6.4, 4.9), 0.4, sides=4)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_decarabia(k):
    dua.chest_cuirass(k)
    extrude(k, "chest", "star", "trim", star_poly(0, -7.0, 3.0, 1.3, 5), 3.1, 3.55)
    glow_orb(k, "chest", "star_gem", (0.0, -7.0, 3.7), 0.6, sides=5)
    for i in range(2):
        du.bar(k.part("chest", f"ridge_{i}", "bone"), (-3.6, -4.2 - 5.6 * i, 3.0), (3.6, -4.2 - 5.6 * i, 3.0), 0.5, 0.3, up=(0, 0, 1), taper=0.8)


def pauldron_decarabia(k):
    def fn(k, piece, sgn, cx, bx):
        claw(k, piece, "claw", (bx - sgn * 0.5, 2.6, 0.2), 70, 7.0, 2.6, sgn, "plate", True, 56.0)
        for i in range(3):
            du.stud(k.part(piece, f"scute_{i}", k.trim()), (bx - sgn * (0.4 + 0.8 * i), 3.0 + 0.2 * i, -1.6), 0.5, 0.7, (0, 1.0, -0.2), 5)
        glow_orb(k, piece, "pivot", (bx - sgn * 0.5, 3.4, 1.6), 0.5, sides=5)
    L.pauldrons(k, fn, 4.8, "plate", 1)


def wings_decarabia(k, sgn):
    """DECARABIA_SHELLS: the wing is a fan of carapace plates laid over each other like the shell of a crab, edged in gold, four jointed crab legs reaching out from under it."""
    bone_wing(k, sgn, "extra", 1.3)
    sdp = sd(sgn)
    for seg, spec, B in (("in", ((-16, 0.15, 9.0, 6.0), (-52, 0.6, 8.0, 5.4)), INNER),
                         ("out", ((24, 0.0, 13.0, 8.0), (4, 0.08, 18.0, 9.6), (-18, 0.18, 21.0, 10.0), (-40, 0.32, 19.0, 9.0), (-62, 0.5, 15.0, 7.6)), OUTER)):
        for j, (ang, s, Ln, W) in enumerate(spec):
            P = leaf_poly((sgn * s * B, 0, 0.15 * j), ang, Ln, W, sgn, -10, 3)
            lf(k, f"wing_{seg}_{sdp}", f"plate_{j}", "plate" if j % 2 == 0 else "extra", P)
            a = rad(ang)
            rib(k, f"wing_{seg}_{sdp}", f"rib_{j}", "trim", (sgn * s * B, 0, 0.3 + 0.15 * j), (sgn * (s * B + math.cos(a) * Ln * 0.95), math.sin(a) * Ln * 0.95, 0.3 + 0.15 * j), 0.5, 0.18, taper=1.5)
            if j % 2 == 0:
                glow_orb(k, f"wing_{seg}_{sdp}", f"gem_{j}", (sgn * (s * B + math.cos(a) * Ln * 0.5), math.sin(a) * Ln * 0.5, 0.5 + 0.15 * j), 0.4, sides=4)
    for j, a in enumerate((-70, -90, -112, -134)):                                                           # the legs
        leg = [(sgn * 3.0 * j, -3.0, 0.0), (sgn * (3.0 * j + 4.0 * math.cos(rad(a + 40))), -3.0 + 4.0 * math.sin(rad(a + 40)), 0.0), (sgn * (3.0 * j + 7.0 * math.cos(rad(a + 10))), -3.0 + 7.0 * math.sin(rad(a + 10)) , 0.0), (sgn * (3.0 * j + 9.0 * math.cos(rad(a))), -3.0 + 9.0 * math.sin(rad(a)) - 3.0, 0.0)]
        du.tube_direct(k.part(f"wing_out_{sdp}", f"leg_{j}", "bone", smooth=40), leg, 0.5, 0.15, 4)


def tail_decarabia(k):
    ml.loft(k.part("tail_seg", "plate", "plate", smooth=40), "-z", (0, 0, 0), [(0, 1.7, 1.2), (3.0, 1.8, 1.3), (6.0, 1.5, 1.1)], C(1.0, S(8)), caps=(False, False))
    for sgn in (-1, 1):
        spike(k.part("tail_seg", f"scute_{sgn}", "bone"), (sgn * 1.2, 0.4, -1.0), (sgn * 1.0, 0.2, -0.4), 1.8, 0.45, 4)
    extrude(k, "tail_tip", "fan", "plate", star_poly(0, 0, 4.0, 1.7, 5, 90), -3.4, -2.8)
    glow_orb(k, "tail_tip", "gem", (0, 0, -2.5), 0.6, sides=4)


def halo_decarabia(k):
    du.sweep(k.part("halo", "ring", k.trim()), ring_xz(HR), C(0.3, S(5)))
    for i in range(5):
        a = 2 * PI * i / 5
        n0 = len(k.m.parts)
        extrude(k, "halo", f"star_{i}", "trim", star_poly(0, 0, 2.2, 0.95, 5, 90), -0.25, 0.25, layer=2)
        L.transform_since(k, n0, rot=("x", 90))
        L.transform_since(k, n0, shift=(HR * math.cos(a), 0.0, HR * math.sin(a)))


def back_decarabia(k):
    """DECARABIA_PENTAGRAM: a great pentagram in a ring stands behind the shoulders, a gem at each point, two crab legs reaching down from the lower points."""
    R_ = 12.5
    du.tube_direct(k.part("backdrop", "ring", "plate", smooth=40), ring_xy(R_, 0.0, 14), 0.8, 0.8, 5)
    P = star_outline(k, "backdrop", "bar", (0, 0, 0.0), R_ * 0.97, "trim", 0.55)
    star_outline(k, "backdrop", "glow", (0, 0, 0.6), R_ * 0.97, "crystal", 0.2, layer=2)
    for i, p in enumerate(P):
        orb(k.part("backdrop", f"point_{i}", "bone", smooth=0), (p[0], p[1], 0.0), 1.2, sides=5)
        glow_orb(k, "backdrop", f"point_gem_{i}", (p[0], p[1], 0.9), 0.55, sides=4)
    extrude(k, "backdrop", "core", "plate", ngon(0, 0, 3.0, 5, rad(90)), -0.4, 0.5)
    glow_orb(k, "backdrop", "core_gem", (0, 0, 0.9), 1.2)
    for sgn in (-1, 1):
        p = P[2] if sgn > 0 else P[3]
        leg = [(p[0], p[1], 0), (p[0] + sgn * 2.4, p[1] - 3.2, 0), (p[0] + sgn * 4.4, p[1] - 4.0, 0), (p[0] + sgn * 6.4, p[1] - 8.0, 0)]
        du.tube_direct(k.part("backdrop", f"leg_{sgn}", "bone", smooth=40), leg, 0.7, 0.18, 4)


register("DECARABIA", head=head_decarabia, chest=chest_decarabia, pauldron=pauldron_decarabia, wings=wings_decarabia, tail=tail_decarabia, halo=halo_decarabia, back=back_decarabia)


# ============================================================================================ SEERE: the swift wind prince
def head_seere(k):
    head_base(k, "circlet", cheeks=False, nasal=False)
    brow_gem(k, 5.9, 0.65)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # small wings at the temples
        fan(k, piece, f"tw{sgn}_", (sgn * 4.3, 4.6, 0.0), (28, 10, -8, -26), (7.0, 8.4, 8.0, 6.0), 1.4, "plate", -12, 4, sgn, bz=0.2)
    hair_blades(k, (-2.8, -1.4, 0.0, 1.4, 2.8), 8.0, -1.2, 13.0, 1.6, (0, 0.4, -0.9), (0, -0.2, -1.0), mat="cloth", spread=0.4, name="wind")
    for sgn in (-1, 1):
        fan(k, "crest", f"tw{sgn}_", (sgn * 0.9, 6.0, 4.2), (50, 28, 8), (3.4, 4.0, 3.4), 1.1, "plate", -10, 3, sgn)
    gemz(k, "crest", "jewel", (0, 5.6, BROW_Z), 0.6, 1.2)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))
    for sgn, nm in ((1, "l"), (-1, "r")):
        blade3(k.part("ears", "ear_" + nm, "cloth", smooth=45), (sgn * 4.2, 3.4, 0.0), (sgn * 0.9, 0.35, -0.2), (sgn * 0.8, 0.5, -0.3), 4.6, 1.3, 0.4, wdir=(0, 0, 1), n=3)


def chest_seere(k):
    dua.chest_harness(k)
    sp = [(0.55 * t * math.cos(t * 1.5), -9.8 + 0.55 * t * math.sin(t * 1.5), 3.2) for t in np.linspace(0.4, 5.0, 9)]
    du.tube_direct(k.part("chest", "swirl", "trim", smooth=40), sp, 0.34, 0.2, 4)
    glow_orb(k, "chest", "eye", (0.0, -9.8, 3.5), 0.5, sides=4)


def pauldron_seere(k):
    def fn(k, piece, sgn, cx, bx):
        fan(k, piece, "fea", (bx - sgn * 0.8, 2.6, 0.0), (80, 56, 34), (7.4, 8.2, 7.0), 1.5, "plate", -20, 4, sgn, bz=0.3)
        du.tube_direct(k.part(piece, "gust", "trim"), arc_pts((bx, 5.2, 1.8), 1.8, 0, 280, 7), 0.22, 0.22, 4)
    L.pauldrons(k, fn, 4.3, "plate", 1)


def wings_seere(k, sgn):
    """SEERE_SWIFT: the narrow scythe-wing of a swift: seven long thin primaries swept far back in a near-parallel sheaf, a glowing shaft in each, short coverts at the root."""
    bone_wing(k, sgn, "plate", 1.0)
    for j, (a, s, Ln) in enumerate(((-22, 0.2, 9.0), (-48, 0.55, 9.0), (-74, 0.85, 8.0))):
        wf(k, "in", f"cov{j}", sgn, s * INNER, a, Ln, 2.0, 0.4, -16, "plate", 3, pf_blade)
    for j, (a, s, Ln) in enumerate(((16, 0.0, 17.0), (2, 0.06, 23.0), (-10, 0.12, 27.0), (-22, 0.2, 29.0), (-34, 0.3, 27.0), (-46, 0.42, 23.0), (-58, 0.55, 17.0))):
        wf(k, "out", f"pri{j}", sgn, s * OUTER, a, Ln, 2.1, 0.45, -9, "plate", 5, pf_blade)
        wcore(k, "out", f"core{j}", sgn, s * OUTER, a, Ln, 2.1, -9, 4)


def tail_seere(k):
    for i, off in enumerate((-0.7, 0.7)):
        sweep_flat(k.part("tail_seg", f"strm_{i}", "cloth", smooth=45), [(off, 0.3, 0), (off * 1.8, 0.8, -3.0), (off * 1.2, 0.2, -6.0)], 1.4, 0.25, pf_ribbon, (1, 0, 0))
    seg_core(k, 0.4, "trim", "cord", 5, 0.9)
    for i, off in enumerate((-1.2, 0.0, 1.2)):
        sweep_flat(k.part("tail_tip", f"strm_{i}", "plate" if i != 1 else "cloth", smooth=45), [(off * 0.3, 0, 0), (off * 1.6, 0.6 * (i % 2), -5.0), (off * 2.6, -0.4, -11.0 - 2.0 * (i == 1))], 1.8, 0.25, pf_ribbon, (1, 0, 0))
    glow_orb(k, "tail_tip", "pearl", (0, 0.3, -0.8), 0.45, sides=4)


def halo_seere(k):
    for i, (R_, y, a0, a1) in enumerate(((HR, 0.0, 0, 250), (HR * 0.8, 0.6, 120, 380), (HR * 0.6, 1.2, 240, 500))):
        pts = [(R_ * math.cos(rad(a)), y, R_ * math.sin(rad(a))) for a in np.linspace(a0, a1, 10)]
        du.tube_direct(k.part("halo", f"gust_{i}", "plate", layer=2, smooth=40), pts, 0.34, 0.1, 4)


def back_seere(k):
    """SEERE_PINWHEEL: a pinwheel of six curved blades of wind stands behind the shoulders round an eye of calm, each blade a long sweeping arc."""
    disc(k.part("backdrop", "eye", k.trim()), (0, 0, 0), 2.6, 1.0, "z", 8)
    glow_orb(k, "backdrop", "eye_gem", (0, 0, 0.8), 1.2)
    for i in range(6):
        a0 = 60 * i
        pts = [(3.2 * math.cos(rad(a0 + t * 70)) * (1 + t * 2.2), 3.2 * math.sin(rad(a0 + t * 70)) * (1 + t * 2.2), 0.0) for t in np.linspace(0, 1, 7)]
        sweep_flat(k.part("backdrop", f"blade_{i}", "plate", smooth=45), curve(pts, 3), 3.0, 0.7, pf_blade, (0, 0, 1))
        sweep_flat(k.part("backdrop", f"edge_{i}", "crystal", layer=2), [(x, y, 0.45) for (x, y, z) in curve(pts, 3)], 0.5, 0.25, pf_taper, (0, 0, 1))
    du.tube_direct(k.part("backdrop", "rim", "trim", layer=2, smooth=40), ring_xy(10.5, 0.0, 14, 0, 360), 0.22, 0.22, 4)


def mantle_seere(k):
    for sgn in (-1, 1):
        ribbon(k, "mantle", f"scarf_{sgn}", "cloth", [(sgn * 3.0, 0.4, 1.0), (sgn * 5.0, -1.0, -1.4), (sgn * 8.0, -1.6, -4.4), (sgn * 12.0, -1.2, -8.0)], 3.0, 0.25, 0.5)


def weapon_seere(k):
    """SEERE_SABRE: a swift sabre 2.8 blocks long: a curved single-edged blade of 36 px, a guard of three swept feathers, a ring pommel with a ribbon streaming from it."""
    ml.loft(k.part("weapon", "grip", "cloth"), "y", (0, -8, 0), [(0, 0.8, 0.8), (9, 0.8, 0.8)], ml.octagon(2.0, 2.0, 0.5))
    for j in range(3):
        dua.w_ring(k, -7.0 + 2.6 * j, 1.0, f"wrap_{j}", "trim", 0.5)
    ring_at(k, "weapon", "pommel", k.trim(), (0, -10.4, 0), 1.9, 0.4, "z", 8)
    for sgn in (-1, 1):
        fan(k, "weapon", f"wing{sgn}_", (sgn * 1.4, 2.0, 0.0), (36, 14, -8), (6.4, 7.4, 6.4), 1.4, "plate", -20, 3, sgn, bz=0.2)
    du.box(k.part("weapon", "guard", k.trim()), (-2.0, 1.2, -0.7), (2.0, 2.6, 0.7), chamfer=0.3)
    path = [(0, 3.0, 0), (0.4, 12.0, 0), (1.6, 22.0, 0), (4.2, 31.0, 0), (8.0, 38.0, 0)]
    sweep_flat(k.part("weapon", "blade", "plate", smooth=45), curve(path, 5), 3.4, 0.9, lambda s: (1.0 - 0.3 * s) * (1 - s ** 4) + 0.04, (0, 0, 1))
    sweep_flat(k.part("weapon", "edge", "crystal", layer=2), curve([(x + 1.2, y, 0.5) for (x, y, z) in path], 5), 0.6, 0.3, pf_taper, (0, 0, 1))
    du.bar(k.part("weapon", "fuller", k.trim()), (0.0, 5.0, 0.5), (1.4, 28.0, 0.5), 0.6, 0.15, up=(0, 0, 1), taper=3.0)
    ribbon(k, "weapon", "ribbon", "cloth", [(0.0, -12.0, 0.2), (-2.4, -17.0, 0.4), (0.4, -23.0, 0.2), (-1.8, -29.0, 0.0)], 2.0, 0.25, 0.4)


register("SEERE", head=head_seere, chest=chest_seere, pauldron=pauldron_seere, wings=wings_seere, tail=tail_seere, halo=halo_seere, back=back_seere, mantle=mantle_seere, weapon=weapon_seere)


# ============================================================================================ DANTALION: the many-faced scholar
def page_poly(w, h, fold=1.4):
    return [(-w / 2, -h / 2), (w / 2, -h / 2), (w / 2, h / 2 - fold), (w / 2 - fold, h / 2), (-w / 2, h / 2)]


def head_dantalion(k):
    head_base(k, "circlet", cheeks=False, nasal=False)
    brow_gem(k, 5.8, 0.65)
    du.box(k.part("helm", "board", "plate"), (-5.2, 8.6, -5.2), (5.2, 9.4, 5.2), chamfer=0.4)                # the scholar's flat cap
    du.box(k.part("helm", "board_trim", k.trim()), (-5.4, 8.5, -5.4), (5.4, 8.8, 5.4))
    ml.lathe(k.part("helm", "crown", "cloth"), "y", (0, 6.0, 0), [(0.0, 4.9), (1.6, 4.9), (2.6, 4.2)], sides=S(12), caps=(False, False))
    ribbon(k, "helm", "tassel", "cloth", [(5.0, 9.2, 5.0), (6.4, 7.0, 5.6), (6.0, 3.6, 5.0), (6.6, 0.6, 4.6)], 1.2, 0.3, 0.5)
    glow_orb(k, "helm", "tassel_bead", (6.6, 0.2, 4.6), 0.5, sides=4)
    L.mask(k, "helm", "back_face", (0.0, 4.4, -4.9), 1.4, 180.0)
    for sgn, nm in ((1, "l"), (-1, "r")):
        L.mask(k, "ears", "face_" + nm, (sgn * 4.9, 4.0, 0.0), 1.2, sgn * 90.0)
    hair_blades(k, (-2.4, 0.0, 2.4), 8.4, -2.0, 11.0, 2.0, (0, 0.2, -1.0), (0, -1.0, -0.4), mat="cloth", spread=0.3, name="loose")
    L.mask(k, "crest", "face", (0.0, 6.4, 4.5), 1.0)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_dantalion(k):
    dua.chest_harness(k)
    for sgn in (-1, 1):                                                                                      # an open book on the breast: two pages, a spine of light
        lf(k, "chest", f"page_{sgn}", "bone", [(sgn * 0.2, -7.6, 3.1), (sgn * 3.2, -7.2, 3.1), (sgn * 3.4, -11.4, 3.1), (sgn * 0.2, -11.8, 3.1)])
        for i in range(3):
            du.bar(k.part("chest", f"line_{sgn}{i}", "crystal", layer=2), (sgn * 0.6, -8.4 - 1.0 * i, 3.25), (sgn * 2.8, -8.2 - 1.0 * i, 3.25), 0.18, 0.1, up=(0, 0, 1))
    du.bar(k.part("chest", "spine", k.trim()), (0.0, -7.4, 3.2), (0.0, -12.0, 3.2), 0.5, 0.3, up=(0, 0, 1))


def book(k, piece, name, c, w=3.0, h=4.0, t=1.2, mat="plate", rot_z=0.0, rot_y=0.0):
    n0 = len(k.m.parts)
    du.box(k.part(piece, name + "_cover", mat), (-w / 2, -h / 2, -t / 2), (w / 2, h / 2, t / 2), chamfer=0.2)
    du.box(k.part(piece, name + "_pages", "bone"), (-w / 2 + 0.2, -h / 2 + 0.2, -t / 2 + 0.15), (w / 2 + 0.15, h / 2 - 0.2, t / 2 - 0.15))
    du.bar(k.part(piece, name + "_spine", k.trim()), (-w / 2, -h / 2, 0.0), (-w / 2, h / 2, 0.0), 0.4, 0.5, up=(-1, 0, 0))
    gemz(k, piece, name + "_gem", (0.0, 0.0, t / 2 + 0.1), 0.35, 1.0)
    if rot_z:
        L.transform_since(k, n0, rot=("z", rot_z))
    if rot_y:
        L.transform_since(k, n0, rot=("y", rot_y))
    L.transform_since(k, n0, shift=c)


def pauldron_dantalion(k):
    def fn(k, piece, sgn, cx, bx):
        book(k, piece, "tome_a", (bx - sgn * 0.4, 3.6, 0.0), 4.4, 3.0, 1.6, "plate", 0.0)
        book(k, piece, "tome_b", (bx - sgn * 0.2, 5.2, 0.0), 3.8, 2.6, 1.4, "extra", 14.0 * sgn)
        sweep_tube(k.part(piece, "quill", "bone", smooth=40), [(bx + sgn * 1.4, 4.0, 1.4), (bx + sgn * 2.4, 7.0, 1.8), (bx + sgn * 2.8, 10.0, 1.6)], 0.2, 0.1, 4)
        lf(k, piece, "quill_vane", "plate", [(bx + sgn * 2.4, 7.0, 1.8), (bx + sgn * 4.4, 9.2, 1.8), (bx + sgn * 3.0, 11.4, 1.6), (bx + sgn * 2.6, 9.0, 1.7)])
    L.pauldrons(k, fn, 4.4, "plate", 1)


def wings_dantalion(k, sgn):
    """DANTALION_PAGES: the wing is a fan of loose pages, sheets of parchment with a folded corner, lines of glowing script on some, a bone for a binding."""
    bone_wing(k, sgn, "extra", 1.2)
    sdp = sd(sgn)
    for seg, spec, B in (("in", ((-14, 0.15, 10.0, 5.0), (-48, 0.55, 9.0, 4.6), (-80, 0.85, 7.0, 4.0)), INNER),
                         ("out", ((22, 0.0, 14.0, 7.0), (2, 0.1, 19.0, 8.0), (-18, 0.2, 22.0, 8.4), (-38, 0.34, 20.0, 8.0), (-58, 0.5, 16.0, 7.0), (-80, 0.7, 11.0, 5.4)), OUTER)):
        for j, (ang, s, Ln, w) in enumerate(spec):
            a = rad(ang)
            d = np.array([sgn * math.cos(a), math.sin(a), 0.0])
            n = np.array([-d[1], d[0], 0.0])
            b = np.array([sgn * s * B, 0.0, 0.12 * j])
            cpos = b + d * Ln / 2
            pg = page_poly(Ln, w)
            # rotate the page's own frame (x along the page length) into the wing plane
            ca, sa = math.cos(math.atan2(d[1], d[0])), math.sin(math.atan2(d[1], d[0]))
            poly = [(cpos[0] + x * ca - y * sa, cpos[1] + x * sa + y * ca) for (x, y) in pg]
            extrude(k, f"wing_{seg}_{sdp}", f"page_{j}", "bone", poly, b[2] - 0.12, b[2] + 0.12, smooth=0)
            if j % 2 == 0:
                for q in range(3):
                    off = (q - 1) * w * 0.22
                    du.bar(k.part(f"wing_{seg}_{sdp}", f"line_{j}{q}", "crystal", layer=2), tuple(b + d * Ln * 0.2 + n * off + (0, 0, 0.25)), tuple(b + d * Ln * 0.8 + n * off + (0, 0, 0.25)), 0.2, 0.1, up=(0, 0, 1), taper=0.5)


def halo_dantalion(k):
    du.sweep(k.part("halo", "ring", k.trim()), ring_xz(HR), C(0.3, S(5)))
    for i in range(6):
        a = 2 * PI * i / 6
        L.mask(k, "halo", f"face_{i}", (HR * math.cos(a), 1.6, HR * math.sin(a)), 1.0, -math.degrees(a) + 90.0)


def back_dantalion(k):
    """DANTALION_TOMES: five tomes ride in a ring behind the shoulders, each lit by a sigil of its own, a great rune of light behind them."""
    R_ = 10.0
    du.tube_direct(k.part("backdrop", "orbit", k.trim(), layer=2, smooth=40), ring_xy(R_, 0.0, 14), 0.24, 0.24, 4)
    du.tube_direct(k.part("backdrop", "orbit_in", "plate", smooth=40), ring_xy(R_ * 0.6, 0.0, 12), 0.34, 0.34, 4)
    for i in range(6):
        a = rad(90 + 60 * i)
        du.bar(k.part("backdrop", f"ray_{i}", "crystal", layer=2), (R_ * 0.6 * math.cos(a), R_ * 0.6 * math.sin(a), 0.0), (R_ * math.cos(a), R_ * math.sin(a), 0.0), 0.28, 0.14, up=(0, 0, 1))
    for i in range(5):
        a = rad(90 + 72 * i)
        book(k, "backdrop", f"tome_{i}", (R_ * math.cos(a), R_ * math.sin(a), 0.0), 4.6, 6.0, 1.8, "plate" if i % 2 == 0 else "extra", math.degrees(a) - 90.0)
    glow_orb(k, "backdrop", "heart", (0, 0, 0.5), 1.3)


def mantle_dantalion(k):
    cape(k, 24.0, 9.0, "cloth", 1.0, k.trim())
    for sgn in (-1, 1):
        ribbon(k, "mantle", f"stole_{sgn}", "cloth", [(sgn * 2.0, 0.2, 2.4), (sgn * 2.4, -6.0, 3.0), (sgn * 2.2, -12.0, 2.6), (sgn * 2.8, -18.0, 2.2)], 2.2, 0.3, 0.2)


def weapon_dantalion(k):
    """DANTALION_TOME: a lectern-staff 2.8 blocks long: a wrapped shaft ending in a great chained tome with a glowing sigil, loose pages whirling from it."""
    dua.w_shaft(k, -10, 28, 0.72, "extra", "shaft")
    for j, y in enumerate((-6, -2, 2, 6)):
        dua.w_ring(k, y, 1.05, f"wrap_{j}", "cloth", 0.9)
    orb(k.part("weapon", "butt", k.trim(), smooth=40), (0, -11.2, 0), 1.5, sides=8)
    dua.w_ring(k, 27.0, 1.6, "mount", "trim", 1.8)
    book(k, "weapon", "tome", (0.0, 35.0, 0.0), 10.0, 14.0, 3.2, "plate", 0.0)
    for sgn in (-1, 1):
        for q in (-1, 1):
            spike(k.part("weapon", f"corner_{sgn}{q}", k.trim()), (sgn * 5.0, 35.0 + q * 7.0, 0.0), (sgn, q, 0.0), 1.8, 0.6, 4)
    du.bar(k.part("weapon", "clasp", k.trim()), (-5.4, 35.0, 1.6), (5.4, 35.0, 1.6), 0.9, 0.35, up=(0, 0, 1))
    L.mask(k, "weapon", "sigil", (0.0, 35.0, 2.0), 1.4)
    for i in range(4):                                                                                       # lines of script glowing on the cover, a ring of light round the face
        du.bar(k.part("weapon", f"script_{i}", "crystal", layer=2), (-3.6, 30.0 + 1.2 * i, 1.8), (3.6, 30.0 + 1.2 * i, 1.8), 0.4, 0.2, up=(0, 0, 1))
        du.bar(k.part("weapon", f"script_b_{i}", "crystal", layer=2), (-3.6, 39.6 + 1.2 * i - 1.2, 1.8), (3.6, 39.6 + 1.2 * i - 1.2, 1.8), 0.4, 0.2, up=(0, 0, 1))
    du.tube_direct(k.part("weapon", "face_ring", "crystal", layer=2, smooth=40), ring_path((0.0, 35.0, 1.9), 2.6, 0, 360, "xy", 10), 0.25, 0.25, 4)
    for i, a in enumerate((20, 70, 120, 200)):
        r = rad(a)
        lf(k, "weapon", f"page_{i}", "bone", [(7.4 * math.cos(r), 35.0 + 8.0 * math.sin(r), 0.4 * i), (9.6 * math.cos(r) + 1.4, 35.0 + 9.6 * math.sin(r), 0.4 * i), (10.6 * math.cos(r), 35.0 + 11.0 * math.sin(r) + 1.6, 0.4 * i), (8.4 * math.cos(r) - 1.0, 35.0 + 9.6 * math.sin(r) + 1.0, 0.4 * i)])
    sweep_tube(k.part("weapon", "chain", "plate", smooth=40), [(0.0, 27.5, 1.2), (3.0, 29.0, 2.0), (4.6, 31.0, 2.0)], 0.3, 0.3, 4)


register("DANTALION", head=head_dantalion, chest=chest_dantalion, pauldron=pauldron_dantalion, wings=wings_dantalion, halo=halo_dantalion, back=back_dantalion,
         mantle=mantle_dantalion, weapon=weapon_dantalion)

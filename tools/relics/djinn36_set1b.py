"""djinn36_set1b: Barbatos, Gusion, Vepar."""
from djinn36_lib import *                                     # noqa: F401,F403
import djinn36_lib as L
from djinn36_reg import register

HR = dub.HALO_R


def ring_xz(R_, y=0.0, n=12):
    return dub.ring_xz(R_, y, n)


def leaf_poly(base, ang, Ln, W, sgn=1.0, curl=0.0, n=5):
    return dub.leaf_blade(None, base, ang, Ln, W, sgn, curl, n)


# ============================================================================================ BARBATOS: the hunter of the wild green
def head_barbatos(k):
    head_base(k, "cowl", mat="cloth", cheeks=False, nasal=False)
    brow_gem(k, 6.0, 0.7)
    for i, a in enumerate(np.linspace(-115, 115, 9)):                                                       # a crown of oak leaves round the brow
        r = rad(a)
        b = np.array([math.sin(r) * 4.5, 6.2, math.cos(r) * 4.5])
        t = np.array([math.cos(r), 0.0, -math.sin(r)])
        o = np.array([math.sin(r), 0.0, math.cos(r)])
        up = np.array([0, 1.0, 0])
        lf(k, "helm", f"leaf_{i}", "cloth", [tuple(b), tuple(b + t * 1.0 + up * 1.5 + o * 0.2), tuple(b + up * (3.6 + (i % 2)) + o * 0.5), tuple(b - t * 1.0 + up * 1.5 + o * 0.2)])
    hair_blades(k, (-2.2, 0.0, 2.2), 7.8, -2.6, 11.0, 2.2, (0, 0.4, -0.9), (0, -0.9, -0.5), mat="cloth", spread=0.4, name="moss")
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # the great antlers: a beam of three points and two brow tines
        beam = [(sgn * 2.8, 6.6, 0.0), (sgn * 4.0, 9.4, -0.6), (sgn * 4.6, 12.6, -1.4), (sgn * 3.8, 15.8, -2.4), (sgn * 2.4, 18.2, -3.2)]
        tapered_tube(k, piece, "beam", "bone", beam, 0.95, 0.12, 6)
        for j, (i0, d) in enumerate(((1, (2.2, 2.4, 0.6)), (2, (2.6, 2.2, -0.2)), (3, (2.0, 2.4, -0.6)))):
            p0 = beam[i0]
            tapered_tube(k, piece, f"tine_{j}", "bone", [p0, (p0[0] + sgn * d[0] * 0.5, p0[1] + d[1] * 0.5, p0[2] + d[2] * 0.5), (p0[0] + sgn * d[0], p0[1] + d[1], p0[2] + d[2])], 0.6, 0.08, 5)
        tapered_tube(k, piece, "brow", "bone", [(sgn * 3.0, 6.8, 1.2), (sgn * 3.6, 8.4, 2.6), (sgn * 3.4, 10.0, 3.4)], 0.55, 0.08, 5)
        collar(k.part(piece, "ring", "trim"), 0.0, 1.35, 0.7, lip=0.2, sides=S(10), axis=(sgn * 1.2, 2.8, -0.6), origin=(sgn * 2.8, 6.6, 0))
    pointed_ears(k, 5.0, 16.0, 1.5, mat="cloth")
    for sgn in (-1, 1):
        lf(k, "crest", f"leaf_{sgn}", "cloth", [(sgn * 1.6, 5.8, 4.4), (sgn * 3.0, 7.4, 4.3), (sgn * 1.4, 8.6, 4.4), (sgn * 0.6, 6.6, 4.4)])
        tapered_tube(k, "crest", f"tine_{sgn}", "bone", [(sgn * 2.8, 6.0, 4.2), (sgn * 3.6, 8.0, 3.8), (sgn * 3.8, 9.6, 3.4)], 0.4, 0.08, 5)
    gemz(k, "crest", "jewel", (0, 5.6, BROW_Z), 0.7, 1.2)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_barbatos(k):
    dua.chest_harness(k)
    lf(k, "chest", "oak", "cloth", leaf_poly((0.0, -9.6, 3.2), 90, 5.2, 3.2))
    rib(k, "chest", "oak_rib", "trim", (0.0, -9.5, 3.3), (0.0, -14.4, 3.3), 0.3, 0.12)
    orb(k.part("chest", "acorn", "bone", smooth=0), (0.0, -9.4, 3.5), 0.8, sides=6)


def pauldron_barbatos(k):
    def fn(k, piece, sgn, cx, bx):
        for j in range(3):
            lf(k, piece, f"leaf_{j}", "cloth", leaf_poly((bx - sgn * 0.6, 2.8 + 0.2 * j, 0.5 * j - 0.5), 60 + 14 * j if sgn > 0 else 60 + 14 * j, 6.4 - 0.8 * j, 2.8, sgn, 18))
        tapered_tube(k, piece, "tine", "bone", [(bx, 3.0, -1.4), (bx + sgn * 0.6, 5.6, -1.6), (bx + sgn * 0.2, 8.4, -2.0)], 0.6, 0.08, 5)
        orb(k.part(piece, "acorn", "bone", smooth=0), (bx + sgn * 0.6, 3.6, 1.8), 0.8, sides=6)
    L.pauldrons(k, fn, 4.6, "extra", 1)


def wings_barbatos(k, sgn):
    """BARBATOS_BOUGHS: each wing is a bough: a bark bone with forked branches, a few oak leaves at their ends, an acorn or two."""
    bone_wing(k, sgn, "extra", 1.35)
    sdp = sd(sgn)
    for seg, spec, B in (("in", ((-22, 0.15, 9.5, 0.4), (-60, 0.6, 9.0, 0.4)), INNER),
                         ("out", ((26, 0.0, 14.0, 0.5), (8, 0.1, 19.0, 0.5), (-12, 0.22, 22.0, 0.55), (-32, 0.38, 20.0, 0.5), (-52, 0.55, 16.0, 0.5), (-74, 0.72, 11.0, 0.4)), OUTER)):
        for j, (ang, s, Ln, curl_k) in enumerate(spec):
            curl = -18 * (1 if ang > -20 else 0.6)
            main = arc_path((sgn * s * B, 0, 0), ang, Ln, curl, 4, "xy", sgn)
            du.tube_direct(k.part(f"wing_{seg}_{sdp}", f"bough_{j}", "extra", smooth=40), main, 0.62, 0.12, 4)
            for q, (f, da, fl) in enumerate(((0.5, 34, 0.5),)):
                i0 = int(len(main) * f) - 1
                p0 = main[i0]
                sub = arc_path(p0, ang + da, Ln * fl, curl * 0.5, 2, "xy", sgn)
                du.tube_direct(k.part(f"wing_{seg}_{sdp}", f"twig_{j}{q}", "extra", smooth=40), sub, 0.34, 0.08, 4)
                lf(k, f"wing_{seg}_{sdp}", f"leaf_{j}{q}", "cloth", leaf_poly(sub[-1], ang + da, 4.6, 2.4, sgn, 10, 3))
            lf(k, f"wing_{seg}_{sdp}", f"tipleaf_{j}", "cloth", leaf_poly(main[-1], ang + curl * 0.6, 5.6, 3.0, sgn, 8, 3))
            if j % 2 == 0:
                glow_orb(k, f"wing_{seg}_{sdp}", f"berry_{j}", tuple(np.array(main[int(len(main) * 0.6)]) + (0, 0.9, 0.3)), 0.45, sides=5)


def tail_barbatos(k):
    seg_core(k, 1.1, "cloth", "seg", 8, 0.9)
    for i in range(3):
        blade3(k.part("tail_seg", f"brush_{i}", "cloth", smooth=45), (0, 0, -0.6), (math.cos(i * 2.1) * 0.5, math.sin(i * 2.1) * 0.5, -0.9), (math.cos(i * 2.1) * 0.9, math.sin(i * 2.1) * 0.9, -0.7), 5.4, 2.0, 0.4, wdir=(0, 0, 1), n=3)
    for i in range(5):
        a = 2 * PI * i / 5
        blade3(k.part("tail_tip", f"tuft_{i}", "cloth", smooth=45), (0, 0, 0), (math.cos(a) * 0.4, math.sin(a) * 0.4, -0.9), (math.cos(a) * 0.15, math.sin(a) * 0.15, -1.0), 9.0 - (i % 2) * 2.5, 2.4, 0.4, wdir=(0, 0, 1), n=4)
    glow_orb(k, "tail_tip", "seed", (0, 0, -1.4), 0.7)


def halo_barbatos(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HR), C(0.38, S(5)))
    for i in range(8):
        a = 2 * PI * i / 8
        spike(k.part("halo", f"head_{i}", "plate"), (HR * math.cos(a), 0, HR * math.sin(a)), (math.cos(a), 0.0, math.sin(a)), 3.0, 0.6, 4)
        spike(k.part("halo", f"fletch_{i}", "cloth"), (HR * math.cos(a), 0, HR * math.sin(a)), (-math.cos(a), 0.0, -math.sin(a)), 1.8, 0.5, 4)
        glow_orb(k, "halo", f"gem_{i}", (HR * math.cos(a) * 1.0, 0.0, HR * math.sin(a)), 0.34, sides=5)


def back_barbatos(k):
    """BARBATOS_HUNT: a great horn-bow arches over the shoulders, its string a line of light, and seven arrows fan out of a quiver at the small of the back."""
    C0 = (0.0, -5.0, -0.6)
    pts = arc_pts(C0, 15.0, 22, 158, 14)
    sweep_flat(k.part("backdrop", "bow", "extra", smooth=40), pts, 2.2, 1.6, lambda s: 0.55 + 0.45 * math.sin(math.pi * s), (0, 0, 1))
    sweep_flat(k.part("backdrop", "bow_inlay", "trim"), [(x, y, z + 0.7) for (x, y, z) in pts], 0.6, 0.3, pf_taper, (0, 0, 1))
    for sgn in (-1, 1):
        spike(k.part("backdrop", f"nock_{sgn}", "bone"), (sgn * 13.9, 0.6 + 5.0 - 5.0 + 0.0, -0.6), (sgn * 0.3, 1.0, 0), 3.0, 0.7, 4)
    du.bar(k.part("backdrop", "string", "crystal", layer=2), (-13.9, 0.6, -0.4), (13.9, 0.6, -0.4), 0.28, 0.18, up=(0, 0, 1))
    ml.lathe(k.part("backdrop", "quiver", "extra"), "y", (3.0, -12.0, -0.2), [(0.0, 1.8), (5.0, 2.2), (9.0, 2.5)], sides=S(10), caps=(True, False))
    collar(k.part("backdrop", "quiver_band", k.trim()), -7.0, 2.35, 0.8, lip=0.2, sides=S(10), origin=(3.0, 0, -0.2))
    for i, a in enumerate(np.linspace(62, 118, 7)):
        r = rad(a)
        base = np.array([3.0 + 0.4 * (i - 3), -3.4, 0.4 * ((i % 2) - 0.5)])
        d = np.array([math.cos(r), math.sin(r), 0.0])
        tip = base + d * (20.0 - 1.4 * abs(i - 3))
        rib(k, "backdrop", f"shaft_{i}", "bone", tuple(base), tuple(tip), 0.36, 0.2, taper=0.2, up=(0, 0, 1))
        spike(k.part("backdrop", f"head_{i}", "plate"), tuple(tip), tuple(d), 3.0, 0.55, 4)
        glow_orb(k, "backdrop", f"tip_{i}", tuple(tip + d * 3.3), 0.3, sides=4)
        spike(k.part("backdrop", f"fletch_{i}", "cloth"), tuple(base), tuple(-d), 2.4, 0.55, 4)


def mantle_barbatos(k):
    cape(k, 15.0, 8.0, "cloth", 1.0, k.trim())
    jag_hem(k, "mantle", "leaf", "cloth", -15.2, -4.5, 5.0, 3.4, 6)


register("BARBATOS", head=head_barbatos, chest=chest_barbatos, pauldron=pauldron_barbatos, wings=wings_barbatos, tail=tail_barbatos, halo=halo_barbatos, back=back_barbatos,
         mantle=mantle_barbatos)


# ============================================================================================ GUSION: the seer of the tide mirror
def head_gusion(k):
    head_base(k, "circlet", cheeks=False, nasal=False)
    for i, x in enumerate((-3.2, -1.6, 0.0, 1.6, 3.2)):                                                     # a crown of five standing panes of glass
        Lh = 7.0 - 1.1 * abs(i - 2)
        shard(k, "helm", f"pane_{i}", "plate", (x, 6.0, 3.0 - 0.4 * abs(i - 2)), (x * 0.08, 1.0, -0.12), Lh, 1.5, 0.5, glow=True)
    ml.lathe(k.part("helm", "mirror_rim", "trim"), "z", (0, 6.3, 4.35), [(0.0, 1.35), (0.3, 1.55), (0.6, 1.35), (0.8, 0.0)], sides=S(12), caps=(True, False), squash=1.5)
    ml.lathe(k.part("helm", "mirror", "crystal", layer=2), "z", (0, 6.3, 4.9), [(0.0, 1.1), (0.15, 1.0), (0.2, 0.0)], sides=S(12), caps=(True, False), squash=1.5)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # a breaking wave curls over each temple
        pth = arc_pts((sgn * 5.2, 5.4, 0.0), 2.6, 200 if sgn > 0 else -20, 20 if sgn > 0 else -200, 10)
        if sgn < 0:
            pth = arc_pts((sgn * 5.2, 5.4, 0.0), 2.6, 160, 340, 10)[::-1]
        sweep_flat(k.part(piece, "wave", "cloth", smooth=45), pth, 2.4, 0.6, lambda s: 0.45 + 0.55 * math.sin(math.pi * min(1.0, s * 1.1)), (0, 0, 1))
        sweep_flat(k.part(piece, "wave_edge", "crystal", layer=2), [(x, y, z + 0.3) for (x, y, z) in pth], 0.5, 0.25, pf_taper, (0, 0, 1))
    for sgn, nm in ((1, "l"), (-1, "r")):
        lf(k, "ears", "fin_" + nm, "cloth", [(sgn * 4.2, 2.6, 0.4), (sgn * 7.4, 4.4, -0.4), (sgn * 7.0, 1.2, -1.2), (sgn * 4.8, 0.4, -0.2)])
    hair_blades(k, (-2.4, 0.0, 2.4), 7.6, -2.8, 15.0, 2.2, (0, 0.2, -1.0), (0, -1.0, -0.4), mat="cloth", spread=0.3, name="tide")
    shard(k, "crest", "pane", "plate", (0, 5.8, 4.2), (0, 1.0, -0.1), 4.4, 1.3, 0.5, glow=True)
    gemz(k, "crest", "jewel", (0, 5.6, BROW_Z + 0.2), 0.6, 1.2)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_gusion(k):
    dua.chest_harness(k)
    ml.lathe(k.part("chest", "mirror_rim", "trim"), "z", (0, -9.4, 2.7), [(0.0, 2.0), (0.4, 2.25), (0.8, 2.0), (1.1, 0.0)], sides=S(14), caps=(True, False), squash=1.35)
    ml.lathe(k.part("chest", "mirror", "crystal", layer=2), "z", (0, -9.4, 3.5), [(0.0, 1.6), (0.15, 1.5), (0.2, 0.0)], sides=S(14), caps=(True, False), squash=1.35)


def pauldron_gusion(k):
    def fn(k, piece, sgn, cx, bx):
        for j, (dx, Lh) in enumerate(((0.0, 6.0), (1.8, 4.6), (-1.6, 4.0))):
            shard(k, piece, f"pane_{j}", "plate", (bx - sgn * 0.8 + sgn * dx, 2.6, 0.6 * (j - 1)), (sgn * (0.3 + 0.2 * j), 1.0, 0), Lh, 1.7, 0.5, glow=True)
        orb(k.part(piece, "drop", "crystal", layer=2, smooth=0), (bx + sgn * 0.4, 3.4, 2.2), 0.7, sides=6)
    L.pauldrons(k, fn, 4.5, "plate", 1)


def wings_gusion(k, sgn):
    """GUSION_PANES: the wing is a fan of glass panes, trapezoids of mirror in falling sizes with a glowing frame and a water drop between each."""
    bone_wing(k, sgn, "plate", 1.1)
    sdp = sd(sgn)
    for seg, spec, B in (("in", ((-14, 0.15, 10.0, 4.4), (-44, 0.5, 9.5, 4.0), (-76, 0.85, 8.0, 3.4)), INNER),
                         ("out", ((22, 0.0, 14.0, 4.6), (2, 0.1, 19.0, 5.4), (-18, 0.22, 21.0, 5.6), (-38, 0.38, 19.5, 5.2), (-58, 0.55, 16.0, 4.6), (-80, 0.72, 11.0, 3.6)), OUTER)):
        for j, (ang, s, Ln, w) in enumerate(spec):
            a = rad(ang)
            d = np.array([sgn * math.cos(a), math.sin(a), 0.0])
            n = np.array([-d[1], d[0], 0.0])
            b = np.array([sgn * s * B, 0.0, 0.0])
            q = [b - n * w * 0.30, b + d * Ln * 0.92 - n * w * 0.5, b + d * Ln + n * w * 0.12, b + n * w * 0.30 + d * Ln * 0.04]
            poly = [(p[0], p[1]) for p in q]
            extrude(k, f"wing_{seg}_{sdp}", f"pane_{j}", "plate", poly, -0.25, 0.25)
            for e in range(4):
                rib(k, f"wing_{seg}_{sdp}", f"frame_{j}{e}", "crystal", tuple(q[e] + (0, 0, 0.3)), tuple(q[(e + 1) % 4] + (0, 0, 0.3)), 0.34, 0.12, layer=2, taper=0.3)
            if j % 2 == 0:
                orb(k.part(f"wing_{seg}_{sdp}", f"drop_{j}", "crystal", layer=2, smooth=0), tuple(b + d * Ln * 0.5 + (0, 0, 0.5)), 0.5, sides=5)


def halo_gusion(k):
    du.sweep(k.part("halo", "ring", "plate"), ring_xz(HR * 0.85), C(0.32, S(5)))
    for i in range(7):
        a = 2 * PI * i / 7
        shard(k, "halo", f"pane_{i}", "plate", (HR * 0.85 * math.cos(a), 0.0, HR * 0.85 * math.sin(a)), (math.cos(a) * 0.7, 0.8, math.sin(a) * 0.7), 4.0 + (i % 2) * 1.2, 1.4, 0.4, layer=2)


def back_gusion(k):
    """GUSION_MIRROR: the tide mirror: a ring of silver wave-scrolls with a pane of light inside, three crests of surf rising at its foot."""
    R_ = 11.0
    du.tube_direct(k.part("backdrop", "frame", "plate", smooth=40), ring_xy(R_, 0.0, 14), 0.9, 0.9, 5)
    du.tube_direct(k.part("backdrop", "frame_in", k.trim()), ring_xy(R_ * 0.9, 0.3, 14), 0.34, 0.34, 4)
    disc(k.part("backdrop", "glass", "crystal", layer=2), (0, 0, -0.2), R_ * 0.88, 0.3, "z", 12)
    for i in range(8):
        a = 2 * PI * i / 8
        c = (R_ * math.cos(a), R_ * math.sin(a), 0.0)
        sc = arc_pts((c[0] + math.cos(a) * 1.6, c[1] + math.sin(a) * 1.6, 0.0), 1.6, math.degrees(a) + 180, math.degrees(a) + 450, 5)
        sweep_flat(k.part("backdrop", f"scroll_{i}", "plate", smooth=45), sc, 1.0, 0.6, pf_taper, (0, 0, 1))
    for i, (x, h) in enumerate(((-6.0, 4.6), (0.0, 6.4), (6.0, 4.6))):
        sweep_flat(k.part("backdrop", f"surf_{i}", "cloth", smooth=45), arc_pts((x, -R_ - 1.0, 0.0), h * 0.6, 200, 20, 8)[::-1], 2.2, 0.5, pf_feather, (0, 0, 1))
    for i in range(4):
        a = i * PI / 2 + PI / 4
        shard(k, "backdrop", f"point_{i}", "plate", (R_ * math.cos(a) * 1.12, R_ * math.sin(a) * 1.12, 0.0), (math.cos(a), math.sin(a), 0.0), 4.6, 1.8, 0.6, glow=True)


def ring_xy(R_, z=0.0, n=18, a0=0, a1=360):
    return dub.ring_xy(R_, z, n, a0, a1)


register("GUSION", head=head_gusion, chest=chest_gusion, pauldron=pauldron_gusion, wings=wings_gusion, halo=halo_gusion, back=back_gusion)


# ============================================================================================ VEPAR: the mermaid sovereign of wrecks
def coral(k, piece, name, base, direction, Ln, r, depth=2, mat="bone", pearl=True):
    d = unit(direction)
    side = np.cross(d, (0, 0, 1.0))
    side = unit(side) if np.linalg.norm(side) > 1e-6 else np.array([1.0, 0, 0])
    b = np.asarray(base, float)
    tip = b + d * Ln
    tapered_tube(k, piece, name, mat, [tuple(b), tuple(b + d * Ln * 0.5 + side * 0.3), tuple(tip)], r, r * 0.2, 5)
    if depth > 0:
        for q, sg in enumerate((1, -1)):
            coral(k, piece, f"{name}{q}", tuple(b + d * Ln * (0.5 + 0.1 * q)), unit(d + side * sg * 0.8), Ln * 0.5, r * 0.65, depth - 1, mat, pearl)
    elif pearl:
        orb(k.part(piece, name + "_p", "crystal", layer=2, smooth=0), tuple(tip + d * 0.4), r * 0.9, sides=5)


def head_vepar(k):
    head_base(k, "circlet", cheeks=False, nasal=False)
    for i, (x, z, ang) in enumerate(((-3.0, 1.0, 28), (-1.4, 3.0, 12), (0.0, 3.6, 0), (1.4, 3.0, -12), (3.0, 1.0, -28))):      # a crown of coral
        r = rad(ang)
        coral(k, "helm", f"coral_{i}", (x, 6.0, z), (math.sin(-r), 1.0, 0.0), 5.6 - 0.8 * abs(i - 2), 0.55, 1)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # a nautilus shell at each temple
        sp = [(5.2 * sgn + sgn * (0.35 + 0.5 * t / PI) * math.cos(t), 4.4 + (0.35 + 0.5 * t / PI) * math.sin(t), 0.0) for t in np.linspace(0, 3.4 * PI, 20)]
        du.tube_direct(k.part(piece, "shell", "bone", smooth=40), sp, 0.95, 0.45, 5)
        glow_orb(k, piece, "pearl", (5.2 * sgn, 4.4, 0.5), 0.5, sides=5)
    for sgn, nm in ((1, "l"), (-1, "r")):                                                                    # long fin ears
        lf(k, "ears", "fin_" + nm, "cloth", [(sgn * 4.2, 3.4, 0.6), (sgn * 8.4, 7.8, -0.6), (sgn * 11.0, 6.4, -2.0), (sgn * 10.0, 2.6, -2.2), (sgn * 6.0, 0.8, -0.6)])
        for j, (ex, ey) in enumerate(((8.4, 7.6), (10.8, 6.0), (9.8, 2.8))):
            rib(k, "ears", f"fin_rib_{nm}{j}", "crystal", (sgn * 4.4, 3.4, 0.7), (sgn * ex, ey, -0.8 - 0.4 * j), layer=2)
    for i, x in enumerate((-2.6, 0.0, 2.6)):
        ribbon(k, "helm", f"weed_{i}", "cloth", [(x, 7.4, -3.8), (x * 1.3, 4.0, -5.8), (x * 1.6 + 1.0, -1.0, -6.4), (x * 1.2 - 1.0, -6.0, -5.8), (x * 1.5, -10.0, -5.0)], 1.8, 0.25, 0.5)
    coral(k, "crest", "coral", (0, 5.8, 4.2), (0, 1.0, 0.1), 3.4, 0.45, 1)
    orb(k.part("crest", "pearl", "bone", smooth=0), (0, 5.7, 4.7), 0.8, sides=6)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_vepar(k):
    dua.chest_harness(k)
    scallop(k, "chest", "shell", "plate", (0.0, -10.2, 3.1), 4.0, 7, 150.0, up_deg=90.0)
    orb(k.part("chest", "pearl", "bone", smooth=0), (0.0, -10.0, 3.5), 0.7, sides=6)


def pauldron_vepar(k):
    def fn(k, piece, sgn, cx, bx):
        scallop(k, piece, "shell", "plate", (bx - sgn * 0.6, 2.2, 1.4), 6.4, 7, 120.0, up_deg=90.0 + sgn * -10.0)
        orb(k.part(piece, "pearl", "bone", smooth=0), (bx - sgn * 0.6, 2.4, 2.0), 0.9, sides=6)
        coral(k, piece, "coral", (bx, 2.4, -1.4), (sgn * 0.6, 1.0, -0.2), 4.4, 0.5, 1)
    L.pauldrons(k, fn, 4.6, "extra", 2)


def wings_vepar(k, sgn):
    """VEPAR_MANTA: the wing is a manta's: a broad membrane swept back from a spined edge, ribs of light running to the scalloped rim, a whip at the tip."""
    bone_wing(k, sgn, "extra", 1.2)
    sdp = sd(sgn)
    inner = [(0.0, 3.0), (6.0, 5.0), (11.0, 1.0), (10.0, -5.0), (3.0, -6.0), (0.0, -2.0)]
    lf(k, f"wing_in_{sdp}", "mem", "cloth", [(sgn * x, y, -0.1) for x, y in inner])
    for j, (x, y) in enumerate(inner[1:4]):
        rib(k, f"wing_in_{sdp}", f"vein_{j}", "crystal", (0, 0, 0.1), (sgn * x, y, 0.1), layer=2)
    outer = [(0.0, 3.0), (8.0, 6.4), (17.0, 5.0), (25.0, 0.6), (26.0, -5.0), (21.0, -10.0), (13.0, -11.0), (6.0, -8.0), (0.0, -3.0)]
    lf(k, f"wing_out_{sdp}", "mem", "cloth", [(sgn * x, y, -0.1) for x, y in outer])
    lf(k, f"wing_out_{sdp}", "mem_hi", "plate", [(sgn * x, y, 0.15) for x, y in [(0.0, 3.0), (8.0, 6.4), (17.0, 5.0), (25.0, 0.6), (18.0, 1.0), (8.0, 2.0)]])
    for j, (x, y) in enumerate(outer[1:8]):
        rib(k, f"wing_out_{sdp}", f"vein_{j}", "crystal", (0, 0.0, 0.3), (sgn * x * 0.96, y * 0.96, 0.3), 0.34, 0.12, layer=2, taper=1.0)
    for j, (x, y) in enumerate(outer[1:4]):
        spike(k.part(f"wing_out_{sdp}", f"spine_{j}", "bone"), (sgn * x * 0.95, y * 0.95, 0.0), (sgn * 0.2, 1.0, 0), 3.0, 0.5, 4)
    sweep_tube(k.part(f"wing_out_{sdp}", "whip", "cloth", smooth=40), [(sgn * 25.0, 0.0, 0.0), (sgn * 30.0, -4.0, 0.0), (sgn * 33.0, -10.0, 0.3), (sgn * 31.0, -16.0, 0.0)], 0.6, 0.1, 5)
    glow_orb(k, f"wing_out_{sdp}", "bead", (sgn * 31.0, -16.0, 0.0), 0.6, sides=5)


def tail_vepar(k):
    ml.loft(k.part("tail_seg", "body", "plate", smooth=40), "-z", (0, 0, 0), [(0, 1.9, 1.9), (3.0, 2.0, 2.0), (6.0, 1.7, 1.7)], C(1.0, S(10)), caps=(False, False))
    collar(k.part("tail_seg", "scale_band", "bone"), -0.2, 2.1, 0.8, lip=0.2, sides=S(10), axis="-z")
    spike(k.part("tail_seg", "fin", "cloth"), (0, 1.8, -1.0), (0, 1.0, -0.6), 3.2, 0.7, 4)
    for sgn in (-1, 1):
        sweep_flat(k.part("tail_tip", f"fluke_{sgn}", "cloth", smooth=45), [(0, 0, -1.0), (sgn * 3.6, 0, -3.6), (sgn * 7.8, 0, -8.0), (sgn * 9.0, 0, -13.0)], 5.0, 0.4, lambda s: (0.55 + 0.9 * s) * (1 - s ** 3) + 0.05, (0, 1, 0))
        sweep_flat(k.part("tail_tip", f"ray_{sgn}", "crystal", layer=2), [(sgn * 0.4, 0.3, -1.0), (sgn * 3.6, 0.3, -3.6), (sgn * 7.8, 0.3, -8.0), (sgn * 8.6, 0.3, -12.0)], 0.7, 0.3, pf_taper, (0, 1, 0))
    ml.loft(k.part("tail_tip", "stem", "plate"), "-z", (0, 0, 0), [(0, 1.6, 1.6), (4.0, 0.9, 0.9)], C(1.0, S(8)), caps=(False, False))


def halo_vepar(k):
    du.sweep(k.part("halo", "ring", "bone"), ring_xz(HR), C(0.34, S(5)))
    for i in range(5):
        a = 2 * PI * i / 5
        cx, cz = HR * math.cos(a), HR * math.sin(a)
        orb(k.part("halo", f"star_c_{i}", "crystal", layer=2, smooth=0), (cx, 0.0, cz), 0.55, sides=5)
        for j in range(5):
            b = a + 2 * PI * j / 5
            spike(k.part("halo", f"star_{i}_{j}", "crystal", layer=2, smooth=0), (cx, 0.0, cz), (math.cos(b), 0.15, math.sin(b)), 1.9, 0.38, 4)


def back_vepar(k):
    """VEPAR_HELM: a ship's wheel stands behind the shoulders: a rim of oak, a hub, eight spokes running out into turned handles, a rope of light round the rim."""
    R_ = 9.0
    du.tube_direct(k.part("backdrop", "rim", "extra", smooth=40), ring_xy(R_, 0.0, 14), 0.9, 0.9, 5)
    du.tube_direct(k.part("backdrop", "rim_in", "trim"), ring_xy(R_ * 0.8, 0.0, 12), 0.4, 0.4, 4)
    du.tube_direct(k.part("backdrop", "rope", "crystal", layer=2), ring_xy(R_ * 1.0, 0.7, 12), 0.22, 0.22, 4)
    disc(k.part("backdrop", "hub", k.trim()), (0, 0, 0), 2.4, 1.6, "z", 12)
    glow_orb(k, "backdrop", "hub_gem", (0, 0, 1.0), 1.1)
    for i in range(8):
        a = 2 * PI * i / 8
        d = (math.cos(a), math.sin(a), 0.0)
        rib(k, "backdrop", f"spoke_{i}", "extra", (d[0] * 2.0, d[1] * 2.0, 0.0), (d[0] * (R_ + 2.2), d[1] * (R_ + 2.2), 0.0), 0.9, 0.45, taper=0.8)
        orb(k.part("backdrop", f"handle_{i}", k.trim(), smooth=0), (d[0] * (R_ + 3.0), d[1] * (R_ + 3.0), 0.0), 1.1, sides=6)
    coral(k, "backdrop", "coral", (0.0, -R_ - 1.0, 0.0), (0.0, -1.0, 0), 4.0, 0.6, 1)


def mantle_vepar(k):
    cape(k, 22.0, 8.0, "bone", 1.0, k.trim())
    jag_hem(k, "mantle", "tatter", "bone", -22.2, -4.6, 5.2, 4.6, 6, 0.6)


def weapon_vepar(k):
    """VEPAR_ANCHOR: a ship's anchor 3 blocks long, held by its shank: a ring and a length of chain at the butt, a stock across the head, two great flukes curving up round a spear point."""
    dua.w_shaft(k, -10, 30, 0.95, "plate", "shank")
    for j, y in enumerate((-6, -2, 2)):
        dua.w_ring(k, y, 1.3, f"rope_{j}", "cloth", 1.2)
    du.sweep(k.part("weapon", "eye", k.trim()), ring_path((0, -13.4, 0), 2.4, 0, 360, "xy", 10), C(0.55, S(6)))
    sweep_tube(k.part("weapon", "chain", "plate", smooth=40), [(0, -15.8, 0), (1.6, -19.0, 0), (-1.2, -22.0, 0), (1.4, -25.0, 0)], 0.38, 0.3, 5)
    du.box(k.part("weapon", "stock", k.trim()), (-7.0, 25.5, -1.0), (7.0, 27.3, 1.0), chamfer=0.4)
    for sgn in (-1, 1):
        orb(k.part("weapon", f"knob_{sgn}", "plate", smooth=0), (sgn * 7.4, 26.4, 0), 1.3, sides=6)
    dua.w_ring(k, 29.0, 1.7, "crown", "trim", 1.8)
    for sgn, nm in ((1, "l"), (-1, "r")):
        arm = [(sgn * 0.8, 30.0, 0), (sgn * 5.0, 32.0, 0), (sgn * 9.0, 37.0, 0), (sgn * 10.4, 43.0, 0), (sgn * 8.6, 47.0, 0)]
        sweep_flat(k.part("weapon", "fluke_" + nm, "plate", smooth=45), curve(arm, 5), 3.0, 1.4, lambda s: 0.8 + 0.2 * s, (0, 0, 1))
        lf(k, "weapon", "palm_" + nm, "plate", [(sgn * 8.4, 44.0, 0.0), (sgn * 12.6, 44.0, 0.0), (sgn * 9.0, 49.4, 0.0)])
        sweep_flat(k.part("weapon", "edge_" + nm, "crystal", layer=2), curve([(x, y, 0.8) for (x, y, z) in arm], 5), 0.7, 0.3, pf_taper, (0, 0, 1))
    spike(k.part("weapon", "point", "plate"), (0, 30.0, 0), (0, 1.0, 0), 14.0, 1.3, 6)
    glow_orb(k, "weapon", "gem", (0, 34.0, 1.2), 0.9)


register("VEPAR", head=head_vepar, chest=chest_vepar, pauldron=pauldron_vepar, wings=wings_vepar, tail=tail_vepar, halo=halo_vepar, back=back_vepar, mantle=mantle_vepar, weapon=weapon_vepar)

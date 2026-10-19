"""djinn36_set4: Crocell, Furcas, Balam (their weapons are in djinn36_set9)."""
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


def snowflake(k, piece, name, c, R_, mat="plate", glow_mat="crystal", w=1.0, branches=((0.42, 0.34), (0.7, 0.26)), arms=6, a0=90.0, layer=0):
    """A snowflake in the xy plane: arms of bars, two pairs of side branches on each, a shard at every tip."""
    cx, cy, cz = c
    for i in range(arms):
        a = rad(a0 + 360.0 / arms * i)
        d = np.array([math.cos(a), math.sin(a), 0.0])
        tip = np.array(c) + d * R_
        du.bar(k.part(piece, f"{name}_arm{i}", mat, layer=layer), c, tuple(tip), w, w * 0.45, up=(0, 0, 1), taper=R_ * 0.25)
        for q, (t, fl) in enumerate(branches):
            p = np.array(c) + d * R_ * t
            for sg in (-1, 1):
                b = a + sg * rad(55)
                e = p + np.array([math.cos(b), math.sin(b), 0.0]) * R_ * fl
                du.bar(k.part(piece, f"{name}_br{i}{q}{sg}", mat, layer=layer), tuple(p), tuple(e), w * 0.7, w * 0.3, up=(0, 0, 1), taper=0.6)
        shard(k, piece, f"{name}_tip{i}", "crystal", tuple(tip), tuple(d), R_ * 0.2, w * 1.4, 0.5, layer=2)


# ============================================================================================ CROCELL: the frost sage
def head_crocell(k):
    head_base(k, "circlet", cheeks=False, nasal=False)
    brow_gem(k, 5.9, 0.7)
    for i, a in enumerate(np.linspace(-120, 120, 9)):                                                       # a crown of standing ice
        r = rad(a)
        Lh = 6.2 if i == 4 else 3.0 + (i % 2) * 1.8
        shard(k, "helm", f"ice_{i}", "plate", (math.sin(r) * 4.5, 6.0, math.cos(r) * 4.5), (math.sin(r) * 0.25, 1.0, math.cos(r) * 0.2), Lh, 1.3, 0.8, glow=True)
    for i, x in enumerate((-2.6, -1.3, 0.0, 1.3, 2.6)):                                                      # an icicle beard
        spike(k.part("helm", f"beard_{i}", "plate"), (x, 1.0, 4.3), (0, -1.0, 0.12), 6.4 - 1.4 * abs(i - 2) + 1.0, 0.5, 4)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        shard(k, piece, "crystal", "plate", (sgn * 3.8, 6.2, 0.0), (sgn * 0.45, 1.0, -0.2), 9.0, 2.0, 1.0, glow=True)
        shard(k, piece, "crystal_b", "plate", (sgn * 4.2, 5.4, 0.4), (sgn * 0.95, 0.6, -0.1), 5.0, 1.4, 0.8, glow=True)
    hair_blades(k, (-2.4, 0.0, 2.4), 8.0, -1.8, 12.0, 2.0, (0, 0.2, -1.0), (0, -1.0, -0.4), mat="cloth", spread=0.3, name="frost")
    for i, x in enumerate((-1.5, 0.0, 1.5)):
        shard(k, "crest", f"ice_{i}", "plate", (x, 5.8, 4.3), (x * 0.1, 1.0, -0.05), 3.4 if x == 0 else 2.4, 1.0, 0.6, glow=True)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_crocell(k):
    dua.chest_harness(k)
    snowflake(k, "chest", "flake", (0.0, -9.6, 3.1), 3.0, "trim", w=0.5, branches=((0.5, 0.3),))


def pauldron_crocell(k):
    def fn(k, piece, sgn, cx, bx):
        for j, (dx, Lh) in enumerate(((0.0, 6.6), (1.6, 4.6), (-1.6, 4.2))):
            shard(k, piece, f"ice_{j}", "plate", (bx - sgn * 0.6 + sgn * dx, 2.6, 0.5 * (j - 1)), (sgn * (0.2 + 0.25 * j), 1.0, 0), Lh, 1.8, 0.7, glow=True)
    L.pauldrons(k, fn, 4.5, "plate", 1)


def wings_crocell(k, sgn):
    """CROCELL_RIME: the wing is frost on the bone: five crystal spines, each with branching crystals either side like the arms of a snowflake, a core of light in each."""
    bone_wing(k, sgn, "plate", 1.1)
    sdp = sd(sgn)
    for seg, spec, B in (("in", ((-22, 0.2, 9.0), (-60, 0.65, 8.0)), INNER),
                         ("out", ((24, 0.0, 14.0), (4, 0.1, 20.0), (-18, 0.22, 23.0), (-40, 0.38, 20.0), (-64, 0.56, 14.0)), OUTER)):
        for j, (ang, s, Ln) in enumerate(spec):
            a = rad(ang)
            d = np.array([sgn * math.cos(a), math.sin(a), 0.0])
            b = np.array([sgn * s * B, 0.0, 0.0])
            shard(k, f"wing_{seg}_{sdp}", f"spine_{j}", "plate", tuple(b), tuple(d), Ln, 2.2, 0.8, glow=True)
            for q, t in enumerate((0.4, 0.68)):
                p = b + d * Ln * t
                for sg in (-1, 1):
                    ba = a + sg * rad(50) * (1 if sgn > 0 else -1)
                    bd = np.array([sgn * math.cos(ba), math.sin(ba), 0.0])
                    shard(k, f"wing_{seg}_{sdp}", f"br_{j}{q}{sg}", "plate", tuple(p), tuple(bd), Ln * (0.3 - 0.07 * q), 1.3, 0.6)


def halo_crocell(k):
    du.sweep(k.part("halo", "ring", "plate"), ring_xz(HR), C(0.3, S(5)))
    for i in range(8):
        a = 2 * PI * i / 8
        shard(k, "halo", f"ice_{i}", "crystal", (HR * math.cos(a), 0.0, HR * math.sin(a)), (math.cos(a) * 0.3, 1.0, math.sin(a) * 0.3), 2.0 + (i % 2) * 2.2, 0.9, 0.4, layer=2)
        shard(k, "halo", f"drip_{i}", "plate", (HR * math.cos(a), 0.0, HR * math.sin(a)), (0, -1.0, 0), 1.6 + (i % 2), 0.7, 0.4)


def back_crocell(k):
    """CROCELL_FLAKE: a great snowflake stands behind the shoulders, six arms with branching crystals, a ring of rime round its heart."""
    snowflake(k, "backdrop", "flake", (0.0, 0.0, 0.0), 13.0, "plate", w=1.4, branches=((0.38, 0.3), (0.62, 0.26), (0.82, 0.16)))
    du.tube_direct(k.part("backdrop", "ring", "trim", smooth=40), ring_xy(4.0, 0.2, 12), 0.5, 0.5, 4)
    glow_orb(k, "backdrop", "heart", (0, 0, 0.6), 1.6)


def mantle_crocell(k):
    cape(k, 22.0, 8.0, "cloth", 1.0, "plate")
    jag_hem(k, "mantle", "icicle", "plate", -22.2, -4.6, 5.2, 5.6, 6, 0.6)


register("CROCELL", head=head_crocell, chest=chest_crocell, pauldron=pauldron_crocell, wings=wings_crocell, halo=halo_crocell, back=back_crocell, mantle=mantle_crocell)


# ============================================================================================ FURCAS: the old knight of the ice wind
def kite_poly(w, h_top, h_bot):
    return [(-w, h_top), (w, h_top), (w, h_top - h_top * 0.35 - 2.0), (w * 0.55, -h_bot * 0.55), (0, -h_bot), (-w * 0.55, -h_bot * 0.55), (-w, h_top - h_top * 0.35 - 2.0)]


def head_furcas(k):
    head_base(k, "visor", nasal=True)
    ml.lathe(k.part("helm", "crest_ridge", k.trim()), "y", (0.0, 8.8, -0.2), [(0.0, 1.0), (1.4, 0.9), (2.6, 0.0)], sides=S(8), caps=(False, False), squash=3.0)
    for i in range(7):                                                                                       # a plume of frozen cloth swept back
        x = (i - 3) * 0.9
        blade3(k.part("helm", f"plume_{i}", "cloth", smooth=45), (x, 9.0, 0.6), (x * 0.1, 0.8, -0.4), (x * 0.2, -0.2, -1.0), 13.0 - 1.2 * abs(i - 3), 1.6, 0.4, wdir=(1, 0, 0), n=6)
    for i, x in enumerate((-1.8, 0.0, 1.8)):                                                                 # a long white moustache and beard
        ribbon(k, "helm", f"beard_{i}", "cloth", [(x, 1.4, 4.3), (x * 1.2, -1.6, 4.8), (x * 1.4, -4.6, 4.4), (x * 1.0, -8.0 - (i == 1) * 3.0, 4.0)], 1.8, 0.3, 0.7)
    for sgn in (-1, 1):
        for j in range(3):
            spike(k.part("helm", f"frost_{sgn}{j}", "plate"), (sgn * 4.4, 3.0 + 1.2 * j, 1.0 - 1.4 * j), (sgn * 1.0, -0.2, -0.3), 2.4 - 0.3 * j, 0.4, 4)
    for i, x in enumerate((-1.2, 0.0, 1.2)):
        blade3(k.part("crest", f"plume_{i}", "cloth", smooth=45), (x, 6.2, 4.2), (x * 0.3, 1.0, -0.2), (x * 0.5, 0.4, -0.9), 4.4, 1.2, 0.4, wdir=(1, 0, 0), n=3)
    gemz(k, "crest", "jewel", (0, 5.6, BROW_Z), 0.7, 1.2)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_furcas(k):
    dua.chest_cuirass(k)
    du.bar(k.part("chest", "cross_v", k.trim()), (0, -4.2, 3.0), (0, -10.6, 3.0), 0.9, 0.35, up=(0, 0, 1), taper=0.5)
    du.bar(k.part("chest", "cross_h", k.trim()), (-2.4, -6.6, 3.0), (2.4, -6.6, 3.0), 0.9, 0.35, up=(0, 0, 1), taper=0.5)
    for sg in (-1, 1):
        spike(k.part("chest", f"ice_{sg}", "plate"), (sg * 1.4, -8.4, 3.0), (0, -1.0, 0.2), 2.4, 0.4, 4)


def belt_furcas(k):
    dua.belt_light(k)
    dua.SKIRT["FURCAS"] = 20.0
    dua.build_skirt(k, "FURCAS")


def pauldron_furcas(k):
    def fn(k, piece, sgn, cx, bx):
        for j in range(3):                                                                                   # three lames sloping down over the arm
            du.box(k.part(piece, f"lame_{j}", "plate"), (bx - sgn * 3.0 - 2.0, 1.4 - 1.5 * j, -3.0 + 0.4 * j), (bx - sgn * 3.0 + 4.6, 2.4 - 1.5 * j, 3.0 - 0.4 * j), chamfer=0.4)
        for j in range(3):
            spike(k.part(piece, f"ice_{j}", "plate"), (bx + sgn * 0.8, 2.6, -1.8 + 1.8 * j), (sgn * 0.5, 1.0, 0), 4.6 - 0.6 * j, 0.7, 4)
        glow_orb(k, piece, "gem", (bx + sgn * 0.4, 3.0, 2.8), 0.5, sides=4)
    L.pauldrons(k, fn, 5.6, "plate", 2)


def wings_furcas(k, sgn):
    """FURCAS_ICEWIND: the wing is a knight's cloak torn by the ice wind: a broad frayed sheet of cloth on the bone, icicles hanging all along its edge, ribs of frost."""
    bone_wing(k, sgn, "extra", 1.3)
    sdp = sd(sgn)
    rng = np.random.default_rng(5)
    inner = [(0.0, 3.0), (7.0, 4.0), (11.0, -1.0), (9.0, -7.0), (3.0, -6.0), (0.0, -2.0)]
    lf(k, f"wing_in_{sdp}", "cloak", "cloth", [(sgn * x, y, -0.1) for x, y in inner])
    outer = [(0.0, 4.0), (8.0, 7.0), (17.0, 6.0), (25.0, 1.0), (27.0, -5.0), (23.0, -11.0), (15.0, -13.0), (7.0, -10.0), (0.0, -4.0)]
    lf(k, f"wing_out_{sdp}", "cloak", "cloth", [(sgn * x, y, -0.1) for x, y in outer])
    for j in range(len(outer) - 3):
        x, y = outer[2 + j]
        spike(k.part(f"wing_out_{sdp}", f"icicle_{j}", "plate"), (sgn * x * 0.97, y * 0.97, 0.0), (0, -1.0, 0.0), 3.4 + 2.0 * rng.random(), 0.55, 4)
    for j, (x, y) in enumerate(outer[1:7]):
        rib(k, f"wing_out_{sdp}", f"rib_{j}", "crystal", (0, 0, 0.3), (sgn * x * 0.95, y * 0.95, 0.3), 0.34, 0.12, layer=2, taper=1.0)
    for j in range(3):
        x, y = inner[1 + j]
        spike(k.part(f"wing_in_{sdp}", f"icicle_{j}", "plate"), (sgn * x * 0.95, y * 0.95, 0.0), (0, -1.0, 0.0), 2.8, 0.45, 4)


def halo_furcas(k):
    du.sweep(k.part("halo", "ring", k.trim()), ring_xz(HR), C(0.34, S(5)))
    for i in range(6):
        a = 2 * PI * i / 6
        spike(k.part("halo", f"ice_{i}", "plate", layer=2), (HR * math.cos(a), 0.0, HR * math.sin(a)), (0, -1.0, 0), 3.0 + (i % 2) * 1.4, 0.5, 4)
        glow_orb(k, "halo", f"gem_{i}", (HR * math.cos(a), 0.2, HR * math.sin(a)), 0.35, sides=4)


def back_furcas(k):
    """FURCAS_BULWARK: a knight's kite shield stands behind the shoulders in a rim of frost, a cross of light on its face, two lances crossed behind it with pennons."""
    for sgn in (-1, 1):
        n0 = len(k.m.parts)
        du.bar(k.part("backdrop", f"shaft_{sgn}", "extra"), (0, -14.0, -1.4), (0, 14.0, -1.4), 0.9, 0.5, up=(0, 0, 1))
        spike(k.part("backdrop", f"head_{sgn}", "plate"), (0, 14.0, -1.4), (0, 1.0, 0), 5.0, 0.9, 4)
        ribbon(k, "backdrop", f"pennon_{sgn}", "cloth", [(0.0, 11.0, -1.0), (sgn * 3.0, 11.6, -1.0), (sgn * 6.0, 10.0, -1.0)], 2.6, 0.25, 0.6)
        L.transform_since(k, n0, rot=("z", -sgn * 26))
    poly = kite_poly(7.4, 12.0, 14.0)
    extrude(k, "backdrop", "shield", "plate", poly, -0.6, 0.6, smooth=40)
    rim = [(x * 0.94, y * 0.94 - 0.0, 0.7) for x, y in poly] + [(poly[0][0] * 0.94, poly[0][1] * 0.94, 0.7)]
    du.tube_direct(k.part("backdrop", "rim", k.trim(), smooth=40), rim, 0.45, 0.45, 4)
    du.bar(k.part("backdrop", "cross_v", "crystal", layer=2), (0, 9.0, 0.8), (0, -9.0, 0.8), 1.1, 0.4, up=(0, 0, 1), taper=1.0)
    du.bar(k.part("backdrop", "cross_h", "crystal", layer=2), (-4.6, 3.4, 0.8), (4.6, 3.4, 0.8), 1.1, 0.4, up=(0, 0, 1), taper=1.0)
    orb(k.part("backdrop", "boss", k.trim(), smooth=0), (0, 3.4, 1.0), 1.5, sides=6)
    for i in range(7):
        spike(k.part("backdrop", f"ice_{i}", "plate"), (-6.0 + 2.0 * i, -3.0 - 8.0 * abs(i - 3) / 3.0 - 1.0 + 0.0, 0.0), (0, -1.0, 0.2), 2.4, 0.45, 4)


register("FURCAS", head=head_furcas, chest=chest_furcas, belt=belt_furcas, pauldron=pauldron_furcas, wings=wings_furcas, halo=halo_furcas, back=back_furcas)


# ============================================================================================ BALAM: the three-headed prophet
def head_balam(k):
    head_base(k, "open", nasal=False)
    for i, (a, mt) in enumerate(((-34, "bone"), (0, "plate"), (34, "bone"))):                                # three faces on the brow: the ram, the man, the bull
        r = rad(a)
        L.mask(k, "helm", f"face_{i}", (math.sin(r) * 4.9, 7.6 + (0.6 if i == 1 else 0.0), math.cos(r) * 4.9), 1.0 + (0.25 if i == 1 else 0.0), a, mt)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # the bull's lyre horns
        path = [(sgn * 3.4, 6.2, 0.0), (sgn * 6.4, 6.6, 0.2), (sgn * 7.8, 9.0, 0.6), (sgn * 7.2, 12.4, 1.0), (sgn * 5.6, 14.8, 1.0)]
        tapered_tube(k, piece, "lyre", "bone", path, 1.5, 0.15, 6)
        sp = [(sgn * (2.2 + (0.4 + 0.45 * t / PI) * math.cos(t)), 9.4 + (0.4 + 0.45 * t / PI) * math.sin(t), -1.4) for t in np.linspace(0, 3.8 * PI, 18)]
        du.tube_direct(k.part(piece, "ram_curl", "bone", smooth=40), sp, 0.8, 0.35, 5)
    for i, x in enumerate((-2.4, 0.0, 2.4)):                                                                 # a crown of flame behind them
        tongue(k.part("helm", f"flame_{i}", "crystal", layer=2, smooth=45), (x, 9.4, -1.8), (0, 1.0, -0.2), 5.4 - 0.8 * abs(x), 2.0, 0.5, 0.7, i * 1.5, plane="xy", n=4)
    for sgn, nm in ((1, "l"), (-1, "r")):
        blade3(k.part("ears", "ear_" + nm, "cloth", smooth=45), (sgn * 4.2, 3.8, -0.4), (sgn * 1.0, 0.3, -0.2), (sgn * 1.0, 0.0, -0.4), 4.2, 1.8, 0.4, wdir=(0, 0, 1), n=3)
    L.mask(k, "crest", "face", (0.0, 6.6, 4.5), 0.9)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_balam(k):
    dua.chest_harness(k)
    for i, x in enumerate((-1.8, 0.0, 1.8)):
        tongue(k.part("chest", f"flame_{i}", "crystal", layer=2, smooth=45), (x, -11.4, 3.2), (0, 1.0, 0), 3.6 + (i == 1) * 1.4, 1.4, 0.4, 0.4, i, n=3)
    ring_at(k, "chest", "ring", k.trim(), (0.0, -10.4, 3.1), 3.0, 0.3, "z", 10)


def pauldron_balam(k):
    def fn(k, piece, sgn, cx, bx):
        sp = [(bx - sgn * 0.6 + sgn * (0.4 + 0.5 * t / PI) * math.cos(t), 3.6 + (0.4 + 0.5 * t / PI) * math.sin(t), 0.0) for t in np.linspace(0, 3.6 * PI, 16)]
        du.tube_direct(k.part(piece, "curl", "bone", smooth=40), sp, 0.8, 0.34, 5)
        tongue(k.part(piece, "flame", "crystal", layer=2, smooth=45), (bx + sgn * 1.4, 2.8, -1.6), (sgn * 0.2, 1.0, 0), 5.0, 1.8, 0.4, 0.5, 0.0, n=3)
    L.pauldrons(k, fn, 4.6, "plate", 1)


def wings_balam(k, sgn):
    """BALAM_TRIAD: the wing is three plumes of fire, each a fan of four blade-feathers with a flame burning between them, three tongues of the prophecy."""
    bone_wing(k, sgn, "extra", 1.2)
    sdp = sd(sgn)
    for j, (a, s, Ln) in enumerate(((-30, 0.3, 9.0), (-70, 0.8, 8.0))):
        fan(k, f"wing_in_{sdp}", f"f{j}_", (sgn * s * INNER, 0, 0), (a + 10, a, a - 10), (Ln * 0.8, Ln, Ln * 0.8), 2.2, "plate", -12, 3, sgn)
        tongue(k.part(f"wing_in_{sdp}", f"fire_{j}", "crystal", layer=2, smooth=45), (sgn * s * INNER, 0, 0.2), (sgn * math.cos(rad(a)), math.sin(rad(a)), 0), Ln * 0.9, 2.4, 0.4, 0.6 * sgn, j, n=4)
    for j, (a, s) in enumerate(((22, 0.0), (-12, 0.18), (-46, 0.4))):
        base = (sgn * s * OUTER, 0, 0)
        fan(k, f"wing_out_{sdp}", f"f{j}_", base, (a + 16, a + 5, a - 6, a - 17), (17.0, 22.0, 24.0, 18.0), 2.8, "plate", -16, 4, sgn, bz=0.12)
        tongue(k.part(f"wing_out_{sdp}", f"fire_{j}", "crystal", layer=2, smooth=45), (base[0], 0, 0.2), (sgn * math.cos(rad(a)), math.sin(rad(a)), 0), 19.0, 4.0, 0.4, 0.9 * sgn, j * 1.3, n=5)


def tail_balam(k):
    seg_core(k, 0.9, "plate", "seg", 6, 0.9)
    collar(k.part("tail_seg", "band", k.trim()), -0.3, 1.15, 0.7, lip=0.2, sides=S(8), axis="-z")
    for i, a in enumerate((-30, 0, 30)):
        r = rad(a)
        tongue(k.part("tail_tip", f"flame_{i}", "crystal", layer=2, smooth=45), (0, 0, 0), (math.sin(r) * 0.6, 0.2, -1.0), 10.0 - 2.0 * (i != 1), 3.0, 0.5, 0.7, i * 1.4, plane="xy", n=5)


def halo_balam(k):
    for i, rot in enumerate((0, 60, 120)):                                                                   # three rings that interlock
        n0 = len(k.m.parts)
        du.tube_direct(k.part("halo", f"ring_{i}", k.trim(), layer=2, smooth=40), ring_xy(HR, 0.0, 12), 0.32, 0.32, 4)
        L.transform_since(k, n0, rot=("y", rot))


def back_balam(k):
    """BALAM_TRIAD: an arch of fire carries three great faces, the ram on the left, the man in the middle and above, the bull on the right, a flame between each."""
    arch = arc_pts((0.0, -6.0, -0.3), 14.0, 20, 160, 12)
    du.tube_direct(k.part("backdrop", "arch", "plate", smooth=40), arch, 0.9, 0.9, 5)
    for i, (a, w, mt) in enumerate(((158, 2.8, "bone"), (90, 3.6, "plate"), (22, 2.8, "bone"))):
        r = rad(a)
        c = (14.0 * math.cos(r), -6.0 + 14.0 * math.sin(r) + (0.0 if i != 1 else 1.0), 0.0)
        L.mask(k, "backdrop", f"face_{i}", c, w, 0.0, mt)
        ring_at(k, "backdrop", f"halo_{i}", k.trim(), (c[0], c[1], -0.2), w * 1.9, 0.3, "z", 8)
    for i, a in enumerate((124, 56, 90 + 2, 140, 40)):
        r = rad(a)
        tongue(k.part("backdrop", f"flame_{i}", "crystal", layer=2, smooth=45), (14.4 * math.cos(r), -6.0 + 14.4 * math.sin(r), 0.0), (math.cos(r), math.sin(r), 0), 4.8, 1.8, 0.4, 0.6, i, n=3)
    for sgn in (-1, 1):
        tapered_tube(k, "backdrop", f"leg_{sgn}", "plate", [(sgn * 14.0, -6.0 + 0.0, -0.3), (sgn * 14.0, -14.0, -0.3), (sgn * 12.0, -20.0, -0.3)], 0.7, 0.4, 5)


register("BALAM", head=head_balam, chest=chest_balam, pauldron=pauldron_balam, wings=wings_balam, tail=tail_balam, halo=halo_balam, back=back_balam)

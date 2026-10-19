"""djinn36_set4b: Alloces, Camio, Murmur (their weapons are in djinn36_set9)."""
from djinn36_lib import *                                     # noqa: F401,F403
import djinn36_lib as L
from djinn36_reg import register

HR = dub.HALO_R


def ring_xz(R_, y=0.0, n=12):
    return dub.ring_xz(R_, y, n)


def ring_xy(R_, z=0.0, n=14, a0=0, a1=360):
    return dub.ring_xy(R_, z, n, a0, a1)


# ============================================================================================ ALLOCES: the lion-eyed knight
def head_alloces(k):
    head_base(k, "open", nasal=False)
    du.box(k.part("helm", "brow", "plate"), (-3.8, 5.2, 3.9), (3.8, 8.0, 4.8), chamfer=0.5)                  # a brow plate engraved with two lion's eyes
    for sgn in (-1, 1):
        L.lens(k, "helm", f"eye_{sgn}", (sgn * 1.9, 6.6, 4.95), 1.3, 0.6)
    du.bar(k.part("helm", "bridge", k.trim()), (0, 5.2, 4.9), (0, 7.6, 4.9), 0.8, 0.3, up=(0, 0, 1), taper=0.5)
    for i, a in enumerate(np.linspace(-150, 150, 13)):                                                       # the mane: a ring of locks round the head, long behind
        r = rad(a)
        base = (math.sin(r) * 4.4, 3.4 + 1.4 * math.cos(r * 0.5), math.cos(r) * 4.4)
        out = (math.sin(r), 0.45, math.cos(r))
        Lh = 3.4 + 2.6 * (1 - math.cos(r)) * 0.5
        blade3(k.part("helm", f"lock_{i}", "cloth", smooth=45), base, out, (out[0] * 0.8, -0.2, out[2] * 0.8), Lh + 1.0, 1.7, 0.45, wdir=(0, 1, 0), n=3)
        if i % 2 == 0:
            tongue(k.part("helm", f"flame_{i}", "crystal", layer=2, smooth=45), (base[0] * 1.05, base[1] + 0.4, base[2] * 1.05), (out[0], 0.6, out[2]), Lh + 1.6, 1.2, 0.35, 0.4, i, plane="xy", n=3)
    for sgn, nm in ((1, "l"), (-1, "r")):                                                                    # round lion's ears
        orb(k.part("ears", "ear_" + nm, "cloth", smooth=0), (sgn * 3.4, 8.2, -0.4), 1.6, sides=6)
        orb(k.part("ears", "ear_in_" + nm, "crystal", layer=2, smooth=0), (sgn * 3.4, 8.3, 0.3), 0.9, sides=5)
    du.box(k.part("crest", "brow", "plate"), (-2.4, 5.4, 4.0), (2.4, 7.6, 4.8), chamfer=0.4)
    for sgn in (-1, 1):
        L.lens(k, "crest", f"eye_{sgn}", (sgn * 1.2, 6.5, 4.9), 0.9, 0.45)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def lion_face(k, piece, c, R_, spikes=12):
    cx, cy, cz = c
    extrude(k, piece, "lion_face", "trim", ngon(cx, cy, R_ * 0.55, 8), cz - 0.4, cz + 0.4)
    for i in range(spikes):
        a = 2 * PI * i / spikes
        spike(k.part(piece, f"mane_{i}", k.trim() if i % 2 else "plate"), (cx + math.cos(a) * R_ * 0.55, cy + math.sin(a) * R_ * 0.55, cz), (math.cos(a), math.sin(a), 0.1), R_ * (0.5 if i % 2 else 0.7), R_ * 0.16, 4)
    for sg in (-1, 1):
        gemz(k, piece, f"lion_eye_{sg}", (cx + sg * R_ * 0.2, cy + R_ * 0.1, cz + 0.5), R_ * 0.1, 1.0)


def chest_alloces(k):
    dua.chest_cuirass(k)
    lion_face(k, "chest", (0.0, -7.0, 3.1), 2.8, 10)


def pauldron_alloces(k):
    def fn(k, piece, sgn, cx, bx):
        for i in range(9):
            a = rad(-40 + 11 * i)
            spike(k.part(piece, f"mane_{i}", "cloth" if i % 2 else "plate"), (bx - sgn * 0.6 + math.sin(a) * 1.8, 2.8, math.cos(a) * 1.8 * 0.4), (sgn * 0.3 + math.sin(a) * 0.5, 1.0, math.sin(a) * 0.6), 4.8 - 0.5 * abs(i - 4), 0.8, 4)
        glow_orb(k, piece, "gem", (bx, 3.4, 2.8), 0.6, sides=5)
    L.pauldrons(k, fn, 4.8, "plate", 1)


def wings_alloces(k, sgn):
    """ALLOCES_MANE: the wing is a mane of fire: curling locks of cloth lie on the bone, each with a tongue of flame laid along it."""
    bone_wing(k, sgn, "extra", 1.2)
    sdp = sd(sgn)
    for j, (a, s, Ln, w) in enumerate(((-18, 0.2, 11.0, 3.0), (-50, 0.6, 10.0, 2.8), (-82, 0.9, 8.0, 2.4))):
        wf(k, "in", f"lock_{j}", sgn, s * INNER, a, Ln, w, 0.45, -34 if j % 2 else 30, "cloth", 5, pf_ribbon)
        tongue(k.part(f"wing_in_{sdp}", f"fire_{j}", "crystal", layer=2, smooth=45), (sgn * s * INNER, 0, 0.3), (sgn * math.cos(rad(a)), math.sin(rad(a)), 0), Ln * 0.9, 1.6, 0.4, 0.6 * sgn, j, n=4)
    for j, (a, s, Ln, w) in enumerate(((26, 0.0, 15.0, 3.2), (10, 0.06, 20.0, 3.6), (-6, 0.12, 24.0, 4.0), (-22, 0.2, 26.0, 4.0), (-38, 0.3, 24.0, 3.8), (-54, 0.42, 21.0, 3.6), (-70, 0.55, 17.0, 3.2), (-86, 0.7, 12.0, 2.6))):
        wf(k, "out", f"lock_{j}", sgn, s * OUTER, a, Ln, w, 0.45, 38 if j % 2 else -34, "cloth", 6, pf_ribbon)
        tongue(k.part(f"wing_out_{sdp}", f"fire_{j}", "crystal", layer=2, smooth=45), (sgn * s * OUTER, 0, 0.3), (sgn * math.cos(rad(a)), math.sin(rad(a)), 0), Ln * 0.95, 2.2, 0.4, 0.8 * sgn, j * 1.2, n=5)


def tail_alloces(k):
    seg_core(k, 0.9, "cloth", "seg", 6, 0.9)
    collar(k.part("tail_seg", "band", k.trim()), -0.3, 1.2, 0.7, lip=0.2, sides=S(8), axis="-z")
    for i in range(5):
        a = 2 * PI * i / 5
        blade3(k.part("tail_tip", f"lock_{i}", "cloth", smooth=45), (0, 0, 0), (math.cos(a) * 0.5, math.sin(a) * 0.5, -0.9), (math.cos(a) * 0.2, math.sin(a) * 0.2, -1.0), 7.0, 2.2, 0.4, wdir=(0, 0, 1), n=3)
        tongue(k.part("tail_tip", f"fire_{i}", "crystal", layer=2, smooth=45), (0, 0, -0.4), (math.cos(a) * 0.3, math.sin(a) * 0.3, -1.0), 9.0 - (i % 2) * 2.0, 2.2, 0.4, 0.7, i, plane="xy" if i % 2 else "yz", n=4)
    glow_orb(k, "tail_tip", "sun", (0, 0, -0.6), 0.9)


def halo_alloces(k):
    du.sweep(k.part("halo", "ring", k.trim(), layer=2), ring_xz(HR), C(0.34, S(5)))
    for i in range(12):
        a = 2 * PI * i / 12
        spike(k.part("halo", f"ray_{i}", "crystal", layer=2), (HR * math.cos(a), 0.0, HR * math.sin(a)), (math.cos(a), 0.12, math.sin(a)), 3.0 if i % 2 == 0 else 1.8, 0.4, 4)


def back_alloces(k):
    """ALLOCES_SUN: the sun of a lion's mane stands behind the shoulders: a disc of light in a ring of sixteen flame-rays, long and short, a second ring of locks beyond."""
    extrude(k, "backdrop", "disc", k.trim(), ngon(0, 0, 5.6, 10), -0.6, 0.6)
    extrude(k, "backdrop", "core", "crystal", ngon(0, 0, 4.2, 10), 0.6, 0.9, layer=2)
    L.mask(k, "backdrop", "face", (0.0, 0.0, 0.9), 1.9, 0.0, "plate")
    for i in range(12):
        a = 2 * PI * i / 12
        Ln = 8.4 if i % 2 == 0 else 5.0
        tongue(k.part("backdrop", f"flame_{i}", "crystal", layer=2, smooth=45), (6.2 * math.cos(a), 6.2 * math.sin(a), 0.0), (math.cos(a), math.sin(a), 0), Ln, 2.4, 0.5, 0.7, i * 0.9, n=4)
        spike(k.part("backdrop", f"ray_{i}", "plate"), (5.4 * math.cos(a), 5.4 * math.sin(a), -0.2), (math.cos(a), math.sin(a), 0), Ln * 0.8, 0.9, 4)
    du.tube_direct(k.part("backdrop", "ring", "plate", smooth=40), ring_xy(15.0, 0.0, 12), 0.5, 0.5, 4)


def mantle_alloces(k):
    cape(k, 24.0, 9.0, "cloth", 1.1, k.trim())
    for sgn in (-1, 1):
        for j in range(3):
            orb(k.part("mantle", f"mane_{sgn}{j}", "cloth", smooth=0), (sgn * (2.6 + 1.5 * j), 0.5 - 0.3 * j, 0.6 - 0.8 * j), 1.6 - 0.2 * j, sides=5)


register("ALLOCES", head=head_alloces, chest=chest_alloces, pauldron=pauldron_alloces, wings=wings_alloces, tail=tail_alloces, halo=halo_alloces, back=back_alloces, mantle=mantle_alloces)


# ============================================================================================ CAMIO: the songbird of the crows
def crow(k, piece, name, c, s=1.0, yaw=0.0, mat="plate", wings_up=0.4):
    """A crow in flight seen from behind / the front: a body, a head with a beak and an eye of light, two spread wings of feathers, a fan tail. Faces +z, then turned about y."""
    n0 = len(k.m.parts)
    orb(k.part(piece, name + "_b", mat, smooth=0), (0, 0, 0), 1.3 * s, sides=5)
    orb(k.part(piece, name + "_h", mat, smooth=0), (0, 0.8 * s, 1.5 * s), 0.8 * s, sides=5)
    spike(k.part(piece, name + "_beak", "bone"), (0, 0.7 * s, 2.1 * s), (0, -0.15, 1.0), 1.5 * s, 0.34 * s, 4)
    glow_orb(k, piece, name + "_eye", (0.4 * s, 1.0 * s, 1.9 * s), 0.2 * s, sides=4)
    for sg in (-1, 1):
        lf(k, piece, f"{name}_w{sg}", mat, [(sg * 0.8 * s, 0.2 * s, 0.2 * s), (sg * 3.4 * s, (0.6 + wings_up * 3) * s, -0.2 * s), (sg * 6.4 * s, (0.4 + wings_up * 2.4) * s, -1.4 * s), (sg * 5.0 * s, -0.4 * s, -1.8 * s), (sg * 2.0 * s, -0.8 * s, -1.0 * s)])
    spike(k.part(piece, name + "_t", mat, smooth=0), (0, -0.2 * s, -1.0 * s), (0, -0.1, -1.0), 2.4 * s, 0.9 * s, 4)
    if yaw:
        L.transform_since(k, n0, rot=("y", yaw))
    L.transform_since(k, n0, shift=c)


def head_camio(k):
    head_base(k, "dome", mat="cloth", cheeks=False, nasal=False)
    ml.loft(k.part("helm", "beak", "bone", smooth=40), (0.0, -0.35, 1.0), (0.0, 7.6, 4.2), [(0, 1.3, 1.1), (2.4, 1.0, 0.8), (5.0, 0.55, 0.45), (6.4, 0.0, 0.0)], C(1.0, S(8)), caps=(False, False))
    spike(k.part("helm", "beak_low", "bone"), (0.0, 7.2, 4.4), (0, -0.7, 1.0), 3.4, 0.7, 4)
    for sgn in (-1, 1):
        glow_orb(k, "helm", f"eye_{sgn}", (sgn * 2.5, 7.4, 4.3), 0.55, sides=5)
    for i, a in enumerate(np.linspace(-130, 130, 9)):                                                       # a ruff of black feathers
        r = rad(a)
        blade3(k.part("helm", f"ruff_{i}", "plate", smooth=45), (math.sin(r) * 4.5, 1.6, math.cos(r) * 4.5), (math.sin(r), -0.6, math.cos(r)), (math.sin(r) * 0.6, -1.0, math.cos(r) * 0.6), 4.8, 1.8, 0.4, wdir=(0, 1, 0), n=3)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        for j in range(2):
            blade3(k.part(piece, f"tuft_{j}", "plate", smooth=45), (sgn * 3.6, 7.4 - 0.6 * j, -0.8), (sgn * (0.3 + 0.3 * j), 1.0, -0.2), (sgn * (0.6 + 0.3 * j), 0.5, -0.8), 7.0 - 1.4 * j, 1.5, 0.4, wdir=(0, 0, 1), n=4)
    ml.loft(k.part("crest", "beak", "bone", smooth=40), (0.0, -0.3, 1.0), (0.0, 6.6, 4.2), [(0, 0.9, 0.8), (1.8, 0.7, 0.6), (3.6, 0.0, 0.0)], C(1.0, S(8)), caps=(False, False))
    for sgn in (-1, 1):
        glow_orb(k, "crest", f"eye_{sgn}", (sgn * 1.8, 6.4, 4.4), 0.4, sides=4)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_camio(k):
    dua.chest_harness(k)
    crow(k, "chest", "crow", (0.0, -9.8, 3.4), 0.7, 0.0)


def pauldron_camio(k):
    def fn(k, piece, sgn, cx, bx):
        fan(k, piece, "fea", (bx - sgn * 0.8, 2.4, -0.6), (84, 64, 44, 24), (7.0, 7.8, 7.0, 5.6), 1.7, "plate", -22, 3, sgn, bz=0.3)
        spike(k.part(piece, "beak", "bone"), (bx + sgn * 0.2, 3.0, 2.6), (sgn * 0.2, -0.2, 1.0), 2.8, 0.6, 4)
        glow_orb(k, piece, "eye", (bx + sgn * 0.8, 3.8, 2.4), 0.35, sides=4)
    L.pauldrons(k, fn, 4.5, "plate", 1)


def wings_camio(k, sgn):
    """CAMIO_CORVID: a crow's wing: eight primaries spread like fingers, their tips curling up, a double row of coverts on the arm, a glint down each shaft."""
    bone_wing(k, sgn, "plate", 1.1)
    for j, (a, s, Ln) in enumerate(((-16, 0.15, 9.0), (-42, 0.5, 9.5), (-70, 0.82, 8.0))):
        wf(k, "in", f"cov{j}", sgn, s * INNER, a, Ln, 2.6, 0.5, -12, "plate", 3, pf_feather)
    for j, (a, s, Ln) in enumerate(((30, 0.0, 14.0), (16, 0.05, 19.0), (4, 0.1, 23.0), (-9, 0.17, 25.0), (-22, 0.26, 24.0), (-35, 0.36, 21.0), (-48, 0.48, 17.0), (-62, 0.6, 13.0))):
        wf(k, "out", f"pri{j}", sgn, s * OUTER, a, Ln, 2.0, 0.5, 26, "plate", 5, pf_blade)
        if j % 2 == 0:
            wcore(k, "out", f"core{j}", sgn, s * OUTER, a, Ln, 2.0, 26, 4)
    for j in range(4):
        wf(k, "out", f"cov{j}", sgn, (0.1 + 0.15 * j) * OUTER, -20 - 12 * j, 8.0, 3.0, 0.5, -12, "cloth", 3, pf_feather, bz=0.4)


def tail_camio(k):
    ml.loft(k.part("tail_seg", "quill", "trim"), "-z", (0, 0, 0), [(0, 0.5, 0.5), (6, 0.45, 0.45)], C(1.0, S(6)), caps=(False, False))
    for i, a in enumerate((-22, 0, 22)):
        r = rad(a)
        sweep_flat(k.part("tail_seg", f"fea_{i}", "plate", smooth=45), [(0, 0, -0.4), (math.sin(r) * 2.0, 0, -3.2), (math.sin(r) * 3.6, 0, -5.8)], 1.6, 0.3, pf_feather, (0, 1, 0))
    for i, a in enumerate((-36, -18, 0, 18, 36)):
        r = rad(a)
        sweep_flat(k.part("tail_tip", f"fan_{i}", "plate", smooth=45), [(0, 0, 0), (math.sin(r) * 2.4, 0.2, -4.2), (math.sin(r) * 5.0, 0, -9.0 - 3.0 * (1 - abs(a) / 40))], 2.0, 0.3, pf_blade, (0, 1, 0))


def halo_camio(k):
    du.sweep(k.part("halo", "ring", "plate"), ring_xz(HR), C(0.28, S(5)))
    for i in range(5):
        a = 2 * PI * i / 5
        n0 = len(k.m.parts)
        crow(k, "halo", f"crow_{i}", (0, 0, 0), 0.55, 0.0)
        L.transform_since(k, n0, rot=("y", 90.0 - math.degrees(a) + 90.0))
        L.transform_since(k, n0, shift=(HR * math.cos(a), 0.6, HR * math.sin(a)))


def back_camio(k):
    """CAMIO_MURDER: a murder of crows circles behind the shoulders: seven black birds round a small ring, notes of light between them, a feather falling from each."""
    du.tube_direct(k.part("backdrop", "ring", "plate", smooth=40), ring_xy(5.0, 0.0, 10), 0.3, 0.3, 4)
    glow_orb(k, "backdrop", "song", (0, 0, 0.4), 1.2)
    for i in range(7):
        a = rad(90 + 360 / 7 * i)
        R_ = 11.0 + 2.0 * (i % 2)
        c = (R_ * math.cos(a), R_ * math.sin(a), 0.0)
        crow(k, "backdrop", f"crow_{i}", c, 1.0 + 0.2 * (i % 3 == 0), 0.0, "plate", 0.5)
        glow_orb(k, "backdrop", f"note_{i}", (R_ * 0.62 * math.cos(a + 0.28), R_ * 0.62 * math.sin(a + 0.28), 0.4), 0.4, sides=4)
        lf(k, "backdrop", f"feather_{i}", "plate", [(c[0], c[1] - 3.0, 0.0), (c[0] + 0.8, c[1] - 5.0, 0.0), (c[0], c[1] - 8.0, 0.0), (c[0] - 0.8, c[1] - 5.0, 0.0)])


def mantle_camio(k):
    cape(k, 18.0, 8.0, "cloth", 1.0, "plate")
    for sgn in (-1, 1):
        fan(k, "mantle", f"ruff{sgn}_", (sgn * 3.4, 0.4, -1.0), (70, 50, 30, 10), (4.4, 5.0, 5.0, 4.4), 1.6, "plate", -12, 3, sgn, bz=0.3)


register("CAMIO", head=head_camio, chest=chest_camio, pauldron=pauldron_camio, wings=wings_camio, tail=tail_camio, halo=halo_camio, back=back_camio, mantle=mantle_camio)


# ============================================================================================ MURMUR: the death drummer
def head_murmur(k):
    head_base(k, "visor", nasal=False)
    du.box(k.part("helm", "skull", "bone"), (-3.8, 5.0, 4.0), (3.8, 7.6, 5.0), chamfer=0.8)
    for sgn in (-1, 1):
        glow_orb(k, "helm", f"socket_{sgn}", (sgn * 1.9, 6.4, 5.1), 0.7, sides=5)
    for i, x in enumerate((-2.7, -1.35, 0.0, 1.35, 2.7)):
        spike(k.part("helm", f"tooth_{i}", "bone"), (x, 5.1, 4.9), (0, -1.0, 0.1), 1.3, 0.36, 4)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # a drumstick stands from each temple, a ball on its head
        tapered_tube(k, piece, "stick", "bone", [(sgn * 3.4, 6.0, 0.0), (sgn * 5.0, 10.0, -0.4), (sgn * 6.4, 14.0, -0.8)], 0.5, 0.36, 5)
        orb(k.part(piece, "ball", "plate", smooth=0), (sgn * 6.6, 14.8, -0.9), 1.5, sides=6)
        glow_orb(k, piece, "ball_gem", (sgn * 6.6, 14.8, -0.2), 0.6, sides=4)
    for i, x in enumerate((-2.8, -1.0, 1.0, 2.8)):                                                          # dreadlocks hung with bones
        ribbon(k, "helm", f"lock_{i}", "cloth", [(x, 7.0, -3.8), (x * 1.3, 3.0, -5.4), (x * 1.5, -2.0, -5.8), (x * 1.2, -7.0 - (i % 2) * 2.0, -5.2)], 1.5, 0.4, 0.4)
        orb(k.part("helm", f"bone_{i}", "bone", smooth=0), (x * 1.4, -1.0, -5.9), 0.6, sides=5)
    du.box(k.part("crest", "skull", "bone"), (-2.8, 5.2, 4.0), (2.8, 7.0, 4.8), chamfer=0.6)
    for sgn in (-1, 1):
        glow_orb(k, "crest", f"socket_{sgn}", (sgn * 1.4, 6.2, 4.9), 0.5, sides=4)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_murmur(k):
    dua.chest_cuirass(k)
    ml.lathe(k.part("chest", "drum", "plate", smooth=40), "z", (0.0, -7.0, 3.0), [(-0.3, 2.3), (0.4, 2.45), (1.0, 2.3)], sides=S(12), caps=(True, False))
    ml.lathe(k.part("chest", "drum_skin", "crystal", layer=2), "z", (0.0, -7.0, 3.9), [(0.0, 2.0), (0.15, 1.8), (0.2, 0.0)], sides=S(12), caps=(True, False))
    for sg in (-1, 1):
        du.bar(k.part("chest", f"stick_{sg}", "bone"), (sg * 2.6, -4.6, 4.2), (-sg * 2.2, -9.4, 4.2), 0.4, 0.3, up=(0, 0, 1))


def pauldron_murmur(k):
    def fn(k, piece, sgn, cx, bx):
        ml.lathe(k.part(piece, "drum", "plate", smooth=40), "z", (bx - sgn * 0.6, 3.6, 0.8), [(-0.8, 2.6), (0.0, 2.8), (0.8, 2.6)], sides=S(12), caps=(True, False))
        ml.lathe(k.part(piece, "skin", "bone"), "z", (bx - sgn * 0.6, 3.6, 1.6), [(0.0, 2.4), (0.2, 2.0), (0.25, 0.0)], sides=S(12), caps=(True, False))
        glow_orb(k, piece, "skin_gem", (bx - sgn * 0.6, 3.6, 2.0), 0.7, sides=4)
        spike(k.part(piece, "bone", "bone"), (bx + sgn * 0.8, 2.4, -1.6), (sgn * 0.6, 0.9, -0.3), 4.4, 0.8, 4)
    L.pauldrons(k, fn, 4.6, "extra", 1)


def wings_murmur(k, sgn):
    """MURMUR_SKINS: the wing is ribs of bone with drumskins stretched across, three small drums hung on cords between them, each skin ringed with light."""
    bone_wing(k, sgn, "bone", 1.3)
    sdp = sd(sgn)
    ribs = []
    for j, (a, Ln) in enumerate(((28, 15.0), (6, 22.0), (-18, 22.0), (-42, 16.0))):
        path = arc_path((0, 0, 0), a, Ln, -10, 3, "xy", sgn)
        du.tube_direct(k.part(f"wing_out_{sdp}", f"rib_{j}", "bone", smooth=40), path, 0.5, 0.14, 4)
        ribs.append(path)
    for j in range(3):
        a, b = np.array(ribs[j][2]), np.array(ribs[j + 1][2])
        c = (a + b) / 2
        r = float(np.linalg.norm(a - b)) * 0.62
        extrude(k, f"wing_out_{sdp}", f"skin_{j}", "bone", ngon(c[0], c[1], r, 8), -0.4, 0.2, smooth=0)
        du.tube_direct(k.part(f"wing_out_{sdp}", f"hoop_{j}", "crystal", layer=2, smooth=40), ring_path((c[0], c[1], 0.3), r, 0, 360, "xy", 8), 0.22, 0.22, 4)
        orb(k.part(f"wing_out_{sdp}", f"stud_{j}", "plate", smooth=0), (c[0], c[1], 0.35), r * 0.22, sides=4)
    for q, a in enumerate((-22, -56)):
        path = arc_path((0, 0, 0), a, 9.0, -8, 2, "xy", sgn)
        du.tube_direct(k.part(f"wing_in_{sdp}", f"rib_{q}", "bone", smooth=40), path, 0.4, 0.12, 4)
    extrude(k, f"wing_in_{sdp}", "skin", "bone", ngon(sgn * 4.6, -3.0, 3.2, 8), -0.4, 0.2, smooth=0)
    du.tube_direct(k.part(f"wing_in_{sdp}", "hoop", "crystal", layer=2, smooth=40), ring_path((sgn * 4.6, -3.0, 0.3), 3.2, 0, 360, "xy", 8), 0.2, 0.2, 4)


def tail_murmur(k):
    seg_core(k, 0.55, "trim", "cord", 5, 0.9)
    for j in range(2):
        orb(k.part("tail_seg", f"bone_{j}", "bone", smooth=0), (0, 0, -1.6 - 2.4 * j), 0.6, sides=5)
    ml.loft(k.part("tail_tip", "stem", "bone"), "-z", (0, 0, 0), [(0, 0.5, 0.5), (6, 0.4, 0.4)], C(1.0, S(6)), caps=(False, False))
    orb(k.part("tail_tip", "ball", "plate", smooth=40), (0, 0, -7.0), 2.4, sides=6)
    glow_orb(k, "tail_tip", "ball_gem", (0, 0, -7.0), 1.4, sides=5)


def halo_murmur(k):
    du.sweep(k.part("halo", "ring", "bone"), ring_xz(HR), C(0.3, S(5)))
    for i in range(6):
        a = 2 * PI * i / 6
        c = (HR * math.cos(a), 0.4, HR * math.sin(a))
        orb(k.part("halo", f"skull_{i}", "bone", smooth=0), c, 0.8, sides=5)
        glow_orb(k, "halo", f"eye_{i}", (c[0] + math.cos(a) * 0.5, c[1] + 0.2, c[2] + math.sin(a) * 0.5), 0.26, sides=4)
        spike(k.part("halo", f"stick_{i}", "bone"), c, (0, 1.0, 0), 3.0, 0.2, 4)


def back_murmur(k):
    """MURMUR_BASS: the great bass drum of the dead hangs behind the shoulders: a shell ringed with rods, a skin with a skull and rings of sound upon it, two small drums beside, two mallets crossed behind."""
    for sg in (-1, 1):
        n0 = len(k.m.parts)
        du.bar(k.part("backdrop", f"mallet_{sg}", "bone"), (0, -16.0, -1.6), (0, 14.0, -1.6), 0.9, 0.5, up=(0, 0, 1))
        orb(k.part("backdrop", f"mallet_head_{sg}", "plate", smooth=0), (0, 15.2, -1.6), 2.0, sides=6)
        L.transform_since(k, n0, rot=("z", sg * 32))
    L.drum(k, "backdrop", "bass", (0.0, 0.0, 0.0), 9.5, 4.0, "z", "plate", "bone", "trim", 6)
    for sg in (-1, 1):
        L.drum(k, "backdrop", f"side_{sg}", (sg * 13.0, -4.0, 0.0), 3.6, 2.6, "z", "plate", "bone", "trim", 0)
    L.mask(k, "backdrop", "skull", (0.0, 0.0, 2.6), 2.6, 0.0, "bone")
    for i, r_ in enumerate((5.8, 7.8)):
        du.tube_direct(k.part("backdrop", f"wave_{i}", "crystal", layer=2, smooth=40), ring_xy(r_, 2.5, 10), 0.18, 0.18, 4)


def mantle_murmur(k):
    cape(k, 22.0, 8.0, "cloth", 1.0, k.trim())
    jag_hem(k, "mantle", "rag", "cloth", -22.2, -4.6, 5.2, 5.0, 6, 0.6)
    for i in range(3):
        orb(k.part("mantle", f"bone_{i}", "bone", smooth=0), (-3.0 + 3.0 * i, -3.4 - 0.6 * (i % 2), -2.8), 0.7, sides=5)


register("MURMUR", head=head_murmur, chest=chest_murmur, pauldron=pauldron_murmur, wings=wings_murmur, tail=tail_murmur, halo=halo_murmur, back=back_murmur, mantle=mantle_murmur)

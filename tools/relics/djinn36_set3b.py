"""djinn36_set3b: Andromalius, Glasya-Labolas, Haagenti."""
from djinn36_lib import *                                     # noqa: F401,F403
import djinn36_lib as L
from djinn36_reg import register

HR = dub.HALO_R


def ring_xz(R_, y=0.0, n=12):
    return dub.ring_xz(R_, y, n)


def ring_xy(R_, z=0.0, n=14, a0=0, a1=360):
    return dub.ring_xy(R_, z, n, a0, a1)


def coil(c, r, y0, y1, turns, n=12, a0=0.0):
    """A helix about the vertical axis through c: points from y0 to y1."""
    return [(c[0] + r * math.sin(a0 + 2 * PI * turns * t), y0 + (y1 - y0) * t, c[2] + r * math.cos(a0 + 2 * PI * turns * t)) for t in np.linspace(0, 1, n)]


def snake_head(k, piece, name, c, d=(0, 0, 1), r=0.9, mat="plate"):
    """A snake's head at c looking along d: a skull, two glowing eyes, two fangs."""
    d = unit(d)
    side = unit(np.cross(d, (0, 1.0, 0))) if abs(d[1]) < 0.9 else np.array([1.0, 0, 0])
    orb(k.part(piece, name + "_h", mat, smooth=0), tuple(c), r, sides=5)
    for sg in (-1, 1):
        glow_orb(k, piece, f"{name}_e{sg}", tuple(np.asarray(c) + d * r * 0.5 + side * sg * r * 0.55 + (0, r * 0.4, 0)), r * 0.28, sides=4)
        spike(k.part(piece, f"{name}_f{sg}", "bone"), tuple(np.asarray(c) + d * r * 0.8 + side * sg * r * 0.3 - (0, r * 0.3, 0)), tuple(d * 0.4 + (0, -1.0, 0)), r * 1.2, r * 0.18, 4)


# ============================================================================================ ANDROMALIUS: the seeker of hidden things
def head_andromalius(k):
    head_base(k, "cowl", mat="cloth", cheeks=False, nasal=False)
    ml.lathe(k.part("helm", "peak", "cloth"), "y", (0.0, 9.2, -0.8), [(0.0, 3.2), (2.4, 1.6), (5.6, 0.0)], sides=S(10), caps=(False, False))
    almond(k, "helm", "third", (0.0, 7.0, 4.75), 2.0, 1.1, "trim", 0.28)
    pts = coil((0, 0, 0), 5.0, 2.4, 8.4, 1.5, 14, -2 * PI * 1.5)
    du.tube_direct(k.part("helm", "serpent", "plate", smooth=40), pts, 0.7, 0.5, 5)
    snake_head(k, "helm", "snake", np.array(pts[-1]) + (0, 0.2, 0.6), (0, 0, 1), 1.0)
    for sgn, nm in ((1, "l"), (-1, "r")):
        du.bar(k.part("helm", "fold_" + nm, "cloth"), (sgn * 4.3, 7.6, 3.4), (sgn * 4.6, 2.0, 1.0), 1.2, 0.25, up=(sgn, 0, 0), section="rect", taper=0.4)
    almond(k, "crest", "eye", (0.0, 6.6, 4.6), 1.5, 0.8, "trim", 0.24)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_andromalius(k):
    dua.chest_harness(k)
    almond(k, "chest", "eye", (0.0, -9.8, 3.15), 3.0, 1.6, "trim", 0.34, True, 5)


def pauldron_andromalius(k):
    def fn(k, piece, sgn, cx, bx):
        pts = coil((bx - sgn * 0.6, 0, 0), 2.6, 2.0, 6.4, 1.6, 12, 0.0)
        du.tube_direct(k.part(piece, "serpent", "plate", smooth=40), pts, 0.62, 0.42, 4)
        snake_head(k, piece, "snake", np.array(pts[-1]) + (0, 0.2, 0.8), (sgn * 0.5, 0.2, 1.0), 0.85)
        glow_orb(k, piece, "spark", (bx + sgn * 0.8, 3.6, -1.6), 0.4, sides=4)
    L.pauldrons(k, fn, 4.4, "plate", 1)


def wings_andromalius(k, sgn):
    """ANDROMALIUS_LATTICE: the wing is a lattice of thin bone, long ribs crossed by two bands of web, a watching eye at the crossings."""
    bone_wing(k, sgn, "extra", 1.15)
    sdp = sd(sgn)
    ribs = []
    for j, (a, Ln) in enumerate(((26, 16.0), (4, 23.0), (-18, 23.0), (-42, 17.0))):
        path = arc_path((0, 0, 0), a, Ln, -14, 4, "xy", sgn)
        du.tube_direct(k.part(f"wing_out_{sdp}", f"rib_{j}", "extra", smooth=40), path, 0.5, 0.12, 4)
        ribs.append(path)
    for q, f in enumerate((2, 4)):
        pts = [tuple(np.array(r[f]) * (1.0) + (0, 0, 0.05)) for r in ribs]
        du.tube_direct(k.part(f"wing_out_{sdp}", f"band_{q}", "trim", smooth=40), pts, 0.28, 0.28, 4)
    for j, r in enumerate(ribs[:3]):
        e = np.array(r[3]) * 0.5 + np.array(ribs[j + 1][3]) * 0.5
        orb(k.part(f"wing_out_{sdp}", f"eye_o_{j}", "plate", smooth=0), tuple(e + (0, 0, 0.2)), 1.2, sides=5)
        glow_orb(k, f"wing_out_{sdp}", f"eye_{j}", tuple(e + (0, 0, 0.8)), 0.6, sides=4)
    for q, a in enumerate((-20, -52)):
        path = arc_path((0, 0, 0), a, 9.0, -10, 3, "xy", sgn)
        du.tube_direct(k.part(f"wing_in_{sdp}", f"rib_{q}", "extra", smooth=40), path, 0.4, 0.12, 4)
    du.tube_direct(k.part(f"wing_in_{sdp}", "band", "trim", smooth=40), [(sgn * 3.6, -1.4, 0), (sgn * 4.4, -2.8, 0), (sgn * 3.2, -5.0, 0)], 0.26, 0.26, 4)
    lf(k, f"wing_in_{sdp}", "web", "cloth", [(0, 0, -0.1), (sgn * 6.0, -2.4, -0.1), (sgn * 3.6, -6.6, -0.1)])


def tail_andromalius(k):
    seg_core(k, 1.0, "plate", "seg", 7, 0.82)
    du.bar(k.part("tail_seg", "belly", "bone"), (0, -0.9, 0), (0, -0.9, -6.0), 0.9, 0.2, up=(0, -1, 0), taper=1.0)
    spike(k.part("tail_seg", "dorsal", k.trim()), (0, 1.0, -1.0), (0, 1.0, -0.4), 1.5, 0.4, 4)
    ml.loft(k.part("tail_tip", "stem", "plate"), "-z", (0, 0, 0), [(0, 0.8, 0.8), (3.0, 1.4, 1.2)], C(1.0, S(8)), caps=(False, False))
    snake_head(k, "tail_tip", "snake", (0, 0.2, -4.4), (0, 0, -1.0), 1.5)
    for sg in (-1, 1):
        spike(k.part("tail_tip", f"tongue_{sg}", "crystal", layer=2), (0, -0.4, -6.0), (sg * 0.4, -0.2, -1.0), 3.0, 0.14, 4)


def halo_andromalius(k):
    du.sweep(k.part("halo", "ring", "plate"), ring_xz(HR), C(0.28, S(5)))
    for i in range(6):
        a = 2 * PI * i / 6
        n0 = len(k.m.parts)
        almond(k, "halo", f"eye_{i}", (0, 0, 0), 1.1, 0.6, "trim", 0.16, True)
        L.transform_since(k, n0, rot=("y", 90.0 - math.degrees(a)))
        L.transform_since(k, n0, shift=(HR * math.cos(a), 1.2, HR * math.sin(a)))


def back_andromalius(k):
    """ANDROMALIUS_EYE: one great eye stands behind the shoulders, lids of dark metal, an iris of light, fourteen rays of seeing round it, a ring of small eyes beyond."""
    almond(k, "backdrop", "eye", (0.0, 1.0, 0.0), 11.0, 6.0, "plate", 0.9, True, 0)
    du.tube_direct(k.part("backdrop", "lid_in", k.trim(), smooth=40), [(11.0 * u * 0.92, 1.0 + 6.0 * 0.9 * (1 - u * u), 0.4) for u in np.linspace(-1, 1, 7)], 0.34, 0.34, 4)
    for i in range(14):
        a = rad(10 + 160 * i / 13)
        spike(k.part("backdrop", f"ray_{i}", "crystal", layer=2), (13.0 * math.cos(a), 1.0 + 8.5 * math.sin(a), 0.0), (math.cos(a), math.sin(a), 0), 3.4 if i % 2 == 0 else 2.0, 0.34, 4)
    for i in range(8):
        a = rad(200 + 140 * i / 7)
        orb(k.part("backdrop", f"small_{i}", "plate", smooth=0), (12.0 * math.cos(a), 1.0 + 8.0 * math.sin(a), 0.0), 0.8, sides=5)
        glow_orb(k, "backdrop", f"small_gem_{i}", (12.0 * math.cos(a), 1.0 + 8.0 * math.sin(a), 0.6), 0.42, sides=4)


def mantle_andromalius(k):
    cape(k, 24.0, 9.0, "cloth", 1.0, k.trim())
    for sgn in (-1, 1):
        glow_orb(k, "mantle", f"clasp_{sgn}", (sgn * 3.4, 0.2, 2.0), 0.5, sides=4)


register("ANDROMALIUS", head=head_andromalius, chest=chest_andromalius, pauldron=pauldron_andromalius, wings=wings_andromalius, tail=tail_andromalius, halo=halo_andromalius,
         back=back_andromalius, mantle=mantle_andromalius)


# ============================================================================================ GLASYA_LABOLAS: the hound of slaughter
def head_glasya(k):
    head_base(k, "open", nasal=False)
    du.box(k.part("helm", "snout", "plate"), (-2.3, 4.7, 4.0), (2.3, 7.8, 6.8), chamfer=0.7)                 # the hound's skull lies over the brow
    orb(k.part("helm", "nose", "dark", smooth=0), (0.0, 7.2, 6.9), 0.8, sides=5)
    for i, x in enumerate((-1.8, -0.6, 0.6, 1.8)):
        spike(k.part("helm", f"fang_{i}", "bone"), (x, 4.8, 6.2), (0, -1.0, 0.2), 1.9 - 0.2 * (i % 3 == 1) - 0.2 * (i % 3 == 2), 0.38, 4)
    for sgn in (-1, 1):
        glow_orb(k, "helm", f"eye_{sgn}", (sgn * 2.7, 7.6, 4.2), 0.55, sides=5)
    for i in range(10):                                                                                      # a collar of spikes round the neck
        a = 2 * PI * i / 10
        spike(k.part("helm", f"collar_{i}", k.trim()), (4.6 * math.sin(a), 0.6, 4.6 * math.cos(a)), (math.sin(a), 0.1, math.cos(a)), 1.8, 0.42, 4)
    collar(k.part("helm", "collar_band", "plate"), 0.2, 4.7, 0.8, lip=0.2, sides=S(12))
    for sgn, nm in ((1, "l"), (-1, "r")):                                                                    # ears: up, then folded over
        blade3(k.part("ears", "ear_" + nm, "plate", smooth=45), (sgn * 3.4, 8.0, -0.4), (sgn * 0.5, 1.0, 0.0), (sgn * 1.0, -0.7, 0.0), 6.6, 3.0, 0.5, wdir=(0, 0, 1), n=5)
        blade3(k.part("ears", "ear_in_" + nm, "crystal", layer=2, smooth=45), (sgn * 3.5, 8.1, 0.0), (sgn * 0.5, 1.0, 0.0), (sgn * 1.0, -0.7, 0.0), 4.4, 1.4, 0.3, wdir=(0, 0, 1), n=4)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        blade3(k.part(piece, "blade", "plate", smooth=45), (sgn * 3.4, 6.4, -0.8), (sgn * 0.4, 0.7, -0.6), (sgn * 0.2, 0.1, -1.0), 11.0, 2.4, 0.5, wdir=(1, 0, 0), n=5)
    du.box(k.part("crest", "snout", "plate"), (-1.6, 5.0, 4.1), (1.6, 7.4, 5.6), chamfer=0.5)
    for i, x in enumerate((-0.8, 0.8)):
        spike(k.part("crest", f"fang_{i}", "bone"), (x, 5.1, 5.2), (0, -1.0, 0.1), 1.4, 0.3, 4)
    glow_orb(k, "crest", "eye", (0, 7.4, 4.6), 0.4, sides=4)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_glasya(k):
    dua.chest_cuirass(k)
    for i in range(3):
        du.bar(k.part("chest", f"slash_{i}", "crystal", layer=2), (-2.6 + 1.7 * i, -3.6, 3.1), (-0.6 + 1.7 * i, -10.2, 3.1), 0.7, 0.25, up=(0, 0, 1), taper=2.4)


def pauldron_glasya(k):
    def fn(k, piece, sgn, cx, bx):
        for j, (dx, dy, Ln) in enumerate(((0.8, 1.0, 6.4), (1.6, 0.6, 5.2), (0.2, 1.0, 4.6), (2.2, 0.1, 3.8))):
            spike(k.part(piece, f"spike_{j}", "plate"), (bx - sgn * 0.6 + sgn * 0.5 * j, 2.4, -1.6 + 1.0 * j), (sgn * dx, dy, 0), Ln, 0.8, 4)
        sweep_tube(k.part(piece, "chain", "trim", smooth=40), [(bx + sgn * 1.0, 0.4, 2.4), (bx + sgn * 2.6, -2.4, 2.8), (bx + sgn * 2.0, -5.6, 2.2)], 0.3, 0.3, 4)
        glow_orb(k, piece, "gem", (bx, 3.4, 2.4), 0.5, sides=4)
    L.pauldrons(k, fn, 4.8, "extra", 1)


def wings_glasya(k, sgn):
    """GLASYA_BLADES: the wing is a fan of curved blades on bone fingers, each edged with teeth along its under side and a glowing line on its back."""
    bone_wing(k, sgn, "plate", 1.3)
    sdp = sd(sgn)
    for j, (a, s, Ln, w) in enumerate(((-20, 0.2, 10.0, 2.6), (-52, 0.6, 9.0, 2.4), (-84, 0.9, 7.0, 2.0))):
        wf(k, "in", f"blade_{j}", sgn, s * INNER, a, Ln, w, 0.6, -18, "plate", 3, pf_blade)
    for j, (a, s, Ln, w) in enumerate(((26, 0.0, 15.0, 3.0), (6, 0.08, 21.0, 3.6), (-14, 0.18, 25.0, 3.8), (-34, 0.3, 23.0, 3.6), (-54, 0.44, 19.0, 3.2), (-74, 0.6, 13.0, 2.6))):
        wf(k, "out", f"blade_{j}", sgn, s * OUTER, a, Ln, w, 0.6, -24, "plate", 4, pf_blade)
        wcore(k, "out", f"core_{j}", sgn, s * OUTER, a, Ln, w, -24, 3)
        for q in range(3):
            t = 0.35 + 0.22 * q
            ang = rad(a - 24 * t)
            base = np.array([sgn * (s * OUTER + math.cos(rad(a)) * Ln * t * 0.9), math.sin(rad(a)) * Ln * t * 0.9, 0.0])
            spike(k.part(f"wing_out_{sdp}", f"tooth_{j}{q}", "bone"), tuple(base), (sgn * math.cos(ang) * 0.3, -1.0, 0), 2.2, 0.4, 4)


def tail_glasya(k):
    seg_core(k, 1.2, "plate", "seg", 6, 0.85)
    for sg in (-1, 1):
        spike(k.part("tail_seg", f"spur_{sg}", k.trim()), (sg * 1.0, 0.5, -1.0), (sg * 1.0, 0.3, -0.6), 2.0, 0.5, 4)
    path = [(0, 0, 0), (0, 0.6, -4.0), (0, 1.0, -9.0), (0, 0.4, -14.0)]
    sweep_flat(k.part("tail_tip", "blade", "plate", smooth=45), curve(path, 4), 5.0, 0.9, lambda s: (0.5 + 0.5 * math.sin(min(1, s * 1.7) * PI / 2)) * (1 - s ** 3) + 0.04, (1, 0, 0))
    for i in range(4):
        spike(k.part("tail_tip", f"tooth_{i}", "bone"), (0, 0.6 + 0.0, -3.0 - 2.4 * i), (0, 1.0, -0.2), 1.8, 0.4, 4)
    sweep_flat(k.part("tail_tip", "edge", "crystal", layer=2), curve([(0.0, 1.0, z) for z in (-2.0, -6.0, -10.0, -13.0)], 3), 0.6, 0.3, pf_taper, (1, 0, 0))


def back_glasya(k):
    """GLASYA_MAW: a ring of fangs, the open maw of the hound, round the shoulders, three great claw-slashes of red light torn across it."""
    R_ = 11.0
    du.tube_direct(k.part("backdrop", "ring", "plate", smooth=40), ring_xy(R_, 0.0, 14), 1.0, 1.0, 5)
    for i in range(12):
        a = 2 * PI * i / 12
        spike(k.part("backdrop", f"fang_{i}", "bone"), (R_ * math.cos(a) * 0.94, R_ * math.sin(a) * 0.94, 0), (-math.cos(a), -math.sin(a), 0), 4.4 if i % 2 == 0 else 2.8, 0.8, 4)
        spike(k.part("backdrop", f"barb_{i}", "plate"), (R_ * math.cos(a), R_ * math.sin(a), 0), (math.cos(a), math.sin(a), 0), 2.0, 0.5, 4)
    for i in range(3):
        x0 = -7.0 + 5.6 * i
        sl = [(x0, 11.0 - 1.0 * i, 0.9), (x0 + 3.0, 4.0, 0.9), (x0 + 6.0, -4.0, 0.9), (x0 + 8.4, -12.0 + i, 0.9)]
        sweep_flat(k.part("backdrop", f"slash_{i}", "crystal", layer=2), curve(sl, 4), 2.2, 0.4, lambda s: math.sin(math.pi * min(1.0, s * 1.02)) ** 0.8, (0, 0, 1))


register("GLASYA_LABOLAS", head=head_glasya, chest=chest_glasya, pauldron=pauldron_glasya, wings=wings_glasya, tail=tail_glasya, back=back_glasya)


# ============================================================================================ HAAGENTI: the alchemist of the winged bull
def goggles(k, piece, y, z, r=1.5, size=1.0, strap=True):
    for sgn in (-1, 1):
        ring_at(k, piece, f"gog_{sgn}", "trim", (sgn * 2.0, y, z), r, 0.3 * size, "z", 8)
        disc(k.part(piece, f"lens_{sgn}", "crystal", layer=2), (sgn * 2.0, y, z - 0.2), r * 0.8, 0.3, "z", 8)
    du.bar(k.part(piece, "bridge", k.trim()), (-0.6, y, z), (0.6, y, z), 0.5, 0.3, up=(0, 0, 1))
    if strap:
        du.bar(k.part(piece, "strap", "cloth"), (-4.2, y, z - 0.6), (4.2, y, z - 0.6), 0.9, 0.2, up=(0, 0, 1), section="rect")
        for sgn in (-1, 1):
            du.bar(k.part(piece, f"strap_s{sgn}", "cloth"), (sgn * 4.2, y, z - 0.6), (sgn * 4.2, y, -3.6), 0.9, 0.2, up=(sgn, 0, 0), section="rect")


def head_haagenti(k):
    head_base(k, "visor", nasal=False)
    goggles(k, "helm", 6.6, 4.4)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):                                                      # the bull's horns, heavy and forward-curving, capped in bronze
        path = [(sgn * 3.4, 5.8, 0.6), (sgn * 6.0, 6.2, 1.8), (sgn * 7.6, 8.4, 3.4), (sgn * 7.2, 11.4, 5.0)]
        tapered_tube(k, piece, "horn", "bone", path, 1.7, 0.2, 7)
        collar(k.part(piece, "cap", k.trim()), 0.0, 1.9, 0.8, lip=0.2, sides=S(10), axis=(sgn * 1.0, 0.2, 0.4), origin=(sgn * 3.6, 5.9, 0.7))
    for sgn, nm in ((1, "l"), (-1, "r")):
        blade3(k.part("ears", "ear_" + nm, "cloth", smooth=45), (sgn * 4.2, 4.6, -0.6), (sgn * 1.0, 0.25, -0.3), (sgn * 1.0, 0.0, -0.5), 5.0, 2.4, 0.5, wdir=(0, 0, 1), n=3)
    ring_at(k, "helm", "nose_ring", k.trim(), (0.0, 1.0, 4.9), 0.9, 0.2, "z", 6)
    goggles(k, "crest", 6.4, 4.4, 1.1, 0.7, False)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def chest_haagenti(k):
    dua.chest_cuirass(k)
    L.cog(k, "chest", "cog", "trim", (0.0, -7.0, 3.1), 2.7, 6, 0.7)
    for sgn in (-1, 1):
        ml.lathe(k.part("chest", f"vial_{sgn}", "crystal", layer=2), "y", (sgn * 3.0, -5.0, 3.0), [(0.0, 0.3), (0.5, 0.65), (1.8, 0.65), (2.4, 0.3)], sides=S(8), caps=(True, True))


def pauldron_haagenti(k):
    def fn(k, piece, sgn, cx, bx):
        L.cog(k, piece, "cog", "trim", (bx - sgn * 0.6, 3.8, 1.2), 3.0, 6, 0.8)
        for j in range(2):
            ml.lathe(k.part(piece, f"vial_{j}", "crystal", layer=2), "y", (bx + sgn * (1.2 + 1.4 * j), 2.6, -1.4), [(0.0, 0.3), (0.5, 0.6), (1.6, 0.6), (2.2, 0.3)], sides=S(8), caps=(True, True))
        spike(k.part(piece, "horn", "bone"), (bx + sgn * 0.8, 2.6, -2.0), (sgn * 0.7, 0.9, -0.3), 4.6, 1.0, 4)
    L.pauldrons(k, fn, 5.0, "plate", 2)


def wings_haagenti(k, sgn):
    """HAAGENTI_GEARWING: a bronze wing of riveted plates: five blade-feathers, a great cog turning at the joint, rivets down every blade."""
    bone_wing(k, sgn, "plate", 1.3)
    sdp = sd(sgn)
    L.cog(k, f"wing_out_{sdp}", "cog", "trim", (sgn * 1.0, -1.0, -0.5), 4.6, 6, 1.0)
    L.cog(k, f"wing_in_{sdp}", "cog", "trim", (sgn * 6.0, 0.0, -0.4), 2.6, 5, 0.8)
    for j, (a, s, Ln) in enumerate(((-24, 0.15, 8.5), (-58, 0.6, 8.0))):
        wf(k, "in", f"blade_{j}", sgn, s * INNER, a, Ln, 2.6, 0.6, -10, "plate", 3, pf_blade)
    for j, (a, s, Ln) in enumerate(((22, 0.0, 14.0), (2, 0.1, 19.0), (-18, 0.2, 22.0), (-38, 0.34, 20.0), (-58, 0.5, 15.0))):
        wf(k, "out", f"blade_{j}", sgn, s * OUTER, a, Ln, 3.0, 0.6, -14, "plate", 4, pf_blade)
        for q in range(3):
            t = 0.28 + 0.24 * q
            ang = rad(a - 14 * t)
            pos = (sgn * (s * OUTER + math.cos(ang) * Ln * t), math.sin(ang) * Ln * t, 0.5)
            du.stud(k.part(f"wing_out_{sdp}", f"rivet_{j}{q}", "trim"), pos, 0.45, 0.45, (0, 0, 1), 5)
        wcore(k, "out", f"core_{j}", sgn, s * OUTER, a, Ln, 3.0, -14, 3)


def tail_haagenti(k):
    seg_core(k, 1.0, "plate", "seg", 6, 0.88)
    collar(k.part("tail_seg", "band", k.trim()), -0.3, 1.2, 0.7, lip=0.2, sides=S(8), axis="-z")
    for i in range(6):
        a = 2 * PI * i / 6
        spike(k.part("tail_tip", f"hair_{i}", "cloth"), (0, 0, 0), (math.cos(a) * 0.35, math.sin(a) * 0.35, -1.0), 8.0 - (i % 2) * 2.0, 0.9, 4)
    orb(k.part("tail_tip", "knot", k.trim(), smooth=0), (0, 0, -0.6), 1.2, sides=6)


def back_haagenti(k):
    """HAAGENTI_ALEMBIC: an alembic stands behind the shoulders: a great glass flask of glowing spirit, a swan-neck pipe bending over to a coil that drips into a vial, two cogs turning behind."""
    ml.lathe(k.part("backdrop", "flask", "plate", smooth=40), "y", (0.0, -9.0, 0.0), [(0.0, 0.5), (1.0, 3.8), (4.0, 6.2), (8.0, 6.0), (11.0, 3.4), (13.0, 1.6), (17.0, 1.2)], sides=S(12), caps=(True, False))
    orb(k.part("backdrop", "spirit", "crystal", layer=2, smooth=0), (0.0, -3.4, 0.0), 4.4, sides=6)
    collar(k.part("backdrop", "flask_band", k.trim()), -3.8, 6.4, 0.8, lip=0.2, sides=S(12), origin=(0, 0, 0))
    collar(k.part("backdrop", "neck_band", k.trim()), 5.2, 1.6, 0.8, lip=0.2, sides=S(8))
    swan = [(0.0, 8.0, 0.0), (0.4, 11.4, 0.0), (3.2, 13.6, 0.0), (7.0, 12.8, 0.0), (9.6, 9.6, 0.0), (10.0, 6.0, 0.0)]
    tapered_tube(k, "backdrop", "swan", "plate", swan, 0.9, 0.8, 6)
    du.tube_direct(k.part("backdrop", "coil", k.trim(), smooth=40), [(10.0 + 1.4 * math.sin(i * 1.4), 5.6 - 1.0 * i, 1.4 * math.cos(i * 1.4)) for i in range(7)], 0.5, 0.5, 4)
    ml.lathe(k.part("backdrop", "vial", "crystal", layer=2), "y", (10.0, -2.6, 0.0), [(0.0, 0.6), (0.6, 1.5), (3.0, 1.5), (4.0, 0.7)], sides=S(8), caps=(True, True))
    L.cog(k, "backdrop", "cog_a", "extra", (-8.5, -8.0, -1.4), 6.0, 6, 1.0)
    L.cog(k, "backdrop", "cog_b", "trim", (-11.0, 2.0, -1.4), 3.6, 6, 1.0)
    L.cog(k, "backdrop", "cog_c", "trim", (9.0, -11.0, -1.4), 3.2, 5, 1.0)


def mantle_haagenti(k):
    du.sweep(k.part("mantle", "apron", "cloth", uv_mode="loft"), curve([(0.0, -0.6, 2.8), (0.0, -7.0, 3.3), (0.0, -15.0, 3.6)], 4), ml.rect_ring(1.0, 0.3), scales=lambda t: (7.0 * (1 - 0.1 * t), 1.0))
    for i in range(3):
        ml.lathe(k.part("mantle", f"vial_{i}", "crystal", layer=2), "y", (-2.2 + 2.2 * i, -9.4, 3.7), [(0.0, 0.25), (0.5, 0.55), (1.6, 0.55), (2.2, 0.25)], sides=S(8), caps=(True, True))
    cape(k, 18.0, 8.0, "cloth", 1.0, k.trim())


register("HAAGENTI", head=head_haagenti, chest=chest_haagenti, pauldron=pauldron_haagenti, wings=wings_haagenti, tail=tail_haagenti, back=back_haagenti, mantle=mantle_haagenti)

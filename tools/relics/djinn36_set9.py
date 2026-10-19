"""djinn36_set9: the weapons of the new Djinn whose kind was fixed by the rules table after the first sketches (claymore, harpoon, daggers, bow, rapier, fans, lance,
pincers, spear, claws, cleaver) and of Crocell, Furcas, Balam, Alloces, Camio and Murmur. It replaces the `weapon` builder of those names (loaded last).

Item space: the grip at the origin, the blade along +y, units px (1/16 block). Kinds and names: djinn36_weapon_data.
"""
from djinn36_lib import *                                     # noqa: F401,F403
import djinn36_lib as L
from djinn36_reg import SETS


def set_weapon(name):
    def deco(fn):
        SETS[name]["weapon"] = fn
        return fn
    return deco


def grip(k, y0=-8.0, y1=2.0, mat="cloth", r=0.85, wraps=4, wrap_mat="trim"):
    ml.loft(k.part("weapon", "grip", mat), "y", (0, y0, 0), [(0, r, r), (y1 - y0, r, r)], ml.octagon(2.0, 2.0, 0.5))
    for j in range(wraps):
        dua.w_ring(k, y0 + 0.5 + (y1 - y0 - 1.0) * j / max(1, wraps - 1), r + 0.2, f"wrap_{j}", wrap_mat, 0.5)


def tassel(k, y, off=0.8, Ln=7.0, mat="cloth"):
    sweep_flat(k.part("weapon", "tassel", mat, smooth=45), [(off, y, 0.4), (off * 3, y - Ln * 0.5, 0.6), (off * 2, y - Ln, 0.2)], 1.8, 0.25, pf_ribbon, (0, 0, 1))


# ============================================================================================ BAEL: the crown claymore
@set_weapon("BAEL")
def weapon_bael(k):
    """BAEL_CLAYMORE: a king's two-handed sword 3.1 blocks long: a broad blade, a guard that is a crown (seven gilded spires over a bar), a gem at its heart, a crown-orb pommel."""
    grip(k, -9, 2, "cloth", 0.9, 5, "trim")
    orb(k.part("weapon", "pommel", k.trim(), smooth=40), (0, -10.4, 0), 1.9, sides=8)
    glow_orb(k, "weapon", "pommel_gem", (0, -12.2, 0), 0.7)
    du.box(k.part("weapon", "guard", k.trim()), (-7.2, 1.6, -1.0), (7.2, 3.2, 1.0), chamfer=0.4)
    for i in range(-3, 4):
        Lh = 4.8 - 0.8 * abs(i) + (1.0 if i == 0 else 0.0)
        spike(k.part("weapon", f"spire_{i}", k.trim()), (i * 2.0, 3.0, 0), (i * 0.12, 1.0, 0), Lh, 0.7, 5)
        glow_orb(k, "weapon", f"tip_{i}", (i * 2.0 + i * 0.12 * Lh, 3.0 + Lh + 0.4, 0), 0.38, sides=4)
    for sgn in (-1, 1):
        sweep_flat(k.part("weapon", f"quillon_{sgn}", k.trim(), smooth=45), [(sgn * 6.6, 2.4, 0), (sgn * 8.4, 1.4, 0), (sgn * 9.0, -0.8, 0)], 1.6, 0.9, pf_taper, (0, 0, 1))
    du.box(k.part("weapon", "ricasso", "plate"), (-2.6, 3.2, -0.7), (2.6, 8.0, 0.7), chamfer=0.3)
    ml.loft(k.part("weapon", "blade", "plate", smooth=45), "y", (0, 8.0, 0), [(0, 1.0, 1.0), (22, 1.06, 1.0), (38, 0.98, 1.0), (43, 0.42, 0.7), (46, 0.0, 0.0)], ml.bevelled(7.0, 1.4, 0.42), up=(0, 0, 1))
    for sgn, nm in ((1, "f"), (-1, "b")):
        ml.ridge_pair(k.part("weapon", "fuller_" + nm, k.trim()), (0, 10.0, sgn * 0.7), (0, 40.0, sgn * 0.6), (0, 0, sgn), 1.4, 0.3, 0.14, 3.0)
    for j, y in enumerate((16, 26, 36)):
        du.bar(k.part("weapon", f"inlay_{j}", k.trim()), (-2.2, y, 0.74), (2.2, y, 0.74), 0.5, 0.12, up=(0, 0, 1), taper=0.5)
    gemz(k, "weapon", "heart", (0, 5.6, 0.9), 1.0, 1.2)
    for sgn in (-1, 1):
        du.bar(k.part("weapon", f"edge_{sgn}", "crystal", layer=2), (sgn * 3.3, 10.0, 0.0), (sgn * 0.4, 44.0, 0.0), 0.4, 0.3, up=(sgn, 0, 0), taper=4.0)
    tassel(k, 0.0, 1.0, 9.0)


# ============================================================================================ AGARES: the riptide harpoon
@set_weapon("AGARES")
def weapon_agares(k):
    """AGARES_HARPOON: a harpoon 3.4 blocks long: a scaled shaft with a coil of rope, a barbed head with two great hooked barbs set with crocodile teeth, an eye of light in the socket."""
    dua.w_shaft(k, -12, 32, 0.75, "extra", "shaft")
    for j, y in enumerate((-8, -4, 0, 4)):
        dua.w_ring(k, y, 1.1, f"scale_{j}", "plate" if j % 2 else "trim", 0.9)
    coil = [(1.4 * math.cos(i * 1.05), 8.0 + 2.0 * i, 1.4 * math.sin(i * 1.05)) for i in range(11)]
    du.tube_direct(k.part("weapon", "rope", "cloth", smooth=40), coil, 0.5, 0.45, 4)
    spike(k.part("weapon", "butt", k.trim()), (0, -12.0, 0), (0, -1.0, 0), 4.0, 1.0, 5)
    dua.w_ring(k, 31.0, 1.7, "socket", "trim", 1.8)
    glow_orb(k, "weapon", "eye", (0, 32.0, 1.6), 0.7)
    dua.spear_blade(k, 33.0, 17.0, 4.2, 1.0, "head", "plate")
    for sgn, nm in ((1, "l"), (-1, "r")):
        bp = [(sgn * 0.8, 34.0, 0), (sgn * 4.6, 36.4, 0), (sgn * 7.6, 34.6, 0), (sgn * 8.4, 30.0, 0), (sgn * 6.6, 26.6, 0)]
        sweep_flat(k.part("weapon", "barb_" + nm, "plate", smooth=45), curve(bp, 4), 2.6, 1.0, lambda s: 0.9 - 0.7 * s * s + 0.1, (0, 0, 1))
        sweep_flat(k.part("weapon", "barb_edge_" + nm, "crystal", layer=2), curve([(x, y, 0.6) for (x, y, z) in bp], 4), 0.5, 0.3, pf_taper, (0, 0, 1))
        for i in range(3):
            spike(k.part("weapon", f"tooth_{nm}{i}", "bone"), (sgn * (5.0 + 1.6 * i * 0.6), 35.6 - 3.0 * i, 0), (-sgn * 0.5, -0.5, 0), 1.8, 0.4, 4)
    du.bar(k.part("weapon", "core", "crystal", layer=2), (0, 36.0, 0.9), (0, 48.0, 0.6), 0.5, 0.2, up=(0, 0, 1), taper=3.0)
    sweep_tube(k.part("weapon", "line", "cloth", smooth=40), [(0.0, 12.0, 1.4), (-2.0, 8.0, 2.4), (1.0, 2.0, 2.0), (-1.0, -6.0, 1.4)], 0.3, 0.3, 4)


# ============================================================================================ VALEFOR: the stormheart daggers
def kris(k, nm):
    ml.loft(k.part("weapon", nm + "_grip", "cloth"), "y", (0, -6, 0), [(0, 0.8, 0.8), (7, 0.8, 0.8)], ml.octagon(2.0, 2.0, 0.5))
    for j in range(2):
        dua.w_ring(k, -5.0 + 2.4 * j, 1.05, f"{nm}_wrap_{j}", "trim", 0.55)
    orb(k.part("weapon", nm + "_pommel", k.trim(), smooth=40), (0, -7.0, 0), 1.2, sides=6)
    du.box(k.part("weapon", nm + "_guard", k.trim()), (-3.6, 1.0, -0.9), (3.6, 2.2, 0.9), chamfer=0.4)
    for sgn in (-1, 1):
        spike(k.part("weapon", f"{nm}_quillon{sgn}", "plate"), (sgn * 3.2, 1.6, 0), (sgn * 0.8, 0.6, 0), 3.0, 0.6, 4)
    path = [(0.6 * math.sin(i * 1.25) * (1 if i < 6 else 0.8), 2.2 + 3.4 * i, 0) for i in range(7)]
    sweep_flat(k.part("weapon", nm + "_blade", "plate", smooth=45), curve(path, 5), 3.2, 0.9, lambda s: (1.0 - 0.15 * s) * (1 - s ** 4) + 0.04, (0, 0, 1))
    sweep_flat(k.part("weapon", nm + "_core", "crystal", layer=2), curve([(x, y, 0.5) for (x, y, z) in path], 5), 0.6, 0.3, pf_taper, (0, 0, 1))
    zig(k.part("weapon", nm + "_bolt", "crystal", layer=2), [(0.9, 4.0, 0.6), (-0.5, 8.0, 0.6), (0.7, 12.0, 0.6), (-0.3, 16.0, 0.6), (0.5, 19.0, 0.6)], 0.55, 0.2)
    heart(k, "weapon", nm + "_heart", (0, 1.6, 1.2), 0.9)


@set_weapon("VALEFOR")
def weapon_valefor(k):
    """VALEFOR_DAGGERS: the heart-thief's pair of kris crossed at one grip, each blade a wavy flame of steel with lightning in it and a stolen heart in its guard, a hooked chain from the pommel."""
    n0 = len(k.m.parts)
    kris(k, "a")
    L.transform_since(k, n0, rot=("z", 16))
    L.transform_since(k, n0, shift=(0.0, 0.0, 0.8))
    n1 = len(k.m.parts)
    kris(k, "b")
    L.transform_since(k, n1, rot=("z", -22))
    L.transform_since(k, n1, shift=(0.0, 0.0, -0.8))
    chain = [(0, -8.0, 0), (1.4, -11.0, 0), (-1.0, -14.0, 0), (1.2, -17.0, 0), (0, -20.0, 0)]
    du.tube_direct(k.part("weapon", "chain", "plate", smooth=40), chain, 0.34, 0.3, 4)
    sweep_tube(k.part("weapon", "hook", "plate", smooth=40), [(0, -20.0, 0), (0, -23.0, 0), (1.6, -25.0, 0), (3.0, -23.6, 0)], 0.5, 0.16, 4)


# ============================================================================================ BARBATOS: the greenwood bow
@set_weapon("BARBATOS")
def weapon_barbatos(k):
    """BARBATOS_BOW: a hunter's recurve bow 2.7 blocks tall: living wood limbs set with leaves, antler tines for tips, a string of light, a green-fletched arrow on the string."""
    limb = [(0.0, 0.0, 0.0), (-2.0, 7.0, 0.0), (-4.8, 14.0, 0.0), (-4.2, 21.0, 0.0), (-1.2, 25.0, 0.0), (1.8, 26.4, 0.0)]
    for sgn, nm in ((1, "up"), (-1, "dn")):
        pts = [(x, y * sgn, z) for (x, y, z) in limb]
        sweep_flat(k.part("weapon", "limb_" + nm, "extra", smooth=45), curve(pts, 4), 1.8, 1.4, lambda s: 1.0 - 0.5 * s, (0, 0, 1), caps=(True, True))
        sweep_flat(k.part("weapon", "inlay_" + nm, "trim", smooth=45), curve([(x, y * sgn, z + 0.62) for (x, y, z) in pts], 4), 0.5, 0.3, pf_taper, (0, 0, 1))
        for i, (t, a) in enumerate(((0.35, 50), (0.6, 40), (0.85, 30))):
            p = limb[min(len(limb) - 1, int(t * (len(limb) - 1)) + 1)]
            lf(k, "weapon", f"leaf_{nm}{i}", "cloth", dub.leaf_blade(None, (p[0], p[1] * sgn, 0.9), 180 - a if sgn > 0 else 180 + a, 4.2, 2.2, 1.0, 10, 3))
        spike(k.part("weapon", "tine_" + nm, "bone"), (1.8, 26.4 * sgn, 0.0), (0.7, 0.9 * sgn, 0.0), 5.6, 0.7, 4)
        spike(k.part("weapon", "tine2_" + nm, "bone"), (0.6, 25.2 * sgn, 0.0), (-0.3, 0.9 * sgn, 0.0), 3.4, 0.5, 4)
    du.box(k.part("weapon", "riser", "extra"), (-1.8, -4.2, -1.2), (1.8, 4.2, 1.2), chamfer=0.4)
    for j in range(3):
        dua.w_ring(k, -3.2 + 2.4 * j, 1.7, f"wrap_{j}", "cloth", 0.7)
    glow_orb(k, "weapon", "riser_gem", (0, 0, 1.3), 0.8)
    du.bar(k.part("weapon", "string", "crystal", layer=2), (1.8, 26.4, 0.0), (1.8, -26.4, 0.0), 0.4, 0.2, up=(0, 0, 1))
    spike(k.part("weapon", "arrow_head", "plate"), (3.4, 0, 0.0), (1.0, 0, 0), 4.4, 0.8, 4)
    glow_orb(k, "weapon", "arrow_tip", (8.2, 0, 0.0), 0.4, sides=4)
    du.bar(k.part("weapon", "arrow", "bone"), (-9.0, 0.0, 0.0), (3.4, 0.0, 0.0), 0.4, 0.3, up=(0, 0, 1))
    for sg in (-1, 1):
        lf(k, "weapon", f"fletch_{sg}", "cloth", [(-9.0, 0, 0), (-6.0, sg * 1.8, 0), (-3.4, sg * 1.2, 0), (-4.2, 0, 0)])


# ============================================================================================ GUSION: the mirrorwater rapier
@set_weapon("GUSION")
def weapon_gusion(k):
    """GUSION_RAPIER: a rapier 2.9 blocks long: a needle blade of silver water, a guard that is a disc of mirror between two breaking waves, a tear of glass for the pommel."""
    grip(k, -9, 2, "cloth", 0.8, 3, "trim")
    orb(k.part("weapon", "pommel", "crystal", layer=2, smooth=0), (0, -10.2, 0), 1.3, sides=6)
    orb(k.part("weapon", "pommel_cap", k.trim(), smooth=0), (0, -9.2, 0), 1.2, sides=6)
    disc(k.part("weapon", "guard", k.trim()), (0, 2.6, 0), 3.8, 1.2, "z", 12)
    disc(k.part("weapon", "glass", "crystal", layer=2), (0, 2.6, 0.8), 2.8, 0.25, "z", 10)
    for sgn in (-1, 1):                                                                                      # a wave curling over at each end of the guard
        pts = arc_pts((4.6, 2.6, 0.0), 2.4, 180, -90, 7) if sgn > 0 else arc_pts((-4.6, 2.6, 0.0), 2.4, 0, 270, 7)
        sweep_flat(k.part("weapon", f"wave_{sgn}", "plate", smooth=45), pts, 1.6, 0.8, lambda s: 0.5 + 0.5 * math.sin(math.pi * min(1, s)), (0, 0, 1))
    du.box(k.part("weapon", "ricasso", k.trim()), (-0.9, 4.2, -0.5), (0.9, 7.0, 0.5))
    ml.loft(k.part("weapon", "blade", "plate", smooth=45), "y", (0, 7.0, 0), [(0, 1.0, 1.0), (16, 0.9, 1.0), (32, 0.6, 0.9), (41, 0.0, 0.0)], ml.bevelled(2.8, 0.9, 0.45), up=(0, 0, 1))
    for sgn, nm in ((1, "f"), (-1, "b")):
        ml.ridge_pair(k.part("weapon", "fuller_" + nm, k.trim()), (0, 9.0, sgn * 0.45), (0, 37.0, sgn * 0.3), (0, 0, sgn), 0.9, 0.2, 0.1, 3.0)
    du.bar(k.part("weapon", "core", "crystal", layer=2), (0, 9.5, 0.5), (0, 39.0, 0.35), 0.4, 0.2, up=(0, 0, 1), taper=3.0)
    for i in range(3):
        orb(k.part("weapon", f"drop_{i}", "crystal", layer=2, smooth=0), (0.0, 16.0 + 8.0 * i, 0.7), 0.4, sides=4)
    sweep_flat(k.part("weapon", "streamer", "cloth", smooth=45), [(0.6, -9.0, 0.3), (-2.4, -14.0, 0.6), (0.4, -19.0, 0.2), (-2.0, -24.0, 0.0)], 2.2, 0.25, pf_ribbon, (0, 0, 1))


# ============================================================================================ SHAX: the dove-feather fans
def war_fan(k, nm, base_y=3.0):
    fan(k, "weapon", nm + "_", (0.0, base_y, 0.0), (150, 132, 114, 96, 78, 60, 42, 24, 6)[::1], (17.0, 19.0, 20.5, 21.5, 22.0, 21.5, 20.5, 19.0, 17.0), 2.7, "plate", 0.0, 3, 1.0, pf_feather, 0.4)
    for j, a in enumerate((150, 114, 78, 42)):
        r = rad(a)
        rib(k, "weapon", f"{nm}_rib_{j}", "trim", (0.0, base_y, 0.35), (math.cos(r) * 14.0, base_y + math.sin(r) * 14.0, 0.35), 0.4, 0.16, taper=1.0)
    arc = [(math.cos(rad(a)) * 11.0, base_y + math.sin(rad(a)) * 11.0, 0.4) for a in np.linspace(24, 150, 7)]
    du.tube_direct(k.part("weapon", nm + "_band", k.trim(), smooth=40), arc, 0.3, 0.3, 4)
    orb(k.part("weapon", nm + "_rivet", k.trim(), smooth=0), (0.0, base_y, 0.5), 1.0, sides=5)
    glow_orb(k, "weapon", nm + "_rivet_gem", (0.0, base_y, 1.0), 0.45, sides=4)


@set_weapon("SHAX")
def weapon_shax(k):
    """SHAX_FANS: a pair of war fans of white feathers crossed at one grip, each a spread of nine feathers on ribs of silver banded with light, a ribbon trailing from the handle."""
    grip(k, -8, 3, "cloth", 0.8, 3, "trim")
    orb(k.part("weapon", "pommel", "plate", smooth=40), (0, -9.2, 0), 1.3, sides=6)
    n0 = len(k.m.parts)
    war_fan(k, "a")
    L.transform_since(k, n0, rot=("z", -24))
    L.transform_since(k, n0, shift=(0.0, 0.0, 0.5))
    n1 = len(k.m.parts)
    war_fan(k, "b")
    L.transform_since(k, n1, rot=("z", 24))
    L.transform_since(k, n1, shift=(0.0, 0.0, -0.5))
    ribbon(k, "weapon", "ribbon", "cloth", [(0.0, -10.0, 0.2), (-2.4, -14.0, 0.4), (0.4, -19.0, 0.2), (-1.8, -24.0, 0.0)], 2.2, 0.25, 0.4)


# ============================================================================================ ANDREALPHUS: the starfall lance
@set_weapon("ANDREALPHUS")
def weapon_andrealphus(k):
    """ANDREALPHUS_LANCE: a lance 3.3 blocks long: a blue shaft banded in gold, a slim head set in a star, three plumes of a peacock hanging from the socket each ending in an eye of light."""
    dua.w_shaft(k, -12, 38, 0.72, "extra", "shaft")
    for j, y in enumerate((-8, -4, 0, 4, 8)):
        dua.w_ring(k, y, 1.08, f"wrap_{j}", "trim" if j % 2 else "plate", 0.8)
    orb(k.part("weapon", "pommel", k.trim(), smooth=40), (0, -13.2, 0), 1.6, sides=8)
    glow_orb(k, "weapon", "pommel_gem", (0, -14.6, 0), 0.6)
    ml.lathe(k.part("weapon", "cup", "plate", smooth=40), "y", (0, 2.0, 0), [(0.0, 3.4), (2.0, 2.8), (5.0, 1.2), (7.0, 0.95)], sides=S(10), caps=(False, False))
    dua.w_ring(k, 37.0, 1.5, "socket", "trim", 1.8)
    extrude(k, "weapon", "star", "trim", star_poly(0, 41.0, 4.6, 1.8, 5), -0.5, 0.5)
    glow_orb(k, "weapon", "star_gem", (0, 41.0, 0.9), 0.8, sides=5)
    dua.spear_blade(k, 43.0, 18.0, 3.4, 0.9, "head", "plate")
    for sg in (-1, 1):
        fan(k, "weapon", f"crest{sg}_", (sg * 0.8, 38.0, -0.2), (150, 128), (7.0, 9.0), 1.8, "plate", -14, 3, sg)
    for i, (off, Ln) in enumerate(((-2.4, 17.0), (0.0, 21.0), (2.4, 17.0))):
        pts = [(off * 0.4, 36.0, 0.4 + 0.2 * i), (off * 1.5, 36.0 - Ln * 0.4, 0.6 + 0.2 * i), (off * 2.0, 36.0 - Ln, 0.4 + 0.2 * i)]
        sweep_flat(k.part("weapon", f"plume_{i}", "plate" if i != 1 else "cloth", smooth=45), curve(pts, 4), 2.6, 0.3, pf_feather, (0, 0, 1))
        extrude(k, "weapon", f"eye_o_{i}", "trim", ngon(pts[-1][0], pts[-1][1], 1.2, 6), pts[-1][2] - 0.2, pts[-1][2] + 0.2)
        glow_orb(k, "weapon", f"eye_{i}", (pts[-1][0], pts[-1][1], pts[-1][2] + 0.4), 0.7, sides=4)


# ============================================================================================ DECARABIA: the star-shell pincers
@set_weapon("DECARABIA")
def weapon_decarabia(k):
    """DECARABIA_PINCERS: a pair of crab's pincers on one short haft: a grip with a carapace guard and a gem of the star, two great claws opening about it, teeth on their inner edges."""
    grip(k, -8, 6, "extra", 0.9, 4, "trim")
    orb(k.part("weapon", "butt", k.trim(), smooth=40), (0, -9.4, 0), 1.5, sides=8)
    ml.lathe(k.part("weapon", "shell", "plate", smooth=40), "y", (0, 4.5, 0), [(0.0, 1.0), (1.4, 3.6), (3.2, 3.8), (4.6, 1.4)], sides=S(10), caps=(False, False))
    dua.w_ring(k, 8.5, 1.6, "collar", "trim", 1.6)
    for s2, nm, ln, w in ((1, "up", 22.0, 5.8), (-1, "lo", 17.0, 4.6)):
        pts = arc_path((0.0, 10.0, 0.0), 90 + s2 * 33, ln, -s2 * 26.0, 5, "xy", 1.0)
        sweep_flat(k.part("weapon", "claw_" + nm, "plate", smooth=45), curve(pts, 4), w, 1.2, lambda s: (0.6 + 0.4 * math.sin(math.pi * min(1.0, s * 1.2))) * (1 - s ** 3) + 0.05, (0, 0, 1))
        sweep_flat(k.part("weapon", "claw_edge_" + nm, "crystal", layer=2), curve([(x, y, 0.7) for (x, y, z) in pts], 4), 0.6, 0.3, pf_taper, (0, 0, 1))
        for i in range(4):
            p = np.array(pts[1 + i])
            d = unit(np.array(pts[min(i + 2, 5)]) - np.array(pts[i]))
            nrm = np.array([d[1], -d[0], 0.0])
            if nrm[1] * s2 > 0:
                nrm = -nrm
            spike(k.part("weapon", f"tooth_{nm}{i}", "bone"), tuple(p), tuple(nrm * 0.9 + d * 0.2), 1.9, 0.4, 4)
    extrude(k, "weapon", "star", "trim", star_poly(0, 13.0, 2.6, 1.1, 5), 0.0, 0.6)
    glow_orb(k, "weapon", "star_gem", (0, 13.0, 0.9), 0.6, sides=5)
    tassel(k, 8.0, 1.0, 8.0)


# ============================================================================================ ANDROMALIUS: the seeker spear
@set_weapon("ANDROMALIUS")
def weapon_andromalius(k):
    """ANDROMALIUS_SPEAR: a seeker's spear 3.2 blocks long: a dark shaft with a serpent coiled up it, a head of cold light whose blade holds a watching eye, small eyes down the haft."""
    dua.w_shaft(k, -11, 32, 0.7, "extra", "shaft")
    for j, y in enumerate((-7, -3)):
        dua.w_ring(k, y, 1.05, f"wrap_{j}", "cloth", 1.0)
    orb(k.part("weapon", "butt", k.trim(), smooth=40), (0, -12.2, 0), 1.4, sides=8)
    pts = [(1.35 * math.sin(i * 0.95), 2.0 + 2.2 * i, 1.35 * math.cos(i * 0.95)) for i in range(13)]
    du.tube_direct(k.part("weapon", "serpent", "plate", smooth=40), pts, 0.66, 0.42, 4)
    hp = np.array(pts[-1])
    orb(k.part("weapon", "snake_head", "plate", smooth=0), tuple(hp + (0, 0.6, 0.4)), 1.0, sides=5)
    for sg in (-1, 1):
        glow_orb(k, "weapon", f"snake_eye_{sg}", tuple(hp + (sg * 0.55, 1.0, 1.0)), 0.25, sides=4)
        spike(k.part("weapon", f"fang_{sg}", "bone"), tuple(hp + (sg * 0.3, 0.2, 1.2)), (0, -1.0, 0.2), 1.2, 0.18, 4)
    dua.w_ring(k, 31.0, 1.6, "collar", "trim", 1.8)
    dua.spear_blade(k, 33.0, 22.0, 5.0, 1.0, "head", "plate", False)
    L.almond(k, "weapon", "eye", (0.0, 40.0, 0.7), 1.9, 1.0, "trim", 0.22)
    du.bar(k.part("weapon", "edge_l", "crystal", layer=2), (-2.3, 35.0, 0.6), (-0.2, 53.0, 0.4), 0.4, 0.2, up=(0, 0, 1), taper=4.0)
    du.bar(k.part("weapon", "edge_r", "crystal", layer=2), (2.3, 35.0, 0.6), (0.2, 53.0, 0.4), 0.4, 0.2, up=(0, 0, 1), taper=4.0)
    for i in range(3):
        glow_orb(k, "weapon", f"eye_{i}", (1.0, 8.0 + 8.0 * i, 0.8), 0.4, sides=4)
    for sg in (-1, 1):
        spike(k.part("weapon", f"barb_{sg}", "plate"), (sg * 1.4, 34.0, 0), (sg * 1.0, -0.2, 0), 3.4, 0.6, 4)
    sweep_flat(k.part("weapon", "tassel", "cloth", smooth=45), [(0.8, 30.0, 0.4), (2.4, 25.0, 0.6), (1.4, 19.0, 0.3)], 1.8, 0.25, pf_ribbon, (0, 0, 1))


# ============================================================================================ GLASYA_LABOLAS: the slaughter fangs
@set_weapon("GLASYA_LABOLAS")
def weapon_glasya(k):
    """GLASYA_FANGS: a pair of slaughter fangs for a fist: a spiked bar to hold, a cuff, three great curved claws edged with teeth, two small fangs at the sides, red light along their backs."""
    du.box(k.part("weapon", "grip", "extra"), (-4.6, -1.4, -1.1), (4.6, 1.4, 1.1), chamfer=0.4)
    for j, x in enumerate((-3.4, -1.1, 1.1, 3.4)):
        du.stud(k.part("weapon", f"knuckle_{j}", k.trim()), (x, 0.0, 1.1), 0.7, 0.6, (0, 0, 1), 6)
    collar(k.part("weapon", "cuff", "plate"), -8.0, 2.4, 3.2, lip=0.4, sides=S(10), axis="y")
    for i in range(6):
        a = 2 * PI * i / 6
        spike(k.part("weapon", f"cuff_spike_{i}", "plate"), (2.4 * math.cos(a), -6.6, 2.4 * math.sin(a)), (math.cos(a), 0.0, math.sin(a)), 1.8, 0.45, 4)
    glow_orb(k, "weapon", "cuff_gem", (0, -6.6, 2.7), 0.6, sides=4)
    du.box(k.part("weapon", "guard", "plate"), (-5.0, 1.4, -1.5), (5.0, 3.8, 1.5), chamfer=0.5)
    for j, (x, Ln, curl) in enumerate(((-3.2, 20.0, -22), (0.0, 27.0, 0), (3.2, 20.0, 22))):
        p = k.part("weapon", f"claw_{j}", "bone", smooth=45)
        feather(p, (x, 3.4, 0.0), 90 - x * 3.0, Ln, 3.0, 1.2, curl, "xy", 5, pf_blade, sx=1.0)
        sweep_flat(k.part("weapon", f"claw_edge_{j}", "crystal", layer=2), curve([(x + (curl / 60.0) * s * s * 6, 5.0 + (Ln - 4) * s, 0.6) for s in np.linspace(0, 1, 5)], 3), 0.6, 0.3, pf_taper, (0, 0, 1))
        for q in range(3):
            spike(k.part("weapon", f"tooth_{j}{q}", "bone"), (x + 1.0 + (curl / 40.0) * (q + 1) * 0.4, 8.0 + 5.0 * q, 0.0), (0.9, -0.2, 0), 2.0, 0.4, 4)
    for sgn in (-1, 1):
        spike(k.part("weapon", f"side_fang_{sgn}", "bone"), (sgn * 5.0, 2.4, 0.0), (sgn * 0.8, 0.8, 0.0), 8.0, 1.1, 5)
    sweep_tube(k.part("weapon", "chain", "plate", smooth=40), [(0, -11.4, 0), (1.4, -14.4, 0), (-1.0, -17.4, 0), (1.2, -20.4, 0)], 0.36, 0.3, 4)


# ============================================================================================ HAAGENTI: the bull cleaver
@set_weapon("HAAGENTI")
def weapon_haagenti(k):
    """HAAGENTI_CLEAVER: a great bronze cleaver 2.7 blocks long: a broad blade with a toothed back and a cog on its spine, a guard of two bull's horns, a flask of burning spirit for a pommel."""
    grip(k, -8, 2, "cloth", 0.95, 4, "trim")
    ml.lathe(k.part("weapon", "flask", "plate", smooth=40), "y", (0, -17.0, 0), [(0.0, 0.6), (0.8, 2.6), (3.0, 3.4), (6.0, 2.6), (8.5, 1.2)], sides=S(10), caps=(True, False))
    orb(k.part("weapon", "spirit", "crystal", layer=2, smooth=0), (0, -13.2, 0), 2.2, sides=6)
    du.box(k.part("weapon", "guard", k.trim()), (-3.0, 1.0, -1.0), (6.4, 2.8, 1.0), chamfer=0.4)
    for sgn in (-1, 1):
        tapered_tube(k, "weapon", f"horn_{sgn}", "bone", [(sgn * 1.0, 2.0, 0), (sgn * 5.0, 1.0, 0), (sgn * 7.4, 3.6, 0), (sgn * 7.4, 7.4, 0)], 1.0, 0.18, 5)
    blade = [(-1.6, 2.8), (7.8, 2.8), (7.8, 30.0), (4.4, 36.0), (-1.6, 28.0)]
    extrude(k, "weapon", "blade", "plate", blade, -0.55, 0.55, smooth=40)
    du.bar(k.part("weapon", "spine", k.trim()), (-1.8, 3.2, 0.0), (-1.8, 28.0, 0.0), 1.2, 0.9, up=(-1, 0, 0), taper=2.0)
    for i in range(5):
        spike(k.part("weapon", f"tooth_{i}", "plate"), (-1.8, 6.0 + 5.0 * i, 0.0), (-1.0, 0.1, 0.0), 2.2 - 0.1 * i, 0.55, 4)
    du.bar(k.part("weapon", "edge", "crystal", layer=2), (7.9, 4.0, 0.0), (7.9, 30.0, 0.0), 0.5, 0.4, up=(1, 0, 0), taper=2.0)
    L.cog(k, "weapon", "cog", "trim", (3.0, 14.0, 0.55), 3.0, 6, 0.7)
    for i in range(3):
        du.bar(k.part("weapon", f"rune_{i}", "crystal", layer=2), (1.0, 20.0 + 3.4 * i, 0.62), (6.0, 20.6 + 3.4 * i, 0.62), 0.3, 0.12, up=(0, 0, 1))
    tassel(k, -9.0, 1.0, 7.0)


# ============================================================================================ CROCELL: the frost sage staff
@set_weapon("CROCELL")
def weapon_crocell(k):
    """CROCELL_STAFF: a sage's staff 3 blocks long: a pale shaft wound in frost, a head of ice crystals about a great snowflake of light, icicles hanging beneath."""
    dua.w_shaft(k, -10, 28, 0.72, "extra", "shaft")
    for j, y in enumerate((-6, -2, 2, 6)):
        dua.w_ring(k, y, 1.05, f"wrap_{j}", "cloth", 1.0)
    orb(k.part("weapon", "butt", "plate", smooth=40), (0, -11.2, 0), 1.5, sides=6)
    dua.w_ring(k, 27.0, 1.7, "collar", "trim", 1.8)
    for i in range(6):
        a = 2 * PI * i / 6
        shard(k, "weapon", f"ice_{i}", "plate", (math.cos(a) * 1.0, 29.0, math.sin(a) * 1.0), (math.cos(a) * 0.55, 1.0, math.sin(a) * 0.55), 9.0 + (i % 2) * 3.0, 2.0, 1.0, glow=True)
    shard(k, "weapon", "ice_top", "plate", (0, 29.0, 0), (0, 1.0, 0), 15.0, 2.6, 1.2, glow=True)
    n0 = len(k.m.parts)
    snowflake_head(k)
    for i in range(4):
        spike(k.part("weapon", f"icicle_{i}", "plate"), (math.cos(i * 1.57) * 1.7, 26.4, math.sin(i * 1.57) * 1.7), (0, -1.0, 0), 4.0 - 0.6 * (i % 2), 0.4, 4)


def snowflake_head(k):
    cy = 39.0
    for i in range(6):
        a = rad(90 + 60 * i)
        d = np.array([math.cos(a), math.sin(a), 0.0])
        tip = np.array([0.0, cy, 0.0]) + d * 6.4
        du.bar(k.part("weapon", f"flake_arm{i}", "crystal", layer=2), (0.0, cy, 0.0), tuple(tip), 0.8, 0.5, up=(0, 0, 1), taper=1.6)
        p = np.array([0.0, cy, 0.0]) + d * 3.6
        for sg in (-1, 1):
            b = a + sg * rad(55)
            e = p + np.array([math.cos(b), math.sin(b), 0.0]) * 2.2
            du.bar(k.part("weapon", f"flake_br{i}{sg}", "crystal", layer=2), tuple(p), tuple(e), 0.6, 0.4, up=(0, 0, 1), taper=0.5)
    glow_orb(k, "weapon", "flake_heart", (0, cy, 0.6), 1.0)


# ============================================================================================ FURCAS: the ice-wind halberd
@set_weapon("FURCAS")
def weapon_furcas(k):
    """FURCAS_HALBERD: a knight's halberd 3.4 blocks long: a banded haft, a spear point, a broad axe blade rimmed with frost, a hook behind, a pennon of the ice wind."""
    dua.w_shaft(k, -12, 36, 0.78, "plate", "shaft")
    for j, y in enumerate((-8, -4, 0, 4, 8)):
        dua.w_ring(k, y, 1.15, f"band_{j}", "trim" if j % 2 else "cloth", 0.9)
    spike(k.part("weapon", "butt", k.trim()), (0, -12.0, 0), (0, -1.0, 0), 4.4, 1.0, 5)
    dua.w_ring(k, 35.0, 1.7, "socket", "trim", 1.8)
    dua.spear_blade(k, 37.0, 16.0, 3.6, 0.9, "point", "plate")
    axe = [(0.8, 30.0), (9.0, 32.0), (14.0, 36.0), (14.4, 26.0), (11.0, 21.0), (0.8, 24.0)]
    extrude(k, "weapon", "axe", "plate", axe, -0.6, 0.6, smooth=40)
    du.tube_direct(k.part("weapon", "axe_rim", "crystal", layer=2, smooth=40), [(x, y, 0.7) for (x, y) in axe[1:5]], 0.4, 0.4, 4)
    for i in range(4):
        spike(k.part("weapon", f"icicle_{i}", "plate"), (11.0 + 1.0 * i, 22.0 + 3.0 * i - 3.0 + i * 0.0, 0.0), (0.3, -1.0, 0), 3.0, 0.5, 4)
    sweep_tube(k.part("weapon", "hook", "plate", smooth=40), [(-0.8, 28.0, 0), (-4.0, 29.0, 0), (-6.6, 26.0, 0), (-6.0, 22.0, 0)], 0.9, 0.18, 5)
    glow_orb(k, "weapon", "gem", (4.0, 28.0, 0.9), 0.7)
    pen = [(1.0, 33.0, 0.0), (-11.0, 34.0, 0.0), (-8.0, 31.0, 0.0), (-11.4, 27.6, 0.0), (1.0, 28.0, 0.0)]
    lf(k, "weapon", "pennon", "cloth", pen)
    rib(k, "weapon", "pennon_edge", "crystal", (1.0, 33.2, 0.2), (-11.0, 34.2, 0.2), 0.5, 0.18, layer=2, taper=1.0)
    tassel(k, 22.0, 0.8, 7.0)


# ============================================================================================ BALAM: the triple-flame trident
@set_weapon("BALAM")
def weapon_balam(k):
    """BALAM_TRIDENT: a trident 3.1 blocks long: a wrapped haft, three prongs each ending in a flame, three small faces of the prophecy on the crossbar."""
    dua.w_shaft(k, -10, 30, 0.75, "extra", "shaft")
    for j, y in enumerate((-6, -2, 2, 6)):
        dua.w_ring(k, y, 1.08, f"wrap_{j}", "cloth", 0.9)
    orb(k.part("weapon", "butt", k.trim(), smooth=40), (0, -11.2, 0), 1.5, sides=8)
    dua.w_ring(k, 29.0, 1.8, "collar", "trim", 1.8)
    du.box(k.part("weapon", "cross", "plate"), (-6.0, 31.0, -1.0), (6.0, 33.0, 1.0), chamfer=0.4)
    for i, x in enumerate((-4.4, 0.0, 4.4)):
        L.mask(k, "weapon", f"face_{i}", (x, 32.0, 1.4), 0.62, 0.0, "bone" if i != 1 else "plate")
    for i, (x, Ln) in enumerate(((-5.0, 15.0), (0.0, 21.0), (5.0, 15.0))):
        bp = [(x * 0.2, 32.0, 0), (x, 34.0, 0), (x * 1.05, 32.0 + Ln * 0.5, 0), (x * 0.9, 32.0 + Ln, 0)]
        sweep_tube(k.part("weapon", f"prong_{i}", "plate", smooth=40), bp, 0.8, 0.45, 5)
        spike(k.part("weapon", f"head_{i}", "plate"), (x * 0.9, 32.0 + Ln, 0), (0, 1.0, 0), 5.0, 0.9, 5)
        for sg in (-1, 1):
            spike(k.part("weapon", f"barb_{i}{sg}", "plate"), (x * 0.9, 32.0 + Ln + 0.4, 0), (sg * 0.9, -0.5, 0), 3.0, 0.5, 4)
        tongue(k.part("weapon", f"flame_{i}", "crystal", layer=2, smooth=45), (x * 0.9, 32.0 + Ln - 1.0, 0.0), (0, 1.0, 0), 8.0 + (i == 1) * 3.0, 2.8, 0.5, 0.7, i * 1.4, n=5)
    tassel(k, 26.0, 0.8, 7.0)


# ============================================================================================ ALLOCES: the lion-eye longsword
@set_weapon("ALLOCES")
def weapon_alloces(k):
    """ALLOCES_LONGSWORD: a longsword 3 blocks long: a straight blade, a guard that is a lion's face with a mane of flame for quillons, an amber eye in each side, a mane of cloth at the pommel."""
    grip(k, -8, 2, "cloth", 0.85, 4, "trim")
    orb(k.part("weapon", "pommel", k.trim(), smooth=40), (0, -9.4, 0), 1.6, sides=8)
    for i in range(7):
        a = rad(200 + 20 * i)
        spike(k.part("weapon", f"pommel_mane_{i}", "cloth"), (math.cos(a) * 1.4, -9.4 + math.sin(a) * 1.4 * 0.0, math.sin(a) * 1.4), (math.cos(a), -0.8, math.sin(a)), 3.6, 0.7, 4)
    du.box(k.part("weapon", "guard", k.trim()), (-6.0, 1.4, -1.0), (6.0, 3.0, 1.0), chamfer=0.4)
    extrude(k, "weapon", "face", "plate", ngon(0, 3.6, 2.8, 8), 0.0, 1.4)
    for sg in (-1, 1):
        L.lens(k, "weapon", f"eye_{sg}", (sg * 1.3, 4.0, 1.5), 0.9, 0.45)
        for j in range(2):
            tongue(k.part("weapon", f"quillon_{sg}{j}", "crystal", layer=2, smooth=45), (sg * (5.0 + 0.8 * j), 2.4, 0.0), (sg * 1.0, 0.5 + 0.4 * j, 0), 4.0 - 0.6 * j, 1.4, 0.4, 0.4 * sg, j, n=3)
    du.box(k.part("weapon", "ricasso", "plate"), (-2.4, 3.0, -0.6), (2.4, 8.0, 0.6), chamfer=0.3)
    ml.loft(k.part("weapon", "blade", "plate", smooth=45), "y", (0, 8.0, 0), [(0, 1.0, 1.0), (24, 1.02, 1.0), (38, 0.9, 1.0), (43, 0.4, 0.7), (45, 0.0, 0.0)], ml.bevelled(5.6, 1.2, 0.42), up=(0, 0, 1))
    for sgn, nm in ((1, "f"), (-1, "b")):
        ml.ridge_pair(k.part("weapon", "fuller_" + nm, k.trim()), (0, 10.0, sgn * 0.6), (0, 38.0, sgn * 0.5), (0, 0, sgn), 1.1, 0.26, 0.12, 3.0)
    for sgn in (-1, 1):
        du.bar(k.part("weapon", f"edge_{sgn}", "crystal", layer=2), (sgn * 2.6, 10.0, 0.0), (sgn * 0.3, 43.0, 0.0), 0.4, 0.3, up=(sgn, 0, 0), taper=4.0)
    for i in range(4):
        glow_orb(k, "weapon", f"sun_{i}", (0.0, 14.0 + 6.0 * i, 0.8), 0.4, sides=4)


# ============================================================================================ CAMIO: the raven quill staff
@set_weapon("CAMIO")
def weapon_camio(k):
    """CAMIO_QUILL: a staff 2.8 blocks long ending in the great quill of a raven, its vane black and edged in light, a crow perched at its foot with an eye of light, an inkwell of glow below."""
    dua.w_shaft(k, -10, 26, 0.7, "extra", "shaft")
    for j, y in enumerate((-6, -2, 2, 6)):
        dua.w_ring(k, y, 1.05, f"wrap_{j}", "cloth", 1.0)
    orb(k.part("weapon", "butt", k.trim(), smooth=40), (0, -11.2, 0), 1.4, sides=8)
    dua.w_ring(k, 25.0, 1.5, "collar", "trim", 1.6)
    qp = [(0, 26.0, 0), (0.6, 32.0, 0), (2.6, 38.0, 0), (5.4, 43.0, 0), (6.6, 48.0, 0)]
    sweep_tube(k.part("weapon", "quill", "bone", smooth=40), curve(qp, 4), 0.55, 0.3, 5)
    sweep_flat(k.part("weapon", "vane", "plate", smooth=45), curve([(x + 0.8, y, 0.1) for (x, y, z) in qp], 4), 6.4, 0.6, lambda s: (0.4 + 0.6 * math.sin(math.pi * min(1.0, s * 1.15))) * (1 - s ** 3) + 0.05, (0, 0, 1))
    sweep_flat(k.part("weapon", "vane_edge", "crystal", layer=2), curve([(x + 3.6, y, 0.4) for (x, y, z) in qp], 4), 0.6, 0.3, pf_taper, (0, 0, 1))
    for i in range(5):
        t = 0.2 + 0.16 * i
        p = np.array(qp[min(4, int(t * 4) + 1)])
        spike(k.part("weapon", f"barb_{i}", "plate"), (p[0] + 1.0, p[1], 0), (1.0, 0.1, 0), 3.6 - 0.4 * i, 0.4, 4)
    crow(k, "weapon", "crow", (-2.2, 28.0, 0.0), 0.7, 90.0, "plate", 0.1)
    glow_orb(k, "weapon", "ink", (0.0, 20.0, 1.0), 0.7)
    tassel(k, 14.0, 0.8, 6.0)


def crow(k, piece, name, c, s=1.0, yaw=0.0, mat="plate", wings_up=0.4):
    n0 = len(k.m.parts)
    orb(k.part(piece, name + "_b", mat, smooth=0), (0, 0, 0), 1.6 * s, sides=5)
    orb(k.part(piece, name + "_h", mat, smooth=0), (0, 1.0 * s, 1.8 * s), 1.0 * s, sides=5)
    spike(k.part(piece, name + "_beak", "bone"), (0, 0.9 * s, 2.6 * s), (0, -0.15, 1.0), 2.0 * s, 0.4 * s, 4)
    glow_orb(k, piece, name + "_eye", (0.5 * s, 1.2 * s, 2.4 * s), 0.26 * s, sides=4)
    for sg in (-1, 1):
        lf(k, piece, f"{name}_w{sg}", mat, [(sg * 1.0 * s, 0.2 * s, 0.2 * s), (sg * 3.0 * s, (0.2 + wings_up) * s, -1.6 * s), (sg * 3.4 * s, -1.6 * s, -3.2 * s), (sg * 1.2 * s, -1.4 * s, -1.8 * s)])
    spike(k.part(piece, name + "_t", mat, smooth=0), (0, -0.2 * s, -1.2 * s), (0, -0.2, -1.0), 3.0 * s, 1.0 * s, 4)
    if yaw:
        L.transform_since(k, n0, rot=("y", yaw))
    L.transform_since(k, n0, shift=c)


# ============================================================================================ MURMUR: the death drum mace
@set_weapon("MURMUR")
def weapon_murmur(k):
    """MURMUR_MACE: a mace 2.8 blocks long: a haft of bone, a great drum for the head with a skull on its skin and a ring of flanges round the rim, bones hanging from the neck."""
    dua.w_shaft(k, -10, 26, 0.8, "bone", "haft")
    for j, y in enumerate((-6, -2, 2, 6)):
        dua.w_ring(k, y, 1.15, f"wrap_{j}", "cloth" if j % 2 else "trim", 0.9)
    orb(k.part("weapon", "butt", "plate", smooth=40), (0, -11.4, 0), 1.7, sides=8)
    dua.w_ring(k, 25.0, 1.9, "neck", "trim", 1.8)
    L.drum(k, "weapon", "drum", (0.0, 36.0, 0.0), 8.0, 5.4, "z", "plate", "bone", "trim", 7)
    for i in range(8):
        a = 2 * PI * i / 8
        spike(k.part("weapon", f"flange_{i}", "plate"), (8.6 * math.cos(a), 36.0 + 8.6 * math.sin(a), 0.0), (math.cos(a), math.sin(a), 0.0), 3.6, 0.9, 4)
    L.mask(k, "weapon", "skull", (0.0, 36.0, 3.2), 2.6, 0.0, "bone")
    for i, r_ in enumerate((5.2, 6.6)):
        du.tube_direct(k.part("weapon", f"wave_{i}", "crystal", layer=2, smooth=40), ring_path((0.0, 36.0, 3.0), r_, 0, 360, "xy", 10), 0.2, 0.2, 4)
    for sg in (-1, 1):
        sweep_tube(k.part("weapon", f"cord_{sg}", "cloth", smooth=40), [(sg * 0.8, 24.0, 0.4), (sg * 2.4, 20.0, 0.8), (sg * 1.8, 16.0, 0.6)], 0.3, 0.3, 4)
        orb(k.part("weapon", f"bone_{sg}", "bone", smooth=0), (sg * 1.8, 15.4, 0.6), 0.8, sides=5)

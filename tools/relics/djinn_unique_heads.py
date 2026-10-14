"""djinn_unique_heads: headgear of the twelve Djinn (pieces helm, horn_l, horn_r, ears, crest = the Half Equip brow crest, eye = the Dark Djinn's
brow crest) built from mesh_lib lofts. One builder per Djinn: a distinct silhouette on the 8 px head (the face, y 1..6 on the front, stays open).

Head frame (author space): origin = the neck centre; the head is x +-4, y 0..8, z +-4 (front +z). Eyes at y 3-4, brow at y 5.4, crown at y 8.
"""
import math

import numpy as np

import djinn_unique as du
from djinn_unique import ml, C, S, curve, collar, sweep_tube, leaf, spike, feather, tongue, zig, orb, disc, crescent, ring_path, sweep_flat, pf_flame, pf_feather, pf_blade, pf_taper, pf_ribbon, UP, unit

# ---- named proportions (see the module header of djinn_unique.py for where they come from)
EYE_W, EYE_H = 5.0, 2.4          # ref 62: the eye crest as wide as 0.6 of the head
BROW_Y, BROW_Z = 5.6, 4.55       # the brow line on the front of the head
EYE_Y, EYE_Z = 7.4, BROW_Z + 0.6     # the Dark Djinn's eye crest: above the brow gem, proud of the brow
CROWN_Y = 8.0


def bpath(base, d0, d1, L, n=5):
    """A path from `base` whose heading turns from d0 to d1 (vectors) over length L."""
    d0, d1 = unit(d0), unit(d1)
    p, out = np.asarray(base, float), []
    for i in range(n + 1):
        out.append(tuple(p))
        t = (i + 0.5) / n
        p = p + unit(d0 * (1 - t) + d1 * t) * (L / n)
    return out


def blade3(part, base, d0, d1, L, w, t, wdir=(1, 0, 0), prof=pf_feather, n=5, caps=(False, False)):
    """A flat tapering blade along a turning heading; the flat face's normal is perpendicular to both the heading and `wdir` (the width)."""
    path = bpath(base, d0, d1, L, n)
    mid = unit(np.asarray(d0, float) + np.asarray(d1, float))
    nrm = np.cross(mid, np.asarray(wdir, float))
    if np.linalg.norm(nrm) < 1e-6:
        nrm = np.array([0.0, 0.0, 1.0])
    sweep_flat(part, path, w, t, prof, tuple(unit(nrm)), caps)


def head_base(k, cap="open", cheeks=True, nasal=False, neckguard=True, brim=True, mat="plate"):
    """The skull cap / circlet, its band, neck guard and cheek plates (djinn_parts.build_helm without the ridge, gem and hair)."""
    c = k.part("helm", "cap", mat)
    if cap == "circlet":
        ml.lathe(c, "y", (0, 0, 0), [(5.0, 4.55), (5.5, 4.6), (6.0, 4.55)], sides=S(16), caps=(False, False))
    elif cap == "cowl":
        ml.lathe(c, "y", (0, 0, 0), [(4.7, 4.7), (5.8, 4.9), (7.0, 4.6), (8.2, 3.7), (8.9, 2.2), (9.3, 0.0)], sides=S(16), caps=(False, True))
    elif cap == "dome":
        ml.lathe(c, "y", (0, 0, 0), [(4.6, 4.8), (6.0, 5.1), (7.4, 4.8), (8.8, 3.8), (9.6, 2.2), (10.0, 0.0)], sides=S(16), caps=(False, True))
    elif cap != "none":
        ml.lathe(c, "y", (0, 0, 0), [(4.9, 4.7), (5.8, 4.75), (6.8, 4.45), (7.8, 3.6), (8.6, 2.2), (9.1, 0.0)], sides=S(16), caps=(False, True))
    if cap not in ("circlet", "none") and brim:
        collar(k.part("helm", "brim", k.trim()), 4.7, 4.95, 0.7, lip=0.2, sides=S(16))
    collar(k.part("helm", "band", k.trim()), 5.9 if cap == "circlet" else 5.5, 4.7 if cap == "circlet" else 4.95, 0.6, lip=0.18, sides=S(16))
    if neckguard:
        du.box(k.part("helm", "neckguard", mat), (-3.7, 0.4, -4.95), (3.7, 4.2, -4.2))
    if cheeks:
        for sgn, nm in ((-1, "cheek_r"), (1, "cheek_l")):
            du.box(k.part("helm", nm, mat), (sgn * 4.25 - 0.4, 1.6, -1.2), (sgn * 4.25 + 0.4, 4.6, 3.0))
    if nasal:
        du.bar(k.part("helm", "nasal", k.trim()), (0, 5.4, 4.45), (0, 3.4, 4.55), 0.9, 0.35, up=(0, 0, 1), taper=0.4)


def brow_gem(k, y=BROW_Y, r=0.75, mat="crystal"):
    g = k.part("helm", "brow_gem", mat, layer=2, smooth=0)
    du.gem(g, (0, y, BROW_Z), r, long=1.2, sides=6, axis="z")


def horn_pair(k, path, r0, r1, sides=6, mat="trim", flare=0.5, ring=True, name="horn"):
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        h = k.part(piece, name, mat, smooth=40)
        sweep_tube(h, [(sgn * x, y, z) for (x, y, z) in path], r0, r1, sides, base_flare=flare)
        if ring:
            d = np.array(path[1]) - np.array(path[0])
            o = path[0]
            collar(k.part(piece, "ring", "plate"), 0.0, r0 * 1.3, 0.7, lip=0.2, sides=S(10), axis=(sgn * d[0], d[1], d[2]), origin=(sgn * o[0], o[1], o[2]))


def hair_blades(k, xs, base_y, base_z, L, w, d0, d1, mat="cloth", t=0.5, name="hair", spread=0.5, piece="helm"):
    for i, x in enumerate(xs):
        p = k.part(piece, f"{name}_{i}", mat, smooth=45)
        sp = spread * x / max(1.0, max(abs(v) for v in xs))
        blade3(p, (x, base_y, base_z), (sp, d0[1], d0[2]), (sp * 1.4, d1[1], d1[2]), L * (1 - 0.18 * abs(x) / max(1.0, max(abs(v) for v in xs))), w, t, wdir=(1, 0, 0))


def pointed_ears(k, L=5.5, ang=25.0, w=1.7, rim=True, mat="cloth"):
    for sgn, nm in ((1, "l"), (-1, "r")):
        a = math.radians(ang)
        e = k.part("ears", "ear_" + nm, mat, smooth=45)
        blade3(e, (sgn * 4.2, 3.4, 0.0), (sgn * math.cos(a), math.sin(a), -0.15), (sgn * math.cos(a) * 0.8, math.sin(a) * 1.2, -0.3), L, w, 0.5, wdir=(0, 0, 1))
        if rim:
            r = k.part("ears", "rim_" + nm, k.trim())
            blade3(r, (sgn * 4.3, 3.45, 0.2), (sgn * math.cos(a), math.sin(a), -0.15), (sgn * math.cos(a) * 0.8, math.sin(a) * 1.2, -0.3), L * 0.92, 0.35, 0.7, wdir=(0, 0, 1), prof=pf_taper)


# ============================================================================================ BAAL: the storm diadem and swept hair
def head_baal(k):
    head_base(k, "open", nasal=True)
    brow_gem(k)
    # BAAL_DIADEM: three forked bolts standing from the brow (a lightning crown), the middle one tallest
    for i, x in enumerate((-2.5, 0.0, 2.5)):
        top = 11.2 if x == 0 else 9.9
        zig(k.part("helm", f"bolt_{i}", "crystal", layer=2), [(x, 5.8, 4.5), (x + 0.8, 7.2, 4.55), (x - 0.5, 8.3, 4.5), (x + 0.5, top, 4.4)], 0.9, 0.3)
    # BAAL_MANE: five swept-back spiky blades (the wild hair of the reference, blade-like)
    hair_blades(k, (-3.0, -1.5, 0.0, 1.5, 3.0), 8.2, -0.8, 10.0, 2.4, (0, 0.55, -0.85), (0, 0.15, -1.0), mat="dark", spread=0.8)
    horn_pair(k, [(3.0, 6.2, 0.2), (4.4, 8.4, -0.8), (4.6, 10.9, -2.6), (3.2, 12.8, -5.0)], 1.25, 0.12)
    pointed_ears(k, 5.8, 20.0)
    # half crest: one small bolt over the brow gem
    zig(k.part("crest", "bolt", "crystal", layer=2), [(0, 6.3, 4.55), (0.7, 7.4, 4.6), (-0.4, 8.2, 4.55), (0.4, 9.6, 4.5)], 0.9, 0.3)
    g = k.part("crest", "jewel", "crystal", layer=2, smooth=0)
    du.gem(g, (0, 5.7, BROW_Z), 0.8, long=1.2, sides=6, axis="z")
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ AMON: the flame crown and ram horns
def head_amon(k):
    head_base(k, "circlet")
    du.bar(k.part("helm", "nasal", k.trim()), (0, 5.2, 4.45), (0, 3.8, 4.55), 0.8, 0.3, up=(0, 0, 1), taper=0.3)
    brow_gem(k, 6.0, 0.8)
    # AMON_FLAMES: seven flame tongues round the circlet, tall in front and swept back, glowing
    for i, a in enumerate(np.linspace(-80, 80, 7)):
        r = math.radians(a)
        base = (math.sin(r) * 4.2, 6.0, math.cos(r) * 4.2)
        L = 5.2 - abs(a) * 0.03 + (1.2 if i == 3 else 0)
        tg = k.part("helm", f"flame_{i}", "crystal", glow=True, smooth=45)
        tongue(tg, base, (math.sin(r) * 0.25, 1.0, math.cos(r) * 0.15 - 0.35), L, 1.7 - abs(a) * 0.006, 0.5, wobble=0.8, phase=i * 1.3, plane="xy" if i % 2 else "yz", n=4)
    hair_blades(k, (-3.0, 0.0, 3.0), 7.0, -2.2, 6.0, 2.2, (0, 0.5, -0.85), (0, -0.1, -1.0), mat="cloth", spread=0.4, name="tuft")
    horn_pair(k, du_ram(), 1.4, 0.2, 7, "extra", 0.4)
    # half crest: three tiny flames
    for i, x in enumerate((-1.5, 0.0, 1.5)):
        tg = k.part("crest", f"flame_{i}", "crystal", glow=True, smooth=45)
        tongue(tg, (x, 6.0, 4.5), (0, 1.0, 0.1), 3.6 - abs(x), 1.4, 0.4, wobble=0.5, phase=i, plane="xy")
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


def du_ram():
    return [(3.4, 5.6, 0.0), (5.4, 6.6, -1.0), (6.9, 5.2, -2.3), (6.6, 2.9, -1.8), (5.2, 1.9, 0.2)]


# ============================================================================================ VINEA: the fin tiara and coral
def head_vinea(k):
    head_base(k, "circlet")
    # VINEA_SAIL: a fan fin standing on the brow: a membrane between five spreading ribs, a pearl at its foot
    mem = k.part("helm", "sail", "cloth", uv_mode="dominant")
    leaf(mem, [(-3.3, 6.2, 4.3), (-2.8, 9.0, 3.9), (-1.2, 11.2, 3.5), (0.0, 12.0, 3.4), (1.2, 11.2, 3.5), (2.8, 9.0, 3.9), (3.3, 6.2, 4.3)])
    for i, x in enumerate((-3.0, -1.5, 0.0, 1.5, 3.0)):
        top = (x * 0.85, 11.6 - abs(x) * 0.8, 3.45)
        du.bar(k.part("helm", f"rib_{i}", "crystal", glow=(i == 2)), (x * 0.5, 6.3, 4.35), top, 0.34, 0.14, up=(0, 0, 1), taper=0.6)
    pearl = k.part("helm", "pearl", "bone", smooth=0)
    orb(pearl, (0, 6.2, 4.7), 0.95)
    # VINEA_FINS: fish-fin ears: membranes with ribs spreading back from the temples
    for sgn, nm in ((1, "l"), (-1, "r")):
        e = k.part("ears", "fin_" + nm, "cloth")
        leaf(e if sgn > 0 else e, ([(sgn * 4.2, 2.6, 0.5), (sgn * 7.8, 6.4, -0.5), (sgn * 9.6, 5.0, -1.5), (sgn * 8.4, 2.2, -1.2), (sgn * 5.6, 0.8, 0.0)] if sgn > 0 else
                                   [(sgn * 5.6, 0.8, 0.0), (sgn * 8.4, 2.2, -1.2), (sgn * 9.6, 5.0, -1.5), (sgn * 7.8, 6.4, -0.5), (sgn * 4.2, 2.6, 0.5)]))
        for j, (ex, ey) in enumerate(((7.8, 6.2), (9.4, 4.8), (8.3, 2.3))):
            du.bar(k.part("ears", f"fin_rib_{nm}{j}", "crystal", glow=(j == 1)), (sgn * 4.4, 2.6, 0.5), (sgn * ex, ey, -0.6 - 0.3 * j), 0.3, 0.12, up=(0, 0, 1), taper=0.5)
    # VINEA_CORAL: branching coral antlers instead of horns
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        main = [(sgn * 3.0, 6.0, -0.4), (sgn * 3.6, 8.4, -1.0), (sgn * 3.4, 10.6, -1.6)]
        sweep_tube(k.part(piece, "coral", "bone", smooth=40), main, 0.9, 0.12, 6)
        for j, (dx, dy, dz) in enumerate(((1.8, 2.0, -0.6), (-1.5, 1.8, -0.2))):
            b = [main[1], (main[1][0] + sgn * dx * 0.6, main[1][1] + dy * 0.5, main[1][2] + dz * 0.5), (main[1][0] + sgn * dx, main[1][1] + dy, main[1][2] + dz)]
            sweep_tube(k.part(piece, f"twig_{j}", "bone", smooth=40), b, 0.55, 0.08, 5)
    hair_blades(k, (-2.0, 2.0), 7.4, -2.6, 20.0, 2.4, (0, 0.2, -1.0), (0, -1.0, -0.4), mat="cloth", spread=0.3, name="wave")
    # half crest: a small fan fin with the pearl
    mem2 = k.part("crest", "sail", "cloth")
    leaf(mem2, [(-1.9, 6.2, 4.4), (-1.5, 8.0, 4.1), (0, 9.0, 3.9), (1.5, 8.0, 4.1), (1.9, 6.2, 4.4)])
    orb(k.part("crest", "pearl", "bone", smooth=0), (0, 6.0, 4.7), 0.8)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ FOCALOR: the wind plume and temple wings
def head_focalor(k):
    head_base(k, "open", nasal=False)
    brow_gem(k, 6.2, 0.7)
    # FOCALOR_PLUME: four long curved blades flowing back from the crown like a gale-blown crest
    for i, x in enumerate((-2.4, -0.8, 0.8, 2.4)):
        p = k.part("helm", f"plume_{i}", "cloth", smooth=45)
        blade3(p, (x, 8.4, 0.6), (x * 0.1, 0.9, -0.3), (x * 0.25, -0.3, -1.0), 15.0 - 2.0 * abs(x) / 2.4, 2.0, 0.45, wdir=(1, 0, 0), n=7)
        k.part("helm", f"plume_edge_{i}", "crystal", layer=2)
    # FOCALOR_TEMPLE_WINGS: two feather fans at the temples
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        for j in range(4):
            f = k.part(piece, f"feather_{j}", "plate", smooth=45)
            a = 28 - j * 17
            blade3(f, (sgn * 4.3, 5.2 - j * 0.2, 0.4), (sgn * math.cos(math.radians(a)), math.sin(math.radians(a)), -0.5), (sgn * math.cos(math.radians(a)) * 0.6, math.sin(math.radians(a)) * 1.1, -1.0), 7.0 - j * 0.7, 1.5, 0.4, wdir=(0, 0, 1))
        sweep_tube(k.part(piece, "quill", "trim"), [(sgn * 4.0, 5.0, 0.3), (sgn * 5.2, 5.4, 0.0)], 0.4, 0.3, 5)
    pointed_ears(k, 5.0, 12.0, 1.4, mat="plate")
    for i, x in enumerate((-1.2, 1.2)):
        p = k.part("crest", f"feather_{i}", "plate", smooth=45)
        blade3(p, (x, 6.2, 4.4), (x * 0.6, 0.9, -0.2), (x * 1.2, 0.5, -0.8), 4.4, 1.3, 0.4, wdir=(1, 0, 0))
    g = k.part("crest", "jewel", "crystal", layer=2, smooth=0)
    du.gem(g, (0, 5.9, BROW_Z), 0.8, long=1.2, sides=6, axis="z")
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ ZAGAN: antlers with leaves and a flower bud crown
def head_zagan(k):
    head_base(k, "circlet", mat="plate")
    # ZAGAN_BLOOM: a closed flower on the brow: six petals round a bud
    bud = k.part("helm", "bud", "crystal", layer=2, smooth=0)
    du.gem(bud, (0, 6.4, 4.7), 0.8, long=1.1, sides=6, axis="z")
    for i in range(6):
        a = i * math.pi / 3
        pt = k.part("helm", f"petal_{i}", "cloth", smooth=45)
        blade3(pt, (math.cos(a) * 0.7, 6.4 + math.sin(a) * 0.7, 4.6), (math.cos(a) * 0.7, math.sin(a) * 0.7, 0.35), (math.cos(a) * 0.5, math.sin(a) * 0.5, 0.2), 2.3, 1.3, 0.35, wdir=(-math.sin(a), math.cos(a), 0), n=3, prof=pf_blade)
    # ZAGAN_ANTLERS: tall stag antlers, three tines a side, a leaf at each tip
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        main = [(sgn * 2.4, 6.6, -0.2), (sgn * 3.4, 9.4, -0.5), (sgn * 4.0, 12.4, -0.9), (sgn * 3.6, 15.4, -1.5)]
        sweep_tube(k.part(piece, "antler", "extra", smooth=40), main, 1.15, 0.12, 6, base_flare=0.5)
        for j, (si, dx, dy, dz) in enumerate(((1, 2.4, 2.0, 0.2), (2, -2.0, 1.8, 0.0), (1, 2.8, 2.6, -0.4))):
            a = np.array(main[si])
            b = [tuple(a), tuple(a + np.array([sgn * dx * 0.5, dy * 0.5, dz * 0.5])), tuple(a + np.array([sgn * dx, dy, dz]))]
            sweep_tube(k.part(piece, f"tine_{j}", "extra", smooth=40), b, 0.6, 0.08, 5)
            lf = k.part(piece, f"leaf_{j}", "cloth")
            e = np.array(b[2])
            P = [e, e + np.array([sgn * 0.9, 0.7, 0.4]), e + np.array([sgn * 0.2, 2.2, 0.1]), e + np.array([-sgn * 0.7, 0.8, -0.3])]
            leaf(lf, P if sgn > 0 else P[::-1])
        collar(k.part(piece, "ring", "trim"), 0.0, 1.5, 0.7, lip=0.2, sides=S(10), axis=(sgn * 1.0, 2.6, 0.3), origin=(sgn * 2.4, 6.6, -0.2))
    pointed_ears(k, 5.4, 32.0, 2.0, mat="cloth")
    hair_blades(k, (-2.6, 0.0, 2.6), 7.6, -3.0, 18.0, 2.2, (0, 0.1, -1.0), (0, -1.0, -0.2), mat="cloth", spread=0.3, name="vine")
    # half crest: the closed bud on a small circlet with two leaves
    orb(k.part("crest", "bud", "crystal", layer=2, smooth=0), (0, 6.2, 4.7), 0.9)
    for sgn in (-1, 1):
        lf = k.part("crest", "leaf_" + ("l" if sgn > 0 else "r"), "cloth")
        P = [(sgn * 0.8, 6.0, 4.5), (sgn * 2.8, 6.6, 4.3), (sgn * 4.2, 8.4, 3.6), (sgn * 2.2, 7.6, 4.2)]
        leaf(lf, P if sgn > 0 else P[::-1])
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ ZEPAR: the great helm and ram horns
def head_zepar(k):
    head_base(k, "dome", nasal=False)
    # ZEPAR_BROW: a heavy brow slab, a mouth guard with vents and a back plume of three thick spikes (the eyes stay free: y 3-4)
    du.box(k.part("helm", "brow", "extra"), (-4.4, 5.0, 3.8), (4.4, 6.4, 4.9), chamfer=0.4)
    du.box(k.part("helm", "mouthguard", "plate"), (-3.9, 0.8, 4.0), (3.9, 2.8, 4.8), chamfer=0.5)
    for i in (-2, 0, 2):
        du.bar(k.part("helm", f"vent_{i}", "dark"), (i * 0.9, 1.1, 4.85), (i * 0.9, 2.5, 4.85), 0.4, 0.12, up=(0, 0, 1))
    for i, x in enumerate((-2.2, 0.0, 2.2)):
        sp = k.part("helm", f"plume_{i}", "trim")
        spike(sp, (x, 9.4, -1.0 - abs(x) * 0.3), (x * 0.1, 1.0, -0.4), 4.4 - abs(x) * 0.5, 1.2, 6)
    for sgn, nm in ((1, "l"), (-1, "r")):
        du.box(k.part("helm", "jaw_" + nm, "plate"), (sgn * 4.0 - 0.5, 0.0, 0.8), (sgn * 4.0 + 0.5, 2.0, 4.4), chamfer=0.3)
    # ZEPAR_RAMS: thick curled ram horns with three ridges
    path = [(3.6, 5.8, 0.2), (6.2, 6.8, -1.2), (8.2, 5.2, -2.8), (7.8, 2.2, -2.4), (5.6, 1.4, 0.0)]
    horn_pair(k, path, 2.0, 0.25, 8, "bone", 0.35, ring=False)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        pts = curve([(sgn * x, y, z) for (x, y, z) in path], 8)
        for j in range(3):
            c = pts[int(len(pts) * (0.22 + 0.17 * j))]
            collar(k.part(piece, f"ridge_{j}", "extra"), 0, 1.7 - 0.28 * j, 0.5, lip=0.1, sides=S(8), axis=(0, 1, 0), origin=tuple(c))
    # half crest: a broad brow plate with a stud
    du.box(k.part("crest", "plate", "extra"), (-3.2, 5.2, 4.0), (3.2, 6.4, 4.9), chamfer=0.35)
    du.stud(k.part("crest", "stud", "trim"), (0, 5.8, 4.9), 0.7, 0.5, (0, 0, 1), 8)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ FURFUR: the bell crown and the speaker ear-cups
def head_furfur(k):
    head_base(k, "circlet")
    brow_gem(k, 6.0, 0.7)
    # FURFUR_BELLS: a ring of five bells standing on the circlet (cone, lip, clapper)
    for i, a in enumerate(np.linspace(-72, 72, 5)):
        r = math.radians(a)
        x, z = math.sin(r) * 4.55, math.cos(r) * 4.55
        b = k.part("helm", f"bell_{i}", k.trim())
        h = 4.8 - abs(a) * 0.012
        ml.loft(b, (math.sin(r) * 0.15, 1.0, math.cos(r) * 0.15), (x, 6.0, z), [(0, 0.5, 0.5), (h * 0.5, 0.8, 0.8), (h * 0.9, 1.5, 1.5), (h, 1.7, 1.7)], C(1.0, S(10)), caps=(False, False))
        du.gem(k.part("helm", f"clapper_{i}", "crystal", layer=2, smooth=0), (x + math.sin(r) * 0.15 * h, 6.0 + h + 0.35, z + math.cos(r) * 0.15 * h), 0.45, long=1.0, sides=5)
    # FURFUR_EARCUPS: a speaker disc over each ear with two concentric ridges
    for sgn, nm in ((1, "l"), (-1, "r")):
        cup = k.part("ears", "cup_" + nm, "plate")
        ml.lathe(cup, "x", (sgn * 4.0, 3.2, 0.2), [(0.0, 2.3), (sgn * 0.5, 2.55), (sgn * 1.0, 2.4), (sgn * 1.6, 1.6)], sides=S(14), caps=(False, True))
        for j, r in enumerate((1.9, 1.15)):
            collar(k.part("ears", f"ring_{nm}{j}", k.trim()), sgn * 1.45 + sgn * 0.0, r, 0.3, lip=0.08, sides=S(14), axis=(sgn, 0, 0), origin=(sgn * 4.0, 3.2, 0.2))
        du.gem(k.part("ears", "core_" + nm, "crystal", layer=2, smooth=0), (sgn * 5.9, 3.2, 0.2), 0.6, long=0.8, sides=6, axis=(sgn, 0, 0))
    # FURFUR_HORNS: two trumpet horns that curve up and out and flare into a bell
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        path = [(sgn * 3.2, 6.4, -0.4), (sgn * 4.8, 8.4, -1.6), (sgn * 6.4, 9.8, -2.4), (sgn * 8.4, 10.2, -1.8), (sgn * 10.0, 9.6, -0.2)]
        pts = curve(path, 8)
        du.sweep(k.part(piece, "trumpet", "extra", smooth=40), pts, C(1.0, S(10)), scales=lambda t: (0.55 + 0.1 * t + 2.4 * max(0.0, t - 0.7) ** 1.4 / 0.3 ** 1.4 * 0.5,) * 2)
        collar(k.part(piece, "lip", k.trim()), 0, 1.7, 0.4, lip=0.1, sides=S(10), axis=(sgn * 1.0, -0.2, 1.2), origin=path[-1])
    # half crest: three small bells
    for i, x in enumerate((-1.8, 0.0, 1.8)):
        h = 2.4 - abs(x) * 0.25
        ml.loft(k.part("crest", f"bell_{i}", k.trim()), "y", (x, 6.0, 4.5), [(0, 0.35, 0.35), (h * 0.6, 0.7, 0.7), (h, 1.0, 1.0)], C(1.0, S(8)), caps=(False, False))
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ MARCHOSIAS: the wolf cowl
def head_marchosias(k):
    head_base(k, "cowl", nasal=True)
    # MARCHOSIAS_EARS: tall wolf ears on the cowl, an inner ember blade in each
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        ml.loft(k.part(piece, "wolf_ear", "plate", smooth=40), "y", (sgn * 2.7, 7.6, -0.4), [(0, 1.0, 1.0), (2.8, 0.85, 0.9), (5.4, 0.0, 0.0)], ml.lozenge(3.6, 1.1), up=(0, 0, 1), caps=(True, False))
        ml.loft(k.part(piece, "ear_ember", "crystal", layer=2), "y", (sgn * 2.7, 7.8, 0.15), [(0, 0.7, 1.0), (2.5, 0.6, 1.0), (4.6, 0.0, 0.0)], ml.lozenge(2.0, 0.5), up=(0, 0, 1), caps=(True, False))
        # swept spike behind each ear
        spike(k.part(piece, "back_spike", k.trim()), (sgn * 4.0, 6.6, -2.4), (sgn * 0.5, 0.5, -1.0), 4.8, 0.9, 6)
    # MARCHOSIAS_FANGS: two fangs hanging from a brow ridge, and cheek fur tufts
    du.box(k.part("helm", "brow", "extra"), (-3.6, 5.5, 4.0), (3.6, 6.4, 4.9), chamfer=0.4)
    for sgn in (-1, 1):
        spike(k.part("helm", "fang_" + ("l" if sgn > 0 else "r"), "bone"), (sgn * 1.7, 5.7, 4.9), (sgn * 0.05, -1.0, 0.1), 2.6, 0.7, 6)
    for sgn, nm in ((1, "l"), (-1, "r")):
        for j in range(3):
            spike(k.part("helm", f"fur_{nm}{j}", "cloth"), (sgn * 4.3, 3.4 - j * 0.5, 2.4 - j * 1.4), (sgn * 0.6, -0.9, 0.3 - 0.5 * j), 3.0 - 0.3 * j, 0.9, 5)
    du.gem(k.part("helm", "brow_gem", "crystal", layer=2, smooth=0), (0, 5.95, 4.95), 0.6, long=1.0, sides=6, axis="z")
    hair_blades(k, (-2.2, 0.0, 2.2), 8.0, -3.2, 7.0, 2.2, (0, 0.2, -1.0), (0, -0.6, -1.0), mat="cloth", spread=0.4, name="tuft")
    # half crest: a fang pair under a small brow ridge
    du.box(k.part("crest", "brow", "extra"), (-2.8, 5.5, 4.0), (2.8, 6.2, 4.8), chamfer=0.3)
    for sgn in (-1, 1):
        spike(k.part("crest", "fang_" + ("l" if sgn > 0 else "r"), "bone"), (sgn * 1.3, 5.6, 4.8), (0, -1.0, 0.1), 2.0, 0.55, 6)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ SITRI: the sun crown and buns
def head_sitri(k):
    head_base(k, "circlet")
    brow_gem(k, 6.1, 0.85)
    # SITRI_BUNS: three round buns on top (ref 61), each ringed in gold
    for i, x in enumerate((-2.0, 0.0, 2.0)):
        orb(k.part("helm", f"bun_{i}", "trim", smooth=40), (x, 9.6 - abs(x) * 0.25, -0.8), 1.35, sides=8)
        collar(k.part("helm", f"bun_ring_{i}", "plate"), 8.5 - abs(x) * 0.2, 1.1, 0.4, lip=0.1, sides=S(8), origin=(x, 0, -0.8))
    # SITRI_RAYS: short rays standing from the circlet at the front
    for i, a in enumerate(np.linspace(-80, 80, 9)):
        r = math.radians(a)
        spike(k.part("helm", f"ray_{i}", "crystal", layer=2), (math.sin(r) * 4.5, 6.0, math.cos(r) * 4.5), (math.sin(r) * 0.55, 1.0, math.cos(r) * 0.55), 3.0 - abs(a) * 0.015, 0.45, 5)
    # SITRI_HAIR: a sheet of long hair falling down the back, in three panels
    for i, x in enumerate((-2.6, 0.0, 2.6)):
        p = k.part("helm", f"hair_{i}", "cloth", uv_mode="loft")
        pth = [(x, 8.0, -3.0), (x * 1.2, 5.0, -5.2), (x * 1.5, 0.0, -6.0), (x * 1.8, -8.0, -5.6), (x * 2.2, -18.0, -4.8), (x * 2.5, -26.0, -4.0)]
        du.sweep(p, curve(pth, 6), ml.rect_ring(1.0, 0.35), scales=lambda t: (3.4 - 1.4 * t, 1.0))
    # SITRI_SCROLLS: gold scroll clasps over the ears; pointed ears
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        sweep_tube(k.part(piece, "scroll", k.trim()), [(sgn * 4.5, 4.4, 0.6), (sgn * 6.2, 5.6, 0.4), (sgn * 7.0, 4.0, 0.2), (sgn * 5.8, 2.8, 0.2), (sgn * 5.0, 3.6, 0.4)], 0.55, 0.3, 6)
        du.gem(k.part(piece, "scroll_gem", "crystal", layer=2, smooth=0), (sgn * 5.3, 3.5, 0.5), 0.5, long=0.8, sides=6, axis="z")
    pointed_ears(k, 4.4, 28.0, 1.6, mat="plate")
    # half crest: the brow jewel in a sun of five rays
    du.gem(k.part("crest", "jewel", "crystal", layer=2, smooth=0), (0, 5.9, BROW_Z), 0.9, long=1.3, sides=6, axis="z")
    for i, a in enumerate(np.linspace(-60, 60, 5)):
        r = math.radians(a)
        spike(k.part("crest", f"ray_{i}", "crystal", layer=2), (math.sin(r) * 1.2, 6.0 + math.cos(r) * 0.4, 4.6), (math.sin(r) * 1.0, math.cos(r) * 1.0, 0.1), 2.2, 0.35, 5)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ BIFRON: the storm visor and forked antlers
def head_bifron(k):
    head_base(k, "open", nasal=False)
    # BIFRON_VISOR: a faceted storm mask over the lower face (the eyes stay free) with a crystal eye slit above it
    du.box(k.part("helm", "mask", "plate"), (-3.9, 0.9, 4.0), (3.9, 3.0, 4.7), chamfer=0.55)
    for i in (-2, 0, 2):
        du.bar(k.part("helm", f"mask_bolt_{i}", "crystal", layer=2), (i * 0.9, 1.1, 4.75), (i * 0.9 + 0.3, 2.7, 4.75), 0.3, 0.1, up=(0, 0, 1))
    brow_gem(k, 6.0, 0.7)
    # BIFRON_ANTLERS: zig-zag forked horns, the bars glowing
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        zig(k.part(piece, "fork", "plate"), [(sgn * 3.0, 6.0, 0.0), (sgn * 4.4, 8.0, -0.4), (sgn * 3.4, 9.6, -0.8), (sgn * 5.0, 12.0, -1.2), (sgn * 4.0, 14.4, -1.6)], 1.2, 0.5, up=(0, 0, 1))
        zig(k.part(piece, "prong", "crystal", layer=2), [(sgn * 4.4, 8.0, -0.4), (sgn * 6.2, 9.2, -0.6), (sgn * 7.2, 11.4, -0.8)], 0.8, 0.3, up=(0, 0, 1))
        zig(k.part(piece, "prong2", "crystal", layer=2), [(sgn * 5.0, 12.0, -1.2), (sgn * 3.0, 13.0, -1.2), (sgn * 3.4, 15.4, -1.4)], 0.7, 0.28, up=(0, 0, 1))
    for sgn, nm in ((1, "l"), (-1, "r")):
        e = k.part("ears", "fin_" + nm, "plate", smooth=45)
        blade3(e, (sgn * 4.2, 3.2, 0.0), (sgn * 1.0, 0.1, -0.3), (sgn * 0.8, -0.3, -1.0), 7.0, 1.8, 0.5, wdir=(0, 1, 0), n=4)
    hair_blades(k, (-2.4, 0.0, 2.4), 8.0, -1.4, 8.0, 2.0, (0, 0.6, -0.8), (0, -0.2, -1.0), mat="cloth", spread=0.5, name="spike")
    # half crest: a V of two small bolts
    for sgn in (-1, 1):
        zig(k.part("crest", "bolt_" + ("l" if sgn > 0 else "r"), "crystal", layer=2), [(sgn * 0.6, 5.8, 4.55), (sgn * 1.4, 7.0, 4.55), (sgn * 0.9, 7.8, 4.5), (sgn * 2.0, 9.2, 4.4)], 0.8, 0.28)
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ PAIMON: the crescent circlet and the white mane
def head_paimon(k):
    head_base(k, "circlet")
    # PAIMON_MOON: a crescent moon on the brow, horns up
    cres = k.part("helm", "moon", "trim")
    crescent(cres, (0, 8.0, 4.4), 2.8, 1.5, 0.5, 210, 330, "xy", 10)
    du.gem(k.part("helm", "moon_gem", "crystal", layer=2, smooth=0), (0, 6.1, 4.65), 0.7, long=1.1, sides=6, axis="z")
    # PAIMON_MANE: a tall wild mane of nine swept blades (ref 59 tile 9: white hair streaming back and up)
    for i, x in enumerate(np.linspace(-3.6, 3.6, 9)):
        p = k.part("helm", f"mane_{i}", "cloth", smooth=45)
        up = 1.0 - abs(x) / 5.0
        blade3(p, (x, 8.0, 0.8 - abs(x) * 0.1), (x * 0.12, 0.8 + 0.2 * up, -0.35), (x * 0.3, 0.1, -1.0), 10.0 + 4.0 * up, 1.9, 0.45, wdir=(1, 0, 0), n=6)
    # PAIMON_WINGHORNS: two crescent wing-horns sweeping up and back
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        p = k.part(piece, "crescent", "plate", smooth=45)
        path = [(sgn * 3.4, 6.2, 0.0), (sgn * 5.0, 8.4, -0.6), (sgn * 5.4, 11.0, -1.8), (sgn * 4.0, 13.4, -3.4), (sgn * 2.2, 14.4, -5.0)]
        sweep_flat(p, curve(path, 4), 1.9, 0.5, lambda s: (0.5 + 0.5 * math.sin(math.pi * min(1.0, s * 1.05))) * (1 - 0.3 * s), (1, 0, 0) if False else (0, 0, 1))
        sweep_tube(k.part(piece, "spine", k.trim()), path[:3], 0.45, 0.3, 5)
    pointed_ears(k, 4.8, 22.0, 1.6, mat="plate")
    # half crest: a small moon
    crescent(k.part("crest", "moon", "trim"), (0, 7.2, 4.4), 1.8, 1.0, 0.4, 210, 330, "xy", 8)
    du.gem(k.part("crest", "jewel", "crystal", layer=2, smooth=0), (0, 5.9, BROW_Z), 0.7, long=1.1, sides=6, axis="z")
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


# ============================================================================================ VASSAGO: the bone mask and the abyssal ram horns
def head_vassago(k):
    head_base(k, "cowl", mat="plate", nasal=False)
    # VASSAGO_SKULL: a skull plate across the forehead and brow: two glowing sockets, a nasal slit and a row of teeth along the lower edge
    du.box(k.part("helm", "skull", "bone"), (-3.8, 5.0, 4.0), (3.8, 7.4, 4.9), chamfer=0.8)
    for sgn in (-1, 1):
        du.gem(k.part("helm", "socket_" + ("l" if sgn > 0 else "r"), "crystal", layer=2, smooth=0), (sgn * 1.9, 6.3, 5.0), 0.8, long=1.0, sides=6, axis="z")
    du.bar(k.part("helm", "nose", "dark"), (0, 6.5, 4.95), (0, 5.3, 4.95), 0.7, 0.2, up=(0, 0, 1))
    for i, x in enumerate((-2.8, -1.4, 0.0, 1.4, 2.8)):
        spike(k.part("helm", f"tooth_{i}", "bone"), (x, 5.1, 4.85), (0, -1.0, 0.1), 1.3 - 0.15 * abs(x) / 1.4, 0.38, 5)
    # VASSAGO_HORNS: heavy back-curling horns with glowing ring bands (the deep)
    path = [(3.4, 6.2, -0.4), (5.8, 7.4, -1.8), (7.6, 6.2, -3.6), (7.4, 3.2, -4.2), (5.6, 1.6, -3.0)]
    horn_pair(k, path, 1.7, 0.18, 7, "bone", 0.4, ring=False)
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        pts = curve([(sgn * x, y, z) for (x, y, z) in path], 8)
        for j in range(3):
            c = pts[int(len(pts) * (0.2 + 0.18 * j))]
            collar(k.part(piece, f"band_{j}", "crystal", layer=2), 0, 1.45 - 0.25 * j, 0.35, lip=0.08, sides=S(8), axis=(0, 1, 0), origin=tuple(c))
    # VASSAGO_TENTACLES: four dangling tentacles from the back of the cowl, a bead on each
    for i, x in enumerate((-2.8, -1.0, 1.0, 2.8)):
        p = k.part("helm", f"tentacle_{i}", "cloth", uv_mode="loft")
        pth = [(x, 5.0, -4.6), (x * 1.2, 2.0, -6.0), (x * 1.4 + 1.0, -2.0, -6.4), (x * 1.0 - 1.0, -6.0, -5.6), (x * 1.3, -10.0 - i % 2 * 2.0, -5.0)]
        sweep_tube(p, pth, 0.8, 0.12, 6)
        orb(k.part("helm", f"bead_{i}", "crystal", layer=2, smooth=0), pth[2], 0.55, sides=6)
    # half crest: a small skull plate with two sockets
    du.box(k.part("crest", "skull", "bone"), (-2.8, 5.2, 4.0), (2.8, 6.9, 4.8), chamfer=0.6)
    for sgn in (-1, 1):
        du.gem(k.part("crest", "socket_" + ("l" if sgn > 0 else "r"), "crystal", layer=2, smooth=0), (sgn * 1.4, 6.0, 4.9), 0.55, long=1.0, sides=6, axis="z")
    collar(k.part("crest", "circlet", k.trim()), 5.2, 4.6, 0.55, lip=0.15, sides=S(16))


HEADS = {"BAAL": head_baal, "AMON": head_amon, "VINEA": head_vinea, "FOCALOR": head_focalor, "ZAGAN": head_zagan, "ZEPAR": head_zepar,
         "FURFUR": head_furfur, "MARCHOSIAS": head_marchosias, "SITRI": head_sitri, "BIFRON": head_bifron, "PAIMON": head_paimon, "VASSAGO": head_vassago}


# ============================================================================================ the Dark Djinn's eye crest (ref 62)
def build_eye(k):
    """The brow crest of the Dark Djinn (ref 62): a black chevron of two swept wings meeting over an oval of gold, an eye in the oval whose pupil glows
    in the Djinn's own colour. Reference 62: the wings are 190 px wide on a 210 px eye distance (about 0.6 of the head), the oval a third of that.
    Standing 0.5 px proud of the brow so the helm and the hair do not hide it."""
    y0, z = EYE_Y, EYE_Z
    for sgn, nm in ((1, "l"), (-1, "r")):
        w = k.part("eye", "wing_" + nm, "plate", smooth=40)
        pth = [(sgn * 0.9, y0 - 0.2, z), (sgn * 2.2, y0 + 0.2, z - 0.1), (sgn * 3.6, y0 + 1.3, z - 0.4), (sgn * 5.0, y0 + 2.9, z - 1.0)]
        sweep_flat(w, curve(pth, 4), 2.2, 0.7, lambda s: (0.9 + 0.1 * s) * (1 - s ** 3), (0, 0, 1), caps=(True, False))
        e = k.part("eye", "edge_" + nm, k.trim())
        sweep_flat(e, curve([(sgn * 1.0, y0 - 0.9, z + 0.1), (sgn * 2.3, y0 - 0.5, z), (sgn * 3.7, y0 + 0.6, z - 0.3), (sgn * 5.0, y0 + 2.1, z - 0.9)], 4), 0.6, 0.4, lambda s: 1 - s ** 2, (0, 0, 1))
    ring = k.part("eye", "ring", k.trim())
    ml.lathe(ring, "z", (0, y0, z - 0.1), [(0.0, 1.05), (0.4, 1.2), (0.8, 1.1), (1.0, 0.7)], sides=S(14), caps=(True, False), squash=1.45)
    white = k.part("eye", "white", "bone", smooth=0)
    ml.lathe(white, "z", (0, y0, z + 0.8), [(0.0, 0.78), (0.3, 0.7), (0.4, 0.0)], sides=S(12), caps=(True, False), squash=1.5)
    pup = k.part("eye", "pupil", "crystal", layer=2, smooth=0)
    du.gem(pup, (0, y0, z + 1.2), 0.42, long=0.8, sides=6, axis="z")

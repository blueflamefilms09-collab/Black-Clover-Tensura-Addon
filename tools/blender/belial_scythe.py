"""Belial's scythe (Magi: Hakuryuu Ren's Djinn BELIAL), the rework of the Belial weapon: a WHITE scythe, a long pole of bone-white lacquer, a round vertebra-ring
emblem (a coiled bone dragon holding a teal dragon-eye) at the base of the curved blade, a thin silver-white crescent with a teal-lit fuller, a spine of bone
thorns, bead-and-linen tassels and a claw butt. Made in Blender with tools/blender/forms_kit.py.

    python3 -B tools/blender/belial_scythe.py                    # bake into the mod's asset folders
    python3 -B tools/blender/belial_scythe.py --trial DIR        # write into DIR instead (nothing in the repo changes)
    python3 -B tools/blender/belial_scythe.py --quick            # no bake: quad estimate + parts table
    python3 -B tools/blender/belial_scythe.py --beauty DIR       # Cycles stills of the build into DIR (before the bake)

mesh id: djinn_weapon_belial_scythe (size class `scythe`; header `place` of vassal_scythe, the same hand and display scale).
`build(lod, S, mirror)` is also used by belial_bone.py for the in-hand `weapon` piece of the djinn/belial_bone set (lod 1, scaled S, head toward +x).
Seeded: the same seed gives the same weapon.
"""
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import forms_kit as K  # noqa: E402
from forms_kit import V, rgb, TAU  # noqa: E402

bl = K.bl
MESH_ID = "djinn_weapon_belial_scythe"
BASE = "vassal_scythe"          # header (place) and apparent-length measure come from the base scythe


# ================================================================ materials
def surf(S, name, base, metal=0.0, rough=0.5, **kw):
    """K.surface with every length parameter scaled by S (a model built at S times the cm size keeps the same look)."""
    kw["scale"] = kw.get("scale", 1.0) / S
    kw["bevel"] = kw.get("bevel", 0.16) * S
    kw["ao_dist"] = kw.get("ao_dist", 1.5) * S
    kw["wear_radius"] = kw.get("wear_radius", 0.55) * S
    return K.surface(name, base, metal, rough, **kw)


def palette(S=1.0):
    P = {}
    P["bone"] = surf(S, "bl_bone", rgb(232, 226, 210), 0.0, 0.52, bump="grain", bump_scale=1.2, bump_strength=0.28, mottle=0.8, mottle_color=rgb(178, 162, 134), wear=0.8,
                     wear_color=rgb(252, 250, 244), wear_metal=0.0, dirt=0.85, dirt_color=(0.07, 0.06, 0.05, 1), scratches=0.4, fine=0.6, seed=51, bevel=0.14, ao_dist=2.2)
    P["bone_smooth"] = surf(S, "bl_bone_smooth", rgb(238, 233, 220), 0.0, 0.32, bump="grain", bump_scale=1.6, bump_strength=0.1, mottle=0.5, mottle_color=rgb(196, 184, 160), wear=0.7,
                            wear_color=rgb(255, 253, 248), wear_metal=0.0, dirt=0.6, dirt_color=(0.07, 0.06, 0.05, 1), fine=0.3, seed=52, bevel=0.1)
    P["steel"] = surf(S, "bl_steel", rgb(150, 170, 186), 0.55, 0.42, wear=0.8, wear_color=rgb(246, 250, 252), wear_metal=1.0, dirt=0.9, dirt_color=(0.012, 0.02, 0.028, 1), scratches=0.3,
                      bump="brushed", bump_strength=0.1, mottle=1.4, mottle_color=rgb(86, 120, 140), fine=0.45, seed=53, bevel=0.25, ao_dist=3.0, wear_radius=0.9)
    P["trim"] = surf(S, "bl_trim", rgb(216, 202, 160), 1.0, 0.3, wear=0.9, wear_color=rgb(255, 246, 210), dirt=0.9, dirt_color=(0.04, 0.03, 0.012, 1), scratches=0.7, bump="hammer",
                     bump_strength=0.2, mottle=0.8, mottle_color=rgb(140, 118, 70), fine=0.5, seed=54, bevel=0.18)
    P["linen"] = surf(S, "bl_linen", rgb(228, 224, 212), 0.0, 0.9, bump="cross", bump_scale=1.0, bump_strength=0.9, groove=rgb(150, 144, 128), groove_amount=0.8, dirt=0.8,
                      dirt_color=(0.08, 0.07, 0.06, 1), mottle=0.8, mottle_color=rgb(184, 174, 152), wear=0.5, wear_color=rgb(246, 244, 238), wear_metal=0.0, fine=0.6, seed=55, bevel=0.0)
    P["cord"] = surf(S, "bl_cord", rgb(26, 40, 46), 0.0, 0.6, bump="wrap", bump_strength=0.8, groove=rgb(6, 10, 12), groove_amount=0.8, wear=0.6, wear_color=rgb(120, 160, 170),
                     wear_metal=0.0, dirt=0.5, seed=56, bevel=0.0, fine=0.4)
    P["teal"] = surf(S, "bl_teal", rgb(70, 215, 225), 0.0, 0.3, emit=rgb(60, 225, 235), emit_strength=0.9, emit_flicker=0.5, mottle=0.6, mottle_color=rgb(190, 250, 255), seed=57, bevel=0.0)
    P["gem"] = surf(S, "bl_gem", rgb(36, 186, 208), 0.0, 0.08, emit=rgb(70, 232, 244), emit_strength=0.8, emit_flicker=0.7, mottle=0.9, mottle_color=rgb(170, 248, 255), seed=58, bevel=0.0)
    P["pupil"] = surf(S, "bl_pupil", rgb(6, 16, 22), 0.0, 0.3, seed=59, bevel=0.0)
    return P


# ================================================================ helpers
def ring_sweep(name, center, R, section, n, material, part, rot=0.0):
    """A closed ring (torus-like solid) in the xy plane round `center` at radius R. section(theta) -> [(radial offset, z)] polygon, one per step, so the profile can
    change round the ring (vertebrae, teeth, bulges)."""
    verts, faces = [], []
    m = len(section(0.0))
    for i in range(n):
        th = rot + TAU * i / n
        c, s = math.cos(th), math.sin(th)
        for (ro, z) in section(th):
            verts.append((center[0] + (R + ro) * c, center[1] + (R + ro) * s, center[2] + z))
    for i in range(n):
        j = (i + 1) % n
        for k in range(m):
            k2 = (k + 1) % m
            faces.append((i * m + k, i * m + k2, j * m + k2, j * m + k))
    return K.mk(name, verts, faces, material, part, False, True, 55.0)


def oct_section(rw, zw, c=0.45):
    """An octagon-ish cross-section (radial half width rw, z half width zw, chamfer c)."""
    return [(-rw + c * rw, -zw), (rw - c * rw, -zw), (rw, -zw + c * zw), (rw, zw - c * zw), (rw - c * rw, zw), (-rw + c * rw, zw), (-rw, zw - c * zw), (-rw, -zw + c * zw)]


def arc_len(P):
    L = [0.0]
    for i in range(1, len(P)):
        L.append(L[-1] + (P[i] - P[i - 1]).length)
    return L


def at_arc(P, L, s):
    d = s * L[-1]
    for i in range(1, len(P)):
        if L[i] >= d:
            t = (d - L[i - 1]) / max(L[i] - L[i - 1], 1e-9)
            return P[i - 1].lerp(P[i], t), (P[i] - P[i - 1]).normalized()
    return P[-1], (P[-1] - P[-2]).normalized()


def claw(name, pts, r0, mat, part, sides=5):
    return K.tapered(name, [V(p) for p in pts], r0, 0.07, mat, sides=sides, sub=2, part=part)


# ================================================================ the scythe
def build(lod=0, S=1.0, mirror=False, grip_y=60.0, seed=1088):
    """Builds the scythe in the current scene at the cm scale (y = length, blade toward -x, +z faces the viewer). lod 1 = the in-hand piece of the djinn set
    (fewer segments, no tassels or beads). Afterwards, when S != 1 or mirror, all points are scaled by S, the grip point is moved to y = 0 and (mirror) x is
    flipped (the armour layer's convention: the head toward +x)."""
    P = palette(S)
    rr = K.rng(seed)
    hi = lod == 0
    sd = 8 if hi else 4                                          # lathe sides of the pole

    # ---------------------------------------------------------------- the pole: a spine of vertebra-joints, a carved spiral, wrapped linen grip, teal bands
    prof = []
    y = 34.0
    joints = [34.0 + (22.0 if hi else 40.0) * k for k in range(8 if hi else 5)]                 # a vertebra joint every 22 cm up to y = 188
    steps = 3 if hi else 1
    for k in range(len(joints) - 1):
        y0, y1 = joints[k], joints[k + 1]
        r_mid = 2.28 + 0.1 * math.sin(k * 1.7)
        prof += [(2.7, y0 - 1.3), (3.0, y0), (2.7, y0 + 1.3)]
        for q in range(1, steps):
            t = q / steps
            prof.append((r_mid + 0.28 * math.sin(math.pi * t) ** 2 * (1.0 if hi else 0.5), y0 + 1.0 + (y1 - y0 - 2.0) * t))
    prof += [(2.7, joints[-1] - 1.3), (3.0, joints[-1]), (2.7, joints[-1] + 1.3), (2.5, 196.0)]
    pole = bl.lathe("pole", prof, P["bone"], sides=sd, part="shaft")
    K._finish_obj(pole, "shaft", True, 50.0)
    if hi:
        K.helix_wrap("pole_spiral", 96.0, 186.0, 2.45, P["bone_smooth"], turns=6.0, cord=0.75, pitch_wobble=0.04, part="shaft_spiral", seed=3, sub=5)
    for i, yy in enumerate((98.0, 128.0, 158.0, 184.0) if hi else (110.0, 160.0)):               # teal inlay bands, each a little off its neighbours
        b = bl.lathe("pole_band%d" % i, [(2.6, yy - 1.2), (3.05, yy - 0.6), (3.1, yy), (3.05, yy + 0.6), (2.6, yy + 1.2)], P["teal"], sides=sd, part="shaft_glow")
        K._finish_obj(b, "shaft_glow", True, 50.0)
    # wrapped grip (linen over a bone core), silver bands
    K.grip_wrapped("grip", 38.0, 92.0, 2.5, P["bone_smooth"], P["linen"], turns=8.0 if hi else 2.0, cord=0.95, bands=(39.0, 66.0, 91.0) if hi else (66.0,), band_mat=P["trim"], swell=0.08, part="grip", seed=7,
                   sides=sd)

    # ---------------------------------------------------------------- the butt: three claws round a teal orb, a bone spike under it
    for k in range(3 if hi else 1):
        a = TAU * k / 3 + 0.45 + rr.uniform(-0.1, 0.1)
        ca, sa = math.cos(a), math.sin(a)
        claw("butt_claw%d" % k, [(2.8 * ca, 36.0, 2.8 * sa), (6.0 * ca, 30.0, 6.0 * sa), (5.6 * ca, 20.5, 5.6 * sa), (2.0 * ca, 13.0, 2.0 * sa)], 1.35, P["bone"], "pommel", 5 if hi else 3)
    K.blob("butt_orb", (0, 22.0, 0), (3.7, 4.2, 3.7), P["gem"], seed=5, lump=0.05, rings=6 if hi else 3, segs=9 if hi else 5, part="pommel_glow")
    claw("butt_spike", [(0, 15.0, 0), (0, 8.0, 0.2), (0.3, 2.6, 0), (0, 0.0, 0)], 1.8, P["bone"], "pommel", 6 if hi else 3)
    cl = bl.lathe("butt_collar", [(2.4, 34.0), (3.5, 34.9), (3.7, 37.0), (3.3, 38.6), (2.5, 39.0)], P["trim"], sides=sd, part="pommel_rings")
    K._finish_obj(cl, "pommel_rings", True, 50.0)

    # ---------------------------------------------------------------- the socket: a bone jaw-collar with claws that hold the emblem
    ycol = 196.0
    sk = bl.lathe("socket", [(2.5, ycol - 7.0), (4.2, ycol - 5.4), (5.0, ycol - 2.5), (4.6, ycol + 1.5), (3.8, ycol + 4.5), (3.0, ycol + 6.0)], P["bone"], sides=sd + (2 if hi else 0), part="socket")
    K._finish_obj(sk, "socket", True, 45.0)
    rg = bl.lathe("socket_ring", [(4.5, ycol - 1.8), (5.5, ycol - 1.2), (5.7, ycol), (5.5, ycol + 1.2), (4.5, ycol + 1.8)], P["trim"], sides=sd + (2 if hi else 0), part="socket_rings")
    K._finish_obj(rg, "socket_rings", True, 50.0)
    cy, R = 221.5, 17.0                                             # the emblem's centre and ring radius
    for k, (ax, az, ln) in enumerate(((-1.0, 0.0, 1.0), (1.0, 0.0, 0.88), (0.0, 1.0, 0.75), (0.0, -1.0, 0.8))[: 4 if hi else 2]):
        a = V((ax, 0, az))
        claw("socket_claw%d" % k, [(4.0 * ax, ycol - 1.0, 4.0 * az), (7.0 * ax, ycol + 3.0, 7.0 * az), (10.0 * ax * ln + 1.5 * ax, ycol + 9.0, 10.0 * az * ln), (8.4 * ax * ln, cy - R + 1.5, 8.4 * az * ln)],
             1.5, P["bone"], "socket_claws", 5 if hi else 3)

    # ---------------------------------------------------------------- the emblem: a coiled bone dragon (a ring of vertebrae), an inner ring, spokes, a teal dragon-eye
    nring = 48 if hi else 12
    def vert_section(th):
        k = math.cos(th * 12.0)                                    # twelve vertebrae round the ring
        bulge = 1.0 + 0.2 * k
        rw, zw = 3.0 * bulge, 3.5 * (1.0 + 0.14 * k)
        return oct_section(rw, zw, 0.4)
    ring = ring_sweep("emblem_ring", (0, cy, 0), R, vert_section, nring, P["bone"], "emblem", rot=0.17)
    K.displace(ring, 0.14, 0.3, seed=4, octaves=2)
    # a gold bead between the vertebrae on both faces: segment rivets
    if hi:
        pts = []
        for i in range(12):
            th = 0.17 + TAU * (i + 0.5) / 12.0
            for sgn in (1, -1):
                pts.append((V((0 + (R + 0.0) * math.cos(th), cy + R * math.sin(th), sgn * 3.4)), (0, 0, sgn)))
        K.rivets("emblem_studs", pts, P["teal"], r=0.8, part="emblem_studs", detail=True, sides=6, rng_=rr, jitter=0.2)
    rin = ring_sweep("emblem_inner", (0, cy, 0), 9.4, lambda th: oct_section(0.9, 1.9, 0.4), 22 if hi else 8, P["trim"], "emblem_rings", rot=0.4)
    n_sp = 8 if hi else 0
    for i in range(n_sp):                                          # spokes: bone bars from the inner ring to the vertebra ring, uneven angles and widths
        a = TAU * (i + rr.uniform(-0.12, 0.12)) / n_sp + 0.3
        w = 0.8 * (1.0 + 0.4 * (i % 2))
        p0 = V((9.8 * math.cos(a), cy + 9.8 * math.sin(a), 0))
        p1 = V(((R - 2.6) * math.cos(a), cy + (R - 2.6) * math.sin(a), 0))
        K.bar("spoke%d" % i, p0, p1, w, 0.8, P["bone_smooth"], part="emblem_spokes", taper=1.15, bevel_w=0.0)
    # sun-burst of small bone thorns on the outer rim (uneven, a few broken off)
    bp, dr, ln = [], [], []
    for i in range(12):
        if i in (3, 8):
            continue
        a = TAU * (i + rr.uniform(-0.2, 0.2)) / 12 + 0.05
        bp.append(V(((R + 2.6) * math.cos(a), cy + (R + 2.6) * math.sin(a), rr.uniform(-0.3, 0.3))))
        dr.append(V((math.cos(a), math.sin(a), 0)))
        ln.append(3.4 + 4.4 * rr.random() ** 1.5 * (1.0 if i % 3 else 1.6))
    if hi:
        K.spike_array("emblem_thorn", bp, dr, ln, 0.95, P["bone"], rr, bend=0.4, sides=3, part="emblem_thorns", len_jit=0.15, curve=0.04)
    # the eye: a slit-pupil dragon eye of teal glass in six bone claws
    eye = K.blob("eye", (0, cy, 0.3), (5.6, 7.6, 4.4), P["gem"], seed=11, lump=0.05, rings=8 if hi else 4, segs=12 if hi else 6, part="emblem_glow")
    for sgn in (1, -1):
        K.seam("pupil%d" % sgn, [(0.0 + 0.15 * math.sin(t * 3), cy - 6.2 + 12.4 * t / 8.0) for t in range(9)], P["pupil"], w=0.7, depth=0.1, z=4.35, side=sgn, part="emblem_pupil", detail=True)
    for k in range(6 if hi else 0):
        a = TAU * (k + rr.uniform(-0.1, 0.1)) / 6 + 0.2
        ca, sa = math.cos(a), math.sin(a)
        p = [(7.8 * ca, cy + 9.0 * sa, 0.2), (6.6 * ca, cy + 8.2 * sa, 3.2), (4.6 * ca, cy + 6.0 * sa, 4.6)]
        claw("eye_claw%d" % k, [p[0], p[1], p[2]], 0.7, P["trim"], "emblem_claws", 4)
    # runes round the ring (bake-only glow relief on both faces)
    if hi:
        for sgn in (1, -1):
            for i in range(12):
                th = 0.17 + TAU * (i + 0.5) / 12.0 + (0.0 if sgn > 0 else 0.0)
                g = K.RUNES[(i * 5 + (3 if sgn < 0 else 0)) % len(K.RUNES)]
                loc = V((R * math.cos(th), cy + R * math.sin(th), sgn * 4.1))
                K.runes("emblem_rune%d_%d" % (sgn, i), g, 2.0, P["teal"], tuple(loc), rot=(0, 0 if sgn > 0 else math.pi, th - math.pi / 2 if sgn > 0 else -(th - math.pi / 2)),
                        extrude=0.07, part="emblem_runes")
    # ---------------------------------------------------------------- the blade: a silver-white crescent growing out of the emblem, a saw-notched cutting edge,
    # a vertebra spine of bone with swept thorns, a teal-lit fuller
    O = [V(p) for p in bl.smooth_path([V((x, y, 0)) for (x, y) in
                                       [(3.0, 238.0), (-8.0, 247.0), (-26.0, 256.5), (-54.0, 262.0), (-84.0, 258.0), (-108.0, 243.0), (-123.0, 219.0), (-129.0, 194.0), (-130.0, 170.0)]], 10)]
    Lc = arc_len(O)
    width = lambda s: K.interp([(0.0, 9.0), (0.08, 15.0), (0.30, 23.0), (0.52, 22.0), (0.72, 15.5), (0.88, 8.0), (0.96, 3.0), (1.0, 0.2)], s)           # noqa: E731
    bites = [(0.31, 3.0, 0.020), (0.455, 2.2, 0.016), (0.575, 4.4, 0.034), (0.585, 1.8, 0.012), (0.70, 2.6, 0.018), (0.815, 1.6, 0.014), (0.9, 1.2, 0.014)]
    tooth_p = 9.0 / Lc[-1]                                          # a saw tooth every ~9 cm of the cutting edge (step in, slow slant out)

    def bite(s):
        b = 0.0
        for c, d, hw in bites:
            u = abs(s - c) / hw
            if u < 1.0:
                b = max(b, d * (1.0 - u) ** 1.2)
        if 0.27 < s < 0.94:
            ph = (s / tooth_p) % 1.0
            b = max(b, (1.0 + 0.9 * K.noise3((s * 40, 2, 1), 1.0, 9, 1) ** 2) * (1.0 - ph) ** 1.5 * 2.4 * min(1.0, (0.94 - s) / 0.1))
        return b
    nrows = 58 if hi else 14
    rows = []
    for j in range(nrows + 1):
        s = j / nrows
        p, t = at_arc(O, Lc, s)
        n = V((-t.y, t.x, 0)).normalized()                          # toward the inside of the curve
        w = max(width(s) - bite(s) - 0.35 * abs(K.noise3((s * 14, 0, 1), 1.0, 7, 2)), 0.2 if s < 1.0 else 0.0)
        rows.append((p, p + n * w))
    sec_v = (-1.0, -0.86, -0.50, -0.34, 0.52, 1.0) if hi else (-1.0, -0.5, 0.3, 1.0)
    sec_h = (0.5, 1.0, 1.0, 0.5, 0.85, 0.0) if hi else (0.6, 1.0, 0.8, 0.0)
    thick = lambda t: 2.0 * (1.0 - 0.5 * t ** 1.6)                  # noqa: E731
    K.strip("blade_body", rows, P["steel"], sec_v, sec_h, thick, part="blade", up=(0, 0, 1), wobble=lambda q, t: (0, 0, 0.18 * K.noise3(q, 0.22, 4, 3)))
    # the spine: a vertebra column of bone laid along the back (a bone dragon's backbone), cut through by silver clasps
    pts_s = [O[i] for i in range(0, len(O), 2 if hi else 6)] + [O[-1]]
    nv = 10 if hi else 4
    rsp = lambda t: 1.9 * (1.0 + 0.3 * math.cos(t * TAU * nv)) * (1.0 - 0.62 * t ** 1.8) + 0.12          # noqa: E731
    sp = bl.sweep("blade_spine", pts_s, bl.circle(1.0, 5 if hi else 4), P["bone"], scale=rsp, up=(0, 0, 1), part="blade_spine")
    K._finish_obj(sp, "blade_spine", True, 55.0)
    K.displace(sp, 0.06, 0.4, seed=12, octaves=2)
    # thorns grow out of the vertebrae, swept backward, uneven
    bpts, bdirs, blen = [], [], []
    for j in range(5, nrows - 4, 5):
        s = j / nrows
        p, t = at_arc(O, Lc, s)
        out = V((t.y, -t.x, 0)).normalized()
        bpts.append(p + out * 1.2 + V((0, 0, rr.uniform(-0.2, 0.2))))
        bdirs.append(out * 0.7 + t * 0.6)
        blen.append((3.4 + 4.2 * math.sin(math.pi * min(s * 1.05, 1.0)) ** 1.3) * (1.0 + (0.5 if j % 9 == 2 else 0.0)))
    if hi:
        K.spike_array("blade_thorn", bpts, bdirs, blen, 1.05, P["bone"], rr, bend=0.35, sides=4, part="blade_thorns", len_jit=0.2, curve=0.03)
    for j in ((9, 28, 47) if hi else (7,)):
        if j >= nrows:
            continue
        p, t = at_arc(O, Lc, j / nrows)
        n = V((-t.y, t.x, 0)).normalized()
        K.bar("blade_clasp%d" % j, p - n * 0.6, p + n * 6.5, 1.3, 2.9, P["trim"], part="blade_clasps", bevel_w=0.0, up=(0, 0, 1))
    # the teal inlay in the fuller, both faces; it fades before the tip
    if hi:
        for sgn in (1, -1):
            pts = []
            for j in range(4, int(nrows * 0.88)):
                a, b = rows[j]
                q = a + (b - a) * 0.318
                pts.append((q.x, q.y))
            K.seam("fuller_glow%d" % sgn, pts, P["teal"], w=0.5, depth=0.1, z=thick(0.35) * 0.78 * sgn, side=sgn, part="blade_glow", detail=True)
        # a thin teal line along the cutting edge near the root (a lit edge, broken by the teeth)
        for sgn in (1, -1):
            pts = []
            for j in range(2, int(nrows * 0.55)):
                a, b = rows[j]
                q = a + (b - a) * 0.93
                pts.append((q.x, q.y))
            K.seam("edge_glow%d" % sgn, pts, P["teal"], w=0.16, depth=0.06, z=thick(0.2) * 0.2 * sgn, side=sgn, part="blade_glow", detail=True)
    # a hooked talon at the tip: the last few cm of the spine curl into a bone claw
    tipp = O[-1]
    claw("tip_talon", [tipp + V((0.6, 6.0, 0)), tipp + V((0.0, -1.0, 0)), tipp + V((2.2, -7.0, 0.7)), tipp + V((6.5, -11.5, 0.6))], 1.2, P["bone"], "blade_tip", 4 if hi else 3)

    # ---------------------------------------------------------------- tassels: linen tails and teal bead strings from the socket
    if hi:
        for i, (ang, ln) in enumerate(((35, 44), (170, 56), (240, 34), (305, 48))):
            a = math.radians(ang)
            sw = 1.0 + 0.5 * i
            path = [V((6.3 * math.cos(a) + 0.5 * k * math.cos(a) + 1.1 * math.sin(k * 0.7 + sw), 193.0 - k * ln / 9.0, 6.3 * math.sin(a) + 0.5 * k * math.sin(a) + 1.3 * math.sin(k * 0.9 + i))) for k in range(10)]
            K.ribbon("tail%d" % i, path, lambda t: 3.2 * (1.0 - 0.55 * t) + 0.35, P["linen"], facing=(math.cos(a + 1.57), 0, math.sin(a + 1.57)), thick=0.08, part="tassel", fray=1.4, rng_=rr,
                     edge_wave=0.14, notch=1.7 if i % 2 else 0.0)
        for i, (ang, ln, nb) in enumerate(((95, 32, 4), (265, 24, 4))):
            a = math.radians(ang)
            cpath = [(6.8 * math.cos(a) + 0.5 * k * math.cos(a + 0.4), 194.0 - k * ln / 6.0, 6.8 * math.sin(a) + 0.5 * k * math.sin(a) + 0.5 * math.sin(k * 1.4)) for k in range(7)]
            K.tube("bead_cord%d" % i, cpath, 0.22, P["cord"], sides=3, sub=2, part="tassel_cord")
            sm = bl.smooth_path([V(p) for p in cpath], 3)
            for b in range(nb):
                q = sm[min(len(sm) - 1, 2 + int((len(sm) - 3) * b / max(nb - 1, 1)))]
                K.blob("bead%d_%d" % (i, b), tuple(q), (1.1, 1.1, 1.1), P["gem"] if b % 2 == 0 else P["bone_smooth"], seed=b, lump=0.0, rings=3, segs=5, part="tassel_beads" if b % 2 else "tassel_glow")
    return K.quad_estimate()


def transform_scene(S, mirror, grip_y, objs=None):
    """Scales every point by S about the grip point (which becomes y = 0) and optionally flips x (head toward +x). Details and bodies alike.
    objs: only these objects (belial_bone.py passes the weapon piece's own, the rest of its scene must not move); default every mesh of the scene."""
    from mathutils import Matrix
    M = Matrix.Diagonal((S * (-1.0 if mirror else 1.0), S, S, 1.0))
    M = M @ Matrix.Translation((0.0, -grip_y, 0.0))
    for o in (bpy_objects() if objs is None else objs):
        K.xf(o, M)


def bpy_objects():
    import bpy
    return [o for o in bpy.context.scene.collection.objects if o.type == "MESH"]


# ================================================================ driver
def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--trial")
    ap.add_argument("--beauty")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--bake", type=int, default=1024)
    ap.add_argument("--skin", type=int, default=1024)
    a = ap.parse_args(argv)
    import bpy
    sc = K.begin(a.samples)
    sc.render.threads_mode = "FIXED"
    sc.render.threads = a.threads
    t0 = time.time()
    q = build()
    print("   parts:", K.quad_table(40))
    print("[%s] built geometry: ~%d body quads in %.1f s" % (MESH_ID, q, time.time() - t0))
    if a.beauty:
        os.makedirs(a.beauty, exist_ok=True)
        for view, size in (("front", (640, 960)),):
            K.beauty(os.path.join(a.beauty, "scythe_%s.png" % view), view=view, samples=14, size=size)
        K.beauty(os.path.join(a.beauty, "scythe_emblem.png"), view="front", samples=18, size=(900, 900), focus=(-30, 232, 0, 80))
        K.beauty(os.path.join(a.beauty, "scythe_emblem_tq.png"), view="three_quarter", samples=18, size=(900, 900), focus=(-20, 225, 0, 70))
        K.beauty(os.path.join(a.beauty, "scythe_butt.png"), view="three_quarter", samples=14, size=(700, 700), focus=(0, 40, 0, 90))
    if not a.quick:
        hdr = os.path.join(bl.MESH_DIR, BASE + ".txt")
        r = finish(a, hdr)
        print(r)


def fix_grip_line(path):
    """bl.finish copies the base item's `grip` fraction; this scythe's real grip (the wrapped section) sits lower, so the header gets the measured centre / length."""
    lines = open(path).read().split("\n")
    qs = [l.split() for l in lines if l.startswith(("q ", "qn "))]
    grip = next((l.split() for l in lines if l.startswith("# part grip ")), None)
    if not grip or not qs:
        return
    a, n = int(grip[3]), int(grip[4])

    def ys(sel):
        return [float(v[2 + k * (8 if v[0] == "qn" else 5) + 1]) for v in sel for k in range(4)]
    g, allv = ys(qs[a:a + n]), ys(qs)
    frac = ((min(g) + max(g)) / 2.0 - min(allv)) / (max(allv) - min(allv))
    for i, l in enumerate(lines):
        if l.startswith("grip "):
            lines[i] = "grip %.3f" % frac
    open(path, "w").write("\n".join(lines))
    print("[grip] header grip fraction set to %.3f (grip section centre / total length)" % frac)


def finish(a, hdr):
    """K.finish_form with the base's header, but a skin size the caller chooses (the owner asked for 1024 px skins)."""
    t0 = K._T0[0]
    if a.trial:
        mesh_dir, skin_dir = os.path.join(a.trial, "weapon_meshes"), os.path.join(a.trial, "textures", "entity", "weapon")
    else:
        mesh_dir, skin_dir = bl.MESH_DIR, bl.SKIN_DIR
    os.makedirs(mesh_dir, exist_ok=True)
    os.makedirs(skin_dir, exist_ok=True)
    bl.finish(MESH_ID, glow=("FFFFFF", 0.12, 0.45, 1.0), header_from=hdr, bake=a.bake, skin=a.skin, out_mesh=mesh_dir, out_skin=skin_dir, preview=None, write=True, budget=10 ** 6, cage=0.5)
    fix_grip_line(os.path.join(mesh_dir, MESH_ID + ".txt"))
    secs = time.time() - t0
    for f in (MESH_ID + ".png", MESH_ID + "_glow.png"):                 # skin + glow must stay <= 350 KB together (owner's limit): 96 colours, a firmer speckle pass
        b, a_, ps = K.pack_png(os.path.join(skin_dir, f), colors=96, denoise=0.85)
        print("[pack] %s %.0f KB -> %.0f KB (psnr %.1f dB)" % (f, b, a_, ps))
    return K.report(MESH_ID, BASE, mesh_dir, skin_dir, None, None, secs)


if __name__ == "__main__":
    main(sys.argv[1:])

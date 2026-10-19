"""djinn36_lib: the shared helpers of the 24 new Djinn (Bael ... Murmur) on top of the first generation's builders (djinn_parts / djinn_unique*).

Nothing of the first twelve is edited: this module registers the new names into the first generation's tables at import time (PALETTE, STYLE, FAMILY),
so its kit, materials, atlas and exporter work for them unchanged, and adds the motifs the new builders share (extruded polygons, banners, shards,
gears, keys, scallops, rotated parts ...). Coordinates are the first generation's author space (see djinn_unique.py).
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                              # noqa: E402
import djinn_parts as dp                                           # noqa: E402
import djinn_unique as du                                          # noqa: E402
import djinn_unique_heads as duh                                   # noqa: E402
import djinn_unique_back as dub                                    # noqa: E402
import djinn_unique_arms as dua                                    # noqa: E402
import djinn36_roster as R                                         # noqa: E402
from djinn_unique import (C, S, curve, collar, sweep_tube, leaf, spike, feather, tongue, zig, orb, disc, crescent, ring_path, sweep_flat, tube,   # noqa: E402,F401
                          pf_flame, pf_feather, pf_blade, pf_taper, pf_ribbon, pf_flat, UP, unit, arc_path, INNER, OUTER)
from djinn_unique_heads import blade3, head_base, brow_gem, horn_pair, hair_blades, pointed_ears, BROW_Z       # noqa: E402,F401

# the helm / pauldron / vambrace / greave styles of the old shared rig for each new Djinn (only the fallback set reads them) and the forearm family
STYLE36 = {
    "BAEL": ("circlet", "dome", "plain", "plain", "plate"), "AGARES": ("visor", "spike", "claw", "spike", "flame"), "VALEFOR": ("open", "fin", "fin", "fin", "bolt"),
    "BARBATOS": ("circlet", "dome", "plain", "plain", "thorn"), "GUSION": ("circlet", "tier", "plain", "plain", "wave"), "VEPAR": ("circlet", "tier", "plain", "plain", "wave"),
    "SHAX": ("open", "fin", "fin", "plain", "feather"), "BUER": ("circlet", "dome", "plain", "plain", "thorn"), "ANDREALPHUS": ("circlet", "fin", "fin", "fin", "feather"),
    "KIMARIS": ("visor", "spike", "claw", "spike", "plate"), "AMDUSIAS": ("circlet", "tier", "fin", "plain", "bell"), "BELIAL": ("visor", "spike", "claw", "spike", "flame"),
    "DECARABIA": ("visor", "dome", "plain", "spike", "plate"), "SEERE": ("open", "fin", "fin", "fin", "feather"), "DANTALION": ("circlet", "tier", "plain", "plain", "bell"),
    "ANDROMALIUS": ("open", "dome", "plain", "plain", "ray"), "GLASYA_LABOLAS": ("visor", "spike", "claw", "spike", "flame"), "HAAGENTI": ("visor", "spike", "claw", "spike", "plate"),
    "CROCELL": ("circlet", "tier", "plain", "plain", "ray"), "FURCAS": ("visor", "dome", "plain", "plain", "plate"), "BALAM": ("open", "spike", "claw", "spike", "flame"),
    "ALLOCES": ("open", "spike", "claw", "spike", "flame"), "CAMIO": ("open", "fin", "fin", "fin", "feather"), "MURMUR": ("visor", "dome", "claw", "spike", "bell"),
}
# the forearm and greave family of the unique looks (djinn_unique_arms.forearm_extra / greave_extra read FAMILY[name])
FAMILY36 = {n: STYLE36[n][4] for n in R.NAMES}


def setup():
    """Registers the 24 names into the first generation's tables (idempotent)."""
    for n in R.NAMES:
        du.PALETTE[n] = R.PALETTE[n]
        h, p, v, g, _ = STYLE36[n]
        dp.STYLE[n] = dict(helm=h, pauldron=p, vambrace=v, greave=g)
        dua.FAMILY[n] = FAMILY36[n]


setup()

PI = math.pi


def rad(a):
    return math.radians(a)


# ================================================================ small shared motifs
def xform(k, part, rot=None, shift=None, scale=None):
    """Replaces `part` (just added to the kit's mesh) by a copy turned (axis, degrees[, about]) and moved; returns the copy."""
    q = part
    if scale is not None:
        M = np.diag(scale if hasattr(scale, "__len__") else (scale, scale, scale))
        q = q.transformed(M, name=q.name)
    if rot is not None:
        axis, deg = rot[0], rot[1]
        about = rot[2] if len(rot) > 2 else (0, 0, 0)
        q = q.rotated(axis, deg, about, name=q.name)
    if shift is not None:
        q = q.translated(*shift, name=q.name)
    k.m.parts[k.m.parts.index(part)] = q
    return q


def extrude(k, piece, name, mat, poly, z0, z1, layer=0, glow=False, smooth=30.0):
    """A convex polygon [(x, y)] extruded along z from z0 to z1 (a pane, a shield, a book cover, a plate, a shard)."""
    p = k.part(piece, name, mat, layer=layer, glow=glow, smooth=smooth)
    ml.loft(p, "z", (0, 0, z0), [(0, 1.0, 1.0), (z1 - z0, 1.0, 1.0)], poly, caps=(True, True))
    return p


def ngon(cx, cy, r, n, a0=0.0, sx=1.0, sy=1.0):
    return [(cx + sx * r * math.cos(a0 + 2 * PI * i / n), cy + sy * r * math.sin(a0 + 2 * PI * i / n)) for i in range(n)]


def poly_shift(poly, dx, dy):
    return [(x + dx, y + dy) for (x, y) in poly]


def poly_rot(poly, deg, cx=0.0, cy=0.0):
    c, s = math.cos(rad(deg)), math.sin(rad(deg))
    return [(cx + (x - cx) * c - (y - cy) * s, cy + (x - cx) * s + (y - cy) * c) for (x, y) in poly]


def lf(k, piece, name, mat, pts, smooth=45, layer=0, glow=False):
    """A flat double-sided polygon (a membrane, a banner, a leaf)."""
    p = k.part(piece, name, mat, smooth=smooth, layer=layer, glow=glow)
    leaf(p, [tuple(q) for q in pts])
    return p


def rib(k, piece, name, mat, p0, p1, w=0.34, h=0.14, up=(0, 0, 1), taper=0.6, layer=0, glow=False):
    return du.bar(k.part(piece, name, mat, layer=layer, glow=glow), p0, p1, w, h, up=up, taper=taper)


def ribbon(k, piece, name, mat, path, w, t=0.25, taper=0.35, n=5, layer=0, up=None):
    """A cloth strap swept along a control path (Catmull-Rom smoothed), width tapering by `taper`."""
    p = k.part(piece, name, mat, uv_mode="loft", layer=layer)
    du.sweep(p, curve(path, n), ml.rect_ring(1.0, t), scales=lambda s: (w * (1 - taper * s), 1.0), up=up)
    return p


def gemz(k, piece, name, c, r=0.8, long=1.2, axis="z", mat="crystal", layer=2):
    return du.gem(k.part(piece, name, mat, layer=layer, smooth=0), c, r, long=long, sides=6, axis=axis)


def glow_orb(k, piece, name, c, r, mat="crystal", sides=6):
    return orb(k.part(piece, name, mat, layer=2, smooth=0), c, r, sides=sides)


def ring_at(k, piece, name, mat, center, R_, tube_r, axis="z", n=14, layer=0, a0=0, a1=360):
    """A torus arc in a plane normal to `axis` (a ring of metal, a hoop)."""
    plane = {"z": "xy", "y": "xz", "x": "yz"}[axis]
    p = k.part(piece, name, mat, layer=layer, smooth=40)
    du.sweep(p, ring_path(center, R_, a0, a1, plane, n), C(tube_r, S(6)))
    return p


def gear(k, piece, name, mat, center, r, teeth=8, thick=1.0, hub=0.35, layer=0):
    """A cog in the xy plane: a thick disc, pyramids on the rim, a hub ring."""
    cx, cy, cz = center
    disc(k.part(piece, name, mat, layer=layer), (cx, cy, cz), r, thick, "z", 12)
    for i in range(teeth):
        a = 2 * PI * i / teeth
        spike(k.part(piece, f"{name}_t{i}", mat, layer=layer), (cx + math.cos(a) * r * 0.94, cy + math.sin(a) * r * 0.94, cz), (math.cos(a), math.sin(a), 0), r * 0.38, r * 0.2, 4)
    collar(k.part(piece, name + "_hub", "trim"), cz - thick * 0.9, r * hub, thick * 1.8, lip=0.15, sides=S(8), axis="z", origin=(cx, cy, 0))


def heart(k, piece, name, c, r=1.0, mat="crystal", layer=2):
    """A heart: two lobes and a point, as two gems and a cone (small, glowing)."""
    cx, cy, cz = c
    for sgn in (-1, 1):
        orb(k.part(piece, f"{name}_l{sgn}", mat, layer=layer, smooth=0), (cx + sgn * r * 0.5, cy + r * 0.25, cz), r * 0.55, sides=6)
    spike(k.part(piece, name + "_pt", mat, layer=layer, smooth=0), (cx, cy + r * 0.3, cz), (0, -1, 0), r * 1.3, r * 0.85, 4)


def scallop(k, piece, name, mat, base, r, n=7, spread=130.0, thick=0.5, rib_mat="trim", up_deg=90.0, plane="xy", layer=0):
    """A shell fan: a leaf polygon with n ribs radiating from `base` over `spread` degrees about the direction `up_deg`."""
    bx, by, bz = base
    a0 = rad(up_deg - spread / 2)
    pts = [base] + [(bx + r * math.cos(a0 + rad(spread) * i / (n * 2)), by + r * math.sin(a0 + rad(spread) * i / (n * 2)), bz) for i in range(n * 2 + 1)]
    lf(k, piece, name, mat, pts, layer=layer)
    for i in range(n):
        a = a0 + rad(spread) * (i + 0.5) / n
        rib(k, piece, f"{name}_rib{i}", rib_mat, (bx, by, bz + 0.1), (bx + r * 0.98 * math.cos(a), by + r * 0.98 * math.sin(a), bz + 0.1), 0.3, 0.14, taper=0.4)


def shard(k, piece, name, mat, base, direction, L, w, t=0.5, layer=0, glow=False):
    """A flat crystal shard: a blade lozenge-shaped in section, pointed, widest a third up."""
    d = unit(direction)
    p = k.part(piece, name, mat, layer=layer, glow=glow, smooth=0)
    nrm = np.cross(d, (0, 0, 1))
    if np.linalg.norm(nrm) < 1e-6:
        nrm = np.array([1.0, 0, 0])
    ml.loft(p, d, base, [(0, 0.5, 1.0), (L * 0.3, 1.0, 1.0), (L, 0.0, 0.0)], ml.lozenge(w, t), up=(0, 0, 1))
    return p


def key(k, piece, name, mat, origin, ang, L=14.0, bow_r=2.4, w=0.9, mat2="trim"):
    """A great key standing at `origin` (bow at the bottom, bit at the top), turned `ang` degrees about z, in the xy plane."""
    parts = []
    ox, oy, oz = origin
    p = k.part(piece, name + "_bow", mat2)
    du.sweep(p, ring_path((0, 0, 0), bow_r, 0, 360, "xy", 10), C(0.55, S(6)))
    parts.append(p)
    p = k.part(piece, name + "_shaft", mat)
    ml.loft(p, "y", (0, bow_r * 0.8, 0), [(0, 1, 1), (L, 1, 1)], ml.octagon(w, w, w * 0.3), caps=(False, True))
    parts.append(p)
    p = k.part(piece, name + "_bit", mat2)
    ml.box(p, (0, L * 0.72, -0.4), (2.8, L * 0.92, 0.4))
    parts.append(p)
    p = k.part(piece, name + "_bit2", mat2)
    ml.box(p, (0, L * 0.5, -0.4), (2.0, L * 0.62, 0.4))
    parts.append(p)
    for q in parts:
        xform(k, q, rot=("z", ang), shift=(ox, oy, oz))


def feather_eye(k, piece, name, c, r, mat_out="trim", mat_in="crystal"):
    """The eye of a peacock feather: a ring and a glowing pupil, facing +z."""
    cx, cy, cz = c
    disc(k.part(piece, name + "_o", mat_out), (cx, cy, cz), r, 0.4, "z", 10)
    gemz(k, piece, name + "_i", (cx, cy, cz + 0.35), r * 0.55, 0.6)


def arc_pts(center, R_, a0, a1, n=10, plane="xy"):
    return ring_path(center, R_, a0, a1, plane, n)


def tapered_tube(k, piece, name, mat, path, r0, r1, sides=6, smooth=40):
    p = k.part(piece, name, mat, smooth=smooth)
    sweep_tube(p, path, r0, r1, sides)
    return p


def bone_wing(k, sgn, mat="plate", r=1.2, joint=True, shoulder=True):
    dub.wing_bones(k, sgn, mat, r, joint=joint, shoulder=shoulder)


def wf(k, seg, nm, sgn, bx, ang, L, w, t=0.5, curl=0.0, mat="plate", n=5, prof=pf_feather, by=0.0, bz=0.0, layer=0, glow=False):
    return dub.wf(k, seg, nm, sgn, bx, ang, L, w, t, curl, mat, n, prof, by, bz, layer, glow)


def wcore(k, seg, nm, sgn, bx, ang, L, w, curl=0.0, n=4, bz=0.25):
    return dub.wcore(k, seg, nm, sgn, bx, ang, L, w, curl, n, bz)


def sd(sgn):
    return "l" if sgn > 0 else "r"


def wing_part(sgn, seg):
    return f"wing_{seg}_{sd(sgn)}"


def seg_core(k, r, mat="plate", name="seg", sides=8, taper=0.8):
    dub.seg_core(k, r, mat, name, sides, taper)


def pauldrons(k, fn, R_=4.45, mat="plate", rims=1):
    """The dome of both shoulders plus the Djinn's own additions: fn(k, piece, sgn, cx, bx) for each."""
    for sgn, piece in ((-1, "pauldron_r"), (1, "pauldron_l")):
        cx = dua.dome_base(k, piece, sgn, R_, mat, rims)
        fn(k, piece, sgn, cx, cx + sgn * 2.8)


def cape(k, length=24.0, width=9.0, mat="cloth", wave=1.0, hem="trim"):
    dub.cape(k, length, width, mat, wave, k.trim() if hem == "trim" else hem)


def jag_hem(k, piece, name, mat, y, z, half_w, depth, n, ragged=0.3):
    """A row of triangular tatters hanging from a horizontal line y (the torn hem of a cape or a sail)."""
    for i in range(n):
        x0 = -half_w + 2 * half_w * i / n
        x1 = -half_w + 2 * half_w * (i + 1) / n
        tip = depth * (0.7 + ragged * math.sin(i * 2.1))
        lf(k, piece, f"{name}{i}", mat, [(x0, y, z), (x1, y, z), ((x0 + x1) / 2, y - tip, z - 0.2)], smooth=30)


def helix_horn(k, piece, name, mat, base, direction, L, r0, r1, turns=3, ridge_r=0.3, steps=14):
    """A spiral horn: a tapering cone with a ridge winding round it."""
    d = unit(direction)
    ml.loft(k.part(piece, name, mat, smooth=40), d, base, [(0, r0, r0), (L * 0.5, (r0 + r1) / 2, (r0 + r1) / 2), (L, r1, r1)], C(1.0, S(8)), caps=(False, False))
    ref = np.array([1.0, 0, 0]) if abs(d[0]) < 0.9 else np.array([0, 1.0, 0])
    u = unit(np.cross(d, ref))
    v = np.cross(d, u)
    pts = []
    for i in range(steps + 1):
        t = i / steps
        rr = (r0 + (r1 - r0) * t) * 1.02
        a = turns * 2 * PI * t
        pts.append(tuple(np.asarray(base, float) + d * L * t * 0.99 + (u * math.cos(a) + v * math.sin(a)) * rr))
    p = k.part(piece, name + "_ridge", "trim", smooth=0)
    du.sweep(p, pts, C(ridge_r, 4), scales=lambda s: (1.0 - 0.7 * s, 1.0 - 0.7 * s), caps=(False, False))


def drum(k, piece, name, center, r, h, axis="z", mat="plate", skin="cloth", rim="trim", rods=8):
    """A drum: a shell, two skins, two rims and tension rods round it (axis z: its face looks at the viewer)."""
    cx, cy, cz = center
    ml.lathe(k.part(piece, name + "_shell", mat), axis, (cx, cy, cz), [(-h / 2, r * 0.95), (0, r * 1.02), (h / 2, r * 0.95)], sides=S(14), caps=(False, False))
    for sgn, nm in ((1, "f"), (-1, "b")):
        ml.lathe(k.part(piece, f"{name}_skin_{nm}", skin), axis, (cx, cy, cz), [(sgn * h / 2, r * 0.93), (sgn * (h / 2 + 0.25), r * 0.8), (sgn * (h / 2 + 0.3), 0.0)], sides=S(14), caps=(False, True))
        collar(k.part(piece, f"{name}_rim_{nm}", rim), sgn * h / 2 - 0.3, r * 1.0, 0.7, lip=0.2, sides=S(14), axis=axis, origin=(cx, cy, cz))
    for i in range(rods):
        a = 2 * PI * i / rods
        if axis == "z":
            p0, p1 = (cx + math.cos(a) * r * 1.05, cy + math.sin(a) * r * 1.05, cz - h / 2), (cx + math.cos(a) * r * 1.05, cy + math.sin(a) * r * 1.05, cz + h / 2)
            du.bar(k.part(piece, f"{name}_rod{i}", rim), p0, p1, 0.3, 0.14, up=(math.cos(a), math.sin(a), 0), taper=0.3)


def transform_since(k, n0, rot=None, shift=None):
    """Turns / moves every part added after index n0 (a motif built at the origin, then placed)."""
    for p in list(k.m.parts[n0:]):
        xform(k, p, rot=rot, shift=shift)


def star_poly(cx, cy, r_out, r_in, n=5, a0=90.0):
    """The outline of an n-pointed star (star-shaped: a fan from its centre closes it)."""
    return [(cx + (r_out if i % 2 == 0 else r_in) * math.cos(rad(a0) + PI * i / n), cy + (r_out if i % 2 == 0 else r_in) * math.sin(rad(a0) + PI * i / n)) for i in range(2 * n)]


def cog_poly(cx, cy, r, teeth=6, depth=0.3):
    """A sprocket outline: a tip and a valley for each tooth (two points a tooth keeps the quad count low)."""
    pts = []
    for i in range(2 * teeth):
        a = PI * i / teeth
        rr = r if i % 2 == 0 else r * (1 - depth)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    return pts


def cog(k, piece, name, mat, c, r, teeth=6, thick=0.9, layer=0, hub=True):
    """A cog in the xy plane (an extruded toothed outline, a glowing hub)."""
    extrude(k, piece, name, mat, cog_poly(c[0], c[1], r, teeth), c[2] - thick / 2, c[2] + thick / 2, layer)
    if hub:
        orb(k.part(piece, name + "_hub", "crystal", layer=2, smooth=0), (c[0], c[1], c[2] + thick * 0.5), r * 0.32, sides=4)


def almond(k, piece, name, c, w, h, mat="trim", tube=0.3, iris=True, lashes=0):
    """An eye in the xy plane: two arcs closing in points, an iris of light, a pupil, optional lashes."""
    cx, cy, cz = c
    us = np.linspace(-1, 1, 5)
    up = [(cx + w * u, cy + h * (1 - u * u), cz) for u in us]
    lo = [(cx + w * u, cy - 0.8 * h * (1 - u * u), cz) for u in us[::-1]]
    du.tube_direct(k.part(piece, name + "_lid", mat, smooth=40), up + lo[1:], tube, tube, 4)
    if iris:
        orb(k.part(piece, name + "_iris", "crystal", layer=2, smooth=0), (cx, cy, cz + 0.1), h * 0.62, sides=6)
        orb(k.part(piece, name + "_pupil", "dark", smooth=0), (cx, cy, cz + h * 0.5), h * 0.28, sides=4)
    for i in range(lashes):
        a = rad(30 + 120 * i / max(1, lashes - 1))
        spike(k.part(piece, f"{name}_lash{i}", mat), (cx + w * 0.9 * math.cos(a), cy + h * 1.0 * math.sin(a), cz), (math.cos(a) * 0.6, math.sin(a), 0), w * 0.3, tube * 0.7, 4)


def lens(k, piece, name, c, w=1.0, h=0.5, mat="crystal", layer=2):
    """A small glowing lens (an eye, a slit): a gem lying along x."""
    return ml.lathe(k.part(piece, name, mat, layer=layer, smooth=0), "x", c, [(-w, 0.0), (-w * 0.25, h), (w * 0.25, h), (w, 0.0)], sides=5, caps=(False, False))


def halo_dark36(k, spikes=9):
    """The Dark Djinn's ring of thorns (the first generation's halo_dark with fewer stations: 12 round the ring, four sides on the gold edges)."""
    R_ = dub.HALO_R
    ring = [(R_ * math.cos(a), 0.0, R_ * math.sin(a)) for a in np.linspace(0, 2 * PI, 13)]
    du.sweep(k.part("halo", "ring", "plate", smooth=40), ring, C(0.62, 5))
    du.sweep(k.part("halo", "edge_top", k.trim()), [(x * 1.01, 0.5, z * 1.01) for (x, y, z) in ring], C(0.26, 4))
    du.sweep(k.part("halo", "edge_bot", k.trim()), [(x * 1.01, -0.5, z * 1.01) for (x, y, z) in ring], C(0.26, 4))
    for i in range(spikes):
        a = i * 2 * PI / spikes
        c = np.array([R_ * math.cos(a), 0.0, R_ * math.sin(a)])
        d = np.array([math.cos(a), 0.0, math.sin(a)])
        spike(k.part("halo", f"thorn_{i}", "plate"), tuple(c), tuple(-d + np.array([0, 0.25, 0])), 2.4 + (i % 2) * 0.9, 0.55, 4)
        spike(k.part("halo", f"tip_{i}", k.trim()), tuple(c), tuple(d + np.array([0, 0.5, 0])), 1.6 + (i % 2) * 0.6, 0.38, 4)
    du.gem(k.part("halo", "gem", "crystal", layer=2, smooth=0), (0, 0.0, R_ + 0.2), 0.8, long=1.1, sides=5, axis="z")


def mask(k, piece, name, c=(0.0, 0.0, 0.0), w=1.0, rot_y=0.0, mat="bone", rot_x=0.0):
    """A small face (a hexagon plate, two glowing eyes, a mouth bar) facing +z, then turned about y and moved to c."""
    n0 = len(k.m.parts)
    poly = [(-1.2 * w, 1.6 * w), (1.2 * w, 1.6 * w), (1.4 * w, 0.0), (0.8 * w, -1.8 * w), (-0.8 * w, -1.8 * w), (-1.4 * w, 0.0)]
    extrude(k, piece, name + "_f", mat, poly, -0.3 * w, 0.3 * w)
    for sgn in (-1, 1):
        gemz(k, piece, f"{name}_e{sgn}", (sgn * 0.6 * w, 0.5 * w, 0.4 * w), 0.3 * w, 1.0)
    du.bar(k.part(piece, name + "_m", "trim"), (-0.5 * w, -0.9 * w, 0.35 * w), (0.5 * w, -0.9 * w, 0.35 * w), 0.3 * w, 0.18 * w, up=(0, 0, 1))
    if rot_x:
        transform_since(k, n0, rot=("x", rot_x))
    if rot_y:
        transform_since(k, n0, rot=("y", rot_y))
    transform_since(k, n0, shift=c)


def fan(k, piece, prefix, base, angs, lens, ws, mat="plate", curl=-18.0, n=4, sx=1.0, prof=pf_feather, t=0.45, layer=0, glow=False, bz=None):
    """A fan of flat blades (feathers, rays) from one base point in the xy plane: angles in degrees from +x (mirrored by sx), lengths, widths."""
    for j, (a, ln) in enumerate(zip(angs, lens)):
        w = ws[j] if hasattr(ws, "__len__") else ws
        b = (base[0], base[1], base[2] + (0.0 if bz is None else bz * j))
        feather(k.part(piece, f"{prefix}{j}", mat, smooth=45, layer=layer, glow=glow), b, a, ln, w, t, curl, "xy", n, prof, sx=sx)


def plus(k, piece, name, c, size=1.6, w=0.7, mat="crystal", layer=2):
    """A plus sign (a healing cross) in the xy plane at c."""
    cx, cy, cz = c
    du.bar(k.part(piece, name + "_h", mat, layer=layer), (cx - size, cy, cz), (cx + size, cy, cz), w, w * 0.45, up=(0, 0, 1), section="rect", taper=0.0)
    du.bar(k.part(piece, name + "_v", mat, layer=layer), (cx, cy - size, cz), (cx, cy + size, cz), w, w * 0.45, up=(0, 0, 1), section="rect", taper=0.0)


def wheel(k, piece, name, c, R_, spokes=5, tube=0.5, mat="plate", spoke_mat="extra", hub=1.4, ang0=0.0, leaf_tip=False, n=12):
    """A spoked wheel in the xy plane: a rim, a hub, spokes (optionally ending in leaves)."""
    cx, cy, cz = c
    du.tube_direct(k.part(piece, name + "_rim", mat, smooth=40), ring_path((cx, cy, cz), R_, 0, 360, "xy", n), tube, tube, 5)
    disc(k.part(piece, name + "_hub", "trim"), (cx, cy, cz), hub, 1.0, "z", 8)
    for i in range(spokes):
        a = rad(ang0) + 2 * PI * i / spokes
        d = (math.cos(a), math.sin(a), 0.0)
        ext = R_ + (2.4 if leaf_tip else 0.0)
        du.bar(k.part(piece, f"{name}_sp{i}", spoke_mat), (cx + d[0] * hub * 0.5, cy + d[1] * hub * 0.5, cz), (cx + d[0] * ext, cy + d[1] * ext, cz), tube * 1.4, tube * 0.6, up=(0, 0, 1), taper=0.6)
        if leaf_tip:
            n_ = np.array([-d[1], d[0], 0.0])
            b = np.array([cx + d[0] * R_, cy + d[1] * R_, cz])
            lf(k, piece, f"{name}_lf{i}", "cloth", [tuple(b), tuple(b + n_ * 1.2 + np.array(d) * 1.8), tuple(b + np.array(d) * 4.2), tuple(b - n_ * 1.2 + np.array(d) * 1.8)])

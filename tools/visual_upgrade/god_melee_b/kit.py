"""Pieces the upgrade of several weapons shares (all add NAMED PARTS to a mesh_lib Mesh; centimetres, y along the weapon, the edge toward -x, the flat faces +-z)."""
import math

import numpy as np

import lib
from lib import P, ml, nl

V = np.array


def chevrons(m, name, material, base, y0, y1, n, half_w0, half_w1, rise, z_surface, w_, h_, faces=(1, -1), cx=0.0):
    """Raised chevrons (a V pointing at the tip) stacked up a blade face between y0 and y1; the arms narrow from half_w0 to half_w1."""
    p = P(m, name, material, base=base, smooth=30)
    for sgn in faces:
        for k in range(n):
            t = k / max(1, n - 1)
            y = y0 + (y1 - y0) * t
            hw = half_w0 + (half_w1 - half_w0) * t
            pts = [(cx - hw, y, sgn * z_surface), (cx, y + rise, sgn * z_surface), (cx + hw, y, sgn * z_surface)]
            ml.sweep(p, pts, ml.bevelled(2.0, 2.0, 0.45), scales=lambda tt: (w_, h_), up=(0, 0, sgn))
    return p


def spike_row(m, name, material, base, specs, rad=0.5, sides=4, glow=False):
    """Spikes from a list of (base xyz, tip xyz[, radius])."""
    p = P(m, name, material, base=base, smooth=20, glow=glow)
    for s in specs:
        r = s[2] if len(s) > 2 else rad
        nl.spike(p, s[0], s[1], r, sides)
    return p


def cog(p, c, r, teeth, thick, tooth=0.9, axis="z", hub=0.35):
    """A cog wheel centred on c (a disc with radial teeth and a hub), facing `axis` ('z' = toward the viewer in hand, 'y' = along the weapon)."""
    sides = max(6, teeth)
    if axis == "z":
        ml.lathe(p, "z", c, [(-thick, r * 0.78), (-thick * 0.45, r), (thick * 0.45, r), (thick, r * 0.78)], sides=sides)
        ml.lathe(p, "z", c, [(thick * 0.9, r * hub), (thick * 1.8, r * hub * 0.8)], sides=sides, caps=(True, True))
        for k in range(teeth):
            a = 2 * math.pi * k / teeth
            nl.spike(p, (c[0] + r * 0.92 * math.cos(a), c[1] + r * 0.92 * math.sin(a), c[2]), (c[0] + (r + tooth) * math.cos(a), c[1] + (r + tooth) * math.sin(a), c[2]), thick * 0.8, 3)
    else:
        ml.lathe(p, "y", c, [(-thick, r * 0.78), (-thick * 0.45, r), (thick * 0.45, r), (thick, r * 0.78)], sides=sides)
        for k in range(teeth):
            a = 2 * math.pi * k / teeth
            nl.spike(p, (c[0] + r * 0.92 * math.cos(a), c[1], c[2] + r * 0.92 * math.sin(a)), (c[0] + (r + tooth) * math.cos(a), c[1], c[2] + (r + tooth) * math.sin(a)), thick * 0.8, 3)


def chain(m, name, material, base, p0, p1, links, link_r, tube_r, sag=0.0, glow=False, sides=5, seg=8):
    """A hanging chain: `links` small rings alternately turned 90 degrees, from p0 to p1 (with an optional sag)."""
    p = P(m, name, material, base=base, smooth=60, glow=glow)
    p0, p1 = V(p0, float), V(p1, float)
    for i in range(links):
        t = (i + 0.5) / links
        c = p0 + (p1 - p0) * t
        c[0] += sag * math.sin(math.pi * t)
        d = (p1 - p0) / max(1e-6, np.linalg.norm(p1 - p0))
        # a ring in the plane spanned by d and a perpendicular, alternating
        perp = V((1.0, 0.0, 0.0)) if i % 2 == 0 else V((0.0, 0.0, 1.0))
        perp = perp - d * float(np.dot(perp, d))
        perp = perp / max(1e-6, np.linalg.norm(perp))
        ln = (np.linalg.norm(p1 - p0) / links) * 0.62
        pts = [tuple(c + d * ln * math.cos(a) + perp * link_r * math.sin(a)) for a in np.linspace(0, 2 * math.pi, seg + 1)]
        ml.sweep(p, pts, ml.circle(tube_r, sides), scales=lambda tt: (1.0, 1.0), caps=(False, False))
    return p


def coil(m, name, material, base, y0, y1, r, turns, tube_r, n=28, sides=4, x=0.0, z=0.0, glow=False, phase=0.0):
    """A spring / vine winding round the y axis."""
    return nl.tube(m, name, material, nl_helix(r, y0, y1, turns, n, phase, x, z), tube_r, tube_r, sides=sides, base=base, glow=glow, smooth=70)


def nl_helix(r, y0, y1, turns, n, phase=0.0, x=0.0, z=0.0):
    return [(x + r * math.cos(phase + 2 * math.pi * turns * i / n), y0 + (y1 - y0) * i / n, z + r * math.sin(phase + 2 * math.pi * turns * i / n)) for i in range(n + 1)]


def flame_ribbons(m, name, material, base, root, dirs, length, half_w, n=6, sway=2.0, up=(0, 0, 1)):
    """Flowing cloth / water / kelp ribbons from a root point along each direction in `dirs`, swaying."""
    p = P(m, name, material, base=base, smooth=35)
    for j, d in enumerate(dirs):
        d = V(d, float)
        d /= np.linalg.norm(d)
        perp = V((-d[1], d[0], 0.0))
        pts = []
        for i in range(n + 1):
            t = i / n
            pts.append(tuple(V(root) + d * length * t + perp * sway * math.sin(2.4 * t + j) * t))
        ml.sweep(p, pts, ml.bevelled(2.0, 2.0, 0.4), scales=lambda tt: (half_w * (1.0 - 0.7 * tt ** 1.5) + 0.05, 0.1), up=up)
    return p


def numerals(m, name, material, base, cy, r, n, size, thick, plane_y=True):
    """Raised radial bars (hour marks) on a disc lying in the xz plane at height cy."""
    p = P(m, name, material, base=base, smooth=30)
    for k in range(n):
        a = 2 * math.pi * k / n
        c, s = math.cos(a), math.sin(a)
        ln = size * (1.5 if k % 3 == 0 else 1.0)
        ml.bar(p, (r * c, cy, r * s), ((r + ln) * c, cy, (r + ln) * s), thick * (1.4 if k % 3 == 0 else 1.0), thick * 0.5, up=(0, 1, 0), taper=0.0)
    return p


def star4(p, c, r_long, r_short, thick):
    """A four-pointed star in the xy plane (two crossed bars)."""
    ml.bar(p, (c[0], c[1] - r_long, c[2]), (c[0], c[1] + r_long, c[2]), r_short, thick, up=(0, 0, 1), taper=r_short * 0.9)
    ml.bar(p, (c[0] - r_long, c[1], c[2]), (c[0] + r_long, c[1], c[2]), r_short, thick, up=(0, 0, 1), taper=r_short * 0.9)

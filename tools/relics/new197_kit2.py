"""More pieces for the 1.97.0 weapons: flat plates, ring blades, smooth curves, a gauntlet, a head-and-haft axe, a hammer head, a bow, a crossbow."""
import math
import os

import numpy as np

from new197_kit import *            # noqa: F401,F403
from new197_kit import P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft


# ================================================================ flat shapes and rings
def extrude(m, name, material, pts, t, edge=0.86, base=None, glow=False, smooth=40, wear=0.5, z0=0.0):
    """A flat plate of the (x, y) outline pts (counter-clockwise, star-shaped about its centroid) with a bevelled rim: thickness 2t about z = z0, the rim shrunk to `edge`."""
    pts = [(float(a), float(b)) for a, b in pts]
    cx = float(np.mean([p[0] for p in pts]))
    cy = float(np.mean([p[1] for p in pts]))
    sec = [(a - cx, b - cy) for a, b in pts]
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, wear=wear)
    ml.loft(p, "z", (0, 0, z0), [(-t, edge, edge, cx, cy), (-t * 0.45, 1.0, 1.0, cx, cy), (t * 0.45, 1.0, 1.0, cx, cy), (t, edge, edge, cx, cy)], sec)
    return p


def arc_pts(cx, cy, r, a0, a1, n=16, squash=1.0):
    """Points of a circular arc (degrees, 0 = +x), counter-clockwise for a1 > a0."""
    return [(cx + r * math.cos(math.radians(a0 + (a1 - a0) * i / n)), cy + r * squash * math.sin(math.radians(a0 + (a1 - a0) * i / n))) for i in range(n + 1)]


def ring_blade(m, name, material, cx, cy, r, half_w, half_t, n=28, bevel=0.5, base=None, glow=False, smooth=40, wear=0.5):
    """A flat ring blade in the xy plane centred on (cx, cy): mean radius r, radial half width half_w, both rims sharp."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, wear=wear)
    pts = [(cx + r * math.cos(a), cy + r * math.sin(a), 0.0) for a in np.linspace(0, 2 * math.pi, n + 1)]
    ml.sweep(p, pts, ml.bevelled(2.0, 2.0, bevel), scales=lambda t: (half_w, half_t), caps=(False, False), up=(0, 0, 1))
    return p


def catmull(pts, n=6):
    """A smooth polyline through control points [(x, y)] (Catmull-Rom, n samples per span)."""
    P_ = [np.array(p, float) for p in pts]
    P_ = [P_[0]] + P_ + [P_[-1]]
    out = []
    for i in range(1, len(P_) - 2):
        p0, p1, p2, p3 = P_[i - 1], P_[i], P_[i + 1], P_[i + 2]
        for k in range(n):
            t = k / n
            out.append(tuple(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3)))
    out.append(tuple(P_[-2]))
    return out


def sweep_blade(m, name, material, ctrl, half_w, half_t, n=6, bevel=0.5, base=None, glow=False, smooth=42, wear=0.5, tip=True):
    """A blade swept along a smooth curve through control points [(x, y)]; half_w(t), half_t(t) in 0..1 along it."""
    pts = catmull(ctrl, n)
    from new197_lib import blade_curve
    return blade_curve(m, name, material, pts, half_w, half_t, bevel=bevel, base=base, glow=glow, smooth=smooth, wear=wear, tip=tip), pts


def cuboid(m, name, material, x0, x1, y0, y1, z0, z1, base=None, glow=False, chamfer=0.6, smooth=35, wear=0.5):
    """A chamfered block."""
    p = P(m, name, material, base=base, glow=glow, smooth=smooth, wear=wear)
    ml.box(p, (x0, y0, z0), (x1, y1, z1), chamfer)
    return p


def gauntlet(m, sx, suffix, base_main, base_trim, glow_rgb=None, fist_h=15.0, wings=True, spikes=0, flare=1.0, mat="iron"):
    """One gauntlet standing on the y axis at x = sx: a forearm bracer, a flared cuff, a fist with four knuckle studs, a thumb, winged side flares. Parts end in `suffix`."""
    body = P(m, "forearm" + suffix, mat, base=base_main, smooth=45, uv_mode="loft")
    ml.loft(body, "y", (sx, 0, 0), [(0.0, 4.0, 3.4), (10.0, 4.8, 4.0), (20.0, 5.6, 4.6), (23.0, 6.2 * flare, 5.0 * flare)], ml.octagon(2.0, 2.0, 0.5))
    cuff = P(m, "cuff" + suffix, "gold", base=base_trim, smooth=40, uv_mode="loft", glow=glow_rgb is not None)
    ml.loft(cuff, "y", (sx, 0, 0), [(19.0, 6.0, 5.0), (20.0, 6.8 * flare, 5.8 * flare), (25.0, 6.8 * flare, 5.8 * flare), (26.0, 6.0, 5.0)], ml.octagon(2.0, 2.0, 0.5))
    fist = P(m, "fist" + suffix, mat, base=base_main, smooth=38)
    ml.loft(fist, "y", (sx, 0, 0), [(26.0, 5.6, 4.6), (28.0, 6.2, 5.0), (26.0 + fist_h * 0.7, 6.4, 5.2), (26.0 + fist_h, 5.4, 4.4)], ml.octagon(2.0, 2.0, 0.55))
    thumb = P(m, "thumb" + suffix, mat, base=base_main, smooth=40)
    ml.loft(thumb, "y", (sx - 6.0, 27.0, 0.0), [(0.0, 1.6, 1.8), (6.0, 1.8, 2.0), (9.0, 1.2, 1.4)], ml.octagon(2.0, 2.0, 0.5))
    kn = P(m, "knuckles" + suffix, "gold", base=base_trim, smooth=45, glow=glow_rgb is not None)
    for k in range(4):
        x = sx - 4.2 + k * 2.8
        ml.stud(kn, (x, 26.0 + fist_h + 0.2, 0.2), 1.3, 1.5, (0, 1, 0), 8, 0.5)
        if spikes:
            spike(kn, (x, 26.0 + fist_h + 1.0, 0.2), (x, 26.0 + fist_h + 1.0 + spikes, 0.2), 0.9, 5)
    plates = P(m, "finger_plates" + suffix, "gold", base=base_trim, smooth=38)
    for k in range(4):
        x = sx - 4.2 + k * 2.8
        ml.bar(plates, (x, 28.0, 4.9), (x, 26.0 + fist_h * 0.9, 4.9), 2.2, 0.35, up=(0, 0, 1), taper=1.6)
    if wings:
        wg = P(m, "wing_flares" + suffix, "gold", base=base_trim, smooth=38, glow=glow_rgb is not None)
        for sgn in (-1, 1):
            pts = [(sx + sgn * (6.0 + 4.6 * (i / 8.0) ** 0.9), 18.0 + 17.0 * (i / 8.0) ** 1.5, 0.0) for i in range(9)]
            ml.sweep(wg, pts, ml.bevelled(2.0, 2.0, 0.5), scales=lambda t: (2.4 * (1 - 0.85 * t) if t < 0.97 else 0.0, 0.5 * (1 - 0.6 * t) if t < 0.97 else 0.0), up=(0, 0, 1))
    return body


def hammer_head(m, y, w_=24.0, h=17.0, d=14.0, material="steel", base=None, name="head", glow=False, taper=0.9, bevel=2.0):
    """A hammer head: a chamfered block, wider than tall, with a slightly narrower striking face at each end (x)."""
    p = P(m, name, material, base=base, glow=glow, smooth=35)
    ml.loft(p, "x", (0, y, 0), [(-w_ / 2, 0.82 * h / 2, 0.82 * d / 2), (-w_ / 2 + 2.0, h / 2, d / 2), (w_ / 2 - 2.0, h / 2, d / 2), (w_ / 2, 0.82 * h / 2, 0.82 * d / 2)], ml.octagon(2.0, 2.0, 0.45))
    return p


def void_poly(m, name, pts, z):
    """A flat polygon of the dimensional void (layer 1, no skin) at z on the +z face and at -z on the back; pts is the (x, y) outline counter-clockwise, star-shaped about its centroid."""
    p = ml.Part(name, layer=1)
    cx = float(np.mean([a for a, _ in pts]))
    cy = float(np.mean([b for _, b in pts]))
    for sgn in (1, -1):
        ring = [p.add_vertex((a, b, sgn * z)) for a, b in (pts if sgn > 0 else pts[::-1])]
        c = p.add_vertex((cx, cy, sgn * z))
        for k in range(len(ring)):
            p.add_face(c, ring[k], ring[(k + 1) % len(ring)], ring[(k + 1) % len(ring)])
    m.add(p)
    return p


def void_strip(m, name, y0, y1, half_w, z, cx=0.0, jag=0.0, n=8):
    """A strip of the void down a blade face between y0 and y1 (half width half_w about x = cx), with a jagged edge when jag > 0."""
    rng = np.random.default_rng(len(name) * 7 + int(y0))
    ys = np.linspace(y0, y1, n + 1)
    left = [(cx - half_w * (1 + jag * (rng.random() - 0.5) * 2), float(y)) for y in ys]
    right = [(cx + half_w * (1 + jag * (rng.random() - 0.5) * 2), float(y)) for y in ys]
    pts = right + left[::-1]                                  # up the right edge, down the left: counter-clockwise seen from +z
    return void_poly(m, name, pts, z)

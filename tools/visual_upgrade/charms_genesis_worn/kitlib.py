"""kitlib: assemblies the items of the slice share (a base, a gyroscope, orbiting shards, a halo set, a fx set), built on geo.py and vu.py.

Every function takes the VU, builds into new parts of its own and returns what it made. Dimensions are model pixels (1/16 block), Y up.
"""
import math

import numpy as np

import geo
import vu as V
from vu import MAIN, TRIM, GLOW, DARK, ACCENT


def rgb(c):
    return ((c >> 16) & 255, (c >> 8) & 255, c & 255)


def lighter(c, k):
    r, g, b = rgb(c)
    return (min(255, int(r + (255 - r) * k)), min(255, int(g + (255 - g) * k)), min(255, int(b + (255 - b) * k)))


def darker(c, k):
    r, g, b = rgb(c) if isinstance(c, int) else c
    return (int(r * (1 - k)), int(g * (1 - k)), int(b * (1 - k)))


def mixc(a, b, t):
    ra, rb = (rgb(a) if isinstance(a, int) else a), (rgb(b) if isinstance(b, int) else b)
    return tuple(int(ra[i] + (rb[i] - ra[i]) * t) for i in range(3))


# ------------------------------------------------------------------ fx sets
def halo_set(vm, pal, y=0.0, R=9.0, group_ring=None, group_flare=None, rune=True, flare=True, shafts=True, shaft_len=16.0, tilt=0.0):
    """The additive layers most items share: a rotating rune ring in the horizontal plane, a pulsing lens flare, light shafts up and down.
    Returns the groups made {'ring', 'flare', 'shafts'} (declared by the caller's layers list)."""
    out = {}
    if rune:
        g = vm.grp("halo_ring", "spin", (0, y, 0), 1.1)
        n = (math.sin(math.radians(tilt)), math.cos(math.radians(tilt)), 0.0)
        vm.fx_disc(g, GLOW, (0, y, 0), n, R * 2.0, "rune")
        vm.fx_disc(g, DARK, (0, y, 0), n, R * 1.55, "ticks", rot=17)
        out["ring"] = g
    if flare:
        g = vm.grp("lens_flare", "pulse", (0, y, 0), 0.16, 0.10, 0.0)
        vm.fx_cross(g, ACCENT, (0, y, 0), (0, 1, 0), R * 1.25, "flare", n=3, lod=True)
        out["flare"] = g
    if shafts:
        g = vm.grp("light_shafts", "pulse", (0, y, 0), 0.10, 0.07, 1.3)
        vm.fx_beam(g, ACCENT, (0, y - shaft_len * 0.5, 0), (0, y + shaft_len * 0.5, 0), R * 0.7, "beam", lod=True)
        out["shafts"] = g
    return out


def gyro(vm, name, group, R, wire, tilt_deg, material, base, n=30, m=4, tilt_axis="z", center=(0, 0, 0), lod=False):
    """A thin ring in a tilted plane; the animation group spins/tumbles it."""
    p = vm.P(name, material, group=group, base=base, uv="dom", lod=lod)
    axis = geo.rot_matrix(tilt_axis, tilt_deg) @ np.array([0.0, 1.0, 0.0])
    geo.ring_part(p, center, R, wire, normal=axis, n=vm.S(n, 8), m=max(3, vm.S(m, 3)))
    return p


def shard_set(vm, name, group, n, R, y, r, h, material, base, tilt=0.0, phase=0.0, sides=4, alt=None, spin_up=True, lod=False, octa=False):
    """n crystal shards on a circle (each pointing along the circle's tangent-up): orbiting jewellery. octa: a bipyramid (8 faces) instead of the column."""
    p = vm.P(name, material, group=group, base=base, lod=lod)
    cr = geo.prim_octa(r, h / 2) if octa else geo.prim_crystal(r, h, sides=max(3, vm.S(sides, 3)), tip=0.5)
    geo.scatter_ring(p, cr, n, R, y=y, up_out=False, tilt=tilt, phase=phase, alt=alt)
    return p


def plinth(vm, name, y0, r0, h, material, base, trim_material="gold", trim_base=None, sides=12, petals=0, petal_len=4.0, group=0, lod=True):
    """A stepped, lathe-turned base (foot, neck, cup) with an optional crown of petal prongs; returns (body, trim)."""
    body = vm.P(name, material, group=group, base=base, lod=lod)
    s = vm.S(sides, 5)
    prof = [(y0, r0 * 0.8), (y0 + h * 0.12, r0), (y0 + h * 0.3, r0 * 0.92), (y0 + h * 0.5, r0 * 0.5), (y0 + h * 0.7, r0 * 0.42), (y0 + h * 0.9, r0 * 0.78), (y0 + h, r0 * 0.7)]
    geo.lathe(body, "y", (0, 0, 0), prof, sides=s, caps=(True, False))
    trim = vm.P(name + "_trim", trim_material, group=group, base=trim_base, slot=TRIM)
    for yy, rr in ((y0 + h * 0.12, r0 * 1.02), (y0 + h * 0.5, r0 * 0.56), (y0 + h * 0.9, r0 * 0.8)):
        geo.lathe(trim, "y", (0, 0, 0), [(yy - 0.25, rr), (yy + 0.25, rr + 0.18)], sides=s, caps=(False, False))
    if petals:
        leaf = geo.prim_leaf(petal_len, r0 * 0.55, 0.5, sides=2, bend=-0.02)
        geo.scatter_ring(trim, leaf, petals, r0 * 0.7, y=y0 + h, up_out=True, tilt=62.0)
    return body, trim


def claws(vm, name, group, n, R, y_base, y_tip, r, material, base, curl=0.8, sides=4, phase=0.0, taper=0.15, segs=4, lod=False):
    """n curved prongs rising from a ring of radius R at y_base and curling in toward the axis, tip at y_tip."""
    p = vm.P(name, material, group=group, base=base, lod=lod)
    for i in range(n):
        th = phase + 2 * math.pi * i / n
        c, s = math.cos(th), math.sin(th)
        path = []
        ns = vm.S(segs, 2)
        for k in range(ns + 1):
            t = k / ns
            rad = R * (1 - curl * t * t) + R * 0.08 * math.sin(t * math.pi)
            path.append((c * rad, y_base + (y_tip - y_base) * t, s * rad))
        sc = [(max(taper, 1.0 - 0.85 * (k / (len(path) - 1))),) * 2 for k in range(len(path))]
        geo.sweep(p, path, geo.circle(r, vm.S(sides, 3)), scales=sc, caps=(True, True))
    return p


def cube_edges(vm, name, group, H, w, material, base, caps=False, lod=True):
    """The 12 edges of a cube of half size H as square bars; open ends (corner pieces cover them)."""
    p = vm.P(name, material, group=group, base=base, lod=lod)
    bar = geo.new_part("bar")
    geo.loft(bar, "y", (0, 0, 0), [-H, H], geo.rect_ring(w, w), caps=(caps, caps))
    for dx in (-1, 1):
        for dz in (-1, 1):
            geo.merge(p, bar, None, (dx * H, 0, dz * H))                                  # four vertical
    for dy in (-1, 1):
        for dz in (-1, 1):
            geo.merge(p, bar, geo.rot_matrix("z", 90), (0, dy * H, dz * H))               # four along x
    for dy in (-1, 1):
        for dx in (-1, 1):
            geo.merge(p, bar, geo.rot_matrix("x", 90), (dx * H, dy * H, 0))               # four along z
    return p


def cube_corners(vm, name, group, H, r, material, base, sides=4, lod=True):
    p = vm.P(name, material, group=group, base=base, slot=TRIM, lod=lod)
    g = geo.prim_octa(r, r * 0.9)
    for dx in (-1, 1):
        for dy in (-1, 1):
            for dz in (-1, 1):
                geo.merge(p, g, None, (dx * H, dy * H, dz * H))
    return p


def orbs(vm, name, group, n, R, y, r, material, base, phase=0.0, lat=3, lon=5, slot=MAIN, lod=False, octa=True, wobble=0.0):
    """n small jewels on a circle (souls, planets, pearls)."""
    p = vm.P(name, material, group=group, base=base, slot=slot, lod=lod)
    prim = geo.prim_octa(r, r * 1.15) if octa else geo.prim_sphere(r, lat, lon)
    for i in range(n):
        th = phase + 2 * math.pi * i / n
        geo.merge(p, prim, None, (R * math.cos(th), y + wobble * math.sin(th * 2), R * math.sin(th)))
    return p


def gem_column(vm, name, group, r, h, sides=6, tip=0.42, material="crystal", base=(255, 220, 90), slot=GLOW, lod=True, y=0.0):
    p = vm.P(name, material, group=group, base=base, slot=slot, lod=lod)
    geo.merge(p, geo.prim_crystal(r, h, sides=vm.S(sides, 3), tip=tip), None, (0, y, 0))
    return p


def vertical_spiral_ridges(vm, name, group, n, R, y0, y1, r, material, base, twist=1.0, sides=3, segs=6, lod=False):
    """n ridges (tubes) climbing a body of revolution from y0 to y1 with a twist (turns), radius R at y0 tapering."""
    p = vm.P(name, material, group=group, base=base, slot=TRIM, lod=lod)
    ns = vm.S(segs, 3)
    for i in range(n):
        ph = 2 * math.pi * i / n
        pts = []
        for k in range(ns + 1):
            t = k / ns
            a = ph + twist * 2 * math.pi * t
            rr = R * (1 - 0.35 * t)
            pts.append((rr * math.cos(a), y0 + (y1 - y0) * t, rr * math.sin(a)))
        geo.tube_path(p, pts, r, sides=sides, caps=(False, True))
    return p

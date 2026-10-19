"""Composite pieces of the 35 weapons of 1.97.0: guards, pommels, grips, heads and the sword / polearm / axe / hammer assemblers built on new197_lib.

Every function adds named parts to a mesh_lib Mesh and returns what a painter needs. Geometry is in centimetres, y along the weapon, the edge toward -x.
"""
import math

import numpy as np

from new197_lib import *            # noqa: F401,F403
from new197_lib import P, plate, blade_curve, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, void_plate, ml, w, Outline, paint, put_glow, sm, step, darken


# ================================================================ grips and pommels
def grip(m, y0, y1, r=1.5, base=(70, 50, 40), wrap_base=(150, 130, 100), n_wraps=7, material="leather", sides=10, name="grip", swell=1.06, wrap_material="leather"):
    """A grip (a round loft with a slow swell) with binding rings."""
    p = P(m, name, material, base=base, uv_mode="loft", smooth=55)
    ml.loft(p, "y", (0, 0, 0), [(y0, 0.96, 0.96), (y0 + (y1 - y0) * 0.3, 1.0, 1.0), ((y0 + y1) / 2, swell, swell), (y1 - (y1 - y0) * 0.2, 1.0, 1.0), (y1, 0.98, 0.98)], ml.circle(r, sides))
    if n_wraps:
        wrap(m, name + "_wraps", y0, y1, r, n_wraps, wrap_material, wrap_base, sides)
    return p


def pommel(m, y, style="orb", r=2.4, material="gold", base=None, glow_rgb=None, name="pommel"):
    """A pommel under the grip at y (its top touches y): orb, disc, spike, ring or eye."""
    if style == "orb":
        orb(m, name, material, (0, y - r * 0.8, 0), r, base=base, glow=glow_rgb is not None)
    elif style == "disc":
        lathe_y(m, name, material, y - r * 0.9, [(0.0, r * 0.4), (0.3, r), (r * 0.5, r), (r * 0.9, r * 0.55)], sides=16, base=base)
    elif style == "spike":
        lathe_y(m, name, material, y - r * 2.2, [(0.0, 0.0), (r * 1.4, r * 0.55), (r * 2.2, r)], sides=8, base=base, caps=(False, True))
    elif style == "ring":
        pts = [(r * 1.0 * math.cos(a), y - r * 0.9, r * 1.0 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)]
        tube(m, name, material, pts, r * 0.32, r * 0.32, sides=6, base=base)
    elif style == "eye":
        lathe_y(m, name, material, y - r * 1.5, [(0.0, r * 0.4), (r * 0.7, r), (r * 1.4, r * 0.9), (r * 1.6, r * 0.4)], sides=12, base=base)
        orb(m, name + "_eye", "crystal", (0, y - r * 0.8, r * 0.8), r * 0.55, base=base and (255, 70, 70), glow=True, rz=r * 0.35)
    return None


# ================================================================ guards
def guard_cross(m, y, half=7.0, t=1.5, h=1.5, material="gold", base=None, bulb=1.2, curve=0.0, name="guard", glow=False):
    """A cross guard: a bar through the blade along x with a bulb at each end and a collar; `curve` bends the ends toward the tip (a quillon curve)."""
    p = P(m, name, material, base=base, smooth=40, glow=glow)
    pts = [(-half + 2 * half * i / 10.0, y + curve * (abs(2.0 * i / 10.0 - 1.0)) ** 2, 0.0) for i in range(11)]
    ml.sweep(p, pts, ml.octagon(2.0, 2.0, 0.5), scales=lambda tt: (h * (1.0 - 0.45 * abs(2 * tt - 1) ** 3), t * (1.0 - 0.35 * abs(2 * tt - 1) ** 3)), up=(0, 0, 1))
    if bulb:
        for sx in (-1, 1):
            ml.lathe(p, "x", (sx * half, y + curve, 0), [(-bulb * 0.1, bulb * 0.2), (bulb * 0.7, bulb), (bulb * 1.5, bulb * 0.35)] if sx > 0 else [(bulb * 0.1, bulb * 0.2), (-bulb * 0.7, bulb), (-bulb * 1.5, bulb * 0.35)], sides=8)
    ml.collar(p, y - h * 0.9, t * 1.15, h * 1.8, lip=0.2, sides=10, oval=1.0)
    return p


def guard_wings(m, y, span=10.0, feathers=4, rise=7.0, t=0.8, material="gold", base=None, name="guard", glow=False, droop=0.0):
    """Swept feather wings: `feathers` blades fanning up and out on each side of the blade root."""
    p = P(m, name, material, base=base, smooth=38, glow=glow)
    for sx in (-1, 1):
        for k in range(feathers):
            f = k / max(1, feathers - 1)
            L = span * (1.0 - 0.35 * f)
            pts = [(sx * (1.2 + L * (i / 8.0)), y + rise * (1.0 - f * 0.8) * (i / 8.0) ** 1.6 - droop * (i / 8.0) - 0.8 * k * 0.0, 0.0) for i in range(9)]
            ml.sweep(p, pts, ml.bevelled(2.0, 2.0, 0.5), scales=lambda tt, f=f: ((1.5 - f * 0.7) * (1 - 0.8 * tt) + 0.2 if tt < 0.97 else 0.0, t * (1 - 0.6 * tt) if tt < 0.97 else 0.0), up=(0, 0, 1))
    ml.collar(p, y - 1.2, 2.0, 2.4, lip=0.3, sides=10)
    return p


def guard_disc(m, y, r=5.0, thick=0.7, material="gold", base=None, name="guard", glow=False, notches=0):
    """A round disc guard (a tsuba / sun disc) with an optional ring of spikes."""
    p = P(m, name, material, base=base, smooth=40, glow=glow, uv_mode="loft")
    ml.lathe(p, "y", (0, y, 0), [(-thick, r * 0.45), (-thick * 0.4, r * 0.97), (thick * 0.4, r), (thick, r * 0.5)], sides=24)
    if notches:
        for i in range(notches):
            a = 2 * math.pi * i / notches
            spike(p, (r * 0.95 * math.cos(a), y, r * 0.95 * math.sin(a)), (r * 1.45 * math.cos(a), y, r * 1.45 * math.sin(a)), thick * 0.9, 4)
    return p


def guard_leaf(m, y, r=4.0, material="gold", base=None, name="guard", glow=False):
    """A four-leaf clover guard: four heart lobes round the blade root in the xz cross."""
    p = P(m, name, material, base=base, smooth=45, glow=glow, uv_mode="loft")
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        w.ellipsoid(p, (dx * r * 0.8, y, dz * r * 0.8), r * 0.62, 0.7, r * 0.62, sides=10, stacks=6)
    ml.collar(p, y - 1.0, 1.8, 2.0, lip=0.3, sides=10)
    return p


def guard_horns(m, y, span=8.0, rise=6.0, t=1.0, material="iron", base=None, name="guard", glow=False):
    """Two curved horns sweeping out and up from the blade root."""
    p = P(m, name, material, base=base, smooth=40, glow=glow)
    for sx in (-1, 1):
        pts = [(sx * (1.2 + span * (i / 10.0) ** 0.8), y + rise * (i / 10.0) ** 2.2, 0.0) for i in range(11)]
        ml.sweep(p, pts, ml.octagon(2.0, 2.0, 0.5), scales=lambda tt: (t * (1 - 0.9 * tt) if tt < 0.97 else 0.0, t * (1 - 0.9 * tt) if tt < 0.97 else 0.0), up=(0, 0, 1))
    ml.collar(p, y - 1.0, 2.0, 2.2, lip=0.3, sides=10)
    return p


def guard_ring(m, y, r=4.5, tube_r=0.7, material="gold", base=None, name="guard", glow=False):
    """A ring guard (a tsuba of a katana as a torus) round the blade root, facing the blade (in the xz plane)."""
    pts = [(r * math.cos(a), y, r * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 21)]
    return tube(m, name, material, pts, tube_r, tube_r, sides=6, base=base, glow=glow)


def collar_ring(m, y, r, h, material="gold", base=None, name="collar", glow=False, sides=12):
    p = P(m, name, material, base=base, smooth=40, glow=glow, uv_mode="loft")
    ml.collar(p, y, r, h, lip=min(0.3, h / 3), sides=sides)
    return p


# ================================================================ assemblers
def straight_sword(m, blade_st, y_guard, *, guard="cross", guard_kw=None, grip_len=None, pommel_style="orb", pommel_kw=None, blade_material="steel", blade_base=None,
                   guard_material="gold", guard_base=None, grip_base=(70, 50, 40), wrap_base=(150, 130, 100), bevel=0.45, ricasso=2.0, glow_blade=False):
    """A straight sword: a plate blade from the stations (y, xl, xr, t) starting at y_guard, a guard, a grip down to y = 0 and a pommel. Returns the Outline."""
    gk = dict(guard_kw or {})
    pk = dict(pommel_kw or {})
    grip_len = grip_len if grip_len is not None else y_guard
    st = [(y_guard - ricasso, -blade_st[0][2] * 0.0 + blade_st[0][1], blade_st[0][2], blade_st[0][3])] + list(blade_st)
    plate(m, "blade", blade_material, st, bevel=bevel, base=blade_base, smooth=48, glow=glow_blade)
    getattr(__import__("new197_kit"), "guard_" + guard)(m, y_guard, material=guard_material, base=guard_base, **gk)
    grip(m, y_guard - grip_len + 3.0, y_guard - 0.6, base=grip_base, wrap_base=wrap_base)
    pommel(m, y_guard - grip_len + 3.4, pommel_style, material=guard_material, base=guard_base, **pk)
    return Outline(st)


def spear_head(m, y, length, half_w, t=0.9, base=None, material="steel", glow=False, leaf=0.35, name="head", bevel=0.45):
    """A leaf-shaped spear head from y to y + length (the widest part at `leaf` of its length)."""
    ys = [0.0, 0.08, 0.2, leaf, 0.55, 0.75, 0.92, 1.0]
    ws = [0.18, 0.5, 0.88, 1.0, 0.84, 0.55, 0.2, 0.0]
    st = [(y + length * a, -half_w * b, half_w * b, t * (0.6 + 0.4 * b) if b > 0 else 0.0) for a, b in zip(ys, ws)]
    plate(m, name, material, st, bevel=bevel, base=base, glow=glow, smooth=45)
    return Outline(st)


def shaft(m, y0, y1, r=1.5, material="wood", base=(90, 66, 44), name="shaft", sides=10, swell=None):
    return rod(m, name, material, y0, y1, r, r, sides=sides, base=base, swell=swell, smooth=55)

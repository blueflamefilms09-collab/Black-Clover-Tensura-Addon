"""Skill icons v3: the symbols. Every function draws one icon's symbol on a Ctx (0..100 units, centre 50,50). Palettes are dicts a / b / c (parts), glow, bg0 (lit centre), bg1 (dark edge)."""
import math

import numpy as np

from icon3_lib import (blur, circle, line, lighten, darken, mix, poly, ring, soften, spline, star, taper, S, U)


def pal(a, b, c, glow, bg0, bg1):
    return dict(a=a, b=b, c=c, glow=glow, bg0=bg0, bg1=bg1)


# ------------------------------------------------------------------------------------------------ shared pieces
def flame(c, cx, base_y, h, w, col_a=(255, 150, 30), col_b=(255, 60, 10), core=(255, 240, 170), k=1.0, side=True):
    """A flame: one tall tongue, two shorter side tongues and a bright core. (cx, base_y) = bottom centre; h, w in units."""
    def pt(x, y):
        return (cx + x * w, base_y - y * h)
    pts = [pt(-0.5, 0.0), pt(-0.56, 0.22), pt(-0.46, 0.44), pt(-0.38, 0.52), pt(-0.40, 0.70), pt(-0.22, 0.52), pt(-0.14, 0.7), pt(0.0, 1.0),
           pt(0.12, 0.72), pt(0.24, 0.56), pt(0.40, 0.74), pt(0.40, 0.46), pt(0.54, 0.34), pt(0.52, 0.16), pt(0.5, 0.0)]
    if not side:
        pts = [pt(-0.5, 0.0), pt(-0.5, 0.25), pt(-0.28, 0.55), pt(-0.12, 0.7), pt(0.0, 1.0), pt(0.14, 0.7), pt(0.3, 0.52), pt(0.5, 0.25), pt(0.5, 0.0)]
    m = poly(pts, round_=2.2)
    c.part(m, col_a, bevel=4.5, grad=(col_a, col_b), spec=0.1, k=k)
    inner = poly([pt(-0.26, 0.02), pt(-0.3, 0.2), pt(-0.12, 0.42), pt(0.0, 0.62), pt(0.12, 0.42), pt(0.3, 0.2), pt(0.26, 0.02)], round_=2.0)
    c.add(blur(inner, 3), core, 0.8)
    return m


def M(c, pts, col, **kw):
    """A part drawn on both sides of the centre line (pts are the right half)."""
    m = poly(pts, mirror=True, round_=kw.pop("round_", 1.0))
    c.part(m, col, **kw)
    return m


def eyes2(c, x, y, w, col, h=None, tilt=0.35):
    """A pair of slit eyes at 50 +- x: dark lid, glowing slit."""
    h = h or w * 0.42
    right = [(50 + x - w * 0.5, y + h * tilt), (50 + x + w * 0.5, y - h * tilt), (50 + x + w * 0.5, y + h * (1 - tilt)), (50 + x - w * 0.5, y + h * (1 + tilt))]
    m = poly(right, mirror=True, round_=0.8)
    c.flat(m, (12, 6, 16), 0.95)
    inner = poly([(50 + x - w * 0.4, y + h * 0.5), (50 + x + w * 0.4, y + h * 0.1), (50 + x + w * 0.4, y + h * 0.7), (50 + x - w * 0.4, y + h * 1.0)], mirror=True, round_=0.6)
    c.flat(inner, col, 1.0)
    c.add(blur(m, 3.0), col, 0.55)


def horn(c, base, tip, w, curve, col=(238, 226, 200)):
    """A horn on both sides: base and tip are the right-side points."""
    c.part(taper(base, tip, w, 1.2, curve=curve), col, bevel=2.2, spec=0.4)
    c.part(taper((100 - base[0], base[1]), (100 - tip[0], tip[1]), w, 1.2, curve=-curve), col, bevel=2.2, spec=0.4)


def wing(c, x, y, length, spread, col, mirror=True, feathers=6, tilt=0.0):
    """A feathered wing rooted at (x, y), sweeping up and outwards; mirror=True also draws the left one (rooted at 100 - x)."""
    sides = (-1,) if mirror == "left" else ((1, -1) if mirror else (1,))
    for side in sides:
        rx = x if side == 1 else 100 - x
        for i in range(feathers):
            f = i / max(1, feathers - 1)
            L = length * (1.0 - 0.38 * f)
            # direction measured from straight up, towards the outside
            a = math.radians(10 + f * spread + tilt)
            tip = (rx + side * L * math.sin(a), y - L * math.cos(a))
            c.part(taper((rx, y), tip, 13.0 - 3.0 * f, 3.0, curve=side * L * 0.16), col, bevel=2.4, spec=0.3)


def eye(c, cx, cy, w, h, iris, pupil=(10, 6, 20), lid=(30, 20, 40), slit=False):
    almond = poly([(cx - w, cy), (cx - w * 0.5, cy - h * 0.8), (cx, cy - h), (cx + w * 0.5, cy - h * 0.8), (cx + w, cy),
                   (cx + w * 0.5, cy + h * 0.8), (cx, cy + h), (cx - w * 0.5, cy + h * 0.8)], round_=2.0)
    c.part(almond, (236, 230, 220), bevel=3.0, spec=0.2)
    ir = circle(cx, cy, h * 0.78)
    c.flat(ir * almond, iris, 1.0)
    c.add(blur(ir * almond, 3), iris, 0.25)
    pm = poly([(cx - 1.6, cy - h * 0.8), (cx, cy - h * 0.9), (cx + 1.6, cy - h * 0.8), (cx + 1.6, cy + h * 0.8), (cx, cy + h * 0.9), (cx - 1.6, cy + h * 0.8)], round_=1.0) if slit else circle(cx, cy, h * 0.38)
    c.flat(pm * almond, pupil, 1.0)
    c.flat(circle(cx - h * 0.25, cy - h * 0.28, h * 0.14), (255, 255, 255), 0.9)


def skull(c, cx, cy, s, bone=(235, 226, 208), eyes=(255, 70, 40)):
    cr = circle(cx, cy - s * 0.12, s * 0.62, s * 0.58)
    jaw = poly([(cx - s * 0.36, cy + s * 0.2), (cx + s * 0.36, cy + s * 0.2), (cx + s * 0.3, cy + s * 0.62), (cx - s * 0.3, cy + s * 0.62)], round_=1.5)
    c.part(np.maximum(cr, jaw), bone, bevel=3.2)
    for sx in (-1, 1):
        c.flat(circle(cx + sx * s * 0.26, cy - s * 0.08, s * 0.17, s * 0.2), (14, 6, 12), 1.0)
        c.add(blur(circle(cx + sx * s * 0.26, cy - s * 0.08, s * 0.1), 2.0), eyes, 0.9)
    c.flat(poly([(cx, cy + s * 0.12), (cx - s * 0.07, cy + s * 0.26), (cx + s * 0.07, cy + s * 0.26)], round_=0.5), (14, 6, 12), 1.0)
    for i in range(-2, 3):
        c.flat(line([(cx + i * s * 0.11, cy + s * 0.42), (cx + i * s * 0.11, cy + s * 0.6)], 0.9, False), (60, 40, 50), 0.8)


def sword(c, cx, top, bottom, w=8, blade=(205, 218, 235), guard=(230, 180, 60), grip=(110, 60, 40), edge=(250, 252, 255)):
    bl = poly([(cx - w / 2, bottom - 20), (cx - w / 2, top + 8), (cx, top), (cx + w / 2, top + 8), (cx + w / 2, bottom - 20)], round_=0.8)
    c.part(bl, blade, bevel=2.6, spec=0.5)
    c.flat(line([(cx, top + 6), (cx, bottom - 22)], 1.2, False), edge, 0.55)
    c.part(poly([(cx - 16, bottom - 22), (cx + 16, bottom - 22), (cx + 13, bottom - 16), (cx - 13, bottom - 16)], round_=1.0), guard, bevel=2.2, spec=0.5)
    c.part(poly([(cx - 3, bottom - 16), (cx + 3, bottom - 16), (cx + 3, bottom - 5), (cx - 3, bottom - 5)], round_=0.8), grip, bevel=1.6)
    c.part(circle(cx, bottom - 3, 4.6), guard, bevel=2.0, spec=0.5)


def orb(c, cx, cy, r, col, core=(255, 255, 255)):
    m = circle(cx, cy, r)
    c.part(m, col, bevel=r * 0.5, spec=0.6)
    c.add(blur(circle(cx - r * 0.25, cy - r * 0.3, r * 0.5), 3.0), core, 0.75)


def book(c, cx, cy, w, h, cover=(120, 60, 150), pages=(240, 228, 200), open_=True):
    if open_:
        l = poly([(cx - w, cy - h * 0.55), (cx - 2, cy - h * 0.7), (cx - 2, cy + h * 0.6), (cx - w, cy + h * 0.45)], round_=1.0)
        r = poly([(cx + w, cy - h * 0.55), (cx + 2, cy - h * 0.7), (cx + 2, cy + h * 0.6), (cx + w, cy + h * 0.45)], round_=1.0)
        c.part(poly([(cx - w - 3, cy - h * 0.45), (cx + w + 3, cy - h * 0.45), (cx + w + 3, cy + h * 0.62), (cx - w - 3, cy + h * 0.62)], round_=1.0), cover, bevel=2.4)
        c.part(l, pages, bevel=2.0, spec=0.1)
        c.part(r, pages, bevel=2.0, spec=0.1)
        for i in range(4):
            y = cy - h * 0.4 + i * h * 0.22
            c.flat(line([(cx - w + 5, y), (cx - 8, y + 1)], 0.9, False), (150, 120, 100), 0.7)
            c.flat(line([(cx + 8, y + 1), (cx + w - 5, y)], 0.9, False), (150, 120, 100), 0.7)


def crown(c, cx, y, w, col=(246, 200, 70)):
    m = poly([(cx - w, y + 9), (cx - w, y - 3), (cx - w * 0.5, y + 3), (cx, y - 8), (cx + w * 0.5, y + 3), (cx + w, y - 3), (cx + w, y + 9)], round_=1.0)
    c.part(m, col, bevel=2.2, spec=0.5)
    for dx in (-w, 0, w):
        c.dot(cx + dx, y - (8 if dx == 0 else 3) - 1, 1.8, (255, 90, 110), 1.0, glow=False)



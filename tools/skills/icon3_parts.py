"""Skill icons v3: more shared pieces (rotation, sword, rays, bell, boot, hexagram) and the icon registry."""
import math

import numpy as np

from icon3_lib import blur, circle, line, lighten, poly, ring, star, taper
from icon3_symbols import orb  # noqa: F401  (re-exported for the symbol files)

ICONS = []


def icon(id_, cls, palette, anim=False):
    """Registers a symbol function as the icon of skill id_ (cls: unique / extra / ultimate; anim: animated strip)."""
    def deco(fn):
        ICONS.append(dict(id=id_, cls=cls, pal=palette, fn=fn, anim=anim))
        return fn
    return deco


def R(pts, ang, cx=50.0, cy=50.0):
    """Rotates points by ang degrees (clockwise on screen) round (cx, cy)."""
    a = math.radians(ang)
    ca, sa = math.cos(a), math.sin(a)
    return [(cx + (x - cx) * ca - (y - cy) * sa, cy + (x - cx) * sa + (y - cy) * ca) for x, y in pts]


def blade_sword(c, ang=0.0, cx=50, top=6, bot=94, w=9, blade=(206, 220, 238), guard=(236, 186, 64), grip=(120, 66, 44), wings=False, runes=None):
    """A longsword; every point is rotated by ang round the middle of the picture."""
    def p(pts):
        return R(pts, ang)
    gy = bot - 26
    bl = poly(p([(cx - w / 2, gy), (cx - w / 2, top + 10), (cx, top), (cx + w / 2, top + 10), (cx + w / 2, gy)]), round_=0.8)
    c.part(bl, blade, bevel=2.8, spec=0.55)
    c.flat(poly(p([(cx - 0.9, gy - 2), (cx - 0.9, top + 12), (cx, top + 6), (cx + 0.9, top + 12), (cx + 0.9, gy - 2)]), round_=0.3), lighten(blade, 0.7), 0.7)
    c.flat(poly(p([(cx + 1.0, gy - 2), (cx + 1.0, top + 12), (cx + w / 2 - 0.5, top + 10), (cx + w / 2 - 0.5, gy)]), round_=0.3), (20, 20, 50), 0.35)
    if runes:
        for i in range(4):
            yy = top + 20 + i * 9
            c.add(blur(poly(p([(cx - 1.4, yy), (cx + 1.4, yy), (cx + 1.4, yy + 3), (cx - 1.4, yy + 3)]), round_=0.3), 2.0), runes, 0.9)
    gw = 17
    c.part(poly(p([(cx - gw, gy), (cx - gw + 3, gy - 4), (cx + gw - 3, gy - 4), (cx + gw, gy), (cx + gw - 4, gy + 5), (cx - gw + 4, gy + 5)]), round_=1.0), guard, bevel=2.2, spec=0.55)
    if wings:
        for sx in (-1, 1):
            c.part(poly(p([(cx + sx * gw, gy), (cx + sx * (gw + 9), gy - 12), (cx + sx * (gw + 4), gy - 1), (cx + sx * (gw + 10), gy + 7), (cx + sx * gw, gy + 3)]), round_=0.8),
                   lighten(guard, 0.3), bevel=1.8, spec=0.4)
    c.part(poly(p([(cx - 3, gy + 5), (cx + 3, gy + 5), (cx + 3, bot - 7), (cx - 3, bot - 7)]), round_=0.6), grip, bevel=1.6)
    for i in range(3):
        yy = gy + 8 + i * 4
        c.flat(poly(p([(cx - 3, yy), (cx + 3, yy), (cx + 3, yy + 1.2), (cx - 3, yy + 1.2)]), round_=0.2), (30, 14, 10), 0.5)
    px, py = R([(cx, bot - 4)], ang)[0]
    c.part(circle(px, py, 4.6), guard, bevel=2.0, spec=0.6)
    qx, qy = R([(cx - 1, bot - 5)], ang)[0]
    c.dot(qx, qy, 1.5, (255, 255, 255), 0.7, glow=False)


def rays(c, cx, cy, r0, r1, n, col, alpha=0.9, w=3.0, rot=0.0):
    """Radiating blades behind a symbol (a flat tint, no outline)."""
    for i in range(n):
        a = math.radians(rot + i * 360.0 / n)
        c.flat(taper((cx + r0 * math.cos(a), cy + r0 * math.sin(a)), (cx + r1 * math.cos(a), cy + r1 * math.sin(a)), w, 0.5), col, alpha)


def burst(c, cx, cy, r_out, r_in, n, col_a, col_b, rot=-90):
    pts = []
    for i in range(n * 2):
        a = math.radians(rot + i * 180.0 / n)
        r = r_out if i % 2 == 0 else r_in
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    c.part(poly(pts, round_=0.8), col_a, bevel=3.0, grad=(col_a, col_b), spec=0.15)


def bell(c, cx, top, col=(240, 200, 90), w=40, h=48, clapper=(220, 160, 50)):
    body = poly([(cx - 5, top + 2), (cx, top - 2), (cx + 5, top + 2), (cx + w * 0.34, top + h * 0.3), (cx + w * 0.4, top + h * 0.62), (cx + w * 0.55, top + h * 0.86),
                 (cx + w * 0.55, top + h), (cx - w * 0.55, top + h), (cx - w * 0.55, top + h * 0.86), (cx - w * 0.4, top + h * 0.62), (cx - w * 0.34, top + h * 0.3)], round_=2.0)
    c.part(body, col, bevel=4.2, spec=0.55)
    c.part(circle(cx, top + h + 3, 5.2), clapper, bevel=2.6, spec=0.6)
    c.flat(line([(cx - w * 0.5, top + h * 0.9), (cx + w * 0.5, top + h * 0.9)], 2.0, False), (60, 30, 10), 0.35)
    c.part(circle(cx, top - 2, 3.2), clapper, bevel=1.6)


def boot(c, col=(210, 70, 50), sole=(60, 36, 36), x=46, y=50, s=1.0):
    """A high boot seen from the side, toe to the right: cuff, shaft, instep, thick sole."""
    def t(pts):
        return [(x + (px - 50) * s, y + (py - 50) * s) for px, py in pts]
    shaft = poly(t([(26, 8), (58, 8), (60, 44), (74, 50), (92, 58), (100, 70), (98, 86), (14, 86), (16, 60), (24, 48)]), round_=2.4)
    c.part(shaft, col, bevel=4.0, spec=0.35)
    c.part(poly(t([(22, 4), (62, 4), (62, 18), (22, 18)]), round_=1.6), lighten(col, 0.35), bevel=2.0, spec=0.4)
    c.part(poly(t([(12, 82), (100, 82), (102, 94), (10, 94)]), round_=1.6), sole, bevel=2.2, spec=0.2)
    c.flat(line(t([(62, 52), (90, 62)]), 2.2, False), lighten(col, 0.5), 0.6)
    c.flat(line(t([(14, 84), (100, 84)]), 1.6, False), (240, 240, 240), 0.8)


def tri_ring(cx, cy, r, w, rot=-90):
    pts = [(cx + r * math.cos(math.radians(rot + i * 120)), cy + r * math.sin(math.radians(rot + i * 120))) for i in range(3)]
    pin = [(cx + (r - w * 1.9) * math.cos(math.radians(rot + i * 120)), cy + (r - w * 1.9) * math.sin(math.radians(rot + i * 120))) for i in range(3)]
    return np.clip(poly(pts, round_=0.4) - poly(pin, round_=0.4), 0, 1)


def hexagram(c, cx, cy, r, w, col):
    c.part(np.maximum(tri_ring(cx, cy, r, w, -90), tri_ring(cx, cy, r, w, 90)), col, bevel=2.0, spec=0.5)

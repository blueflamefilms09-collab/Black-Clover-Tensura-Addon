"""Animated Ultimate skill icon: Jibril, Lord of Rigor / Law ("dawn" medallion).

Layers (every motion is a whole number of cycles per 3.2 s loop):
  1. azure -> lavender sky, a rising dawn glow low in the disc (breathes, 1 cycle), drifting rose cloud bands (1-2 cycles)
  2. two layered feather wings, 3 rows of feathers, softly flexing / breathing in sync (1 cycle, row phase delay)
  3. golden scales of law at the centre: beam tilts and settles (2 swings, eased in/out), soft core glow (1 cycle)
  4. nested double halo above, beads counter-rotating (+2 / -2 steps), tiny feathers orbiting it (1 orbit)
  5. petals + feathers falling with staggered phases (1 fall each, sway + flutter)
  6. iridescent rim cycling pink / gold / aqua / violet (the colours travel 1 full step per loop), twinkling gems, slow ticks
  7. a soft sunrise light sweep (1 pass) and a few twinkling sparkles
Hero frame 0: wings fully open, halo brightest.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(__file__))
from icon_anim_lib import *  # noqa: E402,F401

TAU = 2 * math.pi
Z = 2                                   # shape layers are painted at 2x and downsampled for anti-aliasing
CX = CY = 127.5
R_OUT = 120.0                           # medallion radius (px at S = 256)

PEARL = (255, 250, 246)
LAV_SH = (196, 190, 238)
EDGE = (112, 100, 188)
ROSE = (255, 186, 190)
ROSEGOLD = (240, 170, 150)
AMBER = (255, 212, 100)
GOLD_HI = (255, 246, 205)
GOLD_SH = (176, 100, 76)
PLUM = (84, 44, 78)


def mix(a, b, f):
    f = max(0.0, min(1.0, f))
    return tuple(int(round(a[i] + (b[i] - a[i]) * f)) for i in range(len(a)))


def smooth(x):
    x = np.clip(x, 0, 1)
    return x * x * (3 - 2 * x)


def ease(x):
    return 0.5 - 0.5 * math.cos(math.pi * max(0.0, min(1.0, x)))


# ------------------------------------------------------------------------------------------------ background (numpy)

YY, XX = np.mgrid[0:256, 0:256].astype(np.float64)
DX, DY = XX - CX, YY - CY
RAD = np.hypot(DX, DY)
ANG = np.arctan2(DY, DX)


def sky(t):
    v = (YY / 255.0)
    top = np.array((40, 96, 214), float)       # azure
    mid = np.array((92, 116, 214), float)      # periwinkle
    low = np.array((168, 140, 222), float)      # lavender
    c = np.where(v[..., None] < 0.55, top + (mid - top) * smooth(v / 0.55)[..., None],
                 mid + (low - mid) * smooth((v - 0.55) / 0.45)[..., None])
    # soft cloud bands, tileable horizontally (integer x frequencies), drifting one period per loop
    cl = np.zeros((256, 256))
    for fx, fy, ph, amp, k in ((2, 7, 0.3, 1.0, 1), (3, 11, 1.7, 0.7, -1), (5, 5, 4.1, 0.5, 2)):
        cl += amp * np.sin(TAU * (fx * XX / 256.0 + k * t) + fy * YY / 256.0 * TAU * 0.5 + ph)
    cl = smooth((cl / 2.2 + 0.35) * 1.3) * smooth((YY - 90) / 120.0) * (1 - smooth((YY - 215) / 40.0))
    rose = np.array((255, 190, 205), float)
    c = c + (rose - c) * (0.42 * cl)[..., None]
    # vignette toward the edge: indigo-azure darkening
    vg = smooth((RAD - 56.0) / 70.0)
    c = c + (np.array((34, 44, 120), float) - c) * (0.62 * vg)[..., None]
    # dawn glow low in the disc, breathing once per loop
    br = 0.88 + 0.12 * math.cos(TAU * t)
    gy = 226.0
    d = np.hypot(DX * 0.8, YY - gy)
    g1 = np.exp(-(d / (70.0 * br)) ** 2)
    g2 = np.exp(-(d / (36.0 * br)) ** 2)
    c = c + (np.array((255, 168, 160), float) - c) * (0.85 * g1)[..., None]
    c = c + (np.array((255, 218, 140), float) - c) * (0.85 * g2)[..., None]
    # sun disc peeking above the horizon glow
    sd = np.hypot(DX, YY - 224.0)
    sun = smooth((30.0 - sd) / 3.0)
    sc = np.clip(1 - sd / 30.0, 0, 1)[..., None]
    suncol = np.array((255, 190, 150), float) * (1 - sc) + np.array((255, 244, 200), float) * sc
    c = c + (suncol - c) * (0.92 * sun)[..., None]
    # soft rays fanning up from the sun (slow drift: one ray step per loop)
    ra = np.arctan2(YY - 224.0, DX)
    rays = (0.5 + 0.5 * np.cos(11.0 * ra + TAU * t)) ** 2 * np.exp(-np.hypot(DX, YY - 224.0) / 170.0) * smooth((224.0 - YY) / 30.0)
    c = c + (np.array((255, 214, 190), float) - c) * (0.34 * rays)[..., None]
    return c


def additive(img, arr, strength=1.0):
    a = np.array(img).astype(np.float64)
    a[..., :3] = np.clip(a[..., :3] + arr * strength, 0, 255)
    return Image.fromarray(a.astype(np.uint8), "RGBA")


def radial(cx, cy, r, color, power=2.0):
    d = np.hypot(XX - cx, YY - cy) / r
    f = np.exp(-d ** power)
    return f[..., None] * np.array(color, float)[None, None, :]


# ------------------------------------------------------------------------------------------------ shape layers (2x)

def P(v):
    return v * Z


def new_layer():
    im = Image.new("RGBA", (256 * Z, 256 * Z), (0, 0, 0, 0))
    return im, ImageDraw.Draw(im)


def down(im):
    return im.resize((256, 256), Image.LANCZOS)


def outline(im, px, color, alpha=255):
    a = im.split()[3]
    k = int(px * Z) * 2 + 1
    dil = a.filter(ImageFilter.MaxFilter(k))
    base = Image.new("RGBA", im.size, color + (0,))
    base.putalpha(dil.point(lambda v: min(255, int(v * alpha / 255))))
    base.alpha_composite(im)
    return base


FEATHER_STOPS = ((0.0, LAV_SH), (0.30, (238, 236, 252)), (0.62, PEARL), (0.86, (255, 232, 226)), (1.0, ROSE))


def feather_col(u):
    for i in range(len(FEATHER_STOPS) - 1):
        u0, c0 = FEATHER_STOPS[i]
        u1, c1 = FEATHER_STOPS[i + 1]
        if u <= u1:
            return mix(c0, c1, (u - u0) / (u1 - u0))
    return FEATHER_STOPS[-1][1]


def feather(d, bx, by, dx, dy, L, W, bend, bright=1.0, tint=0.0, light=(-0.55, -0.83), pal=None, hi=0.55, barbs=True):
    """One feather: leaf shape from (bx, by) along unit (dx, dy); layered outline / body / highlight / shaft."""
    px, py = -dy, dx
    NS = 9

    def pts(wf, shift):
        left, right = [], []
        for j in range(NS + 1):
            u = j / NS
            hw = (W / 2.0) * wf * (math.sin(math.pi * (u ** 0.72)) ** 0.5)
            cxp = bx + dx * L * u + px * bend * L * u * u + px * shift * (W / 2.0) * (1 - u * 0.5)
            cyp = by + dy * L * u + py * bend * L * u * u + py * shift * (W / 2.0) * (1 - u * 0.5)
            left.append((P(cxp + px * hw), P(cyp + py * hw)))
            right.append((P(cxp - px * hw), P(cyp - py * hw)))
        return left, right

    # outline (slightly fatter, deep lavender) separates overlapping feathers
    l, r = pts(1.0, 0.0)
    ring = l + r[::-1]
    pal = pal or ROW_PAL[2]
    edge = pal[3]
    d.polygon(ring, fill=edge + (255,))
    d.line(ring + [ring[0]], fill=edge + (255,), width=int(1.6 * Z), joint="curve")
    # body, gradient along the length
    l, r = pts(0.9, 0.0)
    for j in range(NS):
        u = (j + 0.5) / NS
        c = mix(pal[0], pal[1], u / 0.45) if u < 0.45 else mix(pal[1], pal[2], (u - 0.45) / 0.55)
        c = mix(c, (255, 190, 140), tint * float(smooth(np.array((u - 0.5) / 0.5))) * 0.5)
        d.polygon([l[j], l[j + 1], r[j + 1], r[j]], fill=c + (255,))
    # highlight toward the light side
    side = 1.0 if (px * light[0] + py * light[1]) > 0 else -1.0
    l, r = pts(0.45, side * 0.38)
    for j in range(1, NS - 1):
        u = (j + 0.5) / NS
        c = mix(mix(pal[0], pal[1], 0.6 + 0.4 * u), (255, 255, 255), hi)
        if hi > 0.01:
            d.polygon([l[j], l[j + 1], r[j + 1], r[j]], fill=c + (255,))
    # barbs: thin diagonal strokes from the shaft toward the tip side
    for j in range(2, NS if barbs else 0):
        u = j / NS
        hw = (W / 2.0) * 0.8 * (math.sin(math.pi * (u ** 0.72)) ** 0.5)
        mx = bx + dx * L * u + px * bend * L * u * u
        my = by + dy * L * u + py * bend * L * u * u
        for sg in (-1, 1):
            ex = mx + dx * hw * 0.9 + px * sg * hw
            ey = my + dy * hw * 0.9 + py * sg * hw
            d.line([(P(mx), P(my)), (P(ex), P(ey))], fill=mix(pal[0], pal[3], 0.5) + (120,), width=int(0.8 * Z))
    # shaft
    d.line([(P(bx + dx * L * 0.04), P(by + dy * L * 0.04)),
            (P(bx + dx * L * 0.86 + px * bend * L * 0.74), P(by + dy * L * 0.86 + py * bend * L * 0.74))],
           fill=mix(ROSEGOLD, (255, 255, 255), 0.35) + (230,), width=int(1.1 * Z))


ROW_PAL = (
    ((150, 150, 222), (196, 196, 240), (236, 226, 250), (66, 66, 156)),      # back row: cool lavender-blue, strong edge
    ((214, 210, 244), (246, 240, 252), (255, 218, 222), (84, 74, 170)),     # middle row: pearl lavender -> rose tip
    ((250, 246, 250), (255, 253, 252), (255, 214, 196), (122, 104, 192)),    # front row: bright pearl -> rose-gold tip
)

# rows: (angles deg up from horizontal-outward, lengths, width, bend, brightness, phase)
ROWS = (
    ((-14, 2, 18, 34, 50, 66, 80), (86, 92, 90, 82, 72, 60, 48), 26, 0.12, 0.90, 0.00),
    ((-6, 10, 26, 42, 58, 74), (68, 72, 70, 62, 52, 40), 23, 0.10, 0.96, 0.10),
    ((2, 20, 38, 56, 74), (48, 50, 46, 40, 32), 19, 0.08, 1.00, 0.20),
)


def wings(t):
    im, d = new_layer()
    for ri, row in enumerate(ROWS):
        angs, lens, W, bend, bright, ph = row
        flex = math.cos(TAU * (t - ph * 0.5))                 # +1 = fully open (frame 0)
        spread = 0.80 + 0.20 * (0.5 + 0.5 * flex)
        lift = 7.0 * flex
        for sgn in (-1, 1):
            for a, L in zip(angs, lens):
                a2 = 30.0 + (a - 30.0) * spread + lift
                ar = math.radians(a2)
                dx, dy = sgn * math.cos(ar), -math.sin(ar)
                bx = CX + sgn * 12.0 + dx * 5.0
                by = 142.0 + dy * 5.0
                tint = 0.9 if a < 12 else 0.35
                feather(d, bx, by, dx, dy, L * (0.97 + 0.03 * (0.5 + 0.5 * flex)), W, bend * sgn * -sgn * 1.0,
                        bright=bright, tint=tint, pal=ROW_PAL[ri], hi=(0.0, 0.0, 0.35)[ri], barbs=False)
    return down(im)


def gold_poly(d, pts, hi=GOLD_HI, mid=AMBER, lo=GOLD_SH):
    d.polygon([(P(x), P(y)) for x, y in pts], fill=mid + (255,))


def scales(t):
    u = t
    env = math.sin(math.pi * u) ** 2
    phi = math.radians(9.0) * env * math.sin(TAU * 2 * u)          # starts level, tilts, settles level again
    im, d = new_layer()
    bx0, by0 = 128.0, 122.0
    # plinth + post
    for (x0, y0, x1, y1, c) in ((106, 186, 150, 192, GOLD_SH), (110, 181, 146, 187, mix(GOLD_SH, AMBER, 0.55)),
                                (116, 176, 140, 182, AMBER)):
        d.rounded_rectangle((P(x0), P(y0), P(x1), P(y1)), radius=P(2.5), fill=c + (255,))
    d.rectangle((P(106), P(186), P(150), P(187.4)), fill=GOLD_SH + (255,))
    d.polygon([(P(123.6), P(124)), (P(132.4), P(124)), (P(133.6), P(178)), (P(122.4), P(178))], fill=GOLD_SH + (255,))
    d.polygon([(P(124.6), P(124)), (P(130.6), P(124)), (P(131.4), P(178)), (P(124.0), P(178))], fill=AMBER + (255,))
    d.polygon([(P(125.6), P(124)), (P(128.4), P(124)), (P(128.8), P(178)), (P(125.4), P(178))], fill=GOLD_HI + (255,))
    # pans hang straight down from the beam ends
    cp, sp = math.cos(phi), math.sin(phi)
    ends = []
    for sgn in (-1, 1):
        ex, ey = bx0 + sgn * 42.0 * cp, by0 + sgn * 42.0 * sp
        ends.append((ex, ey))
        py = ey + 33.0
        for off in (-15.0, 15.0, 0.0):
            d.line([(P(ex), P(ey + 2)), (P(ex + off), P(py))], fill=mix(AMBER, GOLD_HI, 0.35) + (255,),
                   width=int((1.9 if off else 1.3) * Z))
        bowl = [(P(ex + 17 * math.cos(a)), P(py + 10 * math.sin(a))) for a in np.linspace(0, math.pi, 22)]
        d.polygon(bowl, fill=GOLD_SH + (255,))
        bowl2 = [(P(ex + 14.6 * math.cos(a) - 0.7), P(py + 0.6 + 6.6 * math.sin(a))) for a in np.linspace(0.0, math.pi, 22)]
        d.polygon(bowl2, fill=mix(AMBER, GOLD_SH, 0.25) + (255,))
        bowl3 = [(P(ex + 10 * math.cos(a) - 3.0), P(py + 1.0 + 4.0 * math.sin(a))) for a in np.linspace(0.5, 2.4, 14)]
        d.polygon(bowl3, fill=mix(AMBER, GOLD_HI, 0.55) + (255,))
        d.ellipse((P(ex - 17), P(py - 3.2), P(ex + 17), P(py + 3.0)), fill=GOLD_HI + (255,))
        d.ellipse((P(ex - 13.8), P(py - 1.7), P(ex + 13.8), P(py + 2.2)), fill=mix(GOLD_SH, PLUM, 0.5) + (255,))
        d.ellipse((P(ex - 13.8), P(py - 1.7), P(ex + 13.8), P(py + 0.9)), fill=mix(ROSE, PEARL, 0.4) + (255,))
    # beam (tapered), then ends
    def pt(s, h):
        return (P(bx0 + s * cp - h * sp), P(by0 + s * sp + h * cp))
    d.polygon([pt(-43, -2.4), pt(-8, -5.0), pt(8, -5.0), pt(43, -2.4), pt(43, 2.4), pt(8, 5.0), pt(-8, 5.0), pt(-43, 2.4)],
              fill=GOLD_SH + (255,))
    d.polygon([pt(-42, -1.6), pt(-8, -4.2), pt(8, -4.2), pt(42, -1.6), pt(42, 0.8), pt(8, 2.2), pt(-8, 2.2), pt(-42, 0.8)],
              fill=AMBER + (255,))
    d.polygon([pt(-39, -1.4), pt(-6, -3.8), pt(6, -3.8), pt(39, -1.4), pt(39, -0.4), pt(0, -1.8), pt(-39, -0.4)],
              fill=GOLD_HI + (255,))
    for ex, ey in ends:
        d.ellipse((P(ex - 4.4), P(ey - 4.4), P(ex + 4.4), P(ey + 4.4)), fill=AMBER + (255,))
        d.ellipse((P(ex - 1.8), P(ey - 2.4), P(ex + 0.6), P(ey - 0.4)), fill=GOLD_HI + (255,))
    # pivot gem + finial
    d.ellipse((P(bx0 - 6.5), P(by0 - 6.5), P(bx0 + 6.5), P(by0 + 6.5)), fill=GOLD_SH + (255,))
    d.ellipse((P(bx0 - 5.4), P(by0 - 5.4), P(bx0 + 5.4), P(by0 + 5.4)), fill=AMBER + (255,))
    d.ellipse((P(bx0 - 3.6), P(by0 - 3.6), P(bx0 + 3.6), P(by0 + 3.6)), fill=mix(ROSE, (255, 120, 150), 0.4) + (255,))
    d.ellipse((P(bx0 - 2.6), P(by0 - 3.2), P(bx0 + 0.2), P(by0 - 0.6)), fill=(255, 255, 255, 255))
    d.polygon([(P(128), P(106.5)), (P(131), P(113)), (P(128), P(118)), (P(125), P(113))], fill=GOLD_HI + (255,))
    d.polygon([(P(128), P(106.5)), (P(131), P(113)), (P(128), P(118))], fill=AMBER + (255,))
    im = outline(down_keep(im), 2.2, PLUM)
    return down(im)


def down_keep(im):
    return im


# ------------------------------------------------------------------------------------------------ halo

HALO_C = (128.0, 60.0)


def halo(t):
    bright = 0.86 + 0.14 * math.cos(TAU * t)            # brightest at frame 0
    nod = 1.0 + 0.07 * math.sin(TAU * t)
    im, d = new_layer()
    back, front = [], []

    def ring(rx, ry, w, step_beads, n_beads, shift_deg, ccol):
        n = 90
        for j in range(n):
            a0, a1 = TAU * j / n, TAU * (j + 1.02) / n
            am = 0.5 * (a0 + a1)
            fr = 0.5 + 0.5 * math.sin(am)                # lower half of the ellipse = near side
            col = mix(mix(GOLD_SH, ccol, 0.55), mix(ccol, (255, 255, 255), 0.5 * bright), fr)
            col = mix(col, ROSE, 0.18 * (1 - fr))
            p0 = (P(HALO_C[0] + rx * math.cos(a0)), P(HALO_C[1] + ry * nod * math.sin(a0)))
            p1 = (P(HALO_C[0] + rx * math.cos(a1)), P(HALO_C[1] + ry * nod * math.sin(a1)))
            d.line([p0, p1], fill=col + (255,), width=int(P(w * (0.7 + 0.5 * fr))))
        for b in range(n_beads):
            a = TAU * b / n_beads + math.radians(shift_deg)
            bx = HALO_C[0] + rx * math.cos(a)
            by = HALO_C[1] + ry * nod * math.sin(a)
            fr = 0.5 + 0.5 * math.sin(a)
            s = (1.7 + 1.5 * fr) * (1.0 + 0.0)
            c = mix(ccol, (255, 255, 255), 0.7)
            d.polygon([(P(bx), P(by - s * 1.5)), (P(bx + s), P(by)), (P(bx), P(by + s * 1.5)), (P(bx - s), P(by))],
                      fill=c + (255,))

    ring(50.0, 15.0, 5.4, 0, 12, 30.0 * 2 * t, mix(AMBER, GOLD_HI, 0.45))          # +60 deg per loop (2 of 12 steps)
    ring(38.0, 11.2, 4.2, 0, 8, -45.0 * 2 * t + 22.5, mix(ROSE, GOLD_HI, 0.55))     # -90 deg per loop (2 of 8 steps)
    # tiny feathers orbiting the outer halo, one orbit per loop, counter to the outer beads
    items = []
    for k in range(3):
        a = -TAU * t + TAU * k / 3.0 + 0.6
        items.append((math.sin(a), a))
    items.sort()
    for sn, a in items:
        fx = HALO_C[0] + 60.0 * math.cos(a)
        fy = HALO_C[1] + 17.0 * nod * math.sin(a)
        tx, ty = -math.sin(a), 17.0 / 60.0 * math.cos(a)
        n = math.hypot(tx, ty)
        tx, ty = -tx / n, -ty / n                              # travel direction (a decreases)
        s = 0.75 + 0.3 * (0.5 + 0.5 * sn)
        feather(d, fx - tx * 5.5 * s, fy - ty * 5.5 * s, tx, ty, 11.0 * s, 4.6 * s, 0.12, bright=1.0, tint=0.5)
    return down(im)


# ------------------------------------------------------------------------------------------------ particles

PETALS = []
_rng = np.random.default_rng(7)
for i in range(11):
    PETALS.append(dict(x=float(_rng.uniform(34, 222)), off=float((i / 11.0 + _rng.uniform(-0.03, 0.03)) % 1.0),
                       sw=float(_rng.uniform(5, 12)), swk=int(_rng.integers(1, 3)), swp=float(_rng.uniform(0, TAU)),
                       spin=int(_rng.integers(1, 3)) * (1 if i % 2 else -1), sp=float(_rng.uniform(0, TAU)),
                       kind=i % 3, size=float(_rng.uniform(5.0, 7.5))))


def particles(t):
    im, d = new_layer()
    for p in PETALS:
        ph = (t + p["off"]) % 1.0
        y = -12.0 + 272.0 * ph
        x = p["x"] + p["sw"] * math.sin(TAU * p["swk"] * t + p["swp"])
        alpha = math.sin(math.pi * ph) ** 0.6
        if alpha < 0.03:
            continue
        ang = TAU * p["spin"] * t + p["sp"]
        flip = 0.30 + 0.70 * abs(math.cos(TAU * 2 * t + p["sp"]))
        s = p["size"]
        ca, sa = math.cos(ang), math.sin(ang)
        if p["kind"] == 2:                                      # pearl feather
            col, col2 = PEARL, (225, 220, 250)
            shape = [(-1.0, 0.0), (-0.2, 0.34), (0.55, 0.26), (1.15, 0.0), (0.55, -0.26), (-0.2, -0.34)]
            sc = s * 1.5
        else:                                                   # rose petal
            col = mix(ROSE, (255, 140, 175), 0.35 + 0.4 * (p["kind"]))
            col2 = mix(col, (255, 245, 240), 0.6)
            shape = [(-0.9, 0.0), (-0.4, 0.55), (0.35, 0.62), (0.95, 0.0), (0.35, -0.62), (-0.4, -0.55)]
            sc = s
        for scl, c in ((1.0, col), (0.55, col2)):
            poly = []
            for ux, uy in shape:
                lx, ly = ux * sc * scl, uy * sc * scl * flip
                poly.append((P(x + lx * ca - ly * sa), P(y + lx * sa + ly * ca)))
            d.polygon(poly, fill=c + (int(255 * alpha),))
    im = down(im)
    # keep inside the medallion face
    m = np.clip((104.0 - RAD) / 6.0, 0, 1)
    a = np.array(im).astype(np.float64)
    a[..., 3] *= m
    return Image.fromarray(a.astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------ rim, sparkles, sweep

RIM_STOPS = np.array(((255, 150, 200), (255, 214, 112), (108, 232, 236), (172, 132, 248)), float)


def rim(t):
    p = (2.0 * ANG / TAU + t) % 1.0                              # 2 repeats around, one palette step per loop
    q = p * 4.0
    i = np.floor(q).astype(int) % 4
    f = smooth(q - np.floor(q))
    col = RIM_STOPS[i] * (1 - f)[..., None] + RIM_STOPS[(i + 1) % 4] * f[..., None]
    r0, r1 = 109.0, R_OUT
    n = np.clip((RAD - (r0 + r1) / 2) / ((r1 - r0) / 2), -1, 1)    # -1 inner edge .. +1 outer edge
    la = math.radians(-135.0)
    lit = 0.88 + 0.30 * n * np.cos(ANG - la) - 0.10 * n * n
    sheen = np.exp(-((n + 0.15) / 0.28) ** 2) * 0.28
    c = col * lit[..., None] + 255 * sheen[..., None]
    # engraved edges
    outer = smooth((RAD - (r1 - 2.2)) / 2.2)
    inner = np.exp(-((RAD - r0) / 1.3) ** 2)
    c = c + (np.array((52, 40, 110), float) - c) * (0.75 * outer)[..., None]
    c = c + (np.array((70, 52, 130), float) - c) * (0.55 * inner)[..., None]
    mask = smooth((RAD - r0 + 1.0) / 1.2) * smooth((r1 + 0.8 - RAD) / 1.2)
    img = np.concatenate([np.clip(c, 0, 255), (mask * 255)[..., None]], axis=-1)
    im = Image.fromarray(img.astype(np.uint8), "RGBA")
    # ticks (slowly rotating one step per loop), and four gems
    big, d = new_layer()
    n_t = 48
    for k in range(n_t):
        a = TAU * (k + t) / n_t - math.pi / 2
        major = (k % 6 == 0)
        r_a, r_b = (R_OUT * 0.0 + 99.0, 106.5) if major else (102.5, 106.5)
        c0 = mix(GOLD_HI, ROSE, 0.2) if major else mix(AMBER, (255, 255, 255), 0.25)
        d.line([(P(CX + r_a * math.cos(a)), P(CY + r_a * math.sin(a))), (P(CX + r_b * math.cos(a)), P(CY + r_b * math.sin(a)))],
               fill=c0 + (235 if major else 150,), width=int((2.2 if major else 1.2) * Z))
    d.ellipse((P(CX - 107.6), P(CY - 107.6), P(CX + 107.6), P(CY + 107.6)), outline=mix(AMBER, GOLD_HI, 0.3) + (120,), width=int(1.0 * Z))
    gems = ((0, 0, ROSE), (1, 1, (150, 235, 240)), (2, 0, ROSE), (3, 1, (150, 235, 240)))
    for gi, alt, gc in gems:
        a = gi * math.pi / 2 - math.pi / 2
        gx, gy = CX + 114.5 * math.cos(a), CY + 114.5 * math.sin(a)
        tw = 0.5 + 0.5 * math.cos(TAU * (t + gi * 0.25))
        s = 5.4
        d.polygon([(P(gx), P(gy - s)), (P(gx + s * 0.75), P(gy)), (P(gx), P(gy + s)), (P(gx - s * 0.75), P(gy))], fill=(70, 46, 120, 255))
        s2 = 4.0
        gcol = mix(gc, (255, 255, 255), 0.25 + 0.45 * tw)
        d.polygon([(P(gx), P(gy - s2)), (P(gx + s2 * 0.75), P(gy)), (P(gx), P(gy + s2)), (P(gx - s2 * 0.75), P(gy))], fill=gcol + (255,))
        d.polygon([(P(gx), P(gy - s2)), (P(gx + s2 * 0.75), P(gy)), (P(gx), P(gy - 0.2))], fill=(255, 255, 255, 255))
    im.alpha_composite(down(big))
    return im


def sparkles(t):
    big, d = new_layer()
    for (x, y, k, off, sz) in ((88, 112, 1, 0.00, 5.0), (170, 104, 1, 0.33, 4.2), (104, 178, 2, 0.15, 3.6),
                               (158, 172, 1, 0.66, 4.6), (128, 30, 1, 0.50, 3.8), (60, 70, 2, 0.40, 3.0), (198, 78, 2, 0.80, 3.0)):
        a = math.sin(math.pi * ((t * k + off) % 1.0)) ** 3
        if a < 0.02:
            continue
        for ang, ln, w in ((0, sz * 1.9, 0.5), (math.pi / 2, sz * 1.9, 0.5), (math.pi / 4, sz * 0.9, 0.3), (-math.pi / 4, sz * 0.9, 0.3)):
            dx, dy = math.cos(ang) * ln * a, math.sin(ang) * ln * a
            ww = max(0.4, sz * w * a)
            nx, ny = -math.sin(ang) * ww, math.cos(ang) * ww
            d.polygon([(P(x - dx), P(y - dy)), (P(x + nx), P(y + ny)), (P(x + dx), P(y + dy)), (P(x - nx), P(y - ny))],
                      fill=(255, 252, 240, int(255 * a)))
    return down(big)


def sweep(t):
    c = -0.80 + 1.60 * t
    ds = ((DX + DY) * 0.7071) / 256.0
    band = np.exp(-((ds - c) / 0.075) ** 2) * 0.55 + np.exp(-((ds - c + 0.13) / 0.03) ** 2) * 0.25
    col = np.array((255, 232, 210), float)
    return band[..., None] * col[None, None, :] * 0.42


# ------------------------------------------------------------------------------------------------ the icon

def draw(t, S):
    assert S == 256
    img = Image.fromarray(np.concatenate([sky(t), np.full((256, 256, 1), 255.0)], axis=-1).astype(np.uint8), "RGBA")
    # soft rose-lavender glow behind the wings
    w = wings(t)
    sh = Image.new('RGBA', (256, 256), (28, 36, 120, 0))
    sh.putalpha(w.split()[3].filter(ImageFilter.GaussianBlur(7)).point(lambda v: int(v * 0.55)))
    img.alpha_composite(sh)
    img.alpha_composite(glow(w, 4, 0.35))
    # bright core behind the scales (breathes once per loop)
    core = 0.80 + 0.20 * math.cos(TAU * t)
    img = additive(img, radial(128, 140, 34, (255, 214, 150), 2.0), 0.55 * core)
    img = additive(img, radial(128, 140, 14, (255, 240, 220), 2.0), 0.30 * core)
    img.alpha_composite(glow(scales(t), 4, 0.5))
    hb = glow(halo(t), 6, 0.9)
    img.alpha_composite(hb)
    img = additive(img, radial(128, 64, 52, (255, 214, 170), 2.0), 0.20 * (0.86 + 0.14 * math.cos(TAU * t)))
    img.alpha_composite(particles(t))
    img.alpha_composite(sparkles(t))
    img = additive(img, sweep(t) * smooth((106.0 - RAD) / 8.0)[..., None], 1.0)
    img.alpha_composite(rim(t))
    # medallion alpha
    m = np.clip((R_OUT + 0.7 - RAD) / 1.3, 0, 1)
    a = np.array(img).astype(np.float64)
    a[..., 3] = a[..., 3] * m
    return Image.fromarray(a.astype(np.uint8), "RGBA")


if __name__ == "__main__":
    build("jibril", draw, hero_t=0.0)

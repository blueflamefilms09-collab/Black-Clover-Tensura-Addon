"""The base frame and the ornaments ("motifs") of the boss bar styles. A motif draws on three layers of one atlas:
back (under the fill: the dark track, the name banner, the socket disc), front (the frame, opaque art over the fill) and glow
(white shapes with alpha; the game tints it with the style's glow colour and adds it). All coordinates are GUI units, the widget is 256x42."""
import math
import random

import numpy as np

from lib import (Canvas, W, H, WIN, SOCK, PLATE, mix, shade, spikes, leaf, gear, drop, star, bolt, flame, gem, skull, horn, rune)

WHITE = (255, 255, 255)
BX0, BX1, BY0, BY1 = 34, 248, 22, 38           # the bezel round the fill window (right end is a pointed cap)


class Ctx:
    def __init__(self, pal, seed):
        self.pal = pal
        self.seed = seed
        self.rnd = random.Random(seed)
        self.back, self.front, self.glow = Canvas(), Canvas(), Canvas()


# ---------------------------------------------------------------- the base: track, banner, bezel, plate rails, socket
def base(x):
    p, b, f, g = x.pal, x.back, x.front, x.glow
    dk, md, lt, hi = p["dk"], p["md"], p["lt"], p["hi"]
    wx, wy, ww, wh = WIN
    cx, cy, r = SOCK
    px, py, pw, ph = PLATE

    # ---- BACK: window track, banner, socket disc
    b.grad_rect(wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, shade(dk, 0.5), shade(mix(dk, md, 0.35), 0.8))
    b.rect(wx, wy, wx + ww, wy + 1, shade(dk, 0.3))
    ban = [(px - 3, py + 2), (px + 3, py), (px + pw - 3, py), (px + pw + 3, py + 2), (px + pw + 3, py + ph - 2), (px + pw - 3, py + ph), (px + 3, py + ph), (px - 3, py + ph - 2)]
    b.grad_poly(ban, tuple(mix(dk, md, 0.25)), tuple(shade(dk, 0.55)))
    b.line([(px + 2, py + ph - 1.5), (px + pw - 2, py + ph - 1.5)], mix(md, lt, 0.2), 0.3)
    b.grad_ell(cx, cy, r + 1, r + 1, shade(dk, 0.7), shade(mix(dk, md, 0.4), 0.55))
    b.ell(cx, cy, r - 3, r - 3, shade(dk, 0.45))

    # ---- FRONT: bezel with a chamfered cap on the right, the window cut out
    bez = [(BX0, BY0 + 1), (BX0 + 2, BY0), (BX1 - 12, BY0), (BX1, 30), (BX1 - 12, BY1), (BX0 + 2, BY1), (BX0, BY1 - 1)]
    f.grad_poly(bez, lt, md)
    f.line(bez + [bez[0]], dk, 0.7)
    f.line([(BX0 + 2, BY0 + 1.1), (BX1 - 13, BY0 + 1.1)], hi, 0.35)
    f.line([(BX0 + 2, BY1 - 1.1), (BX1 - 13, BY1 - 1.1)], shade(dk, 1.0), 0.35)
    f.erase_rect(wx - 1.2, wy - 1.2, wx + ww + 1.2, wy + wh + 1.2)
    f.line([(wx - 1.2, wy - 1.2), (wx + ww + 1.2, wy - 1.2), (wx + ww + 1.2, wy + wh + 1.2), (wx - 1.2, wy + wh + 1.2), (wx - 1.2, wy - 1.2)], dk, 0.7)
    f.line([(wx - 0.6, wy + wh + 0.7), (wx + ww + 0.6, wy + wh + 0.7)], mix(lt, hi, 0.5), 0.35)
    f.line([(wx - 0.6, wy - 0.7), (wx + ww + 0.6, wy - 0.7)], shade(dk, 0.7), 0.5)
    # rivets
    for rx in (BX0 + 7, 90, 142, 194, BX1 - 22):
        for ry in (BY0 + 1.8, BY1 - 1.8):
            f.ell(rx, ry, 0.9, 0.9, shade(md, 0.7))
            f.ell(rx - 0.2, ry - 0.25, 0.4, 0.4, hi)

    # ---- plate rails (the banner shows between them)
    for y0, y1 in ((py - 1.5, py + 0.6), (py + ph - 0.6, py + ph + 1.6)):
        rail = [(px - 5, y0 + 0.6), (px - 3, y0), (px + pw + 3, y0), (px + pw + 5, y0 + 0.6), (px + pw + 5, y1 - 0.6), (px + pw + 3, y1), (px - 3, y1), (px - 5, y1 - 0.6)]
        f.grad_poly(rail, lt, md)
        f.line(rail + [rail[0]], dk, 0.5)
    for ex in (px - 5, px + pw + 5):
        f.poly([(ex - 1.2, py - 1), (ex + 1.2, py - 1), (ex + 1.2, py + ph + 1.5), (ex - 1.2, py + ph + 1.5)], md, dk, 0.4)
    # little posts between plate and bezel
    for qx in (px + 14, px + pw / 2, px + pw - 14):
        f.poly([(qx - 3, py + ph + 1.4), (qx + 3, py + ph + 1.4), (qx + 2, BY0 + 0.6), (qx - 2, BY0 + 0.6)], md, dk, 0.4)
    f.ell(px + pw / 2, py + ph + 1.5, 1.6, 1.6, p["ac"], dk, 0.4)

    # ---- socket ring and the flange that joins it to the bezel
    f.poly([(cx + r - 2, cy - 7), (BX0 + 2, BY0 + 1), (BX0 + 2, BY1 - 1), (cx + r - 2, cy + 7)], md, dk, 0.5)
    f.grad_ell(cx, cy, r + 2.6, r + 2.6, lt, shade(md, 0.8))
    f.erase_ell(cx, cy, r, r)
    f.d.ellipse([(cx - r - 2.6) * 4, (cy - r - 2.6) * 4, (cx + r + 2.6) * 4, (cy + r + 2.6) * 4], outline=dk + (255,), width=3)
    f.d.ellipse([(cx - r) * 4, (cy - r) * 4, (cx + r) * 4, (cy + r) * 4], outline=dk + (255,), width=3)
    f.d.ellipse([(cx - r - 1.2) * 4, (cy - r - 1.2) * 4, (cx + r + 1.2) * 4, (cy + r + 1.2) * 4], outline=hi + (255,), width=1)
    for ang in (-90, 0, 90, 180):
        gx, gy = cx + math.cos(math.radians(ang)) * (r + 1.3), cy + math.sin(math.radians(ang)) * (r + 1.3)
        if ang == 0:
            gx, gy = cx + r + 1.3, cy
        gem(f, gx, gy, 1.9, p["ac"], mix(p["ac"], WHITE, 0.6), dk)
        g.ell(gx, gy, 1.7, 1.7, (255, 255, 255, 255))
    # glow: the inner edge of the window and the socket
    g.line([(wx, wy + 0.4), (wx + ww, wy + 0.4)], (255, 255, 255, 150), 0.5)
    g.line([(wx, wy + wh - 0.4), (wx + ww, wy + wh - 0.4)], (255, 255, 255, 90), 0.5)
    g.glow(lambda t: t.d.ellipse([(cx - r) * 4, (cy - r) * 4, (cx + r) * 4, (cy + r) * 4], outline=(255, 255, 255, 255), width=3), WHITE, 1.2, 0.55)


def finish(x):
    """After the motifs: keep the window and the socket clear in the front layer, so no ornament covers the fill or the portrait."""
    wx, wy, ww, wh = WIN
    cx, cy, r = SOCK
    x.front.erase_rect(wx - 1.2, wy - 1.2, wx + ww + 1.2, wy + wh + 1.2)
    x.front.erase_ell(cx, cy, r - 0.2, r - 0.2)
    x.glow.erase_rect(wx, wy + 1, wx + ww, wy + wh - 1)


# ---------------------------------------------------------------- helpers
def fan(c, cx, cy, angles, length, width, col, outline=None, jitter=0.0, seed=0):
    rnd = random.Random(seed)
    for a in angles:
        L = length * (1 + (rnd.random() - 0.5) * jitter)
        leaf(c, cx, cy, math.radians(a), L, width, col, outline=outline)


def top_edge(y=BY0 - 0.2):
    return [(BX0 + 4, y), (BX1 - 14, y)]


# ---------------------------------------------------------------- motifs (name -> function(ctx))
def wings(x):
    p, f, g = x.pal, x.front, x.glow
    fan(f, 18, 7, [-170, -150, -130, -110, -90, -70, -50, -30, -10], 9.5, 3.2, p["lt"], p["dk"])
    fan(f, 18, 7, [-160, -140, -120, -100, -80, -60, -40, -20], 6.5, 2.4, p["hi"])
    cx = BX1 - 8
    fan(f, cx, 29, [-80, -62, -44, -26, -8, 10, 28, 46], 17, 4.6, p["lt"], p["dk"])
    fan(f, cx, 29, [-72, -54, -36, -18, 0, 18, 36], 11.5, 3.4, p["hi"], p["md"])
    for a in (-62, -26, 10):
        g.line([(cx, 29), (cx + math.cos(math.radians(a)) * 15, 29 + math.sin(math.radians(a)) * 15)], (255, 255, 255, 120), 0.4)


def laurel(x):
    p, f = x.pal, x.front
    for sgn, x0 in ((1, BX0 + 4), (-1, BX1 - 16)):
        for k in range(9):
            t = k / 8
            xx = x0 + sgn * 0 + (k * 9 if sgn == 1 else -k * 9)
            if not (BX0 + 14 < xx < BX1 - 14):
                continue
            leaf(f, xx, BY0 + 0.2, math.radians(-90 + (-28 if sgn == 1 else 28)), 7.0, 3.0, p["lt"], p["hi"], p["dk"])
            leaf(f, xx, BY1 - 0.2, math.radians(90 + (28 if sgn == 1 else -28)), 7.0, 3.0, p["lt"], p["hi"], p["dk"])
    x.glow.line([(BX0 + 16, BY0 + 0.5), (BX1 - 16, BY0 + 0.5)], (255, 255, 255, 80), 0.4)


def thorns(x):
    p, f = x.pal, x.front
    spikes(f, [(BX0 + 12, BY0 + 0.3), (BX1 - 14, BY0 + 0.3)], 5.5, 6.5, shade(p["md"], 0.8), p["dk"], -1, x.seed, 0.6)
    spikes(f, [(BX0 + 12, BY1 - 0.3), (BX1 - 14, BY1 - 0.3)], 4.5, 7.5, shade(p["md"], 0.7), p["dk"], 1, x.seed + 1, 0.6)
    spikes(f, [(40, 1.2), (236, 1.2)], 2.6, 9, shade(p["md"], 0.9), p["dk"], -1, x.seed + 2, 0.5)
    for ang in (-60, -120, 160, 20):
        a = math.radians(ang)
        f.poly([(18 + math.cos(a) * 17, 24 + math.sin(a) * 17), (18 + math.cos(a) * 24, 24 + math.sin(a) * 24),
                (18 + math.cos(a + 0.25) * 17.5, 24 + math.sin(a + 0.25) * 17.5)], p["md"], p["dk"], 0.4)


def drips(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 11)
    for i in range(14):
        xx = BX0 + 14 + rnd.random() * (BX1 - BX0 - 40)
        ln = 2.5 + rnd.random() * 3.5
        f.line([(xx, BY1 - 0.4), (xx, BY1 + ln)], p["gl"], 0.8)
        drop(f, xx, BY1 + ln + 0.8, 1.3, p["gl"], p["dk"])
        g.ell(xx, BY1 + ln + 0.8, 1.2, 1.2, (255, 255, 255, 140))


def bones(x):
    p, f = x.pal, x.front
    for xx in range(BX0 + 14, BX1 - 16, 12):
        f.poly([(xx - 2.5, BY0 + 0.4), (xx + 2.5, BY0 + 0.4), (xx + 1.4, BY0 - 3.2), (xx - 1.4, BY0 - 3.2)], p["lt"], p["dk"], 0.35)
        f.ell(xx - 1.3, BY0 - 3.4, 1.1, 1.1, p["lt"], p["dk"], 0.3)
        f.ell(xx + 1.3, BY0 - 3.4, 1.1, 1.1, p["lt"], p["dk"], 0.3)
    skull(f, BX1 - 8, 30, 8, p["hi"], p["dk"])
    skull(f, 18, 4, 5, p["hi"], p["dk"])
    for yy in (BY1 + 0.2,):
        f.line([(BX0 + 12, yy), (BX1 - 16, yy)], p["hi"], 0.7)
    x.glow.ell(BX1 - 10.5, 29, 1.4, 1.5, (255, 255, 255, 220))
    x.glow.ell(BX1 - 5.5, 29, 1.4, 1.5, (255, 255, 255, 220))


def flames(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 4)
    for xx in range(BX0 + 12, BX1 - 14, 9):
        h = 5 + rnd.random() * 5
        flame(f, xx, BY0 + 0.6, h, 6.5, shade(p["lt"], 0.9), p["hi"])
        g.poly([(xx - 2, BY0), (xx, BY0 - h), (xx + 2, BY0)], (255, 255, 255, 140))
    for xx in range(BX0 + 16, BX1 - 18, 14):
        flame(f, xx, BY1 + 3.2, 4, 5, shade(p["md"], 1.0), p["lt"])
    for a in (-100, -70):
        flame(f, 18 + math.cos(math.radians(a)) * 16, 24 + math.sin(math.radians(a)) * 16 + 4, 8, 6, p["lt"], p["hi"])
    flame(f, BX1 - 6, 35, 14, 11, p["lt"], p["hi"])


def icicles(x):
    p, f = x.pal, x.front
    rnd = random.Random(x.seed + 6)
    for xx in range(BX0 + 10, BX1 - 14, 7):
        ln = 3 + rnd.random() * 6
        f.poly([(xx - 1.8, BY1 - 0.3), (xx + 1.8, BY1 - 0.3), (xx + rnd.random() - 0.5, BY1 + ln)], p["hi"], p["md"], 0.3)
    spikes(f, [(BX0 + 12, BY0 + 0.3), (BX1 - 14, BY0 + 0.3)], 3.6, 8, p["lt"], p["md"], -1, x.seed, 0.8)
    x.glow.line([(BX0 + 14, BY1 + 0.3), (BX1 - 16, BY1 + 0.3)], (255, 255, 255, 70), 0.4)


def crystals(x):
    p, f, g = x.pal, x.front, x.glow
    for cx, cy, s in ((BX1 - 8, 30, 9), (BX1 - 20, 36, 6), (22, 4, 5), (12, 6, 4)):
        for k, dx in enumerate((-s * 0.45, 0, s * 0.45)):
            h = s * (1.0 - abs(k - 1) * 0.35)
            f.poly([(cx + dx - s * 0.2, cy + s * 0.5), (cx + dx, cy - h), (cx + dx + s * 0.2, cy + s * 0.5)], p["hi"] if k == 1 else p["lt"], p["md"], 0.35)
    g.glow(lambda t: t.ell(BX1 - 8, 30, 4, 6, (255, 255, 255)), WHITE, 2, 0.5)


def bolts(x):
    p, f, g = x.pal, x.front, x.glow
    for i, (x0, x1) in enumerate(((BX1 - 3, BX1 + 6), (BX1 - 12, BX1 + 4), (BX1 - 6, BX1 + 8))):
        bolt(f, x0, 14 + i * 7, x1, 26 + i * 7, p["gl"], 1.0, x.seed + i)
        bolt(g, x0, 14 + i * 7, x1, 26 + i * 7, (255, 255, 255, 255), 1.2, x.seed + i)
    for xx in range(BX0 + 24, BX1 - 20, 26):
        f.poly([(xx - 3, BY0), (xx, BY0 - 4.5), (xx + 3, BY0)], p["gl"], p["dk"], 0.35)
        g.glow(lambda t, xx=xx: t.poly([(xx - 3, BY0), (xx, BY0 - 4.5), (xx + 3, BY0)], (255, 255, 255)), WHITE, 1.2, 0.6)
    bolt(f, 18, 0, 18, 7, p["gl"], 0.9, x.seed + 9, 4, 1.5)


def void_tendrils(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 8)
    for i in range(7):
        sx = BX0 + 20 + i * 28 + rnd.random() * 10
        pts = [(sx, BY0)]
        for k in range(1, 6):
            pts.append((sx + math.sin(k * 1.3 + i) * 3.2, BY0 - k * 1.5))
        f.line(pts, p["md"], 1.5)
        f.line(pts, p["lt"], 0.6)
        g.line(pts, (255, 255, 255, 150), 0.5)
    for i in range(5):
        sx = BX0 + 34 + i * 40
        pts = [(sx, BY1)]
        for k in range(1, 5):
            pts.append((sx + math.sin(k * 1.1 + i) * 3, BY1 + k * 1.1))
        f.line(pts, p["md"], 1.4)


def stars(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 21)
    for _ in range(16):
        sx, sy = BX0 + 14 + rnd.random() * (BX1 - BX0 - 30), (BY0 - 3.5 if rnd.random() < 0.5 else BY1 + 2.5)
        star(f, sx, sy, 1.2 + rnd.random() * 1.2, p["hi"])
        g.ell(sx, sy, 0.9, 0.9, (255, 255, 255, 220))
    star(f, BX1 - 8, 30, 7, p["hi"], 4, 0.25)
    g.glow(lambda t: star(t, BX1 - 8, 30, 7, (255, 255, 255), 4, 0.25), WHITE, 2, 0.8)


def vines(x):
    p, f = x.pal, x.front
    rnd = random.Random(x.seed + 3)
    pts = [(BX0 + 10 + i * 6, BY0 - 0.4 + math.sin(i * 0.8) * 1.6) for i in range(int((BX1 - BX0 - 24) / 6))]
    f.line(pts, p["md"], 1.1)
    for i, (xx, yy) in enumerate(pts):
        if i % 2 == 0:
            leaf(f, xx, yy, math.radians(-90 + (-35 if i % 4 == 0 else 35)), 5 + rnd.random() * 2, 3, p["lt"], p["hi"], p["dk"])
    pts2 = [(BX0 + 14 + i * 6, BY1 + 0.4 + math.sin(i * 0.9 + 1) * 1.4) for i in range(int((BX1 - BX0 - 30) / 6))]
    f.line(pts2, p["md"], 1.0)
    for i, (xx, yy) in enumerate(pts2):
        if i % 3 == 0:
            leaf(f, xx, yy, math.radians(90 + (30 if i % 6 == 0 else -30)), 4.5, 2.6, p["lt"], p["hi"], p["dk"])
    for a in (-120, -80, 150):
        leaf(f, 18 + math.cos(math.radians(a)) * 14, 24 + math.sin(math.radians(a)) * 14, math.radians(a), 8, 3.6, p["lt"], p["hi"], p["dk"])
    for k in range(3):
        f.ell(BX1 - 14 + k * 4, 32 - k * 3, 2.2, 2.2, p["ac"], p["dk"], 0.35)


def waves(x):
    p, f, g = x.pal, x.front, x.glow
    for row, (y, amp, col) in enumerate(((BY0 - 0.5, 1.6, p["lt"]), (BY0 - 2.2, 1.2, p["hi"]))):
        pts = [(xx, y + math.sin(xx * 0.32 + row) * amp) for xx in np.arange(BX0 + 10, BX1 - 14, 1.0)]
        f.line(pts, col, 0.9)
        g.line(pts, (255, 255, 255, 110), 0.5)
    pts = [(xx, BY1 + 0.6 + math.sin(xx * 0.28) * 1.2) for xx in np.arange(BX0 + 12, BX1 - 16, 1.0)]
    f.line(pts, p["lt"], 0.9)
    for k in range(5):
        xx = BX1 - 10 - k * 3
        f.poly([(xx - 2, 36 + k), (xx, 24 - k * 3 + 2), (xx + 2, 36 + k)], p["lt"], p["dk"], 0.3)


def shells(x):
    p, f = x.pal, x.front
    for cx, cy, s in ((BX1 - 8, 30, 8), (BX1 - 22, 37, 4.5), (12, 4, 4)):
        for k in range(-3, 4):
            a = math.radians(-90 + k * 20)
            f.line([(cx, cy + s * 0.4), (cx + math.cos(a) * s, cy + math.sin(a) * s + s * 0.2)], p["lt"] if k % 2 == 0 else p["hi"], max(0.6, s * 0.18))
        f.ell(cx, cy + s * 0.4, s * 0.35, s * 0.25, p["md"], p["dk"], 0.3)


def fangs(x):
    p, f = x.pal, x.front
    for xx in range(BX0 + 16, BX1 - 18, 16):
        f.poly([(xx - 2, BY0 + 0.3), (xx + 2, BY0 + 0.3), (xx, BY0 - 5)], p["hi"], p["dk"], 0.35)
        f.poly([(xx - 2, BY1 - 0.3), (xx + 2, BY1 - 0.3), (xx, BY1 + 4)], p["hi"], p["dk"], 0.35)
    for sgn in (-1, 1):
        f.poly([(18 + sgn * 3, 33), (18 + sgn * 5.5, 33), (18 + sgn * 4.2, 41)], p["hi"], p["dk"], 0.35)


def gears(x):
    p, f, g = x.pal, x.front, x.glow
    for cx, cy, r, t, rot in ((BX1 - 10, 30, 8.5, 12, 0.1), (BX1 - 24, 38.5, 4.8, 9, 0.3), (14, 3, 4.5, 8, 0.2), (BX0 + 30, BY0 - 1, 3.4, 8, 0.0)):
        gear(f, cx, cy, r, t, p["lt"], p["dk"], p["dk"], rot)
        gear(f, cx, cy, r * 0.62, t, p["md"], p["dk"], p["dk"], rot + 0.2)
        g.ell(cx, cy, r * 0.22, r * 0.22, (255, 255, 255, 230))
    for xx in range(BX0 + 40, BX1 - 40, 30):
        f.rect(xx, BY0 - 1.6, xx + 14, BY0 + 0.3, p["md"])
        for k in range(5):
            f.rect(xx + k * 3, BY0 - 2.4, xx + k * 3 + 1.4, BY0 - 1.4, p["lt"])


def shards(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 31)
    for _ in range(11):
        sx = BX0 + 14 + rnd.random() * (BX1 - BX0 - 34)
        top = rnd.random() < 0.6
        base = BY0 if top else BY1
        h = 3 + rnd.random() * 6
        sgn = -1 if top else 1
        pts = [(sx - 2.5, base), (sx + rnd.random() * 2 - 1, base + sgn * h), (sx + 2.2, base)]
        f.poly(pts, p["hi"] if rnd.random() < 0.5 else p["lt"], p["md"], 0.3)
        g.poly(pts, (255, 255, 255, 80))
    for ang in range(-150, 40, 28):
        a = math.radians(ang)
        f.poly([(18 + math.cos(a) * 17, 24 + math.sin(a) * 17), (18 + math.cos(a + 0.12) * 23, 24 + math.sin(a + 0.12) * 23), (18 + math.cos(a + 0.28) * 17, 24 + math.sin(a + 0.28) * 17)], p["hi"], p["md"], 0.3)
    f.poly([(BX1 - 14, 30), (BX1 - 8, 18), (BX1 - 2, 30), (BX1 - 8, 41)], p["hi"], p["md"], 0.4)
    f.line([(BX1 - 14, 30), (BX1 - 2, 30)], p["lt"], 0.4)


def roots(x):
    p, f = x.pal, x.front
    rnd = random.Random(x.seed + 5)
    for _ in range(9):
        sx = BX0 + 16 + rnd.random() * (BX1 - BX0 - 36)
        top = rnd.random() < 0.5
        pts = [(sx, BY0 if top else BY1)]
        sg = -1 if top else 1
        for k in range(1, 6):
            pts.append((pts[-1][0] + rnd.random() * 4 - 2, pts[-1][1] + sg * (1.0 + rnd.random() * 0.9)))
        f.line(pts, p["lt"], 1.2)
        f.line(pts, p["md"], 0.5)
    for a in (-150, -120, 170):
        a = math.radians(a)
        f.line([(18 + math.cos(a) * 16, 24 + math.sin(a) * 16), (18 + math.cos(a) * 24, 24 + math.sin(a) * 24)], p["lt"], 1.6)


def chains(x):
    p, f, g = x.pal, x.front, x.glow
    for side in (0, 1):
        y = BY0 - 0.8 if side == 0 else BY1 + 0.8
        for xx in np.arange(BX0 + 12, BX1 - 22, 5.0):
            f.ell(xx + (2.5 if int(xx) % 2 else 0), y, 2.6, 1.3, p["lt"], p["dk"], 0.45)
            f.ell(xx + (2.5 if int(xx) % 2 else 0), y, 1.1, 0.5, (0, 0, 0, 0))
    g.glow(lambda t: t.line([(BX0 + 12, BY0 - 0.8), (BX1 - 22, BY0 - 0.8)], (255, 255, 255, 200), 0.6), WHITE, 1, 0.5)


def gate(x):
    p, f, g = x.pal, x.front, x.glow
    cx = BX1 - 12
    arch = [(cx - 9, 40)] + [(cx + math.cos(math.radians(a)) * 9, 21 + math.sin(math.radians(a)) * 9) for a in range(180, 361, 15)] + [(cx + 9, 40)]
    f.poly(arch, p["md"], p["dk"], 0.6)
    arch_in = [(cx - 6, 40)] + [(cx + math.cos(math.radians(a)) * 6, 21 + math.sin(math.radians(a)) * 6) for a in range(180, 361, 15)] + [(cx + 6, 40)]
    f.poly(arch_in, p["dk"])
    g.poly(arch_in, (255, 255, 255, 60))
    for k in range(7):
        xx = BX0 + 20 + k * 26
        f.ell(xx, BY0 - 2.6, 1.9, 1.9, p["md"], p["dk"], 0.4)
        rune(f, xx, BY0 - 2.6, 1.0, p["hi"], k)
        g.ell(xx, BY0 - 2.6, 1.3, 1.3, (255, 255, 255, 200))
    for a in (-60, -120):
        a = math.radians(a)
        f.line([(18 + math.cos(a) * 17, 24 + math.sin(a) * 17), (18 + math.cos(a) * 22, 24 + math.sin(a) * 22)], p["hi"], 1.3)


def horns(x):
    p, f, g = x.pal, x.front, x.glow
    horn(f, 12, 8, 11, p["lt"], p["dk"], -1)
    horn(f, 24, 8, 11, p["lt"], p["dk"], 1)
    horn(f, BX1 - 6, 24, 14, p["lt"], p["dk"], 1)
    spikes(f, [(BX0 + 12, BY0 + 0.3), (BX1 - 20, BY0 + 0.3)], 2.8, 7, p["md"], p["dk"], -1, x.seed, 0.4)
    g.glow(lambda t: horn(t, BX1 - 6, 24, 14, (255, 255, 255), (255, 255, 255), 1), WHITE, 1.4, 0.35)


def crown(x):
    p, f, g = x.pal, x.front, x.glow
    pts = [(86, 3.5), (92, -1), (98, 2), (104, -2.5), (110, 2), (116, -1), (122, 3.5)]
    cx = 136
    pts = [(cx - 16, 3.2), (cx - 12, -0.5), (cx - 7, 2), (cx, -2.8), (cx + 7, 2), (cx + 12, -0.5), (cx + 16, 3.2)]
    f.poly(pts + [(cx + 16, 4.2), (cx - 16, 4.2)], p["lt"], p["dk"], 0.5)
    for qx, qy in ((cx - 12, -0.5), (cx, -2.8), (cx + 12, -0.5)):
        gem(f, qx, qy + 1.2, 1.4, p["ac"], mix(p["ac"], WHITE, 0.6), p["dk"])
        g.ell(qx, qy + 1.2, 1.2, 1.2, (255, 255, 255, 230))


def scales(x):
    p, f = x.pal, x.front
    for row, y in enumerate((BY0 - 0.5, BY1 + 0.5)):
        for xx in np.arange(BX0 + 12 + row * 2.5, BX1 - 16, 5.0):
            f.ell(xx, y, 2.8, 2.2, p["lt"] if row == 0 else p["md"], p["dk"], 0.35)
    spikes(f, [(BX0 + 12, BY0 - 2.2), (BX1 - 20, BY0 - 2.2)], 4.5, 9, p["md"], p["dk"], -1, x.seed, 0.7)
    for a in (-40, -75, -110):
        a = math.radians(a)
        f.poly([(18 + math.cos(a) * 17, 24 + math.sin(a) * 17), (18 + math.cos(a) * 25, 24 + math.sin(a) * 25), (18 + math.cos(a + 0.3) * 17, 24 + math.sin(a + 0.3) * 17)], p["lt"], p["dk"], 0.4)
    horn(f, BX1 - 10, 26, 9, p["lt"], p["dk"], 1)


def halo_small(x):
    p, f, g = x.pal, x.front, x.glow
    f.d.ellipse([(136 - 22) * 4, (-5) * 4, (136 + 22) * 4, (4) * 4], outline=p["hi"] + (255,), width=4)
    g.glow(lambda t: t.d.ellipse([(136 - 22) * 4, (-5) * 4, (136 + 22) * 4, (4) * 4], outline=(255, 255, 255, 255), width=4), WHITE, 1.6, 0.9)


def halo(x):
    p, f, g = x.pal, x.front, x.glow
    for r in (14, 11):
        f.d.ellipse([(BX1 - 11 - r) * 4, (30 - r) * 4, (BX1 - 11 + r) * 4, (30 + r) * 4], outline=(p["hi"] if r == 14 else p["lt"]) + (255,), width=4)
    f.d.ellipse([(18 - 22) * 4, (24 - 22) * 4, (18 + 22) * 4, (24 + 22) * 4], outline=p["lt"] + (255,), width=3)
    g.glow(lambda t: t.d.ellipse([(BX1 - 11 - 14) * 4, (30 - 14) * 4, (BX1 - 11 + 14) * 4, (30 + 14) * 4], outline=(255, 255, 255, 255), width=4), WHITE, 1.8, 0.9)
    halo_small(x)


def rays(x):
    p, f, g = x.pal, x.front, x.glow
    for a in range(-160, 20, 22):
        a = math.radians(a)
        c0, c1 = (BX1 - 11, 30), (BX1 - 11 + math.cos(a) * 21, 30 + math.sin(a) * 21)
        f.line([(c0[0] + math.cos(a) * 12, c0[1] + math.sin(a) * 12), c1], p["hi"], 0.9)
        g.line([(c0[0] + math.cos(a) * 12, c0[1] + math.sin(a) * 12), c1], (255, 255, 255, 160), 0.8)


def phylactery(x):
    p, f, g = x.pal, x.front, x.glow
    cx, cy = 136, 21
    f.poly([(cx - 3.2, cy), (cx, cy - 4), (cx + 3.2, cy), (cx, cy + 3.5)], p["gl"], p["dk"], 0.5)
    g.glow(lambda t: t.poly([(cx - 3.2, cy), (cx, cy - 4), (cx + 3.2, cy), (cx, cy + 3.5)], (255, 255, 255)), WHITE, 1.8, 1.0)
    for sgn in (-1, 1):
        f.line([(cx + sgn * 3.5, cy), (cx + sgn * 16, cy)], p["lt"], 0.6)


def broken_crown(x):
    p, f = x.pal, x.front
    cx = 136
    f.poly([(cx - 16, 4.4), (cx - 14, -0.5), (cx - 9, 2.6), (cx - 5, -1.6), (cx - 2, 4.4)], p["lt"], p["dk"], 0.5)
    f.poly([(cx + 2, 4.4), (cx + 4, 1), (cx + 9, 3.2), (cx + 13, -0.8), (cx + 16, 4.4)], shade(p["lt"], 0.8), p["dk"], 0.5)
    f.line([(cx - 2, 4.4), (cx + 2, 1.5), (cx, 4.4)], p["dk"], 0.6)
    x.glow.ell(cx - 9, 3, 1, 1, (255, 255, 255, 150))


def tatters(x):
    p, f = x.pal, x.front
    rnd = random.Random(x.seed + 41)
    for i in range(10):
        sx = BX0 + 14 + rnd.random() * (BX1 - BX0 - 34)
        h = 3 + rnd.random() * 6
        f.poly([(sx - 1.6, BY1), (sx + 1.6, BY1), (sx + rnd.random() * 2 - 1, BY1 + h)], shade(p["md"], 0.8), p["dk"], 0.25)
    for sx, sy in ((90, 0.5), (160, 0.3)):
        f.line([(sx, sy), (sx + 5, sy + 2), (sx + 3, sy + 4)], p["dk"], 0.5)


def hieroglyph(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 51)
    y0 = BY0 + 0.3
    f.rect(BX0 + 12, y0 - 0.2, BX1 - 18, y0 + 1.4, shade(p["md"], 0.7))
    for xx in np.arange(BX0 + 14, BX1 - 22, 4.0):
        k = rnd.randint(0, 3)
        if k == 0:
            f.ell(xx, y0 + 0.6, 0.8, 0.8, p["hi"])
        elif k == 1:
            f.line([(xx - 0.8, y0 + 1.2), (xx, y0 - 0.1), (xx + 0.8, y0 + 1.2)], p["hi"], 0.35)
        elif k == 2:
            f.rect(xx - 0.8, y0, xx + 0.8, y0 + 1.2, p["hi"])
        else:
            f.line([(xx, y0 - 0.1), (xx, y0 + 1.3)], p["hi"], 0.4)
    f.rect(BX0 + 12, BY1 - 1.4, BX1 - 18, BY1 + 0.2, shade(p["md"], 0.7))
    for xx in np.arange(BX0 + 16, BX1 - 22, 8.0):
        f.poly([(xx - 1.6, BY1 - 1.2), (xx, BY1 - 0.1), (xx + 1.6, BY1 - 1.2)], p["ac"])
    # wings of a scarab at the right end, a sun disc on top of the plate
    f.ell(BX1 - 8, 30, 5, 4, p["ac"], p["dk"], 0.5)
    f.poly([(BX1 - 14, 30), (BX1 - 22, 24), (BX1 - 20, 33)], p["lt"], p["dk"], 0.4)
    f.ell(136, 0.5, 4, 3, p["gl"], p["dk"], 0.5)
    g.glow(lambda t: t.ell(136, 0.5, 3.5, 2.8, (255, 255, 255)), WHITE, 1.5, 0.9)


def spores(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 61)
    for _ in range(22):
        sx = BX0 + 10 + rnd.random() * (BX1 - BX0 - 24)
        sy = BY0 - 1 - rnd.random() * 4 if rnd.random() < 0.6 else BY1 + 1 + rnd.random() * 3
        r = 0.8 + rnd.random() * 1.6
        f.ell(sx, sy, r, r, p["lt"], p["dk"], 0.25)
        g.ell(sx, sy, r * 0.6, r * 0.6, (255, 255, 255, 150))


def tendrils(x):
    p, f = x.pal, x.front
    rnd = random.Random(x.seed + 71)
    for sgn in (-1, 1):
        for i in range(4):
            pts = [(BX1 - 8, 30)]
            ang = math.radians(-90 + sgn * (20 + i * 25))
            for k in range(1, 7):
                ang += (rnd.random() - 0.5) * 0.9
                pts.append((pts[-1][0] + math.cos(ang) * 2.4, pts[-1][1] + math.sin(ang) * 2.4))
            f.line(pts, p["md"], 1.6)
            f.line(pts, p["lt"], 0.6)


def triad(x):
    p, f, g = x.pal, x.front, x.glow
    cx, cy = BX1 - 10, 30
    for k in range(3):
        a = math.radians(-90 + k * 120)
        pts = [(cx + math.cos(a) * 11, cy + math.sin(a) * 11), (cx + math.cos(a + 2.1) * 5, cy + math.sin(a + 2.1) * 5), (cx + math.cos(a - 2.1) * 5, cy + math.sin(a - 2.1) * 5)]
        f.poly(pts, p["lt"] if k else p["hi"], p["dk"], 0.5)
    f.ell(cx, cy, 3, 3, p["gl"], p["dk"], 0.4)
    g.glow(lambda t: t.ell(cx, cy, 3, 3, (255, 255, 255)), WHITE, 1.5, 1.0)
    for sx in (BX0 + 30, 136, BX1 - 40):
        f.poly([(sx - 3, BY0), (sx, BY0 - 4.5), (sx + 3, BY0)], p["lt"], p["dk"], 0.4)


def eclipse(x):
    p, f, g = x.pal, x.front, x.glow
    cx, cy = BX1 - 10, 30
    for k in range(16):
        a = math.radians(k * 22.5)
        f.poly([(cx + math.cos(a - 0.12) * 10, cy + math.sin(a - 0.12) * 10), (cx + math.cos(a) * 15.5, cy + math.sin(a) * 15.5), (cx + math.cos(a + 0.12) * 10, cy + math.sin(a + 0.12) * 10)], p["lt"], None)
    f.ell(cx, cy, 9, 9, p["gl"], p["dk"], 0.5)
    f.ell(cx + 2.4, cy - 1, 8, 8, shade(p["dk"], 1.0), None)
    g.glow(lambda t: t.ell(cx, cy, 12, 12, (255, 255, 255)), WHITE, 2.4, 0.9)
    cx2 = 18
    f.ell(cx2, 4, 4, 3, p["lt"], p["dk"], 0.4)
    f.ell(cx2 + 1.4, 3.5, 3.6, 2.8, p["dk"])


def sins(x):
    p, f, g = x.pal, x.front, x.glow
    cols = [p["lt"], p["gl"], p["hi"], p["ac"], p["lt"], p["gl"], p["hi"]]
    for k in range(7):
        xx = 62 + k * 24
        gem(f, xx, BY0 - 2.2, 2.0, cols[k], mix(cols[k], WHITE, 0.55), p["dk"])
        g.ell(xx, BY0 - 2.2, 1.7, 1.7, (255, 255, 255, 220))
    f.poly([(BX1 - 18, 30), (BX1 - 8, 20), (BX1 - 2, 30), (BX1 - 8, 40)], p["md"], p["dk"], 0.5)
    f.poly([(BX1 - 14, 30), (BX1 - 8, 24), (BX1 - 5, 30), (BX1 - 8, 36)], p["gl"], p["dk"], 0.4)
    g.poly([(BX1 - 14, 30), (BX1 - 8, 24), (BX1 - 5, 30), (BX1 - 8, 36)], (255, 255, 255, 200))


def spades(x):
    p, f, g = x.pal, x.front, x.glow

    def spade(c, cx, cy, s, col, ol):
        c.poly([(cx, cy - s)] + [(cx + math.cos(math.radians(a)) * s * 0.62 * (1 if a < 90 or a > 270 else 1), cy + s * 0.1 + math.sin(math.radians(a)) * s * 0.5) for a in range(0, 181, 15)], col, ol, 0.4)
        c.poly([(cx, cy - s), (cx - s * 0.62, cy + s * 0.15), (cx - s * 0.55, cy + s * 0.5), (cx - s * 0.1, cy + s * 0.5), (cx - s * 0.2, cy + s * 0.9), (cx + s * 0.2, cy + s * 0.9), (cx + s * 0.1, cy + s * 0.5), (cx + s * 0.55, cy + s * 0.5), (cx + s * 0.62, cy + s * 0.15)], col, ol, 0.4)
    spade(f, BX1 - 9, 29, 10, p["md"], p["dk"])
    spade(f, BX1 - 9, 29, 6, p["lt"], p["dk"])
    g.glow(lambda t: spade(t, BX1 - 9, 29, 6, (255, 255, 255), None), WHITE, 1.6, 0.8)
    for xx in range(BX0 + 20, BX1 - 24, 24):
        spade(f, xx, BY0 - 2.4, 2.2, p["lt"], p["dk"])


def knight_wings(x):
    p, f, g = x.pal, x.front, x.glow
    cx = BX1 - 9
    f.poly([(cx - 7, 21), (cx + 7, 21), (cx + 7, 31), (cx, 40), (cx - 7, 31)], p["md"], p["dk"], 0.6)
    f.poly([(cx - 4.5, 23), (cx + 4.5, 23), (cx + 4.5, 30), (cx, 36), (cx - 4.5, 30)], p["lt"], p["dk"], 0.4)
    f.line([(cx, 23), (cx, 35)], p["hi"], 0.6)
    f.line([(cx - 4, 28), (cx + 4, 28)], p["hi"], 0.6)
    g.glow(lambda t: t.line([(cx, 23), (cx, 35)], (255, 255, 255), 0.8), WHITE, 1.2, 0.8)
    fan(f, 18, 7, [-170, -150, -130, -110, -90, -70, -50, -30], 8, 2.6, p["lt"], p["dk"])
    for xx in range(BX0 + 20, BX1 - 26, 20):
        f.poly([(xx - 2, BY0), (xx, BY0 - 3.4), (xx + 2, BY0)], p["hi"], p["dk"], 0.3)


def meander(x):
    p, f, g = x.pal, x.front, x.glow
    y = BY0 - 0.4
    for xx in np.arange(BX0 + 14, BX1 - 24, 8.0):
        f.line([(xx, y), (xx, y - 3), (xx + 4, y - 3), (xx + 4, y - 1.2), (xx + 2, y - 1.2)], p["hi"], 0.7)
    for xx in np.arange(BX0 + 16, BX1 - 24, 8.0):
        f.line([(xx, BY1 + 0.5), (xx, BY1 + 3), (xx + 4, BY1 + 3), (xx + 4, BY1 + 1.2), (xx + 2, BY1 + 1.2)], p["lt"], 0.7)
    horn(f, BX1 - 14, 22, 10, p["lt"], p["dk"], 1)
    horn(f, BX1 - 8, 22, 10, p["lt"], p["dk"], -1)
    flame(f, BX1 - 3, 38, 7, 5, p["gl"], p["hi"])
    g.glow(lambda t: flame(t, BX1 - 3, 38, 7, 5, (255, 255, 255)), WHITE, 1.4, 0.9)


def rivets(x):
    p, f = x.pal, x.front
    for xx in range(BX0 + 14, BX1 - 18, 10):
        for yy in (BY0 - 0.2, BY1 + 0.2):
            f.ell(xx, yy, 1.3, 1.3, p["lt"], p["dk"], 0.35)
            f.ell(xx - 0.3, yy - 0.3, 0.5, 0.5, p["hi"])
    f.poly([(BX1 - 14, 21), (BX1 - 2, 24), (BX1 - 2, 36), (BX1 - 14, 40)], p["md"], p["dk"], 0.6)
    for yy in (26, 31, 36):
        f.ell(BX1 - 8, yy, 1.2, 1.2, p["lt"], p["dk"], 0.3)


def embers_top(x):
    p, f, g = x.pal, x.front, x.glow
    rnd = random.Random(x.seed + 81)
    for _ in range(14):
        sx = BX0 + 14 + rnd.random() * (BX1 - BX0 - 34)
        sy = BY0 - 1 - rnd.random() * 4
        f.ell(sx, sy, 0.7, 0.7, p["gl"])
        g.ell(sx, sy, 1.0, 1.0, (255, 255, 255, 200))
    g.glow(lambda t: t.rect(BX0 + 14, BY0 - 0.5, BX1 - 18, BY0 + 0.2, (255, 255, 255)), WHITE, 1.5, 0.7)


def whirl(x):
    p, f, g = x.pal, x.front, x.glow
    cx, cy = BX1 - 10, 30
    for rr, w in ((12, 1.4), (9, 1.2), (6, 1.0), (3, 0.8)):
        pts = []
        for t in np.linspace(0, 4.5, 40):
            r = rr * (1 - t / 6)
            pts.append((cx + math.cos(t + rr) * r, cy + math.sin(t + rr) * r))
        f.line(pts, p["lt"] if rr % 2 else p["hi"], w)
        g.line(pts, (255, 255, 255, 140), w * 0.6)


def runes_motif(x):
    p, f, g = x.pal, x.front, x.glow
    for k, xx in enumerate(range(BX0 + 16, BX1 - 22, 14)):
        f.ell(xx, BY0 - 2.2, 2.4, 2.4, p["md"], p["dk"], 0.4)
        rune(f, xx, BY0 - 2.2, 1.3, p["hi"], k + x.seed, 0.4)
        g.glow(lambda t, xx=xx, k=k: rune(t, xx, BY0 - 2.2, 1.3, (255, 255, 255), k + x.seed, 0.5), WHITE, 0.9, 0.9)
    f.d.ellipse([(BX1 - 10 - 8) * 4, (30 - 8) * 4, (BX1 - 10 + 8) * 4, (30 + 8) * 4], outline=p["lt"] + (255,), width=5)
    for k in range(6):
        a = k * math.pi / 3
        rune(f, BX1 - 10 + math.cos(a) * 5, 30 + math.sin(a) * 5, 1.4, p["hi"], k)


def orbs(x):
    p, f, g = x.pal, x.front, x.glow
    for cx, cy, r in ((BX1 - 6, 24, 3.6), (BX1 - 16, 36, 2.6), (BX1 - 3, 36, 1.8), (6, 3, 2.4), (30, 2, 1.6)):
        f.ell(cx, cy, r, r, p["hi"], p["md"], 0.35)
        g.glow(lambda t, cx=cx, cy=cy, r=r: t.ell(cx, cy, r, r, (255, 255, 255)), WHITE, 1.6, 0.9)


def quills(x):
    p, f, g = x.pal, x.front, x.glow
    for sgn, ox in ((1, BX1 - 22), (1, BX1 - 10)):
        f.poly([(ox, 38), (ox + 3, 18), (ox + 4.5, 17), (ox + 5, 25), (ox + 3.5, 38)], p["lt"], p["dk"], 0.4)
        f.line([(ox + 4, 18), (ox + 3.6, 34)], p["hi"], 0.4)
    f.poly([(BX1 - 8, 38), (BX1 - 6, 41), (BX1 - 10, 41)], p["gl"], p["dk"], 0.3)
    g.glow(lambda t: t.poly([(BX1 - 8, 38), (BX1 - 6, 41), (BX1 - 10, 41)], (255, 255, 255)), WHITE, 1.0, 0.9)


def scales_motif(x):
    p, f, g = x.pal, x.front, x.glow
    cx, cy = BX1 - 12, 30
    f.line([(cx - 11, 24), (cx + 11, 24)], p["lt"], 1.0)
    f.line([(cx, 24), (cx, 40)], p["lt"], 1.2)
    for sgn in (-1, 1):
        f.line([(cx + sgn * 11, 24), (cx + sgn * 14, 32)], p["hi"], 0.4)
        f.line([(cx + sgn * 11, 24), (cx + sgn * 8, 32)], p["hi"], 0.4)
        f.poly([(cx + sgn * 8, 32), (cx + sgn * 14, 32), (cx + sgn * 11, 35)], p["lt"], p["dk"], 0.4)
    f.poly([(cx - 5, 40), (cx + 5, 40), (cx + 3, 37.5), (cx - 3, 37.5)], p["md"], p["dk"], 0.4)
    g.glow(lambda t: t.line([(cx - 11, 24), (cx + 11, 24)], (255, 255, 255), 1.0), WHITE, 1.4, 0.7)


def embers_unused(x):
    pass


MOTIFS = {k: v for k, v in list(globals().items()) if callable(v) and k in (
    "wings", "laurel", "thorns", "drips", "bones", "flames", "icicles", "crystals", "bolts", "void_tendrils", "stars", "vines", "waves", "shells", "fangs", "gears",
    "shards", "roots", "chains", "gate", "horns", "crown", "scales", "halo_small", "halo", "rays", "phylactery", "broken_crown", "tatters", "hieroglyph", "spores",
    "tendrils", "triad", "eclipse", "sins", "spades", "knight_wings", "meander", "rivets", "embers_top", "whirl", "runes_motif", "orbs", "quills", "scales_motif")}

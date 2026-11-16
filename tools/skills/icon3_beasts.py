"""Skill icons v3: the 27 Beast Will skills, each a Unique (bw_<key>_unique) and an Ultimate (bw_<key>, animated) that share one symbol."""
import math

import numpy as np

from icon3_lib import blur, circle, line, poly, ring, spline, star, taper
from icon3_parts import ICONS, R, rays
from icon3_symbols import eye, eyes2, flame, orb, pal, wing


def beast(key, palette, fn):
    ICONS.append(dict(id="bw_%s_unique" % key, cls="unique", pal=palette, fn=fn, anim=False))
    ICONS.append(dict(id="bw_%s" % key, cls="ultimate", pal=palette, fn=fn, anim=True))


def T(cx, cy, s):
    def f(pts):
        return [(cx + (x - 50) * s, cy + (y - 50) * s) for x, y in pts]
    return f


def hp(c, pts, col, cx=50, cy=50, s=1.0, **kw):
    """A part symmetric round the vertical line through cx (pts = the right half in the 0..100 head space, the head centred on 50,50)."""
    t = T(cx, cy, s)
    r = t(pts)
    left = [(2 * cx - x, y) for x, y in r]
    m = np.maximum(poly(r, round_=kw.pop("round_", 1.0)), poly(left, round_=1.0))
    c.part(m, col, **kw)
    return m


def hflat(c, pts, col, cx=50, cy=50, s=1.0, alpha=1.0):
    t = T(cx, cy, s)
    r = t(pts)
    left = [(2 * cx - x, y) for x, y in r]
    c.flat(np.maximum(poly(r, round_=0.6), poly(left, round_=0.6)), col, alpha)


def glow_eyes(c, cx, cy, s, col, x=13, y=44, w=12):
    for sx in (-1, 1):
        ex, ey = cx + sx * x * s, cy + (y - 50) * s
        e = poly([(ex - sx * w * 0.5 * s, ey + 1.5 * s), (ex + sx * w * 0.5 * s, ey - 2.5 * s), (ex + sx * w * 0.5 * s, ey + 1.6 * s), (ex - sx * w * 0.2 * s, ey + 3.2 * s)], round_=0.5)
        c.flat(e, (14, 8, 12), 0.95)
        c.flat(poly([(ex - sx * w * 0.38 * s, ey + 1.6 * s), (ex + sx * w * 0.42 * s, ey - 1.3 * s), (ex + sx * w * 0.42 * s, ey + 1.0 * s), (ex - sx * w * 0.1 * s, ey + 2.2 * s)], round_=0.4), col, 1.0)
        c.add(blur(e, 3.0), col, 0.6)


def canid(c, fur, light, eyec, cx=50, cy=50, s=1.0, ear=1.0, ear_in=(200, 120, 130), nose=(20, 16, 30), crest=None):
    if crest:
        for i in range(5):
            x = cx - 12 * s + i * 6 * s
            c.part(poly([(x - 2.6 * s, cy - 24 * s), (x, cy - 38 * s - (i % 2) * 4 * s), (x + 2.6 * s, cy - 24 * s)], round_=0.4), crest, bevel=1.6, spec=0.3)
    hp(c, [(60, 26), (70 + 2 * ear, 50 - 46 * ear), (80, 34)], fur, cx, cy, s, bevel=2.2)
    hflat(c, [(64, 28), (70, 16), (75, 32)], ear_in, cx, cy, s, 0.85)
    hp(c, [(50, 22), (62, 26), (76, 38), (80, 52), (68, 64), (60, 70), (56, 84), (50, 88)], fur, cx, cy, s, bevel=3.8, spec=0.4)
    hp(c, [(50, 54), (58, 62), (55, 82), (50, 86)], light, cx, cy, s, bevel=2.0, outline=False)
    c.flat(circle(cx, cy + 32 * s, 3.6 * s, 2.8 * s), nose, 1.0)
    glow_eyes(c, cx, cy, s, eyec)


def fangs(c, cx, y, w, h, n=4, col=(250, 246, 236)):
    for i in range(n):
        x = cx - w / 2 + (i + 0.5) * w / n
        c.flat(poly([(x - w / n * 0.4, y), (x + w / n * 0.4, y), (x, y + h)], round_=0.2), col, 0.97)


def snake_head(c, x, y, ang, s, col, eyec=(255, 70, 50), belly=None):
    h = poly(R([(x - 6 * s, y), (x - 6.5 * s, y - 8 * s), (x - 3 * s, y - 15 * s), (x + 3 * s, y - 15 * s), (x + 6.5 * s, y - 8 * s), (x + 6 * s, y)], ang, x, y), round_=1.0)
    c.part(h, col, bevel=2.2, spec=0.4)
    for sx in (-1, 1):
        ex, ey = R([(x + sx * 3.4 * s, y - 9 * s)], ang, x, y)[0]
        c.dot(ex, ey, 1.5 * s, eyec, 1.0)
    return h


def neck(c, p0, p1, w0, w1, curve, col):
    c.part(taper(p0, p1, w0, w1, curve=curve), col, bevel=2.4, spec=0.35)


def multi_head(c, n, col, eyec, base=(50, 92), spread=130, length=52, s=1.0, wide=9):
    for i in range(n):
        f = i / (n - 1) if n > 1 else 0.5
        a = math.radians(-90 - spread / 2 + f * spread)
        ln = length * (0.8 + 0.2 * (1 - abs(f - 0.5) * 2))
        tip = (base[0] + ln * math.cos(a), base[1] + ln * math.sin(a))
        bend = (f - 0.5) * 14
        neck(c, base, tip, wide, wide * 0.6, bend, col)
        d = math.degrees(a) + 90 + (f - 0.5) * 20
        snake_head(c, tip[0], tip[1], d, s, col, eyec)


# ------------------------------------------------------------------------------------------------ the beasts
def ammit(c):
    c.part(ring(50, 52, 42, 4.0), (240, 200, 90), bevel=1.6, spec=0.6, outline=False)
    up = poly([(8, 36), (56, 28), (92, 36), (94, 46), (58, 50), (10, 54)], round_=2.0)
    lo = poly([(12, 62), (60, 56), (88, 66), (86, 78), (40, 82), (10, 74)], round_=2.0)
    c.part(lo, (130, 150, 70), bevel=3.0, spec=0.3)
    c.part(up, (150, 170, 80), bevel=3.0, spec=0.3)
    c.flat(poly([(14, 52), (92, 48), (88, 62), (14, 60)], round_=1.0), (120, 20, 40), 1.0)
    for i in range(7):
        x = 22 + i * 10
        c.flat(poly([(x - 3, 52 - i * 0.55), (x + 3, 52 - i * 0.55), (x, 59)], round_=0.2), (250, 246, 236), 0.97)
        c.flat(poly([(x - 3, 61 + i * 0.2), (x + 3, 61 + i * 0.2), (x + 0.5, 54)], round_=0.2), (250, 246, 236), 0.97)
    c.part(circle(34, 36, 6), (255, 200, 60), bevel=2.0)
    c.flat(poly([(33.4, 31), (34.6, 31), (34.6, 41), (33.4, 41)], round_=0.2), (20, 10, 10), 1.0)
    for x in (70, 80):
        c.flat(circle(x, 38, 1.6), (40, 50, 20), 0.9)
    c.part(poly([(48, 6), (56, 6), (60, 18), (46, 18)], round_=0.8), (255, 244, 210), bevel=1.8, spec=0.5)


def apophis(c):
    pts = []
    for i in range(48):
        t = i / 47.0
        a = math.radians(-70 + t * 720)
        r = 36 - t * 26
        pts.append((50 + r * math.cos(a), 54 + r * math.sin(a)))
    c.part(line(pts, 10), (110, 60, 160), bevel=3.2, spec=0.35, grad=((140, 80, 200), (70, 30, 110)))
    c.flat(line(pts[2:], 3.0, False), (180, 130, 230), 0.45)
    orb(c, 50, 54, 7, (170, 90, 255), (255, 255, 255))
    hx, hy = pts[0]
    c.part(poly(R([(hx - 8, hy + 4), (hx - 6, hy - 10), (hx, hy - 14), (hx + 6, hy - 10), (hx + 8, hy + 4)], 25, hx, hy), round_=1.5), (130, 70, 180), bevel=2.4, spec=0.4)
    c.dot(hx - 3, hy - 6, 1.8, (255, 220, 80), 1.0)
    c.dot(hx + 3, hy - 6, 1.8, (255, 220, 80), 1.0)


def cerberus(c):
    fur, light = (96, 60, 60), (150, 110, 100)
    canid(c, fur, light, (255, 110, 40), cx=24, cy=58, s=0.5, ear=0.6)
    canid(c, fur, light, (255, 110, 40), cx=76, cy=58, s=0.5, ear=0.6)
    canid(c, fur, light, (255, 110, 40), cx=50, cy=46, s=0.78, ear=0.7)
    c.part(ring(50, 90, 28, 4.0, 200, 340), (230, 170, 60), bevel=1.6, spec=0.6, outline=False)


def crested_boar(c):
    for i in range(7):
        x = 30 + i * 6.7
        c.part(poly([(x - 3.6, 30), (x, 8 + abs(i - 3) * 3), (x + 3.6, 30)], round_=0.5), (190, 90, 50), bevel=1.6, spec=0.3)
    hp(c, [(50, 22), (68, 24), (82, 40), (86, 60), (74, 78), (50, 88)], (150, 96, 70), bevel=4.0, spec=0.3)
    hp(c, [(66, 28), (76, 20), (80, 34)], (120, 70, 50), bevel=1.8)
    c.part(circle(50, 66, 17, 13), (230, 160, 150), bevel=3.2, spec=0.4)
    for sx in (-1, 1):
        c.flat(circle(50 + sx * 6, 66, 2.8, 4.0), (60, 20, 30), 1.0)
        c.part(taper((50 + sx * 18, 70), (50 + sx * 30, 44), 8, 1.4, curve=-sx * 8), (246, 240, 224), bevel=2.0, spec=0.6)
    glow_eyes(c, 50, 50, 1.0, (255, 90, 60), x=17, y=46, w=10)


def cu_sith(c):
    canid(c, (50, 120, 90), (140, 200, 150), (200, 255, 120), ear=1.0, ear_in=(150, 220, 150))
    c.part(ring(50, 50, 42, 2.6, 215, 325), (230, 240, 210), bevel=1.2, spec=0.6, outline=False)
    c.part(star(50, 17, 6, 1.5, 4), (230, 255, 200), bevel=1.0, outline=False)


def dragon_head(c, scale, horn, eyec, fire=True, cx=50, cy=52):
    for sx in (-1, 1):
        c.part(taper((cx + sx * 20, cy - 16), (cx + sx * 40, cy - 46), 11, 1.6, curve=-sx * 9), horn, bevel=2.4, spec=0.5)
        c.part(taper((cx + sx * 26, cy - 4), (cx + sx * 46, cy - 14), 9, 1.6, curve=sx * 5), horn, bevel=2.2, spec=0.5)
    hp(c, [(50, 20), (66, 26), (74, 40), (72, 56), (62, 62), (58, 84), (50, 88)], scale, cx, cy, 1.0, bevel=4.0, spec=0.4, grad=None)
    hp(c, [(50, 62), (58, 66), (56, 84), (50, 86)], (246, 214, 150), cx, cy, 1.0, bevel=2.0, outline=False)
    for sx in (-1, 1):
        c.flat(circle(cx + sx * 4, cy + 28, 1.8, 2.4), (30, 10, 10), 1.0)
    fangs(c, cx, cy + 34, 22, 8, 4)
    hflat(c, [(54, 36), (70, 36), (68, 42), (54, 42)], (12, 6, 10), cx, cy, 1.0, 0.8)
    glow_eyes(c, cx, cy, 1.0, eyec, x=14, y=40, w=14)
    if fire:
        flame(c, cx, cy + 46, 22, 16, (255, 190, 60), (255, 80, 20), side=False)


def dragon(c):
    dragon_head(c, (200, 50, 40), (246, 214, 150), (255, 220, 60))


def elderwood_guardian(c):
    c.part(poly([(40, 96), (36, 60), (30, 40), (70, 40), (64, 60), (60, 96)], round_=2.0), (120, 80, 50), bevel=3.6, spec=0.2)
    for x, y, r in ((30, 32, 16), (50, 22, 18), (70, 32, 16), (40, 40, 12), (62, 40, 12)):
        c.part(circle(x, y, r), (70, 170, 80), bevel=4.0, spec=0.3)
    c.flat(circle(50, 22, 18) * 0.0 + 0, (0, 0, 0), 0.0)
    glow_eyes(c, 50, 60, 0.9, (255, 220, 90), x=9, y=56, w=9)
    c.flat(poly([(43, 74), (50, 70), (57, 74), (54, 80), (46, 80)], round_=1.0), (24, 14, 10), 0.95)
    for sx in (-1, 1):
        c.part(taper((50 + sx * 8, 92), (50 + sx * 30, 94), 6, 2, curve=sx * 3), (100, 66, 42), bevel=1.6)


def fenrir(c):
    canid(c, (170, 190, 220), (230, 240, 255), (120, 230, 255), ear=1.15, ear_in=(150, 200, 240), crest=(200, 230, 255))
    for x, y, r in ((18, 24, 5), (84, 20, 4), (12, 66, 3.6), (90, 62, 4)):
        c.part(star(x, y, r, r * 0.3, 4), (210, 244, 255), bevel=1.0, outline=False)


def flame_serpent(c):
    pts = spline([(18, 90), (24, 62), (42, 52), (62, 42), (76, 26), (62, 14)], 8)
    c.part(line(pts, 12), (255, 150, 40), bevel=3.6, spec=0.2, grad=((255, 230, 90), (255, 60, 20)))
    c.flat(line(pts, 3.6, False), (255, 244, 190), 0.6)
    hx, hy = pts[-1]
    c.part(poly(R([(hx - 7, hy + 4), (hx - 6, hy - 8), (hx, hy - 14), (hx + 6, hy - 8), (hx + 7, hy + 4)], -80, hx, hy), round_=1.5), (255, 110, 40), bevel=2.4)
    c.dot(hx + 2, hy - 2, 1.8, (255, 255, 200), 1.0)
    flame(c, 74, 78, 30, 22, (255, 190, 60), (255, 70, 20), side=False)
    flame(c, 30, 40, 26, 18, (255, 190, 60), (255, 70, 20), side=False)


def hafgufa(c):
    for i in range(3):
        c.part(ring(50, 98, 50 - i * 12, 3.0, 205, 335), (80, 170, 220), bevel=1.4, spec=0.5, outline=False)
    c.part(poly([(8, 60), (22, 42), (50, 36), (78, 42), (92, 58), (84, 74), (52, 82), (22, 76)], round_=5.0), (70, 110, 150), bevel=5.0, spec=0.3)
    c.part(poly([(80, 58), (92, 38), (98, 28), (100, 44), (94, 52), (100, 60), (88, 68)], round_=2.0), (80, 124, 164), bevel=3.0)
    c.part(poly([(24, 70), (60, 76), (84, 70), (60, 82), (30, 80)], round_=3.0), (200, 224, 238), bevel=2.0, outline=False)
    c.part(circle(22, 56, 4), (255, 230, 120), bevel=1.6)
    c.part(poly([(40, 38), (50, 22), (66, 24), (62, 38)], round_=4.0), (110, 150, 90), bevel=2.6)
    c.part(poly([(52, 24), (54, 12), (58, 24)], round_=0.5), (60, 120, 50), bevel=1.2)
    c.flat(line([(30, 28), (30, 14)], 1.8, False), (190, 230, 255), 0.8)


def hydra(c):
    multi_head(c, 5, (60, 150, 90), (255, 220, 60), base=(50, 96), spread=120, length=52, s=1.7, wide=13)
    c.part(circle(50, 94, 12, 9), (50, 120, 70), bevel=3.0)


def jormungandr(c):
    c.part(ring(50, 52, 40, 14), (60, 150, 130), bevel=4.0, spec=0.3, grad=((90, 200, 170), (40, 110, 110)))
    for i in range(18):
        a = math.radians(i * 20)
        c.flat(line([(50 + 33 * math.cos(a), 52 + 33 * math.sin(a)), (50 + 45 * math.cos(a + 0.2), 52 + 45 * math.sin(a + 0.2))], 1.2, False), (30, 90, 90), 0.6)
    c.part(poly(R([(58, 0), (58, 20), (74, 30), (90, 20), (90, 0)], 14, 74, 14), round_=2.0), (90, 210, 175), bevel=3.0, spec=0.4)
    c.dot(70, 10, 2.6, (255, 230, 80), 1.0)
    c.dot(80, 12, 2.6, (255, 230, 80), 1.0)
    c.part(circle(50, 52, 8), (200, 244, 235), bevel=2.0, outline=False)
    c.add(blur(circle(50, 52, 10), 5), (120, 240, 220), 0.5)


def kitsune(c):
    for i, (ang, ln) in enumerate(((-60, 46), (-30, 52), (0, 54), (30, 52), (60, 46))):
        a = math.radians(ang - 90)
        base = (50, 78)
        tip = (50 + ln * math.cos(a), 78 + ln * math.sin(a))
        c.part(taper(base, tip, 15, 7, curve=(i - 2) * 5), (255, 150, 60), bevel=3.0, spec=0.3)
        c.flat(circle(tip[0], tip[1], 4.0), (255, 250, 240), 0.9)
    canid(c, (250, 150, 70), (255, 244, 230), (255, 230, 80), cy=54, s=0.82, ear=1.25, ear_in=(60, 30, 40), nose=(30, 10, 20))
    c.add(blur(circle(50, 94, 6), 5), (120, 200, 255), 0.5)


def kraken(c):
    for i in range(6):
        sx = -1 if i < 3 else 1
        k = i % 3
        base = (50 + sx * (6 + k * 5), 56)
        tip = (50 + sx * (26 + k * 10), 96 - k * 12)
        c.part(taper(base, tip, 11, 3, curve=sx * (10 - k * 4)), (60, 140, 190), bevel=2.6, spec=0.4)
    c.part(circle(50, 42, 27, 28), (50, 120, 180), bevel=5.0, spec=0.5)
    for sx in (-1, 1):
        c.part(circle(50 + sx * 11, 44, 7), (250, 246, 230), bevel=2.0, spec=0.6)
        c.flat(circle(50 + sx * 11, 45, 3.6, 5.2), (20, 10, 40), 1.0)
        c.add(blur(circle(50 + sx * 11, 45, 3.0), 2), (120, 240, 255), 0.7)
    c.flat(poly([(36, 24), (50, 16), (64, 24)], round_=3.0), (130, 210, 240), 0.5)


def lion(c):
    pts = []
    for i in range(24):
        a = math.radians(i * 15)
        r = 46 if i % 2 == 0 else 36
        pts.append((50 + r * math.cos(a), 50 + r * math.sin(a)))
    c.part(poly(pts, round_=1.6), (200, 120, 40), bevel=4.0, spec=0.2, grad=((240, 170, 60), (150, 80, 30)))
    c.part(circle(50, 52, 24, 26), (240, 190, 100), bevel=4.4, spec=0.3)
    c.part(circle(50, 66, 13, 10), (252, 232, 190), bevel=2.4, outline=False)
    c.part(poly([(44, 56), (56, 56), (50, 64)], round_=0.8), (90, 40, 40), bevel=1.0)
    c.flat(line([(50, 64), (50, 70)], 1.2, False), (80, 40, 40), 0.9)
    c.flat(line([(42, 72), (50, 70), (58, 72)], 1.2, False), (80, 40, 40), 0.9)
    glow_eyes(c, 50, 50, 1.0, (255, 200, 40), x=11, y=47, w=10)
    for sx in (-1, 1):
        c.part(circle(50 + sx * 20, 28, 6), (230, 170, 90), bevel=1.6)


def minotaur(c):
    for sx in (-1, 1):
        c.part(taper((50 + sx * 20, 34), (50 + sx * 46, 14), 11, 1.4, curve=-sx * 14), (240, 230, 206), bevel=2.4, spec=0.5)
        c.part(taper((50 + sx * 30, 40), (50 + sx * 46, 52), 11, 3, curve=sx * 4), (110, 66, 46), bevel=2.0)
    hp(c, [(50, 26), (66, 28), (74, 44), (70, 66), (62, 80), (50, 90)], (120, 74, 54), bevel=4.4, spec=0.3)
    c.part(circle(50, 74, 13, 11), (200, 150, 130), bevel=3.0, spec=0.4)
    for sx in (-1, 1):
        c.flat(circle(50 + sx * 5, 74, 2.2, 3.0), (40, 16, 20), 1.0)
    c.part(ring(50, 82, 6, 2.0), (246, 200, 70), bevel=1.2, spec=0.7, outline=False)
    glow_eyes(c, 50, 50, 1.0, (255, 60, 50), x=14, y=48, w=12)


def nidhoggr(c):
    for i in range(7):
        x = 14 + i * 12
        c.part(taper((x, 4), (x + (i - 3) * 4, 34), 5, 1.4, curve=(i % 2) * 6 - 3), (130, 90, 60), bevel=1.6)
    dragon_head(c, (84, 56, 120), (200, 190, 170), (255, 80, 200), fire=False, cy=58)
    c.add(blur(circle(50, 86, 8), 5), (190, 80, 255), 0.5)


def nue(c):
    for x, y, r in ((30, 52, 16), (48, 42, 20), (68, 50, 17), (40, 60, 14), (60, 62, 14)):
        c.part(circle(x, y, r), (86, 84, 130), bevel=4.4, spec=0.2)
    glow_eyes(c, 50, 50, 1.0, (255, 230, 80), x=11, y=48, w=12)
    c.part(poly([(60, 60), (44, 78), (54, 78), (46, 96), (72, 72), (60, 72)], round_=1.0), (255, 232, 90), bevel=2.6, spec=0.6)
    sn = spline([(80, 60), (92, 72), (86, 88), (70, 92)], 8)
    c.part(line(sn, 6), (80, 160, 100), bevel=2.0, spec=0.3)
    c.part(circle(70, 92, 5), (90, 180, 110), bevel=1.6)
    c.add(blur(circle(50, 50, 30), 10), (150, 120, 255), 0.25)


def pegasus(c):
    wing(c, 52, 78, 58, 70, (176, 206, 255), mirror="left", feathers=6, tilt=-8)
    c.part(poly([(30, 90), (32, 50), (42, 20), (50, 8), (58, 14), (62, 28), (78, 38), (94, 52), (96, 64), (84, 72), (70, 66), (62, 72), (60, 92)], round_=3.0), (246, 246, 252), bevel=4.0, spec=0.5)
    for i in range(5):
        c.part(taper((52 - i * 4, 14 + i * 12), (30 - i * 3, 22 + i * 14), 6, 2, curve=-3), (150, 200, 255), bevel=1.4)
    c.flat(circle(66, 44, 3.2), (20, 20, 50), 1.0)
    c.flat(circle(65.2, 43, 1.0), (255, 255, 255), 1.0)
    c.flat(circle(88, 62, 1.6), (150, 140, 160), 0.9)
    c.part(star(86, 14, 7, 1.6, 4), (255, 255, 255), bevel=1.0, outline=False)


def ridge_wolf(c):
    canid(c, (130, 140, 156), (200, 206, 216), (255, 220, 90), ear=1.0, ear_in=(150, 130, 140), crest=(100, 108, 124))


def scylla_charybdis(c):
    pts = []
    for i in range(60):
        t = i / 59.0
        a = math.radians(-30 + t * 900)
        r = 42 - t * 34
        pts.append((50 + r * math.cos(a), 52 + r * math.sin(a)))
    c.part(line(pts, 7), (60, 140, 210), bevel=2.8, spec=0.4, grad=((130, 210, 255), (30, 80, 170)))
    c.part(circle(50, 52, 7), (10, 20, 50), bevel=2.0)
    for sx, tip in ((-1, (16, 18)), (1, (86, 16))):
        c.part(taper((50 + sx * 30, 78), tip, 10, 2, curve=sx * 12), (90, 90, 170), bevel=2.4, spec=0.4)
        c.part(circle(tip[0], tip[1], 5.2), (120, 120, 200), bevel=1.8)
    fangs(c, 50, 47, 14, 5, 3)


def shadow_panther(c):
    c.part(ring(50, 46, 40, 8, 200, 340), (170, 150, 230), bevel=2.4, spec=0.5, outline=False)
    hp(c, [(58, 30), (68, 10), (80, 34)], (60, 40, 90), bevel=2.0)
    hp(c, [(50, 24), (64, 28), (78, 42), (74, 60), (62, 72), (58, 86), (50, 90)], (64, 46, 100), bevel=4.4, spec=0.45)
    hp(c, [(50, 56), (58, 62), (56, 84), (50, 86)], (110, 90, 150), bevel=2.0, outline=False)
    c.part(poly([(44, 60), (56, 60), (50, 68)], round_=0.8), (230, 150, 170), bevel=1.0)
    glow_eyes(c, 50, 50, 1.0, (140, 255, 150), x=14, y=44, w=13)
    for sx in (-1, 1):
        for k in range(3):
            c.flat(line([(50 + sx * 14, 66 + k * 3), (50 + sx * 30, 62 + k * 5)], 0.8, False), (220, 210, 250), 0.7)


def sphinx(c):
    hp(c, [(50, 8), (68, 14), (82, 34), (90, 90), (70, 76), (66, 56), (50, 56)], (40, 90, 170), bevel=4.0, spec=0.4)
    for i in range(5):
        y = 20 + i * 10
        c.flat(poly([(50, y), (50 + 36 - i * 2, y + 4), (50 + 36 - i * 2, y + 7), (50, y + 3)], round_=0.2), (246, 200, 70), 0.9)
        c.flat(poly([(50, y), (50 - 36 + i * 2, y + 4), (50 - 36 + i * 2, y + 7), (50, y + 3)], round_=0.2), (246, 200, 70), 0.9)
    c.part(circle(50, 50, 16, 22), (230, 180, 120), bevel=3.4, spec=0.3)
    c.part(poly([(50, 12), (54, 22), (50, 28), (46, 22)], round_=0.8), (255, 90, 90), bevel=1.2)
    for sx in (-1, 1):
        c.flat(poly([(50 + sx * 4, 46), (50 + sx * 14, 44), (50 + sx * 14, 48), (50 + sx * 4, 49)], round_=0.4), (20, 20, 40), 1.0)
        c.flat(circle(50 + sx * 9, 46.6, 1.5), (255, 220, 120), 1.0)
    c.flat(line([(44, 62), (56, 62)], 1.4, False), (150, 70, 60), 0.9)
    c.part(poly([(40, 74), (60, 74), (56, 94), (44, 94)], round_=1.0), (246, 200, 70), bevel=2.0, spec=0.5)


def thunder_fang_chimera(c):
    for sx in (-1, 1):
        c.part(poly([(50 + sx * 4, 4), (50 + sx * 20, 30), (50 + sx * 12, 30), (50 + sx * 26, 56), (50 + sx * 4, 36), (50 + sx * 12, 36)], round_=0.8), (255, 232, 90), bevel=2.2, spec=0.6)
    lion(c) if False else None
    c.part(circle(50, 52, 24, 26), (230, 170, 70), bevel=4.4, spec=0.3)
    c.part(circle(50, 66, 13, 10), (252, 232, 190), bevel=2.4, outline=False)
    c.flat(poly([(44, 56), (56, 56), (50, 64)], round_=0.6), (80, 36, 36), 1.0)
    for sx in (-1, 1):
        c.part(taper((50 + sx * 7, 70), (50 + sx * 9, 92), 5, 1.2), (250, 246, 240), bevel=1.6, spec=0.7)
    glow_eyes(c, 50, 50, 1.0, (120, 230, 255), x=11, y=47, w=10)
    c.add(blur(circle(50, 52, 24), 8), (120, 200, 255), 0.25)


def typhon(c):
    for sx in (-1, 1):
        sn = spline([(50 + sx * 30, 94), (50 + sx * 44, 74), (50 + sx * 34, 56)], 8)
        c.part(line(sn, 8), (70, 110, 190), bevel=2.4, spec=0.3)
        snake_head(c, sn[-1][0], sn[-1][1], -sx * 20, 0.8, (80, 120, 200), (255, 240, 100))
    dragon_head(c, (70, 80, 150), (230, 230, 250), (255, 240, 100), fire=False, cy=48)
    c.part(poly([(54, 78), (46, 90), (52, 90), (46, 100), (62, 86), (56, 86)], round_=0.6), (255, 240, 100), bevel=2.0, spec=0.6)


def wind_skimmer(c):
    for i, (r, a0, a1) in enumerate(((40, 150, 260), (30, 300, 40), (22, 150, 250))):
        c.part(ring(50, 56, r, 3.0, a0, a1), (170, 240, 240), bevel=1.4, spec=0.5, outline=False)
    hp(c, [(54, 36), (96, 26), (84, 44), (98, 54), (60, 56)], (80, 190, 200), bevel=3.0, spec=0.4)
    hp(c, [(50, 22), (56, 38), (54, 70), (60, 92), (50, 80)], (230, 250, 252), bevel=2.6, spec=0.4)
    c.part(circle(50, 26, 6.4), (240, 252, 252), bevel=2.0)
    c.part(poly([(46, 26), (50, 32), (54, 26)], round_=0.5), (255, 190, 60), bevel=1.0)
    c.dot(47.5, 24, 1.3, (20, 20, 50), 1.0, glow=False)


def yamata_no_orochi(c):
    multi_head(c, 8, (110, 60, 130), (255, 60, 70), base=(50, 96), spread=170, length=50, s=1.1, wide=9)
    c.part(circle(50, 94, 14, 9), (90, 40, 100), bevel=3.0)
    c.part(ring(50, 94, 8, 2.4), (246, 200, 70), bevel=1.2, spec=0.7, outline=False)


beast("ammit", pal((150, 170, 80), (255, 200, 60), (240, 200, 90), (255, 200, 60), (100, 90, 30), (14, 12, 8)), ammit)
beast("apophis", pal((110, 60, 160), (170, 90, 255), (255, 220, 80), (150, 80, 255), (60, 30, 110), (8, 4, 20)), apophis)
beast("cerberus", pal((96, 60, 60), (255, 110, 40), (230, 170, 60), (255, 110, 40), (100, 40, 30), (16, 6, 6)), cerberus)
beast("crested_boar", pal((150, 96, 70), (190, 90, 50), (246, 240, 224), (255, 140, 80), (100, 60, 36), (16, 8, 6)), crested_boar)
beast("cu_sith", pal((50, 120, 90), (200, 255, 120), (230, 240, 210), (120, 240, 160), (30, 90, 64), (4, 14, 12)), cu_sith)
beast("dragon", pal((200, 50, 40), (246, 214, 150), (255, 220, 60), (255, 90, 40), (120, 30, 20), (20, 4, 4)), dragon)
beast("elderwood_guardian", pal((70, 170, 80), (120, 80, 50), (255, 220, 90), (110, 230, 110), (30, 90, 40), (4, 14, 6)), elderwood_guardian)
beast("fenrir", pal((170, 190, 220), (230, 240, 255), (120, 230, 255), (120, 210, 255), (40, 70, 120), (4, 8, 24)), fenrir)
beast("flame_serpent", pal((255, 150, 40), (255, 230, 90), (255, 60, 20), (255, 120, 30), (120, 40, 20), (20, 4, 4)), flame_serpent)
beast("hafgufa", pal((70, 110, 150), (200, 224, 238), (255, 230, 120), (90, 190, 240), (24, 70, 110), (2, 10, 24)), hafgufa)
beast("hydra", pal((60, 150, 90), (255, 220, 60), (50, 120, 70), (90, 240, 130), (24, 80, 44), (2, 14, 8)), hydra)
beast("jormungandr", pal((60, 150, 130), (200, 244, 235), (255, 230, 80), (80, 230, 200), (24, 80, 80), (2, 12, 14)), jormungandr)
beast("kitsune", pal((250, 150, 70), (255, 244, 230), (255, 230, 80), (255, 170, 90), (120, 60, 30), (18, 8, 8)), kitsune)
beast("kraken", pal((50, 120, 180), (60, 140, 190), (120, 240, 255), (80, 190, 255), (20, 60, 110), (2, 8, 24)), kraken)
beast("lion", pal((240, 190, 100), (200, 120, 40), (255, 200, 40), (255, 190, 60), (130, 80, 20), (18, 10, 4)), lion)
beast("minotaur", pal((120, 74, 54), (240, 230, 206), (255, 60, 50), (230, 110, 60), (90, 40, 30), (14, 6, 6)), minotaur)
beast("nidhoggr", pal((84, 56, 120), (200, 190, 170), (255, 80, 200), (180, 70, 240), (50, 24, 80), (8, 2, 14)), nidhoggr)
beast("nue", pal((86, 84, 130), (255, 232, 90), (80, 160, 100), (150, 130, 255), (50, 48, 100), (6, 6, 20)), nue)
beast("pegasus", pal((246, 246, 252), (150, 200, 255), (255, 255, 255), (170, 220, 255), (70, 90, 140), (6, 10, 28)), pegasus)
beast("ridge_wolf", pal((130, 140, 156), (200, 206, 216), (255, 220, 90), (200, 210, 230), (60, 66, 84), (8, 10, 16)), ridge_wolf)
beast("scylla_charybdis", pal((60, 140, 210), (90, 90, 170), (130, 210, 255), (80, 160, 255), (20, 56, 120), (2, 6, 24)), scylla_charybdis)
beast("shadow_panther", pal((64, 46, 100), (170, 150, 230), (140, 255, 150), (150, 120, 255), (44, 30, 80), (4, 2, 14)), shadow_panther)
beast("sphinx", pal((230, 180, 120), (40, 90, 170), (246, 200, 70), (255, 210, 90), (110, 84, 30), (12, 10, 18)), sphinx)
beast("thunder_fang_chimera", pal((230, 170, 70), (255, 232, 90), (120, 230, 255), (255, 230, 90), (110, 90, 30), (14, 12, 8)), thunder_fang_chimera)
beast("typhon", pal((70, 80, 150), (230, 230, 250), (255, 240, 100), (130, 150, 255), (40, 44, 110), (4, 4, 20)), typhon)
beast("wind_skimmer", pal((80, 190, 200), (230, 250, 252), (255, 190, 60), (150, 240, 240), (30, 96, 100), (4, 14, 16)), wind_skimmer)
beast("yamata_no_orochi", pal((110, 60, 130), (90, 40, 100), (255, 60, 70), (200, 80, 240), (60, 24, 76), (8, 2, 14)), yamata_no_orochi)

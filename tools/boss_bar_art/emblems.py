"""The 32x32 emblems of the boss bar styles (one per theme / creature type / arc). Drawn on a 32-unit canvas and scaled down from 4x.
Order = the cell order in emblems.png (8 per row); _index.json lists the names in this order."""
import math
import random

import numpy as np
from PIL import Image

from lib import Canvas, mix, shade, leaf, gear, drop, star, bolt, flame, gem, skull, horn, rune

WHITE = (255, 255, 255)
C = 16


def O(p):
    return p["dk"]


def disc(c, p, r=14.2):
    c.grad_ell(C, C, r, r, shade(p["dk"], 0.9), shade(mix(p["dk"], p["md"], 0.5), 0.55))
    c.d.ellipse([(C - r) * 4, (C - r) * 4, (C + r) * 4, (C + r) * 4], outline=p["lt"] + (255,), width=3)


def wing(c, p):
    for i, a in enumerate(range(-160, -10, 22)):
        leaf(c, 16, 22, math.radians(a), 13 - i * 0.4, 4.2, p["lt"] if i % 2 == 0 else p["hi"], outline=p["dk"])
    for i, a in enumerate(range(-170, -20, 22)):
        leaf(c, 16, 22, math.radians(a + 180 + 140), 6, 2.4, p["hi"])
    c.ell(16, 22, 2.6, 2.6, p["hi"], p["dk"], 0.5)


def thorn_crown(c, p):
    c.d.arc([4 * 4, 8 * 4, 28 * 4, 28 * 4], 0, 360, fill=p["lt"] + (255,), width=6)
    for k in range(9):
        a = math.radians(k * 40)
        x0, y0 = C + math.cos(a) * 10.5, 18 + math.sin(a) * 9.5
        c.poly([(x0 - 1.4, y0 - 1.4), (C + math.cos(a) * 15, 18 + math.sin(a) * 14.5), (x0 + 1.4, y0 + 1.4)], p["gl"], p["dk"], 0.3)
    c.ell(C, 18, 5, 4, p["dk"])
    c.ell(C, 18, 2.2, 1.8, p["gl"])


def skull_e(c, p):
    skull(c, C, 15, 22, p["hi"], p["dk"])
    c.ell(11.8, 15, 1.6, 1.8, p["gl"])
    c.ell(20.2, 15, 1.6, 1.8, p["gl"])


def flame_e(c, p):
    flame(c, C, 29, 26, 20, p["lt"], p["hi"])
    flame(c, C, 29, 14, 11, p["hi"], mix(p["hi"], WHITE, 0.6))


def snowflake(c, p):
    for k in range(6):
        a = math.radians(k * 60)
        ex, ey = C + math.cos(a) * 13, C + math.sin(a) * 13
        c.line([(C, C), (ex, ey)], p["hi"], 1.8)
        for t in (0.55, 0.78):
            bx, by = C + math.cos(a) * 13 * t, C + math.sin(a) * 13 * t
            for s in (-1, 1):
                b = a + s * 0.9
                c.line([(bx, by), (bx + math.cos(b) * 4, by + math.sin(b) * 4)], p["lt"], 1.3)
    c.ell(C, C, 2.2, 2.2, WHITE)


def bolt_e(c, p):
    pts = [(19, 2), (8, 17), (14, 17), (11, 30), (24, 12), (17, 12), (21, 2)]
    c.poly(pts, p["gl"], p["dk"], 0.8)
    c.poly([(18.5, 5), (11.5, 16), (16, 16), (14.5, 25), (21, 13), (16, 13)], mix(p["gl"], WHITE, 0.55))


def void_eye(c, p):
    c.poly([(2, C), (8, 8), (C, 6), (24, 8), (30, C), (24, 24), (C, 26), (8, 24)], p["md"], p["dk"], 0.8)
    c.ell(C, C, 6.5, 6.5, p["hi"], p["dk"], 0.6)
    c.ell(C, C, 3.4, 5, p["dk"])
    c.ell(C - 1.2, C - 1.6, 1.1, 1.1, WHITE)
    for k in range(-2, 3):
        c.line([(C + k * 5, 4), (C + k * 7, 0)], p["lt"], 1.1)
        c.line([(C + k * 5, 28), (C + k * 7, 32)], p["lt"], 1.1)


def leaf_e(c, p):
    leaf(c, 6, 26, math.radians(-50), 26, 14, p["lt"], p["hi"], p["dk"])
    c.line([(6, 26), (24, 6)], p["hi"], 1.0)
    for t in (0.35, 0.55, 0.75):
        bx, by = 6 + 18 * t, 26 - 20 * t
        c.line([(bx, by), (bx + 5, by + 1)], p["hi"], 0.7)
        c.line([(bx, by), (bx - 1, by - 5)], p["hi"], 0.7)


def wave_e(c, p):
    for k, y in enumerate((10, 17, 24)):
        pts = [(x, y + math.sin(x * 0.45 + k) * 2.6) for x in np.arange(3, 29.5, 0.7)]
        c.line(pts, p["hi"] if k != 1 else p["lt"], 2.6)
    pts = [(x, 6 + math.sin(x * 0.45 + 3) * 1.6) for x in np.arange(5, 27, 0.7)]
    c.line(pts, p["lt"], 1.2)


def blood_drop(c, p):
    drop(c, C, 17, 14, p["gl"], p["dk"])
    c.poly([(11, 15), (13, 11), (12, 19), (10.5, 19)], mix(p["gl"], WHITE, 0.6))


def gear_e(c, p):
    gear(c, C, C, 13, 10, p["lt"], p["dk"], p["dk"], 0.1)
    gear(c, C, C, 7, 8, p["hi"], p["dk"], p["dk"], 0.2)


def mirror_e(c, p):
    c.ell(C, 14, 10, 12, p["hi"], p["dk"], 0.9)
    c.ell(C, 14, 8, 10, mix(p["lt"], WHITE, 0.3))
    c.poly([(9, 8), (14, 5), (11, 20), (7, 17)], WHITE)
    c.poly([(C - 3, 26), (C + 3, 26), (C + 5, 31), (C - 5, 31)], p["md"], p["dk"], 0.5)


def root_e(c, p):
    c.line([(C, 4), (C, 28)], p["lt"], 3.0)
    rnd = random.Random(7)
    for s in (-1, 1):
        for y, ln in ((10, 9), (15, 11), (21, 8)):
            pts = [(C, y)]
            for k in range(1, 5):
                pts.append((pts[-1][0] + s * ln / 4, pts[-1][1] + 1.2 + rnd.random() * 1.5))
            c.line(pts, p["lt"], 1.6)
    c.ell(C, 5, 3.4, 3.4, p["gl"], p["dk"], 0.5)


def gate_e(c, p):
    arch = [(6, 29)] + [(C + math.cos(math.radians(a)) * 10, 14 + math.sin(math.radians(a)) * 11) for a in range(180, 361, 15)] + [(26, 29)]
    c.poly(arch, p["lt"], p["dk"], 0.8)
    inner = [(10, 29)] + [(C + math.cos(math.radians(a)) * 6, 14 + math.sin(math.radians(a)) * 7) for a in range(180, 361, 15)] + [(22, 29)]
    c.poly(inner, p["dk"])
    c.poly([(C - 2, 29), (C, 18), (C + 2, 29)], p["gl"])
    for k in range(7):
        c.ell(5 + k * 3.7, 4, 1.0, 1.0, p["hi"])


def horns_e(c, p):
    horn(c, 9, 24, 17, p["lt"], p["dk"], -1)
    horn(c, 23, 24, 17, p["lt"], p["dk"], 1)
    c.ell(C, 22, 6, 7, p["md"], p["dk"], 0.6)
    c.ell(13.5, 21, 1.3, 1.1, p["gl"])
    c.ell(18.5, 21, 1.3, 1.1, p["gl"])


def crown_horns(c, p):
    horn(c, 8, 26, 14, p["md"], p["dk"], -1)
    horn(c, 24, 26, 14, p["md"], p["dk"], 1)
    c.poly([(7, 26), (6, 12), (11, 18), (C, 8), (21, 18), (26, 12), (25, 26)], p["lt"], p["dk"], 0.8)
    for gx, gy in ((6, 12), (C, 8), (26, 12)):
        gem(c, gx, gy + 1, 2.0, p["gl"], mix(p["gl"], WHITE, 0.6), p["dk"])


def dragon_e(c, p):
    pts = [(4, 22), (8, 14), (6, 8), (12, 10), (18, 6), (26, 8), (29, 14), (24, 16), (27, 20), (20, 19), (14, 24), (9, 27)]
    c.poly(pts, p["lt"], p["dk"], 0.8)
    c.poly([(18, 6), (21, 2), (23, 7)], p["hi"], p["dk"], 0.5)
    c.poly([(12, 10), (11, 4), (15, 8)], p["hi"], p["dk"], 0.5)
    c.ell(21, 11, 1.4, 1.2, p["gl"])
    for xx in (24, 26, 28):
        c.poly([(xx, 15), (xx + 1, 17.5), (xx - 1, 17.5)], p["hi"])


def feather_e(c, p):
    leaf(c, 7, 27, math.radians(-50), 28, 12, p["hi"], p["lt"], p["dk"])
    c.line([(4, 30), (14, 18)], p["lt"], 1.4)
    for t in np.linspace(0.2, 0.8, 5):
        bx, by = 7 + 20 * t, 27 - 22 * t
        c.line([(bx, by), (bx - 4, by + 1)], p["lt"], 0.7)


def halo_e(c, p):
    c.d.ellipse([(C - 11) * 4, 4 * 4, (C + 11) * 4, 12 * 4], outline=p["hi"] + (255,), width=7)
    for k in range(-4, 5):
        a = math.radians(90 + k * 16)
        c.line([(C + math.cos(a) * 3, 16 + math.sin(a) * 3), (C + math.cos(a) * 14, 16 + math.sin(a) * 14)], p["lt"], 1.4)
    c.ell(C, 21, 5, 5, p["hi"], p["md"], 0.6)
    c.poly([(8, 31), (24, 31), (21, 25), (11, 25)], p["lt"], p["md"], 0.6)


def lich_e(c, p):
    skull(c, C, 16, 20, p["hi"], p["dk"])
    c.poly([(C - 6, 5), (C - 3, 1), (C, 4), (C + 3, 1), (C + 6, 5), (C + 6, 8), (C - 6, 8)], p["md"], p["dk"], 0.6)
    gem(c, C, 5.5, 2.0, p["gl"], mix(p["gl"], WHITE, 0.6), p["dk"])
    c.ell(12.5, 17, 1.7, 1.9, p["gl"])
    c.ell(19.5, 17, 1.7, 1.9, p["gl"])


def broken_crown_e(c, p):
    c.poly([(5, 24), (5, 10), (10, 16), (13, 6), (14.5, 24)], p["lt"], p["dk"], 0.8)
    c.poly([(18, 24), (19, 13), (23, 17), (27, 9), (27, 24)], shade(p["lt"], 0.85), p["dk"], 0.8)
    c.line([(14.5, 24), (16, 18), (17, 22), (18, 24)], p["dk"], 0.8)
    c.rect(5, 24, 27, 28, p["md"])
    c.ell(16, 12, 1.2, 1.2, p["gl"])


def ankh_e(c, p):
    c.d.ellipse([(C - 6) * 4, 2 * 4, (C + 6) * 4, 14 * 4], outline=p["lt"] + (255,), width=10)
    c.rect(C - 1.8, 13, C + 1.8, 30, p["lt"])
    c.rect(7, 14.5, 25, 18, p["lt"])
    c.ell(C, 8, 2.2, 2.4, p["ac"])
    c.line([(C - 0.4, 14), (C - 0.4, 29)], p["hi"], 0.6)


def plague_e(c, p):
    c.poly([(12, 4), (20, 4), (19, 12), (26, 26), (6, 26), (13, 12)], mix(p["md"], p["lt"], 0.3), p["dk"], 0.8)
    c.poly([(9, 21), (23, 21), (26, 26), (6, 26)], p["gl"])
    for bx, by, r in ((13, 17, 1.6), (18, 15, 1.2), (16, 11, 1.0)):
        c.ell(bx, by, r, r, p["hi"])
    c.rect(11.5, 2, 20.5, 5, p["hi"])


def triad_e(c, p):
    for k in range(3):
        a = math.radians(-90 + k * 120)
        pts = [(C + math.cos(a) * 14, 17 + math.sin(a) * 14), (C + math.cos(a + 2.1) * 6, 17 + math.sin(a + 2.1) * 6), (C + math.cos(a - 2.1) * 6, 17 + math.sin(a - 2.1) * 6)]
        c.poly(pts, [p["hi"], p["lt"], p["gl"]][k], p["dk"], 0.7)
    c.ell(C, 17, 3, 3, p["dk"])
    c.ell(C, 17, 1.5, 1.5, p["gl"])


def eclipse_e(c, p):
    for k in range(16):
        a = math.radians(k * 22.5)
        c.poly([(C + math.cos(a - 0.14) * 9, C + math.sin(a - 0.14) * 9), (C + math.cos(a) * 15, C + math.sin(a) * 15), (C + math.cos(a + 0.14) * 9, C + math.sin(a + 0.14) * 9)], p["lt"])
    c.ell(C, C, 9, 9, p["gl"])
    c.ell(C + 2.4, C - 1, 8.2, 8.2, p["dk"])


def sins_e(c, p):
    cols = [p["lt"], p["gl"], p["hi"], p["ac"], p["lt"], p["gl"], p["hi"]]
    for k in range(7):
        a = math.radians(-90 + k * 360 / 7)
        gem(c, C + math.cos(a) * 10, C + math.sin(a) * 10, 3.0, cols[k], mix(cols[k], WHITE, 0.55), p["dk"])
    c.ell(C, C, 3, 3, p["hi"], p["dk"], 0.5)


def spade_e(c, p):
    c.poly([(C, 3)] + [(C + math.cos(math.radians(a)) * 11, 17 + math.sin(math.radians(a)) * 9) for a in range(-10, 191, 12)], p["lt"], p["dk"], 0.8)
    c.poly([(C, 3), (5, 16), (5.5, 21), (14, 21), (12, 29), (20, 29), (18, 21), (26.5, 21), (27, 16)], p["lt"], p["dk"], 0.8)
    c.poly([(C, 8), (10, 17), (C, 22), (22, 17)], mix(p["gl"], p["md"], 0.3))


def knight_e(c, p):
    c.poly([(5, 5), (27, 5), (27, 17), (C, 29), (5, 17)], p["lt"], p["dk"], 0.9)
    c.poly([(8, 8), (24, 8), (24, 16), (C, 25), (8, 16)], p["md"])
    c.line([(C, 7), (C, 24)], p["hi"], 1.6)
    c.line([(8, 14), (24, 14)], p["hi"], 1.6)
    c.poly([(C - 2, 2), (C + 2, 2), (C, -1)], p["hi"])


def labyrinth_e(c, p):
    for k, r in enumerate((13, 9.5, 6)):
        c.d.arc([(C - r) * 4, (C - r) * 4, (C + r) * 4, (C + r) * 4], 30 + k * 70, 330 + k * 70, fill=p["lt"] + (255,), width=6)
    c.ell(C, C, 2, 2, p["gl"])
    horn(c, C - 2, 8, 6, p["hi"], p["dk"], -1)
    horn(c, C + 2, 8, 6, p["hi"], p["dk"], 1)


def crown_e(c, p):
    c.poly([(4, 24), (3, 8), (10, 15), (C, 4), (22, 15), (29, 8), (28, 24)], p["lt"], p["dk"], 0.8)
    c.rect(4, 24, 28, 28, p["md"])
    for gx, gy in ((3, 8), (C, 4), (29, 8)):
        gem(c, gx if gx != 3 else 4, gy + 1, 2.2, p["ac"], mix(p["ac"], WHITE, 0.6), p["dk"])
    c.ell(C, 19, 2.2, 2.2, p["gl"])


def scales_e(c, p):
    c.line([(5, 8), (27, 8)], p["lt"], 2.0)
    c.line([(C, 6), (C, 28)], p["lt"], 2.4)
    for s in (-1, 1):
        c.line([(C + s * 11, 8), (C + s * 14, 17)], p["hi"], 0.9)
        c.line([(C + s * 11, 8), (C + s * 8, 17)], p["hi"], 0.9)
        c.poly([(C + s * 8, 17), (C + s * 14, 17), (C + s * 11, 21)], p["lt"], p["dk"], 0.6)
    c.poly([(9, 29), (23, 29), (20, 26), (12, 26)], p["md"], p["dk"], 0.6)
    c.ell(C, 6, 2, 2, p["hi"])


def anvil_e(c, p):
    c.poly([(4, 9), (24, 9), (28, 12), (22, 15), (20, 19), (23, 25), (23, 28), (9, 28), (9, 25), (12, 19), (11, 15), (4, 12)], p["lt"], p["dk"], 0.9)
    c.line([(5, 10), (24, 10)], p["hi"], 1.0)
    flame(c, 27, 8, 6, 5, p["gl"], p["hi"])


def whirlpool_e(c, p):
    for rr, w in ((13, 2.4), (9.5, 2.0), (6, 1.8), (3, 1.4)):
        pts = []
        for t in np.linspace(0, 4.6, 40):
            r = rr * (1 - t / 6.5)
            pts.append((C + math.cos(t + rr) * r, C + math.sin(t + rr) * r))
        c.line(pts, p["hi"] if rr % 2 else p["lt"], w)
    c.ell(C, C, 1.6, 1.6, WHITE)


def key_e(c, p):
    c.d.ellipse([4 * 4, 3 * 4, 18 * 4, 17 * 4], outline=p["lt"] + (255,), width=10)
    c.ell(11, 10, 2.6, 2.6, p["gl"])
    c.rect(10, 16, 12.6, 30, p["lt"])
    for yy in (23, 27):
        c.rect(12.6, yy, 17, yy + 2.2, p["lt"])
    c.line([(10.8, 17), (10.8, 29)], p["hi"], 0.6)


def spirit_e(c, p):
    c.glow(lambda t: t.ell(C, C, 6, 6, WHITE), mix(p["gl"], WHITE, 0.3), 3, 1.0)
    c.ell(C, C, 6.4, 6.4, p["hi"], p["lt"], 0.7)
    for k in range(8):
        a = math.radians(k * 45)
        c.line([(C + math.cos(a) * 9, C + math.sin(a) * 9), (C + math.cos(a) * 14, C + math.sin(a) * 14)], p["lt"], 1.4)
    for k in range(4):
        a = math.radians(45 + k * 90)
        c.ell(C + math.cos(a) * 11, C + math.sin(a) * 11, 1.6, 1.6, p["hi"])


def rune_e(c, p):
    c.d.ellipse([3 * 4, 3 * 4, 29 * 4, 29 * 4], outline=p["lt"] + (255,), width=6)
    rune(c, C, C, 10, p["hi"], 4, 1.8)
    for k in range(6):
        a = math.radians(k * 60 + 30)
        rune(c, C + math.cos(a) * 11.5, C + math.sin(a) * 11.5, 1.8, p["lt"], k, 0.8)


EMBLEMS = [("wing", wing), ("thorn_crown", thorn_crown), ("skull", skull_e), ("flame", flame_e), ("snowflake", snowflake), ("bolt", bolt_e), ("void_eye", void_eye), ("leaf", leaf_e),
           ("wave", wave_e), ("blood_drop", blood_drop), ("gear", gear_e), ("mirror", mirror_e), ("root", root_e), ("gate", gate_e), ("horns", horns_e), ("crown_horns", crown_horns),
           ("dragon", dragon_e), ("feather", feather_e), ("halo", halo_e), ("lich", lich_e), ("broken_crown", broken_crown_e), ("ankh", ankh_e), ("plague", plague_e), ("triad", triad_e),
           ("eclipse", eclipse_e), ("sins", sins_e), ("spade", spade_e), ("knight", knight_e), ("labyrinth", labyrinth_e), ("crown", crown_e), ("scales", scales_e), ("anvil", anvil_e),
           ("whirlpool", whirlpool_e), ("key", key_e), ("spirit", spirit_e), ("rune", rune_e)]
NAMES = [n for n, _ in EMBLEMS]
COLS = 8
ROWS = (len(EMBLEMS) + COLS - 1) // COLS


def draw(name, pal):
    c = Canvas(32, 32)
    fn = dict(EMBLEMS)[name]
    fn(c, pal)
    return c.im.resize((32, 32), Image.BOX)

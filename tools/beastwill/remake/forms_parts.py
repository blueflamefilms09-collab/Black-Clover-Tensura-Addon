#!/usr/bin/env python3
"""Reusable Minecraft-detailed parts for the Beast Will forms: body, legs, arms, heads, ears, horns, tails, wings, weapons. Everything is cubes (stepped, angled, layered)."""
import math, os, sys
import numpy as np
sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from forms_lib import *
from forms_cube import *

X = lambda v: V(v, 0, 0)
RNG = lambda s: np.random.default_rng(s)


class Ctx:
    """sc: Scene, m: {role: Mat}, sp: spec dict, rng"""

    def __init__(s, sc, m, sp, seed=1):
        s.sc, s.m, s.sp, s.rng = sc, m, sp, np.random.default_rng(seed)


def cu(c, part, ctr, size, mat, **k): return c.sc.cube(part, ctr, size, c.m[mat], **k)
def sg(c, part, p0, p1, w, h, mat, **k): return c.sc.seg(part, p0, p1, w, h, c.m[mat], **k)


def cn(c, part, pts, ws, hs, mat, mat2=None, **k):
    return c.sc.chain(part, pts, ws, hs, c.m[mat], mat2=c.m[mat2] if mat2 else None, **k)


def curve(p0, p1, bend, n=5):
    """points along a quadratic curve p0 -> p1 pushed by bend at the middle"""
    p0, p1, b = V(*p0), V(*p1), V(*bend)
    mid = (p0 + p1) / 2 + b
    return [(1 - t) ** 2 * p0 + 2 * t * (1 - t) * mid + t * t * p1 for t in np.linspace(0, 1, n)]


def taper(a, b, n): return list(np.linspace(a, b, n))


# ====================================================================================================================== body
def torso(c, bulk=1.0, belly="belly", back="back", cloak=False):
    sp = c.sp
    cu(c, "body", (0, 18, 0), (9.0 * bulk, 12.2, 4.8 * bulk), "body")
    cu(c, "body", (0, 17.2, -2.5 * bulk), (6.4 * bulk, 9.0, .9), belly)
    for i in range(3): cu(c, "body", (0, 20.2 - i * 2.4, -3.0 * bulk), ((5.6 - i * .5) * bulk, 1.7, .7), belly, dt=1 if i % 2 else 0)
    cu(c, "body", (0, 18.4, 2.55 * bulk), (8.2 * bulk, 10.6, .8), back)


def belt_and_cloth(c, cloth="cloth", w=9.6, front_cloth=True):
    cu(c, "body", (0, 12.6, 0), (w, 1.8, 5.2), "leather")
    cu(c, "body", (0, 12.6, -2.8), (2.6, 2.0, .7), "plate")
    cu(c, "body", (0, 12.6, -3.2), (1.0, 1.0, .5), "glow")
    if front_cloth:
        c.sc.def_bone("clothF", (0, 11.8, -2.7), None, [("rot", 0, 5, 2, 0.0)])
        c.sc.def_bone("clothB", (0, 11.8, 2.7), None, [("rot", 0, -4, 2, 0.35)])
        for i, x in enumerate((-3.4, -1.1, 1.1, 3.4)):
            cu(c, "body", (x, 9.6 - (i % 2) * .6, -2.75), (2.1, 4.4 - (i % 2) * 1.2, .8), cloth, dt=(i % 2), bone="clothF")
        cu(c, "body", (0, 9.6, 2.7), (5.6, 4.6, .8), cloth, dt=-1, bone="clothB")


def gorget(c, w=9.6):
    cu(c, "body", (0, 24.2, 0), (w, 1.6, 5.4), "plate")
    cu(c, "body", (0, 25.0, -.2), (w - 1.2, 1.0, 4.6), "plate", dt=1)


def emblem(c, y=19.8, kind="sun"):
    cu(c, "body", (0, y, -3.3), (2.4, 2.4, .8), "plate", rot=(0, 0, 45))
    cu(c, "body", (0, y, -3.4), (1.0, 1.0, 1.0), "glow")
    if kind == "sun":
        for k in range(8):
            a = math.radians(k * 45)
            cu(c, "body", (math.sin(a) * 2.5, y + math.cos(a) * 2.5, -3.2), (.6, 1.4, .6), "plate", rot=(0, 0, -k * 45), dt=1)


def spine_ridge(c, n=5, size=2.2, mat="plate", y0=23.0, dy=2.0, z=2.9, lean=.7):
    for i in range(n):
        sg(c, "body", (0, y0 - i * dy, z), (0, y0 - i * dy + size * .5, z + size * lean), 1.4, 1.2, mat, pad=.3)


def scale_plates(c, mat="plate", n=3):
    for i in range(n):
        cu(c, "body", (0, 22.0 - i * 3.2, 2.9), (6.6 - i * .5, 2.6, .9), mat, dt=i % 2)


# ====================================================================================================================== arms
def arm(c, side, pn, upper="body", sleeve="fur", pauldron=True, claws=3, claw_len=2.4, paw="body", bracer=True, tuft=False, spiked=False, fin=False):
    x = side * 6.0
    if pauldron:
        cu(c, pn, (x, 22.4, 0), (6.8, 2.2, 6.6), "plate")
        cu(c, pn, (x, 21.0, 0), (6.2, 1.2, 6.0), "leather")
        cu(c, pn, (x, 23.8, 0), (5.6, 1.4, 5.4), "plate", dt=1)
        if spiked:
            sg(c, pn, (x + side * 1.0, 24.4, 0), (x + side * 2.8, 27.8, .2), 1.8, 1.8, "plate", dt=1, pad=.3)
        cu(c, pn, (x + side * .5, 24.7, -2.4), (1.2, 1.2, .6), "glow", rot=(0, 0, 45))
    cu(c, pn, (x, 18.0, 0), (5.0, 6.8, 5.0), upper)
    cu(c, pn, (x, 18.0, -2.55), (3.6, 5.8, .6), "belly")
    if tuft:
        for k in range(3): sg(c, pn, (x + side * 2.4, 19.5 - k * 2, 0), (x + side * 3.6, 16.5 - k * 2.2, .6), 2.0, 1.4, upper if k % 2 else "back", dt=-1)
    if bracer:
        cu(c, pn, (x, 12.9, 0), (5.2, 4.6, 5.2), "plate")
        cu(c, pn, (x, 14.5, 0), (5.6, .9, 5.6), "plate", dt=1)
        cu(c, pn, (x, 11.3, 0), (5.6, .9, 5.6), "leather")
    else:
        cu(c, pn, (x, 12.9, 0), (4.8, 4.6, 4.8), upper)
    cu(c, pn, (x, 9.5, -.2), (4.8, 3.4, 4.8), paw)
    for k in range(claws):
        o = (k - (claws - 1) / 2)
        sg(c, pn, (x + o * 1.3, 8.4, -2.2), (x + o * 1.5, 8.4 - claw_len * .8, -3.1 - claw_len * .3), .8, .9, "claw", pad=.3)
        cu(c, pn, (x + o * 1.55, 8.4 - claw_len * .8 - .6, -3.3 - claw_len * .3), (.6, .8, .6), "claw")
    if fin:
        for k in range(3): sg(c, pn, (x + side * 2.6, 17.5 - k * 2.6, .4), (x + side * 5.6, 16.0 - k * 2.9, 1.2), 1.0, 2.6, "plate", dt=1 - k % 2, pad=.2)


def leg_straight(c, side, pn, thigh="body", toes=3):
    x = side * 2.0
    cu(c, pn, (x, 9.4, 0), (4.8, 5.4, 5.0), thigh)
    cu(c, pn, (x, 9.4, -2.6), (3.2, 4.4, .5), "belly")
    cu(c, pn, (x, 6.4, 0), (4.6, 1.0, 4.9), "plate")
    cu(c, pn, (x, 4.0, .2), (4.4, 4.6, 4.6), "plate")
    cu(c, pn, (x, 6.2, -2.4), (3.4, 1.4, .6), "plate", dt=1)
    cu(c, pn, (x, 1.0, -1.8), (4.4, 2.0, 6.6), thigh)
    for k in range(toes): sg(c, pn, (x + (k - (toes - 1) / 2) * 1.3, .6, -5.0), (x + (k - (toes - 1) / 2) * 1.4, .4, -6.5), .8, .8, "claw", pad=.3)


def leg_digi(c, side, pn, thigh="body", hoof=False, toes=3, tall=1.0):
    """digitigrade: the thigh slants forward, the shin back, a long foot; it all swings with the player's leg"""
    x = side * 2.0
    cu(c, pn, (x, 7.4, 0), (4.6, 10.2, 4.6), thigh)                                      # the sleeve over the real leg
    cu(c, pn, (x, 12.2, 0), (4.8, 1.6, 4.8), "leather")
    sg(c, pn, (x, 13.0, .6), (x, 8.0, -2.2), 4.8, 5.0, thigh, pad=1.0)
    cu(c, pn, (x, 10.4, -3.0), (3.2, 3.6, .6), "belly", rot=(-30, 0, 0))
    sg(c, pn, (x, 8.0, -2.2), (x, 2.6, 1.6), 3.4, 3.6, "plate", pad=.8)
    sg(c, pn, (x, 6.8, -1.2), (x, 4.2, 0.4), 4.0, 4.0, "plate", dt=1, pad=0)
    cu(c, pn, (x, 7.8, -3.4), (3.2, 1.6, 1.0), "plate", dt=1)
    if hoof:
        cu(c, pn, (x, 1.1, -1.4), (4.2, 2.2, 5.4), "dark")
        cu(c, pn, (x, .6, -3.4), (4.0, 1.2, 2.6), "claw")
    else:
        cu(c, pn, (x, 1.0, -1.4), (3.8, 2.0, 6.0), thigh)
        cu(c, pn, (x, .8, -3.6), (3.6, 1.5, 2.6), "belly")
        for k in range(toes): sg(c, pn, (x + (k - (toes - 1) / 2) * 1.2, .6, -4.8), (x + (k - (toes - 1) / 2) * 1.3, .4, -6.4), .7, .8, "claw", pad=.3)
    sg(c, pn, (x, 4.0, 2.2), (x, 1.8, 3.8), .9, 1.0, "claw", pad=.2)


# ====================================================================================================================== ears, horns
def ears(c, kind, H, sc_=1.0, mat="body", inner="dark"):
    for s in (-1, 1):
        x = s * 3.9
        eb = "ear" + ("L" if s > 0 else "R")
        c.sc.def_bone(eb, H + V(s * 3.0, 4.4, .4), None, [("rot", 2, 7 * s, 4, 0.0 if s > 0 else .5), ("rot", 0, 4, 2, .3)])
        c.sc.cur = eb
        if kind == "round":
            cu(c, "head", H + V(x, 5.0, .2), (2.6, 2.4, 1.6), mat); cu(c, "head", H + V(x, 5.0, -.7), (1.6, 1.4, .5), inner)
        elif kind in ("pointed", "fox", "long"):
            L = {"pointed": 5.0, "fox": 6.6, "long": 7.4}[kind]; w = {"pointed": 3.2, "fox": 3.8, "long": 2.6}[kind]
            lean = {"pointed": 1.0, "fox": 1.8, "long": 2.4}[kind]; back = 1.8 if kind == "long" else 0.0
            for k in range(5):
                t = k / 4
                cu(c, "head", H + V(s * (3.0 + t * lean), 4.4 + t * L, .4 + t * back), (w * (1 - t * .88) + .5, L / 5 + .8, 1.5 - t * .5), "dark" if (k == 4 and kind == "fox") else mat, dt=-1 if k == 0 else (1 if k == 4 else 0))
            cu(c, "head", H + V(s * (3.1 + .3 * lean), 5.2 + L * .28, -.45), (w * .48, L * .46, .5), inner)
        elif kind == "horse":
            for k in range(3): cu(c, "head", H + V(s * (3.0 + k * .2), 4.6 + k * 1.5, 1.0 - k * .3), (1.9 - k * .5, 2.0, 1.2), mat)
        elif kind == "fin":
            for k in range(3): sg(c, "head", H + V(s * 4.4, 1.0 + k * 1.6, 0), H + V(s * 7.0, 2.4 + k * 1.8, .8), 1.0, 1.4, "plate", dt=k % 2, pad=.2)
        c.sc.cur = "root"


def horns(c, kind, H, mat="claw", scale=1.0):
    for s in (-1, 1):
        if kind == "swept":
            pts = curve(H + V(s * 2.8, 4.5, .6), H + V(s * 5.6, 8.6, 8.0), V(s * 1.0, 5.0, 1.0), 6)
            cn(c, "head", pts, taper(2.2, .5, 6), taper(2.2, .5, 6), mat, up=(0, 1, 0), mat2="plate")
        elif kind == "bull":
            pts = curve(H + V(s * 4.0, 3.0, -.6), H + V(s * 7.2, 9.0, -3.4), V(s * 3.4, -1.0, 0), 6)
            cn(c, "head", pts, taper(2.4, .6, 6), taper(2.4, .6, 6), mat)
        elif kind == "ram":
            pts = [H + V(s * 3.4, 3.6, -.6), H + V(s * 6.0, 4.4, -.6), H + V(s * 7.0, 2.0, -.4), H + V(s * 6.0, .0, 0.2), H + V(s * 4.6, 0.6, 0.8)]
            cn(c, "head", pts, taper(2.6, 1.2, 5), taper(2.4, 1.2, 5), mat)
        elif kind == "crest":
            for k in range(3): sg(c, "head", H + V(s * (1.0 + k * 1.2), 4.4, -2.0 + k * 1.6), H + V(s * (1.6 + k * 1.5), 7.6 - k * .6, -.6 + k * 2.2), 1.2, 1.4, mat, dt=k % 2, pad=.3)
        elif kind == "antler":
            cn(c, "head", [H + V(s * 2.4, 4.5, 0), H + V(s * 3.4, 7.6, .4), H + V(s * 5.0, 10.4, 1.0), H + V(s * 6.0, 13.0, 1.2)], [1.4, 1.2, 1.0], [1.4, 1.2, 1.0], mat)
            for t in (.33, .66):
                p = H + V(s * (2.4 + 3.6 * t), 4.5 + 8.5 * t, 1.0 * t)
                sg(c, "head", p, p + V(s * 2.6, 2.4, -1.4), .9, .9, mat, pad=.2)
        elif kind == "branch":
            cn(c, "head", [H + V(s * 2.2, 4.6, 0), H + V(s * 3.0, 7.4, .2), H + V(s * 4.4, 10.0, .6), H + V(s * 5.2, 12.4, .4)], [1.8, 1.5, 1.2], [1.8, 1.5, 1.2], "bark_" if False else mat)
            for t, d in ((.25, 1), (.5, -1), (.75, 1)):
                p = H + V(s * (2.2 + 3.0 * t), 4.6 + 7.8 * t, .5 * t)
                sg(c, "head", p, p + V(s * 3.0, 1.8, d * 2.0), 1.0, 1.0, mat, pad=.2)
                sg(c, "head", p + V(s * 3.0, 1.8, d * 2.0), p + V(s * 3.4, 3.8, d * 2.6), .8, .8, "leaf", pad=.2)


# ====================================================================================================================== tails
def tail(c, kind, base=(0, 13.0, 2.6), length=1.0):
    b = V(*base)
    if kind in ("whip", "thick", "spiked", "tuft", "curl", "serpent", "serpent2", "fluke", "bushy", "flame", "feather", "fox"):
        n = 10                                                       # more, smaller steps = a smoother curve (3.61.0)
        thick = 1.3 * {"whip": 1.8, "thick": 4.4, "spiked": 4.0, "tuft": 2.4, "curl": 2.4, "serpent": 3.0, "serpent2": 2.6, "fluke": 3.8, "bushy": 3.6, "flame": 3.0, "feather": 2.6, "fox": 3.2}[kind]
        L = {"whip": 22, "thick": 19, "spiked": 22, "tuft": 17, "curl": 15, "serpent": 22, "serpent2": 23, "fluke": 18, "bushy": 18, "flame": 18, "feather": 16, "fox": 17}[kind] * length
        droop = {"whip": 8, "thick": 5, "spiked": 6, "tuft": 6, "curl": -3, "serpent": 6, "serpent2": 4, "fluke": 1, "bushy": 3, "flame": 3, "feather": 4, "fox": 4}[kind]
        sides = (-1, 1) if kind == "serpent2" else (0,)
        for sd in sides:
            pts = [b + V(sd * 3.0 * (i / (n - 1)) * (1 if sd else 0), -droop * (i / (n - 1)) ** 1.4 + (math.sin(i / (n - 1) * math.pi) * 2.5 if kind in ("whip", "curl", "serpent") else 0) - (0), (i / (n - 1)) * L) for i in range(n)]
            ws = taper(thick, thick * (.5 if kind != "bushy" else 1.0), n)
            sfx = "" if not sd else ("L" if sd > 0 else "R")
            TB = [("tail", "tail2", "tail3")[min(2, i * 3 // (n - 1))] + sfx for i in range(n - 1)]
            T3 = "tail3" + sfx
            c.sc.def_bone("tail" + sfx, pts[0], None, [("rot", 1, 9, 2, 0.0), ("rot", 0, 3, 1, 0.0)])
            c.sc.def_bone("tail2" + sfx, pts[3], "tail" + sfx, [("rot", 1, 12, 2, 0.12), ("rot", 0, 4, 2, 0.1)])
            c.sc.def_bone("tail3" + sfx, pts[6], "tail2" + sfx, [("rot", 1, 16, 2, 0.24), ("rot", 0, 5, 2, 0.2)])
            cn(c, "back", pts, ws, ws, "body", mat2="back", bone=TB)
            end = pts[-1]
            if kind in ("spiked", "serpent", "fluke") and kind != "fluke":
                for qi, p in enumerate(pts[1:-1]): sg(c, "back", p + V(0, thick * .4, 0), p + V(0, thick * .4 + 2.6, -.6), 1.2, 1.2, "plate", bone=TB[min(qi + 1, n - 2)], pad=.2)
            if kind == "tuft":
                for k, (dx, dy, dz) in enumerate(((0, 0, 1), (1.2, .8, .8), (-1.2, .8, .8), (0, 1.4, .6), (.7, -.6, .9), (-.7, -.6, .9))):
                    sg(c, "back", end, end + V(dx, dy, dz) * 2.0, 1.7, 1.7, "mane" if k % 2 == 0 else "mane2", pad=.5, bone=T3)
            if kind == "bushy" or kind == "fox":
                for k in range(8):
                    a = k * math.tau / 8
                    sg(c, "back", end + V(0, 0, -3.0), end + V(math.cos(a) * 2.6, math.sin(a) * 2.6, 3.0), 2.2, 2.2, "belly" if k % 2 else "body", bone=T3, dt=(k % 3) - 1, pad=.4)
                for k in range(5):
                    p = pts[1 + k]
                    sg(c, "back", p, p + V((k - 2) * .9, 1.8, 1.0), 2.4, 2.4, "body", bone=TB[min(k + 1, n - 2)], dt=-1 + (k % 2), pad=.3)
            if kind == "flame":
                c.sc.def_bone("flame" + sfx, end + V(0, 0, 1.0), "tail3" + sfx, [("scale", 1, .12, 6, 0.0), ("rot", 2, 5, 5, 0.3)])
                for k, sz in enumerate((4.4, 3.4, 2.4)): cu(c, "back", end + V(0, k * 1.8, k * .6 + 1.0), (sz, sz, sz), "flame", bone="flame" + sfx, rot=(0, 0, 45 * k))
            if kind == "fluke":
                cu(c, "back", end + V(0, .4, 2.0), (11, .9, 4.4), "plate", bone=T3); cu(c, "back", end + V(0, .4, 4.4), (8, .9, 2.4), "plate", bone=T3, dt=-1)
                for s_ in (-1, 1): sg(c, "back", end + V(s_ * 3, .4, 1.0), end + V(s_ * 6.6, .8, 5.6), 2.2, .9, "plate", bone=T3, dt=1, pad=.3)
            if kind in ("serpent", "serpent2"):
                sg(c, "back", end, end + V(0, 0, 3.2), 3.4, 3.0, "body", bone=T3, pad=.4)
                cu(c, "back", end + V(0, .8, 3.6), (2.6, .6, .6), "glow", bone=T3)
                for s_ in (-1, 1): sg(c, "back", end + V(s_ * 1.0, -1, 3.0), end + V(s_ * 1.0, -1.8, 5.0), .6, .9, "claw", bone=T3, pad=.1)
            if kind == "feather":
                for k in range(7):
                    sg(c, "back", pts[3] + V(0, 0, 0), pts[-1] + V((k - 3) * 1.6, (k % 2) * .8, 3.0 + abs(k - 3) * -.4), 1.8, .8, "plate" if k % 2 else "body", bone=T3, dt=(k % 3) - 1, pad=.3)


# ====================================================================================================================== wings
def wing_membrane(c, side, span=22, rise=14, drop=15, fingers=4, bone_w=1.4, tattered=False, root=(3.4, 21.0, 2.8), name="wing"):
    s = side; r = V(root[0] * s, root[1], root[2])
    elbow = r + V(s * span * .38, rise * .6, 4.0)
    wrist = elbow + V(s * span * .34, rise * .32, 2.2)
    bn = f"{name}{'L' if s > 0 else 'R'}"
    c.sc.def_bone(bn, r, None, [("rot", 2, 14 * s, 3, 0.0), ("rot", 0, 3, 3, 0.25)])
    cn(c, "back", [r, elbow, wrist], [bone_w * 1.5, bone_w * 1.2, bone_w], [bone_w * 1.5, bone_w * 1.2, bone_w], "plate", bone=bn)
    tips = []
    for k in range(fingers):
        t = k / (fingers - 1)
        tip = wrist + V(s * (span * .3 * (1 - t * .35) - 1.0), (rise * .18) * (1 - t * 1.9) - t * drop * .7, 2.0 + t * 5.0)
        tips.append(tip)
        cn(c, "back", [wrist, tip], [bone_w * .8], [bone_w * .8], "plate", bone=bn, pad=.2)
    # membrane panels between the arm bone, the fingers and the body
    prev_a, prev_b = r, wrist
    rng = np.random.default_rng(int(abs(side) * 7 + span))
    for k, tip in enumerate(tips + [r + V(s * 1.0, -drop * .6, 2.0)]):
        a0, b0, a1, b1 = prev_a, prev_b, wrist if k < len(tips) else r, tip
        for j in range(5):
            u0, u1 = j / 5, (j + 1) / 5
            p = (a0 * (1 - u0) + b0 * u0, a1 * (1 - u0) + b1 * u0)
            q = (a0 * (1 - u1) + b0 * u1, a1 * (1 - u1) + b1 * u1)
            if tattered and rng.random() < .35 and j > 1: continue
            cen = (p[0] + p[1] + q[0] + q[1]) / 4
            right = (p[1] - p[0] + q[1] - q[0]) / 2
            up = (q[0] - p[0] + q[1] - p[1]) / 2
            cu(c, "back", cen, (max(np.linalg.norm(right), .6) + .4, .45, max(np.linalg.norm(up), .6) + .4), "membrane", rot=_frame(up, V(0, 1, 0)) if False else _plane(right, up), bone=bn, dt=(k + j) % 2)
        prev_a, prev_b = wrist, tip
        prev_a = wrist
    return tips


def _plane(right, up):
    """rotation matrix with local x = right, local z = up (a thin slab lying in that plane)"""
    x = unit(right); z = unit(up - x * np.dot(up, x)); y = np.cross(z, x)
    return np.stack([x, y, z], axis=1)


def wing_feather(c, side, n=9, span=20, root=(3.4, 21.0, 2.8), layers=3, name="wing"):
    s = side; r = V(root[0] * s, root[1], root[2]); bn = f"{name}{'L' if s > 0 else 'R'}"
    c.sc.def_bone(bn, r, None, [("rot", 2, 12 * s, 3, 0.0), ("rot", 0, 3, 3, 0.25)])
    A = [r, r + V(s * 5, 5, 2.2), r + V(s * 11, 9, 3.4)]
    cn(c, "back", A, [2.4, 2.0], [2.4, 2.0], "body", bone=bn)
    for L in range(layers):
        for i in range(n):
            t = i / (n - 1)
            a = math.radians(-38 + t * 100)
            base = A[0] * (1 - t) + A[2] * t + V(0, -.6 * L, .5 * L)
            ln = (8 + 12 * t) * (1 - L * .26) * (span / 20.0)
            tip = base + V(s * math.cos(a) * ln, math.sin(a) * ln, 2.6 + L * .6)
            sg(c, "back", base, tip, 2.8 - L * .3, .8, "plate" if (i + L) % 2 else "feather", bone=bn, dt=(i % 3) - 1 + (L > 1), pad=.4)
            sg(c, "back", base + (tip - base) * .15, tip, .6, 1.0, "feather2", bone=bn, pad=.1)


# ====================================================================================================================== mane / crest
def mane_ring(c, part, mat_a, mat_b, cy, r, drop, out, n, a0, a1, w=2.6, t=1.8, seed=0, bone=None):
    rng = np.random.default_rng(seed)
    if bone: c.sc.def_bone(bone, (0, cy + 1.5, 0), None, [("rot", 0, 3.5, 2, seed * .21), ("rot", 2, 3.0, 2, seed * .21 + .25)])
    for i in range(n):
        a = math.radians(a0 + (a1 - a0) * i / max(1, n - 1))
        d = V(math.sin(a), 0, -math.cos(a))
        rr = r * (1 + rng.uniform(-.06, .06))
        base = V(0, cy + rng.uniform(-.5, .5), 0) + d * V(rr, 1, rr)
        tip = base + d * out * rng.uniform(.8, 1.2) + V(0, -drop * rng.uniform(.75, 1.25), 0)
        sg(c, part, base, tip, w * rng.uniform(.85, 1.15), t, mat_a if i % 2 == 0 else mat_b, pad=.6, dt=int(rng.integers(-1, 2)), bone=bone)

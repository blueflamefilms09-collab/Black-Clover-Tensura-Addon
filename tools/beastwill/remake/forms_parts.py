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
def torso(c, bulk=1.0, belly="belly", back="back", cloak=False, chest="belly"):
    """3.76.0: a fitted, V-shaped trunk instead of one inflated box: a dark under-layer hugging the player's body, a broad chest piece over the upper half, two angled pectoral plates,
    abdominal bands that narrow towards the belt, and dark flanks, so the waist reads narrow and the shoulders wide. `chest` = the material of the pectorals (plate for an armoured will)."""
    cu(c, "body", (0, 18, 0), (8.5, 12.2, 4.5), back)                                             # the under-layer: flanks and waist stay dark
    cu(c, "body", (0, 21.3, -.1), (9.3 * bulk, 5.2, 5.1 * bulk), "body")                           # the chest block, shoulders to the bottom of the ribs
    for s_ in (-1, 1):
        cu(c, "body", (s_ * 2.25, 21.3, -2.7 * bulk), (4.1, 3.8, .9), chest, rot=(0, 0, s_ * -7), dt=1 if chest != "plate" else 0)   # pectorals, tilted to a V
    for i, (y, w) in enumerate(((17.5, 5.6), (15.7, 4.8), (14.0, 4.0))):
        cu(c, "body", (0, y, -2.45), (w * bulk, 1.5, .7), belly, dt=(i % 2))                        # abdominal bands, narrowing down
    cu(c, "body", (0, 20.6, 2.55 * bulk), (8.6 * bulk, 6.4, .9), "body", dt=-1)                    # shoulder blades
    cu(c, "body", (0, 15.6, 2.4), (5.4, 4.6, .7), back, dt=-1)                                     # the small of the back


def belt_and_cloth(c, cloth="cloth", w=9.0, front_cloth=True):
    cu(c, "body", (0, 12.6, 0), (w, 1.8, 5.0), "leather")
    cu(c, "body", (0, 12.6, -2.7), (2.6, 2.2, .7), "gold")
    cu(c, "body", (0, 12.6, -3.1), (1.0, 1.0, .5), "glow")
    if front_cloth:
        c.sc.def_bone("clothF", (0, 11.8, -2.7), None, [("rot", 0, 5, 2, 0.0)])
        c.sc.def_bone("clothB", (0, 11.8, 2.7), None, [("rot", 0, -4, 2, 0.35)])
        for i, (x, ln, wd) in enumerate(((-3.3, 3.4, 1.9), (0, 6.6, 2.8), (3.3, 3.4, 1.9))):         # a long centre tabard between two short side tassets (was four equal flaps)
            cu(c, "body", (x, 11.7 - ln / 2, -2.75), (wd, ln, .8), cloth, dt=0 if i == 1 else -1, bone="clothF")
        cu(c, "body", (0, 5.9, -2.8), (2.8, 1.0, .9), "gold", bone="clothF")
        cu(c, "body", (0, 9.2, 2.7), (5.4, 5.4, .8), cloth, dt=-1, bone="clothB")


def gorget(c, w=9.4, kind="plate"):
    """the collar: plate (a stepped metal ring), ruff (a ring of fur tufts for the wolves, lions and foxes), scale (overlapping scale bands) or none"""
    if kind == "plate":
        cu(c, "body", (0, 24.2, 0), (w, 1.5, 5.3), "plate")
        cu(c, "body", (0, 25.0, -.2), (w - 1.6, 1.0, 4.5), "gold")
    elif kind == "scale":
        cu(c, "body", (0, 24.2, 0), (w - .2, 1.4, 5.3), "back")
        for i in range(3): cu(c, "body", ((i - 1) * 2.9, 24.3, -2.75), (2.6, 1.9, .7), "plate", rot=(0, 0, 45), dt=i % 2)
    elif kind == "ruff":
        cu(c, "body", (0, 24.2, 0), (w - .4, 1.4, 5.2), "back")
        for i in range(7):
            a = math.radians(-78 + i * 26)
            d = V(math.sin(a), 0, -math.cos(a))
            base = V(0, 24.6, 0) + d * V(4.2, 1, 2.6)
            sg(c, "body", base, base + d * 1.5 + V(0, -2.6 - (i % 2) * .8, 0), 2.6, 1.6, "mane" if i % 2 == 0 else "belly", pad=.5, dt=(i % 3) - 1)


def emblem(c, y=19.9, kind="gem"):
    """the mark between the pectorals, per beast family: sun (rays), fang (two fangs round a gem), chevron (stacked scale chevrons), eye (a slit eye), leaf, or the plain gem"""
    z = -3.35
    if kind == "sun":
        cu(c, "body", (0, y, z), (2.2, 2.2, .8), "gold", rot=(0, 0, 45))
        cu(c, "body", (0, y, z - .15), (1.0, 1.0, .9), "glow")
        for k in range(8):
            a = math.radians(k * 45)
            cu(c, "body", (math.sin(a) * 2.4, y + math.cos(a) * 2.4, z + .1), (.6, 1.3, .6), "gold", rot=(0, 0, -k * 45), dt=1)
    elif kind == "fang":
        cu(c, "body", (0, y + .3, z), (1.3, 1.3, .8), "glow", rot=(0, 0, 45))
        for s_ in (-1, 1): sg(c, "body", (s_ * 1.5, y + 1.6, z), (s_ * .5, y - 2.2, z - .2), 1.1, .8, "claw", pad=.2)
    elif kind == "chevron":
        for i in range(3):
            for s_ in (-1, 1): cu(c, "body", (s_ * (1.15 - i * .12), y + 1.6 - i * 1.5, z + .05), (2.5 - i * .5, .8, .7), "gold" if i == 1 else "plate", rot=(0, 0, s_ * 32), dt=1)
        cu(c, "body", (0, y - 2.2, z - .1), (.9, .9, .8), "glow", rot=(0, 0, 45))
    elif kind == "eye":
        cu(c, "body", (0, y, z), (3.6, 1.6, .7), "dark")
        cu(c, "body", (0, y, z - .15), (2.4, 1.0, .7), "glow")
        cu(c, "body", (0, y, z - .3), (.6, 1.4, .6), "dark")
    elif kind == "leaf":
        for k, a in enumerate((-40, 0, 40)): sg(c, "body", (0, y - 1.4, z), (math.sin(math.radians(a)) * 2.6, y - 1.4 + math.cos(math.radians(a)) * 2.8, z - .1), 1.5, .7, "leaf", pad=.2, dt=k % 2)
        cu(c, "body", (0, y - 1.4, z - .2), (1.0, 1.0, .8), "glow", rot=(0, 0, 45))
    else:
        cu(c, "body", (0, y, z), (2.4, 2.4, .8), "gold", rot=(0, 0, 45))
        cu(c, "body", (0, y, z - .15), (1.1, 1.1, .9), "glow", rot=(0, 0, 45))


def spine_ridge(c, n=5, size=2.2, mat="plate", y0=23.0, dy=2.0, z=2.9, lean=.7):
    for i in range(n):
        sg(c, "body", (0, y0 - i * dy, z), (0, y0 - i * dy + size * .5, z + size * lean), 1.4, 1.2, mat, pad=.3)


def scale_plates(c, mat="plate", n=3):
    for i in range(n):
        cu(c, "body", (0, 22.0 - i * 3.2, 2.9), (6.6 - i * .5, 2.6, .9), mat, dt=i % 2)


# ====================================================================================================================== arms
def pauldron(c, side, pn, kind="slab"):
    """3.76.0: the shoulder piece, per beast family (every will used to wear the same slab and gem). At most 5 cubes."""
    x = side * 6.0; o = side
    if kind == "none": return
    if kind == "sleek":                                                    # one low plate and a trim line: the fast, lean wills
        cu(c, pn, (x + o * .2, 23.6, 0), (5.4, 1.6, 5.4), "plate")
        cu(c, pn, (x + o * 1.6, 23.2, 0), (2.2, .9, 5.8), "gold", rot=(0, 0, o * -18))
        return
    if kind == "fur":                                                      # a shaggy shoulder: a fur lump and tufts standing out and back
        cu(c, pn, (x, 23.5, 0), (6.2, 2.2, 6.0), "mane")
        for k, (dz, up) in enumerate(((-1.8, 1.4), (0, 2.4), (1.8, 1.6))):
            sg(c, pn, (x + o * 1.4, 24.0, dz), (x + o * 4.2, 24.6 + up, dz * 1.4 + .4), 2.2, 1.7, "mane2" if k % 2 else "mane", pad=.5, dt=k % 2)
        return
    if kind == "round":                                                    # a heavy boulder of a shoulder with a rim: the bulls and boars
        cu(c, pn, (x + o * .3, 23.7, 0), (7.4, 3.0, 6.8), "plate")
        cu(c, pn, (x + o * .7, 25.5, 0), (5.2, 1.3, 4.8), "plate", dt=1)
        cu(c, pn, (x + o * .3, 22.1, 0), (7.6, .7, 7.0), "gold")
        cu(c, pn, (x + o * 1.2, 26.2, 0), (1.6, 1.2, 1.6), "gold", rot=(0, 45, 0))
        return
    if kind == "scale":                                                    # three overlapping plates stepping outward and down
        for k in range(3):
            cu(c, pn, (x + o * (k * 1.0 - .4), 24.4 - k * 1.1, 0), (4.2, 1.3, 6.4 - k * .5), "plate", rot=(0, 0, o * (-10 - k * 9)), dt=1 - k % 2)
        cu(c, pn, (x - o * .6, 25.2, -2.6), (1.1, 1.1, .6), "glow", rot=(0, 0, 45))
        return
    if kind == "fin":                                                      # a plate and two fin blades sweeping up and back: the sea wills
        cu(c, pn, (x, 23.5, 0), (6.4, 1.9, 6.0), "plate")
        for k in range(2): sg(c, pn, (x + o * (.6 + k * 1.6), 24.2, -.6 + k * 1.2), (x + o * (2.6 + k * 2.2), 27.4 - k * .9, 1.6 + k * 1.4), 1.0, 3.0, "gold" if k else "membrane", pad=.3, dt=k)
        return
    if kind == "feather":                                                  # layered feather blades lying back over the shoulder
        cu(c, pn, (x, 23.5, 0), (6.0, 1.7, 5.8), "plate")
        for k in range(3): sg(c, pn, (x + o * (k - 1) * 1.5, 24.4, -1.6), (x + o * ((k - 1) * 2.0 + 1.2), 25.2 + (1 - abs(k - 1)) * .9, 4.2), 2.2, .7, "feather" if k % 2 == 0 else "belly", pad=.3, dt=k % 2)
        return
    if kind == "bone":                                                     # ribs of bone over a dark pad
        cu(c, pn, (x, 23.3, 0), (6.2, 1.8, 5.8), "dark")
        for k in range(3): sg(c, pn, (x - o * 2.2, 24.4, (k - 1) * 2.0), (x + o * 3.6, 23.4 + k * .2, (k - 1) * 2.4), 1.2, 1.2, "claw", pad=.3, dt=k % 2)
        return
    cu(c, pn, (x, 23.4, 0), (6.6, 2.0, 6.2), "plate")                      # slab (and spike): the knightly plate
    cu(c, pn, (x + o * .3, 24.7, 0), (5.2, 1.2, 5.0), "plate", dt=1)
    cu(c, pn, (x, 22.2, 0), (6.9, .6, 6.5), "gold")
    if kind == "spike":
        sg(c, pn, (x + o * 1.0, 25.0, 0), (x + o * 3.4, 29.4, .4), 2.0, 2.0, "claw", dt=1, pad=.3)
        sg(c, pn, (x + o * 2.4, 24.4, 1.4), (x + o * 4.6, 26.6, 2.6), 1.3, 1.3, "claw", pad=.2)
    else:
        cu(c, pn, (x + o * .5, 25.0, -2.4), (1.2, 1.2, .6), "glow", rot=(0, 0, 45))


def arm(c, side, pn, upper="body", sleeve="fur", pauldron_kind="slab", claws=3, claw_len=2.0, paw="body", bracer=True, tuft=False, spiked=False, fin=False):
    """3.62.0 arm: the same length as the player's own (shoulder guard at 24.8, hand at 10..13, the claws end near 8.6). 3.76.0: tapered (a slimmer upper arm, a dark elbow joint, a flared, trimmed
    forearm guard) and the shoulder piece is chosen per beast family ({@link pauldron})."""
    x = side * 6.0
    o = side
    pauldron(c, side, pn, "spike" if (spiked and pauldron_kind == "slab") else pauldron_kind)
    cu(c, pn, (x, 19.6, 0), (4.7, 4.6, 4.7), upper)                        # upper arm
    cu(c, pn, (x, 16.7, 0), (4.4, 1.6, 4.4), "dark")                       # the elbow joint, dark and narrow
    cu(c, pn, (x + o * 2.15, 20.0, 0), (1.0, 3.0, 3.4), "plate", dt=1, rot=(0, 0, o * 10))   # a plate on the outer side
    cu(c, pn, (x, 17.0, 2.4), (1.8, 1.8, 1.4), "gold", rot=(0, 0, 45))      # elbow guard
    if tuft:
        c.sc.def_bone("furA" + pn, (x + o * 2.4, 19.6, 0), None, [("rot", 2, 6 * o, 2, 0.0 if o > 0 else .4), ("rot", 0, 4, 2, .2)])
        for k in range(3): sg(c, pn, (x + o * 2.4, 19.5 - k * 1.8, 0), (x + o * 3.8, 17.0 - k * 2.0, .6), 2.0, 1.4, upper if k % 2 else "back", dt=-1, bone="furA" + pn)
    if bracer:
        cu(c, pn, (x, 14.3, 0), (5.3, 3.6, 5.3), "plate")
        cu(c, pn, (x, 16.0, 0), (5.9, .8, 5.9), "gold")
        cu(c, pn, (x, 12.5, 0), (5.7, .8, 5.7), "leather")
    else:
        cu(c, pn, (x, 14.4, 0), (4.8, 3.6, 4.8), upper)
    cu(c, pn, (x, 11.2, -.1), (4.6, 2.4, 4.6), paw)                          # hand
    for k in range(2):
        cu(c, pn, (x + (k - .5) * 2.2, 12.0, -2.5), (1.4, 1.0, .8), "plate", dt=1)   # knuckle plates
    for k in range(claws):
        q = (k - (claws - 1) / 2)
        sg(c, pn, (x + q * 1.3, 10.4, -2.0), (x + q * 1.45, 9.2, -3.0), .8, .9, "claw", pad=.3)
        sg(c, pn, (x + q * 1.45, 9.2, -3.0), (x + q * 1.5, 8.5 - claw_len * .15, -2.7), .55, .6, "claw", pad=.15)
    if fin:
        for k in range(3): sg(c, pn, (x + side * 2.6, 17.5 - k * 2.2, .4), (x + side * 5.4, 16.2 - k * 2.4, 1.2), 1.0, 2.2, "membrane", dt=1 - k % 2, pad=.2)


def leg_straight(c, side, pn, thigh="body", toes=3):
    x = side * 2.0
    cu(c, pn, (x, 9.6, 0), (4.6, 4.8, 4.7), thigh)
    cu(c, pn, (x, 9.6, -2.45), (2.8, 3.6, .5), "plate", dt=1)                # thigh guard
    cu(c, pn, (x, 6.6, 0), (4.4, 1.2, 4.4), "dark")                         # the knee joint, dark and narrow
    cu(c, pn, (x, 3.9, .2), (4.9, 4.4, 4.9), "plate")                       # greave, flared
    cu(c, pn, (x, 6.5, -2.5), (2.8, 1.4, .8), "gold")                       # knee cap
    cu(c, pn, (x, .9, -1.2), (4.2, 1.8, 5.2), thigh)                         # 3.77.0: a shorter, lower foot
    for k in range(toes): sg(c, pn, (x + (k - (toes - 1) / 2) * 1.3, .6, -3.8), (x + (k - (toes - 1) / 2) * 1.4, .4, -5.3), .8, .8, "claw", pad=.3)


def leg_digi(c, side, pn, thigh="body", hoof=False, toes=3, tall=1.0):
    """digitigrade: the thigh slants forward, the shin back, a long foot; it all swings with the player's leg"""
    x = side * 2.0
    cu(c, pn, (x, 7.4, 0), (4.6, 10.2, 4.6), thigh)                                      # the sleeve over the real leg
    cu(c, pn, (x, 12.2, 0), (4.8, 1.6, 4.8), "leather")
    sg(c, pn, (x, 13.0, .6), (x, 8.0, -2.2), 4.8, 5.0, thigh, pad=1.0)
    cu(c, pn, (x, 10.4, -3.0), (3.2, 3.6, .6), "belly", rot=(-30, 0, 0))
    sg(c, pn, (x, 8.0, -2.2), (x, 2.6, 1.6), 3.4, 3.6, "back", pad=.8)
    sg(c, pn, (x, 6.8, -1.2), (x, 4.2, 0.4), 4.2, 4.2, "plate", dt=1, pad=0)
    cu(c, pn, (x, 7.9, -3.4), (2.6, 1.3, .9), "gold")
    if hoof:
        cu(c, pn, (x, 1.1, -1.0), (4.0, 2.2, 4.4), "dark")
        cu(c, pn, (x, .6, -2.6), (3.8, 1.2, 2.2), "claw")
    else:
        cu(c, pn, (x, .9, -1.0), (3.6, 1.8, 4.8), thigh)                     # 3.77.0: a shorter paw
        cu(c, pn, (x, .8, -2.8), (3.4, 1.4, 2.2), "belly")
        for k in range(toes): sg(c, pn, (x + (k - (toes - 1) / 2) * 1.2, .6, -3.8), (x + (k - (toes - 1) / 2) * 1.3, .4, -5.3), .7, .8, "claw", pad=.3)
    cu(c, pn, (x, 3.4, 2.3), (2.8, 2.6, 1.8), "back", rot=(-25, 0, 0), dt=-1)       # 3.77.0: the hock, a joint standing out behind the heel
    sg(c, pn, (x, 4.0, 2.6), (x, 1.8, 4.2), .9, 1.0, "claw", pad=.2)


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
    k = scale; kx = 1 + (k - 1) * .5                                       # 3.76.0: `scale` works for the swept and bull horns: they grow up and back, and half as much outward
    for s in (-1, 1):
        if kind == "swept":
            pts = curve(H + V(s * 2.8, 4.5, .6), H + V(s * 5.6 * kx, 4.5 + 4.1 * k, .6 + 7.4 * k), V(s * 1.0, 5.0 * k, 1.0), 6)
            cn(c, "head", pts, taper(2.2 * kx, .5, 6), taper(2.2 * kx, .5, 6), mat, up=(0, 1, 0), mat2="gold")
        elif kind == "bull":
            pts = curve(H + V(s * 4.0, 3.0, -.6), H + V(s * (4.0 + 3.2 * kx), 3.0 + 6.0 * k, -.6 - 2.8 * k), V(s * 3.4 * k, -1.0, 0), 6)
            cn(c, "head", pts, taper(2.4 * kx, .6, 6), taper(2.4 * kx, .6, 6), mat)
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
        thick = 1.45 * {"whip": 1.8, "thick": 4.4, "spiked": 4.0, "tuft": 2.4, "curl": 2.4, "serpent": 3.0, "serpent2": 2.6, "fluke": 3.8, "bushy": 3.6, "flame": 3.0, "feather": 2.6, "fox": 3.2}[kind]
        L = {"whip": 22, "thick": 19, "spiked": 22, "tuft": 17, "curl": 15, "serpent": 22, "serpent2": 23, "fluke": 18, "bushy": 18, "flame": 18, "feather": 16, "fox": 17}[kind] * length
        droop = {"whip": 8, "thick": 5, "spiked": 6, "tuft": 6, "curl": -3, "serpent": 6, "serpent2": 4, "fluke": 1, "bushy": 3, "flame": 3, "feather": 4, "fox": 4}[kind]
        sides = (-1, 1) if kind == "serpent2" else ((-1, 0, 1) if kind == "fox" else (0,))      # 3.76.0: the kitsune fans three tails
        fan = 8.5 if kind == "fox" else 3.0
        for sd in sides:
            pts = [b + V(sd * fan * (i / (n - 1)) * (1 if sd else 0), -droop * (i / (n - 1)) ** 1.4 + (math.sin(i / (n - 1) * math.pi) * 2.5 if kind in ("whip", "curl", "serpent") else 0) - (0), (i / (n - 1)) * L) for i in range(n)]
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
SLAB = {2, 3}        # a thin slab only draws its two big faces (a third of the quads)


def _plane(right, up):
    """rotation matrix with local x = right, local z = up (a thin slab lying in that plane)"""
    x = unit(right); z = unit(up - x * np.dot(up, x)); y = np.cross(z, x)
    return np.stack([x, y, z], axis=1)


def _fan(c, O, A, B, bone, mat="membrane", cell=5.6, depth=.2, tattered=None, k0=0):
    """fills the triangle O, A, B with thin slabs in rows from O; the far edge is scalloped (cut in towards O between A and B), so a wing reads as stretched skin between its fingers"""
    O, A, B = V(*O), V(*A), V(*B)
    nu = max(2, int(round(max(np.linalg.norm(A - O), np.linalg.norm(B - O)) / cell)))
    nv = max(1, int(round(np.linalg.norm(B - A) / cell)))
    P = lambda u, v: O + u * ((A * (1 - v) + B * v) - O)
    for i in range(nu):
        for j in range(nv):
            u0, u1, v0, v1 = i / nu, (i + 1) / nu, j / nv, (j + 1) / nv
            vm = (v0 + v1) / 2
            umax = 1.0 - depth * math.sin(math.pi * vm)
            if (u0 + u1) / 2 > umax + .02: continue
            u1 = min(u1, umax)
            if u1 - u0 < .06: continue
            if tattered is not None and u1 > .55 and tattered.random() < .35: continue
            cen = (P(u0, v0) + P(u0, v1) + P(u1, v0) + P(u1, v1)) / 4
            right = (P(u0, v1) - P(u0, v0) + P(u1, v1) - P(u1, v0)) / 2
            up = (P(u1, v0) - P(u0, v0) + P(u1, v1) - P(u0, v1)) / 2
            cu(c, "back", cen, (max(float(np.linalg.norm(right)), .6) + .5, .45, max(float(np.linalg.norm(up)), .6) + .5), mat, rot=_plane(right, up), bone=bone, dt=-1 if (i + j) % 3 == 0 else 0, faces=SLAB)


def wing_membrane(c, side, span=22, rise=14, drop=15, fingers=4, bone_w=1.4, tattered=False, root=(3.4, 21.0, 2.8), name="wing"):
    """3.62.0 bat / dragon wing: a thick two-part arm rising up and out from the shoulder blade, a wrist knuckle with a thumb claw, long curved fingers fanned downward, skin stretched between them with a scalloped
    edge, and the last skin running back to the waist. The outer half (fingers and skin between them) has its own bone, so the wing tip flexes."""
    s = side; r = V(root[0] * s, root[1], root[2]); k1 = span / 22.0; k2 = rise / 14.0
    bn = f"{name}{'L' if s > 0 else 'R'}"; tb = f"{name}tip{'L' if s > 0 else 'R'}"
    elbow = r + V(s * 8.0 * k1, 7.0 * k2, 3.0)
    wrist = elbow + V(s * 7.0 * k1, 9.0 * k2, 2.0)
    c.sc.def_bone(bn, r, None, [("rot", 2, 12 * s, 3, 0.0), ("rot", 0, 3, 3, 0.25)])
    c.sc.def_bone(tb, wrist, bn, [("rot", 2, 7 * s, 3, 0.16), ("rot", 0, 3, 3, 0.4)])
    cn(c, "back", [r, elbow], [bone_w * 1.9], [bone_w * 1.9], "plate", bone=bn, pad=.4)
    cn(c, "back", [elbow, wrist], [bone_w * 1.5], [bone_w * 1.5], "plate", bone=tb, pad=.4)
    cu(c, "back", elbow, (bone_w * 2.3,) * 3, "plate", dt=1, bone=bn)
    cu(c, "back", wrist, (bone_w * 2.0,) * 3, "plate", dt=1, bone=tb)
    sg(c, "back", wrist, wrist + V(s * 1.2 * k1, 4.2 * k2, -.8), bone_w * 1.1, bone_w * 1.1, "claw", bone=tb, pad=.2)          # thumb claw
    n = fingers
    phis = [math.radians(a) for a in ([-6, 26, 62] if n == 3 else [-8, 20, 50, 80])]
    lens = ([15, 17, 13] if n == 3 else [17, 19, 17, 13])
    tips = []
    for k in range(n):
        d = V(s * math.cos(phis[k]), -math.sin(phis[k]), 0)
        L = lens[k] * k1 if phis[k] < 0.9 else lens[k] * (k1 + k2) / 2
        tip = wrist + d * L + V(0, 0, 1.6 + k * 1.1)
        mid = wrist + d * L * .5 + V(0, 1.6 * (1 - k / max(n - 1, 1)) + .4, .8 + k * .5)
        tips.append(tip)
        cn(c, "back", [wrist, mid, tip], taper(bone_w * 1.1, bone_w * .5, 3), taper(bone_w * 1.1, bone_w * .5, 3), "plate", bone=tb, pad=.35)
        sg(c, "back", tip, tip + d * 1.8 + V(0, -.6, .4), bone_w * .6, bone_w * .6, "claw", bone=tb, pad=.1)
    rng = np.random.default_rng(int(abs(side) * 7 + span)) if tattered else None
    hip = V(s * 1.6, 13.0, 3.2)
    for k in range(n - 1): _fan(c, wrist, tips[k], tips[k + 1], tb, depth=.2, tattered=rng, k0=k)
    _fan(c, wrist, tips[-1], hip, bn, depth=.16, tattered=rng, k0=n)
    _fan(c, wrist, hip, r + V(s * .4, -.6, .3), bn, depth=.0, tattered=None, k0=n + 1)
    return tips


def wing_feather(c, side, n=9, span=20, root=(3.4, 21.0, 2.8), layers=3, name="wing"):
    """3.62.0 feathered wing: a curved arm along the leading edge, long primaries fanned from the wrist, a row of secondaries along the forearm, and short coverts on top. Every feather is a flat blade lying in the wing plane."""
    s = side; r = V(root[0] * s, root[1], root[2]); k = span / 20.0
    bn = f"{name}{'L' if s > 0 else 'R'}"; tb = f"{name}tip{'L' if s > 0 else 'R'}"
    elbow = r + V(s * 6.0 * k, 6.5 * k, 2.2)
    wrist = r + V(s * 12.5 * k, 14.0 * k, 4.2)
    c.sc.def_bone(bn, r, None, [("rot", 2, 10 * s, 3, 0.0), ("rot", 0, 3, 3, 0.25)])
    c.sc.def_bone(tb, wrist, bn, [("rot", 2, 8 * s, 3, 0.16), ("rot", 0, 3, 3, 0.4)])
    cn(c, "back", [r, elbow], [2.6], [2.6], "body", bone=bn, pad=.4)
    cn(c, "back", [elbow, wrist], [2.1], [2.1], "body", bone=tb, pad=.4)
    cu(c, "back", wrist, (2.6, 2.6, 2.6), "gold", dt=1, bone=tb)
    arm_pt = lambda t: (r * (1 - t / .5) + elbow * (t / .5)) if t < .5 else (elbow * (1 - (t - .5) / .5) + wrist * ((t - .5) / .5))
    k_ = 0

    def feather(t, phi, ln, w, mat, lift, bone, dt):
        base = arm_pt(t) + V(0, 0, lift)
        d = V(s * math.cos(math.radians(phi)), -math.sin(math.radians(phi)), 0)
        tip = base + d * ln + V(0, 0, 1.5 + ln * .12)
        return sg(c, "back", base, tip, w, .55, mat, up=(0, 0, 1), bone=bone, dt=dt, pad=.4, faces=SLAB), base, tip
    prim = [14, 17, 19, 19, 17, 14, 11]
    for i, ln in enumerate(prim):                                           # primaries, outermost first
        _, b0, tp = feather(1.0 - .01 * i, 6 + i * 9.0, ln * k, 2.7, "feather", .3 * i, tb, (i % 3) - 1)
        if i % 2 == 0: sg(c, "back", b0 + (tp - b0) * .12 + V(0, 0, .35), tp + V(0, 0, .35), .55, .45, "feather2", up=(0, 0, 1), bone=tb, pad=.1, faces=SLAB)
    sec = [14, 14, 13, 13, 12, 11, 10, 9]
    for i, ln in enumerate(sec):                                           # secondaries along the forearm and upper arm
        t = .9 - i * .105
        feather(t, 66 + i * 2.6, ln * k, 3.0, "feather", .5 + .2 * i, tb if t > .5 else bn, (i % 3) - 1)
    for i, ln in enumerate(prim[:6]):                                      # coverts: short feathers over the roots of the long ones
        feather(1.0 - .01 * i - .05, 6 + i * 9.0, ln * k * .45, 2.6, "plate", .9 + .3 * i, tb, 1 - (i % 3))
    for i in range(6):
        t = .88 - i * .13
        feather(t, 68 + i * 3, sec[i] * k * .4, 2.8, "plate", 1.3 + .2 * i, tb if t > .5 else bn, 1 - (i % 3))


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

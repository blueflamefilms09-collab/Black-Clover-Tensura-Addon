#!/usr/bin/env python3
"""Heads of the Beast Will forms, one builder per kind. head(c, kind, H, s, part): H = centre of the skull, s = scale (side heads are smaller)."""
import math, os, sys
import numpy as np
sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from forms_parts import *


def head(c, kind, H=(0, 28, 0), s=1.0, part="head", ear="pointed", horn=None, bone="root", teeth=True):
    H = V(*H)
    P = lambda x, y, z: H + V(x, y, z) * s
    Z = lambda *a: tuple(v * s for v in a)

    def cb(ctr, size, mat, **k): return c.sc.cube(part, ctr, tuple(v * s for v in size), c.m[mat], bone=bone, **k)
    def sgm(p0, p1, w, h, mat, **k): return c.sc.seg(part, p0, p1, w * s, h * s, c.m[mat], bone=bone, **k)
    def eyes(y=.5, x=2.45, w=2.0, z=-4.7, mat="eye"):
        for sx in (-1, 1):
            cb(P(sx * x, y, z + .05), (w + .8, 1.5, .5), "dark")
            cb(P(sx * x, y, z - .25), (w, 1.0, .5), mat)

    sk = {"snake": (8.6, 7.4, 9.2), "cobra": (8.4, 7.2, 9.0), "squid": (8.0, 8.0, 8.0)}.get(kind, (9.2, 9.0, 9.0))
    if kind in ("wolf", "cat", "lion", "fox", "horse", "bull", "boar", "ape", "dragon", "croc", "bird", "plant", "human", "whale"):
        cb(P(0, 0, 0), sk, "body")
        cb(P(0, 4.5, 0), (sk[0] - 1.0, 1.2, sk[2] - 1.0), "back", dt=-1)
    if kind == "wolf" or kind == "fox":
        long = 1.0 if kind == "wolf" else .85
        cb(P(0, -1.6, -6.4), (4.6 * (1 if kind == "wolf" else .85), 4.0, 5.2), "belly")                     # muzzle base
        cb(P(0, -1.3, -10.0), (3.4 * (1 if kind == "wolf" else .75), 2.8, 3.4 * long), "belly")                  # muzzle tip
        cb(P(0, -.1, -7.2), (3.0, 1.4, 4.6), "body", dt=1)                                                   # nose bridge
        cb(P(0, -.5, -11.9), (2.6, 1.8, .8), "dark")                                                          # nose
        cb(P(0, -3.4, -6.4), (3.4, 1.0, 5.4), "belly", dt=-1)                                                 # lower jaw
        for sx in (-1, 1):
            cb(P(sx * 1.4, -2.5, -9.4), (.7, 1.6, .6), "claw"); cb(P(sx * 1.3, -2.9, -7.0), (.6, 1.2, .6), "claw")
        cb(P(0, .8, -4.7), (8.8, 1.4, 1.2), "back", dt=-1)
        eyes(.9)
        for sx in (-1, 1):                                                                                      # cheek ruff
            for k in range(3): sgm(P(sx * 4.2, -1.2 - k * .9, -2.0 + k * .8), P(sx * 6.0, -2.8 - k * 1.2, -1.6 + k * 1.0), 2.2, 1.6, "belly" if k % 2 == 0 else "body", pad=.4, dt=k % 2)
    elif kind in ("cat", "lion"):
        cb(P(0, -1.8, -5.9), (4.8, 3.4, 4.0), "belly")
        cb(P(0, -1.1, -5.9), (3.6, 1.4, 4.1), "body", dt=1)
        cb(P(0, -.6, -8.0), (2.4, 1.4, .8), "dark")
        cb(P(0, -3.3, -5.4), (3.8, 1.2, 3.6), "belly", dt=-1)
        for sx in (-1, 1):
            cb(P(sx * 1.35, -2.8, -7.6), (.8, 1.8, .7), "claw"); cb(P(sx * 1.2, -3.5, -7.0), (.7, 1.2, .7), "claw")
            cb(P(sx * 2.45, 1.2, -4.9), (2.6, .5, .5), "dark")
        cb(P(0, .2, -4.7), (8.8, 1.4, 1.2), "back", dt=-1)
        eyes(.5)
    elif kind == "horse":
        pts = [P(0, -.5, -4.6), P(0, -2.0, -7.6), P(0, -3.6, -10.4)]
        cn(c, part, pts, [4.4 * s, 3.6 * s], [5.0 * s, 4.0 * s], "belly", bone=bone, pad=.6)
        cb(P(0, -4.4, -11.6), (3.8, 3.0, 2.2), "dark"); cb(P(0, -.1, -7.0), (2.4, 1.2, 5.0), "body", dt=1, rot=(14, 0, 0))
        for sx in (-1, 1): cb(P(sx * 1.2, -3.7, -12.6), (.9, .9, .5), "back")
        eyes(1.6, 3.2, 1.6, -4.4)
    elif kind in ("bull", "boar"):
        w = 5.4 if kind == "bull" else 5.8
        cb(P(0, -2.0, -6.0), (w, 3.8, 4.6), "belly")
        cb(P(0, -2.6, -8.9), (w - .6, 3.4, 1.6), "dark")
        for sx in (-1, 1):
            cb(P(sx * 1.4, -2.6, -9.8), (.8, .8, .5), "back")
            if kind == "boar": sgm(P(sx * 2.4, -4.1, -8.2), P(sx * 3.2, -1.0, -10.2), 1.2, 1.2, "claw", pad=.3); cb(P(sx * 3.2, -.2, -10.2), (.8, 1.4, .8), "claw")
        if kind == "bull": cb(P(0, -4.1, -9.2), (3.4, .7, 1.0), "plate", dt=1)
        cb(P(0, .6, -4.8), (9.4, 1.6, 1.4), "back", dt=-1); eyes(.5, 2.6, 1.8)
    elif kind == "ape":
        cb(P(0, -1.6, -5.2), (6.0, 4.6, 2.4), "belly"); cb(P(0, 1.0, -4.8), (7.6, 1.8, 1.4), "back", dt=-1); cb(P(0, -2.6, -6.2), (4.0, 1.4, 1.2), "dark")
        cb(P(0, -.6, -6.4), (2.0, 1.4, .6), "back"); eyes(.1, 2.2, 1.8, -5.2)
    elif kind == "dragon":
        cb(P(0, -1.2, -6.2), (5.0, 3.6, 4.8), "belly"); cb(P(0, -.8, -9.4), (3.8, 2.6, 3.2), "belly"); cb(P(0, .1, -7.6), (3.6, 1.2, 5.6), "body", dt=1)
        cb(P(0, -3.2, -7.0), (4.0, 1.0, 6.4), "belly", dt=-1)
        for sx in (-1, 1):
            cb(P(sx * 1.2, -.4, -10.9), (.7, .7, .5), "dark"); cb(P(sx * 1.6, -2.4, -9.4), (.7, 1.6, .6), "claw"); cb(P(sx * 1.8, -2.2, -6.6), (.6, 1.2, .6), "claw")
            sgm(P(sx * 3.0, 2.8, -3.6), P(sx * 3.2, 4.8, -2.0), 1.2, 1.2, "plate", pad=.2)
        cb(P(0, 1.2, -4.8), (8.8, 1.8, 1.4), "plate", dt=1); eyes(.5)
        for k in range(4): sgm(P(0, 4.6 - k * .5, 1.4 + k * 1.4), P(0, 6.6 - k * .6, 2.4 + k * 1.4), 1.2, 1.4, "plate", dt=k % 2, pad=.2)
    elif kind == "croc":
        cb(P(0, -2.0, -8.0), (4.8, 2.6, 8.0), "body"); cb(P(0, -4.0, -8.4), (4.2, 1.6, 8.0), "belly")
        for k in range(5):
            for sx in (-1, 1): cb(P(sx * 2.2, -3.2, -4.4 - k * 1.8), (.6, 1.4, .6), "claw")
        for sx in (-1, 1): cb(P(sx * 2.8, 1.0, -5.0), (2.6, 2.0, 2.6), "body"); cb(P(sx * 2.8, 2.2, -5.0), (1.6, .6, 1.6), "glow")
        cb(P(0, -1.0, -12.4), (3.0, 1.8, .8), "dark")
    elif kind == "bird":
        cb(P(0, -1.0, -5.4), (3.0, 3.6, 3.0), "plate"); cb(P(0, -1.6, -8.0), (2.4, 2.6, 2.8), "plate", dt=1, rot=(-12, 0, 0)); cb(P(0, -2.9, -9.2), (1.4, 1.0, 1.4), "plate", dt=-1)
        cb(P(0, -3.9, -6.4), (2.0, 1.2, 3.4), "plate", dt=-1); eyes(1.2, 3.0, 1.4, -4.4)
        for k in range(5): sgm(P(0, 4.4, -.4 + k * .4), P((k - 2) * .8, 7.6 - abs(k - 2) * .5, 1.6 + k * .3), 1.6, 1.0, "belly" if k % 2 else "body", pad=.2, dt=k % 3 - 1)
    elif kind == "plant":
        cb(P(0, -1.6, -5.2), (4.4, 3.6, 3.0), "belly"); cb(P(0, .8, -4.8), (8.6, 1.6, 1.4), "back")
        for sx in (-1, 1): cb(P(sx * 2.3, .0, -4.7), (2.2, 1.6, .5), "glow"); cb(P(sx * 2.3, .0, -4.9), (1.2, .8, .3), "dark")
        cb(P(0, -3.6, -4.4), (3.4, 1.6, 2.0), "back")
        for k in range(6): sgm(P(-4 + k * 1.6, 4.4, -3.6 + (k % 2) * 3), P(-4.4 + k * 1.7, 6.4, -3 + (k % 2) * 3.4), 1.6, 1.4, "leaf", pad=.2, dt=k % 2)
    elif kind == "human":
        cb(P(0, -1.2, -4.3), (6.8, 6.4, 1.2), "skin"); cb(P(0, -4.9, -3.8), (2.4, 2.8, 1.6), "gold")
        cb(P(0, 0.2, -4.9), (6.8, 1.0, .6), "dark"); cb(P(0, -.8, -4.95), (1.2, 2.0, .6), "skin", dt=-1)
        eyes(.6, 1.9, 1.5, -4.9)
        cb(P(0, 5.0, 0), (10.0, 1.8, 10.0), "plate")                                                   # the nemes: striped crown, lappets, the queue
        for k in range(5): cb(P(-3.6 + k * 1.8, 5.0, -5.15), (.9, 1.8, .3), "gold")
        for sx in (-1, 1):
            for k in range(4): cb(P(sx * 5.4, 3.6 - k * 2.2, 1.0 - k * .2), (1.8, 2.0, 5.2 - k * .3), "plate" if k % 2 == 0 else "gold")
        for k in range(5): cb(P(0, 3.0 - k * 2.6, 5.4), (6.0 - k * .4, 2.2, 1.6), "plate" if k % 2 == 0 else "gold")
    elif kind == "whale":
        cb(P(0, -2.2, -4.8), (9.0, 4.4, 6.0), "body"); cb(P(0, -5.0, -4.6), (8.4, 2.4, 5.6), "belly")
        for k in range(7):
            sx = -3.6 + k * 1.2; cb(P(sx, -2.2, -7.6), (.6, 1.8, .6), "claw"); cb(P(sx, -4.4, -7.4), (.6, 1.6, .6), "claw")
        cb(P(0, -3.4, -6.8), (7.0, 1.0, 2.6), "dark"); cb(P(0, 3.0, -2.8), (7.0, 2.4, 6.0), "back")
        for sx in (-1, 1): cb(P(sx * 3.4, 1.4, -4.6), (1.6, 1.2, .6), "glow")
    elif kind in ("snake", "cobra"):
        cb(P(0, -.4, 0), sk, "body"); cb(P(0, 3.6, 0), (sk[0] - 1.0, 1.0, sk[2] - 1.0), "back", dt=-1)
        cb(P(0, -1.2, -5.8), (6.4, 3.2, 3.4), "body", dt=1); cb(P(0, -1.4, -8.0), (5.0, 2.6, 1.8), "belly"); cb(P(0, -3.2, -6.4), (5.4, 1.2, 5.6), "belly", dt=-1)
        for sx in (-1, 1):
            cb(P(sx * 1.4, -2.7, -8.4), (.6, 2.2, .6), "claw"); cb(P(sx * 2.8, -2.2, -6.2), (.6, 1.6, .6), "claw")
            cb(P(sx * 3.2, .8, -4.3), (2.4, .9, .6), "glow"); cb(P(sx * 3.0, 1.8, -4.5), (3.0, .6, .6), "dark")
            cb(P(sx * .8, -.4, -9.0), (.6, .6, .5), "dark")
        if kind == "cobra":
            for sx in (-1, 1):
                for k in range(5): sgm(P(sx * 3.0, 1.0 - k * 1.3, 2.0), P(sx * 8.4, 4.6 - k * 2.4, 3.0), 2.0, .8, "plate" if k % 2 else "body", pad=.2, dt=k % 2 - 1)
            cb(P(0, 0.4, 3.0), (10.0, 8.0, .9), "body", dt=-1); cb(P(0, 0.4, 2.4), (9.0, 7.0, .9), "plate", dt=0)
            for sx in (-1, 1): cb(P(sx * 3.0, 1.6, 2.0), (2.2, 2.2, .5), "glow")
    elif kind == "squid":
        cb(P(0, 2.4, 1.2), (8.0, 6.6, 7.4), "body")
        for k in range(5): cb(P(0, 5.2 + k * 2.4, 3.0 + k * 1.2), (7.4 - k * 1.2, 2.6, 6.6 - k * 1.0), "body", dt=(k % 2) - 1)
        for sx in (-1, 1): cb(P(sx * 3.6, 2.4, 1.0), (1.4, 1.4, 3.4), "plate", dt=1)
        cb(P(0, 0.4, -4.1), (6.2, 3.0, 1.0), "belly")
        for sx in (-1, 1): cb(P(sx * 2.4, 1.0, -4.8), (2.2, 1.6, .6), "glow"); cb(P(sx * 2.4, 1.0, -5.1), (.8, .8, .3), "dark")
        cb(P(0, -2.2, -4.8), (2.4, 2.0, 1.8), "claw"); cb(P(0, -3.0, -5.6), (1.2, 2.0, 1.2), "claw", dt=-1)
    if kind not in ("snake", "cobra", "squid", "bird", "whale", "croc", "human", "plant") and ear:
        pass
    return H

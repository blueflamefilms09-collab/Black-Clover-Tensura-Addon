#!/usr/bin/env python3
"""Heads of the Beast Will forms, one builder per kind. head(c, kind, H, s, part): H = centre of the skull, s = scale (side heads are smaller).

3.77.0 (heads pass): every head is a beast's skull, not a box with a face painted on. The shared recipe: a muzzle of two tapering segments that projects forward and drops, a separate lower jaw
hanging open with fangs, cheek wedges that turn the box into a wedge seen from the front, slanted brow ridges over narrow glowing eyes, and a sloped forehead. Each kind sets the proportions
(long wolf muzzle, short broad cat muzzle, flat crocodile snout, hooked beak ...) and adds its own pieces. `lite` = the small side heads of the many-headed wills: the same silhouette in a
third of the cubes. Hidden faces are left out (`faces`), so a head costs fewer quads than its cube count suggests. Front is -z."""
import math, os, sys
import numpy as np
sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from forms_parts import *

SIDES = (0, 1, 2, 3)            # a segment whose two ends are buried
TIP = (0, 1, 2, 3, 4)           # ... whose far end shows
FRONT = (0, 1, 2, 3, 5)         # a cube against the skull's face: everything but its back


def head(c, kind, H=(0, 28, 0), s=1.0, part="head", ear="pointed", horn=None, bone="root", teeth=True, lite=False):
    H = V(*H)
    P = lambda x, y, z: H + V(x, y, z) * s

    def cb(ctr, size, mat, **k): return c.sc.cube(part, ctr, tuple(v * s for v in size), c.m[mat], bone=bone, **k)
    def sgm(p0, p1, w, h, mat, **k):
        if "pad" in k: k["pad"] *= s
        return c.sc.seg(part, p0, p1, w * s, h * s, c.m[mat], bone=bone, **k)

    def muzzle(L, w0, w1, h0, h1, y0=-.9, drop=1.3, mat="belly", ridge="body", z0=-4.4):
        """two tapering segments from the face forward and down, a ridge along the top; returns (tip centre, y0, drop)"""
        a, m_, t = P(0, y0, z0), P(0, y0 - drop * .45, z0 - L * .55), P(0, y0 - drop, z0 - L)
        sgm(a, m_, w0, h0, mat, faces=SIDES)
        sgm(m_, t, w1, h1, mat, faces=TIP, pad=.3)
        if not lite: sgm(P(0, y0 + h0 / 2 + .1, z0 + .4), P(0, y0 - drop * .85 + h1 / 2 + .2, z0 - L * .88), w1 * .6, .8, ridge, dt=1, faces=(0, 1, 2, 4))
        return t

    def jaw(L, w, y0, drop, h0, h1, mat="belly", z0=-4.4, gape=1.0):
        """the lower jaw, hanging open, and the dark of the mouth above it"""
        sgm(P(0, y0 - h0 / 2 - .5, z0 + 1.0), P(0, y0 - drop - h1 / 2 - .6 - gape, z0 - L * .92), w, 1.2, mat, dt=-1, faces=TIP, pad=.2)
        if not lite: sgm(P(0, y0 - h0 / 2 + .1, z0 - .4), P(0, y0 - drop - h1 / 2 - .2 - gape * .4, z0 - L * .86), w - .9, .9, "dark", faces=(0, 1, 4))

    def fangs(L, w, y0, drop, h1, f=2.0, z0=-4.4, lower=True, gape=1.0):
        zt = z0 - L
        yb = y0 - drop - h1 / 2
        for sx in (-1, 1):
            cb(P(sx * (w / 2 - .35), yb - f / 2 + .5, zt + 1.0), (.75, f, .75), "claw", faces=(0, 1, 3, 4, 5))
            if lower and not lite: cb(P(sx * (w / 2 - .25), yb - gape - .2, zt + 2.6), (.6, 1.4, .6), "claw", faces=(0, 1, 2, 4, 5))

    def cheeks(w0, h0, y0, L, drop, mat="body", z0=-4.4):
        if lite: return
        for sx in (-1, 1): sgm(P(sx * 4.25, y0 - .1, z0 + 1.6), P(sx * (w0 / 2 + .1), y0 - drop * .4, z0 - L * .5), 1.5, h0 * .92, mat, dt=-1, faces=SIDES)

    def face(ye=1.9, xe=2.7, we=2.1, brow="back", slant=16, z=-4.6, eye="eye", forehead="body", browlen=3.7):
        """slanted brow ridges over narrow glowing eyes in dark sockets, and the sloped forehead"""
        if lite:
            cb(P(0, ye + 1.0, z - .3), (8.6, 1.2, 1.3), brow, dt=-1, faces=FRONT)
            for sx in (-1, 1): cb(P(sx * xe, ye, z - .15), (we, .9, .4), eye, faces=(5,))
            return
        for sx in (-1, 1):
            cb(P(sx * xe, ye, z - .05), (we + .9, 1.6, .4), "dark", faces=(5,))
            cb(P(sx * xe, ye - .05, z - .3), (we, .75, .4), eye, rot=(0, 0, sx * slant * .6), faces=(2, 3, 5))
            cb(P(sx * (xe - .1), ye + 1.15, z - .5), (browlen, 1.25, 1.7), brow, rot=(0, 0, sx * slant), dt=-1, faces=FRONT)
        if forehead: cb(P(0, 3.75, -3.0), (7.2, 1.3, 4.2), forehead, rot=(-17, 0, 0), dt=1, faces=(0, 1, 2, 5))

    def skull(sk=(9.2, 9.0, 9.0), top="back"):
        cb(P(0, 0, 0), sk, "body")
        if not lite: cb(P(0, sk[1] / 2, 0), (sk[0] - 1.0, 1.2, sk[2] - 1.0), top, dt=-1, faces=(0, 1, 2, 4, 5))

    if kind in ("wolf", "fox"):
        fx = kind == "fox"
        L, w0, w1, h0, h1, y0, dr = (6.8, 4.6, 2.6, 3.8, 2.4, -.9, 1.2) if fx else (7.6, 5.3, 3.4, 4.1, 2.8, -.9, 1.3)
        skull()
        t = muzzle(L, w0, w1, h0, h1, y0, dr)
        cb(t + V(0, h1 / 2 - .2, -.2) * s, (w1 * .8, 1.4, 1.1), "dark", faces=FRONT)                                        # the nose
        jaw(L, w1 + .3, y0, dr, h0, h1); fangs(L, w1, y0, dr, h1, 2.1); cheeks(w0, h0, y0, L, dr)
        face(brow="back")
        if not lite:
            for sx in (-1, 1):                                                                                             # the cheek ruff
                for k in range(3): sgm(P(sx * 4.2, -1.2 - k * .9, -2.0 + k * .8), P(sx * 6.3, -3.0 - k * 1.2, -1.2 + k * 1.2), 2.2, 1.6, "belly" if k % 2 == 0 else "body", pad=.4, dt=k % 2)
    elif kind in ("cat", "lion"):
        L, w0, w1, h0, h1, y0, dr = 4.6, 5.8, 4.6, 3.8, 3.2, -1.0, .8
        skull()
        t = muzzle(L, w0, w1, h0, h1, y0, dr)
        cb(t + V(0, h1 / 2 - .5, -.25) * s, (2.4, 1.3, .9), "dark", faces=FRONT)                                           # the broad nose
        jaw(L, w1 - .2, y0, dr, h0, h1); fangs(L, w1 - .4, y0, dr, h1, 2.9 if kind == "cat" else 2.2); cheeks(w0, h0, y0, L, dr)
        for sx in (-1, 1): cb(t + V(sx * 1.5, -.5, -.1) * s, (1.9, 1.7, .8), "belly", dt=1, faces=FRONT)                   # whisker pads
        if kind == "lion": sgm(P(0, -4.6, -6.0), P(0, -7.2, -6.6), 2.6, 1.6, "mane", pad=.3)                                # the chin tuft
        face(brow="back", slant=18)
    elif kind == "dragon":
        L, w0, w1, h0, h1, y0, dr = (7.6, 5.0, 3.4, 3.6, 2.4, -.9, 1.3) if lite else (9.0, 5.5, 3.7, 4.0, 2.6, -.9, 1.6)
        skull(top="plate")
        t = muzzle(L, w0, w1, h0, h1, y0, dr, mat="body", ridge="plate")
        jaw(L, w1 + .2, y0, dr, h0, h1); fangs(L, w1, y0, dr, h1, 2.0); cheeks(w0, h0, y0, L, dr)
        face(brow="plate", slant=20, browlen=4.2)
        for sx in (-1, 1):
            if lite:
                sgm(P(sx * 2.8, 4.0, -1.0), P(sx * 3.6, 6.6, 2.6), 1.3, 1.3, "claw", pad=.2)                               # a horn each, swept back
                continue
            cb(t + V(sx * 1.0, h1 / 2 + .25, .6) * s, (.9, .8, 1.2), "plate", faces=(0, 1, 2, 4, 5))                        # nostril ridges
            for z_ in (2.6, 4.4): cb(P(sx * (w1 / 2 - .2), y0 - dr * .7 - h1 / 2 - .3, -4.4 - L + z_ + 1.0), (.55, 1.1, .55), "claw", faces=(0, 1, 3, 4, 5))   # the row of teeth
            for k in range(3): sgm(P(sx * 4.3, -2.0 + k * 1.7, .6), P(sx * 7.2, -1.2 + k * 2.4, 4.4), 1.1, 1.7, "plate" if k % 2 == 0 else "membrane", dt=k % 2, pad=.2)   # the cheek frill
        if not lite:
            sgm(t + V(0, h1 / 2, 1.0) * s, t + V(0, h1 / 2 + 2.0, 2.0) * s, 1.0, 1.0, "claw", pad=.2)                       # the nose horn
            for k in range(4): sgm(P(0, 4.6 - k * .5, 1.4 + k * 1.4), P(0, 6.8 - k * .6, 2.6 + k * 1.4), 1.2, 1.4, "plate", dt=k % 2, pad=.2)
    elif kind == "croc":
        L, w0, w1, h0, h1, y0, dr = 10.5, 5.8, 4.2, 3.0, 2.2, -1.5, .5
        skull()
        t = muzzle(L, w0, w1, h0, h1, y0, dr, mat="body", ridge="back")
        jaw(L, w1 + .3, y0, dr, h0, h1, gape=1.3); cheeks(w0, h0, y0, L, dr)
        for k in range(4):                                                                                                 # interlocking teeth along the snout
            for sx in (-1, 1):
                w = (w0 if k < 2 else w1) / 2 - .25
                cb(P(sx * w, y0 - dr * (k / 4) - 1.9 - (0 if k < 2 else -.25), -5.6 - k * 2.3), (.6, 1.5, .6), "claw", faces=(0, 1, 3, 4, 5))
                if k % 2: cb(P(sx * (w + .1), y0 - dr * (k / 4) - 2.9, -6.8 - k * 2.3), (.55, 1.2, .55), "claw", faces=(0, 1, 2, 4, 5))
        cb(t + V(0, h1 / 2 + .2, .5) * s, (2.6, .9, 1.4), "back", faces=(0, 1, 2, 4, 5))                                    # the nostril bump
        for sx in (-1, 1):                                                                                                 # the eyes ride on top of the skull
            cb(P(sx * 2.8, 3.4, -3.6), (2.8, 2.6, 3.0), "body", dt=1, faces=(0, 1, 2, 5))
            cb(P(sx * 2.8, 3.5, -5.15), (2.0, .8, .4), "eye", rot=(0, 0, sx * 10), faces=(2, 3, 5))
            cb(P(sx * 2.7, 4.75, -4.4), (3.4, 1.0, 2.2), "back", rot=(0, 0, sx * 16), dt=-1, faces=FRONT)
    elif kind == "bird":
        skull()
        sgm(P(0, -.4, -4.4), P(0, -1.0, -7.2), 3.8, 3.6, "plate", faces=SIDES)                                             # the cere
        sgm(P(0, -1.0, -7.2), P(0, -2.6, -9.8), 2.8, 2.8, "gold", faces=TIP, pad=.3)                                        # the beak, curving down
        sgm(P(0, -2.6, -9.9), P(0, -5.2, -9.4), 1.5, 1.6, "claw", dt=-1, faces=TIP, pad=.2)                                 # ... to a hook
        sgm(P(0, -2.9, -4.0), P(0, -4.2, -8.0), 2.4, 1.0, "gold", dt=-1, faces=TIP)                                         # the lower beak
        sgm(P(0, -2.2, -4.6), P(0, -3.3, -8.0), 1.6, .7, "dark", faces=(0, 1))
        face(ye=1.6, xe=3.0, we=1.8, brow="back", slant=22, browlen=4.0)
        for sx in (-1, 1):
            for k in range(2): sgm(P(sx * 4.4, -.6 - k * 1.6, -1.0), P(sx * 6.4, -1.6 - k * 2.0, 2.6), 1.0, 1.8, "feather" if k == 0 else "belly", dt=k, pad=.3)   # cheek feathers
        for k in range(5): sgm(P(0, 4.4, -.4 + k * .4), P((k - 2) * .8, 7.9 - abs(k - 2) * .5, 1.9 + k * .3), 1.6, 1.0, "belly" if k % 2 else "body", pad=.2, dt=k % 3 - 1)
    elif kind in ("bull", "boar"):
        br = kind == "boar"
        L, w0, w1, h0, h1, y0, dr = (5.8, 6.0, 5.0, 4.2, 3.8, -1.0, 1.1) if br else (5.2, 6.2, 5.4, 4.3, 3.9, -1.0, 1.0)
        skull()
        t = muzzle(L, w0, w1, h0, h1, y0, dr)
        cb(t + V(0, .1, -.3) * s, (w1 - .5, h1 - .7, .9), "dark", faces=FRONT)                                              # the flat nose
        for sx in (-1, 1): cb(t + V(sx * 1.1, .1, -.85) * s, (.9, 1.0, .4), "back", faces=(5,))
        jaw(L, w1 - .2, y0, dr, h0, h1, gape=.6); cheeks(w0, h0, y0, L, dr)
        for sx in (-1, 1):
            if br:                                                                                                         # tusks: out of the lower jaw, up past the snout
                sgm(P(sx * 2.7, -4.0, -8.0), P(sx * 4.0, -1.6, -9.8), 1.4, 1.4, "claw", pad=.3)
                sgm(P(sx * 4.0, -1.6, -9.8), P(sx * 3.7, 1.4, -9.2), 1.0, 1.0, "claw", dt=1, pad=.2)
        if not br:                                                                                                         # the ring through the nose
            cb(t + V(0, -h1 / 2 - .5, -.5) * s, (3.0, .7, .7), "gold")
            for sx in (-1, 1): cb(t + V(sx * 1.4, -h1 / 2 + .2, -.5) * s, (.7, 1.6, .7), "gold", faces=(0, 1, 4, 5))
        face(ye=1.8, xe=2.8, we=1.9, brow="back", slant=18, browlen=4.1)
    elif kind == "ape":
        L, w0, w1, h0, h1, y0, dr = 4.8, 5.2, 4.0, 3.8, 3.0, -1.1, 1.0
        skull()
        t = muzzle(L, w0, w1, h0, h1, y0, dr, ridge="plate")
        cb(t + V(0, h1 / 2 - .6, -.25) * s, (2.0, 1.2, .8), "dark", faces=FRONT)
        jaw(L, w1, y0, dr, h0, h1, gape=1.2); fangs(L, w1, y0, dr, h1, 2.6); cheeks(w0, h0, y0, L, dr)
        for sx in (-1, 1):
            sgm(P(sx * 1.7, y0 + h0 / 2 - .2, -4.6), P(sx * 1.4, y0 - dr + h1 / 2 - .1, -4.4 - L * .9), 1.0, .9, "plate", dt=1, faces=(0, 1, 2, 4))   # the ridges of the muzzle
            for k in range(2): sgm(P(sx * 4.3, -.6 - k * 1.8, -1.6), P(sx * 6.2, -2.4 - k * 2.2, .6), 2.0, 1.6, "back" if k else "body", pad=.3, dt=k)     # the cheek mane
        face(ye=1.8, xe=2.5, we=1.9, brow="back", slant=14, browlen=4.2)
    elif kind == "horse":
        L, w0, w1, h0, h1, y0, dr = 9.4, 4.8, 3.6, 4.4, 3.2, -.6, 3.4
        skull()
        t = muzzle(L, w0, w1, h0, h1, y0, dr, mat="belly", ridge="plate")
        cb(t + V(0, -.2, -.3) * s, (w1 - .2, h1 - .4, .8), "dark", faces=FRONT)
        for sx in (-1, 1): cb(t + V(sx * 1.2, .5, -.2) * s, (.9, .9, .9), "back", faces=(0, 1, 2, 5))                       # nostrils
        jaw(L, w1 - .3, y0, dr, h0, h1, gape=.3); cheeks(w0, h0, y0, L, dr)
        cb(P(0, 2.9, -4.9), (3.4, 2.6, .8), "plate", faces=FRONT)                                                           # the chamfron: a plate on the forehead with a gem
        cb(P(0, 3.0, -5.4), (1.5, 1.5, .5), "glow", rot=(0, 0, 45), faces=(0, 1, 2, 3, 5))
        face(ye=1.6, xe=3.3, we=1.6, brow="back", slant=14, browlen=2.8, forehead=None)
    elif kind == "plant":
        skull()
        cb(P(0, -1.0, -4.7), (6.6, 6.4, 1.0), "belly", faces=FRONT)                                                         # the bark mask
        for sx in (-1, 1):
            cb(P(sx * 2.2, .9, -5.05), (2.9, 1.9, .5), "dark", faces=(5,))
            cb(P(sx * 2.2, .85, -5.2), (2.2, .9, .4), "glow", rot=(0, 0, sx * 12), faces=(2, 3, 5))                          # hollow eyes, the glow inside
            cb(P(sx * 2.3, 2.4, -5.3), (4.0, 1.5, 1.6), "back", rot=(0, 0, sx * 20), dt=-1, faces=FRONT)                     # slabs of bark for brows
            sgm(P(sx * 3.4, -1.0, -5.0), P(sx * 3.0, -4.6, -5.2), 1.3, 1.2, "back", pad=.3, dt=-1)                           # cheek bark
        cb(P(0, -2.9, -5.1), (4.2, .9, .6), "dark", faces=(5,))                                                             # the mouth: a crack with light in it
        for k in range(3): cb(P(-1.3 + k * 1.3, -2.9, -5.3), (.6, .5, .4), "glow", faces=(5,))
        for k in range(3): sgm(P(-1.5 + k * 1.5, -4.2, -4.6), P(-1.8 + k * 1.8, -7.2 - (k % 2), -4.9), 1.3, 1.0, "leaf", pad=.2, dt=k % 2)   # the moss beard
        for k in range(6): sgm(P(-4 + k * 1.6, 4.4, -3.6 + (k % 2) * 3), P(-4.4 + k * 1.7, 6.6, -3 + (k % 2) * 3.4), 1.6, 1.4, "leaf", pad=.2, dt=k % 2)
    elif kind == "human":
        cb(P(0, 0, 0), (9.2, 9.0, 9.0), "body")
        cb(P(0, -1.2, -4.3), (6.8, 6.4, 1.2), "skin", faces=FRONT)                                                          # a still, stern face under the crown
        cb(P(0, -.7, -5.0), (1.2, 2.6, .9), "skin", dt=-1, faces=FRONT)                                                     # the nose
        cb(P(0, -3.0, -4.95), (2.6, .45, .3), "dark", faces=(5,))                                                           # the mouth, a line
        for sx in (-1, 1):
            cb(P(sx * 2.0, .7, -4.95), (2.2, 1.0, .3), "dark", faces=(5,))
            cb(P(sx * 2.0, .65, -5.05), (1.6, .55, .3), "eye", rot=(0, 0, sx * 8), faces=(2, 3, 5))
            cb(P(sx * 2.0, 1.65, -5.1), (2.9, .7, .7), "plate", rot=(0, 0, sx * 14), faces=FRONT)                            # painted brows
            cb(P(sx * 2.6, -1.6, -4.9), (1.2, 1.0, .5), "skin", dt=1, faces=FRONT)                                           # cheekbones
            cb(P(sx * 2.2, -.3, -5.0), (.5, 1.4, .3), "plate", faces=(5,))                                                   # the line under the eye
        sea = bool(c.sp.get("waistheads"))                                                                                  # Scylla wears this face too: fangs and fins instead of the pharaoh's beard and serpent
        if sea:
            for sx in (-1, 1):
                cb(P(sx * 1.0, -3.5, -5.0), (.5, 1.1, .4), "claw", faces=(0, 1, 3, 5))
                for k in range(2): sgm(P(sx * 4.4, -.4 - k * 2.0, -2.6), P(sx * 6.6, .6 - k * 2.4, -.2), .8, 2.2, "membrane", dt=k, pad=.2)
        else:
            cb(P(0, -5.2, -3.9), (2.0, 3.4, 1.4), "gold"); cb(P(0, -7.2, -3.9), (2.6, .9, 1.7), "plate")                     # the false beard
        cb(P(0, 5.0, 0), (10.0, 1.8, 10.0), "plate")                                                                        # the nemes: striped crown, lappets, the queue
        for k in range(5): cb(P(-3.6 + k * 1.8, 5.0, -5.15), (.9, 1.8, .3), "gold", faces=(5,))
        if not sea: sgm(P(0, 5.2, -5.0), P(0, 7.6, -6.4), 1.4, 1.2, "gold", pad=.2); cb(P(0, 7.9, -6.9), (2.0, 1.6, 1.2), "glow")   # the uraeus: a rearing serpent on the brow
        for sx in (-1, 1):
            for k in range(4): cb(P(sx * 5.4, 3.6 - k * 2.2, 1.0 - k * .2), (1.8, 2.0, 5.2 - k * .3), "plate" if k % 2 == 0 else "gold")
        for k in range(5): cb(P(0, 3.0 - k * 2.6, 5.4), (6.0 - k * .4, 2.2, 1.6), "plate" if k % 2 == 0 else "gold")
    elif kind == "whale":
        skull()
        cb(P(0, 3.0, -2.8), (7.0, 2.4, 6.0), "back")                                                                        # the dome of the head
        cb(P(0, -.2, -5.4), (9.4, 3.4, 3.0), "body", rot=(-10, 0, 0), faces=FRONT)                                           # the upper lip, a heavy overhang
        cb(P(0, -2.7, -5.6), (8.2, 1.9, 2.6), "dark", faces=(0, 1, 5))                                                      # the maw
        cb(P(0, -4.6, -6.2), (9.2, 2.0, 4.6), "belly", faces=(0, 1, 2, 3, 5))                                               # the lower jaw, thrust forward
        for k in range(5): cb(P(-3.4 + k * 1.7, -3.0, -7.9), (.8, 1.9, .8), "claw", faces=(0, 1, 2, 4, 5))                   # tusks of the lower jaw
        for k in range(4): cb(P(-2.6 + k * 1.75, -1.9, -6.6), (.6, 1.1, .6), "claw", faces=(0, 1, 3, 5))
        for sx in (-1, 1):                                                                                                 # small slit eyes low on the sides, a ridge over each
            cb(P(sx * 4.65, -.2, -2.4), (.4, 1.0, 2.4), "eye", faces=(0 if sx > 0 else 1, 2, 3))
            cb(P(sx * 4.6, .9, -2.4), (1.0, .9, 3.6), "back", dt=-1, faces=(0, 1, 2, 3, 5))
            for k in range(3): cb(P(sx * 4.7, -3.6, -3.0 + k * 1.8), (.4, 2.2, .9), "belly", dt=-1, faces=(0 if sx > 0 else 1,))   # throat grooves
        for k, (x, z) in enumerate(((-2.4, -5.4), (1.6, -4.6), (3.0, -2.0))): cb(P(x, 1.9 if k == 0 else 4.4, z), (1.2, 1.0, 1.2), "claw", dt=-1)     # barnacles
    elif kind in ("snake", "cobra"):
        sk = (8.4, 7.2, 9.0) if kind == "cobra" else (8.6, 7.4, 9.2)
        cb(P(0, -.4, 0), sk, "body")
        if not lite: cb(P(0, 3.6, 0), (sk[0] - 1.0, 1.0, sk[2] - 1.0), "back", dt=-1, faces=(0, 1, 2, 4, 5))
        L, w0, w1, h0, h1, y0, dr = (5.6, 6.0, 3.8, 2.8, 2.0, -1.0, .6) if lite else (6.6, 6.4, 3.9, 3.0, 2.2, -1.0, .8)
        t = muzzle(L, w0, w1, h0, h1, y0, dr, mat="body", ridge="back")
        jaw(L, w1 + .3, y0, dr, h0, h1, gape=1.4); fangs(L, w1, y0, dr, h1, 2.7, lower=False); cheeks(w0, h0, y0, L, dr)
        face(ye=1.3, xe=3.0, we=2.1, brow="back", slant=20, browlen=3.6, forehead=None)
        if not lite:
            for sx in (-1, 1): cb(t + V(sx * .9, h1 / 2, .4) * s, (.6, .5, .7), "dark", faces=(0, 1, 2, 5))                 # nostrils
            sgm(t + V(0, -h1 / 2 - .8, .6) * s, t + V(0, -h1 / 2 - 1.3, -2.0) * s, .5, .3, "glow")                          # the forked tongue
            for sx in (-1, 1): sgm(t + V(0, -h1 / 2 - 1.3, -2.0) * s, t + V(sx * .8, -h1 / 2 - 1.5, -3.2) * s, .35, .3, "glow")
        if kind == "cobra":
            for sx in (-1, 1):
                for k in range(5): sgm(P(sx * 3.0, 1.0 - k * 1.3, 2.0), P(sx * 8.4, 4.6 - k * 2.4, 3.0), 2.0, .8, "plate" if k % 2 else "body", pad=.2, dt=k % 2 - 1)
            cb(P(0, 0.4, 3.0), (10.0, 8.0, .9), "body", dt=-1); cb(P(0, 0.4, 2.4), (9.0, 7.0, .9), "plate", dt=0)
            for sx in (-1, 1): cb(P(sx * 3.0, 1.6, 2.0), (2.2, 2.2, .5), "glow")
    elif kind == "squid":
        cb(P(0, 2.4, 1.2), (8.0, 6.6, 7.4), "body")
        cb(P(0, -1.0, -.4), (8.8, 6.0, 8.6), "body", dt=-1)                                                                 # the head under the mantle (it holds the player's own)
        for k in range(5): cb(P(0, 5.2 + k * 2.4, 3.0 + k * 1.2), (7.4 - k * 1.2, 2.6, 6.6 - k * 1.0), "body", dt=(k % 2) - 1)
        for sx in (-1, 1): cb(P(sx * 3.6, 2.4, 1.0), (1.4, 1.4, 3.4), "plate", dt=1)
        cb(P(0, 2.2, -3.6), (6.4, 3.2, 1.4), "back", rot=(-14, 0, 0), dt=-1, faces=FRONT)                                    # the lip of the mantle, hanging over the face
        for sx in (-1, 1):                                                                                                 # no face: a great slit eye on each side
            cb(P(sx * 4.45, -.2, -1.4), (.4, 2.6, 3.8), "dark", faces=(0 if sx > 0 else 1,))
            cb(P(sx * 4.6, -.2, -1.4), (.4, 1.2, 3.0), "eye", faces=(0 if sx > 0 else 1, 2, 3))
            cb(P(sx * 4.5, 1.3, -1.4), (1.0, .9, 4.4), "back", rot=(sx * 0, 0, sx * -12), dt=-1, faces=(0, 1, 2, 3, 5))
        for k in range(4):                                                                                                 # short arms hang where a face would be
            x = -2.7 + k * 1.8
            sgm(P(x, -1.6, -4.6), P(x * 1.15, -5.2, -5.8), 1.5, 1.4, "body" if k % 2 else "belly", pad=.3, dt=k % 2)
            sgm(P(x * 1.15, -5.2, -5.8), P(x * 1.3, -7.4 - (k % 2), -4.8), 1.0, 1.0, "belly" if k % 2 else "body", pad=.2)
        cb(P(0, -3.4, -4.0), (2.4, 2.0, 1.8), "claw", rot=(18, 0, 0), dt=-1)                                                 # the beak, behind them
        cb(P(0, -4.6, -4.4), (1.3, 1.5, 1.2), "claw", rot=(18, 0, 0), dt=-1, faces=(0, 1, 3, 5))
    return H

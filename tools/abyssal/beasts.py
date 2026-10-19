"""Why: the non-humanoid abyssal looks (Demogorgon, Beholder, Elder Brain, Kraken, the dragons, Lolth, the demons) are full GeoLite bodies, not player skins, and share a handful of shapes and gaits:
a rounded blob built from stacked slabs, a toothed jaw, an eyeball, an eyestalk, a tentacle, and the motion functions for a quadruped, a biped and a floating body. They are here once so each look file
only says what the creature IS.
"""
import math
import lib
from lib import (Creature, AnimRig, over, on, rgb, shade, mix, flat, bone, cloth, metal, plate, scales, flesh, fur, chitin, emissive, glowfill, wisp, halo, eyeball, membrane, brain,
                 beam, curve, spike, tilt_rot, horn, rotpt, TAU, keys, smooth, lerp, pulse, humanoid_motion)


def blob(c, bone_, center, size, paint, bands=6, k=2, octagon=True):
    """A rounded mass of stacked slabs: `size` = (w, h, d) of the whole, `bands` slabs from bottom to top, each an octagon (a slab plus the same slab turned 45 degrees) whose width follows
    the circle, so the silhouette is round from every side. Slabs are 'gently' overlapped so there are no gaps."""
    cx, cy, cz = center
    w, h, d = size
    for i in range(bands):
        y0 = cy - h / 2 + h * i / bands
        t = ((i + 0.5) / bands) * 2 - 1
        f = math.sqrt(max(0.0, 1 - t * t)) * 0.92 + 0.08
        ww, dd, hh = w * f, d * f, h / bands + 0.25
        c.box(bone_, (cx - ww / 2, y0, cz - dd / 2), (ww, hh, dd), paint, k=k)
        if octagon and ww > 3:
            r = min(ww, dd) / 1.4142
            c.box(bone_, (cx - r / 2 * 1.0 - (ww - r) * 0.0, y0 + 0.02, cz - r / 2), (r * 1.0, hh - 0.04, r * 1.0), paint, k=k, rot=(0, 45, 0), pivot=(cx, y0, cz))


def eye(c, bone_, origin, size, iris, glow=True, facing="front", pupil="#101010", white="#e8e0d0", vein="#a03030"):
    """A flat eyeball cube; its iris looks out of the face named `facing`."""
    def f(cv):
        if cv.face == facing:
            eyeball(iris, pupil, white, vein, glow_iris=glow)(cv)
        else:
            flat(white, 0.05)(cv)
    c.box(bone_, origin, size, f, k=2 if max(size) >= 5 else 1)


def teeth_row(c, bone_, start, count, spacing, size, paint, direction=1, rot=None):
    """A row of fangs (small cubes) along x from `start`, pointing down (direction=1) or up (-1)."""
    x, y, z = start
    for i in range(count):
        h = size[1] * (1.0 if i % 2 == 0 else 0.75)
        c.box(bone_, (x + i * spacing - size[0] / 2, y - h if direction > 0 else y, z - size[2] / 2), (size[0], h, size[2]), paint, k=1, rot=rot, pivot=(x + i * spacing, y, z) if rot else None)


def tentacle(c, parent, name, start, direction, lengths, widths, paint, tilt=40.0, curl=14.0, axis="-y", tip=None, k=2, depth=None, sucker=None):
    """A tentacle leaving `start` towards the horizontal direction (dx, dz), tipped out by `tilt` degrees and curling `curl` degrees per segment further; bones name_0 .. name_n-1."""
    dx, dz = direction
    sign = -1 if axis[0] == "-" else 1
    first = tilt_rot(dx * sign, dz * sign, tilt)
    cl = tilt_rot(dx * sign, dz * sign, curl)
    names = []
    px, py, pz = start
    par = parent
    for i, (ln, wd) in enumerate(zip(lengths, widths)):
        bn = "%s_%d" % (name, i)
        c.bone(bn, par, pivot=(px, py, pz), rotation=first if i == 0 else cl)
        dp = depth[i] if depth else wd
        org = (px - wd / 2, py - ln if sign < 0 else py, pz - dp / 2)
        pnt = tip if (tip and i == len(lengths) - 1) else paint
        c.box(bn, org, (wd, ln, dp), pnt, k=k)
        if sucker and i < len(lengths) - 1:
            c.box(bn, (px - 0.4, py - ln * 0.5 if sign < 0 else py + ln * 0.4, pz - dp / 2 - 0.3), (0.8, 0.8, 0.5), sucker, k=1)
        py += sign * ln
        names.append(bn)
        par = bn
    return names


def stalk(c, parent, name, start, direction, lengths, widths, paint, tilt=35.0, curl=12.0, ball=None, ball_size=3.0):
    """An eyestalk rising from `start` (+y), leaning outward towards (dx, dz), with an eyeball (or other tip) on the end."""
    names = tentacle(c, parent, name, start, direction, lengths, widths, paint, tilt=tilt, curl=curl, axis="+y", k=1)
    if ball is not None:
        last = names[-1]
        c.box(last, (start[0] - ball_size / 2, start[1] + sum(lengths) - 0.3, start[2] - ball_size / 2), (ball_size, ball_size, ball_size), ball, k=1)
    return names


def sway_names(R, names, axis=0, amp=4.0, cyc=1, lag=0.12, axis2=None, amp2=None, phase=0.0):
    for i, n in enumerate(names):
        R.sway(n, axis, amp, cyc, phase - lag * i)
        if axis2 is not None:
            R.sway(n, axis2, amp2 if amp2 is not None else amp, cyc, phase - lag * i + 0.25)


# ------------------------------------------------------------------------------------------------ gaits
def merge(P, bone_, kind, v):
    d = P.setdefault(bone_, {})
    cur = d.get(kind, [0.0, 0.0, 0.0] if kind != "scale" else [1.0, 1.0, 1.0])
    d[kind] = [a * b for a, b in zip(cur, v)] if kind == "scale" else [a + b for a, b in zip(cur, v)]


def quad_motion(legs, neck=None, tail=None, wings=None, body="body", stride=22.0, hover=0.0, bob=0.6, rear=14.0, extra=None, lunge=22.0, heads=None):
    """A four-legged beast. legs = (front-left, front-right, back-left, back-right) bones; neck = the base bones of its necks (lunge in an attack, rise in a cast); wings = [(bone, side)];
    heads = [bone] that dip on an attack. extra(clip, t, L) adds the look's own."""
    fl, fr, bl, br = legs

    def motion(clip, t, L):
        P = {}
        ph = TAU * t / L
        k = t / L
        if clip == "idle":
            merge(P, body, "rot", (1.2 * math.sin(ph), 0, 0))
            merge(P, "root", "pos", (0, hover + bob * 0.6 * math.sin(2 * ph), 0))
            for n in neck or []:
                merge(P, n, "rot", (2.0 * math.sin(ph + 0.6), 0, 0))
        elif clip in ("walk", "run"):
            a = stride * (1.0 if clip == "walk" else 1.4)
            s = math.sin(ph)
            merge(P, fl, "rot", (a * s, 0, 0))
            merge(P, br, "rot", (a * s, 0, 0))
            merge(P, fr, "rot", (-a * s, 0, 0))
            merge(P, bl, "rot", (-a * s, 0, 0))
            merge(P, body, "rot", (0, 0, 2.5 * s))
            merge(P, "root", "pos", (0, hover + bob * abs(math.sin(ph)) * 1.5, 0))
            for n in neck or []:
                merge(P, n, "rot", (3 * math.sin(2 * ph), 0, 0))
        elif clip == "attack":
            lg = keys(k, [(0, 0.0), (0.2, -0.6), (0.4, 1.0), (0.7, 0.2), (1, 0.0)])
            for n in neck or []:
                merge(P, n, "rot", (-lunge * lg, 0, 0))
            for h in heads or []:
                merge(P, h, "rot", (-14 * lg, 0, 0))
            merge(P, body, "rot", (-5 * lg, 0, 0))
            merge(P, "root", "pos", (0, hover, -3.0 * max(0.0, lg)))
            merge(P, fl, "rot", (-30 * max(0.0, lg), 0, 0))
            merge(P, fr, "rot", (-30 * max(0.0, lg), 0, 0))
        elif clip in ("cast", "cast_big"):
            up = 1.0 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.15), (0.9, 0.0), (1, 0)])
            if clip == "cast":
                up = 0.7 + 0.3 * math.sin(ph)
            merge(P, body, "rot", (rear * up, 0, 0))
            merge(P, "root", "pos", (0, hover + 1.5 * up, 0))
            for n in neck or []:
                merge(P, n, "rot", (10 * up, 0, 0))
            for w, sd in wings or []:
                merge(P, w, "rot", (0, 0, 22 * sd * up))
            merge(P, fl, "rot", (-40 * up, 0, 0))
            merge(P, fr, "rot", (-40 * up, 0, 0))
        elif clip == "hurt":
            h = math.sin(math.pi * k)
            merge(P, body, "rot", (-10 * h, 0, 0))
            for n in neck or []:
                merge(P, n, "rot", (-14 * h, 0, 0))
            merge(P, "root", "pos", (0, hover, 1.5 * h))
        elif clip == "death":
            f = smooth(k / 0.7)
            merge(P, "root", "rot", (0, 0, 78 * f))
            merge(P, "root", "pos", (0, hover * (1 - f) - 2 * f, 0))
            for n in neck or []:
                merge(P, n, "rot", (-30 * f, 0, 0))
            for w, sd in wings or []:
                merge(P, w, "rot", (0, 0, -20 * sd * f))
            for lg_ in (fl, fr, bl, br):
                merge(P, lg_, "rot", (35 * f, 0, 0))
        if extra:
            for b, items in extra(clip, t, L).items():
                for kind, v in items.items():
                    merge(P, b, kind, v)
        return P
    return motion


def float_motion(body="body", hover=6.0, bob=1.4, tilt=3.0, extra=None, attack_dist=-5.0, rise=3.0):
    """A body that hovers and drifts instead of walking (the Beholder, the Elder Brain)."""
    def motion(clip, t, L):
        P = {}
        ph = TAU * t / L
        k = t / L
        if clip == "idle":
            merge(P, "root", "pos", (0, hover + bob * math.sin(ph), 0))
            merge(P, body, "rot", (tilt * 0.4 * math.sin(ph + 1), 0, tilt * 0.5 * math.sin(ph)))
        elif clip in ("walk", "run"):
            f = 1.0 if clip == "walk" else 1.6
            merge(P, "root", "pos", (0, hover + bob * math.sin(2 * ph) * f, 0))
            merge(P, body, "rot", (tilt * 2.0 * f, 0, tilt * math.sin(ph)))
        elif clip == "attack":
            lg = keys(k, [(0, 0.0), (0.22, -0.5), (0.42, 1.0), (0.7, 0.15), (1, 0.0)])
            merge(P, "root", "pos", (0, hover, attack_dist * lg))
            merge(P, body, "rot", (-12 * lg, 0, 0))
        elif clip in ("cast", "cast_big"):
            up = 0.6 + 0.4 * math.sin(ph) if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0.0), (1, 0)])
            merge(P, "root", "pos", (0, hover + rise * up, 0))
            merge(P, body, "rot", (-10 * up, 0, 0))
        elif clip == "hurt":
            h = math.sin(math.pi * k)
            merge(P, body, "rot", (-16 * h, 0, 8 * h))
            merge(P, "root", "pos", (0, hover, 2.0 * h))
        elif clip == "death":
            f = smooth(k / 0.75)
            merge(P, "root", "pos", (0, hover * (1 - f), 0))
            merge(P, body, "rot", (-25 * f, 0, 40 * f))
        if extra:
            for b, items in extra(clip, t, L).items():
                for kind, v in items.items():
                    merge(P, b, kind, v)
        return P
    return motion

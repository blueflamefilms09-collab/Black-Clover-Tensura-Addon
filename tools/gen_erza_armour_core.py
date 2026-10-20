"""Shared building blocks of the Erza armour remake (1.99.0): lofted tubes, curved plates, spikes, blades, gems, straps, and the per-armour context.

Everything here builds on tools/erza_armour_kit.py (shells on parametric surfaces, oriented boxes) and tools/erza_armour_paint.py (materials). Nothing writes a file by itself: a builder returns the finished
`Piece`, and `Piece.save()` writes it.

The vocabulary:
  Loft(stations)          a tube lofted through stations (y, half width a, front depth bf, back depth bb, exponent n[, centre dx, dz]); the exponent turns an ellipse (2) into a rounded box (4 to 8), so a
                          plate can wrap the player's boxy body without letting a corner of the skin through
  Loft.surf(th0, th1, y0, y1)   the parametric surface of a patch of the tube: u = the angle round the vertical axis (0 = the front, turning toward +x, 180 = the back), v = the height
  shell_auto(...)         a curved plate on a surface, the number of panels chosen from the plate's size (CELL model pixels per panel)
  spike / blade / feather chains of tapered oriented boxes that follow a bent centre line
  gem / rivet / strap     the small hard parts
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from erza_armour_kit import (Piece, Mat, Decal, Anim, V3, superellipse, unit, frame, euler_of, rot_xyz, mx, mv, mrot, sine_rot, spin_y, loop_rot, SKIN)  # noqa: E402,F401

CELL = 1.3                        # the model pixels one panel of a curved plate covers (the Djinn's quads are about this size)
DENSITY = float(os.environ.get("ERZA_D", "14"))      # texels per model pixel (the Djinn atlas: 5.4 to 7.3); ERZA_D=8 builds a quick draft


def lerp(a, b, t):
    return a + (b - a) * t


def sstep(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


class Loft:
    """A tube through stations (y, a, bf, bb, n, dx, dz); everything between two stations is interpolated smoothly. Angles in degrees."""

    def __init__(self, stations, cx=0.0, cz=0.0):
        self.st = sorted([tuple(s) + (0.0,) * (7 - len(s)) for s in stations])
        self.cx, self.cz = cx, cz

    def at(self, y):
        st = self.st
        if y <= st[0][0]:
            s = st[0]
        elif y >= st[-1][0]:
            s = st[-1]
        else:
            for i in range(len(st) - 1):
                if st[i][0] <= y <= st[i + 1][0]:
                    t = sstep((y - st[i][0]) / max(1e-9, st[i + 1][0] - st[i][0]))
                    s = tuple(lerp(st[i][k], st[i + 1][k], t) for k in range(7))
                    break
        return self.cx + s[5], self.cz + s[6], s[1], s[2], s[3], s[4]

    def point(self, th_deg, y, dr=0.0):
        """A point on the tube at the angle th (degrees) and height y; dr pushes it outward by dr model pixels."""
        cx, cz, a, bf, bb, n = self.at(y)
        x, z = superellipse(math.radians(th_deg), a + dr, bf + dr, bb + dr, n)
        return V3((cx + x, y, cz + z))

    _cache = None

    def _curve(self, y):
        """The cross-section at height y sampled over two turns: (degrees, cumulative arc length). Used to space the panels evenly along the curve."""
        if self._cache is None:
            self._cache = {}
        key = round(y, 3)
        r = self._cache.get(key)
        if r is None:
            cx, cz, a, bf, bb, n = self.at(y)
            th = np.radians(np.linspace(-360.0, 360.0, 2881))
            e = 2.0 / n
            sn, cs = np.sin(th), np.cos(th)
            x = a * np.sign(sn) * np.abs(sn) ** e
            z = -np.where(cs >= 0, bf, bb) * np.sign(cs) * np.abs(cs) ** e
            L = np.concatenate([[0.0], np.cumsum(np.hypot(np.diff(x), np.diff(z)))])
            r = (np.degrees(th), L)
            self._cache[key] = r
        return r

    def theta_of(self, y, th0, th1, u):
        """The angle (degrees) at the fraction u of the ARC LENGTH between th0 and th1 at height y (a superellipse is flat in front: equal angles are not equal lengths)."""
        deg, L = self._curve(y)
        s0, s1 = np.interp(th0, deg, L), np.interp(th1, deg, L)
        return float(np.interp(s0 + (s1 - s0) * u, L, deg))

    def surf(self, th0, th1, y0, y1, extra=None, dr=0.0):
        """The surface of a patch: u from th0 to th1 (even along the arc), v from y0 to y1; `dr` (a number or a function of (theta radians, y)) pushes it outward."""
        def S(u, v):
            y = y0 + (y1 - y0) * v
            th = self.theta_of(y, th0, th1, u)
            P = self.point(th, y, dr(math.radians(th), y) if callable(dr) else dr)
            if extra is not None:
                P = P + V3(extra(math.radians(th), y))
            return P
        return S

    def surf_rows(self, th0, th1, ys, extra=None, dr=0.0):
        """Like surf, but the rows of the grid sit at the heights `ys` (so a dome gets thin rows where it curves fast); v = k / (len(ys) - 1) is row k."""
        n = len(ys) - 1

        def S(u, v):
            y = float(np.interp(v * n, np.arange(n + 1), ys))
            th = self.theta_of(y, th0, th1, u)
            P = self.point(th, y, dr(math.radians(th), y) if callable(dr) else dr)
            if extra is not None:
                P = P + V3(extra(math.radians(th), y))
            return P
        return S

    def ref(self):
        return lambda P: V3((self.at(P[1])[0], P[1], self.at(P[1])[1]))


def mirror_surf(S, side):
    """The same surface mirrored for the left side of a pair."""
    if not side:
        return S
    return lambda u, v: V3((-1, 1, 1)) * np.asarray(S(u, v))


def mirror_ref(ref, side):
    if not side or ref is None:
        return ref
    if callable(ref):
        return lambda P: V3((-1, 1, 1)) * np.asarray(ref(V3((-1, 1, 1)) * np.asarray(P)))
    return V3((-1, 1, 1)) * np.asarray(ref)


def measure(S, which, n=24):
    """Length of the surface along u (which=0, at v = 0.5) or along v (which=1, at u = 0.5)."""
    tot, prev = 0.0, None
    for i in range(n + 1):
        t = i / float(n)
        P = np.asarray(S(t, 0.5) if which == 0 else S(0.5, t), np.float64)
        if prev is not None:
            tot += float(np.linalg.norm(P - prev))
        prev = P
    return tot


def counts(S, cell=CELL, nu=None, nv=None):
    return (nu or max(1, int(round(measure(S, 0) / cell))), nv or max(1, int(round(measure(S, 1) / cell))))


def shell_auto(p, bone, S, mat, ref=None, cell=CELL, nu=None, nv=None, **kw):
    """A curved plate with the panel count taken from its size."""
    a, b = counts(S, cell, nu, nv)
    return p.shell(bone, S, a, b, mat, ref=ref, **kw)


def loft_rows(p, bone, loft, th0, th1, y0, y1, mat, cell=CELL, extra=None, rim="tb", thick=0.55, rows=None, decals=(), edges=None, **kw):
    """A patch of a lofted tube built row by row, each row with as many panels as its own circumference needs (a dome narrows toward its top, so the rows do too)."""
    n = rows or max(1, int(round((y1 - y0) / cell)))
    ys = list(edges) if edges else [y0 + (y1 - y0) * i / n for i in range(n + 1)]
    made = []
    for i in range(len(ys) - 1):
        ya, yb = ys[i], ys[i + 1]
        S = loft.surf(th0, th1, ya, yb, extra)
        nu = max(2, int(round(measure(S, 0) / cell)))
        r = ("b" if i == 0 and "b" in rim else "") + ("t" if i == len(ys) - 2 and "t" in rim else "") + ("lr" if abs(th1 - th0) < 359 else "")
        made += p.shell(bone, S, nu, 1, mat, ref=loft.ref(), rim=r, thick=thick, decals=decals if i == 0 else (), **kw)
    return made


def flat_patch(p, bone, centre, wx, wz, y, mat, nu=2, nv=2, up=True, thick=0.5, **kw):
    """A flat horizontal plate (the top of a helm, a pauldron cap): wx x wz model pixels centred on (x, z), facing up (or down)."""
    cx, cz = centre
    def S(u, v):
        return V3((cx - wx / 2 + wx * u, y, cz + wz / 2 - wz * v))
    return p.shell(bone, S, nu, nv, mat, ref=V3((cx, y - (5 if up else -5), cz)), thick=thick, rim="", **kw)


# ====================================================================================================================== small hard parts
def spike(p, bone, base, direction, length, width, mat, segs=4, bend=(0, 0, 0), thick=0.8, normal=None, taper=0.1, pad=0.0, decals=None):
    """A tapered spike / horn / claw of `segs` boxes along a centre line that curves toward `bend` (a vector added to the direction along the length). Returns the tip point."""
    d0 = unit(direction)
    b = np.asarray(bend, np.float64)
    pos = np.asarray(base, np.float64)
    seg = length / segs
    nrm = np.asarray(normal if normal is not None else (0.0, 0.0, -1.0), np.float64)
    for i in range(segs):
        t0, t1 = i / segs, (i + 1) / segs
        d = unit(d0 + b * (t0 + t1) / 2.0)
        w = width * (1.0 - (1.0 - taper) * t0) * (1.0 - (1.0 - taper) * (t1 - t0) * 0.5)
        w1 = width * (1.0 - (1.0 - taper) * t1)
        nxt = pos + d * seg
        p.between(bone, pos - d * pad, nxt + d * 0.12, (w + w1) / 2.0, (w + w1) / 2.0 * thick, mat, normal=nrm)
        pos = nxt
    return pos


def blade(p, bone, root, direction, normal, length, width, mat, segs=3, bend=(0, 0, 0), thick=0.35, tip=0.25, decals=None):
    """A flat tapering blade / feather / fin: `segs` thin boxes from `root` along `direction` (bending toward `bend`), as wide as `width` at the root and `tip` of that at the end. The flat side faces `normal`."""
    d0 = unit(direction)
    b = np.asarray(bend, np.float64)
    pos = np.asarray(root, np.float64)
    seg = length / segs
    made = []
    for i in range(segs):
        t0, t1 = i / segs, (i + 1) / segs
        d = unit(d0 + b * (t0 + t1) / 2.0)
        w = width * lerp(1.0, tip, (t0 + t1) / 2.0)
        nxt = pos + d * seg
        made.append(p.between(bone, pos, nxt + d * 0.1, w, thick, mat, normal=normal))
        pos = nxt
    return made, pos


def gem(p, bone, centre, size, mat, eul=(0, 0, 0), facing=(0, 0, -1)):
    """A cut gem: a flattened box turned 45 degrees on its point (a lozenge seen from the front) with a smaller one in it for the table."""
    c = np.asarray(centre, np.float64)
    M = frame((0, 1, 0), facing, 1, 2)
    R45 = rot_xyz(0, 0, 45)
    Mt = M @ R45
    cu = p.cbox(bone, c, (size, size, size * 0.55), mat, eul=euler_of(Mt))
    return cu


def rivet(p, bone, centre, r, mat, facing=(0, 0, -1)):
    M = frame((0, 1, 0), facing, 1, 2)
    return p.cbox(bone, centre, (r, r, r * 0.8), mat, eul=euler_of(M))


def strap(p, bone, pts, width, thick, mat, normal=(0, 0, -1)):
    """A strap through a polyline: one box per segment."""
    out = []
    for a, b in zip(pts[:-1], pts[1:]):
        out.append(p.between(bone, a, b, width, thick, mat, normal=normal))
    return out


def sshell(p, bone, loft, side, th0, th1, y0, y1, mat, extra=None, dr=0.0, **kw):
    """A shell on the right-hand loft `loft`, mirrored for the left side (side = 1)."""
    S = mirror_surf(loft.surf(th0, th1, y0, y1, extra, dr), side)
    return shell_auto(p, bone, S, mat, ref=mirror_ref(loft.ref(), side), **kw)


def V(x, y, z, side=0):
    """A right-side authored point / vector, mirrored for the left."""
    return V3((mx(x, side), y, z))


def ring_of(p, bone, centre, radius, n, make):
    """Calls make(i, point, outward) for n points round a horizontal circle."""
    out = []
    for i in range(n):
        a = 2 * math.pi * i / n
        d = V3((math.sin(a), 0.0, -math.cos(a)))
        out.append(make(i, V3(centre) + d * radius, d))
    return out


# ====================================================================================================================== the context of one armour
class Ctx:
    """The armour being built: its id, palette (a dict of Mats) and spec (the style names and numbers of each piece)."""

    def __init__(self, aid, pal, spec, density=DENSITY):
        self.id, self.key, self.pal, self.spec, self.D = aid, "erza_" + aid, pal, spec, density
        self.made = []

    def piece(self, name, skin=None, density=None):
        return Piece(self.key, name, density=density or self.D, skin=skin)

    def done(self, p, anim=None):
        r = p.save(anim)
        self.made.append(r)
        return r

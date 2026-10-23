"""Spirit Dive MERGE 2 (4.x): the SLIM spirit models that merge with the diver: smooth low-poly meshes authored by hand (tapered limbs, curved bodies, real wings, fins and tails), NOT stacked cubes.

    python3 -B tools/gen_sdm2_models.py                 # writes the meshes and docs/previews/spirit_dive_merge2/models.txt
    python3 -B tools/gen_sdm2_models.py --view out.png  # also the turn-tables of the four spirits (software rasteriser: debugging and the previews)

Spirits (kinds: `char` = the canon pose of the character tier, `std` = the standard tier, `core` = the faint silhouette round the torso):
  Sylph       slender wind fairy, big leaf / dragonfly wings, flowing hair; char: wings spread; std: wings folded up behind the back
  Salamander  orange four legged winged lizard, lean and serpentine: long curved neck and tail, small wings; char: coiled, neck raised, tail curled round
  Undine      water mermaid: a long S-curved tail with a scalloped fluke, flowing hair and ribbons, fins
  Gnome       small sturdy earth spirit, round body, rock shoulder pads, a crystal cluster on the back (standard only)

Writes src/main/resources/assets/nusmp/sdm2_meshes/<element>_<kind>.txt in the quad line format of book/SpiritDiveWind3Mesh (the loader getIn("sdm2_meshes", name)):
  q <layer> (x y z u v) x 4  nx ny nz  r g b
  layer 0 = the solid body (drawn additive at the ghost's alpha), layer 1 = the soft glow shell (drawn additive, lower alpha: wing margins, fluke, flame, crystals)
Mesh space as the old sdm meshes: model pixels, origin = the centre of the bounding box, +y DOWN, front = -z, the longest side normalised to NORM px. The painter scales the mesh.
Budget: <= 900 vertices a ghost at the character tier, <= 500 at the standard tier, the core <= 300 (checked here, by book/SpiritDiveMerge2Rules and the tests).
"""
import math
import os
import sys

import numpy as np

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sdm2_meshes")
DOC = os.path.join(ROOT, "docs", "previews", "spirit_dive_merge2", "models.txt")

NORM = 32.0
UV = (0.30, 0.30, 0.70, 0.70)                  # the middle of aura/sdm_skin.png: plain light (the same texture as the boxy meshes)
COLOR = {"wind": (140, 255, 196), "water": (100, 184, 255), "fire": (255, 112, 34), "earth": (255, 168, 56)}
BUDGET = {"char": 900, "std": 500, "core": 300}

V = np.array


def nrm(a):
    l = float(np.linalg.norm(a))
    return a / l if l > 1e-9 else np.array([0.0, 1.0, 0.0])


# ------------------------------------------------------------------------------------------ the builder
class B:
    """Collects quads (y up, front = -z): (layer, [4 points], normal, gain). Colour is worked out at the end from the normal, the height and the part gain."""

    def __init__(self):
        self.q = []

    def add(self, layer, pts, gain, ref=None, both=False):
        p = [V(x, float) for x in pts]
        n = np.cross(p[2] - p[0], p[3] - p[1])
        if np.linalg.norm(n) < 1e-9:
            n = np.cross(p[1] - p[0], p[3] - p[0])
        if np.linalg.norm(n) < 1e-9:
            n = np.cross(p[2] - p[1], p[3] - p[1])
        if np.linalg.norm(n) < 1e-9:
            return
        n = nrm(n)
        if ref is not None and np.dot(n, sum(p) / 4 - V(ref, float)) < 0:
            p = [p[0], p[3], p[2], p[1]]
            n = -n
        self.q.append((layer, p, n, gain))
        if both:
            self.q.append((layer, [p[0], p[3], p[2], p[1]], -n, gain))

    # -- a tube along a path: rings of `sides` points, radii (rx, ry) per path point, swept with a transported frame
    def tube(self, layer, path, radii, sides, gain=1.0, hint=(1, 0, 0), start=0.0, caps=False):
        path = [V(p, float) for p in path]
        n = len(path)
        rr = [(r, r) if not isinstance(r, (tuple, list)) else (r[0], r[1]) for r in radii]
        tans = []
        for i in range(n):
            a, b = path[max(0, i - 1)], path[min(n - 1, i + 1)]
            tans.append(nrm(b - a))
        h = V(hint, float)
        nv = nrm(h - np.dot(h, tans[0]) * tans[0])
        if np.linalg.norm(nv) < 1e-6:
            nv = nrm(np.cross(tans[0], (0, 0, 1)))
        rings, centers = [], []
        for i in range(n):
            t = tans[i]
            nv = nv - np.dot(nv, t) * t
            nv = nrm(nv)
            bv = np.cross(t, nv)
            pts = []
            for k in range(sides):
                a = start + 2 * math.pi * k / sides
                pts.append(path[i] + nv * (rr[i][0] * math.cos(a)) + bv * (rr[i][1] * math.sin(a)))
            rings.append(pts)
            centers.append(path[i])
        for i in range(n - 1):
            c = (centers[i] + centers[i + 1]) / 2
            for k in range(sides):
                k2 = (k + 1) % sides
                self.add(layer, [rings[i][k], rings[i][k2], rings[i + 1][k2], rings[i + 1][k]], gain, ref=c)
        if caps:
            for end, rg, c, other in ((0, rings[0], centers[0], centers[1]), (1, rings[-1], centers[-1], centers[-2])):
                if max(rr[0 if end == 0 else -1]) > 0.05:
                    tip = c + nrm(c - other) * max(rr[0 if end == 0 else -1]) * 0.8
                    for k in range(sides):
                        k2 = (k + 1) % sides
                        self.add(layer, [rg[k], rg[k2], tip, tip], gain, ref=c)

    def ellipsoid(self, layer, c, rx, ry, rz, rings, sides, gain=1.0, tilt=0.0):
        """An ellipsoid about the vertical axis: `rings` rings between two poles; `tilt` leans it forward (about x)."""
        c = V(c, float)
        path, radii = [], []
        for k in range(rings + 1):
            th = math.pi * k / rings
            y = ry * math.cos(th)
            path.append(c + V((0.0, y * math.cos(tilt), -y * math.sin(tilt))))
            s = max(math.sin(th), 0.0)
            radii.append((rx * s, rz * s))
        self.tube(layer, path, radii, sides, gain, hint=(1, 0, 0))

    # -- an open surface s (along) x v (across): grid[i][j] points; both faces by default (the additive type may cull the back)
    def sheet(self, layer, grid, gain=1.0, both=True):
        for i in range(len(grid) - 1):
            for j in range(len(grid[i]) - 1):
                self.add(layer, [grid[i][j], grid[i + 1][j], grid[i + 1][j + 1], grid[i][j + 1]], gain, both=both)

    def count(self, layers=(0, 1)):
        return sum(1 for q in self.q if q[0] in layers) * 4


def spline(points, n, closed=False):
    """Catmull-Rom through the points: n samples in all (including both ends)."""
    P = [V(p, float) for p in points]
    ext = [2 * P[0] - P[1]] + P + [2 * P[-1] - P[-2]]
    out = []
    segs = len(P) - 1
    for i in range(n):
        u = i / (n - 1) * segs
        s = min(int(u), segs - 1)
        t = u - s
        p0, p1, p2, p3 = ext[s], ext[s + 1], ext[s + 2], ext[s + 3]
        out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t * t * t))
    return out


def spl_r(radii, n):
    """The radii along the same parameter (linear in the control points, smooth enough)."""
    R = [(r, r) if not isinstance(r, (tuple, list)) else tuple(r) for r in radii]
    out = []
    segs = len(R) - 1
    for i in range(n):
        u = i / (n - 1) * segs
        s = min(int(u), segs - 1)
        t = u - s
        t = t * t * (3 - 2 * t)
        out.append((R[s][0] * (1 - t) + R[s + 1][0] * t, R[s][1] * (1 - t) + R[s + 1][1] * t))
    return out


def leaf_wing(b, layer, root, tip, lead_h, trail_h, stations, cells, gain, bend=(0.0, 0.0, 0.0), lift=(0.0, 1.0, 0.0), both=True, widen=1.0, rootw=0.0, back=(0.0, 0.0, 1.0)):
    """A leaf / dragonfly wing from `root` to `tip`: the leading edge bows up by lead_h, the trailing edge sags by trail_h (both zero at the ends), `cells` across; a gentle cup toward `back`."""
    root, tip, up, back = V(root, float), V(tip, float), nrm(V(lift, float)), V(back, float)
    ax = tip - root
    grid = []
    for i in range(stations):
        s = i / (stations - 1)
        base = root + ax * s + V(bend, float) * math.sin(math.pi * s)
        lead = base + up * lead_h * math.sin(math.pi * s) ** 0.9
        trail = base - up * trail_h * widen * (math.sin(math.pi * min(1.0, s * 1.15)) ** 0.9) * (1.0 - 0.35 * s) + up * (-rootw) * (1 - s)
        cup = back * (0.35 * math.sin(math.pi * s))
        row = []
        for j in range(cells + 1):
            v = j / cells
            row.append(lead * (1 - v) + trail * v + cup * (1 - abs(2 * v - 1) * 0.6))
        grid.append(row)
    b.sheet(layer, grid, gain, both)


def ribbon(b, layer, path, widths, gain, side=(1, 0, 0), both=True):
    """A flat ribbon along a path (hair lock, trailing ribbon): width across `side`."""
    path = [V(p, float) for p in path]
    sd = V(side, float)
    grid = []
    for i, p in enumerate(path):
        t = nrm(path[min(len(path) - 1, i + 1)] - path[max(0, i - 1)])
        w = nrm(np.cross(t, np.cross(sd, t)))
        w = nrm(sd - np.dot(sd, t) * t) if np.linalg.norm(sd - np.dot(sd, t) * t) > 1e-6 else w
        grid.append([p - w * widths[i], p + w * widths[i]])
    b.sheet(layer, grid, gain, both)


def mirror(f, sign=(1, -1)):
    return [f(s) for s in sign]


def sx(p, s):
    return (p[0] * s, p[1], p[2])


# ------------------------------------------------------------------------------------------ the spirits
def sylph(kind):
    b = B()
    ch, core = kind == "char", kind == "core"
    sd = 4 if core else (6 if ch else 5)
    head_rings = 3 if not ch else 4
    b.ellipsoid(0, (0, 9.1, -0.05), 0.92, 1.02, 0.92, head_rings, sd, 1.05)
    # torso, skirt, legs
    torso = spline([(0, 7.95, -0.02), (0, 7.2, -0.05), (0, 6.2, 0.0), (0, 5.1, 0.05)], 4 if (ch or not core) else 3)
    b.tube(0, torso, spl_r([(0.28, 0.26), (0.62, 0.40), (0.46, 0.34), (0.52, 0.38)], len(torso)), sd, 1.0)
    skirt = [(0, 5.3, 0.05), (0, 4.2, 0.1), (0, 3.0, 0.2)]
    b.tube(0, skirt, [(0.5, 0.38), (1.0, 0.85), (0.78, 0.62)], sd, 0.85)
    if not core:
        for s in (1, -1):
            leg = [sx(p, s) for p in [(0.26, 4.9, 0.05), (0.34, 3.7, -0.05), (0.30, 2.3, 0.15), (0.32, 1.1, -0.1)]]
            b.tube(0, leg, [0.22, 0.2, 0.15, 0.07], 3 if not ch else 4, 0.95, hint=(0, 0, 1))
            if ch:                                                                  # arms open and raised: welcoming
                arm = [sx(p, s) for p in [(0.62, 7.3, -0.05), (1.5, 7.9, -0.35), (2.3, 8.9, -0.55), (2.65, 9.8, -0.6)]]
                b.tube(0, arm, [0.17, 0.15, 0.11, 0.06], 4, 0.95, hint=(0, 0, 1))
            else:
                arm = [sx(p, s) for p in [(0.62, 7.3, -0.05), (1.0, 6.3, -0.2), (0.95, 5.3, -0.5)]]
                b.tube(0, arm, [0.17, 0.13, 0.07], 3, 0.95, hint=(0, 0, 1))
        # hair: ribbons from the crown, flowing back and down
        locks = [(-0.55, 0.0), (0.0, 0.3), (0.55, 0.0), (0.0, -0.4)] if ch else [(-0.4, 0.0), (0.4, 0.0)]
        for ox, drop in locks:
            segs = 5 if ch else 3
            pts = spline([(ox * 0.8, 10.0, 0.25), (ox * 1.5, 9.1, 1.0 + drop * 0.2), (ox * 2.3, 7.4, 1.8), (ox * 2.7 + 0.3, 5.2 + drop, 2.3), (ox * 2.2, 3.0 + drop, 2.0)], segs + 1)
            ribbon(b, 0, pts, [0.42 * (1 - i / (segs + 1.3)) + 0.06 for i in range(segs + 1)], 0.9)
    # wings
    for s in (1, -1):
        if ch:
            up = (0.3 * s, 7.0, 0.45)
            leaf_wing(b, 0, up, (7.4 * s, 11.6, 1.6), 1.7, 2.6, 5, 2, 0.5, bend=(0, 0.7, 0.4), widen=1.15)
            leaf_wing(b, 0, (0.3 * s, 6.5, 0.45), (5.2 * s, 2.4, 1.3), 1.0, 1.8, 5, 2, 0.45, bend=(0.0, -0.2, 0.3), lift=(0, 1, 0), widen=1.0)
            leaf_wing(b, 1, up, (7.9 * s, 12.0, 1.8), 2.0, 3.0, 5, 2, 0.6, bend=(0, 0.7, 0.4), both=False, widen=1.2)
        elif core:
            leaf_wing(b, 0, (0.3 * s, 7.0, 0.45), (1.7 * s, 11.4, 1.1), 0.8, 1.2, 3, 1, 0.5)
        else:
            leaf_wing(b, 0, (0.3 * s, 7.0, 0.45), (2.1 * s, 11.6, 1.2), 0.95, 1.5, 3, 2, 0.5, bend=(0.2 * s, 0.2, 0.2))
            leaf_wing(b, 0, (0.3 * s, 6.5, 0.45), (1.8 * s, 3.4, 1.1), 0.6, 1.0, 3, 2, 0.45, bend=(0.1 * s, 0, 0.2))
            leaf_wing(b, 1, (0.3 * s, 7.0, 0.45), (2.4 * s, 12.0, 1.3), 1.15, 1.8, 3, 1, 0.6, bend=(0.2 * s, 0.2, 0.2), both=False)
            leaf_wing(b, 1, (0.3 * s, 6.5, 0.45), (2.1 * s, 3.0, 1.2), 0.75, 1.2, 3, 1, 0.55, bend=(0.1 * s, 0, 0.2), both=False)
    return b


def salamander(kind):
    b = B()
    ch, core = kind == "char", kind == "core"
    sd = 4 if core else 5
    if ch:                                                                           # coiled: the tail curls round inside, the neck rises in an S, the head looks ahead
        ctrl, rad = [], []
        coil = [(1.4, 1.0, 0.2), (2.4, 1.0, 1.9), (0.2, 1.1, 3.1), (-2.5, 1.2, 1.6), (-2.7, 1.3, -1.0), (-0.6, 1.5, -2.4), (2.2, 1.7, -1.8), (3.5, 1.9, 0.4)]
        crad = [0.04, 0.2, 0.4, 0.58, 0.72, 0.84, 0.92, 0.95]
        neck = [(3.3, 3.0, 2.0 - 3.0), (2.3, 4.6, -3.7), (1.1, 6.2, -3.3), (0.4, 7.6, -3.9), (0.2, 8.3, -4.9), (0.1, 8.0, -6.7), (0.1, 7.7, -7.8)]
        nrad = [(0.82, 0.8), (0.66, 0.64), (0.54, 0.52), (0.48, 0.46), (0.54, 0.48), (0.4, 0.32), (0.12, 0.1)]
        ctrl = coil + neck
        rad = crad + nrad
        n = 13 if not core else 9
    else:
        ctrl = [(-0.7, 5.0, 9.4), (-0.3, 3.3, 8.2), (0.0, 2.0, 6.7), (0.0, 1.5, 5.0), (0, 1.7, 3.2), (0, 1.9, 1.0), (0, 2.1, -0.9), (0, 2.6, -2.5), (0, 3.9, -3.5), (0, 4.9, -4.0),
                (0, 5.4, -4.8), (0, 5.3, -6.3), (0, 5.0, -7.4)]
        rad = [0.04, 0.16, 0.32, 0.52, 0.82, 0.95, 0.9, 0.62, 0.46, 0.42, 0.5, 0.38, 0.1]
        n = 10 if not core else 8
    path = spline(ctrl, n)
    b.tube(0, path, spl_r(rad, n), sd, 1.0, hint=(1, 0, 0))
    if not core:
        # legs: four, tapered, at points along the body
        if ch:
            spots = [(len(path) * 0.30, 1), (len(path) * 0.30, -1), (len(path) * 0.46, 1), (len(path) * 0.46, -1)]
        else:
            spots = [(len(path) * 0.38, 1), (len(path) * 0.38, -1), (len(path) * 0.58, 1), (len(path) * 0.58, -1)]
        for idx, s in spots:
            i = min(len(path) - 1, int(idx))
            p = path[i]
            out = V((s, 0.0, 0.0))
            b.tube(0, [p + out * 0.3, p + out * 1.0 + V((0, -0.5, 0)), p + out * 1.5 + V((0, -1.3, 0.2)), p + out * 1.7 + V((0, -min(p[1] - 0.05, 2.0), -0.2))], [0.36, 0.28, 0.2, 0.14], 3 if not ch else 4, 0.95, hint=(0, 0, 1))
        # horns / crest on the head
        hp = path[-3]
        for s in (1, -1):
            b.tube(0, [hp + V((0.25 * s, 0.35, 0.4)), hp + V((0.55 * s, 0.9, 1.0)), hp + V((0.75 * s, 0.9, 1.8))], [0.17, 0.12, 0.02], 3, 1.15, hint=(0, 1, 0))
        # wings: small, bat-like, a root at the shoulders, two finger spokes
        wp = path[int(len(path) * 0.6)] if not ch else path[int(len(path) * 0.62)]
        for s in (1, -1):
            r0 = wp + V((0.45 * s, 0.7, 0.0))
            if ch:
                leaf_wing(b, 0, tuple(r0), tuple(r0 + V((4.6 * s, 3.4, 1.9))), 0.9, 1.9, 4, 2, 0.55, bend=(0, 0.6, 0.5), widen=1.0)
                leaf_wing(b, 1, tuple(r0), tuple(r0 + V((5.0 * s, 3.8, 2.1))), 1.1, 2.2, 4, 2, 0.6, bend=(0, 0.6, 0.5), both=False)
            else:
                leaf_wing(b, 0, tuple(r0), tuple(r0 + V((2.8 * s, 2.5, 2.2))), 0.7, 1.3, 3, 2, 0.55, bend=(0, 0.3, 0.3))
        # the flame at the tail tip (glow) and a glow along the back
        tp = path[0]
        fl = [tp, tp + V((0, 0.7, -0.1)), tp + V((0.0, 1.8, -0.4)), tp + V((0.0, 3.0, -0.2))] if not ch else [tp, tp + V((0, 0.9, 0.2)), tp + V((0, 2.1, 0.1)), tp + V((0, 3.4, 0.3))]
        b.tube(1, fl, [0.2, 0.62, 0.42, 0.04], 4, 1.2, hint=(1, 0, 0))
    return b


def undine(kind):
    b = B()
    ch, core = kind == "char", kind == "core"
    sd = 4 if core else (6 if ch else 5)
    b.ellipsoid(0, (0, 11.2, 0), 1.05, 1.15, 1.0, 3 if not ch else 4, sd, 1.05)
    torso = spline([(0, 10.0, 0), (0, 9.0, -0.05), (0, 7.4, 0.0), (0, 5.9, 0.05)], 4 if not core else 3)
    b.tube(0, torso, spl_r([(0.38, 0.34), (0.86, 0.55), (0.58, 0.42), (0.82, 0.58)], len(torso)), sd, 1.0)
    # the tail: an S down and forward, to the fluke
    n = 7 if ch else (6 if not core else 4)
    tail = spline([(0, 5.9, 0.05), (-0.7, 4.0, 0.4), (-1.9, 2.1, 1.0), (-1.2, 0.2, 1.5), (0.9, -1.6, 1.3), (2.1, -3.0, 0.5)], n)
    b.tube(0, tail, spl_r([(0.8, 0.58), (0.74, 0.54), (0.58, 0.44), (0.42, 0.34), (0.26, 0.2), (0.12, 0.08)], n), 5 if not core else 4, 0.95)
    end = tail[-1]
    # the fluke: two lobes with a scalloped trailing edge, swept out and down
    for s in (1, -1):
        st = 4
        grid = []
        for i in range(st):
            u = i / (st - 1)
            lead = V((end[0] + s * (0.2 + 4.0 * u), end[1] - 0.2 - 1.5 * u - 0.6 * math.sin(math.pi * u), end[2] + 0.3 * u))
            scal = 0.55 * math.cos(u * math.pi * 3) if i not in (0,) else 0.0
            trail = V((end[0] + s * (0.1 + 3.2 * u), end[1] - 0.9 - 2.8 * u - 1.1 * math.sin(math.pi * u) - 0.2 - scal, end[2] + 0.55 * u))
            tip_pull = (1 - u) ** 0.0
            grid.append([lead, (lead + trail) / 2 + V((0, -0.1, 0.1)), trail])
        grid[-1] = [grid[-1][0], grid[-1][0] + V((s * 0.5, -1.2, 0.3)), grid[-1][0] + V((s * 0.9, -2.4, 0.6))]
        b.sheet(0, grid, 0.55)
        if not core:
            b.sheet(1, [[g[0] + V((0, 0.1, -0.2)), g[1] + V((0, 0.0, -0.2)), g[2] + V((0, -0.5, -0.2))] for g in grid], 0.6, both=False)
    if not core:
        for s in (1, -1):
            sh = (s * 0.62, 9.6, 0.0)
            if ch:
                arm = [(sh[0], sh[1], sh[2]), (s * 1.5, 9.1, -0.45), (s * 2.3, 9.9, -0.95), (s * 2.7, 11.0, -1.0)] if s > 0 else \
                      [(sh[0], sh[1], sh[2]), (s * 1.2, 8.3, -0.2), (s * 1.9, 7.1, -0.5), (s * 2.3, 6.0, -0.1)]
            else:
                arm = [(sh[0], sh[1], sh[2]), (s * 1.0, 8.5, -0.2), (s * 1.0, 7.4, -0.55)]
            b.tube(0, arm, [0.17, 0.14, 0.1, 0.06][: len(arm)], 4 if ch else 3, 0.95, hint=(0, 0, 1))
        # hair: long ribbons flowing down the back in S-curves
        locks = [-0.7, -0.25, 0.25, 0.7] if ch else [-0.45, 0.45]
        for k, ox in enumerate(locks):
            segs = 6 if ch else 3
            ph = 0.7 * k
            pts = [(ox * (0.8 + 0.5 * i / segs) + 0.5 * math.sin(i * 0.9 + ph) * (i / segs), 11.9 - 9.0 * (i / segs) ** 1.05 * (1.0 if ch else 0.8), 0.45 + 0.9 * (i / segs) + 0.4 * math.sin(i * 0.7 + ph)) for i in range(segs + 1)]
            ribbon(b, 0, pts, [0.38 * (1 - i / (segs + 1.2)) + 0.05 for i in range(segs + 1)], 0.9)
        # ear fins and hip fins: scalloped spikes
        for s in (1, -1):
            for a in ((-1, 0, 1) if ch else (-1, 1)):
                base = V((s * 0.88, 11.3 + 0.25 * a, 0.0))
                tipp = V((s * (1.7 + 0.15 * (1 - abs(a))), 11.7 + 0.8 * a, 0.15))
                b.add(0, [base, tipp, tipp, base + V((0, -0.45, 0.0))], 0.8, both=True)
        if ch:
            for s in (1, -1):
                for a in (0, 1):
                    base = V((s * 0.6, 5.7 - 0.7 * a, 0.2 + 0.1 * a))
                    tipp = V((s * (1.7 - 0.3 * a), 5.3 - 1.1 * a, 0.6 + 0.2 * a))
                    b.add(0, [base, tipp, tipp, base + V((0, -0.55, 0.0))], 0.75, both=True)
            for s in (1, -1):                                                     # ribbons trailing from the wrists
                w0 = V((s * 2.7, 11.0, -1.0)) if s > 0 else V((s * 2.3, 6.0, -0.1))
                pts = [tuple(w0 + V((s * 0.5 * i, 0.5 * math.sin(i * 1.2) - 0.1 * i, 0.9 * i))) for i in range(5)]
                ribbon(b, 0, pts, [0.3 * (1 - i / 5.5) + 0.04 for i in range(5)], 0.85, side=(0, 1, 0))
    return b


def gnome(kind):
    b = B()
    core = kind == "core"
    sd = 4 if core else 5
    b.ellipsoid(0, (0, 3.6, 0), 2.0, 2.2, 1.8, 3, sd, 1.0)
    b.ellipsoid(0, (0, 7.0, -0.25), 1.5, 1.4, 1.4, 3, sd, 1.05)
    if not core:
        b.tube(0, [(0, 6.8, -1.2), (0, 6.7, -1.9), (0, 6.5, -2.3)], [0.45, 0.32, 0.03], 4, 1.1, hint=(1, 0, 0))                 # the nose
        beard = spline([(0, 6.0, -1.05), (0, 4.9, -1.7), (0, 3.7, -1.8), (0, 2.8, -1.5)], 4)
        b.tube(0, beard, spl_r([(0.95, 0.35), (0.85, 0.5), (0.55, 0.45), (0.04, 0.04)], 4), 4, 0.85)
        b.tube(0, [(0, 7.9, -0.25), (0, 8.7, -0.2), (0, 9.6, -0.1)], [(1.5, 1.45), (1.05, 1.0), (0.28, 0.28)], 4, 0.9, hint=(1, 0, 0))   # the rock cap
        b.tube(1, [(0, 9.4, -0.1), (0, 10.4, -0.1), (0, 11.5, -0.1)], [0.5, 0.42, 0.03], 3, 1.3, hint=(1, 0, 0))                # its crystal
        for s in (1, -1):
            b.tube(0, [(s * 2.0, 4.6, 0.0), (s * 3.0, 2.4, -0.5)], [0.8, 0.6], 4, 1.0, hint=(0, 0, 1))                          # arms
            b.ellipsoid(0, (s * 2.3, 5.2, 0.0), 1.1, 0.7, 1.0, 2, 4, 0.8)                                                       # rock pads
            b.tube(0, [(s * 0.95, 1.8, 0.0), (s * 1.1, 0.05, -0.35)], [0.85, 0.7], 4, 0.95, hint=(0, 0, 1))                     # legs
        for ox, oz, h, tx in ((0.0, 1.7, 4.2, 0.0), (-1.1, 1.5, 3.1, -0.5), (1.1, 1.5, 3.2, 0.5)):
            b.tube(1, [(ox, 3.8, oz), (ox + tx * 0.5, 3.8 + h * 0.55, oz + 0.5), (ox + tx, 3.8 + h, oz + 0.9)], [0.58, 0.5, 0.03], 3, 1.25, hint=(1, 0, 0))   # crystals on the back
        b.tube(1, [(0, 4.5, -1.75), (0, 4.5, -2.2), (0, 4.5, -2.6)], [0.4, 0.34, 0.02], 3, 1.3, hint=(1, 0, 0))                # the gem on the chest
    return b


BUILD = {"wind": sylph, "water": undine, "fire": salamander, "earth": gnome}
KINDS = {"wind": ("char", "std", "core"), "water": ("char", "std", "core"), "fire": ("char", "std", "core"), "earth": ("std", "core")}


# ------------------------------------------------------------------------------------------ colours, normalising, writing
LIGHT = nrm(V((0.25, 0.45, -0.85)))          # file space (y up): from the front (-z) and above


def finish(b, element):
    pts = np.array([p for q in b.q for p in q[1]])
    lo, hi = pts.min(axis=0), pts.max(axis=0)
    mid, k = (lo + hi) / 2, NORM / float((hi - lo).max())
    base = COLOR[element]
    out = []
    for layer, p, n, gain in b.q:
        c = sum(p) / 4
        lit = max(0.0, float(np.dot(n, LIGHT)))
        tall = (c[1] - lo[1]) / max(1e-6, hi[1] - lo[1])                       # 1 at the top
        shade = 0.5 + 0.5 * lit
        g = shade * (0.82 + 0.28 * tall) * gain
        rgb = tuple(int(max(0, min(255, round(ch * g + (255 - ch) * 0.10 * lit * min(1.0, gain))))) for ch in base)
        q = [np.array([(x[0] - mid[0]) * k, -(x[1] - mid[1]) * k, (x[2] - mid[2]) * k]) for x in p]       # y down in the layer frame
        nn = np.array([n[0], -n[1], n[2]])
        # the flip of y mirrors the winding: restore the outward order
        q = [q[0], q[3], q[2], q[1]]
        out.append((layer, q, nn, rgb))
    return out, (hi - lo) * k


def text(name, quads):
    u0, v0, u1, v1 = UV
    uvs = [(u0, v1), (u0, v0), (u1, v0), (u1, v1)]
    lines = ["# generated by tools/gen_sdm2_models.py (%s): %d quads" % (name, len(quads))]
    for layer, q, n, rgb in quads:
        body = " ".join("%.3f %.3f %.3f %.4f %.4f" % (p[0], p[1], p[2], uvs[i][0], uvs[i][1]) for i, p in enumerate(q))
        lines.append("q %d %s %.3f %.3f %.3f %d %d %d" % (layer, body, n[0], n[1], n[2], rgb[0], rgb[1], rgb[2]))
    return "\n".join(lines) + "\n"


def build_all():
    meshes, rows = {}, []
    for element, kinds in KINDS.items():
        for kind in kinds:
            b = BUILD[element](kind)
            quads, size = finish(b, element)
            name = "%s_%s" % (element, kind)
            v0 = sum(1 for q in quads if q[0] == 0) * 4
            v1 = sum(1 for q in quads if q[0] == 1) * 4
            meshes[name] = quads
            ok = (v0 + v1) <= BUDGET[kind]
            rows.append("%-12s quads %3d  vertices %3d (solid %3d + glow %3d)  budget %3d %s  size %5.1f x %5.1f x %5.1f px" % (name, len(quads), v0 + v1, v0, v1, BUDGET[kind], "ok" if ok else "OVER", size[0], size[1], size[2]))
    return meshes, rows


# ------------------------------------------------------------------------------------------ a small software rasteriser (turn-tables)
def render(quads, yaw, size=300, pitch=10.0, scale=None, bg=(10, 12, 20)):
    from PIL import Image, ImageDraw
    S = 2
    im = Image.new("RGB", (size * S, size * S), bg)
    dr = ImageDraw.Draw(im, "RGBA")
    a, p = math.radians(yaw), math.radians(pitch)
    ca, sa, cp, sp = math.cos(a), math.sin(a), math.cos(p), math.sin(p)
    sc = (scale or size / 44.0) * S

    def proj(v):
        x, y, z = v[0] * ca + v[2] * sa, v[1], -v[0] * sa + v[2] * ca          # yaw about y
        y2, z2 = y * cp - z * sp, y * sp + z * cp                               # pitch about x (y down: positive pitch looks from above)
        return x, y2, z2
    items = []
    for layer, q, n, rgb in quads:
        pp = [proj(v) for v in q]
        nx, ny, nz = proj(n)
        items.append((sum(v[2] for v in pp) / 4, layer, pp, rgb, (nx, ny, nz)))
    items.sort(key=lambda t: -t[0])
    for z, layer, pp, rgb, nn in items:
        poly = [(size * S / 2 + v[0] * sc, size * S / 2 + v[1] * sc) for v in pp]
        face = nn[2] < 0
        if layer == 1:
            dr.polygon(poly, fill=rgb + (70,))
        else:
            dr.polygon(poly, fill=rgb + ((235 if face else 120),), outline=tuple(min(255, int(c * 0.55)) for c in rgb) + (200,))
    return im.resize((size, size), Image.LANCZOS)


def sheet(meshes, path, yaws=(0, 45, 90, 135, 180, 225, 270, 315)):
    from PIL import Image, ImageDraw, ImageFont
    names = [n for n in meshes if not n.endswith("_core")]
    cell = 260
    out = Image.new("RGB", (cell * len(yaws), cell * len(names)), (10, 12, 20))
    d = ImageDraw.Draw(out)
    try:
        f = ImageFont.truetype("DejaVuSans.ttf", 14)
    except OSError:
        f = ImageFont.load_default()
    for r, n in enumerate(names):
        for c, y in enumerate(yaws):
            out.paste(render(meshes[n], y, cell), (c * cell, r * cell))
            d.text((c * cell + 6, r * cell + cell - 20), "yaw %d" % y, fill=(255, 235, 150), font=f)
        d.text((6, r * cell + 4), n, fill=(255, 235, 150), font=f)
    out.save(path)


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.dirname(DOC), exist_ok=True)
    meshes, rows = build_all()
    for name, quads in meshes.items():
        with open(os.path.join(OUT, name + ".txt"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text(name, quads))
    with open(DOC, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("Spirit Dive MERGE 2: the slim spirit meshes of tools/gen_sdm2_models.py (layer 0 solid + layer 1 glow shell; both drawn additive)\n" + "\n".join(rows) + "\n")
    print("\n".join(rows))
    if "--view" in sys.argv:
        sheet(meshes, sys.argv[sys.argv.index("--view") + 1])


if __name__ == "__main__":
    main()

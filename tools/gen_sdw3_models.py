"""Spirit Dive WIND remake (three looks, 4.x): the REAL 3D models of the spirit weapons and pieces, authored in the project's mesh pipeline (the same `q` quad lines and the
loft / box / strip vocabulary of tools/gen_weapon_models.py), deterministic.

    python3 tools/gen_sdw3_models.py

Writes
  src/main/resources/assets/nusmp/sdw3_meshes/<name>.txt      one mesh each (see the format below)
  src/main/resources/assets/nusmp/textures/aura/sdw3_skin.png  the 64 x 64 atlas the meshes are mapped on (solid, cube face, leaf panel, bevel)
  docs/previews/spirit_dive_wind3/models.txt                   vertex counts per mesh and layer (the numbers the budgets use)

The models are VISUAL ONLY pieces of the Wind Spirit Dive (worn aura pieces drawn by the dive painter for the diver; not items). Units are model pixels (1/16 block). Mesh space: y runs along the
piece (hilt / grip / root at y <= 0, tip at +y), x across it, z through it. The painter turns the +y axis to any direction. Mesh file format:
  q <layer> (x y z u v) x 4  nx ny nz  r g b        a quad (u, v in 0..1 of the atlas); r g b: the tint (0..255)
  layers: 0 solid body (drawn emissive, face-shaded), 1 soft glow shell (drawn additive), 2 rings and ornaments (solid, dropped at low detail / for the standard tier)

Pieces: sword (Spirit of Zephyr: hexagonal blade with fuller, cross-guard with up-curved quillons, wrapped grip, gem pommel, two crossed rings at the hilt), bow (Spirit of Boreas: two leaf limbs
with cross ornaments, riser, string), arrow (arrow of light), spear (a second blade / spear with a leaf head and a cross), blade (small spirit blade / arrow orbiter), feather (the long feather
blade of the Saint spirit wings), cube (a glowing cube with a bright frame), panel (a thin leaf panel of the voxel wing), plate (an armoured plate of light), crown (a ring of 3D arrow bars).
"""
import math
import os

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "sdw3_meshes")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "aura", "sdw3_skin.png")
SOFT = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "aura", "sdw3_soft.png")
DOC = os.path.join(ROOT, "docs", "previews", "spirit_dive_wind3", "models.txt")

# atlas regions (u0, v0, u1, v1 in 0..1 of the 64 x 64 skin)
WHITE = (0 / 64, 0 / 64, 8 / 64, 8 / 64)
CUBE = (8 / 64, 0 / 64, 24 / 64, 16 / 64)
PANEL = (24 / 64, 0 / 64, 40 / 64, 16 / 64)
BEVEL = (40 / 64, 0 / 64, 56 / 64, 16 / 64)       # u across: bright edge, mint centre, bright edge
GRAD = (0 / 64, 16 / 64, 16 / 64, 32 / 64)        # v along: pale at the root to bright white at the tip

MINT = (176, 255, 214)
PALE = (240, 255, 248)
TEAL = (96, 232, 188)
DEEP = (58, 196, 160)
WHT = (255, 255, 255)


def sub(a, b): return (a[0] - b[0], a[1] - b[1], a[2] - b[2])
def add(a, b): return (a[0] + b[0], a[1] + b[1], a[2] + b[2])
def mul(a, k): return (a[0] * k, a[1] * k, a[2] * k)
def cross(a, b): return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])
def dot(a, b): return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]
def norm(a):
    l = math.sqrt(dot(a, a))
    return (a[0] / l, a[1] / l, a[2] / l) if l > 1e-9 else (0.0, 1.0, 0.0)
def mean(ps): return tuple(sum(p[i] for p in ps) / len(ps) for i in range(3))


class Mesh:
    def __init__(self, name):
        self.name = name
        self.q = []       # (layer, [(x,y,z,u,v)*4], normal, rgb)

    def quad(self, layer, pts, uvs, rgb, ref=None):
        """pts: four 3D points; ref: a point inside the solid (the normal is turned away from it); uvs: four (u, v)."""
        p = [tuple(float(c) for c in q) for q in pts]
        n = cross(sub(p[2], p[0]), sub(p[3], p[1]))
        if dot(n, n) < 1e-10:
            n = cross(sub(p[1], p[0]), sub(p[3], p[0]))
        if dot(n, n) < 1e-10:
            n = cross(sub(p[2], p[1]), sub(p[3], p[1]))
        if dot(n, n) < 1e-10:
            return
        n = norm(n)
        if ref is not None and dot(n, sub(mean(p), ref)) < 0:
            p = [p[0], p[3], p[2], p[1]]
            uvs = [uvs[0], uvs[3], uvs[2], uvs[1]]
            n = mul(n, -1)
        self.q.append((layer, [(p[i][0], p[i][1], p[i][2], uvs[i][0], uvs[i][1]) for i in range(4)], n, rgb))

    # ---- primitives -----------------------------------------------------------------------
    def box(self, layer, c, h, rgb, region=WHITE, rot=None):
        cx, cy, cz = c
        hx, hy, hz = h
        pts = [(cx + sx * hx, cy + sy * hy, cz + sz * hz) for sx in (-1, 1) for sy in (-1, 1) for sz in (-1, 1)]
        idx = {(sx, sy, sz): pts[i] for i, (sx, sy, sz) in enumerate([(a, b, d) for a in (-1, 1) for b in (-1, 1) for d in (-1, 1)])}
        faces = [((1, -1, -1), (1, 1, -1), (1, 1, 1), (1, -1, 1)), ((-1, -1, 1), (-1, 1, 1), (-1, 1, -1), (-1, -1, -1)),
                 ((-1, 1, -1), (-1, 1, 1), (1, 1, 1), (1, 1, -1)), ((-1, -1, 1), (-1, -1, -1), (1, -1, -1), (1, -1, 1)),
                 ((-1, -1, -1), (-1, 1, -1), (1, 1, -1), (1, -1, -1)), ((1, -1, 1), (1, 1, 1), (-1, 1, 1), (-1, -1, 1))]
        u0, v0, u1, v1 = region
        for f in faces:
            self.quad(layer, [idx[k] for k in f], [(u0, v1), (u0, v0), (u1, v0), (u1, v1)], rgb, ref=c)

    def prism(self, layer, p0, p1, w0, t0, w1, t1, rgb, region=WHITE):
        """A tapered four-sided bar from p0 to p1 (straight along y), half widths (x, z) at both ends."""
        cx0, cy0, cz0 = p0
        cx1, cy1, cz1 = p1
        s0 = [(cx0 - w0, cy0, cz0 - t0), (cx0 + w0, cy0, cz0 - t0), (cx0 + w0, cy0, cz0 + t0), (cx0 - w0, cy0, cz0 + t0)]
        s1 = [(cx1 - w1, cy1, cz1 - t1), (cx1 + w1, cy1, cz1 - t1), (cx1 + w1, cy1, cz1 + t1), (cx1 - w1, cy1, cz1 + t1)]
        self.loft(layer, [s0, s1], rgb, region, caps=True)

    def loft(self, layer, sections, rgb, region=WHITE, caps=False, closed=True, vscale=True):
        """Quads between consecutive sections (equal point counts; a tip is the same point repeated). Outward = away from the section centres."""
        u0, v0, u1, v1 = region
        n = len(sections[0])
        cs = [mean(s) for s in sections]
        for i in range(len(sections) - 1):
            a, b = sections[i], sections[i + 1]
            va, vb = v1 - (v1 - v0) * i / (len(sections) - 1), v1 - (v1 - v0) * (i + 1) / (len(sections) - 1)
            for j in range(n if closed else n - 1):
                k = (j + 1) % n
                ref = mean([cs[i], cs[i + 1]])
                ua, ub = u0 + (u1 - u0) * j / n, u0 + (u1 - u0) * (j + 1) / n
                self.quad(layer, [a[j], a[k], b[k], b[j]], [(ua, va), (ub, va), (ub, vb), (ua, vb)], rgb, ref=ref)
        if caps:
            for s, c in ((sections[0], cs[0]), (sections[-1], cs[-1])):
                ref = mean([cs[0], cs[-1]])
                self.quad(layer, [s[0], s[1], s[2], s[3 % n]], [(u0, v0), (u1, v0), (u1, v1), (u0, v1)], rgb, ref=ref)

    def ring(self, layer, c, ex, ey, R, s, n, rgb, region=WHITE):
        """A torus-like strip: n boxes round a circle of radius R in the plane spanned by ex, ey, square section of half size s."""
        ex, ey = norm(ex), norm(ey)
        ez = norm(cross(ex, ey))
        secs = []
        for i in range(n + 1):
            a = 2 * math.pi * i / n
            rad = add(mul(ex, math.cos(a)), mul(ey, math.sin(a)))
            ctr = add(c, mul(rad, R))
            secs.append([add(ctr, add(mul(rad, s), mul(ez, s))), add(ctr, add(mul(rad, -s), mul(ez, s))), add(ctr, add(mul(rad, -s), mul(ez, -s))), add(ctr, add(mul(rad, s), mul(ez, -s)))])
        u0, v0, u1, v1 = region
        for i in range(n):
            ctr = mean([mean(secs[i]), mean(secs[i + 1])])
            ref = add(c, mul(norm(sub(ctr, c)), R))
            for j in range(4):
                k = (j + 1) % 4
                self.quad(layer, [secs[i][j], secs[i][k], secs[i + 1][k], secs[i + 1][j]], [(u0, v1), (u1, v1), (u1, v0), (u0, v0)], rgb, ref=ref)

    def text(self):
        lines = ["# generated by tools/gen_sdw3_models.py (%s): %d quads" % (self.name, len(self.q))]
        for layer, vs, n, rgb in self.q:
            body = " ".join("%.3f %.3f %.3f %.4f %.4f" % v for v in vs)
            lines.append("q %d %s %.3f %.3f %.3f %d %d %d" % (layer, body, n[0], n[1], n[2], rgb[0], rgb[1], rgb[2]))
        return "\n".join(lines) + "\n"

    def counts(self):
        c = [0, 0, 0]
        for q in self.q:
            c[q[0]] += 4
        return c


def hexsec(y, w, t, cx=0.0, cz=0.0):
    """The hexagonal section of a blade: a flat face on each side and a bevel to each edge."""
    return [(cx - w, y, cz), (cx - w * 0.45, y, cz + t), (cx + w * 0.45, y, cz + t), (cx + w, y, cz), (cx + w * 0.45, y, cz - t), (cx - w * 0.45, y, cz - t)]


def diasec(y, w, t, cx=0.0, cz=0.0):
    return [(cx - w, y, cz), (cx, y, cz + t), (cx + w, y, cz), (cx, y, cz - t)]


# ============================================================================================ the pieces
def sword():
    m = Mesh("sword")
    L = 46.0
    # blade: hexagonal sections, a long taper, a point (layer 0); the bevels are bright, the flats mint
    secs = [hexsec(1.0, 2.5, 1.0), hexsec(L * 0.45, 2.4, 0.95), hexsec(L * 0.82, 2.0, 0.8), hexsec(L - 1.6, 1.3, 0.5)]
    m.loft(0, secs, TEAL, BEVEL, caps=False)
    tip = (0.0, L + 1.4, 0.0)
    last = secs[-1]
    for j in range(6):
        k = (j + 1) % 6
        m.quad(0, [last[j], last[k], tip, tip], [(0.7, 0.2), (0.8, 0.2), (0.75, 0.05), (0.75, 0.05)], PALE, ref=(0, L * 0.8, 0))
    # fuller: a bright groove on both faces
    for sz in (-1, 1):
        m.box(0, (0.0, L * 0.45 + 1, sz * 0.97), (0.55, L * 0.42, 0.12), WHT, WHITE)
    # blade glow shell (layer 1)
    gs = [hexsec(0.5, 3.6, 2.0), hexsec(L * 0.5, 3.5, 1.9), hexsec(L * 0.85, 2.9, 1.5), hexsec(L + 0.5, 0.3, 0.2)]
    m.loft(1, gs, (150, 255, 210), WHITE)
    # cross-guard: a core block, two quillons curving up, a gem
    m.box(0, (0, -0.2, 0), (2.6, 1.4, 1.7), PALE, CUBE)
    for s in (-1, 1):
        m.prism(0, (s * 2.6, -0.2, 0), (s * 7.0, 0.1, 0), 1.35, 1.1, 0.9, 0.8, MINT, PANEL)
        m.prism(0, (s * 7.0, 0.1, 0), (s * 9.2, 2.6, 0), 0.9, 0.8, 0.35, 0.4, WHT, PANEL)
        m.box(0, (s * 4.5, -1.5, 0), (0.9, 0.7, 1.3), TEAL, CUBE)
    m.box(0, (0, 0.2, 1.75), (0.9, 0.9, 0.3), WHT, CUBE)
    m.box(0, (0, 0.2, -1.75), (0.9, 0.9, 0.3), WHT, CUBE)
    # grip with wrap bands, pommel
    for i, y in enumerate((-2.0, -5.2, -8.4)):
        m.loft(0, [hexsec(y, 1.15, 1.15), hexsec(y - 2.6, 1.15, 1.15)], DEEP if i % 2 == 0 else MINT, PANEL)
    m.box(0, (0, -11.5, 0), (2.0, 1.1, 1.5), PALE, CUBE)
    m.loft(0, [diasec(-12.4, 1.8, 1.4), diasec(-15.0, 0.1, 0.1)], WHT, BEVEL)
    m.loft(0, [diasec(-10.6, 1.8, 1.4), diasec(-12.4, 1.8, 1.4)], MINT, BEVEL)
    # the two crossed rings of the guard (layer 2): one round the hilt facing the viewer, one across it
    m.ring(2, (0, -0.5, 0), (1, 0, 0), (0, 1, 0), 7.6, 0.32, 12, WHT, WHITE)
    m.ring(2, (0, -0.5, 0), (0, 0, 1), (0, 1, 0), 6.2, 0.28, 12, PALE, WHITE)
    return m


def bow():
    m = Mesh("bow")
    H = 52.0                                     # total height

    def limb(sign):
        secs = []
        N = 6
        for i in range(N + 1):
            t = i / N
            y = sign * (3.0 + t * (H * 0.5 - 3.0))
            x = 7.5 * math.sin(math.pi * min(1.0, t * 1.02) * 0.92) - 1.0 * t      # the limb swings out and curls back toward the tip
            tx = 7.5 * math.pi * 0.92 * math.cos(math.pi * t * 0.92) / N - 1.0 / N
            ty = sign * (H * 0.5 - 3.0) / N
            tn = norm((tx, ty, 0.0))
            nx, ny = -tn[1], tn[0]
            w = 0.5 + 2.6 * math.sin(math.pi * min(1.0, t * 0.95 + 0.05)) ** 0.7 * (1.0 - 0.35 * t)
            th = 1.05 - 0.25 * t
            if i == N:
                w, th = 0.25, 0.25
            secs.append([(x + nx * w, y + ny * w, 0.0), (x, y, th), (x - nx * w, y - ny * w, 0.0), (x, y, -th)])
        return secs

    for s in (1, -1):
        sec = limb(s)
        m.loft(0, sec, TEAL, BEVEL)
        gl = [[(p[0] * 1.0, p[1], p[2] * 2.6) for p in sec[i]] for i in (0, 2, 4, 6)]
        m.loft(1, gl, (150, 255, 210), WHITE)
        # the cross ornament on the limb (layer 2): a plate cross standing in the bow's plane
        t = 0.52
        y = s * (3.0 + t * (H * 0.5 - 3.0))
        x = 7.5 * math.sin(math.pi * t * 1.02 * 0.92) - 0.5 + 2.6
        m.box(2, (x, y, 0), (0.45, 3.6, 0.45), WHT, WHITE)
        m.box(2, (x, y + s * 0.8, 0), (2.4, 0.45, 0.45), WHT, WHITE)
        # the nock at the tip
        tipx = 7.5 * math.sin(math.pi * 0.92) - 1.0
        m.box(0, (tipx, s * (H * 0.5), 0), (0.7, 0.9, 0.7), WHT, CUBE)
    # the riser and grip
    m.loft(0, [diasec(-3.4, 1.9, 1.7, 0.0), diasec(0.0, 2.3, 2.0, 0.0), diasec(3.4, 1.9, 1.7, 0.0)], PALE, PANEL)
    m.box(0, (0.2, 0.0, 0.0), (2.4, 1.6, 2.2), MINT, CUBE)
    # the string: from nock to nock, a hair thin bar
    tipx = 7.5 * math.sin(math.pi * 0.92) - 1.0
    m.box(0, (tipx - 0.1, 0, 0), (0.18, H * 0.5, 0.18), PALE, WHITE)
    return m


def arrow():
    m = Mesh("arrow")
    L = 40.0
    m.loft(0, [diasec(0.0, 0.7, 0.7), diasec(L * 0.82, 0.7, 0.7)], PALE, WHITE)
    m.loft(1, [diasec(0.0, 1.8, 1.8), diasec(L * 0.9, 1.8, 1.8), diasec(L + 6, 0.2, 0.2)], (150, 255, 210), WHITE)
    head = [diasec(L * 0.82, 0.7, 0.7), diasec(L * 0.88, 2.5, 1.4), diasec(L + 6.0, 0.05, 0.05)]
    m.loft(0, head, WHT, BEVEL)
    # barbs and fletching (flat leaves in two planes)
    for s in (-1, 1):
        m.loft(2, [diasec(L * 0.82, 0.3, 0.3), diasec(L * 0.80 - 1.0, 3.3, 0.35), diasec(L * 0.74, 0.3, 0.3)], MINT, BEVEL)
        m.loft(2, [diasec(1.0, 0.3, 0.2, 0, 0), diasec(5.5, 0.3, 2.0, 0, s * 1.0), diasec(8.5, 0.3, 0.2, 0, 0)], PALE, BEVEL)
    m.box(2, (0, 3.5, 0), (2.0, 2.6, 0.12), PALE, PANEL)
    m.box(2, (0, 3.5, 0), (0.12, 2.6, 2.0), PALE, PANEL)
    return m


def spear():
    m = Mesh("spear")
    L = 44.0
    m.loft(0, [diasec(0.0, 0.6, 0.6), diasec(L * 0.72, 0.6, 0.6)], TEAL, WHITE)
    m.loft(0, [diasec(L * 0.7, 0.9, 0.9), diasec(L * 0.78, 2.6, 1.1), diasec(L * 0.9, 2.2, 0.9), diasec(L + 2, 0.05, 0.05)], TEAL, BEVEL)
    m.loft(1, [diasec(L * 0.68, 2.2, 1.9), diasec(L * 0.8, 4.0, 2.2), diasec(L + 4, 0.2, 0.2)], (150, 255, 210), WHITE)
    m.box(2, (0, L * 0.55, 0), (0.45, 3.6, 0.45), MINT, WHITE)
    m.box(2, (0, L * 0.6, 0), (2.6, 0.45, 0.45), MINT, WHITE)
    m.box(2, (0, L * 0.6, 0), (0.45, 0.45, 2.6), MINT, WHITE)
    m.ring(2, (0, L * 0.58, 0), (1, 0, 0), (0, 1, 0), 5.0, 0.25, 10, PALE, WHITE)
    m.loft(0, [diasec(0.0, 0.9, 0.9), diasec(-3.0, 0.1, 0.1)], WHT, BEVEL)
    return m


def blade():
    m = Mesh("blade")
    L = 14.0
    m.loft(0, [diasec(0.0, 0.5, 0.45), diasec(L * 0.30, 1.35, 0.7), diasec(L * 0.86, 0.9, 0.45), diasec(L, 0.0, 0.0)], TEAL, BEVEL, caps=False)
    m.loft(0, [diasec(0.0, 0.5, 0.45), diasec(-3.2, 0.2, 0.2)], WHT, BEVEL)
    m.loft(1, [diasec(0.0, 1.7, 1.4), diasec(L * 0.4, 2.7, 1.8), diasec(L + 1.0, 0.0, 0.0)], (150, 255, 210), WHITE)
    return m


def feather():
    m = Mesh("feather")
    L = 30.0
    secs = [diasec(0.0, 0.5, 0.4), diasec(L * 0.22, 2.2, 0.7), diasec(L * 0.55, 3.1, 0.7), diasec(L * 0.85, 1.8, 0.45), diasec(L, 0.0, 0.0)]
    m.loft(0, secs, MINT, BEVEL)
    m.loft(1, [diasec(0.0, 0.8, 0.8), diasec(L * 0.4, 4.6, 1.6), diasec(L * 0.8, 3.0, 1.1), diasec(L + 1, 0.0, 0.0)], (150, 255, 210), WHITE)
    m.box(2, (0, L * 0.42, 0.75), (0.2, L * 0.4, 0.12), WHT, WHITE)         # the quill
    return m


def cube():
    m = Mesh("cube")
    m.box(0, (0, 0, 0), (1.0, 1.0, 1.0), MINT, CUBE)
    m.box(1, (0, 0, 0), (1.35, 1.35, 1.35), (150, 255, 210), WHITE)
    return m


def panel():
    m = Mesh("panel")
    m.box(0, (0, 0, 0), (1.0, 1.0, 0.18), PALE, PANEL)
    m.box(1, (0, 0, 0), (1.25, 1.25, 0.6), (150, 255, 210), WHITE)
    return m


def plate():
    """An armoured plate of light: a shoulder pauldron, three stacked leaf lames."""
    m = Mesh("plate")
    for i in range(3):
        y = -i * 2.2
        s = 5.0 - i * 0.9
        m.loft(0, [[(-s, y, -3.0), (s, y, -3.0), (s * 0.8, y, 3.2), (-s * 0.8, y, 3.2)], [(-s * 1.04, y - 1.8, -3.3), (s * 1.04, y - 1.8, -3.3), (s * 0.82, y - 1.8, 3.5), (-s * 0.82, y - 1.8, 3.5)]], MINT if i % 2 == 0 else PALE, PANEL, caps=True)
    m.box(2, (0, 2.2, 0), (4.4, 0.4, 3.4), WHT, WHITE)
    m.loft(1, [[(-6.5, 1.0, -4.3), (6.5, 1.0, -4.3), (5.4, 1.0, 4.4), (-5.4, 1.0, 4.4)], [(-6.8, -6.0, -4.6), (6.8, -6.0, -4.6), (5.6, -6.0, 4.7), (-5.6, -6.0, 4.7)]], (150, 255, 210), WHITE)
    return m


def crown():
    """A ring of 3D arrow bars: twelve bars on a square ring (each a long diamond bar with an arrow head), a thin ring band under them."""
    m = Mesh("crown")
    a = 5.4
    pts = []
    for i in range(12):
        s = i / 12.0 * 4
        side = int(s)
        f = s - side
        p = -a + 2 * a * f
        pts.append([(p, -a), (a, p), (-p, a), (-a, -p)][side])
    hs = [5.0, 6.4, 4.6, 6.0, 5.2, 6.8, 4.8, 5.8, 6.2, 7.4, 5.4, 6.9]
    for (x, z), h in zip(pts, hs):
        # bars hang UP in mesh +y (the painter turns the mesh)
        m.loft(0, [diasec(0.0, 0.32, 0.32, x, z), diasec(h * 0.72, 0.32, 0.32, x, z)], WHT, WHITE)
        m.loft(0, [diasec(h * 0.7, 0.32, 0.32, x, z), diasec(h * 0.76, 0.95, 0.62, x, z), diasec(h + 0.8, 0.0, 0.0, x, z)], PALE, BEVEL)
    m.ring(2, (0, 0.2, 0), (1, 0, 0), (0, 0, 1), 5.4, 0.3, 8, WHT, WHITE)
    return m


MESHES = [sword, bow, arrow, spear, blade, feather, cube, panel, plate, crown]


# ============================================================================================ the atlas
def atlas():
    img = np.zeros((64, 64, 4), np.float32)

    def fill(u0, v0, u1, v1, fn):
        for y in range(v0, v1):
            for x in range(u0, u1):
                img[y, x] = fn((x - u0 + 0.5) / (u1 - u0), (y - v0 + 0.5) / (v1 - v0))

    fill(0, 0, 8, 8, lambda u, v: (1, 1, 1, 1))                                                            # solid white
    def cube_face(u, v):
        e = min(u, 1 - u, v, 1 - v)
        if e < 0.12:
            return (1.0, 1.0, 1.0, 1.0)
        g = 0.82 + 0.18 * (1.0 - 2.0 * max(abs(u - 0.5), abs(v - 0.5)))
        return (0.60 * g + 0.2, 0.96 * g, 0.80 * g, 1.0)
    fill(8, 0, 24, 16, cube_face)

    def panel_px(u, v):
        e = min(u, 1 - u, v, 1 - v)
        if e < 0.09:
            return (1.0, 1.0, 1.0, 1.0)
        vein = abs(u - 0.5) < 0.04 or abs((v - 0.5) - (u - 0.5) * 0.9) < 0.035
        return (0.80, 1.0, 0.90, 1.0) if not vein else (0.95, 1.0, 0.98, 1.0)
    fill(24, 0, 40, 16, panel_px)

    def bevel_px(u, v):
        e = min(u, 1 - u)
        k = max(0.0, 1.0 - e / 0.35)
        return (0.62 + 0.38 * k, 0.97 + 0.03 * k, 0.80 + 0.2 * k, 1.0)
    fill(40, 0, 56, 16, bevel_px)
    fill(0, 16, 16, 32, lambda u, v: (0.8 + 0.2 * v, 1.0, 0.9 + 0.1 * v, 1.0))
    Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGBA").save(TEX)
    # the soft glow sprite (a round falloff; black under alpha 0, so every blend mode shows it round)
    yy, xx = np.mgrid[0:64, 0:64]
    r = np.sqrt(((xx + 0.5) / 32.0 - 1.0) ** 2 + ((yy + 0.5) / 32.0 - 1.0) ** 2)
    f = np.clip(1.0 - r, 0, 1) ** 1.8
    soft = np.zeros((64, 64, 4), np.float32)
    soft[..., 0] = f * 0.85
    soft[..., 1] = f
    soft[..., 2] = f * 0.9
    soft[..., 3] = f
    Image.fromarray((soft * 255).astype(np.uint8), "RGBA").save(SOFT)


def main():
    os.makedirs(OUT, exist_ok=True)
    os.makedirs(os.path.dirname(DOC), exist_ok=True)
    lines = []
    for f in MESHES:
        m = f()
        with open(os.path.join(OUT, m.name + ".txt"), "w", encoding="utf-8", newline="\n") as fh:
            fh.write(m.text())
        c = m.counts()
        lines.append("%-8s layer0 %4d  layer1 %4d  layer2 %4d  total %4d vertices" % (m.name, c[0], c[1], c[2], sum(c)))
    atlas()
    with open(DOC, "w", encoding="utf-8", newline="\n") as fh:
        fh.write("\n".join(lines) + "\n")
    print("\n".join(lines))


if __name__ == "__main__":
    main()

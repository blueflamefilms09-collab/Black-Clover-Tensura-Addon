"""0.50: 3D in-hand models for every Black Clover weapon, built to the Genesis Demon-Slayer's standard. Deterministic.

    python tools/gen_weapon_models.py            (then: python tools/item_preview/preview_weapons.py)

For each weapon in SPECS this writes
  assets/nusmp/weapon_meshes/<id>.txt        the mesh WeaponRenderer draws (header lines + one line per quad)
  assets/nusmp/textures/entity/weapon/<id>.png        its 128x128 skin
  assets/nusmp/textures/entity/weapon/<id>_glow.png   its emissive layer (drawn additive, full bright), if it has one
The inventory icons stay the old sprites (textures/item/<id>.png, untouched); each mesh is placed on its sprite's diagonal
(measured here from the sprite), so the old display transforms still fit.

Model units are pixels: y runs along the weapon (0 = where the blade meets the guard, the grip below 0), x across it, z through it.
Parts:
  blade   stations (y, left x, ridge x, right x); a diamond (or flat, ridge == edge) cross-section; edge walls; a pointed tip
  box     an axis-aligned block (grips, pommels, guard bars, shafts)
  plate   an extruded pixel sprite (guards, clover guards, tsuba, prongs): cut-out faces plus walls round every opaque pixel
  void    rectangles on a blade face drawn with the crimson dimensional void (Asta's and the demon swords)
Mesh file format:
  place <ox> <oy> <angle> <scale> <ymin>      item-space origin of y=ymin, the axis angle (deg), the pixel size in item units
  glow <rgb hex> <speed> <min> <max>          the emissive layer's tint and pulse (absent = no glow)
  q <layer> 4 x (x y z u v) nx ny nz          a quad; layer 0 body (uv in texture pixels), 1 void (position only), 2 glow
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm  # noqa: E402

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
TS = 128


# ================================================================ atlas
class Atlas:
    """Shelf packer over a TS x TS skin; every region keeps a painter f(lx, ly) -> (rgba, glow_rgba) in its own pixels."""

    def __init__(self):
        self.x = self.y = self.row = 0
        self.img = np.zeros((TS, TS, 4), np.float32)
        self.glow = np.zeros((TS, TS, 4), np.float32)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > TS:
            self.x, self.y, self.row = 0, self.y + self.row + 1, 0
        if self.y + h > TS:
            raise RuntimeError("atlas full")
        u, v = self.x, self.y
        self.x += w + 1
        self.row = max(self.row, h)
        return u, v

    def paint(self, u, v, w, h, fn):
        for j in range(int(math.ceil(h))):
            for i in range(int(math.ceil(w))):
                c, g = fn(i + 0.5, j + 0.5)
                if c is not None:
                    self.img[v + j, u + i] = c
                if g is not None:
                    self.glow[v + j, u + i] = g


# ================================================================ mesh
class Mesh:
    def __init__(self):
        self.quads = []          # (layer, [(x,y,z,u,v)*4], normal)

    def quad(self, layer, pts, outward):
        p = [np.array(q[:3], np.float64) for q in pts]
        n = np.cross(p[2] - p[0], p[3] - p[1])
        if np.linalg.norm(n) < 1e-9:
            n = np.cross(p[1] - p[0], p[2] - p[0])
        if np.linalg.norm(n) < 1e-9:
            return
        if np.dot(n, outward) < 0:
            pts = [pts[0], pts[3], pts[2], pts[1]]
            n = -n
        n = n / np.linalg.norm(n)
        self.quads.append((layer, pts, n))


def lerp(a, b, t):
    return a + (b - a) * t


# ---------------------------------------------------------------- blade
class Blade:
    """stations: [(y, xl, xc, xr)] bottom to top (the last one may close to a point). te / tr: half thickness at the edges /
    on the ridge. paint(x, y, face, edge_dist, ridge_dist) -> (rgba, glow)."""

    def __init__(self, stations, te, tr, paint, dx=0.0, z=0.0):
        self.st = [(y, xl + dx, xc + dx, xr + dx) for (y, xl, xc, xr) in stations]
        self.te, self.tr, self.paint, self.z = te, tr, paint, z

    def width_at(self, y):
        st = self.st
        for a, b in zip(st, st[1:]):
            if a[0] <= y <= b[0]:
                t = (y - a[0]) / max(1e-6, b[0] - a[0])
                return lerp(a[1], b[1], t), lerp(a[2], b[2], t), lerp(a[3], b[3], t)
        return None

    def build(self, mesh, atlas, voids=()):
        xs = [s[1] for s in self.st] + [s[3] for s in self.st]
        x0, x1 = math.floor(min(xs)) - 1, math.ceil(max(xs)) + 1
        y0, y1 = math.floor(self.st[0][0]), math.ceil(self.st[-1][0])
        w, h = x1 - x0, y1 - y0
        fu, fv = atlas.alloc(w, h)
        bu, bv = atlas.alloc(w, h)
        su, sv = atlas.alloc(2, h)

        def face_paint(front):
            def f(lx, ly):
                x = x0 + lx if front else x1 - lx
                y = y1 - ly
                wd = self.width_at(y)
                if wd is None or not (wd[0] - 0.3 <= x <= wd[2] + 0.3):
                    return None, None
                e = min(x - wd[0], wd[2] - x, (self.st[-1][0] - y) * 0.6 + 0.5 if self.st[-1][1] == self.st[-1][3] else 99)
                return self.paint(x, y, front, e, abs(x - wd[1]))
            return f

        atlas.paint(fu, fv, w, h, face_paint(True))
        atlas.paint(bu, bv, w, h, face_paint(False))
        atlas.paint(su, sv, 2, h, lambda lx, ly: self.paint(None, y1 - ly, None, 0, 0))

        z = self.z

        def uvf(x, y, front):
            return (fu + (x - x0), fv + (y1 - y)) if front else (bu + (x1 - x), bv + (y1 - y))

        for a, b in zip(self.st, self.st[1:]):
            for front in (True, False):
                s = 1 if front else -1
                for (ka, kb, za, zb) in ((1, 2, self.te, self.tr), (2, 3, self.tr, self.te)):
                    P = [(a[ka], a[0], z + s * za), (a[kb], a[0], z + s * zb), (b[kb], b[0], z + s * zb), (b[ka], b[0], z + s * za)]
                    mesh.quad(0, [p + uvf(p[0], p[1], front) for p in P], (0, 0, s))
            if self.te > 0.01:
                for k, side in ((1, -1), (3, 1)):
                    P = [(a[k], a[0], z + self.te, su + 2, sv + y1 - a[0]), (a[k], a[0], z - self.te, su, sv + y1 - a[0]),
                         (b[k], b[0], z - self.te, su, sv + y1 - b[0]), (b[k], b[0], z + self.te, su + 2, sv + y1 - b[0])]
                    dx, dy = b[k] - a[k], b[0] - a[0]
                    mesh.quad(0, P, (side * dy, -side * dx, 0))
        for cap, sgn in ((self.st[0], -1), (self.st[-1], 1)):              # the base (and a blunt top, if any)
            if cap[3] - cap[1] < 0.05:
                continue
            y = cap[0]
            P = [(cap[1], y, z + self.te, su, sv + y1 - y), (cap[2], y, z + self.tr, su + 1, sv + y1 - y),
                 (cap[2], y, z - self.tr, su + 1, sv + y1 - y + 0.5), (cap[1], y, z - self.te, su, sv + y1 - y + 0.5)]
            mesh.quad(0, P, (0, sgn, 0))
            P = [(cap[2], y, z + self.tr, su + 1, sv + y1 - y), (cap[3], y, z + self.te, su + 2, sv + y1 - y),
                 (cap[3], y, z - self.te, su + 2, sv + y1 - y + 0.5), (cap[2], y, z - self.tr, su + 1, sv + y1 - y + 0.5)]
            mesh.quad(0, P, (0, sgn, 0))
        for (vx0, vy0, vx1, vy1, front) in voids:
            s = 1 if front else -1
            pts = []
            for (x, y) in ((vx0, vy0), (vx1, vy0), (vx1, vy1), (vx0, vy1)):
                wd = self.width_at(y)
                zz = self.te + (self.tr - self.te) * (1 - min(1, abs(x - wd[1]) / max(0.5, (wd[2] - wd[0]) / 2))) if wd else self.tr
                pts.append((x, y, z + s * (zz + 0.03), 0, 0))
            mesh.quad(1, pts, (0, 0, s))
        return (fu, fv, bu, bv, su, sv, w, h)


# ---------------------------------------------------------------- boxes
def box(mesh, atlas, x0, y0, z0, x1, y1, z1, paint):
    """paint(face, i, j, fw, fh) -> (rgba, glow); faces: 0 top, 1 bottom, 2..5 the sides (-x, -z, +x, +z)."""
    w, h, d = x1 - x0, y1 - y0, z1 - z0
    W, H, D = [max(1, int(math.ceil(k))) for k in (w, h, d)]
    u, v = atlas.alloc(2 * D + 2 * W, D + H)
    faces = [(u + D, v, W, D), (u + D + W, v, W, D), (u, v + D, D, H), (u + D, v + D, W, H), (u + D + W, v + D, D, H), (u + 2 * D + W, v + D, W, H)]
    for k, (fu, fv, fw, fh) in enumerate(faces):
        atlas.paint(fu, fv, fw, fh, lambda lx, ly, k=k, fw=fw, fh=fh: paint(k, int(lx), int(ly), fw, fh))

    def F(k):
        fu, fv, fw, fh = faces[k]
        return fu, fv, fu + fw, fv + fh

    a, b, c, dd = F(0)
    mesh.quad(0, [(x0, y1, z1, a, dd), (x1, y1, z1, c, dd), (x1, y1, z0, c, b), (x0, y1, z0, a, b)], (0, 1, 0))
    a, b, c, dd = F(1)
    mesh.quad(0, [(x0, y0, z0, a, b), (x1, y0, z0, c, b), (x1, y0, z1, c, dd), (x0, y0, z1, a, dd)], (0, -1, 0))
    a, b, c, dd = F(2)
    mesh.quad(0, [(x0, y0, z0, a, dd), (x0, y0, z1, c, dd), (x0, y1, z1, c, b), (x0, y1, z0, a, b)], (-1, 0, 0))
    a, b, c, dd = F(3)
    mesh.quad(0, [(x1, y0, z0, a, dd), (x0, y0, z0, c, dd), (x0, y1, z0, c, b), (x1, y1, z0, a, b)], (0, 0, -1))
    a, b, c, dd = F(4)
    mesh.quad(0, [(x1, y0, z1, a, dd), (x1, y0, z0, c, dd), (x1, y1, z0, c, b), (x1, y1, z1, a, b)], (1, 0, 0))
    a, b, c, dd = F(5)
    mesh.quad(0, [(x0, y0, z1, a, dd), (x1, y0, z1, c, dd), (x1, y1, z1, c, b), (x0, y1, z1, a, b)], (0, 0, 1))


# ---------------------------------------------------------------- extruded pixel plates
def plate(mesh, atlas, mask, x0, y0, t, paint, plane="xy", c=0.0):
    """mask[j][i] (row 0 = top). plane 'xy': faces toward +-z, the sprite spans x0.. / y0.. (up), thickness z in [c-t, c+t].
    plane 'xz': faces toward +-y (a disc across the blade, e.g. a tsuba), sprite rows run along -z, thickness y in [c-t, c+t].
    paint(i, j, front) -> (rgba, glow) for opaque pixels."""
    m = np.asarray(mask, bool)
    h, w = m.shape
    fu, fv = atlas.alloc(w, h)
    bu, bv = atlas.alloc(w, h)
    atlas.paint(fu, fv, w, h, lambda lx, ly: paint(int(lx), int(ly), True) if m[int(ly), int(lx)] else (None, None))
    atlas.paint(bu, bv, w, h, lambda lx, ly: paint(w - 1 - int(lx), int(ly), False) if m[int(ly), w - 1 - int(lx)] else (None, None))

    def P(i, j, s):                                       # sprite coords (i right, j down) + side s (+1 front) -> xyz
        if plane == "xy":
            return (x0 + i, y0 + h - j, c + s * t)
        return (x0 + i, c + s * t, y0 + j)                    # 'xz': j runs along +z (towards the viewer of the front)

    def out(s):
        return (0, 0, s) if plane == "xy" else (0, s, 0)

    def emit(pts, uv, o):
        mesh.quad(0, [pts[k] + uv[k] for k in range(4)], o)

    emit([P(0, h, 1), P(w, h, 1), P(w, 0, 1), P(0, 0, 1)], [(fu, fv + h), (fu + w, fv + h), (fu + w, fv), (fu, fv)], out(1))
    emit([P(0, h, -1), P(w, h, -1), P(w, 0, -1), P(0, 0, -1)], [(bu + w, bv + h), (bu, bv + h), (bu, bv), (bu + w, bv)], out(-1))

    def wall_dir(di, dj):
        if plane == "xy":
            return (di, -dj, 0)
        return (di, 0, dj)

    # walls: runs of pixels whose neighbour across an edge is empty
    for j in range(h):                                       # horizontal edges (top: dj=-1, bottom: dj=+1)
        for dj in (-1, 1):
            i = 0
            while i < w:
                if m[j, i] and not (0 <= j + dj < h and m[j + dj, i]):
                    k = i
                    while k + 1 < w and m[j, k + 1] and not (0 <= j + dj < h and m[j + dj, k + 1]):
                        k += 1
                    ey = j if dj < 0 else j + 1
                    vv = fv + j + 0.5
                    emit([P(i, ey, 1), P(k + 1, ey, 1), P(k + 1, ey, -1), P(i, ey, -1)],
                         [(fu + i, vv), (fu + k + 1, vv), (fu + k + 1, vv), (fu + i, vv)], wall_dir(0, dj))
                    i = k + 1
                else:
                    i += 1
    for i in range(w):                                       # vertical edges (left: di=-1, right: di=+1)
        for di in (-1, 1):
            j = 0
            while j < h:
                if m[j, i] and not (0 <= i + di < w and m[j, i + di]):
                    k = j
                    while k + 1 < h and m[k + 1, i] and not (0 <= i + di < w and m[k + 1, i + di]):
                        k += 1
                    ex = i if di < 0 else i + 1
                    uu = fu + i + 0.5
                    emit([P(ex, j, 1), P(ex, k + 1, 1), P(ex, k + 1, -1), P(ex, j, -1)],
                         [(uu, fv + j), (uu, fv + k + 1), (uu, fv + k + 1), (uu, fv + j)], wall_dir(di, 0))
                    j = k + 1
                else:
                    j += 1


def disc_mask(r, inner=0.0, lobes=0, lobe_r=0.0):
    n = int(math.ceil(2 * (r + lobe_r)))
    c = n / 2
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    d = np.hypot(xx - c, yy - c)
    m = d <= r
    if lobes:
        for k in range(lobes):
            a = k * 2 * math.pi / lobes + math.pi / 4
            m |= np.hypot(xx - c - math.cos(a) * r, yy - c - math.sin(a) * r) <= lobe_r
    if inner:
        m &= d > inner
    return m


def clover_guard_mask(r, ss=4):
    """Licht's / Asta's quatrefoil (four-leaf clover) cross guard, seen flat: four heart-shaped leaves (up, down, left, right)
    round a small hub, with clear gaps between them. Supersampled, then thresholded to whole pixels."""
    n = int(math.ceil(r * 2.2))
    N = n * ss
    c = N / 2
    yy, xx = np.mgrid[0:N, 0:N] + 0.5
    m = np.hypot(xx - c, yy - c) <= r * 0.22 * ss
    dist, lobe, off = r * 0.66 * ss, r * 0.25 * ss, r * 0.25 * ss
    for k in range(4):
        a = k * math.pi / 2
        ax, ay = math.cos(a), math.sin(a)
        qx, qy = -ay, ax
        for s in (-1, 1):
            m |= np.hypot(xx - c - ax * dist - qx * s * off, yy - c - ay * dist - qy * s * off) <= lobe
        t = (xx - c) * ax + (yy - c) * ay                      # a wedge from the hub to the two lobes (the heart's point)
        q = np.abs((xx - c) * qx + (yy - c) * qy)
        m |= (t > 0) & (t < dist) & (q < (off + lobe * 0.6) * t / dist)
    m = m.reshape(n, ss, n, ss).mean(axis=(1, 3)) > 0.5
    return m


# ================================================================ colour helpers
def col(rgb, a=1.0):
    return np.array([rgb[0], rgb[1], rgb[2], 255 * a], np.float32)


def shade(rgb, k):
    return tuple(np.clip(np.array(rgb, np.float32) * k, 0, 255))


def mix(a, b, t):
    return tuple(np.array(a, np.float32) * (1 - t) + np.array(b, np.float32) * t)


NOISE = fbm(256, 256, 8, 5000)
FINE = fbm(256, 256, 3, 5001)


def nz(x, y, salt=0):
    return NOISE[int(y * 3 + salt * 37) % 256, int(x * 3 + 128 + salt * 11) % 256] - 0.5


def fz(x, y, salt=0):
    return FINE[int(y * 2 + salt * 13) % 256, int(x * 2 + 64 + salt * 7) % 256] - 0.5


def hsh(*k):
    h = 0
    for v in k:
        h = (h * 1000003 + int(v * 97) + 12345) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return (h & 0xFFFF) / 65535


def steel(x, y, base, e, edge_rgb, ridge_d=None, ridge_rgb=None, grime=30, salt=0):
    g = nz(x, y, salt) * grime + fz(x, y, salt) * grime * 0.5
    c = np.array(base, np.float32) + g
    if ridge_d is not None and ridge_d < 0.6 and ridge_rgb is not None:
        c = np.array(ridge_rgb, np.float32) + g * 0.5
    if e < 0.9:
        c = np.array(edge_rgb, np.float32) + fz(x, y, salt + 3) * 30
    elif e < 1.8:
        c = c * 1.14
    return c


def wrap_paint(cloth, under=None, diamonds=False):
    """A wrapped grip: diagonal bands of cloth, the shadowed overlaps, optional diamond windows of the under-layer (tsuka-ito)."""
    def p(face, i, j, fw, fh):
        n = fz(i, j, face) * 18
        if face in (0, 1):
            return col(shade(cloth, 0.8)), None
        band = (i + j) % 4
        c = np.array(cloth, np.float32) + n
        if diamonds and under is not None and (j % 4 in (1, 2)) and (i % 4 in (1, 2)) and fw >= 3:
            c = np.array(under, np.float32) + n
        elif band == 0:
            c = c * 0.6
        elif band == 1:
            c = c * 1.2
        return col(c), None
    return p


def metal_paint(rgb, glow=None, gem=None, rivets=False, glow_rgb=None):
    def p(face, i, j, fw, fh):
        n = fz(i * 1.7, j * 1.3, face) * 22
        c = np.array(rgb, np.float32) * (1.15 if face == 0 else 0.82 if face == 1 else 1.0) + n
        if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
            c = c * 1.2
        if rivets and fh >= 3 and j == fh // 2 and i % 4 == 1 and face >= 2:
            c = c * 1.5
        g = None
        if gem is not None and face in (3, 5) and fw >= 3 and fh >= 3 and 1 <= i <= fw - 2 and 1 <= j <= fh - 2:
            c = np.array(gem, np.float32) * (1.3 if (i, j) == (1, 1) else 1.0)
            if glow_rgb is not None:
                g = col(glow_rgb, 0.9)
        return col(c), g
    return p


def katana_stations(length, base_w, tip_w, sori, kissaki=6.0, steps=10, spine_frac=0.82):
    """A curved, single-edged blade: the spine on the left (x < 0), the edge on the right; 'sori' bends it towards the spine."""
    st = []
    for k in range(steps + 1):
        y = length * k / steps
        if y > length - kissaki:
            break
        t = y / length
        off = -sori * t * t
        hw = lerp(base_w, tip_w, t) / 2
        st.append((y, off - hw, off - hw + 2 * hw * (1 - spine_frac), off + hw))
    y = length - kissaki
    off = -sori * (y / length) ** 2
    hw = tip_w / 2
    st.append((y, off - hw, off - hw + 2 * hw * (1 - spine_frac), off + hw))
    yk = length - kissaki * 0.45
    offk = -sori * (yk / length) ** 2
    st.append((yk, offk - hw, offk - hw * 0.6, offk + hw * 0.55))
    offt = -sori - hw
    st.append((length, offt, offt, offt))
    return st


def straight_stations(length, base_hw, top_hw, tip_len, steps=4, ridge=0.0):
    st = []
    yt = length - tip_len
    for k in range(steps + 1):
        y = yt * k / steps
        hw = lerp(base_hw, top_hw, y / max(1, yt))
        st.append((y, -hw, ridge, hw))
    st.append((length, 0, 0, 0))
    return st


# ================================================================ the weapons
def demon_slasher(m, a):
    """Asta's Demon-Slasher Katana: a jet-black curved blade with a jagged crimson edge, a dark tsuba, black and red wrap."""
    L = 46
    st = katana_stations(L, 5.6, 4.4, 3.2, kissaki=6)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((150, 18, 32)), col((255, 70, 90), 0.8)
        w = Blade_w(st, y)
        jag = 1.1 + 0.6 * (math.sin(y * 1.9) > 0)                 # the serrated red edge (saw teeth)
        de = w[2] - x                                              # distance to the cutting edge
        if de < jag:
            return col((170 + nz(x, y) * 40, 16, 30)), col((255, 60, 80), 0.75)
        if de < jag + 0.8:
            return col((60, 8, 14)), col((255, 60, 80), 0.18)
        c = np.array((24, 22, 26), np.float32) + nz(x, y) * 18 + fz(x, y) * 10
        if x - w[0] < 0.8:
            c = c * 1.6                                             # the spine's highlight
        return col(c), None
    Blade(st, 0.2, 0.8, paint).build(m, a)
    plate(m, a, disc_mask(4.2), -4.2, -4.2, 0.6, lambda i, j, f: (col(shade((34, 28, 32), 1.25 if (i + j) % 3 == 0 else 1.0)), None), "xz", 0)
    box(m, a, -1.6, -1.8, -1.6, 1.6, -0.6, 1.6, metal_paint((120, 20, 32)))                # habaki collar
    box(m, a, -1.4, -15, -1.2, 1.4, -1.8, 1.2, wrap_paint((26, 22, 26), (150, 24, 38), diamonds=True))
    box(m, a, -1.7, -16.6, -1.5, 1.7, -15, 1.5, metal_paint((50, 44, 50)))
    return dict(glow=("FF3C50", 0.12, 0.55, 1.0))


def Blade_w(st, y):
    for p, q in zip(st, st[1:]):
        if p[0] <= y <= q[0]:
            t = (y - p[0]) / max(1e-6, q[0] - p[0])
            return lerp(p[1], q[1], t), lerp(p[2], q[2], t), lerp(p[3], q[3], t)
    return st[-1][1:]


def miasma(m, a):
    """Yami's katana: a bright curved blade with a wavy hamon, a round black tsuba, a black wrap over pale samegawa, a dark
    violet haze breathing along the spine (his Dark Magic)."""
    L = 48
    st = katana_stations(L, 5.4, 4.2, 3.6, kissaki=6.5)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((200, 204, 212)), None
        w = Blade_w(st, y)
        de = w[2] - x
        hamon = 1.6 + 0.7 * math.sin(y * 0.9) + 0.3 * math.sin(y * 2.3 + 1)
        if de < 0.7:
            return col((236, 240, 246)), None
        if de < hamon:
            c = np.array((226, 230, 238), np.float32) + fz(x, y) * 16
            return col(c), None
        if de < hamon + 0.6:
            return col((240, 244, 250)), None                       # the bright hamon line
        c = np.array((128, 134, 146), np.float32) + nz(x, y) * 24 + fz(x, y) * 10
        g = None
        if x - w[0] < 1.2:
            c = np.array((88, 90, 104), np.float32) + nz(x, y) * 10
            g = col((120, 50, 190), 0.55 * (0.5 + 0.5 * math.sin(y * 0.6)))
        return col(c), g
    Blade(st, 0.18, 0.75, paint).build(m, a)
    plate(m, a, disc_mask(4.5), -4.5, -4.5, 0.65, lambda i, j, f: (col(shade((22, 20, 24), 1.4 if (i * 3 + j) % 7 == 0 else 1.0)), None), "xz", 0)
    box(m, a, -1.6, -1.9, -1.6, 1.6, -0.65, 1.6, metal_paint((170, 140, 70)))
    box(m, a, -1.4, -16, -1.2, 1.4, -1.9, 1.2, wrap_paint((22, 20, 26), (218, 214, 200), diamonds=True))
    box(m, a, -1.7, -17.6, -1.5, 1.7, -16, 1.5, metal_paint((36, 32, 36)))
    return dict(glow=("7A3CC8", 0.05, 0.35, 0.9))


def rapier(m, a):
    """Spell-forged rapier: a long needle blade with blue spell runes, a gold swept hilt (knuckle ring + cup)."""
    L = 54
    st = straight_stations(L, 1.7, 1.1, 5, steps=3, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((200, 208, 220)), None
        c = steel(x, y, (180, 188, 202), e, (232, 238, 246), rd, (214, 222, 236), 18)
        g = None
        if rd < 0.6 and 6 < y < L - 8 and int(y) % 5 in (0, 1):
            c = np.array((90, 170, 240), np.float32)
            g = col((130, 210, 255), 0.95)
        return col(c), g
    Blade(st, 0.25, 0.7, paint).build(m, a)
    gold = (206, 162, 60)
    plate(m, a, disc_mask(3.6, inner=0), -3.6, -3.6, 0.5, lambda i, j, f: (col(shade(gold, 1.2 if (i + j) % 4 == 0 else 0.95)), None), "xz", -0.4)
    ring = disc_mask(5.5, inner=4.2)
    ring[ring.shape[0] // 2:, :] &= False
    plate(m, a, ring[:6], -5.5, -14, 0.5, lambda i, j, f: (col(shade(gold, 1.1)), None), "xy")
    box(m, a, -5.6, -14.5, -0.5, -4.4, -0.8, 0.5, metal_paint(gold))                 # the knuckle bow
    box(m, a, -3.5, -1.4, -0.7, 3.5, -0.4, 0.7, metal_paint(gold))                   # the quillons
    box(m, a, -1.1, -13, -1.1, 1.1, -1.4, 1.1, wrap_paint((60, 36, 24), (206, 162, 60), diamonds=False))
    box(m, a, -1.8, -15.6, -1.8, 1.8, -13, 1.8, metal_paint(gold, gem=(60, 140, 230), glow_rgb=(130, 210, 255)))
    return dict(glow=("82D2FF", 0.1, 0.45, 1.0))


def greatsword(m, a):
    """The Severing Greatsword: a broad, heavy blade with a deep fuller lit blue, a massive gold crossguard, a leather grip."""
    L = 50
    st = straight_stations(L, 7.2, 6.4, 8, steps=4, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((190, 196, 208)), None
        c = steel(x, y, (176, 182, 196), e, (236, 240, 248), None, None, 20)
        g = None
        if abs(x) < 1.3 and 2 < y < L - 12:
            c = np.array((86, 96, 120), np.float32) + nz(x, y) * 10              # the fuller
            if abs(x) < 0.6:
                g = col((40, 165, 245), 0.55 + 0.4 * (math.sin(y * 0.5) > 0.4))
        elif abs(x) < 1.9 and 2 < y < L - 12:
            c = c * 0.78
        return col(c), g
    Blade(st, 0.5, 1.3, paint).build(m, a)
    gold = (198, 150, 56)
    box(m, a, -13, -2.5, -2, 13, 0.5, 2, metal_paint(gold, rivets=True))
    box(m, a, -15, -4, -2.4, -12, 1.5, 2.4, metal_paint(gold))
    box(m, a, 12, -4, -2.4, 15, 1.5, 2.4, metal_paint(gold))
    box(m, a, -2.6, -2, -2.6, 2.6, 2, 2.6, metal_paint(gold, gem=(30, 120, 230), glow_rgb=(40, 165, 245)))
    box(m, a, -1.6, -20, -1.6, 1.6, -2.5, 1.6, wrap_paint((92, 58, 36)))
    box(m, a, -2.6, -24, -2.6, 2.6, -20, 2.6, metal_paint(gold, gem=(30, 120, 230), glow_rgb=(40, 165, 245)))
    return dict(glow=("28A5F5", 0.08, 0.5, 1.0))


def dweller(white):
    """The Demon-Dweller: long and straight with a deep central fuller broken by cross-shaped notches. Asta's is black with
    the void showing through the notches; Licht's is white, its notches lit gold."""
    def build(m, a):
        L = 50
        st = straight_stations(L, 4.2, 3.6, 6, steps=4, ridge=0)
        notches = [8, 16, 24, 32, 40]

        def notch(x, y):
            for n in notches:
                if (abs(y - n) < 0.6 and abs(x) < 1.8) or (abs(y - n) < 1.8 and abs(x) < 0.6):
                    return True
            return False

        def paint(x, y, front, e, rd):
            if x is None:
                return (col((210, 212, 220)) if white else col((40, 36, 42))), None
            if white:
                c = steel(x, y, (222, 224, 232), e, (250, 250, 255), None, None, 12)
                g = None
                if abs(x) < 1.1 and 2 < y < L - 8:
                    c = np.array((186, 190, 204), np.float32)
                if notch(x, y):
                    c, g = np.array((255, 236, 170), np.float32), col((255, 224, 112), 1.0)
                elif e < 0.9:
                    g = col((255, 240, 190), 0.35)
                return col(c), g
            c = steel(x, y, (36, 34, 40), e, (96, 90, 100), None, None, 18)
            if abs(x) < 1.1 and 2 < y < L - 8:
                c = np.array((20, 18, 22), np.float32) + nz(x, y) * 8
            if notch(x, y):
                c = np.array((70, 14, 24), np.float32)
            return col(c), None
        voids = [] if white else [(-1.8, n - 0.6, 1.8, n + 0.6, f) for n in notches for f in (True, False)] + \
            [(-0.6, n - 1.8, 0.6, n - 0.6, f) for n in notches for f in (True, False)] + [(-0.6, n + 0.6, 0.6, n + 1.8, f) for n in notches for f in (True, False)]
        Blade(st, 0.7, 0.7, paint).build(m, a, voids)
        guard = (210, 206, 200) if white else (44, 40, 46)
        trim = (214, 172, 70) if white else (120, 24, 36)
        box(m, a, -8, -2, -1.6, 8, 0.4, 1.6, metal_paint(guard, rivets=True))
        box(m, a, -9.2, -2.6, -1.9, -7.6, 1, 1.9, metal_paint(trim))
        box(m, a, 7.6, -2.6, -1.9, 9.2, 1, 1.9, metal_paint(trim))
        box(m, a, -1.3, -16, -1.3, 1.3, -2, 1.3, wrap_paint((230, 226, 214) if white else (30, 26, 30), trim, diamonds=True))
        box(m, a, -2.1, -19, -2.1, 2.1, -16, 2.1, metal_paint(trim))
        return dict(glow=("FFE070", 0.07, 0.55, 1.0)) if white else dict()
    return build


def destroyer(white):
    """The Demon-Destroyer: a heavy straight blade with a broken, notched patch near the tip and a four-leaf clover guard.
    Asta's is black and red; Licht's is white with a gold clover. The broken patch shows the void on both."""
    def build(m, a):
        L = 44
        st = [(0, -5, 0, 5), (12, -5, 0, 5), (24, -4.8, 0, 4.8), (31, -4.6, 0, 4.6), (33, -3.4, 0, 4.6), (35, -4.4, 0, 4.4), (38, -4.3, 0, 4.3), (L, 0, 0, 0)]
        patch = [(-3.2, 27, -0.6, 30.5), (0.6, 18, 3, 21), (-2, 10, 0, 12.5)]

        def paint(x, y, front, e, rd):
            if x is None:
                return (col((214, 214, 222)) if white else col((34, 30, 36))), None
            g = None
            if white:
                c = steel(x, y, (226, 228, 236), e, (252, 252, 255), None, None, 12)
                if e < 0.9:
                    g = col((255, 236, 170), 0.4)
            else:
                c = steel(x, y, (40, 36, 42), e, (110, 100, 108), None, None, 20)
                if abs(x) < 0.5 and 3 < y < L - 8:
                    c = np.array((130, 22, 36), np.float32)                     # Asta's red line
            for (px0, py0, px1, py1) in patch:
                if px0 - 0.8 <= x <= px1 + 0.8 and py0 - 0.8 <= y <= py1 + 0.8:
                    c = np.array((60, 30, 36) if not white else (120, 110, 116), np.float32)
            return col(c), g
        voids = [(px0, py0, px1, py1, f) for (px0, py0, px1, py1) in patch for f in (True, False)]
        Blade(st, 0.75, 0.75, paint).build(m, a, voids)
        leaf = (220, 178, 70) if white else (44, 38, 44)
        hub = (240, 236, 230) if white else (140, 24, 38)
        cm = clover_guard_mask(8.5)
        n = cm.shape[0]

        def clover_paint(i, j, f):
            d = math.hypot(i + 0.5 - n / 2, j + 0.5 - n / 2)
            if d < 2.2:
                return col(hub), (col((255, 224, 112), 0.8) if white else None)
            c = np.array(leaf, np.float32) * (1.25 if (d > n / 2 - 1.6) else 1.0) + fz(i, j) * 16
            return col(c), None
        plate(m, a, cm, -n / 2, -1 - n / 2, 1.2, clover_paint, "xy")
        box(m, a, -1.4, -15, -1.3, 1.4, -n / 2 + 0.2, 1.3, wrap_paint((232, 228, 216) if white else (28, 24, 28), leaf, diamonds=True))
        box(m, a, -2.2, -18, -2.2, 2.2, -15, 2.2, metal_paint(leaf))
        return dict(glow=("FFE070", 0.07, 0.55, 1.0)) if white else dict()
    return build


def rimeheart(m, a):
    """The Rimeheart Runeblade: a pale crystalline blade over a frost lattice, glowing cyan runes, ice-crystal guard."""
    L = 50
    st = [(0, -5, 0, 5), (10, -6, 0, 6), (30, -5.4, 0, 5.4), (40, -4.4, 0, 4.4), (L, 0, 0, 0)]

    def paint(x, y, front, e, rd):
        if x is None:
            return col((170, 220, 236)), col((80, 220, 240), 0.5)
        c = steel(x, y, (150, 196, 214), e, (226, 246, 252), rd, (196, 236, 248), 18)
        g = None
        lat = (abs(((x + y) % 6) - 3) < 0.35 or abs(((x - y) % 6) - 3) < 0.35)    # the frost lattice
        if lat and e > 1.3:
            c = c * 0.86
            g = col((60, 200, 230), 0.25)
        if rd < 0.7 and 4 < y < L - 10 and hsh(int(y)) > 0.35:
            c = np.array((90, 230, 246), np.float32)
            g = col((80, 230, 250), 1.0)
        if e < 0.7:
            g = col((140, 240, 255), 0.35)
        return col(c), g
    Blade(st, 0.4, 1.5, paint).build(m, a)
    n = 9
    yy, xx = np.mgrid[0:n, 0:25] + 0.5
    guard = (np.abs(yy - n / 2) < 1.6 + (np.abs(xx - 12.5) < 3) * 2.5) | ((np.abs(xx - 12.5) - 4) * 0.5 > np.abs(yy - 1.5) - 0.2)
    guard &= (np.abs(xx - 12.5) < 12)

    def gpaint(i, j, f):
        c = np.array((170, 220, 238), np.float32) + fz(i, j, 5) * 30
        if abs(i + 0.5 - 12.5) < 2 and abs(j + 0.5 - 4.5) < 2:
            return col((60, 220, 250)), col((80, 230, 250), 1.0)
        return col(c), None
    plate(m, a, guard, -12.5, -5, 1.4, gpaint, "xy")
    box(m, a, -1.5, -18, -1.4, 1.5, -5, 1.4, wrap_paint((40, 60, 86), (150, 210, 230), diamonds=True))
    box(m, a, -2.2, -21, -2.2, 2.2, -18, 2.2, metal_paint((170, 220, 238), gem=(60, 220, 250), glow_rgb=(80, 230, 250)))
    return dict(glow=("50E6FA", 0.1, 0.45, 1.0))


def trident(m, a):
    """The Otherworld Trident: a long violet-black shaft with silver bands and three violet-lit barbed prongs."""
    shaft_top = 0

    def prong_paint(x, y, front, e, rd):
        if x is None:
            return col((120, 80, 180)), col((170, 110, 255), 0.6)
        c = steel(x, y, (60, 44, 92), e, (200, 170, 255), rd, (110, 80, 160), 16)
        g = col((170, 110, 255), 0.85) if e < 0.8 else None
        return col(c), g
    for (dx, L, w) in ((0, 18, 1.8), (-4.5, 13, 1.4), (4.5, 13, 1.4)):
        st = [(0, -w, 0, w), (L - 5, -w * 0.8, 0, w * 0.8), (L - 4, -w * 1.6, 0, w * 1.6), (L, 0, 0, 0)]
        Blade(st, 0.3, 0.8, prong_paint, dx=dx).build(m, a)
    box(m, a, -6, -2, -1.2, 6, 0, 1.2, metal_paint((70, 50, 100), rivets=True))
    box(m, a, -6, -2, -1.2, -3.2, 2, 1.2, metal_paint((70, 50, 100)))
    box(m, a, 3.2, -2, -1.2, 6, 2, 1.2, metal_paint((70, 50, 100)))
    box(m, a, -1.8, -4, -1.8, 1.8, -2, 1.8, metal_paint((180, 176, 200), gem=(150, 80, 240), glow_rgb=(170, 110, 255)))
    box(m, a, -1, -50, -1, 1, -4, 1, metal_paint((40, 28, 60)))
    for yb in (-14, -28, -42):
        box(m, a, -1.3, yb - 1, -1.3, 1.3, yb, 1.3, metal_paint((180, 176, 200)))
    box(m, a, -1.5, -53, -1.5, 1.5, -50, 1.5, metal_paint((150, 80, 240), glow_rgb=(170, 110, 255), gem=(150, 80, 240)))
    return dict(glow=("AA6EFF", 0.09, 0.5, 1.0))


def magic_tool_sword(m, a):
    """The mana-forged tool sword: bright steel, a gold crossguard, a dark grip, a cyan mana stone pommel."""
    L = 30
    st = straight_stations(L, 2.6, 2.2, 4, steps=2, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((210, 212, 222)), None
        return col(steel(x, y, (200, 204, 216), e, (244, 246, 252), rd, (226, 230, 240), 14)), None
    Blade(st, 0.35, 0.9, paint).build(m, a)
    gold = (214, 168, 60)
    box(m, a, -6, -2, -1.4, 6, 0.2, 1.4, metal_paint(gold))
    box(m, a, -1.2, -10, -1.2, 1.2, -2, 1.2, wrap_paint((70, 44, 28)))
    box(m, a, -2, -13, -2, 2, -10, 2, metal_paint((90, 220, 230), gem=(90, 230, 240), glow_rgb=(120, 240, 250)))
    return dict(glow=("78F0FA", 0.08, 0.5, 1.0))


def magic_tool_spear(m, a):
    """The mana-forged tool spear: a wooden shaft, a steel collar, a leaf-shaped cyan-edged head."""
    st = [(0, -2.2, 0, 2.2), (5, -3.2, 0, 3.2), (11, -1.6, 0, 1.6), (14, 0, 0, 0)]

    def paint(x, y, front, e, rd):
        if x is None:
            return col((210, 220, 228)), None
        c = steel(x, y, (196, 206, 218), e, (130, 236, 244), rd, (228, 236, 244), 14)
        g = col((120, 240, 250), 0.6) if e < 0.9 else None
        return col(c), g
    Blade(st, 0.3, 0.9, paint).build(m, a)
    box(m, a, -1.3, -3, -1.3, 1.3, 0, 1.3, metal_paint((170, 176, 186)))

    def wood(face, i, j, fw, fh):
        c = np.array((120, 82, 46), np.float32) + fz(i, j * 0.3, face) * 30 + (12 if (i % 2) else 0)
        return col(c), None
    box(m, a, -0.9, -38, -0.9, 0.9, -3, 0.9, wood)
    box(m, a, -1.2, -40, -1.2, 1.2, -38, 1.2, metal_paint((90, 220, 230)))
    return dict(glow=("78F0FA", 0.08, 0.4, 0.9))


SPECS = [
    ("demon_slasher_katana", demon_slasher),
    ("miasma_infused_katana", miasma),
    ("spell_forged_rapier", rapier),
    ("severing_greatsword", greatsword),
    ("demon_dweller_sword", dweller(False)),
    ("demon_destroyer_sword", destroyer(False)),
    ("licht_dweller_sword", dweller(True)),
    ("licht_destroyer_sword", destroyer(True)),
    ("rimeheart_runeblade", rimeheart),
    ("otherworld_trident", trident),
    ("magic_tool_sword", magic_tool_sword),
    ("magic_tool_spear", magic_tool_spear),
]


# ================================================================ relics (0.50: the same standard for the mod's items)
def ascii_mask(rows):
    return np.array([[ch != "." for ch in r] for r in rows], bool)


def flat(rgb, n=14, salt=0):
    """A box painter: one colour with fine noise and bevelled corners."""
    def p(face, i, j, fw, fh):
        c = np.array(rgb, np.float32) * (1.12 if face == 0 else 0.85 if face == 1 else 1.0) + fz(i * 1.3, j * 1.7, face + salt) * n
        if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
            c = c * 1.15
        return col(c), None
    return p


def anti_bird_charm(m, a):
    """A black crow talisman: feathered body, head with a white eye, a gold beak, fanned tail and folded wings."""
    feather = (26, 24, 30)

    def plumage(face, i, j, fw, fh):
        c = np.array(feather, np.float32) + fz(i * 2, j * 2, face) * 16 + (10 if (i + 2 * j) % 5 == 0 else 0)
        return col(c), None
    box(m, a, -4, -3, -3, 3, 3, 3, plumage)

    def head(face, i, j, fw, fh):
        c = np.array(feather, np.float32) + fz(i, j, 9) * 12
        if face in (3, 5) and i == fw - 2 and j == 1:
            return col((236, 236, 240)), None                          # the eye
        return col(c), None
    box(m, a, 2, 1, -2.5, 6, 5, 2.5, head)
    box(m, a, 6, 2, -1, 8.5, 3.5, 1, flat((214, 160, 40)))
    tail = ascii_mask(["#....", "##...", "####.", "#####", "####.", "##...", "#...."])
    plate(m, a, tail[:, ::-1], -9, -3.5, 1.2, lambda i, j, f: (col(shade(feather, 1.3 if (i + j) % 2 else 1.0)), None), "xy")
    for z in (-3.3, 3.3):
        wing = ascii_mask(["######.", "#######", ".######", "..####."])
        plate(m, a, wing, -4, -1, 0.35, lambda i, j, f: (col(shade((40, 38, 46), 1.25 if j == 0 else 1.0)), None), "xy", z)
    for x in (-1.5, 0.5):
        box(m, a, x, -6, -0.5, x + 1, -3, 0.5, flat((214, 160, 40)))
    return dict()


def heart_mask(n):
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    x, y = (xx - n / 2) / (n / 2), -(yy - n / 2) / (n / 2) + 0.15
    return (x * x + (y - np.sqrt(np.abs(x)) * 0.75) ** 2 * 1.6) < 0.62


def bond_thread(m, a):
    """A red heart of woven thread, its loose end trailing down: Bond Magic's red thread, faintly lit."""
    hm = heart_mask(13)

    def paint(i, j, f):
        c = np.array((214, 40, 66), np.float32) + fz(i * 2, j * 2, 3) * 20
        if (i + j) % 3 == 0:
            c = c * 0.8                                                   # the weave
        g = col((255, 90, 120), 0.25) if (i - j) % 4 == 0 else None
        return col(c), g
    plate(m, a, hm, -6.5, -2, 1.6, paint, "xy")
    pts = [(0, -2), (0.8, -4), (2, -5.6), (3.4, -6.8), (4.4, -8.4)]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        box(m, a, min(x0, x1) - 0.4, y1 - 0.2, -0.4, max(x0, x1) + 0.4, y0 + 0.2, 0.4,
            lambda face, i, j, fw, fh: (col((200, 30, 56)), col((255, 90, 120), 0.5)))
    return dict(glow=("FF5A78", 0.08, 0.5, 1.0))


def comm_device(m, a):
    """The communication magic device: a dark-rimmed blue disc, its mana crystal glowing in the middle."""
    disc = disc_mask(7)

    def paint(i, j, f):
        d = math.hypot(i + 0.5 - 7, j + 0.5 - 7)
        if d > 5.6:
            return col((24, 30, 48)), None
        if d > 4.4:
            return col((60, 100, 170)), None
        c = np.array((80, 140, 210), np.float32) + fz(i * 2, j * 2, 4) * 20
        return col(c), (col((140, 210, 255), 0.35) if abs(d - 3.5) < 0.6 else None)
    plate(m, a, disc, -7, -7, 1.4, paint, "xy")

    def gem(face, i, j, fw, fh):
        return col((170, 230, 255)), col((170, 230, 255), 1.0)
    box(m, a, -2, -2, -2.2, 2, 2, 2.2, gem)
    return dict(glow=("AAE6FF", 0.1, 0.45, 1.0))


def devil_contract(m, a):
    """A devil's contract: a black-red leather tome with crimson binding straps and a gold sigil."""
    def cover(face, i, j, fw, fh):
        c = np.array((54, 14, 20), np.float32) + fz(i * 2, j * 2, 5) * 14
        if face in (3, 5) and j in (2, 3, 5, 6):
            c = np.array((176, 26, 40), np.float32)                     # the straps
        if face in (0, 1, 2, 4) and fw <= 4:
            c = np.array((222, 214, 190), np.float32) if j % 2 == 0 else np.array((196, 186, 160), np.float32)   # the pages
        return col(c), None
    box(m, a, -6, -7, -2, 6, 7, 2, cover)
    sig = ascii_mask(["..#..", ".###.", "#####", "..#..", ".###."])
    plate(m, a, sig, -2.5, -5.5, 0.4, lambda i, j, f: (col((226, 176, 60)), col((255, 120, 60), 0.35)), "xy", 2.3)
    plate(m, a, sig, -2.5, -5.5, 0.4, lambda i, j, f: (col((226, 176, 60)), col((255, 120, 60), 0.35)), "xy", -2.3)
    return dict(glow=("FF7840", 0.06, 0.4, 1.0))


def fortune_die(m, a):
    """The fortune die: a white rounded cube with cyan pips that glow."""
    pips = {0: [(1, 1)], 1: [(0, 0), (2, 2)], 2: [(0, 0), (1, 1), (2, 2)], 3: [(0, 0), (0, 2), (2, 0), (2, 2)],
            4: [(0, 0), (0, 2), (1, 1), (2, 0), (2, 2)], 5: [(0, 0), (0, 1), (0, 2), (2, 0), (2, 1), (2, 2)]}

    def face(k, i, j, fw, fh):
        c = np.array((232, 236, 240), np.float32) + fz(i, j, k) * 8
        if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
            c = c * 0.86
        for (pi, pj) in pips[k]:
            if abs(i - (1.5 + pi * 2.5)) < 0.9 and abs(j - (1.5 + pj * 2.5)) < 0.9:
                return col((60, 200, 240)), col((90, 220, 255), 1.0)
        return col(c), None
    box(m, a, -4.5, -4.5, -4.5, 4.5, 4.5, 4.5, face)
    return dict(glow=("5ADCFF", 0.12, 0.45, 1.0))


def hand_mirror(m, a):
    """Gauche's hand mirror: an oval silver frame round a pale blue glass, a dark slender handle."""
    oval = np.zeros((16, 12), bool)
    yy, xx = np.mgrid[0:16, 0:12] + 0.5
    d = ((xx - 6) / 6) ** 2 + ((yy - 8) / 8) ** 2
    oval = d <= 1

    def paint(i, j, f):
        dd = ((i + 0.5 - 6) / 6) ** 2 + ((j + 0.5 - 8) / 8) ** 2
        if dd > 0.62:
            return col(shade((196, 204, 220), 1.2 if (i + j) % 3 == 0 else 1.0)), None
        c = np.array((150, 200, 236), np.float32) + (40 if abs((i - j) + 3) < 1.2 else 0)   # the glass, a glint across it
        return col(c), col((180, 220, 255), 0.25)
    plate(m, a, oval, -6, 0, 1.0, paint, "xy")
    box(m, a, -1, -2, -1.2, 1, 0.5, 1.2, flat((196, 204, 220)))
    box(m, a, -1, -18, -1, 1, -2, 1, flat((70, 56, 80)))
    box(m, a, -1.1, -19.5, -1.1, 1.1, -18, 1.1, flat((196, 204, 220)))
    return dict(glow=("B4DCFF", 0.05, 0.4, 0.9))


def grimoire_chain(m, a):
    """The grimoire chain: five heavy steel links, each turned a quarter to the last."""
    link = disc_mask(3.2, inner=1.6)
    link = link[:, 1:-1] if link.shape[1] > 6 else link
    steel_c = (170, 176, 190)
    for k in range(5):
        y = -12 + k * 5
        paintf = (lambda i, j, f: (col(shade(steel_c, 1.25 if (i + j) % 3 == 0 else 0.95)), None))
        if k % 2 == 0:
            plate(m, a, link, -link.shape[1] / 2, y, 1.0, paintf, "xy")
        else:
            box(m, a, -1, y, -2.8, 1, y + 6.4, -1.4, flat(steel_c))
            box(m, a, -1, y, 1.4, 1, y + 6.4, 2.8, flat(steel_c))
            box(m, a, -1, y, -1.4, 1, y + 1.2, 1.4, flat(steel_c))
            box(m, a, -1, y + 5.2, -1.4, 1, y + 6.4, 1.4, flat(steel_c))
    return dict()


def rune_stone(m, a):
    """The mana-method rune stone: a grey diamond of stone with a pink rune ring glowing on both faces."""
    n = 15
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    dm = (np.abs(xx - n / 2) + np.abs(yy - n / 2)) <= n / 2

    def paint(i, j, f):
        dx, dy = i + 0.5 - n / 2, j + 0.5 - n / 2
        r = math.hypot(dx, dy)
        c = np.array((112, 108, 116), np.float32) + nz(i, j, 6) * 40 + fz(i, j, 6) * 16
        if abs(r - 3.4) < 0.7 or (r < 3.4 and (abs(dx) < 0.5 or abs(dy) < 0.5)):
            return col((236, 120, 190)), col((255, 140, 210), 1.0)
        return col(c), None
    plate(m, a, dm, -n / 2, -n / 2, 1.8, paint, "xy")
    return dict(glow=("FF8CD2", 0.09, 0.4, 1.0))


def recovery_salve(m, a):
    """The recovery salve: a squat glass jar of green salve with a cork lid."""
    def jar(face, i, j, fw, fh):
        if face == 0:
            return col((120, 210, 140)), None
        c = np.array((110, 206, 132), np.float32) + fz(i, j, 7) * 18 if j >= 3 else np.array((206, 230, 236), np.float32)
        if (face in (3, 5)) and 2 <= i <= fw - 3 and 5 <= j <= 7:
            c = np.array((236, 226, 196), np.float32)                   # the label
        return col(c), (col((140, 255, 170), 0.3) if j >= 3 else None)
    box(m, a, -5, -6, -5, 5, 3, 5, jar)
    box(m, a, -3.5, 3, -3.5, 3.5, 4.5, 3.5, flat((206, 230, 236)))
    box(m, a, -3, 4.5, -3, 3, 7, 3, flat((150, 112, 72), 24))
    return dict(glow=("8CFFAA", 0.05, 0.4, 0.8))


def spirit_charm(m, a):
    """The spirit charm: a cyan crystal held in a gold cap, lit from inside."""
    n = 13
    yy, xx = np.mgrid[0:n, 0:n] + 0.5
    dm = (np.abs(xx - n / 2) * 1.0 + np.abs(yy - n / 2) * 0.8) <= n / 2 * 0.9

    def paint(i, j, f):
        dx, dy = i + 0.5 - n / 2, j + 0.5 - n / 2
        c = np.array((96, 220, 236), np.float32) + (50 if dx + dy < -2 and abs(dx - dy) < 2 else 0)
        if abs(abs(dx) * 1.0 + abs(dy) * 0.8 - n / 2 * 0.9) < 0.9:
            c = np.array((60, 170, 200), np.float32)
        return col(c), col((120, 240, 255), 0.6)
    plate(m, a, dm, -n / 2, -n / 2, 2.0, paint, "xy")
    box(m, a, -2, 5, -1.5, 2, 7, 1.5, flat((222, 176, 60)))
    box(m, a, -1, 7, -0.5, 1, 9, 0.5, flat((222, 176, 60)))
    return dict(glow=("78F0FF", 0.1, 0.45, 1.0))


def written_consent(m, a):
    """Written consent: a parchment sheet between two rolled ends, lines of writing and a red wax seal."""
    def sheet(face, i, j, fw, fh):
        c = np.array((232, 220, 192), np.float32) + fz(i * 2, j * 2, 8) * 14
        if face in (3, 5) and 2 <= i <= fw - 3 and j % 2 == 1 and j < fh - 2 and (i + j * 3) % 7 != 0:
            c = np.array((90, 76, 70), np.float32)
        return col(c), None
    box(m, a, -6, -6, -0.3, 6, 6, 0.3, sheet)
    for y in (-7.2, 6):
        box(m, a, -6.6, y, -1, 6.6, y + 1.6, 1, flat((206, 190, 156)))
    box(m, a, 2.5, -5, 0.3, 5, -2.5, 1.0, flat((190, 24, 36)))
    return dict()


def last_word(m, a):
    """Last Word, Zagred's quill-blade: a long black rapier with a feather-barbed edge and red text running down the fuller."""
    L = 54
    st = straight_stations(L, 2.0, 1.2, 6, steps=3, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((40, 32, 52)), None
        c = steel(x, y, (38, 32, 48), e, (120, 112, 140), rd, (64, 56, 80), 12)
        g = None
        if abs(x) < 0.5 and 4 < y < L - 8:
            c = np.array((214, 34, 56), np.float32)
            g = col((255, 60, 90), 0.5 + 0.5 * (int(y) % 3 != 0))
        return col(c), g
    Blade(st, 0.3, 0.9, paint).build(m, a)
    gold = (214, 176, 70)
    box(m, a, -5.5, -1.2, -1, 5.5, 0.6, 1, metal_paint(gold, gem=(200, 30, 54), glow_rgb=(255, 60, 90)))
    for sx in (-1, 1):                                                           # the quill's swept guard tips
        box(m, a, sx * 5.5 - 0.6, -2.6, -0.8, sx * 5.5 + 0.6, -1.2, 0.8, metal_paint(gold))
    box(m, a, -1.1, -14, -1.1, 1.1, -1.2, 1.1, wrap_paint((30, 24, 34), (58, 40, 48), diamonds=True))
    box(m, a, -1.8, -16.4, -1.8, 1.8, -14, 1.8, metal_paint(gold, gem=(200, 30, 54), glow_rgb=(255, 60, 90)))
    return dict(glow=("FF3C5A", 0.1, 0.5, 1.0))


def shroud_of_margins(m, a):
    """The Shroud of Margins: a folded violet-black cloak with a white margin and lines of writing, a glowing clasp."""
    def cloth(rgb):
        def p(face, i, j, fw, fh):
            c = np.array(rgb, np.float32) + fz(i * 2, j * 2, 11) * 14
            g = None
            if face in (3, 5):
                if i in (0, fw - 1):
                    c = np.array((206, 196, 232), np.float32)                      # the white margin
                elif j % 2 == 1 and 1 < i < fw - 2 and (i + j) % 5:
                    c = np.array((150, 120, 220), np.float32)
                    g = col((170, 140, 255), 0.5)
            return col(c), g
        return p
    box(m, a, -4.5, -3, -2, 4.5, 4, 2, cloth((38, 26, 62)))
    box(m, a, -5.5, -9, -2.4, 5.5, -3, 2.4, cloth((38, 26, 62)))
    box(m, a, -3.5, 4, -2.5, 3.5, 8, 2.5, cloth((24, 16, 44)))                       # the hood
    box(m, a, -1, 2.5, 2, 1, 4, 3, metal_paint((230, 220, 255), gem=(190, 160, 255), glow_rgb=(190, 160, 255)))
    return dict(glow=("B49BFF", 0.08, 0.4, 0.9))


def circlet_of_thought(m, a):
    """The Circlet of Quickened Thought: a gold band with a cyan stone and two small swept wings."""
    ring = disc_mask(7, inner=5.4)
    plate(m, a, ring, -7, -7, 0.9, lambda i, j, f: (col(shade((214, 176, 70), 1.2 if (i + j) % 3 == 0 else 0.95)), None), "xy")
    box(m, a, -1.5, 3.5, -1.2, 1.5, 6.5, 1.2, metal_paint((110, 230, 250), gem=(150, 240, 255), glow_rgb=(150, 240, 255)))
    for sx in (-1, 1):
        wing = ascii_mask(["..##", ".###", "####", "###."])
        plate(m, a, wing[:, ::-1] if sx < 0 else wing, sx * 7.5 - (4 if sx < 0 else 0), -1, 0.4, lambda i, j, f: (col(shade((240, 232, 200), 1.1 if j == 0 else 0.95)), None), "xy")
    return dict(glow=("96F0FF", 0.1, 0.45, 1.0))


RELICS = [
    ("anti_bird_charm", anti_bird_charm),
    ("bond_thread", bond_thread),
    ("communication_magic_device", comm_device),
    ("devil_contract", devil_contract),
    ("fortune_die", fortune_die),
    ("mana_method_rune_stone", rune_stone),
    ("recovery_salve", recovery_salve),
    ("spirit_charm", spirit_charm),
    ("written_consent", written_consent),
    ("shroud_of_margins", shroud_of_margins),
    ("circlet_of_quickened_thought", circlet_of_thought),
]
SPECS += [("gauches_hand_mirror", hand_mirror), ("grimoire_chain", grimoire_chain), ("last_word", last_word)]                     # diagonal sprites: placed like the weapons


# ================================================================ placement on the old sprite
def sprite_axis(item_id):
    a = np.asarray(Image.open(os.path.join(ASSETS, "textures", "item", item_id + ".png")).convert("RGBA"))
    S = a.shape[0]
    ys, xs = np.nonzero(a[..., 3] > 40)
    P = np.stack([(xs + 0.5) / S, 1 - (ys + 0.5) / S], 1)
    c = P.mean(0)
    d = np.linalg.svd(P - c)[2][0]
    if d[0] < 0:
        d = -d
    t = (P - c) @ d
    return c + d * t.min(), math.degrees(math.atan2(d[1], d[0])), t.max() - t.min()


def sprite_box(item_id):
    a = np.asarray(Image.open(os.path.join(ASSETS, "textures", "item", item_id + ".png")).convert("RGBA"))
    S = a.shape[0]
    ys, xs = np.nonzero(a[..., 3] > 40)
    return (xs.min()) / S, 1 - (ys.max() + 1) / S, (xs.max() + 1) / S, 1 - ys.min() / S


# 0.52: the katana blades faced the wrong way in hand: their meshes are mirrored across x (edge forward, curve forward)
FLIP = {"demon_slasher_katana", "miasma_infused_katana"}
# 0.52: scale about a grip point (fraction along the sprite axis); the trident was too small in hand
SIZE = {"otherworld_trident": 1.65}
GRIP = {"otherworld_trident": 0.42}


def mirror_x(mesh):
    out = []
    for (layer, pts, n) in mesh.quads:
        q = [(-p[0],) + tuple(p[1:]) for p in pts]
        out.append((layer, [q[0], q[3], q[2], q[1]], np.array([-n[0], n[1], n[2]])))
    mesh.quads = out


def write(item_id, mesh, atlas, extra, fit=False):
    if item_id in FLIP:
        mirror_x(mesh)
    ys = [p[1] for (_, pts, _) in mesh.quads for p in pts]
    ymin, ymax = min(ys), max(ys)
    if fit:                                                      # a relic: upright, centred on its sprite's box, same size
        xs = [p[0] for (_, pts, _) in mesh.quads for p in pts]
        x0, y0, x1, y1 = sprite_box(item_id)
        scale = min((x1 - x0) / (max(xs) - min(xs)), (y1 - y0) / (ymax - ymin)) * 0.95
        cx = (min(xs) + max(xs)) / 2
        origin = ((x0 + x1) / 2 - cx * scale, (y0 + y1) / 2 - (ymax - ymin) * scale / 2)
        angle = 90.0
    else:
        origin, angle, length = sprite_axis(item_id)
        scale = length / (ymax - ymin)
        k = SIZE.get(item_id, 1.0)
        if k != 1.0:                                                  # grow about the grip, not the butt
            d = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))])
            origin = origin + d * (GRIP.get(item_id, 0.4) * length) * (1 - k)
            scale *= k
    path = os.path.join(ASSETS, "weapon_meshes")
    os.makedirs(path, exist_ok=True)
    glow_any = atlas.glow[..., 3].max() > 0
    lines = [f"# generated by tools/gen_weapon_models.py ({item_id})",
             f"place {origin[0]:.5f} {origin[1]:.5f} {angle:.3f} {scale:.6f} {ymin:.3f}"]
    if glow_any and extra.get("glow"):
        g = extra["glow"]
        lines.append(f"glow {g[0]} {g[1]} {g[2]} {g[3]}")
    n = 0
    for (layer, pts, nrm) in mesh.quads:
        lines.append("q %d %s %.3f %.3f %.3f" % (layer, " ".join("%.3f %.3f %.3f %.2f %.2f" % tuple(p) for p in pts), *nrm))
        n += 1
        if layer == 0 and glow_any:                                 # an emissive copy where the quad's uv has glow pixels
            us = [p[3] for p in pts]
            vs = [p[4] for p in pts]
            u0, u1 = int(max(0, math.floor(min(us)))), int(min(TS, math.ceil(max(us)) + 0))
            v0, v1 = int(max(0, math.floor(min(vs)))), int(min(TS, math.ceil(max(vs)) + 0))
            if u1 == u0:
                u1 = u0 + 1
            if v1 == v0:
                v1 = v0 + 1
            if atlas.glow[v0:v1, u0:u1, 3].max(initial=0) > 0:
                lines.append("q 2 %s %.3f %.3f %.3f" % (" ".join("%.3f %.3f %.3f %.2f %.2f" % tuple(p) for p in pts), *nrm))
                n += 1
    with open(os.path.join(path, item_id + ".txt"), "w", newline="\n") as f:
        f.write("\n".join(lines) + "\n")
    tdir = os.path.join(ASSETS, "textures", "entity", "weapon")
    os.makedirs(tdir, exist_ok=True)
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(tdir, item_id + ".png"), optimize=True)
    if glow_any:
        Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(tdir, item_id + "_glow.png"), optimize=True)
    print(f"{item_id}: {n} quads, length {ymax - ymin:.1f}px, angle {angle:.1f}, glow {bool(glow_any)}")


def main(only=None):
    for fit, specs in ((False, SPECS), (True, RELICS)):
        for item_id, fn in specs:
            if only and item_id not in only:
                continue
            mesh, atlas = Mesh(), Atlas()
            extra = fn(mesh, atlas) or {}
            write(item_id, mesh, atlas, extra, fit)


if __name__ == "__main__":
    main(sys.argv[1:])

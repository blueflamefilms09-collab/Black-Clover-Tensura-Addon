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
  lathe   a surface of revolution round +y (a round club, a haft, a thorn, a mushroom), turned to any direction and its skin painted per side
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


def elsdocia(m, a):
    """Elsdocia (1.2.0 remake after the wiki and the owner's reference render): a broad blade whose top quarter is worked bronze-gold
    with a clover cut out of it, then shattered-glass crystal in deep azure with bright cracks and white-hot cells down to a long
    pointed tip; a thin gold cross guard with swept tips, a slender dark grip, a gold pommel."""
    length = 86
    stations = [
        (0, -7.2, 0, 7.2), (6, -7.8, 0, 7.8), (58, -7.8, 0, 7.8), (68, -6.4, 0, 6.4),
        (77, -3.8, 0, 3.8), (83, -1.6, 0, 1.6), (length, 0, 0, 0),
    ]
    clover_rows = ["..##.##..", ".#######.", "#########", ".#######.", "..#####..", "...###...", "....#...."]

    def clover_hit(x, y):
        c = int((x + 7.2) / 1.6)
        r = int((19.0 - y) / 1.6)
        return 0 <= r < len(clover_rows) and 0 <= c < 9 and clover_rows[r][c] == "#"

    def voronoi(x, y):
        cell = 3.6
        gx, gy = math.floor(x / cell), math.floor(y / cell)
        best = []
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                ix, iy = gx + dx, gy + dy
                px = (ix + 0.15 + 0.7 * hsh(ix, iy, 7)) * cell
                py = (iy + 0.15 + 0.7 * hsh(ix, iy, 8)) * cell
                best.append((math.hypot(x - px, y - py), ix, iy))
        best.sort()
        return best[1][0] - best[0][0], best[0][1], best[0][2]

    def paint(x, y, front, edge, ridge):
        if x is None:
            return (col((196, 150, 58)) if y < 22 else col((150, 215, 255), 0.95)), (None if y < 22 else col((120, 215, 255), 0.4))
        seam = 22.0 + 2.4 * math.sin(x * 1.0) + (hsh(int(x * 2), 3) - 0.5) * 1.8
        if y < seam:                                                     # worked bronze-gold
            base = np.array((192, 142, 52), np.float32) * (1.0 + nz(x, y, 3) * 0.55) + fz(x, y, 4) * 24
            if int(y * 2) % 7 == 0:
                base = base * 0.86
            if edge < 0.9:
                base = np.array((236, 196, 92), np.float32)
            elif edge < 1.8:
                base = base * 0.82
            if clover_hit(x, y):
                return col((18, 24, 52)), None
            return col(np.clip(base, 0, 255)), None
        d, ix, iy = voronoi(x, y)
        h1, hot = hsh(ix, iy, 11), hsh(ix, iy, 13)
        t = (y - seam) / max(1.0, length - seam)
        base = np.array((22, 66, 172), np.float32) * (1 - h1) + np.array((72, 172, 250), np.float32) * h1
        base = base * (0.82 + 0.34 * hsh(ix, iy, 17)) * (0.92 + 0.3 * math.sin(t * 3.1 + h1 * 6))
        glow = col((100, 215, 255), 0.30 + 0.25 * h1)
        if hot > 0.84:                                                   # white-hot cells
            base = np.array((228, 248, 255), np.float32) * (0.88 + 0.12 * h1)
            glow = col((205, 246, 255), 0.95)
        if d < 0.30:                                                     # bright crack lines between the shards
            base = np.array((214, 240, 255), np.float32)
            glow = col((190, 240, 255), 0.7)
        elif d < 0.75:
            base = base * 0.62
        if edge < 0.9:
            base = np.array((205, 238, 255), np.float32)
            glow = col((170, 235, 255), 0.6)
        if y > length - 6:
            base = base * 1.1 + 12
        return col(np.clip(base, 0, 255)), glow

    Blade(stations, 1.0, 1.9, paint).build(m, a)
    gold = (226, 178, 62)
    gold_paint = metal_paint(gold, gem=(150, 236, 255), glow_rgb=(150, 236, 255))
    dark_gold = metal_paint((168, 122, 40))
    box(m, a, -8.0, -1.8, -2.6, 8.0, 0.2, 2.6, dark_gold)                  # the collar the blade grows from
    box(m, a, -14.0, -2.4, -1.3, 14.0, -0.2, 1.3, gold_paint)              # the thin, wide cross guard
    for sx in (-1, 1):
        lo, hi = sorted((sx * 11.0, sx * 17.0))
        box(m, a, lo, -1.6, -1.1, hi, 2.4, 1.1, gold_paint)                # swept tips
    box(m, a, -1.5, -20.0, -1.5, 1.5, -2.4, 1.5, wrap_paint((24, 28, 40), (52, 62, 90), diamonds=True))   # slender dark grip
    box(m, a, -2.4, -22.8, -2.2, 2.4, -20.0, 2.2, gold_paint)              # pommel
    box(m, a, -1.3, -24.2, -1.3, 1.3, -22.8, 1.3, gold_paint)
    return dict(glow=("7FDFFF", 0.09, 0.45, 1.0))


# ================================================================ 0.100: the weapons of "Custom weapon i want added" (reference sheets)
def _hsv(h, sat, val):
    import colorsys
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, sat, val)
    return np.array((r * 255, g * 255, b * 255), np.float32)


def magebane_longsword(m, a):
    """Magebane Longsword: a translucent turquoise blade with brown rune bands, a dark mage-scorched crossguard, black grip."""
    L = 52
    st = straight_stations(L, 3.4, 3.0, 8, steps=4, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((70, 150, 150)), None
        c = np.array((40, 135, 138), np.float32) * (1 + 0.25 * math.sin((x + y * 0.3) * 1.1)) + fz(int(x * 2), int(y * 2), 5) * 22
        g = None
        if 6 < y < L - 10 and int(y) % 9 in (0, 1) and abs(x) < 2.2:
            c = np.array((196, 142, 74), np.float32) + fz(int(x), int(y), 9) * 20           # rune band
            g = col((255, 190, 100), 0.55)
        if e < 0.9:
            c = np.array((190, 245, 240), np.float32)
        elif abs(x) < 0.6:
            c = c * 1.25 + 10
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.7, 1.4, paint).build(m, a)
    dark = (44, 38, 44)
    box(m, a, -8.5, -2, -1.4, 8.5, 0.6, 1.4, metal_paint((70, 62, 72), rivets=True))
    for sx in (-1, 1):
        lo, hi = sorted((sx * 6.5, sx * 10.2))
        box(m, a, lo, -3.6, -1.1, hi, 1.2, 1.1, metal_paint((54, 48, 56)))
    box(m, a, -1.5, -14, -1.4, 1.5, -2, 1.4, wrap_paint((20, 18, 24), (70, 62, 72), diamonds=True))
    box(m, a, -2.4, -17, -2.1, 2.4, -14, 2.1, metal_paint((70, 62, 72), gem=(80, 220, 210), glow_rgb=(90, 235, 225)))
    return dict(glow=("5FEBDD", 0.08, 0.35, 0.95))


def prism_blade(m, a):
    """Prism Blade: a crystalline longsword whose body shifts through the whole spectrum from hilt to tip, set in a midnight-blue hilt."""
    L = 56
    st = straight_stations(L, 3.6, 3.2, 9, steps=4, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((220, 220, 255)), col((220, 220, 255), 0.4)
        hue = (y / L) * 0.85 + (0.05 if front else 0.0) + 0.04 * math.sin(x)
        c = _hsv(hue, 0.55 + 0.25 * (abs(x) / 3.6), 0.78 + 0.2 * math.sin(y * 0.7 + x))
        g = col(c * 0.9 + 25, 0.45)
        if e < 0.9:
            c = np.array((250, 250, 255), np.float32)
        elif abs(x) < 0.7:
            c = c * 0.6 + 100
        h = hsh(int(x * 2), int(y * 2), 77)
        if h > 0.96:
            c = np.array((255, 255, 255), np.float32)
            g = col((255, 255, 255), 0.95)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.8, 1.6, paint).build(m, a)
    navy = (34, 44, 96)
    box(m, a, -9, -2, -1.3, 9, 0.8, 1.3, metal_paint((200, 168, 70), gem=(120, 200, 255), glow_rgb=(150, 220, 255)))
    for sx in (-1, 1):
        lo, hi = sorted((sx * 7.0, sx * 10.5))
        box(m, a, lo, -3.2, -1.1, hi, 1.8, 1.1, metal_paint((200, 168, 70)))
    box(m, a, -1.6, -14, -1.5, 1.6, -2, 1.5, wrap_paint((26, 36, 88), (120, 190, 255), diamonds=True))
    box(m, a, -2.6, -17.2, -2.2, 2.6, -14, 2.2, metal_paint((200, 168, 70), gem=(255, 140, 220), glow_rgb=(255, 170, 235)))
    return dict(glow=("F0F4FF", 0.1, 0.5, 1.0))


def thundercleave(m, a):
    """Thundercleave: a broad dark-steel greatsword, gold-inlaid, veined with a lit, crackling thunder-blue fracture."""
    L = 56
    st = straight_stations(L, 6.0, 5.6, 9, steps=4, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((50, 70, 84)), None
        c = np.array((54, 78, 92), np.float32) + nz(x, y, 3) * 26
        g = None
        w = math.sin(y * 0.42 + (2.0 if front else 0.0)) * 1.6                   # a jagged vein snaking up the blade
        if abs(x - w) < 0.75 and y < L - 4:
            c = np.array((130, 235, 255), np.float32)
            g = col((120, 235, 255), 0.95)
        elif abs(x - w) < 1.5 and y < L - 4:
            c = c * 0.7
            g = col((60, 170, 255), 0.35)
        if abs(x) > 4.4 and int(y) % 6 < 2:
            c = np.array((214, 170, 70), np.float32)                              # gold inlay along the edge
        if e < 0.9:
            c = np.array((176, 220, 240), np.float32)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.7, 1.5, paint).build(m, a)
    gold = (206, 160, 60)
    box(m, a, -12, -2.5, -2, 12, 0.8, 2, metal_paint(gold, rivets=True))
    for sx in (-1, 1):
        lo, hi = sorted((sx * 10, sx * 14))
        box(m, a, lo, -4, -2.2, hi, 2, 2.2, metal_paint(gold))
    box(m, a, -2.8, -2, -2.6, 2.8, 2.4, 2.6, metal_paint(gold, gem=(60, 170, 250), glow_rgb=(110, 220, 255)))
    box(m, a, -1.7, -20, -1.7, 1.7, -2.5, 1.7, wrap_paint((30, 40, 56), (206, 160, 60), diamonds=True))
    box(m, a, -2.8, -24, -2.8, 2.8, -20, 2.8, metal_paint(gold, gem=(60, 170, 250), glow_rgb=(110, 220, 255)))
    return dict(glow=("7FE6FF", 0.14, 0.45, 1.0))


def bloodbound_katana(m, a):
    """Bloodbound Katana: an obsidian blade, long and curved, crimson veins pulsing, a wrapped hilt and a bleeding tsuba."""
    L = 58
    st = katana_stations(L, 4.6, 3.4, 4.0, kissaki=6.0, steps=10)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((24, 18, 24)), col((200, 20, 40), 0.3)
        c = np.array((22, 18, 26), np.float32) + nz(x, y, 6) * 14
        g = None
        v = math.sin(y * 0.33 + x * 1.3 + (1.0 if front else 4.0))
        if v > 0.82 and y < L - 3:
            c = np.array((210, 20, 44), np.float32)
            g = col((255, 40, 70), 0.9)
        elif v > 0.6 and y < L - 3:
            c = np.array((120, 12, 30), np.float32)
            g = col((200, 20, 50), 0.35)
        if e < 0.9:
            c = np.array((150, 50, 70), np.float32)                             # the honed edge
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.6, 1.1, paint).build(m, a)
    plate(m, a, disc_mask(5.2, inner=0, lobes=4, lobe_r=1.6), -5.2, -5.2, 0.5, lambda i, j, f: (col(shade((60, 20, 28), 1.3 if (i + j) % 3 == 0 else 1.0)), col((220, 30, 50), 0.35) if (i + j) % 5 == 0 else None), "xz", -0.4)
    box(m, a, -1.2, -17, -1.2, 1.2, -1, 1.2, wrap_paint((28, 20, 28), (160, 24, 44), diamonds=True))
    box(m, a, -1.9, -19, -1.9, 1.9, -17, 1.9, metal_paint((120, 20, 36), gem=(255, 60, 90), glow_rgb=(255, 60, 90)))
    return dict(glow=("FF2846", 0.12, 0.35, 1.0))


def quicklings_ivy(m, a):
    """Quickling's Ivy: a slim rapier of living green with an ivy-wrapped gold guard; leaves sprout along the blade."""
    L = 56
    st = straight_stations(L, 1.6, 1.0, 6, steps=3, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((90, 170, 70)), None
        c = np.array((84, 170, 66), np.float32) + fz(int(x * 3), int(y * 2), 8) * 24
        g = None
        if abs(x) < 0.5 and 4 < y < L - 6:
            c = np.array((190, 255, 120), np.float32)
            g = col((170, 255, 110), 0.7)
        if int(y) % 7 == 3 and abs(x) > 0.7:
            c = np.array((40, 120, 40), np.float32)                              # a leaf
        if e < 0.7:
            c = np.array((220, 250, 190), np.float32)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.3, 0.7, paint).build(m, a)
    gold = (206, 162, 60)
    box(m, a, -6.5, -1.6, -0.6, 6.5, -0.4, 0.6, metal_paint(gold))
    ring = disc_mask(5.2, inner=3.8)
    plate(m, a, ring, -5.2, -5.2, 0.5, lambda i, j, f: (col(shade((80, 150, 60), 1.2 if (i + j) % 3 == 0 else 0.9)), None), "xz", -0.8)
    box(m, a, -5.8, -13.5, -0.5, -4.4, -0.8, 0.5, metal_paint(gold))
    box(m, a, -1.1, -13, -1.1, 1.1, -1.4, 1.1, wrap_paint((50, 100, 40), (206, 162, 60), diamonds=False))
    box(m, a, -1.8, -15.6, -1.8, 1.8, -13, 1.8, metal_paint(gold, gem=(120, 255, 100), glow_rgb=(150, 255, 120)))
    return dict(glow=("96FF78", 0.1, 0.4, 1.0))


def permafrost_sabre(m, a):
    """Permafrost Sabre: a curved icy sabre, pale blue with frosted grain, a gold basket hilt set with a frost gem."""
    L = 52
    st = katana_stations(L, 4.0, 3.0, 3.5, kissaki=7.0, steps=9, spine_frac=0.7)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((200, 235, 255)), col((170, 230, 255), 0.35)
        c = np.array((150, 205, 240), np.float32) + fz(int(x * 3), int(y * 3), 12) * 34
        g = col((150, 225, 255), 0.18)
        if (int(x * 2) + int(y)) % 11 == 0:
            c = np.array((240, 252, 255), np.float32)                          # frost crystals
            g = col((220, 250, 255), 0.8)
        if e < 0.9:
            c = np.array((240, 252, 255), np.float32)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.55, 1.0, paint).build(m, a)
    gold = (212, 170, 64)
    box(m, a, -5, -1.4, -0.9, 5, 0.6, 0.9, metal_paint(gold, gem=(150, 235, 255), glow_rgb=(170, 240, 255)))
    ring = disc_mask(6.0, inner=4.6)
    plate(m, a, ring[:7], -6.0, -12, 0.5, lambda i, j, f: (col(shade(gold, 1.15)), None), "xy")
    box(m, a, -1.2, -13, -1.2, 1.2, -1.4, 1.2, wrap_paint((30, 60, 110), (212, 170, 64), diamonds=True))
    box(m, a, -1.9, -15.4, -1.9, 1.9, -13, 1.9, metal_paint(gold))
    return dict(glow=("AEEBFF", 0.09, 0.4, 0.95))


def cyclone_rapier(m, a):
    """Cyclone Rapier: a silver rapier etched with a spiral gale, a swirling gold guard and a teal wind-gem."""
    L = 56
    st = straight_stations(L, 1.7, 1.1, 6, steps=3, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((200, 215, 225)), None
        c = np.array((184, 202, 216), np.float32) + fz(int(x * 3), int(y * 2), 14) * 22
        g = None
        sp = math.sin(y * 0.55 + x * 1.7)
        if sp > 0.75 and 4 < y < L - 6:
            c = np.array((90, 220, 210), np.float32)                             # the gale spiral
            g = col((120, 245, 235), 0.8)
        if e < 0.7:
            c = np.array((236, 244, 250), np.float32)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.3, 0.7, paint).build(m, a)
    gold = (210, 168, 62)
    swirl = ascii_mask([
        ".####.",
        "##..##",
        "#..#.#",
        "#.##.#",
        "##....",
        ".####.",
    ])
    plate(m, a, swirl, -9.5, -3, 0.5, lambda i, j, f: (col(shade(gold, 1.2 if (i + j) % 3 == 0 else 0.92)), None), "xy")
    plate(m, a, swirl[:, ::-1], 3.5, -3, 0.5, lambda i, j, f: (col(shade(gold, 1.2 if (i + j) % 3 == 0 else 0.92)), None), "xy")
    box(m, a, -3.2, -1.4, -0.7, 3.2, -0.4, 0.7, metal_paint(gold))
    box(m, a, -1.1, -13, -1.1, 1.1, -1.4, 1.1, wrap_paint((40, 70, 80), (210, 168, 62), diamonds=False))
    box(m, a, -1.9, -15.6, -1.9, 1.9, -13, 1.9, metal_paint(gold, gem=(90, 235, 220), glow_rgb=(120, 250, 235)))
    return dict(glow=("78F5E8", 0.11, 0.4, 1.0))


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


# ================================================================ the two crossbows of "Magic Item Compendium I" (Bolt Thrower, Crossbow of Swift Death)
class _XbAtlas(Atlas):
    """An Atlas that packs first-fit into free space (the shelf packer wastes the area beside tall parts): the crossbows have many parts."""

    def __init__(self):
        super().__init__()
        self.occ = np.zeros((TS, TS), bool)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        for y in range(0, TS - h + 1):
            blocked = self.occ[y:y + h].any(axis=0)
            run = 0
            for x in range(TS):
                run = 0 if blocked[x] else run + 1
                if run >= w:
                    x0 = x - w + 1
                    self.occ[y:y + h + 1, x0:x0 + w + 1] = True
                    return x0, y
        raise RuntimeError("atlas full")


def _xb_packed(fn):
    """Builds a weapon on a packed atlas, then copies the skin and glow into the one the generator hands over."""
    def build(m, a):
        t = _XbAtlas()
        extra = fn(m, t)
        a.img[:] = t.img
        a.glow[:] = t.glow
        return extra
    build.__doc__ = fn.__doc__
    return build


def _xb_stroke_mask(w, h, pts, hws, ss=4):
    """Pixel mask (rows top first, y up from the bottom row) of a tapered stroke through pts [(x, y)] in mask pixels, half widths hws."""
    ys, xs = np.mgrid[0:h * ss, 0:w * ss]
    X = (xs + 0.5) / ss
    Y = h - (ys + 0.5) / ss
    inside = np.zeros(X.shape, bool)
    for (x0, y0), (x1, y1), w0, w1 in zip(pts, pts[1:], hws, hws[1:]):
        dx, dy = x1 - x0, y1 - y0
        t = np.clip(((X - x0) * dx + (Y - y0) * dy) / max(dx * dx + dy * dy, 1e-9), 0, 1)
        d = np.hypot(X - (x0 + t * dx), Y - (y0 + t * dy))
        inside |= d <= (w0 + (w1 - w0) * t)
    return inside.reshape(h, ss, w, ss).mean(axis=(1, 3)) > 0.5


def _xb_edge_flags(m):
    """Per pixel: is the pixel above empty (a leading edge), below empty (a trailing edge)."""
    p = np.pad(m, 1, constant_values=False)
    return m & ~p[:-2, 1:-1], m & ~p[2:, 1:-1]


def _xb_prod(span, back, root_hw, tip_hw, hook=1.5, bow=1.7):
    """The flat shape of a crossbow prod: two limbs from the middle out to +-span, tips swept back by 'back' and flicked up by 'hook'.
    Returns (mask, x offset of the mask's left edge, y of the root centre inside the mask, centre-line function of |x|)."""
    w = int(2 * (span + hook + 2))
    yroot = back + root_hw + hook + 2
    h = int(math.ceil(yroot + root_hw + 2))
    cx = w / 2
    pts, hws = [], []
    n = 16
    for k in range(-n, n + 1):
        u = abs(k) / n
        y = yroot - back * u ** bow + (hook * max(0.0, (u - 0.8) / 0.2) ** 2 if u > 0.8 else 0)
        pts.append((cx + math.copysign(u * span, k), y))
        hws.append(root_hw + (tip_hw - root_hw) * u ** 0.8)
    m = _xb_stroke_mask(w, h, pts, hws)

    def centre(ax):
        u = min(1.0, abs(ax) / span)
        return yroot - back * u ** bow + (hook * max(0.0, (u - 0.8) / 0.2) ** 2 if u > 0.8 else 0)
    return m, -cx, yroot, centre


def _xb_core(m, a, S):
    """The parts both crossbows share: stock, prod, string, bolt on its rail, grip and trigger. S holds the style numbers and painters."""
    st = S["stations"]
    Blade(st, S["te"], S["tr"], S["stock_paint"]).build(m, a)
    # ---- the prod
    mask, x0, yr, centre = _xb_prod(S["span"], S["back"], S["root_hw"], S["tip_hw"], S["hook"])
    h, w = mask.shape
    lead, trail = _xb_edge_flags(mask)
    yroot = S["prod_y"]

    def prod_paint(i, j, front):
        ii = i if front else w - 1 - i                    # the back face is mirrored; the shape is symmetric
        x = ii + 0.5 + x0
        return S["prod_paint"](x, h - j - 0.5, ii, j, bool(lead[j, ii]), bool(trail[j, ii]), centre(x), front)
    plate(m, a, mask, x0, yroot - yr, S["prod_t"], prod_paint, "xy", 0)
    # ---- the string: from tip to tip through the latch, a hair above the stock
    tip_x = S["span"] - 0.6
    tip_y = yroot - yr + centre(tip_x) + 0.0
    pts = [(-tip_x, tip_y), (0, S["latch_y"]), (tip_x, tip_y)]
    sw, sh = int(2 * tip_x + 4), int(abs(tip_y - S["latch_y"]) + 4)
    sy0 = min(tip_y, S["latch_y"]) - 1.5
    spts = [(sw / 2 + p[0], p[1] - sy0) for p in pts]
    smask = _xb_stroke_mask(sw, sh, spts, [0.5, 0.5, 0.5], ss=6)
    plate(m, a, smask, -sw / 2, sy0, 0.22, S["string_paint"], "xy", S["string_z"])


def _xb_arc_plate(m, a, rows, x0, y0, z, color, glow, plane="xy", t=0.16, flip=False):
    mask = ascii_mask(rows)
    if flip:
        mask = mask[:, ::-1]
    plate(m, a, mask, x0, y0, t, lambda i, j, f: (col(color), col(glow, 0.95)), plane, z)


@_xb_packed
def bolt_thrower(m, a):
    """Bolt Thrower: a dark blued-steel crossbow with brass trim after the reference art: a long tapered stock with a glowing bolt channel
    and a loaded bolt of crackling force, a skull prow, limbs swept back, a cyan orb at the butt and lightning arcing over the frame."""
    cy, wh = (110, 232, 255), (236, 252, 255)
    st = [(-19, -2.1, 0, 2.1), (-15, -2.6, 0, 2.6), (-9, -3.0, 0, 3.0), (-1, -3.2, 0, 3.2), (6, -3.7, 0, 3.7), (14, -3.4, 0, 3.4),
          (20, -4.2, 0, 4.2), (26, -4.7, 0, 4.7), (30, -3.8, 0, 3.8)]

    def stock_paint(x, y, front, e, rd):
        if x is None:
            return col(shade((30, 38, 54), 1.15 if int(y) % 6 == 0 else 1.0)), None
        c = np.array((32, 40, 58), np.float32) + nz(x, y, 2) * 20 + fz(x, y, 1) * 10
        g = None
        if e < 0.9:
            c = np.array((212, 172, 70), np.float32) + fz(x, y, 6) * 22            # the brass edge line
        elif e < 1.7:
            c = c * 0.62
        elif int(y) % 9 == 0:
            c = c * 0.7                                                              # panel seams
        if front:
            if 8 <= y <= 31 and rd < 1.0:                                            # the bolt channel
                c = np.array((6, 16, 28), np.float32)
                if rd < 0.5:
                    c = np.array((70, 190, 230), np.float32)
                    g = col(cy, 0.85)
            elif rd < 0.55 and -14 <= y < 8 and int(y) % 4 in (0, 1):                # a glowing seam down the tiller
                c = np.array((60, 170, 215), np.float32)
                g = col(cy, 0.6)
            elif 1.5 < rd < 2.6 and e > 1.9 and hsh(int(y / 2), 5) > 0.55 and y > -12:   # etched runes
                c = np.array((90, 200, 240), np.float32)
                g = col(cy, 0.8)
            elif e < 2.6 and e >= 1.7 and int(y) % 7 == 3 and int(x * 2) % 2 == 0:  # rivets
                c = np.array((226, 184, 78), np.float32)
        return col(np.clip(c, 0, 255)), g
    brass, dbrass = (206, 166, 66), (146, 108, 38)

    def prod_paint(x, y, i, j, lead, trail, yc, front):
        c = np.array((30, 38, 56), np.float32) + nz(x, y, 4) * 22 + fz(x, y, 3) * 10
        g = None
        if lead:
            c = np.array((214, 174, 72), np.float32) + fz(x, y, 6) * 20            # brass along the leading edge
        elif trail:
            c = c * 0.55
        if abs(y - yc) < 0.55 and abs(x) > 4 and (int(abs(x) * 1.3) % 3 != 2):       # a charged vein down each limb
            c = np.array((120, 225, 250), np.float32)
            g = col(cy, 0.95)
        if abs(x) < 4.2:
            c = np.array((196, 156, 62), np.float32) * (0.85 + 0.2 * (j % 2)) + fz(x, y, 8) * 14   # the saddle
        return col(np.clip(c, 0, 255)), g

    def string_paint(i, j, f):
        return col((200, 244, 255)), col(cy, 1.0)

    S = dict(stations=st, te=1.0, tr=2.0, stock_paint=stock_paint, span=19.5, back=6.0, root_hw=3.2, tip_hw=1.5, hook=1.2,
             prod_y=22.5, prod_t=0.95, prod_paint=prod_paint, latch_y=8.5, string_paint=string_paint, string_z=2.45)
    _xb_core(m, a, S)
    # ---- the loaded bolt: a bar of caged force on the rail, a stepped head, small vanes
    def energy(face, i, j, fw, fh):
        return col(wh, 1.0), col(cy, 1.0)
    box(m, a, -0.6, 9, 2.0, 0.6, 31, 3.2, energy)
    for (lo, hi, hw) in ((31, 32.6, 1.15), (32.6, 34.0, 0.8), (34.0, 35.0, 0.4)):
        box(m, a, -hw, lo, 2.0, hw, hi, 3.2, energy)
    for sx in (-1, 1):
        box(m, a, sx * 0.6 if sx > 0 else -2.1, 9.2, 2.4, 2.1 if sx > 0 else -0.6, 12.4, 2.8, energy)     # the vanes
    for sx in (-1, 1):                                                                        # brass rail guides either side of the channel
        lo, hi = sorted((sx * 1.0, sx * 1.8))
        box(m, a, lo, 8, 2.0, hi, 30, 2.9, metal_paint(brass, rivets=True))
    # ---- brass bands round the stock
    for (y, hw) in ((-5.5, 3.6), (15.5, 3.9), (26.5, 5.1)):
        box(m, a, -hw, y - 0.9, -2.5, hw, y + 0.9, 2.5, metal_paint(brass))
    # ---- the grip and trigger below the stock
    box(m, a, -1.6, -13.5, -8.4, 1.6, -5.5, -1.6, wrap_paint((26, 32, 46), (200, 160, 62), diamonds=True))
    box(m, a, -1.9, -14.6, -8.8, 1.9, -13.5, -1.2, metal_paint(brass))
    box(m, a, -0.5, -2.6, -4.4, 0.5, -1.2, -1.6, metal_paint(dbrass))                       # the trigger
    box(m, a, -0.6, 2.4, -5.0, 0.6, 3.4, -1.6, metal_paint(brass))                          # the trigger guard: front post, floor
    box(m, a, -0.6, -4.2, -5.0, 0.6, 3.4, -4.0, metal_paint(brass))
    box(m, a, -0.6, -4.2, -5.0, 0.6, -3.2, -1.6, metal_paint(brass))
    box(m, a, -0.7, -3, -3.8, 0.7, 19, -2.0, metal_paint((34, 42, 62)))                     # the keel under the tiller
    # ---- the cyan orb at the butt, held in a brass collar
    box(m, a, -2.8, -20.6, -2.8, 2.8, -19, 2.8, metal_paint(brass))
    box(m, a, -2.3, -21.6, -2.3, 2.3, -20.6, 2.3, metal_paint(dbrass))

    def orb(face, i, j, fw, fh):
        k = 1.3 if (i + j) % 3 == 0 else 1.0
        return col(np.array(cy) * 0.55 * k + 70), col(cy, 1.0)
    box(m, a, -2.6, -25.6, -2.6, 2.6, -21.6, 2.6, orb)
    # ---- the skull prow at the muzzle: a horned plate with eye sockets and fangs
    skull = ascii_mask([
        "#.......#",
        "##.....##",
        ".###.###.",
        ".#######.",
        "##.###.##",
        "#########",
        "#.#.#.#.#",
        "..#...#..",
        "..#.#.#..",
    ])

    def skull_paint(i, j, f):
        c = np.array((40, 50, 72), np.float32) + fz(i, j, 11) * 16
        g = None
        if j in (4,) and i in (2, 6):
            return col((20, 230, 255)), col(cy, 1.0)                                          # eyes
        if j >= 6:
            c = np.array((226, 220, 200), np.float32)                                         # fangs
        if i in (0, 8) and j < 2:
            c = np.array((214, 174, 72), np.float32)                                          # gilded horn tips
        return col(c), g
    plate(m, a, skull, -4.5, 30.5, 1.2, skull_paint, "xy", 0)
    # ---- lightning: jagged arcs of force crawling over the frame
    arcs = [
        (["..#......", ".#.......", "#..#.....", "...#.#...", "....#.#..", ".....#.#.", "......#.#"], -9.5, 26.0, 3.4),
        (["#......", ".#.....", "..#.#..", "...#.#.", ".....#."], -8.5, 4.0, 3.4),
        (["..#.....", ".#.#....", "#...#...", "....#.#.", ".....#.#"], 2.0, -8.0, 3.4),
        (["#.....", ".#.#..", "..#.#.", "....#.", "...#.."], 4.0, 15.0, 3.0),
    ]
    for rows, ax, ay, az in arcs:
        _xb_arc_plate(m, a, rows, ax, ay, az, wh, cy)
    _xb_arc_plate(m, a, ["#.....", ".#.#..", "..#.#.", "....#.", "...#.."], -22.0, 19.0, 0.0, wh, cy, "xz")
    return dict(glow=("78E6FF", 0.14, 0.45, 1.0))



def _xb_jaw_mask(R, thick, teeth, tooth_h, span_deg, ss=4):
    """A toothed horseshoe, open to the back: the stirrup jaw at the front of the Crossbow of Swift Death. Returns (mask, x offset, centre y inside the mask)."""
    w = int(2 * (R + tooth_h) + 3)
    h = int(R + tooth_h + 3)
    cx, cy = w / 2, 1.5
    ys, xs = np.mgrid[0:h * ss, 0:w * ss]
    X = (xs + 0.5) / ss - cx
    Y = h - (ys + 0.5) / ss - cy
    r = np.hypot(X, Y)
    ang = np.degrees(np.arctan2(X, Y))
    ph = (ang / span_deg * teeth / 2 + 0.5 * teeth) % 1.0
    tri = 1 - np.abs(ph - 0.5) * 2
    inside = (np.abs(ang) <= span_deg) & (r >= R - thick) & (r <= R + tooth_h * tri ** 0.8 * (np.abs(ang) < span_deg - 4) + 0.3)
    return inside.reshape(h, ss, w, ss).mean(axis=(1, 3)) > 0.45, -cx, cy


@_xb_packed
def crossbow_of_swift_death(m, a):
    """Crossbow of Swift Death: a dark crossbow after the reference art: a long blackened-wood tiller notched above the trigger (each notch a dull red slit),
    a wide dark prod with red ferrules, a toothed jaw at the muzzle, two crank handles at the rear, an arrowhead butt, a black bolt with a barbed crimson head."""
    red, redl = (255, 44, 70), (255, 120, 130)
    iron, irond = (52, 54, 68), (30, 31, 42)
    st = [(-21, -2.6, 0, 2.6), (-14, -3.0, 0, 3.0), (-6, -3.3, 0, 3.3), (2, -3.6, 0, 3.6), (12, -3.6, 0, 3.6), (20, -4.1, 0, 4.1),
          (27, -4.5, 0, 4.5), (31, -3.6, 0, 3.6), (34, -2.4, 0, 2.4)]
    notch_y = [-3.2 + n * 1.85 for n in range(10)]

    def stock_paint(x, y, front, e, rd):
        if x is None:
            return col(shade((46, 32, 30), 1.2 if int(y) % 5 == 0 else 1.0)), None
        grain = math.sin(x * 2.4 + nz(x * 0.5, y * 0.07, 3) * 7)
        c = np.array((66, 44, 38), np.float32) * (0.82 + 0.2 * grain) + nz(x, y, 5) * 14 + fz(x, y, 2) * 10
        g = None
        if e < 0.9:
            c = np.array((84, 84, 104), np.float32) + fz(x, y, 9) * 18                       # the iron edge strip
        elif e < 1.7:
            c = c * 0.66
        if front:
            if rd < 0.5 and 9 <= y <= 33:                                                     # the bolt groove
                c = np.array((12, 10, 14), np.float32)
            for n, yn in enumerate(notch_y):                                                  # the notches above the trigger
                if abs(y - yn) < 0.5 and 1.0 < abs(x) < 2.9 and e > 1.2:
                    c = np.array((10, 4, 6), np.float32)
                    g = col(red, 0.45 + 0.5 * hsh(n, 3))
                elif abs(y - yn) < 1.1 and 1.0 < abs(x) < 2.9 and e > 1.2 and abs(y - yn) >= 0.5:
                    c = c * 0.55
            if y > 15 and e < 2.4 and int(y) % 6 == 1 and e >= 1.7:
                c = np.array((150, 150, 172), np.float32)                                     # rivets
        return col(np.clip(c, 0, 255)), g

    def prod_paint(x, y, i, j, lead, trail, yc, front):
        c = np.array((38, 40, 54), np.float32) + nz(x, y, 6) * 20 + fz(x, y, 4) * 10
        g = None
        if lead:
            c = np.array((104, 106, 130), np.float32) + fz(x, y, 7) * 20
        elif trail:
            c = c * 0.55
        if abs(y - yc) < 0.5 and 5 < abs(x) < 20 and int(abs(x) * 1.1) % 3 != 1:
            c = np.array((190, 30, 52), np.float32)                                           # a blood-red inlay down each limb
            g = col(red, 0.55)
        if abs(x) > 21.2:
            c = np.array((210, 30, 56), np.float32)                                           # red ferrules at the tips
            g = col(red, 0.9)
        if abs(x) < 5.0:
            c = np.array((58, 60, 78), np.float32) * (0.9 + 0.2 * (j % 2)) + fz(x, y, 8) * 14
        return col(np.clip(c, 0, 255)), g

    def string_paint(i, j, f):
        return col((224, 214, 188)), col(red, 0.3)

    S = dict(stations=st, te=1.4, tr=2.3, stock_paint=stock_paint, span=22.5, back=5.5, root_hw=2.9, tip_hw=1.3, hook=0.8,
             prod_y=21.5, prod_t=1.0, prod_paint=prod_paint, latch_y=8.0, string_paint=string_paint, string_z=2.75)
    _xb_core(m, a, S)
    # ---- the toothed jaw: a horseshoe of fangs round the front of the stock
    jm, jx, jc = _xb_jaw_mask(8.8, 2.3, 9, 2.0, 66)
    jh, jw = jm.shape

    def jaw_paint(i, j, f):
        ii = i if f else jw - 1 - i
        X, Y = ii + 0.5 + jx, jh - j - 0.5 - jc
        r = math.hypot(X, Y)
        if r > 8.8 + 0.3:
            return col((228, 218, 194)), None             # the fangs
        c = np.array((50, 52, 68), np.float32) + fz(i, j, 5) * 18
        if r < 6.8:
            c = c * 0.7
        return col(c), None
    plate(m, a, jm, jx, 25.0 - jc, 1.15, jaw_paint, "xy", 0)
    # ---- the bolt: black iron, white fletching, a barbed crimson head
    def shaft(face, i, j, fw, fh):
        return col((30, 28, 36) + fz(i, j, 3) * 10), (col(red, 0.35) if j % 3 == 0 else None)
    box(m, a, -0.6, 9, 2.3, 0.6, 33.5, 3.5, shaft)
    for sx in (-1, 1):
        lo, hi = sorted((sx * 0.6, sx * 2.3))
        box(m, a, lo, 9.2, 2.5, hi, 13.4, 2.9, flat((232, 228, 214), 8))
    head = ascii_mask(["..#..", ".###.", ".###.", "#####", "#.#.#"])
    plate(m, a, head, -2.5, 33.5, 0.5, lambda i, j, f: (col((190, 24, 48)), col(red, 1.0)), "xy", 2.9)
    # ---- iron bands round the stock
    for (y, hw) in ((-10.5, 3.7), (6.5, 4.0), (17.5, 4.4), (29.0, 4.9)):
        box(m, a, -hw, y - 0.9, -2.9, hw, y + 0.9, 2.9, metal_paint(iron, rivets=True))
    # ---- the latch, a blood-red stone set in iron
    box(m, a, -1.9, 6.4, 2.2, 1.9, 9.6, 3.4, metal_paint(iron, gem=(210, 30, 56), glow_rgb=red))
    # ---- grip, trigger and guard
    box(m, a, -1.6, -13.0, -8.6, 1.6, -5.0, -2.0, wrap_paint((30, 24, 28), (130, 26, 40), diamonds=True))
    box(m, a, -1.9, -14.0, -9.0, 1.9, -13.0, -1.6, metal_paint(iron))
    box(m, a, -0.5, -2.4, -4.6, 0.5, -1.0, -2.0, metal_paint(irond))
    box(m, a, -0.6, 2.2, -5.2, 0.6, 3.2, -2.0, metal_paint(iron))
    box(m, a, -0.6, -4.0, -5.2, 0.6, 3.2, -4.0, metal_paint(iron))
    box(m, a, -0.6, -4.0, -5.2, 0.6, -3.0, -2.0, metal_paint(iron))
    # ---- the crank handles at the rear: iron arms out to ridged grips
    box(m, a, -4.2, -19.4, -3.0, 4.2, -13.6, 3.0, metal_paint(irond, rivets=True))
    for sx in (-1, 1):
        lo, hi = sorted((sx * 3.8, sx * 10.2))
        box(m, a, lo, -17.0, -0.6, hi, -16.0, 0.6, metal_paint(iron))
        lo, hi = sorted((sx * 10.2, sx * 12.8))
        box(m, a, lo, -21.2, -1.4, hi, -12.4, 1.4, wrap_paint((26, 22, 26), (150, 30, 44), diamonds=False))
        lo, hi = sorted((sx * 12.8, sx * 13.8))
        box(m, a, lo, -19.6, -0.9, hi, -14.0, 0.9, metal_paint(iron))
    # ---- the arrowhead butt: a steel spike plate with a red gem
    spike = ascii_mask([".#######.", ".#######.", "..#####..", "..#####..", "...###...", "....#...."])
    plate(m, a, spike, -4.5, -28.5, 1.3, lambda i, j, f: (col(shade((86, 88, 108), 1.25 if (i + j) % 3 == 0 else 1.0)) if j else col((52, 54, 68)),
                                                      col(red, 0.9) if (j == 1 and i in (4,)) else None), "xy", 0)
    box(m, a, -2.8, -22.0, -2.4, 2.8, -20.0, 2.4, metal_paint(iron, rivets=True))
    return dict(glow=("FF3A54", 0.1, 0.35, 1.0))


SPECS += [("bolt_thrower", bolt_thrower), ("crossbow_of_swift_death", crossbow_of_swift_death)]


# ================================================================ the Club of Rot and the Doom Hammer (reference sheet "Magic Item Compendium I")
def _lathe_dir(direction):
    """A rotation matrix taking +y to 'direction'."""
    d = np.array(direction, np.float64)
    d = d / np.linalg.norm(d)
    y = np.array([0.0, 1.0, 0.0])
    c = float(np.dot(y, d))
    if c > 0.99999:
        return np.eye(3)
    if c < -0.99999:
        return np.diag([1.0, -1.0, -1.0])
    v = np.cross(y, d)
    s = np.linalg.norm(v)
    K = np.array([[0, -v[2], v[1]], [v[2], 0, -v[0]], [-v[1], v[0], 0]])
    return np.eye(3) + K + K @ K * ((1 - c) / (s * s))


def lathe(mesh, atlas, stations, sides, paint, base=(0.0, 0.0, 0.0), direction=(0.0, 1.0, 0.0), sw=3, jitter=0.0, salt=0, caps=(True, True)):
    """A surface of revolution (a round club, a haft, a thorn, a mushroom): rings of 'sides' points at each station (y, r[, ox, oz]) along +y,
    turned so that +y points along 'direction' and moved to 'base'. paint(k, s, y, theta) -> (rgba, glow) for side k at fraction s round it
    (its own skin strip, sw pixels per side). jitter roughens the rings (a gnarled bone); a zero radius closes the end to a point."""
    st = [tuple(s) + (0.0,) * (4 - len(s)) for s in stations]
    y0, y1 = st[0][0], st[-1][0]
    H = int(math.ceil(y1 - y0)) + 1
    W = sides * sw
    u0, v0 = atlas.alloc(W, H)

    def f(lx, ly):
        k = min(sides - 1, int(lx // sw))
        s = (lx - k * sw) / sw
        return paint(k, s, y1 - ly, (k + s) / sides * 2 * math.pi)
    atlas.paint(u0, v0, W, H, f)
    R = _lathe_dir(direction)
    org = np.array(base, np.float64)

    def ring(i, k):
        y, r, ox, oz = st[i]
        th = 2 * math.pi * (k % sides) / sides
        rr = r * (1 + jitter * (hsh(k % sides, int(y * 2), salt) - 0.5) * 2) if r > 0 else 0.0
        p = np.array([ox + rr * math.cos(th), y, oz + rr * math.sin(th)])
        return tuple(org + R @ p)

    def vtex(y):
        return v0 + (y1 - y)
    for i in range(len(st) - 1):
        for k in range(sides):
            a, b = ring(i, k), ring(i, k + 1)
            c, d = ring(i + 1, k + 1), ring(i + 1, k)
            ua, ub = u0 + k * sw, u0 + (k + 1) * sw
            thm = 2 * math.pi * (k + 0.5) / sides
            out = R @ np.array([math.cos(thm), 0.0, math.sin(thm)])
            mesh.quad(0, [a + (ua, vtex(st[i][0])), b + (ub, vtex(st[i][0])), c + (ub, vtex(st[i + 1][0])), d + (ua, vtex(st[i + 1][0]))], tuple(out))
    for (idx, sgn, on) in ((0, -1, caps[0]), (len(st) - 1, 1, caps[1])):
        if not on or st[idx][1] <= 0.05:
            continue
        cu, cv = u0 + W / 2, vtex(st[idx][0]) + (0.5 if idx == 0 else -0.5)
        ctr = tuple(org + R @ np.array([st[idx][2], st[idx][0], st[idx][3]]))
        for k in range(sides):
            mesh.quad(0, [ctr + (cu, cv), ring(idx, k) + (cu, cv), ring(idx, k + 1) + (cu, cv), ring(idx, k + 1) + (cu, cv)], tuple(R @ np.array([0.0, sgn, 0.0])))


def _quarter_noise(x, y, salt):
    return nz(x * 0.6, y * 0.35, salt)


def club_of_rot_paint():
    """The skin of the club: ivory bone with grain, cracks and pits, rot spreading up the bulb in olive-green and brown patches, ooze veins that glow, grave linen on the grip."""
    def p(k, s, y, th):
        x = (k + s) * 3.0
        grain = fz(x * 1.3, y * 0.25, 3) * 24
        c = np.array((232, 220, 186), np.float32) + nz(x, y, 9) * 64 + grain
        g = None
        if abs(nz(x * 0.45, y * 0.32, 6)) < 0.024:                                  # cracks
            c = np.array((78, 58, 40), np.float32) + fz(x, y, 4) * 14
        elif fz(x * 2.2, y * 2.2, 8) < -0.36:
            c = c * 0.76                                                             # pits
        rot = nz(x * 0.5, y * 0.3, 5) + (y - 18) * 0.0065                          # the rot creeps up towards the head
        if rot > 0.08 and y > 3:
            t = min(1.0, (rot - 0.08) * 6)
            patch = np.array((118, 128, 62), np.float32) + fz(x, y, 16) * 26
            if rot > 0.2:
                patch = np.array((78, 92, 46), np.float32) + fz(x * 2, y * 2, 17) * 18
                if hsh(int(x * 1.5), int(y * 1.5), 21) > 0.9:
                    g = col((170, 255, 90), 0.8)
            c = c * (1 - t) + patch * t
        if fz(x * 3.0, y * 0.12, 22) > 0.33 and y > 4:                              # ooze veins running down
            c = np.array((132, 200, 74), np.float32)
            g = col((150, 255, 80), 0.7)
        if y > 38.2:
            c = np.array((176, 56, 46), np.float32) + fz(x, y, 23) * 24            # the marrow at the head: raw
            g = col((255, 90, 70), 0.55)
        return col(np.clip(c, 0, 255)), g
    return p


def _linen_paint(k, s, y, th):
    """Grave linen wound round the bone in diagonal bands, stained here and there."""
    x = (k + s) * 3.0
    band = int(x * 1.0 + y * 1.4) % 4
    c = np.array((220, 208, 172), np.float32) + fz(x * 2, y * 2, 7) * 18 + nz(x, y, 12) * 30
    if band == 0:
        c = c * 0.66
    elif band == 1:
        c = c * 1.08
    if nz(x * 0.7, y * 0.5, 14) > 0.16:
        c = np.array((142, 128, 82), np.float32) + fz(x, y, 15) * 18                 # grave stains
    return col(np.clip(c, 0, 255)), None


def _growth_paint(cap_rgb=(150, 192, 74), spot=(236, 244, 176)):
    def p(k, s, y, th):
        x = (k + s) * 3.0
        c = np.array(cap_rgb, np.float32) + fz(x * 2, y * 2, 31) * 30
        g = None
        if hsh(int(x), int(y * 1.5), 33) > 0.7:
            c = np.array(spot, np.float32)
            g = col((220, 255, 120), 0.95)
        elif y < 0.6:
            c = np.array((96, 120, 52), np.float32)
        return col(np.clip(c, 0, 255)), g
    return p


def _bone_paint(rgb=(222, 208, 170), tip=(200, 180, 140)):
    def p(k, s, y, th):
        x = (k + s) * 3.0
        c = np.array(rgb, np.float32) + nz(x, y, 41) * 56 + fz(x * 2, y * 2, 42) * 18
        if (k + s) / 3.0 % 1.0 > 0.5:
            c = c * 0.86
        return col(np.clip(c, 0, 255)), None
    return p


def _condyle_paint(k, s, y, th):
    x = (k + s) * 2.0
    t = max(0.0, min(1.0, (y - 0.6) / 3.6))
    c = np.array((232, 220, 186), np.float32) * (1 - t) + np.array((214, 128, 76), np.float32) * t + nz(x, y, 43) * 36 + fz(x * 2, y * 2, 44) * 14
    g = None
    if fz(x * 2.5, y * 0.5, 45) > 0.16 and y > 1.2:
        c = np.array((176, 50, 42), np.float32)
        g = col((255, 96, 64), 0.4)
    return col(np.clip(c, 0, 255)), g


def _ooze_paint(k, s, y, th):
    return col((84, 150, 52)), col((150, 255, 80), 0.7 + 0.25 * (int(y) % 2))


@_xb_packed
def club_of_rot(m, a):
    """Club of Rot (after the reference art): a gnarled bone-white club, a femur's knob for a pommel, grave linen wound round the grip and
    left hanging in tatters, the head a swollen bulb of bone eaten by rot: green-black patches, cracks, bone spurs, fungal growths with
    glowing spots, ooze veins and drips, raw marrow at the crown."""
    # the bone itself: a round, gnarled shaft that swells into the head
    shaft = [(-22.0, 1.6), (-21.0, 3.1), (-19.0, 3.5), (-17.0, 2.7), (-15.0, 1.9), (-2.0, 1.9), (0.0, 2.2), (4.0, 3.0, 0.15, 0.0), (9.0, 3.9, 0.3, 0.1),
             (14.0, 4.9, 0.2, -0.2), (19.0, 6.0, -0.2, -0.1), (24.0, 6.9, -0.3, 0.1), (28.0, 7.3, -0.2, 0.3), (32.0, 7.0, 0.0, 0.2), (35.0, 6.0),
             (37.5, 4.4), (39.5, 2.4), (40.6, 0.0)]
    lathe(m, a, shaft, 8, club_of_rot_paint(), jitter=0.07, salt=3)
    # the wrapped grip: a thicker sleeve of linen, bound at both ends
    wrap = [(-15.2, 2.0), (-14.6, 2.9), (-13.4, 2.65), (-3.6, 2.65), (-2.4, 2.95), (-1.8, 2.1)]
    lathe(m, a, wrap, 8, _linen_paint, jitter=0.05, salt=5, caps=(False, False))
    neck = [(7.6, 3.5), (8.1, 4.55), (9.0, 4.4), (11.2, 4.4), (11.8, 4.55), (12.3, 3.9)]
    lathe(m, a, neck, 8, _linen_paint, jitter=0.04, salt=6, caps=(False, False))
    # the tattered tails of the linen: three strips hanging off the lower binding
    for (x0, z0, hgt, off) in ((2.5, 0.0, 12.0, 0.0), (-2.7, 1.0, 9.0, 0.3), (0.4, -2.8, 10.0, -0.2)):
        def tail(face, i, j, fw, fh, hgt=hgt):
            c = np.array((190, 176, 140), np.float32) + fz(i * 2, j * 2, 51) * 26
            if j > fh - 3 or (i + j) % 7 == 0:
                c = c * 0.6
            return col(np.clip(c, 0, 255)), None
        box(m, a, x0 - 0.9, -14.0 - hgt, z0 - 0.25, x0 + 0.9, -14.0 + off, z0 + 0.25, tail)
    # the knob at the butt: two condyles like the end of a femur
    for sx in (-1, 1):
        lathe(m, a, [(0.0, 1.5), (1.4, 1.9), (2.6, 1.4), (3.4, 0.0)], 6, _bone_paint(), base=(sx * 2.0, -21.2, 0.0), direction=(sx * 0.5, -1.0, 0.0), sw=2)
    # the head: two condyles at the crown, tilted out
    for sx in (-1, 1):
        lathe(m, a, [(0.0, 2.7), (1.8, 3.0), (3.4, 2.2), (4.6, 0.0)], 8, _condyle_paint, base=(sx * 3.2, 36.2, 0.2), direction=(sx * 0.42, 1.0, 0.0), sw=2, jitter=0.08, salt=9)
    # bone spurs and thorns growing out of the rot
    spurs = [(13, 0.1, 4.7, 3.2), (17, 2.6, 5.4, 3.6), (20, 4.2, 6.0, 4.2), (23, 5.4, 6.6, 3.4), (25, 0.7, 6.9, 3.8), (27, 3.3, 7.2, 4.4),
             (29, 5.0, 7.3, 3.6), (31, 1.8, 7.1, 4.0)]
    for (y, ang, r, ln) in spurs:
        d = (math.cos(ang), 0.55, math.sin(ang))
        base = (math.cos(ang) * (r - 0.9), y, math.sin(ang) * (r - 0.9))
        lathe(m, a, [(0.0, 1.25), (ln * 0.5, 0.8), (ln, 0.0)], 4, _bone_paint((236, 226, 196)), base=base, direction=d, sw=2)
    # fungal growths: stalks with pale-green caps, spots that glow
    growths = [(8, 0.9, 3.6, 0.8), (12, 3.9, 4.4, 0.9), (16, 5.6, 5.3, 1.0), (19, 1.9, 6.0, 1.1), (22, 4.7, 6.5, 0.9), (26, 2.4, 7.0, 1.2),
               (28, 5.8, 7.2, 1.0), (33, 0.4, 6.6, 0.9)]
    for (y, ang, r, sc) in growths:
        d = (math.cos(ang), 0.3, math.sin(ang))
        base = (math.cos(ang) * (r - 0.7), y, math.sin(ang) * (r - 0.7))
        lathe(m, a, [(0.0, 0.8 * sc), (1.3 * sc, 0.9 * sc), (2.1 * sc, 1.8 * sc), (3.1 * sc, 1.6 * sc), (4.0 * sc, 0.0)], 5, _growth_paint(), base=base, direction=d, sw=2)
    # ooze dripping off the growths
    for (y, ang, r, ln) in ((14.0, 1.2, 4.9, 7.0), (21.0, 5.1, 6.3, 9.0), (27.0, 0.2, 7.2, 8.0), (18.0, 3.3, 5.6, 6.0), (24.0, 2.3, 6.8, 7.0)):
        base = (math.cos(ang) * (r + 0.4), y, math.sin(ang) * (r + 0.4))
        lathe(m, a, [(0.0, 0.5), (ln * 0.6, 0.38), (ln * 0.86, 0.85), (ln, 0.0)], 4, _ooze_paint, base=base, direction=(0.0, -1.0, 0.0), sw=2)
    return dict(glow=("96FF50", 0.1, 0.4, 1.0))


EMBER_RGB = (255, 98, 40)


def _iron_paint(base=(34, 34, 46), rivets=False, edge=1.35):
    """Black iron: grain, pitting, a lit edge on every face, optional rivets."""
    def p(face, i, j, fw, fh):
        c = np.array(base, np.float32) * (1.12 if face == 0 else 0.84 if face == 1 else 1.0) + nz(i * 1.4, j * 1.4, face + 60) * 26 + fz(i * 2.1, j * 1.7, face + 61) * 12
        if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
            c = c * edge + 14
        if rivets and fw >= 4 and fh >= 4 and i % 4 == 1 and j % 4 == 1 and 0 < i < fw - 1 and 0 < j < fh - 1:
            c = c * 1.7 + 24
        return col(np.clip(c, 0, 255)), None
    return p


def _seg_dist(px, py, ax, ay, bx, by):
    vx, vy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((px - ax) * vx + (py - ay) * vy) / max(1e-9, vx * vx + vy * vy)))
    return math.hypot(px - (ax + t * vx), py - (ay + t * vy))


def _doom_rune(i, j, n=13):
    """The doom rune on a plate n pixels across: a broken ring round an algiz (a plumb line with two raised arms). True where the rune is cut (and lit)."""
    c = (n - 1) / 2.0
    dx, dy = i - c, j - c
    r = math.hypot(dx, dy)
    ang = math.atan2(dy, dx)
    if c - 1.35 < r < c + 0.45 and not (abs(math.sin(ang * 2 + 0.7)) < 0.16):           # the ring, broken in four places
        return True
    k = c * 0.78
    if _seg_dist(dx, dy, 0, -k * 0.9, 0, k) < 0.78:                                        # the stem
        return True
    if _seg_dist(dx, dy, 0, -k * 0.05, -k * 0.62, -k) < 0.78 or _seg_dist(dx, dy, 0, -k * 0.05, k * 0.62, -k) < 0.78:   # the arms
        return True
    return False


def _haft_paint(k, s, y, th):
    x = (k + s) * 2.0
    grain = math.sin(x * 2.0 + nz(x * 0.3, y * 0.05, 71) * 9)
    c = np.array((52, 42, 44), np.float32) * (0.8 + 0.22 * grain) + nz(x, y, 72) * 22 + fz(x, y, 73) * 10
    g = None
    if -33 < y < -12:                                                                 # the grip: black leather, wound
        band = int(x * 1.0 + y * 1.3) % 4
        c = np.array((30, 26, 30), np.float32) + fz(x * 2, y * 2, 74) * 14
        if band == 0:
            c = c * 0.6
        elif band == 1:
            c = c * 1.4 + 10
        if band == 2 and int(y) % 5 == 0:
            c = np.array((170, 40, 28), np.float32)                                   # red stitching
            g = col(EMBER_RGB, 0.25)
    elif -52 < y < -36 and k % 4 == 1 and 0.2 < s < 0.8 and int(y) % 4 in (0, 1):    # runes down the coffin-wood
        c = np.array((255, 140, 70), np.float32)
        g = col(EMBER_RGB, 0.9)
    elif y > -9:
        c = np.array((44, 44, 58), np.float32) + fz(x, y, 75) * 12
    return col(np.clip(c, 0, 255)), g


def _spike_paint(k, s, y, th):
    c = np.array((46, 46, 60), np.float32) + fz((k + s) * 3, y * 2, 81) * 14
    return col(np.clip(c, 0, 255)), None


@_xb_packed
def doom_hammer(m, a):
    """Doom Hammer (after the reference art, a black-iron war pick): a haft carved from coffin-wood, black leather wound round the grip, runes
    cut down it; a heavy iron head with a hammer face (a lit doom sigil) at the front, a long hooked pick at the back, a spike on top, rune
    discs on both sides, studs and corner spikes, an iron pommel with a spike. The weapon's front is -x: the face leads, the pick trails."""
    # the haft with its pommel and spike
    hs = [(-60.0, 0.0), (-55.5, 1.1), (-52.5, 2.1), (-51.0, 3.3), (-49.6, 3.2), (-48.0, 2.2), (-46.0, 2.0)]
    for (yb, yt) in ((-12.0, -9.8), (-24.0, -22.2), (-35.0, -33.2), (-43.5, -41.7)):
        hs += [(yb - 0.15, 2.0), (yb, 2.7), (yt, 2.7), (yt + 0.15, 2.0)]
    hs += [(-7.0, 2.0), (-5.5, 2.5)]
    hs = sorted(hs, key=lambda t: t[0])
    lathe(m, a, hs, 8, _haft_paint, sw=2, caps=(False, False))
    # the head: a collar between two rings
    box(m, a, -5.9, -7.0, -5.9, 5.9, 14.0, 5.9, _iron_paint((36, 36, 50), rivets=True))
    box(m, a, -6.6, -8.6, -6.6, 6.6, -6.4, 6.6, _iron_paint((26, 26, 36)))
    box(m, a, -6.6, 12.8, -6.6, 6.6, 15.2, 6.6, _iron_paint((26, 26, 36)))
    # the hammer face at the front (-x): a core and a flange; its striking face is a lit doom sigil
    def face_paint(face, i, j, fw, fh):
        c, g = _iron_paint((30, 30, 42), rivets=True)(face, i, j, fw, fh)
        if face == 2 and fw >= 12 and fh >= 12:
            n = min(fw, fh) - 2
            if _doom_rune(i - (fw - n) / 2.0, j - (fh - n) / 2.0, n):
                return col((255, 150, 80)), col(EMBER_RGB, 0.95)
        return c, g
    box(m, a, -17.0, -6.4, -7.4, -5.9, 12.8, 7.4, _iron_paint((32, 32, 44), rivets=True))
    box(m, a, -19.4, -8.2, -8.2, -17.0, 14.6, 8.2, face_paint)
    for (sy, sz) in ((-8.2, -8.2), (-8.2, 8.2), (14.6, -8.2), (14.6, 8.2)):                    # corner spikes on the flange
        zdir = 0.35 if sz > 0 else -0.35
        ydir = -0.35 if sy < 0 else 0.35
        lathe(m, a, [(0.0, 1.9), (1.8, 1.4), (4.2, 0.0)], 6, _spike_paint, base=(-18.4, sy, sz), direction=(-0.9, ydir, zdir), sw=2, caps=(True, False))
    # the pick at the back (+x): a root block, then a long curved beak built as one extruded slab
    box(m, a, 5.9, -3.0, -5.2, 13.5, 12.4, 5.2, _iron_paint((32, 32, 44), rivets=True))
    pts = [(0, 14.5), (9, 15.2), (19, 14.2), (28, 11.4), (35.5, 8.0)]
    pts = [(x, y - 3.5) for (x, y) in pts]
    hws = [6.6, 5.6, 4.2, 2.6, 0.5]
    pm = _xb_stroke_mask(38, 22, pts, hws)
    ph, pw = pm.shape
    lead, trail = _xb_edge_flags(pm)
    spine = [11.0, 11.7, 10.7, 7.9, 4.5]
    sx = [0, 9, 19, 28, 35.5]

    def beak_paint(i, j, front):
        ii = i if front else pw - 1 - i
        c = np.array((34, 34, 48), np.float32) + nz(i * 1.2, j * 1.2, 90) * 24 + fz(i * 2, j * 2, 91) * 10
        g = None
        if lead[j, ii]:
            c = np.array((124, 128, 152), np.float32) + fz(i, j, 92) * 18
        elif trail[j, ii]:
            c = c * 0.55
        if ii > 7 and abs((ph - j - 0.5) - np.interp(ii + 0.5, sx, spine)) < 0.65 and ii % 4 != 0:
            c = np.array((255, 140, 70), np.float32)
            g = col(EMBER_RGB, 0.85)
        return col(np.clip(c, 0, 255)), g
    plate(m, a, pm, 13.0, 4.2 - 11.0, 2.4, beak_paint, "xy", 0)
    # the spike on top
    sp = [(15.2, -3.1, 0.0, 3.1), (19.0, -2.3, 0.0, 2.3), (36.0, 0.0, 0.0, 0.0)]

    def spike_blade(x, y, front, e, rd):
        if x is None:
            return col((50, 50, 66)), col(EMBER_RGB, 0.5)
        c = np.array((38, 38, 52), np.float32) + nz(x, y, 95) * 20
        g = None
        if e < 0.9:
            c = np.array((118, 122, 148), np.float32)
        if y > 31:
            c = np.array((255, 130, 60), np.float32)
            g = col(EMBER_RGB, 0.9)
        return col(np.clip(c, 0, 255)), g
    Blade(sp, 2.0, 3.1, spike_blade).build(m, a)
    # the rune discs on both sides of the head
    disc = disc_mask(7.5)
    dn = disc.shape[0]

    def rune_plate(i, j, front):
        r = math.hypot(i + 0.5 - dn / 2, j + 0.5 - dn / 2)
        c = np.array((40, 40, 56), np.float32) + nz(i * 1.3, j * 1.3, 96) * 20
        if r > dn / 2 - 1.2:
            c = np.array((110, 114, 140), np.float32)
        if _doom_rune(i, j, dn):
            return col((255, 150, 80)), col(EMBER_RGB, 1.0)
        return col(np.clip(c, 0, 255)), None
    plate(m, a, disc, -dn / 2, 3.5 - dn / 2, 0.45, rune_plate, "xy", 6.0)
    plate(m, a, disc, -dn / 2, 3.5 - dn / 2, 0.45, rune_plate, "xy", -6.0)
    return dict(glow=("FF6A30", 0.12, 0.4, 1.0))


SPECS += [("club_of_rot", club_of_rot), ("doom_hammer", doom_hammer)]


RELICS = [
    ("anti_bird_charm", anti_bird_charm),
    ("bond_thread", bond_thread),
    ("communication_magic_device", comm_device),
    ("devil_contract_book", devil_contract),
    ("fortune_die", fortune_die),
    ("mana_method_rune_stone", rune_stone),
    ("recovery_salve", recovery_salve),
    ("spirit_charm", spirit_charm),
    ("written_consent", written_consent),
    ("shroud_of_margins", shroud_of_margins),
    ("circlet_of_quickened_thought", circlet_of_thought),
]
SPECS += [("gauches_hand_mirror", hand_mirror), ("grimoire_chain", grimoire_chain), ("last_word", last_word), ("elsdocia", elsdocia),
          ("magebane_longsword", magebane_longsword), ("prism_blade", prism_blade), ("thundercleave", thundercleave), ("bloodbound_katana", bloodbound_katana),
          ("quicklings_ivy", quicklings_ivy), ("permafrost_sabre", permafrost_sabre), ("cyclone_rapier", cyclone_rapier)]                     # diagonal sprites: placed like the weapons


# ================================================================ 1.2.0: the Magebane Shield and the Weapon of Resurgence
# Executed in gen_weapon_models' namespace (np, math, Image, Mesh, Atlas, box, plate, Blade, ascii_mask, col, shade, nz, fz, hsh, fbm ... exist).
from PIL import ImageDraw  # noqa: E402

SH_FACE_NOISE = fbm(256, 256, 24, 7001)          # low-frequency blotches: soot on the rim, patina on the face


def _heater_outline(m=33, hw=12.5, top=15.2, arch=0.9, corner=2.8, y1=-0.5, bottom=-15.8, p=1.65):
    """The convex outline of a heater shield: 2 (m - 1) points equally spaced along its length, from the top centre round the right
    side to the tip (clockwise) and back up the left."""
    xe = hw - corner

    def ytop(x):
        return top + arch * (1 - (x / hw) ** 2)

    dense = []
    N = 240
    for i in range(N):                                         # the top edge, slightly arched
        x = xe * i / N
        dense.append((x, ytop(x)))
    cy = ytop(xe) - corner
    for i in range(N // 3 + 1):                                # the corner
        ang = (math.pi / 2) * i / (N // 3)
        dense.append((xe + corner * math.sin(ang), cy + corner * math.cos(ang)))
    for i in range(1, N // 2):                                 # the straight side
        dense.append((hw, cy + (y1 - cy) * i / (N // 2)))
    for i in range(N * 2 + 1):                                 # the convex curve down to the tip
        t = i / (N * 2)
        dense.append((hw * (1 - t ** p), y1 + (bottom - y1) * t))
    d = np.array(dense)
    seg = np.hypot(*np.diff(d, axis=0).T)
    cum = np.concatenate([[0], np.cumsum(seg)])
    right = []
    for k in range(m):
        target = cum[-1] * k / (m - 1)
        j = min(np.searchsorted(cum, target), len(cum) - 1)
        right.append(tuple(d[j]))
    right[0] = (0.0, top + arch)
    right[-1] = (0.0, bottom)
    left = [(-x, y) for (x, y) in right[-2:0:-1]]
    return right + left


def _petal(draw, S, base, ang, length, width, curve, fill=255):
    """A leaf-shaped feather from `base` (shield units) at angle `ang`, drawn through S(x, y) -> canvas xy."""
    n = 14
    left, right = [], []
    dx, dy = math.cos(ang), math.sin(ang)
    px, py = -dy, dx
    for k in range(n + 1):
        t = k / n
        cx = base[0] + dx * length * t + px * curve * length * t * t
        cy = base[1] + dy * length * t + py * curve * length * t * t
        w = width * (1 - t) ** 0.5 * min(1.0, 0.5 + t * 4)
        left.append(S(cx + px * w, cy + py * w))
        right.append(S(cx - px * w, cy - py * w))
    draw.polygon(left + right[::-1], fill=fill)


def _bez(p0, p1, p2, n=10):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in (k / n for k in range(n + 1))]


def _scallop(tips, pull, toward):
    """The ragged feather edge of a wing: the tips in order, a notch between each pair pulled a little toward `toward`."""
    pts = []
    for k, tp in enumerate(tips):
        pts.append(tp)
        if k + 1 < len(tips):
            mx, my = (tp[0] + tips[k + 1][0]) / 2, (tp[1] + tips[k + 1][1]) / 2
            pts.append((mx + (toward[0] - mx) * pull, my + (toward[1] - my) * pull))
    return pts


def _phoenix(fw, fh, x0, y1, tpu, ss=6):
    """The shield's phoenix over the face region, as a category map: 1 wing coverts, 2 tail, 3 wing, 4 body / head / beak / crest,
    5 eye, 6 feather lines. Wings are single scalloped shapes with a line to every feather tip; later shapes lie over earlier ones."""
    W, H = fw * ss, fh * ss
    cats = Image.new("L", (W, H), 0)
    dc = ImageDraw.Draw(cats)

    def S(x, y):
        return ((x - x0) * tpu * ss, (y1 - y) * tpu * ss)

    def Sm(x, y):
        return S(-x, y)

    # the tail: five long feathers fanned from the rump
    for off, ln, wd in ((-24, 7.4, 1.2), (24, 7.4, 1.2), (-11, 8.6, 1.2), (11, 8.6, 1.2), (0, 9.6, 1.3)):
        _petal(dc, S, (0, -2.6), math.radians(-90 + off), ln, wd, 0.08 * (1 if off > 0 else -1 if off < 0 else 0), 2)
    for T in (S, Sm):
        sh = (1.4, 3.3)
        tips = [(9.6, 9.6), (10.2, 7.0), (9.9, 4.5), (8.7, 2.2), (6.9, 0.2), (4.8, -1.2)]
        edge = _bez(sh, (3.6, 10.8), tips[0], 12) + _scallop(tips, 0.16, sh)[1:] + [(1.9, -1.0)]
        dc.polygon([T(*q) for q in edge], fill=3)
        cov = [(6.3, 6.6), (6.7, 4.7), (6.1, 2.8), (4.9, 1.3), (3.5, 0.2)]
        cedge = _bez((1.6, 5.0), (3.2, 6.8), cov[0], 8) + _scallop(cov, 0.18, sh)[1:] + [(2.0, 0.0)]
        dc.polygon([T(*q) for q in cedge], fill=1)
        for tp in tips:                                                    # a line from the wing's root to each feather tip
            dc.line([T(2.4, 3.0), T(tp[0] - (tp[0] - 2.4) * 0.12, tp[1] - (tp[1] - 3.0) * 0.12)], fill=6, width=max(1, int(ss * 0.34)))
    # body, head, beak, crest
    ex0, ey0 = S(-2.1, 5.7)
    ex1, ey1 = S(2.1, -2.9)
    dc.ellipse([ex0, ey0, ex1, ey1], fill=4)
    hx0, hy0 = S(-1.95, 8.4)
    hx1, hy1 = S(1.3, 5.0)
    dc.ellipse([hx0, hy0, hx1, hy1], fill=4)
    dc.polygon([S(-1.2, 7.2), S(-3.5, 6.2), S(-2.5, 5.7), S(-1.2, 5.9)], fill=4)
    for an, ln in ((62, 3.4), (90, 2.9), (118, 2.4)):
        _petal(dc, S, (0.6, 7.3), math.radians(an), ln, 0.5, 0.1 if an < 90 else -0.1, 4)
    e0, e1 = S(-1.15, 7.05), S(-0.7, 6.6)
    dc.ellipse([e0[0], e0[1], e1[0], e1[1]], fill=5)
    return {"cat": np.asarray(cats.resize((fw, fh), Image.NEAREST))}


def _shift(mask, dx, dy):
    out = np.zeros_like(mask)
    h, w = mask.shape
    ys, xs = slice(max(0, dy), min(h, h + dy)), slice(max(0, dx), min(w, w + dx))
    yd, xd = slice(max(0, -dy), min(h, h - dy)), slice(max(0, -dx), min(w, w - dx))
    out[ys, xs] = mask[yd, xd]
    return out


def magebane_shield(m, a):
    """Magebane Shield: a turquoise heater shield in the Spellplague style. A convex, slightly domed face of etched turquoise holds a bronze
    phoenix with spread wings; a raised rim of worn silver-bronze, scorched black in patches with arcane light in the cracks, carries
    gold rivets and glowing turquoise studs; the back is dark leather with an arm strap and a hand grip. Built upright, the face toward +z."""
    pts = _heater_outline()
    n = len(pts)
    P = np.array(pts, np.float64)
    ccx, ccy = 0.0, -0.3
    cen = np.array([ccx, ccy])
    rel = P - cen
    seg = np.hypot(*(np.roll(P, -1, 0) - P).T)
    cum = np.concatenate([[0], np.cumsum(seg)])
    s_of = cum / cum[-1]
    K_RIM, NR, TPU = 0.84, 8, 2.0
    Z_BACK, ZT_O, ZT_I, Z_F0, DOME = -1.5, 1.7, 1.3, 0.2, 1.6
    inner = cen + rel * K_RIM
    # polar radius of the rim's inner edge, for the face painter
    th = np.arctan2(rel[:, 1], rel[:, 0])
    rr = np.hypot(rel[:, 0], rel[:, 1]) * K_RIM
    order = np.argsort(th)
    th_s, rr_s = th[order], rr[order]
    th_x = np.concatenate([th_s - 2 * math.pi, th_s, th_s + 2 * math.pi])
    rr_x = np.concatenate([rr_s, rr_s, rr_s])

    def R_inner(t):
        return np.interp(t, th_x, rr_x)

    # ---- the atlas regions
    fx0, fx1 = inner[:, 0].min() - 0.5, inner[:, 0].max() + 0.5
    fy0, fy1 = inner[:, 1].min() - 0.5, inner[:, 1].max() + 0.5
    fw, fh = int(math.ceil((fx1 - fx0) * TPU)), int(math.ceil((fy1 - fy0) * TPU))
    fu, fv = a.alloc(fw, fh)
    RW = 122
    ru, rv = a.alloc(RW, 6)
    wu, wv = a.alloc(RW, 4)
    iu, iv = a.alloc(RW, 3)
    bx0, bx1 = P[:, 0].min() - 0.5, P[:, 0].max() + 0.5
    by0, by1 = P[:, 1].min() - 0.5, P[:, 1].max() + 0.5
    bw, bh = int(math.ceil(bx1 - bx0)), int(math.ceil(by1 - by0))
    bu, bv = a.alloc(bw, bh)

    # ---- the face: turquoise field, bronze border and studs, the phoenix
    ph = _phoenix(fw, fh, fx0, fy1, TPU)
    cat = ph["cat"]
    union = cat > 0
    shadow = _shift(union, 1, 1) & ~union
    hh, ww = cat.shape
    outline = union & ~(_shift(union, 1, 0) & _shift(union, -1, 0) & _shift(union, 0, 1) & _shift(union, 0, -1))

    def face_paint(lx, ly):
        x, y = fx0 + lx / TPU, fy1 - ly / TPU
        dx, dy = x - ccx, y - ccy
        t = math.atan2(dy, dx)
        rho = math.hypot(dx, dy) / float(R_inner(t))
        if rho > 1.04:
            return None, None
        ix, iy = int(lx), int(ly)
        v = (fy1 - y) / (fy1 - fy0)
        c = np.array((98, 212, 202), np.float32) * (1 - v) + np.array((52, 132, 138), np.float32) * v
        c = c * (1 - 0.14 * min(1.0, rho) ** 2) + nz(lx, ly, 3) * 24 + fz(lx * 1.3, ly, 4) * 12
        sn = SH_FACE_NOISE[(iy * 2 + 40) % 256, (ix * 2 + 90) % 256]
        if sn > 0.6:
            c = c * 0.88 + np.array((8, 22, 16), np.float32)                                          # verdigris blotches
        g = None
        if 0.92 < rho <= 0.97:                                                                        # the bronze border line
            c = np.array((184, 146, 92), np.float32) * (1.12 if (iy + ix) % 4 else 0.92) + fz(lx, ly, 5) * 12
            studs = ((t + math.pi) / (2 * math.pi)) * 40
            if abs(studs - round(studs)) < 0.16 and 0.935 < rho < 0.955:
                c = np.array((236, 206, 140), np.float32)
        elif rho > 0.97:
            c = np.array((22, 62, 68), np.float32) + fz(lx, ly, 6) * 8
        elif 0.80 < rho <= 0.825:
            c = c * 0.62 + np.array((78, 60, 40), np.float32) * 0.5                                   # a thin etched ring
        elif 0.86 < rho <= 0.9:
            tick = ((t + math.pi) / (2 * math.pi)) * 56                                              # arcane tick marks round the field
            if abs(tick - round(tick)) < 0.22 and hsh(round(tick), 3) > 0.35:
                c = np.array((150, 232, 218), np.float32)
                g = col((130, 255, 235), 0.35)
        cc = int(cat[iy, ix]) if iy < hh and ix < ww else 0
        if shadow[iy, ix] and rho < 0.9:
            c = np.array((24, 66, 72), np.float32)
        if cc:
            lit = 1.0 + 0.22 * (1 - v) - 0.12                                                         # warmer toward the top
            base = {1: (244, 210, 148), 2: (196, 150, 84), 3: (200, 150, 80), 4: (232, 194, 126), 5: (210, 255, 248), 6: (140, 98, 52)}[cc]
            c = np.array(base, np.float32) * lit + fz(lx * 1.4, ly, 7) * 12
            if cc == 4 and iy % 3 == 0 and ix % 2 == 0:
                c = c * 0.86                                                                          # breast feathers
            if cc == 2 and (ix + iy) % 5 == 0:
                c = c * 0.86
            if outline[iy, ix] and cc != 5:
                c = np.array((76, 50, 28), np.float32)
                g = col((120, 255, 232), 0.4)
            if cc == 5:
                c = np.array(base, np.float32)
                g = col((190, 255, 248), 1.0)
        return col(np.clip(c, 0, 255)), g

    a.paint(fu, fv, fw, fh, face_paint)

    # ---- the rim (top strip), its outer wall and inner wall: scorched worn silver-bronze
    def soot(u, v, band):
        s = SH_FACE_NOISE[(int(v * 3) + 170) % 256, (int(u * 1.05) + band * 55) % 256]
        return s

    def rim_paint(lx, ly):
        c = np.array((172, 154, 124), np.float32) + nz(lx, ly, 5) * 34 + fz(lx * 1.7, ly * 1.3, 6) * 16
        g = None
        if ly < 1:
            c = np.array((222, 206, 170), np.float32) + fz(lx, ly, 2) * 18
        elif ly > 4.6:
            c = c * 0.66
        elif 2.3 < ly < 3.3:
            c = c * 0.8
        s = soot(lx, ly, 0)
        if s > 0.62:
            k = min(1.0, (s - 0.62) / 0.1)
            c = c * (1 - 0.88 * k) + np.array((30, 24, 26), np.float32) * 0.88 * k + fz(lx, ly, 3) * 8
        elif 0.60 < s <= 0.62:
            c = np.array((72, 214, 200), np.float32)                                                 # arcane light in the scorch cracks
        return col(np.clip(c, 0, 255)), g

    def wall_paint(lx, ly):
        c = np.array((98, 84, 68), np.float32) * (1 - 0.1 * ly) + nz(lx, ly, 7) * 22
        s = soot(lx, ly + 7, 1)
        if s > 0.6:
            c = c * 0.45
        return col(np.clip(c, 0, 255)), None

    def inner_paint(lx, ly):
        c = np.array((120, 104, 82), np.float32) * (1.0 if ly < 1 else 0.7) + nz(lx, ly, 8) * 22
        if soot(lx, ly + 11, 2) > 0.62:
            c = c * 0.4
        return col(np.clip(c, 0, 255)), None

    a.paint(ru, rv, RW, 6, rim_paint)
    a.paint(wu, wv, RW, 4, wall_paint)
    a.paint(iu, iv, RW, 3, inner_paint)

    def back_paint(lx, ly):
        x, y = bx1 - lx, by1 - ly                                                                     # seen from behind: mirrored
        c = np.array((74, 54, 42), np.float32) + nz(lx, ly, 10) * 24
        if int(lx) % 5 == 0:
            c = c * 0.72                                                                              # plank seams
        if hsh(int(lx), int(ly), 4) > 0.97:
            c = c * 1.3
        if math.hypot(x - ccx, y - ccy) > 12.5:
            c = c * 0.78
        return col(np.clip(c, 0, 255)), None

    a.paint(bu, bv, bw, bh, back_paint)

    # ---- the quads
    def V(p, z):
        return (float(p[0]), float(p[1]), z)

    for i in range(n):
        j = (i + 1) % n
        u0, u1 = s_of[i] * RW, s_of[i + 1] * RW
        po, pj, qi, qj = P[i], P[j], inner[i], inner[j]
        radial = (float((po[0] + pj[0]) / 2 - ccx), float((po[1] + pj[1]) / 2 - ccy), 0.0)
        m.quad(0, [V(po, ZT_O) + (ru + u0, rv), V(pj, ZT_O) + (ru + u1, rv), V(qj, ZT_I) + (ru + u1, rv + 6), V(qi, ZT_I) + (ru + u0, rv + 6)], (0, 0, 1))
        m.quad(0, [V(po, ZT_O) + (wu + u0, wv), V(pj, ZT_O) + (wu + u1, wv), V(pj, Z_BACK) + (wu + u1, wv + 4), V(po, Z_BACK) + (wu + u0, wv + 4)], radial)
        m.quad(0, [V(qi, ZT_I) + (iu + u0, iv), V(qj, ZT_I) + (iu + u1, iv), V(qj, Z_F0) + (iu + u1, iv + 3), V(qi, Z_F0) + (iu + u0, iv + 3)], (-radial[0], -radial[1], 0))

        def bk(p):
            return (bu + (bx1 - p[0]), bv + (by1 - p[1]))
        m.quad(0, [V(cen, Z_BACK) + bk(cen), V(po, Z_BACK) + bk(po), V(pj, Z_BACK) + bk(pj), V(pj, Z_BACK) + bk(pj)], (0, 0, -1))

    def fpt(i, jj):
        s = K_RIM * jj / NR
        p = cen + rel[i % n] * s
        z = Z_F0 + DOME * (1 - (jj / NR) ** 2)
        return (float(p[0]), float(p[1]), z, fu + (float(p[0]) - fx0) * TPU, fv + (fy1 - float(p[1])) * TPU)

    for i in range(n):
        for jj in range(NR):
            m.quad(0, [fpt(i, jj), fpt(i + 1, jj), fpt(i + 1, jj + 1), fpt(i, jj + 1)], (0, 0, 1))

    # ---- rivets and glowing studs along the rim
    def rivet(gem):
        def p(face, i, j, fw_, fh_):
            if gem:
                return col(np.array((86, 230, 214), np.float32) * (1.0 if face == 5 else 0.7)), (col((140, 255, 238), 0.6) if face == 5 else None)
            return col(np.array((214, 172, 92), np.float32) * (1.2 if face == 0 else 0.8 if face == 1 else 1.0)), None
        return p

    for i in range(0, n, 4):
        mid = cen + rel[i] * (1 + K_RIM) / 2
        z0 = (ZT_O + ZT_I) / 2 - 0.1
        gem = (i // 4) % 2 == 1
        box(m, a, mid[0] - 0.45, mid[1] - 0.45, z0, mid[0] + 0.45, mid[1] + 0.45, z0 + (0.9 if gem else 0.7), rivet(gem))

    # ---- the back: an arm strap, a second strap, a hand grip, and their iron anchors
    leather = (86, 58, 40)
    box(m, a, -7.5, 3.4, Z_BACK - 1.0, 7.5, 6.0, Z_BACK, wrap_paint(leather, (140, 98, 62)))
    box(m, a, -6.5, -5.4, Z_BACK - 0.9, 6.5, -3.4, Z_BACK, wrap_paint(leather, (140, 98, 62)))
    box(m, a, -1.2, -4.6, Z_BACK - 2.7, 1.2, 4.6, Z_BACK - 0.9, wrap_paint((46, 32, 28), (96, 70, 48)))
    for (x, y) in ((-7.0, 4.7), (7.0, 4.7), (-6.0, -4.4), (6.0, -4.4)):
        box(m, a, x - 0.9, y - 0.9, Z_BACK - 1.4, x + 0.9, y + 0.9, Z_BACK - 0.6, metal_paint((120, 112, 100), rivets=True))
    return dict(glow=("C8FFF6", 0.09, 0.4, 1.0))


# ---------------------------------------------------------------- the Weapon of Resurgence
def weapon_of_resurgence(m, a):
    """Weapon of Resurgence: a long jade-teal sword wound from the hilt up with a holy gold chain, a gold cross-guard with a radiant cross at its
    heart, a dark jade grip ringed with chain, a gold pommel; the end of the chain hangs loose beside it. Gold light in the links and runes."""
    L = 54
    st = straight_stations(L, 3.0, 2.6, 8, steps=4, ridge=0)

    def paint(x, y, front, e, rd):
        if x is None:
            return col((62, 150, 128)), None
        t = y / L
        c = np.array((34, 112, 98), np.float32) * (1 - t) + np.array((84, 186, 158), np.float32) * t
        c = c + nz(x, y, 4) * 22 + fz(int(x * 2), int(y * 2), 5) * 12
        g = None
        if abs(x) < 0.55 and 4 < y < L - 8:
            c = np.array((170, 240, 210), np.float32)                                                  # the bright ridge
        if abs(x) < 0.5 and 8 < y < L - 10 and int(y) % 6 in (0, 1):
            c = np.array((236, 196, 86), np.float32)                                                   # a gold rune dash on the fuller
            g = col((255, 224, 130), 0.65)
        if 0.9 < abs(x) < 1.4 and 6 < y < L - 6:
            c = c * 0.78                                                                               # the fuller's edges
        if e < 0.9:
            c = np.array((206, 255, 232), np.float32)
        return col(np.clip(c, 0, 255)), g
    Blade(st, 0.7, 1.4, paint).build(m, a)
    gold, goldl = (226, 182, 70), (255, 232, 140)

    def link_paint(face, i, j, fw, fh):
        c = np.array(gold, np.float32) * (1.25 if face in (0, 5) else 0.8 if face == 1 else 1.0) + fz(i * 2, j * 2, 12) * 16
        return col(np.clip(c, 0, 255)), (col((255, 226, 130), 0.55) if face == 5 else None)

    # the chain: a helix round the blade, links alternating long and short
    turns, y_lo, y_hi = 4.5, 3.0, 41.0
    pitch = (y_hi - y_lo) / turns
    path_len = 20.0
    steps = int(turns * path_len / 1.55)
    for k in range(steps):
        ph = 2 * math.pi * turns * k / steps
        y = y_lo + pitch * ph / (2 * math.pi)
        hw = lerp(3.0, 2.6, y / L)
        x = (hw + 0.8) * math.cos(ph)
        z = 2.3 * math.sin(ph)
        tx, tz = -(hw + 0.8) * math.sin(ph), 2.3 * math.cos(ph)
        if k % 2 == 0:
            ex, ez = (0.95, 0.55) if abs(tx) > abs(tz) else (0.55, 0.95)
        else:
            ex, ez = (0.6, 0.55) if abs(tx) > abs(tz) else (0.55, 0.6)
        box(m, a, x - ex, y - 0.62, z - ez, x + ex, y + 0.62, z + ez, link_paint)
    # ---- the cross-guard: swept quillons with spiked ends round a block, a radiant cross on its face
    jade = (30, 96, 84)
    half = ascii_mask([
        "###...........",
        "####..........",
        ".#####........",
        "..#######.....",
        "...##########.",
        "....##########",
        ".....#########",
        "......########",
    ])
    full = np.concatenate([half, half[:, ::-1]], axis=1)

    def guard_paint(i, j, f):
        c = np.array(gold, np.float32) * (1.25 if (i + j) % 4 == 0 else 1.0 if j else 0.8)
        return col(np.clip(c, 0, 255)), (col((255, 226, 130), 0.4) if j == 3 else None)
    plate(m, a, full, -14.0, -5.4, 1.1, guard_paint, "xy", 0)
    box(m, a, -3.2, -3.4, -1.7, 3.2, 1.6, 1.7, metal_paint(jade, rivets=True))
    cross = ascii_mask(["..#..", "#####", "..#..", "..#..", "..#.."])
    plate(m, a, cross, -2.5, -3.4, 0.5, lambda i, j, f: (col(goldl), col((255, 236, 160), 0.95)), "xy", 1.9)
    # ---- the grip: dark jade leather, rings of chain, a gold pommel and a white-gold gem
    box(m, a, -1.5, -17.0, -1.5, 1.5, -3.4, 1.5, wrap_paint((22, 74, 64), (230, 190, 80), diamonds=True))
    def ring_paint(face, i, j, fw, fh):
        return col(np.clip(np.array(gold, np.float32) * (1.2 if face == 0 else 0.8 if face == 1 else 1.0) + fz(i * 2, j * 2, 13) * 14, 0, 255)), None
    for yy in (-5.0, -8.4, -11.8, -15.2):
        box(m, a, -1.95, yy - 0.55, -1.95, 1.95, yy + 0.55, 1.95, ring_paint)
    box(m, a, -2.6, -20.4, -2.6, 2.6, -17.0, 2.6, metal_paint(gold, gem=(255, 246, 200), glow_rgb=(255, 236, 160)))
    box(m, a, -1.5, -22.0, -1.5, 1.5, -20.4, 1.5, metal_paint(gold))
    for k, (x, y) in enumerate(((3.6, -19.2), (4.5, -21.2), (4.2, -23.4), (3.3, -25.2))):       # the loose end of the chain
        ex, ez = (0.55, 0.9) if k % 2 == 0 else (0.9, 0.55)
        box(m, a, x - ex, y - 0.8, -ez, x + ex, y + 0.8, ez, link_paint)
    return dict(glow=("FFEBB0", 0.1, 0.35, 0.9))


SPECS += [("weapon_of_resurgence", weapon_of_resurgence)]
RELICS += [("magebane_shield", magebane_shield)]                  # upright: placed on its sprite's box like a relic


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
SIZE = {"otherworld_trident": 1.65, "elsdocia": 2.0}         # 1.2.0: Elsdocia is a greatsword the size of the Wizard King's legend
GRIP = {"otherworld_trident": 0.42, "elsdocia": 0.2}


# the Club of Rot and the Doom Hammer are two-handed: bigger in hand, held where their grips are (fractions along the sprite axis)
SIZE.update({"club_of_rot": 1.3, "doom_hammer": 1.25})
GRIP.update({"club_of_rot": 0.21, "doom_hammer": 0.39})


# 1.3.0: weapons at the size they have in real life / in the anime (metres, whole weapon). 1 block = 1 m and the hand scale is 0.85.
REAL = {
    "demon_slasher_katana": 1.05, "miasma_infused_katana": 1.05, "bloodbound_katana": 1.05,        # a katana is about a metre
    "demon_dweller_sword": 1.5, "licht_dweller_sword": 1.5,                                          # the black / white broadswords
    "demon_destroyer_sword": 1.7, "licht_destroyer_sword": 1.7,                                      # the great ones
    "magebane_longsword": 1.25, "rimeheart_runeblade": 1.3, "prism_blade": 1.1, "permafrost_sabre": 1.0,
    "spell_forged_rapier": 1.15, "cyclone_rapier": 1.15,                                             # rapiers
    "severing_greatsword": 1.8, "thundercleave": 1.6, "elsdocia": 2.3,                              # two-handed greatswords; Elsdocia as in the movie
    "otherworld_trident": 1.9, "magic_tool_spear": 2.1, "magic_tool_sword": 1.0,
}


# 1.3.0: the Brooch Pin and the Hand of Providence (their spec functions live in tools/gen_pin_providence_models.py, run in this namespace)
exec(compile(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_pin_providence_models.py"), encoding="utf-8").read(), "gen_pin_providence_models.py", "exec"))


# ================================================================ 1.19.0: the Butcher Cleaver
# Executed in this namespace like the 1.3.0 models above (np, math, Image, Mesh, Atlas, box, col, nz, fz, hsh, REAL, GRIP and _xb_packed all
# exist here). The in-hand model of the cook's cleaver of the Living Dungeon:
#
#     python3 -B tools/gen_weapon_models.py butcher_cleaver        (writes the inventory icon and the item models first, then the mesh and skins)
#     python3 -B tools/item_preview/preview_weapons.py butcher_cleaver
#
# The model is built at four model units (and four skin texels) to the centimetre: the tool is 36 cm, so the mesh is 144 units long and ``REAL``
# below scales it to 0.36 m in hand. y runs along the tool (the grip below 0, the blade from 0 to 20 cm), x across it with the spine at +x and the
# honed edge leading at -x (like the Doom Hammer's face), z through it.
#   blade   a broad slab 20 x 10 cm of dark blued steel: a thick chamfered spine, a flat face hammered into facets, a ground bevel with a bright
#           honed edge, a hanging hole near the spine, three nicks in the edge, a maker's stamp, and small cooking runes etched along the spine
#           (they glow faintly teal, and a thin glint runs along the edge)
#   grip    dark wood with a worn leather cord wound round its middle, brass rivets, a brass ferrule and a brass pommel cap
BC_U = 4.0                                     # model units (= skin texels) to the centimetre
BC_XS, BC_XE, BC_XB = 1.7, -8.3, -6.3          # spine, honed edge and the shoulder where the bevel starts (cm)
BC_LEN = 20.0                                  # blade length (cm)
BC_TS, BC_TB, BC_TE = 0.46, 0.30, 0.05         # half thickness at the spine, at the shoulder and on the edge (cm)
BC_CH, BC_CHZ = 0.25, 0.14                     # the chamfer of the spine: how far it runs across the face, how much thinner the spine's top is
BC_HOLE = (-0.7, 15.9, 0.95)                   # the hanging hole: x, y, radius (cm)
BC_NICKS = ((5.4, 0.80, 0.42), (10.6, 1.00, 0.58), (14.0, 0.50, 0.26))   # nicks in the edge: y, width, depth (cm)
BC_RH, BC_RT = 0.9, 1.5                        # corner radii at the heel and the toe of the edge (cm)

_BC_STEEL_FLAT = np.array((60, 74, 100), np.float32)
_BC_RUNES = [                                  # 5 x 5 cooking runes: heat, cut, salt, bone, water, herb, fire
    ["..#..", ".#.#.", "#.#.#", ".#.#.", "..#.."],
    ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"],
    ["#.#.#", ".###.", "..#..", "..#..", "..#.."],
    ["#....", "####.", "#..#.", "####.", "#...."],
    [".#...", ".#.#.", ".###.", ".#.#.", ".#..."],
    ["..#..", ".###.", "#.#.#", "..#..", "..#.."],
    ["#.#..", "###..", "#.###", "###..", "#.#.."],
]


def _bc_edge_x(y):
    """x of the cutting edge at height y (cm): a slight belly, rounded corners and three nicks (each a steep chip: a long ramp in, a short fall out)."""
    x = BC_XE - 0.3 * math.sin(math.pi * y / BC_LEN)
    if y < BC_RH:
        x += BC_RH - math.sqrt(max(0.0, BC_RH ** 2 - (BC_RH - y) ** 2))
    if y > BC_LEN - BC_RT:
        x += BC_RT - math.sqrt(max(0.0, BC_RT ** 2 - (y - (BC_LEN - BC_RT)) ** 2))
    for (yc, w, d) in BC_NICKS:
        y0, ap, y1 = yc - w / 2, yc + 0.2 * w, yc + w / 2
        if y0 <= y <= ap:
            x += d * (y - y0) / (ap - y0)
        elif ap < y <= y1:
            x += d * (y1 - y) / (y1 - ap)
    return x


def _bc_spine_x(y):
    """x of the spine (the top corner at the toe is rounded)."""
    r = 0.5
    if y > BC_LEN - r:
        return BC_XS - r + math.sqrt(max(0.0, r * r - (y - (BC_LEN - r)) ** 2))
    return BC_XS


def _bc_zf(x):
    """Half thickness of the flat face at x: it thins a little from the spine's chamfer to the shoulder."""
    return BC_TB + (BC_TS - BC_TB) * min(1.0, max(0.0, (x - BC_XB) / (BC_XS - BC_CH - BC_XB)))


def _bc_hammer_map(w, h, seed, spacing=4.4):
    """The hammer marks as a shading map (about -1 .. 1): a jittered grid of shallow round dimples, lit from the upper left, every facet a
    little lighter or darker than its neighbours."""
    rng = np.random.default_rng(seed)
    hgt = np.zeros((h, w), np.float32)
    tint = np.zeros((h, w), np.float32)
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float32) + 0.5
    for gy in range(-1, int(h / spacing) + 2):
        for gx in range(-1, int(w / spacing) + 2):
            cx = (gx + 0.5 + (rng.random() - 0.5) * 0.95) * spacing
            cy = (gy + 0.5 + (rng.random() - 0.5) * 0.95) * spacing
            r = spacing * (0.60 + 0.24 * rng.random())
            dep = 0.7 + 0.5 * rng.random()
            tn = (rng.random() - 0.5) * 0.3
            xa, xb = int(max(0, cx - r - 1)), int(min(w, cx + r + 2))
            ya, yb = int(max(0, cy - r - 1)), int(min(h, cy + r + 2))
            if xa >= xb or ya >= yb:
                continue
            d = np.hypot(xs[ya:yb, xa:xb] - cx, ys[ya:yb, xa:xb] - cy)
            bowl = np.clip(1.0 - (d / r) ** 2, 0.0, 1.0) * dep
            win = bowl > hgt[ya:yb, xa:xb]
            hgt[ya:yb, xa:xb] = np.where(win, bowl, hgt[ya:yb, xa:xb])
            tint[ya:yb, xa:xb] = np.where(win & (bowl > 0.04), tn, tint[ya:yb, xa:xb])
    gy_, gx_ = np.gradient(hgt)
    return np.clip(-(gx_ + gy_) * 1.5 + tint, -1.0, 1.0)


def _bc_scratch_map(w, h, seed, n=22):
    """Fine scratches: a (h, w) map of how much lighter each texel is (0 .. 1), mostly along the blade with a few across."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    for k in range(n):
        x, y = rng.random() * w, rng.random() * h
        ln = (3.0 + rng.random() * 11.0)
        ang = math.pi / 2 + (rng.random() - 0.5) * (0.5 if k % 5 else 3.0)
        for s in np.arange(0.0, ln, 0.5):
            px, py = int(x + math.cos(ang) * s), int(y - math.sin(ang) * s)
            if 0 <= px < w and 0 <= py < h:
                out[py, px] = max(out[py, px], 0.55 + 0.45 * rng.random())
    return out


def _bc_rune_bit(lx, ly, front, h, spine_col):
    """Is this blade-face texel part of an etched rune? The runes read along the blade, 7 of them between the heel and the hanging hole."""
    r = lx - (spine_col - 6) if front else 6 - lx
    if not 0 <= r <= 4:
        return False
    yi = h - 1 - ly                                        # texels above the heel
    k, c = divmod(yi - 6, 7)
    if not (0 <= k < len(_BC_RUNES) and 0 <= c <= 4):
        return False
    if not front:
        k, c = len(_BC_RUNES) - 1 - k, 4 - c               # the back reads the other way, with the runes in the other order
    return _BC_RUNES[k][r][c] == "#"


def _bc_blade_painter(front, w, h, x0, x1, spine_col):
    """The skin of one face of the blade: hammered blued steel, a ground bevel, the honed edge, wear, and the runes (with their glow)."""
    U = BC_U
    salt = 0 if front else 5
    hm = _bc_hammer_map(w, h, 1190 + salt)
    scr = _bc_scratch_map(w, h, 77 + salt)
    hx, hy, hr = BC_HOLE

    def f(fx, fy):
        lx, ly = int(fx), int(fy)
        xc = x0 + (lx + 0.5) / U if front else x1 - (lx + 0.5) / U
        yc = BC_LEN - (ly + 0.5) / U
        yq = min(max(yc, 0.0), BC_LEN)
        xe, xs = _bc_edge_x(yq), _bc_spine_x(yq)
        de, ds = xc - xe, xs - xc
        tx, ty = xc * U + salt * 31, yc * U
        g = None
        sh = float(hm[ly, lx])
        if de < 0.14:                                                                  # the very edge: a hairline of white
            c = np.array((250, 253, 255), np.float32) + fz(tx, ty, 11) * 10
        elif de < 0.7:                                                                 # the honed band
            c = np.array((232, 241, 251), np.float32) * (0.93 + 0.1 * math.sin(yc * 1.7 + salt)) + fz(tx * 0.4, ty, 12) * 26
        elif xc < BC_XB:                                                               # the ground bevel: polished steel, grinding streaks along the blade
            t = (xc - xe) / (BC_XB - xe)
            c = np.array((170, 186, 208), np.float32) * (1 - t) + np.array((98, 114, 140), np.float32) * t
            c = c + fz(tx * 0.35, ty * 0.05, 31) * 44 + fz(tx * 0.8, ty * 0.12, 32) * 18 + sh * 7
            if t > 0.84:
                c = c * 0.78                                                           # a soft shadow where the bevel meets the flat
        else:                                                                          # the flat: dark blued steel, hammered into facets
            c = _BC_STEEL_FLAT * (1.0 + 0.55 * sh) + nz(tx * 0.8, ty * 0.8, 21) * 22 + fz(tx, ty * 0.3, 22) * 8
            if ds < 1.3:                                                               # forge scale towards the spine
                k = 1.0 - ds / 1.3
                c = c * (1.0 - 0.34 * k)
                if nz(tx * 0.9, ty * 0.9, 23) > 0.06:
                    c = c * 0.72
            ts_ = BC_XB + 0.8 - xc
            if ts_ > 0:
                c = c * (1 - 0.3 * ts_ / 0.8) + np.array((88, 76, 116), np.float32) * 0.3 * ts_ / 0.8     # a faint heat tint above the shoulder
            if abs(xc - BC_XB) < 0.1:
                c = np.array((26, 33, 50), np.float32)                                 # the shoulder line
            elif ds < BC_CH:
                c = c * 1.5 + 10                                                       # the chamfer of the spine
        dh = math.hypot(xc - hx, yc - hy)
        if hr < dh < hr + 0.2:
            c = np.array((150, 166, 192), np.float32) * (0.85 + 0.3 * math.sin(math.atan2(yc - hy, xc - hx) + salt))   # the bright rim of the hole
        elif hr + 0.2 <= dh < hr + 0.7:
            c = c * 0.82
        if front:                                                                      # a maker's stamp near the heel: a ring round a cross
            ds_ = math.hypot(xc + 2.6, yc - 3.5)
            if 0.62 < ds_ < 0.86 or (ds_ < 0.56 and (abs(xc + 2.6) < 0.1 or abs(yc - 3.5) < 0.1)):
                c = c * 0.5
        if scr[ly, lx] > 0 and de > 0.7:                                              # scratches
            c = c + 34 * scr[ly, lx]
        rust = nz(tx * 0.55, ty * 0.55, 41 + salt)
        if rust > 0.18 and de > 0.14 and (dh < 2.2 or yc < 3.4 or (de < 1.6 and xc > BC_XB)):   # rust freckles round the hole and the heel
            k = min(1.0, (rust - 0.18) * 7.0)
            c = c * (1 - 0.55 * k) + np.array((118, 70, 40), np.float32) * 0.55 * k
        blood = nz(tx * 0.4, ty * 0.4, 43 + salt)
        if blood > 0.12 and 0.14 < de < 3.0 and yc > 7.0:                              # a dull dried-blood film along the edge
            c = c * 0.78 + np.array((88, 32, 34), np.float32) * 0.22 * min(1.0, (blood - 0.12) * 8)
        if de < 0.9 and hsh(int(tx), int(ty), 61 + salt) > 0.965:                      # a few bright fleck-chips on the edge
            c = np.array((255, 255, 255), np.float32)
        if _bc_rune_bit(lx, ly, front, h, spine_col) and de > 1.0:                     # the etched cooking runes
            c = np.array((34, 118, 118), np.float32) + fz(tx, ty, 71) * 22
            g = col((196, 255, 244), 0.62)
        elif 0.14 <= de < 0.36 and 1.0 < yc < 19.2:                                    # the glint along the edge, broken up by the nicks
            near = min(abs(yc - nk[0]) for nk in BC_NICKS)
            g = col((176, 255, 240), (0.14 + 0.26 * (0.5 + 0.5 * math.sin(yc * 0.9 + salt))) * min(1.0, near / 0.7))
        return col(np.clip(c, 0, 255)), g
    return f


def _bc_tube(m, a, st, sides, paint, sw=2, p=2.6, caps=(False, False)):
    """A lathe with a rounded-rectangle (superellipse) section: stations (y, rx, rz) in model units, bottom to top; paint(k, s, y, t) -> (rgba, glow)
    for side k at fraction s across it and fraction t round the tube."""
    y0, y1 = st[0][0], st[-1][0]
    H = int(math.ceil(y1 - y0)) + 1
    W = sides * sw
    u0, v0 = a.alloc(W, H)

    def f(lx, ly):
        k = min(sides - 1, int(lx // sw))
        s = (lx - k * sw) / sw
        return paint(k, s, y1 - ly, (k + s) / sides)
    a.paint(u0, v0, W, H, f)
    e = 2.0 / p

    def ring(i, k):
        y, rx, rz = st[i]
        th = 2 * math.pi * (k % sides) / sides
        c, s_ = math.cos(th), math.sin(th)
        return (rx * math.copysign(abs(c) ** e, c), y, rz * math.copysign(abs(s_) ** e, s_))

    def vtex(y):
        return v0 + (y1 - y)
    for i in range(len(st) - 1):
        for k in range(sides):
            A, B, C, D = ring(i, k), ring(i, k + 1), ring(i + 1, k + 1), ring(i + 1, k)
            ua, ub = u0 + k * sw + 0.02, u0 + (k + 1) * sw - 0.02
            va, vb = min(vtex(st[i][0]), v0 + H - 0.02), max(vtex(st[i + 1][0]), v0 + 0.02)
            m.quad(0, [A + (ua, va), B + (ub, va), C + (ub, vb), D + (ua, vb)], ((A[0] + C[0]) / 2, 0.0, (A[2] + C[2]) / 2))
    for (idx, sgn, on) in ((0, -1, caps[0]), (len(st) - 1, 1, caps[1])):
        if not on:
            continue
        cu, cv = u0 + W / 2, min(max(vtex(st[idx][0]), v0 + 0.02), v0 + H - 0.02)
        for k in range(sides):
            m.quad(0, [(0.0, st[idx][0], 0.0, cu, cv), ring(idx, k) + (cu, cv), ring(idx, k + 1) + (cu, cv), ring(idx, k + 1) + (cu, cv)], (0.0, sgn, 0.0))


def _bc_wood(k, s, y, t):
    """Dark wood: long grain with pores, lighter where hands have worn it."""
    x = (k + s) * 2.0
    yy = y * BC_U
    grain = math.sin(x * 1.6 + nz(x * 0.4, yy * 0.06, 301) * 10.0)
    c = np.array((92, 60, 40), np.float32) * (0.8 + 0.22 * grain) + nz(x, yy, 302) * 16 + fz(x * 2, yy * 0.5, 303) * 10
    if fz(x * 3, yy * 0.9, 304) < -0.3:
        c = c * 0.66                                                         # pores
    c = c * (0.86 + 0.24 * math.cos(2 * math.pi * t - 2.2))
    return col(np.clip(c, 0, 255)), None


def _bc_cord(k, s, y, t):
    """Worn leather cord, wound in tight coils: a bright crest and a dark valley to every coil, sweat-dark patches, a few loose fibres."""
    x = (k + s) * 2.0
    yy = y * BC_U
    pitch = 0.6 * BC_U
    ph = ((yy - (-12.0 * BC_U)) / pitch + 0.1 * t * 12) % 1.0
    prof = max(0.0, 1.0 - (abs(ph - 0.5) * 2.0) ** 2)
    c = np.array((62, 38, 22), np.float32) * (1 - prof) + np.array((166, 118, 74), np.float32) * prof
    c = c + nz(x, yy, 311) * 30 + fz(x * 2, yy * 2, 312) * 12
    if nz(x * 0.5, yy * 0.4, 313) > 0.08:
        c = c * 0.72                                                         # sweat and grease
    if prof > 0.7 and hsh(int(x), int(yy), 314) > 0.93:
        c = np.array((204, 164, 112), np.float32)                            # a loose fibre
    c = c * (0.86 + 0.24 * math.cos(2 * math.pi * t - 2.2))
    return col(np.clip(c, 0, 255)), None


def _bc_brass(k, s, y, t):
    """Worn brass: brushed round the tube, bright on the lit side, a faint green patina in the crevices, a few dents."""
    x = (k + s) * 2.0
    yy = y * BC_U
    c = np.array((214, 154, 56), np.float32) + fz(x * 0.4, yy * 2.5, 321) * 34 + nz(x, yy, 322) * 16
    c = c * (0.74 + 0.34 * math.cos(2 * math.pi * t - 2.0))
    if nz(x * 0.7, yy * 0.7, 323) > 0.17:
        c = c * 0.78 + np.array((92, 124, 92), np.float32) * 0.22             # verdigris
    if hsh(int(x), int(yy), 324) > 0.97:
        c = c * 1.3 + 20
    return col(np.clip(c, 0, 255)), None


def _bc_brass_box(face, i, j, fw, fh):
    c = np.array((236, 176, 66), np.float32) * (1.15 if face == 0 else 0.8 if face == 1 else 1.0) + fz(i * 1.9, j * 1.3, face + 331) * 22
    if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
        c = c * 1.18
    return col(np.clip(c, 0, 255)), None


@_xb_packed
def butcher_cleaver(m, a):
    """Butcher Cleaver: a broad slab of dark blued steel, hammered, with a ground bevel and a bright honed edge, a hanging hole, three nicks, cooking
    runes etched along the spine (they glow faintly teal, and a glint runs along the edge); a dark wood grip wound with worn leather cord, brass
    rivets, ferrule and pommel cap. The edge leads at -x."""
    U = BC_U

    def P(x, y, z):
        return (x * U, y * U, z * U)

    # ---- the blade's skin regions
    xe_min = min(_bc_edge_x(i * BC_LEN / 400) for i in range(401))
    x0 = math.floor(xe_min * U) / U
    w = int(math.ceil((BC_XS - x0) * U - 1e-9))
    x1 = x0 + w / U
    h = int(round(BC_LEN * U))
    spine_col = w - 1
    fu, fv = a.alloc(w, h)
    bu, bv = a.alloc(w, h)
    a.paint(fu, fv, w, h, _bc_blade_painter(True, w, h, x0, x1, spine_col))
    a.paint(bu, bv, w, h, _bc_blade_painter(False, w, h, x0, x1, spine_col))
    TSP = BC_TS - BC_CHZ                                   # half thickness of the spine's top
    sw_ = int(math.ceil(2 * TSP * U))
    su, sv = a.alloc(sw_, h)
    eu, ev = a.alloc(1, h)
    tu, tv = a.alloc(w, 1)
    hu, hv = a.alloc(w, 1)
    N = 16
    wu, wv = a.alloc(N, 3)

    def spine_paint(fx, fy):
        lx, ly = int(fx), int(fy)
        c = np.array((40, 50, 74), np.float32) + fz(lx * 2, ly * 0.5, 81) * 16 + nz(lx, ly, 82) * 10
        if lx in (0, sw_ - 1):
            c = c * 1.45 + 8
        if nz(lx * 0.8, ly * 0.8, 83) > 0.08:
            c = c * 0.7
        return col(np.clip(c, 0, 255)), None

    def edge_paint(fx, fy):
        lx, ly = int(fx), int(fy)
        return col(np.clip(np.array((236, 244, 252), np.float32) + fz(lx, ly, 84) * 16, 0, 255)), None

    def end_paint(salt):
        def fn(fx, fy):
            lx = int(fx)
            xc = x0 + (lx + 0.5) / U
            c = np.array((52, 64, 90), np.float32) + fz(lx * 1.5, salt, 85) * 22
            if xc < BC_XB:
                c = np.array((176, 190, 210), np.float32) + fz(lx * 1.5, salt, 86) * 22
            return col(np.clip(c, 0, 255)), None
        return fn

    def wall_paint(fx, fy):
        k, row = int(fx), int(fy)
        ang = 2 * math.pi * k / N
        c = np.array((176, 190, 210), np.float32) * (0.62 + 0.38 * (0.5 + 0.5 * math.cos(ang - 2.3))) + fz(k * 2, row, 87) * 14
        if row != 1:
            c = c * 1.12
        if nz(k * 3.0, row, 88) > 0.1:
            c = c * 0.75 + np.array((110, 66, 38), np.float32) * 0.25               # rust inside the hole
        return col(np.clip(c, 0, 255)), None
    a.paint(su, sv, sw_, h, spine_paint)
    a.paint(eu, ev, 1, h, edge_paint)
    a.paint(tu, tv, w, 1, end_paint(1))
    a.paint(hu, hv, w, 1, end_paint(2))
    a.paint(wu, wv, N, 3, wall_paint)

    def uv(x, y, front):
        if front:
            return (min(max(fu + (x - x0) * U, fu + 0.02), fu + w - 0.02), min(max(fv + (BC_LEN - y) * U, fv + 0.02), fv + h - 0.02))
        return (min(max(bu + (x1 - x) * U, bu + 0.02), bu + w - 0.02), min(max(bv + (BC_LEN - y) * U, bv + 0.02), bv + h - 0.02))

    def V(x, y, z, front):
        return P(x, y, z) + uv(x, y, front)

    # ---- the blade: bands between stations along y, a main face, a bevel, the edge, the chamfer and the spine on each side
    hx, hy, hr = BC_HOLE
    RB = hr * 1.45
    ys = {0.0, BC_LEN, hy - RB, hy + RB}
    for k in range(5):
        ys.add(BC_RH * k / 4)
    for k in range(7):
        ys.add(BC_LEN - BC_RT + BC_RT * k / 6)
    for k in range(1, 20):
        ys.add(float(k))
    for (yc, wd, d) in BC_NICKS:
        ys.update({yc - wd / 2, yc + 0.2 * wd, yc + wd / 2})
    ys = sorted(ys)
    ys = [y for i, y in enumerate(ys) if i == 0 or y - ys[i - 1] > 0.04]
    for ya, yb in zip(ys, ys[1:]):
        xea, xeb = _bc_edge_x(ya), _bc_edge_x(yb)
        xsa, xsb = _bc_spine_x(ya), _bc_spine_x(yb)
        xca, xcb = xsa - BC_CH, xsb - BC_CH                                                # where the flat face meets the spine's chamfer
        inbox = ya >= hy - RB - 1e-6 and yb <= hy + RB + 1e-6
        for s in (1, -1):
            fr = s > 0
            if inbox:                                                                     # the main face either side of the hole
                m.quad(0, [V(BC_XB, ya, s * _bc_zf(BC_XB), fr), V(hx - RB, ya, s * _bc_zf(hx - RB), fr), V(hx - RB, yb, s * _bc_zf(hx - RB), fr),
                           V(BC_XB, yb, s * _bc_zf(BC_XB), fr)], (0, 0, s))
                m.quad(0, [V(hx + RB, ya, s * _bc_zf(hx + RB), fr), V(xca, ya, s * _bc_zf(xca), fr), V(xcb, yb, s * _bc_zf(xcb), fr),
                           V(hx + RB, yb, s * _bc_zf(hx + RB), fr)], (0, 0, s))
            else:
                m.quad(0, [V(BC_XB, ya, s * _bc_zf(BC_XB), fr), V(xca, ya, s * _bc_zf(xca), fr), V(xcb, yb, s * _bc_zf(xcb), fr),
                           V(BC_XB, yb, s * _bc_zf(BC_XB), fr)], (0, 0, s))
            m.quad(0, [V(xca, ya, s * BC_TS, fr), V(xsa, ya, s * TSP, fr), V(xsb, yb, s * TSP, fr), V(xcb, yb, s * BC_TS, fr)], (0.5, 0, s))     # the chamfer
            m.quad(0, [V(xea, ya, s * BC_TE, fr), V(BC_XB, ya, s * BC_TB, fr), V(BC_XB, yb, s * BC_TB, fr), V(xeb, yb, s * BC_TE, fr)], (0, 0, s))
        # the edge: a hairline wall, one texel wide
        ea = (eu + 0.5, ev + min(max((BC_LEN - ya) * U, 0.02), h - 0.02))
        eb = (eu + 0.5, ev + min(max((BC_LEN - yb) * U, 0.02), h - 0.02))
        m.quad(0, [P(xea, ya, BC_TE) + ea, P(xea, ya, -BC_TE) + ea, P(xeb, yb, -BC_TE) + eb, P(xeb, yb, BC_TE) + eb], (-(yb - ya), xeb - xea, 0.0))
        # the spine: a strip across its thickness
        sa = (lambda z: su + min(max((TSP - z) * U, 0.02), sw_ - 0.02))
        va_, vb_ = sv + min(max((BC_LEN - ya) * U, 0.02), h - 0.02), sv + min(max((BC_LEN - yb) * U, 0.02), h - 0.02)
        m.quad(0, [P(xsa, ya, TSP) + (sa(TSP), va_), P(xsa, ya, -TSP) + (sa(-TSP), va_), P(xsb, yb, -TSP) + (sa(-TSP), vb_), P(xsb, yb, TSP) + (sa(TSP), vb_)], (1, 0, 0))
    # the two ends of the slab (the heel is half inside the ferrule)
    for (yy_, sgn, ru, rv) in ((BC_LEN, 1, tu, tv), (0.0, -1, hu, hv)):
        xe_, xs_ = _bc_edge_x(yy_), _bc_spine_x(yy_)
        xc_ = xs_ - BC_CH
        va_ = rv + 0.5

        def ue(x, ru=ru):
            return ru + min(max((x - x0) * U, 0.02), w - 0.02)
        m.quad(0, [P(xs_, yy_, TSP) + (ue(xs_), va_), P(xc_, yy_, BC_TS) + (ue(xc_), va_), P(xc_, yy_, -BC_TS) + (ue(xc_), va_),
                   P(xs_, yy_, -TSP) + (ue(xs_), va_)], (0, sgn, 0))
        m.quad(0, [P(xc_, yy_, BC_TS) + (ue(xc_), va_), P(BC_XB, yy_, BC_TB) + (ue(BC_XB), va_), P(BC_XB, yy_, -BC_TB) + (ue(BC_XB), va_),
                   P(xc_, yy_, -BC_TS) + (ue(xc_), va_)], (0, sgn, 0))
        m.quad(0, [P(BC_XB, yy_, BC_TB) + (ue(BC_XB), va_), P(xe_, yy_, BC_TE) + (ue(xe_), va_), P(xe_, yy_, -BC_TE) + (ue(xe_), va_),
                   P(BC_XB, yy_, -BC_TB) + (ue(BC_XB), va_)], (0, sgn, 0))
    # ---- the hanging hole: a ring of facets round it on each face, and its wall
    for k in range(N):
        t0, t1 = 2 * math.pi * k / N, 2 * math.pi * (k + 1) / N
        c0 = (hx + hr * math.cos(t0), hy + hr * math.sin(t0))
        c1 = (hx + hr * math.cos(t1), hy + hr * math.sin(t1))
        q0, q1 = max(abs(math.cos(t0)), abs(math.sin(t0))), max(abs(math.cos(t1)), abs(math.sin(t1)))
        s0 = (hx + RB * math.cos(t0) / q0, hy + RB * math.sin(t0) / q0)
        s1 = (hx + RB * math.cos(t1) / q1, hy + RB * math.sin(t1) / q1)
        for s in (1, -1):
            fr = s > 0
            m.quad(0, [V(c0[0], c0[1], s * _bc_zf(c0[0]), fr), V(c1[0], c1[1], s * _bc_zf(c1[0]), fr), V(s1[0], s1[1], s * _bc_zf(s1[0]), fr),
                       V(s0[0], s0[1], s * _bc_zf(s0[0]), fr)], (0, 0, s))
        tm = (t0 + t1) / 2
        z0, z1 = _bc_zf(c0[0]), _bc_zf(c1[0])
        m.quad(0, [P(c0[0], c0[1], z0) + (wu + k + 0.02, wv + 0.02), P(c1[0], c1[1], z1) + (wu + k + 0.98, wv + 0.02),
                   P(c1[0], c1[1], -z1) + (wu + k + 0.98, wv + 2.98), P(c0[0], c0[1], -z0) + (wu + k + 0.02, wv + 2.98)], (-math.cos(tm), -math.sin(tm), 0.0))

    # ---- the grip, from the ferrule to the pommel (cm in, model units out)
    ytop, ybot, cord0, cord1 = -1.9, -14.4, -12.0, -4.3

    def core(y):
        u = (y - (ybot + ytop) / 2) / ((ytop - ybot) / 2)
        rx = 1.58 + 0.13 * u * u
        return rx, rx * 0.78

    def S(y, d=0.0):
        rx, rz = core(y)
        return (y * U, (rx + d) * U, (rz + d) * U)
    stg = [S(ybot), S(ybot + 0.8), S(cord0 - 0.45)]
    j = 0
    y = cord0
    while y < cord1 - 0.01:                                                              # the coils of the cord: valley, crest, valley ...
        stg.append(S(y, 0.0 if j % 2 == 0 else 0.15))
        y += 0.3
        j += 1
    stg += [S(cord1, 0.05), S(cord1 + 0.5), S(ytop)]

    def grip_paint(k, s, yc, t):
        return _bc_cord(k, s, yc, t) if cord0 - 0.2 < yc < cord1 + 0.2 else _bc_wood(k, s, yc, t)

    def tube_paint(fn):
        return lambda k, s, y, t: fn(k, s, y / U, t)
    _bc_tube(m, a, stg, 12, tube_paint(grip_paint), sw=2)
    # rivets: brass studs through the wood either side of the cord
    for yr in (-3.1, -13.2):
        rz = core(yr)[1]
        for s in (1, -1):
            lo, hi = sorted((s * (rz - 0.04) * U, s * (rz + 0.2) * U))
            box(m, a, -0.3 * U, (yr - 0.3) * U, lo, 0.3 * U, (yr + 0.3) * U, hi, _bc_brass_box)
    # the loose end of the cord: a strip lying down the flat of the grip from the top of the wrap, frayed at its tip
    def cord_end(face, i, j, fw, fh):
        c = np.array((152, 106, 66), np.float32) + fz(i * 2, j * 2, 341) * 26
        if j >= fh - 3 and (i + j) % 2 == 0:
            c = c * 0.55 + 20                                                                # fray
        if i == 0 or i == fw - 1:
            c = c * 0.7
        return col(np.clip(c, 0, 255)), None
    rzt = core(-5.8)[1]
    box(m, a, 0.2 * U, -7.4 * U, (rzt + 0.17) * U, 0.7 * U, -4.35 * U, (rzt + 0.23) * U, cord_end)
    # the brass ferrule: rim rings and a waist, closed on top where the blade enters it
    fe = [(-2.0, 1.70, 1.32), (-1.93, 1.88, 1.50), (-1.55, 1.88, 1.50), (-1.47, 1.76, 1.38), (-0.65, 1.76, 1.38), (-0.57, 1.88, 1.50), (0.15, 1.88, 1.50),
          (0.25, 1.76, 1.38)]
    _bc_tube(m, a, [(y_ * U, rx_ * U, rz_ * U) for (y_, rx_, rz_) in fe], 12, tube_paint(_bc_brass), sw=2, caps=(False, True))
    # the pommel cap: flared, domed, closed
    pc = [(-14.3, 1.78, 1.38), (-14.5, 1.94, 1.54), (-15.0, 2.02, 1.62), (-15.5, 1.92, 1.52), (-15.85, 1.5, 1.16), (-16.0, 1.0, 0.8)]
    _bc_tube(m, a, [(y_ * U, rx_ * U, rz_ * U) for (y_, rx_, rz_) in pc][::-1], 12, tube_paint(_bc_brass), sw=2, caps=(True, False))
    return dict(glow=("6BF0E0", 0.09, 0.3, 0.8))


SPECS += [("butcher_cleaver", butcher_cleaver)]
REAL.update({"butcher_cleaver": 0.36})               # metres: the whole tool, the blade 20 cm and the grip 16 cm
GRIP.update({"butcher_cleaver": 0.30})               # the middle of the grip (a fifth of the tool's length from the butt) rests 0.27 of the way up the icon's diagonal, where the hand is


# ---------------------------------------------------------------- the inventory icon and the item models
def _bc_icon_pixels():
    """The 16 x 16 inventory icon on the usual bottom-left to top-right diagonal: the cleaver's broad slab on the upper right with its bright honed edge
    on the upper-left side, a hanging hole and three teal runes near the spine, the brass-capped, cord-wound grip below. {(x, y): (r, g, b)}."""
    R2 = math.sqrt(2.0)

    def frame(px, py):
        dx, dy = px - 0.5, py - 15.5
        return (dx - dy) / R2, (-dx - dy) / R2                         # t along the tool (up and to the right), s across it (towards the edge)

    def rr(t, s, t0, t1, s0, s1, r):
        cx, cy, hx, hy = (t0 + t1) / 2, (s0 + s1) / 2, (t1 - t0) / 2, (s1 - s0) / 2
        qx, qy = abs(t - cx) - hx + r, abs(s - cy) - hy + r
        return math.hypot(max(qx, 0.0), max(qy, 0.0)) + min(max(qx, qy), 0.0) - r
    px = {}
    steel_e, steel_l, blue, blue_d, spine = (240, 247, 255), (166, 188, 212), (76, 94, 128), (56, 70, 100), (40, 50, 76)
    teal = (96, 240, 220)
    for y in range(16):
        for x in range(16):
            t, s = frame(x + 0.5, y + 0.5)
            if rr(t, s, 7.0, 16.8, -1.7, 3.8, 0.4) <= 0:                # the slab
                c = steel_e if s > 2.65 else steel_l if s > 1.75 else (blue if (x + y) % 3 else blue_d) if s > -0.2 else spine
                px[(x, y)] = c
            elif rr(t, s, 0.0, 7.5, -1.2, 1.2, 0.2) <= 0:               # the grip: pommel, wound cord, ferrule
                if t > 6.5:
                    c = (255, 228, 130) if s > 0 else (224, 168, 62)
                elif t < 1.7:
                    c = (224, 168, 62) if s > 0 else (150, 100, 34)
                else:
                    c = (150, 104, 64) if int((t - 1.7) * 0.95) % 2 == 0 else (100, 66, 42)
                    c = tuple(min(255, int(v * 1.14)) for v in c) if s > 0.3 else tuple(int(v * 0.78) for v in c) if s < -0.4 else c
                px[(x, y)] = c
    for (x, y) in list(px):
        t, s = frame(x + 0.5, y + 0.5)
        if 0.0 < s < 1.1 and px[(x, y)] in (blue, blue_d, spine) and any(abs(t - tt) < 0.55 for tt in (9.2, 10.9, 12.5)):
            px[(x, y)] = teal                                          # the runes
        if abs(t - 14.4) < 0.7 and abs(s - 0.9) < 0.7 and px[(x, y)] != teal:
            del px[(x, y)]                                              # the hanging hole
    cells = set(px)
    for (x, y) in cells:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                q = (x + dx, y + dy)
                if q not in cells and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                    px[q] = (24, 20, 34)
    return px


_bc_sprite_axis = sprite_axis


def sprite_axis(item_id):                                                            # noqa: F811
    """The cleaver's icon is not a thin diagonal bar (its slab hangs off one side of the grip, which would tilt a fitted axis): the axis is the exact
    bottom-left to top-right diagonal the grip is drawn on, from the icon's lowest-left pixel to its highest-right one."""
    if item_id != "butcher_cleaver":
        return _bc_sprite_axis(item_id)
    a = np.asarray(Image.open(os.path.join(ASSETS, "textures", "item", item_id + ".png")).convert("RGBA"))
    S = a.shape[0]
    ys, xs = np.nonzero(a[..., 3] > 40)
    u = ((xs + 0.5) / S + 1 - (ys + 0.5) / S) / math.sqrt(2)
    o = float(u.min()) / math.sqrt(2)
    return np.array([o, o]), 45.0, float(u.max() - u.min())


def _bc_write_icon():
    """Writes textures/item/butcher_cleaver.png, models/item/butcher_cleaver_icon.json (the flat icon) and models/item/butcher_cleaver.json (the
    3D in-hand model drawn by client/WeaponRenderer; the display transforms are the Magebane Longsword's, the mesh is placed on the icon's diagonal; only
    the ground and frame transforms are recentred)."""
    import json
    icons = os.path.join(ASSETS, "textures", "item")
    models = os.path.join(ASSETS, "models", "item")
    os.makedirs(icons, exist_ok=True)
    os.makedirs(models, exist_ok=True)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in _bc_icon_pixels().items():
        im.putpixel((x, y), (int(c[0]), int(c[1]), int(c[2]), 255))
    im.save(os.path.join(icons, "butcher_cleaver.png"))

    def dump(name, data):
        with open(os.path.join(models, name + ".json"), "w", newline="\n") as f:
            json.dump(data, f, indent=2)
            f.write("\n")
    dump("butcher_cleaver_icon", {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/butcher_cleaver"}})
    src = json.load(open(os.path.join(models, "magebane_longsword.json")))
    src["textures"] = {"particle": "nusmp:item/butcher_cleaver"}
    # In hand the standard transforms hold (the mesh is placed so the middle of its grip is where the hand takes the icon's diagonal). The cleaver is
    # only a third of the icon's length and sits low on the diagonal, so on the ground and in a frame it is moved to the middle of the model
    # space (its box is centred at 0.325, 0.351 of the 0.5, 0.5 the transforms turn about; a frame turns the model 180 degrees about y, so x flips).
    src["display"]["ground"]["translation"] = [1.4, 3.2, 0]
    src["display"]["fixed"]["translation"] = [-2.8, 2.4, 0]
    dump("butcher_cleaver", src)


# ================================================================ 1.21.0: the relics and the navigation gear of the Dimensional Nexus (tools/gen_nexus_relic_models.py)
# Executed in this namespace like the 1.3.0 models above; run tools/gen_nexus_relic_icons.py first (their meshes are placed on those sprites).
exec(compile(open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "gen_nexus_relic_models.py"), encoding="utf-8").read(), "gen_nexus_relic_models.py", "exec"))



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
        if item_id in REAL:                                           # 1.3.0: the length it has in real life (m, blade and hilt), 1 block = 1 m
            k = REAL[item_id] / (0.85 * length)
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
    try:                                                         # 1.3.0: every skin is raised to 512 (Elsdocia 1024), meshes unchanged
        sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
        import hires_weapon_textures
        hires_weapon_textures.process(os.path.join(tdir, item_id + ".png"))
        if glow_any:
            hires_weapon_textures.process(os.path.join(tdir, item_id + "_glow.png"))
    except ImportError:
        pass
    print(f"{item_id}: {n} quads, length {ymax - ymin:.1f}px, angle {angle:.1f}, glow {bool(glow_any)}")


def main(only=None):
    if not only or "butcher_cleaver" in only:                      # 1.19.0: its sprite must exist before the mesh is placed on it
        _bc_write_icon()
    for fit, specs in ((False, SPECS), (True, RELICS)):
        for item_id, fn in specs:
            if only and item_id not in only:
                continue
            mesh, atlas = Mesh(), Atlas()
            extra = fn(mesh, atlas) or {}
            write(item_id, mesh, atlas, extra, fit)


if __name__ == "__main__":
    main(sys.argv[1:])

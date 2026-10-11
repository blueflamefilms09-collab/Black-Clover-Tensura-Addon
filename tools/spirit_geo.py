"""0.102: a tiny helper for painted geo models: boxes pack themselves into the texture and are painted face by face.

    sheet = Sheet(256, 256)
    sheet.box(m, "head", (-4, 24, -4), (8, 8, 8), paint)            # paint(face, i, j, fw, fh) -> (rgba | None, glow rgba | None)
    sheet.plate(m, "wing", (0, 0, 0), (20, 14), shape)              # a thin cut-out plate: shape(i, j, front) -> (rgba | None, glow | None)
    img, glow = sheet.images()

Faces of a box: "top", "bottom", "right" (-x), "front" (-z), "left" (+x), "back" (+z), laid out like a vanilla box uv.
Colours are 4-tuples 0..255; None = transparent.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm  # noqa: E402

NOISE = fbm(256, 256, 8, 7000)
FINE = fbm(256, 256, 3, 7001)


def nz(x, y, salt=0):
    return float(NOISE[int(y * 3 + salt * 37) % 256, int(x * 3 + 128 + salt * 11) % 256]) - 0.5


def fz(x, y, salt=0):
    return float(FINE[int(y * 2 + salt * 13) % 256, int(x * 2 + 64 + salt * 7) % 256]) - 0.5


def hsh(*k):
    h = 0
    for v in k:
        h = (h * 1000003 + int(v * 97) + 12345) & 0xFFFFFFFF
    h ^= h >> 13
    h = (h * 1274126177) & 0xFFFFFFFF
    return (h & 0xFFFF) / 65535


def rgba(c, a=255):
    return (int(max(0, min(255, c[0]))), int(max(0, min(255, c[1]))), int(max(0, min(255, c[2]))), int(a))


def mix(a, b, t):
    return tuple(a[i] * (1 - t) + b[i] * t for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, v * k)) for v in c[:3])


class Sheet:
    def __init__(self, w, h):
        self.W, self.H = w, h
        self.x = self.y = self.row = 0
        self.img = np.zeros((h, w, 4), np.float32)
        self.glow = np.zeros((h, w, 4), np.float32)

    def alloc(self, w, h):
        w, h = int(math.ceil(w)), int(math.ceil(h))
        if self.x + w > self.W:
            self.x, self.y, self.row = 0, self.y + self.row + 1, 0
        if self.y + h > self.H:
            raise RuntimeError("texture sheet full (%dx%d)" % (self.W, self.H))
        u, v = self.x, self.y
        self.x += w + 1
        self.row = max(self.row, h)
        return u, v

    def _put(self, x, y, c, g):
        if 0 <= x < self.W and 0 <= y < self.H:
            if c is not None:
                self.img[y, x] = c
            if g is not None:
                self.glow[y, x] = g

    def box(self, m, bone, origin, size, paint, inflate=0.0, rotation=None, pivot=None, mirror=False):
        sx, sy, sz = size
        X, Y, Z = [max(1, int(math.ceil(k))) for k in (sx, sy, sz)]
        u, v = self.alloc(2 * (X + Z), Z + Y)
        faces = {"top": (u + Z, v, X, Z), "bottom": (u + Z + X, v, X, Z), "right": (u, v + Z, Z, Y), "front": (u + Z, v + Z, X, Y),
                 "left": (u + Z + X, v + Z, Z, Y), "back": (u + 2 * Z + X, v + Z, X, Y)}
        for name, (fu, fv, fw, fh) in faces.items():
            for j in range(fh):
                for i in range(fw):
                    c, g = paint(name, i, j, fw, fh)
                    self._put(fu + i, fv + j, c, g)
        return m.cube(bone, origin, size, uv=(u, v), inflate=inflate, rotation=rotation, pivot=pivot, mirror=mirror)

    def plate(self, m, bone, origin, wh, shape, inflate=0.0, rotation=None, pivot=None, thick=1):
        """A thin plate in the xy plane (w wide, h tall): faces front/back painted by shape(i, j, front); the edges stay clear."""
        w, h = wh

        def paint(face, i, j, fw, fh):
            if face == "front":
                return shape(i, j, True)
            if face == "back":
                return shape(fw - 1 - i, j, False)
            return None, None
        return self.box(m, bone, origin, (w, h, thick), paint, inflate, rotation, pivot)

    def images(self):
        a = Image.fromarray(np.clip(self.img, 0, 255).astype(np.uint8), "RGBA")
        g = Image.fromarray(np.clip(self.glow, 0, 255).astype(np.uint8), "RGBA")
        return a, g

    def used(self):
        return self.y + self.row


def voronoi(x, y, cell, salt=0):
    """Distance to the nearest cell edge-ish (0 = on a crack) and the cell id, for stone / scale patterns."""
    cx, cy = int(x // cell), int(y // cell)
    best = 1e9
    second = 1e9
    bid = 0
    for dy in (-1, 0, 1):
        for dx in (-1, 0, 1):
            gx, gy = cx + dx, cy + dy
            px = (gx + hsh(gx, gy, salt)) * cell
            py = (gy + hsh(gx, gy, salt + 5)) * cell
            d = math.hypot(x - px, y - py)
            if d < best:
                second, best, bid = best, d, hsh(gx, gy, salt + 9)
            elif d < second:
                second = d
    return (second - best), bid


def _side_plate(self, m, bone, origin, dh, shape, inflate=0.0, rotation=None, pivot=None, thick=1):
    """A thin plate in the yz plane (d deep, h tall): faces right / left painted by shape(i, j) with i = 0 at the model's front (-z)."""
    d, h = dh

    def paint(face, i, j, fw, fh):
        if face == "right":
            return shape(fw - 1 - i, j)
        if face == "left":
            return shape(i, j)
        return None, None
    return self.box(m, bone, origin, (thick, h, d), paint, inflate, rotation, pivot)


Sheet.side_plate = _side_plate


def _flat_plate(self, m, bone, origin, wd, shape, inflate=0.0, rotation=None, pivot=None, thick=1):
    """A thin plate in the xz plane (w wide, d deep): faces top / bottom painted by shape(i, j, top) (j = 0 at the front)."""
    w, d = wd

    def paint(face, i, j, fw, fh):
        if face == "top":
            return shape(i, j, True)
        if face == "bottom":
            return shape(i, fh - 1 - j, False)
        return None, None
    return self.box(m, bone, origin, (w, thick, d), paint, inflate, rotation, pivot)


Sheet.flat_plate = _flat_plate

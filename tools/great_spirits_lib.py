"""Shared kit of the Great Spirits generators (1.99.0): tools/gen_sylph_fairy.py, tools/gen_undine_mermaid.py, tools/gen_salamander_blaze.py, tools/gen_spirit_dive_geo.py.

It sits on top of tools/water_aura_lib.py (Piece = a model whose cubes lay their own texture nets out, Mat = a procedural material) and adds what the spirit looks need:

  * FnMat      a material that is ONE python function: fn(face, w, h, tag) -> (colour image, glow image), both float arrays of shape (h, w, 4). Row 0 is the top of a side
               face (for the "up" face it is the front edge). So a face, a honeycomb wing or a scalloped hem can be drawn exactly, at the density the cube asks for.
  * helpers    signed-distance shapes with soft edges (disc, ellipse, capsule), a hexagon grid, thin-film (soap bubble) colours, a few palettes.

Frame of the files: model pixels = 1/16 block, y UP, front on -z, +x the model's left (see tools/geo_builder.py). Run python as `python3 -B`.
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, rgb, smooth, fbm, mbox, stepped_spike, sine_rot, loop_rot, spin_y, shell_texture  # noqa: E402,F401

TAU = 2.0 * math.pi


# ====================================================================================================================== image helpers
def grid(w, h):
    """u, v in 0..1 (texel centres), x, y in texels; all of shape (h, w)."""
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w, (y + 0.5) / h, x + 0.5, y + 0.5


def col(hexstr):
    return rgb(hexstr)


def lerp(a, b, t):
    t = np.asarray(t, dtype=np.float32)
    return a + (b - a) * t[..., None] if t.ndim else a + (b - a) * t


def cover(d, soft=0.9):
    """Coverage 0..1 of a signed distance d in texels (negative inside): a 1 texel soft edge."""
    return np.clip(0.5 - d / soft, 0.0, 1.0).astype(np.float32)


def disc(x, y, cx, cy, r):
    return np.hypot(x - cx, y - cy) - r


def ellipse(x, y, cx, cy, rx, ry):
    """Approximate signed distance of an ellipse in texels."""
    k = np.hypot((x - cx) / rx, (y - cy) / ry)
    return (k - 1.0) * min(rx, ry)


def capsule(x, y, ax, ay, bx, by, r):
    px, py = x - ax, y - ay
    bx, by = bx - ax, by - ay
    t = np.clip((px * bx + py * by) / max(1e-6, bx * bx + by * by), 0.0, 1.0)
    return np.hypot(px - bx * t, py - by * t) - r


def paste(img, c, a):
    """Alpha-composites colour c (3,) with coverage a (h, w) onto img (h, w, 4) in place (straight alpha)."""
    a = np.clip(a, 0.0, 1.0)[..., None]
    out_a = a + img[..., 3:4] / 255.0 * (1 - a)
    rgbv = (np.asarray(c, dtype=np.float32)[None, None, :] * a + img[..., :3] * (img[..., 3:4] / 255.0) * (1 - a)) / np.maximum(out_a, 1e-5)
    img[..., :3] = np.where(out_a > 0.001, rgbv, img[..., :3])
    img[..., 3:4] = out_a * 255.0


def add_glow(g, c, a):
    a = np.clip(a, 0.0, 1.0)[..., None]
    g[..., :3] = np.maximum(g[..., :3], np.asarray(c, dtype=np.float32)[None, None, :] * a)
    g[..., 3:4] = np.maximum(g[..., 3:4], np.where(a > 0.03, 255.0, 0.0))


def hsv(h, s, v):
    """h, s, v arrays (0..1) -> rgb array (..., 3) in 0..255."""
    h = np.asarray(h, dtype=np.float32) % 1.0
    s = np.asarray(s, dtype=np.float32)
    v = np.asarray(v, dtype=np.float32)
    i = np.floor(h * 6).astype(int)
    f = h * 6 - i
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    i = i % 6
    r = np.choose(i, [v, q, p, p, t, v])
    g = np.choose(i, [t, v, v, q, p, p])
    b = np.choose(i, [p, p, t, v, v, q])
    return np.stack([r, g, b], axis=-1) * 255.0


def film(phase, sat=0.38, val=1.0, lo=0.30, hi=0.92):
    """Thin-film (soap bubble) colour for a phase value: the hue runs green -> cyan -> blue -> violet -> pink and back as the phase goes round (lo..hi is the hue range)."""
    phase = np.asarray(phase, dtype=np.float32)
    k = 0.5 + 0.5 * np.sin(phase * TAU)
    return hsv(lo + (hi - lo) * k, sat, val)


def hexcells(x, y, size):
    """A pointy-top hexagon grid of cell radius `size` (texels): returns (edge, cx, cy): `edge` is the distance in texels to the nearest cell border (0 on a border) and (cx, cy)
    the integer cell coordinates (for a per-cell hash)."""
    s3 = math.sqrt(3.0)
    q = (s3 / 3.0 * x - 1.0 / 3.0 * y) / size
    r = (2.0 / 3.0 * y) / size
    # cube rounding
    cx_, cz_ = q, r
    cy_ = -cx_ - cz_
    rx, ry, rz = np.round(cx_), np.round(cy_), np.round(cz_)
    dx, dy, dz = np.abs(rx - cx_), np.abs(ry - cy_), np.abs(rz - cz_)
    fix_x = (dx > dy) & (dx > dz)
    fix_y = (~fix_x) & (dy > dz)
    rx = np.where(fix_x, -ry - rz, rx)
    rz = np.where(~fix_x & ~fix_y, -rx - ry, rz)
    qc, rc = rx, rz
    # centre of the cell in texel space
    px = size * s3 * (qc + rc / 2.0)
    py = size * 1.5 * rc
    ox, oy = x - px, y - py
    # distance to the border of a pointy-top hexagon: max over the three axes of the projection onto the edge normals
    a1 = np.abs(ox)
    a2 = np.abs(ox * 0.5 + oy * (s3 / 2.0))
    a3 = np.abs(ox * 0.5 - oy * (s3 / 2.0))
    apothem = size * s3 / 2.0
    edge = apothem - np.maximum(a1, np.maximum(a2, a3))
    return edge, qc, rc


def hash2(a, b, salt=0):
    """A deterministic 0..1 hash of two integer arrays."""
    h = (np.asarray(a, dtype=np.int64) * 73856093) ^ (np.asarray(b, dtype=np.int64) * 19349663) ^ (salt * 83492791 + 12345)
    h = (h ^ (h >> 13)) * 1274126177
    h = h ^ (h >> 16)
    return ((h & 0xFFFF) / 65535.0).astype(np.float32)


# ====================================================================================================================== the function material
class FnMat:
    """A material that is one function: fn(face, w, h, tag) -> (img (h, w, 4), glow (h, w, 4)), both float 0..255. Pass it anywhere Piece.box wants a Mat."""

    def __init__(self, fn, name=""):
        self.fn, self.name = fn, name

    def paint(self, face, fw, fh, tag):
        w, h = max(1, fw), max(1, fh)
        img, glow = self.fn(face, w, h, tag)
        return img.astype(np.float32), glow.astype(np.float32)


def blank(w, h):
    return np.zeros((h, w, 4), dtype=np.float32), np.zeros((h, w, 4), dtype=np.float32)


def shaded(face, base, dark, light, w, h, noise=0.05, tag="", seed=0, glow=None, glow_amount=0.0):
    """A smooth coloured face with a top light and a little noise: the plain skin / cloth of a cube. Returns (img, glow)."""
    rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + seed * 7919) % (2 ** 32))
    u, v, x, y = grid(w, h)
    n = fbm(rng, w, h, 3, 3) - 0.5
    lum = 0.55 + 0.2 * (1 - v) - 0.08 * np.abs(u - 0.5) + n * noise * 2
    if face == "up":
        lum = lum + 0.14
    elif face == "down":
        lum = lum - 0.2
    elif face in ("west", "east"):
        lum = lum - 0.05
    lum = np.clip(lum, 0, 1)
    c = np.where((lum < 0.5)[..., None], dark + (base - dark) * (lum * 2)[..., None], base + (light - base) * ((lum - 0.5) * 2)[..., None])
    img = np.zeros((h, w, 4), dtype=np.float32)
    img[..., :3] = c
    img[..., 3] = 255.0
    g = np.zeros((h, w, 4), dtype=np.float32)
    if glow is not None and glow_amount > 0:
        add_glow(g, glow, np.full((h, w), glow_amount, dtype=np.float32))
    return img, g


def solid(c, alpha=255, glow=None, glow_a=0.0, noise=0.0, tag=""):
    """A single colour on every face (with optional noise and glow): FnMat."""
    c = np.asarray(c, dtype=np.float32)

    def fn(face, w, h, t):
        rng = np.random.default_rng(sum(ord(ch) for ch in tag + t + face))
        img = np.zeros((h, w, 4), dtype=np.float32)
        n = (fbm(rng, w, h, 3, 3) - 0.5) * noise * 255 if noise else 0.0
        img[..., :3] = np.clip(c[None, None, :] + (n[..., None] if noise else 0), 0, 255)
        img[..., 3] = alpha
        g = np.zeros((h, w, 4), dtype=np.float32)
        if glow is not None:
            add_glow(g, glow, np.full((h, w), glow_a, dtype=np.float32))
        return img, g
    return FnMat(fn, "solid")

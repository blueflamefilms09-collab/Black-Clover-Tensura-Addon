"""Spirit Dive remake: the 2D kit of tools/gen_spirit_dive_v2.py (shapes drawn with soft edges, gradients, the sprite writer).

Everything is plain numpy / Pillow and deterministic (seeded). A "face" is an (h, w) float image; the colour images are (h, w, 4) float 0..255 like tools/great_spirits_lib.py. Frame of a face: x runs
along the piece (root at 0), y across it (0 = top). Run python as `python3 -B`.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import ROOT  # noqa: E402

TAU = 2.0 * math.pi
SS = 4                                   # supersampling of the shape masks


def rgb(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)


def mix(a, b, t):
    t = np.asarray(t, dtype=np.float32)
    return a[None, None, :] + (b - a)[None, None, :] * t[..., None]


def clamp01(x):
    return np.clip(x, 0.0, 1.0)


def smooth(x, a, b):
    t = clamp01((x - a) / max(1e-6, b - a))
    return t * t * (3 - 2 * t)


# ====================================================================================================================== masks
def mask_from_polys(w, h, polys, ss=SS):
    """Coverage 0..1 (h, w) of the union of polygons given in 0..1 face coordinates [(x, y), ...] (soft edge by supersampling)."""
    im = Image.new("L", (w * ss, h * ss), 0)
    d = ImageDraw.Draw(im)
    for poly in polys:
        d.polygon([(x * w * ss, y * h * ss) for x, y in poly], fill=255)
    im = im.resize((w, h), Image.LANCZOS)
    return np.asarray(im, dtype=np.float32) / 255.0


def blur(a, r):
    if r <= 0:
        return a
    im = Image.fromarray((clamp01(a) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, dtype=np.float32) / 255.0


def rim_of(mask, r):
    """A band along the inside of the edge of a mask (1 on the edge, 0 deep inside)."""
    inner = blur(mask, r)
    return clamp01((mask - inner) * 2.2) * mask


def value_noise(rng, w, h, cells_x, cells_y):
    g = rng.random((cells_y + 2, cells_x + 2)).astype(np.float32)
    ys = np.linspace(0, cells_y, h, endpoint=False)
    xs = np.linspace(0, cells_x, w, endpoint=False)
    y0 = ys.astype(int)[:, None]
    x0 = xs.astype(int)[None, :]
    fy = (ys - y0[:, 0])[:, None]
    fx = (xs - x0[0, :])[None, :]
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    a = g[y0, x0] * (1 - fx) + g[y0, x0 + 1] * fx
    b = g[y0 + 1, x0] * (1 - fx) + g[y0 + 1, x0 + 1] * fx
    return a * (1 - fy) + b * fy


def fbm2(rng, w, h, cx, cy, octaves=4):
    out = np.zeros((h, w), dtype=np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        out += value_noise(rng, w, h, cx * 2 ** o, cy * 2 ** o) * amp
        tot += amp
        amp *= 0.5
    return out / tot


def seed_of(*parts):
    """A stable seed from any parts (python's own hash() changes between runs, this does not)."""
    return sum((i + 1) * sum(ord(c) * (k + 1) for k, c in enumerate(str(p))) * 7919 for i, p in enumerate(parts)) % (2 ** 32)


# ====================================================================================================================== shapes (polygons in 0..1 face coordinates)
def curve_poly(top, bottom):
    """A closed polygon from two edge curves given as point lists left to right."""
    return list(top) + list(reversed(list(bottom)))


def blade_polys(curl=0.0, belly=0.5, tip=0.03):
    """A swept wind blade (scimitar / feather): root at the left, widest at ~35 percent, the trailing edge hollowed, the tip curling up by `curl` (0..0.3)."""
    n = 24
    top, bot = [], []
    for i in range(n + 1):
        t = i / n
        x = t
        prof = (math.sin(math.pi * min(1.0, t ** 0.62 * 1.0)) ** 0.9)                     # 0 at the root, 1 near 35 percent, back to 0 at the tip
        wdt = 0.46 * prof * (1.0 - 0.15 * t)
        yc = 0.5 - curl * (t ** 2.4)
        top.append((x, yc - wdt * (0.35 + 0.65 * belly)))
        bot.append((x, yc + wdt * (1.0 - 0.7 * math.sin(math.pi * t) * 0.55)))
    top[0] = (0.0, 0.5)
    bot[0] = (0.0, 0.5)
    top[-1] = (1.0, 0.5 - curl - tip)
    bot[-1] = top[-1]
    return [curve_poly(top, bot)]


def tongue_polys(sway=0.18, phase=0.0, base=0.8, wob=0.07, n=28):
    """A flame tongue rising from the bottom centre of the face to a pointed, swaying tip at the top (y: 0 top, 1 bottom)."""
    left, right = [], []
    for i in range(n + 1):
        t = i / n                                    # 0 at the base, 1 at the tip
        y = 1.0 - t
        cx = 0.5 + sway * math.sin(t * 2.6 + phase) * t + wob * math.sin(t * 9.0 + phase * 2.0) * t
        wdt = base * 0.5 * (1.0 - t) ** 0.78 * (1.0 + 0.22 * math.sin(t * 7.0 + phase * 1.7))
        left.append((cx - wdt, y))
        right.append((cx + wdt, y))
    return [curve_poly(left, right)]


def tip_right(polys):
    """A shape drawn with its tip at the top and its root at the bottom, turned so the root is at the left and the tip at the right (for a flat box that runs along x)."""
    return [[(1.0 - y, x) for x, y in poly] for poly in polys]


def tip_left(polys):
    """The mirror of tip_right: the root at the right, the tip at the left (the left-hand fan of a pair)."""
    return [[(y, x) for x, y in poly] for poly in polys]


def rect_poly(x0, y0, x1, y1):
    return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]


def place(polys, x0, y0, x1, y1):
    """Maps shapes drawn in 0..1 into the box (x0, y0)-(x1, y1) of the face."""
    return [[(x0 + (x1 - x0) * x, y0 + (y1 - y0) * y) for x, y in poly] for poly in polys]


def tip_down(polys):
    """A shape drawn with its tip at the top, flipped so the tip is at the bottom (a hanging flame, a cape hem)."""
    return [[(x, 1.0 - y) for x, y in poly] for poly in polys]


def ribbon_polys(wave=0.12, phase=0.0, width=0.8, taper=0.5, n=30):
    """A ribbon hanging down the face (y: 0 top, 1 bottom) with a travelling wave, narrowing to a rounded tail."""
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        cx = 0.5 + wave * math.sin(t * 5.0 + phase) * (0.3 + 0.7 * t)
        wdt = width * 0.5 * (1.0 - taper * t ** 1.6) * (1.0 if t < 0.96 else math.sqrt(max(0.0, (1.0 - t) / 0.04)))
        left.append((cx - wdt, t))
        right.append((cx + wdt, t))
    return [curve_poly(left, right)]


def leaf_polys(width=0.9):
    """A pointed leaf / prong standing on the bottom of the face (y: 0 top = the tip)."""
    n = 20
    left, right = [], []
    for i in range(n + 1):
        t = i / n                                    # 0 at the tip, 1 at the foot
        wdt = width * 0.5 * (math.sin(math.pi * min(1.0, t * 0.9 + 0.05)) ** 0.8) * (0.55 + 0.45 * t)
        left.append((0.5 - wdt, t))
        right.append((0.5 + wdt, t))
    return [curve_poly(left, right)]


# ====================================================================================================================== sprite writer
def save_rgba(name, arr, sub="aura"):
    os.makedirs(os.path.join(ROOT, "textures", sub), exist_ok=True)
    path = os.path.join(ROOT, "textures", sub, name + ".png")
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(path)
    return path


def premult(col, alpha):
    """An additive sprite: the colour is already weighted by its alpha (black where it is clear), alpha kept so the alpha-weighted blend and the plain one look the same."""
    a = clamp01(alpha)[..., None]
    out = np.zeros(col.shape[:2] + (4,), dtype=np.float32)
    out[..., :3] = col[..., :3] * a
    out[..., 3] = a[..., 0] * 255.0
    return out


def radial(size):
    y, x = np.mgrid[0:size, 0:size].astype(np.float32)
    c = (size - 1) / 2.0
    dx, dy = (x - c) / c, (y - c) / c
    return np.hypot(dx, dy), np.arctan2(dy, dx)

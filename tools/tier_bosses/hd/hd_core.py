"""HD skins, part 1: the canvas, the faces and the drawing primitives (3.38.0, docs/bosses/hd_outfits.md).

An HD skin is the vanilla 64x64 player layout drawn at 128 or 256 texels: the same UV rectangles scaled by f = size / 64, so every MODEL pixel is an f x f block of texels (the model's UVs are
fractions of 64, so the game simply samples the bigger PNG finer; nothing in the engine changes). This module paints into those rectangles with smooth, anti-aliased shapes:

* `Canvas(size=256, slim=False)`: float RGBA planes for the skin and the glow map. `cv.face(part, face, overlay)` returns a `Face`, one UV rectangle ("island"), the only thing anything is
  ever drawn into. Everything is clipped to its island, so colour can never bleed across a seam; a blur or gradient works on one island at a time, with edge replication at its border.
* Coordinates of a `Face` are MODEL PIXELS of that face as it appears in the texture (viewed from outside; x to the right, y down; floats): the 8 x 12 front of the body runs 0..8 x 0..12,
  whatever the size of the PNG. `face.w`, `face.h` are its size, `face.f` the texels per pixel. Parts: head body arm_r arm_l leg_r leg_l. `overlay=True` is the second layer (hat, jacket,
  sleeve, pants). The faces are front back west east top bottom, laid out as in tools/playermodel/skin_lib.py (west = the entity's right side, its front at the RIGHT end; east = the left side,
  its front at the LEFT end; the back's x = 0 is the entity's left).
* Masks are float coverage arrays (H, W) from signed distance fields (`m_poly`, `m_ellipse`, `m_rect`, `m_line`, `m_polyline`, `m_ring`, `m_band`), anti-aliased to a texel (`soft` = edge width
  in texels, 1 = crisp). Combine with * (and), `union`, `minus`. `face.fill(mask, paint, ...)` composites; a paint is a colour ('#rrggbb' or (r, g, b) 0..255), `lin(...)`, `rad(...)` or a function
  of (X, Y). Shapes (`poly`, `ellipse`, `rect`, `line`, ...) are `fill` of the mask.
* The second layer is drawn with the cutout render type (alpha >= 0.1 is fully opaque in the game), so `Canvas.finish()` makes every overlay texel either fully opaque or fully transparent.
  The base layer is made opaque. The glow map is written premultiplied (colour times alpha, black where nothing glows), which the mod's additive pass handles with or without alpha weighting.

Numpy and Pillow only. Deterministic: no random numbers (`hsh` is a hash).
"""
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TB = os.path.abspath(os.path.join(HERE, ".."))
TOOLS = os.path.abspath(os.path.join(TB, ".."))
for _p in (os.path.join(TOOLS, "playermodel"), TOOLS, TB):
    if _p not in sys.path:
        sys.path.insert(0, _p)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import skin_lib as K  # noqa: E402

FACES = ("front", "back", "west", "east", "top", "bottom")
PARTS = tuple(K.PARTS)
# the direction TOWARD the light inside each face (texture space, x right, y down): the light is at the upper left of the viewer, in front of the entity
LIGHT2D = {"front": (-0.50, -0.86), "back": (0.50, -0.86), "west": (0.50, -0.86), "east": (-0.50, -0.86), "top": (-0.50, 0.86), "bottom": (0.0, 0.0)}
# how strongly each face is lit by that light (0..1), used by the shading helpers
FACE_LIT = {"front": 1.0, "west": 0.8, "top": 1.0, "east": 0.45, "back": 0.4, "bottom": 0.1}


def hsh(*vals):
    """A deterministic hash in 0..1."""
    return K.hsh(*vals)


# ================================================================================================ colour
def col(c):
    """A colour as float32 (3,) in 0..1. '#rrggbb', (r, g, b[, a]) in 0..255 or a float array (taken as 0..1 when it is an ndarray)."""
    if isinstance(c, np.ndarray):
        return np.asarray(c[:3], np.float32)
    if isinstance(c, str):
        s = c.lstrip("#")
        return np.array([int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)], np.float32) / 255.0
    return np.array([c[0], c[1], c[2]], np.float32) / 255.0


def alpha_of(c):
    """The alpha of a colour given as an (r, g, b, a) tuple, else 1."""
    if isinstance(c, (tuple, list)) and len(c) == 4:
        return float(c[3]) / 255.0
    return 1.0


def rgb8(c):
    """A colour as an (r, g, b) tuple of ints 0..255."""
    a = col(c)
    return tuple(int(round(float(v) * 255)) for v in np.clip(a, 0, 1))


def mix(a, b, t):
    """Linear mix of two colours (float arrays); t may be an array of shape (H, W) (then the result is (H, W, 3))."""
    a, b = col(a), col(b)
    if np.ndim(t) == 0:
        return a + (b - a) * float(t)
    t = np.asarray(t, np.float32)[..., None]
    return a + (b - a) * t


_COOL = np.array([70, 60, 130], np.float32) / 255.0          # the anime hue shift of anime_skin.lit: shadows go cool and richer, lights go warm
_WARM = np.array([255, 244, 226], np.float32) / 255.0


def tone(c, k):
    """The colour at light k: 1 = as is, < 1 shadow (cooler and richer), > 1 light (warmer, paler). A colour or an (..., 3) float array; same rule as anime_skin.lit."""
    c = c if (isinstance(c, np.ndarray) and c.ndim > 1) else col(c)
    if k < 1.0:
        t = min(0.5, (1.0 - k) * 0.9)
        return np.clip((c + (_COOL - c) * t) * (0.55 + 0.45 * k), 0, 1).astype(np.float32)
    t = min(0.55, (k - 1.0) * 1.6)
    return np.clip(c + (_WARM - c) * t, 0, 1).astype(np.float32)


def lum(c):
    """Luma of a colour or an (..., 3) array."""
    c = np.asarray(c, np.float32)
    return c[..., 0] * 0.299 + c[..., 1] * 0.587 + c[..., 2] * 0.114


def ramp(c, ks=(0.5, 0.72, 1.0, 1.2, 1.42)):
    """Tones of a colour from dark to light with the anime hue shift (cool shadow, warm light), as float arrays."""
    return [tone(c, k) for k in ks]


def smoothstep(a, b, x):
    t = np.clip((np.asarray(x, np.float32) - a) / float(b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


# ================================================================================================ blur (a Toeplitz matrix per size: faces are small)
_BLUR_CACHE = {}


def _blur_matrix(n, sigma):
    key = (n, round(float(sigma), 3))
    m = _BLUR_CACHE.get(key)
    if m is None:
        r = max(1, int(math.ceil(sigma * 3)))
        ks = np.arange(-r, r + 1)
        w = np.exp(-(ks.astype(np.float64) ** 2) / (2.0 * sigma * sigma))
        w /= w.sum()
        m = np.zeros((n, n), np.float32)
        for i in range(n):
            for k, wk in zip(ks, w):
                m[i, min(max(i + int(k), 0), n - 1)] += wk
        _BLUR_CACHE[key] = m
    return m


def blur(a, sigma):
    """Gaussian blur of an (H, W) or (H, W, C) float array, sigma in texels, edges replicated. Used per island, so it never mixes seams."""
    if sigma <= 0.05:
        return a
    a = np.asarray(a, np.float32)
    mv, mh = _blur_matrix(a.shape[0], sigma), _blur_matrix(a.shape[1], sigma)
    if a.ndim == 2:
        return mv @ a @ mh.T
    out = np.empty_like(a)
    for c in range(a.shape[2]):
        out[..., c] = mv @ a[..., c] @ mh.T
    return out


def shift(a, dy, dx):
    """a shifted by (dy, dx) texels with the border replicated (so nothing wraps and nothing goes empty at an island's edge)."""
    h, w = a.shape[:2]
    ys = np.clip(np.arange(h) - dy, 0, h - 1)
    xs = np.clip(np.arange(w) - dx, 0, w - 1)
    return a[ys][:, xs]


# ================================================================================================ paints
class _Stops:
    def __init__(self, stops):
        stops = sorted(stops, key=lambda s: s[0])
        self.t = np.array([s[0] for s in stops], np.float32)
        self.c = np.stack([col(s[1]) for s in stops]).astype(np.float32)

    def at(self, t):
        t = np.clip(np.asarray(t, np.float32), self.t[0], self.t[-1])
        return np.stack([np.interp(t, self.t, self.c[:, k]) for k in range(3)], axis=-1).astype(np.float32)


class lin:
    """A linear gradient between two points of the face (model pixels); stops = [(0, colour), (1, colour), ...]."""

    def __init__(self, p0, p1, stops):
        self.p0, self.p1, self.s = p0, p1, _Stops(stops)

    def at(self, X, Y):
        dx, dy = self.p1[0] - self.p0[0], self.p1[1] - self.p0[1]
        l2 = dx * dx + dy * dy or 1e-9
        return self.s.at(((X - self.p0[0]) * dx + (Y - self.p0[1]) * dy) / l2)


class rad:
    """A radial (elliptical) gradient: centre, radius or (rx, ry), optional rotation in degrees; stops over 0 (centre) .. 1 (the radius)."""

    def __init__(self, c, r, stops, rot=0.0):
        self.c = c
        self.r = (r, r) if np.ndim(r) == 0 else r
        self.rot = math.radians(rot)
        self.s = _Stops(stops)

    def at(self, X, Y):
        x, y = X - self.c[0], Y - self.c[1]
        cs, sn = math.cos(self.rot), math.sin(self.rot)
        u, v = (x * cs + y * sn) / self.r[0], (-x * sn + y * cs) / self.r[1]
        return self.s.at(np.sqrt(u * u + v * v))


# ================================================================================================ signed distance fields (model pixels) -> coverage
def cov(sd, f, soft=1.0):
    """Coverage of a signed distance (model pixels, negative inside) at f texels per pixel; soft = edge width in texels (1 = crisp anti-aliasing)."""
    return np.clip(0.5 - sd * f / max(0.5, soft), 0.0, 1.0).astype(np.float32)


def sd_segment(X, Y, x0, y0, x1, y1):
    dx, dy = x1 - x0, y1 - y0
    l2 = dx * dx + dy * dy or 1e-9
    t = np.clip(((X - x0) * dx + (Y - y0) * dy) / l2, 0.0, 1.0)
    return np.hypot(X - (x0 + t * dx), Y - (y0 + t * dy)), t


def sd_poly(X, Y, pts):
    """Signed distance to a polygon (even-odd inside = negative)."""
    n = len(pts)
    best = np.full(X.shape, 1e9, np.float32)
    inside = np.zeros(X.shape, bool)
    for i in range(n):
        x0, y0 = pts[i]
        x1, y1 = pts[(i + 1) % n]
        d, _ = sd_segment(X, Y, x0, y0, x1, y1)
        best = np.minimum(best, d)
        if y0 != y1:
            cross = ((y0 > Y) != (y1 > Y)) & (X < (x1 - x0) * (Y - y0) / (y1 - y0) + x0)
            inside ^= cross
    return np.where(inside, -best, best)


def sd_ellipse(X, Y, cx, cy, rx, ry, rot=0.0):
    r = math.radians(rot)
    cs, sn = math.cos(r), math.sin(r)
    x, y = X - cx, Y - cy
    u, v = (x * cs + y * sn), (-x * sn + y * cs)
    k = np.sqrt((u / rx) ** 2 + (v / ry) ** 2)
    return (k - 1.0) * min(rx, ry)


def sd_rect(X, Y, x0, y0, x1, y1, r=0.0):
    cx, cy = (x0 + x1) / 2.0, (y0 + y1) / 2.0
    hx, hy = (x1 - x0) / 2.0, (y1 - y0) / 2.0
    r = min(r, hx, hy)
    qx, qy = np.abs(X - cx) - (hx - r), np.abs(Y - cy) - (hy - r)
    return np.hypot(np.maximum(qx, 0), np.maximum(qy, 0)) + np.minimum(np.maximum(qx, qy), 0) - r


def union(*ms):
    """Union of coverage masks."""
    out = 1.0 - np.asarray(ms[0], np.float32)
    for m in ms[1:]:
        out = out * (1.0 - np.asarray(m, np.float32))
    return (1.0 - out).astype(np.float32)


def minus(a, b):
    return (np.asarray(a, np.float32) * (1.0 - np.asarray(b, np.float32))).astype(np.float32)


# ================================================================================================ the face (one UV island)
class Face:
    """One UV rectangle of the skin (and of the glow map). See the module doc for the coordinates."""

    def __init__(self, cv, part, name, overlay):
        self.cv, self.part, self.name, self.overlay = cv, part, name, bool(overlay)
        w, h, d = cv.dims(part)
        u, v = cv.uv(part, overlay)
        x0, y0, rw, rh = K.rects(u, v, w, h, d)[name]
        f = cv.f
        self.f = f
        self.w, self.h = rw, rh                      # model pixels
        self.x0, self.y0 = x0 * f, y0 * f            # texels
        self.W, self.H = rw * f, rh * f
        self.rgba = cv.px[self.y0:self.y0 + self.H, self.x0:self.x0 + self.W]       # views: drawing writes straight into the canvas
        self.glow = cv.gl[self.y0:self.y0 + self.H, self.x0:self.x0 + self.W]
        ys, xs = np.mgrid[0:self.H, 0:self.W]
        self.X = ((xs + 0.5) / f).astype(np.float32)
        self.Y = ((ys + 0.5) / f).astype(np.float32)
        self.light = LIGHT2D[name]
        self.lit = FACE_LIT[name]

    def __repr__(self):
        return "Face(%s %s%s %dx%d px @%d,%d)" % (self.part, self.name, "+" if self.overlay else "", self.w, self.h, self.x0 // self.f, self.y0 // self.f)

    # ------------------------------------------------------------------ masks
    def m_all(self):
        return np.ones((self.H, self.W), np.float32)

    def m_poly(self, pts, soft=1.0):
        return cov(sd_poly(self.X, self.Y, pts), self.f, soft)

    def m_ellipse(self, cx, cy, rx, ry=None, rot=0.0, soft=1.0):
        return cov(sd_ellipse(self.X, self.Y, cx, cy, rx, rx if ry is None else ry, rot), self.f, soft)

    def m_rect(self, x0, y0, x1, y1, r=0.0, soft=1.0):
        return cov(sd_rect(self.X, self.Y, x0, y0, x1, y1, r), self.f, soft)

    def m_line(self, p0, p1, w0=0.3, w1=None, soft=1.0):
        """A segment with round ends, width w0 at p0 tapering to w1 at p1 (model pixels)."""
        d, t = sd_segment(self.X, self.Y, p0[0], p0[1], p1[0], p1[1])
        w1 = w0 if w1 is None else w1
        return cov(d - (w0 + (w1 - w0) * t) / 2.0, self.f, soft)

    def polyline_sd(self, pts):
        """(distance, arc length along the line of the nearest point) for a polyline of points (model pixels)."""
        best = np.full((self.H, self.W), 1e9, np.float32)
        arc = np.zeros((self.H, self.W), np.float32)
        run = 0.0
        for i in range(len(pts) - 1):
            x0, y0 = pts[i]
            x1, y1 = pts[i + 1]
            d, t = sd_segment(self.X, self.Y, x0, y0, x1, y1)
            seg = math.hypot(x1 - x0, y1 - y0)
            better = d < best
            best = np.where(better, d, best)
            arc = np.where(better, run + t * seg, arc)
            run += seg
        return best, arc

    def m_polyline(self, pts, width=0.3, soft=1.0):
        d, _ = self.polyline_sd(pts)
        return cov(d - width / 2.0, self.f, soft)

    def m_ring(self, cx, cy, r, width=0.3, soft=1.0):
        return cov(np.abs(sd_ellipse(self.X, self.Y, cx, cy, r, r)) - width / 2.0, self.f, soft)

    def m_band(self, y0, y1, soft=1.0, x0=None, x1=None):
        """A horizontal band y0..y1 (optionally limited to x0..x1), edges `soft` texels wide."""
        sd = np.maximum(y0 - self.Y, self.Y - y1)
        if x0 is not None:
            sd = np.maximum(sd, np.maximum(x0 - self.X, self.X - x1))
        return cov(sd, self.f, soft)

    def m_alpha(self):
        """What is painted so far, as a mask (the alpha of the island)."""
        return self.rgba[..., 3].copy()

    def m_color(self, c, tol=0.12):
        """The texels that already have (about) this colour: where the paint is within `tol` (RGB distance, 0..1) of c, times their alpha."""
        d = np.sqrt(((self.rgba[..., :3] - col(c)) ** 2).sum(-1))
        return (np.clip(1.0 - d / tol, 0, 1) * self.rgba[..., 3]).astype(np.float32)

    # ------------------------------------------------------------------ paints
    def paint_rgb(self, paint):
        if paint is None:
            raise ValueError("a paint is needed")
        if isinstance(paint, np.ndarray) and paint.ndim == 3:
            return paint
        if hasattr(paint, "at"):
            return paint.at(self.X, self.Y)
        if callable(paint):
            return np.asarray(paint(self.X, self.Y), np.float32)
        return np.broadcast_to(col(paint), (self.H, self.W, 3))

    # ------------------------------------------------------------------ compositing
    def fill(self, mask, paint, a=1.0, mode="over", glow=None):
        """Composite `paint` through `mask`. mode: 'over' (default), 'erase' (cuts the alpha), 'mul' (multiplies the colour: shade), 'add' (adds light), 'under' (paints only where nothing is yet),
        'replace' (the colour is replaced, the alpha kept: recolour). glow: None, True (the same colour on the glow map) or a colour for the glow map (premultiplied at export)."""
        m = np.asarray(mask, np.float32) * float(a) * (alpha_of(paint) if isinstance(paint, (tuple, list)) else 1.0)
        if mode == "erase":
            self.rgba[..., 3] *= (1.0 - m)
            return self
        if not m.any():
            return self
        src = self.paint_rgb(paint)
        d = self.rgba
        da = d[..., 3]
        if mode == "over":
            oa = m + da * (1.0 - m)
            oc = (src * m[..., None] + d[..., :3] * (da * (1.0 - m))[..., None]) / np.maximum(oa, 1e-6)[..., None]
            d[..., :3] = np.where((oa > 1e-6)[..., None], oc, d[..., :3])
            d[..., 3] = oa
        elif mode == "under":
            ma = m * (1.0 - da)
            oa = ma + da
            oc = (src * ma[..., None] + d[..., :3] * da[..., None]) / np.maximum(oa, 1e-6)[..., None]
            d[..., :3] = np.where((oa > 1e-6)[..., None], oc, d[..., :3])
            d[..., 3] = oa
        elif mode == "mul":
            d[..., :3] = d[..., :3] * (1.0 + (src - 1.0) * m[..., None]) if src.ndim == 3 and src.shape[-1] == 3 else d[..., :3]
        elif mode == "add":
            d[..., :3] = np.clip(d[..., :3] + src * m[..., None], 0, 1)
        elif mode == "replace":
            d[..., :3] = d[..., :3] + (src - d[..., :3]) * m[..., None]
        else:
            raise ValueError("unknown mode " + mode)
        if glow is not None and glow is not False:
            self.glow_fill(m, paint if glow is True else glow)
        return self

    def shade(self, mask, k, a=1.0):
        """Multiplies the colour by k (scalar or (H, W) array) through the mask: k < 1 darkens, with a cool shift."""
        m = np.asarray(mask, np.float32) * float(a)
        kk = np.broadcast_to(np.asarray(k, np.float32), (self.H, self.W))
        d = self.rgba
        dark = np.clip(1.0 - kk, 0, 1)[..., None]
        light = np.clip(kk - 1.0, 0, 1)[..., None]
        c = d[..., :3]
        shaded = np.where(kk[..., None] < 1.0, c * kk[..., None] * (1 - 0.18 * dark) + _COOL * 0.10 * dark, c + (1 - c) * light * 0.75 + _WARM * 0.03 * light)
        d[..., :3] = np.clip(c + (shaded - c) * m[..., None], 0, 1)
        return self

    def glow_fill(self, mask, paint, a=1.0):
        """Light the glow map (straight alpha 'over'; made premultiplied by Canvas.finish)."""
        m = np.asarray(mask, np.float32) * float(a)
        if not m.any():
            return self
        src = self.paint_rgb(paint)
        g = self.glow
        oa = m + g[..., 3] * (1.0 - m)
        oc = (src * m[..., None] + g[..., :3] * (g[..., 3] * (1.0 - m))[..., None]) / np.maximum(oa, 1e-6)[..., None]
        g[..., :3] = np.where((oa > 1e-6)[..., None], oc, g[..., :3])
        g[..., 3] = oa
        return self

    # ------------------------------------------------------------------ shapes (mask + fill)
    def poly(self, pts, paint, a=1.0, soft=1.0, **kw):
        return self.fill(self.m_poly(pts, soft), paint, a, **kw)

    def ellipse(self, cx, cy, rx, ry=None, paint="#ffffff", rot=0.0, a=1.0, soft=1.0, **kw):
        return self.fill(self.m_ellipse(cx, cy, rx, ry, rot, soft), paint, a, **kw)

    def rect(self, x0, y0, x1, y1, paint, r=0.0, a=1.0, soft=1.0, **kw):
        return self.fill(self.m_rect(x0, y0, x1, y1, r, soft), paint, a, **kw)

    def line(self, p0, p1, paint, w0=0.3, w1=None, a=1.0, soft=1.0, **kw):
        return self.fill(self.m_line(p0, p1, w0, w1, soft), paint, a, **kw)

    def polyline(self, pts, paint, width=0.3, a=1.0, soft=1.0, **kw):
        return self.fill(self.m_polyline(pts, width, soft), paint, a, **kw)

    def clear(self, mask=None):
        """Erases (alpha 0) the whole island, or the masked part."""
        if mask is None:
            self.rgba[...] = 0.0
            self.glow[...] = 0.0
        else:
            self.fill(mask, "#000000", mode="erase")
            self.glow[..., 3] *= (1.0 - np.asarray(mask, np.float32))
        return self

    def set_pixels(self, rgba_img):
        """Replaces the island with a float image (H, W, 4) of its size (the bridge uses it)."""
        self.rgba[...] = rgba_img
        return self

    def mx(self, x):
        """The mirrored x of this island (for symmetric designs)."""
        return self.w - x


# ================================================================================================ the canvas
class Canvas:
    """The skin and the glow map at `size` texels (64, 128 or 256). Draw into `cv.face(...)`, then `skin, glow = cv.finish()`."""

    def __init__(self, size=256, slim=False):
        if size < 64 or size % 64 or (size // 64) & (size // 64 - 1):
            raise ValueError("size must be 64, 128, 256 ...")
        self.size, self.f, self.slim = size, size // 64, bool(slim)
        self.px = np.zeros((size, size, 4), np.float32)
        self.gl = np.zeros((size, size, 4), np.float32)
        self._faces = {}

    def dims(self, part):
        (w, h, d), _, _ = K.PARTS[part]
        if part.startswith("arm") and self.slim:
            w = 3
        return w, h, d

    def uv(self, part, overlay):
        _, b, o = K.PARTS[part]
        return o if overlay else b

    def face(self, part, name, overlay=False):
        key = (part, name, bool(overlay))
        fc = self._faces.get(key)
        if fc is None:
            fc = self._faces[key] = Face(self, part, name, overlay)
        return fc

    def faces(self, parts=None, names=FACES, overlay=(False, True)):
        """Iterates the islands: for part, for layer, for face."""
        for part in parts or PARTS:
            for ov in overlay:
                for nm in names:
                    yield self.face(part, nm, ov)

    def limbs(self, kind, overlay=False):
        """Both arms ('arm') or both legs ('leg')."""
        return (self.face_set(kind + "_r", overlay), self.face_set(kind + "_l", overlay))

    def face_set(self, part, overlay=False):
        return {nm: self.face(part, nm, overlay) for nm in FACES}

    # ------------------------------------------------------------------ export
    def finish(self):
        """-> (skin PIL RGBA, glow PIL RGBA or None). Overlay islands become fully opaque or fully transparent, base islands fully opaque where painted; the glow map is premultiplied."""
        px = np.nan_to_num(self.px, nan=0.0)
        for (part, name, ov), fc in self._faces.items():
            r = px[fc.y0:fc.y0 + fc.H, fc.x0:fc.x0 + fc.W]
            a = r[..., 3]
            keep = a >= 0.5                       # the game draws the second layer as a cutout (alpha >= 0.1 is opaque): no half-transparent texel is ever written
            r[..., 3] = np.where(keep, 1.0, 0.0)
            r[..., :3] = np.where(keep[..., None], r[..., :3], 0.0)
        skin = Image.fromarray((np.clip(px, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")
        g = self.gl
        if g[..., 3].max() <= 0.004:
            return skin, None
        out = np.zeros_like(g)
        a = np.clip(g[..., 3], 0, 1)
        out[..., :3] = np.clip(g[..., :3], 0, 1) * a[..., None]
        out[..., 3] = a
        glow = Image.fromarray((out * 255 + 0.5).astype(np.uint8), "RGBA")
        return skin, glow

    def skin_rgba(self):
        """The skin as it will be saved, as a uint8 array (for tests and previews)."""
        return np.asarray(self.finish()[0])


def to_array(img):
    """A PIL image or an array -> float32 RGBA (H, W, 4) in 0..1."""
    if isinstance(img, np.ndarray):
        a = img.astype(np.float32)
        return a / 255.0 if a.max() > 1.5 else a
    return np.asarray(img.convert("RGBA"), np.float32) / 255.0


def downsample(img, size):
    """A skin or glow PIL image at `size` (64 or 128 from 256): an exact area average per block of texels, which never crosses an island (their corners are on multiples of the block). The
    alpha is made binary again (>= 0.5), colour is averaged premultiplied."""
    a = to_array(img)
    n = a.shape[0]
    if size == n:
        return img
    if size > n or n % size:
        raise ValueError("can only downsample by a whole factor")
    k = n // size
    pre = a[..., :3] * a[..., 3:4]
    pre = pre.reshape(size, k, size, k, 3).mean((1, 3))
    al = a[..., 3].reshape(size, k, size, k).mean((1, 3))
    c = pre / np.maximum(al, 1e-6)[..., None]
    out = np.zeros((size, size, 4), np.float32)
    keep = al >= 0.5
    out[..., :3] = np.where(keep[..., None], c, 0.0)
    out[..., 3] = np.where(keep, 1.0, 0.0)
    return Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


def downsample_glow(img, size):
    """The same for a (premultiplied) glow map: plain area average of all four channels."""
    a = to_array(img)
    n = a.shape[0]
    if size == n:
        return img
    k = n // size
    out = a.reshape(size, k, size, k, 4).mean((1, 3))
    return Image.fromarray((np.clip(out, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")

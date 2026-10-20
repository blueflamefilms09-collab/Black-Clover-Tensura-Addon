"""Pixel-art toolkit for the custom particle library (tools/gen_custom_particles.py).

Everything is deterministic: shapes are signed-distance fields on a small canvas (16x16 or 32x32), shaded into a hand-picked colour ramp and
thresholded into hard pixels (a one pixel outline, a few alpha steps for glows), so the result reads as drawn pixel art, not as a blurred gradient.

Coordinates: the canvas is centred, x to the right, y DOWN, one unit = one texel, the centre of texel (i, j) is (i + 0.5 - n/2, j + 0.5 - n/2).
"""
import math

import numpy as np


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*hexes):
    return np.array([hexc(h) for h in hexes], np.float32)


class Canvas:
    def __init__(self, n):
        self.n = n
        j, i = np.mgrid[0:n, 0:n].astype(np.float32)
        self.x = i + 0.5 - n / 2.0
        self.y = j + 0.5 - n / 2.0
        self.r = np.sqrt(self.x ** 2 + self.y ** 2)
        self.a = np.arctan2(self.y, self.x)

    def rot(self, ang, cx=0.0, cy=0.0):
        """Coordinates rotated by -ang around (cx, cy): a shape drawn axis aligned in them is rotated by +ang on the canvas."""
        c, s = math.cos(ang), math.sin(ang)
        dx, dy = self.x - cx, self.y - cy
        return dx * c + dy * s, -dx * s + dy * c


# ---------------------------------------------------------------- signed distance shapes (negative inside)
def sd_disc(c, cx, cy, r):
    return np.sqrt((c.x - cx) ** 2 + (c.y - cy) ** 2) - r


def sd_ellipse(c, cx, cy, a, b, ang=0.0):
    x, y = c.rot(ang, cx, cy)
    q = np.sqrt((x / a) ** 2 + (y / b) ** 2)
    return (q - 1.0) * min(a, b)


def sd_capsule(c, x0, y0, x1, y1, w0, w1=None):
    """Segment with a width tapering from w0 (at p0) to w1 (at p1) (full widths)."""
    if w1 is None:
        w1 = w0
    px, py = c.x - x0, c.y - y0
    dx, dy = x1 - x0, y1 - y0
    L2 = dx * dx + dy * dy + 1e-9
    h = np.clip((px * dx + py * dy) / L2, 0, 1)
    d = np.sqrt((px - dx * h) ** 2 + (py - dy * h) ** 2)
    return d - (w0 + (w1 - w0) * h) * 0.5


def sd_poly(c, pts):
    """Exact distance to a closed polygon, negative inside."""
    pts = list(pts)
    n = len(pts)
    dmin = np.full(c.x.shape, 1e9, np.float32)
    inside = np.zeros(c.x.shape, bool)
    for k in range(n):
        x0, y0 = pts[k]
        x1, y1 = pts[(k + 1) % n]
        ex, ey = x1 - x0, y1 - y0
        px, py = c.x - x0, c.y - y0
        h = np.clip((px * ex + py * ey) / (ex * ex + ey * ey + 1e-9), 0, 1)
        d = np.sqrt((px - ex * h) ** 2 + (py - ey * h) ** 2)
        dmin = np.minimum(dmin, d)
        cond = ((y0 <= c.y) & (c.y < y1)) | ((y1 <= c.y) & (c.y < y0))
        xin = x0 + (c.y - y0) * ex / (ey if abs(ey) > 1e-9 else 1e-9)
        inside ^= cond & (c.x < xin)
    return np.where(inside, -dmin, dmin)


def sd_star(c, cx, cy, n, r_out, r_in, ang=0.0):
    pts = []
    for k in range(2 * n):
        a = ang + k * math.pi / n - math.pi / 2
        r = r_out if k % 2 == 0 else r_in
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return sd_poly(c, pts)


def sd_ring(sd, w):
    return np.abs(sd) - w * 0.5


def union(*s):
    out = s[0]
    for o in s[1:]:
        out = np.minimum(out, o)
    return out


def inter(a, b):
    return np.maximum(a, b)


def subtract(a, b):
    return np.maximum(a, -b)


def cov(sd):
    """Anti-aliased coverage in [0, 1] from a signed distance."""
    return np.clip(0.5 - sd, 0.0, 1.0)


# ---------------------------------------------------------------- noise
def vnoise(n, cells, rng, octaves=3):
    """Value noise in [0, 1], n x n, smooth, deterministic from rng."""
    out = np.zeros((n, n), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        k = cells * (2 ** o)
        g = rng.random((k + 2, k + 2)).astype(np.float32)
        xs = (np.arange(n) + 0.5) / n * k
        i0 = np.floor(xs).astype(int)
        f = xs - i0
        f = f * f * (3 - 2 * f)
        a = g[np.ix_(i0, i0)]
        b = g[np.ix_(i0, i0 + 1)]
        cc = g[np.ix_(i0 + 1, i0)]
        d = g[np.ix_(i0 + 1, i0 + 1)]
        fx = f[None, :]
        fy = f[:, None]
        out += amp * ((a * (1 - fx) + b * fx) * (1 - fy) + (cc * (1 - fx) + d * fx) * fy)
        tot += amp
        amp *= 0.5
    return out / tot


def lumpy(c, rng, cells=3, octaves=2):
    """A noise field in [-1, 1] on the canvas, for lumpy outlines."""
    return vnoise(c.n, cells, rng, octaves) * 2 - 1


# ---------------------------------------------------------------- rendering into pixels
def _idx(shade, nramp):
    return np.clip(np.floor(shade * nramp), 0, nramp - 1).astype(int)


def solid(c, sd, shade, rmp, outline=None, outline_alpha=255, hard=True):
    """A solid shape: texels whose centre is inside get the ramp colour picked by `shade` in [0, 1]; a one texel outline around it."""
    n = c.n
    out = np.zeros((n, n, 4), np.float32)
    inside = sd < 0.0 if hard else sd < 0.15
    k = _idx(np.broadcast_to(shade, (n, n)), len(rmp))
    col = rmp[k]
    out[inside, :3] = col[inside]
    out[inside, 3] = 255
    if outline is not None:
        edge = ~inside
        nb = np.zeros((n, n), bool)
        nb[1:, :] |= inside[:-1, :]
        nb[:-1, :] |= inside[1:, :]
        nb[:, 1:] |= inside[:, :-1]
        nb[:, :-1] |= inside[:, 1:]
        e = edge & nb
        oc = np.array(hexc(outline) if isinstance(outline, str) else outline, np.float32)
        out[e, :3] = oc
        out[e, 3] = outline_alpha
    return out


def glow(c, inten, rmp, levels=4, amax=255, floor=0.08, gamma=1.0):
    """A soft shape: `inten` in [0, 1] is quantised into `levels` alpha steps and picks the ramp colour (bright = late in the ramp)."""
    n = c.n
    v = np.clip(np.broadcast_to(inten, (n, n)), 0, 1) ** gamma
    q = np.floor(v * levels + 0.5) / levels
    q[v < floor] = 0
    out = np.zeros((n, n, 4), np.float32)
    k = _idx(v, len(rmp))
    out[..., :3] = rmp[k]
    out[..., 3] = q * amax
    return out


def over(*layers):
    """Alpha-composite premultiplied-free RGBA float layers, first = bottom."""
    base = layers[0].copy()
    for top in layers[1:]:
        a_t = top[..., 3:4] / 255.0
        a_b = base[..., 3:4] / 255.0
        a_o = a_t + a_b * (1 - a_t)
        rgb = (top[..., :3] * a_t + base[..., :3] * a_b * (1 - a_t)) / np.maximum(a_o, 1e-6)
        base[..., :3] = rgb
        base[..., 3:4] = a_o * 255.0
    return base


def add_px(img, pts, colour, alpha=255):
    """Set single texels (pixel-art highlights)."""
    n = img.shape[0]
    col = np.array(hexc(colour) if isinstance(colour, str) else colour, np.float32)
    for (i, j) in pts:
        if 0 <= i < n and 0 <= j < n:
            img[j, i, :3] = col
            img[j, i, 3] = alpha
    return img


def blank(n):
    return np.zeros((n, n, 4), np.float32)


def lerp(a, b, t):
    return a + (b - a) * t


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a + 1e-9), 0, 1)
    return t * t * (3 - 2 * t)


def to_uint8(img):
    return np.clip(np.rint(img), 0, 255).astype(np.uint8)

"""Painting helpers of the 3.26.0 Skill Book textures (numpy + PIL): noise, materials, relief lighting, ramps, drawn masks.

WHY a library: eleven covers and five trims that share one painting method ("height map, then light it from the top left, then paint the wear and the highlights over it") are the same ten
functions with different numbers; by hand they would drift apart. Everything is deterministic (seeded generators, no clock), so --check can compare pixels.

Conventions: an image is a float32 array (h, w, 3) in 0..255 or an (h, w) mask in 0..1. The light comes from the top left (the same side as the item lighting of the game's GUI).
"""
import math

import numpy as np
from PIL import Image, ImageDraw


# ---------------------------------------------------------------------------------------------------------------------------- basics
def gauss(a, sigma):
    """Separable gaussian blur of a (h, w) or (h, w, c) float array, edges replicated."""
    if sigma <= 0.05:
        return a
    r = max(1, int(sigma * 3))
    k = np.exp(-(np.arange(-r, r + 1) ** 2) / (2 * sigma * sigma)).astype(np.float32)
    k /= k.sum()
    out = a.astype(np.float32)
    for axis in (0, 1):
        pad = [(0, 0)] * out.ndim
        pad[axis] = (r, r)
        p = np.pad(out, pad, mode="edge")
        acc = np.zeros_like(out)
        n = out.shape[axis]
        for i, kv in enumerate(k):
            sl = [slice(None)] * out.ndim
            sl[axis] = slice(i, i + n)
            acc += kv * p[tuple(sl)]
        out = acc
    return out


def resize_f(a, w, h):
    """Smooth resize of a float (h, w) array (PIL mode F)."""
    return np.asarray(Image.fromarray(a.astype(np.float32), "F").resize((w, h), Image.BICUBIC), np.float32)


def value_noise(h, w, rng, cell):
    small = rng.random((h // cell + 3, w // cell + 3)).astype(np.float32)
    big = resize_f(small, (w // cell + 3) * cell, (h // cell + 3) * cell)
    return big[:h, :w]


def fbm(h, w, rng, cells=(32, 16, 8, 4), decay=0.55):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for c in cells:
        out += amp * value_noise(h, w, rng, c)
        tot += amp
        amp *= decay
    out /= tot
    lo, hi = out.min(), out.max()
    return (out - lo) / max(1e-6, hi - lo)


def ramp(t, stops):
    """Maps t in 0..1 (array) to an RGB array through evenly spaced colour stops."""
    stops = np.array(stops, np.float32)
    n = len(stops) - 1
    x = np.clip(t, 0, 1) * n
    i = np.minimum(x.astype(int), n - 1)
    f = (x - i)[..., None]
    return stops[i] * (1 - f) + stops[i + 1] * f


def mix(a, b, m):
    m = m[..., None] if m.ndim == a.ndim - 1 else m
    return a * (1 - m) + b * m


def rgb(c):
    return np.array(c, np.float32)


# ---------------------------------------------------------------------------------------------------------------------------- drawn masks
class Mask:
    """A supersampled PIL canvas: draw with the 4x coordinates helpers, read back as a float mask."""

    def __init__(self, w, h, ss=4):
        self.w, self.h, self.ss = w, h, ss
        self.img = Image.new("L", (w * ss, h * ss), 0)
        self.d = ImageDraw.Draw(self.img)

    def S(self, v):
        return v * self.ss

    def poly(self, pts, fill=255):
        self.d.polygon([(x * self.ss, y * self.ss) for x, y in pts], fill=fill)

    def ellipse(self, x0, y0, x1, y1, fill=255, outline=None, width=1):
        s = self.ss
        self.d.ellipse((x0 * s, y0 * s, x1 * s, y1 * s), fill=fill, outline=outline, width=int(width * s) if outline is not None else 0)

    def rect(self, x0, y0, x1, y1, fill=255):
        s = self.ss
        self.d.rectangle((x0 * s, y0 * s, x1 * s, y1 * s), fill=fill)

    def line(self, pts, width=1.0, fill=255):
        s = self.ss
        self.d.line([(x * s, y * s) for x, y in pts], fill=fill, width=max(1, int(width * s)), joint="curve")

    def brush(self, pts, r0, r1=None, fill=255):
        """A stroke of discs with a radius running from r0 to r1 (a tapered brush stroke)."""
        r1 = r0 if r1 is None else r1
        s = self.ss
        n = len(pts)
        for i, (x, y) in enumerate(pts):
            r = (r0 + (r1 - r0) * i / max(1, n - 1)) * s
            self.d.ellipse((x * s - r, y * s - r, x * s + r, y * s + r), fill=fill)

    def arc(self, x0, y0, x1, y1, a0, a1, width=1.0, fill=255):
        s = self.ss
        self.d.arc((x0 * s, y0 * s, x1 * s, y1 * s), a0, a1, fill=fill, width=max(1, int(width * s)))

    def get(self):
        return np.asarray(self.img.resize((self.w, self.h), Image.BOX), np.float32) / 255.0


def bezier(p0, p1, p2, p3, n=48):
    out = []
    for i in range(n + 1):
        t = i / n
        a, b, c, e = (1 - t) ** 3, 3 * (1 - t) ** 2 * t, 3 * (1 - t) * t ** 2, t ** 3
        out.append((a * p0[0] + b * p1[0] + c * p2[0] + e * p3[0], a * p0[1] + b * p1[1] + c * p2[1] + e * p3[1]))
    return out


def spiral(cx, cy, r0, r1, turns, a0=0.0, n=80, flip=1):
    pts = []
    for i in range(n + 1):
        t = i / n
        a = a0 + flip * t * turns * 2 * math.pi
        r = r0 + (r1 - r0) * t
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


def heart(cx, cy, size, rot=0.0, n=48):
    pts = []
    for i in range(n):
        t = i / n * 2 * math.pi
        x = 16 * math.sin(t) ** 3
        y = -(13 * math.cos(t) - 5 * math.cos(2 * t) - 2 * math.cos(3 * t) - math.cos(4 * t))
        c, s = math.cos(rot), math.sin(rot)
        pts.append((cx + size * (x * c - y * s), cy + size * (x * s + y * c)))
    return pts


def star(cx, cy, r_out, r_in, n=5, rot=-math.pi / 2):
    pts = []
    for i in range(n * 2):
        r = r_out if i % 2 == 0 else r_in
        a = rot + i * math.pi / n
        pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return pts


# ---------------------------------------------------------------------------------------------------------------------------- lighting
LIGHT = (-0.62, -0.78)          # towards the light, in image x, y (top left)


def slope_light(height, strength=1.0):
    """Lambert-ish term in about -1..1 from a height map: positive where the surface faces the light (top left)."""
    gy, gx = np.gradient(height.astype(np.float32))
    return np.clip(-(gx * LIGHT[0] + gy * LIGHT[1]) * strength, -1.5, 1.5)


def cavity(height, sigma=3.0):
    """Positive in hollows: where the height is below its own blurred surroundings (ambient occlusion by cheap difference)."""
    return np.clip(gauss(height, sigma) - height, 0, 4)


def lit(color, height, k=0.55, spec=0.0, spec_color=(255, 244, 214), ao=0.0, ao_sigma=3.0, strength=1.0):
    """Colour (h,w,3) lit by its height map: diffuse from the slope, optional specular glint on the lit slopes, optional cavity darkening."""
    L = slope_light(height, strength)
    out = color * (1.0 + k * L)[..., None]
    if spec > 0:
        out += (np.clip(L - 0.35, 0, 1) ** 1.5 * spec * 255.0)[..., None] * (rgb(spec_color) / 255.0)
    if ao > 0:
        out *= (1.0 - np.clip(cavity(height, ao_sigma) * ao, 0, 0.7))[..., None]
    return out


def relief_layer(mask, color, bevel=1.3, k=0.7, shadow=0.38, shadow_off=(1.4, 1.8), spec=0.25, spec_color=(255, 244, 214), rough=None):
    """A raised ornament as an RGBA layer: the colour lit by the bevelled mask, with a drop shadow (dark, translucent, offset towards the bottom right) under it. mask: (h, w) 0..1."""
    h, w = mask.shape
    height = gauss(mask, bevel) * 4.0
    if rough is not None:
        height = height + rough * 0.5 * mask
    rgb_ = lit(color, height, k=k, spec=spec, spec_color=spec_color, ao=0.25, ao_sigma=1.6)
    a = np.clip(mask * 1.0, 0, 1)
    if shadow > 0:
        sh = np.roll(np.roll(gauss(mask, 1.1), int(round(shadow_off[1])), 0), int(round(shadow_off[0])), 1)
        sh_a = np.clip(sh * shadow, 0, 1) * (1 - a)
        out_a = a + sh_a
        out_rgb = (rgb_ * a[..., None]) / np.maximum(out_a[..., None], 1e-4) * (a / np.maximum(out_a, 1e-4))[..., None] + np.zeros_like(rgb_)
        out_rgb = np.where(out_a[..., None] > 1e-4, (rgb_ * a[..., None] + np.array([8, 4, 6], np.float32) * sh_a[..., None]) / np.maximum(out_a[..., None], 1e-4), 0)
    else:
        out_a, out_rgb = a, rgb_
    rgba = np.zeros((h, w, 4), np.float32)
    rgba[..., :3] = np.clip(out_rgb, 0, 255)
    rgba[..., 3] = np.clip(out_a, 0, 1) * 255.0
    return rgba


def glow_layer(mask, color, core=1.0, halo_sigma=3.0, halo=0.55):
    """An emissive RGBA layer: a bright core on the mask and a soft halo round it (the halo is translucent, so the cover shows through)."""
    h, w = mask.shape
    hal = np.clip(gauss(mask, halo_sigma) * 1.6, 0, 1) * halo
    a = np.clip(np.maximum(mask * core, hal), 0, 1)
    c = rgb(color)
    white = np.clip(mask, 0, 1)[..., None]
    col = c[None, None, :] * (1 - 0.45 * white) + 255.0 * 0.45 * white
    rgba = np.zeros((h, w, 4), np.float32)
    rgba[..., :3] = np.clip(col, 0, 255)
    rgba[..., 3] = a * 255.0
    return rgba


def composite(base_rgb, layer_rgba):
    a = (layer_rgba[..., 3:4] / 255.0)
    return base_rgb * (1 - a) + layer_rgba[..., :3] * a


def to_u8(a):
    return np.clip(a, 0, 255).astype(np.uint8)


def edge_dist(h, w):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))


def wear(h, w, rng, color, width=6.0, amount=0.75):
    """Rubbed edges and corners: a noisy band along the border that lerps the colour towards `color` (a lighter, drier leather or exposed board)."""
    d = edge_dist(h, w)
    n = fbm(h, w, rng, (12, 6, 3), 0.6)
    band = np.clip(1.0 - d / width, 0, 1) * np.clip((n - 0.35) * 2.2, 0, 1)
    return np.clip(band * amount, 0, 1)


def voronoi(h, w, rng, n, margin=0):
    pts = np.stack([rng.random(n) * (w + 2 * margin) - margin, rng.random(n) * (h + 2 * margin) - margin], 1).astype(np.float32)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    d = np.sqrt((xx[..., None] - pts[:, 0]) ** 2 + (yy[..., None] - pts[:, 1]) ** 2)
    idx = np.argsort(d, axis=2)
    d1 = np.take_along_axis(d, idx[..., :1], 2)[..., 0]
    d2 = np.take_along_axis(d, idx[..., 1:2], 2)[..., 0]
    return d1, d2, idx[..., 0]


def dashed_line_mask(h, w, pts, dash=4, gap=3, width=1.2):
    """A stitched seam along a polyline."""
    m = Mask(w, h)
    for (x0, y0), (x1, y1) in zip(pts[:-1], pts[1:]):
        L = math.hypot(x1 - x0, y1 - y0)
        n = int(L // (dash + gap))
        for i in range(n + 1):
            a = i * (dash + gap) / L
            b = min(1.0, (i * (dash + gap) + dash) / L)
            m.line([(x0 + (x1 - x0) * a, y0 + (y1 - y0) * a), (x0 + (x1 - x0) * b, y0 + (y1 - y0) * b)], width)
    return m.get()

"""Drawing helpers of the custom boss bar art (tools/gen_boss_bar_textures.py). Everything is drawn in GUI units (the frame is
256x42 GUI pixels, stored at 2x = 512x84) on a supersampled canvas and scaled down with a box filter, so edges are smooth and the
output is bit-for-bit deterministic. No randomness except random.Random(seed) objects created by the callers."""
import math
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SS = 4                       # canvas pixels per GUI unit while drawing (final texture: 2 px per GUI unit)
OUT_SCALE = 2                # texture pixels per GUI unit

# geometry shared with the Java side (BarLayoutMath) and the previewer; _index.json carries it so a test can compare
W, H = 256, 42
WIN = (38, 25, 196, 10)      # x, y, w, h of the fill window
SOCK = (18, 24, 15)          # cx, cy, inner radius of the socket (portrait / emblem)
PLATE = (40, 2, 192, 17)     # x, y, w, h of the name plate


def rgb(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, k):
    """k < 1 darkens, k > 1 lightens toward white."""
    if k <= 1:
        return tuple(int(c[i] * k) for i in range(3))
    return mix(c, (255, 255, 255), min(1.0, k - 1))


def _col(c):
    return tuple(c) + (255,) if len(c) == 3 else tuple(c)


class Canvas:
    """One RGBA layer of the atlas, drawn in GUI units."""

    def __init__(self, w=W, h=H):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    def P(self, pts):
        return [(x * SS, y * SS) for x, y in pts]

    # ---- plain shapes (opaque or erase)
    def poly(self, pts, fill, outline=None, width=0.5):
        self.d.polygon(self.P(pts), fill=_col(fill))
        if outline is not None:
            self.d.line(self.P(list(pts) + [pts[0]]), fill=_col(outline), width=max(1, int(width * SS)), joint="curve")

    def line(self, pts, fill, width=1.0):
        self.d.line(self.P(pts), fill=_col(fill), width=max(1, int(width * SS)), joint="curve")

    def rect(self, x0, y0, x1, y1, fill):
        self.d.rectangle([x0 * SS, y0 * SS, x1 * SS - 1, y1 * SS - 1], fill=_col(fill))

    def ell(self, cx, cy, rx, ry, fill, outline=None, width=0.5):
        bb = [(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS]
        self.d.ellipse(bb, fill=_col(fill), outline=_col(outline) if outline else None, width=max(1, int(width * SS)) if outline else 0)

    def erase_rect(self, x0, y0, x1, y1):
        self.d.rectangle([x0 * SS, y0 * SS, x1 * SS - 1, y1 * SS - 1], fill=(0, 0, 0, 0))

    def erase_ell(self, cx, cy, rx, ry):
        self.d.ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=(0, 0, 0, 0))

    # ---- gradients and glows
    def grad_poly(self, pts, top, bottom, y0=None, y1=None):
        ys = [p[1] for p in pts]
        y0 = min(ys) if y0 is None else y0
        y1 = max(ys) if y1 is None else y1
        mask = Image.new("L", self.im.size, 0)
        ImageDraw.Draw(mask).polygon(self.P(pts), fill=255)
        self._grad_paste(mask, top, bottom, y0, y1)

    def grad_rect(self, x0, y0, x1, y1, top, bottom):
        self.grad_poly([(x0, y0), (x1, y0), (x1, y1), (x0, y1)], top, bottom)

    def grad_ell(self, cx, cy, rx, ry, top, bottom):
        mask = Image.new("L", self.im.size, 0)
        ImageDraw.Draw(mask).ellipse([(cx - rx) * SS, (cy - ry) * SS, (cx + rx) * SS, (cy + ry) * SS], fill=255)
        self._grad_paste(mask, top, bottom, cy - ry, cy + ry)

    def _grad_paste(self, mask, top, bottom, y0, y1):
        w, h = self.im.size
        t = np.clip((np.arange(h) / SS - y0) / max(1e-6, (y1 - y0)), 0, 1)[:, None, None]
        top_a = np.array(top[:3], float)[None, None, :]
        bot_a = np.array(bottom[:3], float)[None, None, :]
        arr = np.broadcast_to(top_a + (bot_a - top_a) * t, (h, w, 3)).astype(np.uint8)
        g = Image.fromarray(np.dstack([arr, np.full((h, w), 255, np.uint8)]), "RGBA")
        self.im.paste(g, (0, 0), mask)

    def glow(self, draw_fn, color, radius=2.0, alpha=1.0):
        """A soft halo: draw_fn(Canvas) paints shapes on a scratch layer, which is blurred, tinted and composited."""
        tmp = Canvas(self.w, self.h)
        draw_fn(tmp)
        a = tmp.im.split()[3].filter(ImageFilter.GaussianBlur(radius * SS))
        a = a.point(lambda v: min(255, int(v * alpha * 1.6)))
        col = Image.new("RGBA", self.im.size, tuple(color) + (0,))
        col.putalpha(a)
        self.im.alpha_composite(col)

    def mask_alpha(self, fn_mask):
        """Multiply the layer's alpha by a mask drawn by fn_mask(Canvas) (any opaque pixel = keep)."""
        tmp = Canvas(self.w, self.h)
        fn_mask(tmp)
        a = np.array(self.im.split()[3], np.uint16) * np.array(tmp.im.split()[3], np.uint16) // 255
        r, g, b, _ = self.im.split()
        self.im = Image.merge("RGBA", (r, g, b, Image.fromarray(a.astype(np.uint8))))
        self.d = ImageDraw.Draw(self.im)

    def final(self):
        return self.im.resize((self.w * OUT_SCALE, self.h * OUT_SCALE), Image.BOX)


# ---------------------------------------------------------------- small shape generators (all draw on a Canvas)
def spikes(c, pts, length, width, color, outline=None, side=-1, seed=0, jitter=0.0):
    """Triangles along the polyline 'pts', pointing to the 'side' normal (-1 / +1)."""
    rnd = random.Random(seed)
    for i in range(len(pts) - 1):
        (x0, y0), (x1, y1) = pts[i], pts[i + 1]
        dx, dy = x1 - x0, y1 - y0
        ln = math.hypot(dx, dy)
        if ln == 0:
            continue
        nx, ny = side * dy / ln, -side * dx / ln
        n = max(1, int(ln // width))
        for k in range(n):
            t0, t1 = k / n, (k + 1) / n
            tm = (t0 + t1) / 2
            L = length * (1 + (rnd.random() - 0.5) * jitter)
            bx0, by0, bx1, by1 = x0 + dx * t0, y0 + dy * t0, x0 + dx * t1, y0 + dy * t1
            tip = (x0 + dx * tm + nx * L, y0 + dy * tm + ny * L)
            c.poly([(bx0, by0), tip, (bx1, by1)], color, outline)


def leaf(c, x, y, ang, ln, wd, color, vein=None, outline=None):
    ca, sa = math.cos(ang), math.sin(ang)
    pts = []
    for t in np.linspace(0, 1, 9):
        w = math.sin(math.pi * t) * wd / 2
        pts.append((x + ca * ln * t - sa * w, y + sa * ln * t + ca * w))
    for t in np.linspace(1, 0, 9):
        w = math.sin(math.pi * t) * wd / 2
        pts.append((x + ca * ln * t + sa * w, y + sa * ln * t - ca * w))
    c.poly(pts, color, outline)
    if vein:
        c.line([(x, y), (x + ca * ln * 0.92, y + sa * ln * 0.92)], vein, 0.3)


def gear(c, cx, cy, r, teeth, color, hole, outline=None, rot=0.0):
    pts = []
    for i in range(teeth):
        a = rot + 2 * math.pi * i / teeth
        da = math.pi / teeth
        for aa, rr in ((a - da * 0.55, r * 0.8), (a - da * 0.3, r), (a + da * 0.3, r), (a + da * 0.55, r * 0.8)):
            pts.append((cx + math.cos(aa) * rr, cy + math.sin(aa) * rr))
    c.poly(pts, color, outline)
    c.ell(cx, cy, r * 0.4, r * 0.4, hole, outline)


def drop(c, x, y, s, color, outline=None):
    pts = [(x, y - s)]
    for t in np.linspace(-0.92, 0.92, 13):
        a = math.pi / 2 + t * math.pi * 0.92
        pts.append((x + math.cos(a) * s * 0.62, y + s * 0.2 + math.sin(a) * s * 0.62))
    c.poly(pts, color, outline)


def star(c, cx, cy, r, color, points=4, inner=0.35):
    pts = []
    for i in range(points * 2):
        a = -math.pi / 2 + math.pi * i / points
        rr = r if i % 2 == 0 else r * inner
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    c.poly(pts, color)


def bolt(c, x0, y0, x1, y1, color, width=0.8, seed=0, segs=6, amp=2.2):
    rnd = random.Random(seed)
    pts = [(x0, y0)]
    for i in range(1, segs):
        t = i / segs
        pts.append((x0 + (x1 - x0) * t + (rnd.random() - 0.5) * amp * 2, y0 + (y1 - y0) * t + (rnd.random() - 0.5) * amp * 2))
    pts.append((x1, y1))
    c.line(pts, color, width)


def flame(c, x, y, h, w, color, inner=None):
    pts = [(x - w / 2, y), (x - w * 0.42, y - h * 0.35), (x - w * 0.12, y - h * 0.6), (x, y - h), (x + w * 0.16, y - h * 0.55), (x + w * 0.46, y - h * 0.3), (x + w / 2, y)]
    c.poly(pts, color)
    if inner:
        c.poly([(x - w * 0.24, y), (x - w * 0.08, y - h * 0.4), (x + w * 0.04, y - h * 0.62), (x + w * 0.2, y - h * 0.28), (x + w * 0.26, y)], inner)


def gem(c, cx, cy, r, base, hi, outline):
    c.ell(cx, cy, r, r, outline)
    c.grad_ell(cx, cy, r * 0.82, r * 0.82, hi, base)
    c.ell(cx - r * 0.28, cy - r * 0.3, r * 0.22, r * 0.18, (255, 255, 255))


def skull(c, cx, cy, s, color, dark):
    c.ell(cx, cy - s * 0.1, s * 0.62, s * 0.58, color)
    c.rect(cx - s * 0.34, cy + s * 0.2, cx + s * 0.34, cy + s * 0.62, color)
    c.ell(cx - s * 0.26, cy - s * 0.05, s * 0.17, s * 0.2, dark)
    c.ell(cx + s * 0.26, cy - s * 0.05, s * 0.17, s * 0.2, dark)
    c.poly([(cx, cy + s * 0.12), (cx - s * 0.08, cy + s * 0.3), (cx + s * 0.08, cy + s * 0.3)], dark)
    for k in (-0.2, 0, 0.2):
        c.line([(cx + k * s, cy + s * 0.42), (cx + k * s, cy + s * 0.62)], dark, 0.12 * s)


def horn(c, cx, cy, s, color, dark, mirror=1):
    """A horn curling outward and up from (cx, cy); mirror = -1 for the left one."""
    pts_o, pts_i = [], []
    for t in np.linspace(0, 1, 14):
        x = cx + mirror * (t * s * 1.0 + 0.35 * s * math.sin(t * 2.2))
        y = cy - (t ** 1.6) * s * 1.3
        w = s * 0.2 * (1 - t) + 0.02 * s
        pts_o.append((x + mirror * w * 0.3, y - w))
        pts_i.append((x - mirror * w * 0.3, y + w))
    c.poly(pts_o + pts_i[::-1], color, dark, 0.3)


def rune(c, x, y, s, color, seed=0, width=0.5):
    rnd = random.Random(seed)
    c.line([(x, y - s), (x, y + s)], color, width)
    for _ in range(2):
        a, b = rnd.random() * 2 - 1, rnd.random() * 2 - 1
        c.line([(x, y + a * s), (x + (1 if rnd.random() > 0.5 else -1) * s * 0.7, y + b * s)], color, width)


def save_png(im, path):
    im.save(path, optimize=True)

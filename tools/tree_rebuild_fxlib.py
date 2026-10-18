"""Sprite helper of the nature-magic rebuild (Tree, Vine, Plant, Poison Plant, Mud, Mucus): a supersampled RGBA canvas with shapes, gradients, glows and noise, and the writer
into src/main/resources/assets/nusmp/textures/particle. The generators gen_<key>_rebuild_textures.py import this file. Deterministic (fixed seeds). Run python as `python3 -B`.

    from tree_rebuild_fxlib import *
    cv = Canvas(64, 64)
    cv.fill(poly_pts, lambda X, Y: grad(Y, 0, 1, (40, 120, 40, 255), (150, 230, 90, 255)))
    cv.glow(32, 32, 20, (255, 220, 120), 0.8)
    cv.save("tree_mote")
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def clamp01(a):
    return np.clip(a, 0.0, 1.0)


def grad(t, t0, t1, c0, c1):
    """Colour array (h, w, 4) mixing c0 -> c1 as t goes t0 -> t1 (t is an (h, w) array of coordinates)."""
    k = clamp01((t - t0) / float(t1 - t0 if t1 != t0 else 1))[..., None]
    return np.array(c0, dtype=np.float64) * (1 - k) + np.array(c1, dtype=np.float64) * k


def vnoise(w, h, scale, seed):
    """Smooth value noise 0..1 of shape (h, w) with the given feature size in pixels."""
    rng = np.random.RandomState(seed)
    gw, gh = int(w / scale) + 3, int(h / scale) + 3
    g = rng.rand(gh, gw)
    ys, xs = np.mgrid[0:h, 0:w]
    fx, fy = xs / float(scale), ys / float(scale)
    x0, y0 = fx.astype(int), fy.astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a, b, c, d = g[y0, x0], g[y0, x0 + 1], g[y0 + 1, x0], g[y0 + 1, x0 + 1]
    return a * (1 - tx) * (1 - ty) + b * tx * (1 - ty) + c * (1 - tx) * ty + d * tx * ty


class Canvas:
    """A w x h sprite drawn at SS times the size and shrunk on save."""

    def __init__(self, w, h, ss=SS):
        self.w, self.h, self.ss = w, h, ss
        self.W, self.H = w * ss, h * ss
        self.im = Image.new("RGBA", (self.W, self.H), (0, 0, 0, 0))
        ys, xs = np.mgrid[0:self.H, 0:self.W]
        self.X, self.Y = xs / float(ss), ys / float(ss)           # pixel coordinates of every supersample, in sprite pixels

    # ---- layers
    def _over(self, layer):
        self.im = Image.alpha_composite(self.im, layer)

    def _pts(self, pts):
        return [(x * self.ss, y * self.ss) for x, y in pts]

    def mask_poly(self, pts):
        m = Image.new("L", (self.W, self.H), 0)
        ImageDraw.Draw(m).polygon(self._pts(pts), fill=255)
        return np.asarray(m, dtype=np.float64) / 255.0

    def mask_ellipse(self, cx, cy, rx, ry):
        m = Image.new("L", (self.W, self.H), 0)
        s = self.ss
        ImageDraw.Draw(m).ellipse([(cx - rx) * s, (cy - ry) * s, (cx + rx) * s, (cy + ry) * s], fill=255)
        return np.asarray(m, dtype=np.float64) / 255.0

    def mask_line(self, pts, width):
        m = Image.new("L", (self.W, self.H), 0)
        d = ImageDraw.Draw(m)
        d.line(self._pts(pts), fill=255, width=max(1, int(width * self.ss)), joint="curve")
        r = width * self.ss / 2.0
        for x, y in self._pts([pts[0], pts[-1]]):
            d.ellipse([x - r, y - r, x + r, y + r], fill=255)
        return np.asarray(m, dtype=np.float64) / 255.0

    def paint(self, mask, color, blur=0.0):
        """Composites `color` (an RGBA tuple or an (H, W, 4) array) through `mask` (0..1 array)."""
        if blur > 0:
            mask = np.asarray(Image.fromarray((mask * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(blur * self.ss)), dtype=np.float64) / 255.0
        col = np.broadcast_to(np.array(color, dtype=np.float64), (self.H, self.W, 4)) if np.ndim(color) == 1 else color
        a = (col[..., 3] / 255.0 * mask)[..., None]
        lay = np.concatenate([col[..., :3], a * 255.0], axis=-1)
        self._over(Image.fromarray(np.clip(lay, 0, 255).astype(np.uint8), "RGBA"))

    # ---- shapes
    def poly(self, pts, color, blur=0.0):
        self.paint(self.mask_poly(pts), color, blur)

    def ellipse(self, cx, cy, rx, ry, color, blur=0.0):
        self.paint(self.mask_ellipse(cx, cy, rx, ry), color, blur)

    def line(self, pts, width, color, blur=0.0):
        self.paint(self.mask_line(pts, width), color, blur)

    def fill(self, pts, fn, blur=0.0):
        """A polygon filled by fn(X, Y) -> (H, W, 4) colours (gradients, noise)."""
        self.paint(self.mask_poly(pts), fn(self.X, self.Y), blur)

    def fill_ellipse(self, cx, cy, rx, ry, fn, blur=0.0):
        self.paint(self.mask_ellipse(cx, cy, rx, ry), fn(self.X, self.Y), blur)

    def glow(self, cx, cy, r, rgb, strength=1.0, power=2.0, add=False):
        """A soft radial glow (alpha falls off as (1 - d / r) ** power)."""
        d = np.sqrt((self.X - cx) ** 2 + (self.Y - cy) ** 2) / float(r)
        a = clamp01(1.0 - d) ** power * strength
        self.paint(a, tuple(rgb) + (255,))

    def star(self, cx, cy, r_out, r_in, n, rot, color):
        pts = []
        for k in range(2 * n):
            a = rot + k * math.pi / n
            r = r_out if k % 2 == 0 else r_in
            pts.append((cx + r * math.cos(a), cy + r * math.sin(a)))
        self.poly(pts, color)

    def ring(self, cx, cy, r, width, color, blur=0.0):
        d = np.sqrt((self.X - cx) ** 2 + (self.Y - cy) ** 2)
        m = clamp01(1.0 - np.abs(d - r) / (width / 2.0))
        self.paint(m, color, blur)

    def tint_noise(self, amount=0.12, scale=3.0, seed=1):
        """Multiplies the colour by a little noise (keeps alpha): breaks up flat fills."""
        a = np.asarray(self.im, dtype=np.float64)
        n = vnoise(self.W, self.H, scale * self.ss, seed)
        k = 1.0 + (n - 0.5) * 2.0 * amount
        a[..., :3] = np.clip(a[..., :3] * k[..., None], 0, 255)
        self.im = Image.fromarray(a.astype(np.uint8), "RGBA")

    def outline(self, rgb=(20, 12, 8), width=0.9, strength=0.9):
        """A dark outline round the opaque area (cel look)."""
        a = np.asarray(self.im.split()[3], dtype=np.float64) / 255.0
        grown = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(max(3, int(width * self.ss) * 2 + 1))), dtype=np.float64) / 255.0
        ring = np.clip(grown - a, 0, 1) * strength
        lay = np.zeros((self.H, self.W, 4))
        lay[..., :3] = rgb
        lay[..., 3] = ring * 255
        under = Image.fromarray(lay.astype(np.uint8), "RGBA")
        self.im = Image.alpha_composite(under, self.im)

    def save(self, name, quiet=False):
        os.makedirs(OUT, exist_ok=True)
        im = self.im.resize((self.w, self.h), Image.LANCZOS)
        a = np.asarray(im, dtype=np.float64)
        # premultiplied resize bleeds the (black) colour of transparent samples into edges: fix by resizing premultiplied colours
        big = np.asarray(self.im, dtype=np.float64)
        al = big[..., 3:4] / 255.0
        pm = Image.fromarray(np.clip(np.concatenate([big[..., :3] * al, big[..., 3:4]], axis=-1), 0, 255).astype(np.uint8), "RGBA").resize((self.w, self.h), Image.BOX)
        p = np.asarray(pm, dtype=np.float64)
        alpha = p[..., 3:4]
        rgb = np.where(alpha > 0, p[..., :3] / np.maximum(alpha / 255.0, 1e-4), 0)
        out = np.concatenate([np.clip(rgb, 0, 255), alpha], axis=-1).astype(np.uint8)
        Image.fromarray(out, "RGBA").save(os.path.join(OUT, name + ".png"))
        if not quiet:
            print("wrote %-28s %dx%d" % (name, self.w, self.h))


def polar(cx, cy, r, a):
    return (cx + r * math.cos(a), cy + r * math.sin(a))


def jitter_circle(cx, cy, r, n, amp, seed, phase=0.0):
    rng = random.Random(seed)
    return [polar(cx, cy, r * (1 + amp * (rng.random() - 0.5) * 2), phase + 2 * math.pi * k / n) for k in range(n)]


def contact_sheet(names, path, cols=8, cell=96):
    """A contact sheet of the named sprites on a dark checker (to look at them)."""
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell, rows * cell), (34, 34, 40, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        x, y = (i % cols) * cell, (i // cols) * cell
        for cy in range(0, cell, 16):
            for cx in range(0, cell, 16):
                if ((cx + cy) // 16) % 2 == 0:
                    d.rectangle([x + cx, y + cy, x + cx + 15, y + cy + 15], fill=(50, 50, 58, 255))
        try:
            im = Image.open(os.path.join(OUT, n + ".png")).convert("RGBA")
        except OSError:
            continue
        s = min((cell - 8) / float(im.width), (cell - 8) / float(im.height))
        im = im.resize((max(1, int(im.width * s)), max(1, int(im.height * s))), Image.LANCZOS)
        sheet.alpha_composite(im, (x + (cell - im.width) // 2, y + (cell - im.height) // 2))
        d.text((x + 2, y + 1), n[:16], fill=(255, 255, 200, 255))
    sheet.save(path)

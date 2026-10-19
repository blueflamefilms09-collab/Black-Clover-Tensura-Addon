"""Shared drawing kit of the 1.93.0 rebuild's VFX particle textures (Transmutation, Recombination, Modification, Mercury): supersampled PIL drawing, soft glow, ring / ticks / glyph /
lattice helpers. Textures are white or grey-scale with alpha (the vertex colour tints them) unless the colour is the point; 64/128/256 px, deterministic seeds.

    from transmutation_vfx_kit import Img, save
    im = Img(256, 256); im.ring(128, 128, 120, 4); im.glow(6); save("transmutation_circle", im.out())
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


class Img:
    """A white-on-transparent canvas drawn at 4x and averaged down. Coordinates are final pixels (floats); colours are (r, g, b) or grey ints; a = 0..255."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.im = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.im)

    @staticmethod
    def _c(col, a):
        if isinstance(col, int):
            col = (col, col, col)
        return (int(col[0]), int(col[1]), int(col[2]), int(a))

    def line(self, x0, y0, x1, y1, w=1.0, col=255, a=255):
        self.d.line([(x0 * SS, y0 * SS), (x1 * SS, y1 * SS)], fill=self._c(col, a), width=max(1, int(w * SS)))

    def poly(self, pts, col=255, a=255, outline=None, ow=1.0):
        self.d.polygon([(x * SS, y * SS) for x, y in pts], fill=self._c(col, a), outline=None if outline is None else self._c(outline, a))
        if outline is not None:
            pp = [(x * SS, y * SS) for x, y in pts] + [(pts[0][0] * SS, pts[0][1] * SS)]
            self.d.line(pp, fill=self._c(outline, a), width=max(1, int(ow * SS)))

    def ring(self, cx, cy, r, w=1.0, col=255, a=255):
        self.d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], outline=self._c(col, a), width=max(1, int(w * SS)))

    def disc(self, cx, cy, r, col=255, a=255):
        self.d.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], fill=self._c(col, a))

    def rect(self, x0, y0, x1, y1, col=255, a=255, outline=None, ow=1.0):
        self.d.rectangle([x0 * SS, y0 * SS, x1 * SS, y1 * SS], fill=None if col is None else self._c(col, a), outline=None if outline is None else self._c(outline, a), width=max(1, int(ow * SS)))

    def polar(self, cx, cy, r, ang):
        return cx + r * math.cos(ang), cy + r * math.sin(ang)

    def ticks(self, cx, cy, r0, r1, n, w=1.0, col=255, a=255, every=1):
        for k in range(0, n, every):
            t = 2 * math.pi * k / n
            x0, y0 = self.polar(cx, cy, r0, t)
            x1, y1 = self.polar(cx, cy, r1, t)
            self.line(x0, y0, x1, y1, w, col, a)

    def star(self, cx, cy, r, n, w=1.0, col=255, a=255, skip=1, rot=-math.pi / 2):
        """A {n/skip} star polygon as lines (hexagram: n=6 skip=2 draws two triangles)."""
        pts = [self.polar(cx, cy, r, rot + 2 * math.pi * k / n) for k in range(n)]
        if math.gcd(n, skip) == 1:
            for k in range(n):
                self.line(*pts[k], *pts[(k + skip) % n], w, col, a)
        else:
            g = math.gcd(n, skip)
            for s in range(g):
                for k in range(n // g):
                    self.line(*pts[(s + k * skip) % n], *pts[(s + (k + 1) * skip) % n], w, col, a)

    def glyph(self, x, y, s, rng, w=1.0, col=255, a=255):
        """A rune: 3 to 5 strokes on a 3x3 grid scaled to s, in a box centred at (x, y)."""
        pts = [(i, j) for i in range(3) for j in range(3)]
        cur = pts[int(rng.integers(0, 9))]
        for _ in range(int(rng.integers(3, 6))):
            nxt = pts[int(rng.integers(0, 9))]
            if nxt == cur:
                continue
            self.line(x + (cur[0] - 1) * s, y + (cur[1] - 1) * s, x + (nxt[0] - 1) * s, y + (nxt[1] - 1) * s, w, col, a)
            cur = nxt

    def out(self):
        return np.asarray(self.im.resize((self.w, self.h), Image.LANCZOS)).astype(float)


def glow(arr, radius, strength=1.0):
    """Adds a soft halo (gaussian blur of the alpha, max-merged with the original)."""
    a = Image.fromarray(np.clip(arr[..., 3], 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(radius))
    ha = np.asarray(a).astype(float) * strength
    out = arr.copy()
    m = ha > out[..., 3]
    out[..., 3] = np.where(m, ha, out[..., 3])
    for c in range(3):
        out[..., c] = np.where(m & (arr[..., 3] < 8), 255, out[..., c])
    return out


def radial(w, h, power=2.0, inner=0.0):
    yy, xx = np.mgrid[0:h, 0:w]
    r = np.sqrt(((xx + 0.5) / w * 2 - 1) ** 2 + ((yy + 0.5) / h * 2 - 1) ** 2)
    return np.clip((1 - r) / max(1e-3, 1 - inner), 0, 1) ** power


def tint(arr, rgb, mix=1.0):
    out = arr.copy()
    for c in range(3):
        out[..., c] = out[..., c] * (1 - mix) + rgb[c] * mix
    return out


def save(name, arr):
    os.makedirs(ROOT, exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ROOT, name + ".png"))
    print("wrote textures/particle/%s.png %dx%d" % (name, arr.shape[1], arr.shape[0]))

"""Drawing kit for the particle / sprite textures of the Water, Dark, Lightning and Flame rebuild (tools/gen_<key>_fx_textures.py).

Everything is a float mask (h x w, 0..1) that is built from soft primitives (discs, rings, strokes along polylines with a falloff, noise) at 2x and
shrunk with a box filter, so the edges are clean at any size. `save()` writes a WHITE texture whose alpha is the mask and whose rgb brightens toward the
core (so the vertex colour tints it, and an additive blend of it reads like light): the format of tools/gen_time_vfx_textures.py's sprites.
Run python as `python3 -B`.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

sys.dont_write_bytecode = True
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")


class Canvas:
    def __init__(self, w, h=None, ss=2, seed=1):
        h = h or w
        self.w, self.h, self.ss = w, h, ss
        self.W, self.H = w * ss, h * ss
        self.m = np.zeros((self.H, self.W), dtype=np.float32)
        self.core = np.zeros((self.H, self.W), dtype=np.float32)
        self.rng = np.random.default_rng(seed)
        self.yy, self.xx = np.mgrid[0:self.H, 0:self.W].astype(np.float32)

    # coordinates are given in final pixels (floats), origin top-left
    def _px(self, v):
        return v * self.ss

    def add(self, mask, core=None, op="max"):
        if op == "max":
            self.m = np.maximum(self.m, mask)
        else:
            self.m = np.clip(self.m + mask, 0, 1)
        if core is not None:
            self.core = np.maximum(self.core, core)

    def noise(self, octaves=4, base=4, seed=None):
        from water_aura_lib import fbm
        rng = np.random.default_rng(seed if seed is not None else int(self.rng.integers(1 << 30)))
        return fbm(rng, self.W, self.H, octaves, base)

    # ------------------------------------------------------------------ primitives (masks 0..1, soft = falloff width in final px)
    def disc(self, cx, cy, r, soft=2.0, val=1.0, hot=0.0, op="max"):
        d = np.hypot(self.xx - self._px(cx), self.yy - self._px(cy)) / self.ss
        mk = np.clip((r - d) / max(1e-3, soft) + 0.5, 0, 1) * val
        self.add(mk, mk * hot if hot else None, op)
        return mk

    def glow(self, cx, cy, r, power=2.0, val=1.0, hot=0.5, op="add"):
        d = np.hypot(self.xx - self._px(cx), self.yy - self._px(cy)) / self.ss
        mk = np.clip(1 - d / r, 0, 1) ** power * val
        self.add(mk, mk * hot, op)
        return mk

    def ring(self, cx, cy, r, width, soft=1.5, val=1.0, hot=0.0, a0=None, a1=None, op="max"):
        d = np.hypot(self.xx - self._px(cx), self.yy - self._px(cy)) / self.ss
        mk = np.clip((width / 2 - np.abs(d - r)) / max(1e-3, soft) + 0.5, 0, 1) * val
        if a0 is not None:
            ang = np.arctan2(self.yy - self._px(cy), self.xx - self._px(cx))
            span = (a1 - a0) % math.tau
            rel = (ang - a0) % math.tau
            mk = mk * (rel <= span)
        self.add(mk, mk * hot if hot else None, op)
        return mk

    def stroke(self, pts, w0, w1=None, soft=1.5, val=1.0, hot=0.0, fade=(1.0, 1.0), op="max"):
        """A polyline of width w0 -> w1 (final px) and value fading from fade[0] to fade[1] along its length."""
        w1 = w0 if w1 is None else w1
        lens = [math.hypot(pts[i + 1][0] - pts[i][0], pts[i + 1][1] - pts[i][1]) for i in range(len(pts) - 1)]
        total = max(1e-6, sum(lens))
        run = 0.0
        out = np.zeros_like(self.m)
        hotm = np.zeros_like(self.m)
        for i, L in enumerate(lens):
            (x0, y0), (x1, y1) = pts[i], pts[i + 1]
            t0, t1 = run / total, (run + L) / total
            run += L
            ax, ay, bx, by = self._px(x0), self._px(y0), self._px(x1), self._px(y1)
            dx, dy = bx - ax, by - ay
            ll = max(1e-6, dx * dx + dy * dy)
            t = np.clip(((self.xx - ax) * dx + (self.yy - ay) * dy) / ll, 0, 1)
            d = np.hypot(self.xx - (ax + t * dx), self.yy - (ay + t * dy)) / self.ss
            tt = t0 + (t1 - t0) * t
            half = (w0 + (w1 - w0) * tt) / 2
            mk = np.clip((half - d) / max(1e-3, soft) + 0.5, 0, 1) * (fade[0] + (fade[1] - fade[0]) * tt) * val
            out = np.maximum(out, mk)
            if hot:
                hotm = np.maximum(hotm, np.clip((half * 0.45 - d) / max(1e-3, soft) + 0.5, 0, 1) * (fade[0] + (fade[1] - fade[0]) * tt) * hot)
        self.add(out, hotm if hot else None, op)
        return out

    def polygon(self, pts, soft=1.0, val=1.0, hot=0.0, op="max"):
        """Filled convex or concave polygon (even-odd) with a soft edge."""
        from PIL import ImageDraw
        im = Image.new("L", (self.W, self.H), 0)
        ImageDraw.Draw(im).polygon([(self._px(x), self._px(y)) for x, y in pts], fill=255)
        r = max(0.0, soft * self.ss * 0.5)
        if r > 0.3:
            im = im.filter(ImageFilter.GaussianBlur(r))
        mk = np.asarray(im, dtype=np.float32) / 255.0 * val
        self.add(mk, mk * hot if hot else None, op)
        return mk

    def blur(self, r):
        im = Image.fromarray(np.clip(self.m * 255, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r * self.ss))
        self.m = np.asarray(im, dtype=np.float32) / 255.0
        im = Image.fromarray(np.clip(self.core * 255, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r * self.ss))
        self.core = np.asarray(im, dtype=np.float32) / 255.0

    def multiply(self, mask):
        self.m = self.m * mask
        self.core = self.core * mask

    def fade_edges(self, k=0.08):
        """Fade the mask to zero over the outer k fraction so nothing is cut at the texture edge."""
        ex = np.minimum(self.xx, self.W - self.xx) / (self.W * k)
        ey = np.minimum(self.yy, self.H - self.yy) / (self.H * k)
        f = np.clip(np.minimum(ex, ey), 0, 1)
        self.multiply(f)

    # ------------------------------------------------------------------ output
    def image(self):
        m = self.m.reshape(self.h, self.ss, self.w, self.ss).mean(axis=(1, 3))
        c = self.core.reshape(self.h, self.ss, self.w, self.ss).mean(axis=(1, 3))
        lum = np.clip(0.62 + 0.38 * np.clip(c / np.maximum(m, 1e-3), 0, 1), 0, 1)
        out = np.zeros((self.h, self.w, 4), dtype=np.uint8)
        out[..., :3] = (lum * 255)[..., None].astype(np.uint8)
        out[..., 3] = np.clip(m * 255, 0, 255).astype(np.uint8)
        return Image.fromarray(out, "RGBA")

    def save(self, name):
        os.makedirs(ROOT, exist_ok=True)
        self.image().save(os.path.join(ROOT, name + ".png"))
        return name


def rot(p, c, a):
    x, y = p[0] - c[0], p[1] - c[1]
    return (c[0] + x * math.cos(a) - y * math.sin(a), c[1] + x * math.sin(a) + y * math.cos(a))

"""Shared drawing kit of the 1.93.0 magic rebuild texture generators (gen_spatial_textures.py, gen_gravity_textures.py, gen_permeation_textures.py,
gen_mirror_textures.py, gen_slash_textures.py): supersampled shape drawing into float masks, soft glows, rune rings, stars, streaks, shards and the
RGBA writer. Everything is deterministic (seeded) and font-free. Textures are written white (or a light tint) with the shape in the alpha channel, so the
vertex colour of the effect tints them (the same convention as the Time Magic textures); the few coloured ones say so.

    from spatial_tex_kit import *
    m = Mask(256)                       # a float mask 0..1, drawn with supersampled PIL primitives
    m.ring(0.7, 0.03); m.ticks(0.55, 0.64, 24, 0.012); m.add(glow(256, 2.5) * 0.3)
    save("spatial_ring", m.rgba())      # -> src/main/resources/assets/nusmp/textures/particle/spatial_ring.png

Run python as `python3 -B`.
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures")
SS = 4


def save(name, img, folder="particle"):
    d = os.path.join(OUT, folder)
    os.makedirs(d, exist_ok=True)
    img.save(os.path.join(d, name + ".png"), optimize=True)
    print("wrote %s/%s %s" % (folder, name, img.size))


def radial(w, h=None):
    h = h or w
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    return np.sqrt(((xx - cx) / (w / 2.0)) ** 2 + ((yy - cy) / (h / 2.0)) ** 2)


def glow(w, power=2.2, h=None):
    r = np.clip(radial(w, h), 0, 1)
    return (1.0 - r) ** power


def smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def blur(arr, radius):
    img = Image.fromarray((np.clip(arr, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(radius))
    return np.asarray(img).astype(np.float32) / 255.0


def noise(w, h, seed, scale=8, octaves=3):
    rng = np.random.RandomState(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        gw, gh = max(2, int(w / scale * 2 ** o)), max(2, int(h / scale * 2 ** o))
        g = rng.rand(gh, gw).astype(np.float32)
        out += np.asarray(Image.fromarray((g * 255).astype(np.uint8), "L").resize((w, h), Image.BICUBIC)).astype(np.float32) / 255.0 * amp
        tot += amp
        amp *= 0.5
    return out / tot


class Mask:
    """A float mask drawn with supersampled primitives (coordinates are fractions of the width / height, -1..1 from the centre unless noted)."""

    def __init__(self, w, h=None):
        self.w, self.h = w, h or w
        self.a = np.zeros((self.h, self.w), np.float32)

    # ---- helpers
    def _layer(self):
        return Image.new("L", (self.w * SS, self.h * SS), 0)

    def _commit(self, im, k=1.0, blur_px=0.0):
        im = im.resize((self.w, self.h), Image.LANCZOS)
        if blur_px:
            im = im.filter(ImageFilter.GaussianBlur(blur_px))
        self.a = np.clip(np.maximum(self.a, np.asarray(im).astype(np.float32) / 255.0 * k), 0, 1)

    def px(self, x, y):
        return ((x + 1) * 0.5 * self.w * SS, (y + 1) * 0.5 * self.h * SS)

    def add(self, arr, k=1.0):
        self.a = np.clip(self.a + arr * k, 0, 1)

    def mul(self, arr):
        self.a = np.clip(self.a * arr, 0, 1)

    # ---- shapes
    def ring(self, r, width, k=1.0, blur_px=0.0, rx=None, ry=None):
        im = self._layer()
        d = ImageDraw.Draw(im)
        sx, sy = self.w * SS / 2.0, self.h * SS / 2.0
        cx, cy = sx, sy
        rx, ry = (rx or r), (ry or r)
        d.ellipse([cx - rx * sx - width * sx, cy - ry * sy - width * sy, cx + rx * sx + width * sx, cy + ry * sy + width * sy], fill=255)
        d.ellipse([cx - rx * sx + width * sx, cy - ry * sy + width * sy, cx + rx * sx - width * sx, cy + ry * sy - width * sy], fill=0)
        self._commit(im, k, blur_px)

    def disc(self, r, k=1.0, blur_px=0.0, cx=0.0, cy=0.0):
        im = self._layer()
        d = ImageDraw.Draw(im)
        x, y = self.px(cx, cy)
        d.ellipse([x - r * self.w * SS / 2, y - r * self.h * SS / 2, x + r * self.w * SS / 2, y + r * self.h * SS / 2], fill=255)
        self._commit(im, k, blur_px)

    def poly(self, pts, k=1.0, blur_px=0.0, fill=255):
        im = self._layer()
        ImageDraw.Draw(im).polygon([self.px(x, y) for x, y in pts], fill=fill)
        self._commit(im, k, blur_px)

    def line(self, pts, width, k=1.0, blur_px=0.0):
        im = self._layer()
        d = ImageDraw.Draw(im)
        P = [self.px(x, y) for x, y in pts]
        d.line(P, fill=255, width=max(1, int(width * self.w * SS / 2)), joint="curve")
        self._commit(im, k, blur_px)

    def ticks(self, r0, r1, n, width, k=1.0, phase=0.0, skip=0):
        for i in range(n):
            if skip and i % skip == 0:
                continue
            a = phase + 2 * math.pi * i / n
            c, s = math.cos(a), math.sin(a)
            nx, ny = -s * width, c * width
            self.poly([(c * r0 + nx, s * r0 + ny), (c * r1 + nx, s * r1 + ny), (c * r1 - nx, s * r1 - ny), (c * r0 - nx, s * r0 - ny)], k)

    def star(self, arms=4, length=0.95, width=0.09, k=1.0, phase=0.0, blur_px=0.0, inner=0.0):
        for i in range(arms):
            a = phase + 2 * math.pi * i / arms
            c, s = math.cos(a), math.sin(a)
            self.poly([(c * inner - s * width, s * inner + c * width), (c * length, s * length), (c * inner + s * width, s * inner - c * width)], k, blur_px)

    def arc(self, r, width, a0, a1, k=1.0, blur_px=0.0, steps=48):
        pts_o = [(math.cos(a0 + (a1 - a0) * i / steps) * (r + width), math.sin(a0 + (a1 - a0) * i / steps) * (r + width)) for i in range(steps + 1)]
        pts_i = [(math.cos(a1 - (a1 - a0) * i / steps) * (r - width), math.sin(a1 - (a1 - a0) * i / steps) * (r - width)) for i in range(steps + 1)]
        self.poly(pts_o + pts_i, k, blur_px)

    def crescent(self, r, thick, a0, a1, k=1.0, blur_px=0.0, steps=48):
        """A crescent: both edges follow circles, the width swells in the middle and goes to points at the ends."""
        outer = [(math.cos(a0 + (a1 - a0) * i / steps) * r, math.sin(a0 + (a1 - a0) * i / steps) * r) for i in range(steps + 1)]
        inner = []
        for i in range(steps, -1, -1):
            t = i / float(steps)
            rr = r - thick * math.sin(t * math.pi)
            a = a0 + (a1 - a0) * t
            inner.append((math.cos(a) * rr, math.sin(a) * rr))
        self.poly(outer + inner, k, blur_px)

    def rune_glyph(self, cx, cy, size, seed, k=1.0, width=0.012):
        """A little angular glyph made of 3 to 5 strokes on a 3x3 grid (a rune)."""
        rng = random.Random(seed)
        pts = [(cx + (i - 1) * size, cy + (j - 1) * size) for j in range(3) for i in range(3)]
        cur = rng.choice(pts)
        for _ in range(rng.randint(3, 5)):
            nxt = rng.choice(pts)
            if nxt != cur:
                self.line([cur, nxt], width, k)
            cur = nxt

    # ---- output
    def rgba(self, rgb=(255, 255, 255), core=None, edge=None):
        """White (or `rgb`) with the mask as alpha; `core` blends the hottest part to that colour, `edge` the faint part."""
        a = np.clip(self.a, 0, 1)
        out = np.zeros((self.h, self.w, 4), np.uint8)
        base = np.array(rgb, np.float32)
        col = np.ones((self.h, self.w, 3), np.float32) * base
        if edge is not None:
            col = col * smooth(0.0, 0.5, a)[..., None] + np.array(edge, np.float32) * (1 - smooth(0.0, 0.5, a))[..., None]
        if core is not None:
            t = smooth(0.55, 1.0, a)[..., None]
            col = col * (1 - t) + np.array(core, np.float32) * t
        out[..., :3] = np.clip(col, 0, 255).astype(np.uint8)
        out[..., 3] = (a * 255).astype(np.uint8)
        return Image.fromarray(out, "RGBA")


def compose_rgba(color_img, mask):
    """An RGB float array (h,w,3, 0..255) and a mask -> RGBA image."""
    out = np.zeros(mask.shape + (4,), np.uint8)
    out[..., :3] = np.clip(color_img, 0, 255).astype(np.uint8)
    out[..., 3] = (np.clip(mask, 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")

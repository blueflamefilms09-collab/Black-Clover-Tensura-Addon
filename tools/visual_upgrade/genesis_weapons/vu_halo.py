"""The shared additive sprite sheet of the halo layer (layer 4): textures/entity/weapon_vu/halo.png, 512 x 512, 16 cells of 128 px (the cell index = HALO_CELLS in
vu_item.py). White on transparent: the renderer tints a whole group (its `tint`) and pulses its alpha. Drawn with RenderType.eyes (additive)."""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from common import VU_TEX


def _blank():
    return np.zeros((128, 128), np.float32)


def _yx():
    yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
    return (xx - 63.5) / 63.5, (yy - 63.5) / 63.5


def cell_glow():
    x, y = _yx()
    r = np.hypot(x, y)
    return np.clip(np.exp(-(r * 2.3) ** 2) * 1.0 + np.exp(-(r * 1.1) ** 2) * 0.25, 0, 1) * np.clip(1.4 - r, 0, 1)


def cell_ring(w=0.045, r0=0.8):
    x, y = _yx()
    r = np.hypot(x, y)
    return np.clip(np.exp(-((r - r0) / w) ** 2) + 0.18 * np.exp(-((r - r0) / (w * 5)) ** 2), 0, 1) * np.clip(1.15 - r, 0, 1)


def cell_star():
    x, y = _yx()
    r = np.hypot(x, y)
    a = np.exp(-(x / 0.035) ** 2) * np.exp(-np.abs(y) * 2.6) + np.exp(-(y / 0.035) ** 2) * np.exp(-np.abs(x) * 2.6)
    d = np.exp(-((x - y) / 0.05) ** 2) * np.exp(-np.abs(x + y) * 3.6) * 0.5 + np.exp(-((x + y) / 0.05) ** 2) * np.exp(-np.abs(x - y) * 3.6) * 0.5
    return np.clip(a + d + np.exp(-(r * 5.0) ** 2), 0, 1) * np.clip(1.2 - r, 0, 1)


def cell_shaft():
    x, y = _yx()
    t = (1.0 - (y + 1) / 2)                                    # 1 at the bottom of the cell, 0 at the top
    return np.clip(np.exp(-(x / (0.16 + 0.55 * (1 - t))) ** 2) * (t ** 0.8) * 1.0, 0, 1) * np.clip(1.0 - np.abs(x) * 0.2, 0, 1)


def cell_streak():
    x, y = _yx()
    return np.clip(np.exp(-(y / 0.06) ** 2) * np.exp(-(x / 0.75) ** 2), 0, 1)


def cell_rays():
    x, y = _yx()
    r = np.hypot(x, y)
    a = np.arctan2(y, x)
    rays = (0.5 + 0.5 * np.cos(a * 12)) ** 8
    return np.clip(rays * np.exp(-r * 1.8) + np.exp(-(r * 4) ** 2), 0, 1) * np.clip(1.1 - r, 0, 1)


def cell_diamond():
    x, y = _yx()
    d = np.abs(x) * 1.0 + np.abs(y) * 1.9
    return np.clip(1.0 - d, 0, 1) ** 1.4 * 1.2


def cell_crescent():
    x, y = _yx()
    a = np.hypot(x, y)
    b = np.hypot(x + 0.38, y)
    return np.clip(smooth((0.82 - a) * 12) * (1 - smooth((0.72 - b) * 12)), 0, 1)


def smooth(t):
    t = np.clip(t, 0, 1)
    return t * t * (3 - 2 * t)


def cell_runes():
    im = Image.new("L", (128, 128), 0)
    d = ImageDraw.Draw(im)
    for r in (58, 50):
        d.ellipse([64 - r, 64 - r, 64 + r, 64 + r], outline=255, width=2)
    for i in range(24):
        a = 2 * math.pi * i / 24
        d.line([(64 + 50 * math.cos(a), 64 + 50 * math.sin(a)), (64 + 58 * math.cos(a), 64 + 58 * math.sin(a))], fill=255, width=2)
    for i in range(8):
        a = 2 * math.pi * i / 8 + 0.2
        d.polygon([(64 + 40 * math.cos(a), 64 + 40 * math.sin(a)), (64 + 30 * math.cos(a + 0.35), 64 + 30 * math.sin(a + 0.35)), (64 + 30 * math.cos(a - 0.35), 64 + 30 * math.sin(a - 0.35))], outline=255)
    im = im.filter(ImageFilter.GaussianBlur(0.7))
    return np.asarray(im, np.float32) / 255.0


def cell_bolt():
    im = Image.new("L", (128, 128), 0)
    d = ImageDraw.Draw(im)
    rng = np.random.default_rng(5)
    pts = [(64.0, 4.0)]
    for i in range(1, 9):
        pts.append((64 + float(rng.uniform(-18, 18)), 4 + i * 15.5))
    d.line(pts, fill=255, width=4)
    d.line(pts, fill=255, width=2)
    im2 = im.filter(ImageFilter.GaussianBlur(2.2))
    return np.clip(np.asarray(im, np.float32) / 255.0 + np.asarray(im2, np.float32) / 255.0 * 0.8, 0, 1)


def cell_feather():
    x, y = _yx()
    t = (y + 1) / 2
    w = np.abs(np.sin(np.pi * np.clip(t, 0, 1))) ** 0.8 * 0.45 + 0.02
    return np.clip(np.exp(-(x / w) ** 4), 0, 1) * np.clip(1 - np.abs(x) * 0.5, 0, 1) * (0.55 + 0.45 * np.cos(x * 40 + y * 3) ** 2)


def cell_flame():
    x, y = _yx()
    t = (y + 1) / 2
    w = np.clip(1 - t, 0, 1) ** 0.7 * 0.55 * (0.7 + 0.3 * np.sin(t * 9)) + 0.02
    return np.clip(np.exp(-(np.abs(x) / np.maximum(w, 0.02)) ** 2.5), 0, 1) * (np.clip(t, 0, 1) ** 0.15) * np.clip(1.2 - t, 0, 1)


def cell_hex():
    im = Image.new("L", (128, 128), 0)
    d = ImageDraw.Draw(im)
    pts = [(64 + 56 * math.cos(math.radians(60 * i + 30)), 64 + 56 * math.sin(math.radians(60 * i + 30))) for i in range(6)]
    d.polygon(pts, outline=255, width=3)
    pts2 = [(64 + 44 * math.cos(math.radians(60 * i)), 64 + 44 * math.sin(math.radians(60 * i))) for i in range(6)]
    d.polygon(pts2, outline=200, width=1)
    return np.asarray(im.filter(ImageFilter.GaussianBlur(0.8)), np.float32) / 255.0


def cell_sparkle():
    x, y = _yx()
    out = np.zeros_like(x)
    rng = np.random.default_rng(11)
    for _ in range(9):
        cx, cy, s = rng.uniform(-0.7, 0.7), rng.uniform(-0.7, 0.7), rng.uniform(0.08, 0.2)
        dx, dy = x - cx, y - cy
        out = np.maximum(out, np.exp(-(dx / 0.012) ** 2) * np.exp(-np.abs(dy) / s) + np.exp(-(dy / 0.012) ** 2) * np.exp(-np.abs(dx) / s) + np.exp(-(np.hypot(dx, dy) / 0.03) ** 2))
    return np.clip(out, 0, 1)


def cell_solid():
    x, y = _yx()
    return np.clip(1.4 - np.maximum(np.abs(x), np.abs(y)) * 1.0, 0, 1) ** 0.2


CELLS = [cell_glow, cell_ring, lambda: cell_ring(0.12, 0.72), cell_star, cell_shaft, cell_streak, cell_rays, cell_diamond, cell_crescent, cell_runes, cell_bolt,
         cell_feather, cell_flame, cell_hex, cell_sparkle, cell_solid]


def build(path=None):
    sheet = np.zeros((512, 512, 4), np.float32)
    for i, fn in enumerate(CELLS):
        a = np.clip(fn(), 0, 1)
        r, c = divmod(i, 4)
        sheet[r * 128:(r + 1) * 128, c * 128:(c + 1) * 128, :3] = 255
        sheet[r * 128:(r + 1) * 128, c * 128:(c + 1) * 128, 3] = a * 255
    path = path or os.path.join(VU_TEX, "halo.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(sheet.astype(np.uint8), "RGBA").save(path, optimize=True)
    return path


if __name__ == "__main__":
    print(build())

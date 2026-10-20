"""fxtex: the textures of the additive passes of the visual upgrade.

* vu_fx.png     the shared 4 x 4 sheet (64 px cells) of soft light shapes the HALO quads sample (disc, rings, rune ring, beam, flare, ripples, seal circle ...); white with an alpha
                pattern, tinted by the colour slot of each quad in the game.
* <id>_star.png the scrolling nebula / starfield of an item's STAR quads (128 x 128, tileable): a palette gradient of tileable noise, stars, dust lanes; RGB is the light, alpha 255.
"""
import math
import os

import numpy as np
from PIL import Image

CELLS = ["disc", "ring", "ticks", "rune", "beam", "flare", "ripple", "cross", "seal", "streak", "web", "rays", "bolt", "petal", "smoke", "glint"]
CELL = {n: i for i, n in enumerate(CELLS)}
CS = 64


def cell_uv(name, inset=0.6):
    """(u0, v0, u1, v1) of a cell in 0..1, inset by `inset` texels so linear sampling never bleeds from a neighbour."""
    i = CELL[name]
    cx, cy = (i % 4) * CS, (i // 4) * CS
    return ((cx + inset) / 256.0, (cy + inset) / 256.0, (cx + CS - inset) / 256.0, (cy + CS - inset) / 256.0)


def _grid(n=CS):
    y, x = np.mgrid[0:n, 0:n].astype(np.float64)
    x = (x + 0.5) / n * 2 - 1
    y = (y + 0.5) / n * 2 - 1
    return x, y, np.hypot(x, y), np.arctan2(y, x)


def _smooth(x, a, b):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def _glow(r, r0, w):
    return np.exp(-((r - r0) / w) ** 2)


def _noise(n, seed, scale=4, octaves=3, tile=True):
    """Tileable value-ish noise by a sum of sines with integer wave numbers (0..1)."""
    rng = np.random.default_rng(seed)
    y, x = np.mgrid[0:n, 0:n].astype(np.float64)
    out = np.zeros((n, n))
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        for _ in range(5):
            kx, ky = int(rng.integers(-scale * (o + 1), scale * (o + 1) + 1)), int(rng.integers(-scale * (o + 1), scale * (o + 1) + 1))
            if kx == 0 and ky == 0:
                kx = 1
            out += amp * np.sin(2 * math.pi * (kx * x + ky * y) / n + rng.uniform(0, 2 * math.pi))
            tot += amp
        amp *= 0.55
    out = out / tot
    return (out - out.min()) / max(out.max() - out.min(), 1e-9)


def build_fx():
    sheet = np.zeros((256, 256, 4), np.float64)

    def put(name, alpha):
        i = CELL[name]
        cx, cy = (i % 4) * CS, (i // 4) * CS
        a = np.clip(alpha, 0, 1)
        sheet[cy:cy + CS, cx:cx + CS, :3] = 1.0
        sheet[cy:cy + CS, cx:cx + CS, 3] = a

    x, y, r, th = _grid()
    edge = 1.0 - _smooth(r, 0.86, 1.0)                                  # every shape fades to nothing at the cell edge
    put("disc", (1 - np.clip(r, 0, 1)) ** 2.2 * edge)
    put("ring", (_glow(r, 0.78, 0.07) + 0.18 * _glow(r, 0.78, 0.2) + 0.06 * (1 - r)) * edge)
    tick = (np.abs(((th / (2 * math.pi)) * 48) % 1.0 - 0.5) < 0.17) & (r > 0.68) & (r < 0.88)
    put("ticks", (_glow(r, 0.9, 0.035) + _glow(r, 0.62, 0.03) + 0.9 * tick + 0.1 * (1 - r)) * edge)
    # rune ring: a band with 20 glyph cells, each glyph a few strokes
    rng = np.random.default_rng(31)
    band = (r > 0.6) & (r < 0.9)
    rune = np.zeros_like(r)
    nG = 20
    for g in range(nG):
        a0 = -math.pi + 2 * math.pi * g / nG
        for _ in range(3):
            t0 = a0 + rng.uniform(0.02, 0.28) * (2 * math.pi / nG) * 2
            r0, r1 = rng.uniform(0.64, 0.86), rng.uniform(0.64, 0.86)
            t1 = t0 + rng.uniform(-0.05, 0.05)
            for k in range(0, 21):
                tt = k / 20
                px, py = (r0 + (r1 - r0) * tt) * math.cos(t0 + (t1 - t0) * tt), (r0 + (r1 - r0) * tt) * math.sin(t0 + (t1 - t0) * tt)
                rune = np.maximum(rune, np.exp(-(((x - px) ** 2 + (y - py) ** 2) / 0.0006)))
    put("rune", (rune * band * 1.1 + _glow(r, 0.92, 0.025) + _glow(r, 0.57, 0.025) + 0.05 * (1 - r)) * edge)
    # beam: bright core across u, soft falloff, fading at both ends of v
    bu = np.exp(-(x / 0.28) ** 2) * 0.9 + np.exp(-(x / 0.07) ** 2) * 0.6
    put("beam", np.clip(bu, 0, 1) * (_smooth(y, -1.0, -0.55) * (1 - _smooth(y, 0.35, 1.0))))
    # lens flare: horizontal + vertical streaks, a soft core, faint diagonal spikes
    streak = np.exp(-(y / 0.035) ** 2) * (1 - np.clip(np.abs(x), 0, 1)) ** 1.5 + np.exp(-(x / 0.035) ** 2) * (1 - np.clip(np.abs(y), 0, 1)) ** 1.5
    diag = (np.exp(-(((x - y) / 0.05) ** 2)) + np.exp(-(((x + y) / 0.05) ** 2))) * (1 - np.clip(r, 0, 1)) ** 2.5 * 0.45
    put("flare", (streak * 0.9 + diag + np.exp(-(r / 0.16) ** 2) * 1.1) * edge)
    rip = sum(_glow(r, 0.2 + 0.2 * k, 0.045) * (1.0 - 0.18 * k) for k in range(4))
    put("ripple", rip * edge + 0.1 * (1 - r) * edge)
    rays8 = (np.abs(((th / (2 * math.pi)) * 8 + 0.0) % 1.0 - 0.5) > 0.46)
    put("cross", (rays8 * (1 - np.clip(r, 0, 1)) ** 1.4 + np.exp(-(r / 0.1) ** 2)) * edge)
    # seal: two rings, a hexagram, a centre dot, 12 small marks
    seal = _glow(r, 0.9, 0.03) + _glow(r, 0.8, 0.02) + _glow(r, 0.38, 0.02) + np.exp(-(r / 0.06) ** 2)
    for tri in (0, 1):
        for k in range(3):
            a0 = math.radians(90 + 120 * k + 60 * tri)
            a1 = math.radians(90 + 120 * (k + 1) + 60 * tri)
            for t in np.linspace(0, 1, 90):
                px, py = 0.8 * (math.cos(a0) * (1 - t) + math.cos(a1) * t), 0.8 * (math.sin(a0) * (1 - t) + math.sin(a1) * t)
                seal = np.maximum(seal, 0.9 * np.exp(-(((x - px) ** 2 + (y - py) ** 2) / 0.0007)))
    put("seal", seal * edge)
    sx = (x + 1) / 2
    put("streak", np.clip(np.exp(-(y / (0.04 + 0.5 * sx)) ** 2) * (1 - sx) ** 1.2 * 1.1, 0, 1) * (1 - _smooth(np.abs(x), 0.9, 1.0)))
    # caustic web: edges of a voronoi
    rng = np.random.default_rng(7)
    pts = rng.uniform(-1, 1, (14, 2))
    d = np.stack([np.hypot(x - p[0], y - p[1]) for p in pts])
    d.sort(axis=0)
    put("web", np.clip(1 - (d[1] - d[0]) / 0.12, 0, 1) * 0.9 * edge)
    put("rays", (0.5 + 0.5 * np.cos(th * 14)) ** 3 * (1 - np.clip(r, 0, 1)) ** 1.2 * edge + np.exp(-(r / 0.12) ** 2))
    bolt = np.zeros_like(r)
    px = 0.0
    for k in range(64):
        yy = -1 + 2 * k / 63
        px = 0.18 * math.sin(yy * 9.0) + 0.1 * math.sin(yy * 23.0)
        bolt = np.maximum(bolt, np.exp(-(((x - px) ** 2) / 0.003 + ((y - yy) ** 2) / 0.003)))
    put("bolt", bolt * (1 - _smooth(np.abs(y), 0.85, 1.0)))
    petal = np.clip(1 - ((x / 0.45) ** 2 + ((y - 0.1) / 0.9) ** 2), 0, 1) ** 0.7 * (0.55 + 0.45 * (1 - np.abs(x)))
    put("petal", petal)
    put("smoke", np.clip(_noise(CS, 5, 3, 3) * 1.4 - 0.35, 0, 1) * (1 - _smooth(r, 0.5, 1.0)))
    put("glint", (np.exp(-(np.abs(x) / 0.05) ** 2) * np.exp(-(np.abs(y) / 0.5) ** 2) + np.exp(-(np.abs(y) / 0.05) ** 2) * np.exp(-(np.abs(x) / 0.5) ** 2)) * edge + np.exp(-(r / 0.1) ** 2))
    return (np.clip(sheet, 0, 1) * 255).astype(np.uint8)


def save_fx(path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(build_fx(), "RGBA").save(path, optimize=True)


def build_star(rgb_dark, rgb_mid, rgb_hot, style="nebula", seed=1, size=128):
    """A tileable nebula / starfield: noise through a three-colour gradient, bright stars, optional dust lanes. Returns uint8 RGBA (alpha 255)."""
    n = _noise(size, seed, 3, 4)
    n2 = _noise(size, seed + 101, 6, 3)
    if style == "swirl":
        yy, xx = np.mgrid[0:size, 0:size].astype(np.float64)
        n = 0.5 + 0.5 * np.sin(2 * math.pi * (xx / size * 2 + n2 * 1.5)) * np.cos(2 * math.pi * (yy / size * 1 + n * 0.7))
    elif style == "lava":
        n = np.clip(1 - np.abs(n - 0.5) * 3.2, 0, 1) ** 1.3
    elif style == "grid":
        yy, xx = np.mgrid[0:size, 0:size]
        g = ((xx % 16 == 0) | (yy % 16 == 0)).astype(np.float64)
        n = np.clip(n * 0.7 + g * 0.5, 0, 1)
    elif style == "ocean":
        yy, xx = np.mgrid[0:size, 0:size].astype(np.float64)
        n = 0.5 + 0.5 * np.sin(2 * math.pi * (yy / size * 3 + n2 * 1.2)) * (0.6 + 0.4 * n)
    elif style == "void":
        n = np.clip((n - 0.55) * 3.0, 0, 1) * 0.7 + np.clip((n2 - 0.7) * 3, 0, 1) * 0.4
    lo, mid, hi = (np.array(c, np.float64) / 255.0 for c in (rgb_dark, rgb_mid, rgb_hot))
    t = np.clip(n, 0, 1)[..., None]
    col = np.where(t < 0.55, lo + (mid - lo) * (t / 0.55), mid + (hi - mid) * ((t - 0.55) / 0.45))
    rng = np.random.default_rng(seed + 7)
    img = np.clip(col * (0.45 + 0.9 * n2[..., None] * 0.6), 0, 1)
    for _ in range(46):
        sx, sy = int(rng.integers(0, size)), int(rng.integers(0, size))
        amp = rng.uniform(0.5, 1.0)
        rad = rng.uniform(0.7, 1.7)
        for dx in range(-3, 4):
            for dy in range(-3, 4):
                a = amp * math.exp(-(dx * dx + dy * dy) / (rad * rad))
                if dx == 0 or dy == 0:
                    a = max(a, amp * 0.5 * math.exp(-(dx * dx + dy * dy) / 6.0))
                img[(sy + dy) % size, (sx + dx) % size] = np.clip(img[(sy + dy) % size, (sx + dx) % size] + a * (0.75 * hi + 0.25), 0, 1)
    out = np.concatenate([img * 0.82, np.ones((size, size, 1))], axis=2)
    return (out * 255).astype(np.uint8)


def save_star(path, rgb_dark, rgb_mid, rgb_hot, style, seed, size=128):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    Image.fromarray(build_star(rgb_dark, rgb_mid, rgb_hot, style, seed, size), "RGBA").save(path, optimize=True)

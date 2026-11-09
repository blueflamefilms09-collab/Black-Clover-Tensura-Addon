"""Textures for the Saiyan form looks (3.66.0, Stage 8): hair strand, aura flame, lightning bolt, power-up ring, glowing eye,
Great-Ape-free "golden fur" skin map (Super Saiyan 4 forms) and the tail ribbon. Deterministic, no fonts, no external images
(every shape is drawn by code; web pictures were only ever references for colours, never copied).

    python tools/gen_saiyan_look_textures.py   ->  src/main/resources/assets/nusmp/textures/particle/saiyan_*.png

Convention (same as the rest of textures/particle): white / greyscale, one vertex colour tints each quad. The additive sprites carry
their colour only where they are opaque (black under the transparent texels, so RenderType.eyes can never draw a square).
"""
import os

import numpy as np
from PIL import Image

# PARKED (3.66.0): writes next to this file; to resume the look work copy the PNGs to src/main/resources/assets/nusmp/textures/particle/
OUT = os.path.join(os.path.dirname(__file__), "textures")


def save(arr, name):
    os.makedirs(OUT, exist_ok=True)
    im = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def rgba(grey, alpha):
    g = np.clip(grey, 0, 1) * 255
    return np.dstack([g, g, g, np.clip(alpha, 0, 1) * 255])


def additive(intensity):
    """Premultiplied look: grey = alpha = intensity, so nothing is coloured where nothing is drawn."""
    i = np.clip(intensity, 0, 1)
    return rgba(i, i)


def smooth(x):
    return x * x * (3 - 2 * x)


def strand():
    """A tapered hair strand, tip at the top: binary alpha (cutout), shaded streaks along its length."""
    w, h = 32, 128
    rng = np.random.default_rng(11)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = 1.0 - yy / (h - 1)                      # 0 at the base, 1 at the tip
    half = (0.5 - 0.44 * t ** 0.85) * w + 1.0   # taper to a point
    # a slight S curve so a row of strands does not look ruled
    cx = w / 2 + 2.2 * np.sin(t * 3.1)
    inside = np.abs(xx - cx) <= half
    streak = rng.random(w).astype(np.float32)
    streak = np.convolve(np.concatenate([streak, streak[:3]]), np.ones(3) / 3, "same")[:w]
    side = (xx - cx) / np.maximum(half, 1.0)    # -1..1 across the strand
    shade = 0.70 + 0.22 * np.cos(side * 1.35) - 0.10 * t + 0.12 * (streak[xx.astype(int)] - 0.5)
    shade += 0.10 * np.sin(yy * 0.35 + xx * 0.9)
    shade = np.where(side < -0.55, shade * 0.80, shade)   # a darker left edge reads as depth
    save(rgba(shade, inside.astype(np.float32)), "saiyan_strand")


def flame():
    """An aura flame tongue (additive): bright core at the base, licking tip, soft sides."""
    w, h = 32, 96
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = 1.0 - yy / (h - 1)                      # 0 base .. 1 tip
    sway = 3.0 * np.sin(t * 5.2) * t
    half = (0.48 * (1.0 - t ** 1.1) * (1.0 + 0.10 * np.sin(t * 17.0)) + 0.015) * w
    d = np.abs(xx - (w / 2 + sway)) / np.maximum(half, 0.6)
    body = np.clip(1.0 - d, 0, 1)
    body = smooth(body)
    fade = np.clip(1.0 - t ** 1.6, 0, 1) * np.clip(t / 0.10, 0, 1) ** 0.8   # soft bottom edge, so a flame stands in the air without a cut line
    core = np.clip(1.0 - d * 2.2, 0, 1) * np.clip(1.0 - t * 1.4, 0, 1)
    save(additive(np.clip(body * fade * 0.85 + core * 0.9, 0, 1)), "saiyan_flame")


def bolt():
    """A jagged lightning bolt (additive), bright core and a glow, tip at the top."""
    w, h = 32, 128
    rng = np.random.default_rng(23)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    path = np.zeros(h, np.float32)
    x = w / 2
    for y in range(h - 1, -1, -1):
        if (h - 1 - y) % 9 == 0:
            x += rng.uniform(-4.5, 4.5)
        x = float(np.clip(x, 5, w - 6))
        path[y] = x
    dist = np.abs(xx - path[:, None])
    reach = 1.0 - (1.0 - yy / (h - 1)) ** 3 * 0.35   # a little thinner towards the tip
    core = np.clip(1.0 - dist / 2.0, 0, 1)
    glow = np.clip(1.0 - dist / 9.0, 0, 1) ** 2 * 0.75
    tipfade = np.clip(yy / 14.0, 0, 1) ** 0.7
    save(additive(np.clip((core + glow) * reach * tipfade, 0, 1)), "saiyan_bolt")


def ring():
    """The power-up shockwave ring and ground glow (additive)."""
    n = 128
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    r = np.hypot(xx - n / 2 + 0.5, yy - n / 2 + 0.5) / (n / 2)
    band = np.exp(-((r - 0.82) / 0.07) ** 2)
    inner = np.exp(-((r - 0.55) / 0.16) ** 2) * 0.30
    disc = np.clip(1.0 - r, 0, 1) ** 1.6 * 0.38
    ang = np.arctan2(yy - n / 2 + 0.5, xx - n / 2 + 0.5)
    ticks = (np.cos(ang * 12) > 0.82) * np.exp(-((r - 0.93) / 0.05) ** 2) * 0.55
    edge = np.clip((1.0 - r) / 0.06, 0, 1)
    save(additive((band + inner + disc + ticks) * edge), "saiyan_ring")


def eye():
    """A glowing eye bar (translucent emissive): a solid core with soft ends, white so the form colour tints it."""
    w, h = 16, 8
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    dx = np.abs(xx - w / 2 + 0.5) / (w / 2)
    dy = np.abs(yy - h / 2 + 0.5) / (h / 2)
    a = np.clip(1.0 - (dx ** 2.6 + dy ** 2.2), 0, 1)
    save(rgba(np.ones_like(a), smooth(np.clip(a * 1.7, 0, 1))), "saiyan_eye")


def fur():
    """The skin-layout fur map for Super Saiyan 4 (64x64, cutout): fur on arms, legs, the back and the sides of the chest; the front of the chest, the hands and the feet stay bare."""
    n = 64
    rng = np.random.default_rng(37)
    a = np.zeros((n, n), np.float32)
    g = np.zeros((n, n), np.float32)

    def region(x0, y0, w, h):
        a[y0:y0 + h, x0:x0 + w] = 1.0

    # right leg / left leg: front, back and sides of the upper 9 rows (the lower rows are the shin and the foot)
    for x0, y0 in ((0, 16), (16, 48)):
        region(x0, y0 + 4, 16, 9)
    # right arm / left arm: all four faces, the lower 3 rows (the hand) stay bare
    for x0, y0 in ((40, 16), (32, 48)):
        region(x0, y0 + 4, 16, 9)
    # body: back and both sides in full, the front only as a thin collar and belt, so the chest stays bare
    region(32, 20, 8, 12)        # back
    region(16, 20, 4, 12)        # right side
    region(28, 20, 4, 12)        # left side
    region(20, 20, 8, 2)         # collar (front)
    region(20, 30, 8, 2)         # belt (front)
    region(20, 16, 16, 4)        # top and bottom faces
    # ragged tufts along every cut edge
    ragged = rng.random((n, n)) < 0.18
    edge = np.zeros_like(a)
    edge[1:, :] += (a[1:, :] > 0) & (a[:-1, :] == 0)
    edge[:-1, :] += (a[:-1, :] > 0) & (a[1:, :] == 0)
    a = np.where((edge > 0) & ragged, 0.0, a)
    # strands: short vertical streaks with darker bands
    cols = rng.random(n).astype(np.float32)
    rows = np.sin(np.arange(n) * 0.9)[:, None] * 0.07
    g = 0.78 + 0.18 * (cols[None, :] - 0.5) * 2 + rows + 0.08 * (rng.random((n, n)) - 0.5)
    save(rgba(g, a), "saiyan_fur")


def tail():
    """The tail ribbon (cutout): fur strands along the length, ragged edges, tip at the top."""
    w, h = 16, 64
    rng = np.random.default_rng(5)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = 1.0 - yy / (h - 1)
    half = (0.5 - 0.15 * t) * w
    inside = np.abs(xx - w / 2 + 0.5) <= half
    inside &= ~((rng.random((h, w)) < 0.10) & (np.abs(xx - w / 2 + 0.5) > half - 1.5))
    g = 0.82 + 0.16 * np.sin(xx * 1.7 + yy * 0.2) + 0.10 * (rng.random((h, w)) - 0.5)
    save(rgba(g, inside.astype(np.float32)), "saiyan_tail")


if __name__ == "__main__":
    strand(); flame(); bolt(); ring(); eye(); fur(); tail()

"""Generates the Sealing Magic aura textures: three streak rings for Orbital Bind (comet arcs of different density), the rune halo of the gentle
style, the latitude sphere shell and a 64x64 player-skin-layout shimmer. White with alpha (tinted by the vertex colour).
Output: src/main/resources/assets/nusmp/textures/aura/seal_*.png

    python tools/gen_seal_aura_textures.py
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "aura")


def save(a, name):
    a = np.clip(a, 0, 1)
    h, w = a.shape
    Image.fromarray((np.dstack([np.ones((h, w, 3)), a]) * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, name))


def polar(n):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x = (xx + 0.5) / n * 2 - 1
    y = (yy + 0.5) / n * 2 - 1
    return np.sqrt(x * x + y * y), np.arctan2(y, x)


def comet_ring(n, comets, seed, r0=0.86, width=0.045):
    r, th = polar(n)
    rng = random.Random(seed)
    a = np.zeros_like(r)
    for _ in range(comets):
        head = rng.uniform(0, math.tau)
        ln = rng.uniform(1.0, 2.2)
        rr = r0 + rng.uniform(-0.07, 0.07)
        w = width * rng.uniform(0.6, 1.3)
        d = (head - th) % math.tau                       # how far behind the head this pixel is
        t = np.clip(1 - d / ln, 0, 1)
        t = np.where(d < ln, t, 0)
        wr = w * (0.25 + 0.75 * t)
        a = np.maximum(a, t ** 1.5 * np.exp(-((r - rr) / np.maximum(wr, 1e-3)) ** 2) * (d < ln))
        a = np.maximum(a, np.exp(-((d + 0.02) / 0.05) ** 2) * (d < 0.3) * np.exp(-((r - rr) / (w * 2.2)) ** 2) * 0.9)   # bright head
    a += 0.18 * np.exp(-((r - r0) / 0.012) ** 2)         # a faint guide ring
    a *= np.clip((1 - r) * 12, 0, 1)
    return a


def halo(n=256):
    S = 3
    im = Image.new("L", (n * S, n * S), 0)
    d = ImageDraw.Draw(im)
    c = n * S / 2
    R = c - 3 * S
    rng = random.Random(9)

    def ring(f, w):
        d.ellipse([c - R * f, c - R * f, c + R * f, c + R * f], outline=255, width=int(w * S))
    ring(0.99, 2.4); ring(0.93, 1.0); ring(0.70, 2.0); ring(0.66, 1.0)
    for i in range(36):
        a = math.tau * i / 36
        cx, cy = math.cos(a), math.sin(a)
        for _ in range(3):
            x0, y0 = rng.uniform(-0.5, 0.5), rng.uniform(-1, 1)
            x1, y1 = rng.uniform(-0.5, 0.5), rng.uniform(-1, 1)
            pts = []
            for x, y in ((x0, y0), (x1, y1)):
                lx, ly = x * 0.045, -y * 0.055
                rr = 0.815 + ly
                pts.append((c + R * (cx * rr - cy * lx), c + R * (cy * rr + cx * lx)))
            d.line(pts, fill=255, width=int(1.4 * S))
    for i in range(12):
        a = math.tau * i / 12
        d.line([(c + R * 0.66 * math.cos(a), c + R * 0.66 * math.sin(a)), (c + R * 0.7 * math.cos(a), c + R * 0.7 * math.sin(a))], fill=255, width=int(2 * S))
    m = np.asarray(im.resize((n, n), Image.LANCZOS), dtype=np.float32) / 255
    g = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(4)), dtype=np.float32) / 255
    return m + g * 0.8


def sphere(w=256, h=128):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = xx / w
    v = yy / h
    lat = np.exp(-(((v * 8) % 1.0 - 0.5) / 0.045) ** 2) * 0.55
    lon = np.exp(-(((u * 16) % 1.0 - 0.5) / 0.03) ** 2) * 0.35
    rng = np.random.RandomState(4)
    glints = np.zeros((h, w), dtype=np.float32)
    for _ in range(40):
        gx, gy = rng.randint(0, w), rng.randint(0, h)
        glints += np.exp(-(((xx - gx) / 3.0) ** 2 + ((yy - gy) / 3.0) ** 2)) * rng.uniform(0.4, 1)
    pole = np.clip(np.sin(v * math.pi) * 1.6, 0, 1)
    return (lat + lon + glints) * pole * 0.9


def skin(n=64):
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    lat = np.clip(1 - np.abs(((xx + yy) % 8) - 4) / 1.2, 0, 1) * 0.35
    lat += np.clip(1 - np.abs(((xx - yy) % 8) - 4) / 1.2, 0, 1) * 0.25
    rng = np.random.RandomState(12)
    sp = np.zeros((n, n), dtype=np.float32)
    for _ in range(60):
        sp[rng.randint(0, n), rng.randint(0, n)] = rng.uniform(0.6, 1.0)
    return np.clip(lat + sp + 0.12, 0, 1)


def main():
    os.makedirs(OUT, exist_ok=True)
    save(comet_ring(256, 7, 1), "seal_ring_a.png")
    save(comet_ring(256, 12, 2, 0.84, 0.035), "seal_ring_b.png")
    save(comet_ring(256, 5, 3, 0.88, 0.06), "seal_ring_c.png")
    save(halo(), "seal_halo.png")
    save(sphere(), "seal_sphere.png")
    save(skin(), "seal_skin.png")
    print("wrote aura textures to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()

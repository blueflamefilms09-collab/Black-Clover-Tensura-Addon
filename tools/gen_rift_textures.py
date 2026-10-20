"""Reproducible hi-res textures of the mega-rare tear (1.99.0): python3 -B tools/gen_rift_textures.py

  assets/nusmp/textures/misc/rift/core.png     1024 x 1024  a tileable nebula with veins (the skybox-style core; tinted in game by the pocket's sky colour)
  assets/nusmp/textures/misc/rift/crack.png    1024 x 1024  tileable bright cracks (Voronoi edges) on transparent
  assets/nusmp/textures/misc/rift/shimmer.png   512 x  512  tileable soft ripples (the haze round the tear)
  assets/nusmp/textures/misc/rift/spark.png     128 x  128  a soft round glow with a four-point star (the strands and the rim)

Fixed seeds, numpy FFT noise (periodic, so every texture tiles by construction), no time or machine dependence: the same bytes every run.
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "misc", "rift")


def fractal(n, seed, beta=2.2):
    """Periodic 1/f^beta noise in 0..1."""
    rng = np.random.default_rng(seed)
    f = np.fft.fftfreq(n)
    k = np.sqrt(f[:, None] ** 2 + f[None, :] ** 2)
    k[0, 0] = 1.0
    spec = (rng.normal(size=(n, n)) + 1j * rng.normal(size=(n, n))) / k ** (beta / 2.0)
    spec[0, 0] = 0
    a = np.real(np.fft.ifft2(spec))
    a -= a.min()
    return a / a.max()


def voronoi_edges(n, cells, seed):
    rng = np.random.default_rng(seed)
    pts = rng.random((cells, 2)) * n
    y, x = np.mgrid[0:n, 0:n]
    d1 = np.full((n, n), 1e9)
    d2 = np.full((n, n), 1e9)
    for px, py in pts:
        best = np.full((n, n), 1e9)
        for ox in (-n, 0, n):
            for oy in (-n, 0, n):
                best = np.minimum(best, np.hypot(x - (px + ox), y - (py + oy)))
        nd1 = np.minimum(d1, best)
        d2 = np.minimum(d2, np.maximum(d1, best))
        d1 = nd1
    return d2 - d1


def save(name, arr):
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, name), optimize=True)


def core():
    n = 1024
    a = fractal(n, 1999, 2.4)
    b = fractal(n, 2024, 1.6)
    veins = 1.0 - np.abs(2.0 * fractal(n, 77, 2.0) - 1.0)
    veins = np.clip((veins - 0.8) * 5.0, 0, 1)
    v = np.clip(0.25 + 0.7 * a ** 1.6 + 0.25 * b * a, 0, 1)
    stars = (fractal(n, 5, 0.4) > 0.9985).astype(float)
    lum = np.clip(v + 0.9 * veins + stars, 0, 1)
    rgb = np.stack([lum * 0.85 + 0.15 * veins, lum * 0.9, lum], axis=-1)
    alpha = np.clip(0.35 + 0.65 * lum, 0, 1)
    save("core.png", np.dstack([rgb * 255, alpha * 255]))


def crack():
    n = 1024
    e = voronoi_edges(n, 70, 31)
    line = np.clip(1.0 - e / 3.2, 0, 1) ** 1.6
    warp = fractal(n, 9, 2.0)
    line = line * (0.55 + 0.45 * warp)
    rgb = np.stack([np.ones((n, n)), 0.96 * np.ones((n, n)), 0.9 * np.ones((n, n))], axis=-1) * np.clip(line * 1.4, 0, 1)[..., None]
    save("crack.png", np.dstack([rgb * 255, np.clip(line * 1.2, 0, 1) * 255]))


def shimmer():
    n = 512
    a = fractal(n, 404, 2.0)
    y, x = np.mgrid[0:n, 0:n] / n
    rip = 0.5 + 0.5 * np.sin(2 * np.pi * (6 * a + 3 * x + 2 * y))
    s = np.clip(rip * a * 1.4, 0, 1)
    rgb = np.dstack([np.ones((n, n)), np.ones((n, n)), np.ones((n, n))]) * s[..., None]
    save("shimmer.png", np.dstack([rgb * 255, s * 190]))


def spark():
    n = 128
    y, x = np.mgrid[0:n, 0:n]
    r = np.hypot(x - n / 2 + 0.5, y - n / 2 + 0.5) / (n / 2)
    glow = np.clip(1 - r, 0, 1) ** 2.2
    arm = np.clip(1 - np.minimum(np.abs(x - n / 2 + 0.5), np.abs(y - n / 2 + 0.5)) / 5.0, 0, 1) * np.clip(1 - r, 0, 1)
    s = np.clip(glow + 0.6 * arm, 0, 1)
    rgb = np.dstack([np.ones((n, n))] * 3) * s[..., None]
    save("spark.png", np.dstack([rgb * 255, s * 255]))


if __name__ == "__main__":
    core()
    crack()
    shimmer()
    spark()
    print("wrote 4 textures to", os.path.normpath(OUT))

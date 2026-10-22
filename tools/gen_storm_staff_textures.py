"""Textures of the Storm Magic effects (the Stormcaller Staff): the cloud puff, the rain curtain, the bolt strip, the hail stone, the surge wave and the funnel band.

    python3 -B tools/gen_storm_staff_textures.py            writes src/main/resources/assets/nusmp/textures/particle/storm_*.png (deterministic)

All are luminance + alpha (mode LA), like the other effect textures of the mod: the layer tints them with the vertex colour. Where a texture is drawn additively (the bolt) the
colour under the transparent texels is black, so it can never show as a square (docs/vfx_square_bug.md); the alpha-blended ones (cloud, rain, wave, funnel, hail) carry colour
everywhere so their soft edges have no dark fringe.
"""
import os

import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")


def smooth(x, a, b):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def value_noise(rng, size, cells, wrap_x=False, wrap_y=False):
    """Smooth value noise over `size` x `size` with `cells` lattice cells; optionally tileable."""
    g = rng.rand(cells + 1, cells + 1)
    if wrap_x:
        g[:, -1] = g[:, 0]
    if wrap_y:
        g[-1, :] = g[0, :]
    xs = np.linspace(0, cells, size, endpoint=False)
    x0 = np.floor(xs).astype(int)
    f = xs - x0
    f = f * f * (3 - 2 * f)
    a = g[x0][:, x0] * (1 - f)[None, :] * (1 - f)[:, None] + g[x0][:, x0 + 1] * f[None, :] * (1 - f)[:, None]
    b = g[x0 + 1][:, x0] * (1 - f)[None, :] * f[:, None] + g[x0 + 1][:, x0 + 1] * f[None, :] * f[:, None]
    return a + b


def save(name, lum, alpha):
    arr = np.stack([np.clip(lum, 0, 1), np.clip(alpha, 0, 1)], axis=-1)
    Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "LA").save(os.path.join(OUT, name + ".png"))


def cloud():
    """A billowing cumulonimbus puff: overlapping lobes with ragged edges, light on top, dark in the belly."""
    n = 128
    rng = np.random.RandomState(1101)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    field = np.zeros((n, n))
    lobes = [(24, 92, 17), (48, 96, 20), (74, 97, 20), (100, 92, 17), (36, 72, 18), (62, 74, 22), (90, 72, 18), (50, 52, 17), (76, 54, 17), (62, 36, 13), (14, 84, 10), (114, 84, 10)]
    for cx, cy, r in lobes:
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / r
        field += np.exp(-(d ** 2) * 1.9)
    ragged = value_noise(rng, n, 8) * 0.55 + value_noise(rng, n, 16) * 0.3 + value_noise(rng, n, 32) * 0.15
    field = field * (0.6 + 0.8 * ragged)
    alpha = smooth(field, 0.42, 0.9) * 0.96
    # flat belly: the lower edge is squarer than the top, like the base of a storm cloud
    belly = smooth(yy, 118, 100)
    alpha *= 0.55 + 0.45 * belly
    top = 1.0 - yy / n
    lum = 0.42 + 0.5 * top ** 1.2 + 0.18 * (ragged - 0.5)
    # a lighter rim where the lobes meet the sky
    rim = smooth(field, 0.5, 0.9) * (1 - smooth(field, 0.9, 1.7))
    lum = lum + 0.12 * rim
    save("storm_cloud", lum, alpha)


def rain():
    """A tileable rain curtain: thin slanted streaks of different length and strength (the V axis tiles)."""
    w, h = 64, 128
    rng = np.random.RandomState(2202)
    lum = np.zeros((h, w))
    alpha = np.zeros((h, w))
    for _ in range(46):
        x = rng.randint(0, w)
        y0 = rng.randint(0, h)
        length = rng.randint(14, 44)
        strength = 0.35 + 0.65 * rng.rand()
        width = 1 if rng.rand() < 0.75 else 2
        slant = 0.12
        for k in range(length):
            t = k / length
            a = strength * (1 - abs(2 * t - 1) ** 1.6)
            yy = (y0 + k) % h
            xx = int(round(x + slant * k)) % w
            for dx in range(width):
                alpha[yy, (xx + dx) % w] = max(alpha[yy, (xx + dx) % w], a)
                lum[yy, (xx + dx) % w] = 0.92
    # a faint mist behind the streaks
    mist = value_noise(rng, 64, 4, True, True)
    mist = np.tile(mist, (2, 1))[:h, :w]
    alpha = np.maximum(alpha, 0.05 * mist)
    lum = np.where(alpha > 0, np.maximum(lum, 0.7), 0.7)
    save("storm_rain", lum, alpha)


def bolt():
    """A lightning strip, U along the length: a white-hot core, a blue halo, black under the transparent texels (it is drawn additively)."""
    w, h = 256, 32
    rng = np.random.RandomState(3303)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    c = (h - 1) / 2.0
    d = np.abs(yy - c)
    core = np.exp(-((d / 1.6) ** 2))
    halo = np.exp(-((d / 7.0) ** 2)) * 0.7
    pulse = 0.88 + 0.12 * np.sin(xx / 256.0 * np.pi * 10)
    alpha = np.clip(core + halo, 0, 1) * pulse
    lum = np.clip(core * 1.0 + halo * 0.55, 0, 1)
    lum = np.where(alpha > 0.02, lum, 0.0)                       # black under the transparent texels
    # the ends fade so a chain of strips has no seam of full brightness at the join
    endfade = smooth(xx, 0, 6) * smooth(255 - xx, 0, 6)
    save("storm_bolt", lum, alpha * (0.55 + 0.45 * endfade))


def hail():
    """A hail stone: a faceted ice nugget with a bright facet and a dark underside."""
    n = 32
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    cx = cy = (n - 1) / 2.0
    ang = np.arctan2(yy - cy, xx - cx)
    rad = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    r_edge = 10.5 + 1.4 * np.cos(ang * 5 + 0.7) + 0.8 * np.cos(ang * 3)
    alpha = 1.0 - smooth(rad, r_edge - 1.2, r_edge + 0.6)
    lum = 0.78 + 0.18 * np.clip((cx - xx + (cy - yy)) / 14.0, -1, 1)
    # facets
    lum = lum - 0.10 * (np.sin(ang * 3 + 1.0) > 0.4) - 0.08 * (rad > r_edge - 3)
    # a glint
    lum = lum + 0.25 * np.exp(-(((xx - 11.5) ** 2 + (yy - 11.5) ** 2) / 6.0))
    save("storm_hail", lum, alpha)


def wave():
    """A storm-surge wall seen from the side: a body of water with a ragged foaming crest, translucent at the foot. U along the wall, V up (0 at the top)."""
    w, h = 128, 64
    rng = np.random.RandomState(4404)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    n1 = value_noise(rng, 128, 10, True, False)[0, :w]
    n2 = value_noise(rng, 128, 24, True, False)[0, :w]
    top = 8 + 9 * n1 + 4 * n2                                    # the ragged top edge row for each column
    below = yy - top[None, :]
    body = smooth(below, -1.0, 2.0)
    foot = 1.0 - smooth(yy, 52, 63)                              # fades into the ground
    alpha = body * foot * 0.82
    depth = np.clip(below / 40.0, 0, 1)
    foam = np.exp(-np.abs(below - 2.5) / 3.0) * (0.6 + 0.4 * n2[None, :])
    lum = 0.62 - 0.22 * depth + 0.38 * foam
    streak = value_noise(rng, 128, 20, True, False)[:h, :w]
    lum = lum + 0.06 * (streak - 0.5)
    alpha = np.maximum(alpha, foam * 0.5 * body)
    save("storm_wave", lum, alpha)


def funnel():
    """A funnel band, tileable along U: dark swirling bands with lighter streaks and a few flecks of debris."""
    w, h = 256, 64
    rng = np.random.RandomState(5505)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float64)
    n = value_noise(rng, 256, 12, True, False)[:h, :w]
    bands = 0.5 + 0.5 * np.sin((xx / w) * np.pi * 8 + yy * 0.18 + n * 4.0)
    edge = smooth(yy, 0, 14) * smooth(h - 1 - yy, 0, 14)
    alpha = edge * (0.55 + 0.4 * bands)
    lum = 0.30 + 0.45 * bands * n
    for _ in range(26):                                             # debris flecks
        x, y = rng.randint(0, w), rng.randint(14, h - 14)
        for dx in range(-1, 2):
            for dy in range(-1, 1):
                alpha[y + dy, (x + dx) % w] = 0.95
                lum[y + dy, (x + dx) % w] = 0.12
    save("storm_funnel", lum, alpha)


def main():
    os.makedirs(OUT, exist_ok=True)
    for fn in (cloud, rain, bolt, hail, wave, funnel):
        fn()
        print("wrote storm_%s.png" % fn.__name__)


if __name__ == "__main__":
    main()

"""Generates the textures of the "Roblox style" VFX toolkit (vfx/client/RobloxStyle.java, docs/vfx_roblox_pass.md).

The toolkit draws a few real meshes (scrolling-UV swooshes, expanding shock rings, cones / vortices, a flip-book sprite sheet, a core + shell glow, tapered trails, a ground decal ring) instead of
many camera-facing sprites. Every texture here is made for that:

  rbx_swoosh      64x128   U across the ribbon (soft edge, hot centre line), V along it, tileable in V: flowing streaks. For swooshes, slashes, trails, beams.
  rbx_bolt        32x128   a thin white-hot core line inside a soft shell, flickering along V (tileable in V). For lightning and beam cores.
  rbx_swirl       128x128  tileable both ways: diagonal helical streaks. Wrapped round a cone / vortex it reads as wind or water spinning (scroll U to spin it, V to run it up).
  rbx_ring        128x64   a shock-ring band: U runs round the ring (tileable), V from the inner edge (0) to the outer edge (1): a slow rise to a hard bright leading edge, then a quick fade.
  rbx_decal_ring  128x128  a flat ground decal: a thin bright ring, a faint inner ring and tick marks, round mask, clear corners.
  rbx_glow        64x64    the core / shell glow: a hot centre, a long smooth falloff that reaches exactly zero at the rim.
  rbx_streak      16x64    a stretched spark: V = 0 the bright head, V = 1 the tail fading to nothing.
  rbx_flip_burst  256x256  a 4x4 flip-book (16 frames of 64 px): flash -> star rays and a ring racing out -> thin ring and sparks -> nothing.
  rbx_flip_fire   256x256  a 4x4 flip-book: a billowing puff of fire / smoke that boils, rises and dissolves from the edges.
  rbx_rock        64x64    an opaque tileable stone: darker cracks, lighter facets (for the cone spikes, alpha-blended and tinted by the layer).

Look: Roblox VFX artists make "GIF" effects out of layered textured meshes and flip-book particles (docs/vfx_roblox_pass.md has the sources). So alpha falls off smoothly to EXACTLY 0 before every
edge of every sprite (no square halo, docs/vfx_square_bug.md), the colour is white (the vertex colour tints it) and is defined under alpha 0 as well (no dark fringe under linear filtering).
Deterministic (fixed seeds, numpy only + Pillow).

    python tools/gen_roblox_vfx_textures.py
Output: src/main/resources/assets/anime_multiverse/textures/particle/rbx_*.png
"""
import os

import numpy as np
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "particle")
PREFIX = "rbx_"
# name -> (width, height); RobloxStyle.TEXTURE_NAMES and RobloxStyleAssetTest read the same list
SIZES = {
    "swoosh": (64, 128),
    "bolt": (32, 128),
    "swirl": (128, 128),
    "ring": (128, 64),
    "decal_ring": (128, 128),
    "glow": (64, 64),
    "streak": (16, 64),
    "flip_burst": (256, 256),
    "flip_fire": (256, 256),
    "rock": (64, 64),
}


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def lattice(w, h, gx, gy, seed):
    """Value noise on a gx x gy lattice that wraps in both directions (so the image tiles), 0..1."""
    rng = np.random.RandomState(seed)
    g = rng.rand(gy, gx)
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float64)
    fx, fy = (xs + 0.5) / w * gx, (ys + 0.5) / h * gy
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = smooth(0, 1, fx - x0), smooth(0, 1, fy - y0)
    x0, y0, x1, y1 = x0 % gx, y0 % gy, (x0 + 1) % gx, (y0 + 1) % gy
    a, b = g[y0, x0] * (1 - tx) + g[y0, x1] * tx, g[y1, x0] * (1 - tx) + g[y1, x1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, gx, gy, seed, octaves=4):
    out, amp, tot = np.zeros((h, w)), 1.0, 0.0
    for o in range(octaves):
        out += amp * lattice(w, h, gx * 2 ** o, gy * 2 ** o, seed + 31 * o)
        tot += amp
        amp *= 0.5
    return out / tot


def save(name, alpha, lum=None):
    """White RGB (optionally shaded by lum 0..1) with the given alpha, both 0..1, shape (h, w). The colour is kept under alpha 0 too."""
    w, h = SIZES[name][0], SIZES[name][1]
    assert alpha.shape == (h, w), (name, alpha.shape)
    if lum is None:
        lum = np.ones_like(alpha)
    c = np.clip(lum, 0, 1) * 255
    rgba = np.stack([c, c, c, np.clip(alpha, 0, 1) * 255], axis=-1).round().astype(np.uint8)
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, PREFIX + name + ".png"))


def coords(name):
    w, h = SIZES[name]
    ys, xs = np.mgrid[0:h, 0:w].astype(np.float64)
    return (xs + 0.5) / w, (ys + 0.5) / h


# ---------------------------------------------------------------------------------------------------- ribbons
def swoosh():
    u, v = coords("swoosh")
    w, h = SIZES["swoosh"]
    prof = np.sin(np.pi * u) ** 1.6                                     # 0 at both edges, soft shoulders
    # flowing streaks: sheared, tileable noise (integer lattice, wraps in V); streaks are long along V
    n1 = fbm(w, h, 3, 2, 11, 3)
    n2 = lattice(w, h, 7, 1, 23)
    streak = 0.35 + 0.65 * smooth(0.25, 0.8, 0.6 * n1 + 0.4 * n2)
    core = np.exp(-((u - 0.5) / 0.09) ** 2)
    a = prof * (0.55 * streak + 0.7 * core)
    a = np.where((u < 1.5 / w) | (u > 1 - 1.5 / w), 0, a)
    save("swoosh", a, 0.72 + 0.28 * core)


def bolt():
    u, v = coords("bolt")
    w, h = SIZES["bolt"]
    flick = 0.8 + 0.2 * lattice(w, h, 1, 8, 5)
    core = np.exp(-((u - 0.5) / 0.07) ** 2)
    shell = np.exp(-((u - 0.5) / 0.26) ** 2) * 0.55
    a = np.clip(core * 1.2 + shell, 0, 1) * flick * np.sin(np.pi * u) ** 0.6
    a = np.where((u < 1.5 / w) | (u > 1 - 1.5 / w), 0, a)
    save("bolt", a, 0.8 + 0.2 * core)


def swirl():
    u, v = coords("swirl")
    w, h = SIZES["swirl"]
    arms, twist = 5, 2                                                   # integers: tiles in both directions
    ph = 2 * np.pi * (arms * u + twist * v)
    band = 0.5 + 0.5 * np.sin(ph + 1.4 * np.sin(2 * np.pi * (u * 2 - v)))
    n = fbm(w, h, 4, 4, 41, 3)
    a = smooth(0.35, 0.95, band) * (0.45 + 0.55 * n)
    save("swirl", a * 0.9, 0.7 + 0.3 * band)


def ring():
    u, v = coords("ring")
    w, h = SIZES["ring"]
    rise = smooth(0.0, 0.86, v) ** 2.2                                   # slow rise from the inner edge
    edge = np.exp(-((v - 0.88) / 0.05) ** 2)                             # the hard bright leading edge
    drop = 1 - smooth(0.9, 1.0, v)                                       # then gone before the outer edge
    n = lattice(w, h, 16, 2, 77)
    a = (0.55 * rise * (0.6 + 0.4 * n) + edge * 0.95) * drop
    a = np.where(v > 1 - 1.0 / h, 0, a)
    a = np.where(v < 1.0 / h, 0, a)
    save("ring", a, 0.65 + 0.35 * edge)


def decal_ring():
    u, v = coords("decal_ring")
    x, y = u * 2 - 1, v * 2 - 1
    r = np.hypot(x, y)
    th = np.arctan2(y, x)
    main = np.exp(-((r - 0.80) / 0.035) ** 2)
    soft = np.exp(-((r - 0.80) / 0.11) ** 2) * 0.35
    inner = np.exp(-((r - 0.56) / 0.02) ** 2) * 0.5
    ticks = (np.cos(th * 24) > 0.82).astype(np.float64) * smooth(0.60, 0.66, r) * (1 - smooth(0.72, 0.76, r)) * 0.55
    a = np.clip(main + soft + inner + ticks, 0, 1) * (1 - smooth(0.90, 0.97, r))
    a = np.where(r > 0.97, 0, a)
    save("decal_ring", a, 0.7 + 0.3 * main)


def glow():
    u, v = coords("glow")
    r = np.hypot(u * 2 - 1, v * 2 - 1)
    fall = (1 - smooth(0.0, 1.0, r)) ** 2.2
    hot = np.exp(-(r / 0.16) ** 2) * 0.6
    a = np.clip(fall * 0.85 + hot, 0, 1) * (1 - smooth(0.88, 1.0, r))
    a = np.where(r >= 0.99, 0, a)
    save("glow", a, 0.8 + 0.2 * hot)


def streak():
    u, v = coords("streak")
    prof = np.exp(-((u - 0.5) / 0.22) ** 2) * np.sin(np.pi * u) ** 0.5
    along = (1 - smooth(0.0, 1.0, v)) ** 1.6 * smooth(0.0, 0.06, v)      # bright just behind the head, long tail
    a = prof * along
    a = np.where((u < 1.0 / 16) | (u > 1 - 1.0 / 16) | (v > 1 - 1.0 / 64), 0, a)
    save("streak", a, 0.8 + 0.2 * along)


# ---------------------------------------------------------------------------------------------------- flip-books
def flip_cell(kind, f, n, seed):
    """One 64x64 cell, frame f of n; returns (alpha, lum)."""
    s = 64
    ys, xs = np.mgrid[0:s, 0:s].astype(np.float64)
    x, y = (xs + 0.5) / s * 2 - 1, (ys + 0.5) / s * 2 - 1
    r, th = np.hypot(x, y), np.arctan2(y, x)
    p = f / (n - 1)
    mask = 1 - smooth(0.80, 0.94, r)                                     # the cell border is clear: no bleeding into the next frame
    if kind == "burst":
        e = 1 - (1 - p) ** 3
        flash = np.exp(-(r / (0.18 + 0.2 * p)) ** 2) * (1 - smooth(0.0, 0.55, p))
        rr = 0.12 + 0.72 * e
        thick = 0.12 * (1 - p) + 0.02
        ringv = np.exp(-((r - rr) / thick) ** 2) * (1 - smooth(0.35, 1.0, p))
        rays = np.clip(np.cos(th * 8) * 0.5 + 0.5, 0, 1) ** 6 * np.exp(-(r / (0.2 + 0.35 * e)) ** 2) * (1 - smooth(0.0, 0.42, p))
        sp = (np.cos(th * 13 + 1.0) > 0.9).astype(np.float64) * np.exp(-((r - rr * 0.8) / 0.04) ** 2) * (1 - smooth(0.2, 0.8, p))
        a = np.clip(flash * 1.1 + ringv + rays * 0.9 + sp, 0, 1)
        lum = 0.75 + 0.25 * np.clip(flash + rays, 0, 1)
    else:                                                                # fire puff
        t = p
        warp = fbm(s, s, 3, 3, seed + 3, 3)
        n = fbm(s, s, 4, 4, seed + 9 + int(f * 0.0), 4)
        yy = y + 0.55 * t - 0.35                                         # rises
        body = 1 - np.hypot(x * (1.1 + 0.4 * t), yy) / (0.55 + 0.25 * np.sin(np.pi * min(1.0, t * 1.1)))
        dens = np.clip(body + (n - 0.5) * 0.9 * (0.4 + t) + (warp - 0.5) * 0.4, 0, 1)
        erode = 0.05 + 0.75 * t                                          # dissolves from the edges
        a = smooth(erode, erode + 0.35, dens) * (1 - 0.8 * smooth(0.55, 1.0, t))
        lum = np.clip(0.55 + 0.6 * dens * (1 - t), 0, 1)
    a = a * mask
    a = np.where(r > 0.94, 0, a)
    return a, lum


def flipbook(name, kind, seed):
    w, h = SIZES[name]
    A, L = np.zeros((h, w)), np.ones((h, w))
    for i in range(16):
        cx, cy = (i % 4) * 64, (i // 4) * 64
        a, lum = flip_cell(kind, i, 16, seed)
        A[cy:cy + 64, cx:cx + 64] = a
        L[cy:cy + 64, cx:cx + 64] = lum
    save(name, A, L)


def rock():
    w, h = SIZES["rock"]
    n = fbm(w, h, 4, 4, 91, 4)
    cr = lattice(w, h, 8, 8, 17)
    crack = (np.abs(cr - 0.5) < 0.035).astype(np.float64)
    facet = np.floor(n * 5) / 5
    lum = 0.42 + 0.5 * facet + 0.12 * (n - 0.5)
    lum = lum * (1 - 0.45 * crack)
    save("rock", np.ones((h, w)), np.clip(lum, 0.18, 0.95))


def main():
    swoosh()
    bolt()
    swirl()
    ring()
    decal_ring()
    glow()
    streak()
    flipbook("flip_burst", "burst", 100)
    flipbook("flip_fire", "fire", 200)
    rock()
    print("wrote", len(SIZES), "textures to", os.path.abspath(OUT))


if __name__ == "__main__":
    main()

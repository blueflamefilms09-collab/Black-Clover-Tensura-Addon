#!/usr/bin/env python3
"""
High-resolution weapon skins (1.3.0).

tools/gen_weapon_models.py paints each weapon's atlas on a 128x128 grid. The renderer divides the mesh UVs by 128 (client/WeaponRenderer),
so the PNG may be any size: this script raises every weapon skin (and its _glow layer) to 512x512, and Elsdocia to 1024x1024 with a
real faceted blue-crystal blade, without touching the meshes.

  python tools/hires_weapon_textures.py            every skin in assets/nusmp/textures/entity/weapon that is still 128x128
  python tools/hires_weapon_textures.py elsdocia   only those ids

The 128 originals are kept in tools/weapon_tex_128/ (nothing is thrown away) and the run is repeatable: a freshly generated 128 skin is
raised again, an already raised one is skipped. gen_weapon_models.py calls process() for every skin it writes, so new weapons get it too.
"""
import os
import sys

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "entity", "weapon")
SRC = os.path.join(HERE, "weapon_tex_128")
SPECIAL = {"elsdocia": 8}          # id -> scale; everything else 4
DEFAULT_SCALE = 4


def _fill_transparent(rgb, alpha, passes=6):
    """Spread the colour of opaque texels into the transparent ones so the resample has no dark fringe."""
    rgb = rgb.copy()
    have = alpha > 0
    for _ in range(passes):
        if have.all():
            break
        acc = np.zeros_like(rgb)
        cnt = np.zeros(have.shape, np.float32)
        for dy, dx in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            h = np.roll(have, (dy, dx), (0, 1))
            c = np.roll(rgb, (dy, dx), (0, 1))
            acc += c * h[..., None]
            cnt += h
        grow = (~have) & (cnt > 0)
        rgb[grow] = acc[grow] / cnt[grow][:, None]
        have = have | grow
    return rgb


def _noise(n, sigma, seed):
    rng = np.random.default_rng(seed)
    img = Image.fromarray((rng.random((n, n)) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(sigma))
    a = np.asarray(img, np.float32)
    return (a - a.mean()) / (a.std() + 1e-6)


def upscale(im, k, seed=1, grain=True):
    """128 pixel-art atlas -> k times larger: smoothed stair-steps, re-sharpened edges, fine surface grain."""
    a = np.asarray(im.convert("RGBA"), np.float32)
    rgb, alpha = a[..., :3], a[..., 3]
    n = im.width * k
    rgb = _fill_transparent(rgb, alpha)
    big = Image.fromarray(rgb.astype(np.uint8)).resize((n, n), Image.NEAREST)
    big = big.filter(ImageFilter.GaussianBlur(k * 0.34)).filter(ImageFilter.UnsharpMask(radius=max(1.0, k * 0.55), percent=110, threshold=0))
    out = np.asarray(big, np.float32)
    if grain:
        lum = out.mean(axis=2, keepdims=True)
        fine = _noise(n, 0.8, 11 + seed)[..., None] * 3.2
        mid = _noise(n, k * 1.6, 23 + seed)[..., None] * 4.5
        out = out + (fine + mid) * (0.55 + lum / 400.0)
    al = Image.fromarray(alpha.astype(np.uint8)).resize((n, n), Image.BILINEAR)
    al = np.asarray(al, np.float32)
    binary = np.isin(np.unique(alpha), (0, 255)).all()
    if binary:
        al = np.where(al > 127, 255, 0)
    res = np.dstack([np.clip(out, 0, 255), al]).astype(np.uint8)
    return Image.fromarray(res, "RGBA")


# ------------------------------------------------------------------------------------------------ the Elsdocia crystal
def _voronoi(n, cell, seed, stretch=1.0):
    """Jittered-grid Voronoi on an n x n texture (cells `stretch` times longer along y): F1, F2 (pixels) and the id of the nearest cell."""
    rng = np.random.default_rng(seed)
    g = n // cell + 3
    px = (np.arange(g)[None, :] + rng.random((g, g)) * 0.85 + 0.075) * cell - cell
    py = ((np.arange(g)[:, None] + rng.random((g, g)) * 0.85 + 0.075) * cell - cell)
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    ys = ys / stretch
    ci, cj = (ys // cell).astype(int) + 1, (xs // cell).astype(int) + 1
    f1 = np.full((n, n), 1e9, np.float32)
    f2 = np.full((n, n), 1e9, np.float32)
    ident = np.zeros((n, n), np.int64)
    for di in (-1, 0, 1):
        for dj in (-1, 0, 1):
            i, j = np.clip(ci + di, 0, g - 1), np.clip(cj + dj, 0, g - 1)
            d = np.hypot(xs - px[i, j], ys - py[i, j])
            closer = d < f1
            f2 = np.where(closer, f1, np.minimum(f2, d))
            ident = np.where(closer, i * g + j, ident)
            f1 = np.where(closer, d, f1)
    return f1, f2, ident


def _hash01(ids, salt):
    x = (ids.astype(np.int64) * 73856093 ^ (salt * 19349663)) & 0xFFFFFF
    x = (x * 2654435761) & 0xFFFFFF
    return (x % 10007) / 10007.0


def crystal(n, glow=False, seed=5):
    """The blue-crystal blade of the Black Clover movie's Elsdocia: long shattered shards lit from within, white-hot cores, a few bright
    fracture lines, a pale rim. Returns the n x n RGB."""
    cell = max(20, n // 30)
    f1, f2, ident = _voronoi(n, cell, seed, 2.4)
    edge = (f2 - f1)
    ys, xs = np.mgrid[0:n, 0:n].astype(np.float32)
    tilt = _hash01(ident, 3)
    cell_hot = _hash01(ident, 17)
    low = _noise(n, n / 45, seed + 31)
    towards = np.clip(f1 / cell, 0, 1.2)                         # 0 at a shard's heart, 1 at its rim
    deep = np.array((22, 84, 226), np.float32)
    mid = np.array((64, 156, 255), np.float32)
    cyan = np.array((150, 226, 255), np.float32)
    white = np.array((240, 252, 255), np.float32)
    k = np.clip(tilt * 0.95 + low * 0.12 + (1 - towards) * 0.25, 0, 1)[..., None]
    col = np.where(k < 0.5, deep + (mid - deep) * (k / 0.5), mid + (cyan - mid) * ((k - 0.5) / 0.5))
    hot = (cell_hot > 0.84)[..., None]
    core = np.clip(1.0 - towards, 0, 1)[..., None]
    col = np.where(hot, cyan + (white - cyan) * core, col)
    # light seen through the shard: brighter towards the heart, a little deeper at the rim
    col = col * (0.82 + 0.30 * core)
    # only some borders read as fractures
    pick = (_hash01(ident, 29) > 0.45)[..., None]
    crack = np.clip(1.0 - edge / max(2.0, n / 420), 0, 1)[..., None] * pick
    glint = np.clip(1.0 - np.abs(edge - n / 260) / max(1.5, n / 600), 0, 1)[..., None] * pick
    col = col * (1 - 0.35 * crack) + white * 0.55 * glint
    # brighten the blade's long axis, fade to deep blue toward the cross-guard end (the lower half of the atlas strip is the root)
    if glow:
        strength = np.clip(0.30 + 0.85 * hot[..., 0] + glint[..., 0] * 0.9 + core[..., 0] * 0.35, 0, 1)
        col = col * strength[..., None]
    return np.clip(col, 0, 255)


def build_elsdocia(img128, glow):
    """Raise the 128 Elsdocia atlas to 1024: the bronze / gold parts are smoothed up as usual, the blue blade is repainted as crystal."""
    k = SPECIAL["elsdocia"]
    base = upscale(img128, k, seed=3, grain=not glow)
    a = np.asarray(img128.convert("RGBA"), np.float32)
    r, g, b, al = a[..., 0], a[..., 1], a[..., 2], a[..., 3]
    blue = ((b >= r) & (b > 110) & (al > 0)).astype(np.float32)             # the crystal: blue through white, not the bronze or the dark clover cut-out
    closed = Image.fromarray((blue * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.MinFilter(3))   # one-pixel dark specks are crystal too
    blue = np.asarray(closed, np.float32) / 255.0
    n = img128.width * k
    m = Image.fromarray((blue * 255).astype(np.uint8)).resize((n, n), Image.NEAREST).filter(ImageFilter.GaussianBlur(k * 0.5))
    m = (np.asarray(m, np.float32) / 255.0 > 0.5)
    cr = crystal(n, glow=glow, seed=5)
    out = np.asarray(base, np.float32).copy()
    out[..., :3] = np.where(m[..., None], cr, out[..., :3])
    if glow:
        out[..., 3] = np.where(m, np.maximum(out[..., 3], 255), out[..., 3])
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------ driver
def process(path):
    """Raise one skin PNG in place if it is still 128 wide (keeps the original in tools/weapon_tex_128/). Returns True when changed."""
    im = Image.open(path)
    if im.width != 128:
        return False
    os.makedirs(SRC, exist_ok=True)
    keep = os.path.join(SRC, os.path.basename(path))
    im.save(keep)
    name = os.path.basename(path)[:-4]
    glow = name.endswith("_glow")
    wid = name[:-5] if glow else name
    if wid in SPECIAL and wid == "elsdocia":
        res = build_elsdocia(im, glow)
    else:
        res = upscale(im, DEFAULT_SCALE, seed=(sum(map(ord, wid)) % 50) + 1, grain=not glow)
    res.save(path, optimize=True)
    return True


def main(argv):
    ids = set(argv[1:])
    done = 0
    for f in sorted(os.listdir(OUT)):
        if not f.endswith(".png"):
            continue
        name = f[:-4]
        wid = name[:-5] if name.endswith("_glow") else name
        if ids and wid not in ids:
            continue
        if process(os.path.join(OUT, f)):
            done += 1
            print("raised", f)
    print(done, "skins raised")


if __name__ == "__main__":
    main(sys.argv)

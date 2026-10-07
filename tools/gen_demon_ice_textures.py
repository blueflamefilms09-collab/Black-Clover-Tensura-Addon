"""Generates the Demon Ice Magic (Demon Frost) VFX textures. Deterministic (fixed seeds), no fonts, no external images.

    python3 -B tools/gen_demon_ice_textures.py            # all textures
    python3 -B tools/gen_demon_ice_textures.py lance wall  # only the named ones

Output: src/main/resources/assets/nusmp/textures/particle/demon_ice_*.png

Look: corrupted ice, black glassy crystal (obsidian body, bright facet edges) with violet-blue light leaking out of its seams,
frost that crawls as ferns and cracks, black cold fog, angular frost runes. Every crystal sprite is grey-scale + alpha where the
grey level is the FACET SHADE: drawn with ALPHA and a near-black tint it is a black crystal, drawn again with ADD and a blue / violet
tint only its bright edges and seams light up, so one sprite gives the body and the glow.
Colour convention of the rest of textures/particle: white or grey + alpha, tinted by the vertex colour. The one exception is
demon_ice_invert, whose RGB (not alpha) is the mask because it is drawn with the NEGATIVE blend.

Textures (size): lance 64x256 (projectile body, head up) | spray 64x256 (frost trail, head up) | crystal 128x256 (fused cluster, base
down) | shards 256x64 (atlas, four 64x64 fragments) | wall 512x128 (tileable crystal rim + frost veil) | runes 512x64 (tileable rune
ribbon) | sigil 512 (ground circle of runes) | flake 512 (frost-fern star) | cracks 512 (frozen sheet with fracture network) |
mist 256 (cold fog puff) | burst 256 (shatter star) | ring 256 (shock ring with frost teeth) | glint 64 (diamond glint) |
invert 128 (NEGATIVE mask).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4  # supersampling for clean edges


# ------------------------------------------------------------------------------------------------ plumbing
def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def blur(a, sigma):
    """Gaussian blur of a float image (periodic, via FFT). sigma in pixels."""
    if sigma <= 0:
        return a
    h, w = a.shape
    fy, fx = np.fft.fftfreq(h)[:, None], np.fft.fftfreq(w)[None, :]
    g = np.exp(-2 * (math.pi * sigma) ** 2 * (fx * fx + fy * fy))
    return np.real(np.fft.ifft2(np.fft.fft2(a) * g)).astype(np.float32)


def box_down(a, w, h):
    """Exact area average of a supersampled array down to (h, w)."""
    sh, sw = a.shape[0] // h, a.shape[1] // w
    return a.reshape(h, sh, w, sw).mean(axis=(1, 3))


def value_noise(w, h, cell, seed, tile=True):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cell), max(1, h // cell)
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    if tile:
        g[-1, :] = g[0, :]
        g[:, -1] = g[:, 0]
    xx, yy = grid(w, h)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def aniso_noise(w, h, cw, ch, seed, tile=True):
    """Value noise with different cell sizes along x and y (streaks)."""
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cw), max(1, h // ch)
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    if tile:
        g[-1, :] = g[0, :]
        g[:, -1] = g[:, 0]
    xx, yy = grid(w, h)
    fx, fy = xx / w * gw, yy / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, cell, seed, octaves=5, tile=True):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + value_noise(w, h, max(2, cell >> o), seed + 17 * o, tile) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def bilinear(a, x, y):
    h, w = a.shape
    x0, y0 = np.floor(x).astype(int), np.floor(y).astype(int)
    tx, ty = x - x0, y - y0
    x0, y0 = x0 % w, y0 % h
    x1, y1 = (x0 + 1) % w, (y0 + 1) % h
    return (a[y0, x0] * (1 - tx) + a[y0, x1] * tx) * (1 - ty) + (a[y1, x0] * (1 - tx) + a[y1, x1] * tx) * ty


def warped_fbm(w, h, cell, seed, amount, octaves=5):
    """Domain-warped fbm: curling, smoky structure."""
    base = fbm(w, h, cell, seed, octaves)
    wx, wy = fbm(w, h, cell, seed + 101, 3), fbm(w, h, cell, seed + 202, 3)
    xx, yy = grid(w, h)
    return bilinear(base, xx + (wx - 0.5) * 2 * amount, yy + (wy - 0.5) * 2 * amount)


def finish(lum, alpha, name, bleed=True):
    """Writes a grey-scale + alpha sprite. Transparent texels take the colour of their nearest opaque neighbours (no dark fringes)."""
    lum, alpha = np.clip(lum, 0, 1), np.clip(alpha, 0, 1)
    if bleed:
        wa = blur(lum * alpha, 2.0) / np.maximum(blur(alpha, 2.0), 1e-3)
        lum = lum * alpha + np.clip(wa, 0, 1) * (1 - alpha)
    g = (np.clip(lum, 0, 1) * 255 + 0.5).astype(np.uint8)
    img = np.dstack([g, g, g, (alpha * 255 + 0.5).astype(np.uint8)])
    save(Image.fromarray(img, "RGBA"), name)


# ------------------------------------------------------------------------------------------------ the crystal renderer
def render_facets(w, h, facets, seed, mottle=0.07, ridge=0.85, rim=0.7, glow_fn=None, grad=0.35, fractures=0, fr_box=None, grain=0.09):
    """Faceted black glass. facets = [(polygon in output pixels, shade 0..1), ...] painted in order (later on top).
    Each facet gets a gentle gradient (lighter toward the top) and fine grain; seams between facets catch light in proportion to how
    different the two facets are (a sharp cut edge glints, a soft one does not), the outline glints on the lit facets only, and thin
    fracture lines run inside the glass. glow_fn(xs, ys) -> 0..1 adds inner light (output pixel coordinates). Returns (lum, alpha)."""
    S = SS
    rng = np.random.default_rng(seed)
    idm = Image.new("L", (w * S, h * S), 0)
    lm = Image.new("L", (w * S, h * S), 0)
    di, dl = ImageDraw.Draw(idm), ImageDraw.Draw(lm)
    for k, (poly, shade) in enumerate(facets, start=1):
        pts = [(x * S, y * S) for x, y in poly]
        di.polygon(pts, fill=k)
        dl.polygon(pts, fill=int(np.clip(shade, 0, 1) * 255))
    ids = np.asarray(idm).astype(np.int16)
    lum = np.asarray(lm).astype(np.float32) / 255
    cov = (ids > 0).astype(np.float32)
    # seams: strength by contrast; outline: strength by how lit the facet is
    e_int = np.zeros_like(cov)
    e_rim = np.zeros_like(cov)
    for dy, dx in ((0, 1), (1, 0), (0, -1), (-1, 0)):
        sh = np.roll(ids, (dy, dx), (0, 1))
        shl = np.roll(lum, (dy, dx), (0, 1))
        diff = ids != sh
        both = diff & (ids > 0) & (sh > 0)
        e_int = np.maximum(e_int, both * np.clip(np.abs(lum - shl) * 3.2, 0.18, 1.0))
        e_rim = np.maximum(e_rim, (diff & (ids > 0) & (sh == 0)) * (0.2 + 0.9 * lum))
    e_int = np.clip(blur(e_int.astype(np.float32), 1.1) * 3.0, 0, 1) * cov
    e_rim = np.clip(blur(e_rim.astype(np.float32), 1.1) * 3.0, 0, 1) * cov
    # per-facet gradient (lighter toward the top of each facet)
    for k, (poly, shade) in enumerate(facets, start=1):
        xs_ = [p[0] for p in poly]
        ys_ = [p[1] for p in poly]
        x0, x1 = max(0, int(min(xs_) * S) - 1), min(w * S, int(max(xs_) * S) + 2)
        y0, y1 = max(0, int(min(ys_) * S) - 1), min(h * S, int(max(ys_) * S) + 2)
        if x1 <= x0 or y1 <= y0:
            continue
        m = ids[y0:y1, x0:x1] == k
        v = (np.arange(y0, y1, dtype=np.float32)[:, None] - y0) / max(1, y1 - y0 - 1)
        lum[y0:y1, x0:x1] = np.where(m, lum[y0:y1, x0:x1] * (1 + grad * (0.5 - v)), lum[y0:y1, x0:x1])
    xs, ys = grid(w * S, h * S)
    n = fbm(w * S, h * S, max(8, int(26 * S)), seed, 4, tile=False)
    g = fbm(w * S, h * S, max(4, int(2.2 * S)), seed + 7, 3, tile=False)
    lum = lum * (0.85 + 0.3 * n) + mottle * (n - 0.5) + grain * (g - 0.5)
    if fractures:
        fr = Image.new("L", (w * S, h * S), 0)
        df = ImageDraw.Draw(fr)
        bx0, by0, bx1, by1 = fr_box or (0, 0, w, h)
        for _ in range(fractures):
            x, y = rng.uniform(bx0, bx1), rng.uniform(by0, by1)
            ang = math.radians(rng.choice([-1, 1]) * rng.uniform(8, 40) + 90 * rng.integers(0, 2))
            ln = rng.uniform(4, 14)
            pts = [(x, y)]
            for _s in range(3):
                ang += rng.uniform(-0.5, 0.5)
                x, y = x + math.cos(ang) * ln / 3, y + math.sin(ang) * ln / 3
                pts.append((x, y))
            df.line([(px * S, py * S) for px, py in pts], fill=int(rng.uniform(90, 200)), width=max(1, int(0.6 * S)))
        lum = lum + 0.55 * blur(np.asarray(fr).astype(np.float32) / 255, 0.7) * cov
    if glow_fn is not None:
        lum = lum + glow_fn(xs / S, ys / S) * cov
    lum = np.clip(lum + ridge * e_int + rim * e_rim * (1 - 0.6 * e_int), 0, 1)
    a = box_down(cov, w, h)
    l = box_down(lum * cov, w, h) / np.maximum(a, 1e-4)
    return l, a


def crystal(cx, y0, width, height, lean=0.0, tip_skew=0.0, tipfrac=0.28, shades=(0.22, 0.38, 0.14), tipshades=(0.5, 0.66, 0.3), chisel=0.0):
    """A pointed hexagonal crystal prism seen from the side: base centre (cx, y0), the tip at y0 - height. lean shears the whole
    prism sideways by that many pixels at the tip, tip_skew shifts the apex relative to the shaft, chisel flattens the apex (0..1).
    Three visible faces (left lit, centre, right shadowed) and three tip facets: returns a list of (polygon, shade)."""
    wl, wc, wr = 0.30 * width, 0.42 * width, 0.28 * width
    xs = [cx - width / 2, cx - width / 2 + wl, cx - width / 2 + wl + wc, cx + width / 2]
    ys_ = y0 - height * (1 - tipfrac)
    def sh(x, y):
        return x + lean * (y0 - y) / height
    tipx, tipy = cx + tip_skew, y0 - height
    out = []
    for k in range(3):
        x0, x1 = xs[k], xs[k + 1]
        out.append(([(sh(x0, y0), y0), (sh(x1, y0), y0), (sh(x1, ys_), ys_), (sh(x0, ys_), ys_)], shades[k]))
    for k in range(3):
        x0, x1 = xs[k], xs[k + 1]
        if chisel > 0.05:
            half = (xs[3] - xs[0]) * 0.5 * (1 - chisel)
            tx0 = tipx - half + (xs[k] - xs[0]) / (xs[3] - xs[0]) * 2 * half
            tx1 = tipx - half + (xs[k + 1] - xs[0]) / (xs[3] - xs[0]) * 2 * half
            out.append(([(sh(x0, ys_), ys_), (sh(x1, ys_), ys_), (sh(tx1, tipy), tipy), (sh(tx0, tipy), tipy)], tipshades[k]))
        else:
            out.append(([(sh(x0, ys_), ys_), (sh(x1, ys_), ys_), (sh(tipx, tipy), tipy)], tipshades[k]))
    return out


# ================================================================================================ the projectile
def lance(w=64, h=256):
    """Frost Lance: a long black crystal spear, head up. A double-edged blade with two pairs of barbs, a hexagonal shaft with growth
    collars and thorns, a tail that splinters and dissolves."""
    cx = w / 2
    f = []
    # ---- the blade: a ridge down the middle, two bevel facets per side, barbs
    tip = (cx + 0.4, 2)
    R = [(cx + 4.5, 20), (cx + 8, 38), (cx + 11, 54), (cx + 16.5, 66), (cx + 11.5, 67.5), (cx + 12.5, 76), (cx + 18.5, 90), (cx + 11, 91), (cx + 8, 102)]
    L = [(cx - 4, 19), (cx - 7, 37), (cx - 10, 53), (cx - 15.5, 64), (cx - 10.5, 66), (cx - 11.5, 75), (cx - 17.5, 88), (cx - 10, 90), (cx - 7.5, 101)]
    ridge_pts = [(cx + 0.2, 2), (cx + 0.5, 22), (cx + 0.8, 52), (cx + 1.0, 78), (cx + 0.6, 104)]
    # right side (lit second): upper bevel, mid bevel, lower bevel, barbs
    f.append(([tip, R[0], (cx + 0.5, 22)], 0.62))
    f.append(([(cx + 0.5, 22), R[0], R[1], R[2], (cx + 0.8, 52)], 0.52))
    f.append(([(cx + 0.8, 52), R[2], R[3], R[4], R[5], (cx + 1.0, 78)], 0.44))
    f.append(([(cx + 1.0, 78), R[5], R[6], R[7], R[8], (cx + 0.6, 104)], 0.36))
    f.append(([R[3], R[4], (cx + 14, 70.5)], 0.22))
    f.append(([R[6], R[7], (cx + 15.5, 94)], 0.20))
    # left side (the lit one)
    f.append(([tip, L[0], (cx + 0.5, 22)], 0.74))
    f.append(([(cx + 0.5, 22), L[0], L[1], L[2], (cx + 0.8, 52)], 0.64))
    f.append(([(cx + 0.8, 52), L[2], L[3], L[4], L[5], (cx + 1.0, 78)], 0.55))
    f.append(([(cx + 1.0, 78), L[5], L[6], L[7], L[8], (cx + 0.6, 104)], 0.46))
    f.append(([L[3], L[4], (cx - 13, 69)], 0.30))
    f.append(([L[6], L[7], (cx - 14.5, 93)], 0.28))
    # ---- the shaft: hexagonal prism tapering
    def shaft(y0, y1, hw0, hw1):
        l0, l1 = cx - hw0, cx - hw1
        r0, r1 = cx + hw0, cx + hw1
        c0l, c0r = cx - hw0 * 0.30, cx + hw0 * 0.38
        c1l, c1r = cx - hw1 * 0.30, cx + hw1 * 0.38
        return [([(l0, y0), (c0l, y0), (c1l, y1), (l1, y1)], 0.36), ([(c0l, y0), (c0r, y0), (c1r, y1), (c1l, y1)], 0.22),
                ([(c0r, y0), (r0, y0), (r1, y1), (c1r, y1)], 0.12)]
    f += shaft(100, 205, 8.5, 5.5)
    for y, wd in ((120, 12.5), (148, 11.5), (173, 10.5), (195, 8.5)):          # growth collars: flat spiky diamonds
        f.append(([(cx - wd, y), (cx, y - 4.5), (cx + wd, y), (cx, y + 4.5)], 0.46))
        f.append(([(cx - wd, y), (cx, y + 4.5), (cx, y + 1.0)], 0.52))
        f.append(([(cx + wd, y), (cx, y + 4.5), (cx, y + 1.0)], 0.16))
    # thorns off the shaft
    f.append(([(cx + 6, 117), (cx + 7, 131), (cx + 22, 101), (cx + 12, 112)], 0.34))
    f.append(([(cx + 6, 117), (cx + 7, 131), (cx + 13, 116)], 0.14))
    f.append(([(cx - 6, 146), (cx - 6.5, 159), (cx - 23, 129), (cx - 12, 142)], 0.52))
    f.append(([(cx - 6, 146), (cx - 6.5, 159), (cx - 12, 144)], 0.26))
    f.append(([(cx + 5, 177), (cx + 5.5, 187), (cx + 18, 161), (cx + 10, 173)], 0.32))
    # ---- the tail: the shaft thins out and splinters
    f += shaft(205, 232, 5.5, 2.6)
    for (px, py, s, sh) in ((cx - 9, 222, 6, 0.50), (cx + 9, 236, 5, 0.40), (cx - 5, 246, 4, 0.34), (cx + 3, 252, 2.5, 0.28)):
        f.append(([(px, py - s * 1.6), (px + s * 0.55, py), (px, py + s * 0.6), (px - s * 0.55, py)], sh))
    cxl = cx

    def glow(xs, ys):
        core = np.exp(-((xs - cxl) / 2.6) ** 2) * (0.22 + 0.5 * smooth(50, 100, ys) * (1 - smooth(120, 205, ys)))
        return core * (1 - smooth(215, 250, ys))
    lum, a = render_facets(w, h, f, 1101, mottle=0.08, ridge=0.85, rim=0.75, glow_fn=glow, fractures=26, fr_box=(cx - 14, 10, cx + 14, 100))
    yy = np.arange(h, dtype=np.float32)[:, None]
    a = a * np.clip(1 - smooth(214, 252, yy), 0, 1) ** 0.7
    finish(lum, a, "demon_ice_lance")


def shards(w=256, h=64):
    """Atlas of four 64x64 fragments (u = k/4 .. (k+1)/4): a long splinter, a broken wedge, a stubby crystal and a thin flake."""
    S = 64
    lum_all = np.zeros((h, w), np.float32)
    a_all = np.zeros((h, w), np.float32)
    cells = []
    # 0: long splinter (diagonal)
    cells.append([([(8, 58), (14, 54), (56, 6), (60, 4), (50, 16), (20, 60)], 0.30), ([(14, 54), (56, 6), (60, 4), (26, 58), (20, 60)], 0.55),
                  ([(8, 58), (14, 54), (20, 60)], 0.20)])
    # 1: broken wedge
    cells.append([([(10, 50), (22, 14), (44, 8), (56, 40), (40, 56)], 0.26), ([(22, 14), (44, 8), (36, 34)], 0.62), ([(44, 8), (56, 40), (36, 34)], 0.40),
                  ([(10, 50), (22, 14), (36, 34)], 0.46), ([(10, 50), (36, 34), (40, 56)], 0.20), ([(36, 34), (56, 40), (40, 56)], 0.14)])
    # 2: stubby crystal
    cells.append(crystal(32, 56, 26, 48, lean=4, tip_skew=2, tipfrac=0.34))
    # 3: thin flake (a hexagonal plate on edge)
    cells.append([([(32, 6), (46, 22), (46, 44), (32, 58), (18, 44), (18, 22)], 0.30), ([(32, 6), (46, 22), (32, 30), (18, 22)], 0.70),
                  ([(32, 30), (46, 22), (46, 44), (32, 58)], 0.20), ([(32, 30), (18, 22), (18, 44), (32, 58)], 0.40)])
    for k, fs in enumerate(cells):
        lum, a = render_facets(S, S, fs, 1200 + k, mottle=0.06, ridge=0.85, rim=0.8, fractures=3, fr_box=(14, 14, 50, 50))
        lum_all[:, k * S:(k + 1) * S] = lum
        a_all[:, k * S:(k + 1) * S] = a
    finish(lum_all, a_all, "demon_ice_shards")


def crystal_cluster(w=128, h=256):
    """A fused cluster of black crystals thrusting out of the ground (base down), frost crust and splinters round the foot."""
    rng = np.random.default_rng(1301)
    f = []
    base = h - 10
    spec = [(-26, 150, 30, -9, -5, 0.30), (30, 120, 26, 12, 4, 0.30), (-48, 82, 20, -6, -3, 0.34), (50, 70, 18, 8, 2, 0.34),
            (-10, 205, 38, 5, 3, 0.26), (14, 170, 24, -3, -1, 0.3)]
    spec.sort(key=lambda s: -s[1])                                   # tall ones first (behind), short ones in front
    cxm = w / 2
    for dx, hh, ww, lean, skew, tf in spec:
        f += crystal(cxm + dx * 0.7, base + (0 if abs(dx) < 20 else -3), ww, hh, lean=lean, tip_skew=skew, tipfrac=tf)
    # foot splinters
    for i in range(7):
        x = cxm + rng.uniform(-52, 52)
        hh = rng.uniform(10, 26)
        f += crystal(x, base + 4, rng.uniform(5, 9), hh, lean=rng.uniform(-5, 5), tipfrac=0.4, shades=(0.2, 0.34, 0.12))
    cxl = cxm

    def glow(xs, ys):
        up = np.exp(-((base - ys) / 55.0) ** 2) * 0.42
        return up * (0.5 + 0.5 * np.exp(-((xs - cxl) / 46.0) ** 2))
    lum, a = render_facets(w, h, f, 1302, mottle=0.08, ridge=0.85, rim=0.75, glow_fn=glow, fractures=60, fr_box=(10, 40, 118, 240))
    yy = np.arange(h, dtype=np.float32)[:, None]
    foot = 1 - smooth(h - 14, h - 2, yy)                                  # the foot dissolves into frost
    nz = fbm(w, h, 10, 1303, 4, tile=False)
    a = a * np.clip(foot * 1.4 + (nz - 0.35) * 1.5, 0, 1) * (1 - smooth(h - 4, h, yy) * 0)
    finish(lum, a, "demon_ice_crystal")


# ================================================================================================ drawing helpers
class Pen:
    """Supersampled white pen on an L canvas, coordinates in output pixels."""
    def __init__(self, w, h, ss=SS):
        self.w, self.h, self.ss = w, h, ss
        self.im = Image.new("L", (w * ss, h * ss), 0)
        self.d = ImageDraw.Draw(self.im)

    def poly(self, pts, v=255):
        self.d.polygon([(x * self.ss, y * self.ss) for x, y in pts], fill=int(v))

    def line(self, a, b, width, v=255):
        (x0, y0), (x1, y1) = a, b
        dx, dy = x1 - x0, y1 - y0
        ln = math.hypot(dx, dy)
        if ln < 1e-6:
            return
        nx, ny = -dy / ln * width / 2, dx / ln * width / 2
        self.poly([(x0 + nx, y0 + ny), (x1 + nx, y1 + ny), (x1 - nx, y1 - ny), (x0 - nx, y0 - ny)], v)

    def taper(self, a, b, w0, w1, v=255):
        (x0, y0), (x1, y1) = a, b
        dx, dy = x1 - x0, y1 - y0
        ln = math.hypot(dx, dy)
        if ln < 1e-6:
            return
        nx, ny = -dy / ln, dx / ln
        self.poly([(x0 + nx * w0 / 2, y0 + ny * w0 / 2), (x1 + nx * w1 / 2, y1 + ny * w1 / 2),
                   (x1 - nx * w1 / 2, y1 - ny * w1 / 2), (x0 - nx * w0 / 2, y0 - ny * w0 / 2)], v)

    def circle(self, c, r, width, v=255):
        s = self.ss
        self.d.ellipse([(c[0] - r) * s, (c[1] - r) * s, (c[0] + r) * s, (c[1] + r) * s], outline=int(v), width=max(1, int(width * s)))

    def disc(self, c, r, v=255):
        s = self.ss
        self.d.ellipse([(c[0] - r) * s, (c[1] - r) * s, (c[0] + r) * s, (c[1] + r) * s], fill=int(v))

    def array(self):
        return box_down(np.asarray(self.im).astype(np.float32) / 255, self.w, self.h)


def glowed(a, sigma, gain):
    """The sprite plus a soft halo of itself (built-in glow, so one quad carries it)."""
    return np.clip(a + gain * blur(a, sigma), 0, 1)


# ================================================================================================ rim wall, runes, sigil, flake
def wall(w=512, h=128):
    """Tileable rim: a row of black crystal spikes (two depths) standing on a frost crust, under a veil of cold light that streaks up
    and fades. Alpha pass dark = the black wall, ADD pass = veil and seam glow from the same pixels."""
    rng = np.random.default_rng(1401)
    f = []

    def ring_add(cx, y0, wd, hh, **k):
        for off in (-w, 0, w):
            f.extend(crystal(cx + off, y0, wd, hh, **k))
    base = h - 6
    # back row (darker, taller, wide-set), front row (smaller, brighter): evenly spread with jitter so the tile closes
    n_back, n_front = 9, 15
    for i in range(n_back):
        cx = (i + 0.5) * w / n_back + rng.uniform(-9, 9)
        ring_add(cx, base - 6, rng.uniform(22, 34), rng.uniform(68, 104), lean=rng.uniform(-10, 10), tip_skew=rng.uniform(-3, 3), tipfrac=rng.uniform(0.26, 0.4),
                 shades=(0.17, 0.28, 0.10), tipshades=(0.38, 0.5, 0.22))
    for i in range(n_front):
        cx = (i + 0.3) * w / n_front + rng.uniform(-8, 8)
        ring_add(cx, base + 2, rng.uniform(10, 20), rng.uniform(22, 56), lean=rng.uniform(-8, 8), tip_skew=rng.uniform(-2, 2), tipfrac=rng.uniform(0.3, 0.45),
                 chisel=0.0 if rng.random() > 0.2 else 0.5)
    lum, a = render_facets(w, h, f, 1402, mottle=0.08, ridge=0.8, rim=0.7,
                           glow_fn=lambda xs, ys: np.exp(-((h - ys) / 38.0) ** 2) * 0.34, fractures=70, fr_box=(0, 30, w, h - 6))
    # frost crust along the foot and the veil above the spikes
    xx, yy = grid(w, h)
    nz = fbm(w, h, 24, 1403, 4)
    crust = np.clip(smooth(h - 17, h - 3, yy) * (0.55 + nz), 0, 1)
    streak = aniso_noise(w, h, 5, 64, 1404) * 0.6 + aniso_noise(w, h, 11, 40, 1405) * 0.4
    veil_a = np.clip((smooth(0, 70, yy) ** 1.3) * (0.35 + 1.1 * streak) * 0.28, 0, 0.5)
    veil_l = np.clip(0.55 + 0.6 * streak, 0, 1)
    A = np.maximum(a, veil_a)
    Lm = np.where(a > 0.05, lum * a + veil_l * (1 - a), veil_l)
    A = np.maximum(A, crust * 0.8)
    Lm = np.where(crust > a, np.maximum(Lm, 0.35 + 0.4 * nz), Lm)
    finish(Lm, A, "demon_ice_wall", bleed=False)


RUNES = [
    [((0.3, 1), (0.3, 0)), ((0.3, 0.0), (0.78, 0.3)), ((0.3, 0.38), (0.78, 0.68))],
    [((0.25, 1), (0.25, 0)), ((0.25, 0), (0.75, 0.28)), ((0.75, 0.28), (0.75, 1))],
    [((0.3, 0), (0.3, 1)), ((0.3, 0.2), (0.78, 0.5)), ((0.78, 0.5), (0.3, 0.8))],
    [((0.3, 0), (0.3, 1)), ((0.3, 0.05), (0.78, 0.32)), ((0.3, 0.4), (0.78, 0.67))],
    [((0.5, 1), (0.5, 0)), ((0.5, 0.5), (0.2, 0.12)), ((0.5, 0.5), (0.8, 0.12))],
    [((0.72, 0), (0.28, 0.36)), ((0.28, 0.36), (0.72, 0.66)), ((0.72, 0.66), (0.28, 1))],
    [((0.5, 1), (0.5, 0)), ((0.2, 0.32), (0.5, 0)), ((0.8, 0.32), (0.5, 0))],
    [((0.2, 0), (0.2, 1)), ((0.8, 0), (0.8, 1)), ((0.2, 0), (0.8, 1)), ((0.8, 0), (0.2, 1))],
    [((0.5, 0), (0.22, 0.3)), ((0.5, 0), (0.78, 0.3)), ((0.22, 0.3), (0.78, 0.3)), ((0.3, 0.3), (0.16, 1)), ((0.7, 0.3), (0.84, 1))],
    [((0.15, 0.08), (0.85, 0.92)), ((0.85, 0.08), (0.15, 0.92))],
    [((0.5, 0), (0.5, 1)), ((0.2, 0.5), (0.5, 0.18)), ((0.5, 0.18), (0.8, 0.5)), ((0.8, 0.5), (0.5, 0.82)), ((0.5, 0.82), (0.2, 0.5))],
    [((0.3, 0), (0.3, 1)), ((0.3, 0.5), (0.8, 0.0)), ((0.3, 0.5), (0.8, 1))],
]


def draw_rune(pen, k, cx, cy, gw, gh, width, rot=0.0):
    """Rune k centred on (cx, cy), glyph box gw x gh, stems along the box's v axis, rotated by rot radians."""
    ca, sa = math.cos(rot), math.sin(rot)

    def P(u, v):
        x, y = (u - 0.5) * gw, (v - 0.5) * gh
        return (cx + x * ca - y * sa, cy + x * sa + y * ca)
    for (a, b) in RUNES[k % len(RUNES)]:
        pen.line(P(*a), P(*b), width)


def runes(w=512, h=64):
    """Tileable ribbon of angular frost runes between two rails, icicle fringe underneath. Drawn white, glows."""
    pen = Pen(w, h)
    rng = np.random.default_rng(1501)
    pen.line((0, 7), (w, 7), 2.2)
    pen.line((0, h - 8), (w, h - 8), 2.2)
    pen.line((0, 11), (w, 11), 0.9, 150)
    pen.line((0, h - 12), (w, h - 12), 0.9, 150)
    n = 16
    for i in range(n):
        cx = (i + 0.5) * w / n
        draw_rune(pen, int(rng.integers(0, len(RUNES))), cx, h / 2, 17, 28, 2.6)
        if i % 2 == 0:                                                                   # separator diamond
            x = cx + w / n / 2
            pen.poly([(x, h / 2 - 7), (x + 4, h / 2), (x, h / 2 + 7), (x - 4, h / 2)], 230)
        else:
            x = cx + w / n / 2
            pen.line((x, 14), (x, h - 14), 1.4, 170)
    for i in range(64):                                                                  # frost teeth on both rails
        x = (i + 0.5) * w / 64
        lh = 3 + 5 * ((i * 7) % 5) / 4
        pen.poly([(x - 2.2, h - 8), (x + 2.2, h - 8), (x, h - 8 + lh)], 255)
        pen.poly([(x - 1.6, 7), (x + 1.6, 7), (x, 7 - lh * 0.7)], 230)
    a = pen.array()
    out = glowed(a, 2.2, 0.55)
    finish(0.55 + 0.45 * a, out, "demon_ice_runes", bleed=False)


def sigil(size=512):
    """Ground circle of frost runes: toothed outer ring, a band of 24 runes, a dashed ring, a hexagram on diamond nodes."""
    S = size
    c = (S / 2, S / 2)
    R = S / 2
    pen = Pen(S, S)
    rng = np.random.default_rng(1601)
    pen.circle(c, R * 0.975, 2.4)
    pen.circle(c, R * 0.94, 1.1)
    for i in range(144):                                                                 # tick ring
        a = 2 * math.pi * i / 144
        r0, r1 = R * 0.948, R * (0.968 if i % 6 else 0.972)
        pen.line((c[0] + math.cos(a) * r0, c[1] + math.sin(a) * r0), (c[0] + math.cos(a) * r1, c[1] + math.sin(a) * r1), 1.0 if i % 6 else 1.8)
    for i in range(48):                                                                  # icicle teeth pointing inward
        a0, a1 = 2 * math.pi * (i - 0.22) / 48, 2 * math.pi * (i + 0.22) / 48
        am = 2 * math.pi * i / 48
        depth = R * (0.075 if i % 2 == 0 else 0.04)
        r0 = R * 0.94
        pen.poly([(c[0] + math.cos(a0) * r0, c[1] + math.sin(a0) * r0), (c[0] + math.cos(a1) * r0, c[1] + math.sin(a1) * r0),
                  (c[0] + math.cos(am) * (r0 - depth), c[1] + math.sin(am) * (r0 - depth))], 255)
    pen.circle(c, R * 0.80, 1.6)
    pen.circle(c, R * 0.765, 1.0)
    N = 24
    for i in range(N):                                                                   # the rune band, glyph stems radial
        a = 2 * math.pi * (i + 0.5) / N
        rr = R * 0.85
        draw_rune(pen, int(rng.integers(0, len(RUNES))), c[0] + math.cos(a) * rr, c[1] + math.sin(a) * rr, R * 0.062, R * 0.105, 2.9, a - math.pi / 2)
    for i in range(N):
        a = 2 * math.pi * i / N
        pen.line((c[0] + math.cos(a) * R * 0.80, c[1] + math.sin(a) * R * 0.80), (c[0] + math.cos(a) * R * 0.90, c[1] + math.sin(a) * R * 0.90), 0.9, 160)
    pen.circle(c, R * 0.70, 1.3)
    for i in range(36):                                                                  # dashed ring
        a0, a1 = 2 * math.pi * i / 36, 2 * math.pi * (i + (0.7 if i % 3 else 0.4)) / 36
        for t in range(6):
            aa, bb = a0 + (a1 - a0) * t / 6, a0 + (a1 - a0) * (t + 1) / 6
            pen.line((c[0] + math.cos(aa) * R * 0.66, c[1] + math.sin(aa) * R * 0.66), (c[0] + math.cos(bb) * R * 0.66, c[1] + math.sin(bb) * R * 0.66), 3.0)
    hexa = [(c[0] + math.cos(math.radians(-90 + 60 * i)) * R * 0.70, c[1] + math.sin(math.radians(-90 + 60 * i)) * R * 0.70) for i in range(6)]
    for i in range(6):                                                                   # hexagram
        pen.line(hexa[i], hexa[(i + 2) % 6], 1.5, 220)
    for (x, y) in hexa:                                                                  # diamond nodes
        pen.poly([(x, y - 11), (x + 7, y), (x, y + 11), (x - 7, y)], 255)
    pen.circle(c, R * 0.30, 1.4, 200)
    pen.circle(c, R * 0.265, 0.9, 150)
    for i in range(6):
        a = math.radians(-90 + 60 * i)
        pen.poly([(c[0] + math.cos(a - 0.12) * R * 0.30, c[1] + math.sin(a - 0.12) * R * 0.30), (c[0] + math.cos(a + 0.12) * R * 0.30, c[1] + math.sin(a + 0.12) * R * 0.30),
                  (c[0] + math.cos(a) * R * 0.40, c[1] + math.sin(a) * R * 0.40)], 255)
    a = pen.array()
    out = glowed(a, 3.0, 0.5)
    finish(0.6 + 0.4 * a, out, "demon_ice_sigil", bleed=False)


def flake(size=512):
    """Frost-fern star: six dendrite arms with paired side branches and barbed spear tips, six short thorns between them, a hexagon
    heart. White."""
    S = size
    c = (S / 2, S / 2)
    R = S / 2
    pen = Pen(S, S)
    rng = np.random.default_rng(1701)

    def branch(p0, ang, length, w0, w1, depth, v=255):
        p1 = (p0[0] + math.cos(ang) * length, p0[1] + math.sin(ang) * length)
        pen.taper(p0, p1, w0, w1, v)
        if depth <= 0:
            return p1
        for frac in (0.28, 0.5, 0.72):
            q = (p0[0] + math.cos(ang) * length * frac, p0[1] + math.sin(ang) * length * frac)
            bl = length * (0.42 - 0.3 * frac) * (1.0 if depth > 1 else 0.8)
            for sgn in (-1, 1):
                branch(q, ang + sgn * math.radians(58), bl, max(1.2, w0 * 0.55), 0.6, depth - 1, v)
        return p1
    for i in range(6):
        a = math.radians(-90 + 60 * i)
        tip = branch(c, a, R * 0.74, R * 0.034, R * 0.012, 2)
        # barbed spear tip
        for sgn in (-1, 1):
            b = (tip[0] + math.cos(a + sgn * 2.5) * R * 0.07, tip[1] + math.sin(a + sgn * 2.5) * R * 0.07)
            pen.poly([tip, b, (tip[0] + math.cos(a) * R * 0.012, tip[1] + math.sin(a) * R * 0.012)], 255)
        pen.poly([(tip[0] + math.cos(a) * R * 0.12, tip[1] + math.sin(a) * R * 0.12), (tip[0] + math.cos(a + 1.57) * R * 0.025, tip[1] + math.sin(a + 1.57) * R * 0.025),
                  (tip[0] - math.cos(a) * R * 0.01, tip[1] - math.sin(a) * R * 0.01), (tip[0] + math.cos(a - 1.57) * R * 0.025, tip[1] + math.sin(a - 1.57) * R * 0.025)], 255)
        m = (c[0] + math.cos(a) * R * 0.46, c[1] + math.sin(a) * R * 0.46)                # gem node on the arm
        pen.poly([(m[0] + math.cos(a) * 15, m[1] + math.sin(a) * 15), (m[0] + math.cos(a + 1.57) * 8, m[1] + math.sin(a + 1.57) * 8),
                  (m[0] - math.cos(a) * 15, m[1] - math.sin(a) * 15), (m[0] + math.cos(a - 1.57) * 8, m[1] + math.sin(a - 1.57) * 8)], 255)
        b = a + math.radians(30)                                                         # thorn between arms
        branch(c, b, R * (0.34 if i % 2 == 0 else 0.5), R * 0.022, R * 0.004, 1, 235)
    hexa = [(c[0] + math.cos(math.radians(30 + 60 * i)) * R * 0.13, c[1] + math.sin(math.radians(30 + 60 * i)) * R * 0.13) for i in range(6)]
    pen.poly(hexa, 120)
    for i in range(6):
        pen.line(hexa[i], hexa[(i + 1) % 6], 3.0)
        pen.line(hexa[i], hexa[(i + 2) % 6], 1.4, 200)
    pen.circle(c, R * 0.19, 1.6, 220)
    a = pen.array()
    out = glowed(a, 3.5, 0.55)
    finish(0.55 + 0.45 * a, out, "demon_ice_flake", bleed=False)


# ================================================================================================ ground, fog, bursts
def cracks(size=512):
    """Frozen black sheet with a fracture network: a jagged frost boundary, a crazed Voronoi crack mesh thickest at the centre, main
    cracks running out of the centre. Lines are bright (lum 1) on a dark sheet (lum 0.15)."""
    S = size
    rng = np.random.default_rng(1801)
    xx, yy = grid(S, S)
    cx = cy = (S - 1) / 2
    dx, dy = (xx - cx) / cx, (yy - cy) / cy
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    # voronoi mesh, seeds denser toward the centre
    pts = []
    for _ in range(95):
        rr = rng.random() ** 1.5 * 0.95
        a = rng.random() * 2 * math.pi
        pts.append((cx + math.cos(a) * rr * cx, cy + math.sin(a) * rr * cy))
    f1 = np.full((S, S), 1e9, np.float32)
    f2 = np.full((S, S), 1e9, np.float32)
    for (px, py) in pts:
        d = np.sqrt((xx - px) ** 2 + (yy - py) ** 2)
        f2 = np.minimum(f2, np.maximum(f1, d))
        f1 = np.minimum(f1, d)
    edge = f2 - f1
    wmesh = 1.6 + 1.2 * fbm(S, S, 64, 1802, 3, tile=False)
    mesh = np.exp(-(edge / wmesh) ** 2)
    drop = smooth(0.42, 0.62, fbm(S, S, 48, 1803, 3, tile=False) + 0.55 * (r - 0.35))   # the mesh breaks up toward the rim
    mesh = mesh * (1 - drop) * (1 - smooth(0.7, 0.95, r))
    # main cracks
    pen = Pen(S, S)
    def crack(p, ang, length, width, depth):
        steps = 16
        for k in range(steps):
            a2 = ang + rng.uniform(-0.28, 0.28)
            seg = length / steps
            q = (p[0] + math.cos(a2) * seg, p[1] + math.sin(a2) * seg)
            wd = width * (1 - k / steps) + 0.8
            pen.taper(p, q, wd, max(0.8, wd * 0.86))
            if depth > 0 and k > 1 and rng.random() < 0.2:
                crack(q, a2 + rng.choice([-1, 1]) * rng.uniform(0.4, 0.9), length * rng.uniform(0.25, 0.4), width * 0.5, depth - 1)
            p, ang = q, a2
    for i in range(9):
        crack((cx, cy), 2 * math.pi * (i + rng.uniform(-0.2, 0.2)) / 9, cx * rng.uniform(0.62, 0.95), 5.5, 2)
    main = pen.array()
    # the sheet: jagged frost boundary with ferny fringe
    nb = fbm(S, S, 28, 1804, 5, tile=False)
    ang_noise = 0.5 + 0.5 * np.sin(th * 11 + nb * 5) * np.sin(th * 5 + 2)
    rb = 0.80 + 0.12 * nb + 0.05 * ang_noise
    sheet = smooth(rb + 0.012, rb - 0.03, r)
    frost = np.clip((fbm(S, S, 6, 1805, 3, tile=False) - 0.42) * 3, 0, 1) * smooth(rb + 0.07, rb - 0.01, r) * (1 - sheet)   # frost needles beyond the edge
    lines = np.clip(mesh * 0.85 + main, 0, 1)
    A = np.clip(sheet * 0.62 + frost * 0.35 + lines * sheet * 0.5, 0, 1)
    L = 0.14 + 0.1 * fbm(S, S, 18, 1806, 3, tile=False) + 0.9 * lines * sheet + 0.45 * frost
    A = np.maximum(A, lines * sheet)
    finish(L, A, "demon_ice_cracks", bleed=False)


def mist(size=256):
    """Cold fog puff: domain-warped smoke with soft radial falloff. Draw dark (black frost smoke) and again additive blue (underlight)."""
    S = size
    xx, yy = grid(S, S)
    c = (S - 1) / 2
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    n = warped_fbm(S, S, 56, 1901, 38, 5)
    n2 = warped_fbm(S, S, 28, 1911, 14, 4)
    body = np.clip((n - 0.30) * 2.4, 0, 1)
    wisp = np.clip(1 - np.abs(n2 - 0.5) * 5, 0, 1) ** 2
    shape = smooth(1.0, 0.18, r + (n - 0.5) * 0.45)
    A = np.clip(shape * (0.28 + 0.9 * body) + wisp * shape * 0.25, 0, 1) ** 1.1
    A *= smooth(1.0, 0.8, r)
    L = np.clip(0.42 + 0.7 * body * shape + 0.35 * wisp, 0, 1)
    finish(L, A, "demon_ice_mist", bleed=False)


def burst(size=256):
    """Shatter star: tapering shard-rays of alternating length, hairline cracks, a bright core and a thin ring. White."""
    S = size
    c = (S / 2, S / 2)
    R = S / 2
    pen = Pen(S, S)
    rng = np.random.default_rng(2001)
    N = 22
    for i in range(N):
        a = 2 * math.pi * i / N + rng.uniform(-0.1, 0.1)
        length = R * (0.97 if i % 3 == 0 else (0.62 if i % 3 == 1 else 0.78)) * rng.uniform(0.85, 1.0)
        wb = R * (0.065 if i % 3 == 0 else 0.045)
        p0 = (c[0] + math.cos(a) * R * 0.06, c[1] + math.sin(a) * R * 0.06)
        tip = (c[0] + math.cos(a) * length, c[1] + math.sin(a) * length)
        nx, ny = -math.sin(a), math.cos(a)
        mid = (c[0] + math.cos(a) * length * 0.22, c[1] + math.sin(a) * length * 0.22)
        pen.poly([(mid[0] + nx * wb, mid[1] + ny * wb), tip, (mid[0] - nx * wb, mid[1] - ny * wb), p0], 255 if i % 3 == 0 else 215)
        pen.line(p0, tip, 1.2, 255)
    for i in range(46):
        a = rng.uniform(0, 2 * math.pi)
        r0, r1 = R * rng.uniform(0.1, 0.35), R * rng.uniform(0.4, 0.9)
        pen.line((c[0] + math.cos(a) * r0, c[1] + math.sin(a) * r0), (c[0] + math.cos(a + rng.uniform(-0.08, 0.08)) * r1, c[1] + math.sin(a) * r1), 0.8, 140)
    pen.circle(c, R * 0.46, 1.1, 170)
    a = pen.array()
    xx, yy = grid(S, S)
    r = np.sqrt(((xx - S / 2) / R) ** 2 + ((yy - S / 2) / R) ** 2)
    core = np.exp(-(r / 0.16) ** 2)
    out = np.clip(glowed(a, 2.4, 0.7) + core * 1.1, 0, 1)
    finish(0.6 + 0.4 * out, out, "demon_ice_burst", bleed=False)


def ring(size=256):
    """Shock ring: a bright band with a feathered inner trail and a fringe of sharp frost teeth along the outer edge. White."""
    S = size
    xx, yy = grid(S, S)
    c = (S - 1) / 2
    dx, dy = (xx - c) / c, (yy - c) / c
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    rng = np.random.default_rng(2101)
    n = value_noise(S, S, 16, 2102, False)
    band = np.exp(-((r - 0.885) / 0.028) ** 2) + 0.5 * np.exp(-((r - 0.86) / 0.09) ** 2)
    trail = smooth(0.58, 0.88, r) * 0.45 * (0.6 + 0.8 * n) * (r < 0.9)
    teeth_n = 44
    ph = (th / (2 * math.pi) * teeth_n) % 1.0
    tooth = 1 - np.abs(ph - 0.5) * 2
    heights = 0.045 + 0.05 * (0.5 + 0.5 * np.sin(np.floor(th / (2 * math.pi) * teeth_n) * 2.1 + 1.3))
    teeth = np.clip((tooth * heights + 0.915 - r) / 0.012, 0, 1) * (r > 0.9)
    A = np.clip(band + trail + teeth, 0, 1) * (r < 1.0)
    A = A * (0.8 + 0.4 * n)
    finish(0.7 + 0.3 * np.clip(band, 0, 1), np.clip(A, 0, 1), "demon_ice_ring", bleed=False)


def glint(size=64):
    """Diamond glint: long thin four-point star with two short diagonal rays and a hot core. White."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = np.abs(xx - c) / c, np.abs(yy - c) / c
    star = np.clip(1 - (np.sqrt(x) + np.sqrt(y)), 0, 1) ** 1.6
    d1 = np.exp(-(((x - y) / 0.05) ** 2)) * np.clip(1 - (x + y) / 0.85, 0, 1) * 0.55
    core = np.exp(-((x * x + y * y) / 0.012))
    A = np.clip(star * 1.4 + d1 + core, 0, 1)
    finish(np.ones_like(A), A, "demon_ice_glint", bleed=False)


def invert(size=128):
    """NEGATIVE-blend mask: the RGB (not the alpha) says how much of the world behind it is inverted. A soft disc with a feathery,
    crystalline edge and a faint dark hub (so the very centre blazes white-hot through ADD on top)."""
    S = size
    xx, yy = grid(S, S)
    c = (S - 1) / 2
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    th = np.arctan2((yy - c), (xx - c))
    n = value_noise(S, S, 12, 2201, False)
    spikes = 0.5 + 0.5 * np.cos(th * 14 + n * 4)
    m = smooth(1.0, 0.25, r + (n - 0.5) * 0.28 + spikes * 0.06)
    m = np.clip(m ** 1.3 * 1.15, 0, 1)
    g = (m * 255 + 0.5).astype(np.uint8)
    save(Image.fromarray(np.dstack([g, g, g, np.full_like(g, 255)]), "RGBA"), "demon_ice_invert")


def spray(w=64, h=256):
    """Frost trail, head up: a narrow hot head widening into a granular spray of ice dust that streaks and fades toward the tail."""
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / (h - 1)
    width = 0.10 + 0.82 * v ** 0.8
    grain = aniso_noise(w, h, 3, 28, 2301, False) * 0.55 + aniso_noise(w, h, 2, 9, 2302, False) * 0.45
    across = np.clip(1 - (np.abs(u) / width) ** 2, 0, 1)
    env = across ** 0.8 * (1 - v) ** 0.9 * smooth(0.0, 0.02, v)
    A = np.clip(env * (0.25 + 1.2 * grain), 0, 1)
    rng = np.random.default_rng(2303)
    for _ in range(7):                                                                # streak lines
        cxl = rng.uniform(-0.7, 0.7) * width
        A = np.maximum(A, np.exp(-((u - cxl) / 0.045) ** 2) * (1 - v) ** 1.4 * rng.uniform(0.3, 0.7) * (across > 0.05))
    core = np.exp(-(u / 0.1) ** 2) * (1 - smooth(0.0, 0.5, v))
    A = np.clip(A + core * 0.8, 0, 1)
    finish(0.55 + 0.45 * np.clip(core + grain * 0.6, 0, 1), A, "demon_ice_spray", bleed=False)


# ================================================================================================ main
ALL = {"lance": lance, "shards": shards, "crystal": crystal_cluster, "wall": wall, "runes": runes, "sigil": sigil, "flake": flake, "cracks": cracks,
       "mist": mist, "burst": burst, "ring": ring, "glint": glint, "invert": invert, "spray": spray}

if __name__ == "__main__":
    want = sys.argv[1:] or list(ALL)
    for n in want:
        ALL[n]()

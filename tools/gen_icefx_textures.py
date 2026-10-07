"""Generates the Ice Magic VFX textures (icefx_*). Deterministic (fixed seeds), no fonts, no external images.

    python3 -B tools/gen_icefx_textures.py            # all textures
    python3 -B tools/gen_icefx_textures.py fang wall  # only the named ones

Output: src/main/resources/assets/nusmp/textures/particle/icefx_*.png

Look: the Heavenly Ice Fang of the art pack. Clear pale ice (NOT the black glass of Demon Ice): long faceted spikes whose facets run
from deep blue shade to white, hard white edges that glow, frost ferns and six-fold snowflakes, breath mist and fine crack lines.
Every sprite is grey-scale + alpha (the vertex colour tints it). In the crystal sprites the grey is the FACET SHADE and the alpha is
lower inside the ice than on its edges, so drawn with ALPHA in a blue tint it is glass with a bright rim, and drawn again with ADD in
pale cyan / white only the lit facets and the edges flare.

Textures (size): fang 64x256 (projectile, head up) | spike 48x256 (one long blade, tip up) | crystal 128x256 (cluster, base down) | shards 256x64 (atlas of four 64x64
fragments) | burst 256 (shatter star, long spikes) | fern 512 (frost-fern star) | flake 256 (six-fold snowflake) | mist 256 (breath
mist puff) | rime 512 (ring with frost teeth) | glint 64 (diamond glint) | crack 512 (cracked ice sheet) | trail 64x256 (frost
streak, head up) | wall 512x128 (tileable row of ice spikes).
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


def fbm(w, h, cell, seed, octaves=5, tile=True):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + value_noise(w, h, max(2, cell >> o), seed + 17 * o, tile) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def finish(lum, alpha, name, bleed=True):
    """Writes a grey-scale + alpha sprite. Transparent texels take the colour of their nearest opaque neighbours (no dark fringes)."""
    lum, alpha = np.clip(lum, 0, 1), np.clip(alpha, 0, 1)
    if bleed:
        wa = blur(lum * alpha, 2.0) / np.maximum(blur(alpha, 2.0), 1e-3)
        lum = lum * alpha + np.clip(wa, 0, 1) * (1 - alpha)
    g = (np.clip(lum, 0, 1) * 255 + 0.5).astype(np.uint8)
    img = np.dstack([g, g, g, (alpha * 255 + 0.5).astype(np.uint8)])
    save(Image.fromarray(img, "RGBA"), name)


# ------------------------------------------------------------------------------------------------ the ice renderer
def render_ice(w, h, facets, seed, inner=0.5, edge=0.9, rim=0.8, grad=0.3, veins=14, vbox=None, wrap_x=False):
    """Clear faceted ice. facets = [(polygon in output pixels, shade 0..1), ...] painted in order (later on top).
    Facets get a gradient (lighter toward the tip / top), fine inner veins, hard bright seams where two facets of different shade meet and
    a bright outline. Alpha is 'inner' inside the ice and 1 on the seams and outline, so the body is see-through glass with a bright
    rim. Returns (lum, alpha) at the output size."""
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
    e_int = np.zeros_like(cov)
    e_rim = np.zeros_like(cov)
    for dy, dx in ((0, 1), (1, 0), (0, -1), (-1, 0)):
        sh = np.roll(ids, (dy, dx), (0, 1))
        shl = np.roll(lum, (dy, dx), (0, 1))
        diff = ids != sh
        both = diff & (ids > 0) & (sh > 0)
        e_int = np.maximum(e_int, both * np.clip(np.abs(lum - shl) * 3.0, 0.25, 1.0))
        e_rim = np.maximum(e_rim, (diff & (ids > 0) & (sh == 0)) * (0.35 + 0.65 * lum))
    e_int = np.clip(blur(e_int.astype(np.float32), 1.0) * 3.0, 0, 1) * cov
    e_rim = np.clip(blur(e_rim.astype(np.float32), 1.0) * 3.0, 0, 1) * cov
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
    n = fbm(w * S, h * S, max(8, 30 * S), seed, 4, tile=wrap_x)
    g = fbm(w * S, h * S, max(4, 3 * S), seed + 7, 3, tile=wrap_x)
    lum = lum * (0.9 + 0.2 * n) + 0.06 * (g - 0.5)
    vein = np.zeros_like(cov)
    if veins:
        fr = Image.new("L", (w * S, h * S), 0)
        df = ImageDraw.Draw(fr)
        bx0, by0, bx1, by1 = vbox or (0, 0, w, h)
        for _ in range(veins):
            x, y = rng.uniform(bx0, bx1), rng.uniform(by0, by1)
            ang = math.radians(rng.uniform(55, 125) * rng.choice([-1, 1]) + 90)
            ln = rng.uniform(5, 16)
            pts = [(x, y)]
            for _s in range(3):
                ang += rng.uniform(-0.45, 0.45)
                x, y = x + math.cos(ang) * ln / 3, y + math.sin(ang) * ln / 3
                pts.append((x, y))
            df.line([(px * S, py * S) for px, py in pts], fill=int(rng.uniform(110, 230)), width=max(1, int(0.7 * S)))
        vein = blur(np.asarray(fr).astype(np.float32) / 255, 0.6) * cov
    lum = np.clip(np.clip(lum, 0, 1) ** 1.15 + edge * e_int + rim * e_rim + 0.5 * vein, 0, 1)
    al = cov * np.clip(inner + 0.25 * lum + 0.7 * e_int + e_rim + 0.5 * vein, 0, 1)
    a = box_down(al, w, h)
    cv = box_down(cov, w, h)
    l = box_down(lum * cov, w, h) / np.maximum(cv, 1e-4)
    return l, a


def spike(bx, by, tx, ty, width, shade_l=0.8, shade_r=0.3, ridge=0.18, tipfrac=0.0):
    """One pointed ice blade from base centre (bx, by) to the tip (tx, ty): two facets split by an off-centre ridge (light left,
    shadow right), and with tipfrac > 0 a third, chiselled tip facet. Returns [(polygon, shade)]."""
    dx, dy = tx - bx, ty - by
    ln = math.hypot(dx, dy)
    nx, ny = -dy / ln, dx / ln
    hw = width / 2
    bl, br = (bx + nx * hw, by + ny * hw), (bx - nx * hw, by - ny * hw)
    rd = ridge * hw
    rb = (bx + nx * rd, by + ny * rd)
    out = []
    if tipfrac > 0:
        mx, my = bx + dx * (1 - tipfrac), by + dy * (1 - tipfrac)
        k = 1 - tipfrac
        ml, mr, mm = (mx + nx * hw * tipfrac, my + ny * hw * tipfrac), (mx - nx * hw * tipfrac, my - ny * hw * tipfrac), (mx + nx * rd * tipfrac, my + ny * rd * tipfrac)
        # the shaft up to the chisel, then the tip
        sl = (bx + nx * hw * (1 - k * 0), by + ny * hw * (1 - k * 0))
        out.append(([bl, rb, mm, ml], shade_l))
        out.append(([rb, br, mr, mm], shade_r))
        out.append(([ml, mm, (tx, ty)], min(1.0, shade_l + 0.15)))
        out.append(([mm, mr, (tx, ty)], shade_r + 0.1))
    else:
        out.append(([bl, (tx, ty), rb], shade_l))
        out.append(([rb, (tx, ty), br], shade_r))
    return out


def facets_of(*groups):
    out = []
    for g in groups:
        out.extend(g)
    return out


# ================================================================================================ the projectile
def fang(w=64, h=256):
    """Heavenly Ice Fang: a long clear spear of ice, head up. A double bevelled blade with two pairs of barbs, a faceted shaft that
    thins into splinters at the tail."""
    cx = w / 2
    f = []
    # blade: left lit, right shade, with a bevel step half-way
    f.append(([(cx, 2), (cx - 6, 40), (cx + 0.6, 40)], 0.95))
    f.append(([(cx, 2), (cx + 0.6, 40), (cx + 6.5, 40)], 0.55))
    f.append(([(cx + 0.6, 40), (cx - 6, 40), (cx - 12.5, 78), (cx + 0.9, 78)], 0.78))
    f.append(([(cx + 0.6, 40), (cx + 0.9, 78), (cx + 13, 78), (cx + 6.5, 40)], 0.38))
    # barbs
    f.append(([(cx - 12.5, 78), (cx - 25, 92), (cx - 15, 100), (cx - 8, 96)], 0.88))
    f.append(([(cx + 13, 78), (cx + 25, 92), (cx + 16, 100), (cx + 8, 96)], 0.34))
    f.append(([(cx + 0.9, 78), (cx - 12.5, 78), (cx - 10, 118), (cx + 1, 130)], 0.66))
    f.append(([(cx + 0.9, 78), (cx + 1, 130), (cx + 10.5, 118), (cx + 13, 78)], 0.30))
    # shaft collar and second barb pair
    f.append(([(cx - 10, 118), (cx - 22, 134), (cx - 14, 146), (cx - 6, 140)], 0.8))
    f.append(([(cx + 10.5, 118), (cx + 22, 134), (cx + 15, 146), (cx + 6, 140)], 0.32))
    f.append(([(cx + 1, 130), (cx - 10, 118), (cx - 8, 160), (cx + 0.5, 172)], 0.6))
    f.append(([(cx + 1, 130), (cx + 0.5, 172), (cx + 8.5, 160), (cx + 10.5, 118)], 0.28))
    # tail splinters
    f.extend(spike(cx - 4, 160, cx - 14, 226, 9, 0.7, 0.3))
    f.extend(spike(cx + 3, 165, cx + 12, 238, 8, 0.6, 0.26))
    f.extend(spike(cx, 168, cx - 1, 253, 10, 0.64, 0.28))
    lum, a = render_ice(w, h, f, 31, inner=0.55, veins=10, vbox=(cx - 8, 20, cx + 8, 150))
    yy = np.arange(h, dtype=np.float32)[:, None]
    a = a * (1 - 0.65 * smooth(170, 255, yy))
    finish(lum, a, "icefx_fang")


def spike_tex(w=48, h=256):
    """One long clean ice blade, tip up: a light and a shadow facet split by an off-centre ridge, a chisel step near the tip."""
    f = []
    f.extend(spike(w / 2, h - 2, w / 2 + 1.5, 2, w - 6, 0.95, 0.34, 0.16, 0.22))
    lum, a = render_ice(w, h, f, 91, inner=0.5, veins=8, vbox=(w / 2 - 6, 40, w / 2 + 6, h - 20))
    yy = np.arange(h, dtype=np.float32)[:, None]
    a = a * (0.35 + 0.65 * (1 - smooth(h * 0.78, h - 1, yy)) + 0.0)
    finish(lum, a, "icefx_spike")


def crystal(w=128, h=256):
    """A cluster of ice prisms fused at the base (base down): one tall chisel-tipped prism with leaning ones either side."""
    f = []
    f.extend(spike(26, 252, 8, 150, 34, 0.7, 0.28, 0.2))
    f.extend(spike(100, 252, 122, 120, 36, 0.66, 0.26, 0.1))
    f.extend(spike(78, 252, 90, 70, 30, 0.74, 0.3, 0.15))
    f.extend(spike(54, 254, 60, 6, 46, 0.92, 0.36, 0.16))
    f.extend(spike(40, 252, 24, 112, 22, 0.6, 0.24, 0.1))
    f.extend(spike(66, 252, 72, 196, 18, 0.55, 0.24))
    # tall prism: a mid seam to read as a hexagonal prism
    f.append(([(54, 254), (54, 104), (62, 90), (62, 254)], 0.5))
    lum, a = render_ice(w, h, f, 47, inner=0.5, veins=16, vbox=(30, 60, 100, 230))
    yy = np.arange(h, dtype=np.float32)[:, None]
    a = a * (0.55 + 0.45 * smooth(256, 235, yy) * 0 + 0.45 * (1 - smooth(236, 256, yy)))
    finish(lum, a, "icefx_crystal")


def shards(w=256, h=64):
    """Atlas of four 64x64 fragments: a thin sliver, a broken triangle, a kite, a chisel chip."""
    f = []
    f.extend(spike(26, 60, 40, 3, 11, 0.9, 0.35))
    f.append(([(98, 8), (126, 54), (72, 52)], 0.6))
    f.append(([(98, 8), (126, 54), (104, 36)], 0.95))
    f.append(([(98, 8), (72, 52), (92, 34)], 0.36))
    f.append(([(160, 4), (178, 30), (160, 60), (146, 28)], 0.7))
    f.append(([(160, 4), (178, 30), (160, 28)], 0.98))
    f.append(([(160, 28), (178, 30), (160, 60)], 0.32))
    f.append(([(160, 4), (160, 28), (146, 28)], 0.5))
    f.append(([(212, 14), (244, 20), (250, 50), (218, 58), (206, 40)], 0.58))
    f.append(([(212, 14), (244, 20), (230, 36), (206, 40)], 0.96))
    f.append(([(230, 36), (244, 20), (250, 50)], 0.3))
    lum, a = render_ice(w, h, f, 59, inner=0.6, veins=6)
    finish(lum, a, "icefx_shards")


def burst(size=256):
    """Shatter star: long thin ice shards radiating from a white core, a ring of finer ones, a soft core glow."""
    c = size / 2
    rng = np.random.default_rng(71)
    f = []
    for i in range(16):
        ang = math.tau * i / 16 + rng.uniform(-0.1, 0.1)
        ln = rng.uniform(0.62, 0.98) * c if i % 2 == 0 else rng.uniform(0.36, 0.62) * c
        wd = rng.uniform(5, 9) if i % 2 == 0 else rng.uniform(4, 6)
        b0 = (c + math.cos(ang) * 7, c + math.sin(ang) * 7)
        f.extend(spike(b0[0], b0[1], c + math.cos(ang) * ln, c + math.sin(ang) * ln, wd, 0.98, 0.4, 0.2))
    for i in range(12):
        ang = math.tau * (i + 0.5) / 12 + rng.uniform(-0.15, 0.15)
        r0 = c * rng.uniform(0.62, 0.8)
        f.extend(spike(c + math.cos(ang) * r0, c + math.sin(ang) * r0, c + math.cos(ang) * (r0 + c * 0.2), c + math.sin(ang) * (r0 + c * 0.2), 4, 0.9, 0.4))
    lum, a = render_ice(size, size, f, 83, inner=0.75, veins=0, edge=0.5, rim=0.6)
    xx, yy = grid(size, size)
    r = np.hypot(xx - c, yy - c) / c
    core = np.exp(-(r / 0.16) ** 2) + 0.35 * np.exp(-(r / 0.4) ** 2)
    lum = np.clip(lum + core, 0, 1)
    a = np.clip(a + core, 0, 1)
    finish(lum, a, "icefx_burst")


# ================================================================================================ frost patterns
def dendrite(pen, p0, ang, length, width, depth, rng, sym=False, spread=0.95):
    """Frost fern branch: a stem with leaflets that fork again (the fern look)."""
    x, y = p0
    x1, y1 = x + math.cos(ang) * length, y + math.sin(ang) * length
    pen.line([(x, y), (x1, y1)], fill=255, width=max(1, int(width)))
    if depth <= 0 or length < 4:
        return
    n = max(2, int(length / 30))
    for i in range(1, n + 1):
        t = i / (n + 1)
        px, py = x + (x1 - x) * t, y + (y1 - y) * t
        sub = length * (0.36 - 0.26 * t) * rng.uniform(0.85, 1.1)
        for sgn in (-1, 1):
            dendrite(pen, (px, py), ang + sgn * spread * rng.uniform(0.85, 1.1), sub, max(1, width * 0.7), depth - 1, rng)
    dendrite(pen, (x1, y1), ang, length * 0.0, 1, 0, rng)


def fern(size=512):
    """Frost-fern star lying on the ground: six main ferns with side leaflets and a bright centre, soft towards the rim."""
    S = 2
    n = size * S
    c = n / 2
    rng = np.random.default_rng(97)
    im = Image.new("L", (n, n), 0)
    pen = ImageDraw.Draw(im)
    for k in range(6):
        ang = math.tau * k / 6 + 0.1
        dendrite(pen, (c, c), ang, n * 0.43, 4 * S, 2, rng, spread=1.0)
    # a hexagon of crystals round the centre
    hexp = [(c + math.cos(math.tau * k / 6) * n * 0.055, c + math.sin(math.tau * k / 6) * n * 0.055) for k in range(6)]
    pen.polygon(hexp, fill=255)
    a = np.asarray(im).astype(np.float32) / 255
    a = box_down(a, size, size)
    xx, yy = grid(size, size)
    r = np.hypot(xx - size / 2, yy - size / 2) / (size / 2)
    a = np.clip(blur(a, 0.6) * 1.4, 0, 1) * (1 - smooth(0.86, 1.0, r))
    soft = blur(a, 5)
    lum = np.clip(0.55 + 0.7 * a + 0.35 * soft + 0.5 * np.exp(-(r / 0.1) ** 2), 0, 1)
    al = np.clip(a + 0.45 * soft + 0.25 * np.exp(-(r / 0.3) ** 2) * (1 - smooth(0.2, 0.7, r)), 0, 1)
    finish(lum, al, "icefx_fern")


def flake(size=256):
    """A six-fold snowflake: hexagonal arms with paired side branches, a small hexagon at the heart."""
    S = 4
    n = size * S
    c = n / 2
    im = Image.new("L", (n, n), 0)
    pen = ImageDraw.Draw(im)
    for k in range(6):
        ang = math.tau * k / 6 + math.pi / 6
        ux, uy = math.cos(ang), math.sin(ang)
        L = n * 0.44
        pen.line([(c, c), (c + ux * L, c + uy * L)], fill=255, width=int(n * 0.016))
        for t, bl in ((0.3, 0.26), (0.5, 0.22), (0.7, 0.16)):
            px, py = c + ux * L * t, c + uy * L * t
            for sgn in (-1, 1):
                a2 = ang + sgn * math.radians(58)
                pen.line([(px, py), (px + math.cos(a2) * L * bl, py + math.sin(a2) * L * bl)], fill=255, width=int(n * 0.011))
        tip = [(c + ux * L * 0.98, c + uy * L * 0.98), (c + ux * L * 0.9 - uy * n * 0.014, c + uy * L * 0.9 + ux * n * 0.014), (c + ux * L * 0.9 + uy * n * 0.014, c + uy * L * 0.9 - ux * n * 0.014)]
        pen.polygon(tip, fill=255)
    pen.polygon([(c + math.cos(math.tau * k / 6) * n * 0.045, c + math.sin(math.tau * k / 6) * n * 0.045) for k in range(6)], fill=255)
    a = box_down(np.asarray(im).astype(np.float32) / 255, size, size)
    a = np.clip(blur(a, 0.5) * 1.5, 0, 1)
    glow = blur(a, 4)
    lum = np.clip(0.8 + 0.4 * a, 0, 1)
    finish(lum, np.clip(a + 0.35 * glow, 0, 1), "icefx_flake")


def mist(size=256):
    """Breath mist: a soft, streaky puff."""
    xx, yy = grid(size, size)
    r = np.hypot(xx - size / 2, yy - size / 2) / (size / 2)
    n = fbm(size, size, 48, 5, 5)
    n2 = fbm(size, size, 24, 9, 4)
    body = np.clip((1 - smooth(0.12, 1.0, r + 0.35 * (n - 0.5))) * (0.6 + 0.8 * n2), 0, 1)
    lum = np.clip(0.55 + 0.45 * n2 + 0.2 * body, 0, 1)
    finish(lum, np.clip(body * 0.9, 0, 1), "icefx_mist", bleed=False)


def rime(size=512):
    """Ring of rime: a bright thin circle with frost teeth pointing inward and out, a soft frosted band, fine ticks."""
    S = 2
    n = size * S
    c = n / 2
    rng = np.random.default_rng(113)
    im = Image.new("L", (n, n), 0)
    pen = ImageDraw.Draw(im)
    R = n * 0.43
    pen.ellipse([c - R, c - R, c + R, c + R], outline=255, width=int(n * 0.008))
    R2 = n * 0.40
    pen.ellipse([c - R2, c - R2, c + R2, c + R2], outline=170, width=int(n * 0.004))
    for k in range(48):
        ang = math.tau * k / 48 + rng.uniform(-0.02, 0.02)
        ln = n * rng.uniform(0.03, 0.075) * (1.6 if k % 4 == 0 else 1.0)
        for sgn, rr in ((-1, R), (1, R)):
            x0, y0 = c + math.cos(ang) * rr, c + math.sin(ang) * rr
            x1, y1 = c + math.cos(ang) * (rr + sgn * ln), c + math.sin(ang) * (rr + sgn * ln)
            pen.line([(x0, y0), (x1, y1)], fill=255 if k % 4 == 0 else 190, width=int(n * 0.005))
            if k % 4 == 0 and sgn == -1:
                for sd in (-1, 1):
                    a2 = ang + sd * 0.07
                    mx, my = c + math.cos(ang) * (rr - ln * 0.55), c + math.sin(ang) * (rr - ln * 0.55)
                    pen.line([(mx, my), (c + math.cos(a2) * (rr - ln * 0.95), c + math.sin(a2) * (rr - ln * 0.95))], fill=200, width=int(n * 0.003))
    a = box_down(np.asarray(im).astype(np.float32) / 255, size, size)
    xx, yy = grid(size, size)
    r = np.hypot(xx - size / 2, yy - size / 2) / (size / 2)
    band = np.exp(-((r - 0.84) / 0.07) ** 2) * (0.35 + 0.65 * fbm(size, size, 40, 3, 4))
    line = np.clip(blur(a, 0.6) * 1.5, 0, 1)
    al = np.clip(line + 0.5 * band, 0, 1)
    lum = np.clip(0.55 + 0.6 * line + 0.2 * band, 0, 1)
    finish(lum, al, "icefx_rime")


def glint(size=64):
    """Diamond glint: a thin four-pointed star with a hot centre."""
    xx, yy = grid(size, size)
    x, y = (xx - size / 2 + 0.5) / (size / 2), (yy - size / 2 + 0.5) / (size / 2)
    ax, ay = np.abs(x), np.abs(y)
    v = np.exp(-(ax / 0.045) ** 1.2) * np.exp(-(ay / 0.95) ** 1.6) + np.exp(-(ay / 0.045) ** 1.2) * np.exp(-(ax / 0.95) ** 1.6)
    d = np.exp(-(ax + ay) / 0.16) * 0.6
    core = np.exp(-(np.hypot(x, y) / 0.12) ** 2)
    a = np.clip(v + d + core, 0, 1)
    finish(np.clip(0.7 + 0.5 * a, 0, 1), a, "icefx_glint", bleed=False)


def crack(size=512):
    """Cracked ice sheet seen from above: pale translucent plates separated by bright crack lines, a few star cracks, soft rim."""
    rng = np.random.default_rng(131)
    xx, yy = grid(size, size)
    cx = size / 2
    r = np.hypot(xx - cx, yy - cx) / cx
    pts = np.vstack([rng.uniform(0, size, (70, 2)), [[cx, cx]]])
    # distance to nearest and second nearest site: the difference is small along cell borders
    d1 = np.full((size, size), 1e9, np.float32)
    d2 = np.full((size, size), 1e9, np.float32)
    cell = np.zeros((size, size), np.int32)
    for i, (px, py) in enumerate(pts):
        d = np.hypot(xx - px, yy - py)
        m1 = d < d1
        d2 = np.where(m1, d1, np.minimum(d2, d))
        cell = np.where(m1, i, cell)
        d1 = np.where(m1, d, d1)
    edge = 1 - smooth(0.0, 3.2, d2 - d1)
    keep = np.array([rng.random() < 0.85 for _ in range(len(pts))])
    edge = edge * np.where(keep[cell], 1.0, 0.0) * (1 - smooth(0.7, 1.0, r))
    shade = np.array([rng.uniform(0.35, 0.8) for _ in range(len(pts))], np.float32)[cell]
    disc = 1 - smooth(0.86, 1.0, r + 0.06 * (fbm(size, size, 40, 17, 3) - 0.5))
    frost = fbm(size, size, 64, 21, 5)
    lum = np.clip(shade * 0.8 + 0.25 * frost + 0.9 * edge, 0, 1)
    al = np.clip(disc * (0.38 + 0.25 * frost) + edge * disc * 0.95, 0, 1)
    finish(lum, al, "icefx_crack")


def trail(w=64, h=256):
    """Frost streak, head up: a bright narrow head widening into spreading ice dust and tiny glints."""
    xx, yy = grid(w, h)
    x = (xx - w / 2 + 0.5) / (w / 2)
    t = yy / h  # 0 head, 1 tail
    wid = 0.16 + 0.8 * t ** 0.8
    n = fbm(w, h, 12, 61, 4, tile=False)
    body = np.exp(-(x / wid) ** 2) * (1 - smooth(0.0, 0.08, t) * 0 ) * (1 - t) ** 0.9 * (0.55 + 0.9 * n)
    streak = np.exp(-(x / 0.07) ** 2) * (1 - t) ** 1.4
    rng = np.random.default_rng(67)
    sp = np.zeros((h, w), np.float32)
    for _ in range(30):
        sx, sy = rng.uniform(4, w - 4), rng.uniform(6, h * 0.9)
        sp += np.exp(-(((xx - sx) ** 2 + (yy - sy) ** 2) / 2.2)) * rng.uniform(0.5, 1.0) * (1 - sy / h)
    a = np.clip(body * 0.9 + streak + sp, 0, 1)
    finish(np.clip(0.5 + 0.7 * a, 0, 1), a, "icefx_trail", bleed=False)


def wall(w=512, h=128):
    """Tileable row of ice spikes standing on a frosted base (rim of a zone): clear prisms of different heights and a bright bottom band."""
    rng = np.random.default_rng(149)
    f = []
    x = 0.0
    while x < w:
        wd = rng.uniform(26, 52)
        ht = rng.uniform(0.55, 1.0) * h
        lean = rng.uniform(-8, 8)
        bx = x + wd / 2
        for off in (0, -w, w):
            f.extend(spike(bx + off, h - 2, bx + off + lean, h - ht, wd, rng.uniform(0.62, 0.95), rng.uniform(0.22, 0.4), 0.18))
        x += wd * rng.uniform(0.55, 0.85)
    lum, a = render_ice(w, h, f, 151, inner=0.5, veins=20, vbox=(0, 20, w, h - 6), wrap_x=True)
    yy = np.arange(h, dtype=np.float32)[:, None]
    base = (1 - smooth(h - 14, h, yy)) * 0 + smooth(h - 16, h - 2, yy)
    lum = np.clip(lum + 0.2 * base, 0, 1)
    a = np.clip(a + 0.45 * base * (blur(a, 6) > 0.05), 0, 1)
    finish(lum, a, "icefx_wall")


ALL = {"fang": fang, "spike": spike_tex, "crystal": crystal, "shards": shards, "burst": burst, "fern": fern, "flake": flake, "mist": mist, "rime": rime,
       "glint": glint, "crack": crack, "trail": trail, "wall": wall}

if __name__ == "__main__":
    want = sys.argv[1:] or list(ALL)
    for n in want:
        ALL[n]()

"""Generates the Bronze Magic VFX textures (deterministic, no fonts, no external images).

    python3 -B tools/gen_bronze_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/bronze_*.png

Look reference (Black Clover, Sekke family of bronze spells + the owner spec "a tarnished green-and-brown metal look; heavy physical
weaponry"):
  Sekke Magnum Cannonball = a transparent sphere with spikes at random places and an intricate engraved pattern over its surface.
  Sekke Shooting Star     = a winged bronze vessel; large feathered wings, engraved filigree, sturdy enough to ram.
  Sekke Poison Lizard     = a dark bronze lizard that crawls like a living thing.
Texture convention (same as the rest of textures/particle): white / greyscale with alpha so the vertex colour tints them (bronze, hot
orange, verdigris green); bronze_patina is the exception, its colour (verdigris + brown) is the point.

Sprites: sphere (spiked cannonball), streak (cannonball trail), ring (molten hammered ring with rivets), band (riveted plate ribbon),
sigil (filigree ground circle), chip (hammered metal shard), flecks (glint cloud), wing (feathered metal wings), spike (rising
bronze spike), lizard (poison lizard silhouette), patina (verdigris crust), burst (starburst of cannonball peaks).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


# ------------------------------------------------------------------------------------------------ helpers
def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def grey(lum, alpha):
    """Greyscale RGBA from a luminance (0..255) and an alpha (0..1) array."""
    l = np.clip(lum, 0, 255)
    a = np.clip(alpha, 0, 1) * 255
    return Image.fromarray(np.dstack([l, l, l, a]).astype(np.uint8), "RGBA")


def value_noise(w, h, cell_x, cell_y, seed, tile=True):
    rng = np.random.default_rng(seed)
    gw, gh = max(1, int(w // cell_x)), max(1, int(h // cell_y))
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


def fbm(w, h, cell, seed, octaves=4, aniso=1.0):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        c = max(2, cell >> o)
        n = n + value_noise(w, h, c * aniso, c, seed + o * 7) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def cells(w, h, n, seed):
    """Voronoi-ish edge map (0 inside a cell, 1 on the borders): hammered dents."""
    rng = np.random.default_rng(seed)
    pts = rng.random((n * n, 2)) * [w, h]
    xx, yy = grid(w, h)
    d1 = np.full((h, w), 1e9, np.float32)
    d2 = np.full((h, w), 1e9, np.float32)
    for px, py in pts:
        dx = np.minimum(np.abs(xx - px), w - np.abs(xx - px))
        dy = np.minimum(np.abs(yy - py), h - np.abs(yy - py))
        d = np.sqrt(dx * dx + dy * dy)
        d2 = np.minimum(d2, np.maximum(d, d1))
        d1 = np.minimum(d1, d)
    edge = np.clip(1 - (d2 - d1) / (w / n * 0.35), 0, 1)
    return edge, d1 / (w / n)


def downsample(im):
    return im.resize((im.width // SS, im.height // SS), Image.LANCZOS)


def mask_of(draw_fn, size):
    """Draws into a supersampled 'L' canvas via draw_fn(draw, scale) and returns a float 0..1 array at the target size."""
    big = Image.new("L", (size[0] * SS, size[1] * SS), 0)
    draw_fn(ImageDraw.Draw(big), SS)
    small = big.resize(size, Image.LANCZOS)
    return np.asarray(small, np.float32) / 255.0


# ------------------------------------------------------------------------------------------------ sphere
def sphere(n=256):
    """Sekke Magnum Cannonball: a translucent sphere with an engraved lattice, a bright rim and spikes at uneven places."""
    xx, yy = grid(n, n)
    c = (n - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    R = 0.60
    rng = np.random.default_rng(7101)
    spikes = []
    k = 11
    base = rng.random() * math.tau
    for i in range(k):
        ang = base + math.tau * i / k + rng.uniform(-0.18, 0.18)
        length = rng.uniform(0.80, 0.97) if i % 3 else rng.uniform(0.66, 0.78)
        halfw = rng.uniform(0.15, 0.22)
        spikes.append((ang, length, halfw))
    prof = np.full_like(r, R)
    best_side = np.zeros_like(r)
    inside_spike = np.zeros_like(r)
    for ang, length, hw in spikes:
        d = np.arctan2(np.sin(th - ang), np.cos(th - ang))
        s = np.clip(1 - np.abs(d) / hw, 0, 1)
        h = R + (length - R) * s ** 1.15
        take = h > prof
        prof = np.where(take, h, prof)
        best_side = np.where(take, d / hw, best_side)
    body = r <= R
    inside = r <= prof
    # sphere shading
    nz = np.sqrt(np.clip(1 - (r / R) ** 2, 0, 1))
    nx, ny = x / R, y / R
    light = np.array([-0.45, -0.55, 0.70])
    light = light / np.linalg.norm(light)
    diff = np.clip(nx * light[0] + ny * light[1] + nz * light[2], 0, 1)
    lum_body = 70 + 150 * diff ** 1.2
    # engraved lattice on the sphere surface: longitude / latitude lines + diamond filigree
    lon = np.arctan2(nx, np.maximum(nz, 1e-3))
    lat = np.arcsin(np.clip(ny, -0.999, 0.999))
    line = np.maximum(smooth(0.93, 1.0, np.abs(np.cos(lon * 6 + 0.4))), smooth(0.93, 1.0, np.abs(np.cos(lat * 7))))
    dia = smooth(0.92, 1.0, np.abs(np.sin(lon * 9) * np.sin(lat * 11)) * 1.0) * 0.8
    lat_band = smooth(0.0, 0.12, np.abs(np.sin(lat * 3.5))) * 0.0
    pat = np.clip(line + dia, 0, 1) * smooth(0.0, 0.25, nz) * body
    rim = smooth(0.7, 1.0, r / R) * body
    alpha_body = 0.30 + 0.35 * rim + 0.35 * diff ** 2 * (1 - rim)
    alpha_body = np.clip(alpha_body + pat * 0.55, 0, 1)
    lum_body = lum_body + pat * 90 + rim * 70
    # specular dot
    sp = np.exp(-(((nx + 0.38) ** 2 + (ny + 0.42) ** 2) * 60)) * body
    lum_body = lum_body + sp * 200
    alpha_body = np.maximum(alpha_body, sp)
    # spike surfaces: two-tone facets (lit left / dark right) + a bright ridge
    spk = inside & ~body
    ridge = 1 - smooth(0.0, 0.18, np.abs(best_side))
    lum_spk = np.where(best_side < 0, 215, 120) * (0.72 + 0.28 * (1 - (r - R) / 0.4)) + ridge * 40
    edge = smooth(0.82, 1.0, r / np.maximum(prof, 1e-3))
    lum_spk = lum_spk + edge * 55
    lum = np.where(body, lum_body, np.where(spk, lum_spk, 0))
    alpha = np.where(body, alpha_body, np.where(spk, 0.92, 0))
    # soft outer edge
    alpha = alpha * (1 - smooth(0.985, 1.0, r / np.maximum(prof, 1e-3)) * 0.7)
    noise = fbm(n, n, 24, 7102) * 0.25 + 0.875
    lum = lum * noise
    # fine hammered grain on the spikes
    grain, _ = cells(n, n, 14, 7103)
    lum = lum * (1 - 0.18 * grain * spk)
    return grey(lum, alpha)


# ------------------------------------------------------------------------------------------------ cannonball streak
def streak(w=256, h=64):
    """Cannonball trail: thin tail on the left (u = 0), fat hot head on the right (u = 1). Brushed-metal grain along the length."""
    xx, yy = grid(w, h)
    u = xx / (w - 1)
    v = (yy - (h - 1) / 2) / ((h - 1) / 2)
    half = 0.10 + 0.88 * u ** 1.5
    cap = np.sqrt(np.clip(1 - np.clip((u - 0.88) / 0.12, 0, 1) ** 2, 0, 1))
    half = half * cap
    d = np.abs(v) / np.maximum(half, 1e-3)
    body = np.clip(1 - d ** 2.2, 0, 1)
    core = np.exp(-(d * 2.4) ** 2) * (0.35 + 0.65 * u)
    grain = fbm(w, h, 8, 7201, 4, aniso=10.0)
    grain2 = fbm(w, h, 4, 7211, 3, aniso=14.0)
    stripes = 0.65 + 0.35 * (grain * 0.6 + grain2 * 0.4) * 1.6
    tail = smooth(0.0, 0.25, u) ** 0.8
    alpha = np.clip(body * stripes * tail * (0.55 + 0.6 * u) + core * 0.5, 0, 1)
    lum = 120 + 135 * np.clip(core * 1.3, 0, 1) + 40 * grain
    # sparks shed along the edge
    rng = np.random.default_rng(7202)
    for _ in range(26):
        sx, sy = rng.uniform(0.15, 0.95), rng.uniform(-1.1, 1.1)
        rr = rng.uniform(0.012, 0.03)
        dd = ((u - sx) * (w / h) / rr) ** 2 + ((v - sy) / (rr * 2.2)) ** 2
        s = np.exp(-dd) * rng.uniform(0.6, 1.0)
        alpha = np.maximum(alpha, s)
        lum = np.maximum(lum, 255 * s)
    return grey(lum, alpha)


# ------------------------------------------------------------------------------------------------ molten ring
def ring(n=256):
    """A hammered bronze ring, hotter on its inner edge, 16 domed rivets, thin guide lines, sparks flying off the outside."""
    xx, yy = grid(n, n)
    c = (n - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    rc, hw = 0.84, 0.115
    d = np.abs(r - rc) / hw
    band = np.clip(1 - d ** 3, 0, 1)
    edge, dist = cells(n, n, 11, 7301)
    hammer = 0.72 + 0.28 * (1 - edge) + 0.12 * fbm(n, n, 16, 7302)
    inner = smooth(0.0, 1.0, np.clip((rc - r) / hw + 0.5, 0, 1))        # hotter toward the inside
    lum = (150 + 85 * inner) * hammer
    lum = lum + 70 * smooth(0.55, 1.0, 1 - d) * (1 - edge) * 0.4
    alpha = band * (0.82 + 0.18 * hammer)
    # glow skirts (soft, for additive use)
    skirt = np.exp(-((r - rc) / 0.16) ** 2) * 0.28
    alpha = np.maximum(alpha, skirt)
    lum = np.where(band > 0.02, lum, 200)
    # guide lines
    for rr, a in ((0.70, 0.5), (0.985, 0.55)):
        ln = np.exp(-((r - rr) / 0.006) ** 2)
        alpha = np.maximum(alpha, ln * a)
        lum = np.where(ln > 0.2, np.maximum(lum, 230), lum)
    # rivets
    for i in range(16):
        a = math.tau * i / 16
        px, py = math.cos(a) * rc, math.sin(a) * rc
        dr = np.sqrt((x - px) ** 2 + (y - py) ** 2) / 0.034
        dome = np.clip(1 - dr ** 2, 0, 1)
        hl = np.exp(-(((x - px + 0.010) ** 2 + (y - py + 0.012) ** 2) / 0.00012))
        lum = np.where(dr < 1.12, 95 + 140 * dome + 120 * hl, lum)
        alpha = np.where(dr < 1.12, np.maximum(alpha, 0.96 * np.clip(1.12 - dr, 0, 0.12) / 0.12), alpha)
    # sparks off the outer edge
    rng = np.random.default_rng(7303)
    for _ in range(30):
        a = rng.uniform(0, math.tau)
        rr = rng.uniform(0.94, 0.995)
        px, py = math.cos(a) * rr, math.sin(a) * rr
        s = np.exp(-(((x - px) ** 2 + (y - py) ** 2) / rng.uniform(0.00005, 0.00018)))
        alpha = np.maximum(alpha, s * 0.9)
        lum = np.maximum(lum, 255 * s)
    alpha = alpha * (1 - smooth(0.985, 1.0, r))
    return grey(lum, alpha)


# ------------------------------------------------------------------------------------------------ riveted band
def band(w=512, h=32):
    """A ribbon of riveted bronze plates (8 panels of 64 px, tiles in U): rails top and bottom, a chevron engraving, 4 rivets a panel."""
    xx, yy = grid(w, h)
    pu = (xx % 64) / 64.0
    v = yy / (h - 1)
    edge_d = np.minimum(v, 1 - v)
    plate = smooth(0.0, 0.10, edge_d)
    seam = 1 - smooth(0.0, 0.035, np.minimum(pu, 1 - pu))              # dark seam between plates
    bevel = smooth(0.03, 0.30, edge_d)
    noise = 0.78 + 0.35 * fbm(w, h, 8, 7401, 3)
    lum = (120 + 60 * bevel + 40 * (1 - v)) * noise
    # rails
    rail = smooth(0.0, 0.05, np.abs(v - 0.14)) * 0 + np.exp(-((v - 0.14) / 0.02) ** 2) + np.exp(-((v - 0.86) / 0.02) ** 2)
    lum = lum + 90 * rail
    # chevron engraving in the middle of each plate
    cx = np.abs(pu - 0.5) * 2
    chev = np.exp(-(((np.abs(v - 0.5) * 2.0) - (1 - cx) * 0.62) / 0.07) ** 2) * smooth(0.0, 0.2, 0.85 - cx)
    lum = lum - 70 * seam + 110 * chev
    # rivets
    for ru, rv in ((0.12, 0.30), (0.88, 0.30), (0.12, 0.70), (0.88, 0.70)):
        dxp = (pu - ru) * 64
        dyp = (v - rv) * h
        dr = np.sqrt(dxp ** 2 + dyp ** 2) / 3.0
        dome = np.clip(1 - dr ** 2, 0, 1)
        hl = np.exp(-(((dxp + 1.0) ** 2 + (dyp + 1.2) ** 2) / 1.2))
        lum = np.where(dr < 1.1, 85 + 130 * dome + 110 * hl, lum)
    alpha = plate * (0.92 - 0.35 * seam) * (0.9 + 0.1 * noise)
    return grey(lum, alpha)


# ------------------------------------------------------------------------------------------------ filigree sigil
def sigil(n=256):
    """Ground sigil of the engraved pattern: rings with ticks, an eight-point star, lace petals, a hex of dots in the middle."""
    S = n * SS
    c = S / 2

    def draw(d, s):
        def circ(rad, wid, fill=255):
            d.ellipse([c - rad * c, c - rad * c, c + rad * c, c + rad * c], outline=fill, width=max(1, int(wid * c)))

        circ(0.975, 0.016)
        circ(0.915, 0.008)
        circ(0.80, 0.020)
        circ(0.745, 0.007)
        circ(0.47, 0.016)
        circ(0.40, 0.006)
        circ(0.17, 0.014)
        # tick marks and studs between the outer rings
        for i in range(48):
            a = math.tau * i / 48
            r0, r1 = (0.835, 0.89) if i % 2 == 0 else (0.855, 0.88)
            d.line([c + math.cos(a) * r0 * c, c + math.sin(a) * r0 * c, c + math.cos(a) * r1 * c, c + math.sin(a) * r1 * c], fill=255, width=max(1, int(0.006 * c)))
        for i in range(16):
            a = math.tau * i / 16 + math.pi / 16
            px, py = c + math.cos(a) * 0.775 * c, c + math.sin(a) * 0.775 * c
            rr = 0.020 * c
            d.ellipse([px - rr, py - rr, px + rr, py + rr], fill=255)
        # eight-point star made of two squares
        for rot in (0.0, math.pi / 4):
            pts = [(c + math.cos(rot + math.pi / 4 + i * math.pi / 2) * 0.745 * c * 1.2, c + math.sin(rot + math.pi / 4 + i * math.pi / 2) * 0.745 * c * 1.2) for i in range(4)]
            clipped = [(c + (px - c) * 0.88, c + (py - c) * 0.88) for px, py in pts]
            d.line(clipped + [clipped[0]], fill=255, width=max(1, int(0.011 * c)))
        # lace petals: each is a pair of arcs meeting at tip
        for i in range(8):
            a = math.tau * i / 8
            tip_r = 0.74
            for side in (-1, 1):
                pts = []
                for t in np.linspace(0, 1, 28):
                    rr = 0.47 + (tip_r - 0.47) * t
                    off = side * math.sin(t * math.pi) * 0.20 * (1 - t * 0.35)
                    aa = a + off * 1.2
                    pts.append((c + math.cos(aa) * rr * c, c + math.sin(aa) * rr * c))
                d.line(pts, fill=255, width=max(1, int(0.008 * c)))
            # bead at the petal centre
            px, py = c + math.cos(a) * 0.62 * c, c + math.sin(a) * 0.62 * c
            rr = 0.016 * c
            d.ellipse([px - rr, py - rr, px + rr, py + rr], fill=255)
        # inner spokes + hex of dots
        for i in range(6):
            a = math.tau * i / 6 + math.pi / 6
            d.line([c + math.cos(a) * 0.17 * c, c + math.sin(a) * 0.17 * c, c + math.cos(a) * 0.40 * c, c + math.sin(a) * 0.40 * c], fill=255, width=max(1, int(0.007 * c)))
            px, py = c + math.cos(a) * 0.29 * c, c + math.sin(a) * 0.29 * c
            rr = 0.016 * c
            d.ellipse([px - rr, py - rr, px + rr, py + rr], fill=255)
        rr = 0.045 * c
        d.ellipse([c - rr, c - rr, c + rr, c + rr], fill=255)

    m = mask_of(draw, (n, n))
    blur = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.6)), np.float32) / 255
    noise = fbm(n, n, 20, 7501)
    alpha = np.clip(m + blur * 0.55, 0, 1)
    lum = 175 + 70 * m + 40 * noise
    return grey(lum, alpha)


# ------------------------------------------------------------------------------------------------ chip
def chip(n=64):
    """One angular hammered metal chip: pointed along the vertical axis, facets lit differently, bright edge."""
    rng = np.random.default_rng(7601)
    S = n * SS * 2
    big = Image.new("L", (S, S), 0)
    fac = Image.new("L", (S, S), 0)
    d, f = ImageDraw.Draw(big), ImageDraw.Draw(fac)
    pts = [(0.50, 0.04), (0.74, 0.34), (0.66, 0.70), (0.52, 0.96), (0.30, 0.66), (0.28, 0.30)]
    pts = [(px * S + rng.uniform(-0.02, 0.02) * S, py * S) for px, py in pts]
    d.polygon(pts, fill=255)
    cx, cy = S * 0.5, S * 0.52
    shades = [235, 170, 110, 70, 125, 205]
    for i in range(len(pts)):
        f.polygon([pts[i], pts[(i + 1) % len(pts)], (cx, cy)], fill=shades[i])
    a = np.asarray(big.resize((n, n), Image.LANCZOS), np.float32) / 255
    l = np.asarray(fac.resize((n, n), Image.LANCZOS), np.float32)
    edge = a - np.asarray(big.filter(ImageFilter.GaussianBlur(S * 0.03)).resize((n, n), Image.LANCZOS), np.float32) / 255
    lum = l * 0.85 + 70 * np.clip(edge * 4, 0, 1) + 40 * fbm(n, n, 8, 7602)
    return grey(lum, a)


def flecks(n=128):
    """A cloud of tiny glints: four-point sparkles of several sizes and round dots on a transparent field (additive use)."""
    xx, yy = grid(n, n)
    rng = np.random.default_rng(7701)
    alpha = np.zeros((n, n), np.float32)
    for i in range(16):
        px, py = rng.uniform(0.08, 0.92) * n, rng.uniform(0.08, 0.92) * n
        size = rng.choice([2.0, 3.0, 4.5, 7.0], p=[0.35, 0.3, 0.25, 0.1])
        dx, dy = xx - px, yy - py
        core = np.exp(-(dx * dx + dy * dy) / (size * size * 0.35))
        arm = np.exp(-np.abs(dx) / (size * 0.25)) * np.exp(-np.abs(dy) / (size * 2.6)) + np.exp(-np.abs(dy) / (size * 0.25)) * np.exp(-np.abs(dx) / (size * 2.6))
        alpha = np.maximum(alpha, np.clip(core + arm * 0.8, 0, 1) * rng.uniform(0.7, 1.0))
    for i in range(30):
        px, py = rng.uniform(0, n), rng.uniform(0, n)
        alpha = np.maximum(alpha, np.exp(-(((xx - px) ** 2 + (yy - py) ** 2) / 1.3)) * rng.uniform(0.4, 0.9))
    return grey(190 + 65 * alpha, alpha)


# ------------------------------------------------------------------------------------------------ wings
def wing(n=256):
    """A pair of feathered metal wings (Sekke Shooting Star), symmetric about the vertical axis, swept up and out. Three rows of feathers."""
    S = n * SS
    lum_img = Image.new("L", (S, S), 0)
    alpha_img = Image.new("L", (S, S), 0)
    dl, da = ImageDraw.Draw(lum_img), ImageDraw.Draw(alpha_img)
    rng = np.random.default_rng(7801)

    def feather(base, ang, length, width, lumv):
        pts_l, pts_r = [], []
        for t in np.linspace(0, 1, 18):
            wprof = width * (math.sin(min(1.0, t * 1.25 + 0.06) * math.pi * 0.5) ** 0.7) * (1 - t ** 3.2)
            cx = base[0] + math.cos(ang) * length * t
            cy = base[1] + math.sin(ang) * length * t
            # slight curl toward the tip
            curl = 0.10 * length * t * t
            cx += -math.sin(ang) * curl
            cy += math.cos(ang) * curl
            nx, ny = -math.sin(ang), math.cos(ang)
            pts_l.append((cx + nx * wprof, cy + ny * wprof))
            pts_r.append((cx - nx * wprof, cy - ny * wprof))
        poly = pts_l + pts_r[::-1]
        da.polygon(poly, fill=255)
        dl.polygon(poly, fill=int(lumv * 0.55))                         # dark outline
        inner = []
        for t_ in np.linspace(0, 1, 18):
            pass
        # lit half and shaded half, ridge down the shaft
        half_a = pts_l + [pts_l[-1]] + [((a[0] + b[0]) / 2, (a[1] + b[1]) / 2) for a, b in zip(pts_l[::-1], pts_r[::-1])]
        dl.polygon(pts_l + [((a[0] + b[0]) / 2, (a[1] + b[1]) / 2) for a, b in zip(pts_l[::-1], pts_r[::-1])], fill=int(lumv))
        dl.polygon(pts_r + [((a[0] + b[0]) / 2, (a[1] + b[1]) / 2) for a, b in zip(pts_r[::-1], pts_l[::-1])], fill=int(lumv * 0.72))
        mid = [((a[0] + b[0]) / 2, (a[1] + b[1]) / 2) for a, b in zip(pts_l, pts_r)]
        dl.line(mid, fill=255, width=max(2, int(S * 0.004)))
        # engraved barbs
        for t in np.linspace(0.18, 0.85, 7):
            i = int(t * 17)
            a, b = pts_l[i], pts_r[i]
            m = mid[i]
            tipx, tipy = m[0] + math.cos(ang) * length * 0.12, m[1] + math.sin(ang) * length * 0.12
            dl.line([a, (tipx, tipy)], fill=int(lumv * 0.5), width=max(1, int(S * 0.0025)))
            dl.line([b, (tipx, tipy)], fill=int(lumv * 0.5), width=max(1, int(S * 0.0025)))

    for side in (-1, 1):
        root = (S * 0.5 + side * S * 0.04, S * 0.70)
        wrist = (S * 0.5 + side * S * 0.20, S * 0.40)
        rows = [(8, 1.00, 0.075, 150), (7, 0.62, 0.062, 195), (6, 0.34, 0.052, 235)]
        for ri, (cnt, lenk, wk, lv) in enumerate(rows):
            for i in range(cnt):
                t = i / (cnt - 1)
                bx = root[0] + (wrist[0] - root[0]) * (0.18 + 0.82 * t) * (1.0 if ri == 0 else 0.92)
                by = root[1] + (wrist[1] - root[1]) * (0.18 + 0.82 * t) * (1.0 if ri == 0 else 0.92) + (ri * S * 0.012)
                base_ang = math.radians(lerp_(12, -62, t) + rng.uniform(-3, 3))
                ang = base_ang if side > 0 else math.pi - base_ang
                length = S * (0.40 * lenk) * (0.70 + 0.30 * math.sin(math.pi * (0.15 + 0.85 * t)))
                feather((bx, by), ang, length, S * wk * 0.5, lv + rng.uniform(-14, 14))
    a = np.asarray(alpha_img.resize((n, n), Image.LANCZOS), np.float32) / 255
    l = np.asarray(lum_img.resize((n, n), Image.LANCZOS), np.float32)
    noise = fbm(n, n, 16, 7802)
    l = l * (0.84 + 0.3 * noise)
    return grey(l, a)


def lerp_(a, b, t):
    return a + (b - a) * t


# ------------------------------------------------------------------------------------------------ spike
def spike(w=64, h=128):
    """A bronze spike rising from the ground: base at the bottom (fading into the earth), tip at the top; lit left, shaded right."""
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    t = 1 - yy / (h - 1)                                  # 0 at the base, 1 at the tip
    half = (1 - t) ** 0.95 * 0.62
    inside = np.abs(u) <= half
    side = u / np.maximum(half, 1e-3)
    grain = fbm(w, h, 8, 7901, 3)
    lum = np.where(side < -0.05, 210, np.where(side < 0.05, 255, 105)) * (0.72 + 0.4 * grain) * (0.8 + 0.25 * t)
    lum = lum + 60 * np.exp(-(np.abs(u) / 0.06) ** 2) * 0.0
    ring = np.exp(-(((t * 7.0) % 1.0 - 0.5) / 0.05) ** 2) * 0.0
    # hammered bands across the spike
    bands = 1 - 0.22 * smooth(0.9, 1.0, np.abs(np.sin(t * 22 + side * 0.5)))
    lum = lum * bands
    alpha = inside * (0.15 + 0.85 * smooth(0.0, 0.22, t)) * (1 - 0.25 * smooth(0.9, 1.0, np.abs(side)))
    edge = np.clip(1 - np.abs(np.abs(u) - half) / 0.10, 0, 1) * inside
    lum = lum + 50 * edge
    alpha = alpha * (0.4 + 0.6 * smooth(0.0, 0.1, t)) if False else alpha
    return grey(lum, alpha)


# ------------------------------------------------------------------------------------------------ lizard
def lizard(w=256, h=128):
    """Top view of the dark bronze Poison Lizard, head to the right, scaled back, curved tail, four splayed legs."""
    W, H = w * SS, h * SS

    def curve(x):
        return H * 0.5 + math.sin(x / W * math.tau * 1.15 + 0.6) * H * 0.10 * (1 - x / W * 0.55)

    def draw(d, s):
        # body from tail tip (x = 0.03 W) to the neck (0.74 W)
        for x in np.linspace(0.02 * W, 0.78 * W, 220):
            t = (x / W - 0.02) / 0.76
            if t < 0.55:
                rad = H * (0.025 + 0.150 * (t / 0.55) ** 0.9)
            else:
                rad = H * (0.175 - 0.070 * ((t - 0.55) / 0.45) ** 1.2)
            y = curve(x)
            d.ellipse([x - rad * 1.3, y - rad, x + rad * 1.3, y + rad], fill=255)
        # head: a wedge with a snout
        hx, hy = 0.78 * W, curve(0.78 * W)
        d.polygon([(hx - 0.02 * W, hy - 0.075 * H), (hx + 0.07 * W, hy - 0.115 * H), (hx + 0.19 * W, hy - 0.035 * H),
                   (hx + 0.205 * W, hy), (hx + 0.19 * W, hy + 0.035 * H), (hx + 0.07 * W, hy + 0.115 * H), (hx - 0.02 * W, hy + 0.075 * H)], fill=255)
        # legs
        for lx, sign, ph in ((0.40, -1, 0), (0.40, 1, 1), (0.69, -1, 1), (0.69, 1, 0)):
            ax, ay = lx * W, curve(lx * W)
            kx, ky = ax - 0.04 * W * (1 if ph else -0.3), ay + sign * 0.25 * H
            fx, fy = kx + (0.04 if ph else 0.07) * W, ky + sign * 0.13 * H
            d.line([(ax, ay), (kx, ky)], fill=255, width=int(0.11 * H))
            d.line([(kx, ky), (fx, fy)], fill=255, width=int(0.085 * H))
            for k in (-1, 0, 1):
                d.line([(fx, fy), (fx + 0.03 * W, fy + sign * 0.06 * H + k * 0.05 * H)], fill=255, width=int(0.040 * H))

    m = mask_of(draw, (w, h))
    xx, yy = grid(w, h)
    # distance to the spine -> a ridge down the middle and scale cells over the body
    spine = np.array([curve(x * SS) / SS for x in range(w)], np.float32)[None, :]
    dy = (yy - spine)
    ridge = np.exp(-(dy / 3.2) ** 2)
    edge, dist = cells(w, h, 12, 8001)
    scales = 0.62 + 0.38 * (1 - edge)
    lum = (95 + 55 * ridge) * scales
    shade = np.clip(0.5 - dy / 28.0, 0.0, 1.0)
    lum = lum * (0.7 + 0.5 * shade)
    # bright rim so it reads on a dark ground
    blur = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.4)), np.float32) / 255
    rim = np.clip(m - blur, 0, 1)
    lum = lum + rim * 240
    # eyes
    for ex, ey in ((0.92, -0.045), (0.92, 0.045)):
        px, py = ex * w, curve(0.78 * W) / SS + ey * h * 1.3
        lum = np.maximum(lum, 255 * np.exp(-(((xx - px) ** 2 + (yy - py) ** 2) / 5.0)))
    return grey(lum, m)


# ------------------------------------------------------------------------------------------------ patina
def patina(n=128):
    """Verdigris crust (the one coloured sprite): ragged green blotches over bronze brown with drips running down, round ragged outline."""
    crust = fbm(n, n, 28, 8101, 5)
    detail = fbm(n, n, 8, 8111, 3)
    drip = value_noise(n, n, 4, 64, 8121)            # long vertical streaks
    dripmask = smooth(0.52, 0.7, drip) * smooth(0.25, 0.7, crust)
    blot = smooth(0.50, 0.62, crust + 0.18 * detail + 0.10 * dripmask)
    mixn = fbm(n, n, 16, 8131, 3)
    green_dark = np.array([40, 120, 98], np.float32)
    green_lite = np.array([98, 178, 142], np.float32)
    brown = np.array([112, 70, 34], np.float32)
    col = green_dark + (green_lite - green_dark) * smooth(0.35, 0.8, mixn + detail * 0.4)[..., None]
    col = col + (brown - col) * (1 - blot)[..., None] * 0.9
    sparkle = smooth(0.78, 0.9, detail)[..., None] * 60
    col = col + sparkle
    alpha = np.clip(blot * 0.92 + (1 - blot) * smooth(0.30, 0.46, crust) * 0.40, 0, 1)
    alpha = np.maximum(alpha, dripmask * 0.55)
    # round, ragged outline so a square quad reads as a stain, not a tile
    xx, yy = grid(n, n)
    rr = np.sqrt(((xx - (n - 1) / 2) / (n / 2)) ** 2 + ((yy - (n - 1) / 2) / (n / 2)) ** 2)
    rr = rr + 0.28 * (fbm(n, n, 24, 8141, 4) - 0.5)
    alpha = alpha * (1 - smooth(0.52, 0.92, rr))
    return Image.fromarray(np.dstack([np.clip(col[..., 0], 0, 255), np.clip(col[..., 1], 0, 255), np.clip(col[..., 2], 0, 255), alpha * 255]).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------ burst
def burst(n=256):
    """Starburst of cannonball peaks: a hot core, 18 thin spikes of two lengths, a faint ring where the shock stands (additive use)."""
    xx, yy = grid(n, n)
    c = (n - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    rng = np.random.default_rng(8201)
    alpha = np.exp(-(r / 0.16) ** 2) * 1.0 + np.exp(-(r / 0.40) ** 2) * 0.28
    lum = 215 + 40 * np.exp(-(r / 0.12) ** 2)
    k = 18
    for i in range(k):
        ang = math.tau * i / k + rng.uniform(-0.07, 0.07)
        L = rng.uniform(0.82, 0.98) if i % 2 == 0 else rng.uniform(0.40, 0.60)
        w0 = 0.055 if i % 2 == 0 else 0.04
        d = np.arctan2(np.sin(th - ang), np.cos(th - ang))
        along = r * np.cos(d)
        perp = np.abs(r * np.sin(d))
        wid = w0 * np.clip(1 - along / L, 0, 1) ** 1.1 + 0.002
        s = np.clip(1 - perp / wid, 0, 1) ** 1.4 * (along > 0) * (along < L) * (0.55 + 0.45 * (1 - along / L))
        alpha = np.maximum(alpha, s)
    ring_ = np.exp(-((r - 0.58) / 0.012) ** 2) * 0.45
    alpha = np.maximum(alpha, ring_)
    lum = lum - 40 * smooth(0.0, 1.0, r)
    alpha = alpha * (1 - smooth(0.93, 1.0, r))
    return grey(lum, alpha)


def figure(n=256):
    """A cast-metal statuette in a flying pose (the owner's still: a small verdigris figure sweeping through a haze). Cel shaded:
    a dark outline, a bright lit side, a flat shadow side, a few hammered flecks. Grey with alpha so the tint makes it verdigris."""
    def body(d, k):
        P = lambda pts: [(x * n * k, y * n * k) for x, y in pts]
        d.polygon(P([(0.08, 0.30), (0.30, 0.40), (0.52, 0.47), (0.60, 0.42), (0.58, 0.52), (0.40, 0.56), (0.20, 0.38), (0.06, 0.34)]), fill=255)   # outstretched arm
        d.polygon(P([(0.50, 0.40), (0.62, 0.43), (0.70, 0.60), (0.92, 0.80), (0.90, 0.86), (0.66, 0.74), (0.56, 0.58)]), fill=255)               # torso + leg sweep
        d.polygon(P([(0.60, 0.60), (0.74, 0.50), (0.80, 0.54), (0.66, 0.68)]), fill=255)                                                     # bent knee
        d.ellipse([0.50 * n * k, 0.26 * n * k, 0.64 * n * k, 0.42 * n * k], fill=255)                                                          # head
        d.polygon(P([(0.49, 0.30), (0.50, 0.19), (0.54, 0.25), (0.57, 0.15), (0.60, 0.24), (0.66, 0.17), (0.65, 0.30)]), fill=255)       # spiky hair
    m = mask_of(body, (n, n))
    edge = m - np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(7)), np.float32) / 255.0
    xx, yy = grid(n, n)
    lit = np.clip(0.5 + 0.5 * np.sin((xx * 0.7 - yy) / n * 5.0), 0, 1)
    fl = fbm(n, n, 16, 7301, 3)
    lum = np.where(lit > 0.55, 250, 170) - 40 * fl
    lum = np.where(edge > 0.4, 40, lum)
    return grey(lum, np.clip(m * 1.2, 0, 1))


def haze(n=256):
    """Soft white cloud with faint radial light streaks, the glow that wraps the figure in the still (additive)."""
    xx, yy = grid(n, n)
    c = (n - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    f = fbm(n, n, 32, 7302, 4)
    a = np.exp(-(r / 0.55) ** 2) * (0.55 + 0.7 * f)
    rays = np.clip(np.sin(th * 14 + f * 6) , 0, 1) ** 6 * np.exp(-(r / 0.8) ** 2) * 0.5
    a = np.clip(a + rays, 0, 1) * (1 - smooth(0.8, 1.0, r))
    return grey(235 + 20 * f, a)


if __name__ == "__main__":
    save(sphere(), "bronze_sphere")
    save(streak(), "bronze_streak")
    save(ring(), "bronze_ring")
    save(band(), "bronze_band")
    save(sigil(), "bronze_sigil")
    save(chip(), "bronze_chip")
    save(flecks(), "bronze_flecks")
    save(wing(), "bronze_wing")
    save(spike(), "bronze_spike")
    save(lizard(), "bronze_lizard")
    save(patina(), "bronze_patina")
    save(burst(), "bronze_burst")
    save(figure(), "bronze_figure")
    save(haze(), "bronze_haze")

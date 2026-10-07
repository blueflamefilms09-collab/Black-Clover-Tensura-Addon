"""Generates the Fungus Magic VFX textures (spore clouds, spores, mycelium, cap gills, fairy rings, mushrooms ...).

Deterministic (fixed seeds), font-free, Pillow + numpy only.
Output: src/main/resources/assets/nusmp/textures/particle/fungus_*.png

    python3 -B tools/gen_fungus_textures.py              # everything
    python3 -B tools/gen_fungus_textures.py puff gills   # only some of them (names of the functions in MAKERS)

Look reference (Black Clover wiki, Fungus Magic): the user generates and manipulates fungi: spores are put on a target and later sprout into a
mushroom (Heavy Mr. Mushroom: a large mushroom with an old, tired face that pins the target down), a giant Pleurotus eryngii (king oyster)
mushroom with arms, legs and a face grows from an open grimoire (Towering Mr. Mushroom). In the mod it applies localized poison and siphons
magicules from anything in the area, so the look is: warm gold-ochre spore clouds, cream-white mycelium threads crawling over the ground,
mushrooms of several species popping out of it, gill-patterned sigils and fairy rings, with a toxic yellow-green foxfire glow.

Texture convention (same as the rest of textures/particle): white / grey with alpha where the vertex colour tints them (clouds, spores,
mycelium, gills, rings, flares, strands); real colours where the colour is the point (the mushrooms, the debris, Mr. Mushroom).
Atlases (cells are laid out left to right, top to bottom, with a clear margin so linear filtering never bleeds):
    fungus_spores   128 x 128  2 x 2 cells of 64: 0 spore grains, 1 bubble spore (bokeh ring), 2 dandelion pod (pappus), 3 twinkle
    fungus_shrooms  512 x 256  4 x 1 cells of 128 x 256: 0 king oyster, 1 fly agaric, 2 brown cluster, 3 foxfire (glowing) cluster
    fungus_debris   128 x 128  2 x 2 cells of 64: 0 cap chunk, 1 stem chunk, 2 spore pod, 3 baby mushroom
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4          # supersampling of the PIL-drawn line work
HS = 3          # supersampling of the analytically shaded mushrooms
TAU = math.tau
LANCZOS = Image.Resampling.LANCZOS


# ------------------------------------------------------------------------------------------------ helpers
def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def rgba_img(rgb, a):
    rgb = np.asarray(rgb, np.float32)
    if rgb.ndim == 2:
        rgb = np.dstack([rgb] * 3)
    out = np.dstack([np.clip(rgb, 0, 255), np.clip(a, 0, 1) * 255])
    return Image.fromarray(np.rint(out).astype(np.uint8), "RGBA")


def value_noise(w, h, cell, seed, tile=True):
    """Smooth value noise in 0..1 (tileable on both axes); cell = pixels per lattice cell, or (cx, cy)."""
    cx, cy = (cell, cell) if np.isscalar(cell) else cell
    rng = np.random.default_rng(seed)
    gw, gh = max(1, int(round(w / cx))), max(1, int(round(h / cy)))
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


def fbm(w, h, cell, seed, octaves=4, tile=True):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + value_noise(w, h, max(2, int(cell) >> o), seed + o * 7, tile) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def circ_noise(theta, cells, seed):
    """Periodic 1-D value noise of an angle (0..1)."""
    g = np.random.default_rng(seed).random(cells).astype(np.float32)
    t = (theta % TAU) / TAU * cells
    i0 = np.floor(t).astype(int) % cells
    i1 = (i0 + 1) % cells
    f = t - np.floor(t)
    f = f * f * (3 - 2 * f)
    return g[i0] * (1 - f) + g[i1] * f


def gblur(a, sigma):
    """Gaussian blur through the FFT (periodic borders: keep the content away from the edge)."""
    a = np.asarray(a, np.float32)
    if sigma <= 0:
        return a
    if a.ndim == 3:
        return np.dstack([gblur(a[..., i], sigma) for i in range(a.shape[2])])
    h, w = a.shape
    fy = np.fft.fftfreq(h).astype(np.float32)[:, None]
    fx = np.fft.fftfreq(w).astype(np.float32)[None, :]
    k = np.exp(-2.0 * (math.pi * sigma) ** 2 * (fx ** 2 + fy ** 2))
    return np.fft.ifft2(np.fft.fft2(a) * k).real.astype(np.float32)


def box_down(a, f):
    h, w = a.shape[0] // f, a.shape[1] // f
    if a.ndim == 2:
        return a[:h * f, :w * f].reshape(h, f, w, f).mean(axis=(1, 3))
    return a[:h * f, :w * f].reshape(h, f, w, f, a.shape[2]).mean(axis=(1, 3))


def mask_down(im, size):
    """A PIL 'L' drawing made at SS x the size -> float 0..1 at size."""
    if isinstance(size, int):
        size = (size, size)
    return np.asarray(im.resize(size, LANCZOS), np.float32) / 255.0


def dot(a, cx, cy, sigma, amp=1.0):
    """Adds a gaussian dot to the float image a (in place)."""
    h, w = a.shape
    r = int(sigma * 4 + 2)
    x0, x1, y0, y1 = max(0, int(cx) - r), min(w, int(cx) + r + 1), max(0, int(cy) - r), min(h, int(cy) + r + 1)
    if x0 >= x1 or y0 >= y1:
        return
    xx, yy = np.meshgrid(np.arange(x0, x1, dtype=np.float32), np.arange(y0, y1, dtype=np.float32))
    a[y0:y1, x0:x1] += amp * np.exp(-(((xx + 0.5 - cx) ** 2 + (yy + 0.5 - cy) ** 2) / (2 * sigma * sigma)))


class Canvas:
    """Float RGBA canvas with straight-alpha 'over' compositing (used by the mushroom sprites)."""

    def __init__(self, w, h):
        self.rgb = np.zeros((h, w, 3), np.float32)
        self.a = np.zeros((h, w), np.float32)

    def over(self, rgb, m, x0=0, y0=0):
        h, w = m.shape
        H, W = self.a.shape
        sx0, sy0 = max(0, -x0), max(0, -y0)
        dx0, dy0 = max(0, x0), max(0, y0)
        cw, ch = min(w - sx0, W - dx0), min(h - sy0, H - dy0)
        if cw <= 0 or ch <= 0:
            return
        m = np.clip(m[sy0:sy0 + ch, sx0:sx0 + cw], 0, 1)
        if rgb.ndim == 3:
            rgb = rgb[sy0:sy0 + ch, sx0:sx0 + cw]
        da = self.a[dy0:dy0 + ch, dx0:dx0 + cw]
        out_a = m + da * (1 - m)
        dr = self.rgb[dy0:dy0 + ch, dx0:dx0 + cw]
        col = rgb * m[..., None] if rgb.ndim == 3 else np.asarray(rgb, np.float32)[None, None, :] * m[..., None]
        self.rgb[dy0:dy0 + ch, dx0:dx0 + cw] = (col + dr * (da * (1 - m))[..., None]) / np.maximum(out_a, 1e-6)[..., None]
        self.a[dy0:dy0 + ch, dx0:dx0 + cw] = out_a

    def image(self, f):
        prem = box_down(self.rgb * self.a[..., None], f)
        a = box_down(self.a, f)
        rgb = prem / np.maximum(a, 1e-6)[..., None]
        return rgba_img(rgb, a)


# ------------------------------------------------------------------------------------------------ spore clouds
def puff(name="fungus_puff", size=256, seed=1, squash=1.0, warp=0.55, mass=0.80):
    """A billowing spore-cloud puff: a soft round mass with a warped, lumpy outline, lit from the upper left, thinner at the edge, with spores
    suspended in it. Grey with alpha: the vertex colour makes it an ochre, olive or cream cloud."""
    rng = np.random.default_rng(seed)
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c / squash
    wx = fbm(size, size, 90, seed + 1, 4, tile=False) - 0.5
    wy = fbm(size, size, 90, seed + 2, 4, tile=False) - 0.5
    xs, ys = x + wx * warp, y + wy * warp
    rs = np.sqrt(xs * xs + ys * ys)
    base = 1 - smooth(0.12, mass, rs)
    billow = fbm(size, size, 44, seed + 3, 5, tile=False)
    d = base * (0.35 + 1.15 * billow)
    d = d * (0.85 + 0.3 * fbm(size, size, 12, seed + 4, 3, tile=False))
    alpha = np.clip(d * 1.25, 0, 1) ** 1.25 * smooth(1.0, 0.62, np.sqrt(x * x + y * y))
    hmap = gblur(d, 6.0)
    gy, gx = np.gradient(hmap)
    lit = 0.6 * gx + 0.8 * gy
    shade = np.clip(0.80 + lit * 7.0, 0.62, 1.0)
    shade = shade * (1 - 0.20 * smooth(-0.3, 1.0, y)) * (1 - 0.12 * smooth(0.4, 1.2, d))
    shade = shade * (0.94 + 0.12 * fbm(size, size, 6, seed + 5, 2, tile=False))
    spec = np.zeros((size, size), np.float32)
    for _ in range(170):                                        # spores hanging in the cloud
        px, py = rng.uniform(0.12, 0.88) * size, rng.uniform(0.12, 0.88) * size
        if alpha[int(py), int(px)] > 0.4:
            dot(spec, px, py, rng.uniform(0.7, 1.4), rng.uniform(0.35, 0.8))
    shade = np.clip(shade + spec * 0.5, 0, 1)
    save(rgba_img(shade * 255, np.clip(alpha * 0.95 + spec * 0.2 * (alpha > 0.2), 0, 1)), name)


def spores(size=128):
    """Atlas 2x2 (64 px cells): 0 spore grains, 1 bubble spore, 2 dandelion pod, 3 twinkle. White, drawn additively."""
    cell = size // 2
    rng = np.random.default_rng(404)
    out = np.zeros((size, size), np.float32)
    xx, yy = grid(cell, cell)
    c = (cell - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    # 0: a clump of grains of different sizes, each with its own soft halo
    g = np.zeros((cell, cell), np.float32)
    for gx_, gy_, s, amp in [(0.0, 0.02, 3.2, 1.0), (-0.40, -0.30, 2.1, 0.85), (0.42, -0.22, 1.8, 0.8), (0.30, 0.42, 2.4, 0.9),
                             (-0.34, 0.38, 1.5, 0.7), (0.05, -0.55, 1.3, 0.65), (-0.62, 0.05, 1.2, 0.6)]:
        dot(g, c + gx_ * c, c + gy_ * c, s, amp)
        dot(g, c + gx_ * c, c + gy_ * c, s * 3.4, amp * 0.20)
    out[0:cell, 0:cell] = np.clip(g, 0, 1)
    # 1: a bubble spore: bright rim, faint body, a glint
    ring = np.exp(-((r - 0.56) / 0.07) ** 2)
    body = 0.22 * smooth(0.62, 0.0, r) + 0.12 * np.exp(-((r - 0.74) / 0.22) ** 2)
    glint = np.exp(-(((x + 0.25) / 0.16) ** 2 + ((y + 0.28) / 0.07) ** 2)) * 0.9
    out[0:cell, cell:size] = np.clip(ring * (0.75 + 0.25 * (x * -0.5 + y * -0.5 + 1) / 2) + body + glint, 0, 1)
    # 2: a dandelion-like pod: a bright core with fine radiating hairs ending in tiny beads
    th = np.arctan2(y, x)
    hair = np.zeros_like(r)
    n_hair = 11
    for k in range(n_hair):
        a0 = TAU * k / n_hair + 0.2
        da = np.abs(((th - a0 + math.pi) % TAU) - math.pi)
        across = np.exp(-((da * np.maximum(r, 0.05)) / 0.026) ** 2)
        hair = np.maximum(hair, across * smooth(0.10, 0.22, r) * smooth(0.88, 0.50, r) * 0.85)
        bx, by = math.cos(a0) * 0.8, math.sin(a0) * 0.8
        hair = np.maximum(hair, np.exp(-(((x - bx) ** 2 + (y - by) ** 2) / 0.0042)) * 0.9)
    core = np.exp(-(r / 0.13) ** 2) + 0.28 * np.exp(-(r / 0.40) ** 2)
    out[cell:size, 0:cell] = np.clip(hair + core, 0, 1)
    # 3: a four-point twinkle with a soft diagonal cross
    ax, ay = np.abs(x), np.abs(y)
    star = np.maximum(np.exp(-(ay / 0.035)) * smooth(1.0, 0.0, ax) ** 1.6, np.exp(-(ax / 0.035)) * smooth(1.0, 0.0, ay) ** 1.6)
    diag = np.exp(-((np.abs(ax - ay) * 0.7071) / 0.05) ** 2) * smooth(0.65, 0.05, r) * 0.45
    core3 = np.exp(-(r / 0.10) ** 2) + 0.22 * np.exp(-(r / 0.34) ** 2)
    out[cell:size, cell:size] = np.clip(star * 1.3 + diag + core3, 0, 1)
    # keep a clear margin in every cell (linear filtering never bleeds into the neighbour)
    for ci in range(2):
        for cj in range(2):
            sub = out[ci * cell:(ci + 1) * cell, cj * cell:(cj + 1) * cell]
            sub *= smooth(1.0, 0.86, np.sqrt(x * x + y * y)) * (1 - (np.maximum(np.abs(x), np.abs(y)) > 0.94))
    save(rgba_img(np.full((size, size), 255.0), out), "fungus_spores")


# ------------------------------------------------------------------------------------------------ mycelium
def mycelium(size=256, seed=5):
    """Top-down mycelium: a web of cream hyphae growing out of a bright knot, branching three times, fine hairs and glowing nodes.
    Lies on the ground (a plane); white, drawn additively over a dark stain."""
    rng = np.random.default_rng(seed)
    S = size * SS
    c = S / 2
    core = Image.new("L", (S, S), 0)
    hair = Image.new("L", (S, S), 0)
    node = Image.new("L", (S, S), 0)
    dc, dh, dn = ImageDraw.Draw(core), ImageDraw.Draw(hair), ImageDraw.Draw(node)
    limit = c * 0.955
    widths = [6.6, 4.6, 3.3, 2.4]

    def grow(x, y, ang, length, depth, bias):
        step = S * 0.0095
        pts = [(x, y)]
        n = max(2, int(length / step))
        a = ang
        branch_p = [0.10, 0.08, 0.06, 0.0][depth]
        for i in range(n):
            a += rng.normal(0, 0.12) + bias * 0.02
            x += math.cos(a) * step
            y += math.sin(a) * step
            if math.hypot(x - c, y - c) > limit:
                break
            pts.append((x, y))
            if depth < 3 and i > 3 and rng.random() < branch_p:
                side = 1 if rng.random() < 0.5 else -1
                grow(x, y, a + side * rng.uniform(0.45, 1.05), (n - i) * step * rng.uniform(0.40, 0.70), depth + 1, -side)
                dn.ellipse([x - 3.2 * SS * 0.5, y - 3.2 * SS * 0.5, x + 3.2 * SS * 0.5, y + 3.2 * SS * 0.5], fill=255)
        m = len(pts) - 1
        for i in range(m):
            w = max(1.0, widths[depth] * (1.0 - 0.55 * i / max(1, m)))
            dc.line([pts[i], pts[i + 1]], fill=255, width=int(round(w)))
            dc.ellipse([pts[i][0] - w / 2, pts[i][1] - w / 2, pts[i][0] + w / 2, pts[i][1] + w / 2], fill=255)
            if i % 2 == 0 and rng.random() < 0.65:                      # fine hairs
                hang = a + (math.pi / 2 if rng.random() < 0.5 else -math.pi / 2) + rng.normal(0, 0.45)
                hl = S * rng.uniform(0.006, 0.026)
                dh.line([pts[i], (pts[i][0] + math.cos(hang) * hl, pts[i][1] + math.sin(hang) * hl)], fill=255, width=1)

    n_primary = 16
    for k in range(n_primary):
        ang = TAU * k / n_primary + rng.normal(0, 0.12)
        r0 = S * 0.035
        grow(c + math.cos(ang) * r0, c + math.sin(ang) * r0, ang, c * rng.uniform(0.80, 0.95), 0, 1 if k % 2 else -1)
    for k in range(7):                                                   # shorter hyphae starting further out fill the gaps
        ang = TAU * (k + 0.5) / 7 + rng.normal(0, 0.2)
        r0 = c * rng.uniform(0.14, 0.30)
        grow(c + math.cos(ang) * r0, c + math.sin(ang) * r0, ang, c * rng.uniform(0.40, 0.60), 1, 1)
    for k in range(95):                                                  # the meandering mat of fine hyphae between the main cords
        rr0, th0 = c * 0.88 * math.sqrt(rng.uniform(0.02, 1)), rng.uniform(0, TAU)
        x, y = c + math.cos(rr0 and th0) * rr0, c + math.sin(th0) * rr0
        a = rng.uniform(0, TAU)
        turn = rng.normal(0, 0.05)
        pts = [(x, y)]
        for i in range(int(rng.uniform(18, 46))):
            a += turn + rng.normal(0, 0.28)
            x += math.cos(a) * S * 0.0085
            y += math.sin(a) * S * 0.0085
            if math.hypot(x - c, y - c) > limit:
                break
            pts.append((x, y))
        for i in range(len(pts) - 1):
            dh.line([pts[i], pts[i + 1]], fill=255, width=int(SS * 0.9))
    dc.ellipse([c - S * 0.032, c - S * 0.032, c + S * 0.032, c + S * 0.032], fill=255)
    core_a, hair_a, node_a = mask_down(core, size), mask_down(hair, size), mask_down(node, size)
    glow = gblur(core_a, 2.6)
    xx, yy = grid(size, size)
    r = np.sqrt((xx - (size - 1) / 2) ** 2 + (yy - (size - 1) / 2) ** 2) / ((size - 1) / 2)
    knot = np.exp(-(r / 0.07) ** 2) * 0.9 + np.exp(-(r / 0.22) ** 2) * 0.22
    a = np.clip(core_a * 1.0 + hair_a * 0.75 + glow * 0.55 + node_a * 0.8 + knot, 0, 1) * smooth(1.0, 0.88, r)
    save(rgba_img(255 * np.clip(0.72 + 0.28 * core_a + 0.3 * node_a, 0, 1), a), "fungus_mycelium")


# ------------------------------------------------------------------------------------------------ gills and the fairy ring
def gills(size=256):
    """The underside of a mushroom cap as a sigil: ~100 curved gills in three lengths sweeping out of the stem ring to a scalloped margin,
    the annulus, a ring of spore dots. Lies on the ground (a plane); white."""
    S = size * SS
    c = S / 2
    R = c
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)

    def P(r, th):
        return (c + math.cos(th) * r * R, c + math.sin(th) * r * R)

    n = 80
    for k in range(n):
        th0 = TAU * k / n
        lvl = k % 4
        r0, br, wk = ((0.215, 255, 1.0), (0.50, 205, 0.8), (0.34, 235, 0.9), (0.62, 185, 0.7))[lvl]
        r1 = 0.925
        pts = []
        steps = 20
        for i in range(steps + 1):
            r = r0 + (r1 - r0) * i / steps
            th = th0 + 0.46 * ((r - 0.2) / 0.73) ** 1.35
            pts.append(P(r, th))
        for i in range(steps):
            w = (0.8 + 1.7 * i / steps) * SS * wk
            d.line([pts[i], pts[i + 1]], fill=br, width=max(1, int(round(w))))
    def ring(r, w, fill=255):
        d.ellipse([c - r * R, c - r * R, c + r * R, c + r * R], outline=fill, width=max(1, int(round(w * SS))))
    ring(0.196, 2.6)                                    # the annulus
    ring(0.150, 1.2, 210)
    ring(0.955, 2.0)                                    # the margin
    ring(0.605, 0.9, 120)
    ring(0.30, 0.8, 100)
    for k in range(52):                                 # scalloped margin
        th = TAU * k / 52
        px, py = P(0.972, th)
        rr = 0.021 * R
        d.ellipse([px - rr, py - rr, px + rr, py + rr], outline=255, width=max(1, SS))
    for k in range(72):                                 # spore dots between the gills
        th = TAU * (k + 0.5) / 72
        px, py = P(0.695, th)
        rr = (1.1 + 0.7 * ((k % 3) == 0)) * SS * 0.5
        d.ellipse([px - rr, py - rr, px + rr, py + rr], fill=255)
    for k in range(8):                                  # the stem's star in the hub
        th = TAU * k / 8
        d.line([P(0.04, th), P(0.135, th)], fill=255, width=max(1, int(1.8 * SS)))
    d.ellipse([c - 0.035 * R, c - 0.035 * R, c + 0.035 * R, c + 0.035 * R], fill=255)
    core = mask_down(im, size)
    xx, yy = grid(size, size)
    r = np.sqrt((xx - (size - 1) / 2) ** 2 + (yy - (size - 1) / 2) ** 2) / ((size - 1) / 2)
    body = 0.11 * smooth(0.12, 0.2, r) * smooth(0.97, 0.9, r)
    a = np.clip(core + gblur(core, 2.0) * 0.50 + body, 0, 1) * smooth(1.0, 0.965, r)
    save(rgba_img(255 * (0.74 + 0.26 * core), a), "fungus_gills")


def cap_tile(R):
    """A mushroom cap seen from above (lum, alpha): a domed disc lit from the upper left with a darker umbo and a bright rim."""
    n = int(R * 2 + 4)
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    c = (n - 1) / 2
    x, y = (xx - c) / R, (yy - c) / R
    r = np.sqrt(x * x + y * y)
    a = np.clip((1 - r) * R, 0, 1)
    z = np.sqrt(np.clip(1 - r * r, 0, 1))
    lam = np.clip(-0.45 * x - 0.5 * y + 0.74 * z, 0, 1)
    lum = 80 + 150 * lam
    lum = lum + 55 * np.exp(-(((x + 0.32) / 0.30) ** 2 + ((y + 0.36) / 0.20) ** 2))
    lum = lum * (0.78 + 0.22 * smooth(0.0, 0.55, r))
    lum = lum + 30 * np.exp(-((r - 0.9) / 0.1) ** 2)
    return np.clip(lum, 0, 255), a


def ring(size=256):
    """A fairy ring seen from above: 24 mushroom caps in a circle joined by a thread of mycelium, an inner ring of tiny caps, spore dots
    and a small bloom at the centre. Lies on the ground (a plane); grey with alpha (drawn solid, then again additively)."""
    rng = np.random.default_rng(77)
    S = size * 2
    cv = Canvas(S, S)
    c = S / 2

    def place(rpx, cx, cy):
        lum, a = cap_tile(rpx)
        cv.over(np.dstack([lum] * 3), a, int(round(cx - lum.shape[1] / 2)), int(round(cy - lum.shape[0] / 2)))

    th_im = Image.new("L", (S, S), 0)
    dd = ImageDraw.Draw(th_im)
    for rr_, w in ((0.80, 2.4), (0.93, 1.2), (0.50, 1.1)):
        dd.ellipse([c - rr_ * c, c - rr_ * c, c + rr_ * c, c + rr_ * c], outline=255, width=int(w * 2))
    for k in range(96):                                                  # tick marks on the outer thin ring
        th = TAU * k / 96
        r0, r1 = 0.93 * c, (0.955 if k % 4 else 0.975) * c
        dd.line([(c + math.cos(th) * r0, c + math.sin(th) * r0), (c + math.cos(th) * r1, c + math.sin(th) * r1)], fill=255, width=2)
    thread = np.asarray(th_im, np.float32) / 255
    cv.over(np.full(3, 235.0), thread * 0.8)
    n_big = 24
    for k in range(n_big):
        th = TAU * k / n_big + rng.normal(0, 0.012)
        rr = c * 0.80
        rad = c * (0.062 + 0.026 * ((k * 5) % 3) / 2)
        place(rad, c + math.cos(th) * rr, c + math.sin(th) * rr)
        th2 = th + TAU / n_big / 2                                       # a small cap in each gap
        place(c * 0.028, c + math.cos(th2) * rr * 1.012, c + math.sin(th2) * rr * 1.012)
    for k in range(14):
        th = TAU * k / 14 + 0.2
        place(c * 0.040, c + math.cos(th) * c * 0.50, c + math.sin(th) * c * 0.50)
    for k in range(5):                                                   # the bloom at the heart
        th = TAU * k / 5 + 0.4
        place(c * 0.05, c + math.cos(th) * c * 0.075, c + math.sin(th) * c * 0.075)
    place(c * 0.06, c, c)
    spark = np.zeros((S, S), np.float32)
    for _ in range(120):                                                 # spore dots scattered between the rings
        th = rng.uniform(0, TAU)
        rr = c * rng.choice([rng.uniform(0.62, 0.74), rng.uniform(0.86, 0.91)])
        dot(spark, c + math.cos(th) * rr, c + math.sin(th) * rr, rng.uniform(0.9, 1.8), rng.uniform(0.5, 1.0))
    cv.over(np.full(3, 255.0), np.clip(spark, 0, 1))
    im = cv.image(2)
    arr = np.asarray(im, np.float32)
    xx, yy = grid(size, size)
    r = np.sqrt((xx - (size - 1) / 2) ** 2 + (yy - (size - 1) / 2) ** 2) / ((size - 1) / 2)
    arr[..., 3] *= smooth(1.0, 0.965, r)
    save(Image.fromarray(np.rint(arr).astype(np.uint8), "RGBA"), "fungus_ring")


# ------------------------------------------------------------------------------------------------ bursts
def shock(size=256):
    """A shock ring of spore dust: a lumpy bright front, a cloud pushed outward in front of it, streaks trailing behind. White."""
    rng = np.random.default_rng(88)
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    n = 0.5 * circ_noise(th, 13, 5) + 0.3 * circ_noise(th, 37, 6) + 0.2 * circ_noise(th, 91, 7)
    rr = 0.78 + (n - 0.5) * 0.12
    band = np.exp(-((r - rr) / (0.040 + 0.022 * n)) ** 2)
    out = np.exp(-((r - rr - 0.07) / 0.10) ** 2) * (0.28 + 0.55 * n)
    inn = np.exp(-((r - rr + 0.11) / 0.20) ** 2) * 0.17
    streak = (np.sin(th * 58 + n * 9) * 0.5 + 0.5) ** 3 * np.exp(-((r - rr + 0.08) / 0.10) ** 2) * 0.38
    puffy = fbm(size, size, 18, 91, 3)
    a = np.clip(band * 1.0 + (out + inn) * (0.65 + 0.7 * puffy) + streak, 0, 1)
    spec = np.zeros((size, size), np.float32)
    for _ in range(160):
        ang = rng.uniform(0, TAU)
        rad = c * float(np.clip(0.78 + rng.normal(0.02, 0.10), 0.3, 0.97))
        dot(spec, c + math.cos(ang) * rad, c + math.sin(ang) * rad, rng.uniform(0.7, 1.4), rng.uniform(0.5, 1.0))
    a = np.clip(a + spec * 0.7, 0, 1) * smooth(1.0, 0.94, r)
    save(rgba_img(255 * (0.82 + 0.18 * band), a), "fungus_shock")


def flare(size=256):
    """The flash of a spore burst: a hot core, a wide halo, 8 long and 8 short tapering rays and a faint ring of gill ticks. White."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    core = np.exp(-(r / 0.085) ** 2) + 0.45 * np.exp(-(r / 0.22) ** 2) + 0.16 * np.exp(-(r / 0.55) ** 2)
    rays = np.zeros_like(r)
    for k in range(16):
        a0 = TAU * k / 16 + (0.0 if k % 2 == 0 else 0.0)
        length = 0.97 if k % 2 == 0 else 0.55
        wid = 0.020 if k % 2 == 0 else 0.026
        da = np.abs(((th - a0 + math.pi) % TAU) - math.pi)
        across = np.exp(-((da * np.maximum(r, 0.02)) / (wid * (1.0 - 0.6 * np.clip(r / length, 0, 1)) + 0.004)) ** 2)
        rays = np.maximum(rays, across * smooth(length, length * 0.15, r) * (0.95 if k % 2 == 0 else 0.7))
    ticks = np.zeros_like(r)
    for k in range(48):
        a0 = TAU * k / 48
        da = np.abs(((th - a0 + math.pi) % TAU) - math.pi)
        ticks = np.maximum(ticks, np.exp(-((da * np.maximum(r, 0.02)) / 0.008) ** 2) * np.exp(-((r - 0.44) / 0.035) ** 2) * 0.5)
    ring_ = np.exp(-((r - 0.33) / 0.012) ** 2) * 0.3
    a = np.clip(core + rays + ticks + ring_, 0, 1) * smooth(1.0, 0.90, r)
    save(rgba_img(np.full((size, size), 255.0), a), "fungus_flare")


def puffball(size=128):
    """The spore pod that flies as the projectile: a warty puffball sphere lit from the upper left with a bright rim, a hot patch, and a halo of
    curling hyphae around it. Grey with alpha, drawn solid (tinted) under an additive glow."""
    rng = np.random.default_rng(31)
    S = size * SS
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    R0 = 0.47
    rn = r / R0
    inside = np.clip((1 - rn) * R0 * c, 0, 1)
    z = np.sqrt(np.clip(1 - rn ** 2, 0, 1))
    bump = fbm(size, size, 5, 33, 3, tile=False) - 0.5
    nx, ny = x / R0 + bump * 0.5, y / R0 + (fbm(size, size, 5, 34, 3, tile=False) - 0.5) * 0.5
    lam = np.clip(-0.42 * nx - 0.50 * ny + 0.76 * z, 0, 1)
    body = 0.32 + 0.70 * lam
    # warty spines: a lattice of tiny cones, foreshortened towards the limb
    lon = np.arctan2(x / R0, np.maximum(z, 1e-3))
    lat = np.arcsin(np.clip(y / R0, -1, 1))
    cell = 0.21
    fl, fa = (lon / cell) % 1.0 - 0.5, (lat / cell) % 1.0 - 0.5
    wart = np.exp(-((fl ** 2 + fa ** 2) / 0.05)) * z
    body = body + 0.12 * wart * lam
    rim = np.exp(-((rn - 0.93) / 0.12) ** 2) * 0.55
    hot = np.exp(-(((x + 0.14) / 0.18) ** 2 + ((y + 0.17) / 0.14) ** 2)) * 0.7
    lum = np.clip((body + rim * 0.5 + hot) * 235, 0, 255)
    fil = Image.new("L", (S, S), 0)
    df = ImageDraw.Draw(fil)
    cc = S / 2
    for k in range(34):                                   # the halo of hyphae
        a0 = TAU * k / 34 + rng.normal(0, 0.05)
        r0 = 0.44 * cc
        r1 = cc * rng.uniform(0.66, 0.97)
        curl = rng.normal(0, 0.45)
        pts = []
        for i in range(13):
            t = i / 12
            rr = r0 + (r1 - r0) * t
            th = a0 + curl * t * t
            pts.append((cc + math.cos(th) * rr, cc + math.sin(th) * rr))
        for i in range(12):
            df.line([pts[i], pts[i + 1]], fill=int(255 * (1 - 0.8 * i / 12)), width=max(1, int(round(SS * (1.5 - i / 12)))))
        px, py = pts[-1]
        df.ellipse([px - SS * 0.9, py - SS * 0.9, px + SS * 0.9, py + SS * 0.9], fill=210)
    fil_a = mask_down(fil, size)
    halo = np.exp(-(r / 0.62) ** 2) * 0.28
    a = np.clip(inside * 0.97 + fil_a * 0.85 * (1 - inside) + halo * (1 - inside), 0, 1) * smooth(1.0, 0.9, r)
    rgb = np.where(inside[..., None] > 0.05, np.dstack([lum] * 3), np.full((size, size, 3), 235.0))
    save(rgba_img(rgb, a), "fungus_puffball")


# ------------------------------------------------------------------------------------------------ strands, trails and haze
def hypha(w=64, h=256):
    """A braided strand of mycelium running along V (the long side): two intertwined cords with knots and fine side hairs, a soft glow.
    Tiles along V. White."""
    S = (w * SS, h * SS)
    im = Image.new("L", S, 0)
    d = ImageDraw.Draw(im)
    rng = np.random.default_rng(66)
    ys = np.arange(0, S[1] + 1, 2)
    for ph, wd in ((0.0, 1.0), (math.pi, 0.9)):
        v = ys / S[1]
        cx = S[0] * (0.5 + 0.17 * np.sin(TAU * v * 3 + ph) + 0.035 * np.sin(TAU * v * 8 + 1.7 + ph))
        width = S[0] * 0.034 * wd * (1 + 0.55 * np.maximum(0, np.sin(TAU * v * 9 + ph * 0.5)) ** 3)
        for i in range(len(ys) - 1):
            wi = max(1, int(round(width[i])))
            d.line([(cx[i], ys[i]), (cx[i + 1], ys[i + 1])], fill=255 if wd == 1.0 else 225, width=wi)
        for i in range(0, len(ys), 1):                            # knots
            if width[i] > S[0] * 0.034 * wd * 1.45:
                d.ellipse([cx[i] - width[i] * 0.6, ys[i] - width[i] * 0.6, cx[i] + width[i] * 0.6, ys[i] + width[i] * 0.6], fill=255)
        for k in range(26):                                       # side hairs
            yi = rng.integers(0, len(ys) - 1)
            side = 1 if rng.random() < 0.5 else -1
            ang = math.pi / 2 * side + rng.normal(0, 0.5) - side * 0.5
            ln = S[0] * rng.uniform(0.10, 0.26)
            for off in (0, -S[1], S[1]):
                d.line([(cx[yi], ys[yi] + off), (cx[yi] + math.cos(ang) * ln, ys[yi] + off + math.sin(ang) * ln)], fill=160, width=max(1, SS // 2))
    core = mask_down(im, (w, h))
    glow = gblur(core, 1.8) * 0.65 + gblur(core, 4.5) * 0.35
    a = np.clip(core + glow * 0.8, 0, 1)
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    a = a * smooth(1.0, 0.82, np.abs(u))
    save(rgba_img(255 * (0.78 + 0.22 * core), a), "fungus_hypha")


def trail(w=64, h=256):
    """A stream of spore smoke running along V: lumps of different widths drifting in a soft, bright-cored ribbon with suspended specks.
    Tiles along V. White."""
    rng = np.random.default_rng(55)
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    n1 = value_noise(w, h, (w * 0.8, h / 5), 51, True)
    n2 = value_noise(w, h, (w * 0.4, h / 11), 52, True)
    n3 = value_noise(w, h, (w * 0.2, h / 26), 53, True)
    prof = 0.50 + 0.38 * (n1 - 0.5) * 2
    swerve = 0.18 * (value_noise(w, h, (w, h / 3), 54, True) - 0.5) * 2
    d = np.abs(u - swerve) / np.maximum(prof, 0.15)
    body = np.clip(1 - d ** 2, 0, 1) ** 1.4
    billow = 0.55 + 0.9 * (0.5 * n2 + 0.5 * n3)
    a = np.clip(body * billow, 0, 1)
    spec = np.zeros((h, w), np.float32)
    for _ in range(46):
        px, py = rng.uniform(0.15, 0.85) * w, rng.uniform(0, 1) * h
        if a[int(py) % h, int(px)] > 0.25:
            dot(spec, px, py, rng.uniform(0.6, 1.1), rng.uniform(0.5, 1.0))
    a = np.clip(a + spec * 0.6, 0, 1) * smooth(1.0, 0.8, np.abs(u))
    shade = 0.72 + 0.28 * np.clip(body, 0, 1)
    save(rgba_img(255 * np.clip(shade + spec * 0.3, 0, 1), a), "fungus_trail")


def haze(w=256, h=64):
    """Tileable band of spore haze for the wall of a field: dense and lumpy at the foot, thinning to wisps at the top (V = 1 is the ground).
    Tiles along U. White."""
    rng = np.random.default_rng(99)
    xx, yy = grid(w, h)
    v = 1 - yy / (h - 1)                                       # 0 at the foot, 1 at the top
    n1 = fbm(w, h, 48, 101, 4)
    n2 = fbm(w, h, 20, 102, 3)
    top = 0.34 + 0.55 * n1 + 0.10 * n2                         # ragged upper edge
    dens = np.clip((top - v) / 0.28, 0, 1) ** 1.2
    foot = smooth(0.0, 0.10, v)                                # nothing is cut off hard at the ground
    a = dens * (0.55 + 0.75 * n2) * (0.85 + 0.15 * foot)
    a = np.clip(a, 0, 1) * np.clip(1.0 - np.maximum(0, 0.06 - v) / 0.06, 0, 1) ** 0.5 * smooth(0.0, 0.05, 1 - v)
    spec = np.zeros((h, w), np.float32)
    for _ in range(90):
        px, py = rng.uniform(0, 1) * w, rng.uniform(0, 1) * h
        if a[int(py), int(px)] > 0.3:
            sg, am = rng.uniform(0.6, 1.2), rng.uniform(0.4, 1.0)
            for off in (-w, 0, w):                                # wrap round so the band still tiles
                dot(spec, px + off, py, sg, am)
    a = np.clip(a * 0.9 + spec * 0.45, 0, 1)
    shade = np.clip(0.60 + 0.40 * (1 - np.clip(a, 0, 1) * 0.5) + (n2 - 0.5) * 0.3 + v * 0.1, 0, 1)
    save(rgba_img(255 * shade, a), "fungus_haze")


MAKERS = {
    "puff": lambda: (puff("fungus_puff", 256, 1), puff("fungus_puff2", 256, 7, 0.72, 0.7, 0.9)),
    "spores": spores,
    "mycelium": mycelium,
    "gills": gills,
    "ring": ring,
    "shock": shock,
    "flare": flare,
    "puffball": puffball,
    "hypha": hypha,
    "trail": trail,
    "haze": haze,
}


if __name__ == "__main__":
    names = sys.argv[1:] or list(MAKERS)
    for nm in names:
        MAKERS[nm]()

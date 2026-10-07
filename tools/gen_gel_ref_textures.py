"""Generates the Gel Magic VFX textures (Sally's gel salamander, glossy cel-shaded violet gel).

Deterministic (fixed seeds, no fonts). Output: src/main/resources/assets/nusmp/textures/particle/gel_cel_*.png

    python3 -B tools/gen_gel_ref_textures.py

Look reference (owner still, pack_gel): a translucent violet-grey gel mass with a dark ragged outline, lighter swirl blotches
inside, bright white glossy streaks / ovals as highlights, a curling salamander tail and clawed limbs; big white bars with a
lavender edge radiate from it. Textures are near-white with alpha so the vertex colour tints them; the dark outline and the
white gloss are baked into the luminance.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def noise(w, h, cells, seed):
    """Smooth value noise in 0..1 (bicubic upsample of a random lattice, wraps nothing)."""
    rng = np.random.RandomState(seed)
    cw, ch = max(2, int(cells * w / max(w, h))), max(2, int(cells * h / max(w, h)))
    g = (rng.rand(ch + 1, cw + 1) * 255).astype(np.uint8)
    im = Image.fromarray(g, "L").resize((w, h), Image.BICUBIC)
    return np.asarray(im, dtype=np.float32) / 255.0


def fbm(w, h, base, seed, octaves=4):
    out, amp, tot = np.zeros((h, w), np.float32), 1.0, 0.0
    for o in range(octaves):
        out += amp * noise(w, h, base * (2 ** o), seed + o * 17)
        tot += amp
        amp *= 0.5
    return out / tot


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, dtype=np.float32) / 255.0


def save(name, lum, alpha):
    lum = np.clip(lum, 0, 1)
    alpha = np.clip(alpha, 0, 1)
    rgba = np.zeros(lum.shape + (4,), np.uint8)
    rgba[..., 0] = rgba[..., 1] = rgba[..., 2] = (lum * 255).astype(np.uint8)
    rgba[..., 3] = (alpha * 255).astype(np.uint8)
    Image.fromarray(rgba, "RGBA").save(os.path.join(OUT, "gel_cel_" + name + ".png"))
    print("wrote gel_cel_"+name, lum.shape[::-1])


def mask_img(size, fn):
    """Draw with PIL at SS x supersampling; fn(draw, S) draws white on black. Returns float array."""
    w, h = size
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    return np.asarray(im, dtype=np.float32) / 255.0


def comp(lum, alpha, layer_l, layer_a):
    """Paint (layer_l, layer_a) over (lum, alpha)."""
    a = layer_a + alpha * (1 - layer_a)
    l = np.where(a > 1e-4, (layer_l * layer_a + lum * alpha * (1 - layer_a)) / np.maximum(a, 1e-4), 0)
    return l, a


# ------------------------------------------------------------------------------------------------ gel_blob
def gel_blob():
    n = 256
    x, y = grid(n, n)
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    wob = 1 + 0.045 * np.sin(ang * 3 + 0.7) + 0.03 * np.sin(ang * 5 + 2.1) + 0.02 * np.sin(ang * 9)
    d = r / (0.9 * wob)
    inside = 1 - smooth(0.93, 1.0, d)
    # translucent body: thicker toward the rim, thin in the middle
    alpha = inside * (0.52 + 0.3 * smooth(0.2, 0.95, d))
    wx = fbm(n, n, 3, 11) * 2 - 1
    wy = fbm(n, n, 3, 23) * 2 - 1
    sw = fbm(n, n, 4, 5)
    sw = np.sin((sw * 5.0 + wx * 1.5 + wy * 1.2 + r * 3.0) * 3.1)
    patch = smooth(0.35, 0.55, sw) * (1 - smooth(0.55, 0.85, d))
    lum = 0.68 + 0.05 * (fbm(n, n, 6, 3) - 0.5) + 0.3 * patch
    alpha = np.maximum(alpha, inside * patch * 0.9)
    # dark cel outline
    line = smooth(0.84, 0.93, d) * (1 - smooth(0.965, 1.0, d))
    lum = lum * (1 - line) + 0.26 * line
    alpha = np.maximum(alpha, line * 0.97)

    # gloss: crescent on the upper left + small oval
    def draw(dr, S):
        box = [int(n * 0.16 * S), int(n * 0.16 * S), int(n * 0.84 * S), int(n * 0.84 * S)]
        dr.arc(box, 188, 262, fill=255, width=int(8 * S))
        dr.ellipse([int(n * 0.33 * S), int(n * 0.13 * S), int(n * 0.38 * S), int(n * 0.17 * S)], fill=255)
    g = blur(mask_img((n, n), draw), 0.8)
    lum, alpha = comp(lum, alpha, np.ones_like(lum), g * 0.98)
    save("blob", lum, alpha)


# ------------------------------------------------------------------------------------------------ gel_swirl
def gel_swirl():
    n = 128
    x, y = grid(n, n)
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    nz = fbm(n, n, 3, 41)
    band = np.sin(ang * 2 + r * 9 - nz * 3.0)
    arms = smooth(0.1, 0.5, band) * (1 - smooth(0.55, 0.95, r))
    arms = blur(arms, 1.2)
    edge = smooth(0.25, 0.55, arms) * (1 - smooth(0.6, 0.85, arms))
    lum = 0.85 + 0.15 * edge
    save("swirl", lum, arms * 0.85)


# ------------------------------------------------------------------------------------------------ gel_gloss
def gel_gloss():
    n = 128

    def draw(dr, S):
        # long curved streak, tapered, plus an oval like the anime highlight
        pts_o, pts_i = [], []
        for i in range(41):
            t = i / 40
            a = math.radians(200 + 95 * t)
            w = math.sin(t * math.pi) ** 0.8 * 0.085 + 0.004
            for rr, pts in ((0.62 + w, pts_o), (0.62 - w, pts_i)):
                pts.append(((0.5 + math.cos(a) * rr * 0.7) * n * S, (0.62 + math.sin(a) * rr * 0.7) * n * S))
        dr.polygon(pts_o + pts_i[::-1], fill=255)
        dr.ellipse([0.2 * n * S, 0.66 * n * S, 0.285 * n * S, 0.77 * n * S], fill=255)
        dr.ellipse([0.32 * n * S, 0.78 * n * S, 0.355 * n * S, 0.82 * n * S], fill=255)
    m = mask_img((n, n), draw)
    save("gloss", np.ones_like(m), np.clip(m + blur(m, 3) * 0.35, 0, 1))


# ------------------------------------------------------------------------------------------------ gel_drop
def gel_drop():
    n = 64

    def body(dr, S):
        pts = []
        for i in range(60):
            t = i / 59
            yy = 0.08 + 0.84 * t
            wdt = 0.36 * math.sin(math.pi * t ** 1.8) ** 0.9
            pts.append(((0.5 + wdt) * n * S, yy * n * S))
        pts2 = [((1 - px / (n * S)) * n * S, py) for px, py in pts[::-1]]
        dr.polygon(pts + pts2, fill=255)
    m = mask_img((n, n), body)
    core = blur(m, 1.0)
    inner = smooth(0.4, 0.95, blur(m, 2.5))
    lum = 0.7 + 0.2 * inner
    alpha = m * (0.6 + 0.3 * (1 - inner))
    edge = smooth(0.2, 0.6, m) * (1 - smooth(0.6, 0.95, blur(m, 2.0)))
    lum = lum * (1 - edge * 0.7) + 0.26 * edge * 0.7
    alpha = np.maximum(alpha, edge * 0.95)

    def hl(dr, S):
        dr.ellipse([0.36 * n * S, 0.52 * n * S, 0.44 * n * S, 0.74 * n * S], fill=255)
    h = blur(mask_img((n, n), hl), 0.5)
    lum, alpha = comp(lum, alpha, np.ones_like(lum), h)
    save("drop", lum, alpha * (core > 0.01))


# ------------------------------------------------------------------------------------------------ gel_strand (64 x 256, V runs along)
def gel_strand():
    w, h = 64, 256
    x, y = grid(w, h)
    v = (y + 1) / 2
    wid = 0.34 + 0.12 * np.sin(v * 6.28318 * 3) + 0.05 * np.sin(v * 6.28318 * 7 + 1.0)
    # beads: pinch between blobs
    d = np.abs(x + 0.04 * np.sin(v * 6.28318 * 2)) / wid
    inside = 1 - smooth(0.9, 1.0, d)
    nz = fbm(w, h, 4, 71)
    lum = 0.72 + 0.12 * nz
    alpha = inside * (0.55 + 0.3 * smooth(0.3, 0.95, d))
    line = smooth(0.78, 0.9, d) * (1 - smooth(0.95, 1.0, d))
    lum = lum * (1 - line) + 0.28 * line
    alpha = np.maximum(alpha, line * 0.95)
    # gloss stripe along the left edge
    gl = smooth(0.12, 0.2, -(x + 0.04 * np.sin(v * 6.28318 * 2)) / wid - 0.30) * (1 - smooth(0.46, 0.56, -(x + 0.04 * np.sin(v * 6.28318 * 2)) / wid))
    gl = gl * (0.55 + 0.45 * np.sin(v * 6.28318 * 3 + 0.8) ** 2) * inside
    lum, alpha = comp(lum, alpha, np.ones_like(lum), gl * 0.95)
    save("strand", lum, alpha)


# ------------------------------------------------------------------------------------------------ gel_ring (256 x 64 band)
def gel_ring():
    w, h = 256, 64
    x, y = grid(w, h)
    u = (x + 1) / 2
    v = (y + 1) / 2  # 0 inner edge .. 1 outer edge
    # drip scallops along the outer edge, tiling with period 1/8
    scal = 0.78 + 0.16 * np.abs(np.sin(u * math.pi * 8)) ** 0.7 + 0.04 * np.sin(u * math.pi * 26)
    inner_e = 0.1 + 0.03 * np.sin(u * math.pi * 16 + 1.0)
    inside = smooth(inner_e - 0.02, inner_e + 0.03, v) * (1 - smooth(scal - 0.04, scal, v))
    nz = fbm(w, h, 8, 91)
    body = 0.7 + 0.15 * nz
    alpha = inside * 0.78
    # outline on both edges
    e0 = smooth(inner_e - 0.02, inner_e + 0.02, v) * (1 - smooth(inner_e + 0.04, inner_e + 0.09, v))
    e1 = smooth(scal - 0.1, scal - 0.05, v) * (1 - smooth(scal - 0.04, scal, v))
    ln = np.clip(e0 + e1, 0, 1)
    lum = body * (1 - ln) + 0.27 * ln
    alpha = np.maximum(alpha, ln * 0.95)
    # beads of gloss
    bead = np.abs(np.sin(u * math.pi * 8 + 0.5))
    gl = (bead ** 7) * np.exp(-((v - 0.4) / 0.09) ** 2) * inside
    lum, alpha = comp(lum, alpha, np.ones_like(lum), gl * 0.95)
    save("ring", lum, alpha)


# ------------------------------------------------------------------------------------------------ gel_bar (64 x 256)
def gel_bar():
    w, h = 64, 256
    x, y = grid(w, h)
    v = (y + 1) / 2
    taper = 0.55 + 0.45 * smooth(0.0, 0.25, v) * (1 - 0.35 * smooth(0.7, 1.0, v))
    d = np.abs(x) / taper
    cap = 1 - smooth(0.88, 1.0, v)
    core = (1 - smooth(0.25, 0.75, d)) * cap
    halo = (1 - smooth(0.5, 1.0, d)) * cap
    lum = 0.78 + 0.22 * core
    alpha = halo * 0.55 + core * 0.45 + smooth(0.55, 0.7, d) * (1 - smooth(0.75, 0.9, d)) * 0.25 * cap
    save("bar", lum, alpha)


# ------------------------------------------------------------------------------------------------ gel_splat (256)
def gel_splat():
    n = 256
    x, y = grid(n, n)
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    spikes = 0.34 + 0.30 * np.abs(np.sin(ang * 6.5 + 0.4)) ** 2.2 + 0.1 * np.abs(np.sin(ang * 11 + 1.3)) ** 3
    body = 1 - smooth(spikes - 0.05, spikes, r)
    nz = fbm(n, n, 5, 61)
    lum = 0.7 + 0.15 * nz
    alpha = body * 0.75
    ln = smooth(spikes - 0.1, spikes - 0.05, r) * (1 - smooth(spikes - 0.03, spikes, r))
    lum = lum * (1 - ln) + 0.27 * ln
    alpha = np.maximum(alpha, ln * 0.95)
    rng = np.random.RandomState(7)

    def draw(dr, S):
        for i in range(14):
            a = rng.rand() * math.tau
            d = 0.62 + rng.rand() * 0.3
            rad = 0.015 + rng.rand() * 0.032
            cx, cy = (0.5 + math.cos(a) * d * 0.5) * n * S, (0.5 + math.sin(a) * d * 0.5) * n * S
            dr.ellipse([cx - rad * n * S, cy - rad * n * S, cx + rad * n * S, cy + rad * n * S], fill=255)
    dots = mask_img((n, n), draw)
    lum, alpha = comp(lum, alpha, np.full_like(lum, 0.75), dots * 0.9)

    def hl(dr, S):
        dr.arc([int(n * 0.28 * S), int(n * 0.28 * S), int(n * 0.72 * S), int(n * 0.72 * S)], 200, 255, fill=255, width=int(6 * S))
    g = blur(mask_img((n, n), hl), 0.7)
    lum, alpha = comp(lum, alpha, np.ones_like(lum), g * 0.95)
    save("splat", lum, alpha)


# ------------------------------------------------------------------------------------------------ gel_bubble (64)
def gel_bubble():
    n = 64
    x, y = grid(n, n)
    r = np.sqrt(x * x + y * y)
    rim = smooth(0.62, 0.9, r) * (1 - smooth(0.9, 0.97, r))
    inner = (1 - smooth(0.85, 0.95, r)) * 0.12
    ln = smooth(0.88, 0.93, r) * (1 - smooth(0.95, 0.99, r))
    lum = 0.9 * (1 - ln) + 0.3 * ln
    alpha = np.maximum(inner + rim * 0.55, ln * 0.9)

    def hl(dr, S):
        dr.ellipse([0.2 * n * S, 0.2 * n * S, 0.4 * n * S, 0.32 * n * S], fill=255)
        dr.ellipse([0.62 * n * S, 0.66 * n * S, 0.7 * n * S, 0.72 * n * S], fill=255)
    h = blur(mask_img((n, n), hl), 0.5)
    lum, alpha = comp(lum, alpha, np.ones_like(lum), h)
    save("bubble", lum, alpha)


# ------------------------------------------------------------------------------------------------ gel_salamander (256)
def gel_salamander():
    """A curled salamander seen from above: spiral spine, four clawed legs, a round head with two eyes (the ground sigil)."""
    n = 256

    def spine(t):
        # spiral from the head (t=0, outside) in to the tail tip (t=1)
        a = -0.6 + t * 8.2
        rr = 0.78 - 0.60 * t
        return 0.5 + math.cos(a) * rr * 0.5, 0.5 + math.sin(a) * rr * 0.5, a

    def draw(dr, S):
        N = 220
        for i in range(N):
            t = i / (N - 1)
            cx, cy, a = spine(t)
            wdt = (0.062 * (0.35 + 0.65 * math.sin(min(1, t * 1.6 + 0.12) * math.pi * 0.5)) * (1 - 0.82 * t ** 1.5) + 0.006)
            if t < 0.07:
                wdt = 0.058 + 0.018 * (1 - t / 0.07)
            rad = wdt
            dr.ellipse([(cx - rad) * n * S, (cy - rad) * n * S, (cx + rad) * n * S, (cy + rad) * n * S], fill=255)
        for t0, side in ((0.2, 1), (0.2, -1), (0.46, 1), (0.46, -1)):
            cx, cy, a = spine(t0)
            nx, ny = math.cos(a), math.sin(a)  # radial direction at that spot
            tx, ty = -math.sin(a), math.cos(a)
            kx, ky = cx + side * 0.0, cy
            # leg out along the radial normal, toes spread
            for j in range(14):
                f = j / 13
                px = cx + nx * (0.05 + 0.07 * f) * (1 if side > 0 else -1) + tx * 0.02 * f * side
                py = cy + ny * (0.05 + 0.07 * f) * (1 if side > 0 else -1) + ty * 0.02 * f * side
                rr = 0.021 * (1 - 0.35 * f)
                dr.ellipse([(px - rr) * n * S, (py - rr) * n * S, (px + rr) * n * S, (py + rr) * n * S], fill=255)
            ex = cx + nx * 0.125 * (1 if side > 0 else -1)
            ey = cy + ny * 0.125 * (1 if side > 0 else -1)
            for k in (-1, 0, 1):
                px, py = ex + tx * 0.026 * k, ey + ty * 0.026 * k
                rr = 0.011
                dr.ellipse([(px - rr) * n * S, (py - rr) * n * S, (px + rr) * n * S, (py + rr) * n * S], fill=255)
    m = mask_img((n, n), draw)
    soft = blur(m, 1.0)
    body = smooth(0.4, 0.7, soft)
    inner = blur(m, 3.5)
    ln = body * (1 - smooth(0.62, 0.82, inner))
    lum = 0.95 * (1 - ln) + 0.5 * ln
    alpha = np.clip(body * 0.55 + ln * 0.7, 0, 1)
    # eyes on the head
    cx, cy, a = spine(0.02)

    def eyes(dr, S):
        for k in (-1, 1):
            px = cx + (-math.sin(a)) * 0.026 * k
            py = cy + math.cos(a) * 0.026 * k
            dr.ellipse([(px - 0.011) * n * S, (py - 0.011) * n * S, (px + 0.011) * n * S, (py + 0.011) * n * S], fill=255)
    e = blur(mask_img((n, n), eyes), 0.4)
    lum = lum * (1 - e) + 0.15 * e
    alpha = np.maximum(alpha, e)
    save("salamander", lum, alpha)


# ------------------------------------------------------------------------------------------------ gel_ripple (128)
def gel_ripple():
    n = 128
    x, y = grid(n, n)
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    rr = r * (1 + 0.03 * np.sin(ang * 5 + 1) + 0.02 * np.sin(ang * 8))
    ring = smooth(0.78, 0.9, rr) * (1 - smooth(0.9, 0.97, rr))
    ring2 = smooth(0.5, 0.58, rr) * (1 - smooth(0.58, 0.64, rr)) * 0.5
    fill = (1 - smooth(0.9, 0.97, rr)) * 0.14
    save("ripple", np.full((n, n), 0.95, np.float32), np.clip(ring * 0.9 + ring2 + fill, 0, 1))


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    gel_blob(); gel_swirl(); gel_gloss(); gel_drop(); gel_strand(); gel_ring(); gel_bar(); gel_splat(); gel_bubble(); gel_salamander(); gel_ripple()

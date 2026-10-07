"""Generates the Crystal Magic VFX textures (crystal_*.png in textures/particle).

Look reference (owner's anime still): a giant curved fang of pale lavender-white translucent crystal, flat cel shading in a few
facet bands, faint thin curved vein lines inside, a soft bright rim glow. Everything here is white / lavender-white with alpha so
the vertex colour tints it. Deterministic (fixed seeds), no fonts.

    python3 -B tools/gen_crystal_textures.py
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
LAV = np.array([236, 233, 255], dtype=np.float32)   # lavender white (the still)
ICE = np.array([205, 222, 255], dtype=np.float32)   # cool shadow facets


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def noise(h, w, cells, seed, octaves=4):
    """Tileable-ish value noise in 0..1 (bilinear upsample of random grids, summed)."""
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = cells * (2 ** o)
        g = rng.random((c + 1, c + 1)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += np.asarray(im, np.float32) / 255.0 * amp
        tot += amp
        amp *= 0.5
    return out / tot


def save(name, rgb, alpha):
    a = np.clip(alpha, 0, 1)
    img = np.dstack([np.clip(rgb, 0, 255), a * 255]).astype(np.uint8)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT, name))
    print("wrote", name, img.shape[1], "x", img.shape[0])


def mix(a, b, t):
    t = np.asarray(t)[..., None]
    return a * (1 - t) + b * t


# ------------------------------------------------------------------------------------------------ blade (the still)
def blade():
    W, H = 128, 256
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    t = 1 - (ys + 0.5) / H                     # 0 at the base (bottom), 1 at the tip (top)
    x = (xs + 0.5) / W
    centre = 0.42 + 0.40 * t ** 1.7            # the spine sweeps to the right as it rises
    wl = 0.40 * (1 - t) ** 0.62 + 0.012        # convex outer edge (left)
    wr = 0.30 * (1 - t) ** 0.85 + 0.012        # concave inner edge (right)
    left, right = centre - wl, centre + wr
    s = (x - left) / np.maximum(right - left, 1e-4)
    inside = (s > 0) & (s < 1)
    edge = np.minimum(s, 1 - s) * (right - left) * W   # pixels to the nearest edge
    cover = smooth(0.0, 1.6, edge) * (t < 0.998)
    # flat facet bands like cel shading, softened by a little low-frequency noise
    n = noise(H, W, 3, 11, 3)
    band = np.where(s < 0.22, 0.80, np.where(s < 0.58, 1.0, 0.90)) + (n - 0.5) * 0.10
    band = np.where(np.abs(s - 0.22) < 0.012, 1.1, band)
    band = np.where(np.abs(s - 0.58) < 0.010, 1.1, band)
    # faint curved veins crossing the body
    veins = np.zeros((H, W), np.float32)
    for k, (ph, fr) in enumerate([(0.2, 7.0), (0.55, 5.0), (0.9, 9.0)]):
        curve = np.sin(t * fr + ph * 6.0 + s * 2.2) * 0.5 + 0.5
        veins = np.maximum(veins, smooth(0.035, 0.0, np.abs(curve - 0.5)) * (0.5 + 0.5 * np.sin(t * 20 + k)))
    rim = smooth(5.0, 0.0, edge)               # soft inner rim glow
    rgb = mix(ICE, LAV, np.clip(band - 0.55, 0, 1) * 1.6) * np.clip(band, 0.7, 1.1)[..., None]
    rgb = mix(rgb, np.array([255, 255, 255], np.float32), np.clip(rim * 0.75 + veins * 0.35, 0, 1))
    alpha = cover * (0.62 + 0.30 * band + 0.25 * rim) * smooth(0.0, 0.07, t)   # base melts away softly
    # tip glint
    glint = np.exp(-(((x - 0.82) / 0.03) ** 2 + ((t - 0.965) / 0.03) ** 2))
    rgb = mix(rgb, np.array([255, 255, 255], np.float32), np.clip(glint, 0, 1))
    save("crystal_blade.png", rgb, np.clip(alpha, 0, 1) * inside)


# ------------------------------------------------------------------------------------------------ shard / lance
def shard():
    W, H = 64, 128
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    v = (ys + 0.5) / H                          # 0 tip (top), 1 tail
    x = ((xs + 0.5) / W - 0.5) * 2              # -1..1
    hw = np.where(v < 0.32, (v / 0.32) ** 0.9, 1.0 - 0.55 * ((v - 0.32) / 0.68)) * 0.78 + 0.005
    s = x / np.maximum(hw, 1e-3)
    cover = smooth(1.0, 0.82, np.abs(s))
    cover *= smooth(0.0, 0.01, v)
    # hex prism: three vertical facets (left dark, middle bright, right mid) split at +-0.38
    shade = np.where(s < -0.38, 0.72, np.where(s < 0.38, 1.0, 0.86))
    ridge = smooth(0.06, 0.0, np.abs(np.abs(s) - 0.38)) * 0.5 + smooth(0.07, 0.0, np.abs(s)) * 0.0
    rgb = mix(ICE, LAV, np.clip(shade - 0.5, 0, 1) * 2) * shade[..., None]
    rgb = mix(rgb, np.array([255, 255, 255], np.float32), np.clip(ridge + smooth(1.0, 0.6, np.abs(s)) * 0.0, 0, 1))
    # bright tip, tail fades into a streak
    tipglow = np.exp(-((v / 0.10) ** 2)) * 0.8
    rgb = mix(rgb, np.array([255, 255, 255], np.float32), np.clip(tipglow, 0, 1))
    alpha = cover * (0.88 - 0.55 * smooth(0.55, 1.0, v)) * smooth(1.0, 0.82, np.abs(s))
    save("crystal_shard.png", rgb, alpha)


# ------------------------------------------------------------------------------------------------ faceted flare
def flare():
    N = 128
    ys, xs = np.mgrid[0:N, 0:N].astype(np.float32)
    x, y = (xs + 0.5) / N * 2 - 1, (ys + 0.5) / N * 2 - 1
    r = np.sqrt(x * x + y * y) + 1e-4
    a = np.arctan2(y, x)
    ray8 = np.abs(np.cos(a * 4)) ** 28                       # eight sharp rays
    ray4 = np.abs(np.cos(a * 2 + 0.0)) ** 90 * 1.0           # long thin cross
    glow = np.exp(-(r / 0.16) ** 2)
    rays = (ray8 * 1.1 + ray4 * 1.3) * np.exp(-r * 1.8) * smooth(0.0, 0.04, r)
    # six-sided prism halo (faceted ring)
    hexd = np.max([np.abs(x * math.cos(k * math.pi / 3) + y * math.sin(k * math.pi / 3)) for k in range(3)], axis=0)
    halo = smooth(0.05, 0.0, np.abs(hexd - 0.62)) * 0.45 * smooth(1.0, 0.7, r)
    alpha = np.clip(glow * 1.2 + rays + halo, 0, 1) * smooth(1.0, 0.85, r)
    rgb = mix(np.array([200, 215, 255], np.float32), np.array([255, 255, 255], np.float32), np.clip(glow + rays * 0.6, 0, 1))
    save("crystal_flare.png", rgb, alpha)


# ------------------------------------------------------------------------------------------------ ground sigil
def sigil():
    N, SS = 256, 3
    M = N * SS
    img = Image.new("L", (M, M), 0)
    d = ImageDraw.Draw(img)
    c = M / 2

    def P(r, ang):
        return (c + r * M / 2 * math.cos(ang), c + r * M / 2 * math.sin(ang))

    def ring(r, w):
        d.ellipse([c - r * c, c - r * c, c + r * c, c + r * c], outline=255, width=int(w * SS))

    ring(0.97, 2.5)
    ring(0.90, 1.2)
    ring(0.60, 2.0)
    ring(0.30, 1.4)
    # twelve crystal spikes between the outer rings (points out), like a crown of facets
    for i in range(12):
        a = i * math.tau / 12
        d.polygon([P(0.90, a - 0.10), P(0.97, a - 0.0), P(0.90, a + 0.10)], fill=255)
        d.polygon([P(0.60, a - 0.12), P(0.80, a), P(0.60, a + 0.12)], outline=255, width=2 * SS)
        d.line([P(0.80, a), P(0.90, a)], fill=255, width=2 * SS)
    # two interlocked triangles = a six-point star with facet lines
    for off in (0.0, math.pi / 3):
        pts = [P(0.60, off + k * math.tau / 3 - math.pi / 2) for k in range(3)]
        d.polygon(pts, outline=255, width=3 * SS)
    # inner cut-gem: a hexagon with spokes
    hexpts = [P(0.30, k * math.tau / 6) for k in range(6)]
    d.polygon(hexpts, outline=255, width=2 * SS)
    for k in range(6):
        d.line([P(0.0, 0), hexpts[k]], fill=255, width=1 * SS)
        d.line([hexpts[k], hexpts[(k + 2) % 6]], fill=255, width=1 * SS)
    for k in range(6):
        d.line([P(0.30, k * math.tau / 6), P(0.60, k * math.tau / 6 + math.pi / 6)], fill=200, width=1 * SS)
    line = img.resize((N, N), Image.LANCZOS)
    glow = line.filter(ImageFilter.GaussianBlur(3.0))
    a = np.asarray(line, np.float32) / 255 * 0.95 + np.asarray(glow, np.float32) / 255 * 0.7
    ys, xs = np.mgrid[0:N, 0:N].astype(np.float32)
    r = np.sqrt(((xs + 0.5) / N * 2 - 1) ** 2 + ((ys + 0.5) / N * 2 - 1) ** 2)
    a = a * smooth(1.0, 0.94, r)
    # faint facet fill: a few translucent wedges so the floor reads as cut glass
    ang = np.arctan2((ys + 0.5) / N * 2 - 1, (xs + 0.5) / N * 2 - 1)
    wedge = (np.floor((ang + math.pi) / (math.pi / 6)) % 2) * 0.07 * smooth(0.95, 0.9, r) * smooth(0.28, 0.4, r)
    a = np.clip(a + wedge, 0, 1)
    rgb = mix(np.array([205, 215, 255], np.float32), np.array([255, 255, 255], np.float32), np.clip(np.asarray(line, np.float32) / 255, 0, 1))
    save("crystal_sigil.png", rgb, a)


# ------------------------------------------------------------------------------------------------ facet band (tiles in U)
def band():
    W, H = 256, 64
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    n = 8
    u = xs / W * n
    f = u % 1.0
    v = (ys + 0.5) / H
    # alternating up / down triangles forming a zigzag of crystal teeth
    tri = np.where(((u.astype(int)) % 2) == 0, f, 1 - f) * 2           # 0..2
    tri = np.where(tri > 1, 2 - tri, tri)
    up = ((u.astype(int) % 2) == 0)
    height = np.where(up, 0.28 + 0.62 * tri, 0.18 + 0.50 * tri)       # tooth top edge
    top = 1 - height
    cover = smooth(top - 0.01, top + 0.02, v) * smooth(1.0, 0.9, v)
    rng = np.random.default_rng(5)
    tone = rng.random(n + 1)
    shade = 0.78 + 0.22 * tone[np.floor(u).astype(int) % n]
    shade = shade * (0.92 + 0.08 * f)
    edge = smooth(0.05, 0.0, np.abs(v - top)) * 0.6 + smooth(0.05, 0.0, np.minimum(f, 1 - f)) * 0.5
    rgb = mix(ICE, LAV, shade - 0.4) * shade[..., None]
    rgb = mix(rgb, np.array([255, 255, 255], np.float32), np.clip(edge, 0, 1))
    alpha = cover * (0.45 + 0.4 * shade) * (0.4 + 0.6 * smooth(0.0, 0.6, v))
    save("crystal_band.png", rgb, np.clip(alpha, 0, 1))


# ------------------------------------------------------------------------------------------------ trail streak
def streak():
    W, H = 32, 128
    ys, xs = np.mgrid[0:H, 0:W].astype(np.float32)
    x = ((xs + 0.5) / W - 0.5) * 2
    v = (ys + 0.5) / H                        # bottom (v=1) is the head: bright
    h = 1 - v
    core = np.exp(-(x / (0.35 + 0.4 * v)) ** 2)
    lattice = 0.5 + 0.5 * np.cos(np.pi * (v * 9 + np.abs(x) * 3.0))      # diamond lattice
    n = noise(H, W, 4, 7, 3)
    a = core * (0.55 + 0.45 * lattice) * (h ** 0.2)
    a = a * smooth(0.0, 0.3, 1 - v * 0.0) * np.clip(1.15 - v * 1.1, 0, 1) * (0.7 + 0.3 * n)
    a = a * 1.6 + np.exp(-(x / 0.12) ** 2) * (1 - v) ** 1.2 * 0.7
    rgb = mix(np.array([190, 205, 255], np.float32), np.array([255, 255, 255], np.float32), np.clip(core * (1 - v), 0, 1))
    save("crystal_streak.png", rgb, a)


# ------------------------------------------------------------------------------------------------ chip (flying debris)
def chip():
    N, SS = 64, 4
    M = N * SS
    base = Image.new("RGBA", (M, M), (0, 0, 0, 0))
    d = ImageDraw.Draw(base)
    pts = [(0.50, 0.04), (0.84, 0.36), (0.70, 0.94), (0.30, 0.88), (0.14, 0.42)]
    P = [(x * M, y * M) for x, y in pts]
    centre = (0.5 * M, 0.5 * M)
    tones = [255, 200, 235, 170, 215]
    for i in range(5):
        tri = [P[i], P[(i + 1) % 5], centre]
        g = tones[i]
        d.polygon(tri, fill=(g, min(255, g + 8), 255, 235))
    d.line(P + [P[0]], fill=(255, 255, 255, 255), width=int(1.6 * SS))
    for p in P:
        d.line([centre, p], fill=(255, 255, 255, 160), width=SS)
    img = base.resize((N, N), Image.LANCZOS)
    img.save(os.path.join(OUT, "crystal_chip.png"))
    print("wrote crystal_chip.png 64 x 64")


# ------------------------------------------------------------------------------------------------ glint (thin prism sparkle)
def glint():
    N = 64
    ys, xs = np.mgrid[0:N, 0:N].astype(np.float32)
    x, y = (xs + 0.5) / N * 2 - 1, (ys + 0.5) / N * 2 - 1
    cross = np.exp(-(np.abs(x) / 0.045) ** 1.4) * np.exp(-(np.abs(y) / 0.75) ** 2) + np.exp(-(np.abs(y) / 0.045) ** 1.4) * np.exp(-(np.abs(x) / 0.75) ** 2)
    xr, yr = (x + y) * 0.7071, (x - y) * 0.7071
    diag = (np.exp(-(np.abs(xr) / 0.05) ** 1.4) * np.exp(-(np.abs(yr) / 0.35) ** 2) + np.exp(-(np.abs(yr) / 0.05) ** 1.4) * np.exp(-(np.abs(xr) / 0.35) ** 2)) * 0.6
    core = np.exp(-((x * x + y * y) / 0.012))
    a = np.clip(cross + diag + core, 0, 1)
    save("crystal_glint.png", mix(np.array([225, 235, 255], np.float32), np.array([255, 255, 255], np.float32), np.clip(core, 0, 1)), a)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    blade()
    shard()
    flare()
    sigil()
    band()
    streak()
    chip()
    glint()

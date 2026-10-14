"""Generates the boss VFX textures (bfx_*.png) for the 1.35.0 Zogratis effects (vfx/client/layer/Zogratis*FxLayer).

Deterministic (fixed seeds), Pillow + numpy only. White (or lightly coloured) alpha textures: the layers tint them with the vertex colour.
Output: src/main/resources/assets/nusmp/textures/particle/bfx_*.png (never touches time_*.png).

    python3 -B tools/gen_boss_vfx_textures.py            # writes the textures
    python3 -B tools/gen_boss_vfx_textures.py --sheet out.png   # also writes a contact sheet on a dark background

Families
  Dante (gravity)   bfx_warp_disc  bfx_chevron  bfx_ground_crack  bfx_lens  bfx_umbra
  Vanica (blood)    bfx_band  bfx_ribbon  bfx_lance  bfx_blood_sigil
  Zenon (space)     bfx_glass_web  bfx_tear  bfx_portal
  Lucius (divine)   bfx_pillar  bfx_halo  bfx_feather  bfx_wisp  bfx_divine_sigil
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
MADE = {}


def save(name, rgba):
    os.makedirs(OUT, exist_ok=True)
    arr = np.clip(rgba, 0, 255).astype(np.uint8)
    Image.fromarray(arr, "RGBA").save(os.path.join(OUT, name + ".png"))
    MADE[name] = arr
    print("wrote", name, arr.shape[1], "x", arr.shape[0])


def smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def grid(w, h=None):
    h = h or w
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1


def blur(a, r):
    return np.asarray(Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(r)), np.float32) / 255


def noise(h, w, cells, rng, octaves=4):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = cells * (2 ** o)
        g = rng.random((c + 1, c + 1)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(im, np.float32) / 255
        tot += amp
        amp *= 0.5
    return out / tot


def white(alpha, tint=(255, 255, 255)):
    h, w = alpha.shape
    out = np.zeros((h, w, 4), np.float32)
    for i in range(3):
        out[..., i] = tint[i]
    out[..., 3] = np.clip(alpha, 0, 1) * 255
    return out


def tinted(alpha, mix):
    """alpha + a per-pixel colour (mix: h x w x 3 in 0..255)."""
    h, w = alpha.shape
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = mix
    out[..., 3] = np.clip(alpha, 0, 1) * 255
    return out


def draw_lines(size, segs, width, ss=2):
    """Anti-aliased polylines drawn at ss x resolution. segs: list of ((x0,y0),(x1,y1),w) in pixels of the final image."""
    im = Image.new("L", (size[0] * ss, size[1] * ss), 0)
    d = ImageDraw.Draw(im)
    for (a, b, w) in segs:
        d.line([(a[0] * ss, a[1] * ss), (b[0] * ss, b[1] * ss)], fill=255, width=max(1, int(round(w * ss))))
    im = im.resize(size, Image.LANCZOS)
    return np.asarray(im, np.float32) / 255


# ------------------------------------------------------------------------------------------------ Dante: gravity
def gen_warp_disc():
    """Ground disc of warped space: bright rings crowd toward the centre (r^1.9 spacing), a dark umbra in the middle, faint spiral arms."""
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    rr = np.clip(r, 0, 1) ** 1.9
    rings = 0.5 + 0.5 * np.cos(rr * 2 * math.pi * 7.0)
    rings = smooth(0.62, 0.97, rings) * (1 - smooth(0.0, 0.12, 1 - r) * 0)        # crisp thin rings
    rings *= smooth(0.06, 0.2, r) * smooth(1.0, 0.86, r)
    arms = 0.5 + 0.5 * np.cos(th * 3 - np.log(r + 0.05) * 5.0)
    arms = smooth(0.55, 1.0, arms) * smooth(0.12, 0.35, r) * smooth(0.98, 0.5, r) * 0.45
    glow = np.exp(-((r - 0.97) / 0.035) ** 2) * 0.8                                   # bright rim
    inner = smooth(0.3, 0.0, r) * 0.35                                                # the pit's glow
    a = np.clip(rings * 0.85 + arms + glow + inner, 0, 1) * smooth(1.04, 0.95, r)
    save("bfx_warp_disc", white(a))


def gen_chevron():
    """A vertical lane strip with chevrons pointing up (tile in V)."""
    w, h = 64, 128
    x, y = grid(w, h)
    xs = np.abs(x)
    v = (np.mod((y * 0.5 + 0.5) * 2.0, 1.0))                                         # two chevrons per tile
    tip = np.abs(v - 0.5 - 0.5 * xs * 1.0)                                           # a V shape: apex at the centre line
    chev = smooth(0.09, 0.0, np.abs(np.mod(v + xs * 0.45, 1.0) - 0.5) - 0.38) * 0 + smooth(0.07, 0.0, np.abs(np.mod(v + xs * 0.45, 1.0) - 0.5) - 0.17)
    edge = smooth(1.0, 0.7, xs)
    side = np.exp(-((xs - 0.94) / 0.05) ** 2) * 0.7
    save("bfx_chevron", white(np.clip(chev * edge + side, 0, 1)))


def gen_ground_crack():
    """Radial jagged cracks from the centre with side branches; bright core, fading outward."""
    n = 256
    rng = np.random.default_rng(31)
    segs = []
    c = n / 2
    for k in range(11):
        ang = k / 11 * 2 * math.pi + rng.uniform(-0.2, 0.2)
        length = rng.uniform(0.55, 0.98) * c
        px, py = c, c
        steps = 14
        for s in range(steps):
            t0, t1 = s / steps, (s + 1) / steps
            jag = rng.uniform(-0.16, 0.16)
            a2 = ang + jag
            nx, ny = px + math.cos(a2) * length / steps, py + math.sin(a2) * length / steps
            w = 3.4 * (1 - t0) + 0.8
            segs.append(((px, py), (nx, ny), w))
            if rng.random() < 0.28 and s > 2:
                ba = ang + rng.choice([-1, 1]) * rng.uniform(0.5, 0.9)
                bl = length * rng.uniform(0.12, 0.28)
                segs.append(((nx, ny), (nx + math.cos(ba) * bl, ny + math.sin(ba) * bl), w * 0.55))
            px, py = nx, ny
            ang = a2 * 0.5 + ang * 0.5
    a = draw_lines((n, n), segs, 1)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    a = a * smooth(1.0, 0.25, r)
    a = np.clip(a + blur(a, 3) * 0.9 + np.exp(-(r / 0.12) ** 2) * 0.8, 0, 1) * smooth(1.02, 0.92, r)
    save("bfx_ground_crack", white(a))


def gen_lens():
    """Gravitational lens: a thin bright ring with a soft halo and a faint Einstein cross."""
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    ring = np.exp(-((r - 0.62) / 0.022) ** 2)
    halo = np.exp(-((r - 0.62) / 0.16) ** 2) * 0.45
    outer = np.exp(-((r - 0.9) / 0.012) ** 2) * 0.55
    cross = (np.exp(-(x / 0.012) ** 2) + np.exp(-(y / 0.012) ** 2)) * smooth(1.0, 0.6, r) * smooth(0.58, 0.66, r) * 0.6
    a = np.clip(ring + halo + outer + cross, 0, 1) * smooth(1.02, 0.94, r)
    save("bfx_lens", white(a))


def gen_umbra():
    """A soft black disc (draw with ALPHA blend): the sphere of a singularity."""
    n = 128
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    save("bfx_umbra", white(smooth(1.0, 0.7, r), (8, 4, 16)))


# ------------------------------------------------------------------------------------------------ Vanica: blood
def gen_band():
    """A slash / arc / beam band: u runs tail -> head (brighter toward the head), v across with a white-hot centre line."""
    w, h = 256, 64
    x, y = grid(w, h)
    u = (x + 1) / 2
    across = np.abs(y)
    core = np.exp(-(across / 0.10) ** 2)
    body = smooth(1.0, 0.0, across) ** 1.6
    head = smooth(0.0, 0.55, u)
    tail_taper = smooth(1.0, 0.92, u)
    rng = np.random.default_rng(7)
    n = noise(h, w, 6, rng, 3)
    streak = 0.75 + 0.25 * n
    a = (core * 0.9 + body * 0.55 * streak) * head * tail_taper
    save("bfx_band", white(np.clip(a, 0, 1)))


def gen_ribbon():
    """A ribbon for curved streams: U across, V along with the bright head at the TOP (v = 0) and a soft tail at the bottom."""
    w, h = 64, 256
    x, y = grid(w, h)
    t = (y + 1) / 2
    across = np.abs(x)
    core = np.exp(-(across / 0.18) ** 2)
    body = smooth(1.0, 0.0, across) ** 1.5
    rng = np.random.default_rng(19)
    n = noise(h, w, 5, rng, 3)
    head = smooth(0.0, 0.18, t) * 0.0 + 1.0
    fade = smooth(1.0, 0.1, t)
    tip = smooth(0.0, 0.06, t)
    a = (core * 0.95 + body * 0.5 * (0.7 + 0.3 * n)) * fade * tip
    save("bfx_ribbon", white(np.clip(a, 0, 1)))


def gen_lance():
    """A blood lance: slender shaft, barbed head at the TOP of the image (the layer maps V so the head leads)."""
    w, h = 64, 256
    x, y = grid(w, h)
    t = (y + 1) / 2                                                                   # 0 head .. 1 tail
    head_len = 0.30
    half = np.where(t < head_len, 0.85 * np.sin(np.clip(t / head_len, 0, 1) * math.pi / 2) ** 0.9, 0.16 + 0.04 * np.cos(t * 30))
    barb = np.where((t > 0.2) & (t < 0.36), 0.0, 0.0)
    shape = smooth(half + 0.04, half - 0.03, np.abs(x))
    tail_fade = smooth(1.0, 0.55, t)
    core = np.exp(-(x / 0.07) ** 2) * (0.5 + 0.5 * tail_fade)
    rim = np.exp(-((np.abs(x) - half) / 0.05) ** 2) * 0.7 * (t < head_len + 0.1)
    # two barbs below the head
    for sgn in (-1, 1):
        for bt in (0.36, 0.5):
            bx = sgn * (0.16 + 0.35 * (1 - abs((t - bt) / 0.07)).clip(0, 1))
            barb_mask = (np.abs(t - bt) < 0.07) & (np.abs(x - bx) < 0.07)
            shape = np.maximum(shape, barb_mask.astype(np.float32) * 0.9)
    a = np.clip(shape * 0.75 * tail_fade + core * 0.6 + rim, 0, 1)
    save("bfx_lance", white(a))


def gen_blood_sigil():
    """A thorny blood sigil for the ground: ring, eight thorn petals, an inner circle of tiny barbs."""
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    ring = np.exp(-((r - 0.94) / 0.018) ** 2) + np.exp(-((r - 0.84) / 0.012) ** 2) * 0.8
    petals = np.abs(np.cos(th * 4))
    petal_r = 0.28 + 0.52 * petals ** 1.4
    petal_edge = np.exp(-((r - petal_r) / 0.02) ** 2) * smooth(0.12, 0.3, r)
    thorn = np.exp(-((r - (0.84 - 0.1 * (0.5 + 0.5 * np.cos(th * 24)))) / 0.014) ** 2) * 0.7
    cross = (np.exp(-(x / 0.008) ** 2) + np.exp(-(y / 0.008) ** 2)) * smooth(0.82, 0.3, r) * 0.45
    core = np.exp(-(r / 0.07) ** 2)
    a = np.clip(ring + petal_edge * 0.9 + thorn + cross + core, 0, 1) * smooth(1.02, 0.96, r)
    save("bfx_blood_sigil", white(a))


# ------------------------------------------------------------------------------------------------ Zenon: space
def gen_glass_web():
    """Cracked glass: jagged radial cracks of unequal length with side branches and a few short broken arcs (not a spider web); bright thin lines."""
    n = 256
    rng = np.random.default_rng(55)
    c = n / 2
    segs = []
    nrad = 11
    angs = sorted(rng.uniform(0, 2 * math.pi) for _ in range(nrad))
    for ang in angs:
        px, py = c, c
        reach = rng.uniform(0.45, 1.0)
        steps = int(14 * reach) + 3
        a2 = ang
        for s_ in range(steps):
            a2 = ang + rng.uniform(-0.28, 0.28)
            step = c * 0.07 * rng.uniform(0.7, 1.3)
            nx, ny = px + math.cos(a2) * step, py + math.sin(a2) * step
            w = 2.8 * (1 - s_ / (steps + 2)) + 0.7
            segs.append(((px, py), (nx, ny), w))
            if rng.random() < 0.3 and s_ > 1:
                ba = a2 + rng.choice([-1, 1]) * rng.uniform(0.6, 1.1)
                bl = c * rng.uniform(0.08, 0.2)
                segs.append(((nx, ny), (nx + math.cos(ba) * bl, ny + math.sin(ba) * bl), w * 0.5))
            px, py = nx, ny
    for rr in (0.2, 0.38, 0.6):
        k = rng.integers(0, nrad)
        for i in range(nrad):
            if rng.random() < 0.4:
                a0, a1 = angs[i], angs[(i + 1) % nrad]
                if a1 < a0:
                    a1 += 2 * math.pi
                r0, r1 = rr * c * rng.uniform(0.85, 1.15), rr * c * rng.uniform(0.85, 1.15)
                p0 = (c + math.cos(a0) * r0, c + math.sin(a0) * r0)
                p1 = (c + math.cos(a1) * r1, c + math.sin(a1) * r1)
                segs.append((p0, p1, 1.2))
    a = draw_lines((n, n), segs, 1)
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    impact = np.exp(-(r / 0.07) ** 2)
    a = np.clip(a + blur(a, 2.2) * 0.8 + impact, 0, 1) * smooth(1.0, 0.9, r)
    save("bfx_glass_web", white(a))


def gen_tear():
    """A tear in space seen face on: a pointed slit (u along), dark inside, bright jagged rim, a few stars."""
    w, h = 256, 64
    x, y = grid(w, h)
    rng = np.random.default_rng(12)
    jag = noise(h, w, 10, rng, 3) * 0.12
    half = (1 - np.abs(x) ** 1.4) * 0.8 + jag * (1 - np.abs(x))
    inside = smooth(half, half - 0.12, np.abs(y)) * (np.abs(x) < 1)
    rim = np.exp(-((np.abs(y) - half) / 0.07) ** 2) * smooth(1.0, 0.85, np.abs(x))
    stars = (rng.random((h, w)) > 0.992).astype(np.float32)
    stars = blur(stars, 0.9) * 6 * inside
    mix = np.zeros((h, w, 3), np.float32)
    mix[...] = (255, 255, 255)
    a = np.clip(rim * 1.0 + inside * 0.15 + stars, 0, 1)
    save("bfx_tear", white(a))


def gen_portal():
    """A portal ring: bright rim, spiral arms inside, dark heart (the layer adds an umbra behind it)."""
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    rim = np.exp(-((r - 0.88) / 0.035) ** 2) + np.exp(-((r - 0.78) / 0.012) ** 2) * 0.6
    arms = 0.5 + 0.5 * np.cos(th * 5 + r * 14)
    arms = smooth(0.5, 1.0, arms) * smooth(0.15, 0.4, r) * smooth(0.8, 0.5, r) * 0.7
    glow = np.exp(-((r - 0.88) / 0.12) ** 2) * 0.4
    spark = np.exp(-(r / 0.05) ** 2) * 0.8
    a = np.clip(rim + arms + glow + spark, 0, 1) * smooth(1.02, 0.95, r)
    save("bfx_portal", white(a))


# ------------------------------------------------------------------------------------------------ Lucius: divine
def gen_pillar():
    """A column of light: bright core, soft sides, god-ray striations running along V (tile in V)."""
    w, h = 128, 256
    x, y = grid(w, h)
    rng = np.random.default_rng(91)
    n = noise(h, w, 8, rng, 3)
    xs = np.abs(x)
    core = np.exp(-(xs / 0.16) ** 2)
    body = smooth(1.0, 0.0, xs) ** 1.2
    stri = 0.55 + 0.45 * np.cos(x * 22 + n * 5)
    a = core * 0.85 + body * 0.5 * stri
    save("bfx_pillar", white(np.clip(a, 0, 1) * smooth(1.0, 0.8, xs)))


def gen_halo():
    """A holy halo: a thin double ring, fine rays outward, tick marks between."""
    n = 256
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    ring = np.exp(-((r - 0.62) / 0.02) ** 2) + np.exp(-((r - 0.56) / 0.01) ** 2) * 0.7
    glow = np.exp(-((r - 0.62) / 0.1) ** 2) * 0.4
    rays = smooth(0.8, 1.0, 0.5 + 0.5 * np.cos(th * 28)) * smooth(0.64, 0.7, r) * smooth(0.98, 0.7, r) * 0.8
    ticks = smooth(0.85, 1.0, 0.5 + 0.5 * np.cos(th * 56)) * np.exp(-((r - 0.5) / 0.03) ** 2) * 0.7
    a = np.clip(ring + glow + rays + ticks, 0, 1) * smooth(1.02, 0.95, r)
    save("bfx_halo", white(a))


def gen_feather():
    """A feather blade pointing up: soft vane, bright shaft, barbs."""
    w, h = 64, 128
    x, y = grid(w, h)
    t = (y + 1) / 2                                                                   # 0 tip .. 1 base
    half = 0.62 * np.sin(np.clip(t, 0, 1) * math.pi * 0.9) ** 0.8 * (1 - 0.35 * t)
    vane = smooth(half + 0.05, half - 0.05, np.abs(x))
    shaft = np.exp(-((x - 0.0) / 0.035) ** 2) * smooth(1.0, 0.0, t) * 0 + np.exp(-(x / 0.04) ** 2)
    barbs = 0.6 + 0.4 * np.cos((t * 1.0 - np.abs(x) * 0.5) * 70)
    a = np.clip(vane * 0.6 * barbs + shaft * 0.9, 0, 1) * smooth(1.0, 0.82, t) * smooth(0.0, 0.04, t)
    save("bfx_feather", white(a))


def gen_wisp():
    """A soul wisp: round head at the TOP with a flicker tail trailing down."""
    w, h = 64, 128
    x, y = grid(w, h)
    t = (y + 1) / 2
    head = np.exp(-((x / 0.34) ** 2 + ((t - 0.18) / 0.12) ** 2))
    tail_w = 0.30 * (1 - t) + 0.02
    tail = np.exp(-(x / np.maximum(tail_w, 0.03)) ** 2) * smooth(1.0, 0.1, t) * smooth(0.1, 0.25, t) * 0.8
    core = np.exp(-((x / 0.14) ** 2 + ((t - 0.18) / 0.06) ** 2))
    save("bfx_wisp", white(np.clip(head * 0.85 + tail + core, 0, 1)))


def gen_divine_sigil():
    """A holy sigil for the ground: double ring of glyph ticks, a hexagram, a sun at the heart."""
    n = 256
    ss = 2
    N = n * ss
    im = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(im)
    c = N / 2

    def circle(rad, w):
        d.ellipse([c - rad, c - rad, c + rad, c + rad], outline=255, width=w)

    circle(c * 0.97, 5)
    circle(c * 0.90, 3)
    circle(c * 0.62, 3)
    for k in range(48):                                                               # glyph ticks between the outer rings
        a0 = k / 48 * 2 * math.pi
        l0, l1 = c * 0.915, c * (0.955 if k % 2 else 0.945)
        d.line([(c + math.cos(a0) * l0, c + math.sin(a0) * l0), (c + math.cos(a0) * l1, c + math.sin(a0) * l1)], fill=255, width=3)
    for off in (0, math.pi / 3):
        pts = [(c + math.cos(off + k * 2 * math.pi / 3 - math.pi / 2) * c * 0.88, c + math.sin(off + k * 2 * math.pi / 3 - math.pi / 2) * c * 0.88) for k in range(3)]
        d.polygon(pts, outline=255)
        d.line(pts + [pts[0]], fill=255, width=4)
    for k in range(12):                                                               # sun rays at the heart
        a0 = k / 12 * 2 * math.pi
        d.line([(c + math.cos(a0) * c * 0.10, c + math.sin(a0) * c * 0.10), (c + math.cos(a0) * c * (0.30 if k % 2 else 0.22), c + math.sin(a0) * c * (0.30 if k % 2 else 0.22))], fill=255, width=3)
    d.ellipse([c - c * 0.07, c - c * 0.07, c + c * 0.07, c + c * 0.07], fill=255)
    a = np.asarray(im.resize((n, n), Image.LANCZOS), np.float32) / 255
    x, y = grid(n)
    r = np.sqrt(x * x + y * y)
    a = np.clip(a + blur(a, 2.0) * 0.6 + np.exp(-(r / 0.09) ** 2) * 0.7, 0, 1) * smooth(1.04, 0.97, r)
    save("bfx_divine_sigil", white(a))


GENS = [gen_warp_disc, gen_chevron, gen_ground_crack, gen_lens, gen_umbra, gen_band, gen_ribbon, gen_lance, gen_blood_sigil,
        gen_glass_web, gen_tear, gen_portal, gen_pillar, gen_halo, gen_feather, gen_wisp, gen_divine_sigil]


def sheet(path):
    cell = 200
    cols = 6
    rows = (len(MADE) + cols - 1) // cols
    img = Image.new("RGB", (cols * cell, rows * cell), (14, 10, 22))
    d = ImageDraw.Draw(img)
    for i, (name, arr) in enumerate(MADE.items()):
        im = Image.fromarray(arr, "RGBA")
        im.thumbnail((cell - 12, cell - 24))
        bg = Image.new("RGBA", im.size, (24, 20, 36, 255))
        bg.alpha_composite(im)
        gx, gy = (i % cols) * cell, (i // cols) * cell
        img.paste(bg.convert("RGB"), (gx + 6, gy + 6))
        d.text((gx + 6, gy + cell - 16), name, fill=(220, 220, 255))
    img.save(path)
    print("sheet", path)


if __name__ == "__main__":
    for g in GENS:
        g()
    if "--sheet" in sys.argv:
        sheet(sys.argv[sys.argv.index("--sheet") + 1])

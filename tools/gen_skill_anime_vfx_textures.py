"""Generates the anime-style Unique / Ultimate skill VFX textures (SkillAnimeLayer, VFX wave 3, docs/vfx_remake/skill_anime.md).

Deterministic and font-free (numpy maths + Pillow polygons and lines, a fixed seed), in the style of tools/gen_greed_vfx_textures.py: every texture
is white or grey so the layer's vertex colour tints it, shapes are drawn supersampled and downsampled for clean edges, and the colour of a clear
texel is bled in from its visible neighbours so linear filtering never draws a dark fringe.
Output: src/main/resources/assets/anime_multiverse/textures/particle/skillanime_*.png

    python tools/gen_skill_anime_vfx_textures.py

The generic pieces (core, glow, flash, streak, bolt, ring, flame) are the same recipes as the Greed set, so the family is self-contained.
The signature pieces: the golden RIPPLE of the Gate of Babylon, a CHAIN strip (Enkidu), the BELL of Argonaut, the SNOWFLAKE circle and ice SHARDS
of Wynn Fimbulvetr, a MIST puff, rose PETALS (Freya), a HALO, sun RAYS (Goddess of Faith), the hearth SIGIL, a SPARKLE star, the black WING spike
(Black Wings) and a SWIRL band (Ea's spiral wind).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "particle")
PREFIX = "skillanime_"
SS = 4
RNG = np.random.RandomState(3103)

# name -> (width, height); SkillAnimeVfx.TEXTURE_NAMES and SkillAnimeVfxAssetTest read the same list
SIZES = {
    "core": (64, 64),
    "glow": (128, 128),
    "flash": (128, 128),
    "streak": (16, 128),
    "bolt": (16, 128),
    "ring": (128, 32),
    "flame": (64, 128),
    "ripple": (128, 128),
    "chain": (32, 128),
    "bell": (64, 64),
    "snowflake": (128, 128),
    "shard": (32, 64),
    "mist": (64, 64),
    "petal": (32, 32),
    "halo": (128, 128),
    "rays": (128, 128),
    "sigil": (128, 128),
    "sparkle": (64, 64),
    "wing": (32, 128),
    "swirl": (128, 32),
}


# ---------------------------------------------------------------------------------------------------- helpers
def grid(w, h):
    ys, xs = np.mgrid[0:h * SS, 0:w * SS].astype(np.float64)
    return (xs + 0.5) / (w * SS), (ys + 0.5) / (h * SS)


def down(a, w, h):
    return a.reshape(h, SS, w, SS).mean(axis=(1, 3))


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def cel(r, steps):
    a = np.zeros_like(r)
    for rad, al in steps:
        a = np.maximum(a, al * (1 - smooth(rad - 0.015, rad + 0.015, r)))
    return a


def finish(name, alpha, grey=None):
    w, h = SIZES[name]
    a = np.clip(down(alpha, w, h), 0, 1)
    g = np.ones_like(a) if grey is None else np.clip(down(grey, w, h), 0, 1)
    a8 = np.round(a * 255).astype(np.uint8)
    rgb = np.repeat((g * 255)[..., None], 3, axis=2)
    vis = (a8 > 0).astype(np.float64)
    im = Image.fromarray(np.clip(rgb * vis[..., None], 0, 255).astype(np.uint8))
    wim = Image.fromarray((vis * 255).astype(np.uint8))
    num = np.asarray(im.filter(ImageFilter.BoxBlur(2)), dtype=np.float64)
    den = np.asarray(wim.filter(ImageFilter.BoxBlur(2)), dtype=np.float64)[..., None] / 255.0
    bled = np.where(den > 1e-3, num / np.maximum(den, 1e-3), 255.0)
    rgb = np.where(vis[..., None] > 0, rgb, bled)
    out = np.dstack([np.clip(np.round(rgb), 0, 255).astype(np.uint8), a8])
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(out, "RGBA").save(os.path.join(OUT, PREFIX + name + ".png"), optimize=True)
    print("wrote", PREFIX + name, (w, h))


def polar(x, y):
    dx, dy = x * 2 - 1, y * 2 - 1
    return np.sqrt(dx * dx + dy * dy), np.arctan2(dy, dx)


def tile_noise(n, period_cells, seed):
    r = np.random.RandomState(seed)
    k = r.uniform(-1, 1, period_cells)
    t = np.arange(n) / n * period_cells
    i0 = np.floor(t).astype(int) % period_cells
    i1 = (i0 + 1) % period_cells
    f = t - np.floor(t)
    f = f * f * (3 - 2 * f)
    return k[i0] * (1 - f) + k[i1] * f


def canvas(name):
    w, h = SIZES[name]
    im = Image.new("L", (w * SS, h * SS), 0)
    return im, ImageDraw.Draw(im), w * SS, h * SS


def arr(im, blur=0.0):
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur * SS))
    return np.asarray(im, dtype=np.float64) / 255.0


def round_clear(a, name, r0=0.95):
    x, y = grid(*SIZES[name])
    r, _ = polar(x, y)
    return a * (1 - smooth(r0, 0.995, r))


# ---------------------------------------------------------------------------------------------------- the generic pieces (the Greed recipes)
def core():
    x, y = grid(*SIZES["core"])
    r, _ = polar(x, y)
    a = cel(r, [(0.6, 1.0), (0.78, 0.45)]) * (1 - smooth(0.9, 0.99, r))
    finish("core", np.clip(a, 0, 1))


def glow():
    x, y = grid(*SIZES["glow"])
    r, _ = polar(x, y)
    a = np.maximum(0.85 * np.exp(-(r / 0.42) ** 2), cel(r, [(0.22, 0.6)])) * (1 - smooth(0.85, 0.99, r))
    finish("glow", np.clip(a, 0, 1))


def flash():
    x, y = grid(*SIZES["flash"])
    r, th = polar(x, y)
    n = 12
    lens = 0.5 + 0.45 * RNG.uniform(0, 1, n)
    k = (th + math.pi) / (2 * math.pi) * n
    i = np.floor(k).astype(int) % n
    f = k - np.floor(k)
    spike = lens[i] * (1 - np.abs(f - 0.5) * 2) ** 2.0
    a = np.maximum((r < spike).astype(np.float64), cel(r, [(0.2, 1.0), (0.32, 0.5)]))
    a *= 1 - smooth(0.95, 1.0, r)
    finish("flash", a)


def streak():
    x, y = grid(*SIZES["streak"])
    u = np.abs(x * 2 - 1)
    taper = np.sin(np.clip(y, 0, 1) * math.pi) ** 0.7
    a = (1 - smooth(0.25 * taper, 0.25 * taper + 0.12, u)) * taper
    finish("streak", np.clip(a, 0, 1))


def bolt():
    x, y = grid(*SIZES["bolt"])
    u = np.abs(x * 2 - 1)
    a = np.maximum(1 - smooth(0.16, 0.26, u), 0.45 * np.exp(-(u / 0.5) ** 2)) * (1 - smooth(0.85, 1.0, u))
    finish("bolt", np.clip(a, 0, 1))


def ring():
    x, y = grid(*SIZES["ring"])
    w, h = SIZES["ring"]
    gap = tile_noise(w * SS, 9, 41)[None, :]
    lead = 1 - smooth(0.16, 0.24, np.abs(y - 0.12) + 0.05)
    trail = np.clip(1 - (y - 0.12) / 0.88, 0, 1) ** 2 * 0.5 * (y > 0.12)
    a = (lead + trail) * (0.7 + 0.3 * smooth(-0.6, 0.2, gap))
    a *= smooth(0.0, 0.06, y) * (1 - smooth(0.9, 1.0, y))
    finish("ring", np.clip(a, 0, 1))


def flame():
    x, y = grid(*SIZES["flame"])
    t = 1 - y
    sway = 0.07 * np.sin(t * 6.0 + 0.5) * t
    base = np.where(t < 0.25, 0.34 * np.sqrt(np.clip(1 - ((0.25 - t) / 0.25) ** 2, 0, 1)), 0.34 * np.clip((1 - t) / 0.75, 0, 1) ** 0.85)
    right = x > 0.5 + sway
    hw = base * (1 + 0.14 * np.sin(t * 17.0 + 1.0) * smooth(0.15, 0.35, t))
    hw = hw + np.where(right, 0.09, 0.06) * np.exp(-((t - np.where(right, 0.62, 0.48)) / 0.07) ** 2)
    hw = np.minimum(hw, 0.42)
    u = np.abs(x - 0.5 - sway)
    outer = 1 - smooth(hw - 0.02, hw + 0.02, u)
    ihw = base * 0.5 * smooth(0.02, 0.12, t) * (1 - smooth(0.5, 0.75, t))
    inner = 1 - smooth(ihw - 0.02, ihw + 0.02, u)
    inner *= ihw > 0.01
    a = np.clip(outer * 0.8 + inner * 0.2, 0, 1) * (1 - smooth(0.96, 1.0, t))
    grey = 0.7 + 0.3 * inner
    finish("flame", a, np.clip(grey, 0, 1))


# ---------------------------------------------------------------------------------------------------- the signature pieces
def ripple():
    x, y = grid(*SIZES["ripple"])
    r, th = polar(x, y)
    # the Gate of Babylon: a golden ripple in the air, like a drop on still water: a bright wobbling rim, three fainter rings inside, a faint fill
    a = 0.16 * (1 - smooth(0.6, 0.82, r))
    for k, (rad, wid, al) in enumerate([(0.84, 0.035, 1.0), (0.66, 0.03, 0.75), (0.47, 0.026, 0.55), (0.28, 0.022, 0.4)]):
        wob = rad + 0.018 * np.sin(th * (5 + 2 * k) + k * 1.3)
        a = np.maximum(a, al * np.exp(-((r - wob) / wid) ** 2))
    a = np.maximum(a, 0.35 * np.exp(-((r - 0.86) / 0.09) ** 2))
    grey = 0.82 + 0.18 * smooth(0.5, 0.85, r)
    finish("ripple", round_clear(np.clip(a, 0, 1), "ripple"), grey)


def chain():
    im, d, W, H = canvas("chain")
    # Enkidu: a strip of chain along v, two links a tile (the layer draws a chain in short pieces, so the links keep their size): a link seen face on
    # (an oval outline), then one seen edge on (a bar)
    n = 2
    lw = int(0.11 * W)
    for i in range(n):
        y0, y1 = i / n, (i + 1) / n
        if i % 2 == 0:
            d.ellipse([0.2 * W, (y0 - 0.06) * H, 0.8 * W, (y1 + 0.06) * H], outline=255, width=lw)
        else:
            d.rounded_rectangle([0.42 * W, (y0 - 0.08) * H, 0.58 * W, (y1 + 0.08) * H], radius=int(0.08 * W), fill=225)
    a = arr(im, 0.2)
    x, y = grid(*SIZES["chain"])
    a *= smooth(0.0, 0.08, x) * (1 - smooth(0.92, 1.0, x))
    grey = 0.75 + 0.25 * np.clip(1 - np.abs(x * 2 - 1) * 1.5, 0, 1)
    finish("chain", np.clip(a, 0, 1), grey)


def bell():
    im, d, W, H = canvas("bell")
    # Argonaut's bell (the chime of the charge): a dome, a flared skirt, a rim and a clapper, a shine down one side
    right = []
    for k in range(25):
        t = k / 24
        hw = 0.11 + 0.08 * math.sin(t * math.pi * 0.5) + 0.16 * t ** 2.4
        right.append((0.5 + hw, 0.14 + 0.62 * t))
    pts = right + [(1 - px, py) for px, py in reversed(right)]
    d.polygon([(px * W, py * H) for px, py in pts], fill=220)
    d.ellipse([0.42 * W, 0.08 * H, 0.58 * W, 0.18 * H], outline=255, width=int(0.04 * W))
    d.rounded_rectangle([0.12 * W, 0.74 * H, 0.88 * W, 0.8 * H], radius=int(0.03 * W), fill=255)
    d.ellipse([0.43 * W, 0.8 * H, 0.57 * W, 0.92 * H], fill=255)
    d.line([(0.38 * W, 0.24 * H), (0.32 * W, 0.66 * H)], fill=255, width=int(0.05 * W))
    finish("bell", arr(im, 0.25))


def snowflake():
    im, d, W, H = canvas("snowflake")
    # Wynn Fimbulvetr's frost circle: a double ring, a six-armed snowflake with two pairs of branches an arm, hexagon ticks between the rings
    lw = max(2, int(0.02 * W))
    for rr in (0.47, 0.41):
        d.ellipse([(0.5 - rr) * W, (0.5 - rr) * H, (0.5 + rr) * W, (0.5 + rr) * H], outline=255, width=lw)
    for k in range(6):
        ang = k * math.pi / 3 - math.pi / 2
        cx, cy = math.cos(ang), math.sin(ang)
        d.line([(0.5 * W, 0.5 * H), ((0.5 + 0.38 * cx) * W, (0.5 + 0.38 * cy) * H)], fill=255, width=lw)
        for t, ln in ((0.17, 0.09), (0.28, 0.07)):
            bx, by = 0.5 + t * cx, 0.5 + t * cy
            for s in (-1, 1):
                a2 = ang + s * math.pi / 3
                d.line([(bx * W, by * H), ((bx + ln * math.cos(a2)) * W, (by + ln * math.sin(a2)) * H)], fill=255, width=lw)
    for k in range(12):
        ang = k * math.pi / 6
        cx, cy = 0.5 + 0.44 * math.cos(ang), 0.5 + 0.44 * math.sin(ang)
        d.regular_polygon((cx * W, cy * H, 0.018 * W), 6, fill=255)
    hexr = 0.08
    d.regular_polygon((0.5 * W, 0.5 * H, hexr * W), 6, outline=255, width=lw)
    finish("snowflake", round_clear(arr(im, 0.3), "snowflake"))


def shard():
    im, d, W, H = canvas("shard")
    # an ice crystal spike standing up: a long hexagonal prism seen from the side with a sharp tip; the lit facet lighter
    body = [(0.5, 0.03), (0.84, 0.3), (0.8, 0.97), (0.2, 0.97), (0.16, 0.3)]
    d.polygon([(px * W, py * H) for px, py in body], fill=170)
    d.polygon([(0.5 * W, 0.03 * H), (0.84 * W, 0.3 * H), (0.8 * W, 0.97 * H), (0.52 * W, 0.97 * H)], fill=245)
    d.line([(0.5 * W, 0.05 * H), (0.5 * W, 0.95 * H)], fill=255, width=max(1, int(0.05 * W)))
    a = arr(im, 0.2)
    x, y = grid(*SIZES["shard"])
    grey = a.copy()
    alpha = np.clip(a * 1.6, 0, 1) * 0.92 * (1 - smooth(0.96, 1.0, y))
    finish("shard", alpha, np.clip(0.55 + 0.45 * grey, 0, 1))


def mist():
    x, y = grid(*SIZES["mist"])
    r, th = polar(x, y)
    # a soft puff of frost mist: a lumpy round cloud, denser in the middle
    edge = 0.72 + 0.08 * np.sin(th * 5 + 0.7) + 0.05 * np.sin(th * 9 + 2.1)
    a = 0.8 * (1 - smooth(edge - 0.45, edge + 0.1, r)) * (0.82 + 0.18 * np.sin(x * 11 + y * 7) * np.sin(y * 9 - x * 5))
    finish("mist", round_clear(np.clip(a, 0, 1), "mist", 0.9))


def petal():
    x, y = grid(*SIZES["petal"])
    # a rose petal: a broad rounded heart-top tapering to a point, a soft fold line down the middle
    px, py = x * 2 - 1, y * 2 - 1
    top = np.sqrt((np.abs(px) * 1.15 - 0.42) ** 2 + (py + 0.25) ** 2)
    lobe = (1 - smooth(0.42, 0.5, top)) * (py < 0.2)
    point = (np.abs(px) < 0.82 * (1 - (py + 0.1) / 1.0)) * (py >= -0.3) * (py < 0.88)
    a = np.clip(np.maximum(lobe, point.astype(np.float64)), 0, 1)
    a = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(SS * 0.6)), dtype=np.float64) / 255.0
    grey = 0.78 + 0.22 * (1 - smooth(0.0, 0.25, np.abs(px)))
    finish("petal", round_clear(np.clip(a, 0, 1), "petal", 0.92), grey)


def halo():
    x, y = grid(*SIZES["halo"])
    r, _ = polar(x, y)
    # a halo: one hard bright band with a soft glow either side
    a = np.maximum(1 - smooth(0.035, 0.06, np.abs(r - 0.78)), 0.5 * np.exp(-((r - 0.78) / 0.12) ** 2))
    finish("halo", round_clear(np.clip(a, 0, 1), "halo"))


def rays():
    x, y = grid(*SIZES["rays"])
    r, th = polar(x, y)
    # the Goddess of Faith's sunburst: sixteen tapered rays, long and short in turn, round a bright heart
    n = 16
    k = (th + math.pi) / (2 * math.pi) * n
    f = k - np.floor(k)
    idx = np.floor(k).astype(int) % n
    length = np.where(idx % 2 == 0, 0.95, 0.62)
    width = 0.5 * (1 - r / length)
    ray = (np.abs(f - 0.5) < width * 0.5) * (r < length)
    a = np.maximum(ray * (0.55 + 0.45 * (1 - r / length)), 0.9 * np.exp(-(r / 0.2) ** 2))
    a = np.asarray(Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(SS * 0.4)), dtype=np.float64) / 255.0
    finish("rays", round_clear(np.clip(a, 0, 1), "rays"))


def sigil():
    im, d, W, H = canvas("sigil")
    # the sacred hearth circle (Hestia; also the ground seal of the gravity and the hellfire): an outer double ring, eight flame marks between,
    # an inner ring with a hexagram, dots on the outer band
    lw = max(2, int(0.018 * W))
    for rr in (0.47, 0.42, 0.24):
        d.ellipse([(0.5 - rr) * W, (0.5 - rr) * H, (0.5 + rr) * W, (0.5 + rr) * H], outline=255, width=lw)
    for k in range(8):
        ang = k * math.pi / 4
        cx, cy = 0.5 + 0.33 * math.cos(ang), 0.5 + 0.33 * math.sin(ang)
        ox, oy = math.cos(ang), math.sin(ang)
        tx, ty = -oy, ox
        tip = (cx + 0.07 * ox, cy + 0.07 * oy)
        base = [(cx - 0.035 * tx - 0.03 * ox, cy - 0.035 * ty - 0.03 * oy), (cx + 0.035 * tx - 0.03 * ox, cy + 0.035 * ty - 0.03 * oy)]
        d.polygon([(tip[0] * W, tip[1] * H), (base[1][0] * W, base[1][1] * H), (base[0][0] * W, base[0][1] * H)], fill=255)
    for k in range(2):
        pts = [(0.5 + 0.22 * math.cos(-math.pi / 2 + k * math.pi / 3 + j * 2 * math.pi / 3), 0.5 + 0.22 * math.sin(-math.pi / 2 + k * math.pi / 3 + j * 2 * math.pi / 3)) for j in range(4)]
        d.line([(px * W, py * H) for px, py in pts], fill=255, width=lw, joint="curve")
    for k in range(24):
        ang = k * math.pi / 12 + 0.13
        cx, cy = 0.5 + 0.445 * math.cos(ang), 0.5 + 0.445 * math.sin(ang)
        d.ellipse([(cx - 0.009) * W, (cy - 0.009) * H, (cx + 0.009) * W, (cy + 0.009) * H], fill=255)
    finish("sigil", round_clear(arr(im, 0.3), "sigil"))


def sparkle():
    x, y = grid(*SIZES["sparkle"])
    dx, dy = np.abs(x * 2 - 1), np.abs(y * 2 - 1)
    r = np.sqrt(dx * dx + dy * dy)
    # a four-pointed twinkle: a long cross with thin points, a short diagonal cross, a bright heart
    cross = np.maximum(np.exp(-(dy / (0.05 + 0.1 * (1 - dx))) ** 2) * (1 - dx) ** 1.6, np.exp(-(dx / (0.05 + 0.1 * (1 - dy))) ** 2) * (1 - dy) ** 1.6)
    u, v = (dx + dy) / math.sqrt(2), np.abs(dx - dy) / math.sqrt(2)
    diag = np.exp(-(v / 0.04) ** 2) * np.clip(1 - u / 0.55, 0, 1) ** 2 * 0.6
    a = np.maximum(np.maximum(cross, diag), np.exp(-(r / 0.12) ** 2))
    finish("sparkle", round_clear(np.clip(a, 0, 1), "sparkle", 0.96))


def wing():
    x, y = grid(*SIZES["wing"])
    t = 1 - y                                                    # 0 at the root (bottom row, the start of a beam), 1 at the tip
    # one spike of Accelerator's black wings: a long jagged plume, broad at the root and sharp at the tip, saw-teeth on both edges, a lighter vein
    hw = 0.42 * (1 - t) ** 0.8 * (0.6 + 0.4 * smooth(0.0, 0.12, t))
    teeth = 0.08 * (1 - t) * np.abs(np.sin(t * 22.0))
    u = np.abs(x * 2 - 1) * 0.5
    a = 1 - smooth(hw + teeth - 0.015, hw + teeth + 0.015, u)
    a *= smooth(0.0, 0.03, t) * (1 - smooth(0.97, 1.0, t))
    grey = 0.55 + 0.45 * (1 - smooth(0.0, 0.08, u))
    finish("wing", np.clip(a, 0, 1), np.clip(grey, 0, 1))


def swirl():
    x, y = grid(*SIZES["swirl"])
    # Ea's spiral wind: diagonal streaks across a hoop band (U wraps round the hoop), periodic in U, fading at both edges of V
    k = np.mod(x * 6 + y * 0.9, 1.0)
    stripe = np.exp(-((k - 0.5) / 0.16) ** 2)
    a = stripe * np.sin(np.clip(y, 0, 1) * math.pi) ** 1.2 * (0.7 + 0.3 * np.sin(x * 2 * math.pi * 3 + 0.5) ** 2)
    a *= smooth(0.0, 0.06, y) * (1 - smooth(0.94, 1.0, y))
    finish("swirl", np.clip(a, 0, 1))


MAKERS = {"core": core, "glow": glow, "flash": flash, "streak": streak, "bolt": bolt, "ring": ring, "flame": flame, "ripple": ripple, "chain": chain,
          "bell": bell, "snowflake": snowflake, "shard": shard, "mist": mist, "petal": petal, "halo": halo, "rays": rays, "sigil": sigil,
          "sparkle": sparkle, "wing": wing, "swirl": swirl}

if __name__ == "__main__":
    assert set(MAKERS) == set(SIZES)
    for name in SIZES:
        MAKERS[name]()

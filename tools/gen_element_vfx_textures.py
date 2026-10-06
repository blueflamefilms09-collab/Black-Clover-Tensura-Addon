"""Generates the Fire / Water / Wind / Earth VFX textures (deterministic, no fonts, no external images).

    python tools/gen_element_vfx_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/{fire,water,wind,earth}_*.png

Look reference: the Black Clover anime / wiki spell art.
  Fire  - Leo Rugiens (a lion made of flame), Sol Linea (spiralling flame spear), Ignis Columna / Calderos (flame pillars).
          Flames are yellow-white at the core, orange, then red at the tips.
  Water - Sea Dragon's Roar (a water dragon head on a sinuous body), Sea Dragon's Cradle (a smooth whirling water sphere ringed by
          watery globs), Waterball impacts (splash crowns). Pale blue, white foam highlights.
  Wind  - Spirit Storm / Tornado Fang (green-white tornado), Swallow's Gale (swallows made of wind), Crescent Kamaitachi.
  Earth - Ground Wall / Rising Ground / Mother Earth spells: stone spikes and slabs, rock debris, dust clouds, cracked ground.

Texture convention (same as the rest of textures/particle): white or greyscale where one vertex colour tints it; real colours
where one effect needs several tones at once (flames, the dragon and lion heads, rock).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4
RNG = np.random.default_rng(20261006)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def rgba(r, g, b, a):
    return Image.fromarray(np.dstack([np.clip(c, 0, 255) for c in (r, g, b, a)]).astype(np.uint8), "RGBA")


def value_noise(w, h, cell, seed, tile=True):
    """Smooth tileable value noise in 0..1."""
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


def fbm(w, h, cell, seed, octaves=4):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + value_noise(w, h, max(2, cell >> o), seed + o) * amp
        tot += amp
        amp *= 0.5
    return n / tot


FIRE_CORE = np.array([255, 252, 215], np.float32)
FIRE_MID = np.array([255, 168, 40], np.float32)
FIRE_EDGE = np.array([225, 52, 18], np.float32)


def fire_colour(heat):
    """heat 1 = white-yellow core, 0.5 = orange, 0 = red edge."""
    h = np.clip(heat, 0, 1)[..., None]
    lo = FIRE_EDGE + (FIRE_MID - FIRE_EDGE) * np.clip(h * 2, 0, 1)
    return lo + (FIRE_CORE - lo) * np.clip(h * 2 - 1, 0, 1)


# ------------------------------------------------------------------------------------------------ fire
def fire_tongue(w=64, h=128):
    """One flame tongue, base at the bottom, tip at the top; coloured core-to-edge, soft alpha (drawn additively)."""
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = 1 - yy / (h - 1)                                     # 0 bottom, 1 top
    wobble = 0.10 * np.sin(v * 7.0) * v
    width = np.clip(np.sin(np.clip(v, 0, 1) ** 0.8 * math.pi) * (1.05 - v * 0.55) + 0.02, 0.0, 1.0) * np.where(v < 0.18, (v / 0.18) ** 0.5, 1)
    d = np.abs(u - wobble) / np.maximum(width, 1e-3)
    shape = np.clip(1 - d, 0, 1)
    n = fbm(w, h, 16, 3)
    heat = np.clip(shape * 1.25 - v * 0.55 + (n - 0.5) * 0.3, 0, 1)
    a = np.clip(shape ** 0.7 * 1.6, 0, 1) * np.clip(1.15 - v ** 3, 0, 1)
    c = fire_colour(heat)
    save(rgba(c[..., 0], c[..., 1], c[..., 2], a * 255), "fire_tongue")


def fire_band(w=256, h=64):
    """Tileable ring of flame tongues rising from a hot base (bottom row) - for flame pillars and rings."""
    xx, yy = grid(w, h)
    v = 1 - yy / (h - 1)
    n1 = value_noise(w, h, 32, 11)
    n2 = value_noise(w, h, 12, 12)
    tongues = 0.5 + 0.5 * np.sin(xx / w * math.pi * 2 * 9 + n1 * 3.0)       # 9 tongues around, wobbling
    reach = 0.45 + 0.55 * tongues ** 1.5 * (0.7 + 0.3 * n2)
    shape = np.clip((reach - v) / 0.22, 0, 1)
    heat = np.clip(shape * 0.78 - v * 0.6 + 0.05 * n1, 0, 1)
    a = np.clip(shape * 1.2, 0, 1) * np.clip(v * 5, 0, 1) ** 0.5
    c = fire_colour(heat)
    save(rgba(c[..., 0], c[..., 1], c[..., 2], a * 255), "fire_band")


def fire_ribbon(w=32, h=128):
    """Soft streak for spear trails and the lion's body: bright core across, flickering along."""
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    n = value_noise(w, h, 16, 21)
    across = np.exp(-(u / 0.45) ** 2)
    heat = np.clip(across * (0.75 + 0.35 * n), 0, 1)
    a = np.clip(across * (0.7 + 0.5 * n), 0, 1)
    c = fire_colour(heat)
    save(rgba(c[..., 0], c[..., 1], c[..., 2], a * 255), "fire_ribbon")


def fire_lion(size=256):
    """Leo Rugiens: a roaring lion's head made of flame, facing the viewer. Alpha-blended (keeps the dark features)."""
    S = size * SS
    c = S / 2
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    # mane: flame tongues radiating out from behind the face
    for k in range(34):
        a = 2 * math.pi * k / 26 + RNG.uniform(-0.06, 0.06)
        length = S * RNG.uniform(0.40, 0.50)
        base = S * 0.20
        width = S * 0.075
        tipx, tipy = c + math.sin(a) * length, c * 1.02 - math.cos(a) * length
        # curl the tip a little sideways like a flame
        tipx += math.cos(a) * S * 0.04
        l1 = (c + math.sin(a - 0.35) * base, c * 1.02 - math.cos(a - 0.35) * base)
        l2 = (c + math.sin(a + 0.35) * base, c * 1.02 - math.cos(a + 0.35) * base)
        mid1 = (c + math.sin(a - 0.12) * length * 0.75 - math.cos(a) * width * 0.3, c * 1.02 - math.cos(a - 0.12) * length * 0.75)
        mid2 = (c + math.sin(a + 0.12) * length * 0.75 + math.cos(a) * width * 0.3, c * 1.02 - math.cos(a + 0.12) * length * 0.75)
        col = (255, int(120 + 60 * RNG.random()), 30, 235)
        d.polygon([l1, mid1, (tipx, tipy), mid2, l2], fill=col)
    mane = im.filter(ImageFilter.GaussianBlur(SS * 2))
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    im.alpha_composite(mane)
    d = ImageDraw.Draw(im)
    # inner mane ring (hotter)
    d.ellipse([c - S * 0.30, c - S * 0.30, c + S * 0.30, c + S * 0.32], fill=(255, 150, 45, 245))
    # ears
    for sx in (-1, 1):
        d.polygon([(c + sx * S * 0.13, c - S * 0.19), (c + sx * S * 0.24, c - S * 0.30), (c + sx * S * 0.25, c - S * 0.15)], fill=(255, 150, 40, 255))
    # face: wide brow, narrowing to the muzzle
    face = [(c - S * 0.20, c - S * 0.16), (c + S * 0.20, c - S * 0.16), (c + S * 0.22, c - S * 0.02), (c + S * 0.14, c + S * 0.14),
            (c + S * 0.08, c + S * 0.24), (c - S * 0.08, c + S * 0.24), (c - S * 0.14, c + S * 0.14), (c - S * 0.22, c - S * 0.02)]
    d.polygon(face, fill=(255, 196, 80, 255))
    inner = [(x * 0.8 + c * 0.2, y * 0.8 + c * 0.2) for x, y in face]
    d.polygon(inner, fill=(255, 226, 130, 255))
    # brow ridge shadow and fierce slanted eyes
    for sx in (-1, 1):
        d.polygon([(c + sx * S * 0.02, c - S * 0.07), (c + sx * S * 0.18, c - S * 0.11), (c + sx * S * 0.17, c - S * 0.075),
                   (c + sx * S * 0.03, c - S * 0.035)], fill=(215, 90, 25, 255))
        d.polygon([(c + sx * S * 0.045, c - S * 0.035), (c + sx * S * 0.155, c - S * 0.07), (c + sx * S * 0.13, c - S * 0.025),
                   (c + sx * S * 0.06, c - S * 0.01)], fill=(255, 255, 240, 255))
    # nose bridge and nose
    d.polygon([(c - S * 0.035, c - S * 0.03), (c + S * 0.035, c - S * 0.03), (c + S * 0.055, c + S * 0.07), (c - S * 0.055, c + S * 0.07)], fill=(250, 190, 90, 255))
    d.polygon([(c - S * 0.06, c + S * 0.065), (c + S * 0.06, c + S * 0.065), (c, c + S * 0.11)], fill=(150, 45, 20, 255))
    # roaring open mouth with fangs
    d.ellipse([c - S * 0.10, c + S * 0.12, c + S * 0.10, c + S * 0.27], fill=(120, 25, 15, 255))
    d.ellipse([c - S * 0.07, c + S * 0.16, c + S * 0.07, c + S * 0.26], fill=(255, 120, 40, 255))      # fire in the throat
    for sx in (-1, 1):
        d.polygon([(c + sx * S * 0.075, c + S * 0.13), (c + sx * S * 0.045, c + S * 0.13), (c + sx * S * 0.06, c + S * 0.19)], fill=(255, 250, 230, 255))
        d.polygon([(c + sx * S * 0.07, c + S * 0.265), (c + sx * S * 0.04, c + S * 0.265), (c + sx * S * 0.055, c + S * 0.215)], fill=(255, 250, 230, 255))
    # whisker pads
    for sx in (-1, 1):
        d.ellipse([c + sx * S * 0.06 - S * 0.06, c + S * 0.08, c + sx * S * 0.06 + S * 0.06, c + S * 0.16], fill=(255, 215, 120, 255))
    im = im.resize((size, size), Image.LANCZOS)
    glow = im.filter(ImageFilter.GaussianBlur(4))
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    g = np.array(glow).astype(np.float32)
    g[..., 3] *= 0.7
    out.alpha_composite(Image.fromarray(g.astype(np.uint8), "RGBA"))
    out.alpha_composite(im)
    save(out, "fire_lion")


# ------------------------------------------------------------------------------------------------ water
def water_band(w=256, h=64):
    """Tileable flowing water: translucent pale blue with white foam streaks and a bright crest line."""
    xx, yy = grid(w, h)
    v = yy / (h - 1)
    n = fbm(w, h, 32, 31)
    streaks = np.clip(np.sin(v * 2 * math.pi * 3 + n * 7 + np.sin(xx / w * 2 * math.pi * 2) * 1.5) * 0.5 + 0.5, 0, 1) ** 6
    crest = np.exp(-((v - 0.22 - 0.06 * np.sin(xx / w * 2 * math.pi * 3)) / 0.06) ** 2)
    edge = np.clip(np.sin(v * math.pi) * 1.6, 0, 1)
    a = np.clip((0.45 + 0.35 * n + 0.6 * streaks + 0.5 * crest) * edge, 0, 1)
    white = np.clip(streaks * 0.9 + crest, 0, 1)
    r = 120 + 135 * white
    g = 190 + 65 * white
    b = 255 + 0 * white
    save(rgba(r, g, b, a * 255), "water_band")


def water_sphere(size=128):
    """Sea Dragon's Cradle: a smooth whirling water sphere - fresnel rim, swirling flow lines, a soft highlight."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    inside = np.clip((1 - r) / 0.025, 0, 1)
    z = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    fres = (1 - z) ** 2.0
    ang = np.arctan2(ny, nx)
    swirl = np.clip(np.sin(ang * 3 + r * 9.0) * 0.5 + 0.5, 0, 1) ** 5 * (0.3 + 0.7 * r)
    hl = np.exp(-(((nx + 0.35) / 0.25) ** 2 + ((ny + 0.40) / 0.18) ** 2)) * 0.7
    a = np.clip(0.30 + 0.6 * fres + 0.35 * swirl + hl, 0, 1) * inside
    white = np.clip(swirl * 0.8 + hl + fres * 0.3, 0, 1)
    save(rgba(110 + 145 * white, 185 + 70 * white, 255 + 0 * white, a * 255), "water_sphere")


def water_drop(size=64):
    """A watery glob: round, bright rim, highlight, slightly darker core."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    inside = np.clip((0.95 - r) / 0.06, 0, 1)
    z = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    hl = np.exp(-(((nx + 0.3) / 0.22) ** 2 + ((ny + 0.35) / 0.16) ** 2))
    white = np.clip((1 - z) ** 2 * 0.8 + hl, 0, 1)
    a = np.clip(0.55 + 0.45 * (1 - z) + hl, 0, 1) * inside
    save(rgba(130 + 125 * white, 200 + 55 * white, 255 + 0 * white, a * 255), "water_drop")


def water_dragon(w=128, h=256):
    """Sea Dragon's Roar head seen from above, snout pointing DOWN (towards v=1, the direction of travel when drawn as a beam).
    Symmetric left/right so it reads from either side of the flight path. Translucent water with white foam edges."""
    W_, H_ = w * SS, h * SS
    cx = W_ / 2
    im = Image.new("RGBA", (W_, H_), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    body = (90, 175, 250, 220)
    light = (190, 235, 255, 245)
    foam = (245, 252, 255, 255)
    # swept-back horns / fins
    for sx in (-1, 1):
        d.polygon([(cx + sx * W_ * 0.18, H_ * 0.40), (cx + sx * W_ * 0.48, H_ * 0.06), (cx + sx * W_ * 0.30, H_ * 0.34), (cx + sx * W_ * 0.22, H_ * 0.47)], fill=light)
        d.polygon([(cx + sx * W_ * 0.22, H_ * 0.50), (cx + sx * W_ * 0.47, H_ * 0.30), (cx + sx * W_ * 0.32, H_ * 0.52)], fill=body)
    # skull and long snout
    d.polygon([(cx - W_ * 0.26, H_ * 0.36), (cx + W_ * 0.26, H_ * 0.36), (cx + W_ * 0.30, H_ * 0.55), (cx + W_ * 0.18, H_ * 0.80),
               (cx + W_ * 0.12, H_ * 0.97), (cx - W_ * 0.12, H_ * 0.97), (cx - W_ * 0.18, H_ * 0.80), (cx - W_ * 0.30, H_ * 0.55)], fill=body)
    d.polygon([(cx - W_ * 0.10, H_ * 0.40), (cx + W_ * 0.10, H_ * 0.40), (cx + W_ * 0.06, H_ * 0.90), (cx - W_ * 0.06, H_ * 0.90)], fill=light)
    # neck flowing back up
    d.polygon([(cx - W_ * 0.20, 0), (cx + W_ * 0.20, 0), (cx + W_ * 0.26, H_ * 0.38), (cx - W_ * 0.26, H_ * 0.38)], fill=body)
    # eyes and brow ridges
    for sx in (-1, 1):
        d.polygon([(cx + sx * W_ * 0.12, H_ * 0.52), (cx + sx * W_ * 0.27, H_ * 0.48), (cx + sx * W_ * 0.24, H_ * 0.56)], fill=foam)
        d.ellipse([cx + sx * W_ * 0.19 - W_ * 0.035, H_ * 0.555, cx + sx * W_ * 0.19 + W_ * 0.035, H_ * 0.595], fill=(20, 70, 160, 255))
        # nostrils and foam whiskers trailing back
        d.ellipse([cx + sx * W_ * 0.06 - W_ * 0.02, H_ * 0.90, cx + sx * W_ * 0.06 + W_ * 0.02, H_ * 0.925], fill=(30, 90, 180, 255))
        d.line([(cx + sx * W_ * 0.14, H_ * 0.86), (cx + sx * W_ * 0.34, H_ * 0.74), (cx + sx * W_ * 0.40, H_ * 0.62)], fill=foam, width=int(SS * 1.5))
    im = im.resize((w, h), Image.LANCZOS)
    a = np.array(im).astype(np.float32)
    alpha = a[..., 3] / 255
    edge = np.clip(np.array(Image.fromarray((alpha * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(3))).astype(np.float32) / 255 - alpha, 0, 1)
    a[..., :3] = a[..., :3] * (1 - edge[..., None]) + 255 * edge[..., None]
    a[..., 3] = np.clip(a[..., 3] + edge * 255, 0, 255)
    # flow lines inside
    n = fbm(w, h, 16, 41)
    flow = np.clip(np.sin(np.mgrid[0:h, 0:w][0] / h * 40 + n * 8) * 0.5 + 0.5, 0, 1) ** 8 * alpha
    a[..., :3] = a[..., :3] * (1 - flow[..., None] * 0.7) + 255 * flow[..., None] * 0.7
    fade = np.clip(np.mgrid[0:h, 0:w][0] / (h * 0.25), 0, 1)
    a[..., 3] *= fade
    save(Image.fromarray(a.astype(np.uint8), "RGBA"), "water_dragon")


def water_flow(w=64, h=256):
    """The dragon's body: water flowing ALONG v (beam length), bright core, foam streaks, soft sides. Tiles along v."""
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    n = fbm(w, h, 32, 42)
    streaks = np.clip(np.sin(u * 9 + n * 5 + np.sin(yy / h * 2 * math.pi * 2) * 1.2) * 0.5 + 0.5, 0, 1) ** 5
    side = np.clip(1 - np.abs(u) ** 2, 0, 1)
    a = np.clip((0.5 + 0.3 * n + 0.6 * streaks) * side, 0, 1)
    white = np.clip(streaks * 0.9 + (1 - np.abs(u)) * 0.15, 0, 1)
    save(rgba(110 + 145 * white, 185 + 70 * white, 255 + 0 * white, a * 255), "water_flow")


# ------------------------------------------------------------------------------------------------ wind
def wind_streak(w=256, h=32):
    """One long wisp, thick in the middle, tapering and fading at both ends. White (tinted pale green)."""
    xx, yy = grid(w, h)
    u = xx / (w - 1)
    v = (yy - (h - 1) / 2) / ((h - 1) / 2)
    thick = np.clip(np.sin(u * math.pi), 0, 1) ** 0.6 * 0.7 + 0.05
    curve = 0.25 * np.sin(u * math.pi * 1.2)
    a = np.clip(1 - np.abs(v - curve) / thick, 0, 1) ** 1.5 * np.clip(np.sin(u * math.pi) * 1.5, 0, 1)
    a = np.nan_to_num(a)
    save(rgba(255 + 0 * a, 255 + 0 * a, 255 + 0 * a, a * 255), "wind_streak")


def wind_band(w=256, h=64):
    """Tileable band of diagonal wind wisps (tornado walls). White, soft."""
    xx, yy = grid(w, h)
    n = value_noise(w, h, 32, 51)
    k = (yy / h * 2 * math.pi * 2.0 + xx / w * 2 * math.pi * 3 + n * 2.5)     # wisps sweep round the ring, slightly slanted
    wisps = np.clip(np.sin(k) * 0.5 + 0.5, 0, 1) ** 4
    fine = np.clip(np.sin(k * 3.0 + 1.7) * 0.5 + 0.5, 0, 1) ** 8 * 0.5
    v = yy / (h - 1)
    edge = np.clip(np.sin(v * math.pi) * 1.4, 0, 1)
    a = np.clip((wisps + fine) * edge * (0.6 + 0.4 * n), 0, 1)
    save(rgba(255 + 0 * a, 255 + 0 * a, 255 + 0 * a, a * 255), "wind_band")


def wind_swallow(size=128):
    """Swallow's Gale: a swallow made of wind, seen from above, head DOWN (direction of travel), forked tail up."""
    S = size * SS
    c = S / 2
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    white = (255, 255, 255, 255)
    # body
    d.ellipse([c - S * 0.07, S * 0.38, c + S * 0.07, S * 0.80], fill=white)
    d.ellipse([c - S * 0.055, S * 0.72, c + S * 0.055, S * 0.86], fill=white)       # head
    # swept wings
    for sx in (-1, 1):
        d.polygon([(c + sx * S * 0.04, S * 0.55), (c + sx * S * 0.48, S * 0.36), (c + sx * S * 0.46, S * 0.42), (c + sx * S * 0.06, S * 0.68)], fill=white)
        d.polygon([(c + sx * S * 0.03, S * 0.42), (c + sx * S * 0.20, S * 0.06), (c + sx * S * 0.10, S * 0.40)], fill=white)   # forked tail
    im = im.resize((size, size), Image.LANCZOS)
    a = np.array(im).astype(np.float32)
    # make it wispy: streak noise along the length
    n = value_noise(size, size, 8, 61)
    a[..., 3] *= (0.6 + 0.4 * n)
    glow = Image.fromarray(a.astype(np.uint8), "RGBA").filter(ImageFilter.GaussianBlur(3))
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.alpha_composite(glow)
    out.alpha_composite(Image.fromarray(a.astype(np.uint8), "RGBA"))
    save(out, "wind_swallow")


def wind_leaf(size=32):
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.ellipse([S * 0.25, S * 0.08, S * 0.75, S * 0.92], fill=(255, 255, 255, 255))
    d.line([(S * 0.5, S * 0.1), (S * 0.5, S * 0.95)], fill=(200, 200, 200, 255), width=SS * 2)
    save(im.resize((size, size), Image.LANCZOS), "wind_leaf")


# ------------------------------------------------------------------------------------------------ earth
def earth_rock(size=128):
    """Stone face for spikes and slabs: layered brown-grey rock with cracks. Opaque, real colours (vertex colour shades it)."""
    n = fbm(size, size, 32, 71, 5)
    strata = 0.5 + 0.5 * np.sin(np.mgrid[0:size, 0:size][0] / size * math.pi * 10 + n * 4)
    base = np.array([150, 118, 84], np.float32)
    dark = np.array([88, 70, 54], np.float32)
    t = np.clip(n * 0.7 + strata * 0.3, 0, 1)[..., None]
    col = dark + (base - dark) * t
    cr = fbm(size, size, 32, 72, 3)
    cracks = np.exp(-((cr - 0.5) / 0.012) ** 2) * (value_noise(size, size, 32, 73) > 0.45)
    grain = fbm(size, size, 4, 74, 2)
    col = col * (0.85 + 0.3 * grain[..., None]) * (1 - 0.6 * cracks[..., None])
    speck = (RNG.random((size, size)) > 0.985)[..., None] * 30
    col = col + speck
    save(rgba(col[..., 0], col[..., 1], col[..., 2], np.full((size, size), 255)), "earth_rock")


def earth_dust(size=128):
    """Billowing dust cloud: lumpy soft puff, lighter on top. Greyscale, tinted tan."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    n = fbm(size, size, 32, 81)
    r = np.sqrt(nx ** 2 + ny ** 2) - (n - 0.5) * 0.5
    a = np.clip((0.85 - r) / 0.4, 0, 1) ** 1.3
    shade = np.clip(0.75 - ny * 0.25 + (n - 0.5) * 0.4, 0, 1)
    g = 255 * shade
    save(rgba(g, g, g, a * 230), "earth_dust")


def earth_crack(w=256, h=32):
    """Tileable jagged crack: dark core with a bright edge (the edge picks up the magic's glow when drawn additively)."""
    xx, yy = grid(w, h)
    v = (yy - (h - 1) / 2) / ((h - 1) / 2)
    n = value_noise(w, 1, 16, 91)[0]
    n2 = value_noise(w, 1, 5, 92)[0]
    path = (n - 0.5) * 1.0 + (n2 - 0.5) * 0.4
    width = 0.18 + 0.12 * value_noise(w, 1, 24, 93)[0]
    d = np.abs(v - path[None, :]) / width[None, :]
    a = np.clip(1.4 - d, 0, 1)
    core = np.clip(1 - d * 1.6, 0, 1)
    g = 255 * (1 - core * 0.85)
    save(rgba(g, g, g, a * 255), "earth_crack")


def earth_chunk(size=64):
    """A tumbling rock chunk: irregular polygon, lit from the top left, real colours."""
    S = size * SS
    c = S / 2
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts = []
    for k in range(9):
        a = 2 * math.pi * k / 9 + RNG.uniform(-0.2, 0.2)
        r = S * RNG.uniform(0.30, 0.45)
        pts.append((c + math.cos(a) * r, c + math.sin(a) * r))
    d.polygon(pts, fill=(118, 94, 70, 255))
    d.polygon([pts[4], pts[5], pts[6], pts[7], (c, c)], fill=(160, 130, 96, 255))     # lit facet
    d.polygon([pts[0], pts[1], pts[2], (c, c)], fill=(78, 62, 48, 255))                 # shadow facet
    save(im.resize((size, size), Image.LANCZOS), "earth_chunk")


if __name__ == "__main__":
    fire_tongue()
    fire_band()
    fire_ribbon()
    fire_lion()
    water_band()
    water_sphere()
    water_drop()
    water_dragon()
    water_flow()
    wind_streak()
    wind_band()
    wind_swallow()
    wind_leaf()
    earth_rock()
    earth_dust()
    earth_crack()
    earth_chunk()

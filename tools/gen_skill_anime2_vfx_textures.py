"""Generates the wave 4 skill VFX textures (SkillAnimeLayer2, docs/vfx_remake/skill_anime_wave4.md): the signature pieces the Beast Will, hero weapon,
grimoire attribute and card skills need on top of the wave 3 set (tools/gen_skill_anime_vfx_textures.py, skillanime_*).

Deterministic and font-free (numpy maths + Pillow polygons and lines, a fixed seed), the same recipe as the wave 3 generator: every texture is white or
grey so the layer's vertex colour tints it, shapes are drawn supersampled and downsampled for clean edges, and the colour of a clear texel is bled in
from its visible neighbours so linear filtering never draws a dark fringe.
Output: src/main/resources/assets/anime_multiverse/textures/particle/skillanime2_*.png

    python tools/gen_skill_anime2_vfx_textures.py

LEAF, DROPLET, ROCK, BUBBLE, FEATHER (the nature, water, earth, poison and wind elements of the beasts), RUNE (a runic ring: seals, staffs, books),
CLAW (three rake marks), CARD (a summon card with a star), EYE (Hawk Eye), CRESCENT (a thin great slash arc), HEX (a barrier cell ring), STITCH
(a dashed thread), NOTE (the instrument), PUFF (a cotton cloud) and FACET (a mirror shard).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "particle")
PREFIX = "skillanime2_"
SS = 4

# name -> (width, height); SkillAnimeVfx2.TEXTURE_NAMES and SkillAnimeVfx2AssetTest read the same list
SIZES = {
    "leaf": (32, 64),
    "droplet": (32, 64),
    "rock": (32, 32),
    "bubble": (64, 64),
    "feather": (32, 128),
    "rune": (128, 128),
    "claw": (64, 128),
    "card": (64, 128),
    "eye": (128, 64),
    "crescent": (128, 128),
    "hex": (128, 128),
    "stitch": (16, 128),
    "note": (32, 64),
    "puff": (64, 64),
    "facet": (64, 64),
}


# ---------------------------------------------------------------------------------------------------- helpers (the wave 3 recipes)
def grid(w, h):
    ys, xs = np.mgrid[0:h * SS, 0:w * SS].astype(np.float64)
    return (xs + 0.5) / (w * SS), (ys + 0.5) / (h * SS)


def down(a, w, h):
    return a.reshape(h, SS, w, SS).mean(axis=(1, 3))


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


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


def edge_fade(a, m=1.5):
    """Clears the outermost texels so a shape never touches the border (the asset test wants clear corners on the round ones)."""
    h, w = a.shape
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy)) / SS
    return a * smooth(0.0, m, d)


# ---------------------------------------------------------------------------------------------------- the pieces
def leaf():
    x, y = grid(*SIZES["leaf"])
    t = 1 - y                                                     # 0 at the stem (bottom), 1 at the tip (top)
    hw = 0.40 * np.sin(np.pi * np.clip(t, 0, 1) ** 0.75) * (0.2 + 0.8 * smooth(0.04, 0.3, t))
    u = np.abs(x * 2 - 1) * 0.5
    a = 1 - smooth(hw - 0.012, hw + 0.012, u)
    a *= smooth(0.0, 0.03, t) * (1 - smooth(0.96, 1.0, t))
    ax = np.abs(x * 2 - 1)
    vein = np.exp(-(ax / 0.06) ** 2) * smooth(0.05, 0.2, t) * (1 - smooth(0.8, 0.95, t))
    side = np.exp(-((ax - 0.9 * np.mod(t * 4.0, 1.0)) / 0.05) ** 2) * (ax < 0.7) * 0.6 * smooth(0.2, 0.3, t) * (1 - smooth(0.7, 0.8, t))
    grey = 1 - 0.38 * np.clip(vein + side, 0, 1)
    finish("leaf", np.clip(a, 0, 1), grey)


def droplet():
    x, y = grid(*SIZES["droplet"])
    w, h = SIZES["droplet"]
    px, py = (x - 0.5) * w, y * h                                  # pixels
    cy, r = h * 0.68, w * 0.4
    circle = np.sqrt(px ** 2 + (py - cy) ** 2) - r
    tip = 6.0                                                      # a cone from the point at the top to the circle
    k = np.clip((py - tip) / max(1e-6, (cy - tip)), 0, 1)
    cone = np.abs(px) - r * k ** 1.3
    d = np.minimum(circle, np.where(py < cy, cone, 1e9))
    a = 1 - smooth(-0.6, 0.6, d)
    shine = np.exp(-(((px + r * 0.35) ** 2 + (py - cy + r * 0.3) ** 2) / (r * 0.28) ** 2))
    grey = 0.8 + 0.2 * shine - 0.18 * smooth(0.0, 1.0, (px - r * 0.2) / r) * (py > cy)
    finish("droplet", np.clip(a, 0, 1), np.clip(grey, 0, 1))


def rock():
    im, d, W, H = canvas("rock")
    pts = [(0.5, 0.08), (0.86, 0.3), (0.92, 0.66), (0.62, 0.94), (0.22, 0.88), (0.08, 0.5), (0.2, 0.22)]
    d.polygon([(px * W, py * H) for px, py in pts], fill=255)
    a = arr(im, 0.15)
    x, y = grid(*SIZES["rock"])
    facet = 0.78 + 0.11 * np.sign(np.sin(x * 9 + y * 5) + 0.3 * np.sin(y * 13))
    grey = np.clip(facet - 0.25 * smooth(0.45, 1.0, y) + 0.12 * (1 - x), 0.35, 1)
    finish("rock", edge_fade(a, 1.2), grey)


def bubble():
    x, y = grid(*SIZES["bubble"])
    r, _ = polar(x, y)
    ring = np.exp(-((r - 0.82) / 0.07) ** 2)
    body = 0.16 * (1 - smooth(0.0, 0.86, r))
    hx, hy = (x * 2 - 1) + 0.38, (y * 2 - 1) + 0.4
    shine = np.exp(-((hx * hx + hy * hy * 2.5) / 0.03))
    a = np.clip(ring * 0.9 + body + shine, 0, 1) * (1 - smooth(0.9, 0.99, r))
    finish("bubble", np.clip(a, 0, 1))


def feather():
    x, y = grid(*SIZES["feather"])
    t = 1 - y
    u = (x * 2 - 1)
    hw = 0.8 * np.sin(np.pi * np.clip(t, 0, 1) ** 0.7) ** 0.8 * smooth(0.0, 0.08, t)
    body = 1 - smooth(hw - 0.02, hw + 0.02, np.abs(u))
    barbs = 0.5 + 0.5 * np.sin((t * 46.0 - np.abs(u) * 6.0))
    shaft = np.exp(-(u / 0.07) ** 2)
    a = np.clip(body * (0.7 + 0.3 * barbs) + shaft * 0.6, 0, 1)
    a *= smooth(0.0, 0.015, t) * (1 - smooth(0.97, 1.0, t))
    grey = 0.82 + 0.18 * barbs - 0.35 * shaft
    finish("feather", np.clip(a, 0, 1), np.clip(grey, 0, 1))


def rune():
    im, d, W, H = canvas("rune")
    lw = max(2, int(0.012 * W))
    for rr in (0.46, 0.34):
        d.ellipse([(0.5 - rr) * W, (0.5 - rr) * H, (0.5 + rr) * W, (0.5 + rr) * H], outline=255, width=lw)
    n = 20
    for k in range(n):                                              # angular glyphs between the two rings
        ang = 2 * math.pi * k / n
        c, s = math.cos(ang), math.sin(ang)
        tx, ty = -s, c
        r0, r1 = 0.355, 0.445
        kind = (k * 7 + 3) % 5
        mid = (r0 + r1) / 2
        if kind == 0:
            segs = [((c * r0, s * r0), (c * r1, s * r1))]
        elif kind == 1:
            segs = [((c * r0 + tx * 0.02, s * r0 + ty * 0.02), (c * r1 - tx * 0.02, s * r1 - ty * 0.02)), ((c * mid - tx * 0.025, s * mid - ty * 0.025), (c * mid + tx * 0.025, s * mid + ty * 0.025))]
        elif kind == 2:
            segs = [((c * r0 - tx * 0.02, s * r0 - ty * 0.02), (c * r1, s * r1)), ((c * r0 + tx * 0.02, s * r0 + ty * 0.02), (c * r1, s * r1))]
        elif kind == 3:
            segs = [((c * r0 - tx * 0.02, s * r0 - ty * 0.02), (c * mid + tx * 0.02, s * mid + ty * 0.02)), ((c * mid + tx * 0.02, s * mid + ty * 0.02), (c * r1 - tx * 0.02, s * r1 - ty * 0.02))]
        else:
            segs = [((c * r0 + tx * 0.025, s * r0 + ty * 0.025), (c * r0 - tx * 0.025, s * r0 - ty * 0.025)), ((c * r0, s * r0), (c * r1, s * r1)), ((c * r1 + tx * 0.025, s * r1 + ty * 0.025), (c * r1 - tx * 0.025, s * r1 - ty * 0.025))]
        for (ax, ay), (bx, by) in segs:
            d.line([((0.5 + ax) * W, (0.5 + ay) * H), ((0.5 + bx) * W, (0.5 + by) * H)], fill=255, width=lw)
    for k in range(2):                                              # a hexagram inside
        pts = [(0.5 + 0.28 * math.cos(-math.pi / 2 + k * math.pi / 3 + j * 2 * math.pi / 3), 0.5 + 0.28 * math.sin(-math.pi / 2 + k * math.pi / 3 + j * 2 * math.pi / 3)) for j in range(4)]
        d.line([(px * W, py * H) for px, py in pts], fill=255, width=lw, joint="curve")
    finish("rune", round_clear(arr(im, 0.25), "rune"))


def claw():
    im, d, W, H = canvas("claw")
    for k in range(3):                                              # three rake marks, tapering to both ends, a little apart
        ox = (k - 1) * 0.24
        top, bot = (0.5 + ox + 0.18, 0.04), (0.5 + ox - 0.18, 0.96)
        dx, dy = bot[0] - top[0], bot[1] - top[1]
        length = math.hypot(dx, dy)
        nx, ny = -dy / length, dx / length
        left, right = [], []
        for i in range(21):
            t = i / 20
            w = 0.075 * math.sin(math.pi * t) ** 0.8 * (0.85 + 0.15 * k)
            cx, cy = top[0] + dx * t, top[1] + dy * t
            left.append(((cx + nx * w) * W, (cy + ny * w) * H))
            right.append(((cx - nx * w) * W, (cy - ny * w) * H))
        d.polygon(left + right[::-1], fill=255)
    finish("claw", edge_fade(arr(im, 0.12), 1.2))


def card():
    im, d, W, H = canvas("card")
    lw = max(2, int(0.035 * W))
    d.rounded_rectangle([0.06 * W, 0.04 * H, 0.94 * W, 0.96 * H], radius=0.09 * W, outline=255, width=lw)
    d.rounded_rectangle([0.16 * W, 0.12 * H, 0.84 * W, 0.88 * H], radius=0.05 * W, outline=200, width=max(1, lw // 2))
    cx, cy = 0.5 * W, 0.5 * H
    pts = []
    for k in range(10):
        ang = -math.pi / 2 + k * math.pi / 5
        rad = (0.24 * W) if k % 2 == 0 else (0.1 * W)
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    d.polygon(pts, fill=255)
    for sy in (0.2, 0.8):
        d.ellipse([cx - 0.04 * W, sy * H - 0.04 * W, cx + 0.04 * W, sy * H + 0.04 * W], fill=255)
    x, y = grid(*SIZES["card"])
    a = arr(im, 0.12)
    face = ((x > 0.08) & (x < 0.92) & (y > 0.05) & (y < 0.95)) * 0.22
    finish("card", edge_fade(np.maximum(a, face), 1.0))


def eye():
    im, d, W, H = canvas("eye")
    lw = max(2, int(0.045 * H))
    top = [(0.04 * W + (0.92 * W) * t / 24, 0.5 * H - 0.34 * H * math.sin(math.pi * t / 24) ** 0.9) for t in range(25)]
    bot = [(0.04 * W + (0.92 * W) * t / 24, 0.5 * H + 0.28 * H * math.sin(math.pi * t / 24) ** 0.9) for t in range(25)]
    d.line(top, fill=255, width=lw, joint="curve")
    d.line(bot, fill=255, width=lw, joint="curve")
    cx, cy, r = 0.5 * W, 0.5 * H, 0.2 * H
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=255, width=lw)
    d.ellipse([cx - r * 0.45, cy - r * 0.45, cx + r * 0.45, cy + r * 0.45], fill=255)
    finish("eye", edge_fade(arr(im, 0.2), 1.5))


def crescent():
    x, y = grid(*SIZES["crescent"])
    dx, dy = x * 2 - 1, y * 2 - 1
    r1 = np.sqrt(dx * dx + dy * dy)
    r2 = np.sqrt((dx - 0.34) ** 2 + dy ** 2)
    a = (1 - smooth(0.9, 0.93, r1)) * smooth(0.64, 0.67, r2)              # a big disc minus a shifted one: a thin crescent opening to the +x side
    bright = 0.65 + 0.35 * smooth(0.55, 0.0, np.abs(dy))
    finish("crescent", np.clip(a, 0, 1) * (1 - smooth(0.93, 0.99, r1)), bright)


def hexcell():
    im, d, W, H = canvas("hex")
    lw = max(2, int(0.012 * W))

    def hexagon(cx, cy, rad, width):
        pts = [(cx + rad * math.cos(math.pi / 6 + k * math.pi / 3), cy + rad * math.sin(math.pi / 6 + k * math.pi / 3)) for k in range(6)]
        d.polygon([(px * W, py * H) for px, py in pts], outline=255, width=width)

    hexagon(0.5, 0.5, 0.46, lw * 2)
    hexagon(0.5, 0.5, 0.2, lw)
    for k in range(6):                                              # the six cells round the middle one
        ang = k * math.pi / 3
        hexagon(0.5 + 0.345 * math.cos(ang), 0.5 + 0.345 * math.sin(ang), 0.115, lw)
    a = arr(im, 0.2)
    x, y = grid(*SIZES["hex"])
    r, _ = polar(x, y)
    a = np.maximum(a, 0.12 * (1 - smooth(0.4, 0.46, r)))
    finish("hex", round_clear(a, "hex"))


def stitch():
    x, y = grid(*SIZES["stitch"])
    u = np.abs(x * 2 - 1)
    cell = np.mod(y * 8.0, 1.0)
    dash = smooth(0.08, 0.16, cell) * (1 - smooth(0.74, 0.82, cell))
    core = np.exp(-(u / 0.22) ** 2)
    a = core * (0.25 + 0.75 * dash) * smooth(0.0, 0.04, y) * (1 - smooth(0.96, 1.0, y))
    a = np.where(u > 0.95, 0, a)
    finish("stitch", np.clip(a, 0, 1))


def note():
    im, d, W, H = canvas("note")
    cx, cy = 0.36 * W, 0.78 * H
    rx, ry = 0.22 * W, 0.15 * H
    d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=255)
    d.line([(cx + rx * 0.85, cy), (cx + rx * 0.85, 0.12 * H)], fill=255, width=max(2, int(0.07 * W)))
    d.polygon([(cx + rx * 0.85, 0.12 * H), (0.9 * W, 0.3 * H), (0.88 * W, 0.46 * H), (cx + rx * 0.85, 0.3 * H)], fill=255)
    finish("note", edge_fade(arr(im, 0.15), 1.2))


def puff():
    x, y = grid(*SIZES["puff"])
    a = np.zeros_like(x)
    for cx, cy, r in [(0.5, 0.5, 0.26), (0.28, 0.58, 0.19), (0.72, 0.58, 0.19), (0.38, 0.34, 0.19), (0.62, 0.34, 0.19), (0.5, 0.74, 0.17)]:
        d = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)
        a = np.maximum(a, 1 - smooth(r - 0.012, r + 0.012, d))
    shade = 0.78 + 0.22 * smooth(0.9, 0.2, y)
    finish("puff", np.clip(a, 0, 1), shade)


def facet():
    im, d, W, H = canvas("facet")
    pts = [(0.5, 0.04), (0.9, 0.38), (0.72, 0.96), (0.28, 0.96), (0.1, 0.38)]
    d.polygon([(px * W, py * H) for px, py in pts], fill=255)
    a = arr(im, 0.1)
    x, y = grid(*SIZES["facet"])
    f = np.floor((x * 3.1 + y * 2.3) * 1.5 + 0.4 * np.sin(y * 7)) % 3
    grey = 0.55 + 0.22 * f + 0.2 * smooth(0.5, 0.0, y)
    edge = np.abs(np.mod((x * 3.1 + y * 2.3) * 1.5, 1.0)) < 0.04
    grey = np.where(edge, 1.0, grey)
    finish("facet", edge_fade(a, 1.2), np.clip(grey, 0, 1))


MAKERS = {"leaf": leaf, "droplet": droplet, "rock": rock, "bubble": bubble, "feather": feather, "rune": rune, "claw": claw, "card": card, "eye": eye,
          "crescent": crescent, "hex": hexcell, "stitch": stitch, "note": note, "puff": puff, "facet": facet}

if __name__ == "__main__":
    assert set(MAKERS) == set(SIZES)
    for name in SIZES:
        MAKERS[name]()

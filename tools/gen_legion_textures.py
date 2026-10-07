"""Generates the Legion Magic VFX textures (chess-piece soldiers, the circular board, the crimson rising halo, the grimoire page).

Deterministic (fixed seeds, no fonts). Output: src/main/resources/assets/nusmp/textures/particle/legion_*.png

    python3 -B tools/gen_legion_textures.py

Look reference (owner art, Gehenna Game): dozens of translucent pink-white glass chess pieces (pawns with striped, ringed bodies)
standing on a round black-and-white checkerboard, every piece wrapped in a crimson halo that rises off it like cold flame, and an open
grimoire page of handwritten lines glowing red. Sprites are white / grey with alpha so the vertex colour tints them.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def save(name, arr):
    arr = np.clip(arr, 0, 1)
    Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA").save(os.path.join(OUT, name + ".png"))


def rgba(grey, alpha):
    g = np.clip(grey, 0, 1)
    return np.dstack([g, g, g, np.clip(alpha, 0, 1)])


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, dtype=np.float32) / 255.0


def noise(w, h, cell, seed, octaves=3):
    rng = np.random.default_rng(seed)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        cw, ch = max(2, w // max(1, cell >> o)), max(2, h // max(1, cell >> o))
        g = rng.random((ch, cw)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8), "L").resize((w, h), Image.BICUBIC)
        out += amp * (np.asarray(im, np.float32) / 255.0)
        tot += amp
        amp *= 0.5
    return out / tot


def mask_from_polys(w, h, polys, circles=()):
    im = Image.new("L", (w * SS, h * SS), 0)
    d = ImageDraw.Draw(im)
    for p in polys:
        d.polygon([(x * w * SS, y * h * SS) for x, y in p], fill=255)
    for cx, cy, r in circles:
        d.ellipse([(cx - r) * w * SS, (cy - r * w / h) * h * SS, (cx + r) * w * SS, (cy + r * w / h) * h * SS], fill=255)
    return np.asarray(im.resize((w, h), Image.LANCZOS), np.float32) / 255.0


def mirror(pts):
    """Half profile (x from the axis 0.5 outwards, top to bottom) -> closed polygon."""
    left = [(1 - x, y) for x, y in reversed(pts)]
    return pts + left


# ---------------------------------------------------------------------------------------------------- pieces (128 x 256)
PIECES = {
    "pawn": dict(poly=mirror([(0.5, 0.05), (0.60, 0.07), (0.66, 0.14), (0.66, 0.21), (0.60, 0.27), (0.56, 0.29), (0.67, 0.32), (0.67, 0.36),
                              (0.57, 0.38), (0.60, 0.50), (0.64, 0.62), (0.70, 0.76), (0.80, 0.84), (0.80, 0.90), (0.86, 0.93),
                              (0.86, 0.97), (0.5, 0.97)]), bands=[0.42, 0.52, 0.62, 0.72, 0.82]),
    "rook": dict(poly=mirror([(0.5, 0.06), (0.52, 0.06), (0.52, 0.12), (0.58, 0.12), (0.58, 0.06), (0.66, 0.06), (0.66, 0.12),
                              (0.72, 0.12), (0.72, 0.06), (0.80, 0.06), (0.80, 0.24), (0.72, 0.28), (0.72, 0.31), (0.66, 0.34),
                              (0.66, 0.66), (0.72, 0.72), (0.72, 0.78), (0.82, 0.84), (0.82, 0.90), (0.88, 0.93), (0.88, 0.97), (0.5, 0.97)]),
                 bands=[0.40, 0.50, 0.60]),
    "knight": dict(poly=[(0.20, 0.97), (0.82, 0.97), (0.82, 0.91), (0.74, 0.86), (0.72, 0.70), (0.70, 0.52), (0.66, 0.38), (0.62, 0.26),
                         (0.58, 0.14), (0.53, 0.05), (0.47, 0.13), (0.40, 0.09), (0.38, 0.20), (0.30, 0.28), (0.16, 0.40), (0.10, 0.49),
                         (0.15, 0.56), (0.27, 0.53), (0.37, 0.50), (0.43, 0.56), (0.35, 0.70), (0.29, 0.86), (0.20, 0.91)], bands=[0.9]),
    "queen": dict(poly=mirror([(0.5, 0.03), (0.54, 0.07), (0.54, 0.10), (0.60, 0.13), (0.62, 0.20), (0.74, 0.12), (0.72, 0.28), (0.64, 0.33),
                               (0.60, 0.36), (0.66, 0.38), (0.62, 0.44), (0.64, 0.58), (0.70, 0.72), (0.82, 0.84), (0.82, 0.90),
                               (0.88, 0.93), (0.88, 0.97), (0.5, 0.97)]), bands=[0.45, 0.58, 0.70]),
}


def piece(name, seed):
    w, h = 128, 256
    spec = PIECES[name]
    m = mask_from_polys(w, h, [spec["poly"]])
    inner = blur(m, 7)
    vol = np.clip(inner * 1.25, 0, 1) ** 0.8            # rounded, glassy volume
    ys = np.linspace(0, 1, h)[:, None] * np.ones((1, w), np.float32)
    xs = np.ones((h, 1), np.float32) * np.linspace(0, 1, w)[None, :]
    stripe = np.zeros((h, w), np.float32)
    for b in spec["bands"]:
        stripe += np.exp(-((ys - b) / 0.012) ** 2) * 0.7 + np.exp(-((ys - b - 0.022) / 0.007) ** 2) * 0.45   # dark groove + bright lip
    n = noise(w, h, 24, seed) * 0.25
    # light from the upper left: bright rim and a soft pink core, like the glass soldiers in the anime
    rim = np.clip(m - blur(m, 2.2) * 1.0, 0, 1) * 2.4
    lit = 0.50 + 0.38 * (1 - xs) * vol + 0.18 * np.exp(-((xs - 0.38) / 0.1) ** 2) * vol + n * 0.2
    grey = lit * (1 - 0.55 * np.clip(stripe, 0, 1)) + rim * 0.45
    alpha = m * (0.62 + 0.30 * vol) + rim * 0.35
    # a hot spot in the head
    head = np.exp(-(((xs - 0.5) / 0.14) ** 2 + ((ys - 0.15) / 0.07) ** 2)) * m
    grey = grey + head * 0.35
    save("legion_%s" % name, rgba(grey, alpha))


# ---------------------------------------------------------------------------------------------------- board (256^2)
def board():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    cx = cy = (n - 1) / 2
    dx, dy = (xx - cx) / (n / 2), (yy - cy) / (n / 2)
    r = np.sqrt(dx * dx + dy * dy)
    a = (np.arctan2(dy, dx) + math.pi) / (2 * math.pi)
    rings, sect = 5, 24
    ri = np.clip(((r - 0.12) / 0.74 * rings).astype(int), 0, rings - 1)
    si = (a * sect).astype(int) % sect
    light = ((ri + si) % 2 == 0).astype(np.float32)
    inside = ((r > 0.12) & (r < 0.86)).astype(np.float32)
    # thin grout lines between squares
    fr = ((r - 0.12) / 0.74 * rings) % 1.0
    fs = (a * sect) % 1.0
    grout = np.maximum(np.exp(-(np.minimum(fr, 1 - fr) / 0.05) ** 2), np.exp(-(np.minimum(fs, 1 - fs) * (r * 2 * math.pi / sect * 120) / 3.0) ** 2))
    grey = np.where(light > 0.5, 0.92, 0.10) * (1 - 0.35 * grout) + 0.0
    alpha = inside * np.where(light > 0.5, 0.96, 0.88)
    # polished sheen
    sheen = np.exp(-((dx + dy * 0.6 + 0.35) / 0.18) ** 2) * 0.18
    grey = grey + sheen * light * inside
    # rim: a thick gold-white band with engraved ticks and a bevel
    rim = ((r >= 0.86) & (r < 0.99)).astype(np.float32)
    tick = (np.sin(a * 2 * math.pi * 48) > 0.55).astype(np.float32) * ((r > 0.93) & (r < 0.975))
    bev = np.exp(-((r - 0.875) / 0.012) ** 2) + np.exp(-((r - 0.975) / 0.01) ** 2)
    rg = 0.78 - 0.35 * tick + 0.25 * bev + 0.1 * np.exp(-((dx + dy + 0.5) / 0.3) ** 2)
    grey = np.where(rim > 0, rg, grey)
    alpha = np.where(rim > 0, 1.0, alpha)
    hub = (r <= 0.12)
    grey = np.where(hub, 0.55 + 0.4 * np.exp(-(r / 0.07) ** 2), grey)
    alpha = np.where(hub, 0.9, alpha)
    edge = np.clip((0.995 - r) / 0.012, 0, 1)
    save("legion_board", rgba(grey, alpha * edge))


# ---------------------------------------------------------------------------------------------------- ring with chess notation ticks (256^2)
def ring():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    dx, dy = (xx - (n - 1) / 2) / (n / 2), (yy - (n - 1) / 2) / (n / 2)
    r = np.sqrt(dx * dx + dy * dy)
    a = (np.arctan2(dy, dx) + math.pi) / (2 * math.pi)
    line = np.exp(-((r - 0.93) / 0.012) ** 2) + 0.55 * np.exp(-((r - 0.86) / 0.008) ** 2) + 0.4 * np.exp(-((r - 0.97) / 0.007) ** 2)
    # diamond studs every 1/16, small ticks between
    d = np.abs(((a * 16) % 1.0) - 0.5)
    ang_dist = d / 16 * 2 * math.pi * 0.9
    diam = np.clip(1 - (np.abs(ang_dist * 1.0) * 1.0 + np.abs(r - 0.90)) / 0.045, 0, 1) ** 1.2
    tick = ((np.sin(a * 2 * math.pi * 64) > 0.7) & (r > 0.87) & (r < 0.915)).astype(np.float32) * 0.6
    # inner band of chess-square dashes
    dash = ((np.floor(a * 48) % 2 == 0) & (r > 0.79) & (r < 0.83)).astype(np.float32) * 0.7
    v = np.clip(line + diam * 1.2 + tick + dash, 0, 1)
    soft = blur(v, 1.2)
    edge = np.clip((0.995 - r) / 0.01, 0, 1)
    save("legion_ring", rgba(np.clip(0.55 + v * 0.5, 0, 1), np.clip(soft * 1.3, 0, 1) * edge))


# ---------------------------------------------------------------------------------------------------- crimson halo curtain (64 x 128)
def halo():
    w, h = 64, 128
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = xx / (w - 1), yy / (h - 1)             # v = 0 top, 1 bottom
    cols = np.zeros((h, w), np.float32)
    rng = np.random.default_rng(31)
    for i in range(7):                             # soft rising tongues, tilted a little, narrowing upward
        x0 = 0.1 + 0.8 * rng.random()
        wd = 0.05 + 0.07 * rng.random()
        tilt = (rng.random() - 0.5) * 0.25
        xc = x0 + tilt * (1 - v) + 0.04 * np.sin((1 - v) * 9 + i * 1.7)
        taper = wd * (0.35 + 0.65 * v)
        tongue = np.exp(-((u - xc) / taper) ** 2) * (v ** 0.8) * (0.55 + 0.45 * rng.random())
        cols += tongue
    n = noise(w, h, 16, 7) * 0.5 + 0.5
    base = np.clip(cols * (0.7 + 0.6 * n), 0, 1)
    ground = np.exp(-((1 - v) / 0.1) ** 2) * 0.5
    a = np.clip(base + ground * 0.6, 0, 1) * np.clip(v * 2.2, 0, 1) ** 0.7 * np.clip((1 - u) * 20, 0, 1) * np.clip(u * 20, 0, 1)
    grey = np.clip(0.55 + base * 0.6, 0, 1)
    save("legion_halo", rgba(grey, a))


# ---------------------------------------------------------------------------------------------------- grimoire page (128^2)
def page():
    n = 128
    rng = np.random.default_rng(5)
    im = Image.new("L", (n * SS, n * SS), 0)
    d = ImageDraw.Draw(im)
    # a slightly skewed open sheet
    sheet = [(0.10, 0.12), (0.92, 0.06), (0.94, 0.90), (0.08, 0.94)]
    d.polygon([(x * n * SS, y * n * SS) for x, y in sheet], fill=255)
    m = np.asarray(im.resize((n, n), Image.LANCZOS), np.float32) / 255.0
    ink = Image.new("L", (n * SS, n * SS), 0)
    di = ImageDraw.Draw(ink)
    for row in range(11):
        y = 0.20 + row * 0.060
        x = 0.17
        end = 0.82 - (0.25 if row in (4, 9) else 0) * rng.random()
        while x < end:
            wl = 0.015 + 0.03 * rng.random()
            amp = 0.012 * rng.random()
            pts = [((x + wl * k / 6) * n * SS, (y + (0.5 - rng.random()) * amp * 1.4 + 0.012 * math.sin(k * 1.9)) * n * SS) for k in range(7)]
            di.line(pts, fill=255, width=SS)
            x += wl + 0.015
    ink = np.asarray(ink.resize((n, n), Image.LANCZOS), np.float32) / 255.0
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    paper = 0.92 - 0.12 * noise(n, n, 16, 3) - 0.1 * np.exp(-((xx / n - 0.5) / 0.03) ** 2)   # fold crease
    rim = np.clip(m - blur(m, 3.0), 0, 1) * 2.0
    grey = np.clip(paper - ink * 0.65 + rim * 0.3, 0, 1)
    save("legion_page", rgba(grey, np.clip(m + rim * 0.2, 0, 1)))


# ---------------------------------------------------------------------------------------------------- diamond glint (64^2)
def diamond():
    n = 64
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    dx, dy = (xx - 31.5) / 32, (yy - 31.5) / 32
    r = np.sqrt(dx * dx + dy * dy) + 1e-4
    cross = np.exp(-(np.minimum(abs(dx), abs(dy)) / 0.035) ** 2) * np.exp(-r * 2.2)
    diag = np.exp(-(np.minimum(abs(dx + dy), abs(dx - dy)) / 0.08) ** 2) * np.exp(-r * 4.5) * 0.5
    dia = np.clip(1 - (abs(dx) + abs(dy)) / 0.42, 0, 1) ** 0.7                 # the rhombus (a checker square seen on edge)
    core = np.exp(-(r / 0.12) ** 2)
    v = np.clip(cross + diag + dia * 0.9 + core, 0, 1)
    save("legion_diamond", rgba(np.clip(0.6 + v * 0.5, 0, 1), v))


# ---------------------------------------------------------------------------------------------------- flight streak (64 x 256), V runs along the length
def streak():
    w, h = 64, 256
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (xx - 31.5) / 32, yy / (h - 1)
    spine = np.exp(-(u / 0.14) ** 2)
    glowb = np.exp(-(u / 0.55) ** 2) * 0.45
    # chevrons of checker squares along the trail
    chev = np.clip(1 - np.abs(((v * 9 + np.abs(u) * 1.1) % 1.0) - 0.5) * 6, 0, 1) * np.exp(-(u / 0.7) ** 2) * 0.55
    sq = ((np.floor((u + 1) * 3) + np.floor(v * 24)) % 2 == 0) * np.exp(-(np.abs(u) / 0.8) ** 4) * 0.25
    n = noise(w, h, 24, 11)
    a = np.clip((spine + glowb + chev + sq) * (0.75 + 0.5 * n), 0, 1) * np.clip(v * 8, 0, 1) ** 0.5 * np.clip((1 - v) * 4, 0, 1) ** 0.5
    save("legion_streak", rgba(np.clip(0.55 + spine * 0.5, 0, 1), a))


# ---------------------------------------------------------------------------------------------------- shatter rays (256^2): square shards burst
def rays():
    n = 256
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    dx, dy = (xx - 127.5) / 128, (yy - 127.5) / 128
    r = np.sqrt(dx * dx + dy * dy) + 1e-4
    a = (np.arctan2(dy, dx) + math.pi) / (2 * math.pi)
    rng = np.random.default_rng(77)
    acc = np.zeros((n, n), np.float32)
    for i in range(18):
        ang = (i + rng.random() * 0.4) / 18
        wd = 0.004 + 0.012 * rng.random()
        ln = 0.45 + 0.5 * rng.random()
        da = np.minimum(abs(a - ang), 1 - abs(a - ang)) * 2 * math.pi * r
        acc += np.exp(-(da / (wd * 3 * (1 - 0.6 * r / ln))) ** 2) * np.clip((ln - r) / 0.25, 0, 1) * np.clip(r * 8, 0, 1)
    core = np.exp(-(r / 0.16) ** 2)
    ringv = np.exp(-((r - 0.62) / 0.02) ** 2) * 0.5
    v = np.clip(acc * 0.9 + core + ringv, 0, 1)
    save("legion_rays", rgba(np.clip(0.6 + v * 0.45, 0, 1), v * np.clip((0.99 - r) / 0.05, 0, 1)))


def main():
    os.makedirs(OUT, exist_ok=True)
    for i, nm in enumerate(PIECES):
        piece(nm, 100 + i)
    board()
    ring()
    halo()
    page()
    diamond()
    streak()
    rays()
    print("legion textures written to", os.path.normpath(OUT))


if __name__ == "__main__":
    main()

"""Generates the Mana Zone and Grand Magic Zone VFX textures (vfx/client/layer/ManaZoneLayer).

Deterministic (fixed seeds, no fonts): every machine writes the same pixels.
Output: src/main/resources/assets/nusmp/textures/particle/mana_zone_*.png and grand_zone_*.png

    python3 -B tools/gen_mana_zone_textures.py

All textures are near-white with the shape in the alpha channel and in the brightness, so the vertex colour (the caster's grimoire colour,
or a zone's tint) tints them. Look: the Mana Zone is a faintly lit lattice of ambient mana (a diamond lattice on a sphere shell, rune bands
on the floor, hexagonal footholds in the air); the Grand Magic Zones are drawn from rocks, spores, heat haze, currents, bolts and veins.
The Time Magic textures are the quality bar and are not touched.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


def save(arr_or_im, name):
    if isinstance(arr_or_im, np.ndarray):
        a = np.clip(arr_or_im, 0, 1)
        arr_or_im = Image.fromarray((a * 255 + 0.5).astype(np.uint8), "RGBA")
    os.makedirs(OUT, exist_ok=True)
    arr_or_im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, arr_or_im.size)


def rgba(light, alpha):
    """Grey 'light' (0..1) and 'alpha' (0..1) planes to an RGBA float array."""
    a = np.clip(alpha, 0, 1)
    l = np.clip(light, 0, 1) * np.ones_like(a)
    return np.dstack([l, l, l, a])


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def gauss(x, s):
    return np.exp(-(x / s) ** 2)


def periodic_noise(w, h, seed, octaves=4, base=3):
    """Tileable value noise built from random periodic sine fields (integer frequencies, so the tile wraps)."""
    rng = np.random.default_rng(seed)
    xx, yy = grid(w, h)
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        f = base * (2 ** o)
        for _ in range(3):
            fx, fy = rng.integers(1, f + 1), rng.integers(0, f + 1)
            ph = rng.uniform(0, 2 * math.pi)
            out += amp * np.sin(2 * math.pi * (fx * xx / w + fy * yy / h) + ph)
            tot += amp
        amp *= 0.5
    return out / tot


# ---------------------------------------------------------------------------------------------------- the zone shell (sphere)
def shell(size=256):
    """Tileable diamond lattice of ambient mana, brighter nodes, drifting mottling; faintly translucent so the dome reads as air."""
    xx, yy = grid(size, size)
    P = size / 4.0
    d1 = np.abs(np.sin(math.pi * (xx + yy) / P))      # distance-like to the 45 degree lines
    d2 = np.abs(np.sin(math.pi * (xx - yy) / P))
    line = np.exp(-((d1 / 0.10) ** 2)) + np.exp(-((d2 / 0.10) ** 2))
    node = np.exp(-((d1 / 0.16) ** 2) - ((d2 / 0.16) ** 2))
    mott = 0.5 + 0.5 * periodic_noise(size, size, 11, 4, 2)
    alpha = 0.07 + 0.10 * mott + 0.34 * np.clip(line, 0, 1) * (0.55 + 0.45 * mott) + 0.50 * node
    light = 0.70 + 0.30 * node + 0.10 * mott
    save(rgba(light, alpha), "mana_zone_shell")


# ---------------------------------------------------------------------------------------------------- the silhouette of the shell
def rim(size=256):
    """The sphere seen from outside as one round sprite: nearly clear in the middle, a bright thin edge, a soft glow just inside it.
    The shell mesh is inscribed in it, so the outline of the zone is a true circle at any distance."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    n = 0.5 + 0.5 * periodic_noise(size, size, 41, 3, 2)
    a = 0.025 * r ** 2 + 0.60 * gauss(r - 0.985, 0.014) + 0.30 * gauss(r - 0.93, 0.07) * (0.6 + 0.4 * n) + 0.10 * gauss(r - 0.80, 0.12) * n
    a *= np.clip((1.0 - r) / 0.012, 0, 1) * (r < 1.0)
    save(rgba(0.88 + 0.12 * gauss(r - 0.985, 0.03), a), "mana_zone_rim")


# ---------------------------------------------------------------------------------------------------- floor ring (rune band)
def _glyph(d, x, y, h, kind, col):
    """A small geometric rune centred at (x, y), height h, on an ImageDraw (supersampled)."""
    w = max(2, int(h * 0.09))
    r = h * 0.38
    if kind == 0:     # circle with a dot
        d.ellipse([x - r, y - r, x + r, y + r], outline=col, width=w)
        d.ellipse([x - w, y - w, x + w, y + w], fill=col)
    elif kind == 1:   # triangle
        d.polygon([(x, y - r), (x + r * 0.95, y + r * 0.7), (x - r * 0.95, y + r * 0.7)], outline=col, width=w)
    elif kind == 2:   # crossed bars
        d.line([(x - r, y - r), (x + r, y + r)], fill=col, width=w)
        d.line([(x - r, y + r), (x + r, y - r)], fill=col, width=w)
        d.line([(x, y - r), (x, y + r)], fill=col, width=w)
    elif kind == 3:   # diamond with a spine
        d.polygon([(x, y - r), (x + r * 0.6, y), (x, y + r), (x - r * 0.6, y)], outline=col, width=w)
        d.line([(x, y - r * 1.15), (x, y + r * 1.15)], fill=col, width=w)
    elif kind == 4:   # crescent
        d.arc([x - r, y - r, x + r, y + r], 40, 320, fill=col, width=w)
        d.line([(x + r * 0.2, y - r * 0.2), (x + r * 0.9, y - r * 0.2)], fill=col, width=w)
    else:             # three ticks
        for k in (-1, 0, 1):
            d.line([(x + k * r * 0.6, y - r * (0.9 if k == 0 else 0.6)), (x + k * r * 0.6, y + r * (0.9 if k == 0 else 0.6))], fill=col, width=w)


def ring(w=512, h=64):
    """Tileable runic ribbon: double rails top and bottom, a string of runes between, tick marks on the rails."""
    W, H = w * SS, h * SS
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    white = (255, 255, 255, 255)
    soft = (255, 255, 255, 150)
    for y, wd, col in ((H * 0.08, SS * 3, white), (H * 0.17, SS * 1.3, soft), (H * 0.83, SS * 1.3, soft), (H * 0.92, SS * 3, white)):
        d.line([(0, y), (W, y)], fill=col, width=int(wd))
    n_ticks = 64
    for i in range(n_ticks):
        x = W * (i + 0.5) / n_ticks
        d.line([(x, H * 0.17), (x, H * 0.23)], fill=soft, width=SS)
        d.line([(x, H * 0.77), (x, H * 0.83)], fill=soft, width=SS)
    runes = 16
    for i in range(runes):
        x = W * (i + 0.5) / runes
        _glyph(d, x, H * 0.5, H * 0.42, i % 6, white)
        d.line([(x + W / runes * 0.5, H * 0.38), (x + W / runes * 0.5, H * 0.62)], fill=soft, width=SS)
    im = im.resize((w, h), Image.LANCZOS)
    glow = im.filter(ImageFilter.GaussianBlur(1.6))
    a = np.asarray(im, np.float32) / 255.0
    g = np.asarray(glow, np.float32) / 255.0
    alpha = np.clip(a[..., 3] + 0.55 * g[..., 3], 0, 1)
    save(rgba(0.85 + 0.15 * a[..., 3], alpha), "mana_zone_ring")


# ---------------------------------------------------------------------------------------------------- floor disc
def floor(size=256):
    """Concentric rings, radial ticks and six faint arcs: the disc that lies under the whole zone. Very low alpha, brighter at the rim."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    dx, dy = (xx - c) / c, (yy - c) / c
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    a = 0.05 * (r < 1) * (0.5 + 0.5 * r)
    for rr, wd, k in ((0.97, 0.010, 0.55), (0.90, 0.004, 0.30), (0.62, 0.004, 0.25), (0.34, 0.004, 0.22)):
        a += k * gauss(r - rr, wd)
    ticks = np.abs(np.sin(th * 36 / 2))
    a += 0.30 * gauss(r - 0.935, 0.035) * (ticks > 0.97)
    for k in range(6):                                          # six arcs between the rings (a hexagram's sweep)
        t0 = k * math.pi / 3
        dd = np.abs(((th - t0 + math.pi) % (2 * math.pi)) - math.pi)
        a += 0.35 * gauss(r - 0.76, 0.006) * (dd < 0.33)
    a *= (r < 1.0)
    a *= np.clip((1.0 - r) / 0.02, 0, 1)
    save(rgba(0.9 + 0.1 * r, a), "mana_zone_floor")


# ---------------------------------------------------------------------------------------------------- foothold pad (mid-air)
def pad(size=128):
    """A hexagonal foothold of pressed mana: outer and inner hexagon, a six-point star, a soft core."""
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2
    white, soft = (255, 255, 255, 255), (255, 255, 255, 140)

    def hexagon(r):
        return [(c + math.cos(math.radians(60 * k + 30)) * r, c + math.sin(math.radians(60 * k + 30)) * r) for k in range(6)]

    d.polygon(hexagon(c * 0.93), outline=white, width=SS * 3)
    d.polygon(hexagon(c * 0.80), outline=soft, width=int(SS * 1.5))
    d.polygon(hexagon(c * 0.50), outline=white, width=SS * 2)
    for k in range(6):
        a = math.radians(60 * k + 30)
        d.line([(c + math.cos(a) * c * 0.50, c + math.sin(a) * c * 0.50), (c + math.cos(a) * c * 0.80, c + math.sin(a) * c * 0.80)], fill=soft, width=SS * 2)
    star = []
    for k in range(12):
        a = math.radians(30 * k - 90)
        rr = c * (0.34 if k % 2 == 0 else 0.15)
        star.append((c + math.cos(a) * rr, c + math.sin(a) * rr))
    d.polygon(star, fill=(255, 255, 255, 235))
    im = im.resize((size, size), Image.LANCZOS)
    a = np.asarray(im, np.float32) / 255.0
    xx, yy = grid(size, size)
    r = np.sqrt(((xx - size / 2) / (size / 2)) ** 2 + ((yy - size / 2) / (size / 2)) ** 2)
    core = 0.30 * np.exp(-(r / 0.55) ** 2)
    glow = np.asarray(im.filter(ImageFilter.GaussianBlur(3)), np.float32)[..., 3] / 255.0
    alpha = np.clip(a[..., 3] + core * (r < 0.95) + 0.5 * glow, 0, 1)
    save(rgba(0.88 + 0.12 * a[..., 3], alpha), "mana_zone_pad")


# ---------------------------------------------------------------------------------------------------- flow streak
def flow(w=32, h=256):
    """A streak of drifting mana: bright head at v=1 (top of the texture), long soft tail below."""
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = 1.0 - yy / (h - 1)                                       # 1 at the top row
    core = np.exp(-(u / (0.10 + 0.14 * (1 - v))) ** 2)           # a thin head widening into the tail
    tail = v ** 1.7
    head = np.exp(-((1.0 - v) / 0.05) ** 2) * np.exp(-(u / 0.24) ** 2) * 0.6
    edge = np.clip((1.0 - v) / 0.015, 0, 1)                      # the very top row is clear
    a = (core * tail + head) * edge
    save(rgba(0.80 + 0.20 * v, a), "mana_zone_flow")


# ---------------------------------------------------------------------------------------------------- mote
def mote(size=64):
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    dx, dy = (xx - c) / c, (yy - c) / c
    r = np.sqrt(dx * dx + dy * dy)
    core = np.exp(-(r / 0.16) ** 2)
    halo = 0.55 * np.exp(-(r / 0.42) ** 2)
    cross = 0.8 * (gauss(dx, 0.035) * gauss(dy, 0.55) + gauss(dy, 0.035) * gauss(dx, 0.55))
    diag = 0.35 * (gauss((dx - dy) / 1.414, 0.03) * gauss((dx + dy) / 1.414, 0.30) + gauss((dx + dy) / 1.414, 0.03) * gauss((dx - dy) / 1.414, 0.30))
    a = np.clip(core + halo * 0.7 + cross + diag, 0, 1) * np.clip((1.0 - r) / 0.12, 0, 1)
    save(rgba(0.85 + 0.15 * core, a), "mana_zone_mote")


# ---------------------------------------------------------------------------------------------------- perception ping
def ping(size=128):
    """The mark the caster's senses put on a living thing: corner brackets, a ring, a core; reads through walls."""
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2
    white, soft = (255, 255, 255, 255), (255, 255, 255, 120)
    r = c * 0.78
    d.ellipse([c - r, c - r, c + r, c + r], outline=soft, width=SS * 2)
    L, T = c * 0.62, c * 0.30
    for sx in (-1, 1):
        for sy in (-1, 1):
            x, y = c + sx * L, c + sy * L
            d.line([(x, y), (x - sx * T, y)], fill=white, width=SS * 3)
            d.line([(x, y), (x, y - sy * T)], fill=white, width=SS * 3)
    d.ellipse([c - c * 0.07, c - c * 0.07, c + c * 0.07, c + c * 0.07], fill=white)
    im = im.resize((size, size), Image.LANCZOS)
    a = np.asarray(im, np.float32) / 255.0
    glow = np.asarray(im.filter(ImageFilter.GaussianBlur(2.2)), np.float32)[..., 3] / 255.0
    save(rgba(0.9 + 0.1 * a[..., 3], np.clip(a[..., 3] + 0.5 * glow, 0, 1)), "mana_zone_ping")


# ---------------------------------------------------------------------------------------------------- burst ring
def burst(size=256):
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    dx, dy = (xx - c) / c, (yy - c) / c
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    ringv = gauss(r - 0.86, 0.035) * 1.0 + gauss(r - 0.80, 0.012) * 0.6
    inner = 0.18 * np.clip(r / 0.86, 0, 1) ** 2 * (r < 0.86)
    spokes = (0.5 + 0.5 * np.cos(th * 24)) ** 8 * gauss(r - 0.70, 0.17) * 0.45
    a = (ringv + inner + spokes) * np.clip((1.0 - r) / 0.10, 0, 1) * (r < 1.0)
    save(rgba(0.85 + 0.15 * ringv, a), "mana_zone_burst")


# ---------------------------------------------------------------------------------------------------- grand zone textures
def boulder(size=128):
    """A floating rock: noisy outline, lit from the top left, dark cracks with a thin seam of mana light, a pale underside rim."""
    rng = np.random.default_rng(7)
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    dx, dy = (xx - c) / c, (yy - c) / c
    r = np.sqrt(dx * dx + dy * dy)
    th = np.arctan2(dy, dx)
    wob = sum(rng.uniform(0.04, 0.10) / k * np.sin(th * k + rng.uniform(0, 6.28)) for k in (2, 3, 5, 7, 11))
    edge = 0.82 + wob
    inside = np.clip((edge - r) / 0.025, 0, 1)
    nx, ny = dx / np.maximum(edge, 0.2), dy / np.maximum(edge, 0.2)
    nz = np.sqrt(np.clip(1 - nx * nx - ny * ny, 0, 1))
    lit = np.clip(0.30 + 0.62 * (-0.55 * nx - 0.65 * ny + 0.55 * nz), 0, 1)
    grain = 0.5 + 0.5 * periodic_noise(size, size, 5, 4, 3)
    cracks = np.zeros_like(r)
    for k in range(3):
        a0 = rng.uniform(0, 6.28)
        x0, y0 = rng.uniform(-0.4, 0.4, 2)
        for t in range(22):
            a0 += rng.normal(0, 0.16)
            x0 += math.cos(a0) * 0.028
            y0 += math.sin(a0) * 0.028
            cracks += np.exp(-(((dx - x0) ** 2 + (dy - y0) ** 2) / 0.00022)) * (1.0 - t / 26.0)
    cracks = np.clip(cracks, 0, 1)
    light = lit * (0.55 + 0.45 * grain) * (1 - 0.65 * cracks) + 0.35 * cracks * (1 - lit)
    rim = np.exp(-(((edge - r) / 0.04) ** 2)) * np.clip(ny, 0, 1)             # the mana-lit underside
    light = np.clip(light + 0.5 * rim, 0, 1)
    save(rgba(light, inside), "grand_zone_boulder")


def spore(size=64):
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    a = np.clip(np.exp(-(r / 0.22) ** 2) * 1.0 + 0.55 * np.exp(-(r / 0.55) ** 2), 0, 1) * np.clip((1 - r) / 0.15, 0, 1)
    save(rgba(0.8 + 0.2 * np.exp(-(r / 0.2) ** 2), a), "grand_zone_spore")


def haze(w=64, h=256):
    """Heat shimmer: tall wavering translucent ribbons, fading at both ends."""
    xx, yy = grid(w, h)
    u = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / (h - 1)
    wave = 0.35 * np.sin(v * 9.0 + 0.5) + 0.2 * np.sin(v * 23.0 + 1.3)
    a = gauss(u - wave * 0.55, 0.30) * 0.5 + gauss(u + wave * 0.4 + 0.35, 0.12) * 0.35
    a *= np.clip(np.sin(np.clip(v, 0, 1) * math.pi), 0, 1) ** 1.2
    save(rgba(0.9, a * 0.8), "grand_zone_haze")


def current(w=256, h=64):
    """A violent current: long bright streaks with foam flecks, tileable along u."""
    rng = np.random.default_rng(23)
    xx, yy = grid(w, h)
    a = np.zeros((h, w), np.float32)
    for _ in range(14):
        y0 = rng.uniform(0.1, 0.9) * h
        x0 = rng.uniform(0, w)
        ln = rng.uniform(0.25, 0.7) * w
        th = rng.uniform(0.6, 2.0)
        amp = rng.uniform(0.15, 0.5)
        for off in (-w, 0, w):                                   # wrap so it tiles
            dxx = (xx - (x0 + off))
            along = np.clip(dxx / ln, -0.5, 1.5)
            body = np.exp(-(((yy - y0 - np.sin(dxx / 28.0) * 2.0) / th) ** 2)) * np.exp(-((dxx - ln / 2) / (ln * 0.55)) ** 2)
            a += amp * body
    foam = (rng.random((h, w)) > 0.985).astype(np.float32)
    foam = np.asarray(Image.fromarray((foam * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.1)), np.float32) / 255.0 * 6
    a = np.clip(a + foam * 0.5, 0, 1)
    a *= np.clip(np.sin(np.clip(yy / (h - 1), 0, 1) * math.pi), 0, 1) ** 0.8
    save(rgba(0.8 + 0.2 * a, a * 0.9), "grand_zone_current")


def bolt(w=64, h=256):
    """A vertical mana bolt: jagged core, hot white centre, forks, soft halo."""
    rng = np.random.default_rng(31)
    S = 4
    W, H = w * S, h * S
    im = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(im)

    def walk(x, y, ang, steps, width):
        pts = [(x, y)]
        for _ in range(steps):
            ang += rng.normal(0, 0.35)
            ang = 0.5 * ang + 0.5 * (math.pi / 2)                # pulled towards straight down
            x += math.cos(ang) * H / 40
            y += math.sin(ang) * H / 40
            pts.append((x, y))
        d.line(pts, fill=255, width=int(width))
        return pts

    main = walk(W / 2, 0, math.pi / 2, 40, 3 * S)
    for i in (9, 17, 26):                                        # forks
        x, y = main[i]
        walk(x, y, math.pi / 2 + rng.choice([-0.8, 0.8]), 10, 1.6 * S)
    core = np.asarray(im.resize((w, h), Image.LANCZOS), np.float32) / 255.0
    halo = np.asarray(im.resize((w, h), Image.LANCZOS).filter(ImageFilter.GaussianBlur(3.2)), np.float32) / 255.0
    a = np.clip(core + 0.9 * halo, 0, 1)
    save(rgba(0.75 + 0.25 * core, a), "grand_zone_bolt")


def vein(size=256):
    """Mana-flow lines in the ground: tileable ridged noise, thin branching veins with brighter junctions. Drawn flat, scrolled slowly."""
    n1 = periodic_noise(size, size, 3, 4, 3)
    n2 = periodic_noise(size, size, 17, 3, 5)
    ridge = 1.0 - np.abs(n1)
    ridge2 = 1.0 - np.abs(n2)
    line = np.clip((ridge - 0.925) / 0.075, 0, 1) ** 1.5
    fine = np.clip((ridge2 - 0.93) / 0.07, 0, 1) ** 1.5 * 0.55
    glow = np.asarray(Image.fromarray((np.clip(line + fine, 0, 1) * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.5)), np.float32) / 255.0
    a = np.clip(line + fine + 0.55 * glow, 0, 1)
    save(rgba(0.8 + 0.2 * line, a * 0.85), "grand_zone_vein")


def halo(size=128):
    """A big soft disc for sky tint patches and storm glows."""
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    r = np.sqrt(((xx - c) / c) ** 2 + ((yy - c) / c) ** 2)
    a = np.clip(1 - r, 0, 1) ** 1.6
    save(rgba(0.9, a * 0.7), "grand_zone_halo")


def main():
    shell()
    rim()
    ring()
    floor()
    pad()
    flow()
    mote()
    ping()
    burst()
    boulder()
    spore()
    haze()
    current()
    bolt()
    vein()
    halo()


if __name__ == "__main__":
    main()

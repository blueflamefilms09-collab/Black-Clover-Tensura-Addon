"""Textures for Barrier Magic: refractive honeycomb glass. Deterministic, Pillow + numpy only.

    python3 -B tools/gen_barrier_textures.py  ->  src/main/resources/assets/nusmp/textures/particle/barrier_*.png

    barrier_hex_cell   one bevelled hexagonal pane: bright rim, faint refractive body, glossy sheen
    barrier_hex_grid   a honeycomb pane, cells lit at random, vignetted (walls, floors, plates)
    barrier_hex_band   a seamless strip of honeycomb along U (wraps; the dome wall)
    barrier_shard      a faceted glass shard (debris)
    barrier_sigil      ground sigil: rings, ticks, a hex lattice and two counter-rotating triangles
    barrier_flare      four-point lens glint with halo
    barrier_beam       a beam across U: white-hot core, hexagon chevrons running along V
    barrier_crack      radial crack web of shattered glass
    barrier_orb        a glass sphere silhouette: fresnel rim, honeycomb interior, specular highlight
    barrier_ring       a thin pulse ring with fine ticks
Convention (as the rest of textures/particle): greyscale + alpha, tinted per quad by the vertex colour.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm, grey, save  # noqa: E402

SQ3 = math.sqrt(3.0)


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im).astype(np.float32) / 255


def grid(w, h):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return x, y


def hexcells(x, y, cols, w):
    """Honeycomb lattice: x,y pixel arrays; 'cols' cell columns across w pixels. Returns (edge 0..0.5 distance to the cell border,
    cell id hash 0..1)."""
    s = cols / float(w)
    px, py = x * s, y * s
    rx, ry = 1.0, SQ3
    ax = np.mod(px, rx) - rx / 2
    ay = np.mod(py, ry) - ry / 2
    bx = np.mod(px - rx / 2, rx) - rx / 2
    by = np.mod(py - ry / 2, ry) - ry / 2
    use_a = (ax * ax + ay * ay) < (bx * bx + by * by)
    gx, gy = np.where(use_a, ax, bx), np.where(use_a, ay, by)
    d = np.maximum(np.abs(gx) * 0.5 + np.abs(gy) * SQ3 / 2, np.abs(gx))
    cx = np.where(use_a, np.floor(px / rx), np.floor((px - rx / 2) / rx) + 0.5)
    cy = np.where(use_a, np.floor(py / ry), np.floor((py - ry / 2) / ry) + 0.5)
    hsh = np.mod(np.sin(cx * 127.1 + cy * 311.7) * 43758.5453, 1.0)
    return 0.5 - d, hsh.astype(np.float32)


def polygon_mask(n, size, radius, rot, ss=4):
    big = size * ss
    im = Image.new("L", (big, big), 0)
    pts = [(big / 2 + radius * ss * math.cos(rot + k * math.tau / n), big / 2 + radius * ss * math.sin(rot + k * math.tau / n)) for k in range(n)]
    ImageDraw.Draw(im).polygon(pts, fill=255)
    return np.asarray(im.resize((size, size), Image.LANCZOS)).astype(np.float32) / 255


def hex_cell():
    S = 256
    x, y = grid(S, S)
    outer = polygon_mask(6, S, 118, math.pi / 6)
    inner = polygon_mask(6, S, 98, math.pi / 6)
    core = polygon_mask(6, S, 78, math.pi / 6)
    rim = np.clip(outer - inner, 0, 1)
    # bevel: top-left edges lit, bottom-right in shade
    gx = blur(outer, 6)
    gy, gxx = np.gradient(gx)
    light = np.clip(0.55 + (-gxx - gy) * 9.0, 0.25, 1.0)
    n1 = fbm(S, S, 48, 11, 4)
    n2 = fbm(S, S, 12, 12, 3)
    body = inner * (0.10 + 0.16 * n1) + core * 0.08 * n2
    # refractive swirl bands inside
    swirl = 0.5 + 0.5 * np.sin((x * 0.5 + y * 0.8) / 9.0 + n1 * 9.0)
    body += inner * 0.07 * swirl
    # glossy diagonal sheen
    sheen = smooth(0.0, 1.0, 1 - np.abs((x + y) / (2.0 * S) - 0.36) * 9.0) * inner
    sheen2 = smooth(0.0, 1.0, 1 - np.abs((x + y) / (2.0 * S) - 0.5) * 22.0) * inner
    a = rim * (0.75 + 0.25 * light) + body + sheen * 0.22 + sheen2 * 0.28
    # inner hairline
    hl = np.clip(core - polygon_mask(6, S, 74, math.pi / 6), 0, 1)
    a += hl * 0.35
    g = 0.7 + 0.3 * light
    # corner glints
    glint = np.zeros((S, S), np.float32)
    for k in range(6):
        ang = math.pi / 6 + k * math.tau / 6
        cx, cy = S / 2 + 118 * math.cos(ang), S / 2 + 118 * math.sin(ang)
        glint += np.exp(-((x - cx) ** 2 + (y - cy) ** 2) / 40.0) * (0.9 if k in (3, 4) else 0.35)
    a += glint * outer
    save(grey(np.clip(a, 0, 1), np.clip(g + glint * 0.3, 0, 1)), "barrier_hex_cell")


def hex_grid():
    S = 256
    x, y = grid(S, S)
    e, h = hexcells(x, y, 6, S)
    line = 1 - smooth(0.012, 0.045, e)
    halo = 1 - smooth(0.0, 0.16, e)
    lit = smooth(0.62, 1.0, h)
    fill = 0.05 + 0.5 * lit * smooth(0.02, 0.22, e) * (0.6 + 0.4 * fbm(S, S, 24, 21, 3))
    dx, dy = (x - S / 2) / (S / 2), (y - S / 2) / (S / 2)
    vig = 1 - smooth(0.55, 1.0, np.sqrt(dx * dx + dy * dy))
    a = (line * 0.95 + halo * 0.16 + fill) * vig
    sheen = smooth(0, 1, 1 - np.abs((x + y) / (2.0 * S) - 0.5) * 7.0) * 0.1
    a += sheen * vig
    save(grey(np.clip(a, 0, 1), 0.8 + 0.2 * lit), "barrier_hex_grid")


def hex_band():
    W, H = 256, 64
    x, y = grid(W, H)
    e, h = hexcells(x, y + 6, 8, W)
    line = 1 - smooth(0.014, 0.05, e)
    halo = 1 - smooth(0.0, 0.14, e)
    lit = smooth(0.55, 1.0, h)
    fill = (0.07 + 0.42 * lit) * smooth(0.02, 0.2, e)
    v = y / (H - 1.0)
    edge = smooth(0.0, 0.22, v) * smooth(0.0, 0.22, 1 - v)
    bright = np.exp(-((v - 0.5) / 0.1) ** 2) * 0.22
    a = (line * 0.9 + halo * 0.14 + fill) * edge + bright * 0.4
    top = np.exp(-(v / 0.05) ** 2) * 0.5 + np.exp(-((1 - v) / 0.05) ** 2) * 0.5
    a += top * 0.6
    save(grey(np.clip(a, 0, 1), 0.82 + 0.18 * lit), "barrier_hex_band")


def shard():
    S = 128
    ss = 4
    big = S * ss
    rng = np.random.default_rng(31)
    im = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(im)
    pts = [(big * 0.5, big * 0.04), (big * 0.74, big * 0.46), (big * 0.62, big * 0.96), (big * 0.34, big * 0.78), (big * 0.26, big * 0.38)]
    d.polygon(pts, fill=255)
    mask = np.asarray(im.resize((S, S), Image.LANCZOS)).astype(np.float32) / 255
    # facets: fan from a point, each a different shade
    fx, fy = S * 0.5, S * 0.42
    x, y = grid(S, S)
    ang = np.arctan2(y - fy, x - fx)
    facet = np.floor((ang + math.pi) / math.tau * 7)
    shade = (np.mod(np.sin(facet * 12.9898 + 4.1) * 43758.5453, 1.0)).astype(np.float32)
    # edges between facets
    fr = np.mod((ang + math.pi) / math.tau * 7, 1.0)
    seam = 1 - smooth(0.0, 0.06, np.minimum(fr, 1 - fr))
    seam *= smooth(0.0, 12.0, np.sqrt((x - fx) ** 2 + (y - fy) ** 2))
    edge = np.clip(mask - polygon_mask_pts(S, pts, big, 0.88), 0, 1)
    a = mask * (0.35 + 0.4 * shade) + seam * 0.35 * mask + edge * 0.9
    g = 0.6 + 0.4 * shade
    save(grey(np.clip(a, 0, 1), np.clip(g + edge, 0, 1)), "barrier_shard")


def polygon_mask_pts(S, pts, big, k):
    cx, cy = big / 2, big / 2
    pts2 = [(cx + (px - cx) * k, cy + (py - cy) * k) for px, py in pts]
    im = Image.new("L", (big, big), 0)
    ImageDraw.Draw(im).polygon(pts2, fill=255)
    return np.asarray(im.resize((S, S), Image.LANCZOS)).astype(np.float32) / 255


def sigil():
    S = 256
    ss = 3
    big = S * ss
    im = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(im)
    c = big / 2

    def ring(r, w, v=255):
        d.ellipse((c - r * ss, c - r * ss, c + r * ss, c + r * ss), outline=v, width=max(1, int(w * ss)))

    ring(124, 3)
    ring(116, 1)
    ring(98, 2)
    ring(60, 1.5)
    ring(30, 2)
    # outer ticks
    for k in range(72):
        a = k * math.tau / 72
        r0, r1 = (104, 112) if k % 6 == 0 else (106, 110)
        d.line((c + r0 * ss * math.cos(a), c + r0 * ss * math.sin(a), c + r1 * ss * math.cos(a), c + r1 * ss * math.sin(a)), fill=255, width=ss * (2 if k % 6 == 0 else 1))
    # two triangles (hexagram) and hexagon
    for rot in (-math.pi / 2, math.pi / 2):
        pts = [(c + 98 * ss * math.cos(rot + k * math.tau / 3), c + 98 * ss * math.sin(rot + k * math.tau / 3)) for k in range(3)]
        d.polygon(pts, outline=255, width=2 * ss)
    hexp = [(c + 60 * ss * math.cos(k * math.tau / 6), c + 60 * ss * math.sin(k * math.tau / 6)) for k in range(6)]
    d.polygon(hexp, outline=255, width=2 * ss)
    # small hexagon cells on the triangle tips
    for k in range(6):
        a = -math.pi / 2 + k * math.tau / 6
        px, py = c + 98 * ss * math.cos(a), c + 98 * ss * math.sin(a)
        pts = [(px + 9 * ss * math.cos(j * math.tau / 6), py + 9 * ss * math.sin(j * math.tau / 6)) for j in range(6)]
        d.polygon(pts, outline=255, width=ss)
    # spokes
    for k in range(6):
        a = k * math.tau / 6
        d.line((c + 30 * ss * math.cos(a), c + 30 * ss * math.sin(a), c + 60 * ss * math.cos(a), c + 60 * ss * math.sin(a)), fill=200, width=ss)
    line = np.asarray(im.resize((S, S), Image.LANCZOS)).astype(np.float32) / 255
    x, y = grid(S, S)
    r = np.sqrt((x - S / 2) ** 2 + (y - S / 2) ** 2)
    e, h = hexcells(x, y, 14, S)
    lat = (1 - smooth(0.01, 0.04, e)) * 0.22 * (1 - smooth(52, 60, r)) * smooth(30, 36, r)
    floor = (1 - smooth(60, 124, r)) * 0.07
    a = line + blur(line, 2.5) * 0.7 + lat + floor
    a *= 1 - smooth(122, 128, r)
    save(grey(np.clip(a, 0, 1), 0.85 + 0.15 * line), "barrier_sigil")


def flare():
    S = 128
    x, y = grid(S, S)
    dx, dy = (x - S / 2) / (S / 2), (y - S / 2) / (S / 2)
    r = np.sqrt(dx * dx + dy * dy) + 1e-4
    core = np.exp(-(r / 0.09) ** 2)
    halo = np.exp(-(r / 0.35) ** 2) * 0.45
    v = np.exp(-(np.abs(dx) / 0.025)) * np.exp(-np.abs(dy) / 0.55) * 1.0
    hz = np.exp(-(np.abs(dy) / 0.025)) * np.exp(-np.abs(dx) / 0.55) * 1.0
    d1 = np.exp(-(np.abs(dx - dy) / 0.07)) * np.exp(-r / 0.25) * 0.35
    d2 = np.exp(-(np.abs(dx + dy) / 0.07)) * np.exp(-r / 0.25) * 0.35
    ringa = np.exp(-((r - 0.62) / 0.025) ** 2) * 0.25
    a = (core + halo + v + hz + d1 + d2 + ringa) * (1 - smooth(0.85, 1.0, r))
    save(grey(np.clip(a, 0, 1)), "barrier_flare")


def beam():
    W, H = 64, 256
    x, y = grid(W, H)
    u = (x + 0.5) / W * 2 - 1           # -1..1 across
    cs = np.exp(-(u / 0.14) ** 2)       # white core
    body = np.exp(-(np.abs(u) / 0.55) ** 2) * 0.55
    # hexagon chevrons running down the beam
    v = y / 64.0
    chev = np.abs(np.mod(v + np.abs(u) * 0.9, 1.0) - 0.5)
    ch = (1 - smooth(0.02, 0.07, chev)) * smooth(1.0, 0.25, np.abs(u)) * 0.55
    ribs = np.exp(-(np.abs(np.mod(v * 2, 1.0) - 0.5) / 0.04) ** 2) * np.exp(-(np.abs(u) / 0.7) ** 2) * 0.3
    n = fbm(W, H, 16, 41, 3)
    a = cs + body * (0.8 + 0.4 * n) + ch + ribs
    a *= 1 - smooth(0.9, 1.0, np.abs(u))
    save(grey(np.clip(a, 0, 1), 0.8 + 0.2 * cs), "barrier_beam")


def crack():
    S = 256
    ss = 3
    big = S * ss
    rng = np.random.default_rng(55)
    im = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(im)
    c = big / 2

    def walk(x, y, ang, length, w, depth):
        px, py = x, y
        steps = int(length / 6)
        for _ in range(steps):
            ang += rng.normal(0, 0.13)
            nx, ny = px + 6 * math.cos(ang), py + 6 * math.sin(ang)
            d.line((px, py, nx, ny), fill=255, width=max(1, int(w)))
            px, py = nx, ny
            if depth < 2 and rng.random() < 0.07:
                walk(px, py, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.1), length * 0.45, max(1, w * 0.6), depth + 1)
            if px < 0 or py < 0 or px > big or py > big:
                break
        return

    for k in range(11):
        walk(c, c, k * math.tau / 11 + rng.uniform(-0.2, 0.2), 330 * ss / 3 * 0.95, 3.2, 0)
    # concentric stress rings, broken
    for r in (24, 52, 84, 108):
        for k in range(24):
            if rng.random() < 0.55:
                a0 = k * math.tau / 24
                d.arc((c - r * ss, c - r * ss, c + r * ss, c + r * ss), math.degrees(a0), math.degrees(a0 + math.tau / 24), fill=255, width=2)
    line = np.asarray(im.resize((S, S), Image.LANCZOS)).astype(np.float32) / 255
    x, y = grid(S, S)
    r = np.sqrt((x - S / 2) ** 2 + (y - S / 2) ** 2) / (S / 2)
    a = (line + blur(line, 2.0) * 0.8) * (1 - smooth(0.7, 1.0, r)) + np.exp(-(r / 0.08) ** 2) * 0.7
    save(grey(np.clip(a, 0, 1), 0.85 + 0.15 * line), "barrier_crack")


def orb():
    S = 256
    x, y = grid(S, S)
    dx, dy = (x - S / 2) / (S / 2), (y - S / 2) / (S / 2)
    r = np.sqrt(dx * dx + dy * dy)
    inside = 1 - smooth(0.96, 1.0, r)
    fres = smooth(0.55, 0.97, r) * inside
    rim = np.exp(-((r - 0.965) / 0.018) ** 2)
    e, h = hexcells(x, y, 9, S)
    # compress cell lattice near the limb for a spherical feel
    warp = 1 - smooth(0.0, 1.0, r ** 3) * 0.0
    line = (1 - smooth(0.012, 0.045, e)) * (1 - smooth(0.45, 0.97, r) * 0.4)
    lit = smooth(0.7, 1.0, h) * smooth(0.02, 0.2, e)
    n = fbm(S, S, 40, 61, 3)
    spec = np.exp(-(((dx + 0.42) ** 2 + (dy + 0.45) ** 2) / 0.012)) * 0.9
    spec += np.exp(-(((dx + 0.3) ** 2 + (dy + 0.55) ** 2) / 0.05)) * 0.25
    bounce = np.exp(-(((dx - 0.35) ** 2 + (dy - 0.55) ** 2) / 0.06)) * 0.18
    a = inside * (0.05 + 0.06 * n) + fres * 0.34 + rim * 0.9 + (line * 0.4 + lit * 0.28) * inside * (0.35 + 0.65 * smooth(0.2, 0.8, r)) + spec + bounce
    a *= inside
    save(grey(np.clip(a, 0, 1), 0.78 + 0.22 * np.clip(spec + rim, 0, 1)), "barrier_orb")


def pulse_ring():
    S = 256
    x, y = grid(S, S)
    r = np.sqrt((x - S / 2) ** 2 + (y - S / 2) ** 2) / (S / 2)
    ang = np.arctan2(y - S / 2, x - S / 2)
    core = np.exp(-((r - 0.9) / 0.018) ** 2)
    halo = np.exp(-((r - 0.9) / 0.07) ** 2) * 0.35
    inner = np.exp(-((r - 0.82) / 0.008) ** 2) * 0.5
    ticks = (np.abs(np.mod(ang * 36 / math.tau, 1.0) - 0.5) < 0.12) * np.exp(-((r - 0.855) / 0.022) ** 2) * 0.7
    tail = smooth(0.1, 0.88, r) * (1 - smooth(0.9, 0.99, r)) * 0.05
    a = core + halo + inner + ticks + tail
    a *= 1 - smooth(0.96, 1.0, r)
    save(grey(np.clip(a, 0, 1)), "barrier_ring")


if __name__ == "__main__":
    hex_cell()
    hex_grid()
    hex_band()
    shard()
    sigil()
    flare()
    beam()
    crack()
    orb()
    pulse_ring()

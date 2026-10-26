"""Generates the material atlas of the 3D models of "Solomon, Lord of Sorcery" (vfx.client.layer.SolomonModels / SolomonMeshDraw / SolomonLayer).

Deterministic and font-free (every machine writes the same pixels). Written for this mod, same palette as tools/gen_solomon_vfx_textures.py (deep indigo and violet marble, antique gold, parchment white, lilac glass and the
eight element sigils); it IMPORTS the line-art helpers of that script (Mask, runes, the element sigils), it does not change it. It only ADDS one file: the 17 flat textures of gen_solomon_vfx_textures.py stay and are still
drawn when the setting `solomonModels` is off.
Output: src/main/resources/assets/nusmp/textures/particle/solomon_model_atlas.png

    python tools/gen_solomon_model_textures.py

    solomon_model_atlas   8 x 8 cells of 128 px (1024 x 1024), alpha-blended, lit like marble / leather / steel from the upper left and shaded again by the layer per face (cells 0..39 are painted):
        0 violet marble                       1 dark marble plinth with a gold line      2 fluted shaft with a glowing rune column     3 gold moulding
        4 capital top with a glowing sigil    5 base ring of runes on the floor (cut out)   6..13 the sigil panels of the eight elements (flame, tide, gale, stone, storm, frost, light, dark)
        14 leather                            15 grimoire cover with the hexagram       16 grimoire back cover                        17 right page (script)       18 left page
        19 turning page (glowing script)      20 the cut edge of a book: leather over pages   21 silk ribbon                            22 spine with gold bands
        23 chain link slope with a glowing rune inlay    24 link inner slope                 25 / 26 / 27 amethyst facets: light, deep, glowing   28 seal disc of the chain head (cut out)
        29 signet band with engraved runes    30 polished gold band                    31 bezel gold                                32 rune halo (cut out)
        33 / 34 rune crystal facets (pale, tinted by the layer per element)             35 glass shard with a piece of a magic circle  36 shard back  37 chipped bright edge
        38 turning page 2 (glowing script)
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_solomon_vfx_textures as g   # the palette, the line-art mask, the runes and the element sigils of the flat sprites (imported, not changed)

S = 128
COLS = 8
CELLS = 39

INK = (22, 16, 48)
MARBLE = [(0.0, (74, 60, 134)), (0.45, (156, 138, 218)), (0.8, (216, 202, 248)), (1.0, (248, 240, 255))]
LEATHER = [(0.0, (36, 22, 78)), (0.5, (84, 52, 158)), (1.0, (150, 112, 236))]
STEEL = [(0.0, (70, 40, 24)), (0.5, (196, 138, 58)), (1.0, (255, 238, 178))]      # the chain: antique gold-bronze (it must read against violet light)
GOLDRAMP = [(0.0, (92, 56, 14)), (0.4, (200, 140, 44)), (0.75, (255, 214, 120)), (1.0, (255, 248, 214))]
PARCH = (250, 238, 208)
PARCH_D = (214, 192, 150)
INK_RUNE = (96, 60, 150)
GLASS = (206, 190, 255)


# ---------------------------------------------------------------------------------------------------- helpers
def col(c):
    return np.array(c, np.float32) / 255.0


def grid01(w=S, h=S):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


def vnoise(w, h, cell, seed):
    rs = np.random.RandomState(seed)
    gw, gh = max(2, int(math.ceil(w / cell)) + 2), max(2, int(math.ceil(h / cell)) + 2)
    base = (rs.rand(gh, gw) * 255).astype(np.uint8)
    im = Image.fromarray(base).resize((gw * cell, gh * cell), Image.BICUBIC)
    return np.asarray(im, dtype=np.float32)[:h, :w] / 255.0


def fbm(w, h, seed, cells=(32, 16, 8), gains=(0.5, 0.3, 0.2)):
    out = np.zeros((h, w), np.float32)
    for i, (c, k) in enumerate(zip(cells, gains)):
        out += vnoise(w, h, c, seed + i * 31) * k
    return out / sum(gains)


def ramp(t, stops):
    t = np.clip(t, 0, 1)
    out = np.zeros(t.shape + (3,), np.float32)
    for (t0, c0), (t1, c1) in zip(stops[:-1], stops[1:]):
        m = (t >= t0) & (t <= t1)
        u = ((t - t0) / (t1 - t0))[..., None]
        out = np.where(m[..., None], col(c0) * (1 - u) + col(c1) * u, out)
    return out


def lit(height, k=7.0, light=(-0.55, -0.62, 0.56), ambient=0.28):
    gy, gx = np.gradient(height)
    nx, ny, nz = -gx * k, -gy * k, np.ones_like(height)
    n = np.sqrt(nx * nx + ny * ny + nz * nz)
    L = np.array(light, np.float32)
    L /= np.linalg.norm(L)
    lam = np.clip((nx * L[0] + ny * L[1] + nz * L[2]) / n, 0, 1)
    return ambient + (1 - ambient) * lam, lam


def bevel(width=0.12, w=S, h=S):
    u, v = grid01(w, h)
    d = np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v))
    return g.smooth(0.0, width, d)


def over(rgb, colour, amount):
    a = np.clip(amount, 0, 1)[..., None]
    return rgb * (1 - a) + col(colour)[None, None, :] * a


def add_glow(rgb, mask, colour, gain=1.0):
    return np.clip(rgb + col(colour)[None, None, :] * (np.clip(mask, 0, 1) * gain)[..., None], 0, 1)


def stretched(aspect, fn, S_=256):
    """A line-art mask of a face whose width / height is {@code aspect}: the picture is drawn on a canvas of that shape (so a circle is a circle on the face) and squeezed into the square cell."""
    W, H = max(8, int(round(S_ * aspect))), S_
    m = g.Mask(W, H)
    fn(m, W, H)
    arr = np.clip(m.final(), 0, 1)
    im = Image.fromarray((arr * 255).astype(np.uint8)).resize((S, S), Image.LANCZOS)
    return np.asarray(im, dtype=np.float32) / 255.0


def blur(a, r):
    return g.blur(a, r)


def line_layer(rgb, mask, core, glow, glow_r=(1.4, 4.0), gain=(0.8, 0.4)):
    """Draws a mask as bright lines with a soft glow."""
    gl = np.zeros_like(mask)
    for r, k in zip(glow_r, gain):
        gl = gl + blur(mask, r) * k
    gl = np.clip(gl, 0, 1) * (1 - mask * 0.7)
    rgb = add_glow(rgb, gl, glow, 1.0)
    return over(rgb, core, mask)


def opaque(rgb):
    return np.clip(rgb, 1 / 255, 1), np.ones((S, S), np.float32)


# ---------------------------------------------------------------------------------------------------- the pillar
def marble(seed, dark=1.0, bevel_w=0.10, vein=0.5):
    n = fbm(S, S, seed)
    veins = np.abs(np.sin((fbm(S, S, seed + 7, cells=(48, 24)) * 7.0 + n * 2.0) * math.pi))
    veins = (1 - veins) ** 8
    h = bevel(bevel_w) * 0.7 + (n - 0.5) * 0.12
    t, lam = lit(h, k=6.0, ambient=0.38)
    rgb = ramp(np.clip(t * 0.78 + n * 0.16, 0, 1) * dark, MARBLE)
    rgb = over(rgb, (190, 170, 255), veins * vein * 0.55)
    return rgb


def gold_line(rgb, v_px, w_px, horizontal=True, strength=1.0):
    u, v = np.mgrid[0:S, 0:S].astype(np.float32) + 0.5
    d = np.abs((u if horizontal else v) - v_px)
    core = np.exp(-(d / (w_px * 0.55)) ** 2) * strength
    halo = np.exp(-(d / (w_px * 2.2)) ** 2) * 0.4 * strength
    rgb = over(rgb, g.GOLD_D, halo * 0.5)
    return over(rgb, g.GOLD, core)


def cell_stone():
    rgb = marble(3)
    for e in (6.0,):
        d = np.minimum(*[np.minimum(a, S - a) for a in np.mgrid[0:S, 0:S].astype(np.float32)])
        core = np.exp(-((d - e) / 1.5) ** 2)
        rgb = over(over(rgb, g.GOLD_D, np.exp(-((d - e) / 5.0) ** 2) * 0.35), g.GOLD, core)
    return opaque(rgb)


def cell_stone_dark():
    rgb = marble(11, dark=0.78)
    rgb = gold_line(rgb, 10, 3.2)
    rgb = gold_line(rgb, 18, 1.6, strength=0.7)
    low = g.smooth(104, 118, np.mgrid[0:S, 0:S][0].astype(np.float32) + 0.5)                   # a darker foot moulding
    rgb = rgb * (1 - 0.35 * low)[..., None]
    rgb = gold_line(rgb, 108, 2.0, strength=0.8)

    def fn(m, W, H):                                                                            # a row of small diamonds and tick marks between the two lines
        for i in range(9):
            x = W * (i + 0.5) / 9
            m.poly([(x, H * 0.43), (x + 9, H * 0.5), (x, H * 0.57), (x - 9, H * 0.5)], 2.2)
            m.disc(x + W / 18, H * 0.5, 2.6)
    mk = stretched(2.5, fn)
    rgb = line_layer(rgb, mk, g.LILAC, g.VIOLET, (1.4, 4.5), (0.8, 0.5))
    return opaque(rgb)


def cell_flute():
    u, v = grid01()
    n = fbm(S, S, 21)
    base_t = 0.42 + 0.20 * n
    flutes = 0.0
    for c in (0.14, 0.5, 0.86):                                                                  # three shallow flutes: dark groove edge, bright rib between
        flutes = flutes + np.exp(-((u - c) / 0.07) ** 2)
    ribs = 0.5 + 0.5 * np.cos(u * math.pi * 6 + 0.0)
    t = base_t + 0.20 * ribs - 0.26 * np.clip(flutes, 0, 1)
    edge = np.minimum(u, 1 - u)
    t = t * (0.55 + 0.45 * g.smooth(0.0, 0.10, edge))                                            # the corner of the octagon is darker
    rgb = ramp(np.clip(t, 0, 1), MARBLE)
    veins = (1 - np.abs(np.sin((fbm(S, S, 27, cells=(48, 24)) * 7.0 + n * 2.0) * math.pi))) ** 8
    rgb = over(rgb, (190, 170, 255), veins * 0.4)

    def fn(m, W, H):                                                                            # a column of runes down the middle, glowing
        rr = random.Random(14)
        for i in range(8):
            y = H * (i + 0.5) / 8
            g.draw_rune(m, g.make_rune(rr), W * 0.5, y, min(W * 0.55, 30), 0.0, 1.8)
    mk = stretched(0.27, fn)
    mk = np.clip(mk, 0, 1)
    rgb = line_layer(rgb, mk, g.GOLD, g.VIOLET, (1.2, 4.0), (0.9, 0.55))
    # bright, thin gold line at each side of the face (the arrises)
    for x in (4.5, S - 4.5):
        d = np.abs(np.mgrid[0:S, 0:S][1].astype(np.float32) + 0.5 - x)
        rgb = over(rgb, g.GOLD_D, np.exp(-(d / 1.3) ** 2) * 0.55)
    return opaque(rgb)


def cell_trim():
    h = bevel(0.2) * 0.8 + (fbm(S, S, 41) - 0.5) * 0.06
    t, lam = lit(h, k=8.0, ambient=0.3)
    rgb = ramp(t, GOLDRAMP)
    rgb = gold_line(rgb, 64, 2.4, strength=0.4)
    return opaque(rgb)


def cell_cap_top():
    rgb = marble(51, dark=0.62)
    cx = cy = S / 2

    def fn(m, W, H):
        c = W / 2
        m.circle(c, c, 0.86 * c, 3.2)
        m.circle(c, c, 0.68 * c, 1.8)
        m.circle(c, c, 0.40 * c, 2.0)
        g.hexagram(m, c, c, 0.4 * c, 2.0)
        for k in range(24):
            a = 2 * math.pi * k / 24
            m.line([(c + math.cos(a) * 0.72 * c, c + math.sin(a) * 0.72 * c), (c + math.cos(a) * 0.82 * c, c + math.sin(a) * 0.82 * c)], 1.8)
        m.disc(c, c, 0.07 * c)
    mk = stretched(1.0, fn)
    rgb = line_layer(rgb, mk, g.GOLD, g.GOLD_D, (1.6, 5.0), (0.9, 0.6))
    rr = np.sqrt((grid01()[0] - 0.5) ** 2 + (grid01()[1] - 0.5) ** 2) * 2
    rgb = add_glow(rgb, np.exp(-(rr / 0.3) ** 2) * 0.35, g.VIOLET, 1.0)
    rgb = gold_line(rgb, 5, 2.0, strength=0.9)
    rgb = gold_line(rgb, 5, 2.0, horizontal=False, strength=0.9)
    rgb = gold_line(rgb, S - 5, 2.0, strength=0.9)
    rgb = gold_line(rgb, S - 5, 2.0, horizontal=False, strength=0.9)
    return opaque(rgb)


def cell_base_ring():
    W = S
    c = W / 2

    def fn(m, W_, H_):
        c_ = W_ / 2
        m.circle(c_, c_, 0.94 * c_, 3.4)
        m.circle(c_, c_, 0.80 * c_, 1.8)
        m.circle(c_, c_, 0.50 * c_, 1.6)
        g.hexagram(m, c_, c_, 0.46 * c_, 1.8)
        rr = random.Random(5)
        g.rune_ring(m, c_, c_, 0.87 * c_, 20, 0.07 * c_ * 2, rr, 1.5)
    mk = stretched(1.0, fn)
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    disc = g.smooth(0.98, 0.9, r)
    rgb = np.zeros((S, S, 3), np.float32) + col((26, 18, 56))
    rgb = add_glow(rgb, np.exp(-(r / 0.5) ** 2) * 0.5, g.VIOLET, 1.0)
    rgb = line_layer(rgb, mk, g.GOLD, g.GOLD_D, (1.6, 5.0), (0.9, 0.6))
    a = np.clip(disc * 0.62 + mk * 0.95 + blur(mk, 3) * 0.6, 0, 1) * disc
    return np.clip(rgb, 1 / 255, 1), a


def cell_sigil(i):
    core, glow = g.ELEMENTS[i]
    rgb = marble(61 + i, dark=0.62)
    rgb = gold_line(rgb, 7, 2.6)
    rgb = gold_line(rgb, S - 7, 2.6)
    rgb = gold_line(rgb, 7, 2.6, horizontal=False)
    rgb = gold_line(rgb, S - 7, 2.6, horizontal=False)

    def fn(m, W, H):
        g.sigil(m, i, W / 2, H / 2, H * 0.40, 3.0)
    mk = stretched(1.65, fn)
    u, v = grid01()
    r = np.sqrt(((u - 0.5) * 1.65) ** 2 + (v - 0.5) ** 2)
    rgb = add_glow(rgb, np.exp(-(r / 0.28) ** 2) * 0.30, glow, 1.0)
    rgb = line_layer(rgb, mk, core, glow, (1.4, 4.6), (1.0, 0.7))
    return opaque(rgb)


# ---------------------------------------------------------------------------------------------------- the grimoire
def leather(seed, dark=1.0):
    n = fbm(S, S, seed, cells=(24, 12, 6), gains=(0.5, 0.3, 0.2))
    grain = 0.5 + 0.5 * np.sin(np.mgrid[0:S, 0:S][1].astype(np.float32) * 1.7 + n * 9.0) * np.sin(np.mgrid[0:S, 0:S][0].astype(np.float32) * 1.3 + n * 7.0)
    t = (0.45 + 0.25 * n + 0.12 * grain) * dark
    return ramp(t, LEATHER)


def border_frame(rgb, inset, w):
    u, v = np.mgrid[0:S, 0:S].astype(np.float32) + 0.5
    d = np.minimum(np.minimum(u, S - u), np.minimum(v, S - v))
    core = np.exp(-((d - inset) / (w * 0.55)) ** 2)
    rgb = over(rgb, g.GOLD_D, np.exp(-((d - inset) / (w * 2.2)) ** 2) * 0.35)
    return over(rgb, g.GOLD, core)


def cell_leather():
    rgb = leather(7)
    rgb = border_frame(rgb, 8, 2.4)
    return opaque(rgb)


def cell_cover_emblem():
    rgb = leather(17)
    rgb = border_frame(rgb, 9, 2.6)
    rgb = border_frame(rgb, 15, 1.4)

    def fn(m, W, H):
        c, y = W / 2, H * 0.42
        R = W * 0.36
        g.hexagram(m, c, y, R * 0.78, 3.0)
        m.circle(c, y, R, 3.0)
        m.circle(c, y, R * 0.66, 1.6)
        m.disc(c, y, 4)
        rr = random.Random(2)
        for k in range(6):
            a = -math.pi / 2 + math.pi / 3 * k + math.pi / 6
            g.draw_rune(m, g.make_rune(rr), c + math.cos(a) * R * 0.84, y + math.sin(a) * R * 0.84, 14, a + math.pi / 2, 1.7)
        for j, yy in enumerate((H * 0.72, H * 0.77, H * 0.82)):                                  # title bars
            m.line([(c - W * (0.27 - 0.05 * j), yy), (c + W * (0.27 - 0.05 * j), yy)], 2.0)
        for (px, py) in ((W * 0.13, H * 0.06), (W * 0.87, H * 0.06), (W * 0.13, H * 0.94), (W * 0.87, H * 0.94)):
            m.disc(px, py, 4.5)
    mk = stretched(0.46, fn)
    rgb = line_layer(rgb, mk, g.GOLD, g.GOLD_D, (1.4, 4.5), (0.9, 0.6))
    u, v = grid01()
    r = np.sqrt(((u - 0.5) * 0.46) ** 2 + (v - 0.42) ** 2)
    rgb = add_glow(rgb, np.exp(-(r / 0.16) ** 2) * 0.26, g.VIOLET, 1.0)
    return opaque(rgb)


def cell_cover_back():
    rgb = leather(27, dark=0.9)
    rgb = border_frame(rgb, 9, 2.6)

    def fn(m, W, H):
        c, y = W / 2, H / 2
        m.circle(c, y, W * 0.30, 2.4)
        m.circle(c, y, W * 0.20, 1.4)
        for k in range(6):
            a = math.pi / 3 * k
            m.disc(c + math.cos(a) * W * 0.30, y + math.sin(a) * W * 0.30, 4)
        for (px, py) in ((W * 0.13, H * 0.06), (W * 0.87, H * 0.06), (W * 0.13, H * 0.94), (W * 0.87, H * 0.94)):
            m.disc(px, py, 4.0)
    mk = stretched(0.46, fn)
    rgb = line_layer(rgb, mk, g.GOLD, g.GOLD_D, (1.4, 4.0), (0.8, 0.5))
    return opaque(rgb)


def parchment(seed, mirror=False, glow=False, margin=True):
    n = fbm(S, S, seed, cells=(32, 16, 8), gains=(0.5, 0.3, 0.2))
    u, v = grid01()
    base = col(PARCH)[None, None, :] * (0.92 + 0.10 * n[..., None])
    base = base * (0.9 + 0.1 * g.smooth(0.0, 0.5, (1 - u) if mirror else u))[..., None]       # the gutter (toward the spine) is darker
    edge = np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v))
    base = base * (0.84 + 0.16 * g.smooth(0.02, 0.2, edge))[..., None]
    base = over(base, PARCH_D, (1 - g.smooth(0.0, 0.07, edge)) * 0.45)

    def fn(m, W, H):
        rr = random.Random(seed)
        # a hexagram head and rows of small runes
        c = W / 2
        g.hexagram(m, c, H * 0.14, W * 0.15, 1.6)
        m.circle(c, H * 0.14, W * 0.19, 1.2)
        rows = 12
        for r_ in range(rows):
            y = H * (0.27 + 0.62 * r_ / rows)
            x = W * 0.15
            while x < W * 0.85:
                ln = rr.uniform(W * 0.06, W * 0.22)
                if rr.random() < 0.8:
                    m.line([(x, y), (min(x + ln, W * 0.85), y)], 1.8)
                    if rr.random() < 0.5:
                        g.draw_rune(m, g.make_rune(rr), x + ln * 0.5, y - 9, 11, 0.0, 1.3)
                x += ln + W * 0.05
    mk = stretched(0.46, fn)
    if mirror:
        mk = mk[:, ::-1]
    if glow:
        rgb = base * 1.02
        rgb = line_layer(rgb, mk, (255, 190, 70), (255, 150, 40), (1.6, 5.0), (1.0, 0.8))
        rgb = add_glow(rgb, np.exp(-(((u - 0.5) / 0.4) ** 2 + ((v - 0.5) / 0.55) ** 2)) * 0.12, (255, 200, 120), 1.0)
    else:
        rgb = over(base, INK_RUNE, mk * 0.9)
        rgb = add_glow(rgb, blur(mk, 2.5) * 0.25, g.GOLD_D, 1.0)
    if margin:                                                                                  # the cover shows round the pages: a leather border with a gold line
        d = edge
        lea = leather(seed + 3)
        rgb = rgb * g.smooth(0.045, 0.06, d)[..., None] + lea * (1 - g.smooth(0.045, 0.06, d))[..., None]
        core = np.exp(-((d - 0.04) / 0.008) ** 2)
        rgb = over(rgb, g.GOLD, core * 0.9)
    return opaque(rgb)


def cell_page():
    return parchment(101, mirror=False)


def cell_page_l():
    return parchment(103, mirror=True)


def cell_page_glow():
    return parchment(107, glow=True, margin=False)


def cell_page_glow_l():
    return parchment(109, glow=True, margin=False, mirror=True)


def cell_page_edge():
    u, v = grid01()
    n = fbm(S, S, 71, cells=(16, 8), gains=(0.6, 0.4))
    lea = leather(73)
    rgb = lea.copy()
    rgb = over(rgb, g.GOLD_D, np.exp(-((v - 0.05) / 0.02) ** 2) * 0.8)
    rgb = over(rgb, g.GOLD, np.exp(-((v - 0.44) / 0.012) ** 2) * 0.95)
    pages = 0.5 + 0.5 * np.sin((v - 0.46) * 130.0 + n * 3.0)
    pg = col(PARCH)[None, None, :] * (0.78 + 0.22 * pages[..., None])
    mask = g.smooth(0.46, 0.47, v)[..., None]
    rgb = rgb * (1 - mask) + pg * mask
    return opaque(rgb)


def cell_ribbon():
    u, v = grid01()
    n = fbm(S, S, 81, cells=(16, 8), gains=(0.6, 0.4))
    sheen = 0.6 + 0.3 * np.sin(v * 6.0 + n * 1.5)
    rgb = ramp(0.3 + 0.55 * sheen, [(0.0, (70, 40, 140)), (0.6, (138, 88, 236)), (1.0, (214, 192, 255))])
    for x in (0.12, 0.88):
        rgb = over(rgb, g.GOLD, np.exp(-((u - x) / 0.025) ** 2) * 0.9)
    diam = np.exp(-(np.abs(u - 0.5) / 0.12) ** 2) * (0.5 + 0.5 * np.cos((v * 3.0) * math.pi * 2)) ** 6
    rgb = over(rgb, g.GOLD, diam * 0.8)
    return opaque(rgb)


def cell_spine():
    u, v = grid01()
    rgb = leather(91, dark=0.85)
    for y in (0.07, 0.19, 0.5, 0.81, 0.93):
        rgb = over(rgb, g.GOLD, np.exp(-((v - y) / 0.012) ** 2) * 0.95)
        rgb = over(rgb, g.GOLD_D, np.exp(-((v - y) / 0.035) ** 2) * 0.35)
    for y in (0.34, 0.66):                                                                       # a small diamond between the bands
        d = np.abs(u - 0.5) * 1.6 + np.abs(v - y) * 1.0
        rgb = over(rgb, g.GOLD, g.smooth(0.07, 0.05, d) * 0.9)
    return opaque(rgb)


# ---------------------------------------------------------------------------------------------------- the chain
def steel(seed, dark=1.0, strength=1.0):
    u, v = grid01()
    n = fbm(S, S, seed)
    slope = np.clip(0.55 + 0.45 * np.cos((v - 0.18) * math.pi * 1.1), 0, 1)
    t = (0.30 + 0.38 * slope + 0.10 * (n - 0.5)) * dark
    rgb = ramp(t, STEEL)
    rgb = over(rgb, (255, 244, 205), np.exp(-((v - 0.16) / 0.06) ** 2) * 0.4 * strength)    # the bright edge along the ridge
    return rgb


def cell_link_top():
    u, v = grid01()
    rgb = steel(131)
    mid = 0.52
    core = np.exp(-((v - mid) / 0.075) ** 2)
    rgb = over(rgb, g.VIOLET, np.exp(-((v - mid) / 0.2) ** 2) * 0.45)
    rgb = over(rgb, (70, 40, 140), core * 0.8)
    # runes: short strokes across the inlay (tall in the texture, the face is long and narrow)
    rr = random.Random(9)
    m = g.Mask(S, S)
    for i in range(8):
        x = S * (i + 0.5) / 8
        rune = g.make_rune(rr)
        g.draw_rune(m, rune, x, S * mid, 38, 0.0, 2.6)
    mk = np.clip(m.final(), 0, 1)
    rgb = line_layer(rgb, mk, (255, 244, 214), g.VIOLET, (1.2, 4.0), (0.9, 0.6))
    rgb = add_glow(rgb, blur(core, 3) * 0.2, g.LILAC, 1.0)
    return opaque(rgb)


def cell_link_side():
    u, v = grid01()
    rgb = steel(137, dark=0.7, strength=0.5)
    rgb = over(rgb, g.GOLD_D, np.exp(-((v - 0.82) / 0.03) ** 2) * 0.7)
    rgb = over(rgb, g.VIOLET, np.exp(-((v - 0.5) / 0.02) ** 2) * (0.5 + 0.5 * np.sin(u * 60.0)) * 0.5)
    return opaque(rgb)


def facet(seed, light, deep, glowc, gain, rays=0):
    u, v = grid01()
    n = fbm(S, S, seed, cells=(24, 12), gains=(0.6, 0.4))
    t = 0.18 + 0.58 * v + 0.14 * (n - 0.5)
    rgb = ramp(1 - np.clip(t, 0, 1), [(0.0, deep), (0.55, light), (1.0, (250, 244, 255))])
    streak = np.exp(-(((u + v * 0.6) - 0.85) / 0.05) ** 2) * 0.55
    rgb = over(rgb, (255, 255, 255), streak)
    for k in range(rays):
        a = 0.9 + k * 1.1
        d = np.abs((u - 0.5) * math.sin(a) - (v - 0.5) * math.cos(a))
        rgb = over(rgb, (255, 255, 255), np.exp(-(d / 0.012) ** 2) * 0.35)
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.55) ** 2)
    rgb = add_glow(rgb, np.exp(-(r / 0.4) ** 2) * gain, glowc, 1.0)
    return opaque(rgb)


def cell_gem_a():
    return facet(141, (176, 128, 255), (96, 52, 200), (190, 150, 255), 0.18, 1)


def cell_gem_b():
    return facet(143, (122, 74, 224), (54, 28, 138), (140, 90, 240), 0.10, 1)


def cell_gem_c():
    return facet(147, (226, 206, 255), (160, 120, 255), (255, 240, 255), 0.55, 2)


def cell_head_plate():
    def fn(m, W, H):
        c = W / 2
        m.circle(c, c, 0.92 * c, 3.2)
        m.circle(c, c, 0.76 * c, 1.8)
        m.circle(c, c, 0.30 * c, 1.8)
        g.hexagram(m, c, c, 0.70 * c, 2.2)
        rr = random.Random(6)
        g.rune_ring(m, c, c, 0.84 * c, 24, 0.075 * c * 2, rr, 1.5)
        m.disc(c, c, 0.06 * c)
    mk = stretched(1.0, fn)
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    disc = g.smooth(0.97, 0.9, r)
    rgb = np.zeros((S, S, 3), np.float32) + col((30, 20, 66))
    rgb = add_glow(rgb, np.exp(-(r / 0.55) ** 2) * 0.55, g.VIOLET, 1.0)
    rgb = line_layer(rgb, mk, g.GOLD, g.GOLD_D, (1.6, 5.0), (1.0, 0.7))
    a = np.clip(disc * 0.55 + mk * 0.95 + blur(mk, 3) * 0.7, 0, 1) * g.smooth(1.0, 0.92, r)
    return np.clip(rgb, 1 / 255, 1), a


# ---------------------------------------------------------------------------------------------------- the signet ring
def gold(seed, strength=1.0, bevel_w=0.16):
    h = bevel(bevel_w) * 0.9 + (fbm(S, S, seed) - 0.5) * 0.05
    t, lam = lit(h, k=9.0, ambient=0.34)
    rgb = ramp(t, GOLDRAMP)
    return rgb


def cell_ring_band():
    rgb = gold(151)

    def fn(m, W, H):
        rr = random.Random(12)
        for i in range(4):
            g.draw_rune(m, g.make_rune(rr), W * (i + 0.5) / 4, H * 0.5, H * 0.34, 0.0, 3.0)
    mk = stretched(3.0, fn)
    rgb = rgb * (1 - 0.55 * blur(mk, 2.0))[..., None]
    rgb = over(rgb, (255, 236, 170), np.clip(blur(mk, 1.0) - mk, 0, 1) * 0.5)
    rgb = over(rgb, (110, 62, 14), mk * 0.85)
    rgb = add_glow(rgb, blur(mk, 4) * 0.2, g.LILAC, 1.0)
    return opaque(rgb)


def cell_ring_band_b():
    rgb = gold(153, bevel_w=0.2)
    u, v = grid01()
    rgb = over(rgb, (255, 248, 214), np.exp(-((v - 0.5) / 0.025) ** 2) * 0.55)
    rgb = over(rgb, g.LILAC, np.exp(-((v - 0.5) / 0.012) ** 2) * 0.35)
    return opaque(rgb)


def cell_bezel():
    rgb = gold(157, bevel_w=0.22)
    u, v = grid01()
    rgb = over(rgb, (110, 62, 14), np.exp(-((u - 0.5) / 0.02) ** 2) * 0.35)
    rgb = over(rgb, (255, 248, 214), np.exp(-((v - 0.12) / 0.03) ** 2) * 0.5)
    return opaque(rgb)


def cell_halo():
    def fn(m, W, H):
        c = W / 2
        m.circle(c, c, 0.94 * c, 3.0)
        m.circle(c, c, 0.72 * c, 1.6)
        m.circle(c, c, 0.60 * c, 1.2)
        rr = random.Random(21)
        g.rune_ring(m, c, c, 0.83 * c, 22, 0.078 * c * 2, rr, 1.6)
        for k in range(12):
            a = 2 * math.pi * k / 12
            m.line([(c + math.cos(a) * 0.60 * c, c + math.sin(a) * 0.60 * c), (c + math.cos(a) * 0.52 * c, c + math.sin(a) * 0.52 * c)], 1.6)
    mk = stretched(1.0, fn)
    u, v = grid01()
    r = np.sqrt((u - 0.5) ** 2 + (v - 0.5) ** 2) * 2
    ring = g.smooth(0.55, 0.62, r) * g.smooth(0.99, 0.94, r)
    rgb = np.zeros((S, S, 3), np.float32) + col((40, 26, 92))
    rgb = add_glow(rgb, ring * 0.35, g.VIOLET, 1.0)
    rgb = line_layer(rgb, mk, g.GOLD, g.GOLD_D, (1.6, 5.0), (1.0, 0.7))
    a = np.clip(ring * 0.42 + mk * 0.95 + blur(mk, 3) * 0.7, 0, 1) * g.smooth(1.0, 0.95, r)
    return np.clip(rgb, 1 / 255, 1), a


# ---------------------------------------------------------------------------------------------------- crystals and glass
def crystal(seed, deep):
    u, v = grid01()
    n = fbm(S, S, seed, cells=(24, 12), gains=(0.6, 0.4))
    t = 0.30 + 0.55 * v + 0.12 * (n - 0.5)
    rgb = ramp(np.clip(t, 0, 1), [(0.0, deep), (0.55, (214, 206, 255)), (1.0, (250, 248, 255))])
    streak = np.exp(-(((u * 0.8 + v * 0.7) - 0.72) / 0.05) ** 2) * 0.6
    rgb = over(rgb, (255, 255, 255), streak)
    return rgb, u, v


def cell_crystal_a():
    rgb, u, v = crystal(161, (150, 140, 235))
    return opaque(rgb)


def cell_crystal_b():
    rgb, u, v = crystal(163, (118, 108, 214))

    def fn(m, W, H):
        g.draw_rune(m, g.make_rune(random.Random(33)), W / 2, H * 0.55, H * 0.36, 0.0, 3.2)
    mk = stretched(1.0, fn)
    rgb = line_layer(rgb, mk, (255, 255, 255), (200, 190, 255), (1.4, 4.5), (0.9, 0.6))
    return opaque(rgb)


def cell_shard():
    u, v = grid01()
    n = fbm(S, S, 171, cells=(24, 12), gains=(0.6, 0.4))

    def fn(m, W, H):                                                                            # a piece of a magic circle: arcs of rings and a row of runes
        c, y = W * 0.5, H * 1.0
        for rad, w in ((0.92, 3.2), (0.74, 1.8), (0.58, 1.4)):
            m.circle(c, y, rad * H, w)
        rr = random.Random(8)
        g.rune_ring(m, c, y, 0.83 * H, 40, 0.075 * H * 2, rr, 1.6)
        for k in range(14):
            a = -math.pi / 2 + (k - 6.5) * 0.12
            m.line([(c + math.cos(a) * 0.58 * H, y + math.sin(a) * 0.58 * H), (c + math.cos(a) * 0.74 * H, y + math.sin(a) * 0.74 * H)], 1.2)
    mk = stretched(1.0, fn)
    rgb = ramp(0.35 + 0.5 * v + 0.25 * (n - 0.5), [(0.0, (130, 110, 220)), (0.6, GLASS), (1.0, (250, 246, 255))])
    streak = np.exp(-(((u * 0.9 + v * 0.8) - 0.55) / 0.06) ** 2) * 0.55
    rgb = over(rgb, (255, 255, 255), streak)
    rgb = line_layer(rgb, mk, g.GOLD, g.GOLD_D, (1.4, 4.5), (0.9, 0.6))
    a = np.clip(0.50 + 0.18 * (n - 0.5) + streak * 0.3 + mk * 0.9 + blur(mk, 3) * 0.5, 0, 1)
    return np.clip(rgb, 1 / 255, 1), a


def cell_shard_b():
    u, v = grid01()
    n = fbm(S, S, 173, cells=(24, 12), gains=(0.6, 0.4))
    rgb = ramp(0.25 + 0.5 * v + 0.3 * (n - 0.5), [(0.0, (110, 90, 200)), (0.6, (190, 170, 250)), (1.0, (235, 228, 255))])
    streak = np.exp(-(((u * 0.9 - v * 0.8) + 0.3) / 0.07) ** 2) * 0.5
    rgb = over(rgb, (255, 255, 255), streak)
    a = np.clip(0.42 + 0.2 * (n - 0.5) + streak * 0.3, 0, 1)
    return np.clip(rgb, 1 / 255, 1), a


def cell_shard_edge():
    u, v = grid01()
    n = fbm(S, S, 177, cells=(24, 12), gains=(0.6, 0.4))
    rgb = ramp(0.55 + 0.4 * u + 0.15 * (n - 0.5), [(0.0, (200, 184, 255)), (0.7, (244, 238, 255)), (1.0, (255, 255, 255))])
    return np.clip(rgb, 1 / 255, 1), np.full((S, S), 0.96, np.float32)


CELL_FUNCS = [cell_stone, cell_stone_dark, cell_flute, cell_trim, cell_cap_top, cell_base_ring] + [(lambda i=i: cell_sigil(i)) for i in range(8)] + [
    cell_leather, cell_cover_emblem, cell_cover_back, cell_page, cell_page_l, cell_page_glow, cell_page_edge, cell_ribbon, cell_spine,
    cell_link_top, cell_link_side, cell_gem_a, cell_gem_b, cell_gem_c, cell_head_plate,
    cell_ring_band, cell_ring_band_b, cell_bezel, cell_halo, cell_crystal_a, cell_crystal_b, cell_shard, cell_shard_b, cell_shard_edge, cell_page_glow_l]

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")


def tex_atlas():
    assert len(CELL_FUNCS) == CELLS, len(CELL_FUNCS)
    P = np.zeros((COLS * S, COLS * S, 3), np.float32)
    A = np.zeros((COLS * S, COLS * S), np.float32)
    for i, fn in enumerate(CELL_FUNCS):
        rgb, a = fn()
        cx, cy = i % COLS, i // COLS
        P[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = rgb
        A[cy * S:(cy + 1) * S, cx * S:(cx + 1) * S] = a
    a8 = (np.clip(A, 0, 1) * 255 + 0.5).astype(np.uint8)
    px = np.dstack([(np.clip(P, 0, 1) * 255 + 0.5).astype(np.uint8), a8])
    px[px[..., 3] == 0, :3] = 0
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(px, "RGBA").save(os.path.join(OUT, "solomon_model_atlas.png"), optimize=True)
    print("wrote solomon_model_atlas", px.shape)


ALL = {"atlas": tex_atlas}

if __name__ == "__main__":
    wanted = sys.argv[1:] or list(ALL)
    for n in wanted:
        ALL[n]()

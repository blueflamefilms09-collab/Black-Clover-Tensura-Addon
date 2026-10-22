"""Spirit Dive remake: the sprites (textures/aura/sdv_*.png) and the vein layers (textures/aura/spirit2_<element>_veins.png).

Called by tools/gen_spirit_dive_v2.py (parts `sprites` and `veins`). Every sprite is additive: the colour is already weighted by its alpha (black where it is clear) and the vertex colour of the
painter tints it (client/aura/SpiritDiveFx), so they are white / pale. Deterministic. Run python as `python3 -B`.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from sdv_lib import (fbm2, value_noise, clamp01, smooth, save_rgba, premult, radial, mask_from_polys, blur, tongue_polys, rgb, TAU)  # noqa: E402

BIG = 1024                                    # the working size of the line-art sigils (drawn big, shrunk with a good filter)


def white(a):
    """A premultiplied white sprite from an intensity map."""
    a = clamp01(a)
    return premult(np.ones(a.shape + (3,), np.float32) * 255.0, a)


# ====================================================================================================================== column, pillar, shockwave, wisp
def column():
    w, h = 64, 128
    rng = np.random.default_rng(11)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (x + 0.5) / w, (y + 0.5) / h
    hfade = clamp01(1.0 - (2 * u - 1) ** 2) ** 1.4
    vfade = np.sin(np.pi * clamp01(v * 0.92 + 0.04)) ** 0.8
    n = fbm2(rng, w, h, 7, 2, 3)
    streak = clamp01((n - 0.30) * 2.6)
    core = np.exp(-((u - 0.5) / 0.22) ** 2)
    a = hfade * vfade * (0.25 + 0.75 * streak) * (0.55 + 0.45 * core)
    return white(a * 1.15)


def pillar():
    w, h = 32, 256
    rng = np.random.default_rng(12)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (x + 0.5) / w, (y + 0.5) / h
    core = np.exp(-((u - 0.5) / 0.10) ** 2) * 0.9 + np.exp(-((u - 0.5) / 0.30) ** 2) * 0.35
    vf = clamp01(1.0 - v) ** 0.55 * smooth(v, 0.0, 0.08) * 0 + (clamp01(1.0 - v) ** 0.6)
    n = fbm2(rng, w, h, 3, 8, 3)
    return white(core * vf * (0.8 + 0.4 * n))


def shock():
    s = 256
    r, ang = radial(s)
    rng = np.random.default_rng(13)
    ring = np.exp(-((r - 0.80) / 0.045) ** 2)
    halo = np.exp(-((r - 0.80) / 0.14) ** 2) * 0.35
    spikes = value_noise(rng, s, s, 48, 1) if False else None
    # radial streaks: noise by angle only
    ang_idx = ((ang + np.pi) / TAU * 96).astype(int) % 96
    sk = rng.random(96).astype(np.float32)
    streak = sk[ang_idx]
    outer = clamp01((r - 0.80) / 0.2) * clamp01((1.0 - r) / 0.2) * (streak > 0.55) * streak * 0.8
    inner = clamp01(1.0 - r / 0.8) ** 3 * 0.12
    return white(ring + halo + outer + inner)


def wisp():
    w, h = 64, 16
    rng = np.random.default_rng(14)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (x + 0.5) / w, (y + 0.5) / h
    across = np.exp(-((v - 0.5) / 0.30) ** 2)
    along = clamp01(u * 1.1) ** 0.9                                  # tail (u = 0) fades out, the root (u = 1) is bright
    n = fbm2(rng, w, h, 6, 2, 2)
    return white(across * along * (0.7 + 0.6 * n) * 1.35)


# ====================================================================================================================== line-art sigils
def _canvas():
    im = Image.new("L", (BIG, BIG), 0)
    return im, ImageDraw.Draw(im), BIG // 2


def _ring(d, c, r, w):
    d.ellipse([c - r, c - r, c + r, c + r], outline=255, width=w)


def _finish(im, name):
    a = np.asarray(im.resize((256, 256), Image.LANCZOS), dtype=np.float32) / 255.0
    a = np.clip(a * 1.25, 0, 1)
    save_rgba(name, white(a))


def _cross(d, x, y, s, w):
    d.line([x - s, y - s, x + s, y + s], fill=255, width=w)
    d.line([x - s, y + s, x + s, y - s], fill=255, width=w)


def _poly_ring(d, c, r, n, rot, w):
    pts = [(c + r * math.cos(rot + i * TAU / n), c + r * math.sin(rot + i * TAU / n)) for i in range(n)]
    d.line(pts + [pts[0]], fill=255, width=w)


def sigil_wind():
    """A circle of connected crosses (the mark of the wind spirit's dive in the series), turning swirls and a four-point star."""
    im, d, c = _canvas()
    _ring(d, c, 492, 9)
    _ring(d, c, 452, 4)
    _ring(d, c, 400, 5)
    for i in range(16):
        a = i * TAU / 16
        _cross(d, c + 400 * math.cos(a), c + 400 * math.sin(a), 26, 7)
        a2 = a + TAU / 32
        d.ellipse([c + 400 * math.cos(a2) - 5, c + 400 * math.sin(a2) - 5, c + 400 * math.cos(a2) + 5, c + 400 * math.sin(a2) + 5], fill=255)
    for k in range(8):                                                  # eight swirls
        pts = []
        for t in range(0, 61):
            tt = t / 60.0
            r = 120 + 240 * tt
            a = k * TAU / 8 + tt * 1.5
            pts.append((c + r * math.cos(a), c + r * math.sin(a)))
        d.line(pts, fill=255, width=6)
    _ring(d, c, 250, 5)
    _ring(d, c, 110, 6)
    for k in range(4):
        a = k * TAU / 4 + math.pi / 4
        d.polygon([(c + 110 * math.cos(a - 0.13), c + 110 * math.sin(a - 0.13)), (c + 190 * math.cos(a), c + 190 * math.sin(a)), (c + 110 * math.cos(a + 0.13), c + 110 * math.sin(a + 0.13))], fill=255)
    _finish(im, "sdv_sigil_wind")


def sigil_fire():
    """A sun of flames: a toothed ring of flame spikes, a six-pointed star, rings and an inner sun."""
    im, d, c = _canvas()
    _ring(d, c, 440, 9)
    _ring(d, c, 372, 5)
    for i in range(36):                                                 # flame spikes round the ring
        a = i * TAU / 36
        r0, r1 = 440, 498 if i % 2 == 0 else 478
        d.polygon([(c + r0 * math.cos(a - 0.065), c + r0 * math.sin(a - 0.065)), (c + r1 * math.cos(a + 0.02), c + r1 * math.sin(a + 0.02)),
                   (c + r0 * math.cos(a + 0.065), c + r0 * math.sin(a + 0.065))], fill=255)
    _poly_ring(d, c, 372, 3, -math.pi / 2, 7)
    _poly_ring(d, c, 372, 3, math.pi / 2, 7)
    _ring(d, c, 215, 6)
    _ring(d, c, 150, 4)
    for i in range(12):                                                 # rays of the inner sun
        a = i * TAU / 12
        d.polygon([(c + 150 * math.cos(a - 0.09), c + 150 * math.sin(a - 0.09)), (c + 210 * math.cos(a), c + 210 * math.sin(a)), (c + 150 * math.cos(a + 0.09), c + 150 * math.sin(a + 0.09))], fill=255)
    d.ellipse([c - 70, c - 70, c + 70, c + 70], fill=255)
    d.ellipse([c - 40, c - 40, c + 40, c + 40], fill=0)
    d.ellipse([c - 20, c - 20, c + 20, c + 20], fill=255)
    _finish(im, "sdv_sigil_fire")


def sigil_water():
    """Rings, a band of waves, six teardrop lenses and a hexagon."""
    im, d, c = _canvas()
    _ring(d, c, 492, 9)
    _ring(d, c, 330, 5)
    pts = []
    for t in range(0, 721):
        a = t * TAU / 720
        r = 430 + 18 * math.sin(a * 24)
        pts.append((c + r * math.cos(a), c + r * math.sin(a)))
    d.line(pts, fill=255, width=7)
    for k in range(6):                                                  # lens / teardrops
        a = k * TAU / 6
        cx, cy = c + 250 * math.cos(a), c + 250 * math.sin(a)
        tip = (c + 340 * math.cos(a), c + 340 * math.sin(a))
        perp = (-math.sin(a), math.cos(a))
        d.polygon([(cx + perp[0] * 40, cy + perp[1] * 40), tip, (cx - perp[0] * 40, cy - perp[1] * 40), (c + 170 * math.cos(a), c + 170 * math.sin(a))], outline=255, width=6)
    _poly_ring(d, c, 130, 6, 0, 7)
    _ring(d, c, 70, 6)
    d.ellipse([c - 22, c - 22, c + 22, c + 22], fill=255)
    _finish(im, "sdv_sigil_water")


def sigil_earth():
    """Rings, rune ticks, a hexagonal lattice and four crystals at the points of the compass."""
    im, d, c = _canvas()
    _ring(d, c, 492, 10)
    _ring(d, c, 440, 5)
    rng = np.random.default_rng(15)
    for i in range(48):                                                 # rune ticks between the rings
        a = i * TAU / 48
        r0 = 448
        r1 = 448 + 22 + int(rng.integers(0, 14))
        d.line([c + r0 * math.cos(a), c + r0 * math.sin(a), c + r1 * math.cos(a), c + r1 * math.sin(a)], fill=255, width=6 if i % 4 else 11)
    _poly_ring(d, c, 380, 6, 0, 8)
    _poly_ring(d, c, 380, 6, math.pi / 6, 5)
    _ring(d, c, 290, 5)
    for k in range(4):                                                  # crystals
        a = k * TAU / 4
        cx, cy = c + 335 * math.cos(a), c + 335 * math.sin(a)
        ux, uy = math.cos(a), math.sin(a)
        px, py = -uy, ux
        d.polygon([(cx + ux * 60, cy + uy * 60), (cx + px * 26, cy + py * 26), (cx - ux * 40, cy - uy * 40), (cx - px * 26, cy - py * 26)], outline=255, width=7)
    for i in range(6):                                                  # hexagon lattice at the centre
        a = i * TAU / 6
        d.line([c, c, c + 160 * math.cos(a), c + 160 * math.sin(a)], fill=255, width=5)
    _poly_ring(d, c, 160, 6, 0, 7)
    _poly_ring(d, c, 80, 6, math.pi / 6, 6)
    _finish(im, "sdv_sigil_earth")


def runering():
    """A band of runes between two thin rings (the rings that orbit the torso of the character tier)."""
    im, d, c = _canvas()
    rng = np.random.default_rng(16)
    _ring(d, c, 494, 6)
    _ring(d, c, 372, 6)
    _ring(d, c, 340, 3)
    n = 32
    for i in range(n):                                                  # a rune per segment: two or three strokes
        a0 = i * TAU / n
        rm = 433
        for k in range(int(rng.integers(2, 4))):
            kind = int(rng.integers(0, 4))
            ra = rm + float(rng.uniform(-34, 34))
            aa = a0 + float(rng.uniform(0.02, TAU / n - 0.02))
            x, y = c + ra * math.cos(aa), c + ra * math.sin(aa)
            if kind == 0:                                               # a radial stroke
                d.line([c + (ra - 26) * math.cos(aa), c + (ra - 26) * math.sin(aa), c + (ra + 26) * math.cos(aa), c + (ra + 26) * math.sin(aa)], fill=255, width=7)
            elif kind == 1:                                             # a tangent stroke
                d.line([x - 24 * math.sin(aa), y + 24 * math.cos(aa), x + 24 * math.sin(aa), y - 24 * math.cos(aa)], fill=255, width=7)
            elif kind == 2:                                             # a dot
                d.ellipse([x - 9, y - 9, x + 9, y + 9], fill=255)
            else:                                                       # a small chevron
                d.line([x - 18 * math.sin(aa) - 12 * math.cos(aa), y + 18 * math.cos(aa) - 12 * math.sin(aa), x, y, x + 18 * math.sin(aa) - 12 * math.cos(aa), y - 18 * math.cos(aa) - 12 * math.sin(aa)],
                       fill=255, width=7)
    _finish(im, "sdv_runering")


# ====================================================================================================================== the flame sheet (four frames)
def flame_sheet():
    """Four frames (64 x 128 each, side by side) of a flame: tongues rising from the bottom, a white-yellow heart, orange body, red tips, premultiplied for the additive blend."""
    fw, fh = 64, 128
    sheet = np.zeros((fh, fw * 4, 4), np.float32)
    for f in range(4):
        rng = np.random.default_rng(100 + f)
        polys = []
        for t in range(4):
            ph = f * 1.7 + t * 2.1
            base = 0.9 - t * 0.14
            polys += tongue_polys(sway=0.20 + 0.04 * t, phase=ph, base=base, wob=0.08, n=36)
        a = mask_from_polys(fw, fh, polys, ss=4)
        # heights: each tongue should have its own length: cut the union by a noisy skyline
        y, x = np.mgrid[0:fh, 0:fw].astype(np.float32)
        u, v = (x + 0.5) / fw, (y + 0.5) / fh
        sky = 0.10 + 0.5 * fbm2(rng, fw, fh, 4, 1, 2)
        a = a * smooth(v - sky, 0.0, 0.08)
        inner = blur(a, 2.2)
        heat = clamp01(inner * 1.1 * (0.45 + 0.75 * v))                     # hot at the bottom, cooler toward the tips
        c_red, c_or, c_ye, c_wh = rgb("B01808"), rgb("FF5A12"), rgb("FFC030"), rgb("FFF4C8")
        col = c_red[None, None, :] + (c_or - c_red)[None, None, :] * clamp01(heat * 2.0)[..., None]
        col = col + (c_ye - col) * clamp01((heat - 0.45) * 2.6)[..., None]
        col = col + (c_wh - col) * clamp01((heat - 0.86) * 5.0)[..., None]
        al = clamp01(a * 1.25)
        sheet[:, f * fw:(f + 1) * fw] = premult(col, al)
    save_rgba("sdv_flame_sheet", sheet)


def sprites():
    save_rgba("sdv_column", column())
    save_rgba("sdv_pillar", pillar())
    save_rgba("sdv_shock", shock())
    save_rgba("sdv_wisp", wisp())
    sigil_wind()
    sigil_fire()
    sigil_water()
    sigil_earth()
    runering()
    flame_sheet()


# ====================================================================================================================== vein layers (64 x 64 player layout, additive)
def _vor_edge(w, h, cells_x, cells_y, seed):
    """Distance difference to the two nearest of random points on a torus grid: ~0 on a cell border (the veins)."""
    rng = np.random.default_rng(seed)
    pts = []
    for cy in range(cells_y):
        for cx in range(cells_x):
            pts.append(((cx + rng.random()) / cells_x * w, (cy + rng.random()) / cells_y * h))
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    ds = []
    for (px, py) in pts:
        dx = np.minimum(np.abs(x - px), w - np.abs(x - px))
        dy = np.minimum(np.abs(y - py), h - np.abs(y - py))
        ds.append(np.hypot(dx, dy))
    ds = np.sort(np.stack(ds), axis=0)
    return ds[1] - ds[0]


def shells():
    """The remade fire dive's mana skin (textures/aura/spirit2_fire_shell + _glow): a translucent crimson-orange skin with rising flame streaks (the old one is near black obsidian, which hides the
    body of a fire dive in the shade)."""
    w = h = 64
    rng = np.random.default_rng(31)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    n = fbm2(rng, w, h, 6, 3, 3)
    streak = 0.5 + 0.5 * np.sin(x * 0.9 + n * 7.0 + y * 0.18)
    t = clamp01(0.25 + 0.5 * n + 0.35 * streak)
    col = rgb("B81C08")[None, None, :] + (rgb("FF8A24") - rgb("B81C08"))[None, None, :] * t[..., None]
    img = np.zeros((h, w, 4), np.float32)
    img[..., :3] = col
    img[..., 3] = 150.0 + 40.0 * t
    g = np.zeros((h, w, 4), np.float32)
    ga = clamp01((streak - 0.55) * 1.6) * 0.55
    g[..., :3] = rgb("FF7A1E")[None, None, :] * ga[..., None]
    g[..., 3] = np.where(ga > 0.04, 255.0, 0.0)
    g[..., :3] *= (g[..., 3:4] > 0)                                       # no colour under a clear texel (the SafeAdditive texture scan)
    save_rgba("spirit2_fire_shell", img)
    save_rgba("spirit2_fire_shell_glow", g)


def veins():
    shells()
    w = h = 64
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    # ---- wind: long streamlines, mint with a white heart
    rng = np.random.default_rng(21)
    n = fbm2(rng, w, h, 3, 3, 3)
    stream = np.abs(np.sin((y * 0.55 + n * 9.0 + x * 0.12) * 1.0))
    line = clamp01(1.0 - stream / 0.18) ** 1.5
    a = line * (0.35 + 0.65 * fbm2(rng, w, h, 5, 2, 2))
    col = np.ones((h, w, 3), np.float32) * rgb("C8FFE4")[None, None, :]
    save_rgba("spirit2_wind_veins", premult(col, a))
    # ---- fire: lava cracks
    e = _vor_edge(w, h, 7, 7, 22)
    crack = clamp01(1.0 - e / 1.6) ** 1.4
    rng = np.random.default_rng(23)
    hot = 0.5 + 0.5 * fbm2(rng, w, h, 4, 4, 3)
    col = rgb("FF5A12")[None, None, :] + (rgb("FFE070") - rgb("FF5A12"))[None, None, :] * clamp01(hot * crack * 1.4)[..., None]
    save_rgba("spirit2_fire_veins", premult(col, crack * (0.55 + 0.45 * hot)))
    # ---- water: a net of currents
    rng = np.random.default_rng(24)
    n1, n2 = fbm2(rng, w, h, 4, 4, 3), fbm2(rng, w, h, 3, 5, 3)
    caustic = np.abs(np.sin(x * 0.33 + n1 * 6.0) * np.sin(y * 0.29 + n2 * 6.0))
    c2 = clamp01(1.0 - caustic / 0.12) ** 1.4
    col = np.ones((h, w, 3), np.float32) * rgb("BFEFFF")[None, None, :]
    save_rgba("spirit2_water_veins", premult(col, c2 * 0.9))
    # ---- earth: crystal veins, angular, amber with bright nodes
    e = _vor_edge(w, h, 5, 5, 25)
    vein = clamp01(1.0 - e / 1.3) ** 1.3
    rng = np.random.default_rng(26)
    node = clamp01((fbm2(rng, w, h, 8, 8, 2) - 0.62) * 6.0) * vein
    col = rgb("FFB040")[None, None, :] + (rgb("FFF0C0") - rgb("FFB040"))[None, None, :] * node[..., None]
    save_rgba("spirit2_earth_veins", premult(col, vein * (0.6 + 0.4 * node)))

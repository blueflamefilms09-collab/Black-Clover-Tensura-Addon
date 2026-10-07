"""Textures for the rest of Yami's Dark Magic (DarkMagicLayer: black hole, black moon, death thrust, iai slash). Deterministic, procedural
(numpy + Pillow, fixed seeds); builds on the helpers of gen_dark_slash_textures.py so both halves share one palette.

    python3 -B tools/gen_dark_magic_textures.py

Palette (docs/attributes/art_reference/dark_vfx_collection.webp): pitch-black cores, deep violet to magenta light, white-violet rims, tiny stars.
"baked" textures carry their own colours (ALPHA blend holds the black; ADD blend adds the light), "glow" ones are grey + alpha (ADD, tinted by the vertex colour).

  textures/particle/darkfx_orb.png          baked  256x256  the black sphere of the black hole: pure black, a thin white-violet limb light
  textures/particle/darkfx_disk.png         baked  512x512  the accretion disk: hot white-violet inner edge, swirled violet / magenta arms (ADD)
  textures/particle/darkfx_photon.png       glow   256x256  the thin bright photon ring hugging the hole, brighter on one side
  textures/particle/darkfx_ripple.png       glow   256x256  lensing ripples: fine wavy fringes that spread outward
  textures/particle/darkfx_vignette.png     baked  128x128  a soft black shadow (darkens the air round the hole, the ground under the moon)
  textures/particle/darkfx_moon.png         baked  512x512  the black moon: black cratered disc, a bright crescent rim on one side
  textures/particle/darkfx_corona.png       baked  512x512  the moon's violet corona: soft halo and long uneven rays (ADD)
  textures/particle/darkfx_moonring.png     glow   512x512  the ground sigil under the moon: broken rings, ticks, glyphs, an eight pointed star
  textures/particle/darkfx_spear.png        baked   64x512  the death thrust spear (v = 0 tail, 1 tip): black lance body, white-violet edge, ragged tail
  textures/particle/darkfx_spear_glow.png   glow    64x512  its violet halo
  textures/particle/darkfx_shock.png        glow   256x256  the shock cone's rings: broken thin ring with radial ticks
  textures/particle/darkfx_iai.png          baked   32x512  the iai cut line: white-hot core, violet to magenta fringe (ADD)
  textures/particle/darkfx_afterimage.png   baked   64x512  the black afterimage the cut leaves: ragged black slit with a hairline rim
  textures/particle/darkfx_shard_a.png      baked  128x128  a black angular shard with a violet edge (debris)
  textures/particle/darkfx_shard_b.png      baked  128x128  another shard
  textures/particle/darkfx_mote.png         baked  128x128  a drifting black mote with violet edge light
  textures/particle/darkfx_flare.png        glow   256x256  a flare: long horizontal streak, short vertical one, hot core (the sheath flash)
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm  # noqa: E402
import gen_dark_slash_textures as ds  # noqa: E402
from gen_dark_slash_textures import BLACK, VIOLET, DEEP, MAGENTA, RIM, Canvas, ss, mix, grey_image, put, mask, spikes  # noqa: E402


def polar(s):
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x, y = (xx + 0.5) / s * 2 - 1, (yy + 0.5) / s * 2 - 1
    return x, y, np.sqrt(x * x + y * y), np.arctan2(y, x)


def swirl_noise(s, x, y, r, cell, seed, k, off=0.2):
    """Noise sampled through a differential rotation (inner parts turn faster), which pulls it into spiral arms."""
    base = fbm(s, s, cell, seed, 4)
    phi = k / (r + off)
    c, sn = np.cos(phi), np.sin(phi)
    xs, ys = x * c - y * sn, x * sn + y * c
    ix = np.clip(((xs + 1) / 2 * s).astype(int), 0, s - 1)
    iy = np.clip(((ys + 1) / 2 * s).astype(int), 0, s - 1)
    return base[iy, ix]


def ang_noise(th, seed, kmin, kmax, count):
    """A periodic 1-D noise over the angle (sum of random harmonics), 0..1."""
    rng = np.random.default_rng(seed)
    v = np.zeros_like(th)
    tot = 0.0
    for _ in range(count):
        k = int(rng.integers(kmin, kmax))
        a = rng.uniform(0.4, 1.0) / math.sqrt(k)
        v = v + a * np.cos(k * th + rng.uniform(0, 6.28))
        tot += a
    return np.clip(v / tot * 0.9 + 0.5, 0, 1)


# ---------------------------------------------------------------- black hole
def orb():
    s = 256
    x, y, r, th = polar(s)
    n = fbm(s, s, 12, 8001)
    cv = Canvas(s, s)
    cv.over(DEEP, np.exp(-((r - 0.84) / 0.12) ** 2) * 0.35 * (0.6 + 0.8 * n))             # faint volumetric haze just outside the limb
    cv.over(BLACK, ss(r, 0.80, 0.77))
    limb = np.exp(-((r - 0.775) / 0.018) ** 2) * (0.35 + 0.65 * (0.5 + 0.5 * np.cos(th - 2.2)))
    cv.over(mix(VIOLET, RIM, np.clip(limb, 0, 1)), np.clip(limb * 1.1, 0, 1))
    cv.over(BLACK, np.exp(-((r - 0.70) / 0.05) ** 2) * 0.0)
    put(cv.image(), "darkfx_orb")


def disk():
    s = 512
    x, y, r, th = polar(s)
    n1 = swirl_noise(s, x, y, r, 14, 8101, 1.9)
    n2 = swirl_noise(s, x, y, r, 40, 8111, 2.6)
    arms = np.clip(0.25 + 0.95 * ss(n1, 0.30, 0.72) + 0.4 * (n2 - 0.5), 0, 1.2)
    bands = 1 - 0.38 * (0.5 + 0.5 * np.sin(r * 90 + n1 * 5)) * ss(r, 0.34, 0.5)            # fine dark lanes between the hot streaks
    inner = ss(r, 0.255, 0.30)
    outer = (1 - ss(r, 0.50, 0.99)) ** 1.35
    heat = np.exp(-(r - 0.29) / 0.10)
    a = inner * outer * (0.30 + 0.85 * arms) * bands * (0.8 + 0.4 * heat)
    a = np.clip(a * (0.65 + 0.35 * (0.5 + 0.5 * np.cos(th - 0.9))), 0, 1)                   # one side brighter (relativistic beaming)
    col = mix(MAGENTA, VIOLET, np.clip(ss(r, 0.34, 0.8), 0, 1))
    col = mix(col, RIM, np.clip(heat * 1.15 - 0.25, 0, 1))
    cv = Canvas(s, s)
    cv.over(col, a)
    cv.add(RIM, np.exp(-((r - 0.285) / 0.012) ** 2) * 0.9)                                  # the hot inner edge
    put(cv.image(), "darkfx_disk")


def photon():
    s = 256
    x, y, r, th = polar(s)
    side = 0.5 + 0.5 * np.cos(th - 0.9)
    a = np.exp(-((r - 0.62) / 0.012) ** 2) * (0.55 + 0.45 * side)
    a += np.exp(-((r - 0.635) / 0.006) ** 2) * 0.5 * side
    a += np.exp(-((r - 0.62) / 0.07) ** 2) * 0.28 * (0.4 + 0.6 * side)
    a += np.exp(-((r - 0.70) / 0.02) ** 2) * 0.25 * ang_noise(th, 8201, 2, 9, 5)           # a faint outer lensed arc, broken
    a *= 1 - ss(r, 0.93, 1.0)
    put(grey_image(np.clip(a, 0, 1)), "darkfx_photon")


def ripple():
    s = 256
    x, y, r, th = polar(s)
    wob = 0.012 * np.sin(th * 4 + 0.6) + 0.008 * np.sin(th * 9 + 2.0)
    a = np.zeros_like(r)
    for k in range(6):
        rr = 0.40 + k * 0.095 + wob * (1 + k * 0.6)
        a = np.maximum(a, np.exp(-((r - rr) / (0.007 + 0.002 * k)) ** 2) * (1.0 - k * 0.13))
    a += np.exp(-((r - 0.66) / 0.17) ** 2) * 0.10
    a *= (0.45 + 0.55 * ss(ang_noise(th, 8301, 2, 11, 6), 0.2, 0.7))
    a *= ss(r, 0.30, 0.42) * (1 - ss(r, 0.90, 1.0))
    put(grey_image(np.clip(a, 0, 1)), "darkfx_ripple")


def vignette():
    s = 128
    x, y, r, th = polar(s)
    a = np.clip(1 - r, 0, 1) ** 1.7 * 0.92
    cv = Canvas(s, s)
    cv.over(BLACK, a)
    put(cv.image(), "darkfx_vignette")


# ---------------------------------------------------------------- black moon
def moon():
    s = 512
    x, y, r, th = polar(s)
    n = fbm(s, s, 10, 8401)
    n2 = fbm(s, s, 40, 8402)
    body = ss(r, 0.965, 0.945)
    cv = Canvas(s, s)
    cv.over(BLACK, body)
    rng = np.random.default_rng(8403)
    crat = np.zeros_like(r)
    for _ in range(26):                                                                   # craters: a faint lit lip on the lit side, a deeper floor
        ang, rad = rng.uniform(0, 6.28), math.sqrt(rng.uniform(0, 0.8))
        cx, cy = math.cos(ang) * rad, math.sin(ang) * rad
        cr = rng.uniform(0.04, 0.14)
        d = np.sqrt((x - cx) ** 2 + (y - cy) ** 2)
        lip = np.exp(-((d - cr) / (cr * 0.12)) ** 2)
        lit = np.clip(((x - cx) * -0.7 + (y - cy) * -0.7) / (cr + 1e-3), -1, 1) * 0.5 + 0.5      # light comes from the upper left
        crat += lip * lit * 0.55 + ss(d, cr, cr * 0.4) * 0.12
    shade = np.clip((-0.6 * x - 0.6 * y) * 0.5 + 0.5, 0, 1)
    surf = (0.05 + 0.10 * n + 0.05 * n2 + 0.22 * np.clip(crat, 0, 1)) * (0.35 + 0.9 * shade) * body
    cv.add(VIOLET, surf * 0.55)
    cv.add(DEEP, surf * 0.5)
    crescent = np.exp(-((r - 0.94) / 0.016) ** 2) * np.clip(np.cos(th - 3.9), 0, 1) ** 1.3        # a bright thin crescent rim
    cv.add(RIM, np.clip(crescent * 1.3, 0, 1) * body)
    cv.add(MAGENTA, np.exp(-((r - 0.92) / 0.03) ** 2) * 0.35 * (0.5 + 0.5 * np.cos(th - 3.9)).clip(0, 1) * body)
    cv.add(VIOLET, np.exp(-((r - 0.945) / 0.012) ** 2) * 0.55 * body)                           # a faint violet limb all round
    put(cv.image(), "darkfx_moon")


def corona():
    s = 512
    x, y, r, th = polar(s)
    rm = 0.40                                                                             # the moon's edge, as a fraction of the texture radius
    d = np.clip(r - rm, 0, None)
    halo = np.exp(-d / 0.05) * 0.85 + np.exp(-d / 0.20) * 0.40
    rays = np.zeros_like(r)
    for seed, kmin, kmax, w in ((8501, 6, 22, 1.0), (8502, 14, 48, 0.7)):
        an = ss(ang_noise(th, seed, kmin, kmax, 14), 0.55, 0.85)
        rays = np.maximum(rays, an * w)
    reach = 0.12 + 0.30 * ang_noise(th, 8503, 2, 9, 6)
    rays = rays * np.exp(-d / reach) * 0.9
    a = np.clip((halo + rays) * ss(r, rm - 0.01, rm + 0.01) * (1 - ss(r, 0.88, 1.0)), 0, 1)
    a *= 0.9 + 0.1 * fbm(s, s, 20, 8504)
    col = mix(VIOLET, MAGENTA, np.clip(ang_noise(th, 8505, 2, 7, 4) * 0.8, 0, 1))
    col = mix(col, RIM, np.clip(np.exp(-d / 0.035) * 1.05, 0, 1))
    cv = Canvas(s, s)
    cv.over(col, a)
    put(cv.image(), "darkfx_corona")


def moonring():
    s = 512
    SSf = 3
    im = Image.new("L", (s * SSf, s * SSf), 0)
    d = ImageDraw.Draw(im)
    c = s * SSf / 2
    rng = np.random.default_rng(8601)

    def circle(rad, wd, gaps=0, gap_w=0.0, val=255, seed=0):
        rr = rad * c
        if not gaps:
            d.ellipse([c - rr, c - rr, c + rr, c + rr], outline=val, width=max(1, int(wd * SSf)))
            return
        g = np.random.default_rng(seed)
        starts = np.sort(g.uniform(0, 360, gaps))
        for i in range(gaps):
            a0 = starts[i] + gap_w * 360
            a1 = starts[(i + 1) % gaps] + (360 if i == gaps - 1 else 0)
            if a1 > a0:
                d.arc([c - rr, c - rr, c + rr, c + rr], a0, a1, fill=val, width=max(1, int(wd * SSf)))

    circle(0.965, 3.2)
    circle(0.93, 1.3, 5, 0.04, 200, 11)
    circle(0.74, 1.8, 4, 0.05, 230, 12)
    circle(0.56, 1.2, 6, 0.03, 170, 13)
    circle(0.30, 1.0)
    for k in range(96):                                                                     # tick marks between the two outer rings
        a = k / 96 * 2 * math.pi
        l0, l1 = (0.935, 0.96) if k % 4 else (0.915, 0.962)
        d.line([c + math.cos(a) * l0 * c, c + math.sin(a) * l0 * c, c + math.cos(a) * l1 * c, c + math.sin(a) * l1 * c], fill=210 if k % 4 else 255, width=max(1, int(1.2 * SSf)))
    for k in range(24):                                                                     # little glyphs on the middle band: strokes and hooks
        a = (k + 0.5) / 24 * 2 * math.pi
        rc = 0.835
        pts = []
        for j in range(int(rng.integers(3, 6))):
            rr = rc + rng.uniform(-0.045, 0.045)
            aa = a + rng.uniform(-0.04, 0.04)
            pts.append((c + math.cos(aa) * rr * c, c + math.sin(aa) * rr * c))
        d.line(pts, fill=215, width=max(1, int(1.5 * SSf)))
    for k in range(8):                                                                      # an eight pointed star {8/3} and crescents
        a0, a1 = k / 8 * 2 * math.pi, ((k + 3) % 8) / 8 * 2 * math.pi
        d.line([c + math.cos(a0) * 0.74 * c, c + math.sin(a0) * 0.74 * c, c + math.cos(a1) * 0.74 * c, c + math.sin(a1) * 0.74 * c], fill=150, width=max(1, int(1.1 * SSf)))
    for k in range(8):
        a = (k + 0.5) / 8 * 2 * math.pi
        rr, cr = 0.50, 0.10
        cx, cy = c + math.cos(a) * rr * c, c + math.sin(a) * rr * c
        d.arc([cx - cr * c, cy - cr * c, cx + cr * c, cy + cr * c], math.degrees(a) + 90, math.degrees(a) + 270, fill=235, width=max(1, int(1.8 * SSf)))
    im = im.resize((s, s), Image.LANCZOS)
    a = np.asarray(im).astype(np.float32) / 255
    soft = np.asarray(im.filter(ImageFilter.GaussianBlur(3.5))).astype(np.float32) / 255
    x, y, r, th = polar(s)
    a = np.clip(a * 1.15 + soft * 0.55, 0, 1) * (1 - ss(r, 0.985, 1.0))
    a += np.exp(-((r - 0.965) / 0.05) ** 2) * 0.12
    put(grey_image(np.clip(a, 0, 1)), "darkfx_moonring")


# ---------------------------------------------------------------- death thrust
def spear():
    w, h = 64, 512
    y = (np.arange(h, dtype=np.float32) + 0.5) / h                                          # 0 tail .. 1 tip
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    hw = ss(y, 0.0, 0.30) ** 0.8 * (1 - ss(y, 0.55, 1.0)) ** 0.9 * 0.50 + 0.012            # a lance: swells fast, long taper to the point
    hw = hw + 0.06 * np.sin(y * 40) * ss(y, 0.0, 0.4) * (1 - y)
    n = fbm(w, h, 8, 8701)
    n2 = fbm(w, h, 20, 8702)
    d = np.abs(X[None, :])
    hwn = hw[:, None] * (1 + 0.30 * (n - 0.5) * (1 - y[:, None]))
    tail_rag = (1 - ss(y, 0.0, 0.35))[:, None]
    spk = spikes(w, h, np.zeros(h, np.float32), hw * 1.6, 8703, 80, 0.9, 2.6) * (0.4 + 0.6 * tail_rag)
    cv = Canvas(w, h)
    glow = (1 - np.clip((d - hwn) / 0.55, 0, 1)) ** 2.2 * (0.55 + 0.6 * n)
    cv.over(mix(VIOLET, MAGENTA, np.clip(n2 * 1.4 - 0.25, 0, 1)), glow * 0.7 * (1 - ss(d, 0.75, 1.0)))
    body = np.maximum(ss(d, hwn + 0.02, hwn - 0.02), spk * 0.75 * (1 - ss(d, 0.5, 0.98)))
    cv.over(BLACK, np.clip(body, 0, 1))
    cv.over(mix(DEEP, BLACK, np.clip(n * 1.5, 0, 1)), np.clip((1 - d / np.maximum(hwn, 1e-3)), 0, 1) ** 2 * 0.0)
    rim = np.exp(-((d - hwn * 0.95) / 0.032) ** 2) * (0.65 + 0.5 * ss(y, 0.05, 0.9))[:, None]
    cv.over(RIM, np.clip(rim * 1.1, 0, 1) * ss(y, 0.0, 0.1)[:, None])
    center = np.exp(-(d / 0.025) ** 2) * ss(y, 0.2, 0.95)[:, None] * (1 - ss(y, 0.93, 1.0))[:, None]      # a hair of light down the middle of the lance
    cv.over(mix(VIOLET, RIM, np.full((h, w), 0.5, np.float32)), np.clip(center * 0.55, 0, 1))
    img = cv.image()
    arr = np.asarray(img).astype(np.float32)
    arr[..., 3] *= ss(y, 0.0, 0.05)[:, None]
    put(Image.fromarray(arr.astype(np.uint8), "RGBA"), "darkfx_spear")
    g = (np.exp(-((d - hwn * 0.95) / 0.07) ** 2) * 0.9 + 0.5 * glow) * ss(y, 0.0, 0.12)[:, None] * (1 - 0.6 * ss(y, 0.95, 1.0))[:, None]
    g = np.maximum(g, spk * 0.5)
    put(grey_image(np.clip(g, 0, 1)), "darkfx_spear_glow")


def shock():
    s = 256
    x, y, r, th = polar(s)
    gap = ss(ang_noise(th, 8801, 3, 14, 8), 0.12, 0.45)
    a = np.exp(-((r - 0.80) / 0.022) ** 2) * (0.35 + 0.65 * gap)
    a += np.exp(-((r - 0.84) / 0.009) ** 2) * 0.6 * gap
    a += np.exp(-((r - 0.60) / 0.012) ** 2) * 0.35 * (1 - gap * 0.5)
    k = 36
    tick = np.maximum(0, np.cos(th * k)) ** 14 * ss(r, 0.86, 0.9) * (1 - ss(r, 0.9, 0.99))      # short radial ticks outside the ring
    a += tick * 0.7 * gap
    a += np.exp(-((r - 0.80) / 0.10) ** 2) * 0.12
    a *= 1 - ss(r, 0.96, 1.0)
    put(grey_image(np.clip(a, 0, 1)), "darkfx_shock")


# ---------------------------------------------------------------- iai
def iai():
    w, h = 32, 512
    y = (np.arange(h, dtype=np.float32) + 0.5) / h
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    taper = np.clip(np.sin(np.pi * y), 0, 1) ** 0.45
    d = np.abs(X[None, :])
    core = np.exp(-(d / (0.075 * (0.5 + 0.5 * taper[:, None]))) ** 2)
    mid = np.exp(-(d / (0.26 * (0.4 + 0.6 * taper[:, None]))) ** 2)
    halo = np.exp(-(d / (0.70 * (0.3 + 0.7 * taper[:, None]))) ** 2)
    n = fbm(w, h, 8, 8901)[:, :1]
    a = np.clip((core * 1.2 + mid * 0.6 + halo * 0.35) * taper[:, None] * (0.85 + 0.3 * n), 0, 1)
    col = mix(MAGENTA, VIOLET, np.clip(d * 1.3, 0, 1))
    col = mix(col, RIM, np.clip(mid * 1.1, 0, 1))
    col = mix(col, np.ones(3, np.float32), np.clip(core * 1.2, 0, 1))
    cv = Canvas(w, h)
    cv.over(col, a)
    put(cv.image(), "darkfx_iai")


def afterimage():
    ds.strip("darkfx_afterimage", 9001, 0.34, 0.045, 0.55, 30, 0.85, 0.85, 0.65)


# ---------------------------------------------------------------- debris, motes, flare
def shard(name, seed):
    s = 128
    rng = np.random.default_rng(seed)

    def fn(dr, k):
        n = int(rng.integers(5, 8))
        pts = []
        for i in range(n):
            a = i / n * 2 * math.pi + rng.uniform(-0.25, 0.25)
            rr = rng.uniform(0.35, 0.95) * (1.0 if i % 2 == 0 else 0.55) * s * 0.46
            pts.append((s / 2 + math.cos(a) * rr * 1.1, s / 2 + math.sin(a) * rr * 0.75))
        dr.polygon([(px * k, py * k) for px, py in pts], fill=255)

    m = mask(s, s, fn, 0.3)
    edge = np.clip(m - np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.2))).astype(np.float32) / 255, 0, 1)
    n = fbm(s, s, 6, seed + 1)
    cv = Canvas(s, s)
    glow = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(5))).astype(np.float32) / 255
    cv.over(VIOLET, np.clip(glow * 0.55, 0, 1) * (1 - m))
    cv.over(BLACK, m)
    cv.over(mix(VIOLET, RIM, ss(n, 0.35, 0.75)), np.clip(edge * 2.8, 0, 1) * m)
    put(cv.image(), name)


def mote():
    s = 128
    x, y, r, th = polar(s)
    n = fbm(s, s, 10, 9101, 4)
    dens = np.clip((0.85 - r) * 1.7 + (n - 0.5) * 1.3, 0, 1)
    cv = Canvas(s, s)
    cv.over(VIOLET, ss(dens, 0.02, 0.3) * 0.65)
    cv.over(BLACK, ss(dens, 0.25, 0.5))
    edge = np.exp(-((dens - 0.38) / 0.07) ** 2) * ss(n, 0.3, 0.7)
    cv.over(mix(VIOLET, RIM, ss(n, 0.4, 0.8)), np.clip(edge * 0.8, 0, 1))
    arr = np.asarray(cv.image()).astype(np.float32)
    arr[..., 3] *= 1 - ss(r, 0.8, 0.98)
    put(Image.fromarray(arr.astype(np.uint8), "RGBA"), "darkfx_mote")


def flare():
    s = 256
    x, y, r, th = polar(s)
    a = np.exp(-(y / 0.022) ** 2 - (x / 0.95) ** 2 * 1.6) * 0.9                            # the long anamorphic streak
    a += np.exp(-(x / 0.03) ** 2 - (y / 0.45) ** 2) * 0.7
    a += 0.4 * (np.exp(-((x - y) / 0.04) ** 2 - ((x + y) / 0.5) ** 2) + np.exp(-((x + y) / 0.04) ** 2 - ((x - y) / 0.5) ** 2))
    a += np.exp(-r * r / 0.012) * 1.0 + np.exp(-r * r / 0.08) * 0.35
    a *= 1 - ss(r, 0.94, 1.0) * 0.0
    a *= 1 - ss(np.abs(x), 0.9, 1.0)
    put(grey_image(np.clip(a, 0, 1)), "darkfx_flare")


def main():
    orb()
    disk()
    photon()
    ripple()
    vignette()
    moon()
    corona()
    moonring()
    spear()
    shock()
    iai()
    afterimage()
    shard("darkfx_shard_a", 9201)
    shard("darkfx_shard_b", 9211)
    mote()
    flare()


if __name__ == "__main__":
    main()

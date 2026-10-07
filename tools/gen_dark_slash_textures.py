"""Textures for Yami's Dark Magic slashes (DarkSlashLayer). Deterministic, procedural (numpy + Pillow, fixed seeds).

    python3 -B tools/gen_dark_slash_textures.py

Palette (see docs/attributes/art_reference/dark_vfx_collection.webp): pitch-black cores, deep violet to magenta light, white-violet rims,
starry specks. Strip textures run along V (v = 0 at the start, 1 at the end) and across U; "baked" ones carry their own colours (drawn with
the ALPHA blend, they hold the black), "glow" ones are grey + alpha (drawn ADD, tinted by the vertex colour).

  textures/particle/dark_slash_tear.png         baked   128x512  the spatial fracture: black core, white-violet rim, jagged violet / magenta aura, stars inside
  textures/particle/dark_slash_cracks.png       baked   512x512  the air cracking like glass round a fracture: black hairlines, violet edge light
  textures/particle/dark_slash_aura.png         glow    128x512  jagged violet halo spikes round a tear (additive)
  textures/particle/dark_slash_ring.png         glow    256x256  warped, broken afterimage rings with a doubled edge (additive)
  textures/particle/dark_slash_stars.png        glow    256x256  starry specks (additive)
  textures/particle/dark_slash_cut.png          baked    64x512  a fast thin cut: black body, white-violet hairline, magenta halo
  textures/particle/dark_slash_cut_glow.png     glow     64x512  the cut's halo (additive)
  textures/particle/dark_slash_crescent.png     baked    64x512  the Avidya wave seen as a ribbon: thick black body, white-violet rim, ragged leading edge
  textures/particle/dark_slash_blade.png        baked    64x512  a long black blade (v = 0 hand, 1 tip) with violet edge light and ragged wisps
  textures/particle/dark_slash_blade_glow.png   glow     64x512  the blade's violet edge light (additive)
  textures/particle/dark_slash_flame_a.png      baked   128x256  a drifting black flame (v = 0 base, 1 tip), violet rim
  textures/particle/dark_slash_flame_b.png      baked   128x256  the same, another shape
  textures/particle/dark_slash_smoke.png        baked   256x256  a black smoke puff with violet edge light
  textures/particle/dark_slash_ash.png          baked   128x128  flakes of black ash with violet glints
  textures/particle/dark_slash_speed.png        glow     32x512  a tapered speed line
  textures/particle/dark_slash_glint.png        glow      64x64  a four-point star glint
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm  # noqa: E402

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 3

BLACK = np.array([0.012, 0.006, 0.022], np.float32)
VIOLET = np.array([0.42, 0.07, 0.82], np.float32)
DEEP = np.array([0.22, 0.03, 0.46], np.float32)
MAGENTA = np.array([0.93, 0.16, 0.76], np.float32)
RIM = np.array([0.93, 0.86, 1.0], np.float32)


def ss(x, a, b):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def mix(a, b, t):
    return a + (b - a) * t[..., None] if np.ndim(t) else a + (b - a) * t


class Canvas:
    """Premultiplied float canvas; layers go on with the 'over' operator."""

    def __init__(self, w, h):
        self.w, self.h = w, h
        self.rgb = np.zeros((h, w, 3), np.float32)
        self.a = np.zeros((h, w), np.float32)

    def over(self, col, alpha):
        alpha = np.clip(alpha, 0, 1).astype(np.float32)
        col = np.broadcast_to(np.asarray(col, np.float32), (self.h, self.w, 3))
        self.rgb = col * alpha[..., None] + self.rgb * (1 - alpha[..., None])
        self.a = alpha + self.a * (1 - alpha)

    def add(self, col, amt):
        """Light that adds to colour without changing the alpha much (glints on the black)."""
        col = np.broadcast_to(np.asarray(col, np.float32), (self.h, self.w, 3))
        self.rgb = np.clip(self.rgb + col * np.clip(amt, 0, 1)[..., None], 0, 1)

    def image(self, fill=VIOLET):
        a = np.clip(self.a, 0, 1)
        rgb = np.where(a[..., None] > 1e-4, self.rgb / np.maximum(a[..., None], 1e-4), fill[None, None, :])
        out = np.dstack([np.clip(rgb, 0, 1) * 255, a * 255]).astype(np.uint8)
        return Image.fromarray(out, "RGBA")


def grey_image(alpha):
    a = np.clip(alpha, 0, 1)
    return Image.fromarray(np.dstack([np.full_like(a, 255), np.full_like(a, 255), np.full_like(a, 255), a * 255]).astype(np.uint8), "RGBA")


def put(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def mask(w, h, fn, blur=0.0):
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im).astype(np.float32) / 255


def centerline(h, seed, amp, step=14):
    """A jagged centre line x(y) in -1..1 units: random walk with hard kinks every 'step' rows."""
    rng = np.random.default_rng(seed)
    knots = rng.uniform(-1, 1, h // step + 2) * amp
    ys = np.arange(h)
    k = ys / step
    i0 = k.astype(int)
    t = k - i0
    t = np.where(rng.random(h // step + 2)[i0] < 0.5, t, ss(t, 0, 1))        # some segments straight (hard kink), some eased
    return knots[i0] * (1 - t) + knots[i0 + 1] * t


def spikes(w, h, cx, hw, seed, count, reach, base_w=3.0, side_bias=0.0):
    """Jagged energy spikes leaving a centre line: triangles from the line outwards, slightly slanted."""
    rng = np.random.default_rng(seed)

    def fn(d, s):
        for _ in range(count):
            y0 = rng.uniform(0.03, 0.97)
            row = int(y0 * (h - 1))
            sgn = 1 if rng.random() < 0.5 + side_bias else -1
            ln = rng.uniform(0.25, 1.0) * reach * hw[row] * w / 2
            slant = rng.uniform(-0.55, 0.55) * ln
            x0 = (cx[row] + 1) * w / 2 + sgn * hw[row] * w / 2 * 0.15
            bw = rng.uniform(1.2, base_w) * (0.4 + hw[row])
            d.polygon([(x0 * s, (y0 * h - bw) * s), (x0 * s, (y0 * h + bw) * s), ((x0 + sgn * ln) * s, (y0 * h + slant) * s)], fill=int(rng.uniform(150, 255)))

    return mask(w, h, fn, 0.5)


# ---------------------------------------------------------------- the tear (baked) and its aura (glow)
def tear():
    """0.58: a real fracture. A wide lens of black void with hard, angular, uneven edges (each side jags on its own), a white-violet rim, light leaking
    out of it as violet / magenta spikes, a nebula and a field of stars inside the void."""
    w, h = 128, 512
    y = (np.arange(h, dtype=np.float32) + 0.5) / h
    taper = np.sin(np.pi * y) ** 0.62
    lens = np.sin(np.pi * y) ** 0.8
    cx = centerline(h, 7001, 0.06) * taper
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    xd = X[None, :] - cx[:, None]
    rng = np.random.default_rng(7010)
    jl = np.repeat(rng.uniform(0.62, 1.38, h // 7 + 2), 7)[:h]                         # angular steps: the left and the right edge jag on their own
    jr = np.repeat(rng.uniform(0.62, 1.38, h // 9 + 2), 9)[:h]
    d = np.abs(xd) / np.where(xd < 0, jl[:, None], jr[:, None])
    n1 = fbm(w, h, 16, 7002)
    n2 = fbm(w, h, 6, 7003)
    rowj = np.repeat(np.random.default_rng(7004).random(h // 3 + 2), 3)[:h]
    edge = 0.92 * taper[:, None] * (0.62 + 0.55 * rowj ** 2.0)[:, None] * (0.8 + 0.45 * n2)
    core_w = (0.34 * lens * (0.9 + 0.2 * n1[:, w // 2]))[:, None] + 0.004
    spk = spikes(w, h, cx, taper * 0.95, 7005, 90, 1.05)

    cv = Canvas(w, h)
    t = np.clip((d - core_w) / np.maximum(edge - core_w, 1e-3), 0, 1)
    halo = (1 - t) ** 1.7 * (0.62 + 0.55 * n1) * ss(d, 0, core_w * 0.6 + 0.01)
    halo = np.maximum(halo * 0.95, spk * (1 - np.clip(d / 1.0, 0, 1)) ** 0.7 * 0.85)
    colr = mix(VIOLET, MAGENTA, np.clip(n2 * 1.3 - 0.25 + 0.5 * t, 0, 1))
    cv.over(colr, halo * 0.9)
    bright = ss(d, 0, core_w * 1.6) * (1 - ss(d, core_w * 1.6, core_w * 4.5)) * halo
    cv.over(mix(MAGENTA, RIM, np.full((h, w), 0.35, np.float32)), bright * 0.55)
    # the void: a nebula and a field of stars deep inside, then the double rim
    inside = ss(d, core_w + 0.012, core_w - 0.012)
    cv.over(BLACK, inside)
    neb = fbm(w, h, 10, 7007, 4)
    depth = np.clip(1 - d / np.maximum(core_w, 1e-3), 0, 1)
    cv.add(DEEP, np.clip((neb - 0.42) * 1.7, 0, 1) * 0.55 * ss(depth, 0.1, 0.7) * inside)
    cv.add(VIOLET, np.clip((fbm(w, h, 5, 7008) - 0.5) * 2.2, 0, 1) * 0.22 * ss(depth, 0.3, 0.9) * inside)
    rim = np.exp(-((d - core_w) / 0.016) ** 2) * (taper[:, None] ** 0.6)
    cv.over(RIM, np.clip(rim * 1.15, 0, 1) * (0.75 + 0.25 * n2))
    cv.over(mix(VIOLET, RIM, np.full((h, w), 0.6, np.float32)), np.clip(np.exp(-((d - core_w * 0.82) / 0.02) ** 2) * 0.55 * taper[:, None], 0, 1) * inside)
    rng = np.random.default_rng(7006)
    star = np.zeros((h, w), np.float32)
    for _ in range(190):
        sy = int(rng.integers(8, h - 8))
        cwid = core_w[sy, 0] * w / 2
        sx = int((cx[sy] + 1) * w / 2 + rng.uniform(-0.85, 0.85) * cwid)
        if not (1 <= sx < w - 1) or cwid < 3:
            continue
        b = rng.uniform(0.45, 1.0)
        star[sy, sx] = b
        if b > 0.75:
            star[sy, sx - 1] = max(star[sy, sx - 1], b * 0.4)
            star[sy, sx + 1] = max(star[sy, sx + 1], b * 0.4)
    cv.add(RIM, star * (cv.a > 0.9))
    put(cv.image(), "dark_slash_tear")


def aura():
    w, h = 128, 512
    y = (np.arange(h, dtype=np.float32) + 0.5) / h
    taper = np.sin(np.pi * y) ** 0.6
    lens = np.sin(np.pi * y) ** 0.8
    cx = centerline(h, 7001, 0.06) * taper
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    d = np.abs(X[None, :] - cx[:, None])
    n = fbm(w, h, 12, 7102)
    spk = spikes(w, h, cx, taper * 0.95, 7103, 140, 1.0, 2.8)
    reach = (0.85 * taper[:, None]) * (0.7 + 0.5 * n)
    soft = (1 - np.clip(d / np.maximum(reach, 1e-3), 0, 1)) ** 2.2
    a = np.maximum(soft * 0.85, spk * 0.8 * (1 - np.clip(d, 0, 1) ** 1.2))
    a = a * (0.55 + 0.55 * ss(n, 0.25, 0.7))
    a *= ss(d, 0.27 * lens[:, None], 0.40 * lens[:, None] + 0.01)          # a hole where the black core sits: the glow is drawn on top of it
    put(grey_image(a), "dark_slash_aura")


def cracks():
    """0.58: the air cracking like glass round a fracture (a square sprite, drawn facing the tear): black hairlines with a violet edge light."""
    s = 512
    rng = np.random.default_rng(7020)
    im = Image.new("L", (s * SS, s * SS), 0)
    d = ImageDraw.Draw(im)
    c = s * SS / 2

    def crack(x0, y0, ang, ln, wd, depth):
        pts = [(x0, y0)]
        x, y = x0, y0
        steps = max(3, int(ln / 30))
        for i in range(steps):
            ang += rng.uniform(-0.30, 0.30)
            sl = ln / steps * rng.uniform(0.6, 1.3)
            x, y = x + math.cos(ang) * sl, y + math.sin(ang) * sl
            pts.append((x, y))
            if depth < 1 and rng.random() < 0.14:
                crack(x, y, ang + rng.choice([-1, 1]) * rng.uniform(0.5, 1.2), ln * rng.uniform(0.25, 0.5), wd * 0.6, depth + 1)
        for i in range(len(pts) - 1):
            d.line([pts[i], pts[i + 1]], fill=255, width=max(1, int(wd * (1 - 0.7 * i / len(pts)))))

    for k in range(11):                                                                   # cracks leave the fracture along its length, mostly sideways
        yy = c + rng.uniform(-0.62, 0.62) * c
        side = 1 if k % 2 == 0 else -1
        ang = (0.0 if side > 0 else math.pi) + rng.uniform(-0.75, 0.75)
        crack(c + side * rng.uniform(0.0, 0.05) * c, yy, ang, rng.uniform(0.5, 1.0) * c * (1.0 - 0.45 * abs(yy - c) / c), 3.4 * SS, 0)
    m = np.asarray(im.resize((s, s), Image.LANCZOS)).astype(np.float32) / 255
    x, y, r, th = polar_grid(s)
    m *= (1 - ss(np.abs(x), 0.8, 1.0)) * (1 - ss(np.abs(y), 0.82, 1.0))
    glow = np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(3.0))).astype(np.float32) / 255
    cv = Canvas(s, s)
    cv.over(mix(VIOLET, MAGENTA, np.clip(fbm(s, s, 6, 7021) * 1.3 - 0.2, 0, 1)), np.clip(glow * 1.5, 0, 1) * 0.7)
    cv.over(BLACK, np.clip(m * 1.3, 0, 1))
    edge = np.clip(m - np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.2))).astype(np.float32) / 255, 0, 1)
    cv.over(RIM, np.clip(edge * 2.6, 0, 1))
    put(cv.image(), "dark_slash_cracks")


def polar_grid(s):
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x, y = (xx + 0.5) / s * 2 - 1, (yy + 0.5) / s * 2 - 1
    return x, y, np.sqrt(x * x + y * y), np.arctan2(y, x)


# ---------------------------------------------------------------- ring, stars, glint, speed
def ring():
    s = 256
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x, y = (xx + 0.5) / s * 2 - 1, (yy + 0.5) / s * 2 - 1
    r = np.sqrt(x * x + y * y)
    th = np.arctan2(y, x)
    wob = 0.045 * np.sin(th * 3 + 0.7) + 0.03 * np.sin(th * 7 + 2.1) + 0.02 * np.sin(th * 13 + 4.0)
    gap = ss(np.sin(th * 2 + 0.4) + 0.5 * np.sin(th * 5 + 1.3), -0.9, -0.2)               # the ring is broken in places
    a = np.zeros_like(r)
    for rad, wd, k, off in ((0.80, 0.018, 1.0, 0.0), (0.84, 0.010, 0.55, 0.012), (0.64, 0.012, 0.6, -0.01), (0.45, 0.008, 0.35, 0.0)):
        rr = np.abs(r - (rad + wob * (1.0 + off * 10) + off))
        a = np.maximum(a, np.exp(-(rr / wd) ** 2) * k)
    a *= 0.35 + 0.65 * gap
    a += np.exp(-((r - 0.80 - wob) / 0.09) ** 2) * 0.16                                       # a soft lens halo under the thin lines
    a *= 1 - ss(r, 0.93, 1.0)
    put(grey_image(a), "dark_slash_ring")


def stars():
    s = 256
    rng = np.random.default_rng(7201)
    a = np.zeros((s, s), np.float32)
    for _ in range(190):
        x, y = rng.integers(2, s - 2, 2)
        b = rng.uniform(0.35, 1.0) ** 1.5
        a[y, x] = max(a[y, x], b)
        if b > 0.55:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                a[y + dy, x + dx] = max(a[y + dy, x + dx], b * 0.35)
    for _ in range(14):
        x, y = rng.integers(10, s - 10, 2)
        for k in range(1, 7):
            f = (1 - k / 7) ** 1.6
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                a[y + dy * k, x + dx * k] = max(a[y + dy * k, x + dx * k], f * 0.8)
        a[y, x] = 1
    a = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.45))).astype(np.float32) / 255 * 1.6
    put(grey_image(a), "dark_slash_stars")


def glint():
    s = 64
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x, y = (xx + 0.5) / s * 2 - 1, (yy + 0.5) / s * 2 - 1
    a = np.exp(-(x / 0.04) ** 2 - (y / 0.7) ** 2) + np.exp(-(y / 0.04) ** 2 - (x / 0.7) ** 2)
    a += 0.55 * (np.exp(-((x - y) / 0.05) ** 2 - ((x + y) / 0.55) ** 2) + np.exp(-((x + y) / 0.05) ** 2 - ((x - y) / 0.55) ** 2))
    a += np.exp(-(x * x + y * y) / 0.012)
    put(grey_image(np.clip(a, 0, 1)), "dark_slash_glint")


def speed():
    w, h = 32, 512
    y = (np.arange(h, dtype=np.float32) + 0.5) / h
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    prof = np.sin(np.pi * y) ** 0.8 * (0.35 + 0.65 * y)                                       # fades in at the tail, stays hot toward the head
    n = fbm(w, h, 4, 7301)[:, :1]
    a = np.exp(-(X[None, :] / (0.12 * (0.4 + prof[:, None]))) ** 2) * prof[:, None] * (0.7 + 0.5 * n)
    put(grey_image(a), "dark_slash_speed")


# ---------------------------------------------------------------- baked strips: cut, crescent
def strip(name, seed, body, rim_w, taper_pow, spike_n, spike_reach, glow_w, rough):
    """A tapered ribbon along V: black body, white-violet rim, violet-to-magenta halo, optional ragged spikes."""
    w, h = 64, 512
    y = (np.arange(h, dtype=np.float32) + 0.5) / h
    taper = np.clip(np.sin(np.pi * y), 0, 1) ** taper_pow
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    cx = centerline(h, seed, 0.05, 40) * taper
    d = np.abs(X[None, :] - cx[:, None])
    n = fbm(w, h, 8, seed + 1)
    bw = body * taper[:, None] * (1 + rough * (n - 0.5))
    gw = glow_w * taper[:, None] * (0.8 + 0.5 * n)
    spk = spikes(w, h, cx, taper, seed + 2, spike_n, spike_reach, 2.4) if spike_n else np.zeros((h, w), np.float32)
    cv = Canvas(w, h)
    halo = (1 - np.clip((d - bw) / np.maximum(gw - bw, 1e-3), 0, 1)) ** 2.0
    halo = np.maximum(halo * 0.8, spk * 0.7 * (1 - np.clip(d, 0, 1)))
    cv.over(mix(VIOLET, MAGENTA, np.clip(n * 1.4 - 0.2, 0, 1)), halo * ss(d, 0, bw * 0.5 + 1e-3))
    cv.over(BLACK, ss(d, bw + 0.03, bw - 0.03))
    rim = np.exp(-((d - bw) / rim_w) ** 2) * taper[:, None] ** 0.5
    cv.over(RIM, np.clip(rim * 1.2, 0, 1))
    cv.over(BLACK, np.clip(spk * 0.6, 0, 1) * ss(d, bw * 0.9, bw * 1.2) * (1 - ss(d, bw * 2.2, 1.0)))     # black fingers of darkness
    put(cv.image(), name)
    return d, taper, gw, n


def cut():
    d, taper, gw, n = strip("dark_slash_cut", 7401, 0.17, 0.045, 1.25, 10, 0.7, 0.62, 0.35)
    glow = (1 - np.clip(d / np.maximum(gw, 1e-3), 0, 1)) ** 2.4 * taper[:, None] ** 0.8 * (0.7 + 0.6 * n)
    glow += np.exp(-(d / 0.06) ** 2) * taper[:, None]
    put(grey_image(glow), "dark_slash_cut_glow")


def crescent():
    strip("dark_slash_crescent", 7501, 0.40, 0.05, 0.85, 34, 0.9, 0.90, 0.55)


# ---------------------------------------------------------------- the black blade
def blade():
    w, h = 64, 512
    y = (np.arange(h, dtype=np.float32) + 0.5) / h
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    hw = np.where(y < 0.88, 0.44 + 0.04 * np.sin(y * 9), 0.44 * np.clip((1 - y) / 0.12, 0, 1) ** 0.8)
    hw = hw + 0.02
    cx = 0.04 * (y - 0.4) * (y > 0.88) * 3.0
    d = np.abs(X[None, :] - cx[:, None])
    n = fbm(w, h, 6, 7601)
    n2 = fbm(w, h, 16, 7602)
    hwn = hw[:, None] * (1 + 0.34 * (n - 0.5) + 0.12 * (n2 - 0.5))
    spk = spikes(w, h, cx, hw * 1.4, 7603, 90, 0.75, 2.4)
    cv = Canvas(w, h)
    glow = (1 - np.clip((d - hwn) / 0.5, 0, 1)) ** 2.0 * (0.55 + 0.6 * n)
    cv.over(mix(VIOLET, MAGENTA, np.clip(n2 * 1.4 - 0.25, 0, 1)), glow * 0.75 * (1 - ss(d, 0.7, 1.0)))
    cv.over(mix(DEEP, BLACK, np.clip(n * 1.6, 0, 1)), np.maximum(ss(d, hwn + 0.02, hwn - 0.02), spk * 0.7 * (1 - ss(d, 0.55, 0.95))))
    inner = np.clip(1 - d / np.maximum(hwn, 1e-3), 0, 1)
    cv.over(BLACK, ss(inner, 0.0, 0.55) * ss(d, hwn + 0.02, hwn - 0.02))
    rim = np.exp(-((d - hwn * 0.96) / 0.03) ** 2)
    cv.over(RIM, np.clip(rim * (0.55 + 0.6 * ss(y, 0.05, 0.9))[:, None] * 1.1, 0, 1))
    # the base melts into the hand-cloud
    img = cv.image()
    arr = np.asarray(img).astype(np.float32)
    arr[..., 3] *= ss(y, 0.0, 0.07)[:, None]
    put(Image.fromarray(arr.astype(np.uint8), "RGBA"), "dark_slash_blade")
    g = (np.exp(-((d - hwn * 0.96) / 0.06) ** 2) + 0.35 * glow) * ss(y, 0.0, 0.1)[:, None]
    g = np.maximum(g, spk * 0.5)
    put(grey_image(g), "dark_slash_blade_glow")


# ---------------------------------------------------------------- flames, smoke, ash
def flame(name, seed, lean):
    w, h = 128, 256
    y = (np.arange(h, dtype=np.float32) + 0.5) / h                                      # 0 base .. 1 tip
    X = (np.arange(w, dtype=np.float32) + 0.5) / w * 2 - 1
    sway = lean * (y ** 1.6) + 0.12 * np.sin(y * 7 + seed)
    hw = 0.52 * np.sin(np.pi * np.clip(y * 0.92 + 0.04, 0, 1)) ** 0.8 * (1 - y) ** 0.5 + 0.015
    hw = hw * (1 + 0.5 * y * np.sin(y * 19 + seed * 0.7))
    d = (X[None, :] - sway[:, None])
    n = fbm(w, h, 8, seed)
    n2 = fbm(w, h, 24, seed + 3)
    hwn = hw[:, None] * (0.8 + 0.5 * n)
    ad = np.abs(d)
    shape = ss(ad, hwn, hwn * 0.55)
    tongues = ss(n2, 0.55, 0.8) * ss(ad, hwn * 0.5, hwn * 1.05) * (1 - ss(ad, hwn * 1.05, hwn * 1.5)) * (1 - y[:, None]) ** 0.5
    sh = np.clip(shape + tongues * 0.8, 0, 1)
    cv = Canvas(w, h)
    cv.over(mix(VIOLET, MAGENTA, np.clip(n2 * 1.3, 0, 1)), np.clip(ss(ad, hwn * 1.5, hwn * 0.5) * 0.5, 0, 1) * (1 - y[:, None] ** 2))
    cv.over(BLACK, sh)
    edge = np.exp(-((ad - hwn * 0.82) / (0.035 + 0.02 * (1 - y[:, None]))) ** 2) * sh
    cv.over(mix(VIOLET, RIM, np.clip(y[:, None] * 0.5 + 0.2, 0, 1) * np.ones((h, w), np.float32)), np.clip(edge * 1.0, 0, 1))
    img = cv.image()
    arr = np.asarray(img).astype(np.float32)
    arr[..., 3] *= (ss(y, 0.0, 0.06) * (1 - ss(y, 0.88, 1.0)))[:, None]
    put(Image.fromarray(arr.astype(np.uint8), "RGBA"), name)


def smoke():
    s = 256
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x, y = (xx + 0.5) / s * 2 - 1, (yy + 0.5) / s * 2 - 1
    r = np.sqrt(x * x + y * y)
    n = fbm(s, s, 32, 7701, 5)
    n2 = fbm(s, s, 8, 7702, 4)
    dens = np.clip((0.95 - r) * 1.5 + (n - 0.5) * 1.5, 0, 1)
    body = ss(dens, 0.25, 0.55)
    cv = Canvas(s, s)
    cv.over(mix(VIOLET, MAGENTA, np.clip(n2 * 1.3 - 0.2, 0, 1)), ss(dens, 0.02, 0.3) * 0.7)
    cv.over(BLACK, body)
    edge = np.exp(-((dens - 0.42) / 0.07) ** 2) * ss(n2, 0.25, 0.7)
    cv.over(mix(VIOLET, RIM, ss(n, 0.4, 0.8)), np.clip(edge * 0.75, 0, 1))
    img = cv.image()
    arr = np.asarray(img).astype(np.float32)
    arr[..., 3] *= 1 - ss(r, 0.85, 1.0)
    put(Image.fromarray(arr.astype(np.uint8), "RGBA"), "dark_slash_smoke")


def ash():
    s = 128
    rng = np.random.default_rng(7801)

    def fn(d, k):
        for _ in range(16):
            cx, cy = rng.uniform(12, s - 12, 2)
            r = rng.uniform(3.5, 9.0)
            pts = [(cx + math.cos(a) * r * rng.uniform(0.45, 1.15), cy + math.sin(a) * r * rng.uniform(0.3, 1.0)) for a in np.linspace(0, 2 * math.pi, 7)[:-1]]
            d.polygon([(px * k, py * k) for px, py in pts], fill=255)

    m = mask(s, s, fn, 0.35)
    n = fbm(s, s, 6, 7802)
    cv = Canvas(s, s)
    cv.over(BLACK, m)
    edge = np.clip(m - np.asarray(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.6))).astype(np.float32) / 255, 0, 1)
    cv.over(mix(VIOLET, RIM, ss(n, 0.4, 0.75)), np.clip(edge * 2.6, 0, 1) * m)
    put(cv.image(), "dark_slash_ash")


def main():
    tear()
    aura()
    cracks()
    ring()
    stars()
    glint()
    speed()
    cut()
    crescent()
    blade()
    flame("dark_slash_flame_a", 7901, 0.18)
    flame("dark_slash_flame_b", 7911, -0.22)
    smoke()
    ash()


if __name__ == "__main__":
    main()

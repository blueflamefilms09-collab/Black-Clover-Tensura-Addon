"""Generates the Demon Fire Magic VFX textures (the "inverted" fire: a pitch-black flame body with a violet-white light along its edge).

Deterministic (fixed seeds), no fonts, no external images. Output: src/main/resources/assets/nusmp/textures/particle/demon_fire_*.png

    python3 -B tools/gen_demon_fire_textures.py

Look: corrupted fire with the colours turned inside out. Ordinary fire is white-hot in the middle and red at the rim; Demon Fire is black in the
middle and burns with light only along its rim, in violet that tips into lilac-white where the flame is thinnest. The sprites are GREY with
alpha, so the vertex colour tints the rim (the effect colour) while the body stays black. Drawn twice, a sprite gives both halves of the look:
with ALPHA blending the black body covers the scene, with ADD blending (a second pass of the same quad) only the light of the rim is added.

Sprites (all names demon_fire_*):
  tongue   256x256  two tall flame tongues (128x256 each): black body, flowing inner streaks, nested contour line, glowing rim
  orb      128x128  the projectile body: a black disc (an eclipse) with a ragged limb and a bright crescent of light on one side
  corona   256x256  additive halo and curling flare rays that hug the disc; the centre is hollow so the black disc stays black
  trail    64x256   vertically tiling stream of black flame with bright fibres, for the comet tail and the afterimage
  sigil    256x256  ground sigil linework: flame-toothed ring, rune band, octagram, the inverted flame glyph in the middle
  scorch   256x256  the burnt ground: a ragged black patch with glowing cracks
  wall     256x128  horizontally tiling wall of tall flames (the fire wall round a zone)
  swirl    256x256  five spiral arms of black flame (the twister seen head on)
  flash    256x256  additive star flash: hot core, long thin rays
  inverse  256x256  negative-blend burst: grey value = how much the picture behind is inverted (alpha is 1 everywhere)
  ring     256x256  shock ring with flame teeth on the outer edge
  ember    64x64    teardrop spark, bright head at the bottom, tail up
  shard    128x128  four angular shards of burnt glass (2x2): dark facets, bright edges
  smoke    128x128  ash cloud
  runes    128x128  four demonic glyphs (2x2)
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4  # supersampling for drawn shapes


# ------------------------------------------------------------------------------------------------ plumbing
def save(lum, alpha, name):
    """lum / alpha: float arrays 0..1 -> grey RGBA PNG."""
    lum = np.clip(lum, 0, 1)
    alpha = np.clip(alpha, 0, 1)
    rgb = (lum * 255 + 0.5).astype(np.uint8)
    img = np.dstack([rgb, rgb, rgb, (alpha * 255 + 0.5).astype(np.uint8)])
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT, "demon_fire_" + name + ".png"), optimize=True)
    print("wrote demon_fire_" + name, lum.shape[::-1])


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx + 0.5, yy + 0.5


def polar(s):
    """Normalised coordinates of an s x s image: x, y in -1..1 (y down), radius, angle."""
    xx, yy = grid(s, s)
    x, y = (xx - s / 2) / (s / 2), (yy - s / 2) / (s / 2)
    return x, y, np.hypot(x, y), np.arctan2(y, x)


def sstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def vnoise(w, h, cx, cy, seed, tile=True):
    """Smooth value noise 0..1 with its own cell size per axis; tiles when asked."""
    rng = np.random.default_rng(seed)
    gw, gh = max(1, int(round(w / cx))), max(1, int(round(h / cy)))
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    if tile:
        g[-1, :] = g[0, :]
        g[:, -1] = g[:, 0]
    xx, yy = grid(w, h)
    fx, fy = (xx - 0.5) / w * gw, (yy - 0.5) / h * gh
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, cx, cy, seed, octaves=4, tile=True):
    n, amp, tot = 0, 1.0, 0.0
    for o in range(octaves):
        n = n + vnoise(w, h, max(2.0, cx / (2 ** o)), max(2.0, cy / (2 ** o)), seed + 31 * o, tile) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def ang_noise(theta, n, seed):
    """Smooth periodic noise 0..1 around a circle (n control points)."""
    rng = np.random.default_rng(seed)
    g = rng.random(n).astype(np.float32)
    u = (theta % (2 * np.pi)) / (2 * np.pi) * n
    i0 = np.floor(u).astype(int) % n
    i1 = (i0 + 1) % n
    t = u - np.floor(u)
    t = t * t * (3 - 2 * t)
    return g[i0] * (1 - t) + g[i1] * t


def blur(a, sigma, mode="constant"):
    """Gaussian blur of a float array (FFT; zero padded unless told otherwise)."""
    pad = int(sigma * 3) + 2
    p = np.pad(a, pad, mode=mode)
    fy = np.fft.fftfreq(p.shape[0])[:, None]
    fx = np.fft.rfftfreq(p.shape[1])[None, :]
    k = np.exp(-2 * (np.pi ** 2) * (sigma ** 2) * (fx ** 2 + fy ** 2))
    r = np.fft.irfft2(np.fft.rfft2(p) * k, s=p.shape)
    return r[pad:-pad, pad:-pad].astype(np.float32)


def draw_mask(w, h, fn):
    """Draw white shapes with PIL at SS x resolution (fn(draw, k)) and return the antialiased float mask."""
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    return np.asarray(im.resize((w, h), Image.LANCZOS), np.float32) / 255


def glow_lines(mask, tight=1.6, wide=6.0, k1=0.55, k2=0.25):
    """Line work plus its own soft glow (what makes thin white lines read as light)."""
    return np.clip(mask + k1 * blur(mask, tight) + k2 * blur(mask, wide), 0, 1)


# ------------------------------------------------------------------------------------------------ the flame shapes
def flame_field(w, h, tongues, seed, periodic=False, jag=7.0, fine=1.8):
    """Union of flame tongues. Returns (d, vl): d = rough distance in px from the edge (positive inside), vl = 0 at a tongue's base .. 1 at its tip.

    tongue = dict(x0 centre of the base in px, v0 base height 0..1, hh height as a fraction of the image, w0 half width of the base in px,
                  lean px the tip is pushed sideways, sway px of S-curve, freq / phase of the S, pw / pq shape of the profile,
                  lobe amplitude of the unequal flickers along the two edges)."""
    xx, yy = grid(w, h)
    v = 1.0 - yy / h
    wob = (fbm(w, h, 30, 46, seed, 3, tile=periodic) - 0.5) * 2 * jag + (fbm(w, h, 9, 15, seed + 77, 2, tile=periodic) - 0.5) * 2 * fine
    best = np.full((h, w), -1e3, np.float32)
    vbest = np.zeros((h, w), np.float32)
    for ti, t in enumerate(tongues):
        vl = (v - t["v0"]) / t["hh"]
        ok = (vl >= 0) & (vl <= 1.0)
        c = np.clip(vl, 0, 1)
        cx = t["x0"] + t.get("lean", 0) * c ** 1.7 + t.get("sway", 0) * np.sin(c * t.get("freq", 3) * math.pi + t.get("phase", 0)) * c ** 0.7
        dx = xx - cx
        if periodic:
            dx = (dx + w / 2) % w - w / 2
        prof = (1 - c ** t.get("pw", 1.5)) ** t.get("pq", 0.8)
        a = t.get("lobe", 0.0)
        ph = t.get("phase", 0.0)
        lobe = np.where(dx > 0, 1 + a * np.sin(c * 9.0 + ph * 1.7 + ti), 1 + a * np.sin(c * 7.3 + ph * 0.6 + 2.0 * ti + 1.3))
        d = t["w0"] * prof * lobe - np.abs(dx) + wob * (1 - c ** 2.5) * (0.35 + 0.65 * prof)
        d = np.where(ok, d, -1e3)
        upd = d > best
        best = np.where(upd, d, best)
        vbest = np.where(upd, c, vbest)
    return best, vbest


def lick(main, v0, side, hh, w0, lean):
    """A small flame lick growing out of the edge of 'main' at height v0 (side -1 / +1)."""
    c = (v0 - main["v0"]) / main["hh"]
    cx = main["x0"] + main.get("lean", 0) * c ** 1.7 + main.get("sway", 0) * math.sin(c * main.get("freq", 3) * math.pi + main.get("phase", 0)) * c ** 0.7
    hw = main["w0"] * (1 - c ** main.get("pw", 1.5)) ** main.get("pq", 0.8)
    return dict(x0=cx + side * hw * 0.72, v0=v0, hh=hh, w0=w0, lean=side * lean, sway=2, freq=2.0, phase=side * 1.3 + v0 * 9, pw=1.2, pq=0.9)


def flame_sprite(w, h, tongues, seed, periodic=False, base_fade=0.07, rim=1.0):
    """Black flame with a bright rim, a flowing inner texture and a nested contour line. Returns (lum, alpha)."""
    d, vl = flame_field(w, h, tongues, seed, periodic)
    inner = [dict(t, w0=t["w0"] * 0.50, hh=t["hh"] * 0.76, v0=t["v0"] + 0.025) for t in tongues]
    d2, _ = flame_field(w, h, inner, seed + 5, periodic, jag=3.0, fine=1.0)
    inside = np.clip(d, 0, None)
    alpha = np.clip(d / 1.3 + 0.5, 0, 1)
    xx, yy = grid(w, h)
    v = 1.0 - yy / h
    alpha = alpha * sstep(0.0, base_fade, v)
    rim1 = np.exp(-inside / 2.0)                         # the sharp lit edge
    rim2 = np.exp(-inside / 9.0)                         # the soft light that creeps inwards
    heat = 0.72 + 0.55 * vl                              # thin tips burn brightest
    st = fbm(w, h, 5, 64, seed + 11, 3, tile=periodic)
    streak = sstep(0.42, 0.85, st)                       # flowing streaks inside the black
    fill2 = sstep(-1.5, 3.0, d2)
    contour = np.exp(-(d2 / 1.5) ** 2) * (d > 3)
    lum = 0.022 + 0.085 * fill2 + 0.10 * streak * (0.35 + 0.65 * rim2) + rim * (0.85 * rim1 * heat + 0.30 * rim2 * heat) + 0.55 * contour
    return lum, alpha


def licks_d(r, th, r0, specs, px):
    """Rough distance (px) inside the union of curved flame licks growing outward from radius r0 (normalised units).
    specs = list of (angle, length, half width as an arc, curl in radians); positive inside, -1000 outside."""
    best = np.full_like(r, -1e3)
    rho = r - r0
    for a, L, hw, curl in specs:
        c = np.clip(rho / L, 0, 1)
        centre = a + curl * c ** 1.3
        dth = (th - centre + np.pi) % (2 * np.pi) - np.pi
        arc = np.abs(dth) * np.maximum(r, 1e-3)
        d = (hw * (1 - c) ** 1.25 - arc) * px
        d = np.where((rho > -0.05) & (rho < L), d, -1e3)
        best = np.maximum(best, d)
    return best


# ------------------------------------------------------------------------------------------------ textures
def tongue():
    """Two flame tongues side by side (128 x 256 each). The base is the bottom edge, the tip the top."""
    A0 = dict(x0=62, v0=0.0, hh=0.97, w0=33, lean=22, sway=11, freq=3.0, phase=0.4, pw=1.15, pq=1.15, lobe=0.16)
    A1 = dict(x0=34, v0=0.0, hh=0.55, w0=20, lean=-16, sway=6, freq=2.4, phase=2.0, pw=1.1, pq=1.2, lobe=0.12)
    A2 = dict(x0=94, v0=0.03, hh=0.42, w0=15, lean=18, sway=4, freq=2.6, phase=1.0, pw=1.1, pq=1.2, lobe=0.1)
    A = [A0, A1, A2, lick(A0, 0.30, -1, 0.17, 8, 14), lick(A0, 0.46, 1, 0.19, 8, 16), lick(A0, 0.62, -1, 0.15, 6, 12), lick(A1, 0.22, -1, 0.14, 6, 10)]
    B0 = dict(x0=58, v0=0.0, hh=1.0, w0=29, lean=-20, sway=15, freq=3.7, phase=2.4, pw=1.1, pq=1.2, lobe=0.15)
    B1 = dict(x0=82, v0=0.02, hh=0.66, w0=22, lean=15, sway=8, freq=3.0, phase=0.8, pw=1.1, pq=1.2, lobe=0.12)
    B2 = dict(x0=30, v0=0.0, hh=0.34, w0=14, lean=-11, sway=4, freq=2.2, phase=0.2, pw=1.1, pq=1.15, lobe=0.1)
    B = [B0, B1, B2, lick(B0, 0.28, 1, 0.16, 7, 13), lick(B0, 0.50, -1, 0.18, 8, 15), lick(B0, 0.70, 1, 0.14, 6, 12), lick(B1, 0.30, 1, 0.14, 6, 10)]
    la, aa = flame_sprite(128, 256, A, 101)
    lb, ab = flame_sprite(128, 256, B, 202)
    save(np.hstack([la, lb]), np.hstack([aa, ab]), "tongue")


def orb():
    """The projectile body: a black disc (an eclipse) with long curling licks of black flame, a bright crescent of light on the upper left."""
    s = 128
    x, y, r, th = polar(s)
    px = s / 2
    r0 = 0.56
    rng = np.random.default_rng(12)
    specs = []
    a = rng.uniform(0, 1)
    for k in range(10):
        a += 2 * np.pi / 10 * rng.uniform(0.7, 1.3)
        specs.append((a, rng.choice([0.12, 0.17, 0.22, 0.30, 0.38], p=[0.2, 0.25, 0.2, 0.2, 0.15]), rng.uniform(0.055, 0.10), rng.uniform(0.55, 0.95)))
    d = np.maximum((r0 + 0.012 * (ang_noise(th, 20, 3) - 0.5) * 2 - r) * px, licks_d(r, th, r0 - 0.02, specs, px))
    alpha = np.clip(d / 1.2 + 0.5, 0, 1)
    inside = np.clip(d, 0, None)
    light = 0.55 + 0.45 * np.clip(np.cos(th + 2.356), 0, 1)                            # light from the upper left
    swirl = 0.5 + 0.5 * np.sin(5 * (th - 2.0 * r) + 7 * r)
    lum = (0.02 + 0.05 * swirl * sstep(0.1, 0.55, r) + 0.14 * np.exp(-((r - 0.42) / 0.012) ** 2)
           + (0.30 + 0.70 * light) * (0.95 * np.exp(-inside / 2.2) + 0.40 * np.exp(-inside / 7.0)) + 0.22 * np.exp(-inside / 1.8))
    lum = lum + 0.65 * light * np.exp(-np.abs(d - 2.5) / 2.0) * (r < r0 + 0.02)    # the crescent: a lit limb inside the edge
    save(blur(lum, 0.6, "edge"), blur(alpha, 0.6), "orb")


def corona():
    """Additive halo hugging the disc (the disc sits at radius 0.21 when this quad is 3x the orb quad) with curling flares."""
    s = 256
    x, y, r, th = polar(s)
    r0 = 0.21
    out = np.clip(r - r0, 0, None)
    halo = np.exp(-out / 0.075) * 0.9 + np.exp(-out / 0.26) * 0.34
    rng = np.random.default_rng(31)
    flares = np.zeros_like(r)
    for k in range(15):
        a = 2 * np.pi * k / 15 + rng.uniform(-0.12, 0.12)
        length = rng.uniform(0.16, 0.62)
        width = rng.uniform(0.006, 0.018)
        dth = (th - a + 1.7 * out + np.pi) % (2 * np.pi) - np.pi                   # rays curl with the radius
        arc = np.abs(dth) * r
        along = np.clip(1 - out / length, 0, 1) ** 1.35
        flares = np.maximum(flares, np.exp(-(arc / (width + 0.03 * along)) ** 2) * along * rng.uniform(0.6, 1.0))
    sparkle = 0.82 + 0.18 * fbm(s, s, 12, 12, 8, 3)
    lum = np.clip((halo + flares * 1.1) * sparkle, 0, 1) * sstep(r0 - 0.035, r0 + 0.005, r) * sstep(1.0, 0.72, r)
    lum = lum + 0.0
    save(np.ones_like(lum), lum, "corona")


def trail():
    """A vertically tiling stream of black flame. v grows towards the TAIL: every lick is wide at the top and thins to a point towards the
    bottom, as flames streaming away from the head do. Bright fibres run along it; the rim is lit."""
    w, h = 64, 256
    xx, yy = grid(w, h)
    u = (xx - w / 2) / (w / 2)
    v = yy / h
    n1 = vnoise(w, h, 64, 64, 4)
    n2 = vnoise(w, h, 64, 21, 5)

    rng = np.random.default_rng(8)
    amps = [rng.uniform(0.55, 1.25, 8) for _ in range(2)]

    def saw(K, ph, which):
        t = v * K + ph + 0.30 * n1
        f = t % 1.0
        idx = np.floor(t).astype(int) % K
        a = amps[which][idx]
        ramp = sstep(0.0, 0.10, f)                       # the jump to a new lick is steep but not a step
        return (1 - f) ** 1.9 * ramp * a
    left = 0.20 + 0.50 * saw(3, 0.0, 0) + 0.10 * (n2 - 0.5)
    right = 0.20 + 0.50 * saw(4, 0.37, 1) + 0.10 * (n2 - 0.5)
    hw = np.where(u < 0, left, right)
    d = (hw - np.abs(u)) * (w / 2)
    alpha = np.clip(d / 1.6 + 0.5, 0, 1)
    inside = np.clip(d, 0, None)
    body = 0.025 + 0.09 * sstep(0.35, 0.8, fbm(w, h, 6, 56, 6, 3))
    fibres = np.zeros_like(u)
    for k in range(5):
        uk = -0.62 + 0.31 * k + 0.07 * np.sin(2 * np.pi * v * (1 + k % 3) + k * 1.3)
        n = 0.35 + 0.65 * vnoise(w, h, 64, 24 + 8 * k, 20 + k)
        fibres = np.maximum(fibres, np.exp(-((u - uk) / 0.05) ** 2) * n)
    lum = body + 0.62 * fibres * sstep(0, 5, d) + 0.9 * np.exp(-inside / 2.0) + 0.28 * np.exp(-inside / 7.0)
    save(lum, alpha, "trail")


def _rune_strokes(d, k, cx, cy, rho_c, drho, dth, theta, rng, n_nodes=3):
    """One rune on the band: a few straight strokes between random nodes of a 3 x 3 lattice, bent round the ring."""
    nodes = [(i, j) for i in range(3) for j in range(3)]
    pick = [nodes[i] for i in rng.permutation(9)[:n_nodes + 1]]
    pts = []
    for (i, j) in pick:
        s_, t_ = (i - 1) / 1.0, (j - 1) / 1.0
        rho = rho_c + t_ * drho
        a = theta + s_ * dth
        pts.append((cx + math.cos(a) * rho, cy + math.sin(a) * rho))
    d.line(pts, fill=255, width=int(1.3 * k), joint="curve")


def sigil():
    """The ground sigil (additive line work): a crown of flame spikes, a rune band, an octagram, the inverted flame glyph."""
    s = 256
    S = s * SS
    c = S / 2
    rng = np.random.default_rng(77)

    def art(d, k):
        R = S / 2

        def ring(rho, wd):
            d.ellipse([c - rho * R, c - rho * R, c + rho * R, c + rho * R], outline=255, width=int(wd * k))
        ring(0.975, 1.7)
        ring(0.935, 1.0)
        ring(0.62, 1.4)
        ring(0.585, 0.8)
        ring(0.22, 1.2)
        for i in range(96):                                                       # tick marks between the two outer rings
            a = 2 * math.pi * i / 96
            r0, r1 = (0.935, 0.975) if i % 4 else (0.915, 0.975)
            d.line([(c + math.cos(a) * r0 * R, c + math.sin(a) * r0 * R), (c + math.cos(a) * r1 * R, c + math.sin(a) * r1 * R)], fill=255, width=int(0.9 * k))
        for i in range(12):                                                       # crown of flame spikes
            a = 2 * math.pi * i / 12
            w = math.pi / 12 * 0.55
            tip = (c + math.cos(a) * 0.80 * R * 1.0, c + math.sin(a) * 0.80 * R)
            base_l = (c + math.cos(a - w) * 0.66 * R, c + math.sin(a - w) * 0.66 * R)
            base_r = (c + math.cos(a + w) * 0.66 * R, c + math.sin(a + w) * 0.66 * R)
            mid_l = (c + math.cos(a - w * 0.35) * 0.74 * R, c + math.sin(a - w * 0.35) * 0.74 * R)
            mid_r = (c + math.cos(a + w * 0.35) * 0.74 * R, c + math.sin(a + w * 0.35) * 0.74 * R)
            d.line([base_l, mid_l, tip, mid_r, base_r], fill=255, width=int(1.1 * k), joint="curve")
        for i in range(24):                                                       # rune band between rings 0.83 and 0.92 -> drawn outside the crown tips
            theta = 2 * math.pi * (i + 0.5) / 24
            _rune_strokes(d, k, c, c, 0.862 * R, 0.040 * R, math.pi / 24 * 0.62, theta, rng, 3)
        pts = [(c + math.cos(2 * math.pi * (j * 3 % 8) / 8 - math.pi / 2) * 0.58 * R, c + math.sin(2 * math.pi * (j * 3 % 8) / 8 - math.pi / 2) * 0.58 * R) for j in range(9)]
        d.line(pts, fill=255, width=int(1.3 * k), joint="curve")                  # octagram {8/3}
        pts = [(c + math.cos(2 * math.pi * (j * 1 % 8) / 8 - math.pi / 2 + math.pi / 8) * 0.40 * R, c + math.sin(2 * math.pi * (j % 8) / 8 - math.pi / 2 + math.pi / 8) * 0.40 * R) for j in range(9)]
        d.line(pts, fill=255, width=int(0.9 * k), joint="curve")
        # inverted flame glyph: a flame hanging point down, an inner teardrop and a dot
        fl = [(0.0, 0.17), (-0.045, 0.06), (-0.075, -0.045), (-0.05, -0.12), (-0.012, -0.075), (0.0, -0.17), (0.012, -0.075), (0.05, -0.12), (0.075, -0.045), (0.045, 0.06), (0.0, 0.17)]
        d.line([(c + x * R, c + y * R) for x, y in fl], fill=255, width=int(1.3 * k), joint="curve")
        d.ellipse([c - 0.018 * R, c - 0.012 * R, c + 0.018 * R, c + 0.024 * R], fill=255)
        for i in range(8):                                                       # little diamonds where the octagram touches the 0.585 ring
            a = 2 * math.pi * i / 8 - math.pi / 2
            px, py = c + math.cos(a) * 0.585 * R, c + math.sin(a) * 0.585 * R
            q = 0.016 * R
            d.polygon([(px, py - q), (px + q, py), (px, py + q), (px - q, py)], fill=255)
    mask = draw_mask(s, s, art)
    lum = glow_lines(mask, 1.5, 6.0, 0.6, 0.22)
    save(np.ones_like(lum), lum, "sigil")


def scorch():
    """Burnt ground: a ragged black patch (alpha) with a net of glowing cracks (the luminous part, for the additive pass)."""
    s = 256
    x, y, r, th = polar(s)
    lick = (0.5 + 0.5 * np.sin(17 * th + 1.0)) ** 3 * ang_noise(th, 19, 9)
    Rb = 0.80 + 0.10 * ang_noise(th, 11, 12) + 0.10 * lick
    patch = sstep(0.0, 0.30, Rb - r) * (0.78 + 0.22 * fbm(s, s, 40, 40, 13, 4))
    patch = patch * (0.80 + 0.2 * sstep(0.0, 0.5, r))
    rng = np.random.default_rng(55)

    def cracks(dr, k):
        c = s * k / 2
        R = s * k / 2

        def walk(a, rho, length, width, depth):
            p0 = (c + math.cos(a) * rho * R, c + math.sin(a) * rho * R)
            steps = int(length / 0.045)
            for i in range(steps):
                a += rng.normal(0, 0.10)
                rho += 0.045
                p1 = (c + math.cos(a) * rho * R, c + math.sin(a) * rho * R)
                wd = max(1.0, width * (1 - i / steps * 0.7))
                dr.line([p0, p1], fill=255, width=int(wd * k))
                dr.ellipse([p1[0] - wd * k / 2, p1[1] - wd * k / 2, p1[0] + wd * k / 2, p1[1] + wd * k / 2], fill=255)
                p0 = p1
                if depth < 2 and rng.random() < 0.15 and i > 1:
                    walk(a + rng.choice([-1, 1]) * rng.uniform(0.45, 0.8), rho, length * 0.42, width * 0.62, depth + 1)
        for i in range(12):
            walk(2 * math.pi * i / 12 + rng.uniform(-0.15, 0.15), rng.uniform(0.03, 0.08), rng.uniform(0.6, 0.88), 3.8, 0)
    cr = draw_mask(s, s, cracks) * sstep(0.0, 0.18, Rb - r)
    glow = np.clip(cr + 0.8 * blur(cr, 2.2) + 0.3 * blur(cr, 7.0), 0, 1)
    ash = sstep(0.55, 0.95, fbm(s, s, 12, 12, 17, 3)) * patch
    lum = 0.025 + 0.05 * ash + 0.95 * glow
    alpha = np.maximum(patch * 0.93, glow * 0.9)
    save(lum, alpha, "scorch")


def wall():
    """A horizontally tiling wall of tall flames, base at the bottom edge. 256 x 128: one tile is two blocks high per block of width."""
    w, h = 256, 128
    rng = np.random.default_rng(9)
    tongues = []
    n = 4
    for k in range(n):
        x0 = (k + 0.5 + rng.uniform(-0.12, 0.12)) / n * w
        tongues.append(dict(x0=x0, v0=0.0, hh=rng.uniform(0.62, 1.0), w0=rng.uniform(30, 40), lean=rng.uniform(-14, 14), sway=rng.uniform(5, 12),
                            freq=rng.uniform(2.4, 3.6), phase=rng.uniform(0, 6.28), pw=1.5, pq=0.8))
    for k in range(n):                                                              # small tongues filling the gaps, lower
        x0 = (k + 1.0) / n * w + rng.uniform(-3, 3)
        tongues.append(dict(x0=x0, v0=0.0, hh=rng.uniform(0.34, 0.58), w0=rng.uniform(16, 22), lean=rng.uniform(-6, 6), sway=2,
                            freq=2.2, phase=rng.uniform(0, 6.28), pw=1.4, pq=0.8))
    lum, alpha = flame_sprite(w, h, tongues, 303, periodic=True, base_fade=0.10)
    save(lum, alpha, "wall")


def swirl():
    """Five arms of black flame spiralling out of a hollow eye, seen head on (the twister's cross section / the charge vortex)."""
    s = 256
    x, y, r, th = polar(s)
    arms = 5
    warp = (fbm(s, s, 40, 40, 21, 3) - 0.5) * 0.9 + (fbm(s, s, 12, 12, 22, 2) - 0.5) * 0.35
    phase = arms * th - 5.5 * np.log(np.clip(r, 0.04, None)) * 1.0 + warp * 2.0
    cs = np.cos(phase)
    taper = 0.40 + 0.55 * (1 - np.clip(r, 0, 1)) ** 0.6                                           # arms get thinner towards the rim
    arm = cs - (1 - taper * 1.25)                                                  # >0 inside an arm
    dpx = arm * 17 * (0.4 + r)                                                     # rough distance to the arm edge in px
    envelope = sstep(1.0, 0.82, r) * sstep(0.07, 0.17, r)
    alpha = np.clip(dpx / 1.4 + 0.5, 0, 1) * envelope
    inside = np.clip(dpx, 0, None)
    streak = sstep(0.4, 0.85, fbm(s, s, 6, 6, 23, 3))
    lum = 0.022 + 0.08 * streak * np.exp(-inside / 10) + 0.85 * np.exp(-inside / 2.2) + 0.28 * np.exp(-inside / 8.0)
    lum = lum * (0.65 + 0.35 * (1 - np.clip(r, 0, 1)) ** 0.5)
    save(lum, alpha, "swirl")


def flash():
    """Additive star flash: a hot core, eight long thin rays (the four axes longest) and a scatter of short ones. White."""
    s = 256
    x, y, r, th = polar(s)
    core = np.exp(-(r / 0.06) ** 2) + 0.55 * np.exp(-(r / 0.16) ** 2) + 0.22 * np.exp(-(r / 0.42) ** 2)
    rng = np.random.default_rng(41)
    rays = np.zeros_like(r)
    specs = [(k * np.pi / 4, 1.0 if k % 2 == 0 else 0.62, 0.018) for k in range(8)]
    specs += [(rng.uniform(0, 2 * np.pi), rng.uniform(0.22, 0.6), rng.uniform(0.006, 0.012)) for _ in range(22)]
    for a, length, wd in specs:
        dth = (th - a + np.pi) % (2 * np.pi) - np.pi
        along = np.clip(1 - r / length, 0, 1) ** 1.25
        rays = np.maximum(rays, np.exp(-(np.abs(dth) * r / (wd * (0.25 + along))) ** 2) * along)
    lum = np.clip(core + rays * 1.15, 0, 1) * sstep(1.0, 0.9, r)
    save(np.ones_like(lum), lum, "flash")


def inverse():
    """Negative-blend burst. The GREY VALUE is how strongly the picture behind is inverted; alpha is 1 everywhere."""
    s = 256
    x, y, r, th = polar(s)
    rng = np.random.default_rng(43)
    spikes = (0.5 + 0.5 * np.sin(9 * th + 0.4)) ** 2.0 * ang_noise(th, 13, 14) + 0.5 * ang_noise(th, 29, 15)
    Rb = 0.38 + 0.50 * spikes * 0.75
    body = np.clip((Rb - r) / 0.30, 0, 1) ** 0.8
    core = np.exp(-(r / 0.18) ** 2)
    lum = np.clip(body * 0.85 + core * 0.4, 0, 1) * sstep(1.0, 0.92, r)
    save(lum, np.ones_like(lum), "inverse")


def ring():
    """Shock ring: a lit band that fades inwards and curling flame teeth of different lengths on the outside. Additive white."""
    s = 256
    x, y, r, th = polar(s)
    px = s / 2
    r0 = 0.80
    rng = np.random.default_rng(16)
    specs = []
    a = 0.0
    for k in range(30):
        a += 2 * np.pi / 30 * rng.uniform(0.75, 1.25)
        specs.append((a, rng.choice([0.07, 0.10, 0.14, 0.19], p=[0.3, 0.3, 0.25, 0.15]), rng.uniform(0.034, 0.06), rng.uniform(0.2, 0.45)))
    dt = licks_d(r, th, r0 - 0.01, specs, px)
    teeth = sstep(-0.5, 3.0, dt) * (0.55 + 0.45 * np.exp(-np.clip(dt, 0, None) / 9.0))
    band = sstep(0.62, r0, r) ** 1.5 * (r < r0 + 0.012)
    edge = np.exp(-np.abs(r - r0) / 0.010)
    echo = np.exp(-((r - 0.60) / 0.007) ** 2) * 0.5
    lum = np.clip(band * 0.8 + edge * 0.6 + teeth * 0.95 + echo, 0, 1) * (r < 1.0)
    save(np.ones_like(lum), lum, "ring")


def ember():
    """Teardrop spark for ADD: bright head at the bottom, tail pointing up (rotate so the tail trails the motion)."""
    s = 64
    xx, yy = grid(s, s)
    x, y = (xx - s / 2) / (s / 2), (s / 2 - yy) / (s / 2)
    head = np.exp(-((x / 0.2) ** 2 + ((y + 0.38) / 0.22) ** 2))
    halo = np.exp(-((x / 0.5) ** 2 + ((y + 0.3) / 0.55) ** 2)) * 0.35
    f = np.clip((y + 0.35) / 1.3, 0, 1)
    wt = 0.17 * (1 - f) ** 1.4 + 0.012
    tail = np.exp(-(x / wt) ** 2) * (1 - f) ** 1.6 * (y > -0.35)
    lum = np.clip(head * 1.2 + halo + tail * 0.9, 0, 1)
    save(np.ones_like(lum), lum, "ember")


def shard():
    """Four angular shards of burnt glass (2 x 2 cells of 64 px): two dark facets split by a lit ridge, bright edges, hot tips."""
    s = 64
    lum_all, alpha_all = np.zeros((2 * s, 2 * s), np.float32), np.zeros((2 * s, 2 * s), np.float32)
    rng = np.random.default_rng(61)
    for cell in range(4):
        L = rng.uniform(0.82, 0.95)
        Wd = rng.uniform(0.34, 0.50)
        top, bot = (0.0, -L), (rng.uniform(-0.1, 0.1), L * rng.uniform(0.85, 1.0))
        right = [(Wd * rng.uniform(0.8, 1.0), -L * rng.uniform(0.2, 0.45)), (Wd * rng.uniform(0.55, 0.9), L * rng.uniform(0.25, 0.55))]
        left = [(-Wd * rng.uniform(0.6, 1.0), L * rng.uniform(0.1, 0.4)), (-Wd * rng.uniform(0.75, 1.0), -L * rng.uniform(0.15, 0.4))]
        pts = [top] + right + [bot] + left
        rot = rng.uniform(-0.45, 0.45)
        ca, sa = math.cos(rot), math.sin(rot)
        pts = [(x * ca - y * sa, x * sa + y * ca) for x, y in pts]
        ridge = [pts[0], (pts[0][0] * 0.3 + pts[3][0] * 0.7 + 0.04, pts[0][1] * 0.3 + pts[3][1] * 0.7), pts[3]]

        def P(k, x, y):
            sc = s * k / 2 * 0.95
            return (s * k / 2 + x * sc, s * k / 2 + y * sc)

        def art(d, k, pts=pts):
            d.polygon([P(k, x, y) for x, y in pts], fill=255)

        def facet(d, k, pts=pts, ridge=ridge):
            d.polygon([P(k, x, y) for x, y in [pts[0], pts[1], pts[2], pts[3]]] + [P(k, ridge[1][0], ridge[1][1])], fill=255)

        def edge(d, k, pts=pts):
            d.line([P(k, x, y) for x, y in pts + [pts[0]]], fill=255, width=int(1.8 * k), joint="curve")

        def rdg(d, k, ridge=ridge):
            d.line([P(k, x, y) for x, y in ridge], fill=255, width=int(1.1 * k), joint="curve")
        m = draw_mask(s, s, art)
        f = draw_mask(s, s, facet)
        e = np.clip(draw_mask(s, s, edge) * 1.3, 0, 1) * m
        rd = draw_mask(s, s, rdg) * m
        lum = 0.035 + 0.05 * m + 0.11 * f + 0.38 * rd + 0.95 * e
        ox, oy = (cell % 2) * s, (cell // 2) * s
        lum_all[oy:oy + s, ox:ox + s] = lum
        alpha_all[oy:oy + s, ox:ox + s] = m
    save(lum_all, alpha_all, "shard")


def smoke():
    """Ash cloud for ALPHA: dark, billowy, a few violet-lit streaks. Tinted dark violet by the vertex colour."""
    s = 128
    x, y, r, th = polar(s)
    n = fbm(s, s, 26, 26, 71, 4)
    n2 = fbm(s, s, 10, 10, 72, 3)
    alpha = np.clip((1.0 - r * 0.95) * 1.5 + (n - 0.5) * 1.3, 0, 1) * sstep(1.0, 0.68, r)
    alpha = alpha ** 1.15
    lum = 0.07 + 0.10 * n2 + 0.30 * sstep(0.62, 0.9, n) * (1 - r)
    save(lum, alpha * 0.92, "smoke")


def runes():
    """Four angular demonic glyphs (2 x 2 cells of 64 px), additive white with a soft glow."""
    s = 64
    out = np.zeros((2 * s, 2 * s), np.float32)

    def glyph(i):
        def art(d, k):
            c = s * k / 2
            u = s * k / 2 * 0.78
            w = int(2.2 * k)

            def P(x, y):
                return (c + x * u, c + y * u)
            if i == 0:                      # the flame eye: a lozenge, a pupil, a drip below
                d.line([P(0, -0.95), P(0.55, 0), P(0, 0.62), P(-0.55, 0), P(0, -0.95)], fill=255, width=w, joint="curve")
                d.ellipse([c - 0.16 * u, c - 0.2 * u, c + 0.16 * u, c + 0.12 * u], fill=255)
                d.line([P(0, 0.62), P(0, 0.98)], fill=255, width=w)
            elif i == 1:                    # inverted triangle crossed by a bar, two ticks
                d.line([P(-0.7, -0.62), P(0.7, -0.62), P(0, 0.62), P(-0.7, -0.62)], fill=255, width=w, joint="curve")
                d.line([P(0, -0.95), P(0, 0.98)], fill=255, width=w)
                d.line([P(-0.95, 0.1), P(-0.42, 0.1)], fill=255, width=w)
                d.line([P(0.95, 0.1), P(0.42, 0.1)], fill=255, width=w)
            elif i == 2:                    # horns: two hooks over a cross-stroke
                d.arc([c - 0.95 * u, c - 0.8 * u, c + 0.05 * u, c + 0.5 * u], 200, 340, fill=255, width=w)
                d.arc([c - 0.05 * u, c - 0.8 * u, c + 0.95 * u, c + 0.5 * u], 200, 340, fill=255, width=w)
                d.line([P(-0.45, 0.0), P(0, 0.55), P(0.45, 0.0)], fill=255, width=w, joint="curve")
                d.line([P(0, 0.55), P(0, 0.98)], fill=255, width=w)
                d.line([P(-0.4, 0.82), P(0.4, 0.82)], fill=255, width=w)
            else:                           # a trident of flames over a crescent
                d.line([P(-0.62, -0.92), P(-0.62, 0.15), P(0, 0.62), P(0.62, 0.15), P(0.62, -0.92)], fill=255, width=w, joint="curve")
                d.line([P(0, -0.98), P(0, 0.98)], fill=255, width=w)
                d.polygon([P(0, -0.98), P(-0.1, -0.72), P(0.1, -0.72)], fill=255)
                d.line([P(-0.4, 0.3), P(0.4, 0.3)], fill=255, width=w)
        return draw_mask(s, s, art)
    for i in range(4):
        g = glyph(i)
        g = np.clip(g + 0.6 * blur(g, 1.4) + 0.22 * blur(g, 4.0), 0, 1)
        out[(i // 2) * s:(i // 2 + 1) * s, (i % 2) * s:(i % 2 + 1) * s] = g
    save(np.ones_like(out), out, "runes")


if __name__ == "__main__":
    tongue()
    orb()
    corona()
    trail()
    sigil()
    scorch()
    wall()
    swirl()
    flash()
    inverse()
    ring()
    ember()
    shard()
    smoke()
    runes()

"""Generates the Body Magic VFX textures (steam, muscle fibre, veins, heartbeat rings, rifled barrel, tendon strands).

    python3 -B tools/gen_body_textures.py

Output: src/main/resources/assets/nusmp/textures/particle/body_*.png   (deterministic: fixed seeds, no fonts, no external images)

Look reference: Body Magic (Titan). Extreme physical enhancement shown as pulsing muscle expansion with rapid steam emission; the canon
spell Body Compression condenses part of the body and restructures it into a gun barrel that fires a compressed blast. So the sprites are:
billowing steam puffs and steam jets, striated muscle ropes and twisted fibre ribbons, glowing vein networks over a dark muscle-tissue
disc, a ring of spindle-shaped muscle bellies with an ECG line (the heartbeat), a rifled barrel face and its muzzle star, a compressed
round with its shock arcs, whipping tendon strands, a fibre starburst and a flexed-arm emblem.

Texture convention (same as the rest of textures/particle): white or grey with alpha so one vertex colour tints them. Layouts the
layer (vfx/client/layer/BodyLayer.java, BodyFx.java) relies on:
  body_steam       256x256 atlas of 4 puffs (2 x 2 cells of 128), grey-white, soft alpha, shaded
  body_steam_jet   128x256, nozzle at the bottom, plume widening to the top
  body_veins       256x256, radial vein web, glow only (draw additively)
  body_flesh       256x256, dark muscle-tissue disc (draw with alpha)
  body_ring        256x256, sigil: tick band, ring of 12 muscle bellies, ECG line, spokes (outer circle at 0.965 of the half size)
  body_pulse       256x256, thin heartbeat ring at 0.86 of the half size with a fading inner trail
  body_wall        256x128, tiles in U; muscle ropes rising from a hot base (bottom) and dissolving into steam (top)
  body_fibre       256x64, tiles in U; braided fibre ribbon, U runs along the length
  body_slug        256x64, the compressed round, nose at U = 1, U runs along the length
  body_muzzle      256x256, muzzle star (8 long petals, 8 shorter, hot core)
  body_rifling     256x256, barrel seen from the front: knurled flesh ring, bevel, spiral rifling grooves
  body_strand      64x256, whipping tendon strand, base at the bottom, tip at the top
  body_burst       256x256, starburst of fine fibre rays
  body_spark       64x64, ember with a vertical streak (rotate it along its motion)
  body_flex        256x256, flexed-arm emblem with a rim light
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4          # supersampling for drawn shapes
TAU = math.tau


# ------------------------------------------------------------------------------------------------ helpers
def save(lum, alpha, name):
    """lum / alpha: float arrays in 0..1 (lum is defined everywhere, also under alpha 0, so edges never get dark fringes)."""
    lum = np.clip(lum, 0, 1)
    alpha = np.clip(alpha, 0, 1)
    img = np.dstack([lum * 255 + 0.5, lum * 255 + 0.5, lum * 255 + 0.5, alpha * 255 + 0.5]).astype(np.uint8)
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(img, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, img.shape[1], "x", img.shape[0])


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def value_noise(w, h, cw, seed, ch=None, wrap=True):
    """Smooth value noise in 0..1 with cells of cw x ch pixels. wrap = tileable in both directions."""
    ch = ch or cw
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cw), max(1, h // ch)
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    if wrap:
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


def fbm(w, h, cw, seed, octaves=4, ch=None, wrap=True):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        n = n + value_noise(w, h, max(2, cw >> o), seed + 17 * o, None if ch is None else max(2, ch >> o), wrap) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def noise1(u, k, seed):
    """Smooth periodic 1-D noise (0..1) of u in 0..1 with k lobes around the circle."""
    rng = np.random.default_rng(seed)
    vals = rng.random(k + 1).astype(np.float32)
    vals[-1] = vals[0]
    f = (u % 1.0) * k
    i = np.floor(f).astype(int)
    t = f - i
    t = t * t * (3 - 2 * t)
    return vals[i] * (1 - t) + vals[i + 1] * t


def blur(a, sigma, wrap=False):
    """Separable gaussian blur of a float array (no scipy)."""
    if sigma <= 0:
        return a
    r = int(math.ceil(sigma * 3))
    k = np.exp(-0.5 * (np.arange(-r, r + 1) / sigma) ** 2)
    k /= k.sum()
    out = a.astype(np.float32)
    for axis in (0, 1):
        pad = [(0, 0), (0, 0)]
        pad[axis] = (r, r)
        p = np.pad(out, pad, mode="wrap" if wrap else "edge")
        acc = np.zeros_like(out)
        for i, kv in enumerate(k):
            sl = [slice(None), slice(None)]
            sl[axis] = slice(i, i + out.shape[axis])
            acc += kv * p[tuple(sl)]
        out = acc
    return out


def polar(n):
    """x, y in -1..1 (y down), radius 0..~1.41 and angle."""
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float32)
    x, y = (xx + 0.5) / n * 2 - 1, (yy + 0.5) / n * 2 - 1
    return x, y, np.sqrt(x * x + y * y), np.arctan2(y, x)


def to_arr(im, size):
    """A supersampled 'L' image -> float array of the final size."""
    return np.asarray(im.resize((size, size) if isinstance(size, int) else size, Image.LANCZOS), dtype=np.float32) / 255.0


def shade_from_density(dens, light=(-0.65, -0.75), strength=0.26, base=0.74, thick=0.16):
    """Soft cloud shading: bright on the lit side, darker where the cloud is thick (so alpha-blended steam reads as a volume)."""
    ds = blur(dens, 2.0)
    gy, gx = np.gradient(ds)
    g = np.sqrt(gx * gx + gy * gy) + 1e-5
    lit = (-gx * light[0] - gy * light[1]) / g
    mag = np.clip(g / (np.percentile(g, 96) + 1e-6), 0, 1)
    return np.clip(base + strength * lit * mag - thick * np.clip(dens / 1.6, 0, 1), 0.5, 1.0)


# ------------------------------------------------------------------------------------------------ steam
def steam_cell(n, seed, kind):
    rng = np.random.default_rng(seed)
    x, y, r, _ = polar(n)
    wx = (fbm(n, n, 32, seed + 1, 3, wrap=False) - 0.5) * 0.42
    wy = (fbm(n, n, 32, seed + 2, 3, wrap=False) - 0.5) * 0.42
    xw, yw = x + wx, y + wy
    squash = (1.0, 0.70, 0.52, 0.85)[kind]
    count = (8, 7, 10, 8)[kind]
    dens = np.zeros((n, n), np.float32)
    blobs = []
    for _ in range(count):                                   # the body of the cloud
        a, d = rng.uniform(0, TAU), rng.uniform(0.0, 0.46)
        cx, cy = math.cos(a) * d, math.sin(a) * d * squash
        rr = rng.uniform(0.32, 0.46) * (0.62 + 0.38 * squash)
        blobs.append((cx, cy, rr))
        q = ((xw - cx) ** 2 + ((yw - cy) / squash) ** 2) / (rr * rr)
        dens += np.clip(1 - q, 0, 1) ** 1.6 * rng.uniform(0.8, 1.1)
    for _ in range(26):                                      # cauliflower bumps on the rim of the body
        cx0, cy0, rr0 = blobs[rng.integers(len(blobs))]
        a = rng.uniform(0, TAU)
        cx, cy = cx0 + math.cos(a) * rr0 * 0.88, cy0 + math.sin(a) * rr0 * 0.88 * squash
        rr = rr0 * rng.uniform(0.28, 0.5)
        q = ((xw - cx) ** 2 + ((yw - cy) / squash) ** 2) / (rr * rr)
        dens += np.clip(1 - q, 0, 1) ** 1.4 * rng.uniform(0.6, 1.0)
    dens *= 0.80 + 0.40 * fbm(n, n, 10, seed + 3, 4, wrap=False)
    wisp = 0.78 + 0.22 * fbm(n, n, 6, seed + 5, 3, wrap=False)
    alpha = smoothstep(0.03, 0.78, dens) ** 1.15 * wisp * (1 - smoothstep(0.78, 0.97, np.maximum(np.abs(x), np.abs(y))))
    return shade_from_density(dens), alpha


def steam():
    """Four billowing steam puffs in a 2x2 atlas (round, wide, long wisp, medium)."""
    lum, alpha = np.zeros((256, 256), np.float32), np.zeros((256, 256), np.float32)
    for k in range(4):
        l, a = steam_cell(128, 910 + 31 * k, k)
        ox, oy = (k % 2) * 128, (k // 2) * 128
        lum[oy:oy + 128, ox:ox + 128], alpha[oy:oy + 128, ox:ox + 128] = l, a
    save(lum, alpha, "body_steam")


def steam_jet(w=128, h=256):
    """A jet of steam blasting out of a nozzle: narrow and bright at the bottom, billowing into a wide cone toward the top."""
    seed = 4100
    xx, yy = grid(w, h)
    u = (xx + 0.5) / w * 2 - 1                       # -1..1 (1 unit = 64 px)
    v = 1 - (yy + 0.5) / h                            # 0 nozzle .. 1 tip
    Y = v * 4.0                                       # 0..4 in the same unit as u
    uw = u + (fbm(w, h, 32, seed + 1, 3, wrap=False) - 0.5) * 0.45 * v
    Yw = Y + (fbm(w, h, 32, seed + 2, 3, wrap=False) - 0.5) * 0.30
    rng = np.random.default_rng(seed)
    dens = np.zeros((h, w), np.float32)
    n = 22
    for i in range(n):
        f = (i + rng.uniform(0.0, 0.7)) / n
        cy = 0.10 + f * 3.15
        spread = 0.04 + 0.42 * f
        cx = rng.uniform(-1, 1) * spread * 0.9 + 0.10 * math.sin(f * 5) * f
        rr = 0.14 + 0.52 * f ** 0.9 + rng.uniform(0.0, 0.07)
        q = ((uw - cx) ** 2 + (Yw - cy) ** 2) / (rr * rr)
        dens += np.clip(1 - q, 0, 1) ** 1.6 * rng.uniform(0.75, 1.15)
    dens *= 0.80 + 0.40 * fbm(w, h, 10, seed + 3, 4, wrap=False)
    alpha = smoothstep(0.03, 0.80, dens) ** 1.1 * (0.80 + 0.20 * fbm(w, h, 6, seed + 5, 3, wrap=False)) * (1 - smoothstep(0.80, 1.0, v)) * smoothstep(0.0, 0.025, v)
    alpha *= 1 - smoothstep(0.84, 0.99, np.abs(u))
    save(shade_from_density(dens), alpha, "body_steam_jet")


# ------------------------------------------------------------------------------------------------ veins and flesh
def veins(size=256):
    """A radial web of veins: 14 roots growing out from the centre with random-walk bends, side branches and thin capillary arcs.
    White, alpha = glow (hairline core, tight halo, wide halo). Meant to be drawn additively on the ground."""
    S = size * SS
    img = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(7001)
    C = S / 2

    def grow(x, y, ang, length, width, depth, bend):
        step = S * 0.011
        n = max(2, int(length / step))
        px, py = x, y
        for i in range(n):
            ang += rng.normal(0, 0.15) + bend
            nx, ny = px + math.cos(ang) * step, py + math.sin(ang) * step
            if math.hypot(nx - C, ny - C) > S * 0.465:
                break
            w = max(1.0, width * (1 - 0.78 * i / n))
            d.line([(px, py), (nx, ny)], fill=255, width=int(round(w)))
            d.ellipse([nx - w / 2, ny - w / 2, nx + w / 2, ny + w / 2], fill=255)
            px, py = nx, ny
            if depth < 3 and i > 4 and rng.random() < (0.10 if depth == 0 else 0.075):
                side = rng.choice([-1, 1])
                grow(px, py, ang + side * rng.uniform(0.5, 1.0), length * rng.uniform(0.28, 0.5) * (1 - 0.5 * i / n), width * 0.64, depth + 1,
                     rng.uniform(-0.04, 0.04))

    roots = 16
    for k in range(roots):
        a = TAU * k / roots + rng.uniform(-0.12, 0.12)
        grow(C + math.cos(a) * S * 0.03, C + math.sin(a) * S * 0.03, a, S * 0.5, S * 0.021, 0, rng.normal(0, 0.012))
    for rr, wd in ((0.27, 3.0), (0.50, 4.0), (0.74, 3.0)):                        # capillary arcs joining the roots, with gaps
        a = rng.uniform(0, TAU)
        while a < TAU * 2:
            span = rng.uniform(0.25, 0.7)
            if rng.random() < 0.62:
                pts = []
                for t in np.linspace(0, 1, 18):
                    ang = a + span * t
                    rad = (rr + 0.012 * math.sin(t * 9 + rr * 30) + rng.normal(0, 0.003)) * C
                    pts.append((C + math.cos(ang) * rad, C + math.sin(ang) * rad))
                d.line(pts, fill=200, width=int(wd))
            a += span + rng.uniform(0.05, 0.3)
    core = to_arr(img, size)
    x, y, r, _ = polar(size)
    tight, wide, haze = blur(core, 1.3), blur(core, 4.5), blur(core, 12.0)
    edge = 1 - smoothstep(0.84, 0.985, r)
    centre = np.exp(-(r / 0.10) ** 2) * 0.85
    alpha = np.clip((core * 0.95 + tight * 0.55 + wide * 0.60 + haze * 0.55) * edge + centre, 0, 1)
    save(np.ones_like(alpha), alpha, "body_veins")


def sample_wrapx(arr, fx, fy):
    """Bilinear lookup, wrapping in x, clamping in y (pixel coordinates)."""
    h, w = arr.shape
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    xa, xb = x0 % w, (x0 + 1) % w
    ya, yb = np.clip(y0, 0, h - 1), np.clip(y0 + 1, 0, h - 1)
    return (arr[ya, xa] * (1 - tx) + arr[ya, xb] * tx) * (1 - ty) + (arr[yb, xa] * (1 - tx) + arr[yb, xb] * tx) * ty


def flesh(size=256):
    """A dark disc of muscle tissue: fibres swirling outward from the centre, bundles, creases, darker towards the rim."""
    x, y, r, th = polar(size)
    u = (th / TAU) % 1.0
    W, H = 1024, 256
    g1 = fbm(W, H, 8, 311, 4, ch=96)           # fine fibres, long in the radial direction (the swirl turns them)
    g2 = fbm(W, H, 24, 331, 3, ch=128)
    g3 = fbm(W, H, 64, 351, 3, ch=64)
    twist = 0.20 * r + 0.03 * np.sin(r * 9)
    f = ((u + twist) % 1.0) * W, r * (H - 1)
    grain, bundle, mott = sample_wrapx(g1, *f), sample_wrapx(g2, *f), sample_wrapx(g3, *f)
    crease = smoothstep(0.60, 0.78, sample_wrapx(fbm(W, H, 20, 371, 3, ch=160), *f))
    lum = 0.34 + 0.34 * grain + 0.20 * bundle + 0.16 * mott - 0.20 * crease
    lum = lum * (1.0 - 0.30 * smoothstep(0.55, 0.95, r)) + 0.12 * np.exp(-(r / 0.30) ** 2)
    rim = np.exp(-((r - 0.955) / 0.012) ** 2) * 0.5
    alpha = (0.80 + 0.20 * grain) * (1 - smoothstep(0.935, 0.975, r)) + rim
    save(lum + rim, alpha, "body_flesh")


# ------------------------------------------------------------------------------------------------ rings
def ecg(p):
    """One heartbeat (PQRST) at phase p in 0..1, peak ~1."""
    def b(c, w, a):
        return a * math.exp(-((p - c) / w) ** 2)
    return b(0.17, 0.035, 0.16) - b(0.335, 0.011, 0.20) + b(0.372, 0.010, 1.0) - b(0.408, 0.012, 0.34) + b(0.62, 0.055, 0.26)


def ring(size=256):
    """Muscle sigil: a tick band, a ring of 12 spindle-shaped muscle bellies with fibre lines and tendons, an ECG line (4 heartbeats), spokes."""
    S = size * SS
    base, edge = Image.new("L", (S, S), 0), Image.new("L", (S, S), 0)
    db, de = ImageDraw.Draw(base), ImageDraw.Draw(edge)
    C = S / 2

    def P(a, rr):
        return C + math.cos(a) * rr * C, C + math.sin(a) * rr * C

    def circle(d, rr, wd, fill=255):
        d.ellipse([C - rr * C, C - rr * C, C + rr * C, C + rr * C], outline=fill, width=max(1, int(wd * C)))

    circle(de, 0.965, 0.011)
    circle(de, 0.932, 0.005)
    for k in range(120):
        a = TAU * k / 120
        r0, r1 = (0.858, 0.918) if k % 5 == 0 else (0.884, 0.914)
        de.line([P(a, r0), P(a, r1)], fill=255 if k % 5 == 0 else 190, width=int(SS * (2.4 if k % 5 == 0 else 1.3)))
    circle(de, 0.842, 0.004, 170)
    n, rm, T = 12, 0.715, 0.105
    half = math.pi / n * 0.80
    for k in range(n):
        a0 = TAU * k / n
        outer, inner = [], []
        for s in np.linspace(0, 1, 26):
            ang = a0 + half * (2 * s - 1)
            t = T * math.sin(math.pi * s) ** 0.72
            outer.append(P(ang, rm + t))
            inner.append(P(ang, rm - t))
        poly = outer + inner[::-1]
        db.polygon(poly, fill=165)
        de.polygon(poly, outline=255, width=int(SS * 1.7))
        for f in (-0.74, -0.46, -0.18, 0.18, 0.46, 0.74):                         # fibre lines inside the belly
            pts = [P(a0 + half * (2 * s - 1), rm + f * T * math.sin(math.pi * s) ** 0.72) for s in np.linspace(0.05, 0.95, 20)]
            de.line(pts, fill=215, width=int(SS * 0.9))
        pts = [P(a, rm) for a in np.linspace(a0 + half, a0 + TAU / n - half, 6)]      # tendon to the next belly, with a small knot
        de.line(pts, fill=255, width=int(SS * 1.5))
        kx, ky = P(a0 + TAU / n / 2, rm)
        de.ellipse([kx - SS * 2.2, ky - SS * 2.2, kx + SS * 2.2, ky + SS * 2.2], fill=255)
    circle(de, 0.60, 0.007)
    circle(de, 0.585, 0.003, 170)
    pts = [P(TAU * i / 1600, 0.505 + 0.058 * ecg((TAU * i / 1600) / (TAU / 4) % 1.0)) for i in range(1600)]
    de.line(pts + [pts[0]], fill=255, width=int(SS * 1.9))
    circle(de, 0.425, 0.005, 200)
    for k in range(24):
        a = TAU * k / 24
        de.line([P(a, 0.16), P(a, 0.40)], fill=235 if k % 2 == 0 else 150, width=int(SS * (1.6 if k % 2 == 0 else 1.0)))
    circle(de, 0.125, 0.012)
    circle(de, 0.07, 0.006, 200)
    ea, ba = to_arr(edge, size), to_arr(base, size)
    glow = blur(ea, 2.4)
    alpha = np.clip(np.maximum(ea, ba * 0.62) + 0.35 * glow, 0, 1)
    save(np.clip(0.55 + 0.45 * ea + 0.1 * glow, 0, 1), alpha, "body_ring")


def pulse(size=256):
    """A heartbeat shock ring: bright broken ring at 0.86 with a soft outer haze and a long fading inner trail."""
    x, y, r, th = polar(size)
    u = (th / TAU) % 1.0
    r0 = 0.86 + 0.014 * (noise1(u, 9, 1) - 0.5) * 2
    core = np.exp(-((r - r0) / 0.014) ** 2)
    haze = np.exp(-((r - r0) / 0.05) ** 2) * 0.34
    trail = np.where(r < r0, np.clip(r / r0, 0, 1) ** 5, 0) * 0.30
    seg = 0.55 + 0.45 * noise1(u, 7, 2)
    fine = 0.72 + 0.28 * noise1(u, 90, 3)
    a = np.clip((core + haze + trail) * seg * fine, 0, 1) * (1 - smoothstep(0.95, 1.0, r))
    save(np.ones_like(a), a, "body_pulse")


# ------------------------------------------------------------------------------------------------ ribbons
def wall(w=256, h=128):
    """The rim of the field: muscle ropes (twisted tendons) standing on a hot base, thinning upwards and dissolving into steam wisps."""
    xx, yy = grid(w, h)
    v = 1 - (yy + 0.5) / h                               # 0 bottom .. 1 top
    lum, alpha = np.full((h, w), 0.8, np.float32), np.zeros((h, w), np.float32)
    rng = np.random.default_rng(5001)
    n = 10
    for i in range(n):
        xc = (i + 0.5) * w / n + w / n * 0.16 * math.sin(i * 2.3) + 6 * np.sin(v * 5.0 + i * 1.7)
        hw = (12.5 - 8.0 * v) * (0.82 + 0.36 * rng.random())
        dd = (xx - xc) / hw
        cover = smoothstep(1.0, 0.8, np.abs(dd))
        body = np.sqrt(np.clip(1 - dd * dd, 0, 1))
        twist = 0.5 + 0.5 * np.sin((xx - xc) * 0.62 + yy * 0.46 + i * 1.3)
        a_i = cover * (1 - smoothstep(0.30, 0.96, v))
        l_i = 0.42 + 0.38 * body + 0.22 * twist * body
        take = a_i > alpha
        lum = np.where(take, l_i, lum)
        alpha = np.maximum(alpha, a_i)
    wisp_n = value_noise(w, h, 7, 5002, ch=44)
    wisps = smoothstep(0.50, 0.82, wisp_n) * (1 - smoothstep(0.52, 1.0, v)) * smoothstep(0.02, 0.34, v)
    haze = fbm(w, h, 26, 5003) * (1 - smoothstep(0.0, 0.7, v)) * 0.34
    base = np.exp(-(v / 0.09) ** 2)
    alpha = np.maximum(alpha, np.maximum(wisps * 0.8, haze))
    lum = np.where(alpha > 0.3, lum, 0.92)
    alpha = np.clip(alpha + base * 0.95, 0, 1)
    lum = np.clip(lum + base * 0.4, 0, 1)
    save(lum, alpha, "body_wall")


def fibre(w=256, h=64):
    """A braided ribbon of 5 muscle fibres weaving over and under each other. Tiles along U (the length)."""
    xx, yy = grid(w, h)
    x, y = xx / w, (yy + 0.5) / h
    best = np.full((h, w), -9.0, np.float32)
    lum, alpha = np.full((h, w), 0.9, np.float32), np.zeros((h, w), np.float32)
    cycles, N = 2, 5
    for k in range(N):
        ph = TAU * k / N
        yc = 0.5 + 0.20 * np.sin(TAU * x * cycles + ph)
        z = np.cos(TAU * x * cycles + ph)
        d = np.abs(y - yc) / 0.125
        cov = smoothstep(1.0, 0.78, d)
        body = np.sqrt(np.clip(1 - np.minimum(d, 1) ** 2, 0, 1))
        fine = 0.84 + 0.16 * np.sin(TAU * x * 64 + k * 3.1 + (y - yc) * 11)
        l = (0.38 + 0.62 * body) * fine * (0.72 + 0.28 * (z * 0.5 + 0.5))
        score = np.where(cov > 0.05, z, -9.0)
        take = score > best
        best = np.where(take, score, best)
        lum = np.where(take, l, lum)
        alpha = np.maximum(alpha, cov)
    halo = np.exp(-(((y - 0.5) / 0.46) ** 2)) * 0.20
    lum = np.where(alpha > 0.08, lum, 1.0)
    save(lum, np.maximum(alpha, halo), "body_fibre")


def slug(w=256, h=64):
    """The compressed round seen from the side, nose to the right: a fat ogive head with a hot nose, spin bands, bow-shock arcs and a
    streaked tail."""
    xx, yy = grid(w, h)
    x, y = (xx + 0.5) / w, (yy + 0.5) / h * 2 - 1
    ramp = smoothstep(0.0, 0.66, x) ** 0.8
    cap = np.where(x > 0.80, np.sqrt(np.clip(1 - ((x - 0.80) / 0.20) ** 2, 0, 1)), 1.0)
    halfw = 0.84 * (0.07 + 0.93 * ramp) * cap + 1e-3
    d = np.abs(y) / halfw
    m = smoothstep(1.0, 0.80, d)
    streak_n = value_noise(w, h, 64, 6001, ch=2, wrap=False)
    streaks = 0.65 + 0.35 * streak_n
    body_fade = 0.22 + 0.78 * smoothstep(0.0, 0.46, x)
    core = np.exp(-(y / (0.22 * (0.2 + ramp) + 0.03)) ** 2) * (0.35 + 0.65 * x)
    nose = np.exp(-(((x - 0.91) / 0.08) ** 2 + (y / 0.36) ** 2))
    bands = 0.5 + 0.5 * np.sin(TAU * (x * 6.0 + y * 0.8))
    band_w = smoothstep(0.20, 0.42, x) * (1 - smoothstep(0.80, 0.90, x))
    arcs = np.zeros_like(x)
    for off in (0.0, 0.085):
        xa = 0.99 - off - 0.38 * np.abs(y) ** 1.25
        arcs += np.exp(-((x - xa) / 0.010) ** 2) * (np.abs(y) < 0.95)
    rim = np.exp(-((d - 0.90) / 0.09) ** 2) * m * 0.5
    a = 0.80 * m * streaks * body_fade + 0.55 * core + 0.85 * nose + 0.30 * bands * band_w * m + 0.50 * np.clip(arcs, 0, 1) * (x > 0.55) + rim
    lum = 0.55 + 0.45 * np.clip(core * 1.5 + nose * 2 + bands * band_w * 0.5 + rim, 0, 1)
    save(lum, np.clip(a, 0, 1) * (1 - smoothstep(0.985, 1.0, x)), "body_slug")


# ------------------------------------------------------------------------------------------------ barrel and muzzle
def muzzle(size=256):
    """Muzzle star: eight long flame petals, eight shorter ones in between, a hot core and a thin broken ring."""
    x, y, r, th = polar(size)
    u = (th / TAU) % 1.0
    rng = np.random.default_rng(8001)

    def petal(a0, length, w0, sharp):
        dx, dy = x * math.cos(a0) + y * math.sin(a0), -x * math.sin(a0) + y * math.cos(a0)
        t = np.clip(dx / length, 0, 1)
        width = w0 * (1 - t) ** sharp + 0.004
        return np.where((dx > 0) & (dx < length), np.exp(-(dy / width) ** 2) * (1 - t) ** 0.55, 0)

    acc = np.zeros_like(x)
    for i in range(8):
        acc += petal(i * math.pi / 4 + rng.uniform(-0.04, 0.04), 0.95 * rng.uniform(0.82, 1.0), 0.082, 0.85)
    for i in range(8):
        acc += 0.85 * petal(i * math.pi / 4 + math.pi / 8 + rng.uniform(-0.05, 0.05), 0.60 * rng.uniform(0.85, 1.0), 0.055, 0.8)
    for i in range(16):
        acc += 0.6 * petal(i * math.pi / 8 + math.pi / 16 + rng.uniform(-0.05, 0.05), 0.38 * rng.uniform(0.8, 1.0), 0.032, 0.8)
    core = np.exp(-(r / 0.12) ** 2) + 0.5 * np.exp(-(r / 0.30) ** 2)
    ring = np.exp(-((r - 0.34) / 0.012) ** 2) * 0.5 * (0.4 + 0.6 * noise1(u, 11, 4))
    fine = 0.8 + 0.2 * noise1(u, 70, 5)
    a = np.clip((acc * fine + core + ring), 0, 1) * (1 - smoothstep(0.93, 1.0, r))
    save(np.clip(0.72 + 0.28 * (core + acc * 0.5), 0, 1), a, "body_muzzle")


def rifling(size=256):
    """A barrel seen from the front: a knurled flesh ring, a bevel, then the bore with spiral rifling grooves running to a hot centre."""
    x, y, r, th = polar(size)
    u = (th / TAU) % 1.0
    teeth = 0.5 + 0.5 * np.cos(24 * th)
    r_out = 0.955 - 0.030 * (1 - teeth) ** 1.6
    ring_m = smoothstep(0.60, 0.635, r) * (1 - smoothstep(r_out - 0.008, r_out + 0.004, r))
    light = 0.5 + 0.5 * np.cos(th + 0.9)
    ridges = 0.82 + 0.18 * np.cos(24 * th) * smoothstep(0.7, 0.85, r)
    fibre_g = 0.88 + 0.12 * noise1(u, 160, 7)
    ring_l = (0.44 + 0.36 * light * (0.6 + 0.4 * smoothstep(0.6, 0.95, r))) * ridges * fibre_g
    bevel = np.exp(-((r - 0.575) / 0.028) ** 2)
    bevel_l = 0.55 + 0.45 * light
    spiral = np.cos(8 * th + 7.5 * np.log(r + 0.06))
    lines = smoothstep(0.80, 0.97, spiral) * smoothstep(0.04, 0.22, r) * (1 - smoothstep(0.50, 0.58, r))
    core = np.exp(-(r / 0.10) ** 2)
    bore = (0.10 + 0.75 * lines) * (1 - smoothstep(0.50, 0.58, r)) + core * 0.9
    a = np.clip(ring_m * 0.96 + bevel * 0.85 + bore, 0, 1)
    lum = np.where(r > 0.60, ring_l, np.where(bevel > 0.2, bevel_l, 0.55 + 0.45 * np.clip(lines + core, 0, 1)))
    save(lum, a, "body_rifling")


# ------------------------------------------------------------------------------------------------ strands, burst, spark
def strand(w=64, h=256):
    """A whipping tendon: thick at the base (bottom), curving, twisted fibre body with a bright core, tapering to a hair-fine tip (top)."""
    xx, yy = grid(w, h)
    u = (xx + 0.5) / w * 2 - 1
    v = 1 - (yy + 0.5) / h
    xc = 0.38 * np.sin(v * math.pi * 1.45 + 0.35) * v ** 0.85
    half = 0.34 * (1 - v) ** 0.65 * smoothstep(0.0, 0.05, v) + 0.012
    d = (u - xc) / half
    cov = smoothstep(1.0, 0.72, np.abs(d))
    body = np.sqrt(np.clip(1 - np.minimum(np.abs(d), 1) ** 2, 0, 1))
    twist = 0.72 + 0.28 * np.sin(v * 150 + d * 5.0)
    core = np.exp(-(d / 0.30) ** 2)
    lum = np.clip(0.42 + 0.50 * body * twist + 0.35 * core, 0, 1)
    halo = np.exp(-(d / 2.1) ** 2) * 0.30 * (1 - 0.6 * v)
    tipfade = 1 - smoothstep(0.95, 1.0, v)
    alpha = np.maximum(cov, halo) * tipfade * smoothstep(0.0, 0.02, v)
    save(lum, alpha, "body_strand")


def burst(size=256):
    """A starburst of fine fibre rays of different lengths around a hot core, with a thin broken ring."""
    x, y, r, th = polar(size)
    u = (th / TAU) % 1.0
    rng = np.random.default_rng(9001)
    acc = np.zeros_like(x)
    n = 46
    for i in range(n):
        a0 = TAU * i / n + rng.uniform(-0.06, 0.06)
        length = 0.42 + 0.56 * rng.random() ** 1.6
        w0 = rng.uniform(0.007, 0.020)
        dx, dy = x * math.cos(a0) + y * math.sin(a0), -x * math.sin(a0) + y * math.cos(a0)
        t = np.clip(dx / length, 0, 1)
        width = w0 * (1 - t * 0.8) + 0.0025
        acc += np.where((dx > 0.02) & (dx < length), np.exp(-(dy / width) ** 2) * (1 - t) ** 1.25 * rng.uniform(0.6, 1.0), 0)
    core = np.exp(-(r / 0.16) ** 2) + 0.4 * np.exp(-(r / 0.38) ** 2)
    ring = np.exp(-((r - 0.62) / 0.010) ** 2) * 0.5 * (0.3 + 0.7 * noise1(u, 13, 9))
    a = np.clip(acc + core + ring, 0, 1) * (1 - smoothstep(0.93, 1.0, r))
    save(np.ones_like(a), a, "body_burst")


def spark(size=64):
    """A heat ember: round core with a long vertical tail glint (rotate it along its direction of flight)."""
    x, y, r, _ = polar(size)
    core = np.exp(-(r / 0.14) ** 2)
    tail = np.exp(-(x / 0.07) ** 2) * np.exp(-(np.abs(y) / 0.62) ** 1.6)
    cross = np.exp(-(y / 0.05) ** 2) * np.exp(-(np.abs(x) / 0.30) ** 1.6) * 0.55
    halo = np.exp(-(r / 0.40) ** 2) * 0.28
    a = np.clip(core * 1.1 + tail * 0.9 + cross + halo, 0, 1) * (1 - smoothstep(0.92, 1.0, r))
    save(np.ones_like(a), a, "body_spark")


# ------------------------------------------------------------------------------------------------ emblem
def catmull(points, n=14):
    """Closed Catmull-Rom spline through the points."""
    out = []
    m = len(points)
    for i in range(m):
        p0, p1, p2, p3 = (np.array(points[(i + k) % m], np.float64) for k in (-1, 0, 1, 2))
        for t in np.linspace(0, 1, n, endpoint=False):
            out.append(tuple(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3)))
    return out


def flex(size=256):
    """A flexed arm (the strength sign): shoulder and bicep on the left, forearm rising to a fist. Embossed like a medal: rim light, inner
    bevel line, a soft inner glow and fibre lines across the bicep. The emblem of the field and of the burst."""
    S = size * SS
    outline = [(0.07, 0.60), (0.10, 0.47), (0.18, 0.36), (0.30, 0.28), (0.43, 0.27), (0.53, 0.32), (0.59, 0.40), (0.60, 0.30), (0.60, 0.20),
               (0.57, 0.12), (0.62, 0.05), (0.73, 0.03), (0.83, 0.07), (0.87, 0.16), (0.84, 0.26), (0.80, 0.36), (0.80, 0.50),
               (0.80, 0.64), (0.75, 0.76), (0.63, 0.84), (0.46, 0.88), (0.28, 0.86), (0.15, 0.80), (0.08, 0.71)]
    img = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(img)
    d.polygon([(px * S, py * S) for px, py in catmull(outline)], fill=255)
    m = to_arr(img, size)
    # emboss: a smooth height field from the blurred mask, lit from the upper left
    h = blur(m, 7.0)
    gy, gx = np.gradient(h)
    g = np.sqrt(gx * gx + gy * gy) + 1e-6
    lit = np.clip((gx * 0.62 + gy * 0.78) / g, -1, 1) * np.clip(g / 0.035, 0, 1)
    inner = np.clip((blur(m, 5.0) - 0.5) * 2.0, 0, 1)
    bevel = np.exp(-((blur(m, 3.5) - 0.78) / 0.07) ** 2) * m
    rim = np.clip(m - blur(m, 1.6) * 0.98, 0, 1) * 3.0
    xx, yy = grid(size, size)
    X, Y = xx / size, yy / size
    stri = 0.5 + 0.5 * np.cos(2 * math.pi * 11 * np.sqrt(((X - 0.36) / 0.26) ** 2 + ((Y - 0.58) / 0.20) ** 2))
    bic = np.exp(-((np.sqrt(((X - 0.36) / 0.26) ** 2 + ((Y - 0.58) / 0.20) ** 2)) / 0.9) ** 6)
    lum = np.clip(0.45 + 0.25 * inner + 0.22 * lit + 0.45 * bevel + 0.16 * stri * bic * m + rim, 0, 1)
    glow = np.exp(-(np.maximum(0, blur(1 - m, 6.0) - 0.5) / 0.05) ** 2) * 0.0
    halo = blur(m, 9.0) * (1 - m)
    a = np.clip(m * (0.84 + 0.16 * stri * bic) + halo * 0.9 + 0.3 * bevel, 0, 1)
    save(np.where(m > 0.5, lum, 1.0), a, "body_flex")


ALL = [steam, steam_jet, veins, flesh, ring, pulse, wall, fibre, slug, muzzle, rifling, strand, burst, spark, flex]

if __name__ == "__main__":
    import sys
    wanted = sys.argv[1:]
    for fn in ALL:
        if not wanted or fn.__name__ in wanted:
            fn()

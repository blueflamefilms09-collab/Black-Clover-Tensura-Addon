"""Generates the Black Oil Magic VFX textures (textures/particle/black_oil_*.png).

    python3 -B tools/gen_black_oil_textures.py

Look reference (the owner's anime still): a huge round pool of glowing magenta-red liquid, churned like cooled lava or curdled oil with
lighter pink ridges, inside a thick ragged black tar rim with torn, dripping edges; four small white candle flames float over it with soft
halos. Owner spec: an iridescent, viscous, highly explosive fluid.

Every sprite is deterministic (fixed seeds, no fonts). Most are grey-scale + alpha so the layer tints them (the dark tar is dark grey with
glossy highlights, so an ALPHA pass gives the black body and an ADD pass in pink lights only the highlights); the sheen and the bubble carry
their own thin-film colours.

  black_oil_pool     256  churned pool surface (grey, lit relief)
  black_oil_ridges   256  the lighter pink ridges and specks laid over the pool (additive)
  black_oil_rim      256  ragged black tar ring with drips and spatter, glossy
  black_oil_flame    256  two 128x256 candle-flame cells (slender / swayed), white
  black_oil_candle    64x128 a black wax stub with a dripped skirt
  black_oil_drop      64x128 a glossy oil drop, tip up
  black_oil_streak    64x256 a viscous string: head (thick bulb) at the bottom, thin beaded tail with drips at the top
  black_oil_tendril  128x256 a rising tar tendril with a hooked, bulbous tip
  black_oil_splat    256  a top-down splat with radial streaks and spatter
  black_oil_ripple   256  a ragged expanding ring
  black_oil_bubble    64  a thin-film bubble (coloured)
  black_oil_sheen    256  iridescent oil-slick bands (coloured)
  black_oil_burst    256  a spiky explosion flash
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")


# ------------------------------------------------------------------------------------------------ noise helpers
def smooth_noise(h, w, cy, cx, seed):
    g = np.random.default_rng(seed).random((cy + 1, cx + 1)).astype(np.float32)
    im = Image.fromarray(g, mode="F").resize((w, h), Image.BICUBIC)
    return np.clip(np.asarray(im), 0.0, 1.0)


def fbm(h, w, base, octaves, seed, gain=0.5):
    total, amp, norm = np.zeros((h, w), np.float32), 1.0, 0.0
    for o in range(octaves):
        c = base * (2 ** o)
        total += amp * smooth_noise(h, w, max(1, c * h // max(h, w)), max(1, c * w // max(h, w)), seed + 101 * o)
        norm += amp
        amp *= gain
    return total / norm


def sstep(a, b, x):
    u = np.clip((x - a) / (b - a), 0.0, 1.0)
    return u * u * (3 - 2 * u)


def grid(h, w):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    return (x + 0.5) / w * 2 - 1, (y + 0.5) / h * 2 - 1


def ang_noise(theta, seed, n=14, decay=1.0):
    """Periodic noise in -1..1 along an angle array (sum of harmonics)."""
    r = np.random.default_rng(seed)
    out, norm = np.zeros_like(theta), 0.0
    for k in range(1, n + 1):
        a = 1.0 / (k ** decay)
        out += a * np.sin(k * theta + r.random() * 6.2832)
        norm += a
    return out / norm * 1.6


def warp(img, dx, dy):
    """Bilinear sample img at (x + dx, y + dy) in pixels."""
    h, w = img.shape
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    sx, sy = np.clip(x + dx, 0, w - 1.001), np.clip(y + dy, 0, h - 1.001)
    x0, y0 = sx.astype(int), sy.astype(int)
    fx, fy = sx - x0, sy - y0
    return (img[y0, x0] * (1 - fx) * (1 - fy) + img[y0, x0 + 1] * fx * (1 - fy) + img[y0 + 1, x0] * (1 - fx) * fy + img[y0 + 1, x0 + 1] * fx * fy)


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), mode="L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im).astype(np.float32) / 255.0


def lit(height, light=(-0.55, -0.65, 0.52), k=1.0):
    """Lambert shading of a height field (values 0..1). Returns 0..1."""
    gy, gx = np.gradient(height.astype(np.float32))
    nx, ny, nz = -gx * k, -gy * k, np.ones_like(gx)
    ln = np.sqrt(nx * nx + ny * ny + nz * nz)
    lx, ly, lz = light
    ll = math.sqrt(lx * lx + ly * ly + lz * lz)
    return np.clip((nx * lx + ny * ly + nz * lz) / (ln * ll), 0, 1)


def rgba(l, a, color=None):
    """Grey-scale l + alpha a (0..1 arrays) -> RGBA image; or a colour array (h, w, 3) 0..1."""
    h, w = a.shape
    out = np.zeros((h, w, 4), np.uint8)
    if color is None:
        out[..., 0] = out[..., 1] = out[..., 2] = (np.clip(l, 0, 1) * 255).astype(np.uint8)
    else:
        out[..., :3] = (np.clip(color, 0, 1) * 255).astype(np.uint8)
    out[..., 3] = (np.clip(a, 0, 1) * 255).astype(np.uint8)
    return Image.fromarray(out, "RGBA")


def save(im, name):
    im.save(os.path.join(OUT, "black_oil_%s.png" % name))
    print("black_oil_%s.png %dx%d" % (name, im.width, im.height))


def down(img, f):
    return img.resize((img.width // f, img.height // f), Image.LANCZOS)


def lit_blob(w, h, half, centre=None, gloss=1.0, base=0.10, rim=0.45, seed=0, edge_noise=0.0, ss=3):
    """A glossy tube/blob shape: half(y) = half width in px at row y (0 = none), centre(y) = x centre. Returns (L, alpha) arrays."""
    W, H = w * ss, h * ss
    ys = (np.arange(H, dtype=np.float32) + 0.5) / ss
    xs = (np.arange(W, dtype=np.float32) + 0.5) / ss
    hw = np.array([half(y) for y in ys], np.float32)[:, None]
    cx = np.array([centre(y) if centre else w / 2 for y in ys], np.float32)[:, None]
    dx = xs[None, :] - cx
    if edge_noise > 0:
        hw = hw * (1 + edge_noise * (fbm(H, W, 10, 3, seed) - 0.5) * 2)
    inside = np.clip(hw * hw - dx * dx, 0, None)
    hgt = np.sqrt(inside)                               # cylinder / tube cross-section
    alpha = (hw > 0.4) & (np.abs(dx) < hw)
    alpha = alpha.astype(np.float32)
    shade = lit(hgt / max(1.0, float(hw.max())) * 4.0, k=6.0)
    spec = np.clip(shade, 0, 1) ** 6
    # a rim light on the far side (the pink bounce of the pool)
    nd = np.clip(np.abs(dx) / np.maximum(hw, 0.5), 0, 1)
    rimlight = nd ** 3 * (dx > 0)
    L = base + gloss * 0.8 * spec + rim * rimlight * 0.8
    L = L * alpha
    return down_arr(L, ss), down_arr(alpha, ss)


def down_arr(a, f):
    h, w = a.shape
    return a.reshape(h // f, f, w // f, f).mean(axis=(1, 3))


# ------------------------------------------------------------------------------------------------ pool
def pool():
    S = 256
    X, Y = grid(S, S)
    r, th = np.sqrt(X * X + Y * Y), np.arctan2(Y, X)
    wx = (fbm(S, S, 3, 4, 11) - 0.5) * 70
    wy = (fbm(S, S, 3, 4, 12) - 0.5) * 70
    n1 = warp(fbm(S, S, 4, 5, 13), wx, wy)
    n2 = warp(fbm(S, S, 7, 4, 14), wx * 0.6, wy * 0.6)
    ridge = (1 - np.abs(2 * n1 - 1)) ** 2.4                     # churned ridges: curdled / cooled-lava look
    h = 0.55 * ridge + 0.30 * n2 + 0.15 * fbm(S, S, 16, 3, 15)
    shade = lit(h, k=22.0)
    L = (0.30 + 0.62 * shade) * (0.55 + 0.45 * h) + 0.30 * ridge ** 3
    crack = np.exp(-((n2 - 0.5) / 0.05) ** 2)                  # dark seams between plates
    L *= 1 - 0.40 * crack
    L *= 1 - 0.55 * sstep(0.55, 0.92, r)                        # darker where the tar rim is
    L += (fbm(S, S, 64, 2, 16) - 0.5) * 0.10
    L = L * 1.18 + 0.06
    edge = 0.90 + 0.03 * ang_noise(th, 17)
    a = 1 - sstep(edge - 0.035, edge, r)
    save(rgba(np.clip(L, 0, 1), a), "pool")


def ridges():
    S = 256
    X, Y = grid(S, S)
    r, th = np.sqrt(X * X + Y * Y), np.arctan2(Y, X)
    wx = (fbm(S, S, 3, 4, 21) - 0.5) * 90
    wy = (fbm(S, S, 3, 4, 22) - 0.5) * 90
    n = warp(fbm(S, S, 5, 4, 23), wx, wy)
    lines = (1 - np.abs(2 * n - 1)) ** 14                       # thin lighter ridges
    m = fbm(S, S, 3, 3, 24)
    lines *= sstep(0.35, 0.65, m)
    n2 = warp(fbm(S, S, 9, 3, 25), wx * 0.5, wy * 0.5)
    lines2 = (1 - np.abs(2 * n2 - 1)) ** 30 * 0.8
    rng = np.random.default_rng(26)
    specks = np.zeros((S, S), np.float32)
    img = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(img)
    for _ in range(90):                                         # tiny hot specks
        x, y, rr = rng.random() * S, rng.random() * S, 0.6 + rng.random() * 1.6
        d.ellipse([x - rr, y - rr * 0.7, x + rr, y + rr * 0.7], fill=int(120 + rng.random() * 135))
    specks = np.asarray(img.filter(ImageFilter.GaussianBlur(0.6))).astype(np.float32) / 255
    v = np.clip(lines + lines2 + specks * 0.9, 0, 1)
    a = v * (1 - sstep(0.80, 0.90, r))
    save(rgba(np.clip(0.55 + 0.45 * v, 0, 1), a), "ridges")


# ------------------------------------------------------------------------------------------------ rim
def rim():
    S, ss = 256, 2
    N = S * ss
    X, Y = grid(N, N)
    r, th = np.sqrt(X * X + Y * Y), np.arctan2(Y, X)
    jag = (fbm(N, N, 22, 4, 31) - 0.5) * 0.05                   # high-frequency tearing of both edges
    rin = 0.745 + 0.040 * ang_noise(th, 32, decay=0.7) + jag * 1.4
    rout = 0.935 + 0.035 * ang_noise(th, 33, decay=0.6) + jag * 1.6
    band = ((r > rin) & (r < rout)).astype(np.float32)
    # pinch the band in places so it looks torn rather than a clean ring
    pinch = fbm(N, N, 5, 3, 34)
    band *= ((pinch > 0.12) | ((r > rin + 0.02) & (r < rout - 0.02))).astype(np.float32)
    mask = Image.fromarray((band * 255).astype(np.uint8), "L")
    d = ImageDraw.Draw(mask)
    rng = np.random.default_rng(35)
    c = N / 2
    for _ in range(95):                                          # spatter outside the ring
        a = rng.random() * 6.2832
        rr = (0.90 + rng.random() * 0.085) * c
        s = (0.004 + rng.random() ** 2 * 0.020) * c
        x, y = c + math.cos(a) * rr, c + math.sin(a) * rr
        d.ellipse([x - s, y - s, x + s, y + s], fill=255)
    for _ in range(40):                                          # drips hanging inward from the inner edge
        a = rng.random() * 6.2832
        r0 = (0.80 + rng.random() * 0.02) * c
        ln = (0.04 + rng.random() ** 1.5 * 0.12) * c
        wd = (0.014 + rng.random() * 0.020) * c
        ca, sa = math.cos(a), math.sin(a)
        r0 = r0 - 0.04 * c
        p0 = (c + ca * (r0 + wd), c + sa * (r0 + wd))
        pa = (c + ca * r0 - sa * wd, c + sa * r0 + ca * wd)
        pb = (c + ca * r0 + sa * wd, c + sa * r0 - ca * wd)
        tip = (c + ca * (r0 - ln), c + sa * (r0 - ln))
        d.polygon([pa, pb, p0, pa], fill=255)
        d.polygon([pa, tip, pb], fill=255)
        d.ellipse([tip[0] - wd * 0.7, tip[1] - wd * 0.7, tip[0] + wd * 0.7, tip[1] + wd * 0.7], fill=255)
    for _ in range(26):                                          # long wisps pulled outward from the outer edge
        a = rng.random() * 6.2832
        r0 = (0.90 + rng.random() * 0.02) * c
        ln = (0.03 + rng.random() * 0.05) * c
        wd = (0.012 + rng.random() * 0.016) * c
        ca, sa = math.cos(a), math.sin(a)
        d.polygon([(c + ca * r0 - sa * wd, c + sa * r0 + ca * wd), (c + ca * r0 + sa * wd, c + sa * r0 - ca * wd), (c + ca * (r0 + ln), c + sa * (r0 + ln))], fill=255)
    m = np.asarray(mask.filter(ImageFilter.GaussianBlur(0.8))).astype(np.float32) / 255
    soft = blur(m, 3.0 * ss)
    hgt = soft * 1.0 + (fbm(N, N, 40, 3, 36) - 0.5) * 0.18 * m
    sh = lit(hgt, k=26.0)
    spec = sh ** 8 * 0.55
    rimlit = np.clip(soft - blur(m, 7 * ss), 0, 1)
    L = 0.035 + 0.04 * fbm(N, N, 30, 3, 37) + 0.85 * spec * m + 0.22 * np.clip(rimlit * 8, 0, 1) * m * (0.5 + 0.5 * sh)
    L = down_arr(np.clip(L, 0, 1) * m, ss)
    a = down_arr(m, ss)
    save(rgba(L, a), "rim")


# ------------------------------------------------------------------------------------------------ flame / candle / drop
def flame_cell(seed, sway, lean):
    w, h, ss = 128, 256, 3
    W, H = w * ss, h * ss
    y, x = np.mgrid[0:H, 0:W].astype(np.float32)
    ty = 0.04 * H
    by = 0.96 * H
    t = np.clip((y - ty) / (by - ty), 0, 1)
    wmax = 0.30 * W * (1 - 0.15 * lean)
    prof = wmax * np.clip(np.sin(np.pi * np.clip(t, 0, 1) ** 1.45), 0, None) ** 0.9
    cx = W / 2 + sway * W * (1 - t) ** 2.0 + lean * W * 0.04 * np.sin(t * 7 + seed)
    d = np.abs(x - cx)
    inside = (y > ty) & (y < by)
    feather = 0.07 * W
    body = np.clip((prof - d) / feather, 0, 1) * inside
    core = np.clip(1 - d / np.maximum(prof * 0.8, 1), 0, 1) ** 0.6 * body
    halo_w = prof * 1.9 + 0.05 * W
    halo = np.exp(-(d / np.maximum(halo_w, 1)) ** 2) * (y > ty - 0.02 * H) * np.exp(-((t - 0.62) / 0.5) ** 2) * 0.35
    a = np.clip(np.maximum(body, halo), 0, 1)
    L = np.clip(0.80 + 0.20 * core, 0, 1)
    return down_arr(L, ss), down_arr(a, ss)


def flame():
    L0, a0 = flame_cell(1, 0.03, 0.2)
    L1, a1 = flame_cell(2, -0.07, 0.8)
    L = np.concatenate([L0, L1], axis=1)
    a = np.concatenate([a0, a1], axis=1)
    save(rgba(L, a), "flame")


def candle():
    w, h = 64, 64

    def half(y):
        if y < 20 or y > 61:
            return 0
        t = (y - 20) / 41.0
        skirt = 11 * math.exp(-((1 - t) / 0.22) ** 2)           # dripped, spreading foot
        wob = 1.6 * math.sin(t * 13.0) * (1 - t)                # drip lumps
        return 12 + 2 * t + skirt + wob
    L, a = lit_blob(w, h, half, base=0.10, rim=0.9, seed=41, edge_noise=0.05)
    img = rgba(L, a)
    d = ImageDraw.Draw(img)
    d.ellipse([w / 2 - 13, 15, w / 2 + 13, 26], fill=(150, 150, 150, 255))   # the lit, dished top
    d.ellipse([w / 2 - 9, 17, w / 2 + 9, 24], fill=(22, 22, 22, 255))
    d.line([w / 2, 20, w / 2 + 1, 10], fill=(18, 18, 18, 255), width=2)         # wick
    save(img.filter(ImageFilter.GaussianBlur(0.7)), "candle")


def drop():
    w, h = 64, 128

    def half(y):
        t = (y - 6) / 116.0
        if t < 0 or t > 1:
            return 0
        return 25 * math.sin(math.pi * t ** 1.9) ** 0.85 + 0.6
    L, a = lit_blob(w, h, half, base=0.12, rim=0.9, seed=42)
    # a hard specular dot, as on wet black paint
    img = rgba(L, a)
    d = ImageDraw.Draw(img)
    d.pieslice([w / 2 - 20, 56, w / 2 + 2, 100], 150, 215, fill=(255, 255, 255, 255))
    d.ellipse([w / 2 - 14, 52, w / 2 - 9, 57], fill=(255, 255, 255, 255))
    img = img.filter(ImageFilter.GaussianBlur(0.9))
    save(img, "drop")


def streak():
    w, h = 64, 256
    rng = np.random.default_rng(43)
    beads = [(rng.random() * 0.8 + 0.05, 0.05 + rng.random() * 0.05) for _ in range(6)]

    def half(y):
        t = y / h                                              # 0 tail (top) .. 1 head (bottom)
        base = 2.5 + 22 * sstep_f(0.45, 1.0, t) ** 1.3
        for pos, s in beads:
            base += 6.5 * math.exp(-((t - pos) / s) ** 2) * (0.4 + t)
        return base if t > 0.02 else 0
    L, a = lit_blob(w, h, half, centre=lambda y: w / 2 + 3 * math.sin(y * 0.05), base=0.12, rim=0.9, seed=44, edge_noise=0.10)
    img = rgba(L, a)
    d = ImageDraw.Draw(img)
    for i in range(7):                                         # detached drips falling off the tail
        y = 6 + i * 11 + rng.random() * 6
        x = w / 2 + (rng.random() - 0.5) * 20
        s = 1.5 + rng.random() * 2.0
        d.ellipse([x - s, y - s * 1.3, x + s, y + s * 1.3], fill=(70, 70, 70, 210))
    save(img, "streak")


def sstep_f(a, b, x):
    u = min(1.0, max(0.0, (x - a) / (b - a)))
    return u * u * (3 - 2 * u)


def tendril():
    w, h = 128, 256

    def centre(y):
        t = y / h
        return w / 2 + 22 * math.sin(t * 5.0 + 0.6) * (1 - t) ** 0.8 - 10 * sstep_f(0.8, 1.0, 1 - t)

    def half(y):
        t = y / h                                              # 0 tip (top) .. 1 base (bottom)
        if t < 0.03:
            return 0
        body = 5 + 30 * t ** 1.7
        bulb = 8 * math.exp(-((t - 0.17) / 0.07) ** 2)         # a heavy blob near the tip
        neck = -3 * math.exp(-((t - 0.30) / 0.05) ** 2)
        return max(0.0, body + bulb + neck)
    L, a = lit_blob(w, h, half, centre=centre, base=0.11, rim=0.9, seed=45, edge_noise=0.08)
    img = rgba(L, a)
    d = ImageDraw.Draw(img)
    # two thin side tendrils curling off the main one
    for (y0, dx, up) in ((150, -1, 62), (110, 1, 52)):
        x0 = centre(y0) + dx * 12
        pts = [(x0, y0)]
        for k in range(1, 14):
            t = k / 13
            pts.append((x0 + dx * (6 + 22 * t) + 6 * math.sin(t * 5), y0 - up * t))
        for k in range(len(pts) - 1):
            wd = max(1, int(5 * (1 - k / 13)))
            d.line([pts[k], pts[k + 1]], fill=(56, 56, 56, 255), width=wd)
        px, py = pts[-1]
        d.ellipse([px - 3, py - 3, px + 3, py + 3], fill=(80, 80, 80, 255))
    save(img.filter(ImageFilter.GaussianBlur(0.4)), "tendril")


# ------------------------------------------------------------------------------------------------ splat
def splat():
    S, ss = 256, 2
    N = S * ss
    img = Image.new("L", (N, N), 0)
    d = ImageDraw.Draw(img)
    rng = np.random.default_rng(51)
    c = N / 2
    X, Y = grid(N, N)
    r, th = np.sqrt(X * X + Y * Y), np.arctan2(Y, X)
    core = (r < 0.27 + 0.07 * ang_noise(th, 52, 10, 0.9)).astype(np.float32)
    img = Image.fromarray((core * 255).astype(np.uint8), "L")
    d = ImageDraw.Draw(img)
    rays = 11
    for i in range(rays):
        a = (i + rng.random() * 0.7) / rays * 6.2832
        ln = (0.50 + rng.random() ** 0.8 * 0.44) * c
        wd = (0.055 + rng.random() * 0.060) * c
        r0 = 0.15 * c
        ca, sa = math.cos(a), math.sin(a)
        pts = []
        for k in range(0, 9):                                    # tapered, slightly curved streak
            t = k / 8
            bend = 0.10 * math.sin(a * 3 + i) * t * t
            cr = r0 + (ln - r0) * t
            ox = -sa * bend * c
            oy = ca * bend * c
            pts.append((c + ca * cr + ox, c + sa * cr + oy, wd * (1 - 0.70 * t ** 1.2) * (1 + 0.25 * math.sin(t * 9 + i))))
        for k in range(len(pts) - 1):
            x0, y0, w0 = pts[k]
            x1, y1, w1 = pts[k + 1]
            d.line([(x0, y0), (x1, y1)], fill=255, width=max(1, int((w0 + w1)) * 2))
            d.ellipse([x1 - w1, y1 - w1, x1 + w1, y1 + w1], fill=255)
        tx, ty, tw = pts[-1]
        rr = tw * (1.0 + rng.random() * 1.2) + 2
        d.ellipse([tx - rr, ty - rr, tx + rr, ty + rr], fill=255)
        if rng.random() < 0.7:                                    # a satellite droplet beyond the tip
            dd = ln + (0.04 + rng.random() * 0.07) * c
            s = (0.006 + rng.random() * 0.016) * c
            x, y = c + ca * dd, c + sa * dd
            d.ellipse([x - s, y - s, x + s, y + s], fill=255)
    for _ in range(70):                                           # fine spatter
        a = rng.random() * 6.2832
        rr = (0.30 + rng.random() ** 0.6 * 0.68) * c
        s = (0.003 + rng.random() ** 2 * 0.012) * c
        x, y = c + math.cos(a) * rr, c + math.sin(a) * rr
        d.ellipse([x - s, y - s, x + s, y + s], fill=255)
    m = np.asarray(img.filter(ImageFilter.GaussianBlur(1.2))).astype(np.float32) / 255
    m = sstep(0.35, 0.65, m)
    soft = blur(m, 3.0 * ss)
    hgt = soft + (fbm(N, N, 36, 3, 53) - 0.5) * 0.08 * m
    sh = lit(hgt, k=24.0)
    L = 0.06 + 0.04 * fbm(N, N, 30, 3, 54) + 0.8 * sh ** 5 + 0.25 * np.clip((soft - blur(m, 8 * ss)) * 6, 0, 1) * sh
    save(rgba(down_arr(np.clip(L * m, 0, 1), ss), down_arr(m, ss)), "splat")


# ------------------------------------------------------------------------------------------------ ripple / bubble / sheen / burst
def ripple():
    S = 256
    X, Y = grid(S, S)
    r, th = np.sqrt(X * X + Y * Y), np.arctan2(Y, X)
    rr = 0.70 + 0.035 * ang_noise(th, 61, 9, 0.9) + (fbm(S, S, 14, 3, 62) - 0.5) * 0.04
    ring = np.exp(-((r - rr) / 0.050) ** 2)
    inner = 0.30 * np.exp(-((r - rr + 0.12) / 0.07) ** 2) * (0.5 + 0.5 * np.sin(th * 7 + r * 20))
    outer = 0.18 * np.exp(-((r - rr - 0.10) / 0.06) ** 2)
    v = np.clip(ring + inner + outer, 0, 1) * (1 - sstep(0.93, 1.0, r))
    v *= 0.65 + 0.35 * fbm(S, S, 6, 3, 63)
    save(rgba(np.clip(0.6 + 0.4 * v, 0, 1), np.clip(v, 0, 1)), "ripple")


def hue_palette(t):
    """Cyclic oil-slick palette: magenta -> violet -> teal-green (the owner's glow 40E0A0) -> gold -> magenta. t: array in any range."""
    keys = np.array([[1.00, 0.18, 0.60], [0.50, 0.28, 1.00], [0.25, 0.88, 0.63], [1.00, 0.80, 0.35]], np.float32)
    f = (t % 1.0) * 4
    i0 = np.floor(f).astype(int) % 4
    i1 = (i0 + 1) % 4
    u = (f - np.floor(f))[..., None]
    u = u * u * (3 - 2 * u)
    return keys[i0] * (1 - u) + keys[i1] * u


def bubble():
    S = 64
    X, Y = grid(S, S)
    r, th = np.sqrt(X * X + Y * Y), np.arctan2(Y, X)
    film = np.exp(-((r - 0.80) / 0.12) ** 2)
    body = 0.10 * (r < 0.84)
    col = hue_palette(th / 6.2832 * 1.0 + r * 0.8)
    spec = np.exp(-(((X + 0.38) ** 2 + (Y + 0.40) ** 2) / 0.014))
    spec2 = 0.5 * np.exp(-(((X - 0.42) ** 2 + (Y - 0.40) ** 2) / 0.02))
    a = np.clip(film * 0.95 + body + spec + spec2, 0, 1) * (1 - sstep(0.93, 1.0, r))
    mix = np.clip(spec + spec2, 0, 1)[..., None]
    c = col * (1 - mix) + mix
    save(rgba(None, a, c), "bubble")


def sheen():
    S = 256
    X, Y = grid(S, S)
    r = np.sqrt(X * X + Y * Y)
    wx = (fbm(S, S, 3, 4, 71) - 0.5) * 120
    wy = (fbm(S, S, 3, 4, 72) - 0.5) * 120
    n = warp(fbm(S, S, 3, 5, 73), wx, wy)
    t = n * 3.2 + 0.25 * fbm(S, S, 12, 3, 74)
    col = hue_palette(t)
    m = warp(fbm(S, S, 4, 4, 75), wx * 0.7, wy * 0.7)
    a = sstep(0.30, 0.78, m) * (0.50 + 0.50 * fbm(S, S, 10, 3, 76)) * (1 - sstep(0.80, 0.98, r))
    save(rgba(None, np.clip(a, 0, 1), col), "sheen")


def burst():
    S = 256
    X, Y = grid(S, S)
    r, th = np.sqrt(X * X + Y * Y), np.arctan2(Y, X)
    rng = np.random.default_rng(81)
    edge = np.full_like(r, 0.26) + 0.05 * ang_noise(th, 82, 8, 0.8)
    for _ in range(19):
        a0 = rng.random() * 6.2832
        ln = 0.30 + rng.random() ** 1.3 * 0.62
        wd = 0.05 + rng.random() * 0.12
        dth = np.angle(np.exp(1j * (th - a0)))
        edge = np.maximum(edge, 0.24 + ln * np.exp(-(dth / wd) ** 2 * 1.4))
    v = np.clip(1 - r / np.maximum(edge, 0.05), 0, 1) ** 1.1
    v = v * (0.75 + 0.25 * fbm(S, S, 8, 3, 83))
    core = np.exp(-(r / 0.16) ** 2) + 0.35 * np.exp(-(r / 0.42) ** 2)
    a = np.clip(v + core, 0, 1)
    save(rgba(np.clip(0.75 + 0.25 * np.clip(core, 0, 1), 0, 1), a), "burst")


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    pool(); ridges(); rim(); flame(); candle(); drop(); streak(); tendril(); splat(); ripple(); bubble(); sheen(); burst()

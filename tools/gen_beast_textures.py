"""Generates the Beast Magic VFX textures. Deterministic (fixed seeds), no fonts, no external images.

    python3 -B tools/gen_beast_textures.py            # all textures
    python3 -B tools/gen_beast_textures.py flame claw  # only the named ones

Output: src/main/resources/assets/nusmp/textures/particle/beast_*.png

Look (owner's art reference, an anime still): a translucent orange beast-fire streaming off a clawed arm, drawn like cel-shaded
flame: ragged licks, a hot yellow core, orange body, darker red-orange edges with thin dark linework, and fur-like streaks. It reads as
an animal's spirit (claws, fangs, mane), not as plain fire. The flame sprites therefore carry their own colour (the colour IS the
point: stepped cel bands, drawn with the ADD blend and nudged toward the spell tint); every other sprite is grey-scale + alpha and
takes its colour from the vertex colour.

Textures (size): flame 128x256 (a cluster of three flame tongues, tip up, cel bands) | stream 64x256 (a long narrow flame stream,
head up, for trails) | licks 512x128 (atlas of four small detached licks, for embers and fur) | spirit 256 (a roaring beast head in
flame with the mane streaming back to the left, faces right) | claw 256 (three parallel gashes, top to bottom) | paw 128 (paw print
with claws) | sigil 512 (ground circle: fang ring, claw spiral, paw ring) | burst 256 (roar star of ragged spikes) | ring 256
(shock ring with fang teeth) | fangs 256x64 (atlas of four curved fang / claw shards) | haze 256 (heat haze puff with fur streaks)
| glint 64 (claw glint, four curved points).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")

# the palette of the still: hot core, body, edge, linework
C_HOT = np.array([1.00, 0.97, 0.70], np.float32)
C_CORE = np.array([1.00, 0.80, 0.26], np.float32)
C_BODY = np.array([1.00, 0.52, 0.10], np.float32)
C_EDGE = np.array([0.90, 0.25, 0.06], np.float32)
C_LINE = np.array([0.62, 0.12, 0.04], np.float32)


# ------------------------------------------------------------------------------------------------ plumbing
def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, "beast_" + name + ".png"), optimize=True)
    print("wrote beast_" + name, im.size)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def smooth(a, b, x):
    t = np.clip((x - a) / (b - a), 0, 1)
    return t * t * (3 - 2 * t)


def blur(a, sigma):
    if sigma <= 0:
        return a
    h, w = a.shape
    fy, fx = np.fft.fftfreq(h)[:, None], np.fft.fftfreq(w)[None, :]
    g = np.exp(-2 * (math.pi * sigma) ** 2 * (fx * fx + fy * fy))
    return np.real(np.fft.ifft2(np.fft.fft2(a) * g)).astype(np.float32)


def value_noise(w, h, cw, ch, seed):
    """Smooth value noise, cell size cw x ch pixels (anisotropic cells give streaks). Not tiled."""
    rng = np.random.default_rng(seed)
    gw, gh = max(1, w // cw) + 2, max(1, h // ch) + 2
    g = rng.random((gh + 1, gw + 1)).astype(np.float32)
    xx, yy = grid(w, h)
    fx, fy = xx / cw, yy / ch
    x0, y0 = np.floor(fx).astype(int), np.floor(fy).astype(int)
    tx, ty = fx - x0, fy - y0
    tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
    a = g[y0, x0] * (1 - tx) + g[y0, x0 + 1] * tx
    b = g[y0 + 1, x0] * (1 - tx) + g[y0 + 1, x0 + 1] * tx
    return a * (1 - ty) + b * ty


def fbm(w, h, cell, seed, octaves=4):
    n, amp, tot = 0, 1.0, 0
    for o in range(octaves):
        c = max(2, cell >> o)
        n = n + value_noise(w, h, c, c, seed + 17 * o) * amp
        tot += amp
        amp *= 0.5
    return n / tot


def curve1d(seed, n=10):
    """A smooth random 1D function of t in 0..1, values 0..1 (for ragged edges)."""
    rng = np.random.default_rng(seed)
    pts = rng.random(n + 1)
    return lambda t: np.interp(np.clip(t, 0, 1) * n, np.arange(n + 1), pts)


def polygon_mask(w, h, polys, ss=4, closed=True):
    im = Image.new("L", (w * ss, h * ss), 0)
    d = ImageDraw.Draw(im)
    for p in polys:
        d.polygon([(x * ss, y * ss) for x, y in p], fill=255)
    return np.asarray(im.resize((w, h), Image.BOX), np.float32) / 255.0


def line_mask(w, h, lines, width, ss=4):
    im = Image.new("L", (w * ss, h * ss), 0)
    d = ImageDraw.Draw(im)
    for pts in lines:
        d.line([(x * ss, y * ss) for x, y in pts], fill=255, width=max(1, int(width * ss)), joint="curve")
    return np.asarray(im.resize((w, h), Image.BOX), np.float32) / 255.0


def finish_grey(lum, alpha, name):
    """Grey-scale + alpha sprite; transparent texels take the colour of their opaque neighbours (no dark fringes)."""
    lum, alpha = np.clip(lum, 0, 1), np.clip(alpha, 0, 1)
    wa = blur(lum * alpha, 2.0) / np.maximum(blur(alpha, 2.0), 1e-3)
    lum = lum * alpha + np.clip(wa, 0, 1) * (1 - alpha)
    g = (lum * 255 + 0.5).astype(np.uint8)
    save(Image.fromarray(np.dstack([g, g, g, (alpha * 255 + 0.5).astype(np.uint8)]), "RGBA"), name)


def finish_colour(rgb, alpha, name):
    rgb, alpha = np.clip(rgb, 0, 1), np.clip(alpha, 0, 1)
    wa = np.dstack([blur(rgb[..., i] * alpha, 2.0) for i in range(3)]) / np.maximum(blur(alpha, 2.0), 1e-3)[..., None]
    rgb = rgb * alpha[..., None] + np.clip(wa, 0, 1) * (1 - alpha[..., None])
    img = np.dstack([(rgb * 255 + 0.5).astype(np.uint8), (alpha * 255 + 0.5).astype(np.uint8)])
    save(Image.fromarray(img, "RGBA"), name)


# ------------------------------------------------------------------------------------------------ cel-shaded flame
def cel_colour(h, w_edge=0.025):
    """Heat 0..1 -> stepped colour and alpha: rim, edge, body, core, hot. Bands are hard with a ~1 px blend, like cel animation."""
    s1, s2, s3, s4 = (smooth(t - w_edge, t + w_edge, h) for t in (0.14, 0.38, 0.62, 0.85))
    rgb = C_LINE[None, None] * np.ones(h.shape + (1,), np.float32)
    for s, c in ((s1, C_EDGE), (s2, C_BODY), (s3, C_CORE), (s4, C_HOT)):
        rgb = rgb * (1 - s[..., None]) + c[None, None] * s[..., None]
    alpha = 0.50 + 0.18 * s1 + 0.14 * s2 + 0.08 * s3 + 0.06 * s4
    return rgb, alpha


def tongue_field(w, h, seed, cx0, lean, width, length, wobble, rag, base_w=1.0, phase=0.0, v_base=1.0):
    """One flame tongue. Returns (r, t): r = |distance from the centre line| / half width (<1 inside), t = 0 at the tip, 1 at the base."""
    xx, yy = grid(w, h)
    u, v = xx / w, yy / h
    v_tip = v_base - length
    t = (v - v_tip) / max(1e-4, v_base - v_tip)
    tc = np.clip(t, 0, 1)
    cx = cx0 + lean * (1 - tc) ** 1.4 + wobble * np.sin(tc * 5.2 + phase) * (1 - tc) ** 0.8
    hw = width * (tc ** 0.62) * (1 - 0.18 * tc) * base_w
    left, right = curve1d(seed, 14), curve1d(seed + 1, 14)
    fine_l, fine_r = curve1d(seed + 2, 40), curve1d(seed + 3, 40)
    dx = u - cx
    ragged = np.where(dx < 0, 1 + rag * (left(tc) - 0.5) * 2 + rag * 0.5 * (fine_l(tc) - 0.5) * 2, 1 + rag * (right(tc) - 0.5) * 2 + rag * 0.5 * (fine_r(tc) - 0.5) * 2)
    r = np.abs(dx) / np.maximum(hw * ragged, 1e-4)
    r = np.where((t < 0) | (t > 1.0), 9.0, r)
    return r, tc


def flame_image(w, h, tongues, seed, fade_base=0.14):
    """Union of tongues -> colour + alpha with cel bands, dark linework between bands and fur-like streaks."""
    rmin = np.full((h, w), 9.0, np.float32)
    tbest = np.zeros((h, w), np.float32)
    for tg in tongues:
        r, t = tongue_field(w, h, seed=tg.pop("seed"), **tg)
        better = r < rmin
        rmin = np.where(better, r, rmin)
        tbest = np.where(better, t, tbest)
    inside = smooth(1.06, 0.94, rmin)
    streak = value_noise(w, h, 5, 40, seed + 9) * 0.6 + value_noise(w, h, 3, 18, seed + 10) * 0.4
    heat = np.clip((1 - np.clip(rmin, 0, 1)) ** 0.85 * 0.92 + 0.30 * tbest - 0.12, 0, 1) + (streak - 0.5) * 0.12
    rgb, alpha = cel_colour(heat)
    # thin dark linework on the band borders (the cel outline), strongest on the middle borders
    line = np.zeros_like(heat)
    for t0, k in ((0.14, 0.55), (0.38, 0.8), (0.62, 0.45)):
        line = np.maximum(line, np.exp(-((heat - t0) / 0.018) ** 2) * k)
    rgb = rgb * (1 - 0.65 * line[..., None]) + C_LINE[None, None] * 0.65 * line[..., None]
    yy = np.mgrid[0:h, 0:w][0].astype(np.float32) / h
    alpha = alpha * inside * smooth(1.0, 1.0 - fade_base, yy)
    return rgb, alpha


def flame():
    w, h = 128, 256
    rgb, a = flame_image(w, h, [
        dict(seed=11, cx0=0.52, lean=0.10, width=0.30, length=0.97, wobble=0.045, rag=0.30, phase=0.3),
        dict(seed=21, cx0=0.30, lean=-0.17, width=0.17, length=0.60, wobble=0.04, rag=0.30, phase=1.7, v_base=0.98),
        dict(seed=31, cx0=0.73, lean=0.20, width=0.17, length=0.50, wobble=0.04, rag=0.30, phase=2.5, v_base=0.98),
    ], 41)
    finish_colour(rgb, a, "flame")


def stream():
    w, h = 64, 256
    rgb, a = flame_image(w, h, [
        dict(seed=51, cx0=0.5, lean=0.0, width=0.30, length=0.99, wobble=0.05, rag=0.22, phase=0.0),
        dict(seed=61, cx0=0.30, lean=-0.10, width=0.13, length=0.55, wobble=0.04, rag=0.3, phase=1.0, v_base=0.9),
    ], 71, fade_base=0.30)
    finish_colour(rgb, a, "stream")


def licks():
    cw, ch = 128, 128
    rgb, a = np.zeros((ch, cw * 4, 3), np.float32), np.zeros((ch, cw * 4), np.float32)
    shapes = [
        [dict(seed=101, cx0=0.5, lean=0.20, width=0.32, length=0.92, wobble=0.05, rag=0.28, phase=0.0)],
        [dict(seed=111, cx0=0.5, lean=-0.18, width=0.28, length=0.88, wobble=0.07, rag=0.32, phase=2.0)],
        [dict(seed=121, cx0=0.45, lean=0.05, width=0.36, length=0.80, wobble=0.03, rag=0.25, phase=1.0),
         dict(seed=122, cx0=0.62, lean=0.22, width=0.17, length=0.55, wobble=0.04, rag=0.3, phase=3.0, v_base=0.96)],
        [dict(seed=131, cx0=0.5, lean=0.0, width=0.24, length=0.96, wobble=0.09, rag=0.35, phase=4.0)],
    ]
    for i, tg in enumerate(shapes):
        r, al = flame_image(cw, ch, [dict(t) for t in tg], 140 + i, fade_base=0.18)
        rgb[:, i * cw:(i + 1) * cw], a[:, i * cw:(i + 1) * cw] = r, al
    finish_colour(rgb, a, "licks")


# ------------------------------------------------------------------------------------------------ the roaring beast head
def spirit():
    """A roaring beast (wolf / lion) head, facing right, in the same cel flame; the mane streams back to the left and up as flame strands."""
    n = 256
    head = [
        (240, 100), (242, 112), (232, 118), (226, 122), (222, 124), (218, 150), (212, 126), (204, 126), (190, 130), (172, 138),
        (188, 154), (212, 154), (214, 152), (220, 130), (226, 154), (236, 156), (242, 164), (236, 178), (214, 188), (186, 194),
        (164, 206), (140, 216), (112, 214), (86, 196), (70, 160), (76, 118), (96, 86), (120, 66), (122, 40), (136, 12), (152, 30),
        (160, 58), (174, 68), (192, 80), (214, 90), (234, 94),
    ]
    polys = [head]
    rng = np.random.default_rng(321)
    T = lambda pts: [(0.88 * x + 40, 0.88 * y + 24) for x, y in pts]
    roots = [(100, 84, -2.5), (112, 70, -2.3), (86, 104, -2.9), (78, 130, -3.1), (74, 156, -3.35), (86, 184, -3.55), (104, 204, -3.7), (128, 58, -2.2), (140, 24, -2.4), (122, 48, -2.7)]
    for i, (rx, ry, ang0) in enumerate(roots):
        ln = rng.uniform(70, 125) if i < 7 else rng.uniform(40, 70)
        wd = rng.uniform(9, 14)
        sway = rng.uniform(0.25, 0.5) * (1 if i % 2 == 0 else -1)
        steps = 22
        left, right = [], []
        x, y = float(rx), float(ry)
        for k in range(steps + 1):
            t = k / steps
            ang = ang0 + sway * math.sin(t * 3.0 + i) * 0.7 + 0.35 * t * (1 if ang0 < -3.0 else -1)
            if k:
                x += math.cos(ang) * ln / steps
                y += math.sin(ang) * ln / steps - 0.6 * t * ln / steps
            wk = wd * (1 - t) ** 0.85 * (1 + 0.25 * math.sin(t * 17 + i * 3)) + 0.2
            nx, ny = -math.sin(ang), math.cos(ang)
            left.append((x + nx * wk, y + ny * wk))
            right.append((x - nx * wk, y - ny * wk))
        polys.append(left + right[::-1])
    m = polygon_mask(n, n, [T(q) for q in polys], ss=4)
    eye_poly = [(182, 101), (200, 91), (210, 96), (192, 107)]
    eye = polygon_mask(n, n, [T(eye_poly)], ss=4)
    grooves = line_mask(n, n, [T(q) for q in ([(172, 82), (196, 92)], [(214, 100), (228, 110)], [(150, 116), (186, 136)], [(142, 146), (172, 168)], [(120, 100), (150, 98), (170, 106)], [(206, 118), (222, 114)])], 1.6)
    b = blur(m, 20.0)
    heat = np.clip((b - 0.08) / 0.62, 0, 1) ** 1.15 * 0.72
    xx, yy = grid(n, n)
    heat = heat + (value_noise(n, n, 34, 5, 303) - 0.5) * 0.16 * smooth(0.2, 0.5, heat)
    heat = heat - grooves * 0.34 * (b > 0.6)
    rgb, al = cel_colour(heat)
    line = np.zeros_like(heat)
    for t0, k in ((0.14, 0.5), (0.38, 0.8), (0.62, 0.5)):
        line = np.maximum(line, np.exp(-((heat - t0) / 0.02) ** 2) * k)
    rgb = rgb * (1 - 0.65 * line[..., None]) + C_LINE[None, None] * 0.65 * line[..., None]
    alpha = al * smooth(0.0, 1.0, m) * smooth(0.0, 1.0, blur(m, 0.7))
    ye = blur(eye, 0.7)
    rgb = rgb * (1 - ye[..., None]) + C_LINE[None, None] * ye[..., None] * 0.5
    alpha = alpha * (1 - ye)
    finish_colour(rgb, alpha, "spirit")


# ------------------------------------------------------------------------------------------------ grey sprites
def claw():
    """Three parallel gashes, torn: bright core, darker rim, splinters; the slash runs from the top to the bottom."""
    n = 256
    xx, yy = grid(n, n)
    lum = np.zeros((n, n), np.float32)
    al = np.zeros((n, n), np.float32)
    specs = [(0.30, 0.10, 0.95, 0.060, 1.0), (0.50, 0.0, 1.00, 0.075, 0.0), (0.70, -0.10, 0.88, 0.055, 2.0)]
    for i, (cx0, lean, length, width, ph) in enumerate(specs):
        t = (yy / n - (0.5 - length / 2)) / length
        tc = np.clip(t, 0, 1)
        cx = cx0 + lean * (tc - 0.5) + 0.05 * np.sin(tc * 3.1 + ph) * (tc - 0.5)
        hw = width * np.clip(np.sin(np.pi * np.clip(tc, 0, 1) ** 0.72), 0, 1) ** 0.85
        rl, rr = curve1d(900 + i * 7, 18), curve1d(901 + i * 7, 18)
        dx = xx / n - cx
        rag = np.where(dx < 0, 1 + 0.45 * (rl(tc) - 0.5), 1 + 0.45 * (rr(tc) - 0.5))
        r = np.abs(dx) / np.maximum(hw * rag, 1e-4)
        r = np.where((t < 0) | (t > 1), 9.0, r)
        mask = smooth(1.1, 0.92, r)
        shade = 0.45 + 0.55 * smooth(0.95, 0.15, r)
        lum = np.where(mask > al, shade, lum)
        al = np.maximum(al, mask)
        # a faint glow beside the gash and a few splinters flying off it
        al = np.maximum(al, 0.22 * np.exp(-np.clip(r - 1, 0, 9) ** 1.2 * 1.4) * (t > 0) * (t < 1))
    rng = np.random.default_rng(77)
    sp = []
    for _ in range(26):
        x0, y0 = rng.uniform(0.18, 0.82) * n, rng.uniform(0.1, 0.9) * n
        ang = math.pi / 2 + rng.uniform(-0.5, 0.5)
        ln = rng.uniform(4, 14)
        sp.append([(x0, y0), (x0 + math.cos(ang) * ln, y0 + math.sin(ang) * ln)])
    s = line_mask(n, n, sp, 1.2)
    lum = np.maximum(lum, s * 0.9)
    al = np.maximum(al, s * 0.85)
    finish_grey(lum, al, "claw")


def paw():
    n = 128
    ss = 4
    pad = [(64 + 30 * math.cos(a), 84 + 22 * math.sin(a) * (1.0 if math.sin(a) > 0 else 1.15) - (math.sin(a) < 0) * 3 * abs(math.cos(a))) for a in np.linspace(0, 2 * math.pi, 60)]
    pad = [(x, y + (-6 if abs(x - 64) < 14 and y > 90 else 0)) for x, y in pad]
    m = polygon_mask(n, n, [pad], ss)
    toes = [(30, 54, 11, 15, -0.45), (50, 36, 11, 16, -0.15), (78, 36, 11, 16, 0.15), (98, 54, 11, 15, 0.45)]
    tm = np.zeros((n, n), np.float32)
    clawm = np.zeros((n, n), np.float32)
    for cx, cy, rx, ry, rot in toes:
        pts = [(cx + rx * math.cos(a) * math.cos(rot) - ry * math.sin(a) * math.sin(rot), cy + rx * math.cos(a) * math.sin(rot) + ry * math.sin(a) * math.cos(rot)) for a in np.linspace(0, 2 * math.pi, 40)]
        tm = np.maximum(tm, polygon_mask(n, n, [pts], ss))
        tip = (cx + math.sin(rot) * (ry + 12), cy - math.cos(rot) * (ry + 12))
        base_l = (cx - 4 * math.cos(rot) + math.sin(rot) * ry * 0.7, cy - 4 * math.sin(rot) - math.cos(rot) * ry * 0.7)
        base_r = (cx + 4 * math.cos(rot) + math.sin(rot) * ry * 0.7, cy + 4 * math.sin(rot) - math.cos(rot) * ry * 0.7)
        clawm = np.maximum(clawm, polygon_mask(n, n, [[base_l, tip, base_r]], ss))
    body = np.maximum(m, tm)
    soft = blur(body, 3.0)
    lum = 0.55 + 0.45 * smooth(0.4, 1.0, soft) + 0.1 * (value_noise(n, n, 5, 5, 5) - 0.5)
    lum = np.where(clawm > 0.1, 1.0, lum)
    alpha = np.maximum(body, clawm)
    finish_grey(lum, alpha, "paw")


def sigil():
    """Ground circle: an outer double ring with fang teeth pointing in, a ring of claw gashes, a paw ring, and a triple claw spiral at the centre."""
    n = 512
    ss = 2
    big = n * ss
    c = big / 2
    im = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(im)

    def ring(r, wd, v=255):
        d.ellipse([c - r, c - r, c + r, c + r], outline=v, width=int(wd))

    R = c - 6
    ring(R, 7 * ss / 2)
    ring(R - 12 * ss / 2, 2.5 * ss / 2, 190)
    # fang teeth pointing inward, alternating long and short
    teeth = 24
    for i in range(teeth):
        a = 2 * math.pi * i / teeth
        ln = (24 if i % 2 == 0 else 12) * ss / 2
        r0 = R - 13 * ss / 2
        da = 0.07 if i % 2 == 0 else 0.05
        p = [(c + math.cos(a - da) * r0, c + math.sin(a - da) * r0), (c + math.cos(a) * (r0 - ln), c + math.sin(a) * (r0 - ln)), (c + math.cos(a + da) * r0, c + math.sin(a + da) * r0)]
        d.polygon(p, fill=255 if i % 2 == 0 else 200)
    # inner thick ring
    r2 = R * 0.60
    ring(r2, 6 * ss / 2)
    ring(r2 - 9 * ss / 2, 2 * ss / 2, 170)
    # twelve triple-claw scratches between the teeth and the paws (curved, tapering), a tick ring beyond them
    rm = R * 0.855
    for i in range(12):
        a = 2 * math.pi * (i + 0.5) / 12
        for j, off in enumerate((-0.045, 0.0, 0.045)):
            pts_l, pts_r = [], []
            for k in range(13):
                t = k / 12
                rr0 = rm - 13 + 26 * t
                aa = a + off + 0.05 * t
                wd = 3.2 * math.sin(math.pi * t) ** 0.8 + 0.2
                pts_l.append((c + math.cos(aa) * (rr0 - wd), c + math.sin(aa) * (rr0 - wd)))
                pts_r.append((c + math.cos(aa) * (rr0 + wd), c + math.sin(aa) * (rr0 + wd)))
            d.polygon(pts_l + pts_r[::-1], fill=235)
    # eight paws round the inner ring, toes pointing inward
    for i in range(8):
        a = 2 * math.pi * i / 8
        rp = r2 + 36
        px, py = c + math.cos(a) * rp, c + math.sin(a) * rp
        rad = 15
        ux, uy = -math.cos(a), -math.sin(a)            # toward the centre
        sx, sy = -uy, ux
        d.ellipse([px - rad, py - rad * 0.8, px + rad, py + rad * 0.8], fill=235)
        for k in (-1.3, -0.45, 0.45, 1.3):
            tx, ty = px + sx * k * rad * 0.85 + ux * rad * (1.25 + 0.2 * (1 - abs(k) / 1.3)), py + sy * k * rad * 0.85 + uy * rad * (1.25 + 0.2 * (1 - abs(k) / 1.3))
            tr = rad * 0.38
            d.ellipse([tx - tr, ty - tr, tx + tr, ty + tr], fill=235)
    # three curved claws sweeping round the centre (triskelion)
    r3 = r2 * 0.80
    for i in range(3):
        a0 = 2 * math.pi * i / 3
        pts = []
        for k in range(0, 41):
            t = k / 40
            ang = a0 + t * 2.1
            rr = r3 * (0.12 + 0.88 * t ** 0.8)
            pts.append((c + math.cos(ang) * rr, c + math.sin(ang) * rr))
        wd = [max(0.4, 20 * math.sin(math.pi * min(1, (k / 40) ** 0.55))) for k in range(41)]
        left, right = [], []
        for k, (x, y) in enumerate(pts):
            ang = a0 + k / 40 * 2.1
            nx, ny = math.cos(ang), math.sin(ang)
            left.append((x + nx * wd[k], y + ny * wd[k]))
            right.append((x - nx * wd[k] * 0.35, y - ny * wd[k] * 0.35))
        d.polygon(left + right[::-1], fill=255)
    ring(r2 * 0.28, 4 * ss / 2, 220)
    arr = np.asarray(im.resize((n, n), Image.BOX), np.float32) / 255.0
    xx, yy = grid(n, n)
    rr = np.hypot(xx - n / 2, yy - n / 2) / (n / 2)
    ang = np.arctan2(yy - n / 2, xx - n / 2)
    rune = value_noise(n, n, 6, 6, 41) * 0.5 + value_noise(n, n, 18, 18, 42) * 0.5
    lum = 0.62 + 0.38 * smooth(0.3, 0.95, arr) + 0.10 * (rune - 0.5)
    glow = blur(arr, 5.0)
    # soft inner floor glow
    floor = 0.18 * smooth(1.0, 0.0, rr) * (1 - arr)
    alpha = np.clip(arr + 0.55 * glow * (1 - arr) + floor * 0.0, 0, 1)
    finish_grey(lum, alpha, "sigil")


def burst():
    n = 256
    xx, yy = grid(n, n)
    dx, dy = xx - n / 2, yy - n / 2
    r = np.hypot(dx, dy) / (n / 2)
    ang = np.arctan2(dy, dx)
    rng = np.random.default_rng(404)
    spikes = 22
    lens = rng.uniform(0.45, 1.0, spikes)
    lens[::4] = rng.uniform(0.85, 1.0, len(lens[::4]))
    phases = rng.uniform(-0.05, 0.05, spikes)
    a = np.zeros((n, n), np.float32)
    for i in range(spikes):
        a0 = 2 * math.pi * i / spikes + phases[i]
        da = (ang - a0 + math.pi) % (2 * math.pi) - math.pi
        # a curved, tapering lick: the angular half width shrinks with r, the axis bends a little
        bend = 0.18 * r
        da = da - bend
        hw = (0.115 + 0.03 * math.sin(i * 1.7)) * np.clip(1 - r / lens[i], 0, 1) ** 0.85 * (0.4 + r)
        a = np.maximum(a, smooth(1.1, 0.75, np.abs(da) / np.maximum(hw, 1e-4)) * (r < lens[i]))
    core = np.exp(-(r / 0.16) ** 2)
    glow = np.exp(-(r / 0.38) ** 1.6) * 0.5
    fur = value_noise(n, n, 3, 3, 8)
    alpha = np.clip(np.maximum(a * (0.75 + 0.25 * fur), glow) + core, 0, 1) * smooth(1.0, 0.82, r)
    lum = np.clip(0.55 + 0.45 * np.exp(-(r / 0.5) ** 1.5) + 0.12 * (fur - 0.5), 0, 1)
    finish_grey(lum, alpha, "burst")


def ring():
    n = 256
    xx, yy = grid(n, n)
    dx, dy = xx - n / 2, yy - n / 2
    r = np.hypot(dx, dy) / (n / 2)
    ang = np.arctan2(dy, dx)
    teeth = 28
    tt = (ang / (2 * math.pi) * teeth) % 1.0
    tri = np.abs(tt - 0.5) * 2
    tooth_len = np.where((np.floor(ang / (2 * math.pi) * teeth) % 2) == 0, 0.20, 0.11)
    band = np.exp(-((r - 0.74) / 0.030) ** 2)
    inner = np.exp(-((r - 0.66) / 0.012) ** 2) * 0.6
    outer_t = np.clip(1 - tri * 1.25, 0, 1) * smooth(0.77, 0.80, r) * smooth(0.77 + tooth_len, 0.77 + tooth_len * 0.55, r)
    nz = value_noise(n, n, 4, 4, 5)
    a = np.clip(band * (0.85 + 0.3 * nz) + inner + outer_t, 0, 1) * smooth(1.0, 0.94, r)
    glow = np.exp(-((r - 0.72) / 0.12) ** 2) * 0.25
    alpha = np.clip(a + glow, 0, 1)
    lum = np.clip(0.55 + 0.45 * np.maximum(band, outer_t) + 0.1 * (nz - 0.5), 0, 1)
    finish_grey(lum, alpha, "ring")


def fangs():
    cw, ch = 64, 64
    lum, al = np.zeros((ch, cw * 4), np.float32), np.zeros((ch, cw * 4), np.float32)
    rng = np.random.default_rng(55)
    for i in range(4):
        # a curved fang: a tapering crescent from a thick root (bottom) to a sharp point (top-right)
        curve = 0.30 + 0.12 * i
        pts_l, pts_r = [], []
        for k in range(0, 33):
            t = k / 32
            x = 18 + (cw - 36) * (t ** 1.4) * (0.6 + 0.4 * curve) + 14 * math.sin(t * 2.4) * curve
            y = 58 - 52 * t
            wd = 17 * (1 - t) ** 0.8 * (0.7 + 0.3 * (1 - i / 4.0)) + 0.3
            ox, oy = 0.8, 0.6
            pts_l.append((x - wd * ox, y + wd * oy))
            pts_r.append((x + wd * ox, y - wd * oy))
        m = polygon_mask(cw, ch, [pts_l + pts_r[::-1]], 4)
        sh = blur(m, 2.2)
        l = 0.5 + 0.5 * smooth(0.2, 0.9, sh) + 0.1 * (value_noise(cw, ch, 3, 3, 60 + i) - 0.5)
        lum[:, i * cw:(i + 1) * cw] = l
        al[:, i * cw:(i + 1) * cw] = m
    finish_grey(lum, al, "fangs")


def haze():
    n = 256
    xx, yy = grid(n, n)
    r = np.hypot(xx - n / 2, yy - n / 2) / (n / 2)
    warp = value_noise(n, n, 40, 40, 1)
    big = fbm(n, n, 48, 2, 4)
    streak = value_noise(n, n, 3, 34, 3)
    d = r + (warp - 0.5) * 0.35
    env = smooth(1.0, 0.15, d) ** 1.4
    a = np.clip(env * (0.35 + 1.1 * big) * (0.55 + 0.8 * streak), 0, 1)
    finish_grey(0.6 + 0.4 * big, a * 0.9, "haze")


def glint():
    n = 64
    xx, yy = grid(n, n)
    dx, dy = (xx - n / 2 + 0.5) / (n / 2), (yy - n / 2 + 0.5) / (n / 2)
    r = np.hypot(dx, dy)
    # four curved points: |dx|^p + |dy|^p form (an astroid-like star), the points sweep slightly
    ang = np.arctan2(dy, dx)
    spike = np.clip(1 - (np.abs(np.sin(2 * ang)) ** 0.55) * 1.0, 0, 1)
    a = (np.exp(-(r / 0.10) ** 1.3) * 1.0 + (spike ** 2.2) * np.exp(-(r / 0.62) ** 1.3) * 0.9)
    finish_grey(0.8 + 0.2 * a, np.clip(a, 0, 1) * smooth(1.0, 0.85, r), "glint")


# ================================================================================================ main
ALL = {"flame": flame, "stream": stream, "licks": licks, "spirit": spirit, "claw": claw, "paw": paw, "sigil": sigil,
       "burst": burst, "ring": ring, "fangs": fangs, "haze": haze, "glint": glint}

if __name__ == "__main__":
    want = sys.argv[1:] or list(ALL)
    for nme in want:
        ALL[nme]()

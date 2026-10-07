"""Generates the Demon Light Magic VFX textures (src/main/resources/assets/nusmp/textures/particle/demon_light_*.png).

    python3 -B tools/gen_demon_light_textures.py

Look reference (owner's still of the Sword of Judgment): black, ragged, torn-glass shards radiating from one point, each edged by a
thin bright white-grey rim with a soft pale halo; small black four-pointed spiked crosses with ladder bars and a white outline;
short bundles of parallel speed hatch lines. "dark" sprites carry their colour (black body, pale rim) and are drawn with ALPHA;
the "_rim" / light sprites are white with alpha and get tinted (magenta / white) with ADD.
Deterministic seeds, only numpy + Pillow.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 3


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, dtype=np.float32) / 255.0


def mask_from(draw_fn, w, h):
    im = Image.new("L", (w * SS, h * SS), 0)
    draw_fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    return np.asarray(im, dtype=np.float32) / 255.0


def fbm(rng, w, h, octaves=4):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = 4 * 2 ** o
        g = rng.random((n, n)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8), "L").resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(im, np.float32) / 255.0
        tot += amp
        amp *= 0.5
    return out / tot


def save(name, rgba):
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray((np.clip(rgba, 0, 1) * 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, "demon_light_" + name + ".png"))


def finish_dark(mask, rng, glow=1.0, crack=None, rimw=None):
    """Black body, bright pale rim just inside the edge, soft grey halo outside (the still's linework)."""
    h, w = mask.shape
    s = w / 256.0
    inner = blur(mask, 6.0 * s * (rimw or 1.0))
    band = mask * np.clip((0.82 - inner) / 0.82, 0, 1) ** 0.9
    halo = blur(mask, 9 * s) * (1 - mask)
    mott = fbm(rng, w, h, 4)
    body = 0.015 + 0.10 * np.clip(mott - 0.35, 0, 1) ** 1.5
    if crack is not None:
        body = body + 0.75 * crack
    rim = np.array([0.94, 0.92, 1.0], np.float32)
    k = np.clip(band * 1.15 + (1 - mask) * 1.0, 0, 1)[..., None]
    rgb = body[..., None] * (1 - k) + rim * k * np.clip(0.55 + 0.45 * mott, 0, 1)[..., None] ** 0.6 * 1.05
    a = np.clip(np.maximum(mask, halo * 0.75 * glow), 0, 1)
    return np.dstack([np.clip(rgb, 0, 1), a])


def finish_rim(mask, glow=1.0):
    h, w = mask.shape
    s = w / 256.0
    inner = blur(mask, 4.5 * s)
    band = mask * np.clip((0.82 - inner) / 0.82, 0, 1)
    halo = blur(mask, 7 * s) * (1 - mask)
    a = np.clip(band * 1.1 + halo * 0.9 * glow + mask * 0.06, 0, 1)
    return np.dstack([np.ones_like(a), np.ones_like(a), np.ones_like(a), a])


# ------------------------------------------------------------------------------------------------ shard polygons
def blade_poly(rng, cx, base_y, length, base_w, bend=0.0, teeth=16, rag=0.55, ang=0.0, ox=0.0, oy=0.0):
    """Ragged torn blade, base at (cx, base_y) pointing up (negative y); rotated by ang around the base."""
    def side(sign):
        ss = np.sort(rng.random(teeth)) * 0.96
        pts = []
        for s in ss:
            w = base_w * (1 - s) ** 1.15 * (1 - rag * rng.random() * (0.4 + s))
            if rng.random() < 0.28:
                w *= 0.35                       # notch
            pts.append((s, sign * w))
        return pts
    pts = [(0.0, -base_w * 0.5 * rng.uniform(0.7, 1.0))] + side(-1) + [(1.0, 0.0)] + side(1)[::-1] + [(0.0, base_w * 0.5 * rng.uniform(0.7, 1.0))]
    ca, sa = math.cos(ang), math.sin(ang)
    out = []
    for s, w in pts:
        x = w + bend * math.sin(s * math.pi * 0.85) * length * 0.25
        y = -s * length
        out.append((cx + ox + x * ca - y * sa, base_y + oy + x * sa + y * ca))
    return out


def shard_sprite(seed, w=128, h=256, dark=True):
    rng = np.random.default_rng(seed)
    poly = blade_poly(rng, w / 2, h * 0.985, h * 0.97, w * 0.30, bend=rng.uniform(-0.5, 0.5), teeth=18)
    m = mask_from(lambda d, s: d.polygon([(x * s, y * s) for x, y in poly], fill=255), w, h)
    return finish_dark(m, rng) if dark else finish_rim(m)


def burst_sprite(seed, dark=True, size=256, n=17):
    rng = np.random.default_rng(seed)
    c = size / 2
    polys = []
    for i in range(n):
        a = math.tau * i / n + rng.uniform(-0.16, 0.16)
        ln = size * 0.49 * rng.uniform(0.6, 1.0)
        bw = size * rng.uniform(0.028, 0.06)
        # blade_poly points "up" (-y); rotating by (a + pi/2) makes it point along angle a
        polys.append(blade_poly(rng, c, c, ln, bw, bend=rng.uniform(-0.6, 0.6), teeth=11, rag=0.6, ang=a + math.pi / 2))
    m = mask_from(lambda d, s: [d.polygon([(x * s, y * s) for x, y in p], fill=255) for p in polys] +
                  [d.ellipse([(c - size * 0.06) * s, (c - size * 0.06) * s, (c + size * 0.06) * s, (c + size * 0.06) * s], fill=255)], size, size)
    return finish_dark(m, rng) if dark else finish_rim(m)


# ------------------------------------------------------------------------------------------------ cross sprite
def cross_sprite(dark=True, w=128, h=128):
    rng = np.random.default_rng(7)
    cx, cy = w / 2, h / 2
    L, S, k = h * 0.47, w * 0.20, w * 0.05

    def fn(d, s):
        star = [(0, -L), (k, -k), (S, 0), (k, k), (0, L), (-k, k), (-S, 0), (-k, -k)]
        d.polygon([((cx + x) * s, (cy + y) * s) for x, y in star], fill=255)
        for fy, fl in ((-0.34, 0.20), (0.0, 0.32), (0.34, 0.20)):       # ladder bars
            y = cy + fy * L
            d.polygon([((cx - w * fl) * s, y * s), ((cx - w * fl * 0.2) * s, (y - 2.6) * s), ((cx + w * fl * 0.2) * s, (y - 2.6) * s),
                       ((cx + w * fl) * s, y * s), ((cx + w * fl * 0.2) * s, (y + 2.6) * s), ((cx - w * fl * 0.2) * s, (y + 2.6) * s)], fill=255)
    m = mask_from(fn, w, h)
    return finish_dark(m, rng, rimw=0.55) if dark else finish_rim(m)


# ------------------------------------------------------------------------------------------------ hatch, streaks, light
def hatch_sprite(w=128, h=64):
    rng = np.random.default_rng(11)
    a = np.zeros((h, w), np.float32)
    for row in range(5, h - 4, 5):
        ln = rng.uniform(26, 118)
        x0 = rng.uniform(0, w - ln)
        xs = np.arange(w)
        prof = np.clip(1 - np.abs((xs - (x0 + ln / 2)) / (ln / 2)), 0, 1) ** 0.6
        a[row:row + 2, :] = np.maximum(a[row:row + 2, :], prof[None, :] * rng.uniform(0.6, 1.0))
    a = np.clip(a * 1.3 - blur(a, 0.2) * 0.0, 0, 1)
    a = np.maximum(a, blur(a, 1.4) * 0.6)
    return np.dstack([np.ones_like(a)] * 3 + [a])


def beam_sprite(dark=True, w=64, h=256):
    rng = np.random.default_rng(23)
    ys = np.arange(h)
    left = w * (0.30 + 0.07 * np.sin(ys / 9.0) + 0.06 * (rng.random(h) - 0.5))
    right = w * (0.70 - 0.07 * np.sin(ys / 11.0 + 1) + 0.06 * (rng.random(h) - 0.5))
    xs = np.arange(w)[None, :]
    m = ((xs >= left[:, None]) & (xs <= right[:, None])).astype(np.float32)
    m = blur(m, 0.8)
    return finish_dark(m, rng, rimw=0.8) if dark else finish_rim(m)


def streak_sprite(w=64, h=256):
    rng = np.random.default_rng(31)
    xs = np.linspace(-1, 1, w)[None, :]
    ys = np.linspace(0, 1, h)[:, None]
    n = fbm(rng, w, h, 3)
    core = np.exp(-(xs ** 2) / 0.012)
    body = np.exp(-(xs ** 2) / 0.20) * (0.35 + 0.65 * n)
    thr = np.clip(np.sin(xs * 38 + n * 9) * 0.5 + 0.5, 0, 1) * np.exp(-(xs ** 2) / 0.35) * 0.35
    a = np.clip(core * 0.95 + body * 0.55 + thr, 0, 1) * (0.25 + 0.75 * np.sin(ys * math.pi) ** 0.5)
    return np.dstack([np.ones_like(a)] * 3 + [a])


def flare_sprite(size=256):
    rng = np.random.default_rng(41)
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2) / (size / 2), (yy - size / 2) / (size / 2)
    r = np.sqrt(x * x + y * y) + 1e-4
    core = np.exp(-r * r / 0.012)
    vert = np.exp(-(x * x) / 0.0009) * np.exp(-np.abs(y) * 2.4)
    horz = np.exp(-(y * y) / 0.0009) * np.exp(-np.abs(x) * 3.4) * 0.8
    dg = (np.exp(-((x + y) ** 2) / 0.0014) + np.exp(-((x - y) ** 2) / 0.0014)) * np.exp(-r * 4.2) * 0.45
    halo = np.exp(-r * r / 0.07) * 0.5
    a = np.clip(core + vert + horz + dg + halo, 0, 1) * np.clip(1 - r ** 3, 0, 1)
    return np.dstack([np.ones_like(a)] * 3 + [a])


def ring_sprite(size=256):
    rng = np.random.default_rng(53)
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2) / (size / 2), (yy - size / 2) / (size / 2)
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    n = fbm(rng, size, size, 4)
    brk = 0.55 + 0.45 * np.sin(ang * 7 + n * 6) ** 2
    thin = np.exp(-((r - 0.86) ** 2) / 0.00035) * brk
    soft = np.exp(-((r - 0.84) ** 2) / 0.006) * 0.4
    outer = np.exp(-((r - 0.93) ** 2) / 0.0005) * 0.5 * (0.4 + n)
    a = np.clip(thin + soft + outer, 0, 1) * np.clip((1 - r) * 12, 0, 1)
    return np.dstack([np.ones_like(a)] * 3 + [a])


def mote_sprite(size=64):
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2) / (size / 2), (yy - size / 2) / (size / 2)
    d = np.abs(x) * 2.2 + np.abs(y) * 1.1
    a = np.clip(1 - d, 0, 1) ** 1.3
    a = np.maximum(a, np.exp(-r2(x, y) / 0.02))
    return np.dstack([np.ones_like(a)] * 3 + [np.clip(a, 0, 1)])


def r2(x, y):
    return x * x + y * y


# ------------------------------------------------------------------------------------------------ sigils and pool
def sigil_sprite(inner=False, size=256):
    rng = np.random.default_rng(61 if not inner else 67)
    c = size / 2

    def fn(d, s):
        def circ(r, wd):
            d.ellipse([(c - r) * s, (c - r) * s, (c + r) * s, (c + r) * s], outline=255, width=max(1, int(wd * s)))
        R = size * 0.5
        if not inner:
            circ(R * 0.96, 1.6)
            circ(R * 0.90, 3.2)
            circ(R * 0.78, 1.4)
            for i in range(60):                               # tick band
                a = math.tau * i / 60
                r0, r1 = R * 0.80, R * (0.88 if i % 5 else 0.92)
                d.line([((c + math.cos(a) * r0) * s, (c + math.sin(a) * r0) * s), ((c + math.cos(a) * r1) * s, (c + math.sin(a) * r1) * s)], fill=255, width=int(1.2 * s))
            for q in range(4):                                # big spiked crosses at the cardinal points
                a = math.pi / 2 * q
                ux, uy, px, py = math.cos(a), math.sin(a), -math.sin(a), math.cos(a)
                pts = [(R * 0.99, 0), (R * 0.86, 5), (R * 0.70, 0), (R * 0.86, -5)]
                d.polygon([((c + ux * x + px * y) * s, (c + uy * x + py * y) * s) for x, y in pts], fill=255)
                for bx, bl in ((0.92, 9), (0.86, 12), (0.80, 9)):
                    p0 = (c + ux * R * bx + px * bl, c + uy * R * bx + py * bl)
                    p1 = (c + ux * R * bx - px * bl, c + uy * R * bx - py * bl)
                    d.line([(p0[0] * s, p0[1] * s), (p1[0] * s, p1[1] * s)], fill=255, width=int(1.6 * s))
            for k in range(2):                                # inner octagram
                rr = R * 0.72
                pts = [(c + math.cos(math.tau * i / 8 * 3 + k * 0.39) * rr, c + math.sin(math.tau * i / 8 * 3 + k * 0.39) * rr) for i in range(8)]
                d.polygon([(x * s, y * s) for x, y in pts], outline=255)
            circ(R * 0.42, 1.6)
            circ(R * 0.36, 2.6)
        else:
            circ(R * 0.95, 2.4)
            circ(R * 0.62, 1.6)
            for i in range(24):                               # thorn ring: triangular teeth pointing out
                a0, a1 = math.tau * (i - 0.32) / 24, math.tau * (i + 0.32) / 24
                am = math.tau * i / 24
                rr = R * (0.95 if i % 2 else 0.80)
                d.polygon([((c + math.cos(a0) * R * 0.62) * s, (c + math.sin(a0) * R * 0.62) * s),
                           ((c + math.cos(a1) * R * 0.62) * s, (c + math.sin(a1) * R * 0.62) * s),
                           ((c + math.cos(am) * rr) * s, (c + math.sin(am) * rr) * s)], fill=255)
            for i in range(6):
                a = math.tau * i / 6
                d.line([((c + math.cos(a) * R * 0.1) * s, (c + math.sin(a) * R * 0.1) * s), ((c + math.cos(a) * R * 0.5) * s, (c + math.sin(a) * R * 0.5) * s)], fill=255, width=int(2 * s))
            circ(R * 0.22, 2.0)
    m = mask_from(fn, size, size)
    a = np.clip(np.maximum(m, blur(m, 2.2) * 0.9) + blur(m, 7) * 0.35, 0, 1)
    a *= 0.75 + 0.25 * fbm(rng, size, size, 3)
    return np.dstack([np.ones_like(a)] * 3 + [np.clip(a, 0, 1)])


def pool_sprite(dark=True, size=256):
    rng = np.random.default_rng(71)
    c = size / 2
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - c) / c, (yy - c) / c
    r = np.sqrt(x * x + y * y)
    ang = np.arctan2(y, x)
    edge = 0.80 + 0.10 * np.sin(ang * 9 + 1.3) * np.sin(ang * 3.0) + 0.07 * (fbm(rng, size, size, 4) - 0.5) * 2
    spikes = np.clip(np.sin(ang * 14 + 0.5), 0, 1) ** 6 * 0.12
    m = np.clip((edge + spikes - r) * 40, 0, 1)
    # cracks: random walks from the rim toward the middle
    cr = Image.new("L", (size * SS, size * SS), 0)
    d = ImageDraw.Draw(cr)
    for i in range(16):
        a = math.tau * i / 16 + rng.uniform(-0.15, 0.15)
        px, py = c + math.cos(a) * c * 0.78, c + math.sin(a) * c * 0.78
        steps = rng.integers(8, 16)
        for st in range(steps):
            a += rng.uniform(-0.35, 0.35)
            step = c * rng.uniform(0.03, 0.07)
            nx, ny = px - math.cos(a) * step, py - math.sin(a) * step
            d.line([(px * SS, py * SS), (nx * SS, ny * SS)], fill=255, width=int(max(1, 1.8 * SS * (1 - st / steps))))
            px, py = nx, ny
    crack = np.asarray(cr.resize((size, size), Image.LANCZOS), np.float32) / 255.0 * m
    return finish_dark(m, rng, glow=0.6, crack=crack * 0.6) if dark else finish_rim(m, glow=0.8)


def main():
    save("shard_a", shard_sprite(101))
    save("shard_b", shard_sprite(202))
    save("shard_a_rim", shard_sprite(101, dark=False))
    save("shard_b_rim", shard_sprite(202, dark=False))
    save("burst", burst_sprite(303))
    save("burst_rim", burst_sprite(303, dark=False))
    save("cross", cross_sprite())
    save("cross_rim", cross_sprite(dark=False))
    save("hatch", hatch_sprite())
    save("beam", beam_sprite())
    save("beam_rim", beam_sprite(dark=False))
    save("streak", streak_sprite())
    save("flare", flare_sprite())
    save("ring", ring_sprite())
    save("mote", mote_sprite())
    save("sigil", sigil_sprite())
    save("sigil_inner", sigil_sprite(inner=True))
    save("pool", pool_sprite())
    save("pool_rim", pool_sprite(dark=False))


if __name__ == "__main__":
    main()

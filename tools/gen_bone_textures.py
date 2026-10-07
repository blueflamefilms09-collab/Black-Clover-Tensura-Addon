"""Generates the Bone Magic VFX textures (bone_*.png) for BoneLayer.

Deterministic (fixed seeds), Pillow + numpy only. Output: src/main/resources/assets/nusmp/textures/particle/bone_*.png

    python3 -B tools/gen_bone_textures.py

Look reference (owner still): dozens of huge warm-white skeletal arms radiating from the caster like a sunburst; every bone is long,
segmented, with ring-shaped joints and a long curved claw at the end; cel-shaded warm white with a light grey outline.
Everything is grey / white with alpha so the vertex colour tints it (warm bone white #f4f1e6, grey #9a968a, pale violet #c8b8ff).
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
OUTLINE = 0.50


def save(name, rgba):
    os.makedirs(OUT, exist_ok=True)
    Image.fromarray(np.clip(rgba, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(OUT, name + ".png"))
    print("wrote", name, rgba.shape[1], "x", rgba.shape[0])


def noise(h, w, cells, rng, octaves=4):
    out = np.zeros((h, w), np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        c = cells * (2 ** o)
        g = rng.random((c + 1, c + 1)).astype(np.float32)
        im = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
        out += amp * np.asarray(im, np.float32) / 255
        tot += amp
        amp *= 0.5
    return out / tot


def blur(a, r):
    im = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8))
    return np.asarray(im.filter(ImageFilter.GaussianBlur(r)), np.float32) / 255


def erode(mask, k):
    im = Image.fromarray((mask > 0.5).astype(np.uint8) * 255)
    return np.asarray(im.filter(ImageFilter.MinFilter(2 * k + 1)), np.float32) / 255


def down(a, w, h):
    return np.asarray(Image.fromarray(a).resize((w, h), Image.LANCZOS), np.float32)


def finish(mask, rng, ss, outline_px=1.6, cel=True, light=(-0.55, -0.8), pores=True):
    """Cel-shaded bone from a supersampled coverage mask (float 0..1). Returns (grey, alpha) supersampled."""
    h, w = mask.shape
    m = (mask > 0.5).astype(np.float32)
    height = blur(m, 0.055 * min(h, w) if min(h, w) < 900 else 0.05 * min(h, w))
    gy, gx = np.gradient(height)
    mag = np.maximum(np.sqrt(gx * gx + gy * gy).max(), 1e-6)
    lit = (-(gx * light[0] + gy * light[1])) / mag
    if cel:
        grey = np.where(lit > 0.30, 1.0, np.where(lit > -0.22, 0.92, np.where(lit > -0.55, 0.80, 0.70)))
        grey = blur(grey.astype(np.float32), 0.8 * ss)
    else:
        grey = 0.85 + 0.15 * lit
    n = noise(h, w, 24, rng, 4)
    grey = grey * (0.965 + 0.07 * n)
    if pores:
        p = noise(h, w, 80, rng, 2)
        grey = np.where(p > 0.78, grey * 0.88, grey)
    edge = m - erode(m, int(outline_px * ss))
    grey = np.where(edge > 0.5, OUTLINE, grey)
    grey = np.where(m > 0.5, grey, OUTLINE)
    return grey, mask


def pack(grey, alpha, w, h):
    g = down(grey.astype(np.float32), w, h)
    a = down(alpha.astype(np.float32), w, h)
    out = np.zeros((h, w, 4), np.float32)
    out[..., 0] = out[..., 1] = out[..., 2] = g * 255
    out[..., 3] = a * 255
    return out


# ------------------------------------------------------------------------------------------------ long bones (arm, limb)

def segment_hw(s, a, b, wmid):
    """Half width of one long-bone segment [a,b]: slim shaft, flared ends (epiphyses)."""
    u = np.clip((s - a) / (b - a), 0, 1)
    return wmid * (1.0 + 0.42 * (1 - np.sin(np.pi * u)) ** 3)


def long_bone(name, W, H, claw, seed, ss=3):
    rng = np.random.default_rng(seed)
    w, h = W * ss, H * ss
    ys, xs = np.mgrid[0:h, 0:W * ss]
    s = 1.0 - (ys + 0.5) / h                      # 0 = base (bottom of the image), 1 = tip
    x = (xs + 0.5) / w                            # 0..1 across
    if claw:
        segs = [(0.0, 0.34, 0.150), (0.34, 0.60, 0.118), (0.60, 0.72, 0.082), (0.72, 0.81, 0.064), (0.81, 0.885, 0.050)]
        joints = [0.34, 0.60, 0.72, 0.81, 0.885]
        c0 = 0.885
    else:
        segs = [(0.0, 0.46, 0.150), (0.46, 0.93, 0.128)]
        joints = [0.46, 0.93]
        c0 = 2.0
    hw = np.zeros_like(s)
    for a, b, wm in segs:
        sel = (s >= a) & (s < b)
        hw = np.where(sel, segment_hw(s, a, b, wm), hw)
    ax = np.full_like(s, 0.5)
    if claw:
        u = np.clip((s - c0) / (1 - c0), 0, 1)
        sel = s >= c0
        chw = 0.052 * (1 - u) ** 0.85 + 0.002
        hw = np.where(sel, chw, hw)
        ax = np.where(sel, 0.5 + 0.20 * u ** 2.0 * 1.0, ax)
    else:
        # round knob at the top: the knee / wrist joint
        u = np.clip((s - 0.93) / 0.07, 0, 1)
        knob = 0.150 * np.sqrt(np.clip(1 - u ** 2, 0, 1))
        hw = np.where(s >= 0.93, np.maximum(knob, 0.0), hw)
    # rounded base cap
    ub = np.clip(s / 0.035, 0, 1)
    hw = np.where(s < 0.035, hw * np.sqrt(np.clip(1 - (1 - ub) ** 2, 0, 1)), hw)
    for j in joints:                                # ring-shaped joints bulge a little
        hw = hw + 0.020 * np.exp(-((s - j) / 0.010) ** 2) * (s < c0 + 0.001 if claw else 1)
    d = np.abs(x - ax)
    mask = ((d < hw) & (hw > 0.002)).astype(np.float32)
    grey, alpha = finish(mask, rng, ss)
    # cel bands across the shaft: lighter left, darker right
    rel = np.where(hw > 0, (x - ax) / np.maximum(hw, 1e-4), 0)
    band = np.where(rel > 0.62, 0.84, np.where(rel > 0.30, 0.93, np.where(rel < -0.55, 1.0, 0.98)))
    interior = (mask > 0.5) & (grey > OUTLINE + 0.02)
    grey = np.where(interior, np.minimum(grey, 1.0) * band * 1.02, grey)
    # joint rings: a grey line across the bone plus a thin ring a little further on
    for j in joints:
        for off, wd, gv in ((0.0, 0.0035, 0.52), (0.014, 0.0022, 0.66), (-0.014, 0.0022, 0.66)):
            line = (np.abs(s - j - off) < wd) & (mask > 0.5)
            grey = np.where(line, gv, grey)
    # cracks (fine hair lines along the shaft)
    cr = noise(h, w, 10, rng, 5)
    crack = (np.abs(cr - 0.5) < 0.006) & (mask > 0.5) & (grey > 0.7) & (s < 0.7)
    grey = np.where(crack, grey * 0.78, grey)
    save(name, pack(grey, alpha, W, H))


# ------------------------------------------------------------------------------------------------ polygon bones

def poly_mask(size, ss, draw_fn):
    im = Image.new("L", (size * ss, size * ss), 0)
    draw_fn(ImageDraw.Draw(im), size * ss)
    return np.asarray(im, np.float32) / 255


def spike(name, W, H, seed, ss=3):
    rng = np.random.default_rng(seed)
    w, h = W * ss, H * ss
    ys, xs = np.mgrid[0:h, 0:w]
    s = 1.0 - (ys + 0.5) / h
    x = (xs + 0.5) / w
    wob = 0.006 * np.sin(s * 23) + 0.004 * np.sin(s * 57 + 1.3)
    hw = 0.40 * (1 - s) ** 1.15 + 0.004
    hw = hw * (1 + 0.5 * np.exp(-(s / 0.05) ** 2))   # flared root
    # jagged teeth along both flanks
    teeth = 0.0 * s
    ax = 0.5 + 0.06 * s ** 2 + wob
    d = x - ax
    mask = ((d > -(hw + teeth)) & (d < hw) & (s < 0.99)).astype(np.float32)
    grey, alpha = finish(mask, rng, ss)
    ridge = (np.abs(d) < 0.006) & (mask > 0.5) & (s > 0.04)
    grey = np.where(ridge, grey * 0.8, grey)
    for sy in (0.22, 0.47):
        grey = np.where((np.abs(s - sy - 0.04 * np.abs(d) * 4) < 0.003) & (mask > 0.5), np.minimum(grey, 0.68), grey)
    save(name, pack(grey, alpha, W, H))


def claw(name, size, seed, ss=4):
    rng = np.random.default_rng(seed)
    S = size * ss
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    n = 220
    for i in range(n):
        t = i / (n - 1)
        th = math.radians(200 + 150 * t)             # sweep of a crescent
        cx, cy = S * 0.5 + S * 0.36 * math.cos(th), S * 0.56 + S * 0.36 * math.sin(th)
        r = S * 0.085 * (1 - t) ** 0.8 * (1 + 0.5 * math.exp(-(t / 0.12) ** 2)) + 0.5
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    mask = np.asarray(im, np.float32) / 255
    grey, alpha = finish(mask, rng, ss)
    save(name, pack(grey, alpha, size, size))


def shard(name, size, seed, ss=4):
    rng = np.random.default_rng(seed)
    S = size * ss
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    pts = [(0.50, 0.03), (0.60, 0.20), (0.66, 0.34), (0.70, 0.48), (0.62, 0.62), (0.68, 0.74), (0.58, 0.86), (0.46, 0.97),
           (0.37, 0.83), (0.42, 0.70), (0.31, 0.58), (0.36, 0.44), (0.30, 0.30), (0.40, 0.18)]
    pts = [(x * S + rng.normal(0, S * 0.008), y * S + rng.normal(0, S * 0.008)) for x, y in pts]
    d.polygon(pts, fill=255)
    mask = np.asarray(im, np.float32) / 255
    grey, alpha = finish(mask, rng, ss)
    # porous marrow end (bottom)
    ys, xs = np.mgrid[0:S, 0:S]
    p = noise(S, S, 36, rng, 3)
    marrow = (ys > S * 0.80) & (mask > 0.5) & (p > 0.55)
    grey = np.where(marrow, 0.60, grey)
    # a fracture line down the face
    ln = (np.abs(xs - (S * 0.5 + S * 0.06 * np.sin(ys / S * 9))) < 0.7 * ss) & (mask > 0.5) & (ys > S * 0.12) & (ys < S * 0.7)
    grey = np.where(ln, 0.6, grey)
    save(name, pack(grey, alpha, size, size))


def skull(name, size, seed, ss=4):
    rng = np.random.default_rng(seed)
    S = size * ss

    def fn(d, S):
        d.ellipse([S * 0.18, S * 0.08, S * 0.82, S * 0.68], fill=255)
        d.rounded_rectangle([S * 0.30, S * 0.50, S * 0.70, S * 0.86], radius=S * 0.06, fill=255)
    mask = poly_mask(size, ss, fn)
    im = Image.fromarray((mask * 255).astype(np.uint8))
    d = ImageDraw.Draw(im)
    for cx in (0.37, 0.63):                              # eye sockets
        d.ellipse([S * (cx - 0.10), S * 0.36, S * (cx + 0.10), S * 0.56], fill=0)
    d.polygon([(S * 0.50, S * 0.56), (S * 0.45, S * 0.67), (S * 0.55, S * 0.67)], fill=0)   # nose
    for i in range(5):                                   # teeth gaps
        x = S * (0.34 + 0.08 * i)
        d.rectangle([x - S * 0.004, S * 0.76, x + S * 0.004, S * 0.86], fill=0)
    d.rectangle([S * 0.30, S * 0.745, S * 0.70, S * 0.755], fill=0)
    mask = np.asarray(im, np.float32) / 255
    grey, alpha = finish(mask, rng, ss)
    save(name, pack(grey, alpha, size, size))


def vertebra_ring(name, size, seed, ss=3):
    rng = np.random.default_rng(seed)
    S = size * ss
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2
    n = 18
    for ringr, rr, spikes in ((0.84, 0.060, True),):
        for i in range(n):
            th = 2 * math.pi * i / n
            px, py = c + c * ringr * math.cos(th), c + c * ringr * math.sin(th)
            rb = S * rr
            d.ellipse([px - rb, py - rb, px + rb, py + rb], fill=255)
            # transverse wings (tangential) and the spinous process (outward)
            tx, ty = -math.sin(th), math.cos(th)
            ox, oy = math.cos(th), math.sin(th)
            d.polygon([(px + tx * rb * 1.9, py + ty * rb * 1.9), (px + ox * rb * 0.6, py + oy * rb * 0.6),
                       (px - tx * rb * 1.9, py - ty * rb * 1.9), (px - ox * rb * 0.6, py - oy * rb * 0.6)], fill=255)
            d.polygon([(px + ox * rb * 0.4 + tx * rb * 0.35, py + oy * rb * 0.4 + ty * rb * 0.35),
                       (px + ox * rb * 2.9, py + oy * rb * 2.9),
                       (px + ox * rb * 0.4 - tx * rb * 0.35, py + oy * rb * 0.4 - ty * rb * 0.35)], fill=255)
    d.ellipse([c - c * 0.84 - 1, c - c * 0.84 - 1, c + c * 0.84 + 1, c + c * 0.84 + 1], outline=255, width=int(S * 0.012))
    mask = np.asarray(im, np.float32) / 255
    # a thin inner ring
    im2 = Image.fromarray((mask * 255).astype(np.uint8))
    d2 = ImageDraw.Draw(im2)
    d2.ellipse([c - c * 0.66, c - c * 0.66, c + c * 0.66, c + c * 0.66], outline=255, width=int(S * 0.008))
    mask = np.asarray(im2, np.float32) / 255
    grey, alpha = finish(mask, rng, ss, outline_px=1.4)
    save(name, pack(grey, alpha, size, size))


# ------------------------------------------------------------------------------------------------ light / line sprites

def sigil(name, size, seed, ss=4):
    S = size * ss
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2
    lw = max(2, int(S * 0.004))

    def circle(r, w):
        d.ellipse([c - c * r, c - c * r, c + c * r, c + c * r], outline=255, width=w)
    circle(0.97, lw * 2)
    circle(0.90, lw)
    circle(0.62, lw * 2)
    circle(0.56, lw)
    circle(0.20, lw)
    # rib pairs: curved bones between the rings
    for i in range(16):
        th = 2 * math.pi * i / 16
        for sgn in (-1, 1):
            pts = []
            for k in range(25):
                t = k / 24
                r = 0.64 + 0.24 * t
                a = th + sgn * (0.16 * math.sin(t * math.pi) + 0.02)
                pts.append((c + c * r * math.cos(a), c + c * r * math.sin(a)))
            d.line(pts, fill=255, width=lw)
        # vertebra knob on the inner ring
        rb = S * 0.016
        px, py = c + c * 0.59 * math.cos(th), c + c * 0.59 * math.sin(th)
        d.ellipse([px - rb, py - rb, px + rb, py + rb], fill=255)
    # sunburst of claw-arms inside: 8 long curved arms with a ring joint each
    for i in range(8):
        th = 2 * math.pi * i / 8 + 0.2
        pts = []
        for k in range(30):
            t = k / 29
            r = 0.22 + 0.32 * t
            a = th + 0.22 * t * t
            pts.append((c + c * r * math.cos(a), c + c * r * math.sin(a)))
        d.line(pts, fill=255, width=int(lw * 2.2))
        jx, jy = pts[14]
        rj = S * 0.012
        d.ellipse([jx - rj, jy - rj, jx + rj, jy + rj], outline=255, width=lw)
    # eight bone daggers pointing outwards past the outer ring
    for i in range(8):
        th = 2 * math.pi * i / 8 + math.pi / 8
        p0 = (c + c * 0.90 * math.cos(th), c + c * 0.90 * math.sin(th))
        pl = (c + c * 0.84 * math.cos(th - 0.05), c + c * 0.84 * math.sin(th - 0.05))
        pr = (c + c * 0.84 * math.cos(th + 0.05), c + c * 0.84 * math.sin(th + 0.05))
        tip = (c + c * 0.995 * math.cos(th), c + c * 0.995 * math.sin(th))
        d.polygon([pl, tip, pr], fill=255)
    a = np.asarray(im, np.float32) / 255
    a = np.maximum(a, 0.0)
    glow = blur(a, 2.2 * ss)
    alpha = np.clip(a + glow * 0.55, 0, 1)
    grey = np.clip(0.88 + 0.12 * a, 0, 1)
    save(name, pack(grey, alpha, size, size))


def dust(name, size, seed):
    rng = np.random.default_rng(seed)
    ys, xs = np.mgrid[0:size, 0:size]
    r = np.hypot(xs - size / 2 + 0.5, ys - size / 2 + 0.5) / (size / 2)
    n = noise(size, size, 5, rng, 5)
    fine = noise(size, size, 40, rng, 2)
    core = np.clip(1 - r, 0, 1) ** 1.1
    a = np.clip((core * (0.7 + 0.7 * n) + 0.05) * 1.1, 0, 1) * (0.9 + 0.2 * fine)
    a = np.clip(a, 0, 1) * np.clip((1 - r) * 4, 0, 1)
    grain = (fine > 0.72) & (r < 0.8) & (a > 0.1)
    a = np.where(grain, np.minimum(1, a + 0.12), a)
    out = np.zeros((size, size, 4), np.float32)
    out[..., :3] = (0.82 + 0.18 * n)[..., None] * 255
    out[..., 3] = a * 255
    save(name, out)


def flare(name, size):
    ys, xs = np.mgrid[0:size, 0:size]
    x = (xs + 0.5 - size / 2) / (size / 2)
    y = (ys + 0.5 - size / 2) / (size / 2)
    r = np.hypot(x, y) + 1e-4
    core = np.exp(-(r / 0.13) ** 2) + 0.5 * np.exp(-(r / 0.34) ** 2) + 0.18 * np.exp(-(r / 0.7) ** 2)
    ang = np.arctan2(y, x)
    spikes = np.zeros_like(r)
    for k in range(4):
        a0 = k * math.pi / 2 + math.pi / 4
        dd = np.abs(np.sin(ang - a0))
        spikes = np.maximum(spikes, np.exp(-(dd * r / 0.012) ** 2) * np.exp(-r / 0.45) * 0.8)
    for k in range(4):
        a0 = k * math.pi / 2
        dd = np.abs(np.sin(ang - a0))
        spikes = np.maximum(spikes, np.exp(-(dd * r / 0.02) ** 2) * np.exp(-r / 0.8))
    for k in range(8):
        a0 = k * math.pi / 4 + math.pi / 8
        dd = np.abs(np.sin(ang - a0))
        spikes = np.maximum(spikes, np.exp(-(dd * r / 0.008) ** 2) * np.exp(-r / 0.2) * 0.5)
    ring = np.exp(-((r - 0.62) / 0.012) ** 2) * 0.35
    a = np.clip(core + spikes + ring, 0, 1) * np.clip((1 - r) * 3, 0, 1)
    out = np.zeros((size, size, 4), np.float32)
    out[..., :3] = 255
    out[..., 3] = a * 255
    save(name, out)


def shock(name, size, seed, ss=3):
    rng = np.random.default_rng(seed)
    S = size * ss
    im = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(im)
    c = S / 2
    d.ellipse([c - c * 0.78, c - c * 0.78, c + c * 0.78, c + c * 0.78], outline=255, width=int(S * 0.012))
    n = 40
    for i in range(n):
        th = 2 * math.pi * i / n + rng.normal(0, 0.02)
        ln = 0.12 + 0.16 * rng.random() + (0.08 if i % 2 else 0)
        wd = 0.018 + 0.012 * rng.random()
        r0, r1 = 0.76, 0.76 + ln
        tx, ty = -math.sin(th), math.cos(th)
        b = (c + c * r0 * math.cos(th), c + c * r0 * math.sin(th))
        tip = (c + c * r1 * math.cos(th), c + c * r1 * math.sin(th))
        d.polygon([(b[0] + tx * c * wd, b[1] + ty * c * wd), tip, (b[0] - tx * c * wd, b[1] - ty * c * wd)], fill=255)
    a = np.asarray(im, np.float32) / 255
    glow = blur(a, 2.5 * ss)
    ys, xs = np.mgrid[0:S, 0:S]
    r = np.hypot(xs - c, ys - c) / c
    inner = np.clip((0.78 - r) * 2.5, 0, 1) ** 2 * 0.08
    alpha = np.clip(a + glow * 0.7 + inner, 0, 1) * np.clip((1 - r) * 8, 0, 1)
    save(name, pack(np.ones_like(alpha), alpha, size, size))


def trail(name, W, H, seed):
    """Streak: head (top) bright, tail (bottom) fades into chalky dust with small vertebra beads."""
    rng = np.random.default_rng(seed)
    ys, xs = np.mgrid[0:H, 0:W]
    s = 1.0 - (ys + 0.5) / H           # 0 tail, 1 head
    x = (xs + 0.5) / W * 2 - 1
    hw = 0.18 + 0.62 * s ** 0.8
    n = noise(H, W, 6, rng, 5)
    core = np.exp(-(x / (0.35 * hw + 0.02)) ** 2) * (0.35 + 0.65 * s)
    body = np.clip(1 - np.abs(x) / hw, 0, 1) ** 1.3 * (0.25 + 0.75 * s ** 1.4) * (0.55 + 0.9 * n)
    beads = np.zeros_like(s)
    for k in range(7):
        sy = 0.12 + 0.12 * k
        beads = np.maximum(beads, np.exp(-(((s - sy) / 0.016) ** 2 + (x / 0.35) ** 2)) * 0.8 * (0.4 + s))
    a = np.clip(core + body * 0.7 + beads, 0, 1) * np.clip(s * 6, 0, 1) * np.clip((1 - np.abs(x)) * 5, 0, 1)
    out = np.zeros((H, W, 4), np.float32)
    out[..., :3] = 255
    out[..., 3] = a * 255
    save(name, out)


def main():
    long_bone("bone_arm", 128, 1024, True, 11)
    long_bone("bone_limb", 128, 1024, False, 12)
    spike("bone_spike", 128, 256, 13)
    claw("bone_claw", 128, 14)
    shard("bone_shard", 128, 15)
    skull("bone_skull", 128, 16)
    vertebra_ring("bone_ring", 256, 17)
    sigil("bone_sigil", 256, 18)
    dust("bone_dust", 128, 19)
    flare("bone_flare", 128)
    shock("bone_shock", 256, 20)
    trail("bone_trail", 64, 256, 21)


if __name__ == "__main__":
    main()

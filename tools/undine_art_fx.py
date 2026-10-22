"""Painters of the Undine's water pass, additive foam pass and wave base (tools/gen_undine_art.py gathers them). The water pass is alpha blended (the flowing outer gown, foam bands, droplets); the
additive pass is soft light (gradients that fade to alpha 0 at every edge, nothing painted under alpha 0, so no flat white card can show: the SafeAdditive rule); the wave base is the stormy
water the full form rises from, in the same cel look as the body (flat blue, darker blue shade, white caps, hatching)."""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import undine_art_lib as A  # noqa: E402
from undine_art_lib import SS, col, canvas, mask_canvas, done, fill_mask, shifted, minus, torn, smooth_ribbon, wave_pts, hatch  # noqa: E402

TAU = math.tau


def S(v):
    return int(round(v * SS))


# ====================================================================================================================== water pass
def shell(w, h):
    """A strip of the flowing outer gown, seen from outside: clear blue water, hardly there at the waist and thicker toward the hem where it runs past the gown onto the waves, a darker tongue,
    a few soft pale bands, a ragged foamy lower end; the sides fade out so the strips run into each other."""
    rng = np.random.default_rng(1100)
    g = np.linspace(0, 1, h * SS) ** 1.6
    img = canvas(w, h, col(A.GOWN, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w * SS, h * SS], fill=col(A.GOWN, 255))
    x0 = w * 0.55
    pts = wave_pts(x0, h * 0.2, x0 + w * 0.1, h * 0.95, w * 0.1, 1.3, rng.uniform(0, TAU), 40)
    smooth_ribbon(d, [(x * SS, y * SS) for x, y in pts], [w * 0.2 * SS * (0.4 + 0.7 * i / 39.0) for i in range(40)], col(A.GOWN_DARK, 255))
    mask = Image.new("L", img.size, 255)
    md = ImageDraw.Draw(mask)
    md.rectangle([0, S(h * 0.88), w * SS, h * SS], fill=0)
    for i in range(int(w / 10) + 1):
        x = i * 10 + rng.uniform(-3, 3)
        r = rng.uniform(4, 8)
        y = h * 0.88 + rng.uniform(-4, 4)
        md.ellipse([S(x - r), S(y - r), S(x + r), S(y + r * 1.8)], fill=255)
        d.ellipse([S(x - r), S(y - r - 1), S(x + r), S(y + r * 1.8)], fill=col(A.FOAM, 255))
    rough = torn(mask, rng, 3.0, 8.0)
    edge = np.minimum(np.linspace(0, 1, w * SS), np.linspace(1, 0, w * SS)) * 6.0
    al = np.asarray(rough, dtype=np.float32) * (g[:, None] * 0.42) * np.clip(edge, 0, 1)[None, :]
    img.putalpha(Image.fromarray(np.clip(al, 0, 255).astype(np.uint8), "L"))
    return done(img, w, h)


def splash(w, h):
    """A splash of foam: a cluster of white and pale blue blobs and droplets, thicker at the bottom, ragged and transparent toward the top."""
    rng = np.random.default_rng(1110)
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    for i in range(30):
        t = rng.random() ** 0.6
        x, y = w * rng.uniform(0.06, 0.94), h * (0.95 - 0.8 * t)
        r = (h * 0.16 * (1 - 0.6 * t)) * rng.uniform(0.5, 1.2)
        d.ellipse([S(x - r), S(y - r), S(x + r), S(y + r)], fill=col(A.FOAM, 255))
        d.ellipse([S(x - r * 0.8), S(y - r * 0.2), S(x + r * 0.9), S(y + r)], fill=col("#CFE6F7", 255))
        d.ellipse([S(x - r * 0.7), S(y - r * 0.9), S(x + r * 0.5), S(y + r * 0.1)], fill=col(A.FOAM, 255))
    for i in range(10):
        x, y, r = w * rng.uniform(0.05, 0.95), h * rng.uniform(0.05, 0.4), h * rng.uniform(0.03, 0.07)
        d.ellipse([S(x - r), S(y - r), S(x + r), S(y + r)], fill=col(A.FOAM, 255))
    return done(img, w, h)


def foam_band(w, h):
    """The band of foam round an upper arm, seen on any side: a white band with a ragged edge above and below, pale blue shade."""
    rng = np.random.default_rng(1120)
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    d.rectangle([0, S(h * 0.3), w * SS, S(h * 0.7)], fill=col(A.FOAM))
    for i in range(int(w / 7)):
        x = i * 7 + rng.uniform(-2, 2)
        r = rng.uniform(5, 9)
        yt = h * 0.3 - rng.uniform(0, 5)
        yb = h * 0.7 + rng.uniform(0, 5)
        d.ellipse([S(x - r), S(yt - r), S(x + r), S(yt + r * 0.5)], fill=col(A.FOAM))
        d.ellipse([S(x - r), S(yb - r * 0.5), S(x + r), S(yb + r)], fill=col(A.FOAM))
    d.rectangle([0, S(h * 0.56), w * SS, S(h * 0.7)], fill=col("#CFE6F7"))
    for i in range(int(w / 9)):
        x = i * 9 + rng.uniform(-2, 2)
        d.line([(x * SS, h * 0.35 * SS), ((x + 6) * SS, h * 0.5 * SS)], fill=col("#BFD8EE"), width=S(1.6))
    return done(img, w, h)


def foam_tear(w, h):
    """A torn streak of foam that trails off an arm: a pointed white ribbon with blobs."""
    rng = np.random.default_rng(1130)
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    pts = wave_pts(w * 0.04, h * 0.5, w * 0.96, h * 0.52, h * 0.16, 1.5, 0.5, 40)
    smooth_ribbon(d, [(x * SS, y * SS) for x, y in pts], [h * 0.34 * SS * math.sin(math.pi * (0.1 + 0.9 * i / 39.0)) ** 0.7 for i in range(40)], col(A.FOAM))
    for i in range(5):
        x, y, r = w * rng.uniform(0.5, 0.97), h * rng.uniform(0.2, 0.8), h * rng.uniform(0.05, 0.1)
        d.ellipse([S(x - r), S(y - r), S(x + r), S(y + r)], fill=col(A.FOAM))
    return done(img, w, h)


def droplet(w, h):
    """Four droplets and small blobs of water (each a quarter of the region): pale blue with a white glint and a darker underside."""
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    q = w // 4
    for k in range(4):
        cx, cy = q * (k + 0.5), h * 0.52
        r = min(q, h) * (0.30 + 0.05 * (k % 2))
        if k % 2 == 0:
            d.polygon([(cx * SS, (cy - r * 1.5) * SS), ((cx - r * 0.8) * SS, (cy + r * 0.2) * SS), ((cx + r * 0.8) * SS, (cy + r * 0.2) * SS)], fill=col(A.GOWN_MID))
        d.ellipse([S(cx - r), S(cy - r * 0.9), S(cx + r), S(cy + r * 1.1)], fill=col(A.GOWN_MID))
        d.ellipse([S(cx - r * 0.85), S(cy - r * 0.1), S(cx + r * 0.85), S(cy + r * 1.05)], fill=col(A.GOWN))
        d.ellipse([S(cx - r * 0.5), S(cy - r * 0.6), S(cx - r * 0.1), S(cy - r * 0.2)], fill=col(A.FOAM))
    return done(img, w, h)


# ====================================================================================================================== additive pass (soft light: every edge fades to alpha 0)
def _soft(w, h, fn):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u = (xx + 0.5) / w * 2 - 1
    v = (yy + 0.5) / h * 2 - 1
    a = np.clip(fn(u, v), 0, 1)
    out = np.zeros((h, w, 4), dtype=np.float32)
    out[..., 0], out[..., 1], out[..., 2] = 215, 236, 255
    out[..., 3] = a * 255.0
    out[a <= 0.002] = 0
    return out


def streak(w, h):
    """A soft vertical streak of light: a long gaussian ridge with a slow wave, brightest in the middle."""
    return _soft(w, h, lambda u, v: np.exp(-((u + 0.18 * np.sin(v * 2.6)) / 0.22) ** 2) * np.clip(1 - np.abs(v) ** 3, 0, 1) * 0.10)


def glint(w, h):
    """A four-point star glint with a soft core."""
    def f(u, v):
        r = np.sqrt(u * u + v * v)
        core = np.exp(-(r / 0.16) ** 2)
        cross = np.exp(-(np.abs(u) / 0.045) ** 1.5) * np.clip(1 - np.abs(v), 0, 1) ** 2 + np.exp(-(np.abs(v) / 0.045) ** 1.5) * np.clip(1 - np.abs(u), 0, 1) ** 2
        return (core * 0.9 + cross * 0.75) * np.clip(1.3 - r, 0, 1)
    return _soft(w, h, f)


def glow(w, h):
    """A soft round glow."""
    return _soft(w, h, lambda u, v: np.clip(1 - np.sqrt(u * u + v * v), 0, 1) ** 2.2 * 0.8)


def ring(w, h):
    """A soft ripple ring: a thin bright ring at 0.72 of the radius with a faint wider halo, fading to nothing at the centre and the edge."""
    def f(u, v):
        r = np.sqrt(u * u + v * v)
        main = np.exp(-((r - 0.72) / 0.06) ** 2)
        halo = np.exp(-((r - 0.66) / 0.2) ** 2) * 0.28
        return (main * 0.9 + halo) * np.clip((0.98 - r) / 0.12, 0, 1)
    return _soft(w, h, f)


# ====================================================================================================================== wave base
def crest(w, h, seed=1200, tall=1.0):
    """A swell of stormy water seen from the side: a long smooth back rising to a crest that curls over, a white lip and foam in the trough, darker blue shade under the curl, light hatching."""
    from PIL import ImageChops
    rng = np.random.default_rng(seed)
    mask = mask_canvas(w, h)
    md = ImageDraw.Draw(mask)
    peak = w * rng.uniform(0.58, 0.68)
    top = h * 0.10 / tall
    pts = [(0, h * 1.02)]
    for i in range(61):                                                              # the back of the swell
        x = peak * i / 60.0
        k = x / peak
        pts.append((x, h * 0.96 - (h * 0.96 - top) * math.sin(k * math.pi / 2) ** 1.7))
    for i in range(1, 41):                                                           # the front: it drops steeply, with the lip leaning over
        k = i / 40.0
        x = peak + (w - peak) * k - w * 0.07 * math.sin(k * math.pi) * (1 - k)
        y = top + (h * 0.96 - top) * (1 - (1 - k) ** 1.6) + h * 0.02 * math.sin(k * 9)
        pts.append((x, y))
    pts.append((w, h * 1.02))
    md.polygon([(x * SS, y * SS) for x, y in pts], fill=255)
    md.ellipse([S(peak - w * 0.05), S(top - h * 0.04), S(peak + w * 0.14), S(top + h * 0.2)], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(SS * 1.2)).point(lambda v: 255 if v > 120 else 0)
    img = canvas(w, h)
    fill_mask(img, mask, col(A.GOWN))
    dark = minus(mask, shifted(mask, -w * 0.06, -h * 0.30))
    fill_mask(img, dark, col(A.GOWN_DARK))
    deep = minus(mask, shifted(mask, -w * 0.03, -h * 0.6))
    fill_mask(img, ImageChops.multiply(deep, dark), col(A.GOWN_DEEP))
    d = ImageDraw.Draw(img)
    cap = mask_canvas(w, h)
    cd = ImageDraw.Draw(cap)
    for i in range(int(w / 14)):                                                     # the white lip and its blobs
        x = w * rng.uniform(0.46, 0.97)
        k = (x - peak) / (w - peak)
        if x < peak:
            y = h * 0.96 - (h * 0.96 - top) * math.sin(x / peak * math.pi / 2) ** 1.7
        else:
            y = top + (h * 0.96 - top) * (1 - (1 - max(0.0, k)) ** 1.6)
        r = rng.uniform(w * 0.02, w * 0.045)
        cd.ellipse([S(x - r), S(y - r * 0.6), S(x + r), S(y + r * 1.5)], fill=255)
    cap = ImageChops.multiply(cap, mask.filter(ImageFilter.MaxFilter(5)))
    fill_mask(img, cap, col(A.FOAM))
    hatch(d, rng, (w * 0.12, h * 0.4, w * 0.9, h * 0.92), 60, h * 0.1, -62, SS * 1.6, col(A.GOWN_MID, 255))
    for k in range(3):                                                               # foam streaks along the swell
        pp = wave_pts(w * 0.03, h * (0.84 - 0.13 * k), w * 0.97, h * (0.80 - 0.1 * k), h * 0.03, 2.0, k, 40)
        smooth_ribbon(d, [(x * SS, y * SS) for x, y in pp], [h * 0.035 * SS] * 40, col(A.FOAM))
    out = done(img, w, h)
    al = np.asarray(mask.resize((w, h), Image.LANCZOS), dtype=np.float32)
    out[..., 3] = np.minimum(out[..., 3], al)
    return out


def blur_mask(mask):
    return mask.filter(ImageFilter.MaxFilter(3))


def wave_a(w, h):
    return crest(w, h, 1201, 1.0)


def wave_b(w, h):
    return crest(w, h, 1202, 1.25)


def wave_c(w, h):
    return crest(w, h, 1203, 0.85)

"""Generates the Time Magic VFX textures (Chrono Stasis, Chrono Anastasis, rewind / acceleration dials).

Deterministic and font-free: the Roman numerals are drawn from strokes, so every machine produces the same pixels.
Output: src/main/resources/assets/nusmp/textures/particle/time_*.png

    python tools/gen_time_vfx_textures.py

Look reference (Black Clover anime): Chrono Stasis = a pale-blue glass sphere around the target with a white ribbon of Roman
numerals orbiting it at a tilt and four-pointed sparkles inside; Chrono Anastasis = a huge pale-blue/violet clock face with white
Roman numerals and an arrow-tipped hand floating over the area, gold light streaks raining down from it.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4  # supersampling for clean edges

ROMAN = ["XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"]


# ---------------------------------------------------------------------------------------------------- numeral strokes
def glyph_polys(ch, x, h, w_unit):
    """Polygons for one serif capital (I, V or X) at left edge x, height h, on a baseline at y=h. Returns (polys, advance)."""
    t = h * 0.16          # stem thickness
    s = h * 0.10          # serif height
    polys = []
    if ch == "I":
        w = w_unit * 0.55
        cx = x + w / 2
        polys.append([(cx - t / 2, 0), (cx + t / 2, 0), (cx + t / 2, h), (cx - t / 2, h)])
        polys.append([(x, 0), (x + w, 0), (x + w, s), (x, s)])
        polys.append([(x, h - s), (x + w, h - s), (x + w, h), (x, h)])
        return polys, w
    w = w_unit * 1.15
    if ch == "V":
        polys.append([(x + w * 0.05, 0), (x + w * 0.05 + t * 1.1, 0), (x + w / 2 + t * 0.35, h), (x + w / 2 - t * 0.45, h)])
        polys.append([(x + w * 0.95 - t * 0.6, 0), (x + w * 0.95, 0), (x + w / 2 + t * 0.45, h), (x + w / 2 + t * 0.05, h)])
        polys.append([(x, 0), (x + w * 0.38, 0), (x + w * 0.38, s), (x, s)])
        polys.append([(x + w * 0.66, 0), (x + w, 0), (x + w, s), (x + w * 0.66, s)])
        return polys, w
    if ch == "X":
        polys.append([(x + w * 0.08, 0), (x + w * 0.08 + t * 1.1, 0), (x + w * 0.92, h), (x + w * 0.92 - t * 1.1, h)])
        polys.append([(x + w * 0.92 - t * 0.6, 0), (x + w * 0.92, 0), (x + w * 0.08 + t * 0.6, h), (x + w * 0.08, h)])
        polys.append([(x, 0), (x + w * 0.4, 0), (x + w * 0.4, s), (x, s)])
        polys.append([(x + w * 0.6, 0), (x + w, 0), (x + w, s), (x + w * 0.6, s)])
        polys.append([(x, h - s), (x + w * 0.4, h - s), (x + w * 0.4, h), (x, h)])
        polys.append([(x + w * 0.6, h - s), (x + w, h - s), (x + w, h), (x + w * 0.6, h)])
        return polys, w
    raise ValueError(ch)


def numeral_image(text, h, color):
    """RGBA image of a Roman numeral string, height h (pixels, already supersampled)."""
    gap = h * 0.08
    polys, x = [], 0.0
    for ch in text:
        p, adv = glyph_polys(ch, x, h, h * 0.62)
        polys += p
        x += adv + gap
    w = int(math.ceil(x - gap)) + 2
    im = Image.new("RGBA", (max(w, 1), int(h) + 2), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for poly in polys:
        d.polygon([(px + 1, py + 1) for px, py in poly], fill=color)
    return im


def downsample(im, size):
    return im.resize(size, Image.LANCZOS)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


# ---------------------------------------------------------------------------------------------------- textures
def clock_face(size=256):
    """Soft disc with a brighter rim and two thin inner rings. White, tinted pale blue/violet by the vertex colour."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    c = (size - 1) / 2
    r = np.sqrt((xx - c) ** 2 + (yy - c) ** 2) / c
    alpha = np.clip(0.55 + 0.25 * r, 0, 1)
    alpha *= np.clip((1.0 - r) / 0.015, 0, 1)                    # hard-ish outer edge
    rim = np.exp(-((r - 0.975) / 0.02) ** 2) * 0.45
    ring1 = np.exp(-((r - 0.70) / 0.008) ** 2) * 0.35
    ring2 = np.exp(-((r - 0.62) / 0.006) ** 2) * 0.25
    a = np.clip(alpha + (rim + ring1 + ring2) * (r < 1.0), 0, 1)
    light = np.clip(0.80 + 0.20 * r + rim + ring1, 0, 1)        # slightly brighter towards the rim
    rgb = (light * 255).astype(np.uint8)
    img = np.dstack([rgb, rgb, rgb, (a * 255).astype(np.uint8)])
    save(Image.fromarray(img, "RGBA"), "time_clock_face")


def clock_numerals(size=256):
    """XII..XI around the dial (feet towards the centre), minute ticks between the rings. White, drawn over the face."""
    S = size * SS
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    c = S / 2
    for k in range(60):                                          # minute ticks
        a = math.radians(k * 6)
        r0, r1 = c * (0.62 if k % 5 == 0 else 0.655), c * 0.70
        w = SS * (2.2 if k % 5 == 0 else 1.2)
        dx, dy = math.sin(a), -math.cos(a)
        px, py = -dy * w / 2, dx * w / 2
        d.polygon([(c + dx * r0 + px, c + dy * r0 + py), (c + dx * r1 + px, c + dy * r1 + py),
                   (c + dx * r1 - px, c + dy * r1 - py), (c + dx * r0 - px, c + dy * r0 - py)], fill=(255, 255, 255, 255))
    h = c * 0.17
    for k, text in enumerate(ROMAN):
        g = numeral_image(text, h, (255, 255, 255, 255))
        a = k * 30
        g = g.rotate(-a, resample=Image.BICUBIC, expand=True)
        rr = c * 0.835
        cx = c + math.sin(math.radians(a)) * rr
        cy = c - math.cos(math.radians(a)) * rr
        im.alpha_composite(g, (int(cx - g.width / 2), int(cy - g.height / 2)))
    save(downsample(im, (size, size)), "time_clock_numerals")


def clock_hand(w=64, h=256):
    """The arrow-tipped hand from Chrono Anastasis: pivot at the bottom centre, tip at the top. White."""
    W, H = w * SS, h * SS
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx = W / 2
    shaft = W * 0.07
    d.rectangle([cx - shaft, H * 0.22, cx + shaft, H * 0.97], fill=(255, 255, 255, 255))
    d.polygon([(cx, H * 0.01), (cx + W * 0.30, H * 0.26), (cx + W * 0.08, H * 0.22), (cx - W * 0.08, H * 0.22),
               (cx - W * 0.30, H * 0.26)], fill=(255, 255, 255, 255))                   # arrow head
    d.ellipse([cx - W * 0.16, H * 0.40, cx + W * 0.16, H * 0.47], fill=(255, 255, 255, 255))   # collar
    d.rectangle([cx - W * 0.36, H * 0.52, cx + W * 0.36, H * 0.545], fill=(255, 255, 255, 255))  # crossbar
    d.ellipse([cx - W * 0.20, H * 0.90, cx + W * 0.20, H * 0.99], fill=(255, 255, 255, 255))   # pivot
    im = downsample(im, (w, h))
    glow = im.filter(ImageFilter.GaussianBlur(2.5))
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ga = np.array(glow).astype(np.float32)
    ga[..., 3] *= 0.6
    out.alpha_composite(Image.fromarray(ga.astype(np.uint8), "RGBA"))
    out.alpha_composite(im)
    save(out, "time_clock_hand")


def numeral_band(w=512, h=32):
    """The Chrono Stasis ribbon: a white strip with blue Roman numerals and thin edge lines. Tiles horizontally."""
    W, H = w * SS, h * SS
    im = Image.new("RGBA", (W, H), (255, 255, 255, 205))
    d = ImageDraw.Draw(im)
    edge = (150, 165, 235, 255)
    d.rectangle([0, 0, W, H * 0.07], fill=edge)
    d.rectangle([0, H * 0.93, W, H], fill=edge)
    seq = ["XII", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI"]
    gh = H * 0.60
    imgs = [numeral_image(t, gh, (105, 120, 215, 255)) for t in seq]
    total = sum(g.width for g in imgs)
    gap = (W - total) / len(imgs)
    x = gap / 2
    for g in imgs:
        im.alpha_composite(g, (int(x), int((H - g.height) / 2)))
        x += g.width + gap
    save(downsample(im, (w, h)), "time_numeral_band")


def sphere(size=128):
    """Glass bubble seen head-on: faint fill, bright fresnel rim, a soft highlight top-left. Pale blue."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    c = (size - 1) / 2
    nx, ny = (xx - c) / c, (yy - c) / c
    r = np.sqrt(nx ** 2 + ny ** 2)
    inside = np.clip((1.0 - r) / 0.02, 0, 1)
    z = np.sqrt(np.clip(1 - r ** 2, 0, 1))
    fres = (1 - z) ** 2.2
    hl = np.exp(-(((nx + 0.38) / 0.22) ** 2 + ((ny + 0.42) / 0.16) ** 2)) * 0.55
    a = np.clip(0.32 + 0.65 * fres + hl, 0, 1) * inside
    rch = np.clip(170 + 85 * fres + 255 * hl, 0, 255)
    gch = np.clip(200 + 55 * fres + 255 * hl, 0, 255)
    bch = np.full_like(rch, 255)
    img = np.dstack([rch, gch, bch, a * 255]).astype(np.uint8)
    save(Image.fromarray(img, "RGBA"), "time_sphere")


def sparkle(size=64):
    """Four-pointed star glint (the sparkles inside Chrono Stasis). White, drawn additively."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    c = (size - 1) / 2
    x, y = np.abs(xx - c) / c, np.abs(yy - c) / c
    star = np.clip(1 - (np.sqrt(x) + np.sqrt(y)) / 1.0, 0, 1) ** 1.4
    core = np.exp(-((x ** 2 + y ** 2) / 0.02))
    a = np.clip(star * 1.3 + core, 0, 1)
    img = np.dstack([np.full_like(a, 255)] * 3 + [a * 255]).astype(np.uint8)
    save(Image.fromarray(img, "RGBA"), "time_sparkle")


def streak(w=64, h=256, lines=7):
    """A curtain of falling light lines (the gold rain under Chrono Anastasis). Several thin lines of different lengths per quad,
    so a few dozen quads read as the dense rain in the anime. Bright core, soft sides, faded ends."""
    rng = np.random.default_rng(1337)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    a = np.zeros((h, w), np.float32)
    for k in range(lines):
        cx = (k + 0.5) / lines * (w - 1) + rng.uniform(-2.5, 2.5)
        v0, v1 = rng.uniform(0.0, 0.25), rng.uniform(0.7, 1.0)
        width = rng.uniform(0.7, 1.4)
        x = (xx - cx) / width
        across = np.exp(-(x / 0.9) ** 2) + 0.3 * np.exp(-(x / 2.6) ** 2)
        v = (yy / (h - 1) - v0) / (v1 - v0)
        along = np.clip(np.sin(np.clip(v, 0, 1) * math.pi) * 1.6, 0, 1) * rng.uniform(0.6, 1.0)
        a = np.maximum(a, across * along)
    a = np.clip(a, 0, 1)
    img = np.dstack([np.full_like(a, 255)] * 3 + [a * 255]).astype(np.uint8)
    save(Image.fromarray(img, "RGBA"), "time_streak")


if __name__ == "__main__":
    clock_face()
    clock_numerals()
    clock_hand()
    numeral_band()
    sphere()
    sparkle()
    streak()

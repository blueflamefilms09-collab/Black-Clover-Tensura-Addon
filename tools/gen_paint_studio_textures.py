"""Textures for the 0.44 Painting Magic remake (palette & brush): glossy wet-paint ribbons, a specular streak, paint droplets,
pooled paint, the palette silhouette and a pigment swirl. Deterministic, no fonts, no external images.

    python tools/gen_paint_studio_textures.py  ->  src/main/resources/assets/nusmp/textures/particle/paint_*.png

Convention (as the rest of textures/particle): greyscale + alpha, tinted per quad by the vertex colour.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm, grey, save  # noqa: E402

SS = 4


def _mask(w, h, fn, blur=0):
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im).astype(np.float32) / 255


def gloss(w=256, h=64):
    """A wet paint ribbon along U: thick body with bristle-dragged edges, darker toward the edges (depth), a dry-brush tail."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (xx + 0.5) / w, (yy + 0.5) / h
    wob = (fbm(w, h, 32, 301) - 0.5) * 0.18
    half = 0.36 + wob - 0.12 * np.clip((u - 0.8) / 0.2, 0, 1)            # thins into the tail
    d = np.abs(v - 0.5)
    bristle = fbm(w, h, 2, 302) * 0.5 + 0.5 * (np.sin(v * 140 + fbm(w, h, 16, 303) * 6) * 0.5 + 0.5)
    edge = np.clip((half - d) / 0.06, 0, 1)
    tail = np.where(u > 0.82, np.clip(bristle * 1.6 - (u - 0.82) * 4, 0, 1), 1)
    head = np.clip(u / 0.05, 0, 1)
    a = edge * tail * head
    g = 0.55 + 0.45 * (1 - (d / np.maximum(half, 1e-3)) ** 2) + (bristle - 0.5) * 0.15
    save(grey(a, np.clip(g, 0, 1)), "paint_gloss")


def spec(w=256, h=32):
    """The specular highlight on the wet surface: a thin bright line along U, broken into glints."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = (xx + 0.5) / w, (yy + 0.5) / h
    line = np.exp(-((v - 0.5) / 0.09) ** 2)
    glints = np.clip(np.sin(u * 23 + fbm(w, h, 32, 311) * 5) * 0.6 + 0.5, 0, 1) ** 2
    fade = np.clip(u / 0.08, 0, 1) * np.clip((1 - u) / 0.25, 0, 1)
    save(grey(line * (0.35 + 0.65 * glints) * fade), "paint_spec")


def drop(s=64):
    """A falling paint droplet: a teardrop with a bright glossy dot."""
    def fn(d, k):
        c = s * k / 2
        d.ellipse((c - s * k * 0.26, c - s * k * 0.05, c + s * k * 0.26, c + s * k * 0.45), fill=255)
        d.polygon([(c, s * k * 0.04), (c - s * k * 0.2, c + s * k * 0.08), (c + s * k * 0.2, c + s * k * 0.08)], fill=255)
    a = _mask(s, s, fn, 0.6)
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    hl = np.exp(-(((xx - s * 0.42) / (s * 0.07)) ** 2 + ((yy - s * 0.62) / (s * 0.07)) ** 2))
    g = np.clip(0.7 + 0.3 * hl * 3, 0, 1)
    save(grey(a, g), "paint_drop")


def pool(s=64):
    """A pool of wet paint seen from above: a soft-edged blob, darker rim, glossy crescent highlight."""
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x, y = (xx + 0.5) / s * 2 - 1, (yy + 0.5) / s * 2 - 1
    ang = np.arctan2(y, x)
    r = np.hypot(x, y) / (0.8 + 0.08 * np.sin(ang * 3 + 1) + 0.05 * np.sin(ang * 7))
    a = np.clip((1 - r) / 0.12, 0, 1)
    rim = np.clip(1 - np.abs(r - 0.85) / 0.15, 0, 1)
    cres = np.exp(-(((x + 0.28) / 0.22) ** 2 + ((y + 0.3) / 0.13) ** 2))
    g = np.clip(0.8 - 0.25 * rim + 0.6 * cres, 0, 1)
    save(grey(a, g), "paint_pool")


def palette(s=256):
    """The artist's palette from above: a kidney-shaped board with a thumb hole, wood grain in the grey, an outline lip."""
    def board(d, k):
        S = s * k
        d.ellipse((S * 0.04, S * 0.14, S * 0.96, S * 0.88), fill=255)
        d.ellipse((S * -0.10, S * 0.62, S * 0.22, S * 1.02), fill=0)          # the notch where the fingers wrap
        d.ellipse((S * 0.22, S * 0.40, S * 0.36, S * 0.54), fill=0)           # the thumb hole
    a = _mask(s, s, board, 0.5)
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    grain = np.sin((xx * 0.9 + yy * 0.25) / s * 60 + fbm(s, s, 32, 321) * 9) * 0.5 + 0.5
    lip = a - np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(9))).astype(np.float32) / 255
    g = np.clip(0.62 + 0.12 * grain + 0.3 * np.clip(lip, 0, 1), 0, 1)
    save(grey(a, g), "paint_palette_vfx")


def swirl(s=128):
    """A three-armed pigment swirl (element shift): arms thick at the centre, thinning to flicks."""
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    x, y = (xx + 0.5) / s * 2 - 1, (yy + 0.5) / s * 2 - 1
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    arm = np.cos((ang - r * 5.5) * 3) * 0.5 + 0.5
    width = 0.5 + 0.4 * (1 - r)
    a = np.clip((arm - (1 - width)) / 0.15, 0, 1) * np.clip((0.95 - r) / 0.2, 0, 1) * np.clip(r / 0.08, 0, 1)
    save(grey(a, np.clip(0.75 + 0.25 * arm, 0, 1)), "paint_swirl")


def _brush_lines(name, paths, s=128, width=0.045):
    """Line art painted with a loaded brush: each path is a polyline (0..1 coords); the stroke swells in the middle and dries
    out at the ends, with bristle streaks along it."""
    big = s * SS
    im = Image.new("L", (big, big), 0)
    d = ImageDraw.Draw(im)
    for path in paths:
        pts = [(x * big, y * big) for x, y in path]
        n = len(pts) - 1
        for k in range(n):
            (x0, y0), (x1, y1) = pts[k], pts[k + 1]
            steps = max(12, int(math.hypot(x1 - x0, y1 - y0) / (width * big * 0.12)))
            for j in range(steps + 1):
                f = (k + j / steps) / max(1, n)
                w = width * big * (0.45 + 0.55 * math.sin(math.pi * min(1, max(0, f))))
                x, y = x0 + (x1 - x0) * j / steps, y0 + (y1 - y0) * j / steps
                d.ellipse((x - w / 2, y - w / 2, x + w / 2, y + w / 2), fill=255)
    a = np.asarray(im.resize((s, s), Image.LANCZOS).filter(ImageFilter.GaussianBlur(0.5))).astype(np.float32) / 255
    streak = 0.75 + 0.25 * fbm(s, s, 2, 331)
    save(grey(a * np.clip(streak + 0.1, 0, 1), np.clip(streak + 0.15, 0, 1)), name)


def sketches():
    """The living illustrations' line art as they lift off the ground: a beast, a knight (einherjar), a giant."""
    _brush_lines("paint_sketch_beast", [
        [(0.12, 0.52), (0.22, 0.42), (0.30, 0.34), (0.38, 0.40), (0.62, 0.42), (0.80, 0.40), (0.90, 0.30)],     # head, back, tail
        [(0.12, 0.52), (0.20, 0.56), (0.30, 0.50)],                                                         # jaw
        [(0.30, 0.34), (0.27, 0.24), (0.33, 0.30)],                                                         # ear
        [(0.34, 0.50), (0.32, 0.70), (0.30, 0.86)], [(0.42, 0.50), (0.44, 0.70), (0.42, 0.86)],             # fore legs
        [(0.66, 0.52), (0.64, 0.70), (0.62, 0.86)], [(0.76, 0.50), (0.78, 0.70), (0.80, 0.86)],             # hind legs
        [(0.34, 0.50), (0.55, 0.56), (0.76, 0.50)]])                                                        # belly
    _brush_lines("paint_sketch_knight", [
        [(0.50, 0.08), (0.42, 0.14), (0.44, 0.24), (0.56, 0.24), (0.58, 0.14), (0.50, 0.08)],              # helm
        [(0.50, 0.24), (0.50, 0.58)], [(0.34, 0.30), (0.66, 0.30)],                                         # body, shoulders
        [(0.34, 0.30), (0.28, 0.46), (0.30, 0.56)], [(0.66, 0.30), (0.74, 0.42), (0.80, 0.34)],             # arms
        [(0.80, 0.34), (0.86, 0.06)], [(0.76, 0.30), (0.84, 0.36)],                                         # raised sword + guard
        [(0.18, 0.40), (0.30, 0.38), (0.32, 0.56), (0.24, 0.64), (0.16, 0.56), (0.18, 0.40)],              # shield
        [(0.50, 0.58), (0.40, 0.76), (0.38, 0.94)], [(0.50, 0.58), (0.60, 0.76), (0.62, 0.94)]])            # legs
    _brush_lines("paint_sketch_giant", [
        [(0.50, 0.06), (0.40, 0.10), (0.40, 0.20), (0.60, 0.20), (0.60, 0.10), (0.50, 0.06)],              # head
        [(0.24, 0.26), (0.76, 0.26), (0.68, 0.60), (0.32, 0.60), (0.24, 0.26)],                            # huge torso
        [(0.24, 0.26), (0.10, 0.46), (0.08, 0.66)], [(0.76, 0.26), (0.90, 0.46), (0.92, 0.66)],             # long arms
        [(0.04, 0.66), (0.12, 0.72)], [(0.88, 0.72), (0.96, 0.66)],                                         # fists
        [(0.36, 0.60), (0.34, 0.80), (0.30, 0.96)], [(0.64, 0.60), (0.66, 0.80), (0.70, 0.96)]],            # legs
        width=0.06)


def main():
    gloss()
    spec()
    drop()
    pool()
    palette()
    swirl()
    sketches()


if __name__ == "__main__":
    main()

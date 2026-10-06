"""Textures for the 0.40 Dream Magic and Painting Magic VFX. Deterministic, no fonts, no external images.

    python tools/gen_dream_paint_vfx_textures.py   ->  src/main/resources/assets/nusmp/textures/particle/dream_*.png, paint_*.png

Convention (same as the rest of textures/particle): white / greyscale with alpha, so one vertex colour tints each quad; the
iridescence comes from shifting the vertex colours across the quads (pink -> lilac -> sky blue), not from the texture.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm, grey, mask_draw, save, SS  # noqa: E402


# ------------------------------------------------------------------------------------------------ dream
def dream_mist(size=128):
    """A soft, rounded pastel cloud puff: several overlapping lobes with a bright rim (Glamour World's mist)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32) / size
    a = np.zeros((size, size), np.float32)
    for (cx, cy, r) in ((0.5, 0.55, 0.30), (0.33, 0.5, 0.22), (0.67, 0.48, 0.23), (0.45, 0.36, 0.2), (0.6, 0.62, 0.2)):
        d = np.hypot(xx - cx, yy - cy) / r
        a = np.maximum(a, np.clip(1.15 - d, 0, 1))
    n = fbm(size, size, 24, 41)
    a = np.clip(a * (0.8 + 0.4 * n), 0, 1) ** 1.2
    g = 0.82 + 0.18 * np.clip((0.55 - yy) * 2, 0, 1)            # lit from above: the tops of the lobes are brighter
    save(grey(a * 0.95, g), "dream_mist")


def dream_star(size=64):
    """A four-pointed starlight twinkle with a soft halo."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2 + 0.5) / (size / 2), (yy - size / 2 + 0.5) / (size / 2)
    r = np.hypot(x, y)
    rays = np.clip(1 - np.abs(x) * 9, 0, 1) * np.clip(1 - np.abs(y), 0, 1) + np.clip(1 - np.abs(y) * 9, 0, 1) * np.clip(1 - np.abs(x), 0, 1)
    diag = (np.clip(1 - np.abs(x - y) * 7, 0, 1) + np.clip(1 - np.abs(x + y) * 7, 0, 1)) * np.clip(1 - r * 1.6, 0, 1) * 0.45
    core = np.clip(1 - r * 4, 0, 1)
    halo = np.clip(1 - r, 0, 1) ** 3 * 0.45
    save(grey(np.clip(rays ** 1.5 + diag + core + halo, 0, 1)), "dream_star")


def dream_bubble(size=96):
    """A soap bubble: a thin bright rim, a faint body and a crescent highlight."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2 + 0.5) / (size / 2), (yy - size / 2 + 0.5) / (size / 2)
    r = np.hypot(x, y)
    rim = np.exp(-((r - 0.9) / 0.06) ** 2)
    body = np.clip(0.9 - r, 0, 1) * 0.18 * (r < 0.92)
    hl = np.exp(-((np.hypot(x + 0.35, y + 0.4) - 0.28) / 0.07) ** 2) * (x < -0.1) * (y < -0.05)
    save(grey(np.clip(rim * 0.85 + body + hl, 0, 1)), "dream_bubble")


def dream_veil(w=64, h=256):
    """A tall aurora curtain for the dome wall: vertical shimmer streaks, fading out at the top and bottom; tiles left-right."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    v = yy / h
    streak = fbm(w, h, 8, 52)
    streak = 0.4 + 0.6 * np.clip((streak - 0.35) * 2.2, 0, 1)
    wave = 0.75 + 0.25 * np.sin(xx / w * math.tau * 2 + v * 6)
    fade = np.clip(v * 5, 0, 1) * np.clip((1 - v) * 1.6, 0, 1)
    sparkle = (fbm(w, h, 2, 77) > 0.83) * 0.6
    a = np.clip(streak * wave * fade * 0.7 + sparkle * fade, 0, 1)
    save(grey(a, 0.85 + 0.15 * streak), "dream_veil")


def dream_band(w=256, h=32):
    """A horizontal ribbon of iridescent shimmer (the dome's latitude bands, the transition swirl); tiles left-right."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    v = (yy + 0.5) / h
    edge = np.exp(-((v - 0.5) / 0.34) ** 2)
    ripple = 0.8 + 0.2 * np.sin(xx / w * math.tau * 3 + np.sin(xx / w * math.tau) * 2)
    n = fbm(w, h, 16, 63)
    save(grey(np.clip(edge * ripple * (0.75 + 0.5 * n), 0, 1)), "dream_band")


# ------------------------------------------------------------------------------------------------ painting
def paint_stroke(w=256, h=64, seed=5, name="paint_stroke"):
    """A single wet brush stroke along U: bristle grooves, a ragged dry-brush tail, a glossy wet ridge; tiles nowhere."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    u, v = xx / w, (yy + 0.5) / h
    width = 0.42 * np.clip(u * 8, 0, 1) ** 0.5 * (1 - 0.55 * u ** 3)              # loaded at the start, thinning out
    centre = 0.5 + 0.06 * np.sin(u * 7 + 1.3)
    d = np.abs(v - centre) / np.maximum(width, 1e-3)
    bristle = fbm(w, h, 4, seed) * 0.6 + 0.4 * (np.sin(v * 140 + rng.random() * 9) * 0.5 + 0.5)
    dry = np.clip((1 - u) * 3.2 - bristle * 1.2 + 0.2, 0, 1)                       # breaks up into bristle marks at the end
    a = np.clip((1 - d) * 3, 0, 1) * np.clip(dry + (u < 0.6), 0, 1)
    a *= 0.75 + 0.25 * bristle
    gloss = np.exp(-((v - centre + width * 0.35) / 0.05) ** 2) * (u < 0.85)        # the wet highlight along one side
    g = np.clip(0.7 + 0.3 * bristle + gloss * 0.6, 0, 1)
    save(grey(a, g), name)


def paint_splat(size=128, seed=9, name="paint_splat"):
    """A paint splatter: a blob with tendrils and droplets thrown out round it."""
    rng = np.random.default_rng(seed)

    def draw(d, s):
        c = size * s / 2
        d.ellipse((c - size * s * 0.22, c - size * s * 0.2, c + size * s * 0.22, c + size * s * 0.24), fill=255)
        for k in range(11):
            a = rng.random() * math.tau
            L = size * s * (0.22 + 0.22 * rng.random())
            wdt = size * s * (0.03 + 0.04 * rng.random())
            x1, y1 = c + math.cos(a) * L, c + math.sin(a) * L
            d.line((c, c, x1, y1), fill=255, width=int(wdt))
            r = wdt * (0.8 + rng.random())
            d.ellipse((x1 - r, y1 - r, x1 + r, y1 + r), fill=255)
        for k in range(14):
            a = rng.random() * math.tau
            L = size * s * (0.32 + 0.16 * rng.random())
            r = size * s * (0.008 + 0.02 * rng.random())
            x1, y1 = c + math.cos(a) * L, c + math.sin(a) * L
            d.ellipse((x1 - r, y1 - r, x1 + r, y1 + r), fill=255)

    a = mask_draw(size, size, draw, blur=0.5)
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    gloss = np.clip(1 - np.hypot(xx - size * 0.42, yy - size * 0.4) / (size * 0.12), 0, 1) ** 2
    g = np.clip(0.78 + 0.22 * fbm(size, size, 16, seed + 3) + gloss * 0.5, 0, 1)
    save(grey(a, g), name)


def paint_outline(size=128):
    """A ring of glowing wet brushwork: an uneven hand-drawn outline (the glow round painted constructs, the ground circle)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2 + 0.5) / (size / 2), (yy - size / 2 + 0.5) / (size / 2)
    r = np.hypot(x, y)
    ang = np.arctan2(y, x)
    wob = 0.82 + 0.04 * np.sin(ang * 3 + 1) + 0.025 * np.sin(ang * 7)
    width = 0.05 + 0.03 * (np.sin(ang * 2 + 0.5) * 0.5 + 0.5)
    a = np.clip(1 - np.abs(r - wob) / width, 0, 1) ** 0.8
    a *= np.clip((ang + math.pi) / 0.5, 0, 1)                                    # the stroke starts thin and opens up
    save(grey(a, 0.85 + 0.15 * fbm(size, size, 8, 21)), "paint_outline")


def paint_canvas(size=128):
    """A glossy ink sheet with a woven-canvas grain (the painted beasts' bodies, the lacquer of a dried trap)."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    weave = (np.sin(xx * 0.9) * np.sin(yy * 0.9)) * 0.08
    n = fbm(size, size, 32, 30)
    sheen = np.clip(np.sin((xx + yy) / size * math.pi * 1.5) * 0.5 + 0.5, 0, 1) ** 6 * 0.35
    save(grey(np.ones((size, size)) * 0.92, np.clip(0.72 + 0.22 * n + weave + sheen, 0, 1)), "paint_canvas")


def paint_refract(size=128):
    """A camouflage shimmer: a faint, wavy refraction ripple that bends the outline of whatever stands behind it."""
    yy, xx = np.mgrid[0:size, 0:size].astype(np.float32)
    x, y = (xx - size / 2 + 0.5) / (size / 2), (yy - size / 2 + 0.5) / (size / 2)
    r = np.hypot(x, y)
    ripple = np.sin(r * 22 - fbm(size, size, 16, 88) * 6) * 0.5 + 0.5
    a = ripple ** 6 * np.clip(1 - r, 0, 1) * 0.8 + np.clip(0.98 - r, 0, 1) * 0.06
    save(grey(np.clip(a, 0, 1)), "paint_refract")


def main():
    dream_mist()
    dream_star()
    dream_bubble()
    dream_veil()
    dream_band()
    paint_stroke()
    paint_stroke(seed=17, name="paint_stroke2")
    paint_splat()
    paint_outline()
    paint_canvas()
    paint_refract()


if __name__ == "__main__":
    main()

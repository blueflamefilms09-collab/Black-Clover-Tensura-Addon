"""Textures for the 0.45 extended attributes: Light, World Tree, Dice, Slash, Compass, Mercury. Deterministic; no fonts (dice
numbers are drawn as engraved seven-segment strokes), no external images.

    python tools/gen_attr_vfx_textures.py  ->  src/main/resources/assets/nusmp/textures/particle/{light,tree,dice,slash,compass,merc}_*.png

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


def mask(w, h, fn, blur=0.0):
    im = Image.new("L", (w * SS, h * SS), 0)
    fn(ImageDraw.Draw(im), SS)
    im = im.resize((w, h), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im).astype(np.float32) / 255


def uv(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return (xx + 0.5) / w, (yy + 0.5) / h


# ================================================================ Light
def light():
    # a geometric sword pointing +U: a long diamond blade with a fuller, a crossguard and a short grip
    def sword(d, k):
        W, H = 256 * k, 64 * k
        d.polygon([(W * 0.22, H * 0.5), (W * 0.30, H * 0.30), (W * 0.98, H * 0.5), (W * 0.30, H * 0.70)], fill=255)
        d.rectangle((W * 0.18, H * 0.12, W * 0.23, H * 0.88), fill=255)
        d.rectangle((W * 0.03, H * 0.42, W * 0.19, H * 0.58), fill=200)
        d.ellipse((W * 0.0, H * 0.36, W * 0.06, H * 0.64), fill=255)
    a = mask(256, 64, sword, 0.6)
    u, v = uv(256, 64)
    core = np.exp(-((v - 0.5) / 0.05) ** 2) * (u > 0.22)
    save(grey(np.clip(a + core * 0.3, 0, 1), np.clip(0.75 + 0.25 * core + 0.1 * (u > 0.3), 0, 1)), "light_blade")
    # the streak behind it
    streak = np.exp(-((v - 0.5) / 0.12) ** 2) * np.clip(u * 1.2, 0, 1) ** 1.5 + np.exp(-((v - 0.5) / 0.025) ** 2) * u
    save(grey(np.clip(streak, 0, 1)), "light_streak")
    # lens flare star: six long thin rays and a soft core
    s = 256
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    rays = sum(np.exp(-((np.sin(ang - k * math.pi / 3)) * r / 0.012) ** 2) for k in range(3)) * np.clip(1 - r, 0, 1) ** 1.5
    core = np.exp(-(r / 0.12) ** 2)
    save(grey(np.clip(rays * 0.9 + core, 0, 1)), "light_star")
    # a hexagonal lens ghost (ring with a soft fill)
    hexr = r * np.cos((ang % (math.pi / 3)) - math.pi / 6) / math.cos(math.pi / 6)
    ghost = np.clip(1 - np.abs(hexr - 0.8) / 0.06, 0, 1) * 0.9 + np.clip(0.8 - hexr, 0, 1) * 0.25
    save(grey(np.clip(ghost, 0, 1)), "light_ghost")


# ================================================================ World Tree
def tree():
    # a gnarled root segment along U: thick at the base, tapering, bark furrows along it
    u, v = uv(256, 64)
    half = 0.42 * (1 - u * 0.75) + (fbm(256, 64, 32, 401) - 0.5) * 0.08
    d = np.abs(v - 0.5)
    a = np.clip((half - d) / 0.04, 0, 1) * np.clip((1 - u) / 0.04, 0, 1)
    furrow = np.sin(v * 40 + fbm(256, 64, 16, 402) * 8 + u * 6) * 0.5 + 0.5
    shade = 0.45 + 0.4 * (1 - (d / np.maximum(half, 1e-3)) ** 2) + 0.2 * furrow
    save(grey(a, np.clip(shade, 0, 1)), "tree_root")
    # a leaf
    def leaf(dd, k):
        S = 64 * k
        dd.polygon([(S * 0.5, S * 0.04), (S * 0.86, S * 0.42), (S * 0.5, S * 0.96), (S * 0.14, S * 0.42)], fill=255)
    a = mask(64, 64, leaf, 1.2)
    u, v = uv(64, 64)
    vein = np.exp(-((u - 0.5) / 0.02) ** 2) * 0.4
    save(grey(a, np.clip(0.7 + vein + (0.5 - v) * 0.3, 0, 1)), "tree_leaf")
    # a puffy clump of canopy
    s = 128
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    clump = np.zeros((s, s), np.float32)
    rng = np.random.default_rng(403)
    for _ in range(14):
        cx, cy, rr = rng.uniform(-0.5, 0.5), rng.uniform(-0.45, 0.35), rng.uniform(0.25, 0.45)
        clump = np.maximum(clump, np.clip((rr - np.hypot(x - cx, y - cy)) / 0.08, 0, 1))
    light_ = np.clip(0.55 + 0.35 * (-y) + 0.25 * fbm(s, s, 8, 404), 0, 1)
    save(grey(clump, light_), "tree_canopy")


# ================================================================ Dice
SEG = {  # seven-segment digits: a b c d e f g
    "0": "abcdef", "1": "bc", "2": "abged", "3": "abgcd", "4": "fgbc", "5": "afgcd", "6": "afgedc", "7": "abc", "8": "abcdefg", "9": "abcfgd"}


def digit(d, ch, x, y, w, h, t):
    """Draws one seven-segment digit with its top-left at (x, y)."""
    seg = {"a": [(x, y), (x + w, y)], "b": [(x + w, y), (x + w, y + h / 2)], "c": [(x + w, y + h / 2), (x + w, y + h)],
           "d": [(x, y + h), (x + w, y + h)], "e": [(x, y + h / 2), (x, y + h)], "f": [(x, y), (x, y + h / 2)],
           "g": [(x, y + h / 2), (x + w, y + h / 2)]}
    for s_ in SEG[ch]:
        d.line(seg[s_], fill=255, width=int(t))
        for px, py in seg[s_]:
            d.ellipse((px - t / 2, py - t / 2, px + t / 2, py + t / 2), fill=255)


def number(d, n, cx, cy, h, t):
    s = str(n)
    w = h * 0.5
    gap = h * 0.25
    total = len(s) * w + (len(s) - 1) * gap
    x = cx - total / 2
    for ch in s:
        digit(d, ch, x, cy - h / 2, w, h, t)
        x += w + gap


def symbol(d, k, c, r):
    """The six elemental glyphs etched on the d6 (1 fire, 2 earth, 3 water, 4 wind, 5 lightning, 6 light)."""
    cx, cy = c
    if k == 1:          # flame
        d.polygon([(cx, cy - r), (cx + r * 0.6, cy + r * 0.2), (cx + r * 0.4, cy + r * 0.8), (cx, cy + r * 0.45), (cx - r * 0.4, cy + r * 0.8),
                   (cx - r * 0.6, cy + r * 0.2)], fill=255)
        d.polygon([(cx, cy - r * 0.2), (cx + r * 0.25, cy + r * 0.45), (cx, cy + r * 0.75), (cx - r * 0.25, cy + r * 0.45)], fill=0)
    elif k == 2:        # mountain and pine
        d.polygon([(cx - r, cy + r * 0.8), (cx - r * 0.2, cy - r * 0.8), (cx + r * 0.5, cy + r * 0.8)], fill=255)
        d.polygon([(cx + r * 0.65, cy - r * 0.5), (cx + r, cy + r * 0.8), (cx + r * 0.3, cy + r * 0.8)], fill=255)
    elif k == 3:        # wave
        d.arc((cx - r, cy - r * 0.6, cx + r * 0.4, cy + r * 0.8), 180, 360, fill=255, width=int(r * 0.22))
        d.arc((cx - r * 0.3, cy - r * 0.1, cx + r, cy + r * 1.0), 180, 330, fill=255, width=int(r * 0.22))
        d.ellipse((cx + r * 0.05, cy - r * 0.55, cx + r * 0.45, cy - r * 0.15), fill=255)
    elif k == 4:        # wind: cloud with streaks
        d.ellipse((cx - r * 0.7, cy - r * 0.7, cx + r * 0.2, cy + r * 0.1), fill=255)
        d.ellipse((cx - r * 0.1, cy - r * 0.5, cx + r * 0.8, cy + r * 0.2), fill=255)
        for k2, yy in enumerate((0.35, 0.6, 0.85)):
            d.line([(cx - r * (0.9 - 0.2 * k2), cy + r * yy), (cx + r * (0.9 - 0.3 * k2), cy + r * yy)], fill=255, width=int(r * 0.12))
    elif k == 5:        # lightning bolt
        d.polygon([(cx + r * 0.2, cy - r), (cx - r * 0.5, cy + r * 0.1), (cx - r * 0.05, cy + r * 0.1), (cx - r * 0.3, cy + r),
                   (cx + r * 0.55, cy - r * 0.2), (cx + r * 0.05, cy - r * 0.2)], fill=255)
    else:               # light: a sun
        d.ellipse((cx - r * 0.45, cy - r * 0.45, cx + r * 0.45, cy + r * 0.45), fill=255)
        for j in range(8):
            a = j * math.pi / 4
            d.line([(cx + math.cos(a) * r * 0.6, cy + math.sin(a) * r * 0.6), (cx + math.cos(a) * r, cy + math.sin(a) * r)], fill=255,
                   width=int(r * 0.15))


def dice():
    # d6 face atlas (3 x 2 cells of 64): faces 1..6, each with its element glyph and its number in the corner (the engraving)
    W, H = 192, 128

    def faces(d, k):
        for f in range(6):
            cx, cy = (f % 3) * 64 * k, (f // 3) * 64 * k
            symbol(d, f + 1, (cx + 30 * k, cy + 36 * k), 19 * k)
            number(d, f + 1, cx + 51 * k, cy + 14 * k, 14 * k, 2.6 * k)
    save(grey(mask(W, H, faces, 0.4)), "dice_d6_glyphs")
    # the resin body of one face: bright bevelled rim, a milky swirl inside (tinted per die)
    u, v = uv(64, 64)
    edge = np.minimum(np.minimum(u, 1 - u), np.minimum(v, 1 - v))
    rim = np.clip(1 - edge / 0.08, 0, 1)
    swirl = fbm(64, 64, 16, 411) * 0.5 + 0.5 * (np.sin((u + v) * 9 + fbm(64, 64, 8, 412) * 6) * 0.5 + 0.5)
    save(grey(np.clip(0.72 + 0.2 * rim, 0, 1) * np.ones_like(u), np.clip(0.55 + 0.25 * swirl + 0.3 * rim, 0, 1)), "dice_resin")
    # d20 number atlas (5 x 4 cells of 64): 1..20, centred low in each cell (the triangle's centroid sits at 2/3 height)
    def nums(d, k):
        for n in range(1, 21):
            cx, cy = ((n - 1) % 5) * 64 * k, ((n - 1) // 5) * 64 * k
            number(d, n, cx + 32 * k, cy + 40 * k, 15 * k, 2.8 * k)
            if n in (6, 9):
                d.line([(cx + 24 * k, cy + 52 * k), (cx + 40 * k, cy + 52 * k)], fill=255, width=int(2 * k))   # 6 / 9 underline
    save(grey(mask(320, 256, nums, 0.4)), "dice_d20_numbers")
    # a triangular face of resin with bevelled edges
    def tri(d, k):
        S = 64 * k
        d.polygon([(S * 0.5, S * 0.02), (S * 0.98, S * 0.9), (S * 0.02, S * 0.9)], fill=255)
    a = mask(64, 64, tri, 0.4)
    inner = mask(64, 64, lambda d, k: d.polygon([(32 * k, 10 * k), (58 * k, 54 * k), (6 * k, 54 * k)], fill=255), 2.0)
    save(grey(a * 0.85, np.clip(0.95 - 0.35 * inner + 0.15 * fbm(64, 64, 8, 413), 0, 1)), "dice_tri")
    # the galaxy core inside the d20: a spiral of dust with stars
    s = 128
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    arms = (np.cos((ang - np.log(r + 0.05) * 2.6) * 2) * 0.5 + 0.5) ** 2 * np.clip(1 - r, 0, 1)
    rng = np.random.default_rng(414)
    stars = (rng.random((s, s)) > 0.985).astype(np.float32) * np.clip(1.2 - r, 0, 1)
    g = np.clip(arms * 0.8 + np.exp(-(r / 0.15) ** 2) + stars, 0, 1)
    save(grey(g), "dice_galaxy")


# ================================================================ Slash
def slash():
    # a jagged blade along U (serrated top edge, a hooked tip)
    def blade(d, k):
        W, H = 256 * k, 64 * k
        pts = [(0, H * 0.62)]
        for j in range(9):
            x = W * (0.06 + j * 0.1)
            pts += [(x, H * (0.38 - 0.012 * j)), (x + W * 0.05, H * (0.22 - 0.012 * j))]
        pts += [(W * 0.99, H * 0.12), (W * 0.9, H * 0.5), (W * 0.6, H * 0.7), (0, H * 0.78)]
        d.polygon(pts, fill=255)
    a = mask(256, 64, blade, 0.5)
    u, v = uv(256, 64)
    save(grey(a, np.clip(0.7 + 0.3 * np.exp(-((v - 0.45) / 0.08) ** 2), 0, 1)), "slash_blade")
    # a jagged crescent (projected cut)
    s = 128
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    jag = 0.04 * np.abs(np.sin(ang * 14))
    band = np.clip(1 - np.abs(r - 0.78 - jag) / (0.13 * np.clip(np.cos(ang), 0, 1) + 0.005), 0, 1) * (x > -0.1)
    save(grey(np.clip(band, 0, 1), np.clip(0.8 + 0.2 * np.cos(ang), 0, 1)), "slash_crescent")
    # a wide arc band for the scythe (U = along the arc)
    u, v = uv(256, 64)
    edge = np.clip((0.5 - np.abs(v - 0.5 - 0.15 * np.sin(u * 3.14))) / 0.1, 0, 1) * np.clip(np.sin(u * math.pi) * 1.5, 0, 1)
    teeth = 0.85 + 0.15 * np.abs(np.sin(u * 60))
    save(grey(edge * teeth, np.clip(0.6 + 0.4 * (1 - v), 0, 1)), "slash_arc")


# ================================================================ Compass
def compass():
    s = 256
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    r, ang = np.hypot(x, y), np.arctan2(y, x)
    # brass rim: thick ring with a bevel and engraved ticks
    ring = np.clip((0.98 - r) / 0.02, 0, 1) * np.clip((r - 0.78) / 0.02, 0, 1)
    bevel = 0.6 + 0.4 * np.cos((r - 0.88) / 0.1 * math.pi / 2) ** 2 * (0.7 + 0.3 * np.cos(ang - 2.2))
    ticks = (np.abs(np.sin(ang * 36)) < 0.08) * (r > 0.83) * (r < 0.93)
    save(grey(ring, np.clip(bevel - 0.4 * ticks, 0, 1)), "compass_rim")
    # dial: concentric bands, cardinal ticks and an eight-point rose (drawn bright on a mid dial)
    dial = np.clip((0.8 - r) / 0.01, 0, 1)
    bands = 0.55 + 0.1 * (np.abs(r - 0.55) < 0.015) + 0.1 * (np.abs(r - 0.7) < 0.012)
    def rose(d, k):
        S = s * k
        c = S / 2
        for j in range(8):
            a = j * math.pi / 4
            R = S * (0.36 if j % 2 == 0 else 0.22)
            tip = (c + math.cos(a) * R, c + math.sin(a) * R)
            l = (c + math.cos(a + 0.4) * S * 0.05, c + math.sin(a + 0.4) * S * 0.05)
            rr = (c + math.cos(a - 0.4) * S * 0.05, c + math.sin(a - 0.4) * S * 0.05)
            d.polygon([l, tip, (c, c)], fill=255)
            d.polygon([rr, tip, (c, c)], fill=170)
        for j in range(32):
            a = j * math.pi / 16
            d.line([(c + math.cos(a) * S * 0.36, c + math.sin(a) * S * 0.36), (c + math.cos(a) * S * 0.38, c + math.sin(a) * S * 0.38)],
                   fill=230, width=int(1.5 * k))
    rm = mask(s, s, rose, 0.5)
    save(grey(dial, np.clip(bands * (1 - rm) + rm * 1.0, 0, 1)), "compass_dial")
    # needle along U (red-ish north half bright, south half dimmer; tinted gold by the VFX)
    def needle(d, k):
        W, H = 128 * k, 32 * k
        d.polygon([(W * 0.02, H * 0.5), (W * 0.5, H * 0.3), (W * 0.98, H * 0.5), (W * 0.5, H * 0.7)], fill=255)
    a = mask(128, 32, needle, 0.4)
    u, v = uv(128, 32)
    save(grey(a, np.clip(0.55 + 0.45 * (u > 0.5), 0, 1)), "compass_needle")
    # glass: a soft curved sheen
    sheen = np.clip(1 - r / 0.8, 0, 1) * 0.15 + np.exp(-(((x + 0.25) / 0.25) ** 2 + ((y + 0.3) / 0.12) ** 2)) * 0.7
    save(grey(np.clip(sheen * (r < 0.8), 0, 1)), "compass_glass")


# ================================================================ Mercury
def mercury():
    # chrome strip: a sharp sky/ground horizon reflection with streaks, along U (scrolls for flow)
    u, v = uv(256, 64)
    horizon = 0.5 + 0.08 * np.sin(u * 12.566 + fbm(256, 64, 32, 421) * 3)
    sky = np.clip((horizon - v) / 0.02, 0, 1)
    g = 0.25 + 0.65 * sky * (0.75 + 0.25 * (1 - v)) + 0.6 * np.exp(-((v - horizon + 0.04) / 0.02) ** 2)
    streak = 0.12 * np.sin(v * 50 + u * 3) * (1 - sky)
    save(grey(np.ones_like(u) * 0.95, np.clip(g + streak, 0, 1)), "merc_chrome")
    # a sphere of liquid metal: dark rim, bright environment band, hot specular
    s = 128
    u, v = uv(s, s)
    x, y = u * 2 - 1, v * 2 - 1
    r = np.hypot(x, y)
    a = np.clip((1 - r) / 0.03, 0, 1)
    nz = np.sqrt(np.clip(1 - r * r, 0, 1))
    env = np.clip((0.1 - y * 0.8 + 0.3 * nz), 0, 1)
    spec = np.exp(-(((x + 0.35) / 0.12) ** 2 + ((y + 0.4) / 0.08) ** 2))
    save(grey(a, np.clip(0.2 + 0.7 * env * nz + spec + 0.3 * (1 - nz) ** 4, 0, 1)), "merc_sphere")
    # spear along U
    def spear(d, k):
        W, H = 256 * k, 32 * k
        d.polygon([(0, H * 0.42), (W * 0.7, H * 0.4), (W * 0.99, H * 0.5), (W * 0.7, H * 0.6), (0, H * 0.58)], fill=255)
        d.polygon([(W * 0.62, H * 0.18), (W * 0.74, H * 0.5), (W * 0.62, H * 0.82), (W * 0.66, H * 0.5)], fill=255)
    a = mask(256, 32, spear, 0.4)
    u, v = uv(256, 32)
    save(grey(a, np.clip(0.4 + 0.6 * np.exp(-((v - 0.44) / 0.06) ** 2) + 0.2 * (u > 0.62), 0, 1)), "merc_spear")
    # eagle with spread wings, seen from below
    def eagle(d, k):
        S = 256 * k
        d.ellipse((S * 0.44, S * 0.30, S * 0.56, S * 0.62), fill=255)                      # body
        d.ellipse((S * 0.455, S * 0.22, S * 0.545, S * 0.33), fill=255)                    # head
        d.polygon([(S * 0.5, S * 0.18), (S * 0.53, S * 0.25), (S * 0.47, S * 0.25)], fill=255)   # beak
        for sx in (-1, 1):
            # leading edge sweeps up and out to the wingtip; the trailing edge comes back in five feather scallops
            pts = [(S * 0.5 + sx * S * 0.04, S * 0.34), (S * 0.5 + sx * S * 0.2, S * 0.24), (S * 0.5 + sx * S * 0.36, S * 0.2),
                   (S * 0.5 + sx * S * 0.48, S * 0.24)]
            for j in range(5):
                x0 = S * 0.5 + sx * S * (0.48 - j * 0.085)
                pts += [(x0 - sx * S * 0.01, S * (0.38 + 0.03 * j)), (x0 - sx * S * 0.07, S * (0.31 + 0.03 * j))]
            pts += [(S * 0.5 + sx * S * 0.05, S * 0.5)]
            d.polygon(pts, fill=255)
        d.polygon([(S * 0.45, S * 0.6), (S * 0.55, S * 0.6), (S * 0.6, S * 0.76), (S * 0.5, S * 0.72), (S * 0.4, S * 0.76)], fill=255)  # tail
    a = mask(256, 256, eagle, 0.8)
    u, v = uv(256, 256)
    save(grey(a, np.clip(0.45 + 0.4 * (1 - v) + 0.2 * fbm(256, 256, 16, 422), 0, 1)), "merc_eagle")


def main():
    light()
    tree()
    dice()
    slash()
    compass()
    mercury()


if __name__ == "__main__":
    main()

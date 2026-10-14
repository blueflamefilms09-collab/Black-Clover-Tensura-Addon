#!/usr/bin/env python3
"""
Textures of the animated boss bar (client/gui/bossbar, docs: the MAS phase 5 boss bar). Original pixel art, neutral grey so that the game
tints it per boss (the primary / secondary colours of the boss bar sync). Everything lives in assets/nusmp/textures/gui/bossbar/:

    frame.png   210x15  ornate metal frame with a transparent 182x7 window at (14, 4); glow lanes in rows 1-3 and 11-13
    bg.png      182x7   dark inside of the window
    fill.png    182x7   shaded grey fill (tinted per segment)
    flame.png    64x7   tileable flame / aura tongues, white with alpha (drawn additively, scrolled)
    shine.png    28x7   soft diagonal streak that travels along the fill (additive)
    glow.png     64x3   tileable dashes for the scrolling border glow (additive)
    pips.png     16x8   phase gems: unlit at u=0, lit at u=8
    edge.png      4x7   bright leading edge of the fill (additive)

Idempotent: running it again rewrites the same files.

    python3 tools/gen_bossbar_textures.py            # write the textures
    python3 tools/gen_bossbar_textures.py --preview out.png [primary secondary]   # a 4x mock-up of a bar (not part of the build)
"""
import math
import os
import random
import sys

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "gui", "bossbar")

CAP = 14                      # width of each end piece of the frame
W, FRAME_W, FRAME_H, FILL_H = 182, 182 + 2 * CAP, 15, 7
WIN_X, WIN_Y = CAP, 4         # the window of the frame (where the fill shows)

OUTLINE = (13, 12, 18, 255)
DARK = (43, 42, 54, 255)
CHANNEL = (30, 29, 38, 255)
MID = (108, 112, 126, 255)
LIGHT = (166, 172, 188, 255)
HI = (228, 232, 242, 255)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name))


# ================================================================ frame
def cap_left():
    """The CAPx15 left end of the frame: a round-nosed shield with a plate, a gem and two fins; the right end is its mirror image."""
    im = Image.new("RGBA", (CAP, FRAME_H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    body = [(13, 0), (7, 0), (4, 1), (2, 3), (0, 6), (0, 8), (2, 11), (4, 13), (7, 14), (13, 14)]
    d.polygon(body, fill=MID)
    # bevel: light along the upper-left edge, dark along the lower one
    d.line([(8, 1), (5, 1), (3, 3), (1, 6)], fill=LIGHT)
    d.line([(13, 1), (8, 1)], fill=LIGHT)
    d.line([(1, 8), (3, 11), (5, 13), (8, 13)], fill=DARK)
    d.line([(8, 13), (13, 13)], fill=DARK)
    # inner plate
    plate = [(12, 3), (7, 3), (5, 4), (3, 6), (3, 8), (5, 10), (7, 11), (12, 11)]
    d.polygon(plate, fill=DARK)
    d.line(plate + [plate[0]], fill=CHANNEL)
    d.line([(7, 3), (5, 4), (3, 6)], fill=MID)
    # fins: two small horns pointing outward at the top and bottom of the plate
    for (x, y) in [(8, 2), (9, 2), (10, 2), (4, 2), (5, 2), (8, 12), (9, 12), (10, 12), (4, 12), (5, 12)]:
        px(im, x, y, LIGHT if y == 2 else MID)
    # outline
    d.line(body + [body[0]], fill=OUTLINE, width=1)
    d.line([(13, 0), (13, 14)], fill=OUTLINE)
    # the window's edge: columns 12-13 of the rows that face the fill
    for y in range(WIN_Y, WIN_Y + FILL_H):
        px(im, 13, y, DARK)
        px(im, 12, y, MID)
    px(im, 12, WIN_Y - 1, LIGHT)
    # gem
    gem = {(6, 5): (110, 120, 170), (5, 6): (150, 160, 210), (6, 6): (200, 210, 255), (7, 6): (150, 160, 210),
           (4, 7): (110, 120, 170), (5, 7): (200, 210, 255), (6, 7): (255, 255, 255), (7, 7): (200, 210, 255), (8, 7): (110, 120, 170),
           (5, 8): (110, 120, 170), (6, 8): (150, 160, 210), (7, 8): (110, 120, 170), (6, 9): (80, 90, 130)}
    for (x, y), c in gem.items():
        px(im, x, y, c + (255,))
    for (x, y) in [(6, 4), (5, 5), (7, 5), (4, 6), (8, 6), (3, 7), (9, 7), (4, 8), (8, 8), (5, 9), (7, 9), (6, 10)]:
        px(im, x, y, OUTLINE)
    for x in (10, 11):
        px(im, x, 7, LIGHT)
    px(im, 4, 3, HI)
    return im


def frame():
    im = Image.new("RGBA", (FRAME_W, FRAME_H), (0, 0, 0, 0))
    # rim between the caps
    for x in range(WIN_X, WIN_X + W):
        px(im, x, 0, (127, 133, 152, 255))
        px(im, x, 14, (58, 58, 72, 255))
        for y in (1, 3, 11, 13):
            px(im, x, y, CHANNEL)
        for y in (2, 12):
            px(im, x, y, (24, 23, 31, 255))
    # studs that interrupt the lanes
    for k in range(6):
        cx = WIN_X + 15 + 30 * k
        for dx in (-1, 0, 1):
            for y in (1, 2, 3):
                px(im, cx + dx, y, DARK if y != 2 else MID)
                px(im, cx + dx, y + 10, DARK if y != 2 else MID)
        px(im, cx, 2, LIGHT)
        px(im, cx, 12, LIGHT)
        px(im, cx - 1, 1, MID)
        px(im, cx - 1, 11, MID)
    left = cap_left()
    im.alpha_composite(left, (0, 0))
    right = left.transpose(Image.FLIP_LEFT_RIGHT)
    im.alpha_composite(right, (FRAME_W - CAP, 0))
    # the window itself must be clear
    for y in range(WIN_Y, WIN_Y + FILL_H):
        for x in range(WIN_X, WIN_X + W):
            im.putpixel((x, y), (0, 0, 0, 0))
    # a 1px inner shadow under the top lane and a highlight above the bottom lane, drawn into the lanes' edge rows
    for x in range(WIN_X, WIN_X + W):
        im.putpixel((x, 3), (16, 15, 22, 255))
        im.putpixel((x, 11), (74, 76, 90, 255))
    return im


# ================================================================ inside
def bg():
    a = np.zeros((FILL_H, W, 4), np.uint8)
    for y in range(FILL_H):
        base = np.array([10, 8, 14]) + np.array([9, 8, 12]) * (y / (FILL_H - 1))
        for x in range(W):
            hatch = 5 if (x + 2 * y) % 9 == 0 else 0
            a[y, x, :3] = np.clip(base + hatch, 0, 255)
            a[y, x, 3] = 255
    a[0, :, :3] = (a[0, :, :3] * 0.55).astype(np.uint8)
    return Image.fromarray(a, "RGBA")


def fill():
    rng = random.Random(4417)
    a = np.zeros((FILL_H, W, 4), np.uint8)
    shade = [0.98, 1.0, 0.88, 0.78, 0.68, 0.56, 0.42]
    for y in range(FILL_H):
        for x in range(W):
            chev = 1.0 if ((x + y * 2) % 8) < 4 else 0.93
            n = 1.0 + (rng.random() - 0.5) * 0.06
            v = max(0.0, min(1.0, shade[y] * chev * n))
            a[y, x] = (int(255 * v),) * 3 + (255,)
    return Image.fromarray(a, "RGBA")


def flame():
    """Tileable in x: pointed tongues from a sum of whole-period sines, hottest and widest at the bottom, with a bright core and flickering tips."""
    w, h = 64, FILL_H
    rng = random.Random(90210)
    comps = [(1, 0.30), (2, 0.26), (3, 0.24), (5, 0.20), (8, 0.14), (11, 0.08)]
    phases = [rng.random() * math.tau for _ in comps]
    raw = [sum(a * math.sin(math.tau * f * x / w + p) for (f, a), p in zip(comps, phases)) for x in range(w)]
    lo, hi = min(raw), max(raw)
    heights = [0.3 + 0.7 * ((v - lo) / (hi - lo)) ** 1.4 for v in raw]
    a = np.zeros((h, w, 4), np.uint8)
    for x in range(w):
        # a tongue leans: its tip is shifted by the neighbour's height
        lean = (heights[(x + 1) % w] - heights[(x - 1) % w]) * 1.2
        tongue = heights[x] * (h + 1.5)
        for y in range(h):
            up = (h - 1 - y)                                    # 0 at the bottom row
            k = 1.0 - (up + lean * 0.5) / max(1.0, tongue)
            if k <= 0:
                continue
            al = min(1.0, (k ** 0.7) * (0.45 + 0.55 * (1 - up / (h - 1))) * 1.15)
            core = k > 0.55 and up <= 3
            r = 255
            a[y, x] = (r, 255 if core else 226, 255 if core else 205, int(255 * al))
    return Image.fromarray(a, "RGBA")


def shine():
    w, h = 28, FILL_H
    a = np.zeros((h, w, 4), np.uint8)
    for y in range(h):
        for x in range(w):
            d = abs((x - 14) - (y - 3) * 1.4)
            v = max(0.0, 1 - d / 6.5) ** 1.6
            edge = min(1.0, x / 5.0, (w - 1 - x) / 5.0)
            al = v * max(0.0, edge)
            a[y, x] = (255, 255, 255, int(255 * min(1.0, al * 1.15)))
    return Image.fromarray(a, "RGBA")


def glow():
    w, h = 64, 3
    a = np.zeros((h, w, 4), np.uint8)
    dashes = [(2, 16), (30, 8), (44, 12)]          # start, length (periodic)
    for x in range(w):
        v = 0.22
        for s, ln in dashes:
            t = ((x - s) % w) / ln
            if 0 <= t < 1:
                v = max(v, math.sin(math.pi * t) ** 0.7)
        for y, row in enumerate((0.5, 1.0, 0.5)):
            a[y, x] = (255, 255, 255, int(255 * min(1.0, v * row)))
    return Image.fromarray(a, "RGBA")


def pips():
    im = Image.new("RGBA", (16, 8), (0, 0, 0, 0))
    diamond = [(3, 0), (4, 0), (2, 1), (3, 1), (4, 1), (5, 1), (1, 2), (2, 2), (3, 2), (4, 2), (5, 2), (6, 2),
               (0, 3), (1, 3), (2, 3), (3, 3), (4, 3), (5, 3), (6, 3), (7, 3), (0, 4), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4), (6, 4), (7, 4),
               (1, 5), (2, 5), (3, 5), (4, 5), (5, 5), (6, 5), (2, 6), (3, 6), (4, 6), (5, 6), (3, 7), (4, 7)]
    ds = set(diamond)
    for (x, y) in diamond:
        edge = any((x + dx, y + dy) not in ds for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        px(im, x, y, OUTLINE if edge else (46, 44, 58, 255))
        # lit gem on the right
        if edge:
            px(im, 8 + x, y, OUTLINE)
        else:
            core = 1.0 - (abs(x - 3.5) + abs(y - 3.5)) / 6.0
            v = int(150 + 105 * max(0.0, core))
            px(im, 8 + x, y, (v, v, v, 255))
    for (x, y) in [(3, 3), (4, 3), (3, 4), (4, 4)]:
        px(im, x, y, (58, 56, 72, 255))
    px(im, 8 + 2, 2, (255, 255, 255, 255))
    px(im, 8 + 3, 2, (255, 255, 255, 255))
    return im


def edge():
    w, h = 4, FILL_H
    a = np.zeros((h, w, 4), np.uint8)
    for y in range(h):
        for x in range(w):
            ramp = ((x + 1) / w) ** 1.5
            fade = 1.0 - abs(y - 3) / 4.5
            a[y, x] = (255, 255, 255, int(255 * max(0.0, min(1.0, ramp * fade * 1.2))))
    return Image.fromarray(a, "RGBA")


TEXTURES = {"frame.png": frame, "bg.png": bg, "fill.png": fill, "flame.png": flame, "shine.png": shine,
            "glow.png": glow, "pips.png": pips, "edge.png": edge}


# ================================================================ preview (not part of the build)
def tint(img, rgb, k=1.0):
    a = np.asarray(img).astype(np.float32)
    a[..., 0] *= rgb[0] / 255 * k
    a[..., 1] *= rgb[1] / 255 * k
    a[..., 2] *= rgb[2] / 255 * k
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def additive(canvas, layer, pos, rgb, alpha=1.0):
    t = np.asarray(tint(layer, rgb)).astype(np.float32)
    x, y = pos
    h, w = t.shape[:2]
    region = canvas[y:y + h, x:x + w, :3].astype(np.float32)
    region += t[..., :3] * (t[..., 3:4] / 255.0) * alpha
    canvas[y:y + h, x:x + w, :3] = np.clip(region, 0, 255).astype(np.uint8)


def hexrgb(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def preview(path, primary, secondary, progress=0.72, phase=2):
    scale = 4
    canvas = np.zeros((30, FRAME_W + 8, 4), np.uint8)
    canvas[..., :3] = (40, 60, 40)
    canvas[..., 3] = 255
    ox, oy = 4 + WIN_X, 3 + WIN_Y
    canvas[oy:oy + FILL_H, ox:ox + W] = np.asarray(bg())
    fw = int(progress * 181) + 1
    base = np.asarray(tint(fill(), primary))
    canvas[oy:oy + FILL_H, ox:ox + fw] = base[:, :fw]
    fl = flame()
    for off, al in ((10, 0.6), (40, 0.35)):
        for x0 in range(-off, fw, 64):
            c0, c1 = max(0, x0), min(fw, x0 + 64)
            if c1 > c0:
                sub = fl.crop((c0 - x0, 0, c1 - x0, FILL_H))
                additive(canvas, sub, (ox + c0, oy), secondary, al)
    additive(canvas, shine().crop((0, 0, 28, FILL_H)), (ox + 50, oy), (255, 255, 255), 0.7)
    additive(canvas, edge(), (ox + fw - 4, oy), secondary, 0.8)
    f = Image.fromarray(canvas)
    f.alpha_composite(tint(frame(), tuple(int(255 * 0.5 + c * 0.5) for c in primary)), (4, 3))
    canvas = np.asarray(f).copy()
    g = glow()
    for x0 in range(WIN_X, WIN_X + W, 64):
        w = min(64, WIN_X + W - x0)
        additive(canvas, g.crop((0, 0, w, 3)), (4 + x0, 3 + 1), secondary, 0.8)
        additive(canvas, g.crop((0, 0, w, 3)), (4 + x0, 3 + 11), secondary, 0.8)
    f = Image.fromarray(canvas)
    pp = pips()
    for i in range(5):
        lit = i <= phase
        sprite = pp.crop((8 if lit else 0, 0, 16 if lit else 8, 8))
        if lit:
            sprite = tint(sprite, secondary)
        f.alpha_composite(sprite, (4 + FRAME_W // 2 - 24 + i * 10 - 3, 3 + 10))
    f = f.resize((f.width * scale, f.height * scale), Image.NEAREST)
    f.save(path)


if __name__ == "__main__":
    if "--preview" in sys.argv:
        i = sys.argv.index("--preview")
        out = sys.argv[i + 1]
        pr = hexrgb(sys.argv[i + 2]) if len(sys.argv) > i + 3 else (0xE8, 0xB9, 0x23)
        sc = hexrgb(sys.argv[i + 3]) if len(sys.argv) > i + 3 else (0xFF, 0xF0, 0xA0)
        preview(out, pr, sc)
    else:
        for name, fn in TEXTURES.items():
            save(fn(), name)
            print("wrote", name)
